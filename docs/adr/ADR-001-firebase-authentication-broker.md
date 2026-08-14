# ADR-001: Firebase는 credential broker, Identity는 canonical account owner로 사용한다

- Jira: `TMI-90`
- 결정일: 2026-08-13
- 상태: 조건부 채택
- production 상태: 비활성
- 관련 문서: `docs/contracts/social-login-implementation-plan.md`, `docs/poc/firebase-auth-broker-stage-0.md`, `docs/adr/ADR-002-phone-eligibility-binding-server-contract.md`

## 배경

토선생은 email/password, Google, Apple, Kakao와 전화번호 소유 검증을 지원해야 한다. 동시에 Guest가 인증수단을 연결해도 기존 학습 데이터의 canonical `userId`가 바뀌면 안 되고, 여러 기기의 Guest가 기존 MEMBER 계정을 발견했을 때 명시적 merge가 가능해야 한다.

Firebase UID나 email, phone을 서비스 전체의 사용자 ID로 사용하면 Firebase lifecycle과 내부 데이터 소유권이 결합된다. 반대로 모든 Provider credential, Password lifecycle과 SMS OTP를 Identity에서 직접 구현하면 초기 구현과 보안 운영 범위가 커진다.

## 결정

Firebase Authentication은 credential 등록·소유 검증과 Firebase ID Token 발급만 담당한다. Identity Service는 다음 항목을 계속 소유한다.

- UUID 문자열 canonical `userId`
- `User`의 GUEST/MEMBER, ACTIVE/SUSPENDED/WITHDRAWN/MERGED 상태
- 필수 동의와 프로필
- Firebase UID와 canonical `userId`의 매핑
- `SocialIdentity`와 `PhoneIdentity`
- Guest 승격과 Guest → MEMBER merge
- 자체 RS256 Access Token, RefreshSession과 JWKS

Firebase 인증 성공은 토선생 회원가입 완료가 아니다. 신규 MEMBER와 Guest 승격은 같은 Firebase User에 연결된 승인된 primary credential, 검증된 phone credential, 필수 동의와 Identity Transaction이 모두 성공해야 완료된다.

Firebase ID Token은 Identity의 교환·고위험 endpoint에만 제출한다. Learning Core와 다른 downstream은 기존처럼 Identity JWT만 검증한다.

## 식별자 계약

- Identity JWT `sub`는 canonical UUID `userId`다.
- Firebase UID, provider subject, email과 phone은 JWT `sub`가 아니다.
- `(firebaseProjectId, firebaseUid)`는 Firebase account mapping key다.
- `(provider, providerSubject)`는 SocialIdentity의 전역 충돌 방지 key다.
- email과 phone 일치만으로 canonical User를 선택하거나 자동 병합하지 않는다.
- 전화번호 uniqueness는 회원가입 자격 제약이며 merge 규칙이 아니다.

## Provider 결정

| Provider | 결정 | Stage 0 근거 | production gate |
| --- | --- | --- | --- |
| Email/password | Firebase 사용 | Emulator 가입·재로그인, Claim과 Admin 조회 검증 | 실제 email action link·template·발송 도메인 |
| Google | Firebase 사용 | Emulator에서 같은 credential의 Firebase UID·provider UID 안정성 검증 | Android/iOS 실제 OAuth·redirect·계정 link |
| Apple | Firebase 사용 | Emulator에서 같은 credential의 Firebase UID·provider UID 안정성 검증 | 실제 nonce·private relay·authorization revoke·삭제 |
| Phone | Firebase verification + 기존 User에 link | UID 유지, phone-only user 생성, 번호 충돌·owner 삭제 후 재사용 검증 | Android/iOS/Web SDK link, 국내 SMS·quota·abuse |
| Kakao | 기본 비활성, Generic OIDC 조건부 | 공개 discovery의 code flow·PKCE S256·RS256·pairwise subject 확인 | Identity Platform 등록·billing·stable providerData UID·모바일 복귀 |

