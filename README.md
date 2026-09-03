# 토선생 Identity Service

토선생 앱의 사용자 신원과 인증 수명 주기를 소유하는 Spring Boot 서비스다. 현재 이메일 회원가입·로그인, Guest 인증, 개인정보 처리방침·이용약관 필수 동의와 품질 검토 이용 선택 동의 상태 조회·갱신, 회원 탈퇴, RS256 Access Token 발급·검증, Opaque Refresh Token Rotation, 단일·전체 로그아웃, 내 프로필 조회와 Public Key 전용 JWKS endpoint가 구현되어 있다.

## 도메인 범위

Identity Service는 다음 기능을 소유한다.

- 사용자 계정과 프로필
- 이메일 회원가입과 로그인
- 소셜 로그인
- 비밀번호 해시
- Access Token 발급과 Refresh Token 세션
- 로그아웃
- 개인정보 처리방침·이용약관 필수 동의와 품질 검토 이용 선택 동의

시험, 시험 문제, AI 채점, 시험 결과, 10초 챌린지, 스트릭, 단어장, 음성 파일 및 AWS S3 업로드는 Learning Core 또는 다른 서비스의 책임이며 이 저장소에 구현하지 않는다. 서버 간 JWT 계약은 `docs/contracts/identity-learning-jwt.md`를 따른다. Firebase 인증 broker 목표 구조는 `docs/adr/ADR-001-firebase-authentication-broker.md`, 전체 단계는 `docs/contracts/social-login-implementation-plan.md`를 따른다.

## 기술 환경

- Java 21
- Spring Boot 3.4.2
- Gradle Groovy
- MongoDB
- 기본 서버 포트: `8081`

## 패키지 구조

- `domain.auth`: 인증 API, 유스케이스, 응답 변환, RefreshSession 도메인과 인증 오류
- `domain.user`: 사용자 API, 프로필 유스케이스, User 도메인과 사용자 오류
- `global.config`: Spring Security, 비밀번호 인코더와 OpenAPI 설정
- `global.security`: JWT·현재 사용자·Refresh Token 기술 구현과 Security 오류 응답
- `global.observability`: requestId 상관관계, 안전한 오류 문맥과 구조화 HTTP 완료 로그
- `global.response`, `global.exception`: 공통 응답 계약과 전역 HTTP 예외 변환

## 환경변수

