# Codex Current State

이 문서는 Codex 세션 시작 시 추가 개발자 컨텍스트로 읽힌다. Secret, Token, Password, 실제 Key, 전체 MongoDB URI를 기록하지 않는다.

## 프로젝트

- 이름: `app-back-end-identity`
- 현재 단계: User 도메인 기반 완료
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

## 진행 중

- 없음 — User 도메인 기반 완료

## 다음 작업

- 이메일 회원가입 Request/Response DTO와 validation 정책 설계
- `UserFactory`와 `UserRepository`를 사용하는 회원가입 Service 및 API 구현
- 정규화 이메일 사전 중복 확인과 함께 MongoDB `DuplicateKeyException`을 최종 방어선으로 처리하고 도메인 오류로 변환
- 비밀번호 길이·복잡도와 닉네임 입력 정책 결정
- Repository를 Mock 처리한 회원가입 성공·중복·저장 충돌 단위 테스트

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

- 이메일 중복 확인과 회원가입
- 이메일 로그인
- Access Token 발급
- RSA Key 로딩과 JWKS endpoint
- Refresh Token과 로그아웃
- 소셜 로그인
- 사용자 프로필과 음성 데이터 수집 동의 API

## 남아 있는 위험 요소

- 임시 `anyRequest().permitAll()`을 인증 기능 도입 시 보호 정책으로 교체해야 한다.
- 회원가입에서는 사전 중복 조회만 신뢰하지 않고 동시 요청으로 발생할 수 있는 MongoDB `DuplicateKeyException`을 처리해야 한다.
- 현재 자동 index 생성은 초기 개발 편의를 위한 설정이며, 운영에서는 권한·데이터 규모·무중단 배포를 고려한 별도 index 관리 정책이 필요하다.
- 사용자 정보 변경 기능을 추가할 때 `updatedAt` 갱신 책임과 동시 수정 정책을 명확히 해야 한다.
- 민감한 validation 필드명이 추가되면 마스킹 목록도 갱신해야 한다.
- 운영 환경의 MongoDB 연결과 health 상태는 배포 환경에서 별도로 검증해야 한다.
- 예외 타입만 기록하는 현재 정책을 보완할 운영 관측성 기준이 필요하다.

## Codex Hook 운영 메모

- 프로젝트 로컬 Hook은 이 저장소와 각 Hook 정의가 신뢰된 경우에만 실행된다.
- Codex CLI에서 `/hooks`를 열어 `.codex/hooks.json`의 명령을 검토하고 신뢰해야 한다.
- Hook 명령이 변경되면 정의의 hash가 달라지므로 `/hooks`에서 다시 검토하고 신뢰한다.
- 모든 작업 종료 전에 WORKLOG를 append하고 이 문서를 최신 상태로 갱신한다.
