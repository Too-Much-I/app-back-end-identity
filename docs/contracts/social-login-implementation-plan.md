# Firebase 인증 broker 기반 Identity 전체 구현 계획서

## 1. 문서 목적과 결정 상태

이 문서는 Firebase Authentication에는 사용자의 credential 인증만 위임하고, 토선생 Identity Service는 canonical User와 가입·계정 lifecycle·자체 JWT/RefreshSession을 계속 소유하는 목표 구조를 정의한다.

목표 책임 경계는 다음과 같이 확정한다.

```text
Firebase Authentication
→ email/password, Google, Apple, phone credential 인증
→ Kakao는 Generic OIDC PoC 통과 시 같은 경계로 추가
→ Firebase ID Token 발급

Identity Service
→ Firebase ID Token 검증·교환
→ Firebase UID와 canonical UUID userId 매핑
→ User·SocialIdentity·PhoneIdentity
→ Guest 승격·canonical merge
→ 약관·프로필·상태·탈퇴
→ 자체 RS256 Access Token과 RefreshSession

Learning Core
→ Identity가 발급한 JWT만 검증
```

Firebase 인증 성공은 토선생 회원가입 완료와 같지 않다. 신규 사용자는 Firebase credential 인증 뒤에도 검증된 전화번호, 필수 동의와 Identity의 최종 Transaction이 모두 성공해야 `MEMBER`가 된다.

현재 구현 상태는 다음과 같다.

- 단계 1 `SocialIdentity` 기반은 Jira `TMI-88`, GitHub PR #16으로 완료됐다.
- 단계 2 `UserAccountType` 호환 expand는 Jira `TMI-89`, GitHub PR #17로 완료됐다.
- 단계 0은 Jira `TMI-90`으로 진행 중이다. 조건부 채택 ADR과 격리 Auth Emulator PoC를 추가했고 local contract 7개, Emulator 5개와 Kakao 공개 discovery 1개가 통과했다.
- 실제 Google·Apple 모바일 인증, 공식 client SDK phone link, 국내 SMS, Identity Platform의 Kakao 등록·billing·deep-link와 Apple revoke는 production 외부 gate로 남아 있다. 이 gate 전 Firebase 공개 API와 feature flag는 활성화하지 않는다.
- 운영 기존 회원은 없다는 제품 전제를 받았으므로 BCrypt credential migration은 계획하지 않는다. 실제 전환 직전에는 User 0건 또는 허용된 테스트 데이터만 존재하는지 read-only로 재확인한다.
- production Firebase 설정과 애플리케이션 코드 연동은 아직 수행하지 않았다.

## 2. 반드시 유지할 계약

- 실제 사용자 식별자는 Identity가 생성하는 UUID 문자열 `userId`다.
- Firebase UID, provider subject, email과 phone은 내부 `userId`가 아니다.
- Identity Access Token의 `sub`에는 canonical UUID `userId`를 넣는다.
- Access Token은 기존 RS256·`kid`·Identity issuer·`tosunsaeng-learning-core` audience·JWKS 계약을 유지한다.
- Firebase ID Token은 Identity의 인증 교환 endpoint에만 제출하며 Learning Core로 전달하지 않는다.
- 클라이언트가 보낸 `userId`를 신뢰하지 않고 외부 Request Body에 임의의 `userId`를 받지 않는다.
- Firebase UID, email과 phone 일치만으로 두 내부 User를 자동 병합하지 않는다.
- 외부 소셜 계정의 충돌 방지 기준은 검증된 `(provider, providerSubject)`다.
- 전화번호는 로그인 계정 식별자, JWT subject 또는 자동 merge 키로 사용하지 않는다.
- 검증된 휴대전화 번호는 동시에 하나의 `ACTIVE MEMBER`에만 귀속할 수 있다.
- 전화번호 unique는 회원가입 자격의 제약이지 canonical User 선택 규칙이 아니다. 같은 번호의 두 번째 MEMBER 생성은 `PHONE_ALREADY_LINKED`로 거절하고 자동 병합하지 않는다.
- 무료 모의고사는 `ACTIVE MEMBER`만 시작할 수 있으며 Guest에는 `MEMBERSHIP_REQUIRED`를 반환한다.
- 신규 MEMBER 생성·Guest 승격은 같은 Firebase account에서 검증된 phone proof와 필수 동의 없이는 완료하지 않는다.
- 무료체험 정책은 사람당 1회가 아니라 검증된 휴대전화 번호당 1회다.
- 전화번호 원문은 장기 저장하지 않고 versioned domain-separated HMAC-SHA-256 fingerprint를 사용한다.
- Refresh Token 원문은 DB와 로그에 저장하지 않는다.
- Firebase ID Token, Provider credential, Password, OTP code, phone 원문과 fingerprint를 로그·Sentry·metric에 남기지 않는다.
- Identity는 시험·시험 결과·TrialClaim·UserEntitlement·EntitlementReservation을 소유하지 않는다.
- Learning Core 데이터 이전은 서버 간 event 계약으로 수행하며 Identity가 Learning Core DB를 직접 수정하지 않는다.
- Python AI의 `user_id`는 계속 `examId`이며 실제 `userId`를 Python AI로 보내지 않는다.

## 3. 책임 경계

| 기능 | Firebase | Identity |
| --- | --- | --- |
| email/password 등록·검증 | 소유 | 검증 결과만 소비 |
| 비밀번호 reset·email verification | 소유 | 내부 Session 폐기 등 coordination |
| Google·Apple 인증 | 소유 | Firebase Token과 provider mapping 검증 |
| 전화번호 SMS·code 확인 | 소유 | 검증된 번호를 PhoneIdentity로 확정 |
| Kakao 인증 | Generic OIDC PoC 통과 시 소유 | PoC·mapping·feature flag |
| Firebase ID Token | 발급 | 검증 후 내부 Token으로 교환 |
| canonical userId | 소유하지 않음 | UUID 생성·소유 |
| User 상태·프로필·동의 | 소유하지 않음 | 소유 |
| Guest·MEMBER·merge | 소유하지 않음 | 소유 |
| SocialIdentity·PhoneIdentity | 소유하지 않음 | 소유 |
| 앱 Access/Refresh Token | 사용하지 않음 | 소유 |
| Learning Core 인증 계약 | 관여하지 않음 | issuer·audience·JWKS 소유 |

Firebase custom claims는 서비스 User DB를 대체하지 않는다. Claims에는 Token 검증에 필요한 제한적이고 안정적인 정보만 허용하며, 약관·프로필·merge 상태·혜택 원장을 넣지 않는다.

## 4. 목표 아키텍처

```text
Client
  │
  ├─ email/password
  ├─ Google
  ├─ Apple
  ├─ Kakao OIDC (PoC 통과 시)
  └─ phone verification/link
          │
          v
Firebase Authentication
          │ Firebase ID Token
          v
Identity FirebaseAuthVerifier
          │
          ├─ FirebaseIdentity(firebaseUid → canonical userId)
          ├─ SocialIdentity(provider subject → canonical userId)
          ├─ PhoneIdentity(verified phone → canonical userId)
          └─ User(canonical account)
          │
          v
Identity Access/Refresh Token
JWT sub = canonical UUID userId
          │
          v
Learning Core
```

### 4.1 기존 계정 로그인

```text
Firebase 인증
→ Firebase ID Token 검증
→ FirebaseIdentity 조회
→ ACTIVE MEMBER 확인
→ Identity Access/Refresh 발급
```

### 4.2 Guest를 거치지 않는 신규 회원가입

```text
Firebase email/social credential 인증
→ 아직 FirebaseIdentity 없음
→ Identity가 ENROLLMENT_REQUIRED 반환
→ 현재 Firebase user에 phone credential을 link
→ link 전후 Firebase UID 동일성 확인
→ fresh Firebase ID Token 제출 + 필수 동의
→ User + FirebaseIdentity + PhoneIdentity
  + SocialIdentity 0..N + RefreshSession
  + enrollment consume를 한 Transaction으로 처리
```

### 4.3 Guest의 MEMBER 승격

```text
Identity JWT sub=user_A(GUEST)
→ Firebase credential 인증
→ 현재 Firebase user에 phone credential을 link
→ Guest-bound enrollment finalize
→ user_A를 MEMBER로 승격
→ FirebaseIdentity·PhoneIdentity·SocialIdentity 생성
→ canonical userId는 user_A 유지
```

### 4.4 기존 Firebase identity 발견

```text
user_B(GUEST)가 Firebase UID 또는 provider subject를 제시
→ 이미 user_A(MEMBER)의 identity
→ 단계 7 전: MERGE_REQUIRED, mutation 없음
→ 단계 7 활성화 후: user_B → user_A merge
```

여러 기기에서 같은 외부 계정을 제시할 때 클라이언트와 서버의 proof는 다음과 같다.

```text
Device B의 Identity Access Token
→ sub = user_B(GUEST)

같은 Google 계정으로 Firebase sign-in
→ 기존 Firebase User firebase_A로 인증
→ firebase_A의 fresh Firebase ID Token

두 Token을 /auth/firebase/guest/prepare에 함께 제출
→ Identity가 user_B의 Guest 소유권과
  firebase_A → user_A(MEMBER) 소유권을 각각 검증
→ MERGE_REQUIRED(user_B → user_A)
```

클라이언트가 임시 Firebase User에 Google credential을 link하다 credential ownership 충돌을 받으면 credential을 임의 이전하지 않는다. 현재 Identity Guest Session은 유지한 채 Firebase 인증 상태를 기존 Google 소유 Firebase User로 전환하고, 그 User의 fresh ID Token과 Guest JWT를 함께 제출한다. email이나 phone만으로 target을 선택하지 않는다.

