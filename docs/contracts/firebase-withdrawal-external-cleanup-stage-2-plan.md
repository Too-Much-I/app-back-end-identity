# 2단계 구현 계획: Firebase 탈퇴 외부 cleanup worker

## 1. 목적

Jira `TMI-104`의 목표는 Stage 1에서 생성한 `UserWithdrawalLifecycle`을 비동기로 처리해 Firebase 외부 User를 안전하게 disable·refresh revoke·delete하고, 내부 identity release를 담당하는 Stage 3으로 인계하는 것이다.

이 단계는 사용자의 내부 탈퇴를 결정하지 않는다. Stage 1에서 User는 이미 `WITHDRAWN`이고 내부 RefreshSession도 폐기됐다. Stage 2는 외부 시스템 장애가 내부 탈퇴를 되돌리지 않도록 MongoDB Transaction 밖에서 Firebase mutation을 멱등 재시도한다.

Stage 2 완료 상태는 `IDENTITY_RELEASE_PENDING`이다. FirebaseIdentity·SocialIdentity·PhoneIdentity 점유 해제, `CLEANED` 전환과 재가입 허용은 Stage 3 책임이다.

## 2. 선행 상태와 문제

Stage 1 완료 직후 상태는 다음과 같다.

- User: `WITHDRAWN`
- 내부 RefreshSession: 모두 폐기
- active phone eligibility binding: REVOKED revision/outbox 생성
- FirebaseIdentity·SocialIdentity·PhoneIdentity: 아직 점유 유지
- Firebase User: 존재하거나 Stage 2 실행 전 외부에서 이미 삭제됐을 수 있음
- lifecycle: `EXTERNAL_CLEANUP_PENDING`

현재 저장소에는 다음 기반은 있지만 worker 구현은 없다.

- `UserWithdrawalLifecycle`의 target, attempt, lease, retry, error, completion 필드
- worker claim용 `status + nextAttemptAt + leaseUntil` index
- Firebase ID Token 검증용 `FirebaseAdminClient`
- Phone eligibility와 User merge publisher의 atomic lease·retry 구현 패턴

현재 `FirebaseAdminClient`는 verify만 제공한다. disable·refresh revoke·User 조회·delete와 결과 불명 확인용 application port를 새로 만들어야 한다.

## 3. 포함 범위

- due lifecycle의 atomic claim과 claim별 고유 lease token
- 만료된 `EXTERNAL_CLEANUP_IN_PROGRESS` lease 재회수
- lease renewal과 version fencing
- User·FirebaseIdentity target preflight guard
- Firebase User disable
- Firebase Refresh Token revoke
- Provider별 계정 삭제 의무 판정·처리 경계
- Firebase User delete와 삭제 결과 확인
- 이미 삭제된 Firebase User의 멱등 성공
- retryable·reconciliation 오류 분류
- 지수 backoff, 최대 시도 횟수와 안전한 `lastErrorCode`
- `EXTERNAL_CLEANUP_COMPLETED` 기록과 `IDENTITY_RELEASE_PENDING` 인계
- worker·scheduler·metrics·구조화 로그
- 기본 비활성 설정과 staging activation gate
- domain·Repository·worker·adapter·configuration·scheduler 테스트

## 4. 제외 범위

- FirebaseIdentity·SocialIdentity hard delete 또는 release
- PhoneIdentity·PhoneFingerprintAlias release
- `CLEANED` 전환과 동일 credential 재가입 허용
- 가입 중단 Firebase User cleanup
- logout-all Firebase revoke
- Provider unlink·전화번호 변경
- Billing·Learning Core 구현
- 공개 cleanup 조회·재시도 API
- 운영자 reconciliation UI

## 5. 핵심 안전 원칙

### 5.1 내부 탈퇴는 되돌리지 않는다

Firebase mutation이 실패해도 User를 `ACTIVE`로 복구하지 않는다. 실패는 lifecycle의 retry 또는 reconciliation 상태로 남기고 외부 cleanup이 수렴할 때까지 로그인·신규 enrollment를 fail-closed 차단한다.

