# 6단계 구현 계획: 가입 중단 Firebase User cleanup

## 1. 목적

이 단계의 목표는 Firebase 인증 또는 전화번호 연결까지 진행했지만 Identity의 회원가입·Guest 승격 Transaction을 완료하지 않은 사용자가 외부 Firebase에만 남는 문제를 안전하게 해결하는 것이다.

대상은 내부 canonical `User`가 없는 DIRECT_SIGNUP 또는 아직 Firebase identity를 소유하지 않은 Guest 승격 준비 과정이다. 가입 완료 후 탈퇴한 회원은 이미 구현된 `UserWithdrawalLifecycle`이 처리하므로 이 단계에서 다시 다루지 않는다.

Stage 6은 다음 결과를 보장해야 한다.

- 짧은 enrollment TTL이 끝나도 일정 시간 동안 가입을 재개할 수 있다.
- 재개 유예가 끝난 고아 Firebase User만 disable·refresh revoke·provider obligation·delete한다.
- 가입 finalize와 cleanup이 경쟁해도 한쪽만 승리한다.
- 내부 User·FirebaseIdentity·SocialIdentity·PhoneIdentity owner가 있는 계정은 자동 삭제하지 않는다.
- 외부 삭제 성공 여부를 알 수 없으면 존재 여부를 다시 확인하고, 추측으로 재가입이나 삭제를 진행하지 않는다.
- cleanup 완료 뒤 이전 enrollment와 Firebase credential을 재사용하지 않고 fresh 인증부터 다시 시작한다.

## 2. 기존 구현과 문제

현재 `FirebaseEnrollmentAttempt`는 다음 상태와 시간을 가진다.

```text
PENDING → CONSUMED
   └──→ EXPIRED

enrollmentTtl = PT10M
cleanupAt = expiresAt + enrollmentCleanupRetention
enrollmentCleanupRetention = PT24H
```

현재 동작은 다음과 같다.

- 같은 Firebase project·UID·binding의 유효한 PENDING attempt를 재사용한다.
- 만료된 PENDING attempt는 조건부로 `EXPIRED` 전환한다.
- DIRECT_SIGNUP과 GUEST_USER finalize Transaction은 attempt consume과 User·identity·Session 저장을 함께 commit한다.
- `cleanupAt` Mongo TTL은 attempt 문서만 제거한다.
- attempt 만료 또는 TTL 삭제는 Firebase User를 disable·revoke·delete하지 않는다.

따라서 다음과 같은 고아 상태가 남을 수 있다.

```text
Firebase 인증 성공
→ 같은 Firebase UID에 phone/provider link 성공
→ 앱 종료·네트워크 단절·응답 유실
→ Mongo finalize 미실행
→ Firebase User와 phone credential만 외부에 잔존
```

현재 `PT24H`는 문서 retention이지 외부 계정 삭제 grace가 아니다. 이를 cleanup grace로 그대로 사용하면 Mongo TTL 삭제와 worker claim이 같은 시점에 경쟁하므로 두 설정을 반드시 분리해야 한다.

## 3. 포함 범위

- DIRECT_SIGNUP과 GUEST_USER enrollment의 가입 중단 판정
- Firebase project·UID 단위의 별도 abandoned cleanup lifecycle
- attempt 생성·재사용·consume과 cleanup lifecycle의 Transaction 조정
- enrollment 만료 후 재개 grace
- grace 만료 lifecycle의 atomic claim·lease·generation fencing
- 신규 exchange·Guest prepare와 cleanup worker의 경쟁 차단
- User·FirebaseIdentity·SocialIdentity·PhoneIdentity owner preflight
- exact Firebase account inspect
- Firebase User disable·refresh token revoke·provider deletion obligation·delete
- 삭제 결과 존재 여부 재확인
- retry·dead-letter 성격의 reconciliation 상태
- lifecycle·attempt record retention과 TTL index
- 기본 비활성 scheduler, metric, 구조화 로그와 runbook
- 기존 attempt에 대한 bounded dry-run·capture backfill
- 모바일의 가입 재시작 오류 계약
- domain·Repository·Transaction·worker·configuration·integration 테스트

## 4. 제외 범위

