# Firebase authentication broker Stage 0 PoC 결과

- Jira: `TMI-90`
- 실행일: 2026-08-13
- 결과: local contract·Auth Emulator·Kakao discovery 검증 통과, production 외부 gate는 미완료
- production feature: 비활성

## 목적

Firebase가 credential broker 역할을 맡고 Identity가 canonical account와 자체 Session을 유지하는 구조가 기술적으로 가능한지 확인한다. 실제 credential, 운영 Firebase project와 사용자 개인정보는 사용하지 않는다.

## 환경과 격리

- Firebase Authentication Emulator
- demo project ID만 사용해 비에뮬레이트 서비스 접근 차단
- Firebase Web SDK `12.17.1`
- Firebase Admin SDK `14.2.0`
- Firebase CLI `15.26.0`
- PoC dependency는 `poc/firebase-auth`의 devDependency이며 Spring Boot artifact와 기본 Gradle test classpath에 포함되지 않음
- 테스트 계정은 각 test 전에 Emulator에서 삭제
- Kakao discovery 외에는 외부 Provider 호출 없음

현재 로컬 실행기의 Node.js는 Firebase CLI dependency가 명시한 지원 LTS 범위 밖이었지만 테스트 자체는 통과했다. 재현 CI는 `package.json`에 명시한 Node.js 20, 22 또는 24를 사용한다.

## 실행 결과

| 검증 | 방법 | 결과 | 해석 |
| --- | --- | --- | --- |
| Identity policy contract | `npm run test:contract` | 7/7 통과 | provider 허용, phone-only 거절, enrollment, recent-auth, reissue 비의존, Guest dual proof |
| Firebase Auth Emulator | `npm run test:emulator` | 5/5 통과 | email/password, Admin verify·disable·revoke·delete, federated mapping, phone link·충돌·cleanup |
| Kakao public discovery | `npm run test:kakao-discovery` | 1/1 통과 | issuer, authorization code, PKCE S256, RS256, pairwise subject와 `sub` Claim 확인 |

설치 시 PoC 전용 dependency tree에서 moderate 등급 audit 항목이 보고됐다. production runtime dependency는 아니지만 CI에 PoC를 유지한다면 lockfile 갱신과 audit 결과를 별도로 추적해야 한다. 이 작업에서는 자동 breaking upgrade를 적용하지 않았다.

## Provider별 결과

| Provider | Emulator/공개 계약 결과 | 아직 증명하지 못한 항목 | 현재 결정 |
| --- | --- | --- | --- |
| Email/password | 가입·로그인 UID 유지, email verification Claim 변화, Admin Token 검증 성공 | 실제 email 발송·action link·template | Firebase 채택, password enrollment에 verified email 필수 |
| Google | 같은 emulator credential로 Firebase UID와 provider UID 유지 | 실제 Google OAuth·모바일 redirect·계정 선택 | Firebase 채택, 외부 client gate 필요 |
| Apple | 같은 emulator credential로 Firebase UID와 provider UID 유지 | 실제 nonce·private relay·revoke·account deletion | Firebase 채택, Apple lifecycle gate 필요 |
| Phone | 기존 User link UID 유지, direct sign-in으로 별도 phone-only User 생성, 번호 충돌, owner 삭제 후 재사용 | 공식 모바일/Web SDK link, 실제 SMS와 abuse 방어 | 가입 proof로만 사용, phone-only Identity login 거절 |
| Kakao | 공개 OIDC discovery 최소 계약 충족 | Identity Platform provider 등록·billing·providerData UID·Android/iOS 복귀 | feature off, 실제 project PoC 전 production NO-GO |

Emulator의 federated credential은 실제 Provider 서명을 검증하지 않는다. Google·Apple 행의 통과 결과는 Firebase 내부 account/provider mapping이 요구 모델을 지원한다는 증거이고, 실제 Provider 연동 완료 증거가 아니다.

## Phone 시나리오 결과

### 기존 Firebase User에 link

email/password Firebase User의 ID Token으로 phone verification을 완료했을 때 응답의 Firebase UID가 기존 UID와 같았고, Admin UserRecord에 password와 phone provider가 함께 나타났다.

phone link 직후 Token의 sign-in provider만 보고 토선생 로그인 가능 여부를 판단하면 안 된다. Enrollment는 같은 UID의 Admin providerData에서 승인된 primary credential과 verified phone을 함께 확인한다.

### 직접 phone sign-in

