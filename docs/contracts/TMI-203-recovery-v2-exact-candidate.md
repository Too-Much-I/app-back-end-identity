# Identity 회신·새 exact 후보 — 단일 진행 관리 복구 API

> 최신 결정: [통합 인계](TMI-203-Billing-consolidated-handoff-2026-10-12.md)에 따라 미구현 복구 API는 **기존 예정 v1 주소**로 최초 구현하고, source는 floor+별도 range 문서 방식으로 설계한다. 아래 v2 prefix/revision 이름·예상10개 index는 이전 후보 이력이다. 이 문서는 세부 DTO 검토 근거로 보존하며 새 명세 동결본이나 현재 URL 지침으로 사용하지 않는다.

- 작성일: 2026-10-12 / develop / TMI-203·204, 영향199·202·205·206.
- 상태: **Billing 검토 보완안을 반영한 Identity 기술 제안. 공동 동결·구현·운영 호환 검증 완료 아님.**
- 후보 명칭: recovery-v2-candidate-1. 이는 문서 식별자이며 확정된 제품/schema version이 아니다.
- 경로 후보: `/internal/v2/billing/withdrawals`. 기존 push 경로·event schemaVersion=1은 불변이다.
- 기준: [현행 계획](TMI-203-withdrawal-recovery-snapshot-feed-plan.md), [직전 exact 제안](TMI-203-single-authority-exact-amendment-response.md), [기존199 exact](billing-c0-exact-wire-index-proposal-2026-10-07.md).

## 1. 5줄 결론

1. Billing의 local 적용·진행 원자성,5개 후보 저장소·복원 책임 설명을 기술 방향으로 수용한다. 실제 구현/복원 검증을 통과했다는 뜻은 아니다.
2.503 details 생략,페이지 범위에 hole이 있으면 전체 오류,같은 key/new stream 충돌 우선순위를 수용한다.
3.7일 이후 동일 run은 EXPIRED 최소 DTO를120일 metadata 범위에서 반환하고 재생성하지 않는 안을 제안한다.
4. 논리 expiry도410의 근거로 인정하고,head 초과·MAX·응답 크기 초과 등 남은 경계는 아래 표로 명시한다.
5. 새 prefix·index 이름·DTO는 상대 수락 후보이며 기존199 fixture/hash·공동 계약·코드·배포를 이번에 변경하지 않는다.

## 2. 반드시 읽어야 하는 내용

### 2.1 수용·보완 구분

Billing의 다음 보완은 본 후보에 반영했다: snapshot 생성은 run을 쓰는 POST이며 순수 read-only가 아님,503에는 source details 생략,원격 ACK 없는 마지막 cursor의 용도 명시,baseline H와 원event(H,C] 증거 분리,동시 duplicate-key binding 대사,old run 보존 경계.

새로 선택한 후보는 다음과 같다: **페이지 최대 범위20개를 먼저 정하고 범위 안 최초 누락이면 전체 오류**,410 range는 알려진 원 interval 전체를 반환할 수 있음,논리 만료 row는 단일 sequence range 증거 인정,응답 생성 크기 실패는500 RESPONSE_LIMIT_EXCEEDED/retryable=false.

이 문서는 기존 계약의 자동 supersede가 아니다. Billing이 아래 후보의 차이를 수락한 뒤 exact revision·manifest·fixtures·활성 문서 본문을 함께 전환한다.

## 3. 동결·실행 전에 필요한 것

- 새 prefix와 이름 수락. 실제 route/build/client/IAM·index 존재 확인 후 명시 전환,구v1 가짜204/동일 이름 index 덮어쓰기 금지.
- source 전체 unavailable interval 저장 상한·정리/분할 설계. wire1구간만으로 서버 저장 문제가 해결되지 않는다. 이 저장 규격과 overflow 실패 동작은 아직 미동결이다.
- Billing204 retry/circuit 초기값·local 운영 설치 권한·독립 watchdog·205 증거 사용과206 실험은 상대 운영 설계에서 확정한다.
- 오류 고정 message literal·최종 schema 파일·fixture·manifest hash는 공동 수락 단계에서 생성/검증한다. 아래 code/status/retryable 제안과 실제 테스트 통과를 구분한다.

