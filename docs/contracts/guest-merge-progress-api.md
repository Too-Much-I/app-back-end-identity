# Guest 병합 진행 조회 — 구현 계약 및 운영 절차

## 1. 5줄 결론

1. `POST /api/v1/auth/firebase/guest/merge`는 기존 토큰 응답에 `mergeId`를 추가한다. signup/upgrade 응답은 변경하지 않는다.
2. 회원의 `GET /api/v1/users/me/merges/{mergeId}` 및 `GET /api/v1/users/me/merges`로 필수 서비스 완료 확인 상태를 조회한다.
3. 첫 출시는 `LEARNING_CORE_ONLY`: LC만 전송하고 Billing은 `NOT_REQUIRED`. 이후 설정 변경으로 과거 작업을 재실행하지 않는다.
4. 추적 이벤트는 **commit 후 204**만 완료 근거로 인정한다. 동일 eventId/payload 재전송으로 응답 유실을 복구한다.
5. 저장소 구현과 외부 배포 검증은 별개다. 기본 OFF이며 Mongo replica set 및 실제 LC의 commit/멱등 204 검증 후 켠다.

근거: [조회 서비스](../../src/main/java/web/tosunsaeng/identity/domain/auth/mergeprogress/UserMergeQueryService.java), [병합 트랜잭션](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseGuestMergeTransactionService.java), [publisher](../../src/main/java/web/tosunsaeng/identity/domain/auth/ownerevent/application/OwnerEventPublisher.java).

## 2. 반드시 읽어야 하는 내용

### 프론트 계약

병합 성공은 Identity 사용자/세션/event/progress 저장 확정이지, LC 기록 이전 완료가 아니다.
응답의 회원 토큰으로 기존 Guest 토큰을 교체하고 `mergeId`를 보관한다.
응답을 잃었다면 기존 SNS exchange로 회원 토큰을 얻고 목록을 조회한다. Guest merge 재호출로 복구하지 않는다.
승격은 userId가 유지되므로 이 작업을 생성하지 않는다.

단건 result 예시:

```json
{
  "mergeId": "11111111-1111-4111-8111-111111111111",
  "status": "COMPLETED",
  "createdAt": "2026-10-03T00:00:00Z",
  "completedAt": "2026-10-03T00:00:03Z",
  "learningCore": {"status": "COMPLETED", "confirmedAt": "2026-10-03T00:00:03Z"},
  "billing": {"status": "NOT_REQUIRED", "confirmedAt": null},
  "nextPollAfterSeconds": null
}
```

기존 `BaseResponse` 안의 `result`다. 토큰은 응답 예시에 실제 값을 사용하지 않는다.

- 전체 상태: `PROCESSING`(필수 확인 대기), `ACTION_REQUIRED`(운영 확인), `COMPLETED`(필수 확인 모두 저장).
- 서비스별 상태: `PENDING`, `ACTION_REQUIRED`, `COMPLETED`, `NOT_REQUIRED`.
- `NOT_REQUIRED`는 생성 시 대상 제외이며 장애/권한 부여/무료 이용권의 의미가 아니다.
- `confirmedAt/completedAt`은 Identity 확인 저장 시각이다. 원격 DB의 정확한 commit 시각이 아니다.
- `ACTION_REQUIRED`는 회원 로그인 취소나 병합 rollback을 뜻하지 않는다. 운영 복구 후 진행될 수 있다.
- 완료된 서비스 상태는 회귀하지 않는다. Billing ON 이후에도 예전 LC-only 작업은 그대로다.
- capture OFF/legacy 병합은 `mergeId=null`이다. 미추적을 완료로 해석하지 않는다.
- 새 요약은 완료 후 30일 보관한다. 과거 event의 PUBLISHED에서 자동 생성하지 않는다.

목록: `?activeOnly=true&limit=20&cursor=...`

- activeOnly 기본 true, false면 보관 중 완료 포함. limit 1~50, 기본 20.
- `createdAt DESC, _id DESC` keyset 정렬. result는 `{items:[단건 result],nextCursor:null|opaque-string}`.
- 커서는 서버가 저장한 임의 UUID로 사용자/필터에 바인딩하며 1시간 만료. 서명 키 환경변수는 추가하지 않는다.
- 한 회원에 여러 Guest 작업이 있을 수 있다. 최신 작업이 현재 기기 작업이라고 단정하지 않는다.
- 빈 목록/404는 완료 증거가 아니다. 미추적·다른 소유자·보관 만료가 포함된다.

### 조회 권한·오류

JWT `account_type=MEMBER`와 현재 DB `ACTIVE MEMBER`를 둘 다 확인한다.
조회 조건의 target은 JWT sub에서 얻으며 외부 userId를 받지 않는다.
다른 소유자/미존재/논리 만료는 동일 404. Guest·탈퇴·정지 계정은 차단한다.
GET마다 downstream 서버를 호출하지 않는다. 모든 응답에 no-store를 적용한다.

