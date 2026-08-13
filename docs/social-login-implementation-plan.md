# 소셜 로그인·전화번호 인증·무료체험 전체 구현 계획서

## 1. 문서 목적

이 문서는 외부 SNS 계정과 Identity Service의 canonical User를 분리하는 데이터 모델부터 Google·Kakao·Apple 인증, Guest 승격, 여러 Guest의 canonical User 병합, 휴대전화 번호 소유 검증, 검증된 번호당 무료 모의고사 1회 지급과 Learning Core 연동까지의 구현 순서와 완료 조건을 정의한다.

현재 바로 구현할 첫 범위는 `SocialIdentity` 모델·인덱스·Repository다. 외부 Provider 검증, 소셜 로그인 API, 전화번호 인증, 무료체험과 시험 사용권을 이 첫 범위에 섞지 않고 각 단계가 독립적으로 테스트·배포될 수 있도록 나눈다.

## 2. 반드시 유지할 계약

- 실제 사용자 식별자는 Identity Service가 생성한 UUID 문자열 `userId`다.
- JWT `sub`에는 외부 provider subject가 아니라 canonical `userId`를 넣는다.
- 외부 요청 Body에 `userId`를 받지 않고, 클라이언트가 보낸 `userId`를 신뢰하지 않는다.
- Google·Kakao·Apple의 email은 로그인 식별자로 사용하지 않고 첫 `SocialIdentity` 모델에는 저장하지 않는다.
- 외부 계정의 식별 기준은 검증된 `(provider, providerSubject)`다.
- 전화번호는 로그인 계정 식별자나 자동 계정 병합 키로 사용하지 않는다.
- 무료 모의고사 시작은 `ACTIVE MEMBER`만 허용하고 `GUEST`에는 `MEMBERSHIP_REQUIRED`를 반환한다.
- 신규 `MEMBER` 생성·승격은 검증된 전화번호가 없으면 완료하지 않으며, 전화번호 인증은 무료시험 시작이 아니라 회원가입 과정에서 수행한다.
- 무료체험 정책은 사람당 1회가 아니라 검증된 휴대전화 번호당 1회로 정확히 표현한다.
- 전화번호 원문을 장기 저장·로그·metric·event payload에 남기지 않는다.
- 전화번호 fingerprint는 일반 hash가 아니라 서버 비밀키 기반의 domain-separated HMAC-SHA-256으로 만든다.
- Access Token은 기존 RS256·`kid`·issuer·`tosunsaeng-learning-core` audience 계약을 유지한다.
- Refresh Token 원문은 DB와 로그에 저장하지 않고 기존 Rotation 계약을 재사용한다.
- Provider 자격증명, subject, email, Access Token, Refresh Token을 로그에 남기지 않는다.
- Identity Service는 시험·결과 데이터를 직접 수정하지 않는다. Learning Core 이전은 서버 간 계약과 event로 요청한다.
- Identity Service는 전화번호 소유 검증과 `PhoneIdentity`를 소유한다. `TrialClaim`·`UserEntitlement`·`EntitlementReservation`과 시험 시작 reserve/confirm은 별도 Entitlement/Billing 및 Learning Core 경계가 소유한다.
- Python AI의 `user_id`는 계속 `examId`이며 실제 `userId`를 Python AI로 보내지 않는다.

## 3. 현재 구조와 해결해야 할 간극

### 3.1 재사용 가능한 현재 구조

- `User.userId`는 UUID 문자열이며 Mongo `_id`로 사용된다.
- `AccessTokenIssuer`는 입력된 `userId`를 JWT `sub`로 발급한다.
- `RefreshSessionIssuer`는 입력된 `userId` 기준으로 Opaque RefreshSession을 생성한다.
- Guest 생성과 회원 탈퇴에는 Mongo Transaction 경계가 이미 있다.
- Mongo 자동 인덱스 생성이 활성화돼 있다.
- 인증 성공 응답 변환과 민감 Token의 `toString()` redaction이 구현돼 있다.

### 3.2 소셜 로그인 전에 해결할 구조적 간극

현재 `UserProvider`는 `LOCAL`, `GUEST`를 가지며 다음 의미가 섞여 있다.

- User 생성 방식
- 현재 계정 유형
- 프로필의 provider 표시
- 회원 탈퇴 시 비밀번호 요구 여부

또한 현재 `User` 불변식은 `GUEST`가 아니면 email·normalizedEmail·passwordHash를 모두 요구한다. 이 구조로는 다음 상태를 안전하게 표현할 수 없다.

- 비밀번호가 없는 소셜 전용 회원
- Guest userId를 유지한 채 Google·Kakao·Apple 계정을 연결한 회원
- LOCAL 계정에 여러 SNS 계정을 추가 연결한 회원

따라서 `SocialIdentity` 첫 단계에서는 `UserProvider`를 변경하지 않지만, 실제 소셜 API를 열기 전 별도 단계에서 계정 유형과 로그인 수단을 분리해야 한다.

## 4. 목표 구조

```text
Google ID Token ─┐
Kakao ID Token ──┼─> Provider Token Verifier
Apple ID Token ──┘           │
                              v
                 VerifiedSocialPrincipal
                 (provider, subject, optional email)
                              │
                 ┌────────────┴────────────┐
                 v                         v
          SocialIdentity                 User
   외부 계정 → canonical userId       서비스 사용자 자체
                 │                         │
                 └────────────┬────────────┘
                              v
                  기존 Access/Refresh 발급기
                              │
                              v
                    JWT sub = canonical userId
```

전화번호 인증·소셜 identity·무료체험 데이터는 서로 분리하되, 회원가입과 무료시험 접근 정책에서 다음 순서로 조합한다.

```text
앱 첫 진입에서 SNS 로그인
      │ Provider credential 검증
      ├─ 기존 SocialIdentity 발견
      │      └─> Guest 생성 없이 canonical MEMBER 로그인
      │
      └─ 미연결 identity
             └─> direct social enrollment + phone verification
                 + canonical MEMBER + PhoneIdentity + SocialIdentity 생성

Guest 둘러보기
      │
      ├─ LOCAL 가입 전 phone verification
      │      └─> grant 소비 + canonical MEMBER + PhoneIdentity 생성
      │
      └─ 미연결 SNS로 소셜 MEMBER 가입
             └─> Guest-bound grant 소비
                 + 기존 Guest userId 유지
                 + PhoneIdentity + SocialIdentity 생성

Guest의 무료 모의고사 시작 요청
      └─> MEMBERSHIP_REQUIRED

ACTIVE MEMBER의 무료 모의고사 시작 요청
      │ 가입 때 만든 PhoneIdentity에서 benefit-scoped fingerprint/proof 파생
      v
Entitlement/Billing
      ├─ TrialClaim: 검증 번호당 재지급 차단
      └─ UserEntitlement: 지급 1, 사용 0
                     │
                     v
Learning Core ──> entitlement reserve ──> exam 생성 ──> confirm consume
```

SocialIdentity는 어느 User인지 결정하고, PhoneIdentity는 회원가입에서 완료한 번호 소유 검증을 나타내며, TrialClaim은 무료체험 중복을 막고, UserEntitlement는 실제 사용 가능 수량을 관리한다. 어느 역할도 다른 역할의 키로 자동 계정 병합을 수행하지 않는다.

### 4.1 SocialProvider

```java
public enum SocialProvider {
    GOOGLE,
    KAKAO,
    APPLE
}
```

`UserProvider`에 위 값을 추가하지 않는다. `SocialProvider`는 외부 identity namespace만 나타낸다.

### 4.2 SocialIdentity

Mongo collection은 `social_identities`를 사용한다.

| 필드 | 타입 | 규칙 |
| --- | --- | --- |
| `socialIdentityId` | UUID 문자열 | 서버가 생성하는 Mongo `_id` |
| `userId` | UUID 문자열 | canonical `User.userId`의 ID 참조 |
| `provider` | `SocialProvider` | `GOOGLE`, `KAKAO`, `APPLE` |
| `providerSubject` | `String` | 1~255자, null·blank 거부, case-sensitive opaque 값 |
| `createdAt` | `Instant` | 생성 시각 |

도메인 규칙은 다음과 같다.

- `SocialIdentity`가 `User` 객체나 `@DBRef`를 보유하지 않는다.
- `providerSubject`를 trim·소문자화하거나 email 규칙으로 정규화하지 않는다.
- 첫 모델은 OIDC subject의 일반 상한을 기준으로 `providerSubject` 최대 길이를 255자로 제한하고 256자 이상을 거절한다. Provider 공식 계약이 더 짧으면 verifier에서 추가 제한하며 상한 변경은 첫 PR 전에 Google·Kakao·Apple 계약과 Mongo compound index byte 크기를 재확인한다.
- `userId`와 `socialIdentityId`는 canonical UUID 문자열 형식을 검증한다.
- Provider email은 검증 결과에서 필요한 순간에만 사용하고 SocialIdentity에 저장하거나 email 조회 Repository를 만들지 않는다.
- 변경 가능한 필드가 없는 첫 모델에는 `updatedAt`을 두지 않는다. 추후 필요하면 의미가 분명한 `lastVerifiedAt` 등을 별도 요구사항으로 추가한다.
- raw Provider Token과 provider 전체 Claim은 저장하지 않는다.

### 4.3 MongoDB index

```text
uk_social_identities_provider_subject
keys: provider ASC, providerSubject ASC
unique: true
```

이 index가 다음 경쟁 상태의 최종 방어선이다.

```text
user_A → GOOGLE / google_subject_1
user_B → GOOGLE / google_subject_1   // 거절
```

추가 조회 index는 다음과 같다.

```text
ix_social_identities_user_id
keys: userId ASC
unique: false
```

이번 기본안에서는 `(userId, provider)` unique index를 두지 않는다. 한 User가 같은 provider의 여러 계정을 연결하지 못하게 할지는 제품 정책 확정 후 별도 제약으로 추가한다.

### 4.4 SocialIdentityRepository

```java
Optional<SocialIdentity> findByProviderAndProviderSubject(
        SocialProvider provider,
        String providerSubject
);

List<SocialIdentity> findAllByUserId(String userId);
```

애플리케이션의 중복 선조회는 사용자 친화적인 분기를 위한 것이며 Mongo unique index를 대체하지 않는다.

### 4.5 목표 User 계정 모델

실제 소셜 연결 API 전에 `UserProvider`의 혼합 의미를 다음처럼 분리한다.

```text
UserAccountType
- GUEST
- MEMBER

로그인 수단
- LOCAL: User의 email + passwordHash
- SOCIAL: SocialIdentity 0..N개
```

권장 User 불변식은 다음과 같다.

- ACTIVE GUEST는 `guestInstallationIdHash`를 가지며 LOCAL 자격증명은 없다.
- ACTIVE MEMBER는 `guestInstallationIdHash`를 제거한다.
- LOCAL 자격증명은 email·normalizedEmail·passwordHash가 모두 있거나 모두 없어야 한다.
- 소셜 전용 MEMBER는 LOCAL 자격증명이 없어도 된다.
- MEMBER가 최소 한 개 로그인 수단을 가진다는 규칙은 SocialIdentity가 별도 collection이므로 application/transaction 계층에서 보장한다.
- 병합된 source User는 신규 상태 `MERGED`와 `mergedIntoUserId`, `mergedAt`으로 재사용을 차단한다.

기존 데이터 이행은 다음 순서로 진행한다.

1. 운영 DB를 read-only aggregate해 `provider` null/GUEST/LOCAL별 건수, User status, LOCAL credential all-or-none 여부와 guestInstallationIdHash 존재 여부를 값 노출 없이 집계한다.
2. null provider의 ACTIVE 문서가 전부 MEMBER 불변식을 만족하는지 확인하고 anomaly 건수와 처리 목록을 별도 migration 기록으로 남긴다.
3. null 문서의 의미가 확인되지 않거나 GUEST 형태가 섞여 있으면 `null → MEMBER` fallback 배포를 중단하고 명시적 backfill 규칙을 먼저 만든다.
4. 검증 통과 후 `accountType`을 nullable 호환 필드로 추가한다.
5. 기존 `provider=GUEST`는 `GUEST`, 검증된 `provider=LOCAL` 또는 null은 `MEMBER`로 읽는다.
6. 신규 쓰기는 `accountType`을 명시한다.
7. 운영 문서를 status·credential 불변식에 맞춰 backfill한다.
8. 구버전 `provider` API 응답의 유지·폐기 시점을 클라이언트와 합의한다.
9. backfill과 호환 기간이 끝난 뒤 내부 `UserProvider` 의존성을 제거한다.

프로필 응답은 장기적으로 `provider` 단일 값 대신 `accountType`과 `linkedProviders`를 제공한다. 기존 응답 제거가 호환성을 깨면 새 필드를 먼저 추가하고 `provider`를 deprecate한다.

## 5. Provider 인증 공통 설계

### 5.1 공통 검증 인터페이스

각 Provider SDK나 HTTP 호출을 application service에 직접 넣지 않는다.

```java
interface SocialTokenVerifier {
    SocialProvider provider();
    VerifiedSocialPrincipal verify(SocialVerificationRequest request);
}
```

`SocialVerificationRequest`는 하나의 느슨한 문자열 묶음으로 만들지 않는다. 공통 sealed interface 또는 provider별 immutable credential 객체를 사용해 각 Provider가 실제로 요구하는 입력만 표현한다.

```text
GoogleVerificationRequest
- identityToken
- 서버가 조회·검증한 expected nonce context

KakaoOidcVerificationRequest
- identityToken
- 서버가 조회·검증한 expected nonce context

AppleVerificationRequest
- identityToken
- optional single-use authorizationCode
- 서버가 조회·검증한 expected nonce context
```

Controller DTO의 provider 값과 내부 request subtype이 일치하지 않는 상태는 verifier 호출 전에 거절한다. Provider별 입력 차이는 request 객체가 흡수하고 검증 결과만 공통화한다.

검증 성공 결과는 필요한 최소 정보만 전달한다.

```text
VerifiedSocialPrincipal
- provider
- providerSubject
- optional email
- emailVerified 여부가 필요한 경우의 boolean
```

raw Provider 자격증명, 전체 Claim, Provider 응답 객체는 이 경계 밖으로 전달하지 않는다. optional email은 SocialIdentity에 저장하지 않고 해당 유스케이스가 끝나면 폐기한다.

### 5.2 공통 검증 항목