- 가입 완료 회원의 withdrawal lifecycle 재구현
- 내부 `User` hard delete 또는 WITHDRAWN User 복구
- ACTIVE·SUSPENDED·WITHDRAWN·MERGED 회원의 Firebase identity 정리
- phone을 기준으로 기존 User를 찾거나 자동 merge하는 기능
- Provider unlink·전화번호 변경
- logout-all Firebase refresh revoke
- Billing Entitlement consumer
- Guest 생성 응답 유실 복구
- 기존 ACTIVE 회원의 Firebase rebind
- 공개 cleanup 실행·조회 API 또는 운영자 UI
- Firebase 전체 User 목록을 열거하는 무제한 정리 작업

## 5. 시간 계약

기본값은 다음과 같이 분리한다.

```text
enrollmentTtl                     = PT10M  기존 유지
abandonedCleanupGrace             = PT24H  신규
terminalLifecycleRetention        = P7D    신규
terminalEnrollmentAttemptRetention = PT24H 신규
workerFixedDelay                  = PT5S
leaseDuration                     = PT1M
initialBackoff                    = PT5S
maxBackoff                        = PT1H
maxAttempts                       = 12
```

`graceUntil`은 마지막 PENDING attempt의 `expiresAt + abandonedCleanupGrace`로 계산한다. 최초 attempt 기준으로는 기본 10분의 enrollment TTL이 지난 뒤 24시간 동안 재개할 수 있다.

유예기간 안에 사용자가 다시 exchange·Guest prepare를 수행하면 다음 규칙을 적용한다.

- 기존 attempt가 아직 PENDING·미만료이면 동일 attempt를 재사용한다.
- 기존 attempt가 만료됐고 cleanup이 claim되기 전이면 새 attempt를 만들고 lifecycle generation을 증가시킨다.
- 새 attempt의 `expiresAt`을 기준으로 `graceUntil`을 다시 계산한다.
- cleanup이 이미 IN_PROGRESS이면 새 attempt를 만들지 않는다.

반복 exchange가 grace를 계속 연장할 수는 있지만, 유효한 Firebase proof를 가진 사용자가 명시적으로 가입을 재개한 경우에만 허용한다. scheduler나 TTL 관찰만으로 grace를 연장하지 않는다.

`terminalLifecycleRetention`과 `terminalEnrollmentAttemptRetention`은 Firebase 삭제 유예나 로그인 허용 시간이 아니다. FINALIZED·CLEANED 이후 늦은 요청 분류, bounded capture 중복 방지와 장애 확인을 위한 짧은 운영 이력 보존값이다.

TTL은 생성 시점에 미리 고정하지 않고 상태가 terminal에 도달한 시각을 기준으로 설정한다.

- RESUMABLE·CLEANUP_IN_PROGRESS·RETRY_WAIT: lifecycle과 attempt 모두 `cleanupAt` 없음
- FINALIZED·CLEANED: lifecycle `cleanupAt = terminalAt + P7D`
- FINALIZED·CLEANED의 source attempt: `cleanupAt = terminalAt + PT24H`
- RECONCILIATION_REQUIRED: 운영 해결 전 lifecycle과 관련 attempt 모두 `cleanupAt` 없음
- reconciliation 해결 후 FINALIZED·CLEANED로 전환한 경우: 해결 시각을 terminalAt으로 사용

source attempt가 lifecycle보다 먼저 삭제되므로 동일 TTL 시각의 삭제 순서 경쟁을 피한다. 7일과 24시간은 운영 기본값이며 법적 보존기간이나 기능상 최소값이 아니다. 관측·지원 요구가 달라지면 lifecycle이 attempt보다 오래 남는 관계를 유지한 채 조정한다.

## 6. 데이터 모델

withdrawal lifecycle과 분리된 `AbandonedFirebaseEnrollmentCleanup` collection을 추가한다.

예상 필드는 다음과 같다.

```text
cleanupId
firebaseProjectId
firebaseUid
generation
bindingType
boundUserId nullable
sourceEnrollmentId
status
graceUntil
nextAttemptAt nullable
attemptCount
leaseOwner nullable
leaseUntil nullable
lastErrorCode nullable
lastActivityAt
externalDeletedAt nullable
completedAt nullable
cleanupAt nullable
createdAt
updatedAt
version
```

### 6.1 인덱스

- `(firebaseProjectId, firebaseUid)` unique
- `(status, graceUntil, updatedAt)` grace claim 조회
- `(status, nextAttemptAt, leaseUntil)` retry·expired lease claim 조회
- nullable `cleanupAt` TTL. Mongo TTL은 값이 없는 nonterminal·reconciliation 문서를 삭제하지 않는다.