## 5. Identity 데이터 모델

### 5.1 User

`User`는 credential이 아니라 토선생 계정 자체다.

```text
User
- userId: canonical UUID
- accountType: GUEST / MEMBER
- status: ACTIVE / SUSPENDED / WITHDRAWN / MERGED
- profile·consents
- mergedIntoUserId, mergedAt: merge 단계에서 추가
```

규칙:

- Firebase UID를 `User._id`나 JWT `sub`로 사용하지 않는다.
- MEMBER라는 사실만으로 passwordHash나 특정 SocialIdentity 존재를 강제하지 않는다.
- Firebase 채택 뒤 신규 MEMBER에는 Identity passwordHash를 쓰지 않는다.
- 기존 `UserProvider.LOCAL/GUEST`와 profile `provider`는 호환 제거 단계 전까지만 유지한다.
- email을 User에 보관할지는 profile/contact 요구로 별도 결정하되 로그인 조회·merge 키로 사용하지 않는다.
- 기존 `check-email`, 직접 `signup`, 직접 `login` endpoint는 Firebase cutover 전에 deprecate 또는 제거한다.

### 5.2 FirebaseIdentity

Firebase broker account와 canonical User의 매핑을 별도 collection으로 관리한다.

```text
FirebaseIdentity
- firebaseIdentityId: UUID 문자열
- firebaseProjectId: 허용된 Firebase project namespace
- firebaseUid: 1..128자의 opaque Firebase UID
- userId: canonical UUID 문자열
- createdAt
```

권장 index:

```text
uk_firebase_identities_project_uid
keys: firebaseProjectId ASC, firebaseUid ASC
unique: true

uk_firebase_identities_user_id
keys: userId ASC
unique: true
```

첫 정책은 canonical User 한 명에 Firebase account 한 개다. Google·Apple·email/password·phone은 별 Firebase User를 만들지 않고 같은 Firebase UID에 명시적으로 link한다.

- FirebaseIdentity는 raw Token, email, phone과 provider credential을 저장하지 않는다.
- Firebase UID는 query를 위해 저장하지만 로그·Sentry·metric에는 넣지 않는다.
- Firebase UID가 없고 검증된 SocialIdentity만 기존 User를 가리키는 복구 상황은 자동 merge가 아니라 broker rebind 정책으로 처리한다.
- 기존 User가 이미 다른 Firebase UID를 가지면 `FIREBASE_IDENTITY_CONFLICT`로 거절하고 별도 복구 흐름으로 보낸다.

### 5.3 SocialIdentity

이미 구현된 `social_identities` 모델을 유지한다.

```text
SocialIdentity
- socialIdentityId: UUID 문자열
- userId: canonical UUID 문자열
- provider: GOOGLE / KAKAO / APPLE
- providerSubject: 1..255자 case-sensitive opaque 값
- createdAt
```

Firebase 구조에서 `providerSubject`는 클라이언트 Claim이 아니라 검증된 Firebase Admin `UserRecord.providerData`의 provider별 stable UID에서 얻는다.

- `google.com` → `GOOGLE`
- `apple.com` → `APPLE`
- 승인된 Kakao OIDC provider ID → `KAKAO`

Kakao provider ID와 subject 원천은 PoC 뒤 한 namespace로 고정한다. Generic OIDC가 실패했다고 Kakao REST user id와 OIDC subject를 같은 namespace에서 혼용하지 않는다.

기존 index를 유지한다.

```text
uk_social_identities_provider_subject
keys: provider ASC, providerSubject ASC
unique: true

ix_social_identities_user_id
keys: userId ASC
unique: false
```

한 Firebase UID에 link된 여러 provider는 같은 canonical User의 여러 SocialIdentity로 동기화한다. email은 SocialIdentity에 저장하거나 lookup key로 사용하지 않는다.

### 5.4 PhoneIdentity와 PhoneFingerprintAlias

```text
PhoneIdentity
- phoneIdentityId: UUID 문자열
- userId: canonical UUID 문자열
- fingerprintKeyVersion
- phoneFingerprint
- phoneLast4
- verifiedAt
- createdAt

PhoneFingerprintAlias
- aliasId: UUID 문자열
- phoneIdentityId
- fingerprintKeyVersion
- phoneFingerprint
- createdAt
```

권장 index:

- `PhoneIdentity.userId` unique: User당 활성 번호 한 개
- `(PhoneFingerprintAlias.fingerprintKeyVersion, phoneFingerprint)` unique
- `PhoneFingerprintAlias.phoneIdentityId` non-unique

Firebase가 phone 소유 검증을 담당하더라도 Identity는 무료체험 중복 방지와 내부 정책을 위해 PhoneIdentity를 소유한다.

정책은 다음과 같이 고정한다.

- 검증된 번호는 동시에 하나의 ACTIVE MEMBER PhoneIdentity에만 귀속한다.
- Firebase phone credential도 반드시 그 MEMBER의 기존 Firebase UID에 link한다.
- 같은 번호가 다른 Firebase User나 PhoneIdentity에 연결돼 있으면 `PHONE_ALREADY_LINKED`로 거절한다.
- 이 충돌만으로 기존 User를 target으로 선택하거나 두 User를 자동 병합하지 않는다.
- `SUSPENDED` 계정도 제재 우회를 막기 위해 번호 점유를 유지하며, `WITHDRAWN` lifecycle의 unlink/delete와 PhoneIdentity·alias 개인정보 삭제가 끝난 뒤에만 가입 uniqueness를 해제한다.
- 회원탈퇴 후 번호 재사용을 허용하려면 Firebase phone unlink/delete와 PhoneIdentity 개인정보 삭제가 완료돼야 한다. 별도 Entitlement의 TrialClaim 보존 정책은 번호 재사용과 무관하게 적용한다.

### 5.5 FirebaseEnrollmentAttempt

Firebase User 생성과 Identity MEMBER 생성 사이의 중단 가능한 흐름을 별도 단기 entity로 관리한다.

```text
FirebaseEnrollmentAttempt
- enrollmentId: UUID 문자열
- firebaseProjectId
- firebaseUid
- bindingType: DIRECT_SIGNUP / GUEST_USER
- boundUserId: GUEST_USER일 때 현재 JWT sub
- initialSignInProvider
- status: PENDING / CONSUMED / EXPIRED
- expiresAt
- cleanupAt
- createdAt
- consumedAt
```

규칙:

- Firebase ID Token, provider credential, email과 phone 원문은 저장하지 않는다.
- Mongo TTL 삭제 시점에 유효성 판단을 의존하지 않고 application이 `expiresAt`과 status를 검사한다.
- DIRECT_SIGNUP은 verified Firebase UID에, GUEST_USER는 Firebase UID와 현재 Guest JWT `sub` 모두에 묶는다.
- finalize에서 fresh Firebase ID Token을 다시 검증하고 attempt와 같은 UID인지 확인한다.
- `PENDING → CONSUMED`는 User·Identity·PhoneIdentity·RefreshSession 저장 Transaction 안에서 conditional CAS로 수행한다.
- 같은 attempt 동시 finalize의 CAS loser는 새 User나 Session을 남기지 않는다.
- 만료된 attempt는 재인증 또는 resume 정책에 따라 새 attempt로 시작한다.

같은 binding의 활성 attempt는 하나만 허용한다.

```text
DIRECT_SIGNUP
→ firebaseProjectId + firebaseUid + bindingType

GUEST_USER
→ firebaseProjectId + firebaseUid + bindingType + boundUserId
```

권장 index는 다음과 같이 binding 필드에 `status=PENDING` 조건을 둔 partial unique index다.

```text
uk_firebase_enrollment_pending_binding
keys: firebaseProjectId, firebaseUid, bindingType, boundUserId
unique: true
partialFilter: status == PENDING
```

DIRECT_SIGNUP의 `boundUserId`는 명시적 null로 일관되게 저장한다. 처리 순서는 다음과 같다.

1. 같은 binding의 PENDING attempt를 조회한다.
2. `expiresAt > now`면 새 document를 만들지 않고 기존 `enrollmentId`와 남은 요구사항을 반환한다.
3. application 기준으로 만료됐으면 조건부 CAS로 `PENDING → EXPIRED` 전환한다.
4. 새 attempt insert의 duplicate key는 동시 요청의 승자가 만든 PENDING attempt를 재조회해 반환한다.

TTL은 cleanup만 담당하고 active 여부는 언제나 status와 `expiresAt`으로 판단한다. 유효 attempt 재조회 시 만료 시간을 자동 연장하지 않아 무기한 enrollment가 되지 않게 한다.

### 5.6 RefreshSession

Firebase ID Token은 외부 credential proof고, 토선생 로그인 Session은 기존 RefreshSession이 관리한다.

- 신규 내부 Session은 canonical `userId`에 귀속한다.
- Refresh Token 원문은 응답에만 전달하고 DB에는 hash만 저장한다.
- Firebase UID는 Refresh Token 원문이나 JWT Claim에 노출하지 않는다.
- logout은 내부 RefreshSession 폐기와 client Firebase sign-out의 조합이다.
- logout-all, password reset, provider unlink와 withdrawal은 Firebase refresh token revoke와 내부 Session 폐기의 coordination 정책을 갖는다.

## 6. Firebase 인증 adapter

### 6.1 기술 경계