- 허용한 서명 알고리즘
- Provider 공식 issuer
- 환경에 맞는 client ID/audience
- `exp`, `iat` 등 시간 Claim
- 비어 있지 않은 `sub`
- 요청 흐름에서 사용한 nonce
- Provider별 JWKS Key rotation
- 네트워크 timeout과 일시 장애의 안전한 오류 변환

Provider가 반환한 오류 message나 Token 내용은 API 응답과 로그에 포함하지 않는다. 외부 인증 실패는 통합된 401 오류로 노출한다.

### 5.3 Google

- 검증된 Google ID Token의 `sub`를 `providerSubject`로 사용한다.
- email과 `email_verified`는 보조 정보로만 처리한다.
- audience는 허용된 앱 client ID 목록과 일치해야 한다.

### 5.4 Kakao

권장안은 Kakao OpenID Connect를 활성화하고 검증된 ID Token의 `sub`를 `providerSubject`로 사용하는 것이다.

Kakao 사용자 정보 API의 사용자 `id`를 선택해야 한다면 문자열로 변환해 저장하되, 같은 `KAKAO` namespace에서 OIDC `sub`와 사용자 `id`를 임의로 혼용하지 않는다. 구현 전에 canonical subject 원천을 하나로 고정한다.

email 제공이나 동의 여부는 로그인 식별과 분리한다.

### 5.5 Apple

- 검증된 Apple identity token의 `sub`를 `providerSubject`로 사용한다.
- Apple email은 최초 승인에서만 제공될 수 있으므로 필수값으로 두지 않는다.
- issuer·audience·nonce와 Key rotation을 검증한다.
- Apple single-use authorization code를 서버에서 교환할지 단계 0에서 확정한다.
- 계정 삭제 시 Apple authorization/token revocation이 필요하므로 발급·보관·폐기 lifecycle을 Apple 구현 전에 확정한다.
- revoke를 위해 Provider refresh credential을 보관해야 한다면 `SocialIdentity`에 넣지 않고 별도 암호화 저장소·최소 보존·접근통제를 적용한다.

### 5.6 설정 경계

Provider 활성화 여부, 허용 audience/client ID, issuer와 JWKS 위치는 환경별 설정으로 관리한다. 저장소에는 환경변수 이름과 안전한 기본값만 두며 실제 Secret이나 Provider 자격증명은 기록하지 않는다.

Provider별 feature flag를 두어 독립적으로 비활성화할 수 있게 한다.

```text
GOOGLE_SOCIAL_LOGIN_ENABLED
KAKAO_SOCIAL_LOGIN_ENABLED
APPLE_SOCIAL_LOGIN_ENABLED
```

### 5.7 SocialLoginChallenge와 nonce

`nonce`는 우리 서버가 SNS 인증 시작 전에 생성하는 고엔트로피 일회성 값이다. 클라이언트는 이 값을 Provider SDK·OIDC authorization request에 전달하고, Provider가 발급한 ID Token의 nonce Claim이 서버가 기대한 값과 일치하는지 검증한다. 이를 통해 다른 로그인 시도에서 발급된 ID Token의 재전송·바꿔치기를 막는다.

`socialChallengeId`는 nonce 자체나 OAuth PKCE `code_challenge`가 아니다. 서버가 보관한 예상 nonce·Provider·인증 목적·현재 User binding을 찾기 위한 opaque 인증 시도 식별자다.

```text
SocialLoginChallenge
- socialChallengeId: UUID 문자열
- provider
- purpose: LOGIN_OR_SIGNUP / LINK
- boundUserId: LINK일 때 현재 JWT sub, 그 외 null
- expectedNonceHash
- nonceMode: provider별 전달·비교 방식 version
- status: PENDING / CONSUMED / EXPIRED
- expiresAt
- cleanupAt
- createdAt
- consumedAt
```

권장 흐름은 다음과 같다.

```text
1. Client → Identity: social challenge 생성 요청
2. Identity: nonce와 socialChallengeId 생성, expectedNonceHash 저장
3. Identity → Client: socialChallengeId, providerNonce, expiresIn
4. Client → Provider SDK: providerNonce 전달
5. Provider → Client: ID Token 또는 provider credential 반환
6. Client → Identity: provider credential + socialChallengeId
7. Identity: challenge의 provider·purpose·User binding·TTL·status 확인
8. Identity: ID Token nonce를 expectedNonceHash 기준으로 검증
9. 성공 시 challenge를 일회성 소비하고 login 또는 enrollment 진행
```

- 로그인·직접 가입용 challenge 생성 API는 공개하되 provider·IP rate limit을 적용한다.
- SNS 추가 연결용 challenge는 Bearer 인증을 요구하고 현재 JWT `sub`에 묶는다.
- 최종 social login/link 요청 Body에서 클라이언트가 보낸 raw nonce를 기대값으로 신뢰하지 않는다.
- nonce 원문은 장기 저장하거나 로그·Sentry·metric에 넣지 않고 비교 가능한 hash만 짧은 TTL로 저장한다.
- Provider별 SDK가 raw 또는 변환된 nonce를 요구할 수 있으므로 adapter가 `nonceMode`에 따라 전달값과 Claim 비교 방식을 고정한다.
- Provider credential 검증 뒤 `status=PENDING`, `expiresAt>now`, provider·purpose·boundUserId 일치를 조건으로 `PENDING → CONSUMED`를 atomic `findAndModify`한다. CAS 전이에 성공한 요청 하나만 login 또는 enrollment로 진행한다.
- 같은 challenge를 두 요청이 동시에 검증해도 CAS loser는 `SOCIAL_CHALLENGE_INVALID`로 종료하고 Token·SocialEnrollmentAttempt를 만들지 않는다.
- challenge는 재사용·Provider 변경·LOGIN challenge의 LINK 전용 사용을 거절한다.
- 브라우저 authorization-code redirect를 사용한다면 CSRF 응답 상관관계용 OAuth `state`와 code 가로채기 방지용 PKCE도 별도로 적용한다. nonce·state·PKCE는 서로 대체하지 않는다.

### 5.8 SocialEnrollmentAttempt

처음부터 SNS 버튼으로 진입한 신규 사용자와 Guest의 SNS 회원 전환 모두 Provider credential을 먼저 검증한 뒤 전화 인증·동의 입력을 마칠 수 있어야 한다. Provider credential을 OTP 완료 때까지 클라이언트가 반복 제출하거나 서버가 원문으로 보관하지 않도록 짧은 수명의 enrollment attempt를 둔다.

```text
SocialEnrollmentAttempt
- enrollmentId: UUID 문자열
- bindingType: DIRECT_SIGNUP / USER
- bindingId: signupAttemptId 또는 현재 JWT sub
- provider
- providerSubject
- enrollmentGrantHash
- status: VERIFIED / CONSUMED / EXPIRED
- expiresAt
- cleanupAt
- createdAt
- consumedAt
```

- Provider credential·전체 Claim·email·authorization code 원문은 저장하지 않는다.
- `providerSubject`는 최종 SocialIdentity 생성에 필요한 값만 짧은 TTL 동안 보유하고 로그·event에 넣지 않는다.
- 클라이언트에는 추측 불가능한 opaque `socialEnrollmentGrant`를 한 번만 발급하고 DB에는 hash만 저장한다.
- DIRECT_SIGNUP grant는 서버가 만든 signupAttemptId에, USER grant는 현재 JWT `sub`에 묶어 서로 다른 흐름이나 User에서 재사용하지 못하게 한다.
- 전화 인증 attempt도 같은 signupAttemptId 또는 current User binding을 사용하고 최종 Transaction에서 두 grant의 binding 일치를 확인한다.
- 최종 저장 직전에 `(provider, providerSubject)` owner를 다시 조회하고 unique index를 최종 동시성 경계로 사용한다.
- 최종 signup/link Transaction은 `status=VERIFIED`, 미만료, grant hash·binding 일치를 조건으로 SocialEnrollmentAttempt를 conditional consume하고 update count가 정확히 1인지 확인한다.
- Mongo TTL 삭제 시점에 유효성을 의존하지 않고 application이 `expiresAt`과 status를 직접 검사한다.

## 6. API 설계안

API 이름과 응답 형태는 구현 전 클라이언트와 확정하되, 인증과 연결 책임은 분리한다.

### 6.1 앱 첫 진입 SNS 인증과 기존 identity 로그인

```text
POST /api/v1/auth/social/challenges
인증: 공개
입력: provider
출력: socialChallengeId, providerNonce, expiresIn

POST /api/v1/auth/social/login
인증: 공개
입력: provider, provider credential, socialChallengeId
출력:
- AUTHENTICATED: 기존 SocialIdentity의 canonical User 기준 AuthResponse
- SIGNUP_REQUIRED: signupAttemptId, socialEnrollmentGrant, expiresIn
```

처리 규칙:

1. socialChallengeId의 Provider·LOGIN_OR_SIGNUP purpose·TTL·미사용 상태를 확인한다.
2. challenge가 보유한 expected nonce로 Provider credential을 검증하고 challenge를 소비한다.
3. `(provider, providerSubject)`로 SocialIdentity를 조회한다.
4. identity가 있으면 연결된 User가 ACTIVE MEMBER인지 확인한다.
5. 기존 `AccessTokenIssuer`와 `RefreshSessionIssuer`로 Token을 발급한다.
6. JWT `sub`는 SocialIdentity의 canonical `userId`다.
7. identity가 없으면 User를 만들지 않고 DIRECT_SIGNUP SocialEnrollmentAttempt와 짧은 수명의 grant를 발급한다.

따라서 앱을 처음 연 사용자도 Guest 생성 없이 SNS 버튼을 누를 수 있다. 이미 가입된 SNS 계정이면 즉시 로그인하고, 처음 사용하는 SNS 계정이면 같은 UX 안에서 `SIGNUP_REQUIRED` 상태로 전화 인증·약관 동의 화면으로 전환한다. `SIGNUP_REQUIRED`는 인증 실패가 아니며 이 시점에는 User·SocialIdentity·PhoneIdentity를 생성하지 않는다.

### 6.2 현재 User에 소셜 계정 연결 또는 Guest 승격

```text
POST /api/v1/auth/social/link/challenges
인증: Identity Access Token 필수
입력: provider
출력: socialChallengeId, providerNonce, expiresIn

POST /api/v1/auth/social/link/prepare
인증: Identity Access Token 필수
입력: provider, provider credential, socialChallengeId
출력: socialEnrollmentGrant 또는 기존 owner 기반 login/merge 결과

POST /api/v1/auth/social/link
인증: Identity Access Token 필수
입력: socialEnrollmentGrant,
      phoneVerificationGrant(미연결 identity로 Guest를 신규 MEMBER 승격할 때만 필수)
```

Request Body에 `userId`를 넣지 않는다. 연결 대상은 검증된 현재 JWT `sub`에서만 얻는다.

처리 결과는 단계별로 다음과 같이 구분한다.

- `LINKED`: 미연결 identity를 현재 User에 연결
- `ALREADY_LINKED`: 같은 identity가 이미 현재 User를 가리킴
- `MERGE_REQUIRED`: 단계 7에서 identity가 다른 canonical User를 가리키는 Guest 요청을 감지했지만 병합은 수행하지 않음
- `MERGED_TO_CANONICAL`: 단계 8의 merge feature flag 활성화 뒤 identity가 가리키는 canonical User로 현재 Guest를 병합

prepare 단계에서 socialChallengeId가 현재 JWT `sub`와 LINK purpose에 묶였는지 확인하고 expected nonce로 Provider credential을 검증한 뒤 challenge를 소비한다. identity가 미연결일 때만 USER binding SocialEnrollmentAttempt를 만들고 원문 credential은 보관하지 않는다. 이미 owner가 있으면 enrollment attempt를 만들지 않고 현재 User와 owner 관계 및 merge feature flag에 따라 `ALREADY_LINKED`, `MERGE_REQUIRED`, `MERGED_TO_CANONICAL`로 분기한다. Guest가 미연결 identity를 처음 연결하면 다음을 한 Mongo Transaction으로 처리한다.

1. 현재 JWT `sub`에 함께 묶인 유효·미사용 socialEnrollmentGrant와 phoneVerificationGrant를 확인한다.
2. Guest의 `userId`를 유지한 채 MEMBER로 승격한다.
3. 같은 `userId`의 `PhoneIdentity`와 `SocialIdentity`를 생성한다.
4. 모든 저장이 성공한 경우에만 두 grant를 소비한다.

전화 인증 confirm만으로 Guest에 PhoneIdentity를 미리 만들지 않는다. 따라서 인증 후 가입을 중단한 Guest가 번호를 장기간 선점하지 않으며, 동시에 같은 번호로 가입하면 최종적으로 PhoneIdentity unique index를 획득한 한 MEMBER만 성공한다.

Provider credential이 이미 기존 SocialIdentity를 가리키면 신규 회원가입이 아니라 기존 계정 로그인·Guest 병합이다. 이때 source Guest에 새 전화 인증을 요구하지 않고 target ACTIVE MEMBER의 기존 PhoneIdentity를 사용한다. target이 PhoneIdentity가 없는 legacy MEMBER라면 병합·로그인 후 별도 1회 onboarding 정책을 적용한다.

단계 7에서는 위 상황을 감지만 하고 `MERGE_REQUIRED`를 반환하며 source User·Session·Learning Core 데이터와 target Token을 변경하지 않는다. 단계 8의 outbox consumer와 source JWT 공통 정책이 검증되고 merge feature flag가 켜진 뒤에만 실제 병합과 target Token 발급을 수행한다.

### 6.3 Guest를 거치지 않는 직접 소셜 회원가입

직접 소셜 회원가입은 첫 출시 범위에 포함한다. 6.1의 `SIGNUP_REQUIRED` 결과를 받은 클라이언트가 전화 인증을 완료한 뒤 다음 API를 호출한다.

```text
POST /api/v1/auth/social/signup/phone-verifications
인증: 공개
입력: socialEnrollmentGrant, phoneNumber
출력: verificationId, expiresIn, resendAfter

POST /api/v1/auth/social/signup/phone-verifications/{verificationId}/confirm
인증: 공개
입력: verificationCode
출력: phoneVerificationGrant, grantExpiresIn

POST /api/v1/auth/social/signup
인증: 공개
입력: socialEnrollmentGrant, phoneVerificationGrant,
      nickname, 개인정보 처리방침·이용약관 동의와 버전
출력: canonical User 기준 AuthResponse
```

