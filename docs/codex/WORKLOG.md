# Codex Worklog

이 문서는 append-only 작업 기록이다. 새 기록은 파일 끝에만 추가하며, 과거 기록을 수정하거나 삭제하지 않는다. 코드 변경이 없는 분석 작업도 기록한다. Secret, Token, Password, 실제 Key, 전체 MongoDB URI는 기록하지 않는다.

## 2026-07-24 — 초기 프로젝트 및 의존성 구성 기준선

- 날짜: 2026-07-24
- 브랜치: `chore/identity-bootstrap`
- 작업 목표: Identity Service용 Spring Boot 초기 프로젝트와 필수 의존성 구성이 완료된 상태를 첫 기준선으로 기록한다.
- 변경 파일: 초기 프로젝트 기준 `build.gradle`, `settings.gradle`, Gradle Wrapper, `IdentityApplication.java`, `IdentityApplicationTests.java`, `application.properties`
- 구현 내용: Java 21 및 Spring Boot 3.4.2 기반 프로젝트가 생성되었고 Web, Security, Validation, MongoDB, OAuth2 JOSE, OAuth2 Resource Server, Actuator, OpenAPI, Lombok, Test 의존성이 구성되었다.
- 실행한 테스트와 결과: 이 기준선 작성 시점에 과거 `./gradlew clean test` 실행 결과는 확인되지 않았다.
- 유지한 계약: Identity 전용 프로젝트 경계를 유지하고 Learning Core 코드를 포함하지 않았다.
- 결정사항: Java 21, Spring Boot 3.4.2, Gradle Groovy, MongoDB, Spring Security 기반으로 진행한다.
- 위험 요소: Identity Bootstrap 구성과 회원가입, 로그인, 토큰, 소셜 로그인 기능은 아직 구현되지 않았다.
- 다음 작업: `application.yml`, 테스트 환경, `BaseResponse`, 전역 예외 처리, `SecurityConfig`, health endpoint를 구현한다.

## 2026-07-24 — Codex 규칙·JWT 계약·작업 기록 자동화 구성

- 날짜: 2026-07-24
- 브랜치: `chore/identity-bootstrap`
- 작업 목표: 애플리케이션 코드를 변경하지 않고 저장소 작업 규칙, Identity–Learning Core JWT 계약, CURRENT_STATE/WORKLOG 문서와 Codex 작업 기록 Hook을 구성한다.
- 변경 파일: `AGENTS.md`, `docs/contracts/identity-learning-jwt.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`, `.codex/hooks.json`, `.codex/hooks/load_current_state.py`, `.codex/hooks/inject_worklog_instruction.py`, `.codex/hooks/enforce_worklog.py`
- 구현 내용: Identity 소유 범위와 보안·Git·테스트·기록 규칙을 정의하고, UUID `userId`, JWT `sub`, RS256, `kid`, JWKS, `issuer`, `audience`, Python AI의 `examId` 의미를 계약으로 문서화했다. SessionStart에서 현재 상태를 주입하고, UserPromptSubmit에서 turn marker 기록 지침을 주입하며, Stop에서 marker를 검사하고 두 번째 실행에 안전 fallback을 append하도록 구성했다.
- 실행한 테스트와 결과: `python3 -m py_compile`로 Hook 스크립트 3개의 문법 검사를 통과했다. `python3 -m json.tool .codex/hooks.json` 검증을 통과했다. 격리된 임시 Git 저장소를 사용한 Hook 시나리오 검증에서 SessionStart 상태 주입, UserPromptSubmit marker 주입, Stop 첫 차단, 두 번째 fallback append, 기존 marker 종료 허용이 모두 통과했다. `git diff --check`를 통과했고 8개 파일의 끝 개행과 후행 공백 검사를 통과했다. `git ls-files --modified` 결과 기존 tracked 파일 변경은 없었다. 애플리케이션 코드와 비즈니스 로직 변경이 없어 `./gradlew clean test`는 실행하지 않았다.
- 유지한 계약: Identity 도메인 밖의 기능과 Learning Core 코드를 추가하지 않았고, 실제 `userId`는 UUID/JWT `sub`로만 다루며 Python AI에는 전달하지 않는 계약을 유지했다. 실제 Secret, Key, Token, Password, 전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 공식 Codex Hook 이벤트 구조와 JSON 출력 형식을 사용하고, 하위 디렉터리 실행을 위해 Hook 명령에서 `git rev-parse --show-toplevel`로 스크립트 경로를 결정한다. Stop 재진입 시에는 현재 브랜치, `git status --short`, `git diff --stat`만 담은 fallback을 append한다.
- 위험 요소: 프로젝트와 Hook 정의를 신뢰하기 전에는 로컬 Hook이 실행되지 않는다. 이번 구성 작업은 Hook 활성화 이전에 시작되어 이 항목에는 Codex가 주입한 turn marker가 없으며, 신뢰 후 제출되는 다음 프롬프트부터 marker가 자동 주입·검사된다.
- 다음 작업: `application.yml`, 테스트 환경, `BaseResponse`, 전역 예외 처리, `SecurityConfig`, health endpoint 순으로 Identity Bootstrap을 진행한다.

