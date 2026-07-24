# Codex Current State

이 문서는 Codex 세션 시작 시 추가 개발자 컨텍스트로 읽힌다. Secret, Token, Password, 실제 Key, 전체 MongoDB URI를 기록하지 않는다.

## 프로젝트

- 이름: `app-back-end-identity`
- 현재 단계: 이메일 중복 확인 및 일반 이메일 회원가입 완료
- 상태 기준일: 2026-07-24

## 완료

- Spring Boot 프로젝트 및 Identity용 의존성 구성
- 저장소 Codex 작업 규칙과 Identity–Learning Core JWT 계약 문서화
- CURRENT_STATE/WORKLOG 작업 기록 체계와 Codex Hook 구성
- 환경변수 기반 애플리케이션 이름, MongoDB 데이터베이스 및 서버 포트 설정
- Swagger UI `/swagger-ui.html` 및 OpenAPI `/v3/api-docs` 설정
- Actuator health endpoint 노출
- 외부 MongoDB 연결을 생성하지 않는 격리된 테스트 프로필
- Bootstrap 전용 STATELESS·CSRF 비활성화·전체 임시 permit-all Security 구성
- 폼 로그인, Basic 인증 및 Spring Security 기본 생성 계정 비활성화
- 제네릭 `BaseResponse` 성공·실패 Factory
- `ErrorCode`, `CommonErrorStatus`, `BusinessException` 오류 기반
- Validation 상세 민감값 제거와 예상하지 못한 오류 정보 비노출을 포함한 전역 예외 처리
- `.env.example`, 실제 환경 파일 ignore 및 README 실행·경계 문서
- 외부 인프라를 호출하지 않는 Bootstrap 테스트 9개 통과
- `users` 컬렉션의 UUID `userId` 기반 User Document와 `ACTIVE`, `SUSPENDED`, `WITHDRAWN` 상태 모델
- 원본 대소문자를 보존하면서 앞뒤 공백을 제거한 표시용 이메일과 `Locale.ROOT` 소문자 정규화 이메일 분리
- `normalizedEmail`의 `uk_users_normalized_email` unique index 및 MongoDB 자동 index 생성 설정
- BCrypt `PasswordEncoder` Bean과 평문을 Entity에 전달하지 않는 최소 `UserFactory`
- `Instant` 기반 생성·수정 시각과 신규 사용자 `ACTIVE` 초기화
- 정규화 이메일 단건 조회·존재 확인만 제공하는 `UserRepository`
- 실제 MongoDB 연결 없이 User 도메인 기반을 검증하는 테스트를 포함해 전체 23개 통과
- `git diff --check`, 평문 필드·토큰 필드·비소유 도메인 및 생성 경계 정적 검사 통과
- `POST /api/v1/auth/check-email` 이메일 중복 확인 API와 정상 응답 기반 사용 가능 여부 반환
- `POST /api/v1/auth/signup` 일반 이메일 회원가입 API와 Request/Response validation 계약
- 이메일과 닉네임의 앞뒤 공백 제거 후 validation 및 기존 `EmailNormalizer` 기반 중복 조회
- 정규화 이메일 사전 중복 확인과 MongoDB `DuplicateKeyException`의 `EMAIL_ALREADY_EXISTS` 변환
- 비밀번호 8~64자 길이 정책과 별도 복잡도 정규식 없는 BCrypt 해시 저장
- `EMAIL_ALREADY_EXISTS`, `AUDIO_CONSENT_REQUIRED` 인증 도메인 오류와 공통 `BaseResponse` 오류 응답
- User 문서에 `AudioConsent` embedded value object를 저장하고 정책 버전을 환경 설정으로 주입
- 회원가입 시 동의 상태 `agreed=true`, 정책 버전, 동의 시각 및 null 철회 시각을 원자적으로 저장
- Repository를 Mock 처리한 Service·Controller 테스트와 전체 44개 테스트 통과
- 회원가입 성공 응답 및 오류 응답의 해시·정규화 이메일·자격증명·MongoDB 내부 정보 비노출 검증

## 진행 중

- 없음 — 이메일 중복 확인 및 일반 이메일 회원가입 완료

## 다음 작업

- 이메일 로그인에서 저장된 BCrypt 해시를 검증하고 계정 상태를 확인하는 인증 흐름 설계
- Access Token의 환경별 `issuer`, 정확한 `audience`, 만료 시간, `kid` 설정 계약 확정
- 저장소 밖 RSA Private Key 로딩과 RS256 전용 서명 구성을 구현하고 대칭키·알고리즘 혼동을 차단
- UUID `userId`를 `sub`에 넣고 `iss`, `aud`, `iat`, `exp`, `jti`, `scope`를 포함하는 Access Token 발급
- Public Key만 노출하는 JWKS endpoint와 `kid` 기반 키 조회·교체 전략 구현
- 테스트 전용 인메모리 RSA Key로 서명·Claim·JWKS를 검증하고 실제 Key나 외부 서비스를 사용하지 않는 테스트 추가