| HTTP | code | 프론트 처리 |
| --- | --- | --- |
| 400 | INVALID_MERGE_STATUS_REQUEST | UUID/limit/boolean/cursor 형식·만료·바인딩 확인 |
| 401 | COMMON_UNAUTHORIZED 등 기존 인증 오류 | 회원 인증 복구 |
| 403 | ACCOUNT_NOT_ACTIVE 또는 선행 보안 오류 | 계정 상태 안내, 반복 중단 |
| 404 | MERGE_STATUS_NOT_FOUND | 완료 추측 금지 |
| 429 | MERGE_STATUS_RATE_LIMITED | Retry-After 초 대기 |
| 503 | MERGE_STATUS_UNAVAILABLE | 조회 실패, 완료/실패로 확정하지 않음 |

두 GET 합산 **회원당 분당 30회**, Mongo 원자 증가로 여러 인스턴스가 공유한다. 분 단위 고정창이며 경계에서 순간 burst가 가능하므로 ingress 제한도 유지한다.
nextPollAfterSeconds는 PROCESSING 생성 후 첫 1분 5초, 이후 15초. 나머지 null.
실제 앱 polling은 프론트 작업: 양의 jitter, background 중단, foreground 2분 cap, 재진입·수동 새로고침 때 재조회.
앱 전체를 잠글 필요는 없다. 기록 반영 중 안내 후 완료 시 정상 기록 API를 재조회한다.

## 3. 사용자가 결정해야 하는 사항

추가 제품 결정 없음. 첫 출시 LC-only, Billing 도입 후 **신규** 작업에만 양쪽 필수, 과거 Billing backfill/재실행 없음을 구현했다.
실제 consumer 검증·인프라 gate는 배포 담당 작업이며 프론트 화면 구현도 별도 저장소 책임이다.
이 기능은 TMI-192와 별도 후속 범위다. Jira 생성/댓글/상태 변경은 하지 않았다.

## 4. 주요 위험과 미확인 사항

- 로컬 테스트는 mock 및 in-memory Mongo이다. 실제 Mongo replica set의 다중 문서 rollback/경합, LC 실제 이전 범위·중복 204·source write guard·중간 탈퇴는 별도 E2E로 확인해야 한다.
- in-memory Mongo는 partial unique index 필터를 정확히 구현하지 못한다. 다중 UserMerged fixture에서 trial-only partial index를 테스트 환경에서만 제외했다. 운영 인덱스를 제거하지 않는다.
- 기존 read timeout 3초를 유지한다. 원격 처리가 오래 걸리면 timeout→같은 event 재시도가 발생한다. 접수만 한 202를 완료로 바꾸면 안 된다.
- 조회 중 ACK와 경합하거나 문서 누락/손상이 있으면 보수적으로 503일 수 있다. 완료된 요약은 core/delivery TTL 이후에도 독립 조회 가능하다.
- 미완료 delivery의 일부가 장기 보관 만료로 없어지면 자동 완료로 처리하지 않는다. 무결성 복구를 운영에서 검토한다.
- 이미 Billing 필수였던 작업은 현재 OFF여도 대상 제외로 바꾸지 않는다. 채널 OFF/FIFO 앞선 DEAD_LETTER/PAUSED는 ACTION_REQUIRED다.
- 신규 지표는 코드에 등록했지만 외부 대시보드·알람 설정은 이번에 배포하지 않았다.

## 5. 구현·배포 설명

### 원자 저장과 완료 근거

`OwnerEventCore.eventId`를 실제 mergeId로 사용한다. legacy outbox의 별도 ID를 노출하지 않는다.
Guest 전이·세션 폐기/발급·core/delivery/sequence·progress 생성은 기존 merge Mongo 트랜잭션에 참여한다.
추적용 profile 누락/필수 publisher OFF/endpoint 형식 오류는 기동 검증 및 source 전이 전 검증에서 거부한다.

core의 내부 `progressContractVersion=1`은 재시도 시에도 유지된다. 외부 UserMerged v1 JSON은 변경하지 않는다.
delivery PUBLISHED·consumer cursor 전진·progress ACK는 같은 Mongo 트랜잭션이며 progress의 `@Version`으로 동시 ACK 덮어쓰기를 막는다.
lease 패자는 확인하지 않는다. ACK 저장 실패 시 전체 local 트랜잭션이 rollback되고 기존 retry로 수렴한다.
Spring 트랜잭션 프록시를 만들 수 있도록 두 owner-event transactional service의 final 제한을 제거했다.

204 외 2xx는 `ACK_CONTRACT_VIOLATION`으로 circuit PAUSED, cursor/progress 미변경.
408/425/429/5xx/timeout 등 기존 retry, 영구 오류 DEAD_LETTER, 인증/경로 오류 pause는 유지한다.
trial rebind와 과거 미추적 event의 성공 코드 계약은 변경하지 않는다.
consumer는 이전·write guard·inbox PROCESSED를 함께 commit하고 204, 같은 event/payload duplicate도 204여야 한다.