Controller와 application service가 Firebase Admin SDK를 직접 호출하지 않는다.

```java
interface FirebaseAuthenticationVerifier {
    VerifiedFirebasePrincipal verify(
            String firebaseIdToken,
            FirebaseVerificationPurpose purpose
    );
}
```

```text
VerifiedFirebasePrincipal
- firebaseProjectId
- firebaseUid
- signInProvider
- authTime
- issuedAt
- emailVerified 여부
- verifiedPhoneNumber optional
- linkedSocialPrincipals
```

`linkedSocialPrincipals`는 승인된 provider ID와 stable provider UID만 포함한다. Firebase SDK 객체, raw Token, 전체 Claim과 provider email은 application layer로 전달하지 않는다.

### 6.2 필수 검증

- Firebase ID Token signature
- 허용한 algorithm
- 환경별 Firebase project issuer와 audience
- tenant를 사용할 경우 예상 tenant
- `exp`, `iat`, `auth_time`
- 비어 있지 않은 Firebase UID
- 허용된 `firebase.sign_in_provider`
- disabled user 여부
- 목적별 revoke 검사
- high-risk signup·link·withdrawal의 recent authentication
- Admin UserRecord의 providerData와 phone state

클라이언트가 보낸 provider 문자열, email, phone과 provider subject를 인증 근거로 신뢰하지 않는다. Identity는 Firebase Token과 Admin UserRecord에서만 검증 결과를 만든다.

Firebase 오류 message나 Token 내용은 API 응답·로그에 넣지 않고 안정적인 내부 error code로 변환한다.

Firebase Admin SDK의 revocation 검사는 추가 remote lookup이 필요한 것으로 보고 목적별로 적용한다.

| 경로 | Firebase revoke/disabled 검사 | recent-auth | Firebase 호출 |
| --- | --- | --- | --- |
| Firebase login exchange | 적용 | 로그인 시각 상한 적용 | 있음 |
| 신규 signup finalize | 적용 | 적용 | 있음 |
| Guest upgrade·merge proof | 적용 | 적용 | 있음 |
| provider link·unlink·phone 변경 | 적용 | 적용 | 있음 |
| withdrawal | 적용 | 적용 | 있음 |
| Identity RefreshSession reissue | 매 요청 적용하지 않음 | 적용하지 않음 | 없음 |
| Learning Core 일반 요청 | 적용하지 않음 | 적용하지 않음 | 없음 |

Firebase remote 검사가 필요한 경로는 timeout·quota·일시 장애를 성공으로 우회하지 않고 안전한 503/429로 변환한다. 내부 reissue가 Firebase 장애 중에도 가능한 범위와 강제 Session 폐기 전파 지연은 ADR에서 고정한다.

### 6.3 인증 목적

```text
LOGIN_EXCHANGE
DIRECT_ENROLLMENT
GUEST_ENROLLMENT
GUEST_MERGE
AUTH_METHOD_SYNC
HIGH_RISK_REAUTHENTICATION
```

- LOGIN_EXCHANGE는 정상 Firebase 로그인 후 내부 Session 교환이다.
- DIRECT/GUEST enrollment는 fresh Token, 같은 UID와 phone proof를 요구한다.
- GUEST_MERGE는 현재 Identity Guest JWT와 기존 target Firebase User의 fresh Token을 함께 요구한다.
- AUTH_METHOD_SYNC는 현재 Identity JWT와 FirebaseIdentity의 UID 일치를 요구한다.
- HIGH_RISK는 짧은 recent-auth 기준과 revoke check를 적용한다.

### 6.4 Provider 정책

#### Email/password

- 클라이언트가 Firebase SDK로 가입·로그인한다.
- Identity는 Password를 받거나 BCrypt로 비교하지 않는다.
- email verification을 가입 필수로 할지는 단계 0에서 결정한다.
- email은 자동 merge나 canonical identity 복구 키가 아니다.

#### Google

- Firebase Google provider를 사용한다.
- Identity는 Google ID Token을 직접 받거나 검증하지 않는다.
- SocialIdentity subject는 Firebase Admin providerData의 Google UID다.

#### Apple

- Firebase Apple provider를 사용한다.
- Identity는 Apple identity token을 직접 로그인 endpoint에서 받지 않는다.
- SocialIdentity subject는 Firebase Admin providerData의 Apple UID다.
- Apple account deletion revoke는 Firebase 지원 API와 Apple 요구사항을 함께 만족하도록 lifecycle 단계에서 구현한다.

#### Phone

- Firebase client SDK가 SMS 발송과 code 확인을 담당한다.
- Identity endpoint는 raw OTP code를 받지 않는다.
- 신규 MEMBER의 phone은 먼저 인증한 email/Google/Apple/Kakao Firebase User에 `linkWithCredential` 계열 흐름으로 연결한다.
- phone link 전후 current Firebase UID가 같아야 하며, link 성공 뒤 강제 갱신한 ID Token과 Admin UserRecord에서 phone provider 연결을 재확인한다.
- enrollment 중 `signInWithPhoneNumber`·phone credential 직접 sign-in으로 별도 Firebase User를 만드는 흐름은 금지한다.
- phone MFA는 기본 가입 흐름이 아니며 필요하면 별도 ADR로 도입한다.
- phone-only `sign_in_provider`는 토선생 로그인 수단으로 허용하지 않는다. 전화번호는 가입 proof이지 로그인 ID가 아니다.

#### Kakao

- 1순위는 Firebase Authentication with Identity Platform의 Generic OIDC다.
- Firebase Authentication project의 Identity Platform 업그레이드 필요성, billing 활성화와 비용 영향을 PoC에서 확인한다.
- Kakao issuer·discovery metadata로 `oidc.*` provider 등록이 가능한지 확인한다.
- issuer·discovery·redirect·providerData stable UID·mobile client 동작을 PoC한다.
- Android/iOS redirect, custom scheme 또는 universal/app link, deep-link 복귀와 계정 link 동작을 실제 client PoC에 포함한다.
- PoC가 통과할 때만 승인된 OIDC provider ID를 `KAKAO`로 mapping한다.
- Generic OIDC가 불가능하면 Kakao를 자동으로 직접 verifier로 되돌리지 않는다.
- Custom Token bridge는 Identity의 Kakao credential 검증 책임이 다시 생기므로 별도 ADR·보안 검토 후에만 허용한다.
- 결론 전까지 Kakao feature flag는 꺼 둔다.

### 6.5 설정과 Secret

권장 환경 설정 이름만 저장소에 둔다.

```text
FIREBASE_AUTH_ENABLED
FIREBASE_PROJECT_ID
FIREBASE_TENANT_ID
FIREBASE_GOOGLE_ENABLED
FIREBASE_APPLE_ENABLED
FIREBASE_KAKAO_ENABLED
FIREBASE_PHONE_ENABLED
```

- 실제 service credential과 Key는 저장소에 두지 않는다.
- 배포 runtime identity 또는 승인된 Secret 주입 방식을 사용한다.
- local/test 기본값은 Firebase disabled다.
- `application-test.yml`은 실제 Firebase·Google·Apple·Kakao·SMS endpoint를 호출하지 않는다.
- Firebase adapter는 mock 가능한 내부 interface 뒤에 둔다.
- emulator 또는 격리 Firebase project PoC는 기본 `./gradlew test`와 분리한다.

## 7. API와 사용자 흐름

API 이름은 클라이언트 합의 후 확정하되 다음 책임을 유지한다.

### 7.1 Firebase Token 교환

```http
POST /api/v1/auth/firebase/exchange
```

```text
인증: 공개
입력: firebaseIdToken(writeOnly)
출력:
- AUTHENTICATED: Identity AuthResponse
- ENROLLMENT_REQUIRED: enrollmentId, missingRequirements, expiresIn
```

처리:

1. Firebase ID Token을 LOGIN_EXCHANGE 목적으로 검증한다.
2. `FirebaseIdentity.firebaseUid`를 조회한다.
3. 있으면 canonical User가 ACTIVE MEMBER인지 확인하고 자체 Access/Refresh를 발급한다.
4. 없으면 verified social subject로 기존 SocialIdentity owner가 있는지 확인한다.
5. 기존 owner가 있고 FirebaseIdentity가 비어 있으면 승인된 broker rebind 규칙으로 연결한다.
6. 기존 owner가 다른 Firebase UID를 가지면 conflict로 거절한다.
7. owner가 없으면 User를 만들지 않고 같은 Firebase project·UID·binding의 유효한 DIRECT_SIGNUP FirebaseEnrollmentAttempt를 조회한다.
8. 유효 PENDING attempt가 있으면 기존 `enrollmentId`를 재사용하고, 없으면 partial unique index 경쟁 아래 새 attempt 하나만 생성한다.
9. phone·email verification·profile·consent 요구 상태를 `missingRequirements`로 반환한다.

Firebase 인증 성공만으로 User·PhoneIdentity·RefreshSession을 미리 만들지 않는다.

### 7.2 신규 email/password 가입

클라이언트 순서:

```text
Firebase createUserWithEmailAndPassword
→ 필요하면 email verification
→ 현재 Firebase UID 기록
→ SMS code로 PhoneAuthCredential 획득
→ current user에 phone credential link
→ link 전후 Firebase UID 동일성 확인
→ getIdToken(forceRefresh=true)
→ Identity firebase signup finalize
```

신규 가입 중 phone credential로 직접 sign-in해 두 번째 Firebase User를 만드는 흐름은 금지한다. phone link가 이미 다른 Firebase User 소유라 실패하면 신규 내부 User를 만들지 않고 `PHONE_ALREADY_LINKED` 또는 승인된 계정 복구 경로로 보낸다.

