# 프론트 Firebase·SNS 연동 부록 — 배포·QA·구버전 참고

- 기준일: 2026-09-16
- 주 문서: [프론트 API 명세](frontend-firebase-auth-integration-guide.md)
- API 요청·응답과 앱 처리 규칙은 주 문서를 먼저 읽는다. 이 문서는 배포 확인, QA, 구버전 참고 자료다.

## 5줄 결론

1. 코드가 구현돼 있어도 해당 환경에서 활성화·검증됐다는 뜻은 아니다.
2. 서버 주소·지원 SNS·약관 버전·업데이트 기준을 출시 전에 확정한다.
3. Google과 같은 Firebase UID의 phone link부터 검증하고 실제 Provider 검증을 이어간다.
4. 응답 유실·중단·동시 요청 등 실패 사례도 QA에 포함한다.
5. 구버전 Guest·이메일 예시는 호환 참고이며 신규 앱에서 사용하는 기능이 아니다.

<a id="deployment"></a>

## 출시 전 확인사항

### 제공 범위와 미확정 항목

| 구분 | 현재 기준 |
| --- | --- |
| 신규 회원 | SNS → Firebase exchange → same-UID phone link → signup. 임시 Guest 생성 없음 |
| 기존 Guest | 기존 Identity Token을 유지하고 prepare → upgrade 또는 merge |
| 신규 Guest | 신규 앱에서는 제공하지 않음. 서버 `/auth/guest`는 코드에 남아 있으며 이번 문서 작업에서 차단하지 않음 |
| Guest 최초 응답 복구 | 미구현·취소. 새 복구 헤더/API를 구현하지 않음 |
| SNS 추가/재연결 | link prepare/start/complete/status 공통 흐름. sync로 신규 연결 금지 |
| 전화번호 변경 | 셀프 변경 및 번호 재할당 예외 변경 보류 |
| 기존 이메일 회원 이전 | 대상 없음. 신규 이메일 로그인 화면 추가도 이번 인계 범위 아님 |
| 공개 정책·capability API | 현재 없음. 아래 공급 방식 확정 전 존재를 가정하지 않음 |

출시 담당자가 확정해 프론트에 전달할 값: 환경별 Identity/Learning Core base URL, Firebase project/app 설정, 실제 지원 Provider, 약관 URL·현재 버전, 최소 지원 앱 버전·업데이트 링크, 활성 feature 목록. 앱 빌드 설정 또는 합의된 원격 설정으로 공급할 수 있으나 현재 서버에 그 공급 API가 있다고 가정하지 않는다. 이 문서의 `privacy-v1` 등은 예시이며 배포 서버 설정과 일치해야 한다.

아직 미확인인 항목은 실제 모바일 Google/Apple/Kakao redirect, phone link/SMS, Mongo Transaction, 비동기 Billing/Learning Core 전달, 키 공급/회전이다. 모든 기능은 해당 환경의 검증된 범위만 노출한다.

## 기능별 활성화와 QA 인계

### 서버 구현과 배포 설정 구분

아래 설정은 백엔드 담당자 확인용이며 앱이 환경변수를 직접 읽거나 변경하는 계약이 아니다. 표의 대표 flag만 켜면 충분하다는 뜻도 아니다. 키·Transaction·consumer·worker 등의 의존 조건을 함께 확인한다.

| 기능 | 대표 서버 설정 (저장소 기본 OFF) | 앱 노출 조건 |
| --- | --- | --- |
| Firebase 인증 | `FIREBASE_AUTH_ENABLED`, Provider별 `FIREBASE_GOOGLE_ENABLED`/`FIREBASE_APPLE_ENABLED`/`FIREBASE_KAKAO_ENABLED`/`FIREBASE_PHONE_ENABLED` | 프로젝트·SDK·Provider·가입 phone 설정 검증 |
| Guest merge | `GUEST_MERGE_ENABLED` | 기존 Guest 보호와 Billing/Learning Core 전달·이전 검증 |
| 안전한 세션 처리 | `AUTH_SESSION_FENCE_ENABLED` | epoch·회원 전환·로그아웃 경합 검증 |
| 재발급 응답 복구 | `AUTH_REISSUE_RECOVERY_ENABLED` | 암호화 keyring·Mongo·모바일 pending 복구 검증 |
| 전체 로그아웃 Firebase revoke | `FIREBASE_LOGOUT_ALL_CAPTURE_ENABLED`, `FIREBASE_LOGOUT_ALL_WORKER_ENABLED` | remote revoke·지연 처리·운영 대응 검증 |
| 탈퇴/정리 | `FIREBASE_WITHDRAWAL_ENABLED`, `FIREBASE_WITHDRAWAL_CLEANUP_ENABLED`, `FIREBASE_WITHDRAWAL_IDENTITY_RELEASE_ENABLED` | 실제 외부 삭제·내부 release·재가입 검증 |
| SNS 연결 변경 | `FIREBASE_PROVIDER_CHANGE_FENCE_ENABLED`, `FIREBASE_PROVIDER_LINK_ENABLED`, `FIREBASE_PROVIDER_UNLINK_ENABLED`, `FIREBASE_PROVIDER_UNLINK_WORKER_ENABLED` | link/unlink 상태·중단 복구·실제 Provider 검증 |

