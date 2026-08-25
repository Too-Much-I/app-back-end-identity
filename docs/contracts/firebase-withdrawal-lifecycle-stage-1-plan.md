# 1단계 구현 계획: Firebase/SNS 탈퇴 재인증과 withdrawal lifecycle

## 1. 목적

Firebase/SNS MEMBER가 본인 소유의 fresh Firebase credential로 회원 탈퇴를 요청할 수 있게 하고, 내부 탈퇴를 원자적으로 확정한 뒤 후속 Firebase 삭제와 identity release 작업이 안전하게 이어받을 durable withdrawal lifecycle을 만든다.

이 단계의 완료 상태는 Firebase 외부 User 삭제나 재가입 허용이 아니다. 내부 User는 즉시 `WITHDRAWN`이 되지만 lifecycle은 `EXTERNAL_CLEANUP_PENDING`에 머물며, 2단계와 3단계가 완료되어야 최종 `CLEANED`가 된다.

## 2. 현재 문제

- `UserWithdrawalService`는 MEMBER 중 LOCAL credential이 없는 Firebase/SNS 회원을 `INVALID_WITHDRAWAL_CREDENTIALS`로 거절한다.
- 현재 탈퇴 Transaction은 User tombstone, RefreshSession 폐기와 기존 eligibility revoke만 처리한다.
- Firebase disable·refresh revoke·delete를 이어받을 durable lifecycle job이 없다.
- 탈퇴 처리 중인 Firebase UID·provider subject를 일반 충돌과 구분할 상태와 오류가 없다.
- 외부 호출을 MongoDB Transaction 안에서 수행하지 않는다는 계약은 있으나, 내부 commit과 외부 cleanup을 연결할 aggregate가 없다.

## 3. 범위

### 포함

- 기존 `POST /api/v1/users/withdraw`의 Firebase/SNS MEMBER 재인증 지원
- `WITHDRAWAL` 전용 Firebase verification purpose와 recent-auth 검증
- Access Token userId, RefreshSession owner, FirebaseIdentity owner의 삼중 일치 확인
- Firebase principal의 linked SocialIdentity 소유권 일치 확인
- User tombstone·모든 내부 RefreshSession 폐기·필요한 eligibility revoke·withdrawal lifecycle 생성을 하나의 Mongo Transaction으로 처리
- 반복·동시 탈퇴 요청의 멱등성과 CAS
- 탈퇴 cleanup 진행 중임을 구분하는 상태·오류 계약
- 2단계 worker와 3단계 release가 사용할 lifecycle schema·Repository 계약
- 기존 LOCAL·GUEST 탈퇴 회귀 유지
- API·application·domain·Repository·Transaction·Security·OpenAPI 테스트

### 제외

- Firebase User disable·refresh token revoke·delete 실행
- Apple authorization revoke 실행과 material 저장
- FirebaseIdentity·SocialIdentity release 또는 삭제
- PhoneIdentity·PhoneFingerprintAlias release
- `CLEANED` 전환과 실제 재가입 허용
- 가입 중단 Firebase User cleanup
- logout-all Firebase revoke
- Provider unlink·전화번호 변경·ACTIVE User rebind
- Billing consumer 구현
- Learning Core 데이터 삭제·익명화와 Access Token 즉시 deny

## 4. 핵심 결정

### 4.1 내부 탈퇴를 먼저 확정한다

Firebase 호출 성공 뒤 내부 탈퇴를 시도하지 않는다. 다음 내부 변경을 먼저 하나의 Mongo Transaction으로 commit한다.

1. User를 `WITHDRAWN` tombstone으로 전환
2. 모든 내부 RefreshSession 폐기
3. 이미 발행된 phone eligibility binding이 있을 때만 REVOKED outbox 생성
4. withdrawal lifecycle을 `EXTERNAL_CLEANUP_PENDING`으로 생성

commit 이후 Firebase 장애가 발생해도 User를 다시 ACTIVE로 되돌리지 않는다.

### 4.2 Firebase 원격 호출은 재인증 검증까지만 수행한다

탈퇴 요청 경로에서는 Firebase ID Token의 유효성·revoke·disabled·recent-auth와 Firebase UserRecord를 검증한다. disable·revoke·delete mutation은 2단계 worker 책임이며 Mongo Transaction 밖에서 수행한다.

### 4.3 계정 종류는 legacy provider 문자열이 아니라 실제 credential 소유로 판정한다