같은 Firebase target에 DIRECT_SIGNUP과 GUEST_USER lifecycle을 동시에 두지 않는다. target-wide unique index가 최종 경쟁 차단선이다.

### 6.2 FirebaseEnrollmentAttempt 변경

- 기존 PENDING partial unique index와 binding 계약을 유지한다.
- 생성 시 `cleanupAt`을 두지 않는다.
- lifecycle이 FINALIZED·CLEANED가 되는 Transaction에서 exact target의 관련 attempt에 `cleanupAt = terminalAt + terminalEnrollmentAttemptRetention`을 설정한다.
- RECONCILIATION_REQUIRED와 아직 처리 중인 target의 attempt는 TTL로 제거하지 않는다.
- PENDING→EXPIRED와 PENDING→CONSUMED 의미는 유지한다.
- cleanup worker의 lease·retry 필드를 attempt에 추가하지 않는다.
- attempt는 사용자 흐름 proof이고 cleanup lifecycle은 외부 mutation 조정자다.

## 7. lifecycle 상태 계약

```text
RESUMABLE
   ├─ 새 attempt → RESUMABLE, generation + 1, graceUntil 갱신
   ├─ finalize Transaction 성공 → FINALIZED
   └─ grace 만료 atomic claim → CLEANUP_IN_PROGRESS

CLEANUP_IN_PROGRESS
   ├─ retryable failure → RETRY_WAIT
   ├─ unsafe·mixed state → RECONCILIATION_REQUIRED
   └─ Firebase 부재 확인 → CLEANED

RETRY_WAIT
   ├─ due atomic claim → CLEANUP_IN_PROGRESS
   └─ 최신 가입 활동 발견 → RECONCILIATION_REQUIRED

FINALIZED, CLEANED
   └─ terminal retention 뒤 TTL 삭제

RECONCILIATION_REQUIRED
   └─ 운영 해결 전 자동 TTL 없음
```

### `RESUMABLE`

- 유효한 exchange 또는 Guest prepare가 생성·갱신한다.
- `graceUntil` 전에는 worker가 claim하지 않는다.
- `graceUntil <= now`이어도 atomic claim 전까지 fresh proof를 가진 사용자가 새 generation을 시작할 수 있다.

### `CLEANUP_IN_PROGRESS`

- claim마다 고유 lease token을 발급한다.
- claim 시 `attemptCount`를 증가시키고 generation·version을 고정한다.
- 같은 target의 새 enrollment 생성과 기존 attempt consume을 거절한다.
- 만료된 lease는 동일 generation에 한해 재claim한다.

### `RETRY_WAIT`

- retry 가능한 안전한 오류 code와 `nextAttemptAt`만 저장한다.
- 원본 Firebase message나 credential을 저장하지 않는다.
- 같은 generation·version 조건으로만 다시 claim한다.

### `FINALIZED`

- DIRECT_SIGNUP 또는 Guest upgrade Transaction에서 User·identity 저장과 attempt consume이 성공할 때 같은 Mongo Transaction으로 전환한다.
- worker가 이 target을 자동 삭제하지 않는다.
- 이후 owner mapping이 사라져도 Stage 6이 추정 삭제하지 않는다.

### `CLEANED`

- Firebase User가 존재하지 않음을 확인한 경우에만 전환한다.
- 이전 enrollmentId와 generation은 다시 사용할 수 없다.
- 전환 시 lifecycle과 source attempt의 서로 다른 `cleanupAt`을 함께 기록한다.
- source attempt가 먼저 제거되고 lifecycle은 기본 7일 동안 중복 방지 기록으로 남는다.

### `RECONCILIATION_REQUIRED`

- owner·generation·binding 불일치, partial state, 권한·설정 오류, 최대 retry 초과처럼 자동 판단이 안전하지 않은 경우 사용한다.
- 자동 재claim하지 않는다.
- 운영 확인 없이 `RESUMABLE`이나 `CLEANED`로 변경하지 않는다.
- 미해결 상태 자체가 안전 gate이므로 `cleanupAt`을 설정하지 않는다.
- 운영 해결로 FINALIZED·CLEANED가 된 시점부터 terminal retention을 계산한다.

