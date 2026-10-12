"use strict";

// DDL only: no inserts, updates, deletes, drops, collMod or worker activation.
// Callers must first validate data/unique keys. MongoDB also checks unique builds.
const {compatible, validate} = require("./identity-index-audit.cjs");
const {createHash} = require("node:crypto");
function digest(value) { return createHash("sha256").update(JSON.stringify(value)).digest("hex"); }
function target(database, expectedDatabase, manifest) {
    if (!expectedDatabase || database.getName() !== expectedDatabase) throw Error("Database mismatch");
    validate(manifest, expectedDatabase);
    for (const c of manifest.collections) for (const i of c.indexes) {
        if (Object.keys(i.options).some(k => !["name", "unique", "sparse", "hidden",
            "partialFilterExpression", "expireAfterSeconds", "collation"].includes(k)))
            throw Error("Unsupported index option");
    }
}
async function plan(database, expectedDatabase, manifest) {
    target(database, expectedDatabase, manifest);
    const info = new Map((await database.getCollectionInfos()).map(c => [c.name, c]));
    const result = {database: expectedDatabase, manifestDigest: digest(manifest), collections: [], indexes: []};
    for (const c of manifest.collections) {
        const existing = info.get(c.collection);
        if (existing && (existing.type !== "collection" || Object.keys(existing.options || {}).length))
            throw Error(`Nonstandard collection requires review: ${c.collection}`);
        if (!existing) result.collections.push(c.collection);
        const collection = database.getCollection(c.collection);
        const indexes = existing ? await collection.getIndexes() : [];
        for (const expected of c.indexes) {
            const named = indexes.find(i => i.name === expected.options.name);
            if (named) {
                if (!compatible(named, expected)) throw Error(`Index conflict: ${c.collection}/${expected.options.name}`);
                continue;
            }
            if (indexes.some(i => JSON.stringify(i.key) === JSON.stringify(expected.keys)))
                throw Error(`Existing key under another name: ${c.collection}/${expected.options.name}`);
            if (existing && expected.options.expireAfterSeconds !== undefined
                && await collection.countDocuments({}, {limit: 1, maxTimeMS: 15000}))
                throw Error(`Refuse new TTL on populated collection: ${c.collection}`);
            result.indexes.push({collection: c.collection, ...expected});
        }
    }
    return result;
}
async function apply(database, expectedDatabase, manifest, approvedPlan, emit = () => {}) {
    // Exact plan binding prevents silently applying an expanded or stale diff.
    const current = await plan(database, expectedDatabase, manifest);
    if (digest(current) !== digest(approvedPlan)) throw Error("Plan changed; re-review required");
    const created = {collections: 0, indexes: 0};
    for (const name of current.collections) {
        await database.createCollection(name);
        created.collections++;
        emit({event: "COLLECTION_CREATED", collection: name});
    }
    for (const i of current.indexes) {
        const collection = database.getCollection(i.collection);
        const indexes = await collection.getIndexes();
        const named = indexes.find(x => x.name === i.options.name);
        if (named) {
            if (!compatible(named, i)) throw Error("Concurrent incompatible index");
            continue;
        }
        if (indexes.some(x => JSON.stringify(x.key) === JSON.stringify(i.keys)))
            throw Error("Concurrent same-key index; re-review required");
        if (i.options.expireAfterSeconds !== undefined
            && await collection.countDocuments({}, {limit: 1, maxTimeMS: 15000}))
            throw Error("Collection populated before TTL creation; stopped");
        await collection.createIndex(i.keys, i.options);
        created.indexes++;
        emit({event: "INDEX_CREATED", collection: i.collection, index: i.options.name});
    }
    const remaining = await plan(database, expectedDatabase, manifest);
    if (remaining.collections.length || remaining.indexes.length) throw Error("Post-verification incomplete");
    return {...created, verified: true, manifestDigest: current.manifestDigest};
}
module.exports = {plan, apply};