- ACTIVE GUEST: 기존처럼 비밀번호와 Firebase proof 없이 현재 RefreshSession 소유권으로 처리
- FirebaseIdentity가 있는 MEMBER: Firebase withdrawal proof 필수
- FirebaseIdentity가 없고 LOCAL credential이 있는 MEMBER: 기존 비밀번호 필수
- FirebaseIdentity와 LOCAL credential이 모두 있거나 어느 쪽도 없는 예상 밖 상태: 자동 선택하지 않고 명시적 충돌로 거절하고 운영 reconciliation 대상으로 남김

email·phone·provider 이름만으로 탈퇴 대상을 선택하지 않는다.

### 4.4 1단계는 `CLEANED`를 만들지 않는다

1단계 성공 직후 상태는 다음과 같다.

- User: `WITHDRAWN`
- RefreshSession: 모두 폐기
- FirebaseIdentity·SocialIdentity·PhoneIdentity: 아직 점유 유지
- Firebase 외부 User: 아직 존재할 수 있음
- withdrawal lifecycle: `EXTERNAL_CLEANUP_PENDING`
- 동일 credential 로그인·신규 enrollment: fail-closed 차단

## 5. API 계약

### 5.1 요청 확장

기존 endpoint를 유지하고 `WithdrawRequest`에 optional write-only `firebaseIdToken`을 추가한다.

```json
{
  "refreshToken": "<redacted>",
  "password": null,
  "firebaseIdToken": "<redacted>"
}
```

credential 조합은 다음과 같이 검증한다.

| 계정 상태 | password | firebaseIdToken |
| --- | --- | --- |
| GUEST | 금지 또는 무시하지 않고 명시적 거절 | 금지 |
| LOCAL MEMBER | 필수 | 금지 |
| Firebase MEMBER | 금지 | 필수 |
| 불일치·혼합 credential | 거절 | 거절 |

기존 클라이언트의 LOCAL·GUEST JSON은 그대로 동작해야 한다. Firebase ID Token은 DTO `toString`, validation 오류, 로그와 Sentry에 노출하지 않는다.

### 5.2 응답

기존 `WithdrawResponse`의 `status=WITHDRAWN`, `withdrawnAt` 계약을 유지하고 async cleanup 상태를 추가한다.

```json
{
  "status": "WITHDRAWN",
  "withdrawnAt": "2026-08-24T00:00:00Z",
  "cleanupStatus": "EXTERNAL_CLEANUP_PENDING"
}
```

- LOCAL·GUEST처럼 외부 Firebase cleanup 대상이 없는 계정도 이 단계에서는 lifecycle 유무를 명확히 반환한다.
- 응답 필드 추가가 기존 client 역직렬화와 호환되는지 API 계약 테스트로 확인한다.
- 별도 polling API는 1단계에 추가하지 않는다. cleanup 중 동일 SNS 로그인에는 안정적인 pending 오류를 반환한다.

### 5.3 오류 계약

새 오류는 최소한으로 추가한다.

| 상황 | 권장 HTTP/code |
| --- | --- |
| Firebase MEMBER인데 proof 누락 | `400 WITHDRAWAL_FIREBASE_PROOF_REQUIRED` |
| 계정 종류와 credential 조합 불일치 | `400 WITHDRAWAL_CREDENTIAL_TYPE_MISMATCH` |
| Firebase UID·project·user owner 불일치 | `409 FIREBASE_IDENTITY_CONFLICT` 유지 |
| Firebase recent-auth 만료 | `401 FIREBASE_RECENT_AUTH_REQUIRED` 유지 |
| Firebase 장애 | `503 FIREBASE_UNAVAILABLE` 유지 |
| 탈퇴 cleanup 진행 중 로그인·enrollment | `409 WITHDRAWAL_CLEANUP_PENDING` |
| lifecycle/User 상태 불일치 | `409 WITHDRAWAL_LIFECYCLE_CONFLICT` |

Provider 원문 오류나 Firebase UID를 오류 응답에 포함하지 않는다.

## 6. Firebase 재인증 계약

`FirebaseVerificationPurpose.WITHDRAWAL`을 추가한다.

- signature·issuer·audience·tenant·expiry 검증
- revoked·disabled remote 확인
- high-risk recent-auth 최대 경과 시간 적용
- phone-only sign-in을 탈퇴 proof로 허용하지 않음
- FirebaseIdentity의 project·UID와 principal 일치
- FirebaseIdentity의 userId와 Access Token `sub` 일치
- principal에 포함된 기존 SocialIdentity가 모두 같은 userId 소유인지 확인
- 다른 User owner가 하나라도 있으면 자동 merge·추정 없이 conflict