최종 signup은 다음을 한 Mongo Transaction 경계에서 처리한다.

1. socialEnrollmentGrant와 phoneVerificationGrant가 유효·미사용이고 같은 signupAttemptId에 묶였는지 확인한다.
2. 최신 동의와 입력 profile을 검증한다.
3. 서버가 canonical UUID `userId`를 생성한다.
4. MEMBER User·PhoneIdentity·SocialIdentity·최초 RefreshSession을 저장한다.
5. 두 enrollment attempt를 소비한다.
6. 성공한 canonical userId로 기존 Access/Refresh 발급 계약을 사용해 로그인 완료 응답을 반환한다.

Provider email은 User 또는 SocialIdentity의 로그인 식별자로 자동 저장하지 않는다. 직접 소셜 회원의 email·profile 필드 정책은 User 계정 유형 분리 단계에서 nullable 또는 별도 profile 요구사항으로 확정한다.

동시에 같은 SNS identity로 직접 가입하면 `(provider, providerSubject)` unique index를 획득한 한 Transaction만 성공한다. 실패 요청은 User·PhoneIdentity·RefreshSession을 남기지 않고 `SOCIAL_IDENTITY_ALREADY_REGISTERED`로 로그인 재시도를 안내한다. 같은 전화번호 충돌도 자동 계정 병합하지 않고 `PHONE_ALREADY_LINKED`로 처리한다.

### 6.4 소셜 연결 해제

첫 출시 범위에서 제외한다. 후속 구현 시 마지막 로그인 수단을 제거하지 못하도록 LOCAL 자격증명과 남은 SocialIdentity 수를 같은 Transaction 경계에서 확인한다.

## 7. 핵심 유스케이스와 동시성 규칙

### 7.1 기존 소셜 계정 로그인

```text
검증된 GOOGLE / subject_A
        ↓ 조회
SocialIdentity → user_A
        ↓
JWT sub=user_A + user_A RefreshSession
```

User가 없거나 ACTIVE가 아니면 Token을 발급하지 않는다.

### 7.2 첫 진입 직접 소셜 회원가입

```text
비인증 사용자
GOOGLE / subject_A는 미연결
        ↓ Provider 검증
DIRECT_SIGNUP SocialEnrollmentAttempt
        ↓ 전화 인증 + 동의
User user_A(MEMBER) 생성
PhoneIdentity(phone_X, user_A) 생성
SocialIdentity(GOOGLE, subject_A, user_A) 생성
최초 RefreshSession 생성
        ↓
JWT sub=user_A
```

User·PhoneIdentity·SocialIdentity·RefreshSession과 두 grant 소비는 원자적으로 성공하거나 rollback돼야 한다. Provider credential 검증 성공만으로 User를 만들지 않으며 phone verification과 필수 동의가 모두 완료돼야 한다.

### 7.3 Guest 최초 연결

```text
현재 JWT sub=user_A(GUEST)
GOOGLE / subject_A는 미연결
        ↓ Provider 검증 + 전화 인증
USER-bound socialEnrollmentGrant + phoneVerificationGrant
        ↓ Transaction
user_A를 MEMBER로 승격
guestInstallationIdHash 제거
PhoneIdentity(phone_X, user_A) 생성
SocialIdentity(GOOGLE, subject_A, user_A) 생성
        ↓
canonical userId는 계속 user_A
```

User·PhoneIdentity·SocialIdentity 변경과 두 grant 소비는 Mongo Transaction으로 묶는다. conditional update로 ACTIVE GUEST와 예상 `updatedAt`을 확인해 동시 승격 충돌을 감지한다.

### 7.4 동일 identity 동시 연결

두 요청이 같은 `(provider, providerSubject)`를 동시에 연결하면 unique index에서 한 요청만 성공한다. 실패 요청은 Transaction 종료 후 SocialIdentity를 다시 조회한다.

- 같은 current user를 가리키면 멱등 성공
- 다른 MEMBER를 가리키면 연결 충돌
- 다른 canonical User를 가리키고 current user가 GUEST면 단계 7에서는 `MERGE_REQUIRED`, 단계 8 활성화 뒤 병합 흐름

`DuplicateKeyException`을 무조건 일반 409로 바꾸지 않고 저장 후 실제 owner를 재조회해 분류한다.

### 7.5 여러 기기 Guest 병합

```text
기기 1: user_A ← GOOGLE / subject_A
기기 2: user_B(GUEST)가 GOOGLE / subject_A로 연결 시도
                         ↓
기존 SocialIdentity owner user_A가 canonical
                         ↓
user_B → MERGED, mergedIntoUserId=user_A
user_B RefreshSession 전체 폐기
UserMerged outbox 생성
새 Token은 user_A 기준으로만 발급
```

canonical 선택 규칙은 다음과 같다.

- 이미 SocialIdentity를 소유한 User가 항상 canonical이다.
- SocialIdentity가 아직 없으면 현재 인증된 User가 owner가 된다.
- 클라이언트 입력이나 생성 시각만으로 canonical User를 선택하지 않는다.

자동 병합의 코드 수준 불변식은 다음과 같다.

- `sourceUserId`와 `targetUserId`는 서로 달라야 한다.
- source는 `ACTIVE GUEST`만 허용한다.
- target은 `ACTIVE MEMBER`만 허용한다.
- `SUSPENDED`, `WITHDRAWN`, `MERGED` User는 source 또는 target으로 직접 사용하지 않는다.
- MEMBER → MEMBER 자동 병합은 금지하고 별도 계정 복구·소유권 확인 흐름으로 분리한다.
- MERGED User가 입력되면 `mergedIntoUserId` chain을 먼저 canonical User까지 resolve한다.
- chain resolve에는 최대 깊이와 방문 집합을 적용해 순환 참조를 거부한다.
- target이 다시 source를 가리키는 merge나 이미 반대 방향으로 완료된 merge를 금지한다.

병합 Transaction은 source User 상태 전환, source RefreshSession 폐기, outbox 저장과 필요한 새 RefreshSession 저장을 함께 처리한다. 충돌 재시도 횟수는 제한하고 실패 시 일반 500이 아니라 고정된 merge conflict 오류로 변환한다.

### 7.6 LOCAL User의 소셜 계정 연결

ACTIVE MEMBER의 현재 JWT와 유효한 Provider credential을 모두 확인한다. 미연결 identity면 기존 LOCAL userId에 SocialIdentity를 추가하며 passwordHash는 유지한다. 따라서 한 User가 LOCAL + Google + Kakao + Apple 로그인 수단을 함께 가질 수 있다.

## 8. Guest 병합·서비스 공통 source token·Learning Core 계약

Identity Service는 Learning Core의 시험·결과 collection을 직접 수정하지 않는다. Identity DB Transaction 안에서 transactional outbox event를 저장하고, 별도 전달 경계가 Learning Core에 알린다.

권장 event 최소 필드는 다음과 같다.

```text
UserMerged
- eventId
- schemaVersion
- sourceUserId
- targetUserId
- occurredAt
```

event에 provider subject, email, Token이나 개인정보를 넣지 않는다.

outbox document는 event payload와 전달 상태를 함께 관리한다.

```text
UserMergedOutbox
- outboxId
- eventId
- eventType
- schemaVersion
- sourceUserId
- targetUserId
- occurredAt
- status: PENDING / PROCESSING / PUBLISHED / FAILED
- attemptCount
- nextAttemptAt
- leaseUntil
- createdAt
- publishedAt
```

publisher는 `PENDING` 또는 재시도 가능한 `FAILED` 문서를 `findAndModify`로 atomic claim해 `PROCESSING`과 lease를 설정한다. publish 성공 후 `PUBLISHED`, 실패 시 안전한 고정 error code·증가한 attemptCount·backoff가 반영된 `nextAttemptAt`으로 갱신한다. lease가 만료된 `PROCESSING` 문서는 재처리할 수 있어야 한다.

전달 보장은 at-least-once다. publish 성공 직후 process가 종료돼 같은 event가 다시 전달될 수 있으므로 Learning Core는 `eventId`를 영속적으로 기록해 멱등 처리한다. publisher log와 outbox에는 전체 broker 오류 message나 민감 payload를 남기지 않는다.

Learning Core는 다음을 책임진다.

- `eventId` 기준 멱등 처리
- sourceUserId 소유 시험·결과를 targetUserId로 이전 또는 alias 처리
- 일부 이전 실패 시 재처리 가능 상태 유지
- merge alias와 source Access Token 처리 정책

Identity와 Learning Core 사이에는 분산 Transaction이 없으므로 병합 완료 직후 데이터 이전은 eventual consistency다. 사용자에게 즉시 모든 기록이 보여야 한다면 merge 상태 조회 또는 완료 event를 포함한 별도 UX 계약이 필요하다.

기존 source Access Token은 source User를 `MERGED`로 바꿔도 만료 전까지 암호학적으로 유효하므로 다음을 merge 기능의 서비스 공통 선행 계약으로 고정한다.

- source JWT actor를 target userId로 해석하거나 target 권한으로 승격하지 않는다.
- Identity 보호 API는 source User가 `MERGED`임을 확인하면 즉시 `ACCOUNT_MERGED_TOKEN_REJECTED`로 거절한다.
- Learning Core를 포함해 userId 소유 데이터를 가진 서비스는 `UserMerged` consumer에서 source → target ownership migration과 source actor deny marker 저장을 같은 로컬 Transaction으로 처리한다.
- consumer 적용 전 source JWT는 기존 source 소유 Guest 데이터에만 접근할 수 있고 target 데이터에는 접근할 수 없다. consumer 적용 후에는 source JWT를 거절하고 target JWT만 허용한다.
- source userId write path는 deny marker를 확인해야 하며, migration과 경합한 write는 conditional update 또는 Transaction conflict로 실패·재시도해 migration 뒤 source에 새 데이터가 생기지 않게 한다.
- source → target alias는 ownership migration·중복 event 처리·조회 정합성에만 사용하고 actor authorization alias로 사용하지 않는다.
- source RefreshSession은 Identity merge Transaction에서 즉시 전부 폐기하고, 클라이언트는 성공 응답의 target Access/Refresh Token으로 전환한다.
- deny marker와 alias는 최소 `source Access Token 최대 TTL + clock skew`보다 길게 유지하고 실제 데이터 보존 정책과 함께 관리한다.
- 이 계약을 구현하지 못한 보호 서비스가 하나라도 있으면 merge feature flag를 활성화하지 않는다.

Identity merge 직후 다른 서비스 event 처리 전까지 target에서 source 기록이 보이지 않을 수는 있지만, source JWT가 target 권한을 얻는 보안상 승격은 허용하지 않는다. 사용자에게는 merge 진행 상태를 표시하고 consumer 완료 뒤 기록을 갱신한다.

서버 간 상세 계약은 별도 문서로 추가하고 Learning Core 저장소 작업은 별도 이슈와 PR로 수행한다.

## 9. PhoneIdentity와 OTP verification

### 9.1 정책 의미

전화번호 인증은 다음 두 목적에만 사용한다.

- 현재 요청자가 해당 휴대전화 번호를 소유·통제하고 있음을 확인
- 검증된 휴대전화 번호당 무료 모의고사 1회 정책의 입력 제공

전화번호는 다음 용도로 사용하지 않는다.

- 로그인 계정 식별자
- JWT subject
- SocialIdentity 대체 키
- 두 User의 자동 병합 근거

전화번호는 재할당·가족 공유·복수 SIM·가상번호가 가능하므로 정책 문구도 사람당 1회가 아니라 검증된 휴대전화 번호당 1회로 고정한다.

전화 인증 시점은 선택사항이 아니라 "Guest 진입이나 무료시험 시작이 아닌 MEMBER 회원가입에서 1회"로 확정한다.

```text
Guest 생성·둘러보기
→ 전화 인증 요구하지 않음

LOCAL 회원가입
→ 가입 전 OTP 검증
→ 일회성 phoneVerificationGrant와 함께 User+PhoneIdentity 생성

앱 첫 진입 → 미연결 Google/Kakao/Apple로 직접 회원가입
→ Provider 검증 후 socialEnrollmentGrant 발급
→ 가입 전 OTP 검증 후 phoneVerificationGrant 발급
→ 두 grant 소비 + User+PhoneIdentity+SocialIdentity 생성

앱 첫 진입 → 이미 연결된 Google/Kakao/Apple 로그인
→ 신규 회원가입이 아니므로 OTP 재요구 없음
→ 기존 canonical MEMBER로 즉시 로그인

Guest → Google/Kakao/Apple MEMBER 승격
→ 미연결 identity라면 OTP 검증 후 Guest-bound phoneVerificationGrant 발급
→ grant 소비 + PhoneIdentity 생성 + social link + MEMBER 승격을 한 Transaction으로 처리

Guest → 이미 연결된 Google/Kakao/Apple 계정 로그인
→ 신규 회원가입이 아니므로 OTP 재요구 없음
→ 기존 canonical MEMBER로 로그인 또는 Guest merge

기존 MEMBER 로그인
→ 매 로그인마다 OTP를 요구하지 않음

무료 모의고사 시작
→ Guest면 MEMBERSHIP_REQUIRED, 회원가입 화면으로 이동
→ ACTIVE MEMBER만 진행
→ 기존 PhoneIdentity로 TrialClaim을 서버에서 silent claim
→ OTP 화면 없이 entitlement reserve → exam 생성 → confirm
```

즉 Guest는 콘텐츠를 둘러볼 수 있지만 무료 모의고사를 시작할 수 없다. 신규 MEMBER가 되는 모든 경로는 유효한 전화 인증 grant를 소비해 PhoneIdentity를 함께 만들어야 하며, 회원가입이 끝나지 않은 Guest나 검증 attempt만 가진 Guest에는 entitlement를 발급하지 않는다. 기존 legacy MEMBER에 PhoneIdentity가 없으면 로그인 자체를 차단하기보다 다음 로그인 직후 1회 onboarding을 안내하고, 완료 전 무료시험 요청에는 `PHONE_ONBOARDING_REQUIRED`를 반환한다.

여기서 회원가입 시 인증은 매 로그인 시 재인증한다는 뜻이 아니다. PhoneIdentity가 유지되는 동안 LOCAL·Google·Kakao·Apple 로그인은 OTP 없이 처리하고, 번호 변경·계정 복구처럼 별도로 정한 고위험 작업에서만 recent verification을 다시 요구한다.

