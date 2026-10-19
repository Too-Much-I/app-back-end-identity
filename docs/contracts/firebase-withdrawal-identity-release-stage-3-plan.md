# 3단계 구현 계획: Identity release와 CLEANED 재가입 gate

## 1. 목적

이 단계의 목적은 Stage 2가 Firebase 외부 User 삭제를 확인한 withdrawal lifecycle을 이어받아, 탈퇴한 User가 점유하던 내부 인증 identity를 안전하게 해제하고 lifecycle을 `CLEANED`로 완료하는 것이다.

Stage 3 완료 전에는 동일 Firebase 계정, SNS provider subject 또는 전화번호를 이용한 신규 enrollment를 허용하지 않는다. 모든 내부 점유 해제가 한 MongoDB Transaction으로 commit된 뒤에만 신규 Firebase enrollment와 새 canonical UUID User 생성을 허용한다.

이 단계는 기존 User를 복구하거나 새 User에 과거 데이터를 연결하지 않는다. 기존 User는 `WITHDRAWN` tombstone으로 유지하고, 재가입은 fresh Firebase proof·전화번호 재검증·필수 동의를 거치는 완전한 신규 가입이다.

Jira: `TMI-107`

## 2. 선행 상태와 현재 문제

Stage 2 완료 상태는 다음과 같다.

- User: `WITHDRAWN`
- 내부 RefreshSession: 모두 폐기
- Firebase 외부 User: 삭제 확인 완료
- lifecycle: `IDENTITY_RELEASE_PENDING`
- `externalDeletedAt`: 존재
- FirebaseIdentity: 기존 User 점유 유지
- SocialIdentity: 기존 User 점유 유지
- PhoneIdentity·PhoneFingerprintAlias: ACTIVE 점유가 남아 있을 수 있음
- 기존 active eligibility binding: Stage 1에서 REVOKED revision·outbox 생성 대상

현재 내부 identity가 남아 있어 다음 충돌이 발생할 수 있다.

- 같은 Firebase project·UID는 FirebaseIdentity unique index에 걸린다.
- 같은 provider·subject는 SocialIdentity unique index에 걸린다.
- 같은 검증 전화번호는 ACTIVE PhoneFingerprintAlias unique index에 걸린다.
- 같은 User의 새 ACTIVE PhoneIdentity는 userId partial unique index에 걸린다.
- cleanup 중 새 Firebase UID가 같은 SNS subject를 제시하면 `SOCIAL_IDENTITY_CONFLICT`가 반환될 수 있다.
- cleanup 중 같은 전화번호로 signup을 진행하면 `PHONE_ALREADY_LINKED`가 반환될 수 있다.

따라서 단순히 lifecycle만 `CLEANED`로 바꾸거나 identity를 하나씩 비원자적으로 삭제해서는 안 된다.

## 3. 핵심 안전 원칙

### 3.1 외부 삭제 확인 전에는 내부 점유를 해제하지 않는다

다음 조건이 모두 확인돼야 identity release를 시작한다.

- lifecycle status가 `IDENTITY_RELEASE_PENDING`
- `externalDeletedAt`이 존재
- User가 존재하고 `WITHDRAWN`
- lifecycle의 target과 FirebaseIdentity owner가 일치하거나, 이미 전체 release가 완료된 멱등 상태
- lifecycle version이 worker가 읽은 version과 일치

외부 Firebase User 삭제가 확정되지 않은 상태에서 내부 mapping을 먼저 지우면 기존 credential이 새 User에 연결될 수 있으므로 금지한다.

### 3.2 모든 내부 변경은 한 MongoDB Transaction으로 처리한다

다음 변경은 하나의 `mongoTransactionManager` Transaction으로 commit한다.

- active eligibility binding의 REVOKED 보장
- FirebaseIdentity 제거
- SocialIdentity 제거
- PhoneIdentity `RELEASED` 전환
- PhoneFingerprintAlias `RELEASED` 전환
- lifecycle `identitiesReleasedAt`·`cleanedAt` 기록
- lifecycle `IDENTITY_RELEASE_PENDING → CLEANED` 전환

중간 단계에서 오류가 발생하면 전체 rollback한다. Firebase·HTTP 등 외부 네트워크 호출은 이 Transaction에 포함하지 않는다.

