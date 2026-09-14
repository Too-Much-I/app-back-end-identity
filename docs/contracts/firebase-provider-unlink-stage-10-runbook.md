# TMI-131 — SNS 연결 해제·공통 연결 연동 및 운영 인계

- 기준일: 2026-09-14 (2026-09-11 공통 연결 승인 반영)
- 범위: Identity 서버 구현. 모바일·실제 Firebase·Mongo replica set 검증 및 운영 활성화는 미실행.
- 정책 원문: [Stage 10 계획](firebase-provider-unlink-stage-10-plan.md). 이 문서의 API·설정은 이번 코드 기준이다.

## 1. 5줄 결론

1. Google·Apple·Kakao 중 남는 수단의 최근 인증으로만 해제를 접수하며 마지막 수단은 보호한다. [서비스](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderChangeService.java)
2. 접수 Transaction은 대상 차단·작업 저장·전체 자체 세션 epoch 증가를 함께 확정한다. 202는 완료가 아니다.
3. worker는 지정 Provider unlink → 별도 Firebase revoke → 확인 → exact SocialIdentity 한 건 삭제 순서다. User·FirebaseIdentity·phone·나머지 SNS는 유지한다. [worker](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderUnlinkWorker.java)
4. 최초 연결·재연결 모두 공통 prepare → start → 같은 Firebase User의 link → 대상 SNS 재인증 → complete를 사용한다. 프론트는 과거 해제 여부를 분기하지 않는다. [연결 서비스](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderLinkService.java)
5. 기능은 기본 OFF다. PREPARED는 안전하게 만료되지만 STARTED·기존 legacy 미소비 허가는 자동 재전송·TTL 해제하지 않는다. 전체 운영 검증은 후속 통합 테스트에서 수행한다.

## 2. 사용자가 반드시 읽어야 하는 내용

### 2.1 해제와 전체 로그아웃

Google 해제의 경우 Apple 또는 Kakao로 같은 Firebase 사용자에게 재인증한 뒤 ID Token을 강제 갱신한다. Apple과 Kakao 모두를 인증할 필요는 없다. PHONE·해제 대상 Google·Firebase PASSWORD는 이 작업의 재인증 수단이 아니다.

202를 받으면 현재 기기도 Identity Access/Refresh Token 삭제 및 Firebase `signOut`을 수행한다. 응답을 못 받으면 새 요청 ID로 다시 해제하지 말고 아래 status로 조회한다. 다른 기기의 자체 refresh/replay는 epoch 검사로 무효화된다. 이미 발급된 downstream Access Token 즉시 차단을 추가한 것은 아니다.

남는 SNS로 다시 인증한 로그인은 처리 중에도 허용된다. 늦게 완료된 Firebase revoke에 걸린 새 세션은 한 번 더 재인증해야 할 수 있다. 조회 자체는 Token을 발급하지 않으며 새 자체 세션은 기존 exchange로 발급받는다.

### 2.2 공통 연결의 5분과 앱 중단

- 최근 인증 5분은 Firebase `auth_time`의 최대 나이이며, 강제 Token 갱신만으로 재인증되지는 않는다.
- prepare 후 5분 이내에 start한다. **PREPARED는 slot을 잡지 않는다.** 이 단계에서 앱을 닫으면 논리 만료 뒤 새 requestId로 다시 준비할 수 있다. 데이터는 expiresAt+PT24H TTL이며 실제 삭제 전에도 만료를 검사한다.
- 최초 start 응답의 `linkAllowed=true`를 받은 경우에만 동일 Firebase User에 대상 credential을 **한 번** link한다. STARTED 전환 시부터 별도로 5분 안에 완료해야 한다.
- link 성공 후 대상 SNS로 **start 이후** 재인증하여 complete한다. Firebase 인증 시각은 초 단위이므로 start와 같은 초의 인증은 거절될 수 있다. 다음 초 이후 실제 재인증 증거를 사용한다.
- complete 응답 유실은 같은 linkAttemptId로 complete 재시도한다. 동일 소유권·epoch·revision이 유효할 때 현재 완료를 확인할 뿐 저장·차단 해제를 반복하지 않는다.
- start 응답 유실 시 재시도는 `linkAllowed=false`다. **다시 Firebase link를 실행하지 않는다.** status와 같은 UID의 Firebase 현재 상태를 확인하고, 연결 성공이 확인됐으며 기존 호출이 종료됐다면 대상 재인증 후 complete한다.
- STARTED인데 실제 SDK 호출 전에 앱이 종료된 경우도 서버가 미호출을 확정할 수 없다. 만료/오류를 자동 취소로 해석하지 않으며 ACTION_REQUIRED로 운영 확인한다. 실제 link가 없거나 이전 호출 종료가 불명인 경우 새 start/재연결을 반복하지 않는다.
- STARTED의 미해결 상태는 추가 인증수단 변경·Firebase 원격 변경/탈퇴 cleanup barrier다. 남는 정상 SNS 로그인 자체를 차단하는 것은 아니다.
- 구형 `provider_relink_attempts` 미소비 기록은 PREPARED로 옮기지 않는다. 과거 앱의 link 시작 여부를 알 수 없어 기존 보호 상태를 유지한다.