phone proof를 기존 User Token 없이 완료하면 별도 phone-only Firebase User가 생성됐다. 따라서 client 금지만으로 충분하지 않고 LOGIN_EXCHANGE에서 `sign_in_provider=phone`이며 승인된 primary provider가 없는 계정을 거절해야 한다.

### 번호 충돌과 cleanup

한 Firebase User가 점유한 번호를 다른 User에 link하려 하면 ownership 충돌이 발생했다. 기존 owner를 삭제한 뒤에는 같은 번호를 candidate User에 link할 수 있었다.

production에서는 충돌을 자동 merge로 바꾸지 않는다. abandoned enrollment cleanup도 FirebaseIdentity mapping 부재, active enrollment 부재와 resume 유예를 모두 재확인한 뒤 revoke·delete한다.

## Token 검증 결과

Auth Emulator에서 Admin SDK의 다음 동작을 확인했다.

- project audience·issuer·UID·sign-in provider Claim 확인
- `checkRevoked` 검증
- disabled User의 기존 ID Token 거절
- refresh token revoke 뒤 기존 ID Token 거절
- Firebase User delete 뒤 Admin 조회 거절

Emulator는 실제 production 공개키 조회, 네트워크 timeout과 quota를 재현하지 않는다. Stage 3에서는 Admin SDK 뒤에 mock 가능한 adapter를 두고 signature·issuer·audience·tenant·시간·provider allowlist와 안정적인 오류 mapping을 별도 단위 테스트한다.

## Enrollment와 Guest merge 계약 결과

PoC contract는 다음 불변식을 실행 가능한 테스트로 고정했다.

- 같은 project·Firebase UID·binding은 같은 active enrollment 경쟁 key를 사용한다.
- GUEST_USER binding에는 현재 Guest userId가 필수다.
- Firebase 교환·enrollment·merge·auth sync·고위험 재인증은 revoke와 recent-auth를 적용한다.
- Identity RefreshSession reissue와 Learning Core 요청은 Firebase를 호출하지 않는다.
- Guest merge는 source ACTIVE GUEST의 Identity JWT subject와 target ACTIVE MEMBER의 Firebase proof를 모두 요구한다.
- email과 phone을 merge proof 입력으로 사용하지 않는다.

Mongo partial unique index와 CAS 자체는 Stage 3 범위이므로 이번 PoC에서 collection을 추가하지 않았다.

## Kakao 결과와 go/no-go

Kakao 공개 discovery에서 Generic OIDC 후보에 필요한 다음 계약을 확인했다.

- 고정 issuer와 JWKS endpoint
- authorization code flow
- PKCE S256
- RS256 ID Token
- pairwise subject와 `sub` Claim

이 결과만으로 Firebase Identity Platform 등록 가능성, 동일 Kakao 앱에서 providerData UID가 반복 로그인·link 동안 안정적인지, Android/iOS deep-link가 정상인지 증명할 수 없다.

현재 결정은 production `NO-GO`다. 격리 Firebase project를 Identity Platform으로 업그레이드하고 billing 영향이 승인된 뒤 Generic OIDC provider 등록·실기기 login/link를 통과해야 `FIREBASE_KAKAO_ENABLED`를 켤 수 있다.

## 외부 검증 대기 gate

- 격리 Firebase project와 Android/iOS test app 준비
- 실제 email action link와 발송 정책
- 실제 Google·Apple login·link·redirect
- Apple nonce와 authorization revoke·account deletion
- 같은 current Firebase User에 대한 공식 client SDK phone link
- 국내 SMS 도달률·비용·quota·국가 제한·App Check/reCAPTCHA
- Identity Platform upgrade·billing과 Kakao Generic OIDC 등록
- Kakao providerData stable UID와 모바일 deep-link
- Firebase 장애·timeout·quota 오류 mapping
- lifecycle reconciliation과 provider kill switch
- 운영 User 0건 read-only 확인

외부 gate가 남아 있으므로 TMI-90의 production 활성화 완료 조건을 충족했다고 보지 않는다. 다만 Stage 3·4 코드 범위와 fail-closed 계약을 구현할 근거는 확보했다.

## 재현 명령

```shell
cd poc/firebase-auth
npm ci
npm run test:contract
npm run test:emulator
```

외부 공개 discovery 테스트는 명시적으로 별도 실행한다.

```shell
npm run test:kakao-discovery
```

전체 Java 회귀 테스트는 별도로 실행한다.

```shell
./gradlew clean test
```

PoC 실행 로그에는 Emulator가 생성한 일회성 검증값이 표시될 수 있으므로 로그 artifact를 저장하거나 공유하지 않는다.