Emulator의 Google·Apple 검증은 Firebase account/provider mapping의 가능성을 확인한 것이다. 실제 Google·Apple이 발급한 credential의 서명, consent screen과 모바일 redirect까지 검증한 결과로 해석하지 않는다.

Kakao는 production gate가 모두 통과하기 전 `FIREBASE_KAKAO_ENABLED=false`다. 실패하면 직접 verifier나 Custom Token bridge로 자동 전환하지 않고 별도 ADR을 작성한다.

## Email verification 정책

email/password로 신규 가입하거나 Guest를 승격할 때 Firebase `email_verified=true`를 요구한다. 미검증 password account는 Identity MEMBER enrollment를 완료할 수 없다.

Google·Apple·Kakao가 제공하는 email은 로그인 식별자나 merge key로 사용하지 않는다. 소셜 계정 소유권은 Firebase providerData의 승인된 provider ID와 stable provider UID로 판단한다.

## Phone linking 정책

phone은 독립적인 토선생 로그인 수단이 아니라 MEMBER 가입 proof다.

1. 사용자는 먼저 email/password 또는 승인된 SNS로 Firebase User에 인증한다.
2. client는 그 current Firebase User에 phone credential을 link한다.
3. link 전후 Firebase UID가 같은지 확인한다.
4. client는 강제 갱신한 Firebase ID Token을 제출한다.
5. Identity는 Token UID, Admin UserRecord의 primary providerData·phone provider와 검증 번호를 함께 확인한다.

phone link 직후 발급된 Token의 `sign_in_provider`가 `phone`일 수 있다. 따라서 enrollment 검증은 `sign_in_provider` 하나만 보지 않고 같은 UID의 Admin providerData에 승인된 primary credential과 phone이 함께 있는지 확인한다.

일반 LOGIN_EXCHANGE에서는 phone-only Firebase User를 `PHONE_ONLY_LOGIN_NOT_ALLOWED`로 거절한다. phone credential 직접 sign-in을 client 계약에서 금지하더라도 Firebase 자체에서는 phone-only User 생성이 가능하므로 서버 방어를 반드시 둔다.

같은 번호가 다른 Firebase User나 내부 PhoneIdentity에 연결돼 있으면 `PHONE_ALREADY_LINKED`로 거절한다. 이 충돌은 자동 merge 근거가 아니다. SUSPENDED MEMBER도 번호 점유를 유지한다.

## Firebase ID Token 검증 정책

Stage 3 adapter는 Controller와 application service에서 Firebase Admin SDK를 격리한다. 검증 결과에는 허용된 project·tenant, Firebase UID, 인증 시각, 승인된 provider와 최소한의 검증 상태만 포함하고 raw Token·전체 Claim·email·phone을 전달하지 않는다.

모든 Firebase 사용 경로는 signature와 허용 algorithm, project issuer·audience, tenant, `exp`·`iat`·`auth_time`, 비어 있지 않은 UID, disabled 상태와 provider allowlist를 검사한다.

| 목적 | revoke 검사 | 최대 인증 경과 | Firebase remote 조회 |
| --- | --- | --- | --- |
| LOGIN_EXCHANGE | 적용 | 15분 | 있음 |
| DIRECT_ENROLLMENT | 적용 | 5분 | 있음 |
| GUEST_ENROLLMENT | 적용 | 5분 | 있음 |
| GUEST_MERGE | 적용 | 5분 | 있음 |
| AUTH_METHOD_SYNC | 적용 | 5분 | 있음 |
| HIGH_RISK_REAUTHENTICATION | 적용 | 5분 | 있음 |
| Identity RefreshSession reissue | 적용하지 않음 | 적용하지 않음 | 없음 |
| Learning Core 일반 요청 | 적용하지 않음 | 적용하지 않음 | 없음 |

Firebase timeout이나 일시 장애는 성공으로 우회하지 않는다. quota/rate limit은 안정적인 429, 그 외 일시 장애는 503으로 변환하고 SDK 원문 오류를 외부 응답이나 로그에 넣지 않는다.

## Enrollment 계약