로그인 Provider feature flag가 꺼진 상황에서도 기존 사용자의 법적 탈퇴 경로를 무기한 막지 않도록 로그인 활성화 flag와 withdrawal verifier 가용성 정책을 분리한다. Firebase Admin 자체가 불가용하면 fail-closed 503과 운영 탈퇴 지원 절차를 사용한다.

## 7. withdrawal lifecycle 모델

신규 Mongo document `UserWithdrawalLifecycle`을 추가한다.

### 7.1 필드

| 필드 | 의미 |
| --- | --- |
| `withdrawalId` | 서버 생성 UUID, document id |
| `userId` | canonical UUID, lifecycle당 한 User |
| `status` | 현재 lifecycle 상태 |
| `firebaseProjectId` | 2단계 cleanup target namespace |
| `firebaseUid` | 2단계 cleanup target UID |
| `requestedAt` | 내부 탈퇴 확정 시각 |
| `updatedAt` | 마지막 상태 변경 시각 |
| `attemptCount` | worker 시도 횟수, 초기 0 |
| `nextAttemptAt` | worker가 처리 가능한 시각 |
| `leaseOwner` / `leaseUntil` | 다중 worker lease, 초기 null |
| `lastErrorCode` | 안전한 내부 오류 code, 초기 null |
| `externalDeletedAt` | 2단계 완료 시각, 초기 null |
| `identitiesReleasedAt` | 3단계 release 완료 시각, 초기 null |
| `cleanedAt` | terminal 완료 시각, 초기 null |
| `version` | 낙관적 잠금 또는 CAS fencing |

Firebase ID Token, Provider Token, email, raw phone과 SDK 원문 오류는 저장하지 않는다.

### 7.2 상태

```text
EXTERNAL_CLEANUP_PENDING
  → EXTERNAL_CLEANUP_IN_PROGRESS
  → EXTERNAL_CLEANUP_RETRY_WAIT
  → EXTERNAL_CLEANUP_COMPLETED
  → IDENTITY_RELEASE_PENDING
  → CLEANED

반복 실패:
EXTERNAL_CLEANUP_IN_PROGRESS
  → RECONCILIATION_REQUIRED
```

1단계에서 생성·사용하는 상태는 `EXTERNAL_CLEANUP_PENDING`뿐이다. 나머지 전이는 2·3단계가 구현하지만 schema와 허용 전이를 1단계 테스트에서 고정한다.

### 7.3 인덱스

- `userId` unique: User당 withdrawal lifecycle 하나
- `status + nextAttemptAt + leaseUntil`: worker claim 조회
- 필요할 때만 retention용 `cleanupAt` TTL: `CLEANED` 이후 물리 정리용이며 correctness에 사용하지 않음
- Firebase target에는 unique index를 추가하지 않는다. active ownership uniqueness는 FirebaseIdentity가 계속 담당한다.

## 8. Transaction 흐름

```text
Client
  → Access Token 인증
  → RefreshSession 소유·활성 검증
  → 계정 credential 종류 결정
  → fresh Firebase WITHDRAWAL proof 또는 LOCAL password 검증
  → Firebase/Social owner 일치 검증
  → Mongo Transaction
       User WITHDRAWN CAS
       모든 RefreshSession revoke
       기존 eligibility binding만 revoke outbox
       UserWithdrawalLifecycle(EXTERNAL_CLEANUP_PENDING) insert
  → 200 WITHDRAWN + cleanupStatus
```

Mongo Transaction 안에서 Firebase SDK를 호출하지 않는다.

### 8.1 Transaction 재검증

Transaction service는 application service의 사전 검증만 신뢰하지 않고 다음을 다시 확인한다.

- 요청 RefreshSession이 같은 userId 소유이며 활성·미만료
- User가 예상 status·updatedAt과 일치
- FirebaseIdentity snapshot이 같은 userId·project·UID를 가리킴
- 기존 lifecycle이 없거나 동일 요청의 멱등 결과임

User tombstone 저장 또는 lifecycle insert, Session 폐기, eligibility revoke 중 하나라도 실패하면 전체 rollback한다.

## 9. 멱등성·동시성

### 반복 요청