전화번호 셀프 변경, 신규 Guest 응답 복구, 기존 LOCAL migration은 이번 출시 필수 구현으로 요구하지 않는다. 같은 Firebase UID의 Provider 연결과 UID 자체의 교체는 다르며 공개 rebind는 제공하지 않는다.

<a id="integration"></a>

### 첫 통합 검증 순서

1. 테스트 Firebase 프로젝트·Android/iOS 앱 연결, Google 로그인 확인.
2. `/exchange`에서 신규 사용자의 `ENROLLMENT_REQUIRED` 확인.
3. 테스트 전화번호 credential을 같은 Firebase User에 link, UID 유지 및 ID Token 강제 갱신.
4. `/signup` 성공 및 `/users/me`의 MEMBER/userId 확인. 재로그인은 `AUTHENTICATED`여야 함.
5. 기존 Guest Token을 가진 사용자로 승격/병합 확인. 신규 Guest 생성은 새 앱의 흐름에 넣지 않음.
6. 재발급 응답 유실·동시 요청·로그아웃 및 다른 기기의 재발급 거절 확인.
7. SNS 연결/해제 및 앱 중단, 탈퇴·cleanup·동일 SNS 재가입 확인.
8. Apple/Kakao·실제 SMS·서비스 간 비동기 연동을 추가 확인.

Emulator/테스트 번호 통과는 실제 OAuth redirect·국내 SMS·과금/쿼터 검증을 대신하지 않는다. 운영 확인 결과는 이 문서의 구현 사실과 별도로 기록한다.

### 꼭 재현할 실패 사례

| 상황 | 기대 동작 |
| --- | --- |
| Firebase 기능 OFF | 503을 신규 회원 없음으로 오해하지 않음 |
| phone link 중 UID 변경 | 가입 중단, 다른 Firebase 계정에 signup하지 않음 |
| signup/upgrade/merge 응답 유실 | 현재 Firebase/Identity 상태 조회, 임의 새 계정 생성 금지 |
| Guest prepare ALREADY_LINKED + 여전히 GUEST | 완료 표시 금지, 미구현 복구 API 호출 금지, 지원 안내 |
| 같은 refresh 요청 응답 유실 | Stage 9 활성 환경에서 같은 요청 ID+원 Token으로만 재시도 |
| link start 응답 유실 | SDK link 실행 허가가 없으면 실행하지 않음 |
| SDK link 후 complete 유실 | 같은 linkAttemptId로 완료 확인, 새 prepare 금지 |
| unlink 접수 후 앱 종료 | 보존한 requestId+남는 SNS 재인증으로 상태 조회 |
| 탈퇴 정리 중 즉시 재가입 | 정리 중 안내, phone/SNS 충돌 우회 금지 |
| 오래된 Guest 응답이 새 로그인 뒤 도착 | 현재 인증 상태를 덮어쓰지 않음 |

<a id="qa"></a>

## 프론트 구현 체크리스트