## 3. 사용자가 결정해야 하는 사항

추가 제품 정책은 임의로 변경하지 않았다. 전화번호 셀프 변경·번호 재할당 예외 지원은 보류하고, 사용자 삭제·무료권 초기화·자동 merge는 하지 않는다.

운영 활성화 승인 때 다음을 확인한다.

- Provider별 실제 모바일 재인증 출처와 `auth_time`, Firebase 설정 및 계정 연결 해제 의무.
- STARTED 이후 SDK 미호출 crash/결과 불명의 보수적 제한 및 운영 복구 절차를 release에서 수용할지. PREPARED 중단의 자동 만료는 이번 코드에 반영했다.
- 운영자 격리 복구 담당자·승인 절차, 실제 인덱스/Transaction 및 지연 worker 증거.
- 신뢰 가능한 ingress client IP 처리 및 분산 rate limit. 로컬 예산만으로 분산 공격 방어가 완료된 것은 아니다.

## 4. 주요 위험과 미확인 사항

- lease 만료는 Firebase 호출 취소 증거가 아니다. `*_STARTED`에서 worker 유실은 `RECONCILIATION_REQUIRED`로 격리한다. Provider가 없어 보인다는 조회만으로 slot을 풀지 않는다.
- 외부 unlink/revoke 성공 후 DB 응답 유실도 자동 재호출하지 않는다. 확인된 ACK 뒤의 안전한 조회 실패만 재시도한다.
- STARTED link·legacy 미소비 permit과 미확정 unlink operation에는 TTL이 없다. 장기 미해결 항목과 보안 control은 운영 용량/체류시간 모니터링 대상이다.
- 앱에서 직접 Firebase phone을 바꾸거나 Provider를 조작할 수 있다는 위험은 남는다. 이번 sync/exchange는 phone 변경을 내부 PhoneIdentity 또는 혜택 변경으로 전파하지 않는다. 외부 번호 drift 자동 복구·번호 재할당 지원은 제공하지 않는다.
- 운영 dashboard/경보, 실제 Provider 계약, replica set rollback·다중 인스턴스·P99 증빙은 로컬 테스트로 대체하지 않는다.
- `provider-change.fence-enabled`나 기존 session fence를 데이터 생성 이후 끄는 legacy rollback은 금지한다. 새 접수만 OFF해도 영속 차단은 계속 적용된다.

## 5. 현재 구현과 직접 관련된 설명

### 5.1 API와 응답

새 endpoint는 모두 `/api/v1/auth/firebase/providers` 아래이며 하이픈이 없다. [Controller](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderChangeController.java)

| 요청 | 인증 | 성공 |
| --- | --- | --- |
| `POST /unlink` | 사용자 Bearer JWT + 남는 SNS Firebase 증거, `Idempotency-Key` | 202 접수/동일 작업 반환 |
| `POST /unlink/status` | body의 남는 SNS Firebase 증거; 사용자 Bearer 헤더 생략 가능 | 200 상태만 반환 |
| `POST /link/prepare` | 사용자 Bearer JWT + 기존 SNS Firebase 증거, `Idempotency-Key` | 200 PREPARED 또는 ALREADY_LINKED/기존 상태 |
| `POST /link/start` | 사용자 Bearer JWT + 남는 SNS Firebase 증거 | 200 STARTED; 최초 전환만 linkAllowed=true |
| `POST /link/complete` | 사용자 Bearer JWT + 대상 SNS post-start Firebase 증거 | 200 COMPLETED |
| `POST /link/status` | 사용자 Bearer JWT + 동일 owner의 최근 SNS Firebase 증거 | 200 상태; linkAllowed=false |
| 기존 `POST /relink/prepare` | 기존 보호 유지 | 폐기됨, 503 PROVIDER_CHANGE_UNAVAILABLE |
| 기존 `POST /api/v1/auth/firebase/auth-methods/sync` | 사용자 Bearer JWT + Firebase 증거 | 기존 승인 목록 검증만 수행. 신규 저장/permit 소비 금지 |

