# 토선생 Identity Service

토선생 앱의 사용자 신원과 인증 수명 주기를 소유하는 Spring Boot 서비스다. 현재는 실제 인증 기능을 구현하기 전 공통 서버 실행 환경과 API 기반을 구성한 Bootstrap 단계다.

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

로컬 예시는 `.env.example`에만 제공한다. 실제 환경의 사용자 이름, 비밀번호, Secret, Token, MongoDB 주소 및 Private Key는 저장소에 커밋하지 않는다.

## 로컬 실행

Java 21과 접근 가능한 로컬 MongoDB를 준비한 뒤 다음과 같이 실행한다.

```shell
cp .env.example .env
set -a
source .env
set +a
./gradlew bootRun
```

Swagger UI는 `http://localhost:8081/swagger-ui.html`, OpenAPI 문서는 `http://localhost:8081/v3/api-docs`, health endpoint는 `http://localhost:8081/actuator/health`에서 확인할 수 있다. 포트를 변경했다면 URL의 포트도 함께 변경한다.

## 테스트

```shell
./gradlew clean test
```

테스트 프로필은 MongoDB 자동 설정을 제외하므로 Atlas, OAuth Provider 또는 다른 외부 인프라에 연결하지 않는다.

## 아직 구현되지 않은 기능

- User Entity와 Repository
- 회원가입과 이메일 중복 확인
- 로그인과 PasswordEncoder
- Access Token 발급, RSA Key 로딩 및 JWKS endpoint
- Refresh Token과 로그아웃
- 소셜 로그인
- 사용자 프로필과 음성 데이터 수집 동의 API

Bootstrap의 보안 정책은 모든 요청을 임시로 허용한다. 실제 인증 기능을 추가할 때 공개 경로를 제외한 API를 보호해야 한다.