- [ ] Firebase ID Token과 Identity Access/Refresh Token을 타입 수준에서 분리한다.
- [ ] Identity Access Token만 Learning Core Bearer로 보낸다.
- [ ] Refresh Token을 OS 보안 저장소에 보관한다.
- [ ] reissue를 앱 전체 single-flight로 만들고 원 요청은 한 번만 재시도한다.
- [ ] Stage 9 활성 환경에서 원 Refresh Token과 요청 ID를 안전하게 저장한 뒤 보내고, 응답 유실 재시도에 같은 값을 사용한다.
- [ ] signup/upgrade 전 같은 Firebase UID에 phone credential을 link한다.
- [ ] phone link 전후 UID를 비교하고 link 후 ID Token을 강제 갱신한다.
- [ ] `AUTHENTICATED`, `ENROLLMENT_REQUIRED`를 `result.type`으로 분기한다.
- [ ] `missingRequirements`와 `linkedProviders` 배열 순서에 의존하지 않는다.
- [ ] 신규 signup enrollment는 `/exchange`, 기존 Guest 전환 enrollment는 `/guest/prepare`에서 얻는다. 두 binding을 혼용하지 않는다.
- [ ] Guest `MERGE_REQUIRED`에서 사용자 확인을 받는다.
- [ ] Guest `ALREADY_LINKED`를 MEMBER 로그인 성공으로 취급하지 않는다.
- [ ] logout·withdrawal terminal 처리에서 Firebase signOut 실패와 로컬 Token 삭제를 분리한다.
- [ ] Token·OTP·비밀번호·phone을 log, analytics, crash report에서 제거한다.
- [ ] 신규 가입의 현재 약관 URL/버전 공급을 배포 담당자와 확정한다. 인증 후 동의 조회는 `/users/me/consents`를 사용한다.
- [ ] 환경별 Firebase project, Provider button, backend feature flag를 함께 배포한다.
- [ ] Guest merge는 Billing·Learning Core consumer staging E2E 뒤에만 노출한다.
- [ ] 업데이트 후 기존 Guest Token을 삭제하지 않고, 신규 Guest API를 호출하는 초기화 코드를 제거한다.
- [ ] 최소 지원 버전·업데이트 URL·차단/유도 정책을 확정한다.
- [ ] 최초 연결/재연결 모두 공통 link를 사용하고 start 허가와 상태 조회를 구분한다.
- [ ] unlink 접수 후 원 requestId를 보존하여 자체 Token 없이 남는 Firebase proof로 상태를 조회한다.
- [ ] reissue replay의 절대 만료 헤더를 사용하고 늦은 응답이 최신 로그인/로그아웃 결과를 덮어쓰지 않게 한다.

<a id="legacy"></a>

## 구버전·이메일 참고

### 이메일·비밀번호 경로 — 이번 신규 UI 범위 밖

서버에는 Firebase PASSWORD 및 legacy LOCAL 경로가 있으나 현재 제품의 신규 SNS 인계에 이메일 화면을 추가하지 않는다. 기존 이메일 회원이 없으므로 migration은 대상이 없다. 아래는 서버 호환 경로 설명이며 제품 지원을 의미하지 않는다.

```text
Firebase email/password 로그인 또는 가입
→ 신규 가입이면 email verification
→ Firebase ID Token
→ /api/v1/auth/firebase/exchange
→ 기존 MEMBER는 AUTHENTICATED
→ 신규 MEMBER는 same-UID phone link 후 /api/v1/auth/firebase/signup
```

프론트는 legacy `/api/v1/auth/check-email`, `/api/v1/auth/signup`, `/api/v1/auth/login`을 신규 인증 화면에서 사용하지 않는다. 다른 Firebase UID를 기존 ACTIVE 계정에 자동 rebind하지 않는다.

### `POST /api/v1/auth/guest` — 구버전 참고, 신규 앱 호출 금지

인증: 공개

요청:

```json
{
  "installationId": "550e8400-e29b-41d4-a716-446655440000",
  "isPrivacyConsented": true,
  "privacyConsentVersion": "privacy-v1",
  "isTermConsented": true,
  "termConsentVersion": "term-v1",
  "isQualityReviewConsented": false,
  "qualityReviewConsentVersion": null
}
```

성공:

```json
{
  "isSuccess": true,
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "result": {
    "accessToken": "<identity-access-token>",
    "refreshToken": "<identity-refresh-token>",
    "grantType": "Bearer",
    "accessTokenExpiresIn": 1800000,
    "refreshTokenExpiresIn": 1209600000
  }
}
```

서버의 현재 동작은 같은 installationId 재요청 시 `409 GUEST_ALREADY_EXISTS`다. 최초 응답 복구는 구현하지 않았으며 취소한 Stage 11의 헤더·만료 오류·암호화 응답은 제공되지 않는다. installationId는 인증 credential이 아니다. 이 예시는 구버전 동작 확인용으로만 보존한다.

## SNS 공통 연결 변경 참고

### TMI-131 추가 연동