Identity는 기존 email/password signup request를 받지 않는다. 공개 전환 전에 다음 legacy route를 제거하거나 명시적으로 폐기한다.

- `POST /api/v1/auth/check-email`
- 기존 Password Body 기반 `POST /api/v1/auth/signup`
- 기존 Password Body 기반 `POST /api/v1/auth/login`

클라이언트 계약 전환 기간에 두 route를 병행해야 한다면 Firebase flag 활성화 전까지만 허용하고 dual credential writer는 운영에서 동시에 열지 않는다.

### 7.3 신규 MEMBER 최종 가입

```http
POST /api/v1/auth/firebase/signup
```

```text
인증: 공개
입력:
- enrollmentId
- fresh firebaseIdToken(writeOnly)
- nickname
- 개인정보 처리방침·이용약관 동의와 version
출력: canonical User 기준 Identity AuthResponse
```

최종 Transaction:

1. FirebaseEnrollmentAttempt가 PENDING·미만료인지 확인한다.
2. fresh Token의 Firebase UID가 attempt와 같은지 확인한다.
3. email/social primary credential과 같은 UID의 verified phone을 확인한다.
4. 필수 동의와 profile을 검증한다.
5. canonical UUID MEMBER User를 생성한다.
6. FirebaseIdentity를 생성한다.
7. verified phone으로 PhoneIdentity와 retained-version aliases를 생성한다.
8. allowlist된 consumer scope의 benefit fingerprint candidate와 `PhoneEligibilityBindingOutbox`를 생성한다.
9. providerData의 Google·Kakao·Apple SocialIdentity를 생성한다.
10. 최초 RefreshSession을 저장한다.
11. enrollment attempt를 conditional consume한다.

위 저장은 하나의 Mongo Transaction으로 성공하거나 rollback한다. Access Token은 최종 canonical userId를 사용하고 응답은 Transaction 성공 뒤에만 반환한다.

### 7.4 기존 MEMBER 로그인

```text
Client Firebase sign-in
→ Firebase ID Token
→ /auth/firebase/exchange
→ FirebaseIdentity owner 조회
→ User ACTIVE 확인
→ Identity Access/Refresh 발급
```

- 매 로그인마다 phone OTP를 요구하지 않는다.
- Firebase Token이 valid해도 User가 SUSPENDED·WITHDRAWN·MERGED면 내부 Token을 발급하지 않는다.
- Firebase sign-in provider가 phone-only면 로그인 교환을 거절한다.

### 7.5 Guest의 Firebase enrollment

```http
POST /api/v1/auth/firebase/guest/prepare
Authorization: Bearer <Identity Access Token>
```

```text
입력: firebaseIdToken(writeOnly)
출력:
- ENROLLMENT_REQUIRED
- ALREADY_LINKED
- MERGE_REQUIRED
```

prepare는 현재 JWT `sub`의 ACTIVE GUEST와 Firebase UID를 묶은 attempt를 생성한다.

```http
POST /api/v1/auth/firebase/guest/upgrade
Authorization: Bearer <Identity Access Token>
```

```text
입력:
- enrollmentId
- fresh firebaseIdToken(writeOnly)
- 필수 동의·profile
출력: 같은 canonical userId 기준 Identity AuthResponse
```

미연결 Firebase identity면 한 Transaction으로 다음을 수행한다.

- ACTIVE GUEST conditional update
- accountType을 MEMBER로 승격
- guestInstallationIdHash 제거
- FirebaseIdentity 생성
- PhoneIdentity·aliases 생성
- PhoneEligibilityBindingOutbox 생성
- SocialIdentity 동기화
- 기존 Guest RefreshSession 폐기
- 새 MEMBER RefreshSession 저장
- enrollment attempt consume

Firebase UID 또는 SocialIdentity가 다른 ACTIVE MEMBER를 가리키면 단계 7 전에는 `MERGE_REQUIRED`만 반환하고 source·target을 변경하지 않는다.

다른 기기의 같은 SNS 계정은 Guest용 Firebase User에 다시 link하지 않는다. 클라이언트는 기존 SNS 소유 Firebase User로 sign-in해 fresh Firebase ID Token을 얻고, 현재 Guest JWT와 함께 prepare에 제출한다. Identity는 두 Token을 독립 검증해 `source=user_B(GUEST)`, `target=user_A(MEMBER)`를 결정하며, Firebase credential 충돌·email·phone만으로 target을 추정하지 않는다.

### 7.6 MEMBER의 인증수단 추가 연결

클라이언트는 같은 Firebase current user에 `linkWithCredential` 또는 승인된 OIDC linking을 수행하고 fresh Token을 받는다.

```http
POST /api/v1/auth/firebase/auth-methods/sync
Authorization: Bearer <Identity Access Token>
```

```text
입력: fresh firebaseIdToken(writeOnly)
출력: linkedProviders
```

Identity는 다음을 확인한다.

- 현재 JWT User가 ACTIVE MEMBER
- FirebaseIdentity.userId가 현재 JWT `sub`
- fresh Token UID가 FirebaseIdentity UID
- recent authentication
- Admin UserRecord의 providerData

그 후 누락된 SocialIdentity를 현재 canonical User에 추가한다. 다른 User가 같은 `(provider, providerSubject)`를 소유하면 자동 병합하지 않고 conflict로 거절한다.

phone credential sync는 번호 변경 전용 검증 Transaction이 없으면 기존 PhoneIdentity를 덮어쓰지 않는다.

### 7.7 Refresh·logout

- 내부 `/reissue`는 기존 Identity RefreshSession 계약을 사용한다.
- 매 Learning Core 요청이나 내부 refresh마다 Firebase endpoint를 호출하지 않는다.
- login exchange와 high-risk 작업에서는 revoke·disabled 검사를 적용한다.
- 앱 logout은 내부 logout 성공과 Firebase client sign-out을 모두 수행한다.
- logout-all·password reset·withdrawal은 내부 RefreshSession 폐기와 Firebase refresh token revoke를 coordination한다.
- Identity Access Token은 stateless이므로 즉시 폐기 한계와 최대 TTL을 기존 계약대로 관리한다.

## 8. Firebase phone proof와 PhoneIdentity

### 8.1 phone은 인증 보조 수단

전화번호는 회원가입 시 소유 검증과 무료체험 중복 방지 입력이다.

- phone-only Firebase sign-in으로 Identity 로그인을 허용하지 않는다.
- phone을 Firebase UID 또는 canonical userId 선택 근거로 사용하지 않는다.
- 한 verified phone은 동시에 한 Firebase User와 한 ACTIVE MEMBER PhoneIdentity에만 연결한다.
- 같은 phone 충돌은 `PHONE_ALREADY_LINKED`이며 자동 merge 근거가 아니다.
- 신규 MEMBER finalize 시에만 PhoneIdentity를 만든다.
- Firebase phone 인증만 하고 가입을 중단하면 Identity DB의 PhoneIdentity는 점유하지 않지만, phone이 link된 Firebase User는 해당 번호를 계속 점유한다. resume 유예 기간 뒤 unlink/delete cleanup이 필요하다.

Firebase project 안에서도 한 전화번호를 여러 Firebase User에 동시에 link할 수 없다는 제약을 제품 정책과 일치시킨다. 기본 가입 방식은 동일 Firebase UID에 대한 phone credential link로 고정한다. PoC는 link 전후 UID 동일성, 강제 갱신 Token의 phone provider 반영, Admin UserRecord 일치와 이미 사용 중인 번호 충돌을 검증한다. phone MFA는 이 기본 계약에 포함하지 않는다. phone credential link로 phone-only sign-in이 가능해질 수 있으므로 Identity exchange에서는 계속 명시적으로 거절한다.

### 8.2 정규화와 fingerprint

Firebase가 제공한 verified phone도 서버에서 E.164 형식과 국가 정책을 재검증한다.

```text
phoneFingerprint = HMAC-SHA-256(
    keyFor(fingerprintKeyVersion),
    "phone-identity:v1" + separator + normalizedE164
)
```

- 일반 SHA-256만 사용하지 않는다.
- HMAC input에는 고정 domain separator를 넣는다.
- Key는 저장소 밖 Secret 경계에 둔다.
- 정확히 하나의 `ACTIVE_WRITE`와 0..N `LOOKUP_ONLY` version을 허용한다.
- phone 원문을 확인할 수 있는 finalize 순간 모든 retained key version fingerprint를 계산한다.
- PhoneIdentity는 current fingerprint, PhoneFingerprintAlias는 retained version 전체를 저장한다.
- `LOOKUP_ONLY` 참조가 남으면 Key를 제거하지 않는다.
- mixed writer 배포를 금지한다.
- phone 원문·last4·fingerprint·Key를 로그·Sentry·metric에 넣지 않는다.

### 8.3 abuse 방어

Firebase가 SMS code를 처리해도 다음 운영 방어가 필요하다.

- Firebase Phone Auth quota·billing alert
- 허용 국가와 mobile/VoIP 정책
- reCAPTCHA·APNs silent notification 등 Firebase client anti-abuse 설정
- 필요 시 Firebase App Check
- Identity exchange·enrollment의 IP·Firebase UID·Guest user rate limit
- enrollment attempt TTL·동시 finalize CAS
- phone alias unique collision 관측
- provider kill switch

Identity는 OTP code를 생성·저장·확인하지 않는다.

## 9. Transaction과 동시성

