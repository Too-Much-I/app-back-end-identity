# Codex Current State

이 문서는 Codex 세션 시작 시 추가 개발자 컨텍스트로 읽힌다. Secret, Token, Password, 실제 Key, 전체 MongoDB URI를 기록하지 않는다.

## 프로젝트

- 이름: `app-back-end-identity`
- 현재 단계: Identity Bootstrap
- 상태 기준일: 2026-07-24

## 완료

- Spring Boot 프로젝트 생성
- Identity용 의존성 구성
- 저장소 Codex 작업 규칙 작성
- Identity–Learning Core JWT 계약 문서화
- CURRENT_STATE/WORKLOG 작업 기록 체계 구성
- SessionStart/UserPromptSubmit/Stop Hook 구성
- AGENTS.md 및 CURRENT_STATE.md의 핵심 작업 규칙 재확인
- Codex Hook Python 스크립트 문법 검사 재통과

## 진행 중

- Identity Bootstrap

## 다음 작업

- `application.yml`
- 테스트 환경
- `BaseResponse`
- 전역 예외 처리
- `SecurityConfig`
- health endpoint

## 중요 결정

- Java 21
- Spring Boot 3.4.2
- MongoDB
- 실제 `userId`는 UUID 문자열
- Access Token은 JWT RS256
- Public Key 배포는 JWKS 사용
- Learning Core `audience`는 `tosunsaeng-learning-core`
- 실제 `userId`를 Python AI 서버로 보내지 않으며 Python AI의 `user_id`는 `examId` 유지

## 아직 구현되지 않은 것

- 회원가입
- 로그인
- Access Token
- Refresh Token
- 소셜 로그인

## Codex Hook 운영 메모

- 프로젝트 로컬 Hook은 이 저장소와 각 Hook 정의가 신뢰된 경우에만 실행된다.
- Codex CLI에서 `/hooks`를 열어 `.codex/hooks.json`의 명령을 검토하고 신뢰해야 한다.
- Hook 명령이 변경되면 정의의 hash가 달라지므로 `/hooks`에서 다시 검토하고 신뢰한다.
- 모든 작업 종료 전에 WORKLOG를 append하고 이 문서를 최신 상태로 갱신한다.