### 3.3 `CLEANED`는 모든 내부 점유 해제의 commit marker다

`CLEANED`는 단순 표시값이 아니다. 다음 불변식이 한 commit에서 성립했다는 증거다.

- 기존 FirebaseIdentity가 더 이상 unique key를 점유하지 않음
- 기존 SocialIdentity가 더 이상 provider·subject를 점유하지 않음
- 기존 PhoneIdentity와 alias가 ACTIVE 상태가 아님
- active eligibility binding이 남아 있지 않음
- 기존 User는 계속 `WITHDRAWN`

클라이언트 요청이나 시간 경과만으로 `CLEANED`를 만들지 않는다.

### 3.4 재가입은 기존 계정 복구가 아니다

`CLEANED` 이후 동일 SNS·전화번호를 다시 사용하더라도 다음 원칙을 유지한다.

- 새 Firebase User 또는 새 Firebase credential proof를 검증한다.
- 전화번호를 다시 검증한다.
- 필수 동의를 다시 제출한다.
- 새 canonical UUID User를 생성한다.
- 기존 WITHDRAWN User를 ACTIVE로 되돌리지 않는다.
- 기존 프로필·시험·결과·RefreshSession을 새 User에 자동 연결하지 않는다.
- 전화번호가 같다는 이유로 자동 merge하지 않는다.

## 4. 포함 범위

- `IDENTITY_RELEASE_PENDING` lifecycle 조회와 version fencing
- User·lifecycle·FirebaseIdentity·SocialIdentity·PhoneIdentity target preflight
- FirebaseIdentity 내부 mapping 제거
- SocialIdentity 내부 mapping 제거
- PhoneIdentity `RELEASED` 전환
- PhoneFingerprintAlias 전체 `RELEASED` 전환
- active eligibility binding의 REVOKED revision·outbox 보장
- `identitiesReleasedAt`·`cleanedAt` 기록
- `IDENTITY_RELEASE_PENDING → CLEANED` 조건부 전환
- 완전 release 상태의 멱등 완료
- 혼합·owner mismatch 상태의 reconciliation 분류
- cleanup 중 Firebase/SNS/phone owner 충돌의 `WITHDRAWAL_CLEANUP_PENDING` 통합
- CLEANED 이후 신규 Firebase enrollment 허용
- local worker·scheduler·batch·feature flag
- 구조화 로그·metric과 운영 상태 집계
- Transaction·Repository·application·gate·scheduler 테스트
- staging MongoDB Transaction·동시성·재가입 E2E 절차

## 5. 제외 범위

- User tombstone 물리 삭제
- 기존 User 재활성화 또는 계정 복구
- 기존 User와 신규 User 자동 merge
- Learning Core의 시험·시험 결과·사용자 데이터 이전 또는 삭제
- Billing TrialClaim·UserEntitlement 삭제 또는 재지급
- Firebase 외부 User disable·revoke·delete 재실행
- 가입 중단 Firebase User cleanup
- logout-all Firebase revoke
- Provider unlink·전화번호 변경
- Guest 응답 유실 복구
- 기존 ACTIVE 회원 Firebase rebind
- 공개 cleanup 조회·수동 재시도 API
- 운영자 reconciliation UI

## 6. 데이터별 처리 정책

### 6.1 User

User 문서는 물리 삭제하지 않는다.

- status는 `WITHDRAWN` 유지
- 기존 `userId` 유지
- 탈퇴 시각과 tombstone 유지
- email·password 등 Stage 1에서 제거된 credential은 복구하지 않음

User tombstone은 과거 계정 경계와 withdrawal lifecycle owner를 판별하는 기준이다.

### 6.2 FirebaseIdentity

Firebase 외부 User 삭제가 확인된 뒤 내부 FirebaseIdentity mapping을 물리 삭제한다.

현재 FirebaseIdentity에는 status·releasedAt이 없고 다음 unconditional unique index가 있다.

- `(firebaseProjectId, firebaseUid)` unique
- `userId` unique

Stage 3에서는 별도 status·partial index migration을 추가하지 않고 exact owner 검증 후 mapping을 삭제한다. 외부 Firebase User는 이미 Stage 2에서 삭제됐으므로 내부 broker mapping을 장기 보존하지 않는다.

### 6.3 SocialIdentity

탈퇴 User가 소유한 SocialIdentity를 모두 물리 삭제한다.

