"use strict";
const {test} = require("node:test");
const assert = require("node:assert/strict");
const {compatible, validate, inspect} = require("./identity-index-audit.cjs");
const expected = {keys: {userId: 1, requestHash: 1}, options: {name: "request_unique", unique: true,
    partialFilterExpression: {requestHash: {$type: "string"}}}};
const manifest = {formatVersion: 1, collections: [{collection: "sessions", indexes: [expected]}]};
test("exact partial unique and ordered keys required", () => {
    const actual = {key: expected.keys, ...expected.options};
    assert.ok(compatible(actual, expected));
    for (const patch of [{unique: false}, {hidden: true}, {sparse: true}, {partialFilterExpression: {}},
        {collation: {locale: "en"}}, {key: {requestHash: 1, userId: 1}}, {name: "other"}])
        assert.equal(compatible({...actual, ...patch}, expected), false);
});
test("TTL zero differs from missing and string values", () => {
    const e = {keys: {cleanupAt: 1}, options: {name: "ttl", expireAfterSeconds: 0}};
    assert.ok(compatible({key: e.keys, ...e.options}, e));
    assert.ok(compatible({key: e.keys, name: "ttl", expireAfterSeconds: {$numberLong: "0"}}, e));
    for (const v of [undefined, "0", null, 3600]) assert.equal(
        compatible({key: e.keys, name: "ttl", expireAfterSeconds: v}, e), false);
});
test("reject ambiguous database or duplicate manifest entries", () => {
    for (const db of ["", "admin", " local ", "config"]) assert.throws(() => validate(manifest, db));
    assert.throws(() => validate({...manifest, collections: [...manifest.collections, ...manifest.collections]}, "app"));
});
function fixture(indexes, results = []) {
    const queries = [];
    return {queries, getName: () => "audit-fixture", getCollectionInfos: async () => [{name: "sessions"}],
        getCollection: () => ({getIndexes: async () => indexes,
            aggregate: (pipeline, options) => { queries.push({pipeline, options}); return {toArray: async () => results.shift() ?? []}; }})};
}
test("missing definitions returned without any write capability", async () => {
    const db = fixture([]);
    const r = await inspect(db, manifest);
    assert.equal(r.mode, "READ_ONLY"); assert.equal(r.missingIndexes.length, 1);
    assert.equal(r.uniqueChecks[0].duplicateGroups, 0); assert.equal(r.noDetectedIndexConflicts, true);
    assert.ok(db.queries.every(q => q.options.maxTimeMS === 15000));
});
test("supports asynchronous aggregate cursors from an embedded mongosh", async () => {
    const db = fixture([]);
    const original = db.getCollection;
    db.getCollection = () => {
        const collection = original();
        const aggregate = collection.aggregate;
        return {...collection, aggregate: async (...args) => aggregate(...args)};
    };
    const r = await inspect(db, manifest);
    assert.equal(r.uniqueChecks[0].duplicateGroups, 0);
    assert.equal(r.noDetectedIndexConflicts, true);
});
test("duplicates and array keys block approval without revealing values", async () => {
    const duplicate = await inspect(fixture([], [[], [{groups: 2}]]), manifest);
    assert.equal(duplicate.noDetectedIndexConflicts, false);
    assert.equal(duplicate.conflicts[0].reason, "DUPLICATE_GROUPS");
    const array = await inspect(fixture([], [[{count: 1}]]), manifest);
    assert.equal(array.conflicts[0].reason, "SPECIAL_UNIQUENESS_REQUIRES_REVIEW");
});
test("absent collections do not trigger scans or implicit creation", async () => {
    const r = await inspect({getName: () => "audit-fixture", getCollectionInfos: async () => [],
        getCollection: () => {throw Error("must not open absent collection");}}, manifest);
    assert.deepEqual(r.missingCollections, ["sessions"]);
    assert.equal(r.missingIndexes.length, 1);
});
test("mismatching existing index never becomes an automatic replacement", async () => {
    const r = await inspect(fixture([{key: expected.keys, name: expected.options.name}]), manifest);
    assert.equal(r.noDetectedIndexConflicts, false); assert.equal(r.missingIndexes.length, 0);
    assert.equal(r.conflicts[0].reason, "INCOMPATIBLE_DEFINITION");
});
