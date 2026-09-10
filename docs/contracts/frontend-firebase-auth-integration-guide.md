# 프론트엔드 Firebase·SNS 로그인 및 회원 전환 연동 가이드

- 문서 성격: 모든 인증 후속 단계와 staging E2E가 완료된 시점의 최종 프론트 계약
- 기준일: 2026-09-09 (TMI-130 재발급 계약 반영)
- 대상: 모바일·프론트엔드 개발자, QA, 제품 담당자
- 적용 조건: Identity·Firebase·Billing·Learning Core 배포 및 Provider별 운영 설정이 모두 완료된 release

## 1. 5줄 결론

1. 앱은 Google·Apple·Kakao 인증을 Firebase Auth로 수행하고, Firebase ID Token을 Identity API에 교환한 뒤 Identity Access Token만 Learning Core에 보낸다.
2. 기존 MEMBER는 `Firebase 로그인 → /firebase/exchange → AUTHENTICATED`, 신규 사용자는 `ENROLLMENT_REQUIRED → 같은 Firebase User에 phone link → /firebase/signup` 순서다.
3. phone credential은 별도 로그인으로 사용하지 않고 현재 Firebase User에 `linkWithCredential`해야 하며, link 후 강제 갱신한 ID Token을 제출해야 한다.
4. Identity Refresh Token은 rotation되므로 재발급을 single-flight로 처리하고, 성공 시 Access/Refresh Token을 함께 교체해야 한다.
5. 프론트는 서버가 제공하는 환경별 인증 capability에 따라 로그인 버튼을 노출하며, 활성화된 기능은 Firebase·Identity·Billing·Learning Core까지 종단 동작이 보장된 것으로 취급한다.

## 2. 최종 출시 계약의 전제

### 2.1 Kakao도 동일한 Firebase 교환 흐름을 사용한다

Google·Apple뿐 아니라 Kakao도 승인된 Firebase Generic OIDC provider를 통해 같은 흐름을 사용한다.

| 로그인 수단 | Firebase provider ID |
| --- | --- |
| Google | `google.com` |
| Apple | `apple.com` |
| Kakao | `oidc.kakao` 기본값 |
| Phone | `phone` — 가입 proof이며 단독 로그인 수단이 아님 |

Kakao는 Firebase 기본 provider가 아니다. 최종 release에서는 Kakao Developers, Identity Platform Generic OIDC, Billing, 모바일 redirect/deep-link 설정이 모두 완료돼 있다.

### 2.2 `ALREADY_LINKED`는 Token 발급 성공이 아니다

Guest prepare의 `ALREADY_LINKED`만 보고 정상 앱 화면으로 진입하면 안 된다. 이 응답에는 Identity Token이나 enrollmentId가 없으며, 호출 시점의 서버 User는 여전히 Guest다.

프론트는 다음처럼 처리한다.

1. `/api/v1/users/me`로 현재 계정 상태를 다시 확인한다.
2. 여전히 `accountType=GUEST`이면 `/guest/upgrade`나 `/guest/merge`를 임의 호출하지 않는다.
3. 최종 Guest 복구 계약에 따라 같은 논리 작업을 재개한다.
4. 복구 결과로 MEMBER Token 또는 새 enrollment를 받기 전에는 MEMBER 화면으로 진입하지 않는다.

최종 서버는 여전히 Guest인 사용자에게 다음 행동이 없는 bare `ALREADY_LINKED` 상태를 남기지 않는다. 이 조건은 Guest 응답 유실 복구 테스트로 보장한다.

### 2.3 `ACCOUNT_MERGED_TOKEN_REJECTED`는 `/reissue`의 대표 오류가 아니다

이 코드는 MERGED된 Guest의 예전 **Access Token**으로 보호 API를 호출할 때 발생할 수 있다. 예전 Guest Refresh Token은 보통 `INVALID_REFRESH_TOKEN`으로 거절된다. 따라서 앱 시작의 `/reissue` 오류 목록에 이 코드를 고정해서 기대하지 않는다.

### 2.4 recent-auth 오류는 Token 강제 갱신만으로 해결되지 않는다

`getIdToken(forceRefresh=true)`는 Firebase ID Token 내용을 새로 받지만 `auth_time` 자체를 갱신하지 않을 수 있다. `FIREBASE_RECENT_AUTH_REQUIRED`이면 현재 Firebase 사용자에게 Provider credential을 다시 제시하는 명시적 재인증을 수행한 뒤 새 ID Token을 받아야 한다.

### 2.5 `logout-all`은 Identity와 Firebase 전체 세션을 함께 종료한다

최종 서버는 모든 Identity RefreshSession을 폐기하고 Firebase refresh token revoke 작업을 durable하게 인계한다. 현재 기기는 Firebase SDK `signOut`과 로컬 Token 삭제를 수행한다. 다른 기기는 Identity 재발급이 즉시 차단되고 Firebase Token도 revoke 반영 후 다시 사용할 수 없다.

### 2.6 회원가입 정책 버전은 서버 기준으로 공급한다