현재 `(provider, providerSubject)`가 unconditional unique key이므로 삭제해야 동일 SNS credential의 신규 가입이 가능하다. provider subject를 lifecycle이나 audit 문서로 복사하지 않는다.

### 6.4 PhoneIdentity

PhoneIdentity는 기존 lifecycle을 사용해 soft release한다.

- `ACTIVE → RELEASED`
- `releasedAt`·`updatedAt` 기록
- entity version 기반 optimistic locking 유지

PhoneIdentity 문서는 fingerprint key rotation과 과거 ownership 정합성을 위해 보존하되 ACTIVE uniqueness에서는 제외한다.

### 6.5 PhoneFingerprintAlias

해당 PhoneIdentity의 모든 ACTIVE alias를 `RELEASED`로 변경한다.

- fingerprint 원문을 로그·metric·작업 기록에 노출하지 않음
- alias별 `releasedAt` 기록
- ACTIVE partial unique index 점유 해제
- PhoneIdentity release와 같은 Transaction에서 수행

### 6.6 eligibility binding과 outbox

Stage 1은 탈퇴 Transaction에서 기존 active binding을 REVOKED로 전환하고 REVOKED outbox를 생성한다. Stage 3은 최종 안전망으로 active binding이 남아 있는지 다시 확인한다.

- active binding이 있으면 같은 Transaction에서 revision을 REVOKED로 전진시키고 outbox를 생성한다.
- 이미 REVOKED면 새 event를 중복 생성하지 않는다.
- outbox가 PENDING·IN_FLIGHT인 것은 `CLEANED`를 막지 않는다.
- 외부 consumer 전달 완료를 법적 탈퇴 cleanup의 영구 차단 조건으로 사용하지 않는다.
- production에서 VERIFIED가 발행된 scope는 기존 publisher retry·dead-letter·replay 계약으로 REVOKED를 전달한다.

Identity는 TrialClaim이나 UserEntitlement를 삭제하지 않는다.

## 7. lifecycle 상태 계약

Stage 3에서 사용하는 상태 전이는 다음과 같다.

```text
IDENTITY_RELEASE_PENDING
        ├─ all release committed → CLEANED
        ├─ already fully released → CLEANED
        ├─ deterministic mismatch → RECONCILIATION_REQUIRED
        └─ transient Mongo failure → rollback, IDENTITY_RELEASE_PENDING 유지
```

### 별도 in-progress 상태를 추가하지 않는 이유

Stage 3은 외부 네트워크 호출이 없는 짧은 MongoDB Transaction이다. process가 종료되면 Transaction 전체가 commit되거나 rollback되므로 `IDENTITY_RELEASE_IN_PROGRESS` lease가 필요하지 않다.

동시 worker는 lifecycle status·version 조건과 동일 document write conflict로 fencing한다. 한 Transaction만 commit하고 loser는 rollback한 뒤 최신 상태를 재조회한다.

계획 검토에서 실제 Transaction 시간이 길어지거나 외부 side effect가 추가되면 별도 claim 상태를 다시 검토한다. 외부 호출을 추가해 현재 결정을 우회하지 않는다.

## 8. candidate 조회와 실행 모델

scheduler는 `IDENTITY_RELEASE_PENDING` lifecycle을 오래된 순서로 제한된 batch만 조회한다.

candidate 조회는 작업 소유권을 부여하지 않는다. 실제 권한은 Transaction 안에서 다시 읽은 다음 조건으로 결정한다.

- `_id == withdrawalId`
- `status == IDENTITY_RELEASE_PENDING`
- `version == expectedVersion`
- `externalDeletedAt != null`

두 worker가 같은 candidate를 선택할 수 있지만 같은 lifecycle과 identity document에 write하므로 MongoDB write conflict와 version CAS에서 하나만 commit한다.

Transient Transaction 오류는 lifecycle을 다른 상태로 바꾸지 않는다. scheduler의 다음 tick에서 다시 시도한다. 반복되는 infrastructure 오류는 낮은 cardinality metric과 pending age alert로 관측한다.

## 9. preflight 계약

Transaction은 identity mutation 전에 다음을 모두 검증한다.

### 9.1 lifecycle

