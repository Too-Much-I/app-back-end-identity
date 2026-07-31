# 토선생 Identity Service

토선생 앱의 사용자 신원과 인증 수명 주기를 소유하는 Spring Boot 서비스다. 현재 이메일 회원가입·로그인, Guest 인증, RS256 Access Token 발급·검증, Opaque Refresh Token Rotation, 단일·전체 로그아웃, 내 프로필 조회와 Public Key 전용 JWKS endpoint가 구현되어 있다.

## 도메인 범위

Identity Service는 다음 기능을 소유한다.

- 사용자 계정과 프로필
- 이메일 회원가입과 로그인
- 소셜 로그인
- 비밀번호 해시
- Access Token 발급과 Refresh Token 세션
- 로그아웃
- 음성 데이터 수집 동의

시험, 시험 문제, AI 채점, 시험 결과, 10초 챌린지, 스트릭, 단어장, 음성 파일 및 AWS S3 업로드는 Learning Core 또는 다른 서비스의 책임이며 이 저장소에 구현하지 않는다. 서버 간 JWT 계약은 `docs/contracts/identity-learning-jwt.md`를 따른다.

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
- `global.response`, `global.exception`: 공통 응답 계약과 전역 HTTP 예외 변환

## 환경변수

| 이름 | 필수 여부 | 기본값 또는 용도 |
| --- | --- | --- |
| `MONGODB_URI` | 필수 | Identity MongoDB 연결 주소 |
| `MONGODB_DATABASE` | 선택 | `to-teacher-identity` |
| `SERVER_PORT` | 선택 | `8081` |
| `SWAGGER_ENABLED` | 선택 | Swagger UI와 OpenAPI 문서 활성화 여부, 기본값 `true` |
| `AUDIO_POLICY_VERSION` | 선택 | `audio-policy-v1` |
| `REFRESH_TOKEN_TTL` | 선택 | `P14D` |
| `REFRESH_TOKEN_RANDOM_BYTES` | 선택 | `32` 이상 |
| `JWT_ISSUER` | 선택 | `http://localhost:8081` |
| `JWT_AUDIENCE` | 선택 | `tosunsaeng-learning-core` |
| `JWT_KEY_ID` | 선택 | `tosunsaeng-identity-rsa-1` |
| `JWT_ACCESS_TOKEN_TTL` | 선택 | `PT30M` |
| `JWT_PRIVATE_KEY_LOCATION` | 선택 | 로컬 PKCS#8 Private Key Resource 경로 |
| `JWT_PUBLIC_KEY_LOCATION` | 선택 | 로컬 X.509 Public Key Resource 경로 |

로컬 예시는 `.env.example`에만 제공한다. 실제 환경의 사용자 이름, 비밀번호, Secret, Token, MongoDB 주소 및 Private Key는 저장소에 커밋하지 않는다.

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

## 테스트

```shell
./gradlew clean test
```

테스트 프로필은 MongoDB 자동 설정을 제외하므로 Atlas, OAuth Provider 또는 다른 외부 인프라에 연결하지 않는다.

## 인증 수명 주기

- `POST /api/v1/auth/login`은 RS256 Access Token과 Opaque Refresh Token을 발급하고 Refresh Token의 해시만 `RefreshSession`에 저장한다.
- `POST /api/v1/auth/guest`는 설치 단위 Guest를 생성한 뒤 동일한 Access/Refresh Token 발급 구조를 사용한다.
- `POST /api/v1/auth/reissue`는 기존 RefreshSession을 폐기한 뒤 Access Token과 Refresh Token을 모두 Rotation한다. 만료 여부는 MongoDB TTL 삭제 시점에 의존하지 않고 애플리케이션에서 직접 검증한다.
- `POST /api/v1/auth/logout`은 RefreshSession을 멱등적으로 폐기하며 Access Token 블랙리스트를 만들지 않는다.
- `POST /api/v1/auth/logout-all`은 인증된 사용자의 활성 RefreshSession 전체를 `LOGOUT_ALL` 사유로 멱등적으로 폐기한다.

단일 또는 전체 로그아웃 전에 발급된 Access Token은 자체 만료 시각까지 유효할 수 있다. 클라이언트는 로그아웃 성공 직후 로컬 Access Token과 Refresh Token을 모두 삭제해야 한다.

## Guest 인증

앱 최초 실행에서는 다음 흐름을 사용한다.

```text
앱 최초 실행
→ UUID v4 installationId 생성
→ 음성 데이터 수집·이용 동의
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

### MongoDB Transaction 요구사항

Guest User와 최초 RefreshSession은 `MongoTransactionManager`를 사용하는 하나의 Transaction으로 저장한다. 운영·staging과 Guest 흐름을 실행하는 로컬 MongoDB는 replica set 또는 transaction을 지원하는 Atlas·sharded topology여야 하며 logical session도 지원해야 한다.

애플리케이션 시작 시 민감한 연결 정보를 출력하지 않는 `hello` 명령으로 transaction 가능 topology와 logical session 지원을 확인한다. standalone MongoDB이거나 기능 확인에 실패하면 고정된 설정 오류로 기동을 중단하므로 Guest 저장이 조용히 비원자적으로 실행되지 않는다.

배포 전에는 다음을 확인한다.

1. staging MongoDB의 `hello` 결과에 replica set 식별자 또는 transaction 지원 router 정보와 logical session 지원이 있는지 운영 도구로 확인한다.
2. User 저장 또는 RefreshSession 저장 실패를 주입해 두 collection 모두 문서가 남지 않고 같은 설치 ID 재시도가 성공하는지 검증한다.
3. 정상 생성은 User와 RefreshSession이 함께 commit되고, commit 이후 응답 유실을 가정한 재요청은 409인지 확인한다.

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

그 밖의 경로는 기본적으로 인증이 필요하다. `GET /api/v1/users/me`와 `POST /api/v1/auth/logout-all`은 RS256 서명, `typ`, `kid`, 만료·활성 시각, issuer와 audience 검증을 통과한 Access Token만 허용한다. 사용자 식별자는 Request 값이 아니라 검증된 JWT `sub`의 UUID만 사용한다.

## 아직 구현되지 않은 기능

- 소셜 로그인
- 사용자 프로필 수정과 음성 데이터 수집 동의 철회 API
- 다중 Active/Retiring Key를 지원하는 Key Rotation