`/firebase/signup`, `/guest/upgrade`, `/auth/guest`는 개인정보 처리방침·이용약관 version을 요청에 요구한다. 최종 앱은 인증 전 사용 가능한 서버 정책 metadata에서 현재 version을 가져온다. 앱에 임의 version을 작성하거나 이전 version으로 가입을 우회하지 않는다.

### 2.7 문서에 없는 변경 API를 추정하지 않는다

Provider unlink와 전화번호 변경은 fresh proof, 마지막 로그인 수단 보호, Firebase mutation과 Identity reconciliation을 갖춘 별도 공개 계약을 사용한다. 이 로그인 가이드에 URL이 없다는 이유로 `/auth-methods/sync`를 unlink·phone 변경 용도로 재사용하거나 프론트가 Firebase 상태만 변경하면 안 되며, 최종 전용 API 문서를 따른다.

## 3. 프론트가 반드시 지켜야 하는 인증 경계

### 3.1 Token의 역할

| Token | 사용처 | 프론트 처리 |
| --- | --- | --- |
| Firebase ID Token | Identity의 Firebase exchange·signup·Guest 전환·인증수단 sync·Firebase 회원탈퇴 proof | Firebase SDK에서 필요 시 새로 받고 장기 세션 Token처럼 직접 관리하지 않음 |
| Identity Access Token | Identity 보호 API, Learning Core 등 사용자 API | `Authorization: Bearer <identity-access-token>` |
| Identity Refresh Token | Identity `/reissue`, `/logout`, `/users/withdraw` | OS 보안 저장소에 저장하고 일반 API에는 전송하지 않음 |

- Firebase ID Token을 Learning Core에 보내지 않는다.
- Identity Access Token을 `/firebase/exchange`의 `firebaseIdToken`으로 보내지 않는다.
- Request Body·Path·Query에 클라이언트가 선택한 `userId`를 추가하지 않는다. 서버가 검증한 Identity JWT `sub`를 사용한다.
- Token, OTP, Firebase credential, 비밀번호, 전화번호를 log·analytics·crash report에 기록하지 않는다.
- 응답은 HTTP status와 함께 `isSuccess`, `code`, `result`를 확인한다.
- 만료시간 필드는 모두 밀리초다.

### 3.2 공통 응답 envelope

성공:

```json
{
  "isSuccess": true,
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "result": {}
}
```

실패:

```json
{
  "isSuccess": false,
  "code": "FIREBASE_UNAVAILABLE",
  "message": "Firebase 인증을 일시적으로 사용할 수 없습니다.",
  "result": null
}
```

입력 검증 실패의 `result`는 필드 오류 배열일 수 있다. 민감한 필드의 `rejectedValue`는 `null`로 정제된다.

## 4. 로그인·회원 전환 흐름

### 4.1 기존 Google·Apple·Kakao MEMBER 로그인

```text
Firebase SDK로 Provider 로그인 또는 재인증
→ Firebase ID Token 획득
→ POST /api/v1/auth/firebase/exchange
→ result.type 분기
```

| `result.type` | 의미 | 프론트 동작 |
| --- | --- | --- |
| `AUTHENTICATED` | 기존 ACTIVE MEMBER | Identity Access/Refresh Token을 안전하게 저장하고 앱 진입 |
| `ENROLLMENT_REQUIRED` | 아직 내부 MEMBER가 없음 | `enrollmentId`, `expiresIn`, `missingRequirements`를 보관하고 가입 진행 |

- 기존 MEMBER는 매 로그인마다 phone OTP를 반복하지 않는다.
- phone-only Firebase sign-in은 Identity 로그인으로 허용하지 않는다.
- `FIREBASE_RECENT_AUTH_REQUIRED`이면 ID Token 강제 갱신만 반복하지 말고 Provider 재인증을 수행한다.

### 4.2 신규 Google·Apple·Kakao MEMBER 가입

`ENROLLMENT_REQUIRED`를 받은 뒤 다음 순서를 지킨다.

```text
필요한 email verification 완료
→ 닉네임·필수 약관 입력
→ Firebase Phone Auth로 PhoneAuthCredential 획득
→ 현재 Firebase User에 phone credential link
→ link 전후 Firebase UID 동일성 확인
→ Firebase ID Token 강제 갱신
→ POST /api/v1/auth/firebase/signup
```

- enrollment 기본 유효시간은 10분이다. 실제 응답의 `expiresIn`을 기준으로 만료 UI를 구성한다.
- `nickname`은 로그인 Provider에서 서버가 가져오는 값이 아니다. `ENROLLMENT_REQUIRED` 이후 가입 화면에서 사용자가 직접 입력·확정한 값을 보낸다.
- Google·Apple·Kakao profile의 표시 이름을 앱이 초기값으로 제안할 수는 있지만 사용자가 확인·수정하게 하며, Firebase ID Token만으로 Identity nickname이 자동 저장된다고 가정하지 않는다.
- signup·Guest upgrade는 기본 5분 recent-auth 제한을 사용한다.
- `missingRequirements`에 `EMAIL_VERIFICATION`이 있으면 password Firebase 계정의 email 인증을 먼저 완료한다.
- `PROFILE`, `CONSENTS`는 가입 request 필드로 충족한다.
- `PHONE_VERIFICATION`은 별도 Identity OTP API가 아니라 같은 Firebase User에 phone을 link해 충족한다.
- `PhoneAuthCredential`로 `signInWithCredential` 또는 phone 직접 로그인을 해 별도 Firebase User를 만들면 안 된다.
- link 후 UID가 바뀌면 가입을 중단하고 Firebase 상태를 정리한다.
- 전화번호 충돌은 자동 merge 근거가 아니다.