- status가 `IDENTITY_RELEASE_PENDING`
- `externalDeletedAt` 존재
- `externalDeletedAt >= requestedAt`
- leaseOwner·leaseUntil이 없음
- target project·UID가 둘 다 null이거나 둘 다 존재
- expected version 일치

### 9.2 User

- User 존재
- lifecycle userId와 User id 일치
- User status가 `WITHDRAWN`

ACTIVE·SUSPENDED·MERGED User의 identity를 release하지 않는다.

### 9.3 FirebaseIdentity

Firebase target이 있는 경우:

- `(project, UID)` mapping이 존재하면 lifecycle userId와 owner 일치
- userId mapping이 존재하면 lifecycle target과 정확히 일치
- 다른 User owner 또는 다른 target이면 mutation 없이 reconciliation

Firebase target이 없는 LOCAL·GUEST lifecycle인 경우:

- 해당 userId의 FirebaseIdentity가 없어야 함
- 존재하면 lifecycle target 누락이므로 reconciliation

### 9.4 SocialIdentity

- `findAllByUserId`로 기존 User의 identity만 수집
- provider subject를 다른 lookup key로 추정하지 않음
- 다른 User의 SocialIdentity를 삭제하지 않음
- raw subject를 lifecycle·로그·metric에 기록하지 않음

### 9.5 PhoneIdentity·alias

- ACTIVE PhoneIdentity는 0개 또는 1개
- ACTIVE identity가 있으면 userId 일치
- 모든 ACTIVE alias의 phoneIdentityId와 userId 일치
- orphan alias·다른 owner·복수 ACTIVE identity는 reconciliation

## 10. fully released와 partial state 판정

### fully released

다음 조건이면 이전 실행이 이미 모든 내부 release를 완료한 것으로 보고 멱등하게 `CLEANED` 처리할 수 있다.

- FirebaseIdentity 없음
- SocialIdentity 없음
- ACTIVE PhoneIdentity 없음
- ACTIVE PhoneFingerprintAlias 없음
- active eligibility binding 없음
- User는 `WITHDRAWN`
- lifecycle은 `IDENTITY_RELEASE_PENDING`이고 external deletion 증적 존재

### partial 또는 mixed state

일부 mapping만 사라지고 다른 ACTIVE identity가 남아 있거나 owner가 다르면 자동으로 추정해 삭제하지 않는다.

예시:

- FirebaseIdentity는 없지만 다른 User owner의 SocialIdentity가 충돌
- PhoneIdentity는 RELEASED지만 ACTIVE alias가 남음
- lifecycle target과 FirebaseIdentity target 불일치
- User가 WITHDRAWN이 아님
- active eligibility binding을 안전하게 REVOKED로 전환할 수 없음

이 경우 identity mutation 없이 `RECONCILIATION_REQUIRED`로 전환하고 안전한 오류 code만 저장한다.

## 11. Transaction 실행 순서

권장 순서는 다음과 같다.

1. lifecycle을 id·status·version으로 재조회
2. externalDeletedAt과 target 불변식 검증
3. WITHDRAWN User 검증
4. FirebaseIdentity·SocialIdentity·PhoneIdentity·alias snapshot 조회
5. fully released 또는 release 가능 상태 판정
6. active eligibility binding이 있으면 REVOKED revision·outbox 생성
7. exact FirebaseIdentity 삭제
8. User 소유 SocialIdentity 전체 삭제
9. ACTIVE PhoneIdentity를 RELEASED로 저장
10. 해당 identity의 ACTIVE aliases 전체 RELEASED
11. lifecycle `identitiesReleasedAt`·`cleanedAt` 기록
12. lifecycle status를 `CLEANED`로 변경하고 version 증가
13. Transaction commit

7~12 중 하나라도 실패하면 6을 포함해 전체 rollback한다.

## 12. Repository 계약

예상 신규·확장 계약은 다음과 같다. 실제 명칭은 구현 시 패키지 규칙에 맞춰 조정한다.

```java
public interface UserWithdrawalLifecycleRepositoryCustom {
    List<UserWithdrawalLifecycle> findIdentityReleaseCandidates(int limit);

    boolean markIdentityReleaseReconciliationRequired(
            String withdrawalId,
            long expectedVersion,
            WithdrawalCleanupFailureCode failureCode,
            Instant updatedAt
    );

    boolean markCleanedIfIdentityReleasePending(
            String withdrawalId,
            long expectedVersion,
            Instant identitiesReleasedAt,
            Instant cleanedAt
    );
}
```