| 이름 | 필수 여부 | 기본값 또는 용도 |
| --- | --- | --- |
| `MONGODB_URI` | 필수 | Identity MongoDB 연결 주소 |
| `MONGODB_DATABASE` | 선택 | `to-teacher-identity` |
| `SERVER_PORT` | 선택 | `8081` |
| `SWAGGER_ENABLED` | 선택 | Swagger UI와 OpenAPI 문서 활성화 여부, 기본값 `true` |
| `LOGGING_STRUCTURED_FORMAT_CONSOLE` | 선택 | stdout 구조화 로그 형식, 기본값 `ecs` |
| `SENTRY_ENABLED` | 선택 | Sentry 오류 전송 활성화 여부, 기본값 `false` |
| `SENTRY_DSN` | Sentry 활성화 시 필수 | Runtime DSN. 저장소가 아닌 배포 Secret으로 주입 |
| `SENTRY_ENVIRONMENT` | 선택 | Sentry 환경 이름, 기본값 `local` |
| `SENTRY_RELEASE` | 운영 배포 시 권장 | 배포마다 고정되는 release 식별자 |
| `SENTRY_AUTH_TOKEN` | Source context 업로드 시 필수 | Runtime이 아닌 CI build Secret으로만 주입 |
| `PRIVACY_CONSENT_VERSION` | 필수 | 서버의 현재 필수 개인정보 처리방침 버전. 누락·공백이면 기동 실패 |
| `TERM_CONSENT_VERSION` | 필수 | 서버의 현재 필수 이용약관 버전. 누락·공백이면 기동 실패 |
| `QUALITY_REVIEW_CONSENT_VERSION` | 필수 | 서버의 현재 품질 검토 이용 동의 버전. 누락·공백이면 기동 실패 |
| `REFRESH_TOKEN_TTL` | 선택 | `P14D` |
| `REFRESH_TOKEN_RANDOM_BYTES` | 선택 | `32` 이상 |
| `JWT_ISSUER` | 선택 | `http://localhost:8081` |
| `JWT_AUDIENCE` | 선택 | `tosunsaeng-learning-core` |
| `JWT_KEY_ID` | 선택 | `tosunsaeng-identity-rsa-1` |
| `JWT_ACCESS_TOKEN_TTL` | 선택 | `PT30M` |
| `JWT_PRIVATE_KEY_LOCATION` | 선택 | 로컬 PKCS#8 Private Key Resource 경로 |
| `JWT_PUBLIC_KEY_LOCATION` | 선택 | 로컬 X.509 Public Key Resource 경로 |
| `FIREBASE_AUTH_ENABLED` | 선택 | Firebase adapter 전체 활성화 여부, 기본값 `false` |
| `FIREBASE_PROJECT_ID` | Firebase 활성화 시 필수 | 허용할 Firebase project ID |
| `FIREBASE_TENANT_ID` | 선택 | Identity Platform tenant를 사용할 때만 지정 |
| `FIREBASE_GOOGLE_ENABLED` | 선택 | Google provider kill switch, 기본값 `false` |
| `FIREBASE_APPLE_ENABLED` | 선택 | Apple provider kill switch, 기본값 `false` |
| `FIREBASE_KAKAO_ENABLED` | 선택 | Kakao OIDC provider kill switch, 기본값 `false` |
| `FIREBASE_PHONE_ENABLED` | 선택 | Firebase phone proof kill switch, 기본값 `false` |
| `FIREBASE_KAKAO_PROVIDER_ID` | 선택 | Kakao Generic OIDC provider ID, 기본값 `oidc.kakao` |
| `FIREBASE_LOGIN_MAX_AUTHENTICATION_AGE` | 선택 | 로그인 교환 recent-auth 상한, 기본값 `PT15M` |
| `FIREBASE_HIGH_RISK_MAX_AUTHENTICATION_AGE` | 선택 | 가입·연결 등 고위험 recent-auth 상한, 기본값 `PT5M` |
| `FIREBASE_CLOCK_SKEW` | 선택 | Firebase Token 시간 허용 오차, 기본값 `PT30S` |
| `FIREBASE_CONNECT_TIMEOUT` | 선택 | Admin SDK 연결 timeout, 기본값 `PT3S` |
| `FIREBASE_READ_TIMEOUT` | 선택 | Admin SDK 읽기·쓰기 timeout, 기본값 `PT5S` |
| `FIREBASE_ENROLLMENT_TTL` | 선택 | PENDING enrollment 유효 시간, 기본값 `PT10M` |
| `FIREBASE_ENROLLMENT_CLEANUP_RETENTION` | 선택 | 만료 뒤 TTL 정리 유예, 기본값 `PT24H` |
| `PHONE_IDENTITY_FINGERPRINT_ENABLED` | 선택 | PhoneIdentity HMAC fingerprint 활성화 여부, 기본값 `false` |
| `PHONE_IDENTITY_FINGERPRINT_KEY_RING` | PhoneIdentity 활성화 시 필수 | 별도 Secret으로 주입하는 retained identity key ring |
| `PHONE_ELIGIBILITY_BINDING_ENABLED` | 선택 | consumer-scoped eligibility outbox fingerprint 활성화 여부, 기본값 `false` |
| `PHONE_ELIGIBILITY_BINDING_CONSUMER_SCOPE_ID` | eligibility binding 활성화 시 필수 | Identity가 의미를 해석하지 않는 allowlist된 opaque consumer scope |
| `PHONE_ELIGIBILITY_BINDING_KEY_RING` | eligibility binding 활성화 시 필수 | PhoneIdentity key와 분리해 Secret으로 주입하는 retained key ring |
| `PHONE_ELIGIBILITY_PUBLISHER_ENABLED` | 선택 | eligibility event HTTPS publisher 활성화 여부, 기본값 `false` |
| `PHONE_ELIGIBILITY_PUBLISHER_BASE_URL` | publisher 활성화 시 필수 | VPC Lattice Billing HTTPS origin; route는 코드에 고정 |
| `PHONE_ELIGIBILITY_PUBLISHER_REGION` | 선택 | SigV4 region, 기본값 `ap-northeast-2` |
| `PHONE_ELIGIBILITY_PUBLISHER_LEASE_DURATION` | 선택 | atomic claim lease, 기본값 `PT60S` |
| `PHONE_ELIGIBILITY_PUBLISHER_FIXED_DELAY` | 선택 | publisher polling 간격, 기본값 `PT5S` |
| `PHONE_ELIGIBILITY_PUBLISHER_MAX_ATTEMPTS` | 선택 | transient delivery 최대 시도 횟수, 기본값 `12` |
| `PHONE_ELIGIBILITY_PUBLISHER_PUBLISHED_RETENTION` | 선택 | 발행 완료 event 보존 기간, 기본값 `P30D` |
| `PHONE_ELIGIBILITY_PUBLISHER_DEAD_LETTER_REVIEW` | 선택 | dead-letter 검토 기준 기간, 기본값 `P90D`이며 자동 삭제하지 않음 |
| `OWNER_EVENT_USER_MERGED_CAPTURE_ENABLED` | 선택 | 신규 UserMerged core와 consumer별 delivery capture, 기본값 `false` |
| `OWNER_EVENT_TRIAL_REBIND_CAPTURE_ENABLED` | 선택 | phone rejoin lineage와 Billing owner event capture, 기본값 `false` |
| `OWNER_EVENT_BILLING_USER_MERGED_PUBLISHER_ENABLED` | 선택 | Billing UserMerged publisher, 기본값 `false` |
| `OWNER_EVENT_LEARNING_CORE_USER_MERGED_PUBLISHER_ENABLED` | 선택 | Learning Core UserMerged publisher, 기본값 `false` |
| `OWNER_EVENT_BILLING_TRIAL_REBIND_PUBLISHER_ENABLED` | 선택 | Billing TrialOwnerRebindApproved publisher, 기본값 `false` |
| `OWNER_EVENT_BILLING_BASE_URL` | Billing publisher 활성화 시 필수 | VPC Lattice Billing HTTPS origin |
| `OWNER_EVENT_BILLING_REGION` | 선택 | owner event SigV4 region, 기본값 `ap-northeast-2` |
| `OWNER_EVENT_LEARNING_CORE_ENDPOINT` | Learning Core publisher 활성화 시 필수 | workload JWT로 호출할 exact UserMerged HTTPS endpoint |

