# 프론트엔드 Firebase·SNS 로그인 및 회원 전환 연동 가이드

- 문서 성격: 현재 저장소 Controller·DTO·Service 기준 프론트 인계 명세. 구현 사실과 출시 정책, 미검증 설정을 구분한다.
- 기준일: 2026-09-21 (TMI-169 Guest 가입 재개·미충족 요건·enrollment 만료 계약 반영)
- 대상: 모바일·프론트엔드 개발자, QA, 제품 담당자
- 적용 조건: 환경별 backend base URL, 활성 기능, Firebase project와 Provider 설정을 백엔드/모바일 담당자가 함께 확인한다. 코드 존재가 배포·활성화 완료를 뜻하지 않는다.

> 이번 업데이트는 신규 Guest 진입을 제공하지 않는다. 기존 Guest의 Token은 보존하여 SNS 승격/병합에 사용한다. 구버전은 업데이트하도록 하는 정책이며 최소 지원 버전·스토어 URL·강제 업데이트 적용 방식은 배포 전에 별도로 확정해야 한다. Guest 응답 복구(Stage 11)는 취소됐고 기존 이메일 회원 migration도 대상이 없다.

읽는 순서: 앱 구현은 이 문서의 흐름·오류 처리·API 카탈로그를 따른다. 배포 담당자와 QA는 [별도 부록](frontend-firebase-auth-integration-appendix.md)을 함께 확인한다.

Swagger 공유: 배포 서버의 `/swagger-ui.html` 또는 `/v3/api-docs`를 사용한다. 서버 없이 공유하려면 저장소에서 `./gradlew shareSwagger`를 실행해 `build/distributions/identity-swagger.zip`을 전달한다. 압축을 풀고 `index.html`을 열면 읽기 전용 Swagger를 볼 수 있다. [공유본 사용법](../swagger/README.md)을 참고한다. 생성 명세에 학습 기록 삭제처럼 미확정인 API는 포함하지 않는다.

## 1. 5줄 결론