확정한 정책의 trade-off는 다음과 같다.

| 시점 | 장점 | 비용·위험 |
| --- | --- | --- |
| MEMBER enrollment | 시험 시작 UX가 끊기지 않고 회원 연락처·복구 기반을 일찍 확보 | 가입 전환율 저하, 미사용 회원의 SMS 비용과 개인정보 수집 증가 |
| 무료시험 직전 | 실제 혜택 사용자에게만 비용·개인정보 수집 | 회원가입 후에도 시험 시작 순간에 추가 이탈 가능 |

제품 정책은 가입 전환율·SMS 비용·개인정보 수집 증가를 감수하고 MEMBER enrollment 인증을 채택한다. 무료시험 직전에는 전화 인증 UI를 다시 띄우지 않는다. 전화번호 이용 목적은 무료체험 중복 방지와 향후 확정될 계정 복구·결제 보호 범위로 제한하고 사용자 고지·보존 정책에 반영한다.

전화 인증 성공과 TrialClaim 생성을 같은 시점으로 묶지 않는다. 가입했지만 무료시험을 사용하지 않은 번호를 불필요하게 장기 TrialClaim으로 남기지 않기 위해, TrialClaim은 실제 무료시험 요청에서 처음 생성한다.

### 9.2 전화번호 정규화와 fingerprint

클라이언트 입력을 그대로 신뢰하지 않고 서버에서 검증된 phone-number library를 사용해 E.164 형식으로 정규화한다. 국가·mobile line type 제한은 단계 0 정책에 따라 적용한다.

정규화된 번호 원문에는 일반 SHA-256을 사용하지 않는다. 번호 공간이 작아 offline 대입이 가능하므로 서버 Secret 기반 HMAC-SHA-256을 사용한다.

```text
phoneFingerprint = HMAC-SHA-256(
    keyFor(fingerprintKeyVersion),
    "phone-identity:v1" + separator + normalizedE164
)
```

- HMAC 입력에는 고정된 domain separator를 포함한다.
- HMAC key는 저장소 밖 Secret 관리 경계에 둔다.
- fingerprint에는 `fingerprintKeyVersion`을 함께 저장한다.
- key lifecycle은 `ACTIVE_WRITE → LOOKUP_ONLY → RETIRED`로 관리하고 한 시점에 `ACTIVE_WRITE`는 정확히 하나만 둔다.
- 전화번호 원문이 존재하는 OTP 검증 순간 `ACTIVE_WRITE`와 모든 `LOOKUP_ONLY` key로 version별 fingerprint candidate를 계산한다.
- `LOOKUP_ONLY` key는 해당 version을 참조하는 PhoneFingerprintAlias 또는 보존 중인 TrialClaim이 하나라도 있으면 삭제하지 않는다.
- 구버전·신버전 application이 서로 다른 key로 쓰는 mixed writer 배포를 금지하고 배포 설정에서 writer version 일치를 gate로 검증한다.
- raw 번호, HMAC key와 fingerprint를 로그·Sentry·metric에 넣지 않는다.
- Entitlement 도메인에 전달할 무료체험 키는 raw 번호 없이 나중에도 만들 수 있도록 저장된 version별 Identity fingerprint alias에서 다시 domain-separated HMAC으로 파생한다.

```text
benefitPhoneFingerprint = HMAC-SHA-256(
    keyFor(benefitFingerprintKeyVersion),
    "benefit:FREE_MOCK_EXAM:v1" + separator
        + phoneFingerprintKeyVersion + separator
        + phoneFingerprint
)
```

benefit fingerprint key도 동일하게 `ACTIVE_WRITE / LOOKUP_ONLY` lifecycle을 사용한다. Identity는 현재 PhoneIdentity에 저장된 모든 phone fingerprint alias와 모든 조회 가능 benefit key 조합으로 bounded candidate set을 만들고, Entitlement에는 benefit 전용 fingerprint와 composite version의 집합만 전달한다.

Entitlement는 claim 전에 candidate set 전체로 과거 TrialClaim을 조회하고 하나라도 일치하면 재지급을 거절한다. 신규 claim은 현재 `ACTIVE_WRITE` phone·benefit version 조합 하나로만 쓰며 mixed writer를 금지한다. 두 동시 요청이 모두 신규라면 같은 current fingerprint의 unique index가 최종 경쟁 경계가 된다.

raw 번호나 별도 강한 본인식별자가 없으므로 참조가 남은 legacy key를 임의로 폐기하면서 번호 동일성까지 보존하는 것은 불가능하다. 따라서 key 폐기는 다음 중 하나를 만족할 때만 허용한다.

- 해당 version을 참조하는 PhoneFingerprintAlias와 보존 중 TrialClaim이 모두 0건
- 승인된 alias/history migration으로 모든 참조가 다른 lookup 가능 version에 연결됨
- 법무·제품 승인을 받은 중복 방지 정책 reset 또는 더 강한 외부 본인식별자로 전환

key 유출 같은 비상 rotation에서도 이 제약을 숨기지 않고 별도 incident migration으로 처리한다. 단순히 legacy key 설정을 삭제해 무료체험을 다시 받을 수 있게 만들지 않는다.

### 9.3 PhoneIdentity

Identity Service의 `phone_identities` collection이 현재 확인된 번호 소유 관계를 관리한다.

```text
PhoneIdentity
- phoneIdentityId: UUID 문자열
- userId: canonical UUID 문자열
- fingerprintKeyVersion: 생성 당시 ACTIVE_WRITE version
- phoneFingerprint: 생성 당시 current fingerprint
- phoneLast4
- verifiedAt
- createdAt
```

version 교차 동일성은 별도 `phone_fingerprint_aliases` collection이 담당한다.

```text
PhoneFingerprintAlias
- aliasId: UUID 문자열
- phoneIdentityId: UUID 문자열
- fingerprintKeyVersion
- phoneFingerprint
- createdAt
```

권장 제약은 다음과 같다.

- PhoneFingerprintAlias의 `(fingerprintKeyVersion, phoneFingerprint)` unique: 각 lookup key version에서 한 검증 번호는 한 PhoneIdentity만 가리킴
- PhoneFingerprintAlias의 `phoneIdentityId` non-unique index: 현재 PhoneIdentity의 모든 version candidate 조회
- `userId` unique: 첫 정책에서는 한 User에 활성 PhoneIdentity 한 개만 허용
- `phoneLast4`는 UX 표시 전용이며 조회·중복·병합 키로 사용하지 않음
- User 객체나 `@DBRef` 없이 `userId`만 참조
- 번호 변경은 기존 문서의 fingerprint 덮어쓰기보다 검증된 교체 Transaction으로 처리
- 신규 데이터에서는 GUEST가 활성 PhoneIdentity를 보유하지 않으며, 회원가입 Transaction이 성공할 때 MEMBER 전환과 함께 생성

회원가입 Transaction은 PhoneIdentity와 함께 `ACTIVE_WRITE`·모든 `LOOKUP_ONLY` fingerprint alias를 원자적으로 저장한다. 기존 PhoneIdentity는 raw 번호 없이도 현재 저장된 version·fingerprint를 alias row로 backfill할 수 있다. 향후 번호가 다시 검증되면 그 순간 계산 가능한 새 version alias를 추가한다.

가입 시 어떤 alias unique 충돌이 발생해도 같은 실제 번호가 다른 active PhoneIdentity에 연결된 것으로 보고 전체 Transaction을 rollback한다. application 선조회는 UX 최적화일 뿐 version 교차 중복 방지의 최종 경계는 모든 retained version alias insert의 unique index다.

같은 phoneFingerprint가 다른 User를 가리키더라도 자동 병합하지 않는다. `PHONE_ALREADY_LINKED` 계열 충돌 또는 별도 번호 이전·계정 복구 정책으로 처리한다.

### 9.4 PhoneVerificationAttempt

OTP 요청과 확인 사이의 상태는 장기 PhoneIdentity와 분리한다.

```text
PhoneVerificationAttempt
- verificationId: UUID 문자열
- bindingType: SIGNUP_ATTEMPT / USER
- bindingId: 가입 시도 UUID 또는 현재 JWT sub
- purpose: MEMBER_ENROLLMENT / LEGACY_ONBOARDING / PHONE_CHANGE
- fingerprintKeyVersion
- phoneFingerprint
- phoneLast4
- providerReference: 외부 verification provider의 opaque 참조
- status: PENDING / VERIFIED / EXPIRED / LOCKED / CONSUMED
- verificationGrantHash: enrollment grant를 발급한 경우에만 저장
- failedAttemptCount
- resendAvailableAt
- verificationExpiresAt
- grantExpiresAt: enrollment grant를 발급한 경우에만 저장
- cleanupAt
- createdAt
- verifiedAt
- consumedAt
```

- `cleanupAt`에는 TTL index를 두되, Mongo TTL 삭제 시점에 유효성 판단을 의존하지 않는다. application이 `verificationExpiresAt`과 `grantExpiresAt`을 직접 검사한다.
- raw OTP code는 저장하지 않는다.
- 외부 provider가 raw 번호 재전달을 요구하지 않도록 providerReference 기반 확인을 우선한다.
- raw 번호의 임시 저장이 불가피하면 별도 암호화·짧은 TTL·terminal 상태 즉시 삭제를 적용하고 PhoneIdentity에는 복사하지 않는다.
- LOCAL pre-signup, DIRECT social signup과 Guest의 소셜 MEMBER enrollment는 인증 성공 후 짧은 TTL의 opaque phoneVerificationGrant를 한 번만 발급하고 DB에는 hash만 저장한다.
- DIRECT social signup의 PhoneVerificationAttempt는 SocialEnrollmentAttempt와 같은 signupAttemptId에 묶어 두 grant를 서로 다른 가입 시도에서 조합하지 못하게 한다.
- Guest `MEMBER_ENROLLMENT` attempt는 bindingId와 현재 JWT `sub` 일치를 확인하되 confirm 시 PhoneIdentity를 만들지 않는다. 최종 social signup Transaction에서 grant 소비·MEMBER 승격·PhoneIdentity·SocialIdentity 생성을 함께 처리한다.
- `LEGACY_ONBOARDING`과 `PHONE_CHANGE` USER 흐름은 현재 JWT `sub` 일치를 확인하고 각각 PhoneIdentity 생성·교체 Transaction이 성공할 때 attempt를 소비한다.
- 최종 회원가입 Transaction은 `status=VERIFIED`, `grantExpiresAt>now`, grant hash·binding 일치를 조건으로 PhoneVerificationAttempt를 conditional consume하고 update count가 정확히 1인지 확인한다.
- social·phone attempt 중 하나라도 conditional consume에 실패하면 User·PhoneIdentity·SocialIdentity·RefreshSession과 두 consume을 모두 rollback한다. 다른 저장 실패로 rollback되면 유효 기간 안에서 재시도할 수 있다.

### 9.5 API

LOCAL 회원가입 전 공개 verification 흐름은 별도 route로 분리한다. 직접 소셜 가입은 6.3의 공개 social signup verification route를 사용하며 socialEnrollmentGrant에서 같은 signupAttemptId binding을 얻는다.

```text
POST /api/v1/auth/signup/phone-verifications
인증: 공개
입력: phoneNumber
출력: verificationId, expiresIn, resendAfter

POST /api/v1/auth/signup/phone-verifications/{verificationId}/confirm
인증: 공개
입력: verificationCode
출력: phoneVerificationGrant, grantExpiresIn

POST /api/v1/auth/signup
입력 추가: phoneVerificationGrant
처리: grant 소비 + User + PhoneIdentity를 한 Mongo Transaction으로 생성
```

Guest 소셜 회원가입·legacy onboarding·번호 변경은 현재 User에 묶인 보호 route를 사용한다.

```text
POST /api/v1/auth/phone/verifications
인증: Identity Access Token 필수
입력: phoneNumber
출력: verificationId, expiresIn, resendAfter

POST /api/v1/auth/phone/verifications/{verificationId}/confirm
인증: Identity Access Token 필수
입력: verificationCode
출력:
- MEMBER_ENROLLMENT: phoneVerificationGrant, grantExpiresIn
- LEGACY_ONBOARDING / PHONE_CHANGE: verified
```

Guest의 social link 요청은 Provider credential 검증 결과가 미연결 identity일 때 같은 Guest에 묶인 `phoneVerificationGrant`를 필수로 소비한다. 기존 identity 로그인·Guest merge나 이미 PhoneIdentity를 가진 MEMBER의 추가 social link에는 grant를 요구하지 않는다.

confirm 요청에서 전화번호를 다시 받지 않는다. USER binding은 attempt의 bindingId와 현재 JWT `sub`가 일치해야 하며 어떤 Request Body에도 userId를 받지 않는다. phoneNumber·verificationCode·phoneVerificationGrant는 OpenAPI `writeOnly`이고 validation rejected value와 구조화 로그에 포함하지 않는다.

### 9.6 OTP abuse 방어

- phoneFingerprint별 재전송 cooldown과 시간당 한도
- canonical userId별 시간당 요청·실패 한도
- IP별 시간당 요청 한도
- verificationId별 실패 횟수 제한과 lock
- OTP TTL 3~5분 범위의 정책값
- OTP 확인에 성공한 provider verification session과 code의 즉시 재사용 차단
- LOCAL pre-signup, DIRECT social signup과 Guest MEMBER_ENROLLMENT attempt는 `VERIFIED` 상태에서 grant 소비를 기다리고, 각 회원가입 Transaction에서만 `CONSUMED` 전이
- LEGACY_ONBOARDING·PHONE_CHANGE USER attempt는 PhoneIdentity 생성·교체 Transaction이 성공할 때 `CONSUMED` 전이
- 허용 국가 allowlist
- mobile 허용, landline 차단, VoIP 차단 또는 추가 심사 정책
- provider Fraud Guard·rate limit·line type lookup의 선택적 활용
- 비용·성공률·차단률 alert와 비상 provider kill switch

기본 테스트에서는 실제 SMS provider를 호출하지 않고 adapter를 mock 처리한다.

## 10. TrialClaim·Entitlement·무료 모의고사 reserve/consume

### 10.1 서비스 소유권

Identity 저장소에 `freeTrialUsed`나 시험 사용권 로직을 추가하지 않는다.