로컬 예시는 `.env.example`에만 제공한다. 실제 환경의 사용자 이름, 비밀번호, Secret, Token, MongoDB 주소 및 Private Key는 저장소에 커밋하지 않는다.

Firebase는 기본 비활성이다. 활성화 시 Admin SDK는 Workload Identity 또는 Application Default Credentials를 우선 사용하며 credential 파일을 저장소나 image에 포함하지 않는다. 공개 `/api/v1/auth/firebase/exchange`·`/api/v1/auth/firebase/signup`과 Identity Bearer가 필요한 `/api/v1/auth/firebase/guest/prepare`·`/api/v1/auth/firebase/guest/upgrade`·`/api/v1/auth/firebase/auth-methods/sync`는 같은 kill switch를 따른다. signup과 Guest 승격 활성화에는 PhoneIdentity와 consumer-scoped eligibility binding의 서로 다른 key ring이 모두 필요하다. 설정 하나라도 빠지면 신규 User 생성이나 Guest 승격을 비원자적으로 진행하지 않고 기동 단계에서 fail-closed한다.

## 로컬 실행

Java 21, OpenSSL과 접근 가능한 로컬 MongoDB를 준비한다. 최초 실행 전에 다음 스크립트로 Git에서 제외되는 `.local/keys` 아래에 RSA 2048비트 키 쌍을 생성한다.

```shell
./scripts/generate-local-rsa-keys.sh
```

기존 로컬 키를 의도적으로 교체할 때만 `--force`를 사용한다. Private Key는 PKCS#8 PEM과 권한 `600`, Public Key는 `PUBLIC KEY` PEM과 권한 `644`로 생성되며 키 본문은 표준 출력에 표시되지 않는다. 그다음 환경변수를 로드해 실행한다.

```shell
cp .env.example .env
set -a
source .env
set +a
./gradlew bootRun
```

Swagger UI는 `http://localhost:8081/swagger-ui.html`, OpenAPI 문서는 `http://localhost:8081/v3/api-docs`, health endpoint는 `http://localhost:8081/actuator/health`, Public Key JWKS는 `http://localhost:8081/.well-known/jwks.json`에서 확인할 수 있다. 포트를 변경했다면 URL의 포트도 함께 변경한다. 운영 환경에서 API 문서를 노출하지 않을 때는 `SWAGGER_ENABLED=false`로 설정한다.

## 운영 로그

애플리케이션은 기본적으로 ECS 형식의 구조화 JSON을 stdout에 기록한다. 안전한 `X-Request-ID`가 들어오면 응답과 MDC에 이어서 사용하고, 없거나 허용 문자와 64자 제한을 벗어나면 서버 UUID로 교체한다. API 요청 완료 로그에는 `event`, `outcome`, `requestId`, HTTP method, route template, status, duration과 오류 code만 포함한다. health·Swagger/OpenAPI·JWKS의 정상 요청은 반복 노이즈를 줄이기 위해 완료 로그에서 제외한다.

사람이 읽는 애플리케이션 정의 `message`는 한글 문장으로 기록한다. 검색·집계와 대시보드·알림 계약에 사용하는 ECS field name, `event`, `outcome`, `errorCode` 값은 안정적인 영어 식별자로 유지하며 Spring·Tomcat·MongoDB 같은 framework 자체 message와 예외 type은 번역하지 않는다.

예상 밖 5xx는 요청 filter가 `http.request.failed` ERROR 한 건만 기록하고 별도 INFO 완료 로그를 만들지 않는다. 예외 원문 message와 raw Throwable 대신 예외 type, 제한된 cause type과 message 없는 stack frame만 남긴다. 회원가입·Guest 생성·로그인·Token Rotation·재사용 탐지·로그아웃·동의 갱신·회원 탈퇴는 실제 저장 또는 Transaction commit 이후의 별도 상태 전이 event로 기록한다.