### 4.3 Guest 최초 이용

앱은 UUID v4 `installationId`와 현재 정책 동의를 `/api/v1/auth/guest`에 보낸다.

- `installationId`는 중복 방지 값이지 인증 credential이 아니다.
- 같은 installationId를 새로운 Guest를 계속 만드는 수단으로 사용하지 않는다.
- 생성 성공 응답이 유실되면 최종 Guest 복구 계약으로 동일한 Guest와 Session을 복구한다. 임의로 새 installationId를 발급해 새 User를 만들지 않는다.
- Guest Access/Refresh Token도 MEMBER와 같은 보안 저장·재발급 규칙을 사용한다.

### 4.4 Guest를 새 MEMBER로 승격

```text
Guest Identity Access Token 유지
→ Firebase Provider 로그인
→ POST /api/v1/auth/firebase/guest/prepare
→ prepare 결과 분기
```

| prepare 결과 | 프론트 동작 |
| --- | --- |
| `ENROLLMENT_REQUIRED` | 같은 Firebase User에 phone을 link하고 강제 갱신 Token으로 `/guest/upgrade` 호출 |
| `ALREADY_LINKED` | 정상 완료로 간주하지 말고 2.2의 상태 재확인·reconciliation 처리 |
| `MERGE_REQUIRED` | 신규 승격을 중단하고 사용자 확인 후 기존 MEMBER merge 흐름으로 이동 |

`/guest/upgrade` 성공 시:

- Guest의 canonical userId는 유지된다.
- 기존 Guest RefreshSession은 폐기된다.
- 응답의 새 MEMBER Access/Refresh Token으로 로컬 값을 모두 교체한다.

### 4.5 Guest를 기존 MEMBER로 통합

`MERGE_REQUIRED`이면 “현재 Guest에서 만든 데이터가 기존 계정으로 이동하며 현재 Guest 세션은 종료된다”는 확인을 받은 뒤 `/api/v1/auth/firebase/guest/merge`를 호출한다.

- Header에는 현재 Guest Identity Access Token을 보낸다.
- Body에는 기존 MEMBER로 인증된 fresh Firebase ID Token을 보낸다.
- 성공하면 target MEMBER Identity Token으로 로컬 Token을 모두 교체한다.
- 이메일·phone·닉네임만 보고 프론트가 target User를 추정하지 않는다.
- 이전 Guest Access Token의 보호 API 호출은 `ACCOUNT_MERGED_TOKEN_REJECTED`가 될 수 있다.
- Billing·Learning Core의 `UserMerged` consumer와 종단 이전 검증이 끝나기 전 UI를 활성화하지 않는다.

### 4.6 기존 MEMBER에 SNS 인증수단 추가

```text
현재 Firebase User에 새 Provider credential link
→ Firebase ID Token 강제 갱신
→ POST /api/v1/auth/firebase/auth-methods/sync
```

- Identity Access Token과 Firebase ID Token을 동시에 제출한다.
- 응답의 `linkedProviders`를 서버 기준 연결 상태로 사용한다.
- 다른 내부 User가 같은 Provider subject를 소유하면 자동 이전하지 않는다.
- Provider unlink와 phone 변경은 각각의 전용 API를 사용하며 `/auth-methods/sync`는 연결 추가·동기화에만 사용한다.
- “SNS 하나만 로그아웃”은 계정 연결 해제와 다른 개념이다. Firebase `signOut`은 현재 Firebase 세션 전체를 종료하며, 특정 SNS를 더 이상 로그인 수단으로 사용하지 않으려면 Provider unlink 계약을 사용한다.

### 4.7 이메일·비밀번호 로그인

최종 release에서는 이메일·비밀번호 credential도 Firebase가 검증한다.

```text
Firebase email/password 로그인 또는 가입
→ 신규 가입이면 email verification
→ Firebase ID Token
→ /api/v1/auth/firebase/exchange
→ 기존 MEMBER는 AUTHENTICATED
→ 신규 MEMBER는 same-UID phone link 후 /api/v1/auth/firebase/signup
```

프론트는 legacy `/api/v1/auth/check-email`, `/api/v1/auth/signup`, `/api/v1/auth/login`을 신규 인증 화면에서 사용하지 않는다. 기존 LOCAL 회원 migration은 별도의 승인된 rebind·password reset 절차를 따른다.

## 5. Token 재발급과 로그아웃

### 5.1 앱 시작과 401 복구

1. 저장된 Identity Refresh Token이 없으면 비로그인 상태로 시작한다.
2. 있으면 `/api/v1/auth/reissue`를 호출한다.
3. 성공하면 새 Access/Refresh Token을 한 번의 로컬 상태 전환으로 교체한다.
4. 보호 API 401은 앱 전체 single-flight reissue 한 번으로 처리하고, 재발급이 명확히 성공한 경우에만 원 요청을 한 번 재시도한다.
5. reissue 401이면 반복하지 않고 terminal signed-out 상태로 전환한다.

