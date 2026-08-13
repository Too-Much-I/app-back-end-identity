# Firebase Auth broker 격리 PoC

Jira `TMI-90`의 Firebase credential broker 결정을 검증하는 production 비의존 PoC다. Java 애플리케이션의 API나 Spring Bean을 추가하지 않으며, `demo-` project ID를 사용하는 Firebase Authentication Emulator만 실행한다.

## 검증 범위

- email/password 가입·재로그인과 email verification Claim 변화
- Google·Apple emulator credential의 Firebase User·provider UID 안정성
- Firebase Admin SDK의 ID Token Claim 검증
- disabled·refresh token revoke·user delete 동작
- 기존 Firebase User에 phone proof를 연결했을 때 UID 유지
- phone credential 직접 sign-in으로 phone-only Firebase User가 만들어질 수 있음
- 이미 다른 Firebase User가 점유한 번호의 연결 충돌
- 기존 owner 삭제 뒤 번호 점유 해제와 재연결
- Identity의 provider 허용, recent-auth, enrollment 재사용과 Guest merge dual-proof 정책
- Kakao 공개 OIDC discovery 최소 계약을 확인하는 별도 opt-in 테스트

Auth Emulator는 실제 Google·Apple 서명 검증, OAuth redirect, Apple revoke, 국내 SMS 전송, Identity Platform billing이나 Kakao provider 등록을 재현하지 않는다. 이 항목은 `docs/poc/firebase-auth-broker-stage-0.md`의 외부 검증 gate로 남긴다.

## 실행

Firebase CLI가 지원하는 Node.js 20, 22 또는 24를 사용한다.

```shell
cd poc/firebase-auth
npm ci
npm run test:contract
npm run test:emulator
```

Kakao discovery 검증은 외부 공개 endpoint를 호출하므로 기본 PoC와 분리한다.

```shell
npm run test:kakao-discovery
```

`npm test`는 contract와 Auth Emulator 테스트를 순서대로 실행한다. 기본 Java 테스트인 `./gradlew clean test`에는 이 PoC가 포함되지 않으며 외부 Provider를 호출하지 않는다.

## 안전 규칙

- 실제 Firebase project ID, service account, API credential을 사용하지 않는다.
- 테스트 데이터는 Emulator에서만 존재하며 각 테스트 전에 삭제한다.
- 실제 이메일이나 전화번호를 테스트 입력으로 사용하지 않는다.
- Auth Emulator가 일회성 검증값을 터미널에 표시할 수 있으므로 실행 로그를 저장·공유하지 않는다.
- ID Token, provider subject와 Firebase UID를 출력하거나 fixture로 저장하지 않는다.
- `node_modules`, Firebase debug log와 Emulator UI log는 Git에서 제외한다.
- 이 디렉터리의 SDK는 PoC 전용 devDependency이며 production 애플리케이션 artifact에 포함되지 않는다.

Node용 Firebase Web SDK는 browser 전용 phone verification API를 노출하지 않는다. 그래서 Emulator phone 테스트는 Firebase client SDK 내부와 동일한 Identity Toolkit Emulator endpoint를 직접 사용한다. Android/iOS/Web production client는 공식 SDK의 credential link API를 사용하고 실기기에서 UID 유지와 redirect 복귀를 다시 검증해야 한다.