Password, Access/Refresh Token, Authorization Header, 이메일·닉네임, installationId와 그 hash, Token hash, JWT 본문, 실제 Key와 MongoDB URI 및 요청·응답 본문은 로그에 기록하지 않는다. `userId`는 필요한 상태 전이 로그에만 사용하고 metric tag에는 사용하지 않는다.

## Sentry 오류 수집

Sentry는 기본적으로 꺼져 있으며 실제 DSN은 저장소에 두지 않는다. 운영 환경에서는 `SENTRY_ENABLED=true`, 배포 Secret의 `SENTRY_DSN`, 환경별 `SENTRY_ENVIRONMENT`와 immutable `SENTRY_RELEASE`를 주입한다. Source context 업로드용 `SENTRY_AUTH_TOKEN`은 해당 기능을 승인한 CI build에서만 사용하고 애플리케이션 Runtime에는 전달하지 않는다.

`GlobalExceptionHandler`가 처리한 예상 밖 5xx만 명시적으로 한 번 수집한다. Validation·Business·Security 4xx는 수집하지 않으며 Sentry Logback integration, Sentry Logs, tracing과 profiling은 비활성화해 기존 `http.request.failed` ERROR가 두 번째 Issue가 되지 않게 한다. SDK의 기본 unhandled exception resolver 순서는 변경하지 않는다.

`beforeSend`는 최종 방어선으로 기존 event를 그대로 보내지 않고 새 event를 구성한다. 예외 message, request·response body, URL·query, Header·Cookie, user, breadcrumb, extra, thread, runtime context와 알 수 없는 확장 필드는 제거한다. 전송을 허용하는 정보는 message 없는 예외 type·정제된 stack frame, 검증된 `requestId`, `errorCode`, HTTP method·route template·5xx status, 설정에서 읽은 environment·release와 source context 연결에 필요한 UUID 형식의 JVM debug bundle ID뿐이다. 테스트 프로필은 Sentry를 명시적으로 끄며 통합 테스트는 외부 통신 없이 test transport가 받은 최종 event JSON 전체의 민감정보 비노출과 4xx 0건·handled 5xx 1건을 검증한다.

## 테스트

```shell
./gradlew clean test
```

테스트 프로필은 MongoDB 자동 설정을 제외하므로 Atlas, OAuth Provider 또는 다른 외부 인프라에 연결하지 않는다.

Firebase Stage 0 PoC는 Java 기본 테스트와 분리돼 있다. production Firebase project나 credential 없이 demo project ID와 Auth Emulator만 사용하는 재현 방법은 `poc/firebase-auth/README.md`, 검증 결과와 남은 외부 gate는 `docs/poc/firebase-auth-broker-stage-0.md`를 따른다. Kakao 공개 discovery 테스트는 외부 HTTPS를 사용하므로 별도 opt-in 명령으로만 실행한다.

## 인증 수명 주기

- `POST /api/v1/auth/login`은 RS256 Access Token과 Opaque Refresh Token을 발급하고 Refresh Token의 해시만 `RefreshSession`에 저장한다.
- `POST /api/v1/auth/guest`는 설치 단위 Guest를 생성한 뒤 동일한 Access/Refresh Token 발급 구조를 사용한다.
- `POST /api/v1/auth/reissue`는 기존 RefreshSession을 폐기한 뒤 Access Token과 Refresh Token을 모두 Rotation한다. 만료 여부는 MongoDB TTL 삭제 시점에 의존하지 않고 애플리케이션에서 직접 검증한다.
- `POST /api/v1/auth/logout`은 RefreshSession을 멱등적으로 폐기하며 Access Token 블랙리스트를 만들지 않는다.
- `POST /api/v1/auth/logout-all`은 인증된 사용자의 활성 RefreshSession 전체를 `LOGOUT_ALL` 사유로 멱등적으로 폐기한다.
- `POST /api/v1/users/withdraw`는 JWT subject와 현재 RefreshSession 소유권을 확인하고 LOCAL 비밀번호를 재검증한 뒤 User tombstone과 모든 활성 Session 폐기를 원자적으로 저장한다.

단일 또는 전체 로그아웃 전에 발급된 Access Token은 자체 만료 시각까지 유효할 수 있다. 클라이언트는 로그아웃 성공 직후 로컬 Access Token과 Refresh Token을 모두 삭제해야 한다.

## Guest 인증

앱 최초 실행에서는 다음 흐름을 사용한다.

```text
앱 최초 실행
→ UUID v4 installationId 생성
→ 현재 개인정보 처리방침 및 이용약관 확인·동의
→ POST /api/v1/auth/guest
→ Access/Refresh Token을 클라이언트 안전 저장소에 저장
→ Learning Core API 호출

앱 재실행
→ 저장된 Refresh Token으로 POST /api/v1/auth/reissue
→ Rotation된 Access/Refresh Token 저장
```