최종 `/reissue` 응답 유실 계약:

- TMI-130 구현/운영 상세는 [Stage 9 연동·운영 가이드](refresh-token-response-recovery-stage-9-runbook.md)를 따른다. 현재 기능 기본 OFF이며 실제 운영·모바일 검증 전에는 사용하지 않는다.
- `Idempotency-Key` 단일 헤더가 필수다. 요청별 소문자 UUID v4를 전송 전에 안전하게 저장하고, 같은 원 Refresh Token의 전송 재시도는 같은 ID를 사용한다.
- 서버는 rotation과 replacement Session을 원자적으로 확정한다.
- 같은 논리 재발급은 현재 계정/세션 상태가 유효하고 고정 최대 2분 기한 이내일 때 최초 결과를 재전달한다. 재시도에 따라 기한을 연장하거나 새로운 Token을 생성하지 않는다.
- `Reissue-Access-Expires-At`·`Reissue-Refresh-Expires-At` 절대 만료 헤더를 사용하고, replay의 기존 ExpiresIn을 현재 수신 시각에 다시 더하지 않는다.
- `409 REISSUE_RECOVERY_EXPIRED`는 정상 복구 만료이며 전체 세션 폐기가 아니다. `REISSUE_RESULT_SUPERSEDED`이면 로컬 최신 결과를 확인한다. `503 SESSION_SECURITY_UNAVAILABLE`만 같은 요청으로 제한 재시도한다(통신 장애/429 포함).
- 서로 다른 논리 작업에서 실제로 rotation된 옛 Token을 다시 사용하면 `REFRESH_TOKEN_REUSE_DETECTED`로 처리한다.
- 프론트는 서버의 최종 멱등·복구 계약을 보존하고, 매 transport retry마다 새로운 논리 작업을 만들지 않는다.
- 복구 취소/구 epoch/만료된 원 credential은 재사용 공격보다 우선해 거절한다. Guest 자동 초기화나 SNS만으로 과거 Guest 기록 연결은 하지 않는다.

### 5.2 현재 기기 로그아웃

1. `/api/v1/auth/logout`에 현재 Identity Refresh Token을 보낸다.
2. 성공·이미 폐기됨·서버에서 session을 찾지 못함 모두 멱등 성공으로 취급한다.
3. Firebase SDK `signOut`을 시도한다.
4. Firebase signOut 실패 여부와 관계없이 Identity Token과 로컬 사용자·민감 cache를 삭제한다.
5. 로그인 화면으로 이동한다.

### 5.3 모든 기기 로그아웃

1. Identity Access Token으로 `/api/v1/auth/logout-all`을 호출한다.
2. 현재 기기의 Identity Token과 Firebase 상태를 정리한다.
3. 다른 기기는 다음 `/reissue`에서 더 이상 Identity Token을 받을 수 없다.

`200`은 내부 세션 무효화와 Firebase revoke 작업의 durable 접수다. 다른 기기 UI 변경이나 Firebase 원격 완료를 뜻하지 않는다. 기존 자체 Access Token은 기존 만료·차단 정책을 따른다.

- 응답 유실은 같은 유효 Access Token/jti로 제한 재시도한다. 새 Token으로 자동 재요청하면 별도 logout cycle이 된다.
- 대기·결과 불명 상태만으로 새 인증 로그인을 막지 않는다. 과거 Firebase 인증의 강제 Token 갱신은 새 인증이 아니다.
- 지연 revoke에 영향받은 Firebase 기반 자체 세션은 `401 SESSION_LOGGED_OUT`으로 거절될 수 있다. Firebase signOut과 자체 Token 삭제 후 “전체 로그아웃 요청 처리로 다시 로그인이 필요합니다”를 안내한다. 영향 없는 LOCAL·경계 이후 새 인증 세션은 유지한다.
- 안전한 조회/호출 전 실패만 제한 재시도한다. 원격 mutation 결과가 불명확하면 자동 재전송하지 않고 서버 운영 조사로 전환한다. 앱에서 이를 해결하려고 logout이나 exchange를 무한 반복하지 않는다.
- `503 SESSION_SECURITY_UNAVAILABLE`은 접수/인증 처리를 확정할 수 없는 일시 오류다. logout 성공을 단정하지 않고 제한 재시도한다.

서버 구현·활성화 조건은 [Stage 8 계획서](firebase-logout-all-revoke-stage-8-plan.md)와 [운영 검증](firebase-logout-all-revoke-stage-8-runbook.md)을 따른다. 이 가이드의 최종 release 전제와 달리 Stage 8 코드의 기본 feature flag는 OFF다.

## 6. 회원탈퇴 관련 프론트 계약

### 6.1 Firebase/SNS MEMBER 탈퇴

1. 현재 Provider로 Firebase recent reauthentication을 수행한다.
2. fresh Firebase ID Token과 현재 Identity Refresh Token을 `/api/v1/users/withdraw`에 보낸다.
3. 성공하면 `cleanupStatus`와 관계없이 내부 계정 탈퇴는 확정된 것으로 처리한다.
4. Firebase `signOut`을 시도하고 Identity Token·로컬 계정·민감 cache를 모두 삭제한다.
5. `ACCOUNT_WITHDRAWN`을 여러 요청에서 받아도 안내와 화면 전환은 한 번만 수행한다.