## 8. enrollment와 cleanup의 원자적 조정

### 8.1 startOrReuse

현재 `FirebaseEnrollmentAttemptService.startOrReuse`를 단일 Repository save 흐름으로 두지 않고 짧은 Mongo Transaction coordinator를 추가한다.

Transaction은 다음 순서로 처리한다.

1. exact project·UID lifecycle을 조회한다.
2. `CLEANUP_IN_PROGRESS`, `RETRY_WAIT`, `CLEANED`, `RECONCILIATION_REQUIRED`면 새 attempt를 만들지 않고 전용 오류를 반환한다.
3. FINALIZED인데 canonical owner가 없다면 자동 재개하지 않고 reconciliation 오류로 처리한다.
4. active PENDING attempt가 있으면 그대로 반환한다.
5. expired PENDING attempt를 조건부 EXPIRED로 바꾼다.
6. 새 PENDING attempt를 저장한다.
7. lifecycle이 없으면 RESUMABLE generation 1로 생성한다.
8. 기존 RESUMABLE이면 generation을 증가시키고 sourceEnrollmentId·binding·graceUntil을 새 attempt 기준으로 갱신한다.

unique conflict가 발생하면 제한 횟수만 재조회한다. lifecycle target unique와 PENDING binding partial unique가 최종 승자를 결정한다.

### 8.2 finalize consume

DIRECT_SIGNUP과 Guest upgrade Transaction은 기존 aggregate 저장과 attempt consume 외에 lifecycle FINALIZED CAS를 포함한다.

CAS 조건은 최소 다음과 같다.

- exact project·UID
- lifecycle status `RESUMABLE`
- sourceEnrollmentId 일치
- generation 일치
- attempt status PENDING·미만료

한 조건이라도 실패하면 User·identity·Session 저장 전체를 rollback하고 `FIREBASE_ENROLLMENT_CONFLICT` 또는 restart-required 오류로 변환한다.

### 8.3 cleanup claim과의 경합

cleanup claim은 lifecycle의 RESUMABLE→CLEANUP_IN_PROGRESS CAS다. finalize는 RESUMABLE→FINALIZED CAS다. 동일 lifecycle version에서 둘 중 하나만 성공한다.

- finalize 승리: worker claim 실패, Firebase User 유지
- cleanup claim 승리: finalize 전체 rollback, client는 가입 재시작 안내
- 외부 호출 시작 뒤에는 새 attempt를 허용하지 않음

Mongo TTL은 동시성 제어 수단으로 사용하지 않는다.

## 9. DIRECT_SIGNUP과 GUEST_USER 정책

### 9.1 DIRECT_SIGNUP

삭제 가능 후보는 다음을 모두 만족해야 한다.

- source attempt가 만료·미소비
- lifecycle grace 만료
- lifecycle claim과 generation·lease 유효
- FirebaseIdentity target owner 없음
- linked SocialIdentity owner 없음
- verified phone의 ACTIVE alias owner 없음
- canonical User 또는 완료된 signup aggregate 없음

### 9.2 GUEST_USER

bound guest User 자체는 삭제하지 않는다. Firebase account만 아직 Guest 소유로 finalize되지 않은 외부 credential 후보로 취급한다.

자동 cleanup은 다음을 추가로 확인한다.

- boundUserId가 lifecycle snapshot과 일치
- User가 존재하고 여전히 `ACTIVE GUEST`
- 해당 Guest에 FirebaseIdentity가 없음
- Guest가 MEMBER·WITHDRAWN·MERGED·SUSPENDED로 바뀌지 않음
- 동일 target을 다른 User가 소유하지 않음

bound User 상태가 달라졌거나 사라졌으면 Firebase User를 자동 삭제하지 않고 reconciliation으로 보낸다. Guest User와 Guest RefreshSession은 Stage 6에서 변경하지 않는다.

## 10. owner preflight와 외부 inspect

Firebase mutation 전 다음 순서를 적용한다.

1. lifecycle claim·lease·generation·version 재검증
2. 최신 PENDING·CONSUMED attempt와 lifecycle FINALIZED 여부 확인
3. FirebaseIdentity의 target owner와 user owner 확인
4. bound Guest User 상태 확인
5. Firebase account inspect
6. inspect에서 얻은 linked provider subject의 SocialIdentity owner 확인
7. verified phone을 memory에서 정규화·fingerprint한 뒤 ACTIVE PhoneFingerprintAlias owner 확인
8. 모든 owner가 없고 상태가 일관될 때만 외부 mutation 진행