## 중요 결정

- Java 21
- Spring Boot 3.4.2
- MongoDB
- 실제 `userId`는 UUID 문자열
- User Document는 `users` 컬렉션을 사용하고 UUID 문자열 `userId`를 MongoDB `@Id`로 저장
- 이메일 정규화는 null 거부, 앞뒤 공백 제거, `Locale.ROOT` 소문자 변환만 수행
- Provider별 점 제거와 plus addressing 제거는 수행하지 않음
- `normalizedEmail`은 명시적인 unique index를 사용하며 현재 애플리케이션에서 자동 index 생성을 활성화
- 비밀번호 해시는 Spring Security의 기본 cost를 사용하는 BCrypt로 생성하고 User에는 `passwordHash`만 저장
- 비밀번호 validation은 현재 제품 명세에 따라 8자 이상 64자 이하만 적용하고 임의의 복잡도 정규식을 적용하지 않음
- 이메일과 닉네임은 앞뒤 공백을 제거한 값으로 validation하며 비밀번호는 공백을 포함한 입력값을 임의 변환하지 않음
- 이메일 중복 확인은 가입 여부와 관계없이 성공 응답을 사용하며 `isAvailable`로 결과를 구분
- 회원가입의 사전 중복과 unique index 저장 충돌은 모두 `EMAIL_ALREADY_EXISTS` 409 오류로 통일
- 회원가입의 음성 데이터 수집·이용 동의는 반드시 true이며 false는 `AUDIO_CONSENT_REQUIRED` 400 오류로 처리
- 음성 동의 정책 버전은 `app.consent.audio-policy-version`과 `AUDIO_POLICY_VERSION` 환경변수로 관리
- 현재 음성 동의 상태는 User 문서 내부의 `AudioConsent`로 저장하고 별도 이력 컬렉션은 만들지 않음
- User 생성·수정 시각 타입은 `Instant` 사용
- Access Token은 향후 JWT RS256으로 서명
- Public Key 배포는 향후 JWKS 사용
- Learning Core `audience`는 `tosunsaeng-learning-core`
- 실제 `userId`를 Python AI 서버로 보내지 않으며 Python AI의 `user_id`는 `examId` 유지
- 공통 응답 필드는 `isSuccess`, `code`, `message`, `result`
- 공통 오류 enum의 HTTP 상태와 응답 코드를 분리해 관리
- Validation 오류 상세는 `result` 배열로 반환하고 민감한 `rejectedValue`는 `null` 처리
- 테스트 프로필에서는 MongoDB 클라이언트·데이터·Repository 자동 설정 제외
- Bootstrap 동안 모든 요청을 임시 허용하며 폼 로그인과 Basic 인증은 비활성화

## 아직 구현되지 않은 것

- 이메일 로그인
- Access Token 발급
- RSA Key 로딩과 JWKS endpoint
- Refresh Token과 로그아웃
- 소셜 로그인
- 사용자 프로필 API
- 음성 데이터 수집 동의 철회 API와 별도 동의 이력 관리

## 남아 있는 위험 요소

- 임시 `anyRequest().permitAll()`을 인증 기능 도입 시 보호 정책으로 교체해야 한다.
- 현재 자동 index 생성은 초기 개발 편의를 위한 설정이며, 운영에서는 권한·데이터 규모·무중단 배포를 고려한 별도 index 관리 정책이 필요하다.
- 기존 User 문서가 운영 데이터로 존재한다면 필수 embedded 음성 동의 필드 도입 전 데이터 이행 정책이 필요하다.
- 이메일 중복 확인 공개 API와 회원가입 API에는 향후 rate limit, 자동화 요청 방어 및 abuse 관측 기준이 필요하다.
- 비밀번호 복잡도, 유출 비밀번호 차단 및 변경 정책은 제품·보안 명세 확정 후 추가해야 한다.
- 현재는 최신 음성 동의 상태만 저장하므로 동의 철회와 정책 버전 변경 시 감사 가능한 별도 이력 관리가 필요하다.
- 사용자 정보 변경 기능을 추가할 때 `updatedAt` 갱신 책임과 동시 수정 정책을 명확히 해야 한다.
- 민감한 validation 필드명이 추가되면 마스킹 목록도 갱신해야 한다.
- 운영 환경의 MongoDB 연결과 health 상태는 배포 환경에서 별도로 검증해야 한다.
- 예외 타입만 기록하는 현재 정책을 보완할 운영 관측성 기준이 필요하다.

## Codex Hook 운영 메모

- 프로젝트 로컬 Hook은 이 저장소와 각 Hook 정의가 신뢰된 경우에만 실행된다.
- Codex CLI에서 `/hooks`를 열어 `.codex/hooks.json`의 명령을 검토하고 신뢰해야 한다.
- Hook 명령이 변경되면 정의의 hash가 달라지므로 `/hooks`에서 다시 검토하고 신뢰한다.
- 모든 작업 종료 전에 WORKLOG를 append하고 이 문서를 최신 상태로 갱신한다.