`cleanupStatus=EXTERNAL_CLEANUP_PENDING`은 실패가 아니다. 외부 Firebase User와 identity release가 비동기로 이어진다는 뜻이다.

### 6.2 탈퇴 후 재가입

- cleanup이 끝나기 전 같은 Firebase/SNS credential로 접근하면 `WITHDRAWAL_CLEANUP_PENDING`이 발생할 수 있다.
- 프론트가 polling할 공개 cleanup 상태 API는 현재 없다.
- 정리 중에는 계정을 새로 만들거나 phone 충돌을 우회하지 말고 “계정 정보를 정리 중입니다. 잠시 후 다시 시도해 주세요.”로 안내한다.
- cleanup 완료 뒤 재가입은 새 canonical UUID User를 만든다. 이전 시험 기록을 새 User에 자동 연결하지 않는다.

## 7. 오류별 프론트 처리

| HTTP / code | 의미 | 권장 처리 |
| --- | --- | --- |
| `400 INVALID_REQUEST` | JSON·필드 검증 실패 | `result` 필드 오류를 화면에 연결. 민감값은 표시하지 않음 |
| `401 COMMON_UNAUTHORIZED` | Identity Access Token 누락·만료·검증 실패 | single-flight reissue 한 번 후 원 요청 한 번 재시도 |
| `401 INVALID_FIREBASE_ID_TOKEN` | Firebase Token 누락·만료·검증 실패 | 강제 갱신 한 번, 실패하면 Provider 재로그인 |
| `401 FIREBASE_RECENT_AUTH_REQUIRED` | `auth_time`이 목적별 허용시간 초과 | Provider credential로 명시적 재인증 후 재시도 |
| `403 FIREBASE_ACCOUNT_NOT_ALLOWED` | disabled·삭제됨·불완전 Firebase 계정 | Firebase signOut 후 재인증, 반복 시 지원 안내 |
| `403 FIREBASE_PROVIDER_NOT_ALLOWED` | Provider flag off, 미지원 Provider, phone-only login | 해당 버튼/흐름 중단, 지원 로그인 수단 안내 |
| `403 FIREBASE_EMAIL_VERIFICATION_REQUIRED` | password account email 미인증 | Firebase email 인증 완료 후 재인증·Token 갱신 |
| `403 FIREBASE_PHONE_VERIFICATION_REQUIRED` | signup/upgrade에 same-UID verified phone 없음 | 현재 Firebase User에 phone link 후 Token 강제 갱신 |
| `409 PHONE_ALREADY_LINKED` | 번호가 다른 User/Firebase User 소유 | 자동 merge 금지, 기존 계정 로그인·복구 안내 |
| `409 MERGE_REQUIRED` | Guest upgrade 대상 identity가 기존 MEMBER 소유 | 명시적 확인 후 Guest merge 흐름 |
| `409 FIREBASE_ENROLLMENT_CONFLICT` | attempt 없음·만료·소비·UID 불일치 | enrollment 폐기 후 Firebase 로그인과 `/exchange`부터 재시작 |
| `409 FIREBASE_ENROLLMENT_RESTART_REQUIRED` | lifecycle상 가입 재시작 필요 | 로컬 enrollment 폐기 후 처음부터 재시작 |
| `409 FIREBASE_IDENTITY_CONFLICT` | Firebase owner 불일치 | 로컬 추정 복구 금지, 재로그인 후 반복 시 지원 |
| `409 SOCIAL_IDENTITY_CONFLICT` | Provider subject owner 불일치 | 자동 연결 금지, 재로그인 후 반복 시 지원 |
| `403 GUEST_UPGRADE_NOT_ALLOWED` | 현재 User가 ACTIVE GUEST가 아님 | 프로필·Token 상태 재조회 후 로그인 초기화 |
| `403 GUEST_MERGE_NOT_ALLOWED` | merge source가 ACTIVE GUEST가 아님 | merge 중단, 현재 계정 재확인 |
| `409 GUEST_MERGE_TARGET_CONFLICT` | target MEMBER를 하나로 확정할 수 없음 | 자동 선택 금지, 처음부터 재인증 또는 지원 |
| `409 GUEST_MERGE_CONFLICT` | merge 동시성 충돌 | Token·프로필 재조회 후 한 번만 재시도 |
| `429 FIREBASE_RATE_LIMITED` | Firebase quota·rate limit | 입력 차단, backoff 후 재시도 |
| `503 FIREBASE_UNAVAILABLE` | Firebase 기능 off 또는 일시 장애 | 계정 없음으로 간주하지 말고 일시 장애 표시 |
| `401 INVALID_REFRESH_TOKEN` | RefreshSession 없음·일반 폐기 | Identity Token 삭제 후 로그인 |
| `401 REFRESH_TOKEN_EXPIRED` | Refresh Token 만료 | Identity Token 삭제 후 로그인 |
| `401 REFRESH_TOKEN_REUSE_DETECTED` | rotation된 Token 재사용 | 모든 로컬 Token 삭제, 보안상 전체 재로그인 안내 |
| `401 ACCOUNT_WITHDRAWN` | 탈퇴로 Session 폐기 또는 User 탈퇴 | terminal signed-out 처리 후 안내 한 번 표시 |
| `401 ACCOUNT_MERGED_TOKEN_REJECTED` | MERGED Guest의 옛 Access Token 사용 | 옛 Guest 상태 삭제 후 target MEMBER 로그인 |
| `403 ACCOUNT_NOT_ACTIVE` | SUSPENDED 등 비활성 User | 재발급 반복 금지, 상태 안내·지원 |
| `409 WITHDRAWAL_CLEANUP_PENDING` | 탈퇴 identity 정리 진행 중 | 가입·로그인 중단 후 나중에 재시도 안내 |