접수 및 prepare body:

```json
{"provider":"GOOGLE","firebaseIdToken":"<fresh-remaining-provider-proof>"}
```

해제 접수와 연결 prepare의 `Idempotency-Key`는 하나의 canonical lowercase UUID v4다. 앱은 논리 행위마다 생성하고 응답 유실 시 같은 값을 보관한다. 같은 key에 다른 provider는 409이며 원문 key 대신 SHA-256 hash를 작업에 저장한다.

접수 응답 예시:

```json
{
  "isSuccess": true,
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "result": {
    "operationId": "11111111-1111-4111-8111-111111111111",
    "provider": "GOOGLE",
    "status": "PROCESSING",
    "acceptedAt": "2026-09-11T00:00:00Z",
    "completedAt": null,
    "nextPollAfterSeconds": 3
  }
}
```

status body는 `{"requestId":"<original-idempotency-key>","firebaseIdToken":"<fresh-remaining-provider-proof>"}`다. 성공은 같은 result 구조로 `PROCESSING`, `COMPLETED`, `ACTION_REQUIRED`, `SUPERSEDED`만 공개한다. 종료/운영 확인 상태에는 `nextPollAfterSeconds=null`이다. 404는 재실행 허가가 아니다. 탈퇴/병합 사용자는 기존 계정 상태 오류가 우선한다.

공통 연결 API는 [ProviderLinkController](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderLinkController.java)를 따른다.

prepare body는 위 ChangeRequest와 같고, start/complete body는 다음과 같다. start는 기존에 남는 수단, complete는 대상 수단의 증거를 사용한다.

```json
{"linkAttemptId":"<issued-attempt-id>","firebaseIdToken":"<fresh-provider-proof>"}
```

link/status body는 `{"requestId":"<prepare-idempotency-key>","firebaseIdToken":"<fresh-owner-proof>"}`다. 준비 응답을 못 받았어도 원래 요청 ID로 조회할 수 있다. unlink/status와 달리 **사용자 Bearer도 필수**다. 만료된 자체 Token은 정상 재발급/남는 수단 로그인으로 복구한다.

연결 API 모두 HTTP 200 BaseResponse이며 result 형태는 동일하다.

```json
{
  "isSuccess": true,
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "result": {
    "linkAttemptId": "11111111-1111-4111-8111-111111111111",
    "provider": "GOOGLE",
    "status": "PREPARED",
    "expiresAt": "2026-09-11T00:05:00Z",
    "linkAllowed": false
  }
}
```

| status | 프론트 행동 |
| --- | --- |
| PREPARED | start 호출 가능. 아직 Firebase link 금지 |
| ALREADY_LINKED | 현재 대상 소유권 확인됨. Firebase link/start 불필요 |
| STARTED | 최초 start의 linkAllowed=true일 때만 SDK link 1회. 재조회/중복 start는 false |
| COMPLETED | 연결 완료. 같은 요청 재전송은 새 저장/해제를 하지 않음 |
| EXPIRED | PREPARED가 만료됨. 원래 요청은 재사용하지 않고 새 준비 가능 |
| SUPERSEDED | 계정 epoch/Provider revision이 바뀜. 오래된 응답으로 UI/SDK를 실행하지 않음 |
| ACTION_REQUIRED | STARTED 만료/epoch·revision 변경 등. 새 mutation 금지, 운영 확인 |

완료/ALREADY_LINKED의 expiresAt은 과거 요청 처리 기한이지 로그인 수단의 만료일이 아니다. 연결 결과 보존은 별도 cleanupAt(완료+PT24H)이다. status에는 현재 원격 연결 전체 목록을 담지 않는다. 최근 탈퇴/계정 변경은 기존 계정 오류를 우선한다.

기존 sync URL·응답 구조는 유지하되 **읽기 검증으로 제한**한다. Firebase에만 있고 내부 승인되지 않은 SNS는 저장하지 않고 거절한다. `linkAttemptId`도 더 이상 소비하지 않으며 전달하면 409다. 대상 complete는 다른 Provider를 함께 동기화하지 않는다. 외부에서 임의로 연결한 미승인 SNS로 exchange/기타 검증 경로를 사용해 로그인하는 것도 거절한다. 이 변경은 의미상 API 변경이므로 프론트 동시 전환이 필요하다.