## 2026-07-24 — 저장소 핵심 규칙 및 현재 상태 확인

<!-- codex-turn:019f91cf-e90e-7722-a7d1-63edb0c7531a -->

- 날짜: 2026-07-24
- 브랜치: `chore/identity-bootstrap`
- 작업 목표: `AGENTS.md`와 `docs/codex/CURRENT_STATE.md`에 로드된 핵심 규칙과 현재 구현 상태를 확인하고 요약한다.
- 변경 파일: 필수 작업 기록을 위해 `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 갱신했다. 애플리케이션 코드, 설정, `AGENTS.md`는 변경하지 않았다.
- 구현 내용: 코드 구현 없이 기술 환경, Identity 도메인 경계, 사용자 ID와 JWT 계약, 보안·Git·테스트·작업 기록 규칙, Bootstrap 진행 상태와 후속 작업을 재확인했다.
- 실행한 테스트와 결과: 코드 및 비즈니스 로직 변경이 없어 `./gradlew clean test`는 실행하지 않았다. 문서 변경 범위와 작업 기록 marker를 정적 검사했다.
- 유지한 계약: Identity 소유 범위를 벗어난 기능을 추가하지 않았고, UUID `userId`, RS256, `kid`, JWKS, `issuer`, Learning Core audience 및 Python AI의 `examId` 의미를 포함한 기존 계약을 변경하지 않았다. 민감 정보는 기록하지 않았다.
- 결정사항: 현재 단계는 Identity Bootstrap으로 유지하며, 이번 작업은 읽기·요약과 필수 작업 기록에 한정한다.
- 위험 요소: 회원가입, 로그인, Access Token, Refresh Token, 소셜 로그인과 Bootstrap 기반 구성은 아직 구현되지 않았다.
- 다음 작업: `application.yml`, 테스트 환경, `BaseResponse`, 전역 예외 처리, `SecurityConfig`, health endpoint를 구현한다.

## 2026-07-24 — 저장소 상태 및 Codex Hook 문법 검증

<!-- codex-turn:019f91d4-6fd9-7842-ab6e-db13650a7ef9 -->

- 날짜: 2026-07-24
- 브랜치: `chore/identity-bootstrap`
- 작업 목표: 현재 Git 브랜치와 작업 트리 상태, 규칙·문서·Hook 파일 목록을 확인하고 문서 공백 및 Python Hook 문법을 검증한다.
- 변경 파일: 필수 작업 기록을 위해 `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 갱신했다. 애플리케이션 코드와 Hook 구현은 변경하지 않았다.
- 구현 내용: `git branch --show-current`, `git status`, 지정 경로의 `find`, `git diff --check`, Python `py_compile`을 실행해 저장소와 Hook 상태를 점검했다.
- 실행한 테스트와 결과: 현재 브랜치는 `chore/identity-bootstrap`으로 확인했다. Git 상태에는 `.codex/`, `AGENTS.md`, `docs/`가 untracked로 표시되었다. 지정 경로에서 규칙·계약·작업 기록 문서와 Hook 정의·스크립트 8개를 확인했다. `git diff --check`는 출력 없이 성공했다. `py_compile` 최초 실행은 샌드박스 밖의 Python 캐시 경로 쓰기 제한으로 실패했으며, 동일 명령을 승인된 권한으로 재실행해 성공했다. 애플리케이션 변경이 없어 Gradle 테스트는 실행하지 않았다.
- 유지한 계약: Identity 도메인 경계, UUID `userId`, JWT RS256·`kid`·JWKS·issuer·Learning Core audience, Python AI의 `examId` 의미 및 민감 정보 보호 계약을 변경하지 않았다.
- 결정사항: 이번 작업은 요청된 상태 조회와 정적 검증에 한정하며 Hook 또는 애플리케이션 구현을 수정하지 않는다.
- 위험 요소: 규칙·문서·Hook 경로가 모두 untracked이므로 일반 `git diff --check`만으로는 해당 파일의 변경 내용을 검증할 수 없다. 기본 Python은 바이트코드 캐시를 저장소 밖 사용자 캐시 경로에 기록한다.
- 다음 작업: untracked 파일의 포함 범위를 사용자가 검토한 뒤, `application.yml`, 테스트 환경, `BaseResponse`, 전역 예외 처리, `SecurityConfig`, health endpoint를 구현한다.

## 2026-07-24 — Identity Service Bootstrap 기반 완성