### 5.2 Firebase 호출은 MongoDB Transaction 밖에서 실행한다

claim·lease·상태 변경은 짧은 MongoDB atomic update로 처리한다. Firebase 네트워크 호출을 Mongo Transaction에 넣지 않는다.

### 5.3 claim마다 고유 fencing token을 사용한다

worker process의 고정 UUID를 lease owner로 재사용하지 않는다. claim마다 새 UUID `leaseToken`을 생성해 `leaseOwner`에 저장한다. 같은 process에서 이전 실행이 늦게 돌아와도 새 claim과 token이 달라 최신 상태를 수정하지 못한다.

모든 상태 update 조건에는 최소한 다음을 포함한다.

- `withdrawalId`
- 기대 status
- `leaseOwner == leaseToken`
- claim에서 받은 `version`

성공한 update는 version을 증가시키고 lease를 해제한다.

### 5.4 원격 작업은 반복 가능해야 한다

- 이미 disabled: disable 성공과 동일하게 처리
- 이미 refresh revoke됨: revoke 성공과 동일하게 처리
- 이미 삭제됨 또는 `USER_NOT_FOUND`: delete 성공과 동일하게 처리
- delete timeout: 즉시 성공·실패로 단정하지 않고 User 존재 여부를 재조회

Firebase SDK 원문 message나 provider credential은 lifecycle·로그·metric tag에 사용하지 않는다.

## 6. lifecycle 상태 계약

```text
EXTERNAL_CLEANUP_PENDING
        ↓ atomic claim
EXTERNAL_CLEANUP_IN_PROGRESS
        ├─ retryable failure → EXTERNAL_CLEANUP_RETRY_WAIT
        ├─ terminal/ambiguous failure → RECONCILIATION_REQUIRED
        └─ delete confirmed → EXTERNAL_CLEANUP_COMPLETED
                                      ↓ local CAS handoff
                             IDENTITY_RELEASE_PENDING
```

### `EXTERNAL_CLEANUP_PENDING`

- Stage 1이 생성한다.
- `nextAttemptAt <= now`일 때 claim 가능하다.
- Firebase target이 없으면 LOCAL·GUEST lifecycle이므로 외부 호출 없이 완료 처리한다.

### `EXTERNAL_CLEANUP_IN_PROGRESS`

- claim 성공한 단 하나의 worker가 소유한다.
- claim 시 `attemptCount`를 1 증가시킨다.
- `leaseOwner`, `leaseUntil`, `updatedAt`, version을 갱신한다.
- `leaseUntil <= now`이면 crash recovery 대상으로 재claim할 수 있다.

### `EXTERNAL_CLEANUP_RETRY_WAIT`

- retryable 오류 code와 `nextAttemptAt`을 저장한다.
- lease를 제거한다.
- due time 전에는 claim하지 않는다.

### `EXTERNAL_CLEANUP_COMPLETED`

- Firebase User 부재를 확인한 경우에만 진입한다.
- `externalDeletedAt`을 기록하고 error·lease를 제거한다.
- 외부 호출 없이 별도 local CAS로 `IDENTITY_RELEASE_PENDING`에 인계한다.
- completed→release pending 사이에서 worker가 종료돼도 다음 scheduler run이 인계한다.

### `IDENTITY_RELEASE_PENDING`

- Stage 2 terminal이며 Stage 3 시작 상태다.
- Stage 2 worker가 Firebase를 다시 호출하지 않는다.

### `RECONCILIATION_REQUIRED`

- 자동 처리 결과를 안전하게 확정할 수 없거나 최대 재시도 횟수를 초과한 상태다.
- 자동 재claim하지 않는다.
- Firebase target·raw error가 아닌 안전한 code와 attempt 시각만 남긴다.

## 7. atomic Repository 계약

Spring Data derived query만으로 claim을 구현하지 않는다. `MongoTemplate.findAndModify` 기반 custom Repository를 둔다.