`installationId`는 인증 자격 증명이 아니라 중복 Guest 생성을 막기 위한 식별자다. 서버는 앞뒤 공백을 제거하고 UUID v4를 표준 소문자 표현으로 정규화한 뒤 SHA-256을 적용한 Base64URL 무패딩 해시만 `User.guestInstallationIdHash`에 저장한다. 원문과 해시는 JWT Claim에 포함하지 않는다.

서버는 Guest UUID와 기존 RS256 Access Token, Opaque Refresh Token 및 해시된 RefreshSession을 영속성 작업 전에 준비한다. 그다음 MongoDB Transaction 안에서 User와 RefreshSession을 저장하고 응답 객체까지 구성한 뒤 commit한다. `guestInstallationIdHash` partial unique index가 동시 요청을 포함한 중복 생성의 최종 방어선이다. 같은 설치 ID가 이미 존재하면 새 User나 Token을 발급하지 않고 `409 GUEST_ALREADY_EXISTS`를 반환한다.

Access Token 또는 Refresh Token 준비, User 저장, RefreshSession 저장이나 응답 객체 구성에서 서버 내부 실패가 발생하면 Transaction 전체가 rollback되어 User와 RefreshSession이 모두 남지 않는다. 이 경우 같은 `installationId`로 정상 재시도할 수 있다.

Transaction commit 후 네트워크에서 응답을 잃거나 클라이언트가 Token을 분실하면 Guest는 이미 정상 생성된 상태다. 같은 `installationId` 재요청은 `409 GUEST_ALREADY_EXISTS`이며 설치 ID만으로 기존 Guest 계정이나 Token을 복구하지 않는다. 앱 삭제나 기기 변경에서도 기록 복구가 제한될 수 있다. 정상 응답으로 받은 Refresh Token이 이후 인증 상태를 증명하며, 복구·계정 연결은 별도 후속 기능으로 다룬다.

현재 서비스에는 재사용할 Rate Limit 인프라가 없으므로 Guest 대량 생성 방어는 남은 위험이다. 이 작업에서 Redis나 외부 의존성을 추가하지 않았으며, 기존 Gateway 또는 향후 Identity Rate Limit 표준을 확정한 뒤 후속 이슈로 적용해야 한다.

### 회원 탈퇴

`POST /api/v1/users/withdraw`는 Bearer Access Token이 필요한 보호 API다. 탈퇴 대상은 요청 값이 아닌 검증된 JWT `sub`로만 식별하며, 요청에는 현재 Refresh Token과 Provider별 재인증 정보만 전달한다.

LOCAL 요청:

```shell
curl -X POST \
  "${IDENTITY_BASE_URL}/api/v1/users/withdraw" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H 'Content-Type: application/json' \
  --data '{
    "refreshToken": "<current-refresh-token>",
    "password": "<current-password>"
  }'
```

GUEST 요청:

```shell
curl -X POST \
  "${IDENTITY_BASE_URL}/api/v1/users/withdraw" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H 'Content-Type: application/json' \
  --data '{
    "refreshToken": "<current-refresh-token>"
  }'
```

요청 DTO에는 `userId`, `installationId`, `isConfirmed`가 없다. LOCAL은 현재 비밀번호를 기존 PasswordEncoder로 확인하며 GUEST는 비밀번호를 사용하지 않는다. 두 Provider 모두 Refresh Token 해시로 찾은 Session이 JWT subject 사용자 소유이고 미폐기·미만료인지 확인한다. 다른 사용자 Session, 만료·폐기 Session과 비밀번호 불일치는 외부에서 세부 원인을 구분하지 않는 `401 INVALID_WITHDRAWAL_CREDENTIALS`로 처리한다.

탈퇴 Transaction은 User를 물리 삭제하지 않고 다음 tombstone으로 갱신한다.

```text
status = WITHDRAWN
withdrawnAt = 서버 UTC 시각
updatedAt = withdrawnAt
nickname = 탈퇴한 사용자
email / normalizedEmail / passwordHash / guestInstallationIdHash = unset
userId / provider / createdAt / consents = 유지
```

같은 Transaction에서 해당 userId의 모든 미폐기 RefreshSession을 동일 시각과 `ACCOUNT_WITHDRAWN` 사유로 폐기한다. 조건부 User update가 기존 `status`와 `updatedAt`을 비교하므로 동시에 변경된 User를 조용히 덮어쓰지 않는다. 충돌 시 최신 User를 다시 읽어 WITHDRAWN이면 멱등 성공하고, 아직 탈퇴 전 상태이면 자격 증명을 다시 검증해 한 번 재시도한 뒤에도 충돌하면 `409 WITHDRAWAL_CONFLICT`를 반환한다. 동의 갱신도 ACTIVE 상태와 `updatedAt`이 일치할 때 `consents` 필드만 partial update하여 오래된 User 객체가 WITHDRAWN 상태를 ACTIVE로 복구하지 못한다.