## 4. 주요 위험과 미확인

실배포/다른 branch API 존재 여부는 확인하지 않았다. 로컬203 검색 부재를 운영 미배포 증거로 사용하지 않는다. Mongo 동일 시점/페이지 읽기·IAM·복원도 미검증이다.

source 가용성과 Billing 적용은 다른 사실이다. head/H2까지 모두 재생 가능하거나 C 이하 원event를 모두 처리했다고 해석하지 않는다. gap을 새 H로 지우지 않고205는 baseline·실제 event 범위·unresolved gap·거래 증거를 함께 본다.

## 5. exact 후보 본문

### 5.1 공통 규칙·routes

모두 POST이며 prefix 후보는 `/internal/v2/billing/withdrawals`. IAM/SigV4·인증 consumer/environment·직접 ingress 차단 유지. remote generation header는 사용하지 않는다. caller epoch를 대신 보내지 않는다.

표에서 ?는 **키 필수/null 허용**,나머지는 필수/non-null. 상태별 생략은 별도 규칙이다. 추가/중복 키·trailing JSON·타입 coercion 금지,public BaseResponse 없음. 요청/응답 UTF8 byte≤16384,page≤20,redirect 금지. D0=canonical string0..Long.MAX_VALUE,D1=1..MAX,U32=0..4294967295 string. 새ID lowercase UUIDv4,사용자UUID는 버전 제한 없음. UTC Instant canonical과 기존 digest 규칙 유지.

| suffix | 요청 | 성공 |
| --- | --- | --- |
| /snapshots | {streamId},Idempotency-Key 필수 | 신규/BUILDING202 {snapshotId,status},Retry-After:5;그 외 동일 run200 status DTO |
| /snapshots/status | {snapshotId} |200 status DTO,BUILDING이면Retry-After:5 |
| /snapshots/page | {snapshotId,cursor?} |200 {items,nextCursor?,done} |
| /feed/page | {streamId,afterSequence:D0,scanCursor?} |200 {items,scanCursor,targetThroughSequence:D0,nextAfterSequence:D0,scannedThrough:D0,done,availability} |

/recoveries,/checkpoints,/checkpoints/status는 새 revision에 없다. 기존 push204 응답은 유지한다. last snapshot page는 nextCursor=null/done=true. feed 완료 page의 scanCursor는 non-null이며 H2 binding·TTL 내 중복 조회용이다. 원격 ACK 근거가 아니다.

snapshot status 공통은 snapshotId/status,READY만 manifest,FAILED만 failureCode. 다른 상태에서는 해당 키 생략. enum BUILDING/READY/FAILED/EXPIRED. failureCode는 기존 HISTORY_LOST/BUILD_TIMEOUT/BUILD_FAILED 계열의 full code를 유지한다: SNAPSHOT_HISTORY_LOST/SNAPSHOT_BUILD_TIMEOUT/SNAPSHOT_BUILD_FAILED.

manifest: snapshotId,streamId,T,H:D0,itemCount:D0,pageCount:D1,contentDigest,legacyCoverage,knownGaps,expiresAt. T={seconds:U32,increment:U32},digest lower64hex,legacy COMPLETE/PARTIAL/UNKNOWN. knownGaps는 기존 LEGACY_HISTORY/SOURCE_RANGE shape와 nullable ends 규칙 유지. consumer coverage 아님. 빈 snapshot도 pageCount="1". snapshot item={userId,withdrawnAt};feed item={sequence:D1,event:{eventId,schemaVersion:integer1,userId,withdrawnAt}}.

### 5.2 멱등·stream·7일/120일

처리 순서는 인증/strict decode → existing key binding 비교 → stream 검증 → 같은 run 재응답/신규 생성이다.