예상 계약은 다음과 같다.

```java
Optional<UserWithdrawalLifecycle> claimNext(
        String leaseToken,
        Instant now,
        Instant leaseUntil
);

boolean renewLease(
        String withdrawalId,
        String leaseToken,
        long expectedVersion,
        Instant renewedUntil,
        Instant updatedAt
);

boolean scheduleRetry(
        String withdrawalId,
        String leaseToken,
        long expectedVersion,
        WithdrawalCleanupFailureCode code,
        Instant nextAttemptAt,
        Instant updatedAt
);

boolean markReconciliationRequired(...);

boolean markExternalCleanupCompleted(...);

boolean handoffCompletedToIdentityReleasePending(...);
```

claim query는 다음 후보 중 due time과 요청 시각이 가장 오래된 한 건을 선택한다.

1. `EXTERNAL_CLEANUP_PENDING`이고 `nextAttemptAt <= now`
2. `EXTERNAL_CLEANUP_RETRY_WAIT`이고 `nextAttemptAt <= now`
3. `EXTERNAL_CLEANUP_IN_PROGRESS`이고 `leaseUntil <= now`

claim update는 status를 `EXTERNAL_CLEANUP_IN_PROGRESS`로 바꾸고 attempt를 증가시키며 새 lease token과 lease 만료 시각을 저장한다. `ReturnDocument.AFTER`에 해당하는 결과를 worker에 반환한다.

별도 local handoff query는 `EXTERNAL_CLEANUP_COMPLETED`를 오래된 순서로 찾아 `IDENTITY_RELEASE_PENDING`으로 CAS 전환한다. 외부 cleanup claim query에 completed 상태를 섞지 않는다.

## 8. target preflight guard

Firebase mutation 전 다음을 읽기 전용으로 확인한다.

- lifecycle의 User가 존재하고 `WITHDRAWN`인지
- Firebase target이 있는 lifecycle이면 FirebaseIdentity가 존재하는지
- FirebaseIdentity의 userId·project·UID가 lifecycle snapshot과 일치하는지
- lifecycle status·lease·version이 현재 claim과 일치하는지

불일치하면 Firebase를 호출하지 않고 `RECONCILIATION_REQUIRED`로 이동한다.

Firebase target이 모두 null이면 LOCAL·GUEST lifecycle로 간주한다. 이 경우 Firebase 호출 없이 external cleanup completed→identity release pending으로 넘긴다. target 일부만 null인 문서는 도메인 불변식 위반이므로 reconciliation 대상이다.

email, phone, provider subject로 target을 추정하지 않는다.

## 9. Firebase cleanup application port

SDK 타입을 worker·domain에 노출하지 않는 별도 port를 둔다.

```java
public interface FirebaseWithdrawalCleanupPort {
    FirebaseCleanupAccountSnapshot inspect(String projectId, String firebaseUid);
    void disable(String projectId, String firebaseUid);
    void revokeRefreshTokens(String projectId, String firebaseUid);
    ProviderObligationResult satisfyProviderDeletionObligations(
            FirebaseCleanupAccountSnapshot snapshot
    );
    void delete(String projectId, String firebaseUid);
    FirebaseAccountPresence checkPresence(String projectId, String firebaseUid);
}
```

결과 타입에는 Firebase UID, email, phone, provider subject를 담지 않는다. provider obligation 판정에 필요한 provider enum set만 memory에서 사용하고 영속화하지 않는다.

port exception은 안전한 분류만 제공한다.

```text
NOT_FOUND
RATE_LIMITED
TIMEOUT
UNAVAILABLE
PERMISSION_DENIED
CONFIGURATION_ERROR
PROJECT_MISMATCH
PROVIDER_OBLIGATION_REQUIRED
RESULT_UNKNOWN
```

원본 Firebase exception과 message는 adapter 경계에서 버린다.

## 10. Firebase SDK adapter