```text
Identity
- PhoneIdentity
- OTP verification
- benefit-scoped phone fingerprint 또는 검증 증명 발급

Entitlement/Billing domain
- TrialClaim
- UserEntitlement
- 무료/유료 사용권 grant

Learning Core
- 시험 시작
- Entitlement reserve 후 exam 생성
- 생성 결과에 따라 confirm/cancel, 불명 결과는 reconciliation
```

Entitlement/Billing 서비스가 아직 없다면 별도 서비스·저장소 설계가 먼저이며, 임시 편의를 위해 Identity에 시험 혜택 코드를 넣지 않는다.

### 10.2 TrialClaim과 UserEntitlement의 역할 차이

둘은 같은 무료시험 지급 과정에서 만들어지지만 서로 다른 질문에 답한다.

| 구분 | `TrialClaim` | `UserEntitlement` |
| --- | --- | --- |
| 핵심 질문 | 이 검증 전화번호가 이 무료 혜택을 이미 받은 적이 있는가? | 이 canonical User가 지금 사용할 수 있는 권리가 몇 개 남았는가? |
| 역할 | 검증 번호당 1회 지급을 막는 전역 지급 이력·중복 방지 ledger | 특정 User에게 귀속된 사용권·잔액·소비 상태 |
| 핵심 식별 기준 | `benefitType + benefit-scoped phoneFingerprint` | `userId + entitlement type`과 grant source |
| 생성 시점 | 실제 무료혜택 최초 claim 시도 | TrialClaim 신규 생성 성공과 같은 Transaction에서 grant |
| 이후 변화 | 원칙적으로 지급 사실을 유지하고 시험 사용 여부로 변경하지 않음 | reserve·consume·보상에 따라 사용 가능 수량과 상태가 변함 |
| 탈퇴·재가입 | 승인된 보존 기간 동안 fingerprint를 유지해 같은 번호의 재지급 차단 | User lifecycle·merge 정책에 따라 이전·통합·종료 |
| 유료 확장 | 보통 무료체험·프로모션의 1회 수령 제한에만 사용 | 구매 시험권·구독 등 실제 이용 권리에도 공통 사용 가능 |

비유하면 TrialClaim은 "이 번호에 무료 쿠폰을 이미 발급했다"는 발급대장이고, UserEntitlement는 "user_A 지갑에 들어 있는 시험권 1장"이다.

- TrialClaim만 있으면 무료혜택을 지급했다는 사실은 알 수 있지만 그 권리가 미사용·예약·사용·보상 상태인지 관리할 수 없다.
- UserEntitlement만 있으면 현재 User의 사용권은 관리할 수 있지만 탈퇴 후 새 User를 만들었을 때 같은 전화번호에 무료권을 다시 지급하는 것을 막을 수 없다.
- TrialClaim은 시험 시작 권한을 직접 허용하지 않는다. Learning Core가 reserve/confirm하도록 요청하는 실제 권리는 UserEntitlement다.
- UserEntitlement가 모두 사용된 뒤에도 TrialClaim은 남아 같은 검증 번호의 무료권 재발급을 막는다.
- 유료 구매로 만든 UserEntitlement는 TrialClaim 없이 별도 결제 grant source를 가질 수 있다.

상태 변화 예시는 다음과 같다.

```text
회원가입 완료
PhoneIdentity만 존재
TrialClaim 없음
UserEntitlement 없음

첫 무료 모의고사 시작 요청
TrialClaim(phone_X, FREE_MOCK_EXAM) 신규 생성
+ UserEntitlement(user_A, FREE_MOCK_EXAM, granted=1, used=0) grant
  두 저장은 같은 Entitlement/Billing Transaction

시험 사용권 reserve
UserEntitlement.reserved: 0 → 1

exam 생성 후 confirm
UserEntitlement.reserved: 1 → 0
UserEntitlement.used: 0 → 1
TrialClaim: 그대로 유지

탈퇴 후 같은 번호로 새 user_B 가입
기존 TrialClaim 발견
→ 무료 UserEntitlement 재지급 거절
```

### 10.3 Identity와 Entitlement 사이 계약

클라이언트에 phone fingerprint를 반환하지 않는다. 회원가입·MEMBER 승격 시에는 PhoneIdentity만 만들고 Entitlement에 TrialClaim을 요청하지 않는다. 사용자가 실제 무료 모의고사를 시작할 때 Entitlement가 다음 중 확정된 서버 간 방식으로 Identity의 현재 PhoneIdentity를 검증하고 retained version의 benefit-scoped fingerprint candidate set을 받는다.

- Entitlement가 일회성 verification grant를 Identity 내부 API로 교환
- Identity가 audience·benefitType·canonical userId·candidate set·짧은 만료·일회성 ID에 묶인 서명된 proof를 발급

어떤 방식을 선택해도 Entitlement는 raw 전화번호나 HMAC key를 받지 않고 bounded benefit-scoped deterministic fingerprint candidate set과 canonical userId만 받는다. proof 또는 내부 claim request 재사용은 영속 idempotency key로 차단한다. 실제 시험 시작의 동기 UX가 필요하므로 전화 인증 시점의 비동기 event로 미리 TrialClaim을 만들지 않는다.

Identity는 이 교환에서 먼저 User가 ACTIVE MEMBER인지 확인하고 Guest에는 `MEMBERSHIP_REQUIRED`를 반환한다. 그다음 현재 PhoneIdentity를 확인하며, PhoneIdentity가 없는 legacy MEMBER에는 `PHONE_ONBOARDING_REQUIRED`를 반환한다. 클라이언트가 userId나 fingerprint를 중계·선택하지 않으며 검증된 JWT와 서버 간 인증 결과만 사용한다.

### 10.4 TrialClaim

```text
TrialClaim
- claimId
- benefitType: FREE_MOCK_EXAM
- fingerprintKeyVersion: phone·benefit key의 composite version
- phoneFingerprint: benefit-scoped HMAC
- claimedByUserId: 최초 지급 당시 canonical userId
- verificationProofId 또는 claimRequestId
- claimedAt
```

핵심 DB 제약은 다음과 같다.

```text
(benefitType, fingerprintKeyVersion, phoneFingerprint) UNIQUE
```

claim 요청 proof에는 현재 PhoneIdentity의 모든 retained phone alias와 모든 조회 가능 benefit key에서 파생한 fingerprint candidate set이 포함된다. Entitlement는 candidate 전체로 기존 TrialClaim을 먼저 조회하고 현재 ACTIVE_WRITE composite version으로만 신규 claim을 쓴다.

서로 다른 key version writer를 동시에 운영하지 않고 current claim insert와 UserEntitlement grant를 같은 Transaction으로 묶는다. legacy claim 또는 PhoneFingerprintAlias 참조가 남아 있는 key는 `LOOKUP_ONLY`로 유지하며, candidate set을 만들 수 없는 version을 설정에서 제거한 상태로 무료혜택 writer를 활성화하지 않는다.

`claimedByUserId`는 최초 지급의 감사 정보이며 계정 병합 키가 아니다. 탈퇴 시 null/tombstone 처리할지 보존할지는 개인정보 보존 정책으로 정한다.

### 10.5 UserEntitlement

```text
UserEntitlement
- entitlementId
- userId
- type: FREE_MOCK_EXAM
- grantedQuantity: 1
- reservedQuantity: 0..1
- usedQuantity: 0..1
- grantSource: PHONE_VERIFIED_TRIAL
- trialClaimId: 무료체험 grant일 때만 참조
- grantedAt
- consumedAt
- version
```

수량 불변식은 `0 <= reservedQuantity + usedQuantity <= grantedQuantity`다. 분산 시험 생성 상태는 별도 reservation으로 추적한다.

```text
EntitlementReservation
- reservationId
- entitlementId
- userId
- type
- idempotencyKey
- status: RESERVED / CONFIRMED / CANCELLED / RECONCILIATION_REQUIRED
- expiresAt
- createdAt
- confirmedAt
- cancelledAt
```

실제 무료시험 요청에서 TrialClaim 생성과 UserEntitlement grant를 Entitlement/Billing DB의 한 Transaction으로 묶는다. 같은 userId에 무료시험 entitlement를 중복 grant하지 않도록 별도 unique 또는 idempotent upsert를 적용한다. 전화 인증이나 회원가입만 완료한 상태에서는 TrialClaim과 entitlement가 아직 없어도 정상이다.

전화 인증 성공이나 회원가입 자체를 지급·사용 완료로 보지 않는다. 시험 시작 시 기존 PhoneIdentity를 바탕으로 silent claim·grant 후 다음 reserve/create/confirm protocol을 사용한다.

```text
1. Learning Core → Entitlement: reserve(userId, type, idempotencyKey)
        ↓
2. 필요하면 Identity PhoneIdentity proof 확인
   + TrialClaim/UserEntitlement idempotent grant
   + reservedQuantity 0 → 1
   + EntitlementReservation(RESERVED) 생성
   위 변경은 Entitlement/Billing의 한 Transaction
        ↓
3. Learning Core: reservationId를 unique key로 exam 생성
        ↓
4. exam 생성 성공
   → Entitlement confirm(reservationId)
   → reservedQuantity 1 → 0, usedQuantity 0 → 1
   → reservation CONFIRMED
        ↓
5. exam 생성의 확정 실패
   → Entitlement cancel(reservationId)
   → reservedQuantity 1 → 0
   → reservation CANCELLED
```

timeout처럼 exam 생성 결과가 불명확하면 reservation을 즉시 해제하지 않고 `RECONCILIATION_REQUIRED`로 전환한다. reconciliation worker는 Learning Core에서 reservationId의 exam 존재 여부를 멱등 조회해 존재하면 confirm하고, 존재하지 않음이 확정되면 cancel한다. TTL worker가 결과 확인 없이 reservation을 자동 취소해서는 안 된다.

Learning Core는 `reservationId`와 exam 시작 `idempotencyKey`에 unique 제약을 두고 같은 요청 재시도에서 같은 exam을 반환한다. confirm·cancel도 reservation status에 대한 CAS로 멱등 처리하며 `CONFIRMED → CANCELLED` 역전이를 금지한다.

### 10.6 계정 병합과 TrialClaim

- TrialClaim의 phone fingerprint는 source/target User 병합과 무관하게 유지한다.
- source entitlement는 target으로 idempotent 이전하거나 동일 type entitlement와 수량 정책에 따라 합친다.
- target에 이미 무료 entitlement 또는 사용 이력이 있으면 무료 수량을 2개로 늘리지 않는다.
- Learning Core는 Identity의 `UserMerged` event와 Entitlement의 이전 결과를 각각 멱등 처리한다.

PhoneIdentity도 canonical 선택 근거로 사용하지 않는다. 신규 모델에서는 source GUEST가 PhoneIdentity를 보유하지 않으므로 기존 identity owner인 target MEMBER의 PhoneIdentity를 유지한다. source Guest에 남아 있는 미소비 MEMBER_ENROLLMENT attempt·grant는 merge Transaction에서 더 이상 사용할 수 없도록 terminal 처리한다.

과거 데이터나 부분 rollout 때문에 source Guest에 PhoneIdentity가 발견되면 정상 자동 병합 대상으로 보지 않는다. target과 자동 덮어쓰기·교환·삭제하지 않고 migration anomaly로 기록해 별도 복구 정책으로 처리하며, 어떤 경우에도 전화번호를 MEMBER → MEMBER 자동 병합 근거로 사용하지 않는다.

## 11. 회원 탈퇴·재가입·Provider lifecycle

### 11.1 SocialIdentity

실제 SocialIdentity 저장을 시작하기 전에 다음 정책을 확정한다.

- 탈퇴 시 SocialIdentity를 물리 삭제해 같은 SNS 계정의 재가입을 허용
- 또는 subject tombstone을 개인정보 최소화 형태로 보존해 재가입을 제한

현재 LOCAL 회원이 탈퇴 후 같은 email로 재가입할 수 있는 정책과 개인정보 삭제 원칙을 고려하면, 기본 권장안은 회원 탈퇴 Transaction에서 SocialIdentity를 삭제하고 같은 SNS 계정의 신규 가입을 허용하는 것이다.

### 11.2 PhoneIdentity와 TrialClaim

- `PhoneIdentity`와 미완료 verification attempt는 개인정보 삭제 범위에 포함한다.
- Entitlement의 TrialClaim fingerprint는 검증된 번호당 1회 재지급 방지를 위해 정책상 승인된 기간 동안 유지할 수 있다.
- TrialClaim을 유지하더라도 `claimedByUserId`는 null 또는 tombstone으로 최소화할 수 있다.
- HMAC fingerprint도 pseudonymous personal data로 취급하고 목적·보존 기간·접근 권한·삭제 기준을 개인정보 처리방침과 법무 검토로 확정한다.
- 번호 재할당으로 새 소유자가 무료체험을 받지 못할 수 있다는 trade-off를 제품 정책에 명시한다.

### 11.3 탈퇴 재인증과 Apple revoke

탈퇴 검증은 현재 `UserProvider` 분기에서 로그인 수단 기반 정책으로 변경한다.

- LOCAL 자격증명이 있으면 현재 비밀번호 재확인 가능
- 소셜 전용 회원은 최근 Provider 재인증 또는 별도 recent-login 정책 필요
- Refresh Token 보유만으로 충분한지 보안·UX 결정 필요
- Apple authorization code를 서버에서 교환할지 확정
- Apple refresh/access credential의 암호화 보관·보존·접근 정책 확정
- 회원 탈퇴 시 Apple token revoke와 실패 재시도 정책 확정

Apple revoke 외부 호출과 Identity 내부 개인정보 삭제는 분산 Transaction이 아니다. 내부 탈퇴를 무기한 막지 않으면서 revoke 실패를 안전하게 재시도할 outbox 또는 lifecycle job을 설계한다.

## 12. 오류 계약

권장 오류 범주는 다음과 같다. 최종 code와 HTTP 상태는 API 구현 전에 확정한다.