inspect 결과의 email·phone·provider subject는 lifecycle, 로그, metric, WORKLOG에 저장하지 않는다. owner 조회에 필요한 동안 memory에서만 사용하고 안전한 enum·boolean 결과만 worker로 반환한다.

다음 상태에서는 자동 삭제하지 않는다.

- FirebaseIdentity가 하나라도 exact target을 소유
- linked SocialIdentity가 내부 User를 소유
- verified phone fingerprint가 ACTIVE PhoneIdentity를 가리킴
- FINALIZED lifecycle 또는 CONSUMED attempt가 존재
- 새로운 RESUMABLE generation이 존재
- bound Guest가 더 이상 expected 상태가 아님
- repository 일부만 존재하는 mixed state
- project·UID·binding snapshot 불일치

owner가 존재하면 단순 성공 처리하지 않는다. 잘못 생성된 cleanup lifecycle일 수 있으므로 `RECONCILIATION_REQUIRED`에 남겨 원인을 확인한다.

## 11. Firebase cleanup port

Stage 2의 withdrawal lifecycle Entity·Repository·guard는 재사용하지 않는다. 다만 Firebase SDK mutation 자체는 공통 low-level port로 추출하거나 기존 adapter의 안전한 primitive를 공유한다.

예상 application port는 다음과 같다.

```java
public interface AbandonedFirebaseUserCleanupPort {
    AbandonedFirebaseAccountSnapshot inspect(String projectId, String firebaseUid);
    void disable(String projectId, String firebaseUid);
    void revokeRefreshTokens(String projectId, String firebaseUid);
    void satisfyProviderDeletionObligations(AbandonedFirebaseAccountSnapshot snapshot);
    void delete(String projectId, String firebaseUid);
    FirebaseAccountPresence checkPresence(String projectId, String firebaseUid);
}
```

공통 SDK adapter가 두 application port를 구현할 수는 있지만 Stage 6이 `UserWithdrawalLifecycle`이나 withdrawal 전용 failure enum에 의존해서는 안 된다.

원격 작업은 다음처럼 멱등 처리한다.

- 이미 disabled: 성공
- refresh token이 이미 revoked: 성공
- User not found: 삭제 완료
- delete timeout·connection reset: 존재 여부 확인
- 존재 확인 결과 ABSENT: `CLEANED`
- 존재 확인 결과 PRESENT: retry 또는 reconciliation
- 존재 확인 자체 실패: retry

## 12. 오류 분류와 재시도

### retryable

- timeout
- connection failure
- Firebase unavailable
- rate limited
- delete result unknown이지만 존재 확인도 일시 실패

retryable 오류는 exponential backoff와 jitter를 적용하고 최대 12회까지만 자동 재시도한다.

### reconciliation required

- owner·binding·generation mismatch
- lifecycle·attempt partial state
- Firebase project mismatch
- permission denied
- configuration error
- provider deletion obligation을 안전하게 완료할 수 없음
- delete 뒤 account가 계속 존재하고 최대 시도 초과
- 최대 retry 횟수 초과

저장 가능한 `lastErrorCode`는 사전에 정의한 enum만 사용한다. SDK exception class·message·stack trace·identifier를 상태 문서와 metric tag로 사용하지 않는다.

## 13. 사용자·모바일 오류 계약

cleanup claim 전 grace 안에서는 기존 공개 API로 가입을 재개한다. 별도 resume API는 만들지 않는다.

cleanup claim 이후 이전 Firebase ID Token이나 enrollmentId로 exchange·Guest prepare·signup·upgrade를 요청하면 다음 전용 오류를 반환한다.

```text
HTTP 409
code = FIREBASE_ENROLLMENT_RESTART_REQUIRED
message = 회원가입을 다시 시작해 주세요.
```

모바일은 이 오류를 terminal enrollment 오류로 처리한다.

1. 저장된 enrollmentId 제거
2. Firebase client signOut
3. 로컬 임시 가입 입력 제거
4. 인증 첫 화면으로 이동
5. 새 Firebase 인증 후 fresh enrollment 시작

Identity Access·Refresh Token을 이미 가진 Guest라면 Guest Session은 삭제하지 않는다. Guest upgrade 화면만 초기화하고 기존 Guest 이용 상태는 유지한다.