- unique(environment,consumer,requestOperationId),canonical request의 streamId와 고정 scope ALL_WITHDRAWN을 별도 binding. 같은 key/new stream은409 OPERATION_CONFLICT가 STREAM_REPLACED보다 먼저다.
- old key/old body가 일치하지만 stream이 inactive면409 STREAM_REPLACED. 새 stream으로 새 run을 만들려면 새 key.
- 동시 동일 요청의 unique 충돌은 기존 binding을 읽어 같은 snapshotId로 수렴. 다른 요청이면409. 결과 불명은 새 key로 우회하지 않는다.
- metadata의 createdAt은 최초 생성 시각,expiresAt=run 시작+7일,operation metadata 기한=최초 생성+120일. status/page 역시 owner/current stream을 확인한다.
-7일이 지나면 개인정보 rows/manifest/digest/cursor 매핑을 정리하고 EXPIRED 최소 DTO `{snapshotId,status:"EXPIRED"}`를 반환한다. 이전 FAILED도 논리 expiry 뒤에는 EXPIRED로 수렴하며 원 실패 코드는 필요 최소 작업 기록에서 관리할 수 있다.
- current stream의 같은 key/body 재생성 금지.120일 전까지 create/status는200 EXPIRED,page는410 SNAPSHOT_EXPIRED. 물리 cleanup이 늦어도 논리 기한 적용.
-120일 metadata가 사라진 snapshotId status/page는404 SNAPSHOT_NOT_FOUND. metadata가 없는 새 key 재사용을 영구 기억하지 않는다. Billing은 새 작업마다 새 UUID를 사용한다.
- inactive stream 검사는 남아 있는 operation/run에서 expiry보다 먼저. metadata 자체가 없으면404로 응답하고 사라진 stream을 추측하지 않는다.

### 5.3 feed 페이지 범위·검증 순서

auth/strict → 요청 stream/current 검증 → cursor 서명·owner/stream binding·expiry → 숫자 범위 → 일관 source 관측 → 페이지 연속성/expiry 검사 순서다. 잘못된 서명/binding은400 INVALID_REQUEST,유효하나 만료된 cursor는410 CURSOR_EXPIRED. 인프라 인증 실패는401/403.

첫 요청은 same-read committed head로H2를 고정한다. after>head는400 INVALID_REQUEST. 이후 startExclusive≤after≤H2,초과/미달은400. 같은 active stream의 관측 head가H2보다 낮으면 정상 처리하지 않고503 SOURCE_AVAILABILITY_UNCERTAIN·source 무결성 경보다. stream이 실제 교체됐으면409 STREAM_REPLACED.

검사 예정 page 끝 E=after+min(20,H2-after)로 계산해 overflow를 피한다. 조회 결과만 보고 끝을 정하지 않는다. (after,E] 모든 순번이 있어야 하며 **중간 hole/논리 expiry이면 이 요청 전체 오류,items 부분 성공 없음**. Billing C 무변경. 서버는 bounded prefix20개를 검사하므로 그 뒤 hole은 다음 요청에서 확인한다.

- after==H2는 빈 성공,E=H2,done=true. 이 경우 after+1을 계산하지 않는다.
- after<H2는 최소1건,최대20건. MAX_VALUE 요청도위 산식으로 안전하며 H2=MAX/after=MAX는 빈 완료.
- nextAfterSequence=scannedThrough=E,done=(E==H2). H2/cursor는 같은 scan에서 불변.
- row가 남아 있어도 observedAt≥capturedAt+120일이면410. 신뢰 가능한 row의 해당 sequence 한 개를 확정 expiry range로 인정한다. clock/timing 근거 불명은503이며 날짜를 임의 추정하지 않는다.

### 5.4 success availability

모든 키 필수/non-null: streamId,replayFloor:D0,observedThroughSequence:D0,checkedFromExclusive:D0,checkedThroughSequence:D0,observedAt:Instant.

```json
{"streamId":"00000000-0000-4000-8000-000000000001","replayFloor":"80","observedThroughSequence":"150","checkedFromExclusive":"100","checkedThroughSequence":"120","observedAt":"2026-10-12T00:00:00Z"}
```