| 상황 | 권장 HTTP | 외부 오류 의미 |
| --- | --- | --- |
| 지원하지 않거나 비활성 provider | 400 | 지원하지 않는 소셜 provider |
| 서명·issuer·audience·만료·nonce 검증 실패 | 401 | 유효하지 않은 소셜 자격증명 |
| socialChallengeId 위조·만료·재사용·Provider/purpose/User 불일치 | 401 | 유효하지 않은 소셜 인증 시도 (`SOCIAL_CHALLENGE_INVALID`) |
| 공개 social login에서 identity는 유효하지만 아직 미연결 | 200 또는 202 | 신규 가입 전환 (`SIGNUP_REQUIRED`) |
| identity가 다른 MEMBER 소유 | 409 | 이미 다른 계정에 연결됨 |
| 단계 7에서 Guest가 다른 canonical owner identity 제시 | 409 | 병합 기능 필요 (`MERGE_REQUIRED`) |
| socialEnrollmentGrant 위조·만료·재사용 | 400 | 유효하지 않은 소셜 가입 증명 (`SOCIAL_ENROLLMENT_GRANT_INVALID`) |
| social·phone grant의 signupAttempt/User binding 불일치 | 400 | 유효하지 않은 가입 흐름 (`ENROLLMENT_BINDING_MISMATCH`) |
| DIRECT signup 완료 직전 identity가 다른 요청에서 등록됨 | 409 | 기존 계정으로 로그인 재시도 (`SOCIAL_IDENTITY_ALREADY_REGISTERED`) |
| 승격·병합 동시성 충돌 | 409 | 계정 연결 상태 충돌 |
| merge된 source Access Token으로 보호 API 요청 | 401 | target Token으로 전환 필요 (`ACCOUNT_MERGED_TOKEN_REJECTED`) |
| 연결 User가 비활성 | 403 | 활성 상태가 아닌 계정 |
| Guest의 무료 모의고사 시작 요청 | 403 | 회원가입 필요 (`MEMBERSHIP_REQUIRED`) |
| OTP 요청 한도 초과 | 429 | 잠시 후 재시도 필요 |
| verification 만료·실패·소유 User 불일치 | 400 또는 401 | 유효하지 않은 전화번호 인증 |
| LOCAL·DIRECT·Guest social MEMBER signup의 phoneVerificationGrant 누락 | 400 | 가입용 전화 인증 증명 필요 (`PHONE_VERIFICATION_GRANT_REQUIRED`) |
| phoneVerificationGrant 위조·만료·재사용 | 400 | 유효하지 않은 전화번호 인증 (`PHONE_VERIFICATION_GRANT_INVALID`) |
| PhoneIdentity 없는 legacy MEMBER의 전화번호 필수 기능 요청 | 409 | 기존 회원 전화번호 등록 필요 (`PHONE_ONBOARDING_REQUIRED`) |
| 번호가 다른 User에 연결됨 | 409 | 이미 연결된 전화번호 |
| 무료체험이 같은 검증 번호에 지급됨 | 409 또는 멱등 200 | 무료체험 재지급 불가 |

Provider 내부 오류 message, subject 존재 여부, email은 응답에 포함하지 않는다.

## 13. 관측성과 보안 로그

허용할 구조화 event 예시는 다음과 같다.

```text
auth.social.verification.failed
auth.social.login.succeeded
auth.social.signup.required
auth.social.signup.completed
auth.social.link.succeeded
auth.social.merge.completed
auth.social.merge.conflict
auth.phone.verification.requested
auth.phone.verification.succeeded
auth.phone.verification.blocked
```

관측 sink별 식별자 정책을 분리한다.

```text
CloudWatch/application logs
- 진단에 필요한 경우 internal canonical/source userId 허용
- provider, outcome, errorCode, 처리 건수 허용

Sentry
- userId 전송 금지
- 기존 requestId·errorCode·예외 type·안전한 stack frame만 유지

Metrics
- userId를 label/tag로 절대 사용하지 않음
- provider, outcome, errorCode 같은 낮은 cardinality만 사용
```

다음 값은 로그·Sentry event·metric tag에 넣지 않는다.

- Provider credential 또는 Authorization Header
- providerSubject
- provider email과 전체 Claim
- Access Token, Refresh Token과 Hash
- nonce 원문·socialChallengeId
- 전화번호 원문·phoneLast4·phoneFingerprint
- verificationCode·providerReference·socialEnrollmentGrant·phoneVerificationGrant·verification proof

예상된 4xx는 application ERROR나 Sentry issue로 수집하지 않고 기존 request logging 계약을 유지한다.

## 14. 단계별 구현 순서

단계 번호는 참조 ID이며 전체를 한 줄로 직렬 실행한다는 뜻이 아니다. 다음 두 트랙을 병렬로 진행한다.

```text
Identity / Social track
1 SocialIdentity
→ 2 User accountType
→ 3 PhoneIdentity/OTP 기반
→ 6 challenge/verifier framework
→ 7 login/direct-signup/link (merge 제외)
→ 8 Guest merge/outbox/token gate
→ 9 Kakao
→ 10 Apple

Entitlement / Billing track
4 TrialClaim/UserEntitlement/rotation contract
→ 5 reserve/create/confirm 무료시험 protocol
→ 11 결제 확장

두 트랙 안정화
→ 12 lifecycle·운영 안정화
```

Identity/Social 트랙은 Entitlement 구현 완료를 기다리지 않는다. 다만 무료 모의고사 기능 활성화는 PhoneIdentity proof, TrialClaim rotation과 reserve/create/confirm 계약이 모두 준비된 뒤에만 가능하다.

### 단계 0. 제품·보안·서비스 간 결정 확정

소셜 계정:

다음 두 진입 경로는 확정된 범위다.

- 앱 첫 진입에서 기존 SNS identity면 Guest 생성 없이 즉시 로그인
- 앱 첫 진입에서 미연결 SNS identity면 DIRECT social signup으로 전환하고 전화 인증·필수 동의 후 MEMBER 생성
- SNS 인증 전에 서버가 SocialLoginChallenge를 발급하고, login/link 요청은 socialChallengeId로 서버 보관 expected nonce를 조회해 일회성 검증

- Kakao canonical subject를 OIDC `sub`로 사용할지 확정
- 같은 provider의 복수 identity 연결 허용 여부 확정
- 직접 소셜 회원의 필수 profile 필드와 User email nullable 정책 확정
- SocialEnrollmentAttempt·grant TTL과 중단 가입 정리 정책 확정
- Provider별 raw/변환 nonceMode와 브라우저 redirect의 state·PKCE 적용 방식 확정
- accountType fallback 전에 provider null 문서의 status·credential·guest field read-only aggregate와 anomaly 처리 승인
- 소셜 전용 회원 탈퇴 재인증 방식 확정
- 탈퇴 후 같은 SNS 계정 재가입 정책 확정

Apple lifecycle:

- Apple authorization code의 서버 교환 여부
- Apple Provider credential 암호화 보관 여부와 최소 보존 기간
- 회원 탈퇴 시 revoke 시점·실패 재시도·내부 탈퇴와의 일관성

전화번호·무료체험:

다음 두 정책은 확정된 불변식이며 단계 0의 선택 항목이 아니다.

- Guest는 무료 모의고사를 시작할 수 없고 ACTIVE MEMBER만 가능
- LOCAL 가입, DIRECT social signup과 미연결 identity를 이용한 Guest 소셜 MEMBER 승격은 전화 인증 grant를 필수로 소비

- 지원 국가를 한국 `+82 mobile`로 제한할지
- VoIP·landline 차단과 line type lookup 사용 여부
- User당 활성 PhoneIdentity 한 개 정책
- 기존 PhoneIdentity 없는 MEMBER의 grace 기간과 로그인 후 onboarding 강제 시점
- OTP provider, TTL, resend·실패·IP·user·phone rate limit
- HMAC key registry의 ACTIVE_WRITE/LOOKUP_ONLY version 목록, 최대 candidate 수, reference count와 비상 rotation runbook
- 번호 재할당 trade-off와 TrialClaim fingerprint 보존 목적·기간
- 과거·부분 rollout 데이터에서 Guest PhoneIdentity가 발견될 때의 migration anomaly 처리
- Identity → Entitlement 검증 증명의 전달·일회성 소비 방식
- reserve TTL, 결과 불명 reservation reconciliation 주기와 Learning Core 조회 계약

병합·event:

- source JWT는 target actor로 승격하지 않고 Identity와 모든 downstream 서비스에서 migration 뒤 거절하는 공통 계약의 참여 서비스 inventory·검증 기준
- merge alias와 Learning Core ownership migration 방식
- outbox transport, atomic claim lease, retry·dead-letter 운영 기준

완료 조건은 각 결정이 코드·API·개인정보 정책·데이터 migration·서비스 간 계약에 모순 없이 기록되는 것이다.

### 단계 1. 최소 SocialIdentity 기반 모델

- `SocialProvider(GOOGLE, KAKAO, APPLE)`
- `SocialIdentity(socialIdentityId, userId, provider, providerSubject[1..255], createdAt)`
- `(provider, providerSubject)` compound unique index
- userId index
- `SocialIdentityRepository`
- entity·Repository·index 테스트

email과 `updatedAt`은 넣지 않는다. Controller, Provider verifier, User, JWT와 RefreshSession도 변경하지 않는다.

### 단계 2. User 계정 유형 분리와 데이터 호환

- 운영 provider null/GUEST/LOCAL의 status·credential·guest field read-only count 집계와 anomaly 승인
- null 문서가 MEMBER 불변식을 만족한다는 증거가 있을 때만 `null → MEMBER` fallback 활성화
- `UserAccountType(GUEST, MEMBER)` 도입
- LOCAL 자격증명의 all-or-none 불변식
- 소셜 전용 MEMBER 표현
- Guest → MEMBER 승격 메서드
- `UserStatus.MERGED`, `mergedIntoUserId`, `mergedAt`
- 기존 provider 문서 read fallback과 migration
- 프로필 응답 호환 전략
- 회원 탈퇴 자격증명 분기 조정 준비

이 단계도 소셜 API를 공개하지 않는다.

### 단계 3. PhoneIdentity와 OTP verification

Identity Service 범위에서 다음을 구현한다.

- E.164 서버 정규화
- versioned HMAC fingerprint
- `PhoneIdentity`와 unique index
- `PhoneFingerprintAlias`와 version별 unique index·기존 fingerprint alias backfill
- ACTIVE_WRITE/LOOKUP_ONLY key retention과 mixed writer gate
- `PhoneVerificationAttempt`·TTL·상태 전이
- SMS verification provider adapter
- LOCAL pre-signup 공개 verification과 일회성 grant
- 향후 DIRECT_SIGNUP·USER binding을 수용할 수 있는 가입 시도 계약
- legacy User에 묶인 보호 verification
- LOCAL signup의 grant 소비 + User + PhoneIdentity 생성 Transaction
- phone/user/IP rate limit, cooldown, 실패 lock
- raw 번호·OTP 비저장과 민감 로그 차단 테스트

무료시험 지급과 exam 코드는 이 단계에 넣지 않는다.

### 단계 4. TrialClaim·UserEntitlement 모델과 서버 간 계약

Entitlement/Billing 도메인의 별도 작업으로 수행한다.

- benefit-scoped phone fingerprint 계약
- retained phone·benefit version candidate set과 legacy TrialClaim lookup
- legacy claim 참조가 남은 key의 LOOKUP_ONLY 보존·폐기 gate
- `TrialClaim` unique 제약
- `UserEntitlement`과 grant idempotency
- 무료시험 요청 시 Identity verification proof 또는 내부 claim 교환 계약
- 탈퇴 후 fingerprint 보존·claimedByUserId 최소화
- source/target merge 시 무료 수량 중복 방지

Identity 저장소에 위 entity를 복사하지 않는다.

### 단계 5. 무료 모의고사 1회 지급과 reserve/consume

Entitlement/Billing과 Learning Core의 별도 작업으로 수행한다.

- 회원가입·전화 인증 시에는 TrialClaim을 생성하지 않음
- Guest 무료시험 요청은 MEMBERSHIP_REQUIRED로 차단하고 claim·entitlement를 생성하지 않음
- 실제 무료시험 시작 시 기존 PhoneIdentity로 silent TrialClaim + entitlement atomic grant
- 같은 검증 번호 재지급 차단
- 전화 인증과 실제 사용 상태 분리
- EntitlementReservation과 `AVAILABLE → RESERVED → CONFIRMED/CANCELLED` 수량 상태 전이
- `reserve → exam 생성 → confirm`, 확정 실패 cancel protocol
- 결과 불명 timeout의 RECONCILIATION_REQUIRED 전환과 reservationId 기반 exam 존재 조회
- merge 시 entitlement 이전·통합

Identity는 시험을 생성하거나 사용 횟수를 변경하지 않는다.

### 단계 6. 유연한 Provider 검증 프레임워크와 Google

- `SocialVerificationRequest` 기반 verifier interface와 registry
- Provider별 credential request subtype
- 공통 `VerifiedSocialPrincipal`
- `SocialLoginChallenge`·server-generated nonce hash·purpose/User binding·TTL·일회성 소비
- challenge 생성 API와 conditional `PENDING → CONSUMED` CAS
- Provider 설정 validation과 feature flag
- Google signature·issuer·audience·시간·nonce·subject 검증
- 외부 통신 없는 로컬 Key/JWKS 기반 테스트
- Provider 실패 오류 정제

### 단계 7. Google 기존 로그인·직접 가입·Guest 승격

- 공개 social login API
- `SocialEnrollmentAttempt`·hashed opaque grant·TTL·binding
- 기존 identity면 즉시 인증하고 미연결 identity면 SIGNUP_REQUIRED를 반환하는 public entry application service
- 기존 identity의 Guest 없는 즉시 로그인
- DIRECT social signup 전화 인증과 socialEnrollmentGrant·phoneVerificationGrant binding
- 직접 가입의 User + PhoneIdentity + SocialIdentity + 최초 RefreshSession Transaction
- 보호된 social link prepare/finalize API
- 미연결 identity의 Guest는 유효한 두 Guest-bound grant를 소비할 때만 userId 유지 MEMBER 승격
- 두 grant 소비 + PhoneIdentity + SocialIdentity + MEMBER 승격 Transaction
- Guest가 다른 canonical User 소유 identity를 제시하면 데이터·User 상태를 변경하지 않고 `MERGE_REQUIRED` 반환
- merge feature flag는 false로 유지하고 outbox·source token 공통 계약에 의존하지 않는 login/direct-signup/unlinked-link만 활성화
- LOCAL MEMBER에 Google 추가 연결
- unique 충돌의 멱등·conflict 분류
- 기존 Access/Refresh 발급기 재사용

### 단계 8. 기존 계정 발견·Guest 병합·outbox publisher