Firebase User 삭제가 이미 완료돼 ID Token 검증 단계에서 invalid·revoked로 거절되더라도 모바일의 최종 동작은 동일하게 fresh 인증 시작이다. Firebase SDK 원본 오류는 외부 응답에 노출하지 않는다.

## 14. legacy attempt capture와 backfill

Stage 6 배포 전 생성된 attempt에는 cleanup lifecycle이 없다. 이를 위해 bounded capture 작업을 둔다.

원칙은 다음과 같다.

- dry-run 우선
- 고정된 cutover 상한·하한 사용
- 한 번에 최대 100건
- source는 아직 보존된 PENDING 만료 또는 EXPIRED attempt로 한정
- CONSUMED attempt 제외
- FirebaseIdentity·SocialIdentity·PhoneIdentity owner가 확인되면 제외
- 같은 target의 최신 attempt 하나를 기준으로 lifecycle 생성
- 이미 lifecycle이 있으면 멱등 skip
- Firebase 전체 User directory를 열거하지 않음
- capture와 external delete worker를 동시에 처음 활성화하지 않음

capture 대상 nonterminal attempt는 lifecycle 생성과 같은 Transaction에서 기존 `cleanupAt`을 제거해 처리 도중 TTL 삭제되지 않도록 한다. terminal 전환 시에만 새 retention 값을 설정한다.

기존 `cleanupAt`이 지나 이미 TTL 삭제된 attempt는 안전한 source가 없으므로 자동 추정 삭제하지 않는다. Firebase 전체 스캔으로 보완하지 않고 운영 현황만 집계한다.

배포 순서는 다음과 같다.

1. lifecycle schema·index와 enrollment coordinator 배포
2. 신규 attempt lifecycle capture 활성화
3. 기존 attempt bounded dry-run
4. 결과 검토 후 bounded write capture
5. 모바일 restart-required 처리 확인
6. worker staging 활성화
7. 실제 Firebase·Mongo replica set 경쟁 E2E
8. production worker 별도 승인

## 15. scheduler·설정·관측

모든 신규 기능은 기본 비활성으로 둔다.

예상 환경변수는 다음과 같다.

```text
FIREBASE_ABANDONED_CLEANUP_CAPTURE_ENABLED=false
FIREBASE_ABANDONED_CLEANUP_WORKER_ENABLED=false
FIREBASE_ABANDONED_CLEANUP_GRACE=PT24H
FIREBASE_ABANDONED_CLEANUP_TERMINAL_RETENTION=P7D
FIREBASE_ENROLLMENT_TERMINAL_RETENTION=PT24H
FIREBASE_ABANDONED_CLEANUP_FIXED_DELAY=PT5S
FIREBASE_ABANDONED_CLEANUP_LEASE_DURATION=PT1M
FIREBASE_ABANDONED_CLEANUP_MAX_ATTEMPTS=12
FIREBASE_ABANDONED_CLEANUP_INITIAL_BACKOFF=PT5S
FIREBASE_ABANDONED_CLEANUP_MAX_BACKOFF=PT1H
FIREBASE_ABANDONED_CLEANUP_MAX_BATCH_SIZE=20
```

metric은 안전한 상태·outcome·binding type만 tag로 사용한다.

```text
identity.firebase.enrollment.cleanup.claim
identity.firebase.enrollment.cleanup.outcome
identity.firebase.enrollment.cleanup.retry
identity.firebase.enrollment.cleanup.reconciliation
identity.firebase.enrollment.cleanup.backlog
identity.firebase.enrollment.cleanup.oldest_due_age
identity.firebase.enrollment.cleanup.delivery_lag
```

로그에 허용하는 값은 event name, outcome, attempt count, duration, safe error code다. enrollmentId, userId, Firebase UID, phone, email, provider subject와 Token은 기록하지 않는다.

## 16. Transaction·Repository 예상 계약

```java
Optional<AbandonedFirebaseEnrollmentCleanup> claimNext(
        String leaseToken,
        Instant now,
        Instant leaseUntil
);

boolean renewLease(
        String cleanupId,
        String leaseToken,
        long generation,
        long expectedVersion,
        Instant renewedUntil,
        Instant updatedAt
);

boolean scheduleRetry(...);

boolean markCleaned(...);

boolean markReconciliationRequired(...);

EnrollmentStartResult startOrReuseAndCoordinate(...);

boolean finalizeIfResumable(
        String cleanupId,
        String sourceEnrollmentId,
        long generation,
        long expectedVersion,
        Instant finalizedAt
);
```