checkedFromExclusive=after,checkedThroughSequence=E,after≤E≤H2≤observedThroughSequence,floor≤observedThroughSequence. 비어 있지 않은 검사 구간은 prefix floor/논리expiry/known hole에 포함되지 않아야 한다.모순이면 Billing은 C를 갱신하지 않는다.

이는 이번 페이지 범위의 관측 결과이며 미래 replay·head 전체 가용성·consumer 적용을 증명하지 않는다. source stream/counter/floor/interval/journal은 한 페이지의 같은 짧은 snapshot 읽기에서 관측한다. cleanup의 availability 갱신+삭제는 같은 transaction. 이미 읽은 페이지가 이후 만료되더라도 다음 요청에서 논리 expiry/가용성 검증을 새로 한다.

### 5.5 오류 variant·크기

앱 error 기본4키는 code/message/retryable/correlationId. message는 개인정보/exception 없는 code별 고정 문장으로 하고 client는 code만 분기한다. 정확 문장 fixture는 공동 동결 단계에서 작성한다.

| HTTP/code | retryable | details |
| --- | --- | --- |
|400 INVALID_REQUEST |false|생략 |
|404 SNAPSHOT_NOT_FOUND |false|생략 |
|413 REQUEST_TOO_LARGE |false|생략;서버가 요청 byte 상한 차단 시 |
|422 UNSUPPORTED_CONTRACT |false|생략 |
|409 OPERATION_CONFLICT / STREAM_REPLACED |false|생략 |
|409 SNAPSHOT_NOT_READY |true|생략;status 재조회 |
|409 COVERAGE_GAP |false|아래6키,unavailableRange 필수 null |
|410 REPLAY_UNAVAILABLE |false|아래6키,unavailableRange non-null |
|410 SNAPSHOT_EXPIRED / CURSOR_EXPIRED |false|생략;동일 요청 반복 대신 새 작업/scan 판단 |
|503 SOURCE_AVAILABILITY_UNCERTAIN |true|항상 생략 |
|503 SOURCE_UNAVAILABLE |true|생략;일반 저장소 일시 장애 후보code |
|500 RESPONSE_LIMIT_EXCEEDED |false|생략;서버 응답 생성/설계 오류,담당 경보 |

401/403 인프라 응답은 앱 JSON을 가정하지 않는다.429 제한 응답·Retry-After의 구체 정책은 공통 서비스 throttling 규격과 맞춰 최종 exact에서 동결할 항목으로 남긴다.

details의6키: streamId,replayFloor:D0,observedThroughSequence:D0,missingSequence:D1,unavailableRange?,observedAt. missingSequence는 검사 page의 최초 누락/만료 위치이며 요청에 실제 필요해야 한다.

```json
{"streamId":"00000000-0000-4000-8000-000000000001","replayFloor":"80","observedThroughSequence":"150","missingSequence":"101","unavailableRange":{"fromSequence":"101","toSequence":"130"},"observedAt":"2026-10-12T00:00:00Z"}
```

410 range는 알려진 interval 전체로서 H2/page 끝보다 클 수 있으나 관측head 이하,from≤missing≤to. prefix라면[1,floor],논리 만료 row만 증거면[missing,missing]. Billing은 자신의 필요한 범위와 교차하며 다른 구간 정상성을 추론하지 않는다.409는 동일 일관 읽기에서 확정한 hole이나 expiry range 근거 없음. 판정 불명은503/details 없음.

응답은 직렬화 전16384byte 예산을 검증한다. 정상 fixed field/event로20개를 담도록 설계하되metadata+items가 넘으면500 RESPONSE_LIMIT_EXCEEDED;부분 JSON/빈 성공/413 오분류 금지. 실패 응답은 작은 고정 크기다. snapshot READY manifest overflow는READY를 만들지 않고BUILD_FAILED 처리한다. request 크기 오류와 서버 생성 크기 오류를 구분한다.