identity Repository에는 exact loaded document를 제거하거나 release하는 최소 계약을 둔다.

```java
Optional<FirebaseIdentity> findByUserId(String userId);
List<SocialIdentity> findAllByUserId(String userId);
Optional<PhoneIdentity> findByUserIdAndStatus(String userId, ACTIVE);
List<PhoneFingerprintAlias> findAllByPhoneIdentityIdAndStatus(id, ACTIVE);
long releaseAllActiveByPhoneIdentityId(String id, Instant releasedAt);
```

FirebaseIdentity·SocialIdentity delete는 조회한 entity id를 기준으로 수행한다. provider subject, email, phone으로 삭제 대상을 재탐색하지 않는다.

## 13. application 구조

권장 책임 분리는 다음과 같다.

- `UserWithdrawalIdentityReleaseWorker`
  - candidate 선택
  - Transaction service 호출
  - outcome·metric 기록
- `UserWithdrawalIdentityReleaseTransactionService`
  - preflight 재검증
  - eligibility revoke 보장
  - Firebase·Social 삭제
  - Phone·alias release
  - lifecycle CLEANED commit
- `WithdrawalEnrollmentGate`
  - credential owner User·lifecycle 상태 분류
  - cleanup 중 owner를 `WITHDRAWAL_CLEANUP_PENDING`으로 통합
  - CLEANED 뒤 stale mapping은 conflict/reconciliation으로 fail-closed
- `UserWithdrawalLifecycleRepositoryCustom`
  - candidate 조회
  - CLEANED·reconciliation version CAS
- `UserWithdrawalIdentityReleaseScheduler`
  - enabled flag와 max batch 적용
  - fixed delay 실행

Controller와 공개 API는 추가하지 않는다.

## 14. 재가입 gate 계약

### 14.1 gate 입력

gate는 다음 owner 정보를 기준으로 판단한다.

- FirebaseIdentity가 가리키는 userId
- SocialIdentity가 가리키는 userId
- PhoneFingerprintAlias가 가리키는 userId
- owner User status
- owner withdrawal lifecycle status

email, phone 원문, nickname은 owner 판정 key로 사용하지 않는다.

### 14.2 상태별 결과

| owner 상태 | lifecycle | 결과 |
| --- | --- | --- |
| ACTIVE MEMBER | 없음 | 기존 로그인·merge 계약 유지 |
| SUSPENDED | 없음 | 기존 계정 비활성 오류 유지 |
| WITHDRAWN | CLEANED 전 | `WITHDRAWAL_CLEANUP_PENDING` |
| WITHDRAWN | CLEANED | stale identity conflict, 운영 reconciliation |
| WITHDRAWN | lifecycle 없음 | withdrawal lifecycle conflict |
| owner mapping 없음 | 해당 없음 | 신규 enrollment 진행 |

CLEANED인데 ACTIVE mapping이 남아 있는 상태를 신규 가입 허용 근거로 사용하지 않는다. CLEANED 불변식 위반이므로 fail-closed 처리한다.

### 14.3 적용 대상

중앙 gate를 최소 다음 경로에 적용한다.

- Firebase login exchange
- direct Firebase signup owner precheck와 unique conflict 재분류
- Guest prepare·upgrade의 기존 owner 판정
- Guest merge target resolution
- Firebase auth methods sync의 owner conflict 경계

동일 cleanup owner가 경로마다 `SOCIAL_IDENTITY_CONFLICT`, `PHONE_ALREADY_LINKED`, `MERGE_REQUIRED`로 다르게 노출되지 않게 한다.

## 15. 사용자 흐름

### 15.1 CLEANED 전 같은 SNS 로그인

```text
Client Firebase SNS sign-in
→ 새 Firebase UID 또는 기존 credential proof
→ SocialIdentity가 WITHDRAWN owner를 가리킴
→ lifecycle CLEANED 전 확인
→ WITHDRAWAL_CLEANUP_PENDING
```

enrollmentId를 만들거나 기존 User를 로그인시키지 않는다.

### 15.2 CLEANED 전 같은 전화번호 signup

```text
signup finalize
→ ACTIVE phone alias가 WITHDRAWN owner를 가리킴
→ lifecycle CLEANED 전 확인
→ WITHDRAWAL_CLEANUP_PENDING
```