### 저장·개인정보

`user_merge_progress`는 대상/원천 내부 UUID, profile/필수 집합, ACK 시각, 보관·검토 시각을 저장한다. 외부 응답에는 사용자 UUID를 노출하지 않는다.
미완료 cleanupAt=null, 생성+90일 review. 정상 완료+30일 TTL이며 API는 TTL 물리 삭제를 기다리지 않고 만료를 검사한다.

탈퇴는 현재 User 검사로 즉시 조회 차단한다. 기존 identity release 단계에 privacy cleanup을 연결했다.
완료 요약은 cleanupAt을 즉시 설정하고 커서는 삭제한다. 미완료 요약은 `privacyCleanupRequested=true`와 최소 내부 참조를 유지해 event/ACK를 깨뜨리지 않으며, 완료되면 즉시 TTL 대상이 된다. 영구 미완료는 90일 review 절차로 점검한다.

### 첫 출시 설정

```dotenv
OWNER_EVENT_USER_MERGED_CAPTURE_ENABLED=true
OWNER_EVENT_MERGE_PROGRESS_CAPTURE_ENABLED=true
OWNER_EVENT_MERGE_COMPLETION_PROFILE=LEARNING_CORE_ONLY
OWNER_EVENT_LEARNING_CORE_USER_MERGED_PUBLISHER_ENABLED=true
OWNER_EVENT_BILLING_USER_MERGED_PUBLISHER_ENABLED=false
OWNER_EVENT_BILLING_TRIAL_REBIND_PUBLISHER_ENABLED=false
```

실제 LC endpoint/workload credential·audience·서명 설정은 기존 [fanout 운영 런북](owner-event-fanout-runbook.md)대로 검증한다.
LC-only에는 Billing endpoint/AWS credential이 필요 없다. 위 값은 **배포 후 검증 완료 시 설정하는 예**이며 이번 작업에서 실제 배포하지 않았다.
.env.example 및 application.yml의 신규 추적 기본값은 false, profile은 빈 값이다.

1. 코드/인덱스 배포, LC direct commit/duplicate 204 확인.
2. 신규 컬렉션 인덱스 생성 확인: progress 목록 2개·TTL·review, cursor TTL, quota TTL. 기존 core/delivery unique 인덱스 유지.
3. 실제 replica set에서 동시 ACK/rollback/응답 유실, LC-only Billing 완전 미설정, 완료 API와 실제 기록 일치 E2E.
4. profile 명시 후 추적 ON, 프론트 회원 토큰 교체·mergeId 저장·목록 복구 적용.
5. 추후 Billing은 자체 E2E 통과 후 신규 생성 profile만 양쪽으로 변경. 과거 core/progress/delivery/sequence 변경 없음.

capture OFF로 rollback해도 기존 요약 조회와 엄격 ACK 저장은 계속 지원한다. 이미 생성된 필수 publisher까지 끄면 ACTION_REQUIRED로 보이며 작업이 사라지지 않는다.

운영 지표: 기존 `identity.owner_event.publisher`의 ACK_CONTRACT_VIOLATION/DEAD_LETTERED/CIRCUIT_PAUSED, 신규 `identity.merge_progress.query_state`, `identity.merge_progress.query_error`, `identity.merge_progress.pending_age_seconds`.
event/user IDs를 metric label로 사용하지 않는다. pending age는 조회 시 관측값이므로 90일 review는 별도 DB 운영 점검도 필요하다.
운영 replay만 사용하고 사용자용 재실행 API는 만들지 않았다.

## 6. 부록 — 근거와 검증 범위

- [원래 상세 계획](guest-merge-progress-query-implementation-plan.md): 설계·전체 E2E 매트릭스 보존.
- [LC 완료·중복 계약](learning-core-user-merged-consumer-handoff.md): consumer commit/204.
- [조회/저장 코드](../../src/main/java/web/tosunsaeng/identity/domain/auth/mergeprogress/UserMergeQueryService.java)
- [격리 저장소/조회 테스트](../../src/test/java/web/tosunsaeng/identity/domain/auth/mergeprogress/MergeProgressRepositoryTests.java): profile/NOT_REQUIRED·TTL·본인 조회·cursor·quota·CAS·privacy.
- [트랜잭션 advice 테스트](../../src/test/java/web/tosunsaeng/identity/domain/auth/mergeprogress/MergeProgressTransactionTests.java): Spring proxy 및 실패 rollback 호출, 실제 replica-set rollback과 구분.
- [publisher 테스트](../../src/test/java/web/tosunsaeng/identity/domain/auth/ownerevent/application/OwnerEventPublisherTests.java): strict 204·동일 payload 재전송.
- 로컬 auth-test 도구는 회원 로그인 후 최근 20건 수동 조회만 추가했다. 실제 앱 UI나 LC/Billing 데이터 이전 코드는 포함하지 않는다.
