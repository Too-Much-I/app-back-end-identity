# 로컬 Firebase·Identity 테스트 화면

## 결론

1. Node.js 20 이상에서 `node tools/auth-test/server.mjs`로 실행하고 `http://localhost:4173`을 연다.
2. 별도 공개 배포 없이 Firebase Google 로그인과 같은 UID의 가상 전화번호 연결을 수행한다.
3. 실제 Identity 테스트 DB에 가입/세션을 생성한다. 버튼은 사용자가 직접 누른다.
4. 사용자 토큰은 메모리에만 보관하며 원문/응답 개인정보는 화면·로그·파일에 표시하지 않는다. 계정 찾기에서만 의도된 SNS·마스킹 이메일 힌트를 표시한다.
5. Android SHA/SDK 및 Stage 9 응답 유실 복구 검증을 대체하지 않는다.

## 반드시 확인할 준비

- Firebase `to-teacher-firebase`의 웹 앱 등록 → 공개 웹 SDK 구성 객체를 **JSON**으로 입력한다. JS 변수 선언이 아닌 큰따옴표 JSON이다. 서비스 계정/개인키는 절대 입력하지 않는다.
- Authentication 설정의 승인된 도메인에 `localhost`가 있는지 확인한다. 변경이 필요하면 담당자 승인 후 설정한다.
- 콘솔에 등록된 가상 전화번호/코드만 사용한다. SDK의 `appVerificationDisabledForTesting`을 이 화면에만 설정하므로 실제 SMS 번호는 지원하지 않는다.
- Google popup을 허용한다. 웹 authDomain/OAuth 설정에 따라 추가 준비가 필요할 수 있으며 인증 오류는 실제 실행 시 확인한다.
- 현재 배포의 `PRIVACY_CONSENT_VERSION`, `TERM_CONSENT_VERSION`과 해당 약관 본문/URL을 담당자에게 확인한다. 임의 기본값을 넣지 않았으며 동의는 사용자가 직접 한다.

## 순서

설정 적용 → Google 로그인 → Identity 로그인/가입 준비 → 신규일 때만 전화 연결 → 약관 확인/동의·닉네임 입력 → 회원가입 → 프로필/재발급/LC 접근 확인.

기존 MEMBER의 exchange가 성공하면 전화 연결/가입 없이 바로 API 테스트 가능하다. enrollment 만료면 exchange부터, recent-auth 오류면 Google 재인증부터 다시 진행한다. `auth/credential-already-in-use`는 다른 계정 소유이므로 자동 병합/삭제하지 않는다.

## TMI-192 계정 찾기

설정 적용 후 6번 영역에서 계정 찾기 인증 시작 → 가상 전화 코드 입력 → 인증 후 계정 안내 순서로 진행한다. 기존 프로젝트의 별도 in-memory Auth 인스턴스를 사용하며 기존 Identity pair/Firebase SNS 로그인은 덮어쓰지 않는다. 응답 유실 시 같은 접수번호·인증으로 재조회 버튼을 사용한다. 조회 결과는 textContent로 출력한다.

서버의 `ACCOUNT_RECOVERY_ENABLED` 및 독립 HMAC 키, 기존 Firebase/phone-identity 설정이 필요하다. 기본 OFF에서는 503이 정상이다. 실제 SMS·iOS/Android 연동은 이 가상 번호 테스트로 검증되지 않는다. phone-only Firebase UID 생성 후 가입의 전화 credential 중복 처리는 별도 통합 QA 대상이며 자동 삭제하지 않는다. [배포 런북](../../docs/contracts/single-sns-account-recovery-runbook.md).

## 안전장치와 한계

- 로컬 프로세스는 `127.0.0.1:4173`에만 바인딩한다. `localhost` Host, 동일 Origin, 전용 헤더를 검사하고 CORS를 열지 않는다. 포트 포워딩/외부 공개 금지.
- 로컬 게이트웨이는 두 테스트 HTTPS 호스트의 지정 API와 계정 찾기 2개 경로만 전달한다. 운영/임의 URL/탈퇴 API는 지원하지 않는다. 서버 CORS 변경 불필요. 실제 앱 CORS 검증은 별도다.
- SDK는 Google 공식 CDN의 고정 버전을 사용하며 인터넷이 필요하다. Firebase 인증정보는 Firebase에, Firebase ID Token은 테스트 Identity에, Identity Access Token은 테스트 Identity/LC에만 전송한다.
- 브라우저 개발자 도구 네트워크에는 실제 인증정보가 존재한다. HAR/콘솔/스크린샷을 공유하지 않는다. 확장 프로그램이 없는 전용 테스트 브라우저를 권장한다.
- 토큰은 localStorage/sessionStorage에 쓰지 않는다. 새로고침/닫기는 로컬 메모리만 지우며 서버 세션 삭제가 아니다. 사용 후 Ctrl+C로 서버를 종료한다.
- 작업은 single-flight이며 자동 재시도하지 않는다. 재발급마다 UUID v4를 전달하고 실패 시 기존 pair를 버려 Stage 9 OFF에서 이전 토큰 재사용을 막는다. 복구 ON의 동일 요청 재전달·절대 만료 계약 시험은 별도 앱/통합 테스트 대상이다.
- JWT 표시 결과는 서명 검증이 아닌 claim 형태 확인이다. 실제 인증/권한 성공은 보호 API 응답으로 검증한다.
- LC today는 읽기 smoke 호출만 하며 문제/채점/업로드 등 Learning Core 기능은 구현하지 않는다.

## 검증

`node --test tools/auth-test/server.test.mjs` (외부 인프라 호출 없음)

`node --test tools/auth-test/app.test.mjs` (Firebase/HTTP를 가짜 구현으로 대체한 UI 흐름 검사)

2026-09-28 검증: 위 3개 테스트와 `./gradlew clean test` 성공. 로컬 서버 기동 완료. Chrome 자동 열기는 `ERR_BLOCKED_BY_CLIENT`로 차단되어 실제 렌더링/Google 인증/전화 연결/E2E는 미검증이다. 브라우저 보호를 해제하지 않았으며 사용자가 로컬 URL을 직접 열어 확인해야 한다.

기존 계약: [프론트 Firebase 연동 가이드](../../docs/contracts/frontend-firebase-auth-integration-guide.md).

## 병합 진행 조회

회원 exchange 후 ‘내 병합 이전 상태 조회’로 최근 20건(완료 포함)을 수동 조회한다. Billing NOT_REQUIRED는 대상 제외이며 전체 권한 부여가 아니다. 빈 목록은 legacy/만료/미추적일 수 있다. 자동 polling·merge 재실행·프론트 구현은 포함하지 않는다. 30회/분 제한을 준수한다.