Guest 탈퇴는 `guestInstallationIdHash` unique 점유를 해제한다. 이후 같은 installationId로 Guest 인증하면 기존 WITHDRAWN User를 복구하지 않고 새 UUID와 새 RefreshSession을 생성한다. 반면 ACTIVE Guest가 점유 중인 installationId 요청은 기존처럼 `409 GUEST_ALREADY_EXISTS`다. LOCAL도 이메일 필드를 unset하므로 문자열에만 적용되는 partial unique index에서 기존 이메일 점유가 해제되어 새 User로 재가입할 수 있다.

이미 WITHDRAWN인 사용자가 아직 유효한 Access Token으로 같은 API를 다시 호출하면 기존 `withdrawnAt`을 반환하는 200 멱등 성공이다. 탈퇴 성공 후에는 모든 Refresh Token이 즉시 재발급 불가능해지며 클라이언트는 Access/Refresh Token을 모두 삭제해야 한다. Access Token은 stateless JWT이므로 기본 `PT30M` TTL 또는 배포 설정의 만료 시각 전까지 Learning Core 같은 외부 검증 서비스에서 암호학적으로 유효할 수 있다. 이번 범위에는 denylist나 introspection을 추가하지 않는다.

### 필수 정책 및 품질 검토 이용 선택 동의

Guest 생성과 LOCAL 회원가입은 개인정보 처리 동의와 이용약관 동의를 모두 필수로 받는다. 클라이언트가 보낸 두 버전은 서버 설정의 현재 버전과 정확히 일치해야 한다. 품질 검토 이용은 선택 동의이므로 false여도 Guest 생성과 일반 기능을 차단하지 않는다. 동의 시각은 요청에서 받지 않고 서버 `Instant`로 기록한다. 기존 `isAudioConsent` 계약은 지원하지 않는다.

Guest 최초 생성 요청 예시는 다음과 같다.

```shell
curl -X POST \
  "${IDENTITY_BASE_URL}/api/v1/auth/guest" \
  -H 'Content-Type: application/json' \
  --data '{
    "installationId": "550e8400-e29b-41d4-a716-446655440000",
    "isPrivacyConsented": true,
    "privacyConsentVersion": "privacy-v1",
    "isTermConsented": true,
    "termConsentVersion": "term-v1",
    "isQualityReviewConsented": false,
    "qualityReviewConsentVersion": "quality-review-v1"
  }'
```

`POST /api/v1/auth/signup`은 이메일·비밀번호·닉네임과 기존 필수 동의 네 필드만 요구하며 품질 검토 이용 상태는 `false/null/null`로 초기화한다. Guest는 요청한 선택 상태를 함께 저장한다. 두 생성 경로 모두 품질 검토 이용을 묵시적으로 true로 만들지 않는다.

```json
{
  "consents": {
    "privacyConsented": true,
    "privacyConsentVersion": "privacy-v1",
    "privacyConsentedAt": "서버가 기록한 UTC 시각",
    "termConsented": true,
    "termConsentVersion": "term-v1",
    "termConsentedAt": "서버가 기록한 UTC 시각",
    "qualityReviewConsented": false,
    "qualityReviewConsentVersion": null,
    "qualityReviewConsentedAt": null
  }
}
```

로그인 또는 Guest 인증을 마친 사용자는 다음 API로 저장된 동의 상태와 서버의 현재 필수 버전을 조회한다. 요청 파라미터나 본문으로 사용자를 선택하지 않으며 검증된 JWT `sub`만 사용한다.

```shell
curl -X GET \
  "${IDENTITY_BASE_URL}/api/v1/users/me/consents" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H 'Accept: application/json'
```

```json
{
  "isSuccess": true,
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "result": {
    "privacy": {
      "currentVersion": "privacy-v2",
      "consented": true,
      "consentedVersion": "privacy-v1",
      "consentedAt": "2026-08-05T07:00:00Z",
      "requiresConsent": true
    },
    "terms": {
      "currentVersion": "term-v1",
      "consented": true,
      "consentedVersion": "term-v1",
      "consentedAt": "2026-08-05T07:00:00Z",
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
}
```

privacy와 terms의 `requiresConsent`는 저장된 동의가 false이거나 저장 버전이 null·공백이거나 현재 필수 버전과 정확히 일치하지 않으면 true다. Quality review는 저장 상태가 true이고 저장 version이 현재 version과 일치하며 서버 동의 시각이 있을 때만 현재 `consented=true`다. 선택 동의이므로 갱신하지 않아도 `requiresConsent`는 항상 false다. 버전의 대소 관계나 대소문자를 보정하지 않는다.