오류 `message`는 표시 가능한 기본 한국어 문구지만, 앱 분기는 번역 가능한 안정적 `code`를 기준으로 한다.

## 8. API 요청·응답 카탈로그

아래 Token 값은 문서용 placeholder다. 실제 값을 저장소·로그·문서에 복사하지 않는다.

| API | 인증 | 성공 `result` | 주요 실패 HTTP |
| --- | --- | --- | --- |
| `POST /api/v1/auth/firebase/exchange` | 공개 + Firebase ID Token body | `AUTHENTICATED` 또는 `ENROLLMENT_REQUIRED` | 400, 401, 403, 409, 429, 503 |
| `POST /api/v1/auth/firebase/signup` | 공개 + Firebase ID Token body | Identity Token 묶음 | 400, 401, 403, 409, 429, 503 |
| `POST /api/v1/auth/guest` | 공개 | Identity Token 묶음 | 400, 409 |
| `POST /api/v1/auth/firebase/guest/prepare` | Guest Identity Bearer + Firebase ID Token body | `ENROLLMENT_REQUIRED`, `ALREADY_LINKED`, `MERGE_REQUIRED` | 401, 403, 409, 429, 503 |
| `POST /api/v1/auth/firebase/guest/upgrade` | Guest Identity Bearer + Firebase ID Token body | Identity Token 묶음 | 400, 401, 403, 409, 429, 503 |
| `POST /api/v1/auth/firebase/guest/merge` | Guest Identity Bearer + Firebase ID Token body | target MEMBER Identity Token 묶음 | 400, 401, 403, 409, 429, 503 |
| `POST /api/v1/auth/firebase/auth-methods/sync` | MEMBER Identity Bearer + Firebase ID Token body | `linkedProviders` | 400, 401, 403, 409, 429, 503 |
| `POST /api/v1/auth/reissue` | Refresh Token body + Idempotency-Key | 최초 Identity Token 묶음 또는 같은 결과 복구, 절대 만료 헤더 | 400, 401, 403, 409, 503 |
| `POST /api/v1/auth/logout` | Refresh Token body | `null` | 400 |
| `POST /api/v1/auth/logout-all` | Identity Bearer | `null` (원격 완료가 아닌 접수) | 401, 403, 503 |
| `GET /api/v1/users/me` | Identity Bearer | 사용자 프로필 | 401, 403, 404 |
| `POST /api/v1/users/withdraw` | Identity Bearer + account credential body | 탈퇴 상태 | 400, 401, 404, 409 |

Spring Security가 Bearer Token 자체를 거절하면 application 오류 대신 `401 COMMON_UNAUTHORIZED` 또는 `403 COMMON_FORBIDDEN`이 반환될 수 있다. 표의 실패 HTTP는 현재 Controller와 application 경계를 합친 프론트 처리 범위다.

### 8.1 `POST /api/v1/auth/firebase/exchange`

인증: 공개

요청:

```json
{
  "firebaseIdToken": "<firebase-id-token>"
}
```

기존 MEMBER 성공:

```json
{
  "isSuccess": true,
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "result": {
    "type": "AUTHENTICATED",
    "accessToken": "<identity-access-token>",
    "refreshToken": "<identity-refresh-token>",
    "grantType": "Bearer",
    "accessTokenExpiresIn": 1800000,
    "refreshTokenExpiresIn": 1209600000
  }
}
```

신규 사용자 성공:

```json
{
  "isSuccess": true,
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "result": {
    "type": "ENROLLMENT_REQUIRED",
    "enrollmentId": "550e8400-e29b-41d4-a716-446655440000",
    "missingRequirements": ["PHONE_VERIFICATION", "PROFILE", "CONSENTS"],
    "expiresIn": 600000
  }
}
```

`missingRequirements` 배열 순서는 계약으로 사용하지 않는다. 가능한 값은 `EMAIL_VERIFICATION`, `PHONE_VERIFICATION`, `PROFILE`, `CONSENTS`다.

### 8.2 `POST /api/v1/auth/firebase/signup`

인증: 공개

이 API는 로그인 API가 아니라 `/firebase/exchange`에서 `ENROLLMENT_REQUIRED`를 받은 신규 사용자의 가입 완료 API다. `nickname`은 앞선 가입 화면에서 사용자가 입력·확정한다. 기존 MEMBER의 `AUTHENTICATED` 로그인 흐름에서는 이 API를 호출하지 않으며 nickname도 보내지 않는다.

