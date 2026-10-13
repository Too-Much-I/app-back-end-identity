> **개정 전 보관본 — 현행 구현 지침 아님.** 사용자 승인 단순화로 [현행 계획](TMI-203-withdrawal-recovery-snapshot-feed-plan.md)이 아래 원격 ACK/generation 설계를 대체한다. 당시 조사·세부 명세 비교를 위해 원문을 보존한다.

# TMI-203 — 탈퇴 누락 복구 snapshot·feed·checkpoint 구현 계획

- 작성일: 2026-10-12 / 조사 브랜치: develop / Jira: [TMI-203](https://to-teacher.atlassian.net/browse/TMI-203)
- 상태: 계획서. 구현·DB 변경·배포·활성화·Jira 완료 승인이 아니다.
- 범위 개정: 2026-10-12 사용자 승인으로 최초 결제 출시에서 과거 전체 탈퇴 이력의 legacy COMPLETE 증명 의무를 제외한다. 아래 §3.1이 이 계획의 초기 출시 범위 기준이며 exact API·복구 동작은 유지한다.
- 후속 구현 브랜치 제안: `codex/TMI-203-withdrawal-recovery` (이번에는 생성하지 않음).
- 기준: [공동 기술 계약](../../../billing/docs/contracts/IDENTITY-BILLING-PAYMENT-LIFECYCLE-TECHNICAL-CONTRACT.md) §5.2~5.7, [수락한 exact wire/index](billing-c0-exact-wire-index-proposal-2026-10-07.md) §5~6 및 §8. 요약과 원문의 차이는 구현 전 확인하며 임의 변경하지 않는다.

## 1. 5줄 결론

1. TMI-202가 탈퇴 사실을 기록·전송한다면 TMI-203은 Billing이 놓친 과거 상태와 이후 사건을 다시 받아 적용 여부를 보고할 통로다([TMI-202 결과](TMI-202-implementation-result.md)).
2. 먼저 실제 Mongo/driver에서 같은 시점 T의 User와 순번 H를 읽을 수 있는지 검증한다. 일반 최신 조회로 대체하지 않는다(공동 계약 §5.3 C).
3. 과거 목록(snapshot) → H 이후 연속 사건(feed) → 적용 확인(checkpoint)은 유지하되, 최초 출시에는 legacy COMPLETE 증명을 요구하지 않는다. 증거 없는 PARTIAL/UNKNOWN은 그대로 유지한다(사용자 승인 §3.1).
4. 복구마다 새 세대를 발급하고 같은 제어 행에 CAS 쓰기를 하여 옛 작업·옛 ACK가 복원 이후 상태를 바꾸지 못하게 한다(§5.7).
5. 원천120일·snapshot7일 정리를 적용 증거와 연결하되, 구매/자동삭제 활성화는 TMI-204·공동 staging 검증과 별도 승인 이후다(§3, §5.5).

## 2. 사용자가 반드시 읽어야 하는 내용

### 무엇이 추가되는가

예를 들어 Billing이 백업으로 복원되어 일부 탈퇴 처리를 잃었다면, Identity가 **특정 시점에 이미 탈퇴한 사용자 목록**을 제공한다. Billing은 그 목록을 적용한 뒤 **그 시점 이후의 탈퇴 사건**을 순서대로 받는다. 마지막으로 어디까지 적용했는지 Identity에 알린다. 앱 사용자가 직접 호출하는 API가 아니다.

snapshot은 과거 탈퇴 상태이고 feed는 원래 eventId를 가진 사건이다. 과거 목록에 임의 eventId를 만들어 원래 사건을 복원한 것처럼 취급하지 않는다. Identity는 Billing의 ACK를 검증·기록하지만 Billing DB commit 자체를 직접 증명하지는 못한다.

### 지금 구현돼 있는 것과 새로 할 것

- **확인된 코드:** TMI-202 journal·stream·delivery, 제한적 재시도, 명시 index 준비/검증, cleanup의 Evidence 확장 지점이 있다. 현재 index 목록은 수락한13개 중 I2의7개다.
- **TMI-203 계획:** recovery control/operation, snapshot materialization, 서명 cursor, feed, checkpoint, I3 index6개, 보존·coverage adapter와 전용 내부 인증 경로를 구현한다.
- **아직 운영 준비가 끝난 것은 아님:** TMI-202의 clock/capture RuntimeEvidence, 관리자 권한·감사 실행기, 구매 활성화 공동 증거, 실환경 장애 검증은 별도 남은 항목이다. TMI-203을 작성했다고 이들이 해결되지 않는다.

현재 [SecurityConfig](../../src/main/java/web/tosunsaeng/identity/global/config/SecurityConfig.java)는 기본적으로 JWT 인증을 요구한다. 이 경로에 일반 사용자 JWT를 허용하거나 `permitAll`만 추가해서는 안 된다. Billing 전용 AWS_IAM/SigV4 ingress와 신뢰할 수 있는 서비스 주체 전달 경계를 새로 검증해야 한다.

## 3. 사용자가 결정해야 하는 사항

기존 보존 정책(원천120일, snapshot 최대7일, operation120일)은 유지한다. 최초 출시의 legacy 완전성 요건은 아래처럼 사용자 승인으로 변경했다. 다음은 구현 또는 운영 실행 전에 별도 확인한다.

- 개발은 이 계획 범위로 진행하되 실제 DB index 생성·이관·cleanup apply·배포·활성화는 별도 실행 승인이다.
- 고정 T 조회가 현재 Mongo/driver에서 지원되지 않거나 Lattice 인증 ingress를 구성할 수 없다면 진행을 멈추고 대안을 양 서버가 합의한다. live scan·공개 feed·사용자 JWT로 우회하지 않는다.
- 보존 연장, gap 사용자의 별도 처리/이용 제한, 추가 개인정보 저장, 신규 pagination/계약 변경이 필요하면 영향과 대안을 먼저 제시한다.
- Jira 댓글·상태 변경은 이번에 하지 않는다. 관련 구현 병합과 완료 근거 확인 전 Done으로 처리하지 않는다.

### 3.1 승인된 최초 결제 출시 범위 변경 — 2026-10-12

**사용자 확인 전제:** 이번이 최초 결제 출시이며 기존 Billing 구매 계정, purchaseAccountRefId 발급, 실제 결제 거래가 없다. 이는 사용자에게 확인받은 출시 전제로 기록하며, 외부 DB·결제 조회를 수행한 결과라고 표현하지 않는다. 과거 거래가 있을 가능성을 이유로 동일 확인을 반복 요구하지 않는다.

- 과거 전체 탈퇴 이력을 완전하게 복구·증명하는 별도 작업과 legacy COMPLETE 달성을 최초 출시 필수 조건에서 제외한다.
- 기존 증거로 완전성을 증명하지 못하면 PARTIAL/UNKNOWN을 사실대로 반환한다. 과거 완전성 확인만을 위해 출시를 무기한 대기하거나 snapshot/RESYNC를 반복하지 않는다.
- 결제 시작 기준점을 명시적으로 기록하고 그 이후 탈퇴의 durable capture·전달·복구 연속성을 검증한다. 기준점 기록 권장 필드는 환경, 활성 streamId, 관측 순번, 관측 시각, capture 준비 근거 및 최초 결제 활성화 순서다. wall-clock 시각만으로 commit 경계를 대체하지 않으며 실제 값은 활성화 단계에서 확정한다.
- 기준점은 출시 범위 평가를 위한 증거이지 기존 snapshot/feed 필터가 아니다. 기존 Identity 탈퇴 목록을 제외하거나 빈 목록으로 반환하지 않는다. H/H2·checkpoint를 기준점 값으로 임의 덮어쓰지 않는다.
- 고정 T/H snapshot, 고정 H2 연속 feed, ACK·상태 조회, generation/old-worker fence, 만료·누락·응답 유실·복원 처리, TMI-202 capture/delivery와 exact DTO·인증은 전부 유지한다.
- 기준점 이후 GAP/복원 증거 소실은 기존 복구 규칙대로 처리한다. 전체 legacy PARTIAL/UNKNOWN 허용을 이용해 이 구간의 GAP을 무시하거나 구매 활성화 증거를 무조건 true로 만들지 않는다.
- 이후 기존 구매 연결·거래가 발견되면 영향 범위를 다시 검토한다. Billing의 출시 gate도 이 구분을 반영해야 하며 기존 전역 purge 안전장치를 일괄 해제하지 않는다.
- 실제 배포·판매·삭제 활성화는 별도 승인이다. 이번에 변경한 것은 Identity 계획의 출시 필수 범위이며, Billing 저장소·공동 계약 원문·Jira는 수정하지 않았다. 후속 구현 시 관련 gate 문서/테스트를 이 승인과 일치시키고 차이가 있으면 명시한다.

## 4. 주요 위험과 미확인 사항

| 위험 | 대응·완료 근거 |
| --- | --- |
| Mongo 고정 snapshot read/history 지원 | 실제 버전·driver API·동일 T 재사용·5분 history 유지 실험을 선행. Mock 테스트로 대체 불가 |
| 탈퇴시각은 과거인데 commit은 늦음 | 동일 T의 H와 User를 함께 읽고 sequence>H feed로 연결. withdrawnAt watermark 금지 |
| 세대 확인 직후 restore 발생 | snapshot seal/scan 발급/ACK/cleanup이 recovery control을 동일 transaction에서 CAS 쓰기 |
| legacy 자료가 이미 삭제됨 | 운영 근거 없으면 COMPLETE 불가, PARTIAL/UNKNOWN과 knownGaps 유지. 최초 출시 blocker로 삼지 않되 기준점 이후 GAP은 별도 검증 |
| cleanup이 복구 중 필요한 자료 삭제 | 유효 lease·expiry·floor/interval·소비자 증거를 원자 검증. 누락을 빈 성공으로 응답하지 않음 |
| snapshot 빌드 중 장애/중복 worker | lease/version과 불변 rows seal, 동일 T 재개 불가 시 FAILED. 부분 데이터 비공개 |
| 직접 ingress 및 헤더 위조 | Lattice role/route/environment 제한과 직접 ingress 차단을 함께 검증. 세대 header 자체는 인증이 아님 |
| 단순 문자열 정렬/UUID 정규화 | User _id 타입·collation·canonical UUID 사전 조사. 저장된 ID를 소문자로 임의 변경하지 않음 |
| 대용량·metadata 무한 증가 | 최대20/page·16KiB·5분 budget, bounded batch/interval/gap. 넘치면 조용한 절삭 대신 실패·검토 |
| TMI-202 운영 adapter 미완성 | I3 Evidence 구현과 I2 관리자/runtime 증거를 분리 추적. 무조건 true adapter 금지 |

## 5. 현재 작업과 직접 관련된 구현 설계

### 5.1 개발 단위와 선후관계

| 단계 | 산출물 | 다음 단계 진입 기준 |
| --- | --- | --- |
| 0. 실현 가능성 검증 | 고정 T/H 읽기·history loss 재현, 기존 schema/index 조사, ingress 신뢰 경계 설계 | 동일 T 증명 또는 지원 불가 보고. 외부 운영 DB는 사용하지 않음 |
| PR1. 복구 기반 | strict DTO/전용 보안 chain, generation authority·operation 멱등, I3 index와 상태 조회 | 동시 recoveries 한 건만 성공, 이전 세대 거절, exact fixture 통과 |
| PR2. snapshot | durable run/rows/manifest, builder·lease·canonical digest·page/expiry | H>0/빈 목록/늦은 commit/history loss/부분 공개 금지 검증 |
| PR3. feed·ACK·정리 | 고정 H2 cursor, BASELINE/RESYNC/FEED, coverage·I2 Evidence·보존 job/관측 | 누락·낮은 ACK·세대/cleanup 경합·복원 테스트 통과 |
| 공동 검증 | Billing B2(TMI-204) fake→staging, IAM·restore·ACK 유실 | 양측 검증 결과와 남은 gate 명시. 실제 활성화는 별도 승인 |

세 PR은 검토 단위이며 세 번의 운영 배포를 강제하지 않는다. 모든 신규 기능은 기본 OFF로 구현한다. 0단계의 실험은 별도 격리 harness로 실행하고 기본 `application-test.yml`은 실제 Atlas/외부 Provider를 호출하지 않는다.

### 5.2 책임·데이터 모델

- 기존 User, purchase control, journal/stream의 탈퇴 원자성은 TMI-200/202 계약 그대로 유지한다. 새 API가 독립적으로 User 상태를 수정하지 않는다.
- 새 package 제안: `domain/user/withdrawalrecovery`. controller/strict decoder, 인증 principal adapter, generation service/store, snapshot builder/store, cursor codec, feed/checkpoint service, cleanup/coverage를 분리한다. 명칭은 내부 구현 제안이다.
- `withdrawal_consumer_recovery_controls`: `(environment,streamId,consumer)` 현재 세대와 내부 CAS version, 복구 상태. 내부 version은 공개 checkpoint version과 다르다.
- `withdrawal_recovery_operations`: scope+operationId, canonical 요청 binding, 발급 결과·생성시각. 같은 key/같은 body는 같은 결과, 다른 body는409. 원문 사용자 데이터는 저장하지 않는다.
- `withdrawal_snapshot_runs`: operation binding·status·lease/version·deadline과 불변 manifest의 논리적 경계를 분리한다. READY 이후 manifest는 변경하지 않는다. 최소 operation metadata와 7일 대상 manifest는 분리 제거 가능하게 저장한다.
- `withdrawal_snapshot_rows`: snapshotId별 사실 rows. page 경계는 READY 때 고정한다. 미완성 staging도 동일7일 정리 대상이다.
- `withdrawal_consumer_checkpoints`: 세대별 baseline/feed·version·coverage의 최소 증거. digest/사용자 목록을 장기 복제하지 않는다.
- 기존 stream에 unavailableIntervals/coverage 평가를 연결한다. 현재 Stream record에는 replayFloor만 있으므로 additive schema 변경과 기존 문서 읽기 호환을 검증한다. 별도 컬렉션/index 필요 시 명세를 먼저 보완한다.

### 5.3 세대 발급과 transaction fence

복구 단위는 `(environment,authenticatedConsumer,streamId)`이다. consumer/environment를 body로 받지 않는다. 새 generation은 lowercase UUIDv4이며 resumeGeneration·checkpoint version·streamId와 구분한다.

1. 운영자는 Billing worker/ACK/purge를 멈추고 transaction drain·구프로세스 DB 쓰기 차단을 확인한다(TMI-204/운영 책임).
2. recoveries는 INITIAL일 때만 expectedGeneration=null을 허용한다. 이후 현재 세대 exact CAS로 새 generation을 발급한다.
3. operation 기록과 control 변경을 같은 majority+j transaction으로 commit한다. 응답 유실은 같은 key로 재호출하며 새 key로 CAS 실패를 우회하지 않는다.
4. 새 상태는 RECOVERING/coverage UNKNOWN, 이전 baseline/feed를 복사하지 않는다. 오래된 operation 재응답은 현재 generation 설치 권한이 아님을 명시한다.
5. snapshot READY 확정·scan 발급·ACK commit·source cleanup은 이 control을 실제 CAS 쓰기하여 교체 transaction과 충돌시킨다. 단순 조회 검증이나 같은 값의 no-op update에 의존하지 않는다.
6. active stream 교체와도 경합하도록 source fence를 확인한다. stream 복원 중 구작업이 seal/ACK하는 경우 STREAM_REPLACED로 거절한다. lock 순서와 bounded retry를 공통화하고 결과 불명은 성공/롤백으로 단정하지 않는다.

Billing local 세대 설치·old worker fencing은 TMI-204다. Identity에서 그 코드를 구현하거나 status 읽기만으로 Billing 설치 완료라고 판단하지 않는다.

### 5.4 고정 snapshot

- 동일 snapshot read의 atClusterTime T에서 WITHDRAWN User와 committed lastSequence H를 읽는다. T는 BSON Timestamp이며 Date나 wall-clock 시각으로 대신하지 않는다.
- `_id` keyset의 모든 페이지가 같은 T를 사용해야 한다. canonical lowercase UUID/simple collation 전제를 검증하고 userId ASCII 순서를 보장한다. 잘못된 legacy ID는 숨기거나 변환하지 않고 blocker로 보고한다.
- reader는 T에서 읽고 materializer는 durable staging에 bounded write한다. 전체 User를 단일 거대 transaction에 넣지 않는다. 공개 page 크기와 내부 scan batch는 구분한다.
- run 시작부터 최대5분. history loss/timeout/동일 T 재개 불가 시 FAILED로 닫는다. failureCode는 exact 규격의 SNAPSHOT_HISTORY_LOST/SNAPSHOT_BUILD_TIMEOUT/SNAPSHOT_BUILD_FAILED만 사용한다.
- 최종 모든 row/count/order/digest/page 경계를 검증한 후 current generation·stream/lease CAS와 함께 READY를 원자 확정한다. BUILDING/FAILED 데이터는 page API에서 제공하지 않는다.
- rows는 `{userId,withdrawnAt}`만 공개한다. 원 eventId를 만들지 않는다. 신규 시각의 영속 밀리초와 과거 나노초 정밀도 계약을 구분한다.
- canonical compact JSON의 필드 순서는 userId→withdrawnAt, UTF8 noBOM. 각 item 앞에 unsigned32 BE byte 길이를 붙이고 전체 순서의 SHA256 lower64hex를 구한다. Java Instant.toString 기준0/3/6/9자리, float 금지. page JSON은 digest 대상이 아니다.
- 빈 목록은 zero-byte digest와 빈 최종 page1개. 최대20건/16KiB에 맞춘 page 경계는 READY 뒤 불변이다. knownGaps가 상한을 넘으면 조용히 자르지 않고 build 실패/설계 검토한다.
- 같은 멱등 key는 같은 run이다. BUILDING202/Retry-After5, READY·FAILED·EXPIRED 재요청200 status. FAILED/EXPIRED 재시작은 새 key다.

### 5.5 feed·서명 cursor

첫 요청의 majority lastSequence를 inclusive H2로 고정한다. cursor는 environment/consumer/stream/generation/startExclusive/H2/issuedAt/expiresAt에 바인딩하고 수명1시간으로 한다. 이후 페이지에서 목표를 늘리지 않는다.

- `(afterSequence,H2]`의 연속 sequence와 기존4필드 event를 반환한다. nextAfterSequence=scannedThrough이며 done은 H2 도달과 동치다.
- after<H2인데 빈 결과/다음 sequence 누락이면 expiry floor/interval을 검사한다. 알려진 소실410 REPLAY_UNAVAILABLE, gap409 COVERAGE_GAP, 조사 중503이며 빈 완료 응답을 하지 않는다.
- startExclusive≤after≤H2를 검증한다. H2에 이미 도달한 요청만 빈 정상 완료 page가 가능하다. MAX_VALUE 증가 overflow를 피한다.
- cursor는 opaque 서명값이며 URL이 아니다. 구현 권장안은 전용 HMAC key ring+key id와 bounded canonical payload다. 실제 키는 Secret 주입, JWT private key/다른 업무 HMAC 재사용 금지. 알고리즘·rotation은 양측 wire를 바꾸지 않는 내부 설계로 검증한다.
- snapshot cursor도 소유자/stream/세대/run/page/expiry에 귀속한다. 이전 유효 cursor를 허용할 key rotation window는 feed1시간·snapshot 최대7일과 일치시킨다. 키/서명 오류를 성공으로 우회하지 않는다.
- cursor 원문·사용자 목록을 로그에 남기지 않는다. 만료 시 새 scan을 열되 Billing의 실제 local checkpoint부터 재개한다.

### 5.6 checkpoint·coverage

BASELINE/RESYNC는 READY snapshot의 소유자·stream·세대·expiry·contentDigest·throughSequence==H와 expectedCheckpointVersion을 검증한다. 최초 checkpoint만 null version이며 최초 생성 version=1이다. RESYNC는 먼저 새 세대를 발급해야 한다.

- 동일 유효 baseline 재ACK는204이며 기존 상태를 되돌리지 않는다. manifest 정리 후 재ACK는410이지만 이미 저장된 checkpoint는 보존한다.
- FEED ACK는 baseline 전409 RECOVERY_NOT_READY. cursor·principal·현재 세대·stream·유효기간·범위를 모두 검증한 후에만 동일/낮은 정상 ACK를204 NOOP 처리한다.
- 높은 ACK는 연속 적용했다는 Billing의 attestation이다. startExclusive가 현재 검증 범위와 단절된 jump를 허용하지 않으며 snapshot baseline 또는 기존 feed 이후의 연속 범위를 검사한다. 원격 DB 적용 사실을 직접 검증했다고 표시하지 않는다.
- ACK 유실/동시 ACK는 durable checkpoint와 CAS로 수렴한다. verifiedAt은 같은 증거 최초 commit 관측값을 유지한다. version 증가 overflow는 fail-closed한다.
- BASELINE→BASELINED, 고정 scan H2까지 FEED ACK→CAUGHT_UP. 이후 신규 사건은 정상 LAGGING일 수 있다. CAUGHT_UP은 과거 legacy COMPLETE와 다르다.
- push204는 개별 사건 증거, feed ACK는 연속 원사건 적용 증거, snapshot baseline은 상태 증거다. 이 세 가지를 섞어 과거 eventId 수신 증거를 생성하지 않는다.
- coverage는 legacy COMPLETE/PARTIAL/UNKNOWN, stream UNKNOWN/CAUGHT_UP/LAGGING/GAP, nullable throughSequence와 assessedAt. 미적용 null과 적용된 "0"을 구분한다. COMPLETE는 별도 authoritative history 증명과 gap 부재가 있을 때만 허용한다.
- 최초 출시에서는 legacy COMPLETE 달성 자체를 완료/출시 gate로 요구하지 않는다(§3.1). 증거 부족을 정직하게 반환하는 테스트는 유지하며, 기준점 이후 연속성·복원 검증은 생략하지 않는다.

### 5.7 TMI-202 cleanup 연결과 보존

[WithdrawalSourceCleanup.Evidence](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalcapture/WithdrawalSourceCleanup.java)를 구현한다. `recordExpiry`는 호출자의 Mongo transaction 안에서만 수행하며 HTTP를 호출하지 않는다.

1. capture timing bound/clock 신뢰·현재 source/control/consumer generation을 검증한다. 근거가 없으면 삭제를 중단하고 경보하며 무조건 허용 adapter를 넣지 않는다.
2. 만료 row가 연속 prefix이면 replayFloor를 전진시킨다. capturedAt 순서가 sequence와 달라 중간만 만료되면 unavailable interval로 기록하고 floor가 남은 row를 건너뛰지 않게 한다.
3. consumer별 baseline/feed/push 증거와 replay availability를 구분한다. 정상적으로 적용한 증거를 원천 만료 때문에 무효화하지 않는다. 미적용 가능 구간은 GAP로 기록한다. raw event/userId를 gap 기록에 복제하지 않는다.
4. 위 증거와 journal/delivery 삭제는 같은 transaction이다. 세대 변경·ACK·claim과 충돌하면 전체 rollback/retry하고 부분 삭제를 남기지 않는다. 현재 유효 IN_FLIGHT lease를 건너뛰는 동작을 보존한다.
5. intervals/gaps는 병합·상한을 관리한다. 증거가 너무 커지면 보존 무한 연장이나 조용한 누락으로 처리하지 않고 경보·승인된 대안이 필요하다.

보존 정리는 business TTL을 추가하지 않고 명시 bounded job과 dry-run/apply로 한다. snapshot builder/page/ACK와 cleanup도 run/control CAS로 경합시켜 삭제 직후 READY/유효 ACK가 새로 확정되지 못하게 한다.

| 대상 | 보존·정리 기준 |
| --- | --- |
| journal 및 관련 delivery | capturedAt+120일. 재시도/재개로 기산점 변경 금지, 승인 예외 없는 원문 영구 보존 금지 |
| snapshot rows/manifest의 digest/staging/cursor 매핑 | run 시작+7일 이내 제거. retry/READY 시각으로 연장 금지 |
| snapshot/recovery operation 최소 metadata | 생성+120일, 요청 멱등 결과만 보존. rows/digest 복제 금지 |
| 현 stream 최소 checkpoint/baseline | 운영 중 필요한 최소 증거 유지. User/event 목록 없이 보관 |
| 폐기 stream metadata | 종료+120일. snapshot7일/원천120일 규칙을 덮어쓰지 않음 |

Identity User tombstone 자체 수명이나 Billing 결제 보존/15일 삭제 정책은 변경하지 않는다. source restore는 TMI-202 barrier를 거쳐 새 stream/sequence0·새 generation, Billing-only restore는 같은 source의 새 generation이다. cross-stream 순번 비교·ACK 복사 금지.

### 5.8 운영·활성화

신규 복구 API/builder/metadata cleanup은 분리된 기본OFF 설정을 제안한다. 이름은 구현 시 문서화하며 기존 WITHDRAWAL_CAPTURE_REQUIRED/BILLING_WITHDRAWAL_PUBLISHER_ENABLED/WITHDRAWAL_SOURCE_CLEANUP_ENABLED 의미를 바꾸지 않는다.

- OFF 배포 → 명시 index 진단/준비 → runtime/ingress 검증 → 승인된 generation/snapshot/feed 연동 → cleanup dry-run → 개별 승인 후 apply 순서다.
- capture cutover 후 capture OFF 또는 미지원 구버전 rollback은 계속 금지한다. 복구 API OFF가 원천 capture OFF를 뜻하지 않는다.
- 새 index6개는 manifest 이름/key 순서/unique/partial/TTL 없음/simple collation/sparse=false를 검증한다. 기존 index drop/recreate 및 startup 자동 DDL은 하지 않는다.
- 로그는 correlationId·정규 오류코드·단계·건수·소요시간만 사용한다. userId/event payload/token/cursor/서명/Secret을 남기지 않는다. high-cardinality ID를 metric label로 쓰지 않는다.
- 지표: snapshot build 지연/실패·history loss, old generation 거절, feed gap/만료, checkpoint lag, cleanup 실패/기한 임박. 경보 수신처·담당·대응 runbook은 staging gate다.
- TMI-202 RuntimeEvidence/관리 권한·감사 도구는 I2 운영 후속, I3 cleanup 증거는 본 작업, PurchaseActivationEvidence는 TMI-200/203/공동 gate다. I3만 완료하고 구매 허용 증거를 true로 바꾸지 않는다.

## 6. 부록 — exact 계약·검증·근거

### 6.1 API와 보안 범위

모두 POST, prefix `/internal/v1/billing/withdrawals`. 직접 internal DTO로 반환하며 public BaseResponse를 사용하지 않는다. 필드/nullable/상태별 생략의 전체 기준은 [exact §5](billing-c0-exact-wire-index-proposal-2026-10-07.md#5-api-필드-규격)다. 기본 Jackson/public API 설정을 전역 변경하지 않는다.

| suffix | 요청 | 응답·특이사항 |
| --- | --- | --- |
| /recoveries | streamId,expectedGeneration(null 허용 키 필수),reason; Idempotency-Key |200 generation/RECOVERING; 별도 운영 role, generation header 예외 |
| /snapshots | body 없음; Idempotency-Key |202 BUILDING/Retry-After5, 재호출 완료 상태200 |
| /snapshots/status | snapshotId |200 status, READY만 manifest/FAILED만 failureCode |
| /snapshots/page | snapshotId,cursor(키 필수 nullable) |200 items,nextCursor(nullable),done |
| /feed/page | streamId,afterSequence,scanCursor(키 필수 nullable) |200 items,scanCursor,targetThroughSequence,nextAfterSequence,scannedThrough,done |
| /checkpoints | BASELINE/RESYNC: streamId,mode,throughSequence,snapshotId,contentDigest,expectedCheckpointVersion(nullable); FEED: streamId,mode,throughSequence,scanCursor |204. mode별 불필요 필드 금지 |
| /checkpoints/status | streamId |200 exists,generation/state/version/baseline/feed의 필수 nullable,coverage; generation header 예외 |

나머지 데이터 API는 `X-Consumer-Recovery-Generation` 필수. AWS_IAM/SigV4 principal·role·environment와 generation을 별개 검증한다. 일반 worker는 recoveries 불가. 직접 ingress를 차단하고 Lattice가 전달하는 신뢰 정보의 실제 형식을 검증하기 전 임의 헤더로 principal을 만들지 않는다. Spring 전용 chain은 이 prefix만 처리하며 기본 JWT chain으로 fallback하지 않는다.

추가 필드·중복 키·잘못된 타입·숫자 강제 변환 금지. request/response16KiB, page≤20, D0/D1은 canonical decimal string/BSON int64, T.seconds/increment는 U32 string/BSON Timestamp. 새 ID는 lowercase UUIDv4, 사용자 UUID는 v4로 제한하지 않는다.

오류 envelope: `code,message,retryable,correlationId` 필수.400 INVALID_REQUEST,422 UNSUPPORTED_CONTRACT,409 SNAPSHOT_NOT_READY/COVERAGE_GAP/CHECKPOINT_CONFLICT/RECOVERY_GENERATION_MISMATCH/RECOVERY_NOT_READY/STREAM_REPLACED,410 SNAPSHOT_EXPIRED/CURSOR_EXPIRED/REPLAY_UNAVAILABLE, 일시 저장소 장애503. 인증401/403은 자동 반복하지 않고 인프라 응답에 앱 JSON 형식을 가정하지 않는다. operation body 충돌 등 추가 세부 code가 필요하면 exact 원문과 양측 fixture를 먼저 맞춘다.

### 6.2 추가 index6개

[수락 manifest](../../src/test/resources/contracts/payment-lifecycle/v1/identity-indexes.json) 기준으로 아래 모두 unique/방향1/TTL 없음이다. 기존7개는 유지한다. 기본 `_id_`도 별도 필수다.

| collection | name | key 순서 |
| --- | --- | --- |
| withdrawal_snapshot_runs | ux_withdrawal_snapshot_operation | environment,consumer,consumerRecoveryGeneration,requestOperationId |
| withdrawal_snapshot_runs | ux_withdrawal_snapshot | snapshotId |
| withdrawal_snapshot_rows | ux_withdrawal_snapshot_user | snapshotId,userId |
| withdrawal_consumer_checkpoints | ux_withdrawal_consumer_checkpoint | streamId,consumer,consumerRecoveryGeneration |
| withdrawal_consumer_recovery_controls | ux_withdrawal_recovery_control | environment,streamId,consumer |
| withdrawal_recovery_operations | ux_withdrawal_recovery_operation | environment,consumer,streamId,operationId |

User scan/cleanup 쿼리 explain과 기존 `_id_`·collation을 진단한다. manifest에 없는 성능 index가 필요하면 근거와 additive 변경안을 먼저 제시한다. 수락13개만으로 모든 운영 쿼리 성능이 증명됐다고 하지 않는다.

### 6.3 테스트·완료 기준

| 묶음 | 필수 검증 | 증거 종류 |
| --- | --- | --- |
| exact/security | nullable vs 누락, 상태별 필드, 중복/추가 키,16KiB, D0/D1/U3239경계/overflow, role·직접 경로·세대 위조 | 단위/MVC + staging IAM |
| snapshot | H>0, 동일 timestamp, T 이후 과거시각 late commit,0건/20건 경계, 중복/역순, digest golden fixture | 단위 + 격리 Mongo |
| build 장애 | history 소실,5분 초과, 중복 worker, 재시작 동일 T 불가, seal 중 generation/stream 교체 | 단위 + 실제 driver/격리 Mongo |
| feed | 고정 H2/신규 추가, H2 도달 빈 page, 중간 누락/expiry interval, cursor 변조·타 consumer·만료·MAX | 단위/저장소 검증 |
| ACK | 응답 유실, 동일/낮은 정상 ACK, stale 낮은 ACK 거절, H/digest mismatch, baseline 전 FEED, expired manifest, 동시 version | 단위 + transaction 경합 |
| recovery | INITIAL/RESYNC 경쟁, 같은 key 재호출/다른 body, 이전 operation 응답, 세대 변경과 seal/scan/ACK/cleanup | 단위 + 격리 다중 transaction |
| 최초 출시 범위 | 구매 이력 없는 전제에서 legacy PARTIAL/UNKNOWN만으로 무한 복구 요구 금지, 기존 탈퇴 snapshot 포함, 기준점 이후 GAP/증거 소실은 차단·복구 유지 | 가짜 fixture + 양 서버 gate 검증 |
| retention | 중간 sequence expiry, floor 연속성, 적용완료 증거 유지, 새 세대 UNKNOWN, lease 보호,7일/120일 경계, 부분 cleanup 실패 | 단위 + transaction 경합 |
| restore | 정상 failover 동일 stream, source restore 새 stream, consumer-only 새 세대, 이전 cursor/ACK 거절 | I2/I3/I4 공동 격리·staging |
| 회귀 | LC4필드/기존2xx, JWT/Guest·회원/재발급, TMI-200 구매 fence와 capture fail-closed | ./gradlew clean test |

기본 자동 테스트는 repository/provider mock과 가짜 fixture로 작성한다. 별도 격리 Mongo 실험이 불가능하면 해당 gate를 미검증으로 남기고 전체 완료라고 하지 않는다. Billing 코드·실결제·운영 User 데이터는 이 작업에서 수정하지 않는다.

완료 보고는 구현/단위 검증, 실제 Mongo 검증, 공동 RPC/IAM 검증, 배포/활성화 승인을 분리한다. 남은 실험을 다음 이슈로 넘겼다는 이유만으로 TMI-203 완료 기준을 통과한 것으로 보고하지 않는다.

### 6.4 조사 근거와 이번 작업 결과

- 공식 Jira TMI-203의 범위·완료 조건·선행/후속을 읽었다. 이슈 상태는 조회 당시 해야 할 일, 쓰기 없음.
- [TMI-202 계획](TMI-202-withdrawal-capture-billing-delivery-plan.md), [결과](TMI-202-implementation-result.md), [TMI-200 계획](TMI-200-purchase-scope-control-fencing-plan.md)을 선행 계약으로 연결한다.
- [WithdrawalIndexes](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalcapture/WithdrawalIndexes.java): I2 index7개, 명시 준비·옵션 검증·자동 삭제 없음 확인.
- [WithdrawalCaptureConfiguration](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalcapture/WithdrawalCaptureConfiguration.java): RuntimeEvidence/PurchaseActivationEvidence port, cleanup Evidence 주입과 cutover OFF 차단 확인.
- [WithdrawalSourceCleanup](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalcapture/WithdrawalSourceCleanup.java): transaction 내 Evidence 호출 후 delivery/journal 삭제, valid lease 보호 확인.
- 변경 파일: 본 계획서, WORKLOG, CURRENT_STATE. 이번에는 제품 코드·외부 계약·fixture·설정 변경 없음.
- 검증: 문서 로컬 링크 존재와 git diff --check. 문서만 변경하므로 Gradle/실제 Mongo/IAM 호출 미실행. 운영 가능성 검증 결과로 해석하지 않는다.

### 6.5 Jira 댓글 초안 — 자동 등록하지 않음

TMI-203 계획서 작성. 변경 파일은 계획서·WORKLOG·CURRENT_STATE이며 제품 코드 변경 없음. 고정 T/H snapshot 실현 가능성 검증 → 세대 authority/strict API → snapshot → feed/checkpoint/보존 증거 연결 순서로 나눔. exact wire/index와 원천120일/snapshot7일 계약 유지. 문서 링크·diff 정적 검증, Gradle 미실행(문서 작업). 실제 Mongo history·IAM/직접 ingress·restore 경합과 TMI-202 운영 adapter는 미검증 gate로 유지. 배포·활성화·Jira 상태 변경 없음.

범위 개정 초안: 사용자 확인상 최초 결제 출시이며 기존 구매 계정·구매 연결 발급·실제 거래 없음. 과거 전체 탈퇴 이력의 legacy COMPLETE 증명만 최초 출시 필수 조건에서 제외. PARTIAL/UNKNOWN을 유지하고 기존 snapshot 자료를 임의 제외하지 않으며 기준점 이후 capture·feed·ACK·generation·복원 검증은 유지. 변경은 계획/기록3파일, 문서 정적 검증만 수행. 출시 기준점의 실제 확정·양 서버 gate 반영·운영 검증은 후속이며 실제 배포/판매 승인은 별도.