프론트는 로그인 또는 Guest 인증 후 다음 순서로 사용한다.

```text
GET /api/v1/users/me/consents
→ privacy.requiresConsent 또는 terms.requiresConsent 확인
→ 하나라도 true이면 새로운 동의 화면 표시
→ 필수 재동의 또는 품질 검토 이용 선택·철회 시 PUT /api/v1/users/me/consents 호출
```

신규 Guest는 아직 Access Token이 없으므로 GET 조회를 먼저 호출할 수 없다. 앱에 포함된 현재 필수 버전으로 동의받은 뒤 기존 `POST /api/v1/auth/guest` 흐름을 사용한다.

인증된 사용자는 다음 API로 현재 필수 버전에 다시 동의하면서 품질 검토 이용을 선택하거나 철회한다.

```shell
curl -X PUT \
  "${IDENTITY_BASE_URL}/api/v1/users/me/consents" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H 'Content-Type: application/json' \
  --data '{
    "isPrivacyConsented": true,
    "privacyConsentVersion": "privacy-v1",
    "isTermConsented": true,
    "termConsentVersion": "term-v1",
    "isQualityReviewConsented": false,
    "qualityReviewConsentVersion": "quality-review-v1"
  }'
```

세 동의 상태는 같은 User 문서의 단일 MongoDB 저장으로 함께 반영된다. 동일 상태·동일 버전 요청은 저장을 반복하지 않고 기존 동의 시각과 User `updatedAt`을 유지한다. Quality review true는 현재 version이 정확히 일치해야 하며 false 철회는 stale version으로 차단하지 않고 저장 상태를 `false/null/null`로 정리한다. 호환 기간에는 Quality review 요청 필드가 모두 누락돼도 false로 처리한다. 제공된 version 문자열은 trim한 뒤 최대 100자와 영문·숫자·점·밑줄·하이픈 형식을 검증한다.

MongoDB는 스키마리스이므로 새 embedded 필드를 추가하기 위한 파괴적 migration이나 index 변경은 없다. 기존 문서에 Quality review 필드가 없으면 `false/null/null`로 읽는다. 과거 `audioConsent` 값은 다른 동의로 자동 변환하지 않고 읽기에서 무시한다. 현재 snapshot만으로 최초 미동의와 철회를 구분할 수 없으므로 법적 감사 이력이 필요하면 별도 append-only 모델을 도입해야 한다.

Identity의 false 저장만으로 답안·음성 같은 외부 소유 데이터의 이용 중지가 완료되지는 않는다. versioned outbox event, 데이터 소유 서비스의 멱등 consumer와 승인된 보존·삭제 정책이 준비되기 전에는 실제 품질 검토 이용을 활성화하지 않는다.

### ECS Task Definition 환경변수 전환

이 저장소에는 ECS Task Definition 파일이 없으므로 배포 인프라의 새 Task Definition revision에서 다음 값을 직접 변경한다.

1. 필수 `PRIVACY_CONSENT_VERSION`을 현재 배포할 개인정보 처리방침 버전으로 추가한다.
2. 필수 `TERM_CONSENT_VERSION`을 현재 배포할 이용약관 버전으로 추가한다.
3. 필수 `QUALITY_REVIEW_CONSENT_VERSION`을 현재 배포할 품질 검토 이용 동의 버전으로 추가한다.
4. 더 이상 읽지 않는 `AUDIO_POLICY_VERSION`을 제거한다.
5. 새 revision을 staging에 먼저 배포해 Guest 생성, LOCAL 회원가입, 동의 상태 조회·갱신과 프로필 응답을 확인한 뒤 production에 적용한다.

이번 변경은 구 `isAudioConsent` 요청과 호환되지 않는 계약 변경이다. 새 프론트도 구 서버에서는 필수 음성 동의가 없어 실패하므로, ECS 환경변수를 포함한 백엔드 revision과 새 요청을 보내는 프론트를 같은 전환 창에 배포해야 한다. 이미 배포된 구 앱을 계속 지원해야 한다면 강제 업데이트 또는 별도의 명시적 과도기 API 계약이 선행되어야 하며, 설치 식별자만으로 기존 Guest를 복구하는 방식으로 우회하지 않는다.

### MongoDB Transaction 요구사항

Guest User와 최초 RefreshSession 저장, 그리고 회원 탈퇴의 User tombstone과 전체 RefreshSession 폐기는 `MongoTransactionManager`를 사용하는 Transaction으로 각각 묶는다. 운영·staging과 Guest·탈퇴 흐름을 실행하는 로컬 MongoDB는 replica set 또는 transaction을 지원하는 Atlas·sharded topology여야 하며 logical session도 지원해야 한다.