현재 verify 전용 `FirebaseAdminClient`에 mutation을 섞지 않는다. verify adapter와 별도로 `FirebaseWithdrawalCleanupPort` 구현을 두되 동일한 tenant-aware `AbstractFirebaseAuth` 선택 로직을 재사용한다.

SDK 호출은 다음 기능으로 매핑한다.

- inspect: `getUser(uid)`
- disable: `updateUser(new UserRecord.UpdateRequest(uid).setDisabled(true))`
- revoke: `revokeRefreshTokens(uid)`
- delete: `deleteUser(uid)`
- presence 확인: `getUser(uid)`의 정상/`USER_NOT_FOUND`

adapter는 configured Firebase project와 lifecycle project가 정확히 일치할 때만 mutation한다. tenant ID도 기존 Firebase configuration과 동일하게 적용한다.

connect/read timeout은 기존 Firebase 설정과 독립적으로 명시하고, worker lease duration은 외부 호출 전체의 최악 실행 시간보다 길게 설정하거나 단계 사이에 lease를 renew한다.

## 11. cleanup 실행 순서

worker 한 건의 기본 순서는 다음과 같다.

1. lifecycle atomic claim
2. User·FirebaseIdentity target preflight guard
3. Firebase account inspect
4. Firebase User disable
5. lease renew/fencing 확인
6. Firebase Refresh Token revoke
7. lease renew/fencing 확인
8. Provider별 account deletion 의무 처리
9. lease renew/fencing 확인
10. Firebase User delete
11. User 부재 확인
12. `EXTERNAL_CLEANUP_COMPLETED` fenced update
13. local CAS로 `IDENTITY_RELEASE_PENDING` 인계

어느 단계에서든 `NOT_FOUND`가 반환되면 외부 삭제가 이미 완료된 것으로 수렴한다. 단, project mismatch나 target guard 실패를 `NOT_FOUND` 성공으로 숨기지 않는다.

delete가 timeout·connection reset으로 끝나면 presence를 확인한다.

- absent: 성공
- present: retry
- presence 조회도 불명: retry 후 최대 시도 초과 시 reconciliation

## 12. Provider별 삭제 의무

Firebase User 삭제는 Google·Kakao·Apple 원본 Provider 계정을 삭제하지 않는다. Provider 계정 자체 삭제가 목표는 아니지만 계정 삭제 정책상 authorization revoke 의무가 있는 Provider는 별도 처리해야 한다.

### Google·Kakao

- 현재 계약에서 upstream Provider 계정 삭제는 수행하지 않는다.
- Firebase credential과 내부 mapping cleanup만 수행한다.
- Provider 정책 또는 OIDC 설정이 별도 revoke를 요구하면 adapter contract test와 운영 gate를 추가한다.

### Apple

Apple authorization revoke는 일회성 authorization code 등 별도 material이 필요할 수 있다. 현재 Stage 1 lifecycle에는 이를 저장하지 않으며 credential을 lifecycle·로그·일반 Mongo document에 추가하지 않는다.

따라서 구현 전에 다음 중 하나를 PoC와 정책 리뷰로 확정한다.

1. 모바일이 Firebase의 공식 Apple token revoke 흐름을 완료하고 서버가 확인 가능한 receipt만 전달
2. 탈퇴 요청 시 받은 일회성 material을 전용 보안 경계에서 즉시 교환하고 lifecycle에는 결과만 저장
3. 승인된 별도 short-lived secret store를 사용하고 일반 Mongo lifecycle에는 reference와 안전한 상태만 저장

지원 경로가 확정되지 않은 상태에서 Apple 계정을 Firebase delete까지 자동 진행하지 않는다. 해당 lifecycle은 `PROVIDER_OBLIGATION_REQUIRED`로 reconciliation에 보내고 Apple production withdrawal flag를 열지 않는다.

## 13. 오류와 retry 정책

신규 안전 enum `WithdrawalCleanupFailureCode`를 사용하고 `lastErrorCode`에는 enum name만 저장한다.

### retryable