- lifecycle이 이미 있고 User가 WITHDRAWN이면 기존 `withdrawnAt`과 cleanup status를 반환한다.
- 반복 요청으로 새 lifecycle이나 중복 eligibility REVOKED event를 만들지 않는다.
- lifecycle 없이 WITHDRAWN인 legacy User는 자동으로 외부 cleanup target을 추정하지 않고 `WITHDRAWAL_LIFECYCLE_CONFLICT` 또는 운영 reconciliation 대상으로 분류한다.

### 동시 요청

- User status·updatedAt CAS와 `userId` lifecycle unique index로 한 요청만 승리한다.
- loser는 최신 User와 lifecycle을 다시 조회해 동일 완료 결과로 수렴한다.
- 다른 Firebase proof 또는 다른 target snapshot이 섞이면 멱등 성공으로 숨기지 않고 conflict 처리한다.

### 응답 유실

내부 commit 후 응답이 유실되어도 재요청은 같은 lifecycle을 반환해야 한다. RefreshSession이 이미 폐기돼 원래 endpoint 재호출이 불가능할 수 있으므로 client는 성공 여부 불명 시 보유 Access Token으로 한 번 재조회할 수 있는 UX 또는 이후 SNS exchange의 `WITHDRAWAL_CLEANUP_PENDING` 처리를 계약으로 고정한다. 별도 status endpoint 추가 여부는 API 설계 리뷰에서 확정한다.

## 10. application 구조

권장 책임 분리는 다음과 같다.

- `UserWithdrawalService`
  - 현재 user와 credential 종류 결정
  - LOCAL/GUEST 기존 경로 조합
  - Firebase withdrawal verifier 호출
  - transaction command 구성
- `FirebaseWithdrawalCredentialVerifier`
  - `WITHDRAWAL` purpose 검증
  - FirebaseIdentity·SocialIdentity owner 확인
  - redacted target snapshot 반환
- `UserWithdrawalTransactionService`
  - User CAS, Session revoke, eligibility revoke, lifecycle insert
- `UserWithdrawalLifecycleRepository`
  - userId 조회와 stage 2 worker claim/CAS 계약
- `WithdrawalAccessGate`
  - withdrawal pending owner의 login/enrollment을 안정적인 오류로 차단

Firebase SDK 타입은 Controller·DTO·application 결과에 노출하지 않는다.

## 11. 예상 변경 파일

구현 시 실제 패키지 구조에 맞춰 조정하되 최소 변경 대상은 다음과 같다.

- `domain/user/dto/request/WithdrawRequest.java`
- `domain/user/dto/response/WithdrawResponse.java`
- `domain/user/application/UserWithdrawalService.java`
- `domain/user/application/UserWithdrawalTransactionService.java`
- `domain/user/application/FirebaseWithdrawalCredentialVerifier.java` 신규
- `domain/user/domain/entity/UserWithdrawalLifecycle.java` 신규
- `domain/user/domain/enums/UserWithdrawalLifecycleStatus.java` 신규
- `domain/user/domain/repository/UserWithdrawalLifecycleRepository.java` 신규
- `domain/auth/federation/application/FirebaseVerificationPurpose.java`
- `domain/auth/federation/application/FirebaseExchangeService.java`
- `domain/auth/common/exception/AuthErrorStatus.java`
- `domain/user/exception/UserErrorStatus.java`
- `domain/user/api/UserController.java`
- Firebase configuration과 관련 단위·Repository·Controller 테스트

`FirebaseIdentity`, `SocialIdentity`, `PhoneIdentity`의 release 구현은 3단계까지 변경하지 않는다.

## 12. 테스트 계획

### 12.1 application

- Firebase MEMBER가 fresh proof로 탈퇴 성공
- Firebase proof 누락·만료·revoked·disabled·UID mismatch 거절
- Access Token userId, RefreshSession userId와 FirebaseIdentity owner 불일치 거절
- SocialIdentity owner 불일치 거절
- Firebase 장애 503 fail-closed와 mutation 없음
- LOCAL password·GUEST 기존 탈퇴 회귀
- credential 혼합·잘못된 계정 종류 거절
- 이미 WITHDRAWN + lifecycle 존재 시 멱등 응답
- 이미 WITHDRAWN + lifecycle 없음 시 reconciliation conflict

### 12.2 Transaction·Repository

- User tombstone, 전체 Session revoke, lifecycle insert의 단일 commit
- lifecycle 저장 실패 시 User·Session·eligibility 변경 rollback
- Session 저장 실패 시 User·lifecycle rollback
- 동일 User 동시 탈퇴 한 건만 생성
- concurrent loser가 기존 lifecycle 결과로 수렴
- `userId` unique와 worker claim index metadata
- lifecycle 허용·금지 상태 전이와 timestamp 불변식