공통 `Cache-Control: no-store`, `Pragma: no-cache`. 원본 Token/credential/UID/subject를 응답·로그에 넣지 않는다. 상태 조회에 사용자 JWT가 없어도 Firebase 서명·revoked/disabled·project/tenant·최근 인증·동일 내부 소유권 검사는 수행한다. workload JWT는 사용자 JWT 대신 쓸 수 없다.

### 5.2 오류 계약

| HTTP / code | 앱 처리 |
| --- | --- |
| 400 `INVALID_PROVIDER_REQUEST_ID` / `INVALID_REQUEST` | UUID 헤더·body 형식 확인, 무한 재시도 금지 |
| 401 `FIREBASE_RECENT_AUTH_REQUIRED` / 기존 Firebase 인증 오류 | 허용된 남는 SNS로 실제 재인증; 강제 Token 갱신만 반복하지 않음 |
| 401 `SESSION_LOGGED_OUT` | 로컬 자체 Token 제거, 재로그인 |
| 403 `PROVIDER_LAST_METHOD` | 마지막 SNS 해제 불가 안내 |
| 403 `PROVIDER_REMAINING_AUTH_REQUIRED` | unlink/prepare/start는 남는 수단, complete는 요청한 대상 수단으로 재인증 |
| 409 `PROVIDER_CHANGE_CONFLICT` / 기존 owner 충돌 | 현재 작업/계정 상태 확인, 자동 merge 금지 |
| 409 `PROVIDER_RELINK_REQUIRED` | 공통 prepare/start/link/complete 필요. 과거 연결 여부를 프론트에서 분기하지 않음 |
| 409 `PROVIDER_RELINK_EXPIRED` | status 확인. PREPARED 만료만 새 준비 가능; STARTED는 운영 확인 |
| 404 `PROVIDER_OPERATION_NOT_FOUND` | 보존 만료/미존재, 다른 owner 조회 불가 |
| 413 `PROVIDER_REQUEST_TOO_LARGE` | 본문 크기 수정 |
| 429 `PROVIDER_RATE_LIMITED` | 제한 후 backoff; ingress 응답은 `Retry-After: 60` |
| 503 `PROVIDER_CHANGE_UNAVAILABLE` / `SESSION_SECURITY_UNAVAILABLE` / 기존 Firebase 장애 | OFF/DB/외부 장애; 응답 유실 시 같은 requestId로 상태 확인 |

### 5.3 저장과 worker 상태

`PENDING → UNLINK_STARTED → UNLINK_ACKED → REVOKE_STARTED → REVOKE_ACKED → COMPLETED`

`RECONCILIATION_REQUIRED`는 외부에 `ACTION_REQUIRED`다. 안전한 재조회는 같은 phase에서 `nextAttemptAt`으로 지연한다. 한 phase의 조회 retry 때문에 unlink/revoke를 재전송하지 않는다.

새 별도 coordinator document 대신 기존 versioned `UserSessionControl.activeLogoutId`를 공통 slot으로 확장 사용한다: logout은 기존 UUID, unlink는 `unlink:<operationId>`, 새 link는 `link:<attemptId>`, legacy relink는 `relink:<attemptId>`. 기존 logout worker는 다른 slot을 기다린다. 탈퇴 접수는 계속 가능하고, dispatch 전 또는 ACK된 unlink만 원자적으로 supersede한다. 시작됐거나 불명인 actor·STARTED link 및 legacy 미소비 relink는 cleanup/release barrier로 남긴다. PREPARED는 해당 barrier에 포함하지 않는다.

| 문서/인덱스 | 보존 |
| --- | --- |
| `provider_unlink_operations`, unique `(userId,requestIdHash)`, `(state,nextAttemptAt,leaseUntil)` | COMPLETED/SUPERSEDED만 terminal+P7D TTL |
| `provider_link_attempts`, unique (userId,requestIdHash), userId, @Version | PREPARED: expiresAt+PT24H; STARTED: TTL 없음; COMPLETED/ALREADY_LINKED: completedAt+PT24H |
| `provider_relink_attempts`, legacy userId index | 신규 생성/소비 폐기. 기존 미소비 기록은 자동 삭제하지 않고 운영 확인 |
| `auth_method_change_controls`, userId `_id`, version/revision | TTL 없음; 명시적 relink 또는 안전한 탈퇴 release에서 정리 |
| `user_session_controls` 기존 versioned slot | 기존 보안 control 보존 계약 유지 |