- `RATE_LIMITED`
- `TIMEOUT`
- `UNAVAILABLE`
- `DELETE_NOT_CONFIRMED`
- 일시적인 `RESULT_UNKNOWN`

### 즉시 reconciliation

- `PROJECT_MISMATCH`
- `TARGET_OWNERSHIP_MISMATCH`
- `PERMISSION_DENIED`
- `CONFIGURATION_ERROR`
- `PROVIDER_OBLIGATION_REQUIRED`
- domain/schema invariant 위반

### 최대 시도 초과 reconciliation

- 마지막 retryable code와 `MAX_ATTEMPTS_EXCEEDED` 여부를 안전하게 구분한다.
- 원문 exception class·message·stack을 lifecycle에 저장하지 않는다.

backoff는 attempt 기준 지수 증가와 상한을 사용한다.

```text
delay = min(initialDelay × 2^(attempt-1), maxDelay)
```

동일 시각의 thundering herd를 줄이려면 bounded jitter를 적용하되 테스트에서는 고정 가능한 source를 주입한다.

## 14. worker 결과 계약

한 번의 `processNext()`는 다음 outcome 중 하나를 반환한다.

```text
NONE
LOCAL_TARGET_SKIPPED
EXTERNAL_DELETED
HANDED_OFF
RETRY_SCHEDULED
RECONCILIATION_REQUIRED
LEASE_LOST
```

outcome에는 userId, Firebase UID, provider subject를 넣지 않는다. scheduler는 outcome이 `NONE`일 때 batch loop를 종료한다. `LEASE_LOST`는 현재 worker가 후속 상태를 변경하지 않고 종료한다.

## 15. configuration과 feature flag

신규 설정 prefix는 `app.firebase-withdrawal-cleanup`으로 분리한다.

예상 설정:

```yaml
app:
  firebase-withdrawal-cleanup:
    enabled: ${FIREBASE_WITHDRAWAL_CLEANUP_ENABLED:false}
    fixed-delay: ${FIREBASE_WITHDRAWAL_CLEANUP_FIXED_DELAY:PT5S}
    lease-duration: ${FIREBASE_WITHDRAWAL_CLEANUP_LEASE_DURATION:PT1M}
    max-attempts: ${FIREBASE_WITHDRAWAL_CLEANUP_MAX_ATTEMPTS:12}
    initial-backoff: ${FIREBASE_WITHDRAWAL_CLEANUP_INITIAL_BACKOFF:PT5S}
    max-backoff: ${FIREBASE_WITHDRAWAL_CLEANUP_MAX_BACKOFF:PT1H}
    max-batch-size: ${FIREBASE_WITHDRAWAL_CLEANUP_MAX_BATCH_SIZE:20}
    connect-timeout: ${FIREBASE_WITHDRAWAL_CLEANUP_CONNECT_TIMEOUT:PT3S}
    read-timeout: ${FIREBASE_WITHDRAWAL_CLEANUP_READ_TIMEOUT:PT5S}
```

안전 기본값:

- worker bean·scheduler 기본 비활성
- Firebase Auth 자체가 disabled면 cleanup worker 활성화 실패
- project·credential·timeout 설정이 없거나 부정확하면 fail-fast
- max batch 1~100
- lease, delay, backoff는 양수
- lease duration은 SDK timeout과 batch 한 건의 최악 실행 시간을 커버하도록 검증

`FIREBASE_WITHDRAWAL_ENABLED`는 신규 탈퇴 요청을 여는 flag이고, `FIREBASE_WITHDRAWAL_CLEANUP_ENABLED`는 worker flag다. 두 flag를 같은 의미로 합치지 않는다.

## 16. scheduler

`@Scheduled` method는 한 tick에서 최대 `maxBatchSize`만 처리한다. 외부 call을 병렬 fan-out하지 않는다. Firebase rate limit과 lease 복잡도를 줄이기 위해 첫 구현은 순차 처리한다.

scheduler 순서:

1. completed lifecycle의 local handoff 처리
2. due external lifecycle을 최대 batch size까지 처리
3. `NONE`이면 loop 종료

