# TMI-202 구현 결과 — OFF 코드와 운영 준비 경계

- 날짜: 2026-10-12 / 브랜치: `feat/TMI-202-withdrawal-journal-billing-delivery`
- Jira: [TMI-202](https://to-teacher.atlassian.net/browse/TMI-202)
- 기준: [계획](TMI-202-withdrawal-capture-billing-delivery-plan.md), [TMI-199 exact 수락](billing-c0-exact-wire-index-proposal-2026-10-07.md), [TMI-200 결과](TMI-200-implementation-result.md).
- 상태: OFF 구현·로컬 검증. 실제 다중노드 Mongo 완료 조건/운영 증거 adapter/관리 실행기 연결은 미완료. Jira Done·배포·활성화 아님.

## 1. 5줄 결론

1. 기존 탈퇴 transaction에 stream counter·journal·Billing PENDING delivery를 함께 저장하는 경계를 추가했다.
2. Billing 전용 publisher는 SigV4/204-only·20회 dispatch budget·lease/generation CAS·정확한 Retry-After 분류를 사용한다.
3. 권한 있는 내부 재개, additive7개 index 준비/검증, cutover/restore 내부 명령,120일 원천 정리의 I3 evidence 연결 지점을 추가했다.
4. 운영 준비 증거와 관리 권한을 대신하는 true/no-op adapter는 없다. 관련 연동 전 capture/구매/cleanup ON이 차단된다.
5. 테스트 결과는 §6에 기록한다. Mock/Spring transaction 테스트를 실제 Mongo failover·성공 commit 응답유실의 완료 근거로 대체하지 않는다.

## 2. 사용자가 반드시 읽어야 하는 내용

### 지금 가능한 것과 아직 안 되는 것

코드·단위/회귀 테스트를 검토하고 기본 OFF로 배포 준비할 수 있다. **이번 결과만으로 TMI-202 전체 완료 또는 운영 ON을 선언할 수 없다.** 필수 미완료 사항은 §4에 있다.

환경변수만 true로 바꾸면 바로 동작하는 상태가 아니다. `RuntimeEvidence` 구현이 실제 writer drain/cutover·clock 검증 결과를 제공해야 capture가 시작된다. 수동 재개 권한·감사 adapter, 승인된 관리 실행기, I3 정리 증거 adapter도 별도 연동해야 한다. 현재 기본 동작은 거절이며, 테스트용 mock을 운영에 넣어 우회하면 안 된다.

탈퇴 저장 오류에는 신규503 `WITHDRAWAL_CAPTURE_UNAVAILABLE`를 사용한다. **이미 commit됐지만 응답이 유실된 경우도 있으므로 rollback 확정이 아니다.** unknown 결과를 새 eventId로 재시도하지 않는다. 현재 인증으로 접근 가능한 멱등 탈퇴 경로는 기존 방식대로 현재 상태를 반환한다. 인증이 폐기된 사용자를 위해 임의 userId 조회 API를 열지 않는다.

## 3. 사용자가 결정해야 하는 사항

- 기존 제품 정책·보존기간·LC/Billing wire는 변경하지 않았다. 추가 정책 선택은 없다.
- 관리 권한/감사 연결 방식과 준비 증거의 운영 공급자를 정하고 구현해야 한다. 일반 앱 API·관리 화면을 새로 공개하지 않았다.
- 실제 index 생성/cutover, 격리 다중노드 Mongo 실험, IAM·배포·publisher ON·cleanup 적용은 별도 승인한다.
- commit/push는 사용자 수행. Jira 댓글은 §6.4 초안만 보관하며 상태 변경하지 않았다.

## 4. 남은 위험·미완료 gate

1. **TMI-202의 실제 Mongo 완료 조건 미검증:** failover·성공 commit 응답유실·counter 직렬화·unique partial ACTIVE stream 교체·동시 cleanup/claim 등은 실제 replica-set에서 실행하지 않았다. Mock 결과는 이를 보장하지 않는다.
2. **운영 증거 adapter 미구현:** `WithdrawalCaptureConfiguration.RuntimeEvidence`는 clock 신뢰와 capture 준비 증거 포트다. boolean 환경변수로 대체하지 않는다. 구매 연결에는 별도 `PurchaseActivationEvidence.verifiedFor(streamId)`가 control 이관/old writer 배제/I3·공동 token gate까지 검증해야 한다.
3. **관리 실행기/권한 연동 미완료:** `WithdrawalOperations.Authority`는 거절 기본값이다. resume 감사는 같은 Mongo transaction에서 durable하게 저장해야 한다. 이전 재개 operation의 전역 재사용 방지/최소 감사 보존도 이 adapter의 운영 계약에 포함한다. `WithdrawalAdministration.Authority` 역시 권한·writer barrier·감사 구현을 요구한다. 실행 가능한 shell 명령이나 공개 endpoint는 제공하지 않는다.
4. **I3 정리 연동 미완료:** `WithdrawalSourceCleanup.Evidence.recordExpiry`가 같은 transaction에서 source/consumer generation CAS, 관측 budget 확인, replayFloor/중간 unavailable interval/coverage gap을 기록해야 한다. 기본 adapter/자동 삭제 scheduler 없음. 이 부분은 interface만 존재하며 floor/gap 계산 완료라고 주장하지 않는다.
5. **commit 시간 상한:** capture는 전후30초 관측 검사, 동일 commit 최대3회와 서버 maxCommitTime5초/transaction timeout30초를 사용한다. 드라이버 연결·서버 선택 시간까지 포함한 절대 네트워크 상한을 증명한 것은 아니다. 실제 지연/오차를 실측하고 bound 불명 원천의 정리를 막는 증거가 필요하다.
6. **운영 DB 부작용:** 비-test startup에서 durable stream 존재를 읽어 capture OFF rollback을 거절한다. 기존 배포에서도 추가 읽기 권한이 필요하다. capture ON은 snapshot/majority+j 및 bounded commit manager를 공유하므로 다른 인증 transaction의 성능·timeout도 staging에서 검증한다. purchase-only/OFF manager 의미는 유지한다.
7. **경보:** overdue24h/만료7일/review due·실패 metrics는 추가했지만 외부 알림 수신처는 연결하지 않았다. 관측 aggregation 비용과 claim의 만료 lease 조회 효율을 실제 데이터로 측정해야 한다.
8. **rollback:** durable marker 검사만으로 capture 미지원 구버전 프로세스를 통제할 수 없다. 배포/DB 접근 차단이 필요하다. rollback 시 publisher/신규 purchase만 OFF, capture/fence/원천 기록은 유지한다.

## 5. 구현 내용·운영 체크리스트

### 5.1 원천과 탈퇴

- `WithdrawalRecords`의 Stream/Journal/Delivery3개 collection 추가. 원천은 userId/eventId/탈퇴·관측 시각 등 최소 사실이며 문자열 출력은 식별자를 가린다. 원문 payload 복제 없음.
- `WithdrawalCaptureService`: caller transaction을 강제하고 ACTIVE stream counter를 int64 overflow 조건으로 증가시킨 뒤 journal/delivery insert. publisher OFF에도 PENDING 저장. 동일 LC eventId/withdrawnAt 재사용.
- `UserWithdrawalService`: capture 활성 요청의 eventId를 loop 밖에서 한 번 생성한다. 확정 transient 오류는 최대3회, unknown commit은 전체 작업 재시도 금지. 기존 OFF 경로는 기존2회 충돌 정책 유지. 기존 탈퇴 로그의 userId/withdrawnAt도 제거했다.
- `BoundedMongoTransactionManager`: capture ON에서만 적용. 동일 commit의 UNKNOWN label만 최대3회 재시도, 전체 작업·순번을 재할당하지 않는다. TMI-200 purchase-only transaction 옵션은 그대로 유지한다.

### 5.2 Billing 전송

- 고정 route `POST /internal/v1/payments/accounts/withdrawal/events`, 기존4필드, UTC Instant 문자열/4KiB outbound 유지. schemaVersion JSON integer1.
- 기존 SigV4 transport에 `postDetailed`를 추가해 별도 Retry-After header values를 보존한다. 기존 `post`의1~300초 의미 및 LC/JWT publisher는 유지한다. redirect NEVER, connect3초/request5초.
- poll5초·최대20건을 순차 처리하며 각 건 전송 직전에 claim한다. lease60초, 모든 결과 쓰기에 leaseOwner/version/generation/유효시각 CAS.
- dispatch 직전 시도 횟수 두 개와 marker를 원자 저장한다. 기대 횟수도 CAS하여 동일 marker 쓰기의 중복 증가를 차단한다. marker 결과 불명이면 HTTP를 보내지 않는다. crash/응답 유실은 소비한 횟수를 되돌리지 않는다.
-204만 완료. 다른2xx/3xx/영구4xx는 dead-letter,401/403·credential 획득 실패는 auth block. 일시 오류/protocol 오류는 최대20회.20번째204 성공,20번째 retryable 실패 중단. 유효 Retry-After notBefore는 마지막 실패에도 보존한다.
- RFC HTTP-date/정수0/긴 미래값·중복 header·overflow·clock 불신 분류. header/body/식별자는 로그·metrics label에 넣지 않는다.

### 5.3 재개·정리·관측

- `WithdrawalOperations.resume`: 내부 service만 제공. 권한 확인, 원천·active stream·만료/notBefore·expected generation 검사 후 재개 generation 증가. lifetime 및 기존 failure/review 이력 유지. 최근 동일 operation/body 중복은 NOOP, 다른 binding은 거절. 감사 실패는 transaction rollback.
- `WithdrawalSourceCleanup.batch`: 최대100건 dry-run/apply. apply는 별도 권한과 I3 증거 필요. 유효 in-flight lease는 건너뛰며, 원천/전송 참조 삭제는 증거 기록과 같은 transaction. source120일, 새 event 생성/자동 보존 연장 없음. 자동 실행 없음.
- `WithdrawalMonitor`: 미전달24시간·원천 만료7일·검토 기한 경과 수치만 노출. 조회 실패 시 이전 값을 유지하고 별도 실패 counter를 증가시켜 false zero를 피한다.
- 신규 collections에 자동 TTL 없음. 기존 LC outbox cleanup과 분리한다.

### 5.4 명시적 준비와 설정

| 설정 | 기본값 | 조건 |
| --- | --- | --- |
| `WITHDRAWAL_CAPTURE_REQUIRED` | false | 기존 control writer/session fence ON, 환경/7개 index/ACTIVE stream/RuntimeEvidence 필요 |
| `BILLING_WITHDRAWAL_PUBLISHER_ENABLED` | false | capture ON, RuntimeEvidence, 승인된 Lattice origin·region·IAM |
| `WITHDRAWAL_SOURCE_CLEANUP_ENABLED` | false | capture ON, I3 Evidence 구현 필요. bean 설치이며 자동 삭제 시작 의미 아님 |
| `WITHDRAWAL_ENVIRONMENT` | 빈 값 | 환경별 DB 분리, 활성화 시 소문자/숫자/hyphen 이름 |
| `BILLING_WITHDRAWAL_ORIGIN` / `BILLING_WITHDRAWAL_REGION` | 빈 값 | 승인된 HTTPS Lattice origin/region. 실제 값은 이 문서에 기록하지 않음 |

`WithdrawalIndexes.prepare(authority)`는 명시적 승인 DDL이고 일반 startup에서는 verify만 한다. 수락 manifest의 I2 7개만 생성하며 기존 mismatch는 거절한다. drop/recreate 없음. `WithdrawalAdministration.cutover`는 별도 권한·writer barrier 증거·index 검증 뒤 새 UUIDv4 stream을 시작한다. 복원은 기대 old stream을 CLOSED_RESTORE로 바꾸고 새 stream을 만든다. 이미 존재하는 stream ID 재요청은 자동 재설치하지 않고 이전 결과 확인을 요구한다.

일반 startup에서 새 stream을 생성하지 않는다. 테스트 profile을 운영에 사용하지 않는다. 운영 adapter를 준비한 독립 관리 실행기로 index/cutover를 먼저 수행해야 하며, 여기의 Java 내부 메서드를 불특정 사용자 API로 노출하지 않는다.

배포 전 순서: 코드 검토 → 운영 adapter 구현 → 실제 Mongo 실험 → index/환경/IAM 진단 → 승인된 writer barrier/cutover → capture 검증 → publisher 검증/승인 → I3·token 공동 gate 이후 purchase 판단. cleanup은 별도 증거/승인이다.

## 6. 부록 — 파일·검증·Jira 초안

### 6.1 변경 파일

- 신규: `domain/user/withdrawalcapture/`의 WithdrawalRecords, WithdrawalCaptureService, WithdrawalDeliveryStore, BillingWithdrawalRetry, BillingWithdrawalPublisher, WithdrawalOperations, WithdrawalSourceCleanup, WithdrawalIndexes, WithdrawalAdministration, WithdrawalCaptureProperties, WithdrawalCaptureConfiguration, WithdrawalMonitor.
- 신규: `global/config/BoundedMongoTransactionManager.java`.
- 변경: UserWithdrawalService/TransactionService, UserWithdrawnOutbox factory, UserErrorStatus, UserController, MongoTransactionConfig, BillingSigV4JsonTransport, application.yml.
- 테스트: `domain/user/withdrawalcapture/` 테스트/지원 클래스, BoundedMongoTransactionManagerTests, 기존 UserWithdrawalServiceTests/TransactionServiceTests, MongoIndexManifestTests(신규3개 collection/명시7개 index 반영).
- 문서: 본 결과, 계획 상태 링크, WORKLOG append/CURRENT_STATE 최신 항목. 기존 다른 작업의 dirty 기록은 보존.

Java 기준 경로는 `src/main/java/web/tosunsaeng/identity/`다. 요청 외 제품 변경 없음. 직접 관계가 있는 기존 탈퇴 로그의 개인정보 제거와 programmatic index export 확장은 이번 범위에 포함했다.

### 6.2 검증 결과

- 최종 `./gradlew clean test`: BUILD SUCCESSFUL(25초), 1,475건 중1,469통과/6skipped, 실패0/오류0. 신규 suite68건 및 기존 탈퇴 테스트3건 추가. scheduler 시작 전 초기화 검사 보완 후 전체 재실행 결과다. 문서 링크·git diff --check 통과.
- 신규 테스트는 repo/provider Mock·Spring transaction callback·BSON mapper를 사용한다. 실제 외부 OAuth/Atlas/Lattice 호출 없음.
- 확인 범위: 원천 insert 실패 시 기존 탈퇴 rollback, LC와 이벤트 공유, int64/streamId 영속 mapping, no-upsert/overflow 조건, 상태별 retry/date/중복 헤더, marker/lease CAS, source expiry, 재개 권한·중복 binding, cleanup evidence 거절/in-flight 보호, OFF config/marker rollback guard, commit unknown 재시도/전체 작업 재시도 금지, 개인정보 없는 metrics.
- 중간 테스트의 wildcard 상수 충돌·누락 import·raw enum BSON JSON 직렬화 검사 오류를 수정했다. 전체 테스트에서는 신규 collection 때문에 manifest의 기대값31→34 및 I2 명시 index 포함을 갱신했다. 위 실패를 숨기거나 테스트를 skip으로 바꾸지 않았다.

### 6.3 외부 계약

기존 RS256/kid/sub/audience/read, LC workload/2xx/cleanup, 기존 Billing4필드 wire 유지. 새 request body userId나 public 관리 API 없음.503 WITHDRAWAL_CAPTURE_UNAVAILABLE 및 Swagger 설명 추가. 정상 탈퇴 응답 구조 유지, DB 실패503은 rollback 확정이 아님.

### 6.4 Jira 댓글 초안 — 미등록

TMI-202 OFF 코드 구현: 원자 탈퇴 capture/stream counter/journal·Billing delivery, SigV4/204-only/Retry-After/20회 dispatch·lease CAS, 권한 재개·index/cutover 내부 명령·I3 cleanup evidence 기반 추가. 변경 파일은 withdrawalcapture 패키지, 탈퇴/transport/Mongo 설정·테스트·문서. 실제 다중노드 Mongo/IAM·운영 준비/관리 adapter·I3 증거는 미완료이며 활성화/Jira Done 근거로 사용하지 않음. 최종 자동 테스트 수치는 WORKLOG에 기록. 배포·DB 이관·Jira 쓰기·commit/push 없음.