target은 binding ID/createdAt/project/tenant/UID 및 원격 creation을 검사한다. 차단 control에는 raw provider subject를 복제하지 않는다. exact SocialIdentity ID/owner/provider를 확인한 뒤 한 건만 삭제한다. 기존 `(provider,providerSubject)` unique index는 유지한다.

### 5.4 설정과 rollout

| 설정 | 기본 |
| --- | --- |
| `FIREBASE_PROVIDER_CHANGE_FENCE_ENABLED` | false; 기존 session fence와 Firebase 활성화 필요 |
| `FIREBASE_PROVIDER_UNLINK_ENABLED` | false; 신규 접수 |
| `FIREBASE_PROVIDER_UNLINK_WORKER_ENABLED` | false; scheduler/원격 adapter |
| `FIREBASE_PROVIDER_LINK_ENABLED` | false; 신규 공통 prepare/start. OFF라도 fence ON이면 기존 complete/status drain 가능 |
| `FIREBASE_PROVIDER_RELINK_ENABLED` | false; 폐기된 설정. true면 시작 실패하며 새 flag로 묵시 전환하지 않음 |
| `PROVIDER_UNLINK_RECENT_AUTH_MAX_AGE` / `PROVIDER_RELINK_ATTEMPT_TTL` | 각각 PT5M, 최대 5분 |
| `PROVIDER_UNLINK_TERMINAL_RETENTION` / `PROVIDER_RELINK_TERMINAL_RETENTION` | P7D / PT24H |
| `PROVIDER_UNLINK_LEASE` / `PROVIDER_UNLINK_FIXED_DELAY` / `PROVIDER_UNLINK_BATCH_SIZE` | PT60S / PT5S / 20 |
| `PROVIDER_UNLINK_MAX_ATTEMPTS` / `PROVIDER_UNLINK_BACKOFF_INITIAL` / `PROVIDER_UNLINK_BACKOFF_MAX` | 12 / PT5S / PT5M |
| 기존 `FIREBASE_CONNECT_TIMEOUT` / `FIREBASE_READ_TIMEOUT` | PT3S / PT5S; 별도 unlink timeout 설정은 만들지 않음 |

정확한 환경변수는 [application.yml](../../src/main/resources/application.yml)을 따른다. HTTP mutation retry=0, redirect OFF, 401 response-handler refresh/replay OFF, body logging OFF, 응답 최대 64 KiB. credential 공급은 기존 Firebase credentials provider를 사용한다.

Provider API 본문은 chunked 포함 최대 24 KiB. 인스턴스별 socket remoteAddr 120회/분, 검증된 owner 60회/분, 각 예산 map 최대 10,000건이다. reverse proxy 환경에서 주소가 공유되면 과도한 제한이 생길 수 있다. 신뢰 proxy/분산 quota·실사용 부하 검증 전 활성화하지 않는다. 임의 `X-Forwarded-For`를 직접 신뢰하지 않는다.

모든 instance에 코드 및 session/provider fence를 먼저 배포하고 구형 writer 종료를 확인한다. 실제 인덱스/Transaction·Provider·모바일 증빙 후 worker와 신규 접수를 단계적으로 ON한다. 공통 연결은 프론트 전환 및 start 응답 유실/중단 테스트 gate를 통과한 뒤 ON한다. 구형 sync writer가 한 인스턴스라도 남으면 우회 가능하므로 새 연결 flag를 켜지 않는다. 신규 접수 중지 시에도 guard와 status는 유지하고 worker OFF를 원격 호출 취소로 해석하지 않는다.

### 5.5 결과 불명 운영 절차