여러 instance에서 scheduler가 동시에 실행돼도 Repository atomic claim이 단일 실행을 보장한다.

## 17. observability

구조화 로그와 metric에는 다음 안전한 필드만 사용한다.

- event: `user.withdrawal.external_cleanup`
- outcome
- lifecycle status
- attempt bucket 또는 attempt count
- failure code
- provider obligation type enum
- lease lost 여부

금지:

- Firebase UID·project 원문
- userId
- email·phone
- provider subject
- Token·credential
- Firebase SDK 원문 message

권장 metric:

- 처리 outcome counter
- status별 backlog gauge
- oldest due age
- retry/reconciliation count
- lease lost count
- 처리 duration histogram

metric label에 withdrawalId·userId 같은 high-cardinality 값을 사용하지 않는다.

## 18. 예상 코드 구조

실제 구현 시 패키지 경계에 맞춰 조정한다.

```text
domain/user/application/
  UserWithdrawalExternalCleanupWorker.java
  FirebaseWithdrawalCleanupPort.java
  WithdrawalCleanupRetryPolicy.java
  WithdrawalCleanupTargetGuard.java
  WithdrawalCleanupOutcome.java

domain/user/domain/enums/
  WithdrawalCleanupFailureCode.java

domain/user/domain/repository/
  UserWithdrawalLifecycleRepositoryCustom.java
  UserWithdrawalLifecycleRepositoryImpl.java

domain/user/infrastructure/
  UserWithdrawalExternalCleanupProperties.java
  UserWithdrawalExternalCleanupConfiguration.java
  UserWithdrawalExternalCleanupScheduler.java

domain/auth/federation/infrastructure/firebase/
  FirebaseSdkWithdrawalCleanupAdapter.java
```

기존 `UserWithdrawalLifecycle`에는 상태 전이 invariant와 worker update에 필요한 값만 추가한다. SDK 객체나 exception은 entity에 넣지 않는다.

## 19. 테스트 계획

### domain

- 모든 허용·금지 상태 전이
- attempt·lease·completion 시각 invariant
- target 전체 null 또는 전체 present 불변식
- 안전한 failure code만 저장

### Repository integration

- pending·due retry·expired lease claim
- due 전 retry 미claim
- 정상 active lease 미claim
- 동시 claim 단일 승자
- claim별 token과 version 증가
- stale token·version의 retry/complete update 실패
- completed local handoff crash recovery
- worker claim index 이름·key 순서

### worker unit

- LOCAL·GUEST null target은 Firebase 미호출
- target guard mismatch는 Firebase 미호출·reconciliation
- disable→revoke→provider→delete→presence 순서
- 각 단계 `NOT_FOUND` 멱등 성공
- 각 단계 retryable failure와 backoff
- delete timeout 뒤 absent/present/unknown 분기
- max attempts reconciliation
- 단계 사이 lease lost 시 후속 Firebase call·상태 update 중단
- worker crash 후 expired lease 재처리

### Firebase adapter

- tenant-aware auth 선택
- project mismatch mutation 차단
- disable/revoke/delete SDK 호출
- `USER_NOT_FOUND` 안전 분류
- rate limit·timeout·permission·configuration 오류 분류
- 원문 message 비노출
- adapter/result `toString` redaction

### configuration·scheduler

- default disabled에서 worker·scheduler bean 없음
- enabled인데 Firebase disabled 또는 필수 설정 누락 시 startup failure
- batch size 제한과 `NONE` loop 종료
- 전체 설정 `toString`의 민감값 비노출

### regression

- Stage 1 Firebase·LOCAL·GUEST withdrawal
- cleanup pending Firebase exchange 차단
- 기존 Firebase login/signup/merge/auth-method sync
- phone eligibility publisher
- Security·OpenAPI에 신규 공개 endpoint 없음
- `./gradlew clean test`

실제 Firebase·Atlas를 단위 테스트에서 호출하지 않는다. SDK와 Repository는 mock 또는 인메모리 Mongo로 검증한다.