<!-- codex-turn:019f91d8-fb55-7460-ba31-ab8c3feba83f -->

- 날짜: 2026-07-24
- 브랜치: `chore/identity-bootstrap`
- 작업 목표: 회원가입과 로그인 등 실제 인증 기능 전에 공통 서버 설정, API 응답·오류 기반, 임시 보안 정책, health 및 외부 인프라 없는 테스트 환경을 완성한다.
- 변경 파일: `.env.example`, `.gitignore`, `README.md`, `src/main/resources/application.yml`, 삭제한 `src/main/resources/application.properties`, `src/test/resources/application-test.yml`, `src/main/java/web/tosunsaeng/identity/config/SecurityConfig.java`, `src/main/java/web/tosunsaeng/identity/common/response/BaseResponse.java`, `src/main/java/web/tosunsaeng/identity/common/exception/` 아래 공통 오류 파일 5개, `src/test/java/web/tosunsaeng/identity/IdentityApplicationTests.java`, `src/test/java/web/tosunsaeng/identity/common/response/BaseResponseSerializationTests.java`, `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`
- 구현 내용: 환경변수 기반 애플리케이션·MongoDB·포트 설정, Swagger/OpenAPI와 Actuator health 노출, 외부 연결을 생성하지 않는 테스트 프로필, 모든 요청을 임시 허용하면서 CSRF·세션·폼 로그인·Basic 인증과 기본 생성 계정을 비활성화한 Bootstrap 보안 구성을 추가했다. 제네릭 `BaseResponse`, HTTP 상태를 포함한 공통 오류 코드, `BusinessException`, validation 상세와 민감 필드값 제거를 포함한 전역 예외 처리를 구현했다. 로컬 환경 예시와 서비스 경계·실행법을 README에 문서화했다.
- 실행한 테스트와 결과: 최초 `./gradlew clean test`는 샌드박스의 Gradle 사용자 캐시 쓰기 제한으로 실행 전 차단되었다. 승인된 캐시 접근으로 재실행하고 테스트 프로필 보강 후 한 번 더 최종 실행했으며 두 실행 모두 `BUILD SUCCESSFUL`이었다. 최종 결과는 9개 테스트, 실패·오류·건너뜀 0개다. context 로딩, health, OpenAPI 공개 접근, 임시 permit-all, 공통 응답 성공·실패 직렬화, validation 민감값 제거, 비즈니스 오류, 예상하지 못한 오류의 내부 정보 비노출을 검증했다. `git diff --check`와 후행 공백 검사가 통과했고, 실제 자격증명·Private Key·서명 토큰 형태 검색 및 애플리케이션 코드의 비소유 도메인 검색에서 일치 항목이 없었다. `.env` 계열 ignore와 기본 생성 보안 비밀번호 로그 부재도 확인했다.
- 유지한 계약: Identity 소유 범위를 벗어난 애플리케이션 코드를 추가하지 않았고 Learning Core 코드를 복사하지 않았다. UUID 사용자 식별자, 향후 RS256·`kid`·JWKS·issuer·Learning Core audience 검증, Python AI 식별자 의미를 포함한 기존 계약을 변경하지 않았다. 실제 인증 발급·검증 기능이나 키 로딩은 추가하지 않았고 민감한 운영값을 기록하지 않았다.
- 결정사항: 성공 코드는 `SUCCESS`, 공통 실패 코드는 enum 이름을 응답 `code`로 사용하고 별도 `HttpStatus`로 HTTP 상태를 결정한다. Validation 상세는 `result` 배열로 반환하며 민감 필드의 `rejectedValue`는 `null`로 만든다. 테스트에서는 MongoDB 클라이언트·데이터·Repository 자동 설정을 모두 제외한다. 기본 생성 계정을 막기 위해 사용자 없는 인메모리 저장소를 Bootstrap 기간에만 제공한다.
- 위험 요소: 현재 `anyRequest().permitAll()`은 Bootstrap 전용이므로 인증 API 도입 시 반드시 축소해야 한다. 민감값 제거는 필드명 규칙에 의존하므로 새 자격증명 필드가 생기면 목록을 함께 갱신해야 한다. 예상하지 못한 오류는 정보 노출 방지를 위해 예외 타입만 기록하므로 향후 운영 관측성 정책을 별도로 설계해야 한다. 로컬 실행에는 유효한 MongoDB 환경 설정이 필요하다.
- 다음 작업: UUID 기반 User 모델과 저장 계층, 이메일 정규화·중복 정책, 요청 DTO validation, 안전한 비밀번호 해시 구성을 먼저 설계하고 Mock 기반 테스트와 함께 이메일 회원가입을 구현한다. 그 이후 로그인과 토큰 세션을 계약에 맞춰 단계적으로 추가한다.