요청:

```json
{
  "enrollmentId": "550e8400-e29b-41d4-a716-446655440000",
  "firebaseIdToken": "<force-refreshed-firebase-id-token>",
  "nickname": "토스마스터",
  "isPrivacyConsented": true,
  "privacyConsentVersion": "privacy-v1",
  "isTermConsented": true,
  "termConsentVersion": "term-v1"
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

### 8.3 `POST /api/v1/auth/guest`

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

정상적인 응답 유실 재시도는 최종 Guest 복구 계약으로 같은 Guest Session을 복구한다. 소유 증명 없이 이미 사용된 installationId만 다시 제출한 요청은 `409 GUEST_ALREADY_EXISTS`이며, installationId 자체를 인증 credential로 사용하지 않는다.

### 8.4 `POST /api/v1/auth/firebase/guest/prepare`

인증: `Authorization: Bearer <guest-identity-access-token>`

요청:

```json
{
  "firebaseIdToken": "<firebase-id-token>"
}
```

신규 승격 준비:

```json
{
  "isSuccess": true,
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "result": {
    "type": "ENROLLMENT_REQUIRED",
    "enrollmentId": "550e8400-e29b-41d4-a716-446655440000",
    "expiresIn": 600000
  }
}
```

이미 현재 User 소유로 판정된 상태:

```json
{
  "isSuccess": true,
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "result": {
    "type": "ALREADY_LINKED"
  }
}
```

기존 MEMBER merge 필요:

```json
{
  "isSuccess": true,
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "result": {
    "type": "MERGE_REQUIRED"
  }
}
```

`ALREADY_LINKED`, `MERGE_REQUIRED`에는 `enrollmentId`, `expiresIn` 필드가 JSON에 포함되지 않는다.

### 8.5 `POST /api/v1/auth/firebase/guest/upgrade`

인증: `Authorization: Bearer <guest-identity-access-token>`

요청은 `/firebase/signup`과 같은 필드를 사용한다. 성공 응답도 `/firebase/signup`의 Token 응답과 같다. 성공 즉시 응답 Token으로 기존 Guest Token을 전부 교체한다.

### 8.6 `POST /api/v1/auth/firebase/guest/merge`

인증: `Authorization: Bearer <guest-identity-access-token>`

요청:

```json
{
  "firebaseIdToken": "<existing-member-firebase-id-token>"
}
```

성공:

```json
{
  "isSuccess": true,
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "result": {
    "accessToken": "<target-member-identity-access-token>",
    "refreshToken": "<target-member-identity-refresh-token>",
    "grantType": "Bearer",
    "accessTokenExpiresIn": 1800000,
    "refreshTokenExpiresIn": 1209600000
  }
}
```

### 8.7 `POST /api/v1/auth/firebase/auth-methods/sync`

인증: `Authorization: Bearer <member-identity-access-token>`

요청:

```json
{
  "firebaseIdToken": "<force-refreshed-firebase-id-token>"
}
```

성공:

```json
{
  "isSuccess": true,
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "result": {
    "linkedProviders": ["GOOGLE", "APPLE", "KAKAO"]
  }
}
```

`linkedProviders`는 집합이므로 배열 순서를 UI 계약으로 사용하지 않는다. 실제 연결된 Provider만 포함된다.

### 8.8 `POST /api/v1/auth/reissue`

인증: 공개. Body의 Refresh Token 자체가 credential이다.

필수 헤더: `Idempotency-Key: <요청별 소문자 UUID v4>`. 전송 재시도는 같은 값을 유지한다. 만료된 Access Token을 자동 첨부하지 않는다.

성공 응답 헤더: `Reissue-Access-Expires-At`, `Reissue-Refresh-Expires-At` (UTC ISO-8601 Instant). 성공·오류 모두 `Cache-Control: no-store`, `Pragma: no-cache`.

아래 JSON은 최초와 replay에서 동일하며 두 ExpiresIn은 최초 duration(밀리초)이다. 409/503 및 pending 저장·single-flight·계정 전환 처리 전체 계약은 [Stage 9 가이드](refresh-token-response-recovery-stage-9-runbook.md)의 2절을 따른다.

요청:

```json
{
  "refreshToken": "<identity-refresh-token>"
}
```

성공:

```json
{
  "isSuccess": true,
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "result": {
    "accessToken": "<new-identity-access-token>",
    "refreshToken": "<new-identity-refresh-token>",
    "grantType": "Bearer",
    "accessTokenExpiresIn": 1800000,
    "refreshTokenExpiresIn": 1209600000
  }
}
```

### 8.9 `POST /api/v1/auth/logout`

인증: 공개. 존재하지 않거나 이미 폐기된 Refresh Token도 멱등 성공이다.

요청:

```json
{
  "refreshToken": "<identity-refresh-token>"
}
```

성공:

```json
{
  "isSuccess": true,
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "result": null
}
```

### 8.10 `POST /api/v1/auth/logout-all`

인증: `Authorization: Bearer <identity-access-token>`

요청 body: 없음

성공:

```json
{
  "isSuccess": true,
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "result": null
}
```

### 8.11 `GET /api/v1/users/me`

인증: `Authorization: Bearer <identity-access-token>`

성공:

```json
{
  "isSuccess": true,
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "result": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": null,
    "nickname": "토스마스터",
    "accountType": "MEMBER",
    "provider": "FEDERATED",
    "privacyConsented": true,
    "privacyConsentVersion": "privacy-v1",
    "privacyConsentedAt": "2026-09-07T00:00:00Z",
    "termConsented": true,
    "termConsentVersion": "term-v1",
    "termConsentedAt": "2026-09-07T00:00:00Z",
    "createdAt": "2026-09-07T00:00:00Z"
  }
}
```

- `accountType`이 현재 계정 구분의 기준이며 값은 `GUEST`, `MEMBER`다.
- `provider`는 하위 호환용 deprecated 필드다. 실제 값은 `LOCAL`, `FEDERATED`, `GUEST`일 수 있으므로 신규 UI 분기에 사용하지 않는다.
- SNS Provider별 연결 상태는 `provider`가 아니라 `/firebase/auth-methods/sync`의 `linkedProviders`를 사용한다.

### 8.12 `POST /api/v1/users/withdraw`

인증: `Authorization: Bearer <identity-access-token>`

Firebase/SNS MEMBER 요청:

```json
{
  "refreshToken": "<identity-refresh-token>",
  "password": null,
  "firebaseIdToken": "<recently-reauthenticated-firebase-id-token>"
}
```

성공:

```json
{
  "isSuccess": true,
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "result": {
    "status": "WITHDRAWN",
    "withdrawnAt": "2026-09-07T00:00:00Z",
    "cleanupStatus": "EXTERNAL_CLEANUP_PENDING"
  }
}
```

Firebase/SNS MEMBER는 `password`를 보내지 않는다. LOCAL MEMBER는 `firebaseIdToken` 대신 현재 `password`를 보내고, Guest는 둘 다 보내지 않는다.

## 9. 프론트 구현 체크리스트

- [ ] Firebase ID Token과 Identity Access/Refresh Token을 타입 수준에서 분리한다.
- [ ] Identity Access Token만 Learning Core Bearer로 보낸다.
- [ ] Refresh Token을 OS 보안 저장소에 보관한다.
- [ ] reissue를 앱 전체 single-flight로 만들고 원 요청은 한 번만 재시도한다.
- [ ] reissue transport retry가 서버의 최종 멱등·응답 복구 계약을 유지하도록 구현한다.
- [ ] signup/upgrade 전 같은 Firebase UID에 phone credential을 link한다.
- [ ] phone link 전후 UID를 비교하고 link 후 ID Token을 강제 갱신한다.
- [ ] `AUTHENTICATED`, `ENROLLMENT_REQUIRED`를 `result.type`으로 분기한다.
- [ ] `missingRequirements`와 `linkedProviders` 배열 순서에 의존하지 않는다.
- [ ] enrollment 만료·충돌 시 `/exchange`부터 재시작한다.
- [ ] Guest `MERGE_REQUIRED`에서 사용자 확인을 받는다.
- [ ] Guest `ALREADY_LINKED`를 MEMBER 로그인 성공으로 취급하지 않는다.
- [ ] logout·withdrawal terminal 처리에서 Firebase signOut 실패와 로컬 Token 삭제를 분리한다.
- [ ] Token·OTP·비밀번호·phone을 log, analytics, crash report에서 제거한다.
- [ ] 서버 기준 policy metadata에서 current version을 가져온다.
- [ ] 환경별 Firebase project, Provider button, backend feature flag를 함께 배포한다.
- [ ] Guest merge는 Billing·Learning Core consumer staging E2E 뒤에만 노출한다.

## 10. 이 문서가 전제하는 완료 상태

이 가이드를 production 프론트 계약으로 사용하는 release는 다음 상태를 이미 충족한다.

- Android/iOS Google·Apple login·link·redirect 검증 완료
- Kakao Identity Platform Generic OIDC, Billing, deep-link와 stable provider UID 검증 완료
- 같은 Firebase User의 phone link와 UID 유지, 국내 SMS·quota·abuse 방어 검증 완료
- Firebase signup·Guest upgrade·merge Mongo Transaction 검증 완료
- withdrawal external cleanup, identity release와 같은 credential 재가입 검증 완료
- Billing·Learning Core `UserMerged` consumer와 ownership migration 활성화 완료
- UserWithdrawn deny marker와 탈퇴 Access Token 차단 활성화 완료
- logout-all Firebase refresh revoke와 retry·reconciliation 완료
- Refresh Token rotation 원자성·응답 유실 복구 완료
- Provider unlink·전화번호 변경, Guest 생성 응답 유실 복구 완료
- 기존 ACTIVE LOCAL 회원의 Firebase rebind·migration 정책 완료
- 인증 전 정책 metadata와 환경별 Provider capability 공급 완료

이 중 하나라도 충족하지 않은 환경은 이 문서의 일부 기능만 지원하는 개발·staging 환경이다. 프론트는 서버 capability가 보장하지 않는 버튼이나 전환 흐름을 노출하지 않는다.

## 부록 A. 코드·계약 근거

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