claim query는 다음 후보 중 due time이 가장 오래된 한 건을 선택한다.

1. RESUMABLE이고 `graceUntil <= now`
2. RETRY_WAIT이고 `nextAttemptAt <= now`
3. CLEANUP_IN_PROGRESS이고 `leaseUntil <= now`

모든 worker update는 cleanupId·leaseOwner·generation·version·expected status를 조건에 포함한다. lease를 잃은 worker는 외부 단계 사이의 renewal 실패를 확인한 즉시 후속 mutation과 내부 상태 변경을 중단한다.

## 17. 구현 파일 예상 범위

예상 신규 영역:

```text
domain/auth/domain/entity/AbandonedFirebaseEnrollmentCleanup.java
domain/auth/domain/enums/AbandonedFirebaseEnrollmentCleanupStatus.java
domain/auth/federation/repository/AbandonedFirebaseEnrollmentCleanupRepository.java
domain/auth/federation/repository/...Custom.java
domain/auth/federation/repository/...Impl.java
domain/auth/federation/application/FirebaseEnrollmentCoordinationTransactionService.java
domain/auth/federation/application/AbandonedFirebaseEnrollmentCleanupWorker.java
domain/auth/federation/application/AbandonedFirebaseEnrollmentTargetGuard.java
domain/auth/federation/application/AbandonedFirebaseUserCleanupPort.java
domain/auth/federation/infrastructure/firebase/...Adapter.java
domain/auth/federation/infrastructure/...Configuration.java
domain/auth/federation/infrastructure/...Scheduler.java
```

예상 변경 영역:

- `FirebaseEnrollmentAttempt`
- `FirebaseEnrollmentAttemptService`
- `FirebaseExchangeService`
- `FirebaseGuestPrepareService`
- `FirebaseSignupTransactionService`
- `FirebaseGuestUpgradeTransactionService`
- Firebase 오류 enum·Global error/OpenAPI 계약
- Firebase authentication configuration과 application.yml
- Mongo index·mapping·repository integration 테스트

실제 package는 기존 `_class` discriminator와 repository scan 호환성을 유지한다. 기존 entity FQCN을 이동하지 않는다.

## 18. 테스트 계획

### 18.1 domain·Repository

- grace·retention 양수·overflow 검증
- lifecycle target unique index
- RESUMABLE oldest-due claim
- retry due 전 미claim
- expired lease 재claim
- generation·version·lease mismatch update 거절
- terminal TTL index 확인
- active·retry·reconciliation lifecycle에 cleanupAt이 설정되지 않음
- FINALIZED·CLEANED lifecycle에 terminalAt+P7D cleanupAt 설정
- terminal source attempt에 terminalAt+PT24H cleanupAt 설정
- source attempt TTL이 lifecycle TTL보다 먼저 도래함
- attempt PENDING partial unique 회귀
- attempt record가 grace 전에 TTL 삭제되지 않음

### 18.2 enrollment coordinator

- active attempt 재사용
- expired attempt 교체와 generation 증가
- grace 안 fresh exchange 재개
- cleanup claim 뒤 새 attempt 거절
- finalize와 cleanup claim 동시 실행에서 단일 승자
- DIRECT_SIGNUP finalize와 lifecycle FINALIZED 원자 commit
- Guest upgrade와 lifecycle FINALIZED 원자 commit
- 응답 유실 후 FINALIZED 상태에서 중복 finalize가 새 User를 만들지 않음

### 18.3 owner guard

- FirebaseIdentity target owner 존재 시 삭제 금지
- User owner와 target owner 불일치 시 reconciliation
- linked SocialIdentity 0개·1개·여러 개 owner 검사
- verified phone ACTIVE alias owner 존재 시 삭제 금지
- no PhoneIdentity지만 alias만 남은 partial state 차단
- bound Guest ACTIVE GUEST만 허용
- Guest MEMBER·WITHDRAWN·MERGED·SUSPENDED·missing 차단
- 새 generation 발견 시 이전 worker 차단

### 18.4 Firebase worker