일반 `PHONE_ALREADY_LINKED`로 숨기지 않는다.

### 15.3 CLEANED 후 재가입

```text
Firebase credential 검증
→ 기존 Firebase/Social mapping 없음
→ enrollment attempt 생성
→ 같은 Firebase UID에 phone link·재검증
→ 필수 동의·프로필 제출
→ 새 UUID User + 새 identities + 새 RefreshSession commit
```

기존 WITHDRAWN User와 신규 User 사이에 자동 ownership migration을 생성하지 않는다.

## 16. 오류와 reconciliation 분류

안전한 failure code 후보는 다음과 같다.

```text
IDENTITY_RELEASE_USER_NOT_WITHDRAWN
IDENTITY_RELEASE_EXTERNAL_DELETE_NOT_CONFIRMED
IDENTITY_RELEASE_FIREBASE_OWNER_MISMATCH
IDENTITY_RELEASE_FIREBASE_TARGET_MISMATCH
IDENTITY_RELEASE_PHONE_OWNER_MISMATCH
IDENTITY_RELEASE_ORPHAN_PHONE_ALIAS
IDENTITY_RELEASE_ACTIVE_BINDING_CONFLICT
IDENTITY_RELEASE_PARTIAL_STATE
IDENTITY_RELEASE_VERSION_CONFLICT
IDENTITY_RELEASE_UNKNOWN
```

MongoDB exception 원문, Firebase UID, provider subject, phone fingerprint와 document 전체를 `lastErrorCode`, 로그, metric에 넣지 않는다.

- deterministic owner·state mismatch: `RECONCILIATION_REQUIRED`
- transient Transaction/write conflict: rollback 후 pending 유지, 다음 tick 재시도
- 이미 CLEANED: 멱등 no-op
- fully released pending: lifecycle만 CLEANED로 수렴

## 17. 설정과 scheduler

기본 비활성 설정을 추가한다.

```text
WITHDRAWAL_IDENTITY_RELEASE_ENABLED=false
WITHDRAWAL_IDENTITY_RELEASE_FIXED_DELAY_MS
WITHDRAWAL_IDENTITY_RELEASE_MAX_BATCH
```

실제 property prefix와 환경변수 이름은 기존 withdrawal cleanup 설정 규칙에 맞춘다.

설정 검증:

- enabled일 때 Mongo TransactionManager 존재
- fixed delay 양수
- max batch 1 이상·상한 이하
- Stage 2 cleanup worker와 독립적으로 on/off 가능
- Stage 1 withdrawal endpoint가 열리기 전에 Stage 2·3 worker가 선배포됨

## 18. 관측 항목

구조화 event 후보:

- `user.withdrawal.identity_release.started`
- `user.withdrawal.identity_release.cleaned`
- `user.withdrawal.identity_release.idempotent`
- `user.withdrawal.identity_release.conflict`
- `user.withdrawal.identity_release.reconciliation_required`

metric 후보:

- `IDENTITY_RELEASE_PENDING` 건수
- oldest identity release pending age
- CLEANED 처리 수
- idempotent completion 수
- failure code별 reconciliation 수
- Mongo transient retry 수
- cleanup 중 enrollment 차단 수

userId, Firebase UID, provider subject, fingerprint를 metric tag로 사용하지 않는다.

## 19. 예상 변경 파일

실제 구현 시 구조에 따라 조정하되 예상 대상은 다음과 같다.

- `domain/user/domain/enums/UserWithdrawalCleanupStatus.java`
- `domain/user/domain/enums/WithdrawalCleanupFailureCode.java`
- `domain/user/domain/entity/UserWithdrawalLifecycle.java`
- `domain/user/domain/repository/UserWithdrawalLifecycleRepositoryCustom.java`
- `domain/user/domain/repository/UserWithdrawalLifecycleRepositoryImpl.java`
- `domain/user/application/UserWithdrawalIdentityReleaseWorker.java` 신규
- `domain/user/application/UserWithdrawalIdentityReleaseTransactionService.java` 신규
- `domain/user/infrastructure/UserWithdrawalIdentityReleaseProperties.java` 신규
- `domain/user/infrastructure/UserWithdrawalIdentityReleaseConfiguration.java` 신규
- `domain/user/infrastructure/UserWithdrawalIdentityReleaseScheduler.java` 신규
- `domain/auth/federation/application/WithdrawalEnrollmentGate.java` 신규
- `domain/auth/federation/application/FirebaseExchangeService.java`
- `domain/auth/federation/application/FirebaseSignupService.java`
- Guest prepare·upgrade·merge owner 판정 서비스
- FirebaseIdentity·SocialIdentity·PhoneIdentity·alias Repository
- `application.yml`, `application-test.yml`
- 관련 domain·Repository·Transaction·gate·scheduler 테스트

