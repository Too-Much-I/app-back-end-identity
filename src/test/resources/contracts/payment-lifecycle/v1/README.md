# TMI-199 shared synthetic inputs

The four JSON files are byte-identical copies of Billing's reviewed fixtureVersion 1
from 2026-10-07. Their SHA-256 hashes are pinned by `PaymentLifecycleFixtureTests`.
They contain synthetic identifiers, not real user records or credentials.

Tests run without the Billing checkout and without external services:

```
./gradlew test --tests 'web.tosunsaeng.identity.contract.*'
```

The recovery state/decoder tests are independently written **test oracles**, not
production endpoint tests. Billing index JSON is hash-checked only, not installed.
Withdrawal timestamp tests use actual Identity entities, Spring Data's default
MappingMongoConverter and the existing event mapper with Boot Jackson defaults.
They do not connect to MongoDB and do not validate transactions, snapshot history,
deployment overrides, permissions or failure recovery in a replica set.

`protocol-cases.json` uses JSON numbers for oracle inputs; `wire-examples.json`
proposes decimal strings for actual recovery DTOs. Do not copy oracle input types
into production DTOs. UserWithdrawn emitted payload limit remains 4 KiB; a future
recovery receiver's separate request limit is not established by these tests.

Do not regenerate the golden digest from an implementation under test. Update
shared inputs and pinned hashes only after both services review the same revision.