애플리케이션 시작 시 민감한 연결 정보를 출력하지 않는 `hello` 명령으로 transaction 가능 topology와 logical session 지원을 확인한다. standalone MongoDB이거나 기능 확인에 실패하면 고정된 설정 오류로 기동을 중단하므로 Guest 저장이 조용히 비원자적으로 실행되지 않는다.

배포 전에는 다음을 확인한다.

1. staging MongoDB의 `hello` 결과에 replica set 식별자 또는 transaction 지원 router 정보와 logical session 지원이 있는지 운영 도구로 확인한다.
2. User 저장 또는 RefreshSession 저장 실패를 주입해 두 collection 모두 문서가 남지 않고 같은 설치 ID 재시도가 성공하는지 검증한다.
3. 정상 생성은 User와 RefreshSession이 함께 commit되고, commit 이후 응답 유실을 가정한 재요청은 409인지 확인한다.
4. 회원 탈퇴에서 User 조건부 update 또는 Session 저장 실패를 주입해 두 collection 변경이 모두 rollback되는지 확인한다.

격리된 `test` profile은 외부 MongoDB 자동 설정을 사용하지 않으므로 실제 replica set transaction을 호출하지 않는다. 단위 테스트에서 Spring Transaction proxy의 commit·rollback 경계와 모든 실패 전파를 검증하며, 실제 Mongo transaction은 staging 배포 전 필수로 재검증한다.

### MongoDB index 이행

Guest User는 `email`/`normalizedEmail` 필드가 없으므로 LOCAL 이메일 고유성은 문자열 필드만 대상으로 하는 `uk_users_normalized_email_present` partial unique index로 유지한다. Guest 중복 방지는 `uk_users_guest_installation_id_hash` partial unique index가 담당한다.

기존 운영 DB에 `uk_users_normalized_email` index가 있다면 배포 전에 다음을 수동 운영 절차로 수행한다.

1. 백업과 롤백 절차를 확인하고 `users` collection의 현재 index와 `normalizedEmail` 중복·이상 데이터를 점검한다.
2. staging에서 신규 partial unique index 생성과 복수 Guest 삽입을 먼저 검증한다.
3. 기존 index와 같은 key pattern의 신규 index가 공존하지 못하는 MongoDB 버전은 승인된 점검 창에서 쓰기를 중지하고 legacy index를 제거한 직후 신규 partial unique index를 생성한다.
4. 신규 이메일 index의 partial/unique 옵션과 Guest hash index의 partial/unique 옵션을 확인한 뒤 쓰기와 애플리케이션을 재개한다.

애플리케이션은 기존 index를 자동 drop하지 않는다. 운영 절차가 완료되기 전에는 Guest 배포를 진행하지 않는다.

`uk_users_guest_installation_id_hash` 충돌만 `409 GUEST_ALREADY_EXISTS`로 변환한다. `_id`, 이메일 또는 알 수 없는 다른 unique index 충돌은 내부 persistence 오류로 처리하며 index 이름, keyValue, 설치 ID 해시와 문서 내용은 응답이나 로그에 노출하지 않는다.

회원 탈퇴의 활성 Session 조회를 위해 `refresh_sessions`에는 `ix_refresh_sessions_user_id_revoked_at` 비고유 compound index가 필요하다. 애플리케이션은 annotation으로 index 계약을 선언하지만, 운영에서는 새 revision 기동 전에 staging에서 데이터 규모와 build 시간을 확인하고 승인된 방식으로 `{ userId: 1, revokedAt: 1 }` index를 먼저 생성·검증한다. 기존 index를 자동 삭제하지 않는다.

## API 인증 정책

다음 경로는 Access Token 없이 접근할 수 있다.

- `POST /api/v1/auth/check-email`
- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/guest`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/reissue`
- `POST /api/v1/auth/logout`
- `GET /.well-known/jwks.json`
- `GET /actuator/health`
- Swagger UI와 OpenAPI 경로

그 밖의 경로는 기본적으로 인증이 필요하다. `GET /api/v1/users/me`, `GET`·`PUT /api/v1/users/me/consents`, `POST /api/v1/users/withdraw`와 `POST /api/v1/auth/logout-all`은 RS256 서명, `typ`, `kid`, 만료·활성 시각, issuer와 audience 검증을 통과한 Access Token만 허용한다. 사용자 식별자는 Request 값이 아니라 검증된 JWT `sub`의 UUID만 사용한다.

## 아직 구현되지 않은 기능

- 소셜 로그인
- 사용자 프로필 수정
- 정책 버전별 append-only 동의 감사 이력
- 다중 Active/Retiring Key를 지원하는 Key Rotation
- `UserWithdrawn` outbox 발행과 Learning Core 시험·결과 데이터 삭제 또는 익명화
- 탈퇴 즉시 외부 서비스의 기존 stateless Access Token까지 차단하는 서비스 간 폐기 계약