1. 신규 변경을 중지하고 해당 operation/control/permit의 version·상태·slot·정확한 binding 및 worker 실행 증거를 제한된 운영 환경에서 확인한다. 개인정보·Token을 Jira/일반 로그에 복사하지 않는다.
2. read-only Firebase inspect로 정확한 incarnation·대상 연결·남는 수단·validAfter를 확인한다. 단순 부재 또는 lease 만료만으로 완료 처리하지 않는다.
3. 이전 worker/SDK 요청의 최종 응답 및 실행 종료·자동 재시도 부재를 확인한다. STARTED link와 legacy 미소비 relink는 모바일 실행이 실제 중단됐는지와 현재 연결도 확인해야 한다. 신규 PREPARED의 정상 만료에는 이 수동 복구를 요구하지 않는다.
4. **증거를 확보하지 못하면 격리를 유지한다.** 남는 SNS의 새 로그인은 별개다. 수동 재전송 API나 무조건 `activeLogoutId=null` 처리, block/permit 삭제를 제공하지 않는다.
5. 증거가 확보된 건은 승인된 별도 운영 변경에서 exact version/owner/phase를 조건으로 관련 문서를 한 Transaction으로 전환해야 한다. 현재 저장소에 무조건 실행 가능한 수동 복구 명령은 제공하지 않는다. staging 복구 리허설·승인된 보수 스크립트 준비는 운영 활성화 전 잔여다.

지표: `identity.provider.change`(commit 후 고정 outcome), `identity.provider.change.rejected`(거절 시도), `identity.provider.change.pending.age.seconds`(worker가 선택한 작업 나이), `identity.provider.change.phase`(확인된 phase latency). userId/UID/operationId label은 없다. pending age는 전체 backlog gauge가 아니므로 미해결 문서의 상태별 건수/최장 체류시간은 별도 제한된 운영 조회로 감시한다.

## 6. 부록 — 검증 근거 및 완료 인계

- 최종 로컬 검증(2026-09-14): `./gradlew clean test --console=plain` 성공. XML 기준 137 suites / 886 tests, 실패·오류·skip 0. 실제 Provider/모바일/replica set 검증은 아래 잔여대로 별도다.
- [ProviderLinkHttpTests](../../src/test/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderLinkHttpTests.java): 공통 prepare/start/complete/status DTO·owner 위임·proof 필수·기본 OFF·no-store·credential 비노출.
- [ProviderChangeTests](../../src/test/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderChangeTests.java): Provider별 정상·마지막 수단·proof·epoch·멱등 접수·stale revision·rollback fault·늦은 ACK·결과 불명 revoke·exact binding·relink one-time/expiry·탈퇴 barrier·Mongo mapping/CAS.
- [HTTP tests](../../src/test/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderChangeHttpTests.java), [SecurityIntegrationTests](../../src/test/java/web/tosunsaeng/identity/global/config/SecurityIntegrationTests.java): status 증거 필수, 보호 API JWT 분리, 기본 OFF, no-store, 크기/속도 제한, OpenAPI.
- [adapter tests](../../src/test/java/web/tosunsaeng/identity/domain/auth/federation/infrastructure/firebase/FirebaseProviderMutationHttpAdapterTests.java): 지정 Provider만 삭제, 다른 project 거절, 오류·redirect 재시도 없음, 응답/subject 비노출.
- [configuration tests](../../src/test/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderChangeConfigurationTests.java), [policy tests](../../src/test/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderChangePolicyTests.java): 기본 OFF·의존성·기간 제한, 예산 cardinality, commit 후 metrics.

계획 U01~U30은 위 격리 테스트와 기존 전체 회귀로 로컬 부분을 검증한다. `ProviderChangeTests`의 rollback은 **테스트용 snapshot 복원**이며 실제 Mongo Transaction이 아니다. 특히 U03/U06~U09/U14~U19/U22/U27/U30의 실제 동시성·원격 actor 종료·replica set·P99 증빙, U04/U13/U21/U24의 실제 Provider/phone/SDK 검증은 남아 있다. 이를 완료로 표기하지 않는다.

### Jira 댓글 초안 — 자동 등록하지 않음

TMI-131 Identity Stage 10 서버 구현: unlink 접수·상태·최초/재연결 공통 prepare/start/complete/status, 영속 Provider 차단과 epoch/revision fencing, exact Firebase mutation worker 및 기존 logout/withdrawal 연계, HTTP 보안·격리 테스트·프론트/runbook 문서 추가. 변경 파일은 providerchange 패키지, Firebase sync/verifier·세션 보안 경계와 관련 테스트·문서다. 전체 테스트 수치와 최종 검증은 WORKLOG의 해당 구현 항목을 따른다. 실제 Firebase·모바일·replica set·운영 reconciliation 검증 전 기능 OFF 유지. PREPARED는 비차단 만료, STARTED/legacy 미소비는 자동 해제하지 않는 제한이 출시 gate다. 기존 sync의 신규 저장 폐기와 프론트 전환을 포함한다. Jira 본문은 기존 relink 설계이므로 추가 승인 후 별도 갱신이 필요하다.