### 9.1 Guest를 거치지 않는 신규 가입

다음을 한 Mongo Transaction으로 처리한다.

```text
User
+ FirebaseIdentity
+ PhoneIdentity
+ PhoneFingerprintAlias 1..N
+ PhoneEligibilityBindingOutbox
+ SocialIdentity 0..N
+ RefreshSession
+ FirebaseEnrollmentAttempt consume
```

최종 경쟁 경계:

- `(firebaseProjectId, firebaseUid)` unique
- FirebaseIdentity userId unique
- `(provider, providerSubject)` unique
- PhoneFingerprintAlias `(version, fingerprint)` unique
- enrollment conditional consume
- binding별 PENDING enrollment partial unique

unique 충돌 뒤 실제 owner를 재조회해 멱등 성공, 기존 login 안내, phone conflict 또는 broker identity conflict로 분류한다.

### 9.2 Guest 승격

- source는 ACTIVE GUEST여야 한다.
- expected `updatedAt` 또는 version으로 conditional update한다.
- canonical userId를 바꾸지 않는다.
- FirebaseIdentity·PhoneIdentity·SocialIdentity와 새 Session 저장이 실패하면 승격도 rollback한다.
- 성공 시 기존 Guest RefreshSession을 모두 폐기한다.

### 9.3 Firebase와 Mongo는 분산 Transaction이 아님

Firebase User 또는 provider link는 Mongo Transaction 전에 존재한다. Mongo finalize가 실패해도 Firebase 작업은 자동 rollback되지 않는다.

따라서 다음 정책이 필요하다.

- 실패한 finalize는 유효 attempt에서 재시도 가능
- 내부 unique conflict면 Firebase user를 자동으로 다른 User에 연결하지 않음
- 가입 중단 Firebase user는 resume 또는 승인된 cleanup job으로 처리
- 가입 중단 user에 phone credential이 link돼 있으면 Identity DB에는 PhoneIdentity가 없어도 Firebase에서는 번호가 점유된다. cleanup은 resume 유예 기간 뒤 phone unlink 또는 Firebase User delete까지 완료해야 한다
- cleanup은 최근 로그인·provider link·내부 mapping 부재를 다시 확인
- Firebase delete/revoke 실패는 retry 가능한 lifecycle outbox/job으로 처리
- 실제 Firebase 오류 message와 credential은 outbox에 저장하지 않음

## 10. Guest merge와 Learning Core

canonical 선택 규칙:

- 이미 FirebaseIdentity 또는 SocialIdentity를 소유한 ACTIVE MEMBER가 target이다.
- source는 ACTIVE GUEST만 허용한다.
- MEMBER → MEMBER 자동 merge는 금지한다.
- phone·email·Firebase email 일치는 merge 근거가 아니다.
- `sourceUserId != targetUserId`를 강제한다.
- SUSPENDED·WITHDRAWN·MERGED User는 source/target으로 직접 사용하지 않는다.
- merged chain은 최대 깊이·visited set으로 순환을 거절한다.

Identity merge Transaction:

```text
source User → MERGED
source RefreshSession 전체 폐기
UserMergedOutbox 저장
target RefreshSession 저장
```

outbox event:

```text
UserMerged
- eventId
- schemaVersion
- sourceUserId
- targetUserId
- occurredAt
```

publisher는 atomic lease claim, retry·backoff·dead-letter 기준과 at-least-once 전달을 갖는다. Learning Core는 `eventId`로 멱등 처리하고 source ownership 이전과 source actor deny marker를 같은 로컬 Transaction으로 저장한다.

source JWT를 target actor로 해석하지 않는다.

- Identity는 MERGED source JWT를 `ACCOUNT_MERGED_TOKEN_REJECTED`로 거절한다.
- downstream은 migration 뒤 source actor를 거절한다.
- alias는 ownership migration과 idempotency용이며 authorization alias가 아니다.
- 모든 보호 서비스가 준비되기 전 merge feature flag를 켜지 않는다.

## 11. TrialClaim·Entitlement·무료 모의고사

Identity와 Entitlement/Billing 경계를 유지한다.

```text
Identity
→ ACTIVE MEMBER와 PhoneIdentity 검증
→ benefit-scoped phone fingerprint candidate/proof

Entitlement/Billing
→ TrialClaim: 검증 번호별 혜택 지급 ledger
→ UserEntitlement: canonical User의 실제 사용권
→ EntitlementReservation: 시험 생성 동안 사용권 예약

Learning Core
→ reserve → exam 생성 → confirm
```

회원가입 또는 Firebase phone 인증 시 TrialClaim을 생성하지 않는다. 사용자가 처음 무료시험을 요청할 때만 silent claim한다.

PhoneIdentity fingerprint와 혜택 fingerprint는 같은 값을 재사용하지 않는다.

```text
PhoneIdentity fingerprint
= HMAC-SHA-256(
    identityPhoneKey(version),
    "phone-identity:v1" + separator + normalizedE164
  )

FREE_MOCK_EXAM fingerprint
= HMAC-SHA-256(
    trialBenefitKey(version),
    "benefit:FREE_MOCK_EXAM:v1" + separator + normalizedE164
  )
```

- 두 fingerprint는 별도 Secret·key version·domain separator를 사용해 값이 같지 않아야 한다.
- Identity의 PhoneIdentity fingerprint를 Entitlement/Billing에 전달하거나 TrialClaim key로 재사용하지 않는다.
- Entitlement/Billing에는 raw phone 대신 해당 benefit에만 유효한 fingerprint candidate/proof만 전달한다.
- benefit fingerprint만으로 PhoneIdentity collection의 동일 번호를 비교할 수 없어야 한다.

전화번호 원문을 장기 저장하지 않으므로 benefit fingerprint는 첫 시험 시점에 새로 계산할 수 없다. 다음 전달 계약을 단계 9 전에 확정한다.

1. 신규 가입·phone 교체 finalize에서 검증된 E.164가 메모리에 존재할 때 retained benefit key version의 fingerprint candidate를 함께 파생한다.
2. Identity의 같은 Transaction에 consumer-scoped proof/outbox를 저장하고 raw E.164는 저장하지 않는다.
3. Entitlement/Billing은 eventId로 멱등하게 `VerifiedPhoneBenefitBinding`을 저장한다. 이 단계에서는 TrialClaim이나 사용권을 지급하지 않는다.
4. 첫 무료시험 요청에서 binding candidate 전체로 기존 TrialClaim을 조회하고, 없을 때만 TrialClaim과 UserEntitlement를 원자적으로 생성한다.
5. outbox 지연 중 첫 시험 요청은 중복 지급을 우회하지 않고 일시적인 eligibility processing 응답으로 재시도한다.

consumer-scoped proof는 pseudonymous data로 분류하고 delivery·재처리에 필요한 최소 기간만 보존한다. benefit key를 Identity와 Entitlement 중 어디서 소유하고 proof를 어떻게 서명할지는 서버 간 ADR에서 정하되, 두 서비스가 PhoneIdentity fingerprint나 raw phone을 공유하는 방식은 허용하지 않는다.

Identity 구현은 `FREE_MOCK_EXAM` 같은 Billing enum이나 시험 도메인 객체에 의존하지 않는다. ADR에서 allowlist한 opaque `consumerScopeId`와 key reference를 설정으로 받아 generic `PhoneEligibilityBindingOutbox`를 만들고, scope의 제품 의미·TrialClaim mapping은 Entitlement/Billing이 소유한다.

이 binding 기반은 단계 번호와 무관하게 신규 가입 production 활성화의 선행조건이다. 단계 5 전에 outbox schema·key ownership·수신 멱등성과 보존 계약이 준비되지 않으면 signup finalize를 켜지 않는다. 그렇지 않으면 이미 가입한 User의 benefit fingerprint를 raw phone 없이 복구할 수 없어 전화번호 재인증이 필요해진다.

권장 상태:

```text
VerifiedPhoneBenefitBinding
- userId
- benefitType
- fingerprintCandidates[]
- sourceEventId
- verifiedAt

TrialClaim
- benefitType
- fingerprintKeyVersion
- benefitPhoneFingerprint
- claimedByUserId
- claimedAt

UserEntitlement
- userId
- type
- grantedQuantity
- reservedQuantity
- usedQuantity

EntitlementReservation
- reservationId
- status: RESERVED / CONFIRMED / CANCELLED / RECONCILIATION_REQUIRED
```

시험 시작은 `reserve → exam 생성 → confirm`으로 처리한다. 생성 실패는 cancel하고 결과 불명은 reservationId로 exam 존재를 대조한다.

Identity 저장소에는 위 entity나 시험 코드를 추가하지 않는다.

## 12. 탈퇴·unlink·revocation lifecycle

회원탈퇴는 Identity와 Firebase의 분산 작업이다.

Identity가 소유하는 작업:

- User를 WITHDRAWN tombstone으로 전환
- FirebaseIdentity·SocialIdentity·PhoneIdentity의 삭제·보존 정책 적용
- 모든 내부 RefreshSession 폐기
- 필요한 User lifecycle outbox 저장

Firebase 작업:

- Firebase refresh token revoke
- Firebase User disable 또는 delete
- Apple provider revoke 요구사항 처리

기본 순서는 내부 탈퇴를 무기한 막지 않으면서 외부 revoke/delete를 retry 가능하게 한다. exact 순서는 ADR에서 정한다.

- Firebase 작업 성공 뒤 내부 commit
- 내부 commit 뒤 lifecycle outbox로 Firebase 작업 retry
- saga 상태 entity

