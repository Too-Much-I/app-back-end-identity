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

## 2026-07-24 — User 도메인 기반 구현

<!-- codex-turn:019f91e7-ffff-79d2-8bdb-3debaa446609 -->

- 날짜: 2026-07-24
- 브랜치: `feat/user-domain-foundation`
- 작업 목표: 외부 회원가입·로그인 API를 추가하지 않고 User Document, 정규화 이메일, 저장소 계약, BCrypt 암호화 기반과 외부 인프라 없는 단위 테스트를 구현한다.
- 변경 파일: `src/main/java/web/tosunsaeng/identity/user/domain/User.java`, `UserStatus.java`, `EmailNormalizer.java`, `UserFactory.java`, `src/main/java/web/tosunsaeng/identity/user/repository/UserRepository.java`, `src/main/java/web/tosunsaeng/identity/config/PasswordConfig.java`, `src/main/resources/application.yml`, 대응하는 테스트 파일 4개, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`
- 구현 내용: `users` 컬렉션에 UUID 문자열 `userId`를 `@Id`로 사용하는 User Document를 추가하고 표시용 이메일, 정규화 이메일, 해시, 닉네임, 상태, `Instant` 시각을 모델링했다. 정규화는 null 거부, 앞뒤 공백 제거, `Locale.ROOT` 소문자 변환만 수행한다. `normalizedEmail`에는 이름이 명시된 unique index를 적용했고 자동 index 생성을 활성화했다. BCrypt Bean과 최소 `UserFactory`를 통해 Entity에는 인코딩된 값만 전달하며, Repository는 정규화 이메일 조회와 존재 확인만 제공한다.
- 실행한 테스트와 결과: 첫 `./gradlew clean test`는 샌드박스의 Gradle 사용자 캐시 lock 파일 쓰기 제한으로 실행 전에 차단되었다. 승인된 동일 명령의 첫 실행은 테스트 assertion API 호환 문제로 컴파일 단계에서 실패했고 이를 표준 리플렉션 검사로 수정했다. 최종 `./gradlew clean test`는 `BUILD SUCCESSFUL`이었으며 전체 23개 테스트에서 실패·오류·건너뜀은 0개다. `git diff --check`와 User 코드의 평문 필드·토큰 필드·공개 생성 API·비소유 도메인 정적 검사가 통과했다.
- 유지한 계약: 실제 사용자 ID를 UUID 문자열로 생성하며 외부 요청의 `userId`를 신뢰하는 API를 추가하지 않았다. JWT·Refresh Token을 User에 저장하지 않았고 Identity 밖의 시험·채점·챌린지·스트릭·단어장 코드를 추가하지 않았다. 실제 외부 인프라, 운영 자격증명 및 개인정보를 테스트나 기록에 사용하지 않았다.
- 결정사항: User 생성은 `UserFactory`로 제한해 정규화와 BCrypt 인코딩을 한 경로에서 수행하고, Entity의 생성 Factory에는 이미 생성된 해시만 전달한다. 표시용 이메일은 대소문자를 보존하되 앞뒤 공백을 제거한다. 생성 시각과 수정 시각은 하나의 `Instant` 값으로 초기화하며 신규 상태는 `ACTIVE`다. 현재는 annotation 기반 unique index와 애플리케이션 자동 생성을 사용한다.
- 위험 요소: 사전 중복 확인만으로 동시 회원가입을 막을 수 없으므로 다음 회원가입 작업에서 MongoDB `DuplicateKeyException`을 처리해야 한다. 자동 index 생성은 운영 권한, 기존 데이터, startup 영향과 무중단 배포를 고려한 별도 관리 정책으로 대체할 수 있어야 한다. 사용자 변경 기능 도입 시 `updatedAt` 갱신과 동시 수정 정책이 필요하다. Bootstrap의 전체 요청 임시 허용 정책도 아직 남아 있다.
- 다음 작업: 회원가입 DTO validation과 도메인 오류를 설계하고, 정규화 이메일 사전 조회 및 저장 시 `DuplicateKeyException` 변환을 포함하는 회원가입 Service/API를 Repository Mock 테스트와 함께 구현한다. 로그인, 토큰, 소셜 로그인과 프로필 API는 이후 작업으로 유지한다.

## 2026-07-24 — 이메일 중복 확인 및 일반 이메일 회원가입 구현

<!-- codex-turn:019f923f-c83c-7ca2-8b8e-c6f8050cfc65 -->

- 날짜: 2026-07-24
- 브랜치: `feat/local-signup`
- 작업 목표: Identity Service에 정상 응답 기반 이메일 중복 확인 API와 음성 데이터 수집·이용 동의를 포함한 일반 이메일 회원가입 API를 구현하고, 동시 가입 충돌과 민감 정보 비노출을 검증한다.
- 변경 파일: `.env.example`, `src/main/resources/application.yml`, `src/test/resources/application-test.yml`, `src/main/java/web/tosunsaeng/identity/user/domain/User.java`, `UserFactory.java`, 새 `AudioConsent.java`, `src/main/java/web/tosunsaeng/identity/auth/` 아래 Controller·Service·Request/Response DTO·오류 enum 7개, `src/test/java/web/tosunsaeng/identity/auth/` 아래 Service·Controller 테스트 2개, `IdentityApplicationTests.java`, `UserFactoryTests.java`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`
- 구현 내용: `/api/v1/auth/check-email`에서 기존 이메일 정규화기와 Repository 존재 조회를 사용해 사용 가능 여부를 정상 응답으로 반환한다. `/api/v1/auth/signup`은 정규화 이메일 사전 중복 확인, 8~64자 비밀번호 validation, BCrypt 해시 생성, 닉네임 공백 제거, UUID/ACTIVE 사용자 생성, embedded 음성 동의 상태 저장 후 외부 응답 DTO를 반환한다. 이메일 unique index 저장 충돌은 원본 DB 상세를 버리고 사전 중복과 같은 인증 도메인 오류로 변환했다. 음성 동의 정책 버전은 환경 설정값으로 관리하고 테스트 프로필에는 가짜 고정값을 사용했다. Controller에는 두 API의 최소 OpenAPI 설명을 추가했으며 Bootstrap Security 설정은 변경하지 않았다.
- 실행한 테스트와 결과: 샌드박스 기본 권한의 첫 관련 테스트 실행은 Gradle 사용자 캐시 lock 접근 제한으로 애플리케이션 실행 전에 차단되었다. 승인된 캐시 접근으로 관련 Service·Controller·UserFactory 테스트를 실행해 28개 모두 통과했고, deprecated 테스트 API를 최신 Mockito override 방식으로 교체한 뒤 재실행도 통과했다. 최종 `./gradlew clean test`는 `BUILD SUCCESSFUL`이었으며 전체 44개 테스트에서 실패·오류·건너뜀은 0개다. `git diff --check`, 실제 자격증명·Private Key·서명 토큰·인증 포함 MongoDB URI 패턴, 민감 로그, User 평문 필드, 응답 DTO 금지 필드 및 비소유 도메인 검색이 모두 통과했다. 현재 turn marker를 사용한 Stop Hook 검사도 종료 허용 결과를 반환했다.
- 유지한 계약: 실제 사용자 ID는 서버가 생성한 UUID 문자열이며 클라이언트의 `userId`를 받지 않는다. User에는 비밀번호 해시와 현재 음성 동의 상태만 저장하고 평문 자격증명이나 Refresh Token을 추가하지 않았다. 성공 응답에는 해시, 정규화 이메일, 토큰 및 MongoDB 내부 정보를 포함하지 않았다. JWT RS256·`kid`·JWKS·issuer·Learning Core audience와 Python AI `examId` 계약은 변경하지 않았으며 이번 단계에서 로그인·토큰·RSA·JWKS 기능을 구현하지 않았다. Identity 밖의 도메인 코드와 실제 외부 인프라 호출을 추가하지 않았다.
- 결정사항: 이메일과 닉네임은 앞뒤 공백 제거 후 validation하고 이메일은 `Locale.ROOT` 소문자 정규화 값으로 중복을 판단한다. Provider별 점·plus addressing 변환은 하지 않는다. 현재 비밀번호 정책은 길이만 적용하고 임의의 복잡도 정규식을 추가하지 않는다. 음성 동의 false는 `AUDIO_CONSENT_REQUIRED` 400, 사전 중복과 저장 충돌은 `EMAIL_ALREADY_EXISTS` 409로 처리한다. 별도 동의 이력 컬렉션 없이 User 문서 안에 현재 상태를 원자적으로 저장한다.
- 위험 요소: Bootstrap 전체 허용 정책, 운영 index 관리, 공개 인증 API의 rate limit·abuse 방어, 확정되지 않은 비밀번호 복잡도·유출 차단 정책이 남아 있다. 기존 운영 User 문서가 있다면 embedded 동의 필드 도입 전 데이터 이행이 필요하며, 향후 동의 철회·정책 변경을 감사하려면 별도 이력 모델이 필요하다.
- 다음 작업: 이메일 로그인과 계정 상태 검증을 먼저 연결한 뒤, 저장소 밖 RSA Private Key 로딩, 환경별 issuer·audience·만료 시간·`kid`, UUID `sub` 및 필수 Claim을 갖춘 RS256 Access Token, Public Key 전용 JWKS endpoint와 테스트 전용 키 기반 검증을 구현한다.

## 2026-07-24 — RS256 Access Token 발급 기반 및 Public JWKS 구현

<!-- codex-turn:019f925d-cb0d-7fc2-aded-a619baefc433 -->

- 날짜: 2026-07-24
- 브랜치: `feat/access-token-jwks`
- 작업 목표: 로그인·Refresh Token·로그아웃·Learning Core 연동 없이 Identity 전용 RSA Private Key로 서명하는 RS256 Access Token 발급 기반과 Public Key 전용 표준 JWKS endpoint를 구현한다.
- 변경 파일: `.env.example`, `.gitignore`, `README.md`, `scripts/generate-local-rsa-keys.sh`, `src/main/resources/application.yml`, `src/test/resources/application-test.yml`, `src/main/java/web/tosunsaeng/identity/config/SecurityConfig.java`, 새 `src/main/java/web/tosunsaeng/identity/security/jwt/` 패키지의 `JwtProperties.java`, `RsaKeyMaterial.java`, `RsaKeyLoader.java`, `JwtConfiguration.java`, `AccessTokenIssuer.java`, `IssuedAccessToken.java`, `JwtAccessTokenIssuer.java`, `JwksController.java`, 새 테스트 패키지의 `TestRsaKeyConfiguration.java`, `JwtAccessTokenIssuerTests.java`, `JwksControllerTests.java`, `RsaKeyLoaderTests.java`, 기존 `IdentityApplicationTests.java`, `docs/contracts/identity-learning-jwt.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`
- 구현 내용: `app.jwt` Configuration Properties에 issuer, audience, keyId, `Duration` TTL, RSA Key Resource 위치와 기본 scope를 추가하고 모두 환경변수로 교체 가능하게 했다. PKCS#8 Private Key와 X.509 Public Key만 읽는 엄격한 RSA 로더는 `KeyFactory`, `PKCS8EncodedKeySpec`, `X509EncodedKeySpec`을 사용하며 파일 내용이나 하위 예외를 노출하지 않고 RSA 타입과 키 쌍 일치를 검증한다. Spring Security 6.4에 맞춰 Private Key를 포함한 `RSAKey`, `JWKSet`, `JWKSource<SecurityContext>`, `NimbusJwtEncoder`를 구성하고 운영 `Clock.systemUTC()`를 제공했다. 내부 `AccessTokenIssuer`는 UUID `userId`, 결정적인 scope 순서와 기본 scope를 사용해 RS256 Header와 최소 Claim의 Access Token을 발급하며 결과 모델의 문자열 표현에서 발급값을 제거한다. `GET /.well-known/jwks.json`은 `rsaKey.toPublicJWK()`로 만든 단일 Public JWK만 BaseResponse 없이 반환한다. 로컬 생성 스크립트는 RSA 2048비트 PKCS#8/Public Key 쌍, 제한된 파일 권한, 기존 파일 거절과 명시적 `--force` 교체를 지원하고 생성 디렉터리는 Git에서 제외했다. 회원가입 응답이나 외부 발급 endpoint는 추가하지 않았다.
- 실행한 테스트와 결과: 샌드박스 기본 권한의 첫 관련 테스트 실행은 Gradle 사용자 캐시 lock 쓰기 제한으로 실행 전에 차단되었고 승인된 캐시 접근으로 재실행했다. JWT/JWKS/RSA 로더 관련 테스트 10개는 `BUILD SUCCESSFUL`이었고, 최종 `./gradlew clean test`도 `BUILD SUCCESSFUL`로 전체 54개 테스트에서 실패·오류·건너뜀 0개였다. Header `alg`·`kid`·`typ`, 필수 Claim과 TTL, UUID `jti`, scope 기본값·정렬, Bearer 메타데이터, 올바른 Public Key의 서명 검증과 다른 키의 실패, 잘못된 UUID 거절, 자격증명·Private Key 정보 비포함, 표준 Public JWKS와 비인증 접근, PKCS#8/X.509 로딩 및 안전한 오류를 검증했다. 격리된 임시 디렉터리에서 로컬 키 스크립트의 생성 형식, 유효성, 파일 권한, 기존 파일 거절과 `--force` 교체를 검증한 뒤 임시 키를 삭제했다. `git diff --check`, Bash 문법, 실행 권한, Git ignore, 환경변수 매핑, 금지 기능·의존성, 민감 로그, 실제 PEM 본문·서명 토큰·자격증명 포함 URI 패턴 검색을 통과했으며 PEM Header 문자열은 로더와 테스트의 형식 상수 두 곳에만 존재한다.
- 유지한 계약: 실제 `userId`는 UUID 문자열이고 JWT `sub`에만 사용하며 `aud`는 `tosunsaeng-learning-core` 하나를 담은 배열이다. Identity만 RSA Private Key를 사용하고 JWKS에는 Public Key의 `kty`, `use`, `kid`, `alg`, `n`, `e`만 공개하며 private 파라미터를 포함하지 않는다. Access Token에는 개인정보와 자격증명을 넣지 않고 signup 응답에는 토큰을 추가하지 않았다. 로그인, Refresh Token, 로그아웃, Resource Server 강제, Learning Core 및 Python AI 코드를 변경하지 않았고 Identity 밖의 도메인 코드를 추가하지 않았다.
- 결정사항: Access Token 기본 TTL은 `PT30M`, Header는 `RS256`·`JWT`·필수 `kid`, scope는 공백 구분 정렬 문자열로 확정했다. null 또는 빈 요청 scope에는 설정된 기본 scope를 사용한다. 현재는 단일 Active Key만 사용하며 JWKS 변환 경계에서 항상 `toPublicJWK()`를 호출한다. 테스트는 실제 파일 대신 Java `KeyPairGenerator`의 메모리 RSA 2048비트 키와 고정 Clock을 사용한다. Key Material과 발급 결과 모델의 문자열 표현은 민감값을 숨긴다.
- 위험 요소: Bootstrap의 `anyRequest().permitAll()` 정책은 아직 유지된다. 운영 RSA Key의 생성·주입·권한·백업·교체는 저장소 밖 배포 절차가 필요하며 파일 누락·형식 오류·키 쌍 불일치 시 시작이 실패한다. 단일 Active Key 상태에서는 즉시 교체 시 기존 토큰 검증이 깨질 수 있으므로 다중 Public Key와 캐시 기간을 고려한 Rotation 구현이 필요하다. 배포 issuer와 Learning Core 검증 설정의 정확한 일치, HTTPS URL, 서버 간 Clock 오차 정책도 확정해야 한다. 내부 발급기는 아직 로그인 흐름에 연결되지 않았다.
- 다음 작업: 이메일 로그인에서 저장된 BCrypt 해시와 계정 상태를 검증한 뒤 성공한 User의 UUID만 기존 `AccessTokenIssuer`에 전달하고 로그인 응답에 발급 결과를 연결한다. 이후 Refresh Token 원문을 저장하지 않는 세션, 회전·재사용 탐지·폐기 정책과 로그아웃을 별도 단계로 구현하고, Key Rotation과 보호 API 정책을 확정한다.

## 2026-07-24 — 일반 이메일 로그인 및 Opaque RefreshSession 발급 구현

<!-- codex-turn:019f9286-aeb5-7122-afa6-55b6b5cf2267 -->

- 날짜: 2026-07-24
- 브랜치: `feat/local-login-session`
- 작업 목표: 일반 이메일 로그인에서 정규화 이메일과 BCrypt 해시로 자격증명을 검증하고 계정 상태를 확인한 뒤, 기존 RS256 `AccessTokenIssuer`를 연결하고 원문을 저장하지 않는 Opaque RefreshSession을 생성한다. 재발급, Rotation, 재사용 탐지, 로그아웃 및 Resource Server 강제는 이번 범위에서 제외한다.
- 변경 파일: `.env.example`, `src/main/resources/application.yml`, `src/test/resources/application-test.yml`, `src/main/java/web/tosunsaeng/identity/auth/` 아래 기존 Controller·Service·오류 enum과 새 로그인 Request/Response DTO, 새 `src/main/java/web/tosunsaeng/identity/security/refresh/` 패키지의 설정·생성기·해시기·Document·Repository·발급 결과·발급 Service 8개 파일, `src/test/java/web/tosunsaeng/identity/IdentityApplicationTests.java`, 인증 Service·Controller 테스트, 새 RefreshSession 관련 테스트 5개, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`
- 구현 내용: `POST /api/v1/auth/login`은 이메일 앞뒤 공백 제거와 기존 정규화, 정규화 이메일 단건 조회, BCrypt 일치 확인, `ACTIVE` 상태 확인을 순서대로 수행한다. 성공 시 실제 User UUID와 빈 요청 scope를 기존 `AccessTokenIssuer`에 전달해 설정된 기본 scope의 RS256 Access Token을 발급하고, `SecureRandom` 기반 32바이트 이상의 값을 Base64 URL-safe without padding으로 인코딩한 Opaque Refresh Token을 생성한다. 원문을 UTF-8 SHA-256 후 같은 Base64 형식으로 해시해 `refresh_sessions` 문서에 저장하며, UUID 세션 ID, 실제 사용자 ID, 생성·만료·최초 사용 시각과 null 폐기·회전 원본 필드를 기록한다. `tokenHash`에는 unique index, `expiresAt`에는 `expireAfter = "0s"` TTL index를 적용했다. 로그인 응답은 두 토큰, `Bearer` grant type, 기존 Access Token 발급·만료 시각 차이에서 계산한 milliseconds 만료값만 반환하며 문자열 표현에서는 토큰을 숨긴다.
- 실행한 테스트와 결과: 기본 샌드박스의 첫 Gradle 컴파일은 사용자 캐시 lock 접근 제한으로 실행 전에 차단되었고 승인된 캐시 접근으로 `./gradlew compileJava compileTestJava`를 재실행해 성공했다. 최종 로그인·RefreshSession 관련 테스트는 41개, 실패·오류·건너뜀 0개로 `BUILD SUCCESSFUL`이었다. `./gradlew clean test`는 기존 회원가입·JWT·JWKS를 포함한 전체 75개 테스트에서 실패·오류·건너뜀 0개로 `BUILD SUCCESSFUL`이었다. `git diff --check`가 통과했고 운영 소스·설정의 민감 로그, 자격증명 포함 MongoDB URI, PEM 본문, 서명 토큰 형태, `java.util.Random`, RefreshSession 원문 필드 및 Access Token 영속화 검색에서 새 위반이 없었다. PEM Header 문자열은 기존 RSA 형식 로더와 대응 테스트 상수에만 존재한다.
- 유지한 계약: JWT는 기존 RSA Key Loader·Encoder·JWKS endpoint·Header·Claim·TTL 설정을 변경하지 않고 검증된 실제 UUID만 `sub`로 전달한다. Learning Core audience와 Python AI `examId` 의미를 유지했고 실제 사용자 ID를 Python AI로 보내는 코드를 추가하지 않았다. Controller는 Repository·PasswordEncoder·토큰 발급기를 직접 호출하지 않으며 Bootstrap의 STATELESS·전체 임시 permit-all, 폼 로그인·Basic 인증 비활성화 정책도 변경하지 않았다. RefreshSession에는 해시만 저장하고 원문, 이메일, 비밀번호 해시, Access Token을 저장하지 않으며 응답과 로그에 내부 Session 정보나 자격증명 상세를 노출하지 않는다.
- 결정사항: 존재하지 않는 이메일과 불일치 자격증명은 외부에 동일한 `INVALID_CREDENTIALS` 401 code·message를 반환하고, `SUSPENDED`와 `WITHDRAWN`은 상태를 구분하지 않는 `ACCOUNT_NOT_ACTIVE` 403으로 통일한다. 로그인 입력은 `NotBlank`와 최대 64자만 적용해 회원가입 최소 길이·복잡도 규칙을 다시 검사하지 않는다. Refresh Token 기본 TTL은 14일, 난수 길이는 최소 32바이트이며 32 미만 설정은 명확한 설정 오류로 거절한다. RefreshSession 시각은 Access Token과 같은 주입 `Clock`을 사용한다. MongoDB TTL 삭제는 정리 용도이고 향후 재발급 판단 근거로 단독 사용하지 않는다.
- 위험 요소: Bootstrap의 전체 허용 정책이 남아 있고 공개 로그인 API의 rate limit·credential stuffing 방어·abuse 관측이 필요하다. 동일한 외부 자격증명 오류에도 사용자 부재와 BCrypt 검증 경로의 시간 차이는 별도 완화 검토가 필요하다. MongoDB TTL 삭제는 비동기이므로 만료 문서가 잠시 남을 수 있다. 재발급·Rotation·재사용 탐지·로그아웃이 아직 없어 발급 세션의 만료 전 서버 측 사용·폐기 흐름이 없으며, 운영 index 생성과 배포 정책도 별도로 확정해야 한다.
- 다음 작업: 재발급 API에서 원문 해시 조회 후 문서 존재뿐 아니라 `expiresAt`과 `revokedAt`을 직접 검사하고, 기존 세션 폐기와 후속 세션 연결을 원자적으로 처리하는 Rotation 및 재사용 탐지·세션 패밀리 폐기를 구현한다. 이어서 단일·전체 로그아웃의 안전한 폐기, 공개 endpoint 외 Resource Server 보호 정책, 다중 RSA Key Rotation을 순차적으로 구현한다.

## 2026-07-24 — Atlassian MCP 등록

<!-- codex-turn:019f9324-b657-7092-8b5a-31e12a39955d -->

- 날짜: 2026-07-24
- 브랜치: `feat/local-login-session`
- 작업 목표: Codex 사용자 전역 설정에 Atlassian Remote MCP를 등록하고 OAuth 연결 및 활성화 상태를 확인한다.
- 변경 파일: Codex 사용자 전역 MCP 설정, `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션 코드와 저장소 실행 설정은 변경하지 않았다.
- 구현 내용: 명령 끝의 불필요한 `]`를 제외하고 `atlassian` 이름으로 Remote MCP URL을 등록했으며, 제공된 OAuth 흐름을 완료한 뒤 설정 조회를 통해 `enabled: true`, `streamable_http` 전송 방식 및 등록 URL을 확인했다.
- 실행한 테스트와 결과: `codex mcp get atlassian`의 최초 조회에서 미등록 상태를 확인했다. 등록 및 OAuth 연결 명령은 성공했고, 후속 조회에서도 활성화된 설정이 확인됐다. 애플리케이션 코드나 비즈니스 로직 변경이 없어 `./gradlew clean test`는 실행하지 않았다.
- 유지한 계약: Identity 도메인, UUID `userId`, JWT RS256·`kid`·JWKS·issuer·Learning Core audience, Python AI `examId`, Refresh Token 보안 계약을 변경하지 않았다. 인증 URL, 자격증명, Token 또는 Secret을 문서에 기록하지 않았다.
- 결정사항: Atlassian 연결은 저장소 파일이 아니라 Codex 사용자 전역 MCP 설정으로 관리하고, 서버 이름은 `atlassian`, Remote MCP URL은 `https://mcp.atlassian.com/v1/mcp/authv2`로 유지한다.
- 위험 요소: MCP는 연결된 Atlassian 사용자 계정의 허용 범위 내에서 외부 데이터에 접근할 수 있으므로 권한 범위와 연결 유지 여부를 사용자 설정에서 관리해야 한다.
- 다음 작업: 필요 시 새 Codex 세션에서 Atlassian MCP 도구 노출 여부를 확인하고, 더 이상 사용하지 않을 때 `codex mcp remove atlassian`로 등록을 제거하거나 Atlassian 측 OAuth 연결을 해제한다.

## 2026-07-27 — 접근 가능한 Jira 프로젝트 읽기 전용 조회

<!-- codex-turn:019fa110-9e0a-7123-88ac-9a3c930823d4 -->

- 날짜: 2026-07-27
- 브랜치: `feat/refresh-token-lifecycle`
- 작업 목표: Atlassian MCP를 사용해 현재 연결 계정이 접근할 수 있는 Jira 프로젝트 목록을 조회하되 Jira 데이터는 생성하거나 수정하지 않는다.
- 변경 파일: 필수 작업 기록을 위한 `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 변경했다. 애플리케이션 코드와 Jira 데이터는 변경하지 않았다.
- 구현 내용: 접근 가능한 Atlassian 리소스를 읽기 전용으로 확인한 뒤 `to-teacher` 사이트의 Jira 프로젝트를 `view` 권한 기준으로 조회했다. 전체 페이지가 끝났음을 확인했으며, 열람 가능한 프로젝트는 software 유형의 `TMI` 1개이고 프로젝트 key도 `TMI`다. 생성·수정·댓글·전환 등 쓰기 MCP 도구는 호출하지 않았다.
- 실행한 테스트와 결과: Atlassian MCP의 접근 가능 리소스 조회와 Jira visible projects 조회가 성공했고, 프로젝트 응답은 `total=1`, `isLast=true`였다. 애플리케이션 코드나 비즈니스 로직 변경이 없어 `./gradlew clean test`는 실행하지 않았다. 문서 변경 후 `git diff --check`와 turn marker 검사를 실행했으며 모두 통과했다.
- 유지한 계약: Identity 도메인 경계, UUID `userId`, JWT RS256·`kid`·JWKS·issuer·Learning Core audience, Python AI `examId`, Refresh Token 원문 비저장 계약을 변경하지 않았다. Secret, Token, Password, 실제 Key 및 전체 MongoDB URI를 조회 결과나 문서에 기록하지 않았다.
- 결정사항: 사용자 요청에 따라 이번 Atlassian 작업은 `view` 권한 기반 목록 조회에만 한정했고 Jira 데이터의 생성·수정은 수행하지 않았다.
- 위험 요소: 조회 결과는 현재 OAuth 연결 계정의 권한과 Jira 프로젝트 구성에 따라 달라질 수 있으며, 향후 권한 또는 프로젝트 설정 변경 시 목록이 달라질 수 있다.
- 다음 작업: Jira 관련 후속 생성·수정은 사용자가 별도로 요청하기 전까지 수행하지 않으며, Identity Service 개발의 다음 단계는 Refresh Token 재발급·Rotation·재사용 탐지 구현이다.

## 2026-07-27 — Jira 프로젝트 생성 권한 및 이슈 유형 읽기 전용 조회

<!-- codex-turn:019fa128-81f4-7fc2-8b47-028225a8b2a6 -->

- 날짜: 2026-07-27
- 브랜치: `feat/refresh-token-lifecycle`
- 작업 목표: Atlassian MCP로 접근 가능한 Jira 프로젝트마다 프로젝트 key·이름, 현재 계정의 이슈 생성 가능 여부와 사용 가능한 이슈 유형을 읽기 전용으로 확인한다.
- 변경 파일: 이번 작업으로는 필수 기록 문서인 `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 변경했다. 기존 작업 트리의 `AGENTS.md` 변경은 건드리지 않았고 애플리케이션 코드와 Jira 데이터도 변경하지 않았다.
- 구현 내용: `view` 권한 기준 프로젝트 목록과 `create` 권한 기준 프로젝트 목록을 별도로 조회해 key로 대조하고, 각 접근 가능 프로젝트의 이슈 유형 메타데이터를 조회했다. `TMI` 프로젝트는 이름과 key가 모두 `TMI`이며 이슈 생성이 가능하고, 사용 가능한 유형은 `에픽`, `하위 작업`, `작업`, `스토리`다. Jira 쓰기 도구는 호출하지 않았다.
- 실행한 테스트와 결과: `view`와 `create` 프로젝트 조회가 각각 `total=1`, `isLast=true`로 성공했고 두 결과에 모두 `TMI`가 포함됐다. `TMI` 이슈 유형 메타데이터 조회는 총 4개 유형을 반환했다. 애플리케이션 변경이 없어 `./gradlew clean test`는 실행하지 않았으며, 문서 변경 후 `git diff --check`와 turn marker 검사를 실행해 모두 통과했다.
- 유지한 계약: Identity 도메인 경계, UUID `userId`, JWT RS256·`kid`·JWKS·issuer·Learning Core audience, Python AI `examId`, Refresh Token 원문 비저장 계약을 변경하지 않았다. Secret, Token, Password, 실제 Key 및 전체 MongoDB URI를 문서에 기록하지 않았다.
- 결정사항: 이슈 생성 가능 여부는 프로젝트 검색의 `action=create` 결과 포함 여부로 판단하고, 사용 가능한 유형은 프로젝트별 Jira 이슈 유형 메타데이터 응답을 기준으로 한다. 요청에 따라 모든 Atlassian 작업을 조회에 한정했다.
- 위험 요소: 결과는 현재 OAuth 연결 계정의 권한과 Jira 프로젝트 설정에 따라 달라질 수 있다. `하위 작업`은 사용 가능한 유형이지만 실제 생성 시 상위 이슈가 필요하다.
- 다음 작업: Jira 데이터 생성·수정은 사용자가 별도로 요청하기 전까지 수행하지 않으며, 권한 또는 프로젝트 설정이 바뀌면 같은 읽기 전용 조회로 결과를 다시 확인한다.

## 2026-07-27 — TMI Refresh Token 수명주기 구현 이슈 Payload 초안

<!-- codex-turn:019fa12d-2675-75f0-83c8-e3f0af60f800 -->

- 날짜: 2026-07-27
- 브랜치: `feat/refresh-token-lifecycle`
- 작업 목표: 저장소 규칙과 현재 상태를 우선 확인한 뒤, TMI 프로젝트에 생성할 Refresh Token 재발급·Rotation·재사용 탐지·멱등 로그아웃 구현 `작업` 이슈의 최종 Payload를 Jira 변경 없이 준비한다.
- 변경 파일: 이번 작업으로는 필수 기록 문서인 `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 변경했다. 기존 작업 트리의 `AGENTS.md` 변경은 읽기만 하고 건드리지 않았으며 애플리케이션 코드와 Jira 데이터도 변경하지 않았다.
- 구현 내용: `AGENTS.md`와 `CURRENT_STATE.md`를 끝까지 읽어 Identity 경계와 현재 RefreshSession 기반을 확인했다. Atlassian MCP로 TMI의 이슈 유형과 `작업` 생성 필드 19개를 조회했으며, 선택 가능한 `priority` 필드에 `High`가 포함됨을 확인했다. 요청된 배경, API, Rotation·동시성·재사용 탐지·로그아웃 규칙, 오류, 완료 조건, 테스트, 제외 범위와 보안 요구사항을 Markdown 설명으로 구조화하고 실제 전송 필드를 `projectKey`, `issueTypeName`, `summary`, `description`, `contentFormat`, `additional_fields.priority`로 한정한 초안을 작성했다. Jira 생성·수정 도구는 호출하지 않았다.
- 실행한 테스트와 결과: Atlassian 접근 리소스, TMI 이슈 유형, `작업` 생성 메타데이터 조회가 모두 성공했다. `priority`는 생성 필드로 제공되며 `High`가 허용값임을 확인했다. 코드 변경이 없어 `./gradlew clean test`는 실행하지 않았고, 문서 변경 후 `git diff --check`와 turn marker 검사를 실행해 모두 통과했다.
- 유지한 계약: 실제 사용자 ID의 UUID·JWT `sub` 사용, RS256·`kid`·issuer·`tosunsaeng-learning-core` audience·Public JWKS, Opaque Refresh Token과 원문 DB·로그 비저장, 실제 외부 인프라 미사용 계약을 유지했다. Jira 초안과 기록에 실제 자격증명, Token 값, MongoDB URI, RSA Private Key 또는 개인정보를 포함하지 않았다.
- 결정사항: TMI 생성 화면에서 `High`가 실제 선택 가능하므로 우선순위 필드를 생략하지 않고 `additional_fields.priority.name=High`로 제안한다. 설명은 Markdown으로 전송하고 기본값이 있는 보고자 및 요청하지 않은 선택 필드는 명시적으로 보내지 않는다. 사용자 승인 전에는 이슈를 생성하지 않는다.
- 위험 요소: 이 초안에는 아직 Jira 이슈 key가 없고 Jira 구성이나 권한이 변경되면 생성 직전에 메타데이터를 다시 확인해야 한다. 재사용 탐지 시 활성 세션 폐기 범위와 Optimistic Lock 이후 새 세션 생성 실패의 복구·일관성 전략은 구현 과정에서 테스트와 함께 구체화해야 한다.
- 다음 작업: 사용자에게 최종 Payload 초안을 제시하고 명시적인 생성 승인을 기다린다. 승인받더라도 생성 직전에 TMI `작업` 메타데이터와 `High` 허용 여부를 재확인한 뒤 제시한 필드만 전송한다.

## 2026-07-27 — 승인된 Refresh Token 수명주기 Jira 이슈 생성

- 날짜: 2026-07-27
- 브랜치: `feat/refresh-token-lifecycle`
- Jira: `TMI-6`
- 작업 목표: 사용자가 승인한 최종 Payload 그대로 TMI 프로젝트에 Refresh Token 재발급·Rotation·재사용 탐지·멱등 로그아웃 구현 `작업` 이슈를 하나 생성하고 기본 상태를 유지한다.
- 변경 파일: 저장소에서는 필수 기록 문서인 `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 변경했다. 기존 작업 트리의 `AGENTS.md` 변경은 건드리지 않았고 애플리케이션 코드도 변경하지 않았다. 외부 Jira에는 새 이슈 `TMI-6` 하나만 생성했다.
- 구현 내용: 생성 직전 TMI `작업` 메타데이터를 다시 조회해 `priority` 필드와 `High` 허용값을 확인했다. 승인된 제목과 Markdown 설명, `High` 우선순위만 전송해 `TMI-6`을 생성했으며 담당자, 라벨, 스프린트, 에픽 또는 상태 전환을 설정하지 않았다. 다른 Jira 이슈는 수정하지 않았고 댓글도 추가하지 않았다.
- 실행한 테스트와 결과: Jira 생성 응답과 후속 읽기 전용 조회에서 key `TMI-6`, 승인된 제목, 우선순위 `High`, 기본 상태 `해야 할 일`, 담당자 없음과 빈 라벨을 확인했다. 애플리케이션 코드 변경이 없어 `./gradlew clean test`는 실행하지 않았고, 문서 변경 후 `git diff --check`를 실행해 통과했다.
- 유지한 계약: 이슈 설명에는 API 계약과 보안 요구사항만 기록하고 실제 Refresh Token, Access Token, 비밀번호, MongoDB URI, RSA Private Key 또는 개인정보를 포함하지 않았다. UUID `userId`, RS256·`kid`·issuer·audience·JWKS 및 Refresh Token 원문 비저장 계약을 변경하지 않았다.
- 결정사항: 사용자 명시적 승인을 근거로 Jira 생성 작업만 수행했다. Jira 작업은 `TMI-6` 신규 생성, 댓글은 없음, 상태 변경은 없음이며 생성 기본 상태 `해야 할 일`을 유지했다. 우선순위는 지원 여부를 재확인한 뒤 `High`로 설정했다.
- 위험 요소: `TMI-6`은 아직 구현되지 않았고 상태도 `해야 할 일`이다. 구현 과정에서는 동시 Rotation의 저장 일관성, 재사용 탐지의 활성 Session 폐기 범위 및 민감값 비노출을 테스트로 구체화해야 한다.
- 다음 작업: `TMI-6` 설명과 완료 조건을 기준으로 구현·테스트를 진행하되 Jira 댓글이나 상태 변경은 별도 사용자 승인 전까지 수행하지 않는다.

## 2026-07-27 — TMI-6 생성 작업의 현재 turn 기록 보완

<!-- codex-turn:019fa132-27fb-7a43-90ef-245e65651724 -->

- 날짜: 2026-07-27
- 브랜치: `feat/refresh-token-lifecycle`
- Jira: `TMI-6`
- 작업 목표: Stop Hook 요구에 따라 승인된 Jira 이슈 생성 작업의 현재 turn marker를 WORKLOG 끝에 append하고 CURRENT_STATE를 생성 완료·구현 미착수 상태로 명확히 갱신한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 변경했다. 기존 WORKLOG 항목과 작업 트리의 `AGENTS.md` 변경은 건드리지 않았고 애플리케이션 코드도 변경하지 않았다.
- 구현 내용: Jira `TMI-6` 생성 결과를 참조하는 현재 turn 기록을 marker와 함께 파일 끝에 추가하고, CURRENT_STATE의 진행 중 항목을 기본 상태 `해야 할 일`·구현 미착수로 갱신했다. 이 보완 과정에서 Jira 생성·수정·댓글·상태 전환은 수행하지 않았다.
- 실행한 테스트와 결과: 애플리케이션 변경이 없어 `./gradlew clean test`는 실행하지 않았다. `git diff --check`와 현재 turn marker 단일 존재 검사를 실행해 모두 통과했다.
- 유지한 계약: Identity 도메인 경계, UUID `userId`, RS256·`kid`·issuer·audience·JWKS, Refresh Token 원문 DB·로그 비저장 계약을 변경하지 않았다. Secret, 실제 Token 값, Password, MongoDB URI, RSA Private Key 또는 개인정보를 기록하지 않았다.
- 결정사항: 과거 WORKLOG 기록은 수정하지 않고 Hook이 요구한 marker를 포함하는 새 항목을 append했다. 이번 보완의 Jira 작업은 없음, 댓글 없음, 상태 변경 없음이며 기존 생성 승인의 범위를 확장하지 않았다.
- 위험 요소: `TMI-6`은 생성됐지만 구현은 시작되지 않았고 현재 브랜치 이름에는 Jira key가 포함되어 있지 않다. 구현 착수 전에 저장소 Jira 규칙에 맞는 브랜치 운영 방식을 확인해야 한다.
- 다음 작업: `TMI-6`의 승인된 설명과 완료 조건을 기준으로 구현하며 Jira 댓글이나 상태 변경은 별도 사용자 승인 전까지 수행하지 않는다.

## 2026-07-27 — TMI-6 현재 상태 및 가능한 전환 읽기 전용 조회

<!-- codex-turn:019fa13a-e839-7ba0-bea8-617d4380a42d -->

- 날짜: 2026-07-27
- 브랜치: `feat/refresh-token-lifecycle`
- Jira: `TMI-6`
- 작업 목표: Atlassian MCP로 Jira `TMI-6`의 제목, 현재 상태와 현재 계정에서 실행 가능한 상태 전환 목록을 읽기 전용으로 확인한다.
- 변경 파일: 이번 작업으로는 `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 변경했다. 기존 WORKLOG 항목과 작업 트리의 `AGENTS.md` 변경은 건드리지 않았고 애플리케이션 코드와 Jira 데이터도 변경하지 않았다.
- 구현 내용: 이슈 조회에서 승인된 제목과 현재 상태 `해야 할 일`을 확인하고, 사용 가능한 전환만 조회해 `해야 할 일`, `검토 중`, `진행 중`, `완료` 네 항목을 확인했다. Jira 상태 전환·수정·댓글 도구는 호출하지 않았다.
- 실행한 테스트와 결과: Atlassian MCP 이슈 조회와 전환 목록 조회가 모두 성공했으며 네 전환 모두 `isAvailable=true`로 반환됐다. 코드 변경이 없어 `./gradlew clean test`는 실행하지 않았고, 문서 변경 후 `git diff --check`와 현재 turn marker 단일 존재 검사를 실행해 모두 통과했다.
- 유지한 계약: Identity 도메인 경계, UUID `userId`, RS256·`kid`·issuer·audience·JWKS, Refresh Token 원문 DB·로그 비저장 계약을 변경하지 않았다. Secret, 실제 Token 값, Password, MongoDB URI, RSA Private Key 또는 개인정보를 기록하지 않았다.
- 결정사항: 가능한 상태 전환은 Jira가 현재 반환한 사용 가능 전환을 그대로 제시하고, 현재 상태와 동일한 목적지인 `해야 할 일`도 응답에 포함됐으므로 목록에서 제외하지 않는다. 사용자 요청에 따라 조회만 수행했다.
- 위험 요소: 가능한 전환은 Jira 워크플로 설정과 현재 계정 권한에 따라 달라질 수 있으므로 실제 전환 직전에 다시 조회해야 한다.
- 다음 작업: 사용자가 특정 전환을 명시적으로 승인하기 전까지 `TMI-6`의 상태와 다른 Jira 데이터는 변경하지 않는다.

## 2026-07-27 — TMI-6을 진행 중으로 전환

- 날짜: 2026-07-27
- 브랜치: `feat/refresh-token-lifecycle`
- Jira: `TMI-6`
- 작업 목표: 사용자의 명시적 요청에 따라 Jira `TMI-6`을 방금 확인한 `진행 중` 상태로 전환하고 다른 필드와 다른 이슈는 변경하지 않는다.
- 변경 파일: 저장소에서는 `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 변경했다. 기존 WORKLOG 항목과 작업 트리의 `AGENTS.md` 변경은 건드리지 않았고 애플리케이션 코드도 변경하지 않았다. 외부 Jira에서는 `TMI-6`의 상태만 변경했다.
- 구현 내용: 전환 직전 사용 가능한 전환을 다시 조회해 `진행 중` transition ID `21`이 사용 가능함을 확인하고, 필드·update·comment 없이 transition ID만 전송했다. 응답에서 `TMI-6`의 상태가 `진행 중`으로 변경됐음을 확인했으며 다른 Jira 이슈는 수정하지 않았다.
- 실행한 테스트와 결과: Atlassian MCP 전환 목록 재조회와 상태 전환이 성공했고 전환 응답의 status가 `진행 중`이었다. 애플리케이션 코드 변경이 없어 `./gradlew clean test`는 실행하지 않았으며 문서 변경 후 `git diff --check`를 실행해 통과했다.
- 유지한 계약: Jira 설명, Identity 코드와 UUID `userId`, RS256·`kid`·issuer·audience·JWKS, Refresh Token 원문 DB·로그 비저장 계약을 변경하지 않았다. Secret, 실제 Token 값, Password, MongoDB URI, RSA Private Key 또는 개인정보를 기록하지 않았다.
- 결정사항: Jira 작업은 사용자 명시적 승인에 따른 `TMI-6`의 `해야 할 일` → `진행 중` 상태 전환 하나뿐이다. 댓글은 추가하지 않았고 다른 필드는 변경하지 않았다.
- 위험 요소: Jira 상태는 `진행 중`이지만 애플리케이션 구현과 테스트는 아직 시작하지 않았다. 현재 브랜치 이름에는 Jira key가 포함되어 있지 않아 구현 착수 전 저장소 Jira 규칙에 맞는 브랜치 운영 방식을 확인해야 한다.
- 다음 작업: `TMI-6` 설명과 완료 조건을 기준으로 구현·테스트를 시작하고, 추가 Jira 댓글이나 상태 변경은 별도 사용자 승인 전까지 수행하지 않는다.

## 2026-07-27 — TMI-6 진행 중 전환의 현재 turn 기록 보완

<!-- codex-turn:019fa13d-1b50-7bf1-a714-1656df909937 -->

- 날짜: 2026-07-27
- 브랜치: `feat/refresh-token-lifecycle`
- Jira: `TMI-6`
- 작업 목표: Stop Hook 요구에 따라 Jira `TMI-6`의 `진행 중` 상태 전환 작업에 현재 turn marker를 기록하고 후속 Jira 변경 승인 원칙을 CURRENT_STATE에 반영한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 변경했다. 기존 WORKLOG 항목과 작업 트리의 `AGENTS.md` 변경은 건드리지 않았고 애플리케이션 코드도 변경하지 않았다.
- 구현 내용: `TMI-6`의 사용자 승인 기반 `진행 중` 전환 결과를 참조하는 marker 항목을 WORKLOG 끝에 append하고, 추가 댓글이나 상태 변경은 별도 명시적 승인 후 수행한다는 결정을 CURRENT_STATE에 추가했다. 이 보완 과정에서는 Jira를 추가로 조회하거나 변경하지 않았다.
- 실행한 테스트와 결과: 애플리케이션 변경이 없어 `./gradlew clean test`는 실행하지 않았다. `git diff --check`와 현재 turn marker 단일 존재 검사를 실행한다.
- 유지한 계약: Identity 도메인 경계, UUID `userId`, RS256·`kid`·issuer·audience·JWKS, Refresh Token 원문 DB·로그 비저장 계약을 변경하지 않았다. Secret, 실제 Token 값, Password, MongoDB URI, RSA Private Key 또는 개인정보를 기록하지 않았다.
- 결정사항: 과거 WORKLOG는 수정하지 않고 새 marker 항목만 append했다. 이번 보완의 Jira 작업은 없음, 댓글 없음, 추가 상태 변경 없음이며 기존 사용자 승인의 범위를 확장하지 않았다.
- 위험 요소: Jira는 `진행 중`이지만 애플리케이션 구현은 아직 시작하지 않았고 현재 브랜치 이름에도 Jira key가 포함되지 않았다.
- 다음 작업: 저장소 Jira 규칙에 맞는 브랜치 운영 방식을 확인한 뒤 `TMI-6` 구현과 테스트를 진행하고, 후속 Jira 변경은 별도 승인을 받는다.

## 2026-07-27 — TMI-6 현재 상태 읽기 전용 재조회

- 날짜: 2026-07-27
- 브랜치: `feat/refresh-token-lifecycle`
- Jira: `TMI-6`
- 작업 목표: Atlassian MCP로 Jira `TMI-6`의 현재 상태만 다시 조회하고 Jira 데이터는 변경하지 않는다.
- 변경 파일: 이번 작업으로는 `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 변경했다. 기존 WORKLOG 항목과 작업 트리의 `AGENTS.md` 변경은 건드리지 않았고 애플리케이션 코드와 Jira 데이터도 변경하지 않았다.
- 구현 내용: 이슈 조회에 `status` 필드만 요청하고 최근 조회 기록을 갱신하지 않도록 설정해 `TMI-6`의 현재 상태가 `진행 중`임을 확인했다. 상태 전환, 편집, 댓글 또는 다른 이슈 조회는 수행하지 않았다.
- 실행한 테스트와 결과: Atlassian MCP 읽기 전용 조회가 성공해 `TMI-6`과 상태 `진행 중`을 반환했다. 코드 변경이 없어 `./gradlew clean test`는 실행하지 않았고, 앞선 Hook marker 단일 존재 검사와 문서 변경 후 `git diff --check`를 실행해 모두 통과했다.
- 유지한 계약: Identity 도메인 경계, UUID `userId`, RS256·`kid`·issuer·audience·JWKS, Refresh Token 원문 DB·로그 비저장 계약을 변경하지 않았다. Secret, 실제 Token 값, Password, MongoDB URI, RSA Private Key 또는 개인정보를 기록하지 않았다.
- 결정사항: 사용자 요청에 따라 현재 상태만 결과로 사용하고 Jira 변경 작업은 수행하지 않았다.
- 위험 요소: 조회 결과는 현재 시점의 Jira 상태이며 이후 다른 사용자나 자동화에 의해 달라질 수 있다.
- 다음 작업: 추가 Jira 조회·댓글·상태 변경은 사용자 요청과 승인 범위에 따라 수행한다.

## 2026-07-27 — TMI-6 Refresh Token 수명주기 구현

<!-- codex-turn:019fa141-4592-70b3-b251-5f2881aa8113 -->

- 날짜: 2026-07-27
- 브랜치: `feat/TMI-6-refresh-token-lifecycle`
- Jira: `TMI-6`
- 작업 목표: Jira `TMI-6`의 설명과 완료 조건을 기준으로 Refresh Token 재발급·Rotation·재사용 탐지와 멱등 로그아웃을 구현하고, 기존 JWT 및 Identity 보안 계약을 유지한 채 전체 회귀를 검증한다.
- 변경 파일: `README.md`, `src/main/java/web/tosunsaeng/identity/auth/controller/AuthController.java`, 신규 Request·Response DTO 3개, `AuthErrorStatus.java`, `AuthService.java`, `RefreshSession.java`, `RefreshSessionIssuer.java`, `RefreshSessionRepository.java`, 신규 `RevocationReason.java`, 관련 Service·Controller·RefreshSession 테스트 6개, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 변경했다. 작업 시작 전부터 수정돼 있던 `AGENTS.md`와 과거 WORKLOG 항목은 건드리지 않았다.
- 구현 내용: Atlassian 공식 MCP로 `TMI-6`을 읽기 전용 조회해 저장소 규칙 및 JWT 계약과 충돌이 없음을 확인했다. `/api/v1/auth/reissue`에 해시 조회, 폐기 사유·만료 경계·사용자 상태 검사, 기존 Session의 Optimistic Lock 폐기, 후속 Access Token과 Opaque Refresh Token 발급 및 Rotation 관계 저장을 구현했다. 이미 Rotation된 Token 재사용 시 사용자별 미폐기 Session 전체를 `REUSE_DETECTED`로 폐기하고, `/api/v1/auth/logout`은 활성·미만료 Session만 `LOGOUT`으로 폐기하되 없는·폐기된·만료된 Token에는 성공하는 흐름으로 구현했다. Request 길이 제한과 validation 값 마스킹, DTO 문자열 redaction, milliseconds 만료 응답 및 README의 클라이언트 로그아웃 지침도 반영했다.
- 실행한 테스트와 결과: 프로덕션 및 테스트 컴파일이 성공했고 관련 Service·Controller·도메인 테스트 57개가 통과했다. 최종 `./gradlew clean test`는 전체 97개 테스트, 실패 0개, 오류 0개, 건너뜀 0개로 성공했다. `git diff --check`, 직접 시각 호출·금지 범위·민감 로그·원문 자격증명 필드·실제 Secret 패턴 정적 검색과 현재 turn marker 단일 존재 검사를 모두 통과했다. 테스트는 실제 Atlas와 운영 키를 호출하지 않았다.
- 유지한 계약: 실제 사용자 식별자는 검증된 UUID이고 JWT `sub`에만 사용하며 외부 Request에 사용자 식별자를 추가하지 않았다. 기존 RS256, 필수 `kid`, issuer, `tosunsaeng-learning-core` audience, 최소 Claim과 Public JWKS 계약을 변경하지 않았다. Refresh Token 원문은 DB·로그·예외에 저장하지 않고, 비밀번호 관련 정보 및 내부 세션 관계도 외부 응답에 노출하지 않았다. SecurityConfig의 임시 permit-all을 유지했고 Learning Core·Python AI 경계와 Identity 비소유 도메인을 변경하지 않았다.
- 결정사항: `expiresAt <= Clock`을 만료로 처리하고, ROTATED 재사용은 만료 검사보다 먼저 구분해 활성 Session 폐기를 수행한다. 기존 Session의 Optimistic Lock 저장이 성공한 요청만 후속 토큰을 발급하며 충돌 요청은 일반 `INVALID_REFRESH_TOKEN`으로 실패한다. 로그아웃은 Access Token 블랙리스트 없이 RefreshSession만 폐기하고 DB 장애는 성공으로 숨기지 않는다. 이번 Jira 작업은 조회만 수행했으며 댓글·상태·필드 변경은 없고 승인 대상 쓰기 작업도 수행하지 않았다.
- 위험 요소: MongoDB Transaction을 도입하지 않았으므로 기존 Session 폐기 저장 후 Access Token 발급 또는 후속 RefreshSession 저장이 실패하면 현재 Session을 잃고 재로그인이 필요할 수 있다. 여러 활성 Session의 재사용 탐지 폐기도 중간 저장 실패 시 일부만 반영될 수 있다. 기존 RefreshSession 문서에는 Optimistic Lock 버전 및 회전 패밀리 데이터 이행 정책이 필요할 수 있다. 로그아웃 전 Access Token은 만료 시각까지 유효할 수 있으므로 클라이언트의 즉시 로컬 토큰 삭제가 필요하다.
- 다음 작업: 사용자가 변경을 검토한 뒤 직접 커밋·push하고 PR 병합을 확인한다. 이후 별도 승인에 따라 Jira 완료 댓글과 상태 전환을 수행하며, 후속 개발은 공개 API abuse 방어, JWT Resource Server 보호 정책 및 다중 키 Rotation을 다룬다.

## 2026-07-27 — 승인된 TMI-6 구현 완료 댓글 등록

<!-- codex-turn:019fa15b-def2-7e70-9d87-7b0b39dec722 -->

- 날짜: 2026-07-27
- 브랜치: `feat/TMI-6-refresh-token-lifecycle`
- Jira: `TMI-6`
- 작업 목표: 사용자가 승인한 구현 완료 댓글 초안을 Atlassian 공식 MCP로 Jira `TMI-6`에 등록하되 이슈 상태, 다른 필드와 다른 이슈는 변경하지 않는다.
- 변경 파일: 저장소에서는 `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 변경했다. 애플리케이션 코드와 기존 WORKLOG 항목은 건드리지 않았다. 외부 Jira에서는 `TMI-6`에 새 댓글 하나만 추가했다.
- 구현 내용: 직전 Codex 세션 기록에서 승인 대상 Markdown 초안을 확인하고, 쓰기 전 `TMI-6`의 제목·상태·댓글을 읽기 전용으로 조회해 상태 `진행 중`과 기존 댓글 없음을 확인했다. 승인된 작업 요약, 변경 파일, 테스트 결과와 남은 위험 요소를 댓글 ID `10000`으로 등록했다. 댓글의 목적은 Refresh Token 재발급·Rotation·재사용 탐지·멱등 로그아웃 구현 결과와 검증 결과 및 잔여 위험 공유다.
- 실행한 테스트와 결과: Atlassian MCP 댓글 생성 응답이 성공했고 후속 읽기 전용 재조회에서 댓글 수 1개, 최신 댓글 ID `10000`, 승인된 본문과 상태 `진행 중`을 확인했다. 애플리케이션 코드 변경이 없어 `./gradlew clean test`는 다시 실행하지 않았다. 문서 변경 후 `git diff --check`와 현재 turn marker 검사를 실행해 통과했다.
- 유지한 계약: Jira 댓글에는 작업 요약, 변경 파일, 테스트 결과와 남은 위험 요소만 기록했으며 Secret, 실제 Token 값, Password, 전체 MongoDB URI, RSA Private Key 또는 개인정보를 포함하지 않았다. UUID `userId`, RS256·`kid`·issuer·audience·JWKS와 Refresh Token 원문 비저장 계약 및 Identity 도메인 경계를 변경하지 않았다.
- 결정사항: 사용자 명시적 승인을 근거로 `TMI-6`에 새 댓글 하나만 등록했다. 수행한 Jira 작업은 댓글 추가이며 상태는 `진행 중`으로 유지했고 상태 전환, 이슈 필드 편집, 다른 이슈 변경은 수행하지 않았다. 댓글 등록 승인 여부는 승인됨이며, 추가 댓글이나 상태 변경에는 별도 승인이 필요하다.
- 위험 요소: Jira 상태는 계속 `진행 중`이며 PR 병합 여부는 이번 작업에서 확인하지 않았다. 등록된 테스트 결과는 현재 저장소 구현 시점의 결과이므로 이후 코드 변경이 있으면 다시 검증해야 한다.
- 다음 작업: 사용자가 직접 변경을 검토해 커밋·push하고 PR 병합을 확인한다. 병합 후 Jira 상태 전환이 필요하면 전환 내용을 먼저 제시하고 별도 승인을 받은 뒤 수행한다.

## 2026-07-27 — TMI-6 현재 상태 및 가능한 전환 재조회

<!-- codex-turn:019fa160-19bf-7c23-bba3-56b0f536b998 -->

- 날짜: 2026-07-27
- 브랜치: `feat/TMI-6-refresh-token-lifecycle`
- Jira: `TMI-6`
- 작업 목표: Atlassian 공식 MCP로 Jira `TMI-6`의 현재 상태와 현재 계정에서 실행 가능한 상태 전환 목록을 읽기 전용으로 조회하고 Jira 데이터는 변경하지 않는다.
- 변경 파일: 이번 작업으로는 `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 변경했다. 애플리케이션 코드와 기존 WORKLOG 항목은 건드리지 않았고 Jira 데이터도 변경하지 않았다.
- 구현 내용: 이슈 조회에는 제목과 상태만 요청하고 최근 조회 기록을 갱신하지 않도록 설정했다. 별도의 가능한 전환 조회에서 사용할 수 없는 전환을 제외해 현재 상태 `진행 중`과 전환 `해야 할 일(11)`, `검토 중(31)`, `진행 중(21)`, `완료(41)`를 확인했다. 상태 전환, 댓글 추가, 이슈 편집 또는 다른 이슈 조회·변경은 수행하지 않았다.
- 실행한 테스트와 결과: Atlassian MCP의 이슈 및 전환 읽기 전용 조회가 모두 성공했고 네 전환은 모두 현재 사용 가능 상태로 반환됐다. 애플리케이션 코드 변경이 없어 `./gradlew clean test`는 실행하지 않았다. 문서 변경 후 `git diff --check`와 현재 turn marker 검사를 실행해 통과했다.
- 유지한 계약: Identity 도메인 경계, UUID `userId`, RS256·`kid`·issuer·audience·JWKS와 Refresh Token 원문 DB·로그 비저장 계약을 변경하지 않았다. Secret, 실제 Token 값, Password, 전체 MongoDB URI, RSA Private Key 또는 개인정보를 기록하지 않았다.
- 결정사항: 이번 Jira 작업은 `TMI-6`의 상태와 가능한 전환 조회 두 건뿐이며 댓글, 필드와 상태는 변경하지 않았다. 상태 변경은 없음이고 댓글 목적도 해당 없으며, 쓰기 작업이 아니므로 별도 변경 승인은 사용하지 않았다.
- 위험 요소: 가능한 전환 목록은 Jira 워크플로 설정, 현재 상태와 계정 권한에 따라 달라질 수 있으므로 실제 전환 직전에 다시 확인해야 한다.
- 다음 작업: 사용자가 특정 상태 전환을 명시적으로 요청하고 승인하기 전까지 `TMI-6`의 상태와 다른 Jira 데이터는 변경하지 않는다.

## 2026-07-27 — TMI-6 완료 전환

<!-- codex-turn:019fa162-679a-7530-b7e1-ef95319279e3 -->

- 날짜: 2026-07-27
- 브랜치: `feat/TMI-6-refresh-token-lifecycle`
- Jira: `TMI-6`
- 작업 목표: 사용자가 구현 PR의 main 병합과 전체 테스트 성공을 확인한 것을 근거로 Jira `TMI-6`을 방금 확인한 `완료` 상태로 전환하되 다른 필드, 댓글과 다른 이슈는 변경하지 않는다.
- 변경 파일: 저장소에서는 `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 변경했다. 애플리케이션 코드와 기존 WORKLOG 항목은 건드리지 않았다. 외부 Jira에서는 `TMI-6`의 상태만 변경했다.
- 구현 내용: 전환 직전 `TMI-6`의 현재 상태와 가능한 전환을 다시 읽어 상태 `진행 중` 및 `완료` transition ID `41`의 사용 가능 여부를 확인했다. 필드, update, 댓글 또는 이력 메타데이터 없이 transition ID `41`만 전송하고, 후속 읽기 전용 조회에서 상태 `완료`와 status ID `10003`을 확인했다. 다른 이슈는 조회하거나 변경하지 않았다.
- 실행한 테스트와 결과: 사용자가 main 병합 및 전체 테스트 성공을 확인했다고 명시했으며 이번 Jira 작업에서는 애플리케이션 테스트를 다시 실행하지 않았다. Atlassian MCP의 전환 전 조회, 상태 전환과 전환 후 조회가 모두 성공했고 `진행 중`에서 `완료`로 변경된 것을 확인했다. 문서 변경 후 `git diff --check`와 현재 turn marker 검사를 실행해 통과했다.
- 유지한 계약: Jira 상태 외에 설명, 우선순위, 담당자, 라벨, 댓글과 다른 필드를 변경하지 않았다. UUID `userId`, RS256·`kid`·issuer·audience·JWKS, Refresh Token 원문 DB·로그 비저장과 Identity 도메인 계약을 변경하지 않았으며 Secret, 실제 Token 값, Password, 전체 MongoDB URI, RSA Private Key 또는 개인정보를 기록하지 않았다.
- 결정사항: 사용자의 명시적 승인에 따라 `TMI-6`에 transition ID `41`만 적용했다. 수행한 Jira 작업은 상태 `진행 중` → `완료`, 댓글 목적은 해당 없음, 다른 필드 변경은 없음이며 승인 여부는 승인됨이다.
- 위험 요소: PR 병합과 전체 테스트 성공은 사용자 확인을 근거로 했으며 이번 작업에서 Git 원격 상태나 테스트를 독립적으로 재검증하지 않았다. Jira 상태는 이후 다른 사용자나 자동화에 의해 변경될 수 있다.
- 다음 작업: `TMI-6`에 추가 Jira 변경은 별도 요청과 승인 전까지 수행하지 않는다. 후속 개발은 기존 RefreshSession 데이터 이행, 실패 복구 정책, Resource Server 보호 정책과 다중 키 Rotation을 다룬다.

## 2026-07-27 — JWT 인증·내 프로필·전체 로그아웃 Jira Payload 초안

<!-- codex-turn:019fa19b-25bf-7d02-9ab5-38979dcbe1c6 -->

- 날짜: 2026-07-27
- 브랜치: `feat/TMI-6-refresh-token-lifecycle`
- Jira: `TMI-6`
- 작업 목표: 저장소 규칙과 현재 상태를 먼저 확인하고 TMI 프로젝트에 생성할 JWT Resource Server 인증·내 프로필 조회·전체 로그아웃 구현 `작업` 이슈의 최종 Payload를 Jira 변경 없이 준비한다.
- 변경 파일: 이번 작업으로는 필수 기록 문서인 `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 변경했다. 애플리케이션 코드와 기존 WORKLOG 항목은 건드리지 않았고 Jira 데이터도 변경하지 않았다.
- 구현 내용: `AGENTS.md`, `CURRENT_STATE.md`와 Identity–Learning Core JWT 계약을 끝까지 읽어 Identity 경계와 현재 구현 상태를 대조했다. Atlassian 공식 MCP로 TMI의 이슈 생성 권한, 네 이슈 유형, `작업` 유형 ID `10003`의 생성 필드 20개를 읽기 전용으로 조회했으며 `priority` 허용값에 `High`가 포함됨을 확인했다. 요청된 배경, 공개·보호 Endpoint, JWT 검증, 내 프로필, 전체 로그아웃, 완료 조건, 제외 범위와 보안 요구사항을 Markdown 설명으로 구조화하고 실제 전송 예정 필드를 최소화한 최종 초안을 작성했다.
- 실행한 테스트와 결과: Atlassian MCP의 생성 가능 프로젝트, 이슈 유형과 생성 필드 메타데이터 조회가 모두 성공했으며 TMI `작업` 및 `High` 우선순위 지원을 확인했다. 애플리케이션 코드 변경이 없어 `./gradlew clean test`는 실행하지 않았다. 문서 변경 후 `git diff --check`와 현재 turn marker 검사를 실행해 통과했다.
- 유지한 계약: 실제 사용자 ID는 UUID 문자열이고 검증된 JWT `sub`에서만 얻으며 Request Body, Path 또는 Query로 받지 않는 계약을 유지했다. RS256·필수 `kid`·issuer·`tosunsaeng-learning-core` audience·JWKS, Refresh Token 원문 비저장, Python AI의 `examId` 의미와 Identity 비소유 시험·스트릭·학습 경계를 초안에 반영했다. Secret, 실제 Token 값, Password, 전체 MongoDB URI, RSA Private Key 또는 개인정보를 기록하지 않았다.
- 결정사항: 초안은 프로젝트 `TMI`, 유형 `작업`, 지정 제목, Markdown 설명과 지원이 확인된 `High` 우선순위만 전송 대상으로 삼는다. 담당자, 라벨, 상위 항목, 상태 전환과 다른 선택 필드는 지정하지 않는다. 현재 브랜치의 `TMI-6`은 이미 완료된 기존 이슈이며 이번 신규 초안에는 아직 Jira key가 없다. 이번 Jira 작업은 조회만 수행했고 이슈 생성·수정·댓글·상태 변경은 없으며 생성 승인은 아직 받지 않았다.
- 위험 요소: Jira 프로젝트 설정이나 권한이 변경되면 생성 시점의 허용 필드와 우선순위가 달라질 수 있으므로 실제 생성 직전에 다시 확인해야 한다. 프로필의 `provider` 표현과 기존 LOCAL 사용자 데이터 호환 방식, 전체 로그아웃의 다중 Session 저장 실패 처리 및 JWT 검증 Clock 오차 정책은 구현 시 테스트와 함께 구체화해야 한다.
- 다음 작업: 사용자에게 최종 Payload 초안을 제시하고 명시적 생성 승인을 기다린다. 승인받으면 TMI `작업` 메타데이터와 `High` 지원을 다시 확인한 뒤 제시한 필드만 전송하며, 승인 전에는 Jira 이슈를 생성하지 않는다.

## 2026-07-27 — 승인된 JWT 인증·내 프로필·전체 로그아웃 Jira 이슈 생성

<!-- codex-turn:019fa1aa-9f56-7463-9ed6-2d4220e3533f -->

- 날짜: 2026-07-27
- 브랜치: `feat/TMI-6-refresh-token-lifecycle`
- Jira: `TMI-9`
- 작업 목표: 사용자가 승인한 최종 Payload 그대로 TMI 프로젝트에 JWT 인증 적용·내 프로필 조회·전체 로그아웃 구현 `작업` 이슈를 생성하고 기본 상태와 지정하지 않은 필드를 유지한다.
- 변경 파일: 저장소에서는 필수 기록 문서인 `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 변경했다. 애플리케이션 코드와 기존 WORKLOG 항목은 건드리지 않았다. 외부 Jira에는 새 이슈 `TMI-9` 하나만 생성했다.
- 구현 내용: 직전 세션 기록에서 승인된 제목과 Markdown 설명 원문을 복원하고, 생성 직전 Atlassian 공식 MCP로 TMI의 이슈 생성 권한, `작업` 유형 ID `10003`과 `High` 우선순위 지원을 재확인했다. `projectKey`, `issueTypeName`, `summary`, `description`, `contentFormat`, `additional_fields.priority`만 전송해 `TMI-9`를 생성했다. 담당자, 스프린트, 에픽, 라벨과 상태 전환은 전송하지 않았다.
- 실행한 테스트와 결과: Jira 생성 응답이 성공했고 후속 읽기 전용 조회에서 key `TMI-9`, 승인된 제목과 구조화된 설명, 우선순위 `High`, 기본 상태 `해야 할 일`, 담당자 없음과 빈 라벨을 확인했다. 애플리케이션 코드 변경이 없어 `./gradlew clean test`는 실행하지 않았다. 문서 변경 후 `git diff --check`와 현재 turn marker 검사를 실행해 통과했다.
- 유지한 계약: 실제 사용자 ID의 UUID 및 검증된 JWT `sub` 사용, RS256·필수 `kid`·issuer·`tosunsaeng-learning-core` audience·JWKS, Refresh Token 원문 비저장과 Identity 도메인 경계를 승인된 설명에 유지했다. Jira와 기록에 Secret, 실제 Token 값, Password, 전체 MongoDB URI, RSA Private Key 또는 개인정보를 포함하지 않았다.
- 결정사항: 사용자 명시적 승인을 근거로 Jira `TMI-9` 신규 생성만 수행했다. Jira 작업은 새 이슈 생성, 댓글 목적은 해당 없음, 상태는 기본 `해야 할 일`, 우선순위는 `High`, 승인 여부는 승인됨이며 담당자·스프린트·에픽·라벨과 별도 상태 전환은 적용하지 않았다.
- 위험 요소: `TMI-9` 구현은 아직 시작하지 않았고 현재 로컬 브랜치 이름은 완료된 기존 이슈 `TMI-6`을 가리킨다. 구현 착수 전 Jira key를 포함하는 브랜치 운영과 프로필 `provider` 호환, 다중 Session 폐기의 부분 실패 및 JWT Clock 오차 정책을 구체화해야 한다.
- 다음 작업: 새 작업 브랜치에서 Atlassian MCP로 `TMI-9` 설명과 완료 조건을 다시 읽은 뒤 구현·테스트를 진행한다. Jira 댓글이나 상태 변경은 별도 사용자 승인 전까지 수행하지 않는다.

## 2026-07-27 — TMI-9 현재 상태 및 가능한 전환 조회

<!-- codex-turn:019fa1b1-8a2c-7e62-ba5a-7a05f7c5c903 -->

- 날짜: 2026-07-27
- 브랜치: `feat/TMI-6-refresh-token-lifecycle`
- Jira: `TMI-9`
- 작업 목표: Atlassian 공식 MCP로 Jira `TMI-9`의 제목, 현재 상태와 현재 계정에서 실행 가능한 상태 전환 목록을 읽기 전용으로 조회한다.
- 변경 파일: 이번 작업으로는 `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 변경했다. 애플리케이션 코드와 기존 WORKLOG 항목은 건드리지 않았고 Jira 데이터도 변경하지 않았다.
- 구현 내용: 이슈 조회에는 제목과 상태만 요청하고 최근 조회 기록을 갱신하지 않도록 설정했다. 사용 불가능한 전환을 제외한 별도 조회에서 제목 `[Identity] JWT 인증 적용·내 프로필 조회·전체 로그아웃 구현`, 현재 상태 `해야 할 일`과 가능한 전환 `해야 할 일(11)`, `검토 중(31)`, `진행 중(21)`, `완료(41)`를 확인했다. 네 전환은 모두 현재 사용 가능으로 반환됐다.
- 실행한 테스트와 결과: Atlassian MCP의 이슈 및 전환 읽기 전용 조회가 모두 성공했다. 애플리케이션 코드 변경이 없어 `./gradlew clean test`는 실행하지 않았다. 문서 변경 후 `git diff --check`와 현재 turn marker 검사를 실행해 통과했다.
- 유지한 계약: Identity 도메인 경계, UUID `userId`, RS256·`kid`·issuer·audience·JWKS와 Refresh Token 원문 DB·로그 비저장 계약을 변경하지 않았다. Secret, 실제 Token 값, Password, 전체 MongoDB URI, RSA Private Key 또는 개인정보를 기록하지 않았다.
- 결정사항: 이번 Jira 작업은 `TMI-9` 이슈와 가능한 전환 조회 두 건뿐이다. 상태 변경은 없음, 댓글 목적은 해당 없음이며 이슈 편집·댓글·전환 또는 다른 이슈 조회·변경을 수행하지 않았다.
- 위험 요소: 가능한 전환 목록은 Jira 워크플로 설정, 현재 상태와 계정 권한에 따라 달라질 수 있으므로 실제 전환 직전에 다시 확인해야 한다. 현재 로컬 브랜치는 완료된 기존 `TMI-6`을 가리키며 `TMI-9` 구현은 아직 시작하지 않았다.
- 다음 작업: 사용자가 특정 상태 전환을 명시적으로 승인하기 전까지 `TMI-9`의 상태와 다른 Jira 데이터는 변경하지 않는다. 구현 착수 시 Jira key가 포함된 작업 브랜치에서 이슈 설명과 완료 조건을 다시 읽는다.

## 2026-07-27 — TMI-9 진행 중 전환

<!-- codex-turn:019fa1b2-b580-71b2-94da-4cfbc4f491a0 -->

- 날짜: 2026-07-27
- 브랜치: `feat/TMI-6-refresh-token-lifecycle`
- Jira: `TMI-9`
- 작업 목표: 사용자가 방금 확인하고 승인한 `진행 중` 전환을 Jira `TMI-9`에 적용하고 다른 필드와 다른 Jira 이슈는 수정하지 않은 뒤 현재 상태만 다시 조회한다.
- 변경 파일: 저장소에서는 `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 변경했다. 애플리케이션 코드와 기존 WORKLOG 항목은 건드리지 않았다. 외부 Jira에서는 `TMI-9`의 상태만 변경했다.
- 구현 내용: 직전 읽기 전용 조회에서 사용 가능함을 확인한 `진행 중` transition ID `21`만 필드, update, 댓글과 이력 메타데이터 없이 전송했다. 전환 후 최근 조회 기록을 갱신하지 않고 `status` 필드만 다시 요청해 `TMI-9`의 상태 `진행 중`과 status ID `10001`을 확인했다. 다른 Jira 이슈는 조회하거나 변경하지 않았다.
- 실행한 테스트와 결과: Atlassian MCP 상태 전환 응답이 성공했고 후속 상태 전용 조회가 `진행 중`을 반환했다. 애플리케이션 코드 변경이 없어 `./gradlew clean test`는 실행하지 않았다. 문서 변경 후 `git diff --check`와 현재 turn marker 검사를 실행해 통과했다.
- 유지한 계약: Jira 상태 외에 제목, 설명, 우선순위, 담당자, 스프린트, 에픽, 라벨, 댓글과 다른 필드를 변경하지 않았다. Identity 도메인 경계, UUID `userId`, RS256·`kid`·issuer·audience·JWKS와 Refresh Token 원문 비저장 계약을 변경하지 않았으며 Secret, 실제 Token 값, Password, 전체 MongoDB URI, RSA Private Key 또는 개인정보를 기록하지 않았다.
- 결정사항: 사용자 명시적 승인을 근거로 `TMI-9`에 transition ID `21`만 적용했다. 수행한 Jira 작업은 상태 `해야 할 일` → `진행 중`, 댓글 목적은 해당 없음, 다른 필드·이슈 변경은 없음이며 승인 여부는 승인됨이다.
- 위험 요소: Jira 상태는 `진행 중`이지만 현재 로컬 브랜치는 완료된 기존 `TMI-6`을 가리키고 애플리케이션 구현은 아직 시작하지 않았다. Jira 상태는 이후 다른 사용자나 자동화에 의해 변경될 수 있다.
- 다음 작업: Jira key가 포함된 작업 브랜치에서 `TMI-9` 설명과 완료 조건을 다시 읽고 구현·테스트를 진행한다. 추가 Jira 댓글이나 상태 변경은 별도 사용자 승인 전까지 수행하지 않는다.

## 2026-07-27 — TMI-9 JWT 인증·내 프로필·전체 로그아웃 구현

<!-- codex-turn:019fa1b6-532e-74a0-81bd-4e22002367ab -->

- 날짜: 2026-07-27
- 브랜치: `feat/TMI-9-identity-jwt-auth`
- Jira: TMI-9
- 작업 목표: Jira `TMI-9`의 설명과 완료 조건을 기준으로 Identity에 RS256 JWT Resource Server 인증, JWT subject 기반 내 프로필 조회와 현재 사용자의 전체 RefreshSession 로그아웃을 구현하고 공개·보호 경로 및 민감정보 경계를 전체 회귀로 검증한다.
- 변경 파일: `build.gradle`, `README.md`, `SecurityConfig.java`, `JwtConfiguration.java`, 신규 `JwtAudienceValidator.java`, 신규 CurrentUserProvider 2개, 신규 401/403 Security Handler 2개, `CommonErrorStatus.java`, `AuthController.java`, 신규 `LogoutAllService.java`, `RefreshSession.java`, `RevocationReason.java`, `User.java`, 신규 `UserProvider.java`, 신규 User Controller·Service·Response DTO·오류 enum, 관련 기존 테스트 6개와 신규 Security·JWT·CurrentUser·프로필·전체 로그아웃 테스트 7개, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 변경했다. 작업 시작 전부터 수정돼 있던 과거 WORKLOG와 TMI-6/TMI-9 Jira 운영 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 작업 전에 `AGENTS.md`, Identity–Learning Core JWT 계약과 CURRENT_STATE를 끝까지 읽고 Atlassian 공식 MCP로 `TMI-9`의 설명·완료 조건·상태를 읽기 전용 조회해 충돌이 없음을 확인했다. 기존 RSA Public Key Bean과 Clock으로 자기 JWKS HTTP 호출 없는 `NimbusJwtDecoder`를 구성하고 RS256, 정확한 `typ=JWT`, 현재 `kid`, 필수 subject·만료 Claim, `exp`·`nbf`, issuer와 audience를 검증했다. 지정된 공개 POST·GET 경로만 허용하고 나머지를 인증하도록 SecurityFilterChain을 전환했으며 401/403을 UTF-8 BaseResponse JSON으로 처리했다. `JwtCurrentUserProvider`가 검증된 JWT subject를 canonical UUID로 제공하고, `/api/v1/users/me`는 해당 User의 ACTIVE 상태를 확인해 LOCAL provider와 음성 동의 상태만 전용 DTO로 반환한다. `/api/v1/auth/logout-all`은 현재 사용자의 미폐기 RefreshSession만 같은 Clock 시각과 `LOGOUT_ALL` 사유로 폐기해 `saveAll`하고 빈 목록·반복 요청은 성공하며 Repository 오류는 전파한다.
- 실행한 테스트와 결과: 변경 전 `./gradlew test` 기준 97개가 통과했다. 구현 중 main·test 컴파일과 관련 Security·JWT·프로필·전체 로그아웃 테스트를 반복 실행해 fixture 경계값과 Mockito stubbing 문제를 수정했고, 최종 `./gradlew clean test`는 전체 138개, 실패 0개, 오류 0개, 건너뜀 0개로 성공했다. 실제 Atlas, 운영 RSA Key 또는 외부 OAuth Provider는 호출하지 않았다. 실제 키 본문·서명 JWT 문자열·자격증명 포함 MongoDB URI·민감 로그·Entity 직접 응답·과도한 공개 경로·자기 JWKS HTTP 호출 정적 검색과 `git diff --check`도 통과했다.
- 유지한 계약: 실제 사용자 ID는 UUID 문자열이며 Request Body, Path 또는 Query 값이 아니라 검증된 JWT `sub`만 사용한다. 기존 RS256, 필수 `kid`, issuer, `tosunsaeng-learning-core` audience, Public Key 전용 JWKS와 최소 Claim 계약을 유지하고 Private Key를 검증에 사용하지 않았다. Refresh Token 원문은 저장·로그·응답하지 않고 프로필 응답에는 비밀번호 해시, 정규화 이메일, RefreshSession 내부 정보, 시험·스트릭·학습 데이터를 포함하지 않았다. Learning Core 코드와 Python AI의 `examId` 경계는 변경하지 않았다.
- 결정사항: Spring Security 6.4와 Nimbus에서 exact `typ=JWT` verifier를 적용하고 주입 Clock에 zero-skew 시간 검증을 사용했다. Identity 보호 API도 이번에는 기존 Learning Core audience를 그대로 검증하며 Identity 전용 또는 다중 audience는 도입하지 않았다. scope Claim의 `SCOPE_` 변환은 확인했지만 endpoint별 scope 권한은 강제하지 않았다. 이메일 계정 provider는 최소 `LOCAL` enum으로 저장하고 기존 null 문서는 LOCAL로 해석한다. 공통 Security 오류 코드는 `COMMON_UNAUTHORIZED`와 `COMMON_FORBIDDEN`으로 반환한다. 이번 Jira 작업은 `TMI-9` 읽기 전용 조회뿐이며 댓글 목적은 해당 없음, 댓글·필드·상태 변경은 없음이고 쓰기 승인도 사용하지 않았다.
- 위험 요소: 현재 Identity 보호 API와 Learning Core가 같은 audience를 사용하므로 토큰 용도 분리 여부를 후속 검토해야 한다. endpoint별 scope 인가가 아직 없고 다중 키 Rotation도 구현되지 않았다. logout-all의 여러 문서 저장은 MongoDB Transaction이 아니므로 중간 실패 시 일부만 반영될 수 있으며 Optimistic Lock 오류를 포함한 저장 오류는 호출자에게 전파된다. 전체 로그아웃 후에도 이미 발급된 Access Token은 만료 시각까지 유효할 수 있어 클라이언트가 성공 즉시 로컬 인증 정보를 삭제해야 한다. 기존 User provider 이행과 소셜 계정 연결 정책도 소셜 로그인 전에 필요하다.
- 다음 작업: 사용자가 변경을 검토한 뒤 직접 커밋·push하고 PR 병합을 확인한다. Jira 완료 댓글과 상태 변경은 별도 승인 전까지 수행하지 않는다. 다음 Learning Core 작업에서는 Identity JWKS로 RS256 서명·issuer·audience를 로컬 검증하고 검증된 JWT `sub`를 실제 userId로 사용하되 Python AI payload의 `user_id`는 계속 `examId`로 유지한다.

## 2026-07-27 — TMI-9 완료 전환 사전 확인

<!-- codex-turn:019fa213-2031-7663-910f-7323179fabe2 -->

- 날짜: 2026-07-27
- 브랜치: `feat/TMI-9-identity-jwt-auth`
- Jira: TMI-9
- 작업 목표: 사용자의 Jira 종료 요청에 따라 적용할 변경을 먼저 제시하고, 저장소 규칙상 완료 전환에 필요한 PR의 main 병합 확인과 명시적 승인을 요청한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 변경했다. 애플리케이션 코드, 기존 WORKLOG 항목과 Jira 데이터는 변경하지 않았다.
- 구현 내용: Jira `TMI-9`에 적용할 변경을 상태 `진행 중`에서 `완료`로의 transition ID `41` 한 건으로 한정하고 댓글·필드는 변경하지 않는다고 사용자에게 제시했다. CURRENT_STATE에는 읽기 전용 재조회 기준 현재 상태, 사용 가능한 완료 전환, PR 병합 확인 및 명시적 승인 대기와 Jira 쓰기 작업 미수행 상태를 반영했다.
- 실행한 테스트와 결과: 애플리케이션 코드 변경이 없어 테스트를 다시 실행하지 않았으며, 직전 구현 작업의 `./gradlew clean test` 결과는 전체 138개 성공, 실패·오류 0개다. 이번 turn에서는 Jira 호출을 수행하지 않았다. 문서 변경 후 `git diff --check`와 지정된 turn marker 검사를 실행해 통과했다.
- 유지한 계약: PR 병합 확인 전 Jira를 완료로 변경하지 않고, 상태 전환 전에 정확한 변경을 보여주고 명시적 승인을 받는 규칙을 유지했다. Identity의 UUID 사용자 ID, RS256·issuer·audience·JWKS와 Refresh Token 비저장 계약을 변경하지 않았으며 Secret, 실제 Token 값, Password, 전체 MongoDB URI, RSA Private Key 또는 개인정보를 기록하지 않았다.
- 결정사항: 현재 요청만으로 PR의 main 병합 사실을 추정하지 않는다. 수행 예정 Jira 작업은 `TMI-9`의 transition ID `41` 적용뿐이고 댓글 목적과 필드 변경은 해당 없으며, PR 병합 확인과 제시된 전환에 대한 사용자 답변 전까지 Jira 쓰기 승인은 대기 상태다.
- 위험 요소: PR 병합 여부가 아직 확인되지 않았고 Jira 상태나 사용 가능한 전환은 다른 사용자 또는 워크플로 변경으로 달라질 수 있으므로 실제 전환 직전에 다시 조회해야 한다.
- 다음 작업: 사용자가 PR의 main 병합과 transition ID `41` 적용을 명시적으로 승인하면 Atlassian 공식 MCP로 상태와 전환을 재확인한 뒤 `TMI-9` 상태만 `완료`로 변경하고 결과를 읽기 전용으로 검증한다. 댓글·필드와 다른 이슈는 변경하지 않는다.

## 2026-07-27 — TMI-9 완료 전환

<!-- codex-turn:019fa216-fccc-7493-8b45-160cb1170d50 -->

- 날짜: 2026-07-27
- 브랜치: `feat/TMI-9-identity-jwt-auth`
- Jira: TMI-9
- 작업 목표: 사용자가 PR의 main 병합과 사전에 제시한 TMI-9의 `진행 중` → `완료` 전환을 확인·승인한 것을 근거로 Jira 상태만 완료로 변경하고 결과를 검증한다.
- 변경 파일: 저장소에서는 `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 변경했다. 애플리케이션 코드와 기존 WORKLOG 항목은 수정하지 않았다. 외부 Jira에서는 `TMI-9`의 상태만 변경했다.
- 구현 내용: 전환 직전 Atlassian 공식 MCP로 `TMI-9`가 status ID `10001`의 `진행 중`, Resolution 없음인지 확인하고 transition ID `41`이 사용 가능한 status ID `10003`의 `완료` 전환인지 재확인했다. 필드, update, 댓글과 이력 메타데이터 없이 transition ID `41`만 전송한 뒤 읽기 전용 재조회에서 상태와 Resolution이 모두 `완료`임을 확인했다.
- 실행한 테스트와 결과: Jira 상태·전환 사전 조회, 상태 전환과 전환 후 조회가 모두 성공했다. 애플리케이션 코드 변경이 없어 테스트는 다시 실행하지 않았으며 직전 구현 작업의 `./gradlew clean test`는 전체 138개 성공, 실패·오류 0개였다. 문서 변경 후 `git diff --check`와 현재 turn marker 검사를 실행해 통과했다.
- 유지한 계약: 사용자 승인 범위인 TMI-9의 상태 전환만 수행하고 설명, 우선순위, 담당자, 라벨, 댓글과 다른 이슈는 변경하지 않았다. Identity의 UUID 사용자 ID, RS256·issuer·audience·JWKS와 Refresh Token 비저장 계약을 변경하지 않았으며 Secret, 실제 Token 값, Password, 전체 MongoDB URI, RSA Private Key 또는 개인정보를 기록하지 않았다.
- 결정사항: 사용자의 `해줘` 답변을 직전에 제시한 PR main 병합 확인과 transition ID `41` 적용에 대한 명시적 승인으로 사용했다. 수행한 Jira 작업은 상태 `진행 중` → `완료` 한 건이고 댓글 목적과 필드 변경은 해당 없으며 승인 여부는 승인됨이다.
- 위험 요소: PR의 main 병합 여부는 사용자 확인을 근거로 했으며 이 turn에서 Git 원격 상태나 전체 테스트를 독립적으로 재검증하지 않았다. Jira 상태는 이후 다른 사용자나 자동화에 의해 변경될 수 있다.
- 다음 작업: `TMI-9`에 추가 Jira 변경은 별도 요청과 승인 전까지 수행하지 않는다. 다음 Learning Core 작업에서는 Identity JWKS로 RS256 서명·issuer·audience를 로컬 검증하고 검증된 JWT `sub`를 실제 userId로 사용한다.

## 2026-07-27 — Learning Core JWT 연동 Jira Payload 초안

<!-- codex-turn:019fa231-db12-79d3-830e-22bd21b99f75 -->

- 날짜: 2026-07-27
- 브랜치: `feat/TMI-9-identity-jwt-auth`
- 작업 목표: TMI 프로젝트에 생성할 Learning Core의 Identity JWKS 기반 JWT 인증 연동 `작업` 이슈의 최종 Payload를 검증해 먼저 제시하고, 사용자 승인 전에는 이슈를 생성하지 않는다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 변경했다. 애플리케이션 코드와 기존 WORKLOG 항목은 수정하지 않았고 Jira 데이터도 변경하지 않았다.
- 구현 내용: `AGENTS.md`, `docs/codex/CURRENT_STATE.md`와 Identity–Learning Core JWT 계약을 확인했다. Atlassian 공식 MCP의 읽기 전용 호출로 `to-teacher` 사이트의 TMI 프로젝트에 이슈 생성 권한이 있고 `작업` 유형 ID가 `10003`임을 확인했으며, 전체 생성 필드 20개와 `High` 우선순위 ID `2` 지원을 검증했다. 요청받은 배경, 구현 범위, 인증 모드, 설정, 공개·보호 Endpoint, 완료 조건, 범위 제외와 보안 요구사항을 Markdown 설명으로 구성하고 실제 전송 예정 필드를 정리했다.
- 실행한 테스트와 결과: Jira 접근 가능 리소스, TMI 생성 권한, 이슈 유형과 생성 필드 메타데이터 조회가 모두 성공했다. 애플리케이션 코드 변경이 없어 `./gradlew clean test`는 실행하지 않았으며, 문서 변경 후 `git diff --check`와 현재 turn marker 단일 존재 검사를 통과했다.
- 유지한 계약: Access Token의 RS256·`kid`·issuer·`tosunsaeng-learning-core` audience·JWKS 검증, JWT `sub`의 실제 UUID userId 사용, 외부 Request/Response의 userId 비추가, Python AI `user_id = examId`, Learning Core의 RSA Private Key 비보유와 매 요청 Identity 확인 API 미호출 계약을 유지했다. Secret, 실제 Token, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: Jira 설명은 Markdown으로 전송하고 우선순위는 지원이 확인된 `High`를 포함한다. 전송 필드는 프로젝트, `작업` 유형, 제목, 설명과 우선순위로 한정하며 담당자, 상위 항목, 라벨, 스프린트, 상태 전환과 댓글은 포함하지 않는다. 이번 Jira 작업은 읽기 전용 조회뿐이고 쓰기 승인과 이슈 키는 아직 없으며 이슈 생성·수정·댓글·상태 변경은 수행하지 않았다.
- 위험 요소: Jira 생성 메타데이터나 권한은 실제 생성 시점 전에 변경될 수 있다. 개발용 issuer와 JWKS URL은 로컬 주소이므로 배포 환경에서는 정확히 일치하는 환경변수 값과 HTTPS 주소가 필요하며, Identity와 Learning Core 로컬 통합 테스트에는 두 서비스의 실행 환경이 필요하다.
- 다음 작업: 사용자가 제시된 최종 Payload를 명시적으로 승인하면 Atlassian 공식 MCP로 TMI `작업` 이슈 한 건만 생성하고 결과 이슈 키를 확인한다. 생성 이후 담당자·라벨·댓글·상태는 별도 요청과 승인 전까지 변경하지 않는다.

## 2026-07-27 — TMI-10 Learning Core JWT 연동 이슈 생성

<!-- codex-turn:019fa261-8113-76c2-b6d0-9dbb9a970074 -->

- 날짜: 2026-07-27
- 브랜치: `feat/TMI-9-identity-jwt-auth`
- Jira: TMI-10
- 작업 목표: 사용자가 승인한 최종 Payload의 제목과 설명을 그대로 사용해 TMI 프로젝트에 Learning Core의 Identity JWKS 기반 JWT 인증 연동 `작업` 이슈를 생성하고 기본 상태를 확인한다.
- 변경 파일: 저장소에서는 `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 변경했다. 애플리케이션 코드와 기존 WORKLOG 항목은 수정하지 않았다. 외부 Jira에는 `TMI-10` 이슈 한 건을 생성했다.
- 구현 내용: Atlassian 공식 MCP로 승인된 제목과 Markdown 설명, `작업` 유형과 `High` 우선순위만 전송해 `TMI-10`을 생성했다. 담당자, 스프린트, 에픽, 라벨과 생성 시 상태 전환은 전송하지 않았다. 생성 후 읽기 전용 조회에서 제목, `작업` 유형 ID `10003`, `High` 우선순위 ID `2`, 기본 상태 `해야 할 일` ID `10000`, 담당자 없음과 빈 라벨을 확인했다.
- 실행한 테스트와 결과: Jira 이슈 생성과 생성 후 읽기 전용 검증이 모두 성공했다. 애플리케이션 코드 변경이 없어 `./gradlew clean test`는 실행하지 않았으며, 문서 변경 후 `git diff --check`와 현재 turn marker 단일 존재 검사를 통과했다.
- 유지한 계약: 승인된 이슈 설명의 RS256·issuer·audience·JWKS 검증, JWT `sub`의 실제 UUID userId 사용, Python AI `user_id = examId`, 외부 Request/Response의 userId 비추가와 Learning Core의 RSA Private Key 비보유 계약을 유지했다. Secret, 실제 Token, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 Jira나 작업 기록에 추가하지 않았다.
- 결정사항: 사용자의 명시적 승인을 이슈 생성 한 건에만 사용했다. 사용한 Jira 이슈 키는 `TMI-10`, 수행한 Jira 작업은 이슈 생성, 댓글 목적은 해당 없음, 변경한 상태는 없고 프로젝트 기본 상태 `해야 할 일`을 유지했으며 승인 여부는 승인됨이다. 담당자·스프린트·에픽·라벨·댓글·상태 전환은 적용하지 않았다.
- 위험 요소: Learning Core 구현과 두 서비스의 로컬 통합 테스트는 아직 수행하지 않았다. Jira의 상태나 필드는 이후 사용자 또는 자동화에 의해 변경될 수 있으며, 현재 Identity 저장소 브랜치는 이전 Identity 작업 키를 유지하고 있어 Learning Core 구현은 대상 저장소에서 `TMI-10`을 기준으로 진행해야 한다.
- 다음 작업: Learning Core 저장소에서 구현 전에 `TMI-10`을 읽고 설명과 완료 조건을 기준으로 JWT 연동을 구현한다. Jira 댓글·상태·필드 변경은 실행 내용을 먼저 제시하고 별도 승인을 받은 뒤 수행한다.

## 2026-07-27 — TMI-10 상태와 전환 조회

<!-- codex-turn:019fa27f-5e9b-7cc3-9fb7-4b313f8ef246 -->

- 날짜: 2026-07-27
- 브랜치: `feat/TMI-9-identity-jwt-auth`
- Jira: TMI-10
- 작업 목표: Atlassian 공식 MCP로 `TMI-10`의 제목, 현재 상태와 현재 가능한 상태 전환만 읽기 전용으로 조회하고 Jira를 수정하지 않는다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 변경했다. 애플리케이션 코드와 기존 WORKLOG 항목은 수정하지 않았고 외부 Jira 데이터도 변경하지 않았다.
- 구현 내용: `TMI-10`의 제목이 `[Learning Core] Identity JWKS 기반 JWT 인증 연동`, 현재 상태가 status ID `10000`의 `해야 할 일`임을 확인했다. 현재 사용 가능한 전환은 `해야 할 일` ID `11`, `검토 중` ID `31`, `진행 중` ID `21`, `완료` ID `41`로 조회됐다.
- 실행한 테스트와 결과: Jira 이슈 상세와 사용 가능한 전환의 읽기 전용 조회가 모두 성공했다. 애플리케이션 코드 변경이 없어 `./gradlew clean test`는 실행하지 않았으며, 문서 변경 후 `git diff --check`와 현재 turn marker 단일 존재 검사를 통과했다.
- 유지한 계약: Jira 조회만 수행하고 이슈 생성·수정·댓글·상태 전환·삭제 호출을 하지 않았다. Identity–Learning Core의 RS256·issuer·audience·JWKS, UUID `sub`, Python AI `user_id = examId` 계약을 변경하지 않았으며 Secret, 실제 Token, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: 사용자 요청 범위를 제목·현재 상태·가능한 전환 조회로 한정했다. 수행한 Jira 작업은 읽기 전용 조회이고 댓글 목적과 변경한 상태는 해당 없으며 쓰기 승인도 사용하지 않았다. 현재 상태로 되돌아가는 전환 ID `11`도 MCP가 사용 가능하다고 반환했으므로 결과에 포함한다.
- 위험 요소: Jira의 현재 상태와 사용 가능한 전환은 다른 사용자, 자동화 또는 워크플로 변경에 따라 달라질 수 있으므로 실제 상태 변경 직전에 다시 조회해야 한다.
- 다음 작업: 상태 전환이나 다른 Jira 변경이 요청되면 적용할 정확한 변경 내용을 먼저 제시하고 명시적 승인을 받은 뒤, 실행 직전에 상태와 전환을 재확인한다.

## 2026-07-27 — TMI-10 진행 중 전환

<!-- codex-turn:019fa285-f6aa-7140-b4b5-ef2a8a4b530c -->

- 날짜: 2026-07-27
- 브랜치: `feat/TMI-9-identity-jwt-auth`
- Jira: TMI-10
- 작업 목표: 사용자의 명시적 요청에 따라 방금 확인한 `TMI-10`의 `진행 중` 전환만 적용하고 전환 후 현재 상태만 다시 조회한다.
- 변경 파일: 저장소에서는 `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 변경했다. 애플리케이션 코드와 기존 WORKLOG 항목은 수정하지 않았다. 외부 Jira에서는 `TMI-10`의 상태만 변경했다.
- 구현 내용: 전환 직전 `TMI-10`이 status ID `10000`의 `해야 할 일`이고 transition ID `21`이 사용 가능한 status ID `10001`의 `진행 중` 전환임을 재확인했다. 필드, update와 이력 메타데이터 없이 transition ID `21`만 전송한 뒤 현재 상태만 읽기 전용으로 조회해 `진행 중`을 확인했다.
- 실행한 테스트와 결과: Jira 상태·대상 전환 사전 조회, 상태 전환과 전환 후 상태 조회가 모두 성공했다. 애플리케이션 코드 변경이 없어 `./gradlew clean test`는 실행하지 않았으며, 문서 변경 후 `git diff --check`와 현재 turn marker 단일 존재 검사를 통과했다.
- 유지한 계약: 사용자 승인 범위인 `TMI-10`의 상태 전환만 수행하고 다른 필드·댓글·이슈는 변경하지 않았다. Identity–Learning Core의 RS256·issuer·audience·JWKS, UUID `sub`, Python AI `user_id = examId` 계약을 변경하지 않았으며 Secret, 실제 Token, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: 사용자의 요청을 직전에 확인해 제시한 `진행 중` transition ID `21` 적용에 대한 명시적 승인으로 사용했다. 수행한 Jira 작업은 `TMI-10` 상태 `해야 할 일` → `진행 중` 한 건이고 댓글 목적은 해당 없으며 승인 여부는 승인됨이다. 다른 필드와 다른 이슈는 변경하지 않았다.
- 위험 요소: Jira 상태는 이후 다른 사용자나 자동화에 의해 변경될 수 있다. Learning Core 구현과 Identity–Learning Core 로컬 통합 테스트는 아직 수행하지 않았다.
- 다음 작업: Learning Core 저장소에서 `TMI-10`의 설명과 완료 조건을 기준으로 JWT 연동을 구현한다. Jira 댓글·추가 상태·필드 변경은 실행 내용을 먼저 제시하고 별도 승인을 받은 뒤 수행한다.

## 2026-07-29 — RsaKeyLoader ResourceLoader 주입 모호성 해소

<!-- codex-turn:019faccb-2b87-71e2-9b9a-f0c15eae8291 -->

- 날짜: 2026-07-29
- 브랜치: `feat/TMI-9-identity-jwt-auth`
- Jira: TMI-9
- 작업 목표: IDE가 `RsaKeyLoader` 생성자의 `ResourceLoader` 후보로 `gridFsTemplate`과 `webApplicationContext`를 함께 인식해 표시하는 자동 주입 모호성을 제거하고 기존 RSA Key Resource 로딩 동작을 유지한다.
- 변경 파일: `src/main/java/web/tosunsaeng/identity/security/jwt/RsaKeyLoader.java`, `src/main/java/web/tosunsaeng/identity/security/jwt/JwtConfiguration.java`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`
- 구현 내용: `RsaKeyLoader`에서 `@Component`를 제거해 생성자가 일반 `ResourceLoader` 빈 자동 주입 지점으로 해석되지 않게 했다. `JwtConfiguration`에 기존과 같은 이름의 `rsaKeyLoader` Bean을 선언하고 Spring이 유일하게 제공하는 `ApplicationContext`를 생성자에 전달했다. `ApplicationContext`가 `ResourceLoader`를 구현하므로 기존 `file:`·`classpath:` Resource 해석과 Key 로딩 시점은 바꾸지 않았다.
- 실행한 테스트와 결과: 첫 `./gradlew clean test`는 샌드박스가 사용자 Gradle 캐시의 잠금 파일 접근을 막아 테스트 실행 전에 종료됐다. 필요한 접근 권한으로 같은 명령을 다시 실행해 전체 138개 테스트가 성공했고 실패·오류·건너뜀은 모두 0개였다.
- 유지한 계약: RSA Private Key는 Identity에만 유지하고 PKCS#8 Private Key·X.509 Public Key, RS256·`kid`·issuer·`tosunsaeng-learning-core` audience·JWKS와 UUID JWT `sub` 계약을 변경하지 않았다. Jira `TMI-9`의 설명과 완료 조건은 Atlassian 공식 MCP로 읽기 전용 확인만 했고 이슈·댓글·상태·필드는 변경하지 않았다. Secret, 실제 Token 값, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 코드나 문서에 기록하지 않았다.
- 결정사항: 문자열 `@Qualifier`로 특정 구현 이름에 결합하지 않고, 구성 경계인 `JwtConfiguration`에서 목적에 맞는 `ApplicationContext`를 명시적으로 선택했다. `RsaKeyLoader`의 공개 생성자와 `rsaKeyLoader` Bean 이름은 유지했다. 이번 Jira 작업은 읽기 전용 조회뿐이며 댓글 목적, 상태 변경과 쓰기 승인은 해당 없다.
- 위험 요소: 코드와 Spring 컨텍스트 검증은 통과했지만 IDE가 기존 인덱스를 보존 중이면 경고가 사라지기 전에 Gradle 동기화가 필요할 수 있다.
- 다음 작업: IDE에서 Gradle 프로젝트를 동기화한 뒤 `RsaKeyLoader` 생성자의 자동 주입 모호성 경고가 제거됐는지 확인한다. Jira 댓글이나 상태 변경은 별도 요청과 승인 전까지 수행하지 않는다.

## 2026-07-29 — Identity 도메인 중심 구조 리팩토링과 OpenAPI 강화

<!-- codex-turn:019faceb-4122-7d02-b431-317cec4545a2 -->

- 날짜: 2026-07-29
- 브랜치: `feat/TMI-9-identity-jwt-auth`
- Jira: TMI-9
- 작업 목표: `web-back-end`의 도메인 중심 배치 방식을 참고하되 Identity의 RS256·JWKS·RefreshSession 보안 계약에 맞게 전체 패키지를 `domain`과 `global`로 재구성하고, 비대해진 AuthService를 유스케이스별로 분리하며 OpenAPI 문서와 회귀 검증을 강화한다.
- 변경 파일: `.env.example`, `README.md`, `src/main/resources/application.yml`, `src/test/resources/application-test.yml`, 기존 `src/main/java/web/tosunsaeng/identity/{auth,user,common,config,security}/**`를 이동·재구성한 `src/main/java/web/tosunsaeng/identity/domain/**`와 `global/**` 전체, 대응하는 `src/test/java`의 `domain/**`와 `global/**`, `src/test/java/web/tosunsaeng/identity/IdentityApplicationTests.java`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 변경했다. 기존 미커밋 `RsaKeyLoader`·`JwtConfiguration` 수정은 내용 손실 없이 새 `global.security.jwt` 경로로 이동했다.
- 구현 내용: 두 저장소의 Java/Spring Boot 버전, Gradle 의존성, 패키지, Controller와 URL, AuthService 책임, JWT, RefreshSession, SecurityConfig, springdoc, 공통 응답, 예외 처리, 테스트, 환경설정과 Docker/배포 경로 의존성을 먼저 비교했다. 참고 저장소에서는 `domain`·`global` 배치만 차용하고 대칭키 JWT·전역 Swagger 인증·시험 도메인 코드는 가져오지 않았다. Identity의 Auth와 User 코드를 API/application/DTO/domain/exception으로 이동하고 공통 응답·전역 예외·설정·JWT·현재 사용자·Security Handler를 `global`로 이동했다. `RefreshSession`·`RevocationReason`·Repository는 Auth 도메인에, Refresh Token 난수 생성·해싱·설정은 `global.security.refresh`에 배치했다. 기존 AuthService는 제거하고 이메일 확인·회원가입·로그인·재발급·단일 로그아웃 서비스로 분리했으며 전체 로그아웃 서비스는 유지했다. 다중 토큰 응답 조합은 `AuthResponseConverter`로 통합하고 Auth/User 전용 예외를 기존 BusinessException 하위 타입으로 추가했다.
- 실행한 테스트와 결과: 변경 전 `./gradlew clean test`에서 138개 전체 성공을 기준선으로 확보했다. global 이동, User 이동, Auth/RefreshSession 이동과 서비스 분리의 각 단계에서 컴파일 또는 전체 테스트를 실행해 모두 성공시켰다. OpenAPI 계약 테스트 2개를 추가한 최종 `./gradlew clean test`는 전체 140개 성공, 실패·오류·건너뜀 0개였고 `./gradlew build`도 `bootJar`, `jar`, `check`, `build`까지 성공했다. 별도 formatter·checkstyle·spotless 플러그인은 build 설정에 없었으며, 최종 `git diff --check`, 이전 패키지 참조·Controller의 Repository 직접 의존·global의 domain 구현 역참조·민감 Swagger 예시 정적 검사를 통과했다.
- 유지한 계약: 모든 API URL·HTTP Method·요청/응답 JSON 필드·HTTP 상태·BaseResponse의 `isSuccess/code/message/result`·validation null 마스킹을 유지했다. `users`와 `refresh_sessions` collection, Mongo 필드·unique/TTL index·Optimistic Lock, BCrypt, Access/Refresh Token TTL, Opaque Refresh Token Rotation·재사용 탐지·단일/전체 로그아웃 의미를 변경하지 않았다. JWT의 RS256·`kid`·issuer·`tosunsaeng-learning-core` audience·UUID `sub`·JWKS와 Learning Core 로컬 검증 계약을 유지했고 실제 `userId`를 외부 요청에 추가하거나 Python AI 서버로 보내지 않았다. Secret, 실제 Token 값, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 코드·Swagger·문서에 기록하지 않았다.
- 결정사항: 단일 구현체 서비스에 의미 없는 인터페이스/Impl 계층은 만들지 않았고, AccessTokenIssuer처럼 기술 경계 역할을 하는 기존 포트는 유지했다. MongoTransactionManager가 없는 상태에서 구조 변경과 저장 의미 변경을 섞지 않기 위해 새 `@Transactional`은 추가하지 않았다. OpenAPI `bearerAuth`는 전역 적용하지 않고 보호 API인 `/api/v1/users/me`와 `/api/v1/auth/logout-all`에만 선언했으며 공개 API에는 인증 표시가 없음을 테스트했다. 비밀번호는 Swagger에서 password format과 write-only로 표시하고 예시는 추가하지 않았다. Swagger는 `SWAGGER_ENABLED`로 끌 수 있으며 테스트 프로필은 문서 검증을 위해 활성화한다. Jira `TMI-9`는 구현 전 설명·완료 조건·완료 상태를 Atlassian 공식 MCP로 읽기 전용 확인했으며 댓글·필드·상태와 다른 이슈는 변경하지 않았고 쓰기 승인은 사용하지 않았다.
- 위험 요소: Java FQCN을 직접 사용하는 외부 모듈이 별도로 있다면 새 `domain`·`global` import 경로로 갱신해야 한다. MongoDB Transaction 부재로 Rotation과 다중 Session 폐기의 부분 실패 위험은 기존과 동일하게 남아 있다. OpenAPI 응답 설명은 후속 오류·보안 정책 변경 시 코드와 함께 갱신해야 하며 운영 배포에서는 필요에 따라 Swagger를 비활성화해야 한다. 현재 브랜치의 원격 추적 참조가 사라진 상태이므로 사용자가 후속 Git 작업 전에 대상 브랜치와 원격 전략을 확인해야 한다.
- 다음 작업: 사용자가 대규모 이동 diff와 API 문서를 검토한 뒤 직접 커밋·push한다. 패키지 FQCN을 사용하는 별도 모듈이 있으면 import를 갱신하고, 운영 환경에서는 `SWAGGER_ENABLED`, issuer/JWKS URL과 Mongo 데이터 이행·Transaction 정책을 확인한다. Jira 댓글이나 상태 변경은 별도 요청과 명시적 승인 전까지 수행하지 않는다.

## 2026-07-30 — TMI-9 도메인 리팩토링 병합 전 리뷰

<!-- codex-turn:019fb0a8-5cb6-7512-859f-6ce10cd41c7d -->

- 날짜: 2026-07-30
- 브랜치: `feat/TMI-9-identity-jwt-auth`
- Jira: TMI-9
- 작업 목표: 현재 작업 트리의 도메인 중심 패키지 리팩토링, AuthService 분리, JWT·RefreshSession, Security, 공통 응답·예외와 OpenAPI 변경을 최신 원격 main 대비 검토하고 코드 수정 없이 병합 가능 여부를 판정한다.
- 변경 파일: 리뷰 대상은 rename detection과 untracked 파일을 함께 반영한 103개 논리 파일이며, 이번 리뷰 자체에서는 저장소 규칙에 따라 `docs/codex/CURRENT_STATE.md`와 이 WORKLOG 항목만 변경했다. Java, 설정, 테스트 코드는 수정하지 않았고 기존 WORKLOG 항목도 수정하거나 삭제하지 않았다.
- 구현 내용: `git fetch origin` 후 로컬 `main`이 최신 기준보다 4 commit 뒤임을 확인해 `origin/main` commit `53abd6e`를 비교 기준으로 사용했다. 현재 `HEAD`도 같은 commit이고 원격 feature ref는 삭제된 상태이며, 작업 트리에는 staged 삭제 52개, unstaged 경로 33개와 untracked 파일 86개가 있어 커밋된 리팩토링 diff가 전혀 없음을 확인했다. 전체 변경을 패키지·Controller·서비스·DTO·Mongo Entity/Repository·JWT·RefreshSession·Security·예외·응답·OpenAPI·설정·테스트·의존성 단위로 검토하고 참고 저장소의 도메인 중심 구조도 비교했다. API URL·Method·JSON·오류 코드, RS256·kid·issuer·audience·UUID subject·TTL, Opaque Refresh Token 해시·rotation·폐기, Mongo collection·필드·index 계약은 유지됨을 확인했다. 병합 차단 Git 상태 외에는 broad 예외 처리의 500 변환, RefreshSession 다중 쓰기의 비원자성, 패키지 의존 방향, 활성 Session 조회 index 부재, OpenAPI 오류 schema 구체성, UserFactory의 Spring 결합을 위험으로 기록했다.
- 실행한 테스트와 결과: `./gradlew tasks --all`, `./gradlew clean test`, `./gradlew build`, `./gradlew dependencies`, `./gradlew dependencyInsight --dependency springdoc --configuration runtimeClasspath`가 모두 성공했다. 현재 작업 트리는 140개 테스트가 실패·오류·건너뜀 없이 통과했고, 임시로 추출한 `origin/main` 기준선도 138개 테스트가 모두 통과했다. 별도 Checkstyle, Spotless, PMD, JaCoCo, ArchUnit, Sonar task는 없었다. boot JAR의 Start-Class와 Mongo Repository 2개 scan 및 애플리케이션 기동을 확인했다. 실행 중 `/v3/api-docs`와 `/swagger-ui/index.html`은 각각 200이었고 OpenAPI의 bearerAuth, 공개 5개 operation의 무인증 표시, 보호 2개 operation의 Bearer 표시, 비밀번호 write-only와 operationId 비충돌을 확인했다. `SWAGGER_ENABLED=false`에서는 두 문서 경로가 제거되지만 broad 예외 처리 때문에 404 대신 500을 반환함을 확인했다.
- 유지한 계약: 모든 기존 API URL·HTTP Method·요청/응답 JSON 필드·BaseResponse·오류 코드와 상태, Mongo collection·필드·unique/TTL index·Optimistic Lock, BCrypt, Access/Refresh Token 만료·rotation·로그아웃 의미를 변경하지 않았다. JWT의 RS256·`kid`·issuer·`tosunsaeng-learning-core` audience·UUID `sub`·JWKS 공개 키 전용 계약과 Refresh Token 원문 비저장을 확인했다. Secret, 실제 Token 값, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 읽거나 기록하지 않았다.
- 결정사항: 현재 상태는 테스트와 build가 성공해도 merge가 전달할 commit이 없으므로 `DO NOT MERGE`로 판정한다. 발견 사항은 BLOCKER 1개, HIGH 0개, MEDIUM 4개, LOW 2개, INFO 1개로 분류하며 기존 main에도 존재한 위험은 신규 회귀와 구분한다. Jira 작업은 키 기록뿐이고 이슈 조회·생성·수정·댓글·상태 전환을 수행하지 않았으며 쓰기 승인도 사용하지 않았다. 코드 수정, commit, push, PR 생성·병합은 수행하지 않았다.
- 위험 요소: staged 삭제만 커밋하면 기존 핵심 소스가 제거되고, 현재 브랜치를 그대로 병합하면 리팩토링이 전혀 반영되지 않는다. MongoDB transaction 부재로 rotation과 재사용·전체 로그아웃 다중 저장의 부분 실패 가능성이 남고 실제 동시 요청 보안 의미는 replica-set 기반 통합 테스트로 추가 확인해야 한다. 새 Mongo `_class` FQCN은 현재 typed repository에서는 과거 문서를 읽을 수 있으나 외부 `_class` 소비자는 코드만으로 확인할 수 없다.
- 다음 작업: 사용자가 intended replacement 파일을 모두 stage한 뒤 `git diff --cached --find-renames`를 검토하고 전체 test/build를 다시 실행해 직접 commit·push한다. 병합 전 broad HTTP 예외 매핑을 보완하고, RefreshSession transaction·재사용 정책과 운영 index를 확인하며, 패키지 의존 방향과 OpenAPI 오류 schema 개선 여부를 검토한다. Jira 댓글·상태·필드 변경은 별도 요청과 승인 전까지 수행하지 않는다.

## 2026-07-30 — TMI-9 병합 전 필수 수정과 staged diff 정상화

<!-- codex-turn:019fb139-f416-7b61-94db-33cc722f27d1 -->

- 날짜: 2026-07-30
- 브랜치: `feat/TMI-9-identity-jwt-auth`
- Jira: TMI-9
- 작업 목표: 삭제만 stage되고 신규 리팩토링 파일이 untracked였던 Git 상태를 안전하게 정상화하고, 병합 전 필수 HTTP 예외 문제와 최소 범위 패키지 의존을 수정한 뒤 전체 테스트·build·런타임 계약과 최종 staged diff를 재검증한다.
- 변경 파일: 기존 도메인 중심 리팩토링의 `src/main/java/**`, `src/test/java/**`, `.env.example`, `README.md`, `src/main/resources/application.yml`, `src/test/resources/application-test.yml`과 Codex 기록 문서를 의도한 변경으로 stage했다. 이번 후속 수정은 `global/exception/CommonErrorStatus.java`, `global/exception/GlobalExceptionHandler.java`, `domain/user/exception/UserErrorStatus.java`, `domain/user/application/UserProfileService.java`, `domain/auth/application/LoginService.java`, `domain/auth/application/TokenReissueService.java`, `domain/auth/converter/AuthResponseConverter.java`, 관련 application·Security 테스트와 신규 `DisabledSwaggerIntegrationTests.java`에 한정했다. 기존 WORKLOG 항목은 수정하거나 삭제하지 않았다.
- 구현 내용: 변경 전 `HEAD`와 `origin/main`이 같은 commit이고 index에 삭제 52개만 있으며 신규 86개 파일이 untracked임을 재확인했다. 소스·테스트와 확인된 설정·문서만 명시적으로 stage해 최초 103개 논리 파일의 rename-aware diff를 구성했고, 신규 비활성 Swagger 테스트를 포함한 최종 104개 파일에서 rename 68개와 untracked 0개를 확인했다. `HttpMessageNotReadableException`, `NoResourceFoundException`, `HttpRequestMethodNotSupportedException`, `HttpMediaTypeNotSupportedException`을 내부 메시지나 요청 원문 없이 기존 BaseResponse로 각각 400·404·405·415 변환했다. 계정 비활성 오류를 같은 `ACCOUNT_NOT_ACTIVE` 코드·403·메시지를 유지하면서 User 도메인이 소유하게 했고, converter가 application의 `IssuedRefreshSession` 타입을 역참조하지 않도록 필요한 값만 전달해 패키지 의존 방향을 단방향으로 정리했다.
- 실행한 테스트와 결과: 변경 관련 대상 테스트를 두 차례 실행해 모두 성공했다. 최종 `./gradlew clean test`는 25개 test suite의 146개 테스트가 성공했고 실패·오류·건너뜀은 모두 0개였다. `./gradlew build`도 bootJar·jar·check·build까지 성공했으며 `git diff --cached --check`를 통과했다. 실제 JAR은 외부 Mongo 의존 health와 자동 index 생성을 검증용으로 비활성화해 기동했고 Repository 2개 scan을 확인했다. Swagger 활성 상태에서 health, `/v3/api-docs`, `/swagger-ui/index.html`은 200이었고 전역 security 없음, 공개 Auth operation 5개의 security 없음과 보호 operation 2개의 bearerAuth를 확인했다. malformed JSON은 400, 미존재 공개 경로는 404, 보호 API 무인증은 401, 공개 로그인 빈 요청은 인증 단계가 아닌 validation 400, 지원하지 않는 media type은 415였다. Swagger 비활성 상태의 문서 두 경로는 공통 오류 구조의 404였다. 권한 기반 403은 실제 scope 제한 endpoint가 없어 런타임 요청은 수행하지 않았고 기존 AccessDeniedHandler 테스트가 403 계약을 검증한다.
- 유지한 계약: API URL·정상 HTTP Method·요청/응답 JSON 필드, 성공 및 기존 비즈니스 오류, BaseResponse 필드, JWT RS256·`kid`·issuer·audience·UUID `sub`·TTL, Opaque Refresh Token 해시·rotation·폐기, Mongo collection·field·unique/TTL index·Optimistic Lock과 환경변수 이름을 유지했다. 잘못된 JSON·미존재 경로·지원하지 않는 Method·Media Type의 잘못된 500만 의도한 400·404·405·415로 수정했다. Secret, 실제 Token 값, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 코드·테스트·기록에 추가하지 않았다.
- 결정사항: MongoDB replica set과 Transaction 지원 여부를 코드에서 확정할 수 없어 RefreshSession rotation·다중 폐기에 TransactionManager를 임의로 추가하지 않았다. 운영 index 상태를 알 수 없어 `{userId, revokedAt}` compound index를 추가하지 않았고 Mongo `_class` migration도 만들지 않았다. OpenAPI 오류 schema는 일반 오류와 Validation 배열을 정확히 표현하려면 문서 전용 wrapper와 다수 annotation 변경이 필요해 LOW 후속으로 유지했다. UserFactory의 Spring·시스템 시간 결합도 기능 회귀가 아니므로 이번 병합 범위에서 제외했다. 필수 parameter와 타입 변환을 사용하는 현재 공개 API가 없어 MissingServletRequestParameter와 MethodArgumentTypeMismatch의 별도 매핑은 추가하지 않았다. Jira 이슈·댓글·상태·필드는 변경하지 않았고 commit·push·merge·PR 생성도 수행하지 않았다.
- 위험 요소: RefreshSession 기존 폐기와 후속 발급, 재사용 탐지와 logout-all 다중 저장은 Mongo Transaction이 없어 부분 실패 가능성이 남아 있으며 실제 동시 재발급은 replica set 기반 통합 테스트가 필요하다. 운영 DB에 `{userId, revokedAt}` 조회 index가 없으면 활성 Session 조회가 collection scan일 수 있다. 외부 시스템의 Mongo `_class` FQCN 직접 사용 여부, OpenAPI Validation 오류 schema 구체성, UserFactory의 Clock·Spring 결합은 후속 확인이 필요하다. 실제 MongoDB가 없어 로그인·회원가입의 영속 E2E와 운영 index 실행계획은 실행하지 않았다.
- 다음 작업: 사용자가 `git diff --cached --stat`, `git diff --cached --find-renames --summary`, `git status`를 확인한 뒤 현재 staged 변경을 직접 commit·push한다. 운영 Mongo에서 `refresh_sessions` 실행계획과 `_class` 값을 확인하고, Transaction·동시 재발급 통합 테스트·OpenAPI 오류 wrapper·UserFactory Clock 개선은 별도 승인된 작업으로 진행한다. Jira 댓글이나 상태 변경은 별도 요청과 명시적 승인 전까지 수행하지 않는다.

## 2026-07-30 — 핵심 인증·보안 코드 한 줄 주석 보강

<!-- codex-turn:019fb1d6-9e99-7461-89e0-70880a3cb177 -->

- 날짜: 2026-07-30
- 브랜치: `main`
- Jira: TMI-9
- 작업 목표: main에 병합된 Identity 리팩토링 코드에서 인증·세션·보안 흐름의 비자명한 의도를 짧은 한국어 `//` 한 줄 주석으로 설명하고 기능 계약이 그대로 유지되는지 재검증한다.
- 변경 파일: `domain/auth/application`의 `LoginService`, `LogoutService`, `LogoutAllService`, `SignupService`, `TokenReissueService`, `RefreshSessionIssuer`, `domain/auth/domain/entity/RefreshSession`, `domain/user/domain/UserFactory`, `global/config`의 `SecurityConfig`, `OpenApiConfig`, `global/security/jwt`의 `JwtConfiguration`, `JwtAccessTokenIssuer`, `JwksController`, `RsaKeyLoader`, `global/security/refresh`의 `RefreshTokenGenerator`, `RefreshTokenHasher`, `global/security/currentuser/JwtCurrentUserProvider`, `global/exception/GlobalExceptionHandler`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 변경했다.
- 구현 내용: 로그인 검증 이후 토큰 발급, Refresh Token 재사용 탐지와 낙관적 잠금, 로그아웃 멱등성, 사용자 소유 세션 범위, 동시 회원가입 고유 인덱스, 토큰 원문 비저장, 세션 폐기 상태 일관성, 비밀번호 해시, 공개·보호 경로, Operation 단위 OpenAPI 보안, JWT 검증·subject·scope, Public JWK 전용 공개, RSA ResourceLoader 주입 모호성 회피와 Private Key 바이트 정리, Refresh Token 난수·해시, JWT 사용자 식별과 validation 민감값 제거 의도를 각 한 줄로 설명했다. DTO 필드, getter와 단순 대입에는 주석을 추가하지 않았고 실행 코드는 변경하지 않았다.
- 실행한 테스트와 결과: 최초 `./gradlew clean test`는 샌드박스가 사용자 Gradle 캐시 잠금 파일 접근을 막아 테스트 실행 전에 종료됐다. 승인된 Gradle 접근 범위에서 같은 명령을 다시 실행해 전체 146개 테스트가 성공했고 실패·오류·건너뜀은 모두 0개였다. `./gradlew build`와 `git diff --check`도 성공했다.
- 유지한 계약: API URL·HTTP Method·요청/응답 JSON·오류 상태, JWT RS256·`kid`·issuer·audience·UUID `sub`·TTL, Refresh Token 해시·회전·폐기, Mongo collection·field·index와 환경변수를 변경하지 않았다. Secret, 실제 Token 값, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 코드·주석·기록에 추가하지 않았다.
- 결정사항: 코드만으로 목적을 놓치기 쉬운 인증과 보안 경계에만 총 25개의 짧은 한 줄 주석을 추가하고 JavaDoc이나 장문의 설명, 이모지, 자명한 줄별 주석은 사용하지 않았다. Jira `TMI-9`의 설명·완료 조건·완료 상태는 Atlassian 공식 MCP로 읽기 전용 확인했으며 댓글·상태·필드와 다른 이슈는 변경하지 않았다. commit·push·stage도 수행하지 않았다.
- 위험 요소: 실행 동작의 위험은 새로 추가되지 않았지만 향후 코드 변경 시 주석도 함께 갱신하지 않으면 설명과 구현이 어긋날 수 있다. 기존 RefreshSession 다중 쓰기의 비원자성, 운영 조회 index와 Mongo `_class`, OpenAPI Validation 오류 schema, UserFactory의 시스템 시간 결합은 이번 주석 작업 범위 밖의 기존 후속 항목으로 남는다.
- 다음 작업: 사용자가 unstaged diff를 검토한 뒤 필요하면 직접 commit·push하고, 구현 변경 시 관련 주석의 정확성도 함께 확인한다. Jira 댓글 초안은 최종 보고에만 제시하며 별도 승인 전에는 등록하거나 상태를 변경하지 않는다.

## 2026-08-05 — 음성 데이터 동의 저장 구조 확인

<!-- codex-turn:019fd09c-20d1-7021-ba46-89473c4bcc38 -->

- 날짜: 2026-08-05
- 브랜치: `main`
- 작업 목표: 현재 LOCAL 회원가입과 Guest 생성에서 음성 데이터 수집·이용 동의가 검증되고 MongoDB에 저장되며 외부 응답으로 노출되는 구조를 코드 변경 없이 확인해 설명한다.
- 변경 파일: 애플리케이션 코드는 수정하지 않았고 저장소 작업 기록을 위해 `docs/codex/CURRENT_STATE.md`와 `docs/codex/WORKLOG.md`만 변경했다. 기존 WORKLOG 항목은 수정하거나 삭제하지 않았다.
- 구현 내용: `AudioConsent`가 별도 컬렉션이 아니라 `users` 문서의 embedded 객체로 저장되며 `agreed`, `policyVersion`, `agreedAt`, `withdrawnAt`을 보유함을 확인했다. LOCAL과 Guest 생성 모두 동의가 정확히 true일 때 같은 Factory 경로로 `agreed=true`, 설정된 정책 버전, 사용자 생성 시각과 null 철회 시각을 구성한다. 외부 회원가입·프로필 응답은 현재 동의 여부만 반환하고 내부 정책 버전과 시각은 노출하지 않는다.
- 실행한 테스트와 결과: 코드 변경이 없는 소스 분석 작업이므로 `./gradlew clean test`는 실행하지 않았다. 관련 Entity, Factory, LOCAL·Guest application service, Request·Response DTO와 기존 테스트 assertion을 정적으로 확인했다.
- 유지한 계약: 동의 누락의 validation 400과 false의 `AUDIO_CONSENT_REQUIRED` 400, User 문서 저장 구조, Guest 생성 Transaction, 기존 인증·토큰 계약을 변경하지 않았다. Secret, 실제 Token, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: 현재 구현은 최신 상태 하나를 User 문서에 저장하는 모델이며 정책 버전별 append-only 감사 이력 모델로 설명하지 않는다. `withdrawnAt`은 모델에 준비돼 있지만 현재 철회 API나 상태 변경 경로는 없다.
- 위험 요소: 정책 버전 갱신에 따른 재동의, 동의 철회, 변경 이력 감사가 필요해지면 별도 이력 모델과 이행 정책이 필요하다. 기존 User 문서에 `audioConsent`가 없다면 프로필 조회 등에서 null 처리 또는 데이터 이행이 필요할 수 있다.
- 다음 작업: 제품 요구가 확정되면 동의 철회 API, 정책 버전별 재동의와 append-only 이력 보존을 별도 범위로 설계한다.

## 2026-08-05 — Guest 최초 인증 요청 의미 확인

<!-- codex-turn:019fd0aa-ee8d-7ae2-9c95-abb16e593a4a -->

- 날짜: 2026-08-05
- 브랜치: `main`
- 작업 목표: Guest 인증 요청의 `installationId`와 `isAudioConsent` 검증 역할 및 최초 생성 이후 재인증 흐름을 코드 변경 없이 확인해 설명한다.
- 변경 파일: 애플리케이션 코드는 수정하지 않았고 `docs/codex/CURRENT_STATE.md`와 `docs/codex/WORKLOG.md`만 갱신했다. 기존 WORKLOG 항목은 수정하거나 삭제하지 않았다.
- 구현 내용: `POST /api/v1/auth/guest`가 필수 UUID v4 설치 식별자와 필수 음성 동의 값을 받고, 동의가 정확히 true인 신규 설치에만 Guest User와 Token을 생성함을 확인했다. 설치 식별자는 중복 방지 용도이며 인증 자격 증명이 아니므로 동일 값이 이미 존재하면 Token을 재발급하지 않고 `GUEST_ALREADY_EXISTS` 409를 반환한다. 정상 발급 이후에는 보관한 Refresh Token으로 기존 `/api/v1/auth/reissue`를 사용한다.
- 실행한 테스트와 결과: 코드 변경이 없는 확인 작업이므로 `./gradlew clean test`는 실행하지 않았다. Controller, Request validation과 Guest application service 흐름을 정적으로 확인했다.
- 유지한 계약: UUID v4 검증, 음성 동의 필수 정책, 설치 식별자 해시 저장, 동일 설치 409 정책, 기존 Token 재발급 흐름을 변경하지 않았다. Secret, 실제 Token, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: 사용자 표현의 “Guest 로그인”은 최초 호출에서는 Guest 생성과 최초 Token 발급을 의미하며, 이미 생성된 Guest의 인증 복구를 설치 식별자로 수행하는 API는 아니다.
- 위험 요소: 최초 생성 commit 후 응답이 유실되거나 클라이언트가 Refresh Token을 분실하면 설치 식별자만으로 기존 Guest를 복구할 수 없다.
- 다음 작업: 앱은 최초 성공 응답의 Refresh Token을 안전하게 저장하고 이후 실행부터 `/api/v1/auth/reissue`를 호출해야 한다.

## 2026-08-05 — 개인정보 처리방침 및 이용약관 동의 계약 전환

<!-- codex-turn:019fd0aa-ee8d-7ae2-9c95-abb16e593a4a -->

- 날짜: 2026-08-05
- 브랜치: `main`
- 작업 목표: Guest와 LOCAL 회원가입의 기존 음성 데이터 동의 계약을 개인정보 처리방침·이용약관 동의로 교체하고, 서버 정책 버전 검증·서버 동의 시각 저장·기존 사용자 갱신 API와 프로필 조회 계약을 구현한다.
- 변경 파일: `domain.auth`의 Guest·회원가입 Controller, Service, Request/Response와 인증 오류, `domain.user`의 `User`, `UserFactory`, 신규 `UserConsents`·`ConsentPolicy`·`UserConsentService`·동의 Request/Response, User Controller·프로필 DTO·사용자 오류를 변경했다. 기존 `AudioConsent`를 제거하고 관련 API·application·domain·Security·OpenAPI 테스트를 수정·추가했다. `application.yml`, 테스트 설정, `.env.example`, `README.md`, `AGENTS.md`, `docs/codex/CURRENT_STATE.md`와 이 WORKLOG 항목을 갱신했으며 과거 WORKLOG 항목은 수정하거나 삭제하지 않았다.
- 구현 내용: `POST /api/v1/auth/guest`와 `POST /api/v1/auth/signup`이 개인정보·약관 동의 여부 true와 서버 현재 버전을 각각 검증하도록 변경했다. 두 상태·버전·서버 `Instant`를 `users.consents` embedded 객체 한 건으로 저장한다. `PUT /api/v1/users/me/consents`는 JWT `sub` 사용자를 조회해 두 동의를 한 User 저장으로 갱신하며 동일 버전 재요청에서는 저장과 기존 시각 변경을 생략한다. `GET /api/v1/users/me`와 회원가입 응답은 두 동의 상태·버전·시각을 반환한다. `isAudioConsent`, `AudioConsent`, `AUDIO_CONSENT_REQUIRED`와 실행 설정의 기존 음성 정책 변수를 제거했다.
- 실행한 테스트와 결과: 핵심 도메인·Guest·회원가입·프로필·동의 API·Security·OpenAPI 대상 테스트를 먼저 실행해 성공했다. 최종 `./gradlew clean test`는 32개 test suite의 228개 테스트가 성공했고 실패·오류·건너뜀은 모두 0개였다. `git diff --check`, main 소스와 실행 설정의 구 음성 동의 심볼 제거, 새 정책 환경변수 연결, 동의 갱신 경로가 공개 목록에 없고 `anyRequest().authenticated()`에 의해 보호됨을 확인했다.
- 유지한 계약: 동일 설치 ID의 `GUEST_ALREADY_EXISTS`, Guest UUID와 설치 ID 해시 정책, RS256·issuer·audience·scope·JWT `sub`, Opaque Refresh Token·Rotation·재사용 탐지·로그아웃, Learning Core와 AI 계약을 변경하지 않았다. Secret, 실제 Token, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 코드·문서·로그에 기록하지 않았다.
- 결정사항: MongoDB 스키마리스 특성을 사용해 파괴적 migration이나 새 index는 추가하지 않는다. 새 `consents` 필드가 없는 문서는 개인정보·약관 모두 false와 null 버전·시각으로 읽고 과거 `audioConsent`를 새 동의로 자동 변환하지 않는다. 저장소에 ECS Task Definition 파일이 없어 임의 파일을 만들지 않고 README에 새 revision에서 두 환경변수를 추가하고 기존 변수를 제거하는 절차를 기록했다. 구 요청은 호환 필드로 유지하지 않고 새 계약으로 전환한다.
- 위험 요소: 구 앱과 새 백엔드, 새 앱과 구 백엔드는 각각 필수 요청 필드가 달라 호환되지 않으므로 프론트와 ECS revision을 같은 전환 창에 배포하거나 강제 업데이트 정책이 필요하다. 기존 사용자는 새 정책에 미동의로 보이므로 프로필 결과에 따른 재동의 UX가 필요하다. 현재는 최신 동의 상태만 저장하므로 정책 버전별 append-only 감사 이력과 철회 이력은 남지 않는다. 격리 테스트는 실제 운영 MongoDB를 호출하지 않았다.
- 다음 작업: staging ECS Task Definition에 현재 개인정보·약관 버전을 명시하고 기존 음성 정책 변수를 제거한 뒤, Guest·LOCAL 생성, 기존 사용자 재동의, 프로필 응답과 기존 Token 수명 주기를 실제 배포 topology에서 검증한다.

## 2026-08-05 — 개인정보·약관 동의 계약 작업 기록 보정

<!-- codex-turn:019fd0ac-9476-72d2-93e3-d0361a5074db -->

- 날짜: 2026-08-05
- 브랜치: `main`
- 작업 목표: 개인정보 처리방침·이용약관 동의 계약 구현 작업의 현재 turn 감사 marker를 append-only 규칙에 맞게 보완한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`
- 구현 내용: 앞선 구현 기록과 소스는 변경하지 않고 현재 turn marker를 포함한 보정 기록을 파일 끝에 추가했다.
- 실행한 테스트와 결과: 앞선 최종 `./gradlew clean test`에서 228개 테스트가 모두 성공했고 실패·오류·건너뜀은 0개였다. 이번 보정은 문서만 변경했으며 `git diff --check`로 확인한다.
- 유지한 계약: 개인정보·약관 동의 API, 기존 인증·JWT·Refresh Token·Guest 중복 정책을 변경하지 않았다. Secret, 실제 Token, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: 과거 WORKLOG 항목은 수정하거나 삭제하지 않고 누락된 현재 turn marker를 새 항목으로만 보완한다.
- 위험 요소: 코드 위험은 추가되지 않았다. 실제 MongoDB 및 ECS 배포 검증은 앞선 기록의 운영 후속 항목으로 유지한다.
- 다음 작업: 사용자가 변경 사항을 검토한 뒤 staging 환경변수와 프론트 요청 전환 순서를 확정한다.

## 2026-08-05 — malformed JSON 테스트 문자열 파서 호환 수정

<!-- codex-turn:019fd0be-d651-7260-b3aa-63f800198205 -->

- 날짜: 2026-08-05
- 브랜치: `main`
- 작업 목표: `SecurityIntegrationTests`의 의도적으로 닫히지 않은 JSON 조립 표현에서 IDE가 표시한 Java 구문 오류를 테스트 의미 변경 없이 해소한다.
- 변경 파일: `src/test/java/web/tosunsaeng/identity/global/config/SecurityIntegrationTests.java`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`
- 구현 내용: 민감 입력값을 포함한 malformed JSON을 별도 지역 변수에서 `String.formatted`로 구성하고 MockMvc의 `content`에는 해당 변수만 전달하도록 분리했다. 닫는 중괄호가 없는 요청을 보내는 기존 보안 테스트 의도는 유지했다.
- 실행한 테스트와 결과: 대상 `SecurityIntegrationTests`가 성공했고, 최종 `./gradlew clean test`에서 32개 test suite의 228개 테스트가 성공했으며 실패·오류·건너뜀은 모두 0개였다. `git diff --check`도 통과했다.
- 유지한 계약: malformed JSON의 공통 400 오류 응답과 요청 민감값·내부 예외 비노출 검증, 기존 인증·동의·JWT·Refresh Token 계약을 변경하지 않았다. Secret, 실제 인증 정보, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: 테스트 입력은 정상 JSON으로 바꾸지 않고 파서가 명확하게 해석할 수 있도록 문자열 생성과 MockMvc 체인을 분리하는 최소 수정만 적용했다. 기존 작업 트리의 다른 변경은 수정하거나 제거하지 않았다.
- 위험 요소: Gradle 컴파일에서는 기존 표현도 유효했으므로 IDE에 오류 표시가 남으면 프로젝트 동기화 또는 인덱스 갱신이 필요할 수 있다.
- 다음 작업: IDE에서 Gradle 프로젝트를 다시 동기화한 뒤 해당 파일의 오류 표시가 사라졌는지 확인하고, 사용자가 현재 미커밋 변경을 검토한다.

## 2026-08-05 — 로그인 사용자 동의 상태 조회 API

<!-- codex-turn:019fd0cd-6c23-7b52-b4d5-fac37154a935 -->

- 날짜: 2026-08-05
- 브랜치: `main`
- 작업 목표: 프론트가 인증된 사용자의 저장 동의 버전과 서버의 현재 필수 개인정보 처리방침·이용약관 버전을 비교해 재동의 필요 여부를 판단할 수 있는 조회 API를 추가한다.
- 변경 파일: `UserController`, `UserConsentService`, 신규 `UserConsentStatusResponse`·`ConsentPolicyStatusResponse`, `application.yml`, `README.md`, `IdentityApplicationTests`, `UserControllerTests`, `UserConsentServiceTests`, `SecurityIntegrationTests`, 신규 `ConsentPolicyTests`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 변경했다. `.env.example`의 기존 두 정책 버전 예시는 이미 새 필수 설정명과 일치해 그대로 유지했다.
- 구현 내용: 인증이 필요한 `GET /api/v1/users/me/consents`를 추가해 검증된 JWT `sub`의 ACTIVE 사용자만 조회한다. 개인정보 처리방침과 이용약관별로 서버 `currentVersion`, 저장된 `consented`·`consentedVersion`·`consentedAt`, 정확한 문자열 일치 기반 `requiresConsent`를 중첩 응답으로 반환한다. 저장 동의가 false이거나 버전이 null·공백이거나 현재 버전과 다르면 재동의가 필요하다. 기존 PUT 갱신과 동일한 사용자 조회·오류 경로를 재사용한다.
- 실행한 테스트와 결과: 서비스·Controller·Security·OpenAPI·설정 fail-fast 대상 테스트 66개를 실행해 성공했다. 최종 `./gradlew clean test`는 33개 test suite의 249개 테스트가 성공했고 실패·오류·건너뜀은 모두 0개였다. `git diff --check`도 통과했다.
- 유지한 계약: 기존 Guest·LOCAL 생성, 로그인·재발급·단일·전체 로그아웃, 프로필, JWT issuer·audience·scope·`sub`, RS256, Opaque Refresh Token과 Rotation 계약을 변경하지 않았다. GET 동의 조회 경로는 공개 목록에 추가하지 않았고 요청에서 사용자나 설치 식별자를 받지 않는다. 실제 자격증명, 실제 Key, 전체 MongoDB URI와 개인정보를 코드·문서·기록에 추가하지 않았다.
- 결정사항: 기존 User 문서에 `consents`가 없으면 `User.getConsents()`의 기존 fallback을 재사용해 두 정책을 미동의, 버전·시각 null, 재동의 필요로 반환하고 과거 음성 동의를 자동 변환하지 않는다. 정책 버전 환경변수의 애플리케이션 기본값을 제거해 누락 시 placeholder 해석에서, 공백 시 `ConsentPolicy` 검증에서 기동 실패하도록 했다. MongoDB 저장 구조와 index는 변경하지 않았다.
- 위험 요소: 정책 버전 변경 전에 ECS Task Definition과 프론트에 같은 버전을 배포해야 하며 불일치 시 신규 가입·Guest 생성·동의 갱신 요청이 400으로 거절될 수 있다. 격리 테스트는 실제 운영 MongoDB나 배포 환경을 호출하지 않았다.
- 다음 작업: ECS Task Definition에 두 필수 정책 버전을 비공백 값으로 설정하고 staging에서 로그인·Guest 인증 후 GET 조회, 재동의 화면, PUT 갱신, 재조회 흐름을 검증한다. 사용자가 미커밋 diff를 검토한 뒤 commit과 push를 직접 수행한다.

## 2026-08-07 — 단건 로그아웃 처리 흐름 확인

<!-- codex-turn:019fd9ec-9e6a-7972-9f24-081c1335def6 -->

- 날짜: 2026-08-07
- 브랜치: `main`
- 작업 목표: 현재 단건 로그아웃 API가 요청을 인증하고 RefreshSession을 폐기하며 반복·만료·오류 상황을 처리하는 방식을 코드와 테스트 근거로 확인해 설명한다.
- 변경 파일: 애플리케이션 코드와 테스트는 수정하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 기존 `.github/workflows/deploy-staging.yml`의 사용자 변경은 건드리지 않았다.
- 구현 내용: `POST /api/v1/auth/logout`은 공개 POST 경로이며 요청 본문의 Opaque Refresh Token을 SHA-256 기반 해시로 변환해 `RefreshSession` 한 건을 조회한다. 활성·미만료 세션이면 공용 Clock의 현재 시각을 `lastUsedAt`과 `revokedAt`에 기록하고 사유를 `LOGOUT`으로 저장한다. 세션이 없거나 이미 폐기됐거나 만료됐으면 저장 없이 성공하며 응답은 공통 성공 구조의 null 결과다. 로그아웃된 세션의 재발급은 일반 유효하지 않은 Refresh Token 오류가 되고 Access Token은 blacklist가 없어 자체 만료까지 남을 수 있다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석 작업이므로 새 테스트나 `./gradlew clean test`는 실행하지 않았다. `LogoutService`, `AuthController`, `LogoutRequest`, `RefreshSession`, `SecurityConfig`, 재발급 서비스와 기존 Service·Controller·Guest 수명 주기 테스트를 정적으로 확인했다.
- 유지한 계약: 단건 로그아웃의 공개 경로, Refresh Token 원문 비저장, 해시 조회, 멱등 성공, `LOGOUT` 사유, 공통 응답, Guest·LOCAL 공통 세션 흐름과 기존 전체 로그아웃 계약을 변경하지 않았다. 실제 인증 정보, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: 단건 로그아웃은 사용자 전체가 아니라 제출한 Refresh Token의 세션 한 건만 대상으로 하며, Access Token의 즉시 폐기 기능으로 설명하지 않는다. 클라이언트는 성공 응답을 받으면 로컬 Access Token과 Refresh Token을 모두 삭제해야 한다.
- 위험 요소: 같은 활성 세션에 대한 정확한 동시 로그아웃에서는 두 요청이 모두 폐기 전 상태를 읽은 뒤 `@Version` 충돌이 발생할 수 있고, 현재 서비스는 이를 멱등 성공으로 변환하지 않아 한 요청이 일반 500이 될 가능성이 있다. Repository 조회·저장 장애도 숨기지 않고 일반 서버 오류로 전파된다.
- 다음 작업: 제품이 동시 단건 로그아웃까지 강한 멱등성을 요구하면 `OptimisticLockingFailureException`을 안전한 성공으로 변환할지, 저장 후 세션 상태를 재확인할지 별도 구현과 동시성 테스트로 확정한다.

## 2026-08-07 — 회원 탈퇴 구현 상태 확인

<!-- codex-turn:019fd9ec-9e6a-7972-9f24-081c1335def6 -->

- 날짜: 2026-08-07
- 브랜치: `main`
- 작업 목표: 현재 Identity에 회원 탈퇴 요청, 상태 전이, Session 폐기와 사용자 정보 정리 흐름이 구현돼 있는지 확인한다.
- 변경 파일: 애플리케이션 코드와 테스트는 수정하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 기존 `.github/workflows/deploy-staging.yml`의 사용자 변경은 건드리지 않았다.
- 구현 내용: `UserStatus.WITHDRAWN` enum과 비활성 계정을 로그인·재발급·프로필·동의 기능에서 거절하는 방어는 존재하지만, ACTIVE 사용자를 WITHDRAWN으로 전환하는 도메인 메서드·application service·Controller endpoint와 사용자 삭제·익명화 구현은 없음을 확인했다. 탈퇴 시 전체 RefreshSession 폐기, 동의 기록 처리와 관련 서비스 연동도 구현돼 있지 않다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석 작업이므로 새 테스트나 `./gradlew clean test`는 실행하지 않았다. User 상태 모델·Entity, Auth/User Controller, 로그인·재발급·프로필·동의 서비스와 관련 테스트를 정적으로 확인했다.
- 유지한 계약: 기존 로그인·Guest·재발급·로그아웃·프로필·동의와 JWT 계약을 변경하지 않았다. 실제 인증 정보, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: 현재 `WITHDRAWN`은 이미 그 상태인 문서를 차단할 수 있는 모델 값일 뿐, 사용자에게 제공되는 회원 탈퇴 기능으로 간주하지 않는다.
- 위험 요소: 운영 DB를 수동으로 WITHDRAWN으로 바꿔도 기존 Access Token은 자체 만료까지 다른 서비스에서 유효할 수 있고, RefreshSession 전체 폐기와 개인정보 보존·삭제 정책이 자동 적용되지 않는다.
- 다음 작업: 회원 탈퇴를 구현하려면 Bearer 인증 endpoint, 재인증 요구 여부, WITHDRAWN 전이, 전체 Session 폐기, Access Token 잔여 수명, LOCAL·Guest 데이터 익명화·보존 기간과 Learning Core 데이터 처리 정책을 먼저 확정한다.

## 2026-08-07 — 회원 탈퇴 구현 상태 확인 감사 기록

<!-- codex-turn:019fd9f0-2cbf-7391-845b-408b37e3f030 -->

- 날짜: 2026-08-07
- 브랜치: `main`
- 작업 목표: 회원 탈퇴 기능의 현재 구현 여부를 확인하고 이번 turn의 감사 기록을 올바른 marker로 남긴다.
- 변경 파일: 애플리케이션 코드와 테스트는 수정하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 기존 `.github/workflows/deploy-staging.yml` 변경은 건드리지 않았다.
- 구현 내용: `WITHDRAWN` 상태 모델과 비활성 사용자 차단은 있으나 회원 탈퇴 endpoint, ACTIVE 상태 전이, 전체 Session 폐기, 사용자 정보 삭제·익명화와 관련 서비스 연동은 구현되지 않았음을 확인했다.
- 실행한 테스트와 결과: 코드 변경이 없는 정적 확인 작업이므로 테스트를 새로 실행하지 않았다.
- 유지한 계약: 기존 인증·로그아웃·프로필·동의·JWT 계약을 변경하지 않았다. 실제 인증 정보, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: `WITHDRAWN` enum의 존재만으로 회원 탈퇴 기능이 구현된 것으로 보지 않는다.
- 위험 요소: 수동 상태 변경만으로는 기존 Access Token의 잔여 수명과 전체 Session, 개인정보 처리 정책이 해결되지 않는다.
- 다음 작업: 회원 탈퇴 요구사항을 별도 범위로 확정한 뒤 상태 전이·Session 폐기·데이터 보존 및 익명화 정책과 테스트를 함께 구현한다.

## 2026-08-07 — 회원 탈퇴 API 설계 초안

- 날짜: 2026-08-07
- 브랜치: `main`
- 작업 목표: 구현에 착수하지 않고 현재 Identity·JWT·MongoDB·Learning Core 경계를 유지하는 회원 탈퇴 API 계약과 기능·원자성·데이터 처리 계획을 설계한다.
- 변경 파일: 애플리케이션 코드와 테스트는 수정하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 기존 `.github/workflows/deploy-staging.yml` 변경과 과거 WORKLOG 항목은 건드리지 않았다.
- 구현 내용: 보호된 사용자 탈퇴 endpoint가 JWT `sub`, 현재 RefreshSession 소유권과 provider별 재인증을 확인한 뒤 Mongo Transaction에서 User를 익명화된 `WITHDRAWN` tombstone으로 전환하고 모든 RefreshSession을 `ACCOUNT_WITHDRAWN` 사유로 폐기하는 초안을 마련했다. 동일 요청은 기존 탈퇴 시각을 반환하는 멱등 성공으로 설계하고, Learning Core 소유 데이터는 userId와 발생 시각만 담은 transaction outbox 이벤트로 비동기·재시도 가능하게 연동하는 방향을 제안했다.
- 실행한 테스트와 결과: 설계·분석 작업이므로 새 테스트와 `./gradlew clean test`는 실행하지 않았다. User·RefreshSession 모델, MongoTransactionManager, 기존 Guest transaction, Security/JWT 계약과 Identity–Learning Core 계약을 정적으로 확인했다.
- 유지한 계약: JWT `sub` 사용자 식별, RS256·issuer·audience·scope, JWKS 오프라인 검증, Refresh Token 원문 비저장, Identity와 Learning Core의 도메인 소유권, AI `user_id=examId` 계약을 변경하지 않았다. 실제 인증 정보, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: 기본 권장안은 물리 삭제보다 UUID·상태·최소 감사 메타데이터만 남기는 tombstone과 즉시 자격증명 익명화이며, Access Token denylist를 이번 API에 직접 도입하지 않고 기존 최대 TTL 동안의 잔여 유효성을 명시한다. LOCAL 비밀번호 재확인, Guest RefreshSession 증명, 복구 유예와 데이터 보존 기간은 구현 전 확정 항목으로 둔다.
- 위험 요소: User에 낙관적 잠금이 없어 탈퇴와 동의 갱신이 경합하면 stale save 위험이 있으므로 조건부 update 또는 version migration이 필요하다. Learning Core는 JWT를 독립 검증하므로 이미 발급된 Access Token의 즉시 차단은 별도 cross-service revocation 설계 없이는 보장할 수 없다. outbox 인프라가 없으므로 전체 데이터 삭제 완료를 동기 응답으로 보장할 수도 없다.
- 다음 작업: 제품·법무 결정을 확정한 뒤 API DTO·오류 코드, User 상태 전이·익명화, `ACCOUNT_WITHDRAWN` Session 폐기, transaction/outbox, 동시성·회귀 테스트와 운영 migration을 하나의 승인된 구현 이슈로 작성한다.

## 2026-08-07 — 회원 탈퇴 API 설계 감사 기록

<!-- codex-turn:019fd9f3-390b-76e2-a876-ee4bbd97b674 -->

- 날짜: 2026-08-07
- 브랜치: `main`
- 작업 목표: 회원 탈퇴 API 설계 결과를 현재 turn marker와 함께 append-only 감사 기록으로 남긴다.
- 변경 파일: 애플리케이션 코드와 테스트는 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 기존 workflow 변경은 건드리지 않았다.
- 구현 내용: JWT `sub`와 현재 RefreshSession 소유권, LOCAL 재인증, Guest 세션 증명, 익명화된 `WITHDRAWN` tombstone, 전체 Session의 `ACCOUNT_WITHDRAWN` 폐기, Mongo Transaction과 Learning Core outbox 연동을 권장 설계로 정리했다. 실제 endpoint·도메인 메서드·저장 로직은 구현하지 않았다.
- 실행한 테스트와 결과: 설계 문서화만 수행했으므로 테스트를 실행하지 않았다.
- 유지한 계약: 기존 인증·JWT·Refresh Token·로그아웃과 Identity–Learning Core 도메인 경계를 변경하지 않았다. 실제 인증 정보, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: 물리 삭제보다 UUID와 최소 감사 메타데이터를 유지하는 익명화 tombstone을 기본안으로 두고, 이미 발급된 Access Token의 즉시 무효화는 별도 cross-service 설계로 분리한다.
- 위험 요소: 동시 User 갱신 방어, 개인정보·동의 기록 보존 기간, 재가입, 복구 유예, Learning Core 데이터 삭제와 Access Token 잔여 유효성은 구현 전 확정이 필요하다.
- 다음 작업: 제품·법무 결정과 서비스 간 삭제 계약을 확정한 뒤 별도 승인된 구현 이슈로 진행한다.

## 2026-08-07 — Guest Session 및 탈퇴 후 재가입 동작 확인

- 날짜: 2026-08-07
- 브랜치: `main`
- 작업 목표: Guest가 실제 RefreshSession을 보유하는지와 탈퇴 후 동일 installationId 요청이 기존 계정 복구 또는 신규 Guest 생성으로 처리되는지 현재 구현과 권장 설계를 구분해 확인한다.
- 변경 파일: 애플리케이션 코드와 테스트는 수정하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 기존 `.github/workflows/deploy-staging.yml` 변경은 건드리지 않았다.
- 구현 내용: `GuestAuthService`가 Guest UUID로 Access Token과 RefreshSession을 준비하고 `GuestRegistrationTransactionService`가 User와 초기 RefreshSession을 Mongo Transaction 안에서 저장하므로 Guest도 실제 Session 문서를 가진다. 현재 회원 탈퇴 전이는 구현되지 않았으며, User 상태만 `WITHDRAWN`으로 바꾸고 설치 ID 해시를 유지하면 동일 installationId 재요청은 상태와 무관한 존재 확인에 걸려 `GUEST_ALREADY_EXISTS` 409가 된다.
- 실행한 테스트와 결과: 코드 변경이 없는 정적 분석 작업이므로 새 테스트와 전체 테스트는 실행하지 않았다. Guest 인증·Transaction·RefreshSession 발급 및 설치 ID 중복 판정 코드를 확인했다.
- 유지한 계약: installationId를 인증 증명이나 기존 계정 복구 키로 사용하지 않는 정책, Guest·LOCAL 공통 Refresh Token Session과 기존 중복 Guest 409 계약을 변경하지 않았다. 실제 인증 정보, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: 탈퇴 기능 구현 시 기존 `WITHDRAWN` Guest를 복구하지 않고, 탈퇴 Transaction에서 기존 Session을 폐기하고 `guestInstallationIdHash` 점유를 안전하게 해제한 뒤 동일 설치의 다음 Guest 인증은 새 UUID와 새 RefreshSession을 만드는 정책을 권장한다. 이는 아직 구현된 동작이 아니다.
- 위험 요소: 현 상태에서 운영 DB의 Guest를 수동으로 `WITHDRAWN` 처리하면 설치 ID 해시가 계속 점유되어 해당 설치가 409 상태에 머물 수 있다.
- 다음 작업: 회원 탈퇴 구현 범위에서 설치 ID 해시 해제, 기존 Session 전체 폐기, 신규 Guest UUID 생성 및 동시성·원자성 테스트를 명시적으로 포함한다.

## 2026-08-07 — Guest Session 및 탈퇴 후 재가입 확인 감사 기록

<!-- codex-turn:019fd9ff-a418-7411-9d5e-6689720eb78f -->

- 날짜: 2026-08-07
- 브랜치: `main`
- 작업 목표: Guest의 실제 RefreshSession 보유 여부와 탈퇴 후 동일 installationId 재사용 시 현재 동작을 확인한 이번 turn의 감사 기록을 남긴다.
- 변경 파일: 애플리케이션 코드와 테스트는 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 기존 workflow 변경은 건드리지 않았다.
- 구현 내용: Guest User와 초기 RefreshSession이 Mongo Transaction에서 함께 저장되고 기존 재발급·로그아웃 흐름을 공유함을 확인했다. 현재 탈퇴 기능은 미구현이며 설치 ID 해시가 남은 `WITHDRAWN` 문서는 동일 installationId 요청을 기존 계정 복구나 신규 생성 없이 409로 막는다.
- 실행한 테스트와 결과: 코드 변경이 없는 정적 확인이므로 테스트는 실행하지 않았고 `git diff --check`를 통과했다.
- 유지한 계약: installationId를 인증 또는 계정 복구 수단으로 사용하지 않는 정책과 기존 Guest 중복 요청 409 계약을 변경하지 않았다. Secret, Token, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: 향후 탈퇴 구현에서는 기존 Guest를 복구하지 않고 Session 폐기와 설치 ID 해시 점유 해제를 원자적으로 수행한 뒤 재가입 시 새 UUID와 새 RefreshSession을 생성하는 방식을 권장한다.
- 위험 요소: 현재 상태에서 Guest 문서만 수동으로 `WITHDRAWN` 처리하면 설치 ID 해시가 계속 점유되어 해당 설치가 신규 Guest를 만들 수 없다.
- 다음 작업: 회원 탈퇴 구현 이슈에 설치 ID 해제, Session 전체 폐기, 신규 Guest 생성과 경쟁 상황 검증을 포함한다.

## 2026-08-07 — 회원 탈퇴 Jira 생성 준비

<!-- codex-turn:019fda0c-7acf-7f83-89f8-072124e9277d -->

- 날짜: 2026-08-07
- 브랜치: `main`
- 작업 목표: LOCAL·GUEST 회원 탈퇴와 tombstone, 전체 RefreshSession 폐기, Guest 재가입 정책을 구현할 Jira 이슈 생성을 준비한다.
- 변경 파일: 애플리케이션 코드는 수정하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 기존 `.github/workflows/deploy-staging.yml` 변경은 건드리지 않았다.
- 구현 내용: Atlassian 공식 MCP로 Jira 프로젝트와 기존 이슈 형식을 조회하려 했으나 OAuth refresh credential이 유효하지 않아 조회·생성이 중단됐다. 저장소 규칙에 따라 실제 생성 전에 사용자에게 제목, 유형, 우선순위와 본문 초안을 제시하고 승인을 받을 준비를 했다.
- 실행한 테스트와 결과: 코드 변경이 없으므로 테스트는 실행하지 않았다. Jira 쓰기 작업도 수행되지 않았다.
- 유지한 계약: Jira에 Secret, Token, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않는 규칙과 생성 전 사전 승인 절차를 유지했다.
- 결정사항: 제안 이슈는 Task·High 우선순위, 제목 `[Identity] LOCAL·GUEST 회원 탈퇴 및 재가입 처리`, 대상 저장소 `Too-Much-I/app-back-end-identity`로 구성하고, 기존 인증·JWT·Learning Core 계약을 유지하는 범위로 작성한다.
- 위험 요소: Atlassian 연결을 재인증하기 전에는 Jira 이슈를 생성하거나 기존 프로젝트 필드·작성 형식을 재검증할 수 없다.
- 다음 작업: 사용자가 Jira 연결을 복구하고 아래 생성 초안을 승인하면 정확히 한 개의 이슈를 생성한 뒤 생성 결과만 보고한다.

## 2026-08-07 — 회원 탈퇴 Jira 이슈 생성

<!-- codex-turn:019fda15-6233-7651-804d-b865b4d93db7 -->

- 날짜: 2026-08-07
- 브랜치: `main`
- Jira: TMI-75
- 작업 목표: LOCAL·GUEST 회원 탈퇴와 Guest 재가입 정책 구현을 추적하는 Jira 이슈를 승인된 내용으로 생성한다.
- 변경 파일: 애플리케이션 코드는 수정하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 기존 `.github/workflows/deploy-staging.yml` 변경은 건드리지 않았다.
- 구현 내용: Atlassian 공식 MCP에서 TMI-40의 작성 형식과 TMI 프로젝트 Task 생성 메타데이터를 확인한 뒤 `[Identity] LOCAL·GUEST 회원 탈퇴 및 재가입 처리` 이슈를 작업(Task), High 우선순위로 정확히 한 개 생성했다. 본문에는 JWT sub·RefreshSession 소유권 검증, Provider별 재인증, WITHDRAWN tombstone, 인증 식별자 제거, 모든 Session 폐기, Mongo Transaction, Guest 신규 UUID 재가입, 테스트·문서·운영 주의사항과 제외 범위를 포함했다.
- 실행한 테스트와 결과: 코드 변경이 없는 Jira 생성 작업이므로 테스트는 실행하지 않았다. 생성 후 TMI-75를 다시 조회해 제목, 유형, High 우선순위, 해야 할 일 상태와 본문 반영을 확인했다.
- 유지한 계약: 기존 Jira 이슈의 상태·필드·댓글을 변경하지 않았고 TMI-75에도 Secret, Token, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다. Git commit과 push를 수행하지 않았다.
- 결정사항: 이슈 유형은 프로젝트의 실제 명칭인 `작업`, 우선순위는 `High`, 초기 상태는 기본값인 `해야 할 일`로 두었다. Jira 생성은 사용자가 초안을 확인하고 명시적으로 승인한 뒤 수행했다.
- 위험 요소: stateless Access Token의 잔여 유효성, Learning Core 데이터 정리와 outbox, 운영 MongoDB Transaction·index 검증은 TMI-75 본문에 구현 주의사항 또는 후속 범위로 명시했다.
- 다음 작업: 사용자가 구현 착수를 요청하면 TMI-75를 먼저 읽고 저장소 상태와 브랜치를 확인한 뒤 코드·테스트·문서 작업을 진행한다. Jira 상태 변경이나 댓글 등록은 별도 승인 없이는 수행하지 않는다.

## 2026-08-07 — TMI-75 LOCAL·GUEST 회원 탈퇴 구현

<!-- codex-turn:019fda19-0bcc-7450-98b9-eb203bfc732c -->

- 날짜: 2026-08-07
- 브랜치: `feat/TMI-75-user-withdrawal`
- Jira: TMI-75
- 작업 목표: JWT `sub`와 현재 RefreshSession 소유권을 기반으로 LOCAL·GUEST 회원 탈퇴를 처리하고, User tombstone·전체 Session 폐기·Guest 및 LOCAL 신규 계정 재가입을 기존 MongoDB 인증 구조 안에서 구현한다.
- 변경 파일: `User`, `RefreshSession`, `RevocationReason`, `UserRepository`와 custom fragment, 신규 `UserWithdrawalService`·`UserWithdrawalTransactionService`, 탈퇴 Request·Response, `UserController`, Auth·User 오류 코드, `UserConsentService`, 관련 도메인·application·Controller·Security·OpenAPI·수명 주기 테스트, `README.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 변경했다. 기존 `.github/workflows/deploy-staging.yml` 사용자 변경은 수정하지 않았다.
- 구현 내용: 보호된 `POST /api/v1/users/withdraw`가 JWT subject 사용자만 조회하고, Refresh Token 원문을 저장하지 않은 채 기존 SHA-256 해시 방식으로 Session 존재·소유권·미폐기·미만료를 확인한다. LOCAL은 기존 `PasswordEncoder`로 현재 비밀번호를 재검증하고 GUEST는 비밀번호를 요구하지 않는다. 이미 WITHDRAWN이면 기존 시각으로 200 멱등 성공하며 ACTIVE와 SUSPENDED는 탈퇴할 수 있다.
- 구현 내용: 기존 `mongoTransactionManager`가 적용된 별도 Spring Bean 안에서 User를 `WITHDRAWN` tombstone으로 조건부 갱신하고 해당 userId의 모든 미폐기 RefreshSession을 같은 시각과 `ACCOUNT_WITHDRAWN` 사유로 폐기한다. tombstone은 userId·provider·createdAt·consents를 유지하고 nickname을 익명화하며 email·normalizedEmail·passwordHash·guestInstallationIdHash를 Mongo `$unset`한다. User update 또는 Session 저장이 실패하면 Runtime 예외로 Transaction 전체가 rollback된다.
- 구현 내용: User의 기존 `status`와 `updatedAt`을 비교하는 partial update를 사용해 stale 전체 문서 저장을 피하고, 탈퇴 충돌은 최신 상태가 WITHDRAWN이면 멱등 성공, 아니면 자격 증명을 다시 확인해 한 번 재시도한 뒤 `WITHDRAWAL_CONFLICT` 409로 처리한다. 동의 갱신도 ACTIVE+updatedAt 조건의 partial update로 바꿔 탈퇴와 경합한 오래된 요청이 WITHDRAWN을 ACTIVE로 되돌리지 못하게 했다.
- 구현 내용: Guest 탈퇴에서 installation hash의 unique 점유를 해제해 같은 installationId의 다음 Guest 인증이 기존 tombstone을 복구하지 않고 새 UUID와 새 RefreshSession을 생성하도록 했다. ACTIVE Guest의 기존 중복 409는 유지하며 LOCAL 탈퇴도 이메일 unique 점유를 해제해 같은 이메일의 신규 UUID 가입이 가능하다. 활성 Session 조회용 `{ userId: 1, revokedAt: 1 }` compound index 계약을 추가했다.
- 실행한 테스트와 결과: 최종 `./gradlew clean test`가 성공했다. 38개 test suite의 284개 테스트가 실행됐고 실패·오류·건너뜀은 모두 0개였다. Guest 탈퇴 후 같은 installationId로 새 userId·새 RefreshSession 생성, 세 개의 기존 Session 전체 `ACCOUNT_WITHDRAWN` 폐기와 모든 기존 Refresh Token 재발급 실패, LOCAL 동일 이메일 재가입, 다른 사용자 Session 격리, LOCAL·GUEST·SUSPENDED·멱등 탈퇴, User/Session 실패 rollback, Spring Transaction proxy와 지정 manager, Security 401, OpenAPI, 로그인·Guest·Rotation·로그아웃·JWT·JWKS 회귀를 검증했다.
- 유지한 계약: User 물리 삭제, Refresh Token 원문 저장·로그, Guest 비밀번호 요구, WITHDRAWN 계정 복구, Access Token denylist, OAuth 재인증, Learning Core 직접 삭제와 outbox 임의 추가를 하지 않았다. RS256·JWT `sub`·issuer·audience·scope·JWKS, Opaque Refresh Token Rotation, 기존 인증·프로필·동의 API와 AI `user_id=examId` 계약을 유지했다. Secret, 실제 Token, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: User에 `@Version`을 새로 추가하지 않고 기존 문서와 호환되는 status+updatedAt compare-and-set partial update를 선택했다. 동의 기록은 법적 보존 가능성을 고려해 tombstone 안에 유지한다. Jira TMI-75의 댓글·필드·상태는 변경하지 않았고 Git commit·push·PR도 수행하지 않았다.
- 위험 요소: 격리 test profile은 실제 MongoDB를 연결하지 않으므로 Spring Transaction proxy의 rollback 경계는 검증했지만 replica set에서의 다중 collection rollback, custom repository fragment 연결과 index 실행계획은 staging에서 확인해야 한다. 자동 index 생성이 켜져 있어도 운영에서는 새 revision 전에 `{ userId: 1, revokedAt: 1 }` index를 승인된 절차로 선생성·검증해야 한다. 탈퇴 전 Access Token은 기본 `PT30M` 또는 배포 TTL 만료 전까지 Learning Core 같은 외부 서비스에서 암호학적으로 유효할 수 있다.
- 다음 작업: staging replica set에서 실제 Transaction 실패 주입, Guest·LOCAL 재가입과 compound index를 검증한다. `UserWithdrawn` outbox와 Learning Core 데이터 삭제·익명화, 서비스 간 Access Token 즉시 폐기는 별도 이슈로 설계한다. 사용자가 diff를 검토한 뒤 commit·push·PR을 직접 수행하며 Jira 댓글·상태 변경은 별도 승인 후 진행한다.

## 2026-08-07 — TMI-75 회원 탈퇴 데이터 제거 범위 확인

<!-- codex-turn:019fda63-121d-7ec1-bc5a-803ca732239a -->

- 날짜: 2026-08-07
- 브랜치: `feat/TMI-75-user-withdrawal`
- Jira: TMI-75
- 작업 목표: 회원 탈퇴 시 즉시 제거·익명화되는 정보와 보존되거나 Identity 범위 밖에 남는 정보를 현재 구현 기준으로 확인해 설명한다.
- 변경 파일: 애플리케이션 코드는 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. WORKLOG의 과거 기록과 기존 `.github/workflows/deploy-staging.yml` 사용자 변경은 건드리지 않았다.
- 구현 내용: User 문서는 물리 삭제하지 않고 email·normalizedEmail·passwordHash·guestInstallationIdHash를 Mongo `$unset`하며 nickname을 익명 값으로 교체한다. userId·provider·status·createdAt·updatedAt·withdrawnAt과 개인정보 처리방침·이용약관 동의 기록은 tombstone에 유지한다. RefreshSession 문서도 즉시 삭제하지 않고 모든 미폐기 Session을 같은 시각의 `ACCOUNT_WITHDRAWN` 상태로 폐기하며 기존 TTL 만료 후 비동기 정리 대상이 된다.
- 실행한 테스트와 결과: 코드 변경이 없는 범위 확인 작업이므로 새 테스트나 전체 테스트를 다시 실행하지 않았다. 직전 최종 `./gradlew clean test`의 38개 suite·284개 테스트 성공 결과를 유지하며, User partial update·tombstone 도메인 로직·Session 전체 폐기 코드를 정적으로 재확인했다.
- 유지한 계약: 동의 기록은 임의 삭제하지 않고 Learning Core 시험·결과, S3·AI 등 Identity 비소유 데이터도 직접 삭제하지 않는다. Refresh Token 원문은 애초에 서버에 저장하지 않으며 Access Token denylist를 추가하지 않았다. Secret, 실제 Token, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: 현재 회원 탈퇴를 완전한 전 시스템 물리 삭제로 설명하지 않고, Identity 인증 자격과 직접 식별자를 제거한 익명 tombstone 및 Session 폐기로 설명한다. 클라이언트는 성공 즉시 보유 Access/Refresh Token을 삭제해야 한다.
- 위험 요소: 보존된 userId와 동의 기록은 법적 보존·삭제 기간 정책이 별도로 필요하다. 기존 stateless Access Token은 만료 전까지 외부 서비스에서 암호학적으로 유효할 수 있고, Learning Core 데이터 삭제·익명화는 outbox와 소비자 구현 전까지 자동 수행되지 않는다.
- 다음 작업: 법무·제품 정책으로 동의 기록과 tombstone 보존 기간을 확정하고, `UserWithdrawn` outbox 및 Learning Core 후속 정리와 서비스 간 Access Token 즉시 폐기가 필요하면 별도 이슈로 구현한다.

## 2026-08-07 — 단건 로그아웃 처리 흐름 확인

<!-- codex-turn:019fd9ec-9e6a-7972-9f24-081c1335def6 -->

- 날짜: 2026-08-07
- 브랜치: `main`
- 작업 목표: 현재 단건 로그아웃 API가 요청을 인증하고 RefreshSession을 폐기하며 반복·만료·오류 상황을 처리하는 방식을 코드와 테스트 근거로 확인해 설명한다.
- 변경 파일: 애플리케이션 코드와 테스트는 수정하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 기존 `.github/workflows/deploy-staging.yml`의 사용자 변경은 건드리지 않았다.
- 구현 내용: `POST /api/v1/auth/logout`은 공개 POST 경로이며 요청 본문의 Opaque Refresh Token을 SHA-256 기반 해시로 변환해 `RefreshSession` 한 건을 조회한다. 활성·미만료 세션이면 공용 Clock의 현재 시각을 `lastUsedAt`과 `revokedAt`에 기록하고 사유를 `LOGOUT`으로 저장한다. 세션이 없거나 이미 폐기됐거나 만료됐으면 저장 없이 성공하며 응답은 공통 성공 구조의 null 결과다. 로그아웃된 세션의 재발급은 일반 유효하지 않은 Refresh Token 오류가 되고 Access Token은 blacklist가 없어 자체 만료까지 남을 수 있다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석 작업이므로 새 테스트나 `./gradlew clean test`는 실행하지 않았다. `LogoutService`, `AuthController`, `LogoutRequest`, `RefreshSession`, `SecurityConfig`, 재발급 서비스와 기존 Service·Controller·Guest 수명 주기 테스트를 정적으로 확인했다.
- 유지한 계약: 단건 로그아웃의 공개 경로, Refresh Token 원문 비저장, 해시 조회, 멱등 성공, `LOGOUT` 사유, 공통 응답, Guest·LOCAL 공통 세션 흐름과 기존 전체 로그아웃 계약을 변경하지 않았다. 실제 인증 정보, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: 단건 로그아웃은 사용자 전체가 아니라 제출한 Refresh Token의 세션 한 건만 대상으로 하며, Access Token의 즉시 폐기 기능으로 설명하지 않는다. 클라이언트는 성공 응답을 받으면 로컬 Access Token과 Refresh Token을 모두 삭제해야 한다.
- 위험 요소: 같은 활성 세션에 대한 정확한 동시 로그아웃에서는 두 요청이 모두 폐기 전 상태를 읽은 뒤 `@Version` 충돌이 발생할 수 있고, 현재 서비스는 이를 멱등 성공으로 변환하지 않아 한 요청이 일반 500이 될 가능성이 있다. Repository 조회·저장 장애도 숨기지 않고 일반 서버 오류로 전파된다.
- 다음 작업: 제품이 동시 단건 로그아웃까지 강한 멱등성을 요구하면 `OptimisticLockingFailureException`을 안전한 성공으로 변환할지, 저장 후 세션 상태를 재확인할지 별도 구현과 동시성 테스트로 확정한다.

## 2026-08-07 — 회원 탈퇴 구현 상태 확인

<!-- codex-turn:019fd9ec-9e6a-7972-9f24-081c1335def6 -->

- 날짜: 2026-08-07
- 브랜치: `main`
- 작업 목표: 현재 Identity에 회원 탈퇴 요청, 상태 전이, Session 폐기와 사용자 정보 정리 흐름이 구현돼 있는지 확인한다.
- 변경 파일: 애플리케이션 코드와 테스트는 수정하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 기존 `.github/workflows/deploy-staging.yml`의 사용자 변경은 건드리지 않았다.
- 구현 내용: `UserStatus.WITHDRAWN` enum과 비활성 계정을 로그인·재발급·프로필·동의 기능에서 거절하는 방어는 존재하지만, ACTIVE 사용자를 WITHDRAWN으로 전환하는 도메인 메서드·application service·Controller endpoint와 사용자 삭제·익명화 구현은 없음을 확인했다. 탈퇴 시 전체 RefreshSession 폐기, 동의 기록 처리와 관련 서비스 연동도 구현돼 있지 않다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석 작업이므로 새 테스트나 `./gradlew clean test`는 실행하지 않았다. User 상태 모델·Entity, Auth/User Controller, 로그인·재발급·프로필·동의 서비스와 관련 테스트를 정적으로 확인했다.
- 유지한 계약: 기존 로그인·Guest·재발급·로그아웃·프로필·동의와 JWT 계약을 변경하지 않았다. 실제 인증 정보, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: 현재 `WITHDRAWN`은 이미 그 상태인 문서를 차단할 수 있는 모델 값일 뿐, 사용자에게 제공되는 회원 탈퇴 기능으로 간주하지 않는다.
- 위험 요소: 운영 DB를 수동으로 WITHDRAWN으로 바꿔도 기존 Access Token은 자체 만료까지 다른 서비스에서 유효할 수 있고, RefreshSession 전체 폐기와 개인정보 보존·삭제 정책이 자동 적용되지 않는다.
- 다음 작업: 회원 탈퇴를 구현하려면 Bearer 인증 endpoint, 재인증 요구 여부, WITHDRAWN 전이, 전체 Session 폐기, Access Token 잔여 수명, LOCAL·Guest 데이터 익명화·보존 기간과 Learning Core 데이터 처리 정책을 먼저 확정한다.

## 2026-08-07 — 회원 탈퇴 구현 상태 확인 감사 기록

<!-- codex-turn:019fd9f0-2cbf-7391-845b-408b37e3f030 -->

- 날짜: 2026-08-07
- 브랜치: `main`
- 작업 목표: 회원 탈퇴 기능의 현재 구현 여부를 확인하고 이번 turn의 감사 기록을 올바른 marker로 남긴다.
- 변경 파일: 애플리케이션 코드와 테스트는 수정하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 기존 `.github/workflows/deploy-staging.yml` 변경은 건드리지 않았다.
- 구현 내용: `WITHDRAWN` 상태 모델과 비활성 사용자 차단은 있으나 회원 탈퇴 endpoint, ACTIVE 상태 전이, 전체 Session 폐기, 사용자 정보 삭제·익명화와 관련 서비스 연동은 구현되지 않았음을 확인했다.
- 실행한 테스트와 결과: 코드 변경이 없는 정적 확인 작업이므로 테스트를 새로 실행하지 않았다.
- 유지한 계약: 기존 인증·로그아웃·프로필·동의·JWT 계약을 변경하지 않았다. 실제 인증 정보, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: `WITHDRAWN` enum의 존재만으로 회원 탈퇴 기능이 구현된 것으로 보지 않는다.
- 위험 요소: 수동 상태 변경만으로는 기존 Access Token의 잔여 수명과 전체 Session, 개인정보 처리 정책이 해결되지 않는다.
- 다음 작업: 회원 탈퇴 요구사항을 별도 범위로 확정한 뒤 상태 전이·Session 폐기·데이터 보존 및 익명화 정책과 테스트를 함께 구현한다.

## 2026-08-07 — 회원 탈퇴 API 설계 초안

- 날짜: 2026-08-07
- 브랜치: `main`
- 작업 목표: 구현에 착수하지 않고 현재 Identity·JWT·MongoDB·Learning Core 경계를 유지하는 회원 탈퇴 API 계약과 기능·원자성·데이터 처리 계획을 설계한다.
- 변경 파일: 애플리케이션 코드와 테스트는 수정하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 기존 `.github/workflows/deploy-staging.yml` 변경과 과거 WORKLOG 항목은 건드리지 않았다.
- 구현 내용: 보호된 사용자 탈퇴 endpoint가 JWT `sub`, 현재 RefreshSession 소유권과 provider별 재인증을 확인한 뒤 Mongo Transaction에서 User를 익명화된 `WITHDRAWN` tombstone으로 전환하고 모든 RefreshSession을 `ACCOUNT_WITHDRAWN` 사유로 폐기하는 초안을 마련했다. 동일 요청은 기존 탈퇴 시각을 반환하는 멱등 성공으로 설계하고, Learning Core 소유 데이터는 userId와 발생 시각만 담은 transaction outbox 이벤트로 비동기·재시도 가능하게 연동하는 방향을 제안했다.
- 실행한 테스트와 결과: 설계·분석 작업이므로 새 테스트와 `./gradlew clean test`는 실행하지 않았다. User·RefreshSession 모델, MongoTransactionManager, 기존 Guest transaction, Security/JWT 계약과 Identity–Learning Core 계약을 정적으로 확인했다.
- 유지한 계약: JWT `sub` 사용자 식별, RS256·issuer·audience·scope, JWKS 오프라인 검증, Refresh Token 원문 비저장, Identity와 Learning Core의 도메인 소유권, AI `user_id=examId` 계약을 변경하지 않았다. 실제 인증 정보, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: 기본 권장안은 물리 삭제보다 UUID·상태·최소 감사 메타데이터만 남기는 tombstone과 즉시 자격증명 익명화이며, Access Token denylist를 이번 API에 직접 도입하지 않고 기존 최대 TTL 동안의 잔여 유효성을 명시한다. LOCAL 비밀번호 재확인, Guest RefreshSession 증명, 복구 유예와 데이터 보존 기간은 구현 전 확정 항목으로 둔다.
- 위험 요소: User에 낙관적 잠금이 없어 탈퇴와 동의 갱신이 경합하면 stale save 위험이 있으므로 조건부 update 또는 version migration이 필요하다. Learning Core는 JWT를 독립 검증하므로 이미 발급된 Access Token의 즉시 차단은 별도 cross-service revocation 설계 없이는 보장할 수 없다. outbox 인프라가 없으므로 전체 데이터 삭제 완료를 동기 응답으로 보장할 수도 없다.
- 다음 작업: 제품·법무 결정을 확정한 뒤 API DTO·오류 코드, User 상태 전이·익명화, `ACCOUNT_WITHDRAWN` Session 폐기, transaction/outbox, 동시성·회귀 테스트와 운영 migration을 하나의 승인된 구현 이슈로 작성한다.

## 2026-08-07 — 회원 탈퇴 API 설계 감사 기록

<!-- codex-turn:019fd9f3-390b-76e2-a876-ee4bbd97b674 -->

- 날짜: 2026-08-07
- 브랜치: `main`
- 작업 목표: 회원 탈퇴 API 설계 결과를 현재 turn marker와 함께 append-only 감사 기록으로 남긴다.
- 변경 파일: 애플리케이션 코드와 테스트는 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 기존 workflow 변경은 건드리지 않았다.
- 구현 내용: JWT `sub`와 현재 RefreshSession 소유권, LOCAL 재인증, Guest 세션 증명, 익명화된 `WITHDRAWN` tombstone, 전체 Session의 `ACCOUNT_WITHDRAWN` 폐기, Mongo Transaction과 Learning Core outbox 연동을 권장 설계로 정리했다. 실제 endpoint·도메인 메서드·저장 로직은 구현하지 않았다.
- 실행한 테스트와 결과: 설계 문서화만 수행했으므로 테스트를 실행하지 않았다.
- 유지한 계약: 기존 인증·JWT·Refresh Token·로그아웃과 Identity–Learning Core 도메인 경계를 변경하지 않았다. 실제 인증 정보, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: 물리 삭제보다 UUID와 최소 감사 메타데이터를 유지하는 익명화 tombstone을 기본안으로 두고, 이미 발급된 Access Token의 즉시 무효화는 별도 cross-service 설계로 분리한다.
- 위험 요소: 동시 User 갱신 방어, 개인정보·동의 기록 보존 기간, 재가입, 복구 유예, Learning Core 데이터 삭제와 Access Token 잔여 유효성은 구현 전 확정이 필요하다.
- 다음 작업: 제품·법무 결정과 서비스 간 삭제 계약을 확정한 뒤 별도 승인된 구현 이슈로 진행한다.

## 2026-08-07 — Guest Session 및 탈퇴 후 재가입 동작 확인

- 날짜: 2026-08-07
- 브랜치: `main`
- 작업 목표: Guest가 실제 RefreshSession을 보유하는지와 탈퇴 후 동일 installationId 요청이 기존 계정 복구 또는 신규 Guest 생성으로 처리되는지 현재 구현과 권장 설계를 구분해 확인한다.
- 변경 파일: 애플리케이션 코드와 테스트는 수정하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 기존 `.github/workflows/deploy-staging.yml` 변경은 건드리지 않았다.
- 구현 내용: `GuestAuthService`가 Guest UUID로 Access Token과 RefreshSession을 준비하고 `GuestRegistrationTransactionService`가 User와 초기 RefreshSession을 Mongo Transaction 안에서 저장하므로 Guest도 실제 Session 문서를 가진다. 현재 회원 탈퇴 전이는 구현되지 않았으며, User 상태만 `WITHDRAWN`으로 바꾸고 설치 ID 해시를 유지하면 동일 installationId 재요청은 상태와 무관한 존재 확인에 걸려 `GUEST_ALREADY_EXISTS` 409가 된다.
- 실행한 테스트와 결과: 코드 변경이 없는 정적 분석 작업이므로 새 테스트와 전체 테스트는 실행하지 않았다. Guest 인증·Transaction·RefreshSession 발급 및 설치 ID 중복 판정 코드를 확인했다.
- 유지한 계약: installationId를 인증 증명이나 기존 계정 복구 키로 사용하지 않는 정책, Guest·LOCAL 공통 Refresh Token Session과 기존 중복 Guest 409 계약을 변경하지 않았다. 실제 인증 정보, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: 탈퇴 기능 구현 시 기존 `WITHDRAWN` Guest를 복구하지 않고, 탈퇴 Transaction에서 기존 Session을 폐기하고 `guestInstallationIdHash` 점유를 안전하게 해제한 뒤 동일 설치의 다음 Guest 인증은 새 UUID와 새 RefreshSession을 만드는 정책을 권장한다. 이는 아직 구현된 동작이 아니다.
- 위험 요소: 현 상태에서 운영 DB의 Guest를 수동으로 `WITHDRAWN` 처리하면 설치 ID 해시가 계속 점유되어 해당 설치가 409 상태에 머물 수 있다.
- 다음 작업: 회원 탈퇴 구현 범위에서 설치 ID 해시 해제, 기존 Session 전체 폐기, 신규 Guest UUID 생성 및 동시성·원자성 테스트를 명시적으로 포함한다.

## 2026-08-07 — Guest Session 및 탈퇴 후 재가입 확인 감사 기록

<!-- codex-turn:019fd9ff-a418-7411-9d5e-6689720eb78f -->

- 날짜: 2026-08-07
- 브랜치: `main`
- 작업 목표: Guest의 실제 RefreshSession 보유 여부와 탈퇴 후 동일 installationId 재사용 시 현재 동작을 확인한 이번 turn의 감사 기록을 남긴다.
- 변경 파일: 애플리케이션 코드와 테스트는 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 기존 workflow 변경은 건드리지 않았다.
- 구현 내용: Guest User와 초기 RefreshSession이 Mongo Transaction에서 함께 저장되고 기존 재발급·로그아웃 흐름을 공유함을 확인했다. 현재 탈퇴 기능은 미구현이며 설치 ID 해시가 남은 `WITHDRAWN` 문서는 동일 installationId 요청을 기존 계정 복구나 신규 생성 없이 409로 막는다.
- 실행한 테스트와 결과: 코드 변경이 없는 정적 확인이므로 테스트는 실행하지 않았고 `git diff --check`를 통과했다.
- 유지한 계약: installationId를 인증 또는 계정 복구 수단으로 사용하지 않는 정책과 기존 Guest 중복 요청 409 계약을 변경하지 않았다. Secret, Token, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: 향후 탈퇴 구현에서는 기존 Guest를 복구하지 않고 Session 폐기와 설치 ID 해시 점유 해제를 원자적으로 수행한 뒤 재가입 시 새 UUID와 새 RefreshSession을 생성하는 방식을 권장한다.
- 위험 요소: 현재 상태에서 Guest 문서만 수동으로 `WITHDRAWN` 처리하면 설치 ID 해시가 계속 점유되어 해당 설치가 신규 Guest를 만들 수 없다.
- 다음 작업: 회원 탈퇴 구현 이슈에 설치 ID 해제, Session 전체 폐기, 신규 Guest 생성과 경쟁 상황 검증을 포함한다.

## 2026-08-07 — 회원 탈퇴 Jira 생성 준비

<!-- codex-turn:019fda0c-7acf-7f83-89f8-072124e9277d -->

- 날짜: 2026-08-07
- 브랜치: `main`
- 작업 목표: LOCAL·GUEST 회원 탈퇴와 tombstone, 전체 RefreshSession 폐기, Guest 재가입 정책을 구현할 Jira 이슈 생성을 준비한다.
- 변경 파일: 애플리케이션 코드는 수정하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 기존 `.github/workflows/deploy-staging.yml` 변경은 건드리지 않았다.
- 구현 내용: Atlassian 공식 MCP로 Jira 프로젝트와 기존 이슈 형식을 조회하려 했으나 OAuth refresh credential이 유효하지 않아 조회·생성이 중단됐다. 저장소 규칙에 따라 실제 생성 전에 사용자에게 제목, 유형, 우선순위와 본문 초안을 제시하고 승인을 받을 준비를 했다.
- 실행한 테스트와 결과: 코드 변경이 없으므로 테스트는 실행하지 않았다. Jira 쓰기 작업도 수행되지 않았다.
- 유지한 계약: Jira에 Secret, Token, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않는 규칙과 생성 전 사전 승인 절차를 유지했다.
- 결정사항: 제안 이슈는 Task·High 우선순위, 제목 `[Identity] LOCAL·GUEST 회원 탈퇴 및 재가입 처리`, 대상 저장소 `Too-Much-I/app-back-end-identity`로 구성하고, 기존 인증·JWT·Learning Core 계약을 유지하는 범위로 작성한다.
- 위험 요소: Atlassian 연결을 재인증하기 전에는 Jira 이슈를 생성하거나 기존 프로젝트 필드·작성 형식을 재검증할 수 없다.
- 다음 작업: 사용자가 Jira 연결을 복구하고 아래 생성 초안을 승인하면 정확히 한 개의 이슈를 생성한 뒤 생성 결과만 보고한다.

## 2026-08-07 — 회원 탈퇴 Jira 이슈 생성

<!-- codex-turn:019fda15-6233-7651-804d-b865b4d93db7 -->

- 날짜: 2026-08-07
- 브랜치: `main`
- Jira: TMI-75
- 작업 목표: LOCAL·GUEST 회원 탈퇴와 Guest 재가입 정책 구현을 추적하는 Jira 이슈를 승인된 내용으로 생성한다.
- 변경 파일: 애플리케이션 코드는 수정하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 기존 `.github/workflows/deploy-staging.yml` 변경은 건드리지 않았다.
- 구현 내용: Atlassian 공식 MCP에서 TMI-40의 작성 형식과 TMI 프로젝트 Task 생성 메타데이터를 확인한 뒤 `[Identity] LOCAL·GUEST 회원 탈퇴 및 재가입 처리` 이슈를 작업(Task), High 우선순위로 정확히 한 개 생성했다. 본문에는 JWT sub·RefreshSession 소유권 검증, Provider별 재인증, WITHDRAWN tombstone, 인증 식별자 제거, 모든 Session 폐기, Mongo Transaction, Guest 신규 UUID 재가입, 테스트·문서·운영 주의사항과 제외 범위를 포함했다.
- 실행한 테스트와 결과: 코드 변경이 없는 Jira 생성 작업이므로 테스트는 실행하지 않았다. 생성 후 TMI-75를 다시 조회해 제목, 유형, High 우선순위, 해야 할 일 상태와 본문 반영을 확인했다.
- 유지한 계약: 기존 Jira 이슈의 상태·필드·댓글을 변경하지 않았고 TMI-75에도 Secret, Token, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다. Git commit과 push를 수행하지 않았다.
- 결정사항: 이슈 유형은 프로젝트의 실제 명칭인 `작업`, 우선순위는 `High`, 초기 상태는 기본값인 `해야 할 일`로 두었다. Jira 생성은 사용자가 초안을 확인하고 명시적으로 승인한 뒤 수행했다.
- 위험 요소: stateless Access Token의 잔여 유효성, Learning Core 데이터 정리와 outbox, 운영 MongoDB Transaction·index 검증은 TMI-75 본문에 구현 주의사항 또는 후속 범위로 명시했다.
- 다음 작업: 사용자가 구현 착수를 요청하면 TMI-75를 먼저 읽고 저장소 상태와 브랜치를 확인한 뒤 코드·테스트·문서 작업을 진행한다. Jira 상태 변경이나 댓글 등록은 별도 승인 없이는 수행하지 않는다.

## 2026-08-07 — TMI-75 LOCAL·GUEST 회원 탈퇴 구현

<!-- codex-turn:019fda19-0bcc-7450-98b9-eb203bfc732c -->

- 날짜: 2026-08-07
- 브랜치: `feat/TMI-75-user-withdrawal`
- Jira: TMI-75
- 작업 목표: JWT `sub`와 현재 RefreshSession 소유권을 기반으로 LOCAL·GUEST 회원 탈퇴를 처리하고, User tombstone·전체 Session 폐기·Guest 및 LOCAL 신규 계정 재가입을 기존 MongoDB 인증 구조 안에서 구현한다.
- 변경 파일: `User`, `RefreshSession`, `RevocationReason`, `UserRepository`와 custom fragment, 신규 `UserWithdrawalService`·`UserWithdrawalTransactionService`, 탈퇴 Request·Response, `UserController`, Auth·User 오류 코드, `UserConsentService`, 관련 도메인·application·Controller·Security·OpenAPI·수명 주기 테스트, `README.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 변경했다. 기존 `.github/workflows/deploy-staging.yml` 사용자 변경은 수정하지 않았다.
- 구현 내용: 보호된 `POST /api/v1/users/withdraw`가 JWT subject 사용자만 조회하고, Refresh Token 원문을 저장하지 않은 채 기존 SHA-256 해시 방식으로 Session 존재·소유권·미폐기·미만료를 확인한다. LOCAL은 기존 `PasswordEncoder`로 현재 비밀번호를 재검증하고 GUEST는 비밀번호를 요구하지 않는다. 이미 WITHDRAWN이면 기존 시각으로 200 멱등 성공하며 ACTIVE와 SUSPENDED는 탈퇴할 수 있다.
- 구현 내용: 기존 `mongoTransactionManager`가 적용된 별도 Spring Bean 안에서 User를 `WITHDRAWN` tombstone으로 조건부 갱신하고 해당 userId의 모든 미폐기 RefreshSession을 같은 시각과 `ACCOUNT_WITHDRAWN` 사유로 폐기한다. tombstone은 userId·provider·createdAt·consents를 유지하고 nickname을 익명화하며 email·normalizedEmail·passwordHash·guestInstallationIdHash를 Mongo `$unset`한다. User update 또는 Session 저장이 실패하면 Runtime 예외로 Transaction 전체가 rollback된다.
- 구현 내용: User의 기존 `status`와 `updatedAt`을 비교하는 partial update를 사용해 stale 전체 문서 저장을 피하고, 탈퇴 충돌은 최신 상태가 WITHDRAWN이면 멱등 성공, 아니면 자격 증명을 다시 확인해 한 번 재시도한 뒤 `WITHDRAWAL_CONFLICT` 409로 처리한다. 동의 갱신도 ACTIVE+updatedAt 조건의 partial update로 바꿔 탈퇴와 경합한 오래된 요청이 WITHDRAWN을 ACTIVE로 되돌리지 못하게 했다.
- 구현 내용: Guest 탈퇴에서 installation hash의 unique 점유를 해제해 같은 installationId의 다음 Guest 인증이 기존 tombstone을 복구하지 않고 새 UUID와 새 RefreshSession을 생성하도록 했다. ACTIVE Guest의 기존 중복 409는 유지하며 LOCAL 탈퇴도 이메일 unique 점유를 해제해 같은 이메일의 신규 UUID 가입이 가능하다. 활성 Session 조회용 `{ userId: 1, revokedAt: 1 }` compound index 계약을 추가했다.
- 실행한 테스트와 결과: 최종 `./gradlew clean test`가 성공했다. 38개 test suite의 284개 테스트가 실행됐고 실패·오류·건너뜀은 모두 0개였다. Guest 탈퇴 후 같은 installationId로 새 userId·새 RefreshSession 생성, 세 개의 기존 Session 전체 `ACCOUNT_WITHDRAWN` 폐기와 모든 기존 Refresh Token 재발급 실패, LOCAL 동일 이메일 재가입, 다른 사용자 Session 격리, LOCAL·GUEST·SUSPENDED·멱등 탈퇴, User/Session 실패 rollback, Spring Transaction proxy와 지정 manager, Security 401, OpenAPI, 로그인·Guest·Rotation·로그아웃·JWT·JWKS 회귀를 검증했다.
- 유지한 계약: User 물리 삭제, Refresh Token 원문 저장·로그, Guest 비밀번호 요구, WITHDRAWN 계정 복구, Access Token denylist, OAuth 재인증, Learning Core 직접 삭제와 outbox 임의 추가를 하지 않았다. RS256·JWT `sub`·issuer·audience·scope·JWKS, Opaque Refresh Token Rotation, 기존 인증·프로필·동의 API와 AI `user_id=examId` 계약을 유지했다. Secret, 실제 Token, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: User에 `@Version`을 새로 추가하지 않고 기존 문서와 호환되는 status+updatedAt compare-and-set partial update를 선택했다. 동의 기록은 법적 보존 가능성을 고려해 tombstone 안에 유지한다. Jira TMI-75의 댓글·필드·상태는 변경하지 않았고 Git commit·push·PR도 수행하지 않았다.
- 위험 요소: 격리 test profile은 실제 MongoDB를 연결하지 않으므로 Spring Transaction proxy의 rollback 경계는 검증했지만 replica set에서의 다중 collection rollback, custom repository fragment 연결과 index 실행계획은 staging에서 확인해야 한다. 자동 index 생성이 켜져 있어도 운영에서는 새 revision 전에 `{ userId: 1, revokedAt: 1 }` index를 승인된 절차로 선생성·검증해야 한다. 탈퇴 전 Access Token은 기본 `PT30M` 또는 배포 TTL 만료 전까지 Learning Core 같은 외부 서비스에서 암호학적으로 유효할 수 있다.
- 다음 작업: staging replica set에서 실제 Transaction 실패 주입, Guest·LOCAL 재가입과 compound index를 검증한다. `UserWithdrawn` outbox와 Learning Core 데이터 삭제·익명화, 서비스 간 Access Token 즉시 폐기는 별도 이슈로 설계한다. 사용자가 diff를 검토한 뒤 commit·push·PR을 직접 수행하며 Jira 댓글·상태 변경은 별도 승인 후 진행한다.

## 2026-08-07 — TMI-75 회원 탈퇴 데이터 제거 범위 확인

<!-- codex-turn:019fda63-121d-7ec1-bc5a-803ca732239a -->

- 날짜: 2026-08-07
- 브랜치: `feat/TMI-75-user-withdrawal`
- Jira: TMI-75
- 작업 목표: 회원 탈퇴 시 즉시 제거·익명화되는 정보와 보존되거나 Identity 범위 밖에 남는 정보를 현재 구현 기준으로 확인해 설명한다.
- 변경 파일: 애플리케이션 코드는 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. WORKLOG의 과거 기록과 기존 `.github/workflows/deploy-staging.yml` 사용자 변경은 건드리지 않았다.
- 구현 내용: User 문서는 물리 삭제하지 않고 email·normalizedEmail·passwordHash·guestInstallationIdHash를 Mongo `$unset`하며 nickname을 익명 값으로 교체한다. userId·provider·status·createdAt·updatedAt·withdrawnAt과 개인정보 처리방침·이용약관 동의 기록은 tombstone에 유지한다. RefreshSession 문서도 즉시 삭제하지 않고 모든 미폐기 Session을 같은 시각의 `ACCOUNT_WITHDRAWN` 상태로 폐기하며 기존 TTL 만료 후 비동기 정리 대상이 된다.
- 실행한 테스트와 결과: 코드 변경이 없는 범위 확인 작업이므로 새 테스트나 전체 테스트를 다시 실행하지 않았다. 직전 최종 `./gradlew clean test`의 38개 suite·284개 테스트 성공 결과를 유지하며, User partial update·tombstone 도메인 로직·Session 전체 폐기 코드를 정적으로 재확인했다.
- 유지한 계약: 동의 기록은 임의 삭제하지 않고 Learning Core 시험·결과, S3·AI 등 Identity 비소유 데이터도 직접 삭제하지 않는다. Refresh Token 원문은 애초에 서버에 저장하지 않으며 Access Token denylist를 추가하지 않았다. Secret, 실제 Token, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: 현재 회원 탈퇴를 완전한 전 시스템 물리 삭제로 설명하지 않고, Identity 인증 자격과 직접 식별자를 제거한 익명 tombstone 및 Session 폐기로 설명한다. 클라이언트는 성공 즉시 보유 Access/Refresh Token을 삭제해야 한다.
- 위험 요소: 보존된 userId와 동의 기록은 법적 보존·삭제 기간 정책이 별도로 필요하다. 기존 stateless Access Token은 만료 전까지 외부 서비스에서 암호학적으로 유효할 수 있고, Learning Core 데이터 삭제·익명화는 outbox와 소비자 구현 전까지 자동 수행되지 않는다.
- 다음 작업: 법무·제품 정책으로 동의 기록과 tombstone 보존 기간을 확정하고, `UserWithdrawn` outbox 및 Learning Core 후속 정리와 서비스 간 Access Token 즉시 폐기가 필요하면 별도 이슈로 구현한다.

## 2026-08-07 — TMI-75 회원 탈퇴 관측 로그 검토

<!-- codex-turn:019fdac4-7aeb-7f92-8a10-84dd77940f65 -->

- 날짜: 2026-08-07
- 브랜치: `feat/TMI-75-user-withdrawal`
- Jira: TMI-75
- 작업 목표: 회원 탈퇴 로직의 운영 동작을 확인할 로그 추가가 적절한지 현재 로깅·Transaction 구조를 근거로 검토하고 안전한 관측 범위를 제안한다.
- 변경 파일: 애플리케이션 코드와 테스트는 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 현재 애플리케이션은 전역 예외 처리에서 예상하지 못한 예외 타입만 기록하며 회원 탈퇴 전용 성공·멱등·충돌·실패 로그와 메트릭은 없다. 별도 Transaction Service의 proxy 호출이 반환된 뒤에만 commit이 완료되므로 외부 `UserWithdrawalService`에서 성공 로그를 남기고, 내부 결과에 폐기 Session 수를 포함해 전달하는 방식을 권장했다. idempotent·conflict retry·rejected·failed 이벤트는 성공 이벤트와 구분하고 예외를 숨기지 않고 다시 전파해야 한다.
- 실행한 테스트와 결과: 분석·설계 작업이므로 새 테스트와 `./gradlew clean test`는 실행하지 않았다. 기존 로거 사용처, 회원 탈퇴 application·Transaction 호출 경계, Actuator 의존성과 현재 health endpoint 노출만 정적으로 확인했다.
- 유지한 계약: Access Token, Refresh Token 원문, Authorization Header, password·passwordHash, email, installationId 원문·해시, token hash와 User 전체 문서는 로그에 포함하지 않는 기존 보안 계약을 유지한다. 로그는 API 응답이나 Transaction 결과를 변경하지 않고 예외를 성공으로 변환하지 않는다. Secret, 실제 Token, Password, 실제 Key, 전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: INFO에는 commit 이후 `event`, `userId`, `provider`, `withdrawnAt`, `revokedSessionCount`, `outcome`만 허용하고, 멱등 성공은 별도 outcome으로 구분한다. WARN은 충돌 재시도·최종 충돌·자격 검증 거절의 외부 오류 코드 수준으로 제한하며 세부 자격 실패 원인은 기록하지 않는다. 메트릭에는 provider·outcome·errorCode 같은 낮은 cardinality만 사용하고 userId는 태그로 사용하지 않는다.
- 위험 요소: Transaction 내부에서 commit 전에 완료 로그를 기록하면 실제 rollback·commit 실패와 로그가 불일치할 수 있다. 고유 userId를 메트릭 tag로 사용하면 cardinality가 폭증하고, 인증 실패 WARN을 무제한 기록하면 로그 비용·abuse 증폭 문제가 생길 수 있다. 일반 운영 로그는 법적 감사 원장을 대체하지 않는다.
- 다음 작업: 사용자가 구현을 요청하면 내부 Transaction 결과에 폐기 Session 수를 안전하게 전달하고 commit 이후 구조화 로그를 추가한다. 테스트에서는 성공·멱등·재시도·rollback별 이벤트와 민감값 비노출을 검증하고, 메트릭·CloudWatch 경보는 현재 운영 수집 경로를 확인한 뒤 별도 범위로 연결한다.

## 2026-08-07 — Identity 전체 운영 로깅 도입 계획

<!-- codex-turn:019fdac9-85f2-7100-8b2e-e57fd4204035 -->

- 날짜: 2026-08-07
- 브랜치: `main`
- 작업 목표: 현재 Identity 인증·세션·사용자 로직이 운영에서 정상 동작하는지 확인할 수 있도록 로그를 추가할 위치, 이벤트, 레벨, 민감정보 경계와 구현 순서를 계획한다.
- 변경 파일: 애플리케이션 코드와 테스트는 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 현재 애플리케이션 로거가 `GlobalExceptionHandler`의 예상 밖 예외 타입 기록 한 곳뿐이고 requestId·route·duration·도메인 상태 전이 로그가 없음을 확인했다. 1차는 구조화 stdout과 MDC requestId·HTTP 완료·중앙 예외/Security 결과, 2차는 회원가입·Guest 생성·로그인·Token 재발급/재사용·로그아웃·회원 탈퇴, 3차는 동의 갱신·Transaction 기동 검증과 대시보드/알림으로 나누었다.
- 실행한 테스트와 결과: 코드 변경이 없는 정적 분석 작업이므로 `./gradlew clean test`는 실행하지 않았다. 주요 Controller·application service·Transaction 경계·전역 예외/Security handler·Actuator 및 logging 설정 부재를 확인했고 문서 변경은 `git diff --check`를 통과했다.
- 유지한 계약: JWT `sub`의 실제 UUID 사용자 식별, RS256·issuer·audience·JWKS, Refresh Token 원문 비저장, 사용자/Session Transaction과 기존 API 응답 계약을 변경하지 않았다. Password, Access/Refresh Token, Authorization Header, email·nickname, installationId 원문·해시, token hash, JWT 본문, 실제 Key와 전체 MongoDB URI를 로그 계획에서 제외했다.
- 결정사항: 공통 요청 완료 로그는 Controller별 중복 로그 대신 새 observability filter 한 곳에서 route template·status·duration·errorCode를 기록하고, 비즈니스 로그는 application service의 저장 완료 또는 transactional proxy 반환 이후 상태 전이만 기록한다. INFO는 정상 상태 전이, WARN은 Refresh Token 재사용·동시성 최종 충돌 같은 주의 사건, ERROR는 예상 밖 내부 오류와 stack trace에 사용하며 정상 4xx에는 stack trace를 남기지 않는다.
- 위험 요소: Transaction 내부에서 성공을 기록하면 rollback 또는 commit 실패와 불일치할 수 있고, 요청/응답·Header·Token/Hash를 기록하면 자격증명이 노출된다. userId·requestId·세션 식별자를 메트릭 tag로 사용하거나 모든 인증 실패를 WARN으로 남기면 cardinality와 로그 비용이 급증하며, 운영 로그는 법적 감사 이력을 대신하지 않는다.
- 다음 작업: 사용자가 구현을 요청하면 1차 기반부터 작은 PR 단위로 적용하고, ListAppender/MockMvc 기반으로 이벤트 필드·MDC 정리·민감 테스트 값 비노출·실패 시 성공 이벤트 부재를 검증한 뒤 `./gradlew clean test`를 실행한다. 로그 수집 플랫폼이 확정되면 stdout JSON 형식과 보존 기간·대시보드·알림 임계값을 연결한다.

## 2026-08-07 — 예상 밖 5xx 단일 ERROR 기록 방식 구체화

<!-- codex-turn:019fdad0-e86f-7fb0-97e2-60e584cb6a6b -->

- 날짜: 2026-08-07
- 브랜치: `main`
- 작업 목표: 전체 로깅 계획의 “예상 밖 5xx는 ERROR 한 번만 기록”이 실제 코드에서 어떤 소유권과 흐름으로 구현되는지 구체화한다.
- 변경 파일: 애플리케이션 코드와 테스트는 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 새 요청 완료 filter를 application ERROR의 단일 소유자로 둔다. Service·Controller는 예외를 기록한 뒤 다시 던지지 않고 그대로 전파하며, `GlobalExceptionHandler`는 예상 밖 예외를 500 응답으로 변환하면서 예외 type과 message를 제외한 제한된 stack frame 같은 안전한 문맥을 request attribute에 저장하되 직접 로그하지 않는다. filter는 chain 종료 시 해당 문맥 또는 탈출 예외를 확인해 평소 INFO 완료 로그 대신 route·status·duration·requestId를 포함한 ERROR 한 건만 기록한다.
- 실행한 테스트와 결과: 코드 변경이 없는 설계 설명 작업이므로 `./gradlew clean test`는 실행하지 않았다. 현재 `GlobalExceptionHandler`의 단일 `log.error` 위치와 제안한 filter·handler 책임 분리를 정적으로 재확인했고 문서 변경은 `git diff --check`를 통과했다.
- 유지한 계약: 기존 공통 500 응답과 예외 전파 의미를 변경하지 않으며 Password, Token, Authorization Header, 개인정보, Token/installation hash, JWT 본문, 실제 Key와 전체 MongoDB URI를 오류 로그에 포함하지 않는다. 원본 예외 message를 그대로 로깅하지 않는 정책을 유지한다.
- 결정사항: “한 번”은 동일 요청의 예상 밖 실패에 대해 애플리케이션이 생성하는 ERROR event가 정확히 한 건이라는 뜻이다. 5xx 요청에는 별도 INFO 완료 event를 만들지 않고, `errorLogged` request attribute와 error dispatch 제외로 중복을 막는다. 이미 commit된 비즈니스 상태 전이 INFO/WARN은 같은 예외를 중복 기록한 것이 아니므로 별도 사건으로 유지할 수 있다.
- 위험 요소: filter 밖에서 Servlet container 또는 외부 APM이 같은 Throwable을 별도 기록할 수 있어 전체 플랫폼 로그까지 한 건임을 코드만으로 보장할 수는 없다. raw Throwable을 로거에 전달하면 예외 message나 cause에 자격증명·URI가 포함될 수 있고, async/error dispatch를 잘못 처리하면 중복 ERROR가 생길 수 있다.
- 다음 작업: 구현 시 `OncePerRequestFilter`, 안전한 failure context, errorCode/route request attribute와 단일 emit guard를 추가하고 MockMvc·ListAppender로 예상 밖 RuntimeException ERROR 1건, BusinessException ERROR 0건, 5xx INFO 완료 0건, MDC 정리와 민감 테스트 값 비노출을 검증한 뒤 `./gradlew clean test`를 실행한다.

## 2026-08-07 — Identity 구조화 운영 로그 구현

<!-- codex-turn:019fdad7-4fd9-79f0-97ff-08aba2ef29db -->

- 날짜: 2026-08-07
- 브랜치: `feat/logging` (`05342da` 기준, commit·push 미수행)
- 작업 목표: 계획한 requestId 기반 HTTP 완료 로그와 예상 밖 5xx 단일 ERROR 소유권을 구현하고, Identity의 주요 인증·세션·사용자 상태 전이를 민감정보 없이 운영에서 추적할 수 있게 한다.
- 변경 파일: 신규 `global/observability/RequestLoggingFilter.java`·`RequestLogContext.java`, `GlobalExceptionHandler`, Security 401/403 handler, Auth의 signup·guest·login·reissue·logout·logout-all application service, User의 consent·withdrawal application/transaction service와 신규 `WithdrawalTransactionResult`, `MongoTransactionCapabilityVerifier`, `application.yml`, `.env.example`, `README.md`, 관련 application·Security·observability 테스트와 신규 `support/LogCapture`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 변경했다.
- 구현 내용: `X-Request-ID`는 `[A-Za-z0-9._-]{1,64}`만 재사용하고 없거나 잘못되면 UUID를 생성해 응답 Header와 MDC에 넣은 뒤 요청 종료 시 정리한다. 공통 filter는 route template·method·status·duration·outcome·errorCode를 `http.request.completed` INFO로 기록하되 health·Swagger/OpenAPI·JWKS 정상 요청은 제외한다.
- 구현 내용: 예상 밖 5xx는 `http.request.failed` ERROR 한 건만 남기고 같은 요청의 INFO 완료 로그는 만들지 않는다. `GlobalExceptionHandler`는 직접 ERROR를 남기지 않고 원본 예외 message·raw Throwable 없이 exception/cause 타입과 message 없는 최대 24개 stack frame만 request attribute로 전달하며, `errorLogged` guard와 error dispatch 제외로 애플리케이션 중복 기록을 막는다. Business·Validation·Security 401/403은 공통 errorCode만 요청 로그에 전달한다.
- 구현 내용: 회원가입·Guest 생성·로그인, Refresh Token 재발급·재사용 탐지·동시 Rotation 거절, 단일·전체 로그아웃, 동의 갱신, 회원 탈퇴 성공·멱등·충돌과 Mongo Transaction capability 기동 검증을 구조화 이벤트로 추가했다. 성공 이벤트는 저장 완료 또는 transactional proxy 반환 이후에만 기록하고, 회원 탈퇴 Transaction 결과에는 실제 폐기 Session 수를 포함해 외부 service가 commit 이후 기록하도록 했다.
- 실행한 테스트와 결과: `./gradlew clean test`가 성공했다. 40개 test suite의 292개 테스트가 실행됐고 실패·오류·건너뜀은 모두 0개였다. 예상 밖 5xx의 ERROR 1건·INFO 0건, BusinessException ERROR 0건, requestId 생성·전파·MDC 정리, Security 401의 filter 통과, 민감 테스트 문자열·자격증명·개인정보·Token/Hash 비노출, 저장 완료 이벤트·회원 탈퇴 폐기 Session 수와 ECS 설정의 Spring Context 기동을 검증했다.
- 유지한 계약: JWT `sub`의 실제 UUID, RS256·issuer·audience·JWKS, Opaque Refresh Token 해시 저장, 기존 API 응답과 Transaction 경계를 변경하지 않았다. Password, Access/Refresh Token, Authorization Header, email·nickname, installationId 원문·해시, token hash, JWT 본문, 실제 Key, 전체 MongoDB URI와 요청/응답 본문을 로그나 작업 기록에 넣지 않았다. userId는 허용된 상태 전이 로그에서만 사용하고 메트릭 tag로 사용하지 않는다.
- 결정사항: 공통 HTTP 로그와 예상 밖 ERROR 소유권은 filter 한 곳에 두고 Controller·Repository·Entity에는 중복 로그를 추가하지 않았다. 기본 stdout 포맷은 Spring Boot ECS 구조화 로그로 두되 `LOGGING_STRUCTURED_FORMAT_CONSOLE` 환경변수로 조정할 수 있게 했고, 정상 4xx는 ERROR가 아닌 완료 로그로 분류한다. Git commit·push와 Jira 댓글·상태·필드 변경은 수행하지 않았다.
- 위험 요소: 애플리케이션의 예상 밖 5xx ERROR는 한 건으로 제한했지만 Servlet container나 외부 APM이 같은 오류를 별도로 수집할 수 있으므로 staging에서 logger category와 error dispatch를 확인해야 한다. 안전한 오류 문맥은 원본 message를 의도적으로 제외하므로 상세 진단에는 재현·메트릭·추적 도구가 추가로 필요하다. 구조화 로그 보존 기간·대시보드·알림 임계값은 아직 운영 수집 플랫폼에 연결하지 않았다.
- 다음 작업: staging stdout 수집에서 ECS 파싱, requestId 검색, route template과 민감정보 비노출, 예상 밖 5xx 중복 여부를 확인한다. event·outcome·errorCode·provider 같은 낮은 cardinality 필드를 사용해 대시보드·메트릭·알림과 보존 정책을 별도 운영 작업으로 확정한다. 사용자가 diff를 검토한 뒤 commit·push를 직접 수행한다.

## 2026-08-07 — ECS 실행 로그 동작 확인

<!-- codex-turn:019fdb14-8160-7e93-a04f-8ddcf14fda84 -->

- 날짜: 2026-08-07
- 브랜치: `main` (`a5802ad`)
- 작업 목표: 사용자가 제공한 실제 기동 로그가 구조화 운영 로그 구현으로 의도된 출력인지 구분하고 보안·노이즈 관점의 후속 조치를 확인한다.
- 변경 파일: 애플리케이션 코드와 설정은 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: `logging.structured.format.console=ecs`는 사용자 정의 이벤트만이 아니라 root logger를 사용하는 Spring Boot·Tomcat·MongoDB 드라이버의 콘솔 로그 전체를 ECS 한 줄 JSON으로 직렬화하므로 제공된 형식 자체는 의도된 결과임을 확인했다. 사용자 정의 `mongodb.transaction_capability.verified` event의 event·outcome·topology·sessionsSupported 필드도 정상 출력됐다.
- 구현 내용: 제공된 출력에는 실제 API 호출이 없어 `http.request.completed`나 인증·사용자 상태 전이 event가 없고, 정상 health·Swagger/OpenAPI·JWKS 요청은 원래 노이즈 제외 대상이다. 종료 코드 130과 SIGINT 표시는 IDE 중지 또는 Ctrl+C로 종료했을 때의 정상적인 외부 인터럽트 결과로 분류했다.
- 실행한 테스트와 결과: 코드 변경이 없는 런타임 로그 분석이므로 새 테스트와 `./gradlew clean test`는 실행하지 않았다. 제공된 ECS 필드와 현재 구조화 로그 설정·사용자 정의 기동 event를 대조했다.
- 유지한 계약: 제공된 출력의 계정 식별자, 클러스터 주소와 topology 세부값을 문서에 복사하지 않았고 Password, Token, 실제 Key와 전체 MongoDB URI를 기록하지 않았다. JWT·RefreshSession·API 응답과 Transaction 계약은 변경하지 않았다.
- 결정사항: ECS JSON 출력 자체는 로그 수집기에 적합한 의도된 동작으로 유지한다. 다만 제3자 logger의 INFO 범위는 사용자 정의 로그의 민감정보 제한과 별개이므로, 명시적 구현 요청 없이 이번 확인에서 logger level을 변경하지 않았다. Git commit·push와 Jira 변경도 수행하지 않았다.
- 위험 요소: MongoDB 드라이버 INFO가 비밀번호 원문이나 전체 연결 URI를 출력하지 않더라도 계정 식별자와 클러스터 endpoint·topology를 노출할 수 있으며, 모든 framework INFO를 수집하면 비용과 검색 노이즈가 증가한다. 외부에 공유하거나 장기 보존하기 전에 수집 접근 권한과 logger category를 제한해야 한다.
- 다음 작업: 사용자가 요청하면 기본 root INFO는 유지하면서 `org.mongodb.driver`만 WARN으로 낮추는 설정과 테스트·문서를 추가한다. 애플리케이션 event만 보고 싶다면 별도 환경에서 root WARN과 `web.tosunsaeng.identity` INFO 조합의 운영상 손실도 함께 검토한다.

## 2026-08-07 — LogoutAllService AssertJ 타입 추론 오류 수정

<!-- codex-turn:019fdb18-4b54-7bf0-b40f-21473016dcbe -->

- 날짜: 2026-08-07
- 브랜치: `main` (`a5802ad`)
- 작업 목표: `LogoutAllService`가 Token issuer에 의존하지 않는지 확인하는 reflection 테스트의 AssertJ `doesNotContain` IDE 해석 오류를 검증 의미 변경 없이 해결한다.
- 변경 파일: `src/test/java/web/tosunsaeng/identity/domain/auth/application/LogoutAllServiceTests.java`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 변경했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: `Stream<Class<?>>`를 `assertThat`에 바로 전달하면 wildcard capture 때문에 IDE가 `Class<AccessTokenIssuer>`와 `Class<RefreshSessionIssuer>` varargs를 같은 요소 타입으로 추론하지 못할 수 있음을 확인했다. reflection 결과를 먼저 `List<Class<?>> dependencyTypes`로 수집해 AssertJ 요소 타입을 명시적으로 고정한 뒤 기존 `doesNotContain` 검증을 유지했다.
- 실행한 테스트와 결과: `./gradlew test --tests 'web.tosunsaeng.identity.domain.auth.application.LogoutAllServiceTests'`와 `./gradlew clean test`가 모두 성공했다. 전체 40개 suite·292개 테스트가 실행됐고 실패·오류·건너뜀은 모두 0개였다.
- 유지한 계약: LogoutAllService가 Access Token이나 RefreshSession 발급기에 의존하지 않고 현재 JWT 사용자의 기존 RefreshSession만 폐기하는 계약을 그대로 검증한다. JWT·Refresh Token·로그·API 응답·Transaction 동작은 변경하지 않았으며 Secret, Token, Password, 실제 Key와 전체 MongoDB URI를 기록하지 않았다.
- 결정사항: IDE inspection을 억제하거나 raw type cast를 추가하지 않고 Java 제네릭 타입을 지역 변수에 명시해 Gradle compiler와 IDE가 동일하게 해석하도록 했다. Git commit·push와 Jira 변경은 수행하지 않았다.
- 위험 요소: 기능 변경은 없으며 현재 알려진 추가 위험은 없다. 클래스 필드 구조를 변경하면 이 architecture 성격의 reflection 테스트도 의도에 맞게 함께 갱신해야 한다.
- 다음 작업: 사용자가 IDE에서 Gradle project reload 후 오류 표시가 사라졌는지 확인하고 변경을 검토한 뒤 commit·push를 직접 수행한다.

## 2026-08-07 — LogoutAllService wildcard capture 최종 제거

<!-- codex-turn:019fdb20-0f45-7f42-a34b-981517a5d0dd -->

- 날짜: 2026-08-07
- 브랜치: `main` (`a5802ad`)
- 작업 목표: 앞선 `List<Class<?>>` 지역 변수에도 남아 있던 `Class<capture<?>>` 대입 오류를 제거하고 IDE와 Java compiler가 모두 명확히 해석하는 테스트로 정리한다.
- 변경 파일: `src/test/java/web/tosunsaeng/identity/domain/auth/application/LogoutAllServiceTests.java`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 변경했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 앞선 중간 수정 기록은 그대로 보존했다.
- 구현 내용: `Field#getType()` 결과를 Stream에서 List로 변환하는 과정 자체가 wildcard capture를 유지하므로 `List<Class<?>>` 대입을 제거했다. 선언 필드를 바로 순회하는 Java Stream `anyMatch`에서 각 field type을 `AccessTokenIssuer.class`와 `RefreshSessionIssuer.class`에 비교하고, 의존성 존재 여부 boolean이 false인지 AssertJ로 검증한다.
- 실행한 테스트와 결과: `./gradlew clean test`가 성공했다. 전체 40개 suite·292개 테스트가 실행됐고 실패·오류·건너뜀은 모두 0개였으며 test source compile도 정상 완료됐다.
- 유지한 계약: LogoutAllService가 Access Token 또는 RefreshSession 발급기에 직접 의존하지 않는다는 기존 architecture 검증 의미를 유지했다. JWT·Refresh Token·로그·API 응답과 Transaction 동작은 변경하지 않았으며 Secret, Token, Password, 실제 Key와 전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 명시적 cast, raw type, suppression 또는 wildcard collection 대입을 사용하지 않고 boolean predicate로 제네릭 경계를 제거했다. Git commit·push와 Jira 변경은 수행하지 않았다.
- 위험 요소: 기능 변경은 없으며 현재 알려진 추가 위험은 없다. 테스트는 정확한 field type을 비교하므로 향후 wrapper나 상속 기반 의존성까지 금지하려면 `isAssignableFrom` 기준으로 별도 강화해야 한다.
- 다음 작업: 사용자가 IDE에서 이 테스트의 오류 표시가 제거됐는지 확인하고 변경을 검토한 뒤 commit·push를 직접 수행한다.

## 2026-08-08 — 구조화 로그 한글화 범위 검토

<!-- codex-turn:019fdf1c-608c-7700-9640-0ea7cc6c75a5 -->

- 날짜: 2026-08-08
- 브랜치: `main` (`61d43de`)
- 작업 목표: 영어 구조화 로그의 raw console 가독성을 높이기 위해 한글화할 범위와 유지해야 할 운영 검색 계약을 결정한다.
- 변경 파일: 애플리케이션 코드와 테스트는 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 현재 애플리케이션 정의 로그 message가 영어 문장으로 구성되고, ECS 표준 key와 event·outcome·errorCode가 검색·집계에 사용되는 구조를 확인했다. 사람이 직접 읽는 `message`만 한글 문장으로 바꾸면 framework 영어 로그 사이에서 애플리케이션 이벤트를 구분하기 쉬우면서 기존 검색·대시보드·알림 쿼리를 유지할 수 있다고 판단했다.
- 실행한 테스트와 결과: 코드 변경이 없는 설계 검토이므로 새 테스트와 `./gradlew clean test`는 실행하지 않았다. 애플리케이션의 구조화 로그 호출 지점과 message·key-value 구성을 정적으로 확인했다.
- 유지한 계약: `event`, `outcome`, `errorCode`, requestId, route, status와 ECS 표준 field 이름은 영어의 안정적인 machine-readable 계약으로 유지한다. Password, Token, Authorization Header, 개인정보, 실제 Key와 전체 MongoDB URI를 message에 추가하지 않으며 JWT·API·Transaction 계약은 변경하지 않았다.
- 결정사항: 전체 로그를 번역하지 않고 애플리케이션이 직접 작성한 `message`만 한글화하는 방식을 권장한다. Spring·Tomcat·MongoDB 같은 제3자 framework message, 예외 type과 stack frame은 그대로 두며 대시보드의 사용자 표시명은 한글로 구성할 수 있다. 명시적 구현 요청 전에는 로그 문자열을 변경하지 않는다.
- 위험 요소: event·outcome까지 한글화하면 기존 운영 쿼리·경보와 외부 도구 연동이 깨질 수 있다. message만 한글화해도 framework 로그는 영어로 남으므로 raw JSON 전체를 읽는 불편은 logger level·필터·대시보드로 별도 완화해야 한다.
- 다음 작업: 사용자가 구현을 요청하면 애플리케이션 정의 message를 일관된 한글 문장으로 변경하고 event·outcome·errorCode 불변, UTF-8 출력, 민감정보 비노출과 전체 회귀 테스트를 검증한다.

## 2026-08-08 — 애플리케이션 구조화 로그 message 한글화

<!-- codex-turn:019fdf21-c981-7773-b3cc-ba8051be5b27 -->

- 날짜: 2026-08-08
- 브랜치: `main` (`61d43de` 기준, commit·push 미수행)
- 작업 목표: 운영 검색 계약을 유지하면서 raw ECS 콘솔에서 애플리케이션 로그를 쉽게 구별할 수 있도록 사람이 읽는 message를 한글화한다.
- 변경 파일: `RequestLoggingFilter`, `MongoTransactionCapabilityVerifier`, Auth의 signup·guest·login·reissue·logout·logout-all service, User의 consent·withdrawal service, `README.md`, 해당 observability·config·Auth·User 테스트, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 변경했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 애플리케이션이 직접 작성한 18개 로그 message를 HTTP 요청 완료·예상 밖 실패, MongoDB Transaction 지원 확인, 회원가입·게스트 등록·로그인, Refresh Token 재발급·재사용·동시 요청 거절, 단일·전체 로그아웃, 동의 갱신, 회원 탈퇴 완료·충돌 의미에 맞는 한글 문장으로 변경했다. Spring·Tomcat·MongoDB framework 자체 message와 예외 type은 변경하지 않았다.
- 구현 내용: ECS field name과 `event`, `outcome`, `errorCode`, requestId, route, status 등 machine-readable 값은 모두 기존 영어 계약으로 유지했다. README에 message와 검색 식별자의 언어 경계를 문서화하고 HTTP 정상·예상 밖 실패 및 주요 인증·사용자 상태 전이 테스트에서 정확한 한글 message를 검증했다.
- 실행한 테스트와 결과: `./gradlew clean test`가 성공했다. 전체 40개 suite·292개 테스트가 실행됐고 실패·오류·건너뜀은 모두 0개였다. 애플리케이션 로그 호출 지점에 영어-only message가 남지 않은 것도 정적으로 확인했으며 `git diff --check`를 통과했다.
- 유지한 계약: 로그 event 이름·outcome·errorCode와 레벨, 예상 밖 5xx ERROR 1건, requestId·route·duration, 저장/Transaction 이후 상태 전이 시점과 API·JWT·RefreshSession 동작을 변경하지 않았다. Password, Token, Authorization Header, 개인정보, 실제 Key와 전체 MongoDB URI를 message나 작업 기록에 추가하지 않았다.
- 결정사항: 운영 자동화는 번역 가능한 message가 아니라 안정적인 event·outcome·errorCode를 사용하고, message는 한국어 운영자가 읽기 쉬운 한글 완결 문장으로 관리한다. 기술 용어인 HTTP·MongoDB·Refresh Token은 기존 표기를 유지했다. Jira 변경과 Git commit·push는 수행하지 않았다.
- 위험 요소: Spring·Tomcat·MongoDB 등 제3자 framework message는 영어로 남기 때문에 전체 raw JSON은 혼용 언어가 된다. message 문자열을 직접 파싱하는 외부 소비자가 있다면 event 기반으로 전환해야 하며 staging 수집기에서 UTF-8 보존을 확인해야 한다.
- 다음 작업: staging stdout 수집에서 한글 message가 깨지지 않는지 확인하고 기존 event 기반 검색·대시보드·알림 쿼리가 그대로 동작하는지 검증한다. 사용자가 diff를 검토한 뒤 commit·push를 직접 수행한다.

## 2026-08-10 — Sentry Spring Boot 적용 구성 검토

<!-- codex-turn:019fea67-f9bd-7580-901f-009a5774116b -->

- 날짜: 2026-08-10
- 브랜치: `main` (`0f0a1aa`)
- 작업 목표: 제안된 Sentry Gradle plugin·SDK 자동 설치 구성이 Identity Service에 충분한지 공식 문서와 현재 예외·로그 구조를 기준으로 검토한다.
- 변경 파일: 애플리케이션 코드, `build.gradle`, 설정과 테스트는 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Sentry 공식 Spring Boot 문서에서 JVM Gradle plugin `6.18.0`과 Spring Boot 버전에 맞는 starter 자동 선택을 확인했으며 Spring Boot 3은 Jakarta starter가 필요하다. 제안한 `autoInstallation`과 환경변수 기반 build 인증 방식은 구조적으로 맞지만, plugin의 source context 업로드와 runtime 오류 전송은 별도이므로 DSN·environment·release·enabled 설정이 추가로 필요하다고 판단했다.
- 구현 내용: Identity는 `GlobalExceptionHandler`가 예상 밖 Exception까지 500 응답으로 처리하므로 Sentry 기본 unhandled-only 수집만 사용하면 해당 오류가 누락될 수 있다. 공식 `exception-resolver-order`를 최우선으로 바꾸면 `@ExceptionHandler` 처리 예외도 수집되지만 Business·Validation 4xx까지 포함될 수 있고, Sentry Logback integration은 기본적으로 ERROR 로그를 Issue로 보내므로 기존 `http.request.failed`와 원본 예외 수집이 중복될 수 있음을 확인했다.
- 실행한 테스트와 결과: 코드 변경이 없는 검토 작업이므로 `./gradlew clean test`는 실행하지 않았다. 현재 `build.gradle`, main/test application 설정, catch-all 예외 handler, 단일 ERROR 요청 filter와 Sentry 공식 Spring Boot·Logback 문서를 정적으로 대조했고 문서 변경은 `git diff --check`로 검증한다.
- 유지한 계약: Sentry 인증 값, DSN, 실제 환경 식별값을 저장소나 작업 기록에 추가하지 않았다. Password, Token, Authorization Header, request body, 개인정보, 실제 Key와 전체 MongoDB URI 비노출 및 예상 밖 5xx 애플리케이션 ERROR 1건 계약을 유지했고 Git commit·push와 Jira 변경을 수행하지 않았다.
- 결정사항: 제안한 Gradle block만으로 운영 적용이 완료된 것으로 보지 않는다. build 인증 값은 source context 업로드가 필요한 CI에만 주입하고 runtime은 환경변수 설정을 사용하며 test profile은 비활성화한다. SentryAppender issue 전송은 끄고 예상 밖 오류만 명시적 한 경계에서 보내는 방향을 권장하며, expected 4xx는 Sentry Issue에서 제외한다.
- 위험 요소: `includeSourceContext=true`는 소스 코드를 외부 Sentry 프로젝트로 업로드하므로 저장소 secret 부재뿐 아니라 조직 접근 권한과 보존 정책 승인이 필요하다. raw Throwable은 메시지에 자격증명·endpoint가 포함될 수 있고 request context는 인증 Header·body·개인정보 노출 위험이 있으며, tracing을 1.0으로 시작하면 비용과 데이터 수집량이 급증할 수 있다.
- 다음 작업: 구현 시 Sentry plugin·runtime 설정, 환경변수 예시, test 비활성화, SentryAppender 중복 차단과 안전한 예상 밖 오류 capture 경계를 함께 추가한다. DSN 없이 전체 테스트가 외부 호출 없이 통과하는지, expected 4xx 0건·예상 밖 5xx 1건·민감 테스트 값 비노출을 검증하고 staging에서 release/environment/source context 연결을 확인한다.

## 2026-08-10 — Sentry DSN 전달·주입 경계 확인

<!-- codex-turn:019fea72-9a40-7f33-81ea-6d4825de92d8 -->

- 날짜: 2026-08-10
- 브랜치: `main` (`0f0a1aa`)
- 작업 목표: Sentry 설정 구현에 실제 DSN 전달이 필요한지 확인하고 Secret을 저장소와 대화에 노출하지 않는 작업 방식을 확정한다.
- 변경 파일: 애플리케이션 코드, Gradle과 runtime 설정은 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 실제 DSN 없이도 Gradle plugin, `${SENTRY_DSN}` runtime placeholder, enabled·environment·release·PII·request body·tracing 설정, test profile 비활성화, SentryAppender 중복 방지와 안전한 예상 밖 오류 capture 코드를 모두 구현·테스트할 수 있음을 확인했다. 실제 값은 사용자가 로컬 비추적 환경 파일이나 배포 Secret에 직접 주입하도록 경계를 정했다.
- 실행한 테스트와 결과: 코드 변경이 없는 작업 방식 확인이므로 `./gradlew clean test`는 실행하지 않았다. 현재 `.env*` ignore와 Secret 비기록 규칙, 앞선 Sentry 연동 설계를 정적으로 재확인했다.
- 유지한 계약: 실제 DSN, 인증 값, Token, Password, 실제 Key와 전체 MongoDB URI를 요청하거나 기록하지 않았다. Sentry가 비활성화된 test profile과 외부 호출 없는 테스트 원칙, 기존 API·JWT·로그·Transaction 계약을 유지했다.
- 결정사항: 사용자는 실제 DSN을 채팅으로 제공하지 않는다. 저장소에는 환경변수 이름과 안전한 기본값만 두며 build용 인증 값은 CI secret, runtime DSN은 배포 Secret으로 분리한다. Git commit·push와 Jira 변경은 수행하지 않았다.
- 위험 요소: 실제 DSN을 소스·문서·채팅·일반 로그에 붙여 넣으면 불필요한 외부 노출과 오용 가능성이 생긴다. placeholder 구현만으로 Sentry 연결 성공을 증명할 수는 없으므로 staging Secret 주입 후 별도 확인이 필요하다.
- 다음 작업: 사용자가 구현을 요청하면 실제 DSN 없이 전체 Sentry 설정과 테스트를 적용한다. 사용자는 완료된 환경변수 contract에 staging DSN·environment·release를 직접 주입하고 Sentry 프로젝트에서 예상 밖 오류 한 건과 expected 4xx 비수집을 확인한다.

## 2026-08-10 — Sentry 안전 적용 계획 수립

<!-- codex-turn:019fea74-8eaf-7553-8b21-92c60ca0d237 -->

- 날짜: 2026-08-10
- 브랜치: `main` (`0f0a1aa`)
- 작업 목표: Identity의 catch-all 예외 처리와 단일 ERROR 로그 계약을 유지하면서 Sentry를 안전하게 도입할 구현 순서, 검증 기준과 운영 활성화 경계를 계획한다.
- 변경 파일: 애플리케이션 코드, Gradle, runtime 설정과 테스트는 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 1단계에서 Gradle plugin·SDK 버전과 Spring Boot 3 Jakarta starter 자동 설치를 확인하고 source context upload를 CI opt-in으로 분리한다. 2단계에서는 Sentry disabled·빈 DSN·PII false·request body never·trace 0·SentryAppender off의 안전한 runtime 기본값, 환경변수 contract와 test profile 비활성화를 적용한다.
- 구현 내용: 3단계에서는 공식 unhandled 기본 resolver 순서를 유지하면서 catch-all handler가 처리한 예상 밖 Exception만 명시적으로 capture하고 Business·Validation·Security 4xx는 제외한다. 4단계에서는 request-scoped capture와 `beforeSend` whitelist로 exception message, body, query, Header, cookie, user context를 제거하고 requestId·errorCode·method·route template·status·environment·release만 허용한다.
- 구현 내용: 5단계에서는 mock 또는 in-memory transport로 expected 4xx 0건, handled unexpected 5xx 1건, SentryAppender 중복 0건, 민감 테스트 값 0건과 기존 애플리케이션 ERROR 1건을 검증하고 전체 테스트를 실행한다. 6단계는 CI source context·immutable release와 staging Secret 주입 검증, 7단계는 errors-only 점진 활성화 후 volume·중복·데이터 검토와 별도 alert/sampling 승인으로 구성했다.
- 실행한 테스트와 결과: 코드 변경이 없는 계획 작업이므로 `./gradlew clean test`는 실행하지 않았다. 현재 Sentry 공식 문서 검토 결과, `GlobalExceptionHandler`, `RequestLoggingFilter`, main/test 설정과 Secret ignore 경계를 계획에 반영했고 문서 변경은 `git diff --check`로 검증한다.
- 유지한 계약: 실제 DSN, build 인증 값, Token, Password, Authorization Header, 개인정보, 실제 Key와 전체 MongoDB URI를 요청하거나 기록하지 않았다. 예상 밖 5xx 애플리케이션 ERROR 1건, 정상 4xx 비오류 처리, JWT·API·RefreshSession·Transaction 및 외부 호출 없는 test 계약을 유지하는 계획이다.
- 결정사항: Sentry Issue 수집은 번역 가능한 log message가 아닌 Throwable 기반 오류 event로 분리하되 원본 exception message와 request context는 제거한다. SentryAppender, tracing, profiling과 Sentry Logs는 기본 비활성화하고 source context는 조직 접근·보존 정책 확인 후 CI에서만 활성화한다. Git commit·push와 Jira 변경은 수행하지 않았다.
- 위험 요소: Sentry SDK API에서 exception message를 완전히 정제하면서 stack trace grouping을 유지하는 구현을 버전별로 확인해야 한다. 공식 unhandled 자동 수집과 명시적 handled capture 경계가 Servlet·filter 오류에서 중복되지 않는지 integration test가 필요하며, source upload와 외부 telemetry는 조직의 접근·보존 정책 승인이 필요하다.
- 다음 작업: 사용자가 구현을 요청하면 1~5단계를 실제 DSN 없이 먼저 적용해 전체 테스트를 통과시키고, 사용자가 staging Secret과 CI build 인증 값을 직접 주입한 뒤 6단계 검증 결과를 확인한다. 운영 활성화와 alert·sampling은 그 결과를 검토한 후 별도로 진행한다.

## 2026-08-10 — Sentry 최종 민감정보 방어선과 단일 Issue 수집 구현

<!-- codex-turn:019feaaa-11c7-7f90-a3dd-a7721a073f32 -->

- 날짜: 2026-08-10
- 브랜치: `main` (`0f0a1aa` 기준, commit·push 미수행)
- 작업 목표: Sentry Spring Boot 연동 계획을 구현하되 `beforeSend`를 최종 민감정보 방어선으로 만들고, `GlobalExceptionHandler`가 명시 capture한 예상 밖 Exception이 SDK resolver 또는 logging integration으로 두 번째 Issue를 만들지 않는지 최종 transport 기준으로 검증한다.
- 변경 파일: `build.gradle`, `.env.example`, `README.md`, `src/main/resources/application.yml`, `src/test/resources/application-test.yml`, `GlobalExceptionHandler.java`, 신규 `SentryExceptionReporter.java`, 신규 `SentryEventSanitizer.java`, 신규 `SentryCaptureIntegrationTests.java`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 변경했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Sentry JVM Gradle plugin `6.18.0`과 SDK `8.42.0`을 적용해 Spring Boot 3 Jakarta starter를 자동 설치하고, source context는 비공백 build 인증 값이 주입된 CI에서만 활성화하도록 분리했다. Runtime은 기본 disabled·빈 DSN, PII false, request body `NONE`, server name·module 전송 off, tracing·profiling·Sentry Logs off와 `sentry.logging.enabled=false`로 설정했으며 test profile도 명시적으로 비활성화했다.
- 구현 내용: `GlobalExceptionHandler`의 catch-all 예상 밖 Exception 경계만 Sentry에 명시 capture한다. capture scope를 먼저 비운 뒤 검증된 requestId, errorCode, HTTP method, route template와 500 status만 tag로 추가하며 Validation·Business·Security 4xx는 capture하지 않는다. 공식 unhandled exception resolver의 기본 순서는 앞당기지 않았고 기존 요청 filter의 `http.request.failed` ERROR 한 건 계약을 유지했다.
- 구현 내용: `beforeSend`는 기존 event를 부분 마스킹하지 않고 새 `SentryEvent`를 생성하는 whitelist로 구현했다. 원본 exception message, request·response body와 URL·query·Header·Cookie, user, breadcrumb, extra, thread, runtime context, fingerprint, module 목록과 unknown 확장 필드는 버리고, event ID·시각·level, 설정에서 읽고 검증한 environment·release, message 없는 exception type과 data/context 없는 stack frame, 허용 tag와 source context 연결용 UUID 형식 JVM debug bundle ID만 옮긴다.
- 구현 내용: Spring 전체 MVC·Security·Sentry integration을 사용하는 test에서 custom event processor가 예외 message와 request/user/context/unknown 등 모든 민감 위치에 동일한 test-only sentinel을 삽입한다. 외부 통신 없는 custom transport가 `beforeSend` 이후 envelope의 최종 event item JSON 전체를 직접 받아 sentinel 부재를 검사한다. 같은 요청의 handler 명시 capture 후 실제 `http.request.failed` ERROR 로그까지 발생시킨 상태에서 event가 정확히 1건임을 검증하고, expected Business 4xx event 0건 및 Sentry Logback initializer bean 부재도 검증했다.
- 실행한 테스트와 결과: `dependencyInsight`로 `sentry-spring-boot-jakarta`, `sentry-spring-jakarta`, `sentry-logback`이 모두 `8.42.0`으로 해석됨을 확인했다. 신규 `SentryCaptureIntegrationTests` 3개가 성공했다. 첫 전체 실행에서 request body enum을 잘못 표기한 설정 바인딩 실패를 확인해 SDK 상수에 맞는 `none`으로 수정했고, 최종 `./gradlew clean test`가 전체 41개 suite·295개 테스트, 실패·오류·건너뜀 0개로 성공했다. `git diff --check`도 성공했다.
- 유지한 계약: 실제 DSN·build 인증 값·Password·Access Token·Refresh Token·Authorization Header·개인정보·실제 Key·전체 MongoDB URI를 소스, 설정, 문서와 작업 기록에 넣지 않았다. API·JWT·RefreshSession·Mongo Transaction 계약, expected 4xx 비오류 처리, 예상 밖 5xx 애플리케이션 ERROR 1건과 test 외부 인프라 비호출 원칙을 유지했다. Git commit·push와 Jira 조회·변경은 수행하지 않았다.
- 결정사항: Sentry Issue의 단일 소유자는 MVC catch-all이 처리한 예상 밖 오류에 대한 명시 capture이며 Logback ERROR는 stdout 관측 전용으로 유지한다. 민감정보 안전성은 테스트가 callback 반환 객체만 검사하는 수준이 아니라 SDK의 모든 processor와 `beforeSend`를 지난 뒤 transport가 받은 최종 serialized event 전체로 보증한다. 실제 DSN은 배포 Secret, source context 인증 값은 승인된 CI build Secret으로 분리한다.
- 위험 요소: whitelist 때문에 원본 exception message와 request context는 Sentry에서 볼 수 없어 진단은 exception type·stack frame·requestId·errorCode 및 안전한 구조화 로그에 의존한다. 실제 Sentry project의 저장·표시·alert, 조직 접근권한·보존정책, immutable release와 source context 연결은 test transport로 검증할 수 없으며 staging 확인이 필요하다. SDK를 올릴 때 event protocol과 callback 순서를 재검증해야 한다.
- 다음 작업: 사용자가 staging 배포 Secret으로 DSN·environment·release를 직접 주입한 뒤 controlled unexpected 5xx 한 건, expected 4xx 0건, tag·한글 UTF-8·source context·중복과 민감정보 비노출을 Sentry project에서 확인한다. 오류량과 비용을 검토하기 전까지 tracing·profiling·Sentry Logs는 계속 끈다.

## 2026-08-10 — Sentry 배포 환경변수와 Secret 경계 안내

<!-- codex-turn:019feabc-6dfe-79c3-8aca-2179089ddfc3 -->

- 날짜: 2026-08-10
- 브랜치: `main` (`0f0a1aa` 기준, commit·push 미수행)
- 작업 목표: 구현된 Sentry 연동을 실제 환경에서 활성화할 때 사용자가 설정해야 하는 값과 Runtime·CI Secret 경계를 명확히 안내한다.
- 변경 파일: 애플리케이션 코드와 설정은 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Runtime에는 `SENTRY_ENABLED=true`, 배포 Secret의 `SENTRY_DSN`, 환경 구분용 `SENTRY_ENVIRONMENT`, 배포마다 고정되는 `SENTRY_RELEASE`가 필요함을 현재 `application.yml`, `.env.example`, `build.gradle`과 README에서 재확인했다. 실제 비밀값으로 반드시 준비할 항목은 DSN이며 environment와 release는 운영 식별값이다.
- 구현 내용: `SENTRY_AUTH_TOKEN`은 source context 업로드를 사용할 때만 승인된 CI build Secret으로 설정하며 애플리케이션 Runtime에는 넣지 않는다. source context를 사용하지 않으면 이 값은 비워 두고도 오류 event 전송이 가능하다.
- 실행한 테스트와 결과: 코드 변경이 없는 설정 안내 작업이므로 `./gradlew clean test`는 다시 실행하지 않았다. 기존 최종 결과는 전체 41개 suite·295개 테스트 성공이며, 이번에는 환경변수 참조와 안전한 기본값을 정적으로 확인하고 문서 변경을 `git diff --check`로 검증한다.
- 유지한 계약: 실제 DSN·build 인증 값·Password·Access Token·Refresh Token·실제 Key·전체 MongoDB URI를 조회하거나 기록하지 않았다. Sentry 기본 비활성화, test profile 비활성화, Runtime DSN과 CI build 인증 값 분리 및 기존 API·JWT·로그 계약을 유지했다.
- 결정사항: 운영 오류 수집만 필요하면 Runtime 네 변수만 설정하고 CI 인증 값은 필요하지 않다. source context가 승인된 경우에만 별도의 CI 인증 값을 추가하며 실제 값은 채팅·저장소·일반 로그가 아닌 배포 또는 CI Secret 저장소에서 관리한다. Jira 변경과 Git commit·push는 수행하지 않았다.
- 위험 요소: enabled를 true로 바꾸고 DSN을 누락하거나 잘못 설정하면 event가 전송되지 않는다. release를 가변 문자열이나 매 기동 시각으로 만들면 동일 배포 오류 집계와 source context 연결이 불안정해질 수 있으며, CI 인증 값을 Runtime에 전달하면 불필요한 권한 노출이 생긴다.
- 다음 작업: staging에 Runtime 네 값을 주입하고 controlled 5xx가 정확히 한 건 수집되는지 확인한다. source context를 사용할 경우에만 CI Secret을 추가하고 업로드·release 연결을 별도로 검증한다.

## 2026-08-11 — Sentry 자동화·staging 테스트 절차 안내

<!-- codex-turn:019fee43-3e94-7b70-b775-c8a1aeae051d -->

- 날짜: 2026-08-11
- 브랜치: `main` (`4b0c762` 기준, commit·push 미수행)
- 작업 목표: 구현된 Sentry 연동을 사용자가 직접 검증할 수 있도록 외부 통신 없는 자동화 테스트와 실제 Sentry project를 확인하는 staging smoke test 절차를 구분해 안내한다.
- 변경 파일: 애플리케이션 코드와 설정은 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 자동화 단계에서는 `SentryCaptureIntegrationTests`만 실행해 expected Business 4xx event 0건, handled unexpected 5xx final event 1건, ERROR 로그 이후 중복 event 부재, Logback integration 비활성화와 최종 serialized event 전체의 sentinel 비노출을 검증하고 이후 전체 `clean test`로 회귀를 확인하는 순서를 정리했다.
- 구현 내용: 실제 project 단계에서는 staging Runtime에 enabled·DSN·environment·immutable release를 주입하고, controlled 5xx 한 건의 event 수·허용 tag·request/user/body 등 민감 context 부재를 확인하며 안전한 Validation 4xx 호출 후 event 수가 늘지 않는지 확인하는 checklist를 정리했다. Source context는 선택적으로 CI build 인증 값을 사용하는 별도 검증으로 분리했다.
- 구현 내용: 현재 `/test/sentry/business`와 `/test/sentry/unexpected`는 `src/test`의 중첩 Test Controller이므로 실제 애플리케이션 Runtime에는 노출되지 않는다. 실제 dashboard smoke test는 일반 production에 공개하지 않고 기본 비활성, staging 한정, 인증 또는 내부 접근으로 제한된 controlled trigger가 별도로 필요하다고 판단했다.
- 실행한 테스트와 결과: 절차 안내와 정적 확인 작업이므로 Gradle 테스트나 실제 외부 Sentry 전송은 실행하지 않았다. 기존 기준은 전체 41개 suite·295개 테스트 성공이며, 이번에는 테스트 클래스·환경변수 설정·Runtime Controller 범위를 정적으로 확인하고 문서 변경을 `git diff --check`로 검증한다.
- 유지한 계약: 실제 DSN·build 인증 값·Password·Access Token·Refresh Token·Authorization Header·개인정보·실제 Key·전체 MongoDB URI를 요청하거나 기록하지 않았다. test profile 외부 인프라 비호출, production 기본 Sentry 비활성화, 예상 밖 5xx 1건·expected 4xx 0건과 기존 API·JWT·로그 계약을 유지했다.
- 결정사항: 로직 검증은 test transport로 반복 가능하게 수행하고 실제 DSN 연결은 staging에서만 확인한다. test source endpoint를 Runtime endpoint로 오해하지 않으며, controlled error trigger는 사용자가 별도 구현을 요청하기 전에는 추가하지 않는다. Git commit·push와 Jira 변경은 수행하지 않았다.
- 위험 요소: 실제 5xx를 만들기 위해 MongoDB 장애나 정상 API 코드를 임의로 망가뜨리면 데이터·가용성에 영향을 줄 수 있다. 공개 smoke endpoint는 공격자가 오류 event와 비용을 증폭시킬 수 있으므로 production에 노출하면 안 되며, test transport 성공만으로 실제 DSN·네트워크·Sentry project 권한을 증명할 수는 없다.
- 다음 작업: 먼저 단독 통합 테스트와 전체 회귀 테스트를 실행한다. Sentry dashboard 연결까지 확인하려면 사용자의 별도 요청과 검토 후 staging 전용 controlled trigger를 구현하거나 이미 존재하는 안전한 내부 오류 유발 경로를 사용해 checklist를 수행한다.

## 2026-08-11 — Sentry 실제 연결 트리거의 배포 경계 결정

<!-- codex-turn:019fee4a-2be2-75d1-9647-ad668f34c08d -->

- 날짜: 2026-08-11
- 브랜치: `main` (`4b0c762` 기준, commit·push 미수행)
- 작업 목표: 실제 DSN으로 Sentry 표시를 확인하기 위한 로컬·staging 트리거의 역할과 production 배포 여부를 명확히 결정한다.
- 변경 파일: 애플리케이션 코드와 설정은 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 로컬 실제 연결 smoke test가 검증하는 범위를 SDK 초기화, DSN, outbound network, `beforeSend` 이후 event의 Sentry project 표시로 한정했다. `GlobalExceptionHandler`가 처리한 예상 밖 5xx의 정확히 한 건 수집, Logback·SDK integration 중복 부재, expected 4xx 비수집과 최종 event 민감정보 비노출은 외부 통신 없는 기존 `SentryCaptureIntegrationTests`가 담당하도록 검증 책임을 분리했다.
- 실행한 테스트와 결과: 코드 변경이 없는 배포 경계 검토이므로 Gradle 테스트와 실제 외부 Sentry 전송은 실행하지 않았다. 현재 test source의 `/test/sentry/*` Controller가 Runtime artifact에 포함되지 않는 점과 Sentry 설정·통합 테스트 범위를 정적으로 재확인했으며 기존 기준은 전체 41개 suite·295개 테스트 성공이다. 문서 변경은 `git diff --check`로 검증한다.
- 유지한 계약: 실제 DSN·build 인증 값·Password·Access Token·Refresh Token·Authorization Header·개인정보·실제 Key·전체 MongoDB URI를 조회하거나 기록하지 않았다. production 기본 비활성화, expected 4xx 0건·handled unexpected 5xx 1건·민감 sentinel 0건, 기존 API·JWT·로그 계약을 유지했다.
- 결정사항: 공개 오류 endpoint는 production에 배포하지 않는다. 실제 연결 확인은 공개 Controller 대신 `local` 또는 `staging` profile과 명시적인 opt-in property가 모두 설정될 때 기동 중 한 번만 capture하는 임시 startup runner를 우선하며, staging에서 확인한 뒤 제거해 production artifact에는 포함하지 않는다. 로컬 확인만으로 배포 Secret·배포망 outbound·release/source context 연결까지 증명할 수 없으므로 운영 전 staging smoke test를 별도로 수행한다. Git commit·push와 Jira 변경은 수행하지 않았다.
- 위험 요소: 오류 endpoint 또는 상시 활성 trigger를 production에 남기면 외부 호출자가 event와 비용을 증폭할 수 있다. startup runner도 반복 재기동 시 event를 만들 수 있으므로 staging 한정 opt-in과 일회성 확인 후 제거가 필요하며, runner는 MVC `GlobalExceptionHandler` 경로를 통과하지 않으므로 그 계약을 실제 연결 smoke 결과로 대체해서는 안 된다.
- 다음 작업: 사용자가 구현을 요청하면 기본 비활성·non-production 한정 one-shot trigger와 안전한 테스트를 추가하고 로컬 또는 staging에서 한 건을 확인한다. 확인 후 trigger를 제거한 production build로 배포하며, 실제 운영 활성화 전 배포 Secret·outbound network·environment·immutable release와 선택적 source context 연결을 staging에서 검증한다.

## 2026-08-11 — 임시 staging Sentry one-shot smoke trigger 구현

<!-- codex-turn:31dc08fe-df2c-4602-ba12-ddc4c3ef7559 -->

- 날짜: 2026-08-11
- 브랜치: `main` (`4b0c762` 기준, commit·push 미수행)
- 작업 목표: 공개 오류 endpoint나 정상 비즈니스 경로 훼손 없이 실제 staging DSN·배포망·Sentry project 연결을 한 번 확인하고, 확인 뒤 production 배포 전에 제거할 임시 trigger를 구현한다.
- 변경 파일: 신규 `src/main/java/web/tosunsaeng/identity/global/observability/SentryStagingSmokeTrigger.java`, `SentryEventSanitizer.java`, `application.yml`, `.env.example`, `README.md`, 신규 `src/test/java/web/tosunsaeng/identity/global/observability/SentryStagingSmokeTriggerConditionTests.java`, `SentryCaptureIntegrationTests.java`, `application-test.yml`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 변경했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: `SentryStagingSmokeTrigger`는 `ApplicationRunner`로 구현하고 `staging & !production & !prod` profile과 기본 false인 `app.sentry.smoke-trigger.enabled`가 모두 충족될 때만 bean이 생성되게 했다. 명시적으로 켠 경우 Sentry enabled, 정확한 `staging` environment, 비공백 DSN, sanitizer와 같은 문자 계약의 비공백 release를 fail-fast로 검사한다.
- 구현 내용: trigger는 공개 HTTP endpoint를 만들지 않고 전용 message 없는 `SentryStagingSmokeException`을 capture한다. scope를 먼저 비우고 허용된 `errorCode=SENTRY_STAGING_SMOKE_TEST`만 붙이며 `AtomicBoolean`으로 한 JVM에서 최대 한 번만 실행한다. SDK 비활성 또는 빈 event ID는 안전한 고정 오류로 기동을 중단하고, accepted event는 최대 5초 flush한 뒤 `sentry.smoke_trigger.requested` INFO에 event ID만 기록한다.
- 구현 내용: 비HTTP smoke event에는 `http.method` tag가 없어서 기존 `beforeSend`의 immutable method set에 null을 전달할 때 NPE가 발생하고 event가 폐기되는 문제를 첫 targeted test에서 발견했다. method가 있을 때만 allowlist를 검사하도록 `SentryEventSanitizer`를 null-safe하게 수정했으며, HTTP handler event의 기존 tag 계약은 그대로 유지했다.
- 구현 내용: 조건 테스트는 staging+opt-in에서만 bean이 생성되고 flag off, production profile, staging+production 복합 profile에서는 생성되지 않는지 검증한다. Sentry disabled, staging이 아닌 environment, 빈 DSN, 허용되지 않은 release는 외부 값 없이 고정 message로 context 기동에 실패하는지 확인한다. 통합 테스트는 trigger를 두 번 호출해도 test transport의 최종 serialized event가 정확히 한 건이며 민감 sentinel·request·user·message·HTTP status가 없고 전용 exception·errorCode만 남는지 검증한다.
- 실행한 테스트와 결과: 첫 targeted 실행은 smoke event의 누락된 HTTP method 때문에 `beforeSend`가 NPE를 내고 event를 drop하는 것을 확인해 실패했다. null-safe 수정 후 `SentryCaptureIntegrationTests`와 `SentryStagingSmokeTriggerConditionTests`가 성공했고, 최종 `./gradlew clean test`가 전체 42개 suite·304개 테스트, 실패·오류·건너뜀 0개로 성공했다. 실제 외부 Sentry와 Atlas는 호출하지 않았으며 `git diff --check`도 성공했다.
- 유지한 계약: 실제 DSN·build 인증 값·Password·Access Token·Refresh Token·Authorization Header·개인정보·실제 Key·전체 MongoDB URI를 소스·설정·문서·로그에 기록하지 않았다. 기본 Sentry 및 smoke trigger 비활성화, expected 4xx 0건·handled 5xx 1건·Sentry Logback 중복 0건·최종 민감 sentinel 0건과 기존 API·JWT·Mongo Transaction 계약을 유지했다.
- 결정사항: smoke trigger는 staging 연결성만 검증하며 `GlobalExceptionHandler`의 MVC 경로 검증을 대체하지 않는다. trigger가 포함된 artifact는 staging에만 임시 배포하고 Sentry project 확인 후 trigger 클래스·조건 테스트·설정·환경변수·README 안내를 제거한 새 artifact만 production으로 배포한다. capture 요청 로그는 SDK 수락을 의미할 뿐 실제 network 전달 성공은 Sentry project 화면에서 확인한다. Git commit·push와 Jira 조회·변경은 수행하지 않았다.
- 위험 요소: rolling deploy나 여러 staging 인스턴스가 함께 기동하면 인스턴스마다 한 건이 생성될 수 있다. `flush`는 대기만 수행하고 transport 결과를 반환하지 않으므로 event ID 로그만으로 실제 전달을 확정할 수 없으며, staging Secret·outbound network·project 권한·release/source context 연결은 아직 실제 환경에서 검증하지 않았다. 검증 뒤 임시 코드를 제거하지 않으면 이후 staging 재기동마다 추가 event가 생길 수 있다.
- 다음 작업: staging 배포 Secret에 실제 DSN을 직접 주입하고 `SPRING_PROFILES_ACTIVE=staging`, `SENTRY_ENABLED=true`, `SENTRY_ENVIRONMENT=staging`, immutable `SENTRY_RELEASE`, `SENTRY_SMOKE_TRIGGER_ENABLED=true`로 임시 artifact를 한 번 기동한다. Sentry project에서 event ID·전용 exception·environment·release·errorCode와 민감정보 부재를 확인한 뒤 smoke trigger 관련 코드·테스트·설정·문서를 제거하고 전체 테스트를 다시 실행해 production artifact를 준비한다.

## 2026-08-11 — 임시 staging Sentry trigger 구현 기록 보완

<!-- codex-turn:019fee4e-27ba-7162-8b87-95165c3a83d7 -->

- 날짜: 2026-08-11
- 브랜치: `main` (`4b0c762` 기준, commit·push 미수행)
- 작업 목표: 실제 staging 연결 확인 뒤 제거할 Sentry one-shot trigger 구현과 검증 결과를 현재 turn 식별자로 기록하고 CURRENT_STATE를 최종 상태와 일치시킨다.
- 변경 파일: 신규 `SentryStagingSmokeTrigger.java`, `SentryEventSanitizer.java`, `application.yml`, `.env.example`, `README.md`, 신규 `SentryStagingSmokeTriggerConditionTests.java`, `SentryCaptureIntegrationTests.java`, `application-test.yml`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`가 이번 작업 범위다. 이 보완에서는 기존 WORKLOG 항목을 수정하거나 삭제하지 않았다.
- 구현 내용: 공개 endpoint 없이 `staging & !production & !prod` profile과 기본 false opt-in flag를 모두 요구하는 `ApplicationRunner`를 추가했다. 명시적으로 활성화된 경우에만 안전한 설정을 fail-fast로 확인하고 message 없는 전용 exception과 허용된 errorCode를 JVM 기동당 최대 한 번 capture하며, SDK 수락 후 제한된 flush와 event ID INFO만 남긴다.
- 구현 내용: 비HTTP smoke event에 HTTP method tag가 없는 경우 `beforeSend` allowlist가 null을 처리하지 못해 event를 폐기하던 문제를 발견하고 null-safe하게 수정했다. test transport의 최종 serialized event에서 한 건 수집, 민감 sentinel 부재, request·user·message 부재와 전용 exception·errorCode 보존을 확인했다.
- 실행한 테스트와 결과: 첫 targeted test에서 `beforeSend` null 처리 실패를 재현한 뒤 수정했으며 관련 테스트가 성공했다. 최종 `./gradlew clean test`는 전체 42개 suite·304개 테스트, 실패·오류·건너뜀 0개로 성공했고 `git diff --check`도 성공했다. 실제 외부 Sentry나 Atlas는 호출하지 않았다.
- 유지한 계약: 실제 DSN, 인증 값, Password, Access Token, Refresh Token, Authorization Header, 개인정보, 실제 Key와 전체 MongoDB URI를 기록하지 않았다. Sentry와 smoke trigger 기본 비활성화, production profile 차단, expected 4xx 비수집·handled 5xx 단일 Issue·민감정보 최종 정제와 기존 API·JWT·Transaction 계약을 유지했다.
- 결정사항: trigger가 포함된 artifact는 staging 확인 전용이며 production으로 승격하지 않는다. 실제 project에서 event를 확인한 직후 임시 trigger 코드·조건 테스트·설정·환경변수·README 안내를 제거하고 새 production artifact를 만든다. 요청 로그는 실제 전달 성공을 보증하지 않으므로 Sentry project 화면을 최종 기준으로 사용한다. Git commit·push와 Jira 조회·변경은 수행하지 않았다.
- 위험 요소: staging 인스턴스가 여러 개면 인스턴스마다 event 한 건이 생성될 수 있고, 네트워크·project 권한·release/source context 연결은 아직 실제 환경에서 확인하지 않았다. 임시 코드를 제거하지 않으면 이후 staging 재기동 때 추가 event가 생길 수 있다.
- 다음 작업: 사용자가 staging Secret과 non-secret 환경 식별값을 배포 환경에 직접 설정해 전용 event와 허용 필드만 표시되는지 확인한다. 확인 결과를 받은 뒤 임시 trigger 관련 파일과 설정을 제거하고 전체 테스트를 다시 실행한다.

## 2026-08-11 — staging Sentry smoke trigger 테스트 절차 안내

<!-- codex-turn:019fee6e-099f-7121-864d-e8520a46a524 -->

- 날짜: 2026-08-11
- 브랜치: `main` (`4b0c762` 기준, commit·push 미수행)
- 작업 목표: 구현된 임시 staging Sentry one-shot trigger를 외부 민감값 노출 없이 자동화와 실제 배포 환경에서 검증하는 순서, 성공 기준과 실패 진단 기준을 안내한다.
- 변경 파일: 애플리케이션 코드와 설정은 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 1단계는 `SentryCaptureIntegrationTests`와 `SentryStagingSmokeTriggerConditionTests` 단독 실행으로 final transport 한 건·민감 sentinel 부재·production profile 차단·fail-fast 설정을 확인하고, 2단계는 전체 `clean test`로 회귀를 확인하도록 정리했다.
- 구현 내용: 실제 연결 단계는 DSN을 명령줄·저장소·대화에 적지 않고 staging 배포 Secret으로 주입하며, `staging` profile, Sentry enabled, 정확한 staging environment, immutable release와 smoke flag를 설정해 가능하면 단일 인스턴스를 기동하도록 정리했다. HTTP endpoint 호출은 필요하지 않고 startup runner가 기동 중 한 번 capture한다.
- 구현 내용: 애플리케이션 로그의 `sentry.smoke_trigger.requested` event와 event ID를 찾은 뒤 Sentry project에서 동일 event, `SentryStagingSmokeException`, staging environment, 배포 release, `SENTRY_STAGING_SMOKE_TEST` errorCode와 request·user·message 등 민감 context 부재를 확인한다. 로그는 SDK capture 수락만 의미하므로 dashboard 표시를 실제 성공 기준으로 사용한다.
- 실행한 테스트와 결과: 이번 작업은 기존 구현의 테스트 절차 안내이므로 Gradle 테스트와 실제 외부 전송을 새로 실행하지 않았다. 직전 최종 기준은 `./gradlew clean test` 전체 42개 suite·304개 테스트, 실패·오류·건너뜀 0개 성공이며 이번 문서 변경은 `git diff --check`로 검증한다.
- 유지한 계약: 실제 DSN·build 인증 값·Password·Access Token·Refresh Token·Authorization Header·개인정보·실제 Key·전체 MongoDB URI를 요청하거나 기록하지 않았다. test profile 외부 통신 금지, production trigger 차단, Sentry final event allowlist와 기존 API·JWT·로그·Transaction 계약을 유지했다.
- 결정사항: 실제 연결 확인에는 `SENTRY_AUTH_TOKEN`이 필요하지 않으며 source context를 승인한 CI build에서만 별도로 사용한다. 성공 확인 직후 smoke flag를 false로 되돌려 재기동 event를 막고, 임시 trigger 관련 코드·조건 테스트·설정·환경변수·README 안내를 제거한 새 artifact만 production으로 배포한다. Git commit·push와 Jira 조회·변경은 수행하지 않았다.
- 위험 요소: staging 인스턴스 또는 재기동 횟수만큼 event가 생성될 수 있다. 요청 로그가 있지만 dashboard에 event가 없다면 DSN 대상 project, outbound network, firewall·proxy, rate limit과 SDK diagnostic을 staging 내부에서 확인해야 하며 실제 DSN을 일반 로그나 대화에 붙여 넣으면 안 된다.
- 다음 작업: 사용자가 자동화 테스트를 실행한 뒤 staging 배포 환경에서 one-shot event를 확인한다. 확인된 event ID 자체를 공유할 필요 없이 성공 여부와 표시된 허용 필드만 알려주면 임시 trigger 제거 작업을 진행한다.

## 2026-08-11 — 로컬 Sentry smoke event 미표시 원인 진단

<!-- codex-turn:019fee75-07be-7351-b972-5ec96fbe9a58 -->

- 날짜: 2026-08-11
- 브랜치: `main` (`4b0c762` 기준, commit·push 미수행)
- 작업 목표: 사용자가 smoke flag를 true로 설정해 로컬 기동했지만 Sentry event가 표시되지 않은 원인을 현재 trigger 조건과 작업 트리 설정을 기준으로 진단한다.
- 변경 파일: 애플리케이션 코드와 설정은 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 사용자가 로컬 시험을 위해 변경한 `application.yml`의 smoke fallback 값은 덮어쓰지 않았으며 WORKLOG의 과거 기록도 수정하거나 삭제하지 않았다.
- 구현 내용: `SentryStagingSmokeTrigger`가 `staging & !production & !prod` profile과 smoke enabled property를 동시에 요구하므로 기본 또는 local profile에서는 smoke flag가 true여도 bean이 생성되지 않는 것을 정적으로 확인했다. 또한 Runtime Sentry enabled 기본값은 false, environment 기본값은 local이며 trigger가 생성될 때에는 비공백 DSN과 release까지 별도로 필요하다.
- 구현 내용: 로컬 머신에서 실제 연결을 시험하려면 IDE의 active profiles 또는 `SPRING_PROFILES_ACTIVE`를 staging으로 지정하고, smoke flag 외에도 Sentry enabled, 정확한 staging environment, 비공백 release와 로컬 비추적 또는 Secret 경로의 DSN을 모두 설정해야 한다. 성공 여부는 active staging profile 로그, `sentry.smoke_trigger.requested` event와 Sentry project 표시 순서로 판단한다.
- 실행한 테스트와 결과: 코드 변경이 없는 설정 진단이므로 Gradle 테스트와 실제 외부 Sentry 전송은 실행하지 않았다. 직전 기준은 전체 42개 suite·304개 테스트 성공이며, 현재 annotation과 `application.yml`을 정적으로 확인하고 문서 변경은 `git diff --check`로 검증한다.
- 유지한 계약: 실제 DSN·build 인증 값·Password·Access Token·Refresh Token·Authorization Header·개인정보·실제 Key·전체 MongoDB URI를 요청하거나 기록하지 않았다. production profile 차단, 공개 endpoint 부재, Sentry final event 정제와 기존 API·JWT·로그 계약을 유지했다.
- 결정사항: 이번 미표시는 우선적으로 trigger 실패가 아니라 profile 조건 미충족으로 판단한다. `SENTRY_ENABLED`와 `SENTRY_SMOKE_TRIGGER_ENABLED`은 서로 다른 플래그이며 두 값만 true여도 active staging profile·staging environment·DSN·release가 없으면 정상 capture 조건이 완성되지 않는다. Git commit·push와 Jira 조회·변경은 수행하지 않았다.
- 위험 요소: 현재 작업 트리에서 사용자가 smoke fallback을 true로 바꿔 README·`.env.example`의 기본 false와 불일치한다. staging profile로 이후 기동하면 매 JVM 기동당 event가 생성될 수 있으므로 확인 직후 false로 복구하거나 임시 trigger를 제거해야 한다. 요청 로그가 있는데 dashboard에만 없다면 그때는 DSN 대상 project, environment filter, outbound network·방화벽·proxy·rate limit을 별도로 확인해야 한다.
- 다음 작업: 사용자는 민감값을 공유하지 않고 IDE에서 active profile과 네 개의 non-secret 동작 조건 및 DSN Secret 주입 여부를 확인해 다시 기동한다. active profile staging 및 smoke 요청 로그 유무를 알려주면, 요청 로그가 없는 경우 설정 경계를 이어서 진단하고 요청 로그가 있는 경우 Sentry project·network 경계를 진단한다.

## 2026-08-11 — 로컬 기동 로그 기반 Sentry trigger 미실행 확정

<!-- codex-turn:019fee7a-98bb-72f3-b465-d886a6101354 -->

- 날짜: 2026-08-11
- 브랜치: `main` (`4b0c762` 기준, commit·push 미수행)
- 작업 목표: 사용자가 제공한 로컬 애플리케이션 기동 로그로 staging Sentry smoke event가 표시되지 않은 원인을 확정하고 다음 실행 설정을 안내한다.
- 변경 파일: 애플리케이션 코드와 설정은 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 기동 로그가 active Spring profile 없이 default profile로 실행됐음을 확인했다. `SentryStagingSmokeTrigger`는 staging profile을 필수로 요구하므로 bean이 생성되지 않았고, 예상되는 `sentry.smoke_trigger.requested` event도 없어서 Sentry SDK 전송 또는 dashboard 표시 단계까지 도달하지 않았다고 판정했다.
- 구현 내용: IntelliJ Spring Boot 실행 설정의 Active profiles 또는 환경변수로 staging profile을 지정하고 Sentry enabled·smoke enabled·staging environment·비공백 release와 DSN Secret 주입을 모두 확인해야 한다. 다음 실행에서는 시작 로그의 active staging profile과 smoke 요청 event만 최소 발췌해 확인하도록 범위를 제한했다.
- 실행한 테스트와 결과: 코드 변경이 없는 로그 진단이므로 Gradle 테스트와 실제 외부 전송은 실행하지 않았다. 직전 기준은 전체 42개 suite·304개 테스트 성공이며 이번 문서 변경은 `git diff --check`로 검증한다.
- 유지한 계약: 제공된 로그의 데이터베이스 계정 식별자나 cluster endpoint·topology를 WORKLOG에 복사하지 않았고 실제 DSN·Password·Token·실제 Key·전체 MongoDB URI도 기록하지 않았다. production profile 차단, 공개 endpoint 부재와 Sentry final event 정제 계약을 유지했다.
- 결정사항: 이번 실행은 smoke 전송 실패가 아닌 조건부 bean 미생성이다. 정상 재시험의 첫 성공 기준은 시작 로그의 active staging profile이며, 두 번째 기준은 `sentry.smoke_trigger.requested` event다. 이 두 번째 기준까지 충족된 뒤에만 Sentry project·environment filter·network 문제를 조사한다. Git commit·push와 Jira 조회·변경은 수행하지 않았다.
- 위험 요소: 전체 MongoDB driver INFO 로그에는 비밀번호 원문이 숨겨져 있어도 계정 식별자와 cluster topology 같은 인프라 정보가 포함될 수 있다. 후속 공유에서는 전체 기동 로그 대신 active profile 한 줄, smoke 요청 event 한 줄 또는 안전한 고정 startup 오류만 제공해야 한다.
- 다음 작업: 사용자는 IntelliJ 실행 설정에 staging active profile과 필요한 Sentry 환경변수를 설정해 한 번 재기동한다. active staging profile과 smoke 요청 event가 확인되면 Sentry project에서 동일 event ID와 허용 필드를 확인하고 즉시 smoke flag를 false로 되돌린다.

## 2026-08-11 — staging smoke trigger release 검증 실패 진단

<!-- codex-turn:019fee80-d628-71f2-abe3-355deaf2c27f -->

- 날짜: 2026-08-11
- 브랜치: `main` (`4b0c762` 기준, commit·push 미수행)
- 작업 목표: staging profile을 적용한 재실행에서 `SentryStagingSmokeTrigger` bean 생성이 실패한 원인을 제공된 startup 오류로 진단한다.
- 변경 파일: 애플리케이션 코드와 설정은 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 재실행 로그에서 active profile `staging`과 `sentryStagingSmokeTrigger` constructor 진입을 확인했다. 가장 내부 원인은 `sentry.release`가 비어 있거나 허용되지 않는 문자를 포함해 고정 release validation 오류가 발생한 것이며 Sentry capture 이전에 의도적으로 기동을 중단한 fail-fast 동작이다.
- 구현 내용: constructor 검증 순서상 Sentry enabled, 정확한 staging environment와 비공백 DSN 검사는 이미 통과했다. IntelliJ Runtime 환경변수에 공백 없이 허용 문자만 사용하는 non-secret `SENTRY_RELEASE` 식별자를 추가하면 다음 단계로 진행되며 소스의 기본값을 직접 수정할 필요는 없다.
- 실행한 테스트와 결과: 코드 변경이 없는 제공 로그 진단이므로 Gradle 테스트와 실제 Sentry 전송은 실행하지 않았다. 직전 자동화 기준은 전체 42개 suite·304개 테스트 성공이며 이번 문서 변경은 `git diff --check`로 검증한다.
- 유지한 계약: 제공된 전체 command line과 database driver 로그의 경로·계정 식별자·cluster endpoint를 기록에 복사하지 않았고 실제 DSN·Password·Token·실제 Key·전체 MongoDB URI도 기록하지 않았다. staging-only trigger, fail-fast와 final event 정제 계약을 유지했다.
- 결정사항: 이번 오류는 구현 결함이 아니라 필수 release 식별자 누락 또는 형식 불일치다. release는 비밀값이 아니지만 event와 실행 build를 연결해야 하므로 공백 없는 Git SHA 또는 image tag 계열의 안정된 값으로 지정한다. Git commit·push와 Jira 조회·변경은 수행하지 않았다.
- 위험 요소: release에 공백이나 허용되지 않는 문자를 넣으면 동일 오류가 반복된다. 현재 trigger는 JVM 기동마다 한 번 실행되므로 release를 고친 다음부터는 재기동 횟수만큼 event가 생길 수 있고 확인 직후 smoke flag를 false로 복구해야 한다. 전체 startup 로그 공유는 인프라 식별자를 노출할 수 있으므로 다음에는 smoke event 또는 가장 안쪽 고정 오류 한 줄만 공유한다.
- 다음 작업: 사용자는 IntelliJ 환경변수에 안전한 `SENTRY_RELEASE` 값을 추가하고 한 번 재실행한다. `sentry.smoke_trigger.requested` event가 출력되면 Sentry project의 동일 event ID·environment·release·errorCode와 민감 context 부재를 확인하고 smoke flag를 false로 되돌린다.

## 2026-08-11 — Sentry smoke capture 후 project 미표시 진단

<!-- codex-turn:1ea5287c-a110-45d0-a234-c158b2470485 -->

- 날짜: 2026-08-11
- 브랜치: `main` (`4b0c762` 기준, commit·push 미수행)
- 작업 목표: active staging profile에서 smoke capture 요청이 성공했지만 Sentry 화면에 event가 표시되지 않는 원인 범위와 안전한 다음 진단 절차를 확정한다.
- 변경 파일: 애플리케이션 코드와 설정은 변경하지 않고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 사용자가 로컬 시험 중 변경한 `application.yml` fallback은 덮어쓰지 않았고 WORKLOG의 과거 기록도 수정하거나 삭제하지 않았다.
- 구현 내용: 제공된 최소 성공 지표에서 active staging profile, 정상 애플리케이션 기동과 비어 있지 않은 event ID를 포함한 `sentry.smoke_trigger.requested`를 확인해 profile·필수 설정·trigger 실행·`beforeSend`·SDK capture 단계가 모두 통과했음을 확정했다.
- 구현 내용: SDK 8.42.0 소스와 현재 trigger를 확인해 capture가 반환하는 event ID는 로컬 SDK 수락을 나타내고 5초 `flush`는 transport 성공 결과를 반환하지 않는다는 경계를 확인했다. 따라서 남은 원인은 DSN 대상 project 또는 Sentry UI의 project·Issues·environment·시간 필터 불일치와 outbound network·proxy·방화벽·rate limit·ingest 오류다.
- 구현 내용: Runtime event의 대상은 Gradle Sentry plugin의 source context용 organization/project 설정이 아니라 DSN이 결정한다. UI 확인 뒤에도 미표시라면 실제 DSN을 로그로 출력하지 않도록 `SENTRY_DEBUG=true`와 `SENTRY_DIAGNOSTIC_LEVEL=ERROR`만 임시 적용해 Sentry transport 오류를 한 번 확인하도록 정리했다.
- 실행한 테스트와 결과: 코드 변경이 없는 제공 로그·설정·SDK 동작 진단이므로 Gradle 테스트와 실제 외부 전송을 새로 실행하지 않았다. 직전 자동화 기준은 전체 42개 suite·304개 테스트 성공이며 문서 변경은 `git diff --check`로 검증한다.
- 유지한 계약: 실제 DSN·Password·Token·실제 Key·전체 MongoDB URI와 제공 로그의 인프라 식별자를 기록하거나 재출력하지 않았다. production profile 차단, 공개 endpoint 부재, final event allowlist와 기존 API·JWT·MongoDB 계약을 변경하지 않았다.
- 결정사항: 현재 결과를 전송 성공이나 구현 실패로 단정하지 않고 capture 이후 transport/UI 경계의 미확정 상태로 본다. Sentry Logs가 비활성화돼 있으므로 Logs가 아니라 Issues에서 확인하며, diagnostic은 DSN이 INFO로 출력될 수 있는 DEBUG level 대신 ERROR level로 제한한다. Git commit·push와 Jira 조회·변경은 수행하지 않았다.
- 위험 요소: 동일 DSN처럼 보여도 project별 DSN의 project ID가 다르면 event는 다른 project로 전송된다. smoke fallback이 현재 true라 재기동마다 한 건씩 요청될 수 있고, 전체 SDK debug 로그를 공유하면 DSN이 노출될 수 있다.
- 다음 작업: 사용자는 Sentry에서 올바른 organization/project, Issues 화면, staging 또는 전체 environment와 최근 시간 범위를 확인하고 event ID 또는 전용 errorCode로 검색한다. 계속 미표시라면 제한된 diagnostic 재기동 후 Sentry transport ERROR 한 줄만 민감값 없이 공유하며, event 확인 즉시 smoke flag를 false로 복구하고 임시 trigger 제거를 진행한다.

## 2026-08-11 — Sentry 미표시 진단 기록 보완

<!-- codex-turn:019fee83-6063-7372-82d8-8c970b2a64ab -->

- 날짜: 2026-08-11
- 브랜치: `main` (`4b0c762` 기준, commit·push 미수행)
- 작업 목표: 이번 turn에서 확정한 Sentry smoke capture 이후의 UI·transport 진단 결과를 지정된 작업 식별자로 기록하고 현재 상태를 동기화한다.
- 변경 파일: 애플리케이션 코드와 설정은 변경하지 않고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. WORKLOG의 과거 항목은 수정하거나 삭제하지 않았다.
- 구현 내용: active staging profile과 비어 있지 않은 event ID는 trigger 및 로컬 SDK capture 통과만 증명하며 Sentry 서버 수신을 증명하지 않는다고 판정했다. Runtime 대상 project는 Gradle plugin 설정이 아닌 주입된 DSN이 결정하므로 올바른 project·Issues 화면·environment·시간 범위를 우선 확인하도록 안내했다.
- 구현 내용: UI 확인 후에도 미표시라면 `SENTRY_DEBUG=true`와 `SENTRY_DIAGNOSTIC_LEVEL=ERROR`로 한 번만 재기동해 transport 오류를 최소 진단한다. INFO/DEBUG diagnostic에서 DSN이 출력될 수 있으므로 ERROR 이외의 전체 diagnostic 공유를 금지하고 민감 URL을 제거한 Sentry 오류 한 줄만 후속 입력으로 사용한다.
- 실행한 테스트와 결과: 코드 변경이 없는 로그·SDK 동작 진단 및 문서 보완이므로 Gradle 테스트와 외부 전송은 새로 실행하지 않았다. 직전 기준은 전체 42개 suite·304개 테스트 성공이며 이번 문서 변경은 `git diff --check`로 검증한다.
- 유지한 계약: 실제 DSN·Password·Token·Key·전체 MongoDB URI와 인프라 식별자를 기록하지 않았다. staging-only trigger, production profile 차단, final event allowlist와 기존 API·JWT·MongoDB 계약을 변경하지 않았다.
- 결정사항: 현재 상태는 capture 성공·server 수신 미확정이다. Sentry Logs가 비활성화돼 있으므로 event는 Issues에서 찾고, transport 오류가 없으면 DSN 대상 project와 UI 필터를 우선 재검증한다. Git commit·push와 Jira 조회·변경은 수행하지 않았다.
- 위험 요소: smoke fallback이 현재 true라 재기동마다 event가 요청될 수 있고, diagnostic level을 낮추거나 전체 로그를 공유하면 DSN 또는 인프라 정보가 노출될 수 있다.
- 다음 작업: 제한된 UI 및 ERROR diagnostic 확인 결과에 따라 DSN project 불일치 또는 network·ingest 오류를 좁힌다. event가 확인되면 smoke flag를 false로 복구하고 임시 trigger 제거 및 전체 테스트를 진행한다.

## 2026-08-11 — Sentry 수신 확인 후 임시 smoke trigger 제거

<!-- codex-turn:019fee8b-66e3-73e0-9697-b4f873e3ceef -->

- 날짜: 2026-08-11
- 브랜치: `main` (`4b0c762` 기준, commit·push 미수행)
- 작업 목표: 사용자가 실제 Sentry project 수신을 확인한 one-shot 검증 장치를 제거하고 errors-only Sentry 연동의 안전한 기본 상태로 복원한다.
- 변경 파일: 임시 `src/main/java/web/tosunsaeng/identity/global/observability/SentryStagingSmokeTrigger.java`와 `src/test/java/web/tosunsaeng/identity/global/observability/SentryStagingSmokeTriggerConditionTests.java`를 제거했다. `.env.example`, `README.md`, `src/main/resources/application.yml`, `src/test/resources/application-test.yml`, `SentryCaptureIntegrationTests.java`의 smoke 전용 변경을 복원했으며 `SentryEventSanitizer.java`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Sentry project에서 실제 event 수신이 확인됐으므로 startup runner, 전용 exception·errorCode·flush, profile/flag fail-fast 조건과 관련 조건·transport 테스트를 모두 제거했다. smoke 환경변수와 README 운영 절차도 제거해 임시 기능이 Runtime과 test artifact에 남지 않게 했다.
- 구현 내용: 로컬 시험 중 바뀐 `application.yml`의 Sentry fallback을 `enabled=false`, `environment=local`로 복원했다. test profile의 smoke override도 제거했으며 main/test 설정과 문서·환경변수 예시에서 smoke 식별자가 0건임을 정적으로 확인했다.
- 구현 내용: smoke 통합 테스트에서 발견한 비HTTP event의 누락된 `http.method`를 안전하게 처리하는 `SentryEventSanitizer` null check는 일반 event 정제 안정성에 유효하므로 유지했다. 기존 errors-only capture, expected 4xx 제외, handled 5xx 단일 event와 민감정보 whitelist 계약은 변경하지 않았다.
- 실행한 테스트와 결과: 첫 `./gradlew clean test`는 sandbox의 Gradle cache lock 접근 제한으로 실행 전에 중단됐고, 승인된 재실행은 BUILD SUCCESSFUL이었다. 최종 결과는 41개 suite·295개 테스트, 실패·오류·건너뜀 0개이며 실제 Atlas나 외부 Sentry는 테스트에서 호출하지 않았다. smoke 식별자 정적 검색 0건과 `git diff --check`도 성공했다.
- 유지한 계약: 실제 DSN·Password·Access Token·Refresh Token·인증 값·실제 Key·전체 MongoDB URI를 소스·문서·로그에 기록하지 않았다. Sentry 기본 비활성화, test 외부 통신 금지, production에 임시 trigger 미포함과 기존 API·JWT·MongoDB Transaction 계약을 유지했다.
- 결정사항: 실제 연결 확인을 마친 one-shot 검증 코드는 재사용 목적으로 보존하지 않고 제거한다. Runtime Sentry 연동과 final event sanitizer만 유지하며 source context용 build 인증 값은 계속 승인된 CI에만 둔다. Git commit·push와 Jira 조회·변경은 수행하지 않았다.
- 위험 요소: 저장소 밖 IntelliJ 실행 설정에 임시 staging profile·smoke 또는 Sentry diagnostic 환경변수가 남아 있을 수 있으며 Codex가 이를 자동 복원하지 못한다. 배포망·alert·보존정책과 CI source context는 로컬 project 수신 확인으로 검증되지 않았다.
- 다음 작업: 사용자는 IntelliJ Run Configuration에서 임시 staging·smoke·diagnostic 설정을 제거한다. 이후 staging에서는 trigger 없는 errors-only artifact로 handled 5xx 한 건·expected 4xx 0건·중복 및 민감정보 부재를 검증하고 운영 활성화 범위를 결정한다.

## 2026-08-15 — Quality review 선택 동의 구현 계획의 main hotfix 적용 분석

<!-- codex-turn:01a004a3-4aa2-7a71-a250-e8ff5ae61949 -->

- 날짜: 2026-08-15
- 브랜치: `hotfix/quality-review-consent` (`b6eb73e`, `origin/main`과 동일, commit·push 미수행)
- 작업 목표: `/Users/msde76/identity/docs/contracts/quality-review-consent-implementation-plan.md`를 현재 `main` 기반 hotfix 코드와 대조해 실제 구현 순서, 수정 파일, 상태 전이, 하위 호환, 테스트 범위와 선결 결정을 제시한다.
- 변경 파일: 애플리케이션 코드는 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Guest POST와 인증된 동의 PUT·GET에만 `isQualityReviewConsented`와 `qualityReviewConsentVersion` 계약을 추가하고 LOCAL signup request와 프로필 응답은 유지하는 범위를 확인했다. 누락은 false, true만 현재 version exact match, false는 stale version으로 차단하지 않는 호환 규칙을 확정했다.
- 구현 내용: `ConsentPolicy`·`UserConsents`·`User`·`UserConsentService` 흐름을 따라 false→true, true→true current no-op, old→current 갱신, true→false 정리와 false→false no-op을 immutable 결과로 처리하고, 실제 변경 시에만 기존 ACTIVE+`updatedAt` CAS로 embedded `consents` 전체를 교체하는 방식을 정리했다.
- 구현 내용: 계획서 예상 목록 외에 `UserFactory`와 `UserErrorStatus`가 필수 수정이며 `UserRepositoryCustomImpl`은 현재 전체 embedded object 저장 구조라 실행 코드 변경 없이 회귀 테스트만 보강할 수 있음을 확인했다. 필수 정책 factory와 분리된 선택 정책 응답 factory로 유효 동의만 `consented=true`, `requiresConsent=false`를 만들고 PUT 응답에는 저장 snapshot 세 필드를 추가하도록 설계했다.
- 실행한 테스트와 결과: 첫 `./gradlew clean test`는 sandbox의 Gradle cache lock 접근 제한으로 실행 전에 중단됐다. 승인된 재실행은 BUILD SUCCESSFUL이며 전체 41개 suite·295개 테스트, 실패·오류·건너뜀 0개였다. 실제 Atlas, OAuth Provider, Sentry 또는 외부 데이터 소유 서비스는 호출하지 않았다.
- 유지한 계약: UUID 실제 userId와 JWT 계약을 변경하지 않고 외부 Request Body에 userId나 요청 시각을 추가하지 않는다. Identity 밖의 답안·음성·시험 데이터 코드를 추가하지 않으며 실제 품질 검토 이용은 별도 outbox·consumer·보존 정책 전 활성화하지 않는다. Secret, Token, Password, 실제 Key와 전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 원본 계획 문서는 구현 시 hotfix의 `docs/contracts`에 포함한다. Quality review request 두 필드는 호환 기간에는 OpenAPI required로 강제하지 않고, false snapshot은 false/null/null로 정규화하며 같은 상태 반복 요청은 동의 시각·User `updatedAt`·저장을 변경하지 않는다. 프로필과 LOCAL signup request에는 신규 외부 필드를 추가하지 않는다. Jira 키가 없어 Jira 조회·댓글·상태 변경은 수행하지 않았다.
- 위험 요소: 계획서가 version 문자열의 정확한 최대 길이와 허용 문자를 고정하지 않아 구현 전에 프론트와 합의가 필요하다. 현재 회원 탈퇴 tombstone은 `consents`를 보존하므로 quality review true 상태를 탈퇴 시 자동 철회할지 감사 snapshot으로 유지할지 결정해야 한다. snapshot만으로 철회 이력을 증명할 수 없고 외부 데이터 이용 중지는 lifecycle consumer 없이는 보장되지 않는다.
- 다음 작업: 버전 형식과 회원 탈퇴 처리 원칙을 확정한 뒤 설정·오류 코드·DTO, 도메인 전이, 서비스·API, OpenAPI·README·테스트 순으로 구현하고 `./gradlew clean test`, `git diff --check`, `origin/main...HEAD` 범위 검사를 수행한다.

## 2026-08-15 — Quality review 선택 동의 main hotfix 구현

<!-- codex-turn:01a00528-3a0c-7094-8dbf-2ae9c7b28a5f -->

- 날짜: 2026-08-15
- 브랜치: `hotfix/quality-review-consent` (`b6eb73e`, `origin/main` 기반, commit·push 미수행)
- 작업 목표: 승인된 구현 계획에 따라 Guest 생성과 인증된 동의 PUT·GET에 Quality review 선택 동의·철회 상태, version 정책, 하위 호환, OpenAPI·문서와 회귀 테스트를 구현한다.
- 변경 파일: `.env.example`, `README.md`, `docs/contracts/quality-review-consent-implementation-plan.md`, `src/main/resources/application.yml`, Guest request·service·controller, User consent policy·factory·entity·service·request·response·controller·오류 코드 파일을 변경했다. `UserConsentsTests.java`를 추가하고 Guest·User consent·Mongo mapping·OpenAPI·설정 관련 기존 테스트를 갱신했으며 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 최신화했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 외부 필드 `isQualityReviewConsented`, `qualityReviewConsentVersion`을 Guest POST와 동의 PUT에 추가했다. 호환 기간에는 누락 Boolean을 false로 정규화하고 version 누락을 허용하며, true만 current server version exact match를 요구하고 false 선택·철회는 stale version으로 차단하지 않는다. 제공된 version은 trim 후 최대 100자와 영문·숫자·점·밑줄·하이픈 형식을 검증한다.
- 구현 내용: `UserConsents` embedded model에 Quality review boolean·version·서버 `Instant`를 추가했다. false→true, true/current→true no-op, true/old→true/current 갱신, true→false의 false/null/null 정리, false→false no-op을 immutable 결과로 처리하고 같은 상태에서는 기존 instance를 반환한다. LOCAL signup은 false/null/null, Guest는 요청 선택 상태로 생성하며 기존 Mongo 문서의 누락 필드는 false/null/null로 읽는다.
- 구현 내용: 실제 변경일 때만 User `updatedAt`을 바꾸고 기존 ACTIVE+`updatedAt` CAS와 embedded `consents` 전체 교체를 재사용했다. 철회 후 과거 Quality review version/time을 남기지 않으며 저장 실패·동시 탈퇴의 기존 오류 정책을 유지한다. 로그에서는 기존 동의 시각 field를 제거해 userId·provider·outcome만 사용한다.
- 구현 내용: GET 응답에 `qualityReview`를 추가해 저장 true·current version·동의 시각을 모두 만족할 때만 현재 `consented=true`로 계산하고 선택 정책의 `requiresConsent`는 항상 false로 유지했다. PUT 응답에는 저장된 Quality review 상태·version·시각을 추가했으며 LOCAL signup request와 프로필 응답은 변경하지 않았다. 계획 원문, README, 환경변수와 Controller OpenAPI 예시·nullable/required schema를 동기화했다.
- 실행한 테스트와 결과: 최초 `compileTestJava`는 변경된 constructor 호출부 19곳이 남아 실패했고 모두 갱신했다. 첫 targeted 실행은 기존 OpenAPI·record component·설정 기대값 5건이 새 계약과 달라 실패했으며 테스트와 계약을 동기화했다. 이후 targeted 테스트가 성공했고 최종 `./gradlew clean test`는 BUILD SUCCESSFUL, 전체 42개 suite·319개 테스트, 실패·오류·건너뜀 0개였다. `git diff --check`도 성공했으며 실제 Atlas, OAuth Provider, Sentry 또는 외부 데이터 소유 서비스는 호출하지 않았다.
- 유지한 계약: UUID 실제 userId, JWT sub·RS256·kid·issuer·audience 계약과 기존 RefreshSession·회원 탈퇴·Transaction 동작을 변경하지 않았다. 외부 Request Body에 userId나 사용자 시각을 추가하지 않았고 Learning Core의 답안·시험·음성 코드를 가져오지 않았다. Secret, Token, Password, 실제 Key와 전체 MongoDB URI를 소스·문서·기록에 추가하지 않았다.
- 결정사항: `QUALITY_REVIEW_CONSENT_VERSION`은 필수 비공백 기동 설정으로 추가했다. Quality review version 외부 형식은 최대 100자와 `^[A-Za-z0-9][A-Za-z0-9._-]{0,99}$`로 고정했다. 기존 회원 탈퇴 tombstone의 `consents` 보존 동작은 이번 hotfix에서 변경하지 않고 별도 제품·개인정보 결정으로 남겼다. Jira 키가 없어 Jira 조회·댓글·상태 변경은 수행하지 않았고 Git commit·push도 수행하지 않았다.
- 위험 요소: 현재 snapshot만으로 최초 미동의와 동의 후 철회를 법적으로 구분할 수 없으므로 필요한 경우 append-only 감사 이력이 별도 필요하다. 회원 탈퇴 시 Quality review true snapshot 처리 원칙도 미확정이다. Identity false 저장만으로 외부 답안·음성 이용을 중지할 수 없으므로 outbox·멱등 consumer·보존/삭제 정책과 SLA 검증 전 실제 품질 검토 이용을 활성화하면 안 된다.
- 다음 작업: 사용자가 working tree diff를 검토해 직접 commit·push하고 PR base를 `main`으로 지정한다. 배포 환경에 `QUALITY_REVIEW_CONSENT_VERSION`을 주입한 backend 호환 배포 후 프론트가 두 필드를 항상 전송하도록 전환하고 staging에서 Guest false/true, GET current/old, PUT 선택/철회·멱등성과 구버전 client를 검증한다.

## 2026-08-15 — Quality review hotfix 종료 Hook 작업 기록 동기화

<!-- codex-turn:01a004a8-1042-7692-bf0d-5282d02f33ee -->

- 날짜: 2026-08-15
- 브랜치: `hotfix/quality-review-consent` (`b6eb73e`, `origin/main` 기반, commit·push 미수행)
- 작업 목표: 종료 Hook이 요구한 현재 turn marker로 Quality review 선택 동의 구현 결과와 최종 저장소 상태를 WORKLOG 끝에 기록하고 CURRENT_STATE를 다시 동기화한다.
- 변경 파일: 이번 Hook 대응에서는 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 변경했다. 직전 구현의 애플리케이션·설정·README·계약·테스트 변경은 그대로 유지했으며 WORKLOG 과거 기록을 수정하거나 삭제하지 않았다.
- 구현 내용: Guest 생성과 인증된 동의 PUT·GET의 Quality review 선택 동의·철회, 누락=false 호환, true의 current version 검증, false/null/null 정규화, 멱등 상태 전이, GET `requiresConsent=false`, PUT 저장 결과 반환이 구현된 최종 상태임을 재확인했다. 이번 Hook 대응에서 실행 코드는 추가 변경하지 않았다.
- 실행한 테스트와 결과: 직전 최종 `./gradlew clean test`는 BUILD SUCCESSFUL, 전체 42개 suite·319개 테스트, 실패·오류·건너뜀 0개였다. Hook 문서 동기화 후 `git diff --check`를 다시 실행한다. 실제 Atlas, OAuth Provider, Sentry와 외부 데이터 소유 서비스는 호출하지 않았다.
- 유지한 계약: UUID 실제 userId, JWT, RefreshSession, 회원 탈퇴와 MongoDB CAS·Transaction 계약을 유지했다. Identity 밖의 답안·음성·시험 코드를 추가하지 않았고 Secret, Token, Password, 실제 Key와 전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 현재 구현과 검증 결과를 변경하지 않고 Hook이 지정한 별도 turn marker를 append-only 기록으로 추가했다. Jira 키가 없어 Jira 작업을 수행하지 않았고 Git commit·push도 수행하지 않았다.
- 위험 요소: snapshot만으로 최초 미동의와 철회를 법적으로 구분할 수 없고 회원 탈퇴 시 Quality review true snapshot 처리 원칙도 미확정이다. 외부 데이터 이용 중지는 outbox·멱등 consumer·보존/삭제 정책과 SLA가 준비되기 전 보장되지 않는다.
- 다음 작업: 사용자가 working tree를 검토해 직접 commit·push하고 `main` 대상 PR을 만든다. 배포 환경에 Quality review version을 주입한 뒤 staging에서 Guest·GET·PUT·철회·멱등·구버전 client 흐름을 검증한다.

## 2026-08-15 — Quality review hotfix 커밋 가능 상태 재검증

<!-- codex-turn:01a004b3-4ee4-79a3-afe7-66ede4bae4b5 -->

- 날짜: 2026-08-15
- 브랜치: `hotfix/quality-review-consent` (`b6eb73e`, `origin/main` 기반, commit·push 미수행)
- 작업 목표: 사용자가 구현 완료 후 지금 커밋해도 되는지 변경 범위·정적 검사·전체 테스트와 staging 상태를 근거로 확인한다.
- 변경 파일: 이번 확인 기록으로 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 추가 갱신했다. Quality review 애플리케이션·설정·README·계약·테스트 구현은 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: `git status --short`, unstaged diff stat·파일 목록과 HEAD를 확인했다. 변경은 Guest·consent API, `UserConsents`·`ConsentPolicy`·User 저장, quality review 오류·설정·OpenAPI·README·계획서와 관련 테스트 31개 파일로 한정되며 Firebase·TMI-95·TMI-96 등 develop 전용 파일은 포함되지 않았다.
- 구현 내용: 현재 staged 파일은 없고 HEAD는 여전히 `origin/main`과 같은 `b6eb73e`다. 사용자가 `git add` 후 staged diff를 마지막으로 확인하고 단일 hotfix commit을 생성할 수 있는 상태라고 판단했다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 커밋 가능 여부를 질문했다. 저장소 규칙에 따라 Codex는 stage·commit·push를 수행하지 않았고 사용자가 직접 실행할 명령만 안내한다.
- 실행한 테스트와 결과: 샌드박스의 사용자 Gradle cache lock 접근 제한으로 최초 실행이 시작되지 않아 승인된 동일 명령으로 `./gradlew clean test`를 재실행했다. BUILD SUCCESSFUL, 42개 suite·319개 테스트, failure 0, error 0, skipped 0이며 `git diff --check`도 성공했다. 실제 Atlas·OAuth Provider·외부 데이터 소유 서비스는 호출하지 않았다.
- 유지한 계약: main-only hotfix와 develop 격리, 기존 privacy·terms 필수 동의, quality review 선택·철회·구 client 누락=false 호환을 유지했다. Identity 밖의 시험·답안·음성 코드를 추가하지 않았고 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 커밋 가능한 상태다. 커밋 전 사용자가 전체 파일을 stage하고 staged name-only·stat·diff check를 확인한 뒤 quality review hotfix 단일 commit으로 남긴다.
- 위험 요소: 현재 상태는 모두 unstaged이므로 일부 파일만 stage하면 코드·테스트·설정·문서 계약이 분리될 수 있다. production 배포 전 `QUALITY_REVIEW_CONSENT_VERSION` 주입과 외부 데이터 철회 연동 부재를 별도로 확인해야 한다.
- 다음 작업: 사용자가 모든 의도된 변경을 stage하고 staged diff를 확인한 뒤 commit·push한다. PR base는 `main`으로 지정하고 `origin/main...HEAD`에 quality review 관련 변경만 있는지 다시 확인한다.

## 2026-09-18 — TMI-134 main 기반 hotfix 브랜치 생성

- 브랜치: codex/TMI-134-guest-session-recovery-hotfix. Jira: TMI-134.
- 목표: 사용자 요청대로 동일 작업 공간에서 main 기반 hotfix 브랜치 생성.
- 변경 파일: WORKLOG append, CURRENT_STATE 인계. 애플리케이션 변경 없음.
- 수행: main/origin/main 3894627aa4d77740fe1e20ea882e349a48c738ab 확인. 승인된 Git 쓰기로 전환 시도 후 기록 파일 충돌로 중단되어 두 파일만 stash 보존한 뒤 생성·전환 성공. HEAD는 main과 동일.
- 보존: stash 메시지 'TMI-134 preserve develop work records before main hotfix'. 기존 main 과거 기록 보존, hotfix 논의만 인계. 기존 로컬 poc/는 전환 후 미추적 표시되며 삭제/수정하지 않았다.
- 유지 계약·결정: 동일 ACTIVE Guest userId 복구, 대상 외 거절, API/기록 유지, 기본 OFF flag/수동 종료, 자동 종료·별도 횟수 제한 제외, 기존 유효 세션/정상 Refresh 유지.
- 검증: branch/HEAD/stash/status 확인 및 git diff --check. 기록 패치 문맥 불일치를 수정했다. 코드 변경 없어 테스트 미실행.
- 위험·다음: poc/와 develop 보관 기록은 hotfix 커밋 제외. 구현 전 Jira 및 main 모델 확인. 운영 변경·Secret 기록·commit·push 없음.

## 2026-09-18 — TMI-134 hotfix 브랜치 생성 기록 보완

<!-- codex-turn:01a0b374-3825-74b3-ac79-fd02d7a27ec2 -->

- 브랜치: codex/TMI-134-guest-session-recovery-hotfix. Jira: TMI-134.
- 목표: main 기반 브랜치 생성 결과를 이번 작업 식별자로 기록한다.
- 변경 파일: WORKLOG 끝 append, CURRENT_STATE 갱신. 과거 기록 보존.
- 수행 결과: main 3894627에서 브랜치 생성·전환 완료. develop의 기록 두 파일은 명명된 Git stash에 보존하고 확정된 hotfix 정책만 인계했다.
- 유지 계약·결정: API/userId/기록 유지, 한시적 Guest 복구 및 종료 후 유효 세션 유지 정책 보존. 코드·운영 변경이나 commit·push 없음.
- 검증: git diff --check 통과. 기록만 변경하여 테스트 미실행.
- 위험·다음: 기존 로컬 poc/는 변경 없이 보존하며 hotfix 커밋에서 제외한다. 구현 전 TMI-134와 main 코드 확인. Secret 기록·예상 밖 코드 변경 없음.

## 2026-09-18 — TMI-134 기존 Guest 긴급 복구 구현

- 브랜치: codex/TMI-134-guest-session-recovery-hotfix. Jira: TMI-134.
- 목표: main 기반으로 installationId의 기존 ACTIVE Guest에 동일 userId 새 세션을 발급하는 기본 OFF 긴급 복구를 구현한다.
- Jira: 구현 전 공식 MCP로 설명/완료 조건 조회. 댓글/상태 변경 없음. 댓글 초안은 runbook에 작성, 자동 등록하지 않음.
- 변경 파일: GuestAuthService, 신규 GuestRecoveryTransactionService, AuthController OpenAPI, application.yml, README, 신규 docs/contracts/guest-session-recovery-hotfix-TMI-134.md, WORKLOG/CURRENT_STATE. 테스트는 GuestAuthServiceTests, 신규 GuestRecoveryTransactionServiceTests, UserWithdrawalLifecycleTests 및 IdentityApplicationTests/SecurityIntegrationTests/DisabledSwaggerIntegrationTests/SentryCaptureIntegrationTests의 외부 DB 대체 Mock을 수정했다.
- 구현: 기존 설치 및 신규 등록 unique 충돌 후 별도 recovery transaction 호출. 기본 OFF GUEST_RECOVERY_ENABLED. hash/provider=GUEST/status=ACTIVE 조건 findAndModify(upsert=false)로 guestRecoveryFence 증가 후 동일 transaction에서 JWT 발급/Refresh 저장/응답 생성. 실제 사용자 쓰기로 탈퇴와 충돌을 유발하며 프로필/동의/기존 세션은 덮어쓰지 않는다. 성공 감사 이벤트는 proxy commit 이후 민감 값 없이 기록한다.
- 계약: 기존 URL/Request/Response/RS256/kid/userId/Refresh 해시 저장/TTL 유지. 자동 종료·별도 횟수 제한 제외. OFF만으로 기존 세션 폐기하지 않으며 정상 회전 유지. main에는 accountType/병합 모델이 없어 엄격한 GUEST+ACTIVE DB 조건을 적용한다. develop 포팅 시 병합/승격 모델 추가 확인 필요.
- 테스트: 최초 전체 실행은 신규 MongoTemplate 테스트 의존성 및 Mockito restubbing 준비 오류 등으로 실패, 이후 누락 matcher import를 수정했다. 최종 ./gradlew clean test 성공: 43 suites / 329 tests, failures=0/errors=0/skipped=0. git diff --check 통과. Gradle 캐시 sandbox 제한은 승인된 실행으로 해결.
- 검증 범위: 기본 OFF, 동일 userId/조건부 쓰기, 기존 동의 보존, 미대상/상태 변경 거절, 쓰기 충돌 시 토큰 미발급, Spring transaction commit/rollback 경계, 중복 등록 loser 복구, 민감정보 로그 비노출, Mock 기반 병렬 별도 세션 발급, 복구 OFF 후 발급 세션 정상 회전 및 기존 회귀 테스트. 실제 DB write conflict/rollback은 로컬 Mock 테스트로 입증하지 않으며 배포 전 runbook 확인 대상이다.
- 위험·배포 전: 설치 ID 신뢰/수동 OFF 누락/반복 발급 위험 유지. replica set 검증 후 운영자가 true 환경변수를 주입해 재배포해야 활성화됨. false 종료도 전 태스크 재배포 필요. SNS 및 구버전 생성 차단은 후속 인계. 운영 복구 완료는 아직 확인하지 않음.
- diff 점검: 예상 밖 애플리케이션 변경 없음. 기존 사용자 기록 변경은 보존했고 기존 로컬 poc/는 수정/삭제하지 않았다. commit/push/배포 없음.
- 다음: 변경 검토, 사용자 commit/push 및 main PR, 실제 replica set/운영 배포 검증, main 병합 후 develop 포팅. 과거 기록/Secret 보존 규칙 준수.

## 2026-09-18 — TMI-134 구현 작업 식별자 보완

<!-- codex-turn:01a0b376-60f8-7b60-872e-8256489c0173 -->

- 브랜치: codex/TMI-134-guest-session-recovery-hotfix. Jira: TMI-134.
- 목표: Guest 긴급 복구 구현 결과를 이번 작업 식별자로 기록한다.
- 변경 파일: WORKLOG 끝 append, CURRENT_STATE 갱신. 구현 변경 파일과 상세 근거는 바로 앞 구현 항목에 기록했으며 과거 기록은 보존한다.
- 구현·결정: 기존 GUEST+ACTIVE 계정의 동일 userId 세션 복구, 조건부 사용자 쓰기와 세션 저장의 Mongo transaction, 기본 OFF 설정 및 민감정보 없는 감사 이벤트를 추가했다. 자동 종료/추가 횟수 제한은 제외했다.
- 유지 계약: 기존 API/프로필/동의/기록 및 복구 OFF 이후 유효 세션/정상 Refresh 유지. 기존 토큰 만료·폐기 정책은 변경하지 않았다.
- 검증: 최종 전체 테스트 43 suites/329 tests, 실패·오류·skip 0. git diff --check 통과. 이번 보완은 기록만 변경하여 테스트 재실행하지 않았다.
- 위험·다음: 실제 replica set 경쟁/rollback 및 운영 true 설정 배포 검증은 남아 있다. 설치 ID 신뢰 위험 유지. 기존 poc/는 보존·커밋 제외. commit/push/배포/Jira 상태 변경·Secret 기록 없음.

## 2026-09-18 — TMI-134 사용자 커밋·push 명령 안내

- 브랜치: codex/TMI-134-guest-session-recovery-hotfix. Jira: TMI-134.
- 목표: 현재 변경만 커밋하고 기존 poc/를 제외하는 사용자 실행 명령을 제공한다.
- 변경 파일: WORKLOG append, CURRENT_STATE 갱신. 코드 변경 없음.
- 확인·결정: git status로 tracked 변경이 TMI-134 범위임을 확인했다. git add -u와 신규 구현/테스트/runbook 세 파일의 명시적 추가, staged diff 검사, TMI-134 커밋 메시지 및 현재 브랜치 push를 안내한다. git add . 사용은 피한다.
- 유지 계약: commit/push는 사용자가 수행한다. poc/와 보관된 develop stash는 포함하지 않는다. PR base는 main이다.
- 검증: branch/status 및 git diff --check. 문서만 변경하여 테스트 재실행 없음; 직전 329개 통과 결과 유지.
- 위험·다음: stage 후 사용자가 diff를 검토하고 commit/push한다. 명령 안내는 실제 배포/활성화와 별개다. 실제 Git 쓰기·Jira 변경·Secret 기록 없음.

## 2026-09-21 — 테스트 JWT RSA 키 준비 안내 기록

<!-- codex-turn:01a0c2c4-6779-7622-a251-e8674a484157 -->

- 브랜치: codex/TMI-134-guest-session-recovery-hotfix (직전 확인 기준). Jira: TMI-134 (문맥 식별; Jira 변경 없음).
- 작업 목표: Firebase 자격증명 저장 이후 테스트 JWT 서명 키 준비 절차를 안내한다.
- 변경 파일: docs/codex/WORKLOG.md 끝 append, docs/codex/CURRENT_STATE.md 갱신.
- 수행 내용: develop RsaKeyLoader에서 확인한 PKCS8 개인키/X509 공개키 형식에 맞춰 사용자가 OpenSSL로 별도 RSA 2048 키 쌍을 생성하고 개인키를 테스트용 Secret에 저장하도록 안내했다. 실제 키 생성/조회/외부 저장은 수행하지 않았다.
- 검증: git diff --check. 기록만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 WORKLOG 보존. Secret/Token/Password/실제 Key/전체 MongoDB URI 기록 없음. commit/push 없음.
- 결정사항: 운영 키는 재사용하지 않으며 임시 폴더는 영구 보관소로 사용하지 않는다.
- 위험 요소: 키 생성/Secret 저장 및 ECS 주입/실제 서명 검증은 미완료다.
- 다음 작업: 사용자 키 준비 완료 후 공개키/개인키 주입과 테스트 HMAC/복구 암호화 설정 준비.

## 2026-09-21 — 테스트 RSA 공개키 보관 및 실행 요구사항 안내

<!-- codex-turn:01a0c2c8-29d3-7ff0-bdbb-f6acd343cf72 -->

- 브랜치: codex/TMI-134-guest-session-recovery-hotfix (직전 확인 기준). Jira: TMI-134 (식별만; 변경 없음).
- 작업 목표: 개인키 Secret 등록과 서버 실행에 필요한 공개키를 구분한다.
- 변경 파일: WORKLOG append, CURRENT_STATE 갱신.
- 수행 내용: 개인키는 비밀 보관 필수이며 공개키는 비밀정보가 아니지만 현재 서버 설정은 두 PEM 파일을 모두 요구함을 안내했다. 관리 편의를 위해 별도 테스트 공개키 Secret 보관을 선택할 수 있으며 추가 Secret 비용이 발생할 수 있음을 알린다.
- 검증: git diff --check. 설명/기록만 변경하여 Gradle 미실행.
- 유지 계약: 실제 키 조회/생성/외부 저장 및 Secret 기록 없음. 과거 기록 보존. commit/push 없음.
- 결정사항: 공개키를 등록할 경우 동일한 개인키에서 생성된 PEM을 사용한다. Learning Core에는 개인키를 전달하지 않고 JWKS를 사용한다.
- 위험 요소: 저장만으로 ECS 파일 주입이 완료되지 않는다.
- 다음 작업: 키 쌍 보관 완료 후 서버 파일 주입/JWKS 및 나머지 보안 설정 준비.

## 2026-09-21 — 테스트 JWT 키 Secret 생성 확인

<!-- codex-turn:01a0c2cb-0620-75a0-8c82-4db1668db613 -->

- 브랜치: codex/TMI-134-guest-session-recovery-hotfix (직전 확인 기준). Jira: TMI-134 (문맥 식별; 변경 없음).
- 작업 목표: 사용자 저장한 테스트 RSA 키 Secret 리소스 존재 확인.
- 변경 파일: docs/codex/WORKLOG.md append, docs/codex/CURRENT_STATE.md 갱신.
- 수행 내용: 서울 Secrets Manager 목록에서 테스트 jwt-private-key 및 jwt-public-key 항목 확인. 기존 테스트 mongodb/firebase 항목도 존재. 키 내용은 조회하지 않았으며 탭 유지 처리.
- 검증: 콘솔 목록 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지 계약: 과거 기록 보존. Secret/Token/Password/실제 Key/전체 MongoDB URI 조회·기록 및 외부 설정 변경 없음. commit/push 없음.
- 결정사항: Secret 생성 완료와 PEM 형식/키 쌍 일치/실제 서명 검증은 구분한다.
- 위험 요소: ECS 주입 및 실제 키 로딩/서명 검증 미완료.
- 다음 작업: 전화번호 fingerprint HMAC 키와 재발급 응답 복구 암호화 키를 코드 계약에 맞게 준비하고 ECS 권한/파일 주입 구성.

## 2026-09-21 — 테스트 전화번호 fingerprint 키 준비 안내

<!-- codex-turn:01a0c2cc-2837-7cf3-84ea-ae44913b8fe7 -->

- 브랜치: feat/TMI-169-guest-enrollment-resume. Jira: TMI-169 (현재 브랜치 문맥만; 이 안내는 테스트 인프라 준비이며 Jira 변경 없음).
- 작업 목표: 전화번호 fingerprint HMAC 키 생성 및 Secret 저장 형식 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 로컬 develop PhoneFingerprintProperties/PhoneFingerprintKey에서 버전, ACTIVE_WRITE 상태, 표준 Base64 및 최소 32바이트 계약 확인. 사용자 로컬 생성 및 Secrets Manager key/value 저장 절차 안내. 실제 키 생성/조회/저장 없음.
- 테스트와 결과: 코드 읽기 검증, git diff --check. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 기존 미커밋 구현 및 과거 기록 보존. 실제 Secret/Token/Password/Key/전체 MongoDB URI 기록 없음. commit/push 및 외부 변경 없음.
- 결정사항: 테스트 전용 키를 사용하며 JWT/응답 복구/Billing 키와 공유하지 않는다. ECS에는 JSON의 PHONE_IDENTITY_FINGERPRINT_KEY_RING 값만 주입한다.
- 위험 요소: 실제 저장/주입 미검증. 데이터 생성 이후 임의 키 교체는 기존 fingerprint 조회를 깨뜨릴 수 있다.
- 다음 작업: 사용자 Secret 저장 후 응답 복구 암호화 키 및 ECS 설정 준비.

## 2026-09-21 — HMAC 저장 완료 보고 및 응답 복구 키 안내

<!-- codex-turn:01a0c2d0-e07f-7251-bbcf-074b14febafb -->

- 브랜치: feat/TMI-169-guest-enrollment-resume. Jira: TMI-169 (브랜치 식별만; Jira 변경 없음).
- 작업 목표: 사용자 HMAC 저장 완료 보고 정리와 테스트 응답 복구 AES 키 준비 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: MountedReissueEncryptionKeys에서 32바이트 Base64 키, properties 형식 및 절대 파일 경로 계약 확인. 독립 키 생성과 Secrets Manager plaintext 저장 절차 안내. 실제 비밀값 조회/생성/외부 변경 없음.
- 테스트와 결과: 코드 읽기 및 git diff --check; 문서 변경만으로 Gradle 미실행.
- 유지한 계약: 비밀정보 미기록, 기존 작업 보존, commit/push 없음.
- 결정사항: HMAC 저장은 사용자 보고 기준. 복구 keyring은 JSON 아닌 properties 원문으로 저장하고 ECS 파일 공급은 별도 구성한다.
- 위험 요소: Secret 저장만으로 파일 마운트/복구 기능이 활성화되지 않는다. 실제 연결 및 키 유효성 미검증.
- 다음 작업: 사용자 복구 키 저장 후 ECS 파일 공급과 실행 역할 권한 및 환경설정 준비.

## 2026-09-21 — 복구 keyring 저장 형식 재확인

<!-- codex-turn:01a0c2d4-18c8-7302-8f17-1ac5b3747332 -->

- 브랜치: feat/TMI-169-guest-enrollment-resume. Jira: TMI-169 (브랜치 문맥; Jira 변경 없음).
- 작업 목표: 사용자가 이상하게 느낀 복구 Secret 저장 형식 설명 및 확인 경계 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: MountedReissueEncryptionKeys의 Properties.load 및 Base64 32바이트 검증을 다시 확인. properties 원문과 JSON 키/값 화면 차이를 설명하고 비밀값 없이 이상 증상을 질문한다.
- 테스트와 결과: 코드 읽기 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 실제 Secret 값 조회/기록 없음. 과거 기록과 기존 코드 변경 보존. 외부 변경 및 commit/push 없음.
- 결정사항: 저장 완료는 사용자 보고이며 실제 저장 내용 적합성은 확인하지 않았다. Base64 끝의 등호는 보존한다.
- 위험 요소: JSON 포장 또는 키 누락 여부 미확인; ECS 파일 공급은 여전히 별도 작업이다.
- 다음 작업: 사용자 화면 증상을 비밀정보 없이 확인하고 필요시 저장 형식 정정 안내.

## 2026-09-21 — 복구 Secret 리소스 저장 확인

- 브랜치: feat/TMI-169-guest-enrollment-resume. Jira: TMI-169 (문맥만, 변경 없음).
- 작업 목표: 사용자가 저장한 복구 Secret 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 서울 Secrets Manager 목록과 상세 화면에서 reissue-encryption-keyring 리소스 존재 및 aws/secretsmanager 암호화 확인. phone-fingerprint 리소스도 목록에 존재. 비밀값 검색은 실행하지 않음.
- 테스트와 결과: 콘솔 메타데이터 확인, git diff --check; 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 비밀정보 미조회/미기록, 외부 설정 변경 및 commit/push 없음, 기존 변경 보존.
- 결정사항: 리소스 저장 확인과 실제 properties/Base64 형식 검증을 구분한다.
- 위험 요소: 저장된 값의 형식과 32바이트 여부는 아직 미검증.
- 다음 작업: 원문을 노출하지 않는 내용 검증 및 ECS 파일 공급 준비.

## 2026-09-21 — 사용자 예시의 복구 keyring 형식 확인

- 브랜치: feat/TMI-169-guest-enrollment-resume. Jira: TMI-169 (문맥만; 변경 없음).
- 작업 목표: 실제 값과 다르다고 명시한 사용자 예시의 저장 문법 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: key ID와 Base64 값을 등호로 구분하는 properties 한 줄 형식이 코드 계약에 부합함을 안내. 예시 값은 기록하지 않음.
- 테스트와 결과: 형식 확인, git diff --check. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 실제 키 및 예시 키 원문 미기록, 외부 변경/commit/push 없음.
- 결정사항: Base64 패딩 등호는 유지하며 활성 key ID를 일치시킨다.
- 위험 요소: AWS 실제 저장값 및 ECS 파일 공급은 미검증.
- 다음 작업: ECS 파일 공급 및 환경설정 준비.

## 2026-09-21 — TMI-169 Guest SNS 가입 재개 및 enrollment 만료 계약 구현

- 날짜: 2026-09-21
- 브랜치: `feat/TMI-169-guest-enrollment-resume` (`develop`/`origin/develop`의 `8c624ffe` 기반, commit·push 미수행)
- Jira: `TMI-169` (parent `TMI-136`)
- 작업 목표: Guest SNS/Firebase 인증 뒤 가입을 중단한 사용자가 `/firebase/guest/prepare` 응답만으로 유효한 enrollment와 미충족 요건을 받아 재개하고, 만료·기존 MEMBER merge·불가능한 Guest-owned identity를 명확히 구분하도록 구현한다.
- 변경 파일: `AuthErrorStatus`, `FirebaseExchangeController`, `FirebaseExchangeService`, `FirebaseGuestPrepareService`, `FirebaseGuestPrepareResponse`, `FirebaseGuestPrepareResultType`, `FirebaseAuthenticationConfiguration`을 수정하고 `FirebaseEnrollmentRequirementResolver`를 추가했다. enrollment attempt, Guest prepare, resolver, response DTO, Controller와 Firebase configuration 테스트를 추가·수정했다. Firebase 프론트 가이드·부록과 `firebase-guest-enrollment-resume-plan.md`, `CURRENT_STATE.md`, `WORKLOG.md`를 갱신했다.
- 구현 내용: Guest prepare 전용 `ALREADY_LINKED`를 제거했다. identity owner가 없으면 Guest-bound 활성 enrollment를 재사용하고 없거나 만료됐으면 새 attempt를 발급한다. 다른 ACTIVE MEMBER owner는 enrollment mutation 없이 `MERGE_REQUIRED`, 현재 Guest owner는 `409 IDENTITY_STATE_CONFLICT`로 중단한다.
- 구현 내용: `ENROLLMENT_REQUIRED` 응답에 `missingRequirements`, 현재 privacy/terms policy version과 기존 enrollmentId·expiresIn을 함께 반환한다. 공통 resolver가 fresh Firebase proof의 PASSWORD email 검증과 PHONE 연결·검증을 판정하며, Guest 승격의 `PROFILE`은 항상 요구하고 저장된 필수 동의의 boolean·현재 version·timestamp가 모두 유효하지 않으면 `CONSENTS`를 요구한다. direct signup의 기존 requirements 계산도 같은 resolver로 이동했다.
- 구현 내용: 응답 DTO에 type별 null/non-null 불변식과 방어적 Set 복사를 적용했다. 만료 attempt는 기존 CAS로 `EXPIRED` 전환한 후 새 ID를 만들고 `expiresAt` TTL은 추가하지 않았으며 기존 `cleanupAt` 보존 정책을 유지했다. Provider link의 MEMBER용 `ALREADY_LINKED`는 변경하지 않았다.
- 구현 내용: `identity.firebase.guest.prepare` counter에는 `ENROLLMENT_REQUIRED`, `MERGE_REQUIRED`, `IDENTITY_STATE_CONFLICT`의 저카디널리티 outcome만 기록한다. Guest-owned conflict WARN에도 event와 outcome만 넣어 Firebase UID, provider subject, phone, userId, enrollmentId와 credential을 기록하지 않는다.
- 실행한 테스트와 결과: 관련 service·DTO·Controller 테스트가 먼저 성공했다. metric 추가 뒤 첫 전체 실행은 격리된 Firebase configuration 테스트 컨텍스트의 `MeterRegistry` 누락 1건으로 실패했고 `SimpleMeterRegistry` test bean을 추가해 구성 경계를 수정했다. 이후 관련 테스트가 성공했으며 최종 `./gradlew clean test`는 BUILD SUCCESSFUL, 896 tests, 실패 0이었다. 실제 Atlas·Firebase Provider는 호출하지 않았다.
- 유지한 계약: 실제 userId·JWT·Session·Mongo transaction 계약과 `/users/me` 프로필 계약을 변경하지 않았다. client의 phone/email/consent 주장을 신뢰하지 않고 fresh Firebase proof와 서버 User·ConsentPolicy만 사용한다. 만료 upgrade의 기존 `FIREBASE_ENROLLMENT_CONFLICT`, terminal `cleanupAt` TTL, Provider link/unlink의 별도 `ALREADY_LINKED`를 유지했다. Secret, Token, Password, 실제 Key, 전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: Guest prepare의 정상 최초 진입과 재개·만료 후 재발급은 모두 `ENROLLMENT_REQUIRED`로 통일하고 내부 attempt 생성 여부는 외부 계약으로 노출하지 않는다. Guest-owned identity는 legacy가 없다는 전제의 불변식 위반으로 간주해 자동 승격·merge·reconciliation 없이 409와 안전한 관측으로 처리한다. `missingRequirements` 배열 순서는 계약이 아니다.
- 수행한 Jira 작업: 이 구현 turn에서는 Jira 이슈 조회·수정·댓글·상태 전환을 수행하지 않았다. `TMI-169` 설명과 완료 조건을 구현 기준으로 사용했고, Jira 댓글은 작업 요약·변경 파일·테스트 결과·남은 위험만 담은 초안을 최종 보고에 제공한다.
- 추가한 댓글의 목적: 자동 등록한 댓글은 없다. 초안 목적은 구현 범위와 전체 회귀 테스트 결과, staging 확인 항목을 공유하는 것이다.
- 변경한 상태: Jira 상태와 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 구현 시작을 명시적으로 요청했다. Jira 쓰기, Git commit·push와 배포 승인은 요청하거나 사용하지 않았다.
- 위험 요소: 운영·staging에서 Guest-owned identity가 실제 0건인지 확인하지 않았고 metric dashboard·alert도 저장소 밖에서 구성해야 한다. 구버전 앱이 새로운 409를 안전하게 중단하는지, Mongo replica set에서 만료 attempt 교체·동시 prepare가 하나의 활성 attempt만 남기는지는 staging 검증이 필요하다. 현재 두 Codex 기록 파일은 이번 구현 이전부터 develop 기준 대규모 삭제 diff를 포함하고 있어 코드 변경과 분리해 검토해야 하며 임의 복원하지 않았다.
- 다음 작업: 사용자가 변경 diff를 검토해 직접 commit·push하고 PR을 생성한다. staging에서 활성 attempt 재사용, 만료 교체, phone/email requirement 재계산, MEMBER merge와 conflict metric을 확인한 뒤 Jira 댓글 등록·상태 전환이 필요하면 별도 승인 후 수행한다.

## 2026-09-21 — TMI-169 구현 종료 Hook 기록 동기화

<!-- codex-turn:01a0c2c6-6e44-7153-8451-e055695994f8 -->

- 날짜: 2026-09-21
- 브랜치: `feat/TMI-169-guest-enrollment-resume`
- Jira: `TMI-169` (parent `TMI-136`)
- 작업 목표: 종료 Hook이 요구한 현재 turn 식별자로 Guest SNS 가입 재개 및 enrollment 만료 계약 구현 결과를 WORKLOG 끝에 append하고 CURRENT_STATE를 최신화한다.
- 변경 파일: 이번 Hook 대응에서는 `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`만 추가 갱신했다. 직전 TMI-169 애플리케이션·테스트·계약 변경은 그대로 유지했고 과거 WORKLOG 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Guest prepare 전용 `ALREADY_LINKED` 제거, enrollment 재사용·만료 교체, requirements·현재 정책 version 응답, MEMBER merge 분기, Guest-owned identity의 `IDENTITY_STATE_CONFLICT`, 저카디널리티 outcome 관측이 완료된 상태임을 기록했다. 이번 Hook 대응에서 실행 코드는 변경하지 않았다.
- 실행한 테스트와 결과: 직전 최종 `./gradlew clean test`는 BUILD SUCCESSFUL, 896 tests, 실패 0이었다. Hook 문서 반영 뒤 `git diff --check`를 다시 실행한다. 실제 Atlas·Firebase Provider는 호출하지 않았다.
- 유지한 계약: UUID userId, JWT·Session·Mongo transaction, 기존 enrollment lifecycle과 Provider link의 별도 `ALREADY_LINKED` 계약을 유지했다. Secret, Token, Password, 실제 Key, 전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 구현 결과를 변경하지 않고 Hook이 지정한 turn marker를 append-only 기록으로 추가했다. Jira 댓글·상태 변경과 Git commit·push는 수행하지 않았다.
- 위험 요소: staging의 동시 prepare·만료 교체, Guest-owned identity 0건, metric dashboard와 구버전 앱의 새 409 처리 검증이 남아 있다. 두 Codex 기록 파일의 기존 대규모 diff는 임의 복원하지 않았다.
- 다음 작업: 사용자가 diff를 검토해 직접 commit·push하고 PR을 생성한다. Jira 댓글 등록이나 상태 전환은 별도 승인 후 수행한다.

## 2026-09-21 — 복구 키 형식 확인 작업 식별 기록

<!-- codex-turn:01a0c2d6-4241-7cf1-9ccd-5a3c419d50fe -->

- 브랜치: feat/TMI-169-guest-enrollment-resume. Jira: TMI-169 (브랜치 문맥만).
- 작업 목표: 사용자 비실제 예시의 properties 형식 확인 및 이번 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 한 줄 key ID/Base64 형식과 패딩 보존을 안내했다. 예시 및 실제 비밀값은 기록하지 않는다.
- 테스트와 결과: git diff --check. 문서 기록만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 기록/동시 작업 보존. 외부 변경, Jira 쓰기, commit/push 없음.
- 결정사항: 활성 key ID 일치 필요. 실제 Secret 로딩 검증과 예시 형식 확인은 별개다.
- 위험 요소: ECS 파일 공급과 실제 키 검증 미완료.
- 다음 작업: 테스트 ECS 보안 설정 및 파일 공급 준비.

## 2026-09-21 — Secret 준비 이후 테스트 Identity 배포 순서 안내

- 브랜치: feat/TMI-169-guest-enrollment-resume. Jira: TMI-169 (브랜치 문맥만).
- 작업 목표: 다음 테스트 인프라 준비 작업 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Dockerfile 및 entrypoint 확인. JWT 환경변수의 PEM 파일 변환은 기존 지원하지만 Firebase JSON/복구 keyring 파일 공급은 현재 스크립트에 없음을 확인. 파일 공급 설계, 테스트 execution role 권한, task definition, 별도 서비스/HTTPS, 연결 검증 순서 안내.
- 테스트와 결과: 코드 읽기 및 git diff --check. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 운영 설정/외부 리소스 변경 없음. 비밀값 기록 없음. 기존 코드 변경 보존, commit/push 없음.
- 결정사항: ECS Secret 환경변수 주입과 파일 공급을 구분하고 별도 테스트 서비스로 진행한다.
- 위험 요소: 배포 revision, 파일 공급, 권한 및 서버 연결 미검증. 필요한 기능 설정 의존성도 배포 전 확인해야 한다.
- 다음 작업: Firebase/복구 keyring 파일 공급 방식 확정 및 구현 범위 승인 후 준비.

## 2026-09-21 — 시작 스크립트 보완 범위 검토

<!-- codex-turn:01a0c2db-14c8-7751-9894-61ae262ee8f2 -->

- 브랜치: develop. Jira: 이번 검토에 지정된 이슈 없음.
- 작업 목표: 테스트 Secret 공급을 위한 entrypoint 보완 사항 분석.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: entrypoint/Dockerfile, Firebase ADC provider, 복구 설정과 파일 loader 확인. Firebase JSON/복구 keyring 파일 공급, 기능별 필수 설정 검사, 입력 방식 충돌 거절, 권한 제한과 환경변수 제거, 회귀 테스트 필요성을 정리했다. 구현하지 않음.
- 테스트와 결과: 소스 읽기 및 git diff --check. 분석/기록만 변경하여 Gradle 미실행.
- 유지한 계약: JWT 기존 주입 및 비활성 기능 호환, 비밀정보 미기록, 기존 변경 보존. 외부 설정 변경/commit/push 없음.
- 결정사항: JWT resource 위치와 Firebase/복구 절대 파일 경로 형식을 구분한다. 파일 주입과 기존 파일 지정 방식의 우선순위를 암묵적으로 정하지 않는다.
- 위험 요소: 현재 폴더 생성은 umask 설정 전에 수행되며 기존 이미지 폴더 권한도 명시적 제한 필요. 실제 컨테이너 검증 미수행. 파일 생성 후 unset은 ECS 환경변수 방식의 모든 노출 경로 제거를 보장하지 않는다.
- 다음 작업: 승인 시 시작 스크립트/Dockerfile/가짜 값 기반 회귀 테스트 및 설정 문서 보완.

## 2026-09-21 — TMI-169 프론트 가입 재개 인계 문서 갱신

<!-- codex-turn:01a0c2db-619f-7b30-9a42-07c72d664808 -->

- 날짜: 2026-09-21
- 브랜치: `develop` (시작 시 clean, TMI-169 구현 존재 확인)
- Jira: TMI-169
- 작업 목표: 구현된 Guest prepare 계약을 프론트가 적용할 수 있도록 인계 문서의 재개 절차·필수 요청·만료 처리·QA를 최신화한다.
- 변경 파일: `docs/contracts/frontend-firebase-auth-integration-guide.md`, `docs/contracts/frontend-firebase-auth-integration-appendix.md`, `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`.
- 구현 내용: 주 문서의 기준일·5줄 결론과 필수 규칙을 갱신하고 출시 결정·미확인 항목을 앞쪽에 배치했다. Guest prepare의 두 성공 타입과 409, 중단 후 재개, phone proof 재사용, 만료 ID 폐기와 prepare 재호출, TTL 비연장, recent-auth 재인증을 명시했다. PROFILE은 항상 반환되며 빈 requirements를 기다리지 않도록 안내하고 CONSENTS가 없어도 upgrade 동의 boolean/버전 네 필드는 필수임을 실제 DTO·서비스와 대조해 기록했다. upgrade 전체 요청 예시, 정책 변경 오류 처리, 부록의 필드/요건/QA 전체 표와 코드·테스트 링크를 보완했다.
- 실행한 테스트와 결과: 문서만 변경하여 Gradle은 재실행하지 않았다. Ruby로 두 문서의 로컬 파일 링크 52개 존재 및 JSON 예시 40개 파싱 성공을 확인했고 `git diff --check`가 통과했다. 기존 896개 전체 테스트 성공은 이전 구현 결과임을 명시했으며 실제 모바일/배포 E2E 통과로 표현하지 않았다.
- 유지한 계약: Guest prepare 전용 ALREADY_LINKED 제거와 MEMBER provider-link의 별도 멱등 계약, fresh Firebase proof 기반 판정, 필수 동의 검증, 기존 expiresAt/cleanupAt 수명주기 및 JWT/Session 계약을 문서에 반영했다. 실행 코드와 외부 API를 추가 변경하지 않았다. 비밀값과 개인정보를 기록하지 않았다.
- 결정사항: 사용자가 요청한 기존 프론트 가이드와 부록을 직접 갱신하고 세부 QA·근거를 부록에 배치했다. 신규 backend 구현이나 별도 복구 API를 가정하지 않는다. Jira 댓글 초안은 아래 요약으로 준비했으며 자동 등록하지 않았다.
- Jira 댓글 초안 요약: 프론트 가이드·부록에 TMI-169 재개/만료/동의 필수 요청/QA 인계를 반영. 문서 링크 52개·JSON 40개·diff 검사 통과, 배포 및 모바일 E2E 미확인.
- 위험 요소: backend 배포 버전, 약관 본문·URL 공급, 실제 SNS/SMS와 동시 prepare/만료 전환, 새 409 UI는 배포 환경에서 확인해야 한다. 작업 도중 다른 작업의 시작 스크립트 분석 기록이 두 Codex 파일에 추가돼 보존했다. 애플리케이션의 예상 밖 변경은 없다.
- 다음 작업: 프론트 담당자가 가이드 2.2/4.4/8.5와 부록 TMI-169 QA를 적용한다. 배포 전 새 응답과 정책 본문 공급을 확인하고 통합 QA를 수행한다. commit·push는 사용자 수행이며 Jira 쓰기·상태 변경은 하지 않았다.

## 2026-09-21 — 시작 스크립트 변경 여부 설명

<!-- codex-turn:01a0c2e5-b424-7023-b6fc-d93df2c08068 -->

- 브랜치: develop. Jira: 지정 없음.
- 작업 목표: 다음 작업이 저장소 코드 수정인지 확인 요청에 답변.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 시작 스크립트와 필요시 Dockerfile, 테스트/문서 수정이며 인증 API 비즈니스 로직 변경은 아님을 설명했다. 구현은 시작하지 않았다.
- 테스트와 결과: git diff --check. 설명/기록만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 기록과 동시 변경 보존, 비밀정보 기록 없음, 외부 설정 변경 및 commit/push 없음.
- 결정사항: 코드 변경 후 새 이미지 배포와 ECS 설정 연결이 별도로 필요하다.
- 위험 요소: 아직 구현/배포 미수행.
- 다음 작업: 사용자 구현 요청 시 시작 스크립트 보완 진행.

## 2026-09-21 — 학습 기록만 삭제 프론트 요구사항 인계

<!-- codex-turn:01a0c2e5-515b-7321-8afe-9cccff56f1b1 -->

- 날짜: 2026-09-21
- 브랜치: `develop`
- 작업 목표: 로그아웃·회원 탈퇴와 독립적인 학습 기록 삭제 요구사항을 프론트 문서에 명시하고 담당 서비스 및 미확정 범위를 구분한다.
- 변경 파일: `docs/contracts/frontend-firebase-auth-integration-guide.md`, `docs/contracts/frontend-firebase-auth-integration-appendix.md`, `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`.
- 구현 내용: 주 문서 5.4에 기능 비교, 계정·로그인 유지 요구사항, Learning Core 호출 경계와 확인/접수/완료/캐시 갱신 제안 흐름을 추가했다. 부록에 시험·통계·스트릭·단어장·파일 범위, 처리 중 AI/새 학습 경계, 멱등성과 완료 조회·QA 항목을 기록했다. Learning Core 구현 사실과 API는 미확인·미확정으로 표시했다.
- 실행한 테스트와 결과: 문서만 변경해 Gradle은 실행하지 않았다. 기존 JWT 계약을 확인했고 로컬 링크 57개 존재·JSON 예시 40개 파싱·git diff --check가 통과했다.
- 유지한 계약: Identity의 계정·인증 도메인 경계를 유지하고 사용자 식별은 검증된 JWT sub로 한다. Identity에 학습 삭제 코드를 추가하지 않았으며 실제 삭제·외부 API 변경을 수행하지 않았다. 비밀값·개인정보 기록 없음.
- 결정사항: 학습 기록 삭제를 별도 Learning Core 요구사항으로 인계한다. 구매/이용권/무료 체험 초기화 제외는 담당자 확인이 필요한 제안으로 표시했다. 새 endpoint나 오류 코드를 만들어 기정사실화하지 않았다.
- Jira: TMI-169 (기존 프론트 문서 맥락만; 학습 기록 삭제는 별도 이슈 미지정). Jira 조회·댓글·상태 변경·생성을 수행하지 않았으며 해당 구현 완료 범위에 포함하지 않는다.
- 위험 요소: 삭제 대상, MEMBER/GUEST 권한, 복구·보존 정책, 원격 파일·AI callback 처리, 멱등·완료 확인과 실제 배포 상태가 미확정이다. 이전 문서 변경 및 동시 작업의 Codex 기록을 보존했으며 이번 작업의 예상 밖 코드 변경은 없다.
- 다음 작업: 제품/프론트/Learning Core 담당자가 전체·개별 삭제 여부와 데이터 범위를 확정하고 별도 API 계약·구현·통합 QA를 진행한다. 배포 전 완료 기준과 계정 유지, 삭제 후 새 기록 보존을 검증한다. commit·push는 사용자 수행.

## 2026-09-21 — 테스트 Identity 컨테이너 Secret bootstrap 구현

<!-- codex-turn:01a0c2e8-57b2-71e1-8c67-6c8880cf85c8 -->

- 브랜치: develop. Jira: 이번 구현에 지정된 이슈 없음; Jira 변경 없음.
- 작업 목표: 사용자 승인에 따라 Firebase/재발급 복구 파일 공급과 컨테이너 권한/설정 검증을 보완한다.
- 변경 파일: docker-entrypoint.sh, Dockerfile, src/test/java/web/tosunsaeng/identity/deployment/DockerEntrypointTests.java, docs/contracts/identity-container-secret-bootstrap.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Firebase JSON과 복구 properties 원문을 task-local private 파일로 공급한다. 외부 파일 경로 지원과 원문/경로 충돌 거절, 기능 ON 필수 설정 검사, 700 폴더/600 파일, 기동별 임시 폴더, 셸 실패 정리, tracing 비활성 및 JVM 원문 환경변수 제거를 구현했다. 기존 JWT 원문 주입과 resource URI 설정은 유지했다. Dockerfile의 secret root 권한도 제한했다.
- 실행한 테스트와 결과: 집중 DockerEntrypointTests 21개 통과. ./gradlew clean test 140 suites/917 tests, 실패·오류·skip 0. 전체 테스트 최초 sandbox cache 접근 실패 후 승인된 실행으로 재검증 성공. sh -n docker-entrypoint.sh 및 git diff --check 통과. 외부 Firebase/Atlas/AWS 호출 없음.
- 유지한 계약: 로그인/가입/재발급 API와 JWT claim/서명, Java 인증 로직 및 기본 feature flag 변경 없음. 기존 JWT-only 실행 호환. 기존 및 동시 프론트 문서 변경을 보존했고 이번 작업의 예상 밖 코드 변경 없음. 실제 Secret/Token/Password/Key/전체 MongoDB URI 기록 없음.
- 결정사항: Firebase auth ON인 컨테이너는 명시적 credentials 파일을 요구한다. 암호학적/JSON/keyring 내용 검증은 Java 계층에 유지한다. 배포 문서에 ECS secrets 변수 매핑/실행 역할/파일 경로 계약 및 Jira 댓글 초안을 기록하되 등록하지 않았다.
- 위험 요소: 실제 Docker 이미지 실행/ECS 권한/Secret 값과 Firebase 인증 E2E는 미검증. 원문 unset은 초기 환경/관리자 접근 전체 제거 보장이 아니다. JVM exec 후 파일은 task 수명 동안 유지되므로 영구/공유 볼륨 사용 금지. 복구 session fence 등 기능 의존성은 배포 전 확인해야 한다.
- 다음 작업: 사용자 diff 검토 및 직접 commit/push 후 새 이미지 배포 준비. 테스트 Task Definition에 Secret 참조와 일반 설정을 연결하고 non-root 쓰기/기능 활성화 의존성/JWKS/가입·재발급을 검증한다. AWS 변경/배포/commit/push는 수행하지 않았다.

## 2026-09-21 — Secret bootstrap 구현 재검토

- 브랜치: develop. Jira: 지정 없음.
- 작업 목표: 구현과 테스트 결과 및 배포 전 설정 의존성을 다시 확인한다.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md (기록만).
- 수행 내용: entrypoint/배포 안내/Firebase 및 session 설정 확인. 검토 범위에서 추가 코드 수정이 필요한 문제는 발견하지 못했다. 복구 ON에는 AUTH_SESSION_FENCE_ENABLED=true가 필요함을 코드상 필수 Bean 의존성으로 확인했다.
- 테스트와 결과: 기존 전체 테스트 XML에서 140 suites/917 tests 실패·오류·skip 0 확인 후 집중 21개 테스트를 --rerun-tasks로 재실행해 성공. sandbox 캐시 접근 실패 후 승인 실행. sh -n과 git diff --check 통과. 전체 테스트는 이번에는 재실행하지 않았다.
- 유지한 계약: 비밀정보 조회/기록 없음. 기존 코드/문서 변경 보존. 외부 설정 변경, 배포, commit/push 없음.
- 결정사항: 코드 검증 완료와 실제 Docker/ECS 연결 검증을 구분한다. Secret 원문 주입 시 파일 경로는 중복 지정하지 않는다.
- 위험 요소: 실제 Secret 값/컨테이너 이미지 실행/실행 역할/인증 E2E는 미확인. 운영 준비 완료를 의미하지 않는다.
- 다음 작업: 테스트 ECS 전용 execution role과 Task Definition의 Secret 매핑/일반 환경변수 및 새 이미지 준비.

## 2026-09-21 — Bootstrap 재검토 작업 식별 보완

<!-- codex-turn:01a0c2ee-e72a-7d52-9e6b-4debc18832d7 -->

- 브랜치: develop. Jira: 지정 없음.
- 작업 목표: 이번 재검토의 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 시작 스크립트 재검토 및 session fence 의존성 확인 결과를 기록했다. 이번 보완은 문서만 수정했다.
- 테스트와 결과: 앞선 집중 21개 재실행 성공 및 기존 전체 917개 성공 결과 확인 유지. 이번 기록 보완 후 git diff --check 통과.
- 유지한 계약: 과거 기록 보존, 비밀정보 미기록, 외부 변경/commit/push 없음.
- 결정사항: 코드 검증과 실제 ECS 연결 검증은 별개다.
- 위험 요소: 실제 이미지 실행과 Secret 연결/E2E 미검증.
- 다음 작업: 테스트 ECS 실행 역할과 Task Definition 설정 준비.

## 2026-09-21 — 프론트 공유용 Identity Swagger 생성

<!-- codex-turn:01a0c2f4-26db-7572-bf84-dab23d30075c -->

- 날짜: 2026-09-21
- 브랜치: `develop` (기존 동시 작업 디렉터리·변경 보존)
- Jira: TMI-169 (Guest prepare OpenAPI 보완 관련)
- 작업 목표: 현재 Identity API를 프론트에 공유할 수 있는 OpenAPI JSON과 서버 없이 열 수 있는 Swagger ZIP으로 생성한다.
- 변경 파일: `build.gradle`, `FirebaseExchangeController.java`, 신규 `FirebaseGuestPrepareResponseEnvelope.java`, 신규 `OpenApiSharingTests.java`, `docs/swagger/index.html`, `docs/swagger/README.md`, `docs/swagger/LICENSE-swagger-ui.txt`, 프론트 가이드, WORKLOG/CURRENT_STATE. 생성물은 기존 gitignore 대상 build 디렉터리에 둔다.
- 구현 내용: 격리된 Spring test context의 `/v3/api-docs`에서 실제 Controller 명세를 내보내는 exportOpenApi, Swagger UI 의존성과 읽기 전용 HTML을 묶는 prepareSwaggerBundle/shareSwagger task를 추가했다. 사용자 정의 fake endpoint 없이 실제 24개 경로를 포함하며 JSON 참조와 Guest prepare 필드·enum·인증 조건을 검사한다. 예시 서버 주소, 생성 UTC 시점, 사용법, 공식 Swagger UI 라이선스를 함께 포함했다.
- 구현 내용: Guest prepare 성공 응답의 raw BaseResponse 문서 스키마를 문서 전용 typed envelope로 구체화하고 재개·TTL 비연장·PROFILE·동의 필수 필드·409 설명을 보완했다. 런타임 응답은 기존 BaseResponse 그대로다. 미확정 Learning Core 학습 삭제 API를 포함하지 않았다.
- 실행한 테스트와 결과: 최초 export 계약 테스트에서 일반 return type schema 설정으로는 Guest prepare 필드가 노출되지 않음을 확인해 typed envelope로 수정했다. 최종 `./gradlew clean test shareSwagger` 성공, 918 tests/141 suites, failures=0/errors=0/skipped=0이며 exportOpenApi의 별도 1 test도 성공했다. 라이선스 추가 후 shareSwagger 재생성 성공. ZIP 8개 파일 무결성·OpenAPI JSON 파싱·모든 내부 schema 참조·git diff --check 통과. 실제 Atlas/Firebase 호출 없음.
- 유지한 계약: JWT/Session·API URL·실제 request/response·Guest prepare 결과 분기와 MEMBER provider link 멱등 계약을 유지했다. 오프라인 공유본은 요청 실행/외부 validator를 비활성화하고 운영 주소·인증정보를 넣지 않았다. 비밀값·개인정보 기록 없음.
- 결정사항: 공유본은 정적 snapshot으로 제공하며 배포/기능 활성화 확인을 대신하지 않는다. 생성물은 `build/distributions/identity-swagger.zip`과 `build/frontend-swagger/identity-openapi.json`. 새 API 호출 계약을 생성하지 않았다.
- 수행한 Jira 작업: Atlassian 공식 MCP로 TMI-169 설명·완료 조건을 읽어 OpenAPI 갱신 범위를 확인했다. 댓글·상태·필드를 변경하지 않았다. 댓글 초안 요약은 Swagger 모델 보완/내보내기 파일/918개 회귀·별도 export 검사 성공/화면 및 배포 미확인이다. 자동 등록 없음.
- 위험 요소: 앱 브라우저가 로컬 파일 URL을 차단해 실제 화면 렌더링은 확인하지 못했다. 스키마/정적 ZIP은 검증했으며 수신자가 압축 해제 후 브라우저에서 열어 확인해야 한다. 공유 파일은 API 변경 시 재생성해야 하고 실제 base URL·배포된 코드/활성 기능은 별도 확인이 필요하다. 최초 Chrome 연결은 제공되지 않았고 이후 앱 브라우저 차단을 우회하지 않았다.
- 예상 밖 변경: 이번 작업의 예상 밖 애플리케이션 변경 없음. 이미 존재한 Dockerfile/docker-entrypoint/배포 테스트·bootstrap 문서와 이전 프론트 가이드·부록 및 동시 Codex 기록 변경은 보존했다.
- 다음 작업: 프론트에 ZIP 또는 JSON을 전달하고 수신 환경에서 Swagger 표시를 확인한다. 실제 호출에는 배포된 `/swagger-ui.html`·`/v3/api-docs` 주소와 활성화 상태를 확인한다. 사용자 commit·push 전에 기존 작업과 이번 Swagger 변경 범위를 함께 검토한다. 배포·Jira 쓰기·commit·push는 수행하지 않았다.

## 2026-09-21 — Swagger 응답 예시와 폐기 API 제거

<!-- codex-turn:01a0c2fe-bbca-7ca3-80ae-f3966ec62b86 -->

- 날짜: 2026-09-21
- 브랜치: `develop` (기존 작업 디렉터리 및 동시 변경 보존)
- Jira: 이번 전체 Swagger 예시/폐기 경로 제거에 별도 이슈 지정 없음. 선행 TMI-169의 Guest prepare 계약을 유지한다.
- 작업 목표: 프론트 공유 Swagger의 API 누락 여부를 확인하고 응답 예시를 보강하며, 사용자가 제거를 요청한 회색 폐기 API를 없앤다.
- 변경 파일: 신규 `src/main/java/web/tosunsaeng/identity/global/config/IdentityOpenApiExamples.java`; `ProviderChangeController.java`, `ProviderChangeService.java`, `ProviderLinkService.java`, `UserProfileResponse.java`; `OpenApiSharingTests.java`, `IdentityApplicationTests.java`, `ProviderChangeHttpTests.java`, `ProviderChangeTests.java`, `SecurityIntegrationTests.java`; `docs/swagger/README.md`, `docs/swagger/index.html`; 프론트 가이드·부록, `firebase-provider-unlink-stage-10-runbook.md`, WORKLOG/CURRENT_STATE.
- 구현 내용: `POST /api/v1/auth/firebase/providers/relink/prepare`와 항상 unavailable을 반환하던 service prepare/Permit를 제거했다. 기존 미해결 legacy attempt의 보안 차단·정리 로직은 손대지 않았다. 유효한 사용자 인증 요청은 전체 앱에서 404로 처리되며 미인증 요청은 기존 Security 정책에서 먼저 거절할 수 있다. 실제 배포는 하지 않았다.
- 구현 내용: OpenApiCustomizer에서 실제 응답 DTO와 BaseResponse를 ObjectMapper로 직렬화한 예시를 제공한다. 전체 24개 작업에 정상 응답, Guest 재개/merge·MEMBER/Guest 프로필·연결 상태·최초 start와 재시도 구분·주요 오류 등 기존 포함 총 65개 응답 예시를 제공한다. 예시는 가상 ID/시각/자리표시자만 사용하며 전체 오류 조합을 열거하는 것은 아니다. live OpenAPI와 정적 공유본 모두 동일하게 적용한다.
- 구현 내용: 실제 202인 unlink를 200으로 표시하던 문서를 수정하고, 중첩 record Status의 OpenAPI 이름을 ProviderUnlinkStatus/ProviderLinkStatus로 분리했다. 누락된 Guest 생성 성공 schema를 실제 generic DTO에서 보완하고 UserProfileResponse의 FEDERATED 허용값 누락을 수정했다. 런타임 응답 필드·상태 코드를 바꾼 것이 아니다.
- 실행한 테스트와 결과: 집중 검증 최초 46개 중 제거 경로의 standalone 404 기대 1건 실패. standalone에는 정적 리소스 fallback이 없으므로 핸들러 미등록을 직접 검증하고 전체 Security 통합 테스트로 실제 404를 검증하도록 보완했다. 전체 최초 실행에서 기존 단일 example JSONPath 검증 1건 실패해 새 named examples 경로로 갱신했다. 최종 `./gradlew clean test shareSwagger` 성공: 919 tests/141 suites, failures=0/errors=0/skipped=0, 별도 export 1 test 성공. Gradle cache sandbox 접근 실패 후 승인 실행했다. 실제 Atlas/Firebase 호출 없음.
- 실행한 테스트와 결과: RequestMappingHandlerMapping의 Identity Controller 전체 목록과 OpenAPI를 일치 검증해 23 paths/24 operations 확인. 모든 API의 정상 응답 예시와 schema 존재/필드, 모든 내부 ref, link/unlink 모델 구분, Guest merge의 type-only, PROFILE 재개, 최초 start true/재시도 false를 검사했다. 최종 ZIP 8개 파일 무결성 및 `git diff --check` 통과. deprecated operation 0개 확인.
- 유지한 계약: 폐기 API 제거 외 사용 중인 endpoint·HTTP runtime status·request/response 필드·JWT/세션·Provider link의 MEMBER용 ALREADY_LINKED와 Guest prepare 두 결과를 유지했다. 하위 호환 UserProfileResponse.provider 필드는 제거하지 않았다. 학습 기록 삭제는 Learning Core 미확정 요구사항으로 남겨 API를 추가하지 않았다. 인증 원문/개인정보·Secret 기록 없음.
- 결정사항: Swagger에서 숨기기만 하지 않고 사용자의 추가 요청에 따라 폐기 서버 라우트와 dead stub까지 제거했다. DB 문서 삭제·보안 차단 제거는 범위에 포함하지 않았다. 예시는 업무 계약 원문을 대체하지 않으며 실제 반환 ID/버전/유효 기간을 우선한다.
- 위험 요소: 배포된 서버 변경/feature flag 활성화를 확인하지 않았다. 구 relink 경로를 호출하는 클라이언트는 새 link 흐름으로 전환해야 한다. 이전 앱 브라우저 정책이 로컬 HTML 접근을 차단해 실제 화면 렌더링은 미검증이며 우회하지 않았다. 정적 공유본은 소스 변경 후 재생성이 필요하다.
- 예상 밖 변경: 없음. 기존 Dockerfile/docker-entrypoint/배포 테스트·bootstrap 문서와 선행 Swagger 구성/Guest envelope·프론트 문서 변경을 보존했다. 이번 파일 목록은 위 항목이며 기존 diff와 구분해 확인했다.
- 배포 전/다음 작업: 프론트에 `build/distributions/identity-swagger.zip` 또는 `build/frontend-swagger/identity-openapi.json` 전달, 수신 환경에서 압축 해제 후 Swagger 표시 확인. 구 relink 호출 미사용과 배포 base URL/코드 버전·활성화 여부를 확인한 뒤 사용자가 commit/push한다. 배포·Jira 변경·commit/push 없음.
- Jira 댓글 초안(자동 등록하지 않음): Identity 전체 API 응답 예시 보강 및 폐기 relink 라우트 제거; Controller/서비스 문서 모델, OpenAPI 예시·계약 테스트, 공유 안내/프론트 계약 갱신; 전체 919개와 별도 export 검증 성공; 배포/화면 및 클라이언트 구 경로 미사용 확인 필요.

## 2026-09-21 — 테스트 배포 가능 여부 확인

<!-- codex-turn:01a0c30d-c642-75d2-8ec8-b5233eb982a2 -->

- 브랜치: 기존 develop 문맥. Jira: 지정 없음.
- 작업 목표: 현재 workflow로 바로 테스트 배포 가능한지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: deploy-staging.yml은 main push/수동 실행으로 기존 tosunsaeng-identity-service에 배포하며 현재 task definition을 조회해 이미지를 교체함을 확인. 별도 테스트 대상 배포가 아니므로 바로 실행하지 않도록 안내. 미커밋 및 동시 작업 변경 존재 확인.
- 테스트와 결과: workflow/status 읽기 및 git diff --check. 코드 변경 없고 Gradle 미실행. 이전 테스트를 현재 전체 변경 재검증으로 간주하지 않음.
- 유지한 계약: workflow 수정/추가 및 AWS 변경/배포/commit/push 없음. 비밀정보 미기록. 기존 변경 보존.
- 결정사항: 테스트 실행 역할/Task Definition/Secret 매핑/서비스와 HTTPS 준비 후 확정 revision 이미지를 테스트 대상으로만 배포한다.
- 위험 요소: 기존 workflow 실행은 기존 서비스에 영향을 준다. 실시간 AWS 준비 상태는 이번에 재조회하지 않았다.
- 다음 작업: 테스트 전용 ECS 역할과 Task Definition 준비, 배포 revision 및 운영과 분리된 배포 경로 확인.

## 2026-09-22 — Cloud Billing 반영 및 Firebase 요금제 확인

<!-- codex-turn:01a0c6cb-b374-7aa1-881b-1ee58eabe9c8 -->

- 브랜치: develop. Jira: 지정 없음.
- 작업 목표: 사용자 결제 완료 보고 후 Firebase 프로젝트 결제 연결 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Cloud Billing 화면 새로고침 후 선불 반영과 무료 체험 활성 상태 확인. Firebase to-teacher-firebase의 사용량 및 결제 화면은 여전히 Spark임을 확인. 민감한 결제 상세정보는 기록하지 않았다. 양쪽 탭 유지.
- 테스트와 결과: 콘솔 읽기 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 실제 결제/요금제 업그레이드/프로젝트 연결/IAM 변경 없음. Secret/Token/Password/Key/전체 MongoDB URI 기록 없음. commit/push 없음.
- 결정사항: 결제 계정 활성화와 Firebase 프로젝트 연결은 별개이며 기존 프로젝트를 해당 결제 계정에 연결하는 후속 절차가 필요하다.
- 위험 요소: 연결 가능한 계정 목록과 IAM 권한은 아직 미확인. Blaze 사용량 과금이 가능하며 예산 알림은 비용 차단 기능이 아니다.
- 다음 작업: 사용자가 Firebase 업그레이드에서 올바른 결제 계정을 선택해 연결 완료 후 Blaze 상태 확인. Identity Platform/Kakao 설정은 별도 확인.

## 2026-09-22 — Firebase 결제 연결 화면 확인 및 권한 경계

- 브랜치: develop. Jira: 지정 없음.
- 작업 목표: 사용자 요청에 따라 기존 Firebase 프로젝트의 결제 연결 준비.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Firebase Blaze 업그레이드 선택 창을 열었으나 연결 가능한 Cloud Billing 계정이 없다는 UI 확인. 최종 연결/과금 승인/IAM 변경은 수행하지 않고 화면 유지.
- 테스트와 결과: UI 상태 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀정보/개인정보 기록 없음, 외부 리소스 변경/commit/push 없음.
- 결정사항: 새 결제 계정을 추가로 만들지 않는다. 현재 Firebase 로그인 계정의 기존 결제 계정 접근 권한 또는 로그인 계정 불일치를 먼저 확인한다.
- 위험 요소: 원인은 권한 상세 조회 전 미확정. 연결은 사용량 과금과 관련되므로 최종 승인은 사용자 직접 수행 필요.
- 다음 작업: 기존 결제 계정의 권한과 프로젝트 접근 주체 확인 후 필요한 최소 권한 변경을 별도 승인받고 연결 재시도.

## 2026-09-22 — Firebase 결제 연결 준비 식별 기록 보완

<!-- codex-turn:01a0c6cd-6695-7cd2-aed7-32c27ddc67ff -->

- 브랜치: develop. Jira: 지정 없음.
- 작업 목표: 이번 결제 연결 준비 작업의 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 연결 가능한 결제 계정이 없는 UI 상태와 로그인/권한 확인 필요성을 기록했다. 연결 완료로 처리하지 않는다.
- 테스트와 결과: git diff --check. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 미기록, 외부 설정 변경 및 commit/push 없음.
- 결정사항: 기존 결제 계정 접근 권한부터 확인한다.
- 위험 요소: 권한 상세 원인은 미확정이며 최종 과금 승인은 사용자 직접 수행 필요.
- 다음 작업: 결제 계정 권한 확인 후 연결 재시도.

## 2026-09-22 — 결제 계정 IAM 확인 및 최소 역할 저장 준비

<!-- codex-turn:01a0c6db-925e-7461-8daa-6fd08ea964d4 -->

- 브랜치: develop. Jira: 지정 없음.
- 작업 목표: Firebase 관리 계정의 결제 계정 접근 권한 확인 및 누락 시 부여 준비.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 결제 계정 IAM 패널에서 관리자 한 명만 존재하고 Firebase 관리 계정이 없는 것을 확인했다. 주 구성원 추가 양식에 해당 관리 계정과 결제 계정 사용자 역할을 선택했다. 저장 직전 사용자 확인 대기이며 아직 IAM 변경 없음. 양쪽 탭 유지.
- 테스트와 결과: 콘솔 IAM 목록/선택 역할 확인, git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 실제 계정 주소/결제 상세/비밀정보 기록 없음. 과거 기록 보존. 결제 연결/요금제 변경/commit/push 없음.
- 결정사항: 관리자 역할 대신 프로젝트 연결용 Billing Account User만 부여 예정. 권한은 결제 계정 수준이며 단일 Firebase 프로젝트에 한정된 권한은 아니다.
- 위험 요소: 이 권한으로 사용자가 권한을 가진 다른 프로젝트도 연결할 수 있어 비용 발생 가능성을 설명하고 저장 직전 승인을 요청한다.
- 다음 작업: 사용자 확인 후 IAM 저장/반영 확인, Firebase 결제 계정 목록 재조회. 최종 Blaze 연결은 사용자 직접 수행.

## 2026-09-22 — 사용자 결제 권한 저장 결과 확인

<!-- codex-turn:01a0c6db-925e-7461-8daa-6fd08ea964d4 -->

- 브랜치: develop (직전 확인). Jira: 지정 없음.
- 작업 목표: 사용자 직접 저장한 IAM 권한 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Firebase 관리 계정이 결제 계정 사용자 역할로 추가됐으며 정책 업데이트 성공 알림을 확인. Firebase 새로고침 후 Blaze 선택 창에서는 아직 사용 가능한 계정 없음. 두 탭 유지.
- 테스트와 결과: 콘솔 읽기 확인 및 git diff --check. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 계정 주소 및 비밀정보 미기록. 추가 IAM 변경/과금 연결/commit/push 없음.
- 결정사항: IAM 저장 완료와 Firebase 목록 반영은 구분한다. UI가 안내하는 권한 전파 시간을 먼저 고려하고 반복 권한 부여는 하지 않는다.
- 위험 요소: 계정 목록 미반영 원인은 아직 확정하지 않았다. 프로젝트 결제 연결 권한 등 추가 확인이 필요할 수 있다.
- 다음 작업: 잠시 후 Firebase 결제 계정 선택 재확인; 계속 미노출이면 프로젝트 권한 확인.

## 2026-09-22 — IAM 저장 확인 작업 식별 보완

<!-- codex-turn:01a0c6de-40ff-7f52-9c41-ed67b8cffd2a -->

- 브랜치: develop (직전 확인). Jira: 지정 없음.
- 작업 목표: 이번 권한 저장 확인의 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 IAM 저장 성공과 Firebase 계정 목록 미반영 상태를 구분해 기록했다. 추가 외부 작업 없음.
- 테스트와 결과: git diff --check. 기록만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 미기록, commit/push 없음.
- 결정사항: 권한 전파 후 재확인하며 중복 부여하지 않는다.
- 위험 요소: 목록 미반영 원인 및 실제 결제 연결은 미확인.
- 다음 작업: Firebase 결제 계정 목록 재조회.

## 2026-09-22 — Firebase 결제 계정 선택 가능 상태 확인

- 브랜치: develop (직전 확인). Jira: 지정 없음.
- 작업 목표: 사용자 화면이 정상 연결 단계인지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Firebase 업그레이드 창에 기존 Cloud Billing 계정이 선택 가능한 항목으로 표시됨을 확인했다. 최종 연결이나 Blaze 전환 완료 화면은 아니며 선택/과금 승인은 수행하지 않았다. 탭 유지.
- 테스트와 결과: UI 읽기 확인 및 git diff --check. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 기록/기존 변경 보존. 비밀정보 미기록. 외부 설정 변경/commit/push 없음.
- 결정사항: 권한 반영 완료와 결제 연결 완료를 구분한다.
- 위험 요소: 최종 연결 후 사용량 과금 가능. 아직 Blaze 전환 완료 확인 없음.
- 다음 작업: 사용자가 표시된 결제 계정 선택 및 최종 안내 확인 후 연결을 완료하고 Blaze 상태 확인.

## 2026-09-22 — 결제 계정 표시 확인 식별 기록 보완

<!-- codex-turn:01a0c6e0-828d-7cc3-bc67-51a288311df5 -->

- 브랜치: develop (직전 확인). Jira: 지정 없음.
- 작업 목표: 이번 확인 작업의 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Firebase에서 기존 결제 계정을 선택할 수 있음을 확인한 결과를 기록했다. 실제 연결 완료는 아직 미확인이다.
- 테스트와 결과: git diff --check. 기록만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 미기록, 외부 변경 및 commit/push 없음.
- 결정사항: 권한 반영과 Blaze 연결 완료를 구분한다.
- 위험 요소: 최종 연결 이후 사용량 과금 가능.
- 다음 작업: 사용자 최종 연결 후 Blaze 상태 확인.

## 2026-09-22 — Blaze 연결 완료 확인 및 다음 단계 안내

<!-- codex-turn:01a0c6e4-3a39-76d1-b8e9-62fd9d3ec9a1 -->

- 브랜치: develop. Jira: 지정 없음.
- 작업 목표: 사용자 결제 연결 완료 확인 및 인증 테스트 준비 순서 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Firebase 사용량 및 결제 화면에서 기존 결제 계정 연결 및 Blaze 표시 확인. 현재 Firebase 계정은 결제 관리자가 아니어서 예산 데이터를 볼 수 없다는 안내 확인. 예산 설정 여부는 단정하지 않는다. 탭 유지.
- 테스트와 결과: UI 읽기 확인 및 git diff --check. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 결제 상세/개인정보/비밀값 미기록. 외부 변경/commit/push 없음.
- 결정사항: 결제 관리자 계정에서 예산 알림 확인, SMS 지역 정책 및 테스트 전화번호 확인, 테스트 ECS 준비 순서 권장. Google/전화번호 기본 검증에 Kakao OIDC를 선행 요구하지 않는다.
- 위험 요소: 예산 알림은 전체 비용 차단 기능이 아니다. 실 SMS 동작과 Identity Platform/Kakao 설정 및 서버 배포는 미검증.
- 다음 작업: 결제 관리자 계정으로 예산 알림 상태 확인 후 Firebase 인증 설정 및 테스트 ECS 준비.

## 2026-09-22 — Firebase 예산 알림 생성 및 SMS 정책 저장 준비

- 브랜치: develop. Jira: 지정 없음.
- 작업 목표: 사용자 승인한 예산 알림 및 전화 인증 설정 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: to-teacher-firebase 단일 프로젝트/모든 서비스/월 10000원/실제 비용 50·90·100%/결제 관리자 및 사용자 이메일 알림 예산을 생성했다. 할인·프로모션 등 크레딧 차감 없이 비용을 감지하도록 보정 저장 후 성공 및 목록 확인. 알림 전용이며 지출 한도나 Pub/Sub는 설정하지 않았다.
- 인증 확인: 최초 provider 화면에서 Phone 미표시/추가 양식 OFF가 관측됐으나 새로고침 후 Google·Phone·Apple 모두 enabled 확인. agent는 Phone 활성화 저장/약관 동의를 하지 않았다. SMS 허용 목록이 비어 있어 대한민국만 선택했으며 정책 저장 직전 승인 대기. 가상 번호/인증코드는 열거나 생성하지 않아 등록 상태 미확인.
- 테스트와 결과: 콘솔 예산 생성·저장 성공 및 SMS 미저장 화면 확인, git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 실제 전화번호/인증코드/계정 주소/비밀정보 기록 없음. 추가 IAM/Identity Platform 업그레이드/배포/commit/push 없음.
- 결정사항: 예산은 크레딧 차감 전 사용 비용 관찰용이며 청구액이나 자동 차단 한도가 아니다. SMS 허용 범위 확대는 저장 직전 확인 후 진행한다.
- 위험 요소: SMS 실제 발송 과금 가능. Phone 초기/최종 상태 차이 원인은 미확정. 가상 번호 및 실제 앱 인증/연결 E2E 미검증.
- 다음 작업: 사용자 확인 후 대한민국만 SMS 허용 정책 저장 및 재조회. 기존 가상 번호는 원문 노출 없이 사용자 확인 후 앱 테스트 진행. 탭 유지.

## 2026-09-22 — 예산 및 SMS 준비 작업 식별 보완

<!-- codex-turn:01a0c6e5-ce55-7301-bf91-b42dd3bc7966 -->

- 브랜치: develop (직전 확인). Jira: 지정 없음.
- 작업 목표: 이번 예산/SMS 설정 작업의 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 예산 알림 저장 완료와 대한민국 SMS 허용 정책 미저장 상태를 기록했다. 이번 보완에서 외부 변경 없음.
- 테스트와 결과: git diff --check. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 미기록, commit/push 없음.
- 결정사항: SMS 정책 저장은 사용자 확인 후 진행한다.
- 위험 요소: 실제 SMS 과금 가능, 테스트 번호 및 앱 E2E 미검증.
- 다음 작업: 사용자 승인 후 SMS 정책 저장 및 확인.

## 2026-09-22 — 대한민국 SMS 허용 정책 저장

<!-- codex-turn:01a0c6ec-4fd5-7422-ab54-b6287363a51a -->

- 브랜치: develop. Jira: 지정 없음.
- 작업 목표: 사용자 명시적 승인에 따라 대한민국만 SMS 허용 정책 저장.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Firebase to-teacher-firebase의 SMS 리전 정책에서 허용 및 KR 단독 선택을 확인하고 저장했다. 처리 완료 후 변경사항 삭제 버튼 제거와 저장 비활성 상태 확인. 탭 유지.
- 테스트와 결과: 콘솔 저장 반영 확인 및 git diff --check. 문서 기록만 변경하여 Gradle 미실행. SMS 발송 및 앱 인증 테스트는 하지 않았다.
- 유지한 계약: 전화번호/인증코드/비밀값 미조회·미기록. 다른 provider/IAM/요금제 변경 없음. commit/push 없음.
- 결정사항: 대한민국만 SMS 허용, 다른 국가 추가 없음.
- 위험 요소: 실제 SMS 발송 시 과금 가능하며 예산 알림은 차단 장치가 아니다. 가상 테스트 번호 등록 및 앱 E2E는 미검증.
- 다음 작업: 기존 테스트 전화번호 등록 상태 확인 후 Google 로그인과 동일 UID 전화 연결 테스트 및 테스트 ECS 준비.

## 2026-09-22 — 테스트 Identity 서버 준비 순서 안내

<!-- codex-turn:01a0c732-25d8-7e73-9095-31dd4fc63f6e -->

- 브랜치: develop. Jira: 지정 없음.
- 작업 목표: 완료된 Firebase/Secret 준비와 남은 테스트 서버 배포 작업을 구분한다.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 현재 status, bootstrap 문서와 기존 배포 workflow 대상 확인. 테스트 주소/issuer, IAM 실행 역할, 이미지 revision, Task Definition, 서비스/ALB/DNS/인증서, 서버 및 앱 검증 순서를 설명한다.
- 테스트와 결과: 읽기 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 기존 운영 서비스 유지, 비밀값 미기록, 외부 변경/배포/commit/push 및 workflow 추가 없음. 사용자/동시 작업 변경 보존.
- 결정사항: 다음 실제 준비 작업은 테스트 실행 역할 생성/최소 권한 설정이며 AWS 변경은 별도 진행한다. 테스트용 이름/주소는 제안이지 생성 완료가 아니다.
- 위험 요소: 미커밋 변경 존재. 기존 workflow는 기존 서비스 대상이다. 실제 테스트 ECS 준비 상태는 이번에 재조회하지 않았다.
- 다음 작업: 테스트용 주소·JWT issuer를 확정하고 IAM 실행 역할 및 Task Definition 준비; 새 이미지와 별도 서비스 배포 후 DB/인증 E2E 검증.

## 2026-09-22 — 테스트 도메인 DNS 관리 위치 확인

<!-- codex-turn:01a0c735-a39f-7392-b717-2a8077edc548 -->

- 브랜치: develop. Jira: 지정 없음.
- 작업 목표: 테스트 HTTPS 주소 준비 시 가비아와 AWS 역할 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 공개 DNS에서 to-teacher.com NS가 가비아인 것을 확인했다. 제안된 identity-test 호스트의 A/CNAME 조회 결과는 비어 있었다. 가비아 DNS 인증/서비스 레코드와 AWS ACM/ALB 연결 절차를 설명한다.
- 테스트와 결과: 최초 sandbox DNS 조회 실패 후 승인된 공개 DNS 조회 성공. git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 네임서버/DNS/AWS 변경 및 배포 없음. 비밀정보 기록, commit/push 없음.
- 결정사항: 기존 네임서버를 변경하거나 새 도메인을 구매할 필요 없이 테스트 서브도메인을 준비한다. ACM 기존 인증서 적용 가능 여부를 먼저 확인한다.
- 위험 요소: ACM/ALB 실시간 상태는 미조회. DNS A/CNAME 부재만 확인했으며 실제 HTTPS 연결은 아직 없다.
- 다음 작업: 서울 ACM에서 기존 인증서의 테스트 호스트 포함 여부 확인 후 필요시 인증서 준비와 가비아 DNS 검증, 테스트 ALB 라우팅 준비.

## 2026-09-22 — ACM 조회 시 AWS 세션 만료 확인

<!-- codex-turn:01a0c73d-0890-7892-b659-cff669962d5f -->

- 브랜치: develop. Jira: 지정 없음.
- 작업 목표: 서울 ACM 기존 인증서의 테스트 주소 지원 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: AWS 콘솔에서 ACM 목록으로 이동했으나 세션 만료 및 재로그인 화면 확인. 인증서 목록을 조회하지 못했으며 사용자 로그인 대기 상태로 탭 유지.
- 테스트와 결과: 로그인 화면 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 자격증명/인증 URL의 세션값 미기록. 인증서 발급/DNS/ALB 변경, commit/push 없음.
- 결정사항: 로그인 완료 전 기존 인증서 재사용 여부를 단정하지 않는다.
- 위험 요소: 인증서 존재/상태/도메인 및 ALB 연결 미확인.
- 다음 작업: 사용자 재로그인 후 서울 ACM 목록과 인증서 도메인 확인.

## 2026-09-22 — 사용자 지정 ISB 로그인 페이지 다시 열기

- 브랜치: develop (직전 확인).
- 작업 목표: 사용자 요청 URL 다시 열기.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 이전 AWS 탭이 세션에서 사라져 사용자 지정 ISB URL을 새 탭으로 열었다. AWS 로그인 화면으로 연결됨을 확인하고 탭 유지 처리했다.
- 테스트와 결과: 로그인 화면 표시 확인, git diff --check. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 자격증명 및 로그인 세션 URL 미기록. 로그인 제출/외부 설정 변경/commit/push 없음.
- 결정사항: 사용자 직접 로그인 후 ACM 조회를 재개한다.
- 위험 요소: 인증서 상태 아직 미확인.
- 다음 작업: 로그인 완료 후 서울 ACM 인증서 조회.

## 2026-09-22 — 서울 ACM 테스트 도메인 인증서 적용 가능 여부 확인

<!-- codex-turn:01a0c73e-a183-7653-938c-90b2359ae202 -->

- 브랜치: develop. Jira: 지정 없음.
- 작업 목표: 로그인 후 기존 인증서가 제안된 테스트 주소를 지원하는지 읽기 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 서울 ACM의 발급/사용 중 인증서 한 건 확인. 도메인은 identity-staging.to-teacher.com 및 api-staging.to-teacher.com뿐이며 wildcard와 identity-test.to-teacher.com은 없다. 연결 리소스는 tosunsaeng-staging-alb로 표시됐다.
- 테스트와 결과: 콘솔 인증서 상세 확인. 코드 변경 없어 Gradle 미실행. git diff --check 실행.
- 유지한 계약: 인증서/DNS/ALB/서비스 변경 및 commit/push 없음. 비밀값 미조회·미기록. 다른 작업 변경 보존.
- 결정사항: 기존 인증서는 유지하고 identity-test.to-teacher.com 전용 서울 ACM 공개 인증서 별도 발급을 권장한다. 이번에는 발급하지 않았다.
- 위험 요소: ALB listener/rule 상세는 미조회. 테스트 주소 인증서와 DNS 연결은 아직 준비되지 않았다.
- 다음 작업: 사용자 요청 후 테스트 인증서 요청, 가비아 DNS 검증 CNAME 등록, ALB 추가 인증서 및 테스트 호스트 라우팅 준비.

## 2026-09-22 — 테스트 ACM 인증서 요청 방법 안내

- 브랜치: develop. Jira: 지정 없음.
- 작업 목표: 사용자가 직접 테스트 인증서를 요청할 수 있도록 선택 항목 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 서울 리전 공개 인증서, identity-test.to-teacher.com 단일 도메인, DNS 검증, RSA 2048, 내보내기 비활성 선택 및 이후 가비아 CNAME 검증 절차 안내.
- 테스트와 결과: 문서 변경으로 Gradle 미실행, git diff --check 실행.
- 유지한 계약: 기존 인증서/서비스 유지. 외부 설정 변경 및 commit/push 없음.
- 결정사항: ALB용 비내보내기 공개 인증서를 사용하며 wildcard는 추가하지 않는다.
- 위험 요소: 실제 요청/발급 및 DNS 전파는 아직 확인하지 않았다.
- 다음 작업: 사용자 인증서 요청 후 생성된 DNS 검증 레코드 확인 및 가비아 등록.

## 2026-09-22 — 인증서 요청 안내 작업 식별 기록 보완

<!-- codex-turn:01a0c741-c3d2-7551-89a2-d8a3111e9630 -->

- 브랜치: develop. Jira: 지정 없음.
- 작업 목표: 이번 인증서 요청 안내의 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 서울 ACM에서 테스트 단일 도메인의 비내보내기 공개 인증서 요청과 가비아 DNS 검증 방법을 안내했다. 외부 생성/변경은 수행하지 않았다.
- 테스트와 결과: git diff --check 실행. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 기존 인증서/서비스 유지, Secret 미기록, commit/push 없음.
- 결정사항: DNS 검증 및 RSA 2048 사용 안내.
- 위험 요소: 실제 인증서 요청과 발급은 미확인.
- 다음 작업: 사용자 요청 완료 후 DNS 검증 CNAME 확인.

## 2026-09-22 — Identity 운영 안정성 및 공통 요청 로그 누락 진단

<!-- codex-turn:01a0c764-4820-70f0-96e6-b68ede8bca35 -->

- 브랜치: develop. Jira: 지정 없음.
- 작업 목표: 서버 오류 위험·예외 처리·로그 수집을 코드/전체 테스트/실행 중 AWS 서비스 읽기로 점검.
- 변경 파일: docs/codex/identity-operability-review-2026-09-22.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 임시 진단 테스트는 생성·실행 후 제거했으며 제품/기존 테스트 변경 없음.
- 수행 내용: ECS task definition 23, 이미지 태그 7f2188df97d2adc5faef8ddec6cb6ffa6ee8b0d4, running 1 및 ALB 정상 1 확인. awslogs 수집 연결 및 최근 24시간 서비스 성공 로그 15건 확인, 공통 HTTP 이벤트는 없었다.
- 확인한 결함: RequestLoggingFilter가 MDC와 fluent key-value에 requestId를 중복 기록해 Spring Boot ECS JSON writer가 IllegalStateException을 발생시키고 CONSOLE appender가 이벤트를 버린다. 실제 logger INFO/additive=true에서 200/401/500 모두 재현했으며 대조군 service INFO는 출력됐다. 운영 태그에 해당하는 로컬 Git 코드도 동일 결함을 포함한다.
- 추가 검토: LogCapture는 콘솔 직렬화를 우회해 결함을 놓침. 일부 DB 장애의 원인 없는 503 변환, fence/recovery OFF 재발급 부분 실패 위험, CI SENTRY_RELEASE의 runtime 전달 누락 확인/정리.
- 테스트와 결과: sandbox Gradle lock 제한 후 승인된 ./gradlew clean test 성공. 임시 콘솔 진단에서 중복 requestId 오류 3건 assertion 성공. 진단 파일 제거 후 전체 재실행도 141 suites/919 tests, failures=0/errors=0/skipped=0. git diff --check 통과. 실제 DB/Provider/운영 장애 주입 없음.
- 유지한 계약: API/JWT/로그 민감값 비노출 계약 변경 없음. AWS 설정/인증서/DNS 변경·배포·commit/push 없음. 사용자 및 동시 작업 미커밋 변경 보존. 운영 사용자 식별값·Secret·Token·개인정보 미기록.
- 결정사항: 수정 승인 전 제품 코드는 유지. main 별도 로그 hotfix와 실제 JSON console 회귀 테스트를 최우선 권장. 전체 develop의 기존 서비스 배포는 권장하지 않는다.
- 위험 요소: 전체 테스트 성공이 로그 정상 또는 무장애 보장이 아님. Firebase E2E, Atlas rollback, 실제 Sentry/경보 수신은 미검증. 운영 JVM 내부 appender status는 직접 조회하지 않았다.
- 참고: 사용자 인증서 탭 유지 중 identity-test.to-teacher.com 신규 인증서가 검증 대기 상태임을 확인. agent는 발급/변경하지 않았다.
- 다음 작업: 사용자 승인 후 로그 hotfix 구현·테스트, 사용자 commit/push 및 별도 배포 후 CloudWatch 검증. 테스트 인증서 DNS 검증/환경 준비 계속.

## 2026-09-22 — develop 기준 운영성 검토 재확인

- 브랜치: develop, HEAD 5f37c13563f4d777e9870e8668f13164ed85fe9c. Jira: 지정 없음.
- 작업 목표: main이 아닌 develop 커밋과 미커밋 작업 트리를 구분해 이전 발견 사항 재확인.
- 변경 파일: docs/codex/identity-operability-review-2026-09-22.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: git show/diff로 observability/application.yml/session/workflow가 develop 커밋과 동일함을 확인. requestId 중복은 develop에도 존재한다. 재발급 recovery의 fence 필수 의존 확인; 보호 구현은 있으나 기본 OFF라는 구분 명확화. 미커밋 build.gradle 변경은 문서 task뿐이며 로깅 의존 버전 변경 없음.
- 테스트와 결과: 제품 변경 없고 직전 develop 작업 트리 919 tests/141 suites 성공 및 콘솔 200/401/500 누락 재현 결과 재사용. 이번 테스트 재실행 없음. git diff --check 실행. 순수 develop 커밋 snapshot 테스트 및 원격 fetch는 미수행.
- 유지한 계약: 제품/API/배포 변경 없음. 사용자 미커밋 변경 보존, secret/개인정보 미기록, commit/push 없음.
- 결정사항: 수정 시 develop의 로그 결함과 실제 JSON 출력 회귀 검증이 최우선. 세션 보호는 신규 개발 누락이 아니라 활성 설정 검토 대상이다.
- 위험 요소: 현재 작업 트리 테스트 통과가 순수 커밋 전체 검증이나 운영 무장애 보장을 의미하지 않음.
- 다음 작업: 사용자 승인 시 develop에서 requestId 중복 제거와 콘솔 회귀 테스트 구현, 원인 없는 503 진단 보강 검토.

## 2026-09-22 — develop 재검토 작업 식별 기록 보완

<!-- codex-turn:01a0c76d-0b9c-7263-9daf-eb601b7964ee -->

- 브랜치: develop. Jira: 지정 없음.
- 작업 목표: develop 기준 재검토의 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: develop 커밋에도 동일한 requestId 중복 결함이 존재하며 직전 919개 테스트는 미커밋 변경 포함 develop 작업 트리 결과임을 기록했다.
- 테스트와 결과: 문서 보완만 수행, 추가 테스트 없음. git diff --check 실행.
- 유지한 계약: 제품 코드/외부 설정/배포 변경 없음. Secret 미기록, 사용자 변경 보존.
- 결정사항: 수정 승인 대기.
- 위험 요소: 실제 콘솔 회귀 검증 추가 전 기존 테스트 성공만으로 로그 정상을 보장할 수 없음.
- 다음 작업: 승인 후 develop 로그 결함 수정과 콘솔 회귀 테스트 추가.

## 2026-09-22 — 순수 develop 커밋 인증·세션 코드 리뷰

- 브랜치: develop, 기준 커밋 5f37c13563f4d777e9870e8668f13164ed85fe9c. Jira: 지정 없음.
- 작업 목표: 미커밋 변경과 분리해 develop의 주요 인증/회원가입/세션/SNS 변경/JWT/로그 경로 리뷰.
- 변경 파일: docs/codex/develop-code-review-2026-09-22.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 저장소 제품 코드/기존 테스트 변경 없음.
- 수행 내용: git archive 임시 사본으로 순수 커밋 검증. R1 requestId 중복 로그 누락, R2 retained JWKS와 달리 active key만 허용하는 Identity decoder의 회전 호환 결함, R3 DB 원인 소실 503, R4 runtime SENTRY_RELEASE 전달 누락을 우선순위별 정리했다.
- 테스트와 결과: 임시 사본 ./gradlew clean test 성공(139 suites/896 tests, failures=0/errors=0/skipped=0). 임시 진단 2개로 공통 로그 200/401/500 유실 및 만료 전 구키 MEMBER 토큰의 회전 후 검증 거절 재현. 진단 성공은 결함 존재를 확인한 것이다. git diff --check 실행.
- 유지한 계약: API/JWT/인증 정책 변경 없음. 외부 Provider/DB/운영 호출·배포·commit/push 없음. 실제 키/토큰/개인정보 미기록. 사용자 미커밋 변경 보존.
- 결정사항: 수정은 승인 대기. R1 로그 → R2 키 회전 → R3 진단 분류 → R4 배포 식별 순 권장. 기존 flag OFF 위험/수동 reconciliation 정책은 신규 결함과 구분.
- 위험 요소: 원격 develop 최신 여부/실제 외부 E2E/경합 부하 미검증. 순수 커밋 896 tests와 미커밋 포함 이전 919 tests를 혼동하지 않는다.
- 다음 작업: 사용자 요청 시 develop 결함 수정 및 회귀 검증; 별도 테스트 환경에서 Mongo/Firebase/CloudWatch 배포 검증.

## 2026-09-22 — develop 코드 리뷰 작업 식별 기록 보완

<!-- codex-turn:01a0c76f-42cf-7c82-b086-06f227192ef7 -->

- 브랜치: develop. Jira: 지정 없음.
- 작업 목표: 순수 develop 코드 리뷰의 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 공통 로그 누락, 키 회전 시 기존 토큰 거절, DB 장애 원인 소실, Sentry release 전달 누락을 리뷰 문서에 정리한 상태 확인.
- 테스트와 결과: 순수 커밋 전체 896 tests 성공 및 진단 2건 재현 결과 유지. 이번 기록 보완은 테스트 미실행, git diff --check 실행.
- 유지한 계약: 제품 코드/외부 설정 변경 없음. Secret 미기록, 사용자 변경 보존.
- 결정사항: 수정 승인 대기.
- 위험 요소: 외부 Firebase/Atlas E2E 미검증.
- 다음 작업: 사용자 요청 후 develop 결함 수정 및 회귀 테스트 추가.

## 2026-09-22 — develop 운영 안정성 4건 수정 계획 작성

<!-- codex-turn:01a0c777-e6a7-7963-9a79-0897e2e33423 -->

- 브랜치: develop. Jira: 미지정.
- 작업 목표: 리뷰 R1~R4의 수정 설계·테스트·배포 확인·롤백 기준을 계획서로 작성.
- 변경 파일: docs/contracts/identity-operability-remediation-plan.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: MDC 단일 requestId 및 실제 JSON console 검증, 로컬 신뢰 공개키 집합 공용화/active-only 서명, 원문 없는 내부 장애 분류, 기존 workflow runtime release mapping 계획을 작성했다. 단계적 키 전환/구키 보존/안전한 rollback과 기능 flag 불변·사용자 변경 보존도 명시했다.
- 테스트와 결과: 계획 문서 작성으로 Gradle 미실행. 기존 코드/설정/리뷰 근거를 읽기 확인하고 git diff --check 실행. 이전 순수 develop 896 tests 및 진단 재현 결과를 계획 근거로만 사용했다.
- 유지한 계약: 사용자/API/JWT/refresh/feature flag 계약 변경 없음. 제품 코드·AWS·Jira·배포·commit/push 변경 없음. Secret/Token/실제 키/개인정보 미기록.
- 결정사항: 구현 승인 대기. R3는 기존 CloudWatch 진단 보강을 우선하며 새 Sentry 503 수집/경보 시스템은 제외. R4는 기존 정의 보완만 예정하고 새 배포 Actions는 추가하지 않는다.
- 위험 요소: 로깅 복구 후 로그량 증가, 키 교체 전 사전 신뢰 배포/실제 TTL·skew·캐시 확인 필요. 기존 배포 workflow는 테스트 서비스가 아닌 기존 서비스 대상이다.
- 다음 작업: 사용자 계획 승인 후 R1→R2→R3→R4 구현과 전체 테스트. 실제 배포/키 교체는 별도 확인·승인 후 진행.

## 2026-09-22 — 운영 안정성 수정 계획 사용자 설명

<!-- codex-turn:01a0c784-86b7-79e1-a022-6db91de18a0c -->

- 브랜치: develop. Jira: 미지정.
- 작업 목표: 수정 계획의 네 항목을 원인·변경 동작·사용자 영향·검증 순서 중심으로 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 계획서 전체를 확인하고 요청 로그 누락 복구, 키 교체 호환성, 안전한 DB 장애 진단, 실행 버전 식별을 설명했다. 이번 키 교체와 Refresh Token 회전은 다른 개념임을 구분한다.
- 테스트와 결과: 설명/기록만으로 Gradle 미실행. 계획서 읽기 확인 및 git diff --check 수행.
- 유지한 계약: API/JWT/세션/기능 flag 불변. 제품 코드 및 외부 설정 변경 없음. 사용자 미커밋 변경 보존.
- 결정사항: 구현 승인 대기 유지. 새 회원 정책이나 프론트 수정은 요구하지 않는다.
- 위험 요소: 자동 테스트만으로 운영 안전을 보장하지 않으며 테스트 배포 후 실제 로그·인증·Sentry 확인 필요. 기존 배포 workflow의 대상은 테스트 서비스가 아니다.
- 다음 작업: 승인 후 항목별 구현/회귀 테스트, 별도 테스트 배포 검증.

## 2026-09-22 — 운영 안정성 수정의 TMI-136 하위 이슈 생성 준비

- 브랜치: develop.
- Jira: TMI-136 (상위 에픽).
- 작업 목표: 사용자 요청에 따라 코드 수정 전에 하위 이슈 생성안 준비.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 공식 Atlassian MCP로 TMI-136의 제목(sns 로그인), 에픽 유형, 해야 할 일 상태 및 프로젝트 이슈 유형 조회. R1~R4를 묶는 하위 일반 작업 1건의 범위·완료 조건을 생성안으로 제시.
- Jira 작업/승인: 읽기 조회만 수행. 생성 내용 승인 대기, 댓글 등록 및 상태 변경 없음. 구현 요청은 접수했으나 Jira 생성 선행 조건에 따라 착수 대기.
- 테스트와 결과: 코드 미변경으로 Gradle 미실행. git diff --check 수행.
- 유지한 계약: API/JWT/기능 flag/외부 설정 불변. 사용자 미커밋 변경 보존.
- 결정사항: 에픽의 하위 항목은 Sub-task가 아닌 일반 작업으로 제안. 기존 수정 계획 전체를 범위로 유지.
- 위험 요소: 생성 전 내용 승인이 필요하며 배포/실제 키 교체는 구현 승인과 별도다.
- 다음 작업: 생성안 승인 후 하위 작업 생성·조회, 신규 Jira 키 기록 후 코드 수정 및 회귀 테스트. 작업 종료 댓글 초안은 구현 완료 시 작성하며 자동 등록하지 않는다.

## 2026-09-22 — TMI-136 하위 이슈 준비 작업 식별 기록 보완

<!-- codex-turn:01a0c786-950f-7712-8312-46a83682576e -->

- 브랜치: develop. Jira: TMI-136.
- 작업 목표: 현재 작업 식별자를 포함해 Jira 생성 준비 상태 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 상위 에픽 조회 및 운영 안정성 수정 하위 작업 생성안 제시 완료 상태 기록. 과거 WORKLOG 내용은 변경하지 않았다.
- 테스트와 결과: 기록만 변경하여 Gradle 미실행, git diff --check 수행.
- 유지한 계약: 제품 코드/API/JWT/외부 설정 변경 없음. 민감정보 미기록.
- 결정사항: Jira 생성 내용 승인 대기. Jira 쓰기·댓글·상태 전환 없음.
- 위험 요소: 구현과 실제 배포는 별도이며 신규 이슈는 아직 생성되지 않았다.
- 다음 작업: 승인 후 하위 작업 생성 및 요청된 코드 수정 진행.

## 2026-09-22 — TMI-136 하위 운영 안정성 작업 생성

<!-- codex-turn:01a0c787-e3d2-7e32-9ae0-34f490eb54ca -->

- 브랜치: develop (이번은 Jira 생성만 수행, 작업 브랜치 생성 없음).
- Jira: TMI-176. 상위 에픽: TMI-136.
- 작업 목표: 승인된 운영 안정성 수정 범위를 하위 작업 1건으로 생성.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md, docs/contracts/identity-operability-remediation-plan.md의 Jira 참조.
- 수행 내용: 공식 Atlassian MCP로 일반 작업 생성 후 parent=TMI-136 및 해야 할 일 상태를 재조회 확인했다. R1~R4 범위·완료 조건·계약 유지·배포 제외를 등록했다.
- Jira 작업/승인: 사용자의 Jira 생성만 요청에 따라 생성 승인 적용. 추가 댓글 없음(목적 해당 없음), 별도 상태 전환 없음. 초기 상태는 해야 할 일.
- 테스트와 결과: 제품 코드 미변경으로 Gradle 미실행. Jira 생성/상위 관계 읽기 검증 및 git diff --check 수행.
- 유지한 계약: API/JWT/세션/기능 flag 불변. 사용자 미커밋 변경 보존, 실제 외부 인프라 변경 없음.
- 결정사항: 이번에는 Jira만 생성하며 구현은 시작하지 않는다.
- 위험 요소: 계획상의 결함은 아직 수정되지 않았으며 배포/실제 키 교체는 별도 승인 대상이다.
- 다음 작업: 사용자 구현 요청 시 TMI-176 다시 읽고 구현·회귀 검증. Jira 댓글 초안: 작업 생성 및 계획 연결 완료, 제품 코드/테스트 변경 없음, R1~R4 구현과 배포 검증은 미완료(등록하지 않음).

## 2026-09-22 — TMI-176 Identity 운영 안정성 구현

<!-- codex-turn:01a0c78b-1ed3-79a2-83d8-7b5987912baf -->

- 브랜치: fix/TMI-176-identity-operability (작업 시작 시 확인한 기존 브랜치).
- Jira: TMI-176.
- 작업 목표: 승인된 R1~R4 수정 및 외부 계약을 유지한 회귀 검증.
- 변경 파일: RequestLoggingFilter, RequestLogContext, GlobalExceptionHandler, FailureDiagnostic(신규), JwtConfiguration, JwksPublicKeySet, AuthException, SessionSecurityService, FirebaseSessionRevocationWorker, SessionRevocationConfiguration, ProviderUnlinkWorker, ProviderChangeConfiguration, 기존 deploy-staging.yml. 테스트는 RequestConsoleTests/JwtRotationVerificationTests/SessionSecurityDiagnosticTests/ProviderSchedulerDiagnosticTests/SentryReleaseDeploymentTests 신규 및 기존 로그·JWT fixture·SessionRevocationConfigurationTests·SentryCaptureIntegrationTests 보완. identity-learning-jwt.md, 수정 계획서, 신규 identity-operability-runbook.md와 작업 기록 갱신.
- 구현 내용: requestId는 MDC 단일 출처, active+retained 공개키를 JWKS/decoder에서 공유하며 exact kid/RS256 로컬 검증과 active-only 서명 유지. 원본 예외 없는 제한된 DB 진단을 기존 503 ERROR에 합치고 기존 scheduler/worker 처리 의미를 유지한 경고 보강. ECS render에 SENTRY_RELEASE 입력만 추가.
- 테스트와 결과: 초기 컴파일 및 테스트 작성 중 예외 타입/import/Mockito 재설정 오류를 보정했다. 최종 ./gradlew clean test --no-daemon 성공(146 suites/952 tests, failures=0/errors=0/skipped=0). 실제 콘솔 JSON, 보호 HTTP key rotation, 안전 진단/원인 순환, Sentry 503 비수집, YAML release 연결 검증 포함. git diff --check 통과. 작업 트리의 기존 미커밋 변경을 포함한 결과다.
- 유지한 계약: 사용자 API/응답/상태, JWT claim·TTL·시간 검증, workload 경계, RefreshSession/unique conflict/rollback·retry, 기능 flag 유지. 사용자 Docker/Swagger/SNS API 등 선행 변경은 보존했다. 이번 예상 밖 변경 없음; workflow EOF 개행 정리 외 변경은 계획 범위다.
- 결정사항: 외부 GitHub Action 실행을 통한 task definition fixture 렌더링은 하지 않고 YAML 정적 검사와 sanitizer 테스트까지 수행. 실제 runtime/env·secret·log 설정 보존 확인은 배포 gate로 명시. 코드 완료와 실제 배포 완료를 구분한다.
- Jira 작업/승인: 구현 전 공식 MCP로 이슈 설명/완료 조건 조회. 사용자 구현 요청에 따라 구현, Jira 쓰기·댓글·상태 전환은 수행하지 않음. 댓글 초안은 수정 계획서 6.5에만 작성.
- 위험 요소: 로그량 증가, 운영에 적용된 TTL/skew/키 캐시 확인 필요. 실제 AWS/Sentry·Firebase/Atlas E2E와 rollback probe는 미검증. 기존 workflow는 별도 테스트 서비스가 아닌 기존 서비스 대상이다.
- 다음 작업: 사용자 diff 검토 및 commit/push/PR. 승인된 별도 테스트 배포 후 CloudWatch/보호 API/Sentry release 확인. 실제 키 교체는 사전 공개키 게시/전파 및 구키 보존 절차로 별도 승인 후 수행.

## 2026-09-22 — 재발급 응답 복구 활성 조건과 구버전 호환 설명

- 브랜치: fix/TMI-176-identity-operability.
- Jira: TMI-176 (브랜치 맥락, 이번은 기존 Stage 9 계약 설명이며 Jira 변경 없음).
- 작업 목표: /reissue Guest/Member 공통 처리, Stage 9 의미, 요청 ID 수명, 구버전 영향과 활성 시점 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: TokenReissueService/ReissueRecoveryService, recovery 설정과 HTTP 헤더, 프론트 문서 및 운영 가이드를 읽기 확인. AUTH_REISSUE_RECOVERY_ENABLED 기본 false, ON에서는 누락/형식 오류 헤더 400, OFF에서는 기존 rotation이며 헤더가 있어도 응답 복구가 되지 않음을 확인. 상대 링크 문서 실재 확인 및 직접 링크 안내.
- 테스트와 결과: 코드 변경 없는 계약 설명이므로 Gradle 미실행. 코드/문서 대조와 git diff --check 수행. 실제 배포 설정은 조회하지 않음.
- 유지한 계약: 제품 코드/API/기능 flag/운영 인프라 불변, 기존 작업 보존.
- 결정사항: 새 앱은 동일 논리 재발급의 재시도에 동일 원 Refresh Token+ID 사용. 테스트 환경 준비·검증 후 별도 활성화하고 운영은 구버전 전환 전략과 전체 서버 호환 배포 확인 뒤 승인 필요. 과거 구버전 미사용 가정만으로 운영 ON을 권하지 않는다.
- 위험 요소: ON/OFF 혼합 writer와 헤더 미지원 구버전의 인증 실패. 기능 구현과 운영 활성화는 다르며 현재 운영값은 미확인.
- 다음 작업: 프론트 pending/single-flight/원자 저장/절대 만료 처리를 확인하고 대상 환경 및 구버전 업데이트 전환 조건을 확정한 뒤 기능 활성화 검증.

## 2026-09-22 — 재발급 계약 설명 작업 식별 기록 보완

<!-- codex-turn:01a0c79a-b4c8-7e33-a1de-9aba35f232ec -->

- 브랜치: fix/TMI-176-identity-operability. Jira: TMI-176 (브랜치 맥락).
- 작업 목표: 현재 설명 작업 식별자와 완료 상태 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Guest/Member 공통 재발급, Stage 9 응답 복구 기능 및 요청 ID 수명, 구버전 헤더 누락 시 400과 단계적 활성화 조건 설명 완료를 기록. 과거 WORKLOG는 변경하지 않았다.
- 테스트와 결과: 기록만 보완하여 Gradle 미실행. git diff --check 수행.
- 유지한 계약: 제품 코드/API/기능 flag/외부 설정 변경 없음. 민감정보 미기록.
- 결정사항: 운영 활성화는 구버전 전환과 테스트 완료 후 별도 승인 필요. 현재 운영 flag 값은 미확인.
- 위험 요소: 헤더 미지원 구버전 실패 및 ON/OFF writer 혼합 위험 유지.
- 다음 작업: 프론트 구현과 구버전 업데이트 전환 조건 확인 후 별도 테스트 환경 검증.

## 2026-09-22 — Refresh Token 만료 시 재발급 불가 확인

<!-- codex-turn:01a0c7a1-0787-7593-bad3-e3e2e3673c62 -->

- 브랜치: fix/TMI-176-identity-operability. Jira: TMI-176 (브랜치 맥락).
- 작업 목표: /reissue의 만료 조건 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: TokenReissueService와 ReissueRecoveryService의 원 세션 만료 검사를 확인. 유효한 Refresh Token이 필요하며 응답 복구도 만료를 우회하지 않음을 안내.
- 테스트와 결과: 코드 읽기 확인, 설명/기록만 변경하여 Gradle 미실행. git diff --check 수행.
- 유지한 계약: API/인증/만료 정책/운영 설정 불변.
- 결정사항: Access Token 만료와 Refresh Token 만료를 구분하고 만료된 회원 세션은 정상 로그인으로 새 세션을 얻도록 안내.
- 위험 요소: 탈퇴/폐기 등 다른 거절 조건도 유지. 실제 운영 TTL은 이번에 조회하지 않음.
- 다음 작업: 프론트가 만료 오류를 무한 재시도하지 않고 재인증 흐름으로 처리하는지 확인.

## 2026-09-22 — /reissue 요청 ID 필수 여부 확인

<!-- codex-turn:01a0c7a4-fcb0-74f1-8d76-6579c3794f39 -->

- 브랜치: fix/TMI-176-identity-operability. Jira: TMI-176 (브랜치 맥락).
- 작업 목표: Idempotency-Key 필수 조건과 운영 확인 한계 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 설정 기본 false와 recovery 서비스 분기 및 요청 ID 오류 코드를 읽기 확인. ON에서 필수, OFF에서 선택임을 안내.
- 테스트와 결과: 설명/기록만 변경하여 Gradle 미실행. git diff --check 수행.
- 유지한 계약: 제품 코드/API/기능 설정 변경 없음.
- 결정사항: 실제 배포 설정을 조회하지 않았으므로 운영 ON/OFF를 단정하지 않는다.
- 위험 요소: 기본값과 실제 배포 환경변수는 다를 수 있음.
- 다음 작업: 필요 시 대상 배포의 AUTH_REISSUE_RECOVERY_ENABLED 유효값 확인.

## 2026-09-22 — 구버전 주소 분리 및 업데이트 유예 방안 검토

<!-- codex-turn:01a0c7be-4404-7e62-9e0f-4b57fae620e7 -->

- 브랜치: fix/TMI-176-identity-operability. Jira: TMI-176 (브랜치 맥락).
- 작업 목표: 구/신 서버 주소 분리·웹뷰 안내·1주 유예 제안의 안전성 검토.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Stage 9 운영 가이드의 혼합 writer 금지 및 legacy 재사용 시 계정 활성 세션 폐기 경로 확인. 주소 분리가 DB/세션 격리나 버전 인증 수단은 아님을 설명하고 유예 중 복구 OFF 유지와 호환 전환 설계를 권고했다.
- 테스트와 결과: 코드/문서 읽기 검토, 코드 미변경으로 Gradle 미실행. git diff --check 수행. 실제 구버전 앱/웹뷰 렌더링/운영 설정은 이번에 검증하지 않음.
- 유지한 계약: 코드/API/운영 서버/기능 flag/세션 변경 없음. 기존 사용자 변경 보존.
- 결정사항: 1주는 초기 관찰 기간으로 제안하며 구버전 사용량·실패율·업데이트 전환을 보고 종료 판단. 신앱 헤더 선구현과 실제 서버 복구 활성화를 구분한다.
- 위험 요소: 구 코드와 신규 recovery의 같은 저장소 혼재, 인증 실패 전 안내 미노출, 장기 미접속 Guest의 이전 기록 연결 불가, 구버전 종료 시 범용 인증 오류 루프. 업데이트 안내/차단 정책은 별도 구현 확인 필요.
- 다음 작업: 구/신 앱 실제 endpoint·공유 DB 및 Guest 승격 계약을 확인하고 유예 기간·구주소 종료 동작·전환 전용 인증 경로를 확정한 후 별도 구현 승인.

## 2026-09-22 — 주소 분리와 공유 세션 충돌 위험 설명

<!-- codex-turn:01a0c7c5-9c23-7201-9ffa-a85cc14b8d36 -->

- 브랜치: fix/TMI-176-identity-operability. Jira: TMI-176 (브랜치 맥락).
- 작업 목표: 서버 두 개와 인증 데이터 분리의 차이를 쉬운 예시로 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: legacy 재사용 탐지의 사용자별 전체 활성 세션 조회 범위를 재확인. 같은 계정의 구버전 재전송이 신버전 세션까지 폐기할 수 있는 조건부 사례와 동일 source를 구/신 규칙으로 처리할 때의 위험을 설명. 요청이 자동으로 다른 서버에 간다는 의미는 아님을 구분.
- 테스트와 결과: 코드 읽기 및 설명만 수행, Gradle 미실행. git diff --check 수행.
- 유지한 계약: 제품 코드/API/DB/운영 설정 변경 없음.
- 결정사항: 두 주소 운영 자체는 가능하며 공유 저장소 및 세션 처리 호환성을 확인한 뒤 복구 기능 활성화 여부 결정.
- 위험 요소: 실제 구/신 서버가 동일 DB/사용자 식별자를 사용하는지는 미확인. DB 단순 복사로는 계정/세션 변경 동기화가 보장되지 않는다.
- 다음 작업: 실제 저장소·사용자 매핑·새 앱 Guest 전환 경로 확인 후 전환 설계 구체화.

## 2026-09-22 — 단일 기기 업데이트 전환과 조건부 세션 충돌 구분

- 브랜치: fix/TMI-176-identity-operability. Jira: TMI-176 (브랜치 맥락).
- 작업 목표: 사용자 업데이트 전제에서 구/신 서버 동시 이용 위험을 정확히 한정.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 단일 기기 단방향 업데이트와 동일 세션의 구/신 처리 혼재를 구분. 다중 기기도 동일 계정 매핑이 있어야 계정 단위 영향이 생기며 현재 Guest-only 앱에 당연히 적용되는 것은 아니라고 안내. 이전 설명의 복구 활성화 일괄 연기 권고를 조건부로 보정.
- 테스트와 결과: 추가 코드 변경/실행 검증 없이 전제 및 기존 근거 정리, Gradle 미실행. git diff --check 수행.
- 유지한 계약: 코드/API/기능 flag/운영 환경 불변. 과거 WORKLOG 보존.
- 결정사항: 구버전의 신주소 미접근, 업데이트 후 구주소 fallback 없음, 동일 세션 기존 요청 정리, 공유 DB 쓰기/회원 전환 호환성 검증이 충족되면 분리 운영 및 신서버 복구 활성화를 검토할 수 있다. 이번에 활성화를 승인하거나 수행하지 않는다.
- 위험 요소: 요청 중 앱 종료 시 서버 처리는 남을 수 있음. 실제 앱 라우팅·세션 계승·운영 코드 호환성은 아직 미검증이며 기존 runbook 혼합 writer 금지 조건을 임의로 해제하지 않는다.
- 다음 작업: 실제 앱 업데이트 시 pending 요청/토큰 보존과 주소 전환을 확인하고 구/신 경로의 세션 격리 및 호환성 테스트 후 활성화 판단.

## 2026-09-22 — 업데이트 전환 설명 작업 식별 기록 보완

<!-- codex-turn:01a0c7c7-512c-71f3-8f05-64255056a05a -->

- 브랜치: fix/TMI-176-identity-operability. Jira: TMI-176 (브랜치 맥락).
- 작업 목표: 현재 설명 작업 식별자와 완료 상태 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 단일 기기 단방향 업데이트에서는 구/신 서버 동시 이용이 필연적이지 않음을 설명한 상태 기록. 과거 기록은 보존했다.
- 테스트와 결과: 기록만 변경하여 Gradle 미실행. git diff --check 수행.
- 유지한 계약: 제품 코드/API/기능 flag/외부 설정 변경 없음. 민감정보 미기록.
- 결정사항: 새 서버 복구 활성화는 실제 전환 및 공유 DB 호환성 검증 후 판단한다.
- 위험 요소: 업데이트 직전 진행 요청 및 기존 Guest 세션 계승 경로는 미검증.
- 다음 작업: 앱 라우팅/세션 전환 확인 및 호환성 테스트 후 운영 전환 설계 확정.

## 2026-09-22 — Guest 앱 업데이트 네 조건 검토 및 피드백 안내 계약

<!-- codex-turn:01a0c7c9-a219-76c3-9b27-3197f34475fb -->

- 브랜치: fix/TMI-176-identity-operability. Jira: TMI-176 (브랜치 맥락, 이번 추가 검토에 Jira 변경 없음).
- 작업 목표: 기존 Guest 보존·신서버 수용·진행 재발급·주소 fallback 검토, 업데이트 boolean 위치 확정.
- 변경 파일: docs/contracts/guest-app-update-transition-review.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 수행 내용: 프론트 원격 main f6ac7a02/main 코드 및 Identity 원격 main 7f2188df의 세션 모델과 현재 작업 트리 비교. 기존 앱 세션 읽기·raw 피드백 bridge·설정 fallback·메모리 single-flight 확인. 구 ROTATED 토큰 복구 불가 경계와 신규 앱 미확인 분리 기록.
- 테스트와 결과: ./gradlew test --tests '*ReissueRecoveryServiceTests' --tests '*SessionRevocationTests' --no-daemon 성공. 기존 단위 테스트만 재실행했으며 APK/실제 DB/혼합 서버 E2E나 전체 clean test는 이번 문서 작업에서 미실행. git diff --check 수행.
- 유지한 계약: 제품 코드/API/운영 설정 불변. 기존 미커밋 변경 보존, 예상 밖 제품 변경 없음. 시험/피드백 코드를 Identity에 추가하지 않음.
- 결정사항: 사용자 답변으로 피드백 result에 updateRequired 추가, Learning Core도 구·신 주소 분리 확정. 배포별 구 true/신 false 및 기본 false를 LC 인계안으로 기록. LC 구현과 웹뷰 렌더링은 미수행.
- 위험 요소: 신앱 코드와 실제 저장소/키/flag 미확인. 구 회전 응답 유실 후 재사용 감지 위험. 프론트 main에는 Stage 9 헤더/절대 만료 처리 미구현. 인증 실패 사용자는 피드백 안내까지 도달하지 못함.
- 다음 작업: 신규 앱 revision 확보, 실기기 업데이트·응답 유실 검증, Learning Core 담당 범위에서 boolean 구현 및 웹뷰 연동. Jira 댓글 초안: 전환 검토 문서 작성/관련 기존 테스트 성공/신앱·환경·E2E 미확인, 제품 변경 없음. 자동 등록하지 않음.

## 2026-09-22 — 주소 분리 및 업데이트 안내 가능 여부 확인

<!-- codex-turn:01a0c7d6-ee64-73a3-ac1b-abdc66b23e35 -->

- 브랜치: fix/TMI-176-identity-operability. Jira: TMI-176 (브랜치 맥락).
- 작업 목표: 제안한 전환 방식의 구현 가능 여부와 배포 전 조건 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 구/신 Identity·Learning Core 주소 분리 및 피드백 updateRequired 구 true/신 false 방식은 가능함을 확인. 세션 호환성 검증 완료와 구분했다.
- 테스트와 결과: 설명/기록만 변경하여 테스트 미실행, 직전 코드 조사 결과 활용.
- 유지한 계약: 제품 코드/API/DB/운영 설정 불변. 기존 미커밋 작업 보존, 예상 밖 제품 변경 없음.
- 결정사항: 안내 유예 방식은 진행 가능하나 boolean 자체가 강제 업데이트 수단은 아님.
- 위험 요소: 기존 Guest 전환·구 회전 응답 유실·신앱 Stage 9 대응 미검증 유지.
- 다음 작업: LC 응답 필드와 웹뷰 안내 구현, 신앱/전환 검증 후 배포. Jira 변경 없음.

## 2026-09-22 — 테스트 배포와 앱 업데이트 전환 작업 순서 확인

<!-- codex-turn:01a0c7e0-658a-7753-b50a-0512c96c992b -->

- 브랜치: fix/TMI-176-identity-operability. Jira: TMI-176 (브랜치 맥락).
- 작업 목표: 테스트 배포를 우선하고 운영 전환 작업을 앱 업데이트 전에 수행할 수 있는지 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 운영과 격리된 테스트 배포 및 기능 검증 선행 가능, 기존 Guest 세션 전환 및 업데이트 안내는 출시 전 필수 작업으로 구분.
- 테스트와 결과: 설명/기록만 변경하여 테스트 미실행. 기존 조사 결과 기반 순서 안내.
- 유지한 계약: 제품 코드/API/운영 DB/기능 flag 변경 없음. 기존 미커밋 변경 보존, 예상 밖 제품 변경 없음.
- 결정사항: 테스트 배포 → 인증/챌린지 검증 → 업데이트 안내·전환 구현/검증 → 앱 출시 순서. 테스트 배포는 운영 전환 승인이나 실제 배포 수행을 뜻하지 않음.
- 위험 요소: 테스트가 운영 저장소·키·이벤트 목적지를 사용하면 선행 배포의 격리 전제가 깨짐. Stage 9 ON 테스트는 지원 클라이언트 필요. 구 회전 응답 유실 검증은 미완료.
- 다음 작업: 테스트 인프라 준비 상태 확인 후 별도 테스트 배포 진행. 앱 출시 전에 전환 검증 완료. Jira 변경 없음.

## 2026-09-22 — 테스트 인증서 DNS 검증 단계 확인

<!-- codex-turn:01a0c824-820a-7552-89e7-d07cb6586ae1 -->

- 브랜치: fix/TMI-176-identity-operability. Jira: TMI-176 (브랜치 맥락).
- 작업 목표: 현재 ACM 인증서 등록 필요 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 열린 AWS ACM 화면에서 identity-test.to-teacher.com 인증서가 DNS 검증 대기 중/사용 중 아님을 확인. 가비아 검증 CNAME 등록 후 발급 및 ALB 연결 순서 안내.
- 테스트와 결과: 브라우저 읽기 전용 확인. 문서 기록만 변경하여 Gradle 미실행. DNS 실제 전파 상태는 이번에 별도 조회하지 않음.
- 유지한 계약: 제품 코드/인증서/DNS/ALB 변경 없음. 기존 미커밋 변경 보존, 예상 밖 변경 없음. 탭 유지.
- 결정사항: 새 인증서 재요청보다 현재 인증서 DNS 검증 완료가 우선.
- 위험 요소: 가비아 호스트 입력에 도메인 중복 추가 주의. ACM 발급만으로 API 라우팅이 완성되지는 않음.
- 다음 작업: 검증 CNAME 등록 및 발급 확인 후 ALB HTTPS listener 인증서와 테스트 라우팅 연결. Jira 변경 없음.

## 2026-09-23 — 인증서 검증 DNS 등록 확인

<!-- codex-turn:01a0cd4d-6c1e-7e11-a5d5-73866f13e334 -->

- 브랜치: develop.
- 작업 목표: 사용자 CNAME 등록 후 반영 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: dig CNAME 조회 결과가 기존 ACM 화면의 검증 대상과 일치. ACM 확인 시 로그인 세션 만료로 ISB 재로그인 페이지를 열고 유지.
- 테스트와 결과: DNS 조회 성공. 문서 기록만 변경하여 Gradle 미실행. ACM 발급 상태는 확인하지 못함.
- 유지한 계약: 제품 코드/외부 설정 변경 없음. 기존 변경 보존, 예상 밖 제품 변경 없음.
- 결정사항: DNS 등록 확인과 인증서 발급 완료는 구분하여 보고.
- 위험 요소: AWS 재로그인 전 발급 여부 및 ALB 연결 상태 미확인.
- 다음 작업: 사용자 로그인 후 ACM 발급 확인, ALB 인증서 추가 단계 진행. Jira 작업 없음.

## 2026-09-23 — 재로그인 후 테스트 인증서 발급 확인

- 브랜치: develop.
- 작업 목표: ACM 발급 완료 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 현재 AWS 탭에서 인증서 상세 조회. identity-test.to-teacher.com 발급됨/도메인 검증 성공/사용 중 아니요 확인. 발급 시각 2026-09-22 17:14:54 KST.
- 테스트와 결과: ACM 화면 직접 확인. 제품 변경이 없어 Gradle 미실행.
- 유지한 계약: 제품 코드/ALB/DNS/인증서 설정 불변. 기존 변경 보존, 예상 밖 제품 변경 없음.
- 결정사항: 새 발급 불필요, 다음은 ALB HTTPS 443 인증서 목록 추가. 기존 인증서 교체 아님.
- 위험 요소: 인증서 발급만으로 테스트 서버 라우팅과 배포가 완료되지는 않음.
- 다음 작업: ALB listener 현황 확인 후 승인된 범위에서 테스트 인증서 연결. Jira 변경 없음, AWS 탭 유지.

## 2026-09-23 — 인증서 발급 확인 작업 식별 기록 보완

<!-- codex-turn:01a0cd51-d10b-7e50-9788-13a034c1d8bc -->

- 브랜치: develop.
- 작업 목표: 현재 작업 식별자 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: ACM 발급됨/도메인 검증 성공/미사용 확인 결과를 현재 작업과 연결. 과거 기록 유지.
- 테스트와 결과: 기록만 보완하여 테스트 미실행. 직전 ACM 화면 확인 결과 유지.
- 유지한 계약: 제품 코드 및 외부 설정 변경 없음. 민감정보 미기록.
- 결정사항: 인증서 발급 완료, ALB 연결은 미수행.
- 위험 요소: 테스트 HTTPS 라우팅/서버 배포는 별도 필요.
- 다음 작업: ALB listener 현황 확인 및 테스트 인증서 추가.

## 2026-09-23 — 테스트 TLS 인증서 ALB 연결 완료

- 브랜치: develop.
- 작업 목표: 사용자 승인한 테스트 인증서를 기존 ALB HTTPS:443에 추가.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 외부 변경: tosunsaeng-staging-alb HTTPS:443 SNI 인증서 목록에 identity-test.to-teacher.com 추가.
- 수행 내용: 기존 기본 인증서와 규칙 확인 후 테스트 인증서 1개만 선택하여 저장. AWS 추가 성공 메시지 및 기존/테스트 인증서 2개 목록 확인.
- 테스트와 결과: 콘솔 저장 결과 확인. 코드 변경 없어 Gradle 미실행. 실제 TLS 접속/E2E는 미검증.
- 유지한 계약: 기존 identity-staging 기본 인증서 유지. DNS/보안 정책/라우팅 규칙/대상 그룹/서비스 불변. 예상 밖 변경 없음.
- 결정사항: 테스트 인증서는 SNI 추가로 연결하며 기본 인증서를 교체하지 않음.
- 위험 요소: 현재 규칙은 identity-staging/api-staging/기본404로, 인증서 추가만으로 테스트 API 사용 가능하지 않음.
- 다음 작업: 테스트 대상 그룹 및 ECS 서비스 준비 상태 확인, 테스트 호스트 규칙과 DNS 연결 후 HTTPS 검증. Jira 변경 없음, 브라우저 탭 유지.

## 2026-09-23 — ALB 인증서 연결 작업 식별 기록 보완

<!-- codex-turn:01a0cd53-2f36-70b1-9b6a-1743151c5642 -->

- 브랜치: develop.
- 작업 목표: 이번 ALB 인증서 연결 결과와 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 승인으로 수행한 테스트 인증서 SNI 추가 성공을 현재 작업에 연결. 과거 기록 보존.
- 테스트와 결과: AWS 성공 메시지 및 인증서 목록 확인 결과 유지. 기록 보완만 수행하여 추가 테스트 없음.
- 유지한 계약: 기존 기본 인증서/라우팅/서비스 유지. 이번 보완 중 추가 외부 변경 없음.
- 결정사항: HTTPS:443 테스트 인증서 연결 완료.
- 위험 요소: 실제 테스트 API 가용성과 TLS 접속은 아직 미검증.
- 다음 작업: 테스트 대상 그룹/ECS/호스트 라우팅/DNS 준비 확인 및 HTTPS 검증.

## 2026-09-23 — 테스트 대상 그룹 생성 및 배포 이미지 선행 조건 확인

- 브랜치: develop, HEAD bd337be4baf7055bfd7f541d424b06585b09c2a3.
- 작업 목표: 운영과 분리된 테스트 서버 준비 진행.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 외부 생성: tosunsaeng-identity-test-tg.
- 수행 내용: 클러스터의 기존 Identity/LC/AI 서비스와 기존 대상 그룹 2개 확인. 신규 IP 대상 그룹 HTTP:8081, HTTP1, IPv4, staging VPC, health /actuator/health 생성. 기본 상태 검사 30초/timeout5초/정상5회/비정상2회/200 유지. 대상은 ECS 자동 등록 예정으로 0개 유지, 기존 운영 IP 미등록. AWS 생성 성공 확인.
- 테스트와 결과: 콘솔 생성 성공 및 대상0/ALB 미연결 확인. ECR 목록 28개 확인, 최신 이미지 main 7f2188df(9월18일), develop bd337be4 이미지 부재. 로컬 aws sts 조회는 NoCredentials. 제품 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 운영 서비스/기본 인증서/ALB 규칙/보안그룹/IAM/DNS 변경 없음. Git commit/push 및 workflow 추가/실행 없음. 예상 밖 변경 없음.
- 결정사항: 기존 deploy-staging workflow는 ECS_SERVICE=tosunsaeng-identity-service이므로 테스트 이미지 생성 수단으로 실행하지 않는다. develop 고정 commit 이미지를 별도로 빌드/업로드한 뒤 테스트 Task Definition 구성 필요.
- 위험 요소: 테스트 이미지와 실행 설정/최소권한/격리 검증 미완료. 빈 대상 그룹은 서버 배포 완료가 아니며 아직 외부 라우팅 안 됨.
- 다음 작업: 로컬 AWS 사용자 인증 또는 승인된 별도 이미지 빌드 경로 확보, develop 이미지 업로드, 테스트 전용 실행 설정/Secret 참조/IAM 확인 후 ECS 서비스 구성. 권한 확대 및 외부 노출 시 구체적 확인 필요. Jira 변경 없음, 브라우저 탭 유지.

## 2026-09-23 — 테스트 서버 준비 작업 식별 기록 보완

<!-- codex-turn:01a0cd55-55fd-7db1-be8c-6efb91efdc5c -->

- 브랜치: develop.
- 작업 목표: 이번 테스트 인프라 준비 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 테스트 대상 그룹 생성 완료와 develop 이미지 부재/로컬 AWS 인증 부재 확인 결과를 현재 작업에 연결. 과거 기록 보존.
- 테스트와 결과: 기존 콘솔 검증 결과 유지. 기록 보완만 수행하여 추가 테스트 없음.
- 유지한 계약: 추가 외부 변경 및 제품 코드 변경 없음. 운영 리소스 유지, 민감정보 미기록.
- 결정사항: 기존 운영 배포 workflow 미실행 유지.
- 위험 요소: 테스트 서비스는 미배포이며 이미지/실행 설정/격리 검증 필요.
- 다음 작업: 사용자 AWS 인증 준비 후 운영 배포와 분리된 develop 이미지 빌드 및 업로드.

## 2026-09-23 — 기존 workflow main/develop 배포 분리 구현

- 브랜치: develop. 이번 요청의 Jira 이슈 미지정.
- 작업 목표: main 기존 서비스 유지, develop 테스트 서버 배포로 기존 workflow 수정.
- 변경 파일: .github/workflows/deploy-staging.yml, src/test/java/web/tosunsaeng/identity/deployment/DeploymentTargetTests.java, docs/contracts/identity-branch-deployment.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 구현 내용: push main/develop, 수동 다른 ref 거절. 브랜치별 서비스/task family/health/이미지 별칭/배포 role 선택. main AWS_ROLE_ARN 유지, develop AWS_TEST_ROLE_ARN 필수. ACTIVE 서비스·family/container 검사 후 이미지 업로드/동일 서비스 배포. SENTRY_RELEASE 유지 및 브랜치별 실행 잠금. 새 workflow 추가 없음.
- 테스트와 결과: ./gradlew clean test --no-daemon 성공, 147 suites/957 tests/실패0/오류0. 최초 sandbox Gradle 캐시 접근 차단 후 승인된 권한으로 재실행. 분기 shell 실제 실행 테스트 및 YAML 정적 계약 검사, git diff --check 통과. 실제 Actions/AWS 실행은 미검증.
- 유지한 계약: main 기존 배포 목적지/기존 컨테이너명/API/JWT/기능 flag 유지. DB/Secret/키 값을 workflow에 넣지 않음. 기존 사용자 기록 변경 보존. 예상 밖 변경 없음.
- 결정사항: devlop는 실제 develop 브랜치로 반영. 누락한 테스트 설정은 운영으로 fallback하지 않음. 새 test task/service는 사전 구성하며 workflow는 인프라 생성 도구가 아님.
- 위험 요소: 테스트 role/서비스 미준비 시 이미지 업로드 전 실패. 최초 이미지 빌드/업로드 경로와 task/service 초기 구성 필요. IAM trust/최소권한과 DB/키/이벤트 격리는 별도 검증. SHA 태그 불변 설정이나 digest pinning은 추가하지 않음.
- 다음 작업: 사용자 diff 검토/commit/push, 테스트 IAM/variables/task/service 및 ALB/DNS 설정 승인·구성 후 develop 배포 검증. 이번 commit/push/배포/IAM 변경 없음.
- Jira 댓글 초안(미등록): 기존 workflow에 main/develop 배포 대상 분리 및 fail-closed 검사 추가, 전체957 tests 성공, 실제 IAM/초기 서비스 준비와 Actions 검증은 남음.

## 2026-09-23 — 배포 workflow 분리 작업 식별 기록 보완

<!-- codex-turn:01a0cd58-b624-7302-bc90-eaa3b78ea5d8 -->

- 브랜치: develop.
- 작업 목표: 현재 workflow 분리 구현 결과의 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 기존 workflow의 main/develop 분리, 테스트 role 필수 및 대상 검증 결과를 현재 작업과 연결. 과거 기록 보존.
- 테스트와 결과: 직전 전체 147 suites/957 tests 성공 결과 유지. 기록 보완만 하여 추가 실행 없음.
- 유지한 계약: 추가 제품 코드/외부 설정 변경 없음. 민감정보 미기록.
- 결정사항: 로컬 구현 완료, commit/push/배포 미수행.
- 위험 요소: 실제 Actions 실행, IAM/최초 테스트 서비스 및 격리 검증은 남음.
- 다음 작업: 테스트 배포 사전 리소스 준비 및 사용자 commit/push 후 실행 검증.

## 2026-09-23 — 테스트 배포 IAM 정책 준비 및 승인 요청

<!-- codex-turn:01a0cd67-8814-7460-83fc-bc6cb5d1fef7 -->

- 브랜치: develop.
- 작업 목표: 테스트 배포 역할과 GitHub 변수 사전 준비.
- 변경 파일: docs/contracts/identity-test-deploy-trust.json, identity-test-deploy-policy.json, identity-branch-deployment.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: IAM 기존 Identity 배포 role 정책과 trust 읽기 전용 확인. GitHub API에서 immutable OIDC subject prefix 조회하여 develop exact trust 작성. 테스트 ECS service 갱신, 테스트 execution role PassRole, 기존 ECR build 권한과 task definition API 초안 작성. IAM 생성 직전 사용자 확인 요청.
- 테스트와 결과: JSON 두 파일 jq 구문 검사 및 git diff --check 성공. 제품 변경 없이 정책 초안만 작성하여 Gradle 재실행 없음. 실제 role assumption/Actions 미검증.
- 유지한 계약: 기존 workflow 미커밋 변경 보존. IAM/GitHub 변수/Secret/서비스/라우팅 변경 없음. 장기 credential 생성 및 민감정보 열람/기록 없음. 예상 밖 변경 없음.
- 결정사항: 새 role tosunsaeng-github-identity-test-deploy-role 제안. AWS_TEST_ROLE_ARN 등록 예정이나 아직 미수행. Secret 읽기는 CI role이 아니라 별도 테스트 execution role에 한정할 계획이며 세부 권한은 아직 미확정.
- 위험 요소: 기존 운영 trust는 repo wildcard이므로 develop에서도 운영 role 수임 가능 조건 존재. 공유 ECR 권한은 tag 쓰기까지 격리하지 않음. TaskDefinition API Resource=* 범위 명시. 기존 운영 role은 임의 변경하지 않음.
- 다음 작업: 사용자 권한 생성 승인 후 테스트 CI role 적용, GitHub 변수 등록, execution role/정확한 Secret ARN 확인과 별도 승인, 테스트 초기 task/service 및 DNS/라우팅 준비. Jira 변경 없음.

## 2026-09-23 — 승인된 테스트 CI 역할 생성 및 GitHub 변수 등록

<!-- codex-turn:01a0cd6f-0d88-7c52-af7f-ccc9b1ac7f06 -->

- 브랜치: develop. Jira 이슈 미지정.
- 작업 목표: 승인된 테스트 GitHub 배포 역할 및 AWS_TEST_ROLE_ARN 등록 완료.
- 변경 파일: docs/contracts/identity-branch-deployment.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md. 기존 미커밋 workflow/테스트/정책 초안은 보존.
- 수행 내용: AWS에서 tosunsaeng-github-identity-test-deploy-role 및 IdentityTestDeploy 인라인 정책 생성. 저장된 ARN, develop exact immutable OIDC subject, sts audience 및 정책 내용을 확인. GitHub 변수 목록에서 기존 AWS_ROLE_ARN만 존재함을 확인한 뒤 AWS_TEST_ROLE_ARN 생성 및 GET 일치 검증.
- 테스트와 결과: AWS 생성 성공/상세 UI 확인, GitHub POST 성공/GET 확인, git diff --check 수행. 제품 코드 변경 없어 Gradle 재실행 없음. 직전 957 tests 성공 결과는 이전 구현 검증이며 실제 OIDC/Actions 실행은 미검증.
- 유지한 계약: 기존 운영 role/AWS_ROLE_ARN/서비스/Secret 값/라우팅 미변경. 장기 credential 생성 없음. API/JWT/기능 flag 유지. commit/push/배포/Jira 변경 없음. 예상 밖 변경 없음.
- 결정사항: 사용자가 확인한 CI 역할 및 변수 범위만 적용. execution role/Secret 접근은 별도 승인 후 구성.
- 위험 요소: 공유 ECR 쓰기는 tag별 격리 아님. TaskDefinition API Resource=* 유지. 기존 운영 role wildcard 신뢰는 이번에 축소하지 않음. execution role/task/service 미준비로 현재 자동 배포 성공을 보장하지 않음.
- 다음 작업: 테스트 execution role의 정확한 Secret ARN 및 최소권한 확정·승인, 초기 task/service/이미지 및 DNS/ALB 라우팅 준비 후 실제 배포 검증.
- Jira 댓글 초안(미등록): 테스트 CI 역할 및 GitHub 변수 등록·조회 검증 완료. 제품 코드 변경 없음. 실제 배포 및 실행 역할/초기 서비스 준비는 남음.

## 2026-09-23 — 테스트 ECS 실행 역할 최소권한 준비

<!-- codex-turn:01a0cd77-0b9b-7433-a4ca-fb6294c8ca19 -->

- 브랜치: develop. Jira 이슈 미지정.
- 작업 목표: 테스트 서버의 Secret 주입·이미지 pull·로그 전송용 실행 역할 준비.
- 변경 파일: docs/contracts/identity-test-execution-trust.json, identity-test-execution-policy.json, identity-branch-deployment.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 수행 내용: AWS 콘솔에서 테스트 Secret 6개 이름/정확한 ARN 확인, 비밀값 미조회. 동일 계정 서울 ECS task 신뢰, 6개 GetSecretValue, 기존 Identity ECR pull, 제안 테스트 로그 그룹 스트림 쓰기만 포함한 정책 작성. 보안 권한 부여 직전 사용자 확인 요청.
- 테스트와 결과: JSON jq 구문 검사 및 git diff --check 수행. 제품 변경 없어 Gradle 재실행 없음. 실제 IAM 수임/Secret 주입/로그 전송은 미검증.
- 유지한 계약: 기존 운영 및 CI 역할, GitHub 변수, 서비스/Secret 값 미변경. 기존 미커밋 변경 보존. 예상 밖 변경 없음. 민감정보 미기록.
- 결정사항: 실행 역할 생성은 아직 미수행. taskRole이 아닌 executionRole로 사용. logs CreateLogGroup 및 광범위한 KMS 권한 부여 안 함.
- 위험 요소: 로그 그룹 생성 여부/보존 기간 미확인, task/service 준비 필요. ECS 신뢰는 특정 task family까지 제한하지 않으므로 PassRole 통제가 중요. 실제 권한 동작은 배포에서 검증 필요.
- 다음 작업: 정확한 권한 범위 사용자 승인 후 실행 역할 생성·검증, 로그 그룹 및 초기 task/service 준비. commit/push/배포/Jira 변경 없음.
- Jira 댓글 초안(미등록): 테스트 실행 역할 정책 준비 완료, Secret 메타데이터 확인 및 JSON 검사. 실제 생성과 배포 검증은 승인 후 진행.

## 2026-09-23 — 승인된 테스트 ECS 실행 역할 생성 완료

<!-- codex-turn:01a0cd77-0b9b-7433-a4ca-fb6294c8ca19 -->

- 브랜치: develop. Jira 미지정.
- 작업 목표: 사용자 승인한 테스트 실행 역할과 최소권한 적용.
- 변경 파일: docs/contracts/identity-branch-deployment.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 수행 내용: 기존 동일 역할 부재 확인 후 tosunsaeng-identity-test-execution-role 생성. IdentityTestExecution 인라인 정책 연결. 콘솔의 성공 메시지 및 역할 ARN, 저장 정책 전문과 동일 계정 서울 ECS 신뢰 조건 확인. 테스트 Secret 6개 읽기, Identity 이미지 pull, 테스트 로그 스트림 쓰기 범위만 적용.
- 테스트와 결과: AWS 생성 및 저장 정책 UI 검증 성공. git diff --check 수행. 제품 코드 변경 없어 Gradle 재실행 없음. 실제 ECS 수임/Secret 주입/로그 전송은 미검증.
- 유지한 계약: 운영 역할/서비스/Secret 값/GitHub 변수 미변경, 비밀값 미조회. 기존 미커밋 파일 보존. 예상 밖 변경 없음.
- 결정사항: 실행 역할에만 승인된 권한 적용, taskRole 연결 및 로그 그룹 생성/배포는 미수행.
- 위험 요소: 로그 그룹 및 초기 task/service 미준비. SourceArn은 family 단위 제한이 아니며 PassRole 통제가 필요. 실제 기동 검증은 별도 수행.
- 다음 작업: 테스트 로그 그룹과 보존 정책, task definition 및 서비스, 초기 이미지/DNS/라우팅 준비. commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): 테스트 실행 역할 생성과 저장 정책 검증 완료, 운영 변경 없음. 실제 ECS 기동·Secret 주입·로그 전송 검증은 후속.

## 2026-09-23 — 실행 역할 생성 작업 식별 기록 보완

<!-- codex-turn:01a0cd7a-0363-7810-b0c2-41eee45d0375 -->

- 브랜치: develop.
- 작업 목표: 이번 실행 역할 생성 결과에 정확한 작업 식별자 연결.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 승인된 테스트 실행 역할 생성 및 저장 정책 검증 완료 상태 기록 보완. 과거 기록 유지.
- 테스트와 결과: 문서만 수정하여 Gradle 미실행. git diff --check 확인.
- 유지한 계약: 추가 AWS 변경 없음, 비밀값 미기록, 기존 미커밋 변경 보존.
- 결정사항: 역할 생성 완료, 서버 배포 미수행.
- 위험 요소: 실제 ECS 수임/Secret 주입/로그 전송 미검증. 예상 밖 변경 없음.
- 다음 작업: 테스트 로그 그룹, task definition 및 ECS 서비스 준비.

## 2026-09-23 — 테스트 로그 그룹 생성 및 ECS 주입 계약 준비

- 브랜치: develop. Jira 미지정.
- 작업 목표: 테스트 로그 그룹 준비 및 ECS 실행 설정 확인.
- 변경 파일: docs/contracts/identity-branch-deployment.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 수행 내용: 동일 이름 로그 그룹 부재 확인 후 서울 /ecs/tosunsaeng-identity-test 생성(표준, 30일 보존). AWS 성공 메시지·목록 확인. application.yml과 entrypoint 기준 6개 Secret 주입 변수 매핑 문서화.
- 테스트와 결과: CloudWatch 저장 결과 확인 및 git diff --check. 제품 변경 없어 Gradle 재실행 없음. 실제 task 기동·로그 수집은 미검증.
- 유지한 계약: 운영 로그/역할/서비스 미변경, Secret 원문 미조회·미기록. 기존 미커밋 변경 보존. 예상 밖 변경 없음.
- 결정사항: 신규 테스트 로그는 30일 보존. ECS secrets.valueFrom의 전체 ARN/JSON key selector 선택은 사용자 저장 형식 확인 후 확정. task/service는 아직 등록하지 않음.
- 위험 요소: Secret 형식과 테스트 DB/동의 버전/기능 설정 미확정, develop 초기 이미지 및 네트워크 준비 필요.
- 다음 작업: 비밀값 없이 저장 형식 확인, 초기 task/service 구성 및 실제 배포·health·로그 검증. commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): 테스트 로그 그룹 생성 및 보존 설정 확인, Secret 변수 매핑 준비. ECS 등록과 실제 수집 검증은 후속.

## 2026-09-23 — 테스트 로그 그룹 작업 식별 기록 보완

<!-- codex-turn:01a0cd7c-a139-7671-a9c7-6c89a0e1fe68 -->

- 브랜치: develop.
- 작업 목표: 이번 로그 그룹 생성 결과의 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 테스트 로그 그룹 생성(30일 보존) 및 ECS Secret 주입 방식 확인 대기 상태를 현재 작업에 연결. 과거 기록 보존.
- 테스트와 결과: git diff --check 수행. 기록만 보완하여 Gradle 미실행.
- 유지한 계약: 추가 외부 변경 없음. 비밀값 미기록, 기존 미커밋 변경 보존.
- 결정사항: 로그 그룹 생성 완료, task/service 미생성.
- 위험 요소: Secret 저장 형식 및 실제 기동·로그 전송 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자에게 비밀값 없이 저장 형식 확인 후 ECS 설정 확정.

## 2026-09-23 — 테스트 Secret 저장 형식 확인 및 ECS 연결 배열 작성

- 브랜치: develop. Jira 미지정.
- 작업 목표: 혼합 저장 형식에 맞는 ECS secrets 매핑 확정.
- 변경 파일: docs/contracts/identity-test-container-secrets.json, docs/contracts/identity-branch-deployment.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 수행 내용: 사용자 요청으로 6개 Secret 조회 UI에서 저장 형식/키 이름을 확인. 결과는 메타데이터만 출력. MongoDB/fingerprint key selector 및 Firebase JSON/JWT PEM/keyring 전체 주입 배열 작성. 확인 후 비밀값 화면에서 목록으로 이동.
- 테스트와 결과: jq JSON 검사, 6개 고유 name 및 selector 구성 검사, git diff --check. 제품 변경 없어 Gradle 미실행. 실제 컨테이너 연결은 미검증.
- 유지한 계약: 비밀값 파일/로그/문서 저장 없음. Secret 값/권한/운영 서비스 변경 없음. 기존 미커밋 변경 보존. 예상 밖 변경 없음.
- 결정사항: 혼합 저장은 정상이며 Secret을 재작성하지 않음. Linux Fargate JSON selector 지원 버전 1.4.0 이상 필요.
- 위험 요소: 저장 형식 확인은 실제 값 유효성이나 연결 성공 검증이 아님. DB/약관/flag/초기 이미지 및 네트워크 검증 필요.
- 다음 작업: 준비한 secrets 배열을 사용해 초기 task definition/service 설정 확정 및 배포 검증. commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): Secret 저장 형식 확인 및 ECS secrets 매핑 준비 완료. 원문 미기록, 외부 설정 미변경. 실제 task 기동 검증은 후속.

## 2026-09-23 — Secret 형식 확인 작업 식별 기록 보완

<!-- codex-turn:01a0cd7f-9fb8-7201-bfdb-61742a426b9d -->

- 브랜치: develop.
- 작업 목표: 이번 저장 형식 확인 및 ECS 매핑 준비 결과의 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 형식 확인 완료와 ECS secrets 배열 준비 상태를 현재 작업에 연결. 과거 기록 보존.
- 테스트와 결과: git diff --check 수행. 문서만 보완하여 Gradle 미실행.
- 유지한 계약: 추가 외부 변경 없음, 비밀값 미기록, 기존 변경 보존.
- 결정사항: 저장 형식 확인 완료, task 등록/기동 미수행.
- 위험 요소: 실제 값 유효성 및 컨테이너 연결 미검증. 예상 밖 변경 없음.
- 다음 작업: 테스트 task definition/service 구성 및 배포 검증.

## 2026-09-23 — 초기 테스트 Task Definition 준비

- 브랜치: develop. Jira 미지정.
- 작업 목표: 운영과 분리된 초기 ECS task/service 설정 준비.
- 변경 파일: docs/contracts/identity-test-task-definition.draft.json, docs/contracts/identity-branch-deployment.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 수행 내용: 운영 서비스 configuration 및 revision23 실행 사양 읽기 전용 확인. 테스트 실행 역할/5개 필요한 Secret/로그/issuer/kid/DB 및 기능 flag 초안 구성. Stage9 OFF이므로 recovery keyring 미주입. 초기 desired=0 서비스와 첫 이미지 배포/기동 순서 문서화.
- 테스트와 결과: JSON jq 구문 및 task family/Secret 개수/role/flag 검사, git diff --check. 제품 코드 변경 없어 Gradle 재실행 없음. AWS 등록/실행 미검증.
- 유지한 계약: 기존 운영 role/서비스/Secret/라우팅 미변경. 비밀값 미기록. 미커밋 기존 변경 보존. API 계약 변경 및 예상 밖 변경 없음.
- 결정사항: 사양 0.5vCPU/1GB, Google/phone ON 및 Stage9/외부 발행 OFF를 초기 설정으로 제안. AWS 등록 전에 DB 권한과 약관 버전 등 사용자 확인 필요.
- 위험 요소: 현재 develop SHA 이미지 존재 미확인, task/service 미등록. 초기 desired=0이면 workflow health 단계 성공 불가. 네트워크/ALB/DB 권한 검증 필요.
- 다음 작업: 구체적 초기 설정 승인 및 DB명 확인 후 task 등록, 테스트 SG/ALB/service 준비 및 기동 검증. commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): 초기 테스트 task 설정 준비 및 JSON 검사, 운영 변경 없음. 실제 등록/배포와 연결 검증은 후속.

## 2026-09-23 — 초기 테스트 Task Definition 작업 식별 기록 보완

<!-- codex-turn:01a0cd84-68a9-7f42-aa4e-5d6c03b09e5a -->

- 브랜치: develop.
- 작업 목표: 이번 테스트 task 초안 준비 결과에 현재 작업 식별자 연결.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 초기 task 초안 및 JSON 검사 완료, DB명/권한과 등록 승인 확인 대기 상태 기록 보완. 과거 기록 유지.
- 테스트와 결과: git diff --check 수행. 문서만 보완하여 Gradle 미실행.
- 유지한 계약: 추가 외부 변경 없음, 비밀값 미기록, 기존 미커밋 변경 보존.
- 결정사항: AWS task/service 미등록, 서버 미기동.
- 위험 요소: 실제 이미지/네트워크/DB 권한 및 기동 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 설정 승인과 DB 확인 후 task/service 등록 준비.

## 2026-09-23 — 승인된 테스트 Task Definition 등록

- 브랜치: develop. Jira 미지정.
- 작업 목표: 승인된 테스트 실행 설정을 AWS ECS에 등록.
- 변경 파일: docs/contracts/identity-branch-deployment.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 수행 내용: 활성 family 목록에 테스트 family 부재 확인 후 JSON 생성 UI로 승인된 입력 등록. tosunsaeng-identity-test:1 생성 성공 및 저장된 execution role/사양/DB/issuer/이미지 참조/기능 flag 확인. DB 이름 사용 승인은 받았으며 실제 Atlas 권한 확인과는 구분.
- 테스트와 결과: AWS JSON 편집기 오류0/경고0, 생성 성공 메시지 및 저장 화면 검증. git diff --check 수행. 제품 코드 변경 없어 Gradle 재실행 없음. 실제 task 실행/연결/이미지 pull 미검증.
- 유지한 계약: 운영 family/service/Secret 값/권한/라우팅 미변경. 비밀값 주입 대신 ARN 참조만 등록. 기존 미커밋 변경 보존, 예상 밖 변경 없음.
- 결정사항: Task Definition만 등록. 서비스 생성과 desired=0 설정은 아직 미수행이며 task 실행 안 함.
- 위험 요소: 이미지 존재, Atlas readWrite 권한, 실제 Secret 주입 및 인증 기동 미검증. SG/ALB 연결과 초기 서비스 준비 필요.
- 다음 작업: 테스트 네트워크/ALB/SG 확인 및 필요한 접근 변경 승인 후 desired=0 서비스 생성, develop 이미지 배포와 기동 검증. commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): 테스트 Task Definition revision1 등록 및 저장 설정 확인 완료, 운영 변경 없음. 서비스 생성 및 실제 기동/연결 검증은 남음.

## 2026-09-23 — 테스트 Task Definition 등록 작업 식별 기록 보완

<!-- codex-turn:01a0cd87-fb5b-70b3-8aa0-27a1763424cd -->

- 브랜치: develop.
- 작업 목표: 이번 Task Definition 등록 결과의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: tosunsaeng-identity-test:1 등록 및 저장 설정 확인 완료 상태를 현재 작업에 연결. 과거 기록 보존.
- 테스트와 결과: git diff --check 수행. 문서만 보완하여 Gradle 미실행.
- 유지한 계약: 추가 외부 변경 없음, 비밀값 미기록, 기존 미커밋 변경 보존.
- 결정사항: Task Definition 등록 완료, 테스트 서비스 생성/기동 미수행.
- 위험 요소: 실제 이미지 pull/DB 권한/Secret 주입/네트워크 연결 미검증. 예상 밖 변경 없음.
- 다음 작업: 테스트 네트워크와 ALB 연결 확인 후 desired=0 ECS 서비스 준비.

## 2026-09-23 — 테스트 서비스 네트워크 확인 및 변경 승인 준비

- 브랜치: develop. Jira 미지정.
- 작업 목표: 테스트 ECS 서비스의 SG/ALB 연결 확인 및 안전한 생성 범위 확정.
- 변경 파일: docs/contracts/identity-branch-deployment.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 수행 내용: ALB SG와 인바운드80/443·모든 송신 규칙 조회, HTTPS 규칙10 identity-staging/20 api-staging/default404 확인. 신규 test SG, exact test host 우선순위30, 사설 subnet/desired0 서비스 설정안 작성 및 보안 변경 직전 확인 요청.
- 테스트와 결과: AWS 콘솔 읽기 전용 검증, git diff --check 수행. 제품 변경 없어 Gradle 미실행. 실제 네트워크/기동 테스트 미수행.
- 유지한 계약: 기존 SG/ALB/서비스/Secret 변경 없음. 비밀값 미기록, 기존 미커밋 변경 보존. 예상 밖 변경 없음.
- 결정사항: 운영 SG 수정 대신 test SG 분리 제안. 새 ALB 경로 공개와 SG 접근 허용은 사용자 최종 확인 후 적용. 서비스 생성은 아직 미수행.
- 위험 요소: 포트 제한 송신은 목적지별 격리가 아님. NAT/라우트/endpoint/Atlas allowlist/DB 권한 및 이미지 준비 확인 필요. 빈 test TG는503 응답 가능.
- 다음 작업: 승인 후 SG/ALB 규칙 생성 및 desired0 테스트 서비스 구성, 이후 배포/기동 검증. commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): 테스트 네트워크 조사 및 접근 변경안 준비. AWS 변경 없음. SG/ALB/service 생성과 실제 통신 검증은 승인 후 진행.

## 2026-09-23 — 테스트 네트워크 조사 작업 식별 기록 보완

<!-- codex-turn:01a0cd8b-c543-7640-93f7-9b966322cf9b -->

- 브랜치: develop.
- 작업 목표: 이번 네트워크 조사 및 승인 요청 결과에 현재 작업 식별자 연결.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: ALB/SG 읽기 전용 확인과 신규 테스트 접근 범위 승인 대기 상태 기록 보완. 과거 기록 보존.
- 테스트와 결과: git diff --check 수행. 문서만 수정하여 Gradle 미실행.
- 유지한 계약: 추가 외부 변경 없음, 비밀값 미기록, 기존 미커밋 변경 보존.
- 결정사항: 테스트 SG/ALB 규칙/service는 아직 생성하지 않음.
- 위험 요소: 실제 네트워크·이미지·Atlas 연결 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 승인 후 테스트 SG/ALB 규칙 및 desired0 서비스 구성.

## 2026-09-23 — 프론트의 테스트 서버 접근 조건 안내

- 브랜치: develop.
- 작업 목표: 사설 ECS와 공인 ALB 구성에서 프론트 접근 가능 여부 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 현재 준비 상태 확인 및 Java 소스 CORS 설정 검색. 공인 HTTPS ALB 경유 접근과 DNS/이미지/desired1/health 선행 조건, WebView JS CORS 추가 검증 필요 안내.
- 테스트와 결과: 읽기 전용 코드 검색 및 git diff --check. 설명/기록만 변경하여 Gradle 미실행. 실제 외부 연결 테스트 미수행.
- 유지한 계약: AWS/코드/API/보안 설정 변경 없음. 비밀값 미기록. 예상 밖 변경 없음.
- 결정사항: desired0 서비스는 실제 API 사용 가능 상태가 아님. 테스트 네트워크 변경 승인은 여전히 대기.
- 위험 요소: DNS/기동 및 브라우저 CORS 미검증. 챌린지 E2E는 LC/AI 준비도 별도 필요.
- 다음 작업: 사용자 승인 후 테스트 네트워크와 서비스 구성, 실제 기동 및 프론트 호출 검증.

## 2026-09-23 — 프론트 접근 조건 안내 작업 식별 기록 보완

<!-- codex-turn:01a0cd8e-0183-7a71-b125-f58325824bdb -->

- 브랜치: develop.
- 작업 목표: 이번 프론트 접근 조건 안내에 현재 작업 식별자 연결.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: ALB 경유 접근 가능 구조와 DNS/서버 기동/health/CORS 확인 필요 상태 기록 보완. 과거 기록 보존.
- 테스트와 결과: git diff --check 수행. 문서만 보완하여 Gradle 미실행.
- 유지한 계약: 추가 외부 변경 없음, 비밀값 미기록, 기존 변경 보존.
- 결정사항: desired0은 실제 API 사용 가능 상태가 아니며 네트워크 변경 승인 대기 유지.
- 위험 요소: 실제 프론트 호출 및 서버 연결 미검증. 예상 밖 변경 없음.
- 다음 작업: 승인 후 테스트 네트워크/서비스 구성 및 기동 검증.

## 2026-09-23 — 테스트 SG·ALB 규칙·ECS 서비스 생성 완료

- 브랜치: develop. Jira 미지정.
- 작업 목표: 승인된 테스트 네트워크 및 서비스 구성 후 기동 준비 확인.
- 변경 파일: docs/contracts/identity-branch-deployment.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 수행 내용: test SG sg-03c6bd60c4026a227 생성(ALB만8081 수신, IPv4 443/27017 송신), priority30 exact test host→test TG 규칙 생성. 테스트 ECS service 생성 성공/ACTIVE 및 desired0/running0 확인. 사설 subnet 2개, test SG만 연결, public IP/Exec/Auto Scaling OFF, Fargate1.4.0, health grace300초 저장 확인.
- 테스트와 결과: AWS 성공 메시지/저장 설정 확인. GitHub API 원격 develop workflow 읽기 결과 운영 대상 그대로임 확인, dig DNS NXDOMAIN 확인. git diff --check 수행. 제품 변경 없어 Gradle 미실행, 실제 기동/E2E 미검증.
- 유지한 계약: 기존 운영 service/SG/IAM/Secret 값 미변경. 공유 ALB에는 승인된 별도 규칙만 추가, 기존10/20/default 유지. 비밀값 미기록. 기존 로컬 변경 보존. 예상 밖 변경 없음.
- 결정사항: 아직 task 기동 안 함. 원격 기존 workflow는 실행 금지, 사용자 commit/push 후 새 테스트 이미지 사용. 가비아 test CNAME 연결 필요.
- 위험 요소: 실제 이미지 pull/Atlas 권한/Secret 주입/네트워크 송수신 미검증. 초기 desired0 및 DNS 미연결로 API 사용 불가, 기존 원격 workflow 실행 시 운영 변경 위험.
- 다음 작업: 사용자 workflow commit/push 및 DNS 연결, 테스트 revision/이미지 확인 후 desired1 기동 및 health/JWKS/로그/가입 검증. 이번 commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): 승인된 테스트 SG/ALB 규칙/ECS service 생성 완료, desired0 유지. 원격 workflow 반영·DNS 연결 후 실제 기동 검증 필요. 운영 service 변경 없음.

## 2026-09-23 — 테스트 인프라 생성 작업 식별 기록 보완

<!-- codex-turn:01a0cd90-1e27-76f1-a7fb-81e2903fa55f -->

- 브랜치: develop.
- 작업 목표: 이번 테스트 SG·ALB 규칙·ECS 서비스 생성 결과에 현재 작업 식별자 연결.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 테스트 인프라 생성 완료 및 desired0 유지, 원격 workflow 반영/DNS 연결 대기 상태 기록 보완. 과거 기록 보존.
- 테스트와 결과: git diff --check 수행. 기록만 보완하여 Gradle 미실행.
- 유지한 계약: 추가 외부 변경 없음, 비밀값 미기록, 기존 미커밋 변경 보존.
- 결정사항: 실제 테스트 서버 기동은 아직 미수행.
- 위험 요소: 원격 workflow는 운영 대상이며 test DNS는 NXDOMAIN. 실제 연결 및 인증 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 commit/push 및 DNS 연결 후 테스트 이미지 배포와 기동 검증.

## 2026-09-23 — CNAME 등록 실패 확인 요청

<!-- codex-turn:01a0cd9a-63c5-7d63-af9d-3a7af1766713 -->

- 브랜치: develop.
- 작업 목표: 사용자 CNAME 등록 실패의 원인 확인 준비.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 가비아 실제 오류 문구/입력 화면 확인 요청. 호스트 identity-test와 ALB DNS 값 입력 방식, 동일 호스트 레코드 충돌 가능성을 안내하되 원인 확정하지 않음.
- 테스트와 결과: git diff --check. 안내/기록만 변경하여 Gradle 미실행, DNS 재조회 및 외부 설정 변경 없음.
- 유지한 계약: 기존 DNS/인증서 검증 레코드 삭제 또는 수정 없음. 비밀값 미기록, 기존 변경 보존.
- 결정사항: 실제 오류 확인 전 DNS 변경하지 않음.
- 위험 요소: 입력 형식/기존 레코드/권한 중 원인 미확인. 예상 밖 변경 없음.
- 다음 작업: 오류 문구 또는 가비아 화면 확인 후 정확한 해결 방법 안내.

## 2026-09-23 — 테스트 도메인 DNS 및 HTTPS 연결 확인

- 브랜치: develop.
- 작업 목표: 사용자 CNAME 설정 결과 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: dig로 테스트 도메인의 CNAME이 지정한 ALB DNS와 일치함 확인. TLS 검증을 유지한 curl health 요청에서 HTTP503 수신.
- 테스트와 결과: DNS NOERROR/TTL600 및 HTTPS503 확인, git diff --check 수행. 제품 변경 없어 Gradle 미실행.
- 유지한 계약: DNS/AWS/코드 변경 없이 읽기 전용 확인. 비밀값 미기록, 기존 변경 보존. 예상 밖 변경 없음.
- 결정사항: DNS 연결 문제는 해소. HTTP503은 직전 desired0 상태와 부합하며 실제 서비스 정상 기동 완료를 의미하지 않음.
- 위험 요소: 현재 ECS 상태 재조회 및 이미지/DB/인증 기동 검증은 미수행. 원격 workflow 수정 반영 여부도 후속 확인 필요.
- 다음 작업: 사용자 workflow commit/push 후 테스트 이미지 및 revision 확인, 서버 기동/health/JWKS 검증.

## 2026-09-23 — DNS 연결 확인 작업 식별 기록 보완

<!-- codex-turn:01a0cd9b-75fb-7211-9de9-7c090e18cdd7 -->

- 브랜치: develop.
- 작업 목표: 이번 DNS/HTTPS 확인 결과에 현재 작업 식별자 연결.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: CNAME 일치 및 TLS 검증 유지 HTTPS503 확인 결과 기록 보완. 과거 기록 보존.
- 테스트와 결과: git diff --check 수행. 문서만 보완하여 Gradle 미실행.
- 유지한 계약: 추가 외부 변경 없음, 비밀값 미기록, 기존 변경 보존.
- 결정사항: DNS 연결 확인 완료, 실제 서버 정상 기동은 미완료.
- 위험 요소: 현재 ECS 상태/이미지/DB/인증 기동 미검증. 예상 밖 변경 없음.
- 다음 작업: workflow 반영 후 테스트 이미지 배포와 기동 검증.

## 2026-09-23 — develop 최초 테스트 배포 및 시작 실패 진단

- 브랜치: develop.
- 작업 목표: 사용자 push 확인 및 테스트 서버 기동.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 제품 코드 변경 없음.
- 수행 내용: 원격 develop 88ff5bed 및 분기별 workflow 확인. Actions 35843168284 테스트/이미지 빌드/ECS 배포 성공, health503 실패 확인. 테스트 서비스 test:2 desired0에서1 기동, CloudWatch 구조화 로그에서 PhoneEligibilityFingerprintHasher bean 부재 확인. 반복 시작 실패를 막기 위해 desired0 원복 요청.
- 원인: Firebase signup은 eligibility hasher를 필수 주입하지만 테스트 task 설정은 PHONE_ELIGIBILITY_BINDING_ENABLED=false. Billing 외부 publisher 비활성화와 내부 가입 의존성의 구분이 누락됨.
- 테스트와 결과: CI Run tests 성공, 기동 후 HTTPS502 및 application startup failure 확인. 로컬 제품 변경 없어 Gradle 재실행 생략. git diff --check 수행.
- 유지한 계약: 운영 서비스/권한/키 변경 없음, 비밀값 비노출, 사용자 변경 보존. 예상 밖 파일 변경 없음.
- 결정사항: 내부 binding 설정과 전용 키 준비 후 재기동하며 외부 Billing publisher는 OFF 유지. 이번에는 Secret 생성이나 코드 변경하지 않음.
- 위험 요소: DB/인증/JWKS 정상 동작 미검증, CI run은 health 실패 상태로 남음.
- 다음 작업: binding ON, consumer scope, 전용 keyring 주입 범위 확정 및 테스트 task revision 보완 후 health/회원 인증 검증.
- 원복 검증: AWS 서비스 업데이트 성공 알림 및 desired0/running0/pending0 확인 완료, test:2 유지.

## 2026-09-23 — 최초 테스트 배포 진단 작업 식별 기록

<!-- codex-turn:01a0cd9e-ef7f-79e3-8e8d-9535653d9c29 -->

- 브랜치: develop.
- 작업 목표: push 이후 테스트 배포 및 기동 진단 결과의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: CI 테스트/빌드/ECS 배포 성공과 health 실패 확인. 테스트 기동 중 PhoneEligibilityFingerprintHasher 필수 bean 누락 확인, 테스트 서비스 desired0/running0/pending0 원복 검증.
- 실행한 테스트와 결과: CI Run tests 성공. 실제 기동 실패 확인. 문서 보완만 수행하여 로컬 Gradle 미실행, git diff --check 통과.
- 유지한 계약: 운영 서비스 변경 없음, 과거 기록 및 기존 변경 보존, Secret 비기록.
- 결정사항: 내부 eligibility binding 설정 보완 필요. 외부 Billing publisher OFF 유지. 해당 hasher 키는 전화번호 식별용 HMAC 키이며 응답 복구 암호화 키와 구분한다.
- 위험 요소: 실제 가입/로그인/JWKS 정상 동작은 아직 검증하지 못함. 예상 밖 파일 변경 없음.
- 다음 작업: 테스트 내부 binding ON 및 consumer scope/전용 HMAC keyring 준비 후 새 task revision 기동 검증.

## 2026-09-23 — 테스트 내부 eligibility binding 설정 보완 준비

- 브랜치: develop.
- 작업 목표: 가입 필수 hasher 누락 기동 실패를 해결할 설정 준비.
- 변경 파일: docs/contracts/identity-test-task-definition.draft.json, identity-test-container-secrets.json, identity-branch-deployment.md, src/test/java/web/tosunsaeng/identity/deployment/DeploymentTargetTests.java, docs/codex/WORKLOG.md, CURRENT_STATE.md.
- 구현 내용: 내부 binding true, 테스트 consumer scope, 기존 테스트 Secret의 독립 HMAC key 항목 selector 준비. 외부 publisher false 유지. 설정 회귀 테스트 추가. 사용자 키 저장용 Secret 화면 열기.
- 실행한 테스트와 결과: ./gradlew clean test --no-daemon 성공(초기 sandbox Gradle lock 접근 실패 후 승인된 실행 성공), git diff --check 수행.
- 유지한 계약: API/제품 로직/운영 서비스/IAM 변경 없음, 비밀값 미생성/미기록, 기존 fingerprint 키 변경 없음. 기존 사용자 문서 수정 보존, 예상 밖 파일 변경 없음.
- 결정사항: 기존 승인된 Secret 안에 별도 키 항목을 사용해 IAM 확대 없이 주입. 새 키 입력/저장은 사용자 수행.
- 위험 요소: AWS 설정 미적용, 실제 health/인증 검증 미완료. 로컬 draft의 과거 image를 재사용하지 않고 현재 AWS test:2 기준으로 revision 생성 필요.
- 다음 작업: 사용자 키 저장 완료 확인 후 새 task revision/서비스 기동 및 health/JWKS 검증. 배포 전 독립 키 항목 존재와 selector 확인 필수. Jira 연결 없음, 자동 댓글 미등록.

## 2026-09-23 — 내부 binding 설정 보완 작업 식별 기록

<!-- codex-turn:01a0cda6-ba83-7112-91d2-ce73aefcfc96 -->

- 브랜치: develop.
- 작업 목표: 테스트 가입 필수 내부 binding 설정 보완 결과와 사용자 키 저장 대기 상태 기록.
- 변경 파일: docs/contracts/identity-test-task-definition.draft.json, docs/contracts/identity-test-container-secrets.json, docs/contracts/identity-branch-deployment.md, src/test/java/web/tosunsaeng/identity/deployment/DeploymentTargetTests.java, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 내부 binding ON/테스트 consumer scope/독립 HMAC key JSON selector 준비 및 회귀 테스트 추가. Secrets Manager 대상 화면을 사용자에게 인계.
- 실행한 테스트와 결과: ./gradlew clean test --no-daemon 성공, 147 suites/958 tests/0 failures/0 errors. git diff --check 통과.
- 유지한 계약: 외부 publisher OFF, 운영/API/IAM 변경 없음. 기존 키 및 사용자 변경 보존, Secret 미기록.
- 결정사항: 새 비밀키 입력과 저장은 사용자 수행. AWS task revision 변경 및 재기동은 저장 이후 진행.
- 위험 요소: 로컬 설정만 보완된 상태이며 실제 서버 정상 기동/인증 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 저장 완료 확인 후 현재 AWS test:2 기반 새 revision을 적용하고 health/JWKS 검증.

## 2026-09-23 — 테스트 task:3 적용 및 HMAC keyring 형식 진단

- 브랜치: develop.
- 작업 목표: 사용자 Secret 저장 후 새 설정 배포와 정상 기동 확인.
- 변경 파일: docs/contracts/identity-test-task-definition.draft.json(배포 이미지 정합), docs/contracts/identity-branch-deployment.md, docs/codex/WORKLOG.md, CURRENT_STATE.md.
- 수행 내용: Secret 저장 성공과 새 항목 존재 확인. AWS test:2 JSON 편집기에서 현재 이미지를 검증하고 내부 binding ON/consumer scope/독립 Secret selector만 변경해 test:3 등록, 테스트 서비스 desired1 적용. CloudWatch 기동 실패 확인 후 desired0/running0/pending0 원복 완료.
- 진단: bean 누락은 해결됐으나 Phone eligibility binding configuration is invalid 발생. UI 값의 형식만 검사해 Base64 32바이트 단독 저장 및 버전/ACTIVE_WRITE 접두사 누락 확인. 비밀값은 출력/기록하지 않음.
- 테스트와 결과: 실제 HTTPS502/503 및 기동 실패 확인, 정상 health/JWKS 미검증. 이전 전체 958개 테스트 통과 유지, 이번 제품 로직 변경 없어 Gradle 미실행. JSON 파싱/git diff --check 수행.
- 유지한 계약: 기존 배포 이미지/서명/외부 publisher OFF 보존, IAM 및 운영 변경 없음. 사용자 변경 보존, 예상 밖 파일 변경 없음.
- 결정사항: 비밀값 직접 수정은 사용자에게 인계. 기존 값에 test-v1,ACTIVE_WRITE, 접두사 추가 후 test:3 재기동. 새 키 생성 불필요.
- 위험 요소: 서버는 중지 상태이며 최초 가입/로그인/DB 검증 미완료. 구버전 task의 과거 오류가 같은 로그 목록에 존재하므로 시각/태스크 구분 필요.
- 다음 작업: 사용자 형식 보완 저장 후 task:3 기동 및 health/JWKS 확인. Jira 작업 없음.

## 2026-09-23 — task:3 배포 진단 작업 식별 기록

<!-- codex-turn:01a0cdaa-0452-7252-88aa-880e36c157cd -->

- 브랜치: develop.
- 작업 목표: 테스트 배포 및 keyring 형식 오류 진단 결과 기록.
- 변경 파일: docs/contracts/identity-test-task-definition.draft.json, docs/contracts/identity-branch-deployment.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 이미지 88ff5bed 유지한 test:3 등록/기동. 내부 binding bean 누락은 해결됐으나 keyring 형식 오류로 시작 실패. 비밀값 노출 없이 버전/상태 접두사 누락 확인. 테스트 서비스 desired0/running0/pending0 원복 검증.
- 테스트와 결과: 실제 health502/503 및 시작 실패 확인, JSON 검증/git diff --check 통과. 제품 로직 변경 없어 Gradle 재실행 생략; 직전 전체 958개 테스트 통과.
- 유지한 계약: 운영/IAM/외부 publisher 설정 변경 없음, 비밀값 미기록, 사용자 변경 보존. 예상 밖 변경 없음.
- 결정사항: 사용자에게 기존 값의 접두사 추가 저장 인계. 새 비밀키 생성 불필요.
- 위험 요소: 정상 health/JWKS 및 회원 인증 검증 미완료.
- 다음 작업: 사용자 저장 후 task:3 재기동과 상태 검증.

## 2026-09-23 — 사용자 재저장 후 keyring 형식 재확인

- 브랜치: develop.
- 작업 목표: 키 형식 보완 확인 후 재기동 여부 판단.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Secret 저장 성공 표시 확인 후 화면 재진입/조회. ELIGIBILITY 키 항목은 여전히 Base64 단독, IDENTITY 항목은 버전/상태 포함 구조임을 값 비출력 검사로 확인. 비밀값 화면 닫고 인계.
- 실행한 테스트와 결과: UI 형식 검사에서 ELIGIBILITY 접두사 누락 확인. 제품 변경 없어 Gradle 미실행. git diff --check 수행.
- 유지한 계약: AWS 설정/Secret/서비스 실행 수 변경 없음. 비밀값 미기록 및 기존 수정 보존.
- 결정사항: 형식이 맞기 전 서버를 재기동하지 않음. 정확한 ELIGIBILITY 항목 편집 재안내.
- 위험 요소: 테스트 서버 중지 유지, health/JWKS 검증 미완료. 예상 밖 변경 없음.
- 다음 작업: 사용자 ELIGIBILITY 값 접두사 추가 저장 후 검증과 task:3 재기동.

## 2026-09-23 — keyring 재확인 작업 식별 기록

<!-- codex-turn:01a0cdb0-59de-7e23-8903-8fa69fb5a7a2 -->

- 브랜치: develop.
- 작업 목표: 사용자 재저장 결과 및 기동 보류 상태 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 재조회한 ELIGIBILITY 항목의 버전/상태 접두사 누락을 비밀값 출력 없이 확인하고 정확한 수정 항목 안내.
- 테스트와 결과: UI 형식 검사에서 접두사 누락 확인. 문서만 변경하여 Gradle 미실행, git diff --check 통과.
- 유지한 계약: Secret/운영/서비스 설정 변경 없음. 비밀값 미기록, 사용자 변경 보존.
- 결정사항: 형식 보완 전 재기동하지 않음.
- 위험 요소: 정상 health/JWKS 검증 미완료. 예상 밖 변경 없음.
- 다음 작업: 사용자 ELIGIBILITY 값 보완 저장 후 task:3 재기동 검증.

## 2026-09-23 — 저장 결과 이의에 따른 최신 Secret 재조회

- 브랜치: develop.
- 작업 목표: 사용자 저장 완료와 관측 불일치 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 편집 dialog 없음 확인 후 페이지 전체 reload 및 Secret 값 재조회. 대상 ELIGIBILITY cell 형식만 검사해 쉼표 구분 필드 1개 확인(요구 3개). 값 출력 없이 비밀값 화면 닫고 인계.
- 테스트와 결과: UI read-only 재조회 및 형식 검사 수행. 제품 변경 없어 Gradle 미실행, git diff --check 수행.
- 유지한 계약: Secret/서버/운영 변경 없음. 비밀값 비기록, 기존 수정 보존.
- 결정사항: 저장 대상/필드 일치 여부 확인 필요. 캐시 또는 사용자 실수로 원인을 단정하지 않음.
- 위험 요소: 정상 기동 검증 미완료, 실제 값 변경 원인 미확인. 예상 밖 변경 없음.
- 다음 작업: 사용자와 대상 Secret/ELIGIBILITY 필드 확인 후 형식 정상 시 재기동.

## 2026-09-23 — 최신 Secret 재조회 작업 식별 기록

<!-- codex-turn:01a0cdb2-6518-7e00-9096-ff3d03896df6 -->

- 브랜치: develop.
- 작업 목표: 사용자 저장 결과와 관측 불일치 재확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Secret 페이지 전체 새로고침 후 ELIGIBILITY 항목 형식만 검사, 쉼표 구분 필드 1개 확인. 사용자에게 편집 대상 확인 요청. 비밀값 화면 닫음.
- 테스트와 결과: 읽기 전용 UI 재조회 수행, git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: Secret/서버/운영 변경 없음, 비밀값 비기록, 기존 변경 보존.
- 결정사항: 원인을 단정하지 않고 사용자 편집 화면에서 대상 필드 확인 예정.
- 위험 요소: 저장 관측 불일치 원인 및 정상 서버 기동 미확인. 예상 밖 변경 없음.
- 다음 작업: 대상 필드 확인 및 형식 정상 확인 후 test:3 재기동.

## 2026-09-23 — 테스트 Identity 최초 정상 기동 확인

- 브랜치: develop.
- 작업 목표: 사용자 수정 형식 확인 후 테스트 서버 재기동 및 기본 정상 응답 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md, docs/contracts/identity-branch-deployment.md.
- 수행 내용: Secret 값 비출력 검사에서 keyring 3필드/정상 버전/ACTIVE_WRITE/Base64 32바이트 확인. Secret 편집 없이 기존 test:3 desired1 적용. running1/pending0, CloudWatch 시작 완료(19:03 KST) 및 Mongo 트랜잭션 지원 확인 로그 확인.
- 실행한 테스트와 결과: TLS 검증 유지 HTTPS health200/UP, JWKS RSA/RS256/공개 kid 정상 확인. 초기화 중502/503 이후 정상 전환. 문서/운영 기동만 변경하여 Gradle 재실행 생략, 직전 전체958개 테스트 통과. git diff --check 수행.
- 유지한 계약: 동일 이미지88ff5bed/테스트 task:3, 운영/IAM/Secret 미변경, 외부 publisher OFF 유지. 비밀값 비출력/비기록, 기존 사용자 변경 보존.
- 결정사항: 테스트 서비스1개 실행 유지. 앱 인증 연동 검증 단계로 진행 가능.
- 위험 요소: 앱 Google/phone 가입/로그인/재발급 및 실제 rollback 미검증. CI 기존 실패 이력은 남아 있음. 예상 밖 파일 변경 없음.
- 다음 작업: 프론트 테스트 base URL 안내 및 실제 MEMBER 인증 흐름/LC 연동 검증. 로컬 설정/기록 변경은 사용자 commit/push 대상, Jira 변경 없음.

## 2026-09-23 — 테스트 서버 정상 기동 작업 식별 기록

<!-- codex-turn:01a0cdb6-1084-7162-be93-903d3d595ccf -->

- 브랜치: develop.
- 작업 목표: 사용자 keyring 보완 확인 및 테스트 서버 정상 기동 결과 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md, docs/contracts/identity-branch-deployment.md.
- 수행 내용: 비밀값 비출력 형식 검증 통과 후 test:3 desired1 기동. running1/pending0 및 CloudWatch 시작 완료/Mongo 트랜잭션 지원 확인 로그 확인.
- 테스트와 결과: HTTPS health200/UP, JWKS RS256 공개키 메타데이터 확인. git diff --check 통과. 제품 코드 변경 없어 Gradle 재실행 생략.
- 유지한 계약: 운영/IAM/Secret 변경 없음, 기존 배포 이미지 및 외부 publisher OFF 유지. 비밀값 미기록, 기존 변경 보존.
- 결정사항: 테스트 서비스1개 실행 유지, 앱 인증 검증 단계로 인계.
- 위험 요소: 실제 가입/로그인/재발급 및 rollback 미검증, 기존 CI health 실패 이력 유지. 예상 밖 변경 없음.
- 다음 작업: Android Google/phone 연결 및 MEMBER 가입/로그인/재발급 검증.

## 2026-09-23 — Learning Core용 JWT 및 배포 검증 상태 인계

<!-- codex-turn:01a0cdbc-58c1-7d40-907e-47144abe8ae9 -->

- 브랜치: develop.
- 작업 목표: issuer/JWKS/kid/commit/인증 및 claim 검증 상태 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: AWS test:3 JSON의 issuer/audience/image/release 재확인. HTTPS JWKS에서 공개 kid 및 RS256 확인. git show로 배포 commit의 JwtAccessTokenIssuer를 확인해 UUID 검증/subject/account_type/LC 및 Billing audience 구성 확인. 기존 claim 테스트 assertion 확인.
- 테스트와 결과: 실제 JWKS 조회 정상. 직전 전체958개 테스트 통과 이력과 실제 인증 E2E 미검증을 구분. 이번 코드 변경 없어 Gradle 미실행, git diff --check 수행.
- 유지한 계약: 읽기 전용 조회, 운영/테스트 설정 변경 없음, 실제 토큰/비밀키 비기록. 기존 변경 보존.
- 결정사항: issuer는 실제 배포 설정값 확인이며 실제 발급 토큰의 iss 검증 완료로 표현하지 않음. 가입/로그인/재발급 성공은 아직 미검증.
- 위험 요소: 실제 MEMBER 발급 및 재발급 claim/서명 E2E 검증 필요. 예상 밖 변경 없음.
- 다음 작업: Android Firebase 인증 후 테스트 가입/로그인/재발급 성공과 발급 JWT 계약 검증. Jira 변경 없음.

## 2026-09-23 — 비용 절감을 위한 테스트 서비스 중지

<!-- codex-turn:01a0cdc7-7a37-72f3-bbe3-9a987688a90c -->

- 브랜치: develop.
- 작업 목표: 사용자 요청에 따라 다음 테스트까지 테스트 Identity 실행 중지.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: AWS 테스트 서비스 test:3의 desired count1→0 변경 및 업데이트 성공 확인. 운영/DB/Secret/ALB/배포 revision은 변경하지 않음.
- 테스트와 결과: AWS UI 상태 확인, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 설정과 데이터 보존, 운영 서비스 미변경, 비밀값 미기록. 예상 밖 파일 변경 없음.
- 결정사항: 재개 시 desired1 적용 후 health/JWKS 확인. 별도 리소스 삭제 없음.
- 위험 요소: 중지 동안 테스트 API 접근 불가. ALB/Secret/로그 등 잔여 비용 가능, 서비스0 상태 배포 workflow health 실패 가능.
- 다음 작업: 사용자 테스트 재개 요청 시 기동 후 앱 인증 E2E 검증. Jira 변경 없음.
- 최종 검증: 서비스 새로고침 후 desired0/running0/pending0 확인 완료. 테스트 태스크 종료 확인.

## 2026-09-27 — 구·신 서버 분리 및 업데이트 유예 계획 전달 이력 확인

<!-- codex-turn:01a0e309-e393-78c3-b4f6-520be76424ca -->

- 날짜/브랜치: 2026-09-27, develop. 이번 요청의 Jira 지정 없음.
- 작업 목표: 사용자 제시 세 가지 전환 계획의 과거 전달 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md (기록만).
- 수행 내용: 2026-09-22 구버전 주소 분리·웹뷰 안내·1주 유예 검토 기록 및 guest-app-update-transition-review.md를 확인했다. 구·신 서버 분리, 피드백 진입 전 안내, 사용량에 따른 유예 조정 계획이 이미 기록되어 있으며 후속 피드백 result.updateRequired 계약도 확인했다.
- 실행한 테스트와 결과: 저장소 기록 검색/원문 대조 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 기존 작업/과거 기록 보존. API·서버·배포·Jira·기능 설정 변경 없음. 비밀정보 미기록.
- 결정사항: 이전에 전달한 계획이라고 답변하되 계획 합의와 구현·배포 완료를 구분한다.
- 위험 요소: 이번에는 실제 Learning Core/웹뷰 구현 및 운영 전환 상태를 확인하지 않음. 예상 밖 변경 없음.
- 다음 작업: 별도 요청이 있으면 해당 계획 기준으로 실제 구현/배포 준비 상태를 점검한다.

## 2026-09-27 — SNS·10초 챌린지 선출시와 무료 모의고사·결제 후속 출시 검토

<!-- codex-turn:01a0e309-e393-78c3-b4f6-520be76424ca -->

- 날짜/브랜치: 2026-09-27, develop. 별도 Jira 지정 없음.
- 작업 목표: SNS 로그인·10초 챌린지를 먼저 출시하고 전화번호당 1회 무료 모의고사와 결제를 후속 업데이트로 분리할 수 있는지 설명한다.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md (분석 기록만).
- 조사 근거: Identity FirebaseSignupService/FirebaseGuestUpgradeService의 전화번호·eligibility hasher 의존성, PhoneEligibilityPublisherConfiguration의 별도 활성화 조건, identity-branch-deployment.md의 내부 binding ON/외부 publisher OFF 구분 확인. Learning Core application.yml/ChallengeController/ChallengeService/ExamServiceImpl을 읽어 독립 challenge flag와 Billing OFF 시 기존 모의고사 생성 분기를 확인했다. Learning Core 파일은 수정하지 않았다.
- 구현 내용: 없음. 단계 출시 방향은 가능하되 배포 검증 완료와 구분하고 전화번호 인증 자체의 후속 연기는 별도 변경임을 안내한다.
- 테스트와 결과: 코드·설정 읽기 대조 및 git diff --check. 코드 변경 없어 Gradle 미실행, 외부 서비스/실제 앱 E2E 미수행.
- 유지한 계약: Identity와 Learning Core/Billing 도메인 경계, 현재 가입 인증·내부 식별 계약, 기존 작업과 기록 보존. 서버·기능 flag·Jira 변경 없음. 비밀정보 미기록.
- 결정사항: 1차 SNS/챌린지 및 기존 모의고사 정책 유지, 2차 무료 자격/결제 동시 도입을 권고한다. Billing OFF만으로 모의고사가 차단되거나 신규 1회 제한이 적용되는 것은 아니다. 사용자 출시 범위 확정 또는 구현 승인으로 간주하지 않는다.
- 위험 요소: 기존 Guest 기록 승계, 앱 전환, 챌린지 AI/저장소/실기기 연동 확인 필요. 2차 도입 시 기존 사용자에게 새 무료 1회 부여 여부와 이전 이용의 소급 계산 정책, 지연 이벤트/기존 전화번호 binding 인계 검증 필요. 예상 밖 변경 없음.
- 다음 작업: 1차 모의고사 정책과 2차 기존 사용자 무료 자격 기준을 확정한 후 별도 출시 체크리스트 및 환경별 기능 설정을 검증한다. 이번 배포/commit/push 없음.

## 2026-09-27 — 단계 출시 검토 작업 식별 기록 보완

<!-- codex-turn:01a0e30b-5569-7513-b978-5cb92ec91400 -->

- 날짜/브랜치: 2026-09-27, develop. Jira 지정 없음.
- 작업 목표: SNS 로그인·10초 챌린지 선출시와 무료 모의고사·결제 후속 출시 검토의 현재 turn 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 앞선 분석은 현재 작업의 결과이며 이전 turn marker로 기록된 것을 과거 기록 수정 없이 새 항목으로 보완했다. 기능 분리 가능성과 현재 가입의 전화번호 인증/내부 식별 의존성, 기존 사용자 무료 자격 정책 필요를 확인했다.
- 테스트와 결과: git diff --check 통과. 분석 및 기록만 변경하여 Gradle 미실행, 실제 앱/운영 E2E 미검증.
- 유지한 계약: 과거 기록과 기존 작업 보존. 코드/API/서버/기능 설정/배포/Jira 변경 없음. 비밀정보 미기록.
- 결정사항: 1차 SNS·챌린지, 2차 무료 자격·결제 적용을 권고했으며 구현 승인이나 출시 준비 완료로 간주하지 않는다.
- 위험 요소: Guest 승계·챌린지 연동·기존 사용자 자격 이행 검증 필요. 예상 밖 변경 없음.
- 다음 작업: 기존 모의고사 정책과 2차 무료 자격 기준 결정 후 출시 검증 범위 확정.

## 2026-09-28 — 테스트 서버 Swagger 사용 조건 확인

<!-- codex-turn:01a0e5b3-b827-74f0-b3a7-be6caf06d2d0 -->

- 브랜치: develop.
- 작업 목표: 테스트 서버 재기동 시 Swagger 사용 가능 여부 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 조사 내용: 저장된 task draft SWAGGER_ENABLED=false, application.yml UI /swagger-ui.html 및 spec /v3/api-docs, SecurityConfig 문서 GET permitAll 확인.
- 테스트와 결과: 설정/코드 읽기 및 git diff --check. 분석/기록만 수행하여 Gradle 미실행. AWS 실시간 상태 조회 없음.
- 유지한 계약: 코드/API/AWS/기능 설정/서비스 기동 변경 없음. 기존 변경 보존 및 비밀정보 미기록.
- 결정사항: 사용하려면 테스트 Swagger 활성 설정 및 재배포/기동 필요. 보호 API는 Identity Access Token 사용, Firebase 인증은 앱 SDK에서 별도 진행.
- 위험 요소: Swagger 활성화 시 공개 문서 접근 가능하므로 접근 제한 검토 필요. 문서/실행 API는 배포 버전 기준. 예상 밖 변경 없음.
- 다음 작업: 사용자 활성화 요청 시 접근 범위를 확정한 뒤 테스트 설정 적용 및 UI/API 검증.

## 2026-09-28 — 테스트 Swagger 활성화 설정 준비

- 브랜치: develop.
- 작업 목표: Learning Core 준비 후 함께 기동할 테스트 Identity의 Swagger 활성화 준비.
- 변경 파일: docs/contracts/identity-test-task-definition.draft.json, src/test/java/web/tosunsaeng/identity/deployment/DeploymentTargetTests.java, docs/contracts/identity-branch-deployment.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 테스트 draft SWAGGER_ENABLED=true, 배포 설정 회귀 assertion 추가. 기존 사용자 변경 보존. AWS 세션 만료 확인 후 ISB 재로그인 페이지 인계.
- 테스트: ./gradlew clean test --no-daemon 실행. 최종 결과는 아래 추가 기록.
- 유지한 계약: 보호 API JWT 인증 유지, 운영/Secret/IAM/실행 수 변경 없음. commit/push 미수행, 비밀값 미기록.
- 결정사항: 테스트 실행은 지금 하지 않음. 로그인 후 현재 AWS revision에 설정만 적용하고 desired0 유지.
- 위험 요소: 실제 ECS 설정 미반영, live Swagger 미검증. 문서 공개 활성화 요청이며 실제 데이터 API는 인증 유지. 기존 미커밋 변경 외 예상 밖 변경 없음.
- 다음 작업: 사용자 AWS 재로그인 후 테스트 설정 적용, 나중에 기동 요청 시 health/UI/spec 검증. Jira 연결 없음.
- 최종 테스트 결과: ./gradlew clean test --no-daemon BUILD SUCCESSFUL(31초), git diff --check 통과. 실제 AWS 반영은 로그인 대기로 미완료.

## 2026-09-28 — Swagger 활성화 준비 작업 식별 기록

<!-- codex-turn:01a0e5b5-3981-76a2-b63d-3e2160a31201 -->

- 브랜치: develop.
- 작업 목표: 테스트 Swagger 활성화 준비 결과와 AWS 반영 대기 상태 기록.
- 변경 파일: docs/contracts/identity-test-task-definition.draft.json, src/test/java/web/tosunsaeng/identity/deployment/DeploymentTargetTests.java, docs/contracts/identity-branch-deployment.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 로컬 테스트 draft의 SWAGGER_ENABLED=true 및 회귀 assertion 추가. AWS 세션 만료로 로그인 페이지 인계.
- 테스트와 결과: ./gradlew clean test --no-daemon 성공, git diff --check 통과.
- 유지한 계약: 운영/Secret/IAM/실행 수 변경 없음, 보호 API JWT 인증 유지. 비밀값 미기록, 기존 변경 보존.
- 결정사항: 사용자 재로그인 후 테스트 설정만 반영하고 desired0 유지. 이번 실제 배포/기동 없음.
- 위험 요소: AWS 반영 및 live Swagger 검증 미완료. 예상 밖 변경 없음.
- 다음 작업: 재로그인 후 최신 테스트 revision 확인 및 Swagger 설정 반영, 이후 사용자 기동 요청 시 정상 응답 검증.

## 2026-09-28 — AWS 테스트 Swagger 설정 실제 적용

<!-- codex-turn:01a0e5bb-8e86-7c92-bf1d-ce1147c7225d -->

- 브랜치: develop.
- 작업 목표: 재로그인 후 테스트 Swagger 활성화 설정을 반영하되 서버 중지 유지.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md, docs/contracts/identity-branch-deployment.md.
- 수행 내용: AWS 현재 테스트 서비스 test:3/desired0 확인. 현재 revision JSON에서 SWAGGER_ENABLED만 true로 변경하여 test:4 생성, 서비스에 적용. 성공 알림 및 test:4/desired0/running0/pending0 확인.
- 테스트와 결과: AWS 저장 및 연결 상태 UI 검증, git diff --check 수행. 이번 제품 코드 변경 없어 Gradle 재실행 생략(직전 clean test 성공). 서버를 켜지 않아 live Swagger/health 미검증.
- 유지한 계약: 이미지88ff5bed/인증/Secret/IAM/네트워크 보존, 운영 미변경. 기존 사용자 변경 보존 및 비밀값 미기록.
- 결정사항: 테스트 Swagger 설정 준비 완료, 사용자 요청 전 기동하지 않음. 자동 기동 예약 없음.
- 위험 요소: Swagger 문서는 기동 후 공개 접근되며 보호 API JWT 인증은 유지. 실제 UI/spec 응답은 미검증. 예상 밖 변경 없음.
- 다음 작업: Learning Core 준비 후 사용자 기동 요청 시 desired1 및 health/JWKS/Swagger 검증. Jira 변경 없음.

## 2026-09-28 — Swagger 활성 테스트 Identity 기동

<!-- codex-turn:01a0e5dc-f4ed-7513-bd4c-ac48de99b1bb -->

- 브랜치: develop.
- 작업 목표: 사용자 요청에 따라 테스트 서버 기동 및 공개 진단 응답 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: test:4 유지하며 테스트 서비스 desired0→1 적용. AWS running1/pending0 확인. 운영 서비스/이미지/Secret/IAM/네트워크 변경 없음.
- 테스트와 결과: 초기502/503 이후 HTTPS health200/UP. Swagger UI 리다이렉트 후200, OpenAPI JSON3.1.0/Identity API/23경로 확인, JWKS RSA/RS256/테스트 kid 확인. git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 사용자 API 인증 유지, 테스트 데이터/설정 보존, 비밀값 미기록. 기존 변경 보존, 예상 밖 파일 변경 없음.
- 결정사항: 테스트 태스크1개 실행 유지, Swagger URL 인계. 실행 중 Fargate 비용 발생.
- 위험 요소: 실제 Google/phone 가입/로그인/재발급 및 Learning Core E2E는 미검증. Swagger UI HTTP/명세 응답만 확인했고 브라우저 Try it out은 실행하지 않음.
- 다음 작업: 프론트 또는 Swagger를 통한 승인된 테스트 계정 인증 검증. Jira 변경 없음.

## 2026-09-28 — Identity·Learning Core 사전 HTTP smoke 및 Postman collection

<!-- codex-turn:01a0e621-9da6-7d80-b98a-3fa688b86cc0 -->

- 브랜치: develop.
- 작업 목표: 앱 연동 전 양쪽 테스트 API 사전 검증.
- 변경 파일: docs/postman/identity-learning-smoke.postman_collection.json, docs/postman/README.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 제공 LC 테스트 주소/계정 미준비 확인. Postman 제어 권한 부재로 동일 HTTP 요청 직접 검사. health/JWKS/인증 거절 테스트와 공개 명세 조회, 비밀값 없는10요청 collection 작성.
- 테스트와 결과: 두 health200/UP, Identity 프로필 무토큰/잘못된 토큰401, exchange 빈 body/잘못된 token401, 잘못된 refresh401, LC today 무토큰/잘못된 token401. JWKS RS256 테스트 kid 정상. collection JSON/10개 요청 확인 및 git diff --check. 제품 코드 변경 없어 Gradle 미실행. Postman runner는 실행하지 않음.
- 유지한 계약: 실제 토큰/비밀값 비기록. 회원 생성/정상 세션 회전/데이터 삭제/설정 변경 없음. 기존 변경 보존, 예상 밖 변경 없음.
- 결정사항: 계정 없는 기본 검사와 인증 성공 E2E를 구분. 임의 MEMBER/custom token 우회 없음.
- 위험 요소: 실제 회원 가입/로그인/refresh/LC 성공 및 GUEST403 미검증. Identity live OpenAPI의 http 서버 주소 관측, HTTPS 명시 사용 및 후속 수정 검토 필요.
- 다음 작업: Firebase SDK 테스트 계정 Google 인증/phone link 준비 후 승인된 테스트 사용자로 MEMBER E2E 진행. Postman 제어 권한 또는 사용자 import 실행 필요. Jira 변경 없음.

## 2026-09-28 — Firebase·Identity 테스트 회원 생성 절차 안내

<!-- codex-turn:01a0e625-5503-7271-a27a-251d2cc3a321 -->

- 브랜치: develop.
- 작업 목표: 앱 연동 전 테스트 계정을 만드는 올바른 순서 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 조사/안내: 기존 프론트 가입 계약 확인. Google SDK 인증, exchange enrollment, 같은 UID phone link, 강제 갱신 후 signup 순서 안내. Firebase 콘솔 테스트 번호 등록과 회원 생성 구분. 최소 SDK 테스트 화면이 별도로 필요함을 명시.
- 테스트와 결과: 계약 문서 읽기 및 git diff --check. 안내/기록만 변경하여 Gradle 미실행.
- 유지한 계약: 계정/데이터/서버 설정 생성·변경 없음. 실제 전화번호/OTP/credential/토큰 비기록.
- 결정사항: 테스트 번호는 실제 SMS 없이 고정 검증 코드로 사용하되 SDK 연결과 가입 필수 정보/정책 동의는 생략하지 않음.
- 위험 요소: 최소 테스트 화면은 아직 미구현, 웹 사용 시 앱 등록/허용 도메인/reCAPTCHA 준비 필요. 실제 MEMBER E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 요청 시 테스트 전용 Firebase SDK 화면 준비 후 인증/가입 성공 경로 검증. Jira 변경 없음.

## 2026-09-28 — 테스트 계정 생성 안내 작업 식별 기록 보완

<!-- codex-turn:01a0e625-c8ec-7c70-a91b-565707c268de -->

- 브랜치: develop.
- 작업 목표: 테스트 회원 생성 절차 안내의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Firebase 테스트 전화번호 등록, Google SDK 로그인, exchange enrollment, 동일 UID phone link, 갱신된 ID Token으로 Identity signup 절차 안내 기록. 앞선 식별자는 현재 hook 제공 식별자로 이 추가 기록에서 보완하며 과거 기록은 유지.
- 테스트와 결과: 문서만 변경하여 Gradle 미실행, git diff --check 수행.
- 유지한 계약: 계정/서버/외부 설정 변경 없음, 비밀값 미기록, 기존 변경 보존.
- 결정사항: SDK 테스트 화면과 실제 회원 생성은 아직 수행하지 않음.
- 위험 요소: MEMBER 인증 E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 요청 시 최소 Firebase SDK 테스트 화면 준비 및 가입 검증.

## 2026-09-28 — Firebase 테스트 전화번호 등록 확인

<!-- codex-turn:01a0e630-f709-7f23-b4ff-a6fe4bca4d3c -->

- 브랜치: develop.
- 작업 목표: 사용자가 열어 둔 Firebase 탭에서 테스트 전화번호 등록 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: to-teacher-firebase 로그인 방법에서 Google/전화 활성화와 테스트 전화번호·인증 코드 저장 항목 1쌍 확인. 콘솔 설정 저장/변경 및 사용자 생성 없음.
- 테스트와 결과: 콘솔 UI 읽기 확인 완료. 문서 기록만 변경하여 Gradle 미실행. git diff --check 수행.
- 유지한 계약: 번호/인증 코드/토큰/credential을 저장소에 기록하지 않음. 기존 변경 보존.
- 결정사항: 테스트 번호 등록 단계 완료, SDK Google 인증과 같은 UID 전화 연결 및 Identity 가입은 별도 진행.
- 위험 요소: 실제 MEMBER E2E 미검증. 열린 콘솔 Spark 표시는 관측 사실이며 실제 결제 연결 상태 재검증은 이번 범위 밖. 예상 밖 코드 변경 없음.
- 다음 작업: 최소 SDK 테스트 화면 또는 앱으로 Google 로그인→exchange→phone link→signup 검증. Jira 변경 없음.

## 2026-09-28 — 로컬 Firebase 웹 테스트 화면 필요성 안내

<!-- codex-turn:01a0e638-bf2e-7b93-b16e-6b9d4b7d43ce -->

- 브랜치: develop.
- 작업 목표: 사용자 직접 구현 필요 여부와 간단한 웹 테스트 방법 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Codex가 최소 로컬 웹 화면을 구현할 수 있으며 공개 배포는 불필요함을 안내. Google 인증→exchange→동일 UID phone link→signup 순서 유지. 웹 앱 등록/허용 도메인/전화 인증 웹 설정/CORS는 확인 필요.
- 테스트와 결과: 안내와 기록만 수행, Gradle 미실행. git diff --check 수행.
- 유지한 계약: 비밀값 비기록, 서비스 계정 개인키를 웹에 넣지 않음, 외부 설정/계정 생성 및 코드 구현 없음.
- 결정사항: 실제 구현은 후속 요청 시 진행. Android 앱 인증 검증을 웹 테스트로 대체하지 않음.
- 위험 요소: 현재 웹 앱 등록/허용 도메인/CORS 미확인, 실제 인증 성공 미검증. 기존 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 승인 시 최소 테스트 화면 구현과 필요한 Firebase 웹 설정 확인. Jira 변경 없음.

## 2026-09-28 — 로컬 Firebase 인증 테스트 화면 구현

<!-- codex-turn:01a0e638-bf2e-7b93-b16e-6b9d4b7d43ce -->

- 브랜치: develop.
- 작업 목표: 앱 프론트 연동 전에 사용자 직접 Google 인증/전화 연결/가입을 진행할 최소 웹 화면 구현.
- 변경 파일: tools/auth-test/index.html, app.js, session.mjs, server.mjs, app.test.mjs, server.test.mjs, README.md 및 docs/codex/WORKLOG.md, CURRENT_STATE.md.
- 구현 내용: Firebase in-memory 인증, 같은 UID phone link, enrollment 만료 검사, 직접 동의 signup, pair 동시 교체, single-flight 재발급과 UUID 요청 ID, 실패 시 이전 refresh 재사용 금지. 프로필/LC today는 읽기 smoke만 제공. 고정 테스트 HTTPS 5개 API만 허용하는 loopback proxy로 운영 CORS 변경 없이 사용. Host/Origin/전용 헤더/본문 크기 제한, redirect 차단, 로그 민감정보 비출력.
- 테스트와 결과: 최초 sandbox의 포트/Gradle 캐시 접근 제한 후 승인된 재실행에서 Node server 2개 및 mocked UI 1개 성공, node --check 성공, ./gradlew clean test BUILD SUCCESSFUL. git diff --check 수행. localhost 서버 실행 완료. Chrome 자동 열기는 ERR_BLOCKED_BY_CLIENT로 차단되어 실제 화면 렌더링 검증 실패, 보호 우회 없음.
- 유지한 계약: Identity 서버 API/보안 설정/운영 배포 미변경. 실제 토큰/키/전화번호/코드를 파일에 저장하지 않음. 계정 생성·실제 로그인·약관 동의는 수행하지 않음.
- 결정사항: 이 도구는 테스트 가상 번호 전용이며 공개 배포하지 않는다. Stage 9 응답 복구/Android SDK 전체 검증은 별도. 서버 세션 폐기 없이 로컬 정보만 지우는 버튼임을 명시.
- 위험 요소: Firebase 웹 앱 구성/localhost 승인 도메인/정책 버전/실제 OAuth 및 MEMBER E2E 미확인. 브라우저 개발자 도구에는 credential이 존재하므로 공유 금지. 기존 dirty 문서/배포 테스트 변경 보존, 이번 예상 밖 변경 없음.
- 다음 작업: 사용자가 로컬 페이지 접근 확인 후 웹 앱 구성과 승인 도메인 준비, 직접 Google 로그인·약관 동의 후 실제 가입 검증. 별도 서비스 배포 불필요. Jira 변경 없음.

## 2026-09-28 — 로컬 인증 테스트 화면 작업 식별 기록 보완

<!-- codex-turn:01a0e639-5ecc-7283-8bf4-92e25670f712 -->

- 브랜치: develop.
- 작업 목표: 이번 로컬 인증 테스트 도구 구현 결과를 현재 작업 식별자로 기록.
- 변경 파일: tools/auth-test의 화면·SDK 흐름·로컬 서버·테스트·README 및 docs/codex/WORKLOG.md, CURRENT_STATE.md.
- 구현 내용: Google 인증→exchange→같은 UID 가상 전화번호 연결→signup, 프로필/재발급/LC 읽기 smoke. 테스트 HTTPS 목적지 제한, loopback 바인딩, Host/Origin 가드, 토큰 메모리 보관 및 비출력.
- 테스트와 결과: Node 3개 테스트, 문법 검사, ./gradlew clean test 및 git diff --check 통과. 로컬 서버 기동 완료. Chrome 자동 열기는 ERR_BLOCKED_BY_CLIENT로 차단되어 실제 렌더링/인증 E2E 미검증.
- 유지한 계약: 운영/API/Firebase 외부 설정 변경 없음. 비밀값 비기록. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- 결정사항: 별도 공개 배포 없이 로컬 전용 사용. 실제 로그인·약관 동의는 사용자가 수행. 이전 기록은 유지하고 작업 식별자만 이 항목으로 보완.
- 위험 요소: 웹 앱 구성/localhost 승인 도메인/정책 버전 확인 및 실제 회원 인증 검증 필요. Stage 9 복구/Android SDK 검증은 별도.
- 다음 작업: 로컬 화면 사용자 접근 확인 후 Firebase 웹 설정 준비. Jira 변경 없음.

## 2026-09-28 — Firebase 웹 구성 위치 및 JSON 입력 안내

<!-- codex-turn:01a0e645-f435-75a1-b0ad-dcd4d36ecd6b -->

- 브랜치: develop.
- 작업 목표: 사용자가 찾지 못한 Firebase 웹 구성 메뉴와 JSON 형식 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 프로젝트 일반 설정→내 앱→웹 앱→SDK 설정 및 구성 경로와 웹 앱 부재 시 등록 방법 안내. Hosting 불필요, 서비스 계정 JSON과 구분. JavaScript 선언을 제외하고 키를 큰따옴표로 감싼 JSON만 입력하도록 안내.
- 테스트와 결과: 문서 안내만 수행하여 Gradle 미실행, git diff --check 확인.
- 유지한 계약: 실제 키/인증정보 미기록, Firebase 설정/계정 생성 없음. 기존 변경 보존.
- 결정사항: 기존 Firebase 프로젝트에서 웹 앱을 사용하며 새 프로젝트 불필요.
- 위험 요소: 웹 앱 현재 등록 여부는 직접 조회하지 않아 미확인. 실제 인증 E2E 미완료. 예상 밖 변경 없음.
- 다음 작업: 사용자 웹 앱 구성 확인 후 로컬 화면 설정 및 승인 도메인 확인. Jira 변경 없음.

## 2026-09-28 — 사용자 제공 Firebase 웹 구성 형식 확인

- 브랜치: develop.
- 작업 목표: 제공된 코드가 로컬 인증 화면에 필요한 웹 구성인지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 웹 appId와 구성 필드를 기준으로 웹 SDK 설정임을 확인. 실제 값 재출력 없이 네 필드의 JSON 입력 방식과 Analytics 불필요 안내.
- 테스트와 결과: 제공된 구조와 로컬 화면 입력 계약 확인, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 실제 키/토큰 비기록, Firebase 설정/인증/배포 변경 없음.
- 결정사항: 기존 로컬 화면에 공개 웹 설정만 입력하며 서비스 계정 사용 금지 유지.
- 위험 요소: 실제 로그인/승인 도메인/E2E 미확인. 기존 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 사용자 JSON 입력 후 Google 로그인 진행 및 결과 확인. Jira 변경 없음.

## 2026-09-28 — Firebase 웹 구성 확인 작업 식별 기록 보완

<!-- codex-turn:01a0e648-0866-7b60-9158-82ec6a24ecb4 -->

- 브랜치: develop.
- 작업 목표: 웹 구성 형식 확인 결과의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 제공 구성이 웹 SDK용임을 확인하고 로컬 화면에 필요한 네 필드 JSON 입력 및 Google 로그인 순서 안내. 실제 값은 기록하지 않음.
- 테스트와 결과: 구조 확인 및 git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 인증/외부 설정/배포 변경 없음, 비밀정보 비기록, 과거 기록과 기존 변경 보존.
- 결정사항: Analytics 초기화와 서비스 계정 JSON은 불필요.
- 위험 요소: 실제 Google 로그인 및 E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 설정 적용 후 로그인 결과 확인. Jira 변경 없음.

## 2026-09-28 — 로컬 Firebase 설정 입력 오류 진단

- 브랜치: develop.
- 작업 목표: 설정 적용 실패 원인 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 지정 localhost 탭에서 Google 버튼 비활성과 일반 오류 확인. 입력 객체의 속성명 큰따옴표 누락/마지막 쉼표가 JSON 구문과 맞지 않음을 확인하여 수정 방법 안내. 실제 값 기록 없음.
- 테스트와 결과: UI 입력 구조와 기존 JSON.parse 구현 대조. 코드 변경 없어 Gradle 미실행, git diff --check 수행.
- 유지한 계약: 외부 인증/설정 변경 및 사용자 입력 수정 없음. 기존 변경 보존.
- 결정사항: 현재 오류는 로그인 이전 JSON 입력 파싱 문제로 판단. 인증 성공 여부와 구분.
- 위험 요소: 일반 오류 문구가 구문 오류를 명확히 설명하지 못함. 실제 로그인 미검증, 예상 밖 변경 없음.
- 다음 작업: JSON 문법 수정 후 설정 적용 및 Google 로그인 확인. Jira 변경 없음.

## 2026-09-28 — 설정 입력 오류 진단 작업 식별 기록 보완

<!-- codex-turn:01a0e64a-15d6-7291-804b-372af08e97a4 -->

- 브랜치: develop.
- 작업 목표: 로컬 Firebase 설정 실패 진단의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: UI에서 JSON 속성명 따옴표 누락과 마지막 쉼표를 확인하여 수정 안내. 실제 설정값은 기록하지 않음.
- 테스트와 결과: 화면과 JSON 입력 계약 대조, git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 입력/인증/외부 설정 변경 없음, 기존 기록과 변경 보존, 비밀값 비기록.
- 결정사항: 로그인 이전 입력 파싱 문제로 진단.
- 위험 요소: 실제 로그인 미검증, 일반 오류 메시지 개선 여지 있음. 예상 밖 변경 없음.
- 다음 작업: 사용자 JSON 수정 후 설정 적용 확인. Jira 변경 없음.

## 2026-09-28 — Google 로그인 완료 여부 확인

- 브랜치: develop.
- 작업 목표: 사용자 로그인 시도 후 완료 상태 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 로컬 화면 결과 auth/popup-closed-by-user와 exchange 등 후속 버튼 비활성 확인. Google 인증 결과가 로컬 앱에 전달되어 완료된 상태가 아님을 안내.
- 테스트와 결과: UI 읽기 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 인증 재시도/계정 생성/외부 설정 변경 없음, 비밀값 비기록, 기존 변경 보존.
- 결정사항: 팝업 완료까지 유지하며 재시도하고 계속 실패하면 일반 Chrome에서 확인 권장.
- 위험 요소: 팝업 종료 원인은 오류 코드만으로 확정 불가. 실제 MEMBER E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: Google 인증 완료 문구와 exchange 버튼 활성화 확인. Jira 변경 없음.

## 2026-09-28 — 로그인 상태 확인 작업 식별 기록 보완

<!-- codex-turn:01a0e64c-9f49-7db2-a549-9c004ac7b6a8 -->

- 브랜치: develop.
- 작업 목표: 로그인 완료 여부 확인의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: auth/popup-closed-by-user 및 후속 버튼 비활성을 확인하여 인증 미완료 안내. 팝업 인증 완료 후 재확인하도록 설명.
- 테스트와 결과: UI 읽기 확인, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, 외부 인증/설정 변경 없음, 과거 기록 및 기존 변경 보존.
- 결정사항: 현재 화면은 Google 인증 완료 결과를 받지 못한 상태로 판단.
- 위험 요소: 팝업 종료의 구체적 원인과 실제 인증 E2E 미확인. 예상 밖 변경 없음.
- 다음 작업: 사용자 재시도 후 Google 인증 완료와 exchange 활성 여부 확인. Jira 변경 없음.

## 2026-09-28 — 반복 Google 팝업 인증 실패 조사

- 브랜치: develop.
- 작업 목표: 반복 로그인 실패의 단계와 설정 오류 가능성 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: IAB 로컬 화면 popup-closed-by-user 및 exchange 비활성 확인. 수집된 warn/error 로그 없음, 남은 OAuth 팝업 없음. Firebase 승인 도메인 UI에서 localhost와 기본 authDomain 등록 확인. 로컬 코드의 popup SDK 호출/완료 분기 확인.
- 테스트와 결과: UI/코드 읽기 진단 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 인증 재시도/설정 수정/계정 생성/배포 없음. 비밀값 비기록 및 기존 변경 보존.
- 결정사항: 승인 도메인 누락은 배제. SDK 팝업 완료 이전 단계 실패로 판단하며 내장 브라우저 연동 문제는 미확정 가설. 일반 Chrome 비교 요청.
- 위험 요소: 오류 코드만으로 실제 팝업 종료 원인은 확정 불가. 수집 로그가 없다는 것이 브라우저 오류 전체 부재를 보증하지 않음. 실제 인증 E2E 미검증, 예상 밖 변경 없음.
- 다음 작업: 동일 설정으로 일반 Chrome 로그인 비교, 실패 시 팝업 화면과 오류 코드 확인. Jira 변경 없음.

## 2026-09-28 — 반복 팝업 실패 조사 작업 식별 기록 보완

<!-- codex-turn:01a0e64d-f3dd-7173-9711-fb6df33043bb -->

- 브랜치: develop.
- 작업 목표: 반복 Google 팝업 실패 조사 결과의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 로컬 화면 popup-closed-by-user 및 exchange 비활성, 수집된 경고/오류 로그 부재, Firebase localhost 승인 도메인 등록 확인. 일반 Chrome 비교를 안내.
- 테스트와 결과: UI·코드 읽기 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀값 비기록, 설정/인증/배포 변경 없음, 과거 기록과 기존 변경 보존.
- 결정사항: 도메인 누락은 배제하고 내장 브라우저 팝업 처리 문제는 미확정 가설로 유지.
- 위험 요소: 실제 팝업 종료 원인과 인증 E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: 일반 Chrome에서 같은 설정으로 로그인 비교 후 원인 추가 확인. Jira 변경 없음.

## 2026-09-28 — Chrome 로그인 성공 비교 결과 설명

- 브랜치: develop.
- 작업 목표: 내장 브라우저 실패와 일반 Chrome 성공의 차이 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 보고로 Chrome 로그인 성공 확인. Firebase popup 인증은 인증 후 원래 페이지로 결과 전달이 필요하며 브라우저별 팝업/창 간 통신/저장소 정책 차이가 영향을 줄 수 있음을 설명. 정확한 하위 원인은 미확정으로 구분.
- 테스트와 결과: 기존 UI 진단 및 사용자 비교 결과 기반 분석, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀값 비기록, 코드/외부 설정/계정 직접 변경 없음. 기존 변경 보존.
- 결정사항: 로컬 웹 인증 테스트는 일반 Chrome에서 계속 진행. Android SDK 실제 검증은 별도.
- 위험 요소: Identity 가입/로그인 교환 및 MEMBER E2E 성공은 아직 확인하지 않음. 예상 밖 변경 없음.
- 다음 작업: Chrome에서 Identity 로그인/가입 준비 결과 확인. Jira 변경 없음.

## 2026-09-28 — Chrome 로그인 비교 작업 식별 기록 보완

<!-- codex-turn:01a0e64f-ec02-77b2-a1aa-ebe93c7345e5 -->

- 브랜치: develop.
- 작업 목표: Chrome 로그인 성공 비교 설명의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 보고에 따른 Chrome 로그인 성공과 내장 브라우저 팝업 실패 차이 설명. 정확한 내부 원인은 미확정으로 유지.
- 테스트와 결과: 사용자 비교 결과 및 기존 UI 진단 기반 분석, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, 외부 설정/인증/배포 변경 없음, 과거 기록과 기존 변경 보존.
- 결정사항: 로컬 웹 테스트는 Chrome에서 진행하며 Google 로그인과 Identity 가입 완료는 구분.
- 위험 요소: Identity exchange/가입 및 MEMBER E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: Chrome에서 Identity 로그인/가입 준비 결과 확인. Jira 변경 없음.

## 2026-09-28 — 테스트 전화번호 국제 형식 안내

<!-- codex-turn:01a0e651-6dce-7960-83cf-2ef04432c83a -->

- 브랜치: develop.
- 작업 목표: 국내 형식 전화번호 입력 거절 이유 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: app.js 국제 번호 정규식 확인. 한국 번호 맨 앞 0 제거 후 국가번호 +82 사용, Firebase 등록 가상 번호 일치 및 고정 코드 사용 안내.
- 테스트와 결과: 입력 검사 코드 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 실제 번호/인증 코드 비기록, 인증/외부 설정 변경 없음, 기존 변경 보존.
- 결정사항: 형식 수정만으로 임의 번호가 테스트 번호로 인정되지는 않음을 구분.
- 위험 요소: 현재 브라우저 오류 자체는 재조회하지 않았으며 실제 phone link 미검증. 예상 밖 변경 없음.
- 다음 작업: 등록된 가상 번호를 국제 형식으로 입력 후 테스트 연결 확인. Jira 변경 없음.

## 2026-09-28 — 국제 번호 안내 작업 식별 기록 보완

<!-- codex-turn:01a0e651-1f41-7a11-95fd-9e79532fbb82 -->

- 브랜치: develop.
- 작업 목표: 국제 번호 입력 안내의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 로컬 입력 정규식 확인 후 국제 형식 변환 및 Firebase 등록 가상 번호 일치 조건 안내. 실제 번호/코드는 기록하지 않음.
- 테스트와 결과: 코드 읽기 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 설정/인증 변경 없음, 기존 기록과 변경 보존.
- 결정사항: 앞선 식별자를 이번 hook 제공 식별자로 보완.
- 위험 요소: 실제 전화 연결 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 국제 형식 입력 후 연결 결과 확인. Jira 변경 없음.

## 2026-09-28 — 테스트 가입 정책 버전 입력 안내

- 브랜치: develop.
- 작업 목표: 테스트 화면 정책 버전과 AWS 설정의 관계 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 테스트 task draft에서 개인정보 privacy-v1, 이용약관 term-v1 확인. 실제 실행 task 환경변수의 동일 값 사용과 약관 확인 후 직접 동의 안내.
- 테스트와 결과: 설정 파일 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 실제 비밀값 비기록, 외부 설정/동의/가입 요청 변경 또는 실행 없음. 기존 변경 보존.
- 결정사항: 가입을 통과시키기 위해 서버 정책 버전을 임의 변경하지 않음.
- 위험 요소: 현재 live ECS 환경변수는 이번에 재조회하지 않음. 예상 밖 변경 없음.
- 다음 작업: 실행 중 task 정책 버전 일치 확인 후 사용자가 가입 진행. Jira 변경 없음.

## 2026-09-28 — 정책 버전 안내 작업 식별 기록 보완

<!-- codex-turn:01a0e653-09c5-7f00-b670-34ebc093fcef -->

- 브랜치: develop.
- 작업 목표: 가입 정책 버전 안내의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 테스트 배포 초안의 정책 버전을 확인하고 실제 실행 task 설정과 일치해야 함을 안내. live AWS 재확인과 구분.
- 테스트와 결과: 설정 파일 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀값 비기록, 외부 설정/가입/동의 실행 없음, 과거 기록과 기존 변경 보존.
- 결정사항: 서버 정책 버전 변경 없이 해당 버전의 약관 확인 후 사용자 직접 동의.
- 위험 요소: 현재 live 설정 미확인. 예상 밖 변경 없음.
- 다음 작업: 실제 실행 task 정책 버전 확인 후 가입 검증. Jira 변경 없음.

## 2026-09-28 — Identity 토큰 수신 성공 결과 해석

- 브랜치: develop.
- 작업 목표: 사용자 제공 토큰 수신 요약의 성공 범위 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 보고와 accept 구현을 대조하여 Access/Refresh pair 수신 및 UUID sub/MEMBER/LC audience 형태 확인 성공 설명. 사용한 버튼은 요약만으로 구분하지 않으며 서버 서명 검증과 구분.
- 테스트와 결과: 로컬 화면 코드 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행. 실서버 인증 성공 정보는 사용자 보고 기반.
- 유지한 계약: 토큰 원문 비기록, 외부 인증/설정 변경 없음, 기존 변경 보존.
- 결정사항: 프로필 인증→재발급→LC 접근을 다음 확인 단계로 안내.
- 위험 요소: 프로필/재발급/LC 실제 성공 및 서명 검증 미확인. 예상 밖 변경 없음.
- 다음 작업: 사용자 보호 API 호출과 재발급 결과 확인. Jira 변경 없음.

## 2026-09-28 — 토큰 수신 결과 해석 작업 식별 기록 보완

<!-- codex-turn:01a0e654-ccb1-7910-9cbc-8c94acb93265 -->

- 브랜치: develop.
- 작업 목표: Identity 토큰 수신 성공 해석의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 보고의 UUID sub/MEMBER/LC audience 검사 성공과 Access/Refresh pair 수신 의미 설명. 토큰 내용 확인과 실제 서버 인증 검증 구분.
- 테스트와 결과: 기존 화면 구현 대조 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 토큰 원문 비기록, 외부 설정/인증 변경 없음, 과거 기록과 기존 변경 보존.
- 결정사항: 프로필→재발급→LC 접근 순서로 후속 확인 안내.
- 위험 요소: 보호 API/재발급/LC 성공은 아직 미확인. 예상 밖 변경 없음.
- 다음 작업: 사용자 후속 검증 결과 확인. Jira 변경 없음.

## 2026-09-28 — 프로필·재발급 성공 및 LC 403 조사

<!-- codex-turn:01a0e655-dfb5-7220-b937-990b836c73ef -->

- 브랜치: develop.
- 작업 목표: 사용자 테스트 결과 해석과 LC 403 원인 후보 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 보고상 프로필 인증/재발급 성공 및 claim 형태 유지 확인. LC 로컬 ChallengeController의 MEMBER 검사와 feature OFF 모두 COMMON403 반환 확인. application 기본값 및 테스트 task template CHALLENGE_ENABLED=false 확인. LC 코드/파일은 읽기만 수행.
- 테스트와 결과: 사용자 결과와 로컬 코드/배포 템플릿 대조, git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 토큰 원문 비기록, 외부 설정/기능 활성화/배포 변경 없음. 기존 변경 보존.
- 결정사항: MEMBER 누락으로 단정하지 않고 실행 중 LC task feature flag부터 확인하도록 안내. AI 및 Day1 등 준비 없이 flag를 켜지 않음.
- 위험 요소: live LC 환경변수/배포 revision 미확인, 따라서 원인 후보이며 확정 아님. LC 인증·챌린지 E2E 성공 미완료. 예상 밖 변경 없음.
- 다음 작업: LC 담당자가 실행 task CHALLENGE_ENABLED와 요청 거절 위치 확인 후 활성 준비 검토. Jira 변경 없음.

## 2026-09-28 — Learning Core 회원 통합 workload 인계 검토

<!-- codex-turn:01a0e6b0-420b-7b93-a0b1-6a67ac69e80f -->

- 브랜치: develop.
- 작업 목표: LC 요청의 workload 신뢰 정보·이벤트 전송 준비 상태를 구현 기준으로 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: WorkloadJwtConfiguration/Properties/CredentialProvider에서 기존 encoder와 kid 공유, 별도 issuer, RS256/고정 subject/목적별 audience/정확히 2분 TTL 확인. UserMerged HTTPS adapter와 outbox 재시도/2xx 성공 처리 확인. JWKS 이전 공개키 병행 지원 및 테스트 draft merge/workload/publisher OFF 확인. OwnerEvent 대체 전송 경로 존재 확인.
- 테스트와 결과: 코드·설정 읽기 검토 및 git diff --check. 코드 변경 없어 Gradle 미실행, live AWS/전송 E2E 미실행.
- 유지한 계약: 로그인 JWT와 workload 용도 분리, 토큰/비밀키 비기록, 운영 및 테스트 기능 활성화 없음, 기존 변경 보존.
- 결정사항: 동일 테스트 issuer/JWKS 사용 가능하나 WORKLOAD_JWT_ISSUER를 명시하고 LC와 일치시켜야 함. 키 교체는 공유 사용자 토큰 최대 TTL/skew/cache 및 구 instance 종료를 고려. publisher 경로를 확인한 뒤 하나의 배포 계획으로 활성화하며 기본 챌린지 검증과 merge E2E를 구분.
- 위험 요소: 현재 공개 kid는 draft 및 이전 관측 기준, live 재확인 필요. 기존 사용자 토큰 차단과 기록 이전/중복 이벤트 E2E 미검증. 실제 전송 준비 완료로 간주하지 않음. 예상 밖 변경 없음.
- 다음 작업: merge 검증 범위 승인 후 실행 task 설정/배포 revision·키·대상 endpoint 확인과 양측 이벤트 E2E 계획 확정. Jira 변경 없음.

## 2026-09-28 — 회원 통합 테스트 준비 순서 안내

- 브랜치: develop.
- 작업 목표: workload 및 UserMerged 연동을 위해 다음에 할 작업을 구체화.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: merge transaction의 capture flag 분기 확인. live 설정 조회, 경로와 신뢰 계약 확정, LC 수신 준비, Identity 활성화, 가상 계정 병합 E2E 순서 안내. 챌린지 기능 활성화와 구분.
- 테스트와 결과: 코드 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 실제 AWS 조회/설정 변경/병합 요청 없음, 비밀값 비기록, 기존 변경 보존.
- 결정사항: 승인 없이 flag를 켜지 않으며 수신 측 준비 후 발행 측 활성화. Billing은 기존 제외 범위 유지.
- 위험 요소: live 기능 상태 및 키/전송 E2E 미확인. 실제 계정 병합은 테스트 계정으로만 별도 승인 후 수행. 예상 밖 변경 없음.
- 다음 작업: 실행 중 테스트 서비스의 비밀 아닌 환경설정과 배포 revision 읽기 확인. Jira 변경 없음.

## 2026-09-28 — 회원 통합 준비 안내 작업 식별 기록 보완

<!-- codex-turn:01a0e6b3-45c0-77c3-b0d9-a59b1f442f40 -->

- 브랜치: develop.
- 작업 목표: 회원 통합 테스트 준비 안내의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 실행 설정 조회→LC 수신 준비→Identity 발행 활성화→테스트 계정 병합 검증 순서 안내. 이벤트 저장/발행 경로 일치와 챌린지 403 대응의 독립성 설명.
- 테스트와 결과: 코드 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀값 비기록, AWS 변경/실제 병합 없음, 과거 기록과 기존 변경 보존.
- 결정사항: 다음 단계는 설정 변경 없는 live 조회이며 활성화와 테스트 병합은 별도 진행.
- 위험 요소: live 배포/기능 상태 및 이벤트 E2E 미확인. 예상 밖 변경 없음.
- 다음 작업: 테스트 서비스 설정·배포 revision 조회. Jira 변경 없음.

## 2026-09-28 — AWS 테스트 Identity 회원 통합 설정 실조회

- 브랜치: develop.
- 작업 목표: 실제 배포 버전과 workload·merge 이벤트 준비 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: AWS ECS 서비스 새로고침 후 test:4 desired1/running1/pending0 확인. 이미지 및 release의 commit 88ff5bedc1ee661ccd38a8a1d2c4dbf3b9f03f2d 확인. task 정의에서 GUEST_MERGE_ENABLED, WORKLOAD_JWT_ENABLED, USER_MERGED_PUBLISHER_ENABLED, OWNER_EVENT_USER_MERGED_CAPTURE_ENABLED, OWNER_EVENT_LEARNING_CORE_USER_MERGED_PUBLISHER_ENABLED, Billing merge publisher 모두 false 확인. workload issuer 및 이벤트 endpoint 항목 없음, 환경 파일 없음. 사용자 issuer/kid/Access TTL PT30M 확인.
- 테스트와 결과: 15:29~15:30 KST 콘솔 읽기 검증 및 git diff --check. 코드 변경 없어 Gradle 미실행, 이벤트 발급/전송 E2E 미수행.
- 유지한 계약: Secret 값 열람/운영 변경/테스트 설정 변경/계정 병합 없음. 비밀정보 비기록 및 기존 변경 보존.
- 결정사항: 현 배포는 로그인 가능 상태와 별개로 회원 통합 및 workload 이벤트 OFF. LC 수신 준비 후 선택한 발행 경로로 활성화 계획 필요.
- 위험 요소: 실제 JWKS 응답 재조회 및 LC 수신 설정/이벤트 E2E는 이번에 검증하지 않음. task 정의 설정 조회이며 컨테이너 내부 런타임 계측은 아님. 예상 밖 변경 없음.
- 다음 작업: LC에 실제 OFF 상태 인계, workload issuer·endpoint와 사용할 이벤트 경로 확정 후 별도 승인으로 테스트 설정 적용. Jira 변경 없음.

## 2026-09-28 — AWS 설정 조회 작업 식별 기록 보완

<!-- codex-turn:01a0e6b4-6101-7c83-b723-450df80551f6 -->

- 브랜치: develop.
- 작업 목표: AWS 테스트 Identity 실조회 결과의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 실행 중 test:4 및 이미지 commit 확인. 회원 통합/workload/두 이벤트 발행 경로 OFF와 workload issuer·endpoint 미설정 확인. 사용자 JWT 공개 메타데이터와 TTL 확인.
- 테스트와 결과: AWS 콘솔 읽기 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀값 조회·기록 없음, AWS 설정 변경/이벤트 발송/병합 없음, 과거 기록과 기존 변경 보존.
- 결정사항: LC 수신 준비 후 선택한 이벤트 경로의 활성화를 별도 승인으로 진행.
- 위험 요소: LC 수신 설정 및 이벤트 E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: LC 인계 및 테스트 이벤트 설정 확정. Jira 변경 없음.

## 2026-09-28 — 회원 통합 이벤트 두 경로 및 전환 의미 설명

<!-- codex-turn:01a0e6c6-bfe9-71e0-b261-cef72303b933 -->

- 브랜치: develop.
- 작업 목표: 발행 경로 선택 의미와 기존 안내의 과도한 단순화 정정.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: merge transaction capture 분기, OwnerEventCaptureService와 requiredConsumers, fanout runbook/Stage7 legacy 전환 계약 확인. 기존 LC 전용 outbox와 신규 Billing/LC fan-out 차이 및 기존 잔량 배출을 위한 publisher 공존 설명.
- 테스트와 결과: 코드/계약 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 신규 merge는 하나의 저장 경로 선택, 과거 outbox 삭제/자동 backfill 없음, 외부 설정 변경 및 비밀값 기록 없음.
- 결정사항: 두 publisher 동시 활성 자체를 금지하는 것이 아니며 capture와 발행 경로 일치·잔량 처리가 핵심이라고 정정. 신규 경로 Billing OFF는 delivery 미생성이 아니라 미전송 pending 보존임을 안내.
- 위험 요소: 테스트 DB 기존 outbox 잔량 미확인, 새 경로 Billing backlog 처리 결정 필요. 기존 변경 보존, 예상 밖 변경 없음.
- 다음 작업: LC-only 테스트 또는 신규 fan-out 검증 범위에 맞춰 저장/발행 설정과 backlog 정책 결정. Jira 변경 없음.

## 2026-09-28 — LC 전용 기존 경로 우선 사용 방향 확인

- 브랜치: develop.
- 작업 목표: 기존 이벤트 경로 우선 검증 후 Billing 연동 시 공통 경로 전환 가능 여부 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 기존 LC 전용 방식으로 이번 테스트 진행 방향 확인. 신규 capture 전환 이후 발생하는 병합부터 공통 이벤트를 생성하며 기존 잔량은 기존 publisher로 처리, 과거 Billing backfill은 자동 수행하지 않음을 안내.
- 테스트와 결과: 앞선 코드/전환 계약 검토 기반 안내 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 설정/배포 변경 없음, 실제 키/토큰 비기록, 과거 이벤트 삭제·변환 없음. 기존 변경 보존.
- 결정사항: 이번 범위는 기존 LC 전용 경로, 공통 경로는 Billing 소비자 준비와 함께 별도 전환 검증.
- 위험 요소: 과거 통합 계정이 Billing 소유권에 미치는 영향은 후속 검토 필요. 운영 확대나 실제 활성화 승인을 의미하지 않음. 예상 밖 변경 없음.
- 다음 작업: LC 수신 준비 후 기존 UserMerged publisher와 workload 테스트 설정 적용 범위 제시. Jira 변경 없음.

## 2026-09-28 — 기존 경로 우선 사용 작업 식별 기록 보완

<!-- codex-turn:01a0e6c8-5816-7a70-839c-6993a4deb8bb -->

- 브랜치: develop.
- 작업 목표: 기존 LC 전용 경로 우선 사용 방향의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 이번 테스트는 기존 UserMerged 경로, Billing 연동 시 OwnerEvent 공통 경로로 전환하는 방향 확인. 기존 미전송 이벤트 처리와 과거 병합의 Billing 이전 별도 검토 안내.
- 테스트와 결과: 기존 코드·계약 검토 기반 안내 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, AWS 설정/배포/이벤트 변경 없음, 과거 기록과 기존 변경 보존.
- 결정사항: 경로 선택만 확인했으며 실제 활성화는 미수행.
- 위험 요소: 전환 전 병합의 Billing 소유권 영향 검토 필요. 예상 밖 변경 없음.
- 다음 작업: LC 수신 준비 후 기존 경로 활성화 설정 범위 제시. Jira 변경 없음.

## 2026-09-28 — 프론트용 테스트 서버·Swagger 주소 전달

<!-- codex-turn:01a0e738-e560-7cc0-8c08-1bc54397c211 -->

- 브랜치: develop.
- 작업 목표: 프론트가 사용할 테스트 Identity 서버 및 Swagger 주소 제공.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 기존 검증된 테스트 HTTPS base URL, Swagger UI 주소 안내. 실제 API HTTPS 사용 및 Swagger HTTP 서버 주소 관측 이력 주의 안내.
- 테스트와 결과: 이전 검증 기록 기반 안내, git diff --check. 코드 변경 없어 Gradle 미실행, live 재조회 없음.
- 유지한 계약: 공개 주소만 공유, 비밀값 비기록, 서버 설정/배포 변경 없음.
- 결정사항: 프론트 인계 주소는 운영 아닌 테스트 환경임을 명시.
- 위험 요소: Swagger Try it out의 HTTP 주소 문제 해결 여부는 미확인. 기존 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 프론트 테스트 환경 주소 적용 후 인증 연동 확인. Jira 변경 없음.

## 2026-09-28 — LC 배포 연계 Identity 준비사항 검토

<!-- codex-turn:01a0e73f-9870-7be3-bbe6-d5c63e1c039d -->

- 브랜치: develop.
- 작업 목표: LC guard 점검/consumer 배포 전후 Identity의 필요 작업 정리.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 기존 publisher 설정과 workload 의존성/TTL 확인. issuer 및 HTTPS endpoint 사전 합의, outbox 상태 점검, LC 정상 기동·인증 준비 확인 후 publisher 활성화, 신규 merge용 flag 별도 필요 안내. 기존 경로 선택에 따라 OwnerEvent/Billing 설정 OFF 유지.
- 테스트와 결과: 코드·설정 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행, AWS/DB live 조회 없음.
- 유지한 계약: 운영/테스트 기능 변경 및 실제 merge/이벤트 발송 없음, 비밀값 비기록, 기존 변경 보존.
- 결정사항: LC 소비자 준비 전에 발행하지 않음. 401/403 등 영구 실패로 dead-letter 전환될 수 있어 무조건 자동 회복을 기대하지 않도록 안내. DB 최종 점검 이후 테스트 트래픽 조율 필요.
- 위험 요소: live LC guard/index 검증과 Identity 기존 outbox 상태 미확인. Guest merge 승인과 테스트 계정 준비 필요. 예상 밖 변경 없음.
- 다음 작업: LC 배포 완료 증빙 수신 및 Identity outbox/설정 확인 후 승인된 테스트 배포 진행. Jira 변경 없음.

## 2026-09-30 — LC 수신 준비 완료 후 추가 체크리스트 안내

<!-- codex-turn:01a0f0a2-847b-75b0-9a48-0487cbaac804 -->

- 브랜치: develop.
- 작업 목표: 기존 UserMerged 발행 연결 외 필요한 활성화·검증 사항 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 전달 LC 준비 완료와 직접 검증을 구분. application 설정 및 기존 publisher 실패 처리 재확인. 신뢰 설정·현 배포·outbox 상태 확인, 신규 merge flag, 테스트 계정 E2E, 실패 시 publisher 중단과 동일 이벤트 복구 필요 안내.
- 테스트와 결과: 코드 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행, AWS/DB live 확인 없음.
- 유지한 계약: 기존 LC 전용 경로 유지, OwnerEvent/Billing 확대 없음, 비밀정보 비기록, 외부 설정 변경/실제 병합 없음.
- 결정사항: 추가 기능 개발 필요성이 확인된 것은 아니며 테스트 설정 반영과 실연동 검증이 남은 단계. 기존 관측 배포 상태를 현재 상태로 단정하지 않음.
- 위험 요소: 과거 pending/dead-letter 및 live issuer/JWKS/revision 미확인, source 차단·target 기록 조회·중복 처리 E2E 미검증. 기존 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 승인 시 최신 테스트 설정과 outbox 조회 후 설정 반영 및 테스트 계정 병합 검증. Jira 변경 없음.

## 2026-09-30 — 테스트 이벤트 연결 착수 및 AWS 재로그인 대기

<!-- codex-turn:01a0f0a2-847b-75b0-9a48-0487cbaac804 -->

- 브랜치: develop.
- 작업 목표: 승인된 테스트 workload/기존 UserMerged 설정 적용 전 최신 상태 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: AWS task 정의 탭에서 로그아웃 상태 확인. 다시 로그인 버튼의 세션 오류 후 기존 ISB 포털을 열어 로그인 화면 인계. 캐시된 task 정의 정보는 live 증빙으로 사용하지 않음.
- 테스트와 결과: 브라우저 로그인 필요 상태 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: Secret 값 조회/설정 저장/배포/이벤트 전송/계정 병합 없음. 기존 변경 보존, 비밀값 비기록.
- 결정사항: 재로그인 후 최신 배포와 outbox/LC 수신 상태를 확인한 뒤 설정 적용 진행.
- 위험 요소: 최신 AWS 설정 및 대기 이벤트 미확인으로 배포 미완료. 예상 밖 변경 없음.
- 다음 작업: 사용자 AWS 로그인 완료 후 사전 조회 및 테스트 전용 설정 적용 재개. Jira 변경 없음.

## 2026-09-30 — AWS 재로그인 대기 작업 식별 기록 보완

<!-- codex-turn:01a0f0a4-8549-7bb2-93f0-56ad0654afb4 -->

- 브랜치: develop.
- 작업 목표: 테스트 이벤트 연결 착수 결과를 현재 작업 식별자로 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: AWS 세션 만료를 확인하고 기존 ISB 로그인 화면을 열어 사용자에게 인계. 이전 캐시 화면은 최신 배포 증빙으로 사용하지 않음.
- 테스트와 결과: 로그인 필요 상태 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀값 비기록, 설정 변경/배포/이벤트 전송/병합 없음, 과거 기록과 기존 변경 보존.
- 결정사항: 사용자 재로그인 후 최신 배포와 대기 이벤트 조회부터 재개.
- 위험 요소: 최신 설정·outbox 미확인으로 실제 배포 미완료. 예상 밖 변경 없음.
- 다음 작업: AWS 로그인 완료 후 테스트 전용 설정 적용 전 사전 확인. Jira 변경 없음.

## 2026-09-30 — AWS 재로그인 후 양측 설정 확인 및 Atlas 로그인 대기

- 브랜치: develop.
- 작업 목표: 테스트 UserMerged 발행 활성화 사전 확인 재개.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 최신 Identity test:4 실행1/보류0·배포 성공 및 관련 플래그 OFF 확인. LC test:11 실행1/보류0·배포 성공, writer/source-deny/consumer true 및 workload issuer·JWKS 테스트 Identity 일치 확인. 테스트 Atlas 접속은 로그아웃되어 로그인 탭 열고 사용자에게 요청.
- 테스트와 결과: AWS 서비스/태스크 정의 UI 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행. LC 실제 인덱스 검사 로그·인증 이벤트 E2E 및 Identity outbox 조회는 미완료.
- 유지한 계약: 설정 변경/새 revision 생성/배포/이벤트 전송/병합 없음. 비밀정보 비기록, 기존 변경 보존.
- 결정사항: 발행 활성화 시 기존 대기 이벤트가 전송될 수 있어 outbox 확인 전 OFF 유지.
- 위험 요소: Atlas 재로그인이 필요하며 대기·실패 이벤트 범위 미확인. 예상 밖 변경 없음.
- 다음 작업: 사용자 Atlas 로그인 후 테스트 Identity outbox 상태 점검, 안전 조건 충족 시 설정 적용 진행. Jira 변경 없음.

## 2026-09-30 — Atlas 테스트 병합 outbox 확인 완료

<!-- codex-turn:01a0f0a6-526b-7781-9bc6-9422b4ccb65e -->

- 브랜치: develop.
- 작업 목표: 테스트 UserMerged 발행 활성화 전 기존 대기 이벤트 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 재로그인 후 테스트 Atlas Cluster0의 to-teacher-identity-test.user_merged_outbox 조회. Documents 0, 전체 조회 0건 및 빈 컬렉션 안내 확인. 사용자 데이터 수정/삭제 없음.
- 테스트와 결과: Atlas UI 실조회 및 git diff --check. 애플리케이션 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 기존 LC 전용 발행 경로, OwnerEvent/Billing OFF, 운영 설정 미변경, 비밀값 비기록. 기존 사용자 변경 보존.
- 결정사항: 기존 전송 대상 없음 확인. workload 발급 활성화는 서비스 간 인증 기능 활성화이므로 브라우저 정책에 따라 적용 직전 사용자 확인 후 진행.
- 위험 요소: 설정 저장/새 revision/배포/이벤트 전송/병합은 아직 미수행. LC 인덱스 및 이벤트 E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: 확인 후 테스트 workload/Guest merge/기존 publisher 활성화 및 배포 안정성 점검. 실제 테스트 계정 병합과 source 차단·target 기록 이전 검증 별도. Jira 변경 없음.

## 2026-09-30 — 테스트 Identity UserMerged 발행 설정 배포 완료

- 브랜치: develop.
- 작업 목표: 승인된 기존 LC 전용 UserMerged 발행 및 workload 인증 기능 테스트 활성화.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 애플리케이션 코드 변경 없음.
- 구현 내용: 적용 직전 확인에 대한 사용자 응답 `dj`를 한국어 키보드의 `어`로 이해한다고 안내 후 진행. 기존 test:4 JSON을 UI에서 보존하여 환경 설정 7개만 수정하고 입력 JSON 동등성 검증 후 test:5 생성. GUEST_MERGE_ENABLED, WORKLOAD_JWT_ENABLED, USER_MERGED_PUBLISHER_ENABLED=true; WORKLOAD_JWT_ISSUER=https://identity-test.to-teacher.com; WORKLOAD_JWT_SUBJECT=identity-service; WORKLOAD_JWT_TTL=PT2M; USER_MERGED_PUBLISHER_ENDPOINT=https://api-test.to-teacher.com/internal/v1/events/user-merged.
- 배포: tosunsaeng-staging-cluster의 tosunsaeng-identity-test-service를 revision5로 업데이트. desired1 유지, 최종 running1/pending0, 배포 성공 및 steady state 확인. 이미지 commit 88ff5bedc1ee661ccd38a8a1d2c4dbf3b9f03f2d 유지.
- 테스트와 결과: UI JSON 검증, ECS 배포 성공/태스크 실패0 관측, CloudWatch에서 2026-09-30T04:58:28Z Started IdentityApplication(35.888초) 확인. 조회된 기동 로그에 ERROR/WARN/Exception 없음. git diff --check 수행. 코드 변경 없어 Gradle 미실행. 실제 이벤트/계정 병합은 수행하지 않음.
- 유지한 계약: 기존 RS256 workload 계약 및 LC 전용 outbox 사용, 기존 이미지·비밀값 참조·역할·네트워크·태스크 수 보존. OwnerEvent/Billing 발행 OFF. 운영/LC 서비스 변경 없음. 비밀값 비기록. 기존 사용자 파일 변경 보존.
- 결정사항: 테스트 연결 설정과 배포 준비 완료로 판단, 실제 이벤트 E2E 성공과 구분.
- 위험 요소: 204 수신/동일 eventId 재시도/중복 방지/source 토큰 차단/target 기록 이전 미검증. 장기 무오류 보장 아님. 다음 코드 배포 시 새 태스크 환경설정 보존 필요. 예상 밖 파일/설정 변경 없음.
- 다음 작업: 사용자가 지정한 테스트 Guest/MEMBER로 병합 E2E 검증. 문제 시 이전 revision4로 서비스 롤백 가능. Jira 변경 없음; 댓글 초안은 테스트 설정 배포 성공, 기록 파일 2개, 실조회 결과 및 E2E 미검증 요약이며 자동 등록하지 않음.

## 2026-09-30 — 테스트 이벤트 배포 작업 식별 기록 보완

<!-- codex-turn:01a0f0ab-6a03-7c03-8881-57cff08326ac -->

- 브랜치: develop.
- 작업 목표: 이번 테스트 UserMerged 활성화·배포 결과에 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 테스트 Identity revision5에 승인된 workload/Guest merge/기존 publisher 설정 적용 및 서비스 배포 완료. 상세 설정과 검증은 바로 앞 작업 항목에 기록. 이번 보완에서 추가 외부 변경 없음.
- 실행한 테스트와 결과: ECS 배포 성공·running1/pending0·steady state와 애플리케이션 기동 완료 로그 확인. git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 기존 LC 전용 발행, RS256 workload 계약, 운영 미변경, OwnerEvent/Billing 발행 OFF, 비밀값 비기록 및 과거 기록 보존.
- 결정사항: 배포 완료와 실제 이벤트 E2E 검증을 구분.
- 위험 요소: 실제 계정 병합·204 응답·중복 방지·source 차단·target 기록 이전은 미검증. 예상 밖 변경 없음.
- 다음 작업: 지정 테스트 계정으로 병합 E2E 검증 및 향후 배포 시 revision5 설정 보존 확인. Jira 변경 없음.

## 2026-09-30 — 로그인·병합·챌린지 통합 테스트 화면 범위 조사

- 브랜치: develop.
- 작업 목표: localhost:4173 기존 화면을 로그인부터 회원 통합·10초 챌린지 검증까지 확장하기 위한 계약 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 테스트 도구 코드 변경 없음.
- 수행 내용: tools/auth-test의 HTML/JS/로컬 게이트웨이/Mock 테스트와 Identity 프론트 Firebase 계약 확인. 별도 Learning Core 저장소의 ten-second-challenge-frontend-api.md를 읽어 클라이언트 호출 순서 확인; Learning Core 소스 복사 없음.
- 구현 사실: 기존 도구는 로그인/가입/전화 연결/프로필/재발급과 today 읽기만 지원. Guest prepare의 MERGE_REQUIRED 뒤 명시 확인과 Guest Bearer/Firebase proof로 merge, 신규 승격은 별도 enrollment/upgrade 계약. 챌린지는 X-Challenge-Date, attempt, M4A/AAC audio/mp4(2MiB), presigned PUT의 Authorization 미전송, 동일 answer Idempotency-Key 보존 및 결과 polling 계약이 필요.
- 테스트와 결과: 파일·계약 읽기 및 git diff --check. 구현 전 범위 확인 단계여서 Gradle/Node 테스트 미실행. 실제 로그인/녹음/업로드/병합/API 호출 없음.
- 유지한 계약: Identity 외 챌린지/S3 코드 추가 금지 규칙 및 비밀값 비기록. 기존 사용자 수정 파일 보존.
- 결정사항: 전체 통합 화면은 Identity 서비스 저장소가 아닌 별도 로컬 테스트 프로젝트로 분리하는 방향을 사용자에게 확인. 기존 localhost 주소 유지 가능하나 기존 프로세스 확인과 재시작 필요.
- 위험 요소: 브라우저 녹음이 M4A/AAC를 지원하지 않을 수 있어 파일 업로드 대안 필요. MEMBER 전용 챌린지 결과는 Guest 기록 이전 검증과 별개이며 기존 시험 기록 이전은 별도 LC API 계약 필요. 구현/E2E 모두 미완료. 예상 밖 변경 없음.
- 다음 작업: 별도 프로젝트 생성 위치·분리 방향 확인 후 안전한 테스트 UI 구현, Mock 테스트 및 실제 화면 QA. Jira 변경 없음.

## 2026-09-30 — 통합 테스트 화면 조사 작업 식별 기록 보완

<!-- codex-turn:01a0f0b0-d01f-7e82-8c4d-6c891389466a -->

- 브랜치: develop.
- 작업 목표: 로그인·병합·챌린지 테스트 화면 조사 결과에 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 기존 로컬 인증 도구와 Identity/LC 클라이언트 계약을 확인하고 별도 프로젝트 분리 방향을 사용자에게 질문. 상세 조사 내용은 바로 앞 항목에 보존.
- 실행한 테스트와 결과: git diff --check 통과. 코드 구현 전 범위 확인 단계여서 Gradle/Node 테스트 미실행.
- 유지한 계약: Identity 도메인 경계, 과거 작업 기록 보존, 비밀정보 비기록. 실제 인증·병합·업로드·서버 재시작 없음.
- 결정사항: 별도 tosunsaeng-integration-test 폴더에서 localhost:4173을 유지하는 방향 확인 대기.
- 위험 요소: 구현 및 실연동 검증 미완료, 브라우저 M4A/AAC 지원과 기존 시험 기록 이전 검증 계약 확인 필요. 예상 밖 변경 없음.
- 다음 작업: 사용자 확인 후 독립 테스트 화면 구현과 Mock/브라우저 검증. Jira 변경 없음.

## 2026-09-30 — 독립 로그인·병합·챌린지 통합 테스트 화면 구현

- 브랜치: develop (Identity 기록만 변경; 독립 도구는 별도 디렉터리).
- 작업 목표: 사용자 승인에 따라 Identity 도메인 경계를 유지하면서 localhost:4173 통합 테스트 화면 제공.
- 변경 파일: Identity의 docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 별도 /Users/msde76/tosunsaeng-integration-test에 index.html, app.js, session.mjs, challenge.mjs, server.mjs, app.test.mjs, server.test.mjs, challenge.test.mjs, merge.test.mjs, README.md 생성. /private/tmp/tosunsaeng-integration-test에서 구현한 뒤 사용자 권한 승인으로 새 영구 폴더에 복사. 기존 Identity 도구/서비스 코드는 미변경.
- 구현 내용: 기존 로컬 인증 도구 기반 독립 클라이언트. Google/phone/signup/exchange/profile/reissue 유지. Guest와 MEMBER 자격을 별도 메모리에 보관하고 신규 테스트 Guest 생성 또는 기존 테스트 Guest 입력 지원. 서버 GUEST 검사·prepare MERGE_REQUIRED·체크박스 및 확인창 후 merge 수행. 응답 유실 자동 재병합 금지. 병합 후 양측 ACCOUNT_MERGED_TOKEN_REJECTED만 성공으로 판단하고 LC 완료 시험 ID 포함 여부 비교. 기존 기록0은 미검증으로 표시.
- 챌린지 구현: 실제 서버 계약의 today→question→attempt→M4A/AAC 파일 또는 최대10초 지원 브라우저 녹음→presigned S3 PUT→answer→결과/최대60초 polling/history. X-Challenge-Date 전달, S3 사용자 Authorization 미전송, 같은 answer key/body 보존 및 최초 제출 시도 이후 녹음/덮어쓰기 잠금. AI failed와 API200 구분. 새 응시 자동 생성 없음. 녹음은 자동 전송하지 않고 사용자 업로드 버튼으로만 전송.
- 보안: localhost/동일 Origin/전용 헤더, 정확한 테스트 호스트·경로 allowlist, redirect 금지, presigned HTTPS S3 호스트 검증, 토큰 메모리 전용·오류 코드 제한 표시. 실제 secret/token을 파일에 저장하지 않음. 개인정보가 포함될 수 있는 발화/피드백과 네트워크 공유 금지 안내.
- 실행한 테스트와 결과: Node Mock 전체6개 통과(로그인/동의/회전, 게이트웨이 origin·host·redirect·body 크기, 토큰 요약, 챌린지 경로/업로드 host, answer 응답유실·같은키·무JWT PUT, merge 확인·source 차단·기록 비교). 루프백 테스트는 최초 sandbox EPERM 후 권한 승인 실행. ./gradlew clean test도 캐시 접근 sandbox 오류 후 승인 재실행 BUILD SUCCESSFUL. 브라우저 localhost 새 화면·초기 비활성 버튼·M4A 지원 안내·화면 렌더 확인. git diff --check 통과.
- 실행 상태: 영구 프로젝트의 Node 서버를 127.0.0.1:4173에 실행, 기존 포트 점유 프로세스 없어 중지/교체 작업 불필요. 기존 내장 브라우저 localhost 탭 새로고침 및 유지. Firebase 인증·실계정 생성·병합·마이크·실음성 업로드는 실행하지 않음.
- 유지한 계약: Identity/LC API 또는 배포 설정 변경 없음, Learning Core 서비스 코드 복사 없음, 독립 클라이언트만 추가. Guest의 챌린지 접근을 가정하지 않음. 기존 사용자 dirty 파일 보존, 예상 밖 변경 없음.
- 결정사항: 신규 Guest 승격은 지원하지 않고 기존 MEMBER 병합만 테스트. Chrome/OS에서 M4A/AAC 녹음 미지원이면 올바른 M4A 파일 사용. 앱의 Android SDK/업데이트·Stage9 응답복구 검증 대체 아님.
- 위험 요소: 실제 회원 인증/병합/AI E2E 및 S3 CORS 미검증. 새 Guest에는 이전 기록이 없어 별도 기록 보유 Guest 필요. 완료 시험 ID 비교는 전체 데이터 이전·이벤트204·중복 재전송 검증을 대신하지 않음. 녹음 파일의 실제 코덱은 AI 검증; 잘못된 파일은 응시 소비 가능. 토큰은 새로고침하면 소실, 서버 세션은 남음.
- 다음 작업: Chrome에서 localhost 열고 공개 웹 Firebase 설정 적용→MEMBER 로그인부터 단계별 실행. 테스트 계정/약관 버전·가상 전화·S3 CORS 확인 후 실제 제출, 이슈는 서버 로그와 함께 진단. 사용자 직접 실행 전 실제 데이터 변경 없음. Jira 변경 없음; 등록용 요약 초안은 독립 도구 구현·Mock6/Gradle 통과·실E2E 미검증이며 자동 등록하지 않음.

## 2026-09-30 — 독립 통합 테스트 구현 작업 식별 기록 보완

<!-- codex-turn:01a0f0b2-c659-78a2-9787-ecd2246ed647 -->

- 브랜치: develop.
- 작업 목표: 이번 독립 테스트 화면 구현·검증 결과에 현재 turn 식별자 추가.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 별도 프로젝트 파일 목록과 구현 상세는 바로 앞 항목에 보존.
- 구현 내용: 독립 프로젝트에서 로그인·전화 연결·가입·재발급, Guest 병합 확인 및 source 차단/완료 시험 비교, 챌린지 음성 제출/AI 결과 조회 화면 제공. localhost:4173 실행 및 브라우저 초기 화면 확인 완료.
- 테스트와 결과: Node Mock6개, ./gradlew clean test, git diff --check 통과. 실제 계정 변경이나 음성 전송 테스트는 미실행.
- 유지한 계약: Identity 서비스 도메인 경계와 기존 외부 API 유지, 운영 설정 미변경, 비밀정보 비기록, 과거 기록과 사용자 수정 보존.
- 결정사항: 도구 준비 완료와 실제 E2E 성공을 구분. 코드/배포 추가 변경 없이 기록 식별자만 보완.
- 위험 요소: S3 CORS·실제 Firebase/병합/AI 연결은 사용자 단계별 검증 필요. 예상 밖 변경 없음.
- 다음 작업: Chrome에서 Firebase 구성 적용 후 MEMBER 로그인부터 검증. Jira 변경 없음.

## 2026-09-30 — 기존 테스트 Google 계정 재사용 안내

<!-- codex-turn:01a0f0c0-c635-7250-8633-bf35607c3e6c -->

- 브랜치: develop.
- 작업 목표: 기존 계정 사용 가능 여부와 신규 계정 필요 조건 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 기존 도구 및 확인한 exchange 계약 기준으로 같은 Firebase 프로젝트/Identity 테스트 DB의 MEMBER는 기존 Google로 인증 후 Identity 로그인하고 phone/signup 생략 가능함을 안내. 병합 source는 별도 Guest이며 챌린지 당일 제한은 초기화되지 않음.
- 테스트와 결과: 문서 안내 작업, git diff --check 수행. 코드 변경/실 API 호출 없어 Gradle·Node 재실행 안 함.
- 유지한 계약: 기존 MEMBER/Guest 분리, 가입과 로그인 구분, 비밀정보 비기록. 실제 계정/서버 변경 없음.
- 결정사항: 새 Google 계정은 필수 아님; 신규 가입 검증용으로만 별도 계정 고려.
- 위험 요소: 현재 계정 존재/당일 챌린지 진행도 live 조회 없음. ENROLLMENT_REQUIRED이면 같은 프로젝트/DB/계정 여부 확인. 예상 밖 변경 없음.
- 다음 작업: 기존 Google 계정으로 Identity 로그인 후 프로필/챌린지 단계 실행. Jira 변경 없음.

## 2026-09-30 — Chrome 통합 테스트 진행 결과 확인

- 브랜치: develop.
- 작업 목표: 사용자가 실행한 localhost:4173 Chrome 결과의 성공 범위 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 지정 Chrome의 기존 탭 읽기. 검증표에서 Google 인증·Identity 로그인·MEMBER 프로필·토큰 재발급·LC today 인증 성공 확인. today의 문제3개 모두 not_started/not_requested와 nextQuestionNumber=1 확인. Guest 생성 CONSENT_REQUIRED와 빈 정책 버전/필수 동의 미선택 확인.
- 테스트와 결과: 실제 화면 관측(별도 네트워크/서버 로그 재검증 아님), git diff --check. 코드 변경 없어 Gradle/Node 재실행 안 함.
- 유지한 계약: 기존 세션 유지를 위해 새로고침 없음. 추가 API 호출·약관 체크·계정 생성·병합·녹음·업로드 없음. 민감 토큰/개인정보 비기록.
- 결정사항: 인증 연동은 화면상 성공, 병합과 음성/AI 흐름은 아직 검증 전. 기존 MEMBER 가입을 반복할 필요 없으나 새 Guest 생성은 해당 Guest의 정책 동의가 별도로 필요함을 안내.
- 위험 요소: 챌린지 문제/음성/AI 및 병합 E2E 미완료. 약관 버전은 현재 배포값 확인 후 사용자 직접 동의 필요. 예상 밖 변경 없음.
- 다음 작업: 챌린지 다음 문제→응시→녹음/업로드→제출→결과 검증. 병합 시험을 원하면 정책 버전/동의를 확인한 뒤 Guest 준비부터 수행. Jira 변경 없음.

## 2026-09-30 — Chrome 결과 확인 작업 식별 기록 보완

<!-- codex-turn:01a0f0c1-b738-7be1-9bee-47f9b41bf1f8 -->

- 브랜치: develop.
- 작업 목표: 이번 Chrome 테스트 진행 결과 조회에 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 기존 Chrome 화면에서 Google/Identity 로그인·MEMBER 프로필·재발급·LC today 성공 및 Guest 생성 CONSENT_REQUIRED 확인. 상세 관측은 바로 앞 기록에 보존.
- 테스트와 결과: 화면 읽기 및 git diff --check 통과. 코드 변경 없어 Gradle/Node 미실행.
- 유지한 계약: 새로고침/추가 API 호출/약관 동의/병합/녹음/업로드 없음, 비밀정보 비기록, 과거 기록 보존.
- 결정사항: 인증 성공과 아직 수행하지 않은 병합·챌린지 제출/AI 검증을 구분하여 안내.
- 위험 요소: 실E2E 미완료, 정책 버전 확인 및 사용자 직접 동의 필요. 예상 밖 변경 없음.
- 다음 작업: 사용자가 챌린지 다음 문제부터 진행하거나 Guest 준비 후 병합을 별도 검증. Jira 변경 없음.

## 2026-09-30 — Chrome 채점 및 Guest 생성 실진단

<!-- codex-turn:01a0f0c6-2340-7b00-9593-40ae75ce986b -->

- 브랜치: develop.
- 작업 목표: 사용자 요청에 따라 Chrome에서 채점 미진행·Guest 생성 문제 확인 및 가능한 직접 테스트.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 기존 Chrome localhost 탭을 새로고침 없이 읽고 녹음/attempt 준비 후 upload 일반 오류·answer 버튼 비활성·결과0건 확인. 문제 번호1로 결과 GET을 직접 실행해 solvedQuestionCount=0/question=null 재확인. 브라우저 수집 error/warn 로그는 빈 목록. 독립 도구의 업로드·오류 처리 코드를 읽어 현재 메시지만으로 CORS/네트워크 원인을 확정할 수 없음을 확인.
- Guest 관측: guestCreate CONSENT_REQUIRED, 정책 버전 입력 비어 있고 필수 동의 미선택. 테스트 Guest 확인 체크와 병합 확인 체크는 필수 정책 동의를 대신하지 않음.
- 테스트와 결과: 실제 LC 결과 조회 성공·제출 이력0 확인, git diff --check. 코드 변경 없어 Gradle/Node 재실행 안 함. AI 채점 실행/실패 여부 자체는 아직 검증 못 함.
- 유지한 계약: 토큰·서명URL·음성 원문 비기록. 기존 세션 보존. 임의 약관 동의/Guest 생성/병합/음성 전송/서버 설정 변경 없음.
- 결정사항: 현재 녹음된 테스트 음성을 테스트 S3에 재업로드하고 LC 제출할지 구체적으로 승인 요청. Guest용 정책 입력·약관 동의는 사용자가 직접 수행하도록 요청. 사용자 응답 후 승인 범위에서 테스트 재개.
- 위험 요소: 업로드 원인 미확정, S3 CORS는 가능성일 뿐 확정 아님. 화면의 일반 오류를 Google 인증 실패나 AI 실패로 단정하지 않음. 예상 밖 변경 없음.
- 다음 작업: 업로드 승인 후 동일 녹음으로 오류 재현/진단 및 성공 시 제출·AI 결과 확인. 사용자 정책 동의 완료 후 Guest 생성 테스트. Jira 변경 없음.

## 2026-09-30 — S3 업로드 CORS 원인 확정 및 정책 버전 입력

<!-- codex-turn:01a0f0c6-2340-7b00-9593-40ae75ce986b -->

- 브랜치: develop.
- 작업 목표: 사용자 승인된 음성 재업로드 테스트와 AWS 정책 버전 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 앱/서버 코드 변경 없음.
- 수행 내용: 사용자 현재 테스트 음성 업로드·제출 허용 수신 후 Chrome 업로드 버튼 재시도, 같은 일반 오류 재현. Identity test:5 환경값에서 PRIVACY_CONSENT_VERSION=privacy-v1, TERM_CONSENT_VERSION=term-v1 확인 후 Chrome 해당 입력란에 입력. 동의 체크/Guest 생성은 미수행. Chrome native 진단은 OS 권한 미부여로 접근하지 못했고 우회하지 않음.
- 진단: AWS LC test:11의 AWS_S3_BUCKET_NAME/region 확인 후 테스트 버킷에 무인증 OPTIONS 요청. localhost:4173 Origin, PUT, content-type 조합에 S3 HTTP403 AccessForbidden 및 CORSResponse: CORS is not enabled for this bucket 응답 확인. 파일 생성/음성/토큰 전송 없는 preflight 검사. 최초 sandbox DNS 오류 후 사용자 승인으로 네트워크 진단 재실행.
- 테스트와 결과: 브라우저 업로드 실패 재현 및 S3 CORS 미설정 확인. 따라서 현재 브라우저 업로드는 차단되며 answer/AI까지 진행하지 못함. git diff --check. 코드 변경 없어 Gradle/Node 재실행 안 함.
- 유지한 계약: AWS 환경·CORS·권한 변경 없음. 동의 대행 없음. 실제 Secret/Token/서명URL 비기록, 기존 세션/녹음 보존. 운영 미변경.
- 결정사항: 테스트 버킷의 localhost:4173 Origin에 PUT 및 content-type만 허용하는 CORS 설정을 사용자 승인 후 적용하는 방향 제안. 공개 읽기/버킷 정책 변경이나 CORS 우회는 하지 않음.
- 위험 요소: CORS 해결 뒤에도 서명/형식/AI 후속 문제가 있을 수 있어 실제 성공은 추가 검증 필요. Guest는 사용자 정책 동의가 필요. 예상 밖 코드 변경 없음.
- 다음 작업: 테스트 버킷 CORS 수정 승인 후 적용·OPTIONS 재검증 및 현재 녹음 업로드/제출/AI 확인. 사용자 약관 체크 완료 후 Guest 생성 테스트. Jira 변경 없음.

## 2026-09-30 — 재인증 화면 보완·CORS 수정 및 실제 AI 채점 완료

<!-- codex-turn:01a0f0c6-2340-7b00-9593-40ae75ce986b -->

- 브랜치: develop.
- 작업 목표: 사용자 요청의 FIREBASE_RECENT_AUTH_REQUIRED 원인 확인 및 업로드 문제와 함께 수정.
- 변경 파일: 독립 /Users/msde76/tosunsaeng-integration-test의 app.js, challenge.mjs, merge.test.mjs, README.md. Identity는 docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md만 갱신. 임시 작업 폴더에서 apply_patch 후 승인된 복사로 영구 프로젝트 반영.
- 원인: FirebaseAdminAuthenticationVerifier가 LOGIN_EXCHANGE 외 목적의 auth_time에 high-risk-max-authentication-age(기본PT5M)를 검사. 도구의 getIdToken(true)는 토큰 갱신이지 Google 재인증이 아님. 기존 Google 버튼은 재인증 전 pair/챌린지를 지워 작업 흐름도 손실시킴.
- 구현: 같은 Firebase UID reauthenticateWithPopup 성공 후 fresh token을 준비하고 MEMBER/Guest/녹음/attempt 보존. recent-auth 오류에 명확한 사용자 재인증 안내와 수동 prepare 재실행 유도. 확정된 인증 거절 시 merge 불명 상태를 해제하되 자동 병합 금지. S3 fetch 실패는 S3_NETWORK_OR_CORS_ERROR로 분리. 서버5분 기준/Token 검증 완화 없음.
- AWS 변경: 사용자 적용 직전 명시 승인 후 테스트 오디오 버킷의 CORS를 AllowedOrigins localhost:4173, AllowedMethods PUT, AllowedHeaders Content-Type, MaxAgeSeconds300으로 저장. 성공 UI 및 무인증 OPTIONS200·정확한 allow headers 확인. 버킷 공개 접근·IAM·운영 버킷 변경 없음.
- 실제 검증: 사용자 사전 승인된 기존 녹음을 Chrome에서 PUT 성공 후 LC answer로 제출. today에서 1번 submitted/processing 확인 뒤 상세 결과 GET에서 solvedQuestionCount1, gradingStatus completed, verdict needs_improvement, transcript/feedback 존재 확인. 음성·발화·피드백 원문은 기록하지 않음. 신규 응시 추가 생성하지 않음.
- 테스트와 결과: Node Mock6개 통과(최근인증 거절→재인증→MEMBER/Guest 및 이전이력 보존 회귀 포함), ./gradlew clean test BUILD SUCCESSFUL, git diff --check. 기존 코드 경고만 확인. 운영 API 호출 없음.
- 현재 사용자 단계: Chrome의 기존 도구를 새로고침하지 않아 Guest 자격 보존. 기존 버전 Google 재인증 버튼 실행 후 Google 계정 선택 popup에서 사용자 입력 대기. 기존 버전 동작상 MEMBER pair/챌린지 화면 상태는 초기화되었으나 서버의 채점 완료 결과와 Guest 상태는 보존. 새 코드 검증은 Mock 기반이며 새 로딩부터 적용.
- 유지한 계약: 기존 RS256/Firebase 계약, S3 JWT 미전송, 사용자 약관 동의 대행/실제 병합 없음. 비밀값 비기록, 기존 dirty 파일 보존. 예상 밖 코드 변경 없음.
- 위험 요소: 실제 재인증 완료·Guest 병합 E2E는 아직 미검증. 수정 코드 적용용 새로고침은 현재 Guest 자격을 소실시키므로 현 시험 종료 후 수행. 기존 음성1건 제출로 해당 문제 응시 소비됨.
- 다음 작업: 사용자 동일 Google 재인증 완료 후 기존 화면에서 Identity exchange/Guest prepare 확인. 실제 merge는 별도 명시 확인 후 진행. 테스트 종료 후 새 화면 로딩해 보완 UX 확인. Jira 변경 없음; 댓글 초안은 독립도구4파일 수정·Mock6/Gradle 성공·실제 채점 완료·병합 미검증 요약이며 자동 등록하지 않음.

## 2026-09-30 — 실제 재인증 후 Guest 병합 준비 성공

<!-- codex-turn:01a0f0c6-2340-7b00-9593-40ae75ce986b -->

- 브랜치: develop.
- 작업 목표: 사용자 Google 재인증 완료 후 최근인증 오류 해소 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Chrome의 Google 인증 완료 표시 확인 후 Identity exchange와 Guest prepare를 순서대로 직접 호출. MEMBER 토큰 형태 요약 및 기존 MEMBER로 병합 가능 안내·병합 버튼 활성 확인. 실제 merge는 누르지 않음.
- 테스트와 결과: 실제 로그인/병합 prepare 성공으로 FIREBASE_RECENT_AUTH_REQUIRED 해소 확인. 현재 Guest 인증으로 prepare 성공했으므로 Guest 준비 정상 확인. git diff --check 통과; 추가 코드 변경 없어 테스트 재실행 없음.
- 유지한 계약: 원문 토큰 비기록, 자동 병합 금지, 서버 최근인증 기준 유지, 사용자 재인증 직접 수행.
- 결정사항: 업로드/실제 AI 채점 및 재인증 후 병합 준비 검증 완료. 최종 계정 병합/이벤트 E2E와는 구분.
- 위험 요소: 실제 merge/source 차단/기록 이전 미실행, 시간 경과 시 재인증 재요구 가능. 예상 밖 변경 없음.
- 다음 작업: 사용자 최종 확인 후 실제 병합과 이벤트 검증. 현재 브라우저는 구버전 스크립트이므로 시험 종료 후 새로고침하여 보완 UX 적용. Jira 변경 없음.

## 2026-09-30 — 재인증·업로드 수정 작업 식별 기록 보완

<!-- codex-turn:01a0f0ce-0fc0-7af1-baad-490e85aff2b7 -->

- 브랜치: develop.
- 작업 목표: 이번 수정 및 실검증 결과에 정확한 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 독립 도구 수정 파일과 AWS 변경 상세는 앞선 항목에 보존.
- 수행 내용: 승인된 localhost 한정 테스트 버킷 CORS 설정 후 실제 음성 업로드·제출·AI 채점 완료 확인. 독립 도구의 같은 UID 재인증 상태 보존/최근인증 오류 안내/S3 오류 분리 수정. 사용자 Google 재인증 후 Identity 로그인 및 Guest 병합 준비 성공 확인.
- 테스트와 결과: Node Mock6개 및 Gradle clean test 성공, CORS OPTIONS200, 실제 채점 completed, Guest prepare 성공. git diff --check 통과.
- 유지한 계약: 서버 최근인증 기준 유지, 운영·IAM·공개 접근 변경 없음, 비밀정보 비기록, 과거 기록 보존. 실제 계정 병합 미실행.
- 결정사항: 현재 브라우저 Guest 보존을 위해 새로고침 보류; 새 도구 코드는 다음 로딩부터 적용.
- 위험 요소: 최종 병합·이벤트 수신·기록 이전은 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 최종 확인 후 실제 병합 E2E 검증. Jira 변경 없음.

## 2026-09-30 — 사용자 병합 후 검증표 해석

- 브랜치: develop.
- 작업 목표: 병합 후 공통 안내 문구가 실패인지 실제 결과를 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 Chrome localhost 검증표 읽기. Identity 병합·Identity source 차단·LC source 차단·LC target 인증 성공 표시 확인. 이전 대상 완료 시험0건 및 완료 시험 이전 미검증 표시 확인. 화면에는 과거 CONSENT_REQUIRED/recent-auth/upload 실패도 이후 성공 행과 함께 남음.
- 테스트와 결과: 현재 화면 실조회 및 git diff --check. 추가 API/병합/설정 변경 없음. 코드 변경 없어 Gradle/Node 재실행 안 함.
- 유지한 계약: 원문 토큰/개인정보 비기록, 기존 세션 보존, 자동 재병합 금지.
- 결정사항: 병합 및 양 서비스의 이전 Guest 차단/대상 MEMBER 접근은 화면상 확인됨. 하단 공통 안내는 오류가 아니며 실제 데이터 이전 검증 완료와는 구분.
- 위험 요소: source에 완료 시험이 없어 기록 이전 확인 불가. 이벤트204/동일event 재전송 처리는 서버 로그/별도 시험 미확인. 예상 밖 변경 없음.
- 다음 작업: 필요 시 발행/수신 상태를 읽기 전용으로 확인하고 기록 보유 테스트 Guest의 이전 검증을 별도 계획. Jira 변경 없음.

## 2026-09-30 — 병합 후 검증표 조회 작업 식별 기록 보완

<!-- codex-turn:01a0f0d5-a5a6-7ad1-906b-614aeab7aad3 -->

- 브랜치: develop.
- 작업 목표: 이번 병합 후 결과 확인에 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Chrome 검증표의 Identity 병합·Identity/LC source 차단·LC target 인증 성공과 이전 대상 기록0건 확인 결과 기록. 하단 문구는 공통 안내이며 과거 실패 행과 현재 성공을 구분해 설명.
- 테스트와 결과: 화면 읽기 및 git diff --check. 코드 변경 없어 Gradle/Node 미실행.
- 유지한 계약: 비밀정보 비기록, 과거 기록 보존, 추가 API/병합/설정 변경 없음.
- 결정사항: 인증 차단 검증 성공과 실제 기록 이전 미검증을 구분.
- 위험 요소: 이벤트204·중복 재전송 및 기록 이전은 별도 검증 필요. 예상 밖 변경 없음.
- 다음 작업: 필요 시 발행/수신 상태 조회와 기록 보유 테스트 Guest의 이전 검증 계획. Jira 변경 없음.

## 2026-09-30 — 통합 테스트 완료 범위 재확인

- 브랜치: develop.
- 작업 목표: 사용자의 전체 검증 완료 여부 질문에 현재 증거와 남은 범위를 구분.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Chrome 검증표 확인 후 병합 후 검증 버튼 및 챌린지1 상세 결과 GET을 직접 재실행. Identity/LC source 병합 전용 차단과 LC target 인증 성공, solved1/gradingStatus completed 및 발화·피드백 존재 확인. 로그인/프로필/재발급/Guest 생성/업로드/제출은 현재 화면의 앞선 성공 기록과 이전 실검증 근거를 사용.
- 테스트와 결과: 현재 인증 상태·채점 결과 읽기 재검증 성공, git diff --check. 코드 변경 없어 Gradle/Node 재실행 없음.
- 유지한 계약: 실제 계정/데이터 추가 변경 없음, 원문 토큰/개인 발화 비기록. 이전 병합·음성 제출 재실행 안 함.
- 결정사항: 기본 정상 흐름 통과이지 모든 예외·출시 준비 검증 완료는 아님. 과거 실패 표시는 이후 성공과 구분.
- 위험 요소: source 완료 시험0건으로 실제 기록 이전 미검증. 이벤트204·중복/재시도·장애복구, 새 가입/전화 연결의 이번 회차 검증, 챌린지2·3 진행/중복·만료·AI실패, Android SDK/운영 전환 검증 남음. 예상 밖 변경 없음.
- 다음 작업: 기록 보유 테스트 Guest 이전과 서버 이벤트 검증을 우선하고 Android 앱 통합/예외 시나리오 수행. Jira 변경 없음.

## 2026-09-30 — 전체 검증 범위 확인 작업 식별 기록 보완

<!-- codex-turn:01a0f0d6-dc0e-7643-bc0f-aa6c386ca867 -->

- 브랜치: develop.
- 작업 목표: 이번 통합 테스트 완료 범위 확인에 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Chrome에서 병합 후 읽기 검증과 1번 결과 조회를 재실행해 양 서비스 source 차단·target 인증 및 채점 completed를 확인. 기본 흐름 성공과 미검증 항목을 구분해 안내.
- 테스트와 결과: 실제 읽기 재검증 성공 및 git diff --check 통과. 코드 변경 없어 Gradle/Node 재실행 안 함.
- 유지한 계약: 비밀정보 비기록, 과거 기록 보존, 추가 계정 변경/병합/음성 제출 없음.
- 결정사항: 프론트 앱 연동 시험 진행 가능하나 운영 출시 검증 완료는 아님.
- 위험 요소: 기록 보유 Guest 이전, 이벤트 중복/장애 복구, 신규 가입/전화 연결 재검증, 나머지 챌린지와 Android 연동 검증 필요. 예상 밖 변경 없음.
- 다음 작업: 남은 데이터 이전 및 앱/예외 시나리오 검증. Jira 변경 없음.

## 2026-09-30 — 소셜 로그인 제공자 준비 상태 읽기 점검

<!-- codex-turn:01a0f0da-5a97-71a2-91b8-9d2abc0cc7f2 -->

- 브랜치: develop.
- 작업 목표: Google Play 서명·Apple·Kakao 등록 준비의 필요성과 현재 상태 확인. 외부 변경 금지.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md (작업 기록만).
- 수행 내용: Firebase Google/Apple/전화 활성화 및 Kakao 부재 확인. Android com.toteacher.app에 사용자가 전달한 SHA-1 두 개·SHA-256 두 개 등록 확인. Apple Service ID·Team ID·Key ID·비공개 키 입력란은 빈 상태로 관측. 코드상 Apple/Kakao 기본 false, Kakao provider 기본 oidc.kakao 확인.
- 테스트와 결과: Firebase 화면 및 application.yml/프론트 계약 읽기 점검. 코드 변경 없어 Gradle 미실행; git diff --check 수행.
- 유지한 계약: Firebase ID Token→Identity 토큰 계약 유지. 비밀정보 비기록, 등록·저장·권한·배포 변경 없음.
- 결정사항: Google Play 앱 서명 인증서 대조→Apple 개발자 설정→Kakao Generic OIDC 준비 순서 권장. 로컬 빌드는 해당 빌드 서명 등록으로 시험 가능하며 Play 서명이 선행 필수는 아님.
- 위험 요소: Play 인증서 실제 일치, Apple Developer 설정, Kakao Developers 앱 유무 및 Identity Platform 업그레이드 상태 미확인. 배포 중 provider 플래그는 이번에 재조회하지 않음. Firebase 활성화만으로 실제 로그인 준비 완료 아님.
- 다음 작업: 승인 후 각 제공자 설정과 Identity 플래그를 준비하고 실제 Android 로그인 검증. 기존 사용자 변경 보존, 예상 밖 수정 없음. Jira 변경 없음.

## 2026-09-30 — Play 앱 서명 인증서 확인 경로 안내

<!-- codex-turn:01a0f0e0-5d2a-7ba2-ad15-d8907cf2168b -->

- 브랜치: develop.
- 작업 목표: Google Play 앱 서명 인증서 지문 조회 위치 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Play Console 앱 선택→앱 무결성→앱 서명에서 앱 서명 키 인증서 SHA-1/SHA-256 확인 후 Firebase Android 앱 지문에 추가하도록 안내. 업로드 키 인증서와 구분.
- 테스트와 결과: 코드 변경 없어 테스트 미실행. git diff --check 수행. 이번에 Play 화면 실조회는 하지 않음.
- 유지한 계약: 기존 개발용 지문 보존, 비밀정보 비기록, 외부 설정 변경 없음.
- 결정사항: Play 배포용 앱 서명 지문을 기존 Firebase 지문과 대조하고 없는 것만 추가.
- 위험 요소: 실제 인증서 일치 여부와 Play 배포 앱 로그인은 아직 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자가 인증서 화면을 확인한 뒤 Firebase 등록 및 Play 설치 빌드 로그인 시험. Jira 변경 없음.

## 2026-09-30 — 변경된 Play 앱 서명 메뉴 실조회

- 브랜치: develop.
- 작업 목표: 사용자 Chrome 탭에서 앱 무결성 메뉴 이전 위치 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 앱 무결성의 이전 안내 확인 후 Google Play로 보호됨→Play 스토어 보호 세부 정보→Play 앱 서명 관리로 이동, 앱 서명 화면 실조회. 사용 중 키와 이전 앱 서명 키가 함께 존재하며 기존 Firebase 등록 지문 중 한 쌍이 업로드 키 인증서임을 확인. 지문 원문은 기록하지 않음.
- 테스트와 결과: 브라우저 읽기 조회, git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 설정·키·인증서 변경 없음, 사용자 탭 유지.
- 결정사항: 앞선 메뉴 안내를 현재 UI 경로로 정정. 업로드 키가 아닌 실제 배포 서명들을 대상으로 Firebase 대조 필요.
- 위험 요소: 키 업그레이드가 있어 현재·이전 배포 서명 범위 확인 필요. 앱 서명 지문 추가와 실로그인은 미수행. 예상 밖 변경 없음.
- 다음 작업: 승인 후 배포 대상 인증서 지문과 Firebase 등록 목록 대조. Jira 변경 없음.

## 2026-09-30 — Play 메뉴 실조회 작업 식별 기록 보완

<!-- codex-turn:01a0f0e2-71ca-79b1-af41-7595febcc93a -->

- 브랜치: develop.
- 작업 목표: 이번 Play Console 조회의 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 앱 무결성 이전 안내와 Google Play로 보호됨→Play 스토어 보호→Play 앱 서명 관리 경로를 실조회하고 사용자 탭을 인증서 화면에 유지.
- 테스트와 결과: 화면 조회 및 git diff --check 성공. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 설정 미변경, 비밀정보 비기록, 과거 작업 기록 보존.
- 결정사항: 최신 메뉴 경로 안내로 정정; 업로드 인증서와 배포 앱 서명 인증서 구분.
- 위험 요소: 현재·이전 앱 서명 인증서의 Firebase 등록 여부와 Play 설치 로그인 미검증. 예상 밖 변경 없음.
- 다음 작업: 승인 후 필요한 배포 인증서 대조 및 등록. Jira 변경 없음.

## 2026-09-30 — 기존 키와 양자 내성 키 선택 안내

- 브랜치: develop.
- 작업 목표: Firebase Google 로그인 지문 등록 시 화면의 키 구분 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 현재 앱 서명 키 내 기존 키의 SHA-1/SHA-256을 우선 대조하도록 안내. 기존 키는 아래 별도 이전 앱 서명 키와 다른 항목이며 키 업그레이드에 따른 이전 배포 인증서도 확인 필요.
- 테스트와 결과: 앞선 실조회 화면을 근거로 설명. 코드 변경 없어 Gradle 미실행; git diff --check 수행.
- 유지한 계약: 키 변경·업그레이드·설정 저장 없음, 비밀정보 비기록.
- 결정사항: 현재 Google 로그인 준비는 기존 방식 앱 서명 지문 대조부터 진행. 양자 내성 인증서의 추가 필요성은 실제 배포 방식과 Firebase 지원 조건 확인 전 단정하지 않음.
- 위험 요소: 기기별 실제 배포 인증서 및 새 암호화 방식 연동은 미검증. 예상 밖 변경 없음.
- 다음 작업: 현재/이전 앱 서명 인증서 대조 후 필요한 지문 등록 및 Play 빌드 로그인 검증. Jira 변경 없음.

## 2026-09-30 — 앱 서명 키 구분 안내 작업 식별 보완

<!-- codex-turn:01a0f0e5-8efd-7fc0-8c64-21897c97fb2b -->

- 브랜치: develop.
- 작업 목표: 기존 키/양자 내성 키 선택 안내에 현재 작업 식별자 추가.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 현재 앱 서명 키의 기존 방식 인증서 지문부터 Firebase와 대조하도록 안내하고, 별도 이전 앱 서명 키 및 양자 내성 키와 구분.
- 테스트와 결과: git diff --check 수행. 코드 변경 없는 안내로 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 외부 설정 미변경.
- 결정사항: 지문 등록과 키 변경은 별개이며 키 업그레이드 작업은 수행하지 않음.
- 위험 요소: 실제 배포 인증서 대조 및 양자 내성 방식의 Firebase 지원 조건 미검증. 예상 밖 변경 없음.
- 다음 작업: 필요한 배포 인증서 대조 후 승인 범위에서 등록 및 로그인 검증. Jira 변경 없음.

## 2026-09-30 — Google 앱 서명 지문 등록 확인 및 Apple 준비 안내

<!-- codex-turn:01a0f0ea-be30-7211-bdd5-a2eafe81f76f -->

- 브랜치: develop.
- 작업 목표: 사용자 추가 Firebase 지문 검증 및 Apple 로그인 설정 순서 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Chrome Firebase Android 앱에서 SHA-1/SHA-256 각 세 개 등록 확인. 새 지문 한 쌍을 Play 현재 앱 서명 키의 기존 방식 인증서 복사값과 대조하여 둘 다 일치 확인. 지문 값 자체는 기록하지 않음. Apple Developer App ID capability→Services ID/웹 반환 주소→Sign in with Apple 키→Firebase provider 구성→Identity 활성화·실검증 순서 안내.
- 테스트와 결과: 실화면 및 공개 인증서 지문 비교 성공. 코드 변경 없어 Gradle 미실행; git diff --check 수행.
- 유지한 계약: Firebase ID Token 기반 Identity 교환 유지, 비밀정보 비기록, 외부 설정 변경 없음.
- 결정사항: 현재 기존 방식 앱 서명 지문 등록 확인 완료와 실제 Play 로그인 완료를 구분. Android/web Apple 로그인을 위해 Services ID 구성 필요.
- 위험 요소: 이전 앱 서명 인증서와 양자 내성 인증서 추가 필요성, Play 실로그인 및 Apple Developer 실제 설정은 미검증. 기존 사용자 변경 보존, 예상 밖 수정 없음.
- 다음 작업: Apple Developer의 실제 앱 식별자에서 Sign in with Apple 상태 확인 후 설정 진행. Jira 변경 없음.

## 2026-09-30 — Apple Identifiers 접근 경로 안내

<!-- codex-turn:01a0f0f0-ef5d-7870-9435-55bd9f5a61b8 -->

- 브랜치: develop.
- 작업 목표: 사용자가 찾지 못한 Apple Developer Identifiers 메뉴 접근 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Identifiers 직접 링크와 App Store Connect와의 사이트 구분 안내. 앱 등록 팀 선택 및 접근 권한 확인 필요성을 설명.
- 테스트와 결과: 안내만 수행, 실제 Apple 화면 미조회. 코드 변경 없어 Gradle 미실행; git diff --check 수행.
- 유지한 계약: 외부 설정·권한 변경 없음, 비밀정보 비기록.
- 결정사항: 직접 링크에서 앱 등록 개발자 계정으로 로그인 후 실제 App ID 확인부터 진행.
- 위험 요소: 현재 Apple 로그인 팀·멤버십·권한 상태 미확인. 예상 밖 수정 없음.
- 다음 작업: 접근 화면 확인 후 Sign in with Apple 구성 점검. Jira 변경 없음.

## 2026-09-30 — Apple 편집 창과 반환 주소 입력 위치 확인

<!-- codex-turn:01a0f0f5-7222-75b1-bcb2-7e6dd5c5a242 -->

- 브랜치: develop.
- 작업 목표: Apple 열린 Edit 창에서 Firebase 반환 주소 입력 위치 검증.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Chrome의 App ID com.toteacher.app 편집 모달 실조회. 열린 창은 Services ID 웹 설정이 아니라 App ID의 Server-to-Server Notification Endpoint임을 확인. Firebase auth handler를 해당 알림 칸에 넣지 않도록 안내. Services ID의 Domains and Subdomains/Return URLs 입력값 및 Primary App ID 선택 절차 안내.
- 테스트와 결과: 브라우저 읽기 확인, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 설정 저장·입력 없음, 알림 수신 주소와 OAuth 반환 주소 분리, 비밀정보 비기록.
- 결정사항: 현재 알림 주소는 입력하지 않고 Services ID 구성 화면에서 Firebase 기본 auth 도메인과 /__/auth/handler를 설정하도록 안내.
- 위험 요소: 현재 App ID 체크의 저장 여부, Services ID 등록 상태 및 Apple 서버 알림 처리 구현 미확인. 예상 밖 수정 없음.
- 다음 작업: Services ID 웹 인증 구성 및 Firebase 연결, 실제 Apple 로그인 검증. Jira 변경 없음.

## 2026-09-30 — Apple 이메일 릴레이 화면과 Services ID 구분

<!-- codex-turn:01a0f0f9-895a-7952-807c-a92a330f3042 -->

- 브랜치: develop.
- 작업 목표: 사용자가 연 Apple 화면이 로그인 반환 주소 설정인지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 현재 Configure Sign in with Apple for Email Communication 및 Register your email sources 모달 실조회. 이메일 릴레이 발신자 등록 화면으로 확인하고 Identifiers의 Services IDs와 구분해 안내.
- 테스트와 결과: 화면 읽기 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 입력·저장 없음, 비밀정보 비기록.
- 결정사항: 이메일 소스 창에 Firebase 로그인 도메인을 입력하지 않고 Identifiers 목록에서 Services IDs 선택 또는 생성으로 진행.
- 위험 요소: Services ID 생성 및 웹 인증 구성 미완료. 예상 밖 수정 없음.
- 다음 작업: 올바른 Services ID 설정 화면에서 Primary App ID 및 Return URLs 확인. Jira 변경 없음.

## 2026-09-30 — Services ID 생성 화면 확인

<!-- codex-turn:01a0f0fd-0000-70b1-9a00-64653be875c5 -->

- 브랜치: develop.
- 작업 목표: 현재 Apple 화면이 올바른 Services ID 등록 화면인지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Register a Services ID 화면과 빈 Description/Identifier 입력란 확인. Firebase용 설명 및 별도 서비스 식별자 예시 안내; 생성 후 Sign in with Apple Configure 단계 설명.
- 테스트와 결과: 브라우저 읽기 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 입력·등록·저장 없음, 비밀정보 비기록.
- 결정사항: 현재 화면은 올바르며 Services ID는 앱 Bundle ID와 별도 식별자로 생성하도록 안내. 제안 식별자의 사용 가능 여부는 등록 시 확인 필요.
- 위험 요소: Services ID 등록과 반환 주소 설정은 아직 미완료. 예상 밖 수정 없음.
- 다음 작업: 사용자 생성 후 웹 인증 설정 확인 및 Firebase 연결. Jira 변경 없음.

## 2026-09-30 — Services ID 입력값 재안내

<!-- codex-turn:01a0f0fe-747f-79b0-a496-56eec2e1ba9f -->

- 브랜치: develop.
- 작업 목표: Services ID 예시를 그대로 입력해도 되는지 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 제안 Description/Identifier 사용 가능하되 식별자 중복 여부는 Apple 등록 시 확인해야 함을 안내. Firebase 서비스 ID에는 생성한 Identifier를 동일하게 입력하고 Bundle ID와 구분.
- 테스트와 결과: 안내만 수행, 코드 변경 없어 Gradle 미실행. git diff --check 수행.
- 유지한 계약: 외부 등록·설정 변경 없음, 비밀정보 비기록.
- 결정사항: 제안 서비스 식별자를 사용하고 Firebase와 정확히 일치시킴.
- 위험 요소: 식별자 가용성과 실제 등록 미확인. 예상 밖 수정 없음.
- 다음 작업: 사용자 등록 후 웹 인증 반환 주소 구성 확인. Jira 변경 없음.

## 2026-09-30 — Services ID 안내 작업 식별 기록 보완

<!-- codex-turn:01a0f0fe-6365-7df2-98cf-1d1b032a2823 -->

- 브랜치: develop.
- 작업 목표: Services ID 입력값 안내의 정확한 현재 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 제안 Description/Identifier를 사용하고 Firebase 서비스 ID와 일치시키도록 안내한 결과 기록. 과거 항목 수정 없이 추가.
- 테스트와 결과: git diff --check 수행. 안내만 수행하여 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, 외부 설정 변경 없음.
- 결정사항: 앱 Bundle ID와 Services ID 구분 유지.
- 위험 요소: 실제 등록 및 식별자 가용성 미확인. 예상 밖 코드 변경 없음.
- 다음 작업: 사용자 등록 후 반환 주소 구성 확인. Jira 변경 없음.

## 2026-09-30 — Services ID 생성 완료 확인 및 웹 인증 설정 안내

- 브랜치: develop.
- 작업 목표: 사용자 생성 완료 확인 및 다음 설정 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Apple Services ID 목록에서 ToTeacher Firebase Login 및 com.toteacher.app.firebase 생성 확인. 편집 화면을 열어 Sign In with Apple 미선택 및 Configure 비활성 확인. 사용자에게 활성화 후 Primary App ID·Firebase 도메인·반환 URL 설정 안내.
- 테스트와 결과: 브라우저 읽기 확인, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 입력·설정 저장 없음, 비밀정보 비기록.
- 결정사항: Services ID 생성은 완료됐으나 웹 로그인 구성은 미완료.
- 위험 요소: 웹 인증 설정 저장 및 Firebase provider 연결·실제 로그인 미검증. 예상 밖 수정 없음.
- 다음 작업: 사용자 웹 인증 구성 후 저장 상태 점검. Jira 변경 없음.

## 2026-09-30 — Services ID 생성 확인 작업 식별 기록 보완

<!-- codex-turn:01a0f0ff-50d2-7750-9df0-efcbfefaf975 -->

- 브랜치: develop.
- 작업 목표: 이번 Services ID 생성 확인의 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Services ID 생성 및 Sign In with Apple 미활성 상태 확인 결과 기록. 웹 인증 구성과 최종 저장 절차 안내.
- 테스트와 결과: git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 외부 설정 저장 없음.
- 결정사항: 생성 완료와 웹 인증 설정 완료를 구분.
- 위험 요소: 웹 구성 저장·Firebase 연결·로그인 검증 미완료. 예상 밖 수정 없음.
- 다음 작업: 사용자 웹 구성 저장 후 확인. Jira 변경 없음.

## 2026-09-30 — Apple 웹 인증 Next 비활성 원인 확인

- 브랜치: develop.
- 작업 목표: Services ID 웹 인증 설정 Next 비활성 원인 진단.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Web Authentication Configuration의 No App ID is available 표시 및 Next disabled 확인. 입력 도메인/반환 URL은 Firebase 기본 auth handler와 일치. 선택 가능한 Primary App ID 부재가 차단 원인임을 안내하고 App ID의 Sign In with Apple primary 구성 및 최종 저장 확인 요청.
- 테스트와 결과: 브라우저 실조회, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 입력·저장·권한 변경 없음, 비밀정보 비기록.
- 결정사항: URL 수정 대신 같은 팀의 Primary App ID 등록 상태 점검 우선. 앞선 체크 상태만으로 저장 완료를 보장하지 않음.
- 위험 요소: Primary 부재의 근본 원인은 미확정; App ID 설정 저장 누락 또는 반영 지연 등 확인 필요. 예상 밖 수정 없음.
- 다음 작업: App ID primary 설정 최종 저장 후 Services ID Configure 재진입 및 선택 가능 여부 확인. Jira 변경 없음.

## 2026-09-30 — Apple Next 비활성 진단 작업 식별 기록 보완

<!-- codex-turn:01a0f100-fed9-77c2-8c77-41f6dc7f8124 -->

- 브랜치: develop.
- 작업 목표: 이번 Apple Next 비활성 진단의 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 올바른 도메인/반환 URL 입력과 No App ID is available 표시 확인 결과 기록. Primary App ID 구성 및 최종 저장 점검 안내.
- 테스트와 결과: git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 외부 설정 변경 없음.
- 결정사항: 주소 수정이 아닌 Primary App ID 가용성 확인이 다음 단계.
- 위험 요소: 저장 누락·반영 지연 등 근본 원인은 미확정. 예상 밖 수정 없음.
- 다음 작업: 사용자 App ID 저장 후 Services ID 선택 목록 재확인. Jira 변경 없음.

## 2026-09-30 — Apple Services ID 웹 인증 저장 확인

- 브랜치: develop.
- 작업 목표: 사용자 저장 후 Primary App ID 및 웹 인증 주소 검증.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Services ID 목록에서 편집 화면을 다시 열어 Sign In with Apple 활성 확인. Configure에서 Primary App ID가 com.toteacher.app으로 선택됨 확인. Website URLs 목록에 Firebase 기본 도메인과 /__/auth/handler 반환 주소 모두 존재 확인. 읽기 확인 후 Cancel로 모달 닫음.
- 테스트와 결과: 브라우저 저장 상태 재조회 성공, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 설정 변경·키 생성 없음, 비밀정보 비기록.
- 결정사항: Primary 부재 문제 해소 및 Services ID 웹 인증 설정 저장 확인 완료. Apple 로그인 전체 완료와는 구분.
- 위험 요소: Apple 로그인용 키·Firebase provider 구성·Identity 활성화·실로그인 미검증. 예상 밖 수정 없음.
- 다음 작업: Apple 로그인용 키 준비 후 Firebase 구성 및 테스트. Jira 변경 없음.

## 2026-09-30 — Apple 웹 인증 저장 확인 작업 식별 기록 보완

<!-- codex-turn:01a0f103-c0cf-7913-918a-32eb41e94a8c -->

- 브랜치: develop.
- 작업 목표: Apple 웹 인증 저장 확인의 현재 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Services ID 재조회에서 로그인 활성, Primary App ID 선택, Firebase 도메인 및 반환 주소 등록을 확인한 결과 기록. 다음 키 준비 절차 안내.
- 테스트와 결과: git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 외부 설정 변경·키 생성 없음.
- 결정사항: Services ID 구성 저장 확인 완료와 실제 로그인 검증 완료를 구분.
- 위험 요소: 로그인용 키·Firebase 연결·Identity 활성화 및 실제 인증 미검증. 예상 밖 수정 없음.
- 다음 작업: Apple 로그인용 키 준비 후 Firebase 연결 검증. Jira 변경 없음.

## 2026-09-30 — Apple 로그인 키 등록 전 구성 확인

- 브랜치: develop.
- 작업 목표: 현재 키 설정 그대로 등록 가능한지 읽기 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 등록 화면에서 로그인용 이름 및 Sign in with Apple만 선택 확인. Edit에서 Primary App ID com.toteacher.app 및 그룹 내 com.toteacher.app.firebase 확인 후 Back으로 복귀. 추가 기능은 미선택 유지.
- 테스트와 결과: 브라우저 구성 확인, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 키 생성·권한 변경·외부 저장 없음, 비밀정보 비기록.
- 결정사항: 현재 선택 구성이 Firebase Apple 로그인 목적과 일치하며 사용자 Continue/Register 후 안전한 키 다운로드 단계로 진행 가능.
- 위험 요소: 키 실제 발급·Firebase 연결·실로그인 미완료. 예상 밖 수정 없음.
- 다음 작업: 사용자 키 발급 후 Firebase provider 설정. Jira 변경 없음.

## 2026-09-30 — Apple 키 구성 확인 작업 식별 기록 보완

<!-- codex-turn:01a0f106-82c6-7590-8c0f-4d1253eeda65 -->

- 브랜치: develop.
- 작업 목표: Apple 키 등록 전 구성 확인의 현재 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Sign in with Apple만 선택되고 Primary App ID 및 Firebase Services ID가 연결된 설정 확인 결과 기록. 사용자 등록·다운로드 절차와 안전한 보관 안내.
- 테스트와 결과: git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 키 생성·외부 설정 변경 없음.
- 결정사항: 확인된 구성이 로그인 목적과 일치함.
- 위험 요소: 실제 키 발급·Firebase 연결·로그인 미검증. 예상 밖 수정 없음.
- 다음 작업: 사용자 발급 후 Firebase 연결 확인. Jira 변경 없음.

## 2026-09-30 — Apple 키 다운로드 후 Firebase 연결 안내

<!-- codex-turn:01a0f107-a3fc-7dd3-b6e0-a66557eb9cce -->

- 브랜치: develop.
- 작업 목표: 사용자 키 다운로드 완료 보고 후 Firebase Apple provider 입력 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Firebase Authentication Apple의 서비스 ID, 팀 ID, 키 ID 및 비공개 키 입력 위치 설명. 사용자 직접 입력과 안전한 파일 보관 안내; 다운로드 파일을 열거나 원문을 수집하지 않음.
- 테스트와 결과: 안내만 수행. 코드 변경 없어 Gradle 미실행; git diff --check 수행.
- 유지한 계약: 비밀정보 비기록, 외부 설정·권한 변경 없음.
- 결정사항: 다운로드 완료는 사용자 보고 기준이며 Firebase 저장 및 실제 로그인 완료와 구분.
- 위험 요소: provider 저장·Identity 활성화·실제 Apple 인증 미검증. 예상 밖 수정 없음.
- 다음 작업: 사용자 Firebase 저장 후 구성 상태 및 테스트 준비 확인. Jira 변경 없음.

## 2026-09-30 — Firebase Apple 연결 안내 작업 식별 기록 보완

<!-- codex-turn:01a0f107-f2b2-7e33-bf9b-02b2eff722a5 -->

- 브랜치: develop.
- 작업 목표: 다운로드 후 Firebase 연결 안내의 정확한 현재 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 다운로드 완료 보고와 Firebase Apple provider 입력 안내 결과를 기록. 과거 기록은 수정하지 않음.
- 테스트와 결과: git diff --check 수행. 코드 변경 없는 안내로 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, 다운로드 파일 미열람, 외부 설정 변경 없음.
- 결정사항: Firebase 저장과 실제 인증 성공은 아직 확인하지 않음.
- 위험 요소: provider 저장·Identity 활성화·실로그인 미검증. 예상 밖 수정 없음.
- 다음 작업: 사용자 Firebase 저장 후 구성 상태 확인. Jira 변경 없음.

## 2026-09-30 — Firebase Apple provider 저장 상태 확인

<!-- codex-turn:01a0f10b-51f2-7352-a6be-256d9c0119dc -->

- 브랜치: develop.
- 작업 목표: 사용자 등록 후 Firebase Apple 연결 설정 저장 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Chrome Firebase Apple 활성 상태 및 재개방한 설정에서 서비스 식별자 일치, 팀/키 식별자와 비공개 키 필드 입력 존재 확인. 원문 출력 없이 존재 여부만 조회하고 저장 없이 취소. 서버 설정의 Apple 기본 비활성 확인.
- 테스트와 결과: UI 저장 항목 재조회 성공 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, 외부 설정·배포 변경 없음.
- 결정사항: Firebase 구성 저장 확인 완료이나 키 유효성과 Apple 실제 로그인 성공은 아직 미검증. 배포 환경의 활성 플래그는 별도 확인 필요.
- 위험 요소: 현재 ECS 플래그 미조회, 프론트 Apple 인증 및 Identity 교환 미검증. 예상 밖 수정 없음.
- 다음 작업: 테스트 Identity 활성 설정 확인 후 필요한 변경 승인 및 실제 Apple 로그인 검증. Jira 변경 없음.

## 2026-09-30 — 테스트 Identity Apple 활성 플래그 실조회

- 브랜치: develop.
- 작업 목표: 배포된 테스트 Identity Apple 설정 확인 및 비활성 시 활성화 준비.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: ECS 테스트 서비스가 사용하는 tosunsaeng-identity-test:5에서 FIREBASE_APPLE_ENABLED=false 확인. Firebase 인증/Google/전화 활성 및 Kakao 비활성 확인. 새 개정에서 Apple 플래그만 true로 변경하고 테스트 서비스에 배포할 범위를 안내.
- 테스트와 결과: AWS 실화면 조회, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 기존 이미지·Secret 참조·JWT·병합 이벤트·운영 서비스 유지 예정. 실제 변경 및 배포 없음.
- 결정사항: 새 로그인 제공자 수용은 보안상 인증 허용 범위 변경이므로 브라우저 적용 직전 사용자 확인 요청.
- 위험 요소: 활성화/재배포 및 Apple E2E는 미완료. 예상 밖 수정 없음.
- 다음 작업: 적용 승인 후 Apple 플래그 하나만 변경한 새 테스트 개정 배포 및 안정성 확인. Jira 변경 없음.

## 2026-09-30 — 테스트 Apple 플래그 조회 작업 식별 기록 보완

<!-- codex-turn:01a0f10d-5828-7030-8ee9-c3e92c22eba7 -->

- 브랜치: develop.
- 작업 목표: 테스트 Identity Apple 활성 설정 조회의 현재 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 테스트 task definition test:5에서 Apple 비활성을 실조회한 결과 및 Apple 플래그만 활성화할 적용 직전 승인 대기 상태 기록.
- 테스트와 결과: git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 운영 및 테스트 배포 변경 없음.
- 결정사항: 승인 후 테스트 서비스에만 설정 변경 적용.
- 위험 요소: 활성화·재배포·Apple 실제 로그인 미완료. 예상 밖 수정 없음.
- 다음 작업: 사용자 적용 승인 후 새 테스트 개정 배포와 안정성 확인. Jira 변경 없음.

## 2026-09-30 — 테스트 Identity Apple 로그인 활성화 배포 완료

- 브랜치: develop.
- 작업 목표: 사용자 적용 승인 후 테스트 Identity의 Apple 로그인만 활성화하고 정상 배포 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 서버 코드 및 기존 사용자 변경은 수정하지 않음.
- 수행 내용: AWS 콘솔에서 test:5 기반 새 개정 작성, 폼 입력 전후 비교로 FIREBASE_APPLE_ENABLED false→true 한 항목 변경 확인. tosunsaeng-identity-test:6 생성 후 tosunsaeng-staging-cluster의 tosunsaeng-identity-test-service에 배포. 이미지 commit 88ff5bedc1ee661ccd38a8a1d2c4dbf3b9f03f2d·Secret 참조·JWT·병합 이벤트·로그 설정 유지, 원하는 태스크1 유지. 운영 미변경.
- 테스트와 결과: ECS 배포 성공 및 running1/pending0 확인. 신규 태스크의 2026-09-30T06:48:35Z Started IdentityApplication 로그 확인. 로드밸런서 조회 시 정상2/비정상0(이전 태스크 정리 구간) 확인. HTTPS health 브라우저 직접 조회는 클라이언트 차단으로 미완료. git diff --check 수행. 설정만 배포하여 Gradle 미실행.
- 유지한 계약: Firebase ID Token→Identity 인증 계약 유지, Kakao OFF 및 기존 Google/전화 상태 유지. 비밀정보 비기록. 커밋·push 없음.
- 결정사항: 테스트 Apple 수용 설정 활성화 완료. workflow가 현재 서비스 task definition을 가져오는 구조임을 확인했으며 다음 배포 시 revision6 설정 유지 필요. 초기 task definition 초안은 과거 구성으로 자동 배포 입력이 아니므로 수정하지 않음.
- 위험 요소: Apple 실제 인증·가입/로그인·토큰 발급 E2E 미실행. 배포 성공과 사용자 인증 성공을 구분. 기존 미커밋 변경 보존, 예상 밖 로컬 변경 없음.
- 다음 작업: 프론트 또는 테스트 도구에서 실제 Apple 로그인 후 Firebase 인증 및 Identity 교환 검증. Jira 변경 없음.

## 2026-09-30 — 테스트 Apple 활성화 배포 작업 식별 기록 보완

<!-- codex-turn:01a0f10f-31d8-7c73-be2e-e6c742812e36 -->

- 브랜치: develop.
- 작업 목표: 이번 테스트 Apple 활성화 배포의 정확한 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 승인된 Apple 플래그만 활성화한 테스트 revision6 배포 및 정상 기동 확인 결과 기록. 과거 항목은 변경하지 않음.
- 테스트와 결과: ECS 배포 성공/running1/pending0 및 신규 시작 로그 확인 결과 유지. 기록 보완 후 git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 운영·이미지·Secret·다른 설정 유지, 비밀정보 비기록. 이번 보완에서 추가 외부 변경 없음.
- 결정사항: 테스트 배포 완료와 실제 Apple 인증 검증을 구분.
- 위험 요소: Apple 로그인 및 Identity 토큰 발급 E2E 미실행, 직접 HTTPS health 조회 미완료. 예상 밖 수정 없음.
- 다음 작업: 실제 Apple 인증 및 Identity 교환 검증. Jira 변경 없음.

## 2026-09-30 — 로컬 통합 테스트 화면 Apple 로그인 추가

<!-- codex-turn:01a0f119-e71e-7620-b3ef-121870f401e5 -->

- 브랜치: develop. Jira: 이번 요청에 연결된 이슈 없음, Jira 변경 없음.
- 작업 목표: 기존 로컬 테스트 화면에서 Apple 로그인부터 기존 회원가입·병합·챌린지 흐름을 수동 검증할 수 있도록 확장.
- 변경 파일: 별도 프로젝트 `/Users/msde76/tosunsaeng-integration-test`의 app.js, index.html, apple.test.mjs, README.md 및 Identity docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Firebase Apple OAuthProvider와 email/name scope, Apple 로그인/동일 UID 재인증, 제공자별 안내 및 Google/Apple 전환 방지 추가. 명시적 로컬 초기화 후 제공자 전환. 동일 계정 재인증은 Guest/MEMBER/녹음 상태 유지. 기존 테스트 기능 유지.
- 실행한 테스트와 결과: Node app/apple/merge/challenge/server 테스트 7개 통과(최초 sandbox 포트 권한 오류 후 승인된 재실행). Identity ./gradlew clean test 통과(캐시 권한 승인 후). git diff --check 통과. 127.0.0.1:4173 실행 확인. 새 Chrome 탭 UI 조회는 클라이언트 접근 차단으로 미완료, 기존 인증 탭 새로고침하지 않음.
- 유지한 계약: Identity API·JWT·보안 정책·서버 코드·배포 미변경, LC/챌린지 테스트 코드는 별도 프로젝트 유지. 비밀키 입력 및 토큰 원문 표시 없음. 커밋/push 없음.
- 결정사항: 자동 제공자 연결/병합하지 않음. Apple 실제 로그인은 사용자 인증 후 수동 검증. 새 화면 로딩 시 메모리 세션 초기화 주의.
- 위험 요소: Mock 통과는 실제 Apple OAuth 성공을 의미하지 않음. 신규 Apple 계정에 기존 Firebase 계정의 전화번호를 연결하면 충돌 가능. 브라우저 화면 QA 및 실제 Android/iOS 로그인 미확인. 기존 미커밋 사용자 변경 보존, 예상 밖 수정 없음.
- 다음 작업: Chrome에서 localhost:4173 새 화면을 열고 Firebase 초기화→Apple 인증→Identity 교환, 신규 계정은 전화번호/동의/가입, 기존 MEMBER는 프로필/재발급 확인. 배포 전 서버 Apple 플래그 유지 및 실제 앱 SDK 흐름 별도 검증.

## 2026-09-30 — Apple 테스트 PROVIDER_RELINK_REQUIRED 원인 조사

<!-- codex-turn:01a0f125-d451-7be2-becd-0b382bed97af -->

- 브랜치: develop. Jira: 요청에 연결된 키 없음, 변경 없음.
- 작업 목표: 사용자가 보고한 HTTP 409 PROVIDER_RELINK_REQUIRED 의미와 대응 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. FirebaseExchangeService.ensureNoSocialIdentityOwner 및 ProviderChangeGuard.validatePrincipal/authenticate에서 기존 바인딩의 로그인 제공자 승인 누락 또는 차단 시 발생함을 확인. 로컬 테스트 앱은 signInWithPopup/reauthenticateWithPopup만 지원하며 공통 provider link 기능은 미구현임을 확인.
- 실행한 테스트와 결과: 소스 및 frontend-firebase-auth-integration-guide 공통 link 계약 정적 조회, git diff --check. 실행 코드 변경 없는 분석으로 자동 테스트 미실행.
- 유지한 계약: Firebase 인증과 Identity 로그인 제공자 승인을 구분, prepare/start/SDK link/대상 재인증/complete 필수 흐름 유지. DB·Firebase 계정·배포·토큰·보호 설정 미변경.
- 결정사항: 코드상 가능한 원인과 실제 계정 원인 확정을 구분. 기존 승인 SNS로 인증 후 정식 연결 필요하며 자동 계정 삭제·강제 승인하지 않음.
- 위험 요소: 실제 발생 요청 경로·배포 버전·계정 승인 및 차단 상태 미조회. 현재 오류만으로 과거 연결 해제를 단정할 수 없음. 기존 사용자 변경 보존, 예상 밖 수정 없음.
- 다음 작업: 실패 API/계정 연결 상태를 확인하고 필요 시 사용자 요청에 따라 테스트 화면에 공통 SNS 연결 기능 구현. 비밀정보 공유 불필요.

## 2026-09-30 — 새 Apple 계정 가입 테스트 안내

- 브랜치: develop. Jira: 연결된 이슈 없음.
- 작업 목표: 기존 provider 연결 오류와 독립된 신규 계정 테스트 방법 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 로컬 초기화 후 미연결 Apple 계정으로 로그인→Identity 가입 준비→ENROLLMENT_REQUIRED 확인→미사용 테스트 전화번호 연결 및 동의·가입 안내. 동일 Apple 계정으로 새로 로그인해도 새 서버 계정이 되지 않음을 설명.
- 실행한 테스트와 결과: 직전 소스/계약 확인 결과 기반 안내. 코드 변경 없어 테스트 미실행, git diff --check 수행.
- 유지한 계약: 자동 계정 삭제·Firebase unlink·서버 승인 우회 없음. 로컬 메모리 초기화와 서버 계정 삭제 구분.
- 결정사항: 기존 계정을 보존하며 별도 신규 계정으로 테스트. Apple 이메일 숨기기나 이메일 변경을 새 계정 생성 수단으로 안내하지 않음.
- 위험 요소: 신규 Apple 계정 보유 여부 및 전화번호 중복 상태 미확인. 기존 사용자 변경 보존, 예상 밖 수정 없음.
- 다음 작업: 사용자 직접 Apple 인증 후 enrollment 응답 확인. 외부 설정·배포 변경 없음.

## 2026-09-30 — 신규 Apple 가입 안내 작업 식별 기록 보완

<!-- codex-turn:01a0f127-1fa0-7473-a14d-016fb48cb2e1 -->

- 브랜치: develop.
- 작업 목표: 이번 신규 Apple 계정 가입 안내의 정확한 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 별도 미연결 Apple 계정으로 인증 후 신규 enrollment 및 전화번호·동의·가입 절차를 안내한 결과 기록. 과거 항목 보존.
- 테스트와 결과: git diff --check 수행. 문서 기록만 보완하여 실행 테스트 미실행.
- 유지한 계약: 로컬 초기화는 서버 계정 삭제가 아님. 제공자 연결 승인 우회·외부 상태 변경·비밀정보 기록 없음.
- 결정사항: 같은 Apple 계정 재로그인을 신규 계정 생성으로 취급하지 않음.
- 위험 요소: 실제 신규 가입 미검증, 예상 밖 수정 없음.
- 다음 작업: 사용자 직접 인증 후 신규 가입 응답 확인. Jira 변경 없음.

## 2026-09-30 — 현재 Apple 계정 사용 가능 여부 설명 보완

- 브랜치: develop. Jira: 연결된 이슈 없음.
- 작업 목표: 현재 로그인한 계정을 반드시 교체해야 하는지에 대한 오해 해소.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 안내만 수행. Firebase 인증 성공과 Identity의 provider 승인 상태를 구분하고 별도 Apple 계정은 신규 가입 검증용 선택지임을 명시.
- 테스트와 결과: 앞선 소스 조사 근거 활용. 코드 변경 없어 실행 테스트 미실행, git diff --check 수행.
- 유지한 계약: 현재 로그인 세션·DB·연결 상태 변경 및 보호 조건 우회 없음.
- 결정사항: 현재 계정 상태를 확인하기 전에 다른 계정 사용이나 기존 계정 삭제를 필수 조치로 제시하지 않음.
- 위험 요소: 실제 계정의 승인 누락/차단 원인 미확정. 예상 밖 수정 없음.
- 다음 작업: 현재 계정의 Firebase/Identity 연결 상태를 읽기 전용으로 확인하여 정식 연결 경로 결정.

## 2026-09-30 — 현재 Apple 계정 유지 안내 식별 기록 보완

<!-- codex-turn:01a0f128-0010-7923-bda5-c647bc222d71 -->

- 브랜치: develop.
- 작업 목표: 현재 Apple 계정 사용 안내의 정확한 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 계정 교체가 필수는 아니며 Firebase 인증과 Identity 승인이 별개임을 안내한 결과 기록. 과거 기록 보존.
- 테스트와 결과: git diff --check 수행. 문서 보완만 하므로 실행 테스트 미실행.
- 유지한 계약: 계정 삭제·세션 변경·승인 우회·비밀정보 기록 없음.
- 결정사항: 기존 계정 상태를 보존하고 연결 상태 확인 후 대응.
- 위험 요소: 실제 계정의 승인 누락/차단 원인 미확정, 예상 밖 수정 없음.
- 다음 작업: 현재 계정의 연결 상태 확인. Jira 변경 없음.

## 2026-09-30 — Chrome Apple 로그인 차단 단계 실조회

- 브랜치: develop. Jira: 연결된 이슈 없음.
- 작업 목표: 사용자가 로그인한 실제 테스트 탭에서 차단 위치 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Chrome 기존 localhost 테스트 화면의 검증표에서 Apple 인증 성공과 exchange HTTP 409 PROVIDER_RELINK_REQUIRED 확인. 전화번호 연결/회원가입/프로필/재발급/LC 버튼 비활성 확인. app.js의 exchange 요청이 POST /api/v1/auth/firebase/exchange임을 소스로 대조.
- 테스트와 결과: 기존 탭 접근성 화면 읽기 및 요청 매핑 정적 확인. 새로고침·버튼 클릭·네트워크 재요청 없이 수행. git diff --check 확인. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 현재 인증 메모리·계정·DB·배포 상태 보존. 비밀정보 조회/기록 없음.
- 결정사항: Firebase 인증 성공 이후 Identity 로그인/가입 준비 단계 차단으로 확정. Apple 로그인 자체 실패나 전화번호 단계 실패로 해석하지 않음.
- 위험 요소: 해당 계정의 승인 누락인지 과거 차단인지 화면만으로 구분 불가. 실제 배포 코드/DB 대조 미완료. 예상 밖 수정 없음.
- 다음 작업: 필요 시 테스트 Identity의 Firebase 바인딩·SocialIdentity·provider 차단 상태를 읽기 전용 대조해 원인 확정. 외부 변경 없음.

## 2026-09-30 — Chrome 차단 단계 조회 식별 기록 보완

<!-- codex-turn:01a0f129-008d-79c1-bbd4-07ae4be59530 -->

- 브랜치: develop.
- 작업 목표: 현재 Chrome 테스트 탭 실조회 작업의 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Apple Firebase 인증 성공, Identity exchange HTTP 409 PROVIDER_RELINK_REQUIRED 및 후속 단계 비활성 확인 결과 기록. 과거 항목 보존.
- 테스트와 결과: 화면 읽기 및 소스 요청 매핑 확인 결과 유지. git diff --check 수행. 문서 보완만으로 실행 테스트 미실행.
- 유지한 계약: 탭 새로고침·로그아웃·재요청·계정 변경 없음. 비밀정보 비기록.
- 결정사항: 차단 단계는 확인했으나 실제 계정 승인 누락/차단 원인은 미확정으로 구분.
- 위험 요소: 배포 코드·DB 대조 미완료. 예상 밖 수정 없음.
- 다음 작업: 테스트 계정 연결 상태 읽기 전용 확인. Jira 변경 없음.

## 2026-09-30 — Apple 로그인 승인 불일치 원인 실확인

<!-- codex-turn:01a0f12e-6c76-7680-9a3c-d53f892f18cd -->

- 브랜치: develop. Jira: 요청에 연결된 이슈 없음, 변경 없음.
- 작업 목표: Apple 로그인 후 Identity exchange 409의 실제 데이터 원인 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Atlas 테스트 프로젝트의 to-teacher-identity-test에서 firebase_identities 전체 1건과 social_identities 전체 1건 대조. 동일 계정의 승인 provider는 GOOGLE뿐이고 APPLE 없음. Firebase Authentication 사용자 전체 1건에서 해당 UID에 Google/Apple/Phone 연결 확인. 테스트 DB 전체 21개 컬렉션 목록에 auth_method_change_controls 없음. 이전 확인 배포 commit 88ff5bed의 ProviderChangeGuard와 현재 소스에서 현재 로그인 provider/subject 승인 누락 시 해당 409 반환 확인.
- 테스트와 결과: 실제 Firebase/Atlas 읽기 전용 화면 대조 및 git show 정적 검증, git diff --check. 코드 변경 없어 실행 테스트 미실행. 로그인 탭 새로고침·로그아웃·exchange 재요청 없음.
- 유지한 계약: 계정/DB/인증 제공자/접근 권한/배포 미변경. 개인 식별값·자격증명·토큰은 문서에 기록하지 않음. 운영 DB 미조회.
- 결정사항: 신규 Apple 계정이 아니라 기존 Firebase 계정에 Apple이 연결돼 있고 Identity 승인 정보가 따라오지 않은 상태로 판정. 단순 재로그인이나 계정 교체를 필수 해결책으로 제시하지 않음. 서버 보호 조건 우회/DB 강제 삽입하지 않음.
- 위험 요소: Firebase 연결이 형성된 경위는 미확인. 이미 연결된 Apple을 정식 provider link 흐름으로 승인하는 복구 조건은 추가 검토 필요. 최신 배포 revision 재조회는 미수행. 기존 사용자 변경 보존, 예상 밖 수정 없음.
- 다음 작업: 사용자가 수정을 요청하면 공통 provider link의 기존 Firebase 연결 처리·복구 조건을 확인한 뒤 테스트 화면에 해당 흐름 구현 및 검증. 실제 동의/연결 변경은 별도 확인.

## 2026-09-30 — Apple 인증과 Identity 연결 승인 차이 설명

<!-- codex-turn:01a0f131-3fdb-7839-a620-1b01d178bf1e -->

- 브랜치: develop.
- 작업 목표: 사용자에게 현재 Apple 로그인 차단 이유를 쉬운 용어로 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 앞서 확인한 Firebase Google/Apple/Phone 연결과 Identity GOOGLE만 등록된 상태를 바탕으로 본인 인증과 서비스 계정 로그인 승인을 구분해 설명.
- 테스트와 결과: 기존 실조회 결과 기반 안내, git diff --check 수행. 코드 변경 없어 실행 테스트 미실행.
- 유지한 계약: 계정·세션·DB·외부 설정 변경 및 승인 우회 없음. 비밀정보 비기록.
- 결정사항: 새 Apple 계정 사용이나 기존 계정 삭제가 필수라는 오해 해소. Firebase 연결 생성 경위는 단정하지 않음.
- 위험 요소: 정식 연결 복구 흐름의 기존 Firebase 연결 처리 조건 미검증. 예상 밖 수정 없음.
- 다음 작업: 요청 시 정식 연결 절차 및 테스트 화면 보완 검토. Jira 변경 없음.

## 2026-09-30 — 로그인 승인 설명 작업 식별 기록 보완

<!-- codex-turn:01a0f131-7898-7bf2-8879-4fbe3ec015e4 -->

- 브랜치: develop.
- 작업 목표: 이번 설명 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 Firebase 인증 성공과 Identity 제공자 승인 누락을 설명한 작업의 식별 기록 추가.
- 테스트와 결과: git diff --check 수행. 문서 기록 보완만으로 실행 테스트 미실행.
- 유지한 계약: 계정·세션·DB·설정 미변경, 비밀정보 비기록.
- 결정사항: 기존 계정에 Apple 연결을 원하는지는 사용자 의도 확인이 필요하며 실제 변경하지 않음.
- 위험 요소: 연결 복구 조건 미검증. 예상 밖 수정 없음.
- 다음 작업: 사용자 의도에 맞게 정식 연결 또는 별도 가입 흐름 검토. Jira 변경 없음.

## 2026-09-30 — Apple 전역 활성화와 계정 연결 검사 구분

<!-- codex-turn:01a0f135-a035-71a1-97e4-8737e1098e06 -->

- 브랜치: develop.
- 작업 목표: 서버 Apple 기능을 켰는데도 409가 발생하는 이유를 코드 근거로 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. FirebaseAdminAuthenticationVerifier의 validateProviderPolicy와 후속 ProviderChangeGuard.validatePrincipal 호출, appleEnabled 참조 확인. 전역 기능 활성화가 기존 회원의 SocialIdentity 생성이나 승인 누락 검사를 대체하지 않음을 설명.
- 테스트와 결과: 소스 정적 조회 및 git diff --check. 코드 변경 없는 설명으로 실행 테스트 미실행.
- 유지한 계약: Apple 전역 설정·계정·DB·배포 미변경, 비밀정보 비기록.
- 결정사항: 앞선 허용이라는 표현을 전역 기능 활성화와 계정별 연결 확인으로 명확히 구분. 운영자가 회원마다 수동 허용해야 한다는 의미가 아님.
- 위험 요소: 이미 Firebase에 연결된 제공자 복구 흐름 검증은 남음. 예상 밖 수정 없음.
- 다음 작업: 요청 시 정식 계정 연결 흐름의 현재 데이터 상태 처리 확인 및 테스트 UI 보완. Jira 변경 없음.

## 2026-09-30 — 기존 Firebase Apple 연결의 정식 복구 가능성 조사

- 브랜치: develop. Jira: 연결된 이슈 없음.
- 작업 목표: 전역 Apple 활성화 이후 계정 연결 불일치의 생성 경위와 기존 link API 복구 가능성 점검.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 분석만 수행. ProviderLinkService.prepare에서 원격 연결이 있으면 ownedSocial로 기존 승인 레코드를 필수 조회함을 확인. 현재 원격 APPLE 연결/로컬 APPLE 없음 상태는 link 기능 활성화 시에도 SOCIAL_IDENTITY_CONFLICT 발생 조건. prepare 이전 원격 연결을 소급 승인하지 않는 주석과 start의 원격 연결 거절 조건 확인. 해당 서비스는 이전 확인 배포 commit 88ff5bed와 diff 없음. 로컬 테스트 화면은 Apple signInWithPopup/reauthenticateWithPopup만 수행하고 SNS 연결 호출은 없음; linkWithCredential은 전화번호용.
- 테스트와 결과: 소스 및 배포 commit 비교, git diff --check. 코드 변경 없어 실행 테스트 미실행. 실제 계정 mutation/API 재실행 없음.
- 유지한 계약: 기능 활성화와 계정 연결 검증 분리, 소급 자동 승인·DB 수동 삽입·Firebase 해제 미실행. 비밀정보 비기록.
- 결정사항: 현재 상태는 테스트 UI에 연결 버튼만 추가해 해결할 수 없으므로 앞선 단순화 설명 정정. 연결 생성 경위는 자동 연결 등 가능성만 있으며 확정하지 않음. 안전한 불일치 복구 흐름 검토가 선행되어야 함.
- 위험 요소: Firebase 연결 생성 당시 이벤트/로그 미확보. link 플래그 현재 배포값 미조회, 따라서 실제 prepare HTTP 결과는 미실행 상태. 예상 밖 수정 없음.
- 다음 작업: 사용자 요청 시 기존/대상 SNS 소유권 확인 및 계정 충돌 방지를 포함하는 복구 설계와 테스트 계획 마련. 계정 삭제나 강제 연결 해제로 우회하지 않음.

## 2026-09-30 — Apple 연결 복구 조사 작업 식별 기록 보완

<!-- codex-turn:01a0f136-c432-7520-bb9f-0d575ca063ca -->

- 브랜치: develop.
- 작업 목표: 이번 연결 복구 조사 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 기존 link prepare의 승인 누락 거절 조건 및 UI만으로 복구되지 않는다는 조사 결과의 작업 식별자를 추가.
- 테스트와 결과: git diff --check 수행. 문서 보완만으로 실행 테스트 미실행.
- 유지한 계약: 계정·DB·서버 코드·설정 미변경, 비밀정보 비기록.
- 결정사항: 복구 설계 검토 전 강제 승인 또는 연결 해제하지 않음.
- 위험 요소: Firebase 연결 생성 경위 미확정, 실제 복구 미검증. 예상 밖 수정 없음.
- 다음 작업: 요청 시 안전한 불일치 복구 설계 및 테스트 계획 수립. Jira 변경 없음.

## 2026-09-30 — 다중 SNS 지원과 연결 불일치 구분

<!-- codex-turn:01a0f138-0bc6-7f57-9640-70143b10d2d3 -->

- 브랜치: develop.
- 작업 목표: 다중 SNS 로그인 허용 자체가 충돌 원인인지 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 분석만 수행. SocialIdentity의 provider/subject 고유 인덱스와 ProviderLinkService.complete의 대상 provider 추가/동일 provider 중복 검사 확인. 다른 SNS가 이미 등록돼 있다는 이유만으로 새 SNS를 거절하는 구조는 아님.
- 테스트와 결과: 코드 정적 조회 및 git diff --check. 코드 변경 없어 실행 테스트 미실행.
- 유지한 계약: 한 회원에 복수 SNS 연결 지원, 다른 계정 소유 또는 동일 provider 중복 방지 유지. 외부 데이터·설정 미변경.
- 결정사항: 현재 오류는 다중 SNS 자체 충돌이 아닌 Firebase/Identity 연결 상태 불일치로 설명. 정식 연결 흐름과 외부에서 먼저 생긴 연결의 복구 공백을 구분.
- 위험 요소: 최초 설계 전체 이력과 Firebase 연결 생성 경위는 미확정. 예상 밖 수정 없음.
- 다음 작업: 요청 시 기존 다중 SNS 지원을 유지하는 안전한 불일치 복구 설계 검토. Jira 변경 없음.

## 2026-09-30 — 다중 SNS 지원 설명 작업 식별 기록 보완

<!-- codex-turn:01a0f138-9261-75c3-9c98-e9fadd0c0848 -->

- 브랜치: develop.
- 작업 목표: 현재 설명 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 다중 SNS 지원과 계정 연결 정보 불일치를 구분한 설명의 작업 식별자를 추가.
- 테스트와 결과: git diff --check 수행. 문서 보완만으로 실행 테스트 미실행.
- 유지한 계약: 복수 SNS 지원 및 계정 충돌 방지 유지. 코드·DB·설정 미변경, 비밀정보 비기록.
- 결정사항: 현재 오류를 다중 SNS 기능 자체의 충돌로 단정하지 않음.
- 위험 요소: Firebase 연결 생성 경위와 불일치 복구 절차 미검증. 예상 밖 수정 없음.
- 다음 작업: 요청 시 안전한 불일치 복구 설계 검토. Jira 변경 없음.

## 2026-09-30 — Apple 로그인 차단 조건과 보안 목적 설명

- 브랜치: develop.
- 작업 목표: 차단의 직접 조건과 검사 목적을 구분하여 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. ProviderChangeGuard.validatePrincipal의 기존 바인딩 및 현재 provider/subject 승인 레코드 검사 재확인. 현재 APPLE 레코드 부재로 거절되는 상태와 과거 계정 차단을 구분.
- 테스트와 결과: 소스 정적 확인, git diff --check 수행. 설명 작업으로 실행 테스트 미실행.
- 유지한 계약: 외부 연결만으로 서비스의 승인/해제 절차 우회 금지. 코드·계정·설정 미변경, 비밀정보 비기록.
- 결정사항: 기능 OFF나 계정 정지가 아니라 승인된 연결 정보 부재에 의한 요청 거절로 설명. Firebase 연결 생성 경위는 단정하지 않음.
- 위험 요소: 실제 불일치 생성 원인과 복구 절차는 추가 확인 필요. 예상 밖 수정 없음.
- 다음 작업: 요청 시 연결 불일치 복구 설계 검토. Jira 변경 없음.

## 2026-09-30 — Apple 차단 조건 설명 작업 식별 기록 보완

<!-- codex-turn:01a0f13a-3ebc-7d83-b109-a9008978d8d2 -->

- 브랜치: develop.
- 작업 목표: 현재 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 수정하지 않고 Apple 승인 레코드 누락에 따른 거절 조건 설명의 작업 기록 추가.
- 테스트와 결과: git diff --check 수행. 문서 보완만으로 실행 테스트 미실행.
- 유지한 계약: 코드·계정·DB·외부 설정 미변경, 비밀정보 비기록.
- 결정사항: 확인한 차단 조건과 미확정인 불일치 생성 경위를 구분.
- 위험 요소: 실제 불일치 생성 경위와 복구 절차 미검증. 예상 밖 수정 없음.
- 다음 작업: 요청 시 연결 불일치 원인 및 복구 절차 추가 조사. Jira 변경 없음.

## 2026-09-30 — Firebase 신뢰된 제공업체 자동 연결 정책 조사

- 브랜치: develop.
- 작업 목표: Apple 연결이 Firebase에만 생기는 경로를 공식 동작과 코드로 대조.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 공식 https://firebase.google.com/docs/auth/users#verified_email_addresses 실조회로 동일 이메일의 신뢰된 제공업체 로그인 시 자동 연결, Apple의 신뢰된 제공업체 분류, Google의 Gmail 조건 확인. 로컬 app.js는 Apple signInWithPopup/reauthenticateWithPopup만 호출하고 명시적 SNS link나 Identity sync 호출이 없음을 확인. FirebaseSignupService는 신규 가입 때 연결 SNS를 준비하지만 exchange는 기존 바인딩의 미등록 SNS를 거절하고 자동 저장하지 않음. 기존 sync 역시 승인된 연결 검증만 수행.
- 테스트와 결과: 공식 문서 브라우저 조회·코드 정적 대조·git diff --check. 코드 변경 없어 실행 테스트 미실행. 계정 로그인/연결 재현 mutation 미수행.
- 유지한 계약: 계정·DB·배포·설정 미변경, 비밀정보 비기록. 원인 조사와 수정 권한 구분.
- 결정사항: Firebase 자동 연결이 현재 상태와 부합하는 가장 유력한 생성 경로이며, 앱에서 link API를 직접 호출하지 않아도 발생 가능. Identity의 명시적 연결 승인 모델이 이 경로를 처리하지 못하는 통합 공백 확인. 사용자 실수나 테스트 화면 연결 버튼 누락만으로 단정하지 않음.
- 위험 요소: 해당 로그인 당시 이벤트 로그 및 연결 전 snapshot 미확보로 이번 연결 순간의 직접 증명은 아님. 자동 연결 전체를 무조건 승인하면 기존 해제/차단 보호를 훼손할 수 있어 별도 복구 설계 필요. 예상 밖 수정 없음.
- 다음 작업: 요청 시 Firebase 자동 연결을 고려한 안전한 계정 연결/복구 정책 및 회귀 테스트 설계. Jira 변경 없음.

## 2026-09-30 — Firebase 자동 연결 정책 조사 식별 기록 보완

<!-- codex-turn:01a0f13c-7853-7823-af05-3a9850b7edc2 -->

- 브랜치: develop.
- 작업 목표: 이번 자동 연결 정책 조사 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 공식 Firebase 자동 연결 정책 및 Identity 명시적 승인 모델의 불일치 조사 결과 식별 기록 추가.
- 테스트와 결과: git diff --check 수행. 문서 보완만으로 실행 테스트 미실행.
- 유지한 계약: 계정·DB·서버 코드·설정 미변경, 비밀정보 비기록.
- 결정사항: 공식적으로 가능한 자동 연결 동작과 이번 개별 사건의 직접 증명을 구분.
- 위험 요소: 당시 이벤트 로그 미확보, 실제 연결 생성 순간 미확정. 예상 밖 수정 없음.
- 다음 작업: 요청 시 안전한 자동 연결 대응 및 복구 정책 설계. Jira 변경 없음.

## 2026-09-30 — Apple 연결 해제 후 재현 범위 확인

- 브랜치: develop.
- 작업 목표: 사용자 요청의 Apple 연결 해제와 재로그인 재현 범위를 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 전체 계정 삭제가 아닌 테스트 Firebase 사용자의 Apple provider만 해제하는 범위를 설명하고 적용 전 사용자 확인 요청. Google/전화번호/UID/Identity 회원/기록은 유지 대상.
- 테스트와 결과: 안내 및 기록만 수행, git diff --check. 실행 테스트·계정 변경 미수행.
- 유지한 계약: 기존 계정 및 인증 수단 보호, 비밀정보 비기록. 승인 전 unlink/사용자 삭제 없음.
- 결정사항: 재로그인 시 Firebase 자동 재연결과 동일 오류가 발생할 수 있으므로 해결이 아닌 재현 실험으로 설명. Apple 로그인을 다시 수행할 때 사용자 직접 인증 필요.
- 위험 요소: 연결 해제는 해당 로그인 수단을 제거하며 세션 영향 확인 필요. 실제 해제 방법·재현은 승인 후 검토. 예상 밖 수정 없음.
- 다음 작업: 범위 승인 후 지원되는 provider 해제 방법 확인 및 변경 전후 연결 상태 비교. Jira 변경 없음.

## 2026-09-30 — Apple 재현 실험 목적 및 콘솔 해제 기능 확인

- 브랜치: develop.
- 작업 목표: 오류 재현 요청에 맞춰 Apple provider만 해제 가능한 경로 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Firebase 테스트 사용자 메뉴 읽기 전용 조회, 전체 계정 삭제/비활성화 및 비밀번호 재설정만 표시됨을 확인. Apple 단독 해제는 콘솔 메뉴에서 제공되지 않으므로 Firebase SDK 기반 테스트 UI 경로 검토.
- 테스트와 결과: 실제 콘솔 메뉴 확인 및 git diff --check. unlink/로그인 재현은 미실행, 자동 테스트 미실행.
- 유지한 계약: Google·전화번호·UID·Identity 회원과 기록 보존, 전체 사용자 삭제 금지. 비밀정보 비기록.
- 결정사항: 해결 작업이 아닌 해제 전후 provider/UID 및 exchange 결과 비교 실험으로 범위 명확화. 실제 연결 변경 직전 확인 요청.
- 위험 요소: Apple 연결 해제는 인증 수단 변경이며 세션 영향 가능. 재로그인 시 사용자 직접 인증 필요. 예상 밖 수정 없음.
- 다음 작업: Apple 단독 해제 범위 확인 후 재현용 SDK 조작 준비 및 변경 전후 검증. Jira 변경 없음.

## 2026-09-30 — Apple 재현 실험 준비 작업 식별 기록 보완

<!-- codex-turn:01a0f13e-1388-7711-bdc3-0df9a4049ed8 -->

- 브랜치: develop.
- 작업 목표: 이번 재현 실험 준비 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 Firebase 콘솔의 Apple 단독 해제 메뉴 부재 및 실제 변경 전 범위 확인 대기 상태를 기록.
- 테스트와 결과: git diff --check 수행. 문서 보완만으로 실행 테스트 미실행.
- 유지한 계약: 계정·DB·코드·설정 미변경, 비밀정보 비기록. 전체 사용자 삭제 없음.
- 결정사항: Google·전화번호·회원 기록을 유지하는 Apple 연결 단독 해제 승인 후 재현 진행.
- 위험 요소: 실제 unlink와 재로그인 결과 미검증. 예상 밖 수정 없음.
- 다음 작업: 사용자 확인 후 지원되는 SDK 경로로 해제 전후 상태 및 동일 오류 재발 여부 검증. Jira 변경 없음.

## 2026-09-30 — 승인된 Apple 단독 해제 및 재로그인 재현 준비

- 브랜치: develop. Jira: 요청에 연결된 이슈 없음.
- 작업 목표: Apple 연결만 해제한 뒤 동일 계정 로그인으로 자동 연결 및 409 재발 검증.
- 변경 파일: 별도 `/Users/msde76/tosunsaeng-integration-test`의 app.js, index.html, apple.test.mjs, README.md 및 docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 재현 버튼에 테스트 프로젝트·Google 인증·Identity 로그인 성공·Google/전화/Apple 존재·Guest 병합 상태 없음 검사 추가. 확인 후 apple.com만 unlink, reload로 UID/나머지 provider 보존 및 Apple 부재 확인. 불명확한 결과에 자동 재해제하지 않도록 메모리 잠금. 로그아웃 후 Apple 로그인 시 동일 UID/자동 연결/기존 provider 유지 boolean 비교. 토큰/사용자 ID 원문 비출력.
- 실제 수행: 사용자 범위 승인에 따라 기존 오류 탭 새로고침 및 기존 공개 Firebase 웹 구성 적용. 기존 테스트 Google 계정 선택 후 Identity MEMBER 로그인 성공. 재현 확인창 승인 후 Apple 단독 해제 성공 표시와 UID/Google/전화 유지 확인. Apple 로그인 창 열었으나 직접 인증 필요하여 대기. 전체 사용자 삭제/Google·전화 해제 없음. 해제된 Apple 연결은 재인증/연결로 복구 가능한 로그인 수단이며 원인 재현 목적으로 분리함.
- 실행한 테스트와 결과: Node app/apple/merge/challenge 테스트 5개 통과(해제 취소, Apple만 unlink, 상태 보존, 로그인 후 자동 연결 Mock 검증 포함). 브라우저에서 신규 재현 섹션/Google 로그인/실제 해제 결과 확인. 새 탭 직접 이동은 클라이언트 차단됐으나 기존 탭 reload 성공. git diff --check. Identity 서버 코드 변경 없어 Gradle 재실행 생략.
- 유지한 계약: 정식 Identity provider API/보호 조건/DB 승인 레코드 미변경, 운영 미변경, 서버 배포 없음. 비밀정보 비기록, 기존 사용자 변경 보존. Identity 로그인에 따른 정상 세션 발급 외 서버 변경 없음.
- 결정사항: 실험 목적의 Firebase 단독 unlink이며 서비스용 해제 기능으로 사용하지 않음. 사용자 Apple 인증 후 동일 UID와 exchange 결과를 확인해야 재현 완료.
- 위험 요소: Apple 실제 재로그인 및 409 재발 미완료. 새로고침하면 비교 메모리 소실. 기존 서버 세션은 로컬 초기화로 폐기되지 않음. 예상 밖 수정 없음.
- 다음 작업: 사용자가 같은 Apple 계정으로 인증 완료 후 비교 결과 및 Identity exchange 확인. 배포 전 확인: 로컬 재현 버튼은 서비스 배포 대상 아님. Jira 댓글/상태 변경 없음.

## 2026-09-30 — Apple 단독 해제 재현 작업 식별 기록 보완

<!-- codex-turn:01a0f13f-c70e-7da3-9ab4-eb80e23fcf6f -->

- 브랜치: develop.
- 작업 목표: 이번 승인된 Apple 재현 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 로컬 재현 UI 추가·실제 Apple 단독 해제·사용자 재인증 대기 결과의 작업 식별 기록 추가.
- 테스트와 결과: 앞선 Node Mock 테스트 5개 통과 및 실제 해제 결과 확인 유지. 이번 문서 보완 후 git diff --check 수행, 실행 테스트 재실행 없음.
- 유지한 계약: 추가 계정·DB·코드·설정 변경 없음, 비밀정보 비기록.
- 결정사항: Apple 재로그인 후 자동 연결과 Identity 오류 확인 전까지 재현 완료로 보고하지 않음.
- 위험 요소: 사용자 Apple 인증 및 409 재발 미검증. 예상 밖 수정 없음.
- 다음 작업: 사용자 인증 완료 후 기존 페이지 비교 상태와 Identity exchange 결과 확인. Jira 변경 없음.

## 2026-09-30 — Apple 자동 재연결 및 Identity 409 실재현 완료

<!-- codex-turn:01a0f14a-183f-7042-9d8f-de39617a950d -->

- 브랜치: develop. Jira: 연결된 이슈 없음.
- 작업 목표: 승인된 재현 실험에서 Apple 재로그인 후 자동 연결과 동일 오류 발생 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 사용자 인증 완료 후 Chrome 기존 테스트 탭의 비교 결과에서 동일 UID/Apple 연결/기존 로그인 수단 유지가 모두 true임을 확인. 앞선 실제 해제 결과(Apple 없음)와 대조. Identity 로그인/가입 준비 버튼을 눌러 exchange HTTP 409 PROVIDER_RELINK_REQUIRED 재발 확인.
- 테스트와 결과: 실제 Firebase Apple 인증→같은 UID로 Apple 재연결→Identity exchange 409 흐름 재현 성공. 신규 signup/명시적 SNS link API 없이 일반 로그인 경로에서 재연결 확인. git diff --check 수행. 코드 변경 없어 자동 테스트 재실행 없음.
- 유지한 계약: Google·전화번호 연결 유지, 전체 계정 삭제·DB 직접 변경·서버 코드/배포 변경 없음. 개인정보·토큰·비밀정보 비기록. 기존 사용자 변경 보존.
- 결정사항: 이번 재현의 자동 재연결은 추정이 아닌 관측 결과. 최초 사건의 과거 생성 로그까지 확보한 것은 아님. Identity의 누락된 승인 레코드 거절과 Firebase 자동 연결 간 호환성 문제를 해결 범위로 제시.
- 위험 요소: 원인 재현 완료이며 해결 완료는 아님. 현재 Apple 연결은 다시 존재하고 Identity Apple 로그인은 여전히 거절됨. 예상 밖 수정 없음.
- 다음 작업: 사용자 요청 시 기존 제공자 해제/차단 보호를 유지하는 자동 연결 처리 및 안전한 복구 계획 수립. 추가 해제/자동 승인하지 않음. Jira 변경 없음.

## 2026-09-30 — 다른 Apple 계정 비교 실험 준비 및 기존 연결 해제

- 브랜치: develop. Jira: 요청에 연결된 이슈 없음.
- 작업 목표: 기존 Apple 연결 해제 후 다른 Apple 계정 로그인에서도 동일 오류가 발생하는지 비교.
- 변경 파일: 별도 로컬 테스트 프로젝트 app.js, apple.test.mjs 및 docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 다른 계정의 Firebase UID 변경은 예상 가능한 결과이므로 재현 로그인 단계에서 중단하지 않고 boolean 비교만 표시. 서로 다른 계정 연결/토큰 복사 없음. 다른 UID Mock 로그인 후 exchange 버튼 활성화 검증 추가.
- 실제 수행: 기존 오류 화면 새로고침 및 공개 웹 설정 재적용, Google 인증/Identity 로그인 성공 확인. 사용자 요청한 Apple 단독 해제 실행 후 동일 UID·Google/전화 유지·Apple 없음 확인. Apple 로그인 창을 열었으며 사용자 이중 인증 단계 대기.
- 테스트와 결과: Node app/apple/merge/challenge 테스트 5개 통과, 실제 해제 후 상태 확인, git diff --check. 서버 코드 미변경으로 Gradle 재실행 생략.
- 유지한 계약: 전체 사용자 삭제·Google/전화번호 해제·Identity DB 직접 변경·배포 없음. 비밀정보 비기록, 기존 사용자 변경 보존.
- 결정사항: Identity exchange 결과 비교까지만 범위로 하며 신규 signup/약관 동의/전화번호 연결은 수행하지 않음.
- 위험 요소: 실제 다른 Apple 계정 여부·UID 및 오류 결과 미확인, 사용자 인증 필요. 현재 기존 Apple 연결은 해제 상태이며 재연결 가능. 예상 밖 수정 없음.
- 다음 작업: 사용자 인증 후 UID 비교 및 exchange 응답 확인. 로컬 재현 기능은 서비스 배포 대상 아님. Jira 변경 없음.

## 2026-09-30 — 다른 Apple 비교 실험 작업 식별 기록 보완

<!-- codex-turn:01a0f14c-14b3-7c60-bbfb-0531777241b7 -->

- 브랜치: develop.
- 작업 목표: 이번 다른 Apple 계정 비교 실험의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 다른 UID 허용 비교 기능·기존 Apple 단독 해제·사용자 인증 대기 결과의 식별 기록 추가.
- 테스트와 결과: 앞선 Node Mock 테스트 5개 통과 및 실제 해제 결과 확인 유지. git diff --check 수행, 문서 보완만으로 실행 테스트 재실행 없음.
- 유지한 계약: 추가 계정·DB·코드·설정 변경 없음, 비밀정보 비기록.
- 결정사항: 다른 Apple 인증 및 exchange 확인 전까지 재현 결과를 확정하지 않음.
- 위험 요소: 사용자 인증 대기, 실제 다른 UID 및 오류 응답 미확인. 예상 밖 수정 없음.
- 다음 작업: 인증 완료 후 기존 페이지의 비교 상태 및 Identity 응답 확인. Jira 변경 없음.

## 2026-09-30 — 다른 Apple 계정의 Identity 가입 준비 정상 응답 확인

- 브랜치: develop. Jira: 연결된 이슈 없음.
- 작업 목표: 다른 Apple 계정 로그인 시 기존 계정 자동 연결 사례와 동일한 409가 발생하는지 검증.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 사용자 Apple 인증 완료 후 Chrome 재현 화면의 동일 UID=false/Apple 연결=true/기존 로그인 수단 유지=false 확인. Identity 로그인/가입 준비 버튼 실행 후 신규 가입 준비 완료 안내 및 전화 인증 시작 버튼 활성 확인.
- 테스트와 결과: 실제 다른 Firebase UID의 Apple 인증 및 exchange 신규 enrollment 경로 성공, 해당 409 재발 없음. 전화 연결/약관 동의/signup은 미실행. git diff --check 수행. 코드 변경 없어 자동 테스트 재실행 없음.
- 유지한 계약: 기존 계정·Google/전화·회원 기록 삭제 없음, 추가 provider 변경 없음. 토큰/사용자 식별값/비밀정보 비기록. 신규 Identity 회원 생성까지 수행하지 않음.
- 결정사항: Apple 로그인 전체 장애가 아니라 기존 Firebase 계정에 자동 연결된 SNS와 Identity 승인 정보가 불일치할 때의 문제로 범위 좁힘. 기존 로그인 수단 유지=false는 다른 계정 간 비교로 해석하며 기존 계정 수단 삭제로 해석하지 않음.
- 위험 요소: 신규 MEMBER 가입과 이후 API 검증은 미수행. 테스트 enrollment는 만료될 수 있음. 이전 기존 계정의 Apple 해제 상태 유지. 예상 밖 수정 없음.
- 다음 작업: 요청 시 기존 UID 자동 연결 대응 설계 또는 신규 Apple 회원가입 검증 진행. Jira 변경 없음.

## 2026-09-30 — 다른 Apple 계정 검증 작업 식별 기록 보완

<!-- codex-turn:01a0f14f-ab03-7770-88dd-7c286941d9fe -->

- 브랜치: develop.
- 작업 목표: 이번 다른 Apple 계정 검증의 정확한 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 다른 Firebase UID의 Apple 인증 및 Identity 신규 가입 준비 정상 응답 결과의 식별 기록 추가.
- 테스트와 결과: 앞선 실제 화면 검증 결과 유지, git diff --check 수행. 문서 보완만으로 실행 테스트 재실행 없음.
- 유지한 계약: 추가 계정·DB·코드·설정 변경 없음, 비밀정보 비기록.
- 결정사항: 409 미재발과 신규 가입 준비 성공을 최종 회원가입 완료와 구분.
- 위험 요소: 전화번호 연결·MEMBER 가입·후속 API 미검증. 예상 밖 수정 없음.
- 다음 작업: 요청 시 기존 UID 자동 연결 대응 설계 또는 신규 가입 검증 진행. Jira 변경 없음.

## 2026-09-30 — Apple 신규 회원 후속 검증 순서 안내

<!-- codex-turn:01a0f152-ed12-7800-936d-e5e4dcd2227e -->

- 브랜치: develop.
- 작업 목표: 현재 확인 범위와 다음에 필요한 기능 검증의 우선순위 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 신규 enrollment 성공과 MEMBER 가입 완료를 구분하고 전화 연결/동의/가입, 프로필, Apple 재로그인, 토큰 재발급, LC 접근 및 챌린지 순서 제안. 병합은 기본 인증 이후 별도 테스트로 분리.
- 테스트와 결과: 앞선 실검증 결과 기반 안내, git diff --check 수행. 신규 외부 테스트 미실행.
- 유지한 계약: 약관 동의는 사용자 직접 수행, 기존 계정 전화번호 재사용 금지 안내. 실제 계정·데이터·설정 변경 없음, 비밀정보 비기록.
- 결정사항: 정상 신규 계정 경로 검증은 이어갈 수 있으나 기존 UID 자동 연결 409를 해결 완료로 취급하지 않음.
- 위험 요소: 신규 Apple MEMBER 발급/재로그인/재발급 미검증. 자동 연결 문제는 출시 전 수정·회귀 검증 필요. 예상 밖 수정 없음.
- 다음 작업: 사용자 진행 요청 후 미사용 테스트 전화번호 및 약관 동의 준비, 가입 검증. Jira 변경 없음.

## 2026-09-30 — 신규 Apple 가입용 테스트 전화번호 가용성 확인

- 브랜치: develop.
- 작업 목표: 요청된 신규 Apple 가입 검증을 위한 미사용 테스트 전화번호 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Chrome Apple 신규 enrollment 준비 상태 확인 및 Firebase 전화 provider의 테스트 번호 설정 펼쳐 조회. 등록 번호는 1개이며 앞서 확인된 기존 Google 회원의 연결 번호로 별도 번호 필요.
- 테스트와 결과: 실제 설정 화면 읽기 전용 확인, git diff --check 수행. 새로운 전화 인증/가입 및 실행 테스트 미수행.
- 유지한 계약: 기존 Google 전화 연결·회원 기록 보존, provider 설정 및 테스트 인증 코드 변경 없음. 인증 코드/개인정보 비기록.
- 결정사항: 현재 번호를 새 Apple 계정에 재사용하지 않음. 별도 가상 번호 및 고정 코드는 사용자가 콘솔에서 직접 등록하도록 인계, 이후 전화 인증 이어서 진행. 약관 동의는 별도 사용자 확인 필요.
- 위험 요소: 추가 테스트 번호 미등록으로 전화 연결 검증 대기, enrollment 만료 시 가입 준비 재요청 필요. 예상 밖 수정 없음.
- 다음 작업: 사용자 별도 가상 번호 등록 후 신규 Apple 전화 연결 검증. Jira 변경 없음.

## 2026-09-30 — 신규 Apple 전화 인증 준비 작업 식별 기록 보완

<!-- codex-turn:01a0f153-b853-7d73-addf-8c5d552adbf6 -->

- 브랜치: develop.
- 작업 목표: 이번 전화 인증 준비 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 테스트 번호 가용성 확인 및 별도 번호 등록 대기 상태의 식별 기록 추가.
- 테스트와 결과: git diff --check 수행. 문서 보완만으로 실행 테스트 미실행.
- 유지한 계약: 기존 회원 전화 연결 유지, 외부 설정·계정 변경 없음, 비밀정보 비기록.
- 결정사항: 사용 중인 테스트 번호를 새 계정에 재사용하지 않음.
- 위험 요소: 별도 테스트 번호 등록 및 실제 전화 인증 미완료. 예상 밖 수정 없음.
- 다음 작업: 사용자 번호 등록 후 신규 Apple 전화 연결 검증. Jira 변경 없음.

## 2026-09-30 — 신규 Apple 계정 테스트 전화 연결 성공

<!-- codex-turn:01a0f157-dcb6-77f3-92fa-aee455c04396 -->

- 브랜치: develop.
- 작업 목표: 사용자 등록한 별도 가상 번호로 신규 Apple 계정 전화 연결 검증.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Firebase 전화 provider 설정에 테스트 번호 두 개 및 저장 비활성 확인. 기존 번호와 다른 신규 가상 번호를 사용. Chrome Apple 세션에서 exchange 가입 준비 갱신 후 전화 인증 시작 및 고정 코드 연결 수행. SDK 성공 화면에서 Firebase UID 유지와 전화 연결 완료 확인.
- 테스트와 결과: 실제 테스트 Firebase 전화 연결 성공. 최종 signup/프로필/토큰 재발급/LC는 미실행. git diff --check 수행, 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 실제 SMS 미발송, 기존 Google 계정의 전화 연결 유지. 인증 코드·토큰·개인 식별값 비기록. 약관 동의 및 최종 가입 대리 실행 없음. 서버 설정·배포·DB 직접 변경 없음.
- 결정사항: 등록 완료 보고를 전화 연결 검증 진행으로 해석. 가입 전 사용자 닉네임·정책 확인·동의 필요. 기존 draft 정책 버전을 현재 live 값으로 단정하지 않음.
- 위험 요소: 신규 Identity MEMBER는 아직 생성되지 않았으며 enrollment 만료 가능. 기존 자동 연결 409 문제는 미해결. 예상 밖 수정 없음.
- 다음 작업: 현재 정책 버전 확인 및 사용자 직접 약관 동의 후 최종 가입, 프로필/재로그인/재발급 검증. Jira 변경 없음.

## 2026-09-30 — 배포된 정책 버전 확인 및 테스트 화면 입력

<!-- codex-turn:01a0f15a-e8a5-77e3-8b23-e88ece58b884 -->

- 브랜치: develop.
- 작업 목표: 사용자 요청대로 현재 서버 정책 버전만 입력하고 동의·가입은 사용자에게 유지.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. AWS 테스트 서비스 새로고침 후 연결된 task revision 6에서 개인정보 privacy-v1, 이용약관 term-v1 확인. Chrome 테스트 화면 두 버전 필드 입력 및 표시 확인.
- 테스트와 결과: 브라우저 실제 설정 및 입력값 검증 완료. git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, 동의 체크·최종 가입·운영 설정 변경 없음.
- 결정사항: 조회 당시 동의 체크가 이미 선택되어 있었으므로 그대로 보존. 대리 동의 또는 signup 미실행.
- 위험 요소: 신규 MEMBER 가입 및 후속 검증 미완료, enrollment 만료 가능. 기존 사용자 변경 보존, 이번 작업 예상 밖 변경 없음.
- 다음 작업: 사용자 약관 확인 후 최종 가입 및 프로필·재로그인·재발급 검증. Jira 변경 없음.

## 2026-09-30 — 신규 Apple 가입 RESTART_EXCHANGE 진단

- 브랜치: develop.
- 작업 목표: 사용자 로그인 실패 보고 원인 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Chrome signup RESTART_EXCHANGE 및 프로필 버튼 비활성 확인. 로컬 app.js의 signup 사전 검사와 서버 enrollment 기본 10분·응답 밀리초 단위 확인.
- 테스트와 결과: UI 및 소스 읽기 검증. git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 토큰·개인정보 비기록, 약관 동의/가입/서버 변경 미실행.
- 결정사항: Firebase 로그인 실패가 아니라 가입 준비 재시작 요구로 안내. 경과 시간상 enrollment 만료 유력하나 내부 메모리 직접 조회 없이 다른 사전 조건 실패 가능성과 구분.
- 위험 요소: 신규 MEMBER 미생성, 재인증 필요 가능. 기존 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 사용자 exchange 갱신 및 필요 시 Apple 재인증 후 최종 가입. Jira 변경 없음.

## 2026-09-30 — 가입 오류 진단 작업 식별 기록

<!-- codex-turn:01a0f15f-3567-7f61-83c8-5570e01ed056 -->

- 브랜치: develop.
- 작업 목표: 이번 RESTART_EXCHANGE 진단의 turn 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome 오류 및 로컬 가입 사전 검사 확인 결과를 기록. 코드 수정 없음.
- 테스트와 결과: git diff --check 통과. 읽기 전용 진단으로 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, 동의·가입·서버 변경 없음.
- 결정사항: 가입 준비 갱신 안내, enrollment 만료는 유력 원인으로 구분.
- 위험 요소: 신규 가입 및 후속 인증 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 가입 준비 갱신 후 최종 가입 결과 확인. Jira 변경 없음.

## 2026-09-30 — 전화 연결 후 exchange 제공자 거절 진단

- 브랜치: develop.
- 작업 목표: HTTP 403 FIREBASE_PROVIDER_NOT_ALLOWED 원인 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome 검증표에서 exchange 실패 확인. FirebaseAdminAuthenticationVerifier의 LOGIN_EXCHANGE PHONE 거절 조건 확인. 직전 task revision 6의 Apple/Phone 활성 확인과 비교.
- 테스트와 결과: UI 및 소스 읽기 검증, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: PHONE 단독 로그인 제한 유지, 비밀정보 비기록, 계정/동의/배포 변경 없음.
- 결정사항: 전화 연결 후 sign-in provider 변경 가능성이 유력하나 현재 claim 미조회로 확정하지 않음. 같은 Apple 재인증 후 exchange 및 가입 안내.
- 위험 요소: 실제 claim 및 재인증 후 성공 미확인. 기존 변경 보존, 예상 밖 수정 없음.
- 다음 작업: 사용자 Apple 재인증 후 가입 결과 확인. Jira 변경 없음.

## 2026-09-30 — 제공자 거절 진단 식별 기록 보완

<!-- codex-turn:01a0f161-0128-7b92-b88f-57124fe10a98 -->

- 브랜치: develop.
- 작업 목표: 현재 turn의 exchange 403 진단 기록 연결.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: UI exchange 실패 및 서버 PHONE 로그인 제한 확인 결과 기록. 코드 변경 없음.
- 테스트와 결과: git diff --check 통과. 읽기 전용 진단으로 Gradle 미실행.
- 유지한 계약: 제공자 제한 유지, 동의·가입·설정 변경 없음, 비밀정보 비기록.
- 결정사항: PHONE 전환은 미확정 가설이며 같은 Apple 재인증 후 재시도 안내.
- 위험 요소: 재인증 후 성공 및 현재 claim 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 Apple 재인증 및 가입 결과 확인. Jira 변경 없음.

## 2026-09-30 — Apple 가입 후 기능 및 병합 읽기 검증

- 브랜치: develop.
- 작업 목표: 사용자 수행 후 성공 범위와 미검증 범위 구분.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Chrome 검증표에서 Apple 재인증·회원가입·프로필·재발급·LC 인증·Guest 준비·Identity 병합·응시·S3 업로드 성공 확인. 병합 후 검증 버튼의 읽기 전용 구현을 확인하고 실행.
- 테스트와 결과: Identity/LC 이전 Guest 모두 ACCOUNT_MERGED_TOKEN_REJECTED 확인, 대상 MEMBER 완료 이력 조회 성공. 이전 기록 0건으로 실제 이전 미검증. 챌린지 결과 solvedQuestionCount=0 및 question=null, 제출/AI 채점 성공 미확인. git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀정보·개인정보 비기록, 추가 가입·병합·제출·재발급 또는 서버 설정 변경 없음.
- 결정사항: 과거 signup/exchange 오류 기록과 이후 성공 기록 구분. 기본 인증 및 병합 차단은 성공, 전체 기능 완료로 단정하지 않음.
- 위험 요소: AI 채점, 기록 있는 Guest 이전, 이벤트 204/중복 처리는 미검증. 기존 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 챌린지 제출/채점 및 기록 있는 Guest 이전은 별도 범위로 검증. Jira 변경 없음.

## 2026-09-30 — 후속 기능 검증 turn 기록 보완

<!-- codex-turn:01a0f163-6859-73f0-8185-9d56dd1949c7 -->

- 브랜치: develop.
- 작업 목표: 이번 Apple 가입 후 기능 확인의 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 기본 인증·가입·재발급·LC 접근 성공 표시 확인 및 병합 후 읽기 검증 결과 기록. 코드 변경 없음.
- 테스트와 결과: 이전 Guest의 Identity/LC 전용 차단 및 대상 MEMBER 이력 조회 성공. git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 추가 가입·병합·제출·서버 변경 없음, 비밀정보 비기록.
- 결정사항: 기본 인증 성공과 전체 종단 검증 완료를 구분.
- 위험 요소: 기록 0건으로 실제 이전 미검증, AI 채점 성공 미확인. 예상 밖 변경 없음.
- 다음 작업: 챌린지 제출·채점 및 기록 있는 Guest 이전 별도 검증. Jira 변경 없음.

## 2026-09-30 — 이전 Google 채점 성공 기록 확인

- 브랜치: develop.
- 작업 목표: 이전 Google 검증 성공 여부와 최근 Apple 검증 범위 구분.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 WORKLOG에서 CORS 수정 후 실제 업로드·제출·AI completed 및 결과 재조회 성공 확인. 이번 Apple 계정 미확인과 구분하여 설명.
- 테스트와 결과: 과거 기록 읽기 확인, git diff --check 수행. 코드 변경 및 신규 서버 호출 없어 실행 테스트 미실행.
- 유지한 계약: 비밀정보 비기록, 외부 상태 변경 없음.
- 결정사항: AI 전체가 미검증인 것이 아니라 새 Apple 계정 시도만 미확인으로 정정. Google 당시 기록 이전도 0건으로 미검증.
- 위험 요소: Apple 현재 채점 성공 및 기록 이전은 여전히 미확인. 예상 밖 변경 없음.
- 다음 작업: 필요 시 Apple 계정 제출·결과 확인. Jira 변경 없음.

## 2026-09-30 — Google 검증 범위 확인 turn 기록 보완

<!-- codex-turn:01a0f166-2c49-7ef0-a54d-ae1cca42c1bc -->

- 브랜치: develop.
- 작업 목표: 이전 Google 채점 성공 확인과 이번 Apple 시도의 미확인 범위 구분.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 검증 기록의 실제 AI completed 및 결과 재조회 성공 확인. 코드 변경 없음.
- 테스트와 결과: 기록 읽기 확인 및 git diff --check 통과. 코드 변경 없어 실행 테스트 미실행.
- 유지한 계약: 외부 상태 변경 없음, 비밀정보 비기록.
- 결정사항: 채점 기능 전체 미검증이 아닌 Apple 신규 시도 미확인으로 설명 정정.
- 위험 요소: 실제 기록 이전은 당시에도 기록 0건으로 미검증. 예상 밖 변경 없음.
- 다음 작업: 필요 시 Apple 계정 제출·채점 결과 확인. Jira 변경 없음.

## 2026-09-30 — 기록 있는 Guest 병합 재검증 범위 확인

- 브랜치: develop.
- 작업 목표: 기존 Guest 정리 및 임시 모의고사 1건을 이용한 병합 재검증 준비.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: UserMerged 인계 계약에서 source MERGED tombstone 유지 확인. 기존 병합 계정 삭제 대신 신규 Guest 사용 제안, LC 저장소/fixture 경로 확인 요청.
- 테스트와 결과: 계약·이전 검증 기록 읽기 및 git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: Identity에 모의고사 코드 추가 금지, 비밀정보 비기록, 외부 삭제·생성·병합 미실행.
- 결정사항: 삭제 범위 불명확한 기존 병합 기록 보존. LC 스키마 확인 없이 임의 문서 삽입하지 않음.
- 위험 요소: LC 임시 완료 데이터 생성 방법 미확인, 신규 Guest 동의 및 실제 병합은 실행 전 별도 확인 필요.
- 다음 작업: LC 저장소 위치 확인 후 테스트 데이터 준비 및 신규 Guest 병합 검증. Jira 변경 없음.

## 2026-09-30 — 기록 있는 Guest 재검증 준비 turn 기록

<!-- codex-turn:01a0f167-97be-7490-8a52-f32ed56a3dce -->

- 브랜치: develop.
- 작업 목표: 새 Guest와 완료 모의고사 1건을 이용한 병합 검증 준비 범위 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 기존 MERGED Guest 보존 및 새 Guest 사용 제안. LC 스키마 확인을 위한 저장소 위치 요청. 코드 변경 없음.
- 테스트와 결과: 계약·기록 조회 및 git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: Identity 도메인 경계 유지, 비밀정보 비기록, 외부 삭제·생성·병합 미실행.
- 결정사항: 기존 병합 기록은 삭제하지 않고 데이터 생성 방법 확인 후 진행.
- 위험 요소: LC fixture 방법 미확인, 신규 Guest 동의·실제 병합 실행 전 확인 필요. 예상 밖 변경 없음.
- 다음 작업: LC 저장소 위치를 받아 테스트 데이터 준비 방법 확인. Jira 변경 없음.

## 2026-09-30 — 새 Guest 생성 버튼 비활성 진단

- 브랜치: develop.
- 작업 목표: 새 Guest 생성 불가 원인 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome 생성 버튼 disabled 및 기존 병합 검증 버튼 활성 확인. 독립 도구 app.js controls가 oldGuest/mergeUncertain을 포함해 생성 차단하며 병합 후 검증용 상태를 보존함을 확인. clear는 전체 로컬 인증·녹음/응시 상태를 초기화하므로 미실행.
- 테스트와 결과: UI·소스 읽기 검증 및 git diff --check 수행. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 외부 계정 삭제·생성·병합 없음, 비밀정보 비기록.
- 결정사항: 서버 오류와 로컬 도구 재검증 잠금을 구분. 초기화 영향 안내 후 사용자 선택 필요.
- 위험 요소: 전체 초기화 시 현재 메모리 로그인·녹음/진행 상태 손실. 기존 서버 계정·기록은 삭제되지 않음. 예상 밖 변경 없음.
- 다음 작업: 전체 로컬 초기화 후 재로그인 또는 Guest 전용 초기화 기능 개선 요청에 따라 진행. Jira 변경 없음.

## 2026-09-30 — Guest 생성 잠금 진단 turn 기록 보완

<!-- codex-turn:01a0f16a-4741-70b0-9a6e-579e498bae50 -->

- 브랜치: develop.
- 작업 목표: 새 Guest 생성 버튼 비활성 진단의 현재 turn 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: UI disabled 및 기존 병합 상태 보존에 따른 도구 잠금 확인. 코드 수정·로컬 초기화 미실행.
- 테스트와 결과: UI/소스 확인 및 git diff --check 통과. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 서버 계정 삭제·생성·병합 없음, 비밀정보 비기록.
- 결정사항: 전체 초기화의 로그인·녹음 손실을 안내하고 MEMBER 유지형 Guest 전용 초기화 개선 여부 질문.
- 위험 요소: 기존 로컬 상태 유지 중이며 반복 생성은 여전히 잠김. 예상 밖 변경 없음.
- 다음 작업: 사용자 개선 승인 또는 전체 초기화 선택 대기. Jira 변경 없음.

## 2026-09-30 — Apple 자체 로그인 폼 오류 확인

- 브랜치: develop.
- 작업 목표: 사용자 Apple 로그인 일반 오류의 발생 단계 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome Apple 인증 팝업의 계정 로그인 폼에서 문제가 발생했으니 다시 시도하라는 문구 확인. localhost의 Apple/exchange 버튼은 팝업 대기로 비활성 확인. 코드 변경 없음.
- 테스트와 결과: 브라우저 읽기 검증, git diff --check 수행. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 비밀번호·인증 코드·OAuth state 비기록, 로그인 대행·초기화·외부 설정 변경 없음.
- 결정사항: Identity 403/409와 구분하여 Apple 계정 인증 단계 문제로 안내. 상세 원인 및 일시 제한 여부는 미확정.
- 위험 요소: 일반 오류 문구만으로 계정/세션/Apple 서비스 원인을 특정할 수 없음. 예상 밖 변경 없음.
- 다음 작업: 사용자 팝업 재시작 또는 패스키 인증 후 결과 확인. Jira 변경 없음.

## 2026-09-30 — Apple 로그인 팝업 진단 turn 기록 보완

<!-- codex-turn:01a0f16c-c46e-76a2-ad7a-c1a57e771cc4 -->

- 브랜치: develop.
- 작업 목표: Apple 자체 로그인 오류 진단의 현재 turn 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Apple 계정 로그인 폼의 일반 오류와 테스트 화면의 팝업 대기 상태 확인. 코드 수정 없음.
- 테스트와 결과: UI 읽기 검증 및 git diff --check 통과. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 비밀정보 비기록, 인증정보 입력·초기화·계정 삭제·설정 변경 없음.
- 결정사항: Identity 오류와 구분하고 사용자 팝업 재시작 또는 패스키 인증 안내.
- 위험 요소: 상세 원인 미확정, 재시도 성공 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 재인증 결과 확인. Jira 변경 없음.

## 2026-09-30 — 테스트 화면 정책 버전 재입력

- 브랜치: develop.
- 작업 목표: 정책 버전 재입력 및 사용자에게 값 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 이전 AWS test revision 6 확인값 privacy-v1, term-v1을 Chrome 두 입력란에 재입력. 코드 변경 없음.
- 테스트와 결과: 입력값 UI 표시 확인 및 git diff --check 수행. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 동의 체크·가입 버튼 미조작, 비밀정보 비기록.
- 결정사항: 이번 AWS 재조회 없이 앞서 검증한 버전 사용.
- 위험 요소: 이후 배포 정책 변경 여부는 이번에 재확인하지 않음. 예상 밖 변경 없음.
- 다음 작업: 사용자 정책 확인·동의 후 테스트 진행. Jira 변경 없음.

## 2026-09-30 — 새 Guest 더미 모의고사 삽입 사전 조사

- 브랜치: develop.
- 작업 목표: 새 Guest 소유 완료 모의고사 1건 준비.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 로컬 Learning Core 저장소 발견 및 규칙/ExamReadService/ExamSession/ExamSummary/조회 조건 확인. Identity에 LC 코드 추가 없음.
- 테스트와 결과: 스키마·로컬 프록시 읽기 확인, git diff --check 수행. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 운영·기존 계정·병합 marker 변경 없음, 토큰·개인정보 비기록.
- 결정사항: 방금 생성된 Guest UUID가 UI에 없어 소유자 확정 전 삽입 중단. 최신 생성 계정 추정은 사용하지 않음.
- 위험 요소: 정확한 Guest 소유자 및 테스트 DB 삽입 경로 검증 필요. 더미 생성 미완료, 예상 밖 변경 없음.
- 다음 작업: Guest UUID 확인 후 테스트 DB에 한정한 fixture 생성·이력 조회 검증. Jira 변경 없음.

## 2026-09-30 — Guest 더미 데이터 준비 turn 기록 보완

<!-- codex-turn:01a0f16f-fcea-7f02-b7b6-8781010b68a7 -->

- 브랜치: develop.
- 작업 목표: 새 Guest 더미 모의고사 삽입 요청의 현재 turn 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Learning Core 로컬 소스에서 완료 이력 조회 및 세션·요약 스키마 확인. 정확한 Guest 소유자 확인 전 데이터 삽입 보류. 코드 변경 없음.
- 테스트와 결과: 소스 읽기 확인 및 git diff --check 통과. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 비밀정보 비기록, 운영·기존 계정·병합 marker 변경 없음.
- 결정사항: 최신 계정 추정 없이 Guest UUID 확인 필요. 토큰 원문 요청하지 않음.
- 위험 요소: 더미 삽입 및 이력 조회 검증 미완료. 예상 밖 변경 없음.
- 다음 작업: 정확한 Guest UUID 확인 후 테스트 DB 대상 검증 및 fixture 삽입. Jira 변경 없음.

## 2026-09-30 — 정책 버전 재입력 turn 기록 보완

<!-- codex-turn:01a0f16e-72f5-7373-9b33-e76496c0159c -->

- 브랜치: develop.
- 작업 목표: 현재 turn의 정책 버전 재입력 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 이전 테스트 배포 확인값 privacy-v1 및 term-v1을 Chrome 입력란에 채우고 사용자에게 안내. 코드 변경 없음.
- 테스트와 결과: UI 입력값 확인 및 git diff --check 통과. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 동의·가입 대리 실행 없음, 비밀정보 비기록.
- 결정사항: 앞서 확인한 정책 버전 사용, 이번 AWS 재조회 없음.
- 위험 요소: 이후 서버 정책 변경 여부 미재확인. 예상 밖 변경 없음.
- 다음 작업: 사용자 정책 확인·동의 후 테스트 진행. Jira 변경 없음.

## 2026-09-30 — Guest 더미 데이터 준비 최종 상태

<!-- codex-turn:01a0f16f-fcea-7f02-b7b6-8781010b68a7 -->

- 브랜치: develop.
- 작업 목표: Guest 더미 삽입 준비 결과를 WORKLOG 끝에 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: LC 완료 이력 스키마 확인, Guest UUID 확인 대기. 코드 변경 없음.
- 테스트와 결과: 소스 읽기 확인, git diff --check 통과. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 외부 데이터 변경 없음, 비밀정보 비기록.
- 결정사항: 소유자 추정 삽입 금지, 확인 후 테스트 DB만 사용.
- 위험 요소: fixture 삽입 미완료. 예상 밖 코드 변경 없음.
- 다음 작업: Guest UUID 확인 및 테스트 DB 검증 후 삽입. Jira 변경 없음.

## 2026-09-30 — 최신 테스트 Guest의 더미 완료 모의고사 추가

- 브랜치: develop.
- 작업 목표: 사용자 승인한 최신 Guest에 병합 검증용 완료 모의고사 1건 삽입.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 코드 변경 없음.
- 구현 내용: 테스트 Identity users에서 GUEST 최신순 3개를 최소 필드 조회하여 최신 ACTIVE/이전 2개 MERGED 확인. 테스트 LC mock_exams에 active=false·빈 questions·TEST ONLY 제목의 참조 카탈로그 1건, exam_summaries에 합성 점수120/IM2·더미 안내 1건, exam_sessions에 해당 Guest 소유 COMPLETED/active=false·날짜 포함 1건 삽입. 카탈로그 서비스의 비활성 제외와 완료 이력 조회 조건을 소스로 확인.
- 테스트와 결과: Atlas 저장 결과 확인, Chrome 병합 전 완료 시험 기록 조회 실행으로 현재 Guest에서 1건 조회 및 비교 준비 확인. git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 운영 DB·기존 계정·병합 marker/guard 수정 및 실제 AI 호출 없음. Identity에 LC 코드 추가 없음. 사용자 UUID·토큰 비기록, 더미는 실제 채점 결과와 구분.
- 결정사항: 기존 계정 삭제 없이 새 Guest에 합성 완료 이력과 요약만 추가. 비활성 카탈로그라 실제 응시 배정 제외. fixture 참조명 merge-fixture-20260930-173033으로 식별 가능.
- 위험 요소: 문항별 결과·AI 채점 검증용 완전한 시험 아님. 실제 병합 및 소유권 이전은 아직 미실행. 여러 컬렉션 UI 순차 삽입 후 현재 Guest 조회로 확인, 잔존 더미는 테스트 종료 후 정리 대상. 예상 밖 코드 변경 없음.
- 다음 작업: 사용자 재인증/병합 승인 후 MEMBER에서 동일 시험 이전 및 Guest 차단 검증. Jira 변경 없음.

## 2026-09-30 — Guest 더미 삽입 완료 turn 기록

<!-- codex-turn:01a0f172-7184-7dd2-9075-746ef07a8df3 -->

- 브랜치: develop.
- 작업 목표: 최신 테스트 Guest의 완료 모의고사 더미 생성 및 조회 검증 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 코드 변경 없음.
- 구현 내용: 승인된 최신 생성 기준으로 ACTIVE Guest 확인 후 테스트 LC에 비활성 카탈로그·완료 세션·합성 요약 각 1건 삽입.
- 테스트와 결과: Atlas 저장 및 현재 Chrome Guest의 완료 이력 1건 조회 확인. git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 운영·기존 계정·병합 marker 변경 없음, 비밀정보 비기록, 실제 AI 채점과 더미 구분.
- 결정사항: 병합 비교용 이력 준비 완료, 실제 병합 미실행.
- 위험 요소: 문항별 결과 없는 합성 데이터이며 실제 소유권 이전은 아직 미검증. 예상 밖 코드 변경 없음.
- 다음 작업: 사용자 병합 실행 후 MEMBER 기록 이전 및 Guest 차단 확인. Jira 변경 없음.

## 2026-09-30 — 더미 완료 시험 1건 병합 후 검증 성공

- 브랜치: develop.
- 작업 목표: 사용자 실행한 Guest 병합의 실제 기록 이전 및 source 차단 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome 기존 성공 표시에 이어 병합 후 읽기 검증 직접 재실행. 코드 변경 없음.
- 테스트와 결과: Identity/LC 이전 Guest의 병합 전용 거절 코드 확인, target MEMBER 이력 조회 성공, 이전 시험 ID 1건 중 1건 포함 확인. git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 추가 병합·삽입·삭제 없음, 토큰·개인 식별값 비기록, 합성 데이터와 실제 채점 구분.
- 결정사항: 이번에는 기록 있는 Guest의 완료 이력 이전 검증 성공으로 보고. 기존 0건 테스트의 미검증 상태와 구분.
- 위험 요소: 요약/문항별 결과 소유권 DB 재조회 및 이벤트204·중복·장애 재시도는 이번 작업에서 미검증. 예상 밖 변경 없음.
- 다음 작업: 필요 시 요약/상세 결과 및 중복·장애 시나리오 추가 검증. Jira 변경 없음.

## 2026-09-30 — 기록 있는 Guest 병합 검증 turn 기록

<!-- codex-turn:01a0f17a-1321-7121-9953-f75a4c519fa7 -->

- 브랜치: develop.
- 작업 목표: 사용자 병합 후 기록 이전 및 Guest 차단 검증 결과 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome 병합 후 읽기 검증 재실행. 코드 변경 없음.
- 테스트와 결과: Identity/LC 이전 Guest 병합 전용 거절 및 MEMBER 완료 이력 조회 성공, 이전 시험 1건 중 1건 포함 확인. git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 추가 병합·데이터 변경 없음, 비밀정보·개인 식별값 비기록.
- 결정사항: 합성 완료 이력 1건의 실제 이전 조회 검증 성공으로 확정.
- 위험 요소: 상세 결과 소유권 및 이벤트 중복·장애 재시도 별도 미검증. 예상 밖 변경 없음.
- 다음 작업: 필요 시 상세·중복·장애 시나리오 추가 검증. Jira 변경 없음.

## 2026-09-30 — 동일 UID 원격 선연결 복구 수정 방향 검토

- 브랜치: develop.
- 작업 목표: Google/Apple 동일 이메일 로그인 오류의 후속 수정 범위 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: ProviderChangeGuard가 기존 Firebase binding의 현재 provider SocialIdentity 미등록 시 거절하고 ProviderLinkService.prepare도 Firebase 선연결을 사후 승인하지 않는 구현 재확인. 코드 변경 없음.
- 테스트와 결과: 소스 재조회 및 git diff --check 수행. 코드 미변경으로 Gradle 미실행.
- 유지한 계약: 이메일만으로 계정 연결/통합하지 않음, 다른 UID 및 타계정 provider 충돌·차단 유지. 기존 사용자 dirty 변경 보존.
- 결정사항: 기존 제공자 재인증·사용자 명시 동의 기반 선연결 복구 경로를 권장하며 상세 계약·구현은 아직 미확정.
- 위험 요소: guard 단순 제거 또는 SocialIdentity 자동 동기화는 기존 해제/차단 정책 우회 가능. 현재 정상 흐름 검증이 모든 출시 준비 완료를 의미하지 않음. 예상 밖 변경 없음.
- 다음 작업: 복구 정책 확정 후 서버·프론트 계약과 보안 회귀 테스트 설계/구현. Jira 변경 없음.

## 2026-09-30 — Apple/Google 연결 복구 검토 turn 기록

<!-- codex-turn:01a0f17b-6860-7ae0-97d1-3a57b7e70929 -->

- 브랜치: develop.
- 작업 목표: 동일 UID의 Firebase 선연결과 Identity 미등록 상태에 대한 수정 방향 정리.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 현재 로그인 guard 및 연결 prepare의 거절 조건 확인. 기존 제공자 재인증·명시 동의 기반 복구 경로 제안. 코드 변경 없음.
- 테스트와 결과: 소스 읽기 확인 및 git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 이메일만으로 자동 연결하지 않음, provider 차단·소유권 검사 유지, 비밀정보 비기록.
- 결정사항: guard 제거가 아닌 복구 절차 보완 권장. 상세 정책·구현 미확정.
- 위험 요소: 기존 해제/차단 우회 방지 회귀 필요. 예상 밖 변경 없음.
- 다음 작업: 정책 확정 후 서버·프론트 계약과 테스트 설계. Jira 변경 없음.

## 2026-09-30 — EMAIL_VERIFICATION 적용 조건 설명

<!-- codex-turn:01a0f276-318f-73c2-b4c5-d548d50ce4ad -->

- 브랜치: develop.
- 작업 목표: missingRequirements의 EMAIL_VERIFICATION 의미와 SNS 로그인 적용 여부 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: FirebaseEnrollmentRequirementResolver 및 FirebaseAdminAuthenticationVerifier에서 PASSWORD 연결 여부와 emailVerified 조건 확인. 애플리케이션 코드 변경 없음.
- 실행한 테스트와 결과: 소스와 프론트 계약 확인, git diff --check 수행. 설명만 수행하여 Gradle 테스트 미실행.
- 유지한 계약: 기존 enum 및 가입/승격 검증 조건 유지. 비밀정보 비기록, 기존 dirty 변경 보존.
- 결정사항: Firebase 이메일/비밀번호 계정의 이메일 소유 확인으로 설명. 순수 SNS에는 해당 조건이 없으며 PASSWORD가 함께 연결된 미인증 계정은 예외임을 안내.
- 위험 요소: 현재 로그인 제공자만 보고 인증 필요 여부를 판단하면 연결된 PASSWORD 방식 조건을 놓칠 수 있음. 이번 작업의 예상 밖 변경 없음.
- 다음 작업: 프론트는 배열 순서 대신 각 requirement 포함 여부로 필요한 절차를 안내. Jira 작업 없음.

## 2026-09-30 — 가입 전 공개 정책 버전 조회 필요성 확인

<!-- codex-turn:01a0f27a-43d9-7210-8c7d-deeadae61e61 -->

- 브랜치: develop.
- 작업 목표: 회원가입 동의 전 인증 없이 현재 정책 버전을 조회할 수 있는지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: UserController, UserConsentService, SecurityConfig 및 Firebase 응답 확인. 현재 내 동의 조회는 JWT와 현재 사용자 조회에 의존하며 공개 정책 조회 경로는 없음. 공개 GET /api/v1/policies/consents는 제안이며 미구현.
- 실행한 테스트와 결과: 코드 읽기로 인증 경계 및 ConsentPolicy 버전 검증 확인, git diff --check 수행. 분석 작업으로 Gradle 테스트 미실행.
- 유지한 계약: 개인 동의 상태 API 인증 유지, 가입 제출 시 서버의 현재 버전 검증 유지, 기존 dirty 변경 보존.
- 결정사항: 기존 privacyConsentVersion, termConsentVersion, qualityReviewConsentVersion 이름으로 공개 정책 버전을 제공하는 방향 권장. 공통 ConsentPolicy를 사용하여 검증과 조회 값 일치 필요.
- 위험 요소: 조회와 가입 사이 정책이 바뀔 수 있어 재조회·정책 내용 재표시·재동의 필요. 버전만 최신으로 자동 교체하면 사용자가 실제 본 내용과 동의 기록 불일치 가능. 정책 본문 제공 경로는 추가 확인 필요.
- 다음 작업: 공개 정책 조회 API와 프론트 재동의 흐름 구현 시 계약 및 인증 회귀 테스트 추가. 이번 작업의 예상 밖 변경 없음, Jira 변경 없음.

## 2026-09-30 — missingRequirements와 signup 입력 생략 가능 여부 검토

<!-- codex-turn:01a0f27e-be2c-7083-b7aa-6775133ef792 -->

- 브랜치: develop.
- 작업 목표: 프론트의 단계별 저장 가정과 signup 필수 필드 계약을 비교하고 수정 필요성 판단.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: requirement resolver, signup/upgrade DTO·서비스, enrollment entity, 응답과 프론트 계약 및 기존 테스트 확인. 일반 신규 가입은 PROFILE·CONSENTS 항상 포함, enrollment는 입력 초안을 보관하지 않음. Guest 승격에서 CONSENTS가 빠질 수 있는 예외 확인. 코드 수정 없음.
- 실행한 테스트와 결과: 기존 FirebaseExchangeServiceTests의 PROFILE·CONSENTS 포함 기대값 및 FirebaseGuestPrepareServiceTests의 PHONE_VERIFICATION·PROFILE만 포함하는 기대값 확인. 테스트 실행은 하지 않았으며 분석 작업으로 Gradle 미실행. git diff --check 수행.
- 유지한 계약: signup/upgrade 필수 입력 및 최종 정책 버전 검증 유지, enrollment 소유권·만료 경계 유지. 사용자 dirty 변경 보존.
- 결정사항: 신규 가입에는 입력 선택화나 enrollment 초안 조회 API가 불필요. missingRequirements는 요청 필드 생략 목록이 아니며 클라이언트 폼 입력을 최종 제출까지 보존. Guest 승격의 기존 동의는 인증된 내 동의 API로 확인 가능.
- 위험 요소: 신규 가입과 Guest 승격을 동일하게 해석하면 동의 누락이 발생할 수 있음. PHONE_VERIFICATION 외에 PASSWORD 계정의 EMAIL_VERIFICATION도 인증 상태에 따라 달라짐. 정책 변경 시 버전만 자동 교체하지 않고 필요한 재동의 수행. 실제 프론트 구현은 미검토.
- 다음 작업: 프론트에 신규 가입/Guest 승격 차이 전달, 공개 정책 조회 API는 별도 구현 범위로 유지. 예상 밖 변경 없음, Jira 작업 없음.

## 2026-09-30 — signup 입력과 Firebase 사전 인증 시점 설명

<!-- codex-turn:01a0f281-5b58-7311-bb96-2b359e06ba9b -->

- 브랜치: develop.
- 작업 목표: signup 전에 missingRequirements에서 인증 항목만 빠질 수 있다는 설명을 쉽게 풀어 전달.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 직전 확인한 resolver·signup·enrollment 구현을 근거로 프론트 폼 입력과 Firebase 인증 완료 상태의 저장 위치 및 전달 시점 차이를 설명. 코드 변경 없음.
- 실행한 테스트와 결과: 설명 작업으로 Gradle 미실행. 직전 코드 확인 결과를 재사용하고 git diff --check 수행.
- 유지한 계약: 신규 가입 PROFILE·CONSENTS 항상 요구, 최종 signup 필수 입력 유지. 비밀정보 비기록.
- 결정사항: 화면 입력만으로 서버 요구사항이 충족되지 않으며 signup에서 전달됨을 안내. Firebase 전화번호 인증은 signup 전에 완료 가능함을 예시로 설명.
- 위험 요소: PASSWORD 이메일 인증 및 Guest 기존 동의 예외를 구분해야 함. 기존 dirty 변경 보존, 이번 예상 밖 변경 없음.
- 다음 작업: 공개 정책 버전 조회 필요성은 별도 유지. Jira 변경 없음.

## 2026-09-30 — 신규 가입 폼 값 보관 책임 확인

<!-- codex-turn:01a0f285-a6a1-7032-beac-08ff3646d8ef -->

- 브랜치: develop.
- 작업 목표: 전화번호 사전 인증과 signup 필수 입력에 대한 사용자 이해 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 일반 신규 가입은 닉네임·동의·버전을 필수 제출하고 프론트에서 입력값을 보관해야 함을 설명. 코드 변경 없음.
- 실행한 테스트와 결과: 앞서 확인한 resolver와 DTO 근거 재사용. 설명 작업으로 Gradle 미실행, git diff --check 수행.
- 유지한 계약: signup 필수 입력 및 Firebase 인증 경계 유지, 비밀정보 비기록.
- 결정사항: 프론트가 자동으로 값을 보유하는 것이 아닌 화면 입력 후 제출/재시도까지 보관하는 책임으로 명확화.
- 위험 요소: 새로고침·앱 종료 등으로 폼 값이 사라지면 다시 입력받아야 함. Guest 승격의 기존 동의 예외와 구분. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 프론트의 폼 보관/재입력 흐름 적용. Jira 변경 없음.

## 2026-09-30 — signup 선택 동의와 공개 정책 조회 수정 방향 설명

- 브랜치: develop.
- 작업 목표: Firebase 신규 가입 선택 품질 검토 동의 및 가입 전 공개 정책 조회의 변경 계약 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 기존 UserConsentUpdateRequest, ConsentPolicy, UserFactory, UserConsents를 근거로 재사용 방향 정리. signup DTO·서비스·factory 확장과 공개 정책 조회 controller/DTO 및 GET 인증 허용 제안. 애플리케이션 미구현.
- 실행한 테스트와 결과: 기존 구현 소스 확인, git diff --check 수행. 구현 전 설명 단계로 Gradle 미실행.
- 유지한 계약: 기존 선택 동의 필드명, 필수 개인정보·약관 검증, 기존 클라이언트 누락 시 false, 개인 동의 API 인증 유지.
- 결정사항: true일 때 현재 선택 정책 버전 검증, false일 때 버전 생략 가능. 공개 조회는 사용자 상태 없이 동일 ConsentPolicy의 세 버전을 반환. 신규 엔티티·환경변수 추가 불필요.
- 위험 요소: 공개 조회 이후 정책 변경 시 실제 재동의 필요. Guest upgrade에 같은 입력을 추가하는 변경은 이번 신규 signup 설명 범위와 별도로 구분. 실제 정책 내용과 버전의 대응은 프론트에서도 관리 필요.
- 다음 작업: DTO·서비스·factory·공개 GET·계약 문서 수정 및 선택 동의 저장/버전 검증/익명 조회/기존 보호 경로 테스트 구현. 기존 dirty 변경 보존, 이번 예상 밖 변경 없음, Jira 작업 없음.

## 2026-09-30 — signup 선택 동의·공개 조회 설명 turn 기록

<!-- codex-turn:01a0f287-75d4-73d3-9397-eb6175e4ad11 -->

- 브랜치: develop.
- 작업 목표: 이번 수정 방향 설명의 turn 식별자를 포함한 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Firebase signup에 기존 선택 동의 두 필드 추가 및 공개 GET /api/v1/policies/consents 제안 기록. 실제 API 구현 없음.
- 실행한 테스트와 결과: 기존 동의 DTO·factory·엔티티 소스 확인, git diff --check 통과. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 개인정보·약관 필수, 선택 동의 누락은 false, true일 때 현재 버전 검증, 개인 API 인증 유지.
- 결정사항: 기존 ConsentPolicy와 UserConsents 재사용, 새 엔티티·환경변수 불필요. Guest upgrade 확장은 별도 범위.
- 위험 요소: 조회 후 정책 변경 시 재동의 필요. 구현·배포는 아직 수행하지 않음.
- 다음 작업: 설명한 두 변경 구현 및 관련 테스트. 기존 dirty 변경 보존, 예상 밖 변경 없음, Jira 변경 없음.

## 2026-09-30 — Guest upgrade 선택 동의 확장 방향 추가

- 브랜치: develop.
- 작업 목표: Guest upgrade의 학습 품질 검토 선택 동의 입력 누락 확인 및 수정 계획 확장.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: FirebaseGuestUpgradeRequest와 User.promoteGuestToFederatedMember 및 UserConsents.renewRequiredConsents 확인. 요청에 선택 동의 두 필드가 없고 현재 승격이 기존 선택 동의를 보존함을 확인. 실제 코드 구현 없음.
- 실행한 테스트와 결과: 소스 읽기 확인 및 git diff --check 수행. 방향 정리 단계로 Gradle 미실행.
- 유지한 계약: 기존 필드명 및 Guest 승격 소유권/필수 동의 검증 유지, 누락 요청의 기존 동의 보존 제안. 기존 dirty 변경 보존.
- 결정사항: signup 선택 동의·공개 정책 GET에 더해 upgrade 선택 동의 입력 추가를 범위에 포함. 신규 signup 누락은 false, upgrade 누락은 기존 상태 보존. 명시 false는 미동의 처리, true는 현재 버전 검증 후 반영.
- 위험 요소: upgrade 누락을 false로 기본화하면 기존 Guest 선택 동의가 의도 없이 철회될 수 있음. 선택 동의는 missingRequirements의 필수 단계로 추가하지 않음.
- 다음 작업: 세 API 변경 구현 시 기존 동의 보존/명시 철회/명시 동의/버전 오류 회귀 검증 추가. 예상 밖 변경 없음, Jira 변경 없음.

## 2026-09-30 — Guest upgrade 선택 동의 검토 turn 기록

<!-- codex-turn:01a0f289-7e99-7d31-a542-d6c0f23667ca -->

- 브랜치: develop.
- 작업 목표: Guest upgrade 선택 동의 확장 검토에 이번 turn 식별자를 포함한 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 요청 DTO에 선택 동의 필드가 없음을 확인하고 추가 방향 정리. 승격 시 기존 동의를 보존하는 현재 동작 확인. 애플리케이션 코드 변경 없음.
- 실행한 테스트와 결과: DTO·승격·동의 소스 확인 및 git diff --check 통과. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 기존 선택 동의 필드명, 필수 동의 검증 및 Guest 소유권 유지. 비밀정보 비기록.
- 결정사항: upgrade 필드 누락은 기존 동의 유지, 명시 false는 미동의 처리, true는 현재 버전 검증 후 저장하는 방향 제안.
- 위험 요소: 누락을 false로 기본화하면 기존 동의가 의도 없이 철회될 수 있음. 아직 구현·배포하지 않음.
- 다음 작업: signup·upgrade 선택 동의와 공개 정책 조회 구현 및 회귀 검증. 기존 dirty 변경 보존, 예상 밖 변경 없음, Jira 변경 없음.

## 2026-09-30 — Guest upgrade enrollment 사용 설명

- 브랜치: develop.
- 작업 목표: Guest 승격도 enrollment를 사용하는지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: FirebaseGuestPrepareService의 GUEST_USER enrollment 발급/재사용 및 FirebaseGuestUpgradeService의 소유권·유효성·Firebase 일치 검증 확인. 코드 변경 없음.
- 실행한 테스트와 결과: 서비스 소스 확인, git diff --check 수행. 설명 작업으로 Gradle 미실행.
- 유지한 계약: Guest 토큰 사용자 ID 기준 소유권 검증, Firebase 계정 일치 및 enrollment 유효성 유지.
- 결정사항: 신규 signup과 동일한 enrollment 구조를 쓰되 Guest prepare에서 받은 승격용 ID를 제출해야 함을 설명.
- 위험 요소: 신규 가입용 enrollment를 Guest 승격에 대체 사용할 수 없음. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 선택 동의 확장 및 공개 정책 조회 계획 유지. Jira 작업 없음.

## 2026-09-30 — Guest upgrade enrollment 설명 turn 기록

<!-- codex-turn:01a0f28b-a259-7003-b0dc-9ce02b89313b -->

- 브랜치: develop.
- 작업 목표: Guest upgrade enrollment 설명에 이번 turn 식별자를 포함한 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: guest/prepare가 발급한 GUEST_USER enrollment를 guest/upgrade에 제출하며 Guest 소유권·Firebase 계정 일치·유효성을 검증하는 현재 구현 확인. 애플리케이션 코드 변경 없음.
- 실행한 테스트와 결과: 서비스 소스 확인 및 git diff --check 수행. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 인증된 Guest 기준 소유권 및 enrollment 유효성 검사 유지. 비밀정보 비기록.
- 결정사항: 신규 signup과 같은 엔티티 구조를 사용하지만 신규 가입용 ID와 승격용 ID는 대체 불가. enrollment는 닉네임·동의 초안 저장 용도가 아님.
- 위험 요소: 가입/승격 ID 혼용 시 검증 실패. 기존 dirty 변경 보존, 이번 예상 밖 변경 없음.
- 다음 작업: 선택 동의 확장 및 공개 정책 조회 계획 유지. Jira 변경 없음.

## 2026-09-30 — 신규 signup MEMBER와 Firebase 연결 시점 설명

- 브랜치: develop.
- 작업 목표: 신규 가입 완료 시 MEMBER도 Firebase 계정에 연결되는지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: FirebaseExchangeService의 DIRECT_SIGNUP·boundUserId=null 및 FirebaseSignupService/TransactionService의 새 MEMBER 생성·FirebaseIdentity 저장 확인. 코드 변경 없음.
- 실행한 테스트와 결과: 소스 읽기 확인, git diff --check 수행. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 가입 전 Firebase 소유권 검증 및 가입 성공 시 회원·Firebase 매핑 원자적 저장 유지.
- 결정사항: 신규 가입도 Firebase 계정과 연결되며 가입 전 enrollment 연결과 가입 완료 후 MEMBER 매핑을 구분해서 설명.
- 위험 요소: enrollment 자체가 영구 회원 연결 문서라고 오해하지 않도록 FirebaseIdentity와 구분. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 선택 동의 확장 및 공개 정책 조회 계획 유지. Jira 변경 없음.

## 2026-09-30 — 신규 signup Firebase 연결 설명 turn 기록

<!-- codex-turn:01a0f28d-9078-7533-a411-467c1c3c6a4a -->

- 브랜치: develop.
- 작업 목표: 신규 signup Firebase 연결 설명에 이번 turn 식별자를 포함한 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 가입 전 DIRECT_SIGNUP enrollment의 Firebase 연결 및 가입 성공 시 새 MEMBER와 FirebaseIdentity 매핑 저장 확인. 애플리케이션 코드 변경 없음.
- 실행한 테스트와 결과: 서비스·트랜잭션 소스 확인 및 git diff --check 수행. 설명 작업으로 Gradle 미실행.
- 유지한 계약: Firebase 소유권 검증 및 회원·Firebase 매핑 원자적 저장 유지. 비밀정보 비기록.
- 결정사항: 신규 signup은 새 userId 생성, Guest upgrade는 기존 userId 유지. enrollment와 영구 FirebaseIdentity 매핑 역할 구분.
- 위험 요소: enrollment를 영구 회원 연결 문서로 오해하지 않도록 안내. 기존 dirty 변경 보존, 이번 예상 밖 변경 없음.
- 다음 작업: 선택 동의 확장 및 공개 정책 조회 계획 유지. Jira 변경 없음.

## 2026-09-30 — enrollment 유효기간과 DB 삭제 시점 설명

- 브랜치: develop.
- 작업 목표: enrollment가 TTL 초과 시 삭제되는지 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: FirebaseEnrollmentAttempt의 expiresAt·cleanupAt, lifecycle 삭제 예약, 만료 capture 및 application 기본 설정 확인. 코드 변경 없음.
- 실행한 테스트와 결과: 소스·설정 읽기 및 git diff --check 수행. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 만료 시 서버 검증으로 재사용 차단, DB 삭제와 회원·Firebase 연결의 별도 생명주기 유지.
- 결정사항: 기본 10분은 가입 시도 유효기간이며 즉시 삭제 시간이 아님. 가입 완료 후 lifecycle은 기본 24시간 뒤 TTL 정리를 예약. 미완료 건은 별도 정리 절차에 의존.
- 위험 요소: 실제 TTL 인덱스 및 배포 환경 설정 미확인. MongoDB TTL 삭제는 비동기로 정각 삭제 보장 없음. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 필요 시 실제 정리 설정·인덱스 별도 확인. 선택 동의·공개 정책 조회 계획 유지, Jira 변경 없음.

## 2026-09-30 — enrollment TTL 설명 turn 기록

<!-- codex-turn:01a0f28f-d025-7981-8040-2cb687b69152 -->

- 브랜치: develop.
- 작업 목표: enrollment 유효기간과 삭제 시점 설명의 turn 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: expiresAt과 cleanupAt 역할, 가입 완료 후 삭제 예약 및 중단 가입 별도 정리 경로를 소스 기준으로 설명. 코드 변경 없음.
- 실행한 테스트와 결과: 엔티티·lifecycle·설정 소스 확인, git diff --check 수행. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 만료 enrollment 재사용 차단 및 MEMBER/FirebaseIdentity의 별도 생명주기 유지. 비밀정보 비기록.
- 결정사항: 기본 유효기간 10분은 즉시 DB 삭제 시간이 아니며 가입 완료 후 기본 24시간 보관 뒤 TTL 정리 예약. 실제 환경 설정과 구분.
- 위험 요소: 배포 환경 설정·DB TTL 인덱스 미확인, MongoDB TTL 삭제는 비동기. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 선택 동의 확장 및 공개 정책 조회 계획 유지. 필요 시 실환경 정리 설정 별도 확인, Jira 변경 없음.

## 2026-09-30 — Guest 승격 enrollment 정리 동일성 확인

- 브랜치: develop.
- 작업 목표: Guest 승격에도 동일한 enrollment 만료·삭제 흐름 적용 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: FirebaseGuestUpgradeTransactionService의 소비 처리 및 공통 finalizeEnrollment 호출 확인. 코드 변경 없음.
- 실행한 테스트와 결과: 소스 확인 및 git diff --check 수행. 설명 작업으로 Gradle 미실행.
- 유지한 계약: enrollment 소비·만료 경계 및 회원/Firebase 연결의 별도 생명주기 유지.
- 결정사항: 기본 10분 유효기간과 완료 후 기본 24시간 삭제 예약 흐름이 Guest 승격에도 동일함을 안내.
- 위험 요소: 실제 배포 설정·TTL 인덱스 미확인. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 선택 동의 및 공개 정책 조회 계획 유지. Jira 변경 없음.

## 2026-09-30 — TMI-136 하위 선택 동의·공개 정책 조회 이슈 초안

- 브랜치: develop.
- Jira: TMI-136
- 작업 목표: 기존 논의한 SNS 가입·Guest 승격 선택 동의 및 공개 정책 조회 작업을 TMI-136 하위 이슈로 준비.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 공식 Atlassian 도구로 부모 에픽 sns 로그인, 프로젝트 작업 유형 및 기존 하위 이슈 확인. 제목·계약·완료 조건을 포함한 신규 작업 초안 작성. 코드 변경 없음.
- 실행한 테스트와 결과: Jira 읽기 조회 성공, git diff --check 수행. 초안 준비 작업으로 Gradle 미실행.
- 유지한 계약: 기존 필드명, 신규 signup 누락 false, Guest upgrade 누락 기존 동의 보존, 개인 동의 API 인증, enrollment 경계 유지.
- 결정사항: TMI-136 에픽 아래 작업 1건으로 세 API 변경 통합. AGENTS.md에 따라 내용을 먼저 제시하고 승인 후 생성.
- 위험 요소: 사용자 승인 전이라 Jira 생성 미실행. 기존 하위 이슈 제목에서 직접 중복 작업 발견하지 않음, 전체 기존 본문 중복 조사 미수행. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- Jira 작업: 부모/유형/자식 조회만 수행. 댓글 등록·상태 변경 없음. 생성 승인 대기.
- 다음 작업: 사용자 초안 승인 시 생성하고 부모 연결 재조회. Jira 댓글 초안: 선택 동의 및 공개 정책 조회 이슈 범위 준비 완료, 구현·테스트는 후속 작업에서 수행 예정. 자동 등록하지 않음.

## 2026-09-30 — TMI-136 하위 Jira 초안 검토 turn 기록

<!-- codex-turn:01a0f295-c7b4-75e3-ab58-00091111be92 -->

- 브랜치: develop.
- Jira: TMI-136
- 작업 목표: 하위 작업 생성 초안 검토의 turn 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 부모 에픽과 작업 유형 및 기존 자식 조회, signup·upgrade 선택 동의와 공개 정책 조회를 묶은 작업 제목·계약·완료 조건 준비 및 사용자에게 제시. 코드 변경 없음.
- 실행한 테스트와 결과: 공식 Atlassian 읽기 조회 성공, git diff --check 수행. 초안 작업으로 Gradle 미실행.
- 유지한 계약: 선택 동의 필드명, Guest 누락 시 보존, 필수 동의 및 개인 API 인증 유지.
- 결정사항: AGENTS.md의 사전 내용 공개·승인 규칙에 따라 생성 승인 대기.
- 위험 요소: 아직 신규 Jira 이슈는 생성되지 않음. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- Jira 작업: 읽기 조회만 수행, 생성·댓글·상태 변경 없음, 생성 승인 대기.
- 다음 작업: 사용자 승인 후 신규 작업 생성 및 부모 연결 확인. Jira 댓글 초안은 직전 기록 유지, 자동 등록하지 않음.

## 2026-09-30 — TMI-188 선택 동의·공개 정책 조회 작업 생성

- 브랜치: develop.
- Jira: TMI-188
- 작업 목표: 승인된 초안으로 TMI-136 하위 작업 생성.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 공식 Atlassian 도구로 작업 생성 및 부모 TMI-136 연결 재조회 확인. 애플리케이션 코드 변경 없음.
- 실행한 테스트와 결과: 생성 성공 및 제목·부모·해야 할 일 상태 확인, git diff --check 수행. Jira 작업만 수행하여 Gradle 미실행.
- 유지한 계약: 승인한 signup·upgrade 선택 동의 및 공개 정책 조회 범위, 기존 인증/필수 동의/enrollment 계약 유지.
- 결정사항: 신규 이슈 TMI-188로 후속 구현 범위 관리. 구현 미시작.
- 위험 요소: 서버 변경·배포는 아직 없음. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- Jira 작업: TMI-188 생성, 사용자 명시 승인 후 수행. 댓글 등록 없음, 상태 전환 없음(기본 해야 할 일).
- 다음 작업: TMI-188 기준 구현 및 검증. Jira 댓글 초안: 작업 생성 및 TMI-136 부모 연결 확인 완료, 코드 변경 없음, 구현 검증은 후속 작업 예정. 자동 등록하지 않음.

## 2026-09-30 — TMI-188 생성 완료 turn 기록

<!-- codex-turn:01a0f298-f345-7770-9d5a-e9dcdeab20e2 -->

- 브랜치: develop.
- Jira: TMI-188
- 작업 목표: 승인된 Jira 생성 작업의 turn 식별자를 포함한 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 공식 Atlassian 도구로 SNS signup·Guest upgrade 선택 동의 및 공개 정책 조회 작업 생성. 부모 TMI-136 연결 재조회 확인. 코드 변경 없음.
- 실행한 테스트와 결과: Jira 생성 성공 및 부모·기본 상태 확인, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 승인된 이슈 범위 및 기존 필수 동의·인증·enrollment 계약 유지. 비밀정보 비기록.
- 결정사항: 후속 구현은 TMI-188 기준으로 진행, 현재 구현 미시작.
- 위험 요소: API 구현·배포는 아직 없음. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- Jira 작업: 사용자 명시 승인 후 TMI-188 생성. 댓글 등록 없음, 상태 전환 없음, 기본 상태 해야 할 일.
- 다음 작업: TMI-188 구현 및 검증. Jira 댓글 초안은 직전 기록 유지, 자동 등록하지 않음.

## 2026-09-30 — TMI-188 SNS 가입·Guest 승격 선택 동의 및 공개 정책 조회 구현

<!-- codex-turn:01a0f29b-84b6-7b03-92bc-008f0e80c92b -->

- 브랜치: feat/TMI-188-quality-review-consent.
- Jira: TMI-188
- 작업 목표: 승인된 이슈의 signup/upgrade 선택 동의 저장 및 가입 전 익명 정책 버전 조회 구현.
- 변경 파일: FirebaseSignupRequest, FirebaseGuestUpgradeRequest, FirebaseSignupService, FirebaseGuestUpgradeService, FirebaseExchangeController, UserFactory, User, SecurityConfig, IdentityOpenApiExamples. 신규 ConsentPolicyController·CurrentConsentPolicyResponse. 테스트 FirebaseSignupServiceTests·FirebaseGuestUpgradeServiceTests·FirebaseExchangeControllerTests·SecurityIntegrationTests·OpenApiSharingTests 및 신규 FirebaseConsentRequestTests. 문서 docs/contracts/frontend-firebase-auth-integration-guide.md, docs/contracts/frontend-firebase-auth-integration-appendix.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 구현 내용: 기존 선택 동의 필드명/검증 재사용. signup 누락/null은 false, upgrade 누락/null은 기존 선택 동의 보존, 명시 false는 버전·시각 비움, true는 현재 버전 검증 후 저장. 유효한 기존 동의 시각은 보존하고 새 동의/버전 갱신은 서버 시각 사용. 공개 GET /api/v1/policies/consents는 동일 ConsentPolicy의 현재 세 버전을 BaseResponse.result에 반환하며 no-store 사용. 사용자 상태 및 정책 본문은 반환하지 않음. OpenAPI/프론트 요청 예시·누락 처리·버전 오류/재동의·QA 갱신.
- 실행한 테스트와 결과: 최초 샌드박스 Gradle 캐시 잠금 접근 실패 후 승인된 권한으로 실행. 첫 전체 테스트는 983개 중 Swagger endpoint count 기대값 1건 실패. 새 공개 GET로 24→25 operation 및 23→24 path 변경 반영, Swagger 응답 예시 추가 후 ./gradlew clean test 최종 통과(983 tests, failures=0, errors=0). git diff --check 통과. 테스트에서 외부 Provider/Repository는 mock 사용, 실제 외부 인증/DB 호출 없음.
- 유지한 계약: 개인정보/약관 필수 동의, 개인 동의 GET/PUT JWT 인증, Guest userId 유지, enrollment 소유권·만료·소비와 기존 트랜잭션 유지. 선택 동의는 missingRequirements에 추가하지 않음. 구버전 JSON 요청 호환, 새 엔티티·환경변수 없음.
- 결정사항: Guest upgrade는 Boolean null을 유지하여 보존/철회 구분. 기존 domain 승격 overload는 보존 동작으로 위임. 공개 정책 API는 해당 GET만 permitAll.
- 위험 요소: 배포·모바일 E2E는 미수행. 프론트 정책 본문/URL과 조회 버전의 대응 확인 필요, 조회 이후 version 변경 시 재동의 처리 필요. 실환경 정책 설정은 기존 환경변수 재사용.
- 예상 밖 변경: 이번 작업 범위 밖 추가 변경 없음. 시작 전부터 dirty였던 identity-branch-deployment.md, identity-test-container-secrets.json, identity-test-task-definition.draft.json, DeploymentTargetTests.java 및 docs/postman·tools는 수정하지 않고 보존. 기존 작업 기록도 보존.
- 배포 전 확인: 대상 환경의 세 정책 설정·본문/URL 대응 및 새 GET 익명 조회, signup/upgrade의 선택 동의 처리와 개인 API 인증 확인. 프론트는 signup/upgrade의 누락 규칙 차이를 반영.
- Jira 작업: 구현 전 TMI-188 설명 조회만 수행. 상태 전환·댓글 등록 없음. 생성 승인과 별개로 이번 구현 완료 댓글은 승인 전 미등록.
- Jira 댓글 초안: signup/upgrade 선택 동의 및 익명 정책 조회 구현 완료. 관련 DTO·서비스·domain·보안·Swagger·계약 문서/테스트 갱신. clean test 983개 통과. 대상 환경 배포와 정책 본문/버전 대응 및 모바일 연동 검증 필요. 자동 등록하지 않음.
- 다음 작업: 사용자 diff 검토 후 직접 커밋/push. 필요 시 승인받은 Jira 댓글 및 후속 배포·모바일 검증. 커밋/push/PR 생성 및 배포는 이번 작업에서 수행하지 않음.

## 2026-09-23 — CNAME 등록 실패 확인 요청

<!-- codex-turn:01a0cd9a-63c5-7d63-af9d-3a7af1766713 -->

- 브랜치: develop.
- 작업 목표: 사용자 CNAME 등록 실패의 원인 확인 준비.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 가비아 실제 오류 문구/입력 화면 확인 요청. 호스트 identity-test와 ALB DNS 값 입력 방식, 동일 호스트 레코드 충돌 가능성을 안내하되 원인 확정하지 않음.
- 테스트와 결과: git diff --check. 안내/기록만 변경하여 Gradle 미실행, DNS 재조회 및 외부 설정 변경 없음.
- 유지한 계약: 기존 DNS/인증서 검증 레코드 삭제 또는 수정 없음. 비밀값 미기록, 기존 변경 보존.
- 결정사항: 실제 오류 확인 전 DNS 변경하지 않음.
- 위험 요소: 입력 형식/기존 레코드/권한 중 원인 미확인. 예상 밖 변경 없음.
- 다음 작업: 오류 문구 또는 가비아 화면 확인 후 정확한 해결 방법 안내.

## 2026-09-23 — 테스트 도메인 DNS 및 HTTPS 연결 확인

- 브랜치: develop.
- 작업 목표: 사용자 CNAME 설정 결과 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: dig로 테스트 도메인의 CNAME이 지정한 ALB DNS와 일치함 확인. TLS 검증을 유지한 curl health 요청에서 HTTP503 수신.
- 테스트와 결과: DNS NOERROR/TTL600 및 HTTPS503 확인, git diff --check 수행. 제품 변경 없어 Gradle 미실행.
- 유지한 계약: DNS/AWS/코드 변경 없이 읽기 전용 확인. 비밀값 미기록, 기존 변경 보존. 예상 밖 변경 없음.
- 결정사항: DNS 연결 문제는 해소. HTTP503은 직전 desired0 상태와 부합하며 실제 서비스 정상 기동 완료를 의미하지 않음.
- 위험 요소: 현재 ECS 상태 재조회 및 이미지/DB/인증 기동 검증은 미수행. 원격 workflow 수정 반영 여부도 후속 확인 필요.
- 다음 작업: 사용자 workflow commit/push 후 테스트 이미지 및 revision 확인, 서버 기동/health/JWKS 검증.

## 2026-09-23 — DNS 연결 확인 작업 식별 기록 보완

<!-- codex-turn:01a0cd9b-75fb-7211-9de9-7c090e18cdd7 -->

- 브랜치: develop.
- 작업 목표: 이번 DNS/HTTPS 확인 결과에 현재 작업 식별자 연결.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: CNAME 일치 및 TLS 검증 유지 HTTPS503 확인 결과 기록 보완. 과거 기록 보존.
- 테스트와 결과: git diff --check 수행. 문서만 보완하여 Gradle 미실행.
- 유지한 계약: 추가 외부 변경 없음, 비밀값 미기록, 기존 변경 보존.
- 결정사항: DNS 연결 확인 완료, 실제 서버 정상 기동은 미완료.
- 위험 요소: 현재 ECS 상태/이미지/DB/인증 기동 미검증. 예상 밖 변경 없음.
- 다음 작업: workflow 반영 후 테스트 이미지 배포와 기동 검증.

## 2026-09-23 — develop 최초 테스트 배포 및 시작 실패 진단

- 브랜치: develop.
- 작업 목표: 사용자 push 확인 및 테스트 서버 기동.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 제품 코드 변경 없음.
- 수행 내용: 원격 develop 88ff5bed 및 분기별 workflow 확인. Actions 35843168284 테스트/이미지 빌드/ECS 배포 성공, health503 실패 확인. 테스트 서비스 test:2 desired0에서1 기동, CloudWatch 구조화 로그에서 PhoneEligibilityFingerprintHasher bean 부재 확인. 반복 시작 실패를 막기 위해 desired0 원복 요청.
- 원인: Firebase signup은 eligibility hasher를 필수 주입하지만 테스트 task 설정은 PHONE_ELIGIBILITY_BINDING_ENABLED=false. Billing 외부 publisher 비활성화와 내부 가입 의존성의 구분이 누락됨.
- 테스트와 결과: CI Run tests 성공, 기동 후 HTTPS502 및 application startup failure 확인. 로컬 제품 변경 없어 Gradle 재실행 생략. git diff --check 수행.
- 유지한 계약: 운영 서비스/권한/키 변경 없음, 비밀값 비노출, 사용자 변경 보존. 예상 밖 파일 변경 없음.
- 결정사항: 내부 binding 설정과 전용 키 준비 후 재기동하며 외부 Billing publisher는 OFF 유지. 이번에는 Secret 생성이나 코드 변경하지 않음.
- 위험 요소: DB/인증/JWKS 정상 동작 미검증, CI run은 health 실패 상태로 남음.
- 다음 작업: binding ON, consumer scope, 전용 keyring 주입 범위 확정 및 테스트 task revision 보완 후 health/회원 인증 검증.
- 원복 검증: AWS 서비스 업데이트 성공 알림 및 desired0/running0/pending0 확인 완료, test:2 유지.

## 2026-09-23 — 최초 테스트 배포 진단 작업 식별 기록

<!-- codex-turn:01a0cd9e-ef7f-79e3-8e8d-9535653d9c29 -->

- 브랜치: develop.
- 작업 목표: push 이후 테스트 배포 및 기동 진단 결과의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: CI 테스트/빌드/ECS 배포 성공과 health 실패 확인. 테스트 기동 중 PhoneEligibilityFingerprintHasher 필수 bean 누락 확인, 테스트 서비스 desired0/running0/pending0 원복 검증.
- 실행한 테스트와 결과: CI Run tests 성공. 실제 기동 실패 확인. 문서 보완만 수행하여 로컬 Gradle 미실행, git diff --check 통과.
- 유지한 계약: 운영 서비스 변경 없음, 과거 기록 및 기존 변경 보존, Secret 비기록.
- 결정사항: 내부 eligibility binding 설정 보완 필요. 외부 Billing publisher OFF 유지. 해당 hasher 키는 전화번호 식별용 HMAC 키이며 응답 복구 암호화 키와 구분한다.
- 위험 요소: 실제 가입/로그인/JWKS 정상 동작은 아직 검증하지 못함. 예상 밖 파일 변경 없음.
- 다음 작업: 테스트 내부 binding ON 및 consumer scope/전용 HMAC keyring 준비 후 새 task revision 기동 검증.

## 2026-09-23 — 테스트 내부 eligibility binding 설정 보완 준비

- 브랜치: develop.
- 작업 목표: 가입 필수 hasher 누락 기동 실패를 해결할 설정 준비.
- 변경 파일: docs/contracts/identity-test-task-definition.draft.json, identity-test-container-secrets.json, identity-branch-deployment.md, src/test/java/web/tosunsaeng/identity/deployment/DeploymentTargetTests.java, docs/codex/WORKLOG.md, CURRENT_STATE.md.
- 구현 내용: 내부 binding true, 테스트 consumer scope, 기존 테스트 Secret의 독립 HMAC key 항목 selector 준비. 외부 publisher false 유지. 설정 회귀 테스트 추가. 사용자 키 저장용 Secret 화면 열기.
- 실행한 테스트와 결과: ./gradlew clean test --no-daemon 성공(초기 sandbox Gradle lock 접근 실패 후 승인된 실행 성공), git diff --check 수행.
- 유지한 계약: API/제품 로직/운영 서비스/IAM 변경 없음, 비밀값 미생성/미기록, 기존 fingerprint 키 변경 없음. 기존 사용자 문서 수정 보존, 예상 밖 파일 변경 없음.
- 결정사항: 기존 승인된 Secret 안에 별도 키 항목을 사용해 IAM 확대 없이 주입. 새 키 입력/저장은 사용자 수행.
- 위험 요소: AWS 설정 미적용, 실제 health/인증 검증 미완료. 로컬 draft의 과거 image를 재사용하지 않고 현재 AWS test:2 기준으로 revision 생성 필요.
- 다음 작업: 사용자 키 저장 완료 확인 후 새 task revision/서비스 기동 및 health/JWKS 검증. 배포 전 독립 키 항목 존재와 selector 확인 필수. Jira 연결 없음, 자동 댓글 미등록.

## 2026-09-23 — 내부 binding 설정 보완 작업 식별 기록

<!-- codex-turn:01a0cda6-ba83-7112-91d2-ce73aefcfc96 -->

- 브랜치: develop.
- 작업 목표: 테스트 가입 필수 내부 binding 설정 보완 결과와 사용자 키 저장 대기 상태 기록.
- 변경 파일: docs/contracts/identity-test-task-definition.draft.json, docs/contracts/identity-test-container-secrets.json, docs/contracts/identity-branch-deployment.md, src/test/java/web/tosunsaeng/identity/deployment/DeploymentTargetTests.java, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 내부 binding ON/테스트 consumer scope/독립 HMAC key JSON selector 준비 및 회귀 테스트 추가. Secrets Manager 대상 화면을 사용자에게 인계.
- 실행한 테스트와 결과: ./gradlew clean test --no-daemon 성공, 147 suites/958 tests/0 failures/0 errors. git diff --check 통과.
- 유지한 계약: 외부 publisher OFF, 운영/API/IAM 변경 없음. 기존 키 및 사용자 변경 보존, Secret 미기록.
- 결정사항: 새 비밀키 입력과 저장은 사용자 수행. AWS task revision 변경 및 재기동은 저장 이후 진행.
- 위험 요소: 로컬 설정만 보완된 상태이며 실제 서버 정상 기동/인증 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 저장 완료 확인 후 현재 AWS test:2 기반 새 revision을 적용하고 health/JWKS 검증.

## 2026-09-23 — 테스트 task:3 적용 및 HMAC keyring 형식 진단

- 브랜치: develop.
- 작업 목표: 사용자 Secret 저장 후 새 설정 배포와 정상 기동 확인.
- 변경 파일: docs/contracts/identity-test-task-definition.draft.json(배포 이미지 정합), docs/contracts/identity-branch-deployment.md, docs/codex/WORKLOG.md, CURRENT_STATE.md.
- 수행 내용: Secret 저장 성공과 새 항목 존재 확인. AWS test:2 JSON 편집기에서 현재 이미지를 검증하고 내부 binding ON/consumer scope/독립 Secret selector만 변경해 test:3 등록, 테스트 서비스 desired1 적용. CloudWatch 기동 실패 확인 후 desired0/running0/pending0 원복 완료.
- 진단: bean 누락은 해결됐으나 Phone eligibility binding configuration is invalid 발생. UI 값의 형식만 검사해 Base64 32바이트 단독 저장 및 버전/ACTIVE_WRITE 접두사 누락 확인. 비밀값은 출력/기록하지 않음.
- 테스트와 결과: 실제 HTTPS502/503 및 기동 실패 확인, 정상 health/JWKS 미검증. 이전 전체 958개 테스트 통과 유지, 이번 제품 로직 변경 없어 Gradle 미실행. JSON 파싱/git diff --check 수행.
- 유지한 계약: 기존 배포 이미지/서명/외부 publisher OFF 보존, IAM 및 운영 변경 없음. 사용자 변경 보존, 예상 밖 파일 변경 없음.
- 결정사항: 비밀값 직접 수정은 사용자에게 인계. 기존 값에 test-v1,ACTIVE_WRITE, 접두사 추가 후 test:3 재기동. 새 키 생성 불필요.
- 위험 요소: 서버는 중지 상태이며 최초 가입/로그인/DB 검증 미완료. 구버전 task의 과거 오류가 같은 로그 목록에 존재하므로 시각/태스크 구분 필요.
- 다음 작업: 사용자 형식 보완 저장 후 task:3 기동 및 health/JWKS 확인. Jira 작업 없음.

## 2026-09-23 — task:3 배포 진단 작업 식별 기록

<!-- codex-turn:01a0cdaa-0452-7252-88aa-880e36c157cd -->

- 브랜치: develop.
- 작업 목표: 테스트 배포 및 keyring 형식 오류 진단 결과 기록.
- 변경 파일: docs/contracts/identity-test-task-definition.draft.json, docs/contracts/identity-branch-deployment.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 이미지 88ff5bed 유지한 test:3 등록/기동. 내부 binding bean 누락은 해결됐으나 keyring 형식 오류로 시작 실패. 비밀값 노출 없이 버전/상태 접두사 누락 확인. 테스트 서비스 desired0/running0/pending0 원복 검증.
- 테스트와 결과: 실제 health502/503 및 시작 실패 확인, JSON 검증/git diff --check 통과. 제품 로직 변경 없어 Gradle 재실행 생략; 직전 전체 958개 테스트 통과.
- 유지한 계약: 운영/IAM/외부 publisher 설정 변경 없음, 비밀값 미기록, 사용자 변경 보존. 예상 밖 변경 없음.
- 결정사항: 사용자에게 기존 값의 접두사 추가 저장 인계. 새 비밀키 생성 불필요.
- 위험 요소: 정상 health/JWKS 및 회원 인증 검증 미완료.
- 다음 작업: 사용자 저장 후 task:3 재기동과 상태 검증.

## 2026-09-23 — 사용자 재저장 후 keyring 형식 재확인

- 브랜치: develop.
- 작업 목표: 키 형식 보완 확인 후 재기동 여부 판단.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Secret 저장 성공 표시 확인 후 화면 재진입/조회. ELIGIBILITY 키 항목은 여전히 Base64 단독, IDENTITY 항목은 버전/상태 포함 구조임을 값 비출력 검사로 확인. 비밀값 화면 닫고 인계.
- 실행한 테스트와 결과: UI 형식 검사에서 ELIGIBILITY 접두사 누락 확인. 제품 변경 없어 Gradle 미실행. git diff --check 수행.
- 유지한 계약: AWS 설정/Secret/서비스 실행 수 변경 없음. 비밀값 미기록 및 기존 수정 보존.
- 결정사항: 형식이 맞기 전 서버를 재기동하지 않음. 정확한 ELIGIBILITY 항목 편집 재안내.
- 위험 요소: 테스트 서버 중지 유지, health/JWKS 검증 미완료. 예상 밖 변경 없음.
- 다음 작업: 사용자 ELIGIBILITY 값 접두사 추가 저장 후 검증과 task:3 재기동.

## 2026-09-23 — keyring 재확인 작업 식별 기록

<!-- codex-turn:01a0cdb0-59de-7e23-8903-8fa69fb5a7a2 -->

- 브랜치: develop.
- 작업 목표: 사용자 재저장 결과 및 기동 보류 상태 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 재조회한 ELIGIBILITY 항목의 버전/상태 접두사 누락을 비밀값 출력 없이 확인하고 정확한 수정 항목 안내.
- 테스트와 결과: UI 형식 검사에서 접두사 누락 확인. 문서만 변경하여 Gradle 미실행, git diff --check 통과.
- 유지한 계약: Secret/운영/서비스 설정 변경 없음. 비밀값 미기록, 사용자 변경 보존.
- 결정사항: 형식 보완 전 재기동하지 않음.
- 위험 요소: 정상 health/JWKS 검증 미완료. 예상 밖 변경 없음.
- 다음 작업: 사용자 ELIGIBILITY 값 보완 저장 후 task:3 재기동 검증.

## 2026-09-23 — 저장 결과 이의에 따른 최신 Secret 재조회

- 브랜치: develop.
- 작업 목표: 사용자 저장 완료와 관측 불일치 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 편집 dialog 없음 확인 후 페이지 전체 reload 및 Secret 값 재조회. 대상 ELIGIBILITY cell 형식만 검사해 쉼표 구분 필드 1개 확인(요구 3개). 값 출력 없이 비밀값 화면 닫고 인계.
- 테스트와 결과: UI read-only 재조회 및 형식 검사 수행. 제품 변경 없어 Gradle 미실행, git diff --check 수행.
- 유지한 계약: Secret/서버/운영 변경 없음. 비밀값 비기록, 기존 수정 보존.
- 결정사항: 저장 대상/필드 일치 여부 확인 필요. 캐시 또는 사용자 실수로 원인을 단정하지 않음.
- 위험 요소: 정상 기동 검증 미완료, 실제 값 변경 원인 미확인. 예상 밖 변경 없음.
- 다음 작업: 사용자와 대상 Secret/ELIGIBILITY 필드 확인 후 형식 정상 시 재기동.

## 2026-09-23 — 최신 Secret 재조회 작업 식별 기록

<!-- codex-turn:01a0cdb2-6518-7e00-9096-ff3d03896df6 -->

- 브랜치: develop.
- 작업 목표: 사용자 저장 결과와 관측 불일치 재확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Secret 페이지 전체 새로고침 후 ELIGIBILITY 항목 형식만 검사, 쉼표 구분 필드 1개 확인. 사용자에게 편집 대상 확인 요청. 비밀값 화면 닫음.
- 테스트와 결과: 읽기 전용 UI 재조회 수행, git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: Secret/서버/운영 변경 없음, 비밀값 비기록, 기존 변경 보존.
- 결정사항: 원인을 단정하지 않고 사용자 편집 화면에서 대상 필드 확인 예정.
- 위험 요소: 저장 관측 불일치 원인 및 정상 서버 기동 미확인. 예상 밖 변경 없음.
- 다음 작업: 대상 필드 확인 및 형식 정상 확인 후 test:3 재기동.

## 2026-09-23 — 테스트 Identity 최초 정상 기동 확인

- 브랜치: develop.
- 작업 목표: 사용자 수정 형식 확인 후 테스트 서버 재기동 및 기본 정상 응답 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md, docs/contracts/identity-branch-deployment.md.
- 수행 내용: Secret 값 비출력 검사에서 keyring 3필드/정상 버전/ACTIVE_WRITE/Base64 32바이트 확인. Secret 편집 없이 기존 test:3 desired1 적용. running1/pending0, CloudWatch 시작 완료(19:03 KST) 및 Mongo 트랜잭션 지원 확인 로그 확인.
- 실행한 테스트와 결과: TLS 검증 유지 HTTPS health200/UP, JWKS RSA/RS256/공개 kid 정상 확인. 초기화 중502/503 이후 정상 전환. 문서/운영 기동만 변경하여 Gradle 재실행 생략, 직전 전체958개 테스트 통과. git diff --check 수행.
- 유지한 계약: 동일 이미지88ff5bed/테스트 task:3, 운영/IAM/Secret 미변경, 외부 publisher OFF 유지. 비밀값 비출력/비기록, 기존 사용자 변경 보존.
- 결정사항: 테스트 서비스1개 실행 유지. 앱 인증 연동 검증 단계로 진행 가능.
- 위험 요소: 앱 Google/phone 가입/로그인/재발급 및 실제 rollback 미검증. CI 기존 실패 이력은 남아 있음. 예상 밖 파일 변경 없음.
- 다음 작업: 프론트 테스트 base URL 안내 및 실제 MEMBER 인증 흐름/LC 연동 검증. 로컬 설정/기록 변경은 사용자 commit/push 대상, Jira 변경 없음.

## 2026-09-23 — 테스트 서버 정상 기동 작업 식별 기록

<!-- codex-turn:01a0cdb6-1084-7162-be93-903d3d595ccf -->

- 브랜치: develop.
- 작업 목표: 사용자 keyring 보완 확인 및 테스트 서버 정상 기동 결과 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md, docs/contracts/identity-branch-deployment.md.
- 수행 내용: 비밀값 비출력 형식 검증 통과 후 test:3 desired1 기동. running1/pending0 및 CloudWatch 시작 완료/Mongo 트랜잭션 지원 확인 로그 확인.
- 테스트와 결과: HTTPS health200/UP, JWKS RS256 공개키 메타데이터 확인. git diff --check 통과. 제품 코드 변경 없어 Gradle 재실행 생략.
- 유지한 계약: 운영/IAM/Secret 변경 없음, 기존 배포 이미지 및 외부 publisher OFF 유지. 비밀값 미기록, 기존 변경 보존.
- 결정사항: 테스트 서비스1개 실행 유지, 앱 인증 검증 단계로 인계.
- 위험 요소: 실제 가입/로그인/재발급 및 rollback 미검증, 기존 CI health 실패 이력 유지. 예상 밖 변경 없음.
- 다음 작업: Android Google/phone 연결 및 MEMBER 가입/로그인/재발급 검증.

## 2026-09-23 — Learning Core용 JWT 및 배포 검증 상태 인계

<!-- codex-turn:01a0cdbc-58c1-7d40-907e-47144abe8ae9 -->

- 브랜치: develop.
- 작업 목표: issuer/JWKS/kid/commit/인증 및 claim 검증 상태 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: AWS test:3 JSON의 issuer/audience/image/release 재확인. HTTPS JWKS에서 공개 kid 및 RS256 확인. git show로 배포 commit의 JwtAccessTokenIssuer를 확인해 UUID 검증/subject/account_type/LC 및 Billing audience 구성 확인. 기존 claim 테스트 assertion 확인.
- 테스트와 결과: 실제 JWKS 조회 정상. 직전 전체958개 테스트 통과 이력과 실제 인증 E2E 미검증을 구분. 이번 코드 변경 없어 Gradle 미실행, git diff --check 수행.
- 유지한 계약: 읽기 전용 조회, 운영/테스트 설정 변경 없음, 실제 토큰/비밀키 비기록. 기존 변경 보존.
- 결정사항: issuer는 실제 배포 설정값 확인이며 실제 발급 토큰의 iss 검증 완료로 표현하지 않음. 가입/로그인/재발급 성공은 아직 미검증.
- 위험 요소: 실제 MEMBER 발급 및 재발급 claim/서명 E2E 검증 필요. 예상 밖 변경 없음.
- 다음 작업: Android Firebase 인증 후 테스트 가입/로그인/재발급 성공과 발급 JWT 계약 검증. Jira 변경 없음.

## 2026-09-23 — 비용 절감을 위한 테스트 서비스 중지

<!-- codex-turn:01a0cdc7-7a37-72f3-bbe3-9a987688a90c -->

- 브랜치: develop.
- 작업 목표: 사용자 요청에 따라 다음 테스트까지 테스트 Identity 실행 중지.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: AWS 테스트 서비스 test:3의 desired count1→0 변경 및 업데이트 성공 확인. 운영/DB/Secret/ALB/배포 revision은 변경하지 않음.
- 테스트와 결과: AWS UI 상태 확인, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 설정과 데이터 보존, 운영 서비스 미변경, 비밀값 미기록. 예상 밖 파일 변경 없음.
- 결정사항: 재개 시 desired1 적용 후 health/JWKS 확인. 별도 리소스 삭제 없음.
- 위험 요소: 중지 동안 테스트 API 접근 불가. ALB/Secret/로그 등 잔여 비용 가능, 서비스0 상태 배포 workflow health 실패 가능.
- 다음 작업: 사용자 테스트 재개 요청 시 기동 후 앱 인증 E2E 검증. Jira 변경 없음.
- 최종 검증: 서비스 새로고침 후 desired0/running0/pending0 확인 완료. 테스트 태스크 종료 확인.

## 2026-09-27 — 구·신 서버 분리 및 업데이트 유예 계획 전달 이력 확인

<!-- codex-turn:01a0e309-e393-78c3-b4f6-520be76424ca -->

- 날짜/브랜치: 2026-09-27, develop. 이번 요청의 Jira 지정 없음.
- 작업 목표: 사용자 제시 세 가지 전환 계획의 과거 전달 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md (기록만).
- 수행 내용: 2026-09-22 구버전 주소 분리·웹뷰 안내·1주 유예 검토 기록 및 guest-app-update-transition-review.md를 확인했다. 구·신 서버 분리, 피드백 진입 전 안내, 사용량에 따른 유예 조정 계획이 이미 기록되어 있으며 후속 피드백 result.updateRequired 계약도 확인했다.
- 실행한 테스트와 결과: 저장소 기록 검색/원문 대조 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 기존 작업/과거 기록 보존. API·서버·배포·Jira·기능 설정 변경 없음. 비밀정보 미기록.
- 결정사항: 이전에 전달한 계획이라고 답변하되 계획 합의와 구현·배포 완료를 구분한다.
- 위험 요소: 이번에는 실제 Learning Core/웹뷰 구현 및 운영 전환 상태를 확인하지 않음. 예상 밖 변경 없음.
- 다음 작업: 별도 요청이 있으면 해당 계획 기준으로 실제 구현/배포 준비 상태를 점검한다.

## 2026-09-27 — SNS·10초 챌린지 선출시와 무료 모의고사·결제 후속 출시 검토

<!-- codex-turn:01a0e309-e393-78c3-b4f6-520be76424ca -->

- 날짜/브랜치: 2026-09-27, develop. 별도 Jira 지정 없음.
- 작업 목표: SNS 로그인·10초 챌린지를 먼저 출시하고 전화번호당 1회 무료 모의고사와 결제를 후속 업데이트로 분리할 수 있는지 설명한다.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md (분석 기록만).
- 조사 근거: Identity FirebaseSignupService/FirebaseGuestUpgradeService의 전화번호·eligibility hasher 의존성, PhoneEligibilityPublisherConfiguration의 별도 활성화 조건, identity-branch-deployment.md의 내부 binding ON/외부 publisher OFF 구분 확인. Learning Core application.yml/ChallengeController/ChallengeService/ExamServiceImpl을 읽어 독립 challenge flag와 Billing OFF 시 기존 모의고사 생성 분기를 확인했다. Learning Core 파일은 수정하지 않았다.
- 구현 내용: 없음. 단계 출시 방향은 가능하되 배포 검증 완료와 구분하고 전화번호 인증 자체의 후속 연기는 별도 변경임을 안내한다.
- 테스트와 결과: 코드·설정 읽기 대조 및 git diff --check. 코드 변경 없어 Gradle 미실행, 외부 서비스/실제 앱 E2E 미수행.
- 유지한 계약: Identity와 Learning Core/Billing 도메인 경계, 현재 가입 인증·내부 식별 계약, 기존 작업과 기록 보존. 서버·기능 flag·Jira 변경 없음. 비밀정보 미기록.
- 결정사항: 1차 SNS/챌린지 및 기존 모의고사 정책 유지, 2차 무료 자격/결제 동시 도입을 권고한다. Billing OFF만으로 모의고사가 차단되거나 신규 1회 제한이 적용되는 것은 아니다. 사용자 출시 범위 확정 또는 구현 승인으로 간주하지 않는다.
- 위험 요소: 기존 Guest 기록 승계, 앱 전환, 챌린지 AI/저장소/실기기 연동 확인 필요. 2차 도입 시 기존 사용자에게 새 무료 1회 부여 여부와 이전 이용의 소급 계산 정책, 지연 이벤트/기존 전화번호 binding 인계 검증 필요. 예상 밖 변경 없음.
- 다음 작업: 1차 모의고사 정책과 2차 기존 사용자 무료 자격 기준을 확정한 후 별도 출시 체크리스트 및 환경별 기능 설정을 검증한다. 이번 배포/commit/push 없음.

## 2026-09-27 — 단계 출시 검토 작업 식별 기록 보완

<!-- codex-turn:01a0e30b-5569-7513-b978-5cb92ec91400 -->

- 날짜/브랜치: 2026-09-27, develop. Jira 지정 없음.
- 작업 목표: SNS 로그인·10초 챌린지 선출시와 무료 모의고사·결제 후속 출시 검토의 현재 turn 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 앞선 분석은 현재 작업의 결과이며 이전 turn marker로 기록된 것을 과거 기록 수정 없이 새 항목으로 보완했다. 기능 분리 가능성과 현재 가입의 전화번호 인증/내부 식별 의존성, 기존 사용자 무료 자격 정책 필요를 확인했다.
- 테스트와 결과: git diff --check 통과. 분석 및 기록만 변경하여 Gradle 미실행, 실제 앱/운영 E2E 미검증.
- 유지한 계약: 과거 기록과 기존 작업 보존. 코드/API/서버/기능 설정/배포/Jira 변경 없음. 비밀정보 미기록.
- 결정사항: 1차 SNS·챌린지, 2차 무료 자격·결제 적용을 권고했으며 구현 승인이나 출시 준비 완료로 간주하지 않는다.
- 위험 요소: Guest 승계·챌린지 연동·기존 사용자 자격 이행 검증 필요. 예상 밖 변경 없음.
- 다음 작업: 기존 모의고사 정책과 2차 무료 자격 기준 결정 후 출시 검증 범위 확정.

## 2026-09-28 — 테스트 서버 Swagger 사용 조건 확인

<!-- codex-turn:01a0e5b3-b827-74f0-b3a7-be6caf06d2d0 -->

- 브랜치: develop.
- 작업 목표: 테스트 서버 재기동 시 Swagger 사용 가능 여부 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 조사 내용: 저장된 task draft SWAGGER_ENABLED=false, application.yml UI /swagger-ui.html 및 spec /v3/api-docs, SecurityConfig 문서 GET permitAll 확인.
- 테스트와 결과: 설정/코드 읽기 및 git diff --check. 분석/기록만 수행하여 Gradle 미실행. AWS 실시간 상태 조회 없음.
- 유지한 계약: 코드/API/AWS/기능 설정/서비스 기동 변경 없음. 기존 변경 보존 및 비밀정보 미기록.
- 결정사항: 사용하려면 테스트 Swagger 활성 설정 및 재배포/기동 필요. 보호 API는 Identity Access Token 사용, Firebase 인증은 앱 SDK에서 별도 진행.
- 위험 요소: Swagger 활성화 시 공개 문서 접근 가능하므로 접근 제한 검토 필요. 문서/실행 API는 배포 버전 기준. 예상 밖 변경 없음.
- 다음 작업: 사용자 활성화 요청 시 접근 범위를 확정한 뒤 테스트 설정 적용 및 UI/API 검증.

## 2026-09-28 — 테스트 Swagger 활성화 설정 준비

- 브랜치: develop.
- 작업 목표: Learning Core 준비 후 함께 기동할 테스트 Identity의 Swagger 활성화 준비.
- 변경 파일: docs/contracts/identity-test-task-definition.draft.json, src/test/java/web/tosunsaeng/identity/deployment/DeploymentTargetTests.java, docs/contracts/identity-branch-deployment.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 테스트 draft SWAGGER_ENABLED=true, 배포 설정 회귀 assertion 추가. 기존 사용자 변경 보존. AWS 세션 만료 확인 후 ISB 재로그인 페이지 인계.
- 테스트: ./gradlew clean test --no-daemon 실행. 최종 결과는 아래 추가 기록.
- 유지한 계약: 보호 API JWT 인증 유지, 운영/Secret/IAM/실행 수 변경 없음. commit/push 미수행, 비밀값 미기록.
- 결정사항: 테스트 실행은 지금 하지 않음. 로그인 후 현재 AWS revision에 설정만 적용하고 desired0 유지.
- 위험 요소: 실제 ECS 설정 미반영, live Swagger 미검증. 문서 공개 활성화 요청이며 실제 데이터 API는 인증 유지. 기존 미커밋 변경 외 예상 밖 변경 없음.
- 다음 작업: 사용자 AWS 재로그인 후 테스트 설정 적용, 나중에 기동 요청 시 health/UI/spec 검증. Jira 연결 없음.
- 최종 테스트 결과: ./gradlew clean test --no-daemon BUILD SUCCESSFUL(31초), git diff --check 통과. 실제 AWS 반영은 로그인 대기로 미완료.

## 2026-09-28 — Swagger 활성화 준비 작업 식별 기록

<!-- codex-turn:01a0e5b5-3981-76a2-b63d-3e2160a31201 -->

- 브랜치: develop.
- 작업 목표: 테스트 Swagger 활성화 준비 결과와 AWS 반영 대기 상태 기록.
- 변경 파일: docs/contracts/identity-test-task-definition.draft.json, src/test/java/web/tosunsaeng/identity/deployment/DeploymentTargetTests.java, docs/contracts/identity-branch-deployment.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 로컬 테스트 draft의 SWAGGER_ENABLED=true 및 회귀 assertion 추가. AWS 세션 만료로 로그인 페이지 인계.
- 테스트와 결과: ./gradlew clean test --no-daemon 성공, git diff --check 통과.
- 유지한 계약: 운영/Secret/IAM/실행 수 변경 없음, 보호 API JWT 인증 유지. 비밀값 미기록, 기존 변경 보존.
- 결정사항: 사용자 재로그인 후 테스트 설정만 반영하고 desired0 유지. 이번 실제 배포/기동 없음.
- 위험 요소: AWS 반영 및 live Swagger 검증 미완료. 예상 밖 변경 없음.
- 다음 작업: 재로그인 후 최신 테스트 revision 확인 및 Swagger 설정 반영, 이후 사용자 기동 요청 시 정상 응답 검증.

## 2026-09-28 — AWS 테스트 Swagger 설정 실제 적용

<!-- codex-turn:01a0e5bb-8e86-7c92-bf1d-ce1147c7225d -->

- 브랜치: develop.
- 작업 목표: 재로그인 후 테스트 Swagger 활성화 설정을 반영하되 서버 중지 유지.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md, docs/contracts/identity-branch-deployment.md.
- 수행 내용: AWS 현재 테스트 서비스 test:3/desired0 확인. 현재 revision JSON에서 SWAGGER_ENABLED만 true로 변경하여 test:4 생성, 서비스에 적용. 성공 알림 및 test:4/desired0/running0/pending0 확인.
- 테스트와 결과: AWS 저장 및 연결 상태 UI 검증, git diff --check 수행. 이번 제품 코드 변경 없어 Gradle 재실행 생략(직전 clean test 성공). 서버를 켜지 않아 live Swagger/health 미검증.
- 유지한 계약: 이미지88ff5bed/인증/Secret/IAM/네트워크 보존, 운영 미변경. 기존 사용자 변경 보존 및 비밀값 미기록.
- 결정사항: 테스트 Swagger 설정 준비 완료, 사용자 요청 전 기동하지 않음. 자동 기동 예약 없음.
- 위험 요소: Swagger 문서는 기동 후 공개 접근되며 보호 API JWT 인증은 유지. 실제 UI/spec 응답은 미검증. 예상 밖 변경 없음.
- 다음 작업: Learning Core 준비 후 사용자 기동 요청 시 desired1 및 health/JWKS/Swagger 검증. Jira 변경 없음.

## 2026-09-28 — Swagger 활성 테스트 Identity 기동

<!-- codex-turn:01a0e5dc-f4ed-7513-bd4c-ac48de99b1bb -->

- 브랜치: develop.
- 작업 목표: 사용자 요청에 따라 테스트 서버 기동 및 공개 진단 응답 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: test:4 유지하며 테스트 서비스 desired0→1 적용. AWS running1/pending0 확인. 운영 서비스/이미지/Secret/IAM/네트워크 변경 없음.
- 테스트와 결과: 초기502/503 이후 HTTPS health200/UP. Swagger UI 리다이렉트 후200, OpenAPI JSON3.1.0/Identity API/23경로 확인, JWKS RSA/RS256/테스트 kid 확인. git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 사용자 API 인증 유지, 테스트 데이터/설정 보존, 비밀값 미기록. 기존 변경 보존, 예상 밖 파일 변경 없음.
- 결정사항: 테스트 태스크1개 실행 유지, Swagger URL 인계. 실행 중 Fargate 비용 발생.
- 위험 요소: 실제 Google/phone 가입/로그인/재발급 및 Learning Core E2E는 미검증. Swagger UI HTTP/명세 응답만 확인했고 브라우저 Try it out은 실행하지 않음.
- 다음 작업: 프론트 또는 Swagger를 통한 승인된 테스트 계정 인증 검증. Jira 변경 없음.

## 2026-09-28 — Identity·Learning Core 사전 HTTP smoke 및 Postman collection

<!-- codex-turn:01a0e621-9da6-7d80-b98a-3fa688b86cc0 -->

- 브랜치: develop.
- 작업 목표: 앱 연동 전 양쪽 테스트 API 사전 검증.
- 변경 파일: docs/postman/identity-learning-smoke.postman_collection.json, docs/postman/README.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 제공 LC 테스트 주소/계정 미준비 확인. Postman 제어 권한 부재로 동일 HTTP 요청 직접 검사. health/JWKS/인증 거절 테스트와 공개 명세 조회, 비밀값 없는10요청 collection 작성.
- 테스트와 결과: 두 health200/UP, Identity 프로필 무토큰/잘못된 토큰401, exchange 빈 body/잘못된 token401, 잘못된 refresh401, LC today 무토큰/잘못된 token401. JWKS RS256 테스트 kid 정상. collection JSON/10개 요청 확인 및 git diff --check. 제품 코드 변경 없어 Gradle 미실행. Postman runner는 실행하지 않음.
- 유지한 계약: 실제 토큰/비밀값 비기록. 회원 생성/정상 세션 회전/데이터 삭제/설정 변경 없음. 기존 변경 보존, 예상 밖 변경 없음.
- 결정사항: 계정 없는 기본 검사와 인증 성공 E2E를 구분. 임의 MEMBER/custom token 우회 없음.
- 위험 요소: 실제 회원 가입/로그인/refresh/LC 성공 및 GUEST403 미검증. Identity live OpenAPI의 http 서버 주소 관측, HTTPS 명시 사용 및 후속 수정 검토 필요.
- 다음 작업: Firebase SDK 테스트 계정 Google 인증/phone link 준비 후 승인된 테스트 사용자로 MEMBER E2E 진행. Postman 제어 권한 또는 사용자 import 실행 필요. Jira 변경 없음.

## 2026-09-28 — Firebase·Identity 테스트 회원 생성 절차 안내

<!-- codex-turn:01a0e625-5503-7271-a27a-251d2cc3a321 -->

- 브랜치: develop.
- 작업 목표: 앱 연동 전 테스트 계정을 만드는 올바른 순서 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 조사/안내: 기존 프론트 가입 계약 확인. Google SDK 인증, exchange enrollment, 같은 UID phone link, 강제 갱신 후 signup 순서 안내. Firebase 콘솔 테스트 번호 등록과 회원 생성 구분. 최소 SDK 테스트 화면이 별도로 필요함을 명시.
- 테스트와 결과: 계약 문서 읽기 및 git diff --check. 안내/기록만 변경하여 Gradle 미실행.
- 유지한 계약: 계정/데이터/서버 설정 생성·변경 없음. 실제 전화번호/OTP/credential/토큰 비기록.
- 결정사항: 테스트 번호는 실제 SMS 없이 고정 검증 코드로 사용하되 SDK 연결과 가입 필수 정보/정책 동의는 생략하지 않음.
- 위험 요소: 최소 테스트 화면은 아직 미구현, 웹 사용 시 앱 등록/허용 도메인/reCAPTCHA 준비 필요. 실제 MEMBER E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 요청 시 테스트 전용 Firebase SDK 화면 준비 후 인증/가입 성공 경로 검증. Jira 변경 없음.

## 2026-09-28 — 테스트 계정 생성 안내 작업 식별 기록 보완

<!-- codex-turn:01a0e625-c8ec-7c70-a91b-565707c268de -->

- 브랜치: develop.
- 작업 목표: 테스트 회원 생성 절차 안내의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Firebase 테스트 전화번호 등록, Google SDK 로그인, exchange enrollment, 동일 UID phone link, 갱신된 ID Token으로 Identity signup 절차 안내 기록. 앞선 식별자는 현재 hook 제공 식별자로 이 추가 기록에서 보완하며 과거 기록은 유지.
- 테스트와 결과: 문서만 변경하여 Gradle 미실행, git diff --check 수행.
- 유지한 계약: 계정/서버/외부 설정 변경 없음, 비밀값 미기록, 기존 변경 보존.
- 결정사항: SDK 테스트 화면과 실제 회원 생성은 아직 수행하지 않음.
- 위험 요소: MEMBER 인증 E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 요청 시 최소 Firebase SDK 테스트 화면 준비 및 가입 검증.

## 2026-09-28 — Firebase 테스트 전화번호 등록 확인

<!-- codex-turn:01a0e630-f709-7f23-b4ff-a6fe4bca4d3c -->

- 브랜치: develop.
- 작업 목표: 사용자가 열어 둔 Firebase 탭에서 테스트 전화번호 등록 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: to-teacher-firebase 로그인 방법에서 Google/전화 활성화와 테스트 전화번호·인증 코드 저장 항목 1쌍 확인. 콘솔 설정 저장/변경 및 사용자 생성 없음.
- 테스트와 결과: 콘솔 UI 읽기 확인 완료. 문서 기록만 변경하여 Gradle 미실행. git diff --check 수행.
- 유지한 계약: 번호/인증 코드/토큰/credential을 저장소에 기록하지 않음. 기존 변경 보존.
- 결정사항: 테스트 번호 등록 단계 완료, SDK Google 인증과 같은 UID 전화 연결 및 Identity 가입은 별도 진행.
- 위험 요소: 실제 MEMBER E2E 미검증. 열린 콘솔 Spark 표시는 관측 사실이며 실제 결제 연결 상태 재검증은 이번 범위 밖. 예상 밖 코드 변경 없음.
- 다음 작업: 최소 SDK 테스트 화면 또는 앱으로 Google 로그인→exchange→phone link→signup 검증. Jira 변경 없음.

## 2026-09-28 — 로컬 Firebase 웹 테스트 화면 필요성 안내

<!-- codex-turn:01a0e638-bf2e-7b93-b16e-6b9d4b7d43ce -->

- 브랜치: develop.
- 작업 목표: 사용자 직접 구현 필요 여부와 간단한 웹 테스트 방법 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Codex가 최소 로컬 웹 화면을 구현할 수 있으며 공개 배포는 불필요함을 안내. Google 인증→exchange→동일 UID phone link→signup 순서 유지. 웹 앱 등록/허용 도메인/전화 인증 웹 설정/CORS는 확인 필요.
- 테스트와 결과: 안내와 기록만 수행, Gradle 미실행. git diff --check 수행.
- 유지한 계약: 비밀값 비기록, 서비스 계정 개인키를 웹에 넣지 않음, 외부 설정/계정 생성 및 코드 구현 없음.
- 결정사항: 실제 구현은 후속 요청 시 진행. Android 앱 인증 검증을 웹 테스트로 대체하지 않음.
- 위험 요소: 현재 웹 앱 등록/허용 도메인/CORS 미확인, 실제 인증 성공 미검증. 기존 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 승인 시 최소 테스트 화면 구현과 필요한 Firebase 웹 설정 확인. Jira 변경 없음.

## 2026-09-28 — 로컬 Firebase 인증 테스트 화면 구현

<!-- codex-turn:01a0e638-bf2e-7b93-b16e-6b9d4b7d43ce -->

- 브랜치: develop.
- 작업 목표: 앱 프론트 연동 전에 사용자 직접 Google 인증/전화 연결/가입을 진행할 최소 웹 화면 구현.
- 변경 파일: tools/auth-test/index.html, app.js, session.mjs, server.mjs, app.test.mjs, server.test.mjs, README.md 및 docs/codex/WORKLOG.md, CURRENT_STATE.md.
- 구현 내용: Firebase in-memory 인증, 같은 UID phone link, enrollment 만료 검사, 직접 동의 signup, pair 동시 교체, single-flight 재발급과 UUID 요청 ID, 실패 시 이전 refresh 재사용 금지. 프로필/LC today는 읽기 smoke만 제공. 고정 테스트 HTTPS 5개 API만 허용하는 loopback proxy로 운영 CORS 변경 없이 사용. Host/Origin/전용 헤더/본문 크기 제한, redirect 차단, 로그 민감정보 비출력.
- 테스트와 결과: 최초 sandbox의 포트/Gradle 캐시 접근 제한 후 승인된 재실행에서 Node server 2개 및 mocked UI 1개 성공, node --check 성공, ./gradlew clean test BUILD SUCCESSFUL. git diff --check 수행. localhost 서버 실행 완료. Chrome 자동 열기는 ERR_BLOCKED_BY_CLIENT로 차단되어 실제 화면 렌더링 검증 실패, 보호 우회 없음.
- 유지한 계약: Identity 서버 API/보안 설정/운영 배포 미변경. 실제 토큰/키/전화번호/코드를 파일에 저장하지 않음. 계정 생성·실제 로그인·약관 동의는 수행하지 않음.
- 결정사항: 이 도구는 테스트 가상 번호 전용이며 공개 배포하지 않는다. Stage 9 응답 복구/Android SDK 전체 검증은 별도. 서버 세션 폐기 없이 로컬 정보만 지우는 버튼임을 명시.
- 위험 요소: Firebase 웹 앱 구성/localhost 승인 도메인/정책 버전/실제 OAuth 및 MEMBER E2E 미확인. 브라우저 개발자 도구에는 credential이 존재하므로 공유 금지. 기존 dirty 문서/배포 테스트 변경 보존, 이번 예상 밖 변경 없음.
- 다음 작업: 사용자가 로컬 페이지 접근 확인 후 웹 앱 구성과 승인 도메인 준비, 직접 Google 로그인·약관 동의 후 실제 가입 검증. 별도 서비스 배포 불필요. Jira 변경 없음.

## 2026-09-28 — 로컬 인증 테스트 화면 작업 식별 기록 보완

<!-- codex-turn:01a0e639-5ecc-7283-8bf4-92e25670f712 -->

- 브랜치: develop.
- 작업 목표: 이번 로컬 인증 테스트 도구 구현 결과를 현재 작업 식별자로 기록.
- 변경 파일: tools/auth-test의 화면·SDK 흐름·로컬 서버·테스트·README 및 docs/codex/WORKLOG.md, CURRENT_STATE.md.
- 구현 내용: Google 인증→exchange→같은 UID 가상 전화번호 연결→signup, 프로필/재발급/LC 읽기 smoke. 테스트 HTTPS 목적지 제한, loopback 바인딩, Host/Origin 가드, 토큰 메모리 보관 및 비출력.
- 테스트와 결과: Node 3개 테스트, 문법 검사, ./gradlew clean test 및 git diff --check 통과. 로컬 서버 기동 완료. Chrome 자동 열기는 ERR_BLOCKED_BY_CLIENT로 차단되어 실제 렌더링/인증 E2E 미검증.
- 유지한 계약: 운영/API/Firebase 외부 설정 변경 없음. 비밀값 비기록. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- 결정사항: 별도 공개 배포 없이 로컬 전용 사용. 실제 로그인·약관 동의는 사용자가 수행. 이전 기록은 유지하고 작업 식별자만 이 항목으로 보완.
- 위험 요소: 웹 앱 구성/localhost 승인 도메인/정책 버전 확인 및 실제 회원 인증 검증 필요. Stage 9 복구/Android SDK 검증은 별도.
- 다음 작업: 로컬 화면 사용자 접근 확인 후 Firebase 웹 설정 준비. Jira 변경 없음.

## 2026-09-28 — Firebase 웹 구성 위치 및 JSON 입력 안내

<!-- codex-turn:01a0e645-f435-75a1-b0ad-dcd4d36ecd6b -->

- 브랜치: develop.
- 작업 목표: 사용자가 찾지 못한 Firebase 웹 구성 메뉴와 JSON 형식 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 프로젝트 일반 설정→내 앱→웹 앱→SDK 설정 및 구성 경로와 웹 앱 부재 시 등록 방법 안내. Hosting 불필요, 서비스 계정 JSON과 구분. JavaScript 선언을 제외하고 키를 큰따옴표로 감싼 JSON만 입력하도록 안내.
- 테스트와 결과: 문서 안내만 수행하여 Gradle 미실행, git diff --check 확인.
- 유지한 계약: 실제 키/인증정보 미기록, Firebase 설정/계정 생성 없음. 기존 변경 보존.
- 결정사항: 기존 Firebase 프로젝트에서 웹 앱을 사용하며 새 프로젝트 불필요.
- 위험 요소: 웹 앱 현재 등록 여부는 직접 조회하지 않아 미확인. 실제 인증 E2E 미완료. 예상 밖 변경 없음.
- 다음 작업: 사용자 웹 앱 구성 확인 후 로컬 화면 설정 및 승인 도메인 확인. Jira 변경 없음.

## 2026-09-28 — 사용자 제공 Firebase 웹 구성 형식 확인

- 브랜치: develop.
- 작업 목표: 제공된 코드가 로컬 인증 화면에 필요한 웹 구성인지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 웹 appId와 구성 필드를 기준으로 웹 SDK 설정임을 확인. 실제 값 재출력 없이 네 필드의 JSON 입력 방식과 Analytics 불필요 안내.
- 테스트와 결과: 제공된 구조와 로컬 화면 입력 계약 확인, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 실제 키/토큰 비기록, Firebase 설정/인증/배포 변경 없음.
- 결정사항: 기존 로컬 화면에 공개 웹 설정만 입력하며 서비스 계정 사용 금지 유지.
- 위험 요소: 실제 로그인/승인 도메인/E2E 미확인. 기존 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 사용자 JSON 입력 후 Google 로그인 진행 및 결과 확인. Jira 변경 없음.

## 2026-09-28 — Firebase 웹 구성 확인 작업 식별 기록 보완

<!-- codex-turn:01a0e648-0866-7b60-9158-82ec6a24ecb4 -->

- 브랜치: develop.
- 작업 목표: 웹 구성 형식 확인 결과의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 제공 구성이 웹 SDK용임을 확인하고 로컬 화면에 필요한 네 필드 JSON 입력 및 Google 로그인 순서 안내. 실제 값은 기록하지 않음.
- 테스트와 결과: 구조 확인 및 git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 인증/외부 설정/배포 변경 없음, 비밀정보 비기록, 과거 기록과 기존 변경 보존.
- 결정사항: Analytics 초기화와 서비스 계정 JSON은 불필요.
- 위험 요소: 실제 Google 로그인 및 E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 설정 적용 후 로그인 결과 확인. Jira 변경 없음.

## 2026-09-28 — 로컬 Firebase 설정 입력 오류 진단

- 브랜치: develop.
- 작업 목표: 설정 적용 실패 원인 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 지정 localhost 탭에서 Google 버튼 비활성과 일반 오류 확인. 입력 객체의 속성명 큰따옴표 누락/마지막 쉼표가 JSON 구문과 맞지 않음을 확인하여 수정 방법 안내. 실제 값 기록 없음.
- 테스트와 결과: UI 입력 구조와 기존 JSON.parse 구현 대조. 코드 변경 없어 Gradle 미실행, git diff --check 수행.
- 유지한 계약: 외부 인증/설정 변경 및 사용자 입력 수정 없음. 기존 변경 보존.
- 결정사항: 현재 오류는 로그인 이전 JSON 입력 파싱 문제로 판단. 인증 성공 여부와 구분.
- 위험 요소: 일반 오류 문구가 구문 오류를 명확히 설명하지 못함. 실제 로그인 미검증, 예상 밖 변경 없음.
- 다음 작업: JSON 문법 수정 후 설정 적용 및 Google 로그인 확인. Jira 변경 없음.

## 2026-09-28 — 설정 입력 오류 진단 작업 식별 기록 보완

<!-- codex-turn:01a0e64a-15d6-7291-804b-372af08e97a4 -->

- 브랜치: develop.
- 작업 목표: 로컬 Firebase 설정 실패 진단의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: UI에서 JSON 속성명 따옴표 누락과 마지막 쉼표를 확인하여 수정 안내. 실제 설정값은 기록하지 않음.
- 테스트와 결과: 화면과 JSON 입력 계약 대조, git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 입력/인증/외부 설정 변경 없음, 기존 기록과 변경 보존, 비밀값 비기록.
- 결정사항: 로그인 이전 입력 파싱 문제로 진단.
- 위험 요소: 실제 로그인 미검증, 일반 오류 메시지 개선 여지 있음. 예상 밖 변경 없음.
- 다음 작업: 사용자 JSON 수정 후 설정 적용 확인. Jira 변경 없음.

## 2026-09-28 — Google 로그인 완료 여부 확인

- 브랜치: develop.
- 작업 목표: 사용자 로그인 시도 후 완료 상태 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 로컬 화면 결과 auth/popup-closed-by-user와 exchange 등 후속 버튼 비활성 확인. Google 인증 결과가 로컬 앱에 전달되어 완료된 상태가 아님을 안내.
- 테스트와 결과: UI 읽기 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 인증 재시도/계정 생성/외부 설정 변경 없음, 비밀값 비기록, 기존 변경 보존.
- 결정사항: 팝업 완료까지 유지하며 재시도하고 계속 실패하면 일반 Chrome에서 확인 권장.
- 위험 요소: 팝업 종료 원인은 오류 코드만으로 확정 불가. 실제 MEMBER E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: Google 인증 완료 문구와 exchange 버튼 활성화 확인. Jira 변경 없음.

## 2026-09-28 — 로그인 상태 확인 작업 식별 기록 보완

<!-- codex-turn:01a0e64c-9f49-7db2-a549-9c004ac7b6a8 -->

- 브랜치: develop.
- 작업 목표: 로그인 완료 여부 확인의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: auth/popup-closed-by-user 및 후속 버튼 비활성을 확인하여 인증 미완료 안내. 팝업 인증 완료 후 재확인하도록 설명.
- 테스트와 결과: UI 읽기 확인, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, 외부 인증/설정 변경 없음, 과거 기록 및 기존 변경 보존.
- 결정사항: 현재 화면은 Google 인증 완료 결과를 받지 못한 상태로 판단.
- 위험 요소: 팝업 종료의 구체적 원인과 실제 인증 E2E 미확인. 예상 밖 변경 없음.
- 다음 작업: 사용자 재시도 후 Google 인증 완료와 exchange 활성 여부 확인. Jira 변경 없음.

## 2026-09-28 — 반복 Google 팝업 인증 실패 조사

- 브랜치: develop.
- 작업 목표: 반복 로그인 실패의 단계와 설정 오류 가능성 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: IAB 로컬 화면 popup-closed-by-user 및 exchange 비활성 확인. 수집된 warn/error 로그 없음, 남은 OAuth 팝업 없음. Firebase 승인 도메인 UI에서 localhost와 기본 authDomain 등록 확인. 로컬 코드의 popup SDK 호출/완료 분기 확인.
- 테스트와 결과: UI/코드 읽기 진단 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 인증 재시도/설정 수정/계정 생성/배포 없음. 비밀값 비기록 및 기존 변경 보존.
- 결정사항: 승인 도메인 누락은 배제. SDK 팝업 완료 이전 단계 실패로 판단하며 내장 브라우저 연동 문제는 미확정 가설. 일반 Chrome 비교 요청.
- 위험 요소: 오류 코드만으로 실제 팝업 종료 원인은 확정 불가. 수집 로그가 없다는 것이 브라우저 오류 전체 부재를 보증하지 않음. 실제 인증 E2E 미검증, 예상 밖 변경 없음.
- 다음 작업: 동일 설정으로 일반 Chrome 로그인 비교, 실패 시 팝업 화면과 오류 코드 확인. Jira 변경 없음.

## 2026-09-28 — 반복 팝업 실패 조사 작업 식별 기록 보완

<!-- codex-turn:01a0e64d-f3dd-7173-9711-fb6df33043bb -->

- 브랜치: develop.
- 작업 목표: 반복 Google 팝업 실패 조사 결과의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 로컬 화면 popup-closed-by-user 및 exchange 비활성, 수집된 경고/오류 로그 부재, Firebase localhost 승인 도메인 등록 확인. 일반 Chrome 비교를 안내.
- 테스트와 결과: UI·코드 읽기 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀값 비기록, 설정/인증/배포 변경 없음, 과거 기록과 기존 변경 보존.
- 결정사항: 도메인 누락은 배제하고 내장 브라우저 팝업 처리 문제는 미확정 가설로 유지.
- 위험 요소: 실제 팝업 종료 원인과 인증 E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: 일반 Chrome에서 같은 설정으로 로그인 비교 후 원인 추가 확인. Jira 변경 없음.

## 2026-09-28 — Chrome 로그인 성공 비교 결과 설명

- 브랜치: develop.
- 작업 목표: 내장 브라우저 실패와 일반 Chrome 성공의 차이 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 보고로 Chrome 로그인 성공 확인. Firebase popup 인증은 인증 후 원래 페이지로 결과 전달이 필요하며 브라우저별 팝업/창 간 통신/저장소 정책 차이가 영향을 줄 수 있음을 설명. 정확한 하위 원인은 미확정으로 구분.
- 테스트와 결과: 기존 UI 진단 및 사용자 비교 결과 기반 분석, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀값 비기록, 코드/외부 설정/계정 직접 변경 없음. 기존 변경 보존.
- 결정사항: 로컬 웹 인증 테스트는 일반 Chrome에서 계속 진행. Android SDK 실제 검증은 별도.
- 위험 요소: Identity 가입/로그인 교환 및 MEMBER E2E 성공은 아직 확인하지 않음. 예상 밖 변경 없음.
- 다음 작업: Chrome에서 Identity 로그인/가입 준비 결과 확인. Jira 변경 없음.

## 2026-09-28 — Chrome 로그인 비교 작업 식별 기록 보완

<!-- codex-turn:01a0e64f-ec02-77b2-a1aa-ebe93c7345e5 -->

- 브랜치: develop.
- 작업 목표: Chrome 로그인 성공 비교 설명의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 보고에 따른 Chrome 로그인 성공과 내장 브라우저 팝업 실패 차이 설명. 정확한 내부 원인은 미확정으로 유지.
- 테스트와 결과: 사용자 비교 결과 및 기존 UI 진단 기반 분석, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, 외부 설정/인증/배포 변경 없음, 과거 기록과 기존 변경 보존.
- 결정사항: 로컬 웹 테스트는 Chrome에서 진행하며 Google 로그인과 Identity 가입 완료는 구분.
- 위험 요소: Identity exchange/가입 및 MEMBER E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: Chrome에서 Identity 로그인/가입 준비 결과 확인. Jira 변경 없음.

## 2026-09-28 — 테스트 전화번호 국제 형식 안내

<!-- codex-turn:01a0e651-6dce-7960-83cf-2ef04432c83a -->

- 브랜치: develop.
- 작업 목표: 국내 형식 전화번호 입력 거절 이유 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: app.js 국제 번호 정규식 확인. 한국 번호 맨 앞 0 제거 후 국가번호 +82 사용, Firebase 등록 가상 번호 일치 및 고정 코드 사용 안내.
- 테스트와 결과: 입력 검사 코드 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 실제 번호/인증 코드 비기록, 인증/외부 설정 변경 없음, 기존 변경 보존.
- 결정사항: 형식 수정만으로 임의 번호가 테스트 번호로 인정되지는 않음을 구분.
- 위험 요소: 현재 브라우저 오류 자체는 재조회하지 않았으며 실제 phone link 미검증. 예상 밖 변경 없음.
- 다음 작업: 등록된 가상 번호를 국제 형식으로 입력 후 테스트 연결 확인. Jira 변경 없음.

## 2026-09-28 — 국제 번호 안내 작업 식별 기록 보완

<!-- codex-turn:01a0e651-1f41-7a11-95fd-9e79532fbb82 -->

- 브랜치: develop.
- 작업 목표: 국제 번호 입력 안내의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 로컬 입력 정규식 확인 후 국제 형식 변환 및 Firebase 등록 가상 번호 일치 조건 안내. 실제 번호/코드는 기록하지 않음.
- 테스트와 결과: 코드 읽기 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 설정/인증 변경 없음, 기존 기록과 변경 보존.
- 결정사항: 앞선 식별자를 이번 hook 제공 식별자로 보완.
- 위험 요소: 실제 전화 연결 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 국제 형식 입력 후 연결 결과 확인. Jira 변경 없음.

## 2026-09-28 — 테스트 가입 정책 버전 입력 안내

- 브랜치: develop.
- 작업 목표: 테스트 화면 정책 버전과 AWS 설정의 관계 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 테스트 task draft에서 개인정보 privacy-v1, 이용약관 term-v1 확인. 실제 실행 task 환경변수의 동일 값 사용과 약관 확인 후 직접 동의 안내.
- 테스트와 결과: 설정 파일 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 실제 비밀값 비기록, 외부 설정/동의/가입 요청 변경 또는 실행 없음. 기존 변경 보존.
- 결정사항: 가입을 통과시키기 위해 서버 정책 버전을 임의 변경하지 않음.
- 위험 요소: 현재 live ECS 환경변수는 이번에 재조회하지 않음. 예상 밖 변경 없음.
- 다음 작업: 실행 중 task 정책 버전 일치 확인 후 사용자가 가입 진행. Jira 변경 없음.

## 2026-09-28 — 정책 버전 안내 작업 식별 기록 보완

<!-- codex-turn:01a0e653-09c5-7f00-b670-34ebc093fcef -->

- 브랜치: develop.
- 작업 목표: 가입 정책 버전 안내의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 테스트 배포 초안의 정책 버전을 확인하고 실제 실행 task 설정과 일치해야 함을 안내. live AWS 재확인과 구분.
- 테스트와 결과: 설정 파일 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀값 비기록, 외부 설정/가입/동의 실행 없음, 과거 기록과 기존 변경 보존.
- 결정사항: 서버 정책 버전 변경 없이 해당 버전의 약관 확인 후 사용자 직접 동의.
- 위험 요소: 현재 live 설정 미확인. 예상 밖 변경 없음.
- 다음 작업: 실제 실행 task 정책 버전 확인 후 가입 검증. Jira 변경 없음.

## 2026-09-28 — Identity 토큰 수신 성공 결과 해석

- 브랜치: develop.
- 작업 목표: 사용자 제공 토큰 수신 요약의 성공 범위 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 보고와 accept 구현을 대조하여 Access/Refresh pair 수신 및 UUID sub/MEMBER/LC audience 형태 확인 성공 설명. 사용한 버튼은 요약만으로 구분하지 않으며 서버 서명 검증과 구분.
- 테스트와 결과: 로컬 화면 코드 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행. 실서버 인증 성공 정보는 사용자 보고 기반.
- 유지한 계약: 토큰 원문 비기록, 외부 인증/설정 변경 없음, 기존 변경 보존.
- 결정사항: 프로필 인증→재발급→LC 접근을 다음 확인 단계로 안내.
- 위험 요소: 프로필/재발급/LC 실제 성공 및 서명 검증 미확인. 예상 밖 변경 없음.
- 다음 작업: 사용자 보호 API 호출과 재발급 결과 확인. Jira 변경 없음.

## 2026-09-28 — 토큰 수신 결과 해석 작업 식별 기록 보완

<!-- codex-turn:01a0e654-ccb1-7910-9cbc-8c94acb93265 -->

- 브랜치: develop.
- 작업 목표: Identity 토큰 수신 성공 해석의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 보고의 UUID sub/MEMBER/LC audience 검사 성공과 Access/Refresh pair 수신 의미 설명. 토큰 내용 확인과 실제 서버 인증 검증 구분.
- 테스트와 결과: 기존 화면 구현 대조 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 토큰 원문 비기록, 외부 설정/인증 변경 없음, 과거 기록과 기존 변경 보존.
- 결정사항: 프로필→재발급→LC 접근 순서로 후속 확인 안내.
- 위험 요소: 보호 API/재발급/LC 성공은 아직 미확인. 예상 밖 변경 없음.
- 다음 작업: 사용자 후속 검증 결과 확인. Jira 변경 없음.

## 2026-09-28 — 프로필·재발급 성공 및 LC 403 조사

<!-- codex-turn:01a0e655-dfb5-7220-b937-990b836c73ef -->

- 브랜치: develop.
- 작업 목표: 사용자 테스트 결과 해석과 LC 403 원인 후보 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 보고상 프로필 인증/재발급 성공 및 claim 형태 유지 확인. LC 로컬 ChallengeController의 MEMBER 검사와 feature OFF 모두 COMMON403 반환 확인. application 기본값 및 테스트 task template CHALLENGE_ENABLED=false 확인. LC 코드/파일은 읽기만 수행.
- 테스트와 결과: 사용자 결과와 로컬 코드/배포 템플릿 대조, git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 토큰 원문 비기록, 외부 설정/기능 활성화/배포 변경 없음. 기존 변경 보존.
- 결정사항: MEMBER 누락으로 단정하지 않고 실행 중 LC task feature flag부터 확인하도록 안내. AI 및 Day1 등 준비 없이 flag를 켜지 않음.
- 위험 요소: live LC 환경변수/배포 revision 미확인, 따라서 원인 후보이며 확정 아님. LC 인증·챌린지 E2E 성공 미완료. 예상 밖 변경 없음.
- 다음 작업: LC 담당자가 실행 task CHALLENGE_ENABLED와 요청 거절 위치 확인 후 활성 준비 검토. Jira 변경 없음.

## 2026-09-28 — Learning Core 회원 통합 workload 인계 검토

<!-- codex-turn:01a0e6b0-420b-7b93-a0b1-6a67ac69e80f -->

- 브랜치: develop.
- 작업 목표: LC 요청의 workload 신뢰 정보·이벤트 전송 준비 상태를 구현 기준으로 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: WorkloadJwtConfiguration/Properties/CredentialProvider에서 기존 encoder와 kid 공유, 별도 issuer, RS256/고정 subject/목적별 audience/정확히 2분 TTL 확인. UserMerged HTTPS adapter와 outbox 재시도/2xx 성공 처리 확인. JWKS 이전 공개키 병행 지원 및 테스트 draft merge/workload/publisher OFF 확인. OwnerEvent 대체 전송 경로 존재 확인.
- 테스트와 결과: 코드·설정 읽기 검토 및 git diff --check. 코드 변경 없어 Gradle 미실행, live AWS/전송 E2E 미실행.
- 유지한 계약: 로그인 JWT와 workload 용도 분리, 토큰/비밀키 비기록, 운영 및 테스트 기능 활성화 없음, 기존 변경 보존.
- 결정사항: 동일 테스트 issuer/JWKS 사용 가능하나 WORKLOAD_JWT_ISSUER를 명시하고 LC와 일치시켜야 함. 키 교체는 공유 사용자 토큰 최대 TTL/skew/cache 및 구 instance 종료를 고려. publisher 경로를 확인한 뒤 하나의 배포 계획으로 활성화하며 기본 챌린지 검증과 merge E2E를 구분.
- 위험 요소: 현재 공개 kid는 draft 및 이전 관측 기준, live 재확인 필요. 기존 사용자 토큰 차단과 기록 이전/중복 이벤트 E2E 미검증. 실제 전송 준비 완료로 간주하지 않음. 예상 밖 변경 없음.
- 다음 작업: merge 검증 범위 승인 후 실행 task 설정/배포 revision·키·대상 endpoint 확인과 양측 이벤트 E2E 계획 확정. Jira 변경 없음.

## 2026-09-28 — 회원 통합 테스트 준비 순서 안내

- 브랜치: develop.
- 작업 목표: workload 및 UserMerged 연동을 위해 다음에 할 작업을 구체화.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: merge transaction의 capture flag 분기 확인. live 설정 조회, 경로와 신뢰 계약 확정, LC 수신 준비, Identity 활성화, 가상 계정 병합 E2E 순서 안내. 챌린지 기능 활성화와 구분.
- 테스트와 결과: 코드 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 실제 AWS 조회/설정 변경/병합 요청 없음, 비밀값 비기록, 기존 변경 보존.
- 결정사항: 승인 없이 flag를 켜지 않으며 수신 측 준비 후 발행 측 활성화. Billing은 기존 제외 범위 유지.
- 위험 요소: live 기능 상태 및 키/전송 E2E 미확인. 실제 계정 병합은 테스트 계정으로만 별도 승인 후 수행. 예상 밖 변경 없음.
- 다음 작업: 실행 중 테스트 서비스의 비밀 아닌 환경설정과 배포 revision 읽기 확인. Jira 변경 없음.

## 2026-09-28 — 회원 통합 준비 안내 작업 식별 기록 보완

<!-- codex-turn:01a0e6b3-45c0-77c3-b0d9-a59b1f442f40 -->

- 브랜치: develop.
- 작업 목표: 회원 통합 테스트 준비 안내의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 실행 설정 조회→LC 수신 준비→Identity 발행 활성화→테스트 계정 병합 검증 순서 안내. 이벤트 저장/발행 경로 일치와 챌린지 403 대응의 독립성 설명.
- 테스트와 결과: 코드 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀값 비기록, AWS 변경/실제 병합 없음, 과거 기록과 기존 변경 보존.
- 결정사항: 다음 단계는 설정 변경 없는 live 조회이며 활성화와 테스트 병합은 별도 진행.
- 위험 요소: live 배포/기능 상태 및 이벤트 E2E 미확인. 예상 밖 변경 없음.
- 다음 작업: 테스트 서비스 설정·배포 revision 조회. Jira 변경 없음.

## 2026-09-28 — AWS 테스트 Identity 회원 통합 설정 실조회

- 브랜치: develop.
- 작업 목표: 실제 배포 버전과 workload·merge 이벤트 준비 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: AWS ECS 서비스 새로고침 후 test:4 desired1/running1/pending0 확인. 이미지 및 release의 commit 88ff5bedc1ee661ccd38a8a1d2c4dbf3b9f03f2d 확인. task 정의에서 GUEST_MERGE_ENABLED, WORKLOAD_JWT_ENABLED, USER_MERGED_PUBLISHER_ENABLED, OWNER_EVENT_USER_MERGED_CAPTURE_ENABLED, OWNER_EVENT_LEARNING_CORE_USER_MERGED_PUBLISHER_ENABLED, Billing merge publisher 모두 false 확인. workload issuer 및 이벤트 endpoint 항목 없음, 환경 파일 없음. 사용자 issuer/kid/Access TTL PT30M 확인.
- 테스트와 결과: 15:29~15:30 KST 콘솔 읽기 검증 및 git diff --check. 코드 변경 없어 Gradle 미실행, 이벤트 발급/전송 E2E 미수행.
- 유지한 계약: Secret 값 열람/운영 변경/테스트 설정 변경/계정 병합 없음. 비밀정보 비기록 및 기존 변경 보존.
- 결정사항: 현 배포는 로그인 가능 상태와 별개로 회원 통합 및 workload 이벤트 OFF. LC 수신 준비 후 선택한 발행 경로로 활성화 계획 필요.
- 위험 요소: 실제 JWKS 응답 재조회 및 LC 수신 설정/이벤트 E2E는 이번에 검증하지 않음. task 정의 설정 조회이며 컨테이너 내부 런타임 계측은 아님. 예상 밖 변경 없음.
- 다음 작업: LC에 실제 OFF 상태 인계, workload issuer·endpoint와 사용할 이벤트 경로 확정 후 별도 승인으로 테스트 설정 적용. Jira 변경 없음.

## 2026-09-28 — AWS 설정 조회 작업 식별 기록 보완

<!-- codex-turn:01a0e6b4-6101-7c83-b723-450df80551f6 -->

- 브랜치: develop.
- 작업 목표: AWS 테스트 Identity 실조회 결과의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 실행 중 test:4 및 이미지 commit 확인. 회원 통합/workload/두 이벤트 발행 경로 OFF와 workload issuer·endpoint 미설정 확인. 사용자 JWT 공개 메타데이터와 TTL 확인.
- 테스트와 결과: AWS 콘솔 읽기 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀값 조회·기록 없음, AWS 설정 변경/이벤트 발송/병합 없음, 과거 기록과 기존 변경 보존.
- 결정사항: LC 수신 준비 후 선택한 이벤트 경로의 활성화를 별도 승인으로 진행.
- 위험 요소: LC 수신 설정 및 이벤트 E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: LC 인계 및 테스트 이벤트 설정 확정. Jira 변경 없음.

## 2026-09-28 — 회원 통합 이벤트 두 경로 및 전환 의미 설명

<!-- codex-turn:01a0e6c6-bfe9-71e0-b261-cef72303b933 -->

- 브랜치: develop.
- 작업 목표: 발행 경로 선택 의미와 기존 안내의 과도한 단순화 정정.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: merge transaction capture 분기, OwnerEventCaptureService와 requiredConsumers, fanout runbook/Stage7 legacy 전환 계약 확인. 기존 LC 전용 outbox와 신규 Billing/LC fan-out 차이 및 기존 잔량 배출을 위한 publisher 공존 설명.
- 테스트와 결과: 코드/계약 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 신규 merge는 하나의 저장 경로 선택, 과거 outbox 삭제/자동 backfill 없음, 외부 설정 변경 및 비밀값 기록 없음.
- 결정사항: 두 publisher 동시 활성 자체를 금지하는 것이 아니며 capture와 발행 경로 일치·잔량 처리가 핵심이라고 정정. 신규 경로 Billing OFF는 delivery 미생성이 아니라 미전송 pending 보존임을 안내.
- 위험 요소: 테스트 DB 기존 outbox 잔량 미확인, 새 경로 Billing backlog 처리 결정 필요. 기존 변경 보존, 예상 밖 변경 없음.
- 다음 작업: LC-only 테스트 또는 신규 fan-out 검증 범위에 맞춰 저장/발행 설정과 backlog 정책 결정. Jira 변경 없음.

## 2026-09-28 — LC 전용 기존 경로 우선 사용 방향 확인

- 브랜치: develop.
- 작업 목표: 기존 이벤트 경로 우선 검증 후 Billing 연동 시 공통 경로 전환 가능 여부 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 기존 LC 전용 방식으로 이번 테스트 진행 방향 확인. 신규 capture 전환 이후 발생하는 병합부터 공통 이벤트를 생성하며 기존 잔량은 기존 publisher로 처리, 과거 Billing backfill은 자동 수행하지 않음을 안내.
- 테스트와 결과: 앞선 코드/전환 계약 검토 기반 안내 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 설정/배포 변경 없음, 실제 키/토큰 비기록, 과거 이벤트 삭제·변환 없음. 기존 변경 보존.
- 결정사항: 이번 범위는 기존 LC 전용 경로, 공통 경로는 Billing 소비자 준비와 함께 별도 전환 검증.
- 위험 요소: 과거 통합 계정이 Billing 소유권에 미치는 영향은 후속 검토 필요. 운영 확대나 실제 활성화 승인을 의미하지 않음. 예상 밖 변경 없음.
- 다음 작업: LC 수신 준비 후 기존 UserMerged publisher와 workload 테스트 설정 적용 범위 제시. Jira 변경 없음.

## 2026-09-28 — 기존 경로 우선 사용 작업 식별 기록 보완

<!-- codex-turn:01a0e6c8-5816-7a70-839c-6993a4deb8bb -->

- 브랜치: develop.
- 작업 목표: 기존 LC 전용 경로 우선 사용 방향의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 이번 테스트는 기존 UserMerged 경로, Billing 연동 시 OwnerEvent 공통 경로로 전환하는 방향 확인. 기존 미전송 이벤트 처리와 과거 병합의 Billing 이전 별도 검토 안내.
- 테스트와 결과: 기존 코드·계약 검토 기반 안내 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, AWS 설정/배포/이벤트 변경 없음, 과거 기록과 기존 변경 보존.
- 결정사항: 경로 선택만 확인했으며 실제 활성화는 미수행.
- 위험 요소: 전환 전 병합의 Billing 소유권 영향 검토 필요. 예상 밖 변경 없음.
- 다음 작업: LC 수신 준비 후 기존 경로 활성화 설정 범위 제시. Jira 변경 없음.

## 2026-09-28 — 프론트용 테스트 서버·Swagger 주소 전달

<!-- codex-turn:01a0e738-e560-7cc0-8c08-1bc54397c211 -->

- 브랜치: develop.
- 작업 목표: 프론트가 사용할 테스트 Identity 서버 및 Swagger 주소 제공.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 기존 검증된 테스트 HTTPS base URL, Swagger UI 주소 안내. 실제 API HTTPS 사용 및 Swagger HTTP 서버 주소 관측 이력 주의 안내.
- 테스트와 결과: 이전 검증 기록 기반 안내, git diff --check. 코드 변경 없어 Gradle 미실행, live 재조회 없음.
- 유지한 계약: 공개 주소만 공유, 비밀값 비기록, 서버 설정/배포 변경 없음.
- 결정사항: 프론트 인계 주소는 운영 아닌 테스트 환경임을 명시.
- 위험 요소: Swagger Try it out의 HTTP 주소 문제 해결 여부는 미확인. 기존 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 프론트 테스트 환경 주소 적용 후 인증 연동 확인. Jira 변경 없음.

## 2026-09-28 — LC 배포 연계 Identity 준비사항 검토

<!-- codex-turn:01a0e73f-9870-7be3-bbe6-d5c63e1c039d -->

- 브랜치: develop.
- 작업 목표: LC guard 점검/consumer 배포 전후 Identity의 필요 작업 정리.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 기존 publisher 설정과 workload 의존성/TTL 확인. issuer 및 HTTPS endpoint 사전 합의, outbox 상태 점검, LC 정상 기동·인증 준비 확인 후 publisher 활성화, 신규 merge용 flag 별도 필요 안내. 기존 경로 선택에 따라 OwnerEvent/Billing 설정 OFF 유지.
- 테스트와 결과: 코드·설정 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행, AWS/DB live 조회 없음.
- 유지한 계약: 운영/테스트 기능 변경 및 실제 merge/이벤트 발송 없음, 비밀값 비기록, 기존 변경 보존.
- 결정사항: LC 소비자 준비 전에 발행하지 않음. 401/403 등 영구 실패로 dead-letter 전환될 수 있어 무조건 자동 회복을 기대하지 않도록 안내. DB 최종 점검 이후 테스트 트래픽 조율 필요.
- 위험 요소: live LC guard/index 검증과 Identity 기존 outbox 상태 미확인. Guest merge 승인과 테스트 계정 준비 필요. 예상 밖 변경 없음.
- 다음 작업: LC 배포 완료 증빙 수신 및 Identity outbox/설정 확인 후 승인된 테스트 배포 진행. Jira 변경 없음.

## 2026-09-30 — LC 수신 준비 완료 후 추가 체크리스트 안내

<!-- codex-turn:01a0f0a2-847b-75b0-9a48-0487cbaac804 -->

- 브랜치: develop.
- 작업 목표: 기존 UserMerged 발행 연결 외 필요한 활성화·검증 사항 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 전달 LC 준비 완료와 직접 검증을 구분. application 설정 및 기존 publisher 실패 처리 재확인. 신뢰 설정·현 배포·outbox 상태 확인, 신규 merge flag, 테스트 계정 E2E, 실패 시 publisher 중단과 동일 이벤트 복구 필요 안내.
- 테스트와 결과: 코드 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행, AWS/DB live 확인 없음.
- 유지한 계약: 기존 LC 전용 경로 유지, OwnerEvent/Billing 확대 없음, 비밀정보 비기록, 외부 설정 변경/실제 병합 없음.
- 결정사항: 추가 기능 개발 필요성이 확인된 것은 아니며 테스트 설정 반영과 실연동 검증이 남은 단계. 기존 관측 배포 상태를 현재 상태로 단정하지 않음.
- 위험 요소: 과거 pending/dead-letter 및 live issuer/JWKS/revision 미확인, source 차단·target 기록 조회·중복 처리 E2E 미검증. 기존 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 승인 시 최신 테스트 설정과 outbox 조회 후 설정 반영 및 테스트 계정 병합 검증. Jira 변경 없음.

## 2026-09-30 — 테스트 이벤트 연결 착수 및 AWS 재로그인 대기

<!-- codex-turn:01a0f0a2-847b-75b0-9a48-0487cbaac804 -->

- 브랜치: develop.
- 작업 목표: 승인된 테스트 workload/기존 UserMerged 설정 적용 전 최신 상태 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: AWS task 정의 탭에서 로그아웃 상태 확인. 다시 로그인 버튼의 세션 오류 후 기존 ISB 포털을 열어 로그인 화면 인계. 캐시된 task 정의 정보는 live 증빙으로 사용하지 않음.
- 테스트와 결과: 브라우저 로그인 필요 상태 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: Secret 값 조회/설정 저장/배포/이벤트 전송/계정 병합 없음. 기존 변경 보존, 비밀값 비기록.
- 결정사항: 재로그인 후 최신 배포와 outbox/LC 수신 상태를 확인한 뒤 설정 적용 진행.
- 위험 요소: 최신 AWS 설정 및 대기 이벤트 미확인으로 배포 미완료. 예상 밖 변경 없음.
- 다음 작업: 사용자 AWS 로그인 완료 후 사전 조회 및 테스트 전용 설정 적용 재개. Jira 변경 없음.

## 2026-09-30 — AWS 재로그인 대기 작업 식별 기록 보완

<!-- codex-turn:01a0f0a4-8549-7bb2-93f0-56ad0654afb4 -->

- 브랜치: develop.
- 작업 목표: 테스트 이벤트 연결 착수 결과를 현재 작업 식별자로 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: AWS 세션 만료를 확인하고 기존 ISB 로그인 화면을 열어 사용자에게 인계. 이전 캐시 화면은 최신 배포 증빙으로 사용하지 않음.
- 테스트와 결과: 로그인 필요 상태 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀값 비기록, 설정 변경/배포/이벤트 전송/병합 없음, 과거 기록과 기존 변경 보존.
- 결정사항: 사용자 재로그인 후 최신 배포와 대기 이벤트 조회부터 재개.
- 위험 요소: 최신 설정·outbox 미확인으로 실제 배포 미완료. 예상 밖 변경 없음.
- 다음 작업: AWS 로그인 완료 후 테스트 전용 설정 적용 전 사전 확인. Jira 변경 없음.

## 2026-09-30 — AWS 재로그인 후 양측 설정 확인 및 Atlas 로그인 대기

- 브랜치: develop.
- 작업 목표: 테스트 UserMerged 발행 활성화 사전 확인 재개.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 최신 Identity test:4 실행1/보류0·배포 성공 및 관련 플래그 OFF 확인. LC test:11 실행1/보류0·배포 성공, writer/source-deny/consumer true 및 workload issuer·JWKS 테스트 Identity 일치 확인. 테스트 Atlas 접속은 로그아웃되어 로그인 탭 열고 사용자에게 요청.
- 테스트와 결과: AWS 서비스/태스크 정의 UI 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행. LC 실제 인덱스 검사 로그·인증 이벤트 E2E 및 Identity outbox 조회는 미완료.
- 유지한 계약: 설정 변경/새 revision 생성/배포/이벤트 전송/병합 없음. 비밀정보 비기록, 기존 변경 보존.
- 결정사항: 발행 활성화 시 기존 대기 이벤트가 전송될 수 있어 outbox 확인 전 OFF 유지.
- 위험 요소: Atlas 재로그인이 필요하며 대기·실패 이벤트 범위 미확인. 예상 밖 변경 없음.
- 다음 작업: 사용자 Atlas 로그인 후 테스트 Identity outbox 상태 점검, 안전 조건 충족 시 설정 적용 진행. Jira 변경 없음.

## 2026-09-30 — Atlas 테스트 병합 outbox 확인 완료

<!-- codex-turn:01a0f0a6-526b-7781-9bc6-9422b4ccb65e -->

- 브랜치: develop.
- 작업 목표: 테스트 UserMerged 발행 활성화 전 기존 대기 이벤트 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 재로그인 후 테스트 Atlas Cluster0의 to-teacher-identity-test.user_merged_outbox 조회. Documents 0, 전체 조회 0건 및 빈 컬렉션 안내 확인. 사용자 데이터 수정/삭제 없음.
- 테스트와 결과: Atlas UI 실조회 및 git diff --check. 애플리케이션 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 기존 LC 전용 발행 경로, OwnerEvent/Billing OFF, 운영 설정 미변경, 비밀값 비기록. 기존 사용자 변경 보존.
- 결정사항: 기존 전송 대상 없음 확인. workload 발급 활성화는 서비스 간 인증 기능 활성화이므로 브라우저 정책에 따라 적용 직전 사용자 확인 후 진행.
- 위험 요소: 설정 저장/새 revision/배포/이벤트 전송/병합은 아직 미수행. LC 인덱스 및 이벤트 E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: 확인 후 테스트 workload/Guest merge/기존 publisher 활성화 및 배포 안정성 점검. 실제 테스트 계정 병합과 source 차단·target 기록 이전 검증 별도. Jira 변경 없음.

## 2026-09-30 — 테스트 Identity UserMerged 발행 설정 배포 완료

- 브랜치: develop.
- 작업 목표: 승인된 기존 LC 전용 UserMerged 발행 및 workload 인증 기능 테스트 활성화.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 애플리케이션 코드 변경 없음.
- 구현 내용: 적용 직전 확인에 대한 사용자 응답 `dj`를 한국어 키보드의 `어`로 이해한다고 안내 후 진행. 기존 test:4 JSON을 UI에서 보존하여 환경 설정 7개만 수정하고 입력 JSON 동등성 검증 후 test:5 생성. GUEST_MERGE_ENABLED, WORKLOAD_JWT_ENABLED, USER_MERGED_PUBLISHER_ENABLED=true; WORKLOAD_JWT_ISSUER=https://identity-test.to-teacher.com; WORKLOAD_JWT_SUBJECT=identity-service; WORKLOAD_JWT_TTL=PT2M; USER_MERGED_PUBLISHER_ENDPOINT=https://api-test.to-teacher.com/internal/v1/events/user-merged.
- 배포: tosunsaeng-staging-cluster의 tosunsaeng-identity-test-service를 revision5로 업데이트. desired1 유지, 최종 running1/pending0, 배포 성공 및 steady state 확인. 이미지 commit 88ff5bedc1ee661ccd38a8a1d2c4dbf3b9f03f2d 유지.
- 테스트와 결과: UI JSON 검증, ECS 배포 성공/태스크 실패0 관측, CloudWatch에서 2026-09-30T04:58:28Z Started IdentityApplication(35.888초) 확인. 조회된 기동 로그에 ERROR/WARN/Exception 없음. git diff --check 수행. 코드 변경 없어 Gradle 미실행. 실제 이벤트/계정 병합은 수행하지 않음.
- 유지한 계약: 기존 RS256 workload 계약 및 LC 전용 outbox 사용, 기존 이미지·비밀값 참조·역할·네트워크·태스크 수 보존. OwnerEvent/Billing 발행 OFF. 운영/LC 서비스 변경 없음. 비밀값 비기록. 기존 사용자 파일 변경 보존.
- 결정사항: 테스트 연결 설정과 배포 준비 완료로 판단, 실제 이벤트 E2E 성공과 구분.
- 위험 요소: 204 수신/동일 eventId 재시도/중복 방지/source 토큰 차단/target 기록 이전 미검증. 장기 무오류 보장 아님. 다음 코드 배포 시 새 태스크 환경설정 보존 필요. 예상 밖 파일/설정 변경 없음.
- 다음 작업: 사용자가 지정한 테스트 Guest/MEMBER로 병합 E2E 검증. 문제 시 이전 revision4로 서비스 롤백 가능. Jira 변경 없음; 댓글 초안은 테스트 설정 배포 성공, 기록 파일 2개, 실조회 결과 및 E2E 미검증 요약이며 자동 등록하지 않음.

## 2026-09-30 — 테스트 이벤트 배포 작업 식별 기록 보완

<!-- codex-turn:01a0f0ab-6a03-7c03-8881-57cff08326ac -->

- 브랜치: develop.
- 작업 목표: 이번 테스트 UserMerged 활성화·배포 결과에 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 테스트 Identity revision5에 승인된 workload/Guest merge/기존 publisher 설정 적용 및 서비스 배포 완료. 상세 설정과 검증은 바로 앞 작업 항목에 기록. 이번 보완에서 추가 외부 변경 없음.
- 실행한 테스트와 결과: ECS 배포 성공·running1/pending0·steady state와 애플리케이션 기동 완료 로그 확인. git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 기존 LC 전용 발행, RS256 workload 계약, 운영 미변경, OwnerEvent/Billing 발행 OFF, 비밀값 비기록 및 과거 기록 보존.
- 결정사항: 배포 완료와 실제 이벤트 E2E 검증을 구분.
- 위험 요소: 실제 계정 병합·204 응답·중복 방지·source 차단·target 기록 이전은 미검증. 예상 밖 변경 없음.
- 다음 작업: 지정 테스트 계정으로 병합 E2E 검증 및 향후 배포 시 revision5 설정 보존 확인. Jira 변경 없음.

## 2026-09-30 — 로그인·병합·챌린지 통합 테스트 화면 범위 조사

- 브랜치: develop.
- 작업 목표: localhost:4173 기존 화면을 로그인부터 회원 통합·10초 챌린지 검증까지 확장하기 위한 계약 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 테스트 도구 코드 변경 없음.
- 수행 내용: tools/auth-test의 HTML/JS/로컬 게이트웨이/Mock 테스트와 Identity 프론트 Firebase 계약 확인. 별도 Learning Core 저장소의 ten-second-challenge-frontend-api.md를 읽어 클라이언트 호출 순서 확인; Learning Core 소스 복사 없음.
- 구현 사실: 기존 도구는 로그인/가입/전화 연결/프로필/재발급과 today 읽기만 지원. Guest prepare의 MERGE_REQUIRED 뒤 명시 확인과 Guest Bearer/Firebase proof로 merge, 신규 승격은 별도 enrollment/upgrade 계약. 챌린지는 X-Challenge-Date, attempt, M4A/AAC audio/mp4(2MiB), presigned PUT의 Authorization 미전송, 동일 answer Idempotency-Key 보존 및 결과 polling 계약이 필요.
- 테스트와 결과: 파일·계약 읽기 및 git diff --check. 구현 전 범위 확인 단계여서 Gradle/Node 테스트 미실행. 실제 로그인/녹음/업로드/병합/API 호출 없음.
- 유지한 계약: Identity 외 챌린지/S3 코드 추가 금지 규칙 및 비밀값 비기록. 기존 사용자 수정 파일 보존.
- 결정사항: 전체 통합 화면은 Identity 서비스 저장소가 아닌 별도 로컬 테스트 프로젝트로 분리하는 방향을 사용자에게 확인. 기존 localhost 주소 유지 가능하나 기존 프로세스 확인과 재시작 필요.
- 위험 요소: 브라우저 녹음이 M4A/AAC를 지원하지 않을 수 있어 파일 업로드 대안 필요. MEMBER 전용 챌린지 결과는 Guest 기록 이전 검증과 별개이며 기존 시험 기록 이전은 별도 LC API 계약 필요. 구현/E2E 모두 미완료. 예상 밖 변경 없음.
- 다음 작업: 별도 프로젝트 생성 위치·분리 방향 확인 후 안전한 테스트 UI 구현, Mock 테스트 및 실제 화면 QA. Jira 변경 없음.

## 2026-09-30 — 통합 테스트 화면 조사 작업 식별 기록 보완

<!-- codex-turn:01a0f0b0-d01f-7e82-8c4d-6c891389466a -->

- 브랜치: develop.
- 작업 목표: 로그인·병합·챌린지 테스트 화면 조사 결과에 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 기존 로컬 인증 도구와 Identity/LC 클라이언트 계약을 확인하고 별도 프로젝트 분리 방향을 사용자에게 질문. 상세 조사 내용은 바로 앞 항목에 보존.
- 실행한 테스트와 결과: git diff --check 통과. 코드 구현 전 범위 확인 단계여서 Gradle/Node 테스트 미실행.
- 유지한 계약: Identity 도메인 경계, 과거 작업 기록 보존, 비밀정보 비기록. 실제 인증·병합·업로드·서버 재시작 없음.
- 결정사항: 별도 tosunsaeng-integration-test 폴더에서 localhost:4173을 유지하는 방향 확인 대기.
- 위험 요소: 구현 및 실연동 검증 미완료, 브라우저 M4A/AAC 지원과 기존 시험 기록 이전 검증 계약 확인 필요. 예상 밖 변경 없음.
- 다음 작업: 사용자 확인 후 독립 테스트 화면 구현과 Mock/브라우저 검증. Jira 변경 없음.

## 2026-09-30 — 독립 로그인·병합·챌린지 통합 테스트 화면 구현

- 브랜치: develop (Identity 기록만 변경; 독립 도구는 별도 디렉터리).
- 작업 목표: 사용자 승인에 따라 Identity 도메인 경계를 유지하면서 localhost:4173 통합 테스트 화면 제공.
- 변경 파일: Identity의 docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 별도 /Users/msde76/tosunsaeng-integration-test에 index.html, app.js, session.mjs, challenge.mjs, server.mjs, app.test.mjs, server.test.mjs, challenge.test.mjs, merge.test.mjs, README.md 생성. /private/tmp/tosunsaeng-integration-test에서 구현한 뒤 사용자 권한 승인으로 새 영구 폴더에 복사. 기존 Identity 도구/서비스 코드는 미변경.
- 구현 내용: 기존 로컬 인증 도구 기반 독립 클라이언트. Google/phone/signup/exchange/profile/reissue 유지. Guest와 MEMBER 자격을 별도 메모리에 보관하고 신규 테스트 Guest 생성 또는 기존 테스트 Guest 입력 지원. 서버 GUEST 검사·prepare MERGE_REQUIRED·체크박스 및 확인창 후 merge 수행. 응답 유실 자동 재병합 금지. 병합 후 양측 ACCOUNT_MERGED_TOKEN_REJECTED만 성공으로 판단하고 LC 완료 시험 ID 포함 여부 비교. 기존 기록0은 미검증으로 표시.
- 챌린지 구현: 실제 서버 계약의 today→question→attempt→M4A/AAC 파일 또는 최대10초 지원 브라우저 녹음→presigned S3 PUT→answer→결과/최대60초 polling/history. X-Challenge-Date 전달, S3 사용자 Authorization 미전송, 같은 answer key/body 보존 및 최초 제출 시도 이후 녹음/덮어쓰기 잠금. AI failed와 API200 구분. 새 응시 자동 생성 없음. 녹음은 자동 전송하지 않고 사용자 업로드 버튼으로만 전송.
- 보안: localhost/동일 Origin/전용 헤더, 정확한 테스트 호스트·경로 allowlist, redirect 금지, presigned HTTPS S3 호스트 검증, 토큰 메모리 전용·오류 코드 제한 표시. 실제 secret/token을 파일에 저장하지 않음. 개인정보가 포함될 수 있는 발화/피드백과 네트워크 공유 금지 안내.
- 실행한 테스트와 결과: Node Mock 전체6개 통과(로그인/동의/회전, 게이트웨이 origin·host·redirect·body 크기, 토큰 요약, 챌린지 경로/업로드 host, answer 응답유실·같은키·무JWT PUT, merge 확인·source 차단·기록 비교). 루프백 테스트는 최초 sandbox EPERM 후 권한 승인 실행. ./gradlew clean test도 캐시 접근 sandbox 오류 후 승인 재실행 BUILD SUCCESSFUL. 브라우저 localhost 새 화면·초기 비활성 버튼·M4A 지원 안내·화면 렌더 확인. git diff --check 통과.
- 실행 상태: 영구 프로젝트의 Node 서버를 127.0.0.1:4173에 실행, 기존 포트 점유 프로세스 없어 중지/교체 작업 불필요. 기존 내장 브라우저 localhost 탭 새로고침 및 유지. Firebase 인증·실계정 생성·병합·마이크·실음성 업로드는 실행하지 않음.
- 유지한 계약: Identity/LC API 또는 배포 설정 변경 없음, Learning Core 서비스 코드 복사 없음, 독립 클라이언트만 추가. Guest의 챌린지 접근을 가정하지 않음. 기존 사용자 dirty 파일 보존, 예상 밖 변경 없음.
- 결정사항: 신규 Guest 승격은 지원하지 않고 기존 MEMBER 병합만 테스트. Chrome/OS에서 M4A/AAC 녹음 미지원이면 올바른 M4A 파일 사용. 앱의 Android SDK/업데이트·Stage9 응답복구 검증 대체 아님.
- 위험 요소: 실제 회원 인증/병합/AI E2E 및 S3 CORS 미검증. 새 Guest에는 이전 기록이 없어 별도 기록 보유 Guest 필요. 완료 시험 ID 비교는 전체 데이터 이전·이벤트204·중복 재전송 검증을 대신하지 않음. 녹음 파일의 실제 코덱은 AI 검증; 잘못된 파일은 응시 소비 가능. 토큰은 새로고침하면 소실, 서버 세션은 남음.
- 다음 작업: Chrome에서 localhost 열고 공개 웹 Firebase 설정 적용→MEMBER 로그인부터 단계별 실행. 테스트 계정/약관 버전·가상 전화·S3 CORS 확인 후 실제 제출, 이슈는 서버 로그와 함께 진단. 사용자 직접 실행 전 실제 데이터 변경 없음. Jira 변경 없음; 등록용 요약 초안은 독립 도구 구현·Mock6/Gradle 통과·실E2E 미검증이며 자동 등록하지 않음.

## 2026-09-30 — 독립 통합 테스트 구현 작업 식별 기록 보완

<!-- codex-turn:01a0f0b2-c659-78a2-9787-ecd2246ed647 -->

- 브랜치: develop.
- 작업 목표: 이번 독립 테스트 화면 구현·검증 결과에 현재 turn 식별자 추가.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 별도 프로젝트 파일 목록과 구현 상세는 바로 앞 항목에 보존.
- 구현 내용: 독립 프로젝트에서 로그인·전화 연결·가입·재발급, Guest 병합 확인 및 source 차단/완료 시험 비교, 챌린지 음성 제출/AI 결과 조회 화면 제공. localhost:4173 실행 및 브라우저 초기 화면 확인 완료.
- 테스트와 결과: Node Mock6개, ./gradlew clean test, git diff --check 통과. 실제 계정 변경이나 음성 전송 테스트는 미실행.
- 유지한 계약: Identity 서비스 도메인 경계와 기존 외부 API 유지, 운영 설정 미변경, 비밀정보 비기록, 과거 기록과 사용자 수정 보존.
- 결정사항: 도구 준비 완료와 실제 E2E 성공을 구분. 코드/배포 추가 변경 없이 기록 식별자만 보완.
- 위험 요소: S3 CORS·실제 Firebase/병합/AI 연결은 사용자 단계별 검증 필요. 예상 밖 변경 없음.
- 다음 작업: Chrome에서 Firebase 구성 적용 후 MEMBER 로그인부터 검증. Jira 변경 없음.

## 2026-09-30 — 기존 테스트 Google 계정 재사용 안내

<!-- codex-turn:01a0f0c0-c635-7250-8633-bf35607c3e6c -->

- 브랜치: develop.
- 작업 목표: 기존 계정 사용 가능 여부와 신규 계정 필요 조건 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 기존 도구 및 확인한 exchange 계약 기준으로 같은 Firebase 프로젝트/Identity 테스트 DB의 MEMBER는 기존 Google로 인증 후 Identity 로그인하고 phone/signup 생략 가능함을 안내. 병합 source는 별도 Guest이며 챌린지 당일 제한은 초기화되지 않음.
- 테스트와 결과: 문서 안내 작업, git diff --check 수행. 코드 변경/실 API 호출 없어 Gradle·Node 재실행 안 함.
- 유지한 계약: 기존 MEMBER/Guest 분리, 가입과 로그인 구분, 비밀정보 비기록. 실제 계정/서버 변경 없음.
- 결정사항: 새 Google 계정은 필수 아님; 신규 가입 검증용으로만 별도 계정 고려.
- 위험 요소: 현재 계정 존재/당일 챌린지 진행도 live 조회 없음. ENROLLMENT_REQUIRED이면 같은 프로젝트/DB/계정 여부 확인. 예상 밖 변경 없음.
- 다음 작업: 기존 Google 계정으로 Identity 로그인 후 프로필/챌린지 단계 실행. Jira 변경 없음.

## 2026-09-30 — Chrome 통합 테스트 진행 결과 확인

- 브랜치: develop.
- 작업 목표: 사용자가 실행한 localhost:4173 Chrome 결과의 성공 범위 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 지정 Chrome의 기존 탭 읽기. 검증표에서 Google 인증·Identity 로그인·MEMBER 프로필·토큰 재발급·LC today 인증 성공 확인. today의 문제3개 모두 not_started/not_requested와 nextQuestionNumber=1 확인. Guest 생성 CONSENT_REQUIRED와 빈 정책 버전/필수 동의 미선택 확인.
- 테스트와 결과: 실제 화면 관측(별도 네트워크/서버 로그 재검증 아님), git diff --check. 코드 변경 없어 Gradle/Node 재실행 안 함.
- 유지한 계약: 기존 세션 유지를 위해 새로고침 없음. 추가 API 호출·약관 체크·계정 생성·병합·녹음·업로드 없음. 민감 토큰/개인정보 비기록.
- 결정사항: 인증 연동은 화면상 성공, 병합과 음성/AI 흐름은 아직 검증 전. 기존 MEMBER 가입을 반복할 필요 없으나 새 Guest 생성은 해당 Guest의 정책 동의가 별도로 필요함을 안내.
- 위험 요소: 챌린지 문제/음성/AI 및 병합 E2E 미완료. 약관 버전은 현재 배포값 확인 후 사용자 직접 동의 필요. 예상 밖 변경 없음.
- 다음 작업: 챌린지 다음 문제→응시→녹음/업로드→제출→결과 검증. 병합 시험을 원하면 정책 버전/동의를 확인한 뒤 Guest 준비부터 수행. Jira 변경 없음.

## 2026-09-30 — Chrome 결과 확인 작업 식별 기록 보완

<!-- codex-turn:01a0f0c1-b738-7be1-9bee-47f9b41bf1f8 -->

- 브랜치: develop.
- 작업 목표: 이번 Chrome 테스트 진행 결과 조회에 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 기존 Chrome 화면에서 Google/Identity 로그인·MEMBER 프로필·재발급·LC today 성공 및 Guest 생성 CONSENT_REQUIRED 확인. 상세 관측은 바로 앞 기록에 보존.
- 테스트와 결과: 화면 읽기 및 git diff --check 통과. 코드 변경 없어 Gradle/Node 미실행.
- 유지한 계약: 새로고침/추가 API 호출/약관 동의/병합/녹음/업로드 없음, 비밀정보 비기록, 과거 기록 보존.
- 결정사항: 인증 성공과 아직 수행하지 않은 병합·챌린지 제출/AI 검증을 구분하여 안내.
- 위험 요소: 실E2E 미완료, 정책 버전 확인 및 사용자 직접 동의 필요. 예상 밖 변경 없음.
- 다음 작업: 사용자가 챌린지 다음 문제부터 진행하거나 Guest 준비 후 병합을 별도 검증. Jira 변경 없음.

## 2026-09-30 — Chrome 채점 및 Guest 생성 실진단

<!-- codex-turn:01a0f0c6-2340-7b00-9593-40ae75ce986b -->

- 브랜치: develop.
- 작업 목표: 사용자 요청에 따라 Chrome에서 채점 미진행·Guest 생성 문제 확인 및 가능한 직접 테스트.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 기존 Chrome localhost 탭을 새로고침 없이 읽고 녹음/attempt 준비 후 upload 일반 오류·answer 버튼 비활성·결과0건 확인. 문제 번호1로 결과 GET을 직접 실행해 solvedQuestionCount=0/question=null 재확인. 브라우저 수집 error/warn 로그는 빈 목록. 독립 도구의 업로드·오류 처리 코드를 읽어 현재 메시지만으로 CORS/네트워크 원인을 확정할 수 없음을 확인.
- Guest 관측: guestCreate CONSENT_REQUIRED, 정책 버전 입력 비어 있고 필수 동의 미선택. 테스트 Guest 확인 체크와 병합 확인 체크는 필수 정책 동의를 대신하지 않음.
- 테스트와 결과: 실제 LC 결과 조회 성공·제출 이력0 확인, git diff --check. 코드 변경 없어 Gradle/Node 재실행 안 함. AI 채점 실행/실패 여부 자체는 아직 검증 못 함.
- 유지한 계약: 토큰·서명URL·음성 원문 비기록. 기존 세션 보존. 임의 약관 동의/Guest 생성/병합/음성 전송/서버 설정 변경 없음.
- 결정사항: 현재 녹음된 테스트 음성을 테스트 S3에 재업로드하고 LC 제출할지 구체적으로 승인 요청. Guest용 정책 입력·약관 동의는 사용자가 직접 수행하도록 요청. 사용자 응답 후 승인 범위에서 테스트 재개.
- 위험 요소: 업로드 원인 미확정, S3 CORS는 가능성일 뿐 확정 아님. 화면의 일반 오류를 Google 인증 실패나 AI 실패로 단정하지 않음. 예상 밖 변경 없음.
- 다음 작업: 업로드 승인 후 동일 녹음으로 오류 재현/진단 및 성공 시 제출·AI 결과 확인. 사용자 정책 동의 완료 후 Guest 생성 테스트. Jira 변경 없음.

## 2026-09-30 — S3 업로드 CORS 원인 확정 및 정책 버전 입력

<!-- codex-turn:01a0f0c6-2340-7b00-9593-40ae75ce986b -->

- 브랜치: develop.
- 작업 목표: 사용자 승인된 음성 재업로드 테스트와 AWS 정책 버전 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 앱/서버 코드 변경 없음.
- 수행 내용: 사용자 현재 테스트 음성 업로드·제출 허용 수신 후 Chrome 업로드 버튼 재시도, 같은 일반 오류 재현. Identity test:5 환경값에서 PRIVACY_CONSENT_VERSION=privacy-v1, TERM_CONSENT_VERSION=term-v1 확인 후 Chrome 해당 입력란에 입력. 동의 체크/Guest 생성은 미수행. Chrome native 진단은 OS 권한 미부여로 접근하지 못했고 우회하지 않음.
- 진단: AWS LC test:11의 AWS_S3_BUCKET_NAME/region 확인 후 테스트 버킷에 무인증 OPTIONS 요청. localhost:4173 Origin, PUT, content-type 조합에 S3 HTTP403 AccessForbidden 및 CORSResponse: CORS is not enabled for this bucket 응답 확인. 파일 생성/음성/토큰 전송 없는 preflight 검사. 최초 sandbox DNS 오류 후 사용자 승인으로 네트워크 진단 재실행.
- 테스트와 결과: 브라우저 업로드 실패 재현 및 S3 CORS 미설정 확인. 따라서 현재 브라우저 업로드는 차단되며 answer/AI까지 진행하지 못함. git diff --check. 코드 변경 없어 Gradle/Node 재실행 안 함.
- 유지한 계약: AWS 환경·CORS·권한 변경 없음. 동의 대행 없음. 실제 Secret/Token/서명URL 비기록, 기존 세션/녹음 보존. 운영 미변경.
- 결정사항: 테스트 버킷의 localhost:4173 Origin에 PUT 및 content-type만 허용하는 CORS 설정을 사용자 승인 후 적용하는 방향 제안. 공개 읽기/버킷 정책 변경이나 CORS 우회는 하지 않음.
- 위험 요소: CORS 해결 뒤에도 서명/형식/AI 후속 문제가 있을 수 있어 실제 성공은 추가 검증 필요. Guest는 사용자 정책 동의가 필요. 예상 밖 코드 변경 없음.
- 다음 작업: 테스트 버킷 CORS 수정 승인 후 적용·OPTIONS 재검증 및 현재 녹음 업로드/제출/AI 확인. 사용자 약관 체크 완료 후 Guest 생성 테스트. Jira 변경 없음.

## 2026-09-30 — 재인증 화면 보완·CORS 수정 및 실제 AI 채점 완료

<!-- codex-turn:01a0f0c6-2340-7b00-9593-40ae75ce986b -->

- 브랜치: develop.
- 작업 목표: 사용자 요청의 FIREBASE_RECENT_AUTH_REQUIRED 원인 확인 및 업로드 문제와 함께 수정.
- 변경 파일: 독립 /Users/msde76/tosunsaeng-integration-test의 app.js, challenge.mjs, merge.test.mjs, README.md. Identity는 docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md만 갱신. 임시 작업 폴더에서 apply_patch 후 승인된 복사로 영구 프로젝트 반영.
- 원인: FirebaseAdminAuthenticationVerifier가 LOGIN_EXCHANGE 외 목적의 auth_time에 high-risk-max-authentication-age(기본PT5M)를 검사. 도구의 getIdToken(true)는 토큰 갱신이지 Google 재인증이 아님. 기존 Google 버튼은 재인증 전 pair/챌린지를 지워 작업 흐름도 손실시킴.
- 구현: 같은 Firebase UID reauthenticateWithPopup 성공 후 fresh token을 준비하고 MEMBER/Guest/녹음/attempt 보존. recent-auth 오류에 명확한 사용자 재인증 안내와 수동 prepare 재실행 유도. 확정된 인증 거절 시 merge 불명 상태를 해제하되 자동 병합 금지. S3 fetch 실패는 S3_NETWORK_OR_CORS_ERROR로 분리. 서버5분 기준/Token 검증 완화 없음.
- AWS 변경: 사용자 적용 직전 명시 승인 후 테스트 오디오 버킷의 CORS를 AllowedOrigins localhost:4173, AllowedMethods PUT, AllowedHeaders Content-Type, MaxAgeSeconds300으로 저장. 성공 UI 및 무인증 OPTIONS200·정확한 allow headers 확인. 버킷 공개 접근·IAM·운영 버킷 변경 없음.
- 실제 검증: 사용자 사전 승인된 기존 녹음을 Chrome에서 PUT 성공 후 LC answer로 제출. today에서 1번 submitted/processing 확인 뒤 상세 결과 GET에서 solvedQuestionCount1, gradingStatus completed, verdict needs_improvement, transcript/feedback 존재 확인. 음성·발화·피드백 원문은 기록하지 않음. 신규 응시 추가 생성하지 않음.
- 테스트와 결과: Node Mock6개 통과(최근인증 거절→재인증→MEMBER/Guest 및 이전이력 보존 회귀 포함), ./gradlew clean test BUILD SUCCESSFUL, git diff --check. 기존 코드 경고만 확인. 운영 API 호출 없음.
- 현재 사용자 단계: Chrome의 기존 도구를 새로고침하지 않아 Guest 자격 보존. 기존 버전 Google 재인증 버튼 실행 후 Google 계정 선택 popup에서 사용자 입력 대기. 기존 버전 동작상 MEMBER pair/챌린지 화면 상태는 초기화되었으나 서버의 채점 완료 결과와 Guest 상태는 보존. 새 코드 검증은 Mock 기반이며 새 로딩부터 적용.
- 유지한 계약: 기존 RS256/Firebase 계약, S3 JWT 미전송, 사용자 약관 동의 대행/실제 병합 없음. 비밀값 비기록, 기존 dirty 파일 보존. 예상 밖 코드 변경 없음.
- 위험 요소: 실제 재인증 완료·Guest 병합 E2E는 아직 미검증. 수정 코드 적용용 새로고침은 현재 Guest 자격을 소실시키므로 현 시험 종료 후 수행. 기존 음성1건 제출로 해당 문제 응시 소비됨.
- 다음 작업: 사용자 동일 Google 재인증 완료 후 기존 화면에서 Identity exchange/Guest prepare 확인. 실제 merge는 별도 명시 확인 후 진행. 테스트 종료 후 새 화면 로딩해 보완 UX 확인. Jira 변경 없음; 댓글 초안은 독립도구4파일 수정·Mock6/Gradle 성공·실제 채점 완료·병합 미검증 요약이며 자동 등록하지 않음.

## 2026-09-30 — 실제 재인증 후 Guest 병합 준비 성공

<!-- codex-turn:01a0f0c6-2340-7b00-9593-40ae75ce986b -->

- 브랜치: develop.
- 작업 목표: 사용자 Google 재인증 완료 후 최근인증 오류 해소 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Chrome의 Google 인증 완료 표시 확인 후 Identity exchange와 Guest prepare를 순서대로 직접 호출. MEMBER 토큰 형태 요약 및 기존 MEMBER로 병합 가능 안내·병합 버튼 활성 확인. 실제 merge는 누르지 않음.
- 테스트와 결과: 실제 로그인/병합 prepare 성공으로 FIREBASE_RECENT_AUTH_REQUIRED 해소 확인. 현재 Guest 인증으로 prepare 성공했으므로 Guest 준비 정상 확인. git diff --check 통과; 추가 코드 변경 없어 테스트 재실행 없음.
- 유지한 계약: 원문 토큰 비기록, 자동 병합 금지, 서버 최근인증 기준 유지, 사용자 재인증 직접 수행.
- 결정사항: 업로드/실제 AI 채점 및 재인증 후 병합 준비 검증 완료. 최종 계정 병합/이벤트 E2E와는 구분.
- 위험 요소: 실제 merge/source 차단/기록 이전 미실행, 시간 경과 시 재인증 재요구 가능. 예상 밖 변경 없음.
- 다음 작업: 사용자 최종 확인 후 실제 병합과 이벤트 검증. 현재 브라우저는 구버전 스크립트이므로 시험 종료 후 새로고침하여 보완 UX 적용. Jira 변경 없음.

## 2026-09-30 — 재인증·업로드 수정 작업 식별 기록 보완

<!-- codex-turn:01a0f0ce-0fc0-7af1-baad-490e85aff2b7 -->

- 브랜치: develop.
- 작업 목표: 이번 수정 및 실검증 결과에 정확한 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 독립 도구 수정 파일과 AWS 변경 상세는 앞선 항목에 보존.
- 수행 내용: 승인된 localhost 한정 테스트 버킷 CORS 설정 후 실제 음성 업로드·제출·AI 채점 완료 확인. 독립 도구의 같은 UID 재인증 상태 보존/최근인증 오류 안내/S3 오류 분리 수정. 사용자 Google 재인증 후 Identity 로그인 및 Guest 병합 준비 성공 확인.
- 테스트와 결과: Node Mock6개 및 Gradle clean test 성공, CORS OPTIONS200, 실제 채점 completed, Guest prepare 성공. git diff --check 통과.
- 유지한 계약: 서버 최근인증 기준 유지, 운영·IAM·공개 접근 변경 없음, 비밀정보 비기록, 과거 기록 보존. 실제 계정 병합 미실행.
- 결정사항: 현재 브라우저 Guest 보존을 위해 새로고침 보류; 새 도구 코드는 다음 로딩부터 적용.
- 위험 요소: 최종 병합·이벤트 수신·기록 이전은 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 최종 확인 후 실제 병합 E2E 검증. Jira 변경 없음.

## 2026-09-30 — 사용자 병합 후 검증표 해석

- 브랜치: develop.
- 작업 목표: 병합 후 공통 안내 문구가 실패인지 실제 결과를 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 Chrome localhost 검증표 읽기. Identity 병합·Identity source 차단·LC source 차단·LC target 인증 성공 표시 확인. 이전 대상 완료 시험0건 및 완료 시험 이전 미검증 표시 확인. 화면에는 과거 CONSENT_REQUIRED/recent-auth/upload 실패도 이후 성공 행과 함께 남음.
- 테스트와 결과: 현재 화면 실조회 및 git diff --check. 추가 API/병합/설정 변경 없음. 코드 변경 없어 Gradle/Node 재실행 안 함.
- 유지한 계약: 원문 토큰/개인정보 비기록, 기존 세션 보존, 자동 재병합 금지.
- 결정사항: 병합 및 양 서비스의 이전 Guest 차단/대상 MEMBER 접근은 화면상 확인됨. 하단 공통 안내는 오류가 아니며 실제 데이터 이전 검증 완료와는 구분.
- 위험 요소: source에 완료 시험이 없어 기록 이전 확인 불가. 이벤트204/동일event 재전송 처리는 서버 로그/별도 시험 미확인. 예상 밖 변경 없음.
- 다음 작업: 필요 시 발행/수신 상태를 읽기 전용으로 확인하고 기록 보유 테스트 Guest의 이전 검증을 별도 계획. Jira 변경 없음.

## 2026-09-30 — 병합 후 검증표 조회 작업 식별 기록 보완

<!-- codex-turn:01a0f0d5-a5a6-7ad1-906b-614aeab7aad3 -->

- 브랜치: develop.
- 작업 목표: 이번 병합 후 결과 확인에 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Chrome 검증표의 Identity 병합·Identity/LC source 차단·LC target 인증 성공과 이전 대상 기록0건 확인 결과 기록. 하단 문구는 공통 안내이며 과거 실패 행과 현재 성공을 구분해 설명.
- 테스트와 결과: 화면 읽기 및 git diff --check. 코드 변경 없어 Gradle/Node 미실행.
- 유지한 계약: 비밀정보 비기록, 과거 기록 보존, 추가 API/병합/설정 변경 없음.
- 결정사항: 인증 차단 검증 성공과 실제 기록 이전 미검증을 구분.
- 위험 요소: 이벤트204·중복 재전송 및 기록 이전은 별도 검증 필요. 예상 밖 변경 없음.
- 다음 작업: 필요 시 발행/수신 상태 조회와 기록 보유 테스트 Guest의 이전 검증 계획. Jira 변경 없음.

## 2026-09-30 — 통합 테스트 완료 범위 재확인

- 브랜치: develop.
- 작업 목표: 사용자의 전체 검증 완료 여부 질문에 현재 증거와 남은 범위를 구분.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Chrome 검증표 확인 후 병합 후 검증 버튼 및 챌린지1 상세 결과 GET을 직접 재실행. Identity/LC source 병합 전용 차단과 LC target 인증 성공, solved1/gradingStatus completed 및 발화·피드백 존재 확인. 로그인/프로필/재발급/Guest 생성/업로드/제출은 현재 화면의 앞선 성공 기록과 이전 실검증 근거를 사용.
- 테스트와 결과: 현재 인증 상태·채점 결과 읽기 재검증 성공, git diff --check. 코드 변경 없어 Gradle/Node 재실행 없음.
- 유지한 계약: 실제 계정/데이터 추가 변경 없음, 원문 토큰/개인 발화 비기록. 이전 병합·음성 제출 재실행 안 함.
- 결정사항: 기본 정상 흐름 통과이지 모든 예외·출시 준비 검증 완료는 아님. 과거 실패 표시는 이후 성공과 구분.
- 위험 요소: source 완료 시험0건으로 실제 기록 이전 미검증. 이벤트204·중복/재시도·장애복구, 새 가입/전화 연결의 이번 회차 검증, 챌린지2·3 진행/중복·만료·AI실패, Android SDK/운영 전환 검증 남음. 예상 밖 변경 없음.
- 다음 작업: 기록 보유 테스트 Guest 이전과 서버 이벤트 검증을 우선하고 Android 앱 통합/예외 시나리오 수행. Jira 변경 없음.

## 2026-09-30 — 전체 검증 범위 확인 작업 식별 기록 보완

<!-- codex-turn:01a0f0d6-dc0e-7643-bc0f-aa6c386ca867 -->

- 브랜치: develop.
- 작업 목표: 이번 통합 테스트 완료 범위 확인에 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Chrome에서 병합 후 읽기 검증과 1번 결과 조회를 재실행해 양 서비스 source 차단·target 인증 및 채점 completed를 확인. 기본 흐름 성공과 미검증 항목을 구분해 안내.
- 테스트와 결과: 실제 읽기 재검증 성공 및 git diff --check 통과. 코드 변경 없어 Gradle/Node 재실행 안 함.
- 유지한 계약: 비밀정보 비기록, 과거 기록 보존, 추가 계정 변경/병합/음성 제출 없음.
- 결정사항: 프론트 앱 연동 시험 진행 가능하나 운영 출시 검증 완료는 아님.
- 위험 요소: 기록 보유 Guest 이전, 이벤트 중복/장애 복구, 신규 가입/전화 연결 재검증, 나머지 챌린지와 Android 연동 검증 필요. 예상 밖 변경 없음.
- 다음 작업: 남은 데이터 이전 및 앱/예외 시나리오 검증. Jira 변경 없음.

## 2026-09-30 — 소셜 로그인 제공자 준비 상태 읽기 점검

<!-- codex-turn:01a0f0da-5a97-71a2-91b8-9d2abc0cc7f2 -->

- 브랜치: develop.
- 작업 목표: Google Play 서명·Apple·Kakao 등록 준비의 필요성과 현재 상태 확인. 외부 변경 금지.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md (작업 기록만).
- 수행 내용: Firebase Google/Apple/전화 활성화 및 Kakao 부재 확인. Android com.toteacher.app에 사용자가 전달한 SHA-1 두 개·SHA-256 두 개 등록 확인. Apple Service ID·Team ID·Key ID·비공개 키 입력란은 빈 상태로 관측. 코드상 Apple/Kakao 기본 false, Kakao provider 기본 oidc.kakao 확인.
- 테스트와 결과: Firebase 화면 및 application.yml/프론트 계약 읽기 점검. 코드 변경 없어 Gradle 미실행; git diff --check 수행.
- 유지한 계약: Firebase ID Token→Identity 토큰 계약 유지. 비밀정보 비기록, 등록·저장·권한·배포 변경 없음.
- 결정사항: Google Play 앱 서명 인증서 대조→Apple 개발자 설정→Kakao Generic OIDC 준비 순서 권장. 로컬 빌드는 해당 빌드 서명 등록으로 시험 가능하며 Play 서명이 선행 필수는 아님.
- 위험 요소: Play 인증서 실제 일치, Apple Developer 설정, Kakao Developers 앱 유무 및 Identity Platform 업그레이드 상태 미확인. 배포 중 provider 플래그는 이번에 재조회하지 않음. Firebase 활성화만으로 실제 로그인 준비 완료 아님.
- 다음 작업: 승인 후 각 제공자 설정과 Identity 플래그를 준비하고 실제 Android 로그인 검증. 기존 사용자 변경 보존, 예상 밖 수정 없음. Jira 변경 없음.

## 2026-09-30 — Play 앱 서명 인증서 확인 경로 안내

<!-- codex-turn:01a0f0e0-5d2a-7ba2-ad15-d8907cf2168b -->

- 브랜치: develop.
- 작업 목표: Google Play 앱 서명 인증서 지문 조회 위치 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Play Console 앱 선택→앱 무결성→앱 서명에서 앱 서명 키 인증서 SHA-1/SHA-256 확인 후 Firebase Android 앱 지문에 추가하도록 안내. 업로드 키 인증서와 구분.
- 테스트와 결과: 코드 변경 없어 테스트 미실행. git diff --check 수행. 이번에 Play 화면 실조회는 하지 않음.
- 유지한 계약: 기존 개발용 지문 보존, 비밀정보 비기록, 외부 설정 변경 없음.
- 결정사항: Play 배포용 앱 서명 지문을 기존 Firebase 지문과 대조하고 없는 것만 추가.
- 위험 요소: 실제 인증서 일치 여부와 Play 배포 앱 로그인은 아직 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자가 인증서 화면을 확인한 뒤 Firebase 등록 및 Play 설치 빌드 로그인 시험. Jira 변경 없음.

## 2026-09-30 — 변경된 Play 앱 서명 메뉴 실조회

- 브랜치: develop.
- 작업 목표: 사용자 Chrome 탭에서 앱 무결성 메뉴 이전 위치 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 앱 무결성의 이전 안내 확인 후 Google Play로 보호됨→Play 스토어 보호 세부 정보→Play 앱 서명 관리로 이동, 앱 서명 화면 실조회. 사용 중 키와 이전 앱 서명 키가 함께 존재하며 기존 Firebase 등록 지문 중 한 쌍이 업로드 키 인증서임을 확인. 지문 원문은 기록하지 않음.
- 테스트와 결과: 브라우저 읽기 조회, git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 설정·키·인증서 변경 없음, 사용자 탭 유지.
- 결정사항: 앞선 메뉴 안내를 현재 UI 경로로 정정. 업로드 키가 아닌 실제 배포 서명들을 대상으로 Firebase 대조 필요.
- 위험 요소: 키 업그레이드가 있어 현재·이전 배포 서명 범위 확인 필요. 앱 서명 지문 추가와 실로그인은 미수행. 예상 밖 변경 없음.
- 다음 작업: 승인 후 배포 대상 인증서 지문과 Firebase 등록 목록 대조. Jira 변경 없음.

## 2026-09-30 — Play 메뉴 실조회 작업 식별 기록 보완

<!-- codex-turn:01a0f0e2-71ca-79b1-af41-7595febcc93a -->

- 브랜치: develop.
- 작업 목표: 이번 Play Console 조회의 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 앱 무결성 이전 안내와 Google Play로 보호됨→Play 스토어 보호→Play 앱 서명 관리 경로를 실조회하고 사용자 탭을 인증서 화면에 유지.
- 테스트와 결과: 화면 조회 및 git diff --check 성공. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 설정 미변경, 비밀정보 비기록, 과거 작업 기록 보존.
- 결정사항: 최신 메뉴 경로 안내로 정정; 업로드 인증서와 배포 앱 서명 인증서 구분.
- 위험 요소: 현재·이전 앱 서명 인증서의 Firebase 등록 여부와 Play 설치 로그인 미검증. 예상 밖 변경 없음.
- 다음 작업: 승인 후 필요한 배포 인증서 대조 및 등록. Jira 변경 없음.

## 2026-09-30 — 기존 키와 양자 내성 키 선택 안내

- 브랜치: develop.
- 작업 목표: Firebase Google 로그인 지문 등록 시 화면의 키 구분 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 현재 앱 서명 키 내 기존 키의 SHA-1/SHA-256을 우선 대조하도록 안내. 기존 키는 아래 별도 이전 앱 서명 키와 다른 항목이며 키 업그레이드에 따른 이전 배포 인증서도 확인 필요.
- 테스트와 결과: 앞선 실조회 화면을 근거로 설명. 코드 변경 없어 Gradle 미실행; git diff --check 수행.
- 유지한 계약: 키 변경·업그레이드·설정 저장 없음, 비밀정보 비기록.
- 결정사항: 현재 Google 로그인 준비는 기존 방식 앱 서명 지문 대조부터 진행. 양자 내성 인증서의 추가 필요성은 실제 배포 방식과 Firebase 지원 조건 확인 전 단정하지 않음.
- 위험 요소: 기기별 실제 배포 인증서 및 새 암호화 방식 연동은 미검증. 예상 밖 변경 없음.
- 다음 작업: 현재/이전 앱 서명 인증서 대조 후 필요한 지문 등록 및 Play 빌드 로그인 검증. Jira 변경 없음.

## 2026-09-30 — 앱 서명 키 구분 안내 작업 식별 보완

<!-- codex-turn:01a0f0e5-8efd-7fc0-8c64-21897c97fb2b -->

- 브랜치: develop.
- 작업 목표: 기존 키/양자 내성 키 선택 안내에 현재 작업 식별자 추가.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 현재 앱 서명 키의 기존 방식 인증서 지문부터 Firebase와 대조하도록 안내하고, 별도 이전 앱 서명 키 및 양자 내성 키와 구분.
- 테스트와 결과: git diff --check 수행. 코드 변경 없는 안내로 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 외부 설정 미변경.
- 결정사항: 지문 등록과 키 변경은 별개이며 키 업그레이드 작업은 수행하지 않음.
- 위험 요소: 실제 배포 인증서 대조 및 양자 내성 방식의 Firebase 지원 조건 미검증. 예상 밖 변경 없음.
- 다음 작업: 필요한 배포 인증서 대조 후 승인 범위에서 등록 및 로그인 검증. Jira 변경 없음.

## 2026-09-30 — Google 앱 서명 지문 등록 확인 및 Apple 준비 안내

<!-- codex-turn:01a0f0ea-be30-7211-bdd5-a2eafe81f76f -->

- 브랜치: develop.
- 작업 목표: 사용자 추가 Firebase 지문 검증 및 Apple 로그인 설정 순서 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Chrome Firebase Android 앱에서 SHA-1/SHA-256 각 세 개 등록 확인. 새 지문 한 쌍을 Play 현재 앱 서명 키의 기존 방식 인증서 복사값과 대조하여 둘 다 일치 확인. 지문 값 자체는 기록하지 않음. Apple Developer App ID capability→Services ID/웹 반환 주소→Sign in with Apple 키→Firebase provider 구성→Identity 활성화·실검증 순서 안내.
- 테스트와 결과: 실화면 및 공개 인증서 지문 비교 성공. 코드 변경 없어 Gradle 미실행; git diff --check 수행.
- 유지한 계약: Firebase ID Token 기반 Identity 교환 유지, 비밀정보 비기록, 외부 설정 변경 없음.
- 결정사항: 현재 기존 방식 앱 서명 지문 등록 확인 완료와 실제 Play 로그인 완료를 구분. Android/web Apple 로그인을 위해 Services ID 구성 필요.
- 위험 요소: 이전 앱 서명 인증서와 양자 내성 인증서 추가 필요성, Play 실로그인 및 Apple Developer 실제 설정은 미검증. 기존 사용자 변경 보존, 예상 밖 수정 없음.
- 다음 작업: Apple Developer의 실제 앱 식별자에서 Sign in with Apple 상태 확인 후 설정 진행. Jira 변경 없음.

## 2026-09-30 — Apple Identifiers 접근 경로 안내

<!-- codex-turn:01a0f0f0-ef5d-7870-9435-55bd9f5a61b8 -->

- 브랜치: develop.
- 작업 목표: 사용자가 찾지 못한 Apple Developer Identifiers 메뉴 접근 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Identifiers 직접 링크와 App Store Connect와의 사이트 구분 안내. 앱 등록 팀 선택 및 접근 권한 확인 필요성을 설명.
- 테스트와 결과: 안내만 수행, 실제 Apple 화면 미조회. 코드 변경 없어 Gradle 미실행; git diff --check 수행.
- 유지한 계약: 외부 설정·권한 변경 없음, 비밀정보 비기록.
- 결정사항: 직접 링크에서 앱 등록 개발자 계정으로 로그인 후 실제 App ID 확인부터 진행.
- 위험 요소: 현재 Apple 로그인 팀·멤버십·권한 상태 미확인. 예상 밖 수정 없음.
- 다음 작업: 접근 화면 확인 후 Sign in with Apple 구성 점검. Jira 변경 없음.

## 2026-09-30 — Apple 편집 창과 반환 주소 입력 위치 확인

<!-- codex-turn:01a0f0f5-7222-75b1-bcb2-7e6dd5c5a242 -->

- 브랜치: develop.
- 작업 목표: Apple 열린 Edit 창에서 Firebase 반환 주소 입력 위치 검증.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Chrome의 App ID com.toteacher.app 편집 모달 실조회. 열린 창은 Services ID 웹 설정이 아니라 App ID의 Server-to-Server Notification Endpoint임을 확인. Firebase auth handler를 해당 알림 칸에 넣지 않도록 안내. Services ID의 Domains and Subdomains/Return URLs 입력값 및 Primary App ID 선택 절차 안내.
- 테스트와 결과: 브라우저 읽기 확인, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 설정 저장·입력 없음, 알림 수신 주소와 OAuth 반환 주소 분리, 비밀정보 비기록.
- 결정사항: 현재 알림 주소는 입력하지 않고 Services ID 구성 화면에서 Firebase 기본 auth 도메인과 /__/auth/handler를 설정하도록 안내.
- 위험 요소: 현재 App ID 체크의 저장 여부, Services ID 등록 상태 및 Apple 서버 알림 처리 구현 미확인. 예상 밖 수정 없음.
- 다음 작업: Services ID 웹 인증 구성 및 Firebase 연결, 실제 Apple 로그인 검증. Jira 변경 없음.

## 2026-09-30 — Apple 이메일 릴레이 화면과 Services ID 구분

<!-- codex-turn:01a0f0f9-895a-7952-807c-a92a330f3042 -->

- 브랜치: develop.
- 작업 목표: 사용자가 연 Apple 화면이 로그인 반환 주소 설정인지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 현재 Configure Sign in with Apple for Email Communication 및 Register your email sources 모달 실조회. 이메일 릴레이 발신자 등록 화면으로 확인하고 Identifiers의 Services IDs와 구분해 안내.
- 테스트와 결과: 화면 읽기 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 입력·저장 없음, 비밀정보 비기록.
- 결정사항: 이메일 소스 창에 Firebase 로그인 도메인을 입력하지 않고 Identifiers 목록에서 Services IDs 선택 또는 생성으로 진행.
- 위험 요소: Services ID 생성 및 웹 인증 구성 미완료. 예상 밖 수정 없음.
- 다음 작업: 올바른 Services ID 설정 화면에서 Primary App ID 및 Return URLs 확인. Jira 변경 없음.

## 2026-09-30 — Services ID 생성 화면 확인

<!-- codex-turn:01a0f0fd-0000-70b1-9a00-64653be875c5 -->

- 브랜치: develop.
- 작업 목표: 현재 Apple 화면이 올바른 Services ID 등록 화면인지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Register a Services ID 화면과 빈 Description/Identifier 입력란 확인. Firebase용 설명 및 별도 서비스 식별자 예시 안내; 생성 후 Sign in with Apple Configure 단계 설명.
- 테스트와 결과: 브라우저 읽기 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 입력·등록·저장 없음, 비밀정보 비기록.
- 결정사항: 현재 화면은 올바르며 Services ID는 앱 Bundle ID와 별도 식별자로 생성하도록 안내. 제안 식별자의 사용 가능 여부는 등록 시 확인 필요.
- 위험 요소: Services ID 등록과 반환 주소 설정은 아직 미완료. 예상 밖 수정 없음.
- 다음 작업: 사용자 생성 후 웹 인증 설정 확인 및 Firebase 연결. Jira 변경 없음.

## 2026-09-30 — Services ID 입력값 재안내

<!-- codex-turn:01a0f0fe-747f-79b0-a496-56eec2e1ba9f -->

- 브랜치: develop.
- 작업 목표: Services ID 예시를 그대로 입력해도 되는지 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 제안 Description/Identifier 사용 가능하되 식별자 중복 여부는 Apple 등록 시 확인해야 함을 안내. Firebase 서비스 ID에는 생성한 Identifier를 동일하게 입력하고 Bundle ID와 구분.
- 테스트와 결과: 안내만 수행, 코드 변경 없어 Gradle 미실행. git diff --check 수행.
- 유지한 계약: 외부 등록·설정 변경 없음, 비밀정보 비기록.
- 결정사항: 제안 서비스 식별자를 사용하고 Firebase와 정확히 일치시킴.
- 위험 요소: 식별자 가용성과 실제 등록 미확인. 예상 밖 수정 없음.
- 다음 작업: 사용자 등록 후 웹 인증 반환 주소 구성 확인. Jira 변경 없음.

## 2026-09-30 — Services ID 안내 작업 식별 기록 보완

<!-- codex-turn:01a0f0fe-6365-7df2-98cf-1d1b032a2823 -->

- 브랜치: develop.
- 작업 목표: Services ID 입력값 안내의 정확한 현재 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 제안 Description/Identifier를 사용하고 Firebase 서비스 ID와 일치시키도록 안내한 결과 기록. 과거 항목 수정 없이 추가.
- 테스트와 결과: git diff --check 수행. 안내만 수행하여 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, 외부 설정 변경 없음.
- 결정사항: 앱 Bundle ID와 Services ID 구분 유지.
- 위험 요소: 실제 등록 및 식별자 가용성 미확인. 예상 밖 코드 변경 없음.
- 다음 작업: 사용자 등록 후 반환 주소 구성 확인. Jira 변경 없음.

## 2026-09-30 — Services ID 생성 완료 확인 및 웹 인증 설정 안내

- 브랜치: develop.
- 작업 목표: 사용자 생성 완료 확인 및 다음 설정 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Apple Services ID 목록에서 ToTeacher Firebase Login 및 com.toteacher.app.firebase 생성 확인. 편집 화면을 열어 Sign In with Apple 미선택 및 Configure 비활성 확인. 사용자에게 활성화 후 Primary App ID·Firebase 도메인·반환 URL 설정 안내.
- 테스트와 결과: 브라우저 읽기 확인, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 입력·설정 저장 없음, 비밀정보 비기록.
- 결정사항: Services ID 생성은 완료됐으나 웹 로그인 구성은 미완료.
- 위험 요소: 웹 인증 설정 저장 및 Firebase provider 연결·실제 로그인 미검증. 예상 밖 수정 없음.
- 다음 작업: 사용자 웹 인증 구성 후 저장 상태 점검. Jira 변경 없음.

## 2026-09-30 — Services ID 생성 확인 작업 식별 기록 보완

<!-- codex-turn:01a0f0ff-50d2-7750-9df0-efcbfefaf975 -->

- 브랜치: develop.
- 작업 목표: 이번 Services ID 생성 확인의 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Services ID 생성 및 Sign In with Apple 미활성 상태 확인 결과 기록. 웹 인증 구성과 최종 저장 절차 안내.
- 테스트와 결과: git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 외부 설정 저장 없음.
- 결정사항: 생성 완료와 웹 인증 설정 완료를 구분.
- 위험 요소: 웹 구성 저장·Firebase 연결·로그인 검증 미완료. 예상 밖 수정 없음.
- 다음 작업: 사용자 웹 구성 저장 후 확인. Jira 변경 없음.

## 2026-09-30 — Apple 웹 인증 Next 비활성 원인 확인

- 브랜치: develop.
- 작업 목표: Services ID 웹 인증 설정 Next 비활성 원인 진단.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Web Authentication Configuration의 No App ID is available 표시 및 Next disabled 확인. 입력 도메인/반환 URL은 Firebase 기본 auth handler와 일치. 선택 가능한 Primary App ID 부재가 차단 원인임을 안내하고 App ID의 Sign In with Apple primary 구성 및 최종 저장 확인 요청.
- 테스트와 결과: 브라우저 실조회, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 입력·저장·권한 변경 없음, 비밀정보 비기록.
- 결정사항: URL 수정 대신 같은 팀의 Primary App ID 등록 상태 점검 우선. 앞선 체크 상태만으로 저장 완료를 보장하지 않음.
- 위험 요소: Primary 부재의 근본 원인은 미확정; App ID 설정 저장 누락 또는 반영 지연 등 확인 필요. 예상 밖 수정 없음.
- 다음 작업: App ID primary 설정 최종 저장 후 Services ID Configure 재진입 및 선택 가능 여부 확인. Jira 변경 없음.

## 2026-09-30 — Apple Next 비활성 진단 작업 식별 기록 보완

<!-- codex-turn:01a0f100-fed9-77c2-8c77-41f6dc7f8124 -->

- 브랜치: develop.
- 작업 목표: 이번 Apple Next 비활성 진단의 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 올바른 도메인/반환 URL 입력과 No App ID is available 표시 확인 결과 기록. Primary App ID 구성 및 최종 저장 점검 안내.
- 테스트와 결과: git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 외부 설정 변경 없음.
- 결정사항: 주소 수정이 아닌 Primary App ID 가용성 확인이 다음 단계.
- 위험 요소: 저장 누락·반영 지연 등 근본 원인은 미확정. 예상 밖 수정 없음.
- 다음 작업: 사용자 App ID 저장 후 Services ID 선택 목록 재확인. Jira 변경 없음.

## 2026-09-30 — Apple Services ID 웹 인증 저장 확인

- 브랜치: develop.
- 작업 목표: 사용자 저장 후 Primary App ID 및 웹 인증 주소 검증.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Services ID 목록에서 편집 화면을 다시 열어 Sign In with Apple 활성 확인. Configure에서 Primary App ID가 com.toteacher.app으로 선택됨 확인. Website URLs 목록에 Firebase 기본 도메인과 /__/auth/handler 반환 주소 모두 존재 확인. 읽기 확인 후 Cancel로 모달 닫음.
- 테스트와 결과: 브라우저 저장 상태 재조회 성공, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 설정 변경·키 생성 없음, 비밀정보 비기록.
- 결정사항: Primary 부재 문제 해소 및 Services ID 웹 인증 설정 저장 확인 완료. Apple 로그인 전체 완료와는 구분.
- 위험 요소: Apple 로그인용 키·Firebase provider 구성·Identity 활성화·실로그인 미검증. 예상 밖 수정 없음.
- 다음 작업: Apple 로그인용 키 준비 후 Firebase 구성 및 테스트. Jira 변경 없음.

## 2026-09-30 — Apple 웹 인증 저장 확인 작업 식별 기록 보완

<!-- codex-turn:01a0f103-c0cf-7913-918a-32eb41e94a8c -->

- 브랜치: develop.
- 작업 목표: Apple 웹 인증 저장 확인의 현재 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Services ID 재조회에서 로그인 활성, Primary App ID 선택, Firebase 도메인 및 반환 주소 등록을 확인한 결과 기록. 다음 키 준비 절차 안내.
- 테스트와 결과: git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 외부 설정 변경·키 생성 없음.
- 결정사항: Services ID 구성 저장 확인 완료와 실제 로그인 검증 완료를 구분.
- 위험 요소: 로그인용 키·Firebase 연결·Identity 활성화 및 실제 인증 미검증. 예상 밖 수정 없음.
- 다음 작업: Apple 로그인용 키 준비 후 Firebase 연결 검증. Jira 변경 없음.

## 2026-09-30 — Apple 로그인 키 등록 전 구성 확인

- 브랜치: develop.
- 작업 목표: 현재 키 설정 그대로 등록 가능한지 읽기 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 등록 화면에서 로그인용 이름 및 Sign in with Apple만 선택 확인. Edit에서 Primary App ID com.toteacher.app 및 그룹 내 com.toteacher.app.firebase 확인 후 Back으로 복귀. 추가 기능은 미선택 유지.
- 테스트와 결과: 브라우저 구성 확인, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 키 생성·권한 변경·외부 저장 없음, 비밀정보 비기록.
- 결정사항: 현재 선택 구성이 Firebase Apple 로그인 목적과 일치하며 사용자 Continue/Register 후 안전한 키 다운로드 단계로 진행 가능.
- 위험 요소: 키 실제 발급·Firebase 연결·실로그인 미완료. 예상 밖 수정 없음.
- 다음 작업: 사용자 키 발급 후 Firebase provider 설정. Jira 변경 없음.

## 2026-09-30 — Apple 키 구성 확인 작업 식별 기록 보완

<!-- codex-turn:01a0f106-82c6-7590-8c0f-4d1253eeda65 -->

- 브랜치: develop.
- 작업 목표: Apple 키 등록 전 구성 확인의 현재 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Sign in with Apple만 선택되고 Primary App ID 및 Firebase Services ID가 연결된 설정 확인 결과 기록. 사용자 등록·다운로드 절차와 안전한 보관 안내.
- 테스트와 결과: git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 키 생성·외부 설정 변경 없음.
- 결정사항: 확인된 구성이 로그인 목적과 일치함.
- 위험 요소: 실제 키 발급·Firebase 연결·로그인 미검증. 예상 밖 수정 없음.
- 다음 작업: 사용자 발급 후 Firebase 연결 확인. Jira 변경 없음.

## 2026-09-30 — Apple 키 다운로드 후 Firebase 연결 안내

<!-- codex-turn:01a0f107-a3fc-7dd3-b6e0-a66557eb9cce -->

- 브랜치: develop.
- 작업 목표: 사용자 키 다운로드 완료 보고 후 Firebase Apple provider 입력 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Firebase Authentication Apple의 서비스 ID, 팀 ID, 키 ID 및 비공개 키 입력 위치 설명. 사용자 직접 입력과 안전한 파일 보관 안내; 다운로드 파일을 열거나 원문을 수집하지 않음.
- 테스트와 결과: 안내만 수행. 코드 변경 없어 Gradle 미실행; git diff --check 수행.
- 유지한 계약: 비밀정보 비기록, 외부 설정·권한 변경 없음.
- 결정사항: 다운로드 완료는 사용자 보고 기준이며 Firebase 저장 및 실제 로그인 완료와 구분.
- 위험 요소: provider 저장·Identity 활성화·실제 Apple 인증 미검증. 예상 밖 수정 없음.
- 다음 작업: 사용자 Firebase 저장 후 구성 상태 및 테스트 준비 확인. Jira 변경 없음.

## 2026-09-30 — Firebase Apple 연결 안내 작업 식별 기록 보완

<!-- codex-turn:01a0f107-f2b2-7e33-bf9b-02b2eff722a5 -->

- 브랜치: develop.
- 작업 목표: 다운로드 후 Firebase 연결 안내의 정확한 현재 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 다운로드 완료 보고와 Firebase Apple provider 입력 안내 결과를 기록. 과거 기록은 수정하지 않음.
- 테스트와 결과: git diff --check 수행. 코드 변경 없는 안내로 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, 다운로드 파일 미열람, 외부 설정 변경 없음.
- 결정사항: Firebase 저장과 실제 인증 성공은 아직 확인하지 않음.
- 위험 요소: provider 저장·Identity 활성화·실로그인 미검증. 예상 밖 수정 없음.
- 다음 작업: 사용자 Firebase 저장 후 구성 상태 확인. Jira 변경 없음.

## 2026-09-30 — Firebase Apple provider 저장 상태 확인

<!-- codex-turn:01a0f10b-51f2-7352-a6be-256d9c0119dc -->

- 브랜치: develop.
- 작업 목표: 사용자 등록 후 Firebase Apple 연결 설정 저장 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Chrome Firebase Apple 활성 상태 및 재개방한 설정에서 서비스 식별자 일치, 팀/키 식별자와 비공개 키 필드 입력 존재 확인. 원문 출력 없이 존재 여부만 조회하고 저장 없이 취소. 서버 설정의 Apple 기본 비활성 확인.
- 테스트와 결과: UI 저장 항목 재조회 성공 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, 외부 설정·배포 변경 없음.
- 결정사항: Firebase 구성 저장 확인 완료이나 키 유효성과 Apple 실제 로그인 성공은 아직 미검증. 배포 환경의 활성 플래그는 별도 확인 필요.
- 위험 요소: 현재 ECS 플래그 미조회, 프론트 Apple 인증 및 Identity 교환 미검증. 예상 밖 수정 없음.
- 다음 작업: 테스트 Identity 활성 설정 확인 후 필요한 변경 승인 및 실제 Apple 로그인 검증. Jira 변경 없음.

## 2026-09-30 — 테스트 Identity Apple 활성 플래그 실조회

- 브랜치: develop.
- 작업 목표: 배포된 테스트 Identity Apple 설정 확인 및 비활성 시 활성화 준비.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: ECS 테스트 서비스가 사용하는 tosunsaeng-identity-test:5에서 FIREBASE_APPLE_ENABLED=false 확인. Firebase 인증/Google/전화 활성 및 Kakao 비활성 확인. 새 개정에서 Apple 플래그만 true로 변경하고 테스트 서비스에 배포할 범위를 안내.
- 테스트와 결과: AWS 실화면 조회, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 기존 이미지·Secret 참조·JWT·병합 이벤트·운영 서비스 유지 예정. 실제 변경 및 배포 없음.
- 결정사항: 새 로그인 제공자 수용은 보안상 인증 허용 범위 변경이므로 브라우저 적용 직전 사용자 확인 요청.
- 위험 요소: 활성화/재배포 및 Apple E2E는 미완료. 예상 밖 수정 없음.
- 다음 작업: 적용 승인 후 Apple 플래그 하나만 변경한 새 테스트 개정 배포 및 안정성 확인. Jira 변경 없음.

## 2026-09-30 — 테스트 Apple 플래그 조회 작업 식별 기록 보완

<!-- codex-turn:01a0f10d-5828-7030-8ee9-c3e92c22eba7 -->

- 브랜치: develop.
- 작업 목표: 테스트 Identity Apple 활성 설정 조회의 현재 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 테스트 task definition test:5에서 Apple 비활성을 실조회한 결과 및 Apple 플래그만 활성화할 적용 직전 승인 대기 상태 기록.
- 테스트와 결과: git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 운영 및 테스트 배포 변경 없음.
- 결정사항: 승인 후 테스트 서비스에만 설정 변경 적용.
- 위험 요소: 활성화·재배포·Apple 실제 로그인 미완료. 예상 밖 수정 없음.
- 다음 작업: 사용자 적용 승인 후 새 테스트 개정 배포와 안정성 확인. Jira 변경 없음.

## 2026-09-30 — 테스트 Identity Apple 로그인 활성화 배포 완료

- 브랜치: develop.
- 작업 목표: 사용자 적용 승인 후 테스트 Identity의 Apple 로그인만 활성화하고 정상 배포 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 서버 코드 및 기존 사용자 변경은 수정하지 않음.
- 수행 내용: AWS 콘솔에서 test:5 기반 새 개정 작성, 폼 입력 전후 비교로 FIREBASE_APPLE_ENABLED false→true 한 항목 변경 확인. tosunsaeng-identity-test:6 생성 후 tosunsaeng-staging-cluster의 tosunsaeng-identity-test-service에 배포. 이미지 commit 88ff5bedc1ee661ccd38a8a1d2c4dbf3b9f03f2d·Secret 참조·JWT·병합 이벤트·로그 설정 유지, 원하는 태스크1 유지. 운영 미변경.
- 테스트와 결과: ECS 배포 성공 및 running1/pending0 확인. 신규 태스크의 2026-09-30T06:48:35Z Started IdentityApplication 로그 확인. 로드밸런서 조회 시 정상2/비정상0(이전 태스크 정리 구간) 확인. HTTPS health 브라우저 직접 조회는 클라이언트 차단으로 미완료. git diff --check 수행. 설정만 배포하여 Gradle 미실행.
- 유지한 계약: Firebase ID Token→Identity 인증 계약 유지, Kakao OFF 및 기존 Google/전화 상태 유지. 비밀정보 비기록. 커밋·push 없음.
- 결정사항: 테스트 Apple 수용 설정 활성화 완료. workflow가 현재 서비스 task definition을 가져오는 구조임을 확인했으며 다음 배포 시 revision6 설정 유지 필요. 초기 task definition 초안은 과거 구성으로 자동 배포 입력이 아니므로 수정하지 않음.
- 위험 요소: Apple 실제 인증·가입/로그인·토큰 발급 E2E 미실행. 배포 성공과 사용자 인증 성공을 구분. 기존 미커밋 변경 보존, 예상 밖 로컬 변경 없음.
- 다음 작업: 프론트 또는 테스트 도구에서 실제 Apple 로그인 후 Firebase 인증 및 Identity 교환 검증. Jira 변경 없음.

## 2026-09-30 — 테스트 Apple 활성화 배포 작업 식별 기록 보완

<!-- codex-turn:01a0f10f-31d8-7c73-be2e-e6c742812e36 -->

- 브랜치: develop.
- 작업 목표: 이번 테스트 Apple 활성화 배포의 정확한 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 승인된 Apple 플래그만 활성화한 테스트 revision6 배포 및 정상 기동 확인 결과 기록. 과거 항목은 변경하지 않음.
- 테스트와 결과: ECS 배포 성공/running1/pending0 및 신규 시작 로그 확인 결과 유지. 기록 보완 후 git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 운영·이미지·Secret·다른 설정 유지, 비밀정보 비기록. 이번 보완에서 추가 외부 변경 없음.
- 결정사항: 테스트 배포 완료와 실제 Apple 인증 검증을 구분.
- 위험 요소: Apple 로그인 및 Identity 토큰 발급 E2E 미실행, 직접 HTTPS health 조회 미완료. 예상 밖 수정 없음.
- 다음 작업: 실제 Apple 인증 및 Identity 교환 검증. Jira 변경 없음.

## 2026-09-30 — 로컬 통합 테스트 화면 Apple 로그인 추가

<!-- codex-turn:01a0f119-e71e-7620-b3ef-121870f401e5 -->

- 브랜치: develop. Jira: 이번 요청에 연결된 이슈 없음, Jira 변경 없음.
- 작업 목표: 기존 로컬 테스트 화면에서 Apple 로그인부터 기존 회원가입·병합·챌린지 흐름을 수동 검증할 수 있도록 확장.
- 변경 파일: 별도 프로젝트 `/Users/msde76/tosunsaeng-integration-test`의 app.js, index.html, apple.test.mjs, README.md 및 Identity docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Firebase Apple OAuthProvider와 email/name scope, Apple 로그인/동일 UID 재인증, 제공자별 안내 및 Google/Apple 전환 방지 추가. 명시적 로컬 초기화 후 제공자 전환. 동일 계정 재인증은 Guest/MEMBER/녹음 상태 유지. 기존 테스트 기능 유지.
- 실행한 테스트와 결과: Node app/apple/merge/challenge/server 테스트 7개 통과(최초 sandbox 포트 권한 오류 후 승인된 재실행). Identity ./gradlew clean test 통과(캐시 권한 승인 후). git diff --check 통과. 127.0.0.1:4173 실행 확인. 새 Chrome 탭 UI 조회는 클라이언트 접근 차단으로 미완료, 기존 인증 탭 새로고침하지 않음.
- 유지한 계약: Identity API·JWT·보안 정책·서버 코드·배포 미변경, LC/챌린지 테스트 코드는 별도 프로젝트 유지. 비밀키 입력 및 토큰 원문 표시 없음. 커밋/push 없음.
- 결정사항: 자동 제공자 연결/병합하지 않음. Apple 실제 로그인은 사용자 인증 후 수동 검증. 새 화면 로딩 시 메모리 세션 초기화 주의.
- 위험 요소: Mock 통과는 실제 Apple OAuth 성공을 의미하지 않음. 신규 Apple 계정에 기존 Firebase 계정의 전화번호를 연결하면 충돌 가능. 브라우저 화면 QA 및 실제 Android/iOS 로그인 미확인. 기존 미커밋 사용자 변경 보존, 예상 밖 수정 없음.
- 다음 작업: Chrome에서 localhost:4173 새 화면을 열고 Firebase 초기화→Apple 인증→Identity 교환, 신규 계정은 전화번호/동의/가입, 기존 MEMBER는 프로필/재발급 확인. 배포 전 서버 Apple 플래그 유지 및 실제 앱 SDK 흐름 별도 검증.

## 2026-09-30 — Apple 테스트 PROVIDER_RELINK_REQUIRED 원인 조사

<!-- codex-turn:01a0f125-d451-7be2-becd-0b382bed97af -->

- 브랜치: develop. Jira: 요청에 연결된 키 없음, 변경 없음.
- 작업 목표: 사용자가 보고한 HTTP 409 PROVIDER_RELINK_REQUIRED 의미와 대응 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. FirebaseExchangeService.ensureNoSocialIdentityOwner 및 ProviderChangeGuard.validatePrincipal/authenticate에서 기존 바인딩의 로그인 제공자 승인 누락 또는 차단 시 발생함을 확인. 로컬 테스트 앱은 signInWithPopup/reauthenticateWithPopup만 지원하며 공통 provider link 기능은 미구현임을 확인.
- 실행한 테스트와 결과: 소스 및 frontend-firebase-auth-integration-guide 공통 link 계약 정적 조회, git diff --check. 실행 코드 변경 없는 분석으로 자동 테스트 미실행.
- 유지한 계약: Firebase 인증과 Identity 로그인 제공자 승인을 구분, prepare/start/SDK link/대상 재인증/complete 필수 흐름 유지. DB·Firebase 계정·배포·토큰·보호 설정 미변경.
- 결정사항: 코드상 가능한 원인과 실제 계정 원인 확정을 구분. 기존 승인 SNS로 인증 후 정식 연결 필요하며 자동 계정 삭제·강제 승인하지 않음.
- 위험 요소: 실제 발생 요청 경로·배포 버전·계정 승인 및 차단 상태 미조회. 현재 오류만으로 과거 연결 해제를 단정할 수 없음. 기존 사용자 변경 보존, 예상 밖 수정 없음.
- 다음 작업: 실패 API/계정 연결 상태를 확인하고 필요 시 사용자 요청에 따라 테스트 화면에 공통 SNS 연결 기능 구현. 비밀정보 공유 불필요.

## 2026-09-30 — 새 Apple 계정 가입 테스트 안내

- 브랜치: develop. Jira: 연결된 이슈 없음.
- 작업 목표: 기존 provider 연결 오류와 독립된 신규 계정 테스트 방법 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 로컬 초기화 후 미연결 Apple 계정으로 로그인→Identity 가입 준비→ENROLLMENT_REQUIRED 확인→미사용 테스트 전화번호 연결 및 동의·가입 안내. 동일 Apple 계정으로 새로 로그인해도 새 서버 계정이 되지 않음을 설명.
- 실행한 테스트와 결과: 직전 소스/계약 확인 결과 기반 안내. 코드 변경 없어 테스트 미실행, git diff --check 수행.
- 유지한 계약: 자동 계정 삭제·Firebase unlink·서버 승인 우회 없음. 로컬 메모리 초기화와 서버 계정 삭제 구분.
- 결정사항: 기존 계정을 보존하며 별도 신규 계정으로 테스트. Apple 이메일 숨기기나 이메일 변경을 새 계정 생성 수단으로 안내하지 않음.
- 위험 요소: 신규 Apple 계정 보유 여부 및 전화번호 중복 상태 미확인. 기존 사용자 변경 보존, 예상 밖 수정 없음.
- 다음 작업: 사용자 직접 Apple 인증 후 enrollment 응답 확인. 외부 설정·배포 변경 없음.

## 2026-09-30 — 신규 Apple 가입 안내 작업 식별 기록 보완

<!-- codex-turn:01a0f127-1fa0-7473-a14d-016fb48cb2e1 -->

- 브랜치: develop.
- 작업 목표: 이번 신규 Apple 계정 가입 안내의 정확한 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 별도 미연결 Apple 계정으로 인증 후 신규 enrollment 및 전화번호·동의·가입 절차를 안내한 결과 기록. 과거 항목 보존.
- 테스트와 결과: git diff --check 수행. 문서 기록만 보완하여 실행 테스트 미실행.
- 유지한 계약: 로컬 초기화는 서버 계정 삭제가 아님. 제공자 연결 승인 우회·외부 상태 변경·비밀정보 기록 없음.
- 결정사항: 같은 Apple 계정 재로그인을 신규 계정 생성으로 취급하지 않음.
- 위험 요소: 실제 신규 가입 미검증, 예상 밖 수정 없음.
- 다음 작업: 사용자 직접 인증 후 신규 가입 응답 확인. Jira 변경 없음.

## 2026-09-30 — 현재 Apple 계정 사용 가능 여부 설명 보완

- 브랜치: develop. Jira: 연결된 이슈 없음.
- 작업 목표: 현재 로그인한 계정을 반드시 교체해야 하는지에 대한 오해 해소.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 안내만 수행. Firebase 인증 성공과 Identity의 provider 승인 상태를 구분하고 별도 Apple 계정은 신규 가입 검증용 선택지임을 명시.
- 테스트와 결과: 앞선 소스 조사 근거 활용. 코드 변경 없어 실행 테스트 미실행, git diff --check 수행.
- 유지한 계약: 현재 로그인 세션·DB·연결 상태 변경 및 보호 조건 우회 없음.
- 결정사항: 현재 계정 상태를 확인하기 전에 다른 계정 사용이나 기존 계정 삭제를 필수 조치로 제시하지 않음.
- 위험 요소: 실제 계정의 승인 누락/차단 원인 미확정. 예상 밖 수정 없음.
- 다음 작업: 현재 계정의 Firebase/Identity 연결 상태를 읽기 전용으로 확인하여 정식 연결 경로 결정.

## 2026-09-30 — 현재 Apple 계정 유지 안내 식별 기록 보완

<!-- codex-turn:01a0f128-0010-7923-bda5-c647bc222d71 -->

- 브랜치: develop.
- 작업 목표: 현재 Apple 계정 사용 안내의 정확한 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 계정 교체가 필수는 아니며 Firebase 인증과 Identity 승인이 별개임을 안내한 결과 기록. 과거 기록 보존.
- 테스트와 결과: git diff --check 수행. 문서 보완만 하므로 실행 테스트 미실행.
- 유지한 계약: 계정 삭제·세션 변경·승인 우회·비밀정보 기록 없음.
- 결정사항: 기존 계정 상태를 보존하고 연결 상태 확인 후 대응.
- 위험 요소: 실제 계정의 승인 누락/차단 원인 미확정, 예상 밖 수정 없음.
- 다음 작업: 현재 계정의 연결 상태 확인. Jira 변경 없음.

## 2026-09-30 — Chrome Apple 로그인 차단 단계 실조회

- 브랜치: develop. Jira: 연결된 이슈 없음.
- 작업 목표: 사용자가 로그인한 실제 테스트 탭에서 차단 위치 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Chrome 기존 localhost 테스트 화면의 검증표에서 Apple 인증 성공과 exchange HTTP 409 PROVIDER_RELINK_REQUIRED 확인. 전화번호 연결/회원가입/프로필/재발급/LC 버튼 비활성 확인. app.js의 exchange 요청이 POST /api/v1/auth/firebase/exchange임을 소스로 대조.
- 테스트와 결과: 기존 탭 접근성 화면 읽기 및 요청 매핑 정적 확인. 새로고침·버튼 클릭·네트워크 재요청 없이 수행. git diff --check 확인. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 현재 인증 메모리·계정·DB·배포 상태 보존. 비밀정보 조회/기록 없음.
- 결정사항: Firebase 인증 성공 이후 Identity 로그인/가입 준비 단계 차단으로 확정. Apple 로그인 자체 실패나 전화번호 단계 실패로 해석하지 않음.
- 위험 요소: 해당 계정의 승인 누락인지 과거 차단인지 화면만으로 구분 불가. 실제 배포 코드/DB 대조 미완료. 예상 밖 수정 없음.
- 다음 작업: 필요 시 테스트 Identity의 Firebase 바인딩·SocialIdentity·provider 차단 상태를 읽기 전용 대조해 원인 확정. 외부 변경 없음.

## 2026-09-30 — Chrome 차단 단계 조회 식별 기록 보완

<!-- codex-turn:01a0f129-008d-79c1-bbd4-07ae4be59530 -->

- 브랜치: develop.
- 작업 목표: 현재 Chrome 테스트 탭 실조회 작업의 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Apple Firebase 인증 성공, Identity exchange HTTP 409 PROVIDER_RELINK_REQUIRED 및 후속 단계 비활성 확인 결과 기록. 과거 항목 보존.
- 테스트와 결과: 화면 읽기 및 소스 요청 매핑 확인 결과 유지. git diff --check 수행. 문서 보완만으로 실행 테스트 미실행.
- 유지한 계약: 탭 새로고침·로그아웃·재요청·계정 변경 없음. 비밀정보 비기록.
- 결정사항: 차단 단계는 확인했으나 실제 계정 승인 누락/차단 원인은 미확정으로 구분.
- 위험 요소: 배포 코드·DB 대조 미완료. 예상 밖 수정 없음.
- 다음 작업: 테스트 계정 연결 상태 읽기 전용 확인. Jira 변경 없음.

## 2026-09-30 — Apple 로그인 승인 불일치 원인 실확인

<!-- codex-turn:01a0f12e-6c76-7680-9a3c-d53f892f18cd -->

- 브랜치: develop. Jira: 요청에 연결된 이슈 없음, 변경 없음.
- 작업 목표: Apple 로그인 후 Identity exchange 409의 실제 데이터 원인 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Atlas 테스트 프로젝트의 to-teacher-identity-test에서 firebase_identities 전체 1건과 social_identities 전체 1건 대조. 동일 계정의 승인 provider는 GOOGLE뿐이고 APPLE 없음. Firebase Authentication 사용자 전체 1건에서 해당 UID에 Google/Apple/Phone 연결 확인. 테스트 DB 전체 21개 컬렉션 목록에 auth_method_change_controls 없음. 이전 확인 배포 commit 88ff5bed의 ProviderChangeGuard와 현재 소스에서 현재 로그인 provider/subject 승인 누락 시 해당 409 반환 확인.
- 테스트와 결과: 실제 Firebase/Atlas 읽기 전용 화면 대조 및 git show 정적 검증, git diff --check. 코드 변경 없어 실행 테스트 미실행. 로그인 탭 새로고침·로그아웃·exchange 재요청 없음.
- 유지한 계약: 계정/DB/인증 제공자/접근 권한/배포 미변경. 개인 식별값·자격증명·토큰은 문서에 기록하지 않음. 운영 DB 미조회.
- 결정사항: 신규 Apple 계정이 아니라 기존 Firebase 계정에 Apple이 연결돼 있고 Identity 승인 정보가 따라오지 않은 상태로 판정. 단순 재로그인이나 계정 교체를 필수 해결책으로 제시하지 않음. 서버 보호 조건 우회/DB 강제 삽입하지 않음.
- 위험 요소: Firebase 연결이 형성된 경위는 미확인. 이미 연결된 Apple을 정식 provider link 흐름으로 승인하는 복구 조건은 추가 검토 필요. 최신 배포 revision 재조회는 미수행. 기존 사용자 변경 보존, 예상 밖 수정 없음.
- 다음 작업: 사용자가 수정을 요청하면 공통 provider link의 기존 Firebase 연결 처리·복구 조건을 확인한 뒤 테스트 화면에 해당 흐름 구현 및 검증. 실제 동의/연결 변경은 별도 확인.

## 2026-09-30 — Apple 인증과 Identity 연결 승인 차이 설명

<!-- codex-turn:01a0f131-3fdb-7839-a620-1b01d178bf1e -->

- 브랜치: develop.
- 작업 목표: 사용자에게 현재 Apple 로그인 차단 이유를 쉬운 용어로 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 앞서 확인한 Firebase Google/Apple/Phone 연결과 Identity GOOGLE만 등록된 상태를 바탕으로 본인 인증과 서비스 계정 로그인 승인을 구분해 설명.
- 테스트와 결과: 기존 실조회 결과 기반 안내, git diff --check 수행. 코드 변경 없어 실행 테스트 미실행.
- 유지한 계약: 계정·세션·DB·외부 설정 변경 및 승인 우회 없음. 비밀정보 비기록.
- 결정사항: 새 Apple 계정 사용이나 기존 계정 삭제가 필수라는 오해 해소. Firebase 연결 생성 경위는 단정하지 않음.
- 위험 요소: 정식 연결 복구 흐름의 기존 Firebase 연결 처리 조건 미검증. 예상 밖 수정 없음.
- 다음 작업: 요청 시 정식 연결 절차 및 테스트 화면 보완 검토. Jira 변경 없음.

## 2026-09-30 — 로그인 승인 설명 작업 식별 기록 보완

<!-- codex-turn:01a0f131-7898-7bf2-8879-4fbe3ec015e4 -->

- 브랜치: develop.
- 작업 목표: 이번 설명 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 Firebase 인증 성공과 Identity 제공자 승인 누락을 설명한 작업의 식별 기록 추가.
- 테스트와 결과: git diff --check 수행. 문서 기록 보완만으로 실행 테스트 미실행.
- 유지한 계약: 계정·세션·DB·설정 미변경, 비밀정보 비기록.
- 결정사항: 기존 계정에 Apple 연결을 원하는지는 사용자 의도 확인이 필요하며 실제 변경하지 않음.
- 위험 요소: 연결 복구 조건 미검증. 예상 밖 수정 없음.
- 다음 작업: 사용자 의도에 맞게 정식 연결 또는 별도 가입 흐름 검토. Jira 변경 없음.

## 2026-09-30 — Apple 전역 활성화와 계정 연결 검사 구분

<!-- codex-turn:01a0f135-a035-71a1-97e4-8737e1098e06 -->

- 브랜치: develop.
- 작업 목표: 서버 Apple 기능을 켰는데도 409가 발생하는 이유를 코드 근거로 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. FirebaseAdminAuthenticationVerifier의 validateProviderPolicy와 후속 ProviderChangeGuard.validatePrincipal 호출, appleEnabled 참조 확인. 전역 기능 활성화가 기존 회원의 SocialIdentity 생성이나 승인 누락 검사를 대체하지 않음을 설명.
- 테스트와 결과: 소스 정적 조회 및 git diff --check. 코드 변경 없는 설명으로 실행 테스트 미실행.
- 유지한 계약: Apple 전역 설정·계정·DB·배포 미변경, 비밀정보 비기록.
- 결정사항: 앞선 허용이라는 표현을 전역 기능 활성화와 계정별 연결 확인으로 명확히 구분. 운영자가 회원마다 수동 허용해야 한다는 의미가 아님.
- 위험 요소: 이미 Firebase에 연결된 제공자 복구 흐름 검증은 남음. 예상 밖 수정 없음.
- 다음 작업: 요청 시 정식 계정 연결 흐름의 현재 데이터 상태 처리 확인 및 테스트 UI 보완. Jira 변경 없음.

## 2026-09-30 — 기존 Firebase Apple 연결의 정식 복구 가능성 조사

- 브랜치: develop. Jira: 연결된 이슈 없음.
- 작업 목표: 전역 Apple 활성화 이후 계정 연결 불일치의 생성 경위와 기존 link API 복구 가능성 점검.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 분석만 수행. ProviderLinkService.prepare에서 원격 연결이 있으면 ownedSocial로 기존 승인 레코드를 필수 조회함을 확인. 현재 원격 APPLE 연결/로컬 APPLE 없음 상태는 link 기능 활성화 시에도 SOCIAL_IDENTITY_CONFLICT 발생 조건. prepare 이전 원격 연결을 소급 승인하지 않는 주석과 start의 원격 연결 거절 조건 확인. 해당 서비스는 이전 확인 배포 commit 88ff5bed와 diff 없음. 로컬 테스트 화면은 Apple signInWithPopup/reauthenticateWithPopup만 수행하고 SNS 연결 호출은 없음; linkWithCredential은 전화번호용.
- 테스트와 결과: 소스 및 배포 commit 비교, git diff --check. 코드 변경 없어 실행 테스트 미실행. 실제 계정 mutation/API 재실행 없음.
- 유지한 계약: 기능 활성화와 계정 연결 검증 분리, 소급 자동 승인·DB 수동 삽입·Firebase 해제 미실행. 비밀정보 비기록.
- 결정사항: 현재 상태는 테스트 UI에 연결 버튼만 추가해 해결할 수 없으므로 앞선 단순화 설명 정정. 연결 생성 경위는 자동 연결 등 가능성만 있으며 확정하지 않음. 안전한 불일치 복구 흐름 검토가 선행되어야 함.
- 위험 요소: Firebase 연결 생성 당시 이벤트/로그 미확보. link 플래그 현재 배포값 미조회, 따라서 실제 prepare HTTP 결과는 미실행 상태. 예상 밖 수정 없음.
- 다음 작업: 사용자 요청 시 기존/대상 SNS 소유권 확인 및 계정 충돌 방지를 포함하는 복구 설계와 테스트 계획 마련. 계정 삭제나 강제 연결 해제로 우회하지 않음.

## 2026-09-30 — Apple 연결 복구 조사 작업 식별 기록 보완

<!-- codex-turn:01a0f136-c432-7520-bb9f-0d575ca063ca -->

- 브랜치: develop.
- 작업 목표: 이번 연결 복구 조사 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 기존 link prepare의 승인 누락 거절 조건 및 UI만으로 복구되지 않는다는 조사 결과의 작업 식별자를 추가.
- 테스트와 결과: git diff --check 수행. 문서 보완만으로 실행 테스트 미실행.
- 유지한 계약: 계정·DB·서버 코드·설정 미변경, 비밀정보 비기록.
- 결정사항: 복구 설계 검토 전 강제 승인 또는 연결 해제하지 않음.
- 위험 요소: Firebase 연결 생성 경위 미확정, 실제 복구 미검증. 예상 밖 수정 없음.
- 다음 작업: 요청 시 안전한 불일치 복구 설계 및 테스트 계획 수립. Jira 변경 없음.

## 2026-09-30 — 다중 SNS 지원과 연결 불일치 구분

<!-- codex-turn:01a0f138-0bc6-7f57-9640-70143b10d2d3 -->

- 브랜치: develop.
- 작업 목표: 다중 SNS 로그인 허용 자체가 충돌 원인인지 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 분석만 수행. SocialIdentity의 provider/subject 고유 인덱스와 ProviderLinkService.complete의 대상 provider 추가/동일 provider 중복 검사 확인. 다른 SNS가 이미 등록돼 있다는 이유만으로 새 SNS를 거절하는 구조는 아님.
- 테스트와 결과: 코드 정적 조회 및 git diff --check. 코드 변경 없어 실행 테스트 미실행.
- 유지한 계약: 한 회원에 복수 SNS 연결 지원, 다른 계정 소유 또는 동일 provider 중복 방지 유지. 외부 데이터·설정 미변경.
- 결정사항: 현재 오류는 다중 SNS 자체 충돌이 아닌 Firebase/Identity 연결 상태 불일치로 설명. 정식 연결 흐름과 외부에서 먼저 생긴 연결의 복구 공백을 구분.
- 위험 요소: 최초 설계 전체 이력과 Firebase 연결 생성 경위는 미확정. 예상 밖 수정 없음.
- 다음 작업: 요청 시 기존 다중 SNS 지원을 유지하는 안전한 불일치 복구 설계 검토. Jira 변경 없음.

## 2026-09-30 — 다중 SNS 지원 설명 작업 식별 기록 보완

<!-- codex-turn:01a0f138-9261-75c3-9c98-e9fadd0c0848 -->

- 브랜치: develop.
- 작업 목표: 현재 설명 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 다중 SNS 지원과 계정 연결 정보 불일치를 구분한 설명의 작업 식별자를 추가.
- 테스트와 결과: git diff --check 수행. 문서 보완만으로 실행 테스트 미실행.
- 유지한 계약: 복수 SNS 지원 및 계정 충돌 방지 유지. 코드·DB·설정 미변경, 비밀정보 비기록.
- 결정사항: 현재 오류를 다중 SNS 기능 자체의 충돌로 단정하지 않음.
- 위험 요소: Firebase 연결 생성 경위와 불일치 복구 절차 미검증. 예상 밖 수정 없음.
- 다음 작업: 요청 시 안전한 불일치 복구 설계 검토. Jira 변경 없음.

## 2026-09-30 — Apple 로그인 차단 조건과 보안 목적 설명

- 브랜치: develop.
- 작업 목표: 차단의 직접 조건과 검사 목적을 구분하여 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. ProviderChangeGuard.validatePrincipal의 기존 바인딩 및 현재 provider/subject 승인 레코드 검사 재확인. 현재 APPLE 레코드 부재로 거절되는 상태와 과거 계정 차단을 구분.
- 테스트와 결과: 소스 정적 확인, git diff --check 수행. 설명 작업으로 실행 테스트 미실행.
- 유지한 계약: 외부 연결만으로 서비스의 승인/해제 절차 우회 금지. 코드·계정·설정 미변경, 비밀정보 비기록.
- 결정사항: 기능 OFF나 계정 정지가 아니라 승인된 연결 정보 부재에 의한 요청 거절로 설명. Firebase 연결 생성 경위는 단정하지 않음.
- 위험 요소: 실제 불일치 생성 원인과 복구 절차는 추가 확인 필요. 예상 밖 수정 없음.
- 다음 작업: 요청 시 연결 불일치 복구 설계 검토. Jira 변경 없음.

## 2026-09-30 — Apple 차단 조건 설명 작업 식별 기록 보완

<!-- codex-turn:01a0f13a-3ebc-7d83-b109-a9008978d8d2 -->

- 브랜치: develop.
- 작업 목표: 현재 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 수정하지 않고 Apple 승인 레코드 누락에 따른 거절 조건 설명의 작업 기록 추가.
- 테스트와 결과: git diff --check 수행. 문서 보완만으로 실행 테스트 미실행.
- 유지한 계약: 코드·계정·DB·외부 설정 미변경, 비밀정보 비기록.
- 결정사항: 확인한 차단 조건과 미확정인 불일치 생성 경위를 구분.
- 위험 요소: 실제 불일치 생성 경위와 복구 절차 미검증. 예상 밖 수정 없음.
- 다음 작업: 요청 시 연결 불일치 원인 및 복구 절차 추가 조사. Jira 변경 없음.

## 2026-09-30 — Firebase 신뢰된 제공업체 자동 연결 정책 조사

- 브랜치: develop.
- 작업 목표: Apple 연결이 Firebase에만 생기는 경로를 공식 동작과 코드로 대조.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 공식 https://firebase.google.com/docs/auth/users#verified_email_addresses 실조회로 동일 이메일의 신뢰된 제공업체 로그인 시 자동 연결, Apple의 신뢰된 제공업체 분류, Google의 Gmail 조건 확인. 로컬 app.js는 Apple signInWithPopup/reauthenticateWithPopup만 호출하고 명시적 SNS link나 Identity sync 호출이 없음을 확인. FirebaseSignupService는 신규 가입 때 연결 SNS를 준비하지만 exchange는 기존 바인딩의 미등록 SNS를 거절하고 자동 저장하지 않음. 기존 sync 역시 승인된 연결 검증만 수행.
- 테스트와 결과: 공식 문서 브라우저 조회·코드 정적 대조·git diff --check. 코드 변경 없어 실행 테스트 미실행. 계정 로그인/연결 재현 mutation 미수행.
- 유지한 계약: 계정·DB·배포·설정 미변경, 비밀정보 비기록. 원인 조사와 수정 권한 구분.
- 결정사항: Firebase 자동 연결이 현재 상태와 부합하는 가장 유력한 생성 경로이며, 앱에서 link API를 직접 호출하지 않아도 발생 가능. Identity의 명시적 연결 승인 모델이 이 경로를 처리하지 못하는 통합 공백 확인. 사용자 실수나 테스트 화면 연결 버튼 누락만으로 단정하지 않음.
- 위험 요소: 해당 로그인 당시 이벤트 로그 및 연결 전 snapshot 미확보로 이번 연결 순간의 직접 증명은 아님. 자동 연결 전체를 무조건 승인하면 기존 해제/차단 보호를 훼손할 수 있어 별도 복구 설계 필요. 예상 밖 수정 없음.
- 다음 작업: 요청 시 Firebase 자동 연결을 고려한 안전한 계정 연결/복구 정책 및 회귀 테스트 설계. Jira 변경 없음.

## 2026-09-30 — Firebase 자동 연결 정책 조사 식별 기록 보완

<!-- codex-turn:01a0f13c-7853-7823-af05-3a9850b7edc2 -->

- 브랜치: develop.
- 작업 목표: 이번 자동 연결 정책 조사 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 공식 Firebase 자동 연결 정책 및 Identity 명시적 승인 모델의 불일치 조사 결과 식별 기록 추가.
- 테스트와 결과: git diff --check 수행. 문서 보완만으로 실행 테스트 미실행.
- 유지한 계약: 계정·DB·서버 코드·설정 미변경, 비밀정보 비기록.
- 결정사항: 공식적으로 가능한 자동 연결 동작과 이번 개별 사건의 직접 증명을 구분.
- 위험 요소: 당시 이벤트 로그 미확보, 실제 연결 생성 순간 미확정. 예상 밖 수정 없음.
- 다음 작업: 요청 시 안전한 자동 연결 대응 및 복구 정책 설계. Jira 변경 없음.

## 2026-09-30 — Apple 연결 해제 후 재현 범위 확인

- 브랜치: develop.
- 작업 목표: 사용자 요청의 Apple 연결 해제와 재로그인 재현 범위를 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 전체 계정 삭제가 아닌 테스트 Firebase 사용자의 Apple provider만 해제하는 범위를 설명하고 적용 전 사용자 확인 요청. Google/전화번호/UID/Identity 회원/기록은 유지 대상.
- 테스트와 결과: 안내 및 기록만 수행, git diff --check. 실행 테스트·계정 변경 미수행.
- 유지한 계약: 기존 계정 및 인증 수단 보호, 비밀정보 비기록. 승인 전 unlink/사용자 삭제 없음.
- 결정사항: 재로그인 시 Firebase 자동 재연결과 동일 오류가 발생할 수 있으므로 해결이 아닌 재현 실험으로 설명. Apple 로그인을 다시 수행할 때 사용자 직접 인증 필요.
- 위험 요소: 연결 해제는 해당 로그인 수단을 제거하며 세션 영향 확인 필요. 실제 해제 방법·재현은 승인 후 검토. 예상 밖 수정 없음.
- 다음 작업: 범위 승인 후 지원되는 provider 해제 방법 확인 및 변경 전후 연결 상태 비교. Jira 변경 없음.

## 2026-09-30 — Apple 재현 실험 목적 및 콘솔 해제 기능 확인

- 브랜치: develop.
- 작업 목표: 오류 재현 요청에 맞춰 Apple provider만 해제 가능한 경로 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Firebase 테스트 사용자 메뉴 읽기 전용 조회, 전체 계정 삭제/비활성화 및 비밀번호 재설정만 표시됨을 확인. Apple 단독 해제는 콘솔 메뉴에서 제공되지 않으므로 Firebase SDK 기반 테스트 UI 경로 검토.
- 테스트와 결과: 실제 콘솔 메뉴 확인 및 git diff --check. unlink/로그인 재현은 미실행, 자동 테스트 미실행.
- 유지한 계약: Google·전화번호·UID·Identity 회원과 기록 보존, 전체 사용자 삭제 금지. 비밀정보 비기록.
- 결정사항: 해결 작업이 아닌 해제 전후 provider/UID 및 exchange 결과 비교 실험으로 범위 명확화. 실제 연결 변경 직전 확인 요청.
- 위험 요소: Apple 연결 해제는 인증 수단 변경이며 세션 영향 가능. 재로그인 시 사용자 직접 인증 필요. 예상 밖 수정 없음.
- 다음 작업: Apple 단독 해제 범위 확인 후 재현용 SDK 조작 준비 및 변경 전후 검증. Jira 변경 없음.

## 2026-09-30 — Apple 재현 실험 준비 작업 식별 기록 보완

<!-- codex-turn:01a0f13e-1388-7711-bdc3-0df9a4049ed8 -->

- 브랜치: develop.
- 작업 목표: 이번 재현 실험 준비 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 Firebase 콘솔의 Apple 단독 해제 메뉴 부재 및 실제 변경 전 범위 확인 대기 상태를 기록.
- 테스트와 결과: git diff --check 수행. 문서 보완만으로 실행 테스트 미실행.
- 유지한 계약: 계정·DB·코드·설정 미변경, 비밀정보 비기록. 전체 사용자 삭제 없음.
- 결정사항: Google·전화번호·회원 기록을 유지하는 Apple 연결 단독 해제 승인 후 재현 진행.
- 위험 요소: 실제 unlink와 재로그인 결과 미검증. 예상 밖 수정 없음.
- 다음 작업: 사용자 확인 후 지원되는 SDK 경로로 해제 전후 상태 및 동일 오류 재발 여부 검증. Jira 변경 없음.

## 2026-09-30 — 승인된 Apple 단독 해제 및 재로그인 재현 준비

- 브랜치: develop. Jira: 요청에 연결된 이슈 없음.
- 작업 목표: Apple 연결만 해제한 뒤 동일 계정 로그인으로 자동 연결 및 409 재발 검증.
- 변경 파일: 별도 `/Users/msde76/tosunsaeng-integration-test`의 app.js, index.html, apple.test.mjs, README.md 및 docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 재현 버튼에 테스트 프로젝트·Google 인증·Identity 로그인 성공·Google/전화/Apple 존재·Guest 병합 상태 없음 검사 추가. 확인 후 apple.com만 unlink, reload로 UID/나머지 provider 보존 및 Apple 부재 확인. 불명확한 결과에 자동 재해제하지 않도록 메모리 잠금. 로그아웃 후 Apple 로그인 시 동일 UID/자동 연결/기존 provider 유지 boolean 비교. 토큰/사용자 ID 원문 비출력.
- 실제 수행: 사용자 범위 승인에 따라 기존 오류 탭 새로고침 및 기존 공개 Firebase 웹 구성 적용. 기존 테스트 Google 계정 선택 후 Identity MEMBER 로그인 성공. 재현 확인창 승인 후 Apple 단독 해제 성공 표시와 UID/Google/전화 유지 확인. Apple 로그인 창 열었으나 직접 인증 필요하여 대기. 전체 사용자 삭제/Google·전화 해제 없음. 해제된 Apple 연결은 재인증/연결로 복구 가능한 로그인 수단이며 원인 재현 목적으로 분리함.
- 실행한 테스트와 결과: Node app/apple/merge/challenge 테스트 5개 통과(해제 취소, Apple만 unlink, 상태 보존, 로그인 후 자동 연결 Mock 검증 포함). 브라우저에서 신규 재현 섹션/Google 로그인/실제 해제 결과 확인. 새 탭 직접 이동은 클라이언트 차단됐으나 기존 탭 reload 성공. git diff --check. Identity 서버 코드 변경 없어 Gradle 재실행 생략.
- 유지한 계약: 정식 Identity provider API/보호 조건/DB 승인 레코드 미변경, 운영 미변경, 서버 배포 없음. 비밀정보 비기록, 기존 사용자 변경 보존. Identity 로그인에 따른 정상 세션 발급 외 서버 변경 없음.
- 결정사항: 실험 목적의 Firebase 단독 unlink이며 서비스용 해제 기능으로 사용하지 않음. 사용자 Apple 인증 후 동일 UID와 exchange 결과를 확인해야 재현 완료.
- 위험 요소: Apple 실제 재로그인 및 409 재발 미완료. 새로고침하면 비교 메모리 소실. 기존 서버 세션은 로컬 초기화로 폐기되지 않음. 예상 밖 수정 없음.
- 다음 작업: 사용자가 같은 Apple 계정으로 인증 완료 후 비교 결과 및 Identity exchange 확인. 배포 전 확인: 로컬 재현 버튼은 서비스 배포 대상 아님. Jira 댓글/상태 변경 없음.

## 2026-09-30 — Apple 단독 해제 재현 작업 식별 기록 보완

<!-- codex-turn:01a0f13f-c70e-7da3-9ab4-eb80e23fcf6f -->

- 브랜치: develop.
- 작업 목표: 이번 승인된 Apple 재현 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 로컬 재현 UI 추가·실제 Apple 단독 해제·사용자 재인증 대기 결과의 작업 식별 기록 추가.
- 테스트와 결과: 앞선 Node Mock 테스트 5개 통과 및 실제 해제 결과 확인 유지. 이번 문서 보완 후 git diff --check 수행, 실행 테스트 재실행 없음.
- 유지한 계약: 추가 계정·DB·코드·설정 변경 없음, 비밀정보 비기록.
- 결정사항: Apple 재로그인 후 자동 연결과 Identity 오류 확인 전까지 재현 완료로 보고하지 않음.
- 위험 요소: 사용자 Apple 인증 및 409 재발 미검증. 예상 밖 수정 없음.
- 다음 작업: 사용자 인증 완료 후 기존 페이지 비교 상태와 Identity exchange 결과 확인. Jira 변경 없음.

## 2026-09-30 — Apple 자동 재연결 및 Identity 409 실재현 완료

<!-- codex-turn:01a0f14a-183f-7042-9d8f-de39617a950d -->

- 브랜치: develop. Jira: 연결된 이슈 없음.
- 작업 목표: 승인된 재현 실험에서 Apple 재로그인 후 자동 연결과 동일 오류 발생 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 사용자 인증 완료 후 Chrome 기존 테스트 탭의 비교 결과에서 동일 UID/Apple 연결/기존 로그인 수단 유지가 모두 true임을 확인. 앞선 실제 해제 결과(Apple 없음)와 대조. Identity 로그인/가입 준비 버튼을 눌러 exchange HTTP 409 PROVIDER_RELINK_REQUIRED 재발 확인.
- 테스트와 결과: 실제 Firebase Apple 인증→같은 UID로 Apple 재연결→Identity exchange 409 흐름 재현 성공. 신규 signup/명시적 SNS link API 없이 일반 로그인 경로에서 재연결 확인. git diff --check 수행. 코드 변경 없어 자동 테스트 재실행 없음.
- 유지한 계약: Google·전화번호 연결 유지, 전체 계정 삭제·DB 직접 변경·서버 코드/배포 변경 없음. 개인정보·토큰·비밀정보 비기록. 기존 사용자 변경 보존.
- 결정사항: 이번 재현의 자동 재연결은 추정이 아닌 관측 결과. 최초 사건의 과거 생성 로그까지 확보한 것은 아님. Identity의 누락된 승인 레코드 거절과 Firebase 자동 연결 간 호환성 문제를 해결 범위로 제시.
- 위험 요소: 원인 재현 완료이며 해결 완료는 아님. 현재 Apple 연결은 다시 존재하고 Identity Apple 로그인은 여전히 거절됨. 예상 밖 수정 없음.
- 다음 작업: 사용자 요청 시 기존 제공자 해제/차단 보호를 유지하는 자동 연결 처리 및 안전한 복구 계획 수립. 추가 해제/자동 승인하지 않음. Jira 변경 없음.

## 2026-09-30 — 다른 Apple 계정 비교 실험 준비 및 기존 연결 해제

- 브랜치: develop. Jira: 요청에 연결된 이슈 없음.
- 작업 목표: 기존 Apple 연결 해제 후 다른 Apple 계정 로그인에서도 동일 오류가 발생하는지 비교.
- 변경 파일: 별도 로컬 테스트 프로젝트 app.js, apple.test.mjs 및 docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 다른 계정의 Firebase UID 변경은 예상 가능한 결과이므로 재현 로그인 단계에서 중단하지 않고 boolean 비교만 표시. 서로 다른 계정 연결/토큰 복사 없음. 다른 UID Mock 로그인 후 exchange 버튼 활성화 검증 추가.
- 실제 수행: 기존 오류 화면 새로고침 및 공개 웹 설정 재적용, Google 인증/Identity 로그인 성공 확인. 사용자 요청한 Apple 단독 해제 실행 후 동일 UID·Google/전화 유지·Apple 없음 확인. Apple 로그인 창을 열었으며 사용자 이중 인증 단계 대기.
- 테스트와 결과: Node app/apple/merge/challenge 테스트 5개 통과, 실제 해제 후 상태 확인, git diff --check. 서버 코드 미변경으로 Gradle 재실행 생략.
- 유지한 계약: 전체 사용자 삭제·Google/전화번호 해제·Identity DB 직접 변경·배포 없음. 비밀정보 비기록, 기존 사용자 변경 보존.
- 결정사항: Identity exchange 결과 비교까지만 범위로 하며 신규 signup/약관 동의/전화번호 연결은 수행하지 않음.
- 위험 요소: 실제 다른 Apple 계정 여부·UID 및 오류 결과 미확인, 사용자 인증 필요. 현재 기존 Apple 연결은 해제 상태이며 재연결 가능. 예상 밖 수정 없음.
- 다음 작업: 사용자 인증 후 UID 비교 및 exchange 응답 확인. 로컬 재현 기능은 서비스 배포 대상 아님. Jira 변경 없음.

## 2026-09-30 — 다른 Apple 비교 실험 작업 식별 기록 보완

<!-- codex-turn:01a0f14c-14b3-7c60-bbfb-0531777241b7 -->

- 브랜치: develop.
- 작업 목표: 이번 다른 Apple 계정 비교 실험의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 다른 UID 허용 비교 기능·기존 Apple 단독 해제·사용자 인증 대기 결과의 식별 기록 추가.
- 테스트와 결과: 앞선 Node Mock 테스트 5개 통과 및 실제 해제 결과 확인 유지. git diff --check 수행, 문서 보완만으로 실행 테스트 재실행 없음.
- 유지한 계약: 추가 계정·DB·코드·설정 변경 없음, 비밀정보 비기록.
- 결정사항: 다른 Apple 인증 및 exchange 확인 전까지 재현 결과를 확정하지 않음.
- 위험 요소: 사용자 인증 대기, 실제 다른 UID 및 오류 응답 미확인. 예상 밖 수정 없음.
- 다음 작업: 인증 완료 후 기존 페이지의 비교 상태 및 Identity 응답 확인. Jira 변경 없음.

## 2026-09-30 — 다른 Apple 계정의 Identity 가입 준비 정상 응답 확인

- 브랜치: develop. Jira: 연결된 이슈 없음.
- 작업 목표: 다른 Apple 계정 로그인 시 기존 계정 자동 연결 사례와 동일한 409가 발생하는지 검증.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 사용자 Apple 인증 완료 후 Chrome 재현 화면의 동일 UID=false/Apple 연결=true/기존 로그인 수단 유지=false 확인. Identity 로그인/가입 준비 버튼 실행 후 신규 가입 준비 완료 안내 및 전화 인증 시작 버튼 활성 확인.
- 테스트와 결과: 실제 다른 Firebase UID의 Apple 인증 및 exchange 신규 enrollment 경로 성공, 해당 409 재발 없음. 전화 연결/약관 동의/signup은 미실행. git diff --check 수행. 코드 변경 없어 자동 테스트 재실행 없음.
- 유지한 계약: 기존 계정·Google/전화·회원 기록 삭제 없음, 추가 provider 변경 없음. 토큰/사용자 식별값/비밀정보 비기록. 신규 Identity 회원 생성까지 수행하지 않음.
- 결정사항: Apple 로그인 전체 장애가 아니라 기존 Firebase 계정에 자동 연결된 SNS와 Identity 승인 정보가 불일치할 때의 문제로 범위 좁힘. 기존 로그인 수단 유지=false는 다른 계정 간 비교로 해석하며 기존 계정 수단 삭제로 해석하지 않음.
- 위험 요소: 신규 MEMBER 가입과 이후 API 검증은 미수행. 테스트 enrollment는 만료될 수 있음. 이전 기존 계정의 Apple 해제 상태 유지. 예상 밖 수정 없음.
- 다음 작업: 요청 시 기존 UID 자동 연결 대응 설계 또는 신규 Apple 회원가입 검증 진행. Jira 변경 없음.

## 2026-09-30 — 다른 Apple 계정 검증 작업 식별 기록 보완

<!-- codex-turn:01a0f14f-ab03-7770-88dd-7c286941d9fe -->

- 브랜치: develop.
- 작업 목표: 이번 다른 Apple 계정 검증의 정확한 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 다른 Firebase UID의 Apple 인증 및 Identity 신규 가입 준비 정상 응답 결과의 식별 기록 추가.
- 테스트와 결과: 앞선 실제 화면 검증 결과 유지, git diff --check 수행. 문서 보완만으로 실행 테스트 재실행 없음.
- 유지한 계약: 추가 계정·DB·코드·설정 변경 없음, 비밀정보 비기록.
- 결정사항: 409 미재발과 신규 가입 준비 성공을 최종 회원가입 완료와 구분.
- 위험 요소: 전화번호 연결·MEMBER 가입·후속 API 미검증. 예상 밖 수정 없음.
- 다음 작업: 요청 시 기존 UID 자동 연결 대응 설계 또는 신규 가입 검증 진행. Jira 변경 없음.

## 2026-09-30 — Apple 신규 회원 후속 검증 순서 안내

<!-- codex-turn:01a0f152-ed12-7800-936d-e5e4dcd2227e -->

- 브랜치: develop.
- 작업 목표: 현재 확인 범위와 다음에 필요한 기능 검증의 우선순위 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 신규 enrollment 성공과 MEMBER 가입 완료를 구분하고 전화 연결/동의/가입, 프로필, Apple 재로그인, 토큰 재발급, LC 접근 및 챌린지 순서 제안. 병합은 기본 인증 이후 별도 테스트로 분리.
- 테스트와 결과: 앞선 실검증 결과 기반 안내, git diff --check 수행. 신규 외부 테스트 미실행.
- 유지한 계약: 약관 동의는 사용자 직접 수행, 기존 계정 전화번호 재사용 금지 안내. 실제 계정·데이터·설정 변경 없음, 비밀정보 비기록.
- 결정사항: 정상 신규 계정 경로 검증은 이어갈 수 있으나 기존 UID 자동 연결 409를 해결 완료로 취급하지 않음.
- 위험 요소: 신규 Apple MEMBER 발급/재로그인/재발급 미검증. 자동 연결 문제는 출시 전 수정·회귀 검증 필요. 예상 밖 수정 없음.
- 다음 작업: 사용자 진행 요청 후 미사용 테스트 전화번호 및 약관 동의 준비, 가입 검증. Jira 변경 없음.

## 2026-09-30 — 신규 Apple 가입용 테스트 전화번호 가용성 확인

- 브랜치: develop.
- 작업 목표: 요청된 신규 Apple 가입 검증을 위한 미사용 테스트 전화번호 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Chrome Apple 신규 enrollment 준비 상태 확인 및 Firebase 전화 provider의 테스트 번호 설정 펼쳐 조회. 등록 번호는 1개이며 앞서 확인된 기존 Google 회원의 연결 번호로 별도 번호 필요.
- 테스트와 결과: 실제 설정 화면 읽기 전용 확인, git diff --check 수행. 새로운 전화 인증/가입 및 실행 테스트 미수행.
- 유지한 계약: 기존 Google 전화 연결·회원 기록 보존, provider 설정 및 테스트 인증 코드 변경 없음. 인증 코드/개인정보 비기록.
- 결정사항: 현재 번호를 새 Apple 계정에 재사용하지 않음. 별도 가상 번호 및 고정 코드는 사용자가 콘솔에서 직접 등록하도록 인계, 이후 전화 인증 이어서 진행. 약관 동의는 별도 사용자 확인 필요.
- 위험 요소: 추가 테스트 번호 미등록으로 전화 연결 검증 대기, enrollment 만료 시 가입 준비 재요청 필요. 예상 밖 수정 없음.
- 다음 작업: 사용자 별도 가상 번호 등록 후 신규 Apple 전화 연결 검증. Jira 변경 없음.

## 2026-09-30 — 신규 Apple 전화 인증 준비 작업 식별 기록 보완

<!-- codex-turn:01a0f153-b853-7d73-addf-8c5d552adbf6 -->

- 브랜치: develop.
- 작업 목표: 이번 전화 인증 준비 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 테스트 번호 가용성 확인 및 별도 번호 등록 대기 상태의 식별 기록 추가.
- 테스트와 결과: git diff --check 수행. 문서 보완만으로 실행 테스트 미실행.
- 유지한 계약: 기존 회원 전화 연결 유지, 외부 설정·계정 변경 없음, 비밀정보 비기록.
- 결정사항: 사용 중인 테스트 번호를 새 계정에 재사용하지 않음.
- 위험 요소: 별도 테스트 번호 등록 및 실제 전화 인증 미완료. 예상 밖 수정 없음.
- 다음 작업: 사용자 번호 등록 후 신규 Apple 전화 연결 검증. Jira 변경 없음.

## 2026-09-30 — 신규 Apple 계정 테스트 전화 연결 성공

<!-- codex-turn:01a0f157-dcb6-77f3-92fa-aee455c04396 -->

- 브랜치: develop.
- 작업 목표: 사용자 등록한 별도 가상 번호로 신규 Apple 계정 전화 연결 검증.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Firebase 전화 provider 설정에 테스트 번호 두 개 및 저장 비활성 확인. 기존 번호와 다른 신규 가상 번호를 사용. Chrome Apple 세션에서 exchange 가입 준비 갱신 후 전화 인증 시작 및 고정 코드 연결 수행. SDK 성공 화면에서 Firebase UID 유지와 전화 연결 완료 확인.
- 테스트와 결과: 실제 테스트 Firebase 전화 연결 성공. 최종 signup/프로필/토큰 재발급/LC는 미실행. git diff --check 수행, 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 실제 SMS 미발송, 기존 Google 계정의 전화 연결 유지. 인증 코드·토큰·개인 식별값 비기록. 약관 동의 및 최종 가입 대리 실행 없음. 서버 설정·배포·DB 직접 변경 없음.
- 결정사항: 등록 완료 보고를 전화 연결 검증 진행으로 해석. 가입 전 사용자 닉네임·정책 확인·동의 필요. 기존 draft 정책 버전을 현재 live 값으로 단정하지 않음.
- 위험 요소: 신규 Identity MEMBER는 아직 생성되지 않았으며 enrollment 만료 가능. 기존 자동 연결 409 문제는 미해결. 예상 밖 수정 없음.
- 다음 작업: 현재 정책 버전 확인 및 사용자 직접 약관 동의 후 최종 가입, 프로필/재로그인/재발급 검증. Jira 변경 없음.

## 2026-09-30 — 배포된 정책 버전 확인 및 테스트 화면 입력

<!-- codex-turn:01a0f15a-e8a5-77e3-8b23-e88ece58b884 -->

- 브랜치: develop.
- 작업 목표: 사용자 요청대로 현재 서버 정책 버전만 입력하고 동의·가입은 사용자에게 유지.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. AWS 테스트 서비스 새로고침 후 연결된 task revision 6에서 개인정보 privacy-v1, 이용약관 term-v1 확인. Chrome 테스트 화면 두 버전 필드 입력 및 표시 확인.
- 테스트와 결과: 브라우저 실제 설정 및 입력값 검증 완료. git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, 동의 체크·최종 가입·운영 설정 변경 없음.
- 결정사항: 조회 당시 동의 체크가 이미 선택되어 있었으므로 그대로 보존. 대리 동의 또는 signup 미실행.
- 위험 요소: 신규 MEMBER 가입 및 후속 검증 미완료, enrollment 만료 가능. 기존 사용자 변경 보존, 이번 작업 예상 밖 변경 없음.
- 다음 작업: 사용자 약관 확인 후 최종 가입 및 프로필·재로그인·재발급 검증. Jira 변경 없음.

## 2026-09-30 — 신규 Apple 가입 RESTART_EXCHANGE 진단

- 브랜치: develop.
- 작업 목표: 사용자 로그인 실패 보고 원인 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Chrome signup RESTART_EXCHANGE 및 프로필 버튼 비활성 확인. 로컬 app.js의 signup 사전 검사와 서버 enrollment 기본 10분·응답 밀리초 단위 확인.
- 테스트와 결과: UI 및 소스 읽기 검증. git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 토큰·개인정보 비기록, 약관 동의/가입/서버 변경 미실행.
- 결정사항: Firebase 로그인 실패가 아니라 가입 준비 재시작 요구로 안내. 경과 시간상 enrollment 만료 유력하나 내부 메모리 직접 조회 없이 다른 사전 조건 실패 가능성과 구분.
- 위험 요소: 신규 MEMBER 미생성, 재인증 필요 가능. 기존 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 사용자 exchange 갱신 및 필요 시 Apple 재인증 후 최종 가입. Jira 변경 없음.

## 2026-09-30 — 가입 오류 진단 작업 식별 기록

<!-- codex-turn:01a0f15f-3567-7f61-83c8-5570e01ed056 -->

- 브랜치: develop.
- 작업 목표: 이번 RESTART_EXCHANGE 진단의 turn 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome 오류 및 로컬 가입 사전 검사 확인 결과를 기록. 코드 수정 없음.
- 테스트와 결과: git diff --check 통과. 읽기 전용 진단으로 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, 동의·가입·서버 변경 없음.
- 결정사항: 가입 준비 갱신 안내, enrollment 만료는 유력 원인으로 구분.
- 위험 요소: 신규 가입 및 후속 인증 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 가입 준비 갱신 후 최종 가입 결과 확인. Jira 변경 없음.

## 2026-09-30 — 전화 연결 후 exchange 제공자 거절 진단

- 브랜치: develop.
- 작업 목표: HTTP 403 FIREBASE_PROVIDER_NOT_ALLOWED 원인 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome 검증표에서 exchange 실패 확인. FirebaseAdminAuthenticationVerifier의 LOGIN_EXCHANGE PHONE 거절 조건 확인. 직전 task revision 6의 Apple/Phone 활성 확인과 비교.
- 테스트와 결과: UI 및 소스 읽기 검증, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: PHONE 단독 로그인 제한 유지, 비밀정보 비기록, 계정/동의/배포 변경 없음.
- 결정사항: 전화 연결 후 sign-in provider 변경 가능성이 유력하나 현재 claim 미조회로 확정하지 않음. 같은 Apple 재인증 후 exchange 및 가입 안내.
- 위험 요소: 실제 claim 및 재인증 후 성공 미확인. 기존 변경 보존, 예상 밖 수정 없음.
- 다음 작업: 사용자 Apple 재인증 후 가입 결과 확인. Jira 변경 없음.

## 2026-09-30 — 제공자 거절 진단 식별 기록 보완

<!-- codex-turn:01a0f161-0128-7b92-b88f-57124fe10a98 -->

- 브랜치: develop.
- 작업 목표: 현재 turn의 exchange 403 진단 기록 연결.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: UI exchange 실패 및 서버 PHONE 로그인 제한 확인 결과 기록. 코드 변경 없음.
- 테스트와 결과: git diff --check 통과. 읽기 전용 진단으로 Gradle 미실행.
- 유지한 계약: 제공자 제한 유지, 동의·가입·설정 변경 없음, 비밀정보 비기록.
- 결정사항: PHONE 전환은 미확정 가설이며 같은 Apple 재인증 후 재시도 안내.
- 위험 요소: 재인증 후 성공 및 현재 claim 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 Apple 재인증 및 가입 결과 확인. Jira 변경 없음.

## 2026-09-30 — Apple 가입 후 기능 및 병합 읽기 검증

- 브랜치: develop.
- 작업 목표: 사용자 수행 후 성공 범위와 미검증 범위 구분.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Chrome 검증표에서 Apple 재인증·회원가입·프로필·재발급·LC 인증·Guest 준비·Identity 병합·응시·S3 업로드 성공 확인. 병합 후 검증 버튼의 읽기 전용 구현을 확인하고 실행.
- 테스트와 결과: Identity/LC 이전 Guest 모두 ACCOUNT_MERGED_TOKEN_REJECTED 확인, 대상 MEMBER 완료 이력 조회 성공. 이전 기록 0건으로 실제 이전 미검증. 챌린지 결과 solvedQuestionCount=0 및 question=null, 제출/AI 채점 성공 미확인. git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀정보·개인정보 비기록, 추가 가입·병합·제출·재발급 또는 서버 설정 변경 없음.
- 결정사항: 과거 signup/exchange 오류 기록과 이후 성공 기록 구분. 기본 인증 및 병합 차단은 성공, 전체 기능 완료로 단정하지 않음.
- 위험 요소: AI 채점, 기록 있는 Guest 이전, 이벤트 204/중복 처리는 미검증. 기존 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 챌린지 제출/채점 및 기록 있는 Guest 이전은 별도 범위로 검증. Jira 변경 없음.

## 2026-09-30 — 후속 기능 검증 turn 기록 보완

<!-- codex-turn:01a0f163-6859-73f0-8185-9d56dd1949c7 -->

- 브랜치: develop.
- 작업 목표: 이번 Apple 가입 후 기능 확인의 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 기본 인증·가입·재발급·LC 접근 성공 표시 확인 및 병합 후 읽기 검증 결과 기록. 코드 변경 없음.
- 테스트와 결과: 이전 Guest의 Identity/LC 전용 차단 및 대상 MEMBER 이력 조회 성공. git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 추가 가입·병합·제출·서버 변경 없음, 비밀정보 비기록.
- 결정사항: 기본 인증 성공과 전체 종단 검증 완료를 구분.
- 위험 요소: 기록 0건으로 실제 이전 미검증, AI 채점 성공 미확인. 예상 밖 변경 없음.
- 다음 작업: 챌린지 제출·채점 및 기록 있는 Guest 이전 별도 검증. Jira 변경 없음.

## 2026-09-30 — 이전 Google 채점 성공 기록 확인

- 브랜치: develop.
- 작업 목표: 이전 Google 검증 성공 여부와 최근 Apple 검증 범위 구분.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 WORKLOG에서 CORS 수정 후 실제 업로드·제출·AI completed 및 결과 재조회 성공 확인. 이번 Apple 계정 미확인과 구분하여 설명.
- 테스트와 결과: 과거 기록 읽기 확인, git diff --check 수행. 코드 변경 및 신규 서버 호출 없어 실행 테스트 미실행.
- 유지한 계약: 비밀정보 비기록, 외부 상태 변경 없음.
- 결정사항: AI 전체가 미검증인 것이 아니라 새 Apple 계정 시도만 미확인으로 정정. Google 당시 기록 이전도 0건으로 미검증.
- 위험 요소: Apple 현재 채점 성공 및 기록 이전은 여전히 미확인. 예상 밖 변경 없음.
- 다음 작업: 필요 시 Apple 계정 제출·결과 확인. Jira 변경 없음.

## 2026-09-30 — Google 검증 범위 확인 turn 기록 보완

<!-- codex-turn:01a0f166-2c49-7ef0-a54d-ae1cca42c1bc -->

- 브랜치: develop.
- 작업 목표: 이전 Google 채점 성공 확인과 이번 Apple 시도의 미확인 범위 구분.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 검증 기록의 실제 AI completed 및 결과 재조회 성공 확인. 코드 변경 없음.
- 테스트와 결과: 기록 읽기 확인 및 git diff --check 통과. 코드 변경 없어 실행 테스트 미실행.
- 유지한 계약: 외부 상태 변경 없음, 비밀정보 비기록.
- 결정사항: 채점 기능 전체 미검증이 아닌 Apple 신규 시도 미확인으로 설명 정정.
- 위험 요소: 실제 기록 이전은 당시에도 기록 0건으로 미검증. 예상 밖 변경 없음.
- 다음 작업: 필요 시 Apple 계정 제출·채점 결과 확인. Jira 변경 없음.

## 2026-09-30 — 기록 있는 Guest 병합 재검증 범위 확인

- 브랜치: develop.
- 작업 목표: 기존 Guest 정리 및 임시 모의고사 1건을 이용한 병합 재검증 준비.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: UserMerged 인계 계약에서 source MERGED tombstone 유지 확인. 기존 병합 계정 삭제 대신 신규 Guest 사용 제안, LC 저장소/fixture 경로 확인 요청.
- 테스트와 결과: 계약·이전 검증 기록 읽기 및 git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: Identity에 모의고사 코드 추가 금지, 비밀정보 비기록, 외부 삭제·생성·병합 미실행.
- 결정사항: 삭제 범위 불명확한 기존 병합 기록 보존. LC 스키마 확인 없이 임의 문서 삽입하지 않음.
- 위험 요소: LC 임시 완료 데이터 생성 방법 미확인, 신규 Guest 동의 및 실제 병합은 실행 전 별도 확인 필요.
- 다음 작업: LC 저장소 위치 확인 후 테스트 데이터 준비 및 신규 Guest 병합 검증. Jira 변경 없음.

## 2026-09-30 — 기록 있는 Guest 재검증 준비 turn 기록

<!-- codex-turn:01a0f167-97be-7490-8a52-f32ed56a3dce -->

- 브랜치: develop.
- 작업 목표: 새 Guest와 완료 모의고사 1건을 이용한 병합 검증 준비 범위 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 기존 MERGED Guest 보존 및 새 Guest 사용 제안. LC 스키마 확인을 위한 저장소 위치 요청. 코드 변경 없음.
- 테스트와 결과: 계약·기록 조회 및 git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: Identity 도메인 경계 유지, 비밀정보 비기록, 외부 삭제·생성·병합 미실행.
- 결정사항: 기존 병합 기록은 삭제하지 않고 데이터 생성 방법 확인 후 진행.
- 위험 요소: LC fixture 방법 미확인, 신규 Guest 동의·실제 병합 실행 전 확인 필요. 예상 밖 변경 없음.
- 다음 작업: LC 저장소 위치를 받아 테스트 데이터 준비 방법 확인. Jira 변경 없음.

## 2026-09-30 — 새 Guest 생성 버튼 비활성 진단

- 브랜치: develop.
- 작업 목표: 새 Guest 생성 불가 원인 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome 생성 버튼 disabled 및 기존 병합 검증 버튼 활성 확인. 독립 도구 app.js controls가 oldGuest/mergeUncertain을 포함해 생성 차단하며 병합 후 검증용 상태를 보존함을 확인. clear는 전체 로컬 인증·녹음/응시 상태를 초기화하므로 미실행.
- 테스트와 결과: UI·소스 읽기 검증 및 git diff --check 수행. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 외부 계정 삭제·생성·병합 없음, 비밀정보 비기록.
- 결정사항: 서버 오류와 로컬 도구 재검증 잠금을 구분. 초기화 영향 안내 후 사용자 선택 필요.
- 위험 요소: 전체 초기화 시 현재 메모리 로그인·녹음/진행 상태 손실. 기존 서버 계정·기록은 삭제되지 않음. 예상 밖 변경 없음.
- 다음 작업: 전체 로컬 초기화 후 재로그인 또는 Guest 전용 초기화 기능 개선 요청에 따라 진행. Jira 변경 없음.

## 2026-09-30 — Guest 생성 잠금 진단 turn 기록 보완

<!-- codex-turn:01a0f16a-4741-70b0-9a6e-579e498bae50 -->

- 브랜치: develop.
- 작업 목표: 새 Guest 생성 버튼 비활성 진단의 현재 turn 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: UI disabled 및 기존 병합 상태 보존에 따른 도구 잠금 확인. 코드 수정·로컬 초기화 미실행.
- 테스트와 결과: UI/소스 확인 및 git diff --check 통과. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 서버 계정 삭제·생성·병합 없음, 비밀정보 비기록.
- 결정사항: 전체 초기화의 로그인·녹음 손실을 안내하고 MEMBER 유지형 Guest 전용 초기화 개선 여부 질문.
- 위험 요소: 기존 로컬 상태 유지 중이며 반복 생성은 여전히 잠김. 예상 밖 변경 없음.
- 다음 작업: 사용자 개선 승인 또는 전체 초기화 선택 대기. Jira 변경 없음.

## 2026-09-30 — Apple 자체 로그인 폼 오류 확인

- 브랜치: develop.
- 작업 목표: 사용자 Apple 로그인 일반 오류의 발생 단계 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome Apple 인증 팝업의 계정 로그인 폼에서 문제가 발생했으니 다시 시도하라는 문구 확인. localhost의 Apple/exchange 버튼은 팝업 대기로 비활성 확인. 코드 변경 없음.
- 테스트와 결과: 브라우저 읽기 검증, git diff --check 수행. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 비밀번호·인증 코드·OAuth state 비기록, 로그인 대행·초기화·외부 설정 변경 없음.
- 결정사항: Identity 403/409와 구분하여 Apple 계정 인증 단계 문제로 안내. 상세 원인 및 일시 제한 여부는 미확정.
- 위험 요소: 일반 오류 문구만으로 계정/세션/Apple 서비스 원인을 특정할 수 없음. 예상 밖 변경 없음.
- 다음 작업: 사용자 팝업 재시작 또는 패스키 인증 후 결과 확인. Jira 변경 없음.

## 2026-09-30 — Apple 로그인 팝업 진단 turn 기록 보완

<!-- codex-turn:01a0f16c-c46e-76a2-ad7a-c1a57e771cc4 -->

- 브랜치: develop.
- 작업 목표: Apple 자체 로그인 오류 진단의 현재 turn 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Apple 계정 로그인 폼의 일반 오류와 테스트 화면의 팝업 대기 상태 확인. 코드 수정 없음.
- 테스트와 결과: UI 읽기 검증 및 git diff --check 통과. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 비밀정보 비기록, 인증정보 입력·초기화·계정 삭제·설정 변경 없음.
- 결정사항: Identity 오류와 구분하고 사용자 팝업 재시작 또는 패스키 인증 안내.
- 위험 요소: 상세 원인 미확정, 재시도 성공 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 재인증 결과 확인. Jira 변경 없음.

## 2026-09-30 — 테스트 화면 정책 버전 재입력

- 브랜치: develop.
- 작업 목표: 정책 버전 재입력 및 사용자에게 값 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 이전 AWS test revision 6 확인값 privacy-v1, term-v1을 Chrome 두 입력란에 재입력. 코드 변경 없음.
- 테스트와 결과: 입력값 UI 표시 확인 및 git diff --check 수행. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 동의 체크·가입 버튼 미조작, 비밀정보 비기록.
- 결정사항: 이번 AWS 재조회 없이 앞서 검증한 버전 사용.
- 위험 요소: 이후 배포 정책 변경 여부는 이번에 재확인하지 않음. 예상 밖 변경 없음.
- 다음 작업: 사용자 정책 확인·동의 후 테스트 진행. Jira 변경 없음.

## 2026-09-30 — 새 Guest 더미 모의고사 삽입 사전 조사

- 브랜치: develop.
- 작업 목표: 새 Guest 소유 완료 모의고사 1건 준비.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 로컬 Learning Core 저장소 발견 및 규칙/ExamReadService/ExamSession/ExamSummary/조회 조건 확인. Identity에 LC 코드 추가 없음.
- 테스트와 결과: 스키마·로컬 프록시 읽기 확인, git diff --check 수행. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 운영·기존 계정·병합 marker 변경 없음, 토큰·개인정보 비기록.
- 결정사항: 방금 생성된 Guest UUID가 UI에 없어 소유자 확정 전 삽입 중단. 최신 생성 계정 추정은 사용하지 않음.
- 위험 요소: 정확한 Guest 소유자 및 테스트 DB 삽입 경로 검증 필요. 더미 생성 미완료, 예상 밖 변경 없음.
- 다음 작업: Guest UUID 확인 후 테스트 DB에 한정한 fixture 생성·이력 조회 검증. Jira 변경 없음.

## 2026-09-30 — Guest 더미 데이터 준비 turn 기록 보완

<!-- codex-turn:01a0f16f-fcea-7f02-b7b6-8781010b68a7 -->

- 브랜치: develop.
- 작업 목표: 새 Guest 더미 모의고사 삽입 요청의 현재 turn 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Learning Core 로컬 소스에서 완료 이력 조회 및 세션·요약 스키마 확인. 정확한 Guest 소유자 확인 전 데이터 삽입 보류. 코드 변경 없음.
- 테스트와 결과: 소스 읽기 확인 및 git diff --check 통과. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 비밀정보 비기록, 운영·기존 계정·병합 marker 변경 없음.
- 결정사항: 최신 계정 추정 없이 Guest UUID 확인 필요. 토큰 원문 요청하지 않음.
- 위험 요소: 더미 삽입 및 이력 조회 검증 미완료. 예상 밖 변경 없음.
- 다음 작업: 정확한 Guest UUID 확인 후 테스트 DB 대상 검증 및 fixture 삽입. Jira 변경 없음.

## 2026-09-30 — 정책 버전 재입력 turn 기록 보완

<!-- codex-turn:01a0f16e-72f5-7373-9b33-e76496c0159c -->

- 브랜치: develop.
- 작업 목표: 현재 turn의 정책 버전 재입력 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 이전 테스트 배포 확인값 privacy-v1 및 term-v1을 Chrome 입력란에 채우고 사용자에게 안내. 코드 변경 없음.
- 테스트와 결과: UI 입력값 확인 및 git diff --check 통과. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 동의·가입 대리 실행 없음, 비밀정보 비기록.
- 결정사항: 앞서 확인한 정책 버전 사용, 이번 AWS 재조회 없음.
- 위험 요소: 이후 서버 정책 변경 여부 미재확인. 예상 밖 변경 없음.
- 다음 작업: 사용자 정책 확인·동의 후 테스트 진행. Jira 변경 없음.

## 2026-09-30 — Guest 더미 데이터 준비 최종 상태

<!-- codex-turn:01a0f16f-fcea-7f02-b7b6-8781010b68a7 -->

- 브랜치: develop.
- 작업 목표: Guest 더미 삽입 준비 결과를 WORKLOG 끝에 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: LC 완료 이력 스키마 확인, Guest UUID 확인 대기. 코드 변경 없음.
- 테스트와 결과: 소스 읽기 확인, git diff --check 통과. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 외부 데이터 변경 없음, 비밀정보 비기록.
- 결정사항: 소유자 추정 삽입 금지, 확인 후 테스트 DB만 사용.
- 위험 요소: fixture 삽입 미완료. 예상 밖 코드 변경 없음.
- 다음 작업: Guest UUID 확인 및 테스트 DB 검증 후 삽입. Jira 변경 없음.

## 2026-09-30 — 최신 테스트 Guest의 더미 완료 모의고사 추가

- 브랜치: develop.
- 작업 목표: 사용자 승인한 최신 Guest에 병합 검증용 완료 모의고사 1건 삽입.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 코드 변경 없음.
- 구현 내용: 테스트 Identity users에서 GUEST 최신순 3개를 최소 필드 조회하여 최신 ACTIVE/이전 2개 MERGED 확인. 테스트 LC mock_exams에 active=false·빈 questions·TEST ONLY 제목의 참조 카탈로그 1건, exam_summaries에 합성 점수120/IM2·더미 안내 1건, exam_sessions에 해당 Guest 소유 COMPLETED/active=false·날짜 포함 1건 삽입. 카탈로그 서비스의 비활성 제외와 완료 이력 조회 조건을 소스로 확인.
- 테스트와 결과: Atlas 저장 결과 확인, Chrome 병합 전 완료 시험 기록 조회 실행으로 현재 Guest에서 1건 조회 및 비교 준비 확인. git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 운영 DB·기존 계정·병합 marker/guard 수정 및 실제 AI 호출 없음. Identity에 LC 코드 추가 없음. 사용자 UUID·토큰 비기록, 더미는 실제 채점 결과와 구분.
- 결정사항: 기존 계정 삭제 없이 새 Guest에 합성 완료 이력과 요약만 추가. 비활성 카탈로그라 실제 응시 배정 제외. fixture 참조명 merge-fixture-20260930-173033으로 식별 가능.
- 위험 요소: 문항별 결과·AI 채점 검증용 완전한 시험 아님. 실제 병합 및 소유권 이전은 아직 미실행. 여러 컬렉션 UI 순차 삽입 후 현재 Guest 조회로 확인, 잔존 더미는 테스트 종료 후 정리 대상. 예상 밖 코드 변경 없음.
- 다음 작업: 사용자 재인증/병합 승인 후 MEMBER에서 동일 시험 이전 및 Guest 차단 검증. Jira 변경 없음.

## 2026-09-30 — Guest 더미 삽입 완료 turn 기록

<!-- codex-turn:01a0f172-7184-7dd2-9075-746ef07a8df3 -->

- 브랜치: develop.
- 작업 목표: 최신 테스트 Guest의 완료 모의고사 더미 생성 및 조회 검증 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 코드 변경 없음.
- 구현 내용: 승인된 최신 생성 기준으로 ACTIVE Guest 확인 후 테스트 LC에 비활성 카탈로그·완료 세션·합성 요약 각 1건 삽입.
- 테스트와 결과: Atlas 저장 및 현재 Chrome Guest의 완료 이력 1건 조회 확인. git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 운영·기존 계정·병합 marker 변경 없음, 비밀정보 비기록, 실제 AI 채점과 더미 구분.
- 결정사항: 병합 비교용 이력 준비 완료, 실제 병합 미실행.
- 위험 요소: 문항별 결과 없는 합성 데이터이며 실제 소유권 이전은 아직 미검증. 예상 밖 코드 변경 없음.
- 다음 작업: 사용자 병합 실행 후 MEMBER 기록 이전 및 Guest 차단 확인. Jira 변경 없음.

## 2026-09-30 — 더미 완료 시험 1건 병합 후 검증 성공

- 브랜치: develop.
- 작업 목표: 사용자 실행한 Guest 병합의 실제 기록 이전 및 source 차단 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome 기존 성공 표시에 이어 병합 후 읽기 검증 직접 재실행. 코드 변경 없음.
- 테스트와 결과: Identity/LC 이전 Guest의 병합 전용 거절 코드 확인, target MEMBER 이력 조회 성공, 이전 시험 ID 1건 중 1건 포함 확인. git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 추가 병합·삽입·삭제 없음, 토큰·개인 식별값 비기록, 합성 데이터와 실제 채점 구분.
- 결정사항: 이번에는 기록 있는 Guest의 완료 이력 이전 검증 성공으로 보고. 기존 0건 테스트의 미검증 상태와 구분.
- 위험 요소: 요약/문항별 결과 소유권 DB 재조회 및 이벤트204·중복·장애 재시도는 이번 작업에서 미검증. 예상 밖 변경 없음.
- 다음 작업: 필요 시 요약/상세 결과 및 중복·장애 시나리오 추가 검증. Jira 변경 없음.

## 2026-09-30 — 기록 있는 Guest 병합 검증 turn 기록

<!-- codex-turn:01a0f17a-1321-7121-9953-f75a4c519fa7 -->

- 브랜치: develop.
- 작업 목표: 사용자 병합 후 기록 이전 및 Guest 차단 검증 결과 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome 병합 후 읽기 검증 재실행. 코드 변경 없음.
- 테스트와 결과: Identity/LC 이전 Guest 병합 전용 거절 및 MEMBER 완료 이력 조회 성공, 이전 시험 1건 중 1건 포함 확인. git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 추가 병합·데이터 변경 없음, 비밀정보·개인 식별값 비기록.
- 결정사항: 합성 완료 이력 1건의 실제 이전 조회 검증 성공으로 확정.
- 위험 요소: 상세 결과 소유권 및 이벤트 중복·장애 재시도 별도 미검증. 예상 밖 변경 없음.
- 다음 작업: 필요 시 상세·중복·장애 시나리오 추가 검증. Jira 변경 없음.

## 2026-09-30 — 동일 UID 원격 선연결 복구 수정 방향 검토

- 브랜치: develop.
- 작업 목표: Google/Apple 동일 이메일 로그인 오류의 후속 수정 범위 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: ProviderChangeGuard가 기존 Firebase binding의 현재 provider SocialIdentity 미등록 시 거절하고 ProviderLinkService.prepare도 Firebase 선연결을 사후 승인하지 않는 구현 재확인. 코드 변경 없음.
- 테스트와 결과: 소스 재조회 및 git diff --check 수행. 코드 미변경으로 Gradle 미실행.
- 유지한 계약: 이메일만으로 계정 연결/통합하지 않음, 다른 UID 및 타계정 provider 충돌·차단 유지. 기존 사용자 dirty 변경 보존.
- 결정사항: 기존 제공자 재인증·사용자 명시 동의 기반 선연결 복구 경로를 권장하며 상세 계약·구현은 아직 미확정.
- 위험 요소: guard 단순 제거 또는 SocialIdentity 자동 동기화는 기존 해제/차단 정책 우회 가능. 현재 정상 흐름 검증이 모든 출시 준비 완료를 의미하지 않음. 예상 밖 변경 없음.
- 다음 작업: 복구 정책 확정 후 서버·프론트 계약과 보안 회귀 테스트 설계/구현. Jira 변경 없음.

## 2026-09-30 — Apple/Google 연결 복구 검토 turn 기록

<!-- codex-turn:01a0f17b-6860-7ae0-97d1-3a57b7e70929 -->

- 브랜치: develop.
- 작업 목표: 동일 UID의 Firebase 선연결과 Identity 미등록 상태에 대한 수정 방향 정리.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 현재 로그인 guard 및 연결 prepare의 거절 조건 확인. 기존 제공자 재인증·명시 동의 기반 복구 경로 제안. 코드 변경 없음.
- 테스트와 결과: 소스 읽기 확인 및 git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 이메일만으로 자동 연결하지 않음, provider 차단·소유권 검사 유지, 비밀정보 비기록.
- 결정사항: guard 제거가 아닌 복구 절차 보완 권장. 상세 정책·구현 미확정.
- 위험 요소: 기존 해제/차단 우회 방지 회귀 필요. 예상 밖 변경 없음.
- 다음 작업: 정책 확정 후 서버·프론트 계약과 테스트 설계. Jira 변경 없음.

## 2026-09-30 — EMAIL_VERIFICATION 적용 조건 설명

<!-- codex-turn:01a0f276-318f-73c2-b4c5-d548d50ce4ad -->

- 브랜치: develop.
- 작업 목표: missingRequirements의 EMAIL_VERIFICATION 의미와 SNS 로그인 적용 여부 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: FirebaseEnrollmentRequirementResolver 및 FirebaseAdminAuthenticationVerifier에서 PASSWORD 연결 여부와 emailVerified 조건 확인. 애플리케이션 코드 변경 없음.
- 실행한 테스트와 결과: 소스와 프론트 계약 확인, git diff --check 수행. 설명만 수행하여 Gradle 테스트 미실행.
- 유지한 계약: 기존 enum 및 가입/승격 검증 조건 유지. 비밀정보 비기록, 기존 dirty 변경 보존.
- 결정사항: Firebase 이메일/비밀번호 계정의 이메일 소유 확인으로 설명. 순수 SNS에는 해당 조건이 없으며 PASSWORD가 함께 연결된 미인증 계정은 예외임을 안내.
- 위험 요소: 현재 로그인 제공자만 보고 인증 필요 여부를 판단하면 연결된 PASSWORD 방식 조건을 놓칠 수 있음. 이번 작업의 예상 밖 변경 없음.
- 다음 작업: 프론트는 배열 순서 대신 각 requirement 포함 여부로 필요한 절차를 안내. Jira 작업 없음.

## 2026-09-30 — 가입 전 공개 정책 버전 조회 필요성 확인

<!-- codex-turn:01a0f27a-43d9-7210-8c7d-deeadae61e61 -->

- 브랜치: develop.
- 작업 목표: 회원가입 동의 전 인증 없이 현재 정책 버전을 조회할 수 있는지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: UserController, UserConsentService, SecurityConfig 및 Firebase 응답 확인. 현재 내 동의 조회는 JWT와 현재 사용자 조회에 의존하며 공개 정책 조회 경로는 없음. 공개 GET /api/v1/policies/consents는 제안이며 미구현.
- 실행한 테스트와 결과: 코드 읽기로 인증 경계 및 ConsentPolicy 버전 검증 확인, git diff --check 수행. 분석 작업으로 Gradle 테스트 미실행.
- 유지한 계약: 개인 동의 상태 API 인증 유지, 가입 제출 시 서버의 현재 버전 검증 유지, 기존 dirty 변경 보존.
- 결정사항: 기존 privacyConsentVersion, termConsentVersion, qualityReviewConsentVersion 이름으로 공개 정책 버전을 제공하는 방향 권장. 공통 ConsentPolicy를 사용하여 검증과 조회 값 일치 필요.
- 위험 요소: 조회와 가입 사이 정책이 바뀔 수 있어 재조회·정책 내용 재표시·재동의 필요. 버전만 최신으로 자동 교체하면 사용자가 실제 본 내용과 동의 기록 불일치 가능. 정책 본문 제공 경로는 추가 확인 필요.
- 다음 작업: 공개 정책 조회 API와 프론트 재동의 흐름 구현 시 계약 및 인증 회귀 테스트 추가. 이번 작업의 예상 밖 변경 없음, Jira 변경 없음.

## 2026-09-30 — missingRequirements와 signup 입력 생략 가능 여부 검토

<!-- codex-turn:01a0f27e-be2c-7083-b7aa-6775133ef792 -->

- 브랜치: develop.
- 작업 목표: 프론트의 단계별 저장 가정과 signup 필수 필드 계약을 비교하고 수정 필요성 판단.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: requirement resolver, signup/upgrade DTO·서비스, enrollment entity, 응답과 프론트 계약 및 기존 테스트 확인. 일반 신규 가입은 PROFILE·CONSENTS 항상 포함, enrollment는 입력 초안을 보관하지 않음. Guest 승격에서 CONSENTS가 빠질 수 있는 예외 확인. 코드 수정 없음.
- 실행한 테스트와 결과: 기존 FirebaseExchangeServiceTests의 PROFILE·CONSENTS 포함 기대값 및 FirebaseGuestPrepareServiceTests의 PHONE_VERIFICATION·PROFILE만 포함하는 기대값 확인. 테스트 실행은 하지 않았으며 분석 작업으로 Gradle 미실행. git diff --check 수행.
- 유지한 계약: signup/upgrade 필수 입력 및 최종 정책 버전 검증 유지, enrollment 소유권·만료 경계 유지. 사용자 dirty 변경 보존.
- 결정사항: 신규 가입에는 입력 선택화나 enrollment 초안 조회 API가 불필요. missingRequirements는 요청 필드 생략 목록이 아니며 클라이언트 폼 입력을 최종 제출까지 보존. Guest 승격의 기존 동의는 인증된 내 동의 API로 확인 가능.
- 위험 요소: 신규 가입과 Guest 승격을 동일하게 해석하면 동의 누락이 발생할 수 있음. PHONE_VERIFICATION 외에 PASSWORD 계정의 EMAIL_VERIFICATION도 인증 상태에 따라 달라짐. 정책 변경 시 버전만 자동 교체하지 않고 필요한 재동의 수행. 실제 프론트 구현은 미검토.
- 다음 작업: 프론트에 신규 가입/Guest 승격 차이 전달, 공개 정책 조회 API는 별도 구현 범위로 유지. 예상 밖 변경 없음, Jira 작업 없음.

## 2026-09-30 — signup 입력과 Firebase 사전 인증 시점 설명

<!-- codex-turn:01a0f281-5b58-7311-bb96-2b359e06ba9b -->

- 브랜치: develop.
- 작업 목표: signup 전에 missingRequirements에서 인증 항목만 빠질 수 있다는 설명을 쉽게 풀어 전달.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 직전 확인한 resolver·signup·enrollment 구현을 근거로 프론트 폼 입력과 Firebase 인증 완료 상태의 저장 위치 및 전달 시점 차이를 설명. 코드 변경 없음.
- 실행한 테스트와 결과: 설명 작업으로 Gradle 미실행. 직전 코드 확인 결과를 재사용하고 git diff --check 수행.
- 유지한 계약: 신규 가입 PROFILE·CONSENTS 항상 요구, 최종 signup 필수 입력 유지. 비밀정보 비기록.
- 결정사항: 화면 입력만으로 서버 요구사항이 충족되지 않으며 signup에서 전달됨을 안내. Firebase 전화번호 인증은 signup 전에 완료 가능함을 예시로 설명.
- 위험 요소: PASSWORD 이메일 인증 및 Guest 기존 동의 예외를 구분해야 함. 기존 dirty 변경 보존, 이번 예상 밖 변경 없음.
- 다음 작업: 공개 정책 버전 조회 필요성은 별도 유지. Jira 변경 없음.

## 2026-09-30 — 신규 가입 폼 값 보관 책임 확인

<!-- codex-turn:01a0f285-a6a1-7032-beac-08ff3646d8ef -->

- 브랜치: develop.
- 작업 목표: 전화번호 사전 인증과 signup 필수 입력에 대한 사용자 이해 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 일반 신규 가입은 닉네임·동의·버전을 필수 제출하고 프론트에서 입력값을 보관해야 함을 설명. 코드 변경 없음.
- 실행한 테스트와 결과: 앞서 확인한 resolver와 DTO 근거 재사용. 설명 작업으로 Gradle 미실행, git diff --check 수행.
- 유지한 계약: signup 필수 입력 및 Firebase 인증 경계 유지, 비밀정보 비기록.
- 결정사항: 프론트가 자동으로 값을 보유하는 것이 아닌 화면 입력 후 제출/재시도까지 보관하는 책임으로 명확화.
- 위험 요소: 새로고침·앱 종료 등으로 폼 값이 사라지면 다시 입력받아야 함. Guest 승격의 기존 동의 예외와 구분. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 프론트의 폼 보관/재입력 흐름 적용. Jira 변경 없음.

## 2026-09-30 — signup 선택 동의와 공개 정책 조회 수정 방향 설명

- 브랜치: develop.
- 작업 목표: Firebase 신규 가입 선택 품질 검토 동의 및 가입 전 공개 정책 조회의 변경 계약 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 기존 UserConsentUpdateRequest, ConsentPolicy, UserFactory, UserConsents를 근거로 재사용 방향 정리. signup DTO·서비스·factory 확장과 공개 정책 조회 controller/DTO 및 GET 인증 허용 제안. 애플리케이션 미구현.
- 실행한 테스트와 결과: 기존 구현 소스 확인, git diff --check 수행. 구현 전 설명 단계로 Gradle 미실행.
- 유지한 계약: 기존 선택 동의 필드명, 필수 개인정보·약관 검증, 기존 클라이언트 누락 시 false, 개인 동의 API 인증 유지.
- 결정사항: true일 때 현재 선택 정책 버전 검증, false일 때 버전 생략 가능. 공개 조회는 사용자 상태 없이 동일 ConsentPolicy의 세 버전을 반환. 신규 엔티티·환경변수 추가 불필요.
- 위험 요소: 공개 조회 이후 정책 변경 시 실제 재동의 필요. Guest upgrade에 같은 입력을 추가하는 변경은 이번 신규 signup 설명 범위와 별도로 구분. 실제 정책 내용과 버전의 대응은 프론트에서도 관리 필요.
- 다음 작업: DTO·서비스·factory·공개 GET·계약 문서 수정 및 선택 동의 저장/버전 검증/익명 조회/기존 보호 경로 테스트 구현. 기존 dirty 변경 보존, 이번 예상 밖 변경 없음, Jira 작업 없음.

## 2026-09-30 — signup 선택 동의·공개 조회 설명 turn 기록

<!-- codex-turn:01a0f287-75d4-73d3-9397-eb6175e4ad11 -->

- 브랜치: develop.
- 작업 목표: 이번 수정 방향 설명의 turn 식별자를 포함한 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Firebase signup에 기존 선택 동의 두 필드 추가 및 공개 GET /api/v1/policies/consents 제안 기록. 실제 API 구현 없음.
- 실행한 테스트와 결과: 기존 동의 DTO·factory·엔티티 소스 확인, git diff --check 통과. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 개인정보·약관 필수, 선택 동의 누락은 false, true일 때 현재 버전 검증, 개인 API 인증 유지.
- 결정사항: 기존 ConsentPolicy와 UserConsents 재사용, 새 엔티티·환경변수 불필요. Guest upgrade 확장은 별도 범위.
- 위험 요소: 조회 후 정책 변경 시 재동의 필요. 구현·배포는 아직 수행하지 않음.
- 다음 작업: 설명한 두 변경 구현 및 관련 테스트. 기존 dirty 변경 보존, 예상 밖 변경 없음, Jira 변경 없음.

## 2026-09-30 — Guest upgrade 선택 동의 확장 방향 추가

- 브랜치: develop.
- 작업 목표: Guest upgrade의 학습 품질 검토 선택 동의 입력 누락 확인 및 수정 계획 확장.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: FirebaseGuestUpgradeRequest와 User.promoteGuestToFederatedMember 및 UserConsents.renewRequiredConsents 확인. 요청에 선택 동의 두 필드가 없고 현재 승격이 기존 선택 동의를 보존함을 확인. 실제 코드 구현 없음.
- 실행한 테스트와 결과: 소스 읽기 확인 및 git diff --check 수행. 방향 정리 단계로 Gradle 미실행.
- 유지한 계약: 기존 필드명 및 Guest 승격 소유권/필수 동의 검증 유지, 누락 요청의 기존 동의 보존 제안. 기존 dirty 변경 보존.
- 결정사항: signup 선택 동의·공개 정책 GET에 더해 upgrade 선택 동의 입력 추가를 범위에 포함. 신규 signup 누락은 false, upgrade 누락은 기존 상태 보존. 명시 false는 미동의 처리, true는 현재 버전 검증 후 반영.
- 위험 요소: upgrade 누락을 false로 기본화하면 기존 Guest 선택 동의가 의도 없이 철회될 수 있음. 선택 동의는 missingRequirements의 필수 단계로 추가하지 않음.
- 다음 작업: 세 API 변경 구현 시 기존 동의 보존/명시 철회/명시 동의/버전 오류 회귀 검증 추가. 예상 밖 변경 없음, Jira 변경 없음.

## 2026-09-30 — Guest upgrade 선택 동의 검토 turn 기록

<!-- codex-turn:01a0f289-7e99-7d31-a542-d6c0f23667ca -->

- 브랜치: develop.
- 작업 목표: Guest upgrade 선택 동의 확장 검토에 이번 turn 식별자를 포함한 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 요청 DTO에 선택 동의 필드가 없음을 확인하고 추가 방향 정리. 승격 시 기존 동의를 보존하는 현재 동작 확인. 애플리케이션 코드 변경 없음.
- 실행한 테스트와 결과: DTO·승격·동의 소스 확인 및 git diff --check 통과. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 기존 선택 동의 필드명, 필수 동의 검증 및 Guest 소유권 유지. 비밀정보 비기록.
- 결정사항: upgrade 필드 누락은 기존 동의 유지, 명시 false는 미동의 처리, true는 현재 버전 검증 후 저장하는 방향 제안.
- 위험 요소: 누락을 false로 기본화하면 기존 동의가 의도 없이 철회될 수 있음. 아직 구현·배포하지 않음.
- 다음 작업: signup·upgrade 선택 동의와 공개 정책 조회 구현 및 회귀 검증. 기존 dirty 변경 보존, 예상 밖 변경 없음, Jira 변경 없음.

## 2026-09-30 — Guest upgrade enrollment 사용 설명

- 브랜치: develop.
- 작업 목표: Guest 승격도 enrollment를 사용하는지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: FirebaseGuestPrepareService의 GUEST_USER enrollment 발급/재사용 및 FirebaseGuestUpgradeService의 소유권·유효성·Firebase 일치 검증 확인. 코드 변경 없음.
- 실행한 테스트와 결과: 서비스 소스 확인, git diff --check 수행. 설명 작업으로 Gradle 미실행.
- 유지한 계약: Guest 토큰 사용자 ID 기준 소유권 검증, Firebase 계정 일치 및 enrollment 유효성 유지.
- 결정사항: 신규 signup과 동일한 enrollment 구조를 쓰되 Guest prepare에서 받은 승격용 ID를 제출해야 함을 설명.
- 위험 요소: 신규 가입용 enrollment를 Guest 승격에 대체 사용할 수 없음. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 선택 동의 확장 및 공개 정책 조회 계획 유지. Jira 작업 없음.

## 2026-09-30 — Guest upgrade enrollment 설명 turn 기록

<!-- codex-turn:01a0f28b-a259-7003-b0dc-9ce02b89313b -->

- 브랜치: develop.
- 작업 목표: Guest upgrade enrollment 설명에 이번 turn 식별자를 포함한 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: guest/prepare가 발급한 GUEST_USER enrollment를 guest/upgrade에 제출하며 Guest 소유권·Firebase 계정 일치·유효성을 검증하는 현재 구현 확인. 애플리케이션 코드 변경 없음.
- 실행한 테스트와 결과: 서비스 소스 확인 및 git diff --check 수행. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 인증된 Guest 기준 소유권 및 enrollment 유효성 검사 유지. 비밀정보 비기록.
- 결정사항: 신규 signup과 같은 엔티티 구조를 사용하지만 신규 가입용 ID와 승격용 ID는 대체 불가. enrollment는 닉네임·동의 초안 저장 용도가 아님.
- 위험 요소: 가입/승격 ID 혼용 시 검증 실패. 기존 dirty 변경 보존, 이번 예상 밖 변경 없음.
- 다음 작업: 선택 동의 확장 및 공개 정책 조회 계획 유지. Jira 변경 없음.

## 2026-09-30 — 신규 signup MEMBER와 Firebase 연결 시점 설명

- 브랜치: develop.
- 작업 목표: 신규 가입 완료 시 MEMBER도 Firebase 계정에 연결되는지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: FirebaseExchangeService의 DIRECT_SIGNUP·boundUserId=null 및 FirebaseSignupService/TransactionService의 새 MEMBER 생성·FirebaseIdentity 저장 확인. 코드 변경 없음.
- 실행한 테스트와 결과: 소스 읽기 확인, git diff --check 수행. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 가입 전 Firebase 소유권 검증 및 가입 성공 시 회원·Firebase 매핑 원자적 저장 유지.
- 결정사항: 신규 가입도 Firebase 계정과 연결되며 가입 전 enrollment 연결과 가입 완료 후 MEMBER 매핑을 구분해서 설명.
- 위험 요소: enrollment 자체가 영구 회원 연결 문서라고 오해하지 않도록 FirebaseIdentity와 구분. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 선택 동의 확장 및 공개 정책 조회 계획 유지. Jira 변경 없음.

## 2026-09-30 — 신규 signup Firebase 연결 설명 turn 기록

<!-- codex-turn:01a0f28d-9078-7533-a411-467c1c3c6a4a -->

- 브랜치: develop.
- 작업 목표: 신규 signup Firebase 연결 설명에 이번 turn 식별자를 포함한 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 가입 전 DIRECT_SIGNUP enrollment의 Firebase 연결 및 가입 성공 시 새 MEMBER와 FirebaseIdentity 매핑 저장 확인. 애플리케이션 코드 변경 없음.
- 실행한 테스트와 결과: 서비스·트랜잭션 소스 확인 및 git diff --check 수행. 설명 작업으로 Gradle 미실행.
- 유지한 계약: Firebase 소유권 검증 및 회원·Firebase 매핑 원자적 저장 유지. 비밀정보 비기록.
- 결정사항: 신규 signup은 새 userId 생성, Guest upgrade는 기존 userId 유지. enrollment와 영구 FirebaseIdentity 매핑 역할 구분.
- 위험 요소: enrollment를 영구 회원 연결 문서로 오해하지 않도록 안내. 기존 dirty 변경 보존, 이번 예상 밖 변경 없음.
- 다음 작업: 선택 동의 확장 및 공개 정책 조회 계획 유지. Jira 변경 없음.

## 2026-09-30 — enrollment 유효기간과 DB 삭제 시점 설명

- 브랜치: develop.
- 작업 목표: enrollment가 TTL 초과 시 삭제되는지 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: FirebaseEnrollmentAttempt의 expiresAt·cleanupAt, lifecycle 삭제 예약, 만료 capture 및 application 기본 설정 확인. 코드 변경 없음.
- 실행한 테스트와 결과: 소스·설정 읽기 및 git diff --check 수행. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 만료 시 서버 검증으로 재사용 차단, DB 삭제와 회원·Firebase 연결의 별도 생명주기 유지.
- 결정사항: 기본 10분은 가입 시도 유효기간이며 즉시 삭제 시간이 아님. 가입 완료 후 lifecycle은 기본 24시간 뒤 TTL 정리를 예약. 미완료 건은 별도 정리 절차에 의존.
- 위험 요소: 실제 TTL 인덱스 및 배포 환경 설정 미확인. MongoDB TTL 삭제는 비동기로 정각 삭제 보장 없음. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 필요 시 실제 정리 설정·인덱스 별도 확인. 선택 동의·공개 정책 조회 계획 유지, Jira 변경 없음.

## 2026-09-30 — enrollment TTL 설명 turn 기록

<!-- codex-turn:01a0f28f-d025-7981-8040-2cb687b69152 -->

- 브랜치: develop.
- 작업 목표: enrollment 유효기간과 삭제 시점 설명의 turn 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: expiresAt과 cleanupAt 역할, 가입 완료 후 삭제 예약 및 중단 가입 별도 정리 경로를 소스 기준으로 설명. 코드 변경 없음.
- 실행한 테스트와 결과: 엔티티·lifecycle·설정 소스 확인, git diff --check 수행. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 만료 enrollment 재사용 차단 및 MEMBER/FirebaseIdentity의 별도 생명주기 유지. 비밀정보 비기록.
- 결정사항: 기본 유효기간 10분은 즉시 DB 삭제 시간이 아니며 가입 완료 후 기본 24시간 보관 뒤 TTL 정리 예약. 실제 환경 설정과 구분.
- 위험 요소: 배포 환경 설정·DB TTL 인덱스 미확인, MongoDB TTL 삭제는 비동기. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 선택 동의 확장 및 공개 정책 조회 계획 유지. 필요 시 실환경 정리 설정 별도 확인, Jira 변경 없음.

## 2026-09-30 — Guest 승격 enrollment 정리 동일성 확인

- 브랜치: develop.
- 작업 목표: Guest 승격에도 동일한 enrollment 만료·삭제 흐름 적용 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: FirebaseGuestUpgradeTransactionService의 소비 처리 및 공통 finalizeEnrollment 호출 확인. 코드 변경 없음.
- 실행한 테스트와 결과: 소스 확인 및 git diff --check 수행. 설명 작업으로 Gradle 미실행.
- 유지한 계약: enrollment 소비·만료 경계 및 회원/Firebase 연결의 별도 생명주기 유지.
- 결정사항: 기본 10분 유효기간과 완료 후 기본 24시간 삭제 예약 흐름이 Guest 승격에도 동일함을 안내.
- 위험 요소: 실제 배포 설정·TTL 인덱스 미확인. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 선택 동의 및 공개 정책 조회 계획 유지. Jira 변경 없음.

## 2026-09-30 — TMI-136 하위 선택 동의·공개 정책 조회 이슈 초안

- 브랜치: develop.
- Jira: TMI-136
- 작업 목표: 기존 논의한 SNS 가입·Guest 승격 선택 동의 및 공개 정책 조회 작업을 TMI-136 하위 이슈로 준비.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 공식 Atlassian 도구로 부모 에픽 sns 로그인, 프로젝트 작업 유형 및 기존 하위 이슈 확인. 제목·계약·완료 조건을 포함한 신규 작업 초안 작성. 코드 변경 없음.
- 실행한 테스트와 결과: Jira 읽기 조회 성공, git diff --check 수행. 초안 준비 작업으로 Gradle 미실행.
- 유지한 계약: 기존 필드명, 신규 signup 누락 false, Guest upgrade 누락 기존 동의 보존, 개인 동의 API 인증, enrollment 경계 유지.
- 결정사항: TMI-136 에픽 아래 작업 1건으로 세 API 변경 통합. AGENTS.md에 따라 내용을 먼저 제시하고 승인 후 생성.
- 위험 요소: 사용자 승인 전이라 Jira 생성 미실행. 기존 하위 이슈 제목에서 직접 중복 작업 발견하지 않음, 전체 기존 본문 중복 조사 미수행. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- Jira 작업: 부모/유형/자식 조회만 수행. 댓글 등록·상태 변경 없음. 생성 승인 대기.
- 다음 작업: 사용자 초안 승인 시 생성하고 부모 연결 재조회. Jira 댓글 초안: 선택 동의 및 공개 정책 조회 이슈 범위 준비 완료, 구현·테스트는 후속 작업에서 수행 예정. 자동 등록하지 않음.

## 2026-09-30 — TMI-136 하위 Jira 초안 검토 turn 기록

<!-- codex-turn:01a0f295-c7b4-75e3-ab58-00091111be92 -->

- 브랜치: develop.
- Jira: TMI-136
- 작업 목표: 하위 작업 생성 초안 검토의 turn 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 부모 에픽과 작업 유형 및 기존 자식 조회, signup·upgrade 선택 동의와 공개 정책 조회를 묶은 작업 제목·계약·완료 조건 준비 및 사용자에게 제시. 코드 변경 없음.
- 실행한 테스트와 결과: 공식 Atlassian 읽기 조회 성공, git diff --check 수행. 초안 작업으로 Gradle 미실행.
- 유지한 계약: 선택 동의 필드명, Guest 누락 시 보존, 필수 동의 및 개인 API 인증 유지.
- 결정사항: AGENTS.md의 사전 내용 공개·승인 규칙에 따라 생성 승인 대기.
- 위험 요소: 아직 신규 Jira 이슈는 생성되지 않음. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- Jira 작업: 읽기 조회만 수행, 생성·댓글·상태 변경 없음, 생성 승인 대기.
- 다음 작업: 사용자 승인 후 신규 작업 생성 및 부모 연결 확인. Jira 댓글 초안은 직전 기록 유지, 자동 등록하지 않음.

## 2026-09-30 — TMI-188 선택 동의·공개 정책 조회 작업 생성

- 브랜치: develop.
- Jira: TMI-188
- 작업 목표: 승인된 초안으로 TMI-136 하위 작업 생성.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 공식 Atlassian 도구로 작업 생성 및 부모 TMI-136 연결 재조회 확인. 애플리케이션 코드 변경 없음.
- 실행한 테스트와 결과: 생성 성공 및 제목·부모·해야 할 일 상태 확인, git diff --check 수행. Jira 작업만 수행하여 Gradle 미실행.
- 유지한 계약: 승인한 signup·upgrade 선택 동의 및 공개 정책 조회 범위, 기존 인증/필수 동의/enrollment 계약 유지.
- 결정사항: 신규 이슈 TMI-188로 후속 구현 범위 관리. 구현 미시작.
- 위험 요소: 서버 변경·배포는 아직 없음. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- Jira 작업: TMI-188 생성, 사용자 명시 승인 후 수행. 댓글 등록 없음, 상태 전환 없음(기본 해야 할 일).
- 다음 작업: TMI-188 기준 구현 및 검증. Jira 댓글 초안: 작업 생성 및 TMI-136 부모 연결 확인 완료, 코드 변경 없음, 구현 검증은 후속 작업 예정. 자동 등록하지 않음.

## 2026-09-30 — TMI-188 생성 완료 turn 기록

<!-- codex-turn:01a0f298-f345-7770-9d5a-e9dcdeab20e2 -->

- 브랜치: develop.
- Jira: TMI-188
- 작업 목표: 승인된 Jira 생성 작업의 turn 식별자를 포함한 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 공식 Atlassian 도구로 SNS signup·Guest upgrade 선택 동의 및 공개 정책 조회 작업 생성. 부모 TMI-136 연결 재조회 확인. 코드 변경 없음.
- 실행한 테스트와 결과: Jira 생성 성공 및 부모·기본 상태 확인, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 승인된 이슈 범위 및 기존 필수 동의·인증·enrollment 계약 유지. 비밀정보 비기록.
- 결정사항: 후속 구현은 TMI-188 기준으로 진행, 현재 구현 미시작.
- 위험 요소: API 구현·배포는 아직 없음. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- Jira 작업: 사용자 명시 승인 후 TMI-188 생성. 댓글 등록 없음, 상태 전환 없음, 기본 상태 해야 할 일.
- 다음 작업: TMI-188 구현 및 검증. Jira 댓글 초안은 직전 기록 유지, 자동 등록하지 않음.

## 2026-09-30 — TMI-188 SNS 가입·Guest 승격 선택 동의 및 공개 정책 조회 구현

<!-- codex-turn:01a0f29b-84b6-7b03-92bc-008f0e80c92b -->

- 브랜치: feat/TMI-188-quality-review-consent.
- Jira: TMI-188
- 작업 목표: 승인된 이슈의 signup/upgrade 선택 동의 저장 및 가입 전 익명 정책 버전 조회 구현.
- 변경 파일: FirebaseSignupRequest, FirebaseGuestUpgradeRequest, FirebaseSignupService, FirebaseGuestUpgradeService, FirebaseExchangeController, UserFactory, User, SecurityConfig, IdentityOpenApiExamples. 신규 ConsentPolicyController·CurrentConsentPolicyResponse. 테스트 FirebaseSignupServiceTests·FirebaseGuestUpgradeServiceTests·FirebaseExchangeControllerTests·SecurityIntegrationTests·OpenApiSharingTests 및 신규 FirebaseConsentRequestTests. 문서 docs/contracts/frontend-firebase-auth-integration-guide.md, docs/contracts/frontend-firebase-auth-integration-appendix.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 구현 내용: 기존 선택 동의 필드명/검증 재사용. signup 누락/null은 false, upgrade 누락/null은 기존 선택 동의 보존, 명시 false는 버전·시각 비움, true는 현재 버전 검증 후 저장. 유효한 기존 동의 시각은 보존하고 새 동의/버전 갱신은 서버 시각 사용. 공개 GET /api/v1/policies/consents는 동일 ConsentPolicy의 현재 세 버전을 BaseResponse.result에 반환하며 no-store 사용. 사용자 상태 및 정책 본문은 반환하지 않음. OpenAPI/프론트 요청 예시·누락 처리·버전 오류/재동의·QA 갱신.
- 실행한 테스트와 결과: 최초 샌드박스 Gradle 캐시 잠금 접근 실패 후 승인된 권한으로 실행. 첫 전체 테스트는 983개 중 Swagger endpoint count 기대값 1건 실패. 새 공개 GET로 24→25 operation 및 23→24 path 변경 반영, Swagger 응답 예시 추가 후 ./gradlew clean test 최종 통과(983 tests, failures=0, errors=0). git diff --check 통과. 테스트에서 외부 Provider/Repository는 mock 사용, 실제 외부 인증/DB 호출 없음.
- 유지한 계약: 개인정보/약관 필수 동의, 개인 동의 GET/PUT JWT 인증, Guest userId 유지, enrollment 소유권·만료·소비와 기존 트랜잭션 유지. 선택 동의는 missingRequirements에 추가하지 않음. 구버전 JSON 요청 호환, 새 엔티티·환경변수 없음.
- 결정사항: Guest upgrade는 Boolean null을 유지하여 보존/철회 구분. 기존 domain 승격 overload는 보존 동작으로 위임. 공개 정책 API는 해당 GET만 permitAll.
- 위험 요소: 배포·모바일 E2E는 미수행. 프론트 정책 본문/URL과 조회 버전의 대응 확인 필요, 조회 이후 version 변경 시 재동의 처리 필요. 실환경 정책 설정은 기존 환경변수 재사용.
- 예상 밖 변경: 이번 작업 범위 밖 추가 변경 없음. 시작 전부터 dirty였던 identity-branch-deployment.md, identity-test-container-secrets.json, identity-test-task-definition.draft.json, DeploymentTargetTests.java 및 docs/postman·tools는 수정하지 않고 보존. 기존 작업 기록도 보존.
- 배포 전 확인: 대상 환경의 세 정책 설정·본문/URL 대응 및 새 GET 익명 조회, signup/upgrade의 선택 동의 처리와 개인 API 인증 확인. 프론트는 signup/upgrade의 누락 규칙 차이를 반영.
- Jira 작업: 구현 전 TMI-188 설명 조회만 수행. 상태 전환·댓글 등록 없음. 생성 승인과 별개로 이번 구현 완료 댓글은 승인 전 미등록.
- Jira 댓글 초안: signup/upgrade 선택 동의 및 익명 정책 조회 구현 완료. 관련 DTO·서비스·domain·보안·Swagger·계약 문서/테스트 갱신. clean test 983개 통과. 대상 환경 배포와 정책 본문/버전 대응 및 모바일 연동 검증 필요. 자동 등록하지 않음.
- 다음 작업: 사용자 diff 검토 후 직접 커밋/push. 필요 시 승인받은 Jira 댓글 및 후속 배포·모바일 검증. 커밋/push/PR 생성 및 배포는 이번 작업에서 수행하지 않음.

## 2026-10-01 — TMI-189 Firebase 동일 UID SNS 최초 연결의 단일 로그인 구현

<!-- codex-turn:01a0f58d-ed66-7321-b9da-8badaf7639dd -->

- 브랜치: feat/TMI-189-firebase-provider-auto-link.
- Jira: TMI-189
- 작업 목표: 기존 ACTIVE MEMBER의 같은 Firebase 프로젝트/UID에 연결된 현재 Google/Apple을 안전 조건 확인 후 등록하여 정상 최초 연결에 추가 기존 SNS 재로그인을 요구하지 않음.
- 변경 파일: 신규 ProviderLoginRegistrationService.java, ProviderLoginRegistrationServiceTests.java. FirebaseExchangeService.java, FirebaseAdminAuthenticationVerifier.java, FirebaseSdkAdminClient.java, ProviderChangeGuard.java, AuthMethodChangeControl.java, ProviderChangeConfiguration.java. 테스트 FirebaseExchangeServiceTests.java, FirebaseAdminAuthenticationVerifierTests.java, FirebaseSdkAdminClientTests.java, ProviderChangeConfigurationTests.java. docs/contracts/frontend-firebase-auth-integration-guide.md, frontend-firebase-auth-integration-appendix.md, docs/codex/CURRENT_STATE.md, WORKLOG.md.
- 구현 내용: LOGIN_EXCHANGE에 한해 Guard의 미등록 Google/Apple 판단을 등록 서비스로 위임하되 block/floor는 즉시 검사. Firebase SDK에서 서명된 현재 제공자 subject 증빙과 Admin 최신 연결을 교차 확인(누락/다중/불일치 거절). 등록 서비스는 exact Firebase binding·ACTIVE MEMBER·세션 epoch 및 인증 경계를 재검증하고 현재 provider만 저장. 세션 control을 touch하여 unlink/withdrawal/logout-all 등과 CAS 경합하고 제공자 revision 증가로 오래된 PREPARED intent를 무효화. 등록·기존 소유권 검사·세션 발급을 동일 Mongo 트랜잭션에서 실행. 과거 block/floor, 기존 같은 provider의 다른 subject, 다른 회원 소유, active security slot/미완료 작업은 자동 승인하지 않음. duplicate key는 409, 일반 저장/트랜잭션 장애는 기존 진단을 포함한 503으로 안전하게 실패하며 다음 제한 재시도에서 동일 회원의 기등록 연결로 수렴. 기존 탈퇴/비활성 오류 선행 검사 유지.
- 실행한 테스트와 결과: 초기 새 mock 테스트에서 Mockito 재스텁/중첩 mock 생성 오류가 발생하여 테스트 초기화를 수정. Gradle 캐시 권한 제한은 승인된 실행으로 해결. 최종 ./gradlew clean test BUILD SUCCESSFUL, XML 집계 tests=1009, failures=0, errors=0, skipped=0. git diff --check 통과. Google↔Apple 정상 등록, 현재 provider만 등록, 중복 요청, 소유권/교체/이력/Guest/UID/binding/탈퇴 거절, 트랜잭션 진입 시 unlink/logout 경합, CAS 실패, 발급 예외, 로그인 목적 한정, 증빙 불일치, fence OFF의 거절과 기존 signed JWT 발급 계약 테스트 포함. 신규 테스트의 외부 Provider/Repository는 mock; 실제 Atlas/OAuth는 호출하지 않음.
- 유지한 계약: /api/v1/auth/firebase/exchange 요청/응답 및 UUID userId, RS256/JWKS/audience/MEMBER 토큰 계약 불변. signup/Guest/sync/high-risk 경로에 자동 등록 없음, Kakao 자동 등록 제외. Google/Apple의 SDK 증빙 교차 검증은 공통 검증 어댑터에 적용됨. 이메일만으로 병합·UID rebind·다른 제공자 일괄 등록·기존 차단 해제·LC/Billing 변경 없음.
- 결정사항: 기존 AUTH_SESSION_FENCE_ENABLED=true에서 등록 서비스를 설치하고 OFF에서는 미등록 SNS를 거절한다. 명시 link capture와 별개이며 새로운 설정/컬렉션 없음. auto registration 후 provider control이 생성되므로 sync 사용 환경에서는 FIREBASE_PROVIDER_CHANGE_FENCE_ENABLED를 확인한다(OFF+보안 상태 존재 시 기존 sync 거절 유지). 충돌 시 무한 자동 재시도/성공 간주 금지. 프론트 정상 경로와 409/503 대응·배포 QA 문서화.
- 위험 요소: mock 테스트는 실제 replica-set write conflict/rollback의 증명이 아님. 테스트 환경 E2E와 모바일 Firebase 자동 연결·subject claim 형태는 배포 후 확인 필요. 대상 환경 feature flag, 기존 unique index 및 모든 보안 writer의 동시 배포 호환성 확인 필요. 배포/외부 데이터 보정/실제 SNS 로그인 미수행이므로 Jira 전체 완료로 판단하지 않음.
- 예상 밖 변경: 없음. 시작 전부터 WORKLOG에 존재한 누적 미커밋 변경은 그대로 보존했고 과거 항목은 변경하지 않음. 이번 작업은 코드/테스트/계약/작업 기록 범위만 변경.
- 배포 전 확인: AUTH_SESSION_FENCE_ENABLED, 필요 시 provider-change fence, Mongo 트랜잭션 및 social_identities provider+subject unique 인덱스. Google→Apple/Apple→Google 동일 회원 로그인을 테스트 환경에서 재현, 다른 UID/차단/교체/동시 보안 작업 실패 및 rollback 확인. 기존 전화번호·가입·Guest·명시 link 회귀 확인.
- Jira 작업: 공식 Atlassian MCP로 TMI-189 설명/완료 조건 조회만 수행. 사용자의 개발 요청에 따라 구현했으며 이슈 수정·상태 전환·댓글 등록 없음.
- Jira 댓글 초안(미등록): TMI-189 로컬 구현 완료. ProviderLoginRegistrationService 및 Exchange/Guard/SDK/설정·테스트·프론트 계약 갱신. 동일 UID ACTIVE MEMBER의 현재 Google/Apple만 안전 조건하에 등록하고 세션 발급과 트랜잭션 결합. clean test 1009개 및 diff check 통과. 실제 Firebase E2E/replica-set 경합 검증과 배포 설정 확인은 남아 있음.
- 다음 작업: 사용자 diff 검토 후 직접 커밋/push. 별도 승인된 테스트 배포 및 E2E 후 PR 병합/완료 조건 확인. 이번 작업에서 커밋/push/배포/PR 생성 및 Jira 변경은 수행하지 않음.

## 2026-10-01 — TMI-189 구현 코드 설명

<!-- codex-turn:01a0f5c9-f303-7da2-b06f-4d7a367122e2 -->

- 브랜치: feat/TMI-189-firebase-provider-auto-link.
- Jira: TMI-189
- 작업 목표: 실제 구현을 코드 근거와 함께 사용자에게 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md만 이번 turn 갱신. 기존 코드/테스트/계약 문서 dirty 변경 보존.
- 구현 내용: 추가 구현 없음. FirebaseSdkAdminClient의 signed subject와 Admin providerData 비교, ProviderChangeGuard의 로그인 목적 한정 위임, FirebaseExchangeService의 등록 후 기존 토큰 발급, ProviderLoginRegistrationService의 binding/회원/소유권/보안 상태 검사와 트랜잭션, SessionSecurityService의 control version 경합, 조건부 Bean 설치를 재확인.
- 실행한 테스트와 결과: 코드 변경 없는 설명 작업이라 테스트 재실행하지 않음. 기존 XML 결과 tests=1009, failures=0, errors=0 확인. git diff --check 수행.
- 유지한 계약: exchange DTO/userId/JWT 유지, 이메일 자동 병합·다른 UID 연결·차단 해제·다른 provider 일괄 등록 없음. 현재 Google/Apple 최초 등록만 허용.
- 결정사항: Guard의 return은 로그인 성공이 아니라 최종 등록 서비스로 판단을 넘기는 것임을 명시. epoch/CAS와 제공자 revision 역할, 경합 후 제한 재시도와 응답 멱등성 차이를 구분하여 설명.
- 위험 요소: 실제 Firebase 및 Mongo replica-set E2E/경합/rollback 미검증. AUTH_SESSION_FENCE_ENABLED=true 필요, sync 사용 시 provider-change fence 확인 필요. 기존 로컬 테스트 성공을 배포 완료로 해석하지 않음.
- 예상 밖 변경: 없음. Jira 조회/변경, 커밋/push/배포 미수행.
- Jira 댓글 초안(미등록): 구현 코드 흐름과 보안 조건 설명 완료. 애플리케이션 추가 변경 없음. 기존 1009개 성공 결과 확인, 대상 환경 E2E/경합 검증은 남아 있음.
- 다음 작업: 사용자 변경 검토 후 직접 커밋/push 및 승인된 테스트 배포·실환경 검증.

## 2026-10-01 — TMI-189 Kakao 최초 연결 확장 범위 설명

<!-- codex-turn:01a0f5d2-f322-7111-a23f-f7cb5e59783d -->

- 브랜치: feat/TMI-189-firebase-provider-auto-link.
- Jira: TMI-189
- 작업 목표: Google/Apple 한정 이유와 향후 Kakao 동일 로그인 UX 지원에 필요한 변경 설명.
- 변경 파일: docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md. 애플리케이션 추가 수정 없음.
- 구현 내용: 기존 FirebaseAdminAuthenticationVerifier의 Kakao 설정 ID 매핑/활성 검사와 Guard·ProviderLoginRegistrationService·FirebaseSdkAdminClient의 Google/Apple 한정 조건 재확인. 기존 Kakao 로그인 지원과 새 미등록 제공자 자동 등록 지원을 구분.
- 실행한 테스트와 결과: 관련 소스 조회 및 git diff --check. 설명 작업이므로 Gradle 재실행 없음.
- 유지한 계약: 기존 Kakao 일반 로그인/명시 연결 동작 유지. 현재 TMI-189 자동 등록은 Google/Apple 한정. 비밀정보 비기록.
- 결정사항: 기술적 제약이 아닌 기존 이슈 범위 제한임을 설명. Kakao 확장은 활성화된 지원 제공자 정책·설정 OIDC ID 기반 signed subject와 최신 원격 연결 교차 검증·소유권/차단/교체/경합 테스트를 함께 변경하는 방향 권장. 단순 KAKAO 조건 추가는 불충분.
- 위험 요소: 실제 Kakao OIDC 토큰의 증빙 형태와 Firebase 연결 동작은 실환경 확인 필요. 확인되지 않은 provider를 포괄 허용하지 않음.
- Jira 작업: 변경 없음. 댓글 초안(미등록): Kakao 자동 등록 확장 지점 분석, 코드 변경 없음, 실제 OIDC 증빙 검증과 회귀 테스트 필요.
- 예상 밖 변경: 없음. 기존 dirty 변경 보존.
- 다음 작업: 사용자 구현 요청 및 필요 시 Jira 범위 변경 승인 후 Kakao를 포함한 공통 정책 구현/검증.

## 2026-10-01 — 테스트 배포와 Kakao 활성 설정 운영 순서 안내

<!-- codex-turn:01a0f5e2-83d1-7f90-a47f-d1d96e61114e -->

- 브랜치: feat/TMI-189-firebase-provider-auto-link.
- Jira: TMI-189
- 작업 목표: 현재 변경 배포 후 Kakao 준비 순서 및 활성 설정 유지 이유 설명.
- 변경 파일: docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 구현 내용: 코드 추가 변경 없음. application.yml의 FIREBASE_KAKAO_ENABLED 기본 false 및 설정된 OIDC ID 매핑/활성 검사 확인. 서버 활성 플래그와 Firebase provider 등록·프론트 버튼 노출을 구분.
- 실행한 테스트와 결과: 소스 읽기 및 git diff --check. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 사용자가 commit/push 수행, 미준비 제공자 자동 허용 없음. 현재 Google/Apple 자동 등록 범위 유지.
- 결정사항: 현재 변경 테스트 배포·Google/Apple 재현 검증 후 Kakao 준비, 미등록 provider 자동 등록/subject 증빙 검증 확장 및 회귀 검증, 서버 ON/프론트 노출 순서 권장. 장기적으로 Kakao를 지원하더라도 환경별 준비/운영 제어 설정은 유지하고 정상 운영에서는 ON으로 유지 가능.
- 위험 요소: Kakao 설정만 ON해도 TMI-189 자동 등록 범위가 확장되지는 않음. 기존 Kakao 전용 회원이 생긴 뒤 OFF하면 로그인 제한이 발생하므로 영향 평가 필요. 실제 배포 및 Firebase/Kakao 설정 미확인.
- 예상 밖 변경: 없음. 기존 dirty 변경 보존. Jira 변경/댓글 등록 및 commit/push/배포 미수행.
- Jira 댓글 초안(미등록): 배포 및 Kakao 준비/활성화 순서 설명, 코드 변경 없음. 실제 OIDC 준비·자동 등록 확장·E2E 남음.
- 다음 작업: 사용자 변경 검토·직접 commit/push 후 테스트 배포 검증. 이후 승인된 Kakao 설정/코드 확장 진행.

## 2026-10-01 — Kakao 연결 불일치 해결 의도 확인

<!-- codex-turn:01a0f5e3-7766-74d1-8e15-e6f4d7d515b4 -->

- 브랜치: feat/TMI-189-firebase-provider-auto-link.
- Jira: TMI-189
- 작업 목표: 사용자의 Kakao 확장 이해 확인.
- 변경 파일: docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 구현 내용: 추가 구현 없음. 같은 Firebase UID에 Kakao가 연결됐지만 Identity에 미등록인 경우 안전 조건하에 현재 provider 등록을 지원하려는 의미로 설명. Firebase의 자동 연결 자체가 아니라 양쪽 등록 상태 불일치의 처리 공백을 해결하는 것임을 구분.
- 실행한 테스트와 결과: 코드 변경 없는 설명으로 Gradle 미실행, git diff --check 수행.
- 유지한 계약: 이메일만으로 병합하지 않음, 다른 UID/소유자/차단/교체는 우회하지 않음.
- 결정사항: Google/Apple과 동일한 UX 및 보안 정책으로 Kakao 확대 방향. 실제 Kakao OIDC의 동일 UID 자동 연결 발생 여부는 미확인으로 명시.
- 위험 요소: 카카오가 항상 같은 이메일에 자동 연결된다고 단정할 수 없음. Firebase 연결 형태/토큰 증빙 확인 필요.
- 예상 밖 변경: 없음. Jira/외부 설정/커밋/push/배포 변경 없음.
- Jira 댓글 초안(미등록): Kakao 등록 상태 불일치 해결 의도 확인. 구현 추가 없음, 실제 OIDC 동작 확인 필요.
- 다음 작업: 승인된 범위에서 Kakao 검증 및 자동 등록 확장 구현.

## 2026-10-01 — Kakao 확장 의도 확인 turn 기록 보완

<!-- codex-turn:01a0f5e3-d705-70c1-9e94-15d82a5da49d -->

- 브랜치: feat/TMI-189-firebase-provider-auto-link.
- Jira: TMI-189
- 작업 목표: 현재 설명 turn의 정확한 식별자로 작업 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Kakao도 동일 Firebase UID에 연결됐지만 Identity에 미등록인 상태를 안전 검증 후 처리하는 확장 의도 확인. Firebase 자동 연결 자체를 막는 작업은 아님.
- 실행한 테스트와 결과: 문서 변경으로 Gradle 미실행. git diff --check 수행.
- 유지한 계약: 이메일 자동 병합·차단 우회 없음, 현재 애플리케이션 동작 불변.
- 결정사항: 과거 기록은 유지하고 현재 turn 식별자 기록을 EOF에 추가.
- 위험 요소: 실제 Kakao OIDC 자동 연결/식별 증빙은 미검증.
- 예상 밖 변경: 없음. 외부 설정·Jira·커밋/push/배포 변경 없음.
- Jira 댓글 초안(미등록): Kakao 확장 의도 설명 완료, 추가 구현 없음, 실제 OIDC 검증 필요.
- 다음 작업: 승인된 범위의 Kakao 자동 등록 확장 구현 및 검증.

## 2026-09-23 — CNAME 등록 실패 확인 요청

<!-- codex-turn:01a0cd9a-63c5-7d63-af9d-3a7af1766713 -->

- 브랜치: develop.
- 작업 목표: 사용자 CNAME 등록 실패의 원인 확인 준비.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 가비아 실제 오류 문구/입력 화면 확인 요청. 호스트 identity-test와 ALB DNS 값 입력 방식, 동일 호스트 레코드 충돌 가능성을 안내하되 원인 확정하지 않음.
- 테스트와 결과: git diff --check. 안내/기록만 변경하여 Gradle 미실행, DNS 재조회 및 외부 설정 변경 없음.
- 유지한 계약: 기존 DNS/인증서 검증 레코드 삭제 또는 수정 없음. 비밀값 미기록, 기존 변경 보존.
- 결정사항: 실제 오류 확인 전 DNS 변경하지 않음.
- 위험 요소: 입력 형식/기존 레코드/권한 중 원인 미확인. 예상 밖 변경 없음.
- 다음 작업: 오류 문구 또는 가비아 화면 확인 후 정확한 해결 방법 안내.

## 2026-09-23 — 테스트 도메인 DNS 및 HTTPS 연결 확인

- 브랜치: develop.
- 작업 목표: 사용자 CNAME 설정 결과 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: dig로 테스트 도메인의 CNAME이 지정한 ALB DNS와 일치함 확인. TLS 검증을 유지한 curl health 요청에서 HTTP503 수신.
- 테스트와 결과: DNS NOERROR/TTL600 및 HTTPS503 확인, git diff --check 수행. 제품 변경 없어 Gradle 미실행.
- 유지한 계약: DNS/AWS/코드 변경 없이 읽기 전용 확인. 비밀값 미기록, 기존 변경 보존. 예상 밖 변경 없음.
- 결정사항: DNS 연결 문제는 해소. HTTP503은 직전 desired0 상태와 부합하며 실제 서비스 정상 기동 완료를 의미하지 않음.
- 위험 요소: 현재 ECS 상태 재조회 및 이미지/DB/인증 기동 검증은 미수행. 원격 workflow 수정 반영 여부도 후속 확인 필요.
- 다음 작업: 사용자 workflow commit/push 후 테스트 이미지 및 revision 확인, 서버 기동/health/JWKS 검증.

## 2026-09-23 — DNS 연결 확인 작업 식별 기록 보완

<!-- codex-turn:01a0cd9b-75fb-7211-9de9-7c090e18cdd7 -->

- 브랜치: develop.
- 작업 목표: 이번 DNS/HTTPS 확인 결과에 현재 작업 식별자 연결.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: CNAME 일치 및 TLS 검증 유지 HTTPS503 확인 결과 기록 보완. 과거 기록 보존.
- 테스트와 결과: git diff --check 수행. 문서만 보완하여 Gradle 미실행.
- 유지한 계약: 추가 외부 변경 없음, 비밀값 미기록, 기존 변경 보존.
- 결정사항: DNS 연결 확인 완료, 실제 서버 정상 기동은 미완료.
- 위험 요소: 현재 ECS 상태/이미지/DB/인증 기동 미검증. 예상 밖 변경 없음.
- 다음 작업: workflow 반영 후 테스트 이미지 배포와 기동 검증.

## 2026-09-23 — develop 최초 테스트 배포 및 시작 실패 진단

- 브랜치: develop.
- 작업 목표: 사용자 push 확인 및 테스트 서버 기동.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 제품 코드 변경 없음.
- 수행 내용: 원격 develop 88ff5bed 및 분기별 workflow 확인. Actions 35843168284 테스트/이미지 빌드/ECS 배포 성공, health503 실패 확인. 테스트 서비스 test:2 desired0에서1 기동, CloudWatch 구조화 로그에서 PhoneEligibilityFingerprintHasher bean 부재 확인. 반복 시작 실패를 막기 위해 desired0 원복 요청.
- 원인: Firebase signup은 eligibility hasher를 필수 주입하지만 테스트 task 설정은 PHONE_ELIGIBILITY_BINDING_ENABLED=false. Billing 외부 publisher 비활성화와 내부 가입 의존성의 구분이 누락됨.
- 테스트와 결과: CI Run tests 성공, 기동 후 HTTPS502 및 application startup failure 확인. 로컬 제품 변경 없어 Gradle 재실행 생략. git diff --check 수행.
- 유지한 계약: 운영 서비스/권한/키 변경 없음, 비밀값 비노출, 사용자 변경 보존. 예상 밖 파일 변경 없음.
- 결정사항: 내부 binding 설정과 전용 키 준비 후 재기동하며 외부 Billing publisher는 OFF 유지. 이번에는 Secret 생성이나 코드 변경하지 않음.
- 위험 요소: DB/인증/JWKS 정상 동작 미검증, CI run은 health 실패 상태로 남음.
- 다음 작업: binding ON, consumer scope, 전용 keyring 주입 범위 확정 및 테스트 task revision 보완 후 health/회원 인증 검증.
- 원복 검증: AWS 서비스 업데이트 성공 알림 및 desired0/running0/pending0 확인 완료, test:2 유지.

## 2026-09-23 — 최초 테스트 배포 진단 작업 식별 기록

<!-- codex-turn:01a0cd9e-ef7f-79e3-8e8d-9535653d9c29 -->

- 브랜치: develop.
- 작업 목표: push 이후 테스트 배포 및 기동 진단 결과의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: CI 테스트/빌드/ECS 배포 성공과 health 실패 확인. 테스트 기동 중 PhoneEligibilityFingerprintHasher 필수 bean 누락 확인, 테스트 서비스 desired0/running0/pending0 원복 검증.
- 실행한 테스트와 결과: CI Run tests 성공. 실제 기동 실패 확인. 문서 보완만 수행하여 로컬 Gradle 미실행, git diff --check 통과.
- 유지한 계약: 운영 서비스 변경 없음, 과거 기록 및 기존 변경 보존, Secret 비기록.
- 결정사항: 내부 eligibility binding 설정 보완 필요. 외부 Billing publisher OFF 유지. 해당 hasher 키는 전화번호 식별용 HMAC 키이며 응답 복구 암호화 키와 구분한다.
- 위험 요소: 실제 가입/로그인/JWKS 정상 동작은 아직 검증하지 못함. 예상 밖 파일 변경 없음.
- 다음 작업: 테스트 내부 binding ON 및 consumer scope/전용 HMAC keyring 준비 후 새 task revision 기동 검증.

## 2026-09-23 — 테스트 내부 eligibility binding 설정 보완 준비

- 브랜치: develop.
- 작업 목표: 가입 필수 hasher 누락 기동 실패를 해결할 설정 준비.
- 변경 파일: docs/contracts/identity-test-task-definition.draft.json, identity-test-container-secrets.json, identity-branch-deployment.md, src/test/java/web/tosunsaeng/identity/deployment/DeploymentTargetTests.java, docs/codex/WORKLOG.md, CURRENT_STATE.md.
- 구현 내용: 내부 binding true, 테스트 consumer scope, 기존 테스트 Secret의 독립 HMAC key 항목 selector 준비. 외부 publisher false 유지. 설정 회귀 테스트 추가. 사용자 키 저장용 Secret 화면 열기.
- 실행한 테스트와 결과: ./gradlew clean test --no-daemon 성공(초기 sandbox Gradle lock 접근 실패 후 승인된 실행 성공), git diff --check 수행.
- 유지한 계약: API/제품 로직/운영 서비스/IAM 변경 없음, 비밀값 미생성/미기록, 기존 fingerprint 키 변경 없음. 기존 사용자 문서 수정 보존, 예상 밖 파일 변경 없음.
- 결정사항: 기존 승인된 Secret 안에 별도 키 항목을 사용해 IAM 확대 없이 주입. 새 키 입력/저장은 사용자 수행.
- 위험 요소: AWS 설정 미적용, 실제 health/인증 검증 미완료. 로컬 draft의 과거 image를 재사용하지 않고 현재 AWS test:2 기준으로 revision 생성 필요.
- 다음 작업: 사용자 키 저장 완료 확인 후 새 task revision/서비스 기동 및 health/JWKS 검증. 배포 전 독립 키 항목 존재와 selector 확인 필수. Jira 연결 없음, 자동 댓글 미등록.

## 2026-09-23 — 내부 binding 설정 보완 작업 식별 기록

<!-- codex-turn:01a0cda6-ba83-7112-91d2-ce73aefcfc96 -->

- 브랜치: develop.
- 작업 목표: 테스트 가입 필수 내부 binding 설정 보완 결과와 사용자 키 저장 대기 상태 기록.
- 변경 파일: docs/contracts/identity-test-task-definition.draft.json, docs/contracts/identity-test-container-secrets.json, docs/contracts/identity-branch-deployment.md, src/test/java/web/tosunsaeng/identity/deployment/DeploymentTargetTests.java, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 내부 binding ON/테스트 consumer scope/독립 HMAC key JSON selector 준비 및 회귀 테스트 추가. Secrets Manager 대상 화면을 사용자에게 인계.
- 실행한 테스트와 결과: ./gradlew clean test --no-daemon 성공, 147 suites/958 tests/0 failures/0 errors. git diff --check 통과.
- 유지한 계약: 외부 publisher OFF, 운영/API/IAM 변경 없음. 기존 키 및 사용자 변경 보존, Secret 미기록.
- 결정사항: 새 비밀키 입력과 저장은 사용자 수행. AWS task revision 변경 및 재기동은 저장 이후 진행.
- 위험 요소: 로컬 설정만 보완된 상태이며 실제 서버 정상 기동/인증 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 저장 완료 확인 후 현재 AWS test:2 기반 새 revision을 적용하고 health/JWKS 검증.

## 2026-09-23 — 테스트 task:3 적용 및 HMAC keyring 형식 진단

- 브랜치: develop.
- 작업 목표: 사용자 Secret 저장 후 새 설정 배포와 정상 기동 확인.
- 변경 파일: docs/contracts/identity-test-task-definition.draft.json(배포 이미지 정합), docs/contracts/identity-branch-deployment.md, docs/codex/WORKLOG.md, CURRENT_STATE.md.
- 수행 내용: Secret 저장 성공과 새 항목 존재 확인. AWS test:2 JSON 편집기에서 현재 이미지를 검증하고 내부 binding ON/consumer scope/독립 Secret selector만 변경해 test:3 등록, 테스트 서비스 desired1 적용. CloudWatch 기동 실패 확인 후 desired0/running0/pending0 원복 완료.
- 진단: bean 누락은 해결됐으나 Phone eligibility binding configuration is invalid 발생. UI 값의 형식만 검사해 Base64 32바이트 단독 저장 및 버전/ACTIVE_WRITE 접두사 누락 확인. 비밀값은 출력/기록하지 않음.
- 테스트와 결과: 실제 HTTPS502/503 및 기동 실패 확인, 정상 health/JWKS 미검증. 이전 전체 958개 테스트 통과 유지, 이번 제품 로직 변경 없어 Gradle 미실행. JSON 파싱/git diff --check 수행.
- 유지한 계약: 기존 배포 이미지/서명/외부 publisher OFF 보존, IAM 및 운영 변경 없음. 사용자 변경 보존, 예상 밖 파일 변경 없음.
- 결정사항: 비밀값 직접 수정은 사용자에게 인계. 기존 값에 test-v1,ACTIVE_WRITE, 접두사 추가 후 test:3 재기동. 새 키 생성 불필요.
- 위험 요소: 서버는 중지 상태이며 최초 가입/로그인/DB 검증 미완료. 구버전 task의 과거 오류가 같은 로그 목록에 존재하므로 시각/태스크 구분 필요.
- 다음 작업: 사용자 형식 보완 저장 후 task:3 기동 및 health/JWKS 확인. Jira 작업 없음.

## 2026-09-23 — task:3 배포 진단 작업 식별 기록

<!-- codex-turn:01a0cdaa-0452-7252-88aa-880e36c157cd -->

- 브랜치: develop.
- 작업 목표: 테스트 배포 및 keyring 형식 오류 진단 결과 기록.
- 변경 파일: docs/contracts/identity-test-task-definition.draft.json, docs/contracts/identity-branch-deployment.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 이미지 88ff5bed 유지한 test:3 등록/기동. 내부 binding bean 누락은 해결됐으나 keyring 형식 오류로 시작 실패. 비밀값 노출 없이 버전/상태 접두사 누락 확인. 테스트 서비스 desired0/running0/pending0 원복 검증.
- 테스트와 결과: 실제 health502/503 및 시작 실패 확인, JSON 검증/git diff --check 통과. 제품 로직 변경 없어 Gradle 재실행 생략; 직전 전체 958개 테스트 통과.
- 유지한 계약: 운영/IAM/외부 publisher 설정 변경 없음, 비밀값 미기록, 사용자 변경 보존. 예상 밖 변경 없음.
- 결정사항: 사용자에게 기존 값의 접두사 추가 저장 인계. 새 비밀키 생성 불필요.
- 위험 요소: 정상 health/JWKS 및 회원 인증 검증 미완료.
- 다음 작업: 사용자 저장 후 task:3 재기동과 상태 검증.

## 2026-09-23 — 사용자 재저장 후 keyring 형식 재확인

- 브랜치: develop.
- 작업 목표: 키 형식 보완 확인 후 재기동 여부 판단.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Secret 저장 성공 표시 확인 후 화면 재진입/조회. ELIGIBILITY 키 항목은 여전히 Base64 단독, IDENTITY 항목은 버전/상태 포함 구조임을 값 비출력 검사로 확인. 비밀값 화면 닫고 인계.
- 실행한 테스트와 결과: UI 형식 검사에서 ELIGIBILITY 접두사 누락 확인. 제품 변경 없어 Gradle 미실행. git diff --check 수행.
- 유지한 계약: AWS 설정/Secret/서비스 실행 수 변경 없음. 비밀값 미기록 및 기존 수정 보존.
- 결정사항: 형식이 맞기 전 서버를 재기동하지 않음. 정확한 ELIGIBILITY 항목 편집 재안내.
- 위험 요소: 테스트 서버 중지 유지, health/JWKS 검증 미완료. 예상 밖 변경 없음.
- 다음 작업: 사용자 ELIGIBILITY 값 접두사 추가 저장 후 검증과 task:3 재기동.

## 2026-09-23 — keyring 재확인 작업 식별 기록

<!-- codex-turn:01a0cdb0-59de-7e23-8903-8fa69fb5a7a2 -->

- 브랜치: develop.
- 작업 목표: 사용자 재저장 결과 및 기동 보류 상태 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 재조회한 ELIGIBILITY 항목의 버전/상태 접두사 누락을 비밀값 출력 없이 확인하고 정확한 수정 항목 안내.
- 테스트와 결과: UI 형식 검사에서 접두사 누락 확인. 문서만 변경하여 Gradle 미실행, git diff --check 통과.
- 유지한 계약: Secret/운영/서비스 설정 변경 없음. 비밀값 미기록, 사용자 변경 보존.
- 결정사항: 형식 보완 전 재기동하지 않음.
- 위험 요소: 정상 health/JWKS 검증 미완료. 예상 밖 변경 없음.
- 다음 작업: 사용자 ELIGIBILITY 값 보완 저장 후 task:3 재기동 검증.

## 2026-09-23 — 저장 결과 이의에 따른 최신 Secret 재조회

- 브랜치: develop.
- 작업 목표: 사용자 저장 완료와 관측 불일치 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 편집 dialog 없음 확인 후 페이지 전체 reload 및 Secret 값 재조회. 대상 ELIGIBILITY cell 형식만 검사해 쉼표 구분 필드 1개 확인(요구 3개). 값 출력 없이 비밀값 화면 닫고 인계.
- 테스트와 결과: UI read-only 재조회 및 형식 검사 수행. 제품 변경 없어 Gradle 미실행, git diff --check 수행.
- 유지한 계약: Secret/서버/운영 변경 없음. 비밀값 비기록, 기존 수정 보존.
- 결정사항: 저장 대상/필드 일치 여부 확인 필요. 캐시 또는 사용자 실수로 원인을 단정하지 않음.
- 위험 요소: 정상 기동 검증 미완료, 실제 값 변경 원인 미확인. 예상 밖 변경 없음.
- 다음 작업: 사용자와 대상 Secret/ELIGIBILITY 필드 확인 후 형식 정상 시 재기동.

## 2026-09-23 — 최신 Secret 재조회 작업 식별 기록

<!-- codex-turn:01a0cdb2-6518-7e00-9096-ff3d03896df6 -->

- 브랜치: develop.
- 작업 목표: 사용자 저장 결과와 관측 불일치 재확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Secret 페이지 전체 새로고침 후 ELIGIBILITY 항목 형식만 검사, 쉼표 구분 필드 1개 확인. 사용자에게 편집 대상 확인 요청. 비밀값 화면 닫음.
- 테스트와 결과: 읽기 전용 UI 재조회 수행, git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: Secret/서버/운영 변경 없음, 비밀값 비기록, 기존 변경 보존.
- 결정사항: 원인을 단정하지 않고 사용자 편집 화면에서 대상 필드 확인 예정.
- 위험 요소: 저장 관측 불일치 원인 및 정상 서버 기동 미확인. 예상 밖 변경 없음.
- 다음 작업: 대상 필드 확인 및 형식 정상 확인 후 test:3 재기동.

## 2026-09-23 — 테스트 Identity 최초 정상 기동 확인

- 브랜치: develop.
- 작업 목표: 사용자 수정 형식 확인 후 테스트 서버 재기동 및 기본 정상 응답 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md, docs/contracts/identity-branch-deployment.md.
- 수행 내용: Secret 값 비출력 검사에서 keyring 3필드/정상 버전/ACTIVE_WRITE/Base64 32바이트 확인. Secret 편집 없이 기존 test:3 desired1 적용. running1/pending0, CloudWatch 시작 완료(19:03 KST) 및 Mongo 트랜잭션 지원 확인 로그 확인.
- 실행한 테스트와 결과: TLS 검증 유지 HTTPS health200/UP, JWKS RSA/RS256/공개 kid 정상 확인. 초기화 중502/503 이후 정상 전환. 문서/운영 기동만 변경하여 Gradle 재실행 생략, 직전 전체958개 테스트 통과. git diff --check 수행.
- 유지한 계약: 동일 이미지88ff5bed/테스트 task:3, 운영/IAM/Secret 미변경, 외부 publisher OFF 유지. 비밀값 비출력/비기록, 기존 사용자 변경 보존.
- 결정사항: 테스트 서비스1개 실행 유지. 앱 인증 연동 검증 단계로 진행 가능.
- 위험 요소: 앱 Google/phone 가입/로그인/재발급 및 실제 rollback 미검증. CI 기존 실패 이력은 남아 있음. 예상 밖 파일 변경 없음.
- 다음 작업: 프론트 테스트 base URL 안내 및 실제 MEMBER 인증 흐름/LC 연동 검증. 로컬 설정/기록 변경은 사용자 commit/push 대상, Jira 변경 없음.

## 2026-09-23 — 테스트 서버 정상 기동 작업 식별 기록

<!-- codex-turn:01a0cdb6-1084-7162-be93-903d3d595ccf -->

- 브랜치: develop.
- 작업 목표: 사용자 keyring 보완 확인 및 테스트 서버 정상 기동 결과 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md, docs/contracts/identity-branch-deployment.md.
- 수행 내용: 비밀값 비출력 형식 검증 통과 후 test:3 desired1 기동. running1/pending0 및 CloudWatch 시작 완료/Mongo 트랜잭션 지원 확인 로그 확인.
- 테스트와 결과: HTTPS health200/UP, JWKS RS256 공개키 메타데이터 확인. git diff --check 통과. 제품 코드 변경 없어 Gradle 재실행 생략.
- 유지한 계약: 운영/IAM/Secret 변경 없음, 기존 배포 이미지 및 외부 publisher OFF 유지. 비밀값 미기록, 기존 변경 보존.
- 결정사항: 테스트 서비스1개 실행 유지, 앱 인증 검증 단계로 인계.
- 위험 요소: 실제 가입/로그인/재발급 및 rollback 미검증, 기존 CI health 실패 이력 유지. 예상 밖 변경 없음.
- 다음 작업: Android Google/phone 연결 및 MEMBER 가입/로그인/재발급 검증.

## 2026-09-23 — Learning Core용 JWT 및 배포 검증 상태 인계

<!-- codex-turn:01a0cdbc-58c1-7d40-907e-47144abe8ae9 -->

- 브랜치: develop.
- 작업 목표: issuer/JWKS/kid/commit/인증 및 claim 검증 상태 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: AWS test:3 JSON의 issuer/audience/image/release 재확인. HTTPS JWKS에서 공개 kid 및 RS256 확인. git show로 배포 commit의 JwtAccessTokenIssuer를 확인해 UUID 검증/subject/account_type/LC 및 Billing audience 구성 확인. 기존 claim 테스트 assertion 확인.
- 테스트와 결과: 실제 JWKS 조회 정상. 직전 전체958개 테스트 통과 이력과 실제 인증 E2E 미검증을 구분. 이번 코드 변경 없어 Gradle 미실행, git diff --check 수행.
- 유지한 계약: 읽기 전용 조회, 운영/테스트 설정 변경 없음, 실제 토큰/비밀키 비기록. 기존 변경 보존.
- 결정사항: issuer는 실제 배포 설정값 확인이며 실제 발급 토큰의 iss 검증 완료로 표현하지 않음. 가입/로그인/재발급 성공은 아직 미검증.
- 위험 요소: 실제 MEMBER 발급 및 재발급 claim/서명 E2E 검증 필요. 예상 밖 변경 없음.
- 다음 작업: Android Firebase 인증 후 테스트 가입/로그인/재발급 성공과 발급 JWT 계약 검증. Jira 변경 없음.

## 2026-09-23 — 비용 절감을 위한 테스트 서비스 중지

<!-- codex-turn:01a0cdc7-7a37-72f3-bbe3-9a987688a90c -->

- 브랜치: develop.
- 작업 목표: 사용자 요청에 따라 다음 테스트까지 테스트 Identity 실행 중지.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: AWS 테스트 서비스 test:3의 desired count1→0 변경 및 업데이트 성공 확인. 운영/DB/Secret/ALB/배포 revision은 변경하지 않음.
- 테스트와 결과: AWS UI 상태 확인, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 설정과 데이터 보존, 운영 서비스 미변경, 비밀값 미기록. 예상 밖 파일 변경 없음.
- 결정사항: 재개 시 desired1 적용 후 health/JWKS 확인. 별도 리소스 삭제 없음.
- 위험 요소: 중지 동안 테스트 API 접근 불가. ALB/Secret/로그 등 잔여 비용 가능, 서비스0 상태 배포 workflow health 실패 가능.
- 다음 작업: 사용자 테스트 재개 요청 시 기동 후 앱 인증 E2E 검증. Jira 변경 없음.
- 최종 검증: 서비스 새로고침 후 desired0/running0/pending0 확인 완료. 테스트 태스크 종료 확인.

## 2026-09-27 — 구·신 서버 분리 및 업데이트 유예 계획 전달 이력 확인

<!-- codex-turn:01a0e309-e393-78c3-b4f6-520be76424ca -->

- 날짜/브랜치: 2026-09-27, develop. 이번 요청의 Jira 지정 없음.
- 작업 목표: 사용자 제시 세 가지 전환 계획의 과거 전달 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md (기록만).
- 수행 내용: 2026-09-22 구버전 주소 분리·웹뷰 안내·1주 유예 검토 기록 및 guest-app-update-transition-review.md를 확인했다. 구·신 서버 분리, 피드백 진입 전 안내, 사용량에 따른 유예 조정 계획이 이미 기록되어 있으며 후속 피드백 result.updateRequired 계약도 확인했다.
- 실행한 테스트와 결과: 저장소 기록 검색/원문 대조 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 기존 작업/과거 기록 보존. API·서버·배포·Jira·기능 설정 변경 없음. 비밀정보 미기록.
- 결정사항: 이전에 전달한 계획이라고 답변하되 계획 합의와 구현·배포 완료를 구분한다.
- 위험 요소: 이번에는 실제 Learning Core/웹뷰 구현 및 운영 전환 상태를 확인하지 않음. 예상 밖 변경 없음.
- 다음 작업: 별도 요청이 있으면 해당 계획 기준으로 실제 구현/배포 준비 상태를 점검한다.

## 2026-09-27 — SNS·10초 챌린지 선출시와 무료 모의고사·결제 후속 출시 검토

<!-- codex-turn:01a0e309-e393-78c3-b4f6-520be76424ca -->

- 날짜/브랜치: 2026-09-27, develop. 별도 Jira 지정 없음.
- 작업 목표: SNS 로그인·10초 챌린지를 먼저 출시하고 전화번호당 1회 무료 모의고사와 결제를 후속 업데이트로 분리할 수 있는지 설명한다.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md (분석 기록만).
- 조사 근거: Identity FirebaseSignupService/FirebaseGuestUpgradeService의 전화번호·eligibility hasher 의존성, PhoneEligibilityPublisherConfiguration의 별도 활성화 조건, identity-branch-deployment.md의 내부 binding ON/외부 publisher OFF 구분 확인. Learning Core application.yml/ChallengeController/ChallengeService/ExamServiceImpl을 읽어 독립 challenge flag와 Billing OFF 시 기존 모의고사 생성 분기를 확인했다. Learning Core 파일은 수정하지 않았다.
- 구현 내용: 없음. 단계 출시 방향은 가능하되 배포 검증 완료와 구분하고 전화번호 인증 자체의 후속 연기는 별도 변경임을 안내한다.
- 테스트와 결과: 코드·설정 읽기 대조 및 git diff --check. 코드 변경 없어 Gradle 미실행, 외부 서비스/실제 앱 E2E 미수행.
- 유지한 계약: Identity와 Learning Core/Billing 도메인 경계, 현재 가입 인증·내부 식별 계약, 기존 작업과 기록 보존. 서버·기능 flag·Jira 변경 없음. 비밀정보 미기록.
- 결정사항: 1차 SNS/챌린지 및 기존 모의고사 정책 유지, 2차 무료 자격/결제 동시 도입을 권고한다. Billing OFF만으로 모의고사가 차단되거나 신규 1회 제한이 적용되는 것은 아니다. 사용자 출시 범위 확정 또는 구현 승인으로 간주하지 않는다.
- 위험 요소: 기존 Guest 기록 승계, 앱 전환, 챌린지 AI/저장소/실기기 연동 확인 필요. 2차 도입 시 기존 사용자에게 새 무료 1회 부여 여부와 이전 이용의 소급 계산 정책, 지연 이벤트/기존 전화번호 binding 인계 검증 필요. 예상 밖 변경 없음.
- 다음 작업: 1차 모의고사 정책과 2차 기존 사용자 무료 자격 기준을 확정한 후 별도 출시 체크리스트 및 환경별 기능 설정을 검증한다. 이번 배포/commit/push 없음.

## 2026-09-27 — 단계 출시 검토 작업 식별 기록 보완

<!-- codex-turn:01a0e30b-5569-7513-b978-5cb92ec91400 -->

- 날짜/브랜치: 2026-09-27, develop. Jira 지정 없음.
- 작업 목표: SNS 로그인·10초 챌린지 선출시와 무료 모의고사·결제 후속 출시 검토의 현재 turn 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 앞선 분석은 현재 작업의 결과이며 이전 turn marker로 기록된 것을 과거 기록 수정 없이 새 항목으로 보완했다. 기능 분리 가능성과 현재 가입의 전화번호 인증/내부 식별 의존성, 기존 사용자 무료 자격 정책 필요를 확인했다.
- 테스트와 결과: git diff --check 통과. 분석 및 기록만 변경하여 Gradle 미실행, 실제 앱/운영 E2E 미검증.
- 유지한 계약: 과거 기록과 기존 작업 보존. 코드/API/서버/기능 설정/배포/Jira 변경 없음. 비밀정보 미기록.
- 결정사항: 1차 SNS·챌린지, 2차 무료 자격·결제 적용을 권고했으며 구현 승인이나 출시 준비 완료로 간주하지 않는다.
- 위험 요소: Guest 승계·챌린지 연동·기존 사용자 자격 이행 검증 필요. 예상 밖 변경 없음.
- 다음 작업: 기존 모의고사 정책과 2차 무료 자격 기준 결정 후 출시 검증 범위 확정.

## 2026-09-28 — 테스트 서버 Swagger 사용 조건 확인

<!-- codex-turn:01a0e5b3-b827-74f0-b3a7-be6caf06d2d0 -->

- 브랜치: develop.
- 작업 목표: 테스트 서버 재기동 시 Swagger 사용 가능 여부 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 조사 내용: 저장된 task draft SWAGGER_ENABLED=false, application.yml UI /swagger-ui.html 및 spec /v3/api-docs, SecurityConfig 문서 GET permitAll 확인.
- 테스트와 결과: 설정/코드 읽기 및 git diff --check. 분석/기록만 수행하여 Gradle 미실행. AWS 실시간 상태 조회 없음.
- 유지한 계약: 코드/API/AWS/기능 설정/서비스 기동 변경 없음. 기존 변경 보존 및 비밀정보 미기록.
- 결정사항: 사용하려면 테스트 Swagger 활성 설정 및 재배포/기동 필요. 보호 API는 Identity Access Token 사용, Firebase 인증은 앱 SDK에서 별도 진행.
- 위험 요소: Swagger 활성화 시 공개 문서 접근 가능하므로 접근 제한 검토 필요. 문서/실행 API는 배포 버전 기준. 예상 밖 변경 없음.
- 다음 작업: 사용자 활성화 요청 시 접근 범위를 확정한 뒤 테스트 설정 적용 및 UI/API 검증.

## 2026-09-28 — 테스트 Swagger 활성화 설정 준비

- 브랜치: develop.
- 작업 목표: Learning Core 준비 후 함께 기동할 테스트 Identity의 Swagger 활성화 준비.
- 변경 파일: docs/contracts/identity-test-task-definition.draft.json, src/test/java/web/tosunsaeng/identity/deployment/DeploymentTargetTests.java, docs/contracts/identity-branch-deployment.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 테스트 draft SWAGGER_ENABLED=true, 배포 설정 회귀 assertion 추가. 기존 사용자 변경 보존. AWS 세션 만료 확인 후 ISB 재로그인 페이지 인계.
- 테스트: ./gradlew clean test --no-daemon 실행. 최종 결과는 아래 추가 기록.
- 유지한 계약: 보호 API JWT 인증 유지, 운영/Secret/IAM/실행 수 변경 없음. commit/push 미수행, 비밀값 미기록.
- 결정사항: 테스트 실행은 지금 하지 않음. 로그인 후 현재 AWS revision에 설정만 적용하고 desired0 유지.
- 위험 요소: 실제 ECS 설정 미반영, live Swagger 미검증. 문서 공개 활성화 요청이며 실제 데이터 API는 인증 유지. 기존 미커밋 변경 외 예상 밖 변경 없음.
- 다음 작업: 사용자 AWS 재로그인 후 테스트 설정 적용, 나중에 기동 요청 시 health/UI/spec 검증. Jira 연결 없음.
- 최종 테스트 결과: ./gradlew clean test --no-daemon BUILD SUCCESSFUL(31초), git diff --check 통과. 실제 AWS 반영은 로그인 대기로 미완료.

## 2026-09-28 — Swagger 활성화 준비 작업 식별 기록

<!-- codex-turn:01a0e5b5-3981-76a2-b63d-3e2160a31201 -->

- 브랜치: develop.
- 작업 목표: 테스트 Swagger 활성화 준비 결과와 AWS 반영 대기 상태 기록.
- 변경 파일: docs/contracts/identity-test-task-definition.draft.json, src/test/java/web/tosunsaeng/identity/deployment/DeploymentTargetTests.java, docs/contracts/identity-branch-deployment.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 로컬 테스트 draft의 SWAGGER_ENABLED=true 및 회귀 assertion 추가. AWS 세션 만료로 로그인 페이지 인계.
- 테스트와 결과: ./gradlew clean test --no-daemon 성공, git diff --check 통과.
- 유지한 계약: 운영/Secret/IAM/실행 수 변경 없음, 보호 API JWT 인증 유지. 비밀값 미기록, 기존 변경 보존.
- 결정사항: 사용자 재로그인 후 테스트 설정만 반영하고 desired0 유지. 이번 실제 배포/기동 없음.
- 위험 요소: AWS 반영 및 live Swagger 검증 미완료. 예상 밖 변경 없음.
- 다음 작업: 재로그인 후 최신 테스트 revision 확인 및 Swagger 설정 반영, 이후 사용자 기동 요청 시 정상 응답 검증.

## 2026-09-28 — AWS 테스트 Swagger 설정 실제 적용

<!-- codex-turn:01a0e5bb-8e86-7c92-bf1d-ce1147c7225d -->

- 브랜치: develop.
- 작업 목표: 재로그인 후 테스트 Swagger 활성화 설정을 반영하되 서버 중지 유지.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md, docs/contracts/identity-branch-deployment.md.
- 수행 내용: AWS 현재 테스트 서비스 test:3/desired0 확인. 현재 revision JSON에서 SWAGGER_ENABLED만 true로 변경하여 test:4 생성, 서비스에 적용. 성공 알림 및 test:4/desired0/running0/pending0 확인.
- 테스트와 결과: AWS 저장 및 연결 상태 UI 검증, git diff --check 수행. 이번 제품 코드 변경 없어 Gradle 재실행 생략(직전 clean test 성공). 서버를 켜지 않아 live Swagger/health 미검증.
- 유지한 계약: 이미지88ff5bed/인증/Secret/IAM/네트워크 보존, 운영 미변경. 기존 사용자 변경 보존 및 비밀값 미기록.
- 결정사항: 테스트 Swagger 설정 준비 완료, 사용자 요청 전 기동하지 않음. 자동 기동 예약 없음.
- 위험 요소: Swagger 문서는 기동 후 공개 접근되며 보호 API JWT 인증은 유지. 실제 UI/spec 응답은 미검증. 예상 밖 변경 없음.
- 다음 작업: Learning Core 준비 후 사용자 기동 요청 시 desired1 및 health/JWKS/Swagger 검증. Jira 변경 없음.

## 2026-09-28 — Swagger 활성 테스트 Identity 기동

<!-- codex-turn:01a0e5dc-f4ed-7513-bd4c-ac48de99b1bb -->

- 브랜치: develop.
- 작업 목표: 사용자 요청에 따라 테스트 서버 기동 및 공개 진단 응답 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: test:4 유지하며 테스트 서비스 desired0→1 적용. AWS running1/pending0 확인. 운영 서비스/이미지/Secret/IAM/네트워크 변경 없음.
- 테스트와 결과: 초기502/503 이후 HTTPS health200/UP. Swagger UI 리다이렉트 후200, OpenAPI JSON3.1.0/Identity API/23경로 확인, JWKS RSA/RS256/테스트 kid 확인. git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 사용자 API 인증 유지, 테스트 데이터/설정 보존, 비밀값 미기록. 기존 변경 보존, 예상 밖 파일 변경 없음.
- 결정사항: 테스트 태스크1개 실행 유지, Swagger URL 인계. 실행 중 Fargate 비용 발생.
- 위험 요소: 실제 Google/phone 가입/로그인/재발급 및 Learning Core E2E는 미검증. Swagger UI HTTP/명세 응답만 확인했고 브라우저 Try it out은 실행하지 않음.
- 다음 작업: 프론트 또는 Swagger를 통한 승인된 테스트 계정 인증 검증. Jira 변경 없음.

## 2026-09-28 — Identity·Learning Core 사전 HTTP smoke 및 Postman collection

<!-- codex-turn:01a0e621-9da6-7d80-b98a-3fa688b86cc0 -->

- 브랜치: develop.
- 작업 목표: 앱 연동 전 양쪽 테스트 API 사전 검증.
- 변경 파일: docs/postman/identity-learning-smoke.postman_collection.json, docs/postman/README.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 제공 LC 테스트 주소/계정 미준비 확인. Postman 제어 권한 부재로 동일 HTTP 요청 직접 검사. health/JWKS/인증 거절 테스트와 공개 명세 조회, 비밀값 없는10요청 collection 작성.
- 테스트와 결과: 두 health200/UP, Identity 프로필 무토큰/잘못된 토큰401, exchange 빈 body/잘못된 token401, 잘못된 refresh401, LC today 무토큰/잘못된 token401. JWKS RS256 테스트 kid 정상. collection JSON/10개 요청 확인 및 git diff --check. 제품 코드 변경 없어 Gradle 미실행. Postman runner는 실행하지 않음.
- 유지한 계약: 실제 토큰/비밀값 비기록. 회원 생성/정상 세션 회전/데이터 삭제/설정 변경 없음. 기존 변경 보존, 예상 밖 변경 없음.
- 결정사항: 계정 없는 기본 검사와 인증 성공 E2E를 구분. 임의 MEMBER/custom token 우회 없음.
- 위험 요소: 실제 회원 가입/로그인/refresh/LC 성공 및 GUEST403 미검증. Identity live OpenAPI의 http 서버 주소 관측, HTTPS 명시 사용 및 후속 수정 검토 필요.
- 다음 작업: Firebase SDK 테스트 계정 Google 인증/phone link 준비 후 승인된 테스트 사용자로 MEMBER E2E 진행. Postman 제어 권한 또는 사용자 import 실행 필요. Jira 변경 없음.

## 2026-09-28 — Firebase·Identity 테스트 회원 생성 절차 안내

<!-- codex-turn:01a0e625-5503-7271-a27a-251d2cc3a321 -->

- 브랜치: develop.
- 작업 목표: 앱 연동 전 테스트 계정을 만드는 올바른 순서 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 조사/안내: 기존 프론트 가입 계약 확인. Google SDK 인증, exchange enrollment, 같은 UID phone link, 강제 갱신 후 signup 순서 안내. Firebase 콘솔 테스트 번호 등록과 회원 생성 구분. 최소 SDK 테스트 화면이 별도로 필요함을 명시.
- 테스트와 결과: 계약 문서 읽기 및 git diff --check. 안내/기록만 변경하여 Gradle 미실행.
- 유지한 계약: 계정/데이터/서버 설정 생성·변경 없음. 실제 전화번호/OTP/credential/토큰 비기록.
- 결정사항: 테스트 번호는 실제 SMS 없이 고정 검증 코드로 사용하되 SDK 연결과 가입 필수 정보/정책 동의는 생략하지 않음.
- 위험 요소: 최소 테스트 화면은 아직 미구현, 웹 사용 시 앱 등록/허용 도메인/reCAPTCHA 준비 필요. 실제 MEMBER E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 요청 시 테스트 전용 Firebase SDK 화면 준비 후 인증/가입 성공 경로 검증. Jira 변경 없음.

## 2026-09-28 — 테스트 계정 생성 안내 작업 식별 기록 보완

<!-- codex-turn:01a0e625-c8ec-7c70-a91b-565707c268de -->

- 브랜치: develop.
- 작업 목표: 테스트 회원 생성 절차 안내의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Firebase 테스트 전화번호 등록, Google SDK 로그인, exchange enrollment, 동일 UID phone link, 갱신된 ID Token으로 Identity signup 절차 안내 기록. 앞선 식별자는 현재 hook 제공 식별자로 이 추가 기록에서 보완하며 과거 기록은 유지.
- 테스트와 결과: 문서만 변경하여 Gradle 미실행, git diff --check 수행.
- 유지한 계약: 계정/서버/외부 설정 변경 없음, 비밀값 미기록, 기존 변경 보존.
- 결정사항: SDK 테스트 화면과 실제 회원 생성은 아직 수행하지 않음.
- 위험 요소: MEMBER 인증 E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 요청 시 최소 Firebase SDK 테스트 화면 준비 및 가입 검증.

## 2026-09-28 — Firebase 테스트 전화번호 등록 확인

<!-- codex-turn:01a0e630-f709-7f23-b4ff-a6fe4bca4d3c -->

- 브랜치: develop.
- 작업 목표: 사용자가 열어 둔 Firebase 탭에서 테스트 전화번호 등록 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: to-teacher-firebase 로그인 방법에서 Google/전화 활성화와 테스트 전화번호·인증 코드 저장 항목 1쌍 확인. 콘솔 설정 저장/변경 및 사용자 생성 없음.
- 테스트와 결과: 콘솔 UI 읽기 확인 완료. 문서 기록만 변경하여 Gradle 미실행. git diff --check 수행.
- 유지한 계약: 번호/인증 코드/토큰/credential을 저장소에 기록하지 않음. 기존 변경 보존.
- 결정사항: 테스트 번호 등록 단계 완료, SDK Google 인증과 같은 UID 전화 연결 및 Identity 가입은 별도 진행.
- 위험 요소: 실제 MEMBER E2E 미검증. 열린 콘솔 Spark 표시는 관측 사실이며 실제 결제 연결 상태 재검증은 이번 범위 밖. 예상 밖 코드 변경 없음.
- 다음 작업: 최소 SDK 테스트 화면 또는 앱으로 Google 로그인→exchange→phone link→signup 검증. Jira 변경 없음.

## 2026-09-28 — 로컬 Firebase 웹 테스트 화면 필요성 안내

<!-- codex-turn:01a0e638-bf2e-7b93-b16e-6b9d4b7d43ce -->

- 브랜치: develop.
- 작업 목표: 사용자 직접 구현 필요 여부와 간단한 웹 테스트 방법 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Codex가 최소 로컬 웹 화면을 구현할 수 있으며 공개 배포는 불필요함을 안내. Google 인증→exchange→동일 UID phone link→signup 순서 유지. 웹 앱 등록/허용 도메인/전화 인증 웹 설정/CORS는 확인 필요.
- 테스트와 결과: 안내와 기록만 수행, Gradle 미실행. git diff --check 수행.
- 유지한 계약: 비밀값 비기록, 서비스 계정 개인키를 웹에 넣지 않음, 외부 설정/계정 생성 및 코드 구현 없음.
- 결정사항: 실제 구현은 후속 요청 시 진행. Android 앱 인증 검증을 웹 테스트로 대체하지 않음.
- 위험 요소: 현재 웹 앱 등록/허용 도메인/CORS 미확인, 실제 인증 성공 미검증. 기존 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 승인 시 최소 테스트 화면 구현과 필요한 Firebase 웹 설정 확인. Jira 변경 없음.

## 2026-09-28 — 로컬 Firebase 인증 테스트 화면 구현

<!-- codex-turn:01a0e638-bf2e-7b93-b16e-6b9d4b7d43ce -->

- 브랜치: develop.
- 작업 목표: 앱 프론트 연동 전에 사용자 직접 Google 인증/전화 연결/가입을 진행할 최소 웹 화면 구현.
- 변경 파일: tools/auth-test/index.html, app.js, session.mjs, server.mjs, app.test.mjs, server.test.mjs, README.md 및 docs/codex/WORKLOG.md, CURRENT_STATE.md.
- 구현 내용: Firebase in-memory 인증, 같은 UID phone link, enrollment 만료 검사, 직접 동의 signup, pair 동시 교체, single-flight 재발급과 UUID 요청 ID, 실패 시 이전 refresh 재사용 금지. 프로필/LC today는 읽기 smoke만 제공. 고정 테스트 HTTPS 5개 API만 허용하는 loopback proxy로 운영 CORS 변경 없이 사용. Host/Origin/전용 헤더/본문 크기 제한, redirect 차단, 로그 민감정보 비출력.
- 테스트와 결과: 최초 sandbox의 포트/Gradle 캐시 접근 제한 후 승인된 재실행에서 Node server 2개 및 mocked UI 1개 성공, node --check 성공, ./gradlew clean test BUILD SUCCESSFUL. git diff --check 수행. localhost 서버 실행 완료. Chrome 자동 열기는 ERR_BLOCKED_BY_CLIENT로 차단되어 실제 화면 렌더링 검증 실패, 보호 우회 없음.
- 유지한 계약: Identity 서버 API/보안 설정/운영 배포 미변경. 실제 토큰/키/전화번호/코드를 파일에 저장하지 않음. 계정 생성·실제 로그인·약관 동의는 수행하지 않음.
- 결정사항: 이 도구는 테스트 가상 번호 전용이며 공개 배포하지 않는다. Stage 9 응답 복구/Android SDK 전체 검증은 별도. 서버 세션 폐기 없이 로컬 정보만 지우는 버튼임을 명시.
- 위험 요소: Firebase 웹 앱 구성/localhost 승인 도메인/정책 버전/실제 OAuth 및 MEMBER E2E 미확인. 브라우저 개발자 도구에는 credential이 존재하므로 공유 금지. 기존 dirty 문서/배포 테스트 변경 보존, 이번 예상 밖 변경 없음.
- 다음 작업: 사용자가 로컬 페이지 접근 확인 후 웹 앱 구성과 승인 도메인 준비, 직접 Google 로그인·약관 동의 후 실제 가입 검증. 별도 서비스 배포 불필요. Jira 변경 없음.

## 2026-09-28 — 로컬 인증 테스트 화면 작업 식별 기록 보완

<!-- codex-turn:01a0e639-5ecc-7283-8bf4-92e25670f712 -->

- 브랜치: develop.
- 작업 목표: 이번 로컬 인증 테스트 도구 구현 결과를 현재 작업 식별자로 기록.
- 변경 파일: tools/auth-test의 화면·SDK 흐름·로컬 서버·테스트·README 및 docs/codex/WORKLOG.md, CURRENT_STATE.md.
- 구현 내용: Google 인증→exchange→같은 UID 가상 전화번호 연결→signup, 프로필/재발급/LC 읽기 smoke. 테스트 HTTPS 목적지 제한, loopback 바인딩, Host/Origin 가드, 토큰 메모리 보관 및 비출력.
- 테스트와 결과: Node 3개 테스트, 문법 검사, ./gradlew clean test 및 git diff --check 통과. 로컬 서버 기동 완료. Chrome 자동 열기는 ERR_BLOCKED_BY_CLIENT로 차단되어 실제 렌더링/인증 E2E 미검증.
- 유지한 계약: 운영/API/Firebase 외부 설정 변경 없음. 비밀값 비기록. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- 결정사항: 별도 공개 배포 없이 로컬 전용 사용. 실제 로그인·약관 동의는 사용자가 수행. 이전 기록은 유지하고 작업 식별자만 이 항목으로 보완.
- 위험 요소: 웹 앱 구성/localhost 승인 도메인/정책 버전 확인 및 실제 회원 인증 검증 필요. Stage 9 복구/Android SDK 검증은 별도.
- 다음 작업: 로컬 화면 사용자 접근 확인 후 Firebase 웹 설정 준비. Jira 변경 없음.

## 2026-09-28 — Firebase 웹 구성 위치 및 JSON 입력 안내

<!-- codex-turn:01a0e645-f435-75a1-b0ad-dcd4d36ecd6b -->

- 브랜치: develop.
- 작업 목표: 사용자가 찾지 못한 Firebase 웹 구성 메뉴와 JSON 형식 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 프로젝트 일반 설정→내 앱→웹 앱→SDK 설정 및 구성 경로와 웹 앱 부재 시 등록 방법 안내. Hosting 불필요, 서비스 계정 JSON과 구분. JavaScript 선언을 제외하고 키를 큰따옴표로 감싼 JSON만 입력하도록 안내.
- 테스트와 결과: 문서 안내만 수행하여 Gradle 미실행, git diff --check 확인.
- 유지한 계약: 실제 키/인증정보 미기록, Firebase 설정/계정 생성 없음. 기존 변경 보존.
- 결정사항: 기존 Firebase 프로젝트에서 웹 앱을 사용하며 새 프로젝트 불필요.
- 위험 요소: 웹 앱 현재 등록 여부는 직접 조회하지 않아 미확인. 실제 인증 E2E 미완료. 예상 밖 변경 없음.
- 다음 작업: 사용자 웹 앱 구성 확인 후 로컬 화면 설정 및 승인 도메인 확인. Jira 변경 없음.

## 2026-09-28 — 사용자 제공 Firebase 웹 구성 형식 확인

- 브랜치: develop.
- 작업 목표: 제공된 코드가 로컬 인증 화면에 필요한 웹 구성인지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 웹 appId와 구성 필드를 기준으로 웹 SDK 설정임을 확인. 실제 값 재출력 없이 네 필드의 JSON 입력 방식과 Analytics 불필요 안내.
- 테스트와 결과: 제공된 구조와 로컬 화면 입력 계약 확인, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 실제 키/토큰 비기록, Firebase 설정/인증/배포 변경 없음.
- 결정사항: 기존 로컬 화면에 공개 웹 설정만 입력하며 서비스 계정 사용 금지 유지.
- 위험 요소: 실제 로그인/승인 도메인/E2E 미확인. 기존 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 사용자 JSON 입력 후 Google 로그인 진행 및 결과 확인. Jira 변경 없음.

## 2026-09-28 — Firebase 웹 구성 확인 작업 식별 기록 보완

<!-- codex-turn:01a0e648-0866-7b60-9158-82ec6a24ecb4 -->

- 브랜치: develop.
- 작업 목표: 웹 구성 형식 확인 결과의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 제공 구성이 웹 SDK용임을 확인하고 로컬 화면에 필요한 네 필드 JSON 입력 및 Google 로그인 순서 안내. 실제 값은 기록하지 않음.
- 테스트와 결과: 구조 확인 및 git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 인증/외부 설정/배포 변경 없음, 비밀정보 비기록, 과거 기록과 기존 변경 보존.
- 결정사항: Analytics 초기화와 서비스 계정 JSON은 불필요.
- 위험 요소: 실제 Google 로그인 및 E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 설정 적용 후 로그인 결과 확인. Jira 변경 없음.

## 2026-09-28 — 로컬 Firebase 설정 입력 오류 진단

- 브랜치: develop.
- 작업 목표: 설정 적용 실패 원인 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 지정 localhost 탭에서 Google 버튼 비활성과 일반 오류 확인. 입력 객체의 속성명 큰따옴표 누락/마지막 쉼표가 JSON 구문과 맞지 않음을 확인하여 수정 방법 안내. 실제 값 기록 없음.
- 테스트와 결과: UI 입력 구조와 기존 JSON.parse 구현 대조. 코드 변경 없어 Gradle 미실행, git diff --check 수행.
- 유지한 계약: 외부 인증/설정 변경 및 사용자 입력 수정 없음. 기존 변경 보존.
- 결정사항: 현재 오류는 로그인 이전 JSON 입력 파싱 문제로 판단. 인증 성공 여부와 구분.
- 위험 요소: 일반 오류 문구가 구문 오류를 명확히 설명하지 못함. 실제 로그인 미검증, 예상 밖 변경 없음.
- 다음 작업: JSON 문법 수정 후 설정 적용 및 Google 로그인 확인. Jira 변경 없음.

## 2026-09-28 — 설정 입력 오류 진단 작업 식별 기록 보완

<!-- codex-turn:01a0e64a-15d6-7291-804b-372af08e97a4 -->

- 브랜치: develop.
- 작업 목표: 로컬 Firebase 설정 실패 진단의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: UI에서 JSON 속성명 따옴표 누락과 마지막 쉼표를 확인하여 수정 안내. 실제 설정값은 기록하지 않음.
- 테스트와 결과: 화면과 JSON 입력 계약 대조, git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 입력/인증/외부 설정 변경 없음, 기존 기록과 변경 보존, 비밀값 비기록.
- 결정사항: 로그인 이전 입력 파싱 문제로 진단.
- 위험 요소: 실제 로그인 미검증, 일반 오류 메시지 개선 여지 있음. 예상 밖 변경 없음.
- 다음 작업: 사용자 JSON 수정 후 설정 적용 확인. Jira 변경 없음.

## 2026-09-28 — Google 로그인 완료 여부 확인

- 브랜치: develop.
- 작업 목표: 사용자 로그인 시도 후 완료 상태 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 로컬 화면 결과 auth/popup-closed-by-user와 exchange 등 후속 버튼 비활성 확인. Google 인증 결과가 로컬 앱에 전달되어 완료된 상태가 아님을 안내.
- 테스트와 결과: UI 읽기 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 인증 재시도/계정 생성/외부 설정 변경 없음, 비밀값 비기록, 기존 변경 보존.
- 결정사항: 팝업 완료까지 유지하며 재시도하고 계속 실패하면 일반 Chrome에서 확인 권장.
- 위험 요소: 팝업 종료 원인은 오류 코드만으로 확정 불가. 실제 MEMBER E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: Google 인증 완료 문구와 exchange 버튼 활성화 확인. Jira 변경 없음.

## 2026-09-28 — 로그인 상태 확인 작업 식별 기록 보완

<!-- codex-turn:01a0e64c-9f49-7db2-a549-9c004ac7b6a8 -->

- 브랜치: develop.
- 작업 목표: 로그인 완료 여부 확인의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: auth/popup-closed-by-user 및 후속 버튼 비활성을 확인하여 인증 미완료 안내. 팝업 인증 완료 후 재확인하도록 설명.
- 테스트와 결과: UI 읽기 확인, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, 외부 인증/설정 변경 없음, 과거 기록 및 기존 변경 보존.
- 결정사항: 현재 화면은 Google 인증 완료 결과를 받지 못한 상태로 판단.
- 위험 요소: 팝업 종료의 구체적 원인과 실제 인증 E2E 미확인. 예상 밖 변경 없음.
- 다음 작업: 사용자 재시도 후 Google 인증 완료와 exchange 활성 여부 확인. Jira 변경 없음.

## 2026-09-28 — 반복 Google 팝업 인증 실패 조사

- 브랜치: develop.
- 작업 목표: 반복 로그인 실패의 단계와 설정 오류 가능성 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: IAB 로컬 화면 popup-closed-by-user 및 exchange 비활성 확인. 수집된 warn/error 로그 없음, 남은 OAuth 팝업 없음. Firebase 승인 도메인 UI에서 localhost와 기본 authDomain 등록 확인. 로컬 코드의 popup SDK 호출/완료 분기 확인.
- 테스트와 결과: UI/코드 읽기 진단 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 인증 재시도/설정 수정/계정 생성/배포 없음. 비밀값 비기록 및 기존 변경 보존.
- 결정사항: 승인 도메인 누락은 배제. SDK 팝업 완료 이전 단계 실패로 판단하며 내장 브라우저 연동 문제는 미확정 가설. 일반 Chrome 비교 요청.
- 위험 요소: 오류 코드만으로 실제 팝업 종료 원인은 확정 불가. 수집 로그가 없다는 것이 브라우저 오류 전체 부재를 보증하지 않음. 실제 인증 E2E 미검증, 예상 밖 변경 없음.
- 다음 작업: 동일 설정으로 일반 Chrome 로그인 비교, 실패 시 팝업 화면과 오류 코드 확인. Jira 변경 없음.

## 2026-09-28 — 반복 팝업 실패 조사 작업 식별 기록 보완

<!-- codex-turn:01a0e64d-f3dd-7173-9711-fb6df33043bb -->

- 브랜치: develop.
- 작업 목표: 반복 Google 팝업 실패 조사 결과의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 로컬 화면 popup-closed-by-user 및 exchange 비활성, 수집된 경고/오류 로그 부재, Firebase localhost 승인 도메인 등록 확인. 일반 Chrome 비교를 안내.
- 테스트와 결과: UI·코드 읽기 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀값 비기록, 설정/인증/배포 변경 없음, 과거 기록과 기존 변경 보존.
- 결정사항: 도메인 누락은 배제하고 내장 브라우저 팝업 처리 문제는 미확정 가설로 유지.
- 위험 요소: 실제 팝업 종료 원인과 인증 E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: 일반 Chrome에서 같은 설정으로 로그인 비교 후 원인 추가 확인. Jira 변경 없음.

## 2026-09-28 — Chrome 로그인 성공 비교 결과 설명

- 브랜치: develop.
- 작업 목표: 내장 브라우저 실패와 일반 Chrome 성공의 차이 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 보고로 Chrome 로그인 성공 확인. Firebase popup 인증은 인증 후 원래 페이지로 결과 전달이 필요하며 브라우저별 팝업/창 간 통신/저장소 정책 차이가 영향을 줄 수 있음을 설명. 정확한 하위 원인은 미확정으로 구분.
- 테스트와 결과: 기존 UI 진단 및 사용자 비교 결과 기반 분석, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀값 비기록, 코드/외부 설정/계정 직접 변경 없음. 기존 변경 보존.
- 결정사항: 로컬 웹 인증 테스트는 일반 Chrome에서 계속 진행. Android SDK 실제 검증은 별도.
- 위험 요소: Identity 가입/로그인 교환 및 MEMBER E2E 성공은 아직 확인하지 않음. 예상 밖 변경 없음.
- 다음 작업: Chrome에서 Identity 로그인/가입 준비 결과 확인. Jira 변경 없음.

## 2026-09-28 — Chrome 로그인 비교 작업 식별 기록 보완

<!-- codex-turn:01a0e64f-ec02-77b2-a1aa-ebe93c7345e5 -->

- 브랜치: develop.
- 작업 목표: Chrome 로그인 성공 비교 설명의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 보고에 따른 Chrome 로그인 성공과 내장 브라우저 팝업 실패 차이 설명. 정확한 내부 원인은 미확정으로 유지.
- 테스트와 결과: 사용자 비교 결과 및 기존 UI 진단 기반 분석, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, 외부 설정/인증/배포 변경 없음, 과거 기록과 기존 변경 보존.
- 결정사항: 로컬 웹 테스트는 Chrome에서 진행하며 Google 로그인과 Identity 가입 완료는 구분.
- 위험 요소: Identity exchange/가입 및 MEMBER E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: Chrome에서 Identity 로그인/가입 준비 결과 확인. Jira 변경 없음.

## 2026-09-28 — 테스트 전화번호 국제 형식 안내

<!-- codex-turn:01a0e651-6dce-7960-83cf-2ef04432c83a -->

- 브랜치: develop.
- 작업 목표: 국내 형식 전화번호 입력 거절 이유 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: app.js 국제 번호 정규식 확인. 한국 번호 맨 앞 0 제거 후 국가번호 +82 사용, Firebase 등록 가상 번호 일치 및 고정 코드 사용 안내.
- 테스트와 결과: 입력 검사 코드 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 실제 번호/인증 코드 비기록, 인증/외부 설정 변경 없음, 기존 변경 보존.
- 결정사항: 형식 수정만으로 임의 번호가 테스트 번호로 인정되지는 않음을 구분.
- 위험 요소: 현재 브라우저 오류 자체는 재조회하지 않았으며 실제 phone link 미검증. 예상 밖 변경 없음.
- 다음 작업: 등록된 가상 번호를 국제 형식으로 입력 후 테스트 연결 확인. Jira 변경 없음.

## 2026-09-28 — 국제 번호 안내 작업 식별 기록 보완

<!-- codex-turn:01a0e651-1f41-7a11-95fd-9e79532fbb82 -->

- 브랜치: develop.
- 작업 목표: 국제 번호 입력 안내의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 로컬 입력 정규식 확인 후 국제 형식 변환 및 Firebase 등록 가상 번호 일치 조건 안내. 실제 번호/코드는 기록하지 않음.
- 테스트와 결과: 코드 읽기 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 설정/인증 변경 없음, 기존 기록과 변경 보존.
- 결정사항: 앞선 식별자를 이번 hook 제공 식별자로 보완.
- 위험 요소: 실제 전화 연결 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 국제 형식 입력 후 연결 결과 확인. Jira 변경 없음.

## 2026-09-28 — 테스트 가입 정책 버전 입력 안내

- 브랜치: develop.
- 작업 목표: 테스트 화면 정책 버전과 AWS 설정의 관계 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 테스트 task draft에서 개인정보 privacy-v1, 이용약관 term-v1 확인. 실제 실행 task 환경변수의 동일 값 사용과 약관 확인 후 직접 동의 안내.
- 테스트와 결과: 설정 파일 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 실제 비밀값 비기록, 외부 설정/동의/가입 요청 변경 또는 실행 없음. 기존 변경 보존.
- 결정사항: 가입을 통과시키기 위해 서버 정책 버전을 임의 변경하지 않음.
- 위험 요소: 현재 live ECS 환경변수는 이번에 재조회하지 않음. 예상 밖 변경 없음.
- 다음 작업: 실행 중 task 정책 버전 일치 확인 후 사용자가 가입 진행. Jira 변경 없음.

## 2026-09-28 — 정책 버전 안내 작업 식별 기록 보완

<!-- codex-turn:01a0e653-09c5-7f00-b670-34ebc093fcef -->

- 브랜치: develop.
- 작업 목표: 가입 정책 버전 안내의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 테스트 배포 초안의 정책 버전을 확인하고 실제 실행 task 설정과 일치해야 함을 안내. live AWS 재확인과 구분.
- 테스트와 결과: 설정 파일 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀값 비기록, 외부 설정/가입/동의 실행 없음, 과거 기록과 기존 변경 보존.
- 결정사항: 서버 정책 버전 변경 없이 해당 버전의 약관 확인 후 사용자 직접 동의.
- 위험 요소: 현재 live 설정 미확인. 예상 밖 변경 없음.
- 다음 작업: 실제 실행 task 정책 버전 확인 후 가입 검증. Jira 변경 없음.

## 2026-09-28 — Identity 토큰 수신 성공 결과 해석

- 브랜치: develop.
- 작업 목표: 사용자 제공 토큰 수신 요약의 성공 범위 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 보고와 accept 구현을 대조하여 Access/Refresh pair 수신 및 UUID sub/MEMBER/LC audience 형태 확인 성공 설명. 사용한 버튼은 요약만으로 구분하지 않으며 서버 서명 검증과 구분.
- 테스트와 결과: 로컬 화면 코드 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행. 실서버 인증 성공 정보는 사용자 보고 기반.
- 유지한 계약: 토큰 원문 비기록, 외부 인증/설정 변경 없음, 기존 변경 보존.
- 결정사항: 프로필 인증→재발급→LC 접근을 다음 확인 단계로 안내.
- 위험 요소: 프로필/재발급/LC 실제 성공 및 서명 검증 미확인. 예상 밖 변경 없음.
- 다음 작업: 사용자 보호 API 호출과 재발급 결과 확인. Jira 변경 없음.

## 2026-09-28 — 토큰 수신 결과 해석 작업 식별 기록 보완

<!-- codex-turn:01a0e654-ccb1-7910-9cbc-8c94acb93265 -->

- 브랜치: develop.
- 작업 목표: Identity 토큰 수신 성공 해석의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 보고의 UUID sub/MEMBER/LC audience 검사 성공과 Access/Refresh pair 수신 의미 설명. 토큰 내용 확인과 실제 서버 인증 검증 구분.
- 테스트와 결과: 기존 화면 구현 대조 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 토큰 원문 비기록, 외부 설정/인증 변경 없음, 과거 기록과 기존 변경 보존.
- 결정사항: 프로필→재발급→LC 접근 순서로 후속 확인 안내.
- 위험 요소: 보호 API/재발급/LC 성공은 아직 미확인. 예상 밖 변경 없음.
- 다음 작업: 사용자 후속 검증 결과 확인. Jira 변경 없음.

## 2026-09-28 — 프로필·재발급 성공 및 LC 403 조사

<!-- codex-turn:01a0e655-dfb5-7220-b937-990b836c73ef -->

- 브랜치: develop.
- 작업 목표: 사용자 테스트 결과 해석과 LC 403 원인 후보 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 보고상 프로필 인증/재발급 성공 및 claim 형태 유지 확인. LC 로컬 ChallengeController의 MEMBER 검사와 feature OFF 모두 COMMON403 반환 확인. application 기본값 및 테스트 task template CHALLENGE_ENABLED=false 확인. LC 코드/파일은 읽기만 수행.
- 테스트와 결과: 사용자 결과와 로컬 코드/배포 템플릿 대조, git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 토큰 원문 비기록, 외부 설정/기능 활성화/배포 변경 없음. 기존 변경 보존.
- 결정사항: MEMBER 누락으로 단정하지 않고 실행 중 LC task feature flag부터 확인하도록 안내. AI 및 Day1 등 준비 없이 flag를 켜지 않음.
- 위험 요소: live LC 환경변수/배포 revision 미확인, 따라서 원인 후보이며 확정 아님. LC 인증·챌린지 E2E 성공 미완료. 예상 밖 변경 없음.
- 다음 작업: LC 담당자가 실행 task CHALLENGE_ENABLED와 요청 거절 위치 확인 후 활성 준비 검토. Jira 변경 없음.

## 2026-09-28 — Learning Core 회원 통합 workload 인계 검토

<!-- codex-turn:01a0e6b0-420b-7b93-a0b1-6a67ac69e80f -->

- 브랜치: develop.
- 작업 목표: LC 요청의 workload 신뢰 정보·이벤트 전송 준비 상태를 구현 기준으로 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: WorkloadJwtConfiguration/Properties/CredentialProvider에서 기존 encoder와 kid 공유, 별도 issuer, RS256/고정 subject/목적별 audience/정확히 2분 TTL 확인. UserMerged HTTPS adapter와 outbox 재시도/2xx 성공 처리 확인. JWKS 이전 공개키 병행 지원 및 테스트 draft merge/workload/publisher OFF 확인. OwnerEvent 대체 전송 경로 존재 확인.
- 테스트와 결과: 코드·설정 읽기 검토 및 git diff --check. 코드 변경 없어 Gradle 미실행, live AWS/전송 E2E 미실행.
- 유지한 계약: 로그인 JWT와 workload 용도 분리, 토큰/비밀키 비기록, 운영 및 테스트 기능 활성화 없음, 기존 변경 보존.
- 결정사항: 동일 테스트 issuer/JWKS 사용 가능하나 WORKLOAD_JWT_ISSUER를 명시하고 LC와 일치시켜야 함. 키 교체는 공유 사용자 토큰 최대 TTL/skew/cache 및 구 instance 종료를 고려. publisher 경로를 확인한 뒤 하나의 배포 계획으로 활성화하며 기본 챌린지 검증과 merge E2E를 구분.
- 위험 요소: 현재 공개 kid는 draft 및 이전 관측 기준, live 재확인 필요. 기존 사용자 토큰 차단과 기록 이전/중복 이벤트 E2E 미검증. 실제 전송 준비 완료로 간주하지 않음. 예상 밖 변경 없음.
- 다음 작업: merge 검증 범위 승인 후 실행 task 설정/배포 revision·키·대상 endpoint 확인과 양측 이벤트 E2E 계획 확정. Jira 변경 없음.

## 2026-09-28 — 회원 통합 테스트 준비 순서 안내

- 브랜치: develop.
- 작업 목표: workload 및 UserMerged 연동을 위해 다음에 할 작업을 구체화.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: merge transaction의 capture flag 분기 확인. live 설정 조회, 경로와 신뢰 계약 확정, LC 수신 준비, Identity 활성화, 가상 계정 병합 E2E 순서 안내. 챌린지 기능 활성화와 구분.
- 테스트와 결과: 코드 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 실제 AWS 조회/설정 변경/병합 요청 없음, 비밀값 비기록, 기존 변경 보존.
- 결정사항: 승인 없이 flag를 켜지 않으며 수신 측 준비 후 발행 측 활성화. Billing은 기존 제외 범위 유지.
- 위험 요소: live 기능 상태 및 키/전송 E2E 미확인. 실제 계정 병합은 테스트 계정으로만 별도 승인 후 수행. 예상 밖 변경 없음.
- 다음 작업: 실행 중 테스트 서비스의 비밀 아닌 환경설정과 배포 revision 읽기 확인. Jira 변경 없음.

## 2026-09-28 — 회원 통합 준비 안내 작업 식별 기록 보완

<!-- codex-turn:01a0e6b3-45c0-77c3-b0d9-a59b1f442f40 -->

- 브랜치: develop.
- 작업 목표: 회원 통합 테스트 준비 안내의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 실행 설정 조회→LC 수신 준비→Identity 발행 활성화→테스트 계정 병합 검증 순서 안내. 이벤트 저장/발행 경로 일치와 챌린지 403 대응의 독립성 설명.
- 테스트와 결과: 코드 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀값 비기록, AWS 변경/실제 병합 없음, 과거 기록과 기존 변경 보존.
- 결정사항: 다음 단계는 설정 변경 없는 live 조회이며 활성화와 테스트 병합은 별도 진행.
- 위험 요소: live 배포/기능 상태 및 이벤트 E2E 미확인. 예상 밖 변경 없음.
- 다음 작업: 테스트 서비스 설정·배포 revision 조회. Jira 변경 없음.

## 2026-09-28 — AWS 테스트 Identity 회원 통합 설정 실조회

- 브랜치: develop.
- 작업 목표: 실제 배포 버전과 workload·merge 이벤트 준비 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: AWS ECS 서비스 새로고침 후 test:4 desired1/running1/pending0 확인. 이미지 및 release의 commit 88ff5bedc1ee661ccd38a8a1d2c4dbf3b9f03f2d 확인. task 정의에서 GUEST_MERGE_ENABLED, WORKLOAD_JWT_ENABLED, USER_MERGED_PUBLISHER_ENABLED, OWNER_EVENT_USER_MERGED_CAPTURE_ENABLED, OWNER_EVENT_LEARNING_CORE_USER_MERGED_PUBLISHER_ENABLED, Billing merge publisher 모두 false 확인. workload issuer 및 이벤트 endpoint 항목 없음, 환경 파일 없음. 사용자 issuer/kid/Access TTL PT30M 확인.
- 테스트와 결과: 15:29~15:30 KST 콘솔 읽기 검증 및 git diff --check. 코드 변경 없어 Gradle 미실행, 이벤트 발급/전송 E2E 미수행.
- 유지한 계약: Secret 값 열람/운영 변경/테스트 설정 변경/계정 병합 없음. 비밀정보 비기록 및 기존 변경 보존.
- 결정사항: 현 배포는 로그인 가능 상태와 별개로 회원 통합 및 workload 이벤트 OFF. LC 수신 준비 후 선택한 발행 경로로 활성화 계획 필요.
- 위험 요소: 실제 JWKS 응답 재조회 및 LC 수신 설정/이벤트 E2E는 이번에 검증하지 않음. task 정의 설정 조회이며 컨테이너 내부 런타임 계측은 아님. 예상 밖 변경 없음.
- 다음 작업: LC에 실제 OFF 상태 인계, workload issuer·endpoint와 사용할 이벤트 경로 확정 후 별도 승인으로 테스트 설정 적용. Jira 변경 없음.

## 2026-09-28 — AWS 설정 조회 작업 식별 기록 보완

<!-- codex-turn:01a0e6b4-6101-7c83-b723-450df80551f6 -->

- 브랜치: develop.
- 작업 목표: AWS 테스트 Identity 실조회 결과의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 실행 중 test:4 및 이미지 commit 확인. 회원 통합/workload/두 이벤트 발행 경로 OFF와 workload issuer·endpoint 미설정 확인. 사용자 JWT 공개 메타데이터와 TTL 확인.
- 테스트와 결과: AWS 콘솔 읽기 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀값 조회·기록 없음, AWS 설정 변경/이벤트 발송/병합 없음, 과거 기록과 기존 변경 보존.
- 결정사항: LC 수신 준비 후 선택한 이벤트 경로의 활성화를 별도 승인으로 진행.
- 위험 요소: LC 수신 설정 및 이벤트 E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: LC 인계 및 테스트 이벤트 설정 확정. Jira 변경 없음.

## 2026-09-28 — 회원 통합 이벤트 두 경로 및 전환 의미 설명

<!-- codex-turn:01a0e6c6-bfe9-71e0-b261-cef72303b933 -->

- 브랜치: develop.
- 작업 목표: 발행 경로 선택 의미와 기존 안내의 과도한 단순화 정정.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: merge transaction capture 분기, OwnerEventCaptureService와 requiredConsumers, fanout runbook/Stage7 legacy 전환 계약 확인. 기존 LC 전용 outbox와 신규 Billing/LC fan-out 차이 및 기존 잔량 배출을 위한 publisher 공존 설명.
- 테스트와 결과: 코드/계약 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 신규 merge는 하나의 저장 경로 선택, 과거 outbox 삭제/자동 backfill 없음, 외부 설정 변경 및 비밀값 기록 없음.
- 결정사항: 두 publisher 동시 활성 자체를 금지하는 것이 아니며 capture와 발행 경로 일치·잔량 처리가 핵심이라고 정정. 신규 경로 Billing OFF는 delivery 미생성이 아니라 미전송 pending 보존임을 안내.
- 위험 요소: 테스트 DB 기존 outbox 잔량 미확인, 새 경로 Billing backlog 처리 결정 필요. 기존 변경 보존, 예상 밖 변경 없음.
- 다음 작업: LC-only 테스트 또는 신규 fan-out 검증 범위에 맞춰 저장/발행 설정과 backlog 정책 결정. Jira 변경 없음.

## 2026-09-28 — LC 전용 기존 경로 우선 사용 방향 확인

- 브랜치: develop.
- 작업 목표: 기존 이벤트 경로 우선 검증 후 Billing 연동 시 공통 경로 전환 가능 여부 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 기존 LC 전용 방식으로 이번 테스트 진행 방향 확인. 신규 capture 전환 이후 발생하는 병합부터 공통 이벤트를 생성하며 기존 잔량은 기존 publisher로 처리, 과거 Billing backfill은 자동 수행하지 않음을 안내.
- 테스트와 결과: 앞선 코드/전환 계약 검토 기반 안내 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 설정/배포 변경 없음, 실제 키/토큰 비기록, 과거 이벤트 삭제·변환 없음. 기존 변경 보존.
- 결정사항: 이번 범위는 기존 LC 전용 경로, 공통 경로는 Billing 소비자 준비와 함께 별도 전환 검증.
- 위험 요소: 과거 통합 계정이 Billing 소유권에 미치는 영향은 후속 검토 필요. 운영 확대나 실제 활성화 승인을 의미하지 않음. 예상 밖 변경 없음.
- 다음 작업: LC 수신 준비 후 기존 UserMerged publisher와 workload 테스트 설정 적용 범위 제시. Jira 변경 없음.

## 2026-09-28 — 기존 경로 우선 사용 작업 식별 기록 보완

<!-- codex-turn:01a0e6c8-5816-7a70-839c-6993a4deb8bb -->

- 브랜치: develop.
- 작업 목표: 기존 LC 전용 경로 우선 사용 방향의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 이번 테스트는 기존 UserMerged 경로, Billing 연동 시 OwnerEvent 공통 경로로 전환하는 방향 확인. 기존 미전송 이벤트 처리와 과거 병합의 Billing 이전 별도 검토 안내.
- 테스트와 결과: 기존 코드·계약 검토 기반 안내 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, AWS 설정/배포/이벤트 변경 없음, 과거 기록과 기존 변경 보존.
- 결정사항: 경로 선택만 확인했으며 실제 활성화는 미수행.
- 위험 요소: 전환 전 병합의 Billing 소유권 영향 검토 필요. 예상 밖 변경 없음.
- 다음 작업: LC 수신 준비 후 기존 경로 활성화 설정 범위 제시. Jira 변경 없음.

## 2026-09-28 — 프론트용 테스트 서버·Swagger 주소 전달

<!-- codex-turn:01a0e738-e560-7cc0-8c08-1bc54397c211 -->

- 브랜치: develop.
- 작업 목표: 프론트가 사용할 테스트 Identity 서버 및 Swagger 주소 제공.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 기존 검증된 테스트 HTTPS base URL, Swagger UI 주소 안내. 실제 API HTTPS 사용 및 Swagger HTTP 서버 주소 관측 이력 주의 안내.
- 테스트와 결과: 이전 검증 기록 기반 안내, git diff --check. 코드 변경 없어 Gradle 미실행, live 재조회 없음.
- 유지한 계약: 공개 주소만 공유, 비밀값 비기록, 서버 설정/배포 변경 없음.
- 결정사항: 프론트 인계 주소는 운영 아닌 테스트 환경임을 명시.
- 위험 요소: Swagger Try it out의 HTTP 주소 문제 해결 여부는 미확인. 기존 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 프론트 테스트 환경 주소 적용 후 인증 연동 확인. Jira 변경 없음.

## 2026-09-28 — LC 배포 연계 Identity 준비사항 검토

<!-- codex-turn:01a0e73f-9870-7be3-bbe6-d5c63e1c039d -->

- 브랜치: develop.
- 작업 목표: LC guard 점검/consumer 배포 전후 Identity의 필요 작업 정리.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 기존 publisher 설정과 workload 의존성/TTL 확인. issuer 및 HTTPS endpoint 사전 합의, outbox 상태 점검, LC 정상 기동·인증 준비 확인 후 publisher 활성화, 신규 merge용 flag 별도 필요 안내. 기존 경로 선택에 따라 OwnerEvent/Billing 설정 OFF 유지.
- 테스트와 결과: 코드·설정 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행, AWS/DB live 조회 없음.
- 유지한 계약: 운영/테스트 기능 변경 및 실제 merge/이벤트 발송 없음, 비밀값 비기록, 기존 변경 보존.
- 결정사항: LC 소비자 준비 전에 발행하지 않음. 401/403 등 영구 실패로 dead-letter 전환될 수 있어 무조건 자동 회복을 기대하지 않도록 안내. DB 최종 점검 이후 테스트 트래픽 조율 필요.
- 위험 요소: live LC guard/index 검증과 Identity 기존 outbox 상태 미확인. Guest merge 승인과 테스트 계정 준비 필요. 예상 밖 변경 없음.
- 다음 작업: LC 배포 완료 증빙 수신 및 Identity outbox/설정 확인 후 승인된 테스트 배포 진행. Jira 변경 없음.

## 2026-09-30 — LC 수신 준비 완료 후 추가 체크리스트 안내

<!-- codex-turn:01a0f0a2-847b-75b0-9a48-0487cbaac804 -->

- 브랜치: develop.
- 작업 목표: 기존 UserMerged 발행 연결 외 필요한 활성화·검증 사항 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 전달 LC 준비 완료와 직접 검증을 구분. application 설정 및 기존 publisher 실패 처리 재확인. 신뢰 설정·현 배포·outbox 상태 확인, 신규 merge flag, 테스트 계정 E2E, 실패 시 publisher 중단과 동일 이벤트 복구 필요 안내.
- 테스트와 결과: 코드 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행, AWS/DB live 확인 없음.
- 유지한 계약: 기존 LC 전용 경로 유지, OwnerEvent/Billing 확대 없음, 비밀정보 비기록, 외부 설정 변경/실제 병합 없음.
- 결정사항: 추가 기능 개발 필요성이 확인된 것은 아니며 테스트 설정 반영과 실연동 검증이 남은 단계. 기존 관측 배포 상태를 현재 상태로 단정하지 않음.
- 위험 요소: 과거 pending/dead-letter 및 live issuer/JWKS/revision 미확인, source 차단·target 기록 조회·중복 처리 E2E 미검증. 기존 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 승인 시 최신 테스트 설정과 outbox 조회 후 설정 반영 및 테스트 계정 병합 검증. Jira 변경 없음.

## 2026-09-30 — 테스트 이벤트 연결 착수 및 AWS 재로그인 대기

<!-- codex-turn:01a0f0a2-847b-75b0-9a48-0487cbaac804 -->

- 브랜치: develop.
- 작업 목표: 승인된 테스트 workload/기존 UserMerged 설정 적용 전 최신 상태 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: AWS task 정의 탭에서 로그아웃 상태 확인. 다시 로그인 버튼의 세션 오류 후 기존 ISB 포털을 열어 로그인 화면 인계. 캐시된 task 정의 정보는 live 증빙으로 사용하지 않음.
- 테스트와 결과: 브라우저 로그인 필요 상태 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: Secret 값 조회/설정 저장/배포/이벤트 전송/계정 병합 없음. 기존 변경 보존, 비밀값 비기록.
- 결정사항: 재로그인 후 최신 배포와 outbox/LC 수신 상태를 확인한 뒤 설정 적용 진행.
- 위험 요소: 최신 AWS 설정 및 대기 이벤트 미확인으로 배포 미완료. 예상 밖 변경 없음.
- 다음 작업: 사용자 AWS 로그인 완료 후 사전 조회 및 테스트 전용 설정 적용 재개. Jira 변경 없음.

## 2026-09-30 — AWS 재로그인 대기 작업 식별 기록 보완

<!-- codex-turn:01a0f0a4-8549-7bb2-93f0-56ad0654afb4 -->

- 브랜치: develop.
- 작업 목표: 테스트 이벤트 연결 착수 결과를 현재 작업 식별자로 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: AWS 세션 만료를 확인하고 기존 ISB 로그인 화면을 열어 사용자에게 인계. 이전 캐시 화면은 최신 배포 증빙으로 사용하지 않음.
- 테스트와 결과: 로그인 필요 상태 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀값 비기록, 설정 변경/배포/이벤트 전송/병합 없음, 과거 기록과 기존 변경 보존.
- 결정사항: 사용자 재로그인 후 최신 배포와 대기 이벤트 조회부터 재개.
- 위험 요소: 최신 설정·outbox 미확인으로 실제 배포 미완료. 예상 밖 변경 없음.
- 다음 작업: AWS 로그인 완료 후 테스트 전용 설정 적용 전 사전 확인. Jira 변경 없음.

## 2026-09-30 — AWS 재로그인 후 양측 설정 확인 및 Atlas 로그인 대기

- 브랜치: develop.
- 작업 목표: 테스트 UserMerged 발행 활성화 사전 확인 재개.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 최신 Identity test:4 실행1/보류0·배포 성공 및 관련 플래그 OFF 확인. LC test:11 실행1/보류0·배포 성공, writer/source-deny/consumer true 및 workload issuer·JWKS 테스트 Identity 일치 확인. 테스트 Atlas 접속은 로그아웃되어 로그인 탭 열고 사용자에게 요청.
- 테스트와 결과: AWS 서비스/태스크 정의 UI 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행. LC 실제 인덱스 검사 로그·인증 이벤트 E2E 및 Identity outbox 조회는 미완료.
- 유지한 계약: 설정 변경/새 revision 생성/배포/이벤트 전송/병합 없음. 비밀정보 비기록, 기존 변경 보존.
- 결정사항: 발행 활성화 시 기존 대기 이벤트가 전송될 수 있어 outbox 확인 전 OFF 유지.
- 위험 요소: Atlas 재로그인이 필요하며 대기·실패 이벤트 범위 미확인. 예상 밖 변경 없음.
- 다음 작업: 사용자 Atlas 로그인 후 테스트 Identity outbox 상태 점검, 안전 조건 충족 시 설정 적용 진행. Jira 변경 없음.

## 2026-09-30 — Atlas 테스트 병합 outbox 확인 완료

<!-- codex-turn:01a0f0a6-526b-7781-9bc6-9422b4ccb65e -->

- 브랜치: develop.
- 작업 목표: 테스트 UserMerged 발행 활성화 전 기존 대기 이벤트 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 재로그인 후 테스트 Atlas Cluster0의 to-teacher-identity-test.user_merged_outbox 조회. Documents 0, 전체 조회 0건 및 빈 컬렉션 안내 확인. 사용자 데이터 수정/삭제 없음.
- 테스트와 결과: Atlas UI 실조회 및 git diff --check. 애플리케이션 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 기존 LC 전용 발행 경로, OwnerEvent/Billing OFF, 운영 설정 미변경, 비밀값 비기록. 기존 사용자 변경 보존.
- 결정사항: 기존 전송 대상 없음 확인. workload 발급 활성화는 서비스 간 인증 기능 활성화이므로 브라우저 정책에 따라 적용 직전 사용자 확인 후 진행.
- 위험 요소: 설정 저장/새 revision/배포/이벤트 전송/병합은 아직 미수행. LC 인덱스 및 이벤트 E2E 미검증. 예상 밖 변경 없음.
- 다음 작업: 확인 후 테스트 workload/Guest merge/기존 publisher 활성화 및 배포 안정성 점검. 실제 테스트 계정 병합과 source 차단·target 기록 이전 검증 별도. Jira 변경 없음.

## 2026-09-30 — 테스트 Identity UserMerged 발행 설정 배포 완료

- 브랜치: develop.
- 작업 목표: 승인된 기존 LC 전용 UserMerged 발행 및 workload 인증 기능 테스트 활성화.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 애플리케이션 코드 변경 없음.
- 구현 내용: 적용 직전 확인에 대한 사용자 응답 `dj`를 한국어 키보드의 `어`로 이해한다고 안내 후 진행. 기존 test:4 JSON을 UI에서 보존하여 환경 설정 7개만 수정하고 입력 JSON 동등성 검증 후 test:5 생성. GUEST_MERGE_ENABLED, WORKLOAD_JWT_ENABLED, USER_MERGED_PUBLISHER_ENABLED=true; WORKLOAD_JWT_ISSUER=https://identity-test.to-teacher.com; WORKLOAD_JWT_SUBJECT=identity-service; WORKLOAD_JWT_TTL=PT2M; USER_MERGED_PUBLISHER_ENDPOINT=https://api-test.to-teacher.com/internal/v1/events/user-merged.
- 배포: tosunsaeng-staging-cluster의 tosunsaeng-identity-test-service를 revision5로 업데이트. desired1 유지, 최종 running1/pending0, 배포 성공 및 steady state 확인. 이미지 commit 88ff5bedc1ee661ccd38a8a1d2c4dbf3b9f03f2d 유지.
- 테스트와 결과: UI JSON 검증, ECS 배포 성공/태스크 실패0 관측, CloudWatch에서 2026-09-30T04:58:28Z Started IdentityApplication(35.888초) 확인. 조회된 기동 로그에 ERROR/WARN/Exception 없음. git diff --check 수행. 코드 변경 없어 Gradle 미실행. 실제 이벤트/계정 병합은 수행하지 않음.
- 유지한 계약: 기존 RS256 workload 계약 및 LC 전용 outbox 사용, 기존 이미지·비밀값 참조·역할·네트워크·태스크 수 보존. OwnerEvent/Billing 발행 OFF. 운영/LC 서비스 변경 없음. 비밀값 비기록. 기존 사용자 파일 변경 보존.
- 결정사항: 테스트 연결 설정과 배포 준비 완료로 판단, 실제 이벤트 E2E 성공과 구분.
- 위험 요소: 204 수신/동일 eventId 재시도/중복 방지/source 토큰 차단/target 기록 이전 미검증. 장기 무오류 보장 아님. 다음 코드 배포 시 새 태스크 환경설정 보존 필요. 예상 밖 파일/설정 변경 없음.
- 다음 작업: 사용자가 지정한 테스트 Guest/MEMBER로 병합 E2E 검증. 문제 시 이전 revision4로 서비스 롤백 가능. Jira 변경 없음; 댓글 초안은 테스트 설정 배포 성공, 기록 파일 2개, 실조회 결과 및 E2E 미검증 요약이며 자동 등록하지 않음.

## 2026-09-30 — 테스트 이벤트 배포 작업 식별 기록 보완

<!-- codex-turn:01a0f0ab-6a03-7c03-8881-57cff08326ac -->

- 브랜치: develop.
- 작업 목표: 이번 테스트 UserMerged 활성화·배포 결과에 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 테스트 Identity revision5에 승인된 workload/Guest merge/기존 publisher 설정 적용 및 서비스 배포 완료. 상세 설정과 검증은 바로 앞 작업 항목에 기록. 이번 보완에서 추가 외부 변경 없음.
- 실행한 테스트와 결과: ECS 배포 성공·running1/pending0·steady state와 애플리케이션 기동 완료 로그 확인. git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 기존 LC 전용 발행, RS256 workload 계약, 운영 미변경, OwnerEvent/Billing 발행 OFF, 비밀값 비기록 및 과거 기록 보존.
- 결정사항: 배포 완료와 실제 이벤트 E2E 검증을 구분.
- 위험 요소: 실제 계정 병합·204 응답·중복 방지·source 차단·target 기록 이전은 미검증. 예상 밖 변경 없음.
- 다음 작업: 지정 테스트 계정으로 병합 E2E 검증 및 향후 배포 시 revision5 설정 보존 확인. Jira 변경 없음.

## 2026-09-30 — 로그인·병합·챌린지 통합 테스트 화면 범위 조사

- 브랜치: develop.
- 작업 목표: localhost:4173 기존 화면을 로그인부터 회원 통합·10초 챌린지 검증까지 확장하기 위한 계약 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 테스트 도구 코드 변경 없음.
- 수행 내용: tools/auth-test의 HTML/JS/로컬 게이트웨이/Mock 테스트와 Identity 프론트 Firebase 계약 확인. 별도 Learning Core 저장소의 ten-second-challenge-frontend-api.md를 읽어 클라이언트 호출 순서 확인; Learning Core 소스 복사 없음.
- 구현 사실: 기존 도구는 로그인/가입/전화 연결/프로필/재발급과 today 읽기만 지원. Guest prepare의 MERGE_REQUIRED 뒤 명시 확인과 Guest Bearer/Firebase proof로 merge, 신규 승격은 별도 enrollment/upgrade 계약. 챌린지는 X-Challenge-Date, attempt, M4A/AAC audio/mp4(2MiB), presigned PUT의 Authorization 미전송, 동일 answer Idempotency-Key 보존 및 결과 polling 계약이 필요.
- 테스트와 결과: 파일·계약 읽기 및 git diff --check. 구현 전 범위 확인 단계여서 Gradle/Node 테스트 미실행. 실제 로그인/녹음/업로드/병합/API 호출 없음.
- 유지한 계약: Identity 외 챌린지/S3 코드 추가 금지 규칙 및 비밀값 비기록. 기존 사용자 수정 파일 보존.
- 결정사항: 전체 통합 화면은 Identity 서비스 저장소가 아닌 별도 로컬 테스트 프로젝트로 분리하는 방향을 사용자에게 확인. 기존 localhost 주소 유지 가능하나 기존 프로세스 확인과 재시작 필요.
- 위험 요소: 브라우저 녹음이 M4A/AAC를 지원하지 않을 수 있어 파일 업로드 대안 필요. MEMBER 전용 챌린지 결과는 Guest 기록 이전 검증과 별개이며 기존 시험 기록 이전은 별도 LC API 계약 필요. 구현/E2E 모두 미완료. 예상 밖 변경 없음.
- 다음 작업: 별도 프로젝트 생성 위치·분리 방향 확인 후 안전한 테스트 UI 구현, Mock 테스트 및 실제 화면 QA. Jira 변경 없음.

## 2026-09-30 — 통합 테스트 화면 조사 작업 식별 기록 보완

<!-- codex-turn:01a0f0b0-d01f-7e82-8c4d-6c891389466a -->

- 브랜치: develop.
- 작업 목표: 로그인·병합·챌린지 테스트 화면 조사 결과에 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 기존 로컬 인증 도구와 Identity/LC 클라이언트 계약을 확인하고 별도 프로젝트 분리 방향을 사용자에게 질문. 상세 조사 내용은 바로 앞 항목에 보존.
- 실행한 테스트와 결과: git diff --check 통과. 코드 구현 전 범위 확인 단계여서 Gradle/Node 테스트 미실행.
- 유지한 계약: Identity 도메인 경계, 과거 작업 기록 보존, 비밀정보 비기록. 실제 인증·병합·업로드·서버 재시작 없음.
- 결정사항: 별도 tosunsaeng-integration-test 폴더에서 localhost:4173을 유지하는 방향 확인 대기.
- 위험 요소: 구현 및 실연동 검증 미완료, 브라우저 M4A/AAC 지원과 기존 시험 기록 이전 검증 계약 확인 필요. 예상 밖 변경 없음.
- 다음 작업: 사용자 확인 후 독립 테스트 화면 구현과 Mock/브라우저 검증. Jira 변경 없음.

## 2026-09-30 — 독립 로그인·병합·챌린지 통합 테스트 화면 구현

- 브랜치: develop (Identity 기록만 변경; 독립 도구는 별도 디렉터리).
- 작업 목표: 사용자 승인에 따라 Identity 도메인 경계를 유지하면서 localhost:4173 통합 테스트 화면 제공.
- 변경 파일: Identity의 docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 별도 /Users/msde76/tosunsaeng-integration-test에 index.html, app.js, session.mjs, challenge.mjs, server.mjs, app.test.mjs, server.test.mjs, challenge.test.mjs, merge.test.mjs, README.md 생성. /private/tmp/tosunsaeng-integration-test에서 구현한 뒤 사용자 권한 승인으로 새 영구 폴더에 복사. 기존 Identity 도구/서비스 코드는 미변경.
- 구현 내용: 기존 로컬 인증 도구 기반 독립 클라이언트. Google/phone/signup/exchange/profile/reissue 유지. Guest와 MEMBER 자격을 별도 메모리에 보관하고 신규 테스트 Guest 생성 또는 기존 테스트 Guest 입력 지원. 서버 GUEST 검사·prepare MERGE_REQUIRED·체크박스 및 확인창 후 merge 수행. 응답 유실 자동 재병합 금지. 병합 후 양측 ACCOUNT_MERGED_TOKEN_REJECTED만 성공으로 판단하고 LC 완료 시험 ID 포함 여부 비교. 기존 기록0은 미검증으로 표시.
- 챌린지 구현: 실제 서버 계약의 today→question→attempt→M4A/AAC 파일 또는 최대10초 지원 브라우저 녹음→presigned S3 PUT→answer→결과/최대60초 polling/history. X-Challenge-Date 전달, S3 사용자 Authorization 미전송, 같은 answer key/body 보존 및 최초 제출 시도 이후 녹음/덮어쓰기 잠금. AI failed와 API200 구분. 새 응시 자동 생성 없음. 녹음은 자동 전송하지 않고 사용자 업로드 버튼으로만 전송.
- 보안: localhost/동일 Origin/전용 헤더, 정확한 테스트 호스트·경로 allowlist, redirect 금지, presigned HTTPS S3 호스트 검증, 토큰 메모리 전용·오류 코드 제한 표시. 실제 secret/token을 파일에 저장하지 않음. 개인정보가 포함될 수 있는 발화/피드백과 네트워크 공유 금지 안내.
- 실행한 테스트와 결과: Node Mock 전체6개 통과(로그인/동의/회전, 게이트웨이 origin·host·redirect·body 크기, 토큰 요약, 챌린지 경로/업로드 host, answer 응답유실·같은키·무JWT PUT, merge 확인·source 차단·기록 비교). 루프백 테스트는 최초 sandbox EPERM 후 권한 승인 실행. ./gradlew clean test도 캐시 접근 sandbox 오류 후 승인 재실행 BUILD SUCCESSFUL. 브라우저 localhost 새 화면·초기 비활성 버튼·M4A 지원 안내·화면 렌더 확인. git diff --check 통과.
- 실행 상태: 영구 프로젝트의 Node 서버를 127.0.0.1:4173에 실행, 기존 포트 점유 프로세스 없어 중지/교체 작업 불필요. 기존 내장 브라우저 localhost 탭 새로고침 및 유지. Firebase 인증·실계정 생성·병합·마이크·실음성 업로드는 실행하지 않음.
- 유지한 계약: Identity/LC API 또는 배포 설정 변경 없음, Learning Core 서비스 코드 복사 없음, 독립 클라이언트만 추가. Guest의 챌린지 접근을 가정하지 않음. 기존 사용자 dirty 파일 보존, 예상 밖 변경 없음.
- 결정사항: 신규 Guest 승격은 지원하지 않고 기존 MEMBER 병합만 테스트. Chrome/OS에서 M4A/AAC 녹음 미지원이면 올바른 M4A 파일 사용. 앱의 Android SDK/업데이트·Stage9 응답복구 검증 대체 아님.
- 위험 요소: 실제 회원 인증/병합/AI E2E 및 S3 CORS 미검증. 새 Guest에는 이전 기록이 없어 별도 기록 보유 Guest 필요. 완료 시험 ID 비교는 전체 데이터 이전·이벤트204·중복 재전송 검증을 대신하지 않음. 녹음 파일의 실제 코덱은 AI 검증; 잘못된 파일은 응시 소비 가능. 토큰은 새로고침하면 소실, 서버 세션은 남음.
- 다음 작업: Chrome에서 localhost 열고 공개 웹 Firebase 설정 적용→MEMBER 로그인부터 단계별 실행. 테스트 계정/약관 버전·가상 전화·S3 CORS 확인 후 실제 제출, 이슈는 서버 로그와 함께 진단. 사용자 직접 실행 전 실제 데이터 변경 없음. Jira 변경 없음; 등록용 요약 초안은 독립 도구 구현·Mock6/Gradle 통과·실E2E 미검증이며 자동 등록하지 않음.

## 2026-09-30 — 독립 통합 테스트 구현 작업 식별 기록 보완

<!-- codex-turn:01a0f0b2-c659-78a2-9787-ecd2246ed647 -->

- 브랜치: develop.
- 작업 목표: 이번 독립 테스트 화면 구현·검증 결과에 현재 turn 식별자 추가.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 별도 프로젝트 파일 목록과 구현 상세는 바로 앞 항목에 보존.
- 구현 내용: 독립 프로젝트에서 로그인·전화 연결·가입·재발급, Guest 병합 확인 및 source 차단/완료 시험 비교, 챌린지 음성 제출/AI 결과 조회 화면 제공. localhost:4173 실행 및 브라우저 초기 화면 확인 완료.
- 테스트와 결과: Node Mock6개, ./gradlew clean test, git diff --check 통과. 실제 계정 변경이나 음성 전송 테스트는 미실행.
- 유지한 계약: Identity 서비스 도메인 경계와 기존 외부 API 유지, 운영 설정 미변경, 비밀정보 비기록, 과거 기록과 사용자 수정 보존.
- 결정사항: 도구 준비 완료와 실제 E2E 성공을 구분. 코드/배포 추가 변경 없이 기록 식별자만 보완.
- 위험 요소: S3 CORS·실제 Firebase/병합/AI 연결은 사용자 단계별 검증 필요. 예상 밖 변경 없음.
- 다음 작업: Chrome에서 Firebase 구성 적용 후 MEMBER 로그인부터 검증. Jira 변경 없음.

## 2026-09-30 — 기존 테스트 Google 계정 재사용 안내

<!-- codex-turn:01a0f0c0-c635-7250-8633-bf35607c3e6c -->

- 브랜치: develop.
- 작업 목표: 기존 계정 사용 가능 여부와 신규 계정 필요 조건 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 기존 도구 및 확인한 exchange 계약 기준으로 같은 Firebase 프로젝트/Identity 테스트 DB의 MEMBER는 기존 Google로 인증 후 Identity 로그인하고 phone/signup 생략 가능함을 안내. 병합 source는 별도 Guest이며 챌린지 당일 제한은 초기화되지 않음.
- 테스트와 결과: 문서 안내 작업, git diff --check 수행. 코드 변경/실 API 호출 없어 Gradle·Node 재실행 안 함.
- 유지한 계약: 기존 MEMBER/Guest 분리, 가입과 로그인 구분, 비밀정보 비기록. 실제 계정/서버 변경 없음.
- 결정사항: 새 Google 계정은 필수 아님; 신규 가입 검증용으로만 별도 계정 고려.
- 위험 요소: 현재 계정 존재/당일 챌린지 진행도 live 조회 없음. ENROLLMENT_REQUIRED이면 같은 프로젝트/DB/계정 여부 확인. 예상 밖 변경 없음.
- 다음 작업: 기존 Google 계정으로 Identity 로그인 후 프로필/챌린지 단계 실행. Jira 변경 없음.

## 2026-09-30 — Chrome 통합 테스트 진행 결과 확인

- 브랜치: develop.
- 작업 목표: 사용자가 실행한 localhost:4173 Chrome 결과의 성공 범위 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 지정 Chrome의 기존 탭 읽기. 검증표에서 Google 인증·Identity 로그인·MEMBER 프로필·토큰 재발급·LC today 인증 성공 확인. today의 문제3개 모두 not_started/not_requested와 nextQuestionNumber=1 확인. Guest 생성 CONSENT_REQUIRED와 빈 정책 버전/필수 동의 미선택 확인.
- 테스트와 결과: 실제 화면 관측(별도 네트워크/서버 로그 재검증 아님), git diff --check. 코드 변경 없어 Gradle/Node 재실행 안 함.
- 유지한 계약: 기존 세션 유지를 위해 새로고침 없음. 추가 API 호출·약관 체크·계정 생성·병합·녹음·업로드 없음. 민감 토큰/개인정보 비기록.
- 결정사항: 인증 연동은 화면상 성공, 병합과 음성/AI 흐름은 아직 검증 전. 기존 MEMBER 가입을 반복할 필요 없으나 새 Guest 생성은 해당 Guest의 정책 동의가 별도로 필요함을 안내.
- 위험 요소: 챌린지 문제/음성/AI 및 병합 E2E 미완료. 약관 버전은 현재 배포값 확인 후 사용자 직접 동의 필요. 예상 밖 변경 없음.
- 다음 작업: 챌린지 다음 문제→응시→녹음/업로드→제출→결과 검증. 병합 시험을 원하면 정책 버전/동의를 확인한 뒤 Guest 준비부터 수행. Jira 변경 없음.

## 2026-09-30 — Chrome 결과 확인 작업 식별 기록 보완

<!-- codex-turn:01a0f0c1-b738-7be1-9bee-47f9b41bf1f8 -->

- 브랜치: develop.
- 작업 목표: 이번 Chrome 테스트 진행 결과 조회에 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 기존 Chrome 화면에서 Google/Identity 로그인·MEMBER 프로필·재발급·LC today 성공 및 Guest 생성 CONSENT_REQUIRED 확인. 상세 관측은 바로 앞 기록에 보존.
- 테스트와 결과: 화면 읽기 및 git diff --check 통과. 코드 변경 없어 Gradle/Node 미실행.
- 유지한 계약: 새로고침/추가 API 호출/약관 동의/병합/녹음/업로드 없음, 비밀정보 비기록, 과거 기록 보존.
- 결정사항: 인증 성공과 아직 수행하지 않은 병합·챌린지 제출/AI 검증을 구분하여 안내.
- 위험 요소: 실E2E 미완료, 정책 버전 확인 및 사용자 직접 동의 필요. 예상 밖 변경 없음.
- 다음 작업: 사용자가 챌린지 다음 문제부터 진행하거나 Guest 준비 후 병합을 별도 검증. Jira 변경 없음.

## 2026-09-30 — Chrome 채점 및 Guest 생성 실진단

<!-- codex-turn:01a0f0c6-2340-7b00-9593-40ae75ce986b -->

- 브랜치: develop.
- 작업 목표: 사용자 요청에 따라 Chrome에서 채점 미진행·Guest 생성 문제 확인 및 가능한 직접 테스트.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 기존 Chrome localhost 탭을 새로고침 없이 읽고 녹음/attempt 준비 후 upload 일반 오류·answer 버튼 비활성·결과0건 확인. 문제 번호1로 결과 GET을 직접 실행해 solvedQuestionCount=0/question=null 재확인. 브라우저 수집 error/warn 로그는 빈 목록. 독립 도구의 업로드·오류 처리 코드를 읽어 현재 메시지만으로 CORS/네트워크 원인을 확정할 수 없음을 확인.
- Guest 관측: guestCreate CONSENT_REQUIRED, 정책 버전 입력 비어 있고 필수 동의 미선택. 테스트 Guest 확인 체크와 병합 확인 체크는 필수 정책 동의를 대신하지 않음.
- 테스트와 결과: 실제 LC 결과 조회 성공·제출 이력0 확인, git diff --check. 코드 변경 없어 Gradle/Node 재실행 안 함. AI 채점 실행/실패 여부 자체는 아직 검증 못 함.
- 유지한 계약: 토큰·서명URL·음성 원문 비기록. 기존 세션 보존. 임의 약관 동의/Guest 생성/병합/음성 전송/서버 설정 변경 없음.
- 결정사항: 현재 녹음된 테스트 음성을 테스트 S3에 재업로드하고 LC 제출할지 구체적으로 승인 요청. Guest용 정책 입력·약관 동의는 사용자가 직접 수행하도록 요청. 사용자 응답 후 승인 범위에서 테스트 재개.
- 위험 요소: 업로드 원인 미확정, S3 CORS는 가능성일 뿐 확정 아님. 화면의 일반 오류를 Google 인증 실패나 AI 실패로 단정하지 않음. 예상 밖 변경 없음.
- 다음 작업: 업로드 승인 후 동일 녹음으로 오류 재현/진단 및 성공 시 제출·AI 결과 확인. 사용자 정책 동의 완료 후 Guest 생성 테스트. Jira 변경 없음.

## 2026-09-30 — S3 업로드 CORS 원인 확정 및 정책 버전 입력

<!-- codex-turn:01a0f0c6-2340-7b00-9593-40ae75ce986b -->

- 브랜치: develop.
- 작업 목표: 사용자 승인된 음성 재업로드 테스트와 AWS 정책 버전 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 앱/서버 코드 변경 없음.
- 수행 내용: 사용자 현재 테스트 음성 업로드·제출 허용 수신 후 Chrome 업로드 버튼 재시도, 같은 일반 오류 재현. Identity test:5 환경값에서 PRIVACY_CONSENT_VERSION=privacy-v1, TERM_CONSENT_VERSION=term-v1 확인 후 Chrome 해당 입력란에 입력. 동의 체크/Guest 생성은 미수행. Chrome native 진단은 OS 권한 미부여로 접근하지 못했고 우회하지 않음.
- 진단: AWS LC test:11의 AWS_S3_BUCKET_NAME/region 확인 후 테스트 버킷에 무인증 OPTIONS 요청. localhost:4173 Origin, PUT, content-type 조합에 S3 HTTP403 AccessForbidden 및 CORSResponse: CORS is not enabled for this bucket 응답 확인. 파일 생성/음성/토큰 전송 없는 preflight 검사. 최초 sandbox DNS 오류 후 사용자 승인으로 네트워크 진단 재실행.
- 테스트와 결과: 브라우저 업로드 실패 재현 및 S3 CORS 미설정 확인. 따라서 현재 브라우저 업로드는 차단되며 answer/AI까지 진행하지 못함. git diff --check. 코드 변경 없어 Gradle/Node 재실행 안 함.
- 유지한 계약: AWS 환경·CORS·권한 변경 없음. 동의 대행 없음. 실제 Secret/Token/서명URL 비기록, 기존 세션/녹음 보존. 운영 미변경.
- 결정사항: 테스트 버킷의 localhost:4173 Origin에 PUT 및 content-type만 허용하는 CORS 설정을 사용자 승인 후 적용하는 방향 제안. 공개 읽기/버킷 정책 변경이나 CORS 우회는 하지 않음.
- 위험 요소: CORS 해결 뒤에도 서명/형식/AI 후속 문제가 있을 수 있어 실제 성공은 추가 검증 필요. Guest는 사용자 정책 동의가 필요. 예상 밖 코드 변경 없음.
- 다음 작업: 테스트 버킷 CORS 수정 승인 후 적용·OPTIONS 재검증 및 현재 녹음 업로드/제출/AI 확인. 사용자 약관 체크 완료 후 Guest 생성 테스트. Jira 변경 없음.

## 2026-09-30 — 재인증 화면 보완·CORS 수정 및 실제 AI 채점 완료

<!-- codex-turn:01a0f0c6-2340-7b00-9593-40ae75ce986b -->

- 브랜치: develop.
- 작업 목표: 사용자 요청의 FIREBASE_RECENT_AUTH_REQUIRED 원인 확인 및 업로드 문제와 함께 수정.
- 변경 파일: 독립 /Users/msde76/tosunsaeng-integration-test의 app.js, challenge.mjs, merge.test.mjs, README.md. Identity는 docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md만 갱신. 임시 작업 폴더에서 apply_patch 후 승인된 복사로 영구 프로젝트 반영.
- 원인: FirebaseAdminAuthenticationVerifier가 LOGIN_EXCHANGE 외 목적의 auth_time에 high-risk-max-authentication-age(기본PT5M)를 검사. 도구의 getIdToken(true)는 토큰 갱신이지 Google 재인증이 아님. 기존 Google 버튼은 재인증 전 pair/챌린지를 지워 작업 흐름도 손실시킴.
- 구현: 같은 Firebase UID reauthenticateWithPopup 성공 후 fresh token을 준비하고 MEMBER/Guest/녹음/attempt 보존. recent-auth 오류에 명확한 사용자 재인증 안내와 수동 prepare 재실행 유도. 확정된 인증 거절 시 merge 불명 상태를 해제하되 자동 병합 금지. S3 fetch 실패는 S3_NETWORK_OR_CORS_ERROR로 분리. 서버5분 기준/Token 검증 완화 없음.
- AWS 변경: 사용자 적용 직전 명시 승인 후 테스트 오디오 버킷의 CORS를 AllowedOrigins localhost:4173, AllowedMethods PUT, AllowedHeaders Content-Type, MaxAgeSeconds300으로 저장. 성공 UI 및 무인증 OPTIONS200·정확한 allow headers 확인. 버킷 공개 접근·IAM·운영 버킷 변경 없음.
- 실제 검증: 사용자 사전 승인된 기존 녹음을 Chrome에서 PUT 성공 후 LC answer로 제출. today에서 1번 submitted/processing 확인 뒤 상세 결과 GET에서 solvedQuestionCount1, gradingStatus completed, verdict needs_improvement, transcript/feedback 존재 확인. 음성·발화·피드백 원문은 기록하지 않음. 신규 응시 추가 생성하지 않음.
- 테스트와 결과: Node Mock6개 통과(최근인증 거절→재인증→MEMBER/Guest 및 이전이력 보존 회귀 포함), ./gradlew clean test BUILD SUCCESSFUL, git diff --check. 기존 코드 경고만 확인. 운영 API 호출 없음.
- 현재 사용자 단계: Chrome의 기존 도구를 새로고침하지 않아 Guest 자격 보존. 기존 버전 Google 재인증 버튼 실행 후 Google 계정 선택 popup에서 사용자 입력 대기. 기존 버전 동작상 MEMBER pair/챌린지 화면 상태는 초기화되었으나 서버의 채점 완료 결과와 Guest 상태는 보존. 새 코드 검증은 Mock 기반이며 새 로딩부터 적용.
- 유지한 계약: 기존 RS256/Firebase 계약, S3 JWT 미전송, 사용자 약관 동의 대행/실제 병합 없음. 비밀값 비기록, 기존 dirty 파일 보존. 예상 밖 코드 변경 없음.
- 위험 요소: 실제 재인증 완료·Guest 병합 E2E는 아직 미검증. 수정 코드 적용용 새로고침은 현재 Guest 자격을 소실시키므로 현 시험 종료 후 수행. 기존 음성1건 제출로 해당 문제 응시 소비됨.
- 다음 작업: 사용자 동일 Google 재인증 완료 후 기존 화면에서 Identity exchange/Guest prepare 확인. 실제 merge는 별도 명시 확인 후 진행. 테스트 종료 후 새 화면 로딩해 보완 UX 확인. Jira 변경 없음; 댓글 초안은 독립도구4파일 수정·Mock6/Gradle 성공·실제 채점 완료·병합 미검증 요약이며 자동 등록하지 않음.

## 2026-09-30 — 실제 재인증 후 Guest 병합 준비 성공

<!-- codex-turn:01a0f0c6-2340-7b00-9593-40ae75ce986b -->

- 브랜치: develop.
- 작업 목표: 사용자 Google 재인증 완료 후 최근인증 오류 해소 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Chrome의 Google 인증 완료 표시 확인 후 Identity exchange와 Guest prepare를 순서대로 직접 호출. MEMBER 토큰 형태 요약 및 기존 MEMBER로 병합 가능 안내·병합 버튼 활성 확인. 실제 merge는 누르지 않음.
- 테스트와 결과: 실제 로그인/병합 prepare 성공으로 FIREBASE_RECENT_AUTH_REQUIRED 해소 확인. 현재 Guest 인증으로 prepare 성공했으므로 Guest 준비 정상 확인. git diff --check 통과; 추가 코드 변경 없어 테스트 재실행 없음.
- 유지한 계약: 원문 토큰 비기록, 자동 병합 금지, 서버 최근인증 기준 유지, 사용자 재인증 직접 수행.
- 결정사항: 업로드/실제 AI 채점 및 재인증 후 병합 준비 검증 완료. 최종 계정 병합/이벤트 E2E와는 구분.
- 위험 요소: 실제 merge/source 차단/기록 이전 미실행, 시간 경과 시 재인증 재요구 가능. 예상 밖 변경 없음.
- 다음 작업: 사용자 최종 확인 후 실제 병합과 이벤트 검증. 현재 브라우저는 구버전 스크립트이므로 시험 종료 후 새로고침하여 보완 UX 적용. Jira 변경 없음.

## 2026-09-30 — 재인증·업로드 수정 작업 식별 기록 보완

<!-- codex-turn:01a0f0ce-0fc0-7af1-baad-490e85aff2b7 -->

- 브랜치: develop.
- 작업 목표: 이번 수정 및 실검증 결과에 정확한 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 독립 도구 수정 파일과 AWS 변경 상세는 앞선 항목에 보존.
- 수행 내용: 승인된 localhost 한정 테스트 버킷 CORS 설정 후 실제 음성 업로드·제출·AI 채점 완료 확인. 독립 도구의 같은 UID 재인증 상태 보존/최근인증 오류 안내/S3 오류 분리 수정. 사용자 Google 재인증 후 Identity 로그인 및 Guest 병합 준비 성공 확인.
- 테스트와 결과: Node Mock6개 및 Gradle clean test 성공, CORS OPTIONS200, 실제 채점 completed, Guest prepare 성공. git diff --check 통과.
- 유지한 계약: 서버 최근인증 기준 유지, 운영·IAM·공개 접근 변경 없음, 비밀정보 비기록, 과거 기록 보존. 실제 계정 병합 미실행.
- 결정사항: 현재 브라우저 Guest 보존을 위해 새로고침 보류; 새 도구 코드는 다음 로딩부터 적용.
- 위험 요소: 최종 병합·이벤트 수신·기록 이전은 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 최종 확인 후 실제 병합 E2E 검증. Jira 변경 없음.

## 2026-09-30 — 사용자 병합 후 검증표 해석

- 브랜치: develop.
- 작업 목표: 병합 후 공통 안내 문구가 실패인지 실제 결과를 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 Chrome localhost 검증표 읽기. Identity 병합·Identity source 차단·LC source 차단·LC target 인증 성공 표시 확인. 이전 대상 완료 시험0건 및 완료 시험 이전 미검증 표시 확인. 화면에는 과거 CONSENT_REQUIRED/recent-auth/upload 실패도 이후 성공 행과 함께 남음.
- 테스트와 결과: 현재 화면 실조회 및 git diff --check. 추가 API/병합/설정 변경 없음. 코드 변경 없어 Gradle/Node 재실행 안 함.
- 유지한 계약: 원문 토큰/개인정보 비기록, 기존 세션 보존, 자동 재병합 금지.
- 결정사항: 병합 및 양 서비스의 이전 Guest 차단/대상 MEMBER 접근은 화면상 확인됨. 하단 공통 안내는 오류가 아니며 실제 데이터 이전 검증 완료와는 구분.
- 위험 요소: source에 완료 시험이 없어 기록 이전 확인 불가. 이벤트204/동일event 재전송 처리는 서버 로그/별도 시험 미확인. 예상 밖 변경 없음.
- 다음 작업: 필요 시 발행/수신 상태를 읽기 전용으로 확인하고 기록 보유 테스트 Guest의 이전 검증을 별도 계획. Jira 변경 없음.

## 2026-09-30 — 병합 후 검증표 조회 작업 식별 기록 보완

<!-- codex-turn:01a0f0d5-a5a6-7ad1-906b-614aeab7aad3 -->

- 브랜치: develop.
- 작업 목표: 이번 병합 후 결과 확인에 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Chrome 검증표의 Identity 병합·Identity/LC source 차단·LC target 인증 성공과 이전 대상 기록0건 확인 결과 기록. 하단 문구는 공통 안내이며 과거 실패 행과 현재 성공을 구분해 설명.
- 테스트와 결과: 화면 읽기 및 git diff --check. 코드 변경 없어 Gradle/Node 미실행.
- 유지한 계약: 비밀정보 비기록, 과거 기록 보존, 추가 API/병합/설정 변경 없음.
- 결정사항: 인증 차단 검증 성공과 실제 기록 이전 미검증을 구분.
- 위험 요소: 이벤트204·중복 재전송 및 기록 이전은 별도 검증 필요. 예상 밖 변경 없음.
- 다음 작업: 필요 시 발행/수신 상태 조회와 기록 보유 테스트 Guest의 이전 검증 계획. Jira 변경 없음.

## 2026-09-30 — 통합 테스트 완료 범위 재확인

- 브랜치: develop.
- 작업 목표: 사용자의 전체 검증 완료 여부 질문에 현재 증거와 남은 범위를 구분.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Chrome 검증표 확인 후 병합 후 검증 버튼 및 챌린지1 상세 결과 GET을 직접 재실행. Identity/LC source 병합 전용 차단과 LC target 인증 성공, solved1/gradingStatus completed 및 발화·피드백 존재 확인. 로그인/프로필/재발급/Guest 생성/업로드/제출은 현재 화면의 앞선 성공 기록과 이전 실검증 근거를 사용.
- 테스트와 결과: 현재 인증 상태·채점 결과 읽기 재검증 성공, git diff --check. 코드 변경 없어 Gradle/Node 재실행 없음.
- 유지한 계약: 실제 계정/데이터 추가 변경 없음, 원문 토큰/개인 발화 비기록. 이전 병합·음성 제출 재실행 안 함.
- 결정사항: 기본 정상 흐름 통과이지 모든 예외·출시 준비 검증 완료는 아님. 과거 실패 표시는 이후 성공과 구분.
- 위험 요소: source 완료 시험0건으로 실제 기록 이전 미검증. 이벤트204·중복/재시도·장애복구, 새 가입/전화 연결의 이번 회차 검증, 챌린지2·3 진행/중복·만료·AI실패, Android SDK/운영 전환 검증 남음. 예상 밖 변경 없음.
- 다음 작업: 기록 보유 테스트 Guest 이전과 서버 이벤트 검증을 우선하고 Android 앱 통합/예외 시나리오 수행. Jira 변경 없음.

## 2026-09-30 — 전체 검증 범위 확인 작업 식별 기록 보완

<!-- codex-turn:01a0f0d6-dc0e-7643-bc0f-aa6c386ca867 -->

- 브랜치: develop.
- 작업 목표: 이번 통합 테스트 완료 범위 확인에 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Chrome에서 병합 후 읽기 검증과 1번 결과 조회를 재실행해 양 서비스 source 차단·target 인증 및 채점 completed를 확인. 기본 흐름 성공과 미검증 항목을 구분해 안내.
- 테스트와 결과: 실제 읽기 재검증 성공 및 git diff --check 통과. 코드 변경 없어 Gradle/Node 재실행 안 함.
- 유지한 계약: 비밀정보 비기록, 과거 기록 보존, 추가 계정 변경/병합/음성 제출 없음.
- 결정사항: 프론트 앱 연동 시험 진행 가능하나 운영 출시 검증 완료는 아님.
- 위험 요소: 기록 보유 Guest 이전, 이벤트 중복/장애 복구, 신규 가입/전화 연결 재검증, 나머지 챌린지와 Android 연동 검증 필요. 예상 밖 변경 없음.
- 다음 작업: 남은 데이터 이전 및 앱/예외 시나리오 검증. Jira 변경 없음.

## 2026-09-30 — 소셜 로그인 제공자 준비 상태 읽기 점검

<!-- codex-turn:01a0f0da-5a97-71a2-91b8-9d2abc0cc7f2 -->

- 브랜치: develop.
- 작업 목표: Google Play 서명·Apple·Kakao 등록 준비의 필요성과 현재 상태 확인. 외부 변경 금지.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md (작업 기록만).
- 수행 내용: Firebase Google/Apple/전화 활성화 및 Kakao 부재 확인. Android com.toteacher.app에 사용자가 전달한 SHA-1 두 개·SHA-256 두 개 등록 확인. Apple Service ID·Team ID·Key ID·비공개 키 입력란은 빈 상태로 관측. 코드상 Apple/Kakao 기본 false, Kakao provider 기본 oidc.kakao 확인.
- 테스트와 결과: Firebase 화면 및 application.yml/프론트 계약 읽기 점검. 코드 변경 없어 Gradle 미실행; git diff --check 수행.
- 유지한 계약: Firebase ID Token→Identity 토큰 계약 유지. 비밀정보 비기록, 등록·저장·권한·배포 변경 없음.
- 결정사항: Google Play 앱 서명 인증서 대조→Apple 개발자 설정→Kakao Generic OIDC 준비 순서 권장. 로컬 빌드는 해당 빌드 서명 등록으로 시험 가능하며 Play 서명이 선행 필수는 아님.
- 위험 요소: Play 인증서 실제 일치, Apple Developer 설정, Kakao Developers 앱 유무 및 Identity Platform 업그레이드 상태 미확인. 배포 중 provider 플래그는 이번에 재조회하지 않음. Firebase 활성화만으로 실제 로그인 준비 완료 아님.
- 다음 작업: 승인 후 각 제공자 설정과 Identity 플래그를 준비하고 실제 Android 로그인 검증. 기존 사용자 변경 보존, 예상 밖 수정 없음. Jira 변경 없음.

## 2026-09-30 — Play 앱 서명 인증서 확인 경로 안내

<!-- codex-turn:01a0f0e0-5d2a-7ba2-ad15-d8907cf2168b -->

- 브랜치: develop.
- 작업 목표: Google Play 앱 서명 인증서 지문 조회 위치 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Play Console 앱 선택→앱 무결성→앱 서명에서 앱 서명 키 인증서 SHA-1/SHA-256 확인 후 Firebase Android 앱 지문에 추가하도록 안내. 업로드 키 인증서와 구분.
- 테스트와 결과: 코드 변경 없어 테스트 미실행. git diff --check 수행. 이번에 Play 화면 실조회는 하지 않음.
- 유지한 계약: 기존 개발용 지문 보존, 비밀정보 비기록, 외부 설정 변경 없음.
- 결정사항: Play 배포용 앱 서명 지문을 기존 Firebase 지문과 대조하고 없는 것만 추가.
- 위험 요소: 실제 인증서 일치 여부와 Play 배포 앱 로그인은 아직 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자가 인증서 화면을 확인한 뒤 Firebase 등록 및 Play 설치 빌드 로그인 시험. Jira 변경 없음.

## 2026-09-30 — 변경된 Play 앱 서명 메뉴 실조회

- 브랜치: develop.
- 작업 목표: 사용자 Chrome 탭에서 앱 무결성 메뉴 이전 위치 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 앱 무결성의 이전 안내 확인 후 Google Play로 보호됨→Play 스토어 보호 세부 정보→Play 앱 서명 관리로 이동, 앱 서명 화면 실조회. 사용 중 키와 이전 앱 서명 키가 함께 존재하며 기존 Firebase 등록 지문 중 한 쌍이 업로드 키 인증서임을 확인. 지문 원문은 기록하지 않음.
- 테스트와 결과: 브라우저 읽기 조회, git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 설정·키·인증서 변경 없음, 사용자 탭 유지.
- 결정사항: 앞선 메뉴 안내를 현재 UI 경로로 정정. 업로드 키가 아닌 실제 배포 서명들을 대상으로 Firebase 대조 필요.
- 위험 요소: 키 업그레이드가 있어 현재·이전 배포 서명 범위 확인 필요. 앱 서명 지문 추가와 실로그인은 미수행. 예상 밖 변경 없음.
- 다음 작업: 승인 후 배포 대상 인증서 지문과 Firebase 등록 목록 대조. Jira 변경 없음.

## 2026-09-30 — Play 메뉴 실조회 작업 식별 기록 보완

<!-- codex-turn:01a0f0e2-71ca-79b1-af41-7595febcc93a -->

- 브랜치: develop.
- 작업 목표: 이번 Play Console 조회의 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 앱 무결성 이전 안내와 Google Play로 보호됨→Play 스토어 보호→Play 앱 서명 관리 경로를 실조회하고 사용자 탭을 인증서 화면에 유지.
- 테스트와 결과: 화면 조회 및 git diff --check 성공. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 설정 미변경, 비밀정보 비기록, 과거 작업 기록 보존.
- 결정사항: 최신 메뉴 경로 안내로 정정; 업로드 인증서와 배포 앱 서명 인증서 구분.
- 위험 요소: 현재·이전 앱 서명 인증서의 Firebase 등록 여부와 Play 설치 로그인 미검증. 예상 밖 변경 없음.
- 다음 작업: 승인 후 필요한 배포 인증서 대조 및 등록. Jira 변경 없음.

## 2026-09-30 — 기존 키와 양자 내성 키 선택 안내

- 브랜치: develop.
- 작업 목표: Firebase Google 로그인 지문 등록 시 화면의 키 구분 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 현재 앱 서명 키 내 기존 키의 SHA-1/SHA-256을 우선 대조하도록 안내. 기존 키는 아래 별도 이전 앱 서명 키와 다른 항목이며 키 업그레이드에 따른 이전 배포 인증서도 확인 필요.
- 테스트와 결과: 앞선 실조회 화면을 근거로 설명. 코드 변경 없어 Gradle 미실행; git diff --check 수행.
- 유지한 계약: 키 변경·업그레이드·설정 저장 없음, 비밀정보 비기록.
- 결정사항: 현재 Google 로그인 준비는 기존 방식 앱 서명 지문 대조부터 진행. 양자 내성 인증서의 추가 필요성은 실제 배포 방식과 Firebase 지원 조건 확인 전 단정하지 않음.
- 위험 요소: 기기별 실제 배포 인증서 및 새 암호화 방식 연동은 미검증. 예상 밖 변경 없음.
- 다음 작업: 현재/이전 앱 서명 인증서 대조 후 필요한 지문 등록 및 Play 빌드 로그인 검증. Jira 변경 없음.

## 2026-09-30 — 앱 서명 키 구분 안내 작업 식별 보완

<!-- codex-turn:01a0f0e5-8efd-7fc0-8c64-21897c97fb2b -->

- 브랜치: develop.
- 작업 목표: 기존 키/양자 내성 키 선택 안내에 현재 작업 식별자 추가.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 현재 앱 서명 키의 기존 방식 인증서 지문부터 Firebase와 대조하도록 안내하고, 별도 이전 앱 서명 키 및 양자 내성 키와 구분.
- 테스트와 결과: git diff --check 수행. 코드 변경 없는 안내로 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 외부 설정 미변경.
- 결정사항: 지문 등록과 키 변경은 별개이며 키 업그레이드 작업은 수행하지 않음.
- 위험 요소: 실제 배포 인증서 대조 및 양자 내성 방식의 Firebase 지원 조건 미검증. 예상 밖 변경 없음.
- 다음 작업: 필요한 배포 인증서 대조 후 승인 범위에서 등록 및 로그인 검증. Jira 변경 없음.

## 2026-09-30 — Google 앱 서명 지문 등록 확인 및 Apple 준비 안내

<!-- codex-turn:01a0f0ea-be30-7211-bdd5-a2eafe81f76f -->

- 브랜치: develop.
- 작업 목표: 사용자 추가 Firebase 지문 검증 및 Apple 로그인 설정 순서 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Chrome Firebase Android 앱에서 SHA-1/SHA-256 각 세 개 등록 확인. 새 지문 한 쌍을 Play 현재 앱 서명 키의 기존 방식 인증서 복사값과 대조하여 둘 다 일치 확인. 지문 값 자체는 기록하지 않음. Apple Developer App ID capability→Services ID/웹 반환 주소→Sign in with Apple 키→Firebase provider 구성→Identity 활성화·실검증 순서 안내.
- 테스트와 결과: 실화면 및 공개 인증서 지문 비교 성공. 코드 변경 없어 Gradle 미실행; git diff --check 수행.
- 유지한 계약: Firebase ID Token 기반 Identity 교환 유지, 비밀정보 비기록, 외부 설정 변경 없음.
- 결정사항: 현재 기존 방식 앱 서명 지문 등록 확인 완료와 실제 Play 로그인 완료를 구분. Android/web Apple 로그인을 위해 Services ID 구성 필요.
- 위험 요소: 이전 앱 서명 인증서와 양자 내성 인증서 추가 필요성, Play 실로그인 및 Apple Developer 실제 설정은 미검증. 기존 사용자 변경 보존, 예상 밖 수정 없음.
- 다음 작업: Apple Developer의 실제 앱 식별자에서 Sign in with Apple 상태 확인 후 설정 진행. Jira 변경 없음.

## 2026-09-30 — Apple Identifiers 접근 경로 안내

<!-- codex-turn:01a0f0f0-ef5d-7870-9435-55bd9f5a61b8 -->

- 브랜치: develop.
- 작업 목표: 사용자가 찾지 못한 Apple Developer Identifiers 메뉴 접근 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Identifiers 직접 링크와 App Store Connect와의 사이트 구분 안내. 앱 등록 팀 선택 및 접근 권한 확인 필요성을 설명.
- 테스트와 결과: 안내만 수행, 실제 Apple 화면 미조회. 코드 변경 없어 Gradle 미실행; git diff --check 수행.
- 유지한 계약: 외부 설정·권한 변경 없음, 비밀정보 비기록.
- 결정사항: 직접 링크에서 앱 등록 개발자 계정으로 로그인 후 실제 App ID 확인부터 진행.
- 위험 요소: 현재 Apple 로그인 팀·멤버십·권한 상태 미확인. 예상 밖 수정 없음.
- 다음 작업: 접근 화면 확인 후 Sign in with Apple 구성 점검. Jira 변경 없음.

## 2026-09-30 — Apple 편집 창과 반환 주소 입력 위치 확인

<!-- codex-turn:01a0f0f5-7222-75b1-bcb2-7e6dd5c5a242 -->

- 브랜치: develop.
- 작업 목표: Apple 열린 Edit 창에서 Firebase 반환 주소 입력 위치 검증.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Chrome의 App ID com.toteacher.app 편집 모달 실조회. 열린 창은 Services ID 웹 설정이 아니라 App ID의 Server-to-Server Notification Endpoint임을 확인. Firebase auth handler를 해당 알림 칸에 넣지 않도록 안내. Services ID의 Domains and Subdomains/Return URLs 입력값 및 Primary App ID 선택 절차 안내.
- 테스트와 결과: 브라우저 읽기 확인, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 설정 저장·입력 없음, 알림 수신 주소와 OAuth 반환 주소 분리, 비밀정보 비기록.
- 결정사항: 현재 알림 주소는 입력하지 않고 Services ID 구성 화면에서 Firebase 기본 auth 도메인과 /__/auth/handler를 설정하도록 안내.
- 위험 요소: 현재 App ID 체크의 저장 여부, Services ID 등록 상태 및 Apple 서버 알림 처리 구현 미확인. 예상 밖 수정 없음.
- 다음 작업: Services ID 웹 인증 구성 및 Firebase 연결, 실제 Apple 로그인 검증. Jira 변경 없음.

## 2026-09-30 — Apple 이메일 릴레이 화면과 Services ID 구분

<!-- codex-turn:01a0f0f9-895a-7952-807c-a92a330f3042 -->

- 브랜치: develop.
- 작업 목표: 사용자가 연 Apple 화면이 로그인 반환 주소 설정인지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 현재 Configure Sign in with Apple for Email Communication 및 Register your email sources 모달 실조회. 이메일 릴레이 발신자 등록 화면으로 확인하고 Identifiers의 Services IDs와 구분해 안내.
- 테스트와 결과: 화면 읽기 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 입력·저장 없음, 비밀정보 비기록.
- 결정사항: 이메일 소스 창에 Firebase 로그인 도메인을 입력하지 않고 Identifiers 목록에서 Services IDs 선택 또는 생성으로 진행.
- 위험 요소: Services ID 생성 및 웹 인증 구성 미완료. 예상 밖 수정 없음.
- 다음 작업: 올바른 Services ID 설정 화면에서 Primary App ID 및 Return URLs 확인. Jira 변경 없음.

## 2026-09-30 — Services ID 생성 화면 확인

<!-- codex-turn:01a0f0fd-0000-70b1-9a00-64653be875c5 -->

- 브랜치: develop.
- 작업 목표: 현재 Apple 화면이 올바른 Services ID 등록 화면인지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Register a Services ID 화면과 빈 Description/Identifier 입력란 확인. Firebase용 설명 및 별도 서비스 식별자 예시 안내; 생성 후 Sign in with Apple Configure 단계 설명.
- 테스트와 결과: 브라우저 읽기 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 입력·등록·저장 없음, 비밀정보 비기록.
- 결정사항: 현재 화면은 올바르며 Services ID는 앱 Bundle ID와 별도 식별자로 생성하도록 안내. 제안 식별자의 사용 가능 여부는 등록 시 확인 필요.
- 위험 요소: Services ID 등록과 반환 주소 설정은 아직 미완료. 예상 밖 수정 없음.
- 다음 작업: 사용자 생성 후 웹 인증 설정 확인 및 Firebase 연결. Jira 변경 없음.

## 2026-09-30 — Services ID 입력값 재안내

<!-- codex-turn:01a0f0fe-747f-79b0-a496-56eec2e1ba9f -->

- 브랜치: develop.
- 작업 목표: Services ID 예시를 그대로 입력해도 되는지 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 제안 Description/Identifier 사용 가능하되 식별자 중복 여부는 Apple 등록 시 확인해야 함을 안내. Firebase 서비스 ID에는 생성한 Identifier를 동일하게 입력하고 Bundle ID와 구분.
- 테스트와 결과: 안내만 수행, 코드 변경 없어 Gradle 미실행. git diff --check 수행.
- 유지한 계약: 외부 등록·설정 변경 없음, 비밀정보 비기록.
- 결정사항: 제안 서비스 식별자를 사용하고 Firebase와 정확히 일치시킴.
- 위험 요소: 식별자 가용성과 실제 등록 미확인. 예상 밖 수정 없음.
- 다음 작업: 사용자 등록 후 웹 인증 반환 주소 구성 확인. Jira 변경 없음.

## 2026-09-30 — Services ID 안내 작업 식별 기록 보완

<!-- codex-turn:01a0f0fe-6365-7df2-98cf-1d1b032a2823 -->

- 브랜치: develop.
- 작업 목표: Services ID 입력값 안내의 정확한 현재 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 제안 Description/Identifier를 사용하고 Firebase 서비스 ID와 일치시키도록 안내한 결과 기록. 과거 항목 수정 없이 추가.
- 테스트와 결과: git diff --check 수행. 안내만 수행하여 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, 외부 설정 변경 없음.
- 결정사항: 앱 Bundle ID와 Services ID 구분 유지.
- 위험 요소: 실제 등록 및 식별자 가용성 미확인. 예상 밖 코드 변경 없음.
- 다음 작업: 사용자 등록 후 반환 주소 구성 확인. Jira 변경 없음.

## 2026-09-30 — Services ID 생성 완료 확인 및 웹 인증 설정 안내

- 브랜치: develop.
- 작업 목표: 사용자 생성 완료 확인 및 다음 설정 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Apple Services ID 목록에서 ToTeacher Firebase Login 및 com.toteacher.app.firebase 생성 확인. 편집 화면을 열어 Sign In with Apple 미선택 및 Configure 비활성 확인. 사용자에게 활성화 후 Primary App ID·Firebase 도메인·반환 URL 설정 안내.
- 테스트와 결과: 브라우저 읽기 확인, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 입력·설정 저장 없음, 비밀정보 비기록.
- 결정사항: Services ID 생성은 완료됐으나 웹 로그인 구성은 미완료.
- 위험 요소: 웹 인증 설정 저장 및 Firebase provider 연결·실제 로그인 미검증. 예상 밖 수정 없음.
- 다음 작업: 사용자 웹 인증 구성 후 저장 상태 점검. Jira 변경 없음.

## 2026-09-30 — Services ID 생성 확인 작업 식별 기록 보완

<!-- codex-turn:01a0f0ff-50d2-7750-9df0-efcbfefaf975 -->

- 브랜치: develop.
- 작업 목표: 이번 Services ID 생성 확인의 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Services ID 생성 및 Sign In with Apple 미활성 상태 확인 결과 기록. 웹 인증 구성과 최종 저장 절차 안내.
- 테스트와 결과: git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 외부 설정 저장 없음.
- 결정사항: 생성 완료와 웹 인증 설정 완료를 구분.
- 위험 요소: 웹 구성 저장·Firebase 연결·로그인 검증 미완료. 예상 밖 수정 없음.
- 다음 작업: 사용자 웹 구성 저장 후 확인. Jira 변경 없음.

## 2026-09-30 — Apple 웹 인증 Next 비활성 원인 확인

- 브랜치: develop.
- 작업 목표: Services ID 웹 인증 설정 Next 비활성 원인 진단.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Web Authentication Configuration의 No App ID is available 표시 및 Next disabled 확인. 입력 도메인/반환 URL은 Firebase 기본 auth handler와 일치. 선택 가능한 Primary App ID 부재가 차단 원인임을 안내하고 App ID의 Sign In with Apple primary 구성 및 최종 저장 확인 요청.
- 테스트와 결과: 브라우저 실조회, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 입력·저장·권한 변경 없음, 비밀정보 비기록.
- 결정사항: URL 수정 대신 같은 팀의 Primary App ID 등록 상태 점검 우선. 앞선 체크 상태만으로 저장 완료를 보장하지 않음.
- 위험 요소: Primary 부재의 근본 원인은 미확정; App ID 설정 저장 누락 또는 반영 지연 등 확인 필요. 예상 밖 수정 없음.
- 다음 작업: App ID primary 설정 최종 저장 후 Services ID Configure 재진입 및 선택 가능 여부 확인. Jira 변경 없음.

## 2026-09-30 — Apple Next 비활성 진단 작업 식별 기록 보완

<!-- codex-turn:01a0f100-fed9-77c2-8c77-41f6dc7f8124 -->

- 브랜치: develop.
- 작업 목표: 이번 Apple Next 비활성 진단의 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 올바른 도메인/반환 URL 입력과 No App ID is available 표시 확인 결과 기록. Primary App ID 구성 및 최종 저장 점검 안내.
- 테스트와 결과: git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 외부 설정 변경 없음.
- 결정사항: 주소 수정이 아닌 Primary App ID 가용성 확인이 다음 단계.
- 위험 요소: 저장 누락·반영 지연 등 근본 원인은 미확정. 예상 밖 수정 없음.
- 다음 작업: 사용자 App ID 저장 후 Services ID 선택 목록 재확인. Jira 변경 없음.

## 2026-09-30 — Apple Services ID 웹 인증 저장 확인

- 브랜치: develop.
- 작업 목표: 사용자 저장 후 Primary App ID 및 웹 인증 주소 검증.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Services ID 목록에서 편집 화면을 다시 열어 Sign In with Apple 활성 확인. Configure에서 Primary App ID가 com.toteacher.app으로 선택됨 확인. Website URLs 목록에 Firebase 기본 도메인과 /__/auth/handler 반환 주소 모두 존재 확인. 읽기 확인 후 Cancel로 모달 닫음.
- 테스트와 결과: 브라우저 저장 상태 재조회 성공, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 설정 변경·키 생성 없음, 비밀정보 비기록.
- 결정사항: Primary 부재 문제 해소 및 Services ID 웹 인증 설정 저장 확인 완료. Apple 로그인 전체 완료와는 구분.
- 위험 요소: Apple 로그인용 키·Firebase provider 구성·Identity 활성화·실로그인 미검증. 예상 밖 수정 없음.
- 다음 작업: Apple 로그인용 키 준비 후 Firebase 구성 및 테스트. Jira 변경 없음.

## 2026-09-30 — Apple 웹 인증 저장 확인 작업 식별 기록 보완

<!-- codex-turn:01a0f103-c0cf-7913-918a-32eb41e94a8c -->

- 브랜치: develop.
- 작업 목표: Apple 웹 인증 저장 확인의 현재 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Services ID 재조회에서 로그인 활성, Primary App ID 선택, Firebase 도메인 및 반환 주소 등록을 확인한 결과 기록. 다음 키 준비 절차 안내.
- 테스트와 결과: git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 외부 설정 변경·키 생성 없음.
- 결정사항: Services ID 구성 저장 확인 완료와 실제 로그인 검증 완료를 구분.
- 위험 요소: 로그인용 키·Firebase 연결·Identity 활성화 및 실제 인증 미검증. 예상 밖 수정 없음.
- 다음 작업: Apple 로그인용 키 준비 후 Firebase 연결 검증. Jira 변경 없음.

## 2026-09-30 — Apple 로그인 키 등록 전 구성 확인

- 브랜치: develop.
- 작업 목표: 현재 키 설정 그대로 등록 가능한지 읽기 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 등록 화면에서 로그인용 이름 및 Sign in with Apple만 선택 확인. Edit에서 Primary App ID com.toteacher.app 및 그룹 내 com.toteacher.app.firebase 확인 후 Back으로 복귀. 추가 기능은 미선택 유지.
- 테스트와 결과: 브라우저 구성 확인, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 키 생성·권한 변경·외부 저장 없음, 비밀정보 비기록.
- 결정사항: 현재 선택 구성이 Firebase Apple 로그인 목적과 일치하며 사용자 Continue/Register 후 안전한 키 다운로드 단계로 진행 가능.
- 위험 요소: 키 실제 발급·Firebase 연결·실로그인 미완료. 예상 밖 수정 없음.
- 다음 작업: 사용자 키 발급 후 Firebase provider 설정. Jira 변경 없음.

## 2026-09-30 — Apple 키 구성 확인 작업 식별 기록 보완

<!-- codex-turn:01a0f106-82c6-7590-8c0f-4d1253eeda65 -->

- 브랜치: develop.
- 작업 목표: Apple 키 등록 전 구성 확인의 현재 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Sign in with Apple만 선택되고 Primary App ID 및 Firebase Services ID가 연결된 설정 확인 결과 기록. 사용자 등록·다운로드 절차와 안전한 보관 안내.
- 테스트와 결과: git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 키 생성·외부 설정 변경 없음.
- 결정사항: 확인된 구성이 로그인 목적과 일치함.
- 위험 요소: 실제 키 발급·Firebase 연결·로그인 미검증. 예상 밖 수정 없음.
- 다음 작업: 사용자 발급 후 Firebase 연결 확인. Jira 변경 없음.

## 2026-09-30 — Apple 키 다운로드 후 Firebase 연결 안내

<!-- codex-turn:01a0f107-a3fc-7dd3-b6e0-a66557eb9cce -->

- 브랜치: develop.
- 작업 목표: 사용자 키 다운로드 완료 보고 후 Firebase Apple provider 입력 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Firebase Authentication Apple의 서비스 ID, 팀 ID, 키 ID 및 비공개 키 입력 위치 설명. 사용자 직접 입력과 안전한 파일 보관 안내; 다운로드 파일을 열거나 원문을 수집하지 않음.
- 테스트와 결과: 안내만 수행. 코드 변경 없어 Gradle 미실행; git diff --check 수행.
- 유지한 계약: 비밀정보 비기록, 외부 설정·권한 변경 없음.
- 결정사항: 다운로드 완료는 사용자 보고 기준이며 Firebase 저장 및 실제 로그인 완료와 구분.
- 위험 요소: provider 저장·Identity 활성화·실제 Apple 인증 미검증. 예상 밖 수정 없음.
- 다음 작업: 사용자 Firebase 저장 후 구성 상태 및 테스트 준비 확인. Jira 변경 없음.

## 2026-09-30 — Firebase Apple 연결 안내 작업 식별 기록 보완

<!-- codex-turn:01a0f107-f2b2-7e33-bf9b-02b2eff722a5 -->

- 브랜치: develop.
- 작업 목표: 다운로드 후 Firebase 연결 안내의 정확한 현재 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 사용자 다운로드 완료 보고와 Firebase Apple provider 입력 안내 결과를 기록. 과거 기록은 수정하지 않음.
- 테스트와 결과: git diff --check 수행. 코드 변경 없는 안내로 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, 다운로드 파일 미열람, 외부 설정 변경 없음.
- 결정사항: Firebase 저장과 실제 인증 성공은 아직 확인하지 않음.
- 위험 요소: provider 저장·Identity 활성화·실로그인 미검증. 예상 밖 수정 없음.
- 다음 작업: 사용자 Firebase 저장 후 구성 상태 확인. Jira 변경 없음.

## 2026-09-30 — Firebase Apple provider 저장 상태 확인

<!-- codex-turn:01a0f10b-51f2-7352-a6be-256d9c0119dc -->

- 브랜치: develop.
- 작업 목표: 사용자 등록 후 Firebase Apple 연결 설정 저장 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: Chrome Firebase Apple 활성 상태 및 재개방한 설정에서 서비스 식별자 일치, 팀/키 식별자와 비공개 키 필드 입력 존재 확인. 원문 출력 없이 존재 여부만 조회하고 저장 없이 취소. 서버 설정의 Apple 기본 비활성 확인.
- 테스트와 결과: UI 저장 항목 재조회 성공 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, 외부 설정·배포 변경 없음.
- 결정사항: Firebase 구성 저장 확인 완료이나 키 유효성과 Apple 실제 로그인 성공은 아직 미검증. 배포 환경의 활성 플래그는 별도 확인 필요.
- 위험 요소: 현재 ECS 플래그 미조회, 프론트 Apple 인증 및 Identity 교환 미검증. 예상 밖 수정 없음.
- 다음 작업: 테스트 Identity 활성 설정 확인 후 필요한 변경 승인 및 실제 Apple 로그인 검증. Jira 변경 없음.

## 2026-09-30 — 테스트 Identity Apple 활성 플래그 실조회

- 브랜치: develop.
- 작업 목표: 배포된 테스트 Identity Apple 설정 확인 및 비활성 시 활성화 준비.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: ECS 테스트 서비스가 사용하는 tosunsaeng-identity-test:5에서 FIREBASE_APPLE_ENABLED=false 확인. Firebase 인증/Google/전화 활성 및 Kakao 비활성 확인. 새 개정에서 Apple 플래그만 true로 변경하고 테스트 서비스에 배포할 범위를 안내.
- 테스트와 결과: AWS 실화면 조회, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 기존 이미지·Secret 참조·JWT·병합 이벤트·운영 서비스 유지 예정. 실제 변경 및 배포 없음.
- 결정사항: 새 로그인 제공자 수용은 보안상 인증 허용 범위 변경이므로 브라우저 적용 직전 사용자 확인 요청.
- 위험 요소: 활성화/재배포 및 Apple E2E는 미완료. 예상 밖 수정 없음.
- 다음 작업: 적용 승인 후 Apple 플래그 하나만 변경한 새 테스트 개정 배포 및 안정성 확인. Jira 변경 없음.

## 2026-09-30 — 테스트 Apple 플래그 조회 작업 식별 기록 보완

<!-- codex-turn:01a0f10d-5828-7030-8ee9-c3e92c22eba7 -->

- 브랜치: develop.
- 작업 목표: 테스트 Identity Apple 활성 설정 조회의 현재 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 테스트 task definition test:5에서 Apple 비활성을 실조회한 결과 및 Apple 플래그만 활성화할 적용 직전 승인 대기 상태 기록.
- 테스트와 결과: git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 운영 및 테스트 배포 변경 없음.
- 결정사항: 승인 후 테스트 서비스에만 설정 변경 적용.
- 위험 요소: 활성화·재배포·Apple 실제 로그인 미완료. 예상 밖 수정 없음.
- 다음 작업: 사용자 적용 승인 후 새 테스트 개정 배포와 안정성 확인. Jira 변경 없음.

## 2026-09-30 — 테스트 Identity Apple 로그인 활성화 배포 완료

- 브랜치: develop.
- 작업 목표: 사용자 적용 승인 후 테스트 Identity의 Apple 로그인만 활성화하고 정상 배포 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 서버 코드 및 기존 사용자 변경은 수정하지 않음.
- 수행 내용: AWS 콘솔에서 test:5 기반 새 개정 작성, 폼 입력 전후 비교로 FIREBASE_APPLE_ENABLED false→true 한 항목 변경 확인. tosunsaeng-identity-test:6 생성 후 tosunsaeng-staging-cluster의 tosunsaeng-identity-test-service에 배포. 이미지 commit 88ff5bedc1ee661ccd38a8a1d2c4dbf3b9f03f2d·Secret 참조·JWT·병합 이벤트·로그 설정 유지, 원하는 태스크1 유지. 운영 미변경.
- 테스트와 결과: ECS 배포 성공 및 running1/pending0 확인. 신규 태스크의 2026-09-30T06:48:35Z Started IdentityApplication 로그 확인. 로드밸런서 조회 시 정상2/비정상0(이전 태스크 정리 구간) 확인. HTTPS health 브라우저 직접 조회는 클라이언트 차단으로 미완료. git diff --check 수행. 설정만 배포하여 Gradle 미실행.
- 유지한 계약: Firebase ID Token→Identity 인증 계약 유지, Kakao OFF 및 기존 Google/전화 상태 유지. 비밀정보 비기록. 커밋·push 없음.
- 결정사항: 테스트 Apple 수용 설정 활성화 완료. workflow가 현재 서비스 task definition을 가져오는 구조임을 확인했으며 다음 배포 시 revision6 설정 유지 필요. 초기 task definition 초안은 과거 구성으로 자동 배포 입력이 아니므로 수정하지 않음.
- 위험 요소: Apple 실제 인증·가입/로그인·토큰 발급 E2E 미실행. 배포 성공과 사용자 인증 성공을 구분. 기존 미커밋 변경 보존, 예상 밖 로컬 변경 없음.
- 다음 작업: 프론트 또는 테스트 도구에서 실제 Apple 로그인 후 Firebase 인증 및 Identity 교환 검증. Jira 변경 없음.

## 2026-09-30 — 테스트 Apple 활성화 배포 작업 식별 기록 보완

<!-- codex-turn:01a0f10f-31d8-7c73-be2e-e6c742812e36 -->

- 브랜치: develop.
- 작업 목표: 이번 테스트 Apple 활성화 배포의 정확한 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 승인된 Apple 플래그만 활성화한 테스트 revision6 배포 및 정상 기동 확인 결과 기록. 과거 항목은 변경하지 않음.
- 테스트와 결과: ECS 배포 성공/running1/pending0 및 신규 시작 로그 확인 결과 유지. 기록 보완 후 git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 운영·이미지·Secret·다른 설정 유지, 비밀정보 비기록. 이번 보완에서 추가 외부 변경 없음.
- 결정사항: 테스트 배포 완료와 실제 Apple 인증 검증을 구분.
- 위험 요소: Apple 로그인 및 Identity 토큰 발급 E2E 미실행, 직접 HTTPS health 조회 미완료. 예상 밖 수정 없음.
- 다음 작업: 실제 Apple 인증 및 Identity 교환 검증. Jira 변경 없음.

## 2026-09-30 — 로컬 통합 테스트 화면 Apple 로그인 추가

<!-- codex-turn:01a0f119-e71e-7620-b3ef-121870f401e5 -->

- 브랜치: develop. Jira: 이번 요청에 연결된 이슈 없음, Jira 변경 없음.
- 작업 목표: 기존 로컬 테스트 화면에서 Apple 로그인부터 기존 회원가입·병합·챌린지 흐름을 수동 검증할 수 있도록 확장.
- 변경 파일: 별도 프로젝트 `/Users/msde76/tosunsaeng-integration-test`의 app.js, index.html, apple.test.mjs, README.md 및 Identity docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Firebase Apple OAuthProvider와 email/name scope, Apple 로그인/동일 UID 재인증, 제공자별 안내 및 Google/Apple 전환 방지 추가. 명시적 로컬 초기화 후 제공자 전환. 동일 계정 재인증은 Guest/MEMBER/녹음 상태 유지. 기존 테스트 기능 유지.
- 실행한 테스트와 결과: Node app/apple/merge/challenge/server 테스트 7개 통과(최초 sandbox 포트 권한 오류 후 승인된 재실행). Identity ./gradlew clean test 통과(캐시 권한 승인 후). git diff --check 통과. 127.0.0.1:4173 실행 확인. 새 Chrome 탭 UI 조회는 클라이언트 접근 차단으로 미완료, 기존 인증 탭 새로고침하지 않음.
- 유지한 계약: Identity API·JWT·보안 정책·서버 코드·배포 미변경, LC/챌린지 테스트 코드는 별도 프로젝트 유지. 비밀키 입력 및 토큰 원문 표시 없음. 커밋/push 없음.
- 결정사항: 자동 제공자 연결/병합하지 않음. Apple 실제 로그인은 사용자 인증 후 수동 검증. 새 화면 로딩 시 메모리 세션 초기화 주의.
- 위험 요소: Mock 통과는 실제 Apple OAuth 성공을 의미하지 않음. 신규 Apple 계정에 기존 Firebase 계정의 전화번호를 연결하면 충돌 가능. 브라우저 화면 QA 및 실제 Android/iOS 로그인 미확인. 기존 미커밋 사용자 변경 보존, 예상 밖 수정 없음.
- 다음 작업: Chrome에서 localhost:4173 새 화면을 열고 Firebase 초기화→Apple 인증→Identity 교환, 신규 계정은 전화번호/동의/가입, 기존 MEMBER는 프로필/재발급 확인. 배포 전 서버 Apple 플래그 유지 및 실제 앱 SDK 흐름 별도 검증.

## 2026-09-30 — Apple 테스트 PROVIDER_RELINK_REQUIRED 원인 조사

<!-- codex-turn:01a0f125-d451-7be2-becd-0b382bed97af -->

- 브랜치: develop. Jira: 요청에 연결된 키 없음, 변경 없음.
- 작업 목표: 사용자가 보고한 HTTP 409 PROVIDER_RELINK_REQUIRED 의미와 대응 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. FirebaseExchangeService.ensureNoSocialIdentityOwner 및 ProviderChangeGuard.validatePrincipal/authenticate에서 기존 바인딩의 로그인 제공자 승인 누락 또는 차단 시 발생함을 확인. 로컬 테스트 앱은 signInWithPopup/reauthenticateWithPopup만 지원하며 공통 provider link 기능은 미구현임을 확인.
- 실행한 테스트와 결과: 소스 및 frontend-firebase-auth-integration-guide 공통 link 계약 정적 조회, git diff --check. 실행 코드 변경 없는 분석으로 자동 테스트 미실행.
- 유지한 계약: Firebase 인증과 Identity 로그인 제공자 승인을 구분, prepare/start/SDK link/대상 재인증/complete 필수 흐름 유지. DB·Firebase 계정·배포·토큰·보호 설정 미변경.
- 결정사항: 코드상 가능한 원인과 실제 계정 원인 확정을 구분. 기존 승인 SNS로 인증 후 정식 연결 필요하며 자동 계정 삭제·강제 승인하지 않음.
- 위험 요소: 실제 발생 요청 경로·배포 버전·계정 승인 및 차단 상태 미조회. 현재 오류만으로 과거 연결 해제를 단정할 수 없음. 기존 사용자 변경 보존, 예상 밖 수정 없음.
- 다음 작업: 실패 API/계정 연결 상태를 확인하고 필요 시 사용자 요청에 따라 테스트 화면에 공통 SNS 연결 기능 구현. 비밀정보 공유 불필요.

## 2026-09-30 — 새 Apple 계정 가입 테스트 안내

- 브랜치: develop. Jira: 연결된 이슈 없음.
- 작업 목표: 기존 provider 연결 오류와 독립된 신규 계정 테스트 방법 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 로컬 초기화 후 미연결 Apple 계정으로 로그인→Identity 가입 준비→ENROLLMENT_REQUIRED 확인→미사용 테스트 전화번호 연결 및 동의·가입 안내. 동일 Apple 계정으로 새로 로그인해도 새 서버 계정이 되지 않음을 설명.
- 실행한 테스트와 결과: 직전 소스/계약 확인 결과 기반 안내. 코드 변경 없어 테스트 미실행, git diff --check 수행.
- 유지한 계약: 자동 계정 삭제·Firebase unlink·서버 승인 우회 없음. 로컬 메모리 초기화와 서버 계정 삭제 구분.
- 결정사항: 기존 계정을 보존하며 별도 신규 계정으로 테스트. Apple 이메일 숨기기나 이메일 변경을 새 계정 생성 수단으로 안내하지 않음.
- 위험 요소: 신규 Apple 계정 보유 여부 및 전화번호 중복 상태 미확인. 기존 사용자 변경 보존, 예상 밖 수정 없음.
- 다음 작업: 사용자 직접 Apple 인증 후 enrollment 응답 확인. 외부 설정·배포 변경 없음.

## 2026-09-30 — 신규 Apple 가입 안내 작업 식별 기록 보완

<!-- codex-turn:01a0f127-1fa0-7473-a14d-016fb48cb2e1 -->

- 브랜치: develop.
- 작업 목표: 이번 신규 Apple 계정 가입 안내의 정확한 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 별도 미연결 Apple 계정으로 인증 후 신규 enrollment 및 전화번호·동의·가입 절차를 안내한 결과 기록. 과거 항목 보존.
- 테스트와 결과: git diff --check 수행. 문서 기록만 보완하여 실행 테스트 미실행.
- 유지한 계약: 로컬 초기화는 서버 계정 삭제가 아님. 제공자 연결 승인 우회·외부 상태 변경·비밀정보 기록 없음.
- 결정사항: 같은 Apple 계정 재로그인을 신규 계정 생성으로 취급하지 않음.
- 위험 요소: 실제 신규 가입 미검증, 예상 밖 수정 없음.
- 다음 작업: 사용자 직접 인증 후 신규 가입 응답 확인. Jira 변경 없음.

## 2026-09-30 — 현재 Apple 계정 사용 가능 여부 설명 보완

- 브랜치: develop. Jira: 연결된 이슈 없음.
- 작업 목표: 현재 로그인한 계정을 반드시 교체해야 하는지에 대한 오해 해소.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 안내만 수행. Firebase 인증 성공과 Identity의 provider 승인 상태를 구분하고 별도 Apple 계정은 신규 가입 검증용 선택지임을 명시.
- 테스트와 결과: 앞선 소스 조사 근거 활용. 코드 변경 없어 실행 테스트 미실행, git diff --check 수행.
- 유지한 계약: 현재 로그인 세션·DB·연결 상태 변경 및 보호 조건 우회 없음.
- 결정사항: 현재 계정 상태를 확인하기 전에 다른 계정 사용이나 기존 계정 삭제를 필수 조치로 제시하지 않음.
- 위험 요소: 실제 계정의 승인 누락/차단 원인 미확정. 예상 밖 수정 없음.
- 다음 작업: 현재 계정의 Firebase/Identity 연결 상태를 읽기 전용으로 확인하여 정식 연결 경로 결정.

## 2026-09-30 — 현재 Apple 계정 유지 안내 식별 기록 보완

<!-- codex-turn:01a0f128-0010-7923-bda5-c647bc222d71 -->

- 브랜치: develop.
- 작업 목표: 현재 Apple 계정 사용 안내의 정확한 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 계정 교체가 필수는 아니며 Firebase 인증과 Identity 승인이 별개임을 안내한 결과 기록. 과거 기록 보존.
- 테스트와 결과: git diff --check 수행. 문서 보완만 하므로 실행 테스트 미실행.
- 유지한 계약: 계정 삭제·세션 변경·승인 우회·비밀정보 기록 없음.
- 결정사항: 기존 계정 상태를 보존하고 연결 상태 확인 후 대응.
- 위험 요소: 실제 계정의 승인 누락/차단 원인 미확정, 예상 밖 수정 없음.
- 다음 작업: 현재 계정의 연결 상태 확인. Jira 변경 없음.

## 2026-09-30 — Chrome Apple 로그인 차단 단계 실조회

- 브랜치: develop. Jira: 연결된 이슈 없음.
- 작업 목표: 사용자가 로그인한 실제 테스트 탭에서 차단 위치 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Chrome 기존 localhost 테스트 화면의 검증표에서 Apple 인증 성공과 exchange HTTP 409 PROVIDER_RELINK_REQUIRED 확인. 전화번호 연결/회원가입/프로필/재발급/LC 버튼 비활성 확인. app.js의 exchange 요청이 POST /api/v1/auth/firebase/exchange임을 소스로 대조.
- 테스트와 결과: 기존 탭 접근성 화면 읽기 및 요청 매핑 정적 확인. 새로고침·버튼 클릭·네트워크 재요청 없이 수행. git diff --check 확인. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 현재 인증 메모리·계정·DB·배포 상태 보존. 비밀정보 조회/기록 없음.
- 결정사항: Firebase 인증 성공 이후 Identity 로그인/가입 준비 단계 차단으로 확정. Apple 로그인 자체 실패나 전화번호 단계 실패로 해석하지 않음.
- 위험 요소: 해당 계정의 승인 누락인지 과거 차단인지 화면만으로 구분 불가. 실제 배포 코드/DB 대조 미완료. 예상 밖 수정 없음.
- 다음 작업: 필요 시 테스트 Identity의 Firebase 바인딩·SocialIdentity·provider 차단 상태를 읽기 전용 대조해 원인 확정. 외부 변경 없음.

## 2026-09-30 — Chrome 차단 단계 조회 식별 기록 보완

<!-- codex-turn:01a0f129-008d-79c1-bbd4-07ae4be59530 -->

- 브랜치: develop.
- 작업 목표: 현재 Chrome 테스트 탭 실조회 작업의 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Apple Firebase 인증 성공, Identity exchange HTTP 409 PROVIDER_RELINK_REQUIRED 및 후속 단계 비활성 확인 결과 기록. 과거 항목 보존.
- 테스트와 결과: 화면 읽기 및 소스 요청 매핑 확인 결과 유지. git diff --check 수행. 문서 보완만으로 실행 테스트 미실행.
- 유지한 계약: 탭 새로고침·로그아웃·재요청·계정 변경 없음. 비밀정보 비기록.
- 결정사항: 차단 단계는 확인했으나 실제 계정 승인 누락/차단 원인은 미확정으로 구분.
- 위험 요소: 배포 코드·DB 대조 미완료. 예상 밖 수정 없음.
- 다음 작업: 테스트 계정 연결 상태 읽기 전용 확인. Jira 변경 없음.

## 2026-09-30 — Apple 로그인 승인 불일치 원인 실확인

<!-- codex-turn:01a0f12e-6c76-7680-9a3c-d53f892f18cd -->

- 브랜치: develop. Jira: 요청에 연결된 이슈 없음, 변경 없음.
- 작업 목표: Apple 로그인 후 Identity exchange 409의 실제 데이터 원인 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Atlas 테스트 프로젝트의 to-teacher-identity-test에서 firebase_identities 전체 1건과 social_identities 전체 1건 대조. 동일 계정의 승인 provider는 GOOGLE뿐이고 APPLE 없음. Firebase Authentication 사용자 전체 1건에서 해당 UID에 Google/Apple/Phone 연결 확인. 테스트 DB 전체 21개 컬렉션 목록에 auth_method_change_controls 없음. 이전 확인 배포 commit 88ff5bed의 ProviderChangeGuard와 현재 소스에서 현재 로그인 provider/subject 승인 누락 시 해당 409 반환 확인.
- 테스트와 결과: 실제 Firebase/Atlas 읽기 전용 화면 대조 및 git show 정적 검증, git diff --check. 코드 변경 없어 실행 테스트 미실행. 로그인 탭 새로고침·로그아웃·exchange 재요청 없음.
- 유지한 계약: 계정/DB/인증 제공자/접근 권한/배포 미변경. 개인 식별값·자격증명·토큰은 문서에 기록하지 않음. 운영 DB 미조회.
- 결정사항: 신규 Apple 계정이 아니라 기존 Firebase 계정에 Apple이 연결돼 있고 Identity 승인 정보가 따라오지 않은 상태로 판정. 단순 재로그인이나 계정 교체를 필수 해결책으로 제시하지 않음. 서버 보호 조건 우회/DB 강제 삽입하지 않음.
- 위험 요소: Firebase 연결이 형성된 경위는 미확인. 이미 연결된 Apple을 정식 provider link 흐름으로 승인하는 복구 조건은 추가 검토 필요. 최신 배포 revision 재조회는 미수행. 기존 사용자 변경 보존, 예상 밖 수정 없음.
- 다음 작업: 사용자가 수정을 요청하면 공통 provider link의 기존 Firebase 연결 처리·복구 조건을 확인한 뒤 테스트 화면에 해당 흐름 구현 및 검증. 실제 동의/연결 변경은 별도 확인.

## 2026-09-30 — Apple 인증과 Identity 연결 승인 차이 설명

<!-- codex-turn:01a0f131-3fdb-7839-a620-1b01d178bf1e -->

- 브랜치: develop.
- 작업 목표: 사용자에게 현재 Apple 로그인 차단 이유를 쉬운 용어로 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 앞서 확인한 Firebase Google/Apple/Phone 연결과 Identity GOOGLE만 등록된 상태를 바탕으로 본인 인증과 서비스 계정 로그인 승인을 구분해 설명.
- 테스트와 결과: 기존 실조회 결과 기반 안내, git diff --check 수행. 코드 변경 없어 실행 테스트 미실행.
- 유지한 계약: 계정·세션·DB·외부 설정 변경 및 승인 우회 없음. 비밀정보 비기록.
- 결정사항: 새 Apple 계정 사용이나 기존 계정 삭제가 필수라는 오해 해소. Firebase 연결 생성 경위는 단정하지 않음.
- 위험 요소: 정식 연결 복구 흐름의 기존 Firebase 연결 처리 조건 미검증. 예상 밖 수정 없음.
- 다음 작업: 요청 시 정식 연결 절차 및 테스트 화면 보완 검토. Jira 변경 없음.

## 2026-09-30 — 로그인 승인 설명 작업 식별 기록 보완

<!-- codex-turn:01a0f131-7898-7bf2-8879-4fbe3ec015e4 -->

- 브랜치: develop.
- 작업 목표: 이번 설명 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 Firebase 인증 성공과 Identity 제공자 승인 누락을 설명한 작업의 식별 기록 추가.
- 테스트와 결과: git diff --check 수행. 문서 기록 보완만으로 실행 테스트 미실행.
- 유지한 계약: 계정·세션·DB·설정 미변경, 비밀정보 비기록.
- 결정사항: 기존 계정에 Apple 연결을 원하는지는 사용자 의도 확인이 필요하며 실제 변경하지 않음.
- 위험 요소: 연결 복구 조건 미검증. 예상 밖 수정 없음.
- 다음 작업: 사용자 의도에 맞게 정식 연결 또는 별도 가입 흐름 검토. Jira 변경 없음.

## 2026-09-30 — Apple 전역 활성화와 계정 연결 검사 구분

<!-- codex-turn:01a0f135-a035-71a1-97e4-8737e1098e06 -->

- 브랜치: develop.
- 작업 목표: 서버 Apple 기능을 켰는데도 409가 발생하는 이유를 코드 근거로 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. FirebaseAdminAuthenticationVerifier의 validateProviderPolicy와 후속 ProviderChangeGuard.validatePrincipal 호출, appleEnabled 참조 확인. 전역 기능 활성화가 기존 회원의 SocialIdentity 생성이나 승인 누락 검사를 대체하지 않음을 설명.
- 테스트와 결과: 소스 정적 조회 및 git diff --check. 코드 변경 없는 설명으로 실행 테스트 미실행.
- 유지한 계약: Apple 전역 설정·계정·DB·배포 미변경, 비밀정보 비기록.
- 결정사항: 앞선 허용이라는 표현을 전역 기능 활성화와 계정별 연결 확인으로 명확히 구분. 운영자가 회원마다 수동 허용해야 한다는 의미가 아님.
- 위험 요소: 이미 Firebase에 연결된 제공자 복구 흐름 검증은 남음. 예상 밖 수정 없음.
- 다음 작업: 요청 시 정식 계정 연결 흐름의 현재 데이터 상태 처리 확인 및 테스트 UI 보완. Jira 변경 없음.

## 2026-09-30 — 기존 Firebase Apple 연결의 정식 복구 가능성 조사

- 브랜치: develop. Jira: 연결된 이슈 없음.
- 작업 목표: 전역 Apple 활성화 이후 계정 연결 불일치의 생성 경위와 기존 link API 복구 가능성 점검.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 분석만 수행. ProviderLinkService.prepare에서 원격 연결이 있으면 ownedSocial로 기존 승인 레코드를 필수 조회함을 확인. 현재 원격 APPLE 연결/로컬 APPLE 없음 상태는 link 기능 활성화 시에도 SOCIAL_IDENTITY_CONFLICT 발생 조건. prepare 이전 원격 연결을 소급 승인하지 않는 주석과 start의 원격 연결 거절 조건 확인. 해당 서비스는 이전 확인 배포 commit 88ff5bed와 diff 없음. 로컬 테스트 화면은 Apple signInWithPopup/reauthenticateWithPopup만 수행하고 SNS 연결 호출은 없음; linkWithCredential은 전화번호용.
- 테스트와 결과: 소스 및 배포 commit 비교, git diff --check. 코드 변경 없어 실행 테스트 미실행. 실제 계정 mutation/API 재실행 없음.
- 유지한 계약: 기능 활성화와 계정 연결 검증 분리, 소급 자동 승인·DB 수동 삽입·Firebase 해제 미실행. 비밀정보 비기록.
- 결정사항: 현재 상태는 테스트 UI에 연결 버튼만 추가해 해결할 수 없으므로 앞선 단순화 설명 정정. 연결 생성 경위는 자동 연결 등 가능성만 있으며 확정하지 않음. 안전한 불일치 복구 흐름 검토가 선행되어야 함.
- 위험 요소: Firebase 연결 생성 당시 이벤트/로그 미확보. link 플래그 현재 배포값 미조회, 따라서 실제 prepare HTTP 결과는 미실행 상태. 예상 밖 수정 없음.
- 다음 작업: 사용자 요청 시 기존/대상 SNS 소유권 확인 및 계정 충돌 방지를 포함하는 복구 설계와 테스트 계획 마련. 계정 삭제나 강제 연결 해제로 우회하지 않음.

## 2026-09-30 — Apple 연결 복구 조사 작업 식별 기록 보완

<!-- codex-turn:01a0f136-c432-7520-bb9f-0d575ca063ca -->

- 브랜치: develop.
- 작업 목표: 이번 연결 복구 조사 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 기존 link prepare의 승인 누락 거절 조건 및 UI만으로 복구되지 않는다는 조사 결과의 작업 식별자를 추가.
- 테스트와 결과: git diff --check 수행. 문서 보완만으로 실행 테스트 미실행.
- 유지한 계약: 계정·DB·서버 코드·설정 미변경, 비밀정보 비기록.
- 결정사항: 복구 설계 검토 전 강제 승인 또는 연결 해제하지 않음.
- 위험 요소: Firebase 연결 생성 경위 미확정, 실제 복구 미검증. 예상 밖 수정 없음.
- 다음 작업: 요청 시 안전한 불일치 복구 설계 및 테스트 계획 수립. Jira 변경 없음.

## 2026-09-30 — 다중 SNS 지원과 연결 불일치 구분

<!-- codex-turn:01a0f138-0bc6-7f57-9640-70143b10d2d3 -->

- 브랜치: develop.
- 작업 목표: 다중 SNS 로그인 허용 자체가 충돌 원인인지 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 분석만 수행. SocialIdentity의 provider/subject 고유 인덱스와 ProviderLinkService.complete의 대상 provider 추가/동일 provider 중복 검사 확인. 다른 SNS가 이미 등록돼 있다는 이유만으로 새 SNS를 거절하는 구조는 아님.
- 테스트와 결과: 코드 정적 조회 및 git diff --check. 코드 변경 없어 실행 테스트 미실행.
- 유지한 계약: 한 회원에 복수 SNS 연결 지원, 다른 계정 소유 또는 동일 provider 중복 방지 유지. 외부 데이터·설정 미변경.
- 결정사항: 현재 오류는 다중 SNS 자체 충돌이 아닌 Firebase/Identity 연결 상태 불일치로 설명. 정식 연결 흐름과 외부에서 먼저 생긴 연결의 복구 공백을 구분.
- 위험 요소: 최초 설계 전체 이력과 Firebase 연결 생성 경위는 미확정. 예상 밖 수정 없음.
- 다음 작업: 요청 시 기존 다중 SNS 지원을 유지하는 안전한 불일치 복구 설계 검토. Jira 변경 없음.

## 2026-09-30 — 다중 SNS 지원 설명 작업 식별 기록 보완

<!-- codex-turn:01a0f138-9261-75c3-9c98-e9fadd0c0848 -->

- 브랜치: develop.
- 작업 목표: 현재 설명 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 다중 SNS 지원과 계정 연결 정보 불일치를 구분한 설명의 작업 식별자를 추가.
- 테스트와 결과: git diff --check 수행. 문서 보완만으로 실행 테스트 미실행.
- 유지한 계약: 복수 SNS 지원 및 계정 충돌 방지 유지. 코드·DB·설정 미변경, 비밀정보 비기록.
- 결정사항: 현재 오류를 다중 SNS 기능 자체의 충돌로 단정하지 않음.
- 위험 요소: Firebase 연결 생성 경위와 불일치 복구 절차 미검증. 예상 밖 수정 없음.
- 다음 작업: 요청 시 안전한 불일치 복구 설계 검토. Jira 변경 없음.

## 2026-09-30 — Apple 로그인 차단 조건과 보안 목적 설명

- 브랜치: develop.
- 작업 목표: 차단의 직접 조건과 검사 목적을 구분하여 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. ProviderChangeGuard.validatePrincipal의 기존 바인딩 및 현재 provider/subject 승인 레코드 검사 재확인. 현재 APPLE 레코드 부재로 거절되는 상태와 과거 계정 차단을 구분.
- 테스트와 결과: 소스 정적 확인, git diff --check 수행. 설명 작업으로 실행 테스트 미실행.
- 유지한 계약: 외부 연결만으로 서비스의 승인/해제 절차 우회 금지. 코드·계정·설정 미변경, 비밀정보 비기록.
- 결정사항: 기능 OFF나 계정 정지가 아니라 승인된 연결 정보 부재에 의한 요청 거절로 설명. Firebase 연결 생성 경위는 단정하지 않음.
- 위험 요소: 실제 불일치 생성 원인과 복구 절차는 추가 확인 필요. 예상 밖 수정 없음.
- 다음 작업: 요청 시 연결 불일치 복구 설계 검토. Jira 변경 없음.

## 2026-09-30 — Apple 차단 조건 설명 작업 식별 기록 보완

<!-- codex-turn:01a0f13a-3ebc-7d83-b109-a9008978d8d2 -->

- 브랜치: develop.
- 작업 목표: 현재 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 수정하지 않고 Apple 승인 레코드 누락에 따른 거절 조건 설명의 작업 기록 추가.
- 테스트와 결과: git diff --check 수행. 문서 보완만으로 실행 테스트 미실행.
- 유지한 계약: 코드·계정·DB·외부 설정 미변경, 비밀정보 비기록.
- 결정사항: 확인한 차단 조건과 미확정인 불일치 생성 경위를 구분.
- 위험 요소: 실제 불일치 생성 경위와 복구 절차 미검증. 예상 밖 수정 없음.
- 다음 작업: 요청 시 연결 불일치 원인 및 복구 절차 추가 조사. Jira 변경 없음.

## 2026-09-30 — Firebase 신뢰된 제공업체 자동 연결 정책 조사

- 브랜치: develop.
- 작업 목표: Apple 연결이 Firebase에만 생기는 경로를 공식 동작과 코드로 대조.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 공식 https://firebase.google.com/docs/auth/users#verified_email_addresses 실조회로 동일 이메일의 신뢰된 제공업체 로그인 시 자동 연결, Apple의 신뢰된 제공업체 분류, Google의 Gmail 조건 확인. 로컬 app.js는 Apple signInWithPopup/reauthenticateWithPopup만 호출하고 명시적 SNS link나 Identity sync 호출이 없음을 확인. FirebaseSignupService는 신규 가입 때 연결 SNS를 준비하지만 exchange는 기존 바인딩의 미등록 SNS를 거절하고 자동 저장하지 않음. 기존 sync 역시 승인된 연결 검증만 수행.
- 테스트와 결과: 공식 문서 브라우저 조회·코드 정적 대조·git diff --check. 코드 변경 없어 실행 테스트 미실행. 계정 로그인/연결 재현 mutation 미수행.
- 유지한 계약: 계정·DB·배포·설정 미변경, 비밀정보 비기록. 원인 조사와 수정 권한 구분.
- 결정사항: Firebase 자동 연결이 현재 상태와 부합하는 가장 유력한 생성 경로이며, 앱에서 link API를 직접 호출하지 않아도 발생 가능. Identity의 명시적 연결 승인 모델이 이 경로를 처리하지 못하는 통합 공백 확인. 사용자 실수나 테스트 화면 연결 버튼 누락만으로 단정하지 않음.
- 위험 요소: 해당 로그인 당시 이벤트 로그 및 연결 전 snapshot 미확보로 이번 연결 순간의 직접 증명은 아님. 자동 연결 전체를 무조건 승인하면 기존 해제/차단 보호를 훼손할 수 있어 별도 복구 설계 필요. 예상 밖 수정 없음.
- 다음 작업: 요청 시 Firebase 자동 연결을 고려한 안전한 계정 연결/복구 정책 및 회귀 테스트 설계. Jira 변경 없음.

## 2026-09-30 — Firebase 자동 연결 정책 조사 식별 기록 보완

<!-- codex-turn:01a0f13c-7853-7823-af05-3a9850b7edc2 -->

- 브랜치: develop.
- 작업 목표: 이번 자동 연결 정책 조사 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 공식 Firebase 자동 연결 정책 및 Identity 명시적 승인 모델의 불일치 조사 결과 식별 기록 추가.
- 테스트와 결과: git diff --check 수행. 문서 보완만으로 실행 테스트 미실행.
- 유지한 계약: 계정·DB·서버 코드·설정 미변경, 비밀정보 비기록.
- 결정사항: 공식적으로 가능한 자동 연결 동작과 이번 개별 사건의 직접 증명을 구분.
- 위험 요소: 당시 이벤트 로그 미확보, 실제 연결 생성 순간 미확정. 예상 밖 수정 없음.
- 다음 작업: 요청 시 안전한 자동 연결 대응 및 복구 정책 설계. Jira 변경 없음.

## 2026-09-30 — Apple 연결 해제 후 재현 범위 확인

- 브랜치: develop.
- 작업 목표: 사용자 요청의 Apple 연결 해제와 재로그인 재현 범위를 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 전체 계정 삭제가 아닌 테스트 Firebase 사용자의 Apple provider만 해제하는 범위를 설명하고 적용 전 사용자 확인 요청. Google/전화번호/UID/Identity 회원/기록은 유지 대상.
- 테스트와 결과: 안내 및 기록만 수행, git diff --check. 실행 테스트·계정 변경 미수행.
- 유지한 계약: 기존 계정 및 인증 수단 보호, 비밀정보 비기록. 승인 전 unlink/사용자 삭제 없음.
- 결정사항: 재로그인 시 Firebase 자동 재연결과 동일 오류가 발생할 수 있으므로 해결이 아닌 재현 실험으로 설명. Apple 로그인을 다시 수행할 때 사용자 직접 인증 필요.
- 위험 요소: 연결 해제는 해당 로그인 수단을 제거하며 세션 영향 확인 필요. 실제 해제 방법·재현은 승인 후 검토. 예상 밖 수정 없음.
- 다음 작업: 범위 승인 후 지원되는 provider 해제 방법 확인 및 변경 전후 연결 상태 비교. Jira 변경 없음.

## 2026-09-30 — Apple 재현 실험 목적 및 콘솔 해제 기능 확인

- 브랜치: develop.
- 작업 목표: 오류 재현 요청에 맞춰 Apple provider만 해제 가능한 경로 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Firebase 테스트 사용자 메뉴 읽기 전용 조회, 전체 계정 삭제/비활성화 및 비밀번호 재설정만 표시됨을 확인. Apple 단독 해제는 콘솔 메뉴에서 제공되지 않으므로 Firebase SDK 기반 테스트 UI 경로 검토.
- 테스트와 결과: 실제 콘솔 메뉴 확인 및 git diff --check. unlink/로그인 재현은 미실행, 자동 테스트 미실행.
- 유지한 계약: Google·전화번호·UID·Identity 회원과 기록 보존, 전체 사용자 삭제 금지. 비밀정보 비기록.
- 결정사항: 해결 작업이 아닌 해제 전후 provider/UID 및 exchange 결과 비교 실험으로 범위 명확화. 실제 연결 변경 직전 확인 요청.
- 위험 요소: Apple 연결 해제는 인증 수단 변경이며 세션 영향 가능. 재로그인 시 사용자 직접 인증 필요. 예상 밖 수정 없음.
- 다음 작업: Apple 단독 해제 범위 확인 후 재현용 SDK 조작 준비 및 변경 전후 검증. Jira 변경 없음.

## 2026-09-30 — Apple 재현 실험 준비 작업 식별 기록 보완

<!-- codex-turn:01a0f13e-1388-7711-bdc3-0df9a4049ed8 -->

- 브랜치: develop.
- 작업 목표: 이번 재현 실험 준비 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 Firebase 콘솔의 Apple 단독 해제 메뉴 부재 및 실제 변경 전 범위 확인 대기 상태를 기록.
- 테스트와 결과: git diff --check 수행. 문서 보완만으로 실행 테스트 미실행.
- 유지한 계약: 계정·DB·코드·설정 미변경, 비밀정보 비기록. 전체 사용자 삭제 없음.
- 결정사항: Google·전화번호·회원 기록을 유지하는 Apple 연결 단독 해제 승인 후 재현 진행.
- 위험 요소: 실제 unlink와 재로그인 결과 미검증. 예상 밖 수정 없음.
- 다음 작업: 사용자 확인 후 지원되는 SDK 경로로 해제 전후 상태 및 동일 오류 재발 여부 검증. Jira 변경 없음.

## 2026-09-30 — 승인된 Apple 단독 해제 및 재로그인 재현 준비

- 브랜치: develop. Jira: 요청에 연결된 이슈 없음.
- 작업 목표: Apple 연결만 해제한 뒤 동일 계정 로그인으로 자동 연결 및 409 재발 검증.
- 변경 파일: 별도 `/Users/msde76/tosunsaeng-integration-test`의 app.js, index.html, apple.test.mjs, README.md 및 docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 재현 버튼에 테스트 프로젝트·Google 인증·Identity 로그인 성공·Google/전화/Apple 존재·Guest 병합 상태 없음 검사 추가. 확인 후 apple.com만 unlink, reload로 UID/나머지 provider 보존 및 Apple 부재 확인. 불명확한 결과에 자동 재해제하지 않도록 메모리 잠금. 로그아웃 후 Apple 로그인 시 동일 UID/자동 연결/기존 provider 유지 boolean 비교. 토큰/사용자 ID 원문 비출력.
- 실제 수행: 사용자 범위 승인에 따라 기존 오류 탭 새로고침 및 기존 공개 Firebase 웹 구성 적용. 기존 테스트 Google 계정 선택 후 Identity MEMBER 로그인 성공. 재현 확인창 승인 후 Apple 단독 해제 성공 표시와 UID/Google/전화 유지 확인. Apple 로그인 창 열었으나 직접 인증 필요하여 대기. 전체 사용자 삭제/Google·전화 해제 없음. 해제된 Apple 연결은 재인증/연결로 복구 가능한 로그인 수단이며 원인 재현 목적으로 분리함.
- 실행한 테스트와 결과: Node app/apple/merge/challenge 테스트 5개 통과(해제 취소, Apple만 unlink, 상태 보존, 로그인 후 자동 연결 Mock 검증 포함). 브라우저에서 신규 재현 섹션/Google 로그인/실제 해제 결과 확인. 새 탭 직접 이동은 클라이언트 차단됐으나 기존 탭 reload 성공. git diff --check. Identity 서버 코드 변경 없어 Gradle 재실행 생략.
- 유지한 계약: 정식 Identity provider API/보호 조건/DB 승인 레코드 미변경, 운영 미변경, 서버 배포 없음. 비밀정보 비기록, 기존 사용자 변경 보존. Identity 로그인에 따른 정상 세션 발급 외 서버 변경 없음.
- 결정사항: 실험 목적의 Firebase 단독 unlink이며 서비스용 해제 기능으로 사용하지 않음. 사용자 Apple 인증 후 동일 UID와 exchange 결과를 확인해야 재현 완료.
- 위험 요소: Apple 실제 재로그인 및 409 재발 미완료. 새로고침하면 비교 메모리 소실. 기존 서버 세션은 로컬 초기화로 폐기되지 않음. 예상 밖 수정 없음.
- 다음 작업: 사용자가 같은 Apple 계정으로 인증 완료 후 비교 결과 및 Identity exchange 확인. 배포 전 확인: 로컬 재현 버튼은 서비스 배포 대상 아님. Jira 댓글/상태 변경 없음.

## 2026-09-30 — Apple 단독 해제 재현 작업 식별 기록 보완

<!-- codex-turn:01a0f13f-c70e-7da3-9ab4-eb80e23fcf6f -->

- 브랜치: develop.
- 작업 목표: 이번 승인된 Apple 재현 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 로컬 재현 UI 추가·실제 Apple 단독 해제·사용자 재인증 대기 결과의 작업 식별 기록 추가.
- 테스트와 결과: 앞선 Node Mock 테스트 5개 통과 및 실제 해제 결과 확인 유지. 이번 문서 보완 후 git diff --check 수행, 실행 테스트 재실행 없음.
- 유지한 계약: 추가 계정·DB·코드·설정 변경 없음, 비밀정보 비기록.
- 결정사항: Apple 재로그인 후 자동 연결과 Identity 오류 확인 전까지 재현 완료로 보고하지 않음.
- 위험 요소: 사용자 Apple 인증 및 409 재발 미검증. 예상 밖 수정 없음.
- 다음 작업: 사용자 인증 완료 후 기존 페이지 비교 상태와 Identity exchange 결과 확인. Jira 변경 없음.

## 2026-09-30 — Apple 자동 재연결 및 Identity 409 실재현 완료

<!-- codex-turn:01a0f14a-183f-7042-9d8f-de39617a950d -->

- 브랜치: develop. Jira: 연결된 이슈 없음.
- 작업 목표: 승인된 재현 실험에서 Apple 재로그인 후 자동 연결과 동일 오류 발생 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 사용자 인증 완료 후 Chrome 기존 테스트 탭의 비교 결과에서 동일 UID/Apple 연결/기존 로그인 수단 유지가 모두 true임을 확인. 앞선 실제 해제 결과(Apple 없음)와 대조. Identity 로그인/가입 준비 버튼을 눌러 exchange HTTP 409 PROVIDER_RELINK_REQUIRED 재발 확인.
- 테스트와 결과: 실제 Firebase Apple 인증→같은 UID로 Apple 재연결→Identity exchange 409 흐름 재현 성공. 신규 signup/명시적 SNS link API 없이 일반 로그인 경로에서 재연결 확인. git diff --check 수행. 코드 변경 없어 자동 테스트 재실행 없음.
- 유지한 계약: Google·전화번호 연결 유지, 전체 계정 삭제·DB 직접 변경·서버 코드/배포 변경 없음. 개인정보·토큰·비밀정보 비기록. 기존 사용자 변경 보존.
- 결정사항: 이번 재현의 자동 재연결은 추정이 아닌 관측 결과. 최초 사건의 과거 생성 로그까지 확보한 것은 아님. Identity의 누락된 승인 레코드 거절과 Firebase 자동 연결 간 호환성 문제를 해결 범위로 제시.
- 위험 요소: 원인 재현 완료이며 해결 완료는 아님. 현재 Apple 연결은 다시 존재하고 Identity Apple 로그인은 여전히 거절됨. 예상 밖 수정 없음.
- 다음 작업: 사용자 요청 시 기존 제공자 해제/차단 보호를 유지하는 자동 연결 처리 및 안전한 복구 계획 수립. 추가 해제/자동 승인하지 않음. Jira 변경 없음.

## 2026-09-30 — 다른 Apple 계정 비교 실험 준비 및 기존 연결 해제

- 브랜치: develop. Jira: 요청에 연결된 이슈 없음.
- 작업 목표: 기존 Apple 연결 해제 후 다른 Apple 계정 로그인에서도 동일 오류가 발생하는지 비교.
- 변경 파일: 별도 로컬 테스트 프로젝트 app.js, apple.test.mjs 및 docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 다른 계정의 Firebase UID 변경은 예상 가능한 결과이므로 재현 로그인 단계에서 중단하지 않고 boolean 비교만 표시. 서로 다른 계정 연결/토큰 복사 없음. 다른 UID Mock 로그인 후 exchange 버튼 활성화 검증 추가.
- 실제 수행: 기존 오류 화면 새로고침 및 공개 웹 설정 재적용, Google 인증/Identity 로그인 성공 확인. 사용자 요청한 Apple 단독 해제 실행 후 동일 UID·Google/전화 유지·Apple 없음 확인. Apple 로그인 창을 열었으며 사용자 이중 인증 단계 대기.
- 테스트와 결과: Node app/apple/merge/challenge 테스트 5개 통과, 실제 해제 후 상태 확인, git diff --check. 서버 코드 미변경으로 Gradle 재실행 생략.
- 유지한 계약: 전체 사용자 삭제·Google/전화번호 해제·Identity DB 직접 변경·배포 없음. 비밀정보 비기록, 기존 사용자 변경 보존.
- 결정사항: Identity exchange 결과 비교까지만 범위로 하며 신규 signup/약관 동의/전화번호 연결은 수행하지 않음.
- 위험 요소: 실제 다른 Apple 계정 여부·UID 및 오류 결과 미확인, 사용자 인증 필요. 현재 기존 Apple 연결은 해제 상태이며 재연결 가능. 예상 밖 수정 없음.
- 다음 작업: 사용자 인증 후 UID 비교 및 exchange 응답 확인. 로컬 재현 기능은 서비스 배포 대상 아님. Jira 변경 없음.

## 2026-09-30 — 다른 Apple 비교 실험 작업 식별 기록 보완

<!-- codex-turn:01a0f14c-14b3-7c60-bbfb-0531777241b7 -->

- 브랜치: develop.
- 작업 목표: 이번 다른 Apple 계정 비교 실험의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 다른 UID 허용 비교 기능·기존 Apple 단독 해제·사용자 인증 대기 결과의 식별 기록 추가.
- 테스트와 결과: 앞선 Node Mock 테스트 5개 통과 및 실제 해제 결과 확인 유지. git diff --check 수행, 문서 보완만으로 실행 테스트 재실행 없음.
- 유지한 계약: 추가 계정·DB·코드·설정 변경 없음, 비밀정보 비기록.
- 결정사항: 다른 Apple 인증 및 exchange 확인 전까지 재현 결과를 확정하지 않음.
- 위험 요소: 사용자 인증 대기, 실제 다른 UID 및 오류 응답 미확인. 예상 밖 수정 없음.
- 다음 작업: 인증 완료 후 기존 페이지의 비교 상태 및 Identity 응답 확인. Jira 변경 없음.

## 2026-09-30 — 다른 Apple 계정의 Identity 가입 준비 정상 응답 확인

- 브랜치: develop. Jira: 연결된 이슈 없음.
- 작업 목표: 다른 Apple 계정 로그인 시 기존 계정 자동 연결 사례와 동일한 409가 발생하는지 검증.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 사용자 Apple 인증 완료 후 Chrome 재현 화면의 동일 UID=false/Apple 연결=true/기존 로그인 수단 유지=false 확인. Identity 로그인/가입 준비 버튼 실행 후 신규 가입 준비 완료 안내 및 전화 인증 시작 버튼 활성 확인.
- 테스트와 결과: 실제 다른 Firebase UID의 Apple 인증 및 exchange 신규 enrollment 경로 성공, 해당 409 재발 없음. 전화 연결/약관 동의/signup은 미실행. git diff --check 수행. 코드 변경 없어 자동 테스트 재실행 없음.
- 유지한 계약: 기존 계정·Google/전화·회원 기록 삭제 없음, 추가 provider 변경 없음. 토큰/사용자 식별값/비밀정보 비기록. 신규 Identity 회원 생성까지 수행하지 않음.
- 결정사항: Apple 로그인 전체 장애가 아니라 기존 Firebase 계정에 자동 연결된 SNS와 Identity 승인 정보가 불일치할 때의 문제로 범위 좁힘. 기존 로그인 수단 유지=false는 다른 계정 간 비교로 해석하며 기존 계정 수단 삭제로 해석하지 않음.
- 위험 요소: 신규 MEMBER 가입과 이후 API 검증은 미수행. 테스트 enrollment는 만료될 수 있음. 이전 기존 계정의 Apple 해제 상태 유지. 예상 밖 수정 없음.
- 다음 작업: 요청 시 기존 UID 자동 연결 대응 설계 또는 신규 Apple 회원가입 검증 진행. Jira 변경 없음.

## 2026-09-30 — 다른 Apple 계정 검증 작업 식별 기록 보완

<!-- codex-turn:01a0f14f-ab03-7770-88dd-7c286941d9fe -->

- 브랜치: develop.
- 작업 목표: 이번 다른 Apple 계정 검증의 정확한 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 다른 Firebase UID의 Apple 인증 및 Identity 신규 가입 준비 정상 응답 결과의 식별 기록 추가.
- 테스트와 결과: 앞선 실제 화면 검증 결과 유지, git diff --check 수행. 문서 보완만으로 실행 테스트 재실행 없음.
- 유지한 계약: 추가 계정·DB·코드·설정 변경 없음, 비밀정보 비기록.
- 결정사항: 409 미재발과 신규 가입 준비 성공을 최종 회원가입 완료와 구분.
- 위험 요소: 전화번호 연결·MEMBER 가입·후속 API 미검증. 예상 밖 수정 없음.
- 다음 작업: 요청 시 기존 UID 자동 연결 대응 설계 또는 신규 가입 검증 진행. Jira 변경 없음.

## 2026-09-30 — Apple 신규 회원 후속 검증 순서 안내

<!-- codex-turn:01a0f152-ed12-7800-936d-e5e4dcd2227e -->

- 브랜치: develop.
- 작업 목표: 현재 확인 범위와 다음에 필요한 기능 검증의 우선순위 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 신규 enrollment 성공과 MEMBER 가입 완료를 구분하고 전화 연결/동의/가입, 프로필, Apple 재로그인, 토큰 재발급, LC 접근 및 챌린지 순서 제안. 병합은 기본 인증 이후 별도 테스트로 분리.
- 테스트와 결과: 앞선 실검증 결과 기반 안내, git diff --check 수행. 신규 외부 테스트 미실행.
- 유지한 계약: 약관 동의는 사용자 직접 수행, 기존 계정 전화번호 재사용 금지 안내. 실제 계정·데이터·설정 변경 없음, 비밀정보 비기록.
- 결정사항: 정상 신규 계정 경로 검증은 이어갈 수 있으나 기존 UID 자동 연결 409를 해결 완료로 취급하지 않음.
- 위험 요소: 신규 Apple MEMBER 발급/재로그인/재발급 미검증. 자동 연결 문제는 출시 전 수정·회귀 검증 필요. 예상 밖 수정 없음.
- 다음 작업: 사용자 진행 요청 후 미사용 테스트 전화번호 및 약관 동의 준비, 가입 검증. Jira 변경 없음.

## 2026-09-30 — 신규 Apple 가입용 테스트 전화번호 가용성 확인

- 브랜치: develop.
- 작업 목표: 요청된 신규 Apple 가입 검증을 위한 미사용 테스트 전화번호 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Chrome Apple 신규 enrollment 준비 상태 확인 및 Firebase 전화 provider의 테스트 번호 설정 펼쳐 조회. 등록 번호는 1개이며 앞서 확인된 기존 Google 회원의 연결 번호로 별도 번호 필요.
- 테스트와 결과: 실제 설정 화면 읽기 전용 확인, git diff --check 수행. 새로운 전화 인증/가입 및 실행 테스트 미수행.
- 유지한 계약: 기존 Google 전화 연결·회원 기록 보존, provider 설정 및 테스트 인증 코드 변경 없음. 인증 코드/개인정보 비기록.
- 결정사항: 현재 번호를 새 Apple 계정에 재사용하지 않음. 별도 가상 번호 및 고정 코드는 사용자가 콘솔에서 직접 등록하도록 인계, 이후 전화 인증 이어서 진행. 약관 동의는 별도 사용자 확인 필요.
- 위험 요소: 추가 테스트 번호 미등록으로 전화 연결 검증 대기, enrollment 만료 시 가입 준비 재요청 필요. 예상 밖 수정 없음.
- 다음 작업: 사용자 별도 가상 번호 등록 후 신규 Apple 전화 연결 검증. Jira 변경 없음.

## 2026-09-30 — 신규 Apple 전화 인증 준비 작업 식별 기록 보완

<!-- codex-turn:01a0f153-b853-7d73-addf-8c5d552adbf6 -->

- 브랜치: develop.
- 작업 목표: 이번 전화 인증 준비 작업의 정확한 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 기록을 보존하고 테스트 번호 가용성 확인 및 별도 번호 등록 대기 상태의 식별 기록 추가.
- 테스트와 결과: git diff --check 수행. 문서 보완만으로 실행 테스트 미실행.
- 유지한 계약: 기존 회원 전화 연결 유지, 외부 설정·계정 변경 없음, 비밀정보 비기록.
- 결정사항: 사용 중인 테스트 번호를 새 계정에 재사용하지 않음.
- 위험 요소: 별도 테스트 번호 등록 및 실제 전화 인증 미완료. 예상 밖 수정 없음.
- 다음 작업: 사용자 번호 등록 후 신규 Apple 전화 연결 검증. Jira 변경 없음.

## 2026-09-30 — 신규 Apple 계정 테스트 전화 연결 성공

<!-- codex-turn:01a0f157-dcb6-77f3-92fa-aee455c04396 -->

- 브랜치: develop.
- 작업 목표: 사용자 등록한 별도 가상 번호로 신규 Apple 계정 전화 연결 검증.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Firebase 전화 provider 설정에 테스트 번호 두 개 및 저장 비활성 확인. 기존 번호와 다른 신규 가상 번호를 사용. Chrome Apple 세션에서 exchange 가입 준비 갱신 후 전화 인증 시작 및 고정 코드 연결 수행. SDK 성공 화면에서 Firebase UID 유지와 전화 연결 완료 확인.
- 테스트와 결과: 실제 테스트 Firebase 전화 연결 성공. 최종 signup/프로필/토큰 재발급/LC는 미실행. git diff --check 수행, 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 실제 SMS 미발송, 기존 Google 계정의 전화 연결 유지. 인증 코드·토큰·개인 식별값 비기록. 약관 동의 및 최종 가입 대리 실행 없음. 서버 설정·배포·DB 직접 변경 없음.
- 결정사항: 등록 완료 보고를 전화 연결 검증 진행으로 해석. 가입 전 사용자 닉네임·정책 확인·동의 필요. 기존 draft 정책 버전을 현재 live 값으로 단정하지 않음.
- 위험 요소: 신규 Identity MEMBER는 아직 생성되지 않았으며 enrollment 만료 가능. 기존 자동 연결 409 문제는 미해결. 예상 밖 수정 없음.
- 다음 작업: 현재 정책 버전 확인 및 사용자 직접 약관 동의 후 최종 가입, 프로필/재로그인/재발급 검증. Jira 변경 없음.

## 2026-09-30 — 배포된 정책 버전 확인 및 테스트 화면 입력

<!-- codex-turn:01a0f15a-e8a5-77e3-8b23-e88ece58b884 -->

- 브랜치: develop.
- 작업 목표: 사용자 요청대로 현재 서버 정책 버전만 입력하고 동의·가입은 사용자에게 유지.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. AWS 테스트 서비스 새로고침 후 연결된 task revision 6에서 개인정보 privacy-v1, 이용약관 term-v1 확인. Chrome 테스트 화면 두 버전 필드 입력 및 표시 확인.
- 테스트와 결과: 브라우저 실제 설정 및 입력값 검증 완료. git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, 동의 체크·최종 가입·운영 설정 변경 없음.
- 결정사항: 조회 당시 동의 체크가 이미 선택되어 있었으므로 그대로 보존. 대리 동의 또는 signup 미실행.
- 위험 요소: 신규 MEMBER 가입 및 후속 검증 미완료, enrollment 만료 가능. 기존 사용자 변경 보존, 이번 작업 예상 밖 변경 없음.
- 다음 작업: 사용자 약관 확인 후 최종 가입 및 프로필·재로그인·재발급 검증. Jira 변경 없음.

## 2026-09-30 — 신규 Apple 가입 RESTART_EXCHANGE 진단

- 브랜치: develop.
- 작업 목표: 사용자 로그인 실패 보고 원인 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Chrome signup RESTART_EXCHANGE 및 프로필 버튼 비활성 확인. 로컬 app.js의 signup 사전 검사와 서버 enrollment 기본 10분·응답 밀리초 단위 확인.
- 테스트와 결과: UI 및 소스 읽기 검증. git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 토큰·개인정보 비기록, 약관 동의/가입/서버 변경 미실행.
- 결정사항: Firebase 로그인 실패가 아니라 가입 준비 재시작 요구로 안내. 경과 시간상 enrollment 만료 유력하나 내부 메모리 직접 조회 없이 다른 사전 조건 실패 가능성과 구분.
- 위험 요소: 신규 MEMBER 미생성, 재인증 필요 가능. 기존 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 사용자 exchange 갱신 및 필요 시 Apple 재인증 후 최종 가입. Jira 변경 없음.

## 2026-09-30 — 가입 오류 진단 작업 식별 기록

<!-- codex-turn:01a0f15f-3567-7f61-83c8-5570e01ed056 -->

- 브랜치: develop.
- 작업 목표: 이번 RESTART_EXCHANGE 진단의 turn 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome 오류 및 로컬 가입 사전 검사 확인 결과를 기록. 코드 수정 없음.
- 테스트와 결과: git diff --check 통과. 읽기 전용 진단으로 Gradle 미실행.
- 유지한 계약: 비밀정보 비기록, 동의·가입·서버 변경 없음.
- 결정사항: 가입 준비 갱신 안내, enrollment 만료는 유력 원인으로 구분.
- 위험 요소: 신규 가입 및 후속 인증 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 가입 준비 갱신 후 최종 가입 결과 확인. Jira 변경 없음.

## 2026-09-30 — 전화 연결 후 exchange 제공자 거절 진단

- 브랜치: develop.
- 작업 목표: HTTP 403 FIREBASE_PROVIDER_NOT_ALLOWED 원인 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome 검증표에서 exchange 실패 확인. FirebaseAdminAuthenticationVerifier의 LOGIN_EXCHANGE PHONE 거절 조건 확인. 직전 task revision 6의 Apple/Phone 활성 확인과 비교.
- 테스트와 결과: UI 및 소스 읽기 검증, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: PHONE 단독 로그인 제한 유지, 비밀정보 비기록, 계정/동의/배포 변경 없음.
- 결정사항: 전화 연결 후 sign-in provider 변경 가능성이 유력하나 현재 claim 미조회로 확정하지 않음. 같은 Apple 재인증 후 exchange 및 가입 안내.
- 위험 요소: 실제 claim 및 재인증 후 성공 미확인. 기존 변경 보존, 예상 밖 수정 없음.
- 다음 작업: 사용자 Apple 재인증 후 가입 결과 확인. Jira 변경 없음.

## 2026-09-30 — 제공자 거절 진단 식별 기록 보완

<!-- codex-turn:01a0f161-0128-7b92-b88f-57124fe10a98 -->

- 브랜치: develop.
- 작업 목표: 현재 turn의 exchange 403 진단 기록 연결.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: UI exchange 실패 및 서버 PHONE 로그인 제한 확인 결과 기록. 코드 변경 없음.
- 테스트와 결과: git diff --check 통과. 읽기 전용 진단으로 Gradle 미실행.
- 유지한 계약: 제공자 제한 유지, 동의·가입·설정 변경 없음, 비밀정보 비기록.
- 결정사항: PHONE 전환은 미확정 가설이며 같은 Apple 재인증 후 재시도 안내.
- 위험 요소: 재인증 후 성공 및 현재 claim 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 Apple 재인증 및 가입 결과 확인. Jira 변경 없음.

## 2026-09-30 — Apple 가입 후 기능 및 병합 읽기 검증

- 브랜치: develop.
- 작업 목표: 사용자 수행 후 성공 범위와 미검증 범위 구분.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Chrome 검증표에서 Apple 재인증·회원가입·프로필·재발급·LC 인증·Guest 준비·Identity 병합·응시·S3 업로드 성공 확인. 병합 후 검증 버튼의 읽기 전용 구현을 확인하고 실행.
- 테스트와 결과: Identity/LC 이전 Guest 모두 ACCOUNT_MERGED_TOKEN_REJECTED 확인, 대상 MEMBER 완료 이력 조회 성공. 이전 기록 0건으로 실제 이전 미검증. 챌린지 결과 solvedQuestionCount=0 및 question=null, 제출/AI 채점 성공 미확인. git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 비밀정보·개인정보 비기록, 추가 가입·병합·제출·재발급 또는 서버 설정 변경 없음.
- 결정사항: 과거 signup/exchange 오류 기록과 이후 성공 기록 구분. 기본 인증 및 병합 차단은 성공, 전체 기능 완료로 단정하지 않음.
- 위험 요소: AI 채점, 기록 있는 Guest 이전, 이벤트 204/중복 처리는 미검증. 기존 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 챌린지 제출/채점 및 기록 있는 Guest 이전은 별도 범위로 검증. Jira 변경 없음.

## 2026-09-30 — 후속 기능 검증 turn 기록 보완

<!-- codex-turn:01a0f163-6859-73f0-8185-9d56dd1949c7 -->

- 브랜치: develop.
- 작업 목표: 이번 Apple 가입 후 기능 확인의 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 기본 인증·가입·재발급·LC 접근 성공 표시 확인 및 병합 후 읽기 검증 결과 기록. 코드 변경 없음.
- 테스트와 결과: 이전 Guest의 Identity/LC 전용 차단 및 대상 MEMBER 이력 조회 성공. git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 추가 가입·병합·제출·서버 변경 없음, 비밀정보 비기록.
- 결정사항: 기본 인증 성공과 전체 종단 검증 완료를 구분.
- 위험 요소: 기록 0건으로 실제 이전 미검증, AI 채점 성공 미확인. 예상 밖 변경 없음.
- 다음 작업: 챌린지 제출·채점 및 기록 있는 Guest 이전 별도 검증. Jira 변경 없음.

## 2026-09-30 — 이전 Google 채점 성공 기록 확인

- 브랜치: develop.
- 작업 목표: 이전 Google 검증 성공 여부와 최근 Apple 검증 범위 구분.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 WORKLOG에서 CORS 수정 후 실제 업로드·제출·AI completed 및 결과 재조회 성공 확인. 이번 Apple 계정 미확인과 구분하여 설명.
- 테스트와 결과: 과거 기록 읽기 확인, git diff --check 수행. 코드 변경 및 신규 서버 호출 없어 실행 테스트 미실행.
- 유지한 계약: 비밀정보 비기록, 외부 상태 변경 없음.
- 결정사항: AI 전체가 미검증인 것이 아니라 새 Apple 계정 시도만 미확인으로 정정. Google 당시 기록 이전도 0건으로 미검증.
- 위험 요소: Apple 현재 채점 성공 및 기록 이전은 여전히 미확인. 예상 밖 변경 없음.
- 다음 작업: 필요 시 Apple 계정 제출·결과 확인. Jira 변경 없음.

## 2026-09-30 — Google 검증 범위 확인 turn 기록 보완

<!-- codex-turn:01a0f166-2c49-7ef0-a54d-ae1cca42c1bc -->

- 브랜치: develop.
- 작업 목표: 이전 Google 채점 성공 확인과 이번 Apple 시도의 미확인 범위 구분.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 과거 검증 기록의 실제 AI completed 및 결과 재조회 성공 확인. 코드 변경 없음.
- 테스트와 결과: 기록 읽기 확인 및 git diff --check 통과. 코드 변경 없어 실행 테스트 미실행.
- 유지한 계약: 외부 상태 변경 없음, 비밀정보 비기록.
- 결정사항: 채점 기능 전체 미검증이 아닌 Apple 신규 시도 미확인으로 설명 정정.
- 위험 요소: 실제 기록 이전은 당시에도 기록 0건으로 미검증. 예상 밖 변경 없음.
- 다음 작업: 필요 시 Apple 계정 제출·채점 결과 확인. Jira 변경 없음.

## 2026-09-30 — 기록 있는 Guest 병합 재검증 범위 확인

- 브랜치: develop.
- 작업 목표: 기존 Guest 정리 및 임시 모의고사 1건을 이용한 병합 재검증 준비.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: UserMerged 인계 계약에서 source MERGED tombstone 유지 확인. 기존 병합 계정 삭제 대신 신규 Guest 사용 제안, LC 저장소/fixture 경로 확인 요청.
- 테스트와 결과: 계약·이전 검증 기록 읽기 및 git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: Identity에 모의고사 코드 추가 금지, 비밀정보 비기록, 외부 삭제·생성·병합 미실행.
- 결정사항: 삭제 범위 불명확한 기존 병합 기록 보존. LC 스키마 확인 없이 임의 문서 삽입하지 않음.
- 위험 요소: LC 임시 완료 데이터 생성 방법 미확인, 신규 Guest 동의 및 실제 병합은 실행 전 별도 확인 필요.
- 다음 작업: LC 저장소 위치 확인 후 테스트 데이터 준비 및 신규 Guest 병합 검증. Jira 변경 없음.

## 2026-09-30 — 기록 있는 Guest 재검증 준비 turn 기록

<!-- codex-turn:01a0f167-97be-7490-8a52-f32ed56a3dce -->

- 브랜치: develop.
- 작업 목표: 새 Guest와 완료 모의고사 1건을 이용한 병합 검증 준비 범위 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 기존 MERGED Guest 보존 및 새 Guest 사용 제안. LC 스키마 확인을 위한 저장소 위치 요청. 코드 변경 없음.
- 테스트와 결과: 계약·기록 조회 및 git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: Identity 도메인 경계 유지, 비밀정보 비기록, 외부 삭제·생성·병합 미실행.
- 결정사항: 기존 병합 기록은 삭제하지 않고 데이터 생성 방법 확인 후 진행.
- 위험 요소: LC fixture 방법 미확인, 신규 Guest 동의·실제 병합 실행 전 확인 필요. 예상 밖 변경 없음.
- 다음 작업: LC 저장소 위치를 받아 테스트 데이터 준비 방법 확인. Jira 변경 없음.

## 2026-09-30 — 새 Guest 생성 버튼 비활성 진단

- 브랜치: develop.
- 작업 목표: 새 Guest 생성 불가 원인 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome 생성 버튼 disabled 및 기존 병합 검증 버튼 활성 확인. 독립 도구 app.js controls가 oldGuest/mergeUncertain을 포함해 생성 차단하며 병합 후 검증용 상태를 보존함을 확인. clear는 전체 로컬 인증·녹음/응시 상태를 초기화하므로 미실행.
- 테스트와 결과: UI·소스 읽기 검증 및 git diff --check 수행. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 외부 계정 삭제·생성·병합 없음, 비밀정보 비기록.
- 결정사항: 서버 오류와 로컬 도구 재검증 잠금을 구분. 초기화 영향 안내 후 사용자 선택 필요.
- 위험 요소: 전체 초기화 시 현재 메모리 로그인·녹음/진행 상태 손실. 기존 서버 계정·기록은 삭제되지 않음. 예상 밖 변경 없음.
- 다음 작업: 전체 로컬 초기화 후 재로그인 또는 Guest 전용 초기화 기능 개선 요청에 따라 진행. Jira 변경 없음.

## 2026-09-30 — Guest 생성 잠금 진단 turn 기록 보완

<!-- codex-turn:01a0f16a-4741-70b0-9a6e-579e498bae50 -->

- 브랜치: develop.
- 작업 목표: 새 Guest 생성 버튼 비활성 진단의 현재 turn 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: UI disabled 및 기존 병합 상태 보존에 따른 도구 잠금 확인. 코드 수정·로컬 초기화 미실행.
- 테스트와 결과: UI/소스 확인 및 git diff --check 통과. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 서버 계정 삭제·생성·병합 없음, 비밀정보 비기록.
- 결정사항: 전체 초기화의 로그인·녹음 손실을 안내하고 MEMBER 유지형 Guest 전용 초기화 개선 여부 질문.
- 위험 요소: 기존 로컬 상태 유지 중이며 반복 생성은 여전히 잠김. 예상 밖 변경 없음.
- 다음 작업: 사용자 개선 승인 또는 전체 초기화 선택 대기. Jira 변경 없음.

## 2026-09-30 — Apple 자체 로그인 폼 오류 확인

- 브랜치: develop.
- 작업 목표: 사용자 Apple 로그인 일반 오류의 발생 단계 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome Apple 인증 팝업의 계정 로그인 폼에서 문제가 발생했으니 다시 시도하라는 문구 확인. localhost의 Apple/exchange 버튼은 팝업 대기로 비활성 확인. 코드 변경 없음.
- 테스트와 결과: 브라우저 읽기 검증, git diff --check 수행. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 비밀번호·인증 코드·OAuth state 비기록, 로그인 대행·초기화·외부 설정 변경 없음.
- 결정사항: Identity 403/409와 구분하여 Apple 계정 인증 단계 문제로 안내. 상세 원인 및 일시 제한 여부는 미확정.
- 위험 요소: 일반 오류 문구만으로 계정/세션/Apple 서비스 원인을 특정할 수 없음. 예상 밖 변경 없음.
- 다음 작업: 사용자 팝업 재시작 또는 패스키 인증 후 결과 확인. Jira 변경 없음.

## 2026-09-30 — Apple 로그인 팝업 진단 turn 기록 보완

<!-- codex-turn:01a0f16c-c46e-76a2-ad7a-c1a57e771cc4 -->

- 브랜치: develop.
- 작업 목표: Apple 자체 로그인 오류 진단의 현재 turn 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Apple 계정 로그인 폼의 일반 오류와 테스트 화면의 팝업 대기 상태 확인. 코드 수정 없음.
- 테스트와 결과: UI 읽기 검증 및 git diff --check 통과. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 비밀정보 비기록, 인증정보 입력·초기화·계정 삭제·설정 변경 없음.
- 결정사항: Identity 오류와 구분하고 사용자 팝업 재시작 또는 패스키 인증 안내.
- 위험 요소: 상세 원인 미확정, 재시도 성공 미검증. 예상 밖 변경 없음.
- 다음 작업: 사용자 재인증 결과 확인. Jira 변경 없음.

## 2026-09-30 — 테스트 화면 정책 버전 재입력

- 브랜치: develop.
- 작업 목표: 정책 버전 재입력 및 사용자에게 값 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 이전 AWS test revision 6 확인값 privacy-v1, term-v1을 Chrome 두 입력란에 재입력. 코드 변경 없음.
- 테스트와 결과: 입력값 UI 표시 확인 및 git diff --check 수행. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 동의 체크·가입 버튼 미조작, 비밀정보 비기록.
- 결정사항: 이번 AWS 재조회 없이 앞서 검증한 버전 사용.
- 위험 요소: 이후 배포 정책 변경 여부는 이번에 재확인하지 않음. 예상 밖 변경 없음.
- 다음 작업: 사용자 정책 확인·동의 후 테스트 진행. Jira 변경 없음.

## 2026-09-30 — 새 Guest 더미 모의고사 삽입 사전 조사

- 브랜치: develop.
- 작업 목표: 새 Guest 소유 완료 모의고사 1건 준비.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 로컬 Learning Core 저장소 발견 및 규칙/ExamReadService/ExamSession/ExamSummary/조회 조건 확인. Identity에 LC 코드 추가 없음.
- 테스트와 결과: 스키마·로컬 프록시 읽기 확인, git diff --check 수행. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 운영·기존 계정·병합 marker 변경 없음, 토큰·개인정보 비기록.
- 결정사항: 방금 생성된 Guest UUID가 UI에 없어 소유자 확정 전 삽입 중단. 최신 생성 계정 추정은 사용하지 않음.
- 위험 요소: 정확한 Guest 소유자 및 테스트 DB 삽입 경로 검증 필요. 더미 생성 미완료, 예상 밖 변경 없음.
- 다음 작업: Guest UUID 확인 후 테스트 DB에 한정한 fixture 생성·이력 조회 검증. Jira 변경 없음.

## 2026-09-30 — Guest 더미 데이터 준비 turn 기록 보완

<!-- codex-turn:01a0f16f-fcea-7f02-b7b6-8781010b68a7 -->

- 브랜치: develop.
- 작업 목표: 새 Guest 더미 모의고사 삽입 요청의 현재 turn 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Learning Core 로컬 소스에서 완료 이력 조회 및 세션·요약 스키마 확인. 정확한 Guest 소유자 확인 전 데이터 삽입 보류. 코드 변경 없음.
- 테스트와 결과: 소스 읽기 확인 및 git diff --check 통과. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 비밀정보 비기록, 운영·기존 계정·병합 marker 변경 없음.
- 결정사항: 최신 계정 추정 없이 Guest UUID 확인 필요. 토큰 원문 요청하지 않음.
- 위험 요소: 더미 삽입 및 이력 조회 검증 미완료. 예상 밖 변경 없음.
- 다음 작업: 정확한 Guest UUID 확인 후 테스트 DB 대상 검증 및 fixture 삽입. Jira 변경 없음.

## 2026-09-30 — 정책 버전 재입력 turn 기록 보완

<!-- codex-turn:01a0f16e-72f5-7373-9b33-e76496c0159c -->

- 브랜치: develop.
- 작업 목표: 현재 turn의 정책 버전 재입력 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 이전 테스트 배포 확인값 privacy-v1 및 term-v1을 Chrome 입력란에 채우고 사용자에게 안내. 코드 변경 없음.
- 테스트와 결과: UI 입력값 확인 및 git diff --check 통과. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 동의·가입 대리 실행 없음, 비밀정보 비기록.
- 결정사항: 앞서 확인한 정책 버전 사용, 이번 AWS 재조회 없음.
- 위험 요소: 이후 서버 정책 변경 여부 미재확인. 예상 밖 변경 없음.
- 다음 작업: 사용자 정책 확인·동의 후 테스트 진행. Jira 변경 없음.

## 2026-09-30 — Guest 더미 데이터 준비 최종 상태

<!-- codex-turn:01a0f16f-fcea-7f02-b7b6-8781010b68a7 -->

- 브랜치: develop.
- 작업 목표: Guest 더미 삽입 준비 결과를 WORKLOG 끝에 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: LC 완료 이력 스키마 확인, Guest UUID 확인 대기. 코드 변경 없음.
- 테스트와 결과: 소스 읽기 확인, git diff --check 통과. 코드 변경 없어 자동 테스트 미실행.
- 유지한 계약: 외부 데이터 변경 없음, 비밀정보 비기록.
- 결정사항: 소유자 추정 삽입 금지, 확인 후 테스트 DB만 사용.
- 위험 요소: fixture 삽입 미완료. 예상 밖 코드 변경 없음.
- 다음 작업: Guest UUID 확인 및 테스트 DB 검증 후 삽입. Jira 변경 없음.

## 2026-09-30 — 최신 테스트 Guest의 더미 완료 모의고사 추가

- 브랜치: develop.
- 작업 목표: 사용자 승인한 최신 Guest에 병합 검증용 완료 모의고사 1건 삽입.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 코드 변경 없음.
- 구현 내용: 테스트 Identity users에서 GUEST 최신순 3개를 최소 필드 조회하여 최신 ACTIVE/이전 2개 MERGED 확인. 테스트 LC mock_exams에 active=false·빈 questions·TEST ONLY 제목의 참조 카탈로그 1건, exam_summaries에 합성 점수120/IM2·더미 안내 1건, exam_sessions에 해당 Guest 소유 COMPLETED/active=false·날짜 포함 1건 삽입. 카탈로그 서비스의 비활성 제외와 완료 이력 조회 조건을 소스로 확인.
- 테스트와 결과: Atlas 저장 결과 확인, Chrome 병합 전 완료 시험 기록 조회 실행으로 현재 Guest에서 1건 조회 및 비교 준비 확인. git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 운영 DB·기존 계정·병합 marker/guard 수정 및 실제 AI 호출 없음. Identity에 LC 코드 추가 없음. 사용자 UUID·토큰 비기록, 더미는 실제 채점 결과와 구분.
- 결정사항: 기존 계정 삭제 없이 새 Guest에 합성 완료 이력과 요약만 추가. 비활성 카탈로그라 실제 응시 배정 제외. fixture 참조명 merge-fixture-20260930-173033으로 식별 가능.
- 위험 요소: 문항별 결과·AI 채점 검증용 완전한 시험 아님. 실제 병합 및 소유권 이전은 아직 미실행. 여러 컬렉션 UI 순차 삽입 후 현재 Guest 조회로 확인, 잔존 더미는 테스트 종료 후 정리 대상. 예상 밖 코드 변경 없음.
- 다음 작업: 사용자 재인증/병합 승인 후 MEMBER에서 동일 시험 이전 및 Guest 차단 검증. Jira 변경 없음.

## 2026-09-30 — Guest 더미 삽입 완료 turn 기록

<!-- codex-turn:01a0f172-7184-7dd2-9075-746ef07a8df3 -->

- 브랜치: develop.
- 작업 목표: 최신 테스트 Guest의 완료 모의고사 더미 생성 및 조회 검증 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 코드 변경 없음.
- 구현 내용: 승인된 최신 생성 기준으로 ACTIVE Guest 확인 후 테스트 LC에 비활성 카탈로그·완료 세션·합성 요약 각 1건 삽입.
- 테스트와 결과: Atlas 저장 및 현재 Chrome Guest의 완료 이력 1건 조회 확인. git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 운영·기존 계정·병합 marker 변경 없음, 비밀정보 비기록, 실제 AI 채점과 더미 구분.
- 결정사항: 병합 비교용 이력 준비 완료, 실제 병합 미실행.
- 위험 요소: 문항별 결과 없는 합성 데이터이며 실제 소유권 이전은 아직 미검증. 예상 밖 코드 변경 없음.
- 다음 작업: 사용자 병합 실행 후 MEMBER 기록 이전 및 Guest 차단 확인. Jira 변경 없음.

## 2026-09-30 — 더미 완료 시험 1건 병합 후 검증 성공

- 브랜치: develop.
- 작업 목표: 사용자 실행한 Guest 병합의 실제 기록 이전 및 source 차단 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome 기존 성공 표시에 이어 병합 후 읽기 검증 직접 재실행. 코드 변경 없음.
- 테스트와 결과: Identity/LC 이전 Guest의 병합 전용 거절 코드 확인, target MEMBER 이력 조회 성공, 이전 시험 ID 1건 중 1건 포함 확인. git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 추가 병합·삽입·삭제 없음, 토큰·개인 식별값 비기록, 합성 데이터와 실제 채점 구분.
- 결정사항: 이번에는 기록 있는 Guest의 완료 이력 이전 검증 성공으로 보고. 기존 0건 테스트의 미검증 상태와 구분.
- 위험 요소: 요약/문항별 결과 소유권 DB 재조회 및 이벤트204·중복·장애 재시도는 이번 작업에서 미검증. 예상 밖 변경 없음.
- 다음 작업: 필요 시 요약/상세 결과 및 중복·장애 시나리오 추가 검증. Jira 변경 없음.

## 2026-09-30 — 기록 있는 Guest 병합 검증 turn 기록

<!-- codex-turn:01a0f17a-1321-7121-9953-f75a4c519fa7 -->

- 브랜치: develop.
- 작업 목표: 사용자 병합 후 기록 이전 및 Guest 차단 검증 결과 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome 병합 후 읽기 검증 재실행. 코드 변경 없음.
- 테스트와 결과: Identity/LC 이전 Guest 병합 전용 거절 및 MEMBER 완료 이력 조회 성공, 이전 시험 1건 중 1건 포함 확인. git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 추가 병합·데이터 변경 없음, 비밀정보·개인 식별값 비기록.
- 결정사항: 합성 완료 이력 1건의 실제 이전 조회 검증 성공으로 확정.
- 위험 요소: 상세 결과 소유권 및 이벤트 중복·장애 재시도 별도 미검증. 예상 밖 변경 없음.
- 다음 작업: 필요 시 상세·중복·장애 시나리오 추가 검증. Jira 변경 없음.

## 2026-09-30 — 동일 UID 원격 선연결 복구 수정 방향 검토

- 브랜치: develop.
- 작업 목표: Google/Apple 동일 이메일 로그인 오류의 후속 수정 범위 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: ProviderChangeGuard가 기존 Firebase binding의 현재 provider SocialIdentity 미등록 시 거절하고 ProviderLinkService.prepare도 Firebase 선연결을 사후 승인하지 않는 구현 재확인. 코드 변경 없음.
- 테스트와 결과: 소스 재조회 및 git diff --check 수행. 코드 미변경으로 Gradle 미실행.
- 유지한 계약: 이메일만으로 계정 연결/통합하지 않음, 다른 UID 및 타계정 provider 충돌·차단 유지. 기존 사용자 dirty 변경 보존.
- 결정사항: 기존 제공자 재인증·사용자 명시 동의 기반 선연결 복구 경로를 권장하며 상세 계약·구현은 아직 미확정.
- 위험 요소: guard 단순 제거 또는 SocialIdentity 자동 동기화는 기존 해제/차단 정책 우회 가능. 현재 정상 흐름 검증이 모든 출시 준비 완료를 의미하지 않음. 예상 밖 변경 없음.
- 다음 작업: 복구 정책 확정 후 서버·프론트 계약과 보안 회귀 테스트 설계/구현. Jira 변경 없음.

## 2026-09-30 — Apple/Google 연결 복구 검토 turn 기록

<!-- codex-turn:01a0f17b-6860-7ae0-97d1-3a57b7e70929 -->

- 브랜치: develop.
- 작업 목표: 동일 UID의 Firebase 선연결과 Identity 미등록 상태에 대한 수정 방향 정리.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 현재 로그인 guard 및 연결 prepare의 거절 조건 확인. 기존 제공자 재인증·명시 동의 기반 복구 경로 제안. 코드 변경 없음.
- 테스트와 결과: 소스 읽기 확인 및 git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 이메일만으로 자동 연결하지 않음, provider 차단·소유권 검사 유지, 비밀정보 비기록.
- 결정사항: guard 제거가 아닌 복구 절차 보완 권장. 상세 정책·구현 미확정.
- 위험 요소: 기존 해제/차단 우회 방지 회귀 필요. 예상 밖 변경 없음.
- 다음 작업: 정책 확정 후 서버·프론트 계약과 테스트 설계. Jira 변경 없음.

## 2026-09-30 — EMAIL_VERIFICATION 적용 조건 설명

<!-- codex-turn:01a0f276-318f-73c2-b4c5-d548d50ce4ad -->

- 브랜치: develop.
- 작업 목표: missingRequirements의 EMAIL_VERIFICATION 의미와 SNS 로그인 적용 여부 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: FirebaseEnrollmentRequirementResolver 및 FirebaseAdminAuthenticationVerifier에서 PASSWORD 연결 여부와 emailVerified 조건 확인. 애플리케이션 코드 변경 없음.
- 실행한 테스트와 결과: 소스와 프론트 계약 확인, git diff --check 수행. 설명만 수행하여 Gradle 테스트 미실행.
- 유지한 계약: 기존 enum 및 가입/승격 검증 조건 유지. 비밀정보 비기록, 기존 dirty 변경 보존.
- 결정사항: Firebase 이메일/비밀번호 계정의 이메일 소유 확인으로 설명. 순수 SNS에는 해당 조건이 없으며 PASSWORD가 함께 연결된 미인증 계정은 예외임을 안내.
- 위험 요소: 현재 로그인 제공자만 보고 인증 필요 여부를 판단하면 연결된 PASSWORD 방식 조건을 놓칠 수 있음. 이번 작업의 예상 밖 변경 없음.
- 다음 작업: 프론트는 배열 순서 대신 각 requirement 포함 여부로 필요한 절차를 안내. Jira 작업 없음.

## 2026-09-30 — 가입 전 공개 정책 버전 조회 필요성 확인

<!-- codex-turn:01a0f27a-43d9-7210-8c7d-deeadae61e61 -->

- 브랜치: develop.
- 작업 목표: 회원가입 동의 전 인증 없이 현재 정책 버전을 조회할 수 있는지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: UserController, UserConsentService, SecurityConfig 및 Firebase 응답 확인. 현재 내 동의 조회는 JWT와 현재 사용자 조회에 의존하며 공개 정책 조회 경로는 없음. 공개 GET /api/v1/policies/consents는 제안이며 미구현.
- 실행한 테스트와 결과: 코드 읽기로 인증 경계 및 ConsentPolicy 버전 검증 확인, git diff --check 수행. 분석 작업으로 Gradle 테스트 미실행.
- 유지한 계약: 개인 동의 상태 API 인증 유지, 가입 제출 시 서버의 현재 버전 검증 유지, 기존 dirty 변경 보존.
- 결정사항: 기존 privacyConsentVersion, termConsentVersion, qualityReviewConsentVersion 이름으로 공개 정책 버전을 제공하는 방향 권장. 공통 ConsentPolicy를 사용하여 검증과 조회 값 일치 필요.
- 위험 요소: 조회와 가입 사이 정책이 바뀔 수 있어 재조회·정책 내용 재표시·재동의 필요. 버전만 최신으로 자동 교체하면 사용자가 실제 본 내용과 동의 기록 불일치 가능. 정책 본문 제공 경로는 추가 확인 필요.
- 다음 작업: 공개 정책 조회 API와 프론트 재동의 흐름 구현 시 계약 및 인증 회귀 테스트 추가. 이번 작업의 예상 밖 변경 없음, Jira 변경 없음.

## 2026-09-30 — missingRequirements와 signup 입력 생략 가능 여부 검토

<!-- codex-turn:01a0f27e-be2c-7083-b7aa-6775133ef792 -->

- 브랜치: develop.
- 작업 목표: 프론트의 단계별 저장 가정과 signup 필수 필드 계약을 비교하고 수정 필요성 판단.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: requirement resolver, signup/upgrade DTO·서비스, enrollment entity, 응답과 프론트 계약 및 기존 테스트 확인. 일반 신규 가입은 PROFILE·CONSENTS 항상 포함, enrollment는 입력 초안을 보관하지 않음. Guest 승격에서 CONSENTS가 빠질 수 있는 예외 확인. 코드 수정 없음.
- 실행한 테스트와 결과: 기존 FirebaseExchangeServiceTests의 PROFILE·CONSENTS 포함 기대값 및 FirebaseGuestPrepareServiceTests의 PHONE_VERIFICATION·PROFILE만 포함하는 기대값 확인. 테스트 실행은 하지 않았으며 분석 작업으로 Gradle 미실행. git diff --check 수행.
- 유지한 계약: signup/upgrade 필수 입력 및 최종 정책 버전 검증 유지, enrollment 소유권·만료 경계 유지. 사용자 dirty 변경 보존.
- 결정사항: 신규 가입에는 입력 선택화나 enrollment 초안 조회 API가 불필요. missingRequirements는 요청 필드 생략 목록이 아니며 클라이언트 폼 입력을 최종 제출까지 보존. Guest 승격의 기존 동의는 인증된 내 동의 API로 확인 가능.
- 위험 요소: 신규 가입과 Guest 승격을 동일하게 해석하면 동의 누락이 발생할 수 있음. PHONE_VERIFICATION 외에 PASSWORD 계정의 EMAIL_VERIFICATION도 인증 상태에 따라 달라짐. 정책 변경 시 버전만 자동 교체하지 않고 필요한 재동의 수행. 실제 프론트 구현은 미검토.
- 다음 작업: 프론트에 신규 가입/Guest 승격 차이 전달, 공개 정책 조회 API는 별도 구현 범위로 유지. 예상 밖 변경 없음, Jira 작업 없음.

## 2026-09-30 — signup 입력과 Firebase 사전 인증 시점 설명

<!-- codex-turn:01a0f281-5b58-7311-bb96-2b359e06ba9b -->

- 브랜치: develop.
- 작업 목표: signup 전에 missingRequirements에서 인증 항목만 빠질 수 있다는 설명을 쉽게 풀어 전달.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 직전 확인한 resolver·signup·enrollment 구현을 근거로 프론트 폼 입력과 Firebase 인증 완료 상태의 저장 위치 및 전달 시점 차이를 설명. 코드 변경 없음.
- 실행한 테스트와 결과: 설명 작업으로 Gradle 미실행. 직전 코드 확인 결과를 재사용하고 git diff --check 수행.
- 유지한 계약: 신규 가입 PROFILE·CONSENTS 항상 요구, 최종 signup 필수 입력 유지. 비밀정보 비기록.
- 결정사항: 화면 입력만으로 서버 요구사항이 충족되지 않으며 signup에서 전달됨을 안내. Firebase 전화번호 인증은 signup 전에 완료 가능함을 예시로 설명.
- 위험 요소: PASSWORD 이메일 인증 및 Guest 기존 동의 예외를 구분해야 함. 기존 dirty 변경 보존, 이번 예상 밖 변경 없음.
- 다음 작업: 공개 정책 버전 조회 필요성은 별도 유지. Jira 변경 없음.

## 2026-09-30 — 신규 가입 폼 값 보관 책임 확인

<!-- codex-turn:01a0f285-a6a1-7032-beac-08ff3646d8ef -->

- 브랜치: develop.
- 작업 목표: 전화번호 사전 인증과 signup 필수 입력에 대한 사용자 이해 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 일반 신규 가입은 닉네임·동의·버전을 필수 제출하고 프론트에서 입력값을 보관해야 함을 설명. 코드 변경 없음.
- 실행한 테스트와 결과: 앞서 확인한 resolver와 DTO 근거 재사용. 설명 작업으로 Gradle 미실행, git diff --check 수행.
- 유지한 계약: signup 필수 입력 및 Firebase 인증 경계 유지, 비밀정보 비기록.
- 결정사항: 프론트가 자동으로 값을 보유하는 것이 아닌 화면 입력 후 제출/재시도까지 보관하는 책임으로 명확화.
- 위험 요소: 새로고침·앱 종료 등으로 폼 값이 사라지면 다시 입력받아야 함. Guest 승격의 기존 동의 예외와 구분. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 프론트의 폼 보관/재입력 흐름 적용. Jira 변경 없음.

## 2026-09-30 — signup 선택 동의와 공개 정책 조회 수정 방향 설명

- 브랜치: develop.
- 작업 목표: Firebase 신규 가입 선택 품질 검토 동의 및 가입 전 공개 정책 조회의 변경 계약 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 기존 UserConsentUpdateRequest, ConsentPolicy, UserFactory, UserConsents를 근거로 재사용 방향 정리. signup DTO·서비스·factory 확장과 공개 정책 조회 controller/DTO 및 GET 인증 허용 제안. 애플리케이션 미구현.
- 실행한 테스트와 결과: 기존 구현 소스 확인, git diff --check 수행. 구현 전 설명 단계로 Gradle 미실행.
- 유지한 계약: 기존 선택 동의 필드명, 필수 개인정보·약관 검증, 기존 클라이언트 누락 시 false, 개인 동의 API 인증 유지.
- 결정사항: true일 때 현재 선택 정책 버전 검증, false일 때 버전 생략 가능. 공개 조회는 사용자 상태 없이 동일 ConsentPolicy의 세 버전을 반환. 신규 엔티티·환경변수 추가 불필요.
- 위험 요소: 공개 조회 이후 정책 변경 시 실제 재동의 필요. Guest upgrade에 같은 입력을 추가하는 변경은 이번 신규 signup 설명 범위와 별도로 구분. 실제 정책 내용과 버전의 대응은 프론트에서도 관리 필요.
- 다음 작업: DTO·서비스·factory·공개 GET·계약 문서 수정 및 선택 동의 저장/버전 검증/익명 조회/기존 보호 경로 테스트 구현. 기존 dirty 변경 보존, 이번 예상 밖 변경 없음, Jira 작업 없음.

## 2026-09-30 — signup 선택 동의·공개 조회 설명 turn 기록

<!-- codex-turn:01a0f287-75d4-73d3-9397-eb6175e4ad11 -->

- 브랜치: develop.
- 작업 목표: 이번 수정 방향 설명의 turn 식별자를 포함한 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Firebase signup에 기존 선택 동의 두 필드 추가 및 공개 GET /api/v1/policies/consents 제안 기록. 실제 API 구현 없음.
- 실행한 테스트와 결과: 기존 동의 DTO·factory·엔티티 소스 확인, git diff --check 통과. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 개인정보·약관 필수, 선택 동의 누락은 false, true일 때 현재 버전 검증, 개인 API 인증 유지.
- 결정사항: 기존 ConsentPolicy와 UserConsents 재사용, 새 엔티티·환경변수 불필요. Guest upgrade 확장은 별도 범위.
- 위험 요소: 조회 후 정책 변경 시 재동의 필요. 구현·배포는 아직 수행하지 않음.
- 다음 작업: 설명한 두 변경 구현 및 관련 테스트. 기존 dirty 변경 보존, 예상 밖 변경 없음, Jira 변경 없음.

## 2026-09-30 — Guest upgrade 선택 동의 확장 방향 추가

- 브랜치: develop.
- 작업 목표: Guest upgrade의 학습 품질 검토 선택 동의 입력 누락 확인 및 수정 계획 확장.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: FirebaseGuestUpgradeRequest와 User.promoteGuestToFederatedMember 및 UserConsents.renewRequiredConsents 확인. 요청에 선택 동의 두 필드가 없고 현재 승격이 기존 선택 동의를 보존함을 확인. 실제 코드 구현 없음.
- 실행한 테스트와 결과: 소스 읽기 확인 및 git diff --check 수행. 방향 정리 단계로 Gradle 미실행.
- 유지한 계약: 기존 필드명 및 Guest 승격 소유권/필수 동의 검증 유지, 누락 요청의 기존 동의 보존 제안. 기존 dirty 변경 보존.
- 결정사항: signup 선택 동의·공개 정책 GET에 더해 upgrade 선택 동의 입력 추가를 범위에 포함. 신규 signup 누락은 false, upgrade 누락은 기존 상태 보존. 명시 false는 미동의 처리, true는 현재 버전 검증 후 반영.
- 위험 요소: upgrade 누락을 false로 기본화하면 기존 Guest 선택 동의가 의도 없이 철회될 수 있음. 선택 동의는 missingRequirements의 필수 단계로 추가하지 않음.
- 다음 작업: 세 API 변경 구현 시 기존 동의 보존/명시 철회/명시 동의/버전 오류 회귀 검증 추가. 예상 밖 변경 없음, Jira 변경 없음.

## 2026-09-30 — Guest upgrade 선택 동의 검토 turn 기록

<!-- codex-turn:01a0f289-7e99-7d31-a542-d6c0f23667ca -->

- 브랜치: develop.
- 작업 목표: Guest upgrade 선택 동의 확장 검토에 이번 turn 식별자를 포함한 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 요청 DTO에 선택 동의 필드가 없음을 확인하고 추가 방향 정리. 승격 시 기존 동의를 보존하는 현재 동작 확인. 애플리케이션 코드 변경 없음.
- 실행한 테스트와 결과: DTO·승격·동의 소스 확인 및 git diff --check 통과. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 기존 선택 동의 필드명, 필수 동의 검증 및 Guest 소유권 유지. 비밀정보 비기록.
- 결정사항: upgrade 필드 누락은 기존 동의 유지, 명시 false는 미동의 처리, true는 현재 버전 검증 후 저장하는 방향 제안.
- 위험 요소: 누락을 false로 기본화하면 기존 동의가 의도 없이 철회될 수 있음. 아직 구현·배포하지 않음.
- 다음 작업: signup·upgrade 선택 동의와 공개 정책 조회 구현 및 회귀 검증. 기존 dirty 변경 보존, 예상 밖 변경 없음, Jira 변경 없음.

## 2026-09-30 — Guest upgrade enrollment 사용 설명

- 브랜치: develop.
- 작업 목표: Guest 승격도 enrollment를 사용하는지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: FirebaseGuestPrepareService의 GUEST_USER enrollment 발급/재사용 및 FirebaseGuestUpgradeService의 소유권·유효성·Firebase 일치 검증 확인. 코드 변경 없음.
- 실행한 테스트와 결과: 서비스 소스 확인, git diff --check 수행. 설명 작업으로 Gradle 미실행.
- 유지한 계약: Guest 토큰 사용자 ID 기준 소유권 검증, Firebase 계정 일치 및 enrollment 유효성 유지.
- 결정사항: 신규 signup과 동일한 enrollment 구조를 쓰되 Guest prepare에서 받은 승격용 ID를 제출해야 함을 설명.
- 위험 요소: 신규 가입용 enrollment를 Guest 승격에 대체 사용할 수 없음. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 선택 동의 확장 및 공개 정책 조회 계획 유지. Jira 작업 없음.

## 2026-09-30 — Guest upgrade enrollment 설명 turn 기록

<!-- codex-turn:01a0f28b-a259-7003-b0dc-9ce02b89313b -->

- 브랜치: develop.
- 작업 목표: Guest upgrade enrollment 설명에 이번 turn 식별자를 포함한 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: guest/prepare가 발급한 GUEST_USER enrollment를 guest/upgrade에 제출하며 Guest 소유권·Firebase 계정 일치·유효성을 검증하는 현재 구현 확인. 애플리케이션 코드 변경 없음.
- 실행한 테스트와 결과: 서비스 소스 확인 및 git diff --check 수행. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 인증된 Guest 기준 소유권 및 enrollment 유효성 검사 유지. 비밀정보 비기록.
- 결정사항: 신규 signup과 같은 엔티티 구조를 사용하지만 신규 가입용 ID와 승격용 ID는 대체 불가. enrollment는 닉네임·동의 초안 저장 용도가 아님.
- 위험 요소: 가입/승격 ID 혼용 시 검증 실패. 기존 dirty 변경 보존, 이번 예상 밖 변경 없음.
- 다음 작업: 선택 동의 확장 및 공개 정책 조회 계획 유지. Jira 변경 없음.

## 2026-09-30 — 신규 signup MEMBER와 Firebase 연결 시점 설명

- 브랜치: develop.
- 작업 목표: 신규 가입 완료 시 MEMBER도 Firebase 계정에 연결되는지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: FirebaseExchangeService의 DIRECT_SIGNUP·boundUserId=null 및 FirebaseSignupService/TransactionService의 새 MEMBER 생성·FirebaseIdentity 저장 확인. 코드 변경 없음.
- 실행한 테스트와 결과: 소스 읽기 확인, git diff --check 수행. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 가입 전 Firebase 소유권 검증 및 가입 성공 시 회원·Firebase 매핑 원자적 저장 유지.
- 결정사항: 신규 가입도 Firebase 계정과 연결되며 가입 전 enrollment 연결과 가입 완료 후 MEMBER 매핑을 구분해서 설명.
- 위험 요소: enrollment 자체가 영구 회원 연결 문서라고 오해하지 않도록 FirebaseIdentity와 구분. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 선택 동의 확장 및 공개 정책 조회 계획 유지. Jira 변경 없음.

## 2026-09-30 — 신규 signup Firebase 연결 설명 turn 기록

<!-- codex-turn:01a0f28d-9078-7533-a411-467c1c3c6a4a -->

- 브랜치: develop.
- 작업 목표: 신규 signup Firebase 연결 설명에 이번 turn 식별자를 포함한 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 가입 전 DIRECT_SIGNUP enrollment의 Firebase 연결 및 가입 성공 시 새 MEMBER와 FirebaseIdentity 매핑 저장 확인. 애플리케이션 코드 변경 없음.
- 실행한 테스트와 결과: 서비스·트랜잭션 소스 확인 및 git diff --check 수행. 설명 작업으로 Gradle 미실행.
- 유지한 계약: Firebase 소유권 검증 및 회원·Firebase 매핑 원자적 저장 유지. 비밀정보 비기록.
- 결정사항: 신규 signup은 새 userId 생성, Guest upgrade는 기존 userId 유지. enrollment와 영구 FirebaseIdentity 매핑 역할 구분.
- 위험 요소: enrollment를 영구 회원 연결 문서로 오해하지 않도록 안내. 기존 dirty 변경 보존, 이번 예상 밖 변경 없음.
- 다음 작업: 선택 동의 확장 및 공개 정책 조회 계획 유지. Jira 변경 없음.

## 2026-09-30 — enrollment 유효기간과 DB 삭제 시점 설명

- 브랜치: develop.
- 작업 목표: enrollment가 TTL 초과 시 삭제되는지 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: FirebaseEnrollmentAttempt의 expiresAt·cleanupAt, lifecycle 삭제 예약, 만료 capture 및 application 기본 설정 확인. 코드 변경 없음.
- 실행한 테스트와 결과: 소스·설정 읽기 및 git diff --check 수행. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 만료 시 서버 검증으로 재사용 차단, DB 삭제와 회원·Firebase 연결의 별도 생명주기 유지.
- 결정사항: 기본 10분은 가입 시도 유효기간이며 즉시 삭제 시간이 아님. 가입 완료 후 lifecycle은 기본 24시간 뒤 TTL 정리를 예약. 미완료 건은 별도 정리 절차에 의존.
- 위험 요소: 실제 TTL 인덱스 및 배포 환경 설정 미확인. MongoDB TTL 삭제는 비동기로 정각 삭제 보장 없음. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 필요 시 실제 정리 설정·인덱스 별도 확인. 선택 동의·공개 정책 조회 계획 유지, Jira 변경 없음.

## 2026-09-30 — enrollment TTL 설명 turn 기록

<!-- codex-turn:01a0f28f-d025-7981-8040-2cb687b69152 -->

- 브랜치: develop.
- 작업 목표: enrollment 유효기간과 삭제 시점 설명의 turn 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: expiresAt과 cleanupAt 역할, 가입 완료 후 삭제 예약 및 중단 가입 별도 정리 경로를 소스 기준으로 설명. 코드 변경 없음.
- 실행한 테스트와 결과: 엔티티·lifecycle·설정 소스 확인, git diff --check 수행. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 만료 enrollment 재사용 차단 및 MEMBER/FirebaseIdentity의 별도 생명주기 유지. 비밀정보 비기록.
- 결정사항: 기본 유효기간 10분은 즉시 DB 삭제 시간이 아니며 가입 완료 후 기본 24시간 보관 뒤 TTL 정리 예약. 실제 환경 설정과 구분.
- 위험 요소: 배포 환경 설정·DB TTL 인덱스 미확인, MongoDB TTL 삭제는 비동기. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 선택 동의 확장 및 공개 정책 조회 계획 유지. 필요 시 실환경 정리 설정 별도 확인, Jira 변경 없음.

## 2026-09-30 — Guest 승격 enrollment 정리 동일성 확인

- 브랜치: develop.
- 작업 목표: Guest 승격에도 동일한 enrollment 만료·삭제 흐름 적용 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: FirebaseGuestUpgradeTransactionService의 소비 처리 및 공통 finalizeEnrollment 호출 확인. 코드 변경 없음.
- 실행한 테스트와 결과: 소스 확인 및 git diff --check 수행. 설명 작업으로 Gradle 미실행.
- 유지한 계약: enrollment 소비·만료 경계 및 회원/Firebase 연결의 별도 생명주기 유지.
- 결정사항: 기본 10분 유효기간과 완료 후 기본 24시간 삭제 예약 흐름이 Guest 승격에도 동일함을 안내.
- 위험 요소: 실제 배포 설정·TTL 인덱스 미확인. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 선택 동의 및 공개 정책 조회 계획 유지. Jira 변경 없음.

## 2026-09-30 — TMI-136 하위 선택 동의·공개 정책 조회 이슈 초안

- 브랜치: develop.
- Jira: TMI-136
- 작업 목표: 기존 논의한 SNS 가입·Guest 승격 선택 동의 및 공개 정책 조회 작업을 TMI-136 하위 이슈로 준비.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 공식 Atlassian 도구로 부모 에픽 sns 로그인, 프로젝트 작업 유형 및 기존 하위 이슈 확인. 제목·계약·완료 조건을 포함한 신규 작업 초안 작성. 코드 변경 없음.
- 실행한 테스트와 결과: Jira 읽기 조회 성공, git diff --check 수행. 초안 준비 작업으로 Gradle 미실행.
- 유지한 계약: 기존 필드명, 신규 signup 누락 false, Guest upgrade 누락 기존 동의 보존, 개인 동의 API 인증, enrollment 경계 유지.
- 결정사항: TMI-136 에픽 아래 작업 1건으로 세 API 변경 통합. AGENTS.md에 따라 내용을 먼저 제시하고 승인 후 생성.
- 위험 요소: 사용자 승인 전이라 Jira 생성 미실행. 기존 하위 이슈 제목에서 직접 중복 작업 발견하지 않음, 전체 기존 본문 중복 조사 미수행. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- Jira 작업: 부모/유형/자식 조회만 수행. 댓글 등록·상태 변경 없음. 생성 승인 대기.
- 다음 작업: 사용자 초안 승인 시 생성하고 부모 연결 재조회. Jira 댓글 초안: 선택 동의 및 공개 정책 조회 이슈 범위 준비 완료, 구현·테스트는 후속 작업에서 수행 예정. 자동 등록하지 않음.

## 2026-09-30 — TMI-136 하위 Jira 초안 검토 turn 기록

<!-- codex-turn:01a0f295-c7b4-75e3-ab58-00091111be92 -->

- 브랜치: develop.
- Jira: TMI-136
- 작업 목표: 하위 작업 생성 초안 검토의 turn 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 부모 에픽과 작업 유형 및 기존 자식 조회, signup·upgrade 선택 동의와 공개 정책 조회를 묶은 작업 제목·계약·완료 조건 준비 및 사용자에게 제시. 코드 변경 없음.
- 실행한 테스트와 결과: 공식 Atlassian 읽기 조회 성공, git diff --check 수행. 초안 작업으로 Gradle 미실행.
- 유지한 계약: 선택 동의 필드명, Guest 누락 시 보존, 필수 동의 및 개인 API 인증 유지.
- 결정사항: AGENTS.md의 사전 내용 공개·승인 규칙에 따라 생성 승인 대기.
- 위험 요소: 아직 신규 Jira 이슈는 생성되지 않음. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- Jira 작업: 읽기 조회만 수행, 생성·댓글·상태 변경 없음, 생성 승인 대기.
- 다음 작업: 사용자 승인 후 신규 작업 생성 및 부모 연결 확인. Jira 댓글 초안은 직전 기록 유지, 자동 등록하지 않음.

## 2026-09-30 — TMI-188 선택 동의·공개 정책 조회 작업 생성

- 브랜치: develop.
- Jira: TMI-188
- 작업 목표: 승인된 초안으로 TMI-136 하위 작업 생성.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 공식 Atlassian 도구로 작업 생성 및 부모 TMI-136 연결 재조회 확인. 애플리케이션 코드 변경 없음.
- 실행한 테스트와 결과: 생성 성공 및 제목·부모·해야 할 일 상태 확인, git diff --check 수행. Jira 작업만 수행하여 Gradle 미실행.
- 유지한 계약: 승인한 signup·upgrade 선택 동의 및 공개 정책 조회 범위, 기존 인증/필수 동의/enrollment 계약 유지.
- 결정사항: 신규 이슈 TMI-188로 후속 구현 범위 관리. 구현 미시작.
- 위험 요소: 서버 변경·배포는 아직 없음. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- Jira 작업: TMI-188 생성, 사용자 명시 승인 후 수행. 댓글 등록 없음, 상태 전환 없음(기본 해야 할 일).
- 다음 작업: TMI-188 기준 구현 및 검증. Jira 댓글 초안: 작업 생성 및 TMI-136 부모 연결 확인 완료, 코드 변경 없음, 구현 검증은 후속 작업 예정. 자동 등록하지 않음.

## 2026-09-30 — TMI-188 생성 완료 turn 기록

<!-- codex-turn:01a0f298-f345-7770-9d5a-e9dcdeab20e2 -->

- 브랜치: develop.
- Jira: TMI-188
- 작업 목표: 승인된 Jira 생성 작업의 turn 식별자를 포함한 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 공식 Atlassian 도구로 SNS signup·Guest upgrade 선택 동의 및 공개 정책 조회 작업 생성. 부모 TMI-136 연결 재조회 확인. 코드 변경 없음.
- 실행한 테스트와 결과: Jira 생성 성공 및 부모·기본 상태 확인, git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 승인된 이슈 범위 및 기존 필수 동의·인증·enrollment 계약 유지. 비밀정보 비기록.
- 결정사항: 후속 구현은 TMI-188 기준으로 진행, 현재 구현 미시작.
- 위험 요소: API 구현·배포는 아직 없음. 기존 dirty 변경 보존, 예상 밖 변경 없음.
- Jira 작업: 사용자 명시 승인 후 TMI-188 생성. 댓글 등록 없음, 상태 전환 없음, 기본 상태 해야 할 일.
- 다음 작업: TMI-188 구현 및 검증. Jira 댓글 초안은 직전 기록 유지, 자동 등록하지 않음.

## 2026-09-30 — TMI-188 SNS 가입·Guest 승격 선택 동의 및 공개 정책 조회 구현

<!-- codex-turn:01a0f29b-84b6-7b03-92bc-008f0e80c92b -->

- 브랜치: feat/TMI-188-quality-review-consent.
- Jira: TMI-188
- 작업 목표: 승인된 이슈의 signup/upgrade 선택 동의 저장 및 가입 전 익명 정책 버전 조회 구현.
- 변경 파일: FirebaseSignupRequest, FirebaseGuestUpgradeRequest, FirebaseSignupService, FirebaseGuestUpgradeService, FirebaseExchangeController, UserFactory, User, SecurityConfig, IdentityOpenApiExamples. 신규 ConsentPolicyController·CurrentConsentPolicyResponse. 테스트 FirebaseSignupServiceTests·FirebaseGuestUpgradeServiceTests·FirebaseExchangeControllerTests·SecurityIntegrationTests·OpenApiSharingTests 및 신규 FirebaseConsentRequestTests. 문서 docs/contracts/frontend-firebase-auth-integration-guide.md, docs/contracts/frontend-firebase-auth-integration-appendix.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 구현 내용: 기존 선택 동의 필드명/검증 재사용. signup 누락/null은 false, upgrade 누락/null은 기존 선택 동의 보존, 명시 false는 버전·시각 비움, true는 현재 버전 검증 후 저장. 유효한 기존 동의 시각은 보존하고 새 동의/버전 갱신은 서버 시각 사용. 공개 GET /api/v1/policies/consents는 동일 ConsentPolicy의 현재 세 버전을 BaseResponse.result에 반환하며 no-store 사용. 사용자 상태 및 정책 본문은 반환하지 않음. OpenAPI/프론트 요청 예시·누락 처리·버전 오류/재동의·QA 갱신.
- 실행한 테스트와 결과: 최초 샌드박스 Gradle 캐시 잠금 접근 실패 후 승인된 권한으로 실행. 첫 전체 테스트는 983개 중 Swagger endpoint count 기대값 1건 실패. 새 공개 GET로 24→25 operation 및 23→24 path 변경 반영, Swagger 응답 예시 추가 후 ./gradlew clean test 최종 통과(983 tests, failures=0, errors=0). git diff --check 통과. 테스트에서 외부 Provider/Repository는 mock 사용, 실제 외부 인증/DB 호출 없음.
- 유지한 계약: 개인정보/약관 필수 동의, 개인 동의 GET/PUT JWT 인증, Guest userId 유지, enrollment 소유권·만료·소비와 기존 트랜잭션 유지. 선택 동의는 missingRequirements에 추가하지 않음. 구버전 JSON 요청 호환, 새 엔티티·환경변수 없음.
- 결정사항: Guest upgrade는 Boolean null을 유지하여 보존/철회 구분. 기존 domain 승격 overload는 보존 동작으로 위임. 공개 정책 API는 해당 GET만 permitAll.
- 위험 요소: 배포·모바일 E2E는 미수행. 프론트 정책 본문/URL과 조회 버전의 대응 확인 필요, 조회 이후 version 변경 시 재동의 처리 필요. 실환경 정책 설정은 기존 환경변수 재사용.
- 예상 밖 변경: 이번 작업 범위 밖 추가 변경 없음. 시작 전부터 dirty였던 identity-branch-deployment.md, identity-test-container-secrets.json, identity-test-task-definition.draft.json, DeploymentTargetTests.java 및 docs/postman·tools는 수정하지 않고 보존. 기존 작업 기록도 보존.
- 배포 전 확인: 대상 환경의 세 정책 설정·본문/URL 대응 및 새 GET 익명 조회, signup/upgrade의 선택 동의 처리와 개인 API 인증 확인. 프론트는 signup/upgrade의 누락 규칙 차이를 반영.
- Jira 작업: 구현 전 TMI-188 설명 조회만 수행. 상태 전환·댓글 등록 없음. 생성 승인과 별개로 이번 구현 완료 댓글은 승인 전 미등록.
- Jira 댓글 초안: signup/upgrade 선택 동의 및 익명 정책 조회 구현 완료. 관련 DTO·서비스·domain·보안·Swagger·계약 문서/테스트 갱신. clean test 983개 통과. 대상 환경 배포와 정책 본문/버전 대응 및 모바일 연동 검증 필요. 자동 등록하지 않음.
- 다음 작업: 사용자 diff 검토 후 직접 커밋/push. 필요 시 승인받은 Jira 댓글 및 후속 배포·모바일 검증. 커밋/push/PR 생성 및 배포는 이번 작업에서 수행하지 않음.

## 2026-10-01 — TMI-189 Firebase 동일 UID SNS 최초 연결의 단일 로그인 구현

<!-- codex-turn:01a0f58d-ed66-7321-b9da-8badaf7639dd -->

- 브랜치: feat/TMI-189-firebase-provider-auto-link.
- Jira: TMI-189
- 작업 목표: 기존 ACTIVE MEMBER의 같은 Firebase 프로젝트/UID에 연결된 현재 Google/Apple을 안전 조건 확인 후 등록하여 정상 최초 연결에 추가 기존 SNS 재로그인을 요구하지 않음.
- 변경 파일: 신규 ProviderLoginRegistrationService.java, ProviderLoginRegistrationServiceTests.java. FirebaseExchangeService.java, FirebaseAdminAuthenticationVerifier.java, FirebaseSdkAdminClient.java, ProviderChangeGuard.java, AuthMethodChangeControl.java, ProviderChangeConfiguration.java. 테스트 FirebaseExchangeServiceTests.java, FirebaseAdminAuthenticationVerifierTests.java, FirebaseSdkAdminClientTests.java, ProviderChangeConfigurationTests.java. docs/contracts/frontend-firebase-auth-integration-guide.md, frontend-firebase-auth-integration-appendix.md, docs/codex/CURRENT_STATE.md, WORKLOG.md.
- 구현 내용: LOGIN_EXCHANGE에 한해 Guard의 미등록 Google/Apple 판단을 등록 서비스로 위임하되 block/floor는 즉시 검사. Firebase SDK에서 서명된 현재 제공자 subject 증빙과 Admin 최신 연결을 교차 확인(누락/다중/불일치 거절). 등록 서비스는 exact Firebase binding·ACTIVE MEMBER·세션 epoch 및 인증 경계를 재검증하고 현재 provider만 저장. 세션 control을 touch하여 unlink/withdrawal/logout-all 등과 CAS 경합하고 제공자 revision 증가로 오래된 PREPARED intent를 무효화. 등록·기존 소유권 검사·세션 발급을 동일 Mongo 트랜잭션에서 실행. 과거 block/floor, 기존 같은 provider의 다른 subject, 다른 회원 소유, active security slot/미완료 작업은 자동 승인하지 않음. duplicate key는 409, 일반 저장/트랜잭션 장애는 기존 진단을 포함한 503으로 안전하게 실패하며 다음 제한 재시도에서 동일 회원의 기등록 연결로 수렴. 기존 탈퇴/비활성 오류 선행 검사 유지.
- 실행한 테스트와 결과: 초기 새 mock 테스트에서 Mockito 재스텁/중첩 mock 생성 오류가 발생하여 테스트 초기화를 수정. Gradle 캐시 권한 제한은 승인된 실행으로 해결. 최종 ./gradlew clean test BUILD SUCCESSFUL, XML 집계 tests=1009, failures=0, errors=0, skipped=0. git diff --check 통과. Google↔Apple 정상 등록, 현재 provider만 등록, 중복 요청, 소유권/교체/이력/Guest/UID/binding/탈퇴 거절, 트랜잭션 진입 시 unlink/logout 경합, CAS 실패, 발급 예외, 로그인 목적 한정, 증빙 불일치, fence OFF의 거절과 기존 signed JWT 발급 계약 테스트 포함. 신규 테스트의 외부 Provider/Repository는 mock; 실제 Atlas/OAuth는 호출하지 않음.
- 유지한 계약: /api/v1/auth/firebase/exchange 요청/응답 및 UUID userId, RS256/JWKS/audience/MEMBER 토큰 계약 불변. signup/Guest/sync/high-risk 경로에 자동 등록 없음, Kakao 자동 등록 제외. Google/Apple의 SDK 증빙 교차 검증은 공통 검증 어댑터에 적용됨. 이메일만으로 병합·UID rebind·다른 제공자 일괄 등록·기존 차단 해제·LC/Billing 변경 없음.
- 결정사항: 기존 AUTH_SESSION_FENCE_ENABLED=true에서 등록 서비스를 설치하고 OFF에서는 미등록 SNS를 거절한다. 명시 link capture와 별개이며 새로운 설정/컬렉션 없음. auto registration 후 provider control이 생성되므로 sync 사용 환경에서는 FIREBASE_PROVIDER_CHANGE_FENCE_ENABLED를 확인한다(OFF+보안 상태 존재 시 기존 sync 거절 유지). 충돌 시 무한 자동 재시도/성공 간주 금지. 프론트 정상 경로와 409/503 대응·배포 QA 문서화.
- 위험 요소: mock 테스트는 실제 replica-set write conflict/rollback의 증명이 아님. 테스트 환경 E2E와 모바일 Firebase 자동 연결·subject claim 형태는 배포 후 확인 필요. 대상 환경 feature flag, 기존 unique index 및 모든 보안 writer의 동시 배포 호환성 확인 필요. 배포/외부 데이터 보정/실제 SNS 로그인 미수행이므로 Jira 전체 완료로 판단하지 않음.
- 예상 밖 변경: 없음. 시작 전부터 WORKLOG에 존재한 누적 미커밋 변경은 그대로 보존했고 과거 항목은 변경하지 않음. 이번 작업은 코드/테스트/계약/작업 기록 범위만 변경.
- 배포 전 확인: AUTH_SESSION_FENCE_ENABLED, 필요 시 provider-change fence, Mongo 트랜잭션 및 social_identities provider+subject unique 인덱스. Google→Apple/Apple→Google 동일 회원 로그인을 테스트 환경에서 재현, 다른 UID/차단/교체/동시 보안 작업 실패 및 rollback 확인. 기존 전화번호·가입·Guest·명시 link 회귀 확인.
- Jira 작업: 공식 Atlassian MCP로 TMI-189 설명/완료 조건 조회만 수행. 사용자의 개발 요청에 따라 구현했으며 이슈 수정·상태 전환·댓글 등록 없음.
- Jira 댓글 초안(미등록): TMI-189 로컬 구현 완료. ProviderLoginRegistrationService 및 Exchange/Guard/SDK/설정·테스트·프론트 계약 갱신. 동일 UID ACTIVE MEMBER의 현재 Google/Apple만 안전 조건하에 등록하고 세션 발급과 트랜잭션 결합. clean test 1009개 및 diff check 통과. 실제 Firebase E2E/replica-set 경합 검증과 배포 설정 확인은 남아 있음.
- 다음 작업: 사용자 diff 검토 후 직접 커밋/push. 별도 승인된 테스트 배포 및 E2E 후 PR 병합/완료 조건 확인. 이번 작업에서 커밋/push/배포/PR 생성 및 Jira 변경은 수행하지 않음.

## 2026-10-01 — TMI-189 구현 코드 설명

<!-- codex-turn:01a0f5c9-f303-7da2-b06f-4d7a367122e2 -->

- 브랜치: feat/TMI-189-firebase-provider-auto-link.
- Jira: TMI-189
- 작업 목표: 실제 구현을 코드 근거와 함께 사용자에게 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md만 이번 turn 갱신. 기존 코드/테스트/계약 문서 dirty 변경 보존.
- 구현 내용: 추가 구현 없음. FirebaseSdkAdminClient의 signed subject와 Admin providerData 비교, ProviderChangeGuard의 로그인 목적 한정 위임, FirebaseExchangeService의 등록 후 기존 토큰 발급, ProviderLoginRegistrationService의 binding/회원/소유권/보안 상태 검사와 트랜잭션, SessionSecurityService의 control version 경합, 조건부 Bean 설치를 재확인.
- 실행한 테스트와 결과: 코드 변경 없는 설명 작업이라 테스트 재실행하지 않음. 기존 XML 결과 tests=1009, failures=0, errors=0 확인. git diff --check 수행.
- 유지한 계약: exchange DTO/userId/JWT 유지, 이메일 자동 병합·다른 UID 연결·차단 해제·다른 provider 일괄 등록 없음. 현재 Google/Apple 최초 등록만 허용.
- 결정사항: Guard의 return은 로그인 성공이 아니라 최종 등록 서비스로 판단을 넘기는 것임을 명시. epoch/CAS와 제공자 revision 역할, 경합 후 제한 재시도와 응답 멱등성 차이를 구분하여 설명.
- 위험 요소: 실제 Firebase 및 Mongo replica-set E2E/경합/rollback 미검증. AUTH_SESSION_FENCE_ENABLED=true 필요, sync 사용 시 provider-change fence 확인 필요. 기존 로컬 테스트 성공을 배포 완료로 해석하지 않음.
- 예상 밖 변경: 없음. Jira 조회/변경, 커밋/push/배포 미수행.
- Jira 댓글 초안(미등록): 구현 코드 흐름과 보안 조건 설명 완료. 애플리케이션 추가 변경 없음. 기존 1009개 성공 결과 확인, 대상 환경 E2E/경합 검증은 남아 있음.
- 다음 작업: 사용자 변경 검토 후 직접 커밋/push 및 승인된 테스트 배포·실환경 검증.

## 2026-10-01 — TMI-189 Kakao 최초 연결 확장 범위 설명

<!-- codex-turn:01a0f5d2-f322-7111-a23f-f7cb5e59783d -->

- 브랜치: feat/TMI-189-firebase-provider-auto-link.
- Jira: TMI-189
- 작업 목표: Google/Apple 한정 이유와 향후 Kakao 동일 로그인 UX 지원에 필요한 변경 설명.
- 변경 파일: docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md. 애플리케이션 추가 수정 없음.
- 구현 내용: 기존 FirebaseAdminAuthenticationVerifier의 Kakao 설정 ID 매핑/활성 검사와 Guard·ProviderLoginRegistrationService·FirebaseSdkAdminClient의 Google/Apple 한정 조건 재확인. 기존 Kakao 로그인 지원과 새 미등록 제공자 자동 등록 지원을 구분.
- 실행한 테스트와 결과: 관련 소스 조회 및 git diff --check. 설명 작업이므로 Gradle 재실행 없음.
- 유지한 계약: 기존 Kakao 일반 로그인/명시 연결 동작 유지. 현재 TMI-189 자동 등록은 Google/Apple 한정. 비밀정보 비기록.
- 결정사항: 기술적 제약이 아닌 기존 이슈 범위 제한임을 설명. Kakao 확장은 활성화된 지원 제공자 정책·설정 OIDC ID 기반 signed subject와 최신 원격 연결 교차 검증·소유권/차단/교체/경합 테스트를 함께 변경하는 방향 권장. 단순 KAKAO 조건 추가는 불충분.
- 위험 요소: 실제 Kakao OIDC 토큰의 증빙 형태와 Firebase 연결 동작은 실환경 확인 필요. 확인되지 않은 provider를 포괄 허용하지 않음.
- Jira 작업: 변경 없음. 댓글 초안(미등록): Kakao 자동 등록 확장 지점 분석, 코드 변경 없음, 실제 OIDC 증빙 검증과 회귀 테스트 필요.
- 예상 밖 변경: 없음. 기존 dirty 변경 보존.
- 다음 작업: 사용자 구현 요청 및 필요 시 Jira 범위 변경 승인 후 Kakao를 포함한 공통 정책 구현/검증.

## 2026-10-01 — 테스트 배포와 Kakao 활성 설정 운영 순서 안내

<!-- codex-turn:01a0f5e2-83d1-7f90-a47f-d1d96e61114e -->

- 브랜치: feat/TMI-189-firebase-provider-auto-link.
- Jira: TMI-189
- 작업 목표: 현재 변경 배포 후 Kakao 준비 순서 및 활성 설정 유지 이유 설명.
- 변경 파일: docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 구현 내용: 코드 추가 변경 없음. application.yml의 FIREBASE_KAKAO_ENABLED 기본 false 및 설정된 OIDC ID 매핑/활성 검사 확인. 서버 활성 플래그와 Firebase provider 등록·프론트 버튼 노출을 구분.
- 실행한 테스트와 결과: 소스 읽기 및 git diff --check. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 사용자가 commit/push 수행, 미준비 제공자 자동 허용 없음. 현재 Google/Apple 자동 등록 범위 유지.
- 결정사항: 현재 변경 테스트 배포·Google/Apple 재현 검증 후 Kakao 준비, 미등록 provider 자동 등록/subject 증빙 검증 확장 및 회귀 검증, 서버 ON/프론트 노출 순서 권장. 장기적으로 Kakao를 지원하더라도 환경별 준비/운영 제어 설정은 유지하고 정상 운영에서는 ON으로 유지 가능.
- 위험 요소: Kakao 설정만 ON해도 TMI-189 자동 등록 범위가 확장되지는 않음. 기존 Kakao 전용 회원이 생긴 뒤 OFF하면 로그인 제한이 발생하므로 영향 평가 필요. 실제 배포 및 Firebase/Kakao 설정 미확인.
- 예상 밖 변경: 없음. 기존 dirty 변경 보존. Jira 변경/댓글 등록 및 commit/push/배포 미수행.
- Jira 댓글 초안(미등록): 배포 및 Kakao 준비/활성화 순서 설명, 코드 변경 없음. 실제 OIDC 준비·자동 등록 확장·E2E 남음.
- 다음 작업: 사용자 변경 검토·직접 commit/push 후 테스트 배포 검증. 이후 승인된 Kakao 설정/코드 확장 진행.

## 2026-10-01 — Kakao 연결 불일치 해결 의도 확인

<!-- codex-turn:01a0f5e3-7766-74d1-8e15-e6f4d7d515b4 -->

- 브랜치: feat/TMI-189-firebase-provider-auto-link.
- Jira: TMI-189
- 작업 목표: 사용자의 Kakao 확장 이해 확인.
- 변경 파일: docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 구현 내용: 추가 구현 없음. 같은 Firebase UID에 Kakao가 연결됐지만 Identity에 미등록인 경우 안전 조건하에 현재 provider 등록을 지원하려는 의미로 설명. Firebase의 자동 연결 자체가 아니라 양쪽 등록 상태 불일치의 처리 공백을 해결하는 것임을 구분.
- 실행한 테스트와 결과: 코드 변경 없는 설명으로 Gradle 미실행, git diff --check 수행.
- 유지한 계약: 이메일만으로 병합하지 않음, 다른 UID/소유자/차단/교체는 우회하지 않음.
- 결정사항: Google/Apple과 동일한 UX 및 보안 정책으로 Kakao 확대 방향. 실제 Kakao OIDC의 동일 UID 자동 연결 발생 여부는 미확인으로 명시.
- 위험 요소: 카카오가 항상 같은 이메일에 자동 연결된다고 단정할 수 없음. Firebase 연결 형태/토큰 증빙 확인 필요.
- 예상 밖 변경: 없음. Jira/외부 설정/커밋/push/배포 변경 없음.
- Jira 댓글 초안(미등록): Kakao 등록 상태 불일치 해결 의도 확인. 구현 추가 없음, 실제 OIDC 동작 확인 필요.
- 다음 작업: 승인된 범위에서 Kakao 검증 및 자동 등록 확장 구현.

## 2026-10-01 — Kakao 확장 의도 확인 turn 기록 보완

<!-- codex-turn:01a0f5e3-d705-70c1-9e94-15d82a5da49d -->

- 브랜치: feat/TMI-189-firebase-provider-auto-link.
- Jira: TMI-189
- 작업 목표: 현재 설명 turn의 정확한 식별자로 작업 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Kakao도 동일 Firebase UID에 연결됐지만 Identity에 미등록인 상태를 안전 검증 후 처리하는 확장 의도 확인. Firebase 자동 연결 자체를 막는 작업은 아님.
- 실행한 테스트와 결과: 문서 변경으로 Gradle 미실행. git diff --check 수행.
- 유지한 계약: 이메일 자동 병합·차단 우회 없음, 현재 애플리케이션 동작 불변.
- 결정사항: 과거 기록은 유지하고 현재 turn 식별자 기록을 EOF에 추가.
- 위험 요소: 실제 Kakao OIDC 자동 연결/식별 증빙은 미검증.
- 예상 밖 변경: 없음. 외부 설정·Jira·커밋/push/배포 변경 없음.
- Jira 댓글 초안(미등록): Kakao 확장 의도 설명 완료, 추가 구현 없음, 실제 OIDC 검증 필요.
- 다음 작업: 승인된 범위의 Kakao 자동 등록 확장 구현 및 검증.

## 2026-10-01 — Kakao 플랫폼 키 위치 안내

<!-- codex-turn:01a0f60c-7d39-7100-a0f3-2e572fcbef7c -->

- 브랜치: 현재 작업 트리 유지. Jira: TMI-189 관련 준비 후속.
- 작업 목표: REST API 키와 Client Secret 메뉴 위치 안내.
- 변경 파일: docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 구현 내용: 코드 변경 없음. 카카오 앱 메뉴의 플랫폼 키를 열어 Default Rest API Key 카드 및 클라이언트 시크릿 라벨 확인. 실제 값은 출력/기록하지 않음.
- 실행한 테스트와 결과: UI 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 키 생성/변경/전송/설정 저장 없음.
- 결정사항: 사용자가 기존 REST API 키 카드에서 직접 복사/상세 확인, 다른 키 종류와 구분.
- 위험 요소: Client Secret 상세 활성 상태는 미확인. 작업 중 기록 파일이 외부에서 변경되어 최신 EOF를 다시 읽고 현재 항목만 추가함.
- 예상 밖 변경: 이번 작업 외 코드 수정 없음, 외부 변경은 보존. Jira 변경 없음.
- Jira 댓글 초안(미등록): Kakao 플랫폼 키 위치 안내, 실제 값 입력 및 연동 검증 대기.
- 다음 작업: 사용자 client 설정 입력 후 Firebase callback 확인.

## 2026-10-01 — MEMBER 승격 후 Guest prepare 에러 코드 확인

<!-- codex-turn:01a0f60f-afb5-72e2-bbe8-ea3f7ccafd29 -->

- 브랜치: develop (이번 확인 시점 기준, 브랜치 전환 직접 수행 없음).
- 작업 목표: 이미 MEMBER가 된 사용자 본인의 기존 Guest 인증으로 prepare 호출 시 에러 코드 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. FirebaseGuestPrepareService의 ACTIVE/Guest 검사와 AuthErrorStatus의 HTTP 상태, JwtCurrentUserProvider의 MERGED source 거절을 확인.
- 실행한 테스트와 결과: 소스 읽기 및 git diff --check. 설명/기록만 변경하여 Gradle 미실행.
- 유지한 계약: DB 현재 User 상태 검사, JWT sub 기반 현재 사용자 식별, MERGED source 인증을 target 권한으로 자동 치환하지 않음.
- 결정사항: 본인 승격 완료 후 유효한 기존 Guest Access Token으로 서비스 진입 시 403 GUEST_UPGRADE_NOT_ALLOWED. 인증 만료/무효로 먼저 거절되는 경우 및 통합된 MERGED source의 401 ACCOUNT_MERGED_TOKEN_REJECTED와 구분. 이 에러만으로 승격 성공을 단정하지 않음.
- 위험 요소: 실제 요청/배포 인증 설정/E2E는 미확인. 토큰 인증 실패가 서비스 에러보다 먼저 발생할 수 있음.
- 예상 밖 변경: 이번 애플리케이션 변경 없음. 외부에서 변경된 브랜치와 기존 문서 변경 보존. 커밋/push/Jira/배포 미수행.
- 다음 작업: 필요 시 프론트에서 fresh Firebase proof exchange 및 동일 사용자 확인 복구 분기 검증.

## 2026-10-01 — Kakao 키 위치 안내 turn 기록 보완

<!-- codex-turn:01a0f60e-2766-75b3-83e6-1e09a1d932b3 -->

- 브랜치: develop(기록 보완 시 확인).
- Jira: TMI-189 관련 Kakao 준비 후속.
- 작업 목표: 현재 turn의 정확한 식별자로 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 앱 설정 → 앱 → 플랫폼 키의 REST API 키 카드 및 클라이언트 시크릿 위치 안내. 실제 값은 기록하지 않음.
- 실행한 테스트와 결과: UI 읽기 확인 및 git diff --check. 문서 변경으로 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 키 생성/변경/전송 및 설정 저장 없음.
- 결정사항: 정확한 현재 turn marker를 EOF에 추가.
- 위험 요소: Client Secret 상세 활성 상태와 Firebase 입력 완료는 미확인.
- 예상 밖 변경: 이번 작업 외 수정 없음. 외부 변경 보존, Jira/커밋/push/배포 미수행.
- Jira 댓글 초안(미등록): 플랫폼 키 위치 안내 완료, 사용자 입력 및 연동 검증 대기.
- 다음 작업: Firebase client 입력 및 callback 단계 확인.

## 2026-10-01 — Guest prepare 에러 코드 확인 최종 기록

- 브랜치: develop.
- 작업 목표: 이번 MEMBER 승격 후 prepare 에러 설명 작업을 최신 EOF에 기록(현재 turn marker는 위 본 작업 항목에 포함).
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 구현 내용: 소스 확인만 수행, 코드 변경 없음.
- 실행한 테스트와 결과: git diff --check 통과. 설명 작업으로 Gradle 미실행.
- 유지한 계약/결정사항: 서비스 진입 시 DB User가 MEMBER이면 403 GUEST_UPGRADE_NOT_ALLOWED. 인증 실패 및 MERGED source 거절은 별도 401 가능.
- 위험 요소: 실제 요청/E2E 미검증. 동시 문서 기록은 수정하지 않고 보존.
- 다음 작업: 필요 시 프론트 복구 분기 검증. 외부 계약/코드/커밋/push/Jira 변경 없음.

## 2026-10-01 — Kakao 로그인용 Client Secret 선택 안내

<!-- codex-turn:01a0f611-9f15-7e33-8398-0f0b641bb2f2 -->

- 브랜치: develop.
- Jira: TMI-189 관련 Kakao 준비 후속.
- 작업 목표: 카카오 로그인/비즈니스 인증 두 시크릿 중 Firebase OIDC용 선택 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 카카오 로그인용 Client Secret과 REST API 키를 Firebase client 설정에 사용하도록 안내, 비즈니스 인증용 제외.
- 실행한 테스트와 결과: 설명 작업으로 Gradle 미실행. git diff --check 수행.
- 유지한 계약: 비밀값 열람/기록/전송, 자격증명 생성/변경 없음.
- 결정사항: 사용자가 카카오 로그인용 값을 Firebase 클라이언트 보안 비밀번호에 직접 입력.
- 위험 요소: 실제 값의 일치 및 활성 상태/로그인 결과 미검증.
- 예상 밖 변경: 없음. 기존 변경 보존, 외부 설정/Jira/커밋/push/배포 변경 없음.
- Jira 댓글 초안(미등록): Firebase OIDC에 사용할 카카오 로그인용 시크릿 선택 안내. 실제 연동 검증 대기.
- 다음 작업: Firebase 입력 후 callback 등록 및 로그인 검증.

## 2026-10-01 — 카카오 시크릿 선택 안내 turn 기록 보완

<!-- codex-turn:01a0f611-13c7-7253-a099-fe1c2c31b3f6 -->

- 브랜치: develop.
- Jira: TMI-189 관련 Kakao 준비 후속.
- 작업 목표: 현재 turn의 정확한 식별자로 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Firebase OIDC에는 비즈니스 인증용이 아닌 카카오 로그인용 Client Secret을 사용하도록 안내.
- 실행한 테스트와 결과: 문서 변경으로 Gradle 미실행, git diff --check 수행.
- 유지한 계약: 과거 기록 보존, 비밀값 조회/기록/전송 및 외부 설정 변경 없음.
- 결정사항: 정확한 현재 turn marker를 파일 끝에 추가.
- 위험 요소: 실제 client 입력/활성 상태와 연동 결과 미확인.
- 예상 밖 변경: 없음. Jira/커밋/push/배포 미수행.
- Jira 댓글 초안(미등록): 카카오 로그인용 시크릿 선택 안내 완료, 실제 연동 검증 대기.
- 다음 작업: Firebase 입력과 callback 등록 후 로그인 검증.

## 2026-10-01 — Firebase OIDC 등록 확인 및 Kakao callback 안내

- 브랜치: 현재 작업 트리 유지.
- Jira: TMI-189 관련 Kakao 준비 후속.
- 작업 목표: 사용자 OIDC 입력 후 다음 단계 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Firebase 목록에서 oidc.kakao 사용 설정됨과 통합 구성의 공개 callback 확인. Kakao REST API 키 수정 화면에서 카카오 로그인 리다이렉트 URI 라벨 확인.
- 실행한 테스트와 결과: 비밀값을 출력하지 않는 UI 상태 조회 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 키/시크릿 값 비기록, 외부 설정 쓰기 없음.
- 결정사항: 실제 Firebase callback을 카카오 로그인 리다이렉트 URI에 사용자가 입력하도록 안내, 비즈니스 인증 URI에 입력하지 않음.
- 위험 요소: 제공업체 저장 상태와 실제 로그인 성공은 다름. callback 저장 및 서버 자동 등록 확장/프론트 테스트는 미검증.
- 예상 밖 변경: 없음. Jira/커밋/push/배포 변경 없음.
- Jira 댓글 초안(미등록): oidc.kakao 활성 목록 확인, Kakao callback 사용자 등록 안내.
- 다음 작업: Kakao callback 저장 확인 후 테스트 로그인 및 서버 준비 확인.

## 2026-10-01 — OIDC callback 안내 turn 기록 보완

<!-- codex-turn:01a0f612-122b-7513-a043-88cbe6b7eb57 -->

- 브랜치: develop.
- Jira: TMI-189 관련 Kakao 준비 후속.
- 작업 목표: 현재 turn의 정확한 식별자로 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Firebase oidc.kakao 사용 설정 확인 후 Kakao REST API 키의 카카오 로그인 리다이렉트 URI에 실제 Firebase callback 등록 안내.
- 실행한 테스트와 결과: UI 읽기 확인 및 git diff --check. 문서 변경으로 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀값 비기록, 외부 설정 쓰기 없음.
- 결정사항: 현재 turn marker를 EOF에 추가.
- 위험 요소: callback 저장/실제 로그인/서버 준비는 미검증.
- 예상 밖 변경: 없음. Jira/커밋/push/배포 미수행.
- Jira 댓글 초안(미등록): OIDC 활성 확인 및 callback 등록 안내 완료, 실제 연동 검증 대기.
- 다음 작업: 사용자 callback 저장 확인 후 테스트 로그인 준비.

## 2026-10-01 — Firebase OIDC 플랫폼별 단계 의미 안내

<!-- codex-turn:01a0f615-3265-72d0-bde3-3e59a71ebedb -->

- 브랜치: 현재 작업 트리 유지.
- Jira: TMI-189 관련 Kakao 준비 후속.
- 작업 목표: Firebase의 플랫폼별 단계 안내 의미와 남은 연동 책임 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. UI에서 Android/iOS/웹 OpenID Connect 문서 링크를 확인. 콘솔 provider 설정과 앱 SDK 로그인 구현이 별도임을 안내.
- 실행한 테스트와 결과: 비밀값 제외한 UI 텍스트 확인 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: Firebase 인증 후 Identity exchange 경로 유지. 실제 사용자/비밀값/외부 설정 변경 없음.
- 결정사항: Android 대상 앱은 oidc.kakao 제공자 로그인 및 복귀 처리를 프론트에서 구현, 로컬 웹 테스트는 웹 SDK 경로 사용. 모든 플랫폼을 설정할 필요는 없음. 카카오 직접 토큰을 기존 Identity Firebase exchange에 전달하지 않음.
- 위험 요소: provider 활성화만으로 프론트 버튼/서버 허용/자동 등록 확장이 완료되지 않음. 실제 frontend framework와 모바일 로그인 E2E 미확인.
- 예상 밖 변경: 없음. Jira/커밋/push/배포 변경 없음.
- Jira 댓글 초안(미등록): 플랫폼별 안내는 클라이언트 SDK 연동 문서임을 확인, Android 및 웹 테스트 구현은 별도 준비 필요.
- 다음 작업: callback 저장 확인 및 프론트 SDK/서버 Kakao 설정·확장 준비.

## 2026-10-01 — Kakao Android 및 iOS 지원 범위 정정

- 브랜치: develop.
- Jira: TMI-189 관련 Kakao 준비 후속.
- 작업 목표: Android만 대상으로 설명한 범위를 사용자의 Android/iOS 지원 의도에 맞게 정정.
- 변경 파일: docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 구현 내용: 코드 변경 없음. Firebase OIDC Apple 문서 링크는 iOS 플랫폼 연동 안내이며 Apple 로그인 제공자와 별개임을 설명. 양 플랫폼의 카카오 로그인 및 복귀 검증 필요.
- 실행한 테스트와 결과: 설명/기록 작업으로 Gradle 미실행, git diff --check 수행.
- 유지한 계약: 공통 Firebase oidc.kakao 및 Identity exchange 사용, 별도 플랫폼별 provider 생성 불필요. 비밀정보 비기록.
- 결정사항: Android/iOS 둘 다 프론트 연동 대상, localhost 테스트는 웹 SDK 대상으로 분리.
- 위험 요소: 실제 프론트 프레임워크/플랫폼별 복귀 설정 및 E2E 미확인.
- 예상 밖 변경: 없음. 외부 설정/Jira/커밋/push/배포 변경 없음.
- Jira 댓글 초안(미등록): Kakao 연동 대상 Android/iOS 모두로 정정, 양 플랫폼 E2E 준비 필요.
- 다음 작업: 공통 콘솔 설정 완료 후 양 플랫폼 프론트와 서버 연동 준비.

## 2026-10-01 — Android/iOS 범위 정정 turn 기록 보완

<!-- codex-turn:01a0f617-cae4-73f2-96b5-dd209ddd29ad -->

- 브랜치: develop.
- Jira: TMI-189 관련 Kakao 준비 후속.
- 작업 목표: 현재 turn의 정확한 식별자로 작업 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 추가 코드 없음. Kakao 연동 대상은 Android/iOS 모두이며 Firebase 문서의 Apple 링크는 iOS 플랫폼 안내임을 정정.
- 실행한 테스트와 결과: 문서 변경으로 Gradle 미실행, git diff --check 수행.
- 유지한 계약: 공통 oidc.kakao와 Identity exchange 사용, 비밀정보 비기록, 과거 기록 보존.
- 결정사항: 정확한 turn marker 기록을 EOF에 추가.
- 위험 요소: 양 플랫폼 SDK/복귀 설정 및 실제 로그인 E2E 미확인.
- 예상 밖 변경: 없음. 외부 설정/Jira/커밋/push/배포 미수행.
- Jira 댓글 초안(미등록): Android/iOS 모두 연동 대상임을 정정, 플랫폼별 검증 필요.
- 다음 작업: 공통 설정 완료 후 Android/iOS 및 웹 테스트 연동 준비.

## 2026-10-01 — Kakao Redirect URI 확인 및 민감정보 출력 주의

- 브랜치: develop.
- Jira: TMI-189 관련 Kakao 준비 후속.
- 작업 목표: 사용자가 설정한 redirect URI 확인 및 다음 단계 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 카카오 로그인 리다이렉트 URI 입력란의 주소가 확인된 Firebase callback과 일치함을 확인. 저장 완료는 사용자 보고이며 별도 재조회 미수행.
- 실행한 테스트와 결과: UI 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 외부 설정/자격증명 변경 없음. 실제 키/시크릿은 작업 문서와 응답에 기록하지 않음.
- 결정사항: 탭 선택 API 자동 화면 출력에 client secret이 포함되는 문제를 사용자에게 알리고 해당 시크릿 재발급 및 Firebase 값 갱신 권장. 향후 이 화면은 기존 핸들에서 비출력 조회 후 허용된 항목만 추출해야 함.
- 위험 요소: 도구 출력에 민감정보가 포함됐으며 자동 재발급/삭제는 하지 않음. 실제 로그인/서버 Kakao 준비는 미검증.
- 예상 밖 변경: 코드/외부 설정 변경 없음. Jira/커밋/push/배포 미수행.
- Jira 댓글 초안(미등록): Redirect URI 일치 확인, 실제 연동 테스트 대기. 민감값은 기록하지 않음.
- 다음 작업: 사용자 시크릿 재발급·Firebase 대응 값 갱신 후 테스트 로그인 및 서버 준비 확인.

## 2026-10-01 — Redirect URI 확인 turn 기록 보완

<!-- codex-turn:01a0f618-e6ad-7d61-b7bb-675ca428db21 -->

- 브랜치: develop.
- Jira: TMI-189 관련 Kakao 준비 후속.
- 작업 목표: 현재 turn의 정확한 식별자로 작업 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Kakao 로그인 redirect 입력값과 Firebase callback 일치 확인. 탭 선택 도구의 자동 출력에 민감값이 포함된 문제를 알리고 사용자 직접 시크릿 재발급 및 Firebase 갱신 권장.
- 실행한 테스트와 결과: UI 확인 및 git diff --check. 문서 변경으로 Gradle 미실행.
- 유지한 계약: 실제 비밀값 비기록, 과거 기록 보존, 외부 설정 변경 없음.
- 결정사항: 정확한 현재 turn marker를 EOF에 추가.
- 위험 요소: 시크릿 교체 및 실제 로그인은 아직 미확인. 도구 출력 내용을 문서에 복사하지 않음.
- 예상 밖 변경: 없음. Jira/커밋/push/배포 미수행.
- Jira 댓글 초안(미등록): Redirect URI 일치 확인, 실제 연동 검증 대기. 민감값 미포함.
- 다음 작업: 사용자 시크릿 교체 후 테스트 로그인 준비.

## 2026-10-01 — Kakao callback 등록 이후 진행 순서 안내

- 브랜치: develop.
- 작업 목표: 사용자 callback 등록 보고에 따른 다음 작업 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 안내만 수행. 공통 OIDC 설정 이후 웹 테스트, Identity Kakao 지원 점검·확장, Android/iOS 연동 순서를 구분.
- 실행한 테스트와 결과: git diff --check 수행. 설명 및 문서만 변경하여 Gradle 미실행.
- 유지한 계약: JWT 및 API 변경 없음, 비밀값 비기록.
- 결정사항: 두 모바일 플랫폼 모두 지원 대상이며 별도 OIDC 제공자를 만들 필요 없음.
- 위험 요소: 앞서 권장한 시크릿 교체 완료 및 실제 Kakao 로그인은 미확인.
- 다음 작업: 시크릿 갱신 완료 확인 후 웹 테스트 도구와 서버 Kakao 지원 준비.

## 2026-10-01 — Kakao 시크릿 교체 완료 보고 수신

- 브랜치: develop.
- 작업 목표: 사용자 시크릿 교체 완료 보고 반영 및 다음 단계 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 사용자 보고로 시크릿 교체 및 Firebase 반영 완료 상태 기록. 실제 값은 조회하지 않음.
- 실행한 테스트와 결과: git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: JWT/API 및 외부 설정 변경 없음, 비밀값 비기록.
- 결정사항: 다음 단계는 웹 테스트 화면 및 Identity Kakao 지원 준비.
- 위험 요소: 실제 OIDC 로그인과 서버 연동은 아직 미검증.
- 다음 작업: 테스트 화면 Kakao 로그인 및 서버 허용·최초 연결 검증 준비 후 Android/iOS 연동.

## 2026-10-01 — 시크릿 교체 보고 turn 기록 보완

<!-- codex-turn:01a0f61d-5376-7742-a131-23791eb7258b -->

- 브랜치: develop.
- 작업 목표: 현재 turn 식별자를 포함한 작업 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 사용자 시크릿 교체 완료 보고 반영. 코드 및 외부 설정 변경 없음.
- 실행한 테스트와 결과: git diff --check 통과. 문서 변경만 있어 Gradle 미실행.
- 유지한 계약: API/JWT 유지, 비밀값 미조회·비기록.
- 결정사항: 웹 테스트 화면 및 Identity Kakao 지원 준비가 다음 단계.
- 위험 요소: 실제 OIDC 로그인 및 서버 연동 미검증.
- 다음 작업: Kakao 테스트 화면과 서버 지원 준비 후 Android/iOS 검증.

## 2026-10-01 — TMI-189 후속 Kakao 최초 로그인 등록 및 로컬 테스트 화면

<!-- codex-turn:01a0f61f-4c59-79f1-b577-fa829a69fa49 -->

- 브랜치: develop (기존 브랜치 유지).
- Jira: TMI-189. 구현 전 이슈 조회. 원 이슈는 Google/Apple 범위이며 이번 사용자의 명시적 후속 요청으로 Kakao까지 확장. Jira 본문/댓글/상태 변경 없음.
- 작업 목표: 로컬 웹 테스트 화면 Kakao 로그인 추가 및 Identity 동일 UID 활성 MEMBER 최초 연결 지원.
- 변경 파일: src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/{ProviderChangeGuard,ProviderLoginRegistrationService}.java, src/main/java/web/tosunsaeng/identity/domain/auth/federation/infrastructure/firebase/FirebaseSdkAdminClient.java, 대응 ProviderLoginRegistrationServiceTests/FirebaseSdkAdminClientTests/FirebaseAdminAuthenticationVerifierTests, docs/contracts/frontend-firebase-auth-integration-{guide,appendix}.md, docs/codex/{WORKLOG,CURRENT_STATE}.md.
- 외부 로컬 도구 변경 파일: /Users/msde76/tosunsaeng-integration-test/{app.js,index.html,apple.test.mjs}. 허용된 임시 디렉터리에서 apply_patch로 준비하고 파일 반영 승인을 받아 복사. 운영 코드나 LC 코드 추가 없음.
- 구현 내용: LOGIN_EXCHANGE의 최초 등록 허용 목록에 KAKAO 추가 및 guard/service 공통 판정 사용. SDK에 설정된 Kakao OIDC ID를 전달하고 서명된 firebase.identities의 단일 현재 subject와 Admin 최신 providerData를 비교. 같은 binding/ACTIVE MEMBER/소유권/차단/과거 이력/보안 작업/트랜잭션 및 CAS 유지. 미등록 OIDC 또는 Kakao OFF는 verifier에서 거절. 이메일 자동 병합 없음.
- UI: oidc.kakao 팝업 로그인과 같은 계정 재인증 버튼, 제공자 전환 차단, 토큰 메모리 보관·민감정보 비출력 유지. 선택적 email scope 강제 없음. Android/iOS는 공통 제공자를 쓰지만 각 SDK E2E 필요함을 안내. 기존 Apple 재현 전용 기능은 변경하지 않음.
- 실행한 테스트와 결과: 최초 Gradle sandbox 캐시 접근 실패 후 승인된 전체 실행. 신규 mock의 중첩 stubbing 오류 1건 수정 후 최종 ./gradlew clean test 성공(1024개, 실패/오류/스킵 0). node --test app.test.mjs apple.test.mjs challenge.test.mjs merge.test.mjs server.test.mjs 7개 성공. Node 첫 sandbox 실행은 localhost listen EPERM으로 1건 실패하여 승인 후 재실행 성공. git diff --check 통과.
- 수동 확인: localhost:4173 미가동 확인 후 node server.mjs 재시작, HTTP HTML 응답 정상. 열린 브라우저 새로고침/로그인/계정 생성/연결/병합 미수행. 서버 테스트는 mock만 사용, 실 OAuth/Atlas 호출 없음.
- 유지한 계약: exchange 요청/응답·UUID sub·JWT RS256/audience 및 소유권 유지. 비밀값 미조회·비기록. 기존 FIREBASE_KAKAO_ENABLED 기본 false와 AUTH_SESSION_FENCE_ENABLED 기본값 유지, 외부 배포 설정 변경 없음.
- 결정사항: 실제 테스트 전 사용자 커밋/push 및 테스트 배포 필요. FIREBASE_KAKAO_ENABLED=true, FIREBASE_KAKAO_PROVIDER_ID=oidc.kakao, AUTH_SESSION_FENCE_ENABLED=true 확인. sync 사용 시 FIREBASE_PROVIDER_CHANGE_FENCE_ENABLED 확인.
- 위험 요소: 실제 Kakao OIDC 동일 UID 연결 동작, 신규 가입/기존 회원/전화번호/모바일 앱 복귀 및 실제 Mongo 동시성은 미검증. 웹 성공을 Android/iOS 검증으로 대체하지 않음. 새로고침 시 로컬 메모리의 세션/Guest/녹음 상태 소실 주의.
- 예상 밖 변경: 없음. 시작 전 기존 WORKLOG/CURRENT_STATE 누적 변경 보존. 커밋/push/배포/Jira 수정 없음.
- Jira 댓글 초안(미등록): 사용자 후속 요청으로 Kakao 최초 등록·설정 OIDC subject 교차 검증·로컬 UI 확장. 서버/웹 회귀 테스트 통과. 실환경 및 모바일 검증 대기.
- 다음 작업: 사용자 diff 확인 및 직접 커밋/push → 테스트 배포/플래그 확인 → Chrome Kakao 로그인 및 Identity exchange·가입·재발급 검증 → Android/iOS 적용.

## 2026-10-01 — Kakao exchange 403 진단

<!-- codex-turn:01a0f62c-f8aa-7c52-86a3-19b59f1e2cf5 -->

- 브랜치: develop.
- 작업 목표: 사용자 보고 FIREBASE_PROVIDER_NOT_ALLOWED 원인 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 코드 변경 없음.
- 구현 내용: verifier가 현재 제공자 미인식 또는 비활성 시 해당 오류를 반환함 확인. application.yml 기본 Kakao OFF와 기존 테스트 task definition 초안 OFF 확인. 실제 배포 설정과 초안은 구분.
- 실행한 테스트와 결과: 소스/설정 읽기 및 git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 토큰 원문/비밀값 조회 없음, 외부 설정/회원 데이터 변경 없음.
- 결정사항: 배포된 Kakao 허용 플래그 미반영이 유력하나 정확한 provider ID 및 실행 task 설정 확인 전 확정하지 않음.
- 위험 요소: AWS CLI 프로필 없음, Chrome AWS 콘솔 세션 만료로 실환경 값 확인 불가. 기존 접속 포털을 열어 로그인 인계.
- 다음 작업: 사용자 AWS 로그인 후 실행 중 Identity 테스트 task의 비민감 플래그/배포 버전 확인. 변경·재배포는 별도 승인 후 진행.

## 2026-10-01 — Kakao 코드 지원과 배포 활성 설정 구분

- 브랜치: develop.
- 작업 목표: Kakao 허용 설정 적용 여부 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 최초 등록 허용 목록의 KAKAO 추가와 application.yml의 기본 OFF 상태 재확인. 이번 코드/외부 설정 변경 없음.
- 실행한 테스트와 결과: 소스 조회 및 git diff --check 통과. 설명만 수행하여 Gradle 미실행.
- 유지한 계약: JWT/API 및 기존 활성 플래그 기본값 유지, 비밀값 비기록.
- 결정사항: 코드 지원 완료와 AWS 활성화는 별개. assistant는 ECS 환경변수 변경 및 배포를 하지 않음.
- 위험 요소: 실제 실행 task 플래그 미확인. 새 코드 배포만으로 기본 OFF가 ON으로 바뀌지 않음.
- 다음 작업: 테스트 ECS의 FIREBASE_KAKAO_ENABLED=true 및 정확한 provider ID 확인 후 새 task 배포, 실제 로그인 검증.

## 2026-10-01 — Kakao 활성 설정 설명 기록 보완

<!-- codex-turn:01a0f62f-eb28-78c0-bd9b-37316ca2d8c6 -->

- 브랜치: develop.
- 작업 목표: 현재 turn의 Kakao 허용 설정 설명 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 지원은 완료했지만 AWS 활성 설정 변경과 배포는 미수행임을 안내. 이번 코드 변경 없음.
- 실행한 테스트와 결과: 소스 확인 및 git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: API/JWT 및 기본 OFF 유지. 비밀값 비기록.
- 결정사항: 새 코드 배포와 테스트 환경 Kakao ON 설정 모두 필요.
- 위험 요소: 실행 중 ECS 설정 미확인, 실제 로그인 검증 대기.
- 다음 작업: 테스트 ECS 활성 플래그와 provider ID 확인 후 승인된 배포 진행.

## 2026-10-01 — CI/CD 완료 후 테스트 Identity Kakao 활성화 재배포

<!-- codex-turn:01a0f634-af94-7f81-a4c5-bef57d5d7cc1 -->

- 브랜치: develop.
- 작업 목표: 사용자가 push한 CI/CD 완료 확인 후 승인된 테스트 Kakao 설정 적용·재배포.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 애플리케이션/워크플로 코드 변경 없음.
- 구현 내용: GitHub run 36826056025의 테스트·이미지 빌드·ECS 배포·헬스 검증 성공 확인. commit 1cca3c17c3460e01a7ed75b82f4c22ed6fabede0, CI 생성 revision tosunsaeng-identity-test:9 확인. 기존 revision 8과 9의 Kakao 및 session fence OFF 확인.
- 외부 변경: 사용자에게 세 설정의 범위와 효과를 제시하고 '세 설정 적용 승인' 수신 후 revision 9를 기반으로 revision 10 생성. FIREBASE_KAKAO_ENABLED=true, FIREBASE_KAKAO_PROVIDER_ID=oidc.kakao, AUTH_SESSION_FENCE_ENABLED=true. 동일 commit 이미지와 나머지 설정 유지. tosunsaeng-staging-cluster의 tosunsaeng-identity-test-service만 revision 10으로 업데이트.
- 실행한 테스트와 결과: gh run watch 및 run view로 CI success 확인. ECS revision 10 저장된 세 환경값 확인, 최종 배포 성공·1 running/0 pending·0/3 task failures 확인. 배포 후 HTTPS /actuator/health 응답 UP. git diff --check 통과. 애플리케이션 변경 없어 로컬 Gradle 재실행 없음; 이전 1024개 로컬 테스트 및 이번 CI 테스트 성공과 구분.
- 유지한 계약: JWT/API/키/DB/이벤트 목적지 변경 없음. 운영 서비스 미변경. 토큰·비밀값 조회 및 기록 없음. Google/Apple 허용, 재발급 복구 및 다른 기능 설정 유지.
- 결정사항: CI/CD 종료를 기다려 설정 덮어쓰기 방지. 현 워크플로는 서비스의 현재 task definition을 가져와 이미지와 SENTRY_RELEASE를 바꾸므로 다음 배포도 해당 환경 설정을 승계. 워크플로 자체는 수정하지 않음.
- 위험 요소: 실 카카오 exchange·신규 가입·기존 회원 최초 연결 및 Android/iOS E2E는 별도 검증 필요. session fence ON으로 세션 보호 동작 활성화; auth-methods/sync를 사용하는 경우 별도 provider fence 계약 확인 필요. 새 태스크 실패 시 이전 revision 9가 설정 롤백 기준이나 이번 자동 롤백은 수행하지 않음.
- 예상 밖 변경: 없음. 사용자 push 완료로 시작 시 작업 트리 깨끗함 확인. 이번 commit/push/Jira 변경 없음. AWS CLI 자격증명이 없어 로그인된 Chrome 콘솔로 배포 수행, 자격증명 복사 없음.
- 다음 작업: Chrome Kakao 재인증 후 Identity exchange 재시도, MEMBER/가입 준비 응답 확인. 이후 가입·재발급·기존 회원 연결 및 Android/iOS 검증.

## 2026-10-01 — 로컬 통합 테스트 서버 재시작

- 브랜치: develop.
- 작업 목표: localhost 테스트 화면 접속 복구.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 코드 변경 없음.
- 구현 내용: 4173 포트 리스너 없음 확인 후 기존 통합 테스트 도구의 node server.mjs 재실행.
- 실행한 테스트와 결과: 시작 메시지 및 localhost:4173 HTTP 200 확인, git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: AWS 배포/인증/회원 데이터 변경 없음. 비밀값 비기록.
- 결정사항: 로컬 서버 종료가 접속 장애 원인이었으며 재시작으로 화면 응답 복구.
- 위험 요소: 프로세스의 이전 종료 원인은 미확인. 브라우저 새로고침 시 메모리 세션 소실 가능, 직접 새로고침 미수행.
- 다음 작업: Chrome에서 로컬 화면 접속 후 Kakao 테스트 재개. 사용자가 터미널에서 node server.mjs를 직접 실행해 유지할 수도 있음.

## 2026-10-01 — 로컬 서버 복구 turn 기록 보완

<!-- codex-turn:01a0f641-fed8-7fc2-a684-1ec67ac33d82 -->

- 브랜치: develop.
- 작업 목표: 로컬 테스트 서버 복구 결과에 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 4173 포트 리스너 부재 확인 후 기존 node server.mjs 재실행, 코드 수정 없음.
- 실행한 테스트와 결과: localhost:4173 HTTP 200 및 git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: AWS/회원 데이터/API 변경 없음, 비밀값 비기록.
- 결정사항: 로컬 화면 응답 복구 완료.
- 위험 요소: 이전 프로세스 종료 원인 미확인, 실제 Kakao 로그인은 별도 검증 필요.
- 다음 작업: Chrome 로컬 화면에서 Kakao 테스트 재개.

## 2026-10-01 — 전화번호 테스트 CAPTCHA 오류 진단

- 브랜치: develop.
- 작업 목표: send의 auth/captcha-check-failed 원인 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 로컬 app.js에서 appVerificationDisabledForTesting=true 및 send의 Firebase PhoneAuthProvider 직접 호출 확인. Identity와 분리된 Firebase 인증 오류이며 콘솔 등록 가상 번호 전용 도구임을 안내.
- 실행한 테스트와 결과: 코드 읽기 및 git diff --check 통과. 코드 변경 없어 테스트 재실행 없음.
- 유지한 계약: 실제 SMS/외부 인증 요청/설정 변경 없음. 전화번호·코드·토큰 비기록.
- 결정사항: 입력 번호가 동일 Firebase 프로젝트에 등록된 테스트 번호인지 국제 형식으로 비교하도록 안내. 미등록 번호와 mock CAPTCHA 조합이 유력한 원인이나 입력값/콘솔 미확인으로 확정하지 않음.
- 위험 요소: 실제 입력 및 Firebase 테스트 번호 설정, 허용 도메인과 브라우저 상태 미검증.
- 다음 작업: 사용자 테스트 번호 등록 여부 확인 후 필요 시 허용 도메인/브라우저 CAPTCHA 상태 점검.

## 2026-10-01 — CAPTCHA 진단 turn 기록 보완

<!-- codex-turn:01a0f644-9b1a-7e01-9609-04c8fe75ce44 -->

- 브랜치: develop.
- 작업 목표: 현재 CAPTCHA 진단 작업의 식별자 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 로컬 테스트 도구의 가상 전화번호 인증 모드 확인. Firebase 테스트 번호 등록 여부 확인을 사용자에게 요청. 코드 변경 없음.
- 실행한 테스트와 결과: 소스 조회 및 git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 외부 설정·인증 요청 변경 없음, 비밀값 및 전화번호 비기록.
- 결정사항: 미등록 번호와 테스트 CAPTCHA 조합 가능성을 설명하되 확정 원인으로 단정하지 않음.
- 위험 요소: 실제 입력과 Firebase 콘솔 설정 미확인.
- 다음 작업: 사용자 등록 여부 확인 후 추가 진단.

## 2026-10-01 — 전화번호 연결 계정 충돌 오류 안내

- 브랜치: develop.
- 작업 목표: link의 auth/account-exists-with-different-credential 해석.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 로컬 link가 PhoneAuthProvider credential을 현재 Firebase user에 linkWithCredential로 연결하며 Identity signup 이전임을 확인. Firebase 계정/인증수단 충돌로 구분하고 정확한 충돌 대상은 미확정으로 안내.
- 실행한 테스트와 결과: 소스 조회 및 git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 기존 계정 삭제·연결 해제·UID 변경·이메일 자동 병합 없음. 개인정보/토큰 비기록.
- 결정사항: 전화번호 다른 UID 소유는 통상 credential-already-in-use이므로 현재 오류만으로 전화번호 중복을 단정하지 않음. 기존 Google/Apple 테스트 번호 재사용 여부 확인 요청.
- 위험 요소: Firebase 사용자 연결 및 원격 오류 상세 미확인.
- 다음 작업: 신규 가입 테스트인지 기존 회원 연결 테스트인지 구분하고 계정 연결 상태 확인. 임의 삭제/우회 금지.

## 2026-10-01 — Firebase 연결 충돌 진단 기록 보완

<!-- codex-turn:01a0f646-138a-75c1-9054-308451961e49 -->

- 브랜치: develop.
- 작업 목표: 현재 turn의 계정 연결 충돌 진단 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Firebase linkWithCredential 단계 오류를 설명하고 기존 테스트 번호 재사용 여부 확인 요청. 코드 변경 없음.
- 실행한 테스트와 결과: 소스 조회 및 git diff --check 통과. 문서 변경만 수행하여 Gradle 미실행.
- 유지한 계약: 계정 삭제·연결 해제·자동 병합·외부 설정 변경 없음. 비밀값 비기록.
- 결정사항: 오류만으로 전화번호 중복을 단정하지 않고 Firebase 연결 상태 확인 필요.
- 위험 요소: 실제 충돌 대상과 원격 계정 상태 미확인.
- 다음 작업: 사용자 답변 후 신규 가입/기존 회원 연결 목적을 구분해 진단.

## 2026-10-01 — 기존 연결 전화번호 재사용 확인

<!-- codex-turn:01a0f646-ffd2-7f31-b935-c5d3992bfaf1 -->

- 브랜치: develop.
- 작업 목표: 다른 계정에 연결된 테스트 번호라는 사용자 확인에 따른 흐름 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 신규 Kakao 가입에는 다른 미사용 등록 테스트 번호, 기존 회원에 Kakao 추가는 기존 회원 인증 후 명시 연결 흐름이 필요함을 구분. 코드/계정 변경 없음.
- 실행한 테스트와 결과: git diff --check 통과. 안내만 수행하여 Gradle 미실행.
- 유지한 계약: 전화번호 소유권 강제 이전·다른 Firebase UID 자동 병합·기존 연결 삭제 없음. 개인정보/비밀값 비기록.
- 결정사항: 번호 재사용은 사용자 보고로 확인됨. 실제 Firebase UID 및 정확한 오류 매핑은 직접 확인하지 않음. 같은 UID의 최초 제공자 자동 등록과 다른 UID 간 연결 충돌은 별개임을 설명.
- 위험 요소: 이미 다른 UID에 연결된 Kakao는 단순 추가 연결도 충돌할 수 있어 상태 확인 필요. 테스트 화면의 일반 로그인 버튼을 명시 연결 기능으로 간주하지 않음.
- 다음 작업: 사용자 목적을 신규 가입 검증 또는 기존 회원 로그인 수단 추가로 확정 후 해당 흐름 진행.

## 2026-10-01 — 기존 연결 번호 재사용 안내 기록 보완

<!-- codex-turn:01a0f646-ffd2-75f2-a1e2-a70a9da0ac47 -->

- 브랜치: develop.
- 작업 목표: 현재 turn의 정확한 식별자로 안내 결과 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 기존 연결 번호 재사용에 대해 신규 가입과 기존 회원의 Kakao 연결 흐름을 구분해 안내. 앞선 항목의 식별자는 잘못 기재되었으며 이 항목으로 보완. 과거 기록 수정 없음.
- 실행한 테스트와 결과: git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 계정 삭제·연결 해제·자동 병합 및 외부 설정 변경 없음. 비밀값 비기록.
- 결정사항: 같은 Firebase UID의 자동 등록과 다른 UID 간 충돌을 구분.
- 위험 요소: 실제 Firebase 연결 상태와 사용자의 테스트 목적 미확정.
- 다음 작업: 신규 가입 또는 기존 회원 연결 중 사용자 목적 확인 후 진행.

## 2026-10-01 — Kakao 기존 회원에 새 Google 로그인 시 충돌 구분

<!-- codex-turn:01a0f64c-f5f9-78a1-ae3f-a6f7dadb9f55 -->

- 브랜치: develop.
- 작업 목표: 동일 전화번호를 사용하는 새 Google 로그인 충돌 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 로컬 Google 버튼이 signInWithPopup 또는 동일 제공자 재인증이며 명시적 Google 추가 연결 기능이 아님을 재확인. 동일 전화번호로 별도 Firebase 계정을 자동 통합하지 않음을 안내.
- 실행한 테스트와 결과: 소스 조회 및 git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 같은 UID의 최초 제공자 등록만 허용, 전화번호/이메일 기반 자동 병합 없음. 비밀값 및 개인정보 비기록.
- 결정사항: Google 팝업 단계 오류는 기존 이메일/다른 제공자 충돌 가능성, 전화 연결 단계는 기존 계정 소유 충돌 가능성으로 구분하며 실제 실패 버튼 확인 요청. 현재 오류를 전화번호 충돌로 확정하지 않음.
- 위험 요소: 원격 Firebase UID/이메일 및 오류 발생 단계 미확인. 이미 다른 UID 소유인 Google은 명시 연결도 바로 성공한다고 보장하지 않음.
- 다음 작업: 실패 항목 google/link 확인 후 기존 Kakao MEMBER에 Google 추가 연결 흐름과 충돌 상태 점검.

## 2026-10-01 — link 단계 전화번호 연결 충돌 확인

<!-- codex-turn:01a0f64e-9492-7852-8098-94054e2b7575 -->

- 브랜치: develop.
- 작업 목표: 사용자 확인으로 실패 단계가 전화번호 연결임을 구분.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: link가 현재 Firebase 사용자에 PhoneAuthProvider 인증정보를 연결하는 호출임을 재확인. 기존 Kakao 회원의 Google 추가 연결과 신규 Google 가입의 전화번호 연결은 다른 흐름임을 안내.
- 실행한 테스트와 결과: 코드 조회 및 git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 기존 계정/전화번호 연결 삭제 및 다른 UID 자동 병합 없음. 개인정보·비밀값 비기록.
- 결정사항: 동일 번호가 다른 계정에 연결됐다는 사용자 보고와 link 실패는 확인됨. 정확한 Firebase UID 및 오류 매핑은 미확정. 기존 회원 인증 후 명시적 Google 연결이 필요한 흐름이며 테스트 도구의 일반 로그인으로 대체하지 않음.
- 위험 요소: Google 인증정보가 이미 다른 UID 소유이면 명시 연결 전 별도 상태 확인 필요. 전화번호 충돌 우회로 자동 이전하면 안 됨.
- 다음 작업: Firebase 연결 상태를 읽기 확인하고 사용자 요청 시 테스트 화면에 기존 회원의 로그인 수단 추가 절차 구현.

## 2026-10-01 — 전화번호 연결 충돌 안내 기록 보완

<!-- codex-turn:01a0f64e-9492-7230-b634-04162f451eaf -->

- 브랜치: develop.
- 작업 목표: 현재 turn의 정확한 식별자로 작업 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: link 단계의 전화번호 연결 실패와 기존 회원의 Google 추가 연결 흐름을 구분해 안내. 앞선 항목의 식별자 오기를 이 항목으로 보완하며 과거 기록은 보존.
- 실행한 테스트와 결과: git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 계정 삭제·연결 해제·자동 병합·외부 설정 변경 없음. 비밀값 비기록.
- 결정사항: 기존 회원의 명시 연결 절차와 신규 가입 전화번호 연결은 별개.
- 위험 요소: 실제 Firebase UID 및 Google 인증정보 소유 상태 미확인.
- 다음 작업: 연결 상태 확인 후 사용자 요청에 따라 테스트 도구의 로그인 수단 추가 기능 준비.

## 2026-10-01 — 신규 가입과 기존 회원 SNS 추가 연결 구분

- 브랜치: develop.
- 작업 목표: 기존 전화번호 재사용 및 SNS 연결 흐름 이해 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 다른 Firebase 계정에 연결된 전화번호를 새 UID에 중복 연결하지 않으며 기존 회원 인증 후 새 SNS를 연결하는 흐름을 안내. 이미 같은 UID인 경우 자동 등록 예외와 다른 회원 소유 SNS의 제한도 구분.
- 실행한 테스트와 결과: git diff --check 통과. 설명만 수행하여 Gradle 미실행.
- 유지한 계약: 전화번호 기반 자동 병합·소유권 이전 없음. 비밀값 비기록.
- 결정사항: 기존 회원 로그인 및 필요 시 최근 재인증 후 SNS 연결, 전화번호 재연결 불필요.
- 위험 요소: 로그인 상태만으로 모든 SNS 연결이 허용되는 것은 아니며 기존 소유권 충돌은 별도 확인 필요.
- 다음 작업: 사용자 요청 시 명시적 SNS 추가 연결 테스트 화면 준비.

## 2026-10-01 — SNS 추가 연결 안내 기록 보완

<!-- codex-turn:01a0f650-2a2c-7e12-9ca9-6aebb66d8895 -->

- 브랜치: develop.
- 작업 목표: 신규 가입과 기존 회원 SNS 연결 설명의 현재 turn 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 기존 회원 인증 후 새 SNS를 연결하고 전화번호를 유지하는 흐름 설명. 코드 변경 없음.
- 실행한 테스트와 결과: git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 다른 UID 자동 병합·전화번호 소유권 이전·외부 설정 변경 없음. 비밀값 비기록.
- 결정사항: 연결 완료 후 여러 SNS로 같은 회원 로그인 가능하나 다른 회원 소유 SNS는 무조건 연결하지 않음.
- 위험 요소: 실제 Firebase 소유권과 연결 상태는 별도 확인 필요.
- 다음 작업: 사용자 요청 시 명시적 SNS 연결 테스트 기능 준비.

## 2026-10-01 — 전화번호를 통한 기존 회원 복구 방향 검토

<!-- codex-turn:01a0f651-54ef-7e11-a255-693c88b4a800 -->

- 브랜치: develop.
- 작업 목표: 기존 SNS를 잃은 사용자의 새 SNS 및 전화번호 기반 복구 제안 검토.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 설명/설계 검토만 수행. 전화번호 입력 또는 SMS 단독 소유 증명을 기존 계정 소유권과 동일시하지 않도록 안내. 기존 로그인 수단 없는 경우 별도 계정 복구 흐름 필요.
- 실행한 테스트와 결과: git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 자동 UID 이전·회원 병합·전화번호 연결 해제 없음. 개인정보/비밀값 비기록.
- 결정사항: 새 SNS 인증 후 기존 계정 복구 선택, 실 SMS 인증 및 위험 기반 추가 확인, 명시 동의와 서버 검증을 거쳐 기존 userId 보존 방식 권장. 정상 로그인 경로와 분리하며 Firebase 충돌을 클라이언트 강제 해제로 우회하지 않음.
- 위험 요소: 번호 재사용·SIM 탈취·공유 번호로 타인 계정 탈취 가능. 같은 기기 여부는 신뢰 증거가 아니며 기존 SNS 접근 불가 시 복구 수단 및 대기/수동 검토 정책 결정 필요. 실제 기술 설계/구현 미수행.
- 다음 작업: 사용자 승인 시 복구 증거·추가 검증·알림/세션 폐기·Firebase 소유권 변경 원자성 및 복구 정책 상세 설계.

## 2026-10-01 — 일반 로그인과 계정 복구 경계 확인

<!-- codex-turn:01a0f653-2a83-76d2-bb18-2e739fd15687 -->

- 브랜치: develop.
- 작업 목표: 충돌 차단 유지와 별도 계정 복구 제안의 의미 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 정상 연결된 SNS 로그인은 유지하고, 다른 Firebase 계정의 기존 전화번호를 이용한 자동 연결만 차단하며 별도 복구 절차를 제공하는 방향 설명. 코드 변경 없음.
- 실행한 테스트와 결과: git diff --check 통과. 설명만 수행하여 Gradle 미실행.
- 유지한 계약: 기존 인증 계약 및 소유권 충돌 차단 유지, 계정/외부 설정 변경 없음. 비밀값 비기록.
- 결정사항: 충돌 안내에서 기존 로그인 또는 계정 복구를 선택하게 하고 복구는 추가 본인 확인 후 처리하는 방향 제안.
- 위험 요소: 복구 정책과 구현은 미확정이며 아직 제공되는 기능이 아님.
- 다음 작업: 사용자 요청 시 계정 복구 상세 정책 및 구현 계획 수립.

## 2026-10-01 — 계정 복구 경계 설명 기록 보완

<!-- codex-turn:01a0f653-6f6c-7a80-a239-fe957be526a5 -->

- 브랜치: develop.
- 작업 목표: 현재 turn의 정확한 식별자를 포함한 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 정상 SNS 로그인은 유지하고 계정 소유권 충돌은 차단하며 별도 복구 절차를 제안한 설명 기록. 앞선 식별자 오기를 이 항목으로 보완하고 과거 기록은 보존.
- 실행한 테스트와 결과: git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 인증 및 충돌 차단 계약 유지, 계정·외부 설정 변경 없음. 비밀값 비기록.
- 결정사항: 복구는 추가 본인 확인을 거치는 별도 기능이며 아직 구현하지 않음.
- 위험 요소: 복구 정책과 구현 범위 미확정.
- 다음 작업: 사용자 요청 시 복구 정책 및 구현 계획 수립.

## 2026-10-01 — 기존 회원 SNS 추가 연결 테스트 사전 확인

<!-- codex-turn:01a0f654-67f9-7301-b75a-649b922135fd -->

- 브랜치: develop.
- 작업 목표: 현재 회원의 다른 SNS 추가 연결 테스트 준비.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 로컬 UI 및 프록시에 providers/link 경로가 없는 상태 확인. 서버 계약의 prepare → start → SDK link → 대상 재인증 → complete 확인. 추가할 SNS를 사용자에게 질문. 구현/외부 설정 변경 없음.
- 실행한 테스트와 결과: 코드/계약 조회 및 git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 기존 전화번호·회원 기록 유지, 서버 승인 없이 Firebase link 실행하지 않음. 비밀값 비기록.
- 결정사항: Firebase 로그인만이 아니라 Identity MEMBER 인증이 필요. 기존 카카오 일반 로그인 허용과 명시 연결 기능은 별도이며 FIREBASE_PROVIDER_LINK_ENABLED 및 provider fence 배포 설정 확인 필요.
- 위험 요소: 연결 대상 SNS, 현재 MEMBER 인증 여부, 실제 배포 연결 플래그 미확인. 다른 UID에 이미 등록된 대상은 연결 충돌 가능.
- 다음 작업: 사용자 대상 SNS 확인 후 MEMBER 로그인 상태 및 테스트 서버 연결 설정 점검, 로컬 도구에 계약에 맞는 명시 연결 UI 준비. 실제 연결 변경은 대상 확인·승인 및 사용자 직접 SNS 인증 후 수행.

## 2026-10-01 — 프론트의 Firebase 인증 후 Identity 교환 흐름 설명

<!-- codex-turn:01a0f657-2995-7bc0-9338-a7335fcd3889 -->

- 브랜치: develop.
- 작업 목표: SNS 인증 후 Identity 로그인 요청 주체 설명 및 Kakao 준비 완료 보고 반영.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 로컬 exchange 버튼의 Firebase ID Token 취득 및 Identity 교환 호출 확인. 실제 앱에서는 프론트가 인증 성공 후 연속 호출하며 사용자에게 별도 로그인을 요구하는 것이 아님을 설명.
- 실행한 테스트와 결과: 코드 조회 및 git diff --check 통과. 설명만 수행하여 Gradle 미실행.
- 유지한 계약: Firebase ID Token은 Identity 검증용, 서비스 요청에는 Identity 토큰 사용. 신규 회원은 ENROLLMENT_REQUIRED 이후 가입 절차 유지. 비밀값 비기록.
- 결정사항: 사용자의 Kakao 계정 준비 완료 보고 수신. 실제 화면/서버의 MEMBER 상태는 이번에 재검증하지 않음. 기존 회원에 다른 SNS 추가 연결 테스트 맥락 유지.
- 위험 요소: 앱 프론트의 자동 교환 구현 여부는 별도 확인 필요, 로컬 도구는 단계별 수동 실행용.
- 다음 작업: 현재 Kakao 회원을 기준으로 대상 SNS 확인 및 명시 연결 테스트 도구/서버 설정 준비.

## 2026-10-01 — 프론트 인증 교환 설명 기록 보완

<!-- codex-turn:01a0f657-2995-78b1-bc70-b7cdc7b8b635 -->

- 브랜치: develop.
- 작업 목표: 현재 turn의 정확한 식별자로 작업 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 프론트의 Firebase 인증 후 Identity 교환 흐름 설명 및 사용자 Kakao 준비 완료 보고 반영. 앞선 식별자 오기를 이 항목으로 보완하며 과거 기록은 보존.
- 실행한 테스트와 결과: git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 인증 계약 유지, 비밀값 비기록, 외부 설정·계정 변경 없음.
- 결정사항: 로컬 테스트 버튼 분리는 단계별 검증용이며 실제 앱은 인증 후 교환 요청을 이어서 수행하는 구조.
- 위험 요소: 실제 프론트 구현 및 현재 MEMBER 상태는 별도 검증 필요.
- 다음 작업: 기존 Kakao 회원에 다른 SNS를 추가하는 테스트 준비.

## 2026-10-01 — 기존 MEMBER SNS 연결 로컬 테스트 화면 구현

<!-- codex-turn:01a0f658-3a2b-7923-838b-608da5a6d39b -->

- 브랜치: develop.
- 작업 목표: 기존 Kakao MEMBER를 유지하면서 Google·Apple 등 SNS 추가 연결을 단계별 검증하는 로컬 화면 제공.
- 변경 파일: 외부 로컬 도구의 app.js, index.html, server.mjs, README.md, app.test.mjs, apple.test.mjs, merge.test.mjs, server.test.mjs 수정; provider-link.mjs, provider-link.test.mjs, provider-link-ui.test.mjs 추가. 저장소는 docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md만 변경.
- 구현 내용: MEMBER·사용자 동의·기존 SNS 재인증 후 prepare/start, 최초 linkAllowed 허용에만 SDK 연결 1회, 대상 SNS 재인증 후 complete, 원래 requestId 기반 status. 동일 UID·전화번호·기존 SNS 보존 검증, 중복 클릭 방지, 응답 유실별 안전한 재시도 제한, 진행 중 로컬 초기화 방지. 프록시는 기존 테스트 Identity의 네 연결 경로만 추가 허용.
- 실행한 테스트와 결과: 실 도구 디렉터리에서 Node 테스트 28개 통과(실제 OAuth/Identity 호출 없이 Mock). 4173 서버 재시작 및 화면·모듈 HTTP 정상 응답 확인. Chrome 새 탭 자동 열기는 ERR_BLOCKED_BY_CLIENT로 시각 검증 미완료. Java 변경이 없어 Gradle clean test 미실행.
- 유지한 계약: Identity API·JWT 계약 변경 없음, 전화번호 이전·자동 병합·해제 없음, 토큰 원문 표시/로그/영구 저장 없음. Learning Core 도메인 코드 추가 없음.
- 결정사항: 테스트 화면만 구현하고 실제 SNS 인증/연결은 사용자가 수행. AWS 설정·배포·Jira·commit·push 없음. 기존 로그인 탭 새로고침하지 않음.
- 위험 요소: 명시 연결 플래그 FIREBASE_PROVIDER_LINK_ENABLED 및 FIREBASE_PROVIDER_CHANGE_FENCE_ENABLED의 실배포 활성 여부 미확인. 서버 Kakao 로그인 허용과 별개. 브라우저 화면 실인증 및 모바일 검증 별도 필요. 진행 상태는 메모리에만 존재.
- 예상 밖 변경: 없음. 기존 사용자 변경 문서 보존 후 기록 추가, Identity 비즈니스 코드는 변경하지 않음.
- 다음 작업: 기존 Kakao로 Firebase 및 Identity MEMBER 로그인 후 새 2-2 섹션 사용. 실연동 전 서버 명시 연결 설정 확인, 승인 없이 설정 활성화하지 않음.

## 2026-10-01 — 기존 SNS 재인증 필수 여부 분석

- 브랜치: develop.
- 작업 목표: SNS 추가 연결 시 매번 기존 SNS 팝업 인증이 필요한지 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없이 ProviderLinkService.remainingProof와 ProviderChangeService.verify/recent 및 ProviderChangeProperties 확인. 서버는 기존 승인 SNS proof와 최근 auth_time을 검증하며 기본 제한은 5분. 로컬 테스트 도구의 별도 재인증 강제와 서버 요구를 구분.
- 실행한 테스트와 결과: 코드 읽기 및 git diff --check. 분석/문서만 변경하여 테스트 미실행.
- 유지한 계약: 기존 SNS 소유 증명 유지, 토큰 강제 갱신만으로 auth_time 갱신되지 않음, 대상 SNS의 start 이후 인증 요구 유지.
- 결정사항: 최근 기존 SNS 로그인 증명 재사용 UX는 가능하나 이번에는 구현하지 않음.
- 위험 요소: 실제 배포 recent-auth 제한 및 클라이언트 상태에 따라 재인증 필요. Identity 세션만으로 대체 불가.
- 다음 작업: 요청 시 로컬 화면에서 유효한 최근 기존 SNS 로그인 재사용 구현 및 테스트.

## 2026-10-01 — 재인증 요구 분석 turn 기록 보완

<!-- codex-turn:01a0f667-94ef-7400-91cf-3e1bc5af5315 -->

- 브랜치: develop.
- 작업 목표: 기존 SNS 재인증 필수 여부 분석의 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 서버는 기존 승인 SNS의 최근 인증(기본 5분)을 요구하고, 별도 팝업을 매번 요구하는 것은 로컬 도구 UX임을 설명. 최근 로그인 재사용은 미구현.
- 실행한 테스트와 결과: git diff --check 통과. 분석/문서만 변경하여 실행 테스트 생략.
- 유지한 계약: 기존 SNS 소유 증명 및 대상 SNS의 start 이후 인증 유지. 비밀값 비기록.
- 결정사항: 코드·계정·외부 설정 변경 없음.
- 위험 요소: 배포 설정과 인증 시각에 따라 재인증 필요.
- 다음 작업: 요청 시 최근 로그인 재사용 UX 구현 및 테스트.

## 2026-10-01 — SNS 추가 연결 인증 UX 설명

- 브랜치: develop.
- 작업 목표: 로그인 상태에서 기존 SNS 재인증으로 인한 사용자 불편과 보안 경계 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 최근 기존 SNS 인증은 재사용하고 오래된 세션만 재인증하는 UX 권장. Identity 세션 유지와 최근 소유 증명을 구분하고, 새 SNS 인증만으로 기존 회원 소유가 입증되지 않는 이유 설명.
- 실행한 테스트와 결과: git diff --check. 설명/문서만 변경하여 실행 테스트 미수행.
- 유지한 계약: 현재 기본 5분 recent-auth 및 기존 SNS 검증 유지. 서버 정책 완화나 클라이언트 우회 없음.
- 결정사항: UI 단계 자동화는 권장안이며 코드 변경 미수행.
- 위험 요소: 재인증 완전 제거는 탈취된 세션에 공격자 SNS를 추가하는 위험. 대상 SNS 재인증 요구도 별도 존재.
- 다음 작업: 사용자 요청 시 최근 인증 재사용 및 단계 자동화 구현, 정책 완화는 별도 검토.

## 2026-10-01 — 오래된 로그인 판단 기준 설명

- 브랜치: develop.
- 작업 목표: SNS 추가 연결의 최근 인증 기준 명확화.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: ProviderChangeService.recent와 ProviderChangeProperties 확인. Firebase auth_time을 서버 현재 시각과 비교하며 기본 5분 초과 시 재인증 요구. 앱 사용 시각·Identity 토큰 재발급과 구분.
- 실행한 테스트와 결과: 코드 읽기 및 git diff --check, 설명/문서만 변경하여 실행 테스트 생략.
- 유지한 계약: 최근 인증·토큰 만료 검사 유지, 서버 설정 변경 없음.
- 결정사항: 기본 코드 기준 설명이며 실배포 설정은 이번에 확인하지 않음.
- 위험 요소: 자동 토큰 갱신이 auth_time을 갱신한다고 오해하지 않도록 안내.
- 다음 작업: 요청 시 최근 SNS 인증 재사용 UX 구현.

## 2026-10-01 — 최근 인증 기준 설명 기록 보완

<!-- codex-turn:01a0f669-50fa-7b92-a3c0-7f60a993f453 -->

- 브랜치: develop.
- 작업 목표: 현재 turn의 최근 인증 기준 설명 기록 식별자 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Firebase auth_time 기준 기본 5분 초과 시 SNS 연결 재인증이 필요하며 일반 로그인 유지 및 토큰 갱신과 다름을 설명. 코드 변경 없음.
- 실행한 테스트와 결과: git diff --check 통과. 문서만 변경하여 실행 테스트 생략.
- 유지한 계약: 기존 recent-auth 검증 유지, 비밀값 비기록.
- 결정사항: 배포 설정 변경 없음, 코드 기본값 설명.
- 위험 요소: 실배포 설정은 별도 확인 필요.
- 다음 작업: 요청 시 최근 인증 재사용 UX 구현 및 검증.

## 2026-10-01 — 5분 재인증 정책의 실제 UX 영향 설명

- 브랜치: develop.
- 작업 목표: 일상적인 SNS 추가 연결에서 5분 기준이 사실상 재인증을 요구한다는 사용자 지적에 답변.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 설명만 수행. 최근 인증 재사용은 가입/로그인 직후에 주로 유효하고 장기 로그인 사용자 불편은 해결하지 못함을 명확화. 시간 연장만으로 문제를 해결한다고 단정하지 않고 정책 완화와 보안 위험 구분.
- 실행한 테스트와 결과: git diff --check, 설명/기록만 변경하여 실행 테스트 생략.
- 유지한 계약: 현재 서버 인증 요구 변경 없음.
- 결정사항: 매번 인증 UX와 유효 세션 기반 연결 허용은 별도 정책 선택이며 이번에 구현하지 않음.
- 위험 요소: 기존 소유 증명 제거 시 탈취 세션에 공격자 SNS 추가 가능. 단순 토큰 갱신은 재인증 대체 불가.
- 다음 작업: 사용자 정책 선택 후 보안 검토 및 구현 범위 확정.

## 2026-10-01 — SNS 추가 연결 unavailable 설정 진단

- 브랜치: develop.
- 작업 목표: 사용자 PROVIDER_CHANGE_UNAVAILABLE 원인 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드의 기능 OFF/서비스 미설치/트랜잭션 실패 경로 확인. AWS 콘솔에서 테스트 서비스 참조 개정 10 및 환경 변수 48개 조회. SNS 허용과 session fence는 true, provider change fence 및 provider link 플래그는 없음. application.yml 기본 false로 명시 연결 비활성 확인.
- 실행한 테스트와 결과: AWS CLI는 자격증명 부재로 조회 실패, Chrome 콘솔 읽기로 확인. git diff --check. 진단만 수행하여 실행 테스트 생략.
- 유지한 계약: 계정 연결·설정 수정·재배포·비밀값 조회 없음.
- 결정사항: 두 연결 플래그의 테스트 환경 활성화는 별도 승인 후 수행. 5분 재인증 문제와 다른 원인.
- 위험 요소: 설정 활성화 후 DB 트랜잭션 및 실제 연결 검증은 남음. 오류 코드 자체는 트랜잭션 오류에도 사용됨.
- 다음 작업: 승인 후 테스트 태스크 정의에 FIREBASE_PROVIDER_CHANGE_FENCE_ENABLED=true 및 FIREBASE_PROVIDER_LINK_ENABLED=true 적용하고 재배포/검증.

## 2026-10-01 — 재인증 UX 정책 설명 기록 보완

<!-- codex-turn:01a0f66b-0edf-7711-9beb-e8d0934d8128 -->

- 브랜치: develop.
- 작업 목표: 5분 재인증 정책의 실제 UX 영향 설명에 현재 turn 식별자 추가.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 장기 로그인 사용자에게 사실상 매번 재인증이 필요함을 설명. 기존 인증 생략은 UI 수정이 아니라 서버 보안 정책 변경임을 구분.
- 실행한 테스트와 결과: git diff --check 통과. 설명/기록만 변경하여 실행 테스트 생략.
- 유지한 계약: 현재 recent-auth 및 기존 계정 소유 검증 유지, 비밀값 비기록.
- 결정사항: 코드·서버 설정 변경 없음.
- 위험 요소: 기존 인증 제거 시 탈취 세션을 통한 공격자 SNS 연결 위험.
- 다음 작업: 사용자 정책 선택 후 보안 검토 및 구현 범위 확정.

## 2026-10-01 — SNS 연결 오류 진단 turn 기록 보완

<!-- codex-turn:01a0f66c-948a-75a2-8a54-78b709f5a5b7 -->

- 브랜치: develop.
- 작업 목표: PROVIDER_CHANGE_UNAVAILABLE 진단의 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: AWS 테스트 서비스 참조 개정 10에서 provider change fence 및 provider link 설정 누락 확인. 코드 기본 false와 연결 기능 비활성 오류 경로 대조. 앞선 진단 기록을 보완하며 과거 기록 보존.
- 실행한 테스트와 결과: AWS 콘솔 읽기 확인 및 git diff --check 통과. 분석/문서만 변경하여 실행 테스트 생략.
- 유지한 계약: 계정·서버 설정·배포 변경 없음, 비밀값 비기록.
- 결정사항: 테스트 환경의 두 연결 플래그 활성화 및 재배포 승인 대기.
- 위험 요소: 활성화 후 실제 연결 및 DB 처리 검증 필요.
- 다음 작업: 사용자 승인 후 테스트 환경만 설정 적용/재배포.

## 2026-10-01 — 테스트 SNS 추가 연결 활성화 배포

- 브랜치: develop.
- 작업 목표: 사용자 승인한 테스트 환경 provider change fence 및 provider link 활성화 적용.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 애플리케이션 코드 변경 없음.
- 구현 내용: AWS ECS tosunsaeng-identity-test:10 기반 개정 11에 FIREBASE_PROVIDER_CHANGE_FENCE_ENABLED=true만 추가하여 먼저 배포. 성공/개정 10 실행 0 확인 후 개정 12에 FIREBASE_PROVIDER_LINK_ENABLED=true 추가하여 배포. 환경 변수 48→49→50개, 두 값 true 확인. 기존 이미지 1cca3c17c3460e01a7ed75b82f4c22ed6fabede0 유지.
- 실행한 테스트와 결과: ECS 최종 배포 성공, 1 실행/0 보류 및 이전 개정 0 실행 확인. 공개 actuator/health UP. git diff --check 통과. 코드 변경이 없어 Gradle 테스트 생략, 실제 SNS 인증/연결 API 변경 요청은 수행하지 않음.
- 유지한 계약: 테스트 서비스만 변경, 운영·IAM·Secret·토큰 TTL·5분 recent-auth·unlink/worker 설정 유지. 기존 provider 보호 OFF writer가 종료된 후 link 신규 접수 활성화. 계정/전화번호 변경 없음.
- 결정사항: 사용자 명시 승인 범위의 두 플래그를 단계적 배포. 기존 CI는 현재 서비스 태스크 정의를 읽고 이미지/SENTRY_RELEASE를 갱신하므로 이번 환경 변수를 유지하는 구조이며 워크플로는 수정하지 않음.
- 위험 요소: 실제 SNS 연결/DB 트랜잭션 검증은 별도 필요. STARTED 연결은 임의 초기화/자동 해제 금지, 문제 발생 시 원래 요청 상태 조회 우선. 연결 데이터 생성 후 provider fence를 끄는 롤백 금지.
- 예상 밖 변경: 없음. 기존 문서 변경 보존, 요청한 외부 설정 두 개 외 변경 없음. Jira/commit/push 미수행.
- 다음 작업: 로컬 도구에서 기존 요청 상태 확인 후 같은 prepare 요청으로 재시도, 최근 인증 만료 시 기존 SNS 재인증. 실제 complete 성공 및 동일 회원/전화번호 유지 검증.

## 2026-10-01 — 테스트 SNS 연결 활성화 배포 기록 보완

<!-- codex-turn:01a0f66e-090c-7241-ae20-b905461d3b9c -->

- 브랜치: develop.
- 작업 목표: 이번 테스트 SNS 연결 활성화 배포의 정확한 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 승인된 provider change fence를 개정 11에 먼저 배포하고 기존 태스크 종료 후 provider link를 추가한 개정 12 배포 완료. 두 설정 true 확인.
- 실행한 테스트와 결과: ECS 배포 성공, 1 실행/0 보류 및 이전 개정 0 실행, 공개 health UP 확인. git diff --check 통과. 코드 변경이 없어 Gradle 미실행.
- 유지한 계약: 운영 서버·기존 인증 정책·기존 이미지 유지. 비밀값 비기록, 실제 계정 연결 미수행.
- 결정사항: 테스트 환경 설정 적용 완료, 추가 배포 없음.
- 위험 요소: 실제 SNS 연결 완료 및 회원 데이터 보존 검증은 남음.
- 다음 작업: 사용자 로컬 테스트에서 상태 조회 후 동일 요청 재시도 및 연결 결과 확인.

## 2026-10-01 — 새로고침 후 SNS 연결 충돌 진단

<!-- codex-turn:01a0f67f-41a7-7422-b39d-ebeffb1e20df -->

- 브랜치: develop.
- 작업 목표: 첫 연결 실패 후 새로고침한 사용자의 PROVIDER_CHANGE_CONFLICT 원인 조사.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 로컬 화면의 Google PREPARE_UNKNOWN 확인, 현재 요청 상태 조회 결과 PROVIDER_OPERATION_NOT_FOUND. 테스트 DB provider_link_attempts에 GOOGLE STARTED 1건(17:01 시작, 17:06 만료) 확인. 현재 로그인 회원과 DB 작업 소유자의 일치 및 실제 Firebase 연결 결과는 미확정. ProviderLinkService.start의 보호 slot 점유와 prepare 충돌 조건, 새로고침 시 로컬 작업 메모리 유실 및 기존 작업 복구 UI 부재 확인.
- 실행한 테스트와 결과: 코드/로컬 UI/Atlas 읽기 및 상태 조회만 수행. 세션 control 대조는 Atlas 화면이 이전 컬렉션 내용을 유지하여 증거로 사용하지 않음. git diff --check 통과. 코드 변경 없어 실행 테스트 생략.
- 유지한 계약: 계정·연결·DB 수정/삭제/잠금 해제 없음. 불명 작업의 SDK 재실행 금지, STARTED 만료 후 자동 해제 금지 유지. 식별자·비밀값 기록 없음.
- 결정사항: 남은 STARTED 작업은 사용자 설명과 정황상 일치하나 정확한 소유 대조 전 확정하지 않음. 새 준비 요청 반복 대신 원래 작업과 Firebase 결과 대조 필요.
- 위험 요소: 현재 로컬 도구는 새로고침 후 원래 요청 복구 불가. 다른 회원 소유 SNS 선택 실패 후 원격 연결 성공 여부가 불명확할 수 있으므로 임의 activeLogoutId 초기화 금지.
- 다음 작업: 정확한 회원·원래 작업·Firebase 연결 결과를 대조하고 별도 승인된 조건부 복구 설계. 로컬 작업 복구와 명확한 실패 안내 개선 검토.

## 2026-10-01 — 실패한 SNS 연결 작업의 보존과 복구 설명

<!-- codex-turn:01a0f67f-41a7-7422-b39d-ebeffb1e20df -->

- 브랜치: develop.
- 작업 목표: 첫 실패 후 작업이 계속 남는지 및 해결 방향 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: ProviderLinkAttempt.start가 cleanupAt을 제거하고 만료된 STARTED를 조회 시 ACTION_REQUIRED로 표시하는 구현 확인. PREPARED 만료와 구분. 현재 자동 실패 종료/재개 복구가 부족함을 설명.
- 실행한 테스트와 결과: 코드 읽기 및 git diff --check. 설명/문서만 변경하여 실행 테스트 미수행.
- 유지한 계약: 원격 결과 불명 시 자동 보호 해제 금지. DB/계정/외부 설정 수정 없음.
- 결정사항: 실제 연결 부재 및 이전 실행 종료 증거 확보 후 조건부 복구 필요. 실패 취소/상태 복구 설계는 제안이며 미구현.
- 위험 요소: SDK 오류만으로 원격 연결 미실행을 단정할 수 없음. 현재 작업의 정확한 회원 소유 대조 미완료.
- 다음 작업: 정확한 작업과 원격 상태 대조, 승인된 복구 및 재시도 UX 구현 범위 확정.

## 2026-10-01 — 실패 연결 작업 보존 설명 기록 보완

<!-- codex-turn:01a0f683-a3f8-7e60-8bb8-1f2f618ac6c9 -->

- 브랜치: develop.
- 작업 목표: 실패 연결 작업 보존 및 복구 설명의 정확한 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: PREPARED 만료와 STARTED 보호 유지의 차이, 확정된 원격 결과와 이전 실행 종료 증거에 기반한 조건부 복구 필요성을 설명. 앞선 식별자 오기를 보완하며 과거 기록 보존.
- 실행한 테스트와 결과: git diff --check 통과. 문서만 변경하여 실행 테스트 생략.
- 유지한 계약: 불명 상태의 자동 보호 해제 금지, 비밀값 비기록.
- 결정사항: 복구 기능은 제안 단계, 코드·계정·DB·외부 설정 변경 없음.
- 위험 요소: 정확한 작업 소유 및 Firebase 연결 결과 대조가 아직 필요.
- 다음 작업: 원래 작업 대조 및 승인된 복구/재시도 UX 구현 범위 확정.

## 2026-10-01 — 막힌 SNS 연결 작업 복구 사전 점검

- 브랜치: develop.
- 작업 목표: 사용자 요청에 따라 현재 미완료 연결을 안전하게 복구하기 위한 정확한 상태 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 사용자 첫 SDK 오류가 auth/credential-already-in-use임을 확인. 테스트 Atlas 실제 user_session_controls 컬렉션을 열고 exact owner 필터로 단일 문서 조회, 기존 STARTED attempt의 link slot 일치 확인. Firebase 콘솔에서 작업 UID와 일치하는 계정의 제공자는 OIDC와 Phone이며 Google은 없음 확인. 현재 로컬 로그인과 owner의 직접 대조 및 이전 SDK 종료 확인은 추가 필요.
- 실행한 테스트와 결과: CloudShell 재연결 후 임시 venv에 공식 boto3/pymongo/google-auth/requests 설치. 테스트 MongoDB Secret을 메모리로 읽는 제한 진단은 ServerSelectionTimeoutError로 종료, 비밀값 출력 없음. Atlas 테스트 프로젝트 허용 목록에 기존 단일 서버 IP만 있으며 관리 셸 IP는 미포함 확인. git diff --check 통과, 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 사용자·Firebase 연결·DB·네트워크 설정 수정 없음. 무조건 slot 초기화나 다중 문서 개별 변경 미수행. 운영 자원 변경 없음.
- 결정사항: 조건부 트랜잭션 복구를 위해 테스트 Atlas에 관리 셸 단일 IP /32 임시 허용 승인 필요. 승인 전 접속 허용 변경하지 않음.
- 위험 요소: Google 연결 부재만으로 SDK 종료를 단정하지 않음. 복구 전 정확한 binding/version/phase 및 다른 미완료 작업 대조 필요. 관리 셸 IP는 세션마다 바뀔 수 있음.
- 다음 작업: 임시 네트워크 접근 승인 후 DB/Firebase 읽기 검증과 제한 복구 설계, 복구 적용 시 변경 내용 확인. 임시 접근은 작업 종료 후 제거.

## 2026-10-01 — SNS 연결 복구 사전 점검 기록 보완

<!-- codex-turn:01a0f684-e7ea-7682-a5af-15695f51935b -->

- 브랜치: develop.
- 작업 목표: 현재 연결 복구 사전 점검의 정확한 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 원래 연결 작업과 회원 세션 잠금 일치, 해당 Firebase UID의 Google 연결 부재 확인. 관리 셸의 DB 접속 시간 초과 및 Atlas 허용 목록에 해당 IP 미포함 확인.
- 실행한 테스트와 결과: 읽기 검증 및 git diff --check 통과. 코드 변경 없어 실행 테스트 생략.
- 유지한 계약: DB·계정·네트워크 설정 변경 없음, 비밀값 비기록.
- 결정사항: 단일 관리 IP 임시 허용 승인 및 이전 SDK 실행 종료 확인 대기.
- 위험 요소: 현재 로그인 회원과 작업 소유 직접 대조 및 원자적 복구 검증이 남아 있음.
- 다음 작업: 승인 후 제한된 검증과 조건부 복구 진행, 종료 후 임시 접근 제거.

## 2026-10-01 — reissue 절대 만료 헤더 null 원인 조사

<!-- codex-turn:01a0f68c-1d9d-7901-8991-188d8660c536 -->

- 브랜치: develop. 특정 Jira 이슈 구현 요청 없음.
- 작업 목표: /auth/reissue의 Reissue-Access-Expires-At 및 Reissue-Refresh-Expires-At 헤더가 null로 읽히는 원인 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 애플리케이션 변경 없음.
- 구현 내용: TokenReissueService의 Recovery OFF 결과에서 절대 만료 필드가 null이며 AuthController는 accessExpiresAt 비-null일 때만 두 헤더를 설정함을 확인. application.yml의 AUTH_REISSUE_RECOVERY_ENABLED 기본 false, 조건부 Recovery Bean 설치, ON 경로 실제 만료 시각 반환 확인. CORS 노출 설정은 저장소에서 발견되지 않음.
- 실행한 테스트와 결과: ./gradlew test --tests '*ReissueRecoveryHttpTests' --tests '*ReissueRecoveryConfigurationTests' --tests '*TokenReissueServiceTests' BUILD SUCCESSFUL. 실제 실행 suite/count는 XML 결과로 확인. git diff --check 수행. 전체 clean test는 코드 변경 없는 진단이라 미실행.
- 유지한 계약: 재발급/멱등 재전달/세션 fence/암호화 계약 변경 없음. 배포 환경변수와 프록시, 외부 설정 변경 없음. 토큰/비밀값 조회·출력·기록 없음.
- 결정사항: 서버가 헤더를 생략하는 OFF 경로와 브라우저 CORS로 읽지 못하는 경우를 구분. 200 원 응답 헤더와 배포 flag 확인이 우선. 헤더만을 위해 복구 flag를 즉시 켜지 않으며 fence·암호키·인덱스·클라이언트 요청 ID 준비 및 runbook 검증 필요.
- 위험 요소: 실제 배포 flag/요청 상태/응답 헤더/클라이언트 플랫폼/프록시 미확인. 오류 응답에서도 헤더가 없는 것은 의도된 동작. 프론트 가이드 성공 헤더 안내는 Stage 9 ON 조건과 함께 읽어야 함.
- 예상 밖 변경: 없음. 기존 문서 변경 보존, 코드/커밋/push/Jira/배포 변경 없음.
- 다음 작업: 사용자 요청 환경과 원 응답 헤더 확인 후 OFF 경로의 헤더 제공 개선 또는 exact-origin CORS 노출 필요 여부 판단(구현 별도 승인/요청).

## 2026-10-01 — reissue recovery 활성화 재배포 사전 점검 중단

<!-- codex-turn:01a0f68d-bee4-7942-a6cd-94c3321b2650 -->

- 브랜치: develop.
- 작업 목표: 사용자 요청에 따른 recovery 활성화/재배포 전 대상 및 선행 조건 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Stage 9 runbook 및 main/develop 배포 분리 문서 확인. 기존 AWS 테스트 서비스 콘솔에서 task definition tosunsaeng-identity-test:12, 실행 1/대기 0, 배포 성공 표시 읽기 확인.
- 실행한 테스트와 결과: 문서/UI 읽기, git diff --check. 이번에는 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 운영/테스트 경계 유지. 환경변수 변경, 태스크 등록, 재배포, 커밋/push/Jira 변경 미수행. 비밀값 미조회·비기록.
- 결정사항: test/staging 대상 확인 질문 후 준비 점검 중 사용자 중단으로 외부 변경 진행하지 않음.
- 위험 요소: 실제 recovery/fence 값, 암호키 연결, DB 인덱스, 프론트 Idempotency-Key 준비 미검증. 콘솔 배포 성공은 recovery 활성화를 뜻하지 않음.
- 예상 밖 변경: 없음. 기존 미커밋 기록 보존.
- 다음 작업: 사용자 재개 요청 시 배포 대상 확정 및 필수 준비 검증 후 활성화/재배포 진행.

## 2026-10-01 — reissue 재배포 중단 안내 기록 보완

<!-- codex-turn:01a0f68f-2f5f-7d22-93ea-c9c3b76d8d18 -->

- 브랜치: develop.
- 작업 목표: 현재 turn의 작업 기록 누락 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 애플리케이션 변경 없음. 재배포 미실행 및 대상 확인 대기 상태 기록.
- 실행한 테스트와 결과: git diff --check. 기록만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 WORKLOG 보존, 비밀값 비기록, 외부 설정/배포/커밋/push 변경 없음.
- 결정사항: test/staging 대상 확인 및 세션 fence·키·인덱스·클라이언트 준비 검증 후 재개.
- 위험 요소: 선행 조건 및 실환경 recovery 설정 미검증.
- 예상 밖 변경: 없음. 기존 기록 변경 보존.
- 다음 작업: 사용자 배포 대상 확인 후 안전한 활성화/재배포 진행.

## 2026-10-01 — 테스트 Google 연결 실패 작업 조건부 복구 완료

<!-- codex-turn:01a0f68c-7cc7-7223-9d90-038554e45929 -->

- 브랜치: develop.
- 작업 목표: credential-already-in-use 이후 남은 테스트 회원의 실패 연결 작업 1건을 승인된 범위에서 복구하고 준비 재시도 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 애플리케이션 코드 변경 없음.
- 구현 내용: 사용자 팝업 종료·재시도 없음 확인 및 복구 승인 후, 정확한 작업/소유/버전/단계와 Firebase Google 연결 부재, 다른 미완료 작업 부재 검증. dry-run 후 snapshot/majority 트랜잭션으로 provider_link_recovery_audit에 원본 작업과 보호 상태를 백업하고 해당 실패 작업만 활성 컬렉션에서 제거, 해당 slot만 해제 및 제어 revision/version 증가. Google 차단 및 인증 floor 유지. 임시 관리 접속 /32 허용은 검증 후 제거하고 기존 서버 접근 보존 확인.
- 실행한 테스트와 결과: RECOVERY_DRY_RUN_PASSED_NO_WRITES 및 RECOVERY_COMMITTED 확인. 감사 백업 존재, 실패 작업 제거, slot 해제, epoch/Google 차단/auth floor/회원/binding/원격 제공자/전화번호 보존 확인. Chrome에서 기존 Kakao 재인증 후 Google 연결 prepare 성공 확인. 문서 diff 검사 수행, 코드 변경 없는 운영 복구로 Gradle 미실행.
- 유지한 계약: 운영 자원 불변, 기존 회원·Kakao·전화번호·세션 보호 유지, 비밀값/개인 식별자 비기록. SDK 임의 재실행 없음.
- 결정사항: 기존 실패 작업은 감사 백업으로 보존. 일반 실패 복구 기능 구현과 재배포는 이번 범위 제외. 새로운 Google 연결은 PREPARED까지만 확인.
- 위험 요소: 실제 Google 연결/완료 검증은 남아 있음. 다른 회원 소유 Google로 다시 시도하면 동일 실패가 재발할 수 있음. 감사 snapshot의 무조건 복원 금지.
- 예상 밖 변경: 없음. 기존 문서 변경 보존, commit/push/Jira/배포 변경 없음.
- 다음 작업: 다른 회원에게 연결되지 않은 Google 계정을 사용자가 선택하여 SDK 연결·대상 재인증·완료 검증 진행. 이후 실패 종료 및 새로고침 복구 UX 별도 개선.

## 2026-10-01 — 테스트 reissue recovery 활성화 선행 설정 확인

- 브랜치: develop.
- 작업 목표: 사용자 요청에 따라 테스트 recovery 활성화 재배포 가능 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: CloudShell 읽기 전용 AWS CLI로 실제 서비스 개정 12, desired/running 1, rollout COMPLETED 확인. 해당 개정 AUTH_REISSUE_RECOVERY_ENABLED=false, AUTH_SESSION_FENCE_ENABLED=true 확인. recovery 키 ID/환경/파일 설정 및 keyring secret 주입·volume mount 없음 확인. runbook과 entrypoint의 필수 기동 조건 대조.
- 실행한 테스트와 결과: AWS 조회 및 코드/운영 가이드 대조. git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 운영 및 실행 서비스 변경 없음, 비밀값 조회·출력 없음. Idempotency-Key 필수화 및 exact-origin CORS 노출 별도 검증 필요성 유지.
- 결정사항: true만 설정하면 기동 실패하므로 배포 미실행. 테스트 전용 keyring 연결 및 필요한 최소 읽기 권한 승인 확인 후 준비 진행.
- 위험 요소: 실제 DB 인덱스/키 형식/클라이언트 멱등 요청 준비 미검증. OFF는 헤더 생략 원인이지만 실제 HTTP 원 응답과 브라우저 CORS 가시성은 별도 검증 필요.
- 예상 밖 변경: 없음, 기존 기록 보존. commit/push/Jira 변경 없음.
- 다음 작업: 테스트 keyring 주입 범위 승인, 키/인덱스/클라이언트 준비 확인 후 활성화 배포와 헤더·동일 요청 재전달 검증.

## 2026-10-01 — 테스트 recovery 사전 점검 turn 기록 보완

<!-- codex-turn:01a0f696-a3f8-79c3-91c5-3e9eb3b2db77 -->

- 브랜치: develop.
- 작업 목표: 이번 테스트 recovery 활성화 사전 점검의 turn 식별자 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 실행 개정 12의 recovery=false, fence=true 및 필수 keyring 주입/키 ID/환경 설정 누락 확인 결과 기록. 코드 변경 없음.
- 실행한 테스트와 결과: AWS 읽기 조회와 기동 조건 대조 완료. git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 비밀값 비기록, 실행 서비스/운영/권한 변경 없음.
- 결정사항: 테스트 keyring 연결과 필요한 최소 읽기 권한 승인 대기, 배포 미실행.
- 위험 요소: 키 형식/실제 인덱스/클라이언트 Idempotency-Key 준비와 응답 헤더 검증 필요.
- 예상 밖 변경: 없음, 기존 기록 보존.
- 다음 작업: 승인 후 준비 검증 및 테스트 활성화 배포.

## 2026-10-01 — 승인된 테스트 reissue recovery 활성화 배포 완료

- 브랜치: develop.
- 작업 목표: 테스트 재발급 응답 복구 및 절대 만료 헤더 경로 활성화.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 애플리케이션 코드 변경 없음.
- 구현 내용: 사용자 승인 후 Secrets Manager 테스트 keyring을 메모리에서 형식·32바이트 키 길이·활성 ID 존재 검증(원문 비출력). 실행 역할의 기존 GetSecretValue 권한 허용 확인하여 IAM 변경 없음. 기존 개정 12를 복제하여 개정 13에 recovery=true, active-key-id=test-v1, environment=test 및 AUTH_REISSUE_ENCRYPTION_KEYRING_CONTENT secret 참조만 추가. 기존 이미지·네트워크·역할·다른 설정 유지. AWS 개정 간 비교에서 environment/secrets 외 변경 없음 확인.
- 실행한 테스트와 결과: 관련 45개 테스트 통과, ./gradlew clean test 전체 1024개 성공(실패/오류/skip 0). 최초 clean 명령은 캐시 sandbox 거절, 승인된 재실행 성공. Atlas 실제 회전 unique/partial 인덱스와 응답 source unique/cleanup TTL 인덱스 존재 확인. 개정 13 startup 성공, 기동 실패/오류 유형 미검출, ALB healthy, 최종 ECS COMPLETED 1 running/0 pending 확인. 공개 health 200 UP. 가짜 credential smoke에서 헤더 누락 400 INVALID_REISSUE_REQUEST_ID, canonical ID 포함 401 INVALID_REFRESH_TOKEN, no-store 확인. git diff --check 수행.
- 유지한 계약: 운영 자원/계정/세션 데이터 변경 없음. 실제 토큰/키/URI 비기록. 기존 이미지와 fence 유지. 테스트 ON 환경에서 소문자 UUID v4 Idempotency-Key 필수화.
- 결정사항: 키 주입은 기존 entrypoint가 private runtime 파일 생성 후 환경변수 제거하는 방식. 등록 입력과 반환 객체 직접 비교는 일치하지 않아 개정 간 실제 필드 비교로 재검증했으며 예상 변경만 존재. 배포 완료 후 점검 Python 세션 종료.
- 위험 요소: 실제 사용자 성공 재발급의 두 만료 헤더 및 동일 ID 재전달은 아직 실환경 미검증(HTTP 자동 테스트 통과). 모바일 crash/single-flight 및 replica-set failover 등 runbook 운영 증빙은 별도. 로컬 테스트 프록시는 응답 헤더를 전달하지 않아 서버 ON 이후에도 브라우저 헤더 조회가 null일 수 있음. exact-origin CORS 노출 별도 확인 필요.
- 예상 밖 변경: 없음. 기존 문서 수정 보존. commit/push/Jira 변경 없음. 전체 clean test는 로컬 코드 검증으로 배포 이미지 자체 테스트와 구분.
- 배포 전 확인 사항: 키 형식/접근 권한/fence/인덱스 확인 완료, 클라이언트 멱등 헤더 요구 안내. 긴급 중단은 enabled 유지 + maintenance 사용(runbook 준수).
- 다음 작업: 사용자 테스트 로그인으로 성공 재발급 원 응답 헤더/동일 ID replay 검증. 필요 시 로컬 프록시 헤더 전달 및 프론트 오류 처리 보완.

## 2026-10-01 — 테스트 recovery 배포 완료 turn 기록 보완

<!-- codex-turn:01a0f698-cf44-7e62-8ea4-a9d7f84ae171 -->

- 브랜치: develop.
- 작업 목표: 승인된 테스트 recovery 배포 작업의 현재 turn 식별자 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 테스트 개정 13 recovery 활성화 및 기존 테스트 keyring 주입 완료. 기존 이미지/IAM/운영 유지. 애플리케이션 코드 변경 없음.
- 실행한 테스트와 결과: 전체 clean test 1024개 통과. ECS COMPLETED, ALB healthy, 공개 health UP 및 가짜 credential을 이용한 ON 계약 smoke 확인. git diff --check 통과.
- 유지한 계약: 비밀값 비기록, 실제 사용자 세션 변경 없음, ON 환경 Idempotency-Key 필수.
- 결정사항: 배포 완료, 로컬 프록시 헤더 전달 수정은 별도 작업.
- 위험 요소: 실제 사용자 성공 응답의 절대 만료 헤더와 동일 ID replay 실환경 검증은 남아 있음.
- 예상 밖 변경: 없음. 기존 기록 보존.
- 다음 작업: 실제 성공 응답 헤더/replay 검증 및 필요 시 프록시 개선.

## 2026-10-01 — Chrome 기존 MEMBER SNS 추가 연결 성공 확인

- 브랜치: develop.
- 작업 목표: 사용자가 완료한 기존 회원의 SNS 추가 연결 테스트 결과 읽기 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome 로컬 테스트 탭 검증표에서 Kakao 인증/Identity 로그인/SNS 추가 연결 성공 및 서버 COMPLETED, 동일 Firebase UID, 기존 전화번호·SNS 유지 표시 확인. 현재 폼은 완료 상태 정리 후 연결 전으로 초기화되어 있으며 계정 연결과 토큰은 유지된다는 안내 확인. 로컬 도구 소스에서 complete 응답 COMPLETED 검사 후 성공 표시하는 흐름 대조.
- 실행한 테스트와 결과: UI 읽기 및 성공 표시 조건 코드 대조. 연결/로그인 요청 재실행 없음. 문서만 변경하여 Gradle 미실행, git diff --check 수행.
- 유지한 계약: 개인정보·토큰 비출력, 계정·인증 상태 변경 없음.
- 결정사항: 기존 Kakao 회원에 Google 추가 연결 완료 테스트는 성공으로 확인. 폼 초기화는 연결 해제가 아님.
- 위험 요소: 새 Google 단독 로그인 후 동일 Identity 회원 확인, Android/iOS 실기기 동작, 실패 후 복구 UX는 이번 확인에 포함되지 않음. 이번에는 DB 직접 재조회 없이 UI 완료 결과 및 코드 근거로 확인.
- 예상 밖 변경: 없음. 기존 문서 변경 보존.
- 다음 작업: 사용자가 원하면 추가한 Google 단독 로그인 및 동일 회원/기록 접근 확인. 실패·새로고침 복구 개선은 별도.

## 2026-10-01 — SNS 연결 결과 확인 turn 기록 보완

<!-- codex-turn:01a0f6a2-8df8-7990-aa0a-9fd4df5cc4bf -->

- 브랜치: develop.
- 작업 목표: 현재 SNS 추가 연결 결과 확인 작업의 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome 검증표의 서버 COMPLETED 및 기존 UID/전화번호/SNS 유지 표시를 확인. 폼 초기화는 테스트 상태 정리이며 연결 해제가 아님을 설명.
- 실행한 테스트와 결과: UI 읽기와 로컬 도구 성공 표시 조건 대조, git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 계정/로그인/연결 변경 없음, 비밀값 비기록.
- 결정사항: 추가 연결 성공 확인, 새 Google 단독 로그인 검증은 별도.
- 위험 요소: 실기기 및 실패 복구 UX 미검증.
- 예상 밖 변경: 없음. 기존 기록 보존.
- 다음 작업: 사용자 요청 시 새 SNS 단독 로그인으로 동일 회원 접근 확인.

## 2026-10-01 — 추가한 Google 단독 로그인과 프로필 인증 확인

- 브랜치: develop.
- 작업 목표: 사용자가 진행한 Google 로그인 성공 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome 검증표의 Google Firebase 인증/Identity 로그인 성공 확인 후 내 프로필 인증 확인 버튼으로 읽기 요청 수행. 서버 MEMBER 인증 성공 확인.
- 실행한 테스트와 결과: 실제 테스트 프로필 API MEMBER 인증 성공. git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 로그인/연결 재실행 없음, 개인정보/토큰 비출력.
- 결정사항: 추가 SNS 연결 후 Google 단독 Identity 로그인과 보호 API 인증 성공 확인.
- 위험 요소: 이전 Identity UUID와 직접 비교하거나 LC 기록 동일성 검증한 것은 아님. 모바일 실기기/실패 복구 UX는 별도.
- 예상 밖 변경: 없음. 기존 문서 변경 보존.
- 다음 작업: 필요 시 이전 회원 동일성 및 LC 기록 검증, 실패 후 연결 복구 개선.

## 2026-10-01 — Google 로그인 검증 turn 기록 보완

<!-- codex-turn:01a0f6a4-42a5-7b30-9574-01b57551ac13 -->

- 브랜치: develop.
- 작업 목표: Google 단독 로그인 검증의 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Google 인증·Identity 로그인 성공 표시 확인 후 프로필 읽기 API로 MEMBER 인증 성공 확인.
- 실행한 테스트와 결과: 실환경 프로필 인증 성공, git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 개인정보·토큰 비기록, 연결/로그인 재실행 없음.
- 결정사항: Google 로그인과 보호 API 접근 검증 완료.
- 위험 요소: 이전 Identity UUID 직접 비교 및 LC 기록 동일성은 미검증.
- 예상 밖 변경: 없음. 기존 기록 보존.
- 다음 작업: 필요 시 회원 동일성/LC 기록 검증 및 실패 복구 UX 개선.

## 2026-10-01 — SNS 연결 실패 후 재시도 복구 수정 계획 설명

- 브랜치: develop.
- 작업 목표: credential-already-in-use 이후 지속된 PROVIDER_CHANGE_CONFLICT의 원인과 수정 방향 설명(구현 제외).
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: ProviderLinkService.start의 provider 차단 및 slot 점유, prepare의 기존 slot 충돌 검사, ProviderLinkAttempt의 STARTED cleanupAt=null 및 실패 종료 상태 부재 확인. 만료 status는 ACTION_REQUIRED일 뿐 해제되지 않음. 로컬 ProviderLinkFlow.link는 SDK 호출 전 LINK_UNKNOWN 설정 후 예외 종료, 메모리 전용 상태라 새로고침 시 원 요청 ID 소실 확인.
- 실행한 테스트와 결과: 서버/로컬 도구 코드 및 Stage 10 runbook 5.5 대조. git diff --check 수행. 분석만 수행하여 Gradle 미실행.
- 유지한 계약: 타 회원 소유 SNS 연결 거절 유지. 만료/클라이언트 실패 주장만으로 slot 또는 provider 보호를 해제하지 않음. 개인정보/토큰 비기록.
- 결정사항: 실패 안내와 원 요청 복구 UX, 인증된 pending 조회 및 조건부 실패 종료/reconcile 경로 제안. SDK 종료·자동 재시도 부재 및 원격 상태 증거가 충분한 경우에만 exact owner/version/phase 트랜잭션으로 종료/slot 해제. provider 차단/auth floor는 보존, 실패 작업은 삭제하지 않고 감사 가능 종료 상태 유지. 불명 상태는 ACTION_REQUIRED 유지. 구현/API 명칭 미확정.
- 위험 요소: Firebase와 Mongo는 단일 원자 트랜잭션이 아니며 원격 부재 조회만으로 지연 SDK 호출 종료를 보장할 수 없음. 자동 복구의 증거 요건/관리 경로 구분 설계 필요. 기존 클라이언트 호환성 및 늦은 complete/동시 요청 검증 필요.
- 예상 밖 변경: 없음. 코드·외부 설정·계정·Jira 변경 없음.
- 다음 작업: 구현 요청 시 API/상태 계약과 복구 증거 요건 확정 후 서버·테스트 도구 및 회귀 테스트 구현. 성공 로그인 경로는 유지.

## 2026-10-01 — SNS 실패 복구 수정 계획 turn 기록 보완

<!-- codex-turn:01a0f6a5-445d-7f61-b677-0b5d40dad13e -->

- 브랜치: develop.
- 작업 목표: 실패 후 연결 재시도 문제 분석과 수정 계획의 현재 turn 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 타 회원 소유 SNS 거절은 정상이며, STARTED 잠금 유지·실패 종료 경로 부재·새로고침 시 원 요청 정보 소실이 지속 충돌 원인임을 설명. 조건부 종료/감사 기록/pending 조회/실패 안내 개선 제안. 코드 구현 없음.
- 실행한 테스트와 결과: 코드 및 운영 가이드 대조, git diff --check 통과. 분석만 수행하여 Gradle 미실행.
- 유지한 계약: 원격 결과 불명 시 보호 유지, 클라이언트 주장이나 만료만으로 잠금 해제 금지, 비밀값 비기록.
- 결정사항: 구현 요청 후 복구 증거 요건과 API/상태 계약 확정.
- 위험 요소: 원격 SDK와 DB의 원자성 부재 및 지연 호출 종료 증명 필요.
- 예상 밖 변경: 없음. 기존 기록 보존, 외부 변경 없음.
- 다음 작업: 승인된 구현 범위 확정 후 실패 종료·복구 UX와 회귀 테스트 추가.

## 2026-10-01 — SNS 연결 취소·재시도 UX 추가 계획

- 브랜치: develop.
- 작업 목표: 오래 걸리는 SNS 연결을 사용자가 취소하고 재시도할 수 있는지 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 기존 코드 분석에 기반해 PREPARED의 조건부 즉시 취소와 STARTED 이후 취소 요청/원격 상태 확인을 구분하는 계획 제시. UI 대기 종료와 원격 작업 취소는 다르며, 안전한 종료 확인 전 중복 SDK 연결 금지. 연결 완료 상태는 취소 대신 완료 안내, 연결 해제는 별도 인증 작업.
- 실행한 테스트와 결과: 이전 코드 분석 근거 사용 및 git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 시간 경과/팝업 닫힘만으로 잠금 해제 금지, 기존 SNS/전화번호 보호 및 미확정 대상 차단 유지. 비밀값 비기록.
- 결정사항: 실패 복구 계획에 취소 요청·진행 상태 복원·종료 후 재시도 UX 포함 제안. 새 상태/API 명칭과 시간 기준은 구현 전 확정. 아직 구현 없음.
- 위험 요소: Firebase SDK 지연 성공과 취소의 경합. 서버가 이전 실행 종료를 입증하지 못하면 즉시 재시도를 보장할 수 없음. 기존 SNS 로그인 유지 범위는 보호 정책에 따라 검증 필요.
- 예상 밖 변경: 없음. 기존 기록 보존, 외부 변경 없음.
- 다음 작업: 취소/start/complete 경합, 응답 유실, 새로고침 및 지연 SDK 결과 테스트를 구현 범위에 포함.

## 2026-10-01 — SNS 취소·재시도 계획 turn 기록 보완

<!-- codex-turn:01a0f6a7-07f7-7e12-a2e3-9bef009190e0 -->

- 브랜치: develop.
- 작업 목표: SNS 연결 취소·재시도 설계 설명의 현재 turn 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 준비 단계 취소, 연결 진행 중 종료 확인, 완료 후 별도 해제의 차이를 설명. 취소 버튼·상태 복원·안전한 종료 후 재시도와 지연 성공 경합 테스트를 계획에 포함 제안. 코드 변경 없음.
- 실행한 테스트와 결과: git diff --check 통과. 계획 설명만 수행하여 Gradle 미실행.
- 유지한 계약: 팝업 종료/시간 경과만으로 잠금 해제 금지, 결과 불명 시 중복 연결 제한, 비밀값 비기록.
- 결정사항: 취소와 실패 복구를 함께 설계하되 실제 구현은 후속 요청 대기.
- 위험 요소: 원격 SDK 지연 완료와 취소 경합으로 즉시 재시도를 항상 보장할 수 없음.
- 예상 밖 변경: 없음. 기존 기록 보존, 외부 변경 없음.
- 다음 작업: API/상태 계약과 종료 증거 요건 확정 후 취소·복구·경합 테스트 구현.

## 2026-10-01 — SNS 취소·실패 복구 상세 수정 계획 작성

- 브랜치: develop.
- 작업 목표: 서버 상태/API/프론트/보안 증거/테스트/배포 순서 상세 설명.
- 변경 파일: docs/contracts/provider-link-cancel-recovery-plan.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 현재 코드와 제안을 구분한 계층형 계획 문서 작성. PREPARED 원자 취소, STARTED 취소 요청과 제한된 조건부 복구, 실패 감사 상태, 원 요청 복원, pending 조회/권한, 경합 테스트 및 단계 배포 정리.
- 실행한 테스트와 결과: ProviderLinkAttempt/Service/Controller 및 이전 로컬 도구 조사 근거 대조, git diff --check 수행. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: status에서 SDK 실행권한 재부여 금지, 타 회원 SNS 거절, 불명 원격 실행에 대한 보호 유지, 비밀값 비기록.
- 결정사항: 클라이언트 오류/팝업 종료/원격 부재만으로 STARTED 자동 종료를 보장하지 않음. 일반 취소 API와 승인된 운영 복구를 분리 제안. API/상태 이름은 확정 전.
- 위험 요소: 모든 실패에서 즉시 자동 재시도 요구는 현재 직접 Firebase SDK 구조만으로 충족 불가하며 별도 설계 필요. 구 클라이언트 상태 호환성과 실제 앱 구현 별도.
- 예상 밖 변경: 없음. 코드/계정/배포/Jira 변경 없음, 기존 기록 보존.
- 다음 작업: 구현 요청 시 문서의 범위 및 종료 증거 요건을 기준으로 계약 확정 후 진행.

## 2026-10-01 — SNS 취소·복구 상세 계획 turn 기록 보완

<!-- codex-turn:01a0f6a8-556f-7661-94db-2e163c63943c -->

- 브랜치: develop.
- 작업 목표: 상세 수정 계획 설명의 현재 turn 식별자 기록.
- 변경 파일: docs/contracts/provider-link-cancel-recovery-plan.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 서버 상태·취소/실패 보고/작업 복원 API 제안, 인증·감사·원자적 복구 조건, 프론트 UX, 경합 테스트와 단계 배포 계획 작성. 애플리케이션 구현 없음.
- 실행한 테스트와 결과: 현재 코드 및 운영 계약 대조, git diff --check 통과. 문서 변경만 수행하여 Gradle 미실행.
- 유지한 계약: 불명 STARTED 자동 해제 금지, SDK 실행 허가 재부여 금지, 개인정보·비밀값 비기록.
- 결정사항: 준비 단계 취소와 진행 단계 취소 요청을 구분하며 모든 오류의 즉시 자동 재시도는 보장하지 않음.
- 위험 요소: Firebase 지연 실행과 DB 상태 간 원자성 부재, 실제 앱 및 구 클라이언트 호환성 검증 필요.
- 예상 밖 변경: 없음. 기존 기록 보존, 외부 변경 없음.
- 다음 작업: 구현 요청 후 제안 계약과 증거 요건 확정 및 서버·클라이언트 회귀 테스트 구현.

## 2026-10-01 — Kakao 프론트 Firebase OIDC 설정 안내

<!-- codex-turn:01a0f6ad-a68e-7043-8937-365571ea4c7d -->

- 브랜치: develop.
- 작업 목표: 이전에 안내한 카카오 프론트 플랫폼별 설정과 토큰 교환 계약 재정리.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 저장소 프론트 가이드와 기존 작업 기록 확인. oidc.kakao 공통 provider ID, Firebase SDK 인증 및 Firebase ID Token의 Identity exchange 전달, Android/iOS 각 Firebase 앱 구성과 브라우저 인증 후 앱 복귀 설정·실기기 검증 안내. Kakao Native SDK 직접 토큰 교환과 구분, Client Secret 앱 포함 금지.
- 실행한 테스트와 결과: 계약/이전 기록 읽기, git diff --check 수행. 설명만 수행하여 실행 테스트 미실행.
- 유지한 계약: 카카오 원본 Access Token을 Identity에 전달하지 않음, 공통 provider ID 및 기존 JWT/API 유지, 비밀값 비기록.
- 결정사항: 프론트 프레임워크/SDK 버전 미확인으로 정확한 플랫폼 코드와 callback scheme 값은 단정하지 않음.
- 위험 요소: Chrome 성공은 모바일 앱 복귀 검증이 아님. 실제 Android/iOS 구성은 이번에 직접 조회하지 않음.
- 예상 밖 변경: 없음, 기존 문서 변경 보존. 외부 설정/코드 변경 없음.
- 다음 작업: 프론트 기술 스택에 맞춰 SDK별 설정·로그인·취소·재인증·앱 복귀 체크리스트 구체화.

## 2026-10-02 — 취소·실패 종료 수정 목적 재확인

<!-- codex-turn:01a0fa59-82cf-7150-860b-d13d6caccb2d -->

- 브랜치: develop.
- 작업 목표: 취소/실패 상태 추가와 작업 잠금 해제의 관계 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 안전한 종료가 확인된 연결 작업을 취소/실패 종료로 기록하고 해당 잠금만 해제하여 재시도를 허용하는 계획임을 재확인. 클라이언트 실패 보고나 시간 경과만으로 해제하지 않으며 결과 불명은 보호 유지.
- 실행한 테스트와 결과: 기존 계획 근거 설명 및 git diff --check 통과. 코드 변경 없어 실행 테스트 미실행.
- 유지한 계약: 기존 SNS/전화번호 및 대상 제공자 보호 유지, 비밀값 비기록.
- 결정사항: 상태 enum 추가만이 아니라 조건부 종료 처리와 새로고침 복구까지 포함하는 계획. 구현 미착수.
- 위험 요소: 이전 SDK 실행 종료가 불명인 경우 즉시 재시도를 보장하지 못함.
- 예상 밖 변경: 없음. 기존 기록 보존, 외부 변경 없음.
- 다음 작업: 구현 요청 후 종료 증거와 트랜잭션/API 계약 확정.

## 2026-10-02 — SNS 취소·실패 복구 구현 계획서 정식 보완

<!-- codex-turn:01a0fa77-8925-78f1-9086-0f668e6c3033 -->

- 브랜치: develop.
- 작업 목표: 사용자 요청에 따라 기존 초안을 구현 범위·작업 분할·완료 기준이 있는 수정 계획서로 보완.
- 변경 파일: docs/contracts/provider-link-cancel-recovery-plan.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 범위/제외 항목, 상태 전환표, nullable 데이터 필드 제안, guard의 STARTED 검사와 원자 종료 필요성, API 멱등성, 변경 대상/작업 순서, 완료 체크리스트, 호환 배포 및 중단 기준 추가. 기존 상세 근거/테스트 표 보존.
- 실행한 테스트와 결과: 기존 계획 전체 읽기, ProviderChangeGuard 상태 조회 및 테스트 파일 위치 확인, git diff --check 수행. 문서만 수정하여 Gradle 미실행.
- 유지한 계약: 불명 원격 실행 보호 유지, 상태 조회의 SDK 허가 재부여 금지, owner JWT 기준, 기존 SNS/전화번호 보존, 비밀값 비기록.
- 결정사항: 설계 제안과 실제 구현을 구분. STARTED 종료 증거 부족 시 즉시 재시도 보장하지 않음. 코드/배포/Jira 변경 없음.
- 위험 요소: 실제 앱 저장소 별도, 복구 증거/보존/폴링 정책 확정 필요. 새 enum의 구 서버 호환 확인 필요.
- 예상 밖 변경: 없음. 기존 미커밋 문서 변경 보존.
- 다음 작업: 계획 검토 후 구현 요청 시 계약·운영 복구 권한 확정, 서버/테스트 도구 회귀 테스트부터 구현.

## 2026-10-02 — TMI-136 하위 SNS 취소·복구 이슈 생성안 준비

- 브랜치: develop.
- Jira: TMI-136
- 작업 목표: 사용자 요청에 따른 수정 계획의 하위 Jira 이슈 생성 준비.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 공식 Atlassian MCP로 TMI-136 조회, sns 로그인 에픽 확인. SNS 연결 취소·실패 종료 및 재시도 복구 제목과 계획 기반 범위/완료 조건 초안 준비.
- 실행한 테스트와 결과: Jira 부모 읽기 확인, git diff --check 수행. 코드 변경 없어 실행 테스트 미실행.
- 유지한 계약: Jira 생성 전 내용 제시 및 승인 규칙 유지, 비밀값/개인정보 비기록.
- 결정사항: 이슈 생성/댓글/상태 변경 미실행. 내용 승인 대기.
- 위험 요소: STARTED 불명 결과 자동 해제 제외 및 조건부 운영 복구 범위를 이슈에도 명시해야 함.
- 예상 밖 변경: 없음, 기존 기록 보존.
- 다음 작업: 승인 후 부모 TMI-136 아래 이슈 생성 및 결과 기록. 별도 Jira 댓글은 등록하지 않음.

## 2026-10-02 — Jira 생성안 승인 대기 turn 기록 보완

<!-- codex-turn:01a0fa79-87af-7850-9734-7f823a000bbb -->

- 브랜치: develop.
- Jira: TMI-136
- 작업 목표: 하위 이슈 생성 준비 작업의 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 공식 Atlassian MCP로 부모 에픽 조회 후 제목·범위·완료 조건을 사용자에게 제시. 생성 승인은 대기 중.
- 실행한 테스트와 결과: 부모 이슈 읽기 확인 및 git diff --check 통과. 코드 변경 없어 실행 테스트 미실행.
- 유지한 계약: Jira 변경 전 승인, 비밀값 비기록.
- 결정사항: Jira 생성/댓글/상태 변경 없음. 댓글 목적 및 변경 상태 해당 없음.
- 위험 요소: 승인 전 Jira 변경 금지, 불명 STARTED 자동 해제 제외 유지.
- 다음 작업: 사용자 내용 승인 후 TMI-136 하위 이슈 생성.

## 2026-10-02 — TMI-191 SNS 취소·실패 복구 이슈 생성

- 브랜치: develop (이슈 생성만 수행, 구현 브랜치 변경 없음).
- Jira: TMI-191
- 작업 목표: 승인된 수정 계획을 TMI-136 에픽 하위 작업으로 생성.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md, docs/contracts/provider-link-cancel-recovery-plan.md.
- 구현 내용: 공식 Atlassian MCP로 TMI 작업 유형 확인 후 TMI-191 생성. 제목/범위/보안 경계/완료 조건/계획서 경로 등록, 재조회로 parent=TMI-136 확인.
- 실행한 테스트와 결과: 생성 응답과 부모/상태 재조회 확인, git diff --check 수행. 코드 변경 없어 실행 테스트 미실행.
- 유지한 계약: 사용자 사전 내용 승인에 따라 생성, 비밀값 비기록, 코드/배포/commit/push 변경 없음.
- 결정사항: 생성 승인 있음. 초기 상태 해야 할 일, 별도 상태 전환/댓글 등록 없음. 추가 댓글 목적 해당 없음.
- 위험 요소: 실제 구현 미착수, 종료 증거 및 정책 상세 확정 필요.
- 예상 밖 변경: 없음. 기존 문서 변경 보존.
- 다음 작업: 구현 요청 시 TMI-191 재조회 후 계획과 완료 조건에 맞춰 구현. Jira 댓글 초안: 계획 기반 이슈 생성 및 부모 확인 완료, 코드 변경/실행 테스트 없음, 구현과 실환경 검증 대기(미등록).

## 2026-10-02 — TMI-191 생성 turn 기록 보완

<!-- codex-turn:01a0fa7a-bd43-77f2-8dbd-802099b41c68 -->

- 브랜치: develop.
- Jira: TMI-191
- 작업 목표: 승인된 Jira 생성 작업의 현재 turn 식별자 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md, docs/contracts/provider-link-cancel-recovery-plan.md.
- 구현 내용: 사용자 승인 후 공식 Atlassian MCP로 TMI-191 생성, 부모 TMI-136 및 초기 상태 해야 할 일 재조회 확인. 계획서에 이슈 연결 기록.
- 실행한 테스트와 결과: Jira 생성/부모 조회 확인, git diff --check 통과. 코드 변경 없어 실행 테스트 미실행.
- 유지한 계약: 승인된 생성 범위만 수행, 비밀값 비기록, 코드/배포 변경 없음.
- 결정사항: 생성 승인 있음. 별도 댓글/상태 전환 없음, 추가 댓글 목적 해당 없음.
- 위험 요소: 구현 및 실환경 검증 미착수.
- 예상 밖 변경: 없음. 기존 기록 보존.
- 다음 작업: 구현 요청 시 이슈 재조회 후 범위/완료 조건 기준으로 진행.

## 2026-10-02 — TMI-191 수정 계획 사용자 관점 설명

<!-- codex-turn:01a0fa7e-fd08-78f0-815e-0cdf27555058 -->

- 브랜치: develop.
- Jira: TMI-191
- 작업 목표: SNS 연결 실패·취소 복구 계획을 실제 사용자 사례로 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 진행 중 잠금이 실패 후 남는 현재 문제, 확정 종료 상태/조건부 잠금 해제, 준비 단계 취소와 실행 후 결과 확인 구분, 새로고침 복원, 감사 기록 및 운영 복구 한계를 쉬운 표현으로 설명.
- 실행한 테스트와 결과: 계획서 요약 재확인 및 git diff --check 통과. 설명/기록만 수행하여 Gradle 미실행.
- 유지한 계약: 결과 불명 자동 해제 금지, 기존 회원/SNS/전화번호 보존, 비밀값 비기록.
- 결정사항: 아직 구현 전이며 모든 STARTED 실패의 즉시 자동 재시도는 보장하지 않음.
- 위험 요소: 원격 실행 종료를 확인하지 못한 경우 담당자 복구가 필요할 수 있음.
- 예상 밖 변경: 없음. 기존 기록 보존, 코드/외부/Jira 변경 없음.
- 다음 작업: 구현 요청 시 TMI-191 최신 내용 조회 후 계획 범위와 완료 조건에 따라 진행. Jira 댓글 초안은 설명 완료·구현 미착수·불명 결과 보호 유지(미등록).

## 2026-10-02 — TMI-191 진행 작업 조회 방식 설명

<!-- codex-turn:01a0fa81-fa4b-7df0-ac6f-f6d0855a4a29 -->

- 브랜치: develop.
- Jira: TMI-191
- 작업 목표: 새로고침 후 진행하던 연결 작업을 찾는 방법 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 기존 서버 작업 저장 및 원 요청 ID status 조회, 제안된 최소 메타데이터 보존과 인증된 회원 기준 pending 조회를 구분. 원 ID 해시 조회와 STARTED slot 대조, 여러 PREPARED 후보의 임의 최신 선택 금지, 상태 조회가 SDK 재실행 권한이 아님을 설명.
- 실행한 테스트와 결과: 기존 분석/계획 근거 사용 및 git diff --check 통과. 설명만 수행하여 실행 테스트 미실행.
- 유지한 계약: userId는 인증에서 도출, 다른 사용자 작업 노출 금지, 토큰 원문 복구 메타데이터 저장 금지.
- 결정사항: 로그인 세션 유실 시 재인증 후 본인 작업 조회. pending 및 영속 메타데이터 복원은 아직 계획.
- 위험 요소: 원격 SDK 팝업/실행 자체를 복원하는 기능이 아니며 불명 상태 중복 실행 금지.
- 예상 밖 변경: 없음. 코드/계정/Jira 변경 없음.
- 다음 작업: 구현 요청 시 status/pending 인증·다중 후보·세대 검증 테스트 추가. Jira 댓글은 등록하지 않음.

## 2026-10-02 — TMI-191 SNS 연결 취소·실패 및 작업 복구 구현

<!-- codex-turn:01a0fa83-a6ba-7ef2-aec8-0b8b89fbbb92 -->

- 브랜치: codex/TMI-191-provider-link-recovery (승인된 브랜치 생성).
- Jira: TMI-191. 구현 전 공식 Atlassian MCP로 설명/완료 조건 재조회. Jira 수정/댓글/상태 전환 없음. 댓글 초안은 구현 계약에만 작성(미등록).
- 작업 목표: 실패한 SNS 연결이 불명 상태를 잃지 않도록 취소/실패 의도 기록, 본인 작업 복원, 승인된 안전한 종료 및 재시도 지원.
- 변경 파일(서버): src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/{ProviderChangeProperties,ProviderLinkAttempt,ProviderLinkController,ProviderLinkService}.java, src/main/java/web/tosunsaeng/identity/global/config/IdentityOpenApiExamples.java, src/main/resources/application.yml.
- 변경 파일(테스트/도구): src/test/java/web/tosunsaeng/identity/domain/auth/providerchange/{ProviderChangeTests,ProviderLinkHttpTests}.java, src/test/java/web/tosunsaeng/identity/OpenApiSharingTests.java, scripts/provider_link_recovery.py, scripts/test_provider_link_recovery.py.
- 변경 파일(문서): docs/contracts/provider-link-cancel-recovery-plan.md, docs/contracts/provider-link-recovery-operations.md, docs/contracts/frontend-firebase-auth-integration-guide.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 별도 로컬 테스트 도구 변경: /Users/msde76/tosunsaeng-integration-test/{provider-link.mjs,provider-link.test.mjs,app.js,index.html,server.mjs,server.test.mjs}. /tmp/tmi-191-ui.U6bidJ에서 패치/검증 후 승인된 정확한 파일 복사로 반영. 실행 프로세스 재시작/브라우저 새로고침/실제 로그인 없음.
- 구현 내용: CANCELLED/FAILED enum 및 감사 시각/allowlist 실패 사유 추가. PREPARED 취소는 version 경합 검사로 종료, STARTED 취소/실패 보고는 slot/block 유지 및 ACTION_REQUIRED 응답. 인증된 pending은 실제 활성 slot 우선, 미만료 PREPARED 최대 20개와 hasMore 반환. 조회로 SDK 실행권한 재부여 없음.
- 구현 내용(복구): 공개 관리 API 없이 dry-run 기본 운영 스크립트 제공. 별도 승인 참조/SDK 종료 확인/동일 digest/원격 재조회/정확한 세대·버전·slot 대조 후 snapshot-majority 트랜잭션으로 감사 백업 및 확정 종료. block/floor/epoch 유지, 원문 예외 출력·맹목적 재시도·자동 삭제 금지. 실제 도구 실행 안 함.
- 구현 내용(로컬): sessionStorage에 requestId/attempt/target만 저장, 취소/실패 보고/작업 복원 및 다중 후보 선택 추가. SDK 대기 중 취소와 지연 응답 권한 부활 방지. 새로고침으로 잃은 SDK 실행을 재개하지 않으며 완료 불명은 운영 확인으로 안내.
- 테스트: 최종 ./gradlew clean test 성공 — 1,030 tests, 실패/오류/skip 0. 중간 전체 실행의 OpenAPI 경로 개수/신규 응답 예시 실패는 경로 기대값 28/27 및 실제 예시 추가로 수정 후 targeted/full 재검증. Python unittest 6개 통과. 승인된 실제 로컬 도구 전체 node 테스트 35개 통과. git diff --check 통과.
- 유지한 계약: UUID 주체/RS256/JWKS/audience 및 정상 SNS 연결·전화번호·기존 회원 보존. 토큰/비밀번호 비기록. Learning Core 도메인 코드를 Identity에 추가하지 않음. 사용자 제출 실패/팝업 닫힘/시간 경과만으로 잠금 해제 금지.
- 결정사항: 신규 FIREBASE_PROVIDER_LINK_RECOVERY_ENABLED 기본 false. 새 enum 호환 전체 서버 배포 후 별도 활성화. STARTED의 모든 실패를 자동 즉시 재시도하도록 만들지 않음. commit/push/배포/외부 계정 변경 없음.
- 위험 요소: 실제 Mongo rollback/경합·Firebase 원격 복구·모바일 미검증, 원격 SDK 종료는 운영자 증거 필요. 도구는 tenant 없음/oidc.kakao만 지원. 감사 collection 보존 정책 및 접근 제한, pending 대규모 인덱스/explain 확인 필요. 로컬 gateway 재시작 전 신규 경로 미적용.
- 예상 밖 변경: 없음. 작업 전 존재하던 WORKLOG/CURRENT_STATE/계획서 변경을 보존하고 이번 기록만 추가·갱신. 별도 로컬 프로젝트 수정은 요청 범위와 승인된 경로에 한정.
- 다음 작업: 사용자가 commit/push한 뒤 호환 배포/플래그 활성화 별도 승인, gateway 재시작 및 실제 취소/복원/승인 복구 smoke. 프론트에는 구현·운영 계약을 전달하고 실제 계정 복구는 개별 승인 후 수행.

## 2026-10-02 — Guest merge 대상 탈퇴·정지 오류 분리 구현

<!-- codex-turn:01a0fb71-09af-7732-b79f-40fcaec7ffea -->

- 브랜치: develop(작업 시작 시 확인). 이번 요청에 지정된 Jira 키 없음. Jira 조회/수정/댓글/상태 변경 미수행.
- 작업 목표: 승인된 최소 수정안에 따라 대상 탈퇴/정지와 원본 상태 충돌을 프론트가 구분하도록 구현.
- 변경 파일: AuthErrorStatus.java, FirebaseGuestMergeTargetResolver.java, FirebaseGuestMergeTransactionService.java, FirebaseExchangeController.java, IdentityOpenApiExamples.java, FirebaseGuestMergeTargetResolverTests.java, FirebaseGuestMergeTransactionServiceTests.java, FirebaseExchangeControllerTests.java, OpenApiSharingTests.java, docs/contracts/frontend-firebase-auth-integration-guide.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: target WITHDRAWN을 403 GUEST_MERGE_TARGET_WITHDRAWN, SUSPENDED를 403 GUEST_MERGE_TARGET_NOT_ACTIVE로 두 검사 단계에서 분리. 소유권/구조 모순은 기존 오류 유지. source 조건부 갱신 실패는 기존 GUEST_MERGE_CONFLICT. HTTP 오류 payload 및 비밀정보 비노출, target 상태 변경 시 후속 source/session/outbox 저장 중단, 기존 cleanup gate 우선순위, Swagger 예시 회귀 테스트 추가.
- 실행한 테스트와 결과: 최초 sandbox 실행은 Gradle 캐시 접근 제한으로 실패하여 승인된 확장 권한으로 재실행. 첫 전체 clean test에서 신규 Guest 테스트 fixture의 installation hash 형식 오류 1건 발생, 테스트 값을 유효한 가짜 hash로 수정 후 HTTP/OpenAPI 회귀 추가. 최종 ./gradlew clean test BUILD SUCCESSFUL(1,045 tests, failures/errors/skipped 0). git diff --check 통과.
- 유지한 계약: UUID JWT sub 기반 source/Firebase proof 기반 target, JWT/요청/성공 응답 형식 유지. withdrawal cleanup gate 및 세션 fence/원자 트랜잭션/CAS 유지. 새 환경변수/DB 엔티티/외부 API 없음, Learning Core 코드/데이터 변경 없음.
- 변경한 외부 계약: Guest merge에서 기존 409 충돌로 묶이던 확인된 target 탈퇴·정지 상태에 신규 403 코드 추가. 기존 WITHDRAWAL_CLEANUP_PENDING/FIREBASE_IDENTITY_CONFLICT가 선행할 수 있음. 중복 처리 중/완료 코드나 멱등 재전달 계약은 추가하지 않음.
- 결정사항: 메시지뿐 아니라 code 분리, 프론트는 새 target 오류에서 자동 재시도 중단/상태 안내. 기존 충돌은 상태 재확인 후 분기하며 처리 중/완료로 단정하지 않음. 기록과 가이드에 이 구분 명시.
- 위험 요소: 실제 Mongo 동시성/rollback 및 모바일 E2E 미검증. 모든 탈퇴가 새 코드로 반환되는 것은 아니며 선행 Firebase proof/cleanup/session 오류는 그대로 가능.
- 배포 전 확인: 프론트 새 403 코드 처리 및 기존 cleanup 오류 대응 반영, 서버 전체 인스턴스 호환 배포 후 source/target 상태 변화 QA. 환경변수 추가 없음. 이번 배포/commit/push 미수행.
- 예상 밖 변경: 이번 범위 밖 변경 없음. 시작 전 미커밋 WORKLOG 누적 변경 및 docs/contracts/guest-app-update-transition-review.md는 사용자/다른 작업 변경으로 보존했고 수정하지 않음.
- Jira 댓글 초안(미등록): merge 대상 탈퇴/정지 오류 코드 분리, 2단계 검사 및 cleanup/CAS 계약 유지. 관련 소스/Swagger/프론트 가이드/회귀 테스트 갱신, 전체 1,045 tests 통과. 실DB/모바일 QA 미검증.
- 다음 작업: 사용자 diff 검토/직접 commit·push, 프론트 분기 반영과 배포 후 실제 병합/대상 탈퇴·정지/중복 요청 QA. 중복 PROCESSING 계약은 별도 범위로 설계.

## 2026-10-01 — CI/CD 완료 후 테스트 Identity Kakao 활성화 재배포

<!-- codex-turn:01a0f634-af94-7f81-a4c5-bef57d5d7cc1 -->

- 브랜치: develop.
- 작업 목표: 사용자가 push한 CI/CD 완료 확인 후 승인된 테스트 Kakao 설정 적용·재배포.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 애플리케이션/워크플로 코드 변경 없음.
- 구현 내용: GitHub run 36826056025의 테스트·이미지 빌드·ECS 배포·헬스 검증 성공 확인. commit 1cca3c17c3460e01a7ed75b82f4c22ed6fabede0, CI 생성 revision tosunsaeng-identity-test:9 확인. 기존 revision 8과 9의 Kakao 및 session fence OFF 확인.
- 외부 변경: 사용자에게 세 설정의 범위와 효과를 제시하고 '세 설정 적용 승인' 수신 후 revision 9를 기반으로 revision 10 생성. FIREBASE_KAKAO_ENABLED=true, FIREBASE_KAKAO_PROVIDER_ID=oidc.kakao, AUTH_SESSION_FENCE_ENABLED=true. 동일 commit 이미지와 나머지 설정 유지. tosunsaeng-staging-cluster의 tosunsaeng-identity-test-service만 revision 10으로 업데이트.
- 실행한 테스트와 결과: gh run watch 및 run view로 CI success 확인. ECS revision 10 저장된 세 환경값 확인, 최종 배포 성공·1 running/0 pending·0/3 task failures 확인. 배포 후 HTTPS /actuator/health 응답 UP. git diff --check 통과. 애플리케이션 변경 없어 로컬 Gradle 재실행 없음; 이전 1024개 로컬 테스트 및 이번 CI 테스트 성공과 구분.
- 유지한 계약: JWT/API/키/DB/이벤트 목적지 변경 없음. 운영 서비스 미변경. 토큰·비밀값 조회 및 기록 없음. Google/Apple 허용, 재발급 복구 및 다른 기능 설정 유지.
- 결정사항: CI/CD 종료를 기다려 설정 덮어쓰기 방지. 현 워크플로는 서비스의 현재 task definition을 가져와 이미지와 SENTRY_RELEASE를 바꾸므로 다음 배포도 해당 환경 설정을 승계. 워크플로 자체는 수정하지 않음.
- 위험 요소: 실 카카오 exchange·신규 가입·기존 회원 최초 연결 및 Android/iOS E2E는 별도 검증 필요. session fence ON으로 세션 보호 동작 활성화; auth-methods/sync를 사용하는 경우 별도 provider fence 계약 확인 필요. 새 태스크 실패 시 이전 revision 9가 설정 롤백 기준이나 이번 자동 롤백은 수행하지 않음.
- 예상 밖 변경: 없음. 사용자 push 완료로 시작 시 작업 트리 깨끗함 확인. 이번 commit/push/Jira 변경 없음. AWS CLI 자격증명이 없어 로그인된 Chrome 콘솔로 배포 수행, 자격증명 복사 없음.
- 다음 작업: Chrome Kakao 재인증 후 Identity exchange 재시도, MEMBER/가입 준비 응답 확인. 이후 가입·재발급·기존 회원 연결 및 Android/iOS 검증.

## 2026-10-01 — 로컬 통합 테스트 서버 재시작

- 브랜치: develop.
- 작업 목표: localhost 테스트 화면 접속 복구.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 코드 변경 없음.
- 구현 내용: 4173 포트 리스너 없음 확인 후 기존 통합 테스트 도구의 node server.mjs 재실행.
- 실행한 테스트와 결과: 시작 메시지 및 localhost:4173 HTTP 200 확인, git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: AWS 배포/인증/회원 데이터 변경 없음. 비밀값 비기록.
- 결정사항: 로컬 서버 종료가 접속 장애 원인이었으며 재시작으로 화면 응답 복구.
- 위험 요소: 프로세스의 이전 종료 원인은 미확인. 브라우저 새로고침 시 메모리 세션 소실 가능, 직접 새로고침 미수행.
- 다음 작업: Chrome에서 로컬 화면 접속 후 Kakao 테스트 재개. 사용자가 터미널에서 node server.mjs를 직접 실행해 유지할 수도 있음.

## 2026-10-01 — 로컬 서버 복구 turn 기록 보완

<!-- codex-turn:01a0f641-fed8-7fc2-a684-1ec67ac33d82 -->

- 브랜치: develop.
- 작업 목표: 로컬 테스트 서버 복구 결과에 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 4173 포트 리스너 부재 확인 후 기존 node server.mjs 재실행, 코드 수정 없음.
- 실행한 테스트와 결과: localhost:4173 HTTP 200 및 git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: AWS/회원 데이터/API 변경 없음, 비밀값 비기록.
- 결정사항: 로컬 화면 응답 복구 완료.
- 위험 요소: 이전 프로세스 종료 원인 미확인, 실제 Kakao 로그인은 별도 검증 필요.
- 다음 작업: Chrome 로컬 화면에서 Kakao 테스트 재개.

## 2026-10-01 — 전화번호 테스트 CAPTCHA 오류 진단

- 브랜치: develop.
- 작업 목표: send의 auth/captcha-check-failed 원인 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 로컬 app.js에서 appVerificationDisabledForTesting=true 및 send의 Firebase PhoneAuthProvider 직접 호출 확인. Identity와 분리된 Firebase 인증 오류이며 콘솔 등록 가상 번호 전용 도구임을 안내.
- 실행한 테스트와 결과: 코드 읽기 및 git diff --check 통과. 코드 변경 없어 테스트 재실행 없음.
- 유지한 계약: 실제 SMS/외부 인증 요청/설정 변경 없음. 전화번호·코드·토큰 비기록.
- 결정사항: 입력 번호가 동일 Firebase 프로젝트에 등록된 테스트 번호인지 국제 형식으로 비교하도록 안내. 미등록 번호와 mock CAPTCHA 조합이 유력한 원인이나 입력값/콘솔 미확인으로 확정하지 않음.
- 위험 요소: 실제 입력 및 Firebase 테스트 번호 설정, 허용 도메인과 브라우저 상태 미검증.
- 다음 작업: 사용자 테스트 번호 등록 여부 확인 후 필요 시 허용 도메인/브라우저 CAPTCHA 상태 점검.

## 2026-10-01 — CAPTCHA 진단 turn 기록 보완

<!-- codex-turn:01a0f644-9b1a-7e01-9609-04c8fe75ce44 -->

- 브랜치: develop.
- 작업 목표: 현재 CAPTCHA 진단 작업의 식별자 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 로컬 테스트 도구의 가상 전화번호 인증 모드 확인. Firebase 테스트 번호 등록 여부 확인을 사용자에게 요청. 코드 변경 없음.
- 실행한 테스트와 결과: 소스 조회 및 git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 외부 설정·인증 요청 변경 없음, 비밀값 및 전화번호 비기록.
- 결정사항: 미등록 번호와 테스트 CAPTCHA 조합 가능성을 설명하되 확정 원인으로 단정하지 않음.
- 위험 요소: 실제 입력과 Firebase 콘솔 설정 미확인.
- 다음 작업: 사용자 등록 여부 확인 후 추가 진단.

## 2026-10-01 — 전화번호 연결 계정 충돌 오류 안내

- 브랜치: develop.
- 작업 목표: link의 auth/account-exists-with-different-credential 해석.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 로컬 link가 PhoneAuthProvider credential을 현재 Firebase user에 linkWithCredential로 연결하며 Identity signup 이전임을 확인. Firebase 계정/인증수단 충돌로 구분하고 정확한 충돌 대상은 미확정으로 안내.
- 실행한 테스트와 결과: 소스 조회 및 git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 기존 계정 삭제·연결 해제·UID 변경·이메일 자동 병합 없음. 개인정보/토큰 비기록.
- 결정사항: 전화번호 다른 UID 소유는 통상 credential-already-in-use이므로 현재 오류만으로 전화번호 중복을 단정하지 않음. 기존 Google/Apple 테스트 번호 재사용 여부 확인 요청.
- 위험 요소: Firebase 사용자 연결 및 원격 오류 상세 미확인.
- 다음 작업: 신규 가입 테스트인지 기존 회원 연결 테스트인지 구분하고 계정 연결 상태 확인. 임의 삭제/우회 금지.

## 2026-10-01 — Firebase 연결 충돌 진단 기록 보완

<!-- codex-turn:01a0f646-138a-75c1-9054-308451961e49 -->

- 브랜치: develop.
- 작업 목표: 현재 turn의 계정 연결 충돌 진단 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Firebase linkWithCredential 단계 오류를 설명하고 기존 테스트 번호 재사용 여부 확인 요청. 코드 변경 없음.
- 실행한 테스트와 결과: 소스 조회 및 git diff --check 통과. 문서 변경만 수행하여 Gradle 미실행.
- 유지한 계약: 계정 삭제·연결 해제·자동 병합·외부 설정 변경 없음. 비밀값 비기록.
- 결정사항: 오류만으로 전화번호 중복을 단정하지 않고 Firebase 연결 상태 확인 필요.
- 위험 요소: 실제 충돌 대상과 원격 계정 상태 미확인.
- 다음 작업: 사용자 답변 후 신규 가입/기존 회원 연결 목적을 구분해 진단.

## 2026-10-01 — 기존 연결 전화번호 재사용 확인

<!-- codex-turn:01a0f646-ffd2-7f31-b935-c5d3992bfaf1 -->

- 브랜치: develop.
- 작업 목표: 다른 계정에 연결된 테스트 번호라는 사용자 확인에 따른 흐름 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 신규 Kakao 가입에는 다른 미사용 등록 테스트 번호, 기존 회원에 Kakao 추가는 기존 회원 인증 후 명시 연결 흐름이 필요함을 구분. 코드/계정 변경 없음.
- 실행한 테스트와 결과: git diff --check 통과. 안내만 수행하여 Gradle 미실행.
- 유지한 계약: 전화번호 소유권 강제 이전·다른 Firebase UID 자동 병합·기존 연결 삭제 없음. 개인정보/비밀값 비기록.
- 결정사항: 번호 재사용은 사용자 보고로 확인됨. 실제 Firebase UID 및 정확한 오류 매핑은 직접 확인하지 않음. 같은 UID의 최초 제공자 자동 등록과 다른 UID 간 연결 충돌은 별개임을 설명.
- 위험 요소: 이미 다른 UID에 연결된 Kakao는 단순 추가 연결도 충돌할 수 있어 상태 확인 필요. 테스트 화면의 일반 로그인 버튼을 명시 연결 기능으로 간주하지 않음.
- 다음 작업: 사용자 목적을 신규 가입 검증 또는 기존 회원 로그인 수단 추가로 확정 후 해당 흐름 진행.

## 2026-10-01 — 기존 연결 번호 재사용 안내 기록 보완

<!-- codex-turn:01a0f646-ffd2-75f2-a1e2-a70a9da0ac47 -->

- 브랜치: develop.
- 작업 목표: 현재 turn의 정확한 식별자로 안내 결과 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 기존 연결 번호 재사용에 대해 신규 가입과 기존 회원의 Kakao 연결 흐름을 구분해 안내. 앞선 항목의 식별자는 잘못 기재되었으며 이 항목으로 보완. 과거 기록 수정 없음.
- 실행한 테스트와 결과: git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 계정 삭제·연결 해제·자동 병합 및 외부 설정 변경 없음. 비밀값 비기록.
- 결정사항: 같은 Firebase UID의 자동 등록과 다른 UID 간 충돌을 구분.
- 위험 요소: 실제 Firebase 연결 상태와 사용자의 테스트 목적 미확정.
- 다음 작업: 신규 가입 또는 기존 회원 연결 중 사용자 목적 확인 후 진행.

## 2026-10-01 — Kakao 기존 회원에 새 Google 로그인 시 충돌 구분

<!-- codex-turn:01a0f64c-f5f9-78a1-ae3f-a6f7dadb9f55 -->

- 브랜치: develop.
- 작업 목표: 동일 전화번호를 사용하는 새 Google 로그인 충돌 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 로컬 Google 버튼이 signInWithPopup 또는 동일 제공자 재인증이며 명시적 Google 추가 연결 기능이 아님을 재확인. 동일 전화번호로 별도 Firebase 계정을 자동 통합하지 않음을 안내.
- 실행한 테스트와 결과: 소스 조회 및 git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 같은 UID의 최초 제공자 등록만 허용, 전화번호/이메일 기반 자동 병합 없음. 비밀값 및 개인정보 비기록.
- 결정사항: Google 팝업 단계 오류는 기존 이메일/다른 제공자 충돌 가능성, 전화 연결 단계는 기존 계정 소유 충돌 가능성으로 구분하며 실제 실패 버튼 확인 요청. 현재 오류를 전화번호 충돌로 확정하지 않음.
- 위험 요소: 원격 Firebase UID/이메일 및 오류 발생 단계 미확인. 이미 다른 UID 소유인 Google은 명시 연결도 바로 성공한다고 보장하지 않음.
- 다음 작업: 실패 항목 google/link 확인 후 기존 Kakao MEMBER에 Google 추가 연결 흐름과 충돌 상태 점검.

## 2026-10-01 — link 단계 전화번호 연결 충돌 확인

<!-- codex-turn:01a0f64e-9492-7852-8098-94054e2b7575 -->

- 브랜치: develop.
- 작업 목표: 사용자 확인으로 실패 단계가 전화번호 연결임을 구분.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: link가 현재 Firebase 사용자에 PhoneAuthProvider 인증정보를 연결하는 호출임을 재확인. 기존 Kakao 회원의 Google 추가 연결과 신규 Google 가입의 전화번호 연결은 다른 흐름임을 안내.
- 실행한 테스트와 결과: 코드 조회 및 git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 기존 계정/전화번호 연결 삭제 및 다른 UID 자동 병합 없음. 개인정보·비밀값 비기록.
- 결정사항: 동일 번호가 다른 계정에 연결됐다는 사용자 보고와 link 실패는 확인됨. 정확한 Firebase UID 및 오류 매핑은 미확정. 기존 회원 인증 후 명시적 Google 연결이 필요한 흐름이며 테스트 도구의 일반 로그인으로 대체하지 않음.
- 위험 요소: Google 인증정보가 이미 다른 UID 소유이면 명시 연결 전 별도 상태 확인 필요. 전화번호 충돌 우회로 자동 이전하면 안 됨.
- 다음 작업: Firebase 연결 상태를 읽기 확인하고 사용자 요청 시 테스트 화면에 기존 회원의 로그인 수단 추가 절차 구현.

## 2026-10-01 — 전화번호 연결 충돌 안내 기록 보완

<!-- codex-turn:01a0f64e-9492-7230-b634-04162f451eaf -->

- 브랜치: develop.
- 작업 목표: 현재 turn의 정확한 식별자로 작업 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: link 단계의 전화번호 연결 실패와 기존 회원의 Google 추가 연결 흐름을 구분해 안내. 앞선 항목의 식별자 오기를 이 항목으로 보완하며 과거 기록은 보존.
- 실행한 테스트와 결과: git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 계정 삭제·연결 해제·자동 병합·외부 설정 변경 없음. 비밀값 비기록.
- 결정사항: 기존 회원의 명시 연결 절차와 신규 가입 전화번호 연결은 별개.
- 위험 요소: 실제 Firebase UID 및 Google 인증정보 소유 상태 미확인.
- 다음 작업: 연결 상태 확인 후 사용자 요청에 따라 테스트 도구의 로그인 수단 추가 기능 준비.

## 2026-10-01 — 신규 가입과 기존 회원 SNS 추가 연결 구분

- 브랜치: develop.
- 작업 목표: 기존 전화번호 재사용 및 SNS 연결 흐름 이해 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 다른 Firebase 계정에 연결된 전화번호를 새 UID에 중복 연결하지 않으며 기존 회원 인증 후 새 SNS를 연결하는 흐름을 안내. 이미 같은 UID인 경우 자동 등록 예외와 다른 회원 소유 SNS의 제한도 구분.
- 실행한 테스트와 결과: git diff --check 통과. 설명만 수행하여 Gradle 미실행.
- 유지한 계약: 전화번호 기반 자동 병합·소유권 이전 없음. 비밀값 비기록.
- 결정사항: 기존 회원 로그인 및 필요 시 최근 재인증 후 SNS 연결, 전화번호 재연결 불필요.
- 위험 요소: 로그인 상태만으로 모든 SNS 연결이 허용되는 것은 아니며 기존 소유권 충돌은 별도 확인 필요.
- 다음 작업: 사용자 요청 시 명시적 SNS 추가 연결 테스트 화면 준비.

## 2026-10-01 — SNS 추가 연결 안내 기록 보완

<!-- codex-turn:01a0f650-2a2c-7e12-9ca9-6aebb66d8895 -->

- 브랜치: develop.
- 작업 목표: 신규 가입과 기존 회원 SNS 연결 설명의 현재 turn 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 기존 회원 인증 후 새 SNS를 연결하고 전화번호를 유지하는 흐름 설명. 코드 변경 없음.
- 실행한 테스트와 결과: git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 다른 UID 자동 병합·전화번호 소유권 이전·외부 설정 변경 없음. 비밀값 비기록.
- 결정사항: 연결 완료 후 여러 SNS로 같은 회원 로그인 가능하나 다른 회원 소유 SNS는 무조건 연결하지 않음.
- 위험 요소: 실제 Firebase 소유권과 연결 상태는 별도 확인 필요.
- 다음 작업: 사용자 요청 시 명시적 SNS 연결 테스트 기능 준비.

## 2026-10-01 — 전화번호를 통한 기존 회원 복구 방향 검토

<!-- codex-turn:01a0f651-54ef-7e11-a255-693c88b4a800 -->

- 브랜치: develop.
- 작업 목표: 기존 SNS를 잃은 사용자의 새 SNS 및 전화번호 기반 복구 제안 검토.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 설명/설계 검토만 수행. 전화번호 입력 또는 SMS 단독 소유 증명을 기존 계정 소유권과 동일시하지 않도록 안내. 기존 로그인 수단 없는 경우 별도 계정 복구 흐름 필요.
- 실행한 테스트와 결과: git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 자동 UID 이전·회원 병합·전화번호 연결 해제 없음. 개인정보/비밀값 비기록.
- 결정사항: 새 SNS 인증 후 기존 계정 복구 선택, 실 SMS 인증 및 위험 기반 추가 확인, 명시 동의와 서버 검증을 거쳐 기존 userId 보존 방식 권장. 정상 로그인 경로와 분리하며 Firebase 충돌을 클라이언트 강제 해제로 우회하지 않음.
- 위험 요소: 번호 재사용·SIM 탈취·공유 번호로 타인 계정 탈취 가능. 같은 기기 여부는 신뢰 증거가 아니며 기존 SNS 접근 불가 시 복구 수단 및 대기/수동 검토 정책 결정 필요. 실제 기술 설계/구현 미수행.
- 다음 작업: 사용자 승인 시 복구 증거·추가 검증·알림/세션 폐기·Firebase 소유권 변경 원자성 및 복구 정책 상세 설계.

## 2026-10-01 — 일반 로그인과 계정 복구 경계 확인

<!-- codex-turn:01a0f653-2a83-76d2-bb18-2e739fd15687 -->

- 브랜치: develop.
- 작업 목표: 충돌 차단 유지와 별도 계정 복구 제안의 의미 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 정상 연결된 SNS 로그인은 유지하고, 다른 Firebase 계정의 기존 전화번호를 이용한 자동 연결만 차단하며 별도 복구 절차를 제공하는 방향 설명. 코드 변경 없음.
- 실행한 테스트와 결과: git diff --check 통과. 설명만 수행하여 Gradle 미실행.
- 유지한 계약: 기존 인증 계약 및 소유권 충돌 차단 유지, 계정/외부 설정 변경 없음. 비밀값 비기록.
- 결정사항: 충돌 안내에서 기존 로그인 또는 계정 복구를 선택하게 하고 복구는 추가 본인 확인 후 처리하는 방향 제안.
- 위험 요소: 복구 정책과 구현은 미확정이며 아직 제공되는 기능이 아님.
- 다음 작업: 사용자 요청 시 계정 복구 상세 정책 및 구현 계획 수립.

## 2026-10-01 — 계정 복구 경계 설명 기록 보완

<!-- codex-turn:01a0f653-6f6c-7a80-a239-fe957be526a5 -->

- 브랜치: develop.
- 작업 목표: 현재 turn의 정확한 식별자를 포함한 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 정상 SNS 로그인은 유지하고 계정 소유권 충돌은 차단하며 별도 복구 절차를 제안한 설명 기록. 앞선 식별자 오기를 이 항목으로 보완하고 과거 기록은 보존.
- 실행한 테스트와 결과: git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 인증 및 충돌 차단 계약 유지, 계정·외부 설정 변경 없음. 비밀값 비기록.
- 결정사항: 복구는 추가 본인 확인을 거치는 별도 기능이며 아직 구현하지 않음.
- 위험 요소: 복구 정책과 구현 범위 미확정.
- 다음 작업: 사용자 요청 시 복구 정책 및 구현 계획 수립.

## 2026-10-01 — 기존 회원 SNS 추가 연결 테스트 사전 확인

<!-- codex-turn:01a0f654-67f9-7301-b75a-649b922135fd -->

- 브랜치: develop.
- 작업 목표: 현재 회원의 다른 SNS 추가 연결 테스트 준비.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 로컬 UI 및 프록시에 providers/link 경로가 없는 상태 확인. 서버 계약의 prepare → start → SDK link → 대상 재인증 → complete 확인. 추가할 SNS를 사용자에게 질문. 구현/외부 설정 변경 없음.
- 실행한 테스트와 결과: 코드/계약 조회 및 git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 기존 전화번호·회원 기록 유지, 서버 승인 없이 Firebase link 실행하지 않음. 비밀값 비기록.
- 결정사항: Firebase 로그인만이 아니라 Identity MEMBER 인증이 필요. 기존 카카오 일반 로그인 허용과 명시 연결 기능은 별도이며 FIREBASE_PROVIDER_LINK_ENABLED 및 provider fence 배포 설정 확인 필요.
- 위험 요소: 연결 대상 SNS, 현재 MEMBER 인증 여부, 실제 배포 연결 플래그 미확인. 다른 UID에 이미 등록된 대상은 연결 충돌 가능.
- 다음 작업: 사용자 대상 SNS 확인 후 MEMBER 로그인 상태 및 테스트 서버 연결 설정 점검, 로컬 도구에 계약에 맞는 명시 연결 UI 준비. 실제 연결 변경은 대상 확인·승인 및 사용자 직접 SNS 인증 후 수행.

## 2026-10-01 — 프론트의 Firebase 인증 후 Identity 교환 흐름 설명

<!-- codex-turn:01a0f657-2995-7bc0-9338-a7335fcd3889 -->

- 브랜치: develop.
- 작업 목표: SNS 인증 후 Identity 로그인 요청 주체 설명 및 Kakao 준비 완료 보고 반영.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 로컬 exchange 버튼의 Firebase ID Token 취득 및 Identity 교환 호출 확인. 실제 앱에서는 프론트가 인증 성공 후 연속 호출하며 사용자에게 별도 로그인을 요구하는 것이 아님을 설명.
- 실행한 테스트와 결과: 코드 조회 및 git diff --check 통과. 설명만 수행하여 Gradle 미실행.
- 유지한 계약: Firebase ID Token은 Identity 검증용, 서비스 요청에는 Identity 토큰 사용. 신규 회원은 ENROLLMENT_REQUIRED 이후 가입 절차 유지. 비밀값 비기록.
- 결정사항: 사용자의 Kakao 계정 준비 완료 보고 수신. 실제 화면/서버의 MEMBER 상태는 이번에 재검증하지 않음. 기존 회원에 다른 SNS 추가 연결 테스트 맥락 유지.
- 위험 요소: 앱 프론트의 자동 교환 구현 여부는 별도 확인 필요, 로컬 도구는 단계별 수동 실행용.
- 다음 작업: 현재 Kakao 회원을 기준으로 대상 SNS 확인 및 명시 연결 테스트 도구/서버 설정 준비.

## 2026-10-01 — 프론트 인증 교환 설명 기록 보완

<!-- codex-turn:01a0f657-2995-78b1-bc70-b7cdc7b8b635 -->

- 브랜치: develop.
- 작업 목표: 현재 turn의 정확한 식별자로 작업 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 프론트의 Firebase 인증 후 Identity 교환 흐름 설명 및 사용자 Kakao 준비 완료 보고 반영. 앞선 식별자 오기를 이 항목으로 보완하며 과거 기록은 보존.
- 실행한 테스트와 결과: git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 인증 계약 유지, 비밀값 비기록, 외부 설정·계정 변경 없음.
- 결정사항: 로컬 테스트 버튼 분리는 단계별 검증용이며 실제 앱은 인증 후 교환 요청을 이어서 수행하는 구조.
- 위험 요소: 실제 프론트 구현 및 현재 MEMBER 상태는 별도 검증 필요.
- 다음 작업: 기존 Kakao 회원에 다른 SNS를 추가하는 테스트 준비.

## 2026-10-01 — 기존 MEMBER SNS 연결 로컬 테스트 화면 구현

<!-- codex-turn:01a0f658-3a2b-7923-838b-608da5a6d39b -->

- 브랜치: develop.
- 작업 목표: 기존 Kakao MEMBER를 유지하면서 Google·Apple 등 SNS 추가 연결을 단계별 검증하는 로컬 화면 제공.
- 변경 파일: 외부 로컬 도구의 app.js, index.html, server.mjs, README.md, app.test.mjs, apple.test.mjs, merge.test.mjs, server.test.mjs 수정; provider-link.mjs, provider-link.test.mjs, provider-link-ui.test.mjs 추가. 저장소는 docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md만 변경.
- 구현 내용: MEMBER·사용자 동의·기존 SNS 재인증 후 prepare/start, 최초 linkAllowed 허용에만 SDK 연결 1회, 대상 SNS 재인증 후 complete, 원래 requestId 기반 status. 동일 UID·전화번호·기존 SNS 보존 검증, 중복 클릭 방지, 응답 유실별 안전한 재시도 제한, 진행 중 로컬 초기화 방지. 프록시는 기존 테스트 Identity의 네 연결 경로만 추가 허용.
- 실행한 테스트와 결과: 실 도구 디렉터리에서 Node 테스트 28개 통과(실제 OAuth/Identity 호출 없이 Mock). 4173 서버 재시작 및 화면·모듈 HTTP 정상 응답 확인. Chrome 새 탭 자동 열기는 ERR_BLOCKED_BY_CLIENT로 시각 검증 미완료. Java 변경이 없어 Gradle clean test 미실행.
- 유지한 계약: Identity API·JWT 계약 변경 없음, 전화번호 이전·자동 병합·해제 없음, 토큰 원문 표시/로그/영구 저장 없음. Learning Core 도메인 코드 추가 없음.
- 결정사항: 테스트 화면만 구현하고 실제 SNS 인증/연결은 사용자가 수행. AWS 설정·배포·Jira·commit·push 없음. 기존 로그인 탭 새로고침하지 않음.
- 위험 요소: 명시 연결 플래그 FIREBASE_PROVIDER_LINK_ENABLED 및 FIREBASE_PROVIDER_CHANGE_FENCE_ENABLED의 실배포 활성 여부 미확인. 서버 Kakao 로그인 허용과 별개. 브라우저 화면 실인증 및 모바일 검증 별도 필요. 진행 상태는 메모리에만 존재.
- 예상 밖 변경: 없음. 기존 사용자 변경 문서 보존 후 기록 추가, Identity 비즈니스 코드는 변경하지 않음.
- 다음 작업: 기존 Kakao로 Firebase 및 Identity MEMBER 로그인 후 새 2-2 섹션 사용. 실연동 전 서버 명시 연결 설정 확인, 승인 없이 설정 활성화하지 않음.

## 2026-10-01 — 기존 SNS 재인증 필수 여부 분석

- 브랜치: develop.
- 작업 목표: SNS 추가 연결 시 매번 기존 SNS 팝업 인증이 필요한지 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없이 ProviderLinkService.remainingProof와 ProviderChangeService.verify/recent 및 ProviderChangeProperties 확인. 서버는 기존 승인 SNS proof와 최근 auth_time을 검증하며 기본 제한은 5분. 로컬 테스트 도구의 별도 재인증 강제와 서버 요구를 구분.
- 실행한 테스트와 결과: 코드 읽기 및 git diff --check. 분석/문서만 변경하여 테스트 미실행.
- 유지한 계약: 기존 SNS 소유 증명 유지, 토큰 강제 갱신만으로 auth_time 갱신되지 않음, 대상 SNS의 start 이후 인증 요구 유지.
- 결정사항: 최근 기존 SNS 로그인 증명 재사용 UX는 가능하나 이번에는 구현하지 않음.
- 위험 요소: 실제 배포 recent-auth 제한 및 클라이언트 상태에 따라 재인증 필요. Identity 세션만으로 대체 불가.
- 다음 작업: 요청 시 로컬 화면에서 유효한 최근 기존 SNS 로그인 재사용 구현 및 테스트.

## 2026-10-01 — 재인증 요구 분석 turn 기록 보완

<!-- codex-turn:01a0f667-94ef-7400-91cf-3e1bc5af5315 -->

- 브랜치: develop.
- 작업 목표: 기존 SNS 재인증 필수 여부 분석의 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 서버는 기존 승인 SNS의 최근 인증(기본 5분)을 요구하고, 별도 팝업을 매번 요구하는 것은 로컬 도구 UX임을 설명. 최근 로그인 재사용은 미구현.
- 실행한 테스트와 결과: git diff --check 통과. 분석/문서만 변경하여 실행 테스트 생략.
- 유지한 계약: 기존 SNS 소유 증명 및 대상 SNS의 start 이후 인증 유지. 비밀값 비기록.
- 결정사항: 코드·계정·외부 설정 변경 없음.
- 위험 요소: 배포 설정과 인증 시각에 따라 재인증 필요.
- 다음 작업: 요청 시 최근 로그인 재사용 UX 구현 및 테스트.

## 2026-10-01 — SNS 추가 연결 인증 UX 설명

- 브랜치: develop.
- 작업 목표: 로그인 상태에서 기존 SNS 재인증으로 인한 사용자 불편과 보안 경계 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 최근 기존 SNS 인증은 재사용하고 오래된 세션만 재인증하는 UX 권장. Identity 세션 유지와 최근 소유 증명을 구분하고, 새 SNS 인증만으로 기존 회원 소유가 입증되지 않는 이유 설명.
- 실행한 테스트와 결과: git diff --check. 설명/문서만 변경하여 실행 테스트 미수행.
- 유지한 계약: 현재 기본 5분 recent-auth 및 기존 SNS 검증 유지. 서버 정책 완화나 클라이언트 우회 없음.
- 결정사항: UI 단계 자동화는 권장안이며 코드 변경 미수행.
- 위험 요소: 재인증 완전 제거는 탈취된 세션에 공격자 SNS를 추가하는 위험. 대상 SNS 재인증 요구도 별도 존재.
- 다음 작업: 사용자 요청 시 최근 인증 재사용 및 단계 자동화 구현, 정책 완화는 별도 검토.

## 2026-10-01 — 오래된 로그인 판단 기준 설명

- 브랜치: develop.
- 작업 목표: SNS 추가 연결의 최근 인증 기준 명확화.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: ProviderChangeService.recent와 ProviderChangeProperties 확인. Firebase auth_time을 서버 현재 시각과 비교하며 기본 5분 초과 시 재인증 요구. 앱 사용 시각·Identity 토큰 재발급과 구분.
- 실행한 테스트와 결과: 코드 읽기 및 git diff --check, 설명/문서만 변경하여 실행 테스트 생략.
- 유지한 계약: 최근 인증·토큰 만료 검사 유지, 서버 설정 변경 없음.
- 결정사항: 기본 코드 기준 설명이며 실배포 설정은 이번에 확인하지 않음.
- 위험 요소: 자동 토큰 갱신이 auth_time을 갱신한다고 오해하지 않도록 안내.
- 다음 작업: 요청 시 최근 SNS 인증 재사용 UX 구현.

## 2026-10-01 — 최근 인증 기준 설명 기록 보완

<!-- codex-turn:01a0f669-50fa-7b92-a3c0-7f60a993f453 -->

- 브랜치: develop.
- 작업 목표: 현재 turn의 최근 인증 기준 설명 기록 식별자 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Firebase auth_time 기준 기본 5분 초과 시 SNS 연결 재인증이 필요하며 일반 로그인 유지 및 토큰 갱신과 다름을 설명. 코드 변경 없음.
- 실행한 테스트와 결과: git diff --check 통과. 문서만 변경하여 실행 테스트 생략.
- 유지한 계약: 기존 recent-auth 검증 유지, 비밀값 비기록.
- 결정사항: 배포 설정 변경 없음, 코드 기본값 설명.
- 위험 요소: 실배포 설정은 별도 확인 필요.
- 다음 작업: 요청 시 최근 인증 재사용 UX 구현 및 검증.

## 2026-10-01 — 5분 재인증 정책의 실제 UX 영향 설명

- 브랜치: develop.
- 작업 목표: 일상적인 SNS 추가 연결에서 5분 기준이 사실상 재인증을 요구한다는 사용자 지적에 답변.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 설명만 수행. 최근 인증 재사용은 가입/로그인 직후에 주로 유효하고 장기 로그인 사용자 불편은 해결하지 못함을 명확화. 시간 연장만으로 문제를 해결한다고 단정하지 않고 정책 완화와 보안 위험 구분.
- 실행한 테스트와 결과: git diff --check, 설명/기록만 변경하여 실행 테스트 생략.
- 유지한 계약: 현재 서버 인증 요구 변경 없음.
- 결정사항: 매번 인증 UX와 유효 세션 기반 연결 허용은 별도 정책 선택이며 이번에 구현하지 않음.
- 위험 요소: 기존 소유 증명 제거 시 탈취 세션에 공격자 SNS 추가 가능. 단순 토큰 갱신은 재인증 대체 불가.
- 다음 작업: 사용자 정책 선택 후 보안 검토 및 구현 범위 확정.

## 2026-10-01 — SNS 추가 연결 unavailable 설정 진단

- 브랜치: develop.
- 작업 목표: 사용자 PROVIDER_CHANGE_UNAVAILABLE 원인 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드의 기능 OFF/서비스 미설치/트랜잭션 실패 경로 확인. AWS 콘솔에서 테스트 서비스 참조 개정 10 및 환경 변수 48개 조회. SNS 허용과 session fence는 true, provider change fence 및 provider link 플래그는 없음. application.yml 기본 false로 명시 연결 비활성 확인.
- 실행한 테스트와 결과: AWS CLI는 자격증명 부재로 조회 실패, Chrome 콘솔 읽기로 확인. git diff --check. 진단만 수행하여 실행 테스트 생략.
- 유지한 계약: 계정 연결·설정 수정·재배포·비밀값 조회 없음.
- 결정사항: 두 연결 플래그의 테스트 환경 활성화는 별도 승인 후 수행. 5분 재인증 문제와 다른 원인.
- 위험 요소: 설정 활성화 후 DB 트랜잭션 및 실제 연결 검증은 남음. 오류 코드 자체는 트랜잭션 오류에도 사용됨.
- 다음 작업: 승인 후 테스트 태스크 정의에 FIREBASE_PROVIDER_CHANGE_FENCE_ENABLED=true 및 FIREBASE_PROVIDER_LINK_ENABLED=true 적용하고 재배포/검증.

## 2026-10-01 — 재인증 UX 정책 설명 기록 보완

<!-- codex-turn:01a0f66b-0edf-7711-9beb-e8d0934d8128 -->

- 브랜치: develop.
- 작업 목표: 5분 재인증 정책의 실제 UX 영향 설명에 현재 turn 식별자 추가.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 장기 로그인 사용자에게 사실상 매번 재인증이 필요함을 설명. 기존 인증 생략은 UI 수정이 아니라 서버 보안 정책 변경임을 구분.
- 실행한 테스트와 결과: git diff --check 통과. 설명/기록만 변경하여 실행 테스트 생략.
- 유지한 계약: 현재 recent-auth 및 기존 계정 소유 검증 유지, 비밀값 비기록.
- 결정사항: 코드·서버 설정 변경 없음.
- 위험 요소: 기존 인증 제거 시 탈취 세션을 통한 공격자 SNS 연결 위험.
- 다음 작업: 사용자 정책 선택 후 보안 검토 및 구현 범위 확정.

## 2026-10-01 — SNS 연결 오류 진단 turn 기록 보완

<!-- codex-turn:01a0f66c-948a-75a2-8a54-78b709f5a5b7 -->

- 브랜치: develop.
- 작업 목표: PROVIDER_CHANGE_UNAVAILABLE 진단의 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: AWS 테스트 서비스 참조 개정 10에서 provider change fence 및 provider link 설정 누락 확인. 코드 기본 false와 연결 기능 비활성 오류 경로 대조. 앞선 진단 기록을 보완하며 과거 기록 보존.
- 실행한 테스트와 결과: AWS 콘솔 읽기 확인 및 git diff --check 통과. 분석/문서만 변경하여 실행 테스트 생략.
- 유지한 계약: 계정·서버 설정·배포 변경 없음, 비밀값 비기록.
- 결정사항: 테스트 환경의 두 연결 플래그 활성화 및 재배포 승인 대기.
- 위험 요소: 활성화 후 실제 연결 및 DB 처리 검증 필요.
- 다음 작업: 사용자 승인 후 테스트 환경만 설정 적용/재배포.

## 2026-10-01 — 테스트 SNS 추가 연결 활성화 배포

- 브랜치: develop.
- 작업 목표: 사용자 승인한 테스트 환경 provider change fence 및 provider link 활성화 적용.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 애플리케이션 코드 변경 없음.
- 구현 내용: AWS ECS tosunsaeng-identity-test:10 기반 개정 11에 FIREBASE_PROVIDER_CHANGE_FENCE_ENABLED=true만 추가하여 먼저 배포. 성공/개정 10 실행 0 확인 후 개정 12에 FIREBASE_PROVIDER_LINK_ENABLED=true 추가하여 배포. 환경 변수 48→49→50개, 두 값 true 확인. 기존 이미지 1cca3c17c3460e01a7ed75b82f4c22ed6fabede0 유지.
- 실행한 테스트와 결과: ECS 최종 배포 성공, 1 실행/0 보류 및 이전 개정 0 실행 확인. 공개 actuator/health UP. git diff --check 통과. 코드 변경이 없어 Gradle 테스트 생략, 실제 SNS 인증/연결 API 변경 요청은 수행하지 않음.
- 유지한 계약: 테스트 서비스만 변경, 운영·IAM·Secret·토큰 TTL·5분 recent-auth·unlink/worker 설정 유지. 기존 provider 보호 OFF writer가 종료된 후 link 신규 접수 활성화. 계정/전화번호 변경 없음.
- 결정사항: 사용자 명시 승인 범위의 두 플래그를 단계적 배포. 기존 CI는 현재 서비스 태스크 정의를 읽고 이미지/SENTRY_RELEASE를 갱신하므로 이번 환경 변수를 유지하는 구조이며 워크플로는 수정하지 않음.
- 위험 요소: 실제 SNS 연결/DB 트랜잭션 검증은 별도 필요. STARTED 연결은 임의 초기화/자동 해제 금지, 문제 발생 시 원래 요청 상태 조회 우선. 연결 데이터 생성 후 provider fence를 끄는 롤백 금지.
- 예상 밖 변경: 없음. 기존 문서 변경 보존, 요청한 외부 설정 두 개 외 변경 없음. Jira/commit/push 미수행.
- 다음 작업: 로컬 도구에서 기존 요청 상태 확인 후 같은 prepare 요청으로 재시도, 최근 인증 만료 시 기존 SNS 재인증. 실제 complete 성공 및 동일 회원/전화번호 유지 검증.

## 2026-10-01 — 테스트 SNS 연결 활성화 배포 기록 보완

<!-- codex-turn:01a0f66e-090c-7241-ae20-b905461d3b9c -->

- 브랜치: develop.
- 작업 목표: 이번 테스트 SNS 연결 활성화 배포의 정확한 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 승인된 provider change fence를 개정 11에 먼저 배포하고 기존 태스크 종료 후 provider link를 추가한 개정 12 배포 완료. 두 설정 true 확인.
- 실행한 테스트와 결과: ECS 배포 성공, 1 실행/0 보류 및 이전 개정 0 실행, 공개 health UP 확인. git diff --check 통과. 코드 변경이 없어 Gradle 미실행.
- 유지한 계약: 운영 서버·기존 인증 정책·기존 이미지 유지. 비밀값 비기록, 실제 계정 연결 미수행.
- 결정사항: 테스트 환경 설정 적용 완료, 추가 배포 없음.
- 위험 요소: 실제 SNS 연결 완료 및 회원 데이터 보존 검증은 남음.
- 다음 작업: 사용자 로컬 테스트에서 상태 조회 후 동일 요청 재시도 및 연결 결과 확인.

## 2026-10-01 — 새로고침 후 SNS 연결 충돌 진단

<!-- codex-turn:01a0f67f-41a7-7422-b39d-ebeffb1e20df -->

- 브랜치: develop.
- 작업 목표: 첫 연결 실패 후 새로고침한 사용자의 PROVIDER_CHANGE_CONFLICT 원인 조사.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 로컬 화면의 Google PREPARE_UNKNOWN 확인, 현재 요청 상태 조회 결과 PROVIDER_OPERATION_NOT_FOUND. 테스트 DB provider_link_attempts에 GOOGLE STARTED 1건(17:01 시작, 17:06 만료) 확인. 현재 로그인 회원과 DB 작업 소유자의 일치 및 실제 Firebase 연결 결과는 미확정. ProviderLinkService.start의 보호 slot 점유와 prepare 충돌 조건, 새로고침 시 로컬 작업 메모리 유실 및 기존 작업 복구 UI 부재 확인.
- 실행한 테스트와 결과: 코드/로컬 UI/Atlas 읽기 및 상태 조회만 수행. 세션 control 대조는 Atlas 화면이 이전 컬렉션 내용을 유지하여 증거로 사용하지 않음. git diff --check 통과. 코드 변경 없어 실행 테스트 생략.
- 유지한 계약: 계정·연결·DB 수정/삭제/잠금 해제 없음. 불명 작업의 SDK 재실행 금지, STARTED 만료 후 자동 해제 금지 유지. 식별자·비밀값 기록 없음.
- 결정사항: 남은 STARTED 작업은 사용자 설명과 정황상 일치하나 정확한 소유 대조 전 확정하지 않음. 새 준비 요청 반복 대신 원래 작업과 Firebase 결과 대조 필요.
- 위험 요소: 현재 로컬 도구는 새로고침 후 원래 요청 복구 불가. 다른 회원 소유 SNS 선택 실패 후 원격 연결 성공 여부가 불명확할 수 있으므로 임의 activeLogoutId 초기화 금지.
- 다음 작업: 정확한 회원·원래 작업·Firebase 연결 결과를 대조하고 별도 승인된 조건부 복구 설계. 로컬 작업 복구와 명확한 실패 안내 개선 검토.

## 2026-10-01 — 실패한 SNS 연결 작업의 보존과 복구 설명

<!-- codex-turn:01a0f67f-41a7-7422-b39d-ebeffb1e20df -->

- 브랜치: develop.
- 작업 목표: 첫 실패 후 작업이 계속 남는지 및 해결 방향 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: ProviderLinkAttempt.start가 cleanupAt을 제거하고 만료된 STARTED를 조회 시 ACTION_REQUIRED로 표시하는 구현 확인. PREPARED 만료와 구분. 현재 자동 실패 종료/재개 복구가 부족함을 설명.
- 실행한 테스트와 결과: 코드 읽기 및 git diff --check. 설명/문서만 변경하여 실행 테스트 미수행.
- 유지한 계약: 원격 결과 불명 시 자동 보호 해제 금지. DB/계정/외부 설정 수정 없음.
- 결정사항: 실제 연결 부재 및 이전 실행 종료 증거 확보 후 조건부 복구 필요. 실패 취소/상태 복구 설계는 제안이며 미구현.
- 위험 요소: SDK 오류만으로 원격 연결 미실행을 단정할 수 없음. 현재 작업의 정확한 회원 소유 대조 미완료.
- 다음 작업: 정확한 작업과 원격 상태 대조, 승인된 복구 및 재시도 UX 구현 범위 확정.

## 2026-10-01 — 실패 연결 작업 보존 설명 기록 보완

<!-- codex-turn:01a0f683-a3f8-7e60-8bb8-1f2f618ac6c9 -->

- 브랜치: develop.
- 작업 목표: 실패 연결 작업 보존 및 복구 설명의 정확한 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: PREPARED 만료와 STARTED 보호 유지의 차이, 확정된 원격 결과와 이전 실행 종료 증거에 기반한 조건부 복구 필요성을 설명. 앞선 식별자 오기를 보완하며 과거 기록 보존.
- 실행한 테스트와 결과: git diff --check 통과. 문서만 변경하여 실행 테스트 생략.
- 유지한 계약: 불명 상태의 자동 보호 해제 금지, 비밀값 비기록.
- 결정사항: 복구 기능은 제안 단계, 코드·계정·DB·외부 설정 변경 없음.
- 위험 요소: 정확한 작업 소유 및 Firebase 연결 결과 대조가 아직 필요.
- 다음 작업: 원래 작업 대조 및 승인된 복구/재시도 UX 구현 범위 확정.

## 2026-10-01 — 막힌 SNS 연결 작업 복구 사전 점검

- 브랜치: develop.
- 작업 목표: 사용자 요청에 따라 현재 미완료 연결을 안전하게 복구하기 위한 정확한 상태 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 사용자 첫 SDK 오류가 auth/credential-already-in-use임을 확인. 테스트 Atlas 실제 user_session_controls 컬렉션을 열고 exact owner 필터로 단일 문서 조회, 기존 STARTED attempt의 link slot 일치 확인. Firebase 콘솔에서 작업 UID와 일치하는 계정의 제공자는 OIDC와 Phone이며 Google은 없음 확인. 현재 로컬 로그인과 owner의 직접 대조 및 이전 SDK 종료 확인은 추가 필요.
- 실행한 테스트와 결과: CloudShell 재연결 후 임시 venv에 공식 boto3/pymongo/google-auth/requests 설치. 테스트 MongoDB Secret을 메모리로 읽는 제한 진단은 ServerSelectionTimeoutError로 종료, 비밀값 출력 없음. Atlas 테스트 프로젝트 허용 목록에 기존 단일 서버 IP만 있으며 관리 셸 IP는 미포함 확인. git diff --check 통과, 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 사용자·Firebase 연결·DB·네트워크 설정 수정 없음. 무조건 slot 초기화나 다중 문서 개별 변경 미수행. 운영 자원 변경 없음.
- 결정사항: 조건부 트랜잭션 복구를 위해 테스트 Atlas에 관리 셸 단일 IP /32 임시 허용 승인 필요. 승인 전 접속 허용 변경하지 않음.
- 위험 요소: Google 연결 부재만으로 SDK 종료를 단정하지 않음. 복구 전 정확한 binding/version/phase 및 다른 미완료 작업 대조 필요. 관리 셸 IP는 세션마다 바뀔 수 있음.
- 다음 작업: 임시 네트워크 접근 승인 후 DB/Firebase 읽기 검증과 제한 복구 설계, 복구 적용 시 변경 내용 확인. 임시 접근은 작업 종료 후 제거.

## 2026-10-01 — SNS 연결 복구 사전 점검 기록 보완

<!-- codex-turn:01a0f684-e7ea-7682-a5af-15695f51935b -->

- 브랜치: develop.
- 작업 목표: 현재 연결 복구 사전 점검의 정확한 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 원래 연결 작업과 회원 세션 잠금 일치, 해당 Firebase UID의 Google 연결 부재 확인. 관리 셸의 DB 접속 시간 초과 및 Atlas 허용 목록에 해당 IP 미포함 확인.
- 실행한 테스트와 결과: 읽기 검증 및 git diff --check 통과. 코드 변경 없어 실행 테스트 생략.
- 유지한 계약: DB·계정·네트워크 설정 변경 없음, 비밀값 비기록.
- 결정사항: 단일 관리 IP 임시 허용 승인 및 이전 SDK 실행 종료 확인 대기.
- 위험 요소: 현재 로그인 회원과 작업 소유 직접 대조 및 원자적 복구 검증이 남아 있음.
- 다음 작업: 승인 후 제한된 검증과 조건부 복구 진행, 종료 후 임시 접근 제거.

## 2026-10-01 — reissue 절대 만료 헤더 null 원인 조사

<!-- codex-turn:01a0f68c-1d9d-7901-8991-188d8660c536 -->

- 브랜치: develop. 특정 Jira 이슈 구현 요청 없음.
- 작업 목표: /auth/reissue의 Reissue-Access-Expires-At 및 Reissue-Refresh-Expires-At 헤더가 null로 읽히는 원인 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 애플리케이션 변경 없음.
- 구현 내용: TokenReissueService의 Recovery OFF 결과에서 절대 만료 필드가 null이며 AuthController는 accessExpiresAt 비-null일 때만 두 헤더를 설정함을 확인. application.yml의 AUTH_REISSUE_RECOVERY_ENABLED 기본 false, 조건부 Recovery Bean 설치, ON 경로 실제 만료 시각 반환 확인. CORS 노출 설정은 저장소에서 발견되지 않음.
- 실행한 테스트와 결과: ./gradlew test --tests '*ReissueRecoveryHttpTests' --tests '*ReissueRecoveryConfigurationTests' --tests '*TokenReissueServiceTests' BUILD SUCCESSFUL. 실제 실행 suite/count는 XML 결과로 확인. git diff --check 수행. 전체 clean test는 코드 변경 없는 진단이라 미실행.
- 유지한 계약: 재발급/멱등 재전달/세션 fence/암호화 계약 변경 없음. 배포 환경변수와 프록시, 외부 설정 변경 없음. 토큰/비밀값 조회·출력·기록 없음.
- 결정사항: 서버가 헤더를 생략하는 OFF 경로와 브라우저 CORS로 읽지 못하는 경우를 구분. 200 원 응답 헤더와 배포 flag 확인이 우선. 헤더만을 위해 복구 flag를 즉시 켜지 않으며 fence·암호키·인덱스·클라이언트 요청 ID 준비 및 runbook 검증 필요.
- 위험 요소: 실제 배포 flag/요청 상태/응답 헤더/클라이언트 플랫폼/프록시 미확인. 오류 응답에서도 헤더가 없는 것은 의도된 동작. 프론트 가이드 성공 헤더 안내는 Stage 9 ON 조건과 함께 읽어야 함.
- 예상 밖 변경: 없음. 기존 문서 변경 보존, 코드/커밋/push/Jira/배포 변경 없음.
- 다음 작업: 사용자 요청 환경과 원 응답 헤더 확인 후 OFF 경로의 헤더 제공 개선 또는 exact-origin CORS 노출 필요 여부 판단(구현 별도 승인/요청).

## 2026-10-01 — reissue recovery 활성화 재배포 사전 점검 중단

<!-- codex-turn:01a0f68d-bee4-7942-a6cd-94c3321b2650 -->

- 브랜치: develop.
- 작업 목표: 사용자 요청에 따른 recovery 활성화/재배포 전 대상 및 선행 조건 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Stage 9 runbook 및 main/develop 배포 분리 문서 확인. 기존 AWS 테스트 서비스 콘솔에서 task definition tosunsaeng-identity-test:12, 실행 1/대기 0, 배포 성공 표시 읽기 확인.
- 실행한 테스트와 결과: 문서/UI 읽기, git diff --check. 이번에는 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 운영/테스트 경계 유지. 환경변수 변경, 태스크 등록, 재배포, 커밋/push/Jira 변경 미수행. 비밀값 미조회·비기록.
- 결정사항: test/staging 대상 확인 질문 후 준비 점검 중 사용자 중단으로 외부 변경 진행하지 않음.
- 위험 요소: 실제 recovery/fence 값, 암호키 연결, DB 인덱스, 프론트 Idempotency-Key 준비 미검증. 콘솔 배포 성공은 recovery 활성화를 뜻하지 않음.
- 예상 밖 변경: 없음. 기존 미커밋 기록 보존.
- 다음 작업: 사용자 재개 요청 시 배포 대상 확정 및 필수 준비 검증 후 활성화/재배포 진행.

## 2026-10-01 — reissue 재배포 중단 안내 기록 보완

<!-- codex-turn:01a0f68f-2f5f-7d22-93ea-c9c3b76d8d18 -->

- 브랜치: develop.
- 작업 목표: 현재 turn의 작업 기록 누락 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 애플리케이션 변경 없음. 재배포 미실행 및 대상 확인 대기 상태 기록.
- 실행한 테스트와 결과: git diff --check. 기록만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 WORKLOG 보존, 비밀값 비기록, 외부 설정/배포/커밋/push 변경 없음.
- 결정사항: test/staging 대상 확인 및 세션 fence·키·인덱스·클라이언트 준비 검증 후 재개.
- 위험 요소: 선행 조건 및 실환경 recovery 설정 미검증.
- 예상 밖 변경: 없음. 기존 기록 변경 보존.
- 다음 작업: 사용자 배포 대상 확인 후 안전한 활성화/재배포 진행.

## 2026-10-01 — 테스트 Google 연결 실패 작업 조건부 복구 완료

<!-- codex-turn:01a0f68c-7cc7-7223-9d90-038554e45929 -->

- 브랜치: develop.
- 작업 목표: credential-already-in-use 이후 남은 테스트 회원의 실패 연결 작업 1건을 승인된 범위에서 복구하고 준비 재시도 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 애플리케이션 코드 변경 없음.
- 구현 내용: 사용자 팝업 종료·재시도 없음 확인 및 복구 승인 후, 정확한 작업/소유/버전/단계와 Firebase Google 연결 부재, 다른 미완료 작업 부재 검증. dry-run 후 snapshot/majority 트랜잭션으로 provider_link_recovery_audit에 원본 작업과 보호 상태를 백업하고 해당 실패 작업만 활성 컬렉션에서 제거, 해당 slot만 해제 및 제어 revision/version 증가. Google 차단 및 인증 floor 유지. 임시 관리 접속 /32 허용은 검증 후 제거하고 기존 서버 접근 보존 확인.
- 실행한 테스트와 결과: RECOVERY_DRY_RUN_PASSED_NO_WRITES 및 RECOVERY_COMMITTED 확인. 감사 백업 존재, 실패 작업 제거, slot 해제, epoch/Google 차단/auth floor/회원/binding/원격 제공자/전화번호 보존 확인. Chrome에서 기존 Kakao 재인증 후 Google 연결 prepare 성공 확인. 문서 diff 검사 수행, 코드 변경 없는 운영 복구로 Gradle 미실행.
- 유지한 계약: 운영 자원 불변, 기존 회원·Kakao·전화번호·세션 보호 유지, 비밀값/개인 식별자 비기록. SDK 임의 재실행 없음.
- 결정사항: 기존 실패 작업은 감사 백업으로 보존. 일반 실패 복구 기능 구현과 재배포는 이번 범위 제외. 새로운 Google 연결은 PREPARED까지만 확인.
- 위험 요소: 실제 Google 연결/완료 검증은 남아 있음. 다른 회원 소유 Google로 다시 시도하면 동일 실패가 재발할 수 있음. 감사 snapshot의 무조건 복원 금지.
- 예상 밖 변경: 없음. 기존 문서 변경 보존, commit/push/Jira/배포 변경 없음.
- 다음 작업: 다른 회원에게 연결되지 않은 Google 계정을 사용자가 선택하여 SDK 연결·대상 재인증·완료 검증 진행. 이후 실패 종료 및 새로고침 복구 UX 별도 개선.

## 2026-10-01 — 테스트 reissue recovery 활성화 선행 설정 확인

- 브랜치: develop.
- 작업 목표: 사용자 요청에 따라 테스트 recovery 활성화 재배포 가능 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: CloudShell 읽기 전용 AWS CLI로 실제 서비스 개정 12, desired/running 1, rollout COMPLETED 확인. 해당 개정 AUTH_REISSUE_RECOVERY_ENABLED=false, AUTH_SESSION_FENCE_ENABLED=true 확인. recovery 키 ID/환경/파일 설정 및 keyring secret 주입·volume mount 없음 확인. runbook과 entrypoint의 필수 기동 조건 대조.
- 실행한 테스트와 결과: AWS 조회 및 코드/운영 가이드 대조. git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 운영 및 실행 서비스 변경 없음, 비밀값 조회·출력 없음. Idempotency-Key 필수화 및 exact-origin CORS 노출 별도 검증 필요성 유지.
- 결정사항: true만 설정하면 기동 실패하므로 배포 미실행. 테스트 전용 keyring 연결 및 필요한 최소 읽기 권한 승인 확인 후 준비 진행.
- 위험 요소: 실제 DB 인덱스/키 형식/클라이언트 멱등 요청 준비 미검증. OFF는 헤더 생략 원인이지만 실제 HTTP 원 응답과 브라우저 CORS 가시성은 별도 검증 필요.
- 예상 밖 변경: 없음, 기존 기록 보존. commit/push/Jira 변경 없음.
- 다음 작업: 테스트 keyring 주입 범위 승인, 키/인덱스/클라이언트 준비 확인 후 활성화 배포와 헤더·동일 요청 재전달 검증.

## 2026-10-01 — 테스트 recovery 사전 점검 turn 기록 보완

<!-- codex-turn:01a0f696-a3f8-79c3-91c5-3e9eb3b2db77 -->

- 브랜치: develop.
- 작업 목표: 이번 테스트 recovery 활성화 사전 점검의 turn 식별자 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 실행 개정 12의 recovery=false, fence=true 및 필수 keyring 주입/키 ID/환경 설정 누락 확인 결과 기록. 코드 변경 없음.
- 실행한 테스트와 결과: AWS 읽기 조회와 기동 조건 대조 완료. git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 비밀값 비기록, 실행 서비스/운영/권한 변경 없음.
- 결정사항: 테스트 keyring 연결과 필요한 최소 읽기 권한 승인 대기, 배포 미실행.
- 위험 요소: 키 형식/실제 인덱스/클라이언트 Idempotency-Key 준비와 응답 헤더 검증 필요.
- 예상 밖 변경: 없음, 기존 기록 보존.
- 다음 작업: 승인 후 준비 검증 및 테스트 활성화 배포.

## 2026-10-01 — 승인된 테스트 reissue recovery 활성화 배포 완료

- 브랜치: develop.
- 작업 목표: 테스트 재발급 응답 복구 및 절대 만료 헤더 경로 활성화.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 애플리케이션 코드 변경 없음.
- 구현 내용: 사용자 승인 후 Secrets Manager 테스트 keyring을 메모리에서 형식·32바이트 키 길이·활성 ID 존재 검증(원문 비출력). 실행 역할의 기존 GetSecretValue 권한 허용 확인하여 IAM 변경 없음. 기존 개정 12를 복제하여 개정 13에 recovery=true, active-key-id=test-v1, environment=test 및 AUTH_REISSUE_ENCRYPTION_KEYRING_CONTENT secret 참조만 추가. 기존 이미지·네트워크·역할·다른 설정 유지. AWS 개정 간 비교에서 environment/secrets 외 변경 없음 확인.
- 실행한 테스트와 결과: 관련 45개 테스트 통과, ./gradlew clean test 전체 1024개 성공(실패/오류/skip 0). 최초 clean 명령은 캐시 sandbox 거절, 승인된 재실행 성공. Atlas 실제 회전 unique/partial 인덱스와 응답 source unique/cleanup TTL 인덱스 존재 확인. 개정 13 startup 성공, 기동 실패/오류 유형 미검출, ALB healthy, 최종 ECS COMPLETED 1 running/0 pending 확인. 공개 health 200 UP. 가짜 credential smoke에서 헤더 누락 400 INVALID_REISSUE_REQUEST_ID, canonical ID 포함 401 INVALID_REFRESH_TOKEN, no-store 확인. git diff --check 수행.
- 유지한 계약: 운영 자원/계정/세션 데이터 변경 없음. 실제 토큰/키/URI 비기록. 기존 이미지와 fence 유지. 테스트 ON 환경에서 소문자 UUID v4 Idempotency-Key 필수화.
- 결정사항: 키 주입은 기존 entrypoint가 private runtime 파일 생성 후 환경변수 제거하는 방식. 등록 입력과 반환 객체 직접 비교는 일치하지 않아 개정 간 실제 필드 비교로 재검증했으며 예상 변경만 존재. 배포 완료 후 점검 Python 세션 종료.
- 위험 요소: 실제 사용자 성공 재발급의 두 만료 헤더 및 동일 ID 재전달은 아직 실환경 미검증(HTTP 자동 테스트 통과). 모바일 crash/single-flight 및 replica-set failover 등 runbook 운영 증빙은 별도. 로컬 테스트 프록시는 응답 헤더를 전달하지 않아 서버 ON 이후에도 브라우저 헤더 조회가 null일 수 있음. exact-origin CORS 노출 별도 확인 필요.
- 예상 밖 변경: 없음. 기존 문서 수정 보존. commit/push/Jira 변경 없음. 전체 clean test는 로컬 코드 검증으로 배포 이미지 자체 테스트와 구분.
- 배포 전 확인 사항: 키 형식/접근 권한/fence/인덱스 확인 완료, 클라이언트 멱등 헤더 요구 안내. 긴급 중단은 enabled 유지 + maintenance 사용(runbook 준수).
- 다음 작업: 사용자 테스트 로그인으로 성공 재발급 원 응답 헤더/동일 ID replay 검증. 필요 시 로컬 프록시 헤더 전달 및 프론트 오류 처리 보완.

## 2026-10-01 — 테스트 recovery 배포 완료 turn 기록 보완

<!-- codex-turn:01a0f698-cf44-7e62-8ea4-a9d7f84ae171 -->

- 브랜치: develop.
- 작업 목표: 승인된 테스트 recovery 배포 작업의 현재 turn 식별자 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 테스트 개정 13 recovery 활성화 및 기존 테스트 keyring 주입 완료. 기존 이미지/IAM/운영 유지. 애플리케이션 코드 변경 없음.
- 실행한 테스트와 결과: 전체 clean test 1024개 통과. ECS COMPLETED, ALB healthy, 공개 health UP 및 가짜 credential을 이용한 ON 계약 smoke 확인. git diff --check 통과.
- 유지한 계약: 비밀값 비기록, 실제 사용자 세션 변경 없음, ON 환경 Idempotency-Key 필수.
- 결정사항: 배포 완료, 로컬 프록시 헤더 전달 수정은 별도 작업.
- 위험 요소: 실제 사용자 성공 응답의 절대 만료 헤더와 동일 ID replay 실환경 검증은 남아 있음.
- 예상 밖 변경: 없음. 기존 기록 보존.
- 다음 작업: 실제 성공 응답 헤더/replay 검증 및 필요 시 프록시 개선.

## 2026-10-01 — Chrome 기존 MEMBER SNS 추가 연결 성공 확인

- 브랜치: develop.
- 작업 목표: 사용자가 완료한 기존 회원의 SNS 추가 연결 테스트 결과 읽기 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome 로컬 테스트 탭 검증표에서 Kakao 인증/Identity 로그인/SNS 추가 연결 성공 및 서버 COMPLETED, 동일 Firebase UID, 기존 전화번호·SNS 유지 표시 확인. 현재 폼은 완료 상태 정리 후 연결 전으로 초기화되어 있으며 계정 연결과 토큰은 유지된다는 안내 확인. 로컬 도구 소스에서 complete 응답 COMPLETED 검사 후 성공 표시하는 흐름 대조.
- 실행한 테스트와 결과: UI 읽기 및 성공 표시 조건 코드 대조. 연결/로그인 요청 재실행 없음. 문서만 변경하여 Gradle 미실행, git diff --check 수행.
- 유지한 계약: 개인정보·토큰 비출력, 계정·인증 상태 변경 없음.
- 결정사항: 기존 Kakao 회원에 Google 추가 연결 완료 테스트는 성공으로 확인. 폼 초기화는 연결 해제가 아님.
- 위험 요소: 새 Google 단독 로그인 후 동일 Identity 회원 확인, Android/iOS 실기기 동작, 실패 후 복구 UX는 이번 확인에 포함되지 않음. 이번에는 DB 직접 재조회 없이 UI 완료 결과 및 코드 근거로 확인.
- 예상 밖 변경: 없음. 기존 문서 변경 보존.
- 다음 작업: 사용자가 원하면 추가한 Google 단독 로그인 및 동일 회원/기록 접근 확인. 실패·새로고침 복구 개선은 별도.

## 2026-10-01 — SNS 연결 결과 확인 turn 기록 보완

<!-- codex-turn:01a0f6a2-8df8-7990-aa0a-9fd4df5cc4bf -->

- 브랜치: develop.
- 작업 목표: 현재 SNS 추가 연결 결과 확인 작업의 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome 검증표의 서버 COMPLETED 및 기존 UID/전화번호/SNS 유지 표시를 확인. 폼 초기화는 테스트 상태 정리이며 연결 해제가 아님을 설명.
- 실행한 테스트와 결과: UI 읽기와 로컬 도구 성공 표시 조건 대조, git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 계정/로그인/연결 변경 없음, 비밀값 비기록.
- 결정사항: 추가 연결 성공 확인, 새 Google 단독 로그인 검증은 별도.
- 위험 요소: 실기기 및 실패 복구 UX 미검증.
- 예상 밖 변경: 없음. 기존 기록 보존.
- 다음 작업: 사용자 요청 시 새 SNS 단독 로그인으로 동일 회원 접근 확인.

## 2026-10-01 — 추가한 Google 단독 로그인과 프로필 인증 확인

- 브랜치: develop.
- 작업 목표: 사용자가 진행한 Google 로그인 성공 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Chrome 검증표의 Google Firebase 인증/Identity 로그인 성공 확인 후 내 프로필 인증 확인 버튼으로 읽기 요청 수행. 서버 MEMBER 인증 성공 확인.
- 실행한 테스트와 결과: 실제 테스트 프로필 API MEMBER 인증 성공. git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 로그인/연결 재실행 없음, 개인정보/토큰 비출력.
- 결정사항: 추가 SNS 연결 후 Google 단독 Identity 로그인과 보호 API 인증 성공 확인.
- 위험 요소: 이전 Identity UUID와 직접 비교하거나 LC 기록 동일성 검증한 것은 아님. 모바일 실기기/실패 복구 UX는 별도.
- 예상 밖 변경: 없음. 기존 문서 변경 보존.
- 다음 작업: 필요 시 이전 회원 동일성 및 LC 기록 검증, 실패 후 연결 복구 개선.

## 2026-10-01 — Google 로그인 검증 turn 기록 보완

<!-- codex-turn:01a0f6a4-42a5-7b30-9574-01b57551ac13 -->

- 브랜치: develop.
- 작업 목표: Google 단독 로그인 검증의 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: Google 인증·Identity 로그인 성공 표시 확인 후 프로필 읽기 API로 MEMBER 인증 성공 확인.
- 실행한 테스트와 결과: 실환경 프로필 인증 성공, git diff --check 통과. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 개인정보·토큰 비기록, 연결/로그인 재실행 없음.
- 결정사항: Google 로그인과 보호 API 접근 검증 완료.
- 위험 요소: 이전 Identity UUID 직접 비교 및 LC 기록 동일성은 미검증.
- 예상 밖 변경: 없음. 기존 기록 보존.
- 다음 작업: 필요 시 회원 동일성/LC 기록 검증 및 실패 복구 UX 개선.

## 2026-10-01 — SNS 연결 실패 후 재시도 복구 수정 계획 설명

- 브랜치: develop.
- 작업 목표: credential-already-in-use 이후 지속된 PROVIDER_CHANGE_CONFLICT의 원인과 수정 방향 설명(구현 제외).
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: ProviderLinkService.start의 provider 차단 및 slot 점유, prepare의 기존 slot 충돌 검사, ProviderLinkAttempt의 STARTED cleanupAt=null 및 실패 종료 상태 부재 확인. 만료 status는 ACTION_REQUIRED일 뿐 해제되지 않음. 로컬 ProviderLinkFlow.link는 SDK 호출 전 LINK_UNKNOWN 설정 후 예외 종료, 메모리 전용 상태라 새로고침 시 원 요청 ID 소실 확인.
- 실행한 테스트와 결과: 서버/로컬 도구 코드 및 Stage 10 runbook 5.5 대조. git diff --check 수행. 분석만 수행하여 Gradle 미실행.
- 유지한 계약: 타 회원 소유 SNS 연결 거절 유지. 만료/클라이언트 실패 주장만으로 slot 또는 provider 보호를 해제하지 않음. 개인정보/토큰 비기록.
- 결정사항: 실패 안내와 원 요청 복구 UX, 인증된 pending 조회 및 조건부 실패 종료/reconcile 경로 제안. SDK 종료·자동 재시도 부재 및 원격 상태 증거가 충분한 경우에만 exact owner/version/phase 트랜잭션으로 종료/slot 해제. provider 차단/auth floor는 보존, 실패 작업은 삭제하지 않고 감사 가능 종료 상태 유지. 불명 상태는 ACTION_REQUIRED 유지. 구현/API 명칭 미확정.
- 위험 요소: Firebase와 Mongo는 단일 원자 트랜잭션이 아니며 원격 부재 조회만으로 지연 SDK 호출 종료를 보장할 수 없음. 자동 복구의 증거 요건/관리 경로 구분 설계 필요. 기존 클라이언트 호환성 및 늦은 complete/동시 요청 검증 필요.
- 예상 밖 변경: 없음. 코드·외부 설정·계정·Jira 변경 없음.
- 다음 작업: 구현 요청 시 API/상태 계약과 복구 증거 요건 확정 후 서버·테스트 도구 및 회귀 테스트 구현. 성공 로그인 경로는 유지.

## 2026-10-01 — SNS 실패 복구 수정 계획 turn 기록 보완

<!-- codex-turn:01a0f6a5-445d-7f61-b677-0b5d40dad13e -->

- 브랜치: develop.
- 작업 목표: 실패 후 연결 재시도 문제 분석과 수정 계획의 현재 turn 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 타 회원 소유 SNS 거절은 정상이며, STARTED 잠금 유지·실패 종료 경로 부재·새로고침 시 원 요청 정보 소실이 지속 충돌 원인임을 설명. 조건부 종료/감사 기록/pending 조회/실패 안내 개선 제안. 코드 구현 없음.
- 실행한 테스트와 결과: 코드 및 운영 가이드 대조, git diff --check 통과. 분석만 수행하여 Gradle 미실행.
- 유지한 계약: 원격 결과 불명 시 보호 유지, 클라이언트 주장이나 만료만으로 잠금 해제 금지, 비밀값 비기록.
- 결정사항: 구현 요청 후 복구 증거 요건과 API/상태 계약 확정.
- 위험 요소: 원격 SDK와 DB의 원자성 부재 및 지연 호출 종료 증명 필요.
- 예상 밖 변경: 없음. 기존 기록 보존, 외부 변경 없음.
- 다음 작업: 승인된 구현 범위 확정 후 실패 종료·복구 UX와 회귀 테스트 추가.

## 2026-10-01 — SNS 연결 취소·재시도 UX 추가 계획

- 브랜치: develop.
- 작업 목표: 오래 걸리는 SNS 연결을 사용자가 취소하고 재시도할 수 있는지 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 기존 코드 분석에 기반해 PREPARED의 조건부 즉시 취소와 STARTED 이후 취소 요청/원격 상태 확인을 구분하는 계획 제시. UI 대기 종료와 원격 작업 취소는 다르며, 안전한 종료 확인 전 중복 SDK 연결 금지. 연결 완료 상태는 취소 대신 완료 안내, 연결 해제는 별도 인증 작업.
- 실행한 테스트와 결과: 이전 코드 분석 근거 사용 및 git diff --check 수행. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 시간 경과/팝업 닫힘만으로 잠금 해제 금지, 기존 SNS/전화번호 보호 및 미확정 대상 차단 유지. 비밀값 비기록.
- 결정사항: 실패 복구 계획에 취소 요청·진행 상태 복원·종료 후 재시도 UX 포함 제안. 새 상태/API 명칭과 시간 기준은 구현 전 확정. 아직 구현 없음.
- 위험 요소: Firebase SDK 지연 성공과 취소의 경합. 서버가 이전 실행 종료를 입증하지 못하면 즉시 재시도를 보장할 수 없음. 기존 SNS 로그인 유지 범위는 보호 정책에 따라 검증 필요.
- 예상 밖 변경: 없음. 기존 기록 보존, 외부 변경 없음.
- 다음 작업: 취소/start/complete 경합, 응답 유실, 새로고침 및 지연 SDK 결과 테스트를 구현 범위에 포함.

## 2026-10-01 — SNS 취소·재시도 계획 turn 기록 보완

<!-- codex-turn:01a0f6a7-07f7-7e12-a2e3-9bef009190e0 -->

- 브랜치: develop.
- 작업 목표: SNS 연결 취소·재시도 설계 설명의 현재 turn 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 준비 단계 취소, 연결 진행 중 종료 확인, 완료 후 별도 해제의 차이를 설명. 취소 버튼·상태 복원·안전한 종료 후 재시도와 지연 성공 경합 테스트를 계획에 포함 제안. 코드 변경 없음.
- 실행한 테스트와 결과: git diff --check 통과. 계획 설명만 수행하여 Gradle 미실행.
- 유지한 계약: 팝업 종료/시간 경과만으로 잠금 해제 금지, 결과 불명 시 중복 연결 제한, 비밀값 비기록.
- 결정사항: 취소와 실패 복구를 함께 설계하되 실제 구현은 후속 요청 대기.
- 위험 요소: 원격 SDK 지연 완료와 취소 경합으로 즉시 재시도를 항상 보장할 수 없음.
- 예상 밖 변경: 없음. 기존 기록 보존, 외부 변경 없음.
- 다음 작업: API/상태 계약과 종료 증거 요건 확정 후 취소·복구·경합 테스트 구현.

## 2026-10-01 — SNS 취소·실패 복구 상세 수정 계획 작성

- 브랜치: develop.
- 작업 목표: 서버 상태/API/프론트/보안 증거/테스트/배포 순서 상세 설명.
- 변경 파일: docs/contracts/provider-link-cancel-recovery-plan.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 현재 코드와 제안을 구분한 계층형 계획 문서 작성. PREPARED 원자 취소, STARTED 취소 요청과 제한된 조건부 복구, 실패 감사 상태, 원 요청 복원, pending 조회/권한, 경합 테스트 및 단계 배포 정리.
- 실행한 테스트와 결과: ProviderLinkAttempt/Service/Controller 및 이전 로컬 도구 조사 근거 대조, git diff --check 수행. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: status에서 SDK 실행권한 재부여 금지, 타 회원 SNS 거절, 불명 원격 실행에 대한 보호 유지, 비밀값 비기록.
- 결정사항: 클라이언트 오류/팝업 종료/원격 부재만으로 STARTED 자동 종료를 보장하지 않음. 일반 취소 API와 승인된 운영 복구를 분리 제안. API/상태 이름은 확정 전.
- 위험 요소: 모든 실패에서 즉시 자동 재시도 요구는 현재 직접 Firebase SDK 구조만으로 충족 불가하며 별도 설계 필요. 구 클라이언트 상태 호환성과 실제 앱 구현 별도.
- 예상 밖 변경: 없음. 코드/계정/배포/Jira 변경 없음, 기존 기록 보존.
- 다음 작업: 구현 요청 시 문서의 범위 및 종료 증거 요건을 기준으로 계약 확정 후 진행.

## 2026-10-01 — SNS 취소·복구 상세 계획 turn 기록 보완

<!-- codex-turn:01a0f6a8-556f-7661-94db-2e163c63943c -->

- 브랜치: develop.
- 작업 목표: 상세 수정 계획 설명의 현재 turn 식별자 기록.
- 변경 파일: docs/contracts/provider-link-cancel-recovery-plan.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 서버 상태·취소/실패 보고/작업 복원 API 제안, 인증·감사·원자적 복구 조건, 프론트 UX, 경합 테스트와 단계 배포 계획 작성. 애플리케이션 구현 없음.
- 실행한 테스트와 결과: 현재 코드 및 운영 계약 대조, git diff --check 통과. 문서 변경만 수행하여 Gradle 미실행.
- 유지한 계약: 불명 STARTED 자동 해제 금지, SDK 실행 허가 재부여 금지, 개인정보·비밀값 비기록.
- 결정사항: 준비 단계 취소와 진행 단계 취소 요청을 구분하며 모든 오류의 즉시 자동 재시도는 보장하지 않음.
- 위험 요소: Firebase 지연 실행과 DB 상태 간 원자성 부재, 실제 앱 및 구 클라이언트 호환성 검증 필요.
- 예상 밖 변경: 없음. 기존 기록 보존, 외부 변경 없음.
- 다음 작업: 구현 요청 후 제안 계약과 증거 요건 확정 및 서버·클라이언트 회귀 테스트 구현.

## 2026-10-01 — Kakao 프론트 Firebase OIDC 설정 안내

<!-- codex-turn:01a0f6ad-a68e-7043-8937-365571ea4c7d -->

- 브랜치: develop.
- 작업 목표: 이전에 안내한 카카오 프론트 플랫폼별 설정과 토큰 교환 계약 재정리.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 저장소 프론트 가이드와 기존 작업 기록 확인. oidc.kakao 공통 provider ID, Firebase SDK 인증 및 Firebase ID Token의 Identity exchange 전달, Android/iOS 각 Firebase 앱 구성과 브라우저 인증 후 앱 복귀 설정·실기기 검증 안내. Kakao Native SDK 직접 토큰 교환과 구분, Client Secret 앱 포함 금지.
- 실행한 테스트와 결과: 계약/이전 기록 읽기, git diff --check 수행. 설명만 수행하여 실행 테스트 미실행.
- 유지한 계약: 카카오 원본 Access Token을 Identity에 전달하지 않음, 공통 provider ID 및 기존 JWT/API 유지, 비밀값 비기록.
- 결정사항: 프론트 프레임워크/SDK 버전 미확인으로 정확한 플랫폼 코드와 callback scheme 값은 단정하지 않음.
- 위험 요소: Chrome 성공은 모바일 앱 복귀 검증이 아님. 실제 Android/iOS 구성은 이번에 직접 조회하지 않음.
- 예상 밖 변경: 없음, 기존 문서 변경 보존. 외부 설정/코드 변경 없음.
- 다음 작업: 프론트 기술 스택에 맞춰 SDK별 설정·로그인·취소·재인증·앱 복귀 체크리스트 구체화.

## 2026-10-02 — 취소·실패 종료 수정 목적 재확인

<!-- codex-turn:01a0fa59-82cf-7150-860b-d13d6caccb2d -->

- 브랜치: develop.
- 작업 목표: 취소/실패 상태 추가와 작업 잠금 해제의 관계 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 안전한 종료가 확인된 연결 작업을 취소/실패 종료로 기록하고 해당 잠금만 해제하여 재시도를 허용하는 계획임을 재확인. 클라이언트 실패 보고나 시간 경과만으로 해제하지 않으며 결과 불명은 보호 유지.
- 실행한 테스트와 결과: 기존 계획 근거 설명 및 git diff --check 통과. 코드 변경 없어 실행 테스트 미실행.
- 유지한 계약: 기존 SNS/전화번호 및 대상 제공자 보호 유지, 비밀값 비기록.
- 결정사항: 상태 enum 추가만이 아니라 조건부 종료 처리와 새로고침 복구까지 포함하는 계획. 구현 미착수.
- 위험 요소: 이전 SDK 실행 종료가 불명인 경우 즉시 재시도를 보장하지 못함.
- 예상 밖 변경: 없음. 기존 기록 보존, 외부 변경 없음.
- 다음 작업: 구현 요청 후 종료 증거와 트랜잭션/API 계약 확정.

## 2026-10-02 — SNS 취소·실패 복구 구현 계획서 정식 보완

<!-- codex-turn:01a0fa77-8925-78f1-9086-0f668e6c3033 -->

- 브랜치: develop.
- 작업 목표: 사용자 요청에 따라 기존 초안을 구현 범위·작업 분할·완료 기준이 있는 수정 계획서로 보완.
- 변경 파일: docs/contracts/provider-link-cancel-recovery-plan.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 범위/제외 항목, 상태 전환표, nullable 데이터 필드 제안, guard의 STARTED 검사와 원자 종료 필요성, API 멱등성, 변경 대상/작업 순서, 완료 체크리스트, 호환 배포 및 중단 기준 추가. 기존 상세 근거/테스트 표 보존.
- 실행한 테스트와 결과: 기존 계획 전체 읽기, ProviderChangeGuard 상태 조회 및 테스트 파일 위치 확인, git diff --check 수행. 문서만 수정하여 Gradle 미실행.
- 유지한 계약: 불명 원격 실행 보호 유지, 상태 조회의 SDK 허가 재부여 금지, owner JWT 기준, 기존 SNS/전화번호 보존, 비밀값 비기록.
- 결정사항: 설계 제안과 실제 구현을 구분. STARTED 종료 증거 부족 시 즉시 재시도 보장하지 않음. 코드/배포/Jira 변경 없음.
- 위험 요소: 실제 앱 저장소 별도, 복구 증거/보존/폴링 정책 확정 필요. 새 enum의 구 서버 호환 확인 필요.
- 예상 밖 변경: 없음. 기존 미커밋 문서 변경 보존.
- 다음 작업: 계획 검토 후 구현 요청 시 계약·운영 복구 권한 확정, 서버/테스트 도구 회귀 테스트부터 구현.

## 2026-10-02 — TMI-136 하위 SNS 취소·복구 이슈 생성안 준비

- 브랜치: develop.
- Jira: TMI-136
- 작업 목표: 사용자 요청에 따른 수정 계획의 하위 Jira 이슈 생성 준비.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 공식 Atlassian MCP로 TMI-136 조회, sns 로그인 에픽 확인. SNS 연결 취소·실패 종료 및 재시도 복구 제목과 계획 기반 범위/완료 조건 초안 준비.
- 실행한 테스트와 결과: Jira 부모 읽기 확인, git diff --check 수행. 코드 변경 없어 실행 테스트 미실행.
- 유지한 계약: Jira 생성 전 내용 제시 및 승인 규칙 유지, 비밀값/개인정보 비기록.
- 결정사항: 이슈 생성/댓글/상태 변경 미실행. 내용 승인 대기.
- 위험 요소: STARTED 불명 결과 자동 해제 제외 및 조건부 운영 복구 범위를 이슈에도 명시해야 함.
- 예상 밖 변경: 없음, 기존 기록 보존.
- 다음 작업: 승인 후 부모 TMI-136 아래 이슈 생성 및 결과 기록. 별도 Jira 댓글은 등록하지 않음.

## 2026-10-02 — Jira 생성안 승인 대기 turn 기록 보완

<!-- codex-turn:01a0fa79-87af-7850-9734-7f823a000bbb -->

- 브랜치: develop.
- Jira: TMI-136
- 작업 목표: 하위 이슈 생성 준비 작업의 현재 turn 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 공식 Atlassian MCP로 부모 에픽 조회 후 제목·범위·완료 조건을 사용자에게 제시. 생성 승인은 대기 중.
- 실행한 테스트와 결과: 부모 이슈 읽기 확인 및 git diff --check 통과. 코드 변경 없어 실행 테스트 미실행.
- 유지한 계약: Jira 변경 전 승인, 비밀값 비기록.
- 결정사항: Jira 생성/댓글/상태 변경 없음. 댓글 목적 및 변경 상태 해당 없음.
- 위험 요소: 승인 전 Jira 변경 금지, 불명 STARTED 자동 해제 제외 유지.
- 다음 작업: 사용자 내용 승인 후 TMI-136 하위 이슈 생성.

## 2026-10-02 — TMI-191 SNS 취소·실패 복구 이슈 생성

- 브랜치: develop (이슈 생성만 수행, 구현 브랜치 변경 없음).
- Jira: TMI-191
- 작업 목표: 승인된 수정 계획을 TMI-136 에픽 하위 작업으로 생성.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md, docs/contracts/provider-link-cancel-recovery-plan.md.
- 구현 내용: 공식 Atlassian MCP로 TMI 작업 유형 확인 후 TMI-191 생성. 제목/범위/보안 경계/완료 조건/계획서 경로 등록, 재조회로 parent=TMI-136 확인.
- 실행한 테스트와 결과: 생성 응답과 부모/상태 재조회 확인, git diff --check 수행. 코드 변경 없어 실행 테스트 미실행.
- 유지한 계약: 사용자 사전 내용 승인에 따라 생성, 비밀값 비기록, 코드/배포/commit/push 변경 없음.
- 결정사항: 생성 승인 있음. 초기 상태 해야 할 일, 별도 상태 전환/댓글 등록 없음. 추가 댓글 목적 해당 없음.
- 위험 요소: 실제 구현 미착수, 종료 증거 및 정책 상세 확정 필요.
- 예상 밖 변경: 없음. 기존 문서 변경 보존.
- 다음 작업: 구현 요청 시 TMI-191 재조회 후 계획과 완료 조건에 맞춰 구현. Jira 댓글 초안: 계획 기반 이슈 생성 및 부모 확인 완료, 코드 변경/실행 테스트 없음, 구현과 실환경 검증 대기(미등록).

## 2026-10-02 — TMI-191 생성 turn 기록 보완

<!-- codex-turn:01a0fa7a-bd43-77f2-8dbd-802099b41c68 -->

- 브랜치: develop.
- Jira: TMI-191
- 작업 목표: 승인된 Jira 생성 작업의 현재 turn 식별자 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md, docs/contracts/provider-link-cancel-recovery-plan.md.
- 구현 내용: 사용자 승인 후 공식 Atlassian MCP로 TMI-191 생성, 부모 TMI-136 및 초기 상태 해야 할 일 재조회 확인. 계획서에 이슈 연결 기록.
- 실행한 테스트와 결과: Jira 생성/부모 조회 확인, git diff --check 통과. 코드 변경 없어 실행 테스트 미실행.
- 유지한 계약: 승인된 생성 범위만 수행, 비밀값 비기록, 코드/배포 변경 없음.
- 결정사항: 생성 승인 있음. 별도 댓글/상태 전환 없음, 추가 댓글 목적 해당 없음.
- 위험 요소: 구현 및 실환경 검증 미착수.
- 예상 밖 변경: 없음. 기존 기록 보존.
- 다음 작업: 구현 요청 시 이슈 재조회 후 범위/완료 조건 기준으로 진행.

## 2026-10-02 — TMI-191 수정 계획 사용자 관점 설명

<!-- codex-turn:01a0fa7e-fd08-78f0-815e-0cdf27555058 -->

- 브랜치: develop.
- Jira: TMI-191
- 작업 목표: SNS 연결 실패·취소 복구 계획을 실제 사용자 사례로 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 진행 중 잠금이 실패 후 남는 현재 문제, 확정 종료 상태/조건부 잠금 해제, 준비 단계 취소와 실행 후 결과 확인 구분, 새로고침 복원, 감사 기록 및 운영 복구 한계를 쉬운 표현으로 설명.
- 실행한 테스트와 결과: 계획서 요약 재확인 및 git diff --check 통과. 설명/기록만 수행하여 Gradle 미실행.
- 유지한 계약: 결과 불명 자동 해제 금지, 기존 회원/SNS/전화번호 보존, 비밀값 비기록.
- 결정사항: 아직 구현 전이며 모든 STARTED 실패의 즉시 자동 재시도는 보장하지 않음.
- 위험 요소: 원격 실행 종료를 확인하지 못한 경우 담당자 복구가 필요할 수 있음.
- 예상 밖 변경: 없음. 기존 기록 보존, 코드/외부/Jira 변경 없음.
- 다음 작업: 구현 요청 시 TMI-191 최신 내용 조회 후 계획 범위와 완료 조건에 따라 진행. Jira 댓글 초안은 설명 완료·구현 미착수·불명 결과 보호 유지(미등록).

## 2026-10-02 — TMI-191 진행 작업 조회 방식 설명

<!-- codex-turn:01a0fa81-fa4b-7df0-ac6f-f6d0855a4a29 -->

- 브랜치: develop.
- Jira: TMI-191
- 작업 목표: 새로고침 후 진행하던 연결 작업을 찾는 방법 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 기존 서버 작업 저장 및 원 요청 ID status 조회, 제안된 최소 메타데이터 보존과 인증된 회원 기준 pending 조회를 구분. 원 ID 해시 조회와 STARTED slot 대조, 여러 PREPARED 후보의 임의 최신 선택 금지, 상태 조회가 SDK 재실행 권한이 아님을 설명.
- 실행한 테스트와 결과: 기존 분석/계획 근거 사용 및 git diff --check 통과. 설명만 수행하여 실행 테스트 미실행.
- 유지한 계약: userId는 인증에서 도출, 다른 사용자 작업 노출 금지, 토큰 원문 복구 메타데이터 저장 금지.
- 결정사항: 로그인 세션 유실 시 재인증 후 본인 작업 조회. pending 및 영속 메타데이터 복원은 아직 계획.
- 위험 요소: 원격 SDK 팝업/실행 자체를 복원하는 기능이 아니며 불명 상태 중복 실행 금지.
- 예상 밖 변경: 없음. 코드/계정/Jira 변경 없음.
- 다음 작업: 구현 요청 시 status/pending 인증·다중 후보·세대 검증 테스트 추가. Jira 댓글은 등록하지 않음.

## 2026-10-02 — TMI-191 SNS 연결 취소·실패 및 작업 복구 구현

<!-- codex-turn:01a0fa83-a6ba-7ef2-aec8-0b8b89fbbb92 -->

- 브랜치: codex/TMI-191-provider-link-recovery (승인된 브랜치 생성).
- Jira: TMI-191. 구현 전 공식 Atlassian MCP로 설명/완료 조건 재조회. Jira 수정/댓글/상태 전환 없음. 댓글 초안은 구현 계약에만 작성(미등록).
- 작업 목표: 실패한 SNS 연결이 불명 상태를 잃지 않도록 취소/실패 의도 기록, 본인 작업 복원, 승인된 안전한 종료 및 재시도 지원.
- 변경 파일(서버): src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/{ProviderChangeProperties,ProviderLinkAttempt,ProviderLinkController,ProviderLinkService}.java, src/main/java/web/tosunsaeng/identity/global/config/IdentityOpenApiExamples.java, src/main/resources/application.yml.
- 변경 파일(테스트/도구): src/test/java/web/tosunsaeng/identity/domain/auth/providerchange/{ProviderChangeTests,ProviderLinkHttpTests}.java, src/test/java/web/tosunsaeng/identity/OpenApiSharingTests.java, scripts/provider_link_recovery.py, scripts/test_provider_link_recovery.py.
- 변경 파일(문서): docs/contracts/provider-link-cancel-recovery-plan.md, docs/contracts/provider-link-recovery-operations.md, docs/contracts/frontend-firebase-auth-integration-guide.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 별도 로컬 테스트 도구 변경: /Users/msde76/tosunsaeng-integration-test/{provider-link.mjs,provider-link.test.mjs,app.js,index.html,server.mjs,server.test.mjs}. /tmp/tmi-191-ui.U6bidJ에서 패치/검증 후 승인된 정확한 파일 복사로 반영. 실행 프로세스 재시작/브라우저 새로고침/실제 로그인 없음.
- 구현 내용: CANCELLED/FAILED enum 및 감사 시각/allowlist 실패 사유 추가. PREPARED 취소는 version 경합 검사로 종료, STARTED 취소/실패 보고는 slot/block 유지 및 ACTION_REQUIRED 응답. 인증된 pending은 실제 활성 slot 우선, 미만료 PREPARED 최대 20개와 hasMore 반환. 조회로 SDK 실행권한 재부여 없음.
- 구현 내용(복구): 공개 관리 API 없이 dry-run 기본 운영 스크립트 제공. 별도 승인 참조/SDK 종료 확인/동일 digest/원격 재조회/정확한 세대·버전·slot 대조 후 snapshot-majority 트랜잭션으로 감사 백업 및 확정 종료. block/floor/epoch 유지, 원문 예외 출력·맹목적 재시도·자동 삭제 금지. 실제 도구 실행 안 함.
- 구현 내용(로컬): sessionStorage에 requestId/attempt/target만 저장, 취소/실패 보고/작업 복원 및 다중 후보 선택 추가. SDK 대기 중 취소와 지연 응답 권한 부활 방지. 새로고침으로 잃은 SDK 실행을 재개하지 않으며 완료 불명은 운영 확인으로 안내.
- 테스트: 최종 ./gradlew clean test 성공 — 1,030 tests, 실패/오류/skip 0. 중간 전체 실행의 OpenAPI 경로 개수/신규 응답 예시 실패는 경로 기대값 28/27 및 실제 예시 추가로 수정 후 targeted/full 재검증. Python unittest 6개 통과. 승인된 실제 로컬 도구 전체 node 테스트 35개 통과. git diff --check 통과.
- 유지한 계약: UUID 주체/RS256/JWKS/audience 및 정상 SNS 연결·전화번호·기존 회원 보존. 토큰/비밀번호 비기록. Learning Core 도메인 코드를 Identity에 추가하지 않음. 사용자 제출 실패/팝업 닫힘/시간 경과만으로 잠금 해제 금지.
- 결정사항: 신규 FIREBASE_PROVIDER_LINK_RECOVERY_ENABLED 기본 false. 새 enum 호환 전체 서버 배포 후 별도 활성화. STARTED의 모든 실패를 자동 즉시 재시도하도록 만들지 않음. commit/push/배포/외부 계정 변경 없음.
- 위험 요소: 실제 Mongo rollback/경합·Firebase 원격 복구·모바일 미검증, 원격 SDK 종료는 운영자 증거 필요. 도구는 tenant 없음/oidc.kakao만 지원. 감사 collection 보존 정책 및 접근 제한, pending 대규모 인덱스/explain 확인 필요. 로컬 gateway 재시작 전 신규 경로 미적용.
- 예상 밖 변경: 없음. 작업 전 존재하던 WORKLOG/CURRENT_STATE/계획서 변경을 보존하고 이번 기록만 추가·갱신. 별도 로컬 프로젝트 수정은 요청 범위와 승인된 경로에 한정.
- 다음 작업: 사용자가 commit/push한 뒤 호환 배포/플래그 활성화 별도 승인, gateway 재시작 및 실제 취소/복원/승인 복구 smoke. 프론트에는 구현·운영 계약을 전달하고 실제 계정 복구는 개별 승인 후 수행.

## 2026-10-02 — Guest merge 오류 분리 최종 검증·EOF 기록

<!-- codex-turn:01a0fb71-09af-7732-b79f-40fcaec7ffea -->

- 브랜치: develop. 이번 요청에 지정된 Jira 없음, Jira 변경 없음.
- 작업 목표: 승인된 Guest merge 대상 탈퇴·정지 오류 분리 구현 및 검증 완료.
- 변경 파일: AuthErrorStatus, FirebaseGuestMergeTargetResolver, FirebaseGuestMergeTransactionService, FirebaseExchangeController, IdentityOpenApiExamples, 해당 resolver/transaction/controller 테스트, OpenApiSharingTests, 프론트 Firebase 통합 가이드, WORKLOG/CURRENT_STATE.
- 구현 내용: 최초 및 저장 직전 대상 검사에서 WITHDRAWN/SUSPENDED를 각각 403 GUEST_MERGE_TARGET_WITHDRAWN / GUEST_MERGE_TARGET_NOT_ACTIVE로 분리. Swagger 오류 예시/프론트 처리 기준/HTTP 및 후속 저장 중단 회귀 테스트 추가.
- 실행한 테스트와 결과: 최종 ./gradlew clean test 1,045 tests 성공(failures/errors/skipped 0), git diff --check 통과. 첫 테스트의 잘못된 가짜 Guest hash fixture를 수정하고 전체 재검증 완료. 실제 외부 Provider/Atlas 호출 없음.
- 유지한 계약: source CAS 충돌 및 target 소유권 충돌, withdrawal cleanup 기존 우선순위, 세션 fence/원자 트랜잭션, JWT/요청/성공 응답 유지. 새 환경변수/엔티티/멱등 응답/PROCESSING 상태 없음.
- 결정사항: source 충돌을 첫 요청 처리 중/완료로 단정하지 않음. 프론트는 신규 403 자동 재시도 중단, 기존 conflict는 상태 재확인. 모든 탈퇴가 새 코드로 반환되는 것은 아님.
- 위험 요소: 실DB 동시성/모바일 E2E 미검증. 배포 전 프론트 코드 분기/기존 cleanup 오류 대응 및 전체 서버 호환 배포 확인 필요.
- 예상 밖 변경: 이번 범위 밖 수정 없음. 기존 WORKLOG 누적 미커밋 기록과 앱 전환 조사 문서 변경 보존. 배포/commit/push 미수행.
- Jira 댓글 초안(미등록): 대상 탈퇴·정지 오류 분리 및 소스/문서/테스트 갱신, 전체 1,045 tests 통과. 실DB/모바일 QA 필요.
- 다음 작업: 사용자 diff 검토/직접 commit·push, 프론트 반영 후 배포 및 실제 병합 QA.

<!-- codex-turn:01a0fc3f-f94b-7b73-822d-2fe242d0fb61 -->

## 2026-10-02 병합 API 요청 형식 안내
- 브랜치: develop. 작업 목표: 현재 Guest merge API 경로 및 인증 전달 방법 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 확인 내용: FirebaseExchangeController 및 FirebaseGuestMergeRequest에서 POST /api/v1/auth/firebase/guest/merge, Guest Bearer 인증, 기존 MEMBER의 fresh firebaseIdToken 본문 확인. 성공 시 대상 MEMBER 토큰 반환.
- 테스트: 코드 읽기 및 git diff --check. 안내 작업으로 Gradle/실API 미실행.
- 유지 계약/결정사항: source는 Guest JWT로 결정하며 임의 userId를 요청하지 않음. prepare MERGE_REQUIRED 뒤 병합 흐름 안내.
- 위험 요소: 실제 배포 상태는 이번에 조회하지 않음. 기존 코드/문서 변경 보존, 외부 상태 무변경.
- 다음 작업: 프론트 요청 형식 반영. 배포/commit/push/Jira 변경 없음.

## 2026-10-02 Guest merge 오류 카카오톡용 안내
- 브랜치: develop. 작업 목표: 병합 API의 주요 응답 코드와 대응을 복사용 텍스트로 제공.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 확인 내용: merge 서비스/대상 resolver/transaction, Firebase 검증기, withdrawal gate, 공통 예외 코드 대조. 새 대상 WITHDRAWN/NOT_ACTIVE 분리와 기존 CONFLICT의 의미 구분. 성공은 Identity 병합 완료이며 LC 비동기 이전 완료와 구분.
- 테스트: 정적 코드 확인 및 git diff --check. 설명 작업으로 실행 테스트/실API 호출 없음.
- 유지 계약/결정사항: message 아닌 code 분기, 응답 유실 시 무조건 재병합 금지. 개인정보/토큰 비기록.
- 위험 요소: 최신 코드 기준이며 이번 배포 여부는 미확인. 주요 코드 안내로 전체 인프라/보안 오류를 망라하지 않음.
- 다음 작업: 프론트 오류 처리 적용 및 배포 버전 확인. 외부 전송/계정 변경/commit/push 없음.

<!-- codex-turn:01a0fc41-2e10-7e63-983e-d91ec9c684c2 -->

### 2026-10-02 Guest merge 오류 안내 작업 기록 보완
- 브랜치: develop. 작업 목표: 병합 API 주요 오류 안내의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 앞선 코드 대조 및 카카오톡 복사용 안내 결과 유지. 성공/인증/병합 조건/일시 장애를 구분하고 새 오류의 배포 확인 필요성을 명시함.
- 테스트: git diff --check. 문서 보완만 수행하여 실행 테스트 없음.
- 유지 계약/결정사항: code 기반 분기, 응답 유실 시 무조건 재병합 금지, 비밀값 비기록. 기존 기록 및 다른 작업 변경 보존.
- 위험 요소: 실배포 확인 및 전체 보안 오류 망라 검증은 미수행.
- 다음 작업: 필요 시 프론트 적용 및 배포 버전 확인. 외부 변경/commit/push 없음.

<!-- codex-turn:01a0fc45-f9e6-7b33-b2d1-9f8873ca01b6 -->

## 2026-10-02 인증 장애 및 설정 문의 접수 API 제안
- 브랜치: develop. 작업 목표: 사용자 문의 DB 저장과 Slack/Discord 알림 연동 가능성 및 설계 기준 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 구현 없이 제안만 제공. 인증/비인증 접수 구분, DB 저장과 알림 outbox의 원자적 접수, 비동기 제한 재시도 및 실패 보존, 요청 중복 방지, 최소 진단정보/개인정보 보호 권고.
- 테스트: git diff --check. 설계 질의로 제품 코드/외부 API 테스트 미수행.
- 유지 계약: 클라이언트 userId 불신, 인증된 사용자만 서버에서 식별, 비밀값/전체 요청 로그 수집 금지. 무인증 문의가 인증 오류를 우회하지 않음.
- 결정사항: 문의 저장 성공과 알림 성공을 분리하며 알림 실패로 접수 실패를 반환하지 않는 방향 권고. 일반 고객지원의 서비스 소유권은 구현 전 결정 필요.
- 위험 요소: 공개 접수 스팸, 개인정보 외부 전달, 중복 알림, 재시도 불가능한 인증 오류의 반복 처리. 기존 기능 구현 여부는 조사하지 않았으며 제안 경로는 미구현.
- 다음 작업: 담당 서비스/알림 채널/회신 및 보존 정책 확정 후 계획·구현 승인. 외부 상태 변경/commit/push 없음.

## 2026-10-02 — PROVIDER_RELINK_REQUIRED 의미와 프론트 재연결 순서 설명

<!-- codex-turn:01a0fc5e-3ef7-7d01-8815-078a2d59a1ff -->

- 브랜치: develop. 이번 요청에 지정된 Jira 키 없음.
- 작업 목표: RELINK_REQUIRED의 발생 이유 및 기존 SNS 재인증/link prepare 관계를 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. ProviderChangeGuard, ProviderLoginRegistrationService, ProviderLinkService.prepare/remainingProof 및 프론트 가이드 읽기 확인.
- 실행한 테스트와 결과: 소스/계약 읽기 및 git diff --check. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 인증된 MEMBER 및 동일 Firebase binding, 기존 승인 SNS 증명, 대상 SDK link는 최초 start 허가 후, 소유권/차단/floor 유지. 원격 선연결을 소급 승인하지 않음.
- 결정사항: RELINK_REQUIRED는 SNS 로그인 허용 상태/명시 재연결 문제이며 단순 최근 재인증 부족 오류와 다름. 일반 재연결에는 기존 승인된 다른 SNS 재인증 후 MEMBER 인증과 prepare 요청 필요. prepare에는 기존 SNS proof, SDK 연결 후 complete에는 대상 SNS 재인증 proof 사용. 로그인 중이라면 기존 MEMBER 인증 사용 가능하나 최신 proof 필요. 남은 승인 수단 없거나 원격/서버 상태 불일치·작업 불명은 자동 우회하지 않음.
- 위험 요소: 모든 RELINK_REQUIRED가 prepare 한 번으로 해결되지 않음. Google/Apple 같은 UID 최초 등록은 지원 조건에서 자동 처리되므로 미등록 전체를 재연결로 단정하지 않음. 실제 프론트/배포 설정/E2E 미조회.
- 예상 밖 변경: 없음. 코드/환경변수/배포/커밋/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): SNS 재연결 오류 의미 및 기존 SNS 인증→prepare/start→대상 연결/인증→complete 순서 설명, 소스/계약 확인만 수행.
- 다음 작업: 프론트에 오류와 재인증의 차이 및 이미 진행 중인 작업 status/pending 처리 계약 전달.

## 2026-10-02 — 재연결 시 기존 SNS 확인 방법 설명

<!-- codex-turn:01a0fc62-31a7-7bf3-a431-64e79eb6f511 -->

- 브랜치: develop. 지정된 Jira 키 없음.
- 작업 목표: 프론트가 재연결에 사용할 기존 SNS를 어떻게 식별하는지 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. FirebaseAuthMethodsSyncService/Response 및 프론트 가이드의 linkedProviders/프로필 provider 계약 확인.
- 실행한 테스트와 결과: 소스 읽기 및 git diff --check. 설명 작업으로 Gradle 미실행.
- 유지한 계약: sync MEMBER 인증+동일 Firebase proof 필요, 기존 연결 승인 확인만 수행. provider 분류로 SNS 추측/공개 계정 탐색/다른 UID 자동 병합 금지.
- 결정사항: 기존 로그인 수단 이력과 현재 서버 연결 SNS 목록을 구분. 로그인된 회원은 sync 성공 응답 linkedProviders 사용, Firebase providerData와 마지막 로그인 로컬 기록은 힌트만 사용. 로그인 전 RELINK_REQUIRED 응답에는 기존 SNS 정보가 없어 prepare 즉시 호출 불가, 사용자 기존 SNS 선택/인증 후 회원/binding 확인 필요. 이메일/전화로 기존 SNS 계정 임의 추정하지 않음.
- 위험 요소: sync 자체도 승인 proof/서버 상태 검사가 필요하여 재연결 오류 중 항상 조회 가능한 수단은 아님. 공개 연결 목록 API 미제공. 실제 프론트 캐시/로그인 이력 저장 여부 미조회.
- 예상 밖 변경: 없음. 코드/외부 설정/배포/커밋/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): 기존 SNS 식별은 인증된 sync linkedProviders 사용, 로그인 전 오류만으로 목록 판별 불가 및 힌트/승인 근거 구분 설명.
- 다음 작업: 프론트 로그인 상태별 기존 수단 선택 UI 및 sync/복구 실패 안내 확인.

## 2026-10-02 — Guest merge의 PROVIDER_RELINK_REQUIRED 가능 여부 확인

<!-- codex-turn:01a0fc76-7443-7350-8eb6-7040026a0e2c -->

- 브랜치: develop. 지정된 Jira 키 없음.
- 작업 목표: RELINK_REQUIRED가 Guest merge에서는 발생하지 않는지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. GuestMergeService의 GUEST_MERGE proof 검증과 FirebaseAdminAuthenticationVerifier의 providerChanges.validatePrincipal 호출 조건, Guard의 block/미등록 subject 검사 확인.
- 실행한 테스트와 결과: 소스 읽기 및 git diff --check. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 원 Guest source와 MEMBER target 증명 분리, 차단된 SNS 우회 금지, LOGIN_EXCHANGE 최초 등록 예외의 merge 확대 금지.
- 결정사항: provider guard 설치 환경에서 merge도 해당 오류 발생 가능. 대상 MEMBER에 허용되지 않은 SNS 인증이 이유이며 Guest 자체의 SNS 연결 문제가 아님. 원 Guest 인증 보존 후 대상의 기존 승인 SNS proof로 merge 수행 가능, 실제 SNS 재연결은 MEMBER 인증이 필요한 별도 흐름. Guest bearer로 link/prepare 자동 호출하지 않음.
- 위험 요소: 실제 배포 guard 설정/요청 상태 미조회. 기존 승인 수단을 사용할 수 없으면 자동 가입/병합/선연결 우회하지 않고 복구 안내 필요.
- 예상 밖 변경: 없음. 코드/환경변수/외부 설정/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): Guest merge provider guard 오류 발생 가능 및 원 Guest 인증 보존/대상 승인 SNS 재인증 분기 설명.
- 다음 작업: 프론트 Guest merge 오류 목록에 RELINK_REQUIRED 유지 및 MEMBER 재연결과 Guest 병합 UI 분리 확인.

## 2026-10-02 — 로그인 전 대상 SNS 목록 부재와 재연결 안내 정정

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 대상 MEMBER 승인 SNS를 프론트가 모르는 상황에서 기존 설명의 가정을 정정.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. RELINK_REQUIRED는 승인 SNS 목록을 제공하지 않고 linkedProviders sync는 MEMBER 인증이 필요한 현 계약 재확인.
- 실행한 테스트와 결과: 소스 읽기 및 git diff --check. 설명만 수행하여 Gradle 미실행.
- 유지한 계약: 원 Guest 인증 보존, 승인되지 않은 SNS로 MEMBER 권한 발급/병합 우회 금지, 다른 계정 자동 연결 금지.
- 결정사항: 대상 SNS를 이미 알고 있다는 앞선 예시 가정을 명시적으로 정정. 현재 API만으로 구체 SNS를 자동 선택/안내할 수 없음. 다른 기존 SNS 재인증은 신규 연결이 아니라 소유권 증명이며 사용자 기억/로컬 힌트는 보장되지 않음. 구체 안내에는 검증된 Firebase proof 기반 제한 복구 정보/작업 계약의 별도 보안 설계 필요.
- 위험 요소: 공개 이메일 기반 연결 수단 조회는 계정 탐색 위험, Firebase providerData만으로 Identity 허용 여부 단정 불가. 기존 SNS 기억하지 못하는 사용자 복구 UX 공백 존재.
- 예상 밖 변경: 없음. 코드/API/외부 설정/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): Guest merge RELINK_REQUIRED의 대상 승인 SNS 조회 공백 및 기존 설명 정정, 제한 복구 계약 추가 검토 필요.
- 다음 작업: 사용자 요청 시 복구 요구사항/정보 노출 범위/인증 정책을 확정한 뒤 계약 설계, 구현은 미승인.

## 2026-10-02 — 대상 SNS 복구 정보 공백 설명 기록 보완

<!-- codex-turn:01a0fc78-2b7f-7f21-ba7a-6f7e5d86f08e -->

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 이번 설명 작업의 정확한 현재 식별자 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. Guest 상태에서 대상 MEMBER 연결 목록을 조회할 수 없는 복구 계약 공백과 앞선 안내의 가정 정정 기록 보완.
- 실행한 테스트와 결과: git diff --check. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 승인되지 않은 SNS로 MEMBER 인증/병합 우회 금지.
- 결정사항: 제한 복구 정보 제공은 별도 보안 설계 필요하며 현재 API로 대체 SNS 자동 안내 불가.
- 위험 요소: 기존 로그인 방법을 모르는 사용자 복구 UX 공백. 공개 계정 탐색 API를 임의 추가하지 않음.
- 예상 밖 변경: 없음. 코드/API/외부 설정/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): 대상 승인 SNS 조회 공백 정정 설명 완료, 복구 계약 별도 검토 필요.
- 다음 작업: 사용자 요청 시 복구 계약 설계 검토.

## 2026-10-02 — Guest merge 전용 오류 목록 정리

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: Guest merge에만 정의된 오류와 공통 오류 구분.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. AuthErrorStatus와 전용 코드 발생 위치 확인, 5개 merge 전용 code/status/원인/프론트 대응 요약.
- 실행한 테스트와 결과: 소스 검색 및 git diff --check. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 기존 오류 상태/코드 변경 없음, 원본/대상 및 성공/충돌 구분 유지.
- 결정사항: NOT_ALLOWED, TARGET_CONFLICT, TARGET_WITHDRAWN, TARGET_NOT_ACTIVE, CONFLICT만 전용 목록 포함. RELINK_REQUIRED/ACCOUNT_MERGED_TOKEN_REJECTED/cleanup pending 등 공통 오류는 별도이며 merge에서도 발생 가능.
- 위험 요소: 신규 대상 상태 code의 실제 배포 여부 미조회. 전용 오류 목록은 merge 전체 발생 가능 오류 목록이 아님.
- 예상 밖 변경: 없음. 코드/외부 설정/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): merge 전용 오류 5개 원인/대응 요약, 공통 오류와 분리.
- 다음 작업: 필요 시 프론트에 전용/공통 오류 처리 목록 전달.

## 2026-10-02 — Guest merge 전용 오류 정리 기록 보완

<!-- codex-turn:01a0fc7a-708e-7293-99cc-13260143d218 -->

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 이번 전용 오류 설명의 정확한 현재 작업 식별자 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. merge 전용 오류 5개와 공통 오류 구분 설명 기록 보완.
- 실행한 테스트와 결과: git diff --check 통과. 기록만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 오류 코드/상태 및 인증 계약 변경 없음.
- 결정사항: 전용 목록은 전체 merge 발생 가능 오류 목록과 다름. source/target 상태 및 충돌 대응을 구분.
- 위험 요소: 신규 오류의 실제 배포 여부 미확인.
- 예상 밖 변경: 없음. 코드/외부 설정/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): merge 전용 오류 5개 정리 완료, 공통 오류는 별도 처리 필요.
- 다음 작업: 필요 시 프론트 오류 분기 확인.

## 2026-10-02 — Guest merge 전체 오류 전달용 정리 요청 중단

<!-- codex-turn:01a0fc7b-a86e-7a00-817e-791bf15c8061 -->

- 브랜치: develop.
- 작업 목표: Guest merge 전체 발생 가능 오류를 카카오톡 복사용으로 정리.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 요청 시작 전 중단되어 전체 조사/복사용 결과 작성 미수행. 중단 상태 기록만 추가.
- 실행한 테스트와 결과: git diff --check. 코드 변경 없어 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 코드/API/외부 설정 변경 없음.
- 결정사항: 중단 후 요청 본문 작업 진행하지 않음.
- 위험 요소: 전용 오류 목록은 앞서 확인했지만 공통 오류 포함 전체 목록은 이번 요청에서 미검증.
- 예상 밖 변경: 없음. 배포/commit/push/Jira 변경 없음.
- 다음 작업: 사용자 재개 요청 시 전체 경로 확인 후 복사용 텍스트 작성.

<!-- codex-turn:01a0fc7c-80da-7f10-aa30-97391c14a869 -->

## 2026-10-02 Guest merge 오류 전체 경로 정리
- 브랜치: develop. 작업 목표: merge 전용/공통/Firebase/세션 보호/네트워크 오류를 구분한 복사용 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 확인 내용: merge service/target/transaction, Firebase verifier와 provider guard, SessionSecurityService/RefreshSessionIssuer/UserSessionControl, JwtCurrentUserProvider, 공통 예외 처리 및 owner event capture 경로 대조. 세션 보호 활성 시 SESSION_LOGGED_OUT/SESSION_SECURITY_UNAVAILABLE와 계정 활성 검사 오류도 가능. merge는 enrollment evidence를 요구하지 않아 PHONE/EMAIL_VERIFICATION_REQUIRED를 이 endpoint 목록에 넣지 않음.
- 테스트: 정적 호출 경로 확인 및 git diff --check. 실API/실배포/Gradle 미실행(설명 작업).
- 유지 계약: API code와 외부 HTTP/네트워크 오류 구분, LC 비동기 실패를 merge 동기 응답과 구분, 결과 불명 시 자동 재병합 금지.
- 결정사항: 현재 소스에서 확인되는 오류 목록을 제공하며 환경·검사 순서·동시성에 따른 차이를 명시. 무한 재시도나 내부 재시도 횟수를 보장하지 않음.
- 위험 요소: 배포 버전과 인프라별 응답은 미확인. 임의 외부 오류까지 고정 목록으로 보장할 수 없음.
- 다음 작업: 프론트 code 분기 및 알 수 없는 오류 fallback 적용. 외부 변경/commit/push/Jira 변경 없음. 기존 변경 보존.

## 2026-10-02 — PROVIDER_RELINK_REQUIRED와 SNS 연결 개수 조건 확인

<!-- codex-turn:01a0fc80-a90d-7cd0-b2af-ab9fb71cecc4 -->

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 오류가 SNS 두 개 이상 연결된 대상의 merge에서만 발생하는지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. ProviderChangeGuard의 현재 signInMethod provider 차단/subject 등록 검사 재확인.
- 실행한 테스트와 결과: 소스 읽기 및 git diff --check. 설명 작업으로 Gradle 미실행.
- 유지한 계약: SNS 개수가 아니라 현재 인증 수단의 승인/차단 및 소유권 기준으로 검사, LOGIN_EXCHANGE 전용 최초 등록 예외 유지.
- 결정사항: 최소 두 개 연결 조건 없음. 한 개 상태도 불일치/차단이면 거절 가능, 여러 개 정상 승인 상태면 병합 가능. Guest merge 전용 오류도 아님.
- 위험 요소: 실제 대상의 연결 상태 및 배포 버전 미조회, 마지막 SNS 해제 제한과 불일치 오류 조건은 별개.
- 예상 밖 변경: 없음. 코드/API/외부 설정/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): RELINK_REQUIRED는 연결 개수가 아니라 현재 인증 SNS 허용 여부 조건임을 설명.
- 다음 작업: 프론트는 연결 개수로 오류 발생/복구 여부를 추정하지 않도록 분기 확인.

## 2026-10-02 — 유일 SNS 차단 시 로그인 불가와 마지막 수단 보호 확인

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 승인 SNS 하나가 차단되면 로그인할 수 없다는 사용자 지적에 정상 정책/예외 상태 구분.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. ProviderChangeService.unlink의 remaining.isEmpty 검사 및 PROVIDER_LAST_METHOD 반환이 block 전에 실행됨을 확인.
- 실행한 테스트와 결과: 소스 읽기 및 git diff --check. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 마지막 승인 SNS 해제 금지, 기존 다른 SNS 소유 증명 필요, 차단 상태 자동 우회 금지.
- 결정사항: 정상 흐름에서 SNS 하나뿐인 계정의 유일 수단 차단을 허용하지 않음. 앞선 단일 SNS 차단 예시는 일반 정상 흐름으로 해석하지 않도록 정정. 유일 수단 사용 불가 상태면 다른 SNS 선택으로 해결되지 않으므로 상태 조회/안전한 복구 필요.
- 위험 요소: 실제 계정 상태/외부 Admin 변경/서버와 Firebase 불일치 여부는 미조회. 예외 상태 원인 특정하지 않음.
- 예상 밖 변경: 없음. 코드/외부 설정/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): 마지막 SNS unlink 보호 확인 및 단일 차단 상태를 복구 예외로 구분 설명.
- 다음 작업: 실제 단일 로그인 수단 거절 사례라면 차단/등록/binding 상태를 안전하게 확인 후 복구 설계.

## 2026-10-02 — 마지막 SNS 보호 설명 기록 보완

<!-- codex-turn:01a0fc81-9f1d-7182-8db5-424acfc6f1fd -->

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 이번 마지막 SNS 보호 설명의 정확한 현재 식별자 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 정상 unlink에서 마지막 승인 SNS 해제를 PROVIDER_LAST_METHOD로 거절하며, 단일 수단 차단 예외 상태는 복구 대상임을 설명한 기록 보완.
- 실행한 테스트와 결과: git diff --check 통과. 기록만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 마지막 로그인 수단 보호 및 차단 우회 금지.
- 결정사항: 오류 조건의 개수 독립성과 정상 마지막 수단 보호를 구분.
- 위험 요소: 실제 단일 수단 거절 계정 상태는 미조회.
- 예상 밖 변경: 없음. 코드/외부 설정/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): 마지막 수단 보호 확인/예외 상태 복구 설명 완료.
- 다음 작업: 필요 시 실제 계정 상태 확인 및 안전한 복구 검토.

## 2026-10-02 — SNS 차단 상태의 실제 코드 경로 확인

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 현재 코드에 SNS block을 설정하는 실제 경로가 있는지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. ProviderChangeService.unlink의 block, ProviderLinkService.start의 block 및 complete의 release, AuthMethodChangeControl 상태 갱신 확인.
- 실행한 테스트와 결과: 소스 읽기 및 git diff --check. 설명 작업으로 Gradle 미실행.
- 유지한 계약: 마지막 승인 SNS 해제 금지, 미완료 연결 SNS로 로그인/merge 우회 금지, 명시 completion 전 승인 금지.
- 결정사항: 코드상 block 경로는 존재하며 로그인 실패 자동 차단/외부 SNS 계정 정지와 다른 내부 승인 fence임을 설명. 해제한 SNS와 새 연결 중 SNS에 적용, 완료 시 대상 release. 새 연결 중 Firebase만 반영되고 Identity complete 전이면 RELINK_REQUIRED가 가능.
- 위험 요소: 배포 기능 flag 및 실제 회원 block 상태 미조회. 정상 단일 승인 SNS의 임의 차단을 뜻하지 않음.
- 예상 밖 변경: 없음. 코드/API/외부 설정/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): SNS 내부 block 설정 위치와 외부 계정 차단/자동 로그인 차단의 차이 확인.
- 다음 작업: 필요 시 실제 오류 요청과 미완료 link/unlink 상태를 안전하게 대조.

## 2026-10-02 — SNS 내부 차단 경로 설명 기록 보완

<!-- codex-turn:01a0fc83-4846-73b2-8245-7ba52222832d -->

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 이번 SNS 내부 차단 설명의 정확한 현재 작업 식별자 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 연결/해제 작업의 내부 block과 외부 SNS 제재·로그인 실패 자동 차단을 구분한 설명 기록 보완.
- 실행한 테스트와 결과: git diff --check 통과. 기록만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 미완료 연결 승인 금지 및 마지막 SNS 보호 유지.
- 결정사항: 코드상 block 설정 경로 존재와 실제 배포/계정 적용 여부를 구분.
- 위험 요소: 실제 배포 flag 및 해당 회원 상태는 미조회.
- 예상 밖 변경: 없음. 코드/API/외부 설정/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): SNS block 내부 의미와 설정 경로 확인 완료, 실환경 상태 미조회.
- 다음 작업: 필요 시 실제 오류와 link/unlink 작업 상태 대조.

## 2026-10-02 — PROVIDER_RELINK_REQUIRED 프론트 실행 가능한 대응 정리

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 로그인/Guest merge와 SNS 연결 작업 문맥별 프론트 행동을 명확하게 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 프론트 계약의 status/start/complete 유실 및 로그인 수단 복구 경계 재확인.
- 실행한 테스트와 결과: 계약 읽기 및 git diff --check. 설명 작업으로 Gradle 미실행.
- 유지한 계약: Guest source 인증 보존, MEMBER 전용 prepare, start 허가 없는 SDK 실행 금지, 불명 상태 성공 간주 금지.
- 결정사항: 자동 반복/새 가입/새 Guest 생성 및 code만으로 prepare 호출 금지. 진행 중인 작업이 있으면 기존 작업 상태 기반 복구, SDK 실행 결과가 확인된 경우에만 기존 complete 계약 사용. 로그인/merge에서는 다른 기존 수단 선택 또는 복구 지원 안내, 구체 provider 추정 금지. 현재 응답만으로 자동 원인 분류/완전 복구는 불가.
- 위험 요소: 실제 프론트 작업 보존/계정 상태/배포 recovery flag 미조회. 사용자에게 원인이나 다른 SNS 종류를 단정한 안내를 하지 않음.
- 예상 밖 변경: 없음. 코드/API/외부 설정/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): RELINK_REQUIRED 문맥별 프론트 대응 및 자동 우회 금지 정리, 복구 계약 공백 유지.
- 다음 작업: 필요 시 프론트 분기/안내 문구와 추가 복구 응답 요구사항 검토.

## 2026-10-02 — 재연결 오류 프론트 대응 설명 기록 보완

<!-- codex-turn:01a0fc84-fc41-7a83-85c5-ef14d9d6c09f -->

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 이번 프론트 오류 대응 설명의 정확한 작업 식별자 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 로그인/Guest merge와 진행 중 연결 작업의 대응을 구분한 설명 기록 보완.
- 실행한 테스트와 결과: git diff --check 통과. 기록만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, Guest 인증 유지 및 허가 없는 SDK 재실행 금지.
- 결정사항: 오류만으로 prepare 자동 호출 또는 원인·대체 SNS 추정하지 않음.
- 위험 요소: 현 응답만으로 원인별 자동 복구 불가, 실제 프론트 및 배포 상태 미조회.
- 예상 밖 변경: 없음. 코드/API/외부 설정/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): 문맥별 프론트 대응 및 복구 공백 설명 완료.
- 다음 작업: 필요 시 프론트 분기 검토 및 추가 복구 계약 설계.

<!-- codex-turn:01a0fc90-cc96-77e0-90e9-0a766b820f71 -->

## 2026-10-02 문의 접수 API 구현 구조 제안
- 브랜치: develop. 작업 목표: POST /api/v1/support/inquiries의 요청·저장·알림·실패 처리 설계 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 제품 코드 변경 없음.
- 구현 내용: 미구현 제안으로 인증 선택형 문의 접수, invalid Bearer의 익명 자동 강등 금지, 별도 무인증 접수 허용, 내용 최소 수집/검증/중복 방지, DB 문의와 outbox 원자 저장 및 비동기 알림을 설명. 서버 발급 접수번호와 제한 재시도/실패 상태·알림 중복 가능성 안내.
- 테스트: git diff --check. 설계 설명만 수행하여 실행 테스트 미수행.
- 유지 계약: 사용자 ID는 검증된 인증에서만 도출. 토큰·비밀번호·전체 요청 로그 비수집, Secret은 외부 관리. 사용자 문의 본문은 외부 채널에 기본 미전달.
- 결정사항: 정확한 오류 코드/횟수/보관 기간/담당 서비스는 설계 제안이며 기존 구현 계약으로 주장하지 않음. 일반 문의 서비스 소유권 확정 필요.
- 위험 요소: 익명 스팸/중복 알림/개인정보/운영 조회 권한, webhook 성공 응답 유실 시 정확히 한 번 전달 보장 불가.
- 다음 작업: 서비스 소유권·알림 채널·회신 및 보관 정책 확정 후 구현 승인. 외부 변경/commit/push/Jira 작업 없음. 기존 변경 보존.

## 2026-10-02 — 전화번호 단일 계정과 OTP 기반 연결 SNS 안내 검토

<!-- codex-turn:01a0fcc9-d4ac-7363-8718-a705e6f66a98 -->

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 전화번호 인증으로 연결 계정을 안내하는 사용자 제안의 현 구현/추가 요구사항 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. PhoneIdentity/PhoneIdentityService/PhoneIdentityTransactionService의 활성 alias 소유권 검사 및 PHONE_ALREADY_LINKED, FirebaseVerificationPurpose와 인증 정책 소스 확인.
- 실행한 테스트와 결과: 소스 읽기 및 git diff --check. 설계 검토만 수행하여 Gradle 미실행.
- 유지한 계약: 동일 번호의 타 계정 활성 중복 연결 거절 유지. 원문 번호/토큰 비기록, 임의 userId 요청 금지. 전화 증명으로 기존 SNS block 우회/자동 병합하지 않음.
- 결정사항: 번호 1개와 활성 회원 1개 연결은 현 코드 정책과 부합. 별도 recovery 전용 전화 인증 조회를 도입하면 기존 SNS를 기억하지 못하는 사용자에게 종류만 안내 가능. 최근 전화 OTP proof 서버 검증 후 활성 소유자/승인된 사용 가능 provider만 반환하는 계약 제안이며 기존 API 구현으로 주장하지 않음.
- 위험 요소: Firebase OTP 연결을 다른 UID에 새로 붙여 기존 회원 binding을 변경하는 우회 금지. 번호 재할당/SIM 탈취를 고려해 전화 OTP만으로 자동 로그인/기록 이전/계정 연결 변경 금지. 회원 부재/탈퇴/정지/미완료 보안 작업과 조회 제한 정책 미확정. 실제 배포 phone 활성 여부 및 인덱스 미조회.
- 예상 밖 변경: 없음. 코드/API/외부 설정/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): 전화번호 단일 활성 owner 정책 확인, OTP 기반 로그인 SNS 종류 안내 전용 복구 계약 제안. 실제 구현/보안 정책 추가 확정 필요.
- 다음 작업: 사용자 요청 시 recovery proof/노출 응답/제한/예외 상태 계약을 확정하여 계획 작성. 구현은 별도 요청 필요.

## 2026-10-02 — 전화번호 다중 회원 연결 API 삭제 대상 조사

<!-- codex-turn:01a0fccd-dd32-7661-9395-cb2abc66d700 -->

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 전화번호당 하나의 회원 정책에 맞춰 다중 회원 연결 허용 API 제거 대상을 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 제품 코드 변경 없음. Controller endpoint 및 phone link 호출 경로 확인. 현 API에는 전화번호 연결/변경/공개 rebind가 없고 signup/Guest upgrade는 번호 소유권 검사로 타 회원 중복 연결 거절. SNS link는 기존 MEMBER 인증수단 추가로 다중 userId 생성/동일 번호 공유 API가 아님.
- 실행한 테스트와 결과: 소스/기존 PHONE_ALREADY_LINKED 테스트 및 프론트 계약 읽기 확인, git diff --check. 삭제할 코드 대상이 없으므로 Gradle 미실행. 실제 운영/테스트 DB 미조회.
- 유지한 계약: 전화번호의 단일 활성 userId 소유, 동일 MEMBER의 다중 SNS 허용, signup/Guest upgrade/merge 및 탈퇴 후 release 정책 유지.
- 결정사항: 존재하지 않는 다중 회원 연결 API를 삭제했다고 주장하지 않음. 별도 SNS link를 임의 삭제하지 않으며 제품 API/데이터 변경 없음. 전화 OTP 기반 SNS 안내 복구 API는 여전히 신규 개발 범위.
- 위험 요소: 실제 배포 버전/DB 중복 여부/인덱스 적용 미확인. 사용자가 SNS 여러 개 연결 자체를 금지하려는 뜻이라면 계정 정책 변경이므로 별도 확인 필요.
- 예상 밖 변경: 없음. 기존 기록 변경 보존. 삭제/외부 설정/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): 다중 회원 전화 연결 API 부재와 기존 중복 거절 확인, SNS 인증수단 추가와 회원 추가 구분.
- 다음 작업: 구체적으로 제거하려는 endpoint가 별도로 있으면 사용자 경로 확인 후 조사. 번호 인증 기반 복구 API 설계는 별도 요청 범위로 진행.

## 2026-10-02 — 회원당 SNS 하나 정책 의도 확인 및 변경 범위 조사

<!-- codex-turn:01a0fcdd-2053-7f83-b2ab-a7ae56df9d10 -->

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 사용자의 추가 SNS 연결 금지 의도를 정정 이해하고 필요한 변경 경로 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. ProviderLinkService prepare/start/complete, ProviderLoginRegistrationService 최초 자동 등록, FirebaseSignupService/GuestUpgradeService 다중 SocialIdentity 저장 경로 확인.
- 실행한 테스트와 결과: 소스 읽기 및 git diff --check. 정책 범위 조사로 Gradle 미실행.
- 유지한 계약: 기존 회원/인증수단/보안 block·floor·작업 증거 임의 삭제 금지, 계정 접근 유지, 전화번호 단일 활성 owner 정책 유지.
- 결정사항: 회원당 추가 SNS 금지는 link API 삭제/flag OFF만으로 달성되지 않음. 자동 최초 등록 및 가입·승격 다중 provider 처리까지 변경해야 함. 기존 복수 연결 회원은 유지하면서 신규 추가를 차단하고 진행 중 작업 조회/안전한 복구는 보존하는 방향 권장. 기존 연결 제거/기준 provider 임의 선택은 사용자 승인 없이 수행하지 않음.
- 위험 요소: 이미 복수 SNS가 등록된 회원과 STARTED 원격 작업의 이행 정책 미확정. 기존 최초 자동 등록 기능과 정책 충돌, 프론트 가입/연결 UX 및 전화 OTP 안내 계약도 갱신 필요. 실제 DB 현황 미조회.
- 예상 밖 변경: 없음. 제품 코드/API/외부 설정/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): 추가 SNS 금지 정책 의도 확인, link/auto-registration/signup/upgrade 경로 변경 및 기존 회원/진행 작업 이행 결정 필요.
- 다음 작업: 기존 복수 연결 회원 유지 여부와 진행 중 작업 보존 기준 확인 후 구현 범위를 확정하여 회귀/전체 테스트 수행.

## 2026-10-02 — SNS 추가 연결 prepare와 Guest prepare 구분

<!-- codex-turn:01a0fcde-ecb2-7063-b31c-8c5bc6f1e36c -->

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 추가 SNS 연결 차단이 Guest 승격/병합을 막는지 설명하고 테스트 회원 부재 조건 반영.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. FirebaseExchangeController와 ProviderLinkController의 서로 다른 prepare endpoint 확인.
- 실행한 테스트와 결과: endpoint 소스 읽기 및 git diff --check. 설명 작업으로 Gradle 미실행.
- 유지한 계약: Guest prepare/upgrade/merge 유지, SNS 계정 연결과 Guest 데이터 병합의 경계 구분. 실제 테스트/운영 데이터 삭제 없음.
- 결정사항: 추가 SNS 금지 범위는 providers/link 경로 및 자동 추가 등록이며 guest/prepare 차단 아님. Guest 승격은 단일 SNS 회원으로 전환, Guest merge는 기존 단일 SNS MEMBER로 기록 통합. 사용자 진술 기준 테스트 서버 회원 없음으로 기존 회원 이행 우려 해소하되 실제 DB/진행 작업 검증 사실로 주장하지 않음.
- 위험 요소: 테스트 DB/Firebase 활성 사용자/진행 작업 실조회 없음. 테스트 대상 정책을 기존 운영 서버에 임의 적용하지 않음.
- 예상 밖 변경: 없음. 제품 코드/API/환경변수/외부 설정/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): providers/link/prepare와 guest/prepare 구분 및 Guest 기능 유지 범위 설명, 테스트 회원 부재는 사용자 확인.
- 다음 작업: 신규 단일 SNS 정책 구현 시 추가 등록/가입/승격 검증만 변경하고 Guest 병합 정상 경로 회귀 검증. 별도 전화 OTP 안내 API는 추가 계약 범위.

## 2026-10-02 — Guest/SNS prepare 구분 설명 식별자 보완

<!-- codex-turn:01a0fcde-ecb2-7962-86ad-5f3d9e956eb0 -->

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 이번 설명의 정확한 현재 작업 식별자로 기록 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 제품 코드 변경 없음. SNS 추가 연결용 providers/link/prepare와 Guest 승격용 guest/prepare를 구분한 설명 기록 보완.
- 실행한 테스트와 결과: git diff --check 통과. 기록만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, Guest 승격·병합 정상 기능 유지.
- 결정사항: 회원당 SNS 하나 정책에서 추가 SNS 연결/자동 추가 등록만 차단하는 방향. 테스트 회원 부재는 사용자 진술 기준이며 DB 조회 결과가 아님.
- 위험 요소: 실제 DB/Firebase 및 진행 작업 상태 미조회, 정책 구현·배포 미수행.
- 예상 밖 변경: 없음. 코드/API/외부 설정/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): prepare endpoint 역할 구분 및 Guest 기능 유지 설명 완료.
- 다음 작업: 구현 요청 시 단일 SNS 정책의 가입/로그인/Guest 승격·병합 회귀 검증.

## 2026-10-02 — 단일 SNS 정책 수정 방향 설명

<!-- codex-turn:01a0fcfd-d21d-78c3-98ea-b3d879fef1e3 -->

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 회원당 SNS 하나 정책을 유지하면서 Guest 승격·병합을 보존하는 수정 방향 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 제품 코드 변경 없음.
- 구현 내용: ProviderLinkController, ProviderChangeGuard, FirebaseSignupService, FirebaseGuestUpgradeService, PhoneIdentityTransactionService 경로 확인. 명시 link 및 exchange 자동 추가 등록 차단, 가입·승격의 단일 SNS 검증, Guest 정상 기능 유지 제안.
- 실행한 테스트와 결과: 소스 정적 확인 및 git diff --check. 설명/기록 작업으로 Gradle 미실행.
- 유지한 계약: Guest prepare/upgrade/merge 유지, 기존 승인 SNS의 소유권 검증 유지, 전화번호 단일 활성 owner 유지, 인증수단 block·floor 보안 검사 우회 금지.
- 결정사항: SNS 추가 API 제거만으로 단일 정책 달성 불가. 가입/승격에서 복수 SNS 증명을 임의 선택하지 않고 거절하는 방향 제안(전화 인증은 SNS 개수에 포함하지 않음). 전화 OTP 기반 SNS 종류 안내는 별도 신규 기능이며 로그인/재연결/Guest 병합을 자동 허용하지 않음. 단일 SNS 교체 기능은 이번 범위에서 도입하지 않음.
- 위험 요소: 테스트 회원 부재는 사용자 진술이며 DB/Firebase/진행 link 작업 상태 미조회. 신규 정책 오류 코드·전화 OTP 조회 계약·rate limit 및 번호 재할당 대응은 구현 전 확정 필요. Firebase 원격 연결 자체를 Identity API 차단만으로 방지할 수 없음.
- 예상 밖 변경: 기존 문서 변경 보존, 이번 추가는 기록뿐. 코드/API/외부 설정/계정 삭제/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): 단일 SNS 정책의 link/자동 등록/가입/승격 수정 범위와 Guest 기능 유지 방향 설명. 신규 전화 OTP 복구 안내는 별도 계약 필요.
- 다음 작업: 사용자 구현 요청 후 정책 오류·API 폐지 계약 확정, 회귀 테스트 및 전체 clean test 실행. 배포 전 진행 link 작업과 실제 데이터/인덱스 확인.

## 2026-10-02 — 전화 인증 후 계정 안내 표시 범위 설명

<!-- codex-turn:01a0fd07-e129-7f23-9e3e-809e6bcfac7f -->

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 계정 안내가 이메일 주소 표시를 의미하는지 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 제품 코드 변경 없음. 앞선 SNS 종류 안내 제안과 선택적인 마스킹 이메일 힌트를 구분. SocialIdentity/VerifiedSocialPrincipal 관련 email 필드 검색 수행, 전체 이메일 취득/보관 경로는 미조사.
- 실행한 테스트와 결과: git diff --check 실행. 설명/기록만 변경하여 Gradle 미실행.
- 유지한 계약: 서버가 전화 인증을 검증한 후 제한된 계정 힌트 제공, 자동 로그인·SNS 연결·Guest 병합 권한 부여 없음.
- 결정사항: SNS 종류를 기본 안내하고 이메일 힌트 추가 시 마스킹 권장. 이메일 미제공 시 SNS만 안내, Apple 비공개 중계 주소를 실제 개인 이메일로 표현하지 않음. 표시 계약은 제안이며 미구현.
- 위험 요소: 전화번호 재할당 가능성, SNS별 이메일 제공 여부 및 정확한 저장/조회 출처 미확인.
- 예상 밖 변경: 없음. 기존 기록 보존, 제품 코드/API/외부 설정/배포/commit/push/Jira 변경 없음.
- 다음 작업: 이메일 힌트 도입 시 제공 가능한 데이터 출처와 응답 필드·마스킹 규칙 확인.

## 2026-10-02 — 계정 안내 기능 개발 범위 설명

<!-- codex-turn:01a0fd07-e129-7f23-9e3e-809e6bcfac7f -->

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 전화 인증 기반 계정 안내와 이메일 힌트의 추가 개발 범위 구분.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 제품 코드 변경 없음. 신규 계정 찾기 API의 인증 검증/회원 조회/요청 제한/오류 처리/테스트와 이메일 표시 확장을 구분하여 설명.
- 실행한 테스트와 결과: git diff --check. 설명/기록 작업으로 Gradle 미실행.
- 유지한 계약: 단일 SNS 정책 및 Guest 기능 유지 방향, 계정 안내만으로 인증 토큰 발급/계정 연결 허용하지 않음.
- 결정사항: SNS 종류 안내를 우선 구현하고 이메일 힌트는 데이터 출처 확인 후 확장하는 방향 권장. 이메일이 이미 신뢰 가능한 형태로 확보되면 증분 범위는 작지만 수집/저장이 필요하면 범위 증가. 확정 일정은 제시하지 않음.
- 위험 요소: 이메일 취득/보관 경로 미확인, Firebase 전화 인증을 계정 찾기에 사용하는 구체 계약 미확정.
- 예상 밖 변경: 없음. 기존 문서 변경 보존, 코드/API/외부 설정/배포/commit/push 변경 없음.
- 다음 작업: 구현 요청 시 계정 찾기 범위 및 이메일 데이터 출처 확인 후 계약·테스트 구체화.

## 2026-10-02 — 계정 안내 개발 범위 설명 기록 보완

<!-- codex-turn:01a0fd0a-0b97-7ca1-ba5c-d7c0c061b6b8 -->

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 계정 안내 개발 범위 설명을 현재 turn 식별자로 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 제품 변경 없음. 전화 인증 기반 계정 찾기가 주요 신규 범위이며 이메일 마스킹의 증분 작업량은 데이터 확보 여부에 따라 달라짐을 설명한 결과 기록.
- 실행한 테스트와 결과: git diff --check 통과. 문서 기록만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 WORKLOG 보존, 비밀정보 비기록, Guest 기능 유지 및 단일 SNS 정책 방향 유지.
- 결정사항: 빠른 적용을 위해 단일 SNS 제한 우선, 계정 찾기는 후속 작업으로 분리 권장. 이메일 출처 확인 후 마스킹 힌트 포함 여부 판단.
- 위험 요소: 이메일 취득/저장 경로 및 계정 찾기 API 구체 계약 미확정.
- 예상 밖 변경: 없음. 제품 코드/API/외부 설정/배포/commit/push 변경 없음.
- 다음 작업: 구현 요청 시 단일 SNS 제한과 계정 찾기 범위를 구체화하고 관련 테스트 수행.

## 2026-10-03 — 단일 SNS·전화 인증 계정 찾기 통합 계획서 작성

<!-- codex-turn:01a0fd29-eae2-7e42-823b-f2a76ca3cf73 -->

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: SNS 하나 제한과 전화번호 인증 후 SNS/마스킹 이메일 안내를 동일 개발·출시 범위로 계획.
- 변경 파일: docs/contracts/single-sns-account-recovery-implementation-plan.md 신규, docs/codex/CURRENT_STATE.md 갱신, docs/codex/WORKLOG.md 끝에 append.
- 구현 내용: 제품 코드 변경 없음. SocialIdentity/Firebase provider DTO에 이메일 주소가 없고 FEDERATED User.email이 null임을 확인. userId unique, signup/upgrade 단일 검증, exchange 자동 추가 제거, Guest merge 현재 승인 SNS 검증, link/unlink 제품 API 폐지, 이메일 힌트 수집/마스킹 snapshot, recovery prepare/lookup·전용 전화 verifier·proof 소비/재시도·공유 rate limit·프론트/환경 설정/테스트/배포 계획 작성.
- 실행한 테스트와 결과: 계획서 로컬 링크 28개 존재 검사 통과. git diff --check 및 신규 문서 whitespace 검사 수행. 문서만 변경하여 Gradle/외부 인프라 테스트 미실행.
- 유지한 계약: Guest prepare/upgrade/merge 및 기존 LOCAL 기능 유지, JWT/하위 서비스 이벤트에 이메일·전화 추가 금지, 전화 proof만으로 회원 인증/연결/병합 불가, 번호 단일 활성 owner와 보안 block/floor 유지, 과거 WORKLOG 보존.
- 결정사항: 사용자의 한 번에 개발 요청으로 이전 분리 출시 권고 대체. 마스킹 이메일을 이번 범위에 포함하되 미제공/Apple relay 정상 처리. SNS 이메일은 User.email 대신 SocialIdentity의 표시용 힌트로 보관. 전화 인증은 별도 프로젝트/클라이언트 Auth 격리를 권고하고 실제 환경 선택은 구현 시 확정할 항목으로 명시. 신규 API/오류/설정값은 제안이며 현재 구현 사실과 구분.
- 위험 요소: 실제 DB/Firebase 회원·진행 작업 미조회, provider별 이메일 제공 및 별도 OTP 환경 검증 미수행. 앱 저장소 구현/통합 QA 필요, unique 인덱스·분산 제한·응답 유실·기존 보안 경합 테스트 필요.
- 예상 밖 변경: 없음. 기존 CURRENT_STATE/WORKLOG 변경을 보존하고 이번 계획서와 작업 기록만 추가. 코드/API/계정 삭제/외부 인프라/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): 단일 SNS 제한·전화 인증 계정 찾기·마스킹 이메일을 동일 출시 범위로 계획. 관련 코드 근거와 오류/필드/TTL/재시도/보안/프론트/테스트/배포 점검 수록, 실제 구현·환경 검증은 미수행.
- 다음 작업: 구현 요청 시 전용 OTP 환경 선택 및 계약 확정 후 계획 순서대로 서버/테스트 도구 구현·회귀 테스트 수행, 앱 연동/배포 전 데이터·인덱스 점검.

## 2026-10-03 — 통합 계획서 사용자 흐름 중심 설명

<!-- codex-turn:01a0fd39-00ea-7533-960e-dff9a444171f -->

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 단일 SNS·전화 인증 계정 찾기 통합 계획을 사용자 눈높이로 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 제품 코드/계획서 변경 없음. 특정 SNS 계정 하나와 활성 전화번호 owner 하나의 정책, Guest 승격·병합과 SNS 추가의 차이, 전화 OTP/힌트 안내/실제 SNS 로그인 단계, 이메일 미제공 및 전용 Firebase 권고안 설명.
- 실행한 테스트와 결과: 계획서·최신 기록 확인, git diff --check 통과. 설명/기록만 변경하여 Gradle 미실행.
- 유지한 계약: 통합 개발·출시 범위, Guest 기능, 계정 안내만으로 회원 로그인/병합 불가, 마스킹 힌트 및 원문 비노출, 기존 LOCAL 기능 유지.
- 결정사항: 계정 찾기 prepare는 조회 접수번호 발급이며 Guest prepare와 별개. 이메일 힌트 저장·DB 단일 제약·자동 등록 차단·재시도/제한은 해당 사용자 경험을 구현하는 내부 작업. 별도 Firebase 프로젝트는 권고 설계이며 실제 선택은 구현 착수 시 확인.
- 위험 요소: 계획은 미구현, provider별 이메일 제공과 OTP 환경/실제 앱 연동 검증 필요.
- 예상 밖 변경: 없음. 기존 기록 보존, 제품 코드/API/외부 상태/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): 통합 계획을 가입/Guest/계정 찾기 사용자 흐름으로 설명. OTP는 번호 소유 확인과 계정 힌트 안내에 사용하고 회원 로그인은 기존 SNS 재인증 필요.
- 다음 작업: 구현 요청 시 통합 계획에 따라 환경 선택·구현·테스트·앱 연동 진행.

## 2026-10-03 — Guest JWT와 Firebase 인증 상태 구분 정정

<!-- codex-turn:01a0fd4e-fce1-7673-bfe5-ee21facd843e -->

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 계정 찾기 전화 인증 격리 설명에서 Guest 인증과 Firebase SDK 로그인 상태 혼동 해소.
- 변경 파일: docs/contracts/single-sns-account-recovery-implementation-plan.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 구현 내용: 제품 변경 없음. FirebaseGuestMergeService에서 CurrentUserProvider source와 Firebase 증명 target 분리를 확인. 계획서에 Guest JWT는 Firebase 전화 로그인만으로 폐기되지 않으며 앱의 토큰 저장/교체 처리와 Firebase currentUser를 구분하는 설명 추가.
- 실행한 테스트와 결과: 코드·계획서 정적 확인, git diff --check 통과. 설명/문서 변경으로 Gradle 미실행.
- 유지한 계약: Guest source는 인증된 Identity 사용자에서 결정, target은 Firebase SNS 증명으로 검증. 전화 인증은 계정 안내용이며 Guest 병합/회원 인증을 대신하지 않음.
- 결정사항: 별도 Auth 인스턴스는 클라이언트 로그인 상태, 별도 Firebase 프로젝트는 서버 사용자 공간 격리라는 차이를 설명. Guest 인증 보존만으로 새 프로젝트가 필수라는 주장은 정정. 기존 권고안은 원격 부작용 격리 선택으로 명확히 하며 실제 SDK/프로젝트 선택은 아직 미확정.
- 위험 요소: 실제 앱의 Firebase SDK 사용법/로그인 리스너/토큰 저장 구현 미조사. 같은 프로젝트의 별도 Auth 인스턴스는 전화 credential 소유권·원격 계정 상태를 분리하지 않음.
- 예상 밖 변경: 없음. 기존 기록 보존, 계획서 설명 보완 및 작업 기록만 변경. 코드/외부 인프라/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): Guest JWT/Firebase currentUser 구분과 프로젝트 격리 선택 설명 보완. Guest merge source/target 분리 확인, 앱 구현 및 원격 phone 흐름 확인 필요.
- 다음 작업: 구현 시 앱 상태 관리와 같은 프로젝트 전화 인증 부작용을 확인하여 격리 방식을 결정.

## 2026-10-03 — 계정 찾기와 Guest 병합의 독립성 설명

<!-- codex-turn:01a0fd51-cd56-7b10-a6a3-3416f0226c92 -->

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 계정 찾기가 Guest 병합에 의존하는 것처럼 전달된 설명 정정.
- 변경 파일: docs/contracts/single-sns-account-recovery-implementation-plan.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 구현 내용: 제품 코드 변경 없음. 계획서에 계정 찾기는 SNS/이메일 힌트 안내로 완료되고 Guest 토큰/병합이 필요 없음을 명시. 이후 사용자가 선택하는 로그인 및 Guest 병합은 별도 기존 흐름으로 구분.
- 실행한 테스트와 결과: 계획서 확인 및 git diff --check 통과. 설명/문서 변경으로 Gradle 미실행.
- 유지한 계약: 통합 개발 범위는 유지하되 계정 찾기와 병합 API의 책임 분리. 전화 proof로 로그인/병합 권한 발급 없음.
- 결정사항: Guest 상태 보존은 찾기 구현이 기존 앱 상태를 변경하지 않도록 하는 일반 연동 조건이며 계정 찾기 자체의 병합 의존성이 아님. 별도 프로젝트 필요 여부는 병합과 무관한 Firebase 인증 설계 문제.
- 위험 요소: 실제 앱 로그인/계정 찾기 연동 및 Firebase 환경 선택 미확인.
- 예상 밖 변경: 없음. 기존 기록 보존, 코드/외부 상태/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): 계정 찾기/Guest 병합의 독립성과 선택적 후속 로그인 흐름 문서 명확화.
- 다음 작업: 구현 시 계정 찾기 API를 Guest 없이 사용 가능하게 만들고 기존 인증 상태에 부작용이 없음을 검증.

## 2026-10-03 — 기존 Firebase 프로젝트 재사용 계획 반영

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: Firebase 프로젝트를 추가하지 않는 방향과 필요한 기존 Firebase 설정 설명.
- 변경 파일: docs/contracts/single-sns-account-recovery-implementation-plan.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 구현 내용: 제품 코드 변경 없음. 기존 project/tenant/Admin 설정을 재사용하고 서버 검증만 계정 찾기 목적별로 추가하는 안으로 계획 수정. Phone/SMS/플랫폼 앱 검증 설정과 기존 phone UID/미등록 번호의 phone-only UID 생성·후속 가입 테스트 항목 반영. 공용 프로젝트의 무조건 계정 삭제/30일 정리 제안 제거.
- 실행한 테스트와 결과: FirebaseAuthProperties/application 설정 정적 확인 및 git diff --check. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: Guest JWT 독립, 번호로 계정 힌트 조회, 전화 proof만으로 회원 세션 발급 없음. 기존 가입/승격 enrollment 조건과 SNS 로그인 검증 유지.
- 결정사항: 새 Firebase 프로젝트/service account 불필요. 이미 같은 앱에서 실제 전화 인증이 동작하면 관련 콘솔 설정 재사용 가능. 현재 설정의 활성 여부는 소스의 기본값으로 단정하지 않음. 같은 프로젝트의 일반 PHONE 증명과 계정 찾기 목적 검증을 구분.
- 위험 요소: 실제 Firebase 콘솔·SMS 결제/할당량·앱 SDK 설정 미조회. 미등록 번호의 phone-only 계정 생성 및 후속 가입/정리 상호작용은 통합 검증 필요.
- 예상 밖 변경: 없음. 기존 기록 보존, 제품 코드/외부 설정/계정 삭제/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): 계정 찾기는 기존 Firebase 프로젝트 재사용. 콘솔 Phone/SMS/앱 검증 확인 및 별도 서버 목적 검증, 기존 UID/신규 phone-only UID 회귀 계획 반영.
- 다음 작업: 구현 요청 시 기존 전화 인증 설정과 앱 SDK 흐름을 확인하고 통합 계획 구현.

## 2026-10-03 — 기존 Firebase 재사용 작업 식별 기록 보완

<!-- codex-turn:01a0fd53-58b8-7e53-ab0f-7fc7aa4d8bdc -->

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 기존 Firebase 프로젝트 재사용 계획 반영 작업을 정확한 현재 turn 식별자로 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 앞선 계획서 수정 결과 유지.
- 구현 내용: 제품 변경 없음. 기존 프로젝트/tenant/Admin 설정 재사용, 콘솔 Phone/SMS/앱 검증 확인 및 서버 목적별 전화 증명 검증 추가 방향의 기록 보완.
- 실행한 테스트와 결과: git diff --check 통과. 기록만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 계정 찾기와 Guest 병합 분리, 기존 가입/로그인 보안 조건 유지.
- 결정사항: 새 Firebase 프로젝트 생성 없이 진행하는 계획 유지. 실제 콘솔 및 앱 SDK 설정은 미조회.
- 위험 요소: 미가입 번호의 phone-only UID 생성과 후속 가입 상호작용 통합 검증 필요.
- 예상 밖 변경: 없음. 코드/외부 설정/배포/commit/push/Jira 변경 없음.
- 다음 작업: 구현 요청 시 기존 전화 인증 설정 확인 후 통합 계획 구현 및 테스트.

## 2026-10-03 — 기존 SMS 인증 재사용 및 Phone 로그인 용어 설명

<!-- codex-turn:01a0fd55-b681-7a20-b213-8c6b85d78420 -->

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: SMS 설정이 이미 있는 상황에서 Phone 로그인이라는 표현이 의미하는 바를 설명.
- 변경 파일: docs/contracts/single-sns-account-recovery-implementation-plan.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 구현 내용: 제품 변경 없음. 기존 SMS 설정 재사용을 명시하고 Firebase SDK의 전화 인증 증명 획득과 앱 회원 로그인 기능을 구분하도록 계획서 표현 보완.
- 실행한 테스트와 결과: 계획서 확인 및 git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 기존 Firebase 프로젝트 재사용, 전화 인증 결과로 계정 힌트만 조회, 회원 토큰 발급/자동 병합 없음.
- 결정사항: 사용자 확인 기준 SMS 설정 완료. Phone 제공업체를 새로 켜거나 앱에 전화번호 로그인 메뉴를 추가하는 작업을 요구하지 않음. 기존 SMS 흐름의 서버 검증 가능한 Firebase 증명 전달은 구현 대상.
- 위험 요소: 앱 SDK의 기존 SMS 확인/증명 획득 코드는 미조회, 콘솔 상태는 사용자 확인 기준.
- 예상 밖 변경: 없음. 과거 기록 보존, 코드/외부 설정/배포/commit/push/Jira 변경 없음.
- 다음 작업: 구현 시 기존 SMS 인증 결과 전달을 재사용하고 계정 찾기 서버 검증/API 추가.

## 2026-10-03 — SMS 재사용 설명 작업 식별 기록 보완

<!-- codex-turn:01a0fd55-b681-7541-b03d-c12d81013760 -->

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: SMS 재사용 및 Phone 인증 용어 설명을 정확한 현재 turn 식별자로 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 제품 변경 없음. Firebase 전화 인증 증명 획득과 Identity 회원 로그인 기능을 구분한 설명 기록 보완.
- 실행한 테스트와 결과: git diff --check 통과. 기록만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 기존 SMS/Firebase 설정 재사용, 전화 인증으로 회원 토큰 발급 없음.
- 결정사항: 별도 전화번호 로그인 기능이나 신규 Firebase 설정을 요구하지 않는 방향 유지.
- 위험 요소: 실제 앱 SDK 흐름 미조회, SMS 설정 완료는 사용자 확인 기준.
- 예상 밖 변경: 없음. 코드/외부 설정/배포/commit/push 변경 없음.
- 다음 작업: 구현 요청 시 기존 SMS 인증 결과를 계정 찾기 API에 연동.

## 2026-10-03 — 통합 계획의 남은 사용자 결정사항 점검

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 사용자에게 다시 확정받아야 할 사항과 구현 검증 사항 구분.
- 변경 파일: docs/contracts/single-sns-account-recovery-implementation-plan.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 구현 내용: 제품 코드 변경 없음. 기존 대화와 계획서 결정 섹션을 확인하고 필수 미결정 사항이 없음을 문서에 반영. 기본 표시 정책/인증 유효기간/제한과 SDK·인덱스·데이터 검증을 구분.
- 실행한 테스트와 결과: 계획서 정적 확인 및 git diff --check 통과. 문서만 변경하여 Gradle 미실행.
- 유지한 계약: 단일 SNS/활성 전화번호 owner, Guest 기능, 전화 인증 후 힌트 안내, 기존 Firebase/SMS 재사용, 통합 개발 범위.
- 결정사항: 이미 정한 방향을 재승인받지 않음. 이메일 미제공/Apple relay는 계획 기본안 적용. 기존 SNS에 접근할 수 없는 사용자의 SNS 교체/접근 복구 기능은 이번 범위에 포함하지 않음.
- 위험 요소: 실제 앱 SDK/콘솔/DB 상태와 provider별 이메일 제공은 구현 시 검증 필요하며 사용자 미결정 사항과 구분.
- 예상 밖 변경: 없음. 기존 기록 보존, 제품 코드/외부 설정/배포/commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): 통합 계획의 필수 사용자 결정 완료 상태 확인. 남은 SDK/환경/인덱스 검증과 기존 SNS 접근 복구 제외 범위 문서 명확화.
- 다음 작업: 구현 요청 시 계획 기본안으로 구현 및 회귀 테스트 진행.

## 2026-10-03 — 사용자 결정사항 점검 식별 기록 보완

<!-- codex-turn:01a0fd57-258b-7082-8719-b1260e55b435 -->

- 브랜치: develop. 지정된 Jira 없음.
- 작업 목표: 남은 결정사항 점검 결과를 정확한 현재 turn 식별자로 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 제품 변경 없음. 필수 사용자 결정이 남아 있지 않고 계획 기본안으로 진행 가능하다는 설명 기록 보완.
- 실행한 테스트와 결과: git diff --check 통과. 문서 기록만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 기존 Firebase/SMS 재사용 및 통합 개발 범위 유지.
- 결정사항: 이미 합의한 방향을 재승인받지 않으며 기존 SNS 접근 복구/교체는 이번 범위 밖으로 유지.
- 위험 요소: 앱 SDK/데이터/인덱스/이메일 제공 여부는 구현 검증 필요.
- 예상 밖 변경: 없음. 코드/외부 설정/배포/commit/push 변경 없음.
- 다음 작업: 구현 요청 시 계획 기본안에 따라 구현·회귀 테스트 수행.

## 2026-10-03 — 통합 구현 Jira 생성안 준비

- 브랜치: develop.
- Jira: TMI-136 (상위 에픽 조회 및 제안, 신규 작업 이슈 미생성).
- 작업 목표: 통합 구현 계획을 Jira 작업 하나로 등록할 구체적인 생성안 준비.
- 변경 파일: docs/contracts/single-sns-account-recovery-implementation-plan.md 부록 E, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 구현 내용: 공식 Atlassian MCP로 부모 에픽/이슈 유형/생성 필드를 조회하고 제목·본문·완료 조건·제외 범위를 작성. 제품 코드 변경 없음.
- 실행한 테스트와 결과: Jira read 조회 성공, Rovo 검색은 403 권한 오류로 중복 검색 실패. git diff --check 통과. 문서 작업으로 Gradle 미실행.
- 유지한 계약: Jira 쓰기 전 내용 제시 및 승인, 비밀정보/사용자 개인정보 미기록, 통합 개발 및 기존 Firebase/SMS 재사용 범위 유지.
- 결정사항: TMI-136 하위 작업 유형의 생성안을 제시하되 실제 이슈 유형은 일반 작업(에픽 자식)으로 선택. 담당자/기한 임의 지정 없음. 사용자에게 생성안 확인 요청.
- 위험 요소: 중복 이슈 미확인, 실제 생성 권한은 쓰기 호출 전이므로 미검증.
- Jira 작업: 조회만 수행. 댓글 목적/상태 변경 없음. 승인 여부: 생성 요청 수신, 구체 생성안의 사전 확인 대기.
- 예상 밖 변경: 없음. 기존 기록 보존, Jira 생성/수정/댓글/상태 전환/제품 코드/배포/commit/push 없음.
- 다음 작업: 제시한 생성안 승인 후 신규 작업 생성 및 반환된 이슈/부모/본문 확인, 문서에 새 키 반영.

## 2026-10-03 — Jira 생성안 준비 식별 기록 보완

<!-- codex-turn:01a0fd59-0d07-78a1-92ea-a885691cbd5e -->

- 브랜치: develop.
- Jira: TMI-136 (상위 에픽 조회, 신규 이슈 미생성).
- 작업 목표: Jira 생성안 준비 결과를 정확한 현재 turn 식별자로 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 제품 변경 없음. 계획서 부록 E의 생성안 제시 및 승인 대기 상태 기록 보완.
- 실행한 테스트와 결과: git diff --check 통과. 기록만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, Jira 쓰기 전 내용 제시/승인 규칙 유지.
- 결정사항: 신규 작업 제목/본문/부모 제시 완료, 사용자 승인 후 생성.
- Jira 작업: 추가 조회/쓰기 없음. 댓글 목적/상태 변경 없음. 생성안 승인 대기.
- 위험 요소: 검색 권한 오류로 중복 이슈 여부 미확인.
- 예상 밖 변경: 없음. 제품 코드/외부 상태/배포/commit/push 변경 없음.
- 다음 작업: 승인 후 Jira 작업 생성 및 반환된 키/부모/본문 검증.

## 2026-10-03 — TMI-192 통합 구현 Jira 생성 완료

- 브랜치: develop (이슈 생성·문서 반영 작업, 구현 브랜치 생성 없음).
- Jira: TMI-192. 상위 에픽 TMI-136.
- 작업 목표: 사용자 승인한 단일 SNS·SMS 계정 찾기 작업 이슈 생성.
- 변경 파일: docs/contracts/single-sns-account-recovery-implementation-plan.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 구현 내용: 공식 Atlassian MCP로 승인된 제목/본문의 작업 이슈 생성. 재조회하여 부모 TMI-136, 상태 해야 할 일 및 본문 일치 확인. 계획서에 새 이슈 링크 반영. 제품 코드 변경 없음.
- 실행한 테스트와 결과: 생성 및 재조회 성공, Markdown 목록 기호 정규화 후 승인 본문 일치. git diff --check 통과. 문서/Jira 작업으로 Gradle 미실행.
- 유지한 계약: 기존 Firebase/SMS 재사용, 단일 SNS·계정 찾기 통합 범위, Guest 기능 유지, Jira 비밀정보/개인정보 미기록.
- 결정사항: 사용자 '어' 응답으로 제시한 생성안 승인 확인 후 1개 작업 생성. 담당자 미지정·프로젝트 기본값 사용. 이슈 구현은 별도 요청 전 미착수.
- Jira 작업: TMI-192 생성. 댓글 목적: 해당 없음(댓글 미등록). 상태: 생성 기본 상태 해야 할 일, 별도 전환 없음. 승인 여부: 생성 전 본문 제시 후 명시 승인 받음.
- 위험 요소: 앞선 검색 권한 오류로 기존 중복 이슈 여부 미확인, 실제 Firebase/앱/DB 통합 검증은 구현 단계 필요.
- 예상 밖 변경: 없음. 기존 기록 보존, 제품 코드/배포/commit/push 변경 없음.
- Jira 댓글 초안(미등록): 승인된 통합 계획으로 이슈 생성 및 부모/본문 확인, 계획서 링크 반영. 제품 구현·테스트·배포는 미착수.
- 다음 작업: 구현 요청 시 TMI-192를 기준으로 이슈 키를 포함한 구현 브랜치 및 코드/테스트 작업 진행.

## 2026-10-03 — TMI-192 생성 완료 식별 기록 보완

<!-- codex-turn:01a0fd5b-8e27-72e0-831e-4a79cb1f5fff -->

- 브랜치: develop.
- Jira: TMI-192 (상위 TMI-136).
- 작업 목표: Jira 생성 완료 결과를 정확한 현재 turn 식별자로 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 제품 변경 없음. 승인 후 TMI-192 생성 및 제목/본문/부모 확인 결과의 기록 보완.
- 실행한 테스트와 결과: git diff --check 통과. 기록만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 승인 범위 내 Jira 생성.
- 결정사항: 생성 완료 상태 유지, 추가 이슈 생성/수정 없음.
- Jira 작업: 앞선 생성은 사용자 승인 후 수행. 이번 보완 중 Jira 호출 없음. 댓글 미등록, 상태 전환 없음.
- 위험 요소: 기존 중복 이슈 검색 권한 미확보, 제품 구현/환경 검증 미착수.
- 예상 밖 변경: 없음. 코드/외부 상태/배포/commit/push 변경 없음.
- 다음 작업: 구현 요청 시 TMI-192와 통합 계획에 따라 진행.

## 2026-10-03 — 정지 회원 병합 및 downstream 이전 UI 의미 설명

- 브랜치: develop.
- 작업 목표: 정지 회원의 재로그인/복구 가능성과 병합 성공 후 비동기 이전·Guest 승격 차이 설명.
- 변경 파일: docs/contracts/frontend-firebase-auth-integration-guide.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 구현 내용: 제품 코드 변경 없음. Guest merge resolver/transaction의 SUSPENDED 거절 및 exchange 비활성 거절 확인. upgrade는 동일 userId이고 merge는 source/target 변경 및 outbox 저장임을 확인. 가이드에 출시 gate와 요청별 완료 표시를 구분하고 공개 이전 완료 API 미구현 사실/새 조회 계약 필요성 명시.
- 실행한 테스트와 결과: 소스/프론트 계약/LC 인계서/owner fanout 계획 정적 확인, git diff --check 통과. 설명·문서 변경으로 Gradle 미실행.
- 유지한 계약: 정지 대상에 세션 발급/병합 금지, 실패 시 Guest 상태 보존, 전화 owner 검증 유지, merge의 비동기 전달과 upgrade userId 보존, 하위 서비스 권한 검증 유지.
- 결정사항: 정지는 재로그인으로 해제되지 않으며 현재 사용자용 해제 API 없음. 안내·문의/이의 제기 경로 권고, 실제 운영 해제 절차는 별도. UI gate는 출시 조건이며 앱 전체 대기 요구가 아님. 완료 표시를 원하면 LC/Billing 실제 처리 완료를 조회하는 별도 계약 필요; 이번 작업에서 API 구현/이슈 범위 확대 없음.
- 위험 요소: LC/Billing 실제 저장소·배포/consumer 완료 상태 미검증. 사용자별 migration 상태 API는 Identity에 없고 외부 서비스의 현 구현은 미확인. upgrade 후 Billing 권리 반영은 별도 비동기 상태일 수 있음.
- 예상 밖 변경: 없음. 기존 문서/기록 보존, 제품 코드/외부 상태/Jira/배포/commit/push 변경 없음.
- Jira 댓글 초안(미등록): 정지 대상 재로그인 해결 불가 및 merge 출시 gate/비동기 완료/동일 ID upgrade 구분 명확화. 완료 조회·정지 해제는 별도 계약 필요.
- 다음 작업: 실제 consumer 준비 여부 확인 및 필요 시 사용자별 이전 완료 UX/API와 정지 해제 운영 정책 별도 설계.

## 2026-10-03 — 정지 회원·병합 UI 설명 식별 기록 보완

<!-- codex-turn:01a0fd5c-ec5a-7152-94f6-6b05d4e4f9d9 -->

- 브랜치: develop.
- 작업 목표: 정지 계정 복구 및 병합/승격 UI 설명을 정확한 현재 turn 식별자로 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 제품 변경 없음. 정지 계정은 재로그인으로 복구되지 않고, 병합 UI gate는 출시 조건이며, 승격은 동일 userId라는 설명 결과 기록 보완.
- 실행한 테스트와 결과: git diff --check 통과. 기록만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 기록 보존, 비밀정보 비기록, 정지 대상 인증 거절, Guest 인증 보존, 비동기 병합 및 승격 ID 유지.
- 결정사항: 전체 앱 차단을 필수 동작으로 해석하지 않음. 사용자별 이전 완료 표시는 별도 조회 계약이 필요하며 현재 Identity 공개 API 없음.
- 위험 요소: 외부 consumer 실제 구현/배포 및 정지 해제 운영 절차 미확인.
- 예상 밖 변경: 없음. 코드/외부 상태/Jira/배포/commit/push 변경 없음.
- 다음 작업: 필요 시 하위 서비스 완료 조회 계약과 정지 해제 정책 설계.

## 2026-10-03 — TMI-192 단일 SNS 정책·SMS 계정 찾기 구현

<!-- codex-turn:01a0fd60-52ad-7831-a34f-61c46b6e75ac -->

- 날짜: 2026-10-03.
- 브랜치: feat/TMI-192-single-sns-account-recovery.
- Jira: TMI-192 (상위 TMI-136).
- 작업 목표: 승인 계획의 단일 SNS·전화번호당 활성 회원 하나·SMS 계정 찾기/마스킹 이메일을 Identity 저장소에서 구현. 정지 해제 및 downstream 이전 완료 조회는 범위 제외.
- 변경 파일: 신규 domain/auth/accountrecovery 9개 구현 클래스, domain/auth/domain/EmailHint, federation/application/SingleSocialIdentityPolicy 및 SocialIdentity; Firebase signup/upgrade/exchange/merge target/sync/verifier/SDK/principal; providerchange guard/login fence/config/retired controllers/request filter; SecurityConfig/ReissueNoStoreFilter/IdentityOpenApiExamples/application.yml/.env.example. 대응 신규·기존 Java 테스트, tools/auth-test app/index/server/UI mock tests/README, frontend-firebase-auth-integration-guide, single-sns-account-recovery-implementation-plan/runbook, docs/swagger/index.html, CURRENT_STATE/WORKLOG.
- 구현 내용: SocialIdentity userId unique 및 단일 SNS 검증, 기존 SNS에 대해서만 로그인 권한 부여, 자동 추가 등록 제거. 공개 link/unlink는 410으로 폐지하고 런타임 ProviderLinkService bean 제거. Guest/LOCAL/PASSWORD 및 기존 보안 이력 처리 유지. 이메일 원문 대신 provider별 마스킹 힌트를 가입·승격 시 저장.
- 계정 찾기: 공개 prepare/lookup 추가. 기존 Firebase project/tenant/Admin과 PHONE 인증 재사용, 최근 auth_time/prepare 시각/만료/폐기/disabled/토큰과 Admin 번호 일치 검사. 전용 HMAC(project,tenant,uid,auth_time,phone) retained keys로 proof unique claim 및 attempt CAS를 Mongo transaction에 묶고 재시도 시 현재 owner/상태 재조회. UID/IP/번호 HMAC counter 공유 요청 제한, 본문 크기 제한, no-store. 계정 찾기로 회원 토큰/enrollment/merge 권한을 발급하지 않음.
- UI·문서: 동일 Firebase 프로젝트의 별도 in-memory Auth로 기존 로그인 보존하는 로컬 테스트 화면 추가. 원문 증명 표시/저장/자동 사용자 삭제 없음. 프론트의 과거 자동 등록/추가 연결 안내는 폐지 계약으로 교체, 정확한 오류/설정/인덱스/출시 QA 런북 작성.
- 실행한 테스트와 결과: 최종 ./gradlew clean test 성공 — 157 suite, 1083 tests, failures/errors/skipped 모두 0. node --test tools/auth-test/*.test.mjs 성공 — 3/3. git diff --check 및 변경 계약 문서 상대 링크 검사 통과. 중간 실패는 구 다중 SNS/자동 연결 기대치와 fixture/OpenAPI 예제 갱신, Mockito stubbing 수정으로 해소. Gradle 캐시·로컬 포트의 sandbox 제한은 승인된 확장 실행으로 해결. 실제 Atlas/OAuth 호출 없이 mock/인메모리 Mongo 사용.
- 유지한 계약: UUID userId/JWT sub, RS256/JWKS/issuer/audience, 기존 전화 fingerprint active unique, Guest 승격 동일 ID·병합 source/target 및 outbox, LOCAL/PASSWORD, 로그아웃/탈퇴 epoch 및 provider block/floor, 토큰·비밀번호·원문 PII 비기록.
- 변경한 외부 계약: SNS link/unlink 유효 요청 410 PROVIDER_LINK_RETIRED, 복수 SNS 가입·승격 409 SINGLE_SNS_REQUIRED, 승인 SNS 불일치 SNS_ACCOUNT_MISMATCH. sync legacy linkAttemptId 410. 신규 recovery prepare/lookup 및 전용 오류·한도·환경변수. Firebase 프로젝트 신규 생성 없음.
- 결정사항: 계정 찾기 기본 OFF. Attempt는 proofIds null/비null로 PENDING/CONSUMED 표현하며 retryUntil을 원자 기록. 힌트는 가입/승격 snapshot만 저장하고 로그인 자동 갱신/백필 제외. legacy ProviderChangeTests는 과거 보안 상태 fixture에서만 userId unique를 제외하고 실제 repository 테스트는 새 unique를 검증. 기존 보안 worker/이력은 삭제하지 않음.
- 위험 요소: 실DB 회원·진행 작업 부재 미확인. unique 이행/TTL/replica-set rollback 및 실 다중 인스턴스 경쟁/키 회전 미검증. 실제 앱·Firebase SMS/Apple·Kakao 이메일 제공 여부·phone-only UID 후속 가입 credential 충돌 QA 필요. Identity IP 제한이 Firebase 직접 SMS 남용을 제한하지 않음. 프록시 실 IP 신뢰 설정 확인 필요.
- 배포 전 확인: 런북 순서대로 쓰기 중단/중복·진행 작업 검사 후 정확한 userId 인덱스 전환, 기존 phone unique/신규 TTL 검증, 독립 recovery 키 및 기존 Firebase/phone 설정 준비. old/new HMAC 키 overlap 및 >=24h retained 유지. 앱과 단일 SNS·계정 찾기 동일 출시 gate 준수. 기존 merge LC/Billing E2E gate 유지.
- 예상 밖 변경: 없음. 시작 당시 미커밋 WORKLOG/CURRENT_STATE/프론트 가이드/계획서는 보존. 과거 WORKLOG 항목은 수정·삭제하지 않고 실제 EOF에 append. commit/push/배포/외부 인프라 변경·사용자 삭제 없음.
- Jira 작업: 구현 전 공식 MCP로 TMI-192 조회. 이번 구현에서 생성/수정/댓글/상태 전환 없음. 사용자 구현 승인 범위만 수행.
- Jira 댓글 초안(미등록): TMI-192 단일 SNS 정책·추가 연결 폐지·SMS 계정 찾기/마스킹 힌트·재시도/분산 제한·프론트 계약/테스트 UI 구현. auth/federation/providerchange/accountrecovery, 설정·문서·테스트 변경. clean test 1083개 및 Node 3개 통과. 배포 전 데이터/unique/TTL/실 Firebase 및 앱·replica-set 검증 필요.
- 다음 작업: 사용자 diff 검토 후 직접 commit/push/PR. 별도 앱 저장소 구현·환경 설정·런북의 실제 통합 QA 완료 후 배포 승인. 정지 복구 및 downstream 이전 상태 API는 별도 요구사항으로 유지.

## 2026-10-03 — 병합 downstream 전체 완료 조회 API 유무 확인

<!-- codex-turn:01a10082-bb5b-7010-8e29-8daa0ade9886 -->

- 브랜치: feat/TMI-192-single-sns-account-recovery.
- Jira: TMI-192 (현재 브랜치 문맥; 추가 이슈 작업 없음).
- 작업 목표: Identity 병합 확정 이후 Learning Core/Billing 전체 처리 완료 조회 API 존재 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 제품 변경 없음. Firebase merge Controller 및 공개 Controller 경로, 프론트 계약 4.5, TMI-192 런북 확인. Identity에 downstream 종합 완료 조회 API가 없고 TMI-192 범위에서도 추가하지 않았음을 재확인.
- 실행한 테스트와 결과: 소스·계약 정적 검색 및 git diff --check. 설명/기록만 변경하여 Gradle 미실행.
- 유지한 계약: merge 성공은 Identity 사용자/세션 변경·이벤트 저장 성공이며 하위 서비스 기록 이전 완료가 아님. 기존 Guest 승격·병합 계약 불변.
- 결정사항: 프론트가 전체 완료를 polling하려면 하위 서비스 처리 완료 수집과 조회 계약을 별도로 설계해야 함. 이번 요청은 API 구현 승인이 아님.
- 위험 요소: Learning Core/Billing 저장소의 개별 API 구현 여부는 미확인. 이벤트 전달 완료를 실제 처리 완료로 해석하면 안 됨.
- 예상 밖 변경: 없음. 기존 코드/문서 변경 보존, 외부 상태·Jira·배포·commit/push 변경 없음.
- Jira 댓글 초안(미등록): 종합 이전 완료 조회 API 미구현 확인. 코드 변경 및 테스트 실행 없음; 외부 consumer 완료 확인 계약은 별도 필요.
- 다음 작업: 필요 시 완료 상태 소유 서비스·서비스별 처리 결과 전달·프론트 조회 API 계약을 별도 결정.

## 2026-10-03 — Guest 병합 전체 이전 완료 조회 구현 계획 작성

<!-- codex-turn:01a10082-bb5b-7010-8e29-8daa0ade9886 -->

- 브랜치: feat/TMI-192-single-sns-account-recovery.
- Jira: TMI-192는 현재 브랜치 문맥만 해당. 본 조회 API는 별도 후속 범위이며 신규 이슈 미생성.
- 작업 목표: Identity 병합 이후 Learning Core/Billing 전체 완료 조회 API의 구현 계획과 쉬운 사용자 설명 작성.
- 변경 파일: docs/contracts/guest-merge-progress-query-implementation-plan.md(신규), docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 구현 내용: 제품 코드 변경 없음. 기존 merge transaction, owner fanout capture/core/delivery/publisher/완료 transaction, 공용 응답 DTO, LC consumer 인계서와 Stage 7 계약 확인. 기존 consumer commit 후 ACK 계약을 활용한 strict 204 기반 완료 증거·최소 progress 모델·mergeId·단건/목록 GET 계획 작성.
- 주요 확인 사실: 현재 publisher는 모든 2xx를 성공 처리함. LC 문서 계약은 direct local transaction commit 뒤 204, 202 금지. fanout eventId는 legacy outbox 객체와 별도로 생성되므로 실제 capture 반환 ID를 response/progress와 연결해야 함. Billing 정확한 ACK 코드와 실서비스 준수는 미확인.
- 계획: 추적 대상 USER_MERGED에 한해 strict ACK 및 progress/delivery/cursor 원자 확정, 응답 유실 시 동일 event retry, 필수 두 consumer만으로 합산. 본인 ACTIVE MEMBER 권한·404 비노출·no-store·분산 조회 한도·목록 복구·복수 기기·TTL·FIFO blocker·운영 확인 상태·앱 polling 제한 포함. 과거 자동 backfill 및 신규 callback은 기본안 제외.
- 실행한 테스트와 결과: 신규 계획서 상대 파일 링크 검사 통과, git diff --check 통과. 문서 작업이므로 Gradle/실외부 서비스 테스트 미실행.
- 유지한 계약: Guest merge/source·target/JWT/기존 이벤트 v1 필드/LOCAL/PASSWORD/계정 찾기 계약 변경 없음. 학습·Billing 업무는 각 저장소 소유. Guest upgrade는 이번 진행 조회 대상 아님. 기록 이전 증거 없이 완료 표시 금지.
- 결정사항: 권장 기본안으로 public GET 2개와 merge 전용 응답 mergeId 제안. callback 대신 기존 완료 ACK를 활용하되 LC/Billing 준비 확인이 출시 조건. 아직 구현 계약으로 확정·배포하지 않음.
- 위험 요소: 실제 consumer commit 범위/204 준수/기본 3초 timeout 내 완료 여부, progress retention·탈퇴 정리, 오래된 PUBLISHED에 strict ACK 증거 부재. worker/202 처리 구조라면 별도 receipt/callback 계약 재설계 필요.
- 예상 밖 변경: 없음. 기존 TMI-192 코드·문서 작업 보존. 외부 상태/Jira/commit/push/배포 변경 없음.
- Jira 댓글 초안(미등록): 병합 진행 조회 계획 작성. 실제 eventId 연계·commit ACK·progress 및 본인 조회 API/복구/출시 테스트 범위 정리. 코드 테스트 없음, consumer 실제 계약 검증 필요.
- 다음 작업: 사용자 계획 검토 및 필요 시 별도 Jira 생성 승인, LC/Billing 정확한 완료 계약 확인 후 후속 구현 착수.

## 2026-10-03 — 병합 완료 조회 계획의 현재 작업 식별 기록 보완

<!-- codex-turn:01a10084-f10d-7243-947c-3d0e79ed35df -->

- 브랜치: feat/TMI-192-single-sns-account-recovery.
- Jira: TMI-192는 현재 브랜치 문맥이며 조회 API는 별도 후속 범위. 신규 이슈 미생성.
- 작업 목표: 병합 전체 완료 조회 계획 작성 결과를 정확한 현재 turn 식별자로 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 본 작업의 계획서: docs/contracts/guest-merge-progress-query-implementation-plan.md.
- 구현 내용: 계획서 작성·사용자 설명 완료 사실 기록 보완. 실제 eventId와 연계한 progress, consumer commit 후 strict 204 확인, 회원용 단건/목록 조회, 응답 유실 복구·영역별 UI·출시 검증 계획 유지. 제품 API 미구현.
- 실행한 테스트와 결과: git diff --check 확인. 기록 보완만 수행하여 Gradle 미실행. 앞선 계획서 상대 링크 검사 통과.
- 유지한 계약: 기존 병합/승격/JWT/이벤트 계약 불변, 과거 WORKLOG 보존, 비밀정보 비기록.
- 결정사항: 추가 callback 없이 기존 완료 ACK 활용을 권장하되 실제 LC/Billing 준수 확인 선행. 현재는 문서상 제안이며 배포 계약 아님.
- 위험 요소: 외부 consumer의 commit 범위·204 응답·timeout 내 처리 및 세 서비스 E2E 미확인.
- 예상 밖 변경: 없음. 기존 코드/문서 작업 보존, Jira/배포/commit/push 변경 없음.
- 다음 작업: 계획 검토 후 별도 후속 이슈 승인 및 consumer 계약 확인, 구현 착수.

## 2026-10-03 — 첫 출시 Billing 미배포와 병합 진행 조회 계획의 공백 확인

<!-- codex-turn:01a1008e-be6d-7883-b31a-d35d2fd2d73f -->

- 브랜치: feat/TMI-192-single-sns-account-recovery.
- Jira: TMI-192는 브랜치 문맥이며 조회 API 후속 이슈는 미생성.
- 작업 목표: 첫 업데이트에서 Billing을 배포하지 않을 때 기존 조회 계획이 정상 완료를 지원하는지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 제품 변경 없음. 조회 계획의 필수 consumer 두 개 고정 snapshot 및 양쪽 consumer 준비 gate 재확인. Billing 미배포 정상 출시 모드는 아직 없음을 확인.
- 실행한 테스트와 결과: 문서 정적 검색 및 git diff --check. 분석·기록만 변경하여 Gradle 미실행.
- 유지한 계약: 실제 처리 증거 없이 Billing 완료 표시 금지, 기존 생성 작업의 필수 consumer를 flag 변경으로 축소하지 않음, 과거 Billing 자동 backfill 미승인.
- 결정사항: 보완 제안은 첫 출시에서 LC만 필수/Billing NOT_REQUIRED를 생성 시 기록하고 추후 신규 작업부터 양쪽 필수 적용. 일시 장애는 대상 제외와 구분. OwnerEventCore 불변식/delivery 생성/출시 gate 변경까지 필요하며 아직 구현·계획서에 적용하지 않음.
- 위험 요소: 첫 출시 데이터의 향후 Billing 필요 여부, 권리 관련 기능의 서버 차단/출시 범위 및 historical migration 정책 미확정.
- 예상 밖 변경: 없음. 기존 작업 보존, 외부/Jira/commit/push/배포 변경 없음.
- 다음 작업: 출시별 consumer 범위와 향후 Billing 데이터 이행 정책을 정하고 조회 계획·서버 간 이벤트 계약을 함께 보완.

## 2026-10-03 — Billing 미배포 조건 분석 식별 기록 보완

<!-- codex-turn:01a1008e-7eaa-7a73-bc5e-244a6b6c7dc6 -->

- 브랜치: feat/TMI-192-single-sns-account-recovery.
- Jira: TMI-192는 현재 브랜치 문맥. 별도 후속 이슈 미생성.
- 작업 목표: 첫 출시 Billing 미배포 조건 분석을 정확한 현재 turn 식별자로 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 기록 보완만 수행. 현재 계획은 두 consumer 필수이며 첫 출시 LC만 필수/Billing NOT_REQUIRED 제안은 아직 계획서·코드에 반영하지 않음.
- 실행한 테스트와 결과: git diff --check 확인. 기록만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 WORKLOG 보존, Billing 미처리를 완료로 표시하지 않음, 과거 병합 자동 재전송 금지.
- 결정사항: 생성 시 필수 consumer snapshot, 장애와 출시 대상 제외 구분, 이벤트 생성과 조회 계약 동시 보완 필요.
- 위험 요소: 과거 병합의 향후 Billing 이행 정책 미확정.
- 예상 밖 변경: 없음. 제품 코드/계획서/외부 상태/Jira/commit/push/배포 변경 없음.
- 다음 작업: 출시 범위와 이행 정책 확정 후 후속 계획 보완.

## 2026-10-03 — Learning Core 병합 완료 응답 유실 복구 설명

<!-- codex-turn:01a10082-bb5b-7010-8e29-8daa0ade9886 -->

- 브랜치: feat/TMI-192-single-sns-account-recovery.
- Jira: TMI-192는 브랜치 문맥. 조회 API는 별도 후속 범위.
- 작업 목표: LC commit 후 완료 응답 유실 시 재시도·중복 처리·조회 상태 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 제품 변경 없음. LC consumer 계약 4.2와 progress 계획의 ACK/응답 유실 규칙 확인. 동일 eventId/payload 재전송, 이전+inbox 원자 commit, PROCESSED 중복 요청의 204 재응답, 미확인 상태 보수적 표시를 설명.
- 실행한 테스트와 결과: 문서 정적 확인 및 git diff --check. 설명/기록만 변경하여 Gradle 미실행.
- 유지한 계약: 성공 응답은 consumer commit 뒤 전송, 같은 이벤트 재시도로 데이터 이전 중복 금지, 응답 유실을 rollback/실패 확정으로 간주하지 않음.
- 결정사항: 신규 완료 조회는 확인까지 PROCESSING, 재시도 소진/격리는 운영 확인 필요로 표시하는 계획 유지. 프론트 merge 재호출은 복구 수단이 아님.
- 위험 요소: 실제 Learning Core inbox 구현·원자성·중복 재응답 E2E 미검증. 무한 재시도나 반드시 즉시 복구된다고 보장하지 않음.
- 예상 밖 변경: 없음. 기존 제품 변경 보존, Jira/외부/배포/commit/push 변경 없음.
- 다음 작업: 실제 LC의 commit 후 응답 유실·동일 이벤트 재전송·Identity ACK 저장 실패 통합 테스트를 후속 구현에서 수행.

## 2026-10-03 — 완료 응답 유실 복구 설명 식별 기록 보완

<!-- codex-turn:01a10090-cc2e-73b1-9224-e0548e5606ba -->

- 브랜치: feat/TMI-192-single-sns-account-recovery.
- Jira: TMI-192는 브랜치 문맥이며 조회 API는 후속 범위.
- 작업 목표: Learning Core 완료 응답 유실 복구 설명을 정확한 현재 turn 식별자로 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 기록 보완만 수행. 동일 eventId/payload 재전송, LC의 이전+inbox 원자 commit, 완료된 중복 요청의 204 재응답, Identity 완료 기록 실패 복구 설명 유지. 제품 변경 없음.
- 실행한 테스트와 결과: git diff --check 확인. 기록만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 WORKLOG 보존, 중복 이전 금지, 실제 완료 확인 전 성공 추정 금지, 비밀정보 비기록.
- 결정사항: 프론트 merge 재호출 대신 상태 조회, 자동 재시도 소진 시 운영 확인 필요. 조회 상태는 아직 구현 계획임.
- 위험 요소: 실제 LC consumer 구현 및 응답 유실 통합 검증 미확인.
- 예상 밖 변경: 없음. 코드/계획서/Jira/외부 상태/배포/commit/push 변경 없음.
- 다음 작업: 후속 구현에서 consumer 멱등성과 양쪽 저장·응답 유실 복구 E2E 검증.

## 2026-10-03 — 병합 완료 조회 사용자 결정사항 정리

<!-- codex-turn:01a10092-723d-7583-9760-553b747aeeb3 -->

- 브랜치: feat/TMI-192-single-sns-account-recovery.
- Jira: TMI-192는 브랜치 문맥이며 병합 완료 조회는 별도 후속 범위.
- 작업 목표: 이미 정해진 첫 출시 조건과 사용자 제품 결정/개발 검증 책임 구분.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 코드 변경 없음. 계획서 기본값 및 이전 Billing 미배포 논의 확인. LC만 필수/Billing NOT_REQUIRED를 출시 조건으로 반영할 필요, 이전 중 화면 정책과 향후 Billing 과거 데이터 이행 방향을 사용자 확인 항목으로 정리.
- 실행한 테스트와 결과: 문서 정적 확인 및 git diff --check. 기록/설명 작업으로 Gradle 미실행.
- 유지한 계약: Billing 미처리를 완료로 표시하지 않음, 기존 병합을 미래 flag 변경으로 미완료 전환하지 않음, 과거 자동 재전송 금지, 서버 권한 검증 유지.
- 결정사항: 기본 회원 화면 사용 허용 및 미반영 영역 안내 권장. Billing 도입 시 기존 병합 데이터의 필요 이행은 별도 검토하며 자동 재병합하지 않는 방향 권장. 아직 신규 사용자 승인이나 계획서 수정으로 간주하지 않음.
- 위험 요소: 실제 consumer 완료/멱등성 검증, Billing 도입 시 과거 권리·사용 이력 반영 여부 미확정. 첫 출시 LC-only 제안은 계획서에 아직 미반영.
- 예상 밖 변경: 없음. 기존 코드/계획서 보존, Jira/외부 상태/commit/push/배포 변경 없음.
- 다음 작업: 사용자 제품 방향 확인 후 LC-only 조건을 포함해 조회 계획을 보완하고 후속 구현 범위 확정.

## 2026-10-03 — 프론트 책임 구분 및 첫 출시 LC-only 병합 조회 계획 반영

<!-- codex-turn:01a10082-bb5b-7010-8e29-8daa0ade9886 -->

- 브랜치: feat/TMI-192-single-sns-account-recovery.
- Jira: TMI-192는 브랜치 문맥. 병합 완료 조회는 별도 후속 범위, 이슈 미생성.
- 작업 목표: 사용자의 프론트 책임 지적 및 과거 병합 재실행 불필요 결정을 조회 계획에 반영.
- 변경 파일: docs/contracts/guest-merge-progress-query-implementation-plan.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 구현 내용: 문서만 수정. 화면·polling을 프론트 인계 사항으로 분리. 첫 출시 Learning Core만 필수/Billing NOT_REQUIRED, 도입 후 신규 작업만 두 서비스 필수, 과거 Billing backfill/replay 없음으로 계획 보완. core/progress/delivery의 생성 당시 필수 consumer snapshot 일치·LC-only의 Billing delivery/sequence 미생성·출시 profile 명시·응답 예제·aggregate·gate·테스트 항목을 함께 수정.
- 실행한 테스트와 결과: 계획서 상대 파일 링크 검사 통과, git diff --check 확인. 문서 작업이므로 Gradle 미실행.
- 유지한 계약: 실제 처리 없이 Billing 완료 표시 금지, 현재 flag로 과거 필수 consumer 변경 금지, 응답 유실 시 같은 event 멱등 재시도, 서버 권한 검증 유지. 제품 코드의 기존 동작은 변경하지 않음.
- 결정사항: Identity 계획 진행을 막는 추가 사용자 결정 없음. 과거 병합을 Billing 도입 때문에 다시 실행하지 않음. 이미 필수로 생성된 미완료 작업의 정상 retry와 과거 대상 제외 작업 backfill은 구분. 프론트 화면 구현은 서버 작업 완료 조건으로 요구하지 않음.
- 위험 요소: 현재 OwnerEventCore는 여전히 두 consumer 고정으로 후속 구현 필요. 첫 출시는 LC ACK/원자성/E2E 확인, Billing은 도입 시 별도 검증. 실제 API는 미구현 상태.
- 예상 밖 변경: 없음. 기존 코드/과거 기록 보존, 외부 상태/Jira/commit/push/배포 변경 없음.
- Jira 댓글 초안(미등록): 완료 조회 계획에 LC-only 첫 출시와 NOT_REQUIRED, 미래 신규 작업만 Billing 포함, 과거 재실행 없음 반영. 프론트 책임 분리. 문서 링크/공백 검사 통과, 구현·외부 E2E는 후속.
- 다음 작업: 필요 시 별도 후속 이슈 승인 후 계획에 따른 Identity 조회·publisher·consumer snapshot 구현 및 LC 계약 검증.

## 2026-10-03 — LC-only 조회 계획 반영 식별 기록 보완

<!-- codex-turn:01a10094-39c2-71f3-a0b8-7f6c264a27cc -->

- 브랜치: feat/TMI-192-single-sns-account-recovery.
- Jira: TMI-192는 브랜치 문맥이며 조회 API는 별도 후속 범위.
- 작업 목표: 프론트 책임 구분과 첫 출시 LC-only 계획 반영 결과를 정확한 현재 turn 식별자로 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 기록 보완만 수행. 앞선 계획서의 LC-only/Billing NOT_REQUIRED, 향후 신규 작업만 Billing 포함, 과거 병합 재실행 없음, 프론트 화면·polling 책임 분리 결정을 유지.
- 실행한 테스트와 결과: git diff --check 확인. 기록만 변경하여 Gradle 미실행.
- 유지한 계약: 과거 WORKLOG 보존, 생성 당시 필수 consumer 불변, 처리하지 않은 Billing을 완료로 표시하지 않음, 비밀정보 비기록.
- 결정사항: 추가 사용자 결정 없이 후속 구현 계획 진행 가능. API는 아직 미구현.
- 위험 요소: consumer 실제 ACK/멱등성·출시 E2E와 서버의 필수 consumer 모델 변경은 후속 작업.
- 예상 밖 변경: 없음. 제품 코드/계획서 추가 변경, Jira/외부 상태/commit/push/배포 작업 없음.
- 다음 작업: 별도 후속 구현 범위에 따라 Identity 조회 API 및 LC 계약 검증 진행.

## 2026-10-03 — Guest 병합 진행 조회 및 LC-only 완료 추적 구현

<!-- codex-turn:01a1009a-6e25-7f03-8c9a-5098353f662c -->

- 브랜치: feat/TMI-192-single-sns-account-recovery.
- Jira: TMI-192
- Jira 구분/작업: 브랜치의 TMI-192 본문을 공식 Atlassian MCP로 조회했다. 이번 병합 진행 조회는 사용자 승인한 별도 후속 범위이며 신규 이슈는 없다. Jira 생성/댓글/상태 변경/Done 처리 없음.
- 작업 목표: 승인된 guest-merge-progress-query-implementation-plan에 따라 회원용 단건/목록 조회, 실제 eventId 응답, LC-only 첫 출시 및 strict 204 완료 증거를 구현.
- 변경 파일: 신규 domain/auth/mergeprogress 패키지(요약 엔티티/repository/profile/store/query/controller/cursor/quota/privacy), GuestMergeResult, FirebaseGuestMergeResponse. 기존 FirebaseGuestMergeService/UseCase/DisabledUseCase/TransactionService/Controller, OwnerEventCore/FailureCode/CaptureService/PublishTransactionService/Publisher/Configuration/Properties, AuthErrorStatus, ReissueNoStoreFilter, UserWithdrawalIdentityReleaseTransactionService, IdentityOpenApiExamples, application.yml, .env.example 수정. 관련 Java 테스트/전체 Spring 테스트의 mock 인프라, tools/auth-test의 app/server/UI/테스트/README, API 신규 문서·계획서·프론트 가이드·fanout 런북/Stage7 보완, CURRENT_STATE/WORKLOG 갱신.
- 구현 내용: 실제 OwnerEventCore.eventId를 mergeId로 반환하고 source/세션/event/delivery/progress를 같은 Mongo 트랜잭션에서 생성. profile 명시/필수 publisher 및 endpoint 설정 검증. LC-only에서는 Billing sequence/delivery를 만들지 않음. 생성 profile/필수 집합 불변. 신규 GET 2개는 MEMBER JWT와 현재 ACTIVE MEMBER, 본인 target 조건·논리 만료를 검증. 동일 404로 타인/미존재/만료 숨김. keyset 목록·사용자/필터 바인딩 1시간 서버 저장 opaque cursor·분당30회 분산 공유 quota·Retry-After·no-store 구현.
- 완료/복구: tracked USER_MERGED의 204만 ACK로 인정, 다른 2xx는 ACK_CONTRACT_VIOLATION circuit pause. delivery/cursor/progress 갱신을 동일 Mongo 트랜잭션에서 처리, @Version CAS로 ACK 덮어쓰기 차단. lease 패자는 완료 못함. 응답 유실은 동일 eventId/payload 재전송, 기록·상태 누락은 503. publisher OFF/PAUSED/본인 또는 FIFO 선행 DEAD_LETTER/비활성 채널은 ACTION_REQUIRED. 완료 요약은 core/delivery TTL과 독립.
- 보관/탈퇴: 완료+30일 TTL, 미완료+90일 review/자동삭제 없음. 탈퇴는 현재 User 검사로 즉시 조회 차단. 기존 identity release에서 완료 요약 즉시 만료·cursor 제거, 미완료는 privacyCleanupRequested로 최소 참조 보존 후 ACK 완료 즉시 만료.
- 구현 보완: 기존 owner-event 두 transactional service의 final을 제거해 Spring CGLIB transaction proxy 가능하도록 했다. Spring advice 실패 rollback 호출 테스트 추가. 실제 DB 트랜잭션 rollback 검증을 대체하지 않는다.
- 실행한 테스트와 결과: 최종 ./gradlew clean test exportOpenApi 성공. Java 160 suites / 1108 tests, 실패/오류/skip 0. Node --test tools/auth-test/*.test.mjs 3/3 성공. git diff --check 통과. Gradle 캐시/로컬 테스트 소켓 권한은 승인받아 실행. 생성 OpenAPI는 build/generated/openapi 아래 출력, 32 operations / 31 paths 검증.
- 검증 상세: 실제 eventId/legacy null, LC-only allocation/NOT_REQUIRED, profile 변경 불변, ACK 양 순서/CAS, query IDOR·만료·paging/cursor 바인딩/공유 quota, FIFO/비활성/누락 상태, strict 비204 pause·동일 payload 재시도, 프록시 rollback, privacy, JWT/no-store/Retry-After, Swagger와 기존 인증 회귀.
- 유지한 계약: RS256/sub/audience, 기존 token 필드, signup/upgrade DTO, UserMerged v1 wire JSON, trial rebind·legacy ACK 의미, 게스트 병합 보안 검사, 외부 도메인 경계 유지. 실제 LC/Billing 이전 코드를 Identity에 추가하지 않음.
- 결정사항: 첫 출시 LC만 필수/Billing NOT_REQUIRED. 향후 신규 profile만 양쪽 필수, 과거 backfill/재실행 없음. 서버 저장 cursor로 별도 서명 secret 환경변수 불필요. capture OFF 후에도 기존 조회/엄격 ACK 지원. 실제 UI/polling은 프론트 책임.
- 위험 요소: 실제 Mongo replica set 원자성/동시성, LC의 전체 이전/inbox commit 뒤 204·source guard·탈퇴 경합·원격 데이터와 완료 일치 E2E 미수행. in-memory Mongo partial unique index 미지원으로 다중 UserMerged fixture에서 trial-only 인덱스만 테스트 환경에서 제외, 운영 인덱스는 유지. 조회 중 ACK 경합은 보수적 503일 수 있음. 외부 알람/대시보드·실제 배포는 미수행.
- 배포 전 확인: 신규 컬렉션 목록/TTL/review 인덱스, 기존 core/delivery unique·partial 인덱스, LC endpoint/workload 자격·commit/멱등204, Billing 완전 미설정 LC-only E2E 확인. 신규 플래그 기본 OFF, 명시 profile 필요. 프론트 회원 토큰 전환/mergeId 보관/목록 복구·polling 연결은 별도.
- 예상 밖 변경: 없음. 작업 시작부터 있던 TMI-192 단일 SNS/계정 찾기·문서·테스트 도구 변경은 보존했다. 전체 diff에는 이전 변경이 포함되며 이번 작업은 병합 추적 및 필요한 회귀/문서만 추가. commit/push/배포 하지 않음.
- Jira 댓글 초안(미등록): 별도 후속 Guest 병합 진행 조회 구현 완료. mergeprogress/Guest merge/owner publisher/config·API 문서·테스트 도구 변경, Java1108 및 Node3 테스트 통과. LC-only/Billing NOT_REQUIRED, strict204/CAS/IDOR/TTL/복구 적용. 실제 replica-set/LC 종단 검증·배포 전 인덱스/gate 확인 필요.
- 다음 작업: 사용자가 diff 검토 및 커밋/push 진행. 별도 후속 이슈 관리 여부와 실제 LC E2E/배포 설정을 확인한 뒤 추적 ON 및 프론트 인계.

## 2026-10-03 — Notion 로그인 가이드에 병합 진행 조회 계약 반영

<!-- codex-turn:01a100c3-c1e0-7003-92b6-d71ca75d5d27 -->

- 브랜치: develop (이번 작업 시작 시 확인; 브랜치 전환하지 않음).
- 작업 목표: 사용자가 지정한 크롬의 Notion 로그인 문서에 직전 구현된 병합 진행 조회 계약을 반영.
- 변경 대상: Notion 페이지 3d0dcc5dbaeb80528e0adad685a52dd7의 4.5, 병합 충돌 표, API 카탈로그 병합 응답, 8.6 및 신규 8.22~8.24. 로컬 파일은 docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md만 변경.
- 구현 내용: 문서 편집만 수행. mergeId 응답 예시, 회원용 GET 단건/목록, LC-only/Billing NOT_REQUIRED, 생성 시 대상 불변·과거 재실행 없음, strict204 및 응답 유실 복구, 오류·권한·no-store·30회/분·Retry-After·5/15초 polling·foreground2분 권고·보관/출시 gate 추가. 충돌만으로 merge 자동 재시도/Guest 인증 삭제 금지 명시.
- 범위: 이번 병합 관련 계약만 갱신했으며 다른 로그인 기능의 과거 기술 내용은 전면 동기화하지 않음. 기준일에 부분 갱신 범위를 명시. 기존 문서 본문과 출시 설정 부록 링크를 보존.
- 실행한 테스트와 결과: Notion 새로고침 후 새 절, mergeId 포함 JSON 예시, LC-only 문구 및 기준일 저장 확인. 편집 중 기준일 중복 문구를 정리하고 재로딩으로 확인. 제품 코드 변경 없어 Gradle/Node 테스트 미실행; 직전 구현 테스트 결과는 이번 실행과 구분해 문서에 기술. git diff --check 확인.
- 유지한 계약: 기존 토큰 필드/signup/upgrade 및 외부 이벤트 JSON 유지. 예시 인증값은 placeholder만 사용, 실제 credential/개인정보 기록 없음. 공유/접근 권한 변경 없음.
- 결정사항: 구현 완료와 실제 배포·활성화/LC E2E를 구분. 신규 추적 기본 OFF, 기본 회원 화면 허용·기록 영역 안내, 프론트 polling 별도 작업.
- 위험 요소: Notion의 다른 기존 절(정책·선택 동의·SNS 연결 등)은 이번 병합 조회 편집 범위 밖이며 현재 구현과 별도 대조 필요. 실제 배포·LC/Mongo 종단 검증 미수행.
- 예상 밖 변경: 없음. 로컬 제품 코드·브랜치·commit/push·배포·Jira 변경 없음.
- 다음 작업: 프론트는 갱신한 4.5/8.6/8.22~8.24 기준으로 연동, 기능 노출 전 대상 환경 배포 및 LC 종단 검증 확인.

## 2026-10-03 — 배포 후 병합 진행 추적 활성화 설정 안내

<!-- codex-turn:01a10137-a985-7210-b69e-57c1222cafc4 -->

- 브랜치: develop.
- 작업 목표: 사용자의 배포 완료 이후 신규 병합 진행 추적에 필요한 환경변수와 선행 조건 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md (기록만).
- 확인 내용: application.yml/.env.example, OwnerEventProperties, WorkloadJwtConfiguration/Properties, WorkloadIdentityPurpose, fanout/진행조회 계약 대조. 신규 capture 기본 OFF, 명시 LEARNING_CORE_ONLY profile 및 owner capture/LC publisher ON 필요. LC HTTPS endpoint와 workload JWT enabled/issuer 설정 필요, audience는 learning-core-user-merged로 코드 고정. 기존 Guest merge 기능 활성화도 유지해야 함.
- 실행한 테스트와 결과: 설정/코드 정적 확인 및 git diff --check. 제품 변경 없어 Gradle/Node 미실행. 실제 배포 환경변수·LC 상태는 조회하지 않음.
- 유지한 계약: Billing NOT_REQUIRED·과거 작업 재실행 없음, 실제 commit 후204만 완료, 기존 조회 자체에는 별도 활성화 플래그 없음.
- 결정사항: LC consumer 준비/멱등204 검증 후 연관 설정을 함께 적용하고 서버 재시작·재배포. 레거시 publisher와 신규 owner publisher는 별도이며 기존 미발행 작업 유무 확인 없이 레거시 설정을 끄지 않음.
- 위험 요소: 플래그 일부만 켜거나 profile/endpoint/workload 설정이 없으면 기동 실패 가능. 코드 배포만으로 새 progress가 생성되지 않음. 과거 미추적 작업은 자동 생성되지 않음.
- 예상 밖 변경: 없음. 환경 설정 변경·재배포·commit/push·Jira 작업 없음.
- 다음 작업: 사용자가 배포 환경 설정과 LC 준비 상태 확인 후 활성화, 신규 merge의 mergeId 및 회원 조회에서 LC COMPLETED/Billing NOT_REQUIRED 확인.

## 2026-10-03 — Learning Core 수정 필요 여부 설명

- 브랜치: develop.
- 작업 목표: 신규 Identity 진행 조회 도입에 따른 Learning Core 변경 필요성을 구분.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md (기록만).
- 확인 내용: 기존 learning-core-user-merged-consumer-handoff의 4.2 및 QA 조건에 이미 이전/guard/inbox PROCESSED 동일 트랜잭션 commit 후204, 동일 eventId/payload 중복204, 미확정425/503, 접수만 하는202 금지가 규정돼 있다. 이번 UserMerged wire JSON은 유지되고 완료 조회는 Identity 소유다.
- 실행한 테스트와 결과: 기존 계약 문서 정적 확인. 제품 변경 없어 테스트 미실행. Learning Core 실제 저장소·배포는 확인하지 않음.
- 유지한 계약: 기존 이벤트 endpoint/본문, commit 완료 확인, 응답 유실 시 동일 이벤트 멱등 처리.
- 결정사항: LC가 기존 계약대로 구현돼 있으면 새 callback/프론트 조회 API나 필드 추가 불필요. 200/202 성공·commit 이전 응답·중복 이전 구현이면 수정 필요.
- 위험 요소: 문서상의 요구를 LC 실제 구현 완료로 간주하지 않는다.
- 다음 작업: LC 담당자가 정상/중복204·원자적 이전·응답 유실 재시도 검증 후 Identity 신규 추적 활성화.

## 2026-10-03 — Learning Core 수정 필요 여부 설명 기록 보완

<!-- codex-turn:01a10139-c9ee-7ae2-91f6-d7e05be1f4e0 -->

- 브랜치: develop.
- 작업 목표: 이번 Learning Core 변경 필요성 설명에 현재 작업 식별자를 정확히 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 기록만 보완. 기존 consumer 계약의 commit 후204·동일 이벤트 중복204를 준수하면 신규 callback/조회 API/요청 필드 추가 불필요하다는 결론 유지.
- 실행한 테스트와 결과: git diff --check 확인. 기록만 변경하여 제품 테스트 미실행.
- 유지한 계약: 기존 UserMerged wire 형식과 멱등 처리, Identity 소유의 진행 조회 유지. 과거 WORKLOG 수정 없음.
- 결정사항: LC가200/202 또는 commit 전 성공을 반환한다면 수정 필요. 실제 구현 확인 전 활성화 완료로 간주하지 않음.
- 위험 요소: Learning Core 실제 저장소·배포 상태는 확인하지 않음.
- 다음 작업: LC 정상/중복204·원자적 이전·응답 유실 검증 뒤 Identity 추적 활성화.

## 2026-10-03 — Learning Core 실제 UserMerged 완료 계약 확인

<!-- codex-turn:01a1013b-91c3-7b12-a3d7-e8358e17b200 -->

- 브랜치: Identity develop, Learning Core develop. Jira 작업 없음.
- 작업 목표: 신규 병합 진행 조회에 필요한 LC 수정 여부를 실제 로컬 저장소 구현으로 확인.
- 변경 파일: Identity docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. LC 제품 코드와 기존 작업 기록 변경은 보존.
- 확인 내용: LC UserMergedInternalController는 consumer 반환 뒤 빈204 응답. UserMergedTransactionService는 ExamSession/ExamResult/ExamSummary owner 이전, source guard와 PROCESSED inbox를 UserOwnedTransactionExecutor의 Mongo TransactionTemplate에서 함께 처리한다. 동일 eventId/digest는 중복 mutation 없이 성공, 상이 digest는409. unknown commit/중복 경합은 inbox 재확인 후 성공 또는503으로 수렴한다. 시험 생성 미완료 등 선행조건은503/Retry-After로 처리한다.
- 설정: LC USER_MERGED_WRITER_ENABLED, USER_MERGED_SOURCE_DENY_ENABLED, USER_MERGED_CONSUMER_ENABLED 및 USER_MERGED_WORKLOAD_ISSUER/JWK_SET_URI가 필요하다. 기본 flag OFF, consumer만 단독 ON은 startup 검증 실패. 활성화 전 guard/index 준비와 구 writer drain 필요.
- 실행한 테스트와 결과: LC ./gradlew test --tests 'web.tosunsaeng.domain.usermerge.*' 성공. 정적 코드·설정 대조 및 Identity git diff --check 수행. 분석 범위의 focused test이며 전체 clean test와 실제 replica-set/staging E2E는 이번에 실행하지 않음.
- 유지한 계약: UserMerged v1 body/endpoint, commit 이후204 및 중복 멱등성, Identity 소유 진행 조회. 코드/API/배포/환경 변경 없음.
- 결정사항: 이번 조회 연동을 위한 LC 신규 callback/status API 추가는 필요하지 않다. 로컬 구현 확인과 실제 배포 준비 완료는 구분한다.
- 위험 요소: 현재 배포 revision/환경변수, 실제 Mongo 장애·응답 유실, 3초 read timeout 내 성능과 종단 완료 일치는 미검증.
- 예상 밖 변경: 없음. LC의 기존 WORKLOG 수정 및 Identity의 기존 기록 변경 보존. commit/push 없음.
- 다음 작업: 대상 환경 설정·인덱스·배포 revision 확인 후 신규 병합 및 동일 이벤트 재전송의204/Identity COMPLETED 종단 검증.

## 2026-10-03 — 테스트 Identity 병합 진행 추적 설정 활성화·재배포

<!-- codex-turn:01a10141-313e-77f3-a08a-d3cf5299ee61 -->

- 브랜치: develop. 별도 Jira 작업 없음.
- 작업 목표: 사용자 요청에 따라 필요한 병합 진행 설정을 켜고 테스트 서버 재배포·기동 검증.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 제품 코드 및 GitHub workflow 변경 없음.
- 외부 변경: 기존 테스트 서비스의 tosunsaeng-identity-test:17에서 개정18 생성·배포. OWNER_EVENT_USER_MERGED_CAPTURE_ENABLED=true, OWNER_EVENT_MERGE_PROGRESS_CAPTURE_ENABLED=true, OWNER_EVENT_MERGE_COMPLETION_PROFILE=LEARNING_CORE_ONLY, OWNER_EVENT_LEARNING_CORE_USER_MERGED_PUBLISHER_ENABLED=true, OWNER_EVENT_LEARNING_CORE_ENDPOINT=https://api-test.to-teacher.com/internal/v1/events/user-merged의 5개 값만 변경.
- 유지한 설정: 이미지52690d210e6a921d5378a9345a8d418ab65899c1, 기존 Secret 참조·역할·네트워크·desired1 유지. 편집 JSON을 재읽어 환경변수 외 필드 불변 확인. Guest merge/workload 인증 및 legacy publisher ON 유지, Billing user-merged/trial-rebind와 trial capture OFF 유지. main 운영 서버 미변경.
- 선행 확인: Learning Core 테스트 서비스 개정12 성공/running1/pending0/ALB정상 확인. writer/source-deny/consumer 모두 true, 테스트 Identity issuer/JWKS 사용으로 추가 재배포 불필요. Identity 배포 이미지의 application.yml에 새 progress/profile 설정 존재 및 현재 src/main과 diff 없음 확인.
- 테스트와 결과: ./gradlew test --tests '*OwnerEvent*Tests' --tests '*MergeProgress*Tests' 성공(9 suites/42 tests, 실패0/오류0). 신규 태스크 Started IdentityApplication 로그10:21:23Z, ALB Healthy, 공개 /actuator/health UP 확인. 19:24 KST ECS 배포 성공, 개정18/running1/pending0/태스크 실패0, 이전 개정17 태스크 종료 확인. git diff --check 통과.
- 유지한 계약: LC-only/Billing NOT_REQUIRED, 기존 UserMerged wire 및 JWT 계약 유지. 신규 병합부터 추적, 과거 병합 backfill/replay 없음. 실제 credential 원문 조회·변경·기록 없음.
- 결정사항: 사용자 요청 범위의 테스트 환경 설정 배포 완료. LC 이미 준비돼 있어 변경하지 않음. 새로운 IAM 권한이나 AWS 리소스 추가 없음.
- 위험·미확인: 지정 계정의 실제 병합→기록 이전→Identity COMPLETED, 동일 이벤트 응답 유실 재전송과 실환경 성능 E2E는 수행하지 않음. 인덱스는 코드의 auto-index-creation 및 정상 기동 확인까지이며 원격 DB 인덱스 목록 직접 대조는 미수행. 기동 성공을 종단 기능 검증으로 간주하지 않는다.
- 예상 밖 변경: 없음. 기존 로컬 기록 변경 보존, commit/push 없음.
- 다음 작업: 지정 테스트 계정으로 mergeId 응답·회원 조회 LC COMPLETED/Billing NOT_REQUIRED 및 기록 소유권 이전 확인. 후속 배포 시 현재 서비스 개정의 설정을 보존.

## 2026-10-05 — 최신 앱 버전 조회 API 존재 여부 확인

<!-- codex-turn:01a10b0b-dae8-7660-8531-24d899eef061 -->

- 브랜치: develop. Jira 작업 없음.
- 작업 목표: 현재 앱 버전 조회 API 구현 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md(기록만).
- 확인 내용: Identity src의 버전 관련 키워드 및 Controller 매핑에 앱 버전 조회 API 없음. Learning Core 로컬 src/main·docs/contracts에서도 관련 구현 검색 결과 없음. guest-app-update-transition-review.md 3.1~3.2에는 최신 출시 앱 버전 조회 요구사항만 기록돼 있으며 담당 서비스/경로/응답/갱신 방식 미확정.
- 테스트와 결과: 코드·문서 정적 검색, git diff --check. 제품 변경 없는 조회이므로 Gradle 미실행.
- 유지한 계약: 기존 인증·정책 조회/API 변경 없음. 정책 동의 버전과 앱 출시 버전을 구분.
- 결정사항: 확인한 로컬 저장소 기준 아직 미구현으로 안내. 원격 배포 API/다른 브랜치는 미조회.
- 위험 요소: 로컬에 없는 원격 변경은 이번 확인에 포함하지 않음.
- 다음 작업: 필요 시 공개 플랫폼별 최신 버전 API의 소유 서비스·응답·관리 방식을 확정 후 구현. 기존 변경 보존, commit/push/배포 없음.

## 2026-10-05 — 앱 버전 조회 API 구현 방향 제안

<!-- codex-turn:01a10b0b-dae8-7660-8531-24d899eef061 -->

- 브랜치: develop. Jira 작업 없음.
- 작업 목표: 최신 출시 앱 버전 조회 구현 계획을 쉽게 설명하고 미확정 결정을 구분.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md(기록만).
- 제안 내용: 인증 없는 GET /api/v1/app/version?platform=android|ios, 최신 출시 latestVersion와 플랫폼별 storeUrl 반환. 기존 응답 wrapper 준수. 앱이 설치 버전과 숫자 단위 비교하며 서버는 단말 버전을 추정하지 않음. 최초는 환경변수 기반으로 DB/관리자 API 없이 제공하고 설정 변경 후 재배포. 공개 라우트만 허용, 플랫폼/설정 검증과 무인증·응답·기존 보안 회귀 테스트 추가.
- 계약 구분: 최신 버전 조회와 최소 지원 버전/강제 업데이트는 별개. 이번 권장안은 안내 전용이고 minSupportedVersion/차단 정책은 추가 합의 전 도입하지 않음. Identity 공개 메타데이터 영역 배치는 제안으로만 두며 기존 소유 범위와 다르므로 서비스 배치 확정 후 구현.
- 위험 요소: 스토어 실제 배포 전 버전 올림, 단계적 출시·플랫폼별 출시 차이, 문자열 버전 비교, 조회 장애로 로그인 차단, 기존 구앱의 미호출. 빌드별 비교나 OTA는 초기 범위 밖.
- 테스트와 결과: 기존 요구사항 문서 대조·git diff --check. 제품 변경 없는 설계 설명으로 Gradle 미실행.
- 유지한 계약: 기존 API/JWT/동의 버전 유지. 신규 API는 미구현·미배포이며 경로/필드 모두 제안.
- 결정사항: 사용자 확인 필요 — 담당 서버, 최신 안내만 할지 강제 업데이트까지 할지, 환경변수 관리 허용. 스토어 URL·플랫폼별 실제 버전은 구현/배포 전 제공 필요.
- 다음 작업: 위 결정 후 상세 계약·계획 확정 및 구현. 기존 변경 보존, commit/push/외부 변경 없음.

## 2026-10-05 — 앱 버전 조회 계획 설명 작업 식별 기록 보완

<!-- codex-turn:01a10b0d-f3ea-7920-aa3a-5288a1a184dc -->

- 브랜치: develop. 작업 목표: 이번 계획 설명의 정확한 작업 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 과거 WORKLOG는 수정하지 않음.
- 내용: 플랫폼별 공개 최신 버전/storeUrl 조회, 앱 측 비교, 환경변수 관리 제안 유지. 담당 서버·강제 업데이트 포함 여부·관리 방식은 사용자 확정 전이며 구현하지 않음.
- 테스트와 결과: git diff --check 통과. 기록만 보완하여 제품 테스트 미실행.
- 유지한 계약: 기존 API/JWT 및 도메인 경계 변경 없음. 외부 시스템 변경 없음.
- 결정사항·위험: 제안을 확정 계약 또는 배포 완료로 간주하지 않음. Secret 기록 없음.
- 다음 작업: 사용자 결정 후 상세 계약 확정·구현.

## 2026-10-05 — 앱 버전 API와 프론트 업데이트 처리 책임 구분

<!-- codex-turn:01a10b15-b1d2-70e2-9608-6ce1ec1e977d -->

- 브랜치: develop. 작업 목표: 업데이트 UI/앱 진입 제한과 버전 정보 제공의 책임 구분.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md(기록만).
- 설명 내용: 서버는 플랫폼별 최신 출시 버전을 제공하고 프론트는 설치 버전 비교·안내·스토어 이동·앱 진입 제한을 구현한다. 이번 API 범위에 강제 차단 정책/최소 지원 버전/API 요청 차단을 자동 포함하지 않는다.
- 결정사항: 안내/강제 여부는 이번 최신 버전 조회 API 설계를 막는 필수 결정이 아니다. 추후 원격으로 최소 지원 기준을 관리하려면 별도 계약이 필요하며 단순 최신 버전과 최소 지원 버전은 다르다.
- 유지한 계약: 기존 API/JWT/도메인 경계 불변. 신규 API는 아직 미구현.
- 테스트와 결과: git diff --check 통과. 설명·기록만 변경하여 제품 테스트 미실행.
- 위험 요소: 프론트의 화면 제한은 서버 API의 구버전 요청 차단을 보장하지 않음. Secret 기록/외부 변경 없음.
- 다음 작업: 공개 최신 버전 조회 계약의 서비스 배치·필드·관리 방식 확정 후 구현. 기존 변경 보존.

## 2026-10-05 — TMI-136 하위 앱 버전 조회 이슈 생성안 준비

<!-- codex-turn:01a10b18-3fba-7c72-81ab-04792f774dae -->

- 브랜치: develop. Jira: TMI-136.
- 작업 목표: 사용자 요청으로 구현보다 Jira 등록을 먼저 진행하기 위해 생성안 준비.
- Jira 조회: 공식 Atlassian MCP로 TMI-136의 제목 sns 로그인, 유형 에픽, 상태 해야 할 일을 확인. 생성/수정/댓글/상태 변경 없음. 생성 내용 사전 승인 대기.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 직전 구현 준비는 소스 확인만 했으며 제품 파일 변경 없음.
- 제안 내용: TMI-136 하위 작업으로 [Identity] 플랫폼별 최신 앱 버전 공개 조회 API 구현. GET /api/v1/app/version?platform=android|ios, 기존 BaseResponse에 platform/latestVersion 반환, 무인증 GET만 공개, 환경변수 기반 관리, 플랫폼 검증과 미설정503, 계약·보안 회귀 및 전체 테스트.
- 제외 범위: 업데이트 안내/강제 차단/최소 지원 버전/스토어 이동/DB·관리자 API/배포. 실제 최신 버전은 별도 배포 설정으로 제공.
- 테스트와 결과: Jira/로컬 코드 확인 및 git diff --check. 제품 코드 변경 없어 테스트 미실행.
- 유지한 계약: 기존 인증·JWT·약관 API 유지. 이번 내용은 등록 초안이며 API 미구현.
- 결정사항·위험: AGENTS 규칙에 따라 사용자에게 생성 내용을 제시한 후 승인받아 생성. 상위 이슈 상태는 변경하지 않음.
- 다음 작업: 사용자 승인 후 하위 작업 생성 및 키 기록. commit/push 없음.

## 2026-10-05 — TMI-195 앱 버전 조회 API 이슈 생성

<!-- codex-turn:01a10b18-3fba-7c72-81ab-04792f774dae -->

- 브랜치: develop(이슈 등록만 수행, 전환 없음). Jira: TMI-195. 상위 TMI-136.
- 작업 목표: 사전 제시한 생성안에 대한 사용자 승인 후 Jira 작업 등록.
- Jira 작업: 공식 Atlassian MCP로 [Identity] 플랫폼별 최신 앱 버전 공개 조회 API 구현 생성, 재조회로 parent=TMI-136 확인. 상태는 기본 해야 할 일, 별도 상태 변경·댓글 등록 없음. 승인 여부: 사용자 승인 완료.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md(기록만).
- 내용: 공개 플랫폼별 최신 버전 조회, 환경변수 관리, 입력400/미설정503, 기존 응답·인증 유지 및 테스트/문서 범위 등록. 업데이트 UI·강제 차단·스토어 이동·DB/관리자 API·배포 제외.
- 테스트와 결과: 생성 결과 및 parent 재조회 확인, git diff --check. 제품 변경 없어 Gradle 미실행.
- 유지한 계약: 기존 제품 API/JWT 변경 없음. 구현·배포는 미수행.
- 위험 요소: 실제 출시 버전 값과 배포 설정은 후속 확인 필요. Secret 기록 없음.
- 다음 작업: 구현 요청 시 TMI-195 완료 조건을 기준으로 작업. 댓글 초안(미등록): 이슈 생성 및 상위 연결 확인 완료, 코드·테스트 변경 없음, 실제 버전 설정 후속 확인 필요.

## 2026-10-05 — TMI-195 생성 작업 식별 기록 보완

<!-- codex-turn:01a10b19-26f2-7d50-97ef-1b70781c2d17 -->

- 브랜치: develop. Jira: TMI-195, 상위 TMI-136.
- 작업 목표: 이번 Jira 생성 작업의 정확한 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 과거 기록 수정 없음.
- 작업 결과: 사용자 승인 후 공식 MCP로 이슈 생성 및 parent=TMI-136 확인 완료. 이번 보완에서 Jira 추가 변경 없음. 댓글 미등록, 상태 전환 없음.
- 테스트와 결과: git diff --check 통과. 기록만 변경하여 제품 테스트 미실행.
- 유지한 계약: 최신 버전 조회만 구현 예정, 제품 코드·배포·기존 API 변경 없음.
- 결정사항·위험: 실제 출시 버전과 배포 설정은 후속 확인. Secret 기록 없음.
- 다음 작업: 구현 요청 시 TMI-195 완료 조건 기준 개발.

## 2026-10-05 — TMI-195 최신 앱 버전 공개 조회 구현 완료

<!-- codex-turn:01a10b18-3fba-7c72-81ab-04792f774dae -->

- 브랜치: feat/TMI-195-app-version-api(시작 시 이미 선택돼 있었으며 전환 없음). Jira: TMI-195, 상위 TMI-136.
- 작업 목표: 승인된 Jira 완료 조건에 따라 최신 출시 버전 조회만 구현. 구현 전 공식 MCP로 TMI-195 재조회.
- 변경 파일: domain/appversion의 AppVersionController/Service/Response/ErrorStatus, SecurityConfig, IdentityOpenApiExamples, application.yml, .env.example, AppVersionTests, SecurityIntegrationTests, OpenApiSharingTests, docs/contracts/app-version-api.md 및 guest-app-update-transition-review.md, WORKLOG/CURRENT_STATE.
- 구현 내용: 무인증 exact GET /api/v1/app/version?platform=android|ios. BaseResponse result에 platform/latestVersion만 반환. 플랫폼 누락/빈 값/대소문자·공백·미지원·중복400 INVALID_REQUEST, 해당 플랫폼 설정 누락503 APP_VERSION_UNAVAILABLE. 성공·컨트롤러400/503 no-store. 다른 플랫폼 미설정은 정상 플랫폼을 차단하지 않음.
- 설정: APP_ANDROID_LATEST_VERSION/APP_IOS_LATEST_VERSION을 읽으며 기본 빈값. 공백 정리 후 숫자 major.minor.patch(최대32자, 불필요한 선행0 불가) 검증, 잘못된 비공백 설정은 기동 실패. 운영값을 임의 지정하지 않음. 버전 갱신은 스토어 출시 확인 후 환경변수 변경·재배포.
- 유지한 계약: 기존 BaseResponse/JWT/인증·약관 API 유지. GET 외 메서드와 다른 API는 기존 인증 요구 유지. 잘못된 Bearer 헤더는 기존401. 업데이트 안내/차단/minimumVersion/storeUrl/DB/관리자API/배포 추가 없음.
- 테스트와 결과: 집중 AppVersionTests/SecurityIntegrationTests 성공. 최초 전체1129 중 OpenAPI 고정 경로 개수 검증1 실패를 새 경로 및 DTO 기반 예시 추가로 수정. 최종 ./gradlew clean test exportOpenApi 성공, 161 suites/1129 tests 실패0/오류0/skip0. OpenAPI33 operations/32 paths, 공개 security·필수 platform enum·200/400/503·스키마/예시 검증 성공. git diff --check 통과. 실제 외부 Provider/DB 미호출.
- 결정사항: 최신 버전 조회만 구현, 강제 업데이트 판단은 포함하지 않음. 버전 미설정으로 전체 서버 기동은 차단하지 않음. API 문서에 설정/기동실패/미설정/스토어 배포 주의사항 기록.
- 위험·배포 전 확인: 플랫폼별 실제 출시 버전과 설정 형식 확인 필요. 기본값 그대로 배포하면 조회503. 실서버/실앱 smoke·실제 스토어 공개 검증은 미수행. 버전 비교·업데이트 UI는 프론트 책임.
- 예상 밖 변경: 없음. 시작부터 있던 WORKLOG/CURRENT_STATE 변경 보존. 전체 diff에 과거 기록 포함. commit/push/배포·Jira 상태/댓글 변경 없음.
- Jira 댓글 초안(미등록): 최신 버전 공개 조회 및 설정·보안·Swagger·문서 구현 완료. 전체1129 테스트/OpenAPI 생성 통과. 실제 버전 환경변수와 배포 후 smoke 확인 필요.
- 다음 작업: 사용자 diff 검토·커밋/push, 실제 버전 설정 후 배포 및 두 플랫폼 무인증 GET 검증. PR 병합 확인 전 Jira Done 전환하지 않음.

## 2026-10-05 — TMI-195 구현 작업 식별 기록 보완

<!-- codex-turn:01a10b1c-0cee-7291-b87e-5cb9ee74bbc2 -->

- 브랜치: feat/TMI-195-app-version-api. Jira: TMI-195.
- 작업 목표: 이번 구현 작업의 정확한 식별 기록 보완. 과거 WORKLOG 수정 없음.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 결과: 공개 플랫폼별 최신 앱 버전 조회, 환경변수 설정, 플랫폼400/미설정503, 보안 회귀·Swagger·계약 문서 구현 완료 상태 유지. 이번 보완은 기록만 변경.
- 테스트와 결과: 이번 구현에서 ./gradlew clean test exportOpenApi 성공(1129 tests, 실패0). 기록 보완 후 git diff --check 통과.
- 유지한 계약: 기존 JWT·BaseResponse·인증 경계 유지. 업데이트 정책·강제 차단·DB 기능 제외.
- 결정사항·위험: 실제 출시 버전 설정·배포 후 조회 검증 필요. Secret 기록 없음.
- 다음 작업: 사용자 diff 검토/커밋·배포 준비. 추가 Jira 변경·commit/push·배포 없음.

<!-- codex-turn:01a10b58-4c5c-7651-8cc9-0ad1de41fe34 -->

## 2026-10-05 문의 접수 사용자 식별 설계 보완
- 브랜치: develop. 작업 목표: 문의에 가능한 경우 사용자 ID를 저장하는 요구사항 정리.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 제품 구현 없이 설계 보완. 유효한 Identity Access Token의 검증된 sub로 문의 userId를 서버에서 기록하고 Guest/Member 구분도 서버 확인. 비로그인 접수 userId는 null이며 임의 userId 요청 필드는 추가하지 않음.
- 테스트: git diff --check. 설계 설명으로 실행 테스트 없음.
- 유지 계약: 클라이언트 userId 불신, 외부 Request Body 임의 userId 추가 금지, 토큰 원문 저장/로그 금지.
- 결정사항: 만료/잘못된 Bearer를 조용히 익명 전환하지 않음. 인증 불가 시 명시적 비로그인 문의 및 요청 추적번호로 조사 가능하게 제안.
- 위험 요소: 비로그인 문의의 계정 소유권은 확정할 수 없음. 실제 문의 API 구현/배포 없음.
- 다음 작업: 문의 구현 계획에 nullable 서버 식별 userId 반영. 외부 상태/Jira/commit/push 변경 없음, 기존 변경 보존.

## 2026-10-05 문의 접수 API 구현 계획서 작성
- 브랜치: develop. Jira: 이번 작업에 지정 없음.
- 작업 목표: 인증 실패/설정 문의 공통 접수 API의 상세 구현 계획 작성.
- 변경 파일: docs/contracts/support-inquiry-implementation-plan.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 계획서만 작성. 인증 주체별 userId, 명시적 익명 접수, 요청/오류 계약 제안, 원자 DB+outbox 저장, 멱등 범위/보관/동시성, 비동기 claim/lease/재시도, 개인정보/남용 보호, 구현 순서/테스트/출시 체크리스트 포함.
- 테스트: git diff --check 및 문서 근거 경로 존재 확인. 제품 변경 없어 Gradle 미실행.
- 유지 계약: 임의 body userId 불허, 기존 JWT/인증 보호 경계 유지, 토큰 원문/실제 Secret 비기록. 일반 고객지원 소유권 승인 전 구현 보류.
- 결정사항: 소유 서비스·알림 채널·회신/보관 정책 미확정으로 구분. 제한 수치는 확정 설정이 아닌 제안. 알림 정확히 한 번 전달 보장 없음.
- 위험 요소: 익명 스팸/개인정보/응답 유실/운영 조회 권한 및 공유 인프라 장애. 실제 배포 검증 없음.
- 예상 밖 변경: 없음. 기존 작업 기록 수정 보존, 제품 코드/외부 상태 변경 없음.
- 다음 작업: 사용자 계획 검토 후 미확정 정책 승인 및 필요 시 Jira 생성. commit/push/배포 미수행.

<!-- codex-turn:01a10b59-4d9a-7561-9cc5-0ce90d2da38d -->

### 2026-10-05 문의 접수 계획서 작업 식별 기록 보완
- 브랜치: develop. 작업 목표: 이번 계획서 작성의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 본 작업 계획서는 docs/contracts/support-inquiry-implementation-plan.md.
- 구현 내용: 계획서 작성 완료 상태 유지. 제품 구현/외부 변경 없음.
- 테스트: git diff --check. 문서 작업으로 실행 테스트 미수행.
- 유지 계약/결정사항: 인증에서 userId 식별, 비로그인 null, 비밀값 비기록 및 기존 기록 보존.
- 위험 요소: 담당 서비스/알림 채널/회신·보관 정책 미확정.
- 다음 작업: 사용자 계획 검토 후 정책 확정. commit/push/배포/Jira 변경 없음.

## 2026-10-06 — 전화번호 인증 후 가입 SNS 조회 API 안내

<!-- codex-turn:01a10ec5-9764-7970-b4cd-2b4aaa3dbc48 -->

- 브랜치: develop. 작업 목표: 계정 찾기 API 경로 및 의미 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md(기록만).
- 확인 내용: AccountRecoveryController의 POST /api/v1/auth/account-recovery/prepare 후 SMS PHONE 인증, POST /api/v1/auth/account-recovery/lookup에 recoveryId/firebaseIdToken 제출. 마지막 로그인 이력 조회가 아니라 인증한 번호에 연결된 가입 SNS 및 마스킹 이메일 안내.
- 테스트와 결과: 컨트롤러·응답·계약 정적 확인 및 git diff --check. 제품 변경 없어 테스트 미실행.
- 유지 계약: Identity 토큰 없는 계정 찾기, 회원 로그인/병합/토큰 발급 없음. 개인정보·Secret 비기록.
- 결정사항·위험: 배포 환경 활성화 여부는 이번에 확인하지 않음.
- 다음 작업: 프론트 prepare→전화 인증→lookup 순서 연동. 외부 변경 없음.

<!-- codex-turn:01a10ec8-ada9-7182-9907-f70c1c170a42 -->

## 2026-10-06 문의 접수 미확정 항목 선택지 안내
- 브랜치: develop. 작업 목표: 문의 접수 계획의 결정 항목별 장단점과 초기 권고안 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 계획서 결정 표 확인 후 서비스 배치/알림 채널/회신/운영 조회/보관/남용 제한 비교. Identity 독립 모듈과 대체 연락처, 팀 사용 채널 하나, 이메일 선택형 회신, 최소권한 조회, 보관 및 제한 수치의 승인 필요성을 설명.
- 테스트: 문서 대조 및 git diff --check. 설명 작업으로 실행 테스트 미수행.
- 유지 계약: 서버 식별 userId/비로그인 null, 외부 개인정보 최소화, 인증 도메인과 지원 도메인 분리. 실제 설정 변경 없음.
- 결정사항: 권고안은 사용자 확정이 아님. 법정 보관 의무와 내부 제안 수치를 구분하고 이메일 소유권 미검증 회신의 민감정보 제한 명시.
- 위험 요소: 공동 장애/공개 접수 남용/관리 접근 권한/보관 정책 확인 필요.
- 다음 작업: 사용자 선택 후 계획서 확정. 제품 코드/배포/Jira/commit/push 변경 없음, 기존 기록 보존.

<!-- codex-turn:01a10ed0-dfeb-7c01-9438-490a2f848b2d -->

## 2026-10-06 문의 알림/스팸/상세 조회 선택 반영
- 브랜치: develop. 목표: Slack·횟수 제한만·DB 상세 조회 선택 및 Slack 본문 표시 가능성 설명.
- 변경 파일: docs/contracts/support-inquiry-implementation-plan.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 문서에 선택사항 반영, 요약 없는 Slack 본문 표시 변경안 추가. 별도 이메일/userId/진단 필드 제외, 접수번호 포함 여부 미확정. 실제 전송 전 개인정보 안내·수신 범위·별도 Slack 보관/삭제 검토 필요.
- 테스트: git diff --check. 문서 변경만 수행하여 실행 테스트 미수행.
- 유지 계약: 본문을 로그에 남기지 않음, 서버 식별 userId 유지, 실제 외부 전송 및 설정 변경 없음.
- 위험/결정: DB 삭제로 Slack 사본은 삭제되지 않으며 webhook만으로 삭제 기능을 보장하지 않음. 원문 개인정보 자동 제거를 보장하지 않음.
- 다음 작업: 표시 범위/접수번호 및 남은 소유권·회신·보관 정책 확정. 코드/배포/Jira/commit/push 없음.

## 2026-10-06 문의 입력 화면 주의 문구 선택 반영
- 브랜치: develop. 목표: 별도 Slack 전달 안내 대신 입력 주의 문구를 두는 사용자 선택 반영.
- 변경 파일: docs/contracts/support-inquiry-implementation-plan.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용: 화면 Slack 명시 문구 제외, 비밀번호/인증번호/결제정보 비입력 주의 문구 제안. 개인정보 처리방침 및 실제 계약/위치별 필요한 고지·동의 검토는 유지.
- 테스트: git diff --check. 문서 변경으로 실행 테스트 없음.
- 유지 계약: 본문 외부 전송 미실행, 비밀값 비기록. 주의 문구로 법적 고지를 대체하지 않음.
- 결정/위험: 화면 간소화 선택이며 개인정보 처리 의무 면제의 의미가 아님. 실제 위탁/국외 이전 조건 미확인.
- 다음 작업: 남은 정책 확정 후 구현. 제품 코드/배포/외부 설정/Jira 변경 없음, 기존 기록 보존.

<!-- codex-turn:01a10ed2-c1af-7a10-9220-b5cc9c5fdd92 -->

### 2026-10-06 문의 입력 안내 작업 식별 기록 보완
- 브랜치: develop. 작업 목표: 이번 문의 입력 안내 선택 반영의 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 계획서의 Slack 별도 문구 제외 및 민감정보 입력 주의 문구 선택 상태 유지. 추가 제품 구현 없음.
- 테스트: git diff --check. 문서 보완으로 실행 테스트 미수행.
- 유지 계약: 개인정보 처리방침의 필요한 고지·동의 검토 유지, 비밀값 비기록 및 과거 기록 보존.
- 결정사항/위험: 화면 주의 문구만으로 법적 의무를 대체하지 않으며 실제 처리 조건은 미확인.
- 다음 작업: 남은 정책 확정 후 구현. 외부 전송/설정/배포/Jira/commit/push 없음.

## 2026-10-06 문의 접수 계획 현황 요약 안내
- 브랜치: develop. 목표: 현재 계획서의 선택사항과 미확정 사항을 구분해 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 확인 내용: Slack/횟수 제한만/DB 상세 조회/화면 주의 문구 선택과 본문 표시안, 인증 기반 userId/익명 null, DB 원자 저장/중복 방지/비동기 재시도 구조 요약. 문서의 잔여 Discord 일반화 표현보다 최신 Slack 선택을 기준으로 안내.
- 테스트: 계획서 정적 대조 및 git diff --check. 설명 작업으로 실행 테스트 미수행.
- 유지 계약: 개인정보 처리 고지 검토와 비밀값 비기록, 제품 코드/외부 전송 없음.
- 결정/위험: 소유 서비스·회신·보관·제한 수치·접수번호 Slack 표시 여부는 아직 미확정. 알림 중복/공유 장애 가능성 유지.
- 다음 작업: 남은 선택 확인 후 계획 확정 및 구현 요청. Jira/배포/commit/push 없음.

<!-- codex-turn:01a10ed4-6d36-7761-a710-70cac13f1f21 -->

### 2026-10-06 문의 계획 현황 설명 기록 보완
- 브랜치: develop. 작업 목표: 현재 계획 요약 안내에 이번 작업 식별자 연결.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 선택된 Slack·DB 조회·횟수 제한·주의 문구와 미확정 운영 정책을 구분한 설명 결과 유지. 제품 구현 없음.
- 테스트: git diff --check. 문서 보완으로 실행 테스트 미수행.
- 유지 계약: 서버 기반 사용자 식별, 익명 접수, 개인정보 보호 및 과거 기록 보존.
- 결정사항/위험: 보관·회신·서비스 소유권 등 정책은 아직 확정 전이며 추가 외부 전송 없음.
- 다음 작업: 사용자 선택 후 계획 확정. Jira/배포/commit/push 없음.

## 2026-10-06 — 계정 찾기 prepare 요청과 가입 중복 진입 순서 설명

<!-- codex-turn:01a10ed5-6c44-7792-8b50-b555497427f4 -->

- 브랜치: develop. 작업 목표: prepare 입력 및 기존 회원 여부 사전 확인 필요성 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md(기록만).
- 확인 내용: prepare는 사용자 입력 필드 없이 접수번호/만료시간 발급, 문서상 본문 {}. lookup은 prepare 뒤 PHONE 인증을 요구하며 MongoRecoveryStore가 proof.authTime을 접수 생성시각(초 단위)과 비교한다. Firebase SNS 계정에 번호 연결하는 동작과 계정 찾기용 전화 sign-in은 다름.
- 설명: 회원 여부를 미리 알 필요 없이 계정 찾기 진입 가능. lookup의 FOUND/NOT_FOUND/ACTION_REQUIRED로 안내. 가입 중 번호 중복 오류 뒤 계정 찾기로 유도할 수 있으나 필수 선행 단계는 아님. 새 prepare 이전 인증을 단순 토큰 refresh로 재사용하는 흐름은 보장되지 않음.
- 테스트와 결과: 컨트롤러·서비스·MongoRecoveryStore·resolver·프론트 계약 정적 확인, git diff --check. 제품 변경 없어 테스트 미실행.
- 유지한 계약: 기존 Identity Guest/MEMBER 세션 보존, 조회는 자동 회원 로그인/병합/가입 아님. Secret/사용자 번호 기록 없음.
- 결정사항·위험: 실제 중복 오류의 발생 주체를 Firebase link와 Identity PHONE_ALREADY_LINKED로 구분해야 함. 실앱·배포 설정 미조회.
- 다음 작업: 프론트 계정 찾기 진입과 prepare→PHONE 인증→lookup 연동 확인. 제품/외부/Jira 변경 없음.

<!-- codex-turn:01a10edd-1cc6-7250-94ba-2becea23bf6b -->

## 2026-10-06 문의 접수 권장안 사용자 승인 반영
- 브랜치: develop. 목표: 남은 권장안 승인에 따라 문의 계획 확정 상태 갱신.
- 변경 파일: docs/contracts/support-inquiry-implementation-plan.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용: Identity 독립 support 모듈/선택 이메일 수동 회신/문의90일·멱등7일/시간5건·일10건/Slack 접수번호+본문 확정. 앞선 DB 조회·CAPTCHA 제외·화면 주의 문구 유지. 잔여 Discord 및 소유권 미승인 표현 정리.
- 테스트: git diff --check, 잔여 미확정 표현 검토. 제품 미변경으로 Gradle 미실행.
- 유지 계약: userId 서버 식별, 개인정보·Secret 비기록, 문의와 인증 도메인 분리, 기존 작업 기록 보존.
- 결정/위험: 기능 승인과 실제 개인정보 고지·Slack 채널/권한/삭제 준비를 구분. 구현 요청이나 외부 전송 승인으로 확대 해석하지 않음.
- 다음 작업: 별도 구현 요청 후 개발. 실제 채널/담당자/Secret 및 법적·운영 준비는 출시 전 확인. 코드/배포/Jira/commit/push 변경 없음.

## 2026-10-06 — 가입 중 전화번호 중복 시 가입 계정 힌트 안내 검토

<!-- codex-turn:01a10ee5-22a2-7fe0-b0c9-4bc0d1d5a47b -->

- 브랜치: develop. 작업 목표: 새 SNS 가입 중 이미 사용 중인 전화번호의 가입 SNS 안내 UX 타당성 검토.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md(기록만).
- 검토 내용: 서버가 최근 전화 소유 증명을 검증한 이후 provider/마스킹 이메일을 안내하는 것은 기존 계정 찾기와 같은 경계에서 타당하다. 전화번호 입력 또는 클라이언트/Firebase link 중복 오류만으로 계정 힌트를 반환하면 안 된다. 정지 등은 기존 ACTION_REQUIRED 처리 유지, 자동 로그인/새 SNS 연결/병합 금지.
- 제안: 별도 계정 찾기 화면 대신 가입 화면에서 기존 lookup 결과를 안내하는 통합 UX. 재인증 부담 감소를 위해 전화 인증 시작 전 recovery prepare 발급 후 중복 시 PHONE proof 확보·lookup하는 안을 검토한다. 기존 SNS link용 토큰과 PHONE sign-in proof가 다르며 동일 SMS credential 재사용 가능 여부는 프론트 SDK/경합 조건 검증 필요.
- 테스트와 결과: signup/upgrade 중복 처리 위치 및 이전 확인 recovery 계약 대조, git diff --check. 코드 변경 없어 테스트 미실행.
- 유지 계약: 전화 인증이 기존 SNS 소유권/회원 로그인 자체를 대체하지 않음. 원문 이메일/사용자 ID/토큰 안내 없음. 기존 Guest Identity 및 진행 SNS 상태 보존 필요.
- 결정사항·위험: 방향 제안만 수행, 한 번의 SMS로 완료 가능하다고 보증하지 않음. 기존 API 그대로 연결할지 가입 proof 전용 계약이 필요한지는 상세 설계 전 미확정.
- 다음 작업: 사용자 방향 확정 후 가입 전화 인증·Firebase link 충돌·PHONE proof 확보 순서 설계 및 E2E 검증. 제품/배포/Jira 변경 없음.

## 2026-10-06 — recoveryId와 서버 사전 접수의 역할 설명

- 브랜치: develop. 작업 목표: 프론트 UUID로 prepare를 대체할 수 있는지 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md(기록만).
- 확인 내용: prepare는 UUID뿐 아니라 서버 시각 기준 Attempt(createdAt/expiresAt)를 저장하고 요청 한도를 적용한다. lookup은 저장된 시도 존재, PHONE authTime의 접수 이후 여부, 만료, proof 사용 및 동일 시도 재시도 조건을 검사한다.
- 결정사항: UUID 생성 위치 자체가 보안 근거는 아니다. 클라이언트 UUID를 서버가 사전 등록하는 설계는 가능하지만 사전 접수 역할은 남는다. 현재 API에 임의 UUID를 바로 보내면 INVALID_RECOVERY_REQUEST. prepare 이후 인증 요구는 현재 계약 선택이며 모든 가입에 본질적으로 필수인 단계는 아니다.
- 유지 계약: recoveryId 단독은 인증 증명이 아니고 Firebase 전화 proof 검증이 필수. 기존 API/코드 변경 없음.
- 테스트와 결과: 서비스/저장소 정적 확인, git diff --check. 설명만 수행하여 제품 테스트 미실행.
- 위험 요소: prepare 생략 또는 enrollment 재사용은 기존 최근 인증·재사용·재시도 조건을 재설계해야 하며 이번에는 승인·구현하지 않음.
- 다음 작업: 가입 중 계정 안내 통합 시 기존 enrollment 활용 또는 recovery 유지 여부 별도 설계. Secret/외부/Jira 변경 없음.

## 2026-10-06 — recoveryId 설명 작업 식별 기록 보완

<!-- codex-turn:01a10ee6-7ba7-7083-8fc0-470b2c34ef5a -->

- 브랜치: develop. 작업 목표: 이번 recoveryId 설명의 정확한 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 과거 WORKLOG 수정 없음.
- 내용·결정사항: UUID 생성 위치보다 서버 사전 접수·시각·만료·proof 사용 검증이 중요하다는 설명 유지. 임의 클라이언트 UUID로 현재 prepare를 대체할 수 없음.
- 유지한 계약: 기존 recovery API·전화 인증 검증 유지. 제품 코드/외부/Jira 변경 없음.
- 테스트와 결과: git diff --check 통과. 기록만 변경하여 제품 테스트 미실행.
- 위험 요소: enrollment 재사용 등 대안은 추가 설계 필요, 구현 완료로 간주하지 않음. Secret 기록 없음.
- 다음 작업: 가입 중 계정 안내 통합 흐름 별도 설계.

<!-- codex-turn:01a10eeb-3ff8-7ec3-9c98-19c000ac8d1c -->

## 2026-10-06 확정된 문의 접수 계획 재설명
- 브랜치: develop. 목표: 승인된 문의 접수 기본안을 사용자 흐름 중심으로 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용: 입력→서버 사용자 식별→DB 접수→Slack 접수번호/본문→DB 조사/선택 이메일 수동 회신 흐름, 요청 횟수와 보관 기간, 중복 방지/전송 실패 보존 설명. 계획 내용 추가 변경 없음.
- 테스트: 계획서 대조 및 git diff --check. 문서 설명으로 실행 테스트 미수행.
- 유지 계약: 인증 기반 userId와 익명 null, 본문 외 메타데이터 Slack 미첨부, 개인정보/Secret 비기록.
- 결정/위험: 아직 미구현, 실제 채널·담당자·개인정보 고지 및 Slack 삭제 운영은 출시 준비 사항. 문의가 자동 계정 복구 기능은 아님.
- 다음 작업: 별도 구현 요청 시 개발. 제품/외부 설정/Jira/commit/push 변경 없음, 기존 기록 보존.

<!-- codex-turn:01a10eee-8fe3-7cb0-bb7c-4198221c4961 -->

## 2026-10-06 문의 접수 Jira 생성안 승인 준비
- 브랜치: develop. 작업 목표: 확정 계획을 Jira 생성 초안으로 제시.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용: 제목·범위·완료 조건·제외 범위 및 기존 맥락의 TMI-136 상위 배치 제안. Jira: TMI-136 (상위 후보, 이번 조회/확정 전). 공식 Atlassian 도구 가용성 확인만 수행.
- 테스트: git diff --check. 문서 작업으로 실행 테스트 없음.
- 유지 계약: Jira 변경 전 내용 제시/승인 규칙, 개인정보·Secret 비기록.
- 결정/위험: 실제 Jira 생성/댓글/상태 변경 없음. 내용 및 상위 배치 승인 대기.
- 다음 작업: 승인 후 상위 이슈·유형 확인 및 생성. 제품/배포/commit/push 없음.

<!-- codex-turn:01a10ef1-07cb-7d42-93d8-903c20e323d2 -->

## 2026-10-06 TMI-197 문의 접수 Jira 생성 완료
- 브랜치: develop. Jira: TMI-197 (상위 TMI-136).
- 작업 목표: 승인된 문의 접수 API/Slack 알림 구현 이슈 생성.
- 변경 파일: docs/contracts/support-inquiry-implementation-plan.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 공식 Atlassian MCP로 TMI-136 에픽과 유형 확인, 사용자 승인 내용으로 작업 TMI-197 생성. 재조회로 parent TMI-136 및 해야 할 일 상태 확인.
- Jira 작업/승인: 생성 사전 내용 제시 후 사용자 승인 받음. 댓글 추가 없음, 별도 상태 전환 없음. 초기 상태 해야 할 일.
- 테스트: Jira 생성 결과/parent 재조회, git diff --check. 제품 구현 없어 Gradle 미실행.
- 유지 계약: 개인정보/Secret 비기록, 구현·실배포·외부 문의 전송 제외. 기존 작업 기록 보존.
- 결정/위험: 이슈 생성 완료는 기능 구현 완료가 아님. 실제 채널·권한·보관/삭제 운영은 출시 전 필요.
- 다음 작업: 구현 요청 시 TMI-197을 읽고 해당 키를 포함한 작업 브랜치에서 개발. commit/push/배포 없음.
- Jira 댓글 초안(미등록): 구현 계획 연결 및 생성 확인, 변경 파일은 계획서·기록, 검증은 parent 확인과 문서 검사, 제품 구현과 운영 준비는 후속 필요.

## 2026-10-06 — 가입 SNS 토큰과 계정 찾기 PHONE 토큰 구분

<!-- codex-turn:01a10ef3-6252-7f71-9ac7-f222a68ced6f -->

- 브랜치: develop. 작업 목표: 번호 중복 시 새 SNS 계정 토큰으로 lookup을 호출할 수 있는지 설명 및 이전 안내 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md(기록만).
- 확인 내용: AccountRecoveryService 및 Firebase verifier는 ACCOUNT_RECOVERY에 PHONE sign-in과 검증된 번호를 요구한다. 새 SNS A에 기존 번호 link가 실패하면 A 토큰은 계정 찾기 전화 증명이 되지 않으며 강제 refresh로 해결되지 않는다.
- 설명: 계정 찾기는 유효한 전화 credential로 별도의 Firebase phone sign-in을 완료하고 그 결과의 ID Token을 보내는 흐름이다. 동일 Firebase 프로젝트/tenant에서 번호가 B 사용자에 연결돼 있으면 그 번호 로그인은 B로 인증되는 것이며 A에 번호를 연결하는 동작이 아니다. Identity 회원 토큰 발급/자동 병합과 구분한다.
- 결정사항: prepare 선호출만으로 가입 토큰을 lookup에 사용할 수 있다는 인상을 정정. 기존 API 연계에는 프론트 PHONE sign-in 전환과 기존 SNS/Guest 상태 보존이 필요. 동일 SMS credential 재사용은 SDK 동작/오류·만료 조건 검증 필요.
- 테스트와 결과: verifier/service 정적 확인, git diff --check. 설명·기록만 변경하여 테스트 미실행.
- 유지한 계약: lookup의 PHONE 검증·기존 SNS 소유권 경계 유지. 사용자 번호/원문 토큰·Secret 미기록, 제품/배포/Jira 변경 없음.
- 위험 요소: 같은 Firebase Auth 인스턴스 sign-in은 현재 SNS 사용자를 바꿀 수 있음. secondary Auth 지원·credential 유효성·기존 enrollment 유지의 실앱 검증 미수행.
- 다음 작업: 가입 중 계정 안내를 통합하려면 프론트 인증 전환/격리 설계 확정 및 실제 SDK 검증.

## 2026-10-06 — 가입 중 계정 찾기의 SMS 재인증 필요성 설명

<!-- codex-turn:01a10ef8-100e-70d2-a8e6-231ffcf3414e -->

- 브랜치: develop. 작업 목표: SMS 입력 두 번과 Firebase link/sign-in 처리 구분.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md(기록만).
- 설명: 가입 인증 완료 뒤 새 prepare를 발급하고 별도 계정 찾기를 시작하는 기존 순차 흐름은 새 전화 인증이 필요할 수 있다. 다만 link 실패 후 유효한 동일 전화 credential을 PHONE sign-in에 활용할 수 있다면 SMS 입력을 반복하지 않는 통합 흐름이 가능하며, 두 인증 동작이 반드시 SMS 두 번을 뜻하지 않는다.
- 결정사항: 현재 실앱에서 SMS 1회 흐름이 구현·검증됐다고 주장하지 않는다. 사용 SDK의 credential 재사용·실패 후 상태·authTime 및 prepare 순서를 확인한 뒤 단일 SMS UX 설계가 필요하다.
- 테스트와 결과: 직전 코드 확인 결과 기반 설명, git diff --check. 제품 변경 없어 테스트 미실행.
- 유지 계약: PHONE proof 검증과 기존 SNS/Guest 인증 보존. prepare 선호출만으로 모든 조건이 충족되는 것은 아님.
- 위험 요소: credential 만료/재사용 제한, 기존 Firebase 인증 상태 변경은 미검증. Secret 기록·외부 변경 없음.
- 다음 작업: 실제 프론트 SDK 흐름 확인 후 SMS 1회 통합 가능 여부 검증. 제품/배포/Jira 변경 없음.

<!-- codex-turn:01a10efa-eec3-73b1-ba40-3825c82fb58d -->

## 2026-10-06 Slack 연동 사전 설정 안내
- 브랜치: develop. Jira: TMI-197.
- 작업 목표: Slack 설정과 개발/실연동 테스트의 선후관계 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용: 개발은 mock 기반 선행 가능, 실제 전송 전 비공개 채널/Slack app Incoming Webhooks 활성화/워크스페이스 설치 승인/채널 지정/웹훅 Secret 보관 필요 안내. 테스트·운영 분리 권고.
- 테스트: git diff --check. 설정 안내만 수행, Slack 실제 상태/설치/전송 미실행.
- 유지 계약: 웹훅 URL을 채팅/저장소/로그에 노출하지 않음. 최소 권한 및 실제 승인 절차 유지.
- 결정/위험: 관리자 설치 제한 가능, Secret 저장만으로 서버 주입이 완료되는 것은 아님. 문서상 설정명은 구현 시 확정.
- 다음 작업: 사용자 Slack 채널·앱 설정 후 안전한 Secret 등록과 별도 테스트. 코드/배포/Jira/commit/push 변경 없음.

## 2026-10-06 Slack 앱 생성 선택지 안내
- 브랜치: develop. Jira: TMI-197. 목표: 사용자가 제공한 Slack 생성 메뉴에서 필요한 선택 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용: 웹훅 알림용 Blank app 선택 및 후속 Incoming Webhooks 설정 안내. 사용자 제공 메뉴 기준이며 직접 브라우저 조회/앱 생성 없음.
- 테스트: git diff --check. 설명 작업으로 실행 테스트 미수행.
- 유지 계약: 불필요한 AI/명령어 권한 추가하지 않음, Secret 비기록.
- 결정/위험: 실제 화면 후속 구성/관리자 승인 여부 미확인.
- 다음 작업: 앱 생성 후 Incoming Webhooks 활성화 및 대상 채널 지정. 외부 변경/배포/Jira 변경 없음.

<!-- codex-turn:01a10efc-f6ef-72c3-901c-f3ad63f856e6 -->

### 2026-10-06 Slack 앱 생성 안내 기록 보완
- 브랜치: develop. Jira: TMI-197. 목표: 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 사용자 제공 메뉴 기준 Blank app 및 Incoming Webhooks 설정 안내 결과 유지. 제품/외부 변경 없음.
- 테스트: git diff --check. 기록 보완으로 실행 테스트 미수행.
- 유지 계약: Secret 비기록 및 기존 기록 보존.
- 결정사항/위험: 실제 Slack 생성/설치/권한 상태 미확인.
- 다음 작업: 사용자 앱 생성 후 웹훅 설정 진행. Jira/배포/commit/push 없음.

## 2026-10-06 Slack 설정 완료 보고 및 Secret 보관 안내
- 브랜치: develop. Jira: TMI-197. 목표: 사용자 웹훅 설정 완료 보고에 후속 보관 단계 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용: Slack 설정 완료는 사용자 보고로 기록하며 원격 검증하지 않음. AWS Secrets Manager의 테스트 전용 신규 Secret 이름과 키/값 저장 예시 안내. 서버 환경변수명은 구현 시 확정하며 저장만으로 연동되지 않음을 명시.
- 테스트: 관련 계획/설정 검색 및 git diff --check. 제품 변경 없이 실행 테스트 미수행.
- 유지 계약: 웹훅 URL 채팅/저장소/로그 비노출, 외부 Secret 생성/전송/권한 변경 없음.
- 결정/위험: 채널 실제 대상·권한·Secret 저장 상태 미확인. 기존 Secret 덮어쓰기 금지.
- 다음 작업: 사용자 안전한 Secret 저장 후 별도 구현 요청과 테스트 채널 검증. Jira/배포/commit/push 없음.

<!-- codex-turn:01a10efe-c718-7d50-9428-e0478975d850 -->

### 2026-10-06 Slack 설정 완료 후속 안내 기록 보완
- 브랜치: develop. Jira: TMI-197. 목표: 이번 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용: 사용자 Slack 설정 완료 보고 및 안전한 Secret 보관 안내 결과 유지. 실제 Secret 생성/서버 연결/전송 없음.
- 테스트: git diff --check. 문서 보완으로 실행 테스트 미수행.
- 유지 계약: 비밀값 비기록, 과거 작업 기록 보존.
- 결정/위험: 실제 채널·저장 상태는 미검증, 저장만으로 연동 완료되지 않음.
- 다음 작업: 사용자 보관 완료 후 구현 요청 및 테스트 연동. Jira/배포/commit/push 없음.

## 2026-10-06 문의 Slack 웹훅 Secret 등록 완료 보고
- 브랜치: develop. Jira: TMI-197. 작업 목표: 사용자 Secret 등록 완료 보고와 후속 단계 정리.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용: 사용자가 앞서 안내한 테스트 Secret 등록을 완료했다고 보고함. 실제 AWS 저장값·권한은 조회하지 않았으며 등록 검증 완료로 주장하지 않음.
- 테스트: git diff --check. 기록 작업으로 실행 테스트 미수행.
- 유지 계약: Secret 원문 조회/기록/출력 없음. 서버 구현·외부 전송·권한 변경 없음.
- 결정/위험: 저장과 ECS 주입/알림 연동은 별개. 기능은 미구현이며 실제 채널·권한·전송은 미검증.
- 다음 작업: 구현 요청 후 TMI-197 기반 개발, 별도 배포 단계에서 Secret 주입/최소권한 및 합성 문의 테스트. Jira/배포/commit/push 없음.

<!-- codex-turn:01a10f00-f666-79e3-9142-33cd134048a6 -->

### 2026-10-06 Secret 등록 완료 보고 기록 보완
- 브랜치: develop. Jira: TMI-197. 목표: 이번 작업의 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용: 사용자 Secret 등록 완료 보고 및 후속 구현/배포 연결 안내 결과 유지. 실제 저장값·권한 검증이나 외부 변경 없음.
- 테스트: git diff --check. 문서 보완으로 실행 테스트 미수행.
- 유지 계약: Secret 원문 비기록, 기존 기록 보존.
- 결정/위험: 등록 보고와 실제 서버 연동 검증은 별개이며 기능 미구현 상태.
- 다음 작업: 구현 승인 후 TMI-197 개발. Jira/배포/commit/push 없음.

<!-- codex-turn:01a10f04-4e9c-78a3-b53e-bc138986d9f0 -->

## 2026-10-06 TMI-197 문의 접수 API·Slack outbox 구현

- Jira: TMI-197. 브랜치: 사용자 변경 후 지정한 feat/TMI-197-support-inquiry에서 작업 유지. 목표: 승인된 문의 접수/DB 저장/Slack 본문 알림 구현.
- 변경 파일: domain/support 신규 모듈, 대응 support 테스트6개 파일, SecurityConfig, IdentityOpenApiExamples, application.yml, OpenApiSharingTests, support-inquiry-api.md, support-inquiry-implementation-plan.md, CURRENT_STATE.md, 본 WORKLOG.
- 구현: POST 선택 인증/현재 ACTIVE 계정에서 userId 결정, 익명 null, invalid Bearer 비강등. strict JSON/16KiB/10~2000자 검증, HMAC IP·주체별 quota/멱등 키, 문의+receipt+outbox+quota Mongo 트랜잭션, 동일키 재전송200/신규201, 충돌409/과다429/장애503 등. 문의90일·receipt7일 TTL. 공개 조회/첨부/자동 회신 없음.
- 알림: Slack receipt+전체 본문 plain_text만 전달, URL/멘션 해석 억제, webhook allowlist/redirect 금지/timeout, 원문 로그 금지. 5초 scheduler/60초 lease/최대5회 전송/Retry-After/지수형 간격, low-card counters. FAILED만 내부 CAS 수동 replay1회 및 문의 삭제 감사 primitive; 공개 endpoint/운영 runner는 없음.
- 테스트: compileJava 성공. 초기 mock 설정 및 OpenAPI operation 수 회귀 실패를 수정한 뒤 `./gradlew clean test exportOpenApi` 성공: tests1166/failures0/errors0/skipped6, 실행1160 통과. 이어 별도 opt-in SupportReplicaSetTests6/6 통과: 전체 rollback, commit 응답 유실,6동시 중복 접수, quota 거절 원자성, lease 재확보/stale CAS, 승인 재처리·삭제. 로컬 임시 Mongo만 사용, 합성 DB 삭제. 실제 Atlas/OAuth/Slack 테스트 없음.
- 유지 계약: RS256/JWKS/audience/기존 인증·병합 무변경, 외부 body userId 금지, Secret 원문 비기록, 테스트 외부 인프라 미사용. 사용자 선행 문서 변경은 보존, 예상 밖 제품 파일 변경 없음.
- 결정/위험: 기존 Access JWT의 sessionId/epoch 부재로 개별 로그아웃 즉시 판별은 구현하지 못함(사용자 고지). 인증 상태는 기존 JWT+현재 계정으로 검증. Slack at-least-once 중복 가능. 고정 UTC quota/NAT 오탐, HMAC 교체 시 멱등/제한 초기화, TTL 비동기, Slack 별도 삭제 필요. backlog gauge/경보/실행용 운영 CLI 미구성.
- 배포 전/다음 작업: flags OFF 유지. 별도 HMAC Secret·webhook 주입·최소권한, Mongo replica/index, 실제 ALB trusted proxy와 forward headers NONE 충돌 검사, 합성 Slack 전송, 개인정보 고지·채널 권한·보관/삭제 담당 확인. 사용자가 commit/push. 자동 배포/Slack 전송/Secret 조회/Jira 댓글·상태 변경 없음.
- Jira 댓글 초안(미등록): “문의 API·엄격 입력/선택 인증·멱등/제한·Mongo outbox·Slack 비동기 전송 및 제한된 수동 복구 primitive 구현. 변경: support 모듈/테스트, 보안·설정·OpenAPI·계약 문서. 회귀1160개 및 별도 replica 통합6개 통과. 잔여: 실제 Secret/프록시/Slack 검증, 개인정보 운영 및 경보 준비; 기존 JWT의 세션 단위 즉시 폐기 검증 한계.”

## 2026-10-06 문의 REFUND 분류 추가
- 브랜치: develop(현재 체크아웃 유지). 목표: 사용자 요청 환불 문의 분류 추가.
- 변경 파일: SupportRequest.java, SupportWebTests.java, docs/contracts/support-inquiry-api.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md.
- 구현: Category에 REFUND 추가, HTTP201 접수 및 서비스에 REFUND 전달 테스트. API 문서에 AUTH/GENERAL/REFUND와 환불 자동 실행이 아님을 명시.
- 테스트: `./gradlew clean test exportOpenApi` 성공. tests1167/failures0/errors0/skipped6, 실행1161 통과. Mongo opt-in6개는 이번 enum 변경에서 미실행. 생성 OpenAPI category enum 확인, git diff --check 통과.
- 유지 계약/결정: 기존 AUTH/GENERAL, 선택 인증·서버 userId·멱등·rate limit·저장·Slack 접수번호+본문 그대로. 결제/환불 실행은 범위 밖.
- 위험/배포 전: 배포한 구버전은 REFUND를 거절하므로 새 서버 배포 후 프론트 옵션을 활성화. 실제 인프라 및 Slack 미호출. 예상 밖 변경 없음, 기존 미커밋 WORKLOG 기록 보존.
- 다음 작업: 사용자 commit/push/재배포 및 프론트 category REFUND 연결. Jira 댓글 초안(미등록): “문의 REFUND 분류와 HTTP 검증·문서 추가, 전체1161 테스트 통과. 프론트 활성화 전 서버 배포 필요.” Jira/배포/commit/push 미수행.

<!-- codex-turn:01a10b58-4c5c-7651-8cc9-0ad1de41fe34 -->

## 2026-10-05 문의 접수 사용자 식별 설계 보완
- 브랜치: develop. 작업 목표: 문의에 가능한 경우 사용자 ID를 저장하는 요구사항 정리.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 제품 구현 없이 설계 보완. 유효한 Identity Access Token의 검증된 sub로 문의 userId를 서버에서 기록하고 Guest/Member 구분도 서버 확인. 비로그인 접수 userId는 null이며 임의 userId 요청 필드는 추가하지 않음.
- 테스트: git diff --check. 설계 설명으로 실행 테스트 없음.
- 유지 계약: 클라이언트 userId 불신, 외부 Request Body 임의 userId 추가 금지, 토큰 원문 저장/로그 금지.
- 결정사항: 만료/잘못된 Bearer를 조용히 익명 전환하지 않음. 인증 불가 시 명시적 비로그인 문의 및 요청 추적번호로 조사 가능하게 제안.
- 위험 요소: 비로그인 문의의 계정 소유권은 확정할 수 없음. 실제 문의 API 구현/배포 없음.
- 다음 작업: 문의 구현 계획에 nullable 서버 식별 userId 반영. 외부 상태/Jira/commit/push 변경 없음, 기존 변경 보존.

## 2026-10-05 문의 접수 API 구현 계획서 작성
- 브랜치: develop. Jira: 이번 작업에 지정 없음.
- 작업 목표: 인증 실패/설정 문의 공통 접수 API의 상세 구현 계획 작성.
- 변경 파일: docs/contracts/support-inquiry-implementation-plan.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 계획서만 작성. 인증 주체별 userId, 명시적 익명 접수, 요청/오류 계약 제안, 원자 DB+outbox 저장, 멱등 범위/보관/동시성, 비동기 claim/lease/재시도, 개인정보/남용 보호, 구현 순서/테스트/출시 체크리스트 포함.
- 테스트: git diff --check 및 문서 근거 경로 존재 확인. 제품 변경 없어 Gradle 미실행.
- 유지 계약: 임의 body userId 불허, 기존 JWT/인증 보호 경계 유지, 토큰 원문/실제 Secret 비기록. 일반 고객지원 소유권 승인 전 구현 보류.
- 결정사항: 소유 서비스·알림 채널·회신/보관 정책 미확정으로 구분. 제한 수치는 확정 설정이 아닌 제안. 알림 정확히 한 번 전달 보장 없음.
- 위험 요소: 익명 스팸/개인정보/응답 유실/운영 조회 권한 및 공유 인프라 장애. 실제 배포 검증 없음.
- 예상 밖 변경: 없음. 기존 작업 기록 수정 보존, 제품 코드/외부 상태 변경 없음.
- 다음 작업: 사용자 계획 검토 후 미확정 정책 승인 및 필요 시 Jira 생성. commit/push/배포 미수행.

<!-- codex-turn:01a10b59-4d9a-7561-9cc5-0ce90d2da38d -->

### 2026-10-05 문의 접수 계획서 작업 식별 기록 보완
- 브랜치: develop. 작업 목표: 이번 계획서 작성의 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 본 작업 계획서는 docs/contracts/support-inquiry-implementation-plan.md.
- 구현 내용: 계획서 작성 완료 상태 유지. 제품 구현/외부 변경 없음.
- 테스트: git diff --check. 문서 작업으로 실행 테스트 미수행.
- 유지 계약/결정사항: 인증에서 userId 식별, 비로그인 null, 비밀값 비기록 및 기존 기록 보존.
- 위험 요소: 담당 서비스/알림 채널/회신·보관 정책 미확정.
- 다음 작업: 사용자 계획 검토 후 정책 확정. commit/push/배포/Jira 변경 없음.

## 2026-10-06 — 전화번호 인증 후 가입 SNS 조회 API 안내

<!-- codex-turn:01a10ec5-9764-7970-b4cd-2b4aaa3dbc48 -->

- 브랜치: develop. 작업 목표: 계정 찾기 API 경로 및 의미 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md(기록만).
- 확인 내용: AccountRecoveryController의 POST /api/v1/auth/account-recovery/prepare 후 SMS PHONE 인증, POST /api/v1/auth/account-recovery/lookup에 recoveryId/firebaseIdToken 제출. 마지막 로그인 이력 조회가 아니라 인증한 번호에 연결된 가입 SNS 및 마스킹 이메일 안내.
- 테스트와 결과: 컨트롤러·응답·계약 정적 확인 및 git diff --check. 제품 변경 없어 테스트 미실행.
- 유지 계약: Identity 토큰 없는 계정 찾기, 회원 로그인/병합/토큰 발급 없음. 개인정보·Secret 비기록.
- 결정사항·위험: 배포 환경 활성화 여부는 이번에 확인하지 않음.
- 다음 작업: 프론트 prepare→전화 인증→lookup 순서 연동. 외부 변경 없음.

<!-- codex-turn:01a10ec8-ada9-7182-9907-f70c1c170a42 -->

## 2026-10-06 문의 접수 미확정 항목 선택지 안내
- 브랜치: develop. 작업 목표: 문의 접수 계획의 결정 항목별 장단점과 초기 권고안 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 계획서 결정 표 확인 후 서비스 배치/알림 채널/회신/운영 조회/보관/남용 제한 비교. Identity 독립 모듈과 대체 연락처, 팀 사용 채널 하나, 이메일 선택형 회신, 최소권한 조회, 보관 및 제한 수치의 승인 필요성을 설명.
- 테스트: 문서 대조 및 git diff --check. 설명 작업으로 실행 테스트 미수행.
- 유지 계약: 서버 식별 userId/비로그인 null, 외부 개인정보 최소화, 인증 도메인과 지원 도메인 분리. 실제 설정 변경 없음.
- 결정사항: 권고안은 사용자 확정이 아님. 법정 보관 의무와 내부 제안 수치를 구분하고 이메일 소유권 미검증 회신의 민감정보 제한 명시.
- 위험 요소: 공동 장애/공개 접수 남용/관리 접근 권한/보관 정책 확인 필요.
- 다음 작업: 사용자 선택 후 계획서 확정. 제품 코드/배포/Jira/commit/push 변경 없음, 기존 기록 보존.

<!-- codex-turn:01a10ed0-dfeb-7c01-9438-490a2f848b2d -->

## 2026-10-06 문의 알림/스팸/상세 조회 선택 반영
- 브랜치: develop. 목표: Slack·횟수 제한만·DB 상세 조회 선택 및 Slack 본문 표시 가능성 설명.
- 변경 파일: docs/contracts/support-inquiry-implementation-plan.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 문서에 선택사항 반영, 요약 없는 Slack 본문 표시 변경안 추가. 별도 이메일/userId/진단 필드 제외, 접수번호 포함 여부 미확정. 실제 전송 전 개인정보 안내·수신 범위·별도 Slack 보관/삭제 검토 필요.
- 테스트: git diff --check. 문서 변경만 수행하여 실행 테스트 미수행.
- 유지 계약: 본문을 로그에 남기지 않음, 서버 식별 userId 유지, 실제 외부 전송 및 설정 변경 없음.
- 위험/결정: DB 삭제로 Slack 사본은 삭제되지 않으며 webhook만으로 삭제 기능을 보장하지 않음. 원문 개인정보 자동 제거를 보장하지 않음.
- 다음 작업: 표시 범위/접수번호 및 남은 소유권·회신·보관 정책 확정. 코드/배포/Jira/commit/push 없음.

## 2026-10-06 문의 입력 화면 주의 문구 선택 반영
- 브랜치: develop. 목표: 별도 Slack 전달 안내 대신 입력 주의 문구를 두는 사용자 선택 반영.
- 변경 파일: docs/contracts/support-inquiry-implementation-plan.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용: 화면 Slack 명시 문구 제외, 비밀번호/인증번호/결제정보 비입력 주의 문구 제안. 개인정보 처리방침 및 실제 계약/위치별 필요한 고지·동의 검토는 유지.
- 테스트: git diff --check. 문서 변경으로 실행 테스트 없음.
- 유지 계약: 본문 외부 전송 미실행, 비밀값 비기록. 주의 문구로 법적 고지를 대체하지 않음.
- 결정/위험: 화면 간소화 선택이며 개인정보 처리 의무 면제의 의미가 아님. 실제 위탁/국외 이전 조건 미확인.
- 다음 작업: 남은 정책 확정 후 구현. 제품 코드/배포/외부 설정/Jira 변경 없음, 기존 기록 보존.

<!-- codex-turn:01a10ed2-c1af-7a10-9220-b5cc9c5fdd92 -->

### 2026-10-06 문의 입력 안내 작업 식별 기록 보완
- 브랜치: develop. 작업 목표: 이번 문의 입력 안내 선택 반영의 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 계획서의 Slack 별도 문구 제외 및 민감정보 입력 주의 문구 선택 상태 유지. 추가 제품 구현 없음.
- 테스트: git diff --check. 문서 보완으로 실행 테스트 미수행.
- 유지 계약: 개인정보 처리방침의 필요한 고지·동의 검토 유지, 비밀값 비기록 및 과거 기록 보존.
- 결정사항/위험: 화면 주의 문구만으로 법적 의무를 대체하지 않으며 실제 처리 조건은 미확인.
- 다음 작업: 남은 정책 확정 후 구현. 외부 전송/설정/배포/Jira/commit/push 없음.

## 2026-10-06 문의 접수 계획 현황 요약 안내
- 브랜치: develop. 목표: 현재 계획서의 선택사항과 미확정 사항을 구분해 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 확인 내용: Slack/횟수 제한만/DB 상세 조회/화면 주의 문구 선택과 본문 표시안, 인증 기반 userId/익명 null, DB 원자 저장/중복 방지/비동기 재시도 구조 요약. 문서의 잔여 Discord 일반화 표현보다 최신 Slack 선택을 기준으로 안내.
- 테스트: 계획서 정적 대조 및 git diff --check. 설명 작업으로 실행 테스트 미수행.
- 유지 계약: 개인정보 처리 고지 검토와 비밀값 비기록, 제품 코드/외부 전송 없음.
- 결정/위험: 소유 서비스·회신·보관·제한 수치·접수번호 Slack 표시 여부는 아직 미확정. 알림 중복/공유 장애 가능성 유지.
- 다음 작업: 남은 선택 확인 후 계획 확정 및 구현 요청. Jira/배포/commit/push 없음.

<!-- codex-turn:01a10ed4-6d36-7761-a710-70cac13f1f21 -->

### 2026-10-06 문의 계획 현황 설명 기록 보완
- 브랜치: develop. 작업 목표: 현재 계획 요약 안내에 이번 작업 식별자 연결.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 선택된 Slack·DB 조회·횟수 제한·주의 문구와 미확정 운영 정책을 구분한 설명 결과 유지. 제품 구현 없음.
- 테스트: git diff --check. 문서 보완으로 실행 테스트 미수행.
- 유지 계약: 서버 기반 사용자 식별, 익명 접수, 개인정보 보호 및 과거 기록 보존.
- 결정사항/위험: 보관·회신·서비스 소유권 등 정책은 아직 확정 전이며 추가 외부 전송 없음.
- 다음 작업: 사용자 선택 후 계획 확정. Jira/배포/commit/push 없음.

## 2026-10-06 — 계정 찾기 prepare 요청과 가입 중복 진입 순서 설명

<!-- codex-turn:01a10ed5-6c44-7792-8b50-b555497427f4 -->

- 브랜치: develop. 작업 목표: prepare 입력 및 기존 회원 여부 사전 확인 필요성 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md(기록만).
- 확인 내용: prepare는 사용자 입력 필드 없이 접수번호/만료시간 발급, 문서상 본문 {}. lookup은 prepare 뒤 PHONE 인증을 요구하며 MongoRecoveryStore가 proof.authTime을 접수 생성시각(초 단위)과 비교한다. Firebase SNS 계정에 번호 연결하는 동작과 계정 찾기용 전화 sign-in은 다름.
- 설명: 회원 여부를 미리 알 필요 없이 계정 찾기 진입 가능. lookup의 FOUND/NOT_FOUND/ACTION_REQUIRED로 안내. 가입 중 번호 중복 오류 뒤 계정 찾기로 유도할 수 있으나 필수 선행 단계는 아님. 새 prepare 이전 인증을 단순 토큰 refresh로 재사용하는 흐름은 보장되지 않음.
- 테스트와 결과: 컨트롤러·서비스·MongoRecoveryStore·resolver·프론트 계약 정적 확인, git diff --check. 제품 변경 없어 테스트 미실행.
- 유지한 계약: 기존 Identity Guest/MEMBER 세션 보존, 조회는 자동 회원 로그인/병합/가입 아님. Secret/사용자 번호 기록 없음.
- 결정사항·위험: 실제 중복 오류의 발생 주체를 Firebase link와 Identity PHONE_ALREADY_LINKED로 구분해야 함. 실앱·배포 설정 미조회.
- 다음 작업: 프론트 계정 찾기 진입과 prepare→PHONE 인증→lookup 연동 확인. 제품/외부/Jira 변경 없음.

<!-- codex-turn:01a10edd-1cc6-7250-94ba-2becea23bf6b -->

## 2026-10-06 문의 접수 권장안 사용자 승인 반영
- 브랜치: develop. 목표: 남은 권장안 승인에 따라 문의 계획 확정 상태 갱신.
- 변경 파일: docs/contracts/support-inquiry-implementation-plan.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용: Identity 독립 support 모듈/선택 이메일 수동 회신/문의90일·멱등7일/시간5건·일10건/Slack 접수번호+본문 확정. 앞선 DB 조회·CAPTCHA 제외·화면 주의 문구 유지. 잔여 Discord 및 소유권 미승인 표현 정리.
- 테스트: git diff --check, 잔여 미확정 표현 검토. 제품 미변경으로 Gradle 미실행.
- 유지 계약: userId 서버 식별, 개인정보·Secret 비기록, 문의와 인증 도메인 분리, 기존 작업 기록 보존.
- 결정/위험: 기능 승인과 실제 개인정보 고지·Slack 채널/권한/삭제 준비를 구분. 구현 요청이나 외부 전송 승인으로 확대 해석하지 않음.
- 다음 작업: 별도 구현 요청 후 개발. 실제 채널/담당자/Secret 및 법적·운영 준비는 출시 전 확인. 코드/배포/Jira/commit/push 변경 없음.

## 2026-10-06 — 가입 중 전화번호 중복 시 가입 계정 힌트 안내 검토

<!-- codex-turn:01a10ee5-22a2-7fe0-b0c9-4bc0d1d5a47b -->

- 브랜치: develop. 작업 목표: 새 SNS 가입 중 이미 사용 중인 전화번호의 가입 SNS 안내 UX 타당성 검토.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md(기록만).
- 검토 내용: 서버가 최근 전화 소유 증명을 검증한 이후 provider/마스킹 이메일을 안내하는 것은 기존 계정 찾기와 같은 경계에서 타당하다. 전화번호 입력 또는 클라이언트/Firebase link 중복 오류만으로 계정 힌트를 반환하면 안 된다. 정지 등은 기존 ACTION_REQUIRED 처리 유지, 자동 로그인/새 SNS 연결/병합 금지.
- 제안: 별도 계정 찾기 화면 대신 가입 화면에서 기존 lookup 결과를 안내하는 통합 UX. 재인증 부담 감소를 위해 전화 인증 시작 전 recovery prepare 발급 후 중복 시 PHONE proof 확보·lookup하는 안을 검토한다. 기존 SNS link용 토큰과 PHONE sign-in proof가 다르며 동일 SMS credential 재사용 가능 여부는 프론트 SDK/경합 조건 검증 필요.
- 테스트와 결과: signup/upgrade 중복 처리 위치 및 이전 확인 recovery 계약 대조, git diff --check. 코드 변경 없어 테스트 미실행.
- 유지 계약: 전화 인증이 기존 SNS 소유권/회원 로그인 자체를 대체하지 않음. 원문 이메일/사용자 ID/토큰 안내 없음. 기존 Guest Identity 및 진행 SNS 상태 보존 필요.
- 결정사항·위험: 방향 제안만 수행, 한 번의 SMS로 완료 가능하다고 보증하지 않음. 기존 API 그대로 연결할지 가입 proof 전용 계약이 필요한지는 상세 설계 전 미확정.
- 다음 작업: 사용자 방향 확정 후 가입 전화 인증·Firebase link 충돌·PHONE proof 확보 순서 설계 및 E2E 검증. 제품/배포/Jira 변경 없음.

## 2026-10-06 — recoveryId와 서버 사전 접수의 역할 설명

- 브랜치: develop. 작업 목표: 프론트 UUID로 prepare를 대체할 수 있는지 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md(기록만).
- 확인 내용: prepare는 UUID뿐 아니라 서버 시각 기준 Attempt(createdAt/expiresAt)를 저장하고 요청 한도를 적용한다. lookup은 저장된 시도 존재, PHONE authTime의 접수 이후 여부, 만료, proof 사용 및 동일 시도 재시도 조건을 검사한다.
- 결정사항: UUID 생성 위치 자체가 보안 근거는 아니다. 클라이언트 UUID를 서버가 사전 등록하는 설계는 가능하지만 사전 접수 역할은 남는다. 현재 API에 임의 UUID를 바로 보내면 INVALID_RECOVERY_REQUEST. prepare 이후 인증 요구는 현재 계약 선택이며 모든 가입에 본질적으로 필수인 단계는 아니다.
- 유지 계약: recoveryId 단독은 인증 증명이 아니고 Firebase 전화 proof 검증이 필수. 기존 API/코드 변경 없음.
- 테스트와 결과: 서비스/저장소 정적 확인, git diff --check. 설명만 수행하여 제품 테스트 미실행.
- 위험 요소: prepare 생략 또는 enrollment 재사용은 기존 최근 인증·재사용·재시도 조건을 재설계해야 하며 이번에는 승인·구현하지 않음.
- 다음 작업: 가입 중 계정 안내 통합 시 기존 enrollment 활용 또는 recovery 유지 여부 별도 설계. Secret/외부/Jira 변경 없음.

## 2026-10-06 — recoveryId 설명 작업 식별 기록 보완

<!-- codex-turn:01a10ee6-7ba7-7083-8fc0-470b2c34ef5a -->

- 브랜치: develop. 작업 목표: 이번 recoveryId 설명의 정확한 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 과거 WORKLOG 수정 없음.
- 내용·결정사항: UUID 생성 위치보다 서버 사전 접수·시각·만료·proof 사용 검증이 중요하다는 설명 유지. 임의 클라이언트 UUID로 현재 prepare를 대체할 수 없음.
- 유지한 계약: 기존 recovery API·전화 인증 검증 유지. 제품 코드/외부/Jira 변경 없음.
- 테스트와 결과: git diff --check 통과. 기록만 변경하여 제품 테스트 미실행.
- 위험 요소: enrollment 재사용 등 대안은 추가 설계 필요, 구현 완료로 간주하지 않음. Secret 기록 없음.
- 다음 작업: 가입 중 계정 안내 통합 흐름 별도 설계.

<!-- codex-turn:01a10eeb-3ff8-7ec3-9c98-19c000ac8d1c -->

## 2026-10-06 확정된 문의 접수 계획 재설명
- 브랜치: develop. 목표: 승인된 문의 접수 기본안을 사용자 흐름 중심으로 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용: 입력→서버 사용자 식별→DB 접수→Slack 접수번호/본문→DB 조사/선택 이메일 수동 회신 흐름, 요청 횟수와 보관 기간, 중복 방지/전송 실패 보존 설명. 계획 내용 추가 변경 없음.
- 테스트: 계획서 대조 및 git diff --check. 문서 설명으로 실행 테스트 미수행.
- 유지 계약: 인증 기반 userId와 익명 null, 본문 외 메타데이터 Slack 미첨부, 개인정보/Secret 비기록.
- 결정/위험: 아직 미구현, 실제 채널·담당자·개인정보 고지 및 Slack 삭제 운영은 출시 준비 사항. 문의가 자동 계정 복구 기능은 아님.
- 다음 작업: 별도 구현 요청 시 개발. 제품/외부 설정/Jira/commit/push 변경 없음, 기존 기록 보존.

<!-- codex-turn:01a10eee-8fe3-7cb0-bb7c-4198221c4961 -->

## 2026-10-06 문의 접수 Jira 생성안 승인 준비
- 브랜치: develop. 작업 목표: 확정 계획을 Jira 생성 초안으로 제시.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용: 제목·범위·완료 조건·제외 범위 및 기존 맥락의 TMI-136 상위 배치 제안. Jira: TMI-136 (상위 후보, 이번 조회/확정 전). 공식 Atlassian 도구 가용성 확인만 수행.
- 테스트: git diff --check. 문서 작업으로 실행 테스트 없음.
- 유지 계약: Jira 변경 전 내용 제시/승인 규칙, 개인정보·Secret 비기록.
- 결정/위험: 실제 Jira 생성/댓글/상태 변경 없음. 내용 및 상위 배치 승인 대기.
- 다음 작업: 승인 후 상위 이슈·유형 확인 및 생성. 제품/배포/commit/push 없음.

<!-- codex-turn:01a10ef1-07cb-7d42-93d8-903c20e323d2 -->

## 2026-10-06 TMI-197 문의 접수 Jira 생성 완료
- 브랜치: develop. Jira: TMI-197 (상위 TMI-136).
- 작업 목표: 승인된 문의 접수 API/Slack 알림 구현 이슈 생성.
- 변경 파일: docs/contracts/support-inquiry-implementation-plan.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 수행 내용: 공식 Atlassian MCP로 TMI-136 에픽과 유형 확인, 사용자 승인 내용으로 작업 TMI-197 생성. 재조회로 parent TMI-136 및 해야 할 일 상태 확인.
- Jira 작업/승인: 생성 사전 내용 제시 후 사용자 승인 받음. 댓글 추가 없음, 별도 상태 전환 없음. 초기 상태 해야 할 일.
- 테스트: Jira 생성 결과/parent 재조회, git diff --check. 제품 구현 없어 Gradle 미실행.
- 유지 계약: 개인정보/Secret 비기록, 구현·실배포·외부 문의 전송 제외. 기존 작업 기록 보존.
- 결정/위험: 이슈 생성 완료는 기능 구현 완료가 아님. 실제 채널·권한·보관/삭제 운영은 출시 전 필요.
- 다음 작업: 구현 요청 시 TMI-197을 읽고 해당 키를 포함한 작업 브랜치에서 개발. commit/push/배포 없음.
- Jira 댓글 초안(미등록): 구현 계획 연결 및 생성 확인, 변경 파일은 계획서·기록, 검증은 parent 확인과 문서 검사, 제품 구현과 운영 준비는 후속 필요.

## 2026-10-06 — 가입 SNS 토큰과 계정 찾기 PHONE 토큰 구분

<!-- codex-turn:01a10ef3-6252-7f71-9ac7-f222a68ced6f -->

- 브랜치: develop. 작업 목표: 번호 중복 시 새 SNS 계정 토큰으로 lookup을 호출할 수 있는지 설명 및 이전 안내 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md(기록만).
- 확인 내용: AccountRecoveryService 및 Firebase verifier는 ACCOUNT_RECOVERY에 PHONE sign-in과 검증된 번호를 요구한다. 새 SNS A에 기존 번호 link가 실패하면 A 토큰은 계정 찾기 전화 증명이 되지 않으며 강제 refresh로 해결되지 않는다.
- 설명: 계정 찾기는 유효한 전화 credential로 별도의 Firebase phone sign-in을 완료하고 그 결과의 ID Token을 보내는 흐름이다. 동일 Firebase 프로젝트/tenant에서 번호가 B 사용자에 연결돼 있으면 그 번호 로그인은 B로 인증되는 것이며 A에 번호를 연결하는 동작이 아니다. Identity 회원 토큰 발급/자동 병합과 구분한다.
- 결정사항: prepare 선호출만으로 가입 토큰을 lookup에 사용할 수 있다는 인상을 정정. 기존 API 연계에는 프론트 PHONE sign-in 전환과 기존 SNS/Guest 상태 보존이 필요. 동일 SMS credential 재사용은 SDK 동작/오류·만료 조건 검증 필요.
- 테스트와 결과: verifier/service 정적 확인, git diff --check. 설명·기록만 변경하여 테스트 미실행.
- 유지한 계약: lookup의 PHONE 검증·기존 SNS 소유권 경계 유지. 사용자 번호/원문 토큰·Secret 미기록, 제품/배포/Jira 변경 없음.
- 위험 요소: 같은 Firebase Auth 인스턴스 sign-in은 현재 SNS 사용자를 바꿀 수 있음. secondary Auth 지원·credential 유효성·기존 enrollment 유지의 실앱 검증 미수행.
- 다음 작업: 가입 중 계정 안내를 통합하려면 프론트 인증 전환/격리 설계 확정 및 실제 SDK 검증.

## 2026-10-06 — 가입 중 계정 찾기의 SMS 재인증 필요성 설명

<!-- codex-turn:01a10ef8-100e-70d2-a8e6-231ffcf3414e -->

- 브랜치: develop. 작업 목표: SMS 입력 두 번과 Firebase link/sign-in 처리 구분.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md(기록만).
- 설명: 가입 인증 완료 뒤 새 prepare를 발급하고 별도 계정 찾기를 시작하는 기존 순차 흐름은 새 전화 인증이 필요할 수 있다. 다만 link 실패 후 유효한 동일 전화 credential을 PHONE sign-in에 활용할 수 있다면 SMS 입력을 반복하지 않는 통합 흐름이 가능하며, 두 인증 동작이 반드시 SMS 두 번을 뜻하지 않는다.
- 결정사항: 현재 실앱에서 SMS 1회 흐름이 구현·검증됐다고 주장하지 않는다. 사용 SDK의 credential 재사용·실패 후 상태·authTime 및 prepare 순서를 확인한 뒤 단일 SMS UX 설계가 필요하다.
- 테스트와 결과: 직전 코드 확인 결과 기반 설명, git diff --check. 제품 변경 없어 테스트 미실행.
- 유지 계약: PHONE proof 검증과 기존 SNS/Guest 인증 보존. prepare 선호출만으로 모든 조건이 충족되는 것은 아님.
- 위험 요소: credential 만료/재사용 제한, 기존 Firebase 인증 상태 변경은 미검증. Secret 기록·외부 변경 없음.
- 다음 작업: 실제 프론트 SDK 흐름 확인 후 SMS 1회 통합 가능 여부 검증. 제품/배포/Jira 변경 없음.

<!-- codex-turn:01a10efa-eec3-73b1-ba40-3825c82fb58d -->

## 2026-10-06 Slack 연동 사전 설정 안내
- 브랜치: develop. Jira: TMI-197.
- 작업 목표: Slack 설정과 개발/실연동 테스트의 선후관계 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용: 개발은 mock 기반 선행 가능, 실제 전송 전 비공개 채널/Slack app Incoming Webhooks 활성화/워크스페이스 설치 승인/채널 지정/웹훅 Secret 보관 필요 안내. 테스트·운영 분리 권고.
- 테스트: git diff --check. 설정 안내만 수행, Slack 실제 상태/설치/전송 미실행.
- 유지 계약: 웹훅 URL을 채팅/저장소/로그에 노출하지 않음. 최소 권한 및 실제 승인 절차 유지.
- 결정/위험: 관리자 설치 제한 가능, Secret 저장만으로 서버 주입이 완료되는 것은 아님. 문서상 설정명은 구현 시 확정.
- 다음 작업: 사용자 Slack 채널·앱 설정 후 안전한 Secret 등록과 별도 테스트. 코드/배포/Jira/commit/push 변경 없음.

## 2026-10-06 Slack 앱 생성 선택지 안내
- 브랜치: develop. Jira: TMI-197. 목표: 사용자가 제공한 Slack 생성 메뉴에서 필요한 선택 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용: 웹훅 알림용 Blank app 선택 및 후속 Incoming Webhooks 설정 안내. 사용자 제공 메뉴 기준이며 직접 브라우저 조회/앱 생성 없음.
- 테스트: git diff --check. 설명 작업으로 실행 테스트 미수행.
- 유지 계약: 불필요한 AI/명령어 권한 추가하지 않음, Secret 비기록.
- 결정/위험: 실제 화면 후속 구성/관리자 승인 여부 미확인.
- 다음 작업: 앱 생성 후 Incoming Webhooks 활성화 및 대상 채널 지정. 외부 변경/배포/Jira 변경 없음.

<!-- codex-turn:01a10efc-f6ef-72c3-901c-f3ad63f856e6 -->

### 2026-10-06 Slack 앱 생성 안내 기록 보완
- 브랜치: develop. Jira: TMI-197. 목표: 현재 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현 내용: 사용자 제공 메뉴 기준 Blank app 및 Incoming Webhooks 설정 안내 결과 유지. 제품/외부 변경 없음.
- 테스트: git diff --check. 기록 보완으로 실행 테스트 미수행.
- 유지 계약: Secret 비기록 및 기존 기록 보존.
- 결정사항/위험: 실제 Slack 생성/설치/권한 상태 미확인.
- 다음 작업: 사용자 앱 생성 후 웹훅 설정 진행. Jira/배포/commit/push 없음.

## 2026-10-06 Slack 설정 완료 보고 및 Secret 보관 안내
- 브랜치: develop. Jira: TMI-197. 목표: 사용자 웹훅 설정 완료 보고에 후속 보관 단계 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용: Slack 설정 완료는 사용자 보고로 기록하며 원격 검증하지 않음. AWS Secrets Manager의 테스트 전용 신규 Secret 이름과 키/값 저장 예시 안내. 서버 환경변수명은 구현 시 확정하며 저장만으로 연동되지 않음을 명시.
- 테스트: 관련 계획/설정 검색 및 git diff --check. 제품 변경 없이 실행 테스트 미수행.
- 유지 계약: 웹훅 URL 채팅/저장소/로그 비노출, 외부 Secret 생성/전송/권한 변경 없음.
- 결정/위험: 채널 실제 대상·권한·Secret 저장 상태 미확인. 기존 Secret 덮어쓰기 금지.
- 다음 작업: 사용자 안전한 Secret 저장 후 별도 구현 요청과 테스트 채널 검증. Jira/배포/commit/push 없음.

<!-- codex-turn:01a10efe-c718-7d50-9428-e0478975d850 -->

### 2026-10-06 Slack 설정 완료 후속 안내 기록 보완
- 브랜치: develop. Jira: TMI-197. 목표: 이번 작업 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용: 사용자 Slack 설정 완료 보고 및 안전한 Secret 보관 안내 결과 유지. 실제 Secret 생성/서버 연결/전송 없음.
- 테스트: git diff --check. 문서 보완으로 실행 테스트 미수행.
- 유지 계약: 비밀값 비기록, 과거 작업 기록 보존.
- 결정/위험: 실제 채널·저장 상태는 미검증, 저장만으로 연동 완료되지 않음.
- 다음 작업: 사용자 보관 완료 후 구현 요청 및 테스트 연동. Jira/배포/commit/push 없음.

## 2026-10-06 문의 Slack 웹훅 Secret 등록 완료 보고
- 브랜치: develop. Jira: TMI-197. 작업 목표: 사용자 Secret 등록 완료 보고와 후속 단계 정리.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용: 사용자가 앞서 안내한 테스트 Secret 등록을 완료했다고 보고함. 실제 AWS 저장값·권한은 조회하지 않았으며 등록 검증 완료로 주장하지 않음.
- 테스트: git diff --check. 기록 작업으로 실행 테스트 미수행.
- 유지 계약: Secret 원문 조회/기록/출력 없음. 서버 구현·외부 전송·권한 변경 없음.
- 결정/위험: 저장과 ECS 주입/알림 연동은 별개. 기능은 미구현이며 실제 채널·권한·전송은 미검증.
- 다음 작업: 구현 요청 후 TMI-197 기반 개발, 별도 배포 단계에서 Secret 주입/최소권한 및 합성 문의 테스트. Jira/배포/commit/push 없음.

<!-- codex-turn:01a10f00-f666-79e3-9142-33cd134048a6 -->

### 2026-10-06 Secret 등록 완료 보고 기록 보완
- 브랜치: develop. Jira: TMI-197. 목표: 이번 작업의 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용: 사용자 Secret 등록 완료 보고 및 후속 구현/배포 연결 안내 결과 유지. 실제 저장값·권한 검증이나 외부 변경 없음.
- 테스트: git diff --check. 문서 보완으로 실행 테스트 미수행.
- 유지 계약: Secret 원문 비기록, 기존 기록 보존.
- 결정/위험: 등록 보고와 실제 서버 연동 검증은 별개이며 기능 미구현 상태.
- 다음 작업: 구현 승인 후 TMI-197 개발. Jira/배포/commit/push 없음.

<!-- codex-turn:01a10f04-4e9c-78a3-b53e-bc138986d9f0 -->

## 2026-10-06 TMI-197 문의 접수 API·Slack outbox 구현

- Jira: TMI-197. 브랜치: 사용자 변경 후 지정한 feat/TMI-197-support-inquiry에서 작업 유지. 목표: 승인된 문의 접수/DB 저장/Slack 본문 알림 구현.
- 변경 파일: domain/support 신규 모듈, 대응 support 테스트6개 파일, SecurityConfig, IdentityOpenApiExamples, application.yml, OpenApiSharingTests, support-inquiry-api.md, support-inquiry-implementation-plan.md, CURRENT_STATE.md, 본 WORKLOG.
- 구현: POST 선택 인증/현재 ACTIVE 계정에서 userId 결정, 익명 null, invalid Bearer 비강등. strict JSON/16KiB/10~2000자 검증, HMAC IP·주체별 quota/멱등 키, 문의+receipt+outbox+quota Mongo 트랜잭션, 동일키 재전송200/신규201, 충돌409/과다429/장애503 등. 문의90일·receipt7일 TTL. 공개 조회/첨부/자동 회신 없음.
- 알림: Slack receipt+전체 본문 plain_text만 전달, URL/멘션 해석 억제, webhook allowlist/redirect 금지/timeout, 원문 로그 금지. 5초 scheduler/60초 lease/최대5회 전송/Retry-After/지수형 간격, low-card counters. FAILED만 내부 CAS 수동 replay1회 및 문의 삭제 감사 primitive; 공개 endpoint/운영 runner는 없음.
- 테스트: compileJava 성공. 초기 mock 설정 및 OpenAPI operation 수 회귀 실패를 수정한 뒤 `./gradlew clean test exportOpenApi` 성공: tests1166/failures0/errors0/skipped6, 실행1160 통과. 이어 별도 opt-in SupportReplicaSetTests6/6 통과: 전체 rollback, commit 응답 유실,6동시 중복 접수, quota 거절 원자성, lease 재확보/stale CAS, 승인 재처리·삭제. 로컬 임시 Mongo만 사용, 합성 DB 삭제. 실제 Atlas/OAuth/Slack 테스트 없음.
- 유지 계약: RS256/JWKS/audience/기존 인증·병합 무변경, 외부 body userId 금지, Secret 원문 비기록, 테스트 외부 인프라 미사용. 사용자 선행 문서 변경은 보존, 예상 밖 제품 파일 변경 없음.
- 결정/위험: 기존 Access JWT의 sessionId/epoch 부재로 개별 로그아웃 즉시 판별은 구현하지 못함(사용자 고지). 인증 상태는 기존 JWT+현재 계정으로 검증. Slack at-least-once 중복 가능. 고정 UTC quota/NAT 오탐, HMAC 교체 시 멱등/제한 초기화, TTL 비동기, Slack 별도 삭제 필요. backlog gauge/경보/실행용 운영 CLI 미구성.
- 배포 전/다음 작업: flags OFF 유지. 별도 HMAC Secret·webhook 주입·최소권한, Mongo replica/index, 실제 ALB trusted proxy와 forward headers NONE 충돌 검사, 합성 Slack 전송, 개인정보 고지·채널 권한·보관/삭제 담당 확인. 사용자가 commit/push. 자동 배포/Slack 전송/Secret 조회/Jira 댓글·상태 변경 없음.
- Jira 댓글 초안(미등록): “문의 API·엄격 입력/선택 인증·멱등/제한·Mongo outbox·Slack 비동기 전송 및 제한된 수동 복구 primitive 구현. 변경: support 모듈/테스트, 보안·설정·OpenAPI·계약 문서. 회귀1160개 및 별도 replica 통합6개 통과. 잔여: 실제 Secret/프록시/Slack 검증, 개인정보 운영 및 경보 준비; 기존 JWT의 세션 단위 즉시 폐기 검증 한계.”

<!-- codex-turn:01a10f7b-7694-7b90-b4ef-b55fa4829113 -->

## 2026-10-06 REFUND 문의 분류 구현 결과 및 작업 식별 기록
- 브랜치: develop. 목표: 환불 문의 분류 추가 및 현재 턴 기록 보완.
- 변경 파일: SupportRequest.java, SupportWebTests.java, docs/contracts/support-inquiry-api.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 구현: Category REFUND 추가, HTTP201 및 서비스 전달 테스트, Swagger enum과 API 문서 반영. 환불 실행 기능은 추가하지 않음.
- 테스트: 이번 구현의 `./gradlew clean test exportOpenApi` 성공, 1161 통과/로컬 Mongo opt-in6개 제외. 생성 enum AUTH/GENERAL/REFUND 확인. 기록 보완 후 git diff --check 실행.
- 유지 계약: 기존 인증·userId 결정·멱등·quota·Slack 형식 유지. 기존 사용자 변경 보존, 예상 밖 제품 변경 없음.
- 결정/위험: 새 코드 배포 전 REFUND는 구버전에서 거절될 수 있음. 실제 AWS/Slack 검증 및 변경 없음.
- 다음 작업: 사용자 commit/push/배포 후 프론트 REFUND 옵션 활성화. Jira 댓글 초안은 REFUND 추가·테스트 통과·배포 필요 요약이며 자동 등록하지 않음.

## 2026-10-06 환불 문의 인증된 userId 필수화
- 브랜치: develop. 목표: REFUND 접수에 서버 확인 사용자 ID 필수 적용.
- 변경 파일: SupportController.java, SupportError.java, SupportService.java, SupportServiceTests.java, SupportWebTests.java, docs/contracts/support-inquiry-api.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md. 기존 REFUND enum 추가 변경은 보존.
- 구현: REFUND의 null/빈 userId를401 SUPPORT_REFUND_AUTH_REQUIRED로 저장·멱등 재응답 전에 차단. 컨트롤러/서비스 방어 검사. body userId는 받지 않고 검증된 현재 활성 계정 ID 사용. MEMBER/GUEST 모두 허용, AUTH/GENERAL 익명 접수 유지.
- 테스트: `./gradlew clean test exportOpenApi` 성공, 실행1164 통과/opt-in Mongo6개 제외. HTTP 익명 거절·회원 접수·임의 body ID 거절, 저장 전 차단, MEMBER/GUEST ID 저장 검증. git diff --check 수행.
- 유지 계약: 기존 JWT/토큰 발급·Slack 본문 형식·rate limit·멱등 유지. 외부 계약에 환불 인증401 추가. 자동 환불 및 회원 전용 제한은 없음.
- 결정/위험: 기존 익명 환불 데이터는 소급 변경하지 않음. 새 코드 배포 전에는 새 제한 미적용. 선행 미커밋 변경 보존, 예상 밖 변경 없음.
- 다음 작업: 사용자 commit/push/배포 및 프론트 REFUND의 Identity Bearer 전달. 실제 AWS/Slack/DB 변경 없음. Jira 댓글 초안(미등록): “REFUND 서버 확인 userId 필수화, 익명401, MEMBER/GUEST 허용 및 본문 ID 거절 유지. 실행1164 테스트 통과, 프론트 인증 전달 및 배포 필요.”

<!-- codex-turn:01a10f7e-aad6-7531-a64a-c2f256e484aa -->

## 2026-10-06 환불 문의 인증 필수화 작업 식별 기록 보완
- 브랜치: develop. 목표: 현재 턴 식별자 및 완료 결과 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 이번 보완은 기록만 변경하며 위 구현 결과를 유지한다.
- 구현 결과: REFUND는 서버가 확인한 활성 MEMBER/GUEST userId 필수, 익명401 SUPPORT_REFUND_AUTH_REQUIRED, 본문 userId 거절. AUTH/GENERAL 기존 접수 유지.
- 테스트: 이번 구현의 전체 테스트1164개 통과, opt-in Mongo6개 제외 및 OpenAPI 생성 성공. 기록 보완 후 git diff --check 통과.
- 유지 계약/결정: 기존 JWT·멱등·quota·Slack 형식 유지. Secret 비기록, 과거 기록 보존, 예상 밖 변경 없음.
- 위험/다음 작업: 배포 전 새 제한 미적용. 사용자 commit/push/배포 및 프론트 REFUND Bearer 전달 필요. 실제 인프라 변경·Jira 등록 없음.

## 2026-10-06 테스트 문의 ON 및 재배포 사전 확인
- 브랜치: develop. 목표: 사용자 요청 테스트 Identity 설정 활성화·재배포 전 안전 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 제품 코드 추가 변경 없음, 선행 미커밋 변경 보존.
- 확인 내용: 현재 HEAD는 문의 초기 구현 병합 상태, REFUND 추가와 userId 필수 검사 등은 로컬 미커밋. workflow는 develop push를 테스트 서비스로 배포한다. 코드 미포함 재배포를 피하기 위해 flag 변경 보류.
- 테스트/진단: git status/diff/log 및 workflow 확인. AWS sts 읽기 조회는 NoCredentials로 실패, 원격 상태 미확인. 기록 diff 검사 수행. 코드 변경 없어 테스트 재실행 없음.
- 유지 계약/결정: 사용자 commit/push 규칙 유지. 운영 서버·Secret·IAM·ECS 설정 변경 없음. 예상 밖 변경 없음.
- 위험/다음 작업: 사용자 commit/push 후 CI와 이미지 일치 확인 필요. AWS 인증 확보 후 Secret refs/권한/ALB proxy 및 flag 검증하고 테스트 재배포 진행. 현재 ON/재배포 완료로 주장하지 않음. Jira 변경 없음.

<!-- codex-turn:01a10f81-0dc3-7393-a4f8-f53bf1db6a85 -->

## 2026-10-06 재배포 사전 확인 작업 식별 기록 보완
- 브랜치: develop. 목표: 현재 턴 기록 및 테스트 재배포 대기 상태 명확화.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 이번 보완은 문서만 변경.
- 결과: REFUND·인증 필수 수정이 로컬 미커밋이며 AWS CLI 인증도 없어 기능 ON과 재배포를 수행하지 않음. 사용자 commit/push 요청 상태 유지.
- 테스트: 이전 git 상태/차이/workflow 확인 및 AWS NoCredentials 결과 유지. 문서 보완 후 git diff --check. 제품 변경 없어 실행 테스트 재수행 없음.
- 유지 계약/결정: 기존 기록과 선행 코드 변경 보존, 비밀값 비기록, Codex commit/push 금지 준수. 원격 변경 및 예상 밖 변경 없음.
- 위험/다음 작업: 사용자 push 및 AWS 인증 후 CI 이미지·Secret 참조·권한·프록시 확인을 거쳐 테스트 한정 ON/재배포. Jira 변경 없음.

## 2026-10-06 TMI-197 테스트 문의 기능 및 Slack 알림 활성화 배포
- Jira: TMI-197. 브랜치: develop. 목표: 사용자 push 완료 후 테스트 Identity에 문의 설정 ON 및 재배포.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 제품 코드 변경 없음. 시작 시 로컬 작업 트리 clean, HEAD cc076442 확인.
- 원격 확인: GitHub Actions 실행37414782746에서 동일 커밋 테스트/이미지 업로드/배포/health 성공. 로컬 CLI는 NoCredentials였으나 로그인된 Chrome AWS CloudShell로 작업. 현재 테스트 서비스와 실행 역할·ALB subnet·XFF append/port false 및 target8081의 ALB SG만 허용 확인.
- 승인/원격 변경: 사용자 두 Secret 읽기 권한 추가 명시 승인 후 테스트 execution role에 ReadSupportInquirySecrets 별도 inline 정책 추가(GetSecretValue, 정확히 문의 Secret2개). 기존 역할 정책·운영 서버 변경 없음. Secret 원문 조회/출력 없음.
- 설정/배포: 최신 코드 revision21을 기반으로 문의 ingress/Slack worker true, forward headers none, 신뢰 프록시를 확인한 ALB 두 subnet CIDR로 제한, HMAC 및 Slack JSON 키 참조2개만 추가. 초기 register는 empty tags 거절로 리비전 생성 안 됨, 빈 tags 필드 제외 후 revision22 등록 성공. GitHub 배포 완료 뒤 서비스22 적용. PRIMARY COMPLETED/running1/failed0 확인. 이미지·기타 환경변수·기존 Secret·리소스 설정 유지.
- 테스트: 새 컨테이너 시작 로그55초, 초기 ALB unhealthy는 이후 healthy로 복구. 실제 health200 UP, 익명 REFUND401 SUPPORT_REFUND_AUTH_REQUIRED, 합성 GENERAL 접수201, 동일키/본문 재전송200·동일 접수번호, 동일키 다른본문409 SUPPORT_INQUIRY_REQUEST_CONFLICT. Slack 문의-테스트 채널에서 해당 합성 접수번호/본문 도착 확인. 테스트 문의1건 생성, 원격 DB 직접 조회/삭제 안 함. 문서 diff 검사 수행.
- 유지 계약: REFUND 서버확인 userId 필수, 클라이언트 임의 userId 금지, JWT/기존 인증 무변경. Slack 접수번호+본문만, 테스트 합성 메시지에 개인정보 없음. 예상 밖 제품 변경 없음.
- 위험/미확인: 실제 인증 계정 REFUND 접수는 이번 원격 검증에서 미실행. Slack 네트워크 응답 유실의 중복 가능성은 기존 설계대로 남음. 운영 경보/보관·삭제 정책은 별도 준비 필요. 실제 사용자 문의 원문/Secret 값은 기록하지 않음.
- 다음 작업: 프론트에서 테스트 API 연동 및 REFUND Bearer 전달 검증. 생성한 합성 문의와 Slack 메시지는 보관. CloudShell 임시 변수 정리. Codex commit/push/Jira 변경 없음.
- Jira 댓글 초안(미등록): “테스트 Identity cc076442 및 설정 revision22 배포 완료. 최소 Secret2개 읽기 정책은 사용자 승인 후 적용. 문의·Slack ON, health200/익명환불401/접수201/중복200/충돌409 및 Slack 수신 확인. 남은 확인: 실제 인증 REFUND·프론트 연동, 운영 경보/개인정보 보관 정책.”

<!-- codex-turn:01a10f83-0128-7d02-a611-4b70257bdb79 -->

## 2026-10-06 TMI-197 테스트 활성화 배포 작업 식별 기록 보완
- Jira: TMI-197. 브랜치: develop. 목표: 현재 턴 배포 완료 결과 및 식별자 기록.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 이번 보완은 기록만 변경.
- 결과: cc076442 기반 테스트 revision22 배포 완료, 문의 접수·Slack worker ON, 사용자 승인한 정확한 Secret2개 읽기 권한 및 참조 적용. 운영 서버 미변경.
- 테스트: 해당 턴에서 health200, 익명 REFUND401, 합성 문의201, 동일 요청200, 키 충돌409 및 Slack 수신 확인. 기록 보완 후 git diff --check 수행.
- 유지 계약/결정: 인증된 userId 필수화 및 기존 JWT/알림 형식 유지. 비밀값 비기록, 과거 기록 보존, 예상 밖 변경 없음.
- 위험/다음 작업: 실제 인증된 REFUND 원격 접수·프론트 연동과 운영 경보/보관 정책 확인 필요. 추가 원격 변경·Jira 등록·commit/push 없음.

<!-- codex-turn:01a10f96-3606-7a93-ab8b-2f97db4aa912 -->

## 2026-10-06 사용자 제공 일반 문의 전송 확인
- 브랜치: develop. 목표: 사용자가 지정한 문의 본문을 테스트 API로 한 번 접수하고 Slack 전달 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 제품 코드 변경 없음.
- 수행: GENERAL·익명·연락처 미지정으로 사용자 제공 본문 그대로 전송. 고정 멱등 키 사용. 최초 sandbox DNS 실패 후 승인된 네트워크 실행으로 접수 성공, 중복 신규 요청 없음.
- 테스트: HTTP201 RECEIVED 확인, Slack 문의-테스트 채널에서 동일 접수번호와 전체 본문 도착 확인. DB 직접 조회는 하지 않음. 문서 diff 검사 수행.
- 유지 계약/결정: 테스트 환경만 사용, 사용자 ID/이메일 임의 추가 없음, 문의 원문 및 비밀값은 작업 문서에 복사하지 않음. 과거 기록과 선행 변경 보존, 예상 밖 변경 없음.
- 위험/다음 작업: 회신 연락처 없이 접수된 전송 테스트이며 실제 증상의 원인 분석/해결을 수행한 것은 아님. 테스트 문의·Slack 메시지는 보관. 추가 전송·배포·Jira 변경·commit/push 없음.

<!-- codex-turn:01a10f98-4a50-74a2-93d2-383044a2ff9b -->
<!-- codex-turn:01a10f9d-3d97-7601-831d-1eb47d90ef53 -->

## 2026-10-06 Notion 문의 API 가이드 8.25 추가
- 브랜치: develop. 목표: 사용자 지정 Notion 로그인 문서에 문의 API 연동 계약 업데이트. 진행 중 번호 요청은 최종 8.25로 확정됨.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 외부 변경: 지정 Notion 페이지의 8.25 절 추가. 제품 코드 변경 없음.
- 내용: SupportRequest/Controller/Service/Error와 계약 문서 기준 요청·응답 예시, AUTH/GENERAL/REFUND, 서버 확인 사용자 인증, 필드 제한, 오류 대응, 멱등 키와 재시도, 요청 제한, 주의문구 및 테스트 검증 범위 정리.
- 검증: Notion 새로고침 후 8.25 및 하위 7절 단일 존재·전체 내용·9번 부록/기존 링크 보존 확인. 편집 중 중복 초안 제거 후 저장 확인. 문서 작업으로 실행 테스트 재수행 없음, git diff --check 수행.
- 유지 계약/결정: REFUND는 활성 MEMBER/GUEST 인증 필수, 본문 userId 금지. RECEIVED는 접수일 뿐 Slack/환불 완료 아님. 기존 인증 계약·원격 배포 설정 변경 없음. 비밀값·실제 문의 원문 비기록. 선행 사용자 기록 보존, 예상 밖 제품 변경 없음.
- 위험/다음 작업: 실제 로그인 계정 REFUND 원격 제출과 프론트 통합 QA는 남아 있음을 명시. 운영 활성화로 주장하지 않음. Jira 변경·commit/push 없음.

<!-- codex-turn:01a10fa4-2961-7860-bcba-e6e247a3209d -->

## 2026-10-06 Notion 문의 API 8.25 가독성 정리
- 브랜치: develop. 목표: 사용자 요청에 따라 8.21과 유사한 읽기 쉬운 API 문서 서식 적용.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 외부 변경: 동일 Notion 페이지 8.25 서식. 제품 코드 변경 없음.
- 내용: API 제목·하위 제목 분리, 요청/응답 JSON 구문 강조 코드 블록, 오류 코드/대응 표, 필드 목록과 설명 문단 구분. 패턴의 별표를 코드로 표시해 Markdown 해석 누락 방지. 중복 평문 초안 제거.
- 검증: 브라우저 새로고침 후 하위 7절·기존 9번 부록 보존 확인, 요청/응답 코드 블록 스크린샷 및 오류 표 DOM 확인. git diff --check 수행. 문서 서식 변경이므로 앱 테스트 미실행.
- 유지 계약/결정: 인증·필드 제한·성공/오류·재시도·검증 범위 유지. 기존 문서/선행 로컬 기록 보존, 예상 밖 제품 변경 없음. 비밀값 비기록.
- 위험/다음 작업: 실제 인증 REFUND와 프론트 QA는 기존 남은 작업 그대로. 배포·Jira·commit/push 없음.

## 2026-10-06 — account-recovery prepare 요청 제한 확인

<!-- codex-turn:01a10fc9-f5e9-7623-9554-f039096765a8 -->

- 브랜치: develop. 작업 목표: prepare의429 기준 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md(기록만).
- 확인 내용: 기본 ACCOUNT_RECOVERY_PREPARE_PER_MINUTE=10, 서버가 인식한 IP의 prepare-ip 해시 기준. epochSecond/60 고정 분 구간에서10회 허용 후11번째부터 RECOVERY_RATE_LIMITED. Mongo 카운터로 인스턴스 간 공유. 컨트롤러는429에 보수적 Retry-After:900을 공통 반환하므로 실제 prepare 분 구간과 구분 필요.
- 테스트와 결과: 설정·서비스·저장소·컨트롤러 정적 확인, git diff --check. 제품 변경 없어 테스트 미실행.
- 유지 계약: 기존 API/한도/인증 변경 없음. Secret·IP 원문 비기록.
- 결정사항·위험: 실제 배포 환경변수와 프록시 IP 해석은 미조회. 같은 IP로 인식되는 클라이언트는 한도 공유.
- 다음 작업: 필요 시 대상 환경 한도·프록시/429 헤더 확인. 배포/Jira/외부 변경 없음.

## 2026-10-06 — Notion 계정 찾기 관련 요청 읽기 및 코드 대조

<!-- codex-turn:01a11037-36f0-78b1-9649-90e3be3312a8 -->

- 브랜치: develop. 작업 목표: 사용자 지정 Notion 계정 찾기 관련 요청 페이지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md(기록만). 선행 기록 변경 보존, 예상 밖 제품 변경 없음.
- 확인 내용: 페이지 질문 9개를 브라우저로 읽었다. AccountRecoveryService/MongoRecoveryStore의 PHONE·auth_time·TTL 조건, FirebaseAdminAuthenticationVerifier의 PHONE exchange 거절, FirebaseExchangeService의 enrollment 생성, RecoveryAccountResolver의 상태 분기, EmailHint 마스킹, 가입 중단 cleanup worker 및 기본 비활성 설정을 정적 대조했다.
- 결정사항: 충돌 후 prepare → credential 전화 로그인은 토큰 auth_time 등 검증 조건 충족 시 가능하다. SMS 확인 시각 자체는 서버가 비교하지 않는다. Android 실험은 페이지 작성자 보고로 구분하며 iOS/실제 통합 동작은 보장하지 않는다.
- 테스트와 결과: 정적 검토 및 git diff --check 수행. 제품 변경이 없어 실행 테스트 미수행. 원격 배포 환경변수 미조회.
- 유지 계약: PHONE 인증은 계정 힌트 조회용이고 exchange 로그인으로 사용 불가. 회원 없음과 Firebase 계정 없음은 다르며 NOT_FOUND가 전화번호 연결 해제를 의미하지 않는다. 비밀값/개인정보 비기록.
- 위험 요소: cleanup은 구현 존재와 배포 활성화를 구분해야 한다. 앱에서 Firebase currentUser를 무조건 삭제하면 전화 로그인 후 기존 회원을 삭제할 위험이 있어 정리 대상 식별 필요. 번호 재할당에 따른 마스킹 힌트 노출 위험 잔존.
- 다음 작업: 필요 시 프론트 답변 및 실제 SDK E2E, cleanup 배포 설정/대상 포함 여부 확인. Notion 수정·Jira 변경·배포·commit/push 없음.

## 2026-10-06 — 계정 찾기 질문 9개 간단 답변 작성

<!-- codex-turn:01a1103e-03a4-71b0-8cda-771fac5b3c21 -->

- 브랜치: develop. 작업 목표: 앞서 확인한 Notion 질문 순서대로 프론트 전달용 짧은 답변 작성.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 제품 코드·외부 문서 변경 없음.
- 내용: PHONE/auth_time 조건, exchange 거절, enrollment 잔존, cleanup 구현과 활성화 구분, 전화번호 중복 방어, prepare 한도·TTL·503 재시도, ACTION_REQUIRED 안내, 재할당 개인정보 위험을 요약했다.
- 테스트와 결과: 선행 코드 검토 근거 재사용, git diff --check 수행. 답변·기록 작업으로 실행 테스트 미수행.
- 유지 계약/결정: 현재 계약 설명만 수행. cleanup 보장·SDK E2E 성공·마스킹 안전성을 단정하지 않음. 과거 기록과 선행 변경 보존, 예상 밖 변경 없음.
- 위험/다음 작업: 배포 설정 및 iOS/실제 서버 연동 미확인. 필요 시 확인하되 이번 작업은 배포·Jira·commit/push·Notion 수정 없음.

## 2026-10-07 — recovery 제한 구간과 만료 UX 설명

<!-- codex-turn:01a1140c-3e00-71a3-b8d9-6d1084429182 -->

- 브랜치: develop. 목표: 고정 분 카운터/Retry-After 차이와 recoveryId 만료 후 재인증 조건 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md(기록만).
- 확인 내용: MongoRecoveryStore는 분 경계마다 별도 카운터를 사용한다. Controller는 모든 recovery rate limit에900초를 안내하므로 prepare 실제 해제 시점보다 길다. 최초 lookup 만료와 이미 처리한 lookup의 retryUntil을 구분한다. 새 prepare 이후 PHONE auth_time 필요하며 SMS 재발송 여부는 Firebase credential 유효성/SDK 동작에 달려 있다.
- 결정사항: 5분은 변경 불가한 보안 경계가 아니다. challengeTtl과 recentAuth는 별도이며 각각 최대15분 설정 가능. 접수 TTL10분·최근 인증5분 유지와 실제 제한별 Retry-After 산출을 개선안으로 제시하되 구현/설정 변경은 하지 않는다.
- 검증: 관련 코드 정적 재확인 및 git diff --check. 제품 변경 없는 설명 작업으로 실행 테스트 미수행.
- 유지 계약/위험: 기존 API/설정 유지, 비밀값 비기록. 배포값과 실제 SDK 재인증/credential 재사용 미확인. 선행 변경 보존, 예상 밖 제품 변경 없음.
- 다음 작업: 사용자 요청 시 제한 헤더와 TTL 정책 수정 검토. 외부 변경·배포·Jira·commit/push 없음.

## 2026-10-07 — recoveryId 만료 응답 코드 안내

- 브랜치: develop. 목표: 만료된 접수번호 lookup 오류 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md(기록만).
- 확인/결정: 만료 검사 오류는 HTTP410 RECOVERY_EXPIRED. 앞선 최근 인증 검사 실패는401 RECOVERY_RECENT_AUTH_REQUIRED, 접수 기록 정리 후 부재는400 INVALID_RECOVERY_REQUEST로 구분한다.
- 검증: 오류 정의 및 선행 검증 순서 확인, git diff --check. 제품 변경 없어 실행 테스트 미수행.
- 유지 계약/위험: API 변경 없음, 만료 ID가 항상410을 보장하는 것으로 안내하지 않음. 과거 기록 보존, 예상 밖 변경 없음, 비밀값 비기록.
- 다음 작업: 프론트 오류 분기 참고. 배포·외부 변경 없음.

## 2026-10-07 — 만료 응답 안내 턴 기록 보완

<!-- codex-turn:01a1140e-02b6-7fc0-891b-125b6e213330 -->

- 브랜치: develop. 목표: 현재 턴 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용/결정: 만료 오류410 RECOVERY_EXPIRED 및 선행 인증/접수 기록 부재 예외 안내 결과 유지. 과거 기록 수정 없이 append.
- 검증: git diff --check 통과. 기록만 변경하여 실행 테스트 미수행.
- 유지 계약/위험: 제품/API 변경 및 비밀값 기록 없음. 추가 위험·예상 밖 변경 없음.
- 다음 작업: 필요 시 프론트 오류 분기 적용. 외부 변경 없음.

## 2026-10-07 — RECOVERY_EXPIRED 응답 재확인

<!-- codex-turn:01a1140f-5f75-74d6-8590-979d9c3a0e49 -->

- 브랜치: develop. 목표: 사용자에게 만료 오류 코드 재확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md(기록만).
- 내용/결정: 앞선 인증 검사를 통과하고 접수번호 만료가 확인되면 HTTP410 RECOVERY_EXPIRED임을 재안내.
- 검증: 선행 코드 확인 근거 재사용, git diff --check. 제품 변경 없어 실행 테스트 미수행.
- 유지 계약/위험: 기존 오류 우선순위 유지. 추가 위험·예상 밖 변경 없음, 비밀값 비기록.
- 다음 작업: 프론트 만료 안내 및 재시작 분기 참고. 외부 변경 없음.

## 2026-10-07 — 응답 재확인 턴 기록 보완

<!-- codex-turn:01a1140f-1b03-79e3-bc3b-e87d62fa7c92 -->

- 브랜치: develop. 목표: 현재 턴의 정확한 식별 기록 추가.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용/결정: 인증 검사 통과 후 만료 시410 RECOVERY_EXPIRED 안내 결과 유지. 과거 기록은 변경하지 않음.
- 검증: git diff --check. 기록만 변경하여 실행 테스트 미수행.
- 유지 계약/위험: 제품/API 변경 없음, 비밀값 비기록, 추가 위험·예상 밖 변경 없음.
- 다음 작업: 필요 시 프론트 오류 분기 참고. 외부 변경 없음.

## 2026-10-07 — recovery 정책 조정안 검토

<!-- codex-turn:01a1141e-5d2e-76a3-899a-d7e71bbf0f59 -->

- 브랜치: develop. 목표: 실제 남은 Retry-After, prepare 분당15회, 접수 TTL10분 제안 검토.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md(기록만).
- 분석/결정: 요청의5분은 challenge TTL로 해석하고10분을 권고한다. recentAuth/retryTtl은5분 유지, lookup 한도 유지. 제한된 카운터의 다음 구간까지 남은 초를 반환하되 다른 한도가 남을 수 있어 성공 보장은 아님을 설명한다.
- 검증: application.yml 설정 분리 확인, git diff --check. 제품 변경 없어 실행 테스트 미수행.
- 유지 계약/위험: 코드/배포값 변경 없음. 한도 완화는50%이며 서버 인식 IP 공유/프록시 문제를 해결하지 않는다. 선행 기록 보존, 예상 밖 변경 없음, 비밀값 비기록.
- 다음 작업: 구현 요청 시 기본값·환경변수·오류 헤더·경계 테스트를 함께 수정. 외부 변경 없음.

## 2026-10-07 — 최근 전화 인증 허용시간 변경안 확정

- 브랜치: develop. 목표: 사용자 선택에 따라 recovery 최근 전화 인증 허용시간도10분으로 정리.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md(기록만).
- 결정: recoveryId TTL10분·recentAuth10분, prepare 고정1분15회, 실제 남은 Retry-After로 변경안 정리. 응답 유실 retryTtl5분과 lookup 한도는 유지한다.
- 검증: git diff --check. 제품 변경 없는 정책 정리로 실행 테스트 미수행.
- 유지 계약/위험: Firebase SMS 코드 자체 만료시간을 바꾸는 것은 아님. 새 prepare 이후 인증 조건 유지. 코드/배포 미변경, 비밀값 비기록, 선행 기록 보존 및 예상 밖 변경 없음.
- 다음 작업: 구현 시 설정·검증기·테스트·계약 동시 반영. 외부 변경 없음.

## 2026-10-07 — 전화 인증 정책 결정 턴 기록 보완

<!-- codex-turn:01a1141f-0ff3-7742-aa12-13a253c99bf0 -->

- 브랜치: develop. 목표: 현재 턴 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용/결정: 최근 전화 인증 허용시간10분 변경안 확정 결과 유지. 코드/배포 적용은 미수행.
- 검증: git diff --check. 기록 변경만 있어 실행 테스트 미수행.
- 유지 계약/위험: 과거 기록 보존, 비밀값 비기록, 제품/API 변경 및 추가 위험 없음.
- 다음 작업: 구현 시 관련 설정·검증·테스트 반영. 외부 변경 없음.

## 2026-10-07 — 알림 기능 배치 방향 안내

<!-- codex-turn:01a11422-022f-79e1-857f-204ab33317da -->

- 브랜치: develop. 목표: 알림 기능을 Identity에 추가할지 서비스 경계 관점에서 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 제품 변경 없음.
- 내용/결정: Identity를 지칭한다는 가정을 밝히고 계정 보안 알림, 학습 알림의 발생 판단, 공통 사용자 알림함/푸시 전달 책임을 구분. 문의 Slack은 운영자 알림임을 구분. 별도 알림 모듈/서비스는 제안이며 확정 구현 아님.
- 검증: 저장소 서비스 소유 규칙 및 문의 계약 정적 확인, git diff --check. 설명 작업이므로 실행 테스트 미수행.
- 유지 계약/위험: Identity에 시험/채점 도메인 추가 금지 유지. 알림 종류와 규모 미확인. 비밀값 비기록, 기존 기록 보존, 예상 밖 제품 변경 없음.
- 다음 작업: 사용자에게 앱 푸시/알림함인지 계정 보안 알림인지 확인 후 범위 결정. 외부 문서·Jira·배포·commit/push 변경 없음.

## 2026-10-07 — 계정 찾기 제한·유효시간 정책 구현

- 브랜치: develop. 목표: prepare IP당 고정1분15회, recoveryId/최근 전화 인증10분, 실제 남은 Retry-After 적용.
- 변경 파일: .env.example, src/main/resources/application.yml; accountrecovery의 RecoveryProperties/MongoRecoveryStore/AccountRecoveryController 및 신규 RecoveryRateLimitException; FirebaseAdminAuthenticationVerifier, ProviderChangeRequestFilter; 관련 테스트7파일(신규 ProviderChangeRequestFilterTests 포함); docs/contracts/single-sns-account-recovery-{runbook,implementation-plan}.md; 작업 기록2파일.
- 구현: 거절된 DB 고정 구간의 종료까지 남은 초를 올림해 전달. 계정 찾기 ingress120/분도 다음 분까지 남은 초 반환. Firebase 자체 throttling은 reset 미상으로 Retry-After 생략. 계정 찾기 최근 인증은 전용 서비스 설정으로 검사해 공통 Firebase high-risk5분 제한에 잘리는 문제 해소.
- 테스트: 첫 전체 테스트 성공 후 ingress/설정 테스트 추가. 추가한 설정 테스트의 OFF 컨텍스트 bean 부재1건을 ON fixture로 수정. 최종 ./gradlew clean test 성공(168 suites,1186 tests,실패0/오류0/skip6), git diff --check 통과. 초기 sandbox Gradle cache 접근 실패 후 승인된 실행으로 검증. 실제 Atlas/Firebase 호출 없음.
- 유지 계약: URL/본문/오류 코드 유지. 응답 유실 재시도5분, lookup20/분·UID/번호5/15분 유지. 다른 SNS API ingress60초 및 가입/승격 등 high-risk 최근 인증5분 유지. 새 prepare 이후 auth_time 및 토큰 유효성/revocation 검증 유지. 기존 recoveryId expiresAt 소급 연장 없음.
- 결정/위험: 배포 설정에 기존 override가 있으면 PT10M/PT10M/15로 수정 필요. 현재 원격 배포값/IP 프록시 해석·실제 SDK E2E 미확인. Firebase upstream429는 헤더가 없을 수 있어 제한적 백오프 필요. 여러 제한은 순차 검사하므로 이후 요청이 다른 제한에 걸릴 수 있음.
- 범위 확인: 예상 밖 제품 변경 없음. 선행 및 동시 작업의 WORKLOG/CURRENT_STATE 기록 보존. 비밀값 비기록. commit/push·Jira·Notion·배포 없음.
- 다음 작업: 사용자 commit/push 후 배포 환경 override 확인 및 프론트 헤더/만료 분기 QA. Jira 댓글용 요약은 위 구현·변경 파일·테스트·남은 위험이며 자동 등록하지 않음.

## 2026-10-07 — 계정 찾기 정책 구현 턴 기록 보완

<!-- codex-turn:01a1141f-d011-7c90-8610-48c5f85860e3 -->

- 브랜치: develop. 목표: 완료한 구현 작업의 정확한 턴 식별 기록 추가.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 과거 기록은 보존.
- 구현/결정: 앞선 항목의 prepare15회·접수/최근 인증10분·남은 Retry-After 구현 결과 유지. 이번 보완에서 제품 코드 추가 변경 없음.
- 검증: 최종 전체 테스트1186건 실패0/오류0/skip6 결과 유지, git diff --check 재확인. 문서 보완만 하여 테스트 재실행 없음.
- 유지 계약/위험: 기존 API 계약 및 남은 배포 환경 override/IP/SDK 확인 사항 유지. 비밀값 비기록, 예상 밖 변경 없음.
- 다음 작업: 사용자 commit/push와 배포 설정 확인. 외부 변경·배포 없음.

## 2026-10-07 — 모의고사 미완료 리마인더 설계 안내

<!-- codex-turn:01a11427-599e-7961-b4cd-1a28a8cbff7c -->

- 브랜치: develop. 목표: 매일 특정 시각 미완료 사용자만 받는 학습 알림의 적절한 소유 서비스 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 제품 변경 없음.
- 내용/결정: Learning Core의 당일 학습 상태로 대상 판단, 스케줄 실행 및 발송 직전 재확인, FCM 전달 방향 권고. 제출 완료를 권장 기준으로 제시하되 확정하지 않음. 알림 수신 설정·기기 토큰·날짜별 중복 방지 필요.
- 검증: 기존 서비스 경계 및 최신 기록 확인, git diff --check. 설계 설명만으로 실행 테스트 미수행.
- 유지 계약/위험: Identity에 시험/학습 소유 로직 추가 금지 유지. 오전/오후·시간대·완료 정의 미확정. 푸시 접수 후 지연 수신과 완료 경합 가능. 선행 작업 보존, 비밀값 비기록, 예상 밖 제품 변경 없음.
- 다음 작업: 알림 시각/시간대 및 완료 기준 확정 후 Learning Core 측 구현 범위 수립. 실제 예약·푸시 전송·배포·Jira 변경 없음.

## 2026-10-07 — 테스트 계정 찾기 활성화 및 정책 재배포 완료

<!-- codex-turn:01a11428-f877-7133-a2f3-77ee0218a95a -->

- 브랜치: develop. 목표: 사용자 push 후 prepare15회/접수10분/최근 인증10분 설정 적용 및 테스트 재배포.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 제품 코드 변경 없음. 원격 변경은 테스트 전용 Secret1개·실행 역할 한정 읽기 정책1개·태스크 정의24 및 테스트 서비스 갱신.
- 승인: 기존 태스크22에 계정 찾기 설정/전용 키 연결이 없음을 확인하고 사용자에게 기능 활성화·전용 키 생성·필요한 읽기 권한 추가를 질문했다. 사용자가 함께 활성화를 명시 승인했다. 운영 환경은 변경하지 않음.
- 실행: 커밋 d07cf4d3의 Actions37561447798 성공과 태스크23 안정화를 확인. 테스트 전용32바이트 HMAC을 원문 출력 없이 생성해 Secret에 저장, 테스트 실행 역할에 해당 Secret의 GetSecretValue만 허용한 정책을 추가하고 저장 결과 대조. 기존 역할 정책 유지.
- 설정: ACCOUNT_RECOVERY_ENABLED=true, ACCOUNT_RECOVERY_PREPARE_PER_MINUTE=15, ACCOUNT_RECOVERY_CHALLENGE_TTL=PT10M, ACCOUNT_RECOVERY_RECENT_AUTH=PT10M, ACCOUNT_RECOVERY_KEY_RING은 Secret 참조로 주입. 같은 새 이미지에서 다른 환경변수/Secret 참조/태스크 설정은 순서 정규화 비교로 보존 확인.
- 검증: ECS 태스크24 배포 성공, 실행1/보류0, ALB 정상1/비정상0 확인. 공개 health UP, prepare1회 HTTP200 SUCCESS 및 남은 유효시간600초 확인, 가짜 인증의 lookup1회 HTTP401 INVALID_RECOVERY_PROOF 확인. 실제 SMS·회원 조회와 원격 한도 소진 테스트는 미수행. CI 테스트 성공 및 앞선 로컬1186 tests(실패0/skip6) 결과 사용, 제품 미변경으로 로컬 재실행 없음. git diff --check 수행.
- 처리 메모: 초기 태스크 등록은 빈 tags 배열로 AWS가 거절해 태그가 있을 때만 전달하도록 수정했다. 등록24 후 배열 순서 차이로 단순 동등 검사가 실패했으나 정규화 비교로 의도한4설정/Secret 참조 외 변경 없음 확인 후 배포했다. CloudShell 세션 종료 후 콘솔 서비스 성공·ALB 상태로 최종 검증했다.
- 유지 계약/위험: API 경로/본문/오류 및 다른 기능/운영 서버 유지. 응답 유실 retry5분 및 lookup한도 유지. SERVER_FORWARD_HEADERS_STRATEGY=none 유지로 서버 인식 IP별 한도 공유 가능성 남음; 실제 클라이언트 IP 분리와 Firebase 앱 E2E는 미확인. 요청 제한 해제 시간은 로컬 테스트 검증이며 실제 한도 소진은 하지 않음.
- 범위/다음 작업: 선행·동시 작업 기록 보존, 예상 밖 제품 변경 없음. commit/push·Jira·Notion 변경 없음. 프론트 실전화 인증 연동과 프록시 IP 처리 후속 확인. 생성된 합성 prepare 기록은 기존 TTL 정책으로 정리된다. 배포 성공 화면은 임시 로컬 이미지로 저장했고 비밀값은 문서에 기록하지 않음.

## 2026-10-07 — recoveryId 재발급 시 기존 ID 유효성 분석

<!-- codex-turn:01a114bf-85e4-7381-bad3-cbfd11d14e54 -->

- 브랜치: develop. 목표: 신규 prepare 응답 지연 중 기존 ID로 lookup 가능한지 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 구현 변경 없음.
- 확인 근거: AccountRecoveryService.prepare/lookup 및 MongoRecoveryStore.prepare/lookup. 신규 UUID 접수는 독립 insert로 기존 ID 취소·연장 없음. 최초 만료는 서비스 진입 시각과 expiresAt 비교. 인증 시각은 prepare 이후여야 하고 동일 증명의 여러 ID 소비는 충돌한다.
- 결정/계약: 기존 유효 ID 사용 가능하나 진행 중 전화 인증/lookup의 ID를 자동 교체하지 않도록 안내. 소비 후 동일 ID/증명 재시도는 별도 retryUntil 및 인증 유효 조건 적용. API/오류/배포 설정 유지.
- 검증: 코드 정적 확인 및 git diff --check. 분석·기록만으로 실행 테스트 미수행.
- 위험: 클라이언트 전송 후 서비스 진입 전 지연으로 만료 가능. 새 ID 발급은 기존 전화 인증의 유효기간 연장이 아니며 앱의 자동 교체 구현은 미확인.
- 범위/다음 작업: 기존 기록 변경 보존, 예상 밖 제품 변경 없음. 프론트에서 인증 흐름별 ID 고정 및 만료 처리 확인. 배포·Jira·commit/push 없음, 비밀값 비기록.

## 2026-10-07 — Billing 구매 권한·탈퇴 전달·복구 인계서 검토

<!-- codex-turn:01a114d2-5c8f-7282-b2b1-682e81d6e7e6 -->

- 브랜치: develop. 목표: 사용자 첨부 기술 합의 인계서와 현재 Identity 코드 차이 및 위험 검토.
- 변경 파일: docs/contracts/billing-purchase-withdrawal-handoff-review-2026-10-07.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 제품 변경 없음.
- 확인: purchase scope 계정별 제한·TTL 상한 없음, JWT UUID 대소문자 허용. 발급/탈퇴 경합의 공통 보장 미확정. LC 단일 탈퇴 outbox의2xx 성공·인증오류 dead-letter·기본30일 삭제·기존 backfill 합성 ID/첫 page 한계, SigV4 공통 transport 및 Retry-After1~300초 범위 확인.
- 내용/결정: 원 wire/LC 보존, Billing 독립 delivery 및 capture/발송 flag 분리, 원 event 소실 시 별도 snapshot 계약, commit watermark/coverage·보존 합의 권고. 계정 찾기는 사용자 token 발급 API가 아님을 구분. 판매/purge/구현 승인 아님.
- 검증: 관련 소스/테스트 정의 정적 대조 및 git diff --check. 코드 수정 없는 검토로 테스트 재실행 없음. Billing 저장소·참조 ADR 원문·실제 AWS/토큰 수명·과거 DB 미확인.
- 유지 계약/위험: JWT 기존 audience/read와4필드 wire 유지 제안. 원격 상태나 concurrency 실증 완료 주장 안 함. 비밀값 비기록, 선행 기록 보존, 예상 밖 제품 변경 없음.
- 다음 작업: 양 서버 route/ACK/retry/원천 보존/feed 계약 합의 후 Jira/구현 승인. 외부 전송·문서 수정·배포·commit/push 없음.

## 2026-10-07 — Billing 인계 재검토 회신 대조

<!-- codex-turn:01a114d9-8cca-78c0-bae1-96c6e77947ba -->

- 브랜치: develop. 목표: 사용자가 붙여 넣은 Billing 재검토 회신을 기존 Identity 검토와 비교하고 다음 단계 안내.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 제품 변경 없음.
- 내용/결정: 기존 분석과 일치하며 새 구현 승인 아님. 다음은 원천/보존·발급 경계·snapshot/feed·재시도/재개를 구체화한 공동 기술 계약 초안. tombstone 존재와 전체 coverage 증명 구분, rollback capture 보존, 실제 TTL/발급 지연 상한 필요를 보완 권고.
- 검증: 첨부 회신과 기존 검토서 대조, git diff --check. 새 코드 조사/실행 테스트/외부 검증 미수행.
- 유지 계약/위험: 기존 LC와 결제 책임 경계, 판매/purge 별도 gate 유지. 양 서버 운영 검증 완료 주장 안 함. 선행 기록 보존, 비밀값 비기록, 예상 밖 제품 변경 없음.
- 다음 작업: 사용자 요청 시 공동 기술 계약 보완안 작성 후 양 서버 합의/구현 승인. 외부 전송·Jira·배포·commit/push 없음.

## 2026-10-07 — 공동 기술 계약 초안의 구현 전 누락 검토

<!-- codex-turn:01a114df-2a4a-71b3-a33f-efe9c3559fa5 -->

- 브랜치: develop. 목표: 사용자 첨부 공동 계약의 프로토콜 전이와 현행 Identity 연계 공백 검토.
- 변경 파일: docs/contracts/billing-joint-contract-review-2026-10-07.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 제품 변경 없음.
- 내용/결정: 방향은 타당. snapshot H baseline ACK/고정 H2 바인딩/정상 원천 만료와 consumer GAP 구분/control 이행/일반 발급 commit 불명 처리 보완 권고. 보존시각·재개 필드·canonical digest·snapshot 멱등/expiry·Mongo durability/restore 추가 명세 정리.
- 검증: 첨부 전 문서 및 이전 분석 대조, SessionSecurityService/RefreshSessionIssuer/ReissueRecoveryService 정적 확인, git diff --check. 실행 테스트/외부 호출 미수행.
- 유지 계약/위험: 문서 제안과 현재 구현 사실 분리. 개인정보 보존·TTL상한·barrier 승인 및 구현 승인 아님. 선행 기록 보존, 비밀값 비기록, 예상 밖 제품 변경 없음. 운영 Mongo·Billing·실제 coverage 미확인.
- 다음 작업: 양 서버에서 명시한 프로토콜 조건 보완 후 계약 동결·정책 승인·구현. Jira/외부 문서/메시지/배포/commit/push 없음.

## 2026-10-07 — 공동 계약 개정본 재검토

<!-- codex-turn:01a114e8-cb21-7031-8b31-359129c978e4 -->

- 브랜치: develop. 목표: 사용자 후속 첨부 개정본에서 기존 R1~R5 및 추가 규격 반영 여부 확인.
- 변경 파일: docs/contracts/billing-joint-contract-review-2026-10-07.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 제품 변경 없음.
- 내용/결정: BASELINE/RESYNC, 고정H2, 원천/적용 증거 분리, control 이행, 발급 경로별 commit 불명 처리 반영 인정. manifestDigest 정확 규격 및 consumer 복원 세대/old ACK 차단 추가 보완 권고. 보존 요약/readiness 표현 정리 필요.
- 검증: 첨부 전체 문서 및 이전 검토 대조, git diff --check. 신규 구현/알고리즘 실행 테스트·외부 조회 미수행.
- 유지 계약/위험: 기존 지적이 해결된 부분과 잔여 위험 구분. 새로운 보존·TTL·barrier 승인이나 구현 승인 아님. 선행 문서/기록 보존, 비밀값 비기록, 예상 밖 제품 변경 없음.
- 다음 작업: 남은 프로토콜 명세 보완 및 정책 승인 후 단계별 구현/feasibility 검증. 외부 전송·Jira·배포·commit/push 없음.

## 2026-10-07 — 합의한 Identity–Billing 계약 기준 기록

<!-- codex-turn:01a1150a-ae92-75c1-b2dd-0e85573593b0 -->

- 브랜치: develop. 목표: 사용자 합의 최신 첨부를 이후 작업 기준으로 기록.
- 변경 파일: docs/contracts/billing-joint-contract-review-2026-10-07.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md. 제품 변경 없음.
- 내용/결정: 최종 첨부290줄 전체 확인. contentDigest 통일, recovery generation/local·remote CAS/old ACK 방어, readiness 영향 및 enum 보완 확인. 기존 잔여 지적을 문서 대응 완료로 갱신하고 기술 방향 채택 기록. 문서가 명시한 별도 보존·운영 승인까지 확대하지 않음.
- 검증: 문서 전체 대조, 첨부 체크섬 확인, git diff --check. 제품 변경 없는 기준 기록이므로 실행 테스트 미수행.
- 유지 계약/위험: wire4필드/LC/read·audience 유지, 실제 Mongo/운영 검증 및 판매/purge gate 유지. 선행 기록 보존, 비밀값 비기록, 예상 밖 제품 변경 없음.
- 다음 작업: 사용자 요청 시 단계별 구현 계획/Jira 승인 절차. 이번에 구현·외부 문서·메시지·Jira·배포·commit/push 없음.

## 2026-10-07 — Identity/Billing 구현 작업 분할 검토

<!-- codex-turn:01a1150d-9276-7d42-b460-674d03c07954 -->

- 브랜치: develop. 목표: 사용자 제시 I1~I3/B1~B3 및 공동 검증 순서의 타당성 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 제품 변경 없음.
- 내용/결정: 분할 방향 수용 가능. 공통 fixture·로컬 Mongo snapshot/세대 경합 검증과 User/control 공동 schema 선행 권고. I1/I2 병행은 개발만 의미하며 capture/migration 준비 전 purchase ON 금지. I2 cleanup은 I3 원천 보존과 사전 합의. B1 PR 분할 유지하되 차단 완성 전 실구매 OFF. B3 미확인 거래는 UNKNOWN/삭제 금지 유지.
- 검증: 제시 계획과 합의 기준/기록 대조, git diff --check. PLAN-009~013 원문 및 Billing 코드 미열람. 제품 변경 없이 실행 테스트 미수행.
- 유지 계약/위험: 기존 계약·보존 승인·판매/purge gate 유지. Jira 생성이나 병렬 agent/외부 작업 착수 요청으로 해석하지 않음. 선행 기록 보존, 비밀값 비기록, 예상 밖 제품 변경 없음.
- 다음 작업: 공통 검증 완료 조건과 Identity I1/I2/I3 완료 기준/의존성 확정 후 승인받아 Jira 생성. 외부 전송·구현·배포·commit/push 없음.

## 2026-10-07 — TMI-137 하위 이슈 생성 사전 확인

<!-- codex-turn:01a1150e-e26a-7ee1-8064-7ed8916c4681 -->

- Jira: TMI-137. 브랜치: develop. 목표: 요청한 결제 에픽 하위 공통 준비/Identity 작업 생성 준비.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 제품 변경 없음.
- Jira 작업: 공식 Atlassian MCP get issue 및 parent JQL 조회. TMI-137 제목 결제 기능/에픽 확인, 직접 하위 이슈 없음. 생성/수정/댓글/상태 전환 미수행.
- 생성안: 공통 테스트 fixture·Mongo/generation feasibility; I1 구매 scope/control/발급 경합; I2 탈퇴 journal·Billing 독립 delivery; I3 snapshot/feed/checkpoint/generation·보존. 합의 계약·완료 조건·의존성과 별도 활성화 gate 포함 예정.
- 승인: 사용자 생성 요청은 받았으며 저장소 규칙의 내용 사전 공개 후 최종 승인 대기. 댓글 목적/변경 상태 없음. Billing 작업과 공동 E2E 별도 이슈는 이번4건 제안에 포함하지 않음.
- 검증: Jira 읽기 결과 확인, git diff --check. 코드 변경 없어서 실행 테스트 미수행.
- 유지 계약/위험: 개인정보/Secret 비기록, 선행 기록 보존, 예상 밖 제품 변경 없음. 다음 승인 후4건 생성/부모 재조회 검증. 외부 쓰기·배포·commit/push 없음.

## 2026-10-07 — Billing 측에서 생성한 공통/Identity/Billing 이슈 확인

<!-- codex-turn:01a11512-f611-79a3-a8e0-04b5d446de6b -->

- Jira: TMI-137. 관련 조회: TMI-199, TMI-200, TMI-201, TMI-202, TMI-203, TMI-204, TMI-205, TMI-206. 브랜치: develop.
- 목표: 사용자가 알린 선행 Jira 생성 상태와 중복 여부 확인. 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 제품 변경 없음.
- Jira 수행: 공식 MCP parent JQL 조회. 공통 준비1/Identity3/Billing3/공동 검증1 총8건, 모두 해야 할 일. 본문 범위·완료 기준·의존성·활성화 gate 확인. issuelinks는8건 모두 없음.
- 결정: 기존 Identity3건과 공통 준비를 재사용, 이전 생성 승인 대기는 해소하고 중복 생성 안 함. 요청은 확인이므로 Jira 생성/수정/댓글/상태 전환/링크 추가 모두 미수행. 추가 댓글 목적 및 쓰기 승인 해당 없음.
- 검증: Jira 조회 전체 결과/잘린 구간 정리 조회 확인, git diff --check. 실행 테스트 미수행(조회·기록만).
- 유지 계약/위험: 이슈 내용은 구현/배포 완료 증거가 아님. 의존성은 본문에만 있고 Jira 링크는 미등록. 개인정보/비밀값 비기록, 선행 기록 보존, 예상 밖 제품 변경 없음.
- 다음 작업: TMI-199 공통 검증을 선행으로 삼아 별도 구현 요청 시 해당 Identity 이슈 재조회 후 작업. 외부 쓰기·배포·commit/push 없음.

## 2026-10-07 — 회원탈퇴 로직 정적 분석 및 설명

<!-- codex-turn:01a1151e-3fa0-7441-b220-0bc7c404b299 -->

- 브랜치: develop. 목표: 회원탈퇴 요청 검증·동기 확정·비동기 정리 흐름 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 제품 구현 변경 없음.
- 확인 근거: UserWithdrawalService/TransactionService, FirebaseWithdrawalCredentialVerifier, User.toWithdrawnTombstone, WithdrawRequest/Response, ExternalCleanupWorker, IdentityReleaseTransactionService, UserController.
- 내용: Access의 본인 ID와 Refresh 소유권·유효성 및 유형별 재인증 검사. 트랜잭션 내 tombstone/세션 폐기/정리 상태/철회 및 탈퇴 outbox 저장. 비동기 Firebase 정리 후 내부 SNS·전화 연결 해제. 동의 이력 등 잔존 정보와 외부 Access 유효성 주의 안내.
- 결정/계약: 성공 응답은 탈퇴 확정이며 외부 전체 삭제 완료가 아님. CLEANED와 downstream 완료를 구분. 중복 요청은 인증 경계 통과 시 기존 결과 반환. 구현·외부 API 계약 변경 없음.
- 검증: 관련 코드 정적 확인 및 git diff --check. 설명·기록만으로 실행 테스트 미수행.
- 위험/다음 작업: 실제 배포 worker 활성화·Learning Core 처리 완료 및 앱 재인증 E2E는 이번에 확인하지 않음. 필요 시 별도 환경 점검. 비밀값 비기록, 선행/동시 작업 보존, 예상 밖 제품 변경 없음. Jira·배포·commit/push 없음.

## 2026-10-07 — SNS 탈퇴 재로그인 생략 정책 검토

<!-- codex-turn:01a11521-a832-7ae3-b09b-ca0e7da38a51 -->

- 브랜치: develop. 목표: 사용자·프론트가 정한 세션 갱신 기반 탈퇴와 현재 검증의 차이 설명.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 제품 변경 없음.
- 근거/내용: FirebaseAdminAuthenticationVerifier.validateIdentityAndTime 및 application.yml 확인. WITHDRAWAL은 auth_time의 공통 high-risk 기본5분 제한 적용. 단순 갱신은 최근 사용자 인증을 대체하지 않으므로 기존 구현에서 최근 인증 요구 오류 가능.
- 결정/제안: 재로그인 UX 생략과 서버 검증 생략을 구분. 구현한다면 WITHDRAWAL의 최근 인증 조건만 별도로 변경하고 유효성/폐기/소유권/Refresh 검증 유지. 다른 고위험 기능의 공통 설정은 완화하지 않음.
- 테스트: 정적 코드 확인, git diff --check. 설명·기록만으로 실행 테스트 미수행.
- 위험/다음 작업: 탈취된 유효 세션으로 탈퇴 가능성이 커지는 보안 tradeoff 안내. 후속 구현 시 탈퇴 전용 검사 및 기존 다른 목적 제한 회귀 테스트 필요. 실제 배포 override/앱 SDK는 미확인.
- 범위: 선행 기록 보존, 예상 밖 제품 변경 없음. 비밀값 비기록, 외부 변경·Jira·배포·commit/push 없음.

## 2026-10-07 — SNS 탈퇴 최근 재인증 요구 제거 구현

- 브랜치: develop. 목표: 사용자 승인된 기존 세션 기반 SNS 탈퇴 정책 구현.
- 변경 파일: FirebaseAdminAuthenticationVerifier.java, WithdrawRequest.java, UserController.java, FirebaseAdminAuthenticationVerifierTests.java, docs/contracts/firebase-withdrawal-lifecycle-stage-1-plan.md, 작업 기록2파일.
- 구현: WITHDRAWAL에 한해 auth_time 경과 제한 제외. auth_time 존재/미래·발급 시각 일관성, 만료/서명/issuer/audience/tenant 및 폐기·disabled·Provider 정책 검증 유지. 사용자/세션 소유권과 LOCAL/GUEST 탈퇴 로직은 미변경. OpenAPI와 계약 갱신.
- 테스트: ./gradlew clean test 성공(168 suites/1189 tests/실패0/오류0/skip6). 초기 Gradle cache sandbox 오류 후 승인 실행. 오래된 인증 시각의 갱신 세션 허용, 잘못된 시각·audience·disabled·invalid 증명 거절 및 다른 목적 high-risk 제한 회귀 확인. 기존 소유권/세션 테스트 포함. 외부 Firebase/Atlas 호출 없음. git diff --check 통과.
- 유지/변경 계약: API 필드/응답/오류 종류 유지, SNS 탈퇴 최근 재로그인 요구만 제거. 새 환경변수 없음. 다른 기능 최근 인증 제한 유지.
- 결정/위험: 사용자가 재로그인 생략 UX 승인. 탈취된 유효 세션의 탈퇴 위험 증가, 실제 앱 SDK/배포 연동은 미확인. Firebase 또는 Identity 세션이 무효인 경우 갱신이 실패할 수 있음.
- 범위/다음 작업: 예상 밖 제품 변경 없음. 선행 작업 기록 및 타 작업의 미추적 Billing 분석 문서2개 보존. 사용자 commit/push 후 서버 배포 및 앱 갱신 세션 탈퇴 QA 필요. 배포·Jira·commit/push 없음. Jira 댓글 초안: SNS 탈퇴 최근 인증 제한만 제외, API 설명/계약/테스트 갱신, 전체1189건 실패0/skip6, 앱 E2E 미확인 및 세션 탈취 위험 유지(자동 등록 안 함).

## 2026-10-07 — SNS 탈퇴 정책 구현 턴 기록 보완

<!-- codex-turn:01a11522-f4f3-71d3-ae61-96eadff51b2c -->

- 브랜치: develop. 목표: 완료한 SNS 탈퇴 정책 구현의 정확한 턴 식별 기록 추가.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 과거 기록 보존, 제품 추가 변경 없음.
- 구현/결정: 앞선 항목의 WITHDRAWAL 최근 인증 경과 제한 제외 및 나머지 인증 검증 유지 결과 동일.
- 검증: 전체 테스트1189건 실패0/오류0/skip6 결과 유지. 문서 보완만으로 테스트 재실행 없음, git diff --check 재확인.
- 계약/위험: API 필드 유지, 새 환경변수 없음. 세션 탈취 위험과 앱 E2E 미확인 사항 유지. 비밀값 비기록, 예상 밖 변경 없음.
- 다음 작업: 사용자 commit/push 후 배포 및 앱 연동 QA. 외부 변경·배포 없음.

## 2026-10-07 — 알림 프론트 인계 문서 Notion 작성

- 브랜치: develop. 목표: 사용자 지정 알림 페이지에 프론트 인계 정리.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 외부 변경: 사용자가 지정한 Notion 알림 페이지 본문 작성, 공유 권한 미변경.
- 내용: 비어 있는 페이지 확인 후 기존 모의고사 미완료 리마인더 논의를 바탕으로 요약/역할/권한/기기 생명주기/탭 처리/결정사항/위험/API 계약 필요 목록/QA 작성. 구현 사실과 제안을 구분하고 아직 없는 API를 확정하지 않음.
- 검증: 저장소 기록과 파일명/관련 내용 검색, Notion 작성 후 새로고침하여 본문 저장 확인, 화면 캡처, git diff --check. 문서 작업이므로 실행 테스트 미수행.
- 유지 계약/결정: Identity에 학습 판단 소유 로직 추가 안 함. Learning Core가 학습 판단하는 방향이며 정확한 발송 정책·전송 방식·서버 API는 후속 합의 필요.
- 위험/다음 작업: Learning Core 실제 구현/배포 미확인, 푸시 지연/누락/중복 및 계정 전환 데이터 노출 방지 QA 필요. 발송 시각·시간대·완료 기준·대상·기기 등록 계약 확정 후 연동.
- 범위: 제품 코드 추가 변경 없음, 기존 작업 보존, 예상 밖 제품 변경 없음. 비밀값 비기록, Jira·배포·commit/push 없음.

## 2026-10-07 — TMI-199 공통 fixture 결과 Identity 검토

<!-- codex-turn:01a11523-6394-7922-b221-faec8e21b1f6 -->

- 브랜치: develop. Jira: TMI-199. 목표: Billing 인계 결과와 실제 fixture, Identity 시각 경로 교차 검토.
- 변경 파일: docs/contracts/billing-c0-fixture-review-2026-10-07.md, docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 내용: fixture 4개 SHA-256 기록, Node 고정4행/빈 목록 digest 재계산 일치. 같은 withdrawnAt을 User/outbox에 전달하는 코드 확인. 신규 journal 정밀도·DTO/null/count·내부 오류 및 영속 sequence 숫자 정렬 조건을 후속 검증으로 기록.
- 테스트: JSON parse와 Node digest 명령 성공. git diff --check 확인. 제품 변경 없는 분석이므로 Gradle 및 Mongo 실험 재실행 없음. Billing 보고의285건은 독립 재실행 결과로 주장하지 않음.
- 유지 계약/결정: 기존 LC/public 응답·승인 보존 정책 유지. C0 전체 완료 또는 판매/purge 활성화로 판단하지 않음. Identity serializer/DB 왕복 검증부터 후속 진행 권장.
- 위험: 운영 Mongo/history/failover/성공 commit 응답 유실·인증 배포 경계 미검증. 신규 DTO 후보 미확정.
- 범위/다음 작업: 예상 밖 제품 변경 없음, 기존 기록 보존. 외부 설정·Billing 파일·commit/push 변경 없음. Jira 조회만 수행, 댓글 초안은 검토 문서에만 보관하며 생성/수정/상태 전환 없음. 구현 요청 후 TMI-199 Identity 교차 테스트 및 양측 합의.

## 2026-10-07 — 알림 프론트 인계 작성 턴 기록 보완

<!-- codex-turn:01a11526-aecf-77a3-b298-083cee831020 -->

- 브랜치: develop. 목표: 완료한 Notion 알림 인계 작성의 턴 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 과거 및 동시 작업 기록 보존.
- 내용/결정: 사용자 지정 페이지의 리마인더 인계 초안 작성 및 새로고침 저장 확인 결과 유지. 프론트 역할·권한·기기 연결·화면 이동·미확정 계약·QA 구분, 이번 보완은 제품/페이지 추가 변경 없음.
- 검증: git diff --check. 기록만 변경하여 실행 테스트 미수행.
- 계약/위험: 확정 API로 제시하지 않으며 Learning Core 배포/구현 미확인. 비밀값 비기록, 예상 밖 변경 없음.
- 다음 작업: 발송 시각·전송 방식·대상·API 합의 후 연동. 배포·Jira·commit/push 없음.

## 2026-10-07 — TMI-199 Identity 독립 fixture·정밀도 검증

- 브랜치: develop(기존 사용자 브랜치 유지). Jira: TMI-199. 목표: 사용자가 요청한 공통 계약 독립 교차 검증.
- 변경 파일: src/test/java/web/tosunsaeng/identity/contract/PaymentLifecycleFixtureTests.java, WithdrawalTimestampCompatibilityTests.java; src/test/resources/contracts/payment-lifecycle/v1/JSON4개 및 README; docs/contracts/billing-c0-fixture-review-2026-10-07.md; 작업 기록2파일.
- 구현: Billing과 byte 동일 fixture hash 고정. 독립 typed-record digest·strict decode·generation 우선 ACK·고정 feed·204-only 정책 oracle40건, 실제 Identity User/outbox·Mongo 변환기·기존 mapper와 Boot Jackson 기본값으로 시각0/3/6/9자리4건. 합계44건 추가. 외부 DB/Provider/Repository 호출 없음.
- 테스트: ./gradlew clean test 성공(170 suites/1233 tests/실패0/오류0/기존skip6, 신규44건 모두 통과). 최초 sandbox cache lock 제한 후 승인 실행. golden wire 전체 비교 추가 후 전체 재검증. git diff --check 확인.
- 유지 계약/결정: src/main·build·외부 API·JWT·LC 변경 없음. 저장 후 User/outbox 시각은 동일, 저장 전 고정밀 wire와 혼합하면 digest가 바뀜을 재현. 신규 source에 authoritative 시각 규칙 필요, 과거 event 반올림/재생성 금지. C0 전체 완료 및 기능 ON 아님.
- 위험: 테스트 oracle은 새 production recovery 구현이 아님. converter 왕복은 실제 Mongo transaction/snapshot 검증이 아님. exact DTO/null/오류/auth, Identity index/sequence 자료형·상한, replica failover/history/성공 commit 유실·운영 gate 미완료.
- 다음 작업/배포 전: 양 서버 시간·DTO·오류/auth 합의, 미검증 실험 담당/환경 배정 후 TMI-199 공동 완료 판단 및 I1/I2 진행. 테스트/문서만으로 배포 변경 불필요.
- 범위: 기존 정적 검토 및 동시 알림 문서 기록 보존. 이번 작업 외 제품 변경 없음. Billing 파일·운영·Jira 상태/댓글 미변경, commit/push 없음. Jira 댓글 초안은 검토 문서 §7.6에만 보관.

## 2026-10-07 — TMI-199 검증 턴 식별 기록 보완

<!-- codex-turn:01a11529-5cb9-7080-b744-5a49d7920ab2 -->

- 브랜치: develop. Jira: TMI-199. 목표: 완료한 Identity 교차 검증의 현재 턴 식별 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 과거 기록 append-only 유지.
- 구현 내용: 앞선 신규44건 검증 및 전체1233건 결과에 턴 식별자를 연결. 제품·테스트 코드 추가 변경 없음.
- 테스트: 직전 ./gradlew clean test 성공(170 suites/1233 tests/실패0/오류0/기존skip6, 신규skip0). 이번 문서 보완은 테스트 재실행 없이 git diff --check 확인.
- 유지 계약/결정: JWT·LC·public API 및 배포 설정 유지. C0 전체 완료로 표시하지 않음.
- 위험 요소: 실제 Mongo replica-set·운영 gate 및 exact DTO/오류/auth 합의 미완료 상태 유지. Secret 비기록.
- 다음 작업: 시간 규칙·DTO·인증 경계 합의와 미검증 실험 배정 후 공동 완료 판단. 기존 동시 기록 보존, 예상 밖 제품 변경 없음. Jira·배포·commit/push 없음.

## 2026-10-07 — 열린 회원 탈퇴 Swagger 탭 진단

<!-- codex-turn:01a1152f-1f1e-7ff2-82b3-ab1ea1f5efa4 -->

- 날짜/브랜치: 2026-10-07, develop. 이번 요청 Jira 지정 없음.
- 작업 목표: 사용자가 연 테스트 Swagger 회원 탈퇴 문서의 이상 여부와 원인을 확인한다.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md (기록만).
- 조사 근거: Chrome의 identity-test Swagger /User/withdraw 탭을 읽기 전용 확인. UserController 164~185행의 오류 schema, BaseResponse 11~16행의 성공 example 및 failure 구현, IdentityOpenApiExamples의 탈퇴 200 예시만 등록된 부분, WithdrawRequest/UserWithdrawalService의 유형별 인증 조건과 GlobalExceptionHandler를 대조했다.
- 확인 내용: 400/401/404/409 오류 Example Value가 true/SUCCESS/성공 메시지/result 객체로 표시되는 문서 결함을 확인했다. 실제 실패 코드는 BaseResponse.failure에서 false와 error code/message 및 상세 또는 null을 반환한다. 화면은 실제 실행 응답이 아니라 생성 예시이며 실제 서버 탈퇴 요청은 보내지 않았다.
- 추가 확인: 기본 request example은 모든 인증 필드를 함께 보여주지만 SNS는 비밀번호 생략, LOCAL은 Firebase 증명 생략, Guest는 둘 다 생략해야 한다. 유형 혼합은 WITHDRAWAL_CREDENTIAL_TYPE_MISMATCH로 거절될 수 있어 유형별 예시 필요. 탭의 SNS 최근 재로그인 불필요 설명은 최신 정책과 일치한다. 누락 증명 오류 enum의 최근 인증 문구는 아직 남아 있다. generated server URL이 HTTP로 표시되는 별도 문제도 관찰했으나 원격 프록시 설정 원인은 확정하지 않았다.
- 구현 내용/결정사항: 확인 요청이므로 제품 코드는 수정하지 않았다. 후속 수정은 유형별 요청 예시와 실제 enum 기반 오류 예시 등록, 누락된 오류/설명 검토, Swagger 문서 회귀 테스트를 권고한다. 공통 BaseResponse의 성공 예시를 무조건 실패로 바꾸는 방식은 피한다.
- 실행한 테스트와 결과: ./gradlew test --tests web.tosunsaeng.identity.domain.user.api.UserControllerTests --tests web.tosunsaeng.identity.domain.user.application.FirebaseUserWithdrawalServiceTests 성공. 최초 sandbox 캐시 접근 실패 후 승인 실행. 제품 변경 없는 진단으로 전체 clean test는 실행하지 않았다. 실제 탈퇴/외부 Firebase 호출 없음. git diff --check 수행.
- 유지한 계약: API/회원/세션/최근 재인증 정책/배포 및 Jira 변경 없음. 실제 자격증명·개인정보 미수집/미기록. 기존 Billing C0 분석 및 테스트 fixture 변경 보존.
- 위험 요소: 실서비스 오류 응답을 탈퇴 실행으로 확인하지 않았으며 Swagger Try it out은 사용하지 않았다. HTTP 서버 URL의 실제 호출/redirect 문제는 미검증. 예상 밖 제품 변경 없음.
- 다음 작업: 사용자 수정 요청 시 Swagger 요청/오류 예시와 계약 테스트 보완 후 배포 반영 확인. 이번 commit/push/배포 없음.

## 2026-10-07 — Notion 알림 프론트 인계 가독성 개선

<!-- codex-turn:01a1152b-c523-7d60-8eed-7bf0eec36c4d -->

- 브랜치: develop. 목표: 사용자 지정 알림 인계 문서의 읽기 어려운 단일 본문 개선.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 외부 변경: 기존 Notion 알림 페이지 본문 서식/요약 개선.
- 내용: 상단 목적/프론트/서버/탭 처리/미확정5개 요약으로 축약. 실제 제목1·제목2·제목3 및 개별 목록 블록과 문단 여백 적용. 원래 상세 역할·기기 생명주기·정책 결정·위험·계약·QA·근거 보존.
- 검증: 새로고침 뒤72개 편집 블록과 제목 계층·마지막 상세 항목 유지 확인, 화면 검수 및 임시 캡처 저장. git diff --check. 문서 작업으로 실행 테스트 미수행.
- 유지 계약/결정: 신규 API·정책 확정 없이 기존 설계 초안 의미 유지. Secret 비기록, 공유 권한 변경 없음.
- 위험/다음 작업: 발송 시각·전송 방식·백엔드 계약 및 Learning Core 실제 구현 미확인 사항 유지. 후속 합의 후 연동. 기존 동시 기록·제품 변경 보존, 예상 밖 제품 변경 없음. Jira·배포·commit/push 없음.

## 2026-10-07 — 알림 인계 API 명세 형식 요청 검토

- 브랜치: develop. 목표: 실제 API·응답·오류 중심 문서 재작성 가능 여부 확인.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. Notion·제품 변경 없음.
- 내용/결정: 현재 src/main 및 docs/contracts 파일 검색에서 알림 API 구현 명세를 확인하지 못함. 설계 제안을 실제 API로 오인시키지 않기 위해 Learning Core 구현/Swagger 출처 확인 요청.
- 검증: 파일 검색 및 git diff --check. 분석·기록만으로 실행 테스트 미수행.
- 계약/위험: 임의 경로/응답/오류 코드 생성 없음. Learning Core 실제 구현은 미확인. 비밀값 비기록, 기존 기록 보존, 예상 밖 제품 변경 없음.
- 다음 작업: 실제 명세 출처 확인 후 해당 노션을 API·요청·응답·오류 및 핵심 주의사항 중심으로 재작성. Jira·배포·commit/push 없음.

## 2026-10-07 — 알림 API 명세 검토 턴 기록 보완

<!-- codex-turn:01a11532-430c-7113-a9be-ea4d8dfca99c -->

- 브랜치: develop. 목표: 알림 API 명세 형식 요청 검토의 턴 기록 보완.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 과거 기록 보존.
- 내용/결정: 실제 API 출처 확인 필요 상태 유지. 임의 경로·응답·오류 코드 생성 및 제품·Notion 추가 변경 없음.
- 검증: git diff --check. 기록만 변경하여 실행 테스트 미수행.
- 계약/위험: Learning Core 구현 미확인 상태 유지. 비밀값 비기록, 예상 밖 변경 없음.
- 다음 작업: 구현 저장소 또는 Swagger 확인 후 노션 재작성. Jira·배포·commit/push 없음.

## 2026-10-07 — 회원 탈퇴 Swagger 요청·실패 예시 수정

<!-- codex-turn:01a11532-6b1f-73d3-a7e4-bf52e8d9ead3 -->

- 날짜/브랜치: 2026-10-07, develop. 이번 요청 Jira 지정 없음.
- 작업 목표: 오류 응답을 성공처럼 보여주는 탈퇴 Swagger와 계정 유형별 인증 필드를 섞어 보여주는 요청 예시를 수정한다.
- 변경 파일: IdentityOpenApiExamples.java, 신규 global/response/ApiErrorResponse.java, UserController.java, WithdrawRequest.java, AuthErrorStatus.java; OpenApiSharingTests.java, UserControllerTests.java, FirebaseUserWithdrawalServiceTests.java; docs/contracts/frontend-firebase-auth-integration-guide.md, docs/swagger/README.md, WORKLOG/CURRENT_STATE. 생성물은 기존 build 디렉터리의 Swagger ZIP/JSON.
- 구현 내용: SNS는 refreshToken+firebaseIdToken, LOCAL은 refreshToken+password, Guest는 refreshToken만 제공하는 named request examples 추가. 실제 업무 enum을 직렬화한 오류 예시 16개와 입력 검증 상세 배열 예시 1개 등록. 기존 400/401/404/409와 실제 Firebase/세션 처리가 반환할 수 있는 403/429/503 문서 보완. 민감한 rejectedValue는 null 예시이며 업무 오류 result=null을 유지한다.
- 구현 내용: 탈퇴 전용 적용의 OpenAPI 실패 응답 모델 ApiErrorResponse로 Schema의 성공 예시 재사용을 차단했다. 런타임은 기존 BaseResponse.failure 그대로다. 성공 공통 모델 및 200 응답은 변경하지 않았다. SNS 인증 정보 누락 enum의 message를 '최근 Firebase 인증'에서 '유효한 Firebase 인증 정보'로 보정했고 검증 정책/상태/code는 유지했다.
- 문서: 프론트 탈퇴 예시의 오래된 재인증 자리표시자와 비밀번호 null 필드를 정리하고 현재 정책·오류 의미를 안내했다. 공유 ZIP에 들어가는 README의 오래된 Provider 연결 사용 권고/고정 API 개수도 현재 폐지 계약과 일치하도록 보정했다. Provider 제품 동작을 새로 변경한 것은 아니다.
- 테스트와 결과: 최초 집중 14개에서 Swagger requestMedia.setExample(null)이 example:null을 생성함을 회귀 검사가 탐지해 불필요한 setter 호출을 제거했다. 최종 ./gradlew clean test shareSwagger 성공: 170 suites/1233 tests, failures0/errors0/기존skip6; export 별도1개 성공. 전체 실행의 sandbox cache 접근 실패 후 승인 재실행. 외부 Firebase/실계정 호출 없음.
- 테스트와 결과: 요청 유형별 정확한 필드, refresh 필수/WRITE_ONLY/Bearer 유지, 문서 오류의 상태·code·message와 enum 일치, false/null/검증 배열, 성공 schema 유지 검사 보완. JSON의 3개 요청 예시·7개 오류 상태와 ZIP 8개 파일 무결성 및 git diff --check 확인.
- 유지한 계약: 탈퇴 API URL/요청 필드/HTTP 오류 코드/성공 본문/인증 정책 유지. 외부 message 문구 1건만 명확화. 실제 탈퇴/서버 설정/배포/Jira 변경 없음. 실제 자격증명·개인정보 미기록.
- 결정사항: 문서·예시를 고치고 공통 성공 schema를 실패로 뒤집지 않는다. Swagger generated server URL의 HTTP 표시는 별도 설정 문제로 이번 변경에서 제외한다.
- 위험 요소: 테스트 서버 미배포로 열린 원격 Swagger는 기존 상태다. 원격 UI 렌더링 및 실제 회원 탈퇴 E2E는 실행하지 않았다. 버전 배포 후 문서 갱신 확인 필요.
- 예상 밖 diff: 없음. 기존 Billing C0 분석/교차 테스트 및 fixture, 동시 WORKLOG/CURRENT_STATE 변경을 보존했다. 이번 변경 목록과 기존 미커밋 파일을 구분했다.
- 다음 작업/배포 전 확인: 사용자가 diff와 안내 message 변경을 검토해 commit/push 후 배포하면 원격 /v3/api-docs와 Swagger의 요청 선택 목록·오류 예시를 재확인한다. 갱신 ZIP은 build/distributions/identity-swagger.zip. 이번 commit/push 없음.
- Jira 댓글 초안(미등록): 탈퇴 유형별 요청/실패 schema·예시와 회귀 검사 보완, 전체1233개/기존skip6 및 별도 export 성공, 인증 로직 유지, 원격 배포/화면/E2E 미확인.
