"use strict";
const {test} = require("node:test");
const assert = require("node:assert/strict");
const {plan, apply} = require("./mongodb-additive-ddl.cjs");
const index = {keys: {cleanupAt: 1}, options: {name: "cleanup", expireAfterSeconds: 0}};
const manifest = {formatVersion: 1, collections: [{collection: "events", indexes: [index]}]};
function fixture(initial = []) {
    const state = new Map(initial);
    const writes = [];
    return {state, writes, getName: () => "fixture",
        getCollectionInfos: async () => [...state.keys()].map(name => ({name, type: "collection", options: {}})),
        createCollection: async name => {writes.push(name); state.set(name, {indexes: [], count: 0});},
        getCollection: name => ({getIndexes: async () => state.get(name).indexes,
            countDocuments: async () => state.get(name).count,
            createIndex: async (key, options) => {writes.push(options.name); state.get(name).indexes.push({key, ...options});}})};
}
test("read-only plan then additive apply and idempotent verification", async () => {
    const db = fixture(); const p = await plan(db, "fixture", manifest);
    assert.equal(db.writes.length, 0);
    const r = await apply(db, "fixture", manifest, p);
    assert.equal(r.verified, true); assert.deepEqual(db.writes, ["events", "cleanup"]);
    const empty = await plan(db, "fixture", manifest);
    assert.equal((await apply(db, "fixture", manifest, empty)).indexes, 0);
});
test("wrong database and changed plan fail before writes", async () => {
    const db = fixture(); await assert.rejects(plan(db, "other", manifest));
    const p = await plan(db, "fixture", manifest);
    await assert.rejects(apply(db, "fixture", manifest, {...p, collections: []}));
    assert.equal(db.writes.length, 0);
});
test("populated TTL and incompatible or differently named indexes fail closed", async () => {
    for (const item of [{count: 1, indexes: []}, {count: 0, indexes: [{key: index.keys, name: "cleanup"}]},
        {count: 0, indexes: [{key: index.keys, name: "different"}]}]) {
        const db = fixture([["events", item]]);
        await assert.rejects(plan(db, "fixture", manifest)); assert.equal(db.writes.length, 0);
    }
});
test("TTL is rechecked immediately before creation", async () => {
    const db = fixture(); const p = await plan(db, "fixture", manifest);
    await assert.rejects(apply(db, "fixture", manifest, p, event => {
        if (event.event === "COLLECTION_CREATED") db.state.get("events").count = 1;
    }));
    assert.deepEqual(db.writes, ["events"]);
});
test("partial failures preserve created structures and do not drop anything", async () => {
    const db = fixture(); const p = await plan(db, "fixture", manifest);
    const get = db.getCollection;
    db.getCollection = name => ({...get(name), createIndex: async () => {throw Error("build failure");}});
    await assert.rejects(apply(db, "fixture", manifest, p));
    assert.deepEqual(db.writes, ["events"]); assert.equal(db.state.has("events"), true);
});