### 5.6 index manifest 후보 표

기존13개 manifest/hash는 보존,새 revision 별도. 아래10개는 선언 후보이며 실제 환경 생성/검증 결과가 아니다. keys는 표 순서·모두1. 기본 _id_ 별도,모두TTL 없음/sparse=false/simple collation. partial은 active stream 하나만 status=ACTIVE,나머지 없음.

| collection | name | keys 순서 | unique |
| --- | --- | --- | --- |
|withdrawal_stream|ux_withdrawal_stream|environment,streamId|true|
|withdrawal_stream|ux_withdrawal_active_stream|environment|true|
|withdrawal_journal|ux_withdrawal_sequence|streamId,sequence|true|
|withdrawal_journal|ux_withdrawal_event|eventId|true|
|withdrawal_journal|ix_withdrawal_capture_expiry|capturedAt,_id|false|
|withdrawal_deliveries|ux_withdrawal_destination|eventId,destination|true|
|withdrawal_deliveries|ix_withdrawal_due|destination,status,nextAttemptAt,_id|false|
|withdrawal_snapshot_runs|ux_withdrawal_snapshot_operation_v2|environment,consumer,requestOperationId|true|
|withdrawal_snapshot_runs|ux_withdrawal_snapshot|snapshotId|true|
|withdrawal_snapshot_rows|ux_withdrawal_snapshot_user|snapshotId,userId|true|

I2의7개 재사용,snapshot operation만 새 이름/새 key. remote3개는 새 manifest 제외지만 운영 drop 승인이 아니다. 동일컬렉션 구index/validator가 새 문서와 충돌할 수 있으므로 존재·중복·schema/collation 진단 후 additive migration을 설계한다. startup 자동 DDL 금지. 전체 interval 저장 모델에 추가 index가 필요하면 해당 범위도 별도 동결한다.

## 6. 영향·검증·회신 요청

### 6.1 수락 후 문서 개정

Identity203 계획의 후보 prefix/body/index/availability를 이 revision으로 연결하고202 cleanup port는 consumer remote 증거가 아닌 source availability 원자성으로 개정한다. 과거202 구현 결과를 소급해서 새계약을 검증한 것처럼 바꾸지 않는다.

Billing은 공동 계약 §5.2/5.3/5.7,204/205·ADR/결정서·206 운영을 같은 의미로 갱신한다. C는 baseline H와 그 뒤(H,C] 적용 증거를 구분하며snapshot/CAUGHT_UP만으로삭제 허가하지 않는다.5개 후보 collection은 Billing 내부이며 새 wire에 노출하지 않는다.

기존199 fixture는 유지하고새 revision에 create conflict/concurrency·7일/120일·stream 교체,availability 순번/빈범위/MAX·hole 전체오류·논리 expiry·410/409/503·크기초과·index명/옵션 fixture를 추가해야 한다. 이번에는 fixture를 생성하거나 실행하지 않았다.

### 6.2 이번 확인과 남은 것

첨부 Billing 회신 전체와 기존 Identity13개 index manifest를 대조했다. 문서의 로컬 링크·JSON 예시·diff 정적 검사만 수행,Gradle/실Mongo/IAM/배포 확인 미실행. 제품코드/Jira/공동 exact/기존 fixture 변경 없음.

Billing 회신 요청: 위페이지20범위 전체오류·410 full interval/singleton·7일 EXPIRED/120일404·오류표·operation index명을수락/보완해 달라. source interval 저장 구조·429·고정 message·호환배포 확인 등 남은 항목까지 동결한 뒤 신규 exact/fixture를 생성한다. 아직 양측 동결 완료라고 보고하지 않는다.

Jira 댓글 초안(미등록): Billing 보완 검토 반영한 recovery-v2 exact 후보 작성.503 details 생략/페이지hole전체오류/멱등보존·availability·index10개·오류경계 명시.문서 정적 검증만,실배포/interval저장/최종fixture 미완료.구현·Jira·배포 변경 없음.