어느 방식을 선택해도 partially withdrawn 상태를 관측·재처리할 수 있어야 한다.

인증수단 unlink:

- 마지막 허용 로그인 수단 제거 금지
- Firebase current user의 provider link와 Identity SocialIdentity를 일관되게 변경
- 다른 User owner 충돌 시 자동 merge 금지
- phone 변경은 별도 recent-auth·verified replacement Transaction

Apple:

- Firebase/Apple account deletion revoke API와 authorization code 요구사항을 PoC한다.
- 필요한 code를 저장해야 한다면 SocialIdentity가 아닌 별도 암호화·최소 보존 경계에 둔다.
- revoke 실패를 Provider 원문 message 없이 retry한다.

## 13. 오류 계약

| 상황 | HTTP | 외부 의미 |
| --- | --- | --- |
| Firebase ID Token 누락·형식 오류 | 401 | 인증 실패 |
| Firebase signature·issuer·audience·expiry 실패 | 401 | 인증 실패 |
| Firebase user disabled·revoked | 401 | 재인증 필요 |
| 허용하지 않은 sign-in provider | 403 | 지원하지 않는 로그인 방식 |
| phone-only login | 403 | 전화번호 로그인 미지원 |
| 내부 User 없음 | 200 상태 결과 | `ENROLLMENT_REQUIRED` |
| 가입 phone proof 없음 | 400 | `PHONE_VERIFICATION_REQUIRED` |
| Firebase UID와 enrollment 불일치 | 409 | enrollment conflict |
| Firebase UID가 다른 User 소유 | 409 | `FIREBASE_IDENTITY_CONFLICT` |
| provider subject가 다른 User 소유 | 409 | `SOCIAL_IDENTITY_CONFLICT` |
| Firebase phone link 또는 phone alias가 다른 User 소유 | 409 | `PHONE_ALREADY_LINKED` |
| 필수 동의 누락·version 불일치 | 기존 400 | 기존 동의 오류 유지 |
| Guest가 기존 MEMBER identity 발견 | 409 | `MERGE_REQUIRED` |
| Guest JWT 또는 target Firebase proof 누락·불일치 | 401 | 인증 실패 |
| MERGED source JWT | 401 또는 기존 정책 | `ACCOUNT_MERGED_TOKEN_REJECTED` |
| enrollment 만료·소비됨 | 409 | 새 인증 시도 필요 |
| Firebase 일시 장애 | 503 | 안전한 외부 인증 일시 장애 |
| Firebase quota/rate limit | 429 | 잠시 후 재시도 |
| 내부 User 비활성 | 기존 401/403 | 기존 상태 오류 유지 |
| phone 없는 MEMBER의 무료시험 | 409 | `PHONE_ONBOARDING_REQUIRED` |

Firebase·Google·Apple·Kakao의 원문 오류, project 정보, Token Claim과 사용자 개인정보를 응답에 포함하지 않는다.

## 14. 관측성과 민감정보

Application logs에서 허용할 수 있는 값:

- event
- outcome
- errorCode
- 낮은 cardinality provider category
- 필요한 경우 internal canonical userId
- 처리 건수와 attempt 상태

금지 값:

- Firebase ID Token·refresh token·custom token
- Firebase UID
- Firebase project credential
- provider credential·authorization code
- provider subject·email·phone
- OTP code
- PhoneIdentity fingerprint·last4
- enrollmentId 전체값
- 내부 Access/Refresh Token과 hash

Sentry에는 userId를 전송하지 않는다. Metrics에는 userId·Firebase UID·attempt ID를 label/tag로 넣지 않는다.

예상된 4xx는 application ERROR나 Sentry issue로 수집하지 않는다. Firebase 일시 장애의 진단 로그도 SDK 원문 오류를 그대로 남기지 않고 provider category·고정 error code만 사용한다.

## 15. 단계별 구현 순서

단계 번호는 의존성 기준이며 Entitlement 트랙은 별도 저장소에서 병렬 진행한다.

### 단계 0. Firebase ADR과 PoC — 로컬 검증 완료, 외부 gate 진행 전

확정할 항목:

- Firebase credential broker + Identity account/session owner 책임 경계
- Firebase Admin SDK runtime credential 주입
- email/password·Google·Apple Firebase flow
- email/Google/Apple/Kakao로 인증한 현재 Firebase User에 phone credential을 link하고 link 전후 UID가 유지되는 흐름
- enrollment에서 phone credential 직접 sign-in과 별도 Firebase User 생성을 금지하는 client 계약
- 이미 다른 Firebase User가 소유한 번호의 link 충돌, abandoned enrollment의 phone unlink/delete와 번호 점유 해제
- 검증 번호당 ACTIVE MEMBER 1개, 충돌 시 `PHONE_ALREADY_LINKED`, phone만으로 자동 merge하지 않는 제품 정책
- phone-only login 거절 가능성
- Firebase Authentication의 Identity Platform 업그레이드·billing·비용 영향
- Kakao Generic OIDC provider 등록, issuer·discovery·redirect·providerData stable UID·Android/iOS deep-link 동작
- Kakao PoC 실패 시 feature disable과 별도 ADR 기준
- Firebase ID Token Claim·revocation·disabled user와 목적별 recent-auth 검증
- 일반 Learning Core 요청·Identity RefreshSession reissue에는 Firebase 호출을 추가하지 않는 계약
- 반복 exchange의 동일 active enrollment 재사용과 binding별 PENDING attempt 하나 보장
- 다른 기기 Guest JWT와 기존 SNS 소유 Firebase User ID Token을 함께 제시하는 merge proof
- Firebase user delete·unlink·Apple revoke
- 가입 중단 Firebase user resume·cleanup
- 실제 운영 User 0건 확인과 legacy password route cutover
- Firebase quota·비용·국내 SMS 도달률·허용 국가

PoC는 격리 Firebase project 또는 emulator를 사용하고 실제 credential을 저장소에 기록하지 않는다. 결과를 ADR로 승인하기 전 production feature를 활성화하지 않는다.

Jira `TMI-90`의 현재 결과는 `docs/adr/ADR-001-firebase-authentication-broker.md`와 `docs/poc/firebase-auth-broker-stage-0.md`에 기록한다. Auth Emulator에서 email/password, Google·Apple provider mapping, Admin Token 검증·disable·revoke·delete, phone link UID 유지·phone-only User·번호 충돌·owner cleanup을 확인했다. 반복 enrollment와 Guest merge dual proof는 production entity를 만들지 않는 PoC contract test로 고정했다.

Kakao 공개 discovery는 authorization code·PKCE S256·RS256·pairwise subject 계약을 제공하지만 Identity Platform 등록 가능성, billing, stable providerData UID와 모바일 deep-link는 증명하지 못했다. 따라서 Kakao production 결정은 계속 `NO-GO`이고 feature flag를 끈다.

Stage 3·4의 disabled-by-default foundation 구현은 조건부 ADR을 기준으로 진행할 수 있다. 다만 실제 Firebase login/signup을 공개하는 단계 5와 Provider flag 활성화는 ADR의 production gate가 모두 통과한 뒤에만 허용한다.

### 단계 1. SocialIdentity 기반 — 완료

- `SocialProvider(GOOGLE, KAKAO, APPLE)`
- SocialIdentity·provider+subject unique·userId index
- Repository와 격리 Mongo index 테스트
- Jira `TMI-88`, PR #16

### 단계 2. UserAccountType 호환 expand — 완료

- `UserAccountType(GUEST, MEMBER)`
- 신규 User dual write
- legacy provider fallback
- profile accountType·deprecated provider
- Guest·LOCAL credential withdrawal 분리
- Jira `TMI-89`, PR #17

### 단계 3. Firebase broker foundation

- Firebase Admin SDK dependency와 fail-closed configuration
- `FirebaseAuthenticationVerifier` adapter
- `VerifiedFirebasePrincipal`
- 목적별 Token 검증·revoke·recent-auth와 remote-call 정책
- `FirebaseIdentity` entity·Repository·`(firebaseProjectId, firebaseUid)` unique index
- `FirebaseEnrollmentAttempt`·TTL·CAS repository와 binding별 PENDING partial unique index
- 반복 exchange의 유효 attempt 재사용과 동시 insert duplicate 재조회
- providerData → SocialProvider mapping
- feature flags
- 외부 호출 없는 mock 기반 테스트
- opt-in emulator/격리 project smoke test

공개 signup·Guest upgrade는 아직 열지 않는다.

### 단계 4. PhoneIdentity와 versioned fingerprint

- E.164 검증
- ACTIVE_WRITE/LOOKUP_ONLY HMAC key registry
- `PhoneIdentity`
- `PhoneFingerprintAlias`
- verified phone당 ACTIVE MEMBER 하나를 보장하는 global unique index와 retained version candidate
- `PHONE_ALREADY_LINKED` 거절과 자동 merge 금지
- mixed writer gate
- raw phone·fingerprint 비로그 테스트

Firebase SMS API, 회원가입과 TrialClaim은 이 단계에 넣지 않는다.

### 단계 5. Firebase login exchange와 신규 signup

- `/auth/firebase/exchange`
- email/password·Google·Apple 로그인
- ENROLLMENT_REQUIRED
- 같은 Firebase UID의 phone credential link와 fresh Token·Admin UserRecord proof 확인
- phone credential 직접 sign-in·phone-only exchange 거절
- User + FirebaseIdentity + PhoneIdentity + SocialIdentity
  + PhoneEligibilityBindingOutbox + RefreshSession
  + attempt consume Transaction