## 20. 테스트 계획

### 20.1 Transaction 정상 흐름

- Firebase MEMBER identity 전체 release와 CLEANED commit
- SocialIdentity 0개·1개·여러 개 삭제
- PhoneIdentity와 모든 ACTIVE alias RELEASED
- active eligibility binding이 있으면 REVOKED revision·outbox 생성
- LOCAL·GUEST null Firebase target CLEANED
- fully released pending lifecycle 멱등 CLEANED
- identitiesReleasedAt·cleanedAt·version 기록
- User tombstone 유지

### 20.2 rollback

각 주입 지점의 실패에서 전체 rollback을 검증한다.

- eligibility revoke 저장 실패
- FirebaseIdentity delete 실패
- SocialIdentity delete 실패
- PhoneIdentity save 실패
- alias release 실패
- lifecycle CLEANED CAS 실패
- commit write conflict

rollback 후 기존 identity 점유와 lifecycle `IDENTITY_RELEASE_PENDING`이 유지돼야 한다.

### 20.3 mismatch와 reconciliation

- externalDeletedAt 없음
- User ACTIVE·SUSPENDED·MERGED
- lifecycle target과 FirebaseIdentity target 불일치
- FirebaseIdentity가 다른 User 소유
- orphan ACTIVE alias
- PhoneIdentity·alias userId 불일치
- 일부 identity만 이미 release된 mixed state
- lifecycle version mismatch
- mismatch에서 다른 User identity mutation 0건

### 20.4 동시성·멱등성

- 두 worker가 같은 lifecycle을 선택해 하나만 commit
- concurrent loser가 CLEANED 재조회 후 멱등 종료
- scheduler 재실행이 추가 삭제·outbox를 만들지 않음
- Transaction commit 후 worker 응답 유실을 재실행해 CLEANED no-op
- transient Mongo 오류 후 다음 tick에서 성공

### 20.5 재가입 gate

- cleanup 중 기존 Firebase UID exchange → `WITHDRAWAL_CLEANUP_PENDING`
- cleanup 중 새 Firebase UID + 같은 SocialIdentity → pending
- cleanup 중 같은 phone alias signup → pending
- cleanup 중 Guest upgrade·merge → pending
- CLEANED인데 stale mapping 존재 → fail-closed conflict
- CLEANED 뒤 same SNS exchange → enrollment required
- CLEANED 뒤 same phone signup → 새 User 성공
- 신규 User UUID가 기존 WITHDRAWN User와 다름
- 기존 User·프로필·시험 ownership 자동 복구 없음

### 20.6 보안·관측

- Token·UID·provider subject·raw phone·fingerprint 비로그
- safe failure code만 lifecycle 저장
- userId를 metric tag로 사용하지 않음
- event 중복 기록 없음

### 20.7 기본 검증

```text
./gradlew clean test
git diff --check
```

기본 테스트에서 실제 Atlas·Firebase·Billing·Learning Core를 호출하지 않는다.

## 21. 마이그레이션과 운영 점검

배포 전 read-only 집계를 수행한다.

- status별 withdrawal lifecycle 수
- `IDENTITY_RELEASE_PENDING`인데 externalDeletedAt이 없는 문서 수
- WITHDRAWN User와 FirebaseIdentity owner 불일치 수
- WITHDRAWN User의 ACTIVE PhoneIdentity·alias 수
- CLEANED인데 ACTIVE identity가 남은 문서 수
- lifecycle 없이 WITHDRAWN인 legacy User 수
- active eligibility binding이 남은 WITHDRAWN User 수

기존 데이터가 있으면 자동 추정 backfill하지 않는다. owner·target 증적이 충분한 문서만 별도 승인된 reconciliation 절차로 처리한다.