SNS 해제·재연결은 [전용 API/응답·모바일 흐름·오류 계약](firebase-provider-unlink-stage-10-runbook.md#51-api와-응답)을 따른다. 신규 URL은 `/api/v1/auth/firebase/providers/unlink`, `/unlink/status`, `/link/prepare`, `/link/start`, `/link/complete`, `/link/status`다. 기존 `/relink/prepare`는 폐기되며 sync에는 새 연결을 저장하지 않는다. 기능 기본 OFF이며 지금 배포돼 있다는 뜻이 아니다.

- 해제는 남는 Google·Apple·Kakao로 재인증하고 사용자 Access Token + `Idempotency-Key`로 접수한다. 202 후 현재 기기도 자체 Token 삭제·Firebase signOut한다.
- 응답 유실/로그아웃 후에는 같은 requestId와 남는 SNS의 fresh Firebase 증거로 status를 조회한다. 자체 Token은 없어도 되지만 무인증 조회는 아니다. 404를 해제 재실행 허가로 해석하지 않는다.
- 처음 연결하든 다시 연결하든 prepare → start → 같은 Firebase User에 명시적 link → 대상 SNS 재인증 → complete를 사용한다. 프론트는 해제 이력을 판단하지 않는다.
- 앱은 최초 start 응답의 linkAllowed=true일 때만 SDK link를 한 번 실행한다. 대상 재인증의 auth_time은 start 이후여야 하므로 Firebase 초 단위 경계를 넘겨야 한다.
- **PREPARED 중단은 만료 후 새 준비 가능, STARTED 결과 불명은 자동 해제 불가**다. start 응답 유실 시 SDK를 재실행하지 말고 status·현재 Firebase 연결/이전 호출 종료를 확인한다. [5분과 중단 복구 계약](firebase-provider-unlink-stage-10-runbook.md#22-공통-연결의-5분과-앱-중단)을 UI·QA와 함께 적용한다.
- 전화번호 셀프 변경·번호 재할당 예외 처리, SNS 회사 계정 삭제, 시험 기록/무료권 변경 기능은 추가하지 않았다.

<a id="sources"></a>

## 코드·계약 근거

- Firebase API: `src/main/java/web/tosunsaeng/identity/domain/auth/federation/api/FirebaseExchangeController.java`
- Firebase request·response DTO: `src/main/java/web/tosunsaeng/identity/domain/auth/federation/dto`
- LOCAL·Guest·Session API: `src/main/java/web/tosunsaeng/identity/domain/auth/common/api/AuthController.java`
- 사용자 조회·탈퇴 API: `src/main/java/web/tosunsaeng/identity/domain/user/api/UserController.java`
- Firebase 검증과 Provider mapping: `src/main/java/web/tosunsaeng/identity/domain/auth/federation/infrastructure/firebase/FirebaseAdminAuthenticationVerifier.java`
- 오류 code와 HTTP status: `src/main/java/web/tosunsaeng/identity/domain/auth/common/exception/AuthErrorStatus.java`
- 기본 설정: `src/main/resources/application.yml`
- Firebase broker ADR: `docs/adr/ADR-001-firebase-authentication-broker.md`
- 전체 SNS 구현 계약: `docs/contracts/social-login-implementation-plan.md`
- 탈퇴 모바일 UX: `docs/contracts/withdrawal-session-mobile-ux-stage-4-plan.md`

추가 확인 근거:

- [SNS 연결 Controller](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderLinkController.java), [해제 Controller](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderChangeController.java)
- [연결 상태·허가](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderLinkService.java), [해제 상태·멱등 키](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderChangeService.java)
- [Guest prepare ALREADY_LINKED](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseGuestPrepareService.java)
- [정책 상태 DTO](../../src/main/java/web/tosunsaeng/identity/domain/user/dto/response/UserConsentStatusResponse.java), [개별 정책 DTO](../../src/main/java/web/tosunsaeng/identity/domain/user/dto/response/ConsentPolicyStatusResponse.java), [동의 변경 DTO](../../src/main/java/web/tosunsaeng/identity/domain/user/dto/request/UserConsentUpdateRequest.java)
- [재발급 기능 분기](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/TokenReissueService.java), [탈퇴 cleanup enum](../../src/main/java/web/tosunsaeng/identity/domain/user/domain/enums/UserWithdrawalCleanupStatus.java)
- [보호/공개 route 설정](../../src/main/java/web/tosunsaeng/identity/global/config/SecurityConfig.java)

이 명세 갱신에서는 서버 코드를 변경하지 않았다. 예시 값은 가짜 데이터·placeholder이며 실제 Token·credential을 문서에 붙여 넣지 않는다. 무료 사용권 조회·시험·결과 API는 해당 서비스의 별도 명세를 따른다.