- 기존 Password signup/login/check-email cutover
- 가입 중단 resume 정책
- 반복 exchange의 동일 active enrollment 재사용
- 동일 UID·provider subject·phone 동시 가입 분류

Kakao는 feature flag off로 시작할 수 있다.

### 단계 6. Guest 승격과 MEMBER auth-method sync

- Guest-bound FirebaseEnrollmentAttempt
- 기존 Guest userId 유지 MEMBER 승격 Transaction
- 기존 Guest Session 폐기
- PhoneEligibilityBindingOutbox를 승격 Transaction에 함께 저장
- MEMBER의 Firebase provider linking 후 SocialIdentity sync
- phone 변경 별도 경계
- 기존 owner 발견 시 mutation 없는 MERGE_REQUIRED
- 다른 기기 Guest JWT + 기존 owner Firebase ID Token의 이중 소유권 proof
- Firebase credential ownership 충돌 시 기존 owner sign-in으로 전환하는 client 복구 계약

### 단계 7. Guest merge·outbox·source token gate

- source/target 불변식
- `MERGED`, mergedInto fields
- source Session 폐기
- UserMergedOutbox lease·retry publisher
- Learning Core idempotent consumer
- source JWT deny marker
- 모든 참여 서비스 준비 후 merge feature flag

### 단계 8. Kakao·Apple lifecycle

- Kakao Generic OIDC PoC 통과 구성과 provider mapping
- Kakao conflict·link·login 테스트
- Apple authorization/revoke/delete lifecycle
- Firebase unlink/delete와 Identity lifecycle 일관성
- provider별 kill switch

### 단계 9. Entitlement 계약 — 별도 서비스

- PhoneIdentity와 별도 Secret·domain을 쓰는 benefit-scoped fingerprint candidate/proof
- 가입·phone 교체 시 consumer-scoped eligibility binding outbox와 멱등 `VerifiedPhoneBenefitBinding`
- TrialClaim unique
- UserEntitlement idempotent grant
- 탈퇴 보존·key rotation

이 단계의 binding schema·consumer·key 계약 부분은 별도 서비스에서 병렬 선행하며 단계 5 production signup의 배포 gate다. TrialClaim·UserEntitlement 지급 로직은 이후에 활성화할 수 있다.

### 단계 10. 무료시험 reserve/confirm — 별도 서비스

- MEMBER·PhoneIdentity gate
- silent claim
- reserve → exam → confirm
- cancel·reconciliation
- merge entitlement 이전

### 단계 11. 운영 안정화

- Firebase 장애·quota·cost alert
- enrollment cleanup
- Firebase/Internal lifecycle reconciliation
- HMAC emergency rotation
- outbox dead-letter 운영
- rate limit·abuse dashboard
- 개인정보 보존·삭제 audit

## 16. 예상 변경 파일 묶음

실제 package 이름은 현재 구조를 따른다.

```text
domain/auth/domain/entity/FirebaseIdentity.java
domain/auth/domain/entity/FirebaseEnrollmentAttempt.java
domain/auth/domain/repository/FirebaseIdentityRepository.java
domain/auth/domain/repository/FirebaseEnrollmentAttemptRepository.java

domain/auth/firebase/FirebaseAuthenticationVerifier.java
domain/auth/firebase/FirebaseAdminAuthenticationVerifier.java
domain/auth/firebase/VerifiedFirebasePrincipal.java
domain/auth/firebase/FirebaseVerificationPurpose.java
domain/auth/firebase/FirebaseProviderMapper.java

domain/auth/application/FirebaseExchangeService.java
domain/auth/application/FirebaseSignupService.java
domain/auth/application/FirebaseGuestEnrollmentService.java
domain/auth/application/FirebaseAuthMethodSyncService.java

domain/auth/api/FirebaseAuthController.java
domain/auth/dto/request/FirebaseTokenExchangeRequest.java
domain/auth/dto/request/FirebaseSignupRequest.java
domain/auth/dto/response/FirebaseExchangeResponse.java

domain/user/domain/entity/PhoneIdentity.java
domain/user/domain/entity/PhoneFingerprintAlias.java
domain/user/domain/entity/PhoneEligibilityBindingOutbox.java
domain/user/domain/repository/PhoneIdentityRepository.java
domain/user/domain/repository/PhoneFingerprintAliasRepository.java
domain/user/domain/repository/PhoneEligibilityBindingOutboxRepository.java

global/config/FirebaseAuthProperties.java
global/config/FirebaseAuthConfiguration.java
global/security/phone/PhoneNumberNormalizer.java
global/security/phone/PhoneFingerprintService.java
global/security/phone/PhoneFingerprintKeyRegistry.java
```

기존 직접 Provider 계획에서 다음 타입은 기본 구현 대상에서 제거한다.

```text
SocialTokenVerifier
GoogleTokenVerifier
KakaoTokenVerifier
AppleTokenVerifier
SocialLoginChallenge
PhoneVerificationAttempt
Identity 자체 OTP 발송·확인 Controller
```

Kakao Custom Token bridge가 별도 ADR로 승인될 때만 provider-specific verifier를 제한적으로 다시 추가한다.

## 17. 테스트 계획

### 17.1 Firebase adapter

- 잘못된 signature·issuer·audience·tenant 거절
- 허용된 Firebase project namespace만 principal로 변환
- expired·future issued token 거절
- disabled·revoked user 거절
- 허용되지 않은 sign-in provider 거절
- phone-only login 거절
- purpose별 recent-auth 검증
- login/signup/upgrade/merge/link/unlink/withdrawal의 revoke 검사와 remote failure fail-closed
- Identity RefreshSession reissue와 Learning Core 일반 요청에서 Firebase verifier 비호출
- providerData allowlist와 stable UID mapping
- raw Claim·Token·Firebase SDK object가 application 결과로 새지 않음
- Firebase 원문 오류가 외부 응답·로그에 노출되지 않음

기본 테스트는 adapter mock을 사용하며 실제 Firebase endpoint를 호출하지 않는다.

### 17.2 FirebaseIdentity·enrollment

- `(firebaseProjectId, firebaseUid)` unique
- userId unique
- 같은 User에 provider 여러 개 연결
- DIRECT_SIGNUP·GUEST_USER binding
- 만료·소비된 attempt 거절
- 동시 consume 한 요청만 성공
- 같은 binding의 반복 exchange가 동일한 유효 enrollmentId를 반환
- binding별 PENDING partial unique index가 동시 insert를 하나로 제한
- application 만료 CAS 뒤에만 새 attempt 생성
- 유효 attempt 재조회로 TTL이 연장되지 않음
- Firebase UID mismatch 거절
- cleanup TTL index와 application expiry 분리

### 17.3 PhoneIdentity

- Firebase verified phone만 허용
- E.164 재검증
- HMAC domain separation
- ACTIVE_WRITE와 LOOKUP_ONLY aliases 생성
- version 교차 같은 phone duplicate 차단
- 같은 verified phone의 두 번째 ACTIVE MEMBER 생성 차단
- User당 PhoneIdentity 한 개
- phone 충돌이 자동 User merge로 이어지지 않음
- withdrawal cleanup 전후 Firebase link·PhoneIdentity alias 번호 점유 정책
- raw phone·last4·fingerprint 비로그

### 17.4 신규 signup

- User·FirebaseIdentity·PhoneIdentity·aliases·PhoneEligibilityBindingOutbox·SocialIdentity·RefreshSession·attempt consume 성공
- phone proof·동의 누락 실패
- phone link 전후 Firebase UID 불일치 거절
- phone credential 직접 sign-in·phone-only exchange 거절
- 다른 Firebase User 소유 phone link 충돌 시 내부 User 미생성
- FirebaseIdentity unique 충돌 전체 rollback
- SocialIdentity unique 충돌 전체 rollback
- phone alias unique 충돌 전체 rollback
- PhoneEligibilityBindingOutbox 저장 실패 전체 rollback
- RefreshSession 저장 실패 전체 rollback
- duplicate finalize 멱등 또는 고정 conflict
- Access Token `sub`가 canonical UUID
- Firebase UID가 JWT Claim에 없음

### 17.5 Guest 승격·merge

- 미연결 UID로 기존 Guest userId 유지 승격
- Guest old Session 폐기
- 기존 owner 발견 시 단계 6에서 MERGE_REQUIRED와 mutation 없음
- 다른 기기 Guest JWT와 기존 owner Firebase ID Token을 모두 검증
- credential ownership 충돌 뒤 기존 owner sign-in Token으로 target 확인
- Guest JWT 또는 Firebase proof 하나라도 없거나 binding이 다르면 merge 거절
- source GUEST·target MEMBER 외 자동 merge 거절
- MEMBER→MEMBER 자동 merge 거절
- outbox save 실패 시 merge rollback
- source JWT를 target actor로 해석하지 않음

### 17.6 API·Security·OpenAPI

- Firebase exchange/signup 공개 route만 허용
- Guest upgrade·auth-method sync는 Identity Bearer 필수
- `firebaseIdToken`은 OpenAPI `writeOnly`
- Request `toString()` redaction
- 외부 Body에 userId 없음
- legacy password route cutover 상태
- expected 4xx가 Sentry issue가 되지 않음
- Token·UID·email·phone·subject·fingerprint가 log에 없음

### 17.7 혜택 fingerprint 계약 — 별도 서비스