미등록 Firebase User가 `/exchange`를 반복 호출해도 내부 User를 즉시 생성하지 않는다. 짧은 수명의 `FirebaseEnrollmentAttempt`만 생성하거나 기존 attempt를 재사용한다.

- application 만료 기준: 10분
- TTL index: 물리 cleanup 전용이며 correctness에 사용하지 않음
- DIRECT_SIGNUP key: project + Firebase UID + binding type
- GUEST_USER key: project + Firebase UID + binding type + Guest userId
- 같은 binding에는 유효한 PENDING attempt 하나만 허용
- 반복 exchange는 같은 enrollmentId를 반환
- finalize는 `PENDING && 미만료` 조건의 CAS 승자만 수행
- User·identity·Session 저장과 attempt consume은 Mongo Transaction 하나로 처리

만료 attempt는 먼저 원자적으로 EXPIRED로 전환한 뒤 다음 attempt를 허용한다. duplicate insert loser는 저장된 winner를 다시 조회한다.

## Guest merge proof

여러 기기 Guest merge는 다음 두 proof를 동시에 요구한다.

- source: 현재 Identity JWT `sub`가 가리키는 ACTIVE GUEST
- target: 기존 SNS owner Firebase User의 fresh Firebase ID Token과 FirebaseIdentity mapping이 가리키는 ACTIVE MEMBER

email·phone·닉네임은 target 추정에 사용하지 않는다. source와 target이 같거나 source가 MEMBER이고, target이 ACTIVE MEMBER가 아니면 자동 merge하지 않는다. Stage 7 전에는 mutation 없이 `MERGE_REQUIRED`만 반환한다.

## 가입 중단 cleanup

Firebase에 phone까지 연결했지만 내부 MEMBER enrollment를 완료하지 않은 User는 번호를 점유할 수 있다. 자동 cleanup은 다음 조건을 모두 만족할 때만 허용한다.

- FirebaseIdentity mapping이 없음
- 유효한 PENDING enrollment가 없음
- 마지막 enrollment 활동 후 24시간의 resume 유예가 지남
- cleanup lease 획득 뒤 조건을 다시 확인함

조건을 만족하면 Firebase refresh token을 revoke하고 Firebase User를 삭제한다. 삭제 성공 뒤 번호가 해제됐는지 확인한다. 단일 phone unlink만 수행해 정상 primary account를 훼손하지 않는다. 실패는 retry·reconciliation 대상으로 유지하며 조건이 불확실하면 삭제하지 않는다.

## Lifecycle 순서

| 동작 | 내부 처리 | Firebase/Provider 처리 | 실패 정책 |
| --- | --- | --- | --- |
| 일반 logout | 한 RefreshSession 폐기 | client sign-out | 서로 독립, 기존 Access Token은 TTL까지 유효 |
| logout-all | 내부 Session 전체 폐기 | Firebase refresh token revoke 요청 | 내부 폐기는 유지, 외부 실패 retry |
| provider link | fresh Token·recent-auth 확인 후 mapping sync | Firebase link가 선행 | 내부 sync 실패 시 login/link 차단 후 reconciliation |
| provider unlink | 마지막 primary 수단 보호 | Firebase unlink가 선행 | 내부 stale mapping을 사용하지 않고 reconciliation |
| withdrawal | User tombstone·Session 폐기·lifecycle outbox commit | revoke, Apple 요구 처리, Firebase delete | 외부 실패가 내부 탈퇴를 되돌리지 않으며 retry |
| abandoned enrollment | 내부 mapping·active attempt 부재 확인 | revoke 후 Firebase User delete | 불확실하면 fail-closed로 보존 |

Apple authorization revoke에는 실제 Apple credential과 lifecycle 검증이 필요하다. 필요한 revoke material 확보·단기 보관·Firebase delete 순서는 Apple 실프로젝트 PoC 전까지 production gate로 남긴다.

## 장애와 vendor exit

