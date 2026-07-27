# 토선생 Identity Service

토선생 앱의 사용자 신원과 인증 수명 주기를 소유하는 Spring Boot 서비스다. 현재 이메일 회원가입·로그인, RS256 Access Token, Opaque Refresh Token Rotation, 로그아웃과 Public Key 전용 JWKS endpoint가 구현되어 있다.

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

## 환경변수

| 이름 | 필수 여부 | 기본값 또는 용도 |
| --- | --- | --- |
| `MONGODB_URI` | 필수 | Identity MongoDB 연결 주소 |
| `MONGODB_DATABASE` | 선택 | `to-teacher-identity` |
| `SERVER_PORT` | 선택 | `8081` |
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

Swagger UI는 `http://localhost:8081/swagger-ui.html`, OpenAPI 문서는 `http://localhost:8081/v3/api-docs`, health endpoint는 `http://localhost:8081/actuator/health`, Public Key JWKS는 `http://localhost:8081/.well-known/jwks.json`에서 확인할 수 있다. 포트를 변경했다면 URL의 포트도 함께 변경한다.

## 테스트

```shell
./gradlew clean test
```

테스트 프로필은 MongoDB 자동 설정을 제외하므로 Atlas, OAuth Provider 또는 다른 외부 인프라에 연결하지 않는다.

## 인증 수명 주기

- `POST /api/v1/auth/login`은 RS256 Access Token과 Opaque Refresh Token을 발급하고 Refresh Token의 해시만 `RefreshSession`에 저장한다.
- `POST /api/v1/auth/reissue`는 기존 RefreshSession을 폐기한 뒤 Access Token과 Refresh Token을 모두 Rotation한다. 만료 여부는 MongoDB TTL 삭제 시점에 의존하지 않고 애플리케이션에서 직접 검증한다.
- `POST /api/v1/auth/logout`은 RefreshSession을 멱등적으로 폐기하며 Access Token 블랙리스트를 만들지 않는다.

로그아웃 전에 발급된 Access Token은 자체 만료 시각까지 유효할 수 있다. 클라이언트는 로그아웃 성공 직후 로컬 Access Token과 Refresh Token을 모두 삭제해야 한다.

## 아직 구현되지 않은 기능

- 소셜 로그인
- 사용자 프로필과 음성 데이터 수집 동의 API
- JWT Resource Server 인증 강제와 보호 API 정책
- 다중 Active/Retiring Key를 지원하는 Key Rotation

Bootstrap의 보안 정책은 모든 요청을 임시로 허용한다. 실제 인증 기능을 추가할 때 공개 경로를 제외한 API를 보호해야 한다.