- 같은 E.164에서도 PhoneIdentity와 FREE_MOCK_EXAM fingerprint 값이 다름
- identity phone key와 benefit key·domain·version registry 분리
- raw phone과 PhoneIdentity fingerprint가 Entitlement payload에 없음
- 가입·phone 교체 binding outbox의 eventId 멱등 처리
- binding 저장만으로 TrialClaim·UserEntitlement가 생성되지 않음
- 첫 무료시험에서 retained candidate 전체 조회와 TrialClaim unique 적용
- outbox 미도착 상태가 무료체험 중복 방지 우회로 이어지지 않음

### 17.8 PoC·통합

기본 CI와 분리된 opt-in 검증:

- email/password 가입·로그인
- Google·Apple login
- 같은 Firebase UID에 phone link, link 전후 UID 동일성
- phone credential 직접 sign-in 금지와 phone-only Identity login 거절
- 다른 Firebase User가 가진 번호의 link 충돌과 orphan cleanup 후 재사용
- fresh Token에서 phone proof 확인
- Identity Platform 업그레이드·billing 조건과 Kakao Generic OIDC provider 등록
- Android/iOS redirect·deep-link 복귀와 Kakao providerData stable UID
- account link collision
- 다른 기기 Guest JWT + 기존 owner Firebase Token merge proof
- 반복 exchange active enrollment 재사용
- revoke·disabled·delete
- 가입 중단 resume·cleanup

### 17.9 최종 검증

```text
./gradlew clean test
```

실제 Atlas나 외부 Firebase·OAuth Provider를 기본 테스트에서 호출하지 않는다.

## 18. 배포와 rollback

1. Firebase ADR·PoC 결과, Identity Platform 업그레이드·billing, 비용·국내 SMS·Kakao 가능성을 승인한다.
2. 실제 운영 User가 없음을 read-only 확인한다.
3. Firebase 설정은 disabled 기본값으로 배포한다.
4. FirebaseIdentity·Enrollment·PhoneIdentity index를 writer 활성화 전에 생성·검증한다.
5. Firebase adapter를 dark launch하고 Token validation metric만 안전한 낮은 cardinality로 확인한다.
6. client Firebase SDK와 Identity exchange를 staging에서 검증한다.
7. email/password·Google·Apple을 provider별 flag로 순차 활성화한다.
8. 같은 Firebase UID phone link·가입 중단 cleanup과 검증 번호당 ACTIVE MEMBER 1개 정책을 staging에서 확인한다.
9. consumer-scoped binding outbox schema·key와 Entitlement 멱등 consumer를 준비한 뒤 signup finalize를 활성화한다.
10. legacy password signup/login/check-email route를 닫는다. dual writer를 허용하지 않는다.
11. Guest upgrade를 활성화한다.
12. Kakao는 PoC·staging 통과 뒤 별도 flag로 활성화한다.
13. merge는 Learning Core consumer와 source token gate 뒤 마지막에 활성화한다.

rollback 원칙:

- provider별 Firebase feature flag를 끌 수 있다.
- 신규 enrollment finalize를 중단해 새 내부 User 생성을 막는다.
- 이미 발급된 Identity Access/Refresh 계약은 유지한다.
- FirebaseIdentity·SocialIdentity·PhoneIdentity 데이터를 파괴적으로 되돌리지 않는다.
- legacy password writer를 제거한 뒤 Password fallback을 임의로 재활성화하지 않는다.
- Firebase 장애 시 기존 내부 RefreshSession reissue를 허용할지 kill switch 정책을 ADR로 정한다.

## 19. 전체 완료 정의

- Firebase는 credential 인증만 담당하고 Identity가 canonical account/session owner임이 코드와 계약에 반영됐다.
- Firebase UID와 canonical UUID userId가 분리되고 unique mapping이 보장된다.
- Firebase project namespace와 UID의 조합으로 broker identity가 유일하다.
- Firebase ID Token은 Identity에서 검증·교환되며 Learning Core에 전달되지 않는다.
- email/password·Google·Apple 인증이 Firebase를 통해 동작한다.
- Kakao는 승인된 Generic OIDC 또는 별도 ADR 방식만 사용한다.
- Kakao Generic OIDC 사용 시 Identity Platform·billing·모바일 redirect 계약이 검증됐다.
- phone credential은 먼저 인증된 같은 Firebase UID에 link되고 별도 phone Firebase User를 만들지 않는다.
- phone-only 로그인을 거절하면서 같은 Firebase UID의 verified phone을 가입 proof로 사용할 수 있다.
- 한 verified phone은 동시에 하나의 ACTIVE MEMBER에만 귀속되고 두 번째 가입은 `PHONE_ALREADY_LINKED`로 거절되며 자동 merge되지 않는다.
- 신규 MEMBER는 phone·동의 없이 생성되지 않는다.
- 같은 Firebase UID·binding의 유효 PENDING enrollment는 하나이고 반복 exchange가 이를 재사용한다.
- User·FirebaseIdentity·PhoneIdentity·PhoneEligibilityBindingOutbox·SocialIdentity·RefreshSession·attempt consume이 원자적으로 처리된다.
- Guest 최초 승격은 기존 userId를 유지한다.
- 기존 Firebase/Social identity owner가 canonical User 발견 기준이 된다.
- 여러 기기 Guest merge는 현재 Guest JWT와 기존 owner Firebase ID Token의 이중 proof를 검증한다.
- email·phone만으로 자동 merge하지 않는다.
- provider·phone·UID 동시 충돌에서 고아 내부 User·Session이 남지 않는다.
- 자체 JWT의 UUID sub·issuer·audience·RS256·kid·JWKS 계약이 유지된다.
- Firebase/Internal logout·withdrawal·revoke reconciliation이 정의됐다.
- Firebase login exchange와 high-risk lifecycle 경로는 목적별 revoke·recent-auth를 검사하고 Learning Core 일반 요청과 Identity reissue는 Firebase를 호출하지 않는다.
- TrialClaim·Entitlement·시험 코드는 Identity 저장소에 추가되지 않는다.
- PhoneIdentity fingerprint와 benefit fingerprint는 별도 key·domain으로 파생되고 consumer-scoped binding 외에는 교차 서비스에서 공유되지 않는다.
- Firebase Token·UID·Password·provider subject·email·phone·OTP·fingerprint가 로그·Sentry·metric에 노출되지 않는다.
- 전체 `./gradlew clean test`가 외부 인프라 없이 성공한다.

## 20. 다음 즉시 작업 범위

현재 작업은 Jira `TMI-90`의 단계 0 ADR·PoC다. local contract와 Auth Emulator, Kakao 공개 discovery 검증은 완료했고 production Firebase login은 아직 열지 않는다.

포함:

- Firebase project·Admin SDK 연결 방식 초안
- email/password·Google·Apple 인증 PoC
- 같은 Firebase UID phone credential link, UID 유지, 직접 phone sign-in 금지와 phone-only login 거절 PoC
- 다른 Firebase User 소유 번호 충돌과 중단 가입 phone unlink/delete PoC
- 검증 번호당 ACTIVE MEMBER 1개와 `PHONE_ALREADY_LINKED`·자동 merge 금지 정책 확인
- Identity Platform 업그레이드·billing과 Kakao Generic OIDC 등록·모바일 redirect 가능성 PoC
- Firebase ID Token·providerData와 목적별 revoke·disabled·recent-auth 검증
- 반복 exchange의 active enrollment 재사용·동시성 정책
- 다른 기기 Guest JWT와 기존 owner Firebase Token의 merge proof
- Firebase user와 내부 User lifecycle 표
- 가입 중단 resume·cleanup 정책
- 내부 JWT exchange 계약
- 비용·quota·국내 SMS 도달률 확인
- PhoneIdentity와 별도 benefit-scoped fingerprint 전달 ADR 초안

제외:

- production Secret 저장
- 공개 API 활성화
- 실제 User 생성 migration
- Guest merge
- TrialClaim·Entitlement·시험 구현
- Learning Core 코드 변경

조건부 ADR을 검토한 뒤 Stage 3 Firebase broker foundation과 Stage 4 PhoneIdentity Jira를 각각 생성할 수 있다. 실제 외부 인증을 공개하는 Stage 5는 격리 Firebase project·모바일·SMS·Identity Platform·Apple lifecycle gate 완료 뒤에만 진행한다.

## 21. 참고 자료

- [Firebase Authentication](https://firebase.google.com/docs/auth)
- [Firebase Admin ID Token 검증](https://firebase.google.com/docs/auth/admin/verify-id-tokens)
- [Firebase email/password 인증](https://firebase.google.com/docs/auth/web/password-auth)
- [Firebase 전화번호 인증](https://firebase.google.com/docs/auth/web/phone-auth)
- [Firebase 계정에 인증 Provider 연결](https://firebase.google.com/docs/auth/web/account-linking)
- [Firebase Admin Authentication 오류](https://firebase.google.com/docs/auth/admin/errors)
- [Firebase Admin Java Token 검증 API](https://firebase.google.com/docs/reference/admin/java/reference/com/google/firebase/auth/FirebaseAuth)
- [Firebase 세션·Token 관리](https://firebase.google.com/docs/auth/admin/manage-sessions)
- [Identity Platform OpenID Connect](https://cloud.google.com/identity-platform/docs/web/oidc)
- [Kakao Login OpenID Connect](https://developers.kakao.com/docs/latest/en/kakaologin/common)
- [Apple 계정 삭제와 Token revoke](https://developer.apple.com/documentation/technotes/tn3194-handling-account-deletions-and-revoking-tokens-for-sign-in-with-apple)
- [Identity–Learning Core JWT 계약](identity-learning-jwt.md)
