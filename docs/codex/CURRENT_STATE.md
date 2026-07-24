# Codex Current State

이 문서는 Codex 세션 시작 시 추가 개발자 컨텍스트로 읽힌다. Secret, Token, Password, 실제 Key, 전체 MongoDB URI를 기록하지 않는다.

## 프로젝트

- 이름: `app-back-end-identity`
- 현재 단계: Identity Bootstrap 완료
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
- `git diff --check`, 민감 정보 패턴 및 비소유 도메인 정적 검사 통과

## 진행 중

- 없음 — Identity Bootstrap 완료

## 다음 작업

- 이메일 회원가입 API 설계 및 구현
- UUID 기반 User 모델과 MongoDB 저장 계층 설계
- 이메일 정규화·고유성 및 동시성 정책 결정
- 안전한 비밀번호 해시 구성과 회원가입 DTO validation
- Repository와 외부 경계를 Mock 처리한 회원가입 테스트

## 중요 결정

- Java 21
- Spring Boot 3.4.2
- MongoDB
- 실제 `userId`는 UUID 문자열
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

- User Entity 및 Repository
- 이메일 중복 확인과 회원가입
- 로그인과 PasswordEncoder
- Access Token 발급
- RSA Key 로딩과 JWKS endpoint
- Refresh Token과 로그아웃
- 소셜 로그인
- 사용자 프로필과 음성 데이터 수집 동의 API

## 남아 있는 위험 요소

- 임시 `anyRequest().permitAll()`을 인증 기능 도입 시 보호 정책으로 교체해야 한다.
- 민감한 validation 필드명이 추가되면 마스킹 목록도 갱신해야 한다.
- 운영 환경의 MongoDB 연결과 health 상태는 배포 환경에서 별도로 검증해야 한다.
- 예외 타입만 기록하는 현재 정책을 보완할 운영 관측성 기준이 필요하다.

## Codex Hook 운영 메모

- 프로젝트 로컬 Hook은 이 저장소와 각 Hook 정의가 신뢰된 경우에만 실행된다.
- Codex CLI에서 `/hooks`를 열어 `.codex/hooks.json`의 명령을 검토하고 신뢰해야 한다.
- Hook 명령이 변경되면 정의의 hash가 달라지므로 `/hooks`에서 다시 검토하고 신뢰한다.
- 모든 작업 종료 전에 WORKLOG를 append하고 이 문서를 최신 상태로 갱신한다.