FirebaseIdentity·SocialIdentity index는 entity 물리 삭제 방식을 사용하므로 이번 단계에서 partial unique index로 변경하지 않는다. PhoneIdentity·alias는 기존 ACTIVE partial unique index를 유지한다.

## 22. staging 검증

Transaction을 지원하는 MongoDB replica set에서 다음을 검증한다.

- 정상 Firebase/Social delete·Phone release·CLEANED commit
- 각 mutation 뒤 injected failure rollback
- 두 scheduler instance 동시 실행과 write conflict 수렴
- worker crash 전·후 commit 경계
- CLEANED 전 동일 SNS·phone 차단
- CLEANED 후 새 Firebase UID·동일 SNS·동일 phone 신규 가입
- 새 UUID User 생성과 기존 tombstone 유지
- eligibility REVOKED outbox pending 상태에서도 CLEANED 가능
- publisher 재활성화 후 REVOKED 전달·중복·역순 처리
- metric·로그·Sentry 민감정보 비노출

Stage 2의 실제 Firebase delete 증적과 Stage 3의 Mongo Transaction 증적을 같은 withdrawalId 운영 흐름으로 확인하되 검증 로그에는 식별자를 남기지 않는다.

## 23. 배포·활성화 순서

1. Stage 3 entity·Repository·Transaction·gate를 feature flag off로 배포
2. 신규 Repository query와 index 호환성 확인
3. staging MongoDB rollback·동시성 검증
4. Stage 2 `IDENTITY_RELEASE_PENDING` handoff와 Stage 3 CLEANED E2E
5. CLEANED 전후 모바일 SNS·phone 재가입 UX 검증
6. reconciliation runbook·pending age alert 준비
7. Stage 3 worker 활성화
8. Stage 2 cleanup worker 활성화
9. Stage 1 withdrawal endpoint 활성화

Stage 1~3은 완전 탈퇴를 위한 하나의 release train으로 본다. Stage 3 없이 Stage 1·2만 production 활성화하지 않는다.

Firebase 신규 가입, eligibility publisher와 무료 모의고사 정책의 전체 production 활성화는 후속 Stage 6·7과 consumer staging 검증까지 완료한 뒤 판단한다.

## 24. 완료 조건

다음 조건을 모두 충족해야 Stage 3 구현 완료로 본다.

- `IDENTITY_RELEASE_PENDING`만 처리함
- 외부 삭제 증적과 WITHDRAWN owner를 검증함
- FirebaseIdentity·SocialIdentity 제거와 PhoneIdentity·alias release가 한 Transaction임
- active eligibility binding이 CLEANED와 함께 남지 않음
- CLEANED가 전체 release의 commit marker임
- 실패 시 identity 부분 삭제 없이 rollback함
- 동시 worker 중 하나만 commit함
- 완전 release 상태는 멱등하게 CLEANED로 수렴함
- mixed·owner mismatch는 다른 User mutation 없이 reconciliation으로 이동함
- CLEANED 전 같은 SNS·phone enrollment가 전용 pending 오류로 차단됨
- CLEANED 뒤 fresh proof로 새 UUID User 가입이 가능함
- 기존 User tombstone·과거 데이터·TrialClaim을 복구하거나 삭제하지 않음
- worker·scheduler가 기본 비활성임
- 민감정보 비저장·비로그·비metric 계약을 지킴
- `./gradlew clean test` 통과
- staging Transaction·동시성·재가입 E2E와 운영 runbook 완료

## 25. 후속 단계 인계

Stage 3 완료 후 다음 순서는 가입 중단 Firebase User cleanup이다.

전달 정보:

- complete withdrawal은 `CLEANED`로 식별
- CLEANED 이전 credential owner는 enrollment 차단
- CLEANED 이후 재가입은 새 User 생성
- 중단 enrollment는 기존 User withdrawal lifecycle과 다른 단기 cleanup 문제
- 중단 가입 Firebase User cleanup이 기존 ACTIVE·WITHDRAWN owner를 삭제하지 않도록 owner fencing 필요

Stage 6은 `FirebaseEnrollmentAttempt` 만료·중단 상태와 Firebase User 존재 여부를 기준으로 별도 lifecycle을 사용하며, Stage 3 identity release Transaction을 재사용해 기존 User를 추정 삭제하지 않는다.