- inspect→disable→revoke→provider obligation→delete→absence confirm 순서
- 이미 삭제된 Firebase User 멱등 성공
- disable·revoke 반복 성공
- delete timeout 뒤 ABSENT 확인 성공
- delete timeout 뒤 PRESENT이면 retry
- Firebase unavailable·rate limit backoff
- permission·configuration 오류 reconciliation
- 최대 retry 초과 reconciliation
- lease renewal 실패 후 후속 호출 중단
- 이전 generation worker가 최신 lifecycle 변경 불가

### 18.5 API·모바일 계약

- grace 안 exchange·Guest prepare 재개
- cleanup claim 이후 `409 FIREBASE_ENROLLMENT_RESTART_REQUIRED`
- Guest Session은 restart-required에서도 유지
- Firebase raw error·identifier 비노출
- 기존 signup·Guest upgrade 응답 shape 회귀 없음
- disabled flag에서 기존 동작 유지

### 18.6 staging E2E

- 실제 Firebase Emulator 또는 격리 project에서 direct signup 중단 후 grace cleanup
- phone link 뒤 중단한 Firebase User 삭제와 동일 번호 fresh 가입
- Guest upgrade 중단 뒤 Guest Session 유지와 Firebase User cleanup
- finalize·worker 동시 실행
- worker 외부 성공 후 내부 상태 저장 실패·재시작 수렴
- 다중 instance claim 단일 승자
- Mongo replica set Transaction rollback
- 모바일 앱 종료·재시작·응답 유실 흐름

## 19. 완료 조건

- enrollment TTL·cleanup grace·terminal lifecycle retention·terminal attempt retention이 서로 다른 설정과 필드로 구현됨
- active·retry·reconciliation record에는 TTL이 없고 FINALIZED·CLEANED에서만 terminal 기준 `cleanupAt`이 설정됨
- source attempt가 lifecycle보다 먼저 정리되어 bounded capture가 terminal cleanup을 다시 만들지 않음
- attempt 생성·재사용·consume과 lifecycle generation이 Transaction으로 조정됨
- finalize와 cleanup claim의 단일 승자 CAS가 검증됨
- DIRECT_SIGNUP·GUEST_USER owner preflight가 구현됨
- Firebase User 삭제 전에 Firebase·Social·Phone owner 부재를 검증함
- disable·refresh revoke·provider obligation·delete·presence 확인이 멱등 수렴함
- retry·reconciliation·lease recovery와 안전한 관측이 구현됨
- 이전 attempt bounded capture와 dry-run 절차가 준비됨
- 모바일이 restart-required 오류를 처리함
- 전체 `./gradlew clean test`와 `git diff --check`가 통과함
- 실제 Firebase/mobile·Mongo replica set·multi-instance staging E2E가 통과함
- runbook·feature flag 활성화·중단·reconciliation 절차가 준비됨

코드와 테스트만 병합됐다는 이유로 production worker를 활성화하지 않는다. 위 운영 조건과 staging E2E가 모두 확인된 뒤 별도로 승인한다.

## 20. 구현 순서

1. 상태·시간·오류 enum과 lifecycle Entity·index
2. lifecycle atomic Repository·lease·generation CAS
3. enrollment startOrReuse coordinator와 attempt retention 변경
4. DIRECT_SIGNUP·Guest upgrade finalize Transaction fencing
5. 중앙 API gate와 restart-required 오류 계약
6. exact Firebase inspect와 Firebase·Social·Phone owner preflight
7. 공통 Firebase cleanup adapter 경계 정리
8. worker disable·revoke·provider obligation·delete·confirm
9. retry·reconciliation·scheduler·metric·runbook
10. legacy attempt dry-run·bounded capture
11. 전체 회귀 테스트
12. 모바일·Firebase·Mongo replica set·multi-instance staging E2E

## 21. 후속 단계와의 경계

Stage 6 완료 뒤 다음 순서는 Billing 최소 Entitlement consumer 배포다.

Stage 6은 phone credential 점유를 해제할 수 있지만 무료 모의고사 1회 사용 이력이나 Entitlement를 소유하지 않는다. phone 기준 혜택의 재지급 방지는 Billing이 가진 abuse ledger와 TrialClaim 정책이 담당한다.

logout-all Firebase refresh revoke, Provider unlink·전화번호 변경, Guest 응답 유실 복구와 ACTIVE 회원 rebind도 각각 후속 단계로 유지한다.