## 20. 구현 순서

1. failure code·outcome·retry policy domain 작성
2. lifecycle 상태 invariant와 claim/CAS custom Repository 작성
3. Repository 동시성·lease·version integration test 작성
4. cleanup port와 target guard 작성
5. worker를 fake port로 구현하고 단계별 실패 테스트 작성
6. Firebase SDK adapter 구현과 오류 mapping 테스트 작성
7. properties·configuration·scheduler 추가
8. metrics·구조화 로그와 redaction 검증
9. 전체 회귀 테스트
10. staging Emulator/실제 Firebase runbook 검증

## 21. 배포와 activation 순서

1. lifecycle index와 worker 코드를 `enabled=false`로 배포
2. 운영 Mongo에서 pending/retry/reconciliation 현황 read-only 확인
3. Firebase project·tenant·Admin 권한과 timeout 검증
4. Apple revoke 경로와 Provider별 의무 production gate 확정
5. staging에서 disable·revoke·delete, already deleted, timeout, lease crash E2E
6. production에서 cleanup worker만 먼저 활성화
7. backlog·retry·reconciliation·rate limit 관찰
8. worker가 안정적으로 동작할 때 `FIREBASE_WITHDRAWAL_ENABLED` 활성화
9. Stage 3 release worker 전에는 `CLEANED`·재가입 gate를 열지 않음

worker와 요청 endpoint를 동시에 처음 활성화하는 경우에도 worker health를 먼저 확인한다. endpoint만 단독 활성화하지 않는다.

## 22. staging 검증 시나리오

- Firebase User 정상 disable→revoke→delete
- 이미 disabled
- refresh token 이미 revoked
- 이미 deleted
- disable 성공 후 worker crash
- revoke 성공 후 timeout
- delete request timeout 후 실제 absent
- delete request timeout 후 여전히 present
- 두 instance 동시 claim
- lease 만료 전·후 takeover
- stale worker completion 거절
- wrong project·wrong FirebaseIdentity owner mutation 0건
- Firebase permission denied·rate limit·unavailable
- Apple provider obligation material 있음·없음
- lifecycle completed 후 Stage 3 handoff 대기

검증 증적에는 credential·UID·email·phone과 SDK 원문 오류를 포함하지 않는다.

## 23. 완료 조건

다음 조건을 모두 만족해야 `TMI-104` 구현 완료로 본다.

- atomic claim·lease renewal·expired lease recovery·version fencing 구현
- disable→refresh revoke→Provider 의무→delete 순서 구현
- 이미 삭제된 User와 각 단계 재실행의 멱등 수렴
- delete 결과 불명 presence 확인과 retry/reconciliation 구현
- target mismatch 시 Firebase mutation 0건
- 외부 삭제 확인 후에만 `IDENTITY_RELEASE_PENDING` 인계
- worker·scheduler 기본 비활성 및 설정 fail-closed
- 민감정보 비저장·비로그·비metric 검증
- Repository·worker·adapter·configuration·scheduler 테스트 통과
- `./gradlew clean test` 통과
- staging Firebase·Mongo lease·timeout E2E와 운영 runbook 완료
- Apple 포함 활성 Provider의 삭제 의무가 모두 검증되기 전 해당 Provider production 탈퇴 비활성

## 24. Stage 3 인계

Stage 3은 `IDENTITY_RELEASE_PENDING`만 claim한다.

인계 정보:

- `withdrawalId`
- canonical userId
- external deletion이 확인된 `externalDeletedAt`
- lifecycle version
- Firebase target snapshot

Stage 3은 다음을 수행한다.

- FirebaseIdentity·SocialIdentity release/delete
- PhoneIdentity·PhoneFingerprintAlias release
- 필요하면 eligibility revoke 전달 상태 확인
- `CLEANED` 전환
- CLEANED 이후 동일 credential의 신규 enrollment gate 개방

Stage 2는 위 작업을 선행하거나 부분 실행하지 않는다.
