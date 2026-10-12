"use strict";

// Read-only metadata/duplicate audit. No create/update/delete commands are executed.
const BOOL_OPTIONS = ["unique", "sparse", "hidden"];
function canonical(value) {
    if (Array.isArray(value)) return value.map(canonical);
    if (value && typeof value === "object") return Object.fromEntries(
        Object.keys(value).sort().map(k => [k, canonical(value[k])]));
    return value;
}
function ttl(value) {
    if (value === undefined) return undefined;
    if (value && typeof value === "object" && "$numberLong" in value) return Number(value.$numberLong);
    if (typeof value === "string" || value === null) return NaN;
    return Number(value);
}
function compatible(actual, expected) {
    const options = expected.options;
    return actual.name === options.name
        && JSON.stringify(actual.key) === JSON.stringify(expected.keys)
        && BOOL_OPTIONS.every(k => Boolean(actual[k]) === Boolean(options[k]))
        && Object.is(ttl(actual.expireAfterSeconds), ttl(options.expireAfterSeconds))
        && ["partialFilterExpression", "collation"].every(k =>
            JSON.stringify(canonical(actual[k])) === JSON.stringify(canonical(options[k])));
}
function validate(manifest, name) {
    if (typeof name !== "string" || !name.trim() || name !== name.trim()
        || ["admin", "local", "config"].includes(name.toLowerCase())) throw Error("Explicit application database required");
    if (manifest.formatVersion !== 1 || !Array.isArray(manifest.collections)) throw Error("Unsupported manifest");
    const names = new Set();
    for (const c of manifest.collections) {
        if (typeof c.collection !== "string" || !c.collection || names.has(c.collection)
            || !Array.isArray(c.indexes)) throw Error("Invalid collection definition");
        names.add(c.collection);
        const indexNames = new Set();
        for (const i of c.indexes) {
            if (!i.options?.name || indexNames.has(i.options.name) || !i.keys
                || !Object.keys(i.keys).length || Object.values(i.keys).some(v => ![1, -1].includes(v)))
                throw Error("Unsupported index definition");
            indexNames.add(i.options.name);
            if (i.options.expireAfterSeconds !== undefined && !Number.isFinite(ttl(i.options.expireAfterSeconds)))
                throw Error("Invalid TTL");
        }
    }
}
async function inspect(database, manifest) {
    validate(manifest, database.getName());
    const existing = new Set((await database.getCollectionInfos()).map(c => c.name));
    const report = {database: database.getName(), mode: "READ_ONLY", missingCollections: [],
        missingIndexes: [], conflicts: [], uniqueChecks: [], matchedIndexes: 0};
    for (const c of manifest.collections) {
        if (!existing.has(c.collection)) {
            report.missingCollections.push(c.collection);
            report.missingIndexes.push(...c.indexes.map(i => ({collection: c.collection, ...i})));
            continue;
        }
        const collection = database.getCollection(c.collection);
        const indexes = await collection.getIndexes();
        for (const expected of c.indexes) {
            const actual = indexes.find(i => i.name === expected.options.name);
            if (actual && compatible(actual, expected)) report.matchedIndexes++;
            else if (actual) report.conflicts.push({collection: c.collection, index: expected.options.name,
                reason: "INCOMPATIBLE_DEFINITION"});
            else report.missingIndexes.push({collection: c.collection, ...expected});
            if (!expected.options.unique) continue;
            const fields = Object.keys(expected.keys);
            const prefix = [];
            if (expected.options.partialFilterExpression) prefix.push({$match: expected.options.partialFilterExpression});
            if (expected.options.sparse) prefix.push({$match: {$or: fields.map(k => ({[k]: {$exists: true}}))}});
            const arrayRows = await (await collection.aggregate([...prefix,
                {$match: {$expr: {$or: fields.map(k => ({$eq: [{$type: `$${k}`}, "array"]}))}}},
                {$limit: 1}, {$count: "count"}], {maxTimeMS: 15000})).toArray();
            if (arrayRows.length || expected.options.collation) {
                report.conflicts.push({collection: c.collection, index: expected.options.name,
                    reason: "SPECIAL_UNIQUENESS_REQUIRES_REVIEW"});
                continue;
            }
            const rows = await (await collection.aggregate([...prefix, {$group: {
                _id: Object.fromEntries(fields.map((k, n) => [`k${n}`, {$ifNull: [`$${k}`, null]}])),
                count: {$sum: 1}}}, {$match: {count: {$gt: 1}}}, {$count: "groups"}],
                {maxTimeMS: 15000})).toArray();
            const duplicateGroups = rows[0]?.groups ?? 0;
            report.uniqueChecks.push({collection: c.collection, index: expected.options.name, duplicateGroups});
            if (duplicateGroups) report.conflicts.push({collection: c.collection, index: expected.options.name,
                reason: "DUPLICATE_GROUPS"});
        }
    }
    report.noDetectedIndexConflicts = report.conflicts.length === 0;
    // Even a clean audit is not approval, a backup, or proof of safety with concurrent writers.
    return report;
}
module.exports = {compatible, validate, inspect};