Firebase 장애 중 신규 로그인·가입·link·merge·탈퇴 재인증은 fail-closed다. 기존 ACTIVE User의 유효한 Identity RefreshSession reissue와 Learning Core의 Identity JWT 검증에는 Firebase 호출을 추가하지 않는다. 따라서 Firebase에서 발생한 disable/revoke가 내부 Session에 전파되기까지 지연될 수 있으며, high-risk lifecycle과 운영 revoke job이 내부 Session도 함께 폐기해야 한다.

Provider별 kill switch를 두고 Firebase 전체 기능 flag와 분리한다. vendor 교체 시에도 canonical `userId`와 downstream 데이터는 유지한다. FirebaseIdentity는 broker mapping으로만 취급하고 새 broker의 identity mapping을 추가한 뒤 단계적으로 전환한다. email/password credential export를 전제로 하지 않으며 필요하면 비밀번호 재설정 절차를 사용한다.

## Runtime credential과 설정

production Admin SDK는 런타임 Workload Identity 또는 Application Default Credentials를 우선 사용한다. service account 파일이 불가피하면 배포 Secret을 read-only mount하고 저장소나 image에 포함하지 않는다.

설정 이름만 다음 계약으로 예약한다.

```text
FIREBASE_AUTH_ENABLED
FIREBASE_PROJECT_ID
FIREBASE_TENANT_ID
FIREBASE_GOOGLE_ENABLED
FIREBASE_APPLE_ENABLED
FIREBASE_KAKAO_ENABLED
FIREBASE_PHONE_ENABLED
```

local/test 기본값과 production 초기값은 모두 disabled다. Stage 3 adapter·설정 검증과 feature flag가 구현되기 전 어떤 공개 Firebase endpoint도 추가하지 않는다.

## 결과와 trade-off

장점:

- Password·Google·Apple·phone credential 보안 구현을 Firebase에 위임한다.
- 내부 UUID와 자체 JWT 계약을 유지해 Learning Core와 사용자 데이터가 vendor UID에 결합되지 않는다.
- Guest 승격·merge와 전화번호당 가입·무료혜택 정책을 Identity가 통제한다.

비용:

- Firebase User와 내부 User의 이중 lifecycle·reconciliation이 필요하다.
- Firebase 장애·quota·가격·정책 변경이 신규 인증에 영향을 준다.
- Kakao Generic OIDC와 Apple revoke는 별도 실제 환경 검증이 필요하다.
- 회원가입 필수 phone 인증은 SMS 비용과 가입 전환율 저하를 만든다.

## production 활성화 gate

다음 항목이 모두 확인되기 전 `FIREBASE_AUTH_ENABLED`와 Provider flag를 production에서 켜지 않는다.

- 격리 Firebase project의 Android/iOS email·Google·Apple 실제 인증과 redirect
- 같은 current Firebase User에 대한 공식 client SDK phone link와 UID 유지
- 국내 SMS 도달률, 발신 규제, 허용 국가, quota, App Check/reCAPTCHA와 비용 alert
- Identity Platform 업그레이드·billing 승인
- Kakao Generic OIDC provider 등록, stable providerData UID와 모바일 deep-link 복귀
- Apple nonce·authorization revoke·account deletion sequence
- Firebase timeout·quota의 안정적인 429/503 mapping
- lifecycle reconciliation job과 provider kill switch
- 실제 운영 User 0건 또는 허용된 테스트 데이터만 존재함을 read-only 확인
- Stage 3·4 index와 Transaction 통합 테스트

## 후속 작업 범위

Stage 3 Firebase broker foundation:

- Admin SDK adapter interface와 disabled-by-default 설정
- 목적별 Token 검증·recent-auth·error mapping
- `FirebaseIdentity`와 `(firebaseProjectId, firebaseUid)` unique index
- `FirebaseEnrollmentAttempt`, application TTL·CAS·PENDING partial unique
- 외부 인프라를 호출하지 않는 mock 테스트

Stage 4 PhoneIdentity:

- E.164 정규화
- versioned domain-separated HMAC key registry
- `PhoneIdentity`와 `PhoneFingerprintAlias`
- 한 검증 번호의 ACTIVE MEMBER 귀속 하나 보장
- rotation·legacy key candidate·index 테스트