- 모든 참여 서비스의 source JWT actor 거절·ownership migration 원자성 계약을 선행 검증
- 단계 7의 `MERGE_REQUIRED` 분기를 실제 merge로 전환하는 feature flag 활성화
- source ACTIVE GUEST·target ACTIVE MEMBER 불변식
- MEMBER → MEMBER 자동 병합 금지
- canonical chain resolve와 순환 방지
- source User `MERGED` 상태 전환
- source RefreshSession 전체 폐기
- target userId 기준 새 Token 발급
- 전달 상태·lease·retry를 가진 `UserMergedOutbox`
- at-least-once publisher와 Learning Core eventId 멱등
- source JWT target 권한 비승격·Identity 거절·downstream deny marker/write fencing 계약
- 동시 병합·재시도·rollback 테스트

### 단계 9. Kakao 추가

- Kakao OIDC `sub`를 canonical subject로 검증
- 기존 verifier registry에 Kakao request·adapter 추가
- 동일 login/direct-signup/link/merge 서비스를 재사용
- Google과 같은 subject 문자열도 provider가 다르면 허용
- Kakao feature flag로 독립 배포

### 단계 10. Apple 추가와 revoke lifecycle

- Apple identity token signature·issuer·audience·nonce 검증
- authorization code를 사용하는 경우의 서버 교환
- 최초 승인 이후 email 부재 시나리오
- Key rotation 테스트
- 탈퇴 시 revoke와 실패 재시도
- 기존 login/direct-signup/link/merge 서비스 재사용
- Apple feature flag로 독립 배포

### 단계 11. 결제·Entitlement 확장

Entitlement/Billing 도메인에서 무료 grant 구조를 다음으로 확장한다.

- 구매 모의고사 사용권
- 월간·연간 구독
- 결제 webhook idempotency
- 환불·취소·만료 상태 전이
- 무료·유료 entitlement reserve/consume 우선순위

Identity 저장소에는 결제 Provider나 상품 로직을 추가하지 않는다.

### 단계 12. 계정 lifecycle와 운영 안정화

- 회원 탈퇴 SocialIdentity·PhoneIdentity 처리
- 소셜 전용 회원 재인증
- 선택된 경우의 연결 해제와 마지막 로그인 수단 보호
- TrialClaim 보존·삭제 job과 법무 정책 반영
- SMS·outbox 비용·지연·재시도·dead-letter 관측
- staging index와 query plan 확인
- provider별 점진 활성화와 rollback runbook

## 15. 예상 변경 파일 묶음

실제 패키지명은 현재 `domain.auth`, `domain.user`, `global` 경계를 따른다.

```text
domain/auth/domain/enums/SocialProvider.java
domain/auth/domain/entity/SocialIdentity.java
domain/auth/domain/repository/SocialIdentityRepository.java
domain/auth/domain/entity/SocialLoginChallenge.java
domain/auth/domain/repository/SocialLoginChallengeRepository.java
domain/auth/domain/entity/SocialEnrollmentAttempt.java
domain/auth/domain/repository/SocialEnrollmentAttemptRepository.java

domain/auth/application/SocialLoginService.java
domain/auth/application/SocialChallengeService.java
domain/auth/application/SocialEnrollmentService.java
domain/auth/application/SocialSignupTransactionService.java
domain/auth/application/SocialLinkService.java
domain/auth/application/SocialLinkTransactionService.java
domain/auth/application/UserMergeTransactionService.java

domain/auth/dto/request/SocialLoginRequest.java
domain/auth/dto/request/SocialChallengeRequest.java
domain/auth/dto/request/SocialSignupRequest.java
domain/auth/dto/request/SocialLinkRequest.java
domain/auth/dto/response/SocialEntryResponse.java
domain/auth/dto/response/SocialChallengeResponse.java
domain/auth/dto/response/SocialAuthResponse.java

domain/auth/provider/SocialTokenVerifier.java
domain/auth/provider/SocialVerificationRequest.java
domain/auth/provider/VerifiedSocialPrincipal.java
domain/auth/provider/GoogleVerificationRequest.java
domain/auth/provider/KakaoOidcVerificationRequest.java
domain/auth/provider/AppleVerificationRequest.java
domain/auth/provider/GoogleTokenVerifier.java
domain/auth/provider/KakaoTokenVerifier.java
domain/auth/provider/AppleTokenVerifier.java

domain/user/domain/enums/UserAccountType.java
domain/user/domain/entity/User.java
domain/user/domain/entity/PhoneIdentity.java
domain/user/domain/repository/PhoneIdentityRepository.java
domain/user/domain/entity/PhoneFingerprintAlias.java
domain/user/domain/repository/PhoneFingerprintAliasRepository.java

domain/auth/domain/entity/PhoneVerificationAttempt.java
domain/auth/domain/repository/PhoneVerificationAttemptRepository.java
domain/auth/application/PhoneVerificationService.java
domain/auth/application/PhoneVerificationTransactionService.java
domain/auth/application/SignupTransactionService.java
domain/auth/dto/request/PhoneVerificationRequest.java
domain/auth/dto/request/PhoneVerificationConfirmRequest.java
domain/auth/dto/response/PhoneVerificationResponse.java
domain/auth/dto/request/SignupRequest.java

global/security/phone/PhoneFingerprintService.java
global/security/phone/PhoneVerificationGrantHasher.java
global/security/social/SocialEnrollmentGrantHasher.java
global/security/social/SocialNonceGenerator.java
global/phone/PhoneNumberNormalizer.java
global/phone/PhoneVerificationProvider.java

domain/user/domain/entity/UserMergedOutbox.java
domain/user/domain/repository/UserMergedOutboxRepository.java
domain/user/application/UserMergedOutboxPublisher.java
docs/contracts/identity-learning-user-merge.md
docs/contracts/identity-entitlement-phone-trial.md
```

Provider HTTP/JWT 기술 구현은 `global`에 두고 Auth 도메인은 interface에만 의존하는 구성도 가능하다. 구현 시 외부 기술 의존성과 도메인 규칙이 섞이지 않도록 최종 package를 한 번 확정한다.

`TrialClaim`, `UserEntitlement`, `EntitlementReservation`, 결제와 exam consume 구현 파일은 이 저장소의 예상 변경 파일에 포함하지 않는다. 별도 Entitlement/Billing 및 Learning Core 저장소 계획에서 관리한다.

## 16. 테스트 계획

### 16.1 SocialIdentity 단위·Repository 테스트

- 정상 생성과 UUID·시간 필드
- null/blank provider subject 거부
- providerSubject 255자 허용·256자 거절과 case-sensitive 원문 보존
- email·`updatedAt` 필드가 모델과 index에 없음
- Google·Kakao·Apple 각각 저장 가능
- 동일 provider+subject의 서로 다른 문서 저장 차단
- 같은 subject라도 provider가 다르면 허용
- 한 userId에 Google+Kakao+Apple 모두 연결 가능
- provider+subject 조회로 canonical userId 확인
- userId 전체 조회
- index 이름·key 순서·unique option 검증

현재 test profile은 Mongo 자동설정을 제외하므로 실제 duplicate insert 검증에는 Atlas를 호출하지 않는 격리 Mongo 테스트가 필요하다. 완료를 선언하는 환경에서는 이 테스트를 silently skip하지 않는다.

### 16.2 PhoneIdentity·OTP 테스트

- 국내 입력과 국제 입력의 동일 E.164 정규화
- 잘못된 국가 코드·길이·line type 거절
- 동일 번호·같은 key version의 동일 deterministic HMAC
- 다른 domain separator 또는 key version의 다른 fingerprint
- OTP 검증 시 ACTIVE_WRITE와 모든 LOOKUP_ONLY key의 PhoneFingerprintAlias 생성
- 기존 v1 alias가 있는 번호의 v2 가입은 v1 alias unique 충돌로 전체 거절
- 기존 PhoneIdentity의 저장된 version·fingerprint alias backfill은 raw 번호 없이 가능
- legacy alias·TrialClaim 참조가 남은 lookup key의 RETIRED 전환 거절
- mixed writer version 감지 시 회원가입·무료 claim writer 활성화 거절
- raw 번호가 PhoneIdentity·로그·예외·Sentry에 없음
- 같은 phoneFingerprint의 다른 User 연결 차단
- 같은 userId의 두 활성 PhoneIdentity 차단
- pre-signup attempt는 USER가 아니라 SIGNUP_ATTEMPT binding 사용
- 공개 confirm은 짧은 TTL의 grant를 발급하고 DB에는 hash만 저장
- 유효·미사용 grant로만 User+PhoneIdentity 생성
- 만료·재사용·위조 grant의 signup 거절
- signup 저장 실패 시 grant 소비와 User·PhoneIdentity 전체 rollback
- DIRECT social phone attempt와 SocialEnrollmentAttempt가 같은 signupAttemptId에 binding
- 서로 다른 signupAttemptId의 social·phone grant 조합 거절
- Guest MEMBER_ENROLLMENT confirm은 grant만 발급하고 PhoneIdentity를 미리 만들지 않음
- Guest social signup은 grant 소비·MEMBER 승격·PhoneIdentity·SocialIdentity가 모두 commit되거나 rollback
- 가입을 중단한 검증 Guest가 PhoneIdentity unique 값을 선점하지 않음
- verificationId와 현재 JWT `sub` 불일치 거절
- resend cooldown, user·phone·IP rate limit
- OTP 만료·실패 횟수 lock·성공 후 재사용 거절
- attempt TTL·terminal 상태와 PhoneIdentity 생성 Transaction rollback
- 외부 SMS provider·line type provider는 mock 처리

### 16.3 TrialClaim·Entitlement 계약 테스트

별도 Entitlement/Billing 저장소에서 다음을 검증한다.

- 같은 benefitType+current phoneFingerprint의 중복 claim 차단
- current·legacy phone/benefit candidate 중 하나라도 과거 TrialClaim과 일치하면 신규 claim 차단
- 탈퇴 후 새 key version으로 같은 번호를 인증해도 retained alias candidate로 과거 claim 발견
- legacy TrialClaim 참조 key를 제거한 설정에서는 무료 claim writer 기동 실패
- 같은 검증 proof/event 재전달 멱등
- claim과 entitlement grant의 atomic rollback
- 회원가입·전화 인증 직후 TrialClaim과 entitlement가 생성되지 않음
- Guest 무료시험 요청은 MEMBERSHIP_REQUIRED이고 TrialClaim·entitlement가 생성되지 않음
- 첫 무료시험 요청에서만 silent claim과 entitlement grant
- PhoneIdentity 없는 legacy MEMBER는 PHONE_ONBOARDING_REQUIRED
- 동일 reserve idempotency key 재시도에서 같은 reservation 반환
- reserve 시 reservedQuantity 증가와 reservation 생성이 원자적
- exam 생성 성공 후 confirm은 reservedQuantity 감소·usedQuantity 증가를 정확히 1회 적용
- exam 생성 확정 실패 cancel은 reservedQuantity만 복원하고 usedQuantity를 변경하지 않음
- exam 생성 timeout은 자동 cancel하지 않고 RECONCILIATION_REQUIRED로 전환
- reconciliation은 reservationId exam 존재 시 confirm, 부재 확정 시 cancel
- CONFIRMED reservation의 cancel과 CANCELLED reservation의 confirm 거절
- source/target merge 시 무료 수량이 2개로 증가하지 않음
- source/target PhoneIdentity 조합별 이전·유지·충돌 정책
- 탈퇴 후 PhoneIdentity가 삭제돼도 보존 정책 기간의 TrialClaim 재지급 차단

### 16.4 Provider verifier 테스트

Provider별로 다음을 검증한다.

- 정상 서명과 Claim
- 잘못된 서명
- 잘못된 issuer
- 잘못된 audience
- 만료 또는 아직 유효하지 않은 Token
- 누락·blank subject
- nonce 불일치
- 만료·소비된 socialChallengeId 재사용 거절
- 같은 PENDING challenge의 동시 요청에서 atomic CAS winner 하나만 login/enrollment 진행
- challenge의 Provider·purpose 불일치 거절
- LINK challenge와 현재 JWT `sub` binding 불일치 거절
- login request의 raw nonce가 서버 expected nonce를 대체할 수 없음
- Provider별 nonceMode의 전달값·Claim 비교 규칙
- email 누락
- request provider와 credential subtype 불일치
- Apple authorization code 누락·재사용·교환 실패의 안전한 변환
- JWKS Key rotation과 알 수 없는 `kid`
- 외부 endpoint timeout·오류의 안전한 변환

실제 Google·Kakao·Apple endpoint는 기본 테스트에서 호출하지 않는다.

### 16.5 application service 테스트

- 앱 첫 진입의 기존 social identity는 Guest 생성 없이 canonical userId로 즉시 로그인
- 앱 첫 진입의 미연결 identity는 SIGNUP_REQUIRED와 짧은 socialEnrollmentGrant를 반환하고 User를 생성하지 않음
- Provider credential·전체 Claim·email 원문이 SocialEnrollmentAttempt에 저장되지 않음
- DIRECT social signup은 같은 signupAttemptId의 유효한 social·phone grant와 필수 동의를 모두 요구
- DIRECT social signup 성공 시 새 UUID MEMBER·PhoneIdentity·SocialIdentity·최초 RefreshSession 생성 후 Token 발급
- DIRECT social signup에는 Provider email이 없어도 성공하고 email을 로그인 식별자로 저장하지 않음
- 만료·위조·재사용 socialEnrollmentGrant와 두 grant binding 불일치 거절
- 기존 identity 로그인은 해당 canonical userId로 Token 발급
- Guest 생성에는 전화 인증을 요구하지 않음
- LOCAL signup은 유효한 phoneVerificationGrant를 필수로 소비
- 기존 MEMBER 로그인에는 반복 OTP를 요구하지 않음
- provider null 운영 fixture의 status·credential·guest field 집계와 anomaly 존재 시 fallback 중단
- 미연결 identity 로그인은 User를 자동 생성하지 않음
- 미연결 identity의 Guest social signup은 유효한 Guest-bound phoneVerificationGrant가 필수
- Guest social signup 성공 시 같은 Transaction에서 userId 유지 MEMBER 승격과 PhoneIdentity·SocialIdentity 생성
- grant 없는 Guest social signup은 PHONE_VERIFICATION_GRANT_REQUIRED
- 단계 7에서 기존 identity owner를 발견한 Guest는 상태 변경 없이 MERGE_REQUIRED
- 단계 8 flag 활성화 뒤 기존 identity owner로 merge하는 Guest에는 가입 OTP를 다시 요구하지 않음
- LOCAL MEMBER에 세 provider 연결 가능
- 같은 identity 재연결 멱등 성공
- 다른 MEMBER 소유 identity 연결 거절
- 단계 8 flag 활성화 뒤 다른 Guest가 기존 identity 연결 시 canonical User 병합
- MEMBER → MEMBER 자동 병합 거절
- source와 target 동일 User 거절
- source가 ACTIVE GUEST가 아니거나 target이 ACTIVE MEMBER가 아니면 거절
- mergedInto chain resolve와 순환·최대 깊이 거절
- 새 Access/RefreshSession은 target userId만 사용
- source Session 전체 폐기
- Identity의 merged source JWT 보호 API 요청은 ACCOUNT_MERGED_TOKEN_REJECTED
- suspended·withdrawn·merged User 로그인 거절
- Repository·Provider verifier는 mock 처리