### 12.3 API·Security

- Firebase ID Token write-only·redacted 문서화
- LOCAL·GUEST 기존 request JSON 호환
- cleanup status 응답 schema
- validation에서 Token rejected value 비노출
- pending lifecycle 로그인·enrollment의 안정적인 오류 code
- 로그·Sentry·예외 응답에 Firebase Token·UID·provider subject 비노출

### 12.4 검증 명령

```text
./gradlew clean test
```

실제 Firebase·Atlas·OAuth Provider를 기본 테스트에서 호출하지 않는다. Firebase verifier와 외부 adapter는 mock/fake를 사용하고 실제 Mongo Transaction과 Firebase/mobile E2E는 staging gate로 남긴다.

## 13. 마이그레이션·호환성

- 기존 LOCAL·GUEST User와 request는 migration 없이 읽는다.
- 기존 FirebaseIdentity·SocialIdentity index는 1단계에서 변경하지 않는다.
- 이미 WITHDRAWN인데 lifecycle이 없는 문서는 자동 backfill하지 않는다. 운영 read-only 집계 후 별도 reconciliation 정책을 적용한다.
- 운영 User가 없다는 기존 가정은 배포 전 다시 read-only 확인한다.
- 신규 lifecycle index는 writer 활성화 전에 생성 가능성과 중복 데이터를 staging에서 검증한다.

## 14. 배포 순서

1. lifecycle entity·Repository·index와 disabled application 경계 배포
2. staging MongoDB에서 Transaction rollback·unique/CAS 검증
3. 격리 Firebase/mobile에서 fresh withdrawal proof 검증
4. pending login/enrollment 오류와 client UX 검증
5. LOCAL·GUEST 회귀 및 federated withdrawal E2E 통과
6. lifecycle 생성 metric·pending age·conflict alert 준비
7. 2단계 worker가 준비되기 전 production Firebase withdrawal 기능은 활성화하지 않음

탈퇴 요청만 받아 영구 pending을 쌓지 않도록 1단계와 2단계의 production 활성화는 같은 release train으로 묶거나 worker 선배포 후 endpoint를 연다.

## 15. 관측 항목

민감정보 없이 다음 낮은 cardinality event를 기록한다.

- `user.withdrawal.requested`
- `user.withdrawal.internal_committed`
- `user.withdrawal.idempotent`
- `user.withdrawal.conflict`
- `user.withdrawal.firebase_reauth_failed`

metric 후보:

- lifecycle status별 건수
- 가장 오래된 pending age
- credential type별 성공·실패 수
- lifecycle conflict 수
- Firebase 429·503 수

userId와 Firebase UID를 metric tag로 사용하지 않는다.

## 16. 완료 조건

- Firebase/SNS MEMBER가 fresh Firebase proof로 탈퇴할 수 있다.
- 잘못된 user·Firebase owner 조합은 내부 mutation 없이 거절된다.
- User WITHDRAWN, 모든 RefreshSession 폐기와 lifecycle 생성이 한 Transaction으로 commit된다.
- 성공 응답 유실·반복·동시 요청이 lifecycle 중복이나 재활성화를 만들지 않는다.
- lifecycle `EXTERNAL_CLEANUP_PENDING`이 2단계 worker가 처리할 충분한 target과 fencing 정보를 가진다.
- cleanup 중 같은 credential의 로그인·신규 enrollment가 전용 오류로 차단된다.
- 기존 LOCAL·GUEST 탈퇴 계약이 유지된다.
- Token·Provider credential·email·raw phone이 저장·응답·로그·Sentry에 노출되지 않는다.
- 전체 테스트와 staging Transaction·Firebase reauth E2E가 성공한다.
- 2단계 worker가 배포되기 전 production endpoint를 활성화하지 않는다.

## 17. 후속 단계 인계

### 2단계에 전달

- `EXTERNAL_CLEANUP_PENDING` lifecycle claim·lease·retry 계약
- Firebase target project·UID snapshot
- disable → refresh revoke → Provider 의무 → Firebase delete 순서
- 안전한 error code, nextAttemptAt과 reconciliation 상태

### 3단계에 전달

- external delete 완료 fencing
- FirebaseIdentity·SocialIdentity·PhoneIdentity/aliases release 조건
- `IDENTITY_RELEASE_PENDING → CLEANED` 전이
- CLEANED 이후에만 신규 enrollment를 허용하는 재가입 gate