1. 앱은 Google·Apple·Kakao 인증을 Firebase Auth로 수행하고, Firebase ID Token을 Identity API에 교환한 뒤 Identity Access Token만 Learning Core에 보낸다.
2. 기존 MEMBER는 `Firebase 로그인 → /firebase/exchange → AUTHENTICATED`, 신규 사용자는 `ENROLLMENT_REQUIRED → 같은 Firebase User에 phone link → /firebase/signup` 순서다.
3. phone credential은 별도 로그인으로 사용하지 않고 현재 Firebase User에 `linkWithCredential`해야 하며, link 후 강제 갱신한 ID Token을 제출해야 한다.
4. Identity Refresh Token은 rotation되므로 재발급을 single-flight로 처리하고, 성공 시 Access/Refresh Token을 함께 교체해야 한다.
5. 기존 Guest는 `/firebase/guest/prepare`의 유효한 enrollmentId·미충족 요건·정책 버전으로 가입을 재개하고, 만료되면 prepare를 재호출한다. 신규 Guest 생성은 호출하지 않는다. [재개 계약과 근거](#guest-resume)를 따른다.

## 2. 반드시 읽어야 할 앱 처리 규칙

### 2.0 적용 범위와 설정

신규 앱은 SNS 가입/로그인과 기존 Guest 전환을 제공하며, 신규 Guest 생성·이메일 로그인 UI·전화번호 변경·Firebase UID rebind는 제공하지 않는다. 서버 주소·활성 SNS·Firebase 앱 설정·현재 약관 URL/버전·최소 지원 앱 버전은 인계받은 환경 설정을 사용한다. 공개 정책/capability 조회 API는 없다.

구현 여부와 배포 활성화는 다르다. [출시 전 확인사항과 기능별 활성화 조건](frontend-firebase-auth-integration-appendix.md#deployment)을 확인한 기능만 노출한다.

### 2.1 Kakao도 동일한 Firebase 교환 흐름을 사용한다

Google·Apple뿐 아니라 Kakao도 승인된 Firebase Generic OIDC provider를 통해 같은 흐름을 사용한다.

| 로그인 수단 | Firebase provider ID |
| --- | --- |
| Google | `google.com` |
| Apple | `apple.com` |
| Kakao | `oidc.kakao` 기본값 |
| Phone | `phone` — 가입 proof이며 단독 로그인 수단이 아님 |

Kakao는 Firebase 기본 provider가 아니다. Kakao Developers, Identity Platform Generic OIDC, Billing, 모바일 redirect/deep-link 설정과 실제 검증이 필요하다. 이 문서는 완료를 보증하지 않는다. 준비 전에는 Kakao 버튼을 숨기고 Google부터 테스트한다.

<a id="guest-resume"></a>

### 2.2 Guest 가입 재개 상태는 prepare 응답을 따른다 — TMI-169

Guest 승격은 `/users/me`의 nickname·동의 값만으로 phone proof나 enrollment 상태를 추정하지 않는다. Guest Identity Access Token과 fresh Firebase ID Token을 함께 검증하는 `/firebase/guest/prepare`가 현재 유효한 enrollment와 미충족 요건을 반환한다.

- owner가 없으면 활성 enrollment를 재사용하거나 만료·부재 시 새로 만들어 `ENROLLMENT_REQUIRED`를 반환한다.
- 다른 ACTIVE MEMBER owner면 mutation 없이 `MERGE_REQUIRED`를 반환한다.
- 현재 Guest가 identity owner인 불가능 상태는 `409 IDENTITY_STATE_CONFLICT`이며 자동 승격·merge하지 않는다.

Guest prepare 전용 `ALREADY_LINKED` 결과는 더 이상 사용하지 않는다. 이미 MEMBER인 사용자의 `/providers/link/prepare`가 반환하는 별도 `ALREADY_LINKED`는 완료된 SNS 연결의 멱등 상태이므로 그대로 유지한다.

프론트 변경 필수 사항: Guest prepare의 결과 타입은 `ENROLLMENT_REQUIRED | MERGE_REQUIRED`로 갱신하고, 409 `IDENTITY_STATE_CONFLICT`를 별도 오류로 처리한다. 신규 응답 필드가 없는 구 서버 응답을 받으면 빈 requirements나 임의 정책 버전으로 보완하지 말고 해당 배포의 API 계약을 확인한다.

`missingRequirements`는 서버에 저장된 화면 진행 번호가 아니라 **지금 충족해야 하는 요건의 집합**이다. `ENROLLMENT_REQUIRED`는 가입 절차를 계속할 수 있다는 뜻이며 최종 승격 성공을 보증하지 않는다. upgrade 시 proof·동의·소유권·enrollment를 다시 검증한다.

확인된 구현 근거: [prepare 분기](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseGuestPrepareService.java), [요건 판정](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseEnrollmentRequirementResolver.java), [응답 필드](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/dto/response/FirebaseGuestPrepareResponse.java). 전체 판정표·테스트 근거는 [TMI-169 부록](frontend-firebase-auth-integration-appendix.md#guest-resume-qa)에 있다.

### 2.3 `ACCOUNT_MERGED_TOKEN_REJECTED`는 `/reissue`의 대표 오류가 아니다

이 코드는 MERGED된 Guest의 예전 **Access Token**으로 보호 API를 호출할 때 발생할 수 있다. 예전 Guest Refresh Token은 활성화된 세션 보호 경로에 따라 `INVALID_REFRESH_TOKEN`, `ACCOUNT_NOT_ACTIVE` 등으로 거절될 수 있다. `/reissue`가 이 코드 하나로 실패한다고 가정하지 말고 현재 계정의 terminal 오류를 공통 처리한다.

### 2.4 recent-auth 오류는 Token 강제 갱신만으로 해결되지 않는다

`getIdToken(forceRefresh=true)`는 Firebase ID Token 내용을 새로 받지만 `auth_time` 자체를 갱신하지 않을 수 있다. `FIREBASE_RECENT_AUTH_REQUIRED`이면 현재 Firebase 사용자에게 Provider credential을 다시 제시하는 명시적 재인증을 수행한 뒤 새 ID Token을 받아야 한다.

### 2.5 `logout-all`의 Firebase 폐기는 비동기다

Stage 8 기능을 활성화한 서버는 기존 자체 세션을 무효화하고 Firebase refresh revoke 작업을 저장한다. `200`은 내부 무효화·작업 접수이며 Firebase 원격 완료가 아니다. 현재 기기는 Firebase SDK `signOut`과 로컬 Token 삭제를 수행한다. flag OFF인 기존 모드는 이 원격 폐기를 보장하지 않는다. 이미 발급된 자체 Access Token의 모든 downstream 즉시 차단을 뜻하지 않는다.

### 2.6 회원가입 정책 버전은 서버 기준으로 공급한다

`/firebase/signup`, `/guest/upgrade`는 개인정보 처리방침·이용약관 version을 요구한다. Guest 승격에는 `/firebase/guest/prepare`가 현재 두 정책 version을 공급한다. direct 신규 가입에 사용할 공개 정책 metadata API는 아직 없으므로 배포 담당자와 합의한 설정 공급이 필요하다. 임의 version이나 구버전으로 가입을 우회하지 않는다.

### 2.7 문서에 없는 변경 API를 추정하지 않는다

Provider 연결/해제는 이 문서 8.13~8.18을 사용한다. 전화번호 변경 API와 공개 rebind API는 제공하지 않는다. `/auth-methods/sync`를 추가 연결·unlink·phone 변경에 사용하거나 프론트에서 Firebase 상태만 임의 변경하면 안 된다.

### 2.8 출시 담당자가 결정·전달할 사항

새 prepare 계약을 제공하는 backend 배포 버전·base URL과 프론트 적용 일정을 확정한다. Guest용 약관 version은 prepare 응답을 사용하되 해당 버전의 약관 본문·URL 공급 방식, 충돌 반복 시 지원 안내, 최소 지원 앱 버전은 담당자 합의가 필요하다. 상세 목록은 [출시 전 확인사항](frontend-firebase-auth-integration-appendix.md#deployment)을 따른다.

프론트 추가 요구사항인 **학습 기록만 삭제**는 계정·로그인 상태를 유지하는 별도 기능으로 검토한다. 삭제할 데이터 범위와 Learning Core의 API·완료 확인 계약을 먼저 확정한다. [기능 구분과 앱 흐름](#learning-history-delete), [결정할 범위와 인계 항목](frontend-firebase-auth-integration-appendix.md#learning-history-delete-scope)을 참고한다.

### 2.9 주요 위험과 미확인 사항

이 문서는 저장소에서 확인한 구현을 설명한다. 대상 환경 배포 여부, 실제 SNS·SMS와 Mongo Transaction, Guest-owned identity가 없다는 전제는 별도 검증이 필요하다. prepare 이후에도 정책·소유권·인증 상태가 바뀔 수 있으므로 최종 API 오류를 처리해야 한다. [QA 표](frontend-firebase-auth-integration-appendix.md#guest-resume-qa)의 기대 결과는 배포 환경 테스트 완료 기록이 아니다.

## 3. 프론트가 반드시 지켜야 하는 인증 경계

### 3.1 Token의 역할

| Token | 사용처 | 프론트 처리 |
| --- | --- | --- |
| Firebase ID Token | Identity의 Firebase exchange·signup·Guest 전환·인증수단 sync·SNS 연결/해제·Firebase 회원탈퇴 proof | Firebase SDK에서 필요 시 새로 받고 장기 세션 Token처럼 직접 관리하지 않음 |
| Identity Access Token | Identity 보호 API, Learning Core 등 사용자 API | `Authorization: Bearer <identity-access-token>` |
| Identity Refresh Token | Identity `/reissue`, `/logout`, `/users/withdraw` | OS 보안 저장소에 저장하고 일반 API에는 전송하지 않음 |

- Firebase ID Token을 Learning Core에 보내지 않는다.
- Identity Access Token을 `/firebase/exchange`의 `firebaseIdToken`으로 보내지 않는다.
- Request Body·Path·Query에 클라이언트가 선택한 `userId`를 추가하지 않는다. 서버가 검증한 Identity JWT `sub`를 사용한다.
- Token, OTP, Firebase credential, 비밀번호, 전화번호를 log·analytics·crash report에 기록하지 않는다.
- 응답은 HTTP status와 함께 `isSuccess`, `code`, `result`를 확인한다.
- `expiresIn`, `accessTokenExpiresIn`, `refreshTokenExpiresIn`은 밀리초다. `expiresAt` 등 시각은 UTC ISO-8601 문자열이며, `nextPollAfterSeconds`는 초다.
- 사용자 Access Token의 `account_type`은 `MEMBER` 또는 `GUEST`다. UI 계정 표시는 `/users/me.accountType`을 사용하고 권한 판단은 서버에 맡긴다. workload JWT는 모바일에서 사용하지 않는다.

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

### 4.3 기존 Guest의 업데이트 처리 — 신규 Guest 생성 금지

업데이트 후 보안 저장소의 기존 Identity Access/Refresh Token을 먼저 읽는다. 사용자가 이미 Guest인지 `/users/me`로 확인하고, 필요한 경우 현재 서버 계약에 맞게 single-flight 재발급한다. 기존 Guest 인증을 유지한 채 4.4/4.5를 진행한다.

- Token이 없거나 복구 불가능하면 SNS 로그인/가입 화면으로 이동한다. 식별자만으로 과거 Guest 기록을 자동 연결하지 않는다.
- 신규 앱은 `/api/v1/auth/guest`를 호출하지 않는다. installationId 재생성으로 우회하지 않는다.
- 기존 Guest의 Token을 업데이트 직후 삭제하면 승격/병합의 소유 증명을 잃을 수 있다.
- 구버전 업데이트 정책은 결정됐지만 이 저장소에 최소 버전 gate가 구현됐다고 보증하지 않는다. 강제 업데이트 적용은 출시 전 확인사항이다.

### 4.4 Guest를 새 MEMBER로 승격

```text
Guest Identity Access Token 유지
→ Firebase Provider 로그인
→ POST /api/v1/auth/firebase/guest/prepare
→ prepare 결과 분기
```

| prepare 결과 | 프론트 동작 |
| --- | --- |
| `ENROLLMENT_REQUIRED` | `missingRequirements`에 필요한 단계만 수행하고 같은 Firebase User의 강제 갱신 Token으로 `/guest/upgrade` 호출 |
| `MERGE_REQUIRED` | 신규 승격을 중단하고 사용자 확인 후 기존 MEMBER merge 흐름으로 이동 |

- `PHONE_VERIFICATION`은 같은 Firebase User에 phone credential을 link해 충족한다.
- `EMAIL_VERIFICATION`은 password 계정의 email 인증을 완료한 뒤 Token을 갱신한다.
- `PROFILE`은 MEMBER nickname 입력·확정을 뜻한다.
- `CONSENTS`는 응답의 현재 privacy/terms version으로 필수 동의를 다시 받아야 한다는 뜻이다.
- phone·email 상태가 바뀐 뒤 prepare를 다시 호출하면 활성 enrollmentId는 유지되고 requirements는 fresh proof 기준으로 다시 계산된다.

`PROFILE`은 Guest 승격에서 항상 반환된다. 사용자가 닉네임을 입력·확정했으면 앱에서 충족한 것으로 판단해 최종 요청에 포함한다. prepare에 닉네임을 저장하는 단계는 없으므로 requirements가 빈 배열이 될 때까지 반복 조회하지 않는다. nickname 초안 보존은 앱의 UX 선택이며 서버 재개 응답은 초안을 복원하지 않는다.

`CONSENTS`가 없으면 서버에 현재 필수 동의가 유효하게 저장돼 있어 재동의 화면을 생략할 수 있다. 그래도 `/guest/upgrade`에는 `isPrivacyConsented=true`, `isTermConsented=true`와 prepare의 두 version을 모두 보낸다. `CONSENTS`가 있으면 현재 버전의 필수 동의를 사용자에게 받은 뒤 전송한다. 화면 생략 여부와 요청 필드 필수 여부는 구분한다. 품질 검토 선택 동의는 이 requirements 판정에 포함되지 않는다.

앱 종료 후 재개하는 순서:

1. 기존 Guest 인증을 유지하고 같은 Firebase 계정으로 인증한다. 필요한 Identity 재발급은 기존 single-flight 규칙을 따른다.
2. 새 Firebase ID Token으로 `/guest/prepare`를 호출해 enrollmentId, requirements, 정책 버전, expiresIn을 함께 갱신한다. `/users/me`만 보고 마지막 가입 화면을 추정하지 않는다.
3. `PHONE_VERIFICATION`이 없으면 phone OTP를 다시 요구하지 않는다. 있으면 같은 Firebase User에 phone link 후 ID Token을 강제 갱신한다. email 요건도 같은 방식으로 현재 proof를 반영한다.
4. 닉네임 확정과 필요한 동의를 마치면 [8.5의 필수 필드](#guest-upgrade-request)로 upgrade를 호출한다. `MERGE_REQUIRED`이면 4.5의 사용자 확인 흐름으로 이동한다.

만료 복구는 `expiresIn`이 소진됐거나 upgrade가 `409 FIREBASE_ENROLLMENT_CONFLICT` / `FIREBASE_ENROLLMENT_RESTART_REQUIRED`를 반환할 때 수행한다. 기존 enrollmentId를 폐기하고 현재 Guest 인증과 fresh Firebase proof로 prepare를 다시 호출해 받은 결과로 분기한다. 같은 만료 ID로 무한 재시도하거나 direct signup으로 바꾸지 않는다. `FIREBASE_RECENT_AUTH_REQUIRED`이면 먼저 2.4의 Provider 재인증을 수행한다. 재시작 오류가 계속되면 자동 반복을 멈추고 상태 확인·지원 안내로 연결한다.

활성 enrollment 재조회는 유효시간을 연장하지 않는다. 기본 10분은 서버 설정이며 `expiresIn` 단위는 밀리초다. 앱의 카운트다운은 안내용이고 실제 만료 판정은 서버가 수행한다. 만료는 Firebase phone 인증의 자동 취소나 attempt의 즉시 물리 삭제를 뜻하지 않는다. 다시 받은 requirements에 따라 필요한 인증만 진행한다. 서버의 abandoned cleanup 상태에 따라 재인증·재시작이 필요할 수 있다.

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
- Billing·Learning Core의 `UserMerged` consumer와 종단 이전 검증이 끝나기 전 UI를 활성화하지 않는다. merge 성공 응답이 downstream의 모든 기록 이전 완료를 의미하지는 않는다.

signup·upgrade·merge에는 `/reissue`처럼 응답 자체를 그대로 재전달하는 멱등 계약이 없다. 응답 유실 시 새 enrollment/Guest를 자동 만들지 않는다. fresh Firebase proof로 `/exchange`를 확인하여 `AUTHENTICATED`면 해당 MEMBER Token으로 복구하고, 신규 가입 안내나 충돌이면 기존 Guest Token을 성급히 삭제하지 말고 상태를 재확인한다. Guest 전환을 계속할 때는 `/exchange`에서 받은 direct signup enrollment 대신 `/guest/prepare`의 Guest 전용 enrollment를 사용한다. 불명 상태를 성공으로 표시하지 않는다.

### 4.6 기존 MEMBER에 SNS 연결 — 최초/재연결 공통

1. 사용자가 Google·Apple·Kakao 연결을 선택한다. 과거 해제 여부는 프론트가 판단하지 않는다.
2. 기존에 연결된 다른 SNS로 재인증하고 `/providers/link/prepare`를 호출한다. 요청 ID를 보관한다.
3. PREPARED이면 `/providers/link/start`를 호출한다. ALREADY_LINKED이면 추가 SDK 호출 없이 끝낸다.
4. **최초 start 응답의 linkAllowed=true인 경우에만** 같은 Firebase User에 대상 credential을 한 번 link한다.
5. 성공 및 기존 SDK 실행 종료를 확인한 뒤 대상 SNS로 start 이후 재인증하고 ID Token을 강제 갱신한다.
6. 같은 linkAttemptId로 `/providers/link/complete`를 호출한다. COMPLETED에서만 연결 완료 UI를 표시한다.

모든 API에 MEMBER Identity Bearer와 단계별 Firebase 증거를 제출한다. 예시·응답·status 처리는 [전용 계약](firebase-provider-unlink-stage-10-runbook.md#51-api와-응답)을 따른다.

- prepare 응답 유실은 원래 requestId로 `/providers/link/status`를 조회한다.
- start 응답 유실/중복 start는 **SDK 실행 허가를 재발급하지 않는다**. 조회 결과 STARTED만 보고 link를 다시 호출하지 않는다.
- complete 응답 유실은 같은 linkAttemptId로 complete를 재시도한다. 새 prepare를 하지 않는다.
- PREPARED 만료는 새 요청 ID로 재시작 가능하다. STARTED 만료/결과 불명은 운영 확인 대상이다.
- 다른 User가 대상 SNS를 소유하면 자동 이전/merge하지 않는다. userId·번호·혜택은 바꾸지 않는다.
- 기존 sync는 이미 승인된 연결 목록을 확인할 뿐, 추가·해제·재연결·phone 변경에 사용하지 않는다.
- “SNS 하나만 로그아웃”은 연결 해제와 다르다. Firebase signOut은 현재 Firebase 세션 전체를 종료한다.

### 4.7 이메일·비밀번호 경로 — 이번 신규 UI 범위 밖

신규 앱에서 이메일 로그인 화면과 legacy LOCAL API를 사용하지 않는다. 기존 이메일 회원 migration 대상도 없다. 서버 호환 경로는 [구버전·이메일 참고](frontend-firebase-auth-integration-appendix.md#legacy)에 보존한다.

## 5. Token 재발급과 로그아웃

### 5.1 앱 시작과 401 복구

1. 저장된 Identity Refresh Token이 없으면 비로그인 상태로 시작한다.
2. 있으면 `/api/v1/auth/reissue`를 호출한다.
3. 성공하면 새 Access/Refresh Token을 한 번의 로컬 상태 전환으로 교체한다.
4. 보호 API 401은 앱 전체 single-flight reissue 한 번으로 처리하고, 재발급이 명확히 성공한 경우에만 원 요청을 한 번 재시도한다.
5. reissue 401이면 반복하지 않고 terminal signed-out 상태로 전환한다.

Stage 9 활성 환경의 `/reissue` 응답 유실 계약:

- TMI-130 구현/운영 상세는 [Stage 9 연동·운영 가이드](refresh-token-response-recovery-stage-9-runbook.md)를 따른다. 현재 기능 기본 OFF이며 실제 운영·모바일 검증 전에는 사용하지 않는다.
- `Idempotency-Key` 단일 헤더가 필수다. 요청별 소문자 UUID v4를 전송 전에 안전하게 저장하고, 같은 원 Refresh Token의 전송 재시도는 같은 ID를 사용한다.
- 서버는 rotation과 replacement Session을 원자적으로 확정한다.
- 같은 논리 재발급은 현재 계정/세션 상태가 유효하고 고정 최대 2분 기한 이내일 때 최초 결과를 재전달한다. 재시도에 따라 기한을 연장하거나 새로운 Token을 생성하지 않는다.
- `Reissue-Access-Expires-At`·`Reissue-Refresh-Expires-At` 절대 만료 헤더를 사용하고, replay의 기존 ExpiresIn을 현재 수신 시각에 다시 더하지 않는다.
- `409 REISSUE_RECOVERY_EXPIRED`는 정상 복구 만료이며 전체 세션 폐기가 아니다. `REISSUE_RESULT_SUPERSEDED`이면 로컬 최신 결과를 확인한다. 통신 장애, `503 SESSION_SECURITY_UNAVAILABLE`, 인프라 `429`는 같은 요청으로 제한 재시도한다. 명확한 terminal 오류를 무한 재시도하지 않는다.
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

서버 구현·활성화 조건은 [Stage 8 계획서](firebase-logout-all-revoke-stage-8-plan.md)와 [운영 검증](firebase-logout-all-revoke-stage-8-runbook.md)을 따른다. Stage 8 코드의 기본 feature flag는 OFF다.

<a id="learning-history-delete"></a>

### 5.4 학습 기록만 삭제 — 추가 요구사항, API 미확정

프론트 요청: 사용자가 로그인 상태와 계정을 유지하면서 본인의 학습 기록만 삭제할 수 있어야 한다. 이 항목은 신규 기능 요구사항이며 현재 호출 가능한 API 명세가 아니다. Learning Core 저장소·배포 상태는 이번 작업에서 확인하지 않았다.

| 기능 | 처리 대상 | 계정·세션 | 학습 기록 |
| --- | --- | --- | --- |
| 로그아웃 | 현재 기기 또는 모든 기기의 인증 세션 | 계정 유지, 해당 세션 종료 | 서버 학습 기록 삭제를 요청하지 않음 |
| 회원 탈퇴 | 계정 탈퇴와 관련 정리 | 계정 탈퇴, 세션 폐기 | 서비스별 탈퇴 정리 계약을 따르며 이 문서만으로 삭제 범위·완료를 보장하지 않음 |
| 학습 기록만 삭제 (요구사항) | 사용자가 확인한 본인의 학습 데이터 범위 | 계정·로그인 상태 유지 | Learning Core가 확정한 대상만 삭제 |

소유 서비스는 Learning Core다. Identity에는 학습 데이터 삭제 로직을 추가하지 않고, 프론트는 확정된 Learning Core API에 기존 Identity Access Token을 사용한다. 대상 사용자는 검증된 JWT `sub`로 식별하며 클라이언트가 별도 userId를 보내지 않는다. 인증 방식은 [Identity–Learning Core JWT 계약](identity-learning-jwt.md)을 따른다. 삭제 권한·추가 재인증 필요 여부는 Learning Core 계약에서 확정한다.

제안하는 프론트 흐름은 설정의 독립된 “학습 기록 삭제” 메뉴 → 확정된 삭제 대상·보존 항목·복구 가능 여부 안내 → 사용자 확인 → 삭제 요청 → 완료 확인 → 해당 학습 캐시 무효화와 화면 재조회다. 비동기 접수 응답이라면 접수와 완료를 구분하고 Learning Core가 제공하는 완료 확인 방법을 따른다. API URL, HTTP method, 응답·오류 코드, 상태 조회와 재시도 방식은 아직 정하지 않았다.

이 기능의 정상 처리에서는 로그아웃·회원 탈퇴 API 호출, Firebase signOut, 계정 생성, 사용자 인증정보 삭제를 수행하지 않는다. 계정 UUID·SNS 연결·프로필·동의는 유지하는 요구사항이다. 구매·이용권·무료 체험 자격과 사용 횟수 초기화는 포함하지 않는 방향을 제안하며 제품/Billing 담당자와 확인한다.

로컬 캐시 삭제만으로 서버 기록 삭제 완료를 표시하지 않는다. 요청 유실·실패 시 완료 여부를 확인할 수 있어야 하며 재시도는 최종 API의 멱등 계약을 따른다. 서버 구현·배포와 삭제 범위 확정 전에는 실제 삭제 동작을 활성화하지 않는다. 상세 범위·경합·QA는 [부록](frontend-firebase-auth-integration-appendix.md#learning-history-delete-scope)에 인계한다.

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
| `409 FIREBASE_ENROLLMENT_CONFLICT` | attempt 없음·만료·소비·UID 불일치 | enrollment 폐기. Guest 승격은 `/guest/prepare`, direct 가입은 `/exchange`부터 재시작 |
| `409 FIREBASE_ENROLLMENT_RESTART_REQUIRED` | lifecycle상 가입 재시작 필요 | enrollment 폐기. 현재 진입점의 prepare/exchange부터 재시작 |
| `409 IDENTITY_STATE_CONFLICT` | Guest인데 현재 identity owner로 판정된 불가능 상태 | 자동 승격·merge 금지, 재인증 후 반복되면 지원 안내 |
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
| `400 INVALID_REISSUE_REQUEST_ID` | 재발급 요청 ID 누락/형식 오류 | 구현 점검. 이미 보낸 요청 ID를 새로 바꿔 재시도하지 않음 |
| `409 REISSUE_REQUEST_CONFLICT` | 재발급 요청 ID 재사용 충돌 | 해당 복구 중단, 최신 로컬 인증 상태 확인 |
| `409 REISSUE_RECOVERY_EXPIRED` | 같은 응답의 복구 기한 만료 | 최신 Token이 없다면 SNS 재인증 |
| `409 REISSUE_RESULT_SUPERSEDED` | 이미 교체된 재발급 결과 | 최신 결과 유지, 없으면 재인증 |
| `401 SESSION_LOGGED_OUT` | 세션/epoch/인증 시각 무효화 | 자체 Token 삭제·Firebase signOut·재로그인 |
| `503 SESSION_SECURITY_UNAVAILABLE` | 세션 처리를 확정할 수 없음 | 동일 요청으로 제한 재시도, 성공으로 간주하지 않음 |

Provider 연결/해제 전용 오류는 8.19를 따른다. `401 ACCOUNT_WITHDRAWN`, `401 SESSION_LOGGED_OUT`, `401 ACCOUNT_MERGED_TOKEN_REJECTED`처럼 이유가 명확한 오류는 일반 Access Token 만료와 구분하며 무조건 reissue부터 호출하지 않는다.

오류 `message`는 표시 가능한 기본 한국어 문구지만, 앱 분기는 번역 가능한 안정적 `code`를 기준으로 한다.

## 8. API 요청·응답 카탈로그

아래 Token 값은 문서용 placeholder다. 실제 값을 저장소·로그·문서에 복사하지 않는다.

| API | 인증 | 성공 `result` | 주요 실패 HTTP |
| --- | --- | --- | --- |
| `POST /api/v1/auth/firebase/exchange` | 공개 + Firebase ID Token body | `AUTHENTICATED` 또는 `ENROLLMENT_REQUIRED` | 400, 401, 403, 409, 429, 503 |
| `POST /api/v1/auth/firebase/signup` | 공개 + Firebase ID Token body | Identity Token 묶음 | 400, 401, 403, 409, 429, 503 |
| `POST /api/v1/auth/guest` | 공개, 구버전 참고 전용 | 신규 앱 호출 금지 | 400, 409 |
| `POST /api/v1/auth/firebase/guest/prepare` | Guest Identity Bearer + Firebase ID Token body | `ENROLLMENT_REQUIRED` 또는 `MERGE_REQUIRED` | 401, 403, 409, 429, 503 |
| `POST /api/v1/auth/firebase/guest/upgrade` | Guest Identity Bearer + Firebase ID Token body | Identity Token 묶음 | 400, 401, 403, 409, 429, 503 |
| `POST /api/v1/auth/firebase/guest/merge` | Guest Identity Bearer + Firebase ID Token body | target MEMBER Identity Token 묶음 | 400, 401, 403, 409, 429, 503 |
| `POST /api/v1/auth/firebase/auth-methods/sync` | MEMBER Identity Bearer + Firebase ID Token body | 기존 승인된 `linkedProviders` 검증만 | 400, 401, 403, 409, 429, 503 |
| `POST /api/v1/auth/reissue` | Refresh Token body + Idempotency-Key | 최초 Identity Token 묶음 또는 같은 결과 복구, 절대 만료 헤더 | 400, 401, 403, 409, 503 |
| `POST /api/v1/auth/logout` | Refresh Token body | `null` | 400 |
| `POST /api/v1/auth/logout-all` | Identity Bearer | `null` (원격 완료가 아닌 접수) | 401, 403, 503 |
| `GET /api/v1/users/me` | Identity Bearer | 사용자 프로필 | 401, 403, 404 |
| `POST /api/v1/users/withdraw` | Identity Bearer + account credential body | 탈퇴 상태 | 400, 401, 403, 404, 409, 429, 503 |
| `POST /api/v1/auth/firebase/providers/unlink` | MEMBER Bearer + 남는 SNS proof + Idempotency-Key | 202 해제 작업 | 400, 401, 403, 404, 409, 429, 503 |
| `POST /api/v1/auth/firebase/providers/unlink/status` | 공개 + 남는 SNS proof + requestId body | 200 해제 상태 | 400, 401, 403, 404, 409, 429, 503 |
| `POST /api/v1/auth/firebase/providers/link/prepare` | MEMBER Bearer + 기존 SNS proof + Idempotency-Key | 200 연결 준비 상태 | 400, 401, 403, 404, 409, 429, 503 |
| `POST /api/v1/auth/firebase/providers/link/start` | MEMBER Bearer + 기존 SNS proof | 200 상태 및 linkAllowed | 400, 401, 403, 404, 409, 429, 503 |
| `POST /api/v1/auth/firebase/providers/link/complete` | MEMBER Bearer + 대상 SNS proof | 200 연결 완료 상태 | 400, 401, 403, 404, 409, 429, 503 |
| `POST /api/v1/auth/firebase/providers/link/status` | MEMBER Bearer + Firebase proof + requestId body | 200 연결 상태 | 400, 401, 403, 404, 409, 429, 503 |
| `GET /api/v1/users/me/consents` | Identity Bearer | 200 정책 버전 및 사용자 동의 상태 | 401, 403, 404 |
| `PUT /api/v1/users/me/consents` | Identity Bearer | 200 저장된 동의 상태 | 400, 401, 403, 404, 409 |

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

exchange/signup은 Guest Access Token이 필요 없다. 공개 진입점은 Firebase 인증 증명을 검증한다는 뜻이며 인증 없이 임의 회원을 만드는 API라는 뜻은 아니다.

### 8.2 `POST /api/v1/auth/firebase/signup`

인증: 공개

이 API는 로그인 API가 아니라 `/firebase/exchange`에서 `ENROLLMENT_REQUIRED`를 받은 신규 사용자의 가입 완료 API다. `nickname`은 앞선 가입 화면에서 사용자가 입력·확정한다. 기존 MEMBER의 `AUTHENTICATED` 로그인 흐름에서는 이 API를 호출하지 않으며 nickname도 보내지 않는다.

signup/upgrade 공통 입력 제한: `enrollmentId`는 UUID 문자열, `firebaseIdToken`은 필수·최대 16,384자, trim 후 `nickname`은 2~20자다. 두 필수 동의 값은 true, 버전은 현재 서버 정책과 일치해야 한다. 현재 두 DTO에는 품질 검토 선택 동의 필드가 없다. 필요하면 가입 후 8.21 동의 변경을 사용한다.

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

### 8.3 `POST /api/v1/auth/guest` — 구버전 참고, 신규 앱 호출 금지

신규 앱에서는 호출하지 않는다. 기존 Guest의 전환에는 아래 8.4~8.6을 사용한다. 구버전 요청·응답 예시는 [별도 부록](frontend-firebase-auth-integration-appendix.md#legacy)에 보존한다.

### 8.4 `POST /api/v1/auth/firebase/guest/prepare`

인증: `Authorization: Bearer <guest-identity-access-token>`

요청:

```json
{
  "firebaseIdToken": "<firebase-id-token>"
}
```

최초 진입·중단 후 재개·만료 후 재발급 공통 성공 응답:

```json
{
  "isSuccess": true,
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "result": {
    "type": "ENROLLMENT_REQUIRED",
    "enrollmentId": "550e8400-e29b-41d4-a716-446655440000",
    "missingRequirements": ["PHONE_VERIFICATION", "PROFILE"],
    "privacyConsentVersion": "privacy-v1",
    "termConsentVersion": "term-v1",
    "expiresIn": 600000
  }
}
```

`missingRequirements`의 가능한 값은 `EMAIL_VERIFICATION`, `PHONE_VERIFICATION`, `PROFILE`, `CONSENTS`이며 배열 순서는 계약이 아니다. 활성 enrollment가 있으면 같은 ID를 재사용하고, 만료됐으면 기존 attempt를 `EXPIRED`로 전환한 뒤 새 ID를 반환한다.

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

`MERGE_REQUIRED`에는 enrollmentId, requirements, 정책 version, expiresIn이 포함되지 않는다. 현재 Guest가 identity owner인 비정상 상태는 성공 result가 아니라 `409 IDENTITY_STATE_CONFLICT`다.

<a id="guest-upgrade-request"></a>

### 8.5 `POST /api/v1/auth/firebase/guest/upgrade`

인증: `Authorization: Bearer <guest-identity-access-token>`

요청은 `/firebase/signup`과 같은 필드를 사용한다. 성공 응답도 `/firebase/signup`의 Token 응답과 같다. 성공 즉시 응답 Token으로 기존 Guest Token을 전부 교체한다.

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

일곱 필드 모두 필수다. enrollmentId는 해당 Guest와 Firebase UID에 대해 prepare에서 받은 값, 두 version은 최신 prepare 응답값으로 대체한다. 닉네임은 trim 후 2~20자다. phone 번호·인증 boolean·userId·missingRequirements는 요청에 추가하지 않는다. phone/email 검증은 서버가 Firebase proof로 확인한다. [Request validation 원문](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/dto/request/FirebaseGuestUpgradeRequest.java)과 [최종 검증](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseGuestUpgradeService.java)을 따른다.

prepare 뒤 정책이 바뀌어 `PRIVACY_CONSENT_VERSION_MISMATCH` 또는 `TERM_CONSENT_VERSION_MISMATCH`를 받으면 prepare에서 현재 요건과 version을 다시 받고 필요한 재동의를 진행한다. 동의 화면을 생략했다고 동의 필드를 누락하면 validation 오류가 발생한다.

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

`linkedProviders`는 집합이므로 배열 순서를 UI 계약으로 사용하지 않는다. 내부에서 승인된 Provider만 포함된다. 이 API는 읽기 검증으로 전환됐으며 Firebase에만 존재하는 신규 SNS를 저장하지 않는다. legacy `linkAttemptId`를 전달해도 409로 거절한다. 모든 신규/재연결은 공통 link API를 사용한다.

### 8.8 `POST /api/v1/auth/reissue`

인증: 공개. Body의 Refresh Token 자체가 credential이다.

Stage 9 활성 환경 필수 헤더: `Idempotency-Key: <요청별 소문자 UUID v4>`. 전송 재시도는 같은 값을 유지한다. 만료된 Access Token을 자동 첨부하지 않는다.

Stage 9 OFF에서는 기존 rotation 경로가 동작하고 exact response 복구·절대 만료 헤더를 제공하지 않는다. 헤더를 보냈다는 이유만으로 멱등 복구를 보장받지 못한다. 새 앱의 응답 유실 재시도를 켜기 전에 해당 환경의 Stage 9 활성화를 확인한다.

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

`cleanupStatus`의 enum은 `EXTERNAL_CLEANUP_PENDING`, `EXTERNAL_CLEANUP_IN_PROGRESS`, `EXTERNAL_CLEANUP_RETRY_WAIT`, `EXTERNAL_CLEANUP_COMPLETED`, `IDENTITY_RELEASE_PENDING`, `CLEANED`, `RECONCILIATION_REQUIRED`다. 모든 값이 최초 탈퇴 응답에 나온다는 뜻은 아니며, 이 API는 외부 cleanup polling API가 아니다. 실패 시 `INVALID_WITHDRAWAL_CREDENTIALS`, `WITHDRAWAL_FIREBASE_PROOF_REQUIRED`, `WITHDRAWAL_CREDENTIAL_TYPE_MISMATCH`, `WITHDRAWAL_CONFLICT`, `WITHDRAWAL_LIFECYCLE_CONFLICT` 등도 처리한다. 응답 유실로 성공 여부가 불명확하면 탈퇴를 임의 성공 처리하거나 새 계정을 만들지 말고 현재 인증 상태를 확인한다.

### 8.13 `POST /api/v1/auth/firebase/providers/unlink`

인증: MEMBER Identity Bearer + `Idempotency-Key`(단일 소문자 UUID v4). `provider`는 제거할 대상이고 Firebase proof는 **해제 후 남는 Google/Apple/Kakao**로 최근 재인증한 값이다. phone은 마지막 로그인 수단으로 계산하지 않는다.

예시: Google을 해제하고 Apple을 유지한다.

```json
{
  "provider": "GOOGLE",
  "firebaseIdToken": "<recent-apple-firebase-id-token>"
}
```

성공 HTTP **202**, `result`:

```json
{
  "operationId": "550e8400-e29b-41d4-a716-446655440001",
  "provider": "GOOGLE",
  "status": "PROCESSING",
  "acceptedAt": "2026-09-16T01:00:00Z",
  "completedAt": null,
  "nextPollAfterSeconds": 3
}
```

8.13~8.18 및 8.20~8.21의 응답 예시는 공통 `BaseResponse` 안의 **result만** 표시한다. 202도 `isSuccess=true`, `code=SUCCESS` envelope를 사용한다. 재접수 응답에 이미 terminal status가 올 수 있으며 HTTP 202만으로 원격 작업 완료를 판단하지 않는다.

접수 후 현재 기기를 포함한 자체 세션이 무효화되므로 Firebase signOut·자체 Token 삭제를 수행한다. status 조회용 원래 requestId는 별도로 보존한다. 앱이 Firebase SDK unlink를 직접 실행하지 않는다. 서버 worker가 해제와 revoke를 수행한다.

### 8.14 `POST /api/v1/auth/firebase/providers/unlink/status`

인증: Identity Bearer 불필요. Body의 **남는 SNS fresh Firebase proof**로 검증한다. 기존 만료 Access Token을 자동 첨부하지 않는다. `requestId`는 최초 unlink의 Idempotency-Key이며 operationId가 아니다.

```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440002",
  "firebaseIdToken": "<fresh-remaining-provider-firebase-id-token>"
}
```

성공 HTTP 200, 완료 `result`:

```json
{
  "operationId": "550e8400-e29b-41d4-a716-446655440001",
  "provider": "GOOGLE",
  "status": "COMPLETED",
  "acceptedAt": "2026-09-16T01:00:00Z",
  "completedAt": "2026-09-16T01:00:08Z",
  "nextPollAfterSeconds": null
}
```

| status | 프론트 동작 |
| --- | --- |
| `PROCESSING` | `nextPollAfterSeconds`(현재 3초)에 맞춰 제한 조회 |
| `COMPLETED` | 연결 해제 완료 표시, 남은 수단으로 재인증·exchange |
| `ACTION_REQUIRED` | 원격 결과 확인 필요. 무한 polling/새 unlink 금지, 지원 안내 |
| `SUPERSEDED` | 탈퇴 등 더 최신 작업으로 대체. 현재 계정 상태 확인 |

status 응답은 자체 Token을 발급하지 않는다. 조회 proof가 지연 revoke로 무효화되면 남는 SNS로 재인증한다. 404는 미접수 증명이 아니므로 새로운 요청 ID로 즉시 unlink를 재실행하지 않는다.

### 8.15 `POST /api/v1/auth/firebase/providers/link/prepare`

인증: MEMBER Identity Bearer + `Idempotency-Key`(단일 소문자 UUID v4). 신규/재연결 모두 동일 API다. 아래 예시는 기존 Google에 Apple을 추가한다. 동일한 Firebase User를 유지한다.

```json
{
  "provider": "APPLE",
  "firebaseIdToken": "<recent-existing-google-firebase-id-token>"
}
```

성공 HTTP 200, `result`:

```json
{
  "linkAttemptId": "550e8400-e29b-41d4-a716-446655440003",
  "provider": "APPLE",
  "status": "PREPARED",
  "expiresAt": "2026-09-16T01:05:00Z",
  "linkAllowed": false
}
```

prepare 전에 대상 SDK link를 실행하지 않는다. 이미 서버/Firebase 양쪽에서 승인된 연결이면 같은 schema의 `status=ALREADY_LINKED`, `linkAllowed=false`를 반환한다. 추가 link 없이 종료한다. 준비 응답 유실은 최초 requestId로 8.18을 조회한다.

### 8.16 `POST /api/v1/auth/firebase/providers/link/start`

인증: MEMBER Identity Bearer. Body는 기존 로그인 수단의 recent Firebase proof다.

```json
{
  "linkAttemptId": "550e8400-e29b-41d4-a716-446655440003",
  "firebaseIdToken": "<recent-existing-google-firebase-id-token>"
}
```

최초 성공 HTTP 200, `result`:

```json
{
  "linkAttemptId": "550e8400-e29b-41d4-a716-446655440003",
  "provider": "APPLE",
  "status": "STARTED",
  "expiresAt": "2026-09-16T01:06:00Z",
  "linkAllowed": true
}
```

**이 최초 응답에서 linkAllowed=true를 받았을 때만 SDK link를 한 번 실행한다.** 재요청의 `STARTED` 응답은 linkAllowed=false다. start 응답 유실은 status 확인 대상으로 전환하고 SDK를 추정 실행하지 않는다. 준비와 시작은 각각 기본 최대 5분이며 start에서 실제 SDK 작업용 기한이 다시 정해진다.

### 8.17 `POST /api/v1/auth/firebase/providers/link/complete`

인증: MEMBER Identity Bearer. SDK link 완료 및 호출 종료를 확인한 뒤 **대상 SNS(예: Apple)**로 재인증하고 강제 갱신한 Firebase proof를 보낸다. Firebase `auth_time`은 start 시각보다 엄격히 뒤여야 한다. 강제 갱신만으로 auth_time이 새로워지는 것은 아니며 초 단위 경계도 고려한다.

```json
{
  "linkAttemptId": "550e8400-e29b-41d4-a716-446655440003",
  "firebaseIdToken": "<target-apple-reauthenticated-after-start-firebase-id-token>"
}
```

성공 HTTP 200, `result`:

```json
{
  "linkAttemptId": "550e8400-e29b-41d4-a716-446655440003",
  "provider": "APPLE",
  "status": "COMPLETED",
  "expiresAt": "2026-09-16T01:06:00Z",
  "linkAllowed": false
}
```

서버가 대상 Provider 연결을 승인한 상태다. 연결 완료 API는 새 Access/Refresh Token을 발급하지 않는다. 응답 유실은 같은 linkAttemptId로 complete를 재시도한다. lifecycle이 바뀌면 재시도도 거절될 수 있으며 새 prepare로 우회하지 않는다.

### 8.18 `POST /api/v1/auth/firebase/providers/link/status`

인증: MEMBER Identity Bearer + Firebase proof. `requestId`는 최초 prepare의 Idempotency-Key이며 linkAttemptId가 아니다.

```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440004",
  "firebaseIdToken": "<authorized-current-firebase-id-token>"
}
```

성공 HTTP 200, 예시 `result`:

```json
{
  "linkAttemptId": "550e8400-e29b-41d4-a716-446655440003",
  "provider": "APPLE",
  "status": "STARTED",
  "expiresAt": "2026-09-16T01:06:00Z",
  "linkAllowed": false
}
```

| status | 프론트 동작 |
| --- | --- |
| `PREPARED` | 기한 내 start 진행 가능, SDK는 아직 실행하지 않음 |
| `STARTED` | 진행 중/불명 상태 확인. status는 link 실행 허가를 주지 않음 |
| `COMPLETED`, `ALREADY_LINKED` | 이미 승인된 연결. 추가 SDK link 없음 |
| `EXPIRED` | PREPARED 만료. 새 요청 ID로 prepare 가능 |
| `ACTION_REQUIRED` | STARTED 기한 초과 또는 상태 변경. 새 link/자동 취소 금지, 지원 안내 |
| `SUPERSEDED` | 세션/연결 상태가 바뀜. 현재 인증을 재확인 |

기존 SNS 증명을 기본 사용한다. STARTED/COMPLETED에서는 start 이후 재인증한 대상 SNS proof로도 조회 가능한 경로가 있다. 조회 성공이 exchange 또는 추가 mutation 권한을 부여하는 것은 아니다. 상세 중단·복구 제약은 [Stage 10 runbook](firebase-provider-unlink-stage-10-runbook.md#22-공통-연결의-5분과-앱-중단)을 따른다.

### 8.19 SNS 연결/해제 공통 검증·오류

- `provider` 허용값: `GOOGLE`, `APPLE`, `KAKAO`. SDK provider ID 문자열과 구분한다.
- `firebaseIdToken` 필수, 최대 16,384자. requestId/linkAttemptId는 소문자 UUID v4, 길이 36자다.
- Provider API 요청 Body는 최대 24,576바이트다. 초과 시 `413 PROVIDER_REQUEST_TOO_LARGE`이며 같은 Body를 그대로 재시도하지 않는다.
- recent-auth와 prepare/start 허용 기간 기본 최대 5분. 실제 반환 `expiresAt`을 우선한다.
- `/providers/relink/prepare`는 서버 라우트와 Swagger에서 제거했다. `/providers/link/prepare → start → complete`를 사용한다. 제거된 경로에 유효한 사용자 인증으로 요청하면 404이며, 인증 없는 요청은 기존 Security 정책에 따라 먼저 401이 될 수 있다.
- 원격 Firebase 상태가 바뀌어도 서버 완료 전에는 연결/해제 완료로 표시하지 않는다.

| HTTP/code | 처리 |
| --- | --- |
| 400 `INVALID_PROVIDER_REQUEST_ID` | 요청 식별자 검증 실패, 자동 재시도 중지 |
| 403 `PROVIDER_LAST_METHOD` | 마지막 로그인 수단 제거 불가 |
| 403 `PROVIDER_REMAINING_AUTH_REQUIRED` | 해당 단계가 요구하는 SNS로 재인증. complete에서는 대상 SNS 필요 |
| 409 `PROVIDER_CHANGE_CONFLICT` | 현재 작업/소유/세션 상태 재조회, SDK mutation 반복 금지 |
| 409 `PROVIDER_RELINK_REQUIRED` | 승인되지 않은 연결. 공통 link 흐름 필요, sync 우회 금지 |
| 409 `PROVIDER_RELINK_EXPIRED` | 준비/실행 기한 만료. PREPARED와 STARTED 처리 구분 |
| 404 `PROVIDER_OPERATION_NOT_FOUND` | 상태를 찾을 수 없음, remote 미실행 증거로 사용하지 않음 |
| 413 `PROVIDER_REQUEST_TOO_LARGE` | 요청 Body 크기 초과, 불필요한 필드·중복 데이터 확인 |
| 429 `PROVIDER_RATE_LIMITED` | 중복 탭 방지, Retry-After가 있으면 준수하며 제한 재시도 |
| 503 `PROVIDER_CHANGE_UNAVAILABLE` | 기능 OFF/일시 오류/결과 불명. 기존 requestId로 status 우선 |

공통 Firebase·세션 오류도 발생할 수 있다. `Retry-After`는 정수 초 등 유효한 값만 사용하고 무한 재시도하지 않는다.

### 8.20 `GET /api/v1/users/me/consents`

인증: Identity Bearer. 요청 Body 없음. **공개 가입 정책 API가 아니다.** 성공 HTTP 200, `result`:

```json
{
  "privacy": {
    "currentVersion": "privacy-v1",
    "consented": true,
    "consentedVersion": "privacy-v1",
    "consentedAt": "2026-09-16T01:00:00Z",
    "requiresConsent": false
  },
  "terms": {
    "currentVersion": "term-v1",
    "consented": true,
    "consentedVersion": "term-v1",
    "consentedAt": "2026-09-16T01:00:00Z",
    "requiresConsent": false
  },
  "qualityReview": {
    "currentVersion": "quality-review-v1",
    "consented": false,
    "consentedVersion": null,
    "consentedAt": null,
    "requiresConsent": false
  }
}
```

필수 정책은 과거 동의가 true여도 버전이 바뀌면 `requiresConsent=true`일 수 있다. 갱신 필요 여부는 이 필드로 판단한다. 선택 품질 검토의 `requiresConsent`는 항상 false이며, `consented`는 현재 버전 기준 유효성이다. 사용자 미존재·비활성 오류를 일반 정책 없음으로 해석하지 않는다.

### 8.21 `PUT /api/v1/users/me/consents`

인증: Identity Bearer. 현재 개인정보·약관 동의를 함께 제출한다. 품질 검토 선택 동의는 누락하면 false로 처리되므로 기존 true를 보존하려면 명시적으로 보내야 한다.

```json
{
  "isPrivacyConsented": true,
  "privacyConsentVersion": "privacy-v1",
  "isTermConsented": true,
  "termConsentVersion": "term-v1",
  "isQualityReviewConsented": false,
  "qualityReviewConsentVersion": null
}
```

성공 HTTP 200, `result`:

```json
{
  "privacyConsented": true,
  "privacyConsentVersion": "privacy-v1",
  "privacyConsentedAt": "2026-09-16T01:00:00Z",
  "termConsented": true,
  "termConsentVersion": "term-v1",
  "termConsentedAt": "2026-09-16T01:00:00Z",
  "qualityReviewConsented": false,
  "qualityReviewConsentVersion": null,
  "qualityReviewConsentedAt": null
}
```

필수 동의 누락은 `400 PRIVACY_CONSENT_REQUIRED`/`TERM_CONSENT_REQUIRED`, 버전 불일치는 `PRIVACY_CONSENT_VERSION_MISMATCH`/`TERM_CONSENT_VERSION_MISMATCH`/`QUALITY_REVIEW_CONSENT_VERSION_MISMATCH`다. 정책을 재조회해 사용자가 확인하도록 하며 임의 동의나 version 덮어쓰기를 하지 않는다. 동시 수정은 `409 USER_UPDATE_CONFLICT`일 수 있다.

## 9. 배포·QA·구버전 참고

- [출시 설정·기능 활성화 조건](frontend-firebase-auth-integration-appendix.md#deployment)
- [프론트 구현 체크리스트](frontend-firebase-auth-integration-appendix.md#qa) 및 [통합 검증·실패 사례](frontend-firebase-auth-integration-appendix.md#integration)
- [구버전 Guest·이메일 경로](frontend-firebase-auth-integration-appendix.md#legacy)
- [코드·계약 근거](frontend-firebase-auth-integration-appendix.md#sources)

부록은 참고 자료이며, 본문의 API·오류 처리·응답 유실·재인증 규칙을 대체하지 않는다.