### 16.6 Transaction·동시성·outbox 테스트

- DIRECT social signup의 User·PhoneIdentity·SocialIdentity·RefreshSession·두 attempt 소비 전체 rollback
- 같은 provider+subject의 동시 DIRECT signup에서 정확히 한 User만 생성
- DIRECT signup 중 SocialIdentity unique 충돌 시 고아 User·PhoneIdentity·RefreshSession 없음
- 같은 전화번호의 동시 DIRECT signup에서 자동 merge 없이 한 요청만 성공
- socialEnrollmentAttempt·phoneVerificationAttempt conditional consume update count가 각각 1인 경우만 signup commit
- 같은 challenge/grant 동시 소비에서 CAS loser는 User·Session·Token을 만들지 않음
- User 승격 저장 실패 시 SocialIdentity rollback
- SocialIdentity 저장 실패 시 User 승격 rollback
- RefreshSession 또는 outbox 저장 실패 시 전체 rollback
- 동일 identity 동시 연결에서 owner 한 명만 생성
- 동일 Guest 동시 승격의 멱등 또는 명시적 충돌
- 양방향 동시 병합에서 순환 merge가 생기지 않음
- outbox atomic claim은 한 publisher만 성공
- publisher crash 후 lease 만료 문서 재처리
- publish 성공 후 mark 전 crash의 중복 전달 허용
- retry backoff·attemptCount·FAILED/PUBLISHED 상태 전이
- 같은 event 재전달 시 Learning Core 처리 멱등
- Learning Core consumer의 ownership migration·source deny marker 저장이 같은 Transaction
- consumer 전 source JWT는 source 데이터에만 접근하고 consumer 후 source JWT는 거절
- migration과 source write 경합에서 deny marker/conditional write가 source 신규 데이터 생성을 차단

실제 replica set Transaction과 index는 staging에서 재검증한다.

### 16.7 API·Security·OpenAPI 테스트

- LOGIN_OR_SIGNUP challenge·social login·DIRECT social signup·해당 phone verification만 공개, LINK challenge와 social link prepare/finalize는 Bearer 인증 필수
- challenge 응답은 socialChallengeId·providerNonce·expiresIn만 포함하고 login/link 요청은 raw nonce 대신 socialChallengeId를 받음
- social login 응답은 AUTHENTICATED와 SIGNUP_REQUIRED를 명시적으로 구분
- DIRECT social signup Body에 socialEnrollmentGrant·phoneVerificationGrant·동의가 있고 userId·provider credential 재입력이 없음
- LOCAL·DIRECT pre-signup phone verification route만 공개되고 USER phone verification route는 Bearer 필수
- Guest 무료시험 요청은 MEMBERSHIP_REQUIRED 응답이고 entitlement grant·reserve 호출이 없음
- signup grant 누락은 400 PHONE_VERIFICATION_GRANT_REQUIRED, legacy onboarding은 409 PHONE_ONBOARDING_REQUIRED로 구분
- merge flag 비활성 상태의 다른 owner Guest 연결은 409 MERGE_REQUIRED이고 mutation·target Token이 없음
- Request Body에 `userId`가 없음
- credential 필드는 OpenAPI `writeOnly`
- phoneNumber·verificationCode·socialEnrollmentGrant·phoneVerificationGrant도 OpenAPI `writeOnly`
- confirm Body에 phoneNumber와 userId가 없음
- signup Body에는 phoneNumber·verificationCode가 없고 grant만 있음
- validation rejected value 비노출
- 예상된 4xx에서 ERROR·Sentry event 없음
- application log에는 정책상 허용된 internal userId만 존재할 수 있음
- Sentry와 metrics에는 userId가 없음
- 성공·실패 로그에 Provider 자격증명·subject·email·phone·OTP·fingerprint가 없음
- 기존 signup·guest·login·reissue·logout 계약 회귀 없음

### 16.8 최종 검증

```text
./gradlew clean test
```

외부 Atlas나 실제 OAuth Provider를 테스트에서 호출하지 않는다.

## 17. 배포 순서와 rollback

1. API writer 없이 `social_identities` collection mapping과 index를 먼저 배포한다.
2. staging에서 unique index와 userId index의 실제 생성 상태를 확인한다.
3. 운영 User를 read-only aggregate해 provider null/GUEST/LOCAL별 status·credential·guest field count를 확인하고 anomaly가 있으면 accountType rollout을 중단한다.
4. 검증된 fallback과 User accountType 호환 코드를 배포하고 기존 문서를 backfill한다.
5. SMS 발송 flag가 false인 상태로 PhoneIdentity·PhoneFingerprintAlias·verification attempt·index와 rate limit을 배포한다.
6. 기존 PhoneIdentity의 저장된 version·fingerprint alias를 backfill하고 alias unique index와 ACTIVE_WRITE/LOOKUP_ONLY key registry reference count를 확인한 뒤 writer를 연다.
7. staging 전용 설정으로 Guest 무인증 진입, Guest 무료시험 차단, LOCAL pre-signup grant와 User+PhoneIdentity+alias Transaction, DIRECT social signup의 conditional 두 grant 소비, Guest social enrollment, OTP abuse·민감정보 비노출을 검증한다.
8. Entitlement/Billing에 TrialClaim·UserEntitlement·EntitlementReservation을 writer 비활성 상태로 배포하고 index·legacy candidate query·key retention gate를 확인한다.
9. Identity proof candidate set과 Entitlement `reserve → Learning Core exam 생성 → confirm/cancel/reconcile`을 staging에서 end-to-end 검증한다.
10. 모든 소셜·merge feature flag가 false인 상태로 challenge/verifier와 API artifact를 배포한다.
11. Google login/direct-signup/unlinked-link만 활성화하고 challenge CAS, SIGNUP_REQUIRED, 동시 가입/연결과 MERGE_REQUIRED 무변경 응답을 검증한다.
12. Identity와 모든 user-owned downstream service에서 source actor deny marker·ownership migration Transaction·source write fencing·target Token 전환을 검증한 뒤 merge flag를 활성화한다.
13. Kakao, Apple을 각각 독립적으로 활성화한다.
14. 결제 기능은 무료 entitlement 안정화 뒤 별도 flag로 활성화한다.
15. SMS 비용·TrialClaim version 교차 중복·reservation reconciliation·중복 identity·merge 지연·민감정보 로그를 관찰한 뒤 점진 확대한다.

문제 발생 시 SMS 발송, 무료 entitlement grant, Provider 로그인, outbox publish와 결제를 각각 독립 kill switch로 중단한다. 기존 SocialIdentity·PhoneIdentity·TrialClaim·outbox와 index를 즉시 삭제하지 않으며 데이터 rollback은 별도 검증된 migration으로만 수행한다.

## 18. 단계별 완료 정의

전체 작업은 다음 조건을 모두 만족할 때 완료다.

- 한 canonical User에 Google·Kakao·Apple identity를 함께 연결할 수 있다.
- 동일 `(provider, providerSubject)`는 시스템 전체에서 한 User만 소유한다.
- providerSubject는 case-sensitive opaque 값으로 보존되고 1~255자만 허용된다.
- SocialIdentity에는 email과 의미 없는 `updatedAt`이 없고 email은 어떤 경로에서도 로그인 식별자로 사용되지 않는다.
- 앱 첫 진입에서 기존 SNS identity는 Guest 생성 없이 canonical MEMBER로 즉시 로그인한다.
- 앱 첫 진입에서 미연결 SNS identity는 User를 즉시 만들지 않고 SIGNUP_REQUIRED와 짧은 socialEnrollmentGrant를 반환한다.
- DIRECT social signup은 같은 signupAttemptId에 묶인 socialEnrollmentGrant·phoneVerificationGrant와 필수 동의가 있어야 User·PhoneIdentity·SocialIdentity·RefreshSession을 원자적으로 생성한다.
- 동시 DIRECT social signup은 같은 provider+subject에 User를 하나만 만들고 실패 Transaction에 고아 데이터가 없다.
- 기존 identity 로그인은 저장된 canonical userId를 JWT `sub`로 발급한다.
- provider null 운영 데이터의 status·credential·guest field 집계가 MEMBER fallback 전에 검증되고 anomaly가 해소됐다.
- Guest 최초 연결은 기존 Guest userId를 유지한다.
- ACTIVE GUEST → ACTIVE MEMBER만 자동 병합되고 MEMBER → MEMBER 자동 병합은 거절된다.
- merge chain은 canonical target으로 resolve되며 순환 참조가 생기지 않는다.
- source User에 새 Token이 발급되지 않는다.
- source JWT는 어느 서비스에서도 target actor로 승격되지 않고 Identity와 migration 완료 downstream service에서 거절된다.
- 각 downstream consumer는 ownership migration·source deny marker를 같은 Transaction으로 저장하고 migration 이후 source write를 차단한다.
- lease·retry·at-least-once를 가진 outbox가 event를 전달하고 Learning Core가 eventId로 멱등 처리한다.
- 전화번호는 E.164 정규화 후 versioned HMAC fingerprint로만 장기 식별된다.
- PhoneFingerprintAlias가 ACTIVE_WRITE·LOOKUP_ONLY version candidate를 보유하고 version 교차 동일 번호를 unique index로 차단한다.
- PhoneIdentity 또는 TrialClaim 참조가 남은 legacy key는 LOOKUP_ONLY로 유지되고 mixed writer는 배포 gate에서 차단된다.
- 같은 번호가 다른 User에 확인돼도 자동 계정 병합되지 않는다.
- Guest 생성·둘러보기에는 전화 인증이 없지만 무료 모의고사 시작은 MEMBERSHIP_REQUIRED로 차단된다.
- 신규 LOCAL은 유효한 phoneVerificationGrant, DIRECT·Guest 소셜 MEMBER enrollment는 유효한 socialEnrollmentGrant와 phoneVerificationGrant 없이는 완료되지 않는다.
- Guest MEMBER_ENROLLMENT 인증 성공만으로 PhoneIdentity를 만들지 않고 최종 회원가입 Transaction에서 MEMBER 전환과 함께 생성한다.
- 이미 가입된 social identity로 로그인·merge할 때는 신규 가입 OTP를 반복하지 않는다.
- 기존 MEMBER 로그인마다 OTP를 반복하지 않으며 legacy MEMBER는 명시적인 1회 onboarding을 거친다.
- signup은 일회성 phoneVerificationGrant를 소비해 User와 PhoneIdentity를 같은 Transaction으로 생성한다.
- OTP cooldown·user/phone/IP rate limit·TTL·실패 lock·일회성 소비가 동작한다.
- retained phone·benefit fingerprint candidate 전체 조회와 current unique TrialClaim이 key rotation·탈퇴·재가입 후에도 승인된 보존 기간의 재지급을 막는다.
- 회원가입·전화 인증만으로 TrialClaim을 만들지 않고 첫 무료시험 요청에서 silent claim한다.
- 전화 인증, entitlement grant·reservation과 실제 exam confirm consume이 서로 다른 상태로 관리된다.
- reserve/create/confirm/cancel은 멱등이고 결과 불명 timeout은 reconciliation 전까지 자동 해제되지 않아 무료 사용권이 중복되거나 유실되지 않는다.
- Identity는 TrialClaim·결제·시험 코드를 소유하거나 Learning Core 데이터를 직접 수정하지 않는다.
- 모든 Provider Token 검증은 signature·issuer·audience·시간·subject와 socialChallengeId로 조회한 서버 expected nonce를 확인하고 challenge를 일회성 소비한다.
- Apple account deletion의 revoke lifecycle과 재시도가 검증됐다.
- 실제 외부 Provider와 Atlas를 호출하지 않는 자동 테스트가 통과한다.
- 전체 `./gradlew clean test`가 성공한다.
- Provider 자격증명·subject·email·전화번호·OTP·fingerprint·Secret이 로그와 Sentry에 노출되지 않는다.
- Sentry와 metrics에는 userId가 없고 metrics에 high-cardinality label이 없다.

## 19. 이번 첫 구현 범위

전체 로드맵 중 다음 작업만 첫 PR 범위로 삼는다.

```text
SocialProvider(GOOGLE, KAKAO, APPLE)
SocialIdentity(socialIdentityId, userId, provider, providerSubject[1..255], createdAt)
(provider, providerSubject) unique index
userId index
SocialIdentityRepository
도메인·Repository·index 테스트
```

첫 PR에는 email과 `updatedAt`을 넣지 않는다. Google·Kakao·Apple Provider 검증, SocialLoginChallenge·SocialEnrollmentAttempt·직접 소셜 가입 API, Controller, Guest 승격, User migration, PhoneIdentity·OTP, TrialClaim·Entitlement, 병합, Learning Core event와 기존 Access/Refresh 로직도 변경하지 않는다.

## 20. 참고 자료

- [Google ID Token 서버 검증](https://developers.google.com/identity/gsi/web/guides/verify-google-id-token)
- [Kakao Login 사전 설정과 OpenID Connect](https://developers.kakao.com/docs/en/kakaologin/prerequisite)
- [Apple 계정 삭제와 Token revoke](https://developer.apple.com/documentation/technotes/tn3194-handling-account-deletions-and-revoking-tokens-for-sign-in-with-apple)
- [Twilio Verify 보안 권장사항](https://www.twilio.com/docs/verify/developer-best-practices)
- [Twilio Verification API](https://www.twilio.com/docs/verify/api/verification)
- [Twilio 전화번호 검증 권장사항](https://www.twilio.com/en-us/blog/best-practices-phone-number-validation-user-enrollment)
- [Firebase 전화번호 인증 안내](https://firebase.google.com/docs/auth/web/phone-auth)
