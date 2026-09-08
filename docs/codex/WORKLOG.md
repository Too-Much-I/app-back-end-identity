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

## 2026-08-11 — SocialIdentity 모델 단계 구현 계획 수립

<!-- codex-turn:019fef3a-47d0-7af0-9e7b-28ae72bf1a93 -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: Google/Apple 로그인 API 구현 전에 외부 SNS 계정과 canonical User를 분리하는 `SocialIdentity` 모델·MongoDB index·Repository 단계의 저장소 기반 구현 계획을 작성한다.
- 변경 파일: 애플리케이션 코드는 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 현재 `UserProvider(LOCAL, GUEST)`가 User 생성 불변식, 프로필 응답, 탈퇴 자격증명과 로그 분기에 사용됨을 확인했다. 이번 단계에서는 이를 `GOOGLE`·`APPLE`로 확장하거나 전면 분리하지 않고, Auth 도메인의 별도 `SocialProvider(GOOGLE, APPLE)`와 `SocialIdentity` Document를 추가하는 최소 변경안을 확정했다.
- 구현 내용: `SocialIdentity`는 별도 collection에서 UUID 문자열 id, canonical UUID 문자열 `userId`, provider, case-sensitive opaque `providerSubject`, optional email snapshot, 생성·수정 시각을 보유한다. User와는 `@DBRef` 없이 id로만 연결하고 email은 unique 또는 로그인 조회 기준으로 사용하지 않는다.
- 구현 내용: `(provider, providerSubject)` unique compound index를 동일 외부 계정의 다중 User 연결을 막는 최종 경계로 두고, `userId` non-unique index와 `findByProviderAndProviderSubject`, `findAllByUserId` Repository 계약을 추가한다. User 존재 여부와 ACTIVE 여부 확인, Guest 실제 승격·병합은 후속 application service 책임으로 남긴다.
- 구현 내용: 단위 테스트에는 생성·null/blank·UUID 불변식, Google/Apple 동일 subject의 provider별 구분, 한 User의 복수 SNS 연결과 Repository 계약·index metadata 검증을 포함한다. 현재 test profile이 Mongo 자동설정을 제외하므로 실제 duplicate insert 거절까지 증명하려면 외부 Atlas 없이 동작하는 격리 Mongo 테스트 방식을 구현 시 선택해야 한다.
- 실행한 테스트와 결과: 코드 변경이 없는 계획·문서 작업이므로 `./gradlew clean test`는 실행하지 않았다. 저장소 코드와 설정을 정적으로 조사했고 `git diff --check`는 성공했다.
- 유지한 계약: 실제 `userId`는 canonical UUID 문자열로 유지하고 외부 provider subject 및 email과 분리한다. 클라이언트 `userId` 신뢰 금지, JWT `sub`·Access/Refresh Token 흐름 불변, Python AI의 `user_id=examId`, 외부 Provider 호출 금지, Secret·Token·Password·실제 Key·전체 MongoDB URI 비기록 계약을 유지했다.
- 결정사항: 이 단계에서는 Google/Apple Token 검증, 로그인 API, Guest 승격·병합, Learning Core 데이터 이전, Access/Refresh Token 변경, User provider migration을 수행하지 않는다. Mongo unique index가 경쟁 조건을 포함한 최종 중복 방지 장치이며 애플리케이션 선조회만으로 대체하지 않는다. 요구되지 않은 `(userId, provider)` unique 제약은 같은 provider의 복수 계정 연결 정책이 정해질 때까지 추가하지 않는다. Jira 조회·댓글·상태·필드 변경과 Git commit·push는 수행하지 않았다.
- 위험 요소: `SocialIdentity.userId`는 DB foreign key가 아니므로 존재하지 않거나 탈퇴한 User를 참조하지 않게 하는 application service 검증이 후속 연결 단계에 필요하다. 자동 index 생성은 운영 무중단 index 배포를 보장하지 않으며 기존 중복 데이터가 생기기 전에 index를 먼저 생성해야 한다. provider subject의 로그 노출·email 식별 사용을 금지해야 한다. 실제 연결 저장 전에는 회원 탈퇴 시 identity 삭제 또는 tombstone과 동일 SNS 재가입 정책도 확정해야 한다.
- 다음 작업: 합의한 범위대로 `SocialIdentity` entity·enum·Repository·index와 테스트를 구현하고 `./gradlew clean test`를 실행한다. 실제 SNS API와 Guest 승격·merge는 별도 후속 단계에서 설계한다.

## 2026-08-11 — SocialIdentity 계획에 Kakao provider 추가

<!-- codex-turn:019fef41-d637-7bf1-9f5f-22b7db9fe6a2 -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 기존 Google/Apple 중심 `SocialIdentity` 모델 계획에 Kakao 로그인을 동일한 외부 identity 연결 구조로 추가한다.
- 변경 파일: 애플리케이션 코드는 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 예정된 `SocialProvider`를 `GOOGLE`, `APPLE`, `KAKAO` 세 값으로 확장하고 한 canonical User가 세 provider identity를 함께 가질 수 있도록 계획과 테스트 범위를 갱신했다. 기존 `UserProvider(LOCAL, GUEST)`에는 SNS 값을 추가하지 않는다.
- 구현 내용: Kakao도 email이 아닌 검증된 provider 사용자 식별자를 문자열 `providerSubject`로 저장하고 `(provider, providerSubject)` unique compound index를 동일하게 적용한다. Google·Apple·Kakao 사이에 같은 문자열 subject가 존재하는 것은 provider namespace가 다르므로 허용한다.
- 구현 내용: 실제 Kakao 인증 단계에서는 OIDC `sub` 또는 사용자 정보 API의 사용자 `id` 중 하나를 canonical subject 입력으로 명시적으로 선택하고 같은 `KAKAO` namespace 안에서 두 표현을 임의로 혼용하지 않도록 후속 계약에 포함한다.
- 실행한 테스트와 결과: 코드 변경이 없는 계획 문서 갱신이므로 `./gradlew clean test`는 실행하지 않았다. `git diff --check`는 성공했다.
- 유지한 계약: 실제 `userId`와 Kakao 외부 식별자를 분리하고 email은 로그인 식별 기준으로 사용하지 않는다. Kakao Token 검증·외부 API 호출, Guest 승격·merge, JWT·Refresh Token 변경은 여전히 이번 단계 범위 밖이며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: Kakao는 Google/Apple과 동일한 `SocialIdentity` collection·Repository·index를 재사용하고 provider별 별도 User 필드나 별도 Kakao identity collection을 만들지 않는다. Jira 조회·댓글·상태·필드 변경과 Git commit·push는 수행하지 않았다.
- 위험 요소: Kakao의 canonical subject 원천을 인증 구현 전에 고정하지 않으면 같은 계정이 중복 identity로 저장될 수 있다. provider subject와 optional email은 로그에 기록하지 않고, 실제 저장 시작 전 회원 탈퇴·재가입 시 연결 삭제 또는 보존 정책을 확정해야 한다.
- 다음 작업: `SocialProvider.KAKAO`를 포함한 entity·Repository·index와 Google/Apple/Kakao 조합 테스트를 구현한 뒤 `./gradlew clean test`를 실행한다. Kakao Token 검증과 로그인 API는 별도 후속 단계로 유지한다.

## 2026-08-11 — Google·Kakao·Apple 소셜 로그인 전체 구현 계획서 작성

<!-- codex-turn:019fef41-d637-7bf1-9f5f-22b7db9fe6a2 -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: SocialIdentity 첫 단계만이 아니라 Google·Kakao·Apple 인증, User 계정 유형 분리, Guest 승격·기존 계정 발견·병합, Learning Core 이전과 운영 rollout까지 전체 구현 순서를 하나의 계획서로 정리한다.
- 변경 파일: 새 `docs/social-login-implementation-plan.md`를 작성하고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 현재 `UserProvider(LOCAL, GUEST)`와 User 불변식으로는 비밀번호 없는 소셜 MEMBER와 Guest 승격을 표현할 수 없음을 확인하고, SocialIdentity 첫 배포 뒤 `UserAccountType(GUEST, MEMBER)`와 로그인 수단을 분리하는 호환 migration 단계를 계획했다.
- 구현 내용: SocialProvider·SocialIdentity·index·Repository, Provider 검증 interface, Google·Kakao·Apple별 subject 규칙, 공개 social login과 JWT 보호 social link API, duplicate key 경쟁 처리, 기존 identity owner 중심 canonical merge와 source Session 폐기 흐름을 단계별로 정의했다.
- 구현 내용: Identity가 시험·결과를 직접 수정하지 않고 `UserMerged` transactional outbox로 Learning Core에 이전을 요청하도록 경계를 유지했으며, provider subject·email·자격증명을 event와 로그에 넣지 않는 계약을 포함했다.
- 구현 내용: 모델·Provider verifier·application service·Transaction·동시성·Security·OpenAPI 테스트와 index-first 배포, accountType backfill, provider별 feature flag 활성화, rollback 및 전체 완료 조건을 정의했다. 첫 구현 PR은 SocialIdentity 모델·Repository·index와 테스트만 포함한다.
- 실행한 테스트와 결과: 코드 변경이 없는 계획 문서 작업이므로 `./gradlew clean test`는 실행하지 않았다. `git diff --check`는 성공했고 계획서의 Markdown code fence 개수와 필수 section을 정적으로 확인했다.
- 유지한 계약: 실제 userId는 UUID 문자열이며 JWT `sub`는 canonical userId다. 외부 Request Body의 userId를 신뢰하지 않고 email을 로그인 식별자로 사용하지 않으며, Access/Refresh Token·RS256·JWKS·Python AI `user_id=examId`와 Identity/Learning Core 도메인 경계를 유지했다. Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 미연결 social login은 User를 자동 생성하지 않고 인증된 Guest link 흐름으로 보낸다. 기존 SocialIdentity owner가 canonical User이며 source User에는 새 Token을 발급하지 않는다. Provider별 기능은 Google, Kakao, Apple 순서로 독립 활성화하고 Git commit·push와 Jira 조회·댓글·상태·필드 변경은 수행하지 않았다.
- 위험 요소: Kakao canonical subject, 직접 소셜 가입, 같은 provider 복수 연결, 계정 병합의 eventual consistency, 소셜 전용 회원 탈퇴 재인증과 탈퇴 후 재가입, outbox 전달 방식은 구현 전 제품·보안 결정이 필요하다. 현재 test profile은 Mongo를 제외하므로 실제 unique index·Transaction 검증에는 격리 Mongo와 staging replica set 검증이 필요하다.
- 다음 작업: 계획서 단계 0의 제품·보안 결정을 확정한 뒤 첫 구현 범위인 `SocialProvider(GOOGLE, KAKAO, APPLE)`, `SocialIdentity`, Mongo index, Repository와 외부 인프라 없는 테스트를 구현하고 `./gradlew clean test`를 실행한다.

## 2026-08-11 — 소셜 로그인 전체 계획서 작업 기록 동기화

<!-- codex-turn:019fef43-c151-7f42-ba5e-644ffbcc98f2 -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 현재 turn에서 작성한 Google·Kakao·Apple 소셜 로그인 전체 구현 계획서의 작업 기록을 지정된 turn marker로 동기화한다.
- 변경 파일: `docs/social-login-implementation-plan.md`의 작성 결과를 기준으로 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: SocialIdentity 모델부터 User 계정 유형 분리, Provider 검증, Guest 승격·canonical 병합, Learning Core outbox, lifecycle, 테스트와 provider별 rollout까지 0~8단계로 정리된 계획서가 현재 작업 기준임을 기록했다.
- 실행한 테스트와 결과: 코드 변경이 없는 계획·작업 기록 동기화이므로 `./gradlew clean test`는 실행하지 않았다. `git diff --check`는 성공했고 지정된 turn marker가 한 번 포함됐음을 확인했다.
- 유지한 계약: UUID canonical userId와 JWT `sub`, email 비식별 원칙, RS256·JWKS·RefreshSession, Identity/Learning Core 경계와 Python AI `user_id=examId` 계약을 유지했다. Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 첫 구현 범위는 `SocialProvider(GOOGLE, KAKAO, APPLE)`, `SocialIdentity`, Mongo index, Repository와 격리 테스트이며 실제 Provider API·Guest 승격·병합은 후속 단계로 유지한다. Jira 변경과 Git commit·push는 수행하지 않았다.
- 위험 요소: Kakao subject 원천, 직접 소셜 가입, 같은 provider 복수 연결, 탈퇴·재가입, merge eventual consistency와 outbox 전달 방식은 구현 전에 확정해야 한다.
- 다음 작업: 계획서 단계 0의 결정을 확정한 뒤 SocialIdentity 첫 구현 단계로 진행하고 전체 `./gradlew clean test`를 실행한다.

## 2026-08-11 — 전화번호 검증·무료체험을 포함한 전체 계획 보완

<!-- codex-turn:019fef52-bf25-7f42-adde-a5d79159357c -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 기존 Google·Kakao·Apple 소셜 로그인 계획에 PhoneIdentity·SMS OTP·검증 번호당 무료 모의고사 1회 정책을 추가하고, SocialIdentity 최소화·Provider 입력·Apple revoke·merge·outbox·Access Token·관측성 보완 의견을 전체 구현 계획에 반영한다.
- 변경 파일: `docs/social-login-implementation-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 첫 SocialIdentity를 `socialIdentityId`, `userId`, `provider`, `providerSubject`, `createdAt`으로 제한하고 email과 의미 없는 `updatedAt`을 제거했다. Provider email은 공통 검증 결과에서 필요한 순간에만 사용하고 저장하지 않도록 개인정보 최소화 원칙을 확정했다.
- 구현 내용: `SocialTokenVerifier` 입력을 provider별 `SocialVerificationRequest` subtype으로 유연화하고, Kakao OIDC `sub` 권장, Apple authorization code 교환·credential 보관·탈퇴 revoke lifecycle을 단계 0과 Apple 구현 단계에 추가했다.
- 구현 내용: 자동 merge를 ACTIVE GUEST source와 ACTIVE MEMBER target에만 허용하고 MEMBER 간 자동 merge를 금지했으며, canonical chain·순환 방지와 source Access Token 정책을 명시했다. `UserMerged` outbox에는 전달 상태·attempt·nextAttemptAt·lease를 추가하고 atomic claim·at-least-once·Learning Core eventId 멱등 계약을 정의했다.
- 구현 내용: Identity가 E.164 정규화, versioned domain-separated HMAC-SHA-256, `PhoneIdentity`, `PhoneVerificationAttempt`, 보호된 OTP API와 phone·user·IP abuse 방어를 소유하도록 정리했다. 전화번호는 로그인·자동 merge 키가 아니며 raw 번호·OTP·fingerprint를 장기 저장하거나 로그에 남기지 않는다.
- 구현 내용: `TrialClaim`, `UserEntitlement`, 무료시험 grant·consume과 결제는 별도 Entitlement/Billing·Learning Core 경계가 소유하도록 분리했다. Identity에는 `freeTrialUsed`나 시험 코드를 추가하지 않고 benefit-scoped fingerprint 또는 일회성 proof만 서버 간에 전달하며, claim unique·consume idempotency·exam 생성 실패 보상을 계획했다.
- 구현 내용: 관측 sink를 application logs·Sentry·metrics로 분리해 internal userId는 필요한 application log에서만 허용하고 Sentry와 metrics에는 금지했다. 전체 순서를 SocialIdentity → accountType → PhoneIdentity/OTP → TrialClaim/Entitlement → 무료시험 consume → Google → Guest 승격·merge → Kakao → Apple → 결제·lifecycle의 0~12단계로 재정렬했다.
- 실행한 테스트와 결과: 코드 변경이 없는 계획 문서 작업이므로 `./gradlew clean test`는 실행하지 않았다. `git diff --check`는 성공했고 trailing whitespace가 없으며 Markdown heading 20개 구간·code fence 70개 짝수와 지정 turn marker 1회를 확인했다. 주요 금지·소유권 계약도 정적 검색으로 확인했다.
- 유지한 계약: 실제 userId는 UUID 문자열이고 JWT `sub`는 canonical userId다. 외부 Request Body의 userId를 신뢰하지 않고 email·전화번호를 로그인 식별자로 사용하지 않으며, RS256·JWKS·RefreshSession과 Python AI `user_id=examId` 계약을 유지했다. Identity는 시험·결과·무료 사용권·결제를 소유하지 않고 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 첫 구현 PR 범위는 email 없는 최소 `SocialIdentity`, `SocialProvider`, 두 Mongo index, Repository와 격리 index 테스트로 유지한다. 전화번호는 검증 번호당 무료체험 중복 방지 입력일 뿐 계정 merge 기준이 아니며 `TrialClaim`은 Identity 저장소에 만들지 않는다. Git commit·push와 Jira 조회·댓글·상태·필드 변경은 수행하지 않았다.
- 위험 요소: HMAC key rotation의 mixed writer, SMS 비용 abuse, 번호 재할당, pseudonymous TrialClaim 보존, Apple revoke 실패, source Access Token, outbox 중복 전달, entitlement consume과 exam 생성의 분산 일관성은 구현 전 정책·법무·운영 계약이 없으면 각각 중복 지급·권리 유실·개인정보 보존·권한 오판 위험을 만든다.
- 다음 작업: 계획서 단계 0의 Kakao·Apple·merge·전화번호·HMAC·OTP·TrialClaim 보존·Identity–Entitlement·consume 보상 결정을 확정한 뒤 첫 SocialIdentity PR을 구현하고 `./gradlew clean test`를 실행한다.

## 2026-08-11 — 전화번호 인증 시점을 MEMBER enrollment로 조정

<!-- codex-turn:019fef5e-65f0-7772-a867-5b96eb9d4ff4 -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 무료 모의고사 시작 직전이던 전화번호 인증 UX를 검토하고, 회원가입·소셜 MEMBER 승격 시 1회 인증하는 전체 정책으로 계획서를 다듬는다.
- 변경 파일: `docs/social-login-implementation-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Guest 생성·둘러보기에는 전화 인증을 요구하지 않되 LOCAL 가입은 가입 전 OTP로 받은 일회성 `phoneVerificationGrant`를 소비해 User와 PhoneIdentity를 같은 Mongo Transaction으로 생성하도록 정리했다. Guest의 Google·Kakao·Apple MEMBER 승격은 기존 userId를 유지하면서 PhoneIdentity 보유를 선행 조건으로 둔다.
- 구현 내용: 기존 MEMBER 로그인에는 OTP를 반복하지 않고, PhoneIdentity가 없는 legacy MEMBER만 무료시험 또는 향후 전화번호 필수 기능 전에 1회 onboarding하도록 정리했다. 전화 인증 attempt를 가입 시도 또는 JWT `sub`에 묶고 verification TTL·grant TTL·cleanup TTL을 분리했으며 grant 위조·만료·재사용과 `PHONE_VERIFICATION_REQUIRED` 오류 계약을 추가했다.
- 구현 내용: 전화 인증이나 회원가입 성공만으로 `TrialClaim`을 만들지 않고, 첫 무료 모의고사 요청에서 기존 PhoneIdentity를 이용해 별도 Entitlement/Billing이 silent claim·grant하도록 유지했다. 시험 시작 시에는 OTP 화면 없이 entitlement를 consume하며 Identity는 시험·사용권 코드를 소유하지 않는다.
- 실행한 테스트와 결과: 코드 변경이 없는 계획 문서 작업이므로 `./gradlew clean test`는 실행하지 않았다. `git diff --check`가 성공했고 세 문서에 trailing whitespace가 없음을 확인했다. 계획서의 H2 section 20개와 code fence 74개가 정상적으로 짝을 이루며, 지정 turn marker가 정확히 1회이고 폐기한 `PhoneVerifiedForBenefit`·`PhoneVerificationAttempt.userId` 모델 참조가 없음을 정적 검색으로 확인했다.
- 유지한 계약: 실제 userId는 UUID 문자열이고 JWT `sub`는 canonical userId다. 전화번호는 로그인 식별자나 자동 merge 키가 아니며 외부 Request Body의 userId를 신뢰하지 않는다. RS256·JWKS·RefreshSession, Identity/Learning Core 경계와 Python AI `user_id=examId` 계약을 유지했고 Secret·Token·Password·실제 Key·전체 MongoDB URI와 raw 전화번호·OTP를 기록하지 않았다.
- 결정사항: 전화번호가 계정 복구·결제 보호에도 사용된다는 제품 방향을 전제로 신규 MEMBER enrollment 인증을 기본안으로 채택한다. Guest 접근은 유지하고 기존 MEMBER의 매 로그인 재인증은 금지한다. 전화 인증 성공과 무료체험 claim·실제 사용은 각각 분리한다. 첫 구현 PR은 여전히 email 없는 최소 SocialIdentity 모델·index·Repository와 테스트만 포함한다. Jira 변경과 Git commit·push는 수행하지 않았다.
- 위험 요소: 회원가입 단계 OTP는 전환율 저하, 가입하지 않는 사용자의 SMS 비용과 개인정보 수집을 늘린다. 전화번호가 무료체험 중복 방지에만 쓰인다면 시험 직전 인증이 더 적합하며, 번호 재할당·공유와 HMAC rotation·TrialClaim 보존은 여전히 오탐과 개인정보 보존 위험이 있다.
- 다음 작업: 단계 0에서 MEMBER enrollment 정책과 legacy onboarding 지점, OTP·국가·line type·보존 정책을 제품·보안·법무 기준으로 확정한 뒤 첫 SocialIdentity PR을 구현하고 `./gradlew clean test`를 실행한다.

## 2026-08-11 — 무료 모의고사 회원가입·가입 시 전화번호 인증 정책 확정

<!-- codex-turn:019fef68-8951-7b01-9b4f-20b56262e7b6 -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 무료 모의고사는 반드시 회원가입한 사용자만 시작하게 하고, 전화번호 검증을 시험 시작이 아닌 LOCAL·소셜 회원가입 과정에서 완료하도록 전체 구현 계획을 확정한다.
- 변경 파일: `docs/social-login-implementation-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 무료 모의고사 접근 조건을 `ACTIVE MEMBER + PhoneIdentity`로 고정하고 Guest 요청은 `MEMBERSHIP_REQUIRED`로 차단하도록 아키텍처 흐름, 오류 계약, 단계별 구현, 테스트, 배포와 완료 조건에 반영했다. Guest는 전화 인증 없이 둘러볼 수 있지만 claim·entitlement·consume은 생성하거나 호출하지 않는다.
- 구현 내용: LOCAL 가입은 pre-signup `phoneVerificationGrant` 소비와 User·PhoneIdentity 생성을 한 Mongo Transaction으로 처리한다. 미연결 Google·Kakao·Apple identity를 이용한 Guest 소셜 가입은 Guest-bound grant 소비, 기존 userId의 MEMBER 승격, PhoneIdentity·SocialIdentity 생성을 한 Transaction으로 처리하도록 정리했다.
- 구현 내용: Guest MEMBER_ENROLLMENT OTP confirm은 grant만 발급하고 PhoneIdentity를 미리 만들지 않는다. 가입 중단 Guest의 번호 선점을 피하고 최종 PhoneIdentity unique index가 동시 가입의 최종 경계가 된다. 이미 연결된 social identity 로그인·Guest merge와 기존 MEMBER 로그인은 신규 회원가입이 아니므로 OTP를 반복하지 않는다.
- 구현 내용: 회원가입과 전화 인증만으로 TrialClaim을 생성하지 않고 첫 무료 모의고사 요청에서 기존 PhoneIdentity로 silent claim·grant한 뒤 별도 consume한다. PhoneIdentity 없는 legacy MEMBER는 로그인 후 별도 1회 onboarding 대상으로 남긴다.
- 실행한 테스트와 결과: 코드 변경이 없는 계획 문서 작업이므로 `./gradlew clean test`는 실행하지 않았다. `git diff --check`와 세 문서의 trailing whitespace 검사가 성공했고, 계획서 H2 section 20개·code fence 74개의 정상 구조와 회원가입·Guest 차단 정책의 오류·테스트·완료 조건 반영을 정적 검색으로 확인했다.
- 유지한 계약: 실제 userId는 UUID 문자열이고 JWT `sub`는 canonical userId다. 외부 Request Body의 userId를 신뢰하지 않고 전화번호를 로그인 식별자나 자동 merge 키로 사용하지 않는다. RS256·JWKS·RefreshSession, Identity/Learning Core 경계와 Python AI `user_id=examId` 계약을 유지했으며 민감한 인증 정보나 인프라 비밀값을 기록하지 않았다.
- 결정사항: 무료 모의고사는 ACTIVE MEMBER만 시작할 수 있고 신규 LOCAL 또는 미연결 identity의 소셜 MEMBER 회원가입에는 전화 인증 grant가 필수다. 기존 social identity 로그인·merge에는 가입 OTP를 다시 요구하지 않는다. 가입 성공과 무료체험 claim·실제 사용은 계속 분리하며 첫 구현 PR 범위는 최소 SocialIdentity 모델·index·Repository와 테스트로 유지한다. Jira 변경과 Git commit·push는 수행하지 않았다.
- 위험 요소: 가입 필수 OTP는 가입 전환율을 낮추고 무료시험을 사용하지 않는 회원에게도 SMS 비용과 개인정보 수집을 발생시킨다. 번호 재할당·공유, 동시 가입, grant 탈취·재사용, HMAC rotation과 TrialClaim 보존에 대한 보안·법무·운영 정책이 필요하다.
- 다음 작업: legacy MEMBER onboarding 강제 시점과 OTP 국가·line type·rate limit·보존 정책을 확정하고, 첫 SocialIdentity PR 이후 PhoneIdentity·회원가입 Transaction을 단계적으로 구현해 `./gradlew clean test`와 격리 Mongo·staging Transaction 검증을 수행한다.

## 2026-08-11 — Guest 없는 첫 SNS 로그인·직접 회원가입 흐름 추가

<!-- codex-turn:019fef6e-c8cd-7ee1-814e-de984ee6e6ba -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: Guest에서 SNS로 승격하는 경로뿐 아니라 앱 첫 진입에서 Guest를 거치지 않고 Google·Kakao·Apple 버튼으로 기존 계정에 로그인하거나 신규 소셜 회원가입하는 흐름을 전체 구현 계획에 추가한다.
- 변경 파일: `docs/social-login-implementation-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 공개 social login이 Provider credential 검증 후 기존 SocialIdentity를 찾으면 Guest 생성 없이 canonical MEMBER Token을 발급하고, 미연결 identity면 User를 생성하지 않은 채 `SIGNUP_REQUIRED`와 짧은 `socialEnrollmentGrant`를 반환하도록 두 결과를 분리했다.
- 구현 내용: `SocialEnrollmentAttempt`를 DIRECT_SIGNUP 또는 현재 User에 binding하고 Provider credential·전체 Claim·email·authorization code 원문을 저장하지 않도록 계획했다. opaque grant 원문은 클라이언트에 한 번만 전달하고 DB에는 hash만 저장하며 providerSubject는 최종 SocialIdentity 생성을 위해 짧은 TTL 동안만 보유한다.
- 구현 내용: 직접 소셜 가입은 socialEnrollmentGrant에 묶인 공개 전화 인증을 거쳐 같은 signupAttemptId의 phoneVerificationGrant와 필수 동의를 받는다. 최종 Transaction에서 서버 UUID MEMBER User·PhoneIdentity·SocialIdentity·최초 RefreshSession 생성과 두 attempt 소비를 원자적으로 처리하고 canonical userId 기준 Access/Refresh 응답을 반환한다.
- 구현 내용: Guest 전환은 보호된 prepare/finalize API와 USER-bound 두 grant를 사용해 기존 Guest userId를 유지한다. 이미 가입된 SNS identity의 첫 진입 로그인이나 Guest merge는 신규 가입이 아니므로 전화 OTP를 반복하지 않는다. 무료 모의고사의 ACTIVE MEMBER 제한과 TrialClaim lazy claim 정책은 유지했다.
- 실행한 테스트와 결과: 코드 변경이 없는 계획 문서 작업이므로 `./gradlew clean test`는 실행하지 않았다. `git diff --check`와 세 문서의 trailing whitespace 검사가 성공했고, 계획서 H2 section 20개·code fence 80개의 정상 구조를 확인했다. 직접 SNS 진입·SIGNUP_REQUIRED·grant binding·Transaction·오류·테스트·배포·완료 조건이 포함되고 과거의 직접 가입 제외 문구가 없음을 정적 검색으로 검증했다.
- 유지한 계약: 실제 userId는 서버가 생성한 UUID 문자열이고 JWT `sub`는 canonical userId다. 외부 Request Body의 userId를 받지 않고 Provider email·전화번호를 로그인 식별자나 자동 merge 키로 사용하지 않는다. RS256·JWKS·RefreshSession, Identity/Learning Core 경계와 Python AI `user_id=examId` 계약을 유지했으며 민감한 인증 정보나 인프라 비밀값을 기록하지 않았다.
- 결정사항: 첫 SNS 버튼은 기존 identity의 즉시 로그인과 미연결 identity의 DIRECT signup을 모두 지원한다. Provider 검증 성공만으로 User를 만들지 않고 전화 인증·필수 동의까지 완료해야 가입한다. DIRECT signup과 Guest 승격은 별도 binding을 사용하며 첫 구현 PR은 여전히 최소 SocialIdentity 모델·index·Repository와 테스트로 제한한다. Jira 변경과 Git commit·push는 수행하지 않았다.
- 위험 요소: SocialEnrollmentAttempt와 두 bearer grant의 탈취·재사용·binding 혼합, 중단 가입 데이터 보존, 같은 SNS 또는 전화번호의 동시 가입, social 전용 User의 email nullable·필수 profile 정책이 남아 있다. 짧은 TTL·hash 저장·일회성 소비·unique index·전체 Transaction rollback과 API rate limit이 필요하다.
- 다음 작업: 단계 0에서 social 전용 profile·email nullable, enrollment TTL·정리 정책을 확정한 뒤 최소 SocialIdentity PR을 구현한다. 이후 Provider verifier와 SocialEnrollmentAttempt, DIRECT signup·PhoneIdentity Transaction을 순서대로 구현하고 `./gradlew clean test`, 격리 Mongo index와 staging replica set Transaction을 검증한다.

## 2026-08-11 — SNS nonce·socialChallengeId 계약 명확화

<!-- codex-turn:019fef82-8aa5-73f2-97ae-2cd1ddce6138 -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 소셜 API 계획의 `합의된 nonce 또는 challenge 식별자`라는 모호한 표현을 제거하고 nonce의 보안 목적과 서버 발급 socialChallengeId 흐름을 구체적으로 정의한다.
- 변경 파일: `docs/social-login-implementation-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: nonce는 Identity가 SNS 인증 시작 전에 생성하고 Provider SDK에 전달해 ID Token nonce Claim과 비교하는 일회성 값으로 정의했다. socialChallengeId는 nonce나 PKCE code_challenge가 아니라 서버 보관 expected nonce·Provider·purpose·User binding을 조회하는 opaque 인증 시도 식별자로 구분했다.
- 구현 내용: `SocialLoginChallenge`에 provider, LOGIN_OR_SIGNUP/LINK purpose, LINK의 JWT `sub` binding, expectedNonceHash, provider별 nonceMode, PENDING/CONSUMED/EXPIRED 상태와 TTL을 두도록 계획했다. nonce 원문·socialChallengeId는 로그·Sentry·metric에 남기지 않고 application이 만료와 일회성 소비를 직접 검사한다.
- 구현 내용: 공개 login/signup challenge API와 Bearer 보호 link challenge API가 socialChallengeId·providerNonce·만료 정보를 발급하고, 클라이언트는 providerNonce를 SDK에 전달한 뒤 provider credential과 socialChallengeId만 Identity에 반환한다. login/link Body의 raw nonce는 기대값으로 신뢰하지 않는다.
- 구현 내용: verifier는 서버 challenge에서 얻은 expected nonce context를 사용하고 Provider·purpose·User binding 불일치, 만료·재사용을 거절한다. 브라우저 authorization-code 흐름에서는 nonce와 별도로 OAuth state와 PKCE가 필요하며 세 수단은 서로 대체하지 않는다고 명시했다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석·계획 문서 작업이므로 `./gradlew clean test`는 실행하지 않았다. `git diff --check`와 세 문서의 trailing whitespace 검사가 성공했고, 계획서 H2 section 20개·code fence 84개의 정상 구조를 확인했다. 모호한 기존 문구가 제거되고 challenge 모델·API·오류·테스트·배포·완료 조건이 포함됐음을 정적 검색으로 검증했다.
- 유지한 계약: 실제 userId는 서버 생성 UUID 문자열이고 JWT `sub`는 canonical userId다. 외부 Request Body의 userId를 받지 않고 Provider subject·email·전화번호를 로그인 식별자나 자동 merge 키로 사용하지 않는다. RS256·JWKS·RefreshSession, Identity/Learning Core 경계와 Python AI `user_id=examId` 계약을 유지했으며 민감한 인증 정보나 인프라 비밀값을 기록하지 않았다.
- 결정사항: 클라이언트가 보낸 nonce를 신뢰하지 않고 서버 발급 socialChallengeId로 expected nonce를 조회한다. LOGIN_OR_SIGNUP challenge는 공개·rate-limited, LINK challenge는 JWT `sub` binding으로 보호하고 Provider 검증 성공 시 한 번만 소비한다. 이 모델은 SocialEnrollmentAttempt와 별개이며 첫 SocialIdentity PR 범위에는 포함하지 않는다. Jira 변경과 Git commit·push는 수행하지 않았다.
- 위험 요소: Provider SDK마다 raw 또는 변환 nonce 전달 방식이 다를 수 있어 nonceMode를 잘못 구현하면 정상 로그인을 차단하거나 검증을 우회할 수 있다. challenge TTL·재사용 차단·Provider/purpose/User binding이 빠지면 replay와 흐름 혼합 위험이 있으며 browser redirect에서 state·PKCE를 생략하면 nonce만으로 해당 공격을 막을 수 없다.
- 다음 작업: Provider별 SDK 문서에 맞춰 Google·Kakao·Apple nonceMode와 browser/mobile flow를 확정한 뒤 SocialLoginChallenge의 domain·Repository·API·verifier 테스트를 Provider 구현 단계에서 추가한다. 그 전에는 첫 범위인 최소 SocialIdentity 모델·index·Repository를 구현한다.

## 2026-08-11 — TrialClaim·UserEntitlement 역할 구분 설명

<!-- codex-turn:019fef94-f875-7da1-beda-d6aa6ba57501 -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 무료 모의고사 설계의 `TrialClaim`과 `UserEntitlement`가 각각 무엇을 기록하고 왜 두 모델로 분리되는지 명확하게 설명한다.
- 변경 파일: `docs/social-login-implementation-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: TrialClaim을 `benefitType + benefit-scoped phoneFingerprint` 기준의 전역 무료혜택 지급 이력으로 정의했다. 같은 검증 번호가 탈퇴·재가입·다른 User 생성 후 무료권을 다시 받지 못하게 하며 시험 사용 여부에 따라 변경하거나 삭제하지 않는다.
- 구현 내용: UserEntitlement를 canonical userId에 귀속된 실제 이용 권리로 정의했다. 무료 모의고사 grant 시 수량 1로 생성되고 reserve·consume·보상에 따라 상태·수량이 변하며 Learning Core가 시험 시작 전에 확인·차감하는 대상이다.
- 구현 내용: 최초 무료시험 요청에서 TrialClaim insert와 UserEntitlement grant를 Entitlement/Billing의 한 Transaction으로 처리하고, 시험 consume 시에는 entitlement만 변경하며 TrialClaim은 유지하는 상태 예시를 추가했다. 탈퇴 후 동일 번호의 user_B에는 기존 TrialClaim 때문에 무료 entitlement를 재지급하지 않는다.
- 구현 내용: TrialClaim만으로는 사용권의 미사용·예약·사용·보상 상태를 관리할 수 없고 UserEntitlement만으로는 새 User를 이용한 무료혜택 재수령을 막을 수 없음을 설명했다. 유료 구매 entitlement는 TrialClaim 없이 별도 결제 grant source를 가질 수 있도록 역할을 분리했다.
- 실행한 테스트와 결과: 코드 변경이 없는 설명·계획 문서 작업이므로 `./gradlew clean test`는 실행하지 않았다. `git diff --check`와 세 문서의 trailing whitespace 검사가 성공했고, 계획서 H2 section 20개·code fence 86개의 정상 구조와 역할표·상태 흐름·유료 확장 설명 포함을 정적 검색으로 확인했다.
- 유지한 계약: Identity는 PhoneIdentity와 검증 proof만 소유하고 TrialClaim·UserEntitlement·시험 consume을 소유하지 않는다. 실제 userId와 JWT `sub`의 canonical UUID, 외부 Request Body의 userId 비신뢰, 전화번호 비로그인·비merge 키, Identity/Learning Core 경계와 Python AI `user_id=examId` 계약을 유지했으며 민감한 인증 정보나 인프라 비밀값을 기록하지 않았다.
- 결정사항: TrialClaim은 혜택 수령 이력, UserEntitlement는 사용 가능한 권리이므로 하나의 `freeTrialUsed` boolean이나 단일 document로 합치지 않는다. 두 문서는 최초 무료 claim에서 원자적으로 생성하고 실제 시험 권한 판단과 consume은 UserEntitlement만 대상으로 한다. 첫 SocialIdentity PR 범위에는 둘 다 포함하지 않으며 Jira 변경과 Git commit·push는 수행하지 않았다.
- 위험 요소: TrialClaim 보존은 pseudonymous fingerprint의 개인정보 보존과 번호 재할당 오탐을 만들 수 있다. entitlement consume과 exam 생성의 서비스 간 실패에 예약·확정 또는 보상이 없으면 권리가 유실될 수 있고, claim과 grant를 비원자적으로 저장하면 혜택만 소진되거나 중복 지급될 수 있다.
- 다음 작업: 별도 Entitlement/Billing 경계에서 TrialClaim unique index, UserEntitlement 상태·수량·grant source, claim+grant Transaction과 consume idempotency·보상 계약을 확정한다. Identity 저장소의 첫 구현은 계획대로 최소 SocialIdentity 모델·index·Repository부터 진행한다.

## 2026-08-11 — 소셜 로그인 전체 계획 동시성·운영 계약 보강

<!-- codex-turn:019fefb2-a893-7d33-b0b9-3b5d326a643c -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 소셜 로그인·전화번호 인증·무료체험 전체 계획을 추가 설계 검토와 대조해 단계 책임 중복, merge source Token, HMAC key rotation, 무료시험 분산 상태와 동시성 계약을 구현 가능한 수준으로 보강한다.
- 변경 파일: `docs/social-login-implementation-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 첫 PR의 `SocialIdentity` 범위는 유지하면서 `providerSubject`를 case-sensitive opaque 1~255자로 제한했다. 단계 6은 challenge·verifier framework, 단계 7은 기존 로그인·직접 가입·미연결 link, 단계 8은 실제 Guest merge·outbox로 분리하고, 단계 7에서 다른 owner를 발견하면 mutation과 target Token 발급 없이 `MERGE_REQUIRED`를 반환하도록 정리했다.
- 구현 내용: source JWT를 target actor로 승격하거나 authorization alias로 사용하지 않는 공통 정책을 확정했다. Identity의 `ACCOUNT_MERGED_TOKEN_REJECTED`, downstream ownership migration·source deny marker의 로컬 Transaction, source write fencing과 전체 참여 서비스 검증 전 merge feature flag 금지를 계획에 반영했다.
- 구현 내용: SocialLoginChallenge의 조건부 `PENDING → CONSUMED` CAS 승자만 후속 처리를 진행하고 SocialEnrollmentAttempt·PhoneVerificationAttempt도 최종 Transaction 안에서 조건부 소비하도록 명시했다. 가입 proof 누락과 legacy onboarding은 각각 `PHONE_VERIFICATION_GRANT_REQUIRED` 400, `PHONE_ONBOARDING_REQUIRED` 409로 분리했다.
- 구현 내용: raw 번호를 장기 보관하지 않는 HMAC rotation을 위해 `PhoneFingerprintAlias`, `ACTIVE_WRITE → LOOKUP_ONLY → RETIRED` key lifecycle, retained phone·benefit version candidate 조회, legacy key reference gate와 mixed writer 금지를 추가했다. 운영 provider null 문서는 MEMBER fallback 전에 read-only aggregate로 검증하도록 했다.
- 구현 내용: 무료시험 분산 흐름을 `reserve → exam 생성 → confirm`으로 고정하고 확정 실패 cancel, 결과 불명 timeout의 `RECONCILIATION_REQUIRED`, reservationId 기반 exam 존재 대조, CAS·idempotency 불변식을 추가했다. Identity/Social `1→2→3→6→7→8→9→10`과 Entitlement/Billing `4→5→11`을 독립 트랙으로 표현했다.
- 실행한 테스트와 결과: 문서만 변경했으므로 `./gradlew clean test`는 실행하지 않았다. `git diff --check`가 성공했고 세 문서의 trailing whitespace가 없었다. 계획서의 H2 section 20개와 code fence 90개가 정상적으로 짝을 이루며 단계 7의 `MERGE_REQUIRED`와 단계 8의 실제 merge가 분리됐음을 정적 검색으로 확인했다.
- 유지한 계약: 실제 userId는 UUID 문자열이고 JWT `sub`는 canonical userId다. source JWT를 target 권한으로 승격하지 않고 외부 Body의 userId를 신뢰하지 않는다. 전화번호는 로그인·자동 merge 키가 아니며 Identity는 시험·TrialClaim·UserEntitlement·EntitlementReservation을 소유하지 않는다. RS256·JWKS, Identity/Learning Core 경계와 Python AI `user_id=examId`를 유지했고 Secret·Token·Password·raw 전화번호·OTP·fingerprint·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 제시된 P0/P1 검토 의견은 타당해 계획에 반영했다. 첫 PR은 최소 SocialIdentity 모델·index·Repository·테스트로 유지하며 추가 범위는 subject 최대 길이 계약뿐이다. key version이 달라져도 retained alias candidate로 번호·TrialClaim 중복을 찾고, 결과가 불명확한 exam 생성은 확인 없이 entitlement reservation을 해제하지 않는다. Jira 작업과 Git commit·push는 수행하지 않았다.
- 위험 요소: alias/history와 key registry reference count가 구현·운영되지 않으면 version 교차 중복이 가능하고, legacy key 장기 보존은 Secret 관리 부담을 만든다. source JWT 거절은 모든 user-owned 서비스가 deny marker와 write fencing을 구현해야 하며 한 서비스라도 준비되지 않으면 merge를 열 수 없다. reservation reconciliation의 양쪽 상태 조회가 어긋나면 무료 권리 유실 또는 중복 시험이 생길 수 있다.
- 다음 작업: 첫 SocialIdentity PR을 구현하기 전에 Provider별 subject 공식 상한과 격리 Mongo index 검증 방식을 확인한다. 이후 단계 0의 남은 제품·보안·법무 결정을 확정하고 두 트랙을 독립적으로 구현하며 각 코드 변경에서 `./gradlew clean test`와 staging Transaction·동시성 검증을 수행한다.

## 2026-08-11 — 전체 인증·무료체험 계획 사용자 확인용 요약

<!-- codex-turn:019fefe6-d084-7a21-b1e8-005cfc58dc3c -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 사용자가 합의된 소셜 로그인·회원가입 전화 인증·Guest 병합·무료 모의고사 설계를 자신의 이해와 비교할 수 있도록 구현 세부사항을 핵심 역할과 사용자 흐름 중심으로 요약한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드와 `docs/social-login-implementation-plan.md`의 설계 내용은 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: User는 영구 canonical 계정이고 SocialIdentity는 Google·Kakao·Apple 외부 계정 연결이라는 기본 분리를 요약했다. 앱 첫 진입 SNS는 기존 identity 즉시 로그인과 미연결 identity의 전화 인증·동의 후 DIRECT signup으로 나뉘며, Guest의 미연결 SNS 연결은 기존 Guest userId를 유지한 MEMBER 승격임을 정리했다.
- 구현 내용: 전화번호는 회원가입 시 소유를 검증하지만 로그인 식별자나 자동 병합 키로 쓰지 않는다. Guest는 둘러볼 수 있으나 무료 모의고사는 ACTIVE MEMBER만 가능하고, TrialClaim은 검증 번호별 무료혜택 1회 수령 이력, UserEntitlement는 실제 예약·사용되는 권리라는 차이를 설명했다.
- 구현 내용: 기존 SocialIdentity owner가 발견되는 Guest 요청은 준비 단계에서 `MERGE_REQUIRED`로 멈추고, 공통 source JWT 거절·outbox·downstream migration이 준비된 단계 8에서만 ACTIVE GUEST를 ACTIVE MEMBER canonical User로 병합한다. 무료시험은 첫 요청에서 lazy claim한 뒤 `reserve → exam 생성 → confirm`, 확정 실패 cancel, 결과 불명 reconciliation으로 처리한다.
- 실행한 테스트와 결과: 설명과 기록 문서만 갱신했으므로 `./gradlew clean test`는 실행하지 않았다. 종료 전 `git diff --check`, trailing whitespace, 지정 turn marker 단일 존재와 저장소 변경 상태를 정적으로 확인한다.
- 유지한 계약: 실제 userId와 JWT `sub`는 Identity가 생성한 canonical UUID 문자열이다. Provider subject·email·전화번호를 userId로 사용하지 않고 외부 Body의 userId를 신뢰하지 않는다. 전화번호는 자동 merge 키가 아니며 Identity는 시험·TrialClaim·UserEntitlement 코드를 소유하지 않는다. RS256·JWKS, Identity/Learning Core 경계와 Python AI `user_id=examId` 계약을 유지했고 Secret·Token·Password·raw 전화번호·OTP·fingerprint·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 기존 전체 계획을 변경하지 않고 사용자가 비교하기 쉬운 관점으로만 재구성한다. 핵심 비교 기준은 canonical User 분리, 처음부터 SNS 진입 지원, 가입 시 전화 인증, Guest와 MEMBER의 무료시험 경계, 전화번호와 SocialIdentity의 병합 책임 분리, TrialClaim과 UserEntitlement의 역할 분리다. Jira 작업과 Git commit·push는 수행하지 않았다.
- 위험 요소: 압축 요약은 nonce CAS, HMAC key rotation, outbox lease·retry, reservation reconciliation 같은 구현 세부사항을 생략할 수 있으므로 실제 구현 기준은 계속 전체 계획서를 따른다. 단계 0에 남은 Provider·OTP·개인정보 보존·서비스 간 세부 계약은 별도로 확정해야 한다.
- 다음 작업: 사용자가 자신의 이해와 비교해 다른 부분을 알려주면 해당 차이가 제품 정책인지 설계 오해인지 구분해 전체 계획서에 필요한 수정만 반영한다. 합의 후 첫 구현은 최소 SocialIdentity 모델·index·Repository부터 시작한다.

## 2026-08-13 — 생성 예정 엔티티 역할과 전체 로직 설명

<!-- codex-turn:019ff8a7-2500-7b80-b4e2-bbf639fbc12b -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 합의된 전체 계획에서 생성·확장될 엔티티의 소유 서비스, 수명, 책임과 직접 SNS 가입·Guest 승격·병합·무료 모의고사 흐름에서의 상호작용을 사용자가 이해할 수 있도록 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드와 `docs/social-login-implementation-plan.md`의 설계는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Identity의 장기 엔티티를 canonical 계정인 User, 외부 계정 매핑인 SocialIdentity, 검증 번호 소유 관계인 PhoneIdentity, HMAC key version 교차 동일성을 보존하는 PhoneFingerprintAlias, 로그인 세션인 기존 RefreshSession으로 구분했다. 단기 시도 엔티티는 nonce replay를 막는 SocialLoginChallenge, 검증된 SNS principal을 최종 가입까지 이어 주는 SocialEnrollmentAttempt, OTP 요청·검증·grant 상태를 관리하는 PhoneVerificationAttempt로 설명했다.
- 구현 내용: Guest 병합 시 User의 MERGED 상태 전환과 RefreshSession 폐기, UserMergedOutbox 생성, downstream의 eventId 멱등 처리·ownership migration·source deny marker 저장 관계를 정리했다. UserMerged는 전달 payload 개념이고 outbox document가 Identity의 영속 전달 상태임을 구분했다.
- 구현 내용: 별도 Entitlement/Billing 소유의 TrialClaim은 검증 번호별 무료혜택 수령 ledger, UserEntitlement는 canonical User의 실제 사용권 잔액, EntitlementReservation은 시험 생성 중 해당 사용권을 잠그는 분산 작업 상태로 설명했다. 회원가입 때는 PhoneIdentity까지만 생성하고 첫 무료시험 요청에서 claim·grant·reserve한 뒤 exam 생성 결과에 따라 confirm·cancel·reconciliation하도록 정리했다.
- 구현 내용: VerifiedSocialPrincipal, SocialProvider enum, socialEnrollmentGrant와 phoneVerificationGrant는 독립 장기 엔티티가 아니라 각각 검증 결과·namespace·일회성 bearer proof임을 명시했다. direct signup과 Guest enrollment에서 social·phone attempt를 같은 binding으로 묶고 최종 Mongo Transaction의 conditional consume 승자만 User·identity·Session 변경을 commit하는 흐름을 설명했다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석·설명 작업이므로 `./gradlew clean test`는 실행하지 않았다. 종료 전 `git diff --check`, 두 기록 문서의 trailing whitespace, 지정 turn marker 단일 존재와 저장소 변경 상태를 정적으로 확인한다.
- 유지한 계약: 실제 userId와 JWT `sub`는 canonical UUID 문자열이다. Provider subject·email·전화번호를 userId나 자동 병합 키로 사용하지 않고 외부 Body의 userId를 신뢰하지 않는다. Identity는 TrialClaim·UserEntitlement·EntitlementReservation·시험 데이터를 소유하지 않는다. RS256·JWKS, Identity/Learning Core 경계와 Python AI `user_id=examId` 계약을 유지했고 Secret·Token·Password·raw 전화번호·OTP·fingerprint·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 영속 엔티티와 일시적인 검증 결과·grant를 구분해 설명하며, 생성 시점과 삭제·상태 전이의 차이를 사용자 흐름에 연결한다. 기존 전체 계획을 해석한 작업으로 새로운 제품 정책이나 구현 범위를 추가하지 않았다. Jira 작업과 Git commit·push는 수행하지 않았다.
- 위험 요소: TrialClaim·UserEntitlement·EntitlementReservation은 Identity 저장소에 구현하지 않으며 실제 Entitlement/Billing 경계와 API 계약이 별도로 필요하다. PhoneFingerprintAlias key registry, outbox consumer, source JWT deny marker와 reservation reconciliation이 일부만 구현되면 version 교차 중복·병합 후 접근·무료권 유실 문제가 생길 수 있다.
- 다음 작업: 사용자 이해와 다른 부분이 있으면 제품 정책과 기술 구현을 구분해 수정한다. 합의 후 첫 구현은 계획대로 최소 SocialIdentity 모델·index·Repository부터 시작하고 코드 변경 시 `./gradlew clean test`와 격리 Mongo index 검증을 수행한다.

## 2026-08-13 — SNS nonce·challenge·일회성 소비 의미 설명

<!-- codex-turn:019ff8ca-6834-7742-926e-383b67cc0bea -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 서버 발급 SNS 인증 흐름에서 nonce와 SocialLoginChallenge의 역할 차이, Provider Token nonce 검증, challenge를 소비한다는 상태 전이의 의미를 이해하기 쉽게 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드와 `docs/social-login-implementation-plan.md`의 설계는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: nonce를 Identity가 인증 시작마다 생성해 Provider SDK에 전달하는 예측 불가능한 일회성 값으로 설명했다. Provider가 돌려준 ID Token의 nonce Claim과 서버가 challenge에 보관한 expected nonce를 비교해 해당 Token이 바로 그 인증 시도에서 발급됐는지 확인하며, 클라이언트가 login Body로 보낸 nonce를 기대값으로 신뢰하지 않는다고 정리했다.
- 구현 내용: SocialLoginChallenge를 nonce 자체가 아니라 expected nonce hash, Provider, LOGIN_OR_SIGNUP/LINK 목적, 필요 시 JWT `sub` 사용자 binding, 만료 시각과 PENDING/CONSUMED 상태를 보관하는 짧은 수명의 서버 인증 시도 레코드로 구분했다. socialChallengeId는 이 레코드를 찾는 opaque 식별자다.
- 구현 내용: challenge 소비는 물리적 삭제이나 Token 사용을 뜻하는 표현이 아니라, 모든 검증 성공 후 조건부 `PENDING → CONSUMED` 전이를 수행해 승자 한 요청만 후속 로그인·가입·연결을 진행시키는 것을 뜻한다. 이후 같은 challenge나 Provider credential을 다시 보내면 이미 소비된 시도로 거절해 replay와 동시 중복 처리를 막는다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석·설명 작업이므로 `./gradlew clean test`는 실행하지 않았다. `git diff --check`, 두 기록 문서의 trailing whitespace, 지정 turn marker 단일 존재를 정적으로 검증한다.
- 유지한 계약: 서버가 발급한 challenge에서 expected nonce와 Provider·목적·사용자 binding을 조회하며 외부 Body의 nonce나 userId를 신뢰하지 않는다. 실제 userId와 JWT `sub`는 canonical UUID 문자열이고 RS256·JWKS, Identity/Learning Core 경계와 Python AI `user_id=examId` 계약을 유지했다. Secret·Token·Password·nonce 원문·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: nonce는 요청과 Provider 인증 결과의 결속 값, challenge는 그 검증 문맥과 수명주기를 보관하는 서버 측 시도 레코드, consumption은 일회성 사용을 강제하는 원자적 상태 전이로 설명한다. 기존 설계나 구현 범위는 변경하지 않았고 Jira 작업과 Git commit·push는 수행하지 않았다.
- 위험 요소: nonce 비교 전에 challenge를 소비하면 Provider 장애나 검증 실패에도 정상 재시도를 막을 수 있고, 로그인·가입 같은 부수 효과 뒤에 소비하면 동시 요청이 중복 Session이나 계정을 만들 수 있다. 실제 구현에서는 검증 순서와 CAS 승자만 후속 처리를 진행하는 원자성 또는 Transaction 경계를 보장해야 한다.
- 다음 작업: Provider별 raw/hashed nonce 규칙과 challenge TTL을 확정한 뒤 SocialLoginChallenge 구현에서 만료·Provider/purpose/user binding 불일치·재사용·동시 요청 CAS 테스트를 추가한다.

## 2026-08-13 — EmailAvailabilityService 책임과 한계 설명

<!-- codex-turn:019ff8e4-7635-7b52-8b97-cdaf3d786b93 -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 현재 `EmailAvailabilityService`의 입력부터 응답까지의 호출 흐름, 이메일 정규화 기준, 실제 회원가입 중복 방지와의 관계 및 보장하지 않는 범위를 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 공개 `POST /api/v1/auth/check-email`에서 `CheckEmailRequest`가 공백 제거와 `@NotBlank`·`@Email` 검증을 수행하고 Controller가 `EmailAvailabilityService.checkEmail`을 호출하는 흐름을 확인했다. 서비스는 `EmailNormalizer`로 다시 trim하고 `Locale.ROOT` 소문자화한 값을 `UserRepository.existsByNormalizedEmail`에 전달한다.
- 구현 내용: 조회 결과가 없으면 `isAvailable=true`와 사용 가능 메시지, 있으면 정상 200 응답 안에 `isAvailable=false`와 이미 사용 중 메시지를 반환한다. 대소문자와 양끝 공백은 중복 판단에서 구분하지 않지만 Gmail dot·plus 같은 Provider별 별칭 규칙은 적용하지 않는다.
- 구현 내용: availability 결과는 조회 순간의 안내일 뿐 이메일 예약이나 이후 가입 성공을 보장하지 않는다. 조회 후 다른 요청이 먼저 가입할 수 있으므로 `SignupService`가 다시 존재 여부를 검사하고, 동시 가입의 최종 방어는 `normalizedEmail` partial unique index와 `DuplicateKeyException`의 `EMAIL_ALREADY_EXISTS` 변환이 담당함을 구분했다. 탈퇴 tombstone은 이메일 필드를 제거하므로 현재 정책상 해당 이메일을 다시 사용할 수 있다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석·설명 작업이므로 `./gradlew clean test`는 실행하지 않았다. 기존 application test의 사용 가능·불가능·trim·소문자화 검증과 Controller test의 200 응답 계약을 읽어 확인했으며, 종료 전 `git diff --check`, 기록 문서 trailing whitespace와 지정 turn marker 단일 존재를 정적으로 검증한다.
- 유지한 계약: 클라이언트가 보낸 userId를 사용하지 않고 이메일 원문을 로그나 작업 기록에 남기지 않았다. 실제 userId와 JWT `sub`의 canonical UUID, Identity 도메인 경계, RS256·JWKS와 Python AI `user_id=examId` 계약을 변경하지 않았으며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: `EmailAvailabilityService`는 조회와 응답 변환만 담당하고 회원가입 원자성이나 이메일 소유권을 담당하지 않는 application service로 설명한다. 기존 코드·API·정책을 변경하지 않았고 Jira 작업과 Git commit·push는 수행하지 않았다.
- 위험 요소: 인증 없이 호출 가능한 API가 특정 이메일의 가입 여부를 직접 구분하므로 계정 열거에 이용될 수 있다. 운영 공개 전 rate limit·abuse monitoring·제품 메시지 정책을 검토해야 하며, check 결과를 신뢰해 `SignupService`의 중복 검사나 DB unique index를 제거하면 안 된다.
- 다음 작업: 정책상 계정 존재 공개를 허용할지 확정하고, 허용한다면 공개 endpoint의 rate limit과 관측 기준을 마련한다. 기능 변경이 필요할 때에는 Controller·application service·동시 가입 unique-index 방어 테스트를 함께 유지한다.

## 2026-08-13 — 첫 SocialIdentity 구현 범위 재확인

<!-- codex-turn:019ff8e8-5dcf-70d3-a996-378bd604c55b -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 중단된 설명을 이어서 전체 소셜 로그인 로드맵 중 첫 구현 PR에 실제로 포함되는 항목과 제외되는 후속 항목을 명확히 구분한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드와 `docs/social-login-implementation-plan.md`의 합의된 범위는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 첫 범위는 `SocialProvider(GOOGLE, KAKAO, APPLE)`, `SocialIdentity(socialIdentityId, userId, provider, providerSubject[1..255], createdAt)`, `(provider, providerSubject)` compound unique index, `userId` non-unique index와 `findByProviderAndProviderSubject`·`findAllByUserId` Repository 계약으로 한정됨을 재확인했다.
- 구현 내용: 도메인 생성 불변식, Provider별 동일 문자열 subject 허용, 한 User의 복수 Provider 연결, provider+subject와 userId 조회, index metadata·실제 unique enforcement를 외부 Atlas 없이 검증하는 테스트가 첫 범위에 포함된다. 실제 중복 insert 검증 환경은 구현 시 격리 Mongo 방식을 선택해야 한다.
- 구현 내용: email·updatedAt, User·UserAccountType migration, Google·Kakao·Apple Token verifier, SocialLoginChallenge·SocialEnrollmentAttempt, Controller와 login/signup/link API, Guest 승격·병합, PhoneIdentity·OTP, TrialClaim·UserEntitlement·EntitlementReservation, outbox·Learning Core event와 Access/Refresh 발급 로직 변경은 첫 범위에서 제외됨을 정리했다.
- 실행한 테스트와 결과: 코드 변경이 없는 범위 확인·기록 작업이므로 `./gradlew clean test`는 실행하지 않았다. 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 저장소 변경 상태를 정적으로 확인한다.
- 유지한 계약: 실제 userId와 JWT `sub`는 canonical UUID 문자열이며 SocialIdentity는 외부 identity에서 canonical userId로의 매핑만 소유한다. email은 로그인 식별자가 아니고 전화번호도 로그인·자동 병합 키가 아니다. Identity/Learning Core 경계와 기존 RS256·JWKS·Access/Refresh 계약을 변경하지 않았으며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 첫 PR의 완료 결과는 SNS 연결 정보를 안전하게 저장·조회할 수 있는 데이터 기반이며 실제 SNS 로그인이 동작하는 상태가 아니다. 첫 PR에 후속 User·API·OTP·merge·entitlement 범위를 섞지 않는다. Jira 작업과 Git commit·push는 수행하지 않았다.
- 위험 요소: 현재 test profile은 Mongo 자동설정을 제외하므로 annotation metadata 테스트만으로 실제 unique insert 차단까지 증명할 수 없다. 격리 Mongo 또는 승인된 staging 검증 없이 DB 최종 동시성 방어가 완료됐다고 선언하면 안 된다.
- 다음 작업: 첫 구현을 승인하면 저장소 구조를 다시 확인해 최소 SocialIdentity entity·enum·Repository·index 테스트를 작성하고 `./gradlew clean test`를 실행한다. 실제 OAuth Provider와 Atlas는 기본 테스트에서 호출하지 않는다.

## 2026-08-13 — 회원 탈퇴 tombstone과 필드 제거 의미 설명

<!-- codex-turn:019ff8e8-38c7-7ad2-8eef-6aa0124e2ae0 -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: `tombstone에서 이메일을 제거한다`는 표현이 사용자 문서 삭제와 어떻게 다른지, 현재 회원 탈퇴 구현에서 남는 정보와 제거되는 정보를 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: tombstone은 탈퇴 시 `users` document를 물리 삭제하는 대신 동일한 `userId`를 유지하면서 `status=WITHDRAWN`으로 표시한 최소 잔존 레코드라고 설명했다. 현재 구현은 nickname을 `탈퇴한 사용자`로 치환하고 `updatedAt`·`withdrawnAt`을 탈퇴 시각으로 기록하며 기존 생성 시각, provider와 consent 구조는 유지한다.
- 구현 내용: `User.toWithdrawnTombstone`은 email·normalizedEmail·passwordHash·guestInstallationIdHash를 null인 탈퇴 객체로 만들고, `UserRepositoryCustomImpl.withdrawIfUnchanged`는 MongoDB `$unset`으로 실제 document에서 네 필드를 제거한다. 따라서 `tombstone에서 제거`는 tombstone 자체 삭제가 아니라 그 안의 개인정보·자격증명 필드를 없앤다는 의미로 구분했다.
- 구현 내용: normalizedEmail 필드가 사라지면 partial unique index 대상에서도 빠져 같은 이메일의 신규 가입이 가능해진다. 남은 userId와 WITHDRAWN 상태는 탈퇴 계정임을 식별하고 활성 사용자로 취급하지 않기 위한 표식이며, 별도 탈퇴 Transaction은 기존 RefreshSession도 폐기한다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석·설명 작업이므로 `./gradlew clean test`는 실행하지 않았다. 탈퇴 entity 변환과 custom repository `$unset` 구현 및 관련 테스트 위치를 읽어 확인했으며, 종료 전 `git diff --check`, 기록 문서 trailing whitespace와 지정 turn marker 단일 존재를 정적으로 검증한다.
- 유지한 계약: 탈퇴 후 credential 필드를 보존하거나 로그에 노출하지 않고 실제 userId와 JWT `sub`의 canonical UUID, Identity 도메인 경계, RS256·JWKS와 Python AI `user_id=examId` 계약을 변경하지 않았다. Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: tombstone과 hard delete를 구분하고, `제거`의 목적어가 user document가 아니라 email·normalizedEmail·passwordHash·guestInstallationIdHash 필드임을 명확히 설명한다. 기존 데이터 보존 정책이나 코드는 변경하지 않았고 Jira 작업과 Git commit·push는 수행하지 않았다.
- 위험 요소: tombstone에 현재 유지되는 provider·consent 정보의 법적 보존 근거와 기간은 별도 개인정보 정책 검토가 필요하다. tombstone은 Identity 레코드의 상태 표시이며 Learning Core 등 다른 서비스의 사용자 데이터 삭제·익명화를 자동으로 보장하지 않는다.
- 다음 작업: 개인정보 보존·삭제 정책에서 tombstone의 필드별 보존 근거와 기간, downstream 데이터 삭제 또는 익명화 계약을 확정한다.

## 2026-08-13 — 첫 SocialIdentity Jira 생성 초안 작성

<!-- codex-turn:019ff8eb-64b0-7c12-a380-f49b7763ec5a -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 첫 구현 범위인 최소 SocialIdentity 모델·MongoDB index·Repository 작업을 TMI 프로젝트의 신규 Jira 이슈로 등록하기 전에, 생성할 내용을 정확히 작성하고 사용자 승인용 초안을 제시한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드와 `docs/social-login-implementation-plan.md`는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Atlassian 공식 MCP로 기존 Identity 이슈와 TMI 프로젝트 생성 metadata를 읽기 전용 조회했다. TMI에는 `작업` 유형이 있고 프로젝트·이슈 유형·요약이 필수이며 보고자는 기본값을 사용할 수 있음을 확인했다. SocialIdentity 관련 유사 이슈는 검색 결과에서 발견되지 않았다.
- 구현 내용: 생성 초안은 TMI 프로젝트의 `작업`, 제목 `[Identity] SocialIdentity 모델·인덱스·Repository 기반 구축`으로 준비했다. 담당자·우선순위·라벨·상태 전환은 지정하지 않고 Jira 기본값을 사용하며, 설명에는 배경·목표·도메인 모델·index·Repository·기능 요구사항·완료 조건·테스트·제외 범위·유지 계약을 포함한다.
- 구현 내용: 첫 범위에는 `SocialProvider(GOOGLE, KAKAO, APPLE)`, email·updatedAt 없는 SocialIdentity, provider+subject unique index, userId index, 두 Repository 조회 계약과 테스트만 포함한다. Provider 검증·API·User 변경·Guest 승격·병합·PhoneIdentity·OTP·Entitlement·outbox·Access/Refresh 변경은 Jira 제외 범위로 명시한다.
- 실행한 테스트와 결과: 코드 변경이 없는 Jira 조회·초안 작성 작업이므로 `./gradlew clean test`는 실행하지 않았다. 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 저장소 상태를 정적으로 확인한다.
- 유지한 계약: 실제 userId와 JWT `sub`는 canonical UUID 문자열이고 SocialIdentity는 외부 identity에서 canonical userId로의 매핑만 소유한다. email을 로그인 식별자로 사용하지 않고 외부 Body의 userId를 신뢰하지 않는다. Identity/Learning Core 경계와 기존 RS256·JWKS·Access/Refresh 계약을 변경하지 않았으며 Jira나 기록에 Secret·Token·Password·실제 Key·전체 MongoDB URI·사용자 개인정보를 넣지 않았다.
- 결정사항: 저장소 규칙에 따라 생성 전에 전체 Jira 초안을 사용자에게 보여주고 명시적 승인을 받는다. 이번 작업에서는 Jira 생성·수정·댓글·상태 변경을 수행하지 않았고 발급된 Jira 키도 없다. Git commit·push도 수행하지 않았다.
- 위험 요소: 담당자·우선순위·라벨을 지정하지 않으므로 프로젝트 기본값이 적용되며, 사용자가 이를 요구하면 생성 승인 전에 초안과 실행 payload를 수정해야 한다. 실제 Mongo unique enforcement 테스트 방식은 구현 착수 시 격리 Mongo 환경으로 확정해야 한다.
- 다음 작업: 사용자가 제시된 Jira 초안을 승인하면 동일 내용으로 이슈를 한 번 생성하고 결과 키·상태를 재조회한다. 생성 후 Jira 키를 CURRENT_STATE와 새 WORKLOG 항목에 기록하고 구현 전 해당 이슈를 다시 읽어 완료 조건을 기준으로 작업한다.

## 2026-08-13 — TMI-88 SocialIdentity 기반 작업 Jira 생성

<!-- codex-turn:019ff8ee-b9b0-7a52-b4fd-b1ce6b0ca1a0 -->

- 날짜: 2026-08-13
- Jira: TMI-88
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 사용자가 승인한 첫 SocialIdentity 구현 초안을 TMI 프로젝트의 Jira 작업으로 생성하고 생성 결과와 저장된 계약을 재확인한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드와 `docs/social-login-implementation-plan.md`는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Atlassian 공식 MCP로 Jira `TMI-88` `[Identity] SocialIdentity 모델·인덱스·Repository 기반 구축`을 `작업` 유형으로 한 번 생성했다. 승인된 설명에는 SocialProvider, email·updatedAt 없는 SocialIdentity, provider+subject unique index, userId index, Repository 계약, 완료 조건·테스트·제외 범위·기존 계약을 기록했다.
- 수행한 Jira 작업: 신규 이슈 생성 후 `TMI-88`을 재조회해 프로젝트 `TMI`, 유형 `작업`, 상태 `해야 할 일`, 담당자 없음, 프로젝트 기본 우선순위 `Medium`, 빈 label·component와 승인한 본문이 저장됐음을 확인했다.
- 추가한 댓글의 목적: 댓글은 추가하지 않았다.
- 변경한 상태: 생성 기본 상태인 `해야 할 일`을 유지했고 별도 상태 전환은 수행하지 않았다.
- 승인 여부: 사용자가 전체 Jira 초안을 확인한 뒤 명시적으로 생성을 승인했다.
- 실행한 테스트와 결과: 코드 변경이 없는 Jira 생성·문서 기록 작업이므로 `./gradlew clean test`는 실행하지 않았다. 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 저장소 상태를 정적으로 확인한다.
- 유지한 계약: 실제 userId와 JWT `sub`는 canonical UUID 문자열이며 SocialIdentity는 외부 identity에서 canonical userId로의 매핑만 소유한다. email을 로그인 식별자로 사용하지 않고 외부 Body의 userId를 신뢰하지 않는다. Identity/Learning Core 경계와 기존 RS256·JWKS·Access/Refresh 계약을 변경하지 않았으며 Jira나 기록에 Secret·Token·Password·실제 Key·전체 MongoDB URI·사용자 개인정보를 넣지 않았다.
- 결정사항: 첫 구현 범위는 Jira `TMI-88`의 설명과 완료 조건을 기준으로 하며 Provider 검증·API·User 변경·Guest 승격·병합·PhoneIdentity·OTP·Entitlement·outbox·Access/Refresh 변경은 포함하지 않는다. Jira 댓글·상태 전환과 Git commit·push는 수행하지 않았다.
- 위험 요소: 현재 test profile은 Mongo 자동설정을 제외하므로 실제 duplicate insert unique enforcement를 증명할 격리 Mongo 방식이 구현 전에 필요하다. 이 범위를 넘는 요구는 TMI-88에 섞지 않고 후속 이슈로 분리해야 한다.
- 다음 작업: 구현을 시작할 때 Jira `TMI-88`을 다시 읽고 완료 조건을 기준으로 entity·enum·Repository·index 테스트를 작성한다. 전체 `./gradlew clean test`와 외부 Atlas·OAuth Provider를 호출하지 않는 격리 Mongo 검증을 수행하고 Jira 댓글·상태 변경은 별도 승인 후 진행한다.

## 2026-08-13 — GuestAuthService 생성·인증·원자성 설명

<!-- codex-turn:019ff900-842c-7793-b9ba-b876dcb00cfc -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: `GuestAuthService`가 Guest 생성 요청에서 검증·중복 방지·User 및 Token 준비·Transaction 저장·오류 변환을 어떻게 조율하며 무엇을 보장하지 않는지 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 공개 `POST /api/v1/auth/guest`의 `GuestAuthRequest`가 canonical UUID v4 설치 식별자와 필수 개인정보·약관 동의 및 현재 정책 버전을 받고, Service가 `ConsentPolicy`를 먼저 검증하는 흐름을 확인했다. 실패하면 User·Token·Session 준비를 시작하지 않는다.
- 구현 내용: 설치 UUID는 정규화 후 SHA-256·Base64URL hash로 변환하고 원문 대신 hash만 User에 저장한다. 이 hash로 사전 중복 조회하며 `guestInstallationIdHash` partial unique index가 동시 요청의 최종 중복을 막는다. 설치 UUID는 중복 방지 식별자일 뿐 인증 credential이나 기존 Guest 복구 수단이 아니므로 같은 설치의 재호출은 Token 재발급 없이 `GUEST_ALREADY_EXISTS` 409가 된다.
- 구현 내용: 중복이 없으면 서버 UUID, GUEST provider, ACTIVE 상태, 고정 Guest nickname, email·password 없는 User와 현재 동의 시각을 만든다. User 저장 전에 RS256 Access Token과 원문을 DB에 저장하지 않는 RefreshSession을 준비하고, `GuestRegistrationTransactionService`가 User와 RefreshSession 저장 및 응답 구성을 Mongo Transaction으로 함께 commit한다. 중간 실패 시 두 document를 rollback하고 Transaction proxy 반환 후에만 안전한 완료 로그를 남긴다.
- 구현 내용: 같은 설치의 동시 요청이 모두 사전 조회를 통과해도 unique 충돌 후 해당 설치 hash의 실제 승자가 존재할 때만 `GUEST_ALREADY_EXISTS`로 변환한다. 다른 unique index 충돌은 Guest 중복으로 오분류하지 않는다. commit 뒤 응답이 유실되어 재시도해도 설치 UUID만으로 기존 Token을 다시 받을 수 없고 conflict가 반환되는 비복구 정책임을 설명했다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석·설명 작업이므로 `./gradlew clean test`는 실행하지 않았다. Service·Transaction service·request/response·hasher·Factory 코드와 기존 동의 실패, Token 준비 실패, rollback, 동시 중복, 응답 유실 재시도 테스트를 읽어 확인했다. 종료 전 `git diff --check`, 기록 문서 trailing whitespace와 지정 turn marker 단일 존재를 정적으로 검증한다.
- 유지한 계약: 실제 Guest userId는 서버 생성 UUID이고 Access Token `sub`에 사용된다. 설치 UUID를 userId나 인증 credential로 사용하지 않고 원문 설치 UUID·Access Token·Refresh Token을 로그나 DB에 보존하지 않는다. RS256·JWKS, Identity/Learning Core 경계와 Python AI `user_id=examId` 계약을 유지했으며 Secret·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: `GuestAuthService`는 Guest 등록 유스케이스의 순서를 조율하고 실제 다중 document 원자성은 proxy 경계가 분리된 `GuestRegistrationTransactionService`가 담당한다고 설명한다. 기존 코드·API·정책을 변경하지 않았고 Jira 작업과 Git commit·push는 수행하지 않았다.
- 위험 요소: 설치 UUID와 Token을 모두 잃거나 commit 응답이 유실되면 설치 UUID만으로 Guest를 복구할 수 없다. Guest Access Token은 stateless라 탈퇴나 로그아웃 후에도 최대 TTL 동안 암호학적으로 유효할 수 있고, 설치 UUID 원문은 엔트로피가 제한된 식별자여서 hash 저장이 강한 비밀성을 뜻하지 않는다. 실제 Mongo Transaction은 replica set 환경에서 검증해야 한다.
- 다음 작업: 클라이언트가 최초 성공 응답의 Token을 안전하게 저장하고 응답 불명 상황을 처리하는 UX를 확정한다. 필요하다면 설치 UUID를 credential로 승격하지 않는 별도의 Guest 복구·재발급 계약을 설계하고, staging replica set에서 Transaction 및 동시 중복을 재검증한다.

## 2026-08-13 — TMI-88 최소 SocialIdentity 데이터 기반 구현

<!-- codex-turn:019ff926-cbf7-7a60-ab58-7f99d28a6d86 -->

- 날짜: 2026-08-13
- Jira: TMI-88
- 브랜치: `feat/TMI-88-social-identity-foundation` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: Google·Kakao·Apple 인증 API 전에 외부 identity와 canonical User를 분리해 저장·조회하는 최소 SocialIdentity 모델, MongoDB index, Repository와 실제 중복 차단 테스트를 Jira 완료 조건대로 구현한다.
- 변경 파일: `build.gradle`, `src/main/java/web/tosunsaeng/identity/domain/auth/domain/enums/SocialProvider.java`, `src/main/java/web/tosunsaeng/identity/domain/auth/domain/entity/SocialIdentity.java`, `src/main/java/web/tosunsaeng/identity/domain/auth/domain/repository/SocialIdentityRepository.java`, `src/test/java/web/tosunsaeng/identity/domain/auth/domain/entity/SocialIdentityTests.java`, `src/test/java/web/tosunsaeng/identity/domain/auth/domain/repository/SocialIdentityRepositoryTests.java`, `src/test/java/web/tosunsaeng/identity/domain/auth/domain/repository/SocialIdentityRepositoryIntegrationTests.java`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 변경했다. 기존 untracked 계획 문서는 이번 구현에서 수정하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Auth 도메인에 `SocialProvider(GOOGLE, KAKAO, APPLE)`와 `social_identities` collection의 `SocialIdentity`를 추가했다. entity는 서버 생성 UUID `socialIdentityId`, canonical UUID 문자열 `userId`, non-null provider, case-sensitive opaque 원문을 보존하는 1~255자 `providerSubject`, non-null `createdAt`만 보유한다.
- 구현 내용: `uk_social_identities_provider_subject`를 provider ASC·providerSubject ASC의 compound unique index로, `ix_social_identities_user_id`를 non-unique index로 선언했다. email·updatedAt·Provider Token·전체 Claim·User 객체·`@DBRef`와 `(userId, provider)` unique 제약은 추가하지 않았다.
- 구현 내용: `SocialIdentityRepository`에 `findByProviderAndProviderSubject`와 `findAllByUserId`를 추가했다. `mongo-java-server` test dependency를 사용해 외부 Atlas 없이 임의 로컬 포트의 순수 Java 인메모리 Mongo를 테스트마다 기동하고 Spring Data Repository proxy와 index resolver의 실제 저장·조회·index enforcement를 검증했다.
- 실행한 테스트와 결과: `./gradlew test --tests '*SocialIdentity*'`에서 신규 13개 테스트가 성공했다. 첫 `./gradlew clean test`는 샌드박스의 사용자 Gradle cache lock 접근 제한으로 코드 실행 전에 중단됐고, 승인된 동일 명령을 샌드박스 밖에서 재실행해 44개 suite·308개 테스트가 skip 0·failure 0·error 0으로 성공했다. `git diff --check`도 성공했다.
- 유지한 계약: 실제 userId와 JWT `sub`는 canonical UUID 문자열이다. Provider subject와 email을 userId로 사용하거나 외부 Body에 userId를 추가하지 않았다. 기존 User·UserProvider, Controller, Provider verifier, Access/Refresh Token, RS256·kid·issuer·audience·JWKS와 Identity/Learning Core 및 Python AI `user_id=examId` 계약을 변경하지 않았다. Secret·Token·Password·실제 Key·전체 MongoDB URI·Provider 개인정보를 기록하지 않았다.
- 결정사항: 애플리케이션 선조회가 아니라 MongoDB `(provider, providerSubject)` unique index를 외부 계정 단일 owner의 최종 경쟁 경계로 둔다. subject는 trim·case folding하지 않고 원문을 저장하며 Provider namespace가 다르면 같은 문자열을 허용한다. 실제 duplicate insert 자동화는 Docker·Atlas 없이 실행 가능한 격리 Mongo로 구현했다. Jira 댓글·상태·필드는 변경하지 않았고 Git commit·push도 수행하지 않았다.
- 위험 요소: 인메모리 Mongo 테스트는 Repository query와 index 동작을 자동 회귀하지만 운영 MongoDB 버전·권한·기존 데이터·무중단 index build 영향을 대체하지 않는다. 운영 writer 활성화 전 staging에서 두 index의 실제 생성과 중복 데이터 부재를 확인해야 한다. `providerSubject` 255자 상한은 Provider verifier 구현 전에 각 공식 계약과 Mongo index byte 한도를 다시 확인한다.
- 다음 작업: 사용자가 변경을 검토한 뒤 직접 commit·push한다. Jira 댓글 초안은 SocialProvider·SocialIdentity·index·Repository 구현, 변경 파일, 전체 44개 suite·308개 테스트 성공과 staging index 검증 위험만 포함해 제시하되 승인 전에는 등록하지 않는다. 다음 구현은 별도 범위인 User accountType·PhoneIdentity 또는 Provider challenge/verifier 순서를 계획에 따라 진행한다.

## 2026-08-13 — Guest 설치 UUID SHA-256과 응답 유실 처리 상태 설명

<!-- codex-turn:019ff940-a866-7872-95c0-e2c9f8937de6 -->

- 날짜: 2026-08-13
- 브랜치: `feat/TMI-88-social-identity-foundation` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: Guest 설치 UUID의 SHA-256 변환 과정과 보안적 성격을 설명하고, Guest 생성 commit 후 응답 유실에 대한 현재 구현과 미구현 복구 범위를 구분한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: `GuestInstallationIdHasher`가 입력을 trim한 뒤 canonical UUID v4·RFC variant인지 검증하고 `UUID.toString()`의 lowercase 문자열로 정규화함을 확인했다. 그 UTF-8 bytes에 Java `MessageDigest`의 SHA-256을 적용해 32 bytes digest를 만들고 padding 없는 Base64URL 문자열 43자로 변환해 `guestInstallationIdHash`에 저장한다.
- 구현 내용: SHA-256은 같은 입력에서 같은 digest가 나오는 단방향 fingerprint라 동일 설치 중복 조회에 사용할 수 있지만 복호화하는 암호화 방식이 아니다. 현재 구현은 random salt나 서버 비밀키 HMAC을 사용하지 않는다. UUID v4의 높은 무작위성 때문에 일반 짧은 비밀번호와 같은 사전대입 대상은 아니지만 hash 저장만으로 원문이 인증 비밀이 되는 것은 아니라고 설명했다.
- 구현 내용: 서버가 User·RefreshSession을 commit한 뒤 HTTP 응답이 클라이언트에 도착하지 않는 네트워크 유실을 감지하거나, 설치 UUID로 기존 Refresh Token 원문을 복원·재발급하는 코드는 없다. 원문 Refresh Token은 DB에 저장하지 않으므로 그대로 되돌릴 수도 없다. 같은 설치 UUID 재시도는 기존 중복 검사 코드에 의해 `GUEST_ALREADY_EXISTS`가 되는 현재 실패 정책이다.
- 구현 내용: `commitThenResponseLossRetryStaysConflictWithoutPreparingAnotherToken` 테스트가 첫 등록이 반영된 상황에서 재시도하면 새 Token이나 Transaction을 준비하지 않고 conflict가 반환됨을 검증한다. 따라서 응답 유실 상황의 현재 동작과 회귀 테스트는 구현되어 있지만, 응답 복구·멱등 재응답·별도 Guest recovery API는 구현되지 않았다고 구분했다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석·설명 작업이므로 이번 turn에서 Gradle 테스트를 새로 실행하지 않았다. hasher 구현과 기존 응답 유실 재시도 테스트를 읽어 확인했으며, 종료 전 `git diff --check`, 기록 문서 trailing whitespace와 지정 turn marker 단일 존재·WORKLOG 끝 append를 정적으로 검증한다.
- 유지한 계약: 설치 UUID는 중복 방지 식별자일 뿐 userId나 인증 credential이 아니며 실제 Guest userId와 JWT `sub`는 서버 생성 UUID다. 원문 설치 UUID·Access Token·Refresh Token을 로그나 작업 기록에 남기지 않고 Refresh Token 원문을 DB에 저장하지 않는 계약, RS256·JWKS와 Identity/Learning Core 경계를 유지했다. Secret·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 현재 구현과 미구현을 분리해, SHA-256 중복 fingerprint와 재시도 conflict는 구현 완료 상태이고 유실 응답의 Token 복구는 미구현 상태로 설명한다. Guest recovery를 추가하더라도 설치 UUID 자체를 credential로 승격하지 않는 별도 인증·멱등성 설계가 필요하다. Jira 변경과 Git commit·push는 수행하지 않았다.
- 위험 요소: commit 성공과 클라이언트 수신 실패를 구분할 수 없는 ambiguous outcome에서 현재 클라이언트는 Guest 계정과 Token을 잃을 수 있다. 단순히 installationId만으로 Token을 재발급하면 탈취된 설치 식별자가 인증 수단이 되며, Refresh Token hash에서는 원문 Token을 복원할 수 없다.
- 다음 작업: 제품에서 응답 유실 복구가 필요한지 결정한다. 필요하다면 짧은 수명의 서버 발급 idempotency key·일회성 등록 challenge 또는 별도 복구 credential을 검토하고, 응답 재전송 시 Token 원문 보관 문제와 재사용·탈취 방어를 함께 설계한다.

## 2026-08-13 — IssuedRefreshSession 발급 결과 record 설명

<!-- codex-turn:019ff96f-0b38-7402-bde5-30f283b6cc43 -->

- 날짜: 2026-08-13
- 브랜치: `feat/TMI-89-user-account-type` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: `IssuedRefreshSession`의 record 문법, 필드 의미, 생성 불변식, RefreshSession 저장 흐름에서의 위치와 Token 비노출 `toString()`을 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: `IssuedRefreshSession`은 Mongo entity나 Session 자체가 아니라 `RefreshSessionIssuer.savePrepared` 또는 rotation 저장 성공 후 application 계층에 반환되는 immutable 값 객체다. 클라이언트에 한 번 전달할 Refresh Token 원문과 실제 저장된 Session 기준 발급·만료 시각을 함께 운반한다.
- 구현 내용: Java record가 세 final component, 동일 이름의 `tokenValue()`·`issuedAt()`·`expiresAt()` 접근자와 값 기반 `equals`·`hashCode`를 자동 생성함을 설명했다. compact constructor는 필드 대입 문법 없이 canonical constructor 입력을 검증하며 세 값의 non-null과 만료 시각이 발급 시각보다 엄격하게 뒤라는 불변식을 강제한다.
- 구현 내용: record의 기본 `toString()`은 모든 component를 출력해 Token 원문을 노출하므로 이를 재정의해 `tokenValue=redacted`만 표시하고 두 시각만 남긴다. 사용자가 붙여넣은 코드의 줄 시작 `- ", expiresAt="`는 Java 연산자가 아니며 실제 저장소 코드는 문자열 연결 `+ ", expiresAt="`임을 확인했다.
- 구현 내용: `PreparedRefreshSession`은 저장 전 Token 원문과 hash가 든 RefreshSession entity를 묶고, `IssuedRefreshSession`은 저장 후 응답에 필요한 원문과 시간만 묶는다는 차이를 정리했다. DB에는 Token 원문이 아니라 entity의 hash만 저장되고 `AuthResponseConverter`가 이 결과로 응답 원문과 TTL을 구성한다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석·설명 작업이므로 이번 turn에서 Gradle 테스트를 실행하지 않았다. 실제 record, `RefreshSessionIssuer`, prepared session과 호출부를 읽어 확인했으며 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 WORKLOG 끝 append를 정적으로 검증한다.
- 유지한 계약: Refresh Token 원문은 DB·로그·작업 기록에 저장하지 않고 내부 발급 결과와 클라이언트 응답으로만 전달한다. 실제 userId와 JWT `sub`, RS256·JWKS, Identity/Learning Core 경계를 변경하지 않았으며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: `IssuedRefreshSession`을 영속 Session이 아니라 저장 성공 결과 DTO로 설명하고, constructor invariant와 안전한 문자열 표현을 보안 경계로 강조한다. 기존 코드·Jira를 변경하지 않았고 Git commit·push를 수행하지 않았다.
- 위험 요소: record는 불변이어도 Token 원문을 메모리에 보유하므로 디버거·heap dump·직렬화·별도 로깅에 노출되지 않도록 계속 제한해야 한다. 향후 component를 추가하면 재정의한 `toString()`과 JSON 직렬화 경계를 함께 검토해야 하며, 발급 시각과 만료 시각은 DB 저장 결과와 일치해야 한다.
- 다음 작업: Refresh Session 발급 경로 변경 시 null·시간 역전·redacted `toString()`·DB 원문 비저장 테스트를 유지하고, application 내부 결과가 불필요하게 Controller 밖이나 비동기 로깅 경로로 전달되지 않는지 검토한다.

## 2026-08-13 — Guest 설치 UUID 해시 로직 상세 설명

<!-- codex-turn:019ff94c-2316-7761-a003-118b89f584c9 -->

- 날짜: 2026-08-13
- 브랜치: `feat/TMI-88-social-identity-foundation` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: `GuestInstallationIdHasher`의 애플리케이션 처리 순서와 SHA-256 내부 digest 계산 원리를 구분해 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 입력 설치 식별자를 trim하고 길이 36의 canonical UUID v4·RFC variant인지 검증한 뒤 `UUID.toString()`으로 lowercase 정규화하는 전처리를 설명했다. 정규화 문자열을 UTF-8 bytes로 바꿔 Java `MessageDigest(SHA-256)`에 전달한다.
- 구현 내용: SHA-256은 입력에 길이 정보를 포함한 padding을 붙이고 512-bit block으로 나눈 뒤, 각 block을 64개 word의 message schedule로 확장해 8개 32-bit 내부 상태에 64 round의 rotate·shift·XOR·AND·NOT·modulo 2^32 addition을 적용한다. 모든 block 처리 후 8개 상태를 이어 붙인 고정 256-bit·32-byte digest를 반환한다고 설명했다.
- 구현 내용: digest의 padding 없는 Base64URL encoding은 binary 값을 URL·문서에 안전한 43자 문자열로 표현하는 단계일 뿐 hashing이나 암호화가 아니다. 중복 조회 때에도 같은 installationId에 같은 정규화·SHA-256·Base64URL 파이프라인을 적용해 저장된 `guestInstallationIdHash`와 비교하며, 원문 복구는 수행하지 않는다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석·설명 작업이므로 이번 turn에서 Gradle 테스트를 실행하지 않았다. 기존 hasher 구현을 기준으로 설명했으며 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 WORKLOG 끝 append를 정적으로 검증한다.
- 유지한 계약: 설치 UUID는 중복 방지 식별자이며 userId나 인증 credential이 아니다. 실제 Guest userId와 JWT `sub`는 서버 생성 UUID이고 설치 UUID 원문을 DB·로그·작업 기록에 남기지 않는다. Refresh Token 원문 비저장, RS256·JWKS와 Identity/Learning Core 경계를 유지했으며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 애플리케이션의 hash pipeline과 SHA-256 표준 알고리즘 내부를 구분하고, Base64URL을 hash 자체로 오해하지 않도록 설명한다. 현재 구현은 deterministic SHA-256 fingerprint이고 salt·HMAC·암호화는 사용하지 않는다. Jira 변경과 Git commit·push는 수행하지 않았다.
- 위험 요소: SHA-256의 단방향성만으로 installationId가 인증 비밀이 되지는 않는다. 낮은 엔트로피 입력에는 대입 공격이 가능하고 현재 구현에는 서버 비밀키가 없으므로 이 값을 인증이나 계정 복구 proof로 재사용해서는 안 된다.
- 다음 작업: 설치 fingerprint에 더 강한 비밀성이나 key rotation이 필요해지는 제품 요구가 생기면 HMAC 기반 versioned fingerprint로의 전환과 기존 문서 migration을 별도로 설계한다.

## 2026-08-13 — GuestRegistrationTransactionService 원자성 경계 설명

<!-- codex-turn:019ff94f-e782-7b93-80a4-5ec134ed7c37 -->

- 날짜: 2026-08-13
- 브랜치: `feat/TMI-88-social-identity-foundation` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: `GuestRegistrationTransactionService`의 분리 이유, 입력 객체, userId 불변식, Mongo Transaction 저장 순서, rollback·commit 시점과 응답 구성 책임을 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: `GuestAuthService`가 동의·설치 중복을 확인하고 Guest User, RS256 Access Token, 원문과 hash가 결합된 `PreparedRefreshSession`을 영속성 전에 준비한 뒤 별도 Spring bean인 transaction service를 호출함을 확인했다. 별도 bean 호출을 통해 `@Transactional` proxy가 실제로 동작하고 바깥 service가 commit 이후 완료 로그와 duplicate 오류 변환을 수행한다.
- 구현 내용: transaction service는 Guest User의 canonical userId와 RefreshSession의 userId가 일치하는지 먼저 검사해 다른 사용자의 세션을 잘못 연결하는 것을 저장 전에 차단한다. 이후 `users`에 Guest를 저장하고 `refresh_sessions`에 prepared session을 저장하며, `savePrepared` 결과의 Token 원문·발급·만료 시각과 미리 발급된 Access Token으로 `GuestAuthResponse`를 구성한다.
- 구현 내용: named `mongoTransactionManager` 안에서 User 저장, RefreshSession 저장과 응답 구성을 모두 수행한다. User 저장·Session 저장·응답 변환 중 unchecked 예외가 발생하면 두 document가 rollback되고, 메서드가 Spring proxy 밖으로 정상 반환될 때 commit된다. Transaction manager가 없으면 비원자적으로 진행하지 않고 호출 자체가 실패하도록 기존 테스트가 검증한다.
- 구현 내용: Access Token은 DB 저장 대상이 아니고 prepared Refresh Token 원문은 응답에만 사용되며 DB RefreshSession에는 hash만 저장된다. Token 생성 자체는 Transaction 전에 끝나므로 rollback 시 준비된 Token 값은 폐기될 뿐 DB에 남지 않는다. HTTP 응답 전송은 Transaction 밖의 Controller 계층에서 일어나므로 commit 후 네트워크 유실은 이 서비스가 rollback할 수 없다고 구분했다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석·설명 작업이므로 이번 turn에서 Gradle 테스트를 실행하지 않았다. transaction service·호출부·응답 converter·prepared/issued session 코드와 기존 commit, User/Session/응답 실패 rollback, userId 불일치, transaction manager 부재 테스트를 읽어 확인했다. 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 WORKLOG 끝 append를 정적으로 검증한다.
- 유지한 계약: 실제 Guest userId와 JWT `sub`는 서버 생성 canonical UUID이며 User와 RefreshSession은 동일 userId에 결속된다. Refresh Token 원문을 DB에 저장하지 않고 Access/Refresh Token을 로그나 작업 기록에 남기지 않았다. RS256·JWKS, Identity/Learning Core 경계를 유지했으며 Secret·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 이 클래스는 Guest 비즈니스 검증 전체가 아니라 User·최초 RefreshSession 저장의 좁은 Transaction boundary로 설명한다. 응답 DTO 구성은 내부 실패 rollback 범위에 포함되지만 실제 HTTP 전송 성공은 보장하지 않는다. 기존 코드·Jira를 변경하지 않았고 Git commit·push를 수행하지 않았다.
- 위험 요소: Mongo 다중 document Transaction은 replica set 환경이 필요하며 실제 운영/staging에서 검증해야 한다. Spring 기본 rollback은 unchecked exception 기준이므로 향후 checked exception을 추가하면 rollback 정책을 명시해야 하고, 같은 클래스 내부 호출로 바꾸면 proxy가 우회될 수 있다. commit 성공 후 HTTP 응답 유실은 별도 멱등·복구 설계 없이는 해결되지 않는다.
- 다음 작업: Guest 등록 변경 시 User·Session atomic commit, 각 실패 rollback, proxy 호출과 userId binding 테스트를 유지한다. 운영 전 staging replica set에서 실제 Transaction과 commit 오류를 검증하고 응답 유실 복구 필요 여부는 별도 제품 계약으로 결정한다.

## 2026-08-13 — TMI-88 develop 병합 확인 및 Jira 완료 전환

<!-- codex-turn:019ff94b-23b4-7203-9540-2a7c2876f689 -->

- 날짜: 2026-08-13
- Jira: TMI-88
- 브랜치: 현재 checkout `develop`과 로컬 HEAD·로컬 `origin/develop` 참조는 `b6eb73e`이며, GitHub에서 확인한 PR #16의 `develop` merge commit은 `94d0e61a5cd9d4c8b72ce131143756fece95cd39`이다. 이번 작업에서 fetch·commit·push는 수행하지 않았다.
- 작업 목표: SocialIdentity 최소 데이터 기반 구현 PR의 `develop` 병합을 확인하고, 사용자 승인 범위대로 Jira `TMI-88`을 완료 처리한 뒤 저장소 작업 기록을 실제 종료 상태로 갱신한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: GitHub PR #16이 base `develop`에 병합된 상태와 merge commit `94d0e61a5cd9d4c8b72ce131143756fece95cd39`, 2026-08-13 13:03 KST 병합 시각을 확인했다. CURRENT_STATE에서 TMI-88을 진행 중과 commit·push 대기 항목에서 제거하고 완료 상태와 남은 staging index 검증 위험을 반영했다.
- 수행한 Jira 작업: 사용자 승인에 따라 Jira `TMI-88`에 완료 transition ID `41`만 적용하고 재조회했다.
- 추가한 댓글의 목적: 댓글은 추가하지 않았다.
- 변경한 상태: `해야 할 일`에서 `완료`로 전환했고, 후속 조회에서 status ID `10003`과 Resolution ID `10000`의 `완료`, 2026-08-13 13:06 KST 업데이트를 확인했다. 담당자·우선순위와 다른 필드는 변경하지 않았다.
- 승인 여부: 사용자가 PR의 `develop` 병합을 알리고 작업 완료를 명시적으로 요청해 Jira 완료 전환을 승인했다.
- 실행한 테스트와 결과: 이번 완료·기록 turn에서는 애플리케이션 코드가 바뀌지 않아 Gradle 테스트를 재실행하지 않았다. 직전 구현 검증의 전체 44개 suite·308개 테스트가 skip 0·failure 0·error 0으로 성공한 결과를 완료 근거로 사용했다. 종료 전 `git diff --check`, 기록 문서 trailing whitespace와 turn marker 단일 존재를 정적으로 검사한다.
- 유지한 계약: SocialIdentity는 외부 identity에서 canonical UUID userId로의 매핑만 소유하고 JWT `sub`는 실제 userId를 유지한다. Provider subject와 email을 userId로 사용하지 않았고 Identity/Learning Core 경계, RS256·JWKS와 Access/Refresh 계약을 변경하지 않았다. Jira와 기록에 Secret·Token·Password·실제 Key·전체 MongoDB URI·사용자 개인정보를 넣지 않았다.
- 결정사항: TMI-88 구현은 PR #16의 `develop` 병합과 Jira 상태·Resolution `완료` 확인으로 종료한다. Jira 댓글은 등록하지 않았고 다른 필드·이슈 및 Git commit·push는 변경하지 않았다.
- 위험 요소: 격리 Mongo 테스트는 운영 MongoDB 버전·권한·기존 데이터와 무중단 index build를 대체하지 않는다. 운영 writer 활성화 전 staging에서 `social_identities`의 unique·userId index 생성과 기존 중복 데이터 부재를 확인해야 한다. 로컬 Git 참조는 이번 turn에서 원격 fetch하지 않아 GitHub의 merge commit보다 이전 상태다.
- 다음 작업: TMI-88은 종료 상태로 유지한다. 다음 구현은 별도 Jira 범위에서 User accountType·PhoneIdentity 또는 Provider challenge/verifier의 우선순위를 확정한 뒤 시작한다.

## 2026-08-13 — TMI-88 후속 UserAccountType 구현 범위 검토

<!-- codex-turn:019ff951-f7b6-7921-9827-85a9cea36b6c -->

- 날짜: 2026-08-13
- 브랜치: 현재 checkout `develop`, 로컬 HEAD·로컬 `origin/develop` 참조 `b6eb73e`; 이번 분석에서 fetch·commit·push는 수행하지 않았다.
- 작업 목표: SocialIdentity 최소 기반 다음에 구현할 범위를 현재 User 모델과 합의된 소셜 로그인 로드맵에 맞춰 설명하고, 하나의 안전한 후속 PR 경계를 정리한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 현재 `UserProvider.LOCAL/GUEST`가 로그인 수단과 계정 유형을 동시에 나타내고, User 생성·필드 불변식·회원 탈퇴 비밀번호 분기·프로필·로그에 사용되는 위치를 읽어 확인했다. 다음 범위는 `UserAccountType(GUEST, MEMBER)`를 추가해 계정 유형 판단을 분리하는 backward-compatible expand PR로 추천했다.
- 구현 내용: 신규 LOCAL User는 MEMBER, 신규 Guest는 GUEST accountType을 함께 저장하고, 기존 문서는 `provider=GUEST → GUEST`, `provider=LOCAL 또는 null → MEMBER`로 읽는 호환 전략을 제시했다. 다만 null fallback은 운영 데이터의 provider 값과 email·password·guest installation hash 조합을 read-only aggregate로 먼저 확인하는 것을 배포 선행조건으로 둔다.
- 구현 내용: rolling deployment와 기존 클라이언트를 위해 `UserProvider`와 프로필 `provider`를 즉시 삭제하지 않고 deprecated 호환 계약으로 한시 유지하며, 새 도메인 분기에는 `isGuest`, `isMember`, `hasLocalCredential`처럼 의미가 분리된 판단을 사용한다. social-only MEMBER는 User entity가 외부 SocialIdentity 존재를 직접 검증하지 않도록 설계한다.
- 실행한 테스트와 결과: 코드 변경이 없는 범위 분석이므로 Gradle 테스트는 실행하지 않았다. 현재 User entity, UserProvider, 회원 탈퇴 분기, 프로필 응답과 관련 사용처를 정적으로 확인했으며 종료 전 문서 diff와 whitespace·turn marker를 검사한다.
- 유지한 계약: 실제 userId와 JWT `sub`는 canonical UUID 문자열이고 SocialIdentity는 외부 provider identity 매핑만 소유한다. `UserProvider`에 GOOGLE·KAKAO·APPLE을 추가하지 않으며 Identity/Learning Core 경계, RS256·JWKS와 Access/Refresh 계약을 변경하지 않는다. Secret·Token·Password·실제 Key·전체 MongoDB URI·사용자 개인정보를 기록하지 않았다.
- 결정사항: 다음 추천 구현은 UserAccountType 호환 도입까지만이며 Social Provider 검증·challenge·login/signup/link API, PhoneIdentity·OTP, Guest 승격·merge, TrialClaim·Entitlement, outbox는 포함하지 않는다. Jira 생성·수정·댓글·상태 전환은 수행하지 않았다.
- 위험 요소: legacy provider null을 모두 MEMBER로 간주하기 전에 실제 운영 데이터 분포를 확인해야 한다. provider 필드의 즉시 제거와 API 응답 교체는 rolling deployment 및 기존 클라이언트를 깨뜨릴 수 있고, accountType만으로 local credential 존재를 추론하면 향후 social-only MEMBER를 잘못 처리할 수 있다.
- 다음 작업: 사용자가 범위를 확인하면 Jira 초안을 먼저 제시해 승인을 받고, 구현 전 최신 `develop`을 반영한 뒤 UserAccountType expand·legacy fallback·프로필 호환·회원 탈퇴 회귀 테스트를 작성한다.

## 2026-08-13 — TMI-89 UserAccountType 호환 기반 Jira 생성

<!-- codex-turn:019ff959-5a98-7d00-b90c-5e24d934c30e -->

- 날짜: 2026-08-13
- Jira: TMI-89
- 브랜치: 현재 checkout `develop`, 로컬 HEAD·로컬 `origin/develop` 참조 `b6eb73e`; 이번 Jira 작업에서 fetch·commit·push는 수행하지 않았다.
- 작업 목표: 합의한 SocialIdentity 후속 범위를 UserAccountType 분리와 legacy User 호환 expand 작업으로 Jira에 생성하고 저장 결과를 확인한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Atlassian 공식 MCP로 Jira `TMI-88`의 기존 Identity 작업 형식, TMI 프로젝트의 `작업` 유형 ID `10003`, 필수 생성 필드와 동일 제목의 기존 이슈 부재를 읽기 전용으로 확인했다. 사용자 승인 범위대로 `TMI-89` `[Identity] UserAccountType 분리 및 legacy User 호환 기반 구축`을 한 번 생성했다.
- 구현 내용: Jira 설명에는 `UserAccountType(GUEST, MEMBER)`, 신규 User dual write, 기존 provider 기반 legacy read, 의미 기반 Guest·MEMBER·LOCAL credential 분기, 프로필 accountType 추가와 provider deprecated 호환, 완료 조건·테스트·배포 안전 조건을 기록했다. backfill·UserProvider 삭제·소셜 API·PhoneIdentity·Guest 승격·merge·Entitlement와 Token 계약 변경은 제외했다.
- 수행한 Jira 작업: 신규 이슈 생성 후 `TMI-89`을 재조회해 프로젝트 `TMI`, 유형 `작업`, 제목·본문, 상태 `해야 할 일`, Resolution 없음, 담당자 없음, 프로젝트 기본 우선순위 `Medium`, 빈 label·component가 저장됐음을 확인했다.
- 추가한 댓글의 목적: 댓글은 추가하지 않았다.
- 변경한 상태: 생성 기본 상태인 `해야 할 일`을 유지했고 별도 상태 전환은 수행하지 않았다.
- 승인 여부: 사용자가 직전 turn에서 제시한 전체 구현 범위를 확인한 뒤 명시적으로 Jira 생성을 요청했다.
- 실행한 테스트와 결과: 애플리케이션 코드 변경이 없는 Jira 생성·문서 기록 작업이므로 Gradle 테스트는 실행하지 않았다. 종료 전 `git diff --check`, 기록 문서 trailing whitespace와 지정 turn marker 단일 존재를 정적으로 검사한다.
- 유지한 계약: 실제 userId와 JWT `sub`는 canonical UUID 문자열이며 SocialIdentity는 외부 identity 매핑만 소유한다. `UserProvider`에 GOOGLE·KAKAO·APPLE을 추가하지 않고 기존 RS256·JWKS와 Access/Refresh Token, Identity/Learning Core 경계를 변경하지 않았다. Jira와 기록에 Secret·Token·Password·실제 Key·전체 MongoDB URI·사용자 개인정보를 넣지 않았다.
- 결정사항: TMI-89는 backward-compatible expand 단계만 소유한다. Jira 댓글·상태 전환·담당자·우선순위·라벨·컴포넌트 변경과 Git commit·push는 수행하지 않았다.
- 위험 요소: legacy provider null fallback은 운영 User 데이터 분포 확인 후 활성화해야 한다. provider 즉시 제거, API 교체 또는 accountType만으로 credential을 추론하면 rolling deployment와 향후 social-only MEMBER를 깨뜨릴 수 있다.
- 다음 작업: 구현 전에 Jira `TMI-89`을 다시 읽고 최신 `develop`을 반영한다. 운영 legacy User 분포를 read-only로 확인한 뒤 UserAccountType expand·dual write·legacy fallback·프로필 호환·회원 탈퇴 회귀 테스트를 구현하고 `./gradlew clean test`를 실행한다.

## 2026-08-11 — SocialIdentity 모델 단계 구현 계획 수립

<!-- codex-turn:019fef3a-47d0-7af0-9e7b-28ae72bf1a93 -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: Google/Apple 로그인 API 구현 전에 외부 SNS 계정과 canonical User를 분리하는 `SocialIdentity` 모델·MongoDB index·Repository 단계의 저장소 기반 구현 계획을 작성한다.
- 변경 파일: 애플리케이션 코드는 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 현재 `UserProvider(LOCAL, GUEST)`가 User 생성 불변식, 프로필 응답, 탈퇴 자격증명과 로그 분기에 사용됨을 확인했다. 이번 단계에서는 이를 `GOOGLE`·`APPLE`로 확장하거나 전면 분리하지 않고, Auth 도메인의 별도 `SocialProvider(GOOGLE, APPLE)`와 `SocialIdentity` Document를 추가하는 최소 변경안을 확정했다.
- 구현 내용: `SocialIdentity`는 별도 collection에서 UUID 문자열 id, canonical UUID 문자열 `userId`, provider, case-sensitive opaque `providerSubject`, optional email snapshot, 생성·수정 시각을 보유한다. User와는 `@DBRef` 없이 id로만 연결하고 email은 unique 또는 로그인 조회 기준으로 사용하지 않는다.
- 구현 내용: `(provider, providerSubject)` unique compound index를 동일 외부 계정의 다중 User 연결을 막는 최종 경계로 두고, `userId` non-unique index와 `findByProviderAndProviderSubject`, `findAllByUserId` Repository 계약을 추가한다. User 존재 여부와 ACTIVE 여부 확인, Guest 실제 승격·병합은 후속 application service 책임으로 남긴다.
- 구현 내용: 단위 테스트에는 생성·null/blank·UUID 불변식, Google/Apple 동일 subject의 provider별 구분, 한 User의 복수 SNS 연결과 Repository 계약·index metadata 검증을 포함한다. 현재 test profile이 Mongo 자동설정을 제외하므로 실제 duplicate insert 거절까지 증명하려면 외부 Atlas 없이 동작하는 격리 Mongo 테스트 방식을 구현 시 선택해야 한다.
- 실행한 테스트와 결과: 코드 변경이 없는 계획·문서 작업이므로 `./gradlew clean test`는 실행하지 않았다. 저장소 코드와 설정을 정적으로 조사했고 `git diff --check`는 성공했다.
- 유지한 계약: 실제 `userId`는 canonical UUID 문자열로 유지하고 외부 provider subject 및 email과 분리한다. 클라이언트 `userId` 신뢰 금지, JWT `sub`·Access/Refresh Token 흐름 불변, Python AI의 `user_id=examId`, 외부 Provider 호출 금지, Secret·Token·Password·실제 Key·전체 MongoDB URI 비기록 계약을 유지했다.
- 결정사항: 이 단계에서는 Google/Apple Token 검증, 로그인 API, Guest 승격·병합, Learning Core 데이터 이전, Access/Refresh Token 변경, User provider migration을 수행하지 않는다. Mongo unique index가 경쟁 조건을 포함한 최종 중복 방지 장치이며 애플리케이션 선조회만으로 대체하지 않는다. 요구되지 않은 `(userId, provider)` unique 제약은 같은 provider의 복수 계정 연결 정책이 정해질 때까지 추가하지 않는다. Jira 조회·댓글·상태·필드 변경과 Git commit·push는 수행하지 않았다.
- 위험 요소: `SocialIdentity.userId`는 DB foreign key가 아니므로 존재하지 않거나 탈퇴한 User를 참조하지 않게 하는 application service 검증이 후속 연결 단계에 필요하다. 자동 index 생성은 운영 무중단 index 배포를 보장하지 않으며 기존 중복 데이터가 생기기 전에 index를 먼저 생성해야 한다. provider subject의 로그 노출·email 식별 사용을 금지해야 한다. 실제 연결 저장 전에는 회원 탈퇴 시 identity 삭제 또는 tombstone과 동일 SNS 재가입 정책도 확정해야 한다.
- 다음 작업: 합의한 범위대로 `SocialIdentity` entity·enum·Repository·index와 테스트를 구현하고 `./gradlew clean test`를 실행한다. 실제 SNS API와 Guest 승격·merge는 별도 후속 단계에서 설계한다.

## 2026-08-11 — SocialIdentity 계획에 Kakao provider 추가

<!-- codex-turn:019fef41-d637-7bf1-9f5f-22b7db9fe6a2 -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 기존 Google/Apple 중심 `SocialIdentity` 모델 계획에 Kakao 로그인을 동일한 외부 identity 연결 구조로 추가한다.
- 변경 파일: 애플리케이션 코드는 변경하지 않았고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 예정된 `SocialProvider`를 `GOOGLE`, `APPLE`, `KAKAO` 세 값으로 확장하고 한 canonical User가 세 provider identity를 함께 가질 수 있도록 계획과 테스트 범위를 갱신했다. 기존 `UserProvider(LOCAL, GUEST)`에는 SNS 값을 추가하지 않는다.
- 구현 내용: Kakao도 email이 아닌 검증된 provider 사용자 식별자를 문자열 `providerSubject`로 저장하고 `(provider, providerSubject)` unique compound index를 동일하게 적용한다. Google·Apple·Kakao 사이에 같은 문자열 subject가 존재하는 것은 provider namespace가 다르므로 허용한다.
- 구현 내용: 실제 Kakao 인증 단계에서는 OIDC `sub` 또는 사용자 정보 API의 사용자 `id` 중 하나를 canonical subject 입력으로 명시적으로 선택하고 같은 `KAKAO` namespace 안에서 두 표현을 임의로 혼용하지 않도록 후속 계약에 포함한다.
- 실행한 테스트와 결과: 코드 변경이 없는 계획 문서 갱신이므로 `./gradlew clean test`는 실행하지 않았다. `git diff --check`는 성공했다.
- 유지한 계약: 실제 `userId`와 Kakao 외부 식별자를 분리하고 email은 로그인 식별 기준으로 사용하지 않는다. Kakao Token 검증·외부 API 호출, Guest 승격·merge, JWT·Refresh Token 변경은 여전히 이번 단계 범위 밖이며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: Kakao는 Google/Apple과 동일한 `SocialIdentity` collection·Repository·index를 재사용하고 provider별 별도 User 필드나 별도 Kakao identity collection을 만들지 않는다. Jira 조회·댓글·상태·필드 변경과 Git commit·push는 수행하지 않았다.
- 위험 요소: Kakao의 canonical subject 원천을 인증 구현 전에 고정하지 않으면 같은 계정이 중복 identity로 저장될 수 있다. provider subject와 optional email은 로그에 기록하지 않고, 실제 저장 시작 전 회원 탈퇴·재가입 시 연결 삭제 또는 보존 정책을 확정해야 한다.
- 다음 작업: `SocialProvider.KAKAO`를 포함한 entity·Repository·index와 Google/Apple/Kakao 조합 테스트를 구현한 뒤 `./gradlew clean test`를 실행한다. Kakao Token 검증과 로그인 API는 별도 후속 단계로 유지한다.

## 2026-08-11 — Google·Kakao·Apple 소셜 로그인 전체 구현 계획서 작성

<!-- codex-turn:019fef41-d637-7bf1-9f5f-22b7db9fe6a2 -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: SocialIdentity 첫 단계만이 아니라 Google·Kakao·Apple 인증, User 계정 유형 분리, Guest 승격·기존 계정 발견·병합, Learning Core 이전과 운영 rollout까지 전체 구현 순서를 하나의 계획서로 정리한다.
- 변경 파일: 새 `docs/social-login-implementation-plan.md`를 작성하고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 현재 `UserProvider(LOCAL, GUEST)`와 User 불변식으로는 비밀번호 없는 소셜 MEMBER와 Guest 승격을 표현할 수 없음을 확인하고, SocialIdentity 첫 배포 뒤 `UserAccountType(GUEST, MEMBER)`와 로그인 수단을 분리하는 호환 migration 단계를 계획했다.
- 구현 내용: SocialProvider·SocialIdentity·index·Repository, Provider 검증 interface, Google·Kakao·Apple별 subject 규칙, 공개 social login과 JWT 보호 social link API, duplicate key 경쟁 처리, 기존 identity owner 중심 canonical merge와 source Session 폐기 흐름을 단계별로 정의했다.
- 구현 내용: Identity가 시험·결과를 직접 수정하지 않고 `UserMerged` transactional outbox로 Learning Core에 이전을 요청하도록 경계를 유지했으며, provider subject·email·자격증명을 event와 로그에 넣지 않는 계약을 포함했다.
- 구현 내용: 모델·Provider verifier·application service·Transaction·동시성·Security·OpenAPI 테스트와 index-first 배포, accountType backfill, provider별 feature flag 활성화, rollback 및 전체 완료 조건을 정의했다. 첫 구현 PR은 SocialIdentity 모델·Repository·index와 테스트만 포함한다.
- 실행한 테스트와 결과: 코드 변경이 없는 계획 문서 작업이므로 `./gradlew clean test`는 실행하지 않았다. `git diff --check`는 성공했고 계획서의 Markdown code fence 개수와 필수 section을 정적으로 확인했다.
- 유지한 계약: 실제 userId는 UUID 문자열이며 JWT `sub`는 canonical userId다. 외부 Request Body의 userId를 신뢰하지 않고 email을 로그인 식별자로 사용하지 않으며, Access/Refresh Token·RS256·JWKS·Python AI `user_id=examId`와 Identity/Learning Core 도메인 경계를 유지했다. Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 미연결 social login은 User를 자동 생성하지 않고 인증된 Guest link 흐름으로 보낸다. 기존 SocialIdentity owner가 canonical User이며 source User에는 새 Token을 발급하지 않는다. Provider별 기능은 Google, Kakao, Apple 순서로 독립 활성화하고 Git commit·push와 Jira 조회·댓글·상태·필드 변경은 수행하지 않았다.
- 위험 요소: Kakao canonical subject, 직접 소셜 가입, 같은 provider 복수 연결, 계정 병합의 eventual consistency, 소셜 전용 회원 탈퇴 재인증과 탈퇴 후 재가입, outbox 전달 방식은 구현 전 제품·보안 결정이 필요하다. 현재 test profile은 Mongo를 제외하므로 실제 unique index·Transaction 검증에는 격리 Mongo와 staging replica set 검증이 필요하다.
- 다음 작업: 계획서 단계 0의 제품·보안 결정을 확정한 뒤 첫 구현 범위인 `SocialProvider(GOOGLE, KAKAO, APPLE)`, `SocialIdentity`, Mongo index, Repository와 외부 인프라 없는 테스트를 구현하고 `./gradlew clean test`를 실행한다.

## 2026-08-11 — 소셜 로그인 전체 계획서 작업 기록 동기화

<!-- codex-turn:019fef43-c151-7f42-ba5e-644ffbcc98f2 -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 현재 turn에서 작성한 Google·Kakao·Apple 소셜 로그인 전체 구현 계획서의 작업 기록을 지정된 turn marker로 동기화한다.
- 변경 파일: `docs/social-login-implementation-plan.md`의 작성 결과를 기준으로 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: SocialIdentity 모델부터 User 계정 유형 분리, Provider 검증, Guest 승격·canonical 병합, Learning Core outbox, lifecycle, 테스트와 provider별 rollout까지 0~8단계로 정리된 계획서가 현재 작업 기준임을 기록했다.
- 실행한 테스트와 결과: 코드 변경이 없는 계획·작업 기록 동기화이므로 `./gradlew clean test`는 실행하지 않았다. `git diff --check`는 성공했고 지정된 turn marker가 한 번 포함됐음을 확인했다.
- 유지한 계약: UUID canonical userId와 JWT `sub`, email 비식별 원칙, RS256·JWKS·RefreshSession, Identity/Learning Core 경계와 Python AI `user_id=examId` 계약을 유지했다. Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 첫 구현 범위는 `SocialProvider(GOOGLE, KAKAO, APPLE)`, `SocialIdentity`, Mongo index, Repository와 격리 테스트이며 실제 Provider API·Guest 승격·병합은 후속 단계로 유지한다. Jira 변경과 Git commit·push는 수행하지 않았다.
- 위험 요소: Kakao subject 원천, 직접 소셜 가입, 같은 provider 복수 연결, 탈퇴·재가입, merge eventual consistency와 outbox 전달 방식은 구현 전에 확정해야 한다.
- 다음 작업: 계획서 단계 0의 결정을 확정한 뒤 SocialIdentity 첫 구현 단계로 진행하고 전체 `./gradlew clean test`를 실행한다.

## 2026-08-11 — 전화번호 검증·무료체험을 포함한 전체 계획 보완

<!-- codex-turn:019fef52-bf25-7f42-adde-a5d79159357c -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 기존 Google·Kakao·Apple 소셜 로그인 계획에 PhoneIdentity·SMS OTP·검증 번호당 무료 모의고사 1회 정책을 추가하고, SocialIdentity 최소화·Provider 입력·Apple revoke·merge·outbox·Access Token·관측성 보완 의견을 전체 구현 계획에 반영한다.
- 변경 파일: `docs/social-login-implementation-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 첫 SocialIdentity를 `socialIdentityId`, `userId`, `provider`, `providerSubject`, `createdAt`으로 제한하고 email과 의미 없는 `updatedAt`을 제거했다. Provider email은 공통 검증 결과에서 필요한 순간에만 사용하고 저장하지 않도록 개인정보 최소화 원칙을 확정했다.
- 구현 내용: `SocialTokenVerifier` 입력을 provider별 `SocialVerificationRequest` subtype으로 유연화하고, Kakao OIDC `sub` 권장, Apple authorization code 교환·credential 보관·탈퇴 revoke lifecycle을 단계 0과 Apple 구현 단계에 추가했다.
- 구현 내용: 자동 merge를 ACTIVE GUEST source와 ACTIVE MEMBER target에만 허용하고 MEMBER 간 자동 merge를 금지했으며, canonical chain·순환 방지와 source Access Token 정책을 명시했다. `UserMerged` outbox에는 전달 상태·attempt·nextAttemptAt·lease를 추가하고 atomic claim·at-least-once·Learning Core eventId 멱등 계약을 정의했다.
- 구현 내용: Identity가 E.164 정규화, versioned domain-separated HMAC-SHA-256, `PhoneIdentity`, `PhoneVerificationAttempt`, 보호된 OTP API와 phone·user·IP abuse 방어를 소유하도록 정리했다. 전화번호는 로그인·자동 merge 키가 아니며 raw 번호·OTP·fingerprint를 장기 저장하거나 로그에 남기지 않는다.
- 구현 내용: `TrialClaim`, `UserEntitlement`, 무료시험 grant·consume과 결제는 별도 Entitlement/Billing·Learning Core 경계가 소유하도록 분리했다. Identity에는 `freeTrialUsed`나 시험 코드를 추가하지 않고 benefit-scoped fingerprint 또는 일회성 proof만 서버 간에 전달하며, claim unique·consume idempotency·exam 생성 실패 보상을 계획했다.
- 구현 내용: 관측 sink를 application logs·Sentry·metrics로 분리해 internal userId는 필요한 application log에서만 허용하고 Sentry와 metrics에는 금지했다. 전체 순서를 SocialIdentity → accountType → PhoneIdentity/OTP → TrialClaim/Entitlement → 무료시험 consume → Google → Guest 승격·merge → Kakao → Apple → 결제·lifecycle의 0~12단계로 재정렬했다.
- 실행한 테스트와 결과: 코드 변경이 없는 계획 문서 작업이므로 `./gradlew clean test`는 실행하지 않았다. `git diff --check`는 성공했고 trailing whitespace가 없으며 Markdown heading 20개 구간·code fence 70개 짝수와 지정 turn marker 1회를 확인했다. 주요 금지·소유권 계약도 정적 검색으로 확인했다.
- 유지한 계약: 실제 userId는 UUID 문자열이고 JWT `sub`는 canonical userId다. 외부 Request Body의 userId를 신뢰하지 않고 email·전화번호를 로그인 식별자로 사용하지 않으며, RS256·JWKS·RefreshSession과 Python AI `user_id=examId` 계약을 유지했다. Identity는 시험·결과·무료 사용권·결제를 소유하지 않고 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 첫 구현 PR 범위는 email 없는 최소 `SocialIdentity`, `SocialProvider`, 두 Mongo index, Repository와 격리 index 테스트로 유지한다. 전화번호는 검증 번호당 무료체험 중복 방지 입력일 뿐 계정 merge 기준이 아니며 `TrialClaim`은 Identity 저장소에 만들지 않는다. Git commit·push와 Jira 조회·댓글·상태·필드 변경은 수행하지 않았다.
- 위험 요소: HMAC key rotation의 mixed writer, SMS 비용 abuse, 번호 재할당, pseudonymous TrialClaim 보존, Apple revoke 실패, source Access Token, outbox 중복 전달, entitlement consume과 exam 생성의 분산 일관성은 구현 전 정책·법무·운영 계약이 없으면 각각 중복 지급·권리 유실·개인정보 보존·권한 오판 위험을 만든다.
- 다음 작업: 계획서 단계 0의 Kakao·Apple·merge·전화번호·HMAC·OTP·TrialClaim 보존·Identity–Entitlement·consume 보상 결정을 확정한 뒤 첫 SocialIdentity PR을 구현하고 `./gradlew clean test`를 실행한다.

## 2026-08-11 — 전화번호 인증 시점을 MEMBER enrollment로 조정

<!-- codex-turn:019fef5e-65f0-7772-a867-5b96eb9d4ff4 -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 무료 모의고사 시작 직전이던 전화번호 인증 UX를 검토하고, 회원가입·소셜 MEMBER 승격 시 1회 인증하는 전체 정책으로 계획서를 다듬는다.
- 변경 파일: `docs/social-login-implementation-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Guest 생성·둘러보기에는 전화 인증을 요구하지 않되 LOCAL 가입은 가입 전 OTP로 받은 일회성 `phoneVerificationGrant`를 소비해 User와 PhoneIdentity를 같은 Mongo Transaction으로 생성하도록 정리했다. Guest의 Google·Kakao·Apple MEMBER 승격은 기존 userId를 유지하면서 PhoneIdentity 보유를 선행 조건으로 둔다.
- 구현 내용: 기존 MEMBER 로그인에는 OTP를 반복하지 않고, PhoneIdentity가 없는 legacy MEMBER만 무료시험 또는 향후 전화번호 필수 기능 전에 1회 onboarding하도록 정리했다. 전화 인증 attempt를 가입 시도 또는 JWT `sub`에 묶고 verification TTL·grant TTL·cleanup TTL을 분리했으며 grant 위조·만료·재사용과 `PHONE_VERIFICATION_REQUIRED` 오류 계약을 추가했다.
- 구현 내용: 전화 인증이나 회원가입 성공만으로 `TrialClaim`을 만들지 않고, 첫 무료 모의고사 요청에서 기존 PhoneIdentity를 이용해 별도 Entitlement/Billing이 silent claim·grant하도록 유지했다. 시험 시작 시에는 OTP 화면 없이 entitlement를 consume하며 Identity는 시험·사용권 코드를 소유하지 않는다.
- 실행한 테스트와 결과: 코드 변경이 없는 계획 문서 작업이므로 `./gradlew clean test`는 실행하지 않았다. `git diff --check`가 성공했고 세 문서에 trailing whitespace가 없음을 확인했다. 계획서의 H2 section 20개와 code fence 74개가 정상적으로 짝을 이루며, 지정 turn marker가 정확히 1회이고 폐기한 `PhoneVerifiedForBenefit`·`PhoneVerificationAttempt.userId` 모델 참조가 없음을 정적 검색으로 확인했다.
- 유지한 계약: 실제 userId는 UUID 문자열이고 JWT `sub`는 canonical userId다. 전화번호는 로그인 식별자나 자동 merge 키가 아니며 외부 Request Body의 userId를 신뢰하지 않는다. RS256·JWKS·RefreshSession, Identity/Learning Core 경계와 Python AI `user_id=examId` 계약을 유지했고 Secret·Token·Password·실제 Key·전체 MongoDB URI와 raw 전화번호·OTP를 기록하지 않았다.
- 결정사항: 전화번호가 계정 복구·결제 보호에도 사용된다는 제품 방향을 전제로 신규 MEMBER enrollment 인증을 기본안으로 채택한다. Guest 접근은 유지하고 기존 MEMBER의 매 로그인 재인증은 금지한다. 전화 인증 성공과 무료체험 claim·실제 사용은 각각 분리한다. 첫 구현 PR은 여전히 email 없는 최소 SocialIdentity 모델·index·Repository와 테스트만 포함한다. Jira 변경과 Git commit·push는 수행하지 않았다.
- 위험 요소: 회원가입 단계 OTP는 전환율 저하, 가입하지 않는 사용자의 SMS 비용과 개인정보 수집을 늘린다. 전화번호가 무료체험 중복 방지에만 쓰인다면 시험 직전 인증이 더 적합하며, 번호 재할당·공유와 HMAC rotation·TrialClaim 보존은 여전히 오탐과 개인정보 보존 위험이 있다.
- 다음 작업: 단계 0에서 MEMBER enrollment 정책과 legacy onboarding 지점, OTP·국가·line type·보존 정책을 제품·보안·법무 기준으로 확정한 뒤 첫 SocialIdentity PR을 구현하고 `./gradlew clean test`를 실행한다.

## 2026-08-11 — 무료 모의고사 회원가입·가입 시 전화번호 인증 정책 확정

<!-- codex-turn:019fef68-8951-7b01-9b4f-20b56262e7b6 -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 무료 모의고사는 반드시 회원가입한 사용자만 시작하게 하고, 전화번호 검증을 시험 시작이 아닌 LOCAL·소셜 회원가입 과정에서 완료하도록 전체 구현 계획을 확정한다.
- 변경 파일: `docs/social-login-implementation-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 무료 모의고사 접근 조건을 `ACTIVE MEMBER + PhoneIdentity`로 고정하고 Guest 요청은 `MEMBERSHIP_REQUIRED`로 차단하도록 아키텍처 흐름, 오류 계약, 단계별 구현, 테스트, 배포와 완료 조건에 반영했다. Guest는 전화 인증 없이 둘러볼 수 있지만 claim·entitlement·consume은 생성하거나 호출하지 않는다.
- 구현 내용: LOCAL 가입은 pre-signup `phoneVerificationGrant` 소비와 User·PhoneIdentity 생성을 한 Mongo Transaction으로 처리한다. 미연결 Google·Kakao·Apple identity를 이용한 Guest 소셜 가입은 Guest-bound grant 소비, 기존 userId의 MEMBER 승격, PhoneIdentity·SocialIdentity 생성을 한 Transaction으로 처리하도록 정리했다.
- 구현 내용: Guest MEMBER_ENROLLMENT OTP confirm은 grant만 발급하고 PhoneIdentity를 미리 만들지 않는다. 가입 중단 Guest의 번호 선점을 피하고 최종 PhoneIdentity unique index가 동시 가입의 최종 경계가 된다. 이미 연결된 social identity 로그인·Guest merge와 기존 MEMBER 로그인은 신규 회원가입이 아니므로 OTP를 반복하지 않는다.
- 구현 내용: 회원가입과 전화 인증만으로 TrialClaim을 생성하지 않고 첫 무료 모의고사 요청에서 기존 PhoneIdentity로 silent claim·grant한 뒤 별도 consume한다. PhoneIdentity 없는 legacy MEMBER는 로그인 후 별도 1회 onboarding 대상으로 남긴다.
- 실행한 테스트와 결과: 코드 변경이 없는 계획 문서 작업이므로 `./gradlew clean test`는 실행하지 않았다. `git diff --check`와 세 문서의 trailing whitespace 검사가 성공했고, 계획서 H2 section 20개·code fence 74개의 정상 구조와 회원가입·Guest 차단 정책의 오류·테스트·완료 조건 반영을 정적 검색으로 확인했다.
- 유지한 계약: 실제 userId는 UUID 문자열이고 JWT `sub`는 canonical userId다. 외부 Request Body의 userId를 신뢰하지 않고 전화번호를 로그인 식별자나 자동 merge 키로 사용하지 않는다. RS256·JWKS·RefreshSession, Identity/Learning Core 경계와 Python AI `user_id=examId` 계약을 유지했으며 민감한 인증 정보나 인프라 비밀값을 기록하지 않았다.
- 결정사항: 무료 모의고사는 ACTIVE MEMBER만 시작할 수 있고 신규 LOCAL 또는 미연결 identity의 소셜 MEMBER 회원가입에는 전화 인증 grant가 필수다. 기존 social identity 로그인·merge에는 가입 OTP를 다시 요구하지 않는다. 가입 성공과 무료체험 claim·실제 사용은 계속 분리하며 첫 구현 PR 범위는 최소 SocialIdentity 모델·index·Repository와 테스트로 유지한다. Jira 변경과 Git commit·push는 수행하지 않았다.
- 위험 요소: 가입 필수 OTP는 가입 전환율을 낮추고 무료시험을 사용하지 않는 회원에게도 SMS 비용과 개인정보 수집을 발생시킨다. 번호 재할당·공유, 동시 가입, grant 탈취·재사용, HMAC rotation과 TrialClaim 보존에 대한 보안·법무·운영 정책이 필요하다.
- 다음 작업: legacy MEMBER onboarding 강제 시점과 OTP 국가·line type·rate limit·보존 정책을 확정하고, 첫 SocialIdentity PR 이후 PhoneIdentity·회원가입 Transaction을 단계적으로 구현해 `./gradlew clean test`와 격리 Mongo·staging Transaction 검증을 수행한다.

## 2026-08-11 — Guest 없는 첫 SNS 로그인·직접 회원가입 흐름 추가

<!-- codex-turn:019fef6e-c8cd-7ee1-814e-de984ee6e6ba -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: Guest에서 SNS로 승격하는 경로뿐 아니라 앱 첫 진입에서 Guest를 거치지 않고 Google·Kakao·Apple 버튼으로 기존 계정에 로그인하거나 신규 소셜 회원가입하는 흐름을 전체 구현 계획에 추가한다.
- 변경 파일: `docs/social-login-implementation-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 공개 social login이 Provider credential 검증 후 기존 SocialIdentity를 찾으면 Guest 생성 없이 canonical MEMBER Token을 발급하고, 미연결 identity면 User를 생성하지 않은 채 `SIGNUP_REQUIRED`와 짧은 `socialEnrollmentGrant`를 반환하도록 두 결과를 분리했다.
- 구현 내용: `SocialEnrollmentAttempt`를 DIRECT_SIGNUP 또는 현재 User에 binding하고 Provider credential·전체 Claim·email·authorization code 원문을 저장하지 않도록 계획했다. opaque grant 원문은 클라이언트에 한 번만 전달하고 DB에는 hash만 저장하며 providerSubject는 최종 SocialIdentity 생성을 위해 짧은 TTL 동안만 보유한다.
- 구현 내용: 직접 소셜 가입은 socialEnrollmentGrant에 묶인 공개 전화 인증을 거쳐 같은 signupAttemptId의 phoneVerificationGrant와 필수 동의를 받는다. 최종 Transaction에서 서버 UUID MEMBER User·PhoneIdentity·SocialIdentity·최초 RefreshSession 생성과 두 attempt 소비를 원자적으로 처리하고 canonical userId 기준 Access/Refresh 응답을 반환한다.
- 구현 내용: Guest 전환은 보호된 prepare/finalize API와 USER-bound 두 grant를 사용해 기존 Guest userId를 유지한다. 이미 가입된 SNS identity의 첫 진입 로그인이나 Guest merge는 신규 가입이 아니므로 전화 OTP를 반복하지 않는다. 무료 모의고사의 ACTIVE MEMBER 제한과 TrialClaim lazy claim 정책은 유지했다.
- 실행한 테스트와 결과: 코드 변경이 없는 계획 문서 작업이므로 `./gradlew clean test`는 실행하지 않았다. `git diff --check`와 세 문서의 trailing whitespace 검사가 성공했고, 계획서 H2 section 20개·code fence 80개의 정상 구조를 확인했다. 직접 SNS 진입·SIGNUP_REQUIRED·grant binding·Transaction·오류·테스트·배포·완료 조건이 포함되고 과거의 직접 가입 제외 문구가 없음을 정적 검색으로 검증했다.
- 유지한 계약: 실제 userId는 서버가 생성한 UUID 문자열이고 JWT `sub`는 canonical userId다. 외부 Request Body의 userId를 받지 않고 Provider email·전화번호를 로그인 식별자나 자동 merge 키로 사용하지 않는다. RS256·JWKS·RefreshSession, Identity/Learning Core 경계와 Python AI `user_id=examId` 계약을 유지했으며 민감한 인증 정보나 인프라 비밀값을 기록하지 않았다.
- 결정사항: 첫 SNS 버튼은 기존 identity의 즉시 로그인과 미연결 identity의 DIRECT signup을 모두 지원한다. Provider 검증 성공만으로 User를 만들지 않고 전화 인증·필수 동의까지 완료해야 가입한다. DIRECT signup과 Guest 승격은 별도 binding을 사용하며 첫 구현 PR은 여전히 최소 SocialIdentity 모델·index·Repository와 테스트로 제한한다. Jira 변경과 Git commit·push는 수행하지 않았다.
- 위험 요소: SocialEnrollmentAttempt와 두 bearer grant의 탈취·재사용·binding 혼합, 중단 가입 데이터 보존, 같은 SNS 또는 전화번호의 동시 가입, social 전용 User의 email nullable·필수 profile 정책이 남아 있다. 짧은 TTL·hash 저장·일회성 소비·unique index·전체 Transaction rollback과 API rate limit이 필요하다.
- 다음 작업: 단계 0에서 social 전용 profile·email nullable, enrollment TTL·정리 정책을 확정한 뒤 최소 SocialIdentity PR을 구현한다. 이후 Provider verifier와 SocialEnrollmentAttempt, DIRECT signup·PhoneIdentity Transaction을 순서대로 구현하고 `./gradlew clean test`, 격리 Mongo index와 staging replica set Transaction을 검증한다.

## 2026-08-11 — SNS nonce·socialChallengeId 계약 명확화

<!-- codex-turn:019fef82-8aa5-73f2-97ae-2cd1ddce6138 -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 소셜 API 계획의 `합의된 nonce 또는 challenge 식별자`라는 모호한 표현을 제거하고 nonce의 보안 목적과 서버 발급 socialChallengeId 흐름을 구체적으로 정의한다.
- 변경 파일: `docs/social-login-implementation-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: nonce는 Identity가 SNS 인증 시작 전에 생성하고 Provider SDK에 전달해 ID Token nonce Claim과 비교하는 일회성 값으로 정의했다. socialChallengeId는 nonce나 PKCE code_challenge가 아니라 서버 보관 expected nonce·Provider·purpose·User binding을 조회하는 opaque 인증 시도 식별자로 구분했다.
- 구현 내용: `SocialLoginChallenge`에 provider, LOGIN_OR_SIGNUP/LINK purpose, LINK의 JWT `sub` binding, expectedNonceHash, provider별 nonceMode, PENDING/CONSUMED/EXPIRED 상태와 TTL을 두도록 계획했다. nonce 원문·socialChallengeId는 로그·Sentry·metric에 남기지 않고 application이 만료와 일회성 소비를 직접 검사한다.
- 구현 내용: 공개 login/signup challenge API와 Bearer 보호 link challenge API가 socialChallengeId·providerNonce·만료 정보를 발급하고, 클라이언트는 providerNonce를 SDK에 전달한 뒤 provider credential과 socialChallengeId만 Identity에 반환한다. login/link Body의 raw nonce는 기대값으로 신뢰하지 않는다.
- 구현 내용: verifier는 서버 challenge에서 얻은 expected nonce context를 사용하고 Provider·purpose·User binding 불일치, 만료·재사용을 거절한다. 브라우저 authorization-code 흐름에서는 nonce와 별도로 OAuth state와 PKCE가 필요하며 세 수단은 서로 대체하지 않는다고 명시했다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석·계획 문서 작업이므로 `./gradlew clean test`는 실행하지 않았다. `git diff --check`와 세 문서의 trailing whitespace 검사가 성공했고, 계획서 H2 section 20개·code fence 84개의 정상 구조를 확인했다. 모호한 기존 문구가 제거되고 challenge 모델·API·오류·테스트·배포·완료 조건이 포함됐음을 정적 검색으로 검증했다.
- 유지한 계약: 실제 userId는 서버 생성 UUID 문자열이고 JWT `sub`는 canonical userId다. 외부 Request Body의 userId를 받지 않고 Provider subject·email·전화번호를 로그인 식별자나 자동 merge 키로 사용하지 않는다. RS256·JWKS·RefreshSession, Identity/Learning Core 경계와 Python AI `user_id=examId` 계약을 유지했으며 민감한 인증 정보나 인프라 비밀값을 기록하지 않았다.
- 결정사항: 클라이언트가 보낸 nonce를 신뢰하지 않고 서버 발급 socialChallengeId로 expected nonce를 조회한다. LOGIN_OR_SIGNUP challenge는 공개·rate-limited, LINK challenge는 JWT `sub` binding으로 보호하고 Provider 검증 성공 시 한 번만 소비한다. 이 모델은 SocialEnrollmentAttempt와 별개이며 첫 SocialIdentity PR 범위에는 포함하지 않는다. Jira 변경과 Git commit·push는 수행하지 않았다.
- 위험 요소: Provider SDK마다 raw 또는 변환 nonce 전달 방식이 다를 수 있어 nonceMode를 잘못 구현하면 정상 로그인을 차단하거나 검증을 우회할 수 있다. challenge TTL·재사용 차단·Provider/purpose/User binding이 빠지면 replay와 흐름 혼합 위험이 있으며 browser redirect에서 state·PKCE를 생략하면 nonce만으로 해당 공격을 막을 수 없다.
- 다음 작업: Provider별 SDK 문서에 맞춰 Google·Kakao·Apple nonceMode와 browser/mobile flow를 확정한 뒤 SocialLoginChallenge의 domain·Repository·API·verifier 테스트를 Provider 구현 단계에서 추가한다. 그 전에는 첫 범위인 최소 SocialIdentity 모델·index·Repository를 구현한다.

## 2026-08-11 — TrialClaim·UserEntitlement 역할 구분 설명

<!-- codex-turn:019fef94-f875-7da1-beda-d6aa6ba57501 -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 무료 모의고사 설계의 `TrialClaim`과 `UserEntitlement`가 각각 무엇을 기록하고 왜 두 모델로 분리되는지 명확하게 설명한다.
- 변경 파일: `docs/social-login-implementation-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: TrialClaim을 `benefitType + benefit-scoped phoneFingerprint` 기준의 전역 무료혜택 지급 이력으로 정의했다. 같은 검증 번호가 탈퇴·재가입·다른 User 생성 후 무료권을 다시 받지 못하게 하며 시험 사용 여부에 따라 변경하거나 삭제하지 않는다.
- 구현 내용: UserEntitlement를 canonical userId에 귀속된 실제 이용 권리로 정의했다. 무료 모의고사 grant 시 수량 1로 생성되고 reserve·consume·보상에 따라 상태·수량이 변하며 Learning Core가 시험 시작 전에 확인·차감하는 대상이다.
- 구현 내용: 최초 무료시험 요청에서 TrialClaim insert와 UserEntitlement grant를 Entitlement/Billing의 한 Transaction으로 처리하고, 시험 consume 시에는 entitlement만 변경하며 TrialClaim은 유지하는 상태 예시를 추가했다. 탈퇴 후 동일 번호의 user_B에는 기존 TrialClaim 때문에 무료 entitlement를 재지급하지 않는다.
- 구현 내용: TrialClaim만으로는 사용권의 미사용·예약·사용·보상 상태를 관리할 수 없고 UserEntitlement만으로는 새 User를 이용한 무료혜택 재수령을 막을 수 없음을 설명했다. 유료 구매 entitlement는 TrialClaim 없이 별도 결제 grant source를 가질 수 있도록 역할을 분리했다.
- 실행한 테스트와 결과: 코드 변경이 없는 설명·계획 문서 작업이므로 `./gradlew clean test`는 실행하지 않았다. `git diff --check`와 세 문서의 trailing whitespace 검사가 성공했고, 계획서 H2 section 20개·code fence 86개의 정상 구조와 역할표·상태 흐름·유료 확장 설명 포함을 정적 검색으로 확인했다.
- 유지한 계약: Identity는 PhoneIdentity와 검증 proof만 소유하고 TrialClaim·UserEntitlement·시험 consume을 소유하지 않는다. 실제 userId와 JWT `sub`의 canonical UUID, 외부 Request Body의 userId 비신뢰, 전화번호 비로그인·비merge 키, Identity/Learning Core 경계와 Python AI `user_id=examId` 계약을 유지했으며 민감한 인증 정보나 인프라 비밀값을 기록하지 않았다.
- 결정사항: TrialClaim은 혜택 수령 이력, UserEntitlement는 사용 가능한 권리이므로 하나의 `freeTrialUsed` boolean이나 단일 document로 합치지 않는다. 두 문서는 최초 무료 claim에서 원자적으로 생성하고 실제 시험 권한 판단과 consume은 UserEntitlement만 대상으로 한다. 첫 SocialIdentity PR 범위에는 둘 다 포함하지 않으며 Jira 변경과 Git commit·push는 수행하지 않았다.
- 위험 요소: TrialClaim 보존은 pseudonymous fingerprint의 개인정보 보존과 번호 재할당 오탐을 만들 수 있다. entitlement consume과 exam 생성의 서비스 간 실패에 예약·확정 또는 보상이 없으면 권리가 유실될 수 있고, claim과 grant를 비원자적으로 저장하면 혜택만 소진되거나 중복 지급될 수 있다.
- 다음 작업: 별도 Entitlement/Billing 경계에서 TrialClaim unique index, UserEntitlement 상태·수량·grant source, claim+grant Transaction과 consume idempotency·보상 계약을 확정한다. Identity 저장소의 첫 구현은 계획대로 최소 SocialIdentity 모델·index·Repository부터 진행한다.

## 2026-08-11 — 소셜 로그인 전체 계획 동시성·운영 계약 보강

<!-- codex-turn:019fefb2-a893-7d33-b0b9-3b5d326a643c -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 소셜 로그인·전화번호 인증·무료체험 전체 계획을 추가 설계 검토와 대조해 단계 책임 중복, merge source Token, HMAC key rotation, 무료시험 분산 상태와 동시성 계약을 구현 가능한 수준으로 보강한다.
- 변경 파일: `docs/social-login-implementation-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 첫 PR의 `SocialIdentity` 범위는 유지하면서 `providerSubject`를 case-sensitive opaque 1~255자로 제한했다. 단계 6은 challenge·verifier framework, 단계 7은 기존 로그인·직접 가입·미연결 link, 단계 8은 실제 Guest merge·outbox로 분리하고, 단계 7에서 다른 owner를 발견하면 mutation과 target Token 발급 없이 `MERGE_REQUIRED`를 반환하도록 정리했다.
- 구현 내용: source JWT를 target actor로 승격하거나 authorization alias로 사용하지 않는 공통 정책을 확정했다. Identity의 `ACCOUNT_MERGED_TOKEN_REJECTED`, downstream ownership migration·source deny marker의 로컬 Transaction, source write fencing과 전체 참여 서비스 검증 전 merge feature flag 금지를 계획에 반영했다.
- 구현 내용: SocialLoginChallenge의 조건부 `PENDING → CONSUMED` CAS 승자만 후속 처리를 진행하고 SocialEnrollmentAttempt·PhoneVerificationAttempt도 최종 Transaction 안에서 조건부 소비하도록 명시했다. 가입 proof 누락과 legacy onboarding은 각각 `PHONE_VERIFICATION_GRANT_REQUIRED` 400, `PHONE_ONBOARDING_REQUIRED` 409로 분리했다.
- 구현 내용: raw 번호를 장기 보관하지 않는 HMAC rotation을 위해 `PhoneFingerprintAlias`, `ACTIVE_WRITE → LOOKUP_ONLY → RETIRED` key lifecycle, retained phone·benefit version candidate 조회, legacy key reference gate와 mixed writer 금지를 추가했다. 운영 provider null 문서는 MEMBER fallback 전에 read-only aggregate로 검증하도록 했다.
- 구현 내용: 무료시험 분산 흐름을 `reserve → exam 생성 → confirm`으로 고정하고 확정 실패 cancel, 결과 불명 timeout의 `RECONCILIATION_REQUIRED`, reservationId 기반 exam 존재 대조, CAS·idempotency 불변식을 추가했다. Identity/Social `1→2→3→6→7→8→9→10`과 Entitlement/Billing `4→5→11`을 독립 트랙으로 표현했다.
- 실행한 테스트와 결과: 문서만 변경했으므로 `./gradlew clean test`는 실행하지 않았다. `git diff --check`가 성공했고 세 문서의 trailing whitespace가 없었다. 계획서의 H2 section 20개와 code fence 90개가 정상적으로 짝을 이루며 단계 7의 `MERGE_REQUIRED`와 단계 8의 실제 merge가 분리됐음을 정적 검색으로 확인했다.
- 유지한 계약: 실제 userId는 UUID 문자열이고 JWT `sub`는 canonical userId다. source JWT를 target 권한으로 승격하지 않고 외부 Body의 userId를 신뢰하지 않는다. 전화번호는 로그인·자동 merge 키가 아니며 Identity는 시험·TrialClaim·UserEntitlement·EntitlementReservation을 소유하지 않는다. RS256·JWKS, Identity/Learning Core 경계와 Python AI `user_id=examId`를 유지했고 Secret·Token·Password·raw 전화번호·OTP·fingerprint·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 제시된 P0/P1 검토 의견은 타당해 계획에 반영했다. 첫 PR은 최소 SocialIdentity 모델·index·Repository·테스트로 유지하며 추가 범위는 subject 최대 길이 계약뿐이다. key version이 달라져도 retained alias candidate로 번호·TrialClaim 중복을 찾고, 결과가 불명확한 exam 생성은 확인 없이 entitlement reservation을 해제하지 않는다. Jira 작업과 Git commit·push는 수행하지 않았다.
- 위험 요소: alias/history와 key registry reference count가 구현·운영되지 않으면 version 교차 중복이 가능하고, legacy key 장기 보존은 Secret 관리 부담을 만든다. source JWT 거절은 모든 user-owned 서비스가 deny marker와 write fencing을 구현해야 하며 한 서비스라도 준비되지 않으면 merge를 열 수 없다. reservation reconciliation의 양쪽 상태 조회가 어긋나면 무료 권리 유실 또는 중복 시험이 생길 수 있다.
- 다음 작업: 첫 SocialIdentity PR을 구현하기 전에 Provider별 subject 공식 상한과 격리 Mongo index 검증 방식을 확인한다. 이후 단계 0의 남은 제품·보안·법무 결정을 확정하고 두 트랙을 독립적으로 구현하며 각 코드 변경에서 `./gradlew clean test`와 staging Transaction·동시성 검증을 수행한다.

## 2026-08-11 — 전체 인증·무료체험 계획 사용자 확인용 요약

<!-- codex-turn:019fefe6-d084-7a21-b1e8-005cfc58dc3c -->

- 날짜: 2026-08-11
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 사용자가 합의된 소셜 로그인·회원가입 전화 인증·Guest 병합·무료 모의고사 설계를 자신의 이해와 비교할 수 있도록 구현 세부사항을 핵심 역할과 사용자 흐름 중심으로 요약한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드와 `docs/social-login-implementation-plan.md`의 설계 내용은 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: User는 영구 canonical 계정이고 SocialIdentity는 Google·Kakao·Apple 외부 계정 연결이라는 기본 분리를 요약했다. 앱 첫 진입 SNS는 기존 identity 즉시 로그인과 미연결 identity의 전화 인증·동의 후 DIRECT signup으로 나뉘며, Guest의 미연결 SNS 연결은 기존 Guest userId를 유지한 MEMBER 승격임을 정리했다.
- 구현 내용: 전화번호는 회원가입 시 소유를 검증하지만 로그인 식별자나 자동 병합 키로 쓰지 않는다. Guest는 둘러볼 수 있으나 무료 모의고사는 ACTIVE MEMBER만 가능하고, TrialClaim은 검증 번호별 무료혜택 1회 수령 이력, UserEntitlement는 실제 예약·사용되는 권리라는 차이를 설명했다.
- 구현 내용: 기존 SocialIdentity owner가 발견되는 Guest 요청은 준비 단계에서 `MERGE_REQUIRED`로 멈추고, 공통 source JWT 거절·outbox·downstream migration이 준비된 단계 8에서만 ACTIVE GUEST를 ACTIVE MEMBER canonical User로 병합한다. 무료시험은 첫 요청에서 lazy claim한 뒤 `reserve → exam 생성 → confirm`, 확정 실패 cancel, 결과 불명 reconciliation으로 처리한다.
- 실행한 테스트와 결과: 설명과 기록 문서만 갱신했으므로 `./gradlew clean test`는 실행하지 않았다. 종료 전 `git diff --check`, trailing whitespace, 지정 turn marker 단일 존재와 저장소 변경 상태를 정적으로 확인한다.
- 유지한 계약: 실제 userId와 JWT `sub`는 Identity가 생성한 canonical UUID 문자열이다. Provider subject·email·전화번호를 userId로 사용하지 않고 외부 Body의 userId를 신뢰하지 않는다. 전화번호는 자동 merge 키가 아니며 Identity는 시험·TrialClaim·UserEntitlement 코드를 소유하지 않는다. RS256·JWKS, Identity/Learning Core 경계와 Python AI `user_id=examId` 계약을 유지했고 Secret·Token·Password·raw 전화번호·OTP·fingerprint·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 기존 전체 계획을 변경하지 않고 사용자가 비교하기 쉬운 관점으로만 재구성한다. 핵심 비교 기준은 canonical User 분리, 처음부터 SNS 진입 지원, 가입 시 전화 인증, Guest와 MEMBER의 무료시험 경계, 전화번호와 SocialIdentity의 병합 책임 분리, TrialClaim과 UserEntitlement의 역할 분리다. Jira 작업과 Git commit·push는 수행하지 않았다.
- 위험 요소: 압축 요약은 nonce CAS, HMAC key rotation, outbox lease·retry, reservation reconciliation 같은 구현 세부사항을 생략할 수 있으므로 실제 구현 기준은 계속 전체 계획서를 따른다. 단계 0에 남은 Provider·OTP·개인정보 보존·서비스 간 세부 계약은 별도로 확정해야 한다.
- 다음 작업: 사용자가 자신의 이해와 비교해 다른 부분을 알려주면 해당 차이가 제품 정책인지 설계 오해인지 구분해 전체 계획서에 필요한 수정만 반영한다. 합의 후 첫 구현은 최소 SocialIdentity 모델·index·Repository부터 시작한다.

## 2026-08-13 — 생성 예정 엔티티 역할과 전체 로직 설명

<!-- codex-turn:019ff8a7-2500-7b80-b4e2-bbf639fbc12b -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 합의된 전체 계획에서 생성·확장될 엔티티의 소유 서비스, 수명, 책임과 직접 SNS 가입·Guest 승격·병합·무료 모의고사 흐름에서의 상호작용을 사용자가 이해할 수 있도록 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드와 `docs/social-login-implementation-plan.md`의 설계는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Identity의 장기 엔티티를 canonical 계정인 User, 외부 계정 매핑인 SocialIdentity, 검증 번호 소유 관계인 PhoneIdentity, HMAC key version 교차 동일성을 보존하는 PhoneFingerprintAlias, 로그인 세션인 기존 RefreshSession으로 구분했다. 단기 시도 엔티티는 nonce replay를 막는 SocialLoginChallenge, 검증된 SNS principal을 최종 가입까지 이어 주는 SocialEnrollmentAttempt, OTP 요청·검증·grant 상태를 관리하는 PhoneVerificationAttempt로 설명했다.
- 구현 내용: Guest 병합 시 User의 MERGED 상태 전환과 RefreshSession 폐기, UserMergedOutbox 생성, downstream의 eventId 멱등 처리·ownership migration·source deny marker 저장 관계를 정리했다. UserMerged는 전달 payload 개념이고 outbox document가 Identity의 영속 전달 상태임을 구분했다.
- 구현 내용: 별도 Entitlement/Billing 소유의 TrialClaim은 검증 번호별 무료혜택 수령 ledger, UserEntitlement는 canonical User의 실제 사용권 잔액, EntitlementReservation은 시험 생성 중 해당 사용권을 잠그는 분산 작업 상태로 설명했다. 회원가입 때는 PhoneIdentity까지만 생성하고 첫 무료시험 요청에서 claim·grant·reserve한 뒤 exam 생성 결과에 따라 confirm·cancel·reconciliation하도록 정리했다.
- 구현 내용: VerifiedSocialPrincipal, SocialProvider enum, socialEnrollmentGrant와 phoneVerificationGrant는 독립 장기 엔티티가 아니라 각각 검증 결과·namespace·일회성 bearer proof임을 명시했다. direct signup과 Guest enrollment에서 social·phone attempt를 같은 binding으로 묶고 최종 Mongo Transaction의 conditional consume 승자만 User·identity·Session 변경을 commit하는 흐름을 설명했다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석·설명 작업이므로 `./gradlew clean test`는 실행하지 않았다. 종료 전 `git diff --check`, 두 기록 문서의 trailing whitespace, 지정 turn marker 단일 존재와 저장소 변경 상태를 정적으로 확인한다.
- 유지한 계약: 실제 userId와 JWT `sub`는 canonical UUID 문자열이다. Provider subject·email·전화번호를 userId나 자동 병합 키로 사용하지 않고 외부 Body의 userId를 신뢰하지 않는다. Identity는 TrialClaim·UserEntitlement·EntitlementReservation·시험 데이터를 소유하지 않는다. RS256·JWKS, Identity/Learning Core 경계와 Python AI `user_id=examId` 계약을 유지했고 Secret·Token·Password·raw 전화번호·OTP·fingerprint·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 영속 엔티티와 일시적인 검증 결과·grant를 구분해 설명하며, 생성 시점과 삭제·상태 전이의 차이를 사용자 흐름에 연결한다. 기존 전체 계획을 해석한 작업으로 새로운 제품 정책이나 구현 범위를 추가하지 않았다. Jira 작업과 Git commit·push는 수행하지 않았다.
- 위험 요소: TrialClaim·UserEntitlement·EntitlementReservation은 Identity 저장소에 구현하지 않으며 실제 Entitlement/Billing 경계와 API 계약이 별도로 필요하다. PhoneFingerprintAlias key registry, outbox consumer, source JWT deny marker와 reservation reconciliation이 일부만 구현되면 version 교차 중복·병합 후 접근·무료권 유실 문제가 생길 수 있다.
- 다음 작업: 사용자 이해와 다른 부분이 있으면 제품 정책과 기술 구현을 구분해 수정한다. 합의 후 첫 구현은 계획대로 최소 SocialIdentity 모델·index·Repository부터 시작하고 코드 변경 시 `./gradlew clean test`와 격리 Mongo index 검증을 수행한다.

## 2026-08-13 — SNS nonce·challenge·일회성 소비 의미 설명

<!-- codex-turn:019ff8ca-6834-7742-926e-383b67cc0bea -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 서버 발급 SNS 인증 흐름에서 nonce와 SocialLoginChallenge의 역할 차이, Provider Token nonce 검증, challenge를 소비한다는 상태 전이의 의미를 이해하기 쉽게 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드와 `docs/social-login-implementation-plan.md`의 설계는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: nonce를 Identity가 인증 시작마다 생성해 Provider SDK에 전달하는 예측 불가능한 일회성 값으로 설명했다. Provider가 돌려준 ID Token의 nonce Claim과 서버가 challenge에 보관한 expected nonce를 비교해 해당 Token이 바로 그 인증 시도에서 발급됐는지 확인하며, 클라이언트가 login Body로 보낸 nonce를 기대값으로 신뢰하지 않는다고 정리했다.
- 구현 내용: SocialLoginChallenge를 nonce 자체가 아니라 expected nonce hash, Provider, LOGIN_OR_SIGNUP/LINK 목적, 필요 시 JWT `sub` 사용자 binding, 만료 시각과 PENDING/CONSUMED 상태를 보관하는 짧은 수명의 서버 인증 시도 레코드로 구분했다. socialChallengeId는 이 레코드를 찾는 opaque 식별자다.
- 구현 내용: challenge 소비는 물리적 삭제이나 Token 사용을 뜻하는 표현이 아니라, 모든 검증 성공 후 조건부 `PENDING → CONSUMED` 전이를 수행해 승자 한 요청만 후속 로그인·가입·연결을 진행시키는 것을 뜻한다. 이후 같은 challenge나 Provider credential을 다시 보내면 이미 소비된 시도로 거절해 replay와 동시 중복 처리를 막는다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석·설명 작업이므로 `./gradlew clean test`는 실행하지 않았다. `git diff --check`, 두 기록 문서의 trailing whitespace, 지정 turn marker 단일 존재를 정적으로 검증한다.
- 유지한 계약: 서버가 발급한 challenge에서 expected nonce와 Provider·목적·사용자 binding을 조회하며 외부 Body의 nonce나 userId를 신뢰하지 않는다. 실제 userId와 JWT `sub`는 canonical UUID 문자열이고 RS256·JWKS, Identity/Learning Core 경계와 Python AI `user_id=examId` 계약을 유지했다. Secret·Token·Password·nonce 원문·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: nonce는 요청과 Provider 인증 결과의 결속 값, challenge는 그 검증 문맥과 수명주기를 보관하는 서버 측 시도 레코드, consumption은 일회성 사용을 강제하는 원자적 상태 전이로 설명한다. 기존 설계나 구현 범위는 변경하지 않았고 Jira 작업과 Git commit·push는 수행하지 않았다.
- 위험 요소: nonce 비교 전에 challenge를 소비하면 Provider 장애나 검증 실패에도 정상 재시도를 막을 수 있고, 로그인·가입 같은 부수 효과 뒤에 소비하면 동시 요청이 중복 Session이나 계정을 만들 수 있다. 실제 구현에서는 검증 순서와 CAS 승자만 후속 처리를 진행하는 원자성 또는 Transaction 경계를 보장해야 한다.
- 다음 작업: Provider별 raw/hashed nonce 규칙과 challenge TTL을 확정한 뒤 SocialLoginChallenge 구현에서 만료·Provider/purpose/user binding 불일치·재사용·동시 요청 CAS 테스트를 추가한다.

## 2026-08-13 — EmailAvailabilityService 책임과 한계 설명

<!-- codex-turn:019ff8e4-7635-7b52-8b97-cdaf3d786b93 -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 현재 `EmailAvailabilityService`의 입력부터 응답까지의 호출 흐름, 이메일 정규화 기준, 실제 회원가입 중복 방지와의 관계 및 보장하지 않는 범위를 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 공개 `POST /api/v1/auth/check-email`에서 `CheckEmailRequest`가 공백 제거와 `@NotBlank`·`@Email` 검증을 수행하고 Controller가 `EmailAvailabilityService.checkEmail`을 호출하는 흐름을 확인했다. 서비스는 `EmailNormalizer`로 다시 trim하고 `Locale.ROOT` 소문자화한 값을 `UserRepository.existsByNormalizedEmail`에 전달한다.
- 구현 내용: 조회 결과가 없으면 `isAvailable=true`와 사용 가능 메시지, 있으면 정상 200 응답 안에 `isAvailable=false`와 이미 사용 중 메시지를 반환한다. 대소문자와 양끝 공백은 중복 판단에서 구분하지 않지만 Gmail dot·plus 같은 Provider별 별칭 규칙은 적용하지 않는다.
- 구현 내용: availability 결과는 조회 순간의 안내일 뿐 이메일 예약이나 이후 가입 성공을 보장하지 않는다. 조회 후 다른 요청이 먼저 가입할 수 있으므로 `SignupService`가 다시 존재 여부를 검사하고, 동시 가입의 최종 방어는 `normalizedEmail` partial unique index와 `DuplicateKeyException`의 `EMAIL_ALREADY_EXISTS` 변환이 담당함을 구분했다. 탈퇴 tombstone은 이메일 필드를 제거하므로 현재 정책상 해당 이메일을 다시 사용할 수 있다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석·설명 작업이므로 `./gradlew clean test`는 실행하지 않았다. 기존 application test의 사용 가능·불가능·trim·소문자화 검증과 Controller test의 200 응답 계약을 읽어 확인했으며, 종료 전 `git diff --check`, 기록 문서 trailing whitespace와 지정 turn marker 단일 존재를 정적으로 검증한다.
- 유지한 계약: 클라이언트가 보낸 userId를 사용하지 않고 이메일 원문을 로그나 작업 기록에 남기지 않았다. 실제 userId와 JWT `sub`의 canonical UUID, Identity 도메인 경계, RS256·JWKS와 Python AI `user_id=examId` 계약을 변경하지 않았으며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: `EmailAvailabilityService`는 조회와 응답 변환만 담당하고 회원가입 원자성이나 이메일 소유권을 담당하지 않는 application service로 설명한다. 기존 코드·API·정책을 변경하지 않았고 Jira 작업과 Git commit·push는 수행하지 않았다.
- 위험 요소: 인증 없이 호출 가능한 API가 특정 이메일의 가입 여부를 직접 구분하므로 계정 열거에 이용될 수 있다. 운영 공개 전 rate limit·abuse monitoring·제품 메시지 정책을 검토해야 하며, check 결과를 신뢰해 `SignupService`의 중복 검사나 DB unique index를 제거하면 안 된다.
- 다음 작업: 정책상 계정 존재 공개를 허용할지 확정하고, 허용한다면 공개 endpoint의 rate limit과 관측 기준을 마련한다. 기능 변경이 필요할 때에는 Controller·application service·동시 가입 unique-index 방어 테스트를 함께 유지한다.

## 2026-08-13 — 첫 SocialIdentity 구현 범위 재확인

<!-- codex-turn:019ff8e8-5dcf-70d3-a996-378bd604c55b -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 중단된 설명을 이어서 전체 소셜 로그인 로드맵 중 첫 구현 PR에 실제로 포함되는 항목과 제외되는 후속 항목을 명확히 구분한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드와 `docs/social-login-implementation-plan.md`의 합의된 범위는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 첫 범위는 `SocialProvider(GOOGLE, KAKAO, APPLE)`, `SocialIdentity(socialIdentityId, userId, provider, providerSubject[1..255], createdAt)`, `(provider, providerSubject)` compound unique index, `userId` non-unique index와 `findByProviderAndProviderSubject`·`findAllByUserId` Repository 계약으로 한정됨을 재확인했다.
- 구현 내용: 도메인 생성 불변식, Provider별 동일 문자열 subject 허용, 한 User의 복수 Provider 연결, provider+subject와 userId 조회, index metadata·실제 unique enforcement를 외부 Atlas 없이 검증하는 테스트가 첫 범위에 포함된다. 실제 중복 insert 검증 환경은 구현 시 격리 Mongo 방식을 선택해야 한다.
- 구현 내용: email·updatedAt, User·UserAccountType migration, Google·Kakao·Apple Token verifier, SocialLoginChallenge·SocialEnrollmentAttempt, Controller와 login/signup/link API, Guest 승격·병합, PhoneIdentity·OTP, TrialClaim·UserEntitlement·EntitlementReservation, outbox·Learning Core event와 Access/Refresh 발급 로직 변경은 첫 범위에서 제외됨을 정리했다.
- 실행한 테스트와 결과: 코드 변경이 없는 범위 확인·기록 작업이므로 `./gradlew clean test`는 실행하지 않았다. 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 저장소 변경 상태를 정적으로 확인한다.
- 유지한 계약: 실제 userId와 JWT `sub`는 canonical UUID 문자열이며 SocialIdentity는 외부 identity에서 canonical userId로의 매핑만 소유한다. email은 로그인 식별자가 아니고 전화번호도 로그인·자동 병합 키가 아니다. Identity/Learning Core 경계와 기존 RS256·JWKS·Access/Refresh 계약을 변경하지 않았으며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 첫 PR의 완료 결과는 SNS 연결 정보를 안전하게 저장·조회할 수 있는 데이터 기반이며 실제 SNS 로그인이 동작하는 상태가 아니다. 첫 PR에 후속 User·API·OTP·merge·entitlement 범위를 섞지 않는다. Jira 작업과 Git commit·push는 수행하지 않았다.
- 위험 요소: 현재 test profile은 Mongo 자동설정을 제외하므로 annotation metadata 테스트만으로 실제 unique insert 차단까지 증명할 수 없다. 격리 Mongo 또는 승인된 staging 검증 없이 DB 최종 동시성 방어가 완료됐다고 선언하면 안 된다.
- 다음 작업: 첫 구현을 승인하면 저장소 구조를 다시 확인해 최소 SocialIdentity entity·enum·Repository·index 테스트를 작성하고 `./gradlew clean test`를 실행한다. 실제 OAuth Provider와 Atlas는 기본 테스트에서 호출하지 않는다.

## 2026-08-13 — 회원 탈퇴 tombstone과 필드 제거 의미 설명

<!-- codex-turn:019ff8e8-38c7-7ad2-8eef-6aa0124e2ae0 -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: `tombstone에서 이메일을 제거한다`는 표현이 사용자 문서 삭제와 어떻게 다른지, 현재 회원 탈퇴 구현에서 남는 정보와 제거되는 정보를 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: tombstone은 탈퇴 시 `users` document를 물리 삭제하는 대신 동일한 `userId`를 유지하면서 `status=WITHDRAWN`으로 표시한 최소 잔존 레코드라고 설명했다. 현재 구현은 nickname을 `탈퇴한 사용자`로 치환하고 `updatedAt`·`withdrawnAt`을 탈퇴 시각으로 기록하며 기존 생성 시각, provider와 consent 구조는 유지한다.
- 구현 내용: `User.toWithdrawnTombstone`은 email·normalizedEmail·passwordHash·guestInstallationIdHash를 null인 탈퇴 객체로 만들고, `UserRepositoryCustomImpl.withdrawIfUnchanged`는 MongoDB `$unset`으로 실제 document에서 네 필드를 제거한다. 따라서 `tombstone에서 제거`는 tombstone 자체 삭제가 아니라 그 안의 개인정보·자격증명 필드를 없앤다는 의미로 구분했다.
- 구현 내용: normalizedEmail 필드가 사라지면 partial unique index 대상에서도 빠져 같은 이메일의 신규 가입이 가능해진다. 남은 userId와 WITHDRAWN 상태는 탈퇴 계정임을 식별하고 활성 사용자로 취급하지 않기 위한 표식이며, 별도 탈퇴 Transaction은 기존 RefreshSession도 폐기한다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석·설명 작업이므로 `./gradlew clean test`는 실행하지 않았다. 탈퇴 entity 변환과 custom repository `$unset` 구현 및 관련 테스트 위치를 읽어 확인했으며, 종료 전 `git diff --check`, 기록 문서 trailing whitespace와 지정 turn marker 단일 존재를 정적으로 검증한다.
- 유지한 계약: 탈퇴 후 credential 필드를 보존하거나 로그에 노출하지 않고 실제 userId와 JWT `sub`의 canonical UUID, Identity 도메인 경계, RS256·JWKS와 Python AI `user_id=examId` 계약을 변경하지 않았다. Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: tombstone과 hard delete를 구분하고, `제거`의 목적어가 user document가 아니라 email·normalizedEmail·passwordHash·guestInstallationIdHash 필드임을 명확히 설명한다. 기존 데이터 보존 정책이나 코드는 변경하지 않았고 Jira 작업과 Git commit·push는 수행하지 않았다.
- 위험 요소: tombstone에 현재 유지되는 provider·consent 정보의 법적 보존 근거와 기간은 별도 개인정보 정책 검토가 필요하다. tombstone은 Identity 레코드의 상태 표시이며 Learning Core 등 다른 서비스의 사용자 데이터 삭제·익명화를 자동으로 보장하지 않는다.
- 다음 작업: 개인정보 보존·삭제 정책에서 tombstone의 필드별 보존 근거와 기간, downstream 데이터 삭제 또는 익명화 계약을 확정한다.

## 2026-08-13 — 첫 SocialIdentity Jira 생성 초안 작성

<!-- codex-turn:019ff8eb-64b0-7c12-a380-f49b7763ec5a -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 첫 구현 범위인 최소 SocialIdentity 모델·MongoDB index·Repository 작업을 TMI 프로젝트의 신규 Jira 이슈로 등록하기 전에, 생성할 내용을 정확히 작성하고 사용자 승인용 초안을 제시한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드와 `docs/social-login-implementation-plan.md`는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Atlassian 공식 MCP로 기존 Identity 이슈와 TMI 프로젝트 생성 metadata를 읽기 전용 조회했다. TMI에는 `작업` 유형이 있고 프로젝트·이슈 유형·요약이 필수이며 보고자는 기본값을 사용할 수 있음을 확인했다. SocialIdentity 관련 유사 이슈는 검색 결과에서 발견되지 않았다.
- 구현 내용: 생성 초안은 TMI 프로젝트의 `작업`, 제목 `[Identity] SocialIdentity 모델·인덱스·Repository 기반 구축`으로 준비했다. 담당자·우선순위·라벨·상태 전환은 지정하지 않고 Jira 기본값을 사용하며, 설명에는 배경·목표·도메인 모델·index·Repository·기능 요구사항·완료 조건·테스트·제외 범위·유지 계약을 포함한다.
- 구현 내용: 첫 범위에는 `SocialProvider(GOOGLE, KAKAO, APPLE)`, email·updatedAt 없는 SocialIdentity, provider+subject unique index, userId index, 두 Repository 조회 계약과 테스트만 포함한다. Provider 검증·API·User 변경·Guest 승격·병합·PhoneIdentity·OTP·Entitlement·outbox·Access/Refresh 변경은 Jira 제외 범위로 명시한다.
- 실행한 테스트와 결과: 코드 변경이 없는 Jira 조회·초안 작성 작업이므로 `./gradlew clean test`는 실행하지 않았다. 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 저장소 상태를 정적으로 확인한다.
- 유지한 계약: 실제 userId와 JWT `sub`는 canonical UUID 문자열이고 SocialIdentity는 외부 identity에서 canonical userId로의 매핑만 소유한다. email을 로그인 식별자로 사용하지 않고 외부 Body의 userId를 신뢰하지 않는다. Identity/Learning Core 경계와 기존 RS256·JWKS·Access/Refresh 계약을 변경하지 않았으며 Jira나 기록에 Secret·Token·Password·실제 Key·전체 MongoDB URI·사용자 개인정보를 넣지 않았다.
- 결정사항: 저장소 규칙에 따라 생성 전에 전체 Jira 초안을 사용자에게 보여주고 명시적 승인을 받는다. 이번 작업에서는 Jira 생성·수정·댓글·상태 변경을 수행하지 않았고 발급된 Jira 키도 없다. Git commit·push도 수행하지 않았다.
- 위험 요소: 담당자·우선순위·라벨을 지정하지 않으므로 프로젝트 기본값이 적용되며, 사용자가 이를 요구하면 생성 승인 전에 초안과 실행 payload를 수정해야 한다. 실제 Mongo unique enforcement 테스트 방식은 구현 착수 시 격리 Mongo 환경으로 확정해야 한다.
- 다음 작업: 사용자가 제시된 Jira 초안을 승인하면 동일 내용으로 이슈를 한 번 생성하고 결과 키·상태를 재조회한다. 생성 후 Jira 키를 CURRENT_STATE와 새 WORKLOG 항목에 기록하고 구현 전 해당 이슈를 다시 읽어 완료 조건을 기준으로 작업한다.

## 2026-08-13 — TMI-88 SocialIdentity 기반 작업 Jira 생성

<!-- codex-turn:019ff8ee-b9b0-7a52-b4fd-b1ce6b0ca1a0 -->

- 날짜: 2026-08-13
- Jira: TMI-88
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: 사용자가 승인한 첫 SocialIdentity 구현 초안을 TMI 프로젝트의 Jira 작업으로 생성하고 생성 결과와 저장된 계약을 재확인한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드와 `docs/social-login-implementation-plan.md`는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Atlassian 공식 MCP로 Jira `TMI-88` `[Identity] SocialIdentity 모델·인덱스·Repository 기반 구축`을 `작업` 유형으로 한 번 생성했다. 승인된 설명에는 SocialProvider, email·updatedAt 없는 SocialIdentity, provider+subject unique index, userId index, Repository 계약, 완료 조건·테스트·제외 범위·기존 계약을 기록했다.
- 수행한 Jira 작업: 신규 이슈 생성 후 `TMI-88`을 재조회해 프로젝트 `TMI`, 유형 `작업`, 상태 `해야 할 일`, 담당자 없음, 프로젝트 기본 우선순위 `Medium`, 빈 label·component와 승인한 본문이 저장됐음을 확인했다.
- 추가한 댓글의 목적: 댓글은 추가하지 않았다.
- 변경한 상태: 생성 기본 상태인 `해야 할 일`을 유지했고 별도 상태 전환은 수행하지 않았다.
- 승인 여부: 사용자가 전체 Jira 초안을 확인한 뒤 명시적으로 생성을 승인했다.
- 실행한 테스트와 결과: 코드 변경이 없는 Jira 생성·문서 기록 작업이므로 `./gradlew clean test`는 실행하지 않았다. 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 저장소 상태를 정적으로 확인한다.
- 유지한 계약: 실제 userId와 JWT `sub`는 canonical UUID 문자열이며 SocialIdentity는 외부 identity에서 canonical userId로의 매핑만 소유한다. email을 로그인 식별자로 사용하지 않고 외부 Body의 userId를 신뢰하지 않는다. Identity/Learning Core 경계와 기존 RS256·JWKS·Access/Refresh 계약을 변경하지 않았으며 Jira나 기록에 Secret·Token·Password·실제 Key·전체 MongoDB URI·사용자 개인정보를 넣지 않았다.
- 결정사항: 첫 구현 범위는 Jira `TMI-88`의 설명과 완료 조건을 기준으로 하며 Provider 검증·API·User 변경·Guest 승격·병합·PhoneIdentity·OTP·Entitlement·outbox·Access/Refresh 변경은 포함하지 않는다. Jira 댓글·상태 전환과 Git commit·push는 수행하지 않았다.
- 위험 요소: 현재 test profile은 Mongo 자동설정을 제외하므로 실제 duplicate insert unique enforcement를 증명할 격리 Mongo 방식이 구현 전에 필요하다. 이 범위를 넘는 요구는 TMI-88에 섞지 않고 후속 이슈로 분리해야 한다.
- 다음 작업: 구현을 시작할 때 Jira `TMI-88`을 다시 읽고 완료 조건을 기준으로 entity·enum·Repository·index 테스트를 작성한다. 전체 `./gradlew clean test`와 외부 Atlas·OAuth Provider를 호출하지 않는 격리 Mongo 검증을 수행하고 Jira 댓글·상태 변경은 별도 승인 후 진행한다.

## 2026-08-13 — GuestAuthService 생성·인증·원자성 설명

<!-- codex-turn:019ff900-842c-7793-b9ba-b876dcb00cfc -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: `GuestAuthService`가 Guest 생성 요청에서 검증·중복 방지·User 및 Token 준비·Transaction 저장·오류 변환을 어떻게 조율하며 무엇을 보장하지 않는지 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 공개 `POST /api/v1/auth/guest`의 `GuestAuthRequest`가 canonical UUID v4 설치 식별자와 필수 개인정보·약관 동의 및 현재 정책 버전을 받고, Service가 `ConsentPolicy`를 먼저 검증하는 흐름을 확인했다. 실패하면 User·Token·Session 준비를 시작하지 않는다.
- 구현 내용: 설치 UUID는 정규화 후 SHA-256·Base64URL hash로 변환하고 원문 대신 hash만 User에 저장한다. 이 hash로 사전 중복 조회하며 `guestInstallationIdHash` partial unique index가 동시 요청의 최종 중복을 막는다. 설치 UUID는 중복 방지 식별자일 뿐 인증 credential이나 기존 Guest 복구 수단이 아니므로 같은 설치의 재호출은 Token 재발급 없이 `GUEST_ALREADY_EXISTS` 409가 된다.
- 구현 내용: 중복이 없으면 서버 UUID, GUEST provider, ACTIVE 상태, 고정 Guest nickname, email·password 없는 User와 현재 동의 시각을 만든다. User 저장 전에 RS256 Access Token과 원문을 DB에 저장하지 않는 RefreshSession을 준비하고, `GuestRegistrationTransactionService`가 User와 RefreshSession 저장 및 응답 구성을 Mongo Transaction으로 함께 commit한다. 중간 실패 시 두 document를 rollback하고 Transaction proxy 반환 후에만 안전한 완료 로그를 남긴다.
- 구현 내용: 같은 설치의 동시 요청이 모두 사전 조회를 통과해도 unique 충돌 후 해당 설치 hash의 실제 승자가 존재할 때만 `GUEST_ALREADY_EXISTS`로 변환한다. 다른 unique index 충돌은 Guest 중복으로 오분류하지 않는다. commit 뒤 응답이 유실되어 재시도해도 설치 UUID만으로 기존 Token을 다시 받을 수 없고 conflict가 반환되는 비복구 정책임을 설명했다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석·설명 작업이므로 `./gradlew clean test`는 실행하지 않았다. Service·Transaction service·request/response·hasher·Factory 코드와 기존 동의 실패, Token 준비 실패, rollback, 동시 중복, 응답 유실 재시도 테스트를 읽어 확인했다. 종료 전 `git diff --check`, 기록 문서 trailing whitespace와 지정 turn marker 단일 존재를 정적으로 검증한다.
- 유지한 계약: 실제 Guest userId는 서버 생성 UUID이고 Access Token `sub`에 사용된다. 설치 UUID를 userId나 인증 credential로 사용하지 않고 원문 설치 UUID·Access Token·Refresh Token을 로그나 DB에 보존하지 않는다. RS256·JWKS, Identity/Learning Core 경계와 Python AI `user_id=examId` 계약을 유지했으며 Secret·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: `GuestAuthService`는 Guest 등록 유스케이스의 순서를 조율하고 실제 다중 document 원자성은 proxy 경계가 분리된 `GuestRegistrationTransactionService`가 담당한다고 설명한다. 기존 코드·API·정책을 변경하지 않았고 Jira 작업과 Git commit·push는 수행하지 않았다.
- 위험 요소: 설치 UUID와 Token을 모두 잃거나 commit 응답이 유실되면 설치 UUID만으로 Guest를 복구할 수 없다. Guest Access Token은 stateless라 탈퇴나 로그아웃 후에도 최대 TTL 동안 암호학적으로 유효할 수 있고, 설치 UUID 원문은 엔트로피가 제한된 식별자여서 hash 저장이 강한 비밀성을 뜻하지 않는다. 실제 Mongo Transaction은 replica set 환경에서 검증해야 한다.
- 다음 작업: 클라이언트가 최초 성공 응답의 Token을 안전하게 저장하고 응답 불명 상황을 처리하는 UX를 확정한다. 필요하다면 설치 UUID를 credential로 승격하지 않는 별도의 Guest 복구·재발급 계약을 설계하고, staging replica set에서 Transaction 및 동시 중복을 재검증한다.

## 2026-08-13 — TMI-88 최소 SocialIdentity 데이터 기반 구현

<!-- codex-turn:019ff926-cbf7-7a60-ab58-7f99d28a6d86 -->

- 날짜: 2026-08-13
- Jira: TMI-88
- 브랜치: `feat/TMI-88-social-identity-foundation` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: Google·Kakao·Apple 인증 API 전에 외부 identity와 canonical User를 분리해 저장·조회하는 최소 SocialIdentity 모델, MongoDB index, Repository와 실제 중복 차단 테스트를 Jira 완료 조건대로 구현한다.
- 변경 파일: `build.gradle`, `src/main/java/web/tosunsaeng/identity/domain/auth/domain/enums/SocialProvider.java`, `src/main/java/web/tosunsaeng/identity/domain/auth/domain/entity/SocialIdentity.java`, `src/main/java/web/tosunsaeng/identity/domain/auth/domain/repository/SocialIdentityRepository.java`, `src/test/java/web/tosunsaeng/identity/domain/auth/domain/entity/SocialIdentityTests.java`, `src/test/java/web/tosunsaeng/identity/domain/auth/domain/repository/SocialIdentityRepositoryTests.java`, `src/test/java/web/tosunsaeng/identity/domain/auth/domain/repository/SocialIdentityRepositoryIntegrationTests.java`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 변경했다. 기존 untracked 계획 문서는 이번 구현에서 수정하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Auth 도메인에 `SocialProvider(GOOGLE, KAKAO, APPLE)`와 `social_identities` collection의 `SocialIdentity`를 추가했다. entity는 서버 생성 UUID `socialIdentityId`, canonical UUID 문자열 `userId`, non-null provider, case-sensitive opaque 원문을 보존하는 1~255자 `providerSubject`, non-null `createdAt`만 보유한다.
- 구현 내용: `uk_social_identities_provider_subject`를 provider ASC·providerSubject ASC의 compound unique index로, `ix_social_identities_user_id`를 non-unique index로 선언했다. email·updatedAt·Provider Token·전체 Claim·User 객체·`@DBRef`와 `(userId, provider)` unique 제약은 추가하지 않았다.
- 구현 내용: `SocialIdentityRepository`에 `findByProviderAndProviderSubject`와 `findAllByUserId`를 추가했다. `mongo-java-server` test dependency를 사용해 외부 Atlas 없이 임의 로컬 포트의 순수 Java 인메모리 Mongo를 테스트마다 기동하고 Spring Data Repository proxy와 index resolver의 실제 저장·조회·index enforcement를 검증했다.
- 실행한 테스트와 결과: `./gradlew test --tests '*SocialIdentity*'`에서 신규 13개 테스트가 성공했다. 첫 `./gradlew clean test`는 샌드박스의 사용자 Gradle cache lock 접근 제한으로 코드 실행 전에 중단됐고, 승인된 동일 명령을 샌드박스 밖에서 재실행해 44개 suite·308개 테스트가 skip 0·failure 0·error 0으로 성공했다. `git diff --check`도 성공했다.
- 유지한 계약: 실제 userId와 JWT `sub`는 canonical UUID 문자열이다. Provider subject와 email을 userId로 사용하거나 외부 Body에 userId를 추가하지 않았다. 기존 User·UserProvider, Controller, Provider verifier, Access/Refresh Token, RS256·kid·issuer·audience·JWKS와 Identity/Learning Core 및 Python AI `user_id=examId` 계약을 변경하지 않았다. Secret·Token·Password·실제 Key·전체 MongoDB URI·Provider 개인정보를 기록하지 않았다.
- 결정사항: 애플리케이션 선조회가 아니라 MongoDB `(provider, providerSubject)` unique index를 외부 계정 단일 owner의 최종 경쟁 경계로 둔다. subject는 trim·case folding하지 않고 원문을 저장하며 Provider namespace가 다르면 같은 문자열을 허용한다. 실제 duplicate insert 자동화는 Docker·Atlas 없이 실행 가능한 격리 Mongo로 구현했다. Jira 댓글·상태·필드는 변경하지 않았고 Git commit·push도 수행하지 않았다.
- 위험 요소: 인메모리 Mongo 테스트는 Repository query와 index 동작을 자동 회귀하지만 운영 MongoDB 버전·권한·기존 데이터·무중단 index build 영향을 대체하지 않는다. 운영 writer 활성화 전 staging에서 두 index의 실제 생성과 중복 데이터 부재를 확인해야 한다. `providerSubject` 255자 상한은 Provider verifier 구현 전에 각 공식 계약과 Mongo index byte 한도를 다시 확인한다.
- 다음 작업: 사용자가 변경을 검토한 뒤 직접 commit·push한다. Jira 댓글 초안은 SocialProvider·SocialIdentity·index·Repository 구현, 변경 파일, 전체 44개 suite·308개 테스트 성공과 staging index 검증 위험만 포함해 제시하되 승인 전에는 등록하지 않는다. 다음 구현은 별도 범위인 User accountType·PhoneIdentity 또는 Provider challenge/verifier 순서를 계획에 따라 진행한다.

## 2026-08-13 — Guest 설치 UUID SHA-256과 응답 유실 처리 상태 설명

<!-- codex-turn:019ff940-a866-7872-95c0-e2c9f8937de6 -->

- 날짜: 2026-08-13
- 브랜치: `feat/TMI-88-social-identity-foundation` (`b6eb73e` 기준, commit·push 미수행)
- 작업 목표: Guest 설치 UUID의 SHA-256 변환 과정과 보안적 성격을 설명하고, Guest 생성 commit 후 응답 유실에 대한 현재 구현과 미구현 복구 범위를 구분한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: `GuestInstallationIdHasher`가 입력을 trim한 뒤 canonical UUID v4·RFC variant인지 검증하고 `UUID.toString()`의 lowercase 문자열로 정규화함을 확인했다. 그 UTF-8 bytes에 Java `MessageDigest`의 SHA-256을 적용해 32 bytes digest를 만들고 padding 없는 Base64URL 문자열 43자로 변환해 `guestInstallationIdHash`에 저장한다.
- 구현 내용: SHA-256은 같은 입력에서 같은 digest가 나오는 단방향 fingerprint라 동일 설치 중복 조회에 사용할 수 있지만 복호화하는 암호화 방식이 아니다. 현재 구현은 random salt나 서버 비밀키 HMAC을 사용하지 않는다. UUID v4의 높은 무작위성 때문에 일반 짧은 비밀번호와 같은 사전대입 대상은 아니지만 hash 저장만으로 원문이 인증 비밀이 되는 것은 아니라고 설명했다.
- 구현 내용: 서버가 User·RefreshSession을 commit한 뒤 HTTP 응답이 클라이언트에 도착하지 않는 네트워크 유실을 감지하거나, 설치 UUID로 기존 Refresh Token 원문을 복원·재발급하는 코드는 없다. 원문 Refresh Token은 DB에 저장하지 않으므로 그대로 되돌릴 수도 없다. 같은 설치 UUID 재시도는 기존 중복 검사 코드에 의해 `GUEST_ALREADY_EXISTS`가 되는 현재 실패 정책이다.
- 구현 내용: `commitThenResponseLossRetryStaysConflictWithoutPreparingAnotherToken` 테스트가 첫 등록이 반영된 상황에서 재시도하면 새 Token이나 Transaction을 준비하지 않고 conflict가 반환됨을 검증한다. 따라서 응답 유실 상황의 현재 동작과 회귀 테스트는 구현되어 있지만, 응답 복구·멱등 재응답·별도 Guest recovery API는 구현되지 않았다고 구분했다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석·설명 작업이므로 이번 turn에서 Gradle 테스트를 새로 실행하지 않았다. hasher 구현과 기존 응답 유실 재시도 테스트를 읽어 확인했으며, 종료 전 `git diff --check`, 기록 문서 trailing whitespace와 지정 turn marker 단일 존재·WORKLOG 끝 append를 정적으로 검증한다.
- 유지한 계약: 설치 UUID는 중복 방지 식별자일 뿐 userId나 인증 credential이 아니며 실제 Guest userId와 JWT `sub`는 서버 생성 UUID다. 원문 설치 UUID·Access Token·Refresh Token을 로그나 작업 기록에 남기지 않고 Refresh Token 원문을 DB에 저장하지 않는 계약, RS256·JWKS와 Identity/Learning Core 경계를 유지했다. Secret·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 현재 구현과 미구현을 분리해, SHA-256 중복 fingerprint와 재시도 conflict는 구현 완료 상태이고 유실 응답의 Token 복구는 미구현 상태로 설명한다. Guest recovery를 추가하더라도 설치 UUID 자체를 credential로 승격하지 않는 별도 인증·멱등성 설계가 필요하다. Jira 변경과 Git commit·push는 수행하지 않았다.
- 위험 요소: commit 성공과 클라이언트 수신 실패를 구분할 수 없는 ambiguous outcome에서 현재 클라이언트는 Guest 계정과 Token을 잃을 수 있다. 단순히 installationId만으로 Token을 재발급하면 탈취된 설치 식별자가 인증 수단이 되며, Refresh Token hash에서는 원문 Token을 복원할 수 없다.
- 다음 작업: 제품에서 응답 유실 복구가 필요한지 결정한다. 필요하다면 짧은 수명의 서버 발급 idempotency key·일회성 등록 challenge 또는 별도 복구 credential을 검토하고, 응답 재전송 시 Token 원문 보관 문제와 재사용·탈취 방어를 함께 설계한다.

## 2026-08-13 — TMI-89 UserAccountType 호환 expand 구현

<!-- codex-turn:019ff960-a869-79f2-86cd-5f6ab8d4c5a9 -->

- 날짜: 2026-08-13
- Jira: TMI-89
- 브랜치: `feat/TMI-89-user-account-type` (`94d0e61` 기준, commit·push 미수행)
- 작업 목표: 소셜 로그인 API 전에 User의 Guest·정식 회원 구분을 로그인 수단에서 분리하고, 기존 Mongo 문서와 클라이언트를 깨뜨리지 않는 UserAccountType expand·dual write·legacy read 기반을 Jira 완료 조건대로 구현한다.
- 변경 파일: `src/main/java/web/tosunsaeng/identity/domain/user/domain/enums/UserAccountType.java`, `src/main/java/web/tosunsaeng/identity/domain/user/domain/entity/User.java`, `src/main/java/web/tosunsaeng/identity/domain/user/dto/response/UserProfileResponse.java`, `src/main/java/web/tosunsaeng/identity/domain/user/application/UserWithdrawalService.java`, `src/main/java/web/tosunsaeng/identity/domain/user/application/UserConsentService.java`, `src/main/java/web/tosunsaeng/identity/domain/auth/application/SignupService.java`, `src/main/java/web/tosunsaeng/identity/domain/auth/application/GuestAuthService.java`, `src/main/java/web/tosunsaeng/identity/domain/auth/application/LoginService.java`를 변경했다.
- 변경 파일: `src/test/java/web/tosunsaeng/identity/domain/user/domain/UserAccountTypeCompatibilityTests.java`, `src/test/java/web/tosunsaeng/identity/domain/user/domain/UserFactoryTests.java`, `src/test/java/web/tosunsaeng/identity/domain/user/domain/UserWithdrawalDomainTests.java`, `src/test/java/web/tosunsaeng/identity/domain/user/application/UserProfileServiceTests.java`, `src/test/java/web/tosunsaeng/identity/domain/user/application/UserWithdrawalServiceTests.java`, `src/test/java/web/tosunsaeng/identity/domain/user/application/UserConsentServiceTests.java`, `src/test/java/web/tosunsaeng/identity/domain/user/api/UserControllerTests.java`, `src/test/java/web/tosunsaeng/identity/domain/auth/application/AuthenticationUseCaseServicesTests.java`, `src/test/java/web/tosunsaeng/identity/domain/auth/application/GuestAuthServiceTests.java`, `src/test/java/web/tosunsaeng/identity/domain/auth/application/GuestRefreshLifecycleTests.java`, `src/test/java/web/tosunsaeng/identity/global/config/SecurityIntegrationTests.java`, `src/test/java/web/tosunsaeng/identity/IdentityApplicationTests.java`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 변경했다. WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: `UserAccountType(GUEST, MEMBER)`과 User의 `accountType` 필드를 추가했다. 신규 LOCAL 회원은 `MEMBER + LOCAL`, 신규 Guest는 `GUEST + GUEST`를 dual write한다. 명시 accountType이 있으면 우선하고, 누락된 legacy 문서는 provider GUEST만 GUEST, LOCAL 또는 null은 MEMBER로 호환 해석한다.
- 구현 내용: `isGuest()`, `isMember()`, `hasLocalCredential()`을 추가해 계정 유형과 비밀번호 보유 여부를 분리했다. MEMBER 자체는 passwordHash나 SocialIdentity를 강제하지 않아 social-only MEMBER를 허용하고, 기존 LOCAL factory는 email·normalizedEmail·passwordHash 세 필드를 계속 필수로 검증하며 partial credential 조합을 거절한다.
- 구현 내용: `GET /api/v1/users/me`에 accountType을 추가하고 기존 provider는 OpenAPI deprecated 하위 호환 필드로 유지했다. Guest와 LOCAL 프로필 응답, enum 값과 deprecated schema를 Controller·Security·OpenAPI 테스트로 고정했다.
- 구현 내용: 회원 탈퇴는 provider enum 비교 대신 Guest 여부와 LOCAL credential 보유 여부를 각각 판단한다. Guest는 비밀번호 없는 기존 탈퇴를 유지하고 LOCAL credential MEMBER만 비밀번호를 검증하며, social-only MEMBER는 LOCAL 비밀번호 경로로 오분류하지 않고 현재 미구현 재인증 경계에서 안전하게 거절한다. tombstone은 원래 accountType을 보존한다.
- 구현 내용: 회원가입·로그인 완료 로그에는 accountType과 LOCAL provider를 구분해 남기고, Guest 생성·동의·탈퇴처럼 계정 유형만 필요한 이벤트는 provider 대신 accountType을 기록했다. Token·Hash·자격증명·요청 본문과 개인정보는 로그에 추가하지 않았다.
- 수행한 Jira 작업: 구현 전 Atlassian 공식 MCP로 `TMI-89` 설명·완료 조건·상태를 다시 읽었고 AGENTS.md 및 계약과 충돌이 없음을 확인했다. 이번 구현 turn에는 Jira 댓글·상태·담당자·우선순위와 다른 필드를 변경하지 않았다.
- 추가한 댓글의 목적: 종료 댓글은 등록하지 않았다. 사용자 승인을 받기 전에는 구현 요약·테스트·운영 위험 댓글을 자동 등록하지 않는다.
- 변경한 상태: Jira `TMI-89`은 `해야 할 일`을 유지했다.
- 승인 여부: 사용자가 Jira 생성 후 구현을 명시적으로 요청했다. Jira 댓글이나 상태 전환은 별도로 승인하지 않았다.
- 실행한 테스트와 결과: 첫 관련 테스트 144개 중 새 social-only MEMBER 테스트가 Mockito 중첩 stubbing 문제로 1개 실패해 helper에서 mock userId를 먼저 확정하도록 테스트만 수정했고, 동일 관련 범위 재실행이 성공했다. 추가 focused UserAccountType·프로필·탈퇴 테스트도 성공했다.
- 실행한 테스트와 결과: 첫 `./gradlew clean test`는 샌드박스의 사용자 Gradle cache lock 접근 제한으로 코드 실행 전에 중단됐다. 승인된 동일 명령을 샌드박스 밖에서 재실행해 45개 suite·317개 테스트가 skip 0·failure 0·error 0으로 성공했다. 실제 Atlas와 외부 OAuth Provider는 호출하지 않았다.
- 유지한 계약: 실제 userId와 JWT `sub`는 canonical UUID 문자열이다. `UserProvider`에 GOOGLE·KAKAO·APPLE을 추가하지 않았고 SocialIdentity는 외부 identity 매핑만 소유한다. RS256·kid·issuer·audience·JWKS, 기존 Access/Refresh Token 외부 계약, Identity/Learning Core와 Python AI `user_id=examId` 경계를 변경하지 않았다. Secret·Token·Password·실제 Key·전체 MongoDB URI·사용자 개인정보를 기록하지 않았다.
- 결정사항: 이번 PR은 expand 단계이므로 UserProvider와 저장 provider, 프로필 provider를 제거하거나 rename하지 않고 backfill도 수행하지 않는다. accountType은 계정 유형 기준이고 provider·credential은 로그인 수단 기준이며, accountType 존재 시 provider보다 우선한다.
- 위험 요소: 실제 운영 User의 provider·accountType·credential 필드 분포를 조회하지 않았으므로 배포 전에 read-only aggregate로 provider null, partial LOCAL credential, provider/accountType 불일치와 예상 밖 값을 확인해야 한다. 향후 Guest 승격 뒤 legacy provider가 GUEST로 남을 수 있으므로 신규 소비자가 deprecated provider로 MEMBER 여부나 비밀번호 보유를 추론하면 안 된다.
- 다음 작업: 사용자가 변경을 검토한 뒤 직접 commit·push한다. Jira 종료 댓글 초안을 먼저 제시하고 별도 승인 전에는 등록하거나 상태를 변경하지 않는다. 배포 전에 운영 legacy User aggregate를 확인하고, 다음 별도 범위에서 PhoneIdentity·OTP 또는 SocialLoginChallenge·Provider verifier 구현 순서를 확정한다.

## 2026-08-13 — TMI-89 병합 확인 및 Jira 종료 변경 사전 검토

<!-- codex-turn:019ff97e-4317-7841-9dfe-14cd8f4da02c -->

- 날짜: 2026-08-13
- Jira: TMI-89
- 브랜치: `develop` (`10b1fa0`, `origin/develop`과 일치, Codex commit·push 미수행)
- 작업 목표: Jira `TMI-89` 종료 요청에 앞서 이슈 현재 상태, 사용 가능한 완료 전환과 구현 PR의 `develop` 병합을 확인하고, 실제 Jira 변경 내용을 사용자에게 먼저 제시할 준비를 한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 로컬 Git 이력에서 GitHub PR #17의 merge commit `10b1fa0ac23015191ec41fb62b7e9f54f4cedc28`을 확인했다. 현재 브랜치와 `origin/develop`이 해당 commit을 가리키며 TMI-89 구현 파일은 `develop`과 차이가 없어 병합 선행조건을 충족한다.
- 수행한 Jira 작업: Atlassian 공식 MCP로 `TMI-89`을 다시 읽어 현재 상태 `해야 할 일`, Resolution 없음, 기존 댓글 없음과 연결된 remote link 없음을 확인했다. 사용 가능한 `완료` transition ID `41`도 확인했지만 읽기 전용 조회만 수행했다.
- 추가한 댓글의 목적: UserAccountType·legacy fallback·프로필 하위 호환·탈퇴 분리의 구현 요약, 변경 파일, 전체 테스트 결과와 운영 legacy 데이터 점검 위험을 알리는 종료 댓글을 준비한다. 이번 turn에는 등록하지 않았다.
- 변경한 상태: 상태 전환을 수행하지 않아 `해야 할 일`을 유지했다.
- 승인 여부: 사용자가 Jira 종료를 요청했지만 저장소 규칙에 따라 댓글 전문과 정확한 상태 전환을 먼저 제시한 뒤 최종 승인을 받아야 하므로 외부 변경은 보류했다.
- 실행한 테스트와 결과: 애플리케이션 코드 변경이 없는 Jira·Git 병합 확인 작업이므로 Gradle 테스트를 다시 실행하지 않았다. 병합된 구현의 기존 검증 결과는 45개 suite·317개 테스트, skip 0·failure 0·error 0이다. 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 WORKLOG 끝 append를 확인한다.
- 유지한 계약: Jira 종료 판단 전에 PR 병합을 확인했고 Identity/Learning Core 경계, canonical UUID userId와 JWT `sub`, 기존 RS256·JWKS·Access/Refresh 계약을 변경하지 않았다. Jira와 기록에 Secret·Token·Password·실제 Key·전체 MongoDB URI·사용자 개인정보를 기록하지 않았다.
- 결정사항: 사용자에게 종료 댓글 전문과 `해야 할 일 → 완료` transition ID `41` 적용을 명시적으로 보여준 뒤 승인받은 경우에만 댓글 등록과 상태 전환을 수행한다. 다른 Jira 필드는 수정하지 않는다.
- 위험 요소: 운영 User 문서의 `provider=null`, partial LOCAL credential, provider/accountType 불일치와 예상 밖 값은 아직 실제 환경에서 확인하지 않았다. 배포 전 read-only aggregate가 필요하다.
- 다음 작업: 사용자가 제시된 Jira 댓글과 상태 전환을 승인하면 댓글을 한 번 등록하고 transition ID `41`만 적용한 뒤 `완료` 상태와 Resolution을 재조회한다. 그 결과를 CURRENT_STATE와 새 WORKLOG 항목에 기록한다.

## 2026-08-13 — TMI-89 Jira 완료 및 다음 PhoneIdentity 기반 범위 확정

<!-- codex-turn:019ff981-64f7-70c0-a2f1-f148d9d23ccd -->

- 날짜: 2026-08-13
- Jira: TMI-89
- 브랜치: `develop` (`10b1fa0`, `origin/develop`과 일치, Codex commit·push 미수행)
- 작업 목표: 사용자가 승인한 TMI-89 종료 댓글과 완료 전환을 적용·검증하고, 병합된 SocialIdentity·UserAccountType 다음의 즉시 구현 범위를 의존성과 PR 위험에 맞게 정리한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 다음 계획 단계는 PhoneIdentity/OTP이지만 E.164 정규화, HMAC key lifecycle, Mongo unique 제약, 외부 SMS, 공개 API, abuse 방어와 회원가입 Transaction을 한 PR에 섞지 않도록 분할했다. 즉시 범위 3A는 PhoneIdentity·PhoneFingerprintAlias와 versioned domain-separated HMAC fingerprint 데이터·보안 기반으로 제한한다.
- 구현 내용: 3A에는 서버 E.164 정규화 경계, 정확히 하나의 ACTIVE_WRITE와 복수 LOOKUP_ONLY를 지원하는 key registry 검증, retained version fingerprint candidate 생성, PhoneIdentity의 userId unique, PhoneFingerprintAlias의 `(fingerprintKeyVersion, phoneFingerprint)` unique와 phoneIdentityId index, Repository·도메인·격리 Mongo index 테스트를 포함한다. raw 번호·HMAC key·fingerprint는 로그나 외부 응답에 노출하지 않는다.
- 구현 내용: 외부 SMS provider, PhoneVerificationAttempt, OTP 요청·확인 API, TTL·CAS·rate limit, LOCAL signup DTO 변경과 grant 소비 Transaction, Guest·social 가입, TrialClaim·UserEntitlement·시험 코드는 3A에서 제외한다. 이를 각각 후속 3B OTP verification과 3C LOCAL signup 연동으로 분리한다.
- 수행한 Jira 작업: Atlassian 공식 MCP로 TMI-89의 현재 `해야 할 일`, Resolution 없음, 빈 댓글과 transition ID `41` 사용 가능 여부를 재확인했다. 승인된 종료 댓글을 한 번 등록하고 transition ID `41`만 적용한 뒤 상태 ID `10003`과 Resolution `완료`를 재조회했다.
- 추가한 댓글의 목적: UserAccountType, 신규 User dual write, legacy provider fallback, 프로필 accountType·deprecated provider 호환, 탈퇴 분리, PR #17 병합, 변경 범위, 전체 테스트 결과와 운영 legacy 데이터 점검 위험을 인수인계한다. 등록된 댓글 ID는 `10003`이다.
- 변경한 상태: Jira `TMI-89`을 `해야 할 일`에서 `완료`로 변경했고 Resolution도 `완료`임을 확인했다. 담당자·우선순위·설명과 다른 필드는 변경하지 않았다.
- 승인 여부: 직전 turn에 정확한 댓글 전문과 `해야 할 일 → 완료` transition ID `41`을 제시했고 사용자가 `좋아`라고 응답한 승인을 적용했다. 다음 범위 설명 요청은 Jira 생성 승인이 아니므로 새 Jira는 생성하지 않았다.
- 실행한 테스트와 결과: 애플리케이션 코드 변경이 없는 Jira 종료·범위 분석 작업이므로 Gradle 테스트를 다시 실행하지 않았다. 병합된 TMI-89 구현은 기존 `./gradlew clean test`에서 45개 suite·317개 테스트, skip 0·failure 0·error 0으로 검증됐다. 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 WORKLOG 끝 append를 확인한다.
- 유지한 계약: 실제 userId와 JWT `sub`는 canonical UUID 문자열이며 전화번호는 로그인 ID나 자동 계정 병합 키가 아니다. User 객체 또는 `@DBRef` 대신 PhoneIdentity가 userId만 참조하고, Identity에 TrialClaim·UserEntitlement·시험 코드를 추가하지 않는다. Secret·Token·Password·raw 전화번호·OTP·HMAC key·fingerprint·실제 Key·전체 MongoDB URI·사용자 개인정보를 기록하지 않았다.
- 결정사항: 다음 즉시 PR은 `PhoneIdentity + versioned phone fingerprint 기반(3A)`이다. 가입 시 전화 인증이라는 제품 불변식은 유지하되 OTP 발송·확인과 signup Transaction은 기반 제약이 검증된 뒤 후속 PR에서 연결한다.
- 위험 요소: 지원 국가·mobile/VoIP 정책, 전화번호 library, HMAC key registry의 실제 version과 Secret 주입 방식, key reference count·비상 rotation runbook이 아직 운영값으로 확정되지 않았다. alias unique 제약과 mixed writer gate 없이 writer를 공개하면 version 교차 중복 연결이 가능하다.
- 다음 작업: 사용자가 3A 범위를 확인하면 Jira 생성 전에 제목·설명·완료 조건·테스트·제외 범위의 정확한 초안을 제시한다. 승인 후에만 Jira를 생성하고 구현 전 이슈를 다시 읽는다.

## 2026-08-13 — Prepared·IssuedRefreshSession 엔티티 관계 정정

<!-- codex-turn:019ff986-beed-79a0-b5b3-bf1f0ca53e8e -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`10b1fa0`, commit·push 미수행)
- 작업 목표: `PreparedRefreshSession`과 `IssuedRefreshSession` 중 어떤 객체가 실제 `RefreshSession` entity를 포함하는지에 대한 사용자 이해를 코드 기준으로 바로잡는다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: `PreparedRefreshSession(String tokenValue, RefreshSession session)`은 저장 전 wrapper지만 실제 `RefreshSession` entity 객체를 `session` component로 직접 포함한다고 확인했다. `session()` 접근자는 저장 예정 entity의 동일 객체 참조를 반환하며 `RefreshSessionIssuer.savePrepared`가 이를 Repository에 전달한다.
- 구현 내용: `IssuedRefreshSession(String tokenValue, Instant issuedAt, Instant expiresAt)`은 `RefreshSession` component가 전혀 없으며 entity를 가리키지 않는다. Repository `save`가 반환한 `savedSession`에서 `createdAt`과 `expiresAt` 값만 복사하고 prepared Token 원문과 함께 저장 후 application 결과로 만든다.
- 구현 내용: 두 record 모두 MongoDB entity 자체가 아니고 application 계층의 전달 객체다. 실제 DB 영속 대상은 `RefreshSession`이며 DB에는 Token hash와 Session 상태가 저장된다. `Prepared`는 entity를 포함한 저장 명령 재료, `Issued`는 entity 없이 응답에 필요한 값만 가진 저장 결과라는 방향으로 정리했다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석·설명 작업이므로 Gradle 테스트를 실행하지 않았다. 두 record와 `RefreshSessionIssuer.prepare`·`savePrepared` 구현을 읽어 확인했으며 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 WORKLOG 끝 append를 정적으로 검증한다.
- 유지한 계약: Refresh Token 원문은 application 내부 결과와 클라이언트 응답에만 사용하고 DB에는 hash만 저장한다. 실제 userId와 JWT `sub`, RS256·JWKS, Identity/Learning Core 경계를 변경하지 않았으며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 사용자의 표현은 반대로 정정한다. `PreparedRefreshSession`이 entity를 직접 포함하고 `IssuedRefreshSession`은 entity에서 복사한 scalar 값만 보유한다. 기존 코드·Jira를 변경하지 않았고 Git commit·push를 수행하지 않았다.
- 위험 요소: `PreparedRefreshSession` record 자체가 불변이어도 내부의 `RefreshSession` entity가 반드시 깊은 불변이라는 뜻은 아니다. 전달 중 entity mutation과 Token 원문 노출을 피해야 하며, `IssuedRefreshSession`을 entity 저장 성공의 증거로 사용할 때는 반드시 Repository 저장 후에만 생성하는 순서를 유지해야 한다.
- 다음 작업: Refresh Session 발급 흐름 변경 시 `prepare → repository.save(session) → issued result` 순서와 Token 원문 비저장, entity·결과 DTO 분리 테스트를 유지한다.

## 2026-08-13 — LoginService 인증·Token 발급 흐름 설명

<!-- codex-turn:019ff98e-5bc3-76a3-b9ea-632ab048d6b0 -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`10b1fa0`, commit·push 미수행)
- 작업 목표: `LoginService`의 LOCAL 이메일 로그인 검증 순서, 오류 비노출, 계정 상태 검사, Access·Refresh Token 발급과 성공 로그 및 현재 원자성 한계를 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: `LoginRequest`의 validation 이후 `EmailNormalizer`가 이메일을 trim·`Locale.ROOT` lowercase로 통일하고 `findByNormalizedEmail`로 LOCAL credential User를 찾는 흐름을 확인했다. 사용자 부재와 BCrypt 비밀번호 불일치는 모두 같은 `INVALID_CREDENTIALS` code·message로 반환해 어느 항목이 틀렸는지 외부에 구분하지 않는다.
- 구현 내용: 비밀번호 일치 후 `UserStatus.ACTIVE`만 로그인시키고 SUSPENDED·WITHDRAWN 등은 `ACCOUNT_NOT_ACTIVE`로 거절한다. 모든 인증·상태 검사가 끝난 뒤에만 `AccessTokenIssuer.issue(userId, Set.of())`로 빈 scope RS256 JWT를 만들고 `RefreshSessionIssuer.issue(userId)`로 원문 Refresh Token과 hash 기반 DB Session을 발급한다.
- 구현 내용: `AuthResponseConverter`는 Access Token, Refresh Token 원문, Bearer type과 Access Token TTL을 `LoginResponse`로 변환한다. 응답·request의 `toString()`은 credential과 Token을 redaction하고 성공 로그에는 event·outcome·canonical userId·accountType·provider만 남기며 이메일·비밀번호·Token을 기록하지 않는다.
- 구현 내용: `LoginService.login`에는 `@Transactional`이 없고 `RefreshSessionIssuer.issue`는 Session을 즉시 저장한다. 따라서 인증 실패 전에는 아무 Session도 만들지 않지만 Session 저장 성공 뒤 응답 변환이나 후속 코드가 실패해도 그 Session을 함께 rollback하는 원자성은 없다. Access Token은 DB 저장물이 아니며 호출자에게 응답되지 않으면 일반적으로 클라이언트가 사용할 수 없지만 이미 저장된 RefreshSession은 별도 정리 전까지 남을 수 있다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석·설명 작업이므로 Gradle 테스트를 실행하지 않았다. Login service·request/response·converter와 기존 정상 로그인, 동일 credential 오류, 비활성 계정 Token 미발급, Token redaction 테스트를 읽어 확인했으며 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 WORKLOG 끝 append를 정적으로 검증한다.
- 유지한 계약: 실제 userId와 JWT `sub`는 canonical UUID 문자열이고 클라이언트 userId를 사용하지 않는다. Refresh Token 원문은 응답에만 전달하고 DB에는 hash만 저장하며 이메일·Password·Access/Refresh Token을 로그에 남기지 않는다. RS256·kid·issuer·audience·JWKS, Identity/Learning Core 경계를 변경하지 않았고 Secret·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 이 서비스는 LOCAL credential 인증과 Token 발급을 조율하는 application service로 설명한다. 사용자 부재·잘못된 비밀번호의 외부 오류 통일과 검증 완료 후 Token 발급 순서를 보안 경계로 유지한다. 기존 코드·Jira를 변경하지 않았고 Git commit·push를 수행하지 않았다.
- 위험 요소: 사용자 부재 경로는 BCrypt 연산 없이 끝나므로 비밀번호 불일치 경로와 timing 차이가 있고 공개 로그인에는 rate limit·관측·필요 시 dummy hash 검증을 검토해야 한다. RefreshSession 저장 후 converter·로그 실패의 orphan Session 가능성과 로그인 동시 호출의 복수 활성 Session 허용 정책도 별도 검토 대상이다.
- 다음 작업: 로그인 정책 변경 시 credential 오류 통일, 비활성 계정 Token 미발급, canonical userId Token binding, Refresh Token 원문 비저장과 redacted logging 테스트를 유지한다. 운영 공개 전 rate limit과 timing 완화, Session 발급 후 실패의 보상 또는 좁은 Transaction 경계 필요성을 결정한다.

## 2026-08-13 — 실제 전화번호 인증 provider와 내부 처리 경계 설명

<!-- codex-turn:019ff98e-1910-77d3-9774-42d70f838325 -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`10b1fa0` 기준, Codex commit·push 미수행)
- 작업 목표: PhoneIdentity 기반만으로 실제 번호 소유 인증이 가능한지, Firebase 같은 외부 provider가 필요한 이유와 provider 성공 결과를 자체 회원가입에 안전하게 연결하는 방식을 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드와 소셜 로그인 전체 계획서는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: PhoneIdentity·HMAC fingerprint는 검증 완료 번호의 저장·중복 방지 모델일 뿐 소유 검증 수단이 아니므로 실제 SMS 발송과 OTP 확인에는 외부 verification provider 또는 SMS transport가 필요함을 명확히 했다. provider 선택은 아직 확정하지 않았다.
- 구현 내용: 현재처럼 Identity가 canonical User와 자체 JWT를 소유할 때는 서버가 provider의 start/check를 조율하는 managed verification 방식이 가장 단순한 기본 선택이다. Identity는 요청 번호를 E.164로 검증하고 rate limit 뒤 providerReference를 저장하며, provider가 승인한 경우에만 짧은 수명의 일회성 phoneVerificationGrant를 발급하고 DB에는 grant hash만 저장한다.
- 구현 내용: Firebase Phone Auth도 사용할 수 있지만 클라이언트가 Firebase SDK로 SMS·코드 확인을 마치고 Firebase ID token을 Identity로 보내는 client-oriented 흐름이다. Identity는 token의 서명·issuer·audience·만료, phone sign-in 수단과 phone number를 검증하고 기존 attempt의 fingerprint·purpose·binding과 일치할 때만 내부 grant로 교환한다. Firebase UID나 ID token은 내부 userId 또는 앱 Access Token으로 사용하지 않는다.
- 구현 내용: 최종 LOCAL signup Transaction은 `VERIFIED`, 미만료, binding·grant hash 일치 조건으로 attempt를 `CONSUMED`로 CAS 전이하고 User·PhoneIdentity·fingerprint aliases·RefreshSession을 함께 생성한다. confirm 성공만으로 PhoneIdentity를 만들지 않으며 가입 저장 실패 시 consume도 rollback한다. 이후 일반 로그인에는 OTP를 반복하지 않는다.
- 구현 내용: SMS transport만 사용하는 방식은 서버가 OTP 생성, keyed hash 저장, TTL, 실패 lock, resend 제한과 재사용 차단까지 직접 구현해야 하므로 managed verification보다 책임이 크다. Firebase는 client anti-abuse UX와 Firebase user lifecycle·중단 가입 고아 user·탈퇴 정리 부담이 추가된다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글은 추가하지 않았다.
- 변경한 상태: Jira 상태를 변경하지 않았다.
- 승인 여부: 사용자는 실제 전화번호 인증 처리 방식을 질문했으며 외부 provider 선정이나 Jira 생성·구현은 요청하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 설계 설명 작업이므로 Gradle 테스트를 실행하지 않았다. 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 WORKLOG 끝 append를 정적으로 확인한다.
- 유지한 계약: 실제 userId와 JWT `sub`는 Identity가 생성한 canonical UUID 문자열이다. 전화번호와 외부 provider UID를 로그인 ID나 자동 merge 키로 쓰지 않고 클라이언트가 보낸 userId를 받지 않는다. raw 번호·OTP·provider credential·Firebase ID token·내부 grant·HMAC key·fingerprint를 로그나 기록에 넣지 않으며 Identity/Learning Core 경계를 유지한다.
- 결정사항: 3A는 provider와 무관한 저장·rotation 기반이고 실제 사용자 인증 기능은 3B provider adapter/API와 3C signup grant 소비까지 완료돼야 동작한다. 기존 자체 Identity 구조에는 server-managed verification provider를 우선 추천하되 Firebase를 선택할 수 있으며, 3B Jira 전에 한 방식을 명시적으로 결정한다.
- 위험 요소: provider별 국내 번호 도달률·가격·발신 규제·국가와 line type 지원·fraud protection·테스트 환경·개인정보 처리 위치를 비교해야 한다. Firebase는 bearer ID token을 곧바로 일회성 proof로 간주하면 replay가 가능하므로 자체 attempt와 CAS consume이 반드시 필요하다.
- 다음 작업: provider 후보를 Firebase Phone Auth, managed Verify, 국내 SMS transport로 비교해 제품·클라이언트·백엔드 운영 기준으로 하나를 확정한다. 그 뒤 3A Jira와 별개로 3B의 provider-specific credential, API, TTL·rate limit·failure mapping 완료 조건을 작성한다.

## 2026-08-13 — Firebase 통합 SNS·전화 인증 broker 대안 검토

<!-- codex-turn:019ff99c-c909-7331-be99-e828b117e902 -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`10b1fa0` 기준, Codex commit·push 미수행)
- 작업 목표: Firebase를 전화번호 인증에 도입하면 Google·Apple 등 SNS 인증도 함께 통합할 수 있다는 관찰을 현재 Identity 아키텍처에 대입하고, 단순해지는 부분과 계속 자체 소유해야 하는 규칙을 구분한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드와 전체 구현 계획서는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Firebase Auth를 공통 authentication broker로 두면 클라이언트의 Google·Apple·전화번호 인증을 Firebase SDK로 통합하고 Identity는 Firebase ID Token 검증·교환이라는 하나의 정상 경계로 단순화할 수 있음을 확인했다. Firebase ID Token은 외부 credential일 뿐 Learning Core나 앱의 최종 Token으로 사용하지 않고 Identity가 자체 RS256 Access/Refresh Token을 발급한다.
- 구현 내용: Kakao는 Google·Apple과 동일한 기본 provider 경로로 단정할 수 없다. Firebase를 Identity Platform으로 확장한 generic OIDC가 Kakao의 issuer·redirect·Claim 계약과 맞는지 PoC하거나, Identity가 Kakao credential을 직접 검증해 stable Firebase UID의 Custom Token을 발급하는 별도 경로가 필요하다. 후자는 Kakao 검증 구현을 완전히 없애지 못한다.
- 구현 내용: Firebase가 인증 성공을 증명해도 canonical `User`, provider별 `SocialIdentity`, 검증 번호의 `PhoneIdentity`, Guest 승격·기존 계정 발견·merge 불변식, 필수 전화 인증, TrialClaim 연계와 자체 Session은 계속 Identity 소유다. Firebase UID나 email·phone을 내부 userId 또는 자동 merge 키로 사용하지 않는다.
- 구현 내용: 권장 Firebase broker 흐름은 `client Firebase 인증 → Firebase ID Token + 내부 flow attempt 제출 → Identity의 signature·issuer·audience·expiry·tenant·provider·binding 검증 → 기존/new identity 분기 → 자체 User·Session Transaction → 자체 JWT 반환`이다. Firebase UID를 provider subject와 별도로 저장할 broker mapping이 필요한지는 ADR에서 결정한다.
- 구현 내용: Firebase를 phone에만 쓰고 SNS는 모두 직접 검증하는 혼합안은 Firebase user lifecycle과 직접 verifier 두 제어면을 동시에 유지한다. Firebase를 채택한다면 SNS·phone에 일관되게 적용하는 broker안이 더 자연스럽지만, 기존 LOCAL email/password까지 Firebase로 이전할지 병존할지에 따라 migration·탈퇴·account linking 비용이 크게 달라진다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글은 추가하지 않았다.
- 변경한 상태: Jira 상태를 변경하지 않았다.
- 승인 여부: 사용자는 Firebase 통합 가능성을 지적했으며 아키텍처 변경 확정, 외부 서비스 설정, Jira 생성이나 구현을 요청하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 아키텍처 검토이므로 Gradle 테스트를 실행하지 않았다. 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 WORKLOG 끝 append를 확인한다.
- 유지한 계약: 실제 userId와 JWT `sub`는 Identity의 canonical UUID 문자열이고 Firebase UID·provider subject·email·phone을 대체 userId로 쓰지 않는다. 클라이언트 userId를 신뢰하지 않고 Firebase ID Token·Provider credential·전화번호·OTP·HMAC key·fingerprint를 로그나 기록에 남기지 않는다. Identity/Learning Core 및 자체 RS256·JWKS 계약을 유지한다.
- 결정사항: 3A PhoneIdentity·fingerprint는 provider-neutral이므로 어느 안에서도 유효하다. 3B와 기존 단계 6 Provider verifier 구현 전에 `Firebase broker` 대 `Identity direct + managed phone verify` ADR과 최소 PoC를 먼저 수행하며, 아직 Firebase 채택을 확정하지 않는다.
- 위험 요소: Firebase의 account linking·email collision 정책을 그대로 canonical merge로 사용하면 현재 불변식과 충돌할 수 있다. phone credential을 social Firebase user에 link하지 않고 sign-in하면 별도 Firebase UID가 생길 수 있고, 가입 중단 user·탈퇴·unlink·revocation·기존 LOCAL migration과 Kakao Custom/OIDC 운영 경계가 추가된다.
- 다음 작업: Firebase PoC에서 Google·Apple·phone ID Token Claim과 linking 동작, Kakao generic OIDC 가능 여부, existing LOCAL 병존·이전, account deletion/revocation, 비용·quota·국내 SMS 도달률을 확인한다. 결과를 바탕으로 인증 broker ADR을 승인한 뒤 3B와 소셜 verifier Jira 범위를 다시 작성한다.

## 2026-08-13 — Firebase LOCAL 이메일·비밀번호 검증 이전 의미 설명

<!-- codex-turn:019ff9aa-e3a8-70f3-a928-92bac69ab91f -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`10b1fa0` 기준, Codex commit·push 미수행)
- 작업 목표: Firebase로 기존 이메일·비밀번호 인증까지 이전한다는 말이 로그인 검증 주체와 현재 Identity API·User·JWT에 어떤 변화를 뜻하는지 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드와 전체 구현 계획서는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 완전 이전안에서는 이메일·비밀번호 일치 검증을 Firebase Auth가 담당한다. 일반적인 client 흐름은 `signInWithEmailAndPassword` 성공으로 Firebase ID Token을 받은 뒤 Identity의 token exchange endpoint에 제출하고, Identity가 token 검증과 Firebase UID→canonical userId mapping을 거쳐 기존 자체 RS256 Access/Refresh Token을 발급하는 방식이다.
- 구현 내용: Firebase ID Token은 외부 인증 증명이고 앱·Learning Core용 최종 Token이 아니다. 기존 User document, UUID userId, accountType·status·consent·profile, PhoneIdentity·SocialIdentity와 RefreshSession은 Identity가 계속 소유한다. 로그인 UI는 유지할 수 있지만 Identity의 BCrypt `matches` 경로는 migration 완료 후 사용하지 않는다.
- 구현 내용: 기존 BCrypt 회원은 Firebase가 현재 지원하는 import 알고리즘·hash encoding·cost와 실제 저장값의 호환성을 Emulator 또는 격리 project에서 PoC해야 한다. 호환 bulk import가 가능하면 사용자는 기존 비밀번호를 그대로 쓰고, 불가능하면 기존 서버 첫 로그인 검증 후 Firebase credential을 만드는 lazy migration이나 비밀번호 재설정을 선택한다.
- 구현 내용: migration 전에 Firebase UID와 기존 canonical userId의 unique mapping을 생성해 신규 User 중복 생성을 막는다. 이관 성공이 확인된 Mongo passwordHash는 전환·rollback 정책이 끝난 뒤 제거하며 두 verifier를 무기한 병행하지 않는다. 비밀번호 원문은 migration을 이유로 저장·로그하지 않는다.
- 구현 내용: 신규 email/password 사용자가 가입 필수 전화 인증을 할 때는 phone credential을 별 Firebase 계정으로 sign-in하는 대신 같은 Firebase user에 명시적으로 link해야 한다. Firebase user 생성 후 내부 회원가입을 중단한 고아 계정 cleanup과 link collision 처리 정책이 필요하며, 같은 email·phone이라는 이유만으로 내부 User를 자동 merge하지 않는다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글은 추가하지 않았다.
- 변경한 상태: Jira 상태를 변경하지 않았다.
- 승인 여부: 사용자는 Firebase 완전 이전안의 이메일·비밀번호 검증 주체를 질문했으며 Firebase 채택, 외부 설정, 데이터 migration, Jira 생성이나 코드 구현을 승인하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 아키텍처 설명 작업이므로 Gradle 테스트를 실행하지 않았다. 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 WORKLOG 끝 append를 확인한다.
- 유지한 계약: 실제 userId와 JWT `sub`는 Identity의 canonical UUID 문자열이다. Firebase UID·email·phone을 내부 userId나 자동 merge 키로 사용하지 않고 Firebase ID Token·Password·Token·credential·실제 Key를 로그나 기록에 남기지 않는다. 자체 RS256·JWKS, Refresh Token 원문 비저장과 Identity/Learning Core 경계를 유지한다.
- 결정사항: `Firebase 완전 이전`은 Firebase가 LOCAL 비밀번호까지 검증하고 Identity는 Firebase 인증 결과를 자체 Session으로 교환하는 구조를 뜻한다. 아직 선택을 확정하지 않으며 direct Identity 인증안과 Firebase broker안의 ADR·migration PoC가 선행돼야 한다.
- 위험 요소: import hash 설정이 틀리면 기존 사용자가 같은 비밀번호로 로그인하지 못할 수 있다. lazy migration은 전환 기간 두 인증 경로와 동시 요청 race를 만들고, client Firebase 가입이 내부 phone·동의 완료보다 앞서면 고아 Firebase user가 생긴다. account enumeration·rate limit·password reset·email verification 정책도 Firebase와 앱 계약에 맞춰 재설계해야 한다.
- 다음 작업: 현재 BCrypt encoder 설정과 실제 hash 형식을 개인정보 없이 샘플링해 Firebase import 호환 PoC를 수행하고, Firebase UID mapping·token exchange·LOCAL fallback 종료 조건·phone link·탈퇴 cleanup을 ADR에 명시한 뒤 채택 여부를 결정한다.

## 2026-08-13 — 기존 회원 없는 조건의 Firebase·직접 인증 장단점 비교

<!-- codex-turn:019ff9b1-f400-7a43-b3b9-8d48c6b744f8 -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`10b1fa0` 기준, Codex commit·push 미수행)
- 작업 목표: 운영 기존 회원이 없어 credential migration이 필요하지 않은 조건에서 Firebase 통합 authentication broker안과 기존 Identity 직접 인증 계획의 장단점·적합성을 비교한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드와 전체 구현 계획서는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Firebase broker안은 email/password, Google, Apple과 phone credential의 client SDK·검증 경계를 통합하고 비밀번호 저장·reset·email verification·Provider token 검증·SMS OTP의 많은 보안 책임을 managed service에 위임한다. Identity는 Firebase ID Token exchange, canonical User, Guest 승격·merge·PhoneIdentity와 자체 Access/Refresh Token을 계속 소유한다.
- 구현 내용: Firebase안의 단점은 vendor·가격·quota·장애 종속, client SDK 결합, Firebase user와 내부 User의 이중 lifecycle, 가입 중단 고아 user, explicit credential linking·unlink/delete, token exchange·revocation 일관성이다. Kakao는 기본 Google·Apple 경로와 달라 generic OIDC PoC 또는 Kakao 직접 검증 후 Custom Token 구현이 남을 수 있다.
- 구현 내용: 기존 direct안은 User와 credential lifecycle을 한 서비스에서 완전히 통제하고 Firebase UID mapping·고아 Firebase user가 없으며 Kakao OIDC를 직접 다루기 쉽고 provider 교체·data residency·비용 제어가 상대적으로 좋다. 반면 Google·Kakao·Apple별 signature·issuer·audience·nonce/JWKS 검증, password reset·email verification, SMS provider·OTP 상태·rate limit·fraud 방어와 변경 대응을 모두 직접 구현·운영해야 한다.
- 구현 내용: 운영 기존 회원이 없으므로 Firebase 선택의 가장 큰 migration 위험인 BCrypt import·lazy migration·사용자 reset을 제거할 수 있다. 신규 가입부터 Firebase credential을 만들고 직접 passwordHash writer를 공개 전에 제거할 수 있지만, 실제 운영 User 0건은 read-only 확인해야 한다.
- 구현 내용: 이 조건에서는 Kakao 연동 PoC, 국내 SMS 도달률·비용, Firebase account linking 계약이 수용 가능하면 Firebase broker안이 MVP 개발 속도·보안 기본값·운영 부담 측면에서 우세하다고 판단했다. 장기적인 인증 플랫폼 자체 통제와 vendor 독립성이 핵심 사업 요구라면 direct안을 유지한다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글은 추가하지 않았다.
- 변경한 상태: Jira 상태를 변경하지 않았다.
- 승인 여부: 사용자는 기존 회원이 없다는 제품 상태를 제공하고 두 방식 비교를 요청했으며 Firebase 채택, 외부 설정, Jira 생성이나 코드 변경을 승인하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 아키텍처 비교이므로 Gradle 테스트를 실행하지 않았다. 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 WORKLOG 끝 append를 확인한다.
- 유지한 계약: Firebase 선택 여부와 무관하게 실제 userId와 JWT `sub`는 Identity canonical UUID 문자열이다. email·phone·Firebase UID·provider subject는 자동 merge 키가 아니고 Firebase ID Token을 Learning Core Token으로 사용하지 않는다. Credential·Password·Token·전화번호·OTP·실제 Key·전체 MongoDB URI를 로그나 기록에 남기지 않으며 Identity/Learning Core 경계를 유지한다.
- 결정사항: 현재 추천은 `Firebase 통합 broker + 내부 canonical User/JWT 유지`이며 확정은 아니다. PhoneIdentity·fingerprint 3A는 provider-neutral하게 진행할 수 있지만 3B와 소셜 verifier 전에 Firebase/Kakao/account-linking PoC와 ADR 승인이 필요하다.
- 위험 요소: Firebase의 자동 account linking 또는 email collision 동작을 내부 canonical merge로 오해하면 계정 탈취·오병합 위험이 있다. Firebase 장애·quota·가격 변경과 국내 SMS 성능, Kakao Custom/OIDC 복잡도, internal/Firebase lifecycle 불일치를 실제 PoC 없이 과소평가하면 안 된다.
- 다음 작업: 소규모 Firebase PoC로 email/password 가입·로그인, Google·Apple, 동일 Firebase user에 phone link, Kakao OIDC 또는 Custom Token, ID Token Claim·revocation·delete와 중단 가입 cleanup을 검증한다. 결과를 비교 ADR로 제시하고 승인 후 단계 3B 및 소셜 인증 Jira를 Firebase 기준으로 재작성한다.

## 2026-08-13 — Firebase 사용 시 내부 User·JWT 유지 이유 설명

<!-- codex-turn:019ff9b7-3d36-75d1-a86c-60bbae774cef -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`10b1fa0` 기준, Codex commit·push 미수행)
- 작업 목표: Firebase Auth를 사용하면서도 내부 canonical User와 자체 JWT를 유지한다고 제안한 이유를 각각 설명하고, Firebase만으로 대체 가능한 범위와 현재 계약상 비용을 구분한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드와 JWT 계약 문서는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 내부 User는 인증 credential 저장소가 아니라 토선생 서비스 계정과 데이터 소유자다. canonical UUID, GUEST/MEMBER, ACTIVE/SUSPENDED/WITHDRAWN/MERGED, 약관·프로필, Guest 승격·canonical merge, PhoneIdentity·SocialIdentity 연결과 downstream 데이터 ownership을 표현하므로 Firebase User record만으로 대체하지 않는다.
- 구현 내용: Firebase User는 UID와 email·phone·providerData·disabled 같은 인증 계정 정보를 제공하지만 서비스 계정 상태·약관 이력·merge target·혜택 정책의 영속 원장으로 사용하지 않는다. custom claims도 Token용 제한된 인가 데이터이지 자주 변경되는 계정 도메인 DB 대체물이 아니다.
- 구현 내용: 내부 JWT는 절대 필수는 아니다. Firebase UID를 canonical subject로 채택하고 Learning Core를 포함한 모든 backend가 Firebase ID Token의 issuer·project audience를 직접 검증하게 바꾸면 자체 Access/Refresh·JWKS를 제거할 수 있다. 이 경우 Firebase가 인증과 application session을 모두 소유한다.
- 구현 내용: 현재 저장소는 실제 userId를 UUID 문자열로 두고 JWT `sub`에 넣으며 Identity issuer, `tosunsaeng-learning-core` audience, RS256·kid·자체 JWKS와 RefreshSession을 계약으로 이미 구현했다. Firebase ID Token의 subject·issuer·audience는 이 계약과 다르므로 그대로 전달하면 Learning Core 계약과 canonical userId가 바뀐다.
- 구현 내용: 자체 token exchange를 유지하면 Firebase는 email/password·Google·Apple·phone credential의 진위만 증명하고, Identity가 Firebase UID를 canonical User에 매핑해 ACTIVE·merge·가입 완료 정책을 확인한 뒤 UUID sub·서비스 전용 audience·scope·TTL의 자체 Token을 발급한다. downstream은 Firebase SDK·project 변경과 분리된다.
- 구현 내용: 대가도 명시했다. Firebase session과 내부 RefreshSession의 이중 lifecycle, token exchange endpoint, 두 계정의 탈퇴·revocation 일관성이 추가된다. 기존 JWT 계약과 vendor 격리가 중요하지 않고 출시 단순성이 최우선이면 내부 User profile만 남기고 Firebase Token을 backend에서 직접 받는 전면 단순화안도 가능하지만 별도 계약 변경 작업이다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글은 추가하지 않았다.
- 변경한 상태: Jira 상태를 변경하지 않았다.
- 승인 여부: 사용자는 내부 User/JWT 유지 이유를 질문했으며 Firebase 채택, JWT 계약 변경, 외부 설정, Jira 생성이나 코드 변경을 승인하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 아키텍처 설명이므로 Gradle 테스트를 실행하지 않았다. 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 WORKLOG 끝 append를 확인한다.
- 유지한 계약: 현재 실제 userId와 JWT `sub`는 Identity canonical UUID이고 Access Token은 Identity issuer·Learning Core audience·RS256·kid·JWKS 계약을 유지한다. Firebase UID·email·phone을 자동 merge 키나 내부 userId로 사용하지 않고 Credential·Password·Token·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 현재 아키텍처 추천은 내부 User와 자체 JWT 모두 유지하는 Firebase credential broker 방식이다. 다만 내부 User 유지와 자체 JWT 유지는 서로 별개의 결정이며, 후자는 전 서비스 Firebase 직접 검증과 JWT 계약 재설계를 승인하면 제거할 수 있음을 명확히 한다.
- 위험 요소: 자체 exchange를 유지하면 이중 session의 로그아웃·탈퇴·revocation 경계가 복잡해진다. 반대로 Firebase Token 직접안을 선택하면 Firebase UID 안정성·vendor lock-in·project audience, service-specific scope와 내부 status 즉시 반영 문제를 해결하고 Learning Core를 동시 변경해야 한다.
- 다음 작업: Firebase ADR에서 `A. Firebase credential + 내부 User/JWT exchange`와 `B. Firebase canonical UID + backend 직접 검증`을 구현량·장애 경계·탈퇴·Guest·merge·Learning Core 계약으로 비교하고 하나를 명시적으로 승인한다. 현재 계약을 유지하는 동안은 A를 기준으로 후속 범위를 작성한다.

## 2026-08-13 — Firebase 인증·Identity 계정 책임 경계 확인

<!-- codex-turn:019ff9bd-a84a-7ff2-b497-ca3ab626914b -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`10b1fa0` 기준, Codex commit·push 미수행)
- 작업 목표: Firebase는 인증만 담당하고 그 외 토선생 계정·서비스 인증 책임은 Identity가 담당한다는 목표 구조를 확인하고 양쪽 책임을 명확히 구분한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드, JWT 계약 문서와 전체 구현 계획서는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Firebase 책임은 email/password credential 등록·검증, Google·Apple·phone과 검증 가능한 Kakao 경로의 외부 인증, password reset·email verification·SMS OTP 및 Firebase ID Token 발급으로 제한했다. Kakao의 실제 OIDC/Custom 경로는 PoC가 필요하다.
- 구현 내용: Identity는 Firebase ID Token의 signature·issuer·audience·expiry·provider·flow binding을 검증한 뒤 Firebase UID를 canonical UUID User에 매핑한다. User의 GUEST/MEMBER와 ACTIVE/SUSPENDED/WITHDRAWN/MERGED, 가입 완료·전화 인증·약관, 프로필, SocialIdentity·PhoneIdentity, Guest 승격·merge와 회원탈퇴를 소유한다.
- 구현 내용: Identity는 Firebase 인증 성공만으로 서비스 접근을 허용하지 않고 내부 User 상태와 가입 정책을 확인한 뒤 UUID userId를 `sub`로 하는 자체 RS256 Access Token과 hash 기반 RefreshSession을 발급한다. Firebase ID Token은 Learning Core에 직접 전달하지 않으며 Learning Core는 기존 Identity issuer·audience·JWKS 계약만 검증한다.
- 구현 내용: 실제 회원가입 흐름은 Firebase credential 인증이 먼저 끝나더라도 전화번호 검증·필수 동의와 Identity Transaction이 성공해야 내부 MEMBER가 된다. Firebase user만 있고 Identity User가 없는 중단 가입은 enrollment cleanup 대상으로 두고 Firebase email·phone 일치만으로 내부 계정을 자동 생성·병합하지 않는다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글은 추가하지 않았다.
- 변경한 상태: Jira 상태를 변경하지 않았다.
- 승인 여부: 사용자가 Firebase는 인증, 나머지는 Identity가 담당하는 구조를 확인했고 이에 대한 설명·기록만 수행했다. 외부 Firebase 설정, Jira 생성과 코드 구현은 승인받지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 책임 경계 확인 작업이므로 Gradle 테스트를 실행하지 않았다. 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 WORKLOG 끝 append를 확인한다.
- 유지한 계약: 실제 userId와 JWT `sub`는 Identity canonical UUID 문자열이며 Access Token은 Identity issuer·Learning Core audience·RS256·kid·JWKS 계약을 유지한다. Firebase UID·email·phone·provider subject는 내부 userId 또는 자동 merge 키가 아니며 Credential·Password·Token·전화번호·OTP·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 목표 아키텍처는 `Firebase credential authentication broker + Identity canonical account/session owner`다. Firebase Token 직접 backend 사용과 Firebase UID의 canonical userId 채택은 하지 않는다. PoC는 이 방향을 뒤집는 비교가 아니라 Kakao·phone linking·lifecycle의 실현 가능성을 검증한다.
- 위험 요소: Firebase와 Identity의 이중 lifecycle 때문에 중단 가입, unlink·탈퇴·disable, Firebase ID Token 유효 기간과 내부 RefreshSession 폐기의 일관성을 설계해야 한다. Firebase 인증 성공과 내부 MEMBER 생성을 동일 성공으로 오해하면 전화번호·동의가 없는 불완전 계정이 서비스에 접근할 수 있다.
- 다음 작업: 이 책임 경계를 ADR로 작성하고 Firebase PoC에서 email/password·Google·Apple·phone linking, Kakao OIDC/Custom, token exchange·revocation·delete와 중단 가입 cleanup을 검증한다. 이후 PhoneIdentity 3A와 Firebase 인증 adapter·enrollment 범위의 Jira 초안을 각각 제시한다.

## 2026-08-13 — LogoutAllService 전체 Session 폐기 흐름 설명

<!-- codex-turn:019ff9c7-2d19-75b1-8dc9-3f5341c64a67 -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`10b1fa0`, commit·push 미수행)
- 작업 목표: `LogoutAllService`가 인증된 현재 사용자의 활성 RefreshSession 전체를 조회·폐기하는 흐름, 멱등성, 구조화 로그와 현재 원자성·Access Token 한계를 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: `POST /api/v1/auth/logout-all`은 공개 endpoint가 아니라 JWT 인증이 필요한 보호 API이며 request body의 userId를 받지 않는다. `CurrentUserProvider.getCurrentUserId()`가 검증된 JWT `sub`를 반환하고 Repository는 그 userId와 `revokedAt=null` 조건으로 현재 사용자 소유의 미폐기 Session만 조회한다.
- 구현 내용: 활성 Session이 있으면 `Clock`에서 시각을 한 번 얻어 모든 Session의 `logoutAll` domain method에 동일하게 적용한다. 각 entity는 `lastUsedAt`과 `revokedAt`을 그 시각으로 바꾸고 `revocationReason=LOGOUT_ALL`, `replacedBySessionId=null`로 만든 뒤 Repository `saveAll`로 저장한다.
- 구현 내용: 활성 Session이 없으면 DB write 없이 정상 종료하고 debug 로그에 `no_active_sessions`와 0건을 남긴다. 첫 호출 이후 조회 조건에서 폐기 Session이 제외되므로 반복 호출도 성공하며 첫 요청만 저장하는 멱등 동작이다. 저장 성공 후 info 로그에는 `sessions_revoked`, canonical userId와 폐기 건수만 기록하고 Token hash·Token 원문은 기록하지 않는다.
- 구현 내용: 이 동작은 RefreshSession을 폐기해 해당 Refresh Token들의 이후 재발급을 거절하기 위한 것이며 이미 발급된 stateless Access Token을 즉시 폐기하지 않는다. 클라이언트는 로컬 Token을 삭제해야 하고 기존 Access Token은 최대 TTL까지 암호학적으로 유효할 수 있다. 또한 service에 `@Transactional`이 없어 여러 Session의 `saveAll`이 중간 실패할 경우 전부 또는 전무 원자성을 보장하지 않으며 Repository 오류는 성공으로 숨기지 않고 전파한다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석·설명 작업이므로 Gradle 테스트를 실행하지 않았다. service·RefreshSession domain method·Repository·Controller·Security 설정과 기존 사용자 격리, 동일 Clock·사유, 빈 목록·반복 호출, read/save 실패 전파, Token issuer 비의존 테스트를 읽어 확인했다. 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 WORKLOG 끝 append를 정적으로 검증한다.
- 유지한 계약: 대상 userId는 클라이언트 입력이 아니라 검증된 JWT `sub`의 canonical UUID이고 다른 사용자의 Session을 조회하거나 폐기하지 않는다. Access/Refresh Token과 hash를 로그·작업 기록에 남기지 않고 Refresh Token 원문 비저장, RS256·JWKS와 Identity/Learning Core 경계를 유지했다. Secret·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 전체 로그아웃은 현재 사용자의 미폐기 RefreshSession 전체에 대한 멱등 폐기 명령이며 Token 발급이나 User 상태 변경은 하지 않는다고 설명한다. 기존 코드·Jira를 변경하지 않았고 Git commit·push를 수행하지 않았다.
- 위험 요소: 여러 Session 저장의 비원자성 때문에 일부만 폐기될 수 있고, 조회와 저장 사이 새 Session 생성 또는 동시 logout/rotation이 발생하면 snapshot 밖 Session이나 optimistic lock 충돌이 생길 수 있다. stateless Access Token 즉시 폐기 요구가 생기면 denylist·token version·짧은 TTL 등 서비스 간 계약이 필요하다.
- 다음 작업: 전체 로그아웃 정책 변경 시 JWT subject 사용자 격리, 빈 목록·반복 호출 멱등성, 동일 시각·LOGOUT_ALL 사유와 민감정보 비로그 테스트를 유지한다. 필요하면 Mongo Transaction 또는 조건부 bulk update로 원자성·동시성을 보강하고 Access Token 즉시 폐기 요구를 별도 계약으로 결정한다.

## 2026-08-13 — Firebase 인증 broker 기준 전체 구현 계획 전환

<!-- codex-turn:019ff9c0-fc7d-7691-955a-630ec42e48bc -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`10b1fa0`, commit·push 미수행)
- 작업 목표: 합의한 `Firebase는 credential 인증만 담당하고 Identity는 canonical User·가입·계정 lifecycle·자체 JWT를 담당`하는 경계를 기준으로 기존 직접 Provider 검증·Identity OTP 중심 전체 계획을 개편한다.
- 변경 파일: `docs/social-login-implementation-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Firebase 책임을 email/password·Google·Apple·phone credential 인증과 ID Token 발급으로 고정했다. Kakao는 Generic OIDC PoC 통과 시 같은 경계에 포함하고, Custom Token 경로가 필요하면 별도 ADR로 분리한다.
- 구현 내용: Identity 책임으로 canonical UUID User, `(firebaseProjectId, firebaseUid)` 기반 FirebaseIdentity, SocialIdentity·PhoneIdentity, 필수 전화·동의가 포함된 가입, Guest 승격·merge, 상태·프로필·탈퇴, 자체 RS256 Access Token과 RefreshSession을 유지했다. Firebase UID·email·phone은 내부 userId 또는 자동 merge 키로 사용하지 않는다.
- 구현 내용: 앱 첫 진입의 Firebase 로그인·신규 가입과 기존 Guest 승격을 모두 포함했다. 미등록 Firebase UID는 User를 즉시 만들지 않고 TTL·binding·CAS를 가진 FirebaseEnrollmentAttempt로 보류하며, 동일 Firebase account의 verified phone과 필수 동의를 최종 Transaction에서 확인한다. phone-only Firebase 인증은 앱 로그인으로 거절한다.
- 구현 내용: 이전 기본 계획의 SocialTokenVerifier·SocialLoginChallenge·PhoneVerificationAttempt·Identity 자체 OTP를 제거 대상으로 명시했다. 구현 순서를 `Firebase ADR/PoC → broker foundation → PhoneIdentity → exchange/신규 가입 → Guest 승격 → merge/outbox → Kakao·Apple lifecycle → 별도 Entitlement/Learning Core 작업`으로 재정렬했다.
- 구현 내용: TrialClaim·UserEntitlement·EntitlementReservation과 무료시험 reserve/confirm은 Identity 밖의 Entitlement/Billing·Learning Core 책임으로 유지했다. 가입 시 전화 인증은 무료체험 지급이나 사용으로 처리하지 않는다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글은 추가하지 않았다.
- 변경한 상태: Jira 상태를 변경하지 않았다.
- 승인 여부: 사용자가 합의된 Firebase/Identity 책임 경계를 기준으로 계획 변경을 명시적으로 요청했다. 외부 Firebase project 설정, Jira 변경, 애플리케이션 구현, commit·push는 승인 범위에 포함하지 않았다.
- 실행한 테스트와 결과: 문서만 변경해 Gradle 테스트는 실행하지 않았다. `git diff --check`, 제거된 직접 verifier·OTP 계획의 잔존 검색, 지정 turn marker 단일 존재와 WORKLOG EOF append를 정적으로 검증한다.
- 유지한 계약: 실제 userId와 JWT `sub`는 Identity canonical UUID 문자열이고 Access Token은 Identity issuer·`tosunsaeng-learning-core` audience·RS256·`kid`·JWKS 계약을 유지한다. Firebase ID Token은 Learning Core로 전달하지 않으며 Identity가 시험·결과·Entitlement 데이터를 소유하거나 직접 수정하지 않는다. Credential·Password·Token·전화번호·OTP·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 목표 아키텍처는 `Firebase credential authentication broker + Identity canonical account/session owner`로 확정한다. Firebase UID를 canonical userId로 쓰거나 backend가 Firebase ID Token을 직접 받는 안, Firebase를 phone에만 사용하는 혼합안과 Identity의 직접 Provider/OTP 구현은 기본 계획에서 제외한다. 운영 기존 회원이 없다는 전제에 따라 BCrypt migration은 제외하되 cutover 전에 read-only로 재확인한다.
- 위험 요소: Firebase와 내부 User의 이중 lifecycle, 가입 중단 고아 Firebase user, ID Token replay·revoke·disabled 처리, Firebase 장애·quota·가격·vendor exit, Kakao Generic OIDC 지원 조건, 국내 SMS 도달률·비용, phone credential link collision과 Apple revoke lifecycle이 남아 있다.
- 다음 작업: production API를 바로 열지 않고 Firebase ADR과 격리 PoC로 email/password·Google·Apple·phone link·phone-only 거절·Kakao OIDC·ID Token 검증·탈퇴/revoke·중단 가입 cleanup을 확인한다. 승인된 결과로 Firebase broker foundation과 PhoneIdentity 구현 Jira를 각각 작성한다.

## 2026-08-13 — 전체 로그아웃 후 재로그인 Session 생성 설명

<!-- codex-turn:019ff9d2-7d51-7e91-9596-9f58519d21da -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`10b1fa0`, commit·push 미수행)
- 작업 목표: 전체 로그아웃으로 모든 기존 RefreshSession을 폐기한 뒤 사용자가 다시 로그인하면 Session이 재생성되는지와 기존 폐기 Session의 상태를 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: LOCAL 이메일·비밀번호와 ACTIVE 상태 검증에 다시 성공할 때마다 `LoginService`가 `RefreshSessionIssuer.issue(userId)`를 호출한다고 확인했다. issuer는 새 opaque Refresh Token 원문과 hash를 만들고 `RefreshSession.create`로 새 sessionId·rotationFamilyId, 발급·만료 시각과 `revokedAt=null`의 Session을 생성해 Repository에 저장한다.
- 구현 내용: 전체 로그아웃된 기존 Session의 `revokedAt`과 `LOGOUT_ALL` 사유를 제거하거나 같은 document를 재활성화하지 않는다. 기존 Session은 폐기 이력으로 남고 그 Refresh Token은 계속 거절되며, 재로그인에서 생성한 새로운 Refresh Token과 새 Session만 이후 재발급에 사용할 수 있다.
- 구현 내용: 사용자 계정 자체는 전체 로그아웃으로 비활성화되지 않으므로 ACTIVE 상태와 올바른 credential이 유지되면 재로그인이 가능하다. 로그인 횟수마다 별도 활성 Session을 허용하는 현재 구조라 여러 기기·반복 로그인은 복수 Session을 만들 수 있으며 사용자당 하나로 제한하는 정책은 없다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석·설명 작업이므로 Gradle 테스트를 실행하지 않았다. `LoginService`, `RefreshSessionIssuer.issue`와 `RefreshSession.create`를 읽어 새 Session 생성 경로를 확인했으며 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 WORKLOG 끝 append를 정적으로 검증한다.
- 유지한 계약: 새 Session도 동일 canonical userId에 결속되고 Access Token JWT `sub`에 서버 userId가 들어간다. Refresh Token 원문은 응답에만 전달하고 DB에는 새 hash만 저장하며 Token·hash·Password를 로그나 작업 기록에 남기지 않았다. RS256·JWKS와 Identity/Learning Core 경계를 유지했고 Secret·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 전체 로그아웃은 계정 잠금이나 향후 로그인 금지가 아니라 현재 존재하는 RefreshSession 집합의 폐기다. 재로그인은 기존 Session 부활이 아니라 완전히 새 Session 생성으로 설명한다. 기존 코드·Jira를 변경하지 않았고 Git commit·push를 수행하지 않았다.
- 위험 요소: logout-all 조회와 저장 사이 또는 직후에 동시 로그인이 성공하면 새 Session은 최초 조회 목록에 없어 살아남을 수 있다. 모든 기기 접근을 강하게 차단해야 한다면 계정 단위 session generation/version, write fencing 또는 Transaction과 동시성 계약이 필요하다.
- 다음 작업: 제품 정책상 전체 로그아웃과 동시 재로그인의 허용 여부, 사용자당 활성 Session 수와 기기 목록·개별 폐기 필요성을 확정한다. 변경 시 새 Session 생성, 기존 Session 비재활성화와 Refresh Token hash 비저장 계약 테스트를 유지한다.

## 2026-08-13 — Firebase broker 기반 소셜 로그인 전체 계획 설명

<!-- codex-turn:019ff9f3-a481-7023-b814-b7d1dfcb544c -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`10b1fa0`, commit·push 미수행)
- 작업 목표: 기존 경로에서 `docs/contracts/social-login-implementation-plan.md`로 이동된 21개 section·전체 계획을 읽고 목표 아키텍처, 사용자 흐름, 데이터 모델, 보안·Transaction·merge·혜택 경계, 구현 순서와 현재 완료 상태를 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 계획서와 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Firebase에는 email/password·Google·Apple·phone credential 인증과 Kakao Generic OIDC PoC 통과 시 해당 인증만 위임하고, Identity가 Firebase ID Token을 목적별로 검증·교환해 canonical UUID User와 자체 RS256 Access Token·RefreshSession을 계속 소유하는 핵심 경계를 정리했다. Firebase UID·provider subject·email·phone은 userId나 자동 merge 키가 아니며 Learning Core는 Firebase Token이 아니라 Identity JWT만 받는다.
- 구현 내용: 기존 MEMBER는 FirebaseIdentity 조회 후 내부 Token을 발급하고, 신규 사용자는 Firebase 인증 성공만으로 User를 만들지 않고 reusable PENDING FirebaseEnrollmentAttempt를 받는다. 같은 Firebase UID에 phone credential을 link하고 fresh Token·검증 번호·필수 동의를 제출한 finalize Transaction에서 User·FirebaseIdentity·PhoneIdentity·aliases·generic eligibility binding outbox·SocialIdentity·RefreshSession·attempt consume을 함께 확정한다.
- 구현 내용: Guest 승격은 기존 Guest canonical userId를 유지하며 identity와 새 MEMBER Session을 붙이고 old Guest Session을 폐기한다. 기존 FirebaseIdentity 또는 SocialIdentity owner가 있으면 단계 7 전에는 mutation 없는 `MERGE_REQUIRED`이고, merge 활성화 후에도 source ACTIVE GUEST·target ACTIVE MEMBER와 Guest JWT·target Firebase proof를 모두 요구한다. source는 MERGED·Session 폐기·outbox 처리되며 source JWT를 target 권한으로 승격하지 않는다.
- 구현 내용: Firebase adapter는 SDK를 interface 뒤에 격리해 signature·algorithm·project issuer/audience·tenant·시간·UID·provider·disabled/revoked·recent-auth·Admin providerData/phone을 목적별로 검증한다. high-risk와 login exchange만 필요한 Firebase remote 검사를 하고 내부 RefreshSession reissue와 Learning Core 일반 요청에는 Firebase 호출을 추가하지 않는다. Kakao는 Generic OIDC·billing·redirect·stable UID PoC 통과 전 feature off이며 실패 시 직접 verifier로 임의 회귀하지 않는다.
- 구현 내용: verified phone은 같은 Firebase UID에 link된 가입 proof이고 phone-only Identity login·자동 merge는 금지한다. Identity는 E.164를 재검증해 versioned domain-separated HMAC fingerprint와 aliases만 저장하고 한 번호의 ACTIVE MEMBER 귀속을 하나로 제한한다. 무료시험은 별도 Entitlement/Billing이 benefit 전용 별도 key/domain fingerprint, TrialClaim·UserEntitlement·reservation을 소유하며 Identity에는 시험 entity를 추가하지 않는다.
- 구현 내용: Firebase와 Mongo는 분산 Transaction이 아니므로 중단 Firebase user·phone 점유의 resume/cleanup과 lifecycle outbox가 필요하다. 탈퇴·unlink·Apple revoke도 내부 tombstone·Session 폐기와 Firebase revoke/delete 사이의 saga/retry 정책을 요구한다. 민감 Token·UID·provider subject·email·phone·OTP·fingerprint는 로그·Sentry·metric에서 금지하고 안정적인 오류 code와 낮은 cardinality 관측만 허용한다.
- 구현 내용: 단계 1 SocialIdentity(TMI-88·PR #16)와 단계 2 UserAccountType(TMI-89·PR #17)는 완료됐지만 Firebase 설정·adapter·FirebaseIdentity·Enrollment·PhoneIdentity·공개 API는 미구현이다. 즉시 작업은 production 연동이 아닌 단계 0 Firebase ADR·격리 PoC이며 이후 단계 3 broker foundation, 4 PhoneIdentity, 5 exchange/signup, 6 Guest 승격, 7 merge, 8 provider lifecycle 순서다. Entitlement binding과 수신 계약은 별도 서비스에서 단계 5 production signup의 배포 gate로 선행해야 한다.
- 실행한 테스트와 결과: 코드 변경이 없는 계획서 읽기·설명 작업이므로 Gradle 테스트를 실행하지 않았다. 1,442줄 전체와 21개 section을 읽고 현재 경로 이동 상태를 확인했으며 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 WORKLOG 끝 append를 정적으로 검증한다.
- 유지한 계약: 실제 userId와 JWT `sub`는 Identity canonical UUID 문자열이고 Firebase UID·email·phone·provider subject를 대체 식별자로 사용하지 않는다. RS256·kid·issuer·audience·JWKS, Refresh Token 원문 비저장, Identity/Learning Core 경계와 Python AI `user_id=examId`를 유지했다. Secret·Token·Password·실제 Key·전체 MongoDB URI·사용자 개인정보를 기록하지 않았다.
- 결정사항: 이 문서는 현재 동작 명세가 아니라 Firebase broker 전환의 목표 계약과 단계별 실행 계획으로 설명한다. 이전 direct provider verifier·SocialLoginChallenge·Identity OTP 구현은 기본 대상에서 제거되고 Firebase adapter·FirebaseEnrollmentAttempt가 대체한다. 계획서나 Jira를 변경하지 않았고 Git commit·push를 수행하지 않았다.
- 위험 요소: 단계 0에서 Firebase Identity Platform·billing·Kakao OIDC·모바일 redirect·국내 SMS·phone link collision·revoke/recent-auth·중단 가입 cleanup이 검증되지 않았다. Firebase/Mongo 이중 lifecycle, benefit binding outbox 미준비, merge consumer/source token gate 미준비 상태에서 후속 feature를 열면 고아 계정·번호 점유·무료혜택 중복·권한 혼합 위험이 있다.
- 다음 작업: 격리 Firebase project 또는 emulator로 단계 0 ADR·PoC를 수행하고 승인된 결과로 단계 3 Firebase broker foundation과 단계 4 PhoneIdentity Jira를 각각 작성한다. production Secret·공개 API·Guest merge·Entitlement 구현은 PoC와 선행 계약 완료 전에 활성화하지 않는다.

## 2026-08-13 — Firebase 계획 P0 보강 검토 반영

<!-- codex-turn:019ff9e3-c370-7071-a879-01ffc81130cf -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`10b1fa0`, commit·push 미수행)
- 작업 목표: Firebase 인증 broker 기반 전체 계획에 전화번호 uniqueness, 동일 UID phone link, 여러 기기 Guest merge proof, enrollment 중복 방지, 목적별 revoke, Kakao Identity Platform PoC와 혜택 fingerprint 분리 검토를 반영한다.
- 변경 파일: `docs/contracts/social-login-implementation-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다. 계획 문서의 기존 작업 트리 이동·staging 상태는 변경하지 않았다.
- 구현 내용: 검증된 번호는 동시에 하나의 ACTIVE MEMBER 가입 자격에만 귀속하며 다른 Firebase User 또는 PhoneIdentity가 소유하면 `PHONE_ALREADY_LINKED`로 거절하는 정책을 고정했다. phone은 canonical userId·로그인 ID·자동 merge 키가 아니며 SUSPENDED 계정은 점유를 유지하고 WITHDRAWN cleanup 뒤에만 해제한다.
- 구현 내용: email·Google·Apple·Kakao로 먼저 인증한 현재 Firebase User에 phone credential을 link하고 link 전후 UID를 확인하도록 했다. phone credential 직접 sign-in으로 별도 Firebase User를 만드는 흐름을 금지하고, 중단 가입 user가 점유한 번호는 resume 유예 후 안전한 unlink/delete cleanup 대상으로 정의했다.
- 구현 내용: 여러 기기 Guest merge는 source Guest의 Identity JWT와 기존 SNS owner Firebase User의 fresh ID Token을 동시에 검증한다. credential ownership 충돌 시 임시 User에 link하지 않고 기존 owner로 Firebase sign-in하며 email·phone으로 target을 추정하지 않는다.
- 구현 내용: 같은 Firebase project·UID·binding의 PENDING FirebaseEnrollmentAttempt를 partial unique index로 하나만 허용한다. 반복 exchange는 유효 enrollmentId를 재사용하고 application `expiresAt`, 만료 CAS와 duplicate winner 재조회로 동시성을 처리하며 TTL은 cleanup에만 사용한다.
- 구현 내용: Firebase login exchange와 signup·upgrade·merge·link/unlink·withdrawal은 목적별 revoke·disabled·recent-auth 정책을 적용하고 Learning Core 일반 요청과 Identity RefreshSession reissue는 Firebase를 호출하지 않는다. Kakao PoC에는 Identity Platform 업그레이드·billing·OIDC provider 등록과 모바일 redirect·deep-link를 추가했다.
- 구현 내용: PhoneIdentity와 FREE_MOCK_EXAM fingerprint를 별도 key·version·domain으로 파생한다. 가입·phone 교체 시 raw phone 없는 consumer-scoped binding outbox를 전달하고 Entitlement가 멱등 binding만 저장하며, TrialClaim·UserEntitlement는 첫 무료시험 요청에서만 생성하는 계약을 추가했다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글은 추가하지 않았다.
- 변경한 상태: Jira 상태를 변경하지 않았다.
- 승인 여부: 사용자가 제공한 검토 내용을 확인하고 계획에 필요한 부분을 추가하도록 명시적으로 요청했다. 외부 Firebase·Identity Platform 설정, Jira 변경, 애플리케이션 구현과 commit·push는 승인 범위에 포함하지 않았다.
- 실행한 테스트와 결과: 문서만 변경했으므로 Gradle 테스트는 실행하지 않았다. `git diff --check`, 계획 문서 heading·중복 줄, 핵심 계약 키워드, 지정 turn marker 단일 존재와 WORKLOG EOF append를 정적으로 검증한다.
- 유지한 계약: 실제 userId와 JWT `sub`는 Identity canonical UUID이며 자체 RS256·`kid`·issuer·`tosunsaeng-learning-core` audience·JWKS와 Refresh Token 원문 비저장 계약을 유지했다. Firebase UID·email·phone은 자동 merge 키가 아니고 Firebase ID Token은 Learning Core로 전달하지 않는다. Identity에 시험·TrialClaim·UserEntitlement 코드를 추가하지 않았고 Credential·Password·Token·전화번호·OTP·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 현재 설계와 Firebase phone uniqueness에 맞춰 `전화번호당 MEMBER 가입 계정 1개` 정책을 채택한다. 같은 번호 충돌은 자동 merge가 아닌 명시적 거절·복구 대상이다. 기본 phone 방식은 동일 Firebase UID credential link이며 phone MFA는 별도 ADR 없이는 범위에 포함하지 않는다.
- 위험 요소: 가족 공용·재할당 번호의 가입 거절, abandoned Firebase user의 번호 점유, cleanup 오판, partial unique 만료 race, Firebase revoke remote call 비용·장애, Identity Platform 비용·모바일 redirect, benefit binding의 pseudonymous data 보존·outbox 지연이 남아 있다.
- 다음 작업: Stage 0 ADR·격리 PoC에서 동일 UID phone link·충돌·cleanup, 목적별 Token 검증, active enrollment 재사용, 여러 기기 Guest proof, Identity Platform Kakao OIDC와 국내 SMS 조건을 검증한다. 승인 후 Firebase broker foundation과 PhoneIdentity를 별도 Jira 범위로 작성한다.

## 2026-08-13 — 무료 모의고사 1회 확인 엔티티 존속·역할 설명

<!-- codex-turn:019ffa08-de7f-7921-80fd-562d4a460710 -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`10b1fa0`, commit·push 미수행)
- 작업 목표: Firebase broker 기반 계획에서 전화번호당 무료 모의고사 1회를 확인하던 엔티티가 제거됐는지 확인하고 현재 모델별 역할·생성 시점·소유 서비스를 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 계획서와 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: `TrialClaim`은 제거되지 않았으며 별도 Entitlement/Billing 서비스가 `benefitType + fingerprintKeyVersion + benefitPhoneFingerprint` 기준으로 검증 번호별 혜택 지급 이력을 보존하는 최종 중복 방지 ledger라고 확인했다. 동일 번호가 다른 User로 재가입해도 기존 Claim이 있으면 무료 1회를 다시 지급하지 않는 역할이다.
- 구현 내용: `VerifiedPhoneBenefitBinding`은 무료혜택 지급 이력이 아니라 가입·phone 교체 시 검증된 번호에서 미리 파생한 benefit-scoped fingerprint candidate를 userId에 연결해 두는 사전 증명이다. Identity가 raw phone이나 PhoneIdentity fingerprint를 공유하지 않고 generic `PhoneEligibilityBindingOutbox`를 저장·전달하면 Entitlement가 eventId 멱등으로 binding을 보관한다.
- 구현 내용: 회원가입이나 Firebase phone 인증 시점에는 `TrialClaim`과 `UserEntitlement`를 만들지 않는다. 사용자가 처음 무료 모의고사를 요청할 때 binding candidate 전체로 기존 TrialClaim을 조회하고, 없을 때만 TrialClaim과 수량 1의 `UserEntitlement`를 원자적으로 생성하는 silent claim 정책을 유지한다.
- 구현 내용: `UserEntitlement`는 실제 무료시험 1회 사용권이고 `EntitlementReservation`은 Learning Core가 시험을 생성하는 동안 그 권리를 예약하는 상태다. `reserve → exam 생성 → confirm`으로 진행하며 생성 실패는 cancel, 결과 불명은 reconciliation으로 처리한다. 따라서 중복 수령 확인, 실제 권리와 시험 생성 잠금은 서로 다른 엔티티 책임이다.
- 실행한 테스트와 결과: 코드 변경이 없는 계획 확인·설명 작업이므로 Gradle 테스트를 실행하지 않았다. 계획서 section 11, 단계 9·10과 테스트 계약을 읽어 확인했으며 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 WORKLOG 끝 append를 정적으로 검증한다.
- 유지한 계약: Identity는 시험·TrialClaim·UserEntitlement·EntitlementReservation을 소유하지 않고 raw phone이나 PhoneIdentity fingerprint를 Entitlement에 전달하지 않는다. 실제 userId와 JWT `sub`, RS256·JWKS, Identity/Learning Core 경계와 Python AI `user_id=examId`를 유지했으며 Secret·Token·Password·전화번호·fingerprint·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 기존 무료 1회 중복 확인 모델은 삭제가 아니라 책임 분리·전달 경계 보강으로 설명한다. 최종 중복 판정은 TrialClaim, 사전 번호 증명은 VerifiedPhoneBenefitBinding, 실제 사용권은 UserEntitlement, 시험 생성 잠금은 EntitlementReservation이 담당한다. 기존 코드·계획·Jira를 변경하지 않았고 Git commit·push를 수행하지 않았다.
- 위험 요소: 네 모델과 outbox consumer는 현재 계획이며 아직 구현 완료가 아니다. benefit binding이 준비되기 전에 production signup을 열면 raw phone을 저장하지 않는 구조에서 기존 가입자의 benefit candidate를 복구할 수 없어 재인증이 필요하고, outbox 지연을 무시하면 중복 지급 우회가 생길 수 있다.
- 다음 작업: 단계 5 production signup 전에 benefit fingerprint key ownership·domain·version, PhoneEligibilityBindingOutbox schema, Entitlement의 멱등 consumer·보존 정책을 ADR로 확정한다. 이후 별도 서비스에서 TrialClaim unique, UserEntitlement 원자 grant와 reservation 흐름을 구현·검증한다.

## 2026-08-13 — Firebase Stage 0 Jira 생성 초안 준비

<!-- codex-turn:019ffa0b-29d5-7a11-be71-7f9a2ccacdba -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`10b1fa0`, commit·push 미수행)
- 작업 목표: Firebase broker 계획의 첫 실행 범위인 ADR·격리 PoC를 TMI Jira 작업으로 생성하기 전에 기존 이슈 형식과 생성 필드를 확인하고 정확한 생성 payload 초안을 사용자에게 제시한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드와 전체 계획서는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- Jira: `TMI-88`, `TMI-89`를 Atlassian 공식 MCP로 읽기 전용 조회해 기존 Identity 이슈의 제목·본문 구성·작업 유형·기본 필드 형식을 확인했다.
- 구현 내용: TMI 프로젝트의 `작업` 유형 ID `10003`과 생성 가능 필드 20개를 확인했다. 보고자는 기본값을 사용하고 우선순위는 프로젝트 기본 Medium, 상태는 기본 `해야 할 일`, 담당자·라벨·컴포넌트·상위 항목은 지정하지 않는 생성안을 준비했다.
- 구현 내용: 제목은 `[Identity] Firebase 인증 broker ADR 및 격리 PoC`로 정했다. 본문에는 Firebase/Identity 책임 경계, email/password·Google·Apple, 동일 UID phone link·번호 충돌·고아 cleanup, 목적별 ID Token 검증, Kakao Identity Platform Generic OIDC, reusable enrollment 계약, 여러 기기 Guest merge proof, lifecycle·비용과 ADR 산출물을 포함했다.
- 구현 내용: production Firebase 설정·공개 인증 API, FirebaseIdentity·FirebaseEnrollmentAttempt·PhoneIdentity의 production 구현, Guest 승격·merge/outbox, TrialClaim·Entitlement·Learning Core 변경과 실제 개인정보·인증정보 처리를 명시적 제외 범위로 두었다.
- 수행한 Jira 작업: 접근 가능한 Atlassian resource, TMI-88·TMI-89와 TMI 작업 유형 metadata를 읽기 전용 조회했다. Jira 이슈 생성·수정·댓글·상태 전환·삭제는 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글은 추가하지 않았다.
- 변경한 상태: Jira 상태를 변경하지 않았다.
- 승인 여부: 사용자가 Jira 생성을 요청했지만 AGENTS.md의 사전 payload 공개·승인 규칙에 따라 정확한 생성 내용을 먼저 제시하고 별도 승인을 기다린다.
- 실행한 테스트와 결과: 코드 변경이 없는 Jira 초안 작업이므로 Gradle 테스트를 실행하지 않았다. 종료 전 `git diff --check`, 지정 turn marker 단일 존재와 WORKLOG EOF append를 정적으로 검증한다.
- 유지한 계약: Firebase는 credential authentication broker이고 Identity는 canonical UUID User와 자체 RS256 Access/RefreshSession을 소유한다. Firebase UID·email·phone은 userId나 자동 merge 키가 아니며 Firebase ID Token은 Learning Core로 전달하지 않는다. Jira·문서에 Credential·Password·Token·전화번호·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 첫 Jira는 production broker foundation이 아니라 이를 막고 있는 Stage 0 ADR·격리 PoC로 분리한다. PoC와 ADR 승인이 끝난 뒤 Firebase broker foundation과 PhoneIdentity를 각각 별도 Jira로 생성한다.
- 위험 요소: PoC용 격리 Firebase project 준비와 client 플랫폼 협력이 필요할 수 있고 Identity Platform billing·Kakao redirect·국내 SMS는 emulator만으로 증명되지 않는다. production Secret이나 공개 API를 Jira 범위에 섞으면 검증과 구현 경계가 무너질 수 있다.
- 다음 작업: 사용자가 제시한 정확한 Jira payload를 승인하면 TMI `작업`을 생성하고 발급된 이슈 키·저장된 본문·기본 상태·우선순위를 재조회한 뒤 WORKLOG와 CURRENT_STATE에 기록한다.

## 2026-08-13 — Firebase Stage 0 Jira TMI-90 생성

<!-- codex-turn:019ffa1e-91dd-7b72-82b3-1a58f5322adc -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`10b1fa0`, commit·push 미수행)
- Jira: `TMI-90`
- 작업 목표: 사용자에게 사전 제시한 Firebase 인증 broker Stage 0 ADR·격리 PoC Jira payload의 명시적 승인을 받아 TMI 프로젝트에 이슈를 생성하고 저장 결과를 확인한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드와 계획서는 이번 작업에서 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Jira `TMI-90` `[Identity] Firebase 인증 broker ADR 및 격리 PoC`를 `작업` 유형으로 생성했다. 승인된 설명에는 Firebase와 Identity의 책임 경계, email/password·Google·Apple PoC, 동일 UID phone credential link, 번호 충돌·고아 cleanup, 목적별 Firebase ID Token 검증, Kakao Identity Platform Generic OIDC, reusable enrollment, 여러 기기 Guest merge dual proof와 lifecycle·비용·quota·rollout 결정을 포함했다.
- 구현 내용: production Firebase 연동·공개 API, `FirebaseIdentity`·`FirebaseEnrollmentAttempt`·`PhoneIdentity` production 구현, Guest 승격·merge, Entitlement 구현은 이 이슈의 제외 범위로 유지했다.
- 수행한 Jira 작업: 사용자가 정확한 생성 payload를 명시적으로 승인한 뒤 Atlassian 공식 MCP로 `TMI-90`을 생성하고 제목·설명·유형·우선순위·상태를 읽기 전용 재조회했다.
- 추가한 댓글의 목적: Jira 댓글은 추가하지 않았다.
- 변경한 상태: 생성 기본 상태 `해야 할 일`을 유지했으며 상태 전환을 수행하지 않았다. Resolution은 없고 우선순위는 `Medium`이다.
- 승인 여부: 사용자가 Jira 이슈 생성을 명시적으로 승인했다. 댓글 등록이나 상태 전환에 대한 승인은 없으므로 수행하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 Jira 생성·작업 기록 갱신이므로 Gradle 테스트를 실행하지 않았다. `git diff --check`, `git diff --cached --check`, 지정 turn marker 단일 존재와 WORKLOG EOF append를 정적으로 검증한다.
- 유지한 계약: Firebase는 credential authentication broker이고 Identity는 canonical UUID User와 자체 RS256 Access Token·RefreshSession을 소유한다. Firebase UID·email·phone은 userId나 자동 merge 키가 아니며 Firebase ID Token은 Learning Core에 전달하지 않는다. Secret·Token·Password·전화번호·실제 Key·전체 MongoDB URI와 사용자 개인정보를 Jira나 작업 기록에 추가하지 않았다.
- 결정사항: 첫 Firebase 작업은 production 통합이 아니라 ADR·격리 PoC로 한정한다. Jira 상태는 구현 시작 전 `해야 할 일`로 유지하고 별도 사용자 승인 없이 댓글이나 상태를 변경하지 않는다.
- 위험 요소: 격리 Firebase project와 client platform 협력이 필요할 수 있으며 Identity Platform billing·Kakao OIDC redirect·국내 SMS·phone credential collision과 cleanup은 emulator만으로 완전히 증명되지 않을 수 있다.
- 다음 작업: 구현 요청을 받으면 먼저 Jira `TMI-90`을 재조회해 설명과 완료 조건을 기준으로 Stage 0 ADR·격리 PoC를 수행한다. Jira 댓글이나 상태 전환은 별도 승인 후에만 처리한다.

## 2026-08-13 — TMI-90 Firebase broker 조건부 ADR·격리 PoC 구현

<!-- codex-turn:019ffa22-835c-7742-8bd5-3e1f81c5365b -->

- 날짜: 2026-08-13
- 브랜치: `feat/TMI-90-firebase-auth-broker-poc` (`10b1fa0`에서 시작, commit·push 미수행)
- Jira: `TMI-90`
- 작업 목표: Firebase가 credential 인증만 담당하고 Identity가 canonical UUID User와 자체 Session을 소유하는 구조를 조건부 ADR로 확정하며, production 비의존 Auth Emulator·contract PoC로 email/password·Google·Apple·phone·Token lifecycle·enrollment·Guest merge와 Kakao OIDC 최소 가능성을 검증한다.
- 변경 파일: `README.md`, `docs/adr/ADR-001-firebase-authentication-broker.md`, `docs/poc/firebase-auth-broker-stage-0.md`, `docs/contracts/social-login-implementation-plan.md`, `poc/firebase-auth/.gitignore`, `poc/firebase-auth/firebase.json`, `poc/firebase-auth/package.json`, `poc/firebase-auth/package-lock.json`, `poc/firebase-auth/README.md`, `poc/firebase-auth/src/identity-policy.mjs`, `poc/firebase-auth/test/identity-policy.test.mjs`, `poc/firebase-auth/test/firebase-auth-emulator.test.mjs`, `poc/firebase-auth/test/kakao-oidc-discovery.test.mjs`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 기존 계획 문서의 경로 이동·staging 상태는 보존했고 Java production 코드는 변경하지 않았다.
- 구현 내용: `ADR-001`에 Firebase credential broker·Identity canonical account/session owner 책임, UUID `sub`, Provider별 조건, password email verification, 같은 Firebase UID phone link와 phone-only login 차단, purpose별 revoke·recent-auth, enrollment 재사용·CAS, Guest merge dual proof, 가입 중단 cleanup, unlink·withdrawal lifecycle, 장애·vendor exit와 production gate를 기록했다.
- 구현 내용: production artifact와 분리된 `poc/firebase-auth` Node 모듈을 추가했다. demo project ID의 Firebase Auth Emulator만 사용하며 Firebase Web/Admin SDK와 CLI를 exact devDependency·lockfile로 고정하고 실제 project나 credential이 없어도 재현 가능하게 했다. 기본 Java test와 Kakao 외부 discovery 테스트는 서로 분리했다.
- 구현 내용: Auth Emulator에서 email/password 가입·재로그인, email verification Claim 변화, Admin ID Token Claim 검증, disabled·refresh token revoke·delete 차단, Google·Apple emulator credential의 Firebase UID·provider UID 안정성, phone link 전후 UID 유지, phone-only User 생성 가능성, 번호 ownership 충돌과 owner 삭제 후 재연결을 검증했다.
- 구현 내용: Node Firebase SDK의 browser 전용 phone API가 Node 환경에서 지원되지 않는 사실을 확인해 phone PoC는 SDK 내부와 같은 Identity Toolkit Emulator endpoint를 사용했다. production client는 공식 Android/iOS/Web SDK link 흐름과 실기기 UID 유지·redirect를 별도 gate로 다시 검증하도록 문서화했다.
- 구현 내용: 실행 가능한 policy contract로 password·Google·Apple 허용과 phone-only 거절, verified email·primary credential·same-UID verified phone enrollment, Kakao feature off, login 15분·고위험 5분 recent-auth, Firebase 경로 revoke 검사, Identity reissue·Learning Core Firebase 비의존, active enrollment binding 재사용과 Guest JWT·target Firebase proof 동시 검증을 고정했다.
- 구현 내용: Kakao 공개 OIDC discovery의 issuer, authorization code, PKCE S256, RS256, pairwise subject와 `sub` Claim 최소 계약을 opt-in 테스트로 확인했다. Identity Platform provider 등록·billing·providerData UID·모바일 deep-link는 확인되지 않았으므로 Kakao production 결정은 `NO-GO`와 feature off로 유지했다.
- 수행한 Jira 작업: 구현 전에 Atlassian 공식 MCP로 `TMI-90`의 설명·완료 조건·제외 범위와 현재 상태를 읽기 전용 재조회했다. Jira 생성·수정·댓글·상태 전환·삭제는 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글은 추가하지 않았다. 종료 댓글은 별도 사용자 승인 전 자동 등록하지 않는다.
- 변경한 상태: Jira `TMI-90`은 기존 `해야 할 일`, 우선순위 `Medium`, Resolution 없음 상태를 유지한다.
- 승인 여부: 사용자가 `TMI-90` 구현 시작을 명시적으로 요청했다. Jira 댓글·상태 전환이나 production 외부 Firebase 설정 변경은 승인 범위에 포함하지 않았다.
- 실행한 테스트와 결과: `npm run test:contract` 7개 통과, `npm run test:emulator` 5개 통과, `npm run test:kakao-discovery` 1개 통과, `./gradlew clean test` 전체 45개 suite·317개 테스트가 failure·error·skip 0으로 성공했다. 첫 Emulator 실행은 sandbox의 local port 제한 뒤 승인된 local 실행으로 재검증했고, Node phone API 제약은 REST 기반 Emulator proof로 보완한 뒤 최종 전체가 통과했다.
- 실행한 테스트와 결과: dependency 설치는 PoC 전용 tree에서 moderate audit 항목과 현재 로컬 Node가 CLI dependency 지원 LTS 범위 밖이라는 경고를 보고했다. 자동 breaking upgrade는 수행하지 않았고 재현 환경은 Node 20·22·24로 제한했다. PoC dependency는 Spring Boot runtime과 기본 Gradle classpath에 포함하지 않는다.
- 유지한 계약: Identity JWT `sub`와 실제 userId는 canonical UUID 문자열이며 기존 RS256·`kid`·issuer·`tosunsaeng-learning-core` audience·JWKS와 Refresh Token 원문 비저장 계약을 유지했다. Firebase ID Token은 Learning Core에 전달하지 않고 email·phone·Firebase UID를 자동 merge 키로 사용하지 않는다. Identity에 시험·혜택 코드를 추가하지 않았다.
- 유지한 계약: production Secret·credential·실제 Key·전체 MongoDB URI·사용자 개인정보를 저장소·문서·Jira에 기록하지 않았다. PoC 실행 로그는 일회성 검증값을 표시할 수 있어 artifact로 저장하지 않으며 raw Token·provider subject·Firebase UID를 출력하거나 fixture로 남기지 않는다.
- 결정사항: Firebase broker 구조는 조건부 채택한다. Stage 3·4의 disabled-by-default foundation은 ADR 기준으로 진행할 수 있지만 실제 login/signup 공개와 Provider flag 활성화는 모바일 Google·Apple·phone, 국내 SMS·abuse, Identity Platform Kakao·billing·deep-link, Apple revoke와 장애 mapping gate 통과 뒤에만 허용한다.
- 결정사항: email/password enrollment는 verified email을 요구한다. phone은 current primary Firebase User에 link된 가입 proof이며 phone-only LOGIN_EXCHANGE를 거절한다. 한 검증 번호는 동시에 ACTIVE MEMBER 하나만 점유하고 충돌을 자동 merge로 처리하지 않는다.
- 위험 요소: Auth Emulator는 실제 Provider 서명·OAuth consent·redirect, 국내 SMS, quota·timeout, Identity Platform 등록·billing과 Apple revoke를 증명하지 않는다. Firebase와 Mongo의 이중 lifecycle, abandoned cleanup 오판, revoke 전파 지연과 reconciliation이 production 전 남아 있다.
- 위험 요소: PoC의 npm dependency tree는 production artifact가 아니지만 audit 경고가 있으므로 CI 유지 시 lockfile과 upstream advisory를 추적해야 한다. Firebase CLI는 지원 Node LTS에서 재현해야 하며 Emulator 출력은 저장·공유하지 않는다.
- 다음 작업: 사용자가 조건부 ADR을 검토한 뒤, 실제 격리 Firebase project·Android/iOS test app으로 남은 production gate를 수행한다. Stage 3 Firebase broker foundation과 Stage 4 PhoneIdentity Jira는 각각 정확한 payload를 먼저 제시하고 별도 승인을 받아 생성한다. Jira 댓글·상태 전환과 commit·push는 사용자가 명시적으로 승인하거나 직접 수행하기 전 진행하지 않는다.

## 2026-08-13 — TMI-90 구현 범위·결과 정리

<!-- codex-turn:2026-08-13-tmi-90-implementation-summary -->

- 날짜: 2026-08-13
- 브랜치: `feat/TMI-90-firebase-auth-broker-poc` (`10b1fa0`, commit·push 미수행)
- Jira: `TMI-90`
- 작업 목표: 현재 구현된 Firebase 인증 broker Stage 0의 산출물·검증 결과·운영 미구현 범위를 저장소와 Jira 기준으로 재확인해 사용자에게 명확히 정리한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. ADR·PoC·계획서·애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: `ADR-001`과 격리 `poc/firebase-auth`를 확인해 Firebase는 credential broker, Identity는 canonical UUID User·자체 JWT/RefreshSession·Guest lifecycle owner라는 조건부 결정을 재확인했다. email/password verification, Google·Apple mapping, same-UID phone link, phone-only 거절, 번호 충돌, revoke·disabled·delete, enrollment 재사용과 Guest merge dual proof가 로컬 계약·Emulator PoC 범위에서 검증됐다.
- 구현 내용: Kakao는 공개 OIDC discovery의 최소 계약만 확인됐고 Identity Platform 등록·billing·providerData UID·모바일 redirect가 검증되지 않아 production `NO-GO`와 feature off 상태임을 확인했다. Spring Boot Firebase Admin adapter, `FirebaseIdentity`, `FirebaseEnrollmentAttempt`, `PhoneIdentity`, 공개 exchange/signup, Guest 승격·merge와 Entitlement는 아직 구현되지 않았다.
- 수행한 Jira 작업: Atlassian 공식 MCP로 `TMI-90`의 설명·완료 조건·제외 범위·상태·우선순위·Resolution을 읽기 전용 재조회했다. Jira 생성·수정·댓글·상태 전환·삭제는 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글은 추가하지 않았다.
- 변경한 상태: Jira는 기존 `해야 할 일`, 우선순위 `Medium`, Resolution 없음 상태를 유지한다.
- 승인 여부: 사용자는 구현 결과 정리만 요청했으며 Jira 변경 승인은 요청하거나 받지 않았다.
- 실행한 테스트와 결과: 이번 요청은 코드 변경 없는 검토·문서 기록이므로 테스트를 재실행하지 않았다. 직전 구현에서 기록된 `npm run test:contract` 7개, `npm run test:emulator` 5개, `npm run test:kakao-discovery` 1개와 `./gradlew clean test` 45 suite·317개 성공 결과를 저장소 문서와 대조했다. 종료 전 문서 diff와 whitespace를 정적으로 검증한다.
- 유지한 계약: canonical UUID `userId`와 JWT `sub`, Identity RS256·`kid`·issuer·audience·JWKS, Refresh Token 원문 비저장, Firebase ID Token의 downstream 비전달, email·phone·Firebase UID 자동 merge 금지와 Identity/Learning Core 도메인 경계를 유지했다. Secret·Token·Password·전화번호·Firebase UID·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 현재 완료 표현은 `조건부 ADR 및 production 비의존 Stage 0 PoC 구현·검증`으로 제한한다. 운영 Firebase 인증 기능이나 회원가입 흐름이 완성된 것으로 표현하지 않으며, Jira 댓글·상태와 Git commit·push는 변경하지 않는다.
- 위험 요소: Auth Emulator는 실제 Google·Apple credential, 모바일 redirect, 공식 SDK phone link, 국내 SMS·quota·abuse, Identity Platform Kakao와 Apple revoke를 증명하지 않는다. PoC npm dependency tree의 moderate audit 경고와 Firebase/Mongo 이중 lifecycle·reconciliation도 production 전 남아 있다.
- 다음 작업: 조건부 ADR 검토와 실프로젝트·모바일 외부 gate를 진행한 뒤, Stage 3 disabled-by-default Firebase broker foundation과 Stage 4 PhoneIdentity를 각각 별도 Jira로 작성·구현한다. Jira 변경은 정확한 payload를 먼저 보여주고 사용자 승인을 받은 뒤에만 수행한다.

## 2026-08-13 — TMI-90 구현 정리 turn 기록 보완

<!-- codex-turn:019ffa4c-4791-71c2-bb66-8f317765d35d -->

- 날짜: 2026-08-13
- 브랜치: `feat/TMI-90-firebase-auth-broker-poc` (`10b1fa0`, commit·push 미수행)
- Jira: `TMI-90`
- 작업 목표: Hook이 지정한 현재 turn 식별자로 TMI-90 구현 결과 정리 작업을 WORKLOG 끝에 기록하고 CURRENT_STATE를 동기화한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 구현 코드·ADR·PoC·계획서는 변경하지 않았고 WORKLOG 과거 항목도 수정하거나 삭제하지 않았다.
- 구현 내용: Firebase credential broker 조건부 ADR, production 비의존 Auth Emulator·policy contract PoC, 통과한 검증과 미구현 production 범위를 저장소·Jira 기준으로 정리한 현재 turn을 지정 marker로 기록했다.
- 수행한 Jira 작업: 앞선 정리 과정에서 Atlassian 공식 MCP로 `TMI-90`을 읽기 전용 조회했으며 이 보완에서는 Jira API를 호출하지 않았다. Jira 생성·수정·댓글·상태 전환·삭제는 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글은 추가하지 않았다.
- 변경한 상태: Jira는 `해야 할 일`, 우선순위 `Medium`, Resolution 없음으로 유지한다.
- 승인 여부: 사용자 요청과 Hook 지시에 따라 저장소 작업 기록만 보완했다. Jira 변경이나 Git commit·push 승인은 없었다.
- 실행한 테스트와 결과: 구현 코드가 바뀌지 않아 Gradle·PoC 테스트를 재실행하지 않았다. 문서 변경에 대해 `git diff --check`, `git diff --cached --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: canonical UUID `userId`와 JWT `sub`, Identity의 자체 RS256 Access Token·RefreshSession, Firebase ID Token downstream 비전달, email·phone·Firebase UID 자동 merge 금지와 Identity/Learning Core 경계를 유지했다. Secret·Token·Password·전화번호·Firebase UID·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 현재 완료 범위는 조건부 ADR과 격리 Stage 0 PoC로 제한하며 production Firebase 기능 완료로 표현하지 않는다. Jira·Git 상태는 변경하지 않는다.
- 위험 요소: 실제 모바일 Provider·공식 phone link·국내 SMS·Identity Platform Kakao·Apple revoke와 production adapter·entity·API는 계속 미구현 또는 외부 검증 대기 상태다.
- 다음 작업: 조건부 ADR과 외부 gate를 검토한 뒤 Stage 3 Firebase broker foundation과 Stage 4 PhoneIdentity를 각각 별도 승인된 Jira 범위로 진행한다.

## 2026-08-13 — TMI-90 검증 성공 범위와 미검증 경계 설명

<!-- codex-turn:019ffa50-ab6f-71e2-97f1-55c0cf5878f9 -->

- 날짜: 2026-08-13
- 브랜치: `feat/TMI-90-firebase-auth-broker-poc` (`10b1fa0`, commit·push 미수행)
- Jira: `TMI-90`
- 작업 목표: 첨부된 TMI-90 작업 기록이 실제로 무엇을 구현·검증했는지, 테스트 통과가 어떤 범위의 성공이며 production 준비 완료와 어떻게 다른지 이해하기 쉽게 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. ADR·PoC·계획서와 애플리케이션 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: TMI-90 산출물은 Firebase를 credential broker로 조건부 채택하는 ADR, production과 분리된 Node PoC, Auth Emulator 테스트와 실행 가능한 Identity policy contract임을 확인했다. Spring Boot Firebase adapter·FirebaseIdentity·FirebaseEnrollmentAttempt·PhoneIdentity·exchange/signup API를 구현한 작업은 아니다.
- 구현 내용: contract 7개는 email/password·Google·Apple 허용, phone-only 거절, Kakao flag off, verified phone enrollment, 목적별 revoke/recent-auth, Identity reissue·Learning Core Firebase 비호출, enrollment binding과 Guest merge dual proof라는 자체 정책 함수가 의도대로 동작함을 검증한다. 이는 외부 Firebase 서버나 Provider를 검증한 결과가 아니라 설계 규칙의 실행 가능한 회귀 테스트다.
- 구현 내용: Auth Emulator 5개는 로컬 Firebase 모사 환경에서 email/password UID 유지와 email verification Claim, Admin ID Token 검증·disable·revoke·delete, Google·Apple provider mapping 안정성, 기존 UID에 phone link, phone-only User 생성 가능성, 번호 ownership 충돌과 owner 삭제 후 재연결을 확인했다. 이는 목표 아키텍처의 핵심 Firebase 동작 가설이 성립한다는 유의미한 PoC지만 실제 Provider 서명·OAuth·모바일 redirect·SMS를 증명하지 않는다.
- 구현 내용: Kakao discovery 1개는 공개 metadata에서 issuer·authorization code·PKCE S256·RS256·pairwise subject·`sub` 지원을 확인했을 뿐 Firebase Identity Platform provider 등록·billing·providerData UID·모바일 deep-link는 확인하지 못했다. 따라서 Kakao는 production NO-GO·feature off다. Java 45 suite·317개 통과는 기존 Identity 코드에 회귀가 없다는 뜻이며 production Firebase Java 연동 성공 증거가 아니다.
- 실행한 테스트와 결과: 이번 turn은 분석·설명만 수행해 테스트를 재실행하지 않았다. 첨부 기록, Stage 0 결과 문서, ADR, contract·Emulator·Kakao 테스트 소스와 기존 실행 기록의 contract 7/7, Emulator 5/5, discovery 1/1, Java 45 suite·317개 성공을 대조했다. 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 WORKLOG 끝 append를 정적으로 검증한다.
- 유지한 계약: 현재 결론을 `Stage 0 production 비의존 local PoC 성공`으로 제한하고 Firebase UID·email·phone을 canonical userId나 자동 merge 키로 해석하지 않았다. Identity JWT UUID `sub`, RS256·JWKS, Refresh Token 원문 비저장과 Identity/Learning Core 경계를 유지했으며 Secret·Token·Password·전화번호·Firebase UID·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: `검증이 잘 됐다`는 표현은 local policy·Emulator 가설 검증에는 맞지만 실제 Firebase production 인증 완성에는 맞지 않는다. ADR은 `조건부 채택`, production feature는 비활성, Jira 상태는 `해야 할 일`이며 Jira 댓글·상태·필드와 Git commit·push를 변경하지 않았다.
- 위험 요소: 실제 Android/iOS Google·Apple OAuth, 공식 SDK phone link, 국내 SMS·quota·abuse, Apple nonce/revoke/delete, Firebase timeout·quota 오류 mapping, Identity Platform Kakao·billing·deep-link와 Firebase/Mongo lifecycle reconciliation이 남아 있다. Emulator 성공만으로 production flag를 열면 외부 환경 차이를 놓칠 수 있다.
- 다음 작업: 실제 격리 Firebase project와 Android/iOS test app으로 외부 gate를 수행한다. 그와 별개로 Stage 3 disabled-by-default Spring Firebase adapter foundation과 Stage 4 PhoneIdentity는 정확한 Jira 범위와 사용자 승인 후 구현하며 production exchange/signup은 외부 gate 통과 전 활성화하지 않는다.

## 2026-08-13 — TMI-90 종료안 제시 및 다음 Stage 3 범위 정리

<!-- codex-turn:019ffa55-1835-74b2-989d-53e678b3661b -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`77804e6`, GitHub PR #18 병합 확인, Codex commit·push 미수행)
- Jira: `TMI-90`
- 작업 목표: 병합된 Firebase broker Stage 0 작업의 Jira 종료 조건을 확인하고, 규칙에 따라 종료 댓글·상태 전환 내용을 사용자에게 먼저 제시하며 다음 Stage 3 구현 범위를 정리한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드·ADR·PoC·계획서는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 로컬 `develop`과 `origin/develop`이 GitHub PR #18 merge commit `77804e6`을 가리키며 feature commit `af50153`이 병합된 것을 확인했다. 병합 확인 전 worktree는 깨끗했다.
- 구현 내용: TMI-90 종료 댓글에는 조건부 Firebase broker ADR, production 비의존 Auth Emulator·policy contract PoC, 변경 파일, contract 7개·Emulator 5개·Kakao discovery 1개와 Java 45 suite·317개 성공, 실제 모바일 Provider·국내 SMS·Identity Platform Kakao·Apple revoke가 남은 production gate임을 기록하는 안을 준비했다.
- 구현 내용: 다음 Stage 3은 disabled-by-default Firebase Admin adapter와 안전한 설정, 목적별 ID Token 검증·recent-auth·오류 mapping, `FirebaseIdentity` unique mapping, `FirebaseEnrollmentAttempt`의 application 만료·CAS·PENDING partial unique·TTL cleanup, 외부 인프라 없는 테스트로 제한한다. 공개 exchange/signup, `PhoneIdentity`, Guest 승격·merge는 제외한다.
- 수행한 Jira 작업: Atlassian 공식 MCP로 TMI-90의 설명·상태·Resolution·기존 댓글과 사용 가능한 전환을 읽기 전용 조회했다. 종료 전환 ID `41`을 확인했지만 Jira 댓글·상태·필드 변경은 수행하지 않았다.
- 추가한 댓글의 목적: 구현 산출물·변경 파일·테스트 결과·남은 production gate를 기록하는 종료 댓글 초안만 준비했으며 아직 등록하지 않았다.
- 변경한 상태: Jira는 기존 `해야 할 일`, 우선순위 `Medium`, Resolution 없음 상태다. 승인 후 transition ID `41`만 적용할 예정이다.
- 승인 여부: 사용자가 Jira 종료를 요청했지만 AGENTS.md의 정확한 변경 내용 사전 공개·승인 규칙에 따라 댓글과 전환 payload를 먼저 제시했으며 별도 승인을 기다린다.
- 실행한 테스트와 결과: 이번 turn은 병합·Jira 상태 확인과 범위 정리만 수행해 테스트를 재실행하지 않았다. 병합된 작업의 기록된 결과는 contract 7/7, Emulator 5/5, Kakao discovery 1/1과 `./gradlew clean test` 45 suite·317개 성공이다. 종료 전 문서 diff·whitespace와 지정 marker 단일 존재를 정적으로 검증한다.
- 유지한 계약: Firebase는 credential broker이고 Identity는 canonical UUID User·자체 RS256 Access Token·RefreshSession을 소유한다. Firebase UID·email·phone은 자동 merge 키가 아니며 Firebase ID Token은 Learning Core에 전달하지 않는다. Secret·Token·Password·전화번호·Firebase UID·실제 Key·전체 MongoDB URI와 사용자 개인정보를 Jira나 작업 기록에 추가하지 않았다.
- 결정사항: Stage 0의 local PoC와 조건부 ADR은 병합 완료로 보되 production Firebase 기능 활성화와 외부 gate 완료로 해석하지 않는다. Jira 변경은 사용자 승인 후 종료 댓글 등록과 완료 전환만 수행한다.
- 위험 요소: 실제 Android/iOS Google·Apple 인증, 공식 SDK same-UID phone link, 국내 SMS·quota·abuse, Identity Platform Kakao·billing·deep-link, Apple revoke와 Firebase/Mongo reconciliation은 계속 남아 있다. Stage 3 foundation도 기본 비활성으로 배포해야 한다.
- 다음 작업: 사용자 승인 시 TMI-90 종료 댓글을 등록하고 transition ID `41`로 완료 처리한 뒤 재조회한다. 이후 별도 사전 승인으로 Stage 3 Firebase broker foundation Jira를 생성하고 구현한다.

## 2026-08-13 — Stage 3 Firebase broker foundation Jira TMI-91 생성

<!-- codex-turn:019ffa58-fc35-7653-b47f-756d8f177778 -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`77804e6`, Codex commit·push 미수행)
- Jira: `TMI-91`
- 작업 목표: 앞서 사용자에게 제시한 다음 작업인 Stage 3 Firebase broker foundation을 독립 Jira 작업으로 생성하고 저장된 범위를 확인한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드·ADR·PoC·계획서는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Jira `TMI-91` `[Identity] Firebase 인증 broker foundation 구축`을 `작업` 유형으로 생성했다. 설명에는 disabled-by-default Firebase Admin SDK adapter와 application port, 목적별 ID Token 검증·revocation·recent-auth·401/403/429/503 mapping, `FirebaseIdentity`, `FirebaseEnrollmentAttempt`의 application 만료·CAS·PENDING partial unique·TTL cleanup과 외부 인프라 없는 테스트를 포함했다.
- 구현 내용: 공개 Firebase exchange/signup, 실제 User·RefreshSession 생성, `PhoneIdentity`, phone/SMS, Guest 승격·merge, 실제 모바일 Provider, Kakao Identity Platform, Apple revoke, 기존 password endpoint 제거와 Entitlement는 명시적인 제외 범위로 기록했다.
- 수행한 Jira 작업: 앞서 범위를 제시한 뒤 사용자가 Jira 생성을 명시적으로 요청해 Atlassian 공식 MCP로 TMI-91을 생성하고 제목·설명·유형·우선순위·상태·담당자·라벨·컴포넌트·Resolution을 읽기 전용 재조회했다. TMI-90에는 댓글·상태·필드 변경을 수행하지 않았다.
- 추가한 댓글의 목적: TMI-91과 TMI-90 모두 Jira 댓글을 추가하지 않았다.
- 변경한 상태: 새 TMI-91은 기본 `해야 할 일`, 우선순위 `Medium`, Resolution 없음이다. 상태 전환은 수행하지 않았다.
- 승인 여부: 사용자가 앞서 제시된 Stage 3 범위를 확인한 뒤 `우선 지라부터 생성`을 명시적으로 요청해 생성 승인을 받은 것으로 처리했다. 별도 Jira 댓글·상태 전환 승인은 이번 요청 범위에 포함하지 않았다.
- 실행한 테스트와 결과: 코드 변경 없는 Jira 생성·기록 작업이라 Gradle과 PoC 테스트를 재실행하지 않았다. 종료 전 `git diff --check`, `git diff --cached --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: 실제 userId와 JWT `sub`는 canonical UUID 문자열이고 Identity가 RS256 Access Token·RefreshSession·JWKS를 소유한다. Firebase UID·provider subject·email·phone은 자동 merge key가 아니며 Firebase ID Token은 Learning Core에 전달하지 않는다. Secret·Token·Password·전화번호·Firebase UID·실제 Key·전체 MongoDB URI와 사용자 개인정보를 Jira나 작업 기록에 추가하지 않았다.
- 결정사항: Stage 3은 production 인증 공개가 아닌 기본 비활성 foundation으로 분리한다. TMI-90 종료는 이번 요청보다 뒤로 미뤘으며 별도 승인된 댓글·완료 전환 전에는 현재 상태를 유지한다.
- 위험 요소: Firebase Admin SDK 추가 시 초기화 lifecycle·credential 공급·remote revoke 비용·timeout과 SDK 오류 분류를 안전하게 격리해야 한다. Mongo partial unique는 현재 시각 조건을 표현하지 못하므로 만료 PENDING attempt를 application CAS로 EXPIRED 처리하지 않으면 새 attempt를 막을 수 있다.
- 다음 작업: 사용자가 원하면 먼저 제시된 내용대로 TMI-90 종료 댓글과 완료 전환을 적용한다. TMI-91 구현 요청 시 이슈를 재조회하고 feature branch를 만든 뒤 공개 API 없이 foundation과 테스트만 구현한다.

## 2026-08-13 — TMI-91 Firebase 인증 broker foundation 구현

<!-- codex-turn:019ffa5e-db21-7320-9ff4-f568ab2a8cf1 -->

- 날짜: 2026-08-13
- 브랜치: `feat/TMI-91-firebase-auth-foundation` (Codex commit·push 미수행)
- Jira: `TMI-91`
- 작업 목표: 공개 Firebase 로그인·회원가입을 열기 전에 disabled-by-default Firebase Admin adapter, 목적별 검증 port, canonical User mapping과 중단 가능한 enrollment 상태·CAS 기반을 구현하고 외부 인프라 없는 테스트로 고정한다.
- 변경 파일: `.env.example`, `README.md`, `build.gradle`, `src/main/resources/application.yml`, `src/test/resources/application-test.yml`, `src/main/java/web/tosunsaeng/identity/domain/auth/exception/AuthErrorStatus.java`, `src/main/java/web/tosunsaeng/identity/domain/auth/application/firebase/*`, `src/main/java/web/tosunsaeng/identity/domain/auth/infrastructure/firebase/*`, `src/main/java/web/tosunsaeng/identity/domain/auth/domain/entity/FirebaseIdentity.java`, `FirebaseEnrollmentAttempt.java`, 관련 enum·Repository·custom implementation, `src/test/java/web/tosunsaeng/identity/domain/auth/**/Firebase*Tests.java`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Firebase Admin SDK `9.4.3`을 추가하고 전체·Google·Apple·Kakao·Phone flag를 모두 기본 `false`로 구성했다. 활성화 시 project ID를 필수화하고 선택 tenant, recent-auth·clock skew·connect/read timeout·enrollment TTL을 검증하며 ADC/Workload Identity credential provider와 named FirebaseApp 중복 초기화 방지를 사용한다. credential 파일·실제 project 값·Secret은 추가하지 않았다.
- 구현 내용: Controller와 application이 Firebase SDK 타입을 참조하지 않도록 `FirebaseAuthenticationVerifier`, `FirebaseVerificationPurpose`, 최소 `VerifiedFirebasePrincipal`·`VerifiedSocialPrincipal` 경계를 추가했다. Admin adapter는 Firebase 서명 검증 뒤 모든 목적에서 revoke와 최신 UserRecord를 확인하고 configured project issuer·audience·tenant, UID, 시간 Claim·목적별 recent-auth, disabled 상태와 provider allowlist를 검증한다.
- 구현 내용: LOGIN_EXCHANGE recent-auth는 15분, 그 밖의 가입·merge·sync·고위험 목적은 5분으로 고정했다. phone sign-in 로그인과 primary provider 없는 phone-only 계정을 거절하고, enrollment는 같은 Firebase account에 연결된 verified phone과 password 사용 시 verified email을 요구한다. Google·Apple·Kakao providerData의 opaque UID만 기존 SocialProvider subject로 변환하며 raw ID Token·전체 Claim·email·phone·SDK 객체는 application 결과로 내보내지 않는다.
- 구현 내용: Firebase SDK의 invalid/expired/revoked 계열은 고정 401, disabled·정책 거절은 403, quota는 429, timeout·permission·예상 밖 runtime은 503으로 fail-closed 변환한다. SDK 원문 message와 Token을 예외·문자열 표현에 포함하지 않고 최소 principal과 내부 snapshot의 identifier·provider subject도 redaction한다.
- 구현 내용: `FirebaseIdentity`에 서버 UUID id, Firebase project·opaque case-sensitive UID, canonical UUID userId와 createdAt을 저장하고 `(firebaseProjectId, firebaseUid)` 및 `userId` unique index를 선언했다. Firebase UID는 내부 userId나 JWT `sub`로 사용하지 않으며 email·phone·Token·Claim은 저장하지 않는다.
- 구현 내용: `FirebaseEnrollmentAttempt`는 DIRECT_SIGNUP 또는 canonical Guest UUID binding, initial sign-in method, PENDING/CONSUMED/EXPIRED, application `expiresAt`, 별도 `cleanupAt` TTL과 시각을 가진다. binding별 PENDING partial unique index, 명시적 null direct binding, active attempt 재사용, 만료 CAS, duplicate insert loser의 winner 재조회와 project·UID·binding 전체가 같은 경우에만 성공하는 일회 consume CAS를 구현했다. TTL 삭제 시각은 correctness 판정에 사용하지 않는다.
- 수행한 Jira 작업: 구현 전과 종료 시 Atlassian 공식 MCP로 TMI-91의 제목·설명·완료 조건·제외 범위·상태·우선순위·Resolution을 읽기 전용 재조회했다. Jira 이슈·댓글·상태·필드는 변경하지 않았다.
- 추가한 댓글의 목적: 종료 댓글은 자동 등록하지 않았고, 구현 요약·변경 파일·테스트 결과·남은 위험을 담은 초안만 최종 응답에 제공한다.
- 변경한 상태: Jira TMI-91은 기존 `해야 할 일`, 우선순위 `Medium`, Resolution 없음이다. PR 병합을 확인하지 않았으므로 완료 전환하지 않았다.
- 승인 여부: 사용자가 TMI-91 구현 시작을 명시적으로 요청했다. Jira 댓글·상태 전환, Git commit·push, production Firebase 활성화나 외부 Firebase 설정 변경은 승인 범위에 포함되지 않았다.
- 실행한 테스트와 결과: enrollment service·SDK adapter·configuration·Repository target 테스트를 반복 실행해 active reuse, 만료 전환, duplicate winner, binding mismatch consume 거절, 오류 분류와 enabled/disabled wiring을 확인했다. 최종 `./gradlew clean test`는 54개 suite·353개 테스트가 skip 0, failure 0, error 0으로 성공했다. 실제 Firebase project·credential·Atlas·외부 OAuth Provider는 호출하지 않았다.
- 실행한 테스트와 결과: `git diff --check`, `git diff --cached --check`, Firebase 공개 Controller 부재, application 계층의 Firebase SDK import 부재, feature/provider 기본값 false와 저장소의 Private Key·실제 MongoDB URI·Firebase API key 부재를 정적으로 검증했고 모두 통과했다.
- 유지한 계약: 실제 사용자 ID와 JWT `sub`는 Identity canonical UUID 문자열이고 기존 RS256·`kid`·issuer·`tosunsaeng-learning-core` audience·JWKS와 자체 RefreshSession을 변경하지 않았다. Firebase ID Token은 Identity 내부 인증 경계 밖이나 Learning Core로 전달하지 않고 Firebase UID·provider subject·email·phone을 자동 merge key로 사용하지 않는다. 시험·채점·무료 사용권 코드를 추가하지 않았다.
- 결정사항: 이 PR은 foundation만 제공하며 공개 exchange/signup, User·RefreshSession finalize, PhoneIdentity, Guest 승격·merge, 실제 SNS/mobile/SMS, Kakao Identity Platform과 Apple revoke는 계속 제외한다. Firebase와 Provider flag는 production을 포함해 기본 비활성이고 외부 gate 전에는 켜지 않는다.
- 위험 요소: 인메모리 Mongo 구현은 partial unique filter의 status 변경 후 재삽입 의미를 완전히 지원하지 않아 annotation/index resolver·CAS·경쟁 테스트로 계약을 나눠 검증했다. 실제 MongoDB의 partial unique와 TTL index, custom Repository wiring, 상태 전환 뒤 새 attempt 허용은 staging에서 재검증해야 한다.
- 위험 요소: 모든 Firebase 목적의 revoke·UserRecord 확인은 실제 배포에서 원격 latency·quota·장애 비용을 만든다. ADC/Workload Identity 권한, timeout, 429/503, 실제 Google·Apple·Kakao·phone credential과 Firebase/Mongo lifecycle은 아직 production 환경에서 검증되지 않았다.
- 다음 작업: 사용자가 변경을 검토한 뒤 직접 commit·push하고 PR을 생성한다. 병합 전 Jira를 Done으로 변경하지 않으며, 병합 뒤 정확한 종료 댓글과 전환 payload를 먼저 제시해 별도 승인을 받는다. 후속 Stage 4는 별도 Jira로 PhoneIdentity·versioned domain-separated HMAC·alias/index/rotation 테스트만 구현한다.

## 2026-08-13 — TMI-91 PR 병합 확인·Jira 완료 및 다음 Stage 4 정리

<!-- codex-turn:019ffa80-dfc0-7ba0-8930-4182c8458a76 -->

- 날짜: 2026-08-13
- 브랜치: `develop` (`e25adbc`, GitHub PR #19 병합 확인, Codex commit·push 미수행)
- Jira: `TMI-91`
- 작업 목표: 병합된 Firebase broker foundation의 Jira 종료 조건을 확인하고, 승인된 종료 댓글과 완료 전환을 적용한 뒤 다음 PhoneIdentity 구현 범위를 정리한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: GitHub PR #19 `feat(TMI-91): build Firebase auth broker foundation`이 `develop`에 merge commit `e25adbcec9ffb4c5bd3c5c432a7029fc1e77e8ff`로 병합된 것을 GitHub에서 확인하고 로컬 `develop`을 원격과 fast-forward 동기화했다.
- 구현 내용: 다음 Stage 4는 E.164 재검증, domain-separated HMAC-SHA-256 key registry의 ACTIVE_WRITE/LOOKUP_ONLY lifecycle, `PhoneIdentity`·`PhoneFingerprintAlias`, User당 번호 하나와 retained version 전체의 동일 번호 중복 차단, `PHONE_ALREADY_LINKED`·자동 merge 금지, raw phone·last4·fingerprint 비로그 테스트로 제한한다.
- 구현 내용: Firebase SMS·OTP, 공개 login exchange/signup, User·RefreshSession finalize, Guest 승격·merge, TrialClaim·UserEntitlement와 benefit-scoped fingerprint는 Stage 4에서 제외하고 각 후속 단계의 기존 경계를 유지한다.
- 수행한 Jira 작업: 사용자에게 직전 turn에서 제시한 종료 댓글 초안에 대해 이번 turn의 `지라 닫아줘`로 명시적 승인을 받은 뒤 Atlassian 공식 MCP를 사용했다. TMI-91에 댓글 ID `10004`를 등록하고 사용 가능한 `완료` transition ID `41`만 적용한 뒤 상태와 Resolution을 재조회했다.
- 추가한 댓글의 목적: disabled-by-default Firebase Admin adapter, 목적별 Token 검증, FirebaseIdentity·Enrollment CAS 구현 요약과 54 suite·353개 테스트 성공, 실제 Mongo index·ADC/Workload Identity·모바일 Provider가 남은 위험임을 기록했다.
- 변경한 상태: TMI-91은 `해야 할 일`에서 status ID `10003`의 `완료`로 전환됐고 Resolution도 `완료`임을 확인했다. 다른 Jira 필드는 변경하지 않았다.
- 승인 여부: 사용자가 앞서 공개된 댓글 초안을 확인한 뒤 Jira 종료를 명시적으로 요청했다. 이 승인은 댓글 등록과 완료 전환에 적용했으며 새 Jira 생성이나 후속 구현 승인은 포함하지 않는다.
- 실행한 테스트와 결과: 이번 turn은 병합·Jira 상태 확인과 문서 갱신만 수행해 Gradle 테스트를 재실행하지 않았다. 병합된 PR의 최종 검증 결과는 `./gradlew clean test` 54개 suite·353개 테스트 성공, skip·failure·error 0이다. 종료 전 문서 diff·whitespace와 지정 marker 단일 존재를 정적으로 검증한다.
- 유지한 계약: Firebase는 credential broker이고 Identity는 canonical UUID User·자체 RS256 Access Token·RefreshSession·JWKS를 계속 소유한다. Firebase UID·provider subject·email·phone은 자동 merge key가 아니며 Firebase ID Token은 Learning Core로 전달하지 않는다. Secret·Token·Password·전화번호·fingerprint·실제 Key·전체 MongoDB URI와 사용자 개인정보를 Jira나 기록에 추가하지 않았다.
- 결정사항: TMI-91은 병합과 Jira 종료가 모두 완료됐다. 실제 Firebase login/signup과 production flag는 계속 비활성이며 외부 모바일·SMS·Identity Platform·Apple lifecycle gate 전에는 열지 않는다.
- 위험 요소: Stage 4에서 HMAC key rotation과 retained alias를 잘못 설계하면 version 교차 중복 가입이 가능하다. 실제 MongoDB unique index와 key lifecycle, Firebase phone link·번호 충돌·withdrawal cleanup은 staging과 격리 Firebase 환경에서 후속 검증해야 한다.
- 다음 작업: Stage 4 Jira 생성 전 정확한 제목·범위·완료 조건·제외 범위를 사용자에게 제시하고 승인을 받는다. 승인 후 PhoneIdentity·PhoneFingerprintAlias와 versioned HMAC/index/test만 구현한다.

## 2026-08-14 — 8월 11·12·13일 작업 기록 일자별 정리

<!-- codex-turn:019ffdf0-be38-7650-b8c7-741b2e0981da -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`e25adbc` 기준, Codex commit·push 미수행)
- Jira: `TMI-88`, `TMI-89`, `TMI-90`, `TMI-91` 기존 기록을 정리했으며 Jira 조회·댓글·상태·필드 변경은 수행하지 않았다.
- 작업 목표: WORKLOG와 Git 이력을 근거로 2026-08-11, 2026-08-12, 2026-08-13 작업을 날짜별로 구분해 현재 상태 문서에서 한눈에 확인할 수 있도록 정리한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. WORKLOG 과거 기록과 기존 TMI-91 종료 기록은 수정하거나 삭제하지 않고 이번 항목만 파일 끝에 append했다.
- 구현 내용: 8월 11일을 Sentry staging one-shot 검증·수신 확인·임시 코드 제거와 Google·Kakao·Apple 소셜 인증, MEMBER 전화번호 인증, 무료 모의고사 경계의 단계별 계획 수립 작업으로 묶었다.
- 구현 내용: 8월 12일은 WORKLOG 날짜 항목, UUIDv7 turn marker의 KST 생성 시각, Git 커밋 이력에서 해당 날짜의 저장소 작업을 확인할 수 없으므로 `확인 가능한 저장소 작업 기록 없음`으로 명시했다. 11일과 13일 작업을 12일로 추정 배정하지 않았다.
- 구현 내용: 8월 13일을 TMI-88 SocialIdentity 기반, TMI-89 UserAccountType 호환 확장, TMI-90 Firebase broker 조건부 ADR·격리 PoC, TMI-91 Firebase broker foundation의 생성·구현·검증·병합 및 확인된 Jira 종료 상태 중심으로 정리했다.
- 실행한 테스트와 결과: 애플리케이션 코드 변경이 없는 문서 정리 작업이므로 Gradle 테스트를 재실행하지 않았다. 날짜 판정에는 WORKLOG 제목·본문, UUIDv7 turn marker의 Asia/Seoul 시각과 `git log --all`의 2026-08-11~13 커밋 시각을 대조했다. 종료 전 문서 diff·whitespace, WORKLOG EOF append와 지정 marker 단일 존재를 정적으로 검증한다.
- 유지한 계약: 실제 사용자 ID와 JWT `sub`는 canonical UUID 문자열이고 Identity가 RS256 Access Token·RefreshSession·JWKS를 소유한다. Firebase는 credential broker에 한정하며 Firebase UID·provider subject·email·phone은 자동 merge key가 아니다. 시험·채점·Entitlement 구현을 Identity에 추가하지 않았고 Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 과거 WORKLOG는 append-only 규칙에 따라 중복 블록을 포함해 그대로 보존하고, 날짜별 통합 요약은 CURRENT_STATE의 별도 섹션을 기준으로 사용한다. 8월 12일은 근거 없는 작업일 보정을 하지 않는다.
- 위험 요소: 8월 12일에 저장소 밖에서 수행했거나 기록되지 않은 작업은 현재 자료만으로 확인할 수 없다. 향후 별도 근거가 발견되면 과거 기록을 수정하지 않고 새로운 보완 항목으로 출처와 함께 append해야 한다.
- 다음 작업: Stage 4 Jira 생성 전 `PhoneIdentity`·versioned HMAC fingerprint 범위와 완료 조건·제외 범위를 사용자에게 먼저 제시하고 승인을 받는다. TMI-90 종료 댓글과 완료 전환도 기존 승인 대기 상태를 유지한다.

## 2026-08-14 — 날짜별 정리 전달 위치 정정

<!-- codex-turn:019ffdf3-5831-7a22-b0ec-6a7f00ddbd86 -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`e25adbc` 기준, Codex commit·push 미수행)
- 작업 목표: 8월 11·12·13일 정리는 저장소 상태 문서가 아니라 블로그 초안으로 사용할 대화 답변이라는 사용자 의도를 반영한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 직전 turn에 CURRENT_STATE에 추가했던 날짜별 블로그용 요약 섹션은 제거했고, WORKLOG 과거 기록은 수정하거나 삭제하지 않은 채 이번 정정 항목만 파일 끝에 append했다.
- 구현 내용: 날짜별 블로그 원문은 저장소 문서에 보존하지 않고 이번 대화의 최종 답변으로만 제공한다. CURRENT_STATE는 Identity Service의 현재 구현·진행 상태만 유지한다.
- 실행한 테스트와 결과: 애플리케이션 코드 변경이 없는 문서 정정 작업이므로 Gradle 테스트를 실행하지 않았다. `git diff --check`, CURRENT_STATE의 날짜별 요약 heading 부재, 지정 turn marker 단일 존재와 WORKLOG EOF append를 정적으로 검증한다.
- 유지한 계약: Identity 도메인, UUID userId/JWT `sub`, RS256·JWKS, RefreshSession과 Firebase broker 경계를 변경하지 않았다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 블로그용 요약은 사용자 요청대로 채팅 창에서 제공한다. 저장소에는 프로젝트 운영 규칙상 필요한 작업 감사 기록만 남기며 블로그 본문을 중복 저장하지 않는다.
- 위험 요소: 8월 12일에는 WORKLOG 날짜 항목, turn marker 시각, Git 커밋에서 확인되는 저장소 작업이 없어 블로그에도 작업을 추정해 채우지 않는다.
- 다음 작업: 사용자가 원하는 문체나 분량에 맞춰 채팅에서 블로그 초안을 다듬는다. TMI-90 종료와 Stage 4 Jira는 기존 승인 대기 상태를 유지한다.

## 2026-08-14 — Stage 4 PhoneIdentity Jira 생성 시도

<!-- codex-turn:019ffe10-503d-77d1-a564-97e8678c4185 -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`e25adbc` 기준, Codex commit·push 미수행)
- Jira: 생성 예정이며 아직 이슈 키가 없다.
- 작업 목표: 앞서 공개한 Stage 4 범위에 대한 사용자 승인에 따라 `[Identity] PhoneIdentity 및 versioned HMAC fingerprint 기반 구축` Jira를 생성한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Jira 범위를 E.164 재검증, domain-separated HMAC-SHA-256 key registry의 정확히 하나인 `ACTIVE_WRITE`와 0개 이상인 `LOOKUP_ONLY` lifecycle, `PhoneIdentity`·`PhoneFingerprintAlias`, User당 활성 번호 하나, retained key version 전체의 동일 번호 중복 차단, `PHONE_ALREADY_LINKED`와 자동 merge 금지, 민감 전화정보 비로그 검증으로 확정했다.
- 구현 내용: Firebase SMS·OTP, 공개 Firebase exchange/signup, User·RefreshSession finalize Transaction, Guest 승격·merge, TrialClaim·UserEntitlement와 benefit-scoped fingerprint, production Firebase 활성화는 Jira 제외 범위로 유지했다.
- 수행한 Jira 작업: 현재 tool runtime의 사용 가능한 도구에서 Atlassian 공식 MCP 호출이 제공되지 않아 Jira 생성·댓글·필드 수정·상태 전환을 수행하지 못했다. 로컬 조회에서는 공식 `atlassian` remote MCP 설정이 enabled 상태인 것까지만 확인했으며 비공식 REST 호출, 브라우저 자동화나 다른 Jira provider로 우회하지 않았다.
- 추가한 댓글의 목적: Jira 이슈가 생성되지 않아 댓글을 추가하지 않았다.
- 변경한 상태: 생성된 Jira가 없어 변경한 상태나 Resolution이 없다.
- 승인 여부: 사용자가 직전 답변에서 제시된 Stage 4 목표·모델·DB 제약·테스트·제외 범위를 확인한 뒤 Jira 생성을 명시적으로 요청했다. 구현, Git commit·push 또는 production 기능 활성화는 승인 범위에 포함되지 않는다.
- 실행한 테스트와 결과: 애플리케이션 코드 변경이 없는 Jira 생성 시도와 문서 기록 작업이므로 Gradle 테스트를 실행하지 않았다. 종료 전 `git diff --check`, WORKLOG EOF append와 지정 marker 단일 존재를 정적으로 검증한다.
- 유지한 계약: Firebase는 credential broker이고 Identity는 canonical UUID User·자체 RS256 Access Token·RefreshSession·JWKS를 소유한다. 전화번호는 로그인 ID·canonical userId·자동 merge key가 아니며, Identity에 시험·TrialClaim·UserEntitlement 코드를 추가하지 않았다. Secret·Token·Password·전화번호·fingerprint·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: Jira 변경은 Atlassian 공식 MCP로만 수행한다는 저장소 규칙을 지키기 위해 연결이 제공되지 않는 상태에서 생성 성공으로 간주하거나 다른 경로로 우회하지 않는다.
- 위험 요소: 공식 MCP 호출 도구가 복구되기 전에는 이슈 키 발급과 Jira 설명·상태 재검증을 완료할 수 없다. HMAC key rotation과 retained alias 설계가 잘못되면 key version 교차 중복 가입이 가능하므로 후속 구현에서 실제 Mongo index와 mixed writer gate를 함께 검증해야 한다.
- 다음 작업: Atlassian 공식 MCP 도구를 다시 사용할 수 있는 세션에서 승인된 내용 그대로 TMI `작업` Jira를 기본 우선순위 `Medium`으로 생성하고, 생성 결과를 재조회한 뒤 발급된 이슈 키를 CURRENT_STATE와 새 WORKLOG 항목에 기록한다.

## 2026-08-14 — Stage 4 PhoneIdentity Jira TMI-92 생성

<!-- codex-turn:019ffe13-e8f2-7c32-a883-068a0205c578 -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`e25adbc` 기준, Codex commit·push 미수행)
- Jira: `TMI-92`
- 작업 목표: 공식 Atlassian MCP 도구가 다시 제공된 세션에서 앞서 승인된 Stage 4 PhoneIdentity Jira 생성을 이어서 완료하고 저장 결과를 확인한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Jira `TMI-92` `[Identity] PhoneIdentity 및 versioned HMAC fingerprint 기반 구축`을 TMI `작업` 유형과 우선순위 `Medium`으로 생성했다. 범위는 서버 E.164 재검증, 정확히 하나의 `ACTIVE_WRITE`와 0개 이상의 `LOOKUP_ONLY`를 갖는 domain-separated HMAC-SHA-256 key registry, `PhoneIdentity`·`PhoneFingerprintAlias`, User당 활성 번호 하나, retained version 전체의 동일 번호 중복 차단, `PHONE_ALREADY_LINKED`, 재시도 멱등성·동시성·rotation·민감정보 비노출 테스트다.
- 구현 내용: Firebase SMS·OTP, 공개 exchange/signup, User·RefreshSession finalize Transaction, Guest 승격·merge, 전화번호 로그인·자동 merge, TrialClaim·UserEntitlement·benefit-scoped fingerprint, production Firebase 활성화와 실제 key·전화번호·개인정보 저장은 명시적 제외 범위로 유지했다.
- 수행한 Jira 작업: Atlassian 공식 MCP로 접근 resource, TMI 생성 권한, `작업` 유형 ID `10003`, 전체 생성 필드와 기본 우선순위 `Medium`을 읽기 전용 재확인하고, JQL로 `PhoneIdentity` 제목의 중복 이슈가 없음을 확인한 뒤 승인된 이슈 한 건만 생성했다. 생성 후 `TMI-92`를 읽기 전용 재조회해 제목·설명·유형·우선순위·상태·Resolution·담당자·라벨·컴포넌트를 확인했다.
- 추가한 댓글의 목적: Jira 댓글은 추가하지 않았다.
- 변경한 상태: 신규 이슈의 기본 상태 `해야 할 일`을 유지했고 상태 전환은 수행하지 않았다. Resolution은 없으며 담당자·라벨·컴포넌트도 지정하지 않았다.
- 승인 여부: 사용자는 직전 turn에서 공개된 Stage 4 목표·모델·DB 제약·테스트·제외 범위를 확인한 뒤 Jira 생성을 승인했고, 이번 `지라 생성 이어서 해줘`로 중단됐던 같은 생성을 계속하도록 명시했다. 구현, Jira 댓글·상태 전환, Git commit·push와 production 기능 활성화는 승인 범위에 포함되지 않는다.
- 실행한 테스트와 결과: 애플리케이션 코드 변경이 없는 Jira 생성과 문서 갱신 작업이므로 Gradle 테스트를 실행하지 않았다. Jira 후속 조회에서 제목·본문·`작업`·`Medium`·`해야 할 일`·Resolution 없음과 빈 담당자·라벨·컴포넌트를 확인했다. 종료 전 `git diff --check`, WORKLOG EOF append와 지정 turn marker 단일 존재를 정적으로 검증한다.
- 유지한 계약: 실제 사용자 ID와 JWT `sub`는 Identity canonical UUID 문자열이고 Identity가 자체 RS256 Access Token·RefreshSession·JWKS를 소유한다. 전화번호는 로그인 ID·canonical userId·자동 merge key가 아니며 Firebase ID Token·Firebase UID를 Learning Core로 전달하지 않는다. 시험·채점·Entitlement 코드를 추가하지 않았고 Secret·Token·Password·전화번호·fingerprint·실제 Key·전체 MongoDB URI와 사용자 개인정보를 Jira나 기록에 추가하지 않았다.
- 결정사항: TMI-92는 provider-neutral PhoneIdentity 저장·versioned fingerprint·alias/index/rotation 기반까지만 다룬다. 후속 공개 Firebase 가입과 Entitlement benefit fingerprint는 별도 계약과 Jira로 분리한다.
- 위험 요소: HMAC key rotation과 alias backfill 또는 mixed-version writer 배포 순서를 잘못 설계하면 version 교차 중복 귀속이 가능하다. 실제 MongoDB unique/partial index, 동시 연결 경쟁, WITHDRAWN 번호 해제와 tombstone 정책은 구현 시 격리 테스트와 staging 검증 계획으로 고정해야 한다.
- 다음 작업: 구현 요청을 받으면 먼저 Atlassian 공식 MCP로 TMI-92 설명·완료 조건·제외 범위를 재조회하고 AGENTS.md·기존 계약과 대조한다. Jira 댓글이나 상태 전환, commit·push는 별도 승인 전 수행하지 않는다.

## 2026-08-14 — TMI-92 PhoneIdentity·versioned HMAC 기반 구현

<!-- codex-turn:019ffe1b-3775-7e13-ad44-70e373d267a3 -->

- 날짜: 2026-08-14
- 브랜치: `feat/TMI-92-phone-identity-hmac-foundation` (`e25adbc`에서 시작, Codex commit·push 미수행)
- Jira: TMI-92
- 작업 목표: 승인된 Stage 4 범위에 따라 provider-neutral `PhoneIdentity`·retained version alias·domain-separated HMAC fingerprint 기반과 무결성·rotation·비노출 테스트를 구현한다.
- 변경 파일: `.env.example`, `src/main/resources/application.yml`, `src/test/resources/application-test.yml`, `src/main/java/web/tosunsaeng/identity/domain/auth/{application/phone,domain/entity,domain/enums,domain/phone,domain/repository,infrastructure/phone,exception}/**`, 대응하는 `src/test/java/web/tosunsaeng/identity/domain/auth/**/Phone*Tests.java`, `docs/contracts/phone-identity-key-rotation.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 입력을 서버에서 plus-prefixed E.164로 재검증하고 허용된 공백·구분자 표기를 canonical 숫자열로 정규화한다. 정확히 하나의 `ACTIVE_WRITE`, 0개 이상의 `LOOKUP_ONLY`, 중복 없는 version과 32바이트 이상 Base64 key를 fail-closed 검증하며 `tosunsaeng:identity:phone-identity:v1` domain·NUL separator·HMAC-SHA-256·Base64URL without padding으로 retained fingerprint를 계산한다.
- 구현 내용: fingerprint 설정은 `PHONE_IDENTITY_FINGERPRINT_ENABLED=false` 기본값과 외부 `PHONE_IDENTITY_FINGERPRINT_KEY_RING` 이름만 제공한다. 실제 key material을 저장소에 추가하지 않았고 configuration·key·fingerprint·entity 문자열 표현에서 material과 fingerprint를 redaction했다.
- 구현 내용: `PhoneIdentity`는 canonical UUID userId, current key version·fingerprint, ACTIVE/RELEASED, verified/created/updated/released 시각과 `@Version`을 저장한다. `PhoneFingerprintAlias`는 identity·user·retained version·fingerprint와 ACTIVE/RELEASED lifecycle을 저장하며 raw phone·last4·Firebase UID·email·provider subject를 보유하지 않는다.
- 구현 내용: ACTIVE User당 identity 하나의 partial unique, ACTIVE `(fingerprintKeyVersion, phoneFingerprint)`와 `(phoneIdentityId, fingerprintKeyVersion)` partial unique, user/status lookup index를 선언했다. retained version candidate를 한 번에 조회하고 기존 identity alias를 원자적으로 release하는 custom Repository를 추가했다.
- 구현 내용: application service와 별도 Mongo Transaction service를 구성했다. 신규 claim, 동일 User·동일 번호 멱등 성공, legacy alias로 owner를 확인한 새 ACTIVE_WRITE alias backfill과 current rotation, 번호 교체 시 기존 identity·aliases release 후 신규 claim, 다른 User의 안정적인 `PHONE_ALREADY_LINKED`, duplicate/optimistic concurrency 재시도와 최종 `PHONE_IDENTITY_CONFLICT`를 구현했다.
- 구현 내용: `docs/contracts/phone-identity-key-rotation.md`에 retained version overlap, 새 key LOOKUP_ONLY 선배포, 전체 writer 동기화 뒤 ACTIVE_WRITE 전환, rollback, legacy key 제거 gate와 기존 raw phone 없는 임의 backfill 금지를 기록했다.
- 수행한 Jira 작업: 구현 전에 Atlassian 공식 MCP로 TMI-92의 제목·설명·완료 조건·제외 범위·상태·우선순위·Resolution을 읽기 전용 재조회하고 AGENTS.md·기존 계약과 충돌이 없음을 확인했다. Jira 이슈·댓글·상태·필드는 변경하지 않았다.
- 추가한 댓글의 목적: 구현 요약·변경 파일·전체 테스트 결과·실제 Mongo partial unique/Transaction과 운영 key rotation의 남은 위험을 전달하는 종료 댓글 초안만 준비하며 자동 등록하지 않는다.
- 변경한 상태: Jira TMI-92는 기존 `해야 할 일`, 우선순위 `Medium`, Resolution 없음 상태를 유지한다. PR 병합을 확인하지 않았으므로 완료 전환하지 않았다.
- 승인 여부: 사용자가 `구현 시작해줘`로 TMI-92 구현을 명시적으로 요청했다. Jira 댓글·상태 전환, Git commit·push, production key 주입·feature 활성화와 외부 Firebase/SMS 설정 변경은 승인 범위에 포함되지 않았다.
- 실행한 테스트와 결과: 신규 PhoneIdentity 타깃 테스트 31개가 성공했다. 최종 `./gradlew clean test`는 62개 suite·381개 테스트가 skip 0, failure 0, error 0으로 성공했다. 실제 Firebase·SMS Provider·Atlas를 호출하지 않고 Repository Mock과 프로세스 내부 인메모리 Mongo만 사용했다.
- 실행한 테스트와 결과: `git diff --check`, application/domain phone package의 Firebase SDK import 부재, PhoneIdentity entity의 raw phone·last4 필드 부재, Identity에 TrialClaim·UserEntitlement·시험·채점 코드가 추가되지 않았음을 정적으로 확인했다.
- 유지한 계약: 실제 userId와 JWT `sub`는 Identity canonical UUID 문자열이고 기존 RS256·`kid`·issuer·`tosunsaeng-learning-core` audience·JWKS·RefreshSession을 변경하지 않았다. phone은 로그인 ID·canonical 선택·자동 merge key가 아니며 Firebase ID Token·UID를 Learning Core로 보내지 않는다. Firebase SMS·OTP, 공개 exchange/signup, Guest 승격·merge와 Entitlement/benefit fingerprint를 추가하지 않았다.
- 결정사항: raw phone과 last4를 저장하지 않고 current fingerprint와 retained alias만 저장한다. SUSPENDED User 점유는 alias 상태와 분리해 유지하고 WITHDRAWN 해제는 Firebase unlink/delete·tombstone을 포함하는 별도 lifecycle로 남긴다. 기능과 key ring은 공개 가입 흐름이 준비될 때까지 기본 비활성이다.
- 위험 요소: 인메모리 Mongo는 partial unique filter 상태 변경 후 같은 값 재삽입을 완전히 지원하지 않아 index filter·ACTIVE 중복 거절·release update와 transaction service 교체 순서를 나눠 검증했다. 실제 MongoDB의 ACTIVE→RELEASED 후 신규 claim, 다중 collection rollback·write conflict와 custom Repository wiring은 staging에서 재검증해야 한다.
- 위험 요소: raw phone을 저장하지 않으므로 기존 identity 전체의 새 version alias를 임의로 backfill할 수 없다. 모든 writer가 retained version overlap을 유지하지 않거나 legacy key를 참조 중 제거하면 version 교차 중복 귀속이 가능하며, key reference count·비상 rotation 자동화는 아직 없다.
- 다음 작업: 사용자가 변경을 검토한 뒤 직접 commit·push하고 PR을 생성한다. 병합 전 Jira를 Done으로 변경하지 않으며 Jira 댓글이나 상태 전환은 정확한 payload를 먼저 제시하고 별도 승인을 받은 뒤 수행한다. 후속 Stage 5에서만 Firebase verified phone proof와 User·FirebaseIdentity·PhoneIdentity·SocialIdentity·RefreshSession finalize Transaction을 연결한다.

## 2026-08-14 — Auth 패키지 경계 분석

<!-- codex-turn:019ffe37-c281-7230-b964-ca1c84d4f48d -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`4e8f46d`, GitHub PR #20 TMI-92 병합 확인, 이번 turn Codex commit·push 미수행)
- Jira: TMI-92 구현 결과를 포함한 현재 구조를 분석하고 공식 Atlassian MCP로 상태만 읽기 전용 재조회했다. Jira 수정·댓글·상태 전환은 수행하지 않았다.
- 작업 목표: `domain.auth` 아래 Firebase·PhoneIdentity 코드가 많아진 현재 패키지 구조를 점검하고, 이를 `auth`와 같은 최상위 위치로 옮기는 안의 경계와 더 안전한 정리 방향을 제안한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드와 테스트는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 현재 main source 기준 `domain.auth` 78개, `domain.user` 26개 파일을 확인했다. Auth는 local email 가입·로그인, Guest, Access/RefreshSession, Firebase verifier·enrollment, FirebaseIdentity·SocialIdentity, PhoneIdentity를 horizontal `application/domain/infrastructure`에 함께 배치해 기능 하나를 추적할 때 여러 package를 오가야 한다.
- 구현 내용: `domain.firebase`를 `domain.auth`와 동급으로 올리면 외부 Provider 기술명이 내부 bounded context가 되고 Firebase 교체 시 package 경계가 도메인보다 기술에 종속된다. `domain.phone`도 SMS/통신 일반 도메인처럼 보이지만 현재 책임은 verified phone ownership·fingerprint uniqueness이므로 이름과 실제 책임이 어긋난다.
- 구현 내용: 우선 추천은 top-level sibling 추가가 아니라 `auth` 내부를 capability-first vertical slice로 재구성하는 것이다. 예시는 `auth/local`, `auth/session`, `auth/firebase`, `auth/phoneidentity`이며 각 slice 내부에 필요한 application·domain·infrastructure를 함께 둔다. 더 큰 bounded context 분리는 Stage 5 orchestration과 의존 방향이 확정된 뒤 `authentication`, `account`, `linkedidentity/session` 같은 business 이름을 기준으로 별도 Jira에서 검토한다.
- 수행한 Jira 작업: 공식 Atlassian MCP로 TMI-92의 제목, 우선순위 `Medium`, 상태 `해야 할 일`, Resolution 없음을 읽기 전용 확인했다. Jira 댓글 초안이나 상태 변경 payload는 만들지 않았고 변경 호출은 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태를 변경하지 않았다.
- 승인 여부: 사용자는 패키지 구조에 대한 의견을 요청했으며 코드 이동, Jira 수정, commit·push는 요청하거나 승인하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 구조 분석과 문서 갱신이므로 Gradle 테스트를 재실행하지 않았다. 직전 TMI-92 최종 검증은 62개 suite·381개 테스트 성공이며 이번 종료 전 `git diff --check`, WORKLOG EOF append와 지정 marker 단일 존재를 정적으로 검증한다.
- 유지한 계약: Identity 소유 경계, canonical UUID userId/JWT `sub`, RS256·JWKS·RefreshSession, Firebase broker와 PhoneIdentity 책임을 변경하지 않았다. 시험·채점·Entitlement 코드를 추가하지 않았고 Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 현재 규모에서 `firebase`와 `phone`을 곧바로 최상위 `domain` sibling으로 분리하는 안은 추천하지 않는다. 문제의 핵심은 Auth 크기 자체보다 기능이 horizontal layer에 분산된 구조이므로 vertical slice가 우선이다. 이미 병합된 TMI-92와 package 이동을 소급해 섞지 않고 별도 후속 범위로 다룬다.
- 위험 요소: package refactor를 Stage 5 기능 구현과 동시에 수행하면 diff가 커지고 Mongo `_class` FQCN, Spring Data custom Repository fragment naming, component scan·configuration wiring과 테스트 import가 함께 바뀐다. Mongo 문서에 `_class`가 저장된 환경에서는 단순 Java package 이동도 데이터 호환성 검토가 필요하다.
- 다음 작업: 사용자가 구조 개선을 원하면 별도 Jira/PR로 package-only refactor 범위와 목표 tree를 먼저 확정한다. 구조 이동은 동작 변경 없이 전체 테스트와 Mongo `_class` 호환 전략을 포함해 수행한다. TMI-92 Jira 종료는 댓글·전환 payload를 별도로 제시하고 승인받기 전 수행하지 않는다.

## 2026-08-14 — Auth vertical slice 리팩터링 Jira 생성 초안 준비

<!-- codex-turn:019ffe3b-3122-73e3-b22c-e5092f7340e4 -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`4e8f46d`, GitHub PR #20 병합 기준, Codex commit·push 미수행)
- 작업 목표: Auth 패키지 과밀과 horizontal layer 분산을 해소하는 capability-first vertical slice 리팩터링을 신규 TMI Jira로 생성하기 전에 정확한 payload와 데이터 호환 완료 조건을 준비한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드와 테스트는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Jira 제목을 `[Identity] Auth 패키지 capability-first vertical slice 리팩터링`, 유형을 TMI `작업`, 우선순위를 기본 `Medium`으로 준비했다. `domain.firebase`·`domain.phone` 최상위 분리는 제외하고 `auth` 내부를 registration/local, session, federation/Firebase, phoneidentity, common capability로 재배치하는 package-only 범위로 제한했다.
- 구현 내용: public API·DTO·오류 code·환경변수·Mongo collection/index·JWT/RefreshSession 동작을 바꾸지 않고 import, component scan, configuration, Transaction proxy와 Spring Data custom Repository fragment wiring을 보존하도록 완료 조건을 구성했다. Mongo persistent entity 이동은 기존 `_class` FQCN 문서의 가독성을 깨뜨리지 않는 legacy alias/매핑 테스트가 있을 때만 허용하고, 호환 전략이 없으면 entity FQCN을 안정 경계에 남기도록 했다.
- 수행한 Jira 작업: Atlassian 공식 MCP로 TMI 생성 권한, `작업` 유형 ID `10003`, 생성 필드 20개와 기본 우선순위 `Medium`을 읽기 전용 재확인했다. JQL로 패키지·vertical slice·Auth 구조 관련 중복 이슈가 없음을 확인했으며 Jira 생성·수정·댓글·상태 전환은 수행하지 않았다.
- 추가한 댓글의 목적: Jira 이슈가 아직 생성되지 않아 댓글을 추가하지 않았다.
- 변경한 상태: 생성된 신규 Jira가 없어 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 새 Jira 생성을 요청했지만 저장소 규칙상 실제 생성 전에 정확한 payload를 먼저 제시하고 별도 승인을 기다린다.
- 실행한 테스트와 결과: 코드 변경이 없는 Jira 초안·문서 갱신 작업이므로 Gradle 테스트를 실행하지 않았다. 직전 `develop`의 TMI-92 검증 결과는 62개 suite·381개 테스트 성공이며 종료 전 `git diff --check`와 WORKLOG EOF append를 정적으로 검증한다.
- 유지한 계약: Identity 도메인, canonical UUID userId/JWT `sub`, RS256·JWKS·RefreshSession, Firebase broker·PhoneIdentity 책임을 변경하지 않는다. 시험·채점·Entitlement 코드를 추가하지 않고 Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 Jira 초안이나 기록에 포함하지 않았다.
- 결정사항: 신규 Jira는 구조 변경만 다루며 Stage 5 Firebase exchange/signup 또는 새 인증 기능과 함께 구현하지 않는다. top-level 기술 package를 늘리지 않고 capability-first vertical slice를 적용한다.
- 위험 요소: Java package 이동은 Mongo `_class` FQCN, Spring Data Repository custom implementation 이름, component scan, Bean 이름, Transaction proxy와 외부 FQCN 참조를 깨뜨릴 수 있다. 실제 데이터 호환 전략 없이 persistent entity를 이동하거나 구조 리팩터링과 기능 변경을 섞으면 회귀 원인 분리가 어려워진다.
- 다음 작업: 사용자가 최종 Jira payload를 승인하면 TMI `작업` 한 건을 `Medium`으로 생성하고 제목·설명·상태·Resolution·담당자·라벨을 재조회한 뒤 발급된 이슈 키를 CURRENT_STATE와 새 WORKLOG 항목에 기록한다.

## 2026-08-14 — Auth vertical slice 리팩터링 Jira TMI-93 생성

<!-- codex-turn:019ffe48-93a1-7b25-a137-5e35d642b8aa -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`4e8f46d`, GitHub PR #20 병합 기준, Codex commit·push 미수행)
- Jira: TMI-93
- 작업 목표: 승인된 Auth capability-first vertical slice 리팩터링을 별도 Jira로 생성하고 저장된 범위와 초기 상태를 확인한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드와 테스트는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Jira `TMI-93` `[Identity] Auth 패키지 capability-first vertical slice 리팩터링`을 TMI `작업` 유형과 우선순위 `Medium`으로 생성했다. `auth.registration`, `auth.local`, `auth.session`, `auth.federation`, `auth.phoneidentity`, `auth.common` 경계의 package-only 재배치와 테스트 정리를 범위로 기록했다.
- 구현 내용: public API·DTO·오류 코드·JWT/JWKS/RefreshSession, Mongo collection/index/document field, Spring component scan·Transaction proxy·custom Repository fragment wiring을 유지하도록 완료 조건을 고정했다. Mongo `_class` legacy FQCN 호환 테스트를 요구하고 안전한 호환 전략이 없으면 persistent entity를 기존 안정 패키지에 유지하도록 기록했다.
- 수행한 Jira 작업: 사용자가 공개된 payload를 `어 생성해줘`로 승인한 뒤 Atlassian 공식 MCP로 Jira 한 건을 생성했다. 생성 직후 TMI-93을 재조회해 제목·설명·유형 `작업`·우선순위 `Medium`·상태 `해야 할 일`·Resolution 없음·담당자 없음·빈 라벨을 확인했다.
- 추가한 댓글의 목적: Jira 댓글은 추가하지 않았다.
- 변경한 상태: 신규 이슈의 기본 상태 `해야 할 일`을 유지했으며 상태 전환은 수행하지 않았다. Resolution은 없고 담당자·라벨도 지정하지 않았다.
- 승인 여부: 사용자가 사전에 제시된 제목·범위·완료 조건·제외 범위를 확인한 뒤 Jira 생성을 명시적으로 승인했다. 구현, Jira 댓글·상태 전환, Git commit·push는 승인 범위에 포함되지 않는다.
- 실행한 테스트와 결과: 애플리케이션 코드 변경이 없는 Jira 생성과 문서 갱신 작업이므로 Gradle 테스트를 실행하지 않았다. Jira 후속 조회로 저장 결과를 확인했고 종료 전 `git diff --check`, WORKLOG EOF append와 turn marker 단일 존재를 정적으로 검증한다.
- 유지한 계약: Identity 도메인, canonical UUID userId/JWT `sub`, RS256·`kid`·issuer·`tosunsaeng-learning-core` audience, JWKS·RefreshSession 계약을 변경하지 않았다. 시험·채점·Entitlement 코드를 추가하지 않았고 Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 Jira나 기록에 포함하지 않았다.
- 결정사항: Firebase와 phone을 최상위 기술 패키지로 분리하지 않고 Auth 내부 capability 기준으로 정리한다. 구조 변경과 Stage 5 기능 구현은 분리하며 persistent entity 이동은 저장 데이터 호환성을 증명할 수 있을 때만 수행한다.
- 위험 요소: package 이동은 Mongo `_class` FQCN, Spring Data custom Repository implementation 탐색, Bean 이름·component scan·Transaction proxy와 외부 FQCN 참조를 깨뜨릴 수 있다. 실제 데이터 호환 전략 없이 persistent entity를 이동하면 기존 문서 역직렬화가 실패할 수 있다.
- 다음 작업: 구현 요청을 받으면 먼저 Atlassian 공식 MCP로 TMI-93 설명·완료 조건·제외 범위를 재조회하고 AGENTS.md·JWT 계약과 대조한다. Jira 댓글·상태 전환과 Git commit·push는 별도 승인 전 수행하지 않는다.

## 2026-08-14 — TMI-93 Auth capability-first vertical slice 리팩터링 구현

<!-- codex-turn:019ffe40-b23f-72c3-a0e5-c5e28fb1b0bf -->

- 날짜: 2026-08-14
- 브랜치: `refactor/TMI-93-auth-capability-vertical-slice` (`4e8f46d`에서 시작, Codex commit·push 미수행)
- Jira: TMI-93
- 작업 목표: Auth의 horizontal package를 capability-first vertical slice로 재구성하되 외부 API·JWT·RefreshSession·Mongo 저장 계약과 Spring wiring을 그대로 유지한다.
- 변경 파일: `src/main/java/web/tosunsaeng/identity/domain/auth/{common,registration,local,session,federation,phoneidentity}/**`, 기존 Auth 영속 엔티티의 이동된 타입 import, Auth 타입을 사용하는 User·Security·Observability 코드 import, 대응하는 `src/test/java/**`, 신규 `AuthPersistentTypeCompatibilityTests`, `AuthRepositoryFragmentWiringTests`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Auth Controller·공통 응답 변환·오류는 `common`, 이메일·Guest 가입은 `registration`, 이메일/비밀번호 로그인은 `local`, Access/RefreshSession·재발급·로그아웃은 `session`, Firebase·Social provider application/infrastructure/repository는 `federation`, PhoneIdentity application/domain/infrastructure/repository는 `phoneidentity`로 재배치했다. 운영 코드와 테스트의 package/import를 함께 갱신했다.
- 구현 내용: HTTP endpoint 7개, Request/Response JSON, OpenAPI, 오류 코드, 환경변수, Mongo collection/index/document field를 변경하지 않았다. 공통 Auth Controller는 여러 capability 서비스를 조합하는 transport 경계로 `common.api`에 유지했다.
- 구현 내용: Mongo `_class` 하위 호환을 위해 6개 persistent entity와 enum을 기존 `domain.auth.domain.entity`·`domain.auth.domain.enums` 안정 패키지에 유지했다. `package-info.java`에 이 compatibility island와 향후 명시적 type mapping 없는 entity 이동 금지를 기록했다.
- 구현 내용: registration slice가 session slice의 prepared issuance를 사용하므로 redacted `PreparedRefreshSession` record와 `RefreshSessionIssuer.prepare/savePrepared`를 slice 간 최소 public 계약으로 조정했다. Token 원문은 계속 자동 문자열 표현에서 redaction되고 DB에는 hash만 저장된다.
- 구현 내용: 실제 `MappingMongoConverter`로 legacy FirebaseIdentity `_class` 쓰기·Object 읽기를 검증하고 6개 Auth 영속 타입 FQCN을 고정했다. 실제 Spring Repository 스캔으로 이동된 FirebaseEnrollmentAttempt·PhoneFingerprintAlias custom implementation 탐색과 RefreshSession Repository wiring을 검증했다.
- 수행한 Jira 작업: 구현 전에 Atlassian 공식 MCP로 TMI-93의 설명·완료 조건·제외 범위·상태·우선순위·Resolution을 읽기 전용 재조회하고 AGENTS.md·JWT 계약과 충돌이 없음을 확인했다. Jira 이슈·댓글·상태·필드는 변경하지 않았다.
- 추가한 댓글의 목적: package 재배치, persistent entity FQCN 유지, 전체 테스트 결과와 외부 FQCN 참조 가능성의 남은 위험을 전달하는 종료 댓글 초안만 준비하며 자동 등록하지 않는다.
- 변경한 상태: Jira TMI-93은 기존 `해야 할 일`, 우선순위 `Medium`, Resolution 없음 상태를 유지한다. PR 병합을 확인하지 않았으므로 완료 전환하지 않았다.
- 승인 여부: 사용자가 `진행해줘`로 TMI-93 구현을 명시적으로 요청했다. Jira 댓글·상태 전환, Git commit·push와 Stage 5 기능 구현은 승인 범위에 포함되지 않았다.
- 실행한 테스트와 결과: 신규 persistent type·Repository wiring과 기존 Transaction·PhoneIdentity Repository 타깃 테스트가 성공했다. 최종 `./gradlew clean test`는 64개 suite·384개 테스트가 failure 0, error 0, skipped 0으로 성공했다.
- 실행한 테스트와 결과: 운영·테스트 Java 파일의 package/path 일치, 제거 대상 legacy horizontal package 참조 부재, 기존 Auth POST endpoint 7개와 Mongo collection annotation 6개 유지, `git diff --check`를 정적으로 확인했다.
- 유지한 계약: canonical UUID userId/JWT `sub`, RS256·`kid`·issuer·`tosunsaeng-learning-core` audience, JWKS와 Opaque RefreshSession 동작을 변경하지 않았다. Firebase는 기본 비활성이고 phone은 로그인·자동 merge key가 아니다. 시험·채점·Entitlement를 추가하지 않았으며 Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: top-level `domain.firebase`·`domain.phone`은 만들지 않고 Auth bounded context 내부 capability를 1차 경계로 사용한다. persistent entity의 horizontal legacy package는 저장 데이터 호환을 위한 의도적 예외이며 향후 이동은 explicit type alias/mapping과 운영 데이터 검증을 별도 Jira로 수행한다.
- 위험 요소: 저장소 내부 import와 전체 테스트는 갱신됐지만 별도 모듈이 Java FQCN을 직접 참조한다면 새 capability package로 import 변경이 필요하다. 기존 Mongo 문서는 호환되지만 persistent entity를 이후 임의 이동하면 `_class` 역직렬화가 다시 깨질 수 있다.
- 다음 작업: 사용자가 변경을 검토한 뒤 직접 commit·push하고 PR을 생성한다. PR 병합 전 Jira를 Done으로 변경하지 않으며 Jira 댓글이나 상태 전환은 정확한 payload를 먼저 제시하고 별도 승인받아 수행한다.

## 2026-08-14 — TMI-93 리팩터링 빈 디렉터리 정리

<!-- codex-turn:f2dac9ff-caf2-46b5-b7b9-fa8c0f5547d9 -->

- 날짜: 2026-08-14
- 브랜치: `refactor/TMI-93-auth-capability-vertical-slice` (`4e8f46d`에서 시작, Codex commit·push 미수행)
- Jira: TMI-93
- 작업 목표: capability-first package 이동 뒤 워크스페이스에 남은 빈 디렉터리를 제거한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 빈 디렉터리는 Git 추적 파일이 없어 별도 파일 변경으로 표시되지 않으며 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 빈 `src/test/java/web/tosunsaeng/identity/domain/auth/local/application`, `src/test/java/web/tosunsaeng/identity/user/controller`, `src/test/java/web/tosunsaeng/identity/user/service`와 제거 후 비어 버린 상위 `auth/local`, `identity/user` 디렉터리를 `rmdir`로 제거했다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태를 변경하지 않았다.
- 승인 여부: 사용자가 빈 폴더를 모두 제거하도록 명시적으로 요청했다. 코드 변경, Jira 변경, Git commit·push는 승인 범위에 포함되지 않았다.
- 실행한 테스트와 결과: Java·설정·비즈니스 로직 변경이 없는 빈 디렉터리 정리이므로 Gradle 테스트를 재실행하지 않았다. 직전 TMI-93 전체 검증은 64개 suite·384개 테스트 성공이다. `.git`·`.gradle`·`build`·`.idea`를 제외한 저장소에서 빈 디렉터리가 0개임을 정적으로 확인했다.
- 유지한 계약: 소스 파일·API·Mongo·JWT·RefreshSession 계약을 변경하지 않았다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: VCS·캐시·빌드·IDE 내부 디렉터리는 도구가 관리하므로 빈 폴더 정리 대상에서 제외한다. 저장소 소스·문서 영역의 빈 폴더만 제거한다.
- 위험 요소: 제거한 디렉터리는 모두 비어 있어 코드나 Git 추적 데이터 손실 위험이 없다.
- 다음 작업: 사용자가 변경을 검토한 뒤 직접 commit·push하고 PR을 생성한다.

## 2026-08-14 — TMI-93 빈 디렉터리 정리 Hook 기록 동기화

<!-- codex-turn:019ffe51-4722-7743-9742-9bafef55447a -->

- 날짜: 2026-08-14
- 브랜치: `refactor/TMI-93-auth-capability-vertical-slice` (`4e8f46d`에서 시작, Codex commit·push 미수행)
- Jira: TMI-93
- 작업 목표: 빈 디렉터리 정리 turn을 Hook이 지정한 식별자로 WORKLOG EOF에 기록하고 CURRENT_STATE를 동기화한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Hook 지정 marker를 새 EOF 항목에 추가했다. 직전 작업에서 소스·문서 영역의 빈 디렉터리를 제거했으며 `.git`·`.gradle`·`build`·`.idea` 관리 영역은 제외했다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태를 변경하지 않았다.
- 승인 여부: Hook이 이번 turn 기록 보정을 요구했다. 코드 변경, Jira 변경, Git commit·push는 수행하지 않았다.
- 실행한 테스트와 결과: 기록 동기화만 수행했으므로 Gradle 테스트를 재실행하지 않았다. 직전 `./gradlew clean test`는 64개 suite·384개 테스트가 모두 성공했고, 빈 디렉터리 정리 뒤 정적 검증도 통과했다.
- 유지한 계약: 소스·API·Mongo·JWT·RefreshSession 계약을 변경하지 않았다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: Hook 지정 marker는 기존 항목을 수정하지 않고 새 WORKLOG EOF 항목으로 기록한다.
- 위험 요소: 문서 기록만 변경했으므로 애플리케이션 위험은 없다.
- 다음 작업: 사용자가 변경을 검토한 뒤 직접 commit·push하고 PR을 생성한다.

## 2026-08-14 — TMI-93 PR 병합 확인 및 Jira 완료 전환

<!-- codex-turn:019ffe54-8cb3-73b1-8569-773c4805b733 -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`063fdc7`, GitHub PR #21 병합 commit, Codex commit·push 미수행)
- Jira: TMI-93
- 작업 목표: TMI-93 구현 PR의 병합을 확인하고 사용자 요청에 따라 Jira를 완료한 뒤 다음 Identity 작업의 안전한 분할 순서를 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드와 테스트는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: GitHub PR #21 `refactor(TMI-93): reorganize auth package by capability`가 `develop`에 2026-08-14 병합됐고 feature commit `a0b63f4`가 merge commit `063fdc7`의 ancestor임을 확인했다.
- 구현 내용: 다음 범위는 Stage 5를 한 번에 구현하지 않고 5A login exchange·enrollment, 5B 신규 MEMBER signup finalize Transaction, production provider 활성화·legacy credential cutover로 분리하는 순서를 권장했다.
- 수행한 Jira 작업: Atlassian 공식 MCP로 TMI-93의 기존 `해야 할 일`·Resolution 없음·빈 댓글과 사용 가능한 `완료` transition ID `41`을 재확인했다. PR 병합 확인 후 사용자 요청에 따라 transition ID `41`만 적용하고 상태와 Resolution이 모두 `완료`임을 후속 조회했다.
- 추가한 댓글의 목적: Jira 댓글은 추가하지 않았다. 직전 turn에서 준비한 종료 댓글 초안도 자동 등록하지 않았다.
- 변경한 상태: TMI-93을 `해야 할 일`에서 `완료`로 전환했고 Resolution은 `완료`가 됐다. 다른 Jira 필드는 변경하지 않았다.
- 승인 여부: 사용자가 `좋아 지라 닫아주고`로 TMI-93 종료를 명시적으로 승인했다. Jira 댓글, 다른 이슈 변경, Git commit·push와 다음 기능 구현은 승인 범위에 포함되지 않았다.
- 실행한 테스트와 결과: 이번 turn은 PR·Jira 상태의 읽기 확인과 Jira 전환·문서 기록 작업이므로 Gradle 테스트를 재실행하지 않았다. 병합된 TMI-93의 최종 검증 결과는 64개 suite·384개 테스트가 failure 0, error 0, skipped 0으로 성공했다.
- 유지한 계약: canonical UUID userId/JWT `sub`, RS256·`kid`·issuer·`tosunsaeng-learning-core` audience, JWKS·RefreshSession·Mongo `_class` 호환 계약을 변경하지 않았다. Firebase를 활성화하거나 Stage 5 API·Entitlement 코드를 추가하지 않았고 Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 다음 권장 작업은 Stage 5A exchange/enrollment 기반이다. Firebase 인증만으로 User를 만들지 않고 기존 owner만 자체 Token을 발급하며, 미등록 UID는 동일 active FirebaseEnrollmentAttempt를 재사용한다. signup finalize와 production cutover는 별도 작업으로 둔다.
- 위험 요소: 실제 Firebase/mobile/SMS production gate와 PhoneEligibilityBindingOutbox consumer 계약이 준비되지 않은 상태에서 signup finalize 또는 legacy route cutover를 활성화하면 고아 Firebase User, 중복 내부 User, benefit binding 유실과 dual credential writer 위험이 있다.
- 다음 작업: 사용자가 원하면 Stage 5A Jira payload를 먼저 작성해 승인받은 뒤 생성한다. 구현 전에는 Firebase exchange 요청·응답, error taxonomy, enrollment 재사용·동시성, provider별 허용 정책과 disabled-by-default 조건을 완료 기준으로 고정한다.

## 2026-08-14 — Stage 5A Firebase exchange·enrollment 상세 설명

<!-- codex-turn:019ffe5b-a7d6-7371-8c9f-2cfc6e18562e -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`063fdc7`, GitHub PR #21 병합 기준, Codex commit·push 미수행)
- 작업 목표: Stage 5A Firebase login exchange·enrollment의 요청 검증, owner 분기, enrollment 멱등성, 비활성화와 제외 범위를 현재 계약·코드 기준으로 상세 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드와 테스트는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: `/api/v1/auth/firebase/exchange`는 Firebase 인증 정보를 공개 endpoint에서 받아 `LOGIN_EXCHANGE` 목적으로 검증한다. signature·issuer·audience·project·tenant·시간·revoke·disabled·recent-auth·provider allowlist를 통과하고 password·Google·Apple·승인된 Kakao 중 최소 primary method가 있어야 하며 phone sign-in은 거절한다.
- 구현 내용: `(firebaseProjectId, firebaseUid)` FirebaseIdentity owner가 있으면 canonical User를 조회해 ACTIVE MEMBER만 자체 RS256 Access와 hash-only Opaque RefreshSession을 발급한다. Firebase 인증 정보와 UID는 내부 JWT·Learning Core·로그로 전달하지 않는다.
- 구현 내용: owner가 없으면 Firebase 인증 성공만으로 User·PhoneIdentity·SocialIdentity·RefreshSession을 만들지 않는다. DIRECT_SIGNUP FirebaseEnrollmentAttempt만 생성 또는 재사용하고 `ENROLLMENT_REQUIRED` 상태, enrollmentId, 미충족 요구와 남은 시간을 반환한다.
- 구현 내용: 동일 project·UID·DIRECT_SIGNUP binding의 유효 PENDING attempt는 반복 exchange에서 같은 ID와 원래 만료 시각을 유지한다. 만료 attempt는 application CAS로 EXPIRED 전환한 뒤 새 attempt를 만들며 TTL은 cleanup에만 사용한다. 동시 insert는 partial unique winner를 재조회하는 기존 최대 4회 retry 기반을 재사용한다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다. Stage 5A Jira도 아직 생성하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태를 변경하지 않았다.
- 승인 여부: 사용자는 Stage 5A 동작을 더 자세히 설명해 달라고 요청했다. 구현, Jira 생성·수정, Git commit·push와 Firebase 활성화는 승인 범위에 포함되지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 계약·현재 코드 분석이므로 Gradle 테스트를 실행하지 않았다. 근거로 병합된 verifier·FirebaseIdentity·FirebaseEnrollmentAttempt·startOrReuse 구현과 전체 계획 문서를 읽기 전용 확인했다.
- 유지한 계약: canonical UUID userId/JWT `sub`, RS256·JWKS·Opaque RefreshSession, Firebase broker와 Identity account ownership을 유지한다. 클라이언트 userId를 받지 않고 Firebase credential 원문·Claim·UID·개인정보를 저장하거나 로그에 남기지 않으며 실제 외부 Provider를 호출하지 않았다.
- 결정사항: Stage 5A의 correctness 종료점은 기존 owner 로그인 또는 enrollment 발급까지다. User 생성, phone claim, signup finalize Transaction, attempt consume, outbox와 legacy password route cutover는 Stage 5B 이후로 분리한다.
- 위험 요소: 기존 SocialIdentity owner와 FirebaseIdentity가 어긋난 경우의 broker rebind 정책, AUTHENTICATED/ENROLLMENT_REQUIRED DTO와 missing requirement enum, disabled route의 404/503 정책, `FIREBASE_IDENTITY_CONFLICT`·`SOCIAL_IDENTITY_CONFLICT` 오류는 아직 구현 계약으로 고정되지 않았다. 이를 정하지 않고 구현하면 자동 계정 연결이나 클라이언트 분기 불일치가 생길 수 있다.
- 다음 작업: 사용자가 원하면 위 미결정 사항을 선택한 Stage 5A Jira payload를 먼저 제시하고 승인 후 생성한다. 권장은 명시적 result type, disabled 시 안정적 503, 기존 owner 충돌 시 자동 merge 없는 fail-closed 정책이다.

## 2026-08-14 — Stage 5A Firebase login exchange·enrollment 구현

<!-- codex-turn:019ffe63-259c-7ce1-87ce-716002523c2b -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`063fdc7`, GitHub PR #21 병합 기준, Codex commit·push 미수행)
- 작업 목표: `POST /api/v1/auth/firebase/exchange`에서 기존 FirebaseIdentity owner의 Identity Token 발급과 미등록 UID의 비생성·멱등 enrollment 반환을 구현하고 Firebase 기본 비활성 계약을 유지한다.
- 변경 파일: `src/main/java/web/tosunsaeng/identity/domain/auth/federation/{api,application,dto}/**`, `federation/infrastructure/firebase/FirebaseAuthenticationConfiguration.java`, `auth/common/exception/AuthErrorStatus.java`, `global/config/SecurityConfig.java`, `global/observability/RequestLoggingFilter.java`, 대응하는 `src/test/java/**`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 공개 Firebase exchange controller와 `FirebaseExchangeUseCase`를 추가했다. enabled service는 Firebase ID Token을 기존 verifier의 `LOGIN_EXCHANGE` 목적으로 검증하고 `(firebaseProjectId, firebaseUid)` owner가 있으면 canonical User가 ACTIVE MEMBER인지 확인한 뒤 기존 `AccessTokenIssuer`와 `RefreshSessionIssuer`로 자체 Access/Refresh를 발급한다. Refresh Token 원문은 응답에만 전달되고 기존 issuer 계약대로 DB에는 hash만 저장된다.
- 구현 내용: FirebaseIdentity owner가 없으면 User·PhoneIdentity·SocialIdentity·RefreshSession을 생성하지 않고 DIRECT_SIGNUP `FirebaseEnrollmentAttempt`만 생성 또는 재사용한다. password 연결 상태에서 email 미검증이면 `EMAIL_VERIFICATION`, verified phone이 없으면 `PHONE_VERIFICATION`, 모든 신규 enrollment에는 `PROFILE`·`CONSENTS`를 반환하며 기존 attempt의 원래 만료 시각 기준 남은 milliseconds를 제공한다.
- 구현 내용: 응답을 `AUTHENTICATED`와 `ENROLLMENT_REQUIRED` sealed union으로 고정하고 Access·Refresh TTL을 milliseconds로 반환한다. 기존 SocialIdentity owner 불일치는 자동 rebind·merge 없이 `SOCIAL_IDENTITY_CONFLICT`, canonical User가 사라진 FirebaseIdentity mapping은 `FIREBASE_IDENTITY_CONFLICT` 409로 fail-closed 처리한다. SUSPENDED·WITHDRAWN MEMBER와 ACTIVE GUEST에는 Token을 발급하지 않는다.
- 구현 내용: Firebase가 기본 비활성일 때 controller route는 유지하되 disabled use case가 의존성을 조회하지 않고 안정적인 `503 FIREBASE_UNAVAILABLE`을 반환한다. Security에 공개 POST route를 추가하고 request logging의 고정 route 목록을 갱신했다. Firebase credential은 OpenAPI `writeOnly`·required이며 request와 token response `toString()`에서 redaction된다. 200 공통 envelope result의 두 schema `oneOf`와 401·403·409·429·503 오류를 문서화했다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다. Stage 5A에 연결된 신규 Jira 이슈 키는 없다.
- 추가한 댓글의 목적: 연결된 Stage 5A Jira가 없어 댓글 초안을 등록하거나 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자가 Stage 5A 구현을 명시적으로 요청했다. Jira 변경, Firebase/provider 활성화, signup finalize, legacy credential cutover와 Git commit·push는 승인 범위에 포함되지 않았다.
- 실행한 테스트와 결과: Firebase exchange application·controller·configuration과 Security/OpenAPI 타깃 테스트가 성공했다. 최종 `./gradlew clean test`는 66개 suite·399개 테스트가 failure 0, error 0, skipped 0으로 성공했고 `git diff --check`도 통과했다. 실제 Firebase·Atlas나 외부 OAuth Provider는 호출하지 않았다.
- 유지한 계약: canonical UUID userId를 JWT `sub`로 사용하고 RS256·`kid`·issuer·`tosunsaeng-learning-core` audience·JWKS와 Opaque RefreshSession 계약을 유지했다. 클라이언트 userId를 받지 않고 Firebase credential·UID·project·Claim·provider subject, Secret·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 로그나 기록에 포함하지 않았다. 시험·채점·Entitlement 코드는 추가하지 않았다.
- 결정사항: Stage 5A는 기존 owner 로그인 또는 enrollment 발급까지만 책임진다. SocialIdentity owner가 있더라도 이 단계에서는 broker rebind나 merge를 하지 않는다. 기능 disabled는 route 부재가 아니라 안정적 503으로 표현하며 actual provider flag와 전체 Firebase 기능은 기본 비활성으로 유지한다.
- 위험 요소: 실제 Firebase/mobile/SMS production gate와 endpoint rate limit·quota/circuit·alert, 운영 Mongo index는 staging에서 아직 검증하지 않았다. 엄격한 SocialIdentity conflict는 기존 provider mapping과 FirebaseIdentity migration이 불완전한 계정을 자동 복구하지 않는다. signup finalize·attempt consume·phone claim·outbox가 아직 없으므로 `ENROLLMENT_REQUIRED` 이후 신규 MEMBER 가입은 완료할 수 없다.
- 다음 작업: Stage 5B를 별도 범위로 설계해 fresh same-UID phone proof와 필수 동의를 검증하고 User·FirebaseIdentity·PhoneIdentity·SocialIdentity·RefreshSession·PhoneEligibilityBindingOutbox 생성 및 enrollment consume을 한 Mongo Transaction으로 묶는다. production provider 활성화와 legacy password route cutover는 계속 별도 gate로 둔다.

## 2026-08-14 — Stage 5A Jira 종료 대상 확인 및 Stage 5B 계획 구체화

<!-- codex-turn:019ffec2-6d1d-7f93-9fb8-73a2e17f8589 -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`063fdc7`, Codex commit·push 미수행)
- 작업 목표: Stage 5A 구현에 연결된 Jira 종료 대상을 확인하고, 다음 Stage 5B 신규 MEMBER signup finalize 구현 계획과 선행조건을 현재 계약·코드 기준으로 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드와 테스트는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Stage 5B 요청은 `enrollmentId`, fresh Firebase credential, nickname, 현재 개인정보 처리방침·이용약관 동의와 version만 받는다. client phone이나 userId는 신뢰하지 않고 Firebase verifier의 `DIRECT_ENROLLMENT` 결과와 attempt의 project·UID·binding을 대조한다.
- 구현 내용: 현재 `VerifiedFirebasePrincipal`은 phone verified boolean만 제공하므로 Firebase Admin UserRecord에서 검증한 optional E.164 phone을 application의 최소 principal로 안전하게 전달하는 adapter 확장이 선행되어야 한다. raw phone은 Transaction 동안 PhoneIdentity·별도 benefit fingerprint 후보 파생에만 사용하고 저장·응답·로그·문자열 표현에는 포함하지 않는다.
- 구현 내용: 최종 가입은 User·FirebaseIdentity·PhoneIdentity와 retained aliases·SocialIdentity·PhoneEligibilityBindingOutbox·prepared RefreshSession 저장 및 DIRECT_SIGNUP attempt consume CAS를 하나의 named Mongo Transaction으로 묶는다. Access Token은 Transaction 성공 뒤 canonical UUID userId를 `sub`로 발급하며 Firebase UID를 Claim에 넣지 않는다.
- 구현 내용: PhoneEligibilityBindingOutbox는 Identity가 제품별 TrialClaim 의미를 소유하지 않도록 opaque consumer scope·별도 benefit key/domain·멱등 event 계약으로 먼저 확정해야 한다. consumer schema·key ownership·retention이 준비되지 않으면 signup finalize와 Firebase feature는 계속 비활성으로 둔다.
- 수행한 Jira 작업: Atlassian 공식 MCP로 Stage 5A·Firebase 관련 이슈를 검색하고 TMI-93, TMI-92, TMI-91, TMI-90의 요약·범위·상태·Resolution을 읽기 전용 확인했다. Stage 5A 전용 Jira는 없고 TMI-93은 이미 완료이며, TMI-92와 TMI-90은 별도 범위로 `해야 할 일` 상태다. 두 open 이슈의 사용 가능한 완료 transition ID `41`도 읽기 전용 확인했다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다. Stage 5A 전용 이슈가 없어 종료 댓글 초안을 연결할 대상도 없다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다. 종료 대상이 없는 상태에서 TMI-90 또는 TMI-92를 임의 선택하지 않았다.
- 승인 여부: 사용자는 Jira 종료를 요청했지만 Stage 5A 전용 이슈 키가 없고 다른 open 이슈는 범위가 달라 정확한 대상 선택이 필요하다. 조회는 허용 범위에서 수행했고 Jira mutation은 대상과 payload를 먼저 제시한 뒤 별도 확인을 받기 위해 보류했다.
- 실행한 테스트와 결과: 코드 변경이 없는 Jira 조회·계획·문서 갱신 작업이므로 Gradle 테스트를 재실행하지 않았다. 직전 Stage 5A 최종 `./gradlew clean test`는 66개 suite·399개 테스트가 failure 0, error 0, skipped 0으로 성공했다. 종료 전 `git diff --check`와 WORKLOG marker 단일 존재를 정적으로 확인한다.
- 유지한 계약: canonical UUID userId/JWT `sub`, RS256·`kid`·issuer·`tosunsaeng-learning-core` audience·JWKS와 Opaque RefreshSession을 유지한다. Firebase credential·UID·phone·provider subject와 Secret·Password·실제 Key·전체 MongoDB URI를 Jira나 기록에 포함하지 않았고 시험·채점·Entitlement 코드를 Identity에 추가하지 않는다.
- 결정사항: Jira 종료는 범위가 일치하는 정확한 이슈에만 적용한다. Stage 5B는 verified phone 전달 경계와 generic PhoneEligibilityBindingOutbox 서버 간 계약을 먼저 확정한 뒤 finalize Transaction을 구현하며 production provider 활성화와 legacy password cutover는 계속 분리한다.
- 위험 요소: Stage 5A 변경은 아직 로컬 미커밋 상태여서 PR 병합 근거가 없고 전용 Jira도 없다. 현재 principal만으로는 raw phone 비저장 PhoneIdentity를 만들 수 없으며, outbox consumer 계약 없이 가입을 열면 benefit proof를 나중에 복구하려고 전화 재인증이 필요해질 수 있다. duplicate finalize 정책과 multi-document rollback 경쟁도 구현 전에 고정해야 한다.
- 다음 작업: 사용자가 `TMI-92` 또는 `TMI-90` 중 종료 대상을 정확히 승인하면 해당 이슈에만 transition ID `41`을 적용하고 상태·Resolution을 재조회한다. Stage 5B 작업은 별도 Jira payload로 선행 계약·Transaction·rollback 완료 조건을 제시한 뒤 생성·구현한다.

## 2026-08-14 — Stage 5B Firebase signup finalize Jira 생성안 준비

<!-- codex-turn:019ffec7-f7dd-7b21-9d10-45df8227b6d2 -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`063fdc7`, Codex commit·push 미수행)
- 작업 목표: 다음 구현인 Stage 5B Firebase 신규 MEMBER signup finalize Transaction을 추적할 Jira의 정확한 생성 payload를 준비하고 사용자 승인 전 중복·생성 필드를 확인한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드와 테스트는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Jira 제목을 `[Identity] Stage 5B Firebase 신규 MEMBER signup finalize Transaction`, 유형을 TMI `작업`, 우선순위를 `High`로 준비했다. 기본 상태 `해야 할 일`, 담당자·라벨·상위 항목·스프린트·기한 없음으로 제안한다.
- 구현 내용: 범위는 `POST /api/v1/auth/firebase/signup`, fresh `DIRECT_ENROLLMENT` proof와 DIRECT_SIGNUP attempt 대조, Firebase Admin verified E.164 phone의 redacted principal 전달, nickname·필수 동의 검증, User·FirebaseIdentity·PhoneIdentity/aliases·SocialIdentity·generic PhoneEligibilityBindingOutbox·RefreshSession 저장과 attempt consume의 단일 Mongo Transaction이다.
- 구현 내용: duplicate finalize는 새 Refresh Token 반복 발급 없이 고정 conflict 후 exchange 재진입으로 처리하고, Firebase/Social/phone unique 충돌과 outbox·Session·consume 실패가 모두 고아 User 없이 rollback되도록 완료 조건을 구성했다. Firebase/provider 활성화, legacy password route cutover, Guest 승격·merge, Entitlement consumer·TrialClaim·시험 코드는 제외한다.
- 수행한 Jira 작업: Atlassian 공식 MCP 검색으로 Stage 5B signup finalize·PhoneEligibilityBindingOutbox 관련 중복 이슈가 없음을 확인했다. TMI 프로젝트의 이슈 유형 4개, `작업` 유형 ID `10003`, 생성 필드 20개와 `High` 우선순위 ID `2` 지원을 읽기 전용 재확인했다. Jira 생성·수정·댓글·상태 전환은 수행하지 않았다.
- 추가한 댓글의 목적: 신규 Jira가 아직 생성되지 않아 댓글을 추가하지 않았다.
- 변경한 상태: 생성된 Stage 5B Jira가 없어 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자가 Jira 생성을 요청했지만 저장소 규칙상 exact payload를 먼저 제시하고 최종 승인을 받아야 한다. 이번 turn에서는 생성 전 payload를 준비하고 승인을 기다린다.
- 실행한 테스트와 결과: 코드 변경이 없는 Jira 생성안·문서 기록 작업이므로 Gradle 테스트를 재실행하지 않았다. 직전 Stage 5A `./gradlew clean test`는 66개 suite·399개 테스트가 모두 성공했다. 종료 전 `git diff --check`와 WORKLOG marker 단일 존재를 정적으로 확인한다.
- 유지한 계약: canonical UUID userId/JWT `sub`, RS256·`kid`·issuer·`tosunsaeng-learning-core` audience·JWKS, Opaque RefreshSession과 Firebase 기본 비활성을 유지한다. client userId·phone을 신뢰하지 않고 Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 Jira payload나 기록에 포함하지 않았다. Entitlement·시험·채점 코드를 Identity에 추가하지 않는다.
- 결정사항: Stage 5B는 code-complete 상태에서도 Firebase/provider flag를 켜지 않는다. consumer-scoped outbox schema·key ownership·멱등 수신·보존 계약과 staging production gate가 준비되기 전 공개 신규 가입을 활성화하지 않는다.
- 위험 요소: 현재 verified principal에는 E.164 phone이 없어 adapter 최소 결과 확장이 필요하고, outer signup Transaction 안에서 PhoneIdentity 내부 retry가 rollback-only Transaction을 재사용하지 않도록 Transaction coordinator가 전체 재시도를 소유해야 한다. 실제 Mongo multi-document conflict와 partial unique index는 staging에서 추가 검증이 필요하다.
- 다음 작업: 사용자가 제시된 Stage 5B Jira 제목·유형·우선순위·본문을 승인하면 Atlassian 공식 MCP로 이슈 한 건을 생성하고 발급된 키와 저장된 상태를 재조회해 CURRENT_STATE와 새 WORKLOG 항목에 기록한다.

## 2026-08-14 — TMI-90·TMI-92 완료 전환 및 Stage 5B Jira TMI-94 생성

- 날짜: 2026-08-14
- 브랜치: `develop` (`063fdc7`, Codex commit·push 미수행)
- Jira: TMI-90, TMI-92, TMI-94
- 작업 목표: Firebase ADR·PoC와 PhoneIdentity 작업의 소유·병합 근거를 확인해 완료 처리하고, 승인된 Stage 5B Firebase 신규 MEMBER signup finalize Transaction Jira를 생성한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드와 테스트는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 로컬 Git에서 TMI-90 feature commit `af50153`과 PR #18 merge commit `77804e6`, TMI-92 feature commit `37b6733`과 PR #20 merge commit `4e8f46d`가 `develop` 이력에 포함된 것을 확인했다. Atlassian의 현재 계정과 두 이슈 reporter가 일치하고 두 이슈가 각각 해당 ADR·PoC와 PhoneIdentity 범위를 설명함을 확인했다.
- 구현 내용: 승인된 제목 `[Identity] Stage 5B Firebase 신규 MEMBER signup finalize Transaction`, TMI `작업`, 우선순위 `High`와 공개된 설명·완료 조건·제외 범위로 신규 Jira `TMI-94`를 생성했다. 후속 조회에서 기본 상태 `해야 할 일`, Resolution 없음, 담당자·라벨·컴포넌트 없음과 저장된 본문을 확인했다.
- 수행한 Jira 작업: TMI-90과 TMI-92에 각각 transition ID `41`만 적용해 `해야 할 일`에서 `완료`로 변경했다. 두 이슈의 후속 조회에서 상태와 Resolution이 모두 `완료`임을 확인했다. 이어서 TMI-94 한 건을 생성하고 저장 결과를 재조회했다.
- 추가한 댓글의 목적: Jira 댓글은 추가하지 않았다. 두 완료 이슈와 신규 이슈 어디에도 댓글을 등록하지 않았다.
- 변경한 상태: TMI-90과 TMI-92는 `완료`로 전환됐고 Resolution도 `완료`가 됐다. TMI-94는 생성 기본 상태 `해야 할 일`과 Resolution 없음 상태를 유지한다.
- 승인 여부: 사용자가 TMI-90과 TMI-92를 정확히 지정해 종료를 승인했고, 앞서 공개한 Stage 5B Jira payload에 `좋아`라고 승인한 뒤 두 기존 이슈를 먼저 닫아 달라고 요청했다. 승인 범위 밖의 이슈·필드·댓글은 변경하지 않았다.
- 실행한 테스트와 결과: 이번 turn은 Git/Jira 읽기 확인, 승인된 Jira 전환·생성과 문서 기록 작업이므로 Gradle 테스트를 재실행하지 않았다. 직전 Stage 5A `./gradlew clean test`는 66개 suite·399개 테스트가 모두 성공했다. 종료 전 `git diff --check`와 WORKLOG EOF append를 정적으로 확인한다.
- 유지한 계약: canonical UUID userId/JWT `sub`, RS256·`kid`·issuer·`tosunsaeng-learning-core` audience·JWKS, Opaque RefreshSession과 Firebase 기본 비활성을 변경하지 않았다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 Jira나 기록에 포함하지 않았고 Identity 외 도메인 코드를 추가하지 않았다.
- 결정사항: TMI-94를 다음 Stage 5B 구현의 단일 기준으로 사용한다. code-complete 뒤에도 consumer-scoped outbox 계약과 staging production gate 전에는 Firebase/provider 기능과 legacy route cutover를 활성화하지 않는다.
- 위험 요소: TMI-94는 verified E.164 phone principal 경계, outer Transaction 전체 retry, generic outbox 계약과 여러 unique conflict rollback을 함께 다루는 보안 핵심 작업이다. 실제 Mongo multi-document conflict·index와 외부 consumer는 staging·별도 서비스에서 추가 검증해야 한다.
- 다음 작업: TMI-94 구현 요청을 받으면 먼저 이슈 설명·완료 조건을 재조회해 AGENTS.md와 계약 문서에 대조하고, verified phone adapter 경계부터 Transaction·rollback 테스트 순서로 진행한다. Jira 댓글·상태 전환과 Git commit·push는 별도 승인 전 수행하지 않는다.

## 2026-08-14 — TMI-90·TMI-92 완료 및 TMI-94 생성 Hook 기록 동기화

<!-- codex-turn:019ffeca-aa8a-7a90-b2d7-645c5f5dabe0 -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`063fdc7`, Codex commit·push 미수행)
- Jira: TMI-90, TMI-92, TMI-94
- 작업 목표: 직전 Jira 완료·생성 turn을 Hook이 지정한 식별자로 WORKLOG EOF에 기록하고 CURRENT_STATE를 최신 상태로 동기화한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드와 테스트는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Hook 지정 marker를 새 EOF 항목에 추가했다. 직전 작업에서 TMI-90과 TMI-92의 reporter와 병합 근거를 확인해 두 이슈를 완료 처리하고, 승인된 Stage 5B payload로 TMI-94를 생성했다.
- 수행한 Jira 작업: 이번 Hook 기록 동기화에서는 Jira 조회·생성·수정·댓글·상태 전환을 추가로 수행하지 않았다. 직전 turn 결과는 TMI-90·TMI-92 상태와 Resolution `완료`, TMI-94 상태 `해야 할 일`·우선순위 `High`·Resolution 없음이다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: 이번 기록 동기화에서는 Jira 상태를 변경하지 않았다.
- 승인 여부: Hook이 현재 turn 식별자를 포함한 작업 기록 보정을 요구했다. 애플리케이션 변경, 추가 Jira mutation과 Git commit·push는 수행하지 않았다.
- 실행한 테스트와 결과: 문서 기록만 변경했으므로 Gradle 테스트를 재실행하지 않았다. 직전 Stage 5A `./gradlew clean test`는 66개 suite·399개 테스트가 모두 성공했다. 종료 전 `git diff --check`와 marker 단일 존재를 확인한다.
- 유지한 계약: canonical UUID userId/JWT `sub`, RS256·JWKS·Opaque RefreshSession과 Firebase 기본 비활성 계약을 변경하지 않았다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: Hook 지정 marker는 WORKLOG 과거 항목을 수정하지 않고 별도 EOF 항목으로 보존한다. 다음 구현 기준 Jira는 TMI-94다.
- 위험 요소: 문서 기록만 변경했으므로 새로운 애플리케이션 위험은 없다. Stage 5A 코드는 계속 로컬 미커밋 상태다.
- 다음 작업: TMI-94 구현 요청 시 Jira를 먼저 재조회하고 verified phone adapter 경계, signup Transaction과 rollback 테스트 순으로 진행한다.

## 2026-08-14 — TMI-94 Stage 5B Firebase 신규 MEMBER signup finalize 구현

<!-- codex-turn:019ffed2-7104-7f92-8e53-d5b4f7dcc3e1 -->

- 날짜: 2026-08-14
- 브랜치: `feat/TMI-94-firebase-member-signup-finalize` (`063fdc7`에서 시작, Stage 5A·5B 로컬 미커밋, Codex commit·push 미수행)
- Jira: TMI-94
- 작업 목표: fresh same-UID Firebase proof, verified phone, profile과 필수 동의를 검증하고 신규 canonical MEMBER aggregate와 최초 RefreshSession 및 enrollment consume을 하나의 Mongo Transaction으로 확정한다.
- 변경 파일: `.env.example`, `README.md`, `application.yml`, `application-test.yml`, `domain/auth/common/exception/AuthErrorStatus`, Firebase federation의 api·application·dto·firebase infrastructure·repository wiring, Auth persistent entity·enum, phoneidentity domain·infrastructure·repository, User entity·factory·provider, Security·RequestLoggingFilter, 대응 controller·application·Transaction·adapter·configuration·repository·User·Security/OpenAPI 테스트, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 기존 Stage 5A 미커밋 변경을 보존했고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 공개 `POST /api/v1/auth/firebase/signup`은 enrollmentId, fresh write-only Firebase credential, nickname, 개인정보 처리방침·이용약관 동의와 version만 받는다. 외부 userId·phone 필드는 없고 Firebase Admin UserRecord의 phone provider와 일치하는 verified phone만 최소 principal로 전달하며 credential·UID·phone·provider subject의 문자열 표현은 redaction한다.
- 구현 내용: `DIRECT_ENROLLMENT` 검증 결과에 primary credential과 same-UID verified phone이 있어야 한다. PENDING·미만료 DIRECT_SIGNUP attempt의 project·UID를 대조하고 nickname·서버 현재 동의를 검증한 뒤에만 `UserProvider.FEDERATED`, `accountType=MEMBER`, password/email/guest credential이 없는 canonical UUID User를 만든다.
- 구현 내용: signup 전용 `mongoTransactionManager` 경계에서 User, FirebaseIdentity, PhoneIdentity, retained PhoneFingerprintAlias, generic PhoneEligibilityBindingOutbox, SocialIdentity 0..N, prepared RefreshSession을 저장하고 attempt consume CAS를 마지막에 수행한다. phone owner 선검사와 모든 저장/CAS 실패는 예외를 전파해 전체 rollback하며 기존 PhoneIdentity 내부 retry Transaction을 중첩 호출하지 않는다.
- 구현 내용: Access Token은 Transaction 반환 뒤 canonical UUID userId로 발급한다. 순차 duplicate finalize와 Firebase mapping 경쟁은 새 Session을 저장하지 않고 `FIREBASE_ENROLLMENT_CONFLICT` 409 후 exchange 재진입으로 고정했으며 Social/phone owner 충돌은 자동 rebind·merge 없이 각각 기존 conflict로 거절한다.
- 구현 내용: eligibility candidate는 PhoneIdentity와 별도 key ring, `tosunsaeng:identity:phone-eligibility-binding:v1` domain, opaque consumer scope를 사용한다. raw phone과 fingerprint/key material은 저장·응답·로그에 남기지 않고 outbox에는 retained candidate와 PENDING 상태만 저장한다. 반복 phone 변경 이벤트를 허용하며 publisher 조회용 status+createdAt 및 user+scope+createdAt non-unique index를 추가했다.
- 구현 내용: Firebase signup route를 Security 공개 POST와 RequestLoggingFilter 고정 route에 추가하고 OpenAPI에 credential writeOnly, 외부 userId/phone 부재, 400·401·403·409·429·503을 문서화했다. Firebase·provider·PhoneIdentity fingerprint·eligibility binding 설정은 모두 기본 비활성이며 필요한 signup dependency가 없는 활성화는 fail-closed한다. legacy password route, Guest 승격·merge, publisher/consumer, TrialClaim·Entitlement·시험 코드는 변경하지 않았다.
- 수행한 Jira 작업: 구현 전에 Atlassian 공식 MCP로 TMI-94의 제목·설명·완료 조건·제외 범위·현재 상태를 읽기 전용 확인하고 AGENTS.md 및 계약과 충돌이 없음을 확인했다. Jira 이슈·설명·댓글·상태·Resolution은 변경하지 않았다.
- 추가한 댓글의 목적: verified phone 경계, 단일 Transaction aggregate, 별도 eligibility fingerprint/outbox, Security/OpenAPI와 전체 테스트 결과 및 staging/consumer 잔여 위험을 전달하는 종료 댓글 초안을 준비하되 자동 등록하지 않는다.
- 변경한 상태: Jira TMI-94는 기존 `해야 할 일`, 우선순위 `High`, Resolution 없음 상태를 유지한다. PR 병합을 확인하지 않았으므로 Done 전환하지 않았다.
- 승인 여부: 사용자가 `이제 구현해줘`로 TMI-94 구현을 명시적으로 요청했다. Jira 댓글·상태 전환과 Git commit·push, production Firebase/provider 활성화는 승인 범위에 포함되지 않았다.
- 실행한 테스트와 결과: 구현 중 Firebase verifier·signup service·signup Transaction 타깃 테스트와 `./gradlew test`를 반복 실행했다. 최종 `./gradlew clean test`는 70개 suite·421개 테스트가 failure 0, error 0, skipped 0으로 성공했고 `git diff --check`도 통과했다. 실제 Firebase·Atlas·외부 OAuth Provider·outbox consumer는 호출하지 않았다.
- 유지한 계약: JWT `sub`는 canonical UUID userId이며 RS256·`kid`·issuer·`tosunsaeng-learning-core` audience·JWKS와 hash-only Opaque RefreshSession을 유지했다. client userId·phone을 신뢰하지 않고 Firebase credential·UID·phone·provider subject·fingerprint·Secret·Password·실제 Key·전체 MongoDB URI를 로그·응답·기록에 포함하지 않았다. Identity가 소유하지 않는 시험·채점·Entitlement 코드를 추가하지 않았다.
- 결정사항: duplicate finalize는 토큰 재발급형 멱등 성공이 아니라 고정 enrollment conflict와 exchange 재진입으로 처리한다. PhoneEligibilityBindingOutbox는 제품 enum을 모르는 generic event이며 phone 변경을 위해 user+scope unique를 두지 않는다. Access Token 발급은 commit 이후에만 수행하고 Firebase와 Mongo가 분산 Transaction이 아니라는 경계는 유지한다.
- 위험 요소: 실제 Firebase/mobile/SMS, transaction 지원 MongoDB의 multi-document write conflict와 운영 index 생성은 staging에서 검증하지 않았다. outbox publisher와 외부 consumer의 eventId 멱등 수신·retry/dead-letter·보존/삭제·key ownership 계약은 별도 ADR/서비스 구현이 필요하다. commit 뒤 Access Token 응답 생성이 실패하면 aggregate와 RefreshSession은 성공한 상태이므로 클라이언트는 고정 finalize conflict를 받은 뒤 exchange로 복구해야 한다.
- 다음 작업: 사용자가 변경을 검토해 직접 commit·push하고 PR을 생성한다. PR 병합 확인 전 TMI-94를 Done으로 바꾸지 않으며 Jira 댓글은 초안을 먼저 승인받은 뒤에만 등록한다. production 활성화 전 격리 Firebase/mobile 및 transaction 지원 staging MongoDB와 outbox consumer 계약을 검증한다.

## 2026-08-14 — TMI-94 PR 병합 확인·Jira 완료 및 다음 Stage 5C 정리

<!-- codex-turn:019ffee3-7fe8-7822-b9ee-29ab8cc80671 -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`455db00`, GitHub PR #22 merge commit, Codex commit·push 미수행)
- Jira: TMI-94
- 작업 목표: TMI-94 구현 PR 병합을 확인한 뒤 승인된 종료 댓글과 완료 전환을 적용하고, production Firebase signup 활성화 전에 필요한 다음 작업을 정리한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드와 테스트는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 로컬 `develop`과 `origin/develop`이 PR #22 merge commit `455db00`을 가리키며 TMI-94 feature commit `f68d7e8`이 이력에 포함된 것을 확인했다. Jira 변경 전 TMI-94가 `해야 할 일`, Resolution 없음, 빈 댓글 상태이고 완료 transition ID `41`을 사용할 수 있음을 재조회했다.
- 수행한 Jira 작업: 사용자가 앞서 제시한 종료 댓글과 Jira 종료를 승인한 뒤 TMI-94에 구현 범위, `./gradlew clean test` 70개 suite·421개 성공 결과와 staging/consumer 잔여 위험을 담은 댓글 ID `10005`를 등록했다. 이어서 transition ID `41`만 적용했고 후속 조회에서 상태 ID `10003` `완료`와 Resolution `완료`를 확인했다. 다른 이슈·필드는 변경하지 않았다.
- 추가한 댓글의 목적: verified phone 경계, 신규 MEMBER aggregate 단일 Mongo Transaction, 별도 eligibility outbox, Security/OpenAPI와 전체 테스트 결과 및 실제 Firebase·staging Mongo·consumer 계약의 남은 위험을 인수인계하기 위해 등록했다.
- 변경한 상태: Jira TMI-94를 `해야 할 일`에서 `완료`로 전환했고 Resolution도 `완료`가 됐다.
- 승인 여부: 직전 응답에서 등록할 Jira 종료 댓글 초안을 공개했고 사용자가 `좋아 지라 닫아주고`라고 명시적으로 승인했다. Git commit·push, 신규 Jira 생성과 production feature 활성화는 승인 범위에 포함되지 않았다.
- 실행한 테스트와 결과: 이번 turn은 병합·Jira 읽기 확인, 승인된 Jira 댓글·상태 전환과 문서 기록만 수행했으므로 Gradle 테스트를 재실행하지 않았다. 병합된 PR #22의 최종 검증은 `./gradlew clean test` 70개 suite·421개 테스트 failure 0, error 0, skipped 0이며 이번 문서 변경 뒤 `git diff --check`를 실행한다.
- 유지한 계약: canonical UUID userId/JWT `sub`, RS256·`kid`·issuer·`tosunsaeng-learning-core` audience·JWKS, hash-only Opaque RefreshSession과 Firebase/provider 기본 비활성을 유지했다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 Jira나 기록에 포함하지 않았고 시험·채점·Entitlement 코드를 추가하지 않았다.
- 결정사항: 다음 권장 작업은 즉시 provider flag를 켜거나 legacy password route를 제거하는 작업이 아니다. 먼저 generic PhoneEligibilityBindingOutbox의 버전된 schema, atomic lease publisher, at-least-once delivery, retry/backoff·dead-letter·보존/삭제와 외부 consumer의 eventId 멱등 처리·key ownership·미도착 fail-closed 계약을 Stage 5C로 분리한다.
- 위험 요소: 실제 Firebase/mobile/SMS와 transaction 지원 staging MongoDB 검증은 아직 남아 있다. outbox consumer와 운영 전달 계약 없이 signup을 활성화하면 이미 가입한 번호의 eligibility proof를 raw phone 없이 복구하지 못하거나 outbox 지연을 무료체험 중복 방지 우회로 만들 수 있다.
- 다음 작업: Stage 5C Jira를 만들기 전에 Identity publisher 범위와 외부 Entitlement/Billing consumer 범위를 나누고 event schema·멱등성·보존·key ownership·운영 실패 정책을 완료 조건으로 제시한다. 이 gate와 staging 검증이 끝난 뒤에만 provider별 활성화와 legacy password signup/login/check-email cutover를 별도 작업으로 진행한다.

## 2026-08-14 — Stage 5C 서버 간 eligibility binding 계약 ADR 상세 설명

<!-- codex-turn:019ffee8-2bb9-7e43-ab01-d5cd6ee540bd -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`455db00`, Codex commit·push 미수행)
- 작업 목표: PhoneEligibilityBindingOutbox를 외부 consumer에 전달하기 전에 필요한 versioned event schema, eventId 멱등성, opaque consumer scope, candidate key version, 보존·삭제·rotation과 민감정보 금지 계약을 현재 구현·전체 계획 기준으로 상세 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. ADR·애플리케이션 코드·테스트·설정은 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 권장 이벤트를 `PhoneEligibilityBindingVerified`로 정의하고 eventId, eventType, schemaVersion, producer, occurredAt, consumerScopeId, canonical userId, verifiedAt, user+scope 단위 bindingRevision과 retained fingerprintCandidates(keyVersion, value)의 최소 envelope를 제안했다. candidate 배열 순서는 의미가 없고 consumer는 하나라도 일치하면 같은 verified phone proof로 판단한다.
- 구현 내용: consumer scope는 client 요청이나 제품 enum에서 받지 않고 Identity의 allowlist된 서버 설정으로만 선택하며 HMAC input domain에 포함한다. Identity는 scope의 상품 의미, TrialClaim·Entitlement 지급을 해석하지 않고 같은 phone도 scope가 다르면 상호 비교할 수 없는 candidate가 나오도록 분리한다.
- 구현 내용: at-least-once delivery에서 consumer는 eventId unique inbox와 binding update를 같은 로컬 Transaction으로 처리한다. 같은 eventId·같은 payload 재전달은 성공 no-op으로 응답하고, 같은 eventId의 다른 canonical payload는 덮어쓰지 않고 poison event로 격리·경보한다. 여러 정상 event의 역순 도착은 단조 증가 bindingRevision으로 이전 revision이 최신 binding을 되돌리지 못하게 한다.
- 구현 내용: eligibility HMAC key material은 Identity writer의 Secret 경계에만 두고 consumer에는 keyVersion과 candidate만 전달한다. 새 version은 retained lookup candidate로 선배포하고 writer 동기화 뒤 ACTIVE_WRITE로 전환하며 이전 version은 LOOKUP_ONLY로 유지한다. raw phone이 없으므로 old-only 기존 binding을 새 key로 임의 backfill하지 않고 reference 만료 또는 phone 재인증 전에는 legacy version을 제거하지 않는다.
- 구현 내용: producer PENDING/lease/dead-letter outbox, 짧은 published replay·감사 보존, consumer current binding과 장기 abuse ledger는 목적이 다르므로 동일 TTL로 묶지 않는다. withdrawal·phone 교체 때 current binding을 제거·교체할지, 중복 혜택 방지 ledger를 어떤 법적 근거와 기간으로 보존할지는 제품·개인정보 정책과 함께 ADR에 명시하며 무기한 보존을 기본값으로 두지 않는다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다. TMI-94는 직전 turn에서 완료됐고 이번 요청은 다음 계약의 상세 설명 범위다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 서버 간 계약 ADR 항목을 더 자세히 설명해 달라고 요청했다. ADR 파일 생성, 신규 Jira, publisher·consumer 구현, production flag 활성화와 Git commit·push는 승인 범위에 포함되지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 계약 설명과 문서 기록 작업이므로 Gradle 테스트를 실행하지 않았다. 병합된 기준선의 최종 결과는 `./gradlew clean test` 70개 suite·421개 테스트 failure 0, error 0, skipped 0이며 종료 전 `git diff --check`를 수행한다.
- 유지한 계약: canonical UUID userId/JWT `sub`, Firebase broker와 Identity account owner, PhoneIdentity·benefit fingerprint의 key/domain 분리, Firebase/provider 기본 비활성 및 시험·Entitlement 도메인 경계를 유지했다. raw phone·last4·Firebase UID·provider subject·PhoneIdentity fingerprint·HMAC key와 Secret·Token·Password·실제 Key·전체 MongoDB URI를 payload·로그·기록에 포함하지 않는다.
- 결정사항: event 수신은 혜택 지급이 아니라 verified phone binding 준비일 뿐이다. consumer는 eventId 멱등성과 bindingRevision 순서 보호를 가져야 하고, publisher 2xx는 consumer의 로컬 Transaction commit 이후에만 성공으로 해석한다. schema breaking change는 새 schemaVersion으로 병행하며 producer는 consumer 지원 확인 전 구 version을 중단하지 않는다.
- 위험 요소: 현재 TMI-94 outbox entity에는 schemaVersion·eventType·bindingRevision과 publisher lease/retry/dead-letter 필드가 아직 없다. bindingRevision을 동시 phone 변경에서도 단조 증가시키는 저장 경계, 실제 transport 인증, consumer 소유 서비스, 법적 보존 기간과 key reference count는 Stage 5C ADR·구현에서 확정해야 한다.
- 다음 작업: 사용자가 원하면 위 결정을 Identity producer ADR과 외부 consumer 계약으로 분리한 Stage 5C Jira 생성안을 먼저 제시한다. 승인 뒤에만 ADR 파일·outbox schema 확장·publisher port와 lease/retry 테스트를 구현하며 외부 Entitlement/Billing consumer는 별도 저장소·Jira로 둔다.

## 2026-08-14 — event schemaVersion·keyVersion·bindingRevision 설명

<!-- codex-turn:019fff0f-8d09-7232-a7c7-325f71627f1b -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`455db00`, commit·push 미수행)
- 작업 목표: PhoneEligibilityBindingVerified 서버 간 이벤트 계약의 `schemaVersion`, eligibility candidate `keyVersion`, 사용자 binding `bindingRevision`이 각각 무엇을 버전 관리하는지 쉬운 비유와 변경 예시로 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션·계획·계약 코드는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: `eventType`은 송장 종류, `schemaVersion`은 송장 양식 판본이라는 비유로 설명했다. 소비자는 두 값을 함께 보고 JSON 필드·타입·의미를 아는지 판단하며 선택 필드 추가 같은 호환 변경은 기존 버전에서 허용할 수 있지만 필드 삭제·rename·타입·의미 변경은 새 schemaVersion으로 발행해야 한다.
- 구현 내용: producer와 consumer의 무중단 전환을 위해 새 schemaVersion을 구버전과 병행 지원하고 consumer 준비를 확인한 뒤 구버전을 중단하는 순서를 설명했다. consumer가 모르는 version을 추측해 처리하면 잘못된 사용자 binding을 저장할 수 있으므로 retry로 해결되지 않는 unsupported contract로 격리·경보해야 한다.
- 구현 내용: `keyVersion`은 event JSON 모양과 무관하게 fingerprint candidate를 만든 HMAC key 판본이며 candidate value를 어떤 retained key namespace에서 비교할지 표시한다. 하나의 schema v1 이벤트 안에도 key rotation 동안 v1·v2 candidate가 함께 존재할 수 있음을 설명했다.
- 구현 내용: `bindingRevision`은 같은 canonical userId와 consumerScopeId의 verified phone binding 변경 순번이다. revision 2가 먼저 처리된 뒤 늦게 도착한 revision 1을 무시해 at-least-once·역순 전달이 최신 binding을 과거 상태로 되돌리지 못하게 한다. 이는 eventId 중복 제거와도 별개라고 구분했다.
- 구현 내용: Java 클래스명·package·Mongo document 구조를 이벤트 계약에 노출하면 내부 refactor나 저장 모델 변경이 외부 consumer의 breaking change가 되므로, 이벤트는 업무 의미의 안정적인 JSON 이름만 사용해야 한다고 설명했다.
- 실행한 테스트와 결과: 코드 변경이 없는 개념 설명과 기록 작업이므로 Gradle 테스트를 실행하지 않았다. 현재 병합 기준선은 70개 suite·421개 테스트 성공 상태이며, 이번 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 WORKLOG 끝 append를 정적으로 검증한다.
- 유지한 계약: 이벤트에는 canonical UUID userId와 consumer-scoped eligibility candidate만 전달하고 raw phone·last4·Firebase UID·provider subject·PhoneIdentity fingerprint와 HMAC key material을 포함하지 않는다. Identity/Entitlement 경계, Firebase provider 기본 비활성과 Secret·Token·Password·실제 Key·전체 MongoDB URI 비기록 원칙을 유지했다.
- 결정사항: 세 숫자를 하나의 공통 버전으로 합치지 않는다. `schemaVersion`은 wire contract, `keyVersion`은 HMAC candidate origin, `bindingRevision`은 user+scope 상태 순서로 각각 독립 관리한다. 기존 코드·Jira·Git 상태를 변경하지 않았다.
- 위험 요소: 현재 TMI-94 outbox에는 세 계약 중 eventType·schemaVersion·bindingRevision이 아직 구현되지 않았다. Stage 5C에서 revision 원자 증가, consumer 지원 version, unknown version dead-letter와 구·신 schema 병행 기간을 확정하지 않으면 유실·역행·오해석 위험이 있다.
- 다음 작업: Stage 5C ADR에서 schema v1의 정확한 JSON·호환 규칙, 지원 종료 절차, candidate key rotation과 binding revision CAS를 확정하고 producer·consumer contract test를 각각 추가한다.

## 2026-08-14 — Stage 5C eligibility binding 서버 간 계약 ADR Jira 생성 초안

<!-- codex-turn:019fff14-bba8-72a0-97f5-616d4f4a14a6 -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`455db00`, commit·push 미수행)
- 작업 목표: 앞서 설명한 versioned event schema, eventId 멱등성, opaque consumer scope, candidate key rotation, 보존·삭제 정책과 민감정보 전달 금지를 확정하는 Stage 5C ADR 작업을 Jira 이슈로 생성하기 위한 최종 Payload 초안을 준비한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드·ADR·테스트는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: TMI-94와 최근 TMI 이슈를 Atlassian 공식 MCP로 읽어 프로젝트 `TMI`, 기본 업무 유형 `작업`, 직전 Stage 5B의 완료 상태와 High 우선순위 사용을 확인했다. 이번 Jira는 Identity 저장소가 소유하는 서버 간 계약 ADR 확정으로 한정하고 publisher·consumer 구현, TrialClaim·Entitlement, production Firebase 활성화와 legacy route cutover를 제외하는 초안을 작성했다.
- 수행한 Jira 작업: Atlassian 공식 MCP로 TMI-94, 최근 TMI 이슈와 프로젝트 이슈 유형을 읽기 전용 조회했다. 신규 Jira 생성·수정·댓글·상태 전환은 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: 기존 Jira 상태와 Resolution을 변경하지 않았다. 신규 이슈도 아직 생성하지 않았다.
- 승인 여부: 사용자가 신규 Jira 생성을 요청했지만 저장소 규칙상 실제 생성 전에 제목·설명·완료 조건·우선순위와 적용하지 않을 필드를 먼저 공개하고 별도의 명시적 승인을 받아야 하므로 승인을 기다린다.
- 실행한 테스트와 결과: Jira 읽기·초안 작성과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았다. 병합 기준선은 `./gradlew clean test` 70개 suite·421개 테스트 성공 상태이며 종료 전 `git diff --check`와 turn marker 단일 존재를 확인한다.
- 유지한 계약: canonical UUID userId/JWT `sub`, Identity와 외부 consumer의 도메인 경계, Firebase/provider 기본 비활성을 유지한다. Jira 초안과 기록에 raw phone·last4·Firebase UID·provider subject·PhoneIdentity fingerprint·HMAC key material 및 Secret·Token·Password·실제 Key·전체 MongoDB URI·사용자 개인정보를 포함하지 않았다.
- 결정사항: 신규 Jira는 `[Identity] Stage 5C Phone eligibility binding 서버 간 계약 ADR` 제목의 `작업`, High, 기본 `해야 할 일`로 제안한다. 담당자·스프린트·에픽·라벨·상태 전환은 생성 시 적용하지 않으며 ADR 확정과 구현을 분리한다.
- 위험 요소: transport 인증 방식, 실제 consumer 소유 서비스, bindingRevision 원자 증가 저장 경계, 구 key 제거를 위한 reference 확인 방식과 법적 보존 기간은 ADR에서 아직 확정해야 한다. 승인 전에는 Jira 키가 존재하지 않는다.
- 다음 작업: 사용자가 공개된 Jira Payload를 승인하면 동일 내용으로 TMI 이슈를 생성하고 생성 결과를 재조회한 뒤 이 WORKLOG와 CURRENT_STATE에 발급된 Jira 키와 수행한 Jira 작업을 추가 기록한다.

## 2026-08-14 — TMI-95 Stage 5C eligibility binding 서버 간 계약 ADR Jira 생성

<!-- codex-turn:019fff19-4398-7273-8edc-c62299213db9 -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`455db00`, commit·push 미수행)
- Jira: TMI-95
- 작업 목표: 사용자에게 공개한 Stage 5C Phone eligibility binding 서버 간 계약 ADR Jira Payload를 승인 내용 그대로 생성하고 저장 결과를 검증한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드·ADR·테스트는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Jira 설명에 versioned schema v1, eventId inbox 멱등성, bindingRevision 역순 보호, allowlist 기반 opaque consumer scope, candidate key lifecycle, 데이터 목적별 보존·삭제, revoke/tombstone, transport 책임과 민감정보 전달 금지를 기록했다. publisher·consumer 실제 구현과 TrialClaim·Entitlement, production 활성화, legacy API 종료는 제외 범위로 유지했다.
- 수행한 Jira 작업: Atlassian 공식 MCP로 TMI `작업` 이슈 `TMI-95`를 생성했다. 생성 직후 읽기 전용 재조회해 승인한 제목과 설명, 유형 `작업`, 우선순위 `High`, 상태 `해야 할 일`, Resolution 없음, 담당자 없음, 빈 라벨·컴포넌트를 확인했다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: 생성 시 기본 상태 `해야 할 일`을 유지했으며 별도 transition을 적용하지 않았다. 기존 Jira 이슈의 상태나 Resolution은 변경하지 않았다.
- 승인 여부: 직전 응답에서 생성할 프로젝트·유형·제목·우선순위·설명·완료 조건·제외 범위와 미설정 필드를 공개했고 사용자가 `생성해줘`라고 명시적으로 승인했다.
- 실행한 테스트와 결과: Jira 생성·검증과 기록 문서 변경만 수행해 Gradle 테스트를 실행하지 않았다. 병합 기준선은 `./gradlew clean test` 70개 suite·421개 테스트 성공 상태이며 이번 문서 변경에 대해 `git diff --check`와 turn marker 단일 존재를 검증한다.
- 유지한 계약: canonical UUID userId/JWT `sub`, Identity와 외부 consumer의 도메인 경계, Firebase/provider 기본 비활성을 유지했다. Jira와 기록에 raw phone·last4·Firebase UID·provider subject·PhoneIdentity fingerprint·HMAC key material 및 Secret·Token·Password·실제 Key·전체 MongoDB URI·사용자 개인정보를 포함하지 않았다.
- 결정사항: TMI-95는 ADR 확정 작업만 소유한다. outbox publisher, 외부 consumer와 production feature 활성화는 ADR 결정 이후 별도 작업으로 진행하고, PR 병합 확인 전 TMI-95를 Done으로 전환하지 않는다.
- 위험 요소: transport 인증 방식, consumer 소유 서비스, bindingRevision 원자 증가 저장 경계, 구 key reference 확인 방식과 법적 보존 기간은 TMI-95에서 결정해야 한다. 현재 outbox 모델에는 eventType·schemaVersion·bindingRevision과 lease/retry/dead-letter 필드가 아직 없다.
- 다음 작업: 구현 전에 Atlassian 공식 MCP로 TMI-95 설명과 완료 조건을 다시 읽고 ADR을 작성한다. 완료 후 Jira 댓글 초안을 먼저 제시하고 PR 병합을 확인하기 전에는 Jira 상태를 완료로 변경하지 않는다.

## 2026-08-14 — Stage 5C 서버 간 계약 ADR 전체 흐름 설명

<!-- codex-turn:019fff20-036d-7c01-948f-e708fccd74b4 -->

- 날짜: 2026-08-14
- 브랜치: `docs/TMI-95-phone-eligibility-binding-adr` (`455db00`, commit·push 미수행)
- Jira: TMI-95
- 작업 목표: 이번에 작성할 Phone eligibility binding 서버 간 계약 ADR이 해결하려는 문제와 Identity producer·outbox publisher·Entitlement/Billing consumer의 책임, 정상·중복·역순·장애·key rotation·보존 흐름을 쉬운 사용자 시나리오로 정리한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. ADR·애플리케이션 코드·테스트는 변경하지 않았고 WORKLOG의 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 회원가입 순간 Identity 메모리에만 존재하는 verified phone으로 benefit-scoped candidate를 계산해 User 생성 Transaction에 `PhoneEligibilityBindingOutbox(PENDING)`를 함께 저장하는 이유를 설명했다. raw phone을 장기 저장하지 않으므로 이 시점에 증명을 남기지 않으면 나중에 무료시험 중복 확인을 위해 번호 재인증이 필요하다.
- 구현 내용: publisher는 여러 worker가 같은 outbox를 동시에 보내지 않도록 atomic lease를 획득하고 versioned `PhoneEligibilityBindingVerified` event를 전송한다. 네트워크 결과가 불명확해 같은 event가 재전송될 수 있으므로 delivery는 exactly-once가 아니라 at-least-once로 설계하며 일시 실패는 retry/backoff, 반복·영구 실패는 dead-letter와 운영 경보로 보낸다.
- 구현 내용: consumer는 `eventId` unique inbox와 `VerifiedPhoneBenefitBinding` 갱신을 같은 로컬 Transaction으로 commit한 뒤에만 2xx를 반환한다. 같은 eventId·같은 payload 재전달은 성공 no-op, 같은 eventId·다른 payload는 poison event 격리, 서로 다른 event의 역순 도착은 user+scope 단조 증가 `bindingRevision`으로 과거 revision을 무시한다.
- 구현 내용: event는 혜택 지급 명령이 아니라 verified phone binding 준비 알림이다. consumer가 binding을 받았다고 TrialClaim이나 UserEntitlement를 생성하지 않고, 사용자의 첫 무료시험 요청에서 candidate로 기존 TrialClaim을 조회해 없을 때만 별도 원자 grant를 수행한다. binding/outbox가 아직 도착하지 않았으면 중복 방지를 우회하지 않고 processing/retry로 fail-closed한다.
- 구현 내용: `consumerScopeId`는 client가 고르지 않고 Identity allowlist 서버 설정으로 결정해 서비스·혜택 간 candidate 상호 비교를 막는다. HMAC key material은 Identity Secret 경계에 남기고 consumer에는 `keyVersion+candidate`만 전달하며, rotation 시 새 candidate를 선배포하고 이전 key를 lookup-only로 유지해 참조가 사라지기 전에는 삭제하지 않는다.
- 구현 내용: schemaVersion은 event JSON 계약, keyVersion은 HMAC candidate 생성 key, bindingRevision은 user+scope 상태 변경 순번으로 분리한다. outbox delivery record, consumer current binding, 장기 TrialClaim ledger는 목적이 다르므로 보존 기간과 탈퇴·phone 변경 삭제 정책도 각각 ADR에서 결정해야 하며 Java 클래스명·Mongo document 구조와 민감정보는 wire 계약에 노출하지 않는다.
- 실행한 테스트와 결과: 코드 변경이 없는 개념 설명과 기록 작업이므로 Gradle 테스트를 실행하지 않았다. 병합 기준선은 70개 suite·421개 테스트 성공 상태이며 현재 outbox entity·가입 생성 흐름·계획·TMI-95 기록을 대조했다. 종료 전 `git diff --check`, 기록 문서 trailing whitespace, 지정 turn marker 단일 존재와 WORKLOG 끝 append를 정적으로 검증한다.
- 유지한 계약: canonical UUID userId/JWT `sub`, Identity producer와 외부 Entitlement/Billing consumer의 도메인 경계, 가입 event와 혜택 지급 분리 및 Firebase/provider 기본 비활성을 유지했다. raw phone·last4·Firebase UID·provider subject·PhoneIdentity fingerprint·HMAC key material과 Secret·Token·Password·실제 Key·전체 MongoDB URI를 payload·로그·기록에 포함하지 않았다.
- 결정사항: TMI-95 ADR은 서버 간 합의 문서이며 publisher·consumer 구현이나 production 활성화 자체가 아니다. 핵심 합의는 versioned event, at-least-once+eventId 멱등성, bindingRevision 순서 보호, fail-closed eligibility, key lifecycle과 목적별 보존 정책이다. Jira·Git 상태를 변경하지 않았다.
- 위험 요소: 현재 outbox는 PENDING/PUBLISHED와 candidate 저장까지만 있고 eventType·schemaVersion·bindingRevision, lease·retry·dead-letter가 아직 없다. transport 인증, 실제 consumer 소유 서비스, revision 원자 증가 경계, 법적 보존 기간과 구 key reference 확인도 ADR에서 확정해야 한다.
- 다음 작업: ADR에서 정상 sequence와 중복·timeout·역순·unknown schema·key rotation·withdrawal sequence를 결정표로 확정한다. 승인된 ADR 이후 Identity publisher와 외부 consumer 구현은 각각 별도 Jira로 진행하고 staging consumer end-to-end 확인 전 production signup flag를 활성화하지 않는다.

## 2026-08-14 — Entitlement/Billing과 Learning Core 배포 경계 설명

<!-- codex-turn:019fff23-8347-7131-bcfe-ced858e81858 -->

- 날짜: 2026-08-14
- 브랜치: `docs/TMI-95-phone-eligibility-binding-adr` (`455db00`, commit·push 미수행)
- Jira: TMI-95
- 작업 목표: 계획서의 Entitlement/Billing 서버가 Learning Core를 의미하는지, 별도 서비스를 새로 만들어야 하는지 현재 계약과 미확정 사항을 구분해 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. ADR·애플리케이션 코드·테스트는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 계획서의 Entitlement/Billing은 Learning Core 자체가 아니라 `VerifiedPhoneBenefitBinding`, `TrialClaim`, `UserEntitlement`, `EntitlementReservation`을 소유하는 별도 논리적 bounded context임을 확인했다. Learning Core는 시험 도메인을 소유하고 Entitlement의 사용권을 `reserve`한 뒤 exam 생성과 `confirm`을 조율한다.
- 구현 내용: 계획서에는 Entitlement 트랙을 별도 저장소·별도 서비스로 병렬 진행한다고 명시돼 있어 장기 권장 배포 구조는 독립 Entitlement/Billing 서비스다. 다만 당장 별도 프로세스·인프라를 신설할지, 초기에는 Learning Core 배포물 안의 독립 모듈로 둘지는 현재 확정된 구현 결정이 아니며 TMI-95 ADR에서 실제 consumer owner와 함께 명시해야 한다.
- 구현 내용: 초기에는 같은 Learning Core 배포물을 사용하더라도 시험 entity에 TrialClaim을 섞지 않고 데이터·Transaction·package·API 경계를 분리해야 나중에 별도 서비스로 안전하게 분리할 수 있음을 정리했다. Identity 저장소에는 시험·TrialClaim·Entitlement 코드를 추가하지 않는다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: TMI-95 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 계획서의 서버 책임과 물리 배포 단위에 대한 설명을 요청했다. ADR 작성·외부 저장소 구현·새 서비스 생성·Jira 변경·Git commit·push는 승인 범위에 포함되지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 계약 분석과 기록 작업이므로 Gradle 테스트를 실행하지 않았다. 병합 기준선은 70개 suite·421개 테스트 성공 상태이며 종료 전 문서 정적 검증만 수행한다.
- 유지한 계약: Identity는 회원·verified phone·consumer-scoped binding outbox만 소유하고, Entitlement/Billing은 혜택 ledger와 사용권, Learning Core는 시험 생성을 소유한다. canonical UUID userId/JWT `sub`, Firebase/provider 기본 비활성과 Secret·Token·Password·실제 Key·전체 MongoDB URI 비기록 원칙을 유지했다.
- 결정사항: Entitlement/Billing은 Learning Core와 논리적으로 별도다. 장기적으로는 별도 서비스·저장소가 권장되지만 물리 서버를 즉시 신설하는 결정은 아직 확정되지 않았다. 동일 배포물로 시작하더라도 bounded context 경계는 분리한다.
- 위험 요소: consumer owner와 배포 단위를 ADR에서 명시하지 않으면 Identity publisher의 endpoint·인증·운영 책임과 무료시험 reserve/confirm Transaction 경계가 모호해진다. Learning Core 시험 모델에 Entitlement ledger를 직접 섞으면 결제 확장과 서비스 분리가 어려워진다.
- 다음 작업: TMI-95 ADR에서 consumer 소유 팀·저장소·배포 단위, transport endpoint·인증, 실패 책임을 확정한다. 별도 서비스 신설 또는 Learning Core 내부 독립 모듈 구현은 승인된 외부 Jira에서 진행한다.

## 2026-08-14 — TMI-95 Phone eligibility binding 서버 간 계약 ADR 구현

<!-- codex-turn:019fff20-300c-7132-bc21-9eb74d44c5cf -->

- 날짜: 2026-08-14
- 브랜치: `docs/TMI-95-phone-eligibility-binding-adr` (`455db00`, commit·push 미수행)
- Jira: TMI-95
- 작업 목표: production Firebase signup 활성화 전에 Identity producer와 별도 Entitlement/Billing consumer가 따라야 할 Phone eligibility binding versioned event, 멱등성·순서, scope·key, transport, 보존·삭제 계약을 ADR로 확정한다.
- 변경 파일: 신규 `docs/adr/ADR-002-phone-eligibility-binding-server-contract.md`, `docs/adr/ADR-001-firebase-authentication-broker.md`, `docs/contracts/social-login-implementation-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·테스트 코드는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: schema v1의 `PhoneEligibilityBindingVerified`와 candidate가 없는 `PhoneEligibilityBindingRevoked` JSON을 확정했다. 공통 envelope는 immutable eventId, eventType, schemaVersion, producer, occurredAt, allowlist된 consumerScopeId, canonical UUID userId와 user+scope 단조 bindingRevision을 사용하고 verified event는 verifiedAt과 retained candidate 전체를 포함한다.
- 구현 내용: consumer는 candidate set을 정규화한 canonical payload digest inbox, revision high-water와 current binding을 같은 로컬 Transaction으로 commit한 뒤에만 2xx를 반환한다. 같은 eventId·같은 payload는 no-op, 다른 payload와 다른 eventId·같은 revision은 poison conflict, 낮은 revision은 stale success, 높은 revision과 gap은 완전한 state event로 적용·경보하도록 결정했다.
- 구현 내용: consumerScopeId는 public/client 입력이 아니라 Identity 배포 allowlist만 사용하고 현재 eligibility domain separator에 포함한다. HMAC key material은 Identity만 소유하며 consumer에는 keyVersion과 candidate만 전달한다. 새 key LOOKUP_ONLY 선배포, ACTIVE_WRITE 전환, rollback, consumer reference 0 또는 승인 보존 기간 만료 전 legacy key 제거 금지 순서를 정의했다.
- 구현 내용: v1 transport는 private HTTPS push와 5분 이하 platform workload identity JWT로 결정했다. timeout·429·5xx는 같은 eventId/payload retry, contract 오류는 dead-letter, 인증 실패는 event 격리와 scope delivery 중지로 처리하며 60초 lease, 지수 backoff+jitter, 12회 시도와 수동 replay 불변식을 문서화했다.
- 구현 내용: producer PUBLISHED 30일, DEAD_LETTER 90일, consumer inbox digest와 revoke revision tombstone 120일의 기본 보존을 정했다. current binding은 교체·revoke까지 유지하고 abuse/claim ledger는 제품·개인정보·법무가 승인한 별도 최대 기간을 요구하며 무기한 기본 보존을 금지했다. phone 교체는 높은 verified revision으로 전체 교체하고 해제·탈퇴는 높은 revoked revision으로 candidate를 제거한다.
- 구현 내용: v1 consumer owner는 별도 Entitlement/Billing bounded context·배포 서비스로 확정했다. Learning Core는 binding·claim ledger를 직접 저장하지 않고 추후 reserve/confirm 계약만 사용하며, 외부 consumer 구현과 인프라 provisioning은 별도 저장소·Jira 범위로 남겼다.
- 구현 내용: 현재 outbox가 충족하는 별도 key/domain·opaque scope·PENDING 저장·redaction·기본 비활성과 아직 없는 eventType·schemaVersion·bindingRevision·revoke·lease/retry/dead-letter·consumer Transaction을 구분했다. ADR-001과 social login 계획에 ADR-002 교차 참조를 추가하고 production gate와 Identity/consumer 후속 Jira 분리를 명시했다.
- 수행한 Jira 작업: 구현 전에 Atlassian 공식 MCP로 TMI-95의 제목·설명·완료 조건·제외 범위·상태를 읽기 전용 확인했고 AGENTS.md 및 기존 계약과 충돌이 없음을 확인했다. Jira 이슈·설명·댓글·상태·Resolution은 변경하지 않았다.
- 추가한 댓글의 목적: versioned verified/revoked schema, 멱등·ordering Transaction, scope/key/transport 결정, 보존 기간, 전체 테스트와 publisher·consumer 미구현 위험을 전달하는 종료 댓글 초안을 준비하되 자동 등록하지 않는다.
- 변경한 상태: Jira TMI-95는 기존 `해야 할 일`, Resolution 없음 상태를 유지한다. PR 병합을 확인하지 않았으므로 Done 전환하지 않았다.
- 승인 여부: 사용자가 `구현 해줘`라고 TMI-95 ADR 구현을 명시적으로 요청했다. Jira 댓글·상태 전환, 외부 consumer 서비스·인프라 생성, production flag 활성화와 Git commit·push는 승인 범위에 포함되지 않았다.
- 실행한 테스트와 결과: ADR의 JSON code block 2개를 실제 JSON parser로 검증했다. 첫 Gradle 실행은 사용자 Gradle cache lock의 sandbox 접근 제한으로 시작하지 못해 승인된 명령 범위로 다시 실행했고, 최종 `./gradlew clean test`는 70개 suite·421개 테스트가 failure 0, error 0, skipped 0으로 성공했다. 실제 Firebase·Atlas·외부 OAuth Provider·Entitlement consumer는 호출하지 않았다.
- 유지한 계약: canonical UUID userId/JWT `sub`, Identity·Entitlement/Billing·Learning Core 도메인 경계, Firebase/provider 기본 비활성과 가입 event·혜택 지급 분리를 유지했다. raw phone·last4·Firebase UID·provider subject·PhoneIdentity fingerprint·eligibility candidate·HMAC key material과 Secret·Token·Password·실제 Key·전체 MongoDB URI를 로그·metric·작업 기록에 포함하지 않았다.
- 결정사항: schemaVersion·keyVersion·bindingRevision은 각각 wire 구조, HMAC candidate origin, user+scope 상태 순서를 관리한다. event 수신은 혜택 지급이 아니며 binding 미도착은 fail-closed processing 상태다. ADR은 조건부 채택이고 publisher·consumer 구현과 staging gate가 끝나기 전 production ready가 아니다.
- 위험 요소: Identity outbox publisher·revoke lifecycle과 Entitlement/Billing service가 아직 구현되지 않았다. 실제 workload identity issuer·audience, consumer on-call, abuse ledger 법적 최대 보존 기간, key reference count와 staging Mongo/transport 장애 검증이 남아 있다.
- 다음 작업: 사용자가 변경을 검토해 직접 commit·push하고 PR을 생성한다. PR 병합 확인 전 TMI-95를 Done으로 변경하지 않으며 Jira 종료 댓글은 초안을 먼저 승인받은 뒤에만 등록한다. 후속 Jira는 Identity publisher와 별도 Entitlement/Billing consumer 구현으로 나눈다.

## 2026-08-14 — TMI-95 PR 병합 확인·Jira 종료 변경안 및 다음 개발 범위

<!-- codex-turn:019fff2d-3d4c-7060-9f6b-c5188635c04e -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`31130fd`, GitHub PR #23 merge commit, Codex commit·push 미수행)
- Jira: TMI-95
- 작업 목표: TMI-95 ADR PR 병합과 Jira 완료 가능 여부를 확인하고, 실제 Jira 변경 전에 종료 댓글·상태 전환안을 공개하며 다음 Identity·consumer 개발 범위를 분리한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션·ADR·테스트는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 로컬 `develop`과 `origin/develop`이 PR #23 merge commit `31130fd`를 가리키고 TMI-95 feature commit `cfefbec`이 이력에 포함된 것을 확인했다. GitHub CLI 원격 조회는 sandbox network 제한으로 실패했지만 fetch된 origin ref와 merge commit 제목·부모 이력으로 병합을 확인했다.
- 수행한 Jira 작업: Atlassian 공식 MCP로 TMI-95의 제목, 댓글 없음, 상태 `해야 할 일`, Resolution 없음과 사용할 수 있는 완료 transition ID `41`을 읽기 전용 조회했다. Jira 댓글·상태·Resolution·필드는 변경하지 않았다.
- 추가한 댓글의 목적: ADR-002의 versioned verified/revoked schema, eventId·revision 처리, scope·key·transport·보존 결정, 전체 테스트 결과와 publisher·consumer 미구현 위험을 인수인계하는 종료 댓글 초안을 사용자에게 공개한다.
- 변경한 상태: TMI-95는 `해야 할 일`, Resolution 없음 상태를 유지한다. 실제 완료 전환은 공개된 변경안의 사용자 승인을 받은 뒤에만 수행한다.
- 승인 여부: 사용자가 Jira 종료를 요청했지만 저장소 규칙상 실제 변경 전에 댓글과 transition 내용을 먼저 공개하고 별도 승인을 받아야 하므로 이번 조회 turn에서는 변경하지 않았다.
- 실행한 테스트와 결과: 병합·Jira 읽기 확인, 다음 범위 정리와 문서 기록만 수행해 Gradle 테스트를 재실행하지 않았다. PR #23에 포함된 최종 검증은 ADR JSON 예시 2개 파싱 성공과 `./gradlew clean test` 70개 suite·421개 테스트 failure 0, error 0, skipped 0이다.
- 유지한 계약: canonical UUID userId/JWT `sub`, Identity·Entitlement/Billing·Learning Core 도메인 경계, Firebase/provider 기본 비활성과 가입 event·혜택 지급 분리를 유지했다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 Jira 변경안이나 기록에 포함하지 않았다.
- 결정사항: 다음 Identity 저장소 작업은 outbox schema/revision·revoke와 atomic lease·retry/dead-letter HTTPS publisher다. 외부 Entitlement/Billing consumer는 별도 저장소·Jira에서 inbox·current binding·revision high-water와 abuse ledger를 구현하고, 두 트랙의 staging contract test가 끝나기 전 production Firebase signup을 활성화하지 않는다.
- 위험 요소: Identity publisher, withdrawal/phone lifecycle revoke와 실제 workload identity adapter가 아직 없다. Entitlement/Billing 서비스·datastore·on-call, abuse ledger 보존 기간과 key reference count도 미구현이므로 ADR 완료가 production ready를 뜻하지 않는다.
- 다음 작업: 사용자가 종료 댓글과 완료 transition을 승인하면 Atlassian 공식 MCP로 댓글을 등록하고 transition ID `41`만 적용한 뒤 결과를 재조회한다. 이후 Identity publisher 작업 Jira 생성안을 먼저 제시한다.

## 2026-08-14 — TMI-95 Jira 종료 댓글 등록·완료 전환

<!-- codex-turn:019fff30-11a4-7da1-a051-d18a36cd8cd3 -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`31130fd`, GitHub PR #23 merge commit, Codex commit·push 미수행)
- Jira: TMI-95
- 작업 목표: PR #23 병합이 확인된 TMI-95에 승인된 종료 댓글을 등록하고 완료 상태로 전환한 뒤 결과를 검증한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션·ADR·테스트는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 직전 turn에서 PR #23 merge commit `31130fd`, TMI-95의 `해야 할 일` 상태·Resolution 없음·빈 댓글과 완료 transition ID `41`을 확인하고, 적용할 종료 댓글과 상태 변경을 사용자에게 공개했다.
- 수행한 Jira 작업: 사용자 승인에 따라 TMI-95에 ADR-002의 versioned verified/revoked schema, eventId·bindingRevision 처리, scope·key·transport·보존 결정, 변경 문서, 테스트 결과와 후속 위험을 요약한 댓글 ID `10006`을 등록했다. 이어서 transition ID `41`만 적용하고 후속 조회에서 상태 ID `10003` `완료`와 Resolution `완료`를 확인했다.
- 추가한 댓글의 목적: 구현·검증 결과와 Identity publisher·revoke lifecycle, 외부 Entitlement/Billing consumer, workload identity 및 staging E2E가 후속 범위임을 인수인계하기 위해 등록했다.
- 변경한 상태: Jira TMI-95를 `해야 할 일`에서 `완료`로 전환했고 Resolution도 `완료`가 됐다. 다른 Jira·필드는 변경하지 않았다.
- 승인 여부: 직전 응답에서 댓글 전문과 transition ID `41`, 변경하지 않을 필드를 공개했고 사용자가 `어 닫아줘`라고 명시적으로 승인했다.
- 실행한 테스트와 결과: Jira 댓글·상태 전환과 기록 문서 변경만 수행해 Gradle 테스트를 재실행하지 않았다. PR #23의 최종 검증은 ADR JSON 예시 2개 파싱 성공과 `./gradlew clean test` 70개 suite·421개 테스트 failure 0, error 0, skipped 0이다.
- 유지한 계약: canonical UUID userId/JWT `sub`, Identity·Entitlement/Billing·Learning Core 도메인 경계, Firebase/provider 기본 비활성과 가입 event·혜택 지급 분리를 유지했다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 Jira 댓글이나 작업 기록에 포함하지 않았다.
- 결정사항: TMI-95 ADR 작업은 완료됐지만 production 기능은 아직 활성화할 수 없다. 다음 Identity 범위는 outbox schema/revision·revoke와 atomic lease·retry/dead-letter HTTPS publisher이며 외부 consumer는 별도 저장소·Jira로 유지한다.
- 위험 요소: Identity publisher와 withdrawal/phone lifecycle revoke, 실제 workload identity adapter, Entitlement/Billing consumer·datastore·on-call, abuse ledger 보존 기간과 staging E2E가 아직 남아 있다.
- 다음 작업: Identity publisher 구현 Jira 생성안을 제목·설명·완료 조건·제외 범위로 먼저 제시하고 승인 뒤 생성한다. publisher와 consumer contract test가 staging에서 통과하기 전 production Firebase signup flag를 활성화하지 않는다.

## 2026-08-14 — Stage 5D Phone eligibility binding outbox publisher Jira 생성 초안

<!-- codex-turn:019fff35-8654-7af1-a97c-74bf817f22c3 -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`31130fd`, commit·push 미수행)
- 작업 목표: 완료된 TMI-95 ADR의 다음 Identity 구현인 outbox schema/revision·revoke와 atomic lease·retry/dead-letter HTTPS publisher를 추적할 Jira Payload를 준비한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션·ADR·테스트는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 다음 Jira를 `[Identity] Stage 5D Phone eligibility binding outbox publisher`, TMI `작업`, High, 기본 `해야 할 일`로 제안했다. schema v1 immutable event, user+scope atomic revision, verified/revoked lifecycle, lease claim, retry/backoff·dead-letter, HTTPS delivery/credential port, retention cleanup과 외부 인프라 없는 테스트를 범위로 정리했다.
- 구현 내용: 외부 Entitlement/Billing consumer의 inbox·binding·claim ledger, TrialClaim·Entitlement·시험 코드, cloud 인프라 provisioning, production Firebase 활성화, legacy password route 종료와 Git commit·push는 제외 범위로 분리했다.
- 수행한 Jira 작업: Atlassian 공식 MCP 검색으로 TMI 프로젝트에 PhoneEligibilityBindingOutbox publisher·bindingRevision·lease/retry/dead-letter 관련 중복 이슈가 없음을 읽기 전용 확인했다. Jira 생성·수정·댓글·상태 전환은 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: 기존 Jira 상태나 Resolution을 변경하지 않았고 신규 Jira도 아직 생성하지 않았다.
- 승인 여부: 사용자가 다음 작업 Jira 생성을 요청했지만 저장소 규칙상 실제 생성 전에 전체 Payload를 공개하고 별도 승인을 받아야 하므로 승인을 기다린다.
- 실행한 테스트와 결과: Jira 중복 조회·Payload 작성과 기록 문서 변경만 수행해 Gradle 테스트를 실행하지 않았다. 현재 병합 기준선은 `./gradlew clean test` 70개 suite·421개 테스트 성공 상태이며 종료 전 `git diff --check`와 turn marker 단일 존재를 확인한다.
- 유지한 계약: canonical UUID userId/JWT `sub`, Identity producer와 별도 Entitlement/Billing consumer 경계, Firebase/provider 기본 비활성과 event 수신·혜택 지급 분리를 유지했다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 Jira 초안이나 기록에 포함하지 않았다.
- 결정사항: 다음 Identity Jira는 publisher 구현까지 소유하되 consumer와 cloud 인프라 생성은 소유하지 않는다. workload identity는 static credential을 설정에 저장하지 않는 provider port로 격리하고 기능은 기본 비활성·설정 누락 fail-closed로 유지한다.
- 위험 요소: 실제 consumer endpoint·workload identity 발급 인프라와 운영 on-call이 아직 없으므로 Identity publisher 코드만 완료돼도 production ready가 아니다. withdrawal/phone lifecycle Transaction에 revoke event를 연결할 때 기존 User·Session rollback 계약을 깨지 않도록 별도 동시성 테스트가 필요하다.
- 다음 작업: 사용자에게 Jira 제목·설명·완료 조건·제외 범위와 미설정 필드를 공개한다. 승인하면 동일 Payload로 생성하고 발급된 Jira 키·상태·우선순위를 재조회해 기록한다.

## 2026-08-14 — TMI-96 Stage 5D Phone eligibility binding outbox publisher Jira 생성

<!-- codex-turn:019fff37-f0be-7be0-936e-727f407d501e -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`31130fd`, commit·push 미수행)
- Jira: TMI-96
- 작업 목표: 사용자에게 공개한 Stage 5D Identity outbox publisher Jira Payload를 승인 내용 그대로 생성하고 저장 결과를 검증한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션·ADR·테스트는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: TMI-96 설명에 ADR-002 schema v1 verified/revoked event, user+scope atomic bindingRevision, lifecycle Transaction, PENDING·IN_FLIGHT·PUBLISHED·DEAD_LETTER 상태 머신, atomic lease, retry/backoff·실패 분류, HTTPS delivery·credential port, 보존 cleanup·안전한 metric과 외부 인프라 없는 테스트를 기록했다.
- 구현 내용: 외부 Entitlement/Billing consumer, TrialClaim·Entitlement·시험 코드, cloud workload identity 인프라 provisioning, production Firebase 활성화, legacy password API 종료, 공개 수동 replay API와 Git commit·push는 제외 범위로 유지했다.
- 수행한 Jira 작업: 사용자 승인에 따라 Atlassian 공식 MCP로 TMI `작업` 이슈 `TMI-96`을 생성했다. 생성 직후 읽기 전용 재조회해 승인한 제목·설명, 유형 `작업`, 우선순위 `High`, 상태 `해야 할 일`, Resolution 없음, 담당자 없음, 빈 라벨·컴포넌트를 확인했다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: 생성 시 기본 상태 `해야 할 일`을 유지하고 별도 transition을 적용하지 않았다. 기존 Jira의 상태나 Resolution은 변경하지 않았다.
- 승인 여부: 직전 응답에서 프로젝트·유형·제목·우선순위·설명·완료 조건·제외 범위와 미설정 필드를 공개했고 사용자가 `어 생성해줘`라고 명시적으로 승인했다.
- 실행한 테스트와 결과: Jira 생성·검증과 기록 문서 변경만 수행해 Gradle 테스트를 실행하지 않았다. 현재 병합 기준선은 `./gradlew clean test` 70개 suite·421개 테스트 성공 상태이며 종료 전 `git diff --check`를 실행한다.
- 유지한 계약: canonical UUID userId/JWT `sub`, Identity producer와 별도 Entitlement/Billing consumer 경계, Firebase/provider 기본 비활성과 event 수신·혜택 지급 분리를 유지했다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 Jira나 작업 기록에 포함하지 않았다.
- 결정사항: TMI-96은 Identity publisher 코드까지 소유하지만 외부 consumer와 cloud 인프라 생성을 소유하지 않는다. static credential 설정을 금지하고 workload identity credential provider를 격리하며 production gate는 계속 닫아 둔다.
- 위험 요소: 실제 consumer endpoint·workload identity 발급 인프라·운영 on-call이 없으므로 TMI-96만 완료돼도 production ready가 아니다. withdrawal/phone lifecycle revoke와 기존 User·Session rollback 경계의 동시성 검증이 필요하다.
- 다음 작업: 구현 전에 Atlassian 공식 MCP로 TMI-96 설명과 완료 조건을 다시 읽고 ADR-002와 현재 outbox를 대조한다. schema/revision·lifecycle Transaction을 먼저 구현한 뒤 lease publisher·delivery·cleanup 순으로 검증한다.

## 2026-08-14 — Identity와 외부 Entitlement 선행 개발 관계 확인

<!-- codex-turn:019fff46-5b57-7391-900a-1c773aa6b562 -->

- 날짜: 2026-08-14
- 브랜치: `feat/TMI-96-phone-eligibility-outbox-publisher` (`31130fd`, commit·push 미수행)
- Jira: TMI-96
- 작업 목표: 현재 Identity 수정이 별도 저장소의 Entitlement/Billing을 전제로 진행되는 것인지와 실제 구현 준비 상태를 구분해 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 이번 설명을 위해 갱신했다. 동시에 진행 중인 TMI-96 애플리케이션 변경에는 손대지 않았고 WORKLOG의 다른 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 목표 구조에서는 Entitlement/Billing consumer가 별도 저장소에서 구현된다고 가정하고 Identity가 producer 측 verified phone candidate·outbox와 전달 기능을 먼저 준비한다고 정리했다. 그러나 외부 consumer가 이미 구현돼 있거나 현재 end-to-end 전달이 동작한다는 가정은 하지 않는다.
- 구현 내용: TMI-95에서 서버 간 ADR을 확정했고 현재 TMI-96 범위로 Identity publisher 구현을 진행한다. 외부 consumer·inbox와 TrialClaim·UserEntitlement 처리는 별도 저장소 책임이며, 양쪽 구현과 staging 검증 전에는 production eligibility 연동을 활성화하지 않는다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: TMI-96 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 현재 개발 전제에 대한 설명을 요청했다. 외부 저장소 변경·Jira 변경·Git commit·push는 승인 범위에 포함되지 않았다.
- 실행한 테스트와 결과: 이번 설명과 기록을 위한 별도 Gradle 테스트는 실행하지 않았다. 현재 TMI-96 구현 변경은 완료 전이므로 그 검증 결과를 이번 분석 결과로 주장하지 않으며 종료 전 문서 정적 검증만 수행한다.
- 유지한 계약: Identity는 verified phone과 generic eligibility outbox·publisher만 소유하며 TrialClaim·UserEntitlement·시험 코드를 포함하지 않는다. 외부 consumer가 준비되지 않은 상태에서 중복 혜택 방지를 우회하지 않고 fail-closed하며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 별도 Entitlement/Billing은 목표 아키텍처상의 downstream이며 현재 존재·완료한 시스템으로 간주하지 않는다. Identity와 consumer는 확정된 계약을 기준으로 독립 개발하되 production 활성화 전에 양쪽 구현과 end-to-end 전달을 검증한다.
- 위험 요소: Identity producer만 배포하고 consumer 없이 가입 연동을 활성화하면 outbox가 누적되고 사용자의 첫 무료시험 eligibility가 계속 processing 상태가 될 수 있다.
- 다음 작업: TMI-96에서 Identity publisher를 구현하고 별도 저장소 consumer를 후속 작업으로 구현한 뒤 staging end-to-end 전달과 fail-closed 동작을 검증한다.

## 2026-08-14 — Identity 현재·예정 entity 연결 구조 설명

<!-- codex-turn:019fff4a-078b-7a30-8239-b16eda760d5e -->

- 날짜: 2026-08-14
- 브랜치: `feat/TMI-96-phone-eligibility-outbox-publisher` (`31130fd`, commit·push 미수행)
- Jira: TMI-96
- 작업 목표: Identity에 현재 존재하는 Mongo entity와 앞으로 Identity 또는 외부 Entitlement/Billing에 생길 예정인 entity의 책임과 전체 연결 흐름을 쉽게 구분해 설명한다.
- 변경 파일: 이번 분석 기록으로 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 수정했다. 동시에 진행 중인 TMI-96 애플리케이션 변경에는 손대지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 현재 Identity Mongo document를 계정 root `User`, 인증수단 mapping `FirebaseIdentity`·`SocialIdentity`·`PhoneIdentity`·`PhoneFingerprintAlias`, 인증 세션 `RefreshSession`, 가입 전 임시 절차 `FirebaseEnrollmentAttempt`, 외부 전달 `PhoneEligibilityBindingOutbox`로 분류했다. embedded `UserConsents`와 비영속 결과인 `PreparedRefreshSession`·`IssuedRefreshSession`·`IssuedAccessToken`도 entity와 구별했다.
- 구현 내용: TMI-96 작업 트리에 추가 중인 `PhoneEligibilityBindingRevision`은 user+scope별 event 순번과 현재 활성 여부, `PhoneEligibilityBindingDeliveryScopeState`는 contract·인증 오류가 난 consumer scope의 전송 중지를 기억하는 producer 운영 document라고 정리했다. 두 모델은 현재 구현 진행 중이므로 완료된 기능으로 단정하지 않는다.
- 구현 내용: 이후 Identity 계획 후보인 Guest merge `UserMergedOutbox`, 탈퇴·Firebase cleanup lifecycle outbox/saga 상태는 아직 확정·완료 entity가 아님을 표시했다. 별도 Entitlement/Billing의 inbox·revision high-water, `VerifiedPhoneBenefitBinding`, `TrialClaim`, `UserEntitlement`, `EntitlementReservation`과 Learning Core 시험 entity는 Identity에 추가하지 않는 경계를 유지했다.
- 구현 내용: Firebase proof 검증과 `FirebaseEnrollmentAttempt(PENDING)`에서 시작해 가입 Transaction이 User·각 identity·RefreshSession·eligibility outbox를 저장하고 attempt를 CONSUMED로 바꾼 뒤, publisher와 Entitlement inbox/binding, 첫 무료시험의 TrialClaim·UserEntitlement, Learning Core의 reserve·exam 생성·confirm으로 이어지는 전체 생명주기를 연결했다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: TMI-96 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 현재·예정 entity의 개념 설명을 요청했다. 애플리케이션 구현·외부 저장소 변경·Jira 변경·Git commit·push는 이번 요청의 승인 범위에 포함되지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석과 기록이므로 이번 요청을 위한 Gradle 테스트는 실행하지 않았다. TMI-96 작업 트리의 구현은 진행 중이므로 완료나 테스트 성공을 이번 설명의 결과로 주장하지 않았고 종료 전 문서 정적 검증만 수행한다.
- 유지한 계약: canonical UUID userId/JWT `sub`, Firebase credential broker와 Identity account owner, PhoneIdentity fingerprint와 benefit candidate의 key/domain 분리, Identity·Entitlement/Billing·Learning Core 소유 경계를 유지했다. Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: entity를 장기 계정, 인증수단 mapping, 세션, 단기 가입 절차, 전달/운영, 외부 혜택 ledger로 구분해 이해한다. event outbox는 혜택 자체가 아니며 Entitlement의 binding도 혜택 지급 자체가 아니다.
- 위험 요소: TMI-96의 revision·delivery scope document와 publisher 상태 머신은 현재 작업 중이므로 최종 코드·테스트에 따라 세부 필드가 달라질 수 있다. lifecycle outbox/saga 이름과 구조도 후속 ADR 전에는 확정 모델로 취급하면 안 된다.
- 다음 작업: TMI-96 완료 후 실제 확정된 producer entity와 상태 전이를 다시 대조하고, 별도 Entitlement/Billing 저장소 작업에서는 inbox·binding·claim·entitlement·reservation 모델과 unique/Transaction 계약을 별도 문서로 작성한다.

## 2026-08-14 — Phone eligibility 이벤트 용어집 설명

<!-- codex-turn:019fff68-7d26-71d3-b8c4-5da4fb2e0335 -->

- 날짜: 2026-08-14
- 브랜치: `feat/TMI-96-phone-eligibility-outbox-publisher` (`31130fd`, commit·push 미수행)
- Jira: TMI-96
- 작업 목표: `fingerprintCandidates`처럼 entity 설명만으로 이해하기 어려운 Phone eligibility의 보안·이벤트·전달 용어를 쉬운 정의와 서로의 관계로 정리한다.
- 변경 파일: 이번 분석 기록으로 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 수정했다. 동시에 진행 중인 TMI-96 애플리케이션 변경에는 손대지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: raw phone, E.164 정규화, fingerprint, HMAC-SHA-256, domain separator, eligibility candidate, `fingerprintCandidates`, key ring·`keyVersion`·rotation을 한 흐름으로 설명했다. candidate 한 개는 한 key version으로 만든 비교값이며 candidates 배열은 rotation 중 retained version 전체를 지원하는 현재 binding의 완전한 집합이지 여러 전화번호 목록이 아님을 명확히 했다.
- 구현 내용: `consumerScopeId`, opaque, binding, verified/revoked, `bindingRevision`, state event를 목적·상태 용어로 구분했다. 같은 phone도 scope·domain이 다르면 다른 값이 나오고 bindingRevision은 event schema나 key 판본이 아니라 같은 user+scope의 최신 상태 순서임을 유지했다.
- 구현 내용: producer·consumer, payload·envelope, outbox·publisher·inbox, eventId·멱등성, at-least-once, lease·retry·backoff·dead-letter, high-water mark·stale event·poison event, canonical payload·digest, fail-closed·pseudonymous data를 택배 발송·수령 비유와 함께 정리했다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: TMI-96 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 문서에 반복되는 기술 용어 설명을 요청했다. 애플리케이션 구현·계약 변경·외부 저장소 변경·Jira 변경·Git commit·push는 이번 요청의 승인 범위에 포함되지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 개념 설명과 기록이므로 이번 요청을 위한 Gradle 테스트는 실행하지 않았다. 종료 전 `git diff --check`와 turn marker 단일 존재를 정적으로 검증한다.
- 유지한 계약: raw phone과 PhoneIdentity fingerprint를 Entitlement payload로 전달하지 않고, eligibility candidate는 consumer scope와 별도 key/domain으로 분리한다. candidate도 가명정보로 취급하며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: `fingerprintCandidates`는 복수 전화번호가 아니라 key rotation 호환을 위한 동일 전화번호의 version별 eligibility candidate 집합이다. outbox event 수신은 binding 준비일 뿐 TrialClaim·UserEntitlement 지급을 의미하지 않는다.
- 위험 요소: fingerprint를 익명정보나 암호화된 phone으로 오해하면 복호화 가능성·보존 정책을 잘못 판단할 수 있다. candidate 배열을 patch나 여러 번호로 해석하거나 keyVersion·schemaVersion·bindingRevision을 혼용하면 중복 혜택과 역순 상태 적용 위험이 있다.
- 다음 작업: TMI-96 구현·운영 문서에서 같은 용어와 상태명을 일관되게 사용하고, 외부 Entitlement/Billing consumer 문서에도 동일 glossary와 event 예시를 포함한다.

## 2026-08-14 — TMI-96 Stage 5D Phone eligibility binding outbox publisher 구현

<!-- codex-turn:019fff41-8908-73f2-8998-45043cdbd1b0 -->

- 날짜: 2026-08-14
- 브랜치: `feat/TMI-96-phone-eligibility-outbox-publisher` (`31130fd` 기준, commit·push 미수행)
- Jira: TMI-96
- 작업 목표: ADR-002에 고정된 Phone eligibility binding schema v1, user+scope revision·verified/revoked lifecycle, atomic lease·retry/dead-letter HTTPS publisher와 보존·보안 계약을 Identity producer에 구현한다.
- 변경 파일: `PhoneEligibilityBindingOutbox`와 event/status/failure enum, `PhoneEligibilityBindingRevision`, `PhoneEligibilityBindingDeliveryScopeState`, 세 Mongo repository와 custom fragment, federation signup·PhoneIdentity·withdrawal Transaction, publisher application·wire mapper·retry policy, HTTPS/workload identity infrastructure·scheduler·properties/configuration, `application.yml`·`application-test.yml`, `.env.example`, `README.md`, 관련 단위·Transaction·Mongo 통합 테스트, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 변경했다. WORKLOG 과거 항목은 수정·삭제하지 않았다.
- 구현 내용: immutable UUID eventId, `PhoneEligibilityBindingVerified`·`PhoneEligibilityBindingRevoked`, `schemaVersion=1`, producer `identity`, `(userId, consumerScopeId, bindingRevision)` unique와 user+scope atomic revision을 추가했다. 가입 verified와 phone create/rotation/replace, 교체 release revoke, 회원 탈퇴 revoke를 해당 Mongo Transaction 안에서 outbox와 함께 저장한다.
- 구현 내용: outbox를 `PENDING`·`IN_FLIGHT`·`PUBLISHED`·`DEAD_LETTER`로 확장하고 due/expired lease atomic claim, attempt 증가, lease owner 조건부 상태 전이, 5초 시작·15분 cap·±20% jitter backoff, 최대 12회, 400/409/422 등 permanent dead-letter, 408/425/429/5xx·timeout/connection retry, 401/403 dead-letter와 scope pause, 동일 eventId/payload 수동 replay를 구현했다.
- 구현 내용: 최대 16 KiB canonical JSON wire mapper, HTTPS-only·redirect 금지·response body 미보관 JDK adapter, 최대 5분 credential을 발급하는 workload identity provider port와 민감 Token redaction을 추가했다. publisher는 기본 비활성이고 활성화 시 phone eligibility binding·HTTPS endpoint·audience·credential provider를 요구해 fail-closed한다.
- 구현 내용: PUBLISHED는 30일 cleanupAt TTL과 scheduler로 정리하고 DEAD_LETTER는 90일 review 시각만 기록해 자동 삭제하지 않는다. metric tag는 eventType·schemaVersion·outcome·failureCode로 제한하고 userId·eventId·candidate·credential을 포함하지 않는다. raw phone·last4·Firebase UID·provider subject·PhoneIdentity fingerprint를 wire·로그·metric에 추가하지 않았다.
- 실행한 테스트와 결과: 중간에 `./gradlew compileJava`, `testClasses`와 publisher·signup·withdrawal·PhoneIdentity·Mongo custom fragment 대상 테스트를 실행했다. 최종 `./gradlew clean test`는 73개 suite·433개 테스트가 failure 0, error 0, skipped 0으로 성공했고 `git diff --check`도 성공했다. 실제 Atlas·Firebase·consumer endpoint는 호출하지 않았다.
- 유지한 계약: UUID userId와 JWT `sub`, Identity producer와 외부 Entitlement/Billing consumer·Learning Core 경계, PhoneIdentity와 eligibility candidate의 key/domain 분리, eventId 멱등성과 bindingRevision 역순 방어, Refresh Token 원문 비저장, Firebase·publisher 기본 비활성을 유지했다. TrialClaim·Entitlement·시험 코드는 추가하지 않았다.
- 결정사항: delivery credential 실제 발급은 환경별 `WorkloadIdentityCredentialProvider` 구현 책임으로 남기고 static Token·key 설정을 만들지 않았다. 2xx만 publish 성공이며 consumer 응답 본문은 저장하지 않는다. DEAD_LETTER는 자동 삭제하지 않고 명시적 replay가 같은 event를 재사용한다.
- 위험 요소: 실제 consumer·workload identity 발급 인프라·endpoint allowlist·운영 on-call은 아직 없다. 인메모리 Mongo 검증은 실제 replica set Transaction, TTL 지연, 동시 lease/index rollout을 완전히 증명하지 않으므로 staging E2E가 필요하다. publisher만 준비된 상태에서 production flag를 켜면 outbox가 누적될 수 있다.
- 다음 작업: 사용자가 diff를 검토해 직접 commit·push하고 PR을 병합한다. 병합 확인 전 Jira를 Done으로 바꾸지 않으며, 별도 Entitlement/Billing consumer의 eventId inbox·payload digest·revision high-water·binding Transaction Jira를 생성·구현한 뒤 staging E2E를 수행한다.
- 수행한 Jira 작업: 구현 전에 Atlassian 공식 MCP로 TMI-96 설명·완료 조건·상태를 읽기 전용 확인했다. Jira 생성·수정·댓글·상태 전환은 수행하지 않았다.
- 추가한 댓글의 목적: 종료 댓글 초안에는 구현 요약, 변경 파일, 73개 suite·433개 테스트 결과와 남은 staging/consumer 위험만 포함할 예정이며 자동 등록하지 않았다.
- 변경한 상태: TMI-96은 `해야 할 일`, Resolution 없음으로 유지했다.
- 승인 여부: 사용자가 `구현 해줘`라고 구현을 승인했다. Jira 댓글·상태 변경과 Git commit·push는 승인하지 않았고 수행하지 않았다.

## 2026-08-15 — 선택적 quality review 동의 API 설계 검토

<!-- codex-turn:01a00467-1fc9-7601-8493-c1e844d47a38 -->

- 날짜: 2026-08-15
- 브랜치: `feat/TMI-96-phone-eligibility-outbox-publisher` (`31130fd`, commit·push 미수행)
- 작업 목표: Guest 생성·동의 갱신·동의 상태 조회에 선택적 quality review 동의 필드를 추가하는 제안이 현재 필수 동의 모델과 철회 요구를 올바르게 표현하는지 검토한다.
- 변경 파일: 이번 분석 기록으로 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 수정했다. 진행 중인 TMI-96 애플리케이션 변경과 동의 코드는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 현재 `ConsentPolicyStatusResponse`에는 privacy·terms에도 이미 `consented` 필드가 있으며 factory는 미동의 또는 version 불일치를 모두 `requiresConsent=true`로 계산함을 확인했다. 선택 동의를 같은 factory에 그대로 넣으면 false인데 requiresConsent가 false여야 한다는 제안과 충돌하므로 optional 전용 factory/DTO 또는 명시적 `required` flag를 권장했다.
- 구현 내용: `isQualityReviewConsented=true`는 요청 version이 서버 current version과 정확히 일치할 때만 저장하고, `false` 철회는 stale app version 때문에 차단하지 않는 조건부 검증을 권장했다. false snapshot은 `consentedVersion`·`consentedAt`을 null로 만들되 최초 미동의와 철회를 구분하고 철회 시점을 증명해야 한다면 별도 `changedAt`·`withdrawnAt` 또는 append-only consent history가 필요하다고 정리했다.
- 구현 내용: 동일한 상태로 반복 PUT하면 저장 시각을 바꾸지 않는 멱등성, true→false 철회 시 Mongo partial update의 과거 version/time `$unset`, 기존 문서의 누락 필드를 false/null로 읽는 하위 호환, privacy·terms true 강제 유지와 quality review false 허용을 요구사항으로 제안했다.
- 구현 내용: quality review가 실제 시험 답안·음성의 사람 검토나 품질 개선 이용을 제어한다면 Identity의 현재 상태만 변경해서는 철회가 완료되지 않으며, 데이터를 소유한 외부 서비스에 versioned consent changed/revoked event를 전달해 이후 처리 중지와 승인된 보존·삭제를 수행해야 함을 위험으로 표시했다. Guest 외 LOCAL·Firebase 가입 경로와 PUT 성공 응답·profile 응답도 계약 범위를 결정해야 한다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: TMI-96 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 API·도메인 설계 검토를 요청했다. 동의 기능 구현, 외부 lifecycle event, Jira 변경과 Git commit·push는 이번 요청의 승인 범위에 포함되지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 설계 검토와 기록이므로 Gradle 테스트를 실행하지 않았다. 종료 전 `git diff --check`와 turn marker 단일 존재를 정적으로 검증한다.
- 유지한 계약: privacy·terms는 필수 true와 current version 일치를 계속 요구하고 quality review만 선택 동의로 분리한다. 사용자 요청 시각을 신뢰하지 않고 서버 Clock을 사용하며 동의 내용이나 개인정보를 로그에 추가하지 않는다. Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 제안한 API 방향은 유효하지만 단순 두 필드 추가만으로는 부족하다. 철회 비차단 version 규칙, optional `requiresConsent` 계산, 멱등 timestamp, 철회 감사와 downstream 이용 중지 계약을 먼저 명시해야 한다.
- 위험 요소: false 요청에도 current version 일치를 강제하면 오래된 앱에서 철회를 못 할 수 있다. 반대로 false에서 version/time을 모두 지우고 별도 이력을 남기지 않으면 철회 사실과 시각을 증명할 수 없으며, Identity만 갱신하면 외부 서비스가 기존 데이터를 계속 품질 검토에 사용할 수 있다.
- 다음 작업: 구현 전에 quality review 데이터의 실제 소유 서비스·철회 후 보존/삭제·기존 데이터 적용 범위와 audit 수준을 확정한다. 이후 request/response·UserConsents·ConsentPolicy·Mongo partial update·모든 signup 경로·OpenAPI와 회귀 테스트를 하나의 Jira 범위로 구현한다.

## 2026-08-15 — Quality review 프론트 계약과 긴급 브랜치 적용 순서 검토

<!-- codex-turn:01a0046b-99c3-7013-b1e2-97eebe0bc441 -->

- 날짜: 2026-08-15
- 브랜치: `feat/TMI-96-phone-eligibility-outbox-publisher` (`31130fd`, commit·push 미수행)
- 작업 목표: 프론트가 quality review 동의 변경을 어떻게 이해해야 하는지와 즉시 적용을 위해 main checkout 후 수정해도 되는지 현재 Git 상태 기준으로 판단한다.
- 변경 파일: 이번 분석 기록으로 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 수정했다. 진행 중인 TMI-96 애플리케이션 변경과 동의 코드는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 프론트는 Guest와 consent PUT에 optional boolean·policy version을 추가하고, false를 가입 차단 없이 허용하며, GET의 qualityReview currentVersion·consented·nullable consent metadata·optional action 상태를 표시하고 false PUT을 철회로 사용한다고 정리했다. PUT 성공 응답에도 저장된 quality review 상태를 반환할지 계약을 맞춰야 한다.
- 구현 내용: response에 qualityReview object를 추가하는 것은 일반적으로 additive지만 POST/PUT의 신규 `@NotNull` request 필드는 구버전 client 요청을 400으로 만들 수 있음을 확인했다. 안전한 즉시 rollout은 backend가 누락 필드를 false로 취급하는 호환 모드로 먼저 배포하고 frontend가 새 필드를 전송하도록 전환한 뒤, 구버전 종료 후 명시 입력 강제 여부를 결정하는 순서다.
- 구현 내용: 현재 `main` `b6eb73e`는 `develop` `31130fd`의 ancestor이고 develop보다 16개 commit 뒤에 있음을 read-only Git 명령으로 확인했다. 또한 현재 TMI-96 branch에 tracked·untracked 미커밋 변경이 많으므로 바로 main checkout하면 변경 반입·충돌 또는 기능 누락 위험이 있다.
- 구현 내용: 사용자가 현재 TMI-96 변경을 먼저 commit·push하고 작업 트리를 깨끗하게 만든 다음 `develop`을 최신화해 별도 quality-review feature/hotfix branch를 생성하는 순서를 권장했다. 실제 운영이 main 배포만 허용한다면 develop 변경을 정식 PR로 main에 반영한 후 배포해야 하며 main에서 직접 개발하지 않는다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 프론트 계약과 Git 작업 순서에 대한 설명을 요청했다. checkout·stash·commit·push·branch 생성·동의 구현은 이번 요청의 승인 범위에 포함되지 않았고 수행하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 계약·Git 상태 분석과 기록이므로 Gradle 테스트를 실행하지 않았다. `git merge-base --is-ancestor main develop` 성공과 `main...develop` 차이 0/16을 확인했으며 종료 전 `git diff --check`와 turn marker 단일 존재를 검증한다.
- 유지한 계약: privacy·terms 필수 동의와 quality review 선택 동의를 구분하고 false 철회를 허용한다. 기존 client를 갑자기 차단하지 않는 rollout과 feature branch·PR 흐름을 유지하며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 프론트에는 단순 필드 추가뿐 아니라 선택 동의·철회·nullable·버전 규칙을 함께 전달한다. Git 작업은 main 직접 수정이 아니라 현재 변경 보존 후 최신 develop 기반 별도 branch로 진행한다.
- 위험 요소: backend와 frontend를 동시에 강제 변경하면 모바일 구버전이 장기간 400을 받을 수 있다. main에서 직접 작업하면 develop의 16개 commit과 TMI-96 변경이 빠지거나 나중에 큰 merge conflict가 발생할 수 있다.
- 다음 작업: quality review 정책·downstream 철회 범위를 Jira로 확정하고, 사용자가 TMI-96 변경을 commit·push한 뒤 최신 develop 기반 새 branch에서 backward-compatible backend, frontend, downstream 순으로 배포한다.

## 2026-08-15 — Quality review 선택 동의 구현 계획서 작성

<!-- codex-turn:01a00479-f4dc-7aa2-80fd-5a91610c8278 -->

- 날짜: 2026-08-15
- 브랜치: `feat/TMI-96-phone-eligibility-outbox-publisher` (`31130fd`, commit·push 미수행)
- 작업 목표: 사용자가 처음 설계한 quality review 외부 필드명을 유지하면서 Identity API·도메인·저장·하위 호환·테스트·배포와 외부 철회 연동의 구현 계획서를 작성한다.
- 변경 파일: 신규 `docs/contracts/quality-review-consent-implementation-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 진행 중인 TMI-96 애플리케이션 변경과 실제 동의 코드는 수정하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 요청 필드 `isQualityReviewConsented`·`qualityReviewConsentVersion`과 GET `qualityReview.currentVersion`·`consented`·`consentedVersion`·`consentedAt`·`requiresConsent`를 변경하지 않는 계약으로 고정했다. Guest POST·consent PUT·GET 예시와 PUT 성공 응답 범위를 문서화했다.
- 구현 내용: privacy·terms는 기존 필수 true/current version 규칙을 유지하고 quality review는 선택 동의로 분리했다. true는 current version exact match, false는 stale version으로 차단하지 않으며 false 저장은 version·consentedAt null, 동일 상태 반복 요청은 시각과 User updatedAt을 바꾸지 않는 성공 no-op으로 결정했다.
- 구현 내용: 기존 client가 새 request 필드를 보내지 않아도 초기 backend가 false로 처리하는 하위 호환 배포, 기존 Mongo 문서 누락=false/null 해석, 모든 가입 경로의 묵시적 true 금지, server Clock·ACTIVE User·CAS 갱신과 OpenAPI·설정·README 수정 범위를 계획했다.
- 구현 내용: 현재 snapshot만으로 최초 미동의와 철회를 구분하지 못하는 audit 한계를 명시하고 필요 시 append-only history를 별도 설계하도록 했다. 답안·음성·시험 결과는 Identity 소유가 아니므로 versioned consent changed/revoked event와 외부 멱등 consumer·보존/삭제 계약 전 실제 품질 검토를 활성화하지 않는 gate를 포함했다.
- 구현 내용: 요청 검증, 상태 전이·멱등성, GET 유효 동의 계산, Mongo 저장·동시성, 보안·로그와 전체 `./gradlew clean test`를 완료 조건으로 정리했으며 Learning Core·음성 저장·운영 검토 도구·법적 보존 기간 결정은 제외 범위로 분리했다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 고정 필드명 기반 계획서 작성을 명시적으로 요청했다. 애플리케이션 구현, checkout·branch 생성, Jira 변경과 Git commit·push는 승인 범위에 포함되지 않았고 수행하지 않았다.
- 실행한 테스트와 결과: 문서 작성만 수행해 Gradle 테스트를 실행하지 않았다. 계획서의 코드·설정 경로를 현재 구현과 대조했고 종료 전 `git diff --check`, turn marker 단일 존재와 WORKLOG EOF append를 정적으로 검증한다.
- 유지한 계약: privacy·terms 필수 동의, quality review 선택 동의·철회, canonical userId와 서버 Clock, Identity와 Learning Core 데이터 소유 경계를 유지했다. 요청 본문·동의 상세값을 로그에 추가하지 않고 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 외부 필드명은 사용자의 최초 설계대로 유지한다. Quality review 미동의는 가입·일반 기능을 막지 않고 `requiresConsent=false`이며, 저장된 true가 current version과 일치할 때만 현재 유효 동의로 사용한다.
- 위험 요소: false snapshot만 저장하면 철회 이력 증명이 불가능하고, 외부 consumer 없이 Identity만 false로 바꾸면 실제 데이터 이용이 계속될 수 있다. 새 request 필드를 즉시 필수화하면 구버전 client가 400을 받을 수 있다.
- 다음 작업: 사용자가 계획서를 검토한 뒤 별도 Jira와 최신 develop 기반 branch를 준비한다. 구현 시 backend 호환 모드, frontend 필드 전송, audit·downstream 철회 계약 순으로 진행하고 전체 테스트·staging 검증 후 활성화한다.

## 2026-08-15 — Quality review main-only hotfix 계획으로 수정

<!-- codex-turn:01a00486-d2e1-7b50-b1a7-9ffc0701c69b -->

- 날짜: 2026-08-15
- 브랜치: `feat/TMI-96-phone-eligibility-outbox-publisher` (`31130fd`, checkout·commit·push 미수행)
- 작업 목표: develop을 main에 포함하지 않고 현재 요청한 quality review 동의 변경만 main에 반영하도록 구현 계획의 Git 기준·적용 코드·검증 범위를 수정한다.
- 변경 파일: `docs/contracts/quality-review-consent-implementation-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 진행 중인 TMI-96 애플리케이션 코드와 실제 동의 코드는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: read-only Git 조회로 `main` `b6eb73e`가 `develop` `31130fd`보다 16개 commit 뒤지만 Guest·LOCAL signup, `UserConsents`, `ConsentPolicy`, consent PUT·GET 기반은 이미 보유함을 확인했다. main에는 현재 develop의 Firebase signup·Phone eligibility publisher가 없으므로 이번 hotfix에 가져오지 않는다.
- 구현 내용: 현재 TMI-96 작업 트리의 tracked·untracked 변경을 보존하기 위해 checkout·stash하지 않고 `origin/main` 기준 별도 `hotfix/quality-review-consent` worktree를 만드는 절차로 변경했다. hotfix PR base는 main이며 develop merge를 금지하고 `origin/main...HEAD` diff로 quality review 관련 파일만 포함됐는지 확인한다.
- 구현 내용: main의 기존 refactor 전 package 경로에 맞춘 예상 수정 파일을 계획서에 기록했다. 지정 외부 endpoint는 Guest POST와 consent PUT·GET이며 main의 LOCAL signup은 이번 요청에 새 필드를 임의 추가하지 않고 quality review false/null/null로 초기화한 뒤 인증된 PUT에서 선택하도록 범위를 한정했다. LOCAL signup UI에도 즉시 노출해야 한다면 별도 요구 확인 후 포함한다.
- 구현 내용: TMI-95 ADR, TMI-96 publisher, Firebase signup foundation, auth vertical slice refactor와 다른 develop commit이 main hotfix diff에 포함되지 않아야 한다는 완료 조건을 추가했다. main 반영 후 develop 동기화가 필요하면 hotfix commit만 별도 cherry-pick 또는 develop 구조에 재구현하며 이번 main PR과 분리한다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 develop이 main에 들어가면 안 되고 quality review 수정만 main에 있어야 한다고 명시했다. Git checkout·worktree 생성·commit·push·PR과 애플리케이션 구현은 아직 요청·승인되지 않아 수행하지 않았다.
- 실행한 테스트와 결과: 계획 문서 수정과 read-only Git 비교만 수행해 Gradle 테스트를 실행하지 않았다. main의 관련 파일 존재와 package 경로를 `git ls-tree`·`git show`로 확인했으며 종료 전 `git diff --check`, marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: 외부 quality review 필드명, privacy·terms 필수와 quality review 선택·철회 규칙, 구 client 누락=false 호환을 유지했다. main-only hotfix 경계를 지키고 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 이번 변경의 source와 PR base는 main이다. develop은 main에 merge하지 않고 hotfix에도 섞지 않는다. 현재 dirty TMI-96 worktree와 main hotfix worktree를 물리적으로 분리한다.
- 위험 요소: 현재 작업 트리에서 main으로 checkout하면 TMI-96 변경이 따라가거나 충돌할 수 있다. main과 develop은 package 구조가 달라 develop 코드를 복사하면 불필요한 refactor와 기능이 함께 들어갈 수 있으며, 장기적으로 main hotfix를 develop에 별도 동기화하지 않으면 다음 병합에서 충돌할 수 있다.
- 다음 작업: 사용자가 구현을 요청하면 별도 main worktree 생성과 main 전용 코드 재검토부터 시작한다. 변경 후 main 전체 테스트와 `origin/main...HEAD` 파일 목록을 검증하고 quality review 변경만 포함한 PR 초안을 준비한다.

## 2026-08-15 — Main hotfix worktree branch 확인

<!-- codex-turn:01a0049d-c800-7f50-a34a-a8815e9f7d6e -->

- 날짜: 2026-08-15
- 브랜치: 기존 worktree `feat/TMI-96-phone-eligibility-outbox-publisher` (`31130fd`), 별도 worktree `hotfix/quality-review-consent` (`b6eb73e`)
- 작업 목표: 사용자가 별도 worktree를 생성한 뒤 추가 branch switch가 필요한지 실제 Git worktree·branch 연결 상태로 확인한다.
- 변경 파일: 이번 확인 기록으로 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 수정했다. 애플리케이션 코드와 두 worktree의 branch·HEAD는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: `git worktree list --porcelain`로 기존 `/Users/msde76/identity`가 TMI-96 branch `31130fd`, 새 `/Users/msde76/identity-quality-review-hotfix`가 `hotfix/quality-review-consent` branch `b6eb73e`에 각각 연결된 것을 확인했다. hotfix branch는 `origin/main`을 tracking하며 생성 순간 새 worktree에 이미 checkout됐다.
- 구현 내용: worktree마다 서로 다른 branch가 동시에 checkout되는 구조이므로 기존 폴더에서 `git switch`할 필요가 없다. 사용자는 `cd /Users/msde76/identity-quality-review-hotfix`로 이동한 뒤 `pwd`, `git branch --show-current`, `git status --short`를 확인하고 hotfix 작업을 진행하면 된다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 branch switch 필요 여부 확인을 요청했다. checkout·switch·commit·push·애플리케이션 구현은 수행하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 Git 상태 확인과 기록이므로 Gradle 테스트를 실행하지 않았다. worktree path·HEAD·branch와 tracking 상태를 read-only로 확인하고 종료 전 `git diff --check`, marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: main-only hotfix와 기존 develop/TMI-96 변경의 물리적 격리를 유지했다. develop을 main에 병합하지 않았고 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 추가 `git switch`는 필요 없다. hotfix 폴더 자체가 이미 hotfix branch 작업 트리이며 기존 폴더는 TMI-96 branch 보존용으로 유지한다.
- 위험 요소: 기존 `/Users/msde76/identity`에서 계속 수정하면 quality review 변경이 TMI-96 worktree에 섞일 수 있다. 터미널과 IDE의 열린 프로젝트 경로가 hotfix 폴더인지 확인해야 한다.
- 다음 작업: hotfix 폴더로 이동해 branch와 clean status를 확인한 뒤 main 패키지 구조 기준으로 quality review 변경만 구현한다. PR 전 `origin/main...HEAD` diff에 develop 변경이 없는지 검증한다.

## 2026-08-15 — Quality review hotfix 작업 위치 최종 확인

<!-- codex-turn:01a0049f-fb35-7401-9054-f36d1de5c84f -->

- 날짜: 2026-08-15
- 브랜치: 별도 worktree `hotfix/quality-review-consent` (`b6eb73e`, `origin/main` 기준)
- 작업 목표: 사용자가 새 hotfix worktree로 이동한 뒤 현재 branch와 clean working tree를 올바르게 확인했는지 검증한다.
- 변경 파일: 이번 확인 기록으로 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 수정했다. 애플리케이션 코드와 Git branch·HEAD·index·working tree는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 사용자가 `/Users/msde76/identity-quality-review-hotfix`에서 실행한 `git branch --show-current` 결과가 `hotfix/quality-review-consent`임을 확인했다. 이어진 `git status --short` 출력이 없으므로 hotfix worktree가 clean한 상태임을 확인했다.
- 구현 내용: 추가 branch switch는 필요하지 않으며 터미널과 IDE 모두 hotfix worktree 경로를 사용해야 기존 `/Users/msde76/identity`의 TMI-96 변경과 quality review main hotfix가 섞이지 않는다고 안내했다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 branch와 status 실행 결과를 공유해 확인을 요청했다. checkout·switch·commit·push·PR과 애플리케이션 구현은 수행하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 Git 상태 확인과 기록이므로 Gradle 테스트를 실행하지 않았다. 사용자가 제공한 branch·status 결과와 앞서 조회한 worktree 연결 상태가 일치하며 종료 전 `git diff --check`, marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: main-only hotfix와 develop/TMI-96 worktree 격리를 유지했다. develop을 main에 병합하지 않았으며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: hotfix 구현 준비 상태는 정상이다. 현재 worktree와 branch를 그대로 사용하고 추가 `git switch`는 하지 않는다.
- 위험 요소: IDE가 기존 `/Users/msde76/identity`를 계속 열고 있으면 잘못된 branch에 수정할 수 있으므로 프로젝트 root 표시를 확인해야 한다.
- 다음 작업: 사용자가 구현을 요청하면 hotfix worktree에서 main의 기존 consent 구조를 다시 읽고 quality review 관련 변경만 적용한 뒤 전체 테스트와 `origin/main...HEAD` diff를 검증한다.

## 2026-08-15 — Quality review hotfix 커밋 가능 상태 확인 동기화

<!-- codex-turn:01a004b3-4ee4-79a3-afe7-66ede4bae4b5 -->

- 날짜: 2026-08-15
- 브랜치: 별도 worktree `hotfix/quality-review-consent` (`b6eb73e`, `origin/main` 기준, commit·push 미수행)
- 작업 목표: Quality review 구현을 지금 커밋해도 되는지 hotfix worktree의 변경 범위·정적 검사·전체 테스트로 확인하고 기존 Identity worktree 기록에도 동기화한다.
- 변경 파일: 이번 Hook 대응으로 기존 worktree의 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. hotfix 애플리케이션 구현은 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: hotfix worktree의 변경 파일이 Guest·consent API, User consent 도메인·저장, quality review 설정·오류·OpenAPI·README·계획서와 관련 테스트에 한정되고 Firebase·TMI-95·TMI-96 같은 develop 전용 파일이 없음을 확인했다.
- 구현 내용: staged 파일은 아직 없으며 사용자가 전체 의도 변경을 stage한 뒤 cached diff를 확인하고 단일 hotfix commit을 생성하도록 안내했다. Codex는 저장소 규칙에 따라 stage·commit·push를 수행하지 않았다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 커밋 가능 여부를 질문했다. Git commit·push는 사용자가 직접 수행하도록 명령만 제공했다.
- 실행한 테스트와 결과: hotfix worktree에서 `./gradlew clean test`가 BUILD SUCCESSFUL로 끝났고 42개 suite·319개 테스트가 failure 0, error 0, skipped 0이었다. `git diff --check`도 성공했으며 실제 Atlas·OAuth Provider·외부 데이터 소유 서비스는 호출하지 않았다.
- 유지한 계약: main-only hotfix와 develop/TMI-96 격리, privacy·terms 필수와 quality review 선택·철회·구 client 호환을 유지했다. Identity 밖의 시험·답안·음성 코드를 추가하지 않았고 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 사용자가 staged diff를 마지막으로 확인한 뒤 커밋 가능한 상태다. PR base는 main이며 quality review 변경만 포함해야 한다.
- 위험 요소: 일부 파일만 stage하면 코드·테스트·설정·문서가 분리될 수 있다. production 배포 전 Quality review version 환경설정과 외부 데이터 철회 연동 부재를 확인해야 한다.
- 다음 작업: 사용자가 `git add -A`, cached diff 검증, commit·push를 수행하고 main 대상 PR에서 `origin/main...HEAD` 변경 목록을 다시 확인한다.

## 2026-08-15 — Quality review 배포 환경변수 확인

<!-- codex-turn:01a004b7-968c-7be0-b4ac-6f16bffdb94e -->

- 날짜: 2026-08-15
- 브랜치: 별도 worktree `hotfix/quality-review-consent`, commit `4745652`, 원격 `origin/hotfix/quality-review-consent`
- 작업 목표: Quality review 선택 동의 hotfix 배포에 새 환경변수가 필요한지 코드·설정·문서와 현재 Git 상태로 확인한다.
- 변경 파일: 이번 확인 기록으로 기존 worktree의 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. hotfix 코드·설정·commit은 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: hotfix의 `.env.example`에 `QUALITY_REVIEW_CONSENT_VERSION=quality-review-v1`, `application.yml`에 `app.consent.quality-review-version: ${QUALITY_REVIEW_CONSENT_VERSION}`, `ConsentPolicy`에 non-null·non-blank 기동 검증이 연결된 것을 확인했다. README도 이 값을 필수로 문서화한다.
- 구현 내용: 환경변수는 Secret이 아니라 서버가 현재 제공하는 Quality review 정책 version 식별자다. local·staging·production 배포 환경에 명시적으로 추가하고 frontend 요청의 `qualityReviewConsentVersion`과 정확히 일치시켜야 한다. 누락 또는 공백이면 fail-fast 기동 실패하며 test profile은 `application-test.yml`의 가짜 `quality-review-v1`을 사용한다.
- 구현 내용: hotfix worktree HEAD가 commit `4745652` `feat: add optional quality review consent`이고 원격 hotfix branch와 일치하며 working tree가 clean한 것을 read-only Git 조회로 확인했다. main보다 한 commit 앞이고 아직 main 병합 여부는 확인하지 않았다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 배포 환경변수 필요 여부를 질문했다. 환경 설정·배포·Git 변경은 수행하지 않고 필요한 이름과 값만 안내했다.
- 실행한 테스트와 결과: 이번에는 코드 변경이 없는 설정 확인이므로 Gradle 테스트를 다시 실행하지 않았다. 직전 hotfix 전체 결과는 42개 suite·319개 테스트 failure 0, error 0, skipped 0이며 이번 종료 전 기록 문서의 `git diff --check`, marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: 실제 정책 문구나 개인정보를 환경변수에 넣지 않고 version 식별자만 사용한다. Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았으며 main-only hotfix와 develop 격리를 유지했다.
- 결정사항: 배포 전 `QUALITY_REVIEW_CONSENT_VERSION` 추가는 필수다. 현재 version 값은 frontend 계약과 같은 `quality-review-v1`로 맞춘다.
- 위험 요소: 환경변수가 없으면 애플리케이션이 기동하지 않고, frontend와 server version이 다르면 `true` 동의 요청이 version mismatch로 거절된다. 정책 내용이 변경되면 기존 값을 재사용하지 말고 새 version을 배포 계약으로 함께 전환해야 한다.
- 다음 작업: main 대상 PR과 배포 설정에 `QUALITY_REVIEW_CONSENT_VERSION=quality-review-v1`을 반영하고, 배포 후 GET currentVersion과 Guest·PUT true/false 흐름을 staging에서 검증한다.

## 2026-08-15 — Quality review staging 도메인·환경변수 구분 확인

<!-- codex-turn:01a004be-d763-7163-95ac-0d4594cc5973 -->

- 날짜: 2026-08-15
- 브랜치: 별도 worktree `hotfix/quality-review-consent`, commit `4745652`, 원격 `origin/hotfix/quality-review-consent`
- 작업 목표: Staging frontend의 Identity·Learning API base URL을 Quality review 점검 때문에 다른 domain으로 바꿔야 하는지와 backend 배포 환경변수의 역할을 구분한다.
- 변경 파일: 이번 확인 기록으로 기존 worktree의 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. hotfix 애플리케이션 코드·설정·Git 상태는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: hotfix 코드·설정·문서를 검색해 Quality review 전용 base URL, endpoint 설정 또는 외부 HTTP adapter가 없음을 확인했다. 선택 동의는 기존 Identity의 Guest POST와 consent PUT·GET에서 저장·조회되므로 frontend는 기존 Identity staging base URL을 계속 사용한다.
- 구현 내용: `EXPO_PUBLIC_IDENTITY_API_BASE_URL`과 `EXPO_PUBLIC_LEARNING_API_BASE_URL`은 frontend가 각각 Identity·Learning staging 서버로 요청할 공개 base URL이다. `QUALITY_REVIEW_CONSENT_VERSION`은 Identity runtime의 현재 정책 version 문자열이며 URL이나 점검 서버 주소가 아니다.
- 구현 내용: Staging frontend는 기존 두 staging domain을 유지하고 `qualityReviewConsentVersion=quality-review-v1`을 Identity 요청 body에 보낸다. Identity staging 배포 설정에는 `QUALITY_REVIEW_CONSENT_VERSION=quality-review-v1`을 추가해 양쪽 값을 일치시킨다. Learning API base URL은 이번 hotfix로 변경하지 않는다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 staging domain과 배포 환경변수 사용법을 질문했다. frontend·backend 환경설정, 배포, Git과 외부 시스템을 변경하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 설정·호출 경계 확인이므로 Gradle 테스트를 다시 실행하지 않았다. 직전 hotfix 전체는 42개 suite·319개 테스트 성공 상태이며 종료 전 기록 문서의 `git diff --check`, marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: 인증·동의 API는 Identity domain, 학습 API는 Learning domain으로 분리하고 Quality review 동의 때문에 Learning 또는 별도 도메인으로 사용자 인증 요청을 보내지 않는다. Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 현재 staging에서는 별도 Quality review domain이 필요 없다. 기존 staging API base URL을 유지하고 Identity staging runtime에 정책 version 환경변수만 추가한다.
- 위험 요소: frontend version 문자열과 Identity 환경변수가 다르면 true 동의 요청이 version mismatch로 거절된다. 향후 실제 검토 데이터 consumer가 별도 서비스로 생겨도 그 내부 endpoint는 server-side 계약이며 frontend의 `EXPO_PUBLIC_*` 값으로 노출하지 않는다.
- 다음 작업: Staging Identity 환경에 `QUALITY_REVIEW_CONSENT_VERSION=quality-review-v1`을 추가하고 앱 요청 version을 일치시킨 뒤 기존 Identity staging domain에서 Guest·GET·PUT true/false 흐름을 점검한다.

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

## 2026-08-15 — Staging Identity 기동 실패 원인 분석

<!-- codex-turn:01a004c0-1bfd-7920-a9fb-9364945924ce -->

- 날짜: 2026-08-15
- 브랜치: Quality review main hotfix 배포 진단
- 작업 목표: 사용자가 공유한 staging Identity `Application run failed` 로그의 실제 원인과 복구 방법을 확인한다.
- 변경 파일: 이번 누락 Hook 기록으로 기존 worktree의 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션·배포 설정·Git 상태는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 긴 `UnsatisfiedDependencyException` chain에서 최하위 cause가 `PlaceholderResolutionException: Could not resolve placeholder 'QUALITY_REVIEW_CONSENT_VERSION'`임을 확인했다. `ConsentPolicy` 생성 실패가 UserFactory·SignupService·AuthController로 전파돼 전체 기동이 실패한 것이며 API domain이나 MongoDB 장애가 아니다.
- 구현 내용: Identity staging Task Definition의 Identity container에 비밀이 아닌 정책 version `QUALITY_REVIEW_CONSENT_VERSION=quality-review-v1`을 추가하고 새 task revision·service deployment를 수행해야 한다고 안내했다. frontend `qualityReviewConsentVersion`과 정확히 일치해야 한다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 오류 원인 진단을 요청했다. ECS·환경변수·Git·외부 시스템을 변경하지 않았다.
- 실행한 테스트와 결과: 로그 분석만 수행해 Gradle 테스트를 실행하지 않았다. 최하위 exception과 hotfix 설정 연결을 대조했으며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 유지한 계약: Quality review version 누락은 fail-fast하고 별도 review domain을 만들지 않는다. staging frontend는 기존 Identity domain을 유지한다.
- 결정사항: 장애 원인은 필수 backend runtime 환경변수 누락이다.
- 위험 요소: 변수 추가 후에도 ECS service가 이전 task revision을 사용하면 같은 오류가 반복된다. 다른 container나 frontend에만 설정해도 Identity 기동 문제는 해결되지 않는다.
- 다음 작업: Identity staging의 새 task revision에 version을 추가하고 새 task 로그·health와 Guest·GET·PUT 요청을 확인한다.

## 2026-08-18 — Main hotfix 이후 develop 병합 전략 분석

<!-- codex-turn:01a012db-dd0f-7572-a959-a289a662c2db -->

- 날짜: 2026-08-18
- 브랜치: 로컬 `feat/TMI-96-phone-eligibility-outbox-publisher` (`feaf095`), 원격 비교 `main=3894627`, `develop=6f02f4a`
- 작업 목표: Quality review hotfix를 main에 먼저 반영한 뒤 develop→main merge가 충돌하는 이유와 향후 안전한 동기화 절차를 설명한다.
- 변경 파일: 이번 분석 기록으로 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. merge·checkout·애플리케이션 코드·Jira는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 원격 `git ls-remote`와 fetch로 main `3894627`이 hotfix merge PR #24와 commit `4745652`를 포함하고, develop `6f02f4a`가 TMI-96 merge PR #25를 포함함을 확인했다. 공통 merge base는 hotfix 전 `b6eb73e`이며 main-only 2개, develop-only 18개 commit으로 양쪽이 분기됐다.
- 구현 내용: read-only `git merge-tree` 시뮬레이션에서 `.env.example`, README, consent domain·factory·DTO·tests와 기록 문서처럼 양쪽이 수정한 파일에서 충돌이 재현됐다. 이는 이전 main-only hotfix 때문에 생긴 정상적인 branch divergence와 develop의 package refactor·Firebase 확장이 겹친 결과다.
- 구현 내용: Quality review 기능을 develop에도 반영하는 것이 맞지만 hotfix commit만 cherry-pick하면 내용은 복제돼도 main ancestry가 develop에 들어오지 않아 향후 merge 문제를 확실히 없애지 못한다고 정리했다. 최신 develop 기반 sync branch에서 `origin/main`을 `--no-ff` merge하고 develop 구조를 기준으로 Quality review 동작·설정·테스트를 수동 통합하는 방식을 권장했다.
- 구현 내용: sync PR은 develop을 대상으로 하고 squash/rebase가 아니라 merge commit으로 반영해 main commit이 develop ancestry에 포함되게 해야 한다. 그 뒤 `git merge-base --is-ancestor origin/main origin/develop`을 확인하면 나중에 준비된 시점의 develop→main release merge에서 이번 hotfix를 다시 충돌 원인으로 다루지 않는다. 현재 당장 develop을 main에 merge할 필요는 없다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 향후 병합 방법에 대한 분석을 요청했다. 실제 merge·conflict resolution·branch 생성·commit·push는 승인 범위에 포함되지 않아 수행하지 않았다.
- 실행한 테스트와 결과: 코드 변경 없이 원격 ref·commit graph·merge base·left/right count와 merge-tree를 확인했다. Gradle 테스트는 실행하지 않았고 종료 전 `git diff --check`, 두 turn marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: 지금 develop 전체를 main에 반영하지 않고 Quality review hotfix만 main에 유지한다. 추후 동기화에서도 develop의 Firebase·TMI-96 기능을 main에 조기 배포하지 않으며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 문제는 hotfix 자체가 잘못된 것이 아니라 main과 develop이 각각 다른 변경을 가진 분기 상태라서 발생한다. 다음 release 전 main→develop 동기화 merge를 한 번 수행하고 develop 구조에 Quality review를 통합한다.
- 위험 요소: conflict에서 main 파일 전체를 선택하면 develop의 refactor·Firebase·TMI-96 기능을 잃고, develop 파일 전체를 선택하면 Quality review hotfix가 사라진다. sync PR을 squash하면 ancestry 연결 목적을 달성하지 못할 수 있다.
- 다음 작업: 당장 release merge는 하지 않는다. 준비된 시점에 최신 develop 기반 `sync/main-quality-review-hotfix` branch를 만들어 main을 merge하고 각 conflict를 의미 단위로 해결한 뒤 전체 테스트·main ancestry·diff를 검증해 develop 대상 merge-commit PR로 반영한다.

## 2026-08-18 — Main Quality review hotfix를 develop에 선제 동기화

<!-- codex-turn:01a012df-464a-7833-9cd8-94a42975b582 -->

- 날짜: 2026-08-18
- 브랜치: `codex/sync-main-quality-review-hotfix` (`origin/develop` 기반, `origin/main` merge 진행 중, commit·push 미수행)
- 작업 목표: main에 먼저 반영된 Quality review 선택 동의 hotfix를 develop의 Firebase·TMI-96 구조와 함께 유지하도록 미리 역병합하고 향후 develop→main release merge의 분기와 충돌을 줄인다.
- 변경 파일: main hotfix의 환경변수 예시·README·Quality review 계획서, Guest request·service·controller, User consent policy·factory·entity·service·DTO·controller·오류 코드, application 설정과 관련 테스트를 develop에 통합했다. develop 전용 Firebase test fixture와 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 추가 갱신했으며 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 최신 `origin/develop`에서 별도 동기화 브랜치를 만들고 `git merge --no-ff --no-commit origin/main`을 수행했다. CURRENT_STATE·WORKLOG·계획서·UserConsentService·UserFactoryTests 충돌을 의미 단위로 해결해 develop의 Firebase 회원 생성과 accountType 로그, main의 Quality review Guest 생성·선택·철회·조회 계약과 테스트를 모두 보존했다.
- 구현 내용: 자동 병합 뒤 develop 전용 `FirebaseSignupServiceTests`가 3개 인자의 `ConsentPolicy`를 생성하도록 Quality review test version을 추가했다. 동의 성공 로그는 userId·accountType·provider·outcome을 유지하되 동의 시각은 남기지 않아 기존 로그 비노출 테스트와 main hotfix의 민감정보 방어 결정을 함께 유지했다.
- 구현 내용: main에만 존재하던 Quality review 구현 WORKLOG 기록 중 develop에 없던 항목을 파일 끝에 보존하고, 직전 staging 기동 실패·branch divergence 분석 기록도 임시 보관본에서 복원했다. 필수 외부 필드명 `isQualityReviewConsented`, `qualityReviewConsentVersion`과 runtime 설정 `QUALITY_REVIEW_CONSENT_VERSION`을 변경하지 않았다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자가 main hotfix를 develop에 지금 미리 통합하도록 승인했다. 저장소 규칙에 따라 Codex는 merge commit·push를 수행하지 않고 충돌 해결과 검증된 staged 상태까지만 준비했다.
- 실행한 테스트와 결과: 첫 `./gradlew clean test`는 Gradle 사용자 cache lock의 sandbox 접근 제한으로 시작 전 실패해 승인된 동일 명령으로 재실행했다. 첫 재실행은 Firebase 전용 test fixture의 이전 생성자 호출로 compile 실패했고 수정 후 두 번째는 동의 시각 로그 비노출 assertion 1건이 실패했다. 로그에서 동의 시각을 제거한 최종 실행은 BUILD SUCCESSFUL이며 전체 457개 테스트가 통과했다.
- 유지한 계약: UUID userId, JWT sub·RS256·kid·issuer·audience, RefreshSession, Firebase broker와 TMI-96 outbox publisher를 유지했다. Identity 밖의 시험·답안·음성 코드를 추가하지 않았고 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 지금 main→develop을 동기화하는 것이 이후 충돌 확대를 줄이므로 별도 branch에서 선제 수행했다. 사용자가 현재 merge를 일반 merge commit으로 완료하고 develop 대상 PR도 squash/rebase가 아닌 merge commit 방식으로 병합해야 main ancestry가 develop에 보존된다.
- 위험 요소: 아직 merge commit이 없어 `origin/main`은 현재 HEAD의 ancestor가 아니다. commit 전에 일부 staged 파일을 제외하면 계약·설정·테스트가 분리될 수 있고, develop PR을 squash하면 ancestry 연결 목적을 달성하지 못한다. 배포 환경에는 `QUALITY_REVIEW_CONSENT_VERSION=quality-review-v1`이 계속 필요하며 외부 품질 검토 데이터의 철회 lifecycle은 별도 계약이 필요하다.
- 다음 작업: 사용자가 staged diff를 검토해 현재 merge commit을 생성하고 branch를 push한 뒤 develop 대상 PR을 Create a merge commit 방식으로 병합한다. 병합 후 `git merge-base --is-ancestor origin/main origin/develop`의 종료 코드 0과 staging Guest·GET·PUT 선택·철회 흐름을 확인한다.

## 2026-08-14 — TMI-95 PR 병합 확인·Jira 종료 변경안 및 다음 개발 범위

<!-- codex-turn:019fff2d-3d4c-7060-9f6b-c5188635c04e -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`31130fd`, GitHub PR #23 merge commit, Codex commit·push 미수행)
- Jira: TMI-95
- 작업 목표: TMI-95 ADR PR 병합과 Jira 완료 가능 여부를 확인하고, 실제 Jira 변경 전에 종료 댓글·상태 전환안을 공개하며 다음 Identity·consumer 개발 범위를 분리한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션·ADR·테스트는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 로컬 `develop`과 `origin/develop`이 PR #23 merge commit `31130fd`를 가리키고 TMI-95 feature commit `cfefbec`이 이력에 포함된 것을 확인했다. GitHub CLI 원격 조회는 sandbox network 제한으로 실패했지만 fetch된 origin ref와 merge commit 제목·부모 이력으로 병합을 확인했다.
- 수행한 Jira 작업: Atlassian 공식 MCP로 TMI-95의 제목, 댓글 없음, 상태 `해야 할 일`, Resolution 없음과 사용할 수 있는 완료 transition ID `41`을 읽기 전용 조회했다. Jira 댓글·상태·Resolution·필드는 변경하지 않았다.
- 추가한 댓글의 목적: ADR-002의 versioned verified/revoked schema, eventId·revision 처리, scope·key·transport·보존 결정, 전체 테스트 결과와 publisher·consumer 미구현 위험을 인수인계하는 종료 댓글 초안을 사용자에게 공개한다.
- 변경한 상태: TMI-95는 `해야 할 일`, Resolution 없음 상태를 유지한다. 실제 완료 전환은 공개된 변경안의 사용자 승인을 받은 뒤에만 수행한다.
- 승인 여부: 사용자가 Jira 종료를 요청했지만 저장소 규칙상 실제 변경 전에 댓글과 transition 내용을 먼저 공개하고 별도 승인을 받아야 하므로 이번 조회 turn에서는 변경하지 않았다.
- 실행한 테스트와 결과: 병합·Jira 읽기 확인, 다음 범위 정리와 문서 기록만 수행해 Gradle 테스트를 재실행하지 않았다. PR #23에 포함된 최종 검증은 ADR JSON 예시 2개 파싱 성공과 `./gradlew clean test` 70개 suite·421개 테스트 failure 0, error 0, skipped 0이다.
- 유지한 계약: canonical UUID userId/JWT `sub`, Identity·Entitlement/Billing·Learning Core 도메인 경계, Firebase/provider 기본 비활성과 가입 event·혜택 지급 분리를 유지했다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 Jira 변경안이나 기록에 포함하지 않았다.
- 결정사항: 다음 Identity 저장소 작업은 outbox schema/revision·revoke와 atomic lease·retry/dead-letter HTTPS publisher다. 외부 Entitlement/Billing consumer는 별도 저장소·Jira에서 inbox·current binding·revision high-water와 abuse ledger를 구현하고, 두 트랙의 staging contract test가 끝나기 전 production Firebase signup을 활성화하지 않는다.
- 위험 요소: Identity publisher, withdrawal/phone lifecycle revoke와 실제 workload identity adapter가 아직 없다. Entitlement/Billing 서비스·datastore·on-call, abuse ledger 보존 기간과 key reference count도 미구현이므로 ADR 완료가 production ready를 뜻하지 않는다.
- 다음 작업: 사용자가 종료 댓글과 완료 transition을 승인하면 Atlassian 공식 MCP로 댓글을 등록하고 transition ID `41`만 적용한 뒤 결과를 재조회한다. 이후 Identity publisher 작업 Jira 생성안을 먼저 제시한다.

## 2026-08-14 — TMI-95 Jira 종료 댓글 등록·완료 전환

<!-- codex-turn:019fff30-11a4-7da1-a051-d18a36cd8cd3 -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`31130fd`, GitHub PR #23 merge commit, Codex commit·push 미수행)
- Jira: TMI-95
- 작업 목표: PR #23 병합이 확인된 TMI-95에 승인된 종료 댓글을 등록하고 완료 상태로 전환한 뒤 결과를 검증한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션·ADR·테스트는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 직전 turn에서 PR #23 merge commit `31130fd`, TMI-95의 `해야 할 일` 상태·Resolution 없음·빈 댓글과 완료 transition ID `41`을 확인하고, 적용할 종료 댓글과 상태 변경을 사용자에게 공개했다.
- 수행한 Jira 작업: 사용자 승인에 따라 TMI-95에 ADR-002의 versioned verified/revoked schema, eventId·bindingRevision 처리, scope·key·transport·보존 결정, 변경 문서, 테스트 결과와 후속 위험을 요약한 댓글 ID `10006`을 등록했다. 이어서 transition ID `41`만 적용하고 후속 조회에서 상태 ID `10003` `완료`와 Resolution `완료`를 확인했다.
- 추가한 댓글의 목적: 구현·검증 결과와 Identity publisher·revoke lifecycle, 외부 Entitlement/Billing consumer, workload identity 및 staging E2E가 후속 범위임을 인수인계하기 위해 등록했다.
- 변경한 상태: Jira TMI-95를 `해야 할 일`에서 `완료`로 전환했고 Resolution도 `완료`가 됐다. 다른 Jira·필드는 변경하지 않았다.
- 승인 여부: 직전 응답에서 댓글 전문과 transition ID `41`, 변경하지 않을 필드를 공개했고 사용자가 `어 닫아줘`라고 명시적으로 승인했다.
- 실행한 테스트와 결과: Jira 댓글·상태 전환과 기록 문서 변경만 수행해 Gradle 테스트를 재실행하지 않았다. PR #23의 최종 검증은 ADR JSON 예시 2개 파싱 성공과 `./gradlew clean test` 70개 suite·421개 테스트 failure 0, error 0, skipped 0이다.
- 유지한 계약: canonical UUID userId/JWT `sub`, Identity·Entitlement/Billing·Learning Core 도메인 경계, Firebase/provider 기본 비활성과 가입 event·혜택 지급 분리를 유지했다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 Jira 댓글이나 작업 기록에 포함하지 않았다.
- 결정사항: TMI-95 ADR 작업은 완료됐지만 production 기능은 아직 활성화할 수 없다. 다음 Identity 범위는 outbox schema/revision·revoke와 atomic lease·retry/dead-letter HTTPS publisher이며 외부 consumer는 별도 저장소·Jira로 유지한다.
- 위험 요소: Identity publisher와 withdrawal/phone lifecycle revoke, 실제 workload identity adapter, Entitlement/Billing consumer·datastore·on-call, abuse ledger 보존 기간과 staging E2E가 아직 남아 있다.
- 다음 작업: Identity publisher 구현 Jira 생성안을 제목·설명·완료 조건·제외 범위로 먼저 제시하고 승인 뒤 생성한다. publisher와 consumer contract test가 staging에서 통과하기 전 production Firebase signup flag를 활성화하지 않는다.

## 2026-08-14 — Stage 5D Phone eligibility binding outbox publisher Jira 생성 초안

<!-- codex-turn:019fff35-8654-7af1-a97c-74bf817f22c3 -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`31130fd`, commit·push 미수행)
- 작업 목표: 완료된 TMI-95 ADR의 다음 Identity 구현인 outbox schema/revision·revoke와 atomic lease·retry/dead-letter HTTPS publisher를 추적할 Jira Payload를 준비한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션·ADR·테스트는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 다음 Jira를 `[Identity] Stage 5D Phone eligibility binding outbox publisher`, TMI `작업`, High, 기본 `해야 할 일`로 제안했다. schema v1 immutable event, user+scope atomic revision, verified/revoked lifecycle, lease claim, retry/backoff·dead-letter, HTTPS delivery/credential port, retention cleanup과 외부 인프라 없는 테스트를 범위로 정리했다.
- 구현 내용: 외부 Entitlement/Billing consumer의 inbox·binding·claim ledger, TrialClaim·Entitlement·시험 코드, cloud 인프라 provisioning, production Firebase 활성화, legacy password route 종료와 Git commit·push는 제외 범위로 분리했다.
- 수행한 Jira 작업: Atlassian 공식 MCP 검색으로 TMI 프로젝트에 PhoneEligibilityBindingOutbox publisher·bindingRevision·lease/retry/dead-letter 관련 중복 이슈가 없음을 읽기 전용 확인했다. Jira 생성·수정·댓글·상태 전환은 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: 기존 Jira 상태나 Resolution을 변경하지 않았고 신규 Jira도 아직 생성하지 않았다.
- 승인 여부: 사용자가 다음 작업 Jira 생성을 요청했지만 저장소 규칙상 실제 생성 전에 전체 Payload를 공개하고 별도 승인을 받아야 하므로 승인을 기다린다.
- 실행한 테스트와 결과: Jira 중복 조회·Payload 작성과 기록 문서 변경만 수행해 Gradle 테스트를 실행하지 않았다. 현재 병합 기준선은 `./gradlew clean test` 70개 suite·421개 테스트 성공 상태이며 종료 전 `git diff --check`와 turn marker 단일 존재를 확인한다.
- 유지한 계약: canonical UUID userId/JWT `sub`, Identity producer와 별도 Entitlement/Billing consumer 경계, Firebase/provider 기본 비활성과 event 수신·혜택 지급 분리를 유지했다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 Jira 초안이나 기록에 포함하지 않았다.
- 결정사항: 다음 Identity Jira는 publisher 구현까지 소유하되 consumer와 cloud 인프라 생성은 소유하지 않는다. workload identity는 static credential을 설정에 저장하지 않는 provider port로 격리하고 기능은 기본 비활성·설정 누락 fail-closed로 유지한다.
- 위험 요소: 실제 consumer endpoint·workload identity 발급 인프라와 운영 on-call이 아직 없으므로 Identity publisher 코드만 완료돼도 production ready가 아니다. withdrawal/phone lifecycle Transaction에 revoke event를 연결할 때 기존 User·Session rollback 계약을 깨지 않도록 별도 동시성 테스트가 필요하다.
- 다음 작업: 사용자에게 Jira 제목·설명·완료 조건·제외 범위와 미설정 필드를 공개한다. 승인하면 동일 Payload로 생성하고 발급된 Jira 키·상태·우선순위를 재조회해 기록한다.

## 2026-08-14 — TMI-96 Stage 5D Phone eligibility binding outbox publisher Jira 생성

<!-- codex-turn:019fff37-f0be-7be0-936e-727f407d501e -->

- 날짜: 2026-08-14
- 브랜치: `develop` (`31130fd`, commit·push 미수행)
- Jira: TMI-96
- 작업 목표: 사용자에게 공개한 Stage 5D Identity outbox publisher Jira Payload를 승인 내용 그대로 생성하고 저장 결과를 검증한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션·ADR·테스트는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: TMI-96 설명에 ADR-002 schema v1 verified/revoked event, user+scope atomic bindingRevision, lifecycle Transaction, PENDING·IN_FLIGHT·PUBLISHED·DEAD_LETTER 상태 머신, atomic lease, retry/backoff·실패 분류, HTTPS delivery·credential port, 보존 cleanup·안전한 metric과 외부 인프라 없는 테스트를 기록했다.
- 구현 내용: 외부 Entitlement/Billing consumer, TrialClaim·Entitlement·시험 코드, cloud workload identity 인프라 provisioning, production Firebase 활성화, legacy password API 종료, 공개 수동 replay API와 Git commit·push는 제외 범위로 유지했다.
- 수행한 Jira 작업: 사용자 승인에 따라 Atlassian 공식 MCP로 TMI `작업` 이슈 `TMI-96`을 생성했다. 생성 직후 읽기 전용 재조회해 승인한 제목·설명, 유형 `작업`, 우선순위 `High`, 상태 `해야 할 일`, Resolution 없음, 담당자 없음, 빈 라벨·컴포넌트를 확인했다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: 생성 시 기본 상태 `해야 할 일`을 유지하고 별도 transition을 적용하지 않았다. 기존 Jira의 상태나 Resolution은 변경하지 않았다.
- 승인 여부: 직전 응답에서 프로젝트·유형·제목·우선순위·설명·완료 조건·제외 범위와 미설정 필드를 공개했고 사용자가 `어 생성해줘`라고 명시적으로 승인했다.
- 실행한 테스트와 결과: Jira 생성·검증과 기록 문서 변경만 수행해 Gradle 테스트를 실행하지 않았다. 현재 병합 기준선은 `./gradlew clean test` 70개 suite·421개 테스트 성공 상태이며 종료 전 `git diff --check`를 실행한다.
- 유지한 계약: canonical UUID userId/JWT `sub`, Identity producer와 별도 Entitlement/Billing consumer 경계, Firebase/provider 기본 비활성과 event 수신·혜택 지급 분리를 유지했다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 Jira나 작업 기록에 포함하지 않았다.
- 결정사항: TMI-96은 Identity publisher 코드까지 소유하지만 외부 consumer와 cloud 인프라 생성을 소유하지 않는다. static credential 설정을 금지하고 workload identity credential provider를 격리하며 production gate는 계속 닫아 둔다.
- 위험 요소: 실제 consumer endpoint·workload identity 발급 인프라·운영 on-call이 없으므로 TMI-96만 완료돼도 production ready가 아니다. withdrawal/phone lifecycle revoke와 기존 User·Session rollback 경계의 동시성 검증이 필요하다.
- 다음 작업: 구현 전에 Atlassian 공식 MCP로 TMI-96 설명과 완료 조건을 다시 읽고 ADR-002와 현재 outbox를 대조한다. schema/revision·lifecycle Transaction을 먼저 구현한 뒤 lease publisher·delivery·cleanup 순으로 검증한다.

## 2026-08-14 — Identity와 외부 Entitlement 선행 개발 관계 확인

<!-- codex-turn:019fff46-5b57-7391-900a-1c773aa6b562 -->

- 날짜: 2026-08-14
- 브랜치: `feat/TMI-96-phone-eligibility-outbox-publisher` (`31130fd`, commit·push 미수행)
- Jira: TMI-96
- 작업 목표: 현재 Identity 수정이 별도 저장소의 Entitlement/Billing을 전제로 진행되는 것인지와 실제 구현 준비 상태를 구분해 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 이번 설명을 위해 갱신했다. 동시에 진행 중인 TMI-96 애플리케이션 변경에는 손대지 않았고 WORKLOG의 다른 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 목표 구조에서는 Entitlement/Billing consumer가 별도 저장소에서 구현된다고 가정하고 Identity가 producer 측 verified phone candidate·outbox와 전달 기능을 먼저 준비한다고 정리했다. 그러나 외부 consumer가 이미 구현돼 있거나 현재 end-to-end 전달이 동작한다는 가정은 하지 않는다.
- 구현 내용: TMI-95에서 서버 간 ADR을 확정했고 현재 TMI-96 범위로 Identity publisher 구현을 진행한다. 외부 consumer·inbox와 TrialClaim·UserEntitlement 처리는 별도 저장소 책임이며, 양쪽 구현과 staging 검증 전에는 production eligibility 연동을 활성화하지 않는다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: TMI-96 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 현재 개발 전제에 대한 설명을 요청했다. 외부 저장소 변경·Jira 변경·Git commit·push는 승인 범위에 포함되지 않았다.
- 실행한 테스트와 결과: 이번 설명과 기록을 위한 별도 Gradle 테스트는 실행하지 않았다. 현재 TMI-96 구현 변경은 완료 전이므로 그 검증 결과를 이번 분석 결과로 주장하지 않으며 종료 전 문서 정적 검증만 수행한다.
- 유지한 계약: Identity는 verified phone과 generic eligibility outbox·publisher만 소유하며 TrialClaim·UserEntitlement·시험 코드를 포함하지 않는다. 외부 consumer가 준비되지 않은 상태에서 중복 혜택 방지를 우회하지 않고 fail-closed하며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 별도 Entitlement/Billing은 목표 아키텍처상의 downstream이며 현재 존재·완료한 시스템으로 간주하지 않는다. Identity와 consumer는 확정된 계약을 기준으로 독립 개발하되 production 활성화 전에 양쪽 구현과 end-to-end 전달을 검증한다.
- 위험 요소: Identity producer만 배포하고 consumer 없이 가입 연동을 활성화하면 outbox가 누적되고 사용자의 첫 무료시험 eligibility가 계속 processing 상태가 될 수 있다.
- 다음 작업: TMI-96에서 Identity publisher를 구현하고 별도 저장소 consumer를 후속 작업으로 구현한 뒤 staging end-to-end 전달과 fail-closed 동작을 검증한다.

## 2026-08-14 — Identity 현재·예정 entity 연결 구조 설명

<!-- codex-turn:019fff4a-078b-7a30-8239-b16eda760d5e -->

- 날짜: 2026-08-14
- 브랜치: `feat/TMI-96-phone-eligibility-outbox-publisher` (`31130fd`, commit·push 미수행)
- Jira: TMI-96
- 작업 목표: Identity에 현재 존재하는 Mongo entity와 앞으로 Identity 또는 외부 Entitlement/Billing에 생길 예정인 entity의 책임과 전체 연결 흐름을 쉽게 구분해 설명한다.
- 변경 파일: 이번 분석 기록으로 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 수정했다. 동시에 진행 중인 TMI-96 애플리케이션 변경에는 손대지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 현재 Identity Mongo document를 계정 root `User`, 인증수단 mapping `FirebaseIdentity`·`SocialIdentity`·`PhoneIdentity`·`PhoneFingerprintAlias`, 인증 세션 `RefreshSession`, 가입 전 임시 절차 `FirebaseEnrollmentAttempt`, 외부 전달 `PhoneEligibilityBindingOutbox`로 분류했다. embedded `UserConsents`와 비영속 결과인 `PreparedRefreshSession`·`IssuedRefreshSession`·`IssuedAccessToken`도 entity와 구별했다.
- 구현 내용: TMI-96 작업 트리에 추가 중인 `PhoneEligibilityBindingRevision`은 user+scope별 event 순번과 현재 활성 여부, `PhoneEligibilityBindingDeliveryScopeState`는 contract·인증 오류가 난 consumer scope의 전송 중지를 기억하는 producer 운영 document라고 정리했다. 두 모델은 현재 구현 진행 중이므로 완료된 기능으로 단정하지 않는다.
- 구현 내용: 이후 Identity 계획 후보인 Guest merge `UserMergedOutbox`, 탈퇴·Firebase cleanup lifecycle outbox/saga 상태는 아직 확정·완료 entity가 아님을 표시했다. 별도 Entitlement/Billing의 inbox·revision high-water, `VerifiedPhoneBenefitBinding`, `TrialClaim`, `UserEntitlement`, `EntitlementReservation`과 Learning Core 시험 entity는 Identity에 추가하지 않는 경계를 유지했다.
- 구현 내용: Firebase proof 검증과 `FirebaseEnrollmentAttempt(PENDING)`에서 시작해 가입 Transaction이 User·각 identity·RefreshSession·eligibility outbox를 저장하고 attempt를 CONSUMED로 바꾼 뒤, publisher와 Entitlement inbox/binding, 첫 무료시험의 TrialClaim·UserEntitlement, Learning Core의 reserve·exam 생성·confirm으로 이어지는 전체 생명주기를 연결했다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: TMI-96 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 현재·예정 entity의 개념 설명을 요청했다. 애플리케이션 구현·외부 저장소 변경·Jira 변경·Git commit·push는 이번 요청의 승인 범위에 포함되지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석과 기록이므로 이번 요청을 위한 Gradle 테스트는 실행하지 않았다. TMI-96 작업 트리의 구현은 진행 중이므로 완료나 테스트 성공을 이번 설명의 결과로 주장하지 않았고 종료 전 문서 정적 검증만 수행한다.
- 유지한 계약: canonical UUID userId/JWT `sub`, Firebase credential broker와 Identity account owner, PhoneIdentity fingerprint와 benefit candidate의 key/domain 분리, Identity·Entitlement/Billing·Learning Core 소유 경계를 유지했다. Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: entity를 장기 계정, 인증수단 mapping, 세션, 단기 가입 절차, 전달/운영, 외부 혜택 ledger로 구분해 이해한다. event outbox는 혜택 자체가 아니며 Entitlement의 binding도 혜택 지급 자체가 아니다.
- 위험 요소: TMI-96의 revision·delivery scope document와 publisher 상태 머신은 현재 작업 중이므로 최종 코드·테스트에 따라 세부 필드가 달라질 수 있다. lifecycle outbox/saga 이름과 구조도 후속 ADR 전에는 확정 모델로 취급하면 안 된다.
- 다음 작업: TMI-96 완료 후 실제 확정된 producer entity와 상태 전이를 다시 대조하고, 별도 Entitlement/Billing 저장소 작업에서는 inbox·binding·claim·entitlement·reservation 모델과 unique/Transaction 계약을 별도 문서로 작성한다.

## 2026-08-14 — Phone eligibility 이벤트 용어집 설명

<!-- codex-turn:019fff68-7d26-71d3-b8c4-5da4fb2e0335 -->

- 날짜: 2026-08-14
- 브랜치: `feat/TMI-96-phone-eligibility-outbox-publisher` (`31130fd`, commit·push 미수행)
- Jira: TMI-96
- 작업 목표: `fingerprintCandidates`처럼 entity 설명만으로 이해하기 어려운 Phone eligibility의 보안·이벤트·전달 용어를 쉬운 정의와 서로의 관계로 정리한다.
- 변경 파일: 이번 분석 기록으로 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 수정했다. 동시에 진행 중인 TMI-96 애플리케이션 변경에는 손대지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: raw phone, E.164 정규화, fingerprint, HMAC-SHA-256, domain separator, eligibility candidate, `fingerprintCandidates`, key ring·`keyVersion`·rotation을 한 흐름으로 설명했다. candidate 한 개는 한 key version으로 만든 비교값이며 candidates 배열은 rotation 중 retained version 전체를 지원하는 현재 binding의 완전한 집합이지 여러 전화번호 목록이 아님을 명확히 했다.
- 구현 내용: `consumerScopeId`, opaque, binding, verified/revoked, `bindingRevision`, state event를 목적·상태 용어로 구분했다. 같은 phone도 scope·domain이 다르면 다른 값이 나오고 bindingRevision은 event schema나 key 판본이 아니라 같은 user+scope의 최신 상태 순서임을 유지했다.
- 구현 내용: producer·consumer, payload·envelope, outbox·publisher·inbox, eventId·멱등성, at-least-once, lease·retry·backoff·dead-letter, high-water mark·stale event·poison event, canonical payload·digest, fail-closed·pseudonymous data를 택배 발송·수령 비유와 함께 정리했다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: TMI-96 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 문서에 반복되는 기술 용어 설명을 요청했다. 애플리케이션 구현·계약 변경·외부 저장소 변경·Jira 변경·Git commit·push는 이번 요청의 승인 범위에 포함되지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 개념 설명과 기록이므로 이번 요청을 위한 Gradle 테스트는 실행하지 않았다. 종료 전 `git diff --check`와 turn marker 단일 존재를 정적으로 검증한다.
- 유지한 계약: raw phone과 PhoneIdentity fingerprint를 Entitlement payload로 전달하지 않고, eligibility candidate는 consumer scope와 별도 key/domain으로 분리한다. candidate도 가명정보로 취급하며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: `fingerprintCandidates`는 복수 전화번호가 아니라 key rotation 호환을 위한 동일 전화번호의 version별 eligibility candidate 집합이다. outbox event 수신은 binding 준비일 뿐 TrialClaim·UserEntitlement 지급을 의미하지 않는다.
- 위험 요소: fingerprint를 익명정보나 암호화된 phone으로 오해하면 복호화 가능성·보존 정책을 잘못 판단할 수 있다. candidate 배열을 patch나 여러 번호로 해석하거나 keyVersion·schemaVersion·bindingRevision을 혼용하면 중복 혜택과 역순 상태 적용 위험이 있다.
- 다음 작업: TMI-96 구현·운영 문서에서 같은 용어와 상태명을 일관되게 사용하고, 외부 Entitlement/Billing consumer 문서에도 동일 glossary와 event 예시를 포함한다.

## 2026-08-14 — TMI-96 Stage 5D Phone eligibility binding outbox publisher 구현

<!-- codex-turn:019fff41-8908-73f2-8998-45043cdbd1b0 -->

- 날짜: 2026-08-14
- 브랜치: `feat/TMI-96-phone-eligibility-outbox-publisher` (`31130fd` 기준, commit·push 미수행)
- Jira: TMI-96
- 작업 목표: ADR-002에 고정된 Phone eligibility binding schema v1, user+scope revision·verified/revoked lifecycle, atomic lease·retry/dead-letter HTTPS publisher와 보존·보안 계약을 Identity producer에 구현한다.
- 변경 파일: `PhoneEligibilityBindingOutbox`와 event/status/failure enum, `PhoneEligibilityBindingRevision`, `PhoneEligibilityBindingDeliveryScopeState`, 세 Mongo repository와 custom fragment, federation signup·PhoneIdentity·withdrawal Transaction, publisher application·wire mapper·retry policy, HTTPS/workload identity infrastructure·scheduler·properties/configuration, `application.yml`·`application-test.yml`, `.env.example`, `README.md`, 관련 단위·Transaction·Mongo 통합 테스트, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 변경했다. WORKLOG 과거 항목은 수정·삭제하지 않았다.
- 구현 내용: immutable UUID eventId, `PhoneEligibilityBindingVerified`·`PhoneEligibilityBindingRevoked`, `schemaVersion=1`, producer `identity`, `(userId, consumerScopeId, bindingRevision)` unique와 user+scope atomic revision을 추가했다. 가입 verified와 phone create/rotation/replace, 교체 release revoke, 회원 탈퇴 revoke를 해당 Mongo Transaction 안에서 outbox와 함께 저장한다.
- 구현 내용: outbox를 `PENDING`·`IN_FLIGHT`·`PUBLISHED`·`DEAD_LETTER`로 확장하고 due/expired lease atomic claim, attempt 증가, lease owner 조건부 상태 전이, 5초 시작·15분 cap·±20% jitter backoff, 최대 12회, 400/409/422 등 permanent dead-letter, 408/425/429/5xx·timeout/connection retry, 401/403 dead-letter와 scope pause, 동일 eventId/payload 수동 replay를 구현했다.
- 구현 내용: 최대 16 KiB canonical JSON wire mapper, HTTPS-only·redirect 금지·response body 미보관 JDK adapter, 최대 5분 credential을 발급하는 workload identity provider port와 민감 Token redaction을 추가했다. publisher는 기본 비활성이고 활성화 시 phone eligibility binding·HTTPS endpoint·audience·credential provider를 요구해 fail-closed한다.
- 구현 내용: PUBLISHED는 30일 cleanupAt TTL과 scheduler로 정리하고 DEAD_LETTER는 90일 review 시각만 기록해 자동 삭제하지 않는다. metric tag는 eventType·schemaVersion·outcome·failureCode로 제한하고 userId·eventId·candidate·credential을 포함하지 않는다. raw phone·last4·Firebase UID·provider subject·PhoneIdentity fingerprint를 wire·로그·metric에 추가하지 않았다.
- 실행한 테스트와 결과: 중간에 `./gradlew compileJava`, `testClasses`와 publisher·signup·withdrawal·PhoneIdentity·Mongo custom fragment 대상 테스트를 실행했다. 최종 `./gradlew clean test`는 73개 suite·433개 테스트가 failure 0, error 0, skipped 0으로 성공했고 `git diff --check`도 성공했다. 실제 Atlas·Firebase·consumer endpoint는 호출하지 않았다.
- 유지한 계약: UUID userId와 JWT `sub`, Identity producer와 외부 Entitlement/Billing consumer·Learning Core 경계, PhoneIdentity와 eligibility candidate의 key/domain 분리, eventId 멱등성과 bindingRevision 역순 방어, Refresh Token 원문 비저장, Firebase·publisher 기본 비활성을 유지했다. TrialClaim·Entitlement·시험 코드는 추가하지 않았다.
- 결정사항: delivery credential 실제 발급은 환경별 `WorkloadIdentityCredentialProvider` 구현 책임으로 남기고 static Token·key 설정을 만들지 않았다. 2xx만 publish 성공이며 consumer 응답 본문은 저장하지 않는다. DEAD_LETTER는 자동 삭제하지 않고 명시적 replay가 같은 event를 재사용한다.
- 위험 요소: 실제 consumer·workload identity 발급 인프라·endpoint allowlist·운영 on-call은 아직 없다. 인메모리 Mongo 검증은 실제 replica set Transaction, TTL 지연, 동시 lease/index rollout을 완전히 증명하지 않으므로 staging E2E가 필요하다. publisher만 준비된 상태에서 production flag를 켜면 outbox가 누적될 수 있다.
- 다음 작업: 사용자가 diff를 검토해 직접 commit·push하고 PR을 병합한다. 병합 확인 전 Jira를 Done으로 바꾸지 않으며, 별도 Entitlement/Billing consumer의 eventId inbox·payload digest·revision high-water·binding Transaction Jira를 생성·구현한 뒤 staging E2E를 수행한다.
- 수행한 Jira 작업: 구현 전에 Atlassian 공식 MCP로 TMI-96 설명·완료 조건·상태를 읽기 전용 확인했다. Jira 생성·수정·댓글·상태 전환은 수행하지 않았다.
- 추가한 댓글의 목적: 종료 댓글 초안에는 구현 요약, 변경 파일, 73개 suite·433개 테스트 결과와 남은 staging/consumer 위험만 포함할 예정이며 자동 등록하지 않았다.
- 변경한 상태: TMI-96은 `해야 할 일`, Resolution 없음으로 유지했다.
- 승인 여부: 사용자가 `구현 해줘`라고 구현을 승인했다. Jira 댓글·상태 변경과 Git commit·push는 승인하지 않았고 수행하지 않았다.

## 2026-08-15 — 선택적 quality review 동의 API 설계 검토

<!-- codex-turn:01a00467-1fc9-7601-8493-c1e844d47a38 -->

- 날짜: 2026-08-15
- 브랜치: `feat/TMI-96-phone-eligibility-outbox-publisher` (`31130fd`, commit·push 미수행)
- 작업 목표: Guest 생성·동의 갱신·동의 상태 조회에 선택적 quality review 동의 필드를 추가하는 제안이 현재 필수 동의 모델과 철회 요구를 올바르게 표현하는지 검토한다.
- 변경 파일: 이번 분석 기록으로 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 수정했다. 진행 중인 TMI-96 애플리케이션 변경과 동의 코드는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 현재 `ConsentPolicyStatusResponse`에는 privacy·terms에도 이미 `consented` 필드가 있으며 factory는 미동의 또는 version 불일치를 모두 `requiresConsent=true`로 계산함을 확인했다. 선택 동의를 같은 factory에 그대로 넣으면 false인데 requiresConsent가 false여야 한다는 제안과 충돌하므로 optional 전용 factory/DTO 또는 명시적 `required` flag를 권장했다.
- 구현 내용: `isQualityReviewConsented=true`는 요청 version이 서버 current version과 정확히 일치할 때만 저장하고, `false` 철회는 stale app version 때문에 차단하지 않는 조건부 검증을 권장했다. false snapshot은 `consentedVersion`·`consentedAt`을 null로 만들되 최초 미동의와 철회를 구분하고 철회 시점을 증명해야 한다면 별도 `changedAt`·`withdrawnAt` 또는 append-only consent history가 필요하다고 정리했다.
- 구현 내용: 동일한 상태로 반복 PUT하면 저장 시각을 바꾸지 않는 멱등성, true→false 철회 시 Mongo partial update의 과거 version/time `$unset`, 기존 문서의 누락 필드를 false/null로 읽는 하위 호환, privacy·terms true 강제 유지와 quality review false 허용을 요구사항으로 제안했다.
- 구현 내용: quality review가 실제 시험 답안·음성의 사람 검토나 품질 개선 이용을 제어한다면 Identity의 현재 상태만 변경해서는 철회가 완료되지 않으며, 데이터를 소유한 외부 서비스에 versioned consent changed/revoked event를 전달해 이후 처리 중지와 승인된 보존·삭제를 수행해야 함을 위험으로 표시했다. Guest 외 LOCAL·Firebase 가입 경로와 PUT 성공 응답·profile 응답도 계약 범위를 결정해야 한다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: TMI-96 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 API·도메인 설계 검토를 요청했다. 동의 기능 구현, 외부 lifecycle event, Jira 변경과 Git commit·push는 이번 요청의 승인 범위에 포함되지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 설계 검토와 기록이므로 Gradle 테스트를 실행하지 않았다. 종료 전 `git diff --check`와 turn marker 단일 존재를 정적으로 검증한다.
- 유지한 계약: privacy·terms는 필수 true와 current version 일치를 계속 요구하고 quality review만 선택 동의로 분리한다. 사용자 요청 시각을 신뢰하지 않고 서버 Clock을 사용하며 동의 내용이나 개인정보를 로그에 추가하지 않는다. Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 제안한 API 방향은 유효하지만 단순 두 필드 추가만으로는 부족하다. 철회 비차단 version 규칙, optional `requiresConsent` 계산, 멱등 timestamp, 철회 감사와 downstream 이용 중지 계약을 먼저 명시해야 한다.
- 위험 요소: false 요청에도 current version 일치를 강제하면 오래된 앱에서 철회를 못 할 수 있다. 반대로 false에서 version/time을 모두 지우고 별도 이력을 남기지 않으면 철회 사실과 시각을 증명할 수 없으며, Identity만 갱신하면 외부 서비스가 기존 데이터를 계속 품질 검토에 사용할 수 있다.
- 다음 작업: 구현 전에 quality review 데이터의 실제 소유 서비스·철회 후 보존/삭제·기존 데이터 적용 범위와 audit 수준을 확정한다. 이후 request/response·UserConsents·ConsentPolicy·Mongo partial update·모든 signup 경로·OpenAPI와 회귀 테스트를 하나의 Jira 범위로 구현한다.

## 2026-08-15 — Quality review 프론트 계약과 긴급 브랜치 적용 순서 검토

<!-- codex-turn:01a0046b-99c3-7013-b1e2-97eebe0bc441 -->

- 날짜: 2026-08-15
- 브랜치: `feat/TMI-96-phone-eligibility-outbox-publisher` (`31130fd`, commit·push 미수행)
- 작업 목표: 프론트가 quality review 동의 변경을 어떻게 이해해야 하는지와 즉시 적용을 위해 main checkout 후 수정해도 되는지 현재 Git 상태 기준으로 판단한다.
- 변경 파일: 이번 분석 기록으로 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 수정했다. 진행 중인 TMI-96 애플리케이션 변경과 동의 코드는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 프론트는 Guest와 consent PUT에 optional boolean·policy version을 추가하고, false를 가입 차단 없이 허용하며, GET의 qualityReview currentVersion·consented·nullable consent metadata·optional action 상태를 표시하고 false PUT을 철회로 사용한다고 정리했다. PUT 성공 응답에도 저장된 quality review 상태를 반환할지 계약을 맞춰야 한다.
- 구현 내용: response에 qualityReview object를 추가하는 것은 일반적으로 additive지만 POST/PUT의 신규 `@NotNull` request 필드는 구버전 client 요청을 400으로 만들 수 있음을 확인했다. 안전한 즉시 rollout은 backend가 누락 필드를 false로 취급하는 호환 모드로 먼저 배포하고 frontend가 새 필드를 전송하도록 전환한 뒤, 구버전 종료 후 명시 입력 강제 여부를 결정하는 순서다.
- 구현 내용: 현재 `main` `b6eb73e`는 `develop` `31130fd`의 ancestor이고 develop보다 16개 commit 뒤에 있음을 read-only Git 명령으로 확인했다. 또한 현재 TMI-96 branch에 tracked·untracked 미커밋 변경이 많으므로 바로 main checkout하면 변경 반입·충돌 또는 기능 누락 위험이 있다.
- 구현 내용: 사용자가 현재 TMI-96 변경을 먼저 commit·push하고 작업 트리를 깨끗하게 만든 다음 `develop`을 최신화해 별도 quality-review feature/hotfix branch를 생성하는 순서를 권장했다. 실제 운영이 main 배포만 허용한다면 develop 변경을 정식 PR로 main에 반영한 후 배포해야 하며 main에서 직접 개발하지 않는다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 프론트 계약과 Git 작업 순서에 대한 설명을 요청했다. checkout·stash·commit·push·branch 생성·동의 구현은 이번 요청의 승인 범위에 포함되지 않았고 수행하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 계약·Git 상태 분석과 기록이므로 Gradle 테스트를 실행하지 않았다. `git merge-base --is-ancestor main develop` 성공과 `main...develop` 차이 0/16을 확인했으며 종료 전 `git diff --check`와 turn marker 단일 존재를 검증한다.
- 유지한 계약: privacy·terms 필수 동의와 quality review 선택 동의를 구분하고 false 철회를 허용한다. 기존 client를 갑자기 차단하지 않는 rollout과 feature branch·PR 흐름을 유지하며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 프론트에는 단순 필드 추가뿐 아니라 선택 동의·철회·nullable·버전 규칙을 함께 전달한다. Git 작업은 main 직접 수정이 아니라 현재 변경 보존 후 최신 develop 기반 별도 branch로 진행한다.
- 위험 요소: backend와 frontend를 동시에 강제 변경하면 모바일 구버전이 장기간 400을 받을 수 있다. main에서 직접 작업하면 develop의 16개 commit과 TMI-96 변경이 빠지거나 나중에 큰 merge conflict가 발생할 수 있다.
- 다음 작업: quality review 정책·downstream 철회 범위를 Jira로 확정하고, 사용자가 TMI-96 변경을 commit·push한 뒤 최신 develop 기반 새 branch에서 backward-compatible backend, frontend, downstream 순으로 배포한다.

## 2026-08-15 — Quality review 선택 동의 구현 계획서 작성

<!-- codex-turn:01a00479-f4dc-7aa2-80fd-5a91610c8278 -->

- 날짜: 2026-08-15
- 브랜치: `feat/TMI-96-phone-eligibility-outbox-publisher` (`31130fd`, commit·push 미수행)
- 작업 목표: 사용자가 처음 설계한 quality review 외부 필드명을 유지하면서 Identity API·도메인·저장·하위 호환·테스트·배포와 외부 철회 연동의 구현 계획서를 작성한다.
- 변경 파일: 신규 `docs/contracts/quality-review-consent-implementation-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 진행 중인 TMI-96 애플리케이션 변경과 실제 동의 코드는 수정하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 요청 필드 `isQualityReviewConsented`·`qualityReviewConsentVersion`과 GET `qualityReview.currentVersion`·`consented`·`consentedVersion`·`consentedAt`·`requiresConsent`를 변경하지 않는 계약으로 고정했다. Guest POST·consent PUT·GET 예시와 PUT 성공 응답 범위를 문서화했다.
- 구현 내용: privacy·terms는 기존 필수 true/current version 규칙을 유지하고 quality review는 선택 동의로 분리했다. true는 current version exact match, false는 stale version으로 차단하지 않으며 false 저장은 version·consentedAt null, 동일 상태 반복 요청은 시각과 User updatedAt을 바꾸지 않는 성공 no-op으로 결정했다.
- 구현 내용: 기존 client가 새 request 필드를 보내지 않아도 초기 backend가 false로 처리하는 하위 호환 배포, 기존 Mongo 문서 누락=false/null 해석, 모든 가입 경로의 묵시적 true 금지, server Clock·ACTIVE User·CAS 갱신과 OpenAPI·설정·README 수정 범위를 계획했다.
- 구현 내용: 현재 snapshot만으로 최초 미동의와 철회를 구분하지 못하는 audit 한계를 명시하고 필요 시 append-only history를 별도 설계하도록 했다. 답안·음성·시험 결과는 Identity 소유가 아니므로 versioned consent changed/revoked event와 외부 멱등 consumer·보존/삭제 계약 전 실제 품질 검토를 활성화하지 않는 gate를 포함했다.
- 구현 내용: 요청 검증, 상태 전이·멱등성, GET 유효 동의 계산, Mongo 저장·동시성, 보안·로그와 전체 `./gradlew clean test`를 완료 조건으로 정리했으며 Learning Core·음성 저장·운영 검토 도구·법적 보존 기간 결정은 제외 범위로 분리했다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 고정 필드명 기반 계획서 작성을 명시적으로 요청했다. 애플리케이션 구현, checkout·branch 생성, Jira 변경과 Git commit·push는 승인 범위에 포함되지 않았고 수행하지 않았다.
- 실행한 테스트와 결과: 문서 작성만 수행해 Gradle 테스트를 실행하지 않았다. 계획서의 코드·설정 경로를 현재 구현과 대조했고 종료 전 `git diff --check`, turn marker 단일 존재와 WORKLOG EOF append를 정적으로 검증한다.
- 유지한 계약: privacy·terms 필수 동의, quality review 선택 동의·철회, canonical userId와 서버 Clock, Identity와 Learning Core 데이터 소유 경계를 유지했다. 요청 본문·동의 상세값을 로그에 추가하지 않고 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 외부 필드명은 사용자의 최초 설계대로 유지한다. Quality review 미동의는 가입·일반 기능을 막지 않고 `requiresConsent=false`이며, 저장된 true가 current version과 일치할 때만 현재 유효 동의로 사용한다.
- 위험 요소: false snapshot만 저장하면 철회 이력 증명이 불가능하고, 외부 consumer 없이 Identity만 false로 바꾸면 실제 데이터 이용이 계속될 수 있다. 새 request 필드를 즉시 필수화하면 구버전 client가 400을 받을 수 있다.
- 다음 작업: 사용자가 계획서를 검토한 뒤 별도 Jira와 최신 develop 기반 branch를 준비한다. 구현 시 backend 호환 모드, frontend 필드 전송, audit·downstream 철회 계약 순으로 진행하고 전체 테스트·staging 검증 후 활성화한다.

## 2026-08-15 — Quality review main-only hotfix 계획으로 수정

<!-- codex-turn:01a00486-d2e1-7b50-b1a7-9ffc0701c69b -->

- 날짜: 2026-08-15
- 브랜치: `feat/TMI-96-phone-eligibility-outbox-publisher` (`31130fd`, checkout·commit·push 미수행)
- 작업 목표: develop을 main에 포함하지 않고 현재 요청한 quality review 동의 변경만 main에 반영하도록 구현 계획의 Git 기준·적용 코드·검증 범위를 수정한다.
- 변경 파일: `docs/contracts/quality-review-consent-implementation-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 진행 중인 TMI-96 애플리케이션 코드와 실제 동의 코드는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: read-only Git 조회로 `main` `b6eb73e`가 `develop` `31130fd`보다 16개 commit 뒤지만 Guest·LOCAL signup, `UserConsents`, `ConsentPolicy`, consent PUT·GET 기반은 이미 보유함을 확인했다. main에는 현재 develop의 Firebase signup·Phone eligibility publisher가 없으므로 이번 hotfix에 가져오지 않는다.
- 구현 내용: 현재 TMI-96 작업 트리의 tracked·untracked 변경을 보존하기 위해 checkout·stash하지 않고 `origin/main` 기준 별도 `hotfix/quality-review-consent` worktree를 만드는 절차로 변경했다. hotfix PR base는 main이며 develop merge를 금지하고 `origin/main...HEAD` diff로 quality review 관련 파일만 포함됐는지 확인한다.
- 구현 내용: main의 기존 refactor 전 package 경로에 맞춘 예상 수정 파일을 계획서에 기록했다. 지정 외부 endpoint는 Guest POST와 consent PUT·GET이며 main의 LOCAL signup은 이번 요청에 새 필드를 임의 추가하지 않고 quality review false/null/null로 초기화한 뒤 인증된 PUT에서 선택하도록 범위를 한정했다. LOCAL signup UI에도 즉시 노출해야 한다면 별도 요구 확인 후 포함한다.
- 구현 내용: TMI-95 ADR, TMI-96 publisher, Firebase signup foundation, auth vertical slice refactor와 다른 develop commit이 main hotfix diff에 포함되지 않아야 한다는 완료 조건을 추가했다. main 반영 후 develop 동기화가 필요하면 hotfix commit만 별도 cherry-pick 또는 develop 구조에 재구현하며 이번 main PR과 분리한다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 develop이 main에 들어가면 안 되고 quality review 수정만 main에 있어야 한다고 명시했다. Git checkout·worktree 생성·commit·push·PR과 애플리케이션 구현은 아직 요청·승인되지 않아 수행하지 않았다.
- 실행한 테스트와 결과: 계획 문서 수정과 read-only Git 비교만 수행해 Gradle 테스트를 실행하지 않았다. main의 관련 파일 존재와 package 경로를 `git ls-tree`·`git show`로 확인했으며 종료 전 `git diff --check`, marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: 외부 quality review 필드명, privacy·terms 필수와 quality review 선택·철회 규칙, 구 client 누락=false 호환을 유지했다. main-only hotfix 경계를 지키고 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 이번 변경의 source와 PR base는 main이다. develop은 main에 merge하지 않고 hotfix에도 섞지 않는다. 현재 dirty TMI-96 worktree와 main hotfix worktree를 물리적으로 분리한다.
- 위험 요소: 현재 작업 트리에서 main으로 checkout하면 TMI-96 변경이 따라가거나 충돌할 수 있다. main과 develop은 package 구조가 달라 develop 코드를 복사하면 불필요한 refactor와 기능이 함께 들어갈 수 있으며, 장기적으로 main hotfix를 develop에 별도 동기화하지 않으면 다음 병합에서 충돌할 수 있다.
- 다음 작업: 사용자가 구현을 요청하면 별도 main worktree 생성과 main 전용 코드 재검토부터 시작한다. 변경 후 main 전체 테스트와 `origin/main...HEAD` 파일 목록을 검증하고 quality review 변경만 포함한 PR 초안을 준비한다.

## 2026-08-15 — Main hotfix worktree branch 확인

<!-- codex-turn:01a0049d-c800-7f50-a34a-a8815e9f7d6e -->

- 날짜: 2026-08-15
- 브랜치: 기존 worktree `feat/TMI-96-phone-eligibility-outbox-publisher` (`31130fd`), 별도 worktree `hotfix/quality-review-consent` (`b6eb73e`)
- 작업 목표: 사용자가 별도 worktree를 생성한 뒤 추가 branch switch가 필요한지 실제 Git worktree·branch 연결 상태로 확인한다.
- 변경 파일: 이번 확인 기록으로 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 수정했다. 애플리케이션 코드와 두 worktree의 branch·HEAD는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: `git worktree list --porcelain`로 기존 `/Users/msde76/identity`가 TMI-96 branch `31130fd`, 새 `/Users/msde76/identity-quality-review-hotfix`가 `hotfix/quality-review-consent` branch `b6eb73e`에 각각 연결된 것을 확인했다. hotfix branch는 `origin/main`을 tracking하며 생성 순간 새 worktree에 이미 checkout됐다.
- 구현 내용: worktree마다 서로 다른 branch가 동시에 checkout되는 구조이므로 기존 폴더에서 `git switch`할 필요가 없다. 사용자는 `cd /Users/msde76/identity-quality-review-hotfix`로 이동한 뒤 `pwd`, `git branch --show-current`, `git status --short`를 확인하고 hotfix 작업을 진행하면 된다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 branch switch 필요 여부 확인을 요청했다. checkout·switch·commit·push·애플리케이션 구현은 수행하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 Git 상태 확인과 기록이므로 Gradle 테스트를 실행하지 않았다. worktree path·HEAD·branch와 tracking 상태를 read-only로 확인하고 종료 전 `git diff --check`, marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: main-only hotfix와 기존 develop/TMI-96 변경의 물리적 격리를 유지했다. develop을 main에 병합하지 않았고 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 추가 `git switch`는 필요 없다. hotfix 폴더 자체가 이미 hotfix branch 작업 트리이며 기존 폴더는 TMI-96 branch 보존용으로 유지한다.
- 위험 요소: 기존 `/Users/msde76/identity`에서 계속 수정하면 quality review 변경이 TMI-96 worktree에 섞일 수 있다. 터미널과 IDE의 열린 프로젝트 경로가 hotfix 폴더인지 확인해야 한다.
- 다음 작업: hotfix 폴더로 이동해 branch와 clean status를 확인한 뒤 main 패키지 구조 기준으로 quality review 변경만 구현한다. PR 전 `origin/main...HEAD` diff에 develop 변경이 없는지 검증한다.

## 2026-08-15 — Quality review hotfix 작업 위치 최종 확인

<!-- codex-turn:01a0049f-fb35-7401-9054-f36d1de5c84f -->

- 날짜: 2026-08-15
- 브랜치: 별도 worktree `hotfix/quality-review-consent` (`b6eb73e`, `origin/main` 기준)
- 작업 목표: 사용자가 새 hotfix worktree로 이동한 뒤 현재 branch와 clean working tree를 올바르게 확인했는지 검증한다.
- 변경 파일: 이번 확인 기록으로 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 수정했다. 애플리케이션 코드와 Git branch·HEAD·index·working tree는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 사용자가 `/Users/msde76/identity-quality-review-hotfix`에서 실행한 `git branch --show-current` 결과가 `hotfix/quality-review-consent`임을 확인했다. 이어진 `git status --short` 출력이 없으므로 hotfix worktree가 clean한 상태임을 확인했다.
- 구현 내용: 추가 branch switch는 필요하지 않으며 터미널과 IDE 모두 hotfix worktree 경로를 사용해야 기존 `/Users/msde76/identity`의 TMI-96 변경과 quality review main hotfix가 섞이지 않는다고 안내했다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 branch와 status 실행 결과를 공유해 확인을 요청했다. checkout·switch·commit·push·PR과 애플리케이션 구현은 수행하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 Git 상태 확인과 기록이므로 Gradle 테스트를 실행하지 않았다. 사용자가 제공한 branch·status 결과와 앞서 조회한 worktree 연결 상태가 일치하며 종료 전 `git diff --check`, marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: main-only hotfix와 develop/TMI-96 worktree 격리를 유지했다. develop을 main에 병합하지 않았으며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: hotfix 구현 준비 상태는 정상이다. 현재 worktree와 branch를 그대로 사용하고 추가 `git switch`는 하지 않는다.
- 위험 요소: IDE가 기존 `/Users/msde76/identity`를 계속 열고 있으면 잘못된 branch에 수정할 수 있으므로 프로젝트 root 표시를 확인해야 한다.
- 다음 작업: 사용자가 구현을 요청하면 hotfix worktree에서 main의 기존 consent 구조를 다시 읽고 quality review 관련 변경만 적용한 뒤 전체 테스트와 `origin/main...HEAD` diff를 검증한다.

## 2026-08-15 — Quality review hotfix 커밋 가능 상태 확인 동기화

<!-- codex-turn:01a004b3-4ee4-79a3-afe7-66ede4bae4b5 -->

- 날짜: 2026-08-15
- 브랜치: 별도 worktree `hotfix/quality-review-consent` (`b6eb73e`, `origin/main` 기준, commit·push 미수행)
- 작업 목표: Quality review 구현을 지금 커밋해도 되는지 hotfix worktree의 변경 범위·정적 검사·전체 테스트로 확인하고 기존 Identity worktree 기록에도 동기화한다.
- 변경 파일: 이번 Hook 대응으로 기존 worktree의 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. hotfix 애플리케이션 구현은 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: hotfix worktree의 변경 파일이 Guest·consent API, User consent 도메인·저장, quality review 설정·오류·OpenAPI·README·계획서와 관련 테스트에 한정되고 Firebase·TMI-95·TMI-96 같은 develop 전용 파일이 없음을 확인했다.
- 구현 내용: staged 파일은 아직 없으며 사용자가 전체 의도 변경을 stage한 뒤 cached diff를 확인하고 단일 hotfix commit을 생성하도록 안내했다. Codex는 저장소 규칙에 따라 stage·commit·push를 수행하지 않았다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 커밋 가능 여부를 질문했다. Git commit·push는 사용자가 직접 수행하도록 명령만 제공했다.
- 실행한 테스트와 결과: hotfix worktree에서 `./gradlew clean test`가 BUILD SUCCESSFUL로 끝났고 42개 suite·319개 테스트가 failure 0, error 0, skipped 0이었다. `git diff --check`도 성공했으며 실제 Atlas·OAuth Provider·외부 데이터 소유 서비스는 호출하지 않았다.
- 유지한 계약: main-only hotfix와 develop/TMI-96 격리, privacy·terms 필수와 quality review 선택·철회·구 client 호환을 유지했다. Identity 밖의 시험·답안·음성 코드를 추가하지 않았고 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 사용자가 staged diff를 마지막으로 확인한 뒤 커밋 가능한 상태다. PR base는 main이며 quality review 변경만 포함해야 한다.
- 위험 요소: 일부 파일만 stage하면 코드·테스트·설정·문서가 분리될 수 있다. production 배포 전 Quality review version 환경설정과 외부 데이터 철회 연동 부재를 확인해야 한다.
- 다음 작업: 사용자가 `git add -A`, cached diff 검증, commit·push를 수행하고 main 대상 PR에서 `origin/main...HEAD` 변경 목록을 다시 확인한다.

## 2026-08-15 — Quality review 배포 환경변수 확인

<!-- codex-turn:01a004b7-968c-7be0-b4ac-6f16bffdb94e -->

- 날짜: 2026-08-15
- 브랜치: 별도 worktree `hotfix/quality-review-consent`, commit `4745652`, 원격 `origin/hotfix/quality-review-consent`
- 작업 목표: Quality review 선택 동의 hotfix 배포에 새 환경변수가 필요한지 코드·설정·문서와 현재 Git 상태로 확인한다.
- 변경 파일: 이번 확인 기록으로 기존 worktree의 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. hotfix 코드·설정·commit은 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: hotfix의 `.env.example`에 `QUALITY_REVIEW_CONSENT_VERSION=quality-review-v1`, `application.yml`에 `app.consent.quality-review-version: ${QUALITY_REVIEW_CONSENT_VERSION}`, `ConsentPolicy`에 non-null·non-blank 기동 검증이 연결된 것을 확인했다. README도 이 값을 필수로 문서화한다.
- 구현 내용: 환경변수는 Secret이 아니라 서버가 현재 제공하는 Quality review 정책 version 식별자다. local·staging·production 배포 환경에 명시적으로 추가하고 frontend 요청의 `qualityReviewConsentVersion`과 정확히 일치시켜야 한다. 누락 또는 공백이면 fail-fast 기동 실패하며 test profile은 `application-test.yml`의 가짜 `quality-review-v1`을 사용한다.
- 구현 내용: hotfix worktree HEAD가 commit `4745652` `feat: add optional quality review consent`이고 원격 hotfix branch와 일치하며 working tree가 clean한 것을 read-only Git 조회로 확인했다. main보다 한 commit 앞이고 아직 main 병합 여부는 확인하지 않았다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 배포 환경변수 필요 여부를 질문했다. 환경 설정·배포·Git 변경은 수행하지 않고 필요한 이름과 값만 안내했다.
- 실행한 테스트와 결과: 이번에는 코드 변경이 없는 설정 확인이므로 Gradle 테스트를 다시 실행하지 않았다. 직전 hotfix 전체 결과는 42개 suite·319개 테스트 failure 0, error 0, skipped 0이며 이번 종료 전 기록 문서의 `git diff --check`, marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: 실제 정책 문구나 개인정보를 환경변수에 넣지 않고 version 식별자만 사용한다. Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았으며 main-only hotfix와 develop 격리를 유지했다.
- 결정사항: 배포 전 `QUALITY_REVIEW_CONSENT_VERSION` 추가는 필수다. 현재 version 값은 frontend 계약과 같은 `quality-review-v1`로 맞춘다.
- 위험 요소: 환경변수가 없으면 애플리케이션이 기동하지 않고, frontend와 server version이 다르면 `true` 동의 요청이 version mismatch로 거절된다. 정책 내용이 변경되면 기존 값을 재사용하지 말고 새 version을 배포 계약으로 함께 전환해야 한다.
- 다음 작업: main 대상 PR과 배포 설정에 `QUALITY_REVIEW_CONSENT_VERSION=quality-review-v1`을 반영하고, 배포 후 GET currentVersion과 Guest·PUT true/false 흐름을 staging에서 검증한다.

## 2026-08-15 — Quality review staging 도메인·환경변수 구분 확인

<!-- codex-turn:01a004be-d763-7163-95ac-0d4594cc5973 -->

- 날짜: 2026-08-15
- 브랜치: 별도 worktree `hotfix/quality-review-consent`, commit `4745652`, 원격 `origin/hotfix/quality-review-consent`
- 작업 목표: Staging frontend의 Identity·Learning API base URL을 Quality review 점검 때문에 다른 domain으로 바꿔야 하는지와 backend 배포 환경변수의 역할을 구분한다.
- 변경 파일: 이번 확인 기록으로 기존 worktree의 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. hotfix 애플리케이션 코드·설정·Git 상태는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: hotfix 코드·설정·문서를 검색해 Quality review 전용 base URL, endpoint 설정 또는 외부 HTTP adapter가 없음을 확인했다. 선택 동의는 기존 Identity의 Guest POST와 consent PUT·GET에서 저장·조회되므로 frontend는 기존 Identity staging base URL을 계속 사용한다.
- 구현 내용: `EXPO_PUBLIC_IDENTITY_API_BASE_URL`과 `EXPO_PUBLIC_LEARNING_API_BASE_URL`은 frontend가 각각 Identity·Learning staging 서버로 요청할 공개 base URL이다. `QUALITY_REVIEW_CONSENT_VERSION`은 Identity runtime의 현재 정책 version 문자열이며 URL이나 점검 서버 주소가 아니다.
- 구현 내용: Staging frontend는 기존 두 staging domain을 유지하고 `qualityReviewConsentVersion=quality-review-v1`을 Identity 요청 body에 보낸다. Identity staging 배포 설정에는 `QUALITY_REVIEW_CONSENT_VERSION=quality-review-v1`을 추가해 양쪽 값을 일치시킨다. Learning API base URL은 이번 hotfix로 변경하지 않는다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 staging domain과 배포 환경변수 사용법을 질문했다. frontend·backend 환경설정, 배포, Git과 외부 시스템을 변경하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 설정·호출 경계 확인이므로 Gradle 테스트를 다시 실행하지 않았다. 직전 hotfix 전체는 42개 suite·319개 테스트 성공 상태이며 종료 전 기록 문서의 `git diff --check`, marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: 인증·동의 API는 Identity domain, 학습 API는 Learning domain으로 분리하고 Quality review 동의 때문에 Learning 또는 별도 도메인으로 사용자 인증 요청을 보내지 않는다. Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 현재 staging에서는 별도 Quality review domain이 필요 없다. 기존 staging API base URL을 유지하고 Identity staging runtime에 정책 version 환경변수만 추가한다.
- 위험 요소: frontend version 문자열과 Identity 환경변수가 다르면 true 동의 요청이 version mismatch로 거절된다. 향후 실제 검토 데이터 consumer가 별도 서비스로 생겨도 그 내부 endpoint는 server-side 계약이며 frontend의 `EXPO_PUBLIC_*` 값으로 노출하지 않는다.
- 다음 작업: Staging Identity 환경에 `QUALITY_REVIEW_CONSENT_VERSION=quality-review-v1`을 추가하고 앱 요청 version을 일치시킨 뒤 기존 Identity staging domain에서 Guest·GET·PUT true/false 흐름을 점검한다.

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

## 2026-08-15 — Staging Identity 기동 실패 원인 분석

<!-- codex-turn:01a004c0-1bfd-7920-a9fb-9364945924ce -->

- 날짜: 2026-08-15
- 브랜치: Quality review main hotfix 배포 진단
- 작업 목표: 사용자가 공유한 staging Identity `Application run failed` 로그의 실제 원인과 복구 방법을 확인한다.
- 변경 파일: 이번 누락 Hook 기록으로 기존 worktree의 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션·배포 설정·Git 상태는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 긴 `UnsatisfiedDependencyException` chain에서 최하위 cause가 `PlaceholderResolutionException: Could not resolve placeholder 'QUALITY_REVIEW_CONSENT_VERSION'`임을 확인했다. `ConsentPolicy` 생성 실패가 UserFactory·SignupService·AuthController로 전파돼 전체 기동이 실패한 것이며 API domain이나 MongoDB 장애가 아니다.
- 구현 내용: Identity staging Task Definition의 Identity container에 비밀이 아닌 정책 version `QUALITY_REVIEW_CONSENT_VERSION=quality-review-v1`을 추가하고 새 task revision·service deployment를 수행해야 한다고 안내했다. frontend `qualityReviewConsentVersion`과 정확히 일치해야 한다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 오류 원인 진단을 요청했다. ECS·환경변수·Git·외부 시스템을 변경하지 않았다.
- 실행한 테스트와 결과: 로그 분석만 수행해 Gradle 테스트를 실행하지 않았다. 최하위 exception과 hotfix 설정 연결을 대조했으며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 유지한 계약: Quality review version 누락은 fail-fast하고 별도 review domain을 만들지 않는다. staging frontend는 기존 Identity domain을 유지한다.
- 결정사항: 장애 원인은 필수 backend runtime 환경변수 누락이다.
- 위험 요소: 변수 추가 후에도 ECS service가 이전 task revision을 사용하면 같은 오류가 반복된다. 다른 container나 frontend에만 설정해도 Identity 기동 문제는 해결되지 않는다.
- 다음 작업: Identity staging의 새 task revision에 version을 추가하고 새 task 로그·health와 Guest·GET·PUT 요청을 확인한다.

## 2026-08-18 — Main hotfix 이후 develop 병합 전략 분석

<!-- codex-turn:01a012db-dd0f-7572-a959-a289a662c2db -->

- 날짜: 2026-08-18
- 브랜치: 로컬 `feat/TMI-96-phone-eligibility-outbox-publisher` (`feaf095`), 원격 비교 `main=3894627`, `develop=6f02f4a`
- 작업 목표: Quality review hotfix를 main에 먼저 반영한 뒤 develop→main merge가 충돌하는 이유와 향후 안전한 동기화 절차를 설명한다.
- 변경 파일: 이번 분석 기록으로 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. merge·checkout·애플리케이션 코드·Jira는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 원격 `git ls-remote`와 fetch로 main `3894627`이 hotfix merge PR #24와 commit `4745652`를 포함하고, develop `6f02f4a`가 TMI-96 merge PR #25를 포함함을 확인했다. 공통 merge base는 hotfix 전 `b6eb73e`이며 main-only 2개, develop-only 18개 commit으로 양쪽이 분기됐다.
- 구현 내용: read-only `git merge-tree` 시뮬레이션에서 `.env.example`, README, consent domain·factory·DTO·tests와 기록 문서처럼 양쪽이 수정한 파일에서 충돌이 재현됐다. 이는 이전 main-only hotfix 때문에 생긴 정상적인 branch divergence와 develop의 package refactor·Firebase 확장이 겹친 결과다.
- 구현 내용: Quality review 기능을 develop에도 반영하는 것이 맞지만 hotfix commit만 cherry-pick하면 내용은 복제돼도 main ancestry가 develop에 들어오지 않아 향후 merge 문제를 확실히 없애지 못한다고 정리했다. 최신 develop 기반 sync branch에서 `origin/main`을 `--no-ff` merge하고 develop 구조를 기준으로 Quality review 동작·설정·테스트를 수동 통합하는 방식을 권장했다.
- 구현 내용: sync PR은 develop을 대상으로 하고 squash/rebase가 아니라 merge commit으로 반영해 main commit이 develop ancestry에 포함되게 해야 한다. 그 뒤 `git merge-base --is-ancestor origin/main origin/develop`을 확인하면 나중에 준비된 시점의 develop→main release merge에서 이번 hotfix를 다시 충돌 원인으로 다루지 않는다. 현재 당장 develop을 main에 merge할 필요는 없다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 향후 병합 방법에 대한 분석을 요청했다. 실제 merge·conflict resolution·branch 생성·commit·push는 승인 범위에 포함되지 않아 수행하지 않았다.
- 실행한 테스트와 결과: 코드 변경 없이 원격 ref·commit graph·merge base·left/right count와 merge-tree를 확인했다. Gradle 테스트는 실행하지 않았고 종료 전 `git diff --check`, 두 turn marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: 지금 develop 전체를 main에 반영하지 않고 Quality review hotfix만 main에 유지한다. 추후 동기화에서도 develop의 Firebase·TMI-96 기능을 main에 조기 배포하지 않으며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 문제는 hotfix 자체가 잘못된 것이 아니라 main과 develop이 각각 다른 변경을 가진 분기 상태라서 발생한다. 다음 release 전 main→develop 동기화 merge를 한 번 수행하고 develop 구조에 Quality review를 통합한다.
- 위험 요소: conflict에서 main 파일 전체를 선택하면 develop의 refactor·Firebase·TMI-96 기능을 잃고, develop 파일 전체를 선택하면 Quality review hotfix가 사라진다. sync PR을 squash하면 ancestry 연결 목적을 달성하지 못할 수 있다.
- 다음 작업: 당장 release merge는 하지 않는다. 준비된 시점에 최신 develop 기반 `sync/main-quality-review-hotfix` branch를 만들어 main을 merge하고 각 conflict를 의미 단위로 해결한 뒤 전체 테스트·main ancestry·diff를 검증해 develop 대상 merge-commit PR로 반영한다.

## 2026-08-18 — Main Quality review hotfix를 develop에 선제 동기화

<!-- codex-turn:01a012df-464a-7833-9cd8-94a42975b582 -->

- 날짜: 2026-08-18
- 브랜치: `codex/sync-main-quality-review-hotfix` (`origin/develop` 기반, `origin/main` merge 진행 중, commit·push 미수행)
- 작업 목표: main에 먼저 반영된 Quality review 선택 동의 hotfix를 develop의 Firebase·TMI-96 구조와 함께 유지하도록 미리 역병합하고 향후 develop→main release merge의 분기와 충돌을 줄인다.
- 변경 파일: main hotfix의 환경변수 예시·README·Quality review 계획서, Guest request·service·controller, User consent policy·factory·entity·service·DTO·controller·오류 코드, application 설정과 관련 테스트를 develop에 통합했다. develop 전용 Firebase test fixture와 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 추가 갱신했으며 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 최신 `origin/develop`에서 별도 동기화 브랜치를 만들고 `git merge --no-ff --no-commit origin/main`을 수행했다. CURRENT_STATE·WORKLOG·계획서·UserConsentService·UserFactoryTests 충돌을 의미 단위로 해결해 develop의 Firebase 회원 생성과 accountType 로그, main의 Quality review Guest 생성·선택·철회·조회 계약과 테스트를 모두 보존했다.
- 구현 내용: 자동 병합 뒤 develop 전용 `FirebaseSignupServiceTests`가 3개 인자의 `ConsentPolicy`를 생성하도록 Quality review test version을 추가했다. 동의 성공 로그는 userId·accountType·provider·outcome을 유지하되 동의 시각은 남기지 않아 기존 로그 비노출 테스트와 main hotfix의 민감정보 방어 결정을 함께 유지했다.
- 구현 내용: main에만 존재하던 Quality review 구현 WORKLOG 기록 중 develop에 없던 항목을 파일 끝에 보존하고, 직전 staging 기동 실패·branch divergence 분석 기록도 임시 보관본에서 복원했다. 필수 외부 필드명 `isQualityReviewConsented`, `qualityReviewConsentVersion`과 runtime 설정 `QUALITY_REVIEW_CONSENT_VERSION`을 변경하지 않았다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자가 main hotfix를 develop에 지금 미리 통합하도록 승인했다. 저장소 규칙에 따라 Codex는 merge commit·push를 수행하지 않고 충돌 해결과 검증된 staged 상태까지만 준비했다.
- 실행한 테스트와 결과: 첫 `./gradlew clean test`는 Gradle 사용자 cache lock의 sandbox 접근 제한으로 시작 전 실패해 승인된 동일 명령으로 재실행했다. 첫 재실행은 Firebase 전용 test fixture의 이전 생성자 호출로 compile 실패했고 수정 후 두 번째는 동의 시각 로그 비노출 assertion 1건이 실패했다. 로그에서 동의 시각을 제거한 최종 실행은 BUILD SUCCESSFUL이며 전체 457개 테스트가 통과했다.
- 유지한 계약: UUID userId, JWT sub·RS256·kid·issuer·audience, RefreshSession, Firebase broker와 TMI-96 outbox publisher를 유지했다. Identity 밖의 시험·답안·음성 코드를 추가하지 않았고 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 지금 main→develop을 동기화하는 것이 이후 충돌 확대를 줄이므로 별도 branch에서 선제 수행했다. 사용자가 현재 merge를 일반 merge commit으로 완료하고 develop 대상 PR도 squash/rebase가 아닌 merge commit 방식으로 병합해야 main ancestry가 develop에 보존된다.
- 위험 요소: 아직 merge commit이 없어 `origin/main`은 현재 HEAD의 ancestor가 아니다. commit 전에 일부 staged 파일을 제외하면 계약·설정·테스트가 분리될 수 있고, develop PR을 squash하면 ancestry 연결 목적을 달성하지 못한다. 배포 환경에는 `QUALITY_REVIEW_CONSENT_VERSION=quality-review-v1`이 계속 필요하며 외부 품질 검토 데이터의 철회 lifecycle은 별도 계약이 필요하다.
- 다음 작업: 사용자가 staged diff를 검토해 현재 merge commit을 생성하고 branch를 push한 뒤 develop 대상 PR을 Create a merge commit 방식으로 병합한다. 병합 후 `git merge-base --is-ancestor origin/main origin/develop`의 종료 코드 0과 staging Guest·GET·PUT 선택·철회 흐름을 확인한다.

## 2026-08-18 — Develop 병합 이후 다음 작업 우선순위 확인

<!-- codex-turn:01a012fc-9d3e-76b1-8df6-9ce950a1d2d5 -->

- 날짜: 2026-08-18
- 브랜치: `develop` (`24275a5`, `origin/develop`과 일치, Codex commit·push 미수행)
- Jira: TMI-96
- 작업 목표: Quality review hotfix 역병합과 TMI-96 구현 병합 이후 실제로 남은 종료 작업과 다음 Identity 개발 범위를 현재 Git·Jira·로드맵 기준으로 정한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 이번 분석 시작 전 WORKLOG 끝에 이미 존재하던 과거 기록 복원 append는 수정하거나 삭제하지 않았고 애플리케이션 코드는 변경하지 않았다.
- 구현 내용: 로컬 `develop`과 `origin/develop`이 PR #26 merge commit `24275a5`로 일치하고, TMI-96 PR #25 merge commit `6f02f4a`와 Quality review 역병합 merge commit `d67ba69`을 포함함을 확인했다. `origin/main`이 `origin/develop`의 ancestor인지 검사한 결과 종료 코드 0이었다.
- 구현 내용: 즉시 필요한 종료 작업은 병합된 TMI-96의 Jira 댓글·완료 전환이며, 다음 Identity 기능 작업은 `docs/contracts/social-login-implementation-plan.md`의 Stage 6 Guest 승격·MEMBER 인증수단 동기화로 정리했다. 외부 Entitlement/Billing consumer와 staging E2E는 별도 병렬 production gate이고 Identity 저장소에 TrialClaim·UserEntitlement·시험 코드를 추가하지 않는다.
- 수행한 Jira 작업: Atlassian 공식 MCP로 TMI-96을 읽기 전용 조회해 상태 `해야 할 일`, Resolution 없음, 댓글 없음과 완료 transition ID `41` 사용 가능 여부를 확인했다. 댓글·상태·필드는 변경하지 않았다.
- 추가한 댓글의 목적: PR #25의 outbox schema/revision·revoke, lease/retry/dead-letter HTTPS publisher 구현, 테스트 결과와 실제 consumer·workload identity·staging E2E 미검증 위험을 인수인계하는 종료 댓글을 다음 승인 단계에서 제안한다.
- 변경한 상태: TMI-96은 `해야 할 일`, Resolution 없음 상태를 유지한다.
- 승인 여부: 사용자는 다음 작업 확인을 요청했다. Jira mutation, 새 Jira 생성, 코드 구현, Git commit·push는 요청하거나 승인하지 않았으므로 수행하지 않았다.
- 실행한 테스트와 결과: 코드 변경 없는 Git·Jira·로드맵 분석이므로 Gradle 테스트를 재실행하지 않았다. 통합 기준의 직전 최종 `./gradlew clean test`는 전체 457개 테스트 성공이며, 이번 turn에서 main ancestry 검사는 종료 코드 0이었다. 종료 전 `git diff --check`와 marker 단일 존재를 확인한다.
- 유지한 계약: UUID 실제 userId, JWT sub·RS256·kid·issuer·audience, Firebase 기본 비활성, Identity producer와 외부 Entitlement/Billing consumer 경계를 유지했다. Secret, Token, Password, 실제 Key, 전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: TMI-96을 먼저 행정적으로 종료한 뒤 Identity의 다음 Jira를 Stage 6으로 잡는다. Stage 6은 Guest userId 유지 승격 Transaction과 auth-method sync까지이며 실제 Guest merge·UserMerged publisher는 Stage 7로 분리한다. 외부 consumer와 staging E2E 완료 전 production 기능은 활성화하지 않는다.
- 위험 요소: TMI-96은 병합됐지만 Jira가 아직 열린 상태다. 실제 consumer endpoint·workload identity 발급 인프라·Mongo replica set 경쟁·TTL/index·manual replay가 staging에서 검증되지 않았고, Stage 6에서 기존 Guest Session 폐기와 여러 identity/outbox 저장의 Transaction·동시성 경계를 세밀하게 고정해야 한다.
- 다음 작업: 사용자 승인 후 공개한 종료 댓글을 TMI-96에 등록하고 transition ID `41`만 적용한다. 그 다음 Stage 6 Jira의 제목·설명·완료 조건·제외 범위를 먼저 제시해 승인받고 생성·구현한다.

## 2026-08-18 — TMI-96 종료·Stage 6 Jira 생성 최종 변경안 준비

<!-- codex-turn:01a01303-8c19-7350-8daa-8865670558fc -->

- 날짜: 2026-08-18
- 브랜치: `develop` (`24275a5`, `origin/develop`과 일치, Codex commit·push 미수행)
- Jira: TMI-96
- 작업 목표: 사용자가 요청한 TMI-96 종료와 다음 Stage 6 Jira 생성에 앞서 저장소 규칙에 따라 정확한 Jira 댓글·전환·신규 이슈 Payload를 확인하고 공개한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: TMI-96의 설명과 완료 조건을 PR #25 merge commit `6f02f4a`, 현재 `develop` 통합 commit `24275a5`, 로드맵 Stage 6 및 실제 Firebase enrollment·signup Transaction 코드와 대조했다. Stage 6 범위를 Guest-bound enrollment, 기존 Guest UUID 유지 MEMBER 승격, Guest Session 폐기, identity·phone eligibility outbox·attempt consume Transaction, auth-method sync와 기존 owner의 mutation 없는 `MERGE_REQUIRED`로 정리하고 실제 merge는 Stage 7로 분리했다.
- 수행한 Jira 작업: Atlassian 공식 MCP로 TMI-96의 상태 `해야 할 일`, Resolution 없음, 댓글 없음과 완료 transition ID `41`을 읽기 전용 재확인했다. Rovo Search로 Stage 6 Guest 승격·MEMBER 인증수단 동기화 관련 중복 이슈가 없음을 확인했다. 댓글·상태·필드·신규 이슈는 변경하거나 생성하지 않았다.
- 추가한 댓글의 목적: TMI-96의 schema v1 verified/revoked event, atomic revision, lifecycle Transaction, lease·retry·dead-letter HTTPS publisher, 테스트 결과와 consumer·workload identity·staging E2E 잔여 위험을 인수인계하는 종료 댓글 전문을 사용자에게 공개하기 위해 준비했다.
- 변경한 상태: TMI-96은 `해야 할 일`, Resolution 없음 상태를 유지하고 신규 Stage 6 Jira도 아직 생성하지 않았다.
- 승인 여부: 사용자는 TMI-96 종료와 새 Jira 생성을 요청했다. 그러나 직전 안내에는 Jira 댓글 전문과 신규 이슈의 프로젝트·유형·제목·우선순위·전체 설명·완료 조건·제외 범위가 없었으므로, AGENTS.md가 요구하는 사전 공개 후 승인을 충족하기 위해 이번 turn에서는 정확한 최종안을 제시하고 별도 승인을 기다린다.
- 실행한 테스트와 결과: Jira 읽기 조회·코드/로드맵 대조·문서 기록만 수행해 Gradle 테스트를 재실행하지 않았다. 현재 통합 기준의 직전 `./gradlew clean test`는 전체 457개 테스트 성공이다. 종료 전 `git diff --check`, marker 단일 존재와 WORKLOG EOF append를 확인한다.
- 유지한 계약: canonical UUID 실제 userId를 Guest 승격 후에도 유지하고 client userId를 신뢰하지 않는다. JWT·Firebase 기본 비활성, Identity producer와 외부 Entitlement/Billing consumer 경계, 실제 merge의 Stage 7 분리를 유지하며 Secret, Token, Password, 실제 Key, 전체 MongoDB URI와 사용자 개인정보를 Jira 초안이나 기록에 포함하지 않았다.
- 결정사항: TMI-96은 댓글 등록 뒤 transition ID `41`만 적용한다. 새 이슈는 TMI `작업`, High, 기본 `해야 할 일`로 제안하고 담당자·라벨·컴포넌트·스프린트·에픽·기한·상태 전환을 설정하지 않는다. production flag 활성화, legacy API 종료, 실제 Guest merge와 외부 Entitlement 기능은 제외한다.
- 위험 요소: 실제 consumer endpoint·workload identity 인프라·staging Mongo replica set E2E는 여전히 미검증이다. Stage 6은 Guest Session 폐기와 여러 identity/outbox/attempt 저장의 원자성, concurrent 승격과 기존 owner 충돌 분류를 구현 전에 테스트 계약으로 고정해야 한다.
- 다음 작업: 사용자가 공개된 TMI-96 종료 댓글·완료 전환과 Stage 6 신규 Jira Payload를 동일 내용으로 최종 승인하면 Atlassian 공식 MCP로 댓글 등록, transition ID `41`, 신규 이슈 생성만 수행하고 결과를 재조회한다.

## 2026-08-18 — TMI-96 완료 처리 및 Stage 6 Jira TMI-97 생성

<!-- codex-turn:01a01305-66b6-7b20-8c8a-9aef06103f30 -->

- 날짜: 2026-08-18
- 브랜치: `develop` (`24275a5`, `origin/develop`과 일치, Codex commit·push 미수행)
- Jira: TMI-96, TMI-97
- 작업 목표: 사용자에게 정확히 공개한 TMI-96 종료 댓글·완료 전환과 다음 Stage 6 Guest MEMBER 승격·인증수단 동기화 Jira 생성 Payload를 승인 내용 그대로 적용하고 결과를 검증한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: TMI-96 종료 댓글에 ADR-002 schema v1 verified/revoked event, atomic bindingRevision, lifecycle outbox, lease·retry·dead-letter HTTPS publisher와 기능 기본 비활성, TMI-96 기준 433개 및 develop 통합 기준 457개 테스트 성공, 외부 consumer·workload identity·staging E2E 잔여 위험을 기록했다.
- 구현 내용: TMI-97은 authenticated Guest JWT `sub`를 canonical userId로 유지하는 MEMBER 승격, Guest-bound enrollment, 기존 Guest Session 폐기와 신규 Session, FirebaseIdentity·PhoneIdentity·SocialIdentity·PhoneEligibilityBindingOutbox·attempt consume Transaction, 기존 owner의 mutation 없는 `MERGE_REQUIRED`, MEMBER auth-method sync와 rollback·동시성·민감정보 비노출 테스트를 범위로 생성했다. 실제 merge·Learning Core 데이터 이전·외부 Entitlement 기능·production 활성화·legacy API 종료는 제외했다.
- 수행한 Jira 작업: Atlassian 공식 MCP로 TMI-96에 종료 댓글 ID `10007`을 등록하고 transition ID `41`만 적용했다. 이어서 승인된 Payload로 TMI `작업` 이슈 `TMI-97`을 생성했으며 다른 기존 이슈는 변경하지 않았다.
- 추가한 댓글의 목적: TMI-96 구현·테스트 결과와 실제 consumer·workload identity·Mongo replica set staging E2E가 남아 있음을 인수인계하기 위해 종료 댓글을 등록했다.
- 변경한 상태: TMI-96을 `해야 할 일`에서 `완료`로 전환했고 후속 조회에서 status ID `10003`과 Resolution `완료`를 확인했다. TMI-97은 기본 상태 `해야 할 일`, Resolution 없음으로 생성했고 별도 상태 전환은 적용하지 않았다.
- 승인 여부: 직전 응답에서 TMI-96 댓글 전문·transition ID `41`과 TMI-97의 프로젝트·유형·제목·우선순위·설명·완료 조건·제외 범위·미설정 필드를 공개했고 사용자가 `어 해줘`라고 동일 내용의 실행을 명시적으로 승인했다.
- 실행한 테스트와 결과: Jira mutation·후속 조회와 문서 기록만 수행해 Gradle 테스트를 재실행하지 않았다. 현재 develop 통합 기준의 직전 `./gradlew clean test`는 전체 457개 테스트 성공이다. TMI-96 댓글·상태·Resolution과 TMI-97 제목·유형·우선순위·설명·상태·빈 담당자·라벨·컴포넌트를 후속 조회로 확인했다.
- 유지한 계약: canonical UUID 실제 userId, JWT sub·RS256·kid·issuer·audience, Firebase/provider 기본 비활성, Identity와 외부 Entitlement/Billing 및 Learning Core 경계를 유지했다. Secret, Token, Password, 실제 Key, 전체 MongoDB URI와 사용자 개인정보를 Jira나 기록에 포함하지 않았다.
- 결정사항: 다음 Identity 구현 기준은 TMI-97이다. Guest 승격에서는 Request Body userId를 받지 않고 JWT `sub`를 사용하며 실제 source/target merge와 `UserMergedOutbox`는 Stage 7로 분리한다. consumer와 staging E2E 완료 전 production 기능은 활성화하지 않는다.
- 위험 요소: TMI-97 구현에서 기존 Guest Session 폐기와 identity·phone alias·revision·outbox·attempt·신규 Session의 Transaction 원자성, concurrent 승격과 owner 충돌 분류를 고정해야 한다. 실제 Entitlement/Billing consumer와 workload identity 발급 인프라·staging Mongo 검증도 여전히 별도 gate다.
- 다음 작업: TMI-97 구현 전에 Jira 본문과 완료 조건을 다시 읽고 현재 Guest 생성·Firebase exchange/signup·Session·PhoneIdentity·outbox Transaction을 대조한다. 이후 TMI-97 브랜치를 만들어 구현하되 Git commit·push는 사용자가 수행한다.

## 2026-08-18 — TMI-97 구현 범위와 사용자·데이터 흐름 설명

<!-- codex-turn:01a01315-6c87-7341-a0cb-986f4f42554b -->

- 날짜: 2026-08-18
- 브랜치: `develop` (`24275a5`, `origin/develop`과 일치, Codex commit·push 미수행)
- Jira: TMI-97
- 작업 목표: Stage 6 Guest MEMBER 승격 및 인증수단 동기화에서 사용자 요청 흐름, 내부 Transaction, 충돌 분기, 현재 기반과 추가 구현 항목을 쉽게 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드와 Jira는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: Atlassian 공식 MCP로 TMI-97 설명·완료 조건·상태 `해야 할 일`을 읽고 ADR-001, social login 계획, Guest 생성, Firebase exchange/signup, enrollment, User, RefreshSession, PhoneIdentity와 eligibility outbox 코드를 대조했다. 보호 API `guest/prepare`가 Guest JWT `sub`와 Firebase UID를 묶은 GUEST_USER attempt를 만들고, `guest/upgrade`가 fresh phone proof와 동의를 재검증한 뒤 같은 Guest UUID를 MEMBER로 승격하는 2단계 흐름을 정리했다.
- 구현 내용: 성공 Transaction은 User in-place 승격과 guest installation credential 제거, FirebaseIdentity·PhoneIdentity/alias·SocialIdentity·eligibility binding revision/outbox 저장, 기존 Guest Session 폐기, 신규 RefreshSession 저장과 enrollment CAS consume을 함께 처리해야 한다. Firebase UID·provider subject가 다른 ACTIVE MEMBER 소유면 Stage 7 전에는 `MERGE_REQUIRED`만 반환하고 source/target을 변경하지 않는다.
- 구현 내용: 기존 MEMBER auth-method sync는 Identity JWT와 FirebaseIdentity의 UID 일치, `AUTH_METHOD_SYNC` fresh proof와 provider subject ownership을 검증해 누락 SocialIdentity만 멱등 추가한다. 일반 로그인마다 phone OTP를 반복하거나 email·phone으로 자동 merge하지 않는다.
- 수행한 Jira 작업: TMI-97을 읽기 전용 조회했다. 댓글·상태·설명·필드·다른 Jira를 변경하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: TMI-97은 `해야 할 일`, Resolution 없음 상태를 유지한다.
- 승인 여부: 사용자는 구현할 내용에 대한 설명을 요청했다. 코드 구현, Jira mutation, Git branch 생성·commit·push는 요청하지 않아 수행하지 않았다.
- 실행한 테스트와 결과: 코드 변경 없는 Jira·문서·코드 분석이므로 Gradle 테스트를 재실행하지 않았다. 현재 develop 통합 기준의 직전 `./gradlew clean test`는 전체 457개 테스트 성공이다. 종료 전 `git diff --check`, marker 단일 존재와 WORKLOG EOF append를 확인한다.
- 유지한 계약: 실제 userId는 Guest JWT `sub`의 canonical UUID를 그대로 사용하고 Request Body userId를 받지 않는다. raw Firebase Token·phone·fingerprint와 Access/Refresh Token을 저장·로그하지 않으며 Refresh Token은 hash-only로 유지한다. Identity 밖의 Learning Core·TrialClaim·UserEntitlement 코드를 추가하지 않고 실제 merge는 Stage 7로 분리한다.
- 결정사항: Guest 승격은 새 User 생성이 아니라 기존 User의 in-place accountType 전이다. 계획에 이미 정의된 `guest/prepare`, `guest/upgrade`, `auth-methods/sync` 보호 API를 기준으로 설명했으며, Firebase link와 Mongo Transaction이 분산돼 있으므로 finalize 재시도와 중단 enrollment cleanup을 correctness 계약에 포함한다.
- 위험 요소: 현재 User에는 승격 메서드가 없고 RefreshSession 폐기 사유에도 Guest 승격 전용 값이 없으며 Controller·Security에는 세 보호 API가 없다. 같은 Guest의 동시 upgrade, identity unique 충돌, Session 전체 폐기와 신규 Session 저장, attempt CAS를 한 Transaction에서 검증해야 한다. Mongo rollback은 Firebase provider/phone link를 되돌리지 않으므로 중단 가입 resume·cleanup이 필요하다.
- 다음 작업: 구현을 요청받으면 먼저 TMI-97을 다시 읽고 feature branch를 준비한 뒤 도메인 전이·오류/응답 계약, prepare, upgrade Transaction, auth-method sync, Controller/Security/OpenAPI, rollback·동시성·민감정보 테스트 순으로 진행한다.

## 2026-08-18 — TMI-97 Guest MEMBER 승격·인증수단 동기화 구현 완료

<!-- codex-turn:01a01361-6cde-76b3-8d76-efe11f3766f3 -->

- 날짜: 2026-08-18
- 브랜치: `feat/TMI-97-guest-member-promotion` (기준 HEAD `24275a5`, Codex commit·push 미수행)
- Jira: TMI-97
- 작업 목표: authenticated Guest의 canonical UUID를 유지한 Firebase MEMBER 승격과 기존 MEMBER의 Firebase 인증수단 동기화를 Stage 6 계약대로 구현하고 원자성·소유권·보안 경계를 테스트한다.
- 변경 파일: `README.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`; auth 오류·Session·Firebase Controller/application/configuration/DTO; User·UserConsents·User repository CAS; 관련 domain/service/Transaction/Controller/Security/OpenAPI/configuration 테스트를 변경하거나 추가했다. WORKLOG 과거 기록은 수정하거나 삭제하지 않고 이 항목만 파일 끝에 추가했다.
- 구현 내용: Bearer 보호 API `POST /api/v1/auth/firebase/guest/prepare`, `POST /api/v1/auth/firebase/guest/upgrade`, `POST /api/v1/auth/firebase/auth-methods/sync`를 추가했다. 외부 Body에서 userId를 받지 않고 인증된 JWT `sub`로 User를 조회하며 Firebase 기능 비활성 시 세 API도 안정적인 unavailable 오류를 반환한다.
- 구현 내용: prepare는 ACTIVE GUEST와 phone link 전 fresh Firebase proof를 `GUEST_USER` enrollment로 묶어 생성·재사용한다. current owner는 `ALREADY_LINKED`, 다른 ACTIVE MEMBER owner는 `MERGE_REQUIRED`로 응답하고 attempt를 만들지 않는다. Firebase UID와 SocialIdentity가 current Guest와 다른 MEMBER에 섞여 귀속된 경우도 mutation 없는 `MERGE_REQUIRED`로 분류한다.
- 구현 내용: upgrade는 attempt binding·fresh UID·verified phone·필수 동의·identity ownership을 재검증하고 기존 Guest User를 같은 UUID의 `FEDERATED MEMBER`로 CAS 승격한다. guest installation credential 제거, FirebaseIdentity, PhoneIdentity/alias와 eligibility revision/outbox, SocialIdentity, 기존 Session `GUEST_UPGRADED` 폐기, 신규 RefreshSession, attempt conditional consume을 하나의 Mongo Transaction에 포함하고 성공 뒤에만 Access credential을 발급한다.
- 구현 내용: auth-method sync는 ACTIVE MEMBER의 JWT User와 기존 FirebaseIdentity 및 fresh Firebase UID를 대조하고 누락 SocialIdentity만 Transaction으로 멱등 저장한다. 다른 User가 provider subject를 소유하면 자동 merge하지 않고 `SOCIAL_IDENTITY_CONFLICT`로 거절한다. 실제 Guest source/target merge와 UserMerged outbox는 Stage 7 범위로 유지했다.
- 실행한 테스트와 결과: 구현 중 `./gradlew compileJava`, `./gradlew compileTestJava`가 성공했다. 신규 대상 테스트에서 BSON Query 직렬화 assertion과 nested Transaction 모의 harness를 수정한 뒤 service·Transaction·Controller·Security·OpenAPI·configuration·Firebase verifier 대상 테스트가 성공했다. 최종 `./gradlew clean test`는 BUILD SUCCESSFUL, 전체 479개 테스트 통과였다. `git diff --check`와 신규 API/application 경계의 명시적 로그 호출 부재 검사도 성공했다.
- 유지한 계약: 실제 userId는 JWT `sub`의 UUID이며 Guest 승격 전후 동일하다. RS256/JWKS·issuer·audience 계약과 RefreshSession hash-only 저장, Firebase/provider 기본 비활성, phone identity와 eligibility fingerprint의 목적 분리, Identity와 Learning Core·외부 Entitlement 경계, 실제 merge의 Stage 7 분리를 유지했다. 민감 인증값·개인정보·인프라 접속값은 코드 로그나 작업 기록에 추가하지 않았다.
- 수행한 Jira 작업: 이 turn에서는 Jira를 조회·생성·수정·댓글·상태 전환하지 않았다. TMI-97은 `해야 할 일`, Resolution 없음으로 유지한다.
- 추가한 댓글의 목적: 작업 요약, 주요 변경 파일, 479개 테스트 성공과 staging에서 남은 Firebase/mobile·Mongo Transaction 검증 위험을 담은 종료 댓글 초안만 준비하며 자동 등록하지 않는다.
- 변경한 상태: Jira 상태를 변경하지 않았다. PR 병합을 확인하기 전 TMI-97을 Done으로 전환하지 않는다.
- 승인 여부: 사용자가 반복해서 TMI-97 구현과 계속 진행을 요청해 저장소 내부 구현·테스트·문서 갱신을 수행했다. Jira mutation과 Git commit·push는 요청하지 않았고 수행하지 않았다.
- 결정사항: Guest upgrade는 새 User 생성이나 email·phone 기반 자동 merge가 아니라 기존 UUID User의 in-place 전이다. 성공 Transaction의 첫 경계는 Guest·updatedAt CAS이며, identity/outbox/기존·신규 Session/attempt가 모두 같은 Transaction에서 완료돼야 한다. 성공 응답 유실 후 재호출은 중복 aggregate를 만들지 않고 기존 Firebase exchange로 canonical MEMBER Session을 복구하는 운영 흐름을 staging에서 확인한다.
- 위험 요소: Firebase provider/phone link는 Mongo Transaction 밖에서 먼저 완료되므로 finalize 중단 cleanup·resume 정책이 필요하다. 모의 TransactionManager 테스트는 inner phone service의 `REQUIRED` 참여와 rollback을 고정하지만 실제 replica set의 write conflict·unique race·index 상태를 대체하지 않는다. production flag 활성화 전 격리 Firebase/mobile과 staging Mongo에서 같은 UID phone link, owner 충돌, concurrent upgrade, outbox·Session·attempt rollback을 재검증해야 한다.
- 다음 작업: 사용자가 diff와 테스트 결과를 검토한 뒤 직접 commit·push하고 PR을 생성한다. PR 병합 후 별도 승인을 받아 Jira 종료 댓글과 상태 전환을 수행하며, 그 전에는 TMI-97 상태를 유지한다.

## 2026-08-18 — PR #27 병합 확인 및 Jira TMI-97 완료 처리

<!-- codex-turn:01a01374-3d75-7d93-9f5e-db564831e4b4 -->

- 날짜: 2026-08-18
- 브랜치: `develop` (`5801868`, `origin/develop`과 일치, Codex commit·push 미수행)
- Jira: TMI-97
- 작업 목표: Stage 6 구현 PR의 `develop` 병합을 확인한 뒤 승인된 종료 댓글과 완료 전환만 적용해 TMI-97을 안전하게 종료한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드와 테스트는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 로컬 Git과 GitHub CLI 읽기 조회로 PR #27이 2026-08-18 merge commit `5801868`로 `develop`에 병합됐고 HEAD와 `origin/develop`이 해당 commit으로 일치함을 확인했다.
- 수행한 Jira 작업: Atlassian 공식 MCP로 TMI-97의 사전 상태 `해야 할 일`, Resolution 없음, 댓글 없음과 완료 transition ID `41` 사용 가능 여부를 읽기 전용 확인했다. 이후 승인된 종료 댓글 ID `10008`을 등록하고 transition ID `41`만 적용했으며 다른 필드·이슈는 변경하지 않았다.
- 추가한 댓글의 목적: Guest UUID 유지 승격, identity·eligibility outbox·Session·enrollment Transaction, MEMBER 인증수단 동기화 구현과 주요 변경 범위, 전체 479개 테스트 성공, 실제 Firebase/mobile·Mongo replica set staging 재검증 위험을 인수인계하기 위해 등록했다.
- 변경한 상태: TMI-97을 `해야 할 일`에서 `완료`로 전환했다. 후속 조회에서 status ID `10003`의 `완료`와 Resolution `완료`, 댓글 ID `10008` 존재를 확인했다.
- 승인 여부: 앞선 응답에서 종료 댓글 초안과 완료 transition ID `41`, PR 병합 gate를 공개했고 사용자가 PR #27 병합 뒤 다시 `지라 닫아줘`라고 명시적으로 요청해 댓글 등록과 상태 전환을 승인했다.
- 실행한 테스트와 결과: Jira·GitHub 상태 확인과 문서 기록만 수행해 Gradle 테스트를 재실행하지 않았다. 병합 전 최종 `./gradlew clean test`는 전체 479개 성공이고 `git diff --check`도 성공했다. 이번 turn에서는 PR merge commit, Jira 댓글·상태·Resolution을 후속 조회로 검증했다.
- 유지한 계약: PR 병합 확인 전 Done 전환 금지와 Jira mutation 사전 공개·승인 규칙을 지켰다. 실제 userId·JWT·Firebase·Session·Identity 경계는 변경하지 않았고 민감 인증값·개인정보·인프라 접속값을 Jira나 작업 기록에 추가하지 않았다.
- 결정사항: TMI-97 Stage 6 구현과 행정적 종료가 모두 완료됐다. 실제 Guest merge·UserMergedOutbox는 Stage 7의 별도 Jira로 유지하고 production Firebase flag는 격리 Firebase/mobile·staging Mongo E2E 전에 활성화하지 않는다.
- 위험 요소: 실제 Firebase provider/phone link는 Mongo Transaction 밖에서 진행되므로 중단 enrollment cleanup·resume 정책과 concurrent upgrade·unique/write conflict·outbox·Session·attempt rollback은 staging에서 계속 검증해야 한다.
- 다음 작업: 다음 Identity 범위를 Stage 7 Guest merge·UserMergedOutbox로 검토하되, 새 Jira 생성 전 제목·설명·완료 조건·제외 범위를 사용자에게 공개하고 승인을 받는다.

## 2026-08-18 — Stage 7 Guest merge 신규 Jira Payload 준비

<!-- codex-turn:01a01378-8acc-7191-9527-79d2046e28be -->

- 날짜: 2026-08-18
- 브랜치: `develop` (`5801868`, `origin/develop`과 일치, Codex commit·push 미수행)
- Jira: Stage 7 신규 이슈 초안, 아직 미생성
- 작업 목표: TMI-97 다음 Identity 작업을 설명하고 중복 없는 Stage 7 Guest merge Jira의 정확한 생성 Payload를 사용자 승인 전에 준비한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드와 테스트는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: ADR-001과 social login 계획의 Guest merge proof, source/target 불변식, `MERGED` tombstone, source Session 폐기, target Session, `UserMergedOutbox`, source token gate, lease·retry·dead-letter publisher와 feature flag gate를 현재 TMI-97 코드 기반과 대조했다.
- 구현 내용: 다음 Jira는 Identity가 소유하는 보호 merge API와 이중 proof, source User·Session·outbox·target Session Transaction, source JWT 거절, UserMerged publisher까지만 포함하도록 정리했다. Learning Core의 멱등 consumer와 실제 학습 데이터 이전, Entitlement/Billing, MEMBER→MEMBER merge, phone·email 기반 merge, Firebase credential 이전과 production 활성화는 제외한다.
- 수행한 Jira 작업: Atlassian 공식 MCP로 Stage 7 Guest merge·UserMergedOutbox 관련 이슈를 검색했으나 중복 결과가 없었다. TMI 프로젝트의 `작업` 유형 ID `10003`, High 우선순위 ID `2`, 생성 필드를 읽기 전용 확인했으며 이슈 생성·수정·댓글·상태 전환은 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다. 신규 Jira는 아직 존재하지 않는다.
- 승인 여부: 사용자는 다음 Jira 생성과 작업 설명을 요청했다. AGENTS.md의 Jira mutation 사전 공개·승인 규칙에 따라 이번 turn에서는 프로젝트·유형·제목·우선순위·설명·완료 조건·제외 범위·미설정 필드를 먼저 공개하고 별도 승인을 기다린다.
- 실행한 테스트와 결과: Jira·문서·코드 읽기 분석만 수행해 Gradle 테스트를 재실행하지 않았다. 직전 최종 `./gradlew clean test`는 전체 479개 성공이며 이번 turn 종료 전 `git diff --check`, marker 단일 존재와 WORKLOG EOF append를 확인한다.
- 유지한 계약: source는 Identity JWT `sub`의 ACTIVE GUEST, target은 fresh Firebase proof와 기존 identity mapping이 가리키는 ACTIVE MEMBER로만 결정하고 외부 Body userId·email·phone으로 target을 선택하지 않는다. source JWT를 target actor로 재해석하지 않고 실제 학습 데이터 이전 코드를 Identity 저장소에 추가하지 않으며 민감 인증값·개인정보·인프라 접속값을 기록하지 않았다.
- 결정사항: 제안 이슈는 TMI `작업`, High, 기본 `해야 할 일`이며 담당자·라벨·컴포넌트·스프린트·에픽·기한·상태 전환을 설정하지 않는다. 보호 서비스 consumer 준비와 staging E2E 전에는 merge feature를 기본 비활성으로 유지한다.
- 위험 요소: outbox는 Identity Transaction과 downstream 데이터 이전 사이의 eventual consistency를 만든다. source token gate와 Learning Core consumer가 준비되기 전에 merge를 활성화하면 source·target ownership이 갈리거나 source JWT가 계속 actor로 사용될 수 있으므로 production gate를 강제해야 한다.
- 다음 작업: 사용자가 공개된 Payload를 동일 내용으로 승인하면 Atlassian 공식 MCP로 신규 Jira 하나만 생성하고, 생성된 키·상태·우선순위·본문과 미설정 필드를 후속 조회한다.

## 2026-08-18 — Stage 7 Jira TMI-98 생성

<!-- codex-turn:01a0137e-bac8-78a1-a882-3932bda6871d -->

- 날짜: 2026-08-18
- 브랜치: `develop` (`5801868`, `origin/develop`과 일치, Codex commit·push 미수행)
- Jira: TMI-98
- 작업 목표: 사용자에게 공개한 Stage 7 Guest canonical merge·UserMerged outbox Jira Payload를 승인 내용 그대로 생성하고 저장된 필드를 검증한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드와 테스트는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 신규 이슈는 현재 Guest JWT source와 기존 MEMBER owner의 fresh Firebase proof target 이중 검증, source `MERGED` tombstone·Guest credential 제거·source Session 폐기·target Session·`UserMergedOutbox` Transaction, source JWT 거절, lease·retry·dead-letter publisher와 기본 비활성 gate를 범위로 한다.
- 구현 내용: Learning Core의 멱등 consumer·실제 학습 데이터 이전, Entitlement/Billing, MEMBER→MEMBER 및 email·phone 기반 merge, Firebase credential 이전, Kakao·Apple lifecycle과 production 활성화는 제외 범위로 저장했다.
- 수행한 Jira 작업: Atlassian 공식 MCP로 TMI `작업` 이슈 `TMI-98`을 High 우선순위와 기본 상태 `해야 할 일`로 생성했다. 생성 후 제목·설명·완료 조건·제외 범위·유형·우선순위·상태를 후속 조회했으며 다른 기존 이슈는 변경하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: 신규 TMI-98은 기본 상태 `해야 할 일`, Resolution 없음이다. 별도 상태 전환을 적용하지 않았다.
- 승인 여부: 직전 응답에서 프로젝트·유형·제목·우선순위·설명·완료 조건·제외 범위와 미설정 필드를 모두 공개했고 사용자가 `지라 생성해줘`라고 동일 내용의 생성을 명시적으로 승인했다.
- 실행한 테스트와 결과: Jira 생성·후속 조회와 문서 기록만 수행해 Gradle 테스트를 재실행하지 않았다. 직전 최종 `./gradlew clean test`는 전체 479개 성공이다. 종료 전 `git diff --check`, marker 단일 존재와 WORKLOG EOF append를 확인한다.
- 유지한 계약: source는 JWT `sub`의 ACTIVE GUEST, target은 fresh Firebase proof와 기존 identity mapping의 ACTIVE MEMBER로만 결정하고 외부 Body userId·email·phone으로 target을 선택하지 않는다. source JWT를 target actor로 해석하지 않으며 Learning Core 구현을 Identity 범위에 포함하지 않고 민감 인증값·개인정보·인프라 접속값을 Jira나 기록에 추가하지 않았다.
- 결정사항: TMI-98은 TMI `작업`, High로 생성했고 담당자·라벨·컴포넌트·스프린트·에픽·기한·댓글·상태 전환은 설정하지 않았다. 모든 downstream consumer와 staging E2E가 준비되기 전에는 merge feature와 publisher를 기본 비활성으로 유지한다.
- 위험 요소: Identity merge Transaction과 downstream 학습 데이터 이전 사이에는 eventual consistency가 존재한다. source token gate와 Learning Core consumer가 준비되기 전에 활성화하면 source·target ownership 분리 또는 source actor 재사용 위험이 있으므로 기능 gate와 staging 통합 검증이 필요하다.
- 다음 작업: TMI-98을 구현하기 전에 이슈 본문을 다시 읽고 `feat/TMI-98-guest-canonical-merge` 브랜치를 준비한 뒤 User merge domain·Transaction·source token gate·outbox/publisher·API/Security/OpenAPI·rollback/동시성 테스트 순으로 진행한다.

## 2026-08-20 — Guest JWT sub를 merge source로 사용한다는 의미 설명

<!-- codex-turn:01a01cc2-8e7d-7bd1-a8e5-755c309c8621 -->

- 날짜: 2026-08-20
- Jira: TMI-98
- 브랜치: `feat/TMI-98-guest-canonical-merge`
- 작업 목표: “현재 Guest JWT의 sub를 source로 사용한다”는 Stage 7 canonical merge 문구에서 source와 JWT sub의 관계를 쉽게 설명한다.
- 변경 파일: 설명 기록을 위해 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: JWT `sub`는 검증된 현재 Guest의 canonical UUID userId이며, merge 요청에서 이 값을 `sourceUserId`로 삼는다는 뜻으로 정리했다. source는 병합으로 소유권을 넘기고 폐기·MERGED 상태가 될 출발 Guest이고 target은 Firebase proof와 기존 identity mapping으로 결정되는 최종 ACTIVE MEMBER다.
- 구현 내용: 클라이언트가 Body로 source userId를 보내 선택하는 것이 아니며, JWT 자체나 `sub` 문자열을 다른 User에 복사한다는 뜻도 아니다. JWT를 인증한 서버가 현재 요청자의 Guest User를 안전하게 특정하는 입력으로 사용한다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 용어 설명만 요청했으며 외부 시스템이나 애플리케이션 상태 변경을 승인하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 설명 작업이므로 Gradle 테스트를 실행하지 않았다. 종료 전 `git diff --check`와 marker 단일 존재를 확인한다.
- 유지한 계약: source는 검증된 Guest JWT `sub`의 ACTIVE GUEST로만 결정하고 외부 Body userId·email·phone을 신뢰하지 않는다. target은 별도 Firebase proof와 identity mapping으로 정하며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: source는 “데이터와 소유권이 이동하는 출발 계정”, target은 “병합 후 남는 canonical 계정”이라는 용어로 설명한다.
- 위험 요소: source JWT를 target 회원의 인증 수단으로 오해하면 병합 전 Guest token으로 target 권한을 행사하게 만들 수 있다. merge 완료 후 source token과 session을 폐기하고 target token은 별도 정상 절차로 발급해야 한다.
- 다음 작업: TMI-98 구현 시 source userId가 검증된 JWT subject에서만 유도되고 request DTO에 임의 userId가 추가되지 않는지 API·Security·Transaction 테스트로 고정한다.

## 2026-08-20 — Jira TMI-98 구현 범위 설명

<!-- codex-turn:01a01cc5-4c73-73c3-9afe-1d6cf1490633 -->

- 날짜: 2026-08-20
- Jira: TMI-98
- 브랜치: `feat/TMI-98-guest-canonical-merge`
- 작업 목표: Stage 7 Guest canonical merge와 UserMerged outbox에서 구현할 기능, 처리 순서, TMI-97과의 차이 및 제외 범위를 쉽게 설명한다.
- 변경 파일: 설명 기록을 위해 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: TMI-97은 외부 identity owner가 없는 Guest를 같은 UUID의 MEMBER로 in-place 승격하지만, TMI-98은 Firebase identity가 이미 다른 ACTIVE MEMBER에 귀속된 MERGE_REQUIRED 상황에서 Guest source와 기존 MEMBER target 두 User를 하나의 canonical target으로 합치는 범위임을 구분했다.
- 구현 내용: 보호 merge API가 source를 검증된 Guest JWT sub로, target을 fresh Firebase proof와 기존 identity mapping으로 결정한다. 성공 Transaction은 source를 MERGED tombstone으로 전환하고 Guest credential과 source Session을 정리하며 target Session과 UserMergedOutbox를 함께 저장한다. 성공 뒤 source JWT는 target actor로 재사용할 수 없게 차단한다.
- 구현 내용: outbox publisher는 lease·retry·dead-letter와 기본 비활성 feature gate를 사용해 UserMerged event를 downstream에 전달한다. Identity는 source→target 매핑 사실만 발행하고 실제 시험·결과 등 Learning Core 데이터는 수정하지 않는다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다. 저장소에 이미 기록된 TMI-98 생성 결과와 범위를 읽어 설명했다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: TMI-98 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 작업 내용 설명만 요청했으며 구현, Jira mutation 또는 외부 시스템 변경을 승인하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 설명 작업이므로 Gradle 테스트를 실행하지 않았다. 종료 전 `git diff --check`와 marker 단일 존재를 확인한다.
- 유지한 계약: source는 Guest JWT sub의 ACTIVE GUEST, target은 fresh Firebase proof와 identity mapping의 ACTIVE MEMBER로만 결정한다. 외부 Body userId·email·phone으로 merge 대상을 고르지 않고 민감 인증값·개인정보·인프라 접속값을 기록하지 않았다.
- 결정사항: User 상태·Session·outbox의 원자적 변경과 source token gate는 Identity 범위이며, Learning Core 멱등 consumer·실제 학습 데이터 이전, Entitlement/Billing, MEMBER→MEMBER 및 email·phone 기반 merge는 별도 범위다.
- 위험 요소: Identity Transaction commit과 downstream 데이터 이전 사이에는 eventual consistency가 있다. consumer와 source token 차단이 준비되기 전에 기능을 활성화하면 두 userId에 데이터가 갈리거나 이전 Guest JWT가 잘못 사용될 수 있다.
- 다음 작업: 구현 시작 전에 Atlassian 공식 MCP로 TMI-98 본문과 완료 조건을 다시 읽고 User merge domain·Transaction·source token gate·outbox/publisher·API/Security/OpenAPI·rollback/동시성 테스트 순으로 구현한다.

## 2026-08-20 — Merge source JWT sub 조회와 차단 경계 설명

<!-- codex-turn:01a01ccb-8915-7de3-bf6c-da3adf2f897e -->

- 날짜: 2026-08-20
- Jira: TMI-98
- 브랜치: `feat/TMI-98-guest-canonical-merge`
- 작업 목표: Guest JWT `sub=sourceUserId`를 조회하는 것이 보안상 잘못된 것인지 질문에 답하고 merge 전 식별과 merge 후 token 차단을 구분한다.
- 변경 파일: 설명 기록을 위해 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: merge 전에는 인증된 Guest 본인을 source로 고정해야 하므로 서버가 서명·issuer·audience·만료를 검증한 JWT sub를 내부 sourceUserId로 읽는 것이 올바르다. 외부 Body·Query·Path에서 sourceUserId를 받거나 응답에 별도 merge 식별자로 노출하는 것은 금지한다.
- 구현 내용: 현재 `JwtCurrentUserProvider`는 JWT principal에서 canonical UUID sub만 반환하고 DB User 상태를 검사하지 않는다. 따라서 TMI-98에서는 source를 ACTIVE GUEST로 확인한 merge 요청만 허용하고, commit 후 같은 sub가 MERGED 상태이면 `ACCOUNT_MERGED_TOKEN_REJECTED`로 거절하는 일관된 source token gate가 필요하다.
- 구현 내용: source JWT를 target userId로 치환하거나 target actor alias로 인정하면 안 된다. Access JWT가 암호학적으로 아직 유효해도 source Session 폐기와 User 상태 gate로 Identity 사용을 막고, stateless 검증을 하는 downstream은 UserMerged consumer가 source deny marker와 ownership migration을 함께 저장해야 한다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: TMI-98 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 설계 의미와 보안성 설명만 요청했으며 구현이나 외부 시스템 변경을 승인하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 설명 작업이므로 Gradle 테스트를 실행하지 않았다. 현재 provider와 User status 검사 호출부를 정적으로 확인했으며 종료 전 `git diff --check`와 marker 단일 존재를 검증한다.
- 유지한 계약: JWT sub에는 실제 canonical userId가 들어가지만 userId 비밀성에 의존해 권한을 보호하지 않는다. 클라이언트가 보낸 userId를 신뢰하지 않고 source MERGED token을 target 권한으로 승격하지 않으며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: “조회 금지” 대상은 외부 입력으로 임의 source를 선택하거나 merge 후 source를 정상 actor로 계속 사용하는 행위다. 검증된 JWT에서 source를 식별하고 DB 상태를 조회해 거절하는 것은 오히려 필요한 보안 절차다.
- 위험 요소: Security filter가 JWT 서명만 검증하고 endpoint service가 User 상태를 확인하지 않으면 MERGED source token이 만료 전까지 일부 API를 호출할 수 있다. Learning Core는 매 요청 Identity introspection을 하지 않으므로 consumer와 deny marker가 준비되기 전에 merge 기능을 활성화하면 안 된다.
- 다음 작업: TMI-98 구현에서 중앙 source token gate 또는 모든 보호 경로에 적용 가능한 상태 검사를 설계하고, merge 전 ACTIVE GUEST 허용·merge 후 source 거절·target alias 금지·downstream event 처리 전 feature disabled를 테스트로 고정한다.

## 2026-08-20 — JWT sub의 userId 가시성과 비밀성 설명

- 날짜: 2026-08-20
- Jira: TMI-98
- 브랜치: `feat/TMI-98-guest-canonical-merge`
- 작업 목표: JWT를 가진 클라이언트가 payload의 `sub`를 통해 실제 userId를 알 수 있어도 되는지 현재 계약과 대안 설계를 구분해 설명한다.
- 변경 파일: 설명 기록을 위해 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 현재 Access Token은 RS256 서명 JWS이며 암호화된 JWE가 아니므로 token 보유자가 payload를 Base64URL decode해 `sub`의 UUID userId를 읽을 수 있다. 서명은 내용 위조를 막지만 내용 열람을 막지 않는다.
- 구현 내용: 현재 계약은 UUID userId를 secret이나 credential로 취급하지 않고, 실제 권한은 JWT 서명·issuer·audience·만료와 User 상태·resource ownership 검사로 보호한다. userId를 안다는 사실만으로 다른 사용자의 리소스에 접근할 수 있다면 JWT 노출 문제가 아니라 IDOR authorization 결함이다.
- 구현 내용: 내부 userId 비공개가 요구사항이면 JWT payload에 실제 userId를 넣지 않고 외부용 opaque 또는 pairwise subject를 넣어 서버 내부 mapping으로 canonical userId를 찾는 방식이 적합하다. JWE도 내용을 숨길 수 있지만 모든 소비 서비스의 복호화 key 관리가 필요하고 식별자 분리 문제를 단독으로 해결하지 않는다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: TMI-98 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 보안 설계 설명만 요청했으며 JWT 계약 변경이나 구현을 승인하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 설명 작업이므로 Gradle 테스트를 실행하지 않았다. 종료 전 `git diff --check`를 확인한다.
- 유지한 계약: 현재 실제 UUID userId를 JWT sub에 넣는 저장소 계약을 임의로 변경하지 않았다. Token 원문이나 사용자 식별값 예시를 기록하지 않았으며 Secret·Token·Password·실제 Key·전체 MongoDB URI를 추가하지 않았다.
- 결정사항: 현재 설계에서는 userId 가시성을 허용하고 authorization으로 보호한다. userId 기밀성이 실제 요구사항이면 TMI-98에 즉흥적으로 섞지 않고 Identity·Learning Core·RefreshSession·event 계약을 포함한 별도 migration으로 결정한다.
- 위험 요소: UUID가 예측하기 어렵더라도 이를 authorization 수단처럼 신뢰하면 안 된다. JWT payload에 개인정보나 provider subject를 추가하면 token 보유자와 로그·도구에서 노출될 수 있으므로 최소 claim 원칙을 유지해야 한다.
- 다음 작업: 제품·보안이 내부 userId 비공개를 요구하는지 먼저 확정하고, 필요하면 opaque/pairwise subject mapping, token migration, downstream consumer 변경과 기존 token 전환 계획을 별도 ADR·Jira로 설계한다.

## 2026-08-20 — JWT sub 가시성 설명 Hook 기록 보완

<!-- codex-turn:01a01ccd-a757-7592-8c0a-9609945cd2b2 -->

- 날짜: 2026-08-20
- Jira: TMI-98
- 브랜치: `feat/TMI-98-guest-canonical-merge`
- 작업 목표: 현재 JWT `sub`에 실제 내부 userId를 넣는 계약의 가시성과 대안 설명 결과를 지정된 turn marker로 append-only 기록한다.
- 변경 파일: 이번 Hook 보완으로 `docs/codex/WORKLOG.md`를 append하고, 직전 설명에서 최신화한 `docs/codex/CURRENT_STATE.md`의 상태를 유지했다. 애플리케이션 코드는 변경하지 않았으며 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: RS256 Access Token은 암호화된 JWE가 아니라 서명된 JWS이므로 token 보유자가 payload의 UUID `sub`를 읽을 수 있음을 명확히 했다. 현재 계약은 userId를 secret credential로 보지 않고 authorization과 User 상태 검사로 보호한다.
- 구현 내용: 내부 userId 비공개가 실제 요구사항이면 JWT sub에 외부용 opaque 또는 pairwise subject를 넣고 서버 내부에서 canonical userId로 매핑해야 한다. 이 변경은 TMI-98에 국소 적용할 수 없으며 Identity·Learning Core·RefreshSession·event와 기존 token migration을 함께 설계해야 한다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: TMI-98 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 JWT userId 가시성에 대한 설명을 요청했으며 계약 변경이나 구현을 승인하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 설명·문서 기록이므로 Gradle 테스트를 실행하지 않았다. `git diff --check`는 성공했다.
- 유지한 계약: 현재 실제 UUID userId를 JWT sub에 넣는 계약을 임의로 변경하지 않았다. Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: userId를 반드시 숨겨야 하는지 제품·보안 요구를 먼저 확정하고, 필요할 때 별도 ADR·Jira로 subject mapping migration을 설계한다.
- 위험 요소: JWT payload 가시성을 암호화로 오해하거나 UUID 난수성을 authorization으로 사용하면 안 된다. claim에는 개인정보·provider subject를 추가하지 않고 최소화해야 한다.
- 다음 작업: TMI-98 착수 전에 내부 userId 노출 허용 여부를 결정한다. 비공개 요구가 확정되면 TMI-98과 분리된 subject 계약 migration을 먼저 계획한다.

## 2026-08-20 — 현재 Access JWT payload claim 확인

- 날짜: 2026-08-20
- Jira: TMI-98
- 브랜치: `feat/TMI-98-guest-canonical-merge`
- 작업 목표: 내부 UUID userId의 JWT sub 노출을 허용한다는 사용자 결정 뒤 현재 Access Token payload와 header에 실제로 포함되는 값을 코드 기준으로 설명한다.
- 변경 파일: 설명 기록을 위해 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: `JwtAccessTokenIssuer`가 payload에 UUID `sub`, 설정 `iss`, 단일 원소 배열 `aud`, 초 단위 `iat`·`exp`, 매 발급마다 새 UUID `jti`, 정렬된 공백 구분 문자열 `scope`를 넣음을 확인했다. 현재 모든 발급 호출은 빈 scope set을 전달해 기본 `learning:read learning:write`를 사용한다.
- 구현 내용: JWS header에는 `alg=RS256`, 설정된 `kid`, `typ=JWT`가 들어가며 이는 payload claim과 구분된다. payload에는 email·normalizedEmail·nickname·accountType·provider·Firebase UID·phone·consent·installationId·Refresh Token 정보가 없다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: TMI-98 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 내부 UUID userId를 JWT sub에서 숨기지 않아도 된다고 결정하고 현재 payload 설명을 요청했다. 코드나 계약 변경은 요청하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 정적 확인 작업이므로 Gradle 테스트를 실행하지 않았다. JWT issuer·설정·모든 AccessTokenIssuer 호출부를 읽었고 종료 전 `git diff --check`를 확인한다.
- 유지한 계약: 실제 userId는 JWT sub, audience는 `tosunsaeng-learning-core`, 서명은 RS256이며 실제 token 원문·실제 key·사용자 개인정보를 기록하지 않았다. Secret·Token·Password·전체 MongoDB URI도 추가하지 않았다.
- 결정사항: 현재 최소 claim 구성을 유지하고 userId 비공개용 opaque subject migration은 진행하지 않는다.
- 위험 요소: payload는 token 보유자에게 보이므로 향후 개인정보나 provider subject를 claim에 추가하면 안 된다. `scope`는 현재 endpoint별 세부 인가가 완전히 강제되는 권한 모델과 동일하다고 과대 해석하면 안 된다.
- 다음 작업: TMI-98 구현에서도 target userId·source 상태·provider identity·merge metadata를 Access JWT에 추가하지 않고 기존 claim 계약을 유지한다.

## 2026-08-20 — Access JWT payload 설명 Hook 기록 보완

<!-- codex-turn:01a01cd0-811d-7483-a099-2cd2fce14718 -->

- 날짜: 2026-08-20
- Jira: TMI-98
- 브랜치: `feat/TMI-98-guest-canonical-merge`
- 작업 목표: 현재 Access JWT payload와 header 구성 설명을 지정된 turn marker로 append-only 기록한다.
- 변경 파일: 이번 Hook 보완으로 `docs/codex/WORKLOG.md`를 append하고 직전 설명에서 갱신한 `docs/codex/CURRENT_STATE.md`를 유지했다. 애플리케이션 코드는 변경하지 않았으며 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: payload claim은 실제 UUID `sub`, 설정 `iss`, 단일 audience 배열 `aud`, `iat`, `exp`, token별 UUID `jti`, 공백 구분 `scope`로 확인했다. 현재 기본 scope는 `learning:read learning:write`다.
- 구현 내용: JWS header의 `alg=RS256`, `kid`, `typ=JWT`를 payload와 구분하고, 이메일·닉네임·계정 유형·provider·Firebase UID·전화번호·동의·Refresh Token·merge metadata는 JWT payload에 포함되지 않음을 확인했다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: TMI-98 상태나 Resolution을 변경하지 않았다.
- 승인 여부: 사용자는 UUID userId의 sub 노출을 허용하고 현재 payload 설명을 요청했으며 코드 변경은 승인하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없는 정적 확인·문서 기록이므로 Gradle 테스트를 실행하지 않았다. 직전 `git diff --check`는 성공했다.
- 유지한 계약: 실제 userId를 JWT sub에 두고 RS256·kid·issuer·audience·scope 계약을 유지했다. 실제 Token·Key·Secret·Password·전체 MongoDB URI·사용자 개인정보를 기록하지 않았다.
- 결정사항: 현재 최소 claim 구성을 유지하며 TMI-98 merge 상태나 source/target 정보를 JWT claim에 추가하지 않는다.
- 위험 요소: JWT payload는 token 보유자에게 보이므로 향후 개인정보나 provider subject를 추가하지 않아야 한다.
- 다음 작업: TMI-98 구현 시 기존 Access JWT claim 계약을 회귀 테스트로 유지하고 MERGED 차단은 DB User 상태 기반 source token gate에서 처리한다.

## 2026-08-20 — Jira TMI-98 Guest canonical merge 및 UserMerged outbox 구현

<!-- codex-turn:01a01cd1-5382-7d60-8e95-b9fca318189d -->

- 날짜: 2026-08-20
- Jira: TMI-98
- 브랜치: `feat/TMI-98-guest-canonical-merge`
- 작업 목표: 검증된 Guest JWT `sub`의 ACTIVE GUEST를 source로, fresh Firebase proof의 기존 identity owner인 ACTIVE MEMBER를 target으로 확정해 canonical merge를 수행하고 `UserMerged` v1 outbox와 source token gate를 구현한다.
- 변경 파일: User 상태·entity·custom repository, RefreshSession 폐기 사유, Firebase merge request/use case/service/target resolver/Transaction/controller/configuration, `UserMergedOutbox` entity·enum·repository, publisher application·HTTP adapter·설정·scheduler, JWT current-user gate, application/test 설정과 관련 도메인·repository·service·Transaction·controller·Security/OpenAPI·publisher·configuration 테스트를 추가·갱신했다. `docs/codex/CURRENT_STATE.md`를 최신화하고 이 WORKLOG 항목은 EOF에 append했으며 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: `POST /api/v1/auth/firebase/guest/merge`는 write-only `firebaseIdToken`만 받고 source/target userId를 외부에서 받지 않는다. source는 `CurrentUserProvider`가 검증된 JWT `sub`에서 가져오며 ACTIVE GUEST가 아니면 거절한다.
- 구현 내용: fresh Firebase proof의 FirebaseIdentity와 linked SocialIdentity owner를 모아 정확히 하나의 다른 ACTIVE MEMBER일 때만 target으로 인정한다. owner 없음·source 자신·mixed owner·비활성·Guest target을 거절하고 email·phone·nickname으로 target을 추론하거나 credential/identity mapping을 이전하지 않는다.
- 구현 내용: source User를 CAS로 `MERGED` tombstone으로 전환해 `mergedIntoUserId`·`mergedAt`을 기록하고 Guest installation credential을 제거한다. 모든 source RefreshSession을 `GUEST_MERGED`로 폐기하고 target RefreshSession과 schema v1 `UserMergedOutbox(eventId, schemaVersion, sourceUserId, targetUserId, occurredAt)`를 같은 Mongo Transaction에 저장한다. target Access Token은 Transaction 반환 뒤에만 발급한다.
- 구현 내용: `JwtCurrentUserProvider`는 JWT `sub`가 DB의 MERGED User인지 확인해 `ACCOUNT_MERGED_TOKEN_REJECTED`로 차단하고 source를 target actor로 alias하지 않는다. Access JWT claim에는 target userId나 merge metadata를 추가하지 않았다.
- 구현 내용: 별도 `UserMerged` publisher가 원자적 lease claim, expired lease 회수, 지수 backoff+jitter, 최대 시도, dead-letter, replay, published TTL cleanup과 at-least-once 전달을 제공한다. wire payload는 v1 다섯 필드만 포함하고 userId를 로그·metric tag에 넣지 않는다. `GUEST_MERGE_ENABLED`와 `USER_MERGED_PUBLISHER_ENABLED`는 기본 false다.
- 실행한 테스트와 결과: `./gradlew compileJava` 성공, 관련 merge·controller·security·publisher·configuration targeted test 성공, 최종 `./gradlew clean test` 전체 496개 성공(실패 0, 오류 0), `git diff --check` 성공. 신규 merge 경계의 명시적 logger/System 출력 부재와 credential·provider subject·phone의 로그/metric 노출 부재를 정적으로 확인했다.
- 유지한 계약: 실제 UUID userId는 JWT `sub`에 유지하고 RS256·kid·issuer·`tosunsaeng-learning-core` audience·기존 최소 claim을 변경하지 않았다. 클라이언트 Body userId를 신뢰하지 않고 Learning Core 시험·결과 데이터나 Python AI `user_id` 계약을 수정하지 않았다. Refresh Token 원문·Firebase credential·provider subject·phone·Secret·Password·실제 Key·전체 MongoDB URI를 DB outbox·로그·metric·문서에 추가하지 않았다.
- 결정사항: TMI-97 same-UUID in-place 승격과 TMI-98 source→target canonical merge를 분리한다. Identity는 source tombstone·Session·target Session·outbox와 Identity 보호 API의 MERGED gate까지만 소유하며, Learning Core consumer·source deny marker·실제 학습 데이터 이전은 별도 범위다. downstream readiness 전 두 feature flag를 켜지 않는다.
- 위험 요소: 실제 Mongo replica set의 다중 collection rollback·write conflict·index/TTL 생성, publisher 다중 인스턴스 lease와 workload identity/HTTPS delivery는 staging E2E가 필요하다. Learning Core는 매 요청 Identity introspection을 하지 않으므로 consumer가 source deny marker와 migration을 원자적으로 적용하기 전 merge를 활성화하면 source JWT가 만료 전까지 downstream에서 받아들여질 수 있다.
- 수행한 Jira 작업: 구현 전 공식 Atlassian MCP 재조회는 OAuth refresh 실패 `unauthorized_client`로 완료하지 못했다. 이전에 공식 MCP로 확인해 저장된 TMI-98 설명·완료 조건을 구현 기준으로 사용했으며 Jira 생성·수정·댓글·상태 전환·삭제는 수행하지 않았다.
- 추가한 댓글의 목적: 자동 등록하지 않은 종료 댓글 초안은 “Guest merge API와 MERGED tombstone/CAS, source Session GUEST_MERGED 폐기, target Session·UserMergedOutbox Transaction, MERGED JWT gate, lease/retry/dead-letter publisher 구현 및 전체 496개 테스트 성공; 남은 위험은 Learning Core consumer와 staging Mongo/workload identity E2E”를 전달하는 것이다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다. 마지막 확인 상태는 `해야 할 일`, Resolution 없음이며 이번 turn에는 OAuth 문제로 재확인하지 못했다.
- 승인 여부: 사용자는 TMI-98 구현을 명시적으로 요청했다. Jira mutation은 요청·승인하지 않았으므로 수행하지 않았다.
- 다음 작업: 사용자가 diff를 검토해 직접 commit·push·PR을 진행한다. 별도 Learning Core Jira에서 멱등 `UserMerged` consumer·eventId inbox·source deny marker·학습 데이터 이전을 구현하고 staging E2E를 통과한 뒤에만 merge와 publisher flag 활성화를 검토한다.

## 2026-08-20 — Jira TMI-98 종료 시도 및 Atlassian 연결 차단 확인

<!-- codex-turn:01a01ce8-5717-7081-9724-23a7232df794 -->

- 날짜: 2026-08-20
- Jira: TMI-98
- 브랜치: `develop`
- 작업 목표: PR 병합 상태를 근거로 TMI-98의 현재 Jira 상태와 완료 전환을 재조회하고, 종료 댓글 및 완료 전환을 사용자 승인 뒤 적용한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`
- 구현 내용: Git 이력 기준 PR #29가 merge commit `91671ce`로 `develop`과 `origin/develop`에 반영됐고 구현 commit `b8df69f`가 포함된 상태를 유지했다. 애플리케이션 코드는 변경하지 않았다.
- 수행한 Jira 작업: Atlassian 공식 MCP의 접근 가능 리소스 조회를 시도했으나 OAuth refresh가 `unauthorized_client`로 실패해 TMI-98 조회, 댓글 등록, 상태 전환을 수행하지 못했다.
- 추가한 댓글의 목적: Guest merge API, MERGED tombstone/CAS, source Session의 `GUEST_MERGED` 폐기, target Session·`UserMergedOutbox` Transaction, MERGED JWT gate, lease/retry/dead-letter publisher, 전체 496개 테스트 성공과 남은 Learning Core consumer·staging E2E 위험을 요약하려 했으나 등록하지 않았다.
- 변경한 상태: TMI-98 상태와 Resolution을 변경하지 않았다. 마지막 확인값은 `해야 할 일`, Resolution 없음이며 이번 turn에는 연결 실패로 재검증하지 못했다.
- 승인 여부: 사용자는 TMI-98 종료를 명시적으로 요청했다. 다만 Jira 변경 전 정확한 댓글·전환을 보여주고 별도 승인을 받는 저장소 규칙을 충족하기 전에 연결이 차단됐다.
- 실행한 테스트와 결과: 코드 변경이 없어 Gradle 테스트를 새로 실행하지 않았다. 구현 당시 `./gradlew clean test` 전체 496개와 후속 `./gradlew test` 성공 기록을 확인했다. 문서 변경 후 `git diff --check`를 실행한다.
- 유지한 계약: PR 병합 확인 전 Done 전환 금지 계약을 충족했고, Secret·Token·Password·실제 Key·전체 MongoDB URI·사용자 개인정보를 Jira나 문서에 기록하지 않았다. Git commit·push를 수행하지 않았다.
- 결정사항: 이전 workflow의 완료 transition ID를 추정해 사용하지 않고 Atlassian 연결 복구 뒤 현재 가능한 transition을 반드시 재조회한다. 댓글 등록과 상태 전환은 사용자에게 정확한 변경안을 제시하고 승인받은 뒤 수행한다.
- 위험 요소: Atlassian OAuth 연결이 복구될 때까지 Jira 상태가 코드 병합 상태와 불일치한다. Learning Core consumer와 source deny marker, staging Mongo Transaction·publisher workload identity E2E는 여전히 별도 후속 범위다.
- 다음 작업: Atlassian MCP를 재연결하고 TMI-98 상태·전환을 읽기 전용으로 확인한 뒤, 종료 댓글 초안과 완료 transition을 사용자에게 제시해 승인받고 적용·재검증한다.

## 2026-08-20 — Jira TMI-98 종료 전 상태·전환 재확인

<!-- codex-turn:01a01ceb-3e6b-7ce0-9bfa-e48a6debffc1 -->

- 날짜: 2026-08-20
- Jira: TMI-98
- 브랜치: `develop`
- 작업 목표: 병합 완료된 TMI-98의 현재 상태와 완료 전환을 공식 Atlassian MCP로 재확인하고, 저장소 승인 규칙에 따라 정확한 변경안을 사용자에게 제시한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`
- 구현 내용: 애플리케이션 코드는 변경하지 않았다. PR #29가 `develop`에 병합됐다는 기존 검증 결과를 유지하고 Jira 종료에 필요한 현재 상태와 전환만 읽기 전용으로 확인했다.
- 수행한 Jira 작업: Atlassian 공식 MCP로 TMI-98의 설명·완료 조건·현재 상태와 사용 가능한 전환을 조회했다. 현재 상태는 `해야 할 일`, Resolution 없음이며 `완료` transition ID `41`이 사용 가능하다. 이슈 수정·댓글·상태 전환·삭제는 수행하지 않았다.
- 추가한 댓글의 목적: 댓글을 추가하지 않으며 종료 전환만 적용하는 최소 변경안을 제시했다.
- 변경한 상태: 상태와 Resolution을 변경하지 않았다. 사용자 승인 시 TMI-98에 transition ID `41`만 적용해 `완료`로 변경할 예정이다.
- 승인 여부: 사용자는 Jira 종료를 요청했다. 저장소 규칙에 따라 이슈 키, 현재 상태, 목표 상태와 정확한 transition ID를 먼저 보여준 뒤 최종 승인을 기다린다.
- 실행한 테스트와 결과: 코드 변경이 없어 Gradle 테스트를 실행하지 않았다. 문서 변경은 `git diff --check`로 검증한다.
- 유지한 계약: Jira 조회에는 공식 Atlassian MCP만 사용했다. Secret·Token·Password·실제 Key·전체 MongoDB URI·사용자 개인정보를 Jira나 문서에 기록하지 않았고 Git commit·push를 수행하지 않았다.
- 결정사항: 댓글과 다른 필드는 변경하지 않고 승인 후 `해야 할 일`에서 `완료`로 가는 transition ID `41`만 적용한다. 적용 직후 상태와 Resolution을 재조회한다.
- 위험 요소: 상태 전환 전까지 Jira 상태가 이미 병합된 코드 상태와 불일치한다. Learning Core consumer·source deny marker 및 staging Mongo Transaction·publisher E2E는 별도 후속 범위로 남는다.
- 다음 작업: 사용자의 최종 승인을 받으면 transition ID `41`만 적용하고 `완료` 상태와 Resolution을 검증한 뒤 작업 기록을 다시 갱신한다.

## 2026-08-20 — Jira TMI-98 완료 전환

<!-- codex-turn:01a01cec-145f-72e2-8a68-43436aa8dc06 -->

- 날짜: 2026-08-20
- Jira: TMI-98
- 브랜치: `develop`
- 작업 목표: 사용자 승인에 따라 병합 완료된 TMI-98을 Jira에서 완료 처리하고 상태와 Resolution을 검증한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`
- 구현 내용: 애플리케이션 코드는 변경하지 않았다. TMI-98의 Jira 행정 종료 결과를 현재 상태 문서와 작업 기록에 반영했다.
- 수행한 Jira 작업: Atlassian 공식 MCP의 `transitionJiraIssue`로 TMI-98에 transition ID `41`만 적용했다. 후속 `getJiraIssue` 조회에서 status ID `10003`의 상태 `완료`와 Resolution ID `10000`의 `완료`를 확인했다.
- 추가한 댓글의 목적: 댓글을 추가하지 않았다.
- 변경한 상태: TMI-98을 `해야 할 일`에서 `완료`로 변경했고 Resolution도 `완료`로 설정된 것을 검증했다. 다른 Jira 이슈나 필드는 변경하지 않았다.
- 승인 여부: 사용자가 이슈 키·현재 상태·목표 상태·transition ID와 댓글 미등록 방침을 확인한 뒤 완료 전환을 명시적으로 승인했다.
- 실행한 테스트와 결과: 코드 변경이 없어 Gradle 테스트를 새로 실행하지 않았다. 구현 당시 `./gradlew clean test` 전체 496개와 후속 `./gradlew test` 성공 기록을 유지하며 문서 변경은 `git diff --check`로 검증한다.
- 유지한 계약: PR 병합을 확인한 뒤에만 Jira를 Done으로 변경했고 공식 Atlassian MCP만 사용했다. Secret·Token·Password·실제 Key·전체 MongoDB URI·사용자 개인정보를 Jira나 문서에 기록하지 않았으며 Git commit·push를 수행하지 않았다.
- 결정사항: 승인 범위대로 완료 transition ID `41` 이외의 Jira mutation은 수행하지 않았다. TMI-98 구현과 행정적 종료는 모두 완료됐지만 downstream consumer와 staging E2E 전에는 merge와 publisher 기능을 활성화하지 않는다.
- 위험 요소: Learning Core의 멱등 `UserMerged` consumer·source deny marker·학습 데이터 이전과 staging Mongo Transaction·publisher workload identity E2E가 별도 후속 범위로 남는다.
- 다음 작업: 별도 Jira에서 Learning Core consumer와 source deny marker를 설계·구현하고 staging E2E를 통과한 뒤에만 `GUEST_MERGE_ENABLED`와 `USER_MERGED_PUBLISHER_ENABLED` 활성화를 검토한다.

## 2026-08-20 — Jira TMI-98 종료 기록 turn marker 보정

<!-- codex-turn:01a01cec-5390-7c21-93f9-f47d4dea1d70 -->

- 날짜: 2026-08-20
- Jira: TMI-98
- 브랜치: `develop`
- 작업 목표: 현재 turn의 hook 요구에 맞춰 TMI-98 완료 전환 결과를 정확한 turn marker로 작업 기록에 append하고 CURRENT_STATE를 최신 상태로 유지한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`
- 구현 내용: 과거 WORKLOG 항목을 수정하거나 삭제하지 않고 현재 turn marker를 포함한 보정 항목을 파일 끝에 추가했다. 애플리케이션 코드는 변경하지 않았다.
- 수행한 Jira 작업: 앞선 사용자 승인에 따라 TMI-98에 완료 transition ID `41`만 적용했고 후속 조회에서 상태와 Resolution이 모두 `완료`임을 검증했다. 이번 보정 단계에서는 추가 Jira 조회·수정·댓글·상태 전환·삭제를 수행하지 않았다.
- 추가한 댓글의 목적: 댓글을 추가하지 않았다.
- 변경한 상태: 이번 보정 단계에서 Jira 상태를 추가 변경하지 않았다. TMI-98은 검증된 `완료` 상태와 `완료` Resolution을 유지한다.
- 승인 여부: 사용자가 TMI-98 완료 전환을 명시적으로 승인했으며, 이번 문서 보정은 hook의 작업 기록 요구에 따른 것이다.
- 실행한 테스트와 결과: 코드 변경이 없어 Gradle 테스트를 재실행하지 않았다. 문서 변경은 `git diff --check`와 turn marker 존재 여부로 검증한다.
- 유지한 계약: 공식 Atlassian MCP만 사용했고 Secret·Token·Password·실제 Key·전체 MongoDB URI·사용자 개인정보를 Jira나 문서에 기록하지 않았다. Git commit·push를 수행하지 않았다.
- 결정사항: TMI-98 구현과 Jira 행정 종료는 완료됐다. merge와 publisher 기능은 downstream consumer 및 staging E2E 전까지 기본 비활성으로 유지한다.
- 위험 요소: Learning Core 멱등 consumer·source deny marker·학습 데이터 이전과 staging Mongo Transaction·publisher workload identity E2E가 별도 후속 범위로 남는다.
- 다음 작업: 별도 Jira에서 downstream consumer와 staging E2E 범위를 진행한다.

## 2026-08-20 — TMI-98 후속 작업 설명

- 날짜: 2026-08-20
- Jira: TMI-98 후속 작업, 아직 미생성
- 브랜치: `develop`
- 작업 목표: 완료된 Identity Guest merge 다음에 필요한 Learning Core consumer와 staging E2E의 목적·범위·순서를 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`
- 구현 내용: Identity는 이미 `UserMerged` v1 outbox와 at-least-once publisher, MERGED source token gate를 구현했다. 다음 구현은 별도 Learning Core 저장소에서 eventId inbox, source→target 학습 데이터 ownership 이전, source actor deny marker를 같은 로컬 Transaction으로 처리하는 consumer다.
- 구현 내용: consumer는 중복 event를 멱등 성공시키고 같은 eventId의 다른 payload를 충돌로 거절해야 한다. source deny marker는 authorization alias가 아니므로 source JWT를 target 권한으로 바꾸지 않고 거절해야 한다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환·삭제를 수행하지 않았다. 후속 Jira는 사용자 승인 전 생성하지 않는다.
- 추가한 댓글의 목적: 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태를 변경하지 않았다. TMI-98은 상태와 Resolution 모두 `완료`다.
- 승인 여부: 사용자는 다음 작업의 설명만 요청했으며 Jira mutation이나 코드 구현은 요청하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없어 Gradle 테스트를 실행하지 않았다. 문서 변경은 `git diff --check`로 검증한다.
- 유지한 계약: Learning Core 코드를 Identity 저장소에 추가하지 않고, client userId를 신뢰하지 않으며 Python AI의 `user_id=examId` 계약을 유지한다. Secret·Token·Password·실제 Key·전체 MongoDB URI·사용자 개인정보를 기록하지 않았다.
- 결정사항: 실행 순서는 Learning Core consumer 구현·배포, 양 서비스 staging E2E, Identity publisher 활성화, Guest merge 활성화다. consumer와 source deny marker가 준비되기 전 두 Identity feature flag는 false를 유지한다.
- 위험 요소: consumer 없이 merge를 활성화하면 Learning Core 데이터가 source에 남고 기존 source Access Token이 만료 전까지 downstream에서 허용될 수 있다. at-least-once 중복, 부분 실패, 이벤트 순서, publisher 재시작과 lease 회수를 staging에서 검증해야 한다.
- 다음 작업: 별도 Jira Payload를 사용자에게 먼저 제시해 승인받고, Learning Core 저장소에서 consumer와 deny marker를 구현한 뒤 staging E2E를 수행한다.

## 2026-08-20 — TMI-98 후속 작업 설명 기록 확인

<!-- codex-turn:01a01cee-5fdc-7943-a53a-8830656a91d0 -->

- 날짜: 2026-08-20
- Jira: TMI-98 후속 작업, 아직 미생성
- 브랜치: `develop`
- 작업 목표: 현재 turn의 hook marker와 함께 TMI-98 다음 작업 설명 결과를 작업 기록에 남기고 CURRENT_STATE를 최신화한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`
- 구현 내용: 다음 작업을 별도 Learning Core 저장소의 `UserMerged` v1 멱등 consumer, eventId inbox, source→target ownership 이전, source actor deny marker와 staging E2E로 설명했다. 애플리케이션 코드는 변경하지 않았다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환·삭제를 수행하지 않았다. TMI-98은 상태와 Resolution 모두 `완료`를 유지한다.
- 추가한 댓글의 목적: 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태를 변경하지 않았다.
- 승인 여부: 사용자는 다음 작업 설명만 요청했으며 Jira mutation이나 구현은 승인·요청하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없어 Gradle 테스트를 실행하지 않았다. 문서 변경은 `git diff --check`와 현재 turn marker 존재 여부로 검증한다.
- 유지한 계약: Learning Core 코드를 Identity 저장소에 추가하지 않았고 UUID userId·JWT·Python AI `examId` 계약을 변경하지 않았다. Secret·Token·Password·실제 Key·전체 MongoDB URI·사용자 개인정보를 기록하지 않았으며 Git commit·push를 수행하지 않았다.
- 결정사항: Learning Core consumer와 staging E2E가 준비된 뒤 Identity publisher를 활성화하고 Guest merge는 마지막에 활성화한다. 준비 전 두 feature flag는 false를 유지한다.
- 위험 요소: consumer 없이 merge를 활성화하면 source 학습 데이터와 source Access Token 처리의 downstream 불일치가 발생할 수 있다.
- 다음 작업: 사용자가 원하면 별도 Learning Core Jira의 제목·설명·완료 조건·제외 범위 Payload를 먼저 작성해 승인받는다.

## 2026-08-20 — Learning Core UserMerged consumer 구현 인계 문서 작성

<!-- codex-turn:01a01cf0-62a7-7fe1-a41d-3efeb4eea11f -->

- 날짜: 2026-08-20
- Jira: TMI-98 후속 Learning Core 작업
- 브랜치: `develop` (`91671ce`, `origin/develop`과 일치, commit·push 미수행)
- 작업 목표: Identity TMI-98 다음 단계인 Learning Core `UserMerged` v1 consumer 구현을 다른 저장소에 그대로 전달할 수 있는 독립적인 계약·완료 조건 문서로 작성한다.
- 변경 파일: `docs/contracts/learning-core-user-merged-consumer-handoff.md`를 새로 작성하고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: 현재 구현된 `UserMergedWireEvent`를 기준으로 eventId, schemaVersion 1, sourceUserId, targetUserId, occurredAt의 정확한 JSON 계약과 canonical UUID·source/target 불일치·4 KiB 상한·개인정보 비포함 불변식을 기록했다. v1에는 eventType이 없고 전용 endpoint와 schemaVersion으로 계약을 식별함을 명시했다.
- 구현 내용: Identity publisher의 HTTPS POST, workload identity Bearer credential, redirect 금지, at-least-once 전달, 모든 2xx 성공, 408·425·429·5xx 재시도, 나머지 3xx·4xx 영구 격리 동작을 코드와 일치시켰다. 동일 eventId·동일 payload duplicate에는 409가 아니라 2xx를 반환해야 함을 강조했다.
- 구현 내용: Learning Core가 eventId unique inbox와 payload digest를 소유하고 source ownership migration, aggregate별 충돌 정책, source actor deny marker, inbox 완료를 같은 local Transaction으로 처리하도록 요구했다. source mapping은 migration·감사용이며 source JWT를 target authorization alias로 사용하지 않는 금지 규칙을 포함했다.
- 구현 내용: 시험·결과·풀이 진행·10초 챌린지·스트릭·단어장 등 실제 Learning Core aggregate inventory, direct 또는 durable inbox+worker 처리 선택, concurrency·rollback·response loss·restart·workload auth 테스트, metric·민감정보 비노출, consumer→publisher→merge feature 순차 배포와 Identity 협의 항목을 완료 체크리스트로 정리했다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다. Learning Core Jira는 생성하지 않고 인계 문서만 작성했다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다. Identity TMI-98은 완료 상태를 유지한다.
- 승인 여부: 사용자는 Learning Core에 그대로 붙여 전달할 문서를 Identity 저장소에 작성하도록 요청했다. Learning Core 저장소·서비스·외부 시스템은 변경하지 않았다.
- 실행한 테스트와 결과: 문서만 변경해 Gradle 테스트를 실행하지 않았다. Identity event mapper·publisher·delivery adapter·configuration·tests를 정적으로 대조했고 `git diff --check`, Markdown code fence 짝, 필수 계약 항목과 Secret·실제 인프라 값 비포함을 검증한다.
- 유지한 계약: Identity는 User merge와 outbox producer만 소유하고 Learning Core가 시험·결과·학습 데이터와 consumer를 소유한다. source를 target actor로 승격하지 않고 JWT·RS256·JWKS·RefreshSession 계약을 변경하지 않았으며 Secret·Token·Password·실제 Key·전체 MongoDB URI·사용자 개인정보를 기록하지 않았다.
- 결정사항: endpoint path, workload audience/principal, direct 처리 또는 inbox worker, aggregate별 충돌 정책과 SLA는 Learning Core가 구현 전 확정해 Identity 팀에 공유한다. wire v1 변경이 필요하면 consumer에서 추측하지 않고 새 schemaVersion을 합의한다.
- 위험 요소: aggregate별 unique 충돌 정책 없이 userId를 일괄 update하면 target 데이터 overwrite·중복·감사 이력 훼손이 발생할 수 있다. consumer와 source deny marker, staging E2E가 준비되기 전에 Identity merge/publisher flag를 활성화하면 source JWT가 Learning Core에서 만료 전까지 사용될 수 있다.
- 다음 작업: 문서를 Learning Core 저장소에 전달해 별도 Jira·branch를 만들고 실제 aggregate inventory와 충돌 정책을 채운 뒤 consumer 구현·테스트·staging E2E를 수행한다. 완료 후 endpoint와 audience를 Identity staging에 설정하고 consumer, publisher, merge feature 순서로 활성화한다.

## 2026-08-20 — Learning Core UserMerged 인계서 외부 리뷰 반영

<!-- codex-turn:01a01cf7-b779-7633-8231-de261ef2ac4a -->

- 날짜: 2026-08-20
- Jira: TMI-98 후속 Learning Core 작업
- 브랜치: `develop` (`91671ce`, `origin/develop`과 일치, commit·push 미수행)
- 작업 목표: 사용자가 전달한 Learning Core 인계서 리뷰의 P0·동시성·cache·inventory·metric 제안을 실제 Identity producer 구현과 대조해 타당한 내용을 계약 문서에 반영한다.
- 변경 파일: `docs/contracts/learning-core-user-merged-consumer-handoff.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`를 갱신했다. 애플리케이션 코드는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않았다.
- 구현 내용: v1 consumer를 HTTP 요청 안에서 migration·deny·inbox를 commit하는 direct local Transaction으로 고정하고 신규·정상 duplicate는 commit 확인 후 `204`, 처리 중 결과 미확정은 `425` 또는 `503`으로 정했다. `202`와 durable inbox worker 혼합을 금지하고 Identity 기본 read timeout 5초 안의 staging P99 근거가 없으면 활성화하지 않고 계약을 다시 승인하도록 gate를 추가했다.
- 구현 내용: raw/canonical JSON 전체 digest를 제거하고 domain separator, schemaVersion 1, canonical lowercase source/target UUID, UTC Instant 정규화만 NUL로 구분한 UTF-8 입력의 SHA-256 lowercase hex를 v1 semantic digest로 고정했다. eventId는 unique key로만 쓰고 optional field·property 순서·공백은 digest에서 제외해 호환 필드 추가 정책과 모순을 제거했다.
- 구현 내용: workload credential type·issuer·JWKS·algorithm·audience·Identity principal·TTL·clock skew·rotation·refresh 책임을 채우는 필수 프로파일 표를 추가하고 사용자용 Access JWT 재사용을 금지했다. 구체값이 TBD이면 staging publisher도 활성화하지 않도록 했다.
- 구현 내용: Redis 같은 외부 cache 삭제를 DB Transaction에 포함하지 않고 cache invalidation intent/outbox만 같은 Transaction에 저장하도록 정정했다. source write 경쟁을 막기 위해 모든 user-owned write가 같은 per-user ownership guard의 ACTIVE state와 revision을 Transaction 안에서 CAS/touch하고 merge가 같은 guard를 MERGED로 CAS하도록 구체화했다.
- 구현 내용: ownership inventory를 consumer 구현의 Phase 0 선행 작업과 별도 Jira 분해 대상으로 올리고 직접·간접 ownership, 모든 write 경로, unique 충돌, 예상 처리량, Transaction·cache·audit 규칙을 산출물로 요구했다. 관측 metric은 delivery lag, processing lag, total lag로 분리했다.
- 수행한 Jira 작업: Jira 조회·생성·수정·댓글·상태 전환을 수행하지 않았다. Learning Core Jira도 생성하지 않았다.
- 추가한 댓글의 목적: Jira 댓글을 추가하지 않았다.
- 변경한 상태: Jira 상태나 Resolution을 변경하지 않았다. Identity TMI-98은 완료 상태를 유지한다.
- 승인 여부: 사용자는 첨부 리뷰를 읽고 타당한 내용을 기존 Learning Core 인계 문서에 반영하도록 요청했다. Learning Core 저장소·서비스·외부 시스템은 변경하지 않았다.
- 실행한 테스트와 결과: 문서만 변경해 Gradle 테스트를 실행하지 않았다. Identity event mapper·publisher의 payload와 HTTP 분류를 다시 대조했으며 종료 전 `git diff --check`, code fence 짝, marker 단일 존재, direct/worker·digest·workload·cache·guard·inventory·metric 필수 문구와 Secret 비포함을 검증한다.
- 유지한 계약: source를 target authorization alias로 사용하지 않고 Learning Core ownership migration과 source deny를 같은 local Transaction으로 처리한다. Identity/Learning Core 경계와 UserMerged v1 wire field를 변경하지 않았으며 Secret·Token·Password·실제 Key·전체 MongoDB URI·사용자 개인정보를 기록하지 않았다.
- 결정사항: direct v1을 채택하되 ownership inventory와 staging P99가 producer timeout 요구를 충족하지 못하면 timeout만 임의 확장하지 않고 durable worker 계약으로 명시적 재설계를 진행한다. aggregate별 정책과 workload identity 구체값이 확정되기 전 구현 완료·feature 활성화를 금지한다.
- 위험 요소: 모든 source write가 ownership guard와 같은 Transaction에 참여하지 않으면 migration 직후 source 데이터가 다시 생성될 수 있다. direct migration이 Identity read timeout을 넘거나 workload 인증 TBD가 남은 상태에서 활성화하면 retry 폭증 또는 무인증 내부 endpoint 위험이 있다.
- 다음 작업: 업데이트된 문서를 Learning Core에 전달해 Phase 0 inventory Jira부터 생성하고 workload profile과 aggregate별 충돌 정책을 확정한다. 이후 direct consumer 구현과 staging latency·concurrency·retry E2E를 완료한 뒤 Identity publisher와 merge feature를 순차 활성화한다.

## 2026-08-24 — Learning Core·Identity Sentry 이벤트와 이메일 조건 확인

<!-- codex-turn:01a03176-aecb-7583-9e21-595921287246 -->

- 날짜: 2026-08-24
- 브랜치: `develop`
- Jira: 없음
- 작업 목표: Identity를 포함한 두 앱 서버에서 Sentry가 수집하는 오류와 이메일 알림 조건을 코드 기준으로 확인한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: Identity는 `SENTRY_ENABLED` 기본 false이며 enabled+DSN 환경에서 `GlobalExceptionHandler`의 예상 밖 Exception을 500 event로 명시 capture한다. Validation, JSON parse, 404, method/media type 오류, BusinessException과 단순 ERROR 로그는 event로 보내지 않고 tracing·profiling·Sentry Logs·Logback integration은 0/off다.
- 구현 내용: sanitizer는 request/user/breadcrumb/extra/context와 예외 메시지를 버리고 예외 type·정제 stack frame·안전한 requestId/errorCode/method/route/500 tag만 유지한다. 실제 이메일은 외부 Sentry 프로젝트의 Alert Rule, environment filter, email action과 개인 notification 설정이 충족될 때만 발송된다.
- 실행한 테스트와 결과: 코드 변경 없는 정적 분석이므로 Gradle 테스트와 실제 Sentry 전송은 실행하지 않았다. 설정·handler·reporter·sanitizer와 기존 수신 검증 기록을 확인했으며 종료 전 `git diff --check`와 marker 단일 포함을 검증한다.
- 유지한 계약: Identity JWT·RefreshSession·Security·API 계약과 Learning Core 외부 계약을 변경하지 않았다. Secret·Token·실제 DSN·Password·실제 Key·전체 MongoDB URI와 개인정보를 조회하거나 기록하지 않았다.
- 결정사항: Sentry event 발생과 이메일 수신을 별도 단계로 설명한다. 저장소에는 Alert Rule·수신자·임계값이 없으므로 현재 메일 활성 여부는 저장소만으로 단정하지 않는다.
- 위험 요소: ECS runtime enabled/DSN/environment 주입과 Sentry 조직의 rule·member notification이 빠지면 500이 발생해도 event 또는 email이 오지 않을 수 있다.
- 다음 작업: Sentry 프로젝트의 Alerts와 멤버 Notifications에서 prod 신규 issue·regression·빈도/급증 rule, 이메일 action과 environment 조건을 확인한다.

## 2026-08-24 — Disabled Firebase 인증수단 동기화 구현 역할 설명

<!-- codex-turn:01a0320a-a7b6-70c1-8570-b7f2d6e5be56 -->

- 날짜: 2026-08-24
- 브랜치: `develop` (Codex commit·push 미수행)
- 작업 목표: `DisabledFirebaseAuthMethodsSyncUseCase`가 어떤 상황에서 사용되고 어떤 동작을 하는지 현재 구성·Controller·실제 Service와 대조해 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고, 기존 미커밋 변경과 `docs/contracts/learning-core-user-merged-consumer-handoff.md`는 건드리지 않았으며 WORKLOG 과거 기록을 수정하거나 삭제하지 않고 이번 항목만 파일 끝에 append했다.
- 구현 내용: `app.firebase-auth.enabled=false` 또는 설정 누락 시 `DisabledFirebaseAuthenticationConfiguration`이 실제 `FirebaseAuthMethodsSyncService` 대신 이 구현을 `FirebaseAuthMethodsSyncUseCase` Bean으로 제공함을 확인했다. 기본 환경변수 값도 `FIREBASE_AUTH_ENABLED=false`다.
- 구현 내용: Controller는 동일한 use case interface를 항상 주입받아 `/api/v1/auth/firebase/auth-methods/sync` endpoint 구조를 유지한다. 비활성 구현은 요청을 처리하거나 DB를 변경하지 않고 즉시 `AuthException(FIREBASE_UNAVAILABLE)`을 던지며, 오류 계약은 HTTP 503과 고정 code/message로 변환된다.
- 구현 내용: Firebase 기능이 활성화되면 실제 `FirebaseAuthMethodsSyncService`가 선택되어 Identity JWT의 ACTIVE MEMBER, FirebaseIdentity의 project·UID, fresh Firebase proof를 검증하고 누락된 SocialIdentity만 멱등 저장한 뒤 연결 Provider 목록을 반환한다. 비활성 구현은 이 모든 작업을 의도적으로 수행하지 않는 feature-off 안전장치다.
- 실행한 테스트와 결과: 설명·작업 기록만 변경해 Gradle 테스트는 실행하지 않았다. 관련 configuration·controller·service·오류 enum과 기존 configuration/security 테스트를 정적으로 확인했으며 종료 전 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Firebase 기능은 기본 비활성이고 비활성 상태에서 SDK·외부 Firebase·Repository를 호출하지 않는다. canonical UUID userId/JWT `sub`, RS256·JWKS·RefreshSession, Firebase UID 비노출·비자동 merge와 Identity 도메인 경계를 변경하지 않았다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 이 클래스는 불필요한 빈 구현이 아니라 조건부 Bean 누락을 피하면서 disabled 상태를 503으로 명확히 표현하는 fail-closed adapter다. 성공 응답을 반환하는 no-op이나 실제 동기화 로직으로 해석하지 않는다.
- 위험 요소: endpoint 자체는 Security·OpenAPI에 존재하므로 client는 feature-off 환경에서 404가 아니라 503을 받는다. 운영에서 Firebase를 사용할 환경은 전체 flag와 필수 project/credential 설정을 올바르게 주입해야 하며 provider별 flag는 별도 정책 검증을 계속 적용한다.
- 다음 작업: 코드 변경은 필요 없다. 사용자가 원하면 enabled/disabled Bean 선택 흐름이나 실제 `FirebaseAuthMethodsSyncService`의 소유권·멱등 동기화 로직을 이어서 설명한다.

## 2026-08-24 — Firebase 인증수단 동기화 Service 설명

<!-- codex-turn:01a0320c-8b84-7a31-9632-d0ad54701e2d -->

- 날짜: 2026-08-24
- 브랜치: `develop` (Codex commit·push 미수행)
- 작업 목표: 첨부된 `FirebaseAuthMethodsSyncService`의 책임과 검증·저장·동시성 처리 흐름을 관련 Transaction service·principal·테스트와 대조해 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 기존 미커밋 변경과 새 계약 문서는 건드리지 않았으며, WORKLOG 과거 기록은 수정하거나 삭제하지 않고 이번 항목만 파일 끝에 append했다.
- 구현 내용: 서비스는 Identity JWT의 canonical userId로 User를 조회해 ACTIVE MEMBER만 허용하고, userId에 연결된 FirebaseIdentity와 `AUTH_METHOD_SYNC` 목적의 fresh Firebase proof가 같은 project·UID인지 검증한다. 클라이언트 Body의 userId나 Firebase UID를 canonical userId로 사용하지 않는다.
- 구현 내용: 검증된 Firebase principal의 linked Social provider·subject를 순회해 Identity에 없는 SocialIdentity만 현재 userId로 생성한다. 이미 같은 사용자가 소유하면 건너뛰고 다른 사용자가 소유하면 저장 전 `SOCIAL_IDENTITY_CONFLICT`로 거절한다.
- 구현 내용: 누락 목록은 별도 Mongo Transaction service에서 한 번에 저장한다. 사전 조회 뒤 동시 insert로 `DuplicateKeyException`이 발생하면 모든 principal identity를 재조회해 현재 userId 소유이면 멱등 성공으로 인정하고, 누락됐거나 다른 user 소유이면 conflict로 변환한다. 마지막에는 DB를 다시 조회해 현재 연결 Provider 집합을 응답한다.
- 실행한 테스트와 결과: 설명만 수행해 Gradle 테스트는 실행하지 않았다. 첨부 코드와 실제 Transaction service, `VerifiedFirebasePrincipal`, `VerifiedSocialPrincipal`, 기존 Service 테스트를 정적으로 확인했으며 종료 전 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: 실제 userId와 JWT `sub`는 canonical UUID 문자열이며 Firebase UID·provider subject는 userId나 자동 merge 키가 아니다. fresh Firebase proof와 기존 mapping이 모두 일치해야 mutation을 허용하고 Token·provider subject·사용자 개인정보를 로그나 작업 기록에 남기지 않는다. RS256·JWKS·RefreshSession과 Identity 도메인 경계를 변경하지 않았다.
- 결정사항: 이 서비스는 Firebase 자체 계정을 연결하거나 merge하는 기능이 아니라, 이미 같은 FirebaseIdentity를 소유한 ACTIVE MEMBER의 인증수단 snapshot을 Identity SocialIdentity에 보완하는 단방향·추가 전용 동기화다. 삭제·교체·다른 owner 이전은 수행하지 않는다.
- 결정사항: 사용자는 향후 이와 같은 단순 코드 설명·리뷰를 WORKLOG에 기록하지 않기를 요청했다. 다만 이번 turn에는 상위 개발자 지침과 저장소 AGENTS.md가 모든 분석 작업의 WORKLOG/CURRENT_STATE 기록을 강제하므로 최소 기록을 남겼으며, 해당 규칙이 변경되기 전에는 사용자 선호만으로 생략할 수 없다.
- 위험 요소: Firebase에서 provider 연결이 해제돼도 이 서비스는 기존 SocialIdentity를 삭제하지 않는다. 또한 `DuplicateKeyException` 복구 경로를 직접 검증하는 전용 Service 테스트는 현재 파일에서 확인되지 않아, 동시성 회귀를 더 강하게 고정하려면 별도 테스트를 고려할 수 있다.
- 다음 작업: 코드 변경은 필요 없다. 작업 기록을 정말 생략하려면 저장소의 AGENTS.md와 turn 종료 강제 지침을 먼저 변경해야 하며, 사용자가 원하면 이 서비스의 동시성 분기나 provider unlink 정책을 별도로 검토한다.

## 2026-08-24 — Firebase 인증수단 동기화 Transaction Service 설명

<!-- codex-turn:01a03211-f553-7463-ace9-e26c005ce61a -->

- 날짜: 2026-08-24
- 브랜치: `develop` (Codex commit·push 미수행)
- 작업 목표: `FirebaseAuthMethodsSyncTransactionService`의 입력 방어, Mongo Transaction 저장과 별도 Bean 분리 이유를 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 기존 미커밋 변경과 새 계약 문서는 건드리지 않았으며, WORKLOG 과거 기록은 수정하거나 삭제하지 않고 이번 항목만 파일 끝에 append했다.
- 구현 내용: 생성자는 `SocialIdentityRepository` null 주입을 즉시 거절한다. `saveMissing`은 입력 list null과 null element를 거절하고 `List.copyOf`로 불변 snapshot을 만들어 호출 측의 후속 변경 영향을 차단한다.
- 구현 내용: 목록이 비어 있으면 Repository를 호출하지 않고, 값이 있으면 `mongoTransactionManager`가 관리하는 Transaction 안에서 `saveAll`한다. 여러 누락 identity 중 하나가 unique 충돌 등으로 실패할 경우 지원되는 MongoDB Transaction 환경에서는 전체 write를 rollback하는 원자적 저장 경계다.
- 구현 내용: Transaction method를 별도 Spring Bean의 public method로 두어 호출이 Spring proxy를 통과하게 하고, Firebase 검증·사용자/소유권 조회까지 긴 Transaction에 포함하지 않은 채 실제 write 구간만 좁게 묶는다. 같은 클래스 내부의 self-invocation에 `@Transactional`을 붙여 proxy가 우회되는 문제도 피한다.
- 실행한 테스트와 결과: 설명만 수행해 Gradle 테스트는 실행하지 않았다. 실제 Bean wiring과 호출 위치, 기존 Service 테스트를 정적으로 확인하고 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다. 이 Transaction service를 실제 MongoDB와 함께 직접 검증하는 전용 테스트는 검색 결과에서 확인되지 않았다.
- 유지한 계약: 저장 대상은 검증 완료 후 생성된 SocialIdentity뿐이며 Firebase ID Token·provider subject·canonical userId 계약을 변경하지 않는다. Token·Secret·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 작업 기록에 추가하지 않았다.
- 결정사항: 이 클래스는 인증·소유권 판단을 하지 않는 write-only Transaction boundary다. 누락 목록 산출과 동시 `DuplicateKeyException`의 멱등/충돌 해석은 호출자인 `FirebaseAuthMethodsSyncService`가 담당한다.
- 위험 요소: MongoDB Transaction은 standalone이 아닌 replica set/transaction 지원 환경과 올바른 transaction manager wiring이 필요하다. 또한 `saveAll` 실패 자체를 이 클래스에서 변환하지 않으므로 `DuplicateKeyException` 등은 호출자에게 전파된다.
- 다음 작업: 코드 변경은 필요 없다. 사용자의 단순 설명 비기록 선호와 달리 현재 강제 규칙 때문에 최소 기록을 남겼으며, 향후 생략하려면 해당 상위 규칙 변경이 필요하다.

## 2026-08-24 — Firebase enrollment attempt Service 설명

<!-- codex-turn:01a0321b-a952-7362-bf52-86132439f3c2 -->

- 날짜: 2026-08-24
- 브랜치: `develop` (Codex commit·push 미수행)
- 작업 목표: `FirebaseEnrollmentAttemptService`의 active attempt 재사용, 만료 교체, 동시 insert 경쟁과 일회성 consume 흐름을 Entity·Repository CAS·테스트와 대조해 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 기존 미커밋 변경과 새 계약 문서는 건드리지 않았으며, WORKLOG 과거 기록은 수정하거나 삭제하지 않고 이번 항목만 파일 끝에 append했다.
- 구현 내용: 생성자에서 Repository·Clock과 양수 enrollment TTL·cleanup retention을 강제한다. `startOrReuse`는 입력 불변식을 검증하며 하나의 candidate를 만든 뒤 같은 project·Firebase UID·bindingType·boundUserId의 PENDING attempt를 최대 4회 재조회한다.
- 구현 내용: 조회한 PENDING이 application Clock 기준 active면 그대로 반환한다. 만료됐으면 `_id + PENDING + expiresAt <= now` 조건으로 EXPIRED 전환을 시도하고 결과와 무관하게 다음 반복에서 최신 상태를 다시 확인한다. PENDING이 없으면 candidate를 insert한다.
- 구현 내용: `(firebaseProjectId, firebaseUid, bindingType, boundUserId)`의 PENDING partial unique index가 동시 insert winner 하나만 허용한다. loser의 `DuplicateKeyException`은 다음 반복에서 winner를 재조회·재사용하는 신호이며, 네 번 안에 안정 상태를 찾지 못하면 `IllegalStateException`으로 fail-closed 처리한다.
- 구현 내용: `consume`은 enrollmentId와 전체 binding, status PENDING, `expiresAt > now`를 하나의 Mongo conditional update로 검사해 CONSUMED·consumedAt으로 전환한다. 한 요청만 true를 얻고 재사용·만료·binding mismatch는 false다. TTL cleanupAt은 correctness가 아니라 보존 기간 뒤 비동기 삭제에만 사용한다.
- 실행한 테스트와 결과: 설명만 수행해 Gradle 테스트는 실행하지 않았다. Entity의 partial unique·TTL 계약, custom Repository query/update와 active reuse·expired replacement·duplicate loser·consume delegation 테스트를 정적으로 확인했으며 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Firebase UID는 canonical userId가 아니며 DIRECT_SIGNUP은 boundUserId가 null, GUEST_USER는 canonical UUID binding을 요구한다. attempt는 짧은 수명의 가입 진행 상태이고 Firebase Token·Secret·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 저장하거나 작업 기록에 추가하지 않았다.
- 결정사항: 동일 binding의 반복 요청은 새 attempt를 계속 만들지 않고 active winner를 반환한다. initial sign-in method가 다른 동시 요청도 binding이 같으면 먼저 생성된 attempt를 재사용하며, 만료·소비 correctness는 Mongo TTL 삭제 시점이 아니라 application `expiresAt`과 CAS가 담당한다.
- 위험 요소: 재시도는 backoff 없는 최대 4회이므로 비정상적으로 긴 contention에서는 안정 상태가 생겨도 `IllegalStateException`에 도달할 수 있다. `expireIfPendingAndExpired` 결과를 직접 사용하지 않는 것은 다음 반복 재조회로 경쟁 결과를 해석하려는 의도이며, loop 제거 또는 TTL 의존 변경은 동시성 계약을 깨뜨릴 수 있다.
- 다음 작업: 코드 변경은 필요 없다. 사용자가 원하면 `FirebaseEnrollmentAttempt`의 partial unique/TTL index 또는 Repository의 CAS query를 이어서 설명한다.

## 2026-08-24 — Firebase 로그인 exchange Service 설명

<!-- codex-turn:01a0322f-30f8-7891-8eaa-6a7824cad284 -->

- 날짜: 2026-08-24
- 브랜치: `develop` (Codex commit·push 미수행)
- 작업 목표: 첨부된 `FirebaseExchangeService`의 Firebase proof 검증, 기존 MEMBER 로그인과 신규 enrollment 분기, SocialIdentity 충돌 방어와 요구사항 계산을 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 기존 미커밋 변경과 새 계약 문서는 건드리지 않았으며, WORKLOG 과거 기록은 수정하거나 삭제하지 않고 이번 항목만 파일 끝에 append했다.
- 구현 내용: 요청 Firebase ID Token을 `LOGIN_EXCHANGE` 목적으로 먼저 검증하고 최소 principal을 얻은 뒤 `(firebaseProjectId, firebaseUid)` FirebaseIdentity를 조회한다. verifier 실패 시 Repository·Token issuer·enrollment를 호출하지 않는다.
- 구현 내용: FirebaseIdentity가 있으면 canonical userId의 User가 존재하는지와 ACTIVE MEMBER 여부를 확인한다. Firebase principal에 연결된 각 SocialIdentity가 없거나 같은 user 소유인 경우에만 빈 scope의 자체 RS256 Access Token과 원문 비저장 RefreshSession을 발급해 `AUTHENTICATED` 응답과 millisecond TTL을 반환한다.
- 구현 내용: FirebaseIdentity가 없으면 principal의 linked social provider·subject가 하나라도 기존 Identity User에 귀속된 경우 자동 연결·merge·가입을 하지 않고 `SOCIAL_IDENTITY_CONFLICT`로 fail-closed 처리한다. owner가 전혀 없을 때만 DIRECT_SIGNUP enrollment attempt를 생성·재사용하고 User·RefreshSession 없이 `ENROLLMENT_REQUIRED`를 반환한다.
- 구현 내용: PASSWORD가 linked됐지만 email이 미검증이면 EMAIL_VERIFICATION, PHONE method 누락 또는 phone 미검증이면 PHONE_VERIFICATION을 추가하며 PROFILE·CONSENTS는 신규 enrollment에서 항상 요구한다. expiresIn은 application Clock과 attempt expiresAt의 차이를 millisecond로 계산해 음수이면 0으로 clamp한다.
- 실행한 테스트와 결과: 설명만 수행해 Gradle 테스트는 실행하지 않았다. 두 response subtype과 기존 MEMBER token 발급, 신규 enrollment 무mutation, 요구사항 계산, 기존/다른 social owner 충돌, mapping orphan, 비활성 User·Guest, verifier 조기 실패 테스트를 정적으로 확인했으며 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Firebase ID Token은 Identity 내부 검증 proof이며 Learning Core Token이 아니다. canonical userId와 JWT `sub`는 Firebase UID·provider subject와 분리하고, 미등록 UID에는 전화 인증·프로필·필수 동의 완료 전 User·RefreshSession을 만들지 않는다. 자동 merge 없이 소유권 충돌을 거절하고 Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: exchange 결과는 `AUTHENTICATED` 또는 `ENROLLMENT_REQUIRED`의 명시적 두 subtype이다. 기존 mapping 로그인에서는 누락 SocialIdentity를 이 서비스가 저장하지 않고 별도 auth-method sync가 담당하며, 신규 경로에서도 기존 social owner를 canonical 계정 추정 근거로 자동 연결하지 않는다.
- 위험 요소: `ensureNoSocialIdentityOwner`는 기존 로그인에서는 같은 user 소유를 허용하므로 이름보다 실제 계약이 넓다. 신규 가입에서 PASSWORD가 없는 social-only principal의 email 미검증은 별도 EMAIL_VERIFICATION 요구사항을 만들지 않는 현재 정책이며 Provider/Firebase 검증 계약과 함께 유지해야 한다. attempt가 응답 직전에 만료되면 expiresIn 0이 반환될 수 있다.
- 다음 작업: 코드 변경은 필요 없다. 사용자가 원하면 `authenticate`, social owner guard 또는 `missingRequirements` 정책을 각각 더 세분화해 설명한다.

## 2026-08-24 — Firebase 회원 탈퇴 후 재가입 교착 진단

<!-- codex-turn:01a032a3-dd90-7db3-b2e9-44b61945c653 -->

- 날짜: 2026-08-24
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 없음
- 작업 목표: 기존 Firebase MEMBER가 탈퇴한 뒤 같은 Firebase credential로 로그인도 신규 가입도 할 수 없는 상태가 생기는지 현재 exchange·withdrawal·identity/phone lifecycle 구현을 대조해 진단한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 기존 미커밋 변경과 새 계약 문서는 건드리지 않았으며, WORKLOG 과거 기록은 수정하거나 삭제하지 않고 이번 항목만 파일 끝에 append했다.
- 구현 내용: `FirebaseExchangeService`는 `(project, UID)` FirebaseIdentity가 존재하면 항상 기존 owner 인증 경로로 들어가고 User가 WITHDRAWN이면 `ACCOUNT_NOT_ACTIVE`로 거절한다. 기존 mapping이 있는 상태에서는 DIRECT_SIGNUP enrollment 분기로 돌아가지 않는다.
- 구현 내용: 현재 일반 `UserWithdrawalService.validateProviderCredential`은 GUEST가 아닌 MEMBER 중 local credential이 없는 social-only/Firebase MEMBER를 `INVALID_WITHDRAWAL_CREDENTIALS`로 거절한다. fresh Firebase proof로 탈퇴를 재인증하는 별도 공개 use case는 검색 결과에서 확인되지 않아 Firebase MEMBER는 정상 탈퇴 진입부터 미구현 상태다.
- 구현 내용: `UserWithdrawalTransactionService`는 User tombstone CAS, phone eligibility binding revoke outbox와 RefreshSession 전체 폐기만 수행한다. FirebaseIdentityRepository·SocialIdentityRepository·PhoneIdentity/alias Repository와 Firebase Admin lifecycle adapter를 주입받지 않으므로 내부 mapping·번호 점유를 해제하거나 Firebase refresh revoke/user delete를 요청하지 않는다.
- 구현 내용: 설계 ADR은 withdrawal 시 내부 tombstone·Session 폐기·lifecycle outbox commit 후 외부 Firebase revoke/delete를 retry하고, PhoneIdentity·alias 개인정보 cleanup이 끝난 뒤 가입 uniqueness를 해제하도록 정의한다. 하지만 해당 Firebase withdrawal lifecycle outbox/publisher와 identity/phone cleanup은 현재 코드에서 구현되지 않았다.
- 실행한 테스트와 결과: 진단만 수행해 Gradle 테스트는 실행하지 않았다. exchange·withdrawal service/transaction, Firebase/Social repositories, Firebase signup과 ADR/구현계획의 lifecycle 구간을 정적으로 확인했으며 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Firebase UID·provider subject·phone을 canonical userId나 자동 merge 키로 사용하지 않고 WITHDRAWN tombstone과 기존 UUID를 보존한다. Firebase와 MongoDB 외부 작업을 하나의 Transaction으로 가장하지 않으며 Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 사용자의 지적은 맞으며 단순 exchange 분기 수정만으로 해결하면 안 된다. 제품이 탈퇴 후 새 UUID 재가입을 허용할지 기존 canonical 계정 재활성화를 허용할지 먼저 결정하고, 그 결정에 맞는 explicit lifecycle state와 cleanup/reconciliation을 구현해야 한다. WITHDRAWN owner를 자동 enrollment로 넘기는 것은 SocialIdentity·phone 점유와 탈퇴 처리 중 재진입을 우회할 수 있어 금지한다.
- 위험 요소: 현재 기능을 활성화하면 Firebase MEMBER는 탈퇴할 수 없거나, 수동/부분 처리로 WITHDRAWN이 된 경우 로그인·재가입 교착에 빠질 수 있다. mapping만 먼저 삭제하면 외부 Firebase revoke/delete 실패 중 재가입이 열리고, phone/social만 보존하면 unique 충돌이 계속되므로 단계별 상태와 재처리 가능한 outbox가 필요하다.
- 다음 작업: 별도 승인된 Jira 범위로 Firebase recent-auth 탈퇴 use case, 내부 withdrawal lifecycle state/outbox, Firebase revoke/delete worker, SocialIdentity 및 PhoneIdentity/alias release 정책, 재가입 또는 재활성화 경로와 실패 reconciliation 테스트를 설계·구현한다. 그 전에는 production Firebase signup/exchange 활성화를 유지하지 않는다.

## 2026-08-24 — Firebase 탈퇴 선행·재가입 후속 구현 순서 정리

<!-- codex-turn:01a032b0-f92d-7f50-afc5-76696f568e94 -->

- 날짜: 2026-08-24
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 없음
- 작업 목표: 확인된 Firebase 탈퇴·재가입 교착을 해결하기 위해 탈퇴 lifecycle과 재가입 로직의 안전한 구현 순서와 단계별 완료 조건을 정리한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 기존 미커밋 변경과 새 계약 문서는 건드리지 않았으며, WORKLOG 과거 기록은 수정하거나 삭제하지 않고 이번 항목만 파일 끝에 append했다.
- 구현 내용: 첫 결정은 탈퇴 후 새 canonical UUID User를 만드는 재가입과 기존 tombstone User를 재활성화하는 복구 중 하나를 선택하는 것이다. 현재 LOCAL 재가입·WITHDRAWN tombstone 계약과 Firebase 설계 문서의 credential/phone cleanup 후 uniqueness 해제에는 새 UUID 재가입이 더 일관되며, 재활성화는 별도 계정 복구 정책으로 취급한다.
- 구현 내용: 1단계는 Firebase MEMBER 탈퇴 진입이다. 기존 Identity 인증과 RefreshSession 소유권에 더해 `WITHDRAWAL` 목적의 fresh Firebase ID Token·recent-auth와 `(project, UID)` mapping 일치를 검증하고, 내부 Mongo Transaction에서 User tombstone, 모든 RefreshSession 폐기, phone eligibility revoke와 lifecycle outbox를 commit한다. Firebase Admin 원격 호출을 Mongo Transaction 안에서 수행하지 않는다.
- 구현 내용: 2단계는 lifecycle worker다. outbox를 lease·retry·dead-letter/reconciliation으로 처리해 Firebase refresh revoke/user delete와 Apple 등 Provider별 요구사항을 완료한다. 외부 성공 전에는 FirebaseIdentity mapping을 WITHDRAWN owner에 유지해 login과 enrollment를 모두 fail-closed로 막고, 명시적인 withdrawal pending 오류를 반환한다.
- 구현 내용: 3단계는 cleanup finalize다. 외부 revoke/delete 성공을 확인한 뒤 별도 내부 Transaction에서 FirebaseIdentity·SocialIdentity를 정책대로 release/delete하고 PhoneIdentity·active aliases를 release하며 lifecycle을 CLEANED로 완료한다. User tombstone과 benefit abuse ledger/TrialClaim 성격의 기록은 별도 보존 정책에 따라 유지한다.
- 구현 내용: 4단계에서만 재가입을 연다. 새 Firebase 계정의 fresh proof로 기존 active/pending owner가 없고 이전 withdrawal cleanup이 완료됐음을 확인해 DIRECT_SIGNUP enrollment를 발급한다. WITHDRAWAL_PENDING·cleanup 실패·잔존 social/phone owner는 신규 enrollment로 자동 우회하지 않는다.
- 실행한 테스트와 결과: 설계 순서 정리만 수행해 Gradle 테스트는 실행하지 않았다. 앞선 exchange·withdrawal 구현과 ADR lifecycle 계약을 기준으로 검토했으며 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Firebase UID·provider subject·phone은 canonical userId나 자동 merge 키가 아니며 User tombstone은 보존한다. Firebase와 MongoDB를 하나의 분산 Transaction으로 가장하지 않고 outbox·idempotency·reconciliation을 사용한다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 탈퇴 내부 commit만 끝난 상태를 재가입 가능 상태로 보지 않는다. 외부 revoke/delete와 내부 mapping/phone cleanup까지 완료된 lifecycle terminal state가 재가입 gate다. 기능은 단계별 구현·테스트가 끝날 때까지 기본 비활성을 유지한다.
- 위험 요소: mapping을 너무 일찍 삭제하면 아직 유효한 Firebase credential로 중복 가입이 열리고, 너무 늦게 영구 보존하면 재가입이 계속 막힌다. 외부 삭제 성공 후 내부 finalize 실패, Provider revoke 실패, phone alias 일부 release 실패를 각각 재처리 가능한 상태로 남겨야 한다.
- 다음 작업: 사용자 승인을 받아 단계 1~3을 하나의 lifecycle Jira 또는 독립 Jira들로 구체화하고 완료 조건·오류 code·outbox schema·보존 정책을 먼저 확정한다. 해당 PR 병합과 staging failure/retry 검증 뒤 단계 4 재가입 Jira를 별도로 구현한다.

## 2026-08-24 — 동일 휴대폰 번호 재가입·기존 계정 연결 정책 정리

<!-- codex-turn:01a032b4-1168-7ec3-a837-8df06e0ade5b -->

- 날짜: 2026-08-24
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 없음
- 작업 목표: 회원 탈퇴 후 같은 휴대폰 번호로 새 계정을 만들 수 있는지와, 같은 번호를 근거로 기존 WITHDRAWN 계정에 다시 연결하는 정책이 타당한지 정리한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 기존 미커밋 변경과 새 계약 문서는 건드리지 않았으며, WORKLOG 과거 기록은 수정하거나 삭제하지 않고 이번 항목만 파일 끝에 append했다.
- 구현 내용: 같은 번호의 신규 가입은 withdrawal lifecycle의 Firebase revoke/delete와 PhoneIdentity·active alias release가 모두 완료되고, 새 Firebase account에서 번호를 다시 검증한 경우에 허용할 수 있다. 신규 User는 새 canonical UUID를 가지며 기존 WITHDRAWN tombstone과 서비스 데이터에 자동 연결하지 않는다.
- 구현 내용: 휴대폰 번호는 재할당될 수 있고 가족·조직이 공유할 수 있으므로 같은 번호라는 사실만으로 같은 사람을 확정할 수 없다. 따라서 phone fingerprint를 기존 userId 탐색, 계정 로그인, 자동 merge, tombstone reactivation 또는 데이터 복원 키로 쓰지 않는 기존 계약을 유지한다.
- 구현 내용: 기존 계정 복구가 제품 요구사항이면 재가입과 분리한 명시적 `withdrawal cancel/account recovery` 경로로 설계한다. 제한된 복구 기간, 원래 Firebase UID 또는 원래 Provider credential의 fresh recent-auth, 계정 상태·외부 revoke 진행 단계 확인을 요구하고 phone proof는 추가 요인으로만 사용한다. 외부 delete와 내부 cleanup이 끝난 terminal withdrawal은 복구하지 않고 신규 가입으로 처리한다.
- 구현 내용: 동일 번호의 무료혜택 재수령 방지 등 abuse 정책은 old/new User를 합치는 방식이 아니라 Identity PhoneIdentity와 분리된 benefit-scoped fingerprint/TrialClaim ledger로 처리한다. 이는 계정 데이터 자동 연결 없이 동일 번호 혜택 정책만 유지한다.
- 실행한 테스트와 결과: 정책 정리만 수행해 Gradle 테스트는 실행하지 않았다. 기존 social login 계획의 `phone은 로그인·자동 merge 키가 아님`, withdrawal cleanup 후 uniqueness 해제, PhoneIdentity와 benefit fingerprint domain separation 계약을 기준으로 검토했으며 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: canonical userId와 JWT `sub`는 서버 UUID이고 phone·fingerprint는 userId나 자동 merge 키가 아니다. raw phone을 장기 저장·로그하거나 교차 서비스에 전달하지 않으며 Firebase proof와 PhoneIdentity/benefit fingerprint 경계를 유지한다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 기본 정책은 `cleanup 완료 후 같은 검증 번호로 새 UUID 재가입 허용, 기존 계정 자동 연결 금지`다. 기존 계정 복구는 신규 가입과 다른 보안·제품 기능이며 phone 단독 proof로 제공하지 않는다.
- 위험 요소: 번호 재할당 직후 기존 계정을 자동 복구하면 새 번호 소유자에게 과거 개인정보·시험 데이터가 노출될 수 있다. 반대로 PhoneIdentity alias를 영구 점유하면 정상 재가입과 번호 재할당 사용자를 막으므로 cleanup terminal state와 보존·release 시점을 명확히 해야 한다.
- 다음 작업: lifecycle Jira 작성 전에 새 UUID 재가입을 기본으로 확정하고, 별도 복구 유예 기간을 제공할지 제품 결정을 받는다. 이후 phone release 시점, benefit ledger 보존 기간과 재가입 오류 code·테스트를 완료 조건에 포함한다.

## 2026-08-24 — Firebase·Social·Phone 탈퇴 cleanup 책임 설명

<!-- codex-turn:01a032b6-bc3a-7e51-a356-bb7ac1a7a37d -->

- 날짜: 2026-08-24
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 없음
- 작업 목표: 탈퇴 lifecycle에서 Firebase·Social·Phone cleanup이 각각 어떤 외부·내부 상태를 정리하고 무엇을 보존하는지 구체화한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 기존 미커밋 변경과 새 계약 문서는 건드리지 않았으며, WORKLOG 과거 기록은 수정하거나 삭제하지 않고 이번 항목만 파일 끝에 append했다.
- 구현 내용: Firebase cleanup은 내부 withdrawal outbox를 기준으로 Firebase refresh token revoke, Firebase User delete와 Apple 등 Provider별 account deletion revoke를 멱등·retry·dead-letter/reconciliation으로 처리한다. 외부 성공 전에는 기존 FirebaseIdentity mapping을 유지해 old credential의 login·enrollment를 fail-closed로 막는다.
- 구현 내용: Social cleanup은 old userId에 귀속된 SocialIdentity의 provider·subject unique 점유를 정책에 따라 release/tombstone/delete한다. cleanup 완료 전에는 같은 Google·Apple·Kakao subject가 새 userId로 연결되지 않게 하고, 완료 후에만 신규 가입 Transaction이 unique claim할 수 있게 한다. phone이나 email로 old user를 자동 탐색·merge하지 않는다.
- 구현 내용: Phone cleanup은 old user의 ACTIVE PhoneIdentity를 RELEASED 처리하고 해당 PhoneIdentity의 ACTIVE PhoneFingerprintAlias 전체에 releasedAt을 기록해 번호 uniqueness 점유를 해제한다. Firebase User delete로 Firebase 쪽 phone credential도 해제됐음을 확인하고, consumer-scoped phone eligibility binding에는 revoke revision/outbox를 전달한다.
- 구현 내용: User의 WITHDRAWN tombstone, 기존 canonical UUID와 법적·감사상 필요한 최소 이력은 보존한다. 동일 번호 무료혜택 중복 방지용 benefit-scoped fingerprint/TrialClaim ledger는 PhoneIdentity cleanup과 별도 보존 정책을 적용하며, 과거 시험·프로필 데이터를 새 User에 자동 연결하지 않는다.
- 구현 내용: 현재 애플리케이션에는 User tombstone CAS, RefreshSession 폐기와 phone eligibility revoke outbox까지만 존재한다. Firebase revoke/delete worker, FirebaseIdentity·SocialIdentity release, PhoneIdentity·alias withdrawal release와 lifecycle terminal state는 아직 구현되지 않았다.
- 실행한 테스트와 결과: 설명·정책 정리만 수행해 Gradle 테스트는 실행하지 않았다. 현재 withdrawal transaction과 Firebase ADR·social login 계획의 lifecycle/phone release 계약을 기준으로 검토했으며 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: raw phone·provider credential·Firebase ID Token을 저장·로그하지 않고 phone·provider subject를 canonical userId나 자동 merge 키로 사용하지 않는다. Firebase 외부 호출과 MongoDB 내부 변경을 하나의 Transaction으로 가장하지 않고 outbox·idempotency·reconciliation을 사용한다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: cleanup 완료는 Firebase 외부 revoke/delete와 내부 Social/Phone 점유 해제가 모두 성공한 terminal state다. User tombstone 생성만으로 cleanup 완료나 재가입 가능으로 판정하지 않는다.
- 위험 요소: Firebase 삭제만 성공하고 내부 mapping release가 실패하거나 그 반대가 되면 재가입 차단 또는 credential 재사용 위험이 생긴다. 각 단계의 상태·attempt·nextAttemptAt·lease·lastErrorCode를 저장하고 reconciliation으로 수렴시켜야 한다.
- 다음 작업: lifecycle Jira에서 cleanup state machine, outbox schema, Firebase/Provider adapter, SocialIdentity release 방식, PhoneIdentity/alias release CAS와 재가입 gate를 완료 조건으로 구체화한다.

## 2026-08-24 — 탈퇴 cleanup의 물리 삭제 범위와 안전 조건 검토

<!-- codex-turn:01a032ba-7c1f-79e2-bd1f-e5d4b81bd5d4 -->

- 날짜: 2026-08-24
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 없음
- 작업 목표: Firebase·Social·Phone cleanup에서 실제 물리 삭제가 필요한 대상과 soft release가 필요한 대상을 현재 entity/index 구조와 탈퇴 lifecycle 순서에 맞춰 검토한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 기존 미커밋 변경과 새 계약 문서는 건드리지 않았으며, WORKLOG 과거 기록은 수정하거나 삭제하지 않고 이번 항목만 파일 끝에 append했다.
- 구현 내용: User는 물리 삭제하지 않고 WITHDRAWN tombstone을 유지하며 RefreshSession은 revoke 후 기존 TTL·보존 정책을 따른다. Firebase User는 내부 탈퇴 commit과 session revoke 이후 retry 가능한 lifecycle worker에서 실제 revoke/delete할 수 있지만, 외부 성공 전 내부 FirebaseIdentity mapping을 제거하지 않는다.
- 구현 내용: 현재 FirebaseIdentity와 SocialIdentity에는 lifecycle status·releasedAt이 없고 unique index도 unconditional이어서 retained tombstone과 unique 점유 해제를 동시에 지원하지 않는다. 안전한 cleanup을 위해 ACTIVE/RELEASED와 partial unique index를 도입하거나, terminal cleanup 뒤 별도 durable lifecycle audit를 보존한 상태에서만 mapping을 물리 삭제해야 한다.
- 구현 내용: PhoneIdentity와 PhoneFingerprintAlias에는 이미 ACTIVE/RELEASED, releasedAt 및 ACTIVE 대상 partial unique index가 있으므로 기본 처리는 delete가 아니라 soft release다. 번호 관련 혜택 중복 방지 ledger는 계정 mapping과 분리해 별도 보존 정책을 적용한다.
- 실행한 테스트와 결과: entity와 index 및 탈퇴 ADR을 정적으로 검토한 분석 작업이므로 Gradle 테스트는 실행하지 않았다. 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Firebase UID·provider subject·phone은 canonical userId나 자동 merge 키가 아니며 User tombstone과 새 UUID 재가입 경계를 유지한다. 외부 Firebase 삭제와 MongoDB 변경을 하나의 Transaction으로 가장하지 않고 outbox·retry·reconciliation을 사용하며 Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: cleanup은 전부 hard delete라는 뜻이 아니다. Firebase 외부 계정 삭제는 가능하지만 내부 mapping의 삭제·release는 내부 탈퇴 확정, 외부 revoke/delete와 Provider 의무 완료, phone release, durable audit 확보가 끝난 terminal 단계에서만 수행한다. 현재 schema에서 FirebaseIdentity·SocialIdentity에 단순 repository delete를 추가하는 것은 안전한 완성안이 아니다.
- 위험 요소: mapping을 너무 일찍 삭제하면 아직 유효한 Firebase credential이 신규 enrollment를 만들 수 있고, unconditional unique mapping을 tombstone으로 계속 유지하면 정상 재가입이 영구 차단된다. 단계 실패 시 재처리할 lifecycle state와 reconciliation 기록이 없으면 외부·내부 상태가 갈라질 수 있다.
- 다음 작업: lifecycle Jira에서 FirebaseIdentity·SocialIdentity의 release 모델과 partial unique index 또는 audited hard-delete 정책, cleanup terminal 조건, 보존 기간과 reconciliation 절차를 명시한 뒤 구현한다.

## 2026-08-24 — 탈퇴 시 Firebase 외부 User 보존 가능성 검토

<!-- codex-turn:01a032c4-502b-7033-a63d-b1e459fe8ce0 -->

- 날짜: 2026-08-24
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 없음
- 작업 목표: 회원 탈퇴 후 Firebase 외부 User를 삭제하지 않고 보존해도 되는지 로그인 차단, 재가입, 개인정보와 Provider 의무 관점에서 검토한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 기존 미커밋 변경과 새 계약 문서는 건드리지 않았으며 WORKLOG 과거 기록은 수정하거나 삭제하지 않고 이번 항목만 파일 끝에 append했다.
- 구현 내용: Firebase User를 즉시 물리 삭제하지 않고 disabled 처리와 refresh token revoke로 credential 사용을 차단하는 기술적 선택은 가능하다. 다만 해당 record에 email·phone·provider linkage 등 개인정보와 unique 점유가 남으므로 이를 탈퇴 cleanup 완료와 동일하게 취급할 수는 없다.
- 구현 내용: 외부 User를 계속 보존하면 같은 Firebase UID의 재활성화와 새 Firebase User 생성 중 제품 정책을 명확히 해야 한다. 새 UUID 재가입 정책에서는 기존 Firebase email·phone/provider 점유가 신규 생성·연결을 막을 수 있으므로 terminal cleanup 전에 삭제 또는 Firebase 측 식별정보 정리 절차가 필요하다.
- 구현 내용: 보존을 택하려면 법적 근거와 보존 기간, disabled·revoke 확인, 로그인 fail-closed, 재가입 차단 또는 별도 복구 경로, Provider별 계정 삭제·철회 의무, 만료 후 실제 삭제와 reconciliation을 명시해야 한다. 단순히 비용이나 구현 편의를 위해 무기한 보존하는 것은 권장하지 않는다.
- 실행한 테스트와 결과: 정책 분석만 수행해 Gradle 테스트는 실행하지 않았다. 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Firebase UID·email·phone·provider subject를 canonical userId나 자동 merge 키로 사용하지 않고 User WITHDRAWN tombstone과 새 UUID 경계를 유지한다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 짧은 유예·재처리 기간에는 Firebase User를 disabled·revoked 상태로 유지할 수 있지만, 완전 탈퇴와 새 계정 재가입을 기본 정책으로 삼는다면 외부 cleanup terminal 단계에서 삭제하는 것이 가장 단순하고 안전하다. 장기 보존은 별도의 법적·제품 보존 정책이 있을 때만 허용한다.
- 위험 요소: revoke만 하고 disable을 누락하면 기존 credential 사용 가능성이 남고, disabled User를 무기한 유지하면 개인정보 삭제 요구와 Firebase email·phone/provider uniqueness 때문에 재가입이 막힐 수 있다. Apple 등 Provider별 철회 요구는 Firebase record 보존 여부와 별개로 처리해야 한다.
- 다음 작업: withdrawal lifecycle 완료 조건에 Firebase disabled/revoke, 외부 삭제 시점, 유예 기간, Provider별 revoke, 삭제 실패 retry와 재가입 gate를 명시한다.

## 2026-08-24 — Firebase 외부 User 삭제 기반 회원 탈퇴 정책 확정

<!-- codex-turn:01a032c7-849a-7ca0-a38a-d676d7bf61c1 -->

- 날짜: 2026-08-24
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 없음
- 작업 목표: 회원 탈퇴 시 Firebase 외부 User를 최종 삭제하는 것으로 결정하고 탈퇴 인증, 내부 확정, 외부 cleanup, 내부 release, 재가입 gate와 실패 처리 정책을 정리한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 기존 미커밋 변경과 새 계약 문서는 건드리지 않았으며 WORKLOG 과거 기록은 수정하거나 삭제하지 않고 이번 항목만 파일 끝에 append했다.
- 구현 내용: 탈퇴 요청은 로그인된 canonical user와 목적이 WITHDRAWAL인 fresh Firebase proof/recent-auth를 요구하고 Firebase project·UID mapping과 요청 user 소유권을 검증한다. phone은 본인 확인의 보조 수단일 수 있으나 탈퇴 대상 탐색이나 자동 merge 기준으로 사용하지 않는다.
- 구현 내용: 첫 Mongo Transaction에서 User를 WITHDRAWN tombstone으로 전환하고 모든 RefreshSession을 revoke하며 phone eligibility revoke와 withdrawal lifecycle outbox를 저장한다. 이 commit 이후 로그인과 신규 enrollment는 fail-closed로 차단하고 Firebase 외부 호출 실패가 내부 탈퇴를 되돌리지 않게 한다.
- 구현 내용: lifecycle worker는 Firebase User disable, refresh token revoke, Provider별 철회 의무, Firebase User delete를 멱등·retry 가능한 단계로 수행한다. 외부 삭제가 확인될 때까지 FirebaseIdentity mapping을 유지하며 단계별 상태·attempt·lease·nextAttemptAt·안전한 error code를 기록하고 dead-letter/reconciliation을 지원한다.
- 구현 내용: 외부 삭제 성공 후 별도 Mongo Transaction에서 FirebaseIdentity와 SocialIdentity의 active unique 점유를 release하고 PhoneIdentity·PhoneFingerprintAlias를 RELEASED 처리한 뒤 lifecycle을 CLEANED terminal state로 전환한다. User WITHDRAWN tombstone과 법적·감사상 필요한 최소 기록 및 benefit-scoped abuse ledger는 별도 보존 정책에 따라 유지한다.
- 구현 내용: CLEANED 전에는 동일 Firebase credential·provider subject·phone을 이용한 재가입을 허용하지 않는다. CLEANED 후 새 Firebase User의 fresh proof와 새 전화 검증으로 새 canonical UUID User 가입을 허용하되 과거 User·프로필·시험 데이터에 자동 연결하지 않는다. 탈퇴 철회나 기존 계정 복구는 기본 정책에서 제공하지 않고 필요하면 별도 기능으로 설계한다.
- 실행한 테스트와 결과: 정책 정리만 수행해 Gradle 테스트는 실행하지 않았다. 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: canonical userId와 JWT sub는 서버 UUID이고 Firebase UID·provider subject·phone은 userId·로그인 탐색·자동 merge 키가 아니다. Firebase 외부 작업과 MongoDB 내부 변경을 하나의 Transaction으로 가장하지 않고 outbox·idempotency·retry·reconciliation을 사용한다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 완전 탈퇴의 terminal 조건은 내부 WITHDRAWN만이 아니라 Firebase User 삭제, Provider별 의무 완료, Firebase/Social mapping과 Phone 점유 release가 모두 성공해 lifecycle이 CLEANED가 된 상태다. Firebase 외부 User는 유예 없이 worker가 가능한 즉시 삭제하되 장애 시 안전하게 재시도한다.
- 위험 요소: Firebase 삭제 성공 후 내부 release 실패 또는 그 반대 순서의 잘못된 구현은 재가입 차단이나 credential 재사용을 만들 수 있다. FirebaseIdentity·SocialIdentity는 현재 release 상태와 partial unique index가 없어 구현 전에 schema/index 정책을 확정해야 하며 개인정보·법정 보존 기간은 서비스 법무 정책과 대조해야 한다.
- 다음 작업: 별도 Jira에서 withdrawal lifecycle state/outbox, fresh Firebase withdrawal 인증, Firebase Admin delete worker, Provider revoke, Firebase/Social release schema, Phone release CAS, 재가입 gate와 단계별 장애·동시성 테스트를 구현한다.

## 2026-08-24 — Entitlement 부재 시 phone eligibility outbox 정책 정리

<!-- codex-turn:01a032ca-a88a-7182-90b2-5767daa1b7dd -->

- 날짜: 2026-08-24
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 없음
- 작업 목표: 결제·Entitlement 기능을 후속으로 미룬 상황에서 PhoneEligibilityBindingOutbox의 전달 대상과 Identity의 임시 소유 여부를 현재 ADR·코드 기준으로 판단한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 기존 미커밋 변경과 새 계약 문서는 건드리지 않았으며 WORKLOG 과거 기록은 수정하거나 삭제하지 않고 이번 항목만 파일 끝에 append했다.
- 구현 내용: ADR-002의 v1 consumer는 별도 Entitlement/Billing 서비스이며 Learning Core가 직접 소비하지 않는다. binding 이벤트는 혜택 지급이 아니라 consumer-scoped phone verification binding이고 TrialClaim·UserEntitlement·중복 혜택 정책은 consumer 소유로 정의되어 있다.
- 구현 내용: Entitlement/Billing consumer와 전송 endpoint가 없는 현재 단계에서는 publisher와 eligibility binding 기능을 활성화하지 않고 VERIFIED/REVOKED outbox를 생성하지 않는 것이 맞다. 소비자 없는 outbox를 계속 적재하거나 성공으로 간주해 폐기하지 않는다.
- 구현 내용: Identity는 탈퇴 시 자신의 PhoneIdentity와 PhoneFingerprintAlias를 RELEASED 처리하는 책임만 수행한다. 이전에 외부 consumer로 VERIFIED binding을 발행한 적이 없는 환경이라면 해당 consumer를 위한 REVOKED 이벤트도 필요하지 않다. 반대로 production에서 VERIFIED를 한 번이라도 발행했다면 같은 consumer에 revision이 높은 REVOKED를 반드시 전달해야 한다.
- 구현 내용: 결제·Entitlement 도입 시 consumer inbox/current binding/high-water와 publisher를 먼저 배포하고 그 이후 eligibility producer를 활성화한다. 도입 전 가입자의 혜택 자격이 필요하면 저장되지 않은 phone 원문을 복원하지 말고 사용 시점에 전화번호를 다시 검증해 binding을 생성하는 migration/onboarding 정책을 사용한다.
- 실행한 테스트와 결과: ADR·계약과 현재 producer/publisher/withdrawal 코드를 정적으로 검토한 분석 작업이므로 Gradle 테스트는 실행하지 않았다. 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Identity는 사용자·PhoneIdentity·검증 lifecycle을 소유하지만 TrialClaim·UserEntitlement·결제·무료시험 정책을 소유하지 않는다. phone fingerprint domain/key를 혜택 ledger와 공유하거나 phone을 canonical userId·자동 merge 키로 사용하지 않으며 Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: consumer가 없는 동안 PhoneEligibilityBindingOutbox는 탈퇴의 필수 산출물이 아니다. Identity가 임시로 혜택 상태를 소유하지 않고 eligibility integration 전체를 disabled로 유지하며, 탈퇴 cleanup은 Identity 내부 phone release와 Firebase/Social cleanup만으로 구성한다.
- 위험 요소: consumer 없이 outbox만 생성하면 PENDING/DEAD_LETTER가 누적되고 운영상 cleanup 완료 여부가 왜곡된다. 반대로 과거 VERIFIED 발행 이력이 있는데 REVOKED를 생략하면 향후 consumer에 탈퇴 사용자의 active binding이 남으므로 deployment history와 scope별 high-water를 확인해야 한다.
- 다음 작업: 탈퇴 lifecycle 구현 Jira에서 phone eligibility revoke를 조건부 단계로 정의하고, production에서 eligibility 기능이 활성화된 적이 없는지 확인한다. Entitlement 도입 Jira에서는 consumer 선배포, producer activation gate와 기존 가입자의 fresh phone verification 정책을 포함한다.

## 2026-08-24 — 전화번호당 무료 모의고사 1회 제공의 최소 선행 조건 정리

<!-- codex-turn:01a032cd-3a23-7cf3-819c-8714d7290a6c -->

- 날짜: 2026-08-24
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 없음
- 작업 목표: 결제 기능과 eligibility consumer를 미룬 상태에서 전화번호당 무료 모의고사 1회 정책을 시행할 수 있는지와 필요한 최소 구성요소를 정리한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 기존 미커밋 변경과 새 계약 문서는 건드리지 않았으며 WORKLOG 과거 기록은 수정하거나 삭제하지 않고 이번 항목만 파일 끝에 append했다.
- 구현 내용: 별도의 durable claim ledger가 없으면 탈퇴·재가입이나 새 userId 생성 후 동일 번호의 과거 무료혜택 사용 여부를 판정할 수 없으므로 전화번호당 1회 정책을 보장할 수 없다. User나 PhoneIdentity의 현재 연결 상태만으로는 과거 claim 이력을 대체할 수 없다.
- 구현 내용: 결제 전체를 먼저 구현할 필요는 없지만 최소 Entitlement bounded context로 consumer inbox/current verified binding, benefit-scoped versioned phone fingerprint candidate, TrialClaim unique ledger와 무료시험 reserve/confirm/cancel·reconciliation을 구현해야 한다. 이 원장은 별도 서비스가 이상적이며 초기 배포 단위가 같더라도 Identity domain/collection/transaction과 분리한다.
- 구현 내용: Identity는 phone verification 시 consumer-scoped eligibility binding을 전달하고 탈퇴·번호 교체 시 revoke를 전달한다. TrialClaim은 계정 탈퇴 후에도 정책상 필요한 기간 보존해 동일 번호의 새 UUID 계정에 과거 계정 데이터를 연결하지 않으면서 중복 혜택만 차단한다.
- 실행한 테스트와 결과: 정책 분석만 수행해 Gradle 테스트는 실행하지 않았다. 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Identity는 phone 검증과 계정 연결만 소유하고 시험·TrialClaim·UserEntitlement를 소유하지 않는다. phone fingerprint는 benefit scope와 key version으로 domain separation하고 canonical userId·로그인·자동 merge 키로 사용하지 않는다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 전화번호당 무료 모의고사 1회를 출시하려면 결제 기능과 무관하게 최소 Entitlement/TrialClaim 트랙은 선행해야 한다. 이 트랙을 미루면 무료시험 출시도 미루거나 전화번호당 1회 보장을 포기해야 하며 Identity에 임시 claim 필드를 추가하지 않는다.
- 위험 요소: 단순 boolean을 User나 PhoneIdentity에 저장하면 탈퇴·재가입 우회, 동시 요청 이중 지급, 시험 생성 실패 시 혜택 소진 여부 불일치와 향후 Billing 분리 migration 문제가 생긴다. 번호 재할당과 fingerprint key rotation을 고려한 보존·조회 정책도 필요하다.
- 다음 작업: 무료시험 출시 범위를 확정한 뒤 별도 Jira로 최소 Entitlement consumer, TrialClaim unique, reserve/confirm/cancel과 Identity eligibility publisher activation gate를 구현한다.

## 2026-08-24 — Billing 서버의 최소 무료혜택 Entitlement 범위 확정

- 날짜: 2026-08-24
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 없음
- 작업 목표: 전화번호당 무료 모의고사 1회를 위해 Billing 서버에 결제 기능보다 먼저 구현할 최소 Entitlement 범위와 서비스별 책임을 확정한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 기존 미커밋 변경과 새 계약 문서는 건드리지 않았으며 WORKLOG 과거 기록은 수정하거나 삭제하지 않고 이번 항목만 파일 끝에 append했다.
- 구현 내용: Billing 서버에는 결제수단·주문·PG 연동 없이 eligibility event inbox, current phone benefit binding과 revision high-water, benefit-scoped fingerprint candidate, TrialClaim unique ledger, 무료시험 entitlement reservation의 reserve/confirm/cancel과 reconciliation만 우선 구현한다.
- 구현 내용: Identity는 phone verification과 VERIFIED/REVOKED binding event 생산만 소유하고 Billing은 전화번호별 혜택 중복 판정과 claim/reservation을 소유한다. Learning Core는 Billing의 reserve 성공 뒤 시험을 만들고 생성 결과에 따라 confirm 또는 cancel하며 phone fingerprint를 직접 저장하지 않는다.
- 실행한 테스트와 결과: 책임과 최소 범위 정리만 수행해 Gradle 테스트는 실행하지 않았다. 문서 변경에 대해 `git diff --check`와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Identity는 시험·TrialClaim·Entitlement를 소유하지 않고 Billing은 raw phone을 받거나 phone fingerprint를 canonical userId·자동 merge 키로 사용하지 않는다. Learning Core의 시험 책임과 Identity의 UUID/JWT 계약을 유지하며 Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: Billing 서버를 먼저 완성하는 것이 아니라 무료시험 출시에 필요한 최소 Entitlement vertical slice만 선행한다. 실제 결제·구독·환불·PG 기능은 후속 단계로 남긴다.
- 위험 요소: 최소 범위라도 동시 reserve, 시험 생성 결과 불명 timeout, cancel/confirm 중복, fingerprint key rotation과 탈퇴 후 claim 보존을 생략하면 전화번호당 1회 보장이 깨질 수 있다.
- 다음 작업: Billing 저장소에서 별도 Jira로 최소 entity/API/event contract와 완료 조건을 작성하고 consumer를 먼저 배포한 뒤 Identity eligibility publisher를 활성화한다.

## 2026-08-24 — Billing 최소 Entitlement 결정 기록 동기화

<!-- codex-turn:01a032cf-7429-7f72-b7bc-43107ee3aea0 -->

- 날짜: 2026-08-24
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 없음
- 작업 목표: Billing 서버에 무료 모의고사용 최소 Entitlement vertical slice를 먼저 구현한다는 현재 turn 결정을 Hook 식별자와 함께 작업 기록에 동기화한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 WORKLOG 과거 기록은 수정하거나 삭제하지 않고 이번 항목만 파일 끝에 append했다.
- 구현 내용: Identity는 phone verification과 VERIFIED/REVOKED binding 생산, Billing은 TrialClaim unique와 reserve/confirm/cancel, Learning Core는 시험 생성을 담당한다는 경계를 유지했다. 결제수단·PG·주문·구독·환불은 후속 범위다.
- 실행한 테스트와 결과: 기록 동기화만 수행해 Gradle 테스트는 실행하지 않았다. `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Identity에 시험·혜택·결제 책임을 추가하지 않고 raw phone과 Identity fingerprint를 Billing claim key로 전달하지 않는다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 앞서 확정한 최소 Entitlement 선행 방침을 변경하지 않았으며 Hook이 요구한 현재 turn 기록만 추가했다.
- 위험 요소: 실제 구현 전 Billing consumer와 Identity producer의 event schema, key rotation, revision과 장애 복구 계약을 함께 확정해야 한다.
- 다음 작업: Billing 저장소의 Jira와 계약을 먼저 준비한 뒤 consumer 선배포 및 Identity publisher 활성화 순서로 진행한다.

## 2026-08-24 — 탈퇴·재가입·무료시험 최소 Billing 최종 정책 통합

<!-- codex-turn:01a032d2-5ecf-7201-b038-40f1b33853e0 -->

- 날짜: 2026-08-24
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 없음
- 작업 목표: Firebase 외부 User 삭제 기반 탈퇴, 동일 전화번호의 새 UUID 재가입, 전화번호당 무료 모의고사 1회와 Billing 최소 Entitlement 선행 방침을 하나의 최종 정책으로 통합한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 기존 미커밋 변경과 새 계약 문서는 건드리지 않았으며 WORKLOG 과거 기록은 수정하거나 삭제하지 않고 이번 항목만 파일 끝에 append했다.
- 구현 내용: 탈퇴는 fresh WITHDRAWAL Firebase proof로 소유권을 검증하고 내부 Transaction에서 User WITHDRAWN, 모든 RefreshSession revoke와 withdrawal lifecycle outbox를 먼저 확정한다. worker가 Firebase disable·refresh revoke·Provider별 철회·Firebase User delete를 수행한 뒤 Firebase/Social mapping과 PhoneIdentity/aliases를 release하고 CLEANED로 완료한다.
- 구현 내용: CLEANED 전에는 로그인·신규 enrollment·재가입을 차단한다. CLEANED 후 같은 전화번호를 다시 검증해 새 Firebase User와 새 canonical UUID User로 가입할 수 있지만 기존 User·프로필·시험 데이터를 자동 연결하거나 phone을 복구·merge 키로 사용하지 않는다.
- 구현 내용: 전화번호당 무료 모의고사 1회를 위해 Billing 서버에 결제 전체보다 먼저 최소 Entitlement slice를 구현한다. Billing은 eligibility inbox/current binding/high-water, benefit-scoped fingerprint, TrialClaim unique, reserve/confirm/cancel과 reconciliation을 소유하고 Identity는 VERIFIED/REVOKED binding 생산, Learning Core는 시험 생성을 담당한다.
- 구현 내용: TrialClaim은 계정 탈퇴와 PhoneIdentity release 후에도 별도 정책 기간 유지해 동일 번호의 새 계정에 과거 개인정보를 연결하지 않으면서 혜택 중복만 차단한다. PG·결제수단·주문·구독·환불은 후속 범위로 남기며 Billing consumer를 먼저 배포한 뒤 Identity publisher를 활성화한다.
- 실행한 테스트와 결과: 최종 정책 통합만 수행해 Gradle 테스트는 실행하지 않았다. 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: canonical userId/JWT sub는 서버 UUID이고 Firebase UID·provider subject·phone은 userId·로그인 탐색·자동 merge 키가 아니다. Identity는 시험·TrialClaim·Entitlement를 소유하지 않고 Learning Core는 phone 정보를 저장하지 않으며 Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 완전 탈퇴 terminal은 Firebase 외부 삭제와 모든 내부 점유 release가 끝난 CLEANED다. 무료시험 출시에 필요한 최소 Entitlement는 Billing에 선행 구현하되 실제 결제 기능은 미룬다. consumer 준비 전 eligibility outbox 생산·publisher는 활성화하지 않는다.
- 위험 요소: Firebase/Social release schema 미비, 외부 삭제와 내부 release 사이의 부분 실패, Billing reserve 결과 불명, fingerprint key rotation과 TrialClaim 보존 기간을 해결하지 않으면 재가입 또는 전화번호당 1회 정책이 깨질 수 있다.
- 다음 작업: Identity withdrawal lifecycle Jira와 Billing 최소 Entitlement Jira를 분리해 contract와 완료 조건을 작성하고 Billing consumer 선배포, Identity producer 활성화, Learning Core reserve/confirm/cancel 연동 순서로 구현한다.

## 2026-08-24 — 탈퇴 후 동일 SNS 계정 로그인·재가입 동작 확정

<!-- codex-turn:01a032d9-4e4f-7283-b061-5db9908506a2 -->

- 날짜: 2026-08-24
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 없음
- 작업 목표: Firebase 외부 User 삭제 기반 탈퇴 후 동일 Google·Kakao·Apple 계정으로 다시 SNS 인증할 때 cleanup 상태별 로그인과 재가입 동작을 확정한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 기존 미커밋 변경과 새 계약 문서는 건드리지 않았으며 WORKLOG 과거 기록은 수정하거나 삭제하지 않고 이번 항목만 파일 끝에 append했다.
- 구현 내용: Firebase User 삭제는 Google·Kakao·Apple Provider의 원본 계정을 삭제하지 않는다. 사용자가 같은 SNS credential로 Firebase 인증을 다시 수행하면 Firebase 측에서 새 User·UID가 만들어질 수 있으며 Identity는 이를 기존 canonical User 로그인으로 취급하지 않는다.
- 구현 내용: withdrawal lifecycle이 CLEANED 전이면 기존 mapping 또는 withdrawal tombstone/lifecycle을 기준으로 로그인과 신규 enrollment를 WITHDRAWAL_PENDING 성격의 오류로 fail-closed 차단한다. 외부 삭제나 내부 mapping release가 끝나기 전에 같은 Provider subject가 새 User를 claim하지 못하게 한다.
- 구현 내용: CLEANED 후에는 기존 FirebaseIdentity·SocialIdentity의 active unique 점유가 해제되어 있으므로 같은 SNS 계정의 fresh proof는 미등록 principal로 판정한다. 즉시 로그인 Token을 발급하지 않고 신규 회원가입 enrollment로 보내 전화번호 재검증과 약관 동의를 거친 뒤 새 canonical UUID User, 새 FirebaseIdentity·SocialIdentity를 생성한다.
- 구현 내용: 새 계정은 기존 WITHDRAWN User·프로필·시험 데이터와 자동 연결하지 않는다. Billing의 TrialClaim은 별도 보존되므로 같은 전화번호로 재가입해도 이미 사용한 무료 모의고사 혜택은 다시 지급되지 않는다.
- 실행한 테스트와 결과: 정책 동작 정리만 수행해 Gradle 테스트는 실행하지 않았다. 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Provider account·Firebase UID·provider subject·phone을 canonical userId나 자동 merge/복구 키로 사용하지 않고 새 가입마다 서버 UUID를 발급한다. Firebase 외부 User 삭제와 Provider별 철회 의무를 구분하며 Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 동일 SNS 계정 재사용은 기존 계정 로그인이 아니라 CLEANED 후 가능한 신규 가입이다. 기본 정책에는 탈퇴 철회·기존 User 재활성화를 포함하지 않으며 필요하면 별도 account recovery 기능으로 설계한다.
- 위험 요소: SocialIdentity를 영구 unique tombstone으로 남기면 같은 SNS 재가입이 막히고 너무 일찍 release하면 cleanup 중 중복 가입이 열린다. Firebase가 재인증 때 부여하는 UID 변화만 신뢰하지 말고 Provider subject의 withdrawal terminal/release 상태도 함께 fencing해야 한다.
- 다음 작업: withdrawal lifecycle과 Firebase exchange/signup Jira에 CLEANED gate, 동일 provider subject 재가입, cleanup 중 차단, 새 UID·새 UUID 생성과 이전 데이터 비연결 테스트를 포함한다.

## 2026-08-24 — 인증 전체 흐름·정상 사용자 오류 가능성 감사

<!-- codex-turn:01a032de-2abf-7e93-a61c-3a83cf881cc4 -->

- 날짜: 2026-08-24
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 없음
- 작업 목표: Guest 생성, Firebase/SNS 교환·가입·승격·merge·인증수단 동기화, Session 재발급·로그아웃, phone과 탈퇴·재가입 흐름을 실제 코드와 테스트로 추적해 정상적인 사용자 행위가 교착·일반 오류·세션 유실로 이어질 수 있는 지점을 찾는다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 기존 미커밋 변경과 새 계약 문서는 건드리지 않았으며 WORKLOG 과거 기록은 수정하거나 삭제하지 않고 이번 항목만 파일 끝에 append했다.
- 구현 내용: 현재 정상 경로는 Firebase exchange가 기존 FirebaseIdentity의 ACTIVE MEMBER에는 Token을 발급하고 미등록 UID에는 enrollment를 발급하며, signup이 phone·동의와 attempt를 검증해 User·Firebase/Social/Phone identity·eligibility outbox·Session을 Transaction으로 생성한다. Guest는 unowned credential이면 같은 UUID 승격, existing active owner면 explicit merge를 수행한다.
- 구현 내용: 출시 차단 수준으로 federated MEMBER 탈퇴 재인증 경로가 없어 `hasLocalCredential=false`가 `INVALID_WITHDRAWAL_CREDENTIALS`로 끝나고, withdrawal Transaction에는 Firebase revoke/delete·lifecycle outbox·Firebase/Social/Phone release가 없다. 따라서 수동 WITHDRAWN 뒤 같은 UID는 ACCOUNT_NOT_ACTIVE, 새 UID의 같은 provider subject는 SOCIAL_IDENTITY_CONFLICT, 같은 phone은 PHONE_ALREADY_LINKED가 되어 CLEANED 재가입 정책을 구현하지 못한다.
- 구현 내용: Firebase Admin port가 verify만 제공해 disable·refresh revoke·delete와 가입 중단 Firebase User cleanup을 실행할 수 없다. 사용자가 phone을 Firebase에 link한 뒤 가입을 중단하면 내부 User가 없어도 Firebase 쪽 번호 점유가 남을 수 있다. withdrawal pending/cleaned 상태와 전용 오류도 없어 클라이언트가 대기·재시도·신규 가입을 구분할 수 없다.
- 구현 내용: auth-method sync는 현재 FirebaseIdentity와 같은 UID의 누락 SocialIdentity만 추가하고 provider 제거와 phone replacement를 반영하지 않는다. 사용자가 Firebase에서 provider unlink 또는 번호 변경을 하면 Identity에는 stale SocialIdentity·PhoneIdentity가 남아 이전 provider/번호가 계속 점유되고 Billing VERIFIED/REVOKED 상태도 어긋날 수 있다. 외부 Firebase User를 재생성해 새 UID가 된 ACTIVE MEMBER도 기존 SocialIdentity owner 때문에 exchange에서 conflict가 나며 명시적 account recovery/rebind 경로가 없다.
- 구현 내용: logout-all은 내부 RefreshSession만 폐기해 Firebase refresh revoke를 하지 않으므로 남은 Firebase credential로 exchange를 호출하면 즉시 새 Identity Session을 만들 수 있다. withdrawal과 logout 뒤 stateless Access Token도 만료 전 Learning Core에서 유효하며 UserWithdrawn/deny consumer는 없다.
- 구현 내용: Refresh Token rotation은 기존 Session을 ROTATED 저장한 뒤 새 Session을 별도 저장하므로 중간 실패 시 현재 Session을 잃는다. 성공 응답 유실 후 정상 네트워크 재시도도 old token reuse로 판정해 새 Session을 포함한 모든 활성 Session을 폐기한다. Guest 생성 역시 commit 후 응답 유실 시 같은 installation 재시도가 GUEST_ALREADY_EXISTS로 고정되어 Token 복구 경로가 없다.
- 구현 내용: Firebase signup·Guest upgrade·merge의 commit 후 응답 유실은 같은 finalize 재시도에는 conflict/not-allowed를 반환하지만 Firebase exchange로 복구할 수 있어 클라이언트 fallback 계약이 필요하다. 반복 exchange는 매번 새 RefreshSession을 만들며 기본 login auth_time 15분 정책은 Firebase persistent session 사용자가 자주 recent-auth 오류를 만날 수 있어 제품 UX 확인이 필요하다.
- 구현 내용: Firebase signup/upgrade는 PhoneEligibilityFingerprintHasher와 eligibility repositories를 필수 주입하고 항상 VERIFIED outbox를 저장한다. 따라서 Billing consumer·publisher·key가 준비되기 전 Firebase 가입을 켜면 기동 실패 또는 미전달 outbox 누적이 생길 수 있으며, 최종 합의대로 Billing consumer 선배포를 실제 activation gate로 강제해야 한다.
- 실행한 테스트와 결과: 관련 5개 test class(`FirebaseExchangeServiceTests`, `FirebaseAuthenticationConfigurationTests`, `UserWithdrawalServiceTests`, `RefreshTokenUseCaseServicesTests`, `GuestAuthServiceTests`)를 선택 실행했고 `BUILD SUCCESSFUL`이었다. 이 테스트들은 WITHDRAWN 로그인 거절, rotated token reuse 시 전체 Session 폐기와 Guest response-loss retry conflict 등 현재 동작을 고정하지만 withdrawal cleanup·rejoin·logout-all Firebase revoke·unlink/phone replacement 복구는 검증하지 않는다. 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: email·phone만으로 기존 User를 자동 merge/recover하지 않고 canonical userId/JWT sub는 서버 UUID로 유지한다. Identity·Billing·Learning Core 책임과 Firebase/Mongo 분산 작업의 outbox·retry 경계를 지키며 Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 구현 우선순위는 (1) federated withdrawal 인증과 lifecycle/CLEANED/rejoin fencing, (2) Firebase Admin revoke/delete와 abandoned enrollment cleanup, (3) Billing consumer activation gate, (4) logout-all coordination과 Refresh rotation/Guest response-loss 복구, (5) provider unlink·phone replacement·active account rebind다. 현재 production Firebase/merge/publisher flag는 이 선결 조건과 staging E2E 전까지 비활성으로 유지한다.
- 위험 요소: 단위 테스트가 성공해도 현재 의도된 conflict를 검증하는 경우가 있어 정상 사용자 복구 UX까지 증명하지 않는다. 실제 Firebase/mobile, Mongo replica-set Transaction, Billing/Learning consumer, timeout과 응답 유실을 포함한 staging E2E 없이는 부분 실패 수렴을 확인할 수 없다.
- 다음 작업: 발견 사항을 독립 Jira로 나누되 먼저 withdrawal lifecycle 상태·오류·Admin port·release index와 재가입 테스트를 명세하고, Billing consumer 최소 slice와 함께 배포 gate를 확정한다. 이후 Session idempotency/grace와 auth-method lifecycle Jira를 진행한다.

## 2026-08-24 — Firebase 인증 후속 10단계 구현 순서 문서화

<!-- codex-turn:01a032e9-b93a-7913-8043-2062da0a11e8 -->

- 날짜: 2026-08-24
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 없음
- 작업 목표: 인증 흐름 감사에서 확정한 탈퇴·cleanup·Billing·Session·인증수단 복구 작업 10개를 순서대로 구현해야 하는 고정 체크리스트로 저장한다.
- 변경 파일: `docs/contracts/firebase-auth-follow-up-implementation-order.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 기존 미커밋 변경과 새 계약 문서는 건드리지 않았으며 WORKLOG 과거 기록은 수정하거나 삭제하지 않고 이번 항목만 파일 끝에 append했다.
- 구현 내용: Firebase/SNS 탈퇴 재인증과 lifecycle부터 Firebase revoke/delete, 내부 identity release와 CLEANED gate, 가입 중단 cleanup, Billing 최소 consumer, logout-all revoke, Refresh rotation 복구, provider/phone lifecycle, Guest response-loss 복구, ACTIVE 회원 rebind까지 1~10번 체크리스트로 고정했다.
- 구현 내용: 앞 단계의 코드·테스트·운영 계약 완료 전 다음 production 기능을 활성화하지 않고, Billing consumer 선배포, 외부·내부 부분 실패, 응답 유실, retry·reconciliation과 staging E2E를 각 단계 완료 조건에 포함했다.
- 실행한 테스트와 결과: 문서만 변경해 Gradle 테스트는 실행하지 않았다. `git diff --check`, 지정 marker 단일 존재, 새 체크리스트 순서와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Identity·Billing·Learning Core의 책임을 분리하고 email·phone·provider subject를 canonical userId나 자동 merge 키로 사용하지 않는다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 체크리스트 번호가 기본 구현 순서이며 선행 단계가 완료되지 않은 상태에서 후속 production flag를 활성화하지 않는다. 실제 Jira는 단계 또는 안전한 lifecycle 묶음 단위로 별도 승인 후 생성한다.
- 위험 요소: 여러 저장소 작업을 병렬 개발하더라도 배포 순서까지 병렬로 간주하면 consumer 부재, stale mapping, 재가입 교착과 세션 유실이 다시 발생할 수 있다.
- 다음 작업: 1단계 Firebase/SNS 탈퇴 재인증과 withdrawal lifecycle의 Jira 범위·상태 모델·오류 계약·완료 조건을 먼저 작성한다.

## 2026-08-24 — 1단계 Firebase/SNS 탈퇴 lifecycle 구현 계획 작성

<!-- codex-turn:01a032ec-32c4-7f81-8f92-ec1cd1ddce34 -->

- 날짜: 2026-08-24
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 없음
- 작업 목표: 후속 구현 순서의 1단계인 Firebase/SNS 탈퇴 재인증과 withdrawal lifecycle을 실제 구현 가능한 API·상태·Transaction·테스트·배포 계획으로 구체화한다.
- 변경 파일: `docs/contracts/firebase-withdrawal-lifecycle-stage-1-plan.md`, `docs/contracts/firebase-auth-follow-up-implementation-order.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 기존 미커밋 변경과 다른 새 계약 문서는 건드리지 않았으며 WORKLOG 과거 기록은 수정하거나 삭제하지 않고 이번 항목만 파일 끝에 append했다.
- 구현 내용: 기존 withdraw endpoint에 optional write-only Firebase proof를 추가하고 실제 FirebaseIdentity 보유 여부로 GUEST·LOCAL·Firebase MEMBER credential을 구분한다. WITHDRAWAL purpose의 revoked·disabled·recent-auth와 Access/Refresh/Firebase/Social owner 일치를 검증한 뒤 User WITHDRAWN, 모든 Session revoke, 필요한 eligibility revoke와 lifecycle insert를 한 Mongo Transaction으로 commit하도록 계획했다.
- 구현 내용: 신규 UserWithdrawalLifecycle의 target snapshot, status, attempt·lease·retry·fencing 필드와 EXTERNAL_CLEANUP_PENDING에서 CLEANED까지의 상태 계약을 정의했다. 1단계는 PENDING 생성까지만 담당하고 Firebase disable/revoke/delete와 identity release·CLEANED는 2·3단계로 명시적으로 제외했다.
- 구현 내용: 반복·동시 요청, 응답 유실, legacy WITHDRAWN without lifecycle, pending 로그인/enrollment 오류, Firebase feature kill switch와 법적 탈퇴 가용성, LOCAL·GUEST 하위 호환, 민감정보 비저장·비로그, staging activation gate를 완료 조건에 포함했다.
- 실행한 테스트와 결과: 구현 계획 문서만 변경해 Gradle 테스트는 실행하지 않았다. `git diff --check`, 지정 marker 단일 존재, 구현 순서 문서의 1단계 링크와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Firebase UID·provider subject·email·phone을 canonical userId나 자동 merge 키로 사용하지 않고 Firebase 원격 mutation을 Mongo Transaction 안에서 수행하지 않는다. Identity·Billing·Learning Core 경계와 UUID/JWT 계약을 유지하며 Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 1단계 성공은 내부 WITHDRAWN과 `EXTERNAL_CLEANUP_PENDING` 생성이며 완전 탈퇴 terminal이나 재가입 허용이 아니다. 2단계 worker가 준비되기 전 production 탈퇴 endpoint만 단독 활성화하지 않는다.
- 위험 요소: optional credential 필드의 기존 client 호환, lifecycle target의 pseudonymous data 보존, provider kill switch 중 탈퇴 가용성, commit 후 응답 유실 재조회 방식과 실제 Mongo replica-set Transaction은 구현·staging 단계에서 확정 검증해야 한다.
- 다음 작업: 계획서를 기준으로 1단계 Jira 초안을 작성하고 사용자 승인 후에만 생성한다. 구현 시 lifecycle domain/Repository와 Transaction 테스트를 먼저 만든 뒤 Firebase withdrawal verifier와 API를 연결한다.

## 2026-08-24 — 결제 연기 뒤 SNS·무료시험·챌린지 우선순위 분석

<!-- codex-turn:01a032ed-fff8-7351-863d-a5737a2aa780 -->

- 날짜: 2026-08-24
- 브랜치: `develop`
- Jira: 없음
- 작업 목표: 개정된 1차 범위에서 Identity의 SNS·phone 준비 상태와 다음 작업을 확인한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: Firebase exchange/signup, Guest flow, auth-method sync, SocialIdentity, PhoneIdentity, eligibility publisher가 이미 존재하므로 새 로그인 endpoint 구현보다 탈퇴 lifecycle 1~3과 실제 Google/Apple/Phone 모바일·staging E2E가 선행임을 정리했다.
- 실행한 테스트와 결과: 코드 변경 없는 정적 분석이라 Gradle 테스트와 외부 Firebase 호출은 실행하지 않았다. Controller·계획·ADR·후속 구현 순서를 읽고 종료 전 `git diff --check`와 marker 단일 포함을 검증한다.
- 유지한 계약: Identity는 canonical userId와 credential mapping만 소유하고 TrialClaim·시험·10초 챌린지를 소유하지 않는다. Firebase ID Token·raw phone·provider credential을 Learning Core로 전달하지 않고 Secret·Token·Password·실제 Key·전체 MongoDB URI를 기록하지 않았다.
- 결정사항: 무료 TrialClaim은 기존 결정대로 최소 Billing/Entitlement가 소유한다. 1차 provider는 Google+Apple 우선, Kakao 후속을 권장하며 provider 범위는 최종 사용자 승인이 필요하다. phone eligibility consumer가 준비되기 전 production signup flag를 열지 않는다.
- 위험 요소: lifecycle·consumer 없이 SNS signup을 먼저 활성화하면 탈퇴·재가입과 phone 혜택 원장이 일관되지 않을 수 있다.
- 다음 작업: provider 범위 확정 후 이미 작성된 Firebase/SNS 탈퇴 lifecycle 1단계 Jira 초안을 승인받아 생성한다.

## 2026-08-24 — 1단계 withdrawal lifecycle Jira 생성안 검증

<!-- codex-turn:01a032fc-14cc-70b2-993f-17f09cb518e5 -->

- 날짜: 2026-08-24
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 없음 / 생성 전 승인 대기
- 작업 목표: Firebase/SNS 탈퇴 재인증과 withdrawal lifecycle 1단계 구현 계획을 TMI Jira 작업으로 생성하기 전에 실제 생성 가능 필드와 최종 payload를 검증한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션 코드는 변경하지 않았고 기존 계약 문서와 사용자 소유 미커밋 변경은 보존했다.
- 구현 내용: Atlassian 공식 MCP의 읽기 전용 조회로 `to-teacher` 사이트 연결, TMI 프로젝트 생성 권한, 이슈 유형 `작업`(ID `10003`)과 우선순위 `High`(ID `2`)를 확인했다. 제목·목표·구현 범위·완료 조건·제외 범위·참조 문서가 포함된 생성 payload를 준비했다.
- 실행한 테스트와 결과: Jira와 문서 기록 준비만 수행해 Gradle 테스트는 실행하지 않았다. 종료 전 `git diff --check`, 이 turn marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Jira에는 Secret·Token·Password·전체 MongoDB URI·RSA Private Key·사용자 개인정보를 포함하지 않는다. Identity·Billing·Learning Core 경계, canonical UUID/JWT 계약과 1단계의 외부 cleanup 제외 범위를 유지한다.
- 결정사항: 실제 Jira 생성, 댓글, 상태 전환은 수행하지 않았다. 사용자에게 정확한 생성 payload를 먼저 제시하고 명시적 승인을 받은 뒤 `작업`·`High`로 생성한다.
- 위험 요소: 1단계만 production에 활성화하면 외부 cleanup이 진행되지 않으므로 Stage 2 worker 준비 전 endpoint 단독 활성화를 금지하는 조건을 완료 기준에 포함해야 한다.
- 다음 작업: 사용자가 제시된 payload를 승인하면 Jira를 생성하고 발급된 이슈 키와 저장 결과를 재조회한 뒤 WORKLOG와 CURRENT_STATE에 기록한다.
- 승인 후 실행 결과: 사용자가 제시된 payload 생성을 명시적으로 승인했다. Atlassian 공식 MCP로 Jira `TMI-103` `[Identity] Firebase/SNS 탈퇴 재인증 및 withdrawal lifecycle 구축`을 TMI 프로젝트의 `작업`, 우선순위 `High`로 생성했다.
- Jira 작업: `TMI-103` 생성. 재조회 결과 상태는 `해야 할 일`, 담당자는 없고 라벨은 비어 있으며 승인한 설명·완료 조건·제외 범위·참고 문서가 저장된 것을 확인했다. 댓글, 상태 전환과 다른 필드 변경은 수행하지 않았다.
- 최종 다음 작업: 구현 시작 전에 `TMI-103`을 읽기 전용 재조회하고 계획서에 따라 lifecycle domain/Repository와 Mongo Transaction 테스트부터 구현한다.

## 2026-08-24 — TMI-103 생성 결과 turn 동기화

<!-- codex-turn:01a032ff-6c16-7902-8a83-7f1b347060f1 -->

- 날짜: 2026-08-24
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: `TMI-103`
- 작업 목표: 사용자 승인에 따른 Firebase/SNS 탈퇴 재인증 및 withdrawal lifecycle 1단계 Jira 생성 결과를 현재 종료 turn 기준으로 작업 기록에 동기화한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았고 기존 미커밋 파일은 보존했다.
- 구현 내용: Jira `TMI-103`이 TMI 프로젝트의 `작업`, 우선순위 `High`, 상태 `해야 할 일`로 생성됐음을 기록했다. 승인된 제목·설명·완료 조건·제외 범위가 저장됐고 담당자와 라벨은 비어 있다.
- 실행한 테스트와 결과: 코드 변경이 없어 Gradle 테스트는 실행하지 않았다. 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Jira와 작업 기록에 Secret·Token·Password·전체 MongoDB URI·RSA Private Key·사용자 개인정보를 포함하지 않았다. Identity·Billing·Learning Core 책임과 canonical UUID/JWT 계약을 유지했다.
- 결정사항: 사용자의 명시적 승인으로 이슈 생성만 수행했으며 댓글과 상태 전환, 담당자·라벨 지정은 수행하지 않았다.
- 위험 요소: Stage 2 Firebase cleanup worker가 준비되기 전에 Stage 1 탈퇴 endpoint만 production에 활성화하면 lifecycle이 `EXTERNAL_CLEANUP_PENDING`에 머물 수 있다.
- 다음 작업: 구현 전에 Jira `TMI-103`을 다시 조회하고 lifecycle domain/Repository와 Mongo Transaction 테스트부터 진행한다.

## 2026-08-25 — TMI-103 Firebase/SNS 탈퇴 재인증과 lifecycle Stage 1 구현

<!-- codex-turn:01a0373a-a723-70f2-8046-f6aa615d8c24 -->

- 날짜: 2026-08-25
- 브랜치: `feat/TMI-103-firebase-withdrawal-lifecycle` (Codex commit·push 미수행)
- Jira: `TMI-103`
- 작업 목표: Firebase/SNS MEMBER가 fresh Firebase proof로 탈퇴할 수 있게 하고 User tombstone, 전체 RefreshSession 폐기, 필요한 phone eligibility revoke, durable withdrawal lifecycle 생성을 하나의 Mongo Transaction으로 처리한다.
- 변경 파일: `.env.example`, `src/main/resources/application.yml`, `src/main/java/web/tosunsaeng/identity/domain/auth/common/exception/AuthErrorStatus.java`, `domain/auth/federation/application/FirebaseExchangeService.java`, `FirebaseVerificationPurpose.java`, `domain/auth/federation/infrastructure/firebase/FirebaseAdminAuthenticationVerifier.java`, `FirebaseAuthenticationConfiguration.java`, `domain/user/application/UserWithdrawalService.java`, `UserWithdrawalTransactionService.java`, 신규 `FirebaseWithdrawalCredentialVerifier.java`, `FirebaseWithdrawalTarget.java`, `domain/user/domain/entity/UserWithdrawalLifecycle.java`, `domain/user/domain/enums/UserWithdrawalCleanupStatus.java`, `domain/user/domain/repository/UserWithdrawalLifecycleRepository.java`, `domain/user/dto/request/WithdrawRequest.java`, `domain/user/dto/response/WithdrawResponse.java`, `domain/user/exception/UserErrorStatus.java`, 관련 신규·기존 테스트와 이 작업 기록 문서다. 기존 untracked Learning Core handoff 문서는 변경하지 않았다.
- 구현 내용: 기존 `POST /api/v1/users/withdraw` 요청에 optional write-only `firebaseIdToken`을 추가하고 `toString` redaction과 16 KiB 제한을 유지했다. GUEST는 추가 credential을 거절하고, LOCAL MEMBER는 password만, FirebaseIdentity 보유 MEMBER는 Firebase proof만 허용하며 혼합·모호한 credential ownership은 안정적인 400 오류로 거절한다.
- 구현 내용: `FirebaseVerificationPurpose.WITHDRAWAL`을 high-risk recent-auth 경로로 추가했다. 탈퇴 목적은 일반 provider 로그인 feature flag와 분리하되 phone-only sign-in은 허용하지 않고 revoke·disabled 원격 검증을 유지한다. 검증된 project·UID가 현재 FirebaseIdentity와 canonical userId에 일치하고 linked SocialIdentity가 다른 user owner가 아닌지 확인하며 credential·UID·provider subject를 로그나 응답에 노출하지 않는다.
- 구현 내용: `UserWithdrawalLifecycle`에 userId unique index와 `status + nextAttemptAt + leaseUntil` worker claim index, Firebase target snapshot, attempt·lease·오류·완료 시각·version 필드를 추가했다. 초기 상태는 `EXTERNAL_CLEANUP_PENDING`이며 Stage 2·3이 사용할 retry, external completed, release pending, CLEANED, reconciliation 전이 계약을 enum 테스트로 고정했다.
- 구현 내용: Mongo Transaction 안에서 요청 RefreshSession을 재검증하고 User CAS tombstone, FirebaseIdentity target 재검증, lifecycle insert, 기존 active phone eligibility binding의 REVOKED outbox, 모든 active RefreshSession 폐기를 처리한다. userId lifecycle unique와 User CAS, duplicate winner 재조회로 동시 요청과 응답 유실 재시도가 동일 lifecycle 결과로 수렴하며 lifecycle 없는 legacy WITHDRAWN은 `WITHDRAWAL_LIFECYCLE_CONFLICT`로 fail-closed 처리한다.
- 구현 내용: 기존 FirebaseIdentity가 남은 WITHDRAWN 사용자의 Firebase exchange는 lifecycle 존재 시 `WITHDRAWAL_CLEANUP_PENDING`을 반환한다. `FIREBASE_WITHDRAWAL_ENABLED`를 기본 `false`로 추가해 Stage 2 cleanup worker가 준비되기 전 Firebase 탈퇴 경로의 production 활성화를 막았고 LOCAL·GUEST 기존 경로는 유지했다.
- 실행한 테스트와 결과: 신규 lifecycle domain/index/transition, Firebase proof·Firebase/Social owner conflict, credential 조합, target lifecycle Transaction 테스트를 추가했다. 선택 테스트와 Spring wiring 회귀를 거친 뒤 `./gradlew clean test`를 실행해 전체 505개 테스트가 실패·오류 없이 성공했다. `git diff --check`도 통과했다.
- 유지한 계약: canonical userId와 JWT sub는 서버 UUID이고 Firebase UID·provider subject·phone을 userId·로그인 탐색·자동 merge 키로 사용하지 않는다. Firebase 원격 mutation은 Mongo Transaction 안에서 실행하지 않고 Firebase ID Token·Provider Token·raw phone·email·SDK 오류를 lifecycle에 저장하지 않는다. Identity는 시험·TrialClaim을 소유하지 않으며 기존 `BaseResponse`, Refresh Token 원문 비저장, LOCAL·GUEST 탈퇴 계약을 유지했다.
- 결정사항: Stage 1 성공은 User `WITHDRAWN`과 lifecycle `EXTERNAL_CLEANUP_PENDING`이며 완전 탈퇴나 재가입 허용이 아니다. Firebase disable·refresh revoke·delete worker, Firebase/Social/Phone release와 `CLEANED` 전환은 Stage 2·3으로 남기고 activation flag는 그 전까지 기본 비활성으로 유지한다. 현재 turn에는 Atlassian 도구가 노출되지 않아 Jira 재조회·댓글·상태 전환을 수행하지 않았으며 직전 검증된 `TMI-103` 계약을 기준으로 구현했다.
- 위험 요소: 실제 Mongo replica-set의 다중 collection rollback, Firebase/mobile fresh reauth, provider flag가 꺼진 기존 사용자 탈퇴, timeout·응답 유실·동시 요청은 staging E2E가 필요하다. Stage 2 worker 없이 flag를 활성화하면 lifecycle이 pending에 머물며 Firebase 외부 User와 내부 identity 점유가 정리되지 않는다. 기존 lifecycle 없는 WITHDRAWN 데이터는 자동 target 추정 없이 운영 reconciliation이 필요하다.
- 다음 작업: diff 리뷰 후 staging에서 Mongo Transaction과 Firebase withdrawal proof E2E를 검증하고, 별도 승인 아래 Jira 진행 상태·댓글을 갱신한다. 이후 구현 순서 2단계인 Firebase disable·revoke·delete worker를 진행한다.

## 2026-08-25 — 완전 탈퇴 구현 범위 재확인

<!-- codex-turn:01a03747-53b2-7a91-8cae-367e83da8830 -->

- 날짜: 2026-08-25
- 브랜치: `feat/TMI-103-firebase-withdrawal-lifecycle` (Codex commit·push 미수행)
- Jira: `TMI-103` (조회·수정·댓글·상태 전환 미수행)
- 작업 목표: 현재 계획과 구현에 회원 탈퇴가 어디까지 포함됐는지 내부 탈퇴, Firebase 외부 cleanup, identity release 기준으로 구분해 확인한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았고 기존 미커밋 변경을 보존했다.
- 구현 내용: 구현 순서 문서와 Stage 1 계획을 확인해 완전 탈퇴가 1단계 Firebase/SNS 재인증·내부 `WITHDRAWN`·lifecycle, 2단계 Firebase disable·refresh revoke·delete worker, 3단계 Firebase/Social/Phone release·`CLEANED`·재가입 gate로 구성됨을 재확인했다.
- 실행한 테스트와 결과: 코드 변경 없는 계획 확인이라 Gradle 테스트는 다시 실행하지 않았다. 직전 구현 turn의 `./gradlew clean test` 전체 505개 성공 결과를 유지하며 문서 변경에 대해 `git diff --check`, marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: User tombstone과 canonical UUID는 유지하고 Firebase 외부 mutation은 Mongo Transaction 밖의 retry worker가 담당한다. phone·provider subject는 자동 복구·merge 키가 아니며 Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 현재 완료된 것은 Stage 1로 내부 계정은 즉시 탈퇴 상태가 되지만 완전 탈퇴 terminal은 아니다. Stage 2 외부 Firebase 삭제와 Stage 3 내부 identity 점유 해제가 끝나 `CLEANED`가 되어야 동일 credential의 신규 가입을 허용한다.
- 위험 요소: Stage 2 worker 없이 `FIREBASE_WITHDRAWAL_ENABLED`를 활성화하면 lifecycle이 `EXTERNAL_CLEANUP_PENDING`에 머물고 Firebase User와 identity 점유가 남는다.
- 다음 작업: 구현 순서대로 Stage 2 Firebase disable·refresh revoke·delete worker 계획과 Jira를 별도 승인 후 작성하고, 이후 Stage 3 release와 `CLEANED` gate를 구현한다.

## 2026-08-25 — TMI-103 종료 요청과 상태 전환 승인 대기

<!-- codex-turn:01a03762-dfa2-7883-87cd-5b38133a41e2 -->

- 날짜: 2026-08-25
- 브랜치: `feat/TMI-103-firebase-withdrawal-lifecycle` (Codex commit·push 미수행)
- Jira: `TMI-103` / 상태 전환 전 승인·도구 연결 대기
- 작업 목표: 구현과 테스트가 완료된 `TMI-103`을 Jira 완료 상태로 종료한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았고 기존 미커밋 변경을 보존했다.
- 구현 내용: 사용자 종료 요청을 확인하고 변경 범위를 Jira 상태의 `완료` 전환 하나로 제한했다. 댓글·담당자·우선순위·라벨·설명은 변경하지 않는 안을 준비했다.
- 실행한 테스트와 결과: 코드 변경이 없어 Gradle 테스트는 다시 실행하지 않았다. 직전 `./gradlew clean test` 전체 505개 성공 결과를 유지하며 문서 변경에 대해 `git diff --check`, marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Jira 상태 변경 전에 정확한 변경안을 사용자에게 제시하고 승인을 받는 규칙을 유지했다. Jira와 작업 기록에 Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 포함하지 않았다.
- 결정사항: 현재 turn에는 Atlassian 공식 Jira 도구가 노출되지 않아 현재 상태·사용 가능한 완료 전환 ID를 재조회하거나 상태를 변경하지 않았다. 마지막 확인 상태는 `해야 할 일`이며 임의의 과거 transition ID를 재사용하지 않는다.
- 위험 요소: Jira workflow 또는 현재 상태가 외부에서 바뀌었을 수 있으므로 도구가 다시 제공되면 상태와 전환을 재조회한 뒤 승인된 `완료` 전환만 적용해야 한다.
- 다음 작업: 사용자가 상태만 `완료`로 전환하는 안을 승인하고 Atlassian 도구가 가용해지면 현재 상태·전환 ID를 재조회해 적용하고 결과를 다시 확인한다.

## 2026-08-25 — Stage 2 Firebase 외부 cleanup worker Jira 생성안 작성

<!-- codex-turn:01a03765-fa21-7363-bdba-f11866980479 -->

- 날짜: 2026-08-25
- 브랜치: `feat/TMI-103-firebase-withdrawal-lifecycle` (Codex commit·push 미수행)
- Jira: 신규 생성 전 payload 승인·도구 연결 대기. `TMI-103` 상태 전환도 미수행
- 작업 목표: 고정 구현 순서의 다음 항목인 Firebase disable·refresh revoke·delete worker를 독립 Jira 작업으로 생성할 최종 payload를 작성한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았고 기존 미커밋 변경을 보존했다.
- 구현 내용: Stage 1 lifecycle 인계 계약을 기준으로 `EXTERNAL_CLEANUP_PENDING` atomic claim·lease, Firebase User disable, refresh token revoke, Provider별 계정 삭제 의무, Firebase User delete, retry/backoff, 안전한 오류 code와 `RECONCILIATION_REQUIRED`, external completion fencing을 Jira 범위로 구성했다.
- 구현 내용: 완료 조건에는 다중 worker 단일 claim, lease 만료 recovery, 각 외부 단계의 멱등 재실행, partial failure 수렴, 원문 Firebase 오류·credential 비노출, scheduler·feature flag 기본 비활성, Emulator 단위 테스트와 staging 실제 Firebase 검증을 포함했다. Firebase/Social/Phone release와 `CLEANED`·재가입 gate는 Stage 3으로 명시적으로 제외했다.
- 실행한 테스트와 결과: Jira payload 준비와 계획 확인만 수행해 Gradle 테스트는 다시 실행하지 않았다. 직전 `./gradlew clean test` 전체 505개 성공 결과를 유지하며 문서 변경에 대해 `git diff --check`, marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Firebase 외부 mutation은 Mongo Transaction 밖의 retry worker가 수행하며 lifecycle의 Firebase project·UID snapshot만 사용한다. Token·Provider credential·email·raw phone·SDK 원문 오류를 저장·로그·Jira에 기록하지 않고 identity release는 별도 단계로 유지한다.
- 결정사항: 신규 이슈는 TMI 프로젝트 `작업`, 우선순위 `High`, 제목 `[Identity] Firebase 탈퇴 외부 cleanup worker 구축`으로 제안한다. Jira 변경 규칙에 따라 정확한 설명·완료 조건·제외 범위를 사용자에게 먼저 제시하고 승인 전에는 생성하지 않는다.
- 위험 요소: Apple authorization revoke는 필요한 credential material과 만료·보관 정책이 별도 검증돼야 하며, 지원 material이 없다고 Firebase delete 전체를 무기한 막을지 reconciliation으로 분기할지 구현 전 확정해야 한다. Stage 3 release 전에 external completion을 잘못 terminal로 간주하면 재가입 gate가 조기에 열릴 수 있다.
- 다음 작업: 사용자가 payload를 승인하고 Atlassian 공식 도구가 가용해지면 TMI `작업`·`High`로 생성해 키와 저장 결과를 재조회한다. `TMI-103` 완료 전환은 별도 승인·도구 가용 시 처리한다.
- 승인 후 실행 결과: 사용자가 `TMI-103` 완료 전환과 승인된 Stage 2 payload 생성을 함께 승인했다. Atlassian Rovo 공식 연결을 설치한 뒤 `TMI-103`이 `해야 할 일`·Resolution 없음이고 `완료` transition ID `41`이 사용 가능함을 재조회했다.
- Jira 작업: `TMI-103`에 transition ID `41`만 적용해 상태와 Resolution이 모두 `완료`임을 확인했다. 댓글·담당자·우선순위·라벨·설명은 변경하지 않았다.
- Jira 작업: 신규 Jira `TMI-104` `[Identity] Firebase 탈퇴 외부 cleanup worker 구축`을 TMI 프로젝트의 `작업`, 우선순위 `High`로 생성했다. 재조회 결과 초기 상태 `해야 할 일`, 담당자 없음, 빈 라벨과 승인한 목표·범위·완료 조건·제외 범위·참고 문서가 저장된 것을 확인했다.
- 최종 다음 작업: 새 구현 브랜치에서 `TMI-104`를 읽기 전용 재조회한 뒤 lifecycle claim·lease Repository와 Firebase Admin external cleanup port 테스트부터 구현한다. Jira 상태 변경과 댓글은 별도 승인 후 수행한다.

## 2026-08-25 — TMI-103 완료와 TMI-104 생성 결과 turn 동기화

<!-- codex-turn:01a03767-e851-76a1-872c-be8d2efae7f7 -->

- 날짜: 2026-08-25
- 브랜치: `feat/TMI-103-firebase-withdrawal-lifecycle` (Codex commit·push 미수행)
- Jira: `TMI-103`, `TMI-104`
- 작업 목표: 사용자 승인으로 수행한 Stage 1 Jira 종료와 Stage 2 Jira 생성 결과를 종료 훅의 현재 turn 기준으로 작업 기록에 동기화한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았고 기존 미커밋 변경을 보존했다.
- 구현 내용: Jira `TMI-103`은 transition ID `41` 적용 후 상태와 Resolution이 모두 `완료`인 것을 확인했다. Jira `TMI-104` `[Identity] Firebase 탈퇴 외부 cleanup worker 구축`은 `작업`·`High`·`해야 할 일`, 담당자 없음·빈 라벨로 생성된 것을 확인했다.
- 실행한 테스트와 결과: 코드 변경이 없어 Gradle 테스트는 다시 실행하지 않았다. 직전 `./gradlew clean test` 전체 505개 성공 결과를 유지하며 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Jira에는 승인된 상태 전환과 이슈 생성만 적용했고 댓글·담당자·라벨·기타 필드를 임의 변경하지 않았다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: Stage 1 Jira는 종료됐고 Stage 2 작업은 `TMI-104`를 구현 범위의 기준으로 사용한다. Firebase/Social/Phone release와 `CLEANED` 재가입 gate는 Stage 3으로 계속 제외한다.
- 위험 요소: Stage 2 구현 전 `FIREBASE_WITHDRAWAL_ENABLED`를 production에서 활성화하면 lifecycle이 pending에 머물 수 있다.
- 다음 작업: 새 구현 브랜치에서 `TMI-104`를 읽기 전용 재조회한 뒤 lifecycle claim·lease Repository와 Firebase external cleanup port 테스트부터 구현한다.

## 2026-08-25 — TMI-104 다음 작업 흐름 설명

- 날짜: 2026-08-25
- 브랜치: `feat/TMI-103-firebase-withdrawal-lifecycle` (Codex commit·push 미수행)
- Jira: `TMI-104` (조회·수정·댓글·상태 전환 미수행)
- 작업 목표: Stage 2 Firebase 외부 cleanup worker가 Stage 1과 Stage 3 사이에서 수행할 책임, 상태 전이와 실패 복구 방식을 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 구현 내용: worker는 `EXTERNAL_CLEANUP_PENDING` 또는 due retry lifecycle을 atomic claim하고 lease owner·leaseUntil·version으로 이전 worker를 fencing한 뒤 Firebase disable, refresh token revoke, Provider별 의무, Firebase User delete를 순서대로 멱등 실행한다.
- 구현 내용: 성공은 external deleted 시각과 함께 다음 Stage 3의 `IDENTITY_RELEASE_PENDING`으로 인계한다. 재시도 가능한 장애는 `EXTERNAL_CLEANUP_RETRY_WAIT`와 backoff로, 반복 실패·결과 불명·필수 material 부재는 `RECONCILIATION_REQUIRED`로 분류하며 이미 삭제된 Firebase User는 성공으로 수렴한다.
- 실행한 테스트와 결과: 설명과 계획 재확인만 수행해 Gradle 테스트는 다시 실행하지 않았다. 직전 전체 505개 성공 결과를 유지하고 문서 변경에 대해 `git diff --check`와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Firebase 외부 mutation은 Mongo Transaction 밖에서 실행하며 identity mapping release·`CLEANED`·재가입 허용은 Stage 3까지 수행하지 않는다. credential·Token·email·raw phone·SDK 원문 오류와 Secret을 기록하지 않았다.
- 결정사항: scheduler와 worker는 기본 비활성이고 Stage 2 배포·staging 검증 뒤에만 Stage 1 Firebase withdrawal activation flag를 연다. Stage 2 성공은 외부 삭제 완료이지 완전 탈퇴 terminal이 아니다.
- 위험 요소: network timeout 뒤 delete 결과 불명, worker crash, lease 만료와 Apple revoke material 부재를 단순 실패로 취급하면 중복 mutation 또는 영구 pending이 생길 수 있어 멱등 조회·fencing·reconciliation이 필요하다.
- 다음 작업: `TMI-104` 구현 브랜치에서 lifecycle claim/CAS Repository, Firebase cleanup command port, retry policy와 scheduler를 순서대로 테스트 우선 구현한다.

## 2026-08-25 — TMI-104 설명 turn 동기화

<!-- codex-turn:01a0376b-e2b4-7611-a12e-25fd4e6a7a20 -->

- 날짜: 2026-08-25
- 브랜치: `feat/TMI-103-firebase-withdrawal-lifecycle` (Codex commit·push 미수행)
- Jira: `TMI-104` (조회·수정·댓글·상태 전환 미수행)
- 작업 목표: Firebase 외부 cleanup worker의 처리 순서, 동시성·실패 복구와 Stage 3 경계를 현재 종료 turn 기준으로 기록한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 구현 내용: Stage 2 worker는 pending/retry lifecycle을 atomic lease로 claim하고 Firebase disable, refresh revoke, Provider별 의무, User delete를 멱등 실행한다. lease 만료 recovery와 fencing으로 worker crash·늦은 결과를 처리하고 성공 시 `IDENTITY_RELEASE_PENDING`으로 인계한다.
- 실행한 테스트와 결과: 코드 변경이 없어 Gradle 테스트는 다시 실행하지 않았다. 직전 전체 505개 성공 결과를 유지하며 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Firebase 외부 mutation은 Mongo Transaction 밖에서 실행하며 Token·credential·email·raw phone·SDK 원문 오류와 Secret을 저장하거나 기록하지 않는다. 내부 identity release와 `CLEANED`는 Stage 3 책임이다.
- 결정사항: Stage 2는 외부 Firebase 삭제 완료까지이며 완전 탈퇴 terminal이나 동일 credential 재가입 허용이 아니다. worker·scheduler 및 `FIREBASE_WITHDRAWAL_ENABLED`는 staging 검증 전 기본 비활성으로 유지한다.
- 위험 요소: timeout 뒤 delete 결과 불명, lease 경쟁과 Apple revoke material 부재는 retry만으로 해결하지 못할 수 있어 명시적 reconciliation이 필요하다.
- 다음 작업: `TMI-104` 구현 브랜치에서 lifecycle claim/CAS Repository와 Firebase cleanup command port 테스트부터 구현한다.

## 2026-08-25 — 탈퇴 시 혜택 연결 해제 이벤트 의미 확인

<!-- codex-turn:01a03782-b9a8-7e61-a8c3-d70fba93fb67 -->

- 날짜: 2026-08-25
- 브랜치: `feat/TMI-103-firebase-withdrawal-lifecycle` (Codex commit·push 미수행)
- Jira: 없음 / Jira 작업 미수행
- 작업 목표: 회원 탈퇴 Transaction에서 생성하는 `PhoneEligibilityBindingRevoked` 이벤트의 대상, 효과와 하지 않는 일을 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 구현 내용: 코드를 확인해 active `PhoneEligibilityBindingRevision`이 존재하는 경우에만 revision을 증가시켜 inactive로 전환하고, 같은 탈퇴 Transaction에서 `REVOKED` outbox를 생성함을 확인했다. 이벤트는 userId, consumer scope, 새 revision과 해제 시각을 전달하며 raw phone을 포함하지 않는다.
- 구현 내용: 이 이벤트는 Billing/Entitlement에 해당 사용자가 현재 검증 전화번호 기반 혜택 자격의 활성 연결 대상이 아님을 알린다. 소비자는 revision high-water로 늦게 도착한 과거 VERIFIED 이벤트가 연결을 되살리지 못하게 한다. 기존 TrialClaim과 무료혜택 사용 이력은 유지하므로 탈퇴·재가입으로 혜택이 복원되지 않는다.
- 실행한 테스트와 결과: 코드 설명을 위한 정적 확인만 수행해 Gradle 테스트는 다시 실행하지 않았다. 직전 전체 505개 성공 결과를 유지하며 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Identity는 검증된 전화번호 연결 상태와 outbox만 생산하고 TrialClaim·Entitlement는 소유하지 않는다. 이벤트는 Firebase phone unlink, PhoneIdentity·alias release, 무료시험 기록 삭제를 수행하지 않으며 Secret·Token·Password·실제 Key·전체 MongoDB URI와 raw phone을 기록하지 않았다.
- 결정사항: active binding이 없으면 REVOKED 이벤트를 만들지 않는다. active binding이 있으면 탈퇴 commit과 outbox 생성을 원자적으로 묶고 publisher·consumer가 at-least-once로 전달·멱등 적용한다.
- 위험 요소: consumer 없이 producer를 활성화하면 outbox가 pending으로 쌓이고, REVOKED 전달 전에 동일 user의 eligibility를 계속 active로 보는 외부 상태가 남을 수 있으므로 Billing consumer를 먼저 배포해야 한다.
- 다음 작업: `TMI-104` external Firebase cleanup과 별개로 Stage 3에서 PhoneIdentity·alias 실제 release를 수행하고, Billing consumer는 TrialClaim을 보존한 채 binding만 inactive로 반영한다.

## 2026-08-25 — TMI-104 Firebase 외부 cleanup worker 구현 계획 작성

- 날짜: 2026-08-25
- 브랜치: `feat/TMI-103-firebase-withdrawal-lifecycle` (Codex commit·push 미수행)
- Jira: `TMI-104` 읽기 전용 재조회. 상태 `해야 할 일`, 유형 `작업`, 우선순위 `High`; Jira 수정·댓글·상태 전환 미수행
- 작업 목표: Stage 1 lifecycle을 안전하게 claim해 Firebase 외부 User를 disable·revoke·delete하고 Stage 3 identity release로 인계하는 실제 구현 계획서를 작성한다.
- 변경 파일: 신규 `docs/contracts/firebase-withdrawal-external-cleanup-stage-2-plan.md`, `docs/contracts/firebase-auth-follow-up-implementation-order.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: 현재 `UserWithdrawalLifecycle`, verify-only Firebase Admin client, phone eligibility publisher의 atomic lease·retry 패턴을 분석했다. claim마다 새 lease token을 사용하고 withdrawalId·status·lease token·version을 모든 update fencing 조건에 넣으며, expired in-progress lease를 recovery하는 custom Mongo Repository 계약을 계획했다.
- 구현 내용: target guard 뒤 inspect→disable→refresh revoke→Provider 의무→delete→presence 확인을 순서화했다. 이미 삭제된 User는 성공, delete timeout은 presence로 확정, retryable 장애는 exponential backoff, project·owner·permission·configuration·Provider material 문제는 reconciliation으로 분리했다. LOCAL·GUEST null target은 Firebase 호출 없이 Stage 3으로 인계한다.
- 구현 내용: worker·scheduler 기본 비활성 설정, safe metrics·logs, domain/Repository/worker/adapter/config/scheduler 테스트, Firebase·Mongo staging 시나리오, worker 선활성화 후 withdrawal endpoint 활성화 순서를 정의했다. Apple authorization revoke material은 일반 lifecycle에 저장하지 않고 PoC·보안 경계가 확정되기 전 production activation을 금지했다.
- 실행한 테스트와 결과: 계획 문서만 변경해 Gradle 테스트는 실행하지 않았다. 종료 전 `git diff --check`, 새 문서 링크, WORKLOG EOF append를 검증한다.
- 유지한 계약: 내부 탈퇴를 Firebase 장애로 되돌리지 않고 외부 mutation은 Mongo Transaction 밖에서 수행한다. Firebase UID·provider subject·email·phone을 target 추정 키로 사용하지 않으며 Token·credential·SDK 원문 오류·Secret을 lifecycle·로그·문서에 기록하지 않는다. identity release·`CLEANED`·재가입은 Stage 3 범위로 유지한다.
- 결정사항: Stage 2 terminal은 `IDENTITY_RELEASE_PENDING`이다. `EXTERNAL_CLEANUP_COMPLETED`는 외부 삭제 증적 상태로 먼저 저장하고 별도 local CAS가 Stage 3 인계 상태로 이동해 crash 사이 상태도 복구한다. 공개 cleanup API와 운영 UI는 추가하지 않는다.
- 위험 요소: Firebase delete timeout의 결과 불명, lease 경계에서 늦은 worker, configured project mismatch와 Apple revoke material 부재를 해결하지 않으면 잘못된 외부 삭제 또는 영구 pending이 발생할 수 있다. 실제 Admin SDK timeout과 lease duration은 staging에서 검증해야 한다.
- 다음 작업: 새 `TMI-104` 구현 브랜치에서 failure code·retry policy, lifecycle claim/CAS custom Repository와 동시성 integration test부터 작성한다.

## 2026-08-25 — TMI-104 계획서 작성 결과 turn 동기화

<!-- codex-turn:01a03784-891d-7313-908a-585b9861de56 -->

- 날짜: 2026-08-25
- 브랜치: `feat/TMI-103-firebase-withdrawal-lifecycle` (Codex commit·push 미수행)
- Jira: `TMI-104` (추가 조회·수정·댓글·상태 전환 미수행)
- 작업 목표: Stage 2 Firebase 외부 cleanup worker 계획서와 구현 순서 문서 연결 결과를 현재 종료 turn 기준으로 작업 기록에 동기화한다.
- 변경 파일: `docs/contracts/firebase-withdrawal-external-cleanup-stage-2-plan.md`, `docs/contracts/firebase-auth-follow-up-implementation-order.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: atomic claim·lease token·version fencing, target guard, disable→revoke→Provider 의무→delete, 결과 불명 presence 확인, retry/reconciliation, scheduler·feature flag, 테스트·staging·Stage 3 인계를 포함한 계획서를 저장했다.
- 실행한 테스트와 결과: 문서만 변경해 Gradle 테스트는 실행하지 않았다. `git diff --check`, 새 계획서 존재와 구현 순서 링크, 지정 marker 단일 존재, WORKLOG EOF append를 검증한다.
- 유지한 계약: 외부 Firebase mutation은 Mongo Transaction 밖에서 처리하고 identity release·`CLEANED`·재가입은 Stage 3으로 유지했다. Token·credential·SDK 원문 오류·Secret·Password·실제 Key·전체 MongoDB URI와 개인정보를 기록하지 않았다.
- 결정사항: Stage 2 terminal은 `IDENTITY_RELEASE_PENDING`이며 Apple revoke material의 안전한 경로가 검증되기 전에는 관련 production withdrawal을 활성화하지 않는다.
- 위험 요소: delete timeout, stale worker, project mismatch와 Apple revoke material 부재는 presence 확인·fencing·reconciliation 및 staging 검증이 필요하다.
- 다음 작업: 새 `TMI-104` 구현 브랜치에서 failure code·retry policy와 lifecycle claim/CAS Repository 테스트부터 구현한다.

## 2026-08-25 — TMI-104 저장소 계획서 요청 정정 확인

<!-- codex-turn:01a0378a-bbc6-7932-834e-e9693dc92eb5 -->

- 날짜: 2026-08-25
- 브랜치: `feat/TMI-104-firebase-withdrawal-cleanup-worker` (Codex commit·push 미수행)
- Jira: `TMI-104` (조회·수정·댓글·상태 전환 미수행)
- 작업 목표: 사용자가 요청한 계획서의 위치가 Jira가 아니라 저장소 문서임을 정정하고 기존 생성 결과를 재확인한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 기존 계획서 `docs/contracts/firebase-withdrawal-external-cleanup-stage-2-plan.md`와 구현 순서 문서는 내용 변경 없이 재확인했다. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: `TMI-104` Stage 2 구현 계획서가 저장소에 존재하고 `docs/contracts/firebase-auth-follow-up-implementation-order.md`의 2단계에서 링크되는 것을 확인했다. 현재 브랜치가 `feat/TMI-104-firebase-withdrawal-cleanup-worker`인 것도 반영했다.
- 실행한 테스트와 결과: 문서 위치와 연결만 확인했으므로 Gradle 테스트는 실행하지 않았다. `git diff --check`, 계획서 파일 존재, 구현 순서 링크와 지정 marker 단일 존재, WORKLOG EOF append를 검증한다.
- 유지한 계약: Jira에는 어떤 추가 변경도 수행하지 않았다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 계획서는 Jira 본문이 아니라 저장소 계약 문서로 관리하며, 이후 구현은 해당 문서를 범위와 순서의 기준으로 사용한다.
- 위험 요소: 계획서가 아직 커밋되지 않은 untracked 파일이므로 사용자가 커밋할 때 누락하지 않아야 한다.
- 다음 작업: 계획서에 따라 failure code·retry policy와 lifecycle claim/CAS Repository 테스트부터 구현한다.

## 2026-08-25 — TMI-104 target guard와 Firebase inspect 설명

<!-- codex-turn:01a037bd-23f0-7063-a900-f507853ad7f5 -->

- 날짜: 2026-08-25
- 브랜치: `feat/TMI-104-firebase-withdrawal-cleanup-worker` (Codex commit·push 미수행)
- Jira: `TMI-104` (조회·수정·댓글·상태 전환 미수행)
- 작업 목표: Stage 2 실행 순서의 `User·FirebaseIdentity target preflight guard`와 `Firebase account inspect`가 각각 무엇을 검증하는지 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계획서 내용은 변경하지 않았다.
- 구현 내용: preflight guard는 내부 MongoDB에서 lifecycle의 User가 존재하고 `WITHDRAWN`인지, FirebaseIdentity의 userId·project·UID가 lifecycle snapshot과 일치하는지, 현재 claim의 status·lease token·version이 유효한지를 확인한다. 불일치는 외부 호출 없이 reconciliation으로 분류한다.
- 구현 내용: account inspect는 검증된 project·UID로 Firebase `getUser`를 호출해 외부 User의 존재 여부, disabled 상태와 Provider 삭제 의무 판정에 필요한 provider 집합을 읽는 외부 조회다. 이미 부재하면 멱등 성공으로 수렴하고, email·phone·provider subject로 target을 추정하지 않는다.
- 실행한 테스트와 결과: 코드 변경이 없는 설명 작업이라 Gradle 테스트는 실행하지 않았다. 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Firebase mutation 전에 내부 ownership과 worker fencing을 확인하며, 민감한 외부 계정 속성을 lifecycle·로그·작업 기록에 저장하지 않는다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: preflight는 내부 정합성 검증이고 inspect는 외부 현재 상태 조회로 분리한다. 두 단계 모두 write가 아니며 실제 disable·revoke·delete는 이후 단계에서만 실행한다.
- 위험 요소: 둘을 생략하거나 합쳐 mismatch를 `NOT_FOUND` 성공으로 처리하면 다른 Firebase User를 삭제하거나 내부 데이터 불일치를 숨길 수 있다.
- 다음 작업: 계획서에 따라 target guard와 cleanup port를 구현할 때 mismatch·not-found·provider snapshot 테스트를 분리해 작성한다.

## 2026-08-25 — TMI-104 Firebase 탈퇴 외부 cleanup worker 구현

<!-- codex-turn:01a037c0-886a-7ec0-a290-fef1c1330e8a -->

- 날짜: 2026-08-25
- 브랜치: `feat/TMI-104-firebase-withdrawal-cleanup-worker` (Codex commit·push 미수행)
- Jira: `TMI-104`. 구현 전 Atlassian 공식 연결로 읽기 전용 검색을 시도했으나 현재 인스턴스에 연결 앱이 설치되지 않았다는 403 응답을 받아 재조회하지 못했다. Jira 수정·댓글·상태 전환은 수행하지 않았다.
- 작업 목표: Stage 1의 `EXTERNAL_CLEANUP_PENDING` lifecycle을 안전하게 claim해 Firebase User를 disable·refresh revoke·Provider 의무 처리·delete하고, 삭제 확인 후 Stage 3 `IDENTITY_RELEASE_PENDING`으로 인계하는 비동기 worker를 구현한다.
- 변경 파일: `UserWithdrawalLifecycle.java`와 신규 `WithdrawalCleanupFailureCode.java`; `UserWithdrawalLifecycleRepository.java`와 신규 custom interface·`UserWithdrawalLifecycleRepositoryImpl.java`; `domain/user/application`의 cleanup port·snapshot·presence·provider·exception·retry policy·target guard·worker·outcome·provider result; Firebase infrastructure의 `FirebaseSdkWithdrawalCleanupAdapter.java`, `FirebaseWithdrawalCleanupAppHandle.java`; user infrastructure의 properties·configuration·scheduler; `application.yml`, `application-test.yml`, Stage 2 계획서와 Codex 상태 문서.
- 변경 파일: 신규 테스트 `UserWithdrawalLifecycleRepositoryIntegrationTests`, `WithdrawalCleanupRetryPolicyTests`, `WithdrawalCleanupTargetGuardTests`, `UserWithdrawalExternalCleanupWorkerTests`, `FirebaseSdkWithdrawalCleanupAdapterTests`, `FirebaseWithdrawalCleanupAppHandleTests`, `UserWithdrawalExternalCleanupPropertiesTests`, `UserWithdrawalExternalCleanupSchedulerTests`를 추가했다. 기존 사용자 미커밋 문서 변경은 보존했다.
- 구현 내용: `findAndModify`로 pending·due retry·expired in-progress lifecycle을 오래된 순서로 atomic claim하고 claim마다 새 lease token을 저장한다. 모든 renew·retry·reconciliation·complete update를 withdrawalId·status·lease token·version으로 fencing하고 version을 증가시킨다. completed lifecycle은 외부 claim과 분리한 local atomic handoff로 `IDENTITY_RELEASE_PENDING`에 이동한다.
- 구현 내용: target guard는 현재 claim·lease 만료·version, WITHDRAWN User와 FirebaseIdentity의 userId·project·UID 일치를 검사한다. mismatch·부분 null target은 Firebase mutation 0건으로 reconciliation하고 완전 null LOCAL/GUEST target은 외부 호출 없이 completed로 수렴한다. email·phone·provider subject로 target을 추정하지 않는다.
- 구현 내용: cleanup 전용 Firebase App과 독립 connect/read timeout을 추가했다. adapter는 configured project 일치 후 inspect, disable, refresh revoke, delete와 presence 확인을 SDK에 매핑하고 안전한 enum 오류만 외부에 노출한다. Firebase User 부재는 멱등 성공이며 delete timeout·unavailable·unknown은 presence를 재조회한다. Google·Kakao는 현재 upstream 계정 삭제 의무가 없고 Apple은 승인된 revoke material 경로가 없으므로 reconciliation으로 차단한다.
- 구현 내용: retryable 오류는 attempt 기반 지수 backoff와 주입 가능한 bounded jitter를 사용한다. max attempt 초과 여부는 마지막 안전한 failure code와 별도 boolean으로 보존한다. scheduler는 completed handoff를 먼저 처리한 뒤 max batch 안에서 due cleanup을 순차 실행하며 기본 `enabled=false`다. cleanup 활성화 시 Firebase auth·project·양수 설정·batch 범위·단일 호출보다 긴 lease를 fail-closed 검증한다.
- 구현 내용: metric과 구조화 로그는 outcome·failure code·attempt bucket만 사용하고 NONE tick은 로그를 남기지 않는다. userId·Firebase UID·project·email·phone·provider subject·Token·credential·SDK 원문 message를 로그·metric label·snapshot·lifecycle 오류에 추가하지 않았다.
- 실행한 테스트와 결과: 단계별 targeted test를 반복 실행해 Repository update conflict와 테스트 fixture 문제를 수정했다. 최종 `./gradlew clean test`에서 전체 541개 테스트가 failure 0·error 0·skip 0으로 성공했다. `git diff --check`도 통과했다.
- 유지한 계약: 내부 User `WITHDRAWN`과 RefreshSession 폐기를 Firebase 장애로 되돌리지 않는다. Firebase 네트워크 호출을 Mongo Transaction에 넣지 않고, FirebaseIdentity·SocialIdentity·PhoneIdentity release와 `CLEANED`·재가입은 Stage 3 범위로 유지했다. 공개 API, 기존 응답, Identity JWT, Learning Core 계약과 phone eligibility 소유권은 변경하지 않았다.
- 결정사항: 로그인 검증과 탈퇴 cleanup의 Firebase timeout을 분리하기 위해 cleanup 전용 named Firebase App을 사용한다. Stage 2 local terminal은 `IDENTITY_RELEASE_PENDING`이며 implementation-order 체크는 실제 Firebase·Mongo staging E2E와 Apple revoke gate가 끝날 때까지 완료로 표시하지 않는다.
- 위험 요소: 실제 Firebase 권한·rate limit·timeout 결과와 Mongo lease takeover는 staging에서 검증하지 못했다. Apple authorization revoke material 경로도 미확정이므로 Apple lifecycle은 자동 삭제하지 않고 reconciliation으로 남는다. 이 검증 전 `FIREBASE_WITHDRAWAL_CLEANUP_ENABLED`와 `FIREBASE_WITHDRAWAL_ENABLED`를 production에서 활성화하면 안 된다.
- 다음 작업: 사용자가 변경을 커밋·PR한 뒤 staging에서 정상·already deleted·delete timeout absent/present·worker crash·동시 claim·stale completion·권한 오류를 검증하고 Apple revoke 경로를 확정한다. 그 후 Stage 3 Firebase/Social/Phone release와 `CLEANED` 재가입 gate 계획을 작성한다.

## 2026-08-25 — TMI-104 develop 병합 확인과 Jira 종료 차단

<!-- codex-turn:01a037ef-1845-7cb1-acd4-aad05d6525e9 -->

- 날짜: 2026-08-25
- 브랜치: `develop` (`origin/develop`과 동일한 `ca18fdc`; Codex commit·push 미수행)
- Jira: `TMI-104`. 사용자 요청은 상태를 `완료`로 전환하는 것이며 댓글·담당자·우선순위·라벨·설명은 유지한다. 실제 Jira 조회·수정·댓글·상태 전환은 수행하지 못했다.
- 작업 목표: TMI-104 구현의 develop 병합을 확인하고 Jira 현재 상태와 가능한 완료 전환을 재조회한 뒤 승인 범위의 상태 전환만 적용한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 구현 내용: 로컬 `develop`과 `origin/develop`이 PR #31 merge commit `ca18fdc`를 함께 가리키며 해당 merge가 TMI-104 구현 commit `7f1a113`을 포함하는 것을 확인했다. 작업 시작 당시 worktree는 clean이었다.
- Jira 작업: Atlassian 공식 연결로 `TMI-104` 읽기 전용 검색을 시도했으나 `The app is not installed on this instance` 403이 반환됐다. 플러그인 관리 지침을 확인했지만 현재 세션에는 Jira 연결을 새로 설치·복구하는 동작이 노출되지 않아 상태와 transition ID를 재조회하거나 완료 전환을 실행하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없어 Gradle 테스트는 다시 실행하지 않았다. 병합된 구현의 직전 `./gradlew clean test` 전체 541개 성공 결과를 유지하며 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Jira 연결이 복구되기 전 추정 transition을 전송하지 않았다. 댓글·담당자·우선순위·라벨·설명 등 승인 범위 밖 필드를 변경하지 않았고 Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 연결 복구 후 `TMI-104`의 현재 상태와 사용 가능한 완료 transition을 다시 조회하고, 사용자에게 상태만 `완료`로 바꾼다는 변경안을 보여준 뒤 승인받아 적용한다.
- 위험 요소: Jira는 아직 완료되지 않았을 수 있으며 현재 상태·Resolution을 확인할 수 없다. 저장소 구현은 병합됐지만 staging Firebase·Mongo E2E와 Apple revoke gate는 별도 production activation 선행 조건으로 남는다.
- 다음 작업: 사용자가 Atlassian Rovo/Jira 연결 앱을 다시 연결한 뒤 완료 전환을 재요청하면 읽기 전용 재조회부터 다시 수행한다.

## 2026-08-25 — TMI-104 미종료 상태 재확인

<!-- codex-turn:01a037f5-f77c-7723-9ed6-c74dfbab4064 -->

- 날짜: 2026-08-25
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: `TMI-104` 읽기 전용 조회 재시도 실패. Jira 수정·댓글·상태 전환 미수행
- 작업 목표: 직전 Jira 종료 요청의 실제 실행 여부와 현재 연결 상태를 다시 확인한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 구현 내용: Atlassian 공식 연결로 `TMI-104` 현재 상태 검색을 다시 시도했으나 `The app is not installed on this instance` 403이 동일하게 반환됐다. 완료 transition은 전송하지 못했으므로 Codex가 Jira를 닫았다고 볼 수 없다.
- 실행한 테스트와 결과: 코드 변경이 없어 Gradle 테스트는 실행하지 않았다. 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: 현재 상태와 transition을 확인하지 못한 상태에서 추정 전환을 보내지 않았다. 댓글·담당자·우선순위·라벨·설명과 상태를 변경하지 않았으며 Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: `TMI-104`는 Jira 연결이 복구되고 완료 전환의 성공 응답과 후속 상태를 확인할 때까지 미종료로 취급한다.
- 위험 요소: Jira 웹에서 다른 사용자가 별도로 상태를 변경했을 가능성은 연결 장애 때문에 확인할 수 없다.
- 다음 작업: 사용자가 Atlassian Rovo/Jira 연결 앱을 다시 연결하면 현재 상태와 가능한 transition을 재조회하고 승인된 완료 전환만 적용한다.

## 2026-08-25 — Atlassian Rovo 공식 재설치·재연결 경로 확인

- 날짜: 2026-08-25
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: `TMI-104` 상태 변경 미수행
- 작업 목표: 사용자가 이미 설치한 Atlassian Rovo에서 연결 앱 미설치 403이 발생하는 이유와 공식 설치·인증 링크를 확인한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 구현 내용: OpenAI 공식 Plugins 문서에서 ChatGPT/Codex의 Plugins 탭 설치, connector 인증, 설치 후 새 작업 시작 절차를 확인했다. Atlassian 공식 Rovo MCP 시작 문서에서 Codex Desktop의 Plugins 또는 Connectors에서 Atlassian Rovo 설치 후 OAuth 인증하는 절차와 공식 server URL을 확인했다.
- 구현 내용: Atlassian troubleshooting 문서에서 access denied는 site/product 권한 부족, 인증 세션·scope 문제일 수 있으며 연결 흐름 재실행, 재인증, 승인 scope와 Jira/Confluence 권한 확인이 필요함을 확인했다. 현재 오류는 Codex에 도구가 노출되지만 선택된 Atlassian 인스턴스가 앱 연결을 인정하지 않는 상태로 판단하며, 다른 계정·사이트를 선택했거나 연결 세션이 만료·해제됐을 가능성이 있다.
- 실행한 테스트와 결과: 코드 변경이 없어 Gradle 테스트는 실행하지 않았다. 공식 문서 페이지를 직접 열어 내용을 확인했고 문서 변경에 대해 `git diff --check`와 WORKLOG EOF append를 검증한다.
- 유지한 계약: 설치·인증 과정에서 계정 정보·인증 코드·Token을 입력하거나 전송하지 않았고 Jira 상태·댓글·다른 필드를 변경하지 않았다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: Codex Desktop에서 Atlassian Rovo 세부 화면을 열어 connector를 다시 인증하고 올바른 Jira 사이트와 scope를 승인한 뒤 새 작업에서 연결을 재검증한다. 직접 설치 deep link가 공식 문서에 제공되지 않아 Codex Plugins/Connectors UI와 공식 시작 문서를 사용한다.
- 위험 요소: 조직 관리자가 Rovo MCP 또는 Jira product access를 제한했다면 개인 재인증만으로 복구되지 않으며 Atlassian Admin 확인이 필요하다.
- 다음 작업: 재연결 후 새 Codex 작업에서 `TMI-104` 상태·완료 transition을 읽기 전용 재조회하고 사용자 승인 후 상태만 완료로 전환한다.

## 2026-08-25 — Atlassian Rovo 재연결 안내 turn 동기화

<!-- codex-turn:01a037f7-2a60-7fc2-84e2-fcea66145d4b -->

- 날짜: 2026-08-25
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: `TMI-104` 조회·수정·댓글·상태 전환 미수행
- 작업 목표: Atlassian Rovo 공식 설치·재연결 링크와 403 복구 절차 확인 결과를 종료 훅의 현재 turn 기준으로 작업 기록에 동기화한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 구현 내용: OpenAI 공식 Plugins 문서와 Atlassian 공식 Rovo MCP 설치·troubleshooting 문서를 근거로 Codex Desktop의 Plugins/Connectors에서 connector 재인증, 올바른 Atlassian 계정·사이트·scope 선택과 설치 후 새 작업 시작 절차를 안내했다. 직접 설치 deep link는 공식 문서에 제공되지 않음을 확인했다.
- 실행한 테스트와 결과: 코드 변경이 없어 Gradle 테스트는 실행하지 않았다. 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: 설치·인증을 대신 수행하거나 인증 정보를 입력하지 않았고 Jira 상태와 다른 필드를 변경하지 않았다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: 연결 복구와 새 작업에서의 도구 검증이 끝난 뒤에만 `TMI-104` 현재 상태·transition을 재조회하고 승인된 완료 전환을 수행한다.
- 위험 요소: Atlassian 조직 또는 product access 제한이 원인이면 사용자 재인증 외에 관리자 조치가 필요할 수 있다.
- 다음 작업: 사용자가 재연결을 완료하면 `TMI-104` 완료 전환을 다시 진행한다.

## 2026-08-25 — TMI-104 Jira 완료 전환

- 날짜: 2026-08-25
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: `TMI-104`. 사용자가 Atlassian Rovo 재연결 후 재시도를 요청해 상태를 `완료`로 전환했다.
- 작업 목표: PR #31로 `develop`에 병합된 Firebase 탈퇴 외부 cleanup worker 이슈를 Jira에서 종료한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- Jira 작업: `to-teacher` 사이트 연결을 확인하고 `TMI-104`의 현재 상태 `해야 할 일`, Resolution 없음과 사용 가능한 `완료` transition ID `41`을 조회했다. 승인된 범위대로 transition ID `41`만 적용했고, 후속 조회에서 상태와 Resolution이 모두 `완료`임을 확인했다. 댓글·담당자·우선순위·라벨·설명은 변경하지 않았다.
- 실행한 테스트와 결과: 코드 변경이 없어 Gradle 테스트는 다시 실행하지 않았다. 병합된 구현의 기존 `./gradlew clean test` 전체 541개 성공 결과를 유지하며 문서 변경에 대해 `git diff --check`를 실행한다.
- 유지한 계약: PR 병합을 확인한 이슈만 종료했고 승인 범위 밖 Jira 필드를 수정하지 않았다. Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: `TMI-104`는 Jira 상태와 Resolution 검증까지 끝나 완료로 취급한다. Stage 3 Firebase/Social/Phone release와 `CLEANED` 재가입 gate는 별도 후속 작업으로 유지한다.
- 위험 요소: 실제 Firebase·Mongo staging E2E와 Apple authorization revoke material 경로는 아직 production activation 선행 조건으로 남는다.
- 다음 작업: 고정 구현 순서의 3단계 Firebase/Social/Phone release와 `CLEANED` 재가입 gate Jira와 저장소 계획서를 준비한다.

## 2026-08-25 — TMI-104 Jira 완료 전환 turn 동기화

<!-- codex-turn:01a037fb-e1f7-70f2-95c5-8b0a954b0f15 -->

- 날짜: 2026-08-25
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: `TMI-104`. 사용자 승인에 따라 transition ID `41`만 적용해 상태를 `완료`로 변경했고, 후속 조회에서 Resolution도 `완료`임을 확인했다.
- 작업 목표: Atlassian Rovo 재연결 후 수행한 Jira 종료 결과를 종료 훅의 현재 turn 식별자로 작업 기록에 동기화한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 구현 내용: 직전 WORKLOG의 Jira 완료 전환 기록을 유지하고, 지정된 turn marker가 포함된 동기화 항목을 EOF에 추가했다. CURRENT_STATE에는 Jira 완료와 기록 동기화 상태를 반영했다.
- 실행한 테스트와 결과: 코드 변경이 없어 Gradle 테스트는 재실행하지 않았다. 병합된 구현의 기존 `./gradlew clean test` 전체 541개 성공 결과를 유지하며 `git diff --check`, marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Jira 댓글·담당자·우선순위·라벨·설명은 변경하지 않았고 Secret·Token·Password·실제 Key·전체 MongoDB URI와 사용자 개인정보를 기록하지 않았다.
- 결정사항: `TMI-104`는 상태와 Resolution이 모두 `완료`이므로 종료된 이슈로 관리한다.
- 위험 요소: 실제 Firebase·Mongo staging E2E와 Apple authorization revoke material 경로는 production activation 전에 별도로 검증해야 한다.
- 다음 작업: 고정 구현 순서의 3단계 Firebase/Social/Phone release와 `CLEANED` 재가입 gate를 별도 Jira와 계획서로 진행한다.

## 2026-08-25 — Stage 3 identity release와 CLEANED 재가입 gate 설명

<!-- codex-turn:01a037fe-1e8f-7453-9c54-f27b8412f14c -->

- 날짜: 2026-08-25
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 신규 이슈 생성·조회·수정·댓글·상태 전환 미수행. 직전 `TMI-104`는 완료 상태를 유지한다.
- 작업 목표: 고정 구현 순서의 다음 작업인 FirebaseIdentity·SocialIdentity·PhoneIdentity release와 `CLEANED` 재가입 gate가 해결할 문제, 구현 범위와 사용자 흐름을 현재 코드 기준으로 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 구현 내용: Stage 2 terminal이 `IDENTITY_RELEASE_PENDING`이고 현재 FirebaseIdentity·SocialIdentity의 unconditional unique index와 PhoneIdentity·PhoneFingerprintAlias의 ACTIVE partial unique index가 탈퇴 계정의 credential 점유를 유지하는 것을 확인했다. FirebaseExchangeService와 signup conflict 분류도 cleanup 중 같은 SNS·전화번호 재시도를 모두 일관된 pending 오류로 수렴시키지 못하는 공백이 있다.
- 구현 내용: 권장 Stage 3은 외부 삭제 완료·WITHDRAWN User·lifecycle version과 identity owner를 검증한 뒤 한 Mongo Transaction에서 FirebaseIdentity·SocialIdentity 점유 제거, PhoneIdentity·aliases RELEASED, active eligibility binding 부재 확인과 lifecycle의 `identitiesReleasedAt`·`cleanedAt` 기록 및 `CLEANED` 전환을 함께 commit한다. 완전히 이미 release된 상태는 멱등 성공으로, 혼합·owner mismatch 상태는 mutation 없이 reconciliation으로 분류한다.
- 구현 내용: `CLEANED` 전에는 Firebase UID·Social provider subject·phone alias가 WITHDRAWN owner를 가리키면 `WITHDRAWAL_CLEANUP_PENDING`으로 차단하고, CLEANED commit 후에는 새 Firebase enrollment와 새 canonical UUID User 생성을 허용한다. 기존 WITHDRAWN User tombstone과 프로필·시험 등 과거 데이터는 새 User에 자동 연결하지 않고 Billing의 TrialClaim도 삭제하지 않는다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석 작업이라 Gradle 테스트는 실행하지 않았다. 관련 구현 순서·Stage 1/2 계획서, lifecycle 상태·Repository, identity entity/index와 exchange/signup conflict 경로를 읽어 확인했으며 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Identity는 인증 identity와 계정 lifecycle만 소유하며 시험·TrialClaim·Entitlement를 추가하지 않는다. raw phone·fingerprint·Firebase Token·provider credential과 SDK 원문 오류를 기록하지 않고 새 가입은 이전 User 복구나 자동 merge가 아닌 새 UUID 생성으로 유지한다.
- 결정사항: Stage 3은 외부 네트워크 호출 없는 짧은 Mongo Transaction으로 구현하고, FirebaseIdentity·SocialIdentity는 외부 삭제 확인 뒤 내부 mapping을 제거하며 PhoneIdentity·PhoneFingerprintAlias는 기존 RELEASED lifecycle을 사용한다. eligibility REVOKED outbox의 생성 불변식은 확인하되 외부 delivery 완료를 법적 탈퇴 cleanup의 영구 차단 조건으로 삼지 않는다.
- 위험 요소: 현재 lifecycle enum에는 identity release 전용 in-progress/retry 상태가 없으므로 Transaction CAS와 write conflict 재시도로 충분한지 계획서에서 고정해야 한다. cleanup 전 새 Firebase UID가 같은 SocialIdentity를 제시하면 현재는 SOCIAL_IDENTITY_CONFLICT가 될 수 있고 phone 충돌도 PHONE_ALREADY_LINKED가 될 수 있어 중앙 withdrawal gate 보완이 필요하다.
- 다음 작업: Stage 3 저장소 구현 계획서를 작성하고 Jira 생성안을 승인받은 뒤, transaction/repository와 exchange·signup gate 테스트부터 구현한다.

## 2026-08-25 — Stage 3 identity release 구현 계획서 작성

<!-- codex-turn:01a03815-5e7b-7253-924f-6583c76a3f74 -->

- 날짜: 2026-08-25
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 신규 이슈 생성·조회·수정·댓글·상태 전환 미수행. 직전 `TMI-104`는 완료 상태를 유지한다.
- 작업 목표: FirebaseIdentity·SocialIdentity·PhoneIdentity release와 `CLEANED` 재가입 gate의 Stage 3 저장소 구현 계획서를 작성한다.
- 변경 파일: 신규 `docs/contracts/firebase-withdrawal-identity-release-stage-3-plan.md`, 수정 `docs/contracts/firebase-auth-follow-up-implementation-order.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: Stage 2의 `IDENTITY_RELEASE_PENDING`만 입력으로 받아 externalDeletedAt, WITHDRAWN User, lifecycle target·version과 identity owner를 검증하도록 계획했다. FirebaseIdentity·SocialIdentity mapping 제거, PhoneIdentity·PhoneFingerprintAlias RELEASED, active eligibility binding REVOKED 보장, identitiesReleasedAt·cleanedAt 기록과 `CLEANED` 전환을 한 Mongo Transaction으로 묶었다.
- 구현 내용: Stage 3은 외부 호출 없는 짧은 Transaction이므로 별도 identity-release in-progress lease를 추가하지 않고 status·version CAS, Mongo write conflict와 전체 rollback으로 동시성을 처리한다. fully released pending은 멱등 CLEANED, partial·owner mismatch는 다른 User mutation 없이 reconciliation으로 분류한다.
- 구현 내용: 중앙 `WithdrawalEnrollmentGate`를 Firebase exchange·signup·Guest upgrade/merge·auth method owner 경계에 적용해 CLEANED 전 같은 SNS·phone 사용을 `WITHDRAWAL_CLEANUP_PENDING`으로 통합하고, CLEANED 뒤 mapping이 모두 해제된 경우에만 fresh proof·phone 재검증·동의로 새 UUID User를 생성하도록 계획했다. 기존 WITHDRAWN User·과거 데이터·Billing TrialClaim은 복구하거나 삭제하지 않는다.
- 구현 내용: 데이터 처리 정책, Repository·application 구조, safe failure code, 기본 비활성 scheduler 설정, observability, read-only migration 집계, Transaction rollback·동시성·재가입 테스트, staging과 배포 순서, 완료 조건과 Stage 4 인계를 문서화하고 고정 구현 순서 3단계에 링크했다.
- 실행한 테스트와 결과: 문서 작업이므로 Gradle 테스트는 실행하지 않았다. `git diff --check`, 신규 계획서 존재, 구현 순서 링크, 핵심 lifecycle·gate 계약, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: User tombstone과 UUID를 유지하고 재가입을 신규 User 생성으로 처리한다. Identity에는 시험·TrialClaim·Entitlement 코드를 추가하지 않으며 Token·UID·provider subject·raw phone·fingerprint·SDK 원문 오류와 Secret을 문서·로그·metric에 기록하지 않는다.
- 결정사항: FirebaseIdentity·SocialIdentity는 Stage 2 외부 삭제 확인 뒤 exact owner mapping을 hard delete하고, PhoneIdentity·alias는 기존 RELEASED lifecycle을 사용한다. eligibility REVOKED outbox 생성은 보장하되 외부 delivery 완료를 CLEANED의 영구 차단 조건으로 사용하지 않는다.
- 위험 요소: CLEANED 전 새 Firebase UID가 같은 SocialIdentity를 제시하거나 같은 phone alias를 사용하는 현재 경로는 서로 다른 conflict를 반환하므로 중앙 gate 구현이 필요하다. actual Mongo Transaction write conflict와 worker crash 경계는 staging replica set에서 검증해야 한다.
- 다음 작업: 계획서를 기준으로 Stage 3 Jira 생성안을 작성해 사용자 승인을 받은 뒤 새 브랜치에서 Transaction·Repository·gate 테스트부터 구현한다.

## 2026-08-25 — Stage 3 Firebase·Social hard delete 의미 확인

<!-- codex-turn:01a03826-a24f-7f32-a397-8aae932666a8 -->

- 날짜: 2026-08-25
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 신규 이슈 생성·조회·수정·댓글·상태 전환 미수행. 직전 `TMI-104`는 완료 상태를 유지한다.
- 작업 목표: Stage 3 계획의 `exact FirebaseIdentity 삭제`와 `User 소유 SocialIdentity 전체 삭제`가 실제 물리 삭제인지, 무엇이 보존되는지 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. Stage 3 계획서와 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: 현재 계획의 7·8단계는 `firebase_identities`와 `social_identities`의 내부 mapping document를 실제 hard delete한다. User는 WITHDRAWN tombstone으로 유지하고 PhoneIdentity·PhoneFingerprintAlias는 RELEASED로 보존하며, 외부 Google·Kakao·Apple 원본 계정과 Learning Core 데이터·Billing TrialClaim은 삭제하지 않는다.
- 구현 내용: 현재 FirebaseIdentity와 SocialIdentity에는 status·releasedAt이 없고 unique index가 unconditional이므로 문서를 그대로 보존하면 동일 Firebase UID·provider subject의 신규 가입이 계속 충돌한다. hard delete는 Stage 2 외부 삭제 확인, exact owner 검증과 단일 Mongo Transaction rollback을 전제로 한다.
- 구현 내용: mapping 이력을 보존하려면 hard delete 대신 FirebaseIdentity·SocialIdentity에 ACTIVE/RELEASED·releasedAt을 추가하고 unique index를 ACTIVE partial unique로 교체하며 모든 조회를 active-only로 변경하는 별도 migration이 필요하다. provider subject와 UID를 탈퇴 후 보존할 정책·기간·법적 근거도 함께 정해야 한다.
- 실행한 테스트와 결과: 코드·계획서 변경이 없는 설명 작업이라 Gradle 테스트는 실행하지 않았다. 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: User tombstone과 canonical userId를 유지하고 재가입은 새 UUID User로 처리한다. 시험·TrialClaim·Entitlement를 Identity에서 삭제하거나 변경하지 않으며 Token·provider credential·raw phone·fingerprint와 Secret을 기록하지 않는다.
- 결정사항: 이번 turn에는 기존 hard-delete 계획을 변경하지 않았다. 실제 구현 전 hard delete를 유지할지 mapping soft release로 전환할지는 사용자의 제품·보존 정책 선택을 반영해 계획서에서 확정해야 한다.
- 위험 요소: hard delete는 provider mapping audit 정보를 제거하고, soft release는 탈퇴 후 외부 식별자를 보존하며 index·query migration 복잡도를 만든다. 문서를 그대로 두면서 재가입만 허용하는 방식은 unique ownership 충돌 때문에 사용할 수 없다.
- 다음 작업: 사용자가 hard delete 유지 또는 soft release 전환을 선택하면 Stage 3 계획서를 그 결정에 맞게 확정한 뒤 Jira 생성안을 준비한다.

## 2026-08-25 — Stage 3 중앙 withdrawal enrollment gate 설명

<!-- codex-turn:01a03829-69af-7b22-bc79-e36029401ffa -->

- 날짜: 2026-08-25
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 신규 이슈 생성·조회·수정·댓글·상태 전환 미수행. 직전 `TMI-104`는 완료 상태를 유지한다.
- 작업 목표: Stage 3 계획의 중앙 gate가 무엇이며 Firebase exchange·signup·Guest·merge·auth methods sync 경로에 왜 적용되는지 현재 코드 기준으로 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. Stage 3 계획서와 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: 중앙 `WithdrawalEnrollmentGate`는 HTTP filter나 공개 endpoint가 아니라 기존 FirebaseIdentity·SocialIdentity·PhoneFingerprintAlias owner userId를 입력받아 User status와 withdrawal lifecycle을 공통 분류하는 application service다. owner가 없으면 신규 enrollment를 계속하고, ACTIVE owner는 기존 로그인·merge·일반 conflict를 유지하며, WITHDRAWN owner가 CLEANED 전이면 `WITHDRAWAL_CLEANUP_PENDING`을 반환한다.
- 구현 내용: Firebase exchange는 기존 FirebaseIdentity뿐 아니라 새 Firebase UID가 제시한 SocialIdentity owner도 gate로 분류한다. direct signup은 사전 owner 검사와 DuplicateKeyException 재분류에서 withdrawn owner를 일반 enrollment·social·phone conflict와 구분한다. Guest prepare·upgrade는 active owner일 때만 merge를 제안하고 cleanup 중 owner면 enrollment attempt·승격을 중단한다.
- 구현 내용: Guest merge target resolver는 target이 ACTIVE MEMBER일 때만 merge하고 WITHDRAWN cleanup owner를 generic target conflict로 숨기지 않는다. auth methods sync는 ACTIVE 회원이 추가하려는 provider subject가 cleanup 중 다른 User에게 점유된 경우 새 SocialIdentity를 저장하지 않고 pending으로 차단한다. CLEANED인데 ACTIVE mapping이 남아 있으면 재가입을 허용하지 않고 stale mapping 불변식 위반으로 fail-closed 처리한다.
- 실행한 테스트와 결과: 코드·계획서 변경이 없는 설명 작업이라 Gradle 테스트는 실행하지 않았다. 관련 다섯 application 경로의 현재 owner 조회·conflict 분기를 읽어 확인했고 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: client가 보낸 userId로 owner를 판정하지 않고 Repository mapping과 canonical User status를 사용한다. gate는 identity를 수정하거나 자동 merge하지 않으며 Token·UID·provider subject·raw phone·fingerprint와 Secret을 기록하지 않는다.
- 결정사항: 경로마다 cleanup 판정을 복사하지 않고 공통 application gate로 모아 같은 withdrawn owner가 `SOCIAL_IDENTITY_CONFLICT`, `PHONE_ALREADY_LINKED`, `MERGE_REQUIRED` 등 서로 다른 결과로 노출되지 않게 한다.
- 위험 요소: gate가 ACTIVE owner의 기존 정책까지 덮어쓰면 정상 로그인·Guest merge가 깨질 수 있으므로 owner 없음·ACTIVE·WITHDRAWN pending·WITHDRAWN CLEANED/stale·lifecycle 없음 상태를 분리해 테스트해야 한다.
- 다음 작업: hard delete 또는 soft release 정책을 확정한 뒤 Stage 3 Jira 생성안을 준비하고, 구현 시 중앙 gate의 상태별 contract test부터 작성한다.

## 2026-08-25 — 다중 SNS 탈퇴와 Provider별 로그아웃 정책 점검

<!-- codex-turn:01a0382e-1085-7bb1-978b-a28cb3043891 -->

- 날짜: 2026-08-25
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 신규 이슈 생성·조회·수정·댓글·상태 전환 미수행. 직전 `TMI-104`는 완료 상태를 유지한다.
- 작업 목표: 하나의 User에 여러 SNS가 연결된 경우 탈퇴가 다른 로그인 기기·Provider에 미치는 영향과 SNS 하나만 로그아웃하는 기능의 필요성을 점검한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. Stage 3 계획서와 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: Google·Apple·Kakao 등 여러 Provider가 연결돼도 FirebaseIdentity와 SocialIdentity는 하나의 canonical userId를 가리키므로 탈퇴 단위는 Provider가 아니라 User 전체다. 현재 `UserWithdrawalTransactionService`는 해당 userId의 모든 active RefreshSession을 `ACCOUNT_WITHDRAWN`으로 폐기해 어느 Provider·기기에서 발급된 세션이든 재발급을 차단한다.
- 구현 내용: Stage 2의 Firebase refresh revoke·User delete는 같은 Firebase UID에 link된 모든 Provider의 향후 Firebase 인증을 막고, Stage 3 중앙 gate는 cleanup 중 어느 linked SNS로 재시도해도 pending으로 수렴시킨다. 다른 기기 앱은 Refresh 실패 또는 Identity의 User 상태 확인 시 local Token을 지우고 탈퇴 안내를 표시해야 한다.
- 구현 내용: 자체 Access Token은 RS256 stateless이며 기본 TTL이 30분이고 Learning Core가 Identity를 매 요청 호출하지 않으므로, 이미 발급된 Token은 만료까지 downstream에서 수락될 수 있다. 탈퇴 즉시 모든 서비스 접근을 차단해야 한다면 per-request introspection 대신 withdrawal event 기반 deny marker, token version 또는 더 짧은 TTL 중 별도 계약이 필요하다.
- 구현 내용: 현재 단일 logout은 요청 Refresh Token이 가리키는 RefreshSession 한 건만 폐기하므로 사실상 현재 기기·세션 로그아웃이다. 세션은 Provider별로 소유되지 않으므로 `Google만 로그아웃` 같은 서버 기능은 별도 의미가 약하다. 특정 SNS를 로그인 수단에서 제거하려는 요구는 logout이 아니라 후속 Stage 8 Provider unlink이며 fresh reauth, 마지막 인증수단 제거 금지, Firebase unlink·SocialIdentity 정합성과 필요 시 logout-all 정책이 필요하다.
- 실행한 테스트와 결과: 코드·계획서 변경이 없는 분석 작업이라 Gradle 테스트는 실행하지 않았다. withdrawal Transaction, 단일·전체 logout, RefreshSession revocation reason, Token reissue의 User ACTIVE 검사와 Access Token TTL 설정을 읽어 확인했고 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: 실제 userId 단위 계정 상태와 Session을 사용하고 Provider를 별도 내부 계정으로 취급하지 않는다. Learning Core는 JWT를 로컬 검증하며 매 요청 Identity introspection을 추가하지 않고 Token·Refresh Token 원문·Provider credential과 Secret을 기록하지 않는다.
- 결정사항: MVP에는 현재 세션 로그아웃과 전체 로그아웃을 유지하고 Provider별 로그아웃 API는 추가하지 않는다. 특정 SNS 제거는 Stage 8 Provider unlink로 구현하며 회원탈퇴는 모든 linked Provider와 모든 RefreshSession에 적용되는 계정 전체 행위로 유지한다.
- 위험 요소: 다른 기기의 이미 발급된 Access Token은 만료까지 일부 downstream 요청에 사용될 수 있고, client가 Refresh 실패를 일반 로그인 만료로만 표시하면 사용자가 탈퇴 사실을 이해하지 못할 수 있다. 즉시 차단 수준과 client 오류 UX를 별도 계약으로 확정해야 한다.
- 다음 작업: hard delete 또는 soft release 정책과 함께 탈퇴 후 Access Token 즉시 차단 요구 수준을 확정한 뒤 Stage 3 Jira 생성안을 준비한다. Provider unlink는 고정 구현 순서 Stage 8에서 별도 계획한다.

## 2026-08-25 — 단일·전체 로그아웃 Firebase/SNS 처리 현황 확인

<!-- codex-turn:01a03831-aa95-7ec2-a618-2e60e896553a -->

- 날짜: 2026-08-25
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 신규 이슈 생성·조회·수정·댓글·상태 전환 미수행. 직전 `TMI-104`는 완료 상태를 유지한다.
- 작업 목표: 현재 기기 로그아웃과 모든 기기 로그아웃에 Firebase/SNS 세션 처리가 구현돼 있는지 코드와 테스트로 확인한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. Stage 3 계획서와 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: `LogoutService`는 요청 Refresh Token hash로 Identity RefreshSession 한 건을 찾아 `RevocationReason.LOGOUT`으로 폐기할 뿐 Firebase SDK·FirebaseIdentity·SocialIdentity를 호출하지 않는다. 서버는 특정 기기의 Firebase local session을 직접 signOut할 수 없으므로 모바일 client가 서버 logout 성공과 함께 Firebase client signOut, 자체 Access/Refresh Token 삭제를 수행해야 한다.
- 구현 내용: `LogoutAllService`는 Access Token userId로 모든 active Identity RefreshSession을 찾아 `RevocationReason.LOGOUT_ALL`로 폐기하지만 Firebase Admin의 `revokeRefreshTokens(uid)`를 호출하지 않는다. 현재 Firebase revoke adapter는 withdrawal worker에서만 사용된다.
- 구현 내용: 따라서 전체 로그아웃 후에도 다른 기기 또는 현재 기기의 Firebase SDK가 유효한 Firebase refresh credential을 가지고 있으면 새 ID Token을 발급받아 `/firebase/exchange`로 Identity Session을 다시 만들 수 있다. 이는 고정 구현 순서 Stage 6 `logout-all Firebase refresh revoke`가 해결해야 하는 현재 공백이다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석 작업이라 Gradle 테스트는 실행하지 않았다. AuthController, LogoutService, LogoutAllService, RefreshSession revocation과 전체 저장소의 Firebase `revokeRefreshTokens` 호출 위치 및 기존 단위 테스트를 읽어 확인했고 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: 현재 기기 logout은 Identity RefreshSession 한 건의 멱등 폐기이며 Provider 연결 해제가 아니다. logout-all은 canonical userId의 내부 Session 전체를 대상으로 하고 Token 원문·Firebase credential과 Secret을 기록하지 않는다.
- 결정사항: 단일 logout에는 서버측 Firebase 전체 revoke를 추가하지 않고 client local Firebase signOut을 명시한다. logout-all에는 Stage 6에서 FirebaseIdentity target을 검증한 뒤 Firebase refresh revoke를 추가해야 하며 Provider unlink는 Stage 8로 유지한다.
- 위험 요소: 모바일 client가 Firebase signOut을 누락하면 단일 logout 직후 자동 exchange로 재로그인될 수 있다. logout-all Firebase revoke가 구현되기 전에는 모든 기기에서 강제 로그아웃됐다는 보장을 제공할 수 없다. 이미 발급된 자체 Access Token은 별도 만료·deny 정책의 영향을 받는다.
- 다음 작업: Stage 3 정책을 확정해 Jira를 진행하고, 고정 순서에 따라 Stage 6에서 logout-all Firebase revoke와 client logout 계약을 별도 계획·구현한다.

## 2026-08-26 — 다중 SNS 연결 계정의 탈퇴 후속 범위 재확인

<!-- codex-turn:01a03bc1-5983-77c0-8b8f-b13acd54dcee -->

- 날짜: 2026-08-26
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 신규 이슈 생성·조회·수정·댓글·상태 전환 미수행. 직전 `TMI-104`는 완료 상태를 유지한다.
- 작업 목표: Google·Apple·Kakao 등 여러 SNS가 하나의 canonical User에 연결된 경우 계정 전체 탈퇴 처리가 앞으로 구현할 Stage 3과 후속 순서에 포함돼 있는지 재확인한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드와 Stage 1~3 계획서는 변경하지 않았다.
- 구현 내용: Stage 1은 탈퇴를 요청한 Provider 하나가 아니라 canonical userId를 기준으로 User를 `WITHDRAWN` 처리하고 모든 내부 RefreshSession을 `ACCOUNT_WITHDRAWN`으로 폐기한다. Stage 2는 같은 Firebase UID의 User를 disable·refresh revoke·delete해 Firebase에 연결된 Provider 전체의 향후 인증 기반을 제거한다.
- 구현 내용: Stage 3 계획은 `findAllByUserId`로 탈퇴 User가 소유한 SocialIdentity 전체를 수집하고, SocialIdentity 0개·1개·여러 개 삭제를 명시적으로 테스트한다. FirebaseIdentity 제거, 모든 SocialIdentity 제거, PhoneIdentity·alias release와 lifecycle `CLEANED` 전환을 한 Mongo Transaction으로 묶어 일부 Provider mapping만 남는 상태를 허용하지 않는다.
- 구현 내용: cleanup 완료 전 어느 linked SNS로 로그인·가입을 재시도하더라도 중앙 `WithdrawalEnrollmentGate`에서 `WITHDRAWAL_CLEANUP_PENDING`으로 차단한다. 특정 SNS 하나만 연결 해제하는 행위는 계정 탈퇴나 SNS별 logout이 아니라 고정 구현 순서 Stage 8 Provider unlink 범위다.
- 실행한 테스트와 결과: 코드·계획서 변경이 없는 분석 작업이라 Gradle 테스트는 실행하지 않았다. Stage 1~3 계획서와 구현 순서 문서의 전체 Session 폐기, Firebase 외부 User cleanup, User 소유 SocialIdentity 전체 삭제, 다중 SocialIdentity 테스트와 재가입 gate 항목을 읽어 확인했다. 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: 여러 SNS를 별도 내부 계정으로 취급하지 않고 실제 UUID userId의 로그인 수단으로 유지한다. 탈퇴는 User 전체에 적용하며 Identity는 Learning Core 데이터나 Billing TrialClaim을 직접 삭제하지 않고 Token·UID·provider subject·credential과 Secret을 기록하지 않는다.
- 결정사항: 다중 SNS 탈퇴는 이미 Stage 1~3의 계정 전체 lifecycle 범위에 포함돼 있다. 특정 Provider 하나의 unlink는 Stage 8로 분리하며 이번 turn에는 기존 hard-delete 계획이나 즉시 Access Token 차단 정책을 변경하지 않았다.
- 위험 요소: 현재 Stage 3 계획에는 SocialIdentity 여러 개 삭제 테스트가 있지만, 연결된 Provider 각각으로 cleanup 전·후 로그인·가입을 시도하는 매트릭스가 한 항목으로 명시돼 있지는 않다. 구현 계획 확정 시 모든 linked Provider가 pending으로 수렴하고 CLEANED 뒤 새 enrollment로 진행하는 contract test를 명시적으로 추가해야 한다. 이미 발급된 stateless Access Token의 즉시 전역 차단은 여전히 별도 정책이 필요하다.
- 다음 작업: hard delete 또는 soft release 정책과 탈퇴 후 Access Token 즉시 차단 수준을 확정한 뒤 Stage 3 Jira 생성안을 승인받고, 구현 시 다중 Provider cleanup·gate contract test를 포함한다.

## 2026-08-26 — 탈퇴 후 다른 기기 안내와 downstream 즉시 차단 계획 여부 확인

<!-- codex-turn:01a03bc4-f1a3-7e93-96f0-7f37d14073d5 -->

- 날짜: 2026-08-26
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 신규 이슈 생성·조회·수정·댓글·상태 전환 미수행. 직전 `TMI-104`는 완료 상태를 유지한다.
- 작업 목표: 여러 SNS가 연결된 User의 탈퇴 뒤 다른 기기 Refresh 요청에 전용 오류를 반환하고 앱이 탈퇴 안내·로컬 Token 정리를 수행하는 계약 및 기존 Access Token의 downstream 즉시 차단이 후속 계획에 포함됐는지 확인한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드와 계약·구현 순서 문서는 변경하지 않았다.
- 구현 내용: 탈퇴 Transaction은 해당 userId의 모든 RefreshSession에 `RevocationReason.ACCOUNT_WITHDRAWN`을 저장한다. 그러나 `TokenReissueService`는 `ROTATED` 재사용만 별도 분류하고 그 밖의 revoked Session은 모두 `AuthErrorStatus.INVALID_REFRESH_TOKEN`으로 반환한다. 기존 withdrawal lifecycle 테스트도 탈퇴 전 모든 Refresh Token이 `INVALID_REFRESH_TOKEN`을 반환하는 현재 동작을 명시적으로 검증한다.
- 구현 내용: Stage 1 계획에는 cleanup 중 같은 credential의 login·enrollment에 `WITHDRAWAL_CLEANUP_PENDING`을 반환하고 client UX를 검증하는 항목은 있지만, 폐기된 RefreshSession의 `ACCOUNT_WITHDRAWN` 전용 외부 오류, 앱의 Firebase signOut·모든 로컬 Token 삭제·탈퇴 안내 계약은 정의돼 있지 않다.
- 구현 내용: 이미 발급된 stateless Access Token은 기본 최대 30분 동안 Learning Core에서 로컬 검증될 수 있다. Stage 1 계획은 Access Token 즉시 deny를 명시적으로 제외하며, `UserWithdrawn` event와 downstream deny marker는 작업 기록·CURRENT_STATE에서 별도 이슈 필요성으로만 논의됐다. 현재 10단계 고정 구현 순서에는 이 cross-service 작업이 등록돼 있지 않다.
- 실행한 테스트와 결과: 코드·계획서 변경이 없는 분석 작업이라 Gradle 테스트는 실행하지 않았다. `TokenReissueService`, withdrawal lifecycle 테스트, Stage 1~3 계획서와 10단계 구현 순서를 읽어 확인했다. 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Refresh Token 원문과 credential을 기록하지 않으며 Learning Core는 매 요청 Identity introspection을 호출하지 않고 JWKS 기반 로컬 JWT 검증을 유지한다. 즉시 차단을 도입한다면 Identity event와 downstream 로컬 deny marker 방식으로 책임을 분리해야 한다.
- 결정사항: 해당 문제는 이미 위험 요소로 논의됐지만 정식 후속 구현 단계로 확정된 상태는 아니다. 전용 Refresh 오류·client UX와 cross-service Access Token deny를 서로 다른 범위로 분리해 계획해야 하며 이번 turn에는 구현 순서나 기존 API 오류 계약을 변경하지 않았다.
- 위험 요소: 전용 탈퇴 오류 없이 앱은 탈퇴와 일반 Token 만료·로그아웃을 구분할 수 없다. 반대로 오류 code만 추가하고 모바일 처리를 배포하지 않으면 UX가 달라지지 않는다. downstream deny가 필요한데 일부 서비스 consumer만 배포하면 탈퇴한 Access Token이 서비스별로 다르게 허용될 수 있으므로 consumer 선배포와 feature activation gate가 필요하다.
- 다음 작업: 사용자 승인 아래 `탈퇴 Session 전용 오류·모바일 logout/안내 계약`과 선택적인 `UserWithdrawn event·Learning Core deny marker`를 고정 구현 순서에 추가하고 각각의 저장소 구현 계획서를 작성한다.

## 2026-08-26 — UserWithdrawn event와 downstream deny marker 설명

<!-- codex-turn:01a03bc8-dd87-7162-901d-03844d8d4a81 -->

- 날짜: 2026-08-26
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 신규 이슈 생성·조회·수정·댓글·상태 전환 미수행. 직전 `TMI-104`는 완료 상태를 유지한다.
- 작업 목표: 탈퇴한 Access Token을 downstream에서 차단하기 위한 `UserWithdrawn` event, Learning Core의 멱등 consumer와 로컬 deny marker, 매 요청 Identity 조회를 하지 않는다는 의미를 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드와 계약·구현 순서 문서는 변경하지 않았다.
- 구현 내용: Identity에서 모든 RefreshSession을 `ACCOUNT_WITHDRAWN`으로 폐기하면 새 Access Token 재발급은 막히지만 이미 발급된 stateless RS256 Access Token은 기본 최대 30분 동안 서명·issuer·audience·만료 검증을 통과할 수 있다. downstream 차단은 이 기존 Token으로 Learning Core 작업을 계속하는 공백을 줄이기 위한 별도 계층이다.
- 구현 내용: 즉시 차단 정책을 선택하면 Identity는 User `WITHDRAWN` 전환과 같은 내부 Transaction에 `UserWithdrawn` outbox를 기록하고 최소 eventId·schemaVersion·userId·withdrawnAt만 at-least-once 전달한다. Token 원문이나 Firebase credential은 event에 포함하지 않는다.
- 구현 내용: Learning Core는 eventId unique inbox로 중복 전달을 멱등 성공시키고 같은 local Transaction에서 userId 기반 deny marker를 저장한다. 이후 보호 API는 JWKS 기반 JWT 로컬 검증 뒤 `sub`가 deny marker에 존재하는지 Learning Core의 로컬 저장소 또는 캐시에서 확인하고, 존재하면 학습 데이터 조회·수정 전에 전용 탈퇴 오류로 거절한다.
- 구현 내용: 이 방식은 요청마다 Identity Service에 HTTP introspection을 호출하지 않는다. 토큰별 blacklist가 아니라 탈퇴한 canonical userId 전체의 actor 권한을 차단하며, 기존 시험·결과 데이터를 삭제하거나 신규 재가입 User를 막지 않는다. 재가입이 새 UUID라면 이전 userId deny marker의 영향을 받지 않는다.
- 실행한 테스트와 결과: 코드·계획서 변경이 없는 설명 작업이라 Gradle 테스트는 실행하지 않았다. 현재 Access Token TTL·JWKS 로컬 검증·회원탈퇴 Session 폐기 계약과 기존 `UserMerged` deny marker 설계를 기준으로 의미와 책임 경계를 정리했다. 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Learning Core는 매 요청 Identity introspection을 하지 않고 issuer·audience·JWKS 검증을 로컬로 유지한다. 실제 userId는 JWT `sub`를 사용하고 event에 Token·Provider credential·개인정보와 Secret을 포함하지 않으며 Identity에 시험·결과 코드를 추가하지 않는다.
- 결정사항: 이번 설명은 즉시 차단을 선택할 경우의 권장 구조이며 아직 구현 결정이나 고정 순서 편입은 아니다. userId 단위 deny marker를 사용하고 토큰 원문·jti별 blacklist는 만들지 않는 방향이 현재 경계에 맞다.
- 위험 요소: event 방식은 전달 지연만큼 짧은 허용 구간이 있어 절대적인 동시 즉시 차단은 아니다. Learning Core 외에 Identity Access Token을 받아 사용자 작업을 수행하는 서비스가 생기면 각 서비스도 consumer와 local deny gate를 구현해야 한다. local marker 조회의 성능·캐시 일관성, consumer 선배포와 publisher activation 순서를 계약해야 한다.
- 다음 작업: 제품이 최대 Access Token TTL 동안의 접근을 허용할지 near-real-time 차단을 요구할지 결정한 뒤, 필요하면 Identity outbox·publisher와 Learning Core inbox·deny gate를 별도 구현 계획과 Jira로 분리한다.

## 2026-08-26 — withdrawal deny marker 누적과 TTL 정리 설명

<!-- codex-turn:01a03bcc-4f44-7082-a00f-1e6008bceec5 -->

- 날짜: 2026-08-26
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 신규 이슈 생성·조회·수정·댓글·상태 전환 미수행. 직전 `TMI-104`는 완료 상태를 유지한다.
- 작업 목표: Learning Core의 탈퇴 userId deny marker와 event inbox가 영구 누적되는지, 안전하게 언제 정리할 수 있는지 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드와 계약·구현 순서 문서는 변경하지 않았다.
- 구현 내용: deny marker를 영구 저장하면 계속 누적되는 것이 맞다. 그러나 모든 RefreshSession이 폐기돼 새 Access Token을 발급할 수 없으므로 marker는 탈퇴 전에 발급된 Access Token의 최대 잔여 수명 동안만 필요하다.
- 구현 내용: 권장 marker는 Learning Core의 서비스 공용 영속 저장소에 userId와 `blockedUntil/expireAt`을 저장하고 TTL index로 정리한다. `expireAt`은 하드코딩한 30분이 아니라 `withdrawnAt + 시스템이 허용하는 최대 Access Token 수명 + verifier clock skew 안전 여유`로 계산한다. 각 pod의 메모리 목록은 재시작·다중 인스턴스에서 유실·불일치하므로 권위 저장소로 사용하지 않고 선택적 캐시만 둘 수 있다.
- 구현 내용: MongoDB TTL 삭제는 비동기라 문서가 만료 뒤 잠시 남을 수 있으므로 authorization은 문서 존재만 보지 않고 현재 시각이 `blockedUntil` 이전인지 직접 검사한다. 늦게 삭제되는 것은 접근을 더 오래 막는 오류가 되지 않도록 이 시간 판정이 필요하다.
- 구현 내용: eventId inbox는 중복 event와 payload conflict를 판정하는 별도 목적이므로 deny marker와 같은 짧은 TTL을 사용하지 않는다. source outbox의 최대 자동 재전달, dead-letter와 운영 수동 replay 기간보다 길게 보존한 뒤 별도 TTL로 정리한다. inbox가 정리된 뒤 아주 늦은 event가 다시 와도 old Access Token이 모두 만료된 상태라면 marker를 재생성하지 않고 안전하게 멱등 종료하도록 계약할 수 있다.
- 실행한 테스트와 결과: 코드·계획서 변경이 없는 설명 작업이라 Gradle 테스트는 실행하지 않았다. 현재 기본 Access Token TTL, stateless JWT와 MongoDB TTL의 비동기 특성, at-least-once event 중복 처리 경계를 기준으로 보존 정책을 정리했다. 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Learning Core는 매 요청 Identity introspection을 호출하지 않고 로컬 JWT 검증을 유지한다. Token 원문·Firebase credential·개인정보와 Secret을 marker나 inbox에 저장하지 않으며 Identity에 Learning Core 도메인 코드를 추가하지 않는다.
- 결정사항: deny marker는 영구 tombstone이 아니라 Access Token 최대 유효 구간을 덮는 단기 authorization fence로 설계하는 것이 적절하다. event inbox는 replay 정책에 맞춘 더 긴 별도 retention을 사용한다. 이번 turn에는 구체적인 기간이나 구현을 확정하지 않았다.
- 위험 요소: 운영 Access Token TTL을 늘리면서 marker retention을 함께 늘리지 않으면 유효 Token이 차단 해제 뒤 다시 사용될 수 있다. 반대로 TTL monitor 지연을 문서 존재만으로 판정하면 필요 이상으로 차단될 수 있다. source outbox replay 기간보다 inbox retention이 짧으면 매우 늦은 중복 event의 payload conflict 검증력이 사라진다.
- 다음 작업: 즉시 차단을 채택하면 Identity의 최대 발급 TTL·Learning Core clock skew·outbox replay 기간을 하나의 계약으로 고정하고 marker TTL index, 시간 기반 gate, inbox retention과 재전달 테스트를 구현 계획에 포함한다.

## 2026-08-26 — 탈퇴 전용 오류·모바일 UX와 downstream deny 작업 순서 추가

<!-- codex-turn:01a03bcf-73e8-7950-bc27-d73d1e95f657 -->

- 날짜: 2026-08-26
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 신규 이슈 생성·조회·수정·댓글·상태 전환 미수행. 직전 `TMI-104`는 완료 상태를 유지한다.
- 작업 목표: 사용자가 확정한 탈퇴 Session 전용 오류·모바일 UX와 탈퇴 Access Token downstream 차단을 보존 기간 정책과 함께 고정 Firebase 인증 후속 구현 순서에 추가한다.
- 변경 파일: `docs/contracts/firebase-auth-follow-up-implementation-order.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: 기존 완전 탈퇴 lifecycle 1~3단계 뒤에 4단계 `탈퇴 Session 전용 오류와 모바일 logout·안내 UX 계약`을 추가했다. `ACCOUNT_WITHDRAWN` 폐기 Session을 일반 invalid Refresh Token과 구분하는 외부 오류를 정의하고, 모바일이 이를 받으면 Firebase client signOut, 자체 Access·Refresh Token 전체 삭제와 “탈퇴 처리된 계정입니다” 안내를 수행하도록 범위를 고정했다.
- 구현 내용: 5단계 ``UserWithdrawn` event와 downstream Access Token deny marker`를 추가했다. Learning Core의 eventId 멱등 consumer와 userId 기반 로컬 deny gate를 Identity outbox·publisher보다 먼저 배포하고, 매 요청 Identity introspection 없이 JWKS 검증 뒤 Learning Core 저장소의 marker를 확인하도록 순서를 고정했다.
- 구현 내용: deny marker는 `withdrawnAt + 시스템이 허용하는 최대 Access Token 수명 + verifier clock skew 안전 여유`를 `blockedUntil/expireAt`으로 사용하고 TTL 정리한다. 요청 gate는 MongoDB TTL monitor 지연과 무관하게 `blockedUntil`을 직접 검사한다. event inbox는 source outbox의 최대 자동 재전달·dead-letter·운영 수동 replay 기간보다 길게 보존한 뒤 별도 TTL로 정리한다.
- 구현 내용: 기존 가입 중단 cleanup부터 ACTIVE 회원 Firebase rebind까지의 4~10단계를 6~12단계로 재번호화했다. 1~5단계와 Identity·Learning Core·모바일 staging E2E가 완료되기 전에는 Firebase/SNS withdrawal production flag를 활성화하지 않는 activation gate를 추가했다.
- 실행한 테스트와 결과: 문서 작업이라 Gradle 테스트는 실행하지 않았다. 구현 체크리스트 1~12 순서, 기존 단계 참조 번호, 4·5단계의 client·event·consumer·retention·activation 계약을 읽어 확인했다. 문서 변경에 대해 `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Learning Core는 JWKS 기반 로컬 JWT 검증을 유지하고 매 요청 Identity를 호출하지 않는다. Token 원문·Provider credential·개인정보와 Secret을 event·marker·inbox에 저장하지 않으며 Identity에는 시험·결과·deny consumer 코드를 추가하지 않는다.
- 결정사항: near-real-time 탈퇴 Access Token 차단을 구현 대상으로 채택하고 marker와 inbox 모두 유한 보존한다. consumer·모바일을 producer·서버 contract 활성화보다 먼저 준비하고 1~5단계를 withdrawal production 선행 묶음으로 본다.
- 위험 요소: 외부 오류를 먼저 활성화하고 모바일 처리가 배포되지 않으면 사용자 안내가 달라지지 않는다. Identity publisher를 Learning Core consumer보다 먼저 열면 기존 Token 차단 event를 잃을 수 있다. 운영 Access Token TTL·clock skew·outbox replay 기간 변경이 marker·inbox retention과 동기화되지 않으면 조기 차단 해제 또는 중복 검증 공백이 생길 수 있다.
- 다음 작업: Stage 3 hard delete 또는 soft release 정책을 확정해 Jira를 진행한 뒤, 고정 순서 4단계와 5단계 각각에 대해 API·모바일 handoff 및 Identity·Learning Core 구현 계획서를 작성하고 승인 후 별도 Jira로 구현한다.

## 2026-08-26 — 변경된 순서의 실제 다음 작업 Stage 3 설명

- 날짜: 2026-08-26
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 신규 이슈 생성·조회·수정·댓글·상태 전환 미수행. 직전 `TMI-104`는 완료 상태를 유지한다.
- 작업 목표: 탈퇴 전용 오류와 downstream deny 작업 추가 후 실제 다음 구현 단계가 무엇인지 설명하고 계획서의 후속 단계 번호 정합성을 확인한다.
- 변경 파일: `docs/contracts/firebase-withdrawal-identity-release-stage-3-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: 전체 고정 순서는 10단계에서 12단계로 확장됐지만 새 작업은 기존 Stage 3 뒤에 삽입했으므로 `TMI-104` Stage 2 다음 구현은 그대로 Stage 3 `FirebaseIdentity·SocialIdentity·PhoneIdentity release와 CLEANED 재가입 gate`다.
- 구현 내용: Stage 3 worker는 `IDENTITY_RELEASE_PENDING`, externalDeletedAt, WITHDRAWN User, lifecycle·FirebaseIdentity owner와 version을 검증한다. 하나의 Mongo Transaction에서 eligibility REVOKED 보장, exact FirebaseIdentity와 User 소유 SocialIdentity 전체 제거, PhoneIdentity·alias RELEASED, identitiesReleasedAt·cleanedAt 기록과 `CLEANED` 전환을 수행하며 하나라도 실패하면 전체 rollback한다.
- 구현 내용: cleanup 완료 전 같은 Firebase/SNS/phone 접근은 중앙 gate에서 `WITHDRAWAL_CLEANUP_PENDING`으로 차단하고, CLEANED 뒤에는 fresh Firebase proof·전화번호 재검증·필수 동의로 새 UUID User를 만든다. 기존 WITHDRAWN User와 과거 데이터는 복구·자동 merge하지 않는다.
- 구현 내용: 새 4·5단계 삽입으로 Stage 3 계획서가 가입 중단 cleanup과 Billing consumer를 가리키던 기존 `Stage 4·5` 참조가 잘못돼 새 번호 `Stage 6·7`로 정정했다. 가입 중단 enrollment 별도 lifecycle 참조도 `Stage 4`에서 `Stage 6`으로 바로잡았다.
- 실행한 테스트와 결과: 문서 설명·번호 정합성 수정이라 Gradle 테스트는 실행하지 않았다. Stage 3 목적·Transaction 순서·사용자 흐름·배포·완료 조건과 12단계 구현 목록을 대조했다. 문서 변경에 대해 `git diff --check`와 WORKLOG EOF append를 검증한다.
- 유지한 계약: 외부 Firebase 삭제 확인 전 내부 mapping을 해제하지 않고 다른 User identity를 추정 삭제하지 않는다. 기존 User는 WITHDRAWN tombstone으로 유지하며 재가입은 새 UUID이고 Identity는 Learning Core 데이터·Billing TrialClaim을 복구하거나 삭제하지 않는다.
- 결정사항: 즉시 다음 작업은 바뀌지 않은 Stage 3이고, 새 탈퇴 UX·downstream deny는 Stage 3 완료 뒤 Stage 4·5로 진행한다. 이번 turn에는 Stage 3 hard delete 계획, 공개 API와 Jira를 변경하지 않았다.
- 위험 요소: 현재 계획은 FirebaseIdentity·SocialIdentity hard delete를 사용한다. soft release로 변경하면 entity status·releasedAt, ACTIVE partial unique index, active-only query와 migration이 추가되므로 실제 구현 전 정책을 최종 확정해야 한다. 실제 Mongo Transaction rollback·write conflict는 staging replica set 검증이 필요하다.
- 다음 작업: Stage 3 hard delete 유지 또는 soft release 전환을 확정하고 Jira 생성안을 사용자에게 먼저 보여 승인받은 뒤, 새 브랜치에서 Transaction·Repository·central gate contract test부터 구현한다.

## 2026-08-26 — 종료 훅 Stage 3 다음 작업 설명 기록 동기화

<!-- codex-turn:01a03bd1-9492-7b00-8f71-7a4b11d6d1f0 -->

- 날짜: 2026-08-26
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 신규 이슈 생성·조회·수정·댓글·상태 전환 미수행. 직전 `TMI-104`는 완료 상태를 유지한다.
- 작업 목표: 종료 훅이 요구한 현재 turn marker로 변경된 구현 순서의 실제 다음 작업 설명과 문서 정합성 수정 결과를 WORKLOG EOF에 동기화한다.
- 변경 파일: `docs/contracts/firebase-withdrawal-identity-release-stage-3-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: 12단계로 확장된 목록에서도 `TMI-104` Stage 2 다음 작업은 Stage 3 identity release와 `CLEANED` 재가입 gate임을 재확인했다. 새로 추가한 탈퇴 Session UX와 downstream deny marker는 그 뒤의 Stage 4·5다.
- 구현 내용: Stage 3 계획서에서 가입 중단 Firebase User cleanup과 Billing consumer를 가리키던 낡은 `Stage 4·5` 참조를 현재 순서의 `Stage 6·7`로 정정하고 enrollment cleanup 설명의 `Stage 4`도 `Stage 6`으로 바로잡았다.
- 실행한 테스트와 결과: 문서 작업이라 Gradle 테스트는 실행하지 않았다. `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: 외부 Firebase 삭제 확인 전 내부 identity를 해제하지 않고 재가입은 새 UUID로 처리한다. Token·Provider credential·개인정보와 Secret을 기록하지 않으며 Identity에 Learning Core·Billing 도메인 코드를 추가하지 않는다.
- 결정사항: 즉시 다음 작업은 Stage 3이고 새 Stage 4·5는 Stage 3 완료 뒤 진행한다. Jira와 애플리케이션 계약은 변경하지 않았다.
- 위험 요소: 현재 Stage 3 계획의 FirebaseIdentity·SocialIdentity hard delete 또는 soft release 정책은 실제 구현·Jira 생성 전에 최종 확정해야 한다. Mongo Transaction rollback·동시성은 staging replica set 검증이 필요하다.
- 다음 작업: Stage 3 identity mapping 처리 정책을 확정한 뒤 Jira 생성안을 사용자에게 먼저 제시하고 승인 후 구현한다.

## 2026-08-26 — Stage 3 Jira 생성안 준비

- 날짜: 2026-08-26
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: `TMI-104` 후속 Stage 3 신규 이슈 생성 요청을 받았으나 실제 생성·수정·댓글·상태 전환은 수행하지 않았다. 공식 Atlassian Rovo 읽기 확인은 `app is not installed on this instance` 403으로 실패했다.
- 작업 목표: 저장소 Jira 변경 규칙에 따라 Stage 3 신규 이슈의 제목·유형·우선순위·설명·완료 조건·제외 범위를 먼저 작성해 사용자 승인을 요청한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드와 Stage 3 계획서·구현 순서 문서는 변경하지 않았다.
- 구현 내용: 생성 대상은 TMI 프로젝트의 `작업`, 제목 `[Identity] 탈퇴 Identity release 및 CLEANED 재가입 gate 구축`, 우선순위 `High`, 담당자 없음·라벨 없음으로 준비했다. 현재 Stage 3 계획의 FirebaseIdentity·SocialIdentity exact hard delete, PhoneIdentity·alias RELEASED와 lifecycle CLEANED 단일 Mongo Transaction을 범위로 삼는다.
- 구현 내용: 완료 조건에는 `IDENTITY_RELEASE_PENDING`·externalDeletedAt·WITHDRAWN·exact owner 검증, 전체 rollback, 동시 worker 멱등 수렴, mixed state reconciliation, CLEANED 전 `WITHDRAWAL_CLEANUP_PENDING`, CLEANED 뒤 새 UUID 재가입, 기본 비활성 scheduler와 staging 검증을 포함한다.
- 구현 내용: 탈퇴 Session 전용 오류·모바일 UX, `UserWithdrawn` downstream deny, 가입 중단 Firebase User cleanup, Billing consumer, logout-all revoke, Provider unlink와 Learning Core 데이터 처리는 제외 범위로 분리한다.
- 실행한 테스트와 결과: Jira 생성안 준비와 연결 확인만 수행해 Gradle 테스트는 실행하지 않았다. 공식 Atlassian Rovo search는 403으로 실패했고 Jira mutation은 0건이다. 문서 변경에 대해 `git diff --check`와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Jira에 Token·credential·개인정보와 Secret을 기록하지 않는다. User tombstone·새 UUID 재가입, Identity와 Learning Core·Billing 도메인 경계, Git commit·push 금지 규칙을 유지한다.
- 결정사항: 사용자에게 생성 payload를 먼저 보여 승인받은 뒤에만 Jira 생성 도구를 호출한다. 승인안은 현재 계획의 FirebaseIdentity·SocialIdentity hard delete를 명시하므로 사용자의 승인이 해당 Stage 3 처리 정책 확정도 겸한다.
- 위험 요소: Atlassian Rovo 연결이 복구되지 않으면 승인 후에도 실제 이슈를 생성할 수 없다. hard delete 대신 soft release를 원하면 Jira 생성 전에 본문·계획서를 함께 변경해야 한다.
- 다음 작업: 사용자가 생성안을 승인하면 공식 Atlassian 연결로 cloud와 project metadata를 재확인한 뒤 이슈를 생성하고, 생성 결과를 조회해 key·상태·필드를 검증한다.

## 2026-08-26 — 종료 훅 Stage 3 Jira 생성안 기록 동기화

<!-- codex-turn:01a03bd3-42b3-7f12-b1fd-41802f27e51f -->

- 날짜: 2026-08-26
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 실제 생성·수정·댓글·상태 전환 없음. 공식 Atlassian Rovo 조회는 `app is not installed on this instance` 403으로 실패했다.
- 작업 목표: 종료 훅이 요구한 현재 turn marker로 Stage 3 Jira 생성안 준비와 승인 대기 상태를 WORKLOG EOF에 동기화한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드와 계약 문서는 변경하지 않았다.
- 구현 내용: TMI 프로젝트 `작업`, High, 제목 `[Identity] 탈퇴 Identity release 및 CLEANED 재가입 gate 구축`, 담당자·라벨 없음의 생성안을 사용자에게 제시했다. FirebaseIdentity·SocialIdentity hard delete, Phone release, 단일 Transaction CLEANED, gate·rollback·reconciliation·staging 조건과 제외 범위를 포함했다.
- 실행한 테스트와 결과: Jira mutation 없는 문서 동기화라 Gradle 테스트는 실행하지 않았다. `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: 실제 Jira 생성 전 사용자 승인을 받고 Secret·Token·credential·개인정보를 Jira나 작업 기록에 포함하지 않는다. Git commit·push는 수행하지 않는다.
- 결정사항: Jira 생성안 승인을 기다리며 승인 전에는 Jira를 변경하지 않는다. 현재 제안은 FirebaseIdentity·SocialIdentity hard delete 정책을 사용한다.
- 위험 요소: Atlassian Rovo 연결이 복구되지 않으면 승인 후에도 Jira 생성이 실패할 수 있다. soft release를 선택하면 생성 전에 계획과 본문을 수정해야 한다.
- 다음 작업: 사용자가 생성안을 승인하면 공식 Atlassian 연결로 Jira 생성을 재시도하고 결과를 검증한다.

## 2026-08-26 — Jira TMI-107 Stage 3 생성

- 날짜: 2026-08-26
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: `TMI-107`
- Jira 작업: 사용자가 사전 승인한 Stage 3 생성안으로 신규 이슈를 생성했다. 이슈 유형 `작업`, 우선순위 `High`, 초기 상태 `해야 할 일`, 담당자 없음, 라벨 없음이다. 댓글·상태 전환·다른 이슈 변경은 수행하지 않았다.
- 승인 여부: 제목·범위·완료 조건·제외 범위와 FirebaseIdentity·SocialIdentity hard delete 정책을 사용자에게 먼저 제시했고 사용자가 “어 생성해줘”로 승인했다.
- 작업 목표: Stage 2의 `IDENTITY_RELEASE_PENDING`을 이어받아 내부 identity 점유를 원자적으로 해제하고 lifecycle을 `CLEANED`로 완료하는 Stage 3 구현을 Jira 작업으로 등록한다.
- 변경 파일: `docs/contracts/firebase-withdrawal-identity-release-stage-3-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: Jira 본문에 externalDeletedAt·WITHDRAWN·exact owner preflight, FirebaseIdentity·SocialIdentity hard delete, PhoneIdentity·alias RELEASED, eligibility REVOKED 보장과 CLEANED 단일 Mongo Transaction을 포함했다. rollback, concurrent worker 멱등 수렴, mixed state reconciliation과 중앙 withdrawal gate를 완료 조건으로 등록했다.
- 구현 내용: CLEANED 전 같은 SNS·phone은 `WITHDRAWAL_CLEANUP_PENDING`, CLEANED 뒤 fresh proof·phone 재검증·필수 동의와 새 UUID User 재가입으로 고정했다. 기존 User tombstone·과거 데이터·TrialClaim은 복구·삭제하지 않는다.
- 구현 내용: 탈퇴 Session 전용 오류·모바일 UX, `UserWithdrawn` downstream deny, 가입 중단 Firebase User cleanup, Billing consumer, logout-all revoke, Refresh response recovery, Provider unlink와 Learning Core 데이터 처리는 제외 범위로 등록했다.
- 실행한 테스트와 결과: Jira 생성과 재조회, 문서 키 동기화 작업이라 Gradle 테스트는 실행하지 않았다. 재조회에서 key `TMI-107`, 제목, 유형, High, 해야 할 일, 담당자 없음, 빈 라벨과 승인 본문을 확인했다. `git diff --check`와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Jira에 Token·credential·개인정보와 Secret을 기록하지 않았다. Identity와 Learning Core·Billing 경계, 기존 User WITHDRAWN tombstone, 새 UUID 재가입과 Git commit·push 금지 규칙을 유지했다.
- 결정사항: Stage 3의 현재 구현 정책은 FirebaseIdentity·SocialIdentity exact hard delete와 PhoneIdentity·alias soft release다. Jira는 구현 전 상태로 유지하고 PR 병합 전 완료로 전환하지 않는다.
- 위험 요소: 실제 Mongo replica set의 Transaction rollback·write conflict, scheduler 동시성, staging Firebase 삭제 handoff와 재가입 E2E가 필요하다. 새 Stage 4·5 탈퇴 UX·downstream deny가 완료되기 전 withdrawal production flag를 활성화하지 않는다.
- 다음 작업: 사용자가 `TMI-107` 브랜치를 준비하면 Jira 본문과 저장소 계획서를 기준으로 Transaction·Repository·central gate contract test부터 구현한다.

## 2026-08-26 — 종료 훅 Jira TMI-107 생성 기록 동기화

<!-- codex-turn:01a03bd5-0423-7b83-9601-eacba856b1b0 -->

- 날짜: 2026-08-26
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: `TMI-107`
- Jira 작업: 사용자 승인에 따라 신규 이슈를 생성하고 재조회했다. `작업`, `High`, `해야 할 일`, 담당자 없음, 라벨 없음이며 댓글·상태 전환은 수행하지 않았다.
- 승인 여부: 생성 payload를 사전에 제시했고 사용자가 명시적으로 승인했다.
- 작업 목표: 종료 훅이 요구한 현재 turn marker로 Stage 3 Jira 생성 결과와 저장소 문서 동기화를 WORKLOG EOF에 기록한다.
- 변경 파일: `docs/contracts/firebase-withdrawal-identity-release-stage-3-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: 승인된 목표·범위·완료 조건·제외 범위가 Jira에 저장됐음을 재조회했고 Stage 3 계획서에 `TMI-107` 키를 연결했다.
- 실행한 테스트와 결과: Jira·문서 작업이라 Gradle 테스트는 실행하지 않았다. Jira 필드 재조회, `git diff --check`, 지정 marker 단일 존재와 WORKLOG EOF append를 검증한다.
- 유지한 계약: Jira와 작업 기록에 Token·credential·개인정보와 Secret을 기록하지 않았다. Git commit·push와 PR 병합 전 Jira 완료 전환을 수행하지 않는다.
- 결정사항: `TMI-107`은 구현 전 `해야 할 일` 상태를 유지한다. 현재 Stage 3 정책은 FirebaseIdentity·SocialIdentity hard delete와 PhoneIdentity·alias soft release다.
- 위험 요소: 실제 Mongo Transaction·동시성·Firebase handoff·재가입 staging E2E가 필요하고 Stage 4·5 완료 전 withdrawal production 활성화는 금지한다.
- 다음 작업: 사용자가 Jira 브랜치를 준비하면 계획서와 Jira 본문을 기준으로 Stage 3 구현을 시작한다.

## 2026-08-26 — Jira TMI-107 Stage 3 identity release와 CLEANED 재가입 gate 구현

<!-- codex-turn:01a03bd7-b573-7cd3-8b2b-c07b20115605 -->

- 날짜: 2026-08-26
- 브랜치: `feat/TMI-107-withdrawal-identity-release` (Codex commit·push 미수행)
- Jira: `TMI-107`. 구현 기준으로 조회된 이슈는 `해야 할 일`·High이며, 이번 turn에는 댓글·상태·담당자·설명 등 Jira mutation을 수행하지 않았다. PR 병합 전 완료 전환 금지를 유지한다.
- 작업 목표: Stage 2가 `IDENTITY_RELEASE_PENDING`으로 넘긴 탈퇴 lifecycle에서 내부 Firebase·SNS·전화 identity 점유를 원자적으로 해제하고, 모든 불변식이 성립한 뒤에만 `CLEANED`로 전환해 새 UUID 재가입을 허용한다.
- 변경 파일: `src/main/java/.../federation/application/{WithdrawalEnrollmentGate,FirebaseExchangeService,FirebaseSignupService,FirebaseIdentityOwnershipService,FirebaseGuestMergeTargetResolver,FirebaseGuestUpgradeService,FirebaseAuthMethodsSyncService}.java`, `src/main/java/.../federation/infrastructure/firebase/FirebaseAuthenticationConfiguration.java`, `src/main/java/.../phoneidentity/repository/PhoneFingerprintAliasRepository.java`, `src/main/java/.../user/application/{IdentityReleaseOutcome,UserWithdrawalIdentityReleaseTransactionService,UserWithdrawalIdentityReleaseWorker}.java`, `src/main/java/.../user/infrastructure/{UserWithdrawalIdentityReleaseConfiguration,UserWithdrawalIdentityReleaseProperties,UserWithdrawalIdentityReleaseScheduler}.java`, `src/main/java/.../user/domain/enums/WithdrawalCleanupFailureCode.java`, `src/main/java/.../user/domain/repository/{UserWithdrawalLifecycleRepository,UserWithdrawalLifecycleRepositoryCustom,UserWithdrawalLifecycleRepositoryImpl}.java`, `src/main/resources/application.yml`, 대응 application·repository·configuration·scheduler 테스트와 `src/test/resources/application-test.yml`, `docs/contracts/firebase-withdrawal-identity-release-stage-3-plan.md`, `docs/contracts/firebase-auth-follow-up-implementation-order.md`, `docs/codex/{CURRENT_STATE,WORKLOG}.md`.
- 구현 내용: candidate를 고르는 worker와 실제 변경 Transaction을 분리했다. Transaction은 lifecycle id·status·version·externalDeletedAt·lease 부재, WITHDRAWN User, lifecycle target과 FirebaseIdentity의 exact owner, PhoneIdentity·ACTIVE alias 정합성을 mutation 전에 검증한다. 이미 전부 release된 상태는 멱등 `CLEANED`로 수렴시키고 일부만 사라진 상태, target·owner 불일치, orphan alias와 안전하게 해석할 수 없는 상태는 새 failure code와 함께 `RECONCILIATION_REQUIRED`로 보낸다.
- 구현 내용: 검증을 통과하면 active eligibility binding을 REVOKED revision으로 전진시키고 outbox를 생성한 뒤 exact FirebaseIdentity와 해당 User의 모든 SocialIdentity를 hard delete한다. PhoneIdentity와 모든 ACTIVE alias는 RELEASED로 전환하고 lifecycle의 `identitiesReleasedAt`·`cleanedAt`과 status `CLEANED`를 version CAS로 기록한다. 마지막 CAS나 중간 concurrent mutation을 잃으면 `OptimisticLockingFailureException`으로 전체 Mongo Transaction을 rollback한다. Firebase·HTTP 호출은 Transaction 안에 추가하지 않았다.
- 구현 내용: 기본 비활성 `app.firebase-withdrawal-identity-release` worker·scheduler·batch 설정과 낮은 cardinality outcome metric·식별자 없는 구조화 로그를 추가했다. 중앙 `WithdrawalEnrollmentGate`는 WITHDRAWN owner가 CLEANED 전이면 `WITHDRAWAL_CLEANUP_PENDING`, CLEANED인데 mapping이 남으면 fail-closed conflict를 반환하며 Firebase login exchange, direct signup owner precheck와 unique conflict 재분류, Guest prepare·upgrade 소유권 판정, Guest merge target, auth methods sync와 Guest upgrade phone owner conflict에 연결했다.
- 실행한 테스트와 결과: 최신 경로별 focused test와 Stage 3 Transaction·worker·repository·gate·configuration·scheduler 테스트가 성공했다. 최종 `./gradlew clean test`는 103개 suite·572개 테스트가 failure·error·skip 0건으로 성공했다. `git diff --check`도 통과했다.
- 유지한 계약: User는 `WITHDRAWN` tombstone으로 남고 재가입은 fresh Firebase proof·전화번호 재검증·필수 동의와 새 UUID User를 사용한다. 외부 Firebase User 삭제 확인 전 mapping을 해제하지 않고 다른 User identity를 추정 삭제하지 않는다. Identity에는 Learning Core 시험·결과 또는 Billing TrialClaim 코드를 추가하지 않았고 Token·Firebase UID·provider subject·raw phone·fingerprint·Secret을 로그·metric·문서에 기록하지 않았다. 기존 공개 API·JWT·RefreshSession·JWKS 계약과 기본 비활성 production flag를 유지했다.
- 결정사항: FirebaseIdentity·SocialIdentity는 exact hard delete, PhoneIdentity·alias는 soft release 정책을 구현했다. active eligibility outbox의 전달 완료는 `CLEANED` 조건이 아니지만 REVOKED revision과 outbox 생성 자체는 같은 Transaction의 필수 조건이다. 로컬·Guest lifecycle의 null Firebase target은 Firebase mapping이 없을 때만 정상 release하며 예상 밖 mapping은 reconciliation으로 격리한다.
- 위험 요소: 로컬 테스트는 Transaction annotation·Repository CAS·application 경계를 검증하지만 실제 replica set의 rollback·write conflict와 Stage 2 handoff부터 동일 credential 재가입까지의 E2E는 staging에서 확인해야 한다. worker flag는 기본 false이며 Stage 4 탈퇴 Session UX와 Stage 5 downstream deny marker가 완료되기 전 withdrawal production 활성화도 금지한다.
- 다음 작업: 사용자가 변경을 검토해 직접 commit·push하고 PR을 병합한다. 이후 staging Mongo Transaction·동시성·재가입 E2E를 수행하고, 병합 확인과 별도 승인 뒤 Jira 댓글·완료 전환을 진행한다. 다음 고정 구현 단계는 Stage 4 탈퇴 Session 전용 오류와 모바일 logout·안내 UX 계약이다.

## 2026-08-26 — Billing 무료시험 workload credential 현황 점검

<!-- codex-turn:01a037c4-43ac-7252-9317-ae89a1323e88-identity-workload-review -->

- 날짜: 2026-08-26
- 브랜치: `feat/TMI-107-withdrawal-identity-release` (Codex commit·push 미수행)
- Jira: 없음. 기존 `TMI-107` 코드·문서·Jira 상태는 변경하지 않았다.
- 작업 목표: Billing C3-A 설명을 위해 Identity가 Learning Core용 workload JWT를 현재 직접 발급할 수 있는지와 기존 phone eligibility transport 인증 계약을 읽기 전용 확인한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·ADR은 변경하지 않았다.
- 구현 내용: `JwtAccessTokenIssuer`는 canonical UUID 사용자를 위한 Access Token issuer이고 Learning Core client-credentials issuer가 아님을 확인했다. ADR-002는 private HTTPS push에 배포 플랫폼 발급 5분 이하 workload identity JWT를 요구하며 publisher는 `WorkloadIdentityCredentialProvider` port를 사용하지만 production provider 구현은 아직 없다.
- 실행한 테스트와 결과: 읽기 전용 분석과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았다. 문서 whitespace와 marker 단일 존재를 종료 전에 검증한다.
- 유지한 계약: 사용자 token과 workload credential 분리, RS256 사용자 JWT, raw phone·candidate·Token·Secret 비기록, Identity와 Billing/Learning Core 도메인 경계를 유지했다.
- 결정사항: Billing용 Learning Core credential의 발급 주체는 이번 점검에서 확정하지 않았다. Identity 자체 client-credentials 방식과 기존 ADR-002에 맞춘 배포 플랫폼 workload identity 방식은 별도 선택지다.
- 위험 요소: Identity 직접 발급을 선택하면 현재 없는 client 등록, 인증, token endpoint와 key/secret rotation을 추가해야 한다. 플랫폼 방식을 선택해도 배포 환경 issuer·JWKS·audience·subject 및 로컬/staging 공급 adapter가 필요하다.
- 다음 작업: Billing 계약에서 workload 발급 주체를 확정한 뒤 Identity에 필요한 일이 실제 issuer 구현인지 배포 credential provider 연결인지 분리한다.

## 2026-08-26 — Billing workload identity 플랫폼 발급안 승인 기록

<!-- codex-turn:01a037c4-43ac-7252-9317-ae89a1323e88-platform-workload-approved -->

- 날짜: 2026-08-26
- 브랜치: `feat/TMI-107-withdrawal-identity-release` (Codex commit·push 미수행)
- Jira: 없음. 기존 `TMI-107` 코드·문서·Jira 상태는 변경하지 않았다.
- 작업 목표: 사용자가 Billing 무료시험 계약의 배포 플랫폼 발급 workload identity JWT 권장안을 승인한 사실과 Identity 책임 변화를 기록한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·ADR은 변경하지 않았다.
- 구현 내용: Learning Core → Billing은 배포 플랫폼 발급 5분 이하 service identity JWT를 사용하므로 Identity에 별도 client-credentials issuer를 추가하지 않는 것으로 정리했다. 이는 phone eligibility push에 이미 채택된 ADR-002 방향과 일치한다.
- 실행한 테스트와 결과: 계약 기록만 변경해 Gradle 테스트는 실행하지 않았다. 문서 whitespace와 marker 단일 존재를 종료 전에 검증한다.
- 유지한 계약: 사용자 Access Token과 workload credential 분리, 사용자 JWT `sub` UUID, raw phone·candidate·Token·Secret 비기록과 Identity/Billing/Learning Core 도메인 경계를 유지했다.
- 결정사항: workload credential 발급 주체는 배포 플랫폼이다. Identity의 사용자 JWT issuer를 서버 credential 발급에 재사용하지 않는다.
- 위험 요소: 실제 플랫폼 issuer·JWKS·service subject와 로컬/staging credential provider는 아직 환경별로 확정·검증해야 한다.
- 다음 작업: Billing과 Learning Core가 workload trust 설정과 권한 mapping을 구현하고 staging에서 실제 credential 검증을 수행한다.

## 2026-08-26 — AWS ECS 확인에 따른 workload credential 후속 분석

<!-- codex-turn:01a037c4-43ac-7252-9317-ae89a1323e88-identity-ecs-workload-review -->

- 날짜: 2026-08-26
- 브랜치: `feat/TMI-107-withdrawal-identity-release` (Codex commit·push 미수행)
- Jira: 없음. 기존 `TMI-107` 코드·문서·Jira 상태는 변경하지 않았다.
- 작업 목표: 실제 배포 환경이 AWS ECS라는 후속 정보를 Identity의 기존 workload credential port와 ADR-002 transport 가정에 대조한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·ADR은 변경하지 않았다.
- 구현 내용: ECS task role은 자동 회전 AWS credential이지 일반 OIDC workload JWT가 아니므로 기존 `WorkloadIdentityCredentialProvider`의 실제 adapter는 VPC Lattice/API Gateway SigV4 경로 또는 별도 issuer 결정 없이 구현할 수 없음을 기록했다.
- 실행한 테스트와 결과: 읽기 전용 인프라 검색과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았다. 문서 whitespace와 marker 단일 존재를 종료 전에 검증한다.
- 유지한 계약: 사용자 Access Token과 workload credential 분리, raw phone·candidate·Token·Secret 비기록, Identity/Billing/Learning Core 도메인 경계를 유지했다.
- 결정사항: ECS라는 사실만으로 Identity가 workload JWT issuer를 새로 소유하지 않는다. Billing C3와 ADR-002 publisher credential adapter는 실제 service ingress 확인 뒤 구체화한다.
- 위험 요소: 내부 ALB/Service Connect 직접 경로는 task role principal을 애플리케이션 요청 인증으로 자동 전달하지 않는다. network private만으로 producer를 신뢰하면 ADR-002 보안 계약을 충족하지 못한다.
- 다음 작업: 실제 ECS service-to-service ingress를 확인하고 SigV4/IAM edge 또는 별도 workload issuer 중 하나를 승인한다.

## 2026-08-26 — 기존 Identity·Learning Core 인증 구현 대조

<!-- codex-turn:01a037c4-43ac-7252-9317-ae89a1323e88-identity-existing-auth-review -->

- 날짜: 2026-08-26
- 브랜치: `feat/TMI-107-withdrawal-identity-release` (Codex commit·push 미수행)
- Jira: 없음. 기존 `TMI-107` 코드·문서·Jira 상태는 변경하지 않았다.
- 작업 목표: Billing workload 인증을 기존 방식과 맞추기 위해 Identity JWT issuer와 downstream publisher의 실제 구현 완료 여부를 대조한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·ADR은 변경하지 않았다.
- 구현 내용: Identity는 RS256 사용자 Access Token과 JWKS를 구현했고 Learning Core가 이를 로컬 검증한다. downstream publisher는 `WorkloadIdentityCredentialProvider` port와 Bearer adapter만 있으며 production provider·client-credentials issuer가 없고 기본 비활성임을 확인했다. 동일 메커니즘을 workload에 적용하려면 별도 token profile과 client 인증을 추가해야 한다.
- 실행한 테스트와 결과: 읽기 전용 분석과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았다. 문서 whitespace와 marker 단일 존재를 종료 전에 검증한다.
- 유지한 계약: 사용자 JWT `sub` UUID, Identity private key 단독 소유, JWKS public verification, 사용자/workload token 분리와 Secret·Token 비기록을 유지했다.
- 결정사항: 사용자 Access Token을 workload credential로 재사용하지 않는다. Identity-issued workload JWT는 현재 구현이 아니라 기존 RS256/JWKS 메커니즘을 확장하는 신규 권장안이다.
- 위험 요소: workload client secret 저장·rotation, audience confusion 방지, user/workload token type 검증과 token endpoint abuse 방어가 필요하다.
- 다음 작업: 사용자가 Billing C3-E를 승인하면 별도 Jira와 계약으로 Identity workload issuer 구현을 분리한다.

## 2026-08-26 — Billing C3-D Lattice/SigV4 승인 반영

<!-- codex-turn:01a037c4-43ac-7252-9317-ae89a1323e88-identity-c3d-approved -->

- 날짜: 2026-08-26
- 브랜치: `feat/TMI-107-withdrawal-identity-release` (Codex commit·push 미수행)
- Jira: 없음. 기존 `TMI-107` 코드·문서·Jira 상태는 변경하지 않았다.
- 작업 목표: 사용자가 최종 승인한 Billing C3-D가 Identity phone eligibility publisher에 미치는 인증 경계를 기록한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·ADR은 변경하지 않았다.
- 구현 내용: Identity workload JWT issuer를 추가하지 않고 Identity ECS task role로 Billing Lattice 요청을 SigV4 서명하는 방향을 확정 상태로 기록했다. 기존 Bearer `WorkloadIdentityCredentialProvider`와 delivery adapter는 후속 작업에서 request signer 경계로 변경해야 한다.
- 실행한 테스트와 결과: 계약 기록만 변경해 Gradle 테스트는 실행하지 않았다. 문서 whitespace와 marker 단일 존재를 종료 전에 검증한다.
- 유지한 계약: 사용자 RS256 JWT/JWKS, 사용자/workload 분리, eligibility event at-least-once·멱등성, raw phone·Token·Secret 비기록과 기본 비활성 publisher를 유지했다.
- 결정사항: Identity client-credentials/workload token endpoint는 구현하지 않는다. 기존 사용자 Load Balancer는 유지하고 Billing outbound만 Lattice/SigV4를 사용한다.
- 위험 요소: Identity와 Learning Core task role 분리, SigV4 signer, exact Lattice event route auth policy와 staging 401/403·retry 검증이 남아 있다.
- 다음 작업: Billing event consumer wire 계약과 함께 Identity publisher signer 변경을 별도 Jira로 구현한다.

## 2026-08-26 — Jira TMI-107 완료 전환

<!-- codex-turn:01a03d36-08cd-7fa3-b002-3502573ab7e3 -->

- 날짜: 2026-08-26
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: `TMI-107`
- Jira 작업: PR #32 병합을 확인한 뒤 사용자 요청에 따라 transition ID `41`만 적용했다. 후속 조회에서 status ID `10003`과 Resolution ID `10000`이 모두 `완료`임을 확인했다. 댓글·담당자·우선순위·라벨·설명과 다른 이슈는 변경하지 않았다.
- 승인 여부: 사용자가 “지라 닫아줘”라고 명시적으로 요청했다. 적용 전 현재 `해야 할 일` 상태, Resolution 없음, PR #32 병합과 완료 transition ID `41`을 확인하고 변경 범위를 사용자에게 알렸다.
- 작업 목표: 병합이 완료된 Stage 3 identity release와 `CLEANED` 재가입 gate 작업의 Jira lifecycle을 저장소·PR 상태와 일치시킨다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 구현 내용: 로컬 `develop`과 `origin/develop`이 PR #32 merge commit `1fa1141`을 가리키고, 구현 commit `1832e27`이 병합 이력에 포함된 것을 확인했다. Jira에는 상태 전환 외 mutation을 수행하지 않았다.
- 실행한 테스트와 결과: Jira 상태 전환과 문서 동기화 작업이라 Gradle 테스트는 실행하지 않았다. TMI-107 구현 시 통과한 전체 103개 suite·572개 테스트 결과를 유지하며, 문서 변경은 `git diff --check`와 marker 단일 존재로 검증한다.
- 유지한 계약: PR 병합 확인 전 Jira 완료 전환 금지, Jira mutation 전 사용자 승인, Git commit·push 금지 규칙을 유지했다. Token·Firebase UID·provider subject·raw phone·fingerprint·Secret과 개인정보를 Jira나 작업 기록에 포함하지 않았다.
- 결정사항: `TMI-107`은 상태·Resolution 모두 `완료`다. Jira 종료 댓글은 사용자가 요청하지 않아 등록하지 않았고 production worker flag는 staging 검증 전 false를 유지한다.
- 위험 요소: 실제 Firebase·Mongo staging에서 Stage 2 handoff→Stage 3 Transaction rollback·동시 worker·동일 credential 새 UUID 재가입 E2E와 Apple authorization revoke material 경로 검증은 계속 필요하다. Stage 4·5 완료 전 withdrawal production 활성화 금지도 유지한다.
- 다음 작업: 고정 순서 Stage 4 `ACCOUNT_WITHDRAWN` RefreshSession 전용 오류와 모바일 Firebase signOut·자체 Token 삭제·탈퇴 안내 UX 계약을 계획하고, Billing C3-D Lattice/SigV4 publisher 변경은 별도 Jira로 분리한다.

## 2026-08-26 — TMI-107 후속 Stage 4 작업 설명

<!-- codex-turn:01a03d38-4be1-72f2-bb7e-d916c0fb68ca -->

- 날짜: 2026-08-26
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 신규 이슈 생성·수정·댓글·상태 전환 없음. `TMI-107`은 완료 상태를 유지한다.
- 작업 목표: 다음 고정 구현 순서인 탈퇴 Session 전용 오류와 모바일 logout·안내 UX 계약이 현재 동작에서 무엇을 바꾸는지 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 구현 내용: 현재 `TokenReissueService`는 `ROTATED` Session만 `REFRESH_TOKEN_REUSE_DETECTED`로 구분하고, `ACCOUNT_WITHDRAWN`을 포함한 나머지 revoked Session은 모두 `INVALID_REFRESH_TOKEN` 401로 반환한다. Stage 4는 DB에서 실제 Session을 찾았고 revocation reason이 `ACCOUNT_WITHDRAWN`인 경우에만 탈퇴 전용 401 오류로 분류한다. 존재하지 않는 Token과 LOGOUT·LOGOUT_ALL·GUEST 전환 등 다른 폐기 사유는 기존 오류를 유지한다.
- 구현 내용: User가 `WITHDRAWN`인데 요청이 탈퇴 Transaction과 교차해 Session의 이전 snapshot을 읽은 경계에서도 Token을 발급하지 않고 같은 탈퇴 오류로 수렴하는 race-safe fallback을 검토한다. 모바일은 탈퇴 API 성공 또는 전용 재발급 오류 수신 시 Firebase client signOut, Identity Access/Refresh Token과 로컬 계정 상태 전체 삭제, “탈퇴 처리된 계정입니다” 안내와 로그인 화면 이동을 수행한다.
- 실행한 테스트와 결과: 코드 변경 없는 설명 작업이라 Gradle 테스트는 실행하지 않았다. 구현 순서 문서, `TokenReissueService`, `AuthErrorStatus`, `RevocationReason`과 withdrawal lifecycle 테스트의 현재 `INVALID_REFRESH_TOKEN` 기대값을 읽어 확인했다. 문서 변경은 `git diff --check`로 검증한다.
- 유지한 계약: Refresh Token 원문을 저장·로그하지 않고, 실제 DB Session을 찾지 못한 요청에는 탈퇴 여부를 노출하지 않는다. 기존 RS256 Access Token·RefreshSession·BaseResponse 계약을 유지하며 모바일 signOut은 Firebase Admin delete나 서버 logout-all을 대신하지 않는다. Identity가 매 downstream 요청을 introspection하지 않는다.
- 결정사항: Stage 4는 Refresh 재발급과 모바일 UX 계약이며 이미 발급된 Access Token을 Learning Core에서 즉시 막는 작업은 아니다. 해당 공백은 다음 Stage 5 `UserWithdrawn` event와 downstream deny marker에서 처리한다.
- 위험 요소: 서버 전용 오류를 모바일 처리보다 먼저 배포하면 사용자는 새로운 code를 일반 오류로 볼 수 있다. 탈퇴 Transaction과 재발급 요청의 교차 시점, 여러 기기, 앱 재시작, Firebase signOut 실패와 로컬 Token 삭제 순서를 contract test·모바일 E2E로 검증해야 한다.
- 다음 작업: Stage 4 저장소 구현 계획서에 외부 오류 code·HTTP status, 서버 분기와 race fallback, 모바일 처리 순서, rollout 호환성과 테스트 매트릭스를 확정한 뒤 Jira 생성안을 사용자 승인 후 등록한다.

## 2026-08-26 — Stage 4 탈퇴 Session·모바일 UX 구현 계획서 작성

<!-- codex-turn:01a03d3b-d6db-7903-a204-b69b53191e1c -->

- 날짜: 2026-08-26
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 신규 이슈 생성·수정·댓글·상태 전환 없음. `TMI-107`은 완료 상태를 유지한다.
- 작업 목표: 다음 고정 순서인 탈퇴 Session 전용 오류와 모바일 logout·안내 UX의 서버·모바일 계약, 동시성·보안·배포·테스트 기준을 저장소 구현 계획서로 확정한다.
- 변경 파일: `docs/contracts/withdrawal-session-mobile-ux-stage-4-plan.md`, `docs/contracts/firebase-auth-follow-up-implementation-order.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: 신규 외부 오류를 `401 ACCOUNT_WITHDRAWN`, message `탈퇴 처리된 계정입니다.`, 기존 `BaseResponse` shape로 고정했다. 실제 hash 일치 Session의 reason이 `ACCOUNT_WITHDRAWN`일 때만 전용 오류를 반환하고 unknown Token은 generic invalid를 유지한다. `ROTATED` reuse detection을 최우선으로 보존하며 그 밖의 revoked·expired·User 없음·SUSPENDED 상태 매트릭스를 명시했다.
- 구현 내용: active Session과 WITHDRAWN User가 교차 관찰되는 race-safe fallback, 탈퇴 성공과 새 Session 발급의 동시 확정 금지, multi-device 재발급을 계획했다. 모바일은 탈퇴 API 2xx와 전용 오류를 같은 멱등 terminal handler로 처리하고 Firebase signOut 실패와 무관하게 Identity Access/Refresh Token·local user cache를 삭제하며 동시 401·앱 재시작에서 안내와 navigation을 한 번만 수행한다.
- 구현 내용: 모바일 선배포, 구버전 미지원 code 처리 확인, 서버 배포, staging multi-device·signOut 실패 E2E, Stage 5 consumer 선배포 뒤 전체 withdrawal 활성화 순서를 정의했다. Stage 4에는 UserWithdrawn event, downstream deny marker, logout-all Firebase revoke와 rotation 원자성 개선을 포함하지 않았다.
- 실행한 테스트와 결과: 문서 작업이라 Gradle 테스트는 실행하지 않았다. 현재 `TokenReissueService`, `AuthErrorStatus`, `RevocationReason`, `BaseResponse`, Controller OpenAPI와 reissue·withdrawal 테스트를 읽어 계획을 대조했다. 문서 변경은 `git diff --check`, 링크 존재와 지정 marker 단일 존재로 검증한다.
- 유지한 계약: Refresh Token 원문은 저장·로그하지 않고 unknown Token으로 탈퇴 여부를 조회할 수 없게 한다. 사용자 JWT·JWKS, RefreshSession rotation, 기존 API Request·성공 응답과 Identity/Learning Core·모바일 책임 경계를 유지하며 Token·credential·개인정보와 Secret을 문서에 기록하지 않았다.
- 결정사항: Stage 4는 Refresh 재발급 오류와 모바일 로컬 인증 상태 정리다. 이미 발급된 Access Token의 downstream 차단은 Stage 5로 유지하고, 모바일 구현체는 별도 저장소에서 작업하며 Identity에는 계약만 둔다.
- 위험 요소: 서버 code가 모바일 지원보다 먼저 활성화되면 구버전 앱의 안내·정리 동작이 달라질 수 있다. 실제 replica set의 withdrawal/reissue 경쟁, 여러 기기, Firebase signOut 실패, 앱 종료·재시작과 Access Token 만료 전 공백은 staging에서 검증해야 한다.
- 다음 작업: 계획서를 기준으로 Stage 4 Jira 생성안을 사용자에게 먼저 제시하고 승인 후 생성한다. 이후 Jira 브랜치에서 서버 오류 분기·OpenAPI·테스트를 구현하고 모바일 팀에 contract handoff를 전달한다.

## 2026-08-26 — Jira TMI-108 Stage 4 생성

<!-- codex-turn:01a03d47-9838-7ca2-a13f-22f33b3a231c -->

- 날짜: 2026-08-26
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: `TMI-108`
- Jira 작업: 중복 검색 결과가 없음을 확인한 뒤 `[Identity] 탈퇴 Session 전용 오류 및 모바일 logout·안내 UX 계약`을 TMI 프로젝트 `작업`, High 우선순위로 생성했다. 재조회 결과 상태 `해야 할 일`, Resolution 없음, 담당자·라벨·댓글 없음을 확인했으며 별도 상태 전환과 다른 이슈 변경은 수행하지 않았다.
- 승인 여부: 계획서 완료 뒤 사용자가 “좋아 지라 생성해줘”라고 명시적으로 승인했다. 생성 전 프로젝트·유형·우선순위·제목·포함·제외 범위를 다시 제시하고 해당 승인 범위로 생성했다.
- 작업 목표: Stage 4 서버·모바일 계약과 완료 조건을 추적할 Jira를 생성하고 저장소 구현 계획서와 연결한다.
- 변경 파일: `docs/contracts/withdrawal-session-mobile-ux-stage-4-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 직전 계획 작업의 `docs/contracts/firebase-auth-follow-up-implementation-order.md` 변경을 유지했으며 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: Jira 본문에 `401 ACCOUNT_WITHDRAWN`, 실제 hash 일치 탈퇴 Session 한정 노출, unknown Token generic invalid, ROTATED reuse detection 보존, active Session·WITHDRAWN User race fallback, OpenAPI·서버 테스트와 모바일 멱등 terminal handler를 포함했다. 모바일 선배포와 staging multi-device E2E, Stage 5 전 production 활성화 금지도 완료 조건으로 기록했다.
- 실행한 테스트와 결과: Jira 생성·재조회와 문서 키 동기화 작업이라 Gradle 테스트는 실행하지 않았다. Jira key·제목·유형·High·상태·Resolution·담당자·라벨·댓글을 재조회했고 문서 변경은 `git diff --check`, 계획서 Jira 키와 지정 marker 단일 존재로 검증한다.
- 유지한 계약: Jira에 Token·hash·sessionId·Firebase UID·사용자 식별 정보·개인정보와 Secret을 기록하지 않았다. 기존 RefreshSession·JWT·JWKS·BaseResponse 계약, Identity와 모바일·Learning Core 책임 경계, Git commit·push 금지와 PR 병합 전 Jira 완료 전환 금지를 유지했다.
- 결정사항: `TMI-108`은 서버 재발급 오류·OpenAPI·테스트와 모바일 contract handoff를 하나의 Stage 4 작업으로 추적한다. 모바일 구현체와 Stage 5 downstream deny marker는 Identity 코드 범위에 포함하지 않는다.
- 위험 요소: 모바일 저장소가 별도이므로 서버 구현만으로 Jira 완료 조건을 충족하지 않는다. 구버전 앱 호환성, 실제 Mongo withdrawal/reissue 경쟁, 여러 기기, Firebase signOut 실패와 앱 재시작 E2E가 필요하다.
- 다음 작업: 사용자가 `TMI-108` 브랜치를 준비하면 Jira와 계획서를 읽고 서버 error 분기·OpenAPI·테스트를 구현한다. 모바일 handoff를 별도 산출물로 전달하고 양쪽 staging 검증 전 production withdrawal flag는 유지한다.

## 2026-08-27 — Jira TMI-108 탈퇴 Session 전용 오류 구현

<!-- codex-turn:01a04183-55e3-74e0-b1ba-fdb40d5554c8 -->

- 날짜: 2026-08-27
- 브랜치: `feat/TMI-108-withdrawal-session-mobile-contract` (Codex commit·push 미수행)
- Jira: `TMI-108`. 구현 전에 공식 Atlassian Rovo로 본문·완료 조건·상태 `해야 할 일`을 읽었으며 Jira 댓글·필드·상태는 변경하지 않았다.
- 작업 목표: 회원 탈퇴로 폐기된 RefreshSession을 일반 Refresh Token 오류와 구분하고, 모바일이 탈퇴 terminal logout·안내 흐름을 시작할 수 있도록 안전한 서버 오류와 OpenAPI 계약을 구현한다.
- 변경 파일: `src/main/java/web/tosunsaeng/identity/domain/auth/common/exception/AuthErrorStatus.java`, `src/main/java/web/tosunsaeng/identity/domain/auth/session/application/TokenReissueService.java`, `src/main/java/web/tosunsaeng/identity/domain/auth/common/api/AuthController.java`, 대응 `RefreshTokenUseCaseServicesTests.java`, `AuthControllerTests.java`, `UserWithdrawalLifecycleTests.java`, `SecurityIntegrationTests.java`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 이전 turn의 `docs/contracts/withdrawal-session-mobile-ux-stage-4-plan.md`와 고정 구현 순서 문서 변경은 보존했다.
- 구현 내용: `AuthErrorStatus.ACCOUNT_WITHDRAWN`을 HTTP 401, code `ACCOUNT_WITHDRAWN`, message `탈퇴 처리된 계정입니다.`로 추가했다. 재발급은 `ROTATED` reuse detection을 먼저 수행한 뒤 실제 조회된 Session의 reason이 `ACCOUNT_WITHDRAWN`이면 전용 오류를 반환하며, 그 밖의 revoked Session은 기존 generic invalid를 유지한다. Session은 active snapshot이지만 User가 `WITHDRAWN`인 탈퇴 Transaction 교차 관찰도 같은 전용 오류로 fail-closed 처리한다.
- 구현 내용: `/api/v1/auth/reissue` OpenAPI 401 설명에 invalid·expired·reuse·withdrawn code를 명시했다. application 테스트는 탈퇴 Session에서 User 조회·Session save·Token 발급 0건, WITHDRAWN User fallback, 모든 일반 revocation reason과 ROTATED 우선순위를 검증한다. controller 테스트는 기존 `BaseResponse` shape와 내부 식별자·Token·hash 비노출을 고정하고 withdrawal lifecycle 테스트는 탈퇴된 모든 기기 Refresh Token이 전용 오류로 수렴함을 검증한다. OpenAPI 통합 테스트도 신규 code 노출을 확인한다.
- 실행한 테스트와 결과: focused application·controller·withdrawal lifecycle·OpenAPI 통합 테스트가 성공했다. 최종 `./gradlew clean test`는 103개 suite·578개 테스트가 failure·error·skip 0건으로 성공했다. `git diff --check`도 통과했다.
- 유지한 계약: unknown Token은 계속 `INVALID_REFRESH_TOKEN`이어서 계정 탈퇴 여부를 탐색할 수 없다. `ROTATED` reuse detection과 활성 Session 전체 폐기 side effect, 일반 revoked·expired·User 없음·SUSPENDED 오류, 기존 Request·성공 응답·BaseResponse·RS256 JWT·JWKS 계약을 유지했다. Refresh Token 원문·hash·sessionId·userId와 credential·Secret을 응답·로그·문서에 기록하지 않았고 Identity에 모바일·Learning Core 코드를 추가하지 않았다.
- 결정사항: 모바일 구현은 별도 저장소의 handoff 범위다. 모바일은 탈퇴 API 2xx와 `ACCOUNT_WITHDRAWN`을 같은 멱등 terminal handler로 처리하고 Firebase signOut 실패와 무관하게 Identity Access·Refresh Token과 local user cache를 삭제하며 안내·navigation을 한 번만 수행해야 한다. Stage 4는 이미 발급된 Access Token을 downstream에서 즉시 차단하지 않는다.
- 위험 요소: 실제 Mongo replica set의 withdrawal/reissue 교차 write conflict, 여러 기기, 모바일 Firebase signOut 실패·앱 재시작·구버전 호환성은 staging E2E가 필요하다. Stage 5 `UserWithdrawn` event와 Learning Core deny marker 전에는 기존 Access Token이 만료까지 사용될 수 있으므로 production withdrawal flag를 활성화하지 않는다.
- 다음 작업: 사용자가 변경을 검토해 직접 commit·push하고 PR을 병합한다. 모바일 terminal handler를 별도 저장소에 구현하고 staging multi-device E2E를 수행한다. 병합 확인과 별도 승인 뒤 Jira 댓글·완료 전환을 진행하며 다음 고정 서버 단계는 Stage 5 `UserWithdrawn` event와 downstream deny marker다.

## 2026-08-27 — Jira TMI-108 완료 전환

<!-- codex-turn:01a0418b-0c0e-75f0-a619-eb9e673ee281 -->

- 날짜: 2026-08-27
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: `TMI-108`
- Jira 작업: PR #33 병합을 확인한 뒤 사용자 요청에 따라 transition ID `41`만 적용했다. 후속 조회에서 status ID `10003`과 Resolution ID `10000`이 모두 `완료`임을 확인했다. 댓글·담당자·우선순위·라벨·설명과 다른 이슈는 변경하지 않았다.
- 승인 여부: 사용자가 “머지했어 지라 닫아줘”라고 명시적으로 요청했다. 적용 전 현재 `해야 할 일` 상태, Resolution 없음, PR #33 병합과 완료 transition ID `41`을 확인하고 변경 범위를 사용자에게 알렸다.
- 작업 목표: 병합이 완료된 탈퇴 RefreshSession 전용 오류와 모바일 logout·안내 서버 계약 작업의 Jira lifecycle을 저장소·PR 상태와 일치시킨다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 구현 내용: 로컬 `develop`과 `origin/develop`이 PR #33 merge commit `7fc92c6`을 가리키고, TMI-108 구현 commit `019bcd5`가 병합된 것을 확인했다. Jira에는 완료 상태 전환 외 mutation을 수행하지 않았다.
- 실행한 테스트와 결과: Jira 상태 전환과 문서 동기화 작업이라 Gradle 테스트는 실행하지 않았다. TMI-108 구현 시 통과한 전체 103개 suite·578개 테스트 결과를 유지하며 문서 변경은 `git diff --check`와 marker 단일 존재로 검증한다.
- 유지한 계약: PR 병합 확인 전 Jira 완료 전환 금지, Jira mutation 전 사용자 승인, Git commit·push 금지 규칙을 유지했다. credential·Token·Secret과 사용자 개인정보를 Jira나 작업 기록에 포함하지 않았다.
- 결정사항: `TMI-108`은 상태와 Resolution 모두 `완료`다. Jira 종료 댓글은 사용자가 요청하지 않아 등록하지 않았고 production withdrawal flag는 모바일 호환성과 Stage 5 선행 배포·staging 검증 전까지 비활성으로 유지한다.
- 위험 요소: 서버 병합과 Jira 완료가 모바일 terminal handler, 실제 Mongo withdrawal/reissue 경쟁, 다중 기기, Firebase signOut 실패·앱 재시작 E2E 완료를 대신하지 않는다. Stage 5 전에는 탈퇴 전에 발급된 Access Token이 만료까지 downstream에서 유효할 수 있다.
- 다음 작업: 모바일이 탈퇴 API 성공과 `ACCOUNT_WITHDRAWN`을 같은 멱등 terminal handler로 처리하도록 별도 저장소에서 구현하고 staging E2E를 수행한다. 다음 고정 서버 단계는 Stage 5 `UserWithdrawn` event와 Learning Core downstream deny marker다.

## 2026-08-27 — Stage 5 UserWithdrawn event·downstream deny marker 설명

<!-- codex-turn:01a04199-2aa6-78b0-a1c8-4ddbe5207446 -->

- 날짜: 2026-08-27
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 신규 이슈 생성·수정·댓글·상태 전환 없음. `TMI-108`은 완료 상태를 유지한다.
- 작업 목표: 다음 고정 구현 순서인 Stage 5가 해결하는 Access Token 잔여 유효시간 문제와 Identity·Learning Core 책임, 이벤트·TTL·배포 순서를 현재 저장소 기준으로 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 문서는 변경하지 않았다.
- 구현 내용: 회원 탈퇴는 RefreshSession을 모두 폐기하지만 이미 발급된 stateless Access Token은 최대 TTL까지 Learning Core의 로컬 JWT 검증을 통과할 수 있다. Stage 5에서 Identity는 withdrawal commit과 같은 Transaction에 `UserWithdrawn` outbox를 생성하고 최소 eventId·schemaVersion·userId·withdrawnAt을 at-least-once 전달한다. Learning Core는 eventId inbox로 중복과 payload 충돌을 분류하고 userId deny marker를 자기 저장소에 기록한다.
- 구현 내용: Learning Core 보호 요청은 기존 issuer·audience·signature·expiration 검증을 통과한 뒤 JWT `sub`로 로컬 marker를 확인해 유효 차단 기간이면 application 진입 전에 거절한다. marker의 `blockedUntil/expireAt`은 withdrawnAt에 시스템 최대 Access Token 수명과 verifier clock skew 안전 여유를 더해 계산하고 TTL로 정리하되, TTL 삭제 지연에 의존하지 않고 요청 시 `blockedUntil`을 직접 비교한다. inbox는 outbox 자동 재전달·dead-letter·수동 replay 기간보다 길게 별도 TTL로 보존한다.
- 실행한 테스트와 결과: 코드 변경 없는 설명 작업이라 Gradle 테스트는 실행하지 않았다. 고정 구현 순서, Stage 4 계획서, 기존 Identity outbox·publisher 구조와 Learning Core JWT 인증 경계를 읽어 대조했다. 문서 변경은 `git diff --check`와 marker 단일 존재로 검증한다.
- 유지한 계약: 매 Learning Core 요청마다 Identity introspection을 호출하지 않고 기존 JWKS 로컬 검증을 유지한다. Token 원문·hash·Provider credential을 event·inbox·marker에 저장하지 않으며 Identity에 시험·결과 데이터를 추가하지 않는다. deny marker는 사용자별 임시 authorization 차단이지 token별 blacklist나 학습 데이터 삭제가 아니다.
- 결정사항: 배포는 Learning Core consumer·inbox·deny gate를 먼저 완료하고, 그 뒤 Identity outbox·publisher를 활성화한다. 이벤트 전달은 at-least-once이므로 consumer 멱등성이 필수이며 Stage 5가 제공하는 즉시성은 동기 호출이 아니라 event delivery 지연 범위 안의 수렴이다.
- 위험 요소: event schema·서버 간 인증·Learning Core 오류 code, 최대 Access Token TTL·clock skew·inbox replay 보존기간, 기존 WITHDRAWN User의 backfill/cutover watermark를 계획서에서 확정해야 한다. event 전달 지연·dead-letter 동안 짧은 접근 공백이 생길 수 있고 이 작업은 시험·결과 삭제 또는 익명화 정책을 대신하지 않는다.
- 다음 작업: Stage 5 저장소 계획서에서 wire schema, Identity withdrawal Transaction/outbox·publisher, Learning Core inbox·marker·request gate, TTL/index, backfill, 장애·replay·배포·rollback과 양 서비스 테스트를 확정한 뒤 Jira를 서비스별 또는 통합 작업으로 생성한다.

## 2026-08-27 — Stage 5 UserWithdrawn event·deny marker 구현 계획서 작성

<!-- codex-turn:01a04206-6662-7452-8301-98b9fce8d027 -->

- 날짜: 2026-08-27
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 신규 이슈 생성·수정·댓글·상태 전환 없음. `TMI-108`은 완료 상태를 유지한다.
- 작업 목표: 탈퇴 전에 발급된 Access Token의 잔여 유효시간을 Learning Core에서 차단하는 Stage 5의 Identity producer, Learning Core consumer·deny gate, TTL·backfill·보안·배포·테스트 계약을 저장소 계획서로 확정한다.
- 변경 파일: `docs/contracts/user-withdrawn-downstream-deny-marker-stage-5-plan.md`, `docs/contracts/firebase-auth-follow-up-implementation-order.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: v1 event를 `eventId`, `schemaVersion`, `userId`, `withdrawnAt`으로 제한하고 semantic SHA-256 payload digest, 동일 event duplicate 204, 다른 payload conflict 격리를 정의했다. Identity는 User·Session·lifecycle·eligibility와 `UserWithdrawnOutbox`를 같은 Mongo Transaction으로 commit하고 기존 UserMerged 패턴과 분리된 atomic lease·retry·dead-letter publisher로 at-least-once 전달한다.
- 구현 내용: Learning Core는 workload 전용 internal endpoint에서 inbox와 userId deny marker를 한 local Mongo Transaction으로 저장한다. 기존 RS256 issuer·audience·signature·expiration 검증 뒤 `WithdrawnUserAccessGateFilter`가 marker를 조회해 유효 기간에는 `401 ACCOUNT_WITHDRAWN`, marker store 장애에는 fail-closed `503 WITHDRAWAL_DENY_GATE_UNAVAILABLE`을 반환한다. public AI callback과 workload endpoint는 사용자 deny gate 대상이 아니다.
- 구현 내용: marker는 withdrawnAt에 시스템 최대 Access Token 수명과 verifier clock skew를 더한 blockedUntil까지만 유효하고 TTL 지연과 무관하게 시각을 직접 비교한다. inbox는 producer의 dead-letter·manual replay보다 길게 별도 보존한다. backfill은 모든 pod에 outbox capture가 배포된 시각을 고정 upper bound로 사용하고 그 직전 Access Token 유효 구간만 처리해 outbox TTL 삭제 뒤 과거 User가 반복 event로 재생성되는 문제를 막는다.
- 실행한 테스트와 결과: 문서 작업이라 Gradle 테스트는 실행하지 않았다. Identity withdrawal Transaction·UserMerged outbox/publisher/configuration, JWT TTL과 Learning Core SecurityFilterChain·JwtCurrentUserProvider·Repository scan·오류 경계를 읽어 계획과 대조했다. 문서 변경은 `git diff --check`, 링크와 지정 marker 단일 존재로 검증한다.
- 유지한 계약: Learning Core는 매 요청 Identity introspection 없이 기존 JWKS 로컬 검증을 유지한다. Token 원문·hash·Provider credential을 event·inbox·marker에 저장하지 않고 Identity 저장소에 시험·결과 코드를 추가하지 않는다. deny marker는 유한 사용자별 authorization 차단이며 학습 데이터 삭제·새 User 이전·token별 blacklist가 아니다.
- 결정사항: 구현 Jira는 Learning Core consumer·inbox·deny gate와 Identity outbox·publisher·backfill의 최소 두 개로 분리하고 consumer 작업을 선행 관계로 둔다. producer보다 consumer를 먼저 배포하며 outbox capture는 publisher 비활성 상태로 먼저 배포한다. workload 인증은 사용자 JWT를 재사용하지 않고 실제 ECS ingress·SigV4 또는 workload OIDC profile이 승인되기 전 publisher를 활성화하지 않는다.
- 위험 요소: workload 인증 방식과 internal endpoint, Learning Core의 명시적 JWT clock skew, inbox replay retention, production Transaction/index, 실제 filter order가 아직 구현·검증되지 않았다. event delivery 지연·dead-letter 동안 짧은 접근 공백이 남고 Stage 5는 시험·결과 데이터 삭제 또는 익명화를 대신하지 않는다.
- 다음 작업: 계획서의 활성화 전 결정값을 승인한 뒤 Learning Core Jira를 먼저 생성하고 Identity Jira를 blocks 관계로 연결한다. consumer 구현·배포 후 Identity outbox capture, 최근 cutover backfill, publisher를 순서대로 구현·검증한다.

## 2026-08-27 — Stage 5 internal endpoint 하이픈 제거

<!-- codex-turn:01a04223-1d4a-7983-ae78-3f7bb748e22f -->

- 날짜: 2026-08-27
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 신규 이슈 생성·수정·댓글·상태 전환 없음. `TMI-108`은 완료 상태를 유지한다.
- 작업 목표: Stage 5 계획의 Learning Core internal event URL에서 하이픈을 제거하고 endpoint 표기를 하나로 고정한다.
- 변경 파일: `docs/contracts/user-withdrawn-downstream-deny-marker-stage-5-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: 제안 endpoint를 `POST /internal/v1/events/user-withdrawn`에서 `POST /internal/v1/events/userwithdrawn`으로 변경했다. 기존 하이픈 포함 endpoint 표기가 계획서에 남지 않도록 검색 검증한다.
- 실행한 테스트와 결과: 문서 경로 변경만 수행해 Gradle 테스트는 실행하지 않았다. `git diff --check`, 이전 endpoint 0건, 신규 endpoint 1건과 지정 marker 단일 존재를 검증한다.
- 유지한 계약: HTTP method, internal v1 event 의미, workload 인증, wire payload, semantic digest, consumer 선배포와 민감정보 비노출 계약은 변경하지 않았다.
- 결정사항: 공개·내부 API 경로에는 사용자 요청에 따라 `userwithdrawn`을 한 단어의 lowercase segment로 사용한다. digest domain separator와 Markdown 파일명은 URL endpoint가 아니므로 기존 값을 유지한다.
- 위험 요소: 구현 시 Learning Core Controller와 Identity publisher configuration, contract test가 모두 동일한 하이픈 없는 경로를 사용해야 한다. 이전 경로는 아직 코드로 배포된 적이 없어 redirect·alias 호환 경로를 만들지 않는다.
- 다음 작업: Stage 5 Jira Payload와 양 저장소 구현에서 exact endpoint `/internal/v1/events/userwithdrawn`을 사용한다.

## 2026-08-27 — Stage 5 internal endpoint 단순화

<!-- codex-turn:01a0422d-withdrawn-endpoint -->

- 날짜: 2026-08-27
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 신규 이슈 생성·수정·댓글·상태 전환 없음. `TMI-108`은 완료 상태를 유지한다.
- 작업 목표: Stage 5 Learning Core internal event endpoint를 문맥상 충분한 짧은 경로로 단순화한다.
- 변경 파일: `docs/contracts/user-withdrawn-downstream-deny-marker-stage-5-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: 계획서의 endpoint를 `POST /internal/v1/events/userwithdrawn`에서 `POST /internal/v1/events/withdrawn`으로 변경하고 현재 상태 문서를 동기화했다. 과거 WORKLOG 기록은 당시 변경 이력으로 보존했다.
- 실행한 테스트와 결과: 문서 경로 변경만 수행해 Gradle 테스트는 실행하지 않았다. `git diff --check`, 계획서 내 이전 endpoint 0건, 신규 endpoint 1건과 지정 marker 단일 존재를 검증한다.
- 유지한 계약: HTTP method, internal v1 event 의미, workload 인증, wire payload, semantic digest, consumer 선배포와 민감정보 비노출 계약은 변경하지 않았다.
- 결정사항: `withdrawn`은 HTTP 경로 segment로 사용할 수 있으며 `/events` 상위 문맥과 event payload가 의미를 보완한다. 아직 배포된 endpoint가 아니므로 이전 경로 alias나 redirect는 두지 않는다.
- 위험 요소: 영어 자원명으로는 명사형 `withdrawals`도 가능하지만 이번 계약은 사용자 요청에 따라 `withdrawn`으로 고정한다. 구현 시 양 서비스 Controller·publisher 설정·contract test가 exact path를 공유해야 한다.
- 다음 작업: Stage 5 Jira와 양 저장소 구현에서 exact endpoint `/internal/v1/events/withdrawn`을 사용한다.

## 2026-08-27 — Stage 5 withdrawn endpoint 확정

<!-- codex-turn:01a04224-b9d1-7ab2-8c24-4272b18b273c -->

- 날짜: 2026-08-27
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 신규 이슈 생성·수정·댓글·상태 전환 없음. `TMI-108`은 완료 상태를 유지한다.
- 작업 목표: Stage 5 internal event URL을 `/internal/v1/events/withdrawn`으로 확정하고 현재 문서 상태를 검증한다.
- 변경 파일: `docs/contracts/user-withdrawn-downstream-deny-marker-stage-5-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: endpoint segment로 `withdrawn`을 사용할 수 있음을 확인하고 계획서의 exact path를 `POST /internal/v1/events/withdrawn`으로 유지했다. 현재 상태 문서에도 동일한 경로와 검증 결과를 반영했다.
- 실행한 테스트와 결과: 문서 변경만 있어 Gradle 테스트는 실행하지 않았다. `git diff --check`가 통과했고 계획서·현재 상태에서 이전 `/user-withdrawn`, `/userwithdrawn` 경로가 0건이며 신규 `/withdrawn` 경로가 존재함을 확인했다.
- 유지한 계약: HTTP method, internal v1 event 의미, workload 인증, event payload, consumer 선배포, 민감정보 비노출과 Git commit·push 금지 계약은 변경하지 않았다.
- 결정사항: Stage 5 consumer와 publisher는 exact endpoint `/internal/v1/events/withdrawn`을 사용한다. 아직 배포된 계약이 아니므로 이전 경로 호환 alias는 추가하지 않는다.
- 위험 요소: 구현 시 Identity publisher와 Learning Core Controller·contract test의 경로가 다르면 전달이 실패하므로 양쪽에서 exact path를 고정해야 한다.
- 다음 작업: Stage 5 Jira 생성 시 계획서의 endpoint와 양 서비스 구현 순서를 완료 조건에 반영한다.

## 2026-08-27 — 하이픈 포함 HTTP 경로 점검

<!-- codex-turn:01a04235-url-hyphen-audit -->

- 날짜: 2026-08-27
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 신규 이슈 생성·수정·댓글·상태 전환 없음. `TMI-108`은 완료 상태를 유지한다.
- 작업 목표: Identity 애플리케이션과 계약 문서에 하이픈을 사용하는 다른 HTTP URL 경로가 있는지 확인하고 성격별로 분류한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 경로는 변경하지 않았다.
- 구현 내용: Controller mapping, Security allowlist, request logging route, Springdoc 설정, publisher endpoint 설정과 계약 문서를 검색했다. 실제 비즈니스 API의 `/check-email`, `/logout-all`, `/auth-methods/sync`, 표준·도구 경로의 `/.well-known/jwks.json`, `/swagger-ui...`, `/v3/api-docs`, 계획 단계의 `/internal/v1/phone-eligibility-bindings/events`를 확인했다. UserMerged publisher endpoint는 환경변수로 주입되며 production 코드에 exact path가 고정되어 있지 않다.
- 실행한 테스트와 결과: 읽기 전용 점검과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았다. Controller annotation과 Security·logging·OpenAPI 설정, 관련 테스트 및 계약 문서의 검색 결과를 상호 대조했고 문서 변경은 `git diff --check`로 검증한다.
- 유지한 계약: 기존 공개 API, JWKS 표준 discovery, Springdoc 경로, publisher 설정과 클라이언트 호환성을 변경하지 않았다. 외부 URL·credential·Secret과 사용자 개인정보를 작업 기록에 추가하지 않았다.
- 결정사항: `/.well-known`은 표준 경로이므로 변경 대상에서 제외하고 Springdoc 경로도 제품 API 명명 규칙과 분리한다. 실제 비즈니스 API 3개와 계획 단계 internal endpoint의 변경은 별도 호환성·consumer 선배포 결정을 거쳐야 한다.
- 위험 요소: 기존 공개 API를 즉시 변경하면 모바일·웹 client와 Security allowlist·logging·OpenAPI·테스트가 깨질 수 있다. configurable publisher endpoint는 배포 환경값까지 확인해야 실제 하이픈 사용 여부가 확정된다.
- 다음 작업: 사용자가 하이픈 없는 URL 정책을 기존 API까지 적용하기로 하면 exact 대체 경로, 호환 alias·deprecation 기간과 양쪽 consumer 변경 순서를 먼저 확정한다.

## 2026-08-27 — 하이픈 URL 점검 결과 확정

<!-- codex-turn:01a04226-5c47-7842-bf3f-25539380d83d -->

- 날짜: 2026-08-27
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 신규 이슈 생성·수정·댓글·상태 전환 없음. `TMI-108`은 완료 상태를 유지한다.
- 작업 목표: 저장소에 남은 하이픈 포함 URL을 확인하고 실제 제품 API와 표준·도구·계획 경로를 구분해 사용자에게 전달한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 경로는 변경하지 않았다.
- 구현 내용: 실제 비즈니스 API `/check-email`, `/logout-all`, `/auth-methods/sync` 3개를 확인했다. JWKS의 `/.well-known`, Springdoc의 `/swagger-ui...`와 `/v3/api-docs`는 표준·도구 경로로 분류했다. 구현 전 계약에는 `/internal/v1/phone-eligibility-bindings/events`가 있고 UserMerged publisher의 exact endpoint는 환경 설정으로 주입됨을 확인했다.
- 실행한 테스트와 결과: 읽기 전용 점검과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았다. Controller·Security·logging·Springdoc·계약 문서 검색을 대조했고 `git diff --check`가 통과했다.
- 유지한 계약: 기존 공개 API와 표준 JWKS discovery, Springdoc 경로, configurable publisher endpoint를 변경하지 않았다. Secret·Token·credential·사용자 개인정보를 기록하지 않았다.
- 결정사항: 표준·도구 경로는 하이픈 없는 제품 API 명명 정책의 변경 대상에서 제외한다. 기존 비즈니스 API는 client 호환성 때문에 별도 전환 계획 없이 즉시 변경하지 않는다.
- 위험 요소: 공개 API를 교체할 때 모바일·웹 client, Security allowlist, request logging, OpenAPI와 테스트를 함께 갱신하지 않으면 인증 또는 호출이 실패한다. 배포 환경의 UserMerged endpoint는 저장소만으로 실제 경로를 확정할 수 없다.
- 다음 작업: 기존 비즈니스 API도 변경한다면 `/email/check`, `/logout/all`, `/methods/sync` 같은 exact 대체 경로와 alias·deprecation 기간을 승인받아 별도 작업으로 진행한다.

## 2026-08-27 — Stage 5 Learning Core Jira 생성 초안 준비

<!-- codex-turn:01a0422e-7f71-77b2-b2d5-92a4104587b1 -->

- 날짜: 2026-08-27
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: 공식 Atlassian Rovo로 TMI 프로젝트의 URL·API·endpoint 관련 이슈를 검색하고 사용 가능한 이슈 유형을 조회했다. 동일한 `UserWithdrawn` consumer·deny gate 범위의 중복 후보는 없었고 `작업` 유형을 확인했다. 생성·수정·댓글·상태 전환은 아직 수행하지 않았다.
- 승인 여부: 사용자가 Jira 생성을 요청했으나 저장소 규칙에 따라 mutation 전에 exact 제목·유형·우선순위·본문·완료 조건을 먼저 제시하고 최종 승인을 기다린다.
- 작업 목표: Stage 5의 consumer 선배포 순서에 따라 Learning Core `UserWithdrawn` inbox·deny marker·JWT gate 구현 Jira 초안을 확정한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 구현 내용: 제안 Jira는 `POST /internal/v1/events/withdrawn`, v1 wire schema, workload 전용 인증 경계, eventId semantic digest inbox, userId deny marker와 단일 Mongo Transaction, 유효 JWT 이후 request gate, `401 ACCOUNT_WITHDRAWN`, marker 저장소 장애의 fail-closed `503`, TTL·관측·보안·consumer 선배포를 포함한다. Identity outbox·publisher·backfill은 후속 별도 Jira로 제외한다.
- 실행한 테스트와 결과: Jira 중복·프로젝트 유형 조회와 문서 초안 작업이라 Gradle 테스트는 실행하지 않았다. Stage 5 계획서의 Learning Core 범위·테스트·완료 조건과 Learning Core 저장소의 기존 JWT·workload 인증 결정 문서를 읽어 대조했다. 문서 변경은 `git diff --check`로 검증한다.
- 유지한 계약: 매 요청 Identity introspection 없이 기존 JWT 로컬 검증을 유지하고 사용자 JWT를 workload 인증에 재사용하지 않는다. workload 실제 issuer·JWKS·principal·rotation과 다른 운영값을 임의로 만들거나 Jira에 Secret·Token·개인정보를 기록하지 않는다. Jira mutation 전 승인 규칙과 Git commit·push 금지를 유지했다.
- 결정사항: 이슈 유형은 `작업`, 우선순위는 High를 제안한다. workload 인증 실제 profile과 명시적 clock skew 등 미확정 값은 승인 전 production 비활성 gate로 남기며 구현 Jira를 닫기 전에 staging contract를 확정해야 한다.
- 위험 요소: workload 인증 profile, Learning Core 명시적 JWT clock skew와 inbox retention의 최종 운영값이 미확정이다. consumer보다 Identity publisher를 먼저 활성화하거나 marker store 장애를 fail-open하면 탈퇴 Access Token 차단 계약이 깨진다.
- 다음 작업: 사용자에게 Jira 제목·설명·완료 조건을 제시해 최종 승인을 받은 뒤 TMI `작업` High 이슈를 생성하고 생성된 키를 계획서·WORKLOG·CURRENT_STATE에 연결한다. 이후 Identity producer Jira를 별도로 만들고 blocks 관계를 설정한다.

## 2026-08-27 — Jira TMI-109 Learning Core UserWithdrawn consumer 생성

<!-- codex-turn:01a04231-b960-7163-8136-e1bc5e59dfdd -->

- 날짜: 2026-08-27
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: `TMI-109`
- Jira 작업: 공식 Atlassian Rovo로 `[Learning Core] UserWithdrawn inbox·deny marker·Access Token 차단 gate 구현` 이슈를 TMI 프로젝트 `작업`, High 우선순위로 생성했다. 재조회 결과 상태 `해야 할 일`, Resolution 없음, 담당자·라벨·댓글 없음과 `UserWithdrawn` 제목 이슈 1건만 존재함을 확인했다. 상태·설명·댓글·담당자·라벨의 후속 변경은 수행하지 않았다.
- 승인 여부: 생성할 제목, 유형, 우선순위, 포함·제외 범위와 완료 조건을 먼저 제시했고 사용자가 “어 생성해줘”라고 최종 승인했다.
- 작업 목표: Stage 5 consumer 선배포 순서를 추적할 Learning Core `UserWithdrawn` inbox·deny marker·JWT gate Jira를 생성하고 저장소 계획서에 연결한다.
- 변경 파일: `docs/contracts/user-withdrawn-downstream-deny-marker-stage-5-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: Jira에는 exact endpoint `POST /internal/v1/events/withdrawn`, eventId·schemaVersion·userId·withdrawnAt v1 계약, semantic digest inbox, userId deny marker와 단일 Mongo Transaction, duplicate 204·payload conflict 격리, 유효 JWT 이후 `401 ACCOUNT_WITHDRAWN`, store 장애 `503 WITHDRAWAL_DENY_GATE_UNAVAILABLE`, TTL·workload SecurityFilterChain·보안·테스트·consumer 선배포를 기록했다. Identity outbox·publisher·backfill은 후속 별도 이슈로 제외했다.
- 실행한 테스트와 결과: Jira 생성·재조회와 문서 키 연결 작업이라 Gradle 테스트는 실행하지 않았다. 최초 생성 요청은 Atlassian 504로 성공 여부가 불명확했으나 exact 제목 조회에서 0건을 확인한 뒤 간결한 동일 범위 본문으로 재시도해 `TMI-109` 한 건을 생성했다. 후속 Jira 재조회와 중복 검색, `git diff --check`, 지정 marker 단일 존재로 검증한다.
- 유지한 계약: 매 요청 Identity introspection을 추가하지 않고 사용자 JWT를 workload 인증에 재사용하지 않는다. workload 실제 issuer·JWKS·principal·rotation 등 미확정 운영값을 임의로 만들지 않았고 production 활성화 전 승인 gate를 Jira에 포함했다. Jira와 문서에 Secret·Token·credential·사용자 개인정보를 기록하지 않았으며 Git commit·push 금지를 유지했다.
- 결정사항: `TMI-109`는 Learning Core consumer 선행 이슈다. Identity producer·bounded backfill은 후속 Jira로 분리하고 `TMI-109`가 blocks 관계가 되도록 연결한다. workload 인증 profile, 명시적 clock skew·retention 승인 전 consumer·publisher production 활성화를 금지한다.
- 위험 요소: workload 인증 실제 profile, Learning Core 명시적 JWT clock skew, inbox retention과 replica set staging E2E가 아직 미확정·미수행이다. Jira 생성은 구현·배포 완료를 의미하지 않으며 Identity publisher를 먼저 활성화하면 event 처리 공백이 생긴다.
- 다음 작업: Learning Core 저장소에서 `TMI-109` 계획과 현재 JWT·Mongo 경계를 읽고 consumer 구현 계획서 또는 구현을 진행한다. 그 다음 Identity outbox·publisher·bounded backfill Jira를 생성해 blocks 관계를 설정한다.

## 2026-08-27 — Jira TMI-109 Description 표시 진단

<!-- codex-turn:01a04237-a35b-7010-b55a-b0d4384e81eb -->

- 날짜: 2026-08-27
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: `TMI-109`
- Jira 작업: 공식 Atlassian Rovo로 이슈의 summary, description, renderedFields, status, priority와 Resolution을 읽기 전용 재조회했다. Description 원문과 렌더링 HTML이 모두 저장돼 있음을 확인했으며 이슈 수정·댓글·상태 전환은 수행하지 않았다.
- 승인 여부: 사용자는 작성 내용이 보이지 않는 현상을 알려 진단을 요청했다. Description 재저장이나 댓글 추가는 Jira mutation이므로 exact 변경 내용을 제시하고 별도 승인받기 전에는 수행하지 않는다.
- 작업 목표: `TMI-109` Description이 실제 저장되지 않은 문제인지 Jira 화면 표시 문제인지 구분한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 구현 내용: API 응답의 Description에 목적, 구현 범위, 제외 범위, 완료 조건과 선행·후속 관계가 존재하고 renderedFields에도 HTML로 변환돼 있음을 확인했다. 로그인된 사용자 UI 확인을 위해 브라우저 화면도 열었으나 별도 브라우저 세션은 Atlassian 로그인 화면이어서 실제 issue layout은 검증하지 못했다.
- 실행한 테스트와 결과: Jira 읽기 전용 조회와 문서 기록 작업이라 Gradle 테스트는 실행하지 않았다. Jira API 원문·renderedFields를 대조했으며 문서 변경은 `git diff --check`와 지정 marker 단일 존재로 검증한다.
- 유지한 계약: Jira mutation 전 승인, Git commit·push 금지와 민감정보 비노출 규칙을 유지했다. Jira Description과 작업 기록에 Secret·Token·credential·사용자 개인정보를 추가하지 않았다.
- 결정사항: 서버 측 Description 유실은 아니다. 사용자 화면에서 계속 보이지 않으면 같은 본문을 Description에 명시적으로 재저장하는 것을 우선하고, 중복 comment 추가는 필요할 때만 별도 승인받아 수행한다.
- 위험 요소: Jira UI cache, issue layout의 Description field 숨김·접힘 또는 사용자 화면의 다른 issue 선택 여부는 API만으로 확인할 수 없다. 내용 재저장이 UI layout 문제를 반드시 해결하는 것은 아니다.
- 다음 작업: 사용자에게 현재 저장된 Description 요약과 재저장 예정 내용을 보여주고 승인받으면 `TMI-109` Description을 동일 본문으로 다시 저장한 뒤 API와 화면에서 재검증한다.

## 2026-08-27 — Jira TMI-109 Description 대화 재안내

<!-- codex-turn:tmi109-description-shared -->

- 날짜: 2026-08-27
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: `TMI-109`. 조회·생성·수정·댓글·상태 전환을 수행하지 않았다.
- 작업 목표: 사용자가 Jira 화면에서 확인하기 어려웠던 `TMI-109` Description을 현재 대화에 그대로 제공한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 구현 내용: Jira에 이미 저장된 목적, 구현 범위, 제외 범위, 완료 조건과 선행·후속 관계를 대화용 Markdown으로 다시 전달한다.
- 실행한 테스트와 결과: 설명 전달과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았다. 문서 변경은 `git diff --check`로 검증한다.
- 유지한 계약: Jira mutation 전 승인, Git commit·push 금지와 민감정보 비노출 규칙을 유지했다. Secret·Token·credential·사용자 개인정보를 기록하지 않았다.
- 결정사항: Jira Description은 수정하지 않고 저장된 내용을 그대로 재안내한다.
- 위험 요소: 대화에 제공한 사본은 Jira의 원본 필드를 변경하지 않는다.
- 다음 작업: 사용자가 원하면 Jira Description을 동일 본문으로 명시적으로 재저장하거나 `TMI-109` 구현 계획을 진행한다.

## 2026-08-27 — Jira TMI-109 Description 재안내 종료 기록

<!-- codex-turn:01a04239-c4e3-7db1-9d2b-94bbddfe8b75 -->

- 날짜: 2026-08-27
- 브랜치: `develop` (Codex commit·push 미수행)
- Jira: `TMI-109`. Jira 조회·생성·수정·댓글·상태 전환은 수행하지 않았다.
- 작업 목표: Jira에 이미 저장된 Description 전체를 사용자가 현재 대화에서도 확인할 수 있도록 재안내한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 구현 내용: 목적, 구현 범위, 제외 범위, 완료 조건과 선행·후속 관계를 Jira 원문 의미 그대로 대화에 제공했다.
- 실행한 테스트와 결과: 설명 전달과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았다. `git diff --check`와 지정 marker 단일 존재를 검증한다.
- 유지한 계약: Jira mutation 전 승인, Git commit·push 금지와 민감정보 비노출 규칙을 유지했다. Secret·Token·credential·사용자 개인정보를 기록하지 않았다.
- 결정사항: Jira Description은 변경하지 않고 대화에 사본만 제공했다.
- 위험 요소: 대화 사본은 Jira 원본의 화면 레이아웃 또는 표시 문제를 해결하지 않는다.
- 다음 작업: 사용자가 요청하면 Jira Description 재저장 또는 Learning Core `TMI-109` 구현 계획을 별도 진행한다.

## 2026-08-27 — Phone eligibility Billing transport 계약 보정

- 날짜: 2026-08-27
- 브랜치: `develop`
- Jira: `TMI-95` 기존 완료 계약의 transport 보정; Jira 변경 없음
- 작업 목표: Billing에서 확정된 C3-D와 기존 Identity ADR-002의 Bearer transport 불일치를 문서상 정리하고 staging 전 구현 gate를 명시한다.
- 변경 파일: `docs/adr/ADR-002-phone-eligibility-binding-server-contract.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`; Billing·Learning Core의 관련 계약·작업 기록 문서.
- 구현 내용: eligibility endpoint를 `/internal/v1/eligibility/trial/events`, 인증을 VPC Lattice AWS_IAM·ECS application task role·SigV4로 보정했다. 429·503 `Retry-After`, eligibility 409 EVENT_ID_CONFLICT 전용과 delivery port의 status+Retry-After 목표를 추가했다.
- 실행한 테스트와 결과: 문서만 변경해 Gradle 테스트는 실행하지 않았다. stale Bearer route 문자열, `git diff --check`와 trailing whitespace를 종료 전에 검증한다.
- 유지한 계약: PhoneEligibilityBindingVerified/Revoked schema v1, eventId·bindingRevision, candidate HMAC의 Identity 소유, at-least-once delivery와 raw phone·credential 비기록을 유지했다.
- 결정사항: 기존 workload Bearer JWT 계약은 Billing C3-D로 대체된다. 현재 JDK adapter는 legacy 구현이며 publisher 기본 disabled를 유지하고 SigV4 adapter 배포 전 staging 연동을 활성화하지 않는다.
- 위험 요소: 실제 adapter는 아직 Bearer credential과 audience를 사용하고 Retry-After를 읽지 않는다. ADR 보정만으로 Billing 호출이 성공하지 않는다.
- 다음 작업: Billing PLAN-001 consumer가 준비되면 Identity SigV4 delivery adapter·설정·contract test를 별도 구현하고 staging positive/negative E2E를 수행한다.

## 2026-08-27 — TMI-109 Learning Core UserWithdrawn consumer 구현 연계

<!-- codex-turn:01a0423b-8d3b-76e0-bb98-784f4d37b026 -->

- 날짜: 2026-08-27
- 브랜치: Identity `develop`; 실제 애플리케이션 구현 대상은 Learning Core `develop` working tree다.
- Jira: `TMI-109`; Jira 조회·댓글·필드·상태 변경은 수행하지 않았다.
- 작업 목표: Stage 5 순서에 따라 Identity producer보다 먼저 Learning Core의 `UserWithdrawn` inbox·deny marker·Access Token 차단 gate를 구현한다.
- 변경 파일: Learning Core의 `src/main/java/web/tosunsaeng/domain/withdrawal/**`, 관련 Security·Auth·Error·설정 파일과 withdrawal 테스트, Learning Core 작업 기록; Identity에서는 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 연계 갱신했다.
- 구현 내용: workload JWT 전용 `POST /internal/v1/events/withdrawn`, v1 event 검증·semantic digest, eventId inbox와 userId marker 단일 Mongo Transaction, duplicate 204·conflict 409, JWT 이후 `401 ACCOUNT_WITHDRAWN`, marker store 장애 fail-closed 503, marker·inbox TTL과 기본 비활성 설정을 Learning Core에 추가했다.
- 실행한 테스트와 결과: Learning Core withdrawal·인증 집중 테스트와 `./gradlew clean test`가 성공했다. XML 기준 `389/0/0/0` tests/failures/errors/skipped다. Identity 애플리케이션 코드는 변경하지 않아 Identity Gradle 테스트는 실행하지 않았다.
- 유지한 계약: Identity 사용자 JWT와 workload JWT를 분리했고 기존 Learning Core 공개 API·BaseResponse·retryCount·S3·Redis·AI Callback 및 `user_id=examId` 계약을 유지했다. Secret·Token·credential·개인정보를 기록하지 않았다.
- 결정사항: consumer 선배포와 feature OFF를 유지한다. workload profile, 사용자 Access Token 최대 TTL, verifier clock skew와 inbox retention이 승인되기 전 production 활성화를 금지한다.
- 위험 요소: 실제 replica set Transaction, multi-instance 동시성, workload JWKS rotation과 Identity publisher 연동은 staging E2E 전까지 검증되지 않았다.
- 다음 작업: Learning Core consumer를 비활성 상태로 선배포·검증한 뒤 Identity `UserWithdrawn` outbox·publisher·bounded backfill을 후속 Jira로 구현한다.

## 2026-08-27 — Jira TMI-109 완료 전환 보류

<!-- codex-turn:01a04259-c844-72e2-a82d-0e466ecd0a75 -->

- 날짜: 2026-08-27
- 브랜치: Identity `develop`; 구현 대상 Learning Core도 `develop`이다.
- Jira: `TMI-109`를 읽기 전용 조회했다. 상태는 `해야 할 일`, Resolution은 없음이며 댓글·필드·상태 전환은 수행하지 않았다.
- 승인 여부: 사용자가 Jira 종료를 요청했지만 저장소 규칙상 PR 병합 확인이 완료 전환의 선행 조건이다.
- 작업 목표: `TMI-109` 구현 병합 여부를 확인하고 조건이 충족되면 Jira를 완료로 전환한다.
- 변경 파일: Identity `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 확인 결과: Learning Core의 withdrawal production·test 파일과 관련 설정은 아직 working tree의 미커밋·미추적 변경이다. local `develop`과 `origin/develop`은 모두 `514fb49`이며 해당 구현 커밋이 원격 브랜치나 병합 이력에 없다.
- 실행한 테스트와 결과: 이번 turn은 Jira·Git 상태 확인만 수행해 Gradle 테스트를 재실행하지 않았다. 직전 Learning Core 전체 테스트 결과는 `389/0/0/0`이다.
- 유지한 계약: PR 병합 확인 전 Jira 완료 금지, Jira mutation 전 승인, Git commit·push 금지와 민감정보 비노출 규칙을 유지했다.
- 결정사항: 구현이 병합되지 않았으므로 `TMI-109`를 닫지 않는다.
- 위험 요소: 현재 상태에서 완료 처리하면 Jira와 실제 배포 가능한 소스 이력이 불일치한다. 작업 트리에 관련 없는 기존 변경도 함께 있어 선택적 stage가 필요하다.
- 다음 작업: 사용자가 TMI-109 관련 파일만 commit·push하고 PR을 merge한 뒤 병합 사실을 알려주면 Jira 상태와 사용 가능한 완료 transition을 재조회해 상태만 완료로 변경한다.

## 2026-08-27 — TMI-109 Learning Core 수정 범위 재확인

<!-- codex-turn:01a0425b-7bee-7ee3-94e2-aede3de9d4aa -->

- 날짜: 2026-08-27
- 브랜치: Identity `develop`; 실제 구현 대상은 Learning Core `develop` working tree다.
- Jira: `TMI-109`; Jira 조회·댓글·필드·상태 전환은 수행하지 않았다.
- 작업 목표: `TMI-109`에 Learning Core 수정이 포함되는지 저장소 책임 경계를 명확히 설명한다.
- 변경 파일: Identity `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 확인 내용: `TMI-109`는 Learning Core의 workload event endpoint, event inbox, userId deny marker, 사용자 JWT 이후 Access Token 차단 gate, TTL·Mongo Transaction과 테스트를 구현하는 consumer 이슈다.
- 유지한 계약: Identity의 `UserWithdrawn` outbox·publisher·bounded backfill은 후속 Jira로 분리한다. 기존 공개 API·JWT·AI 계약과 Git commit·push 금지, 민감정보 비노출 규칙을 유지했다.
- 테스트: 범위 설명과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았다. 문서 변경은 `git diff --check`로 검증한다.
- 결정사항: 이번 애플리케이션 구현이 Learning Core 저장소에 위치하는 것은 계획과 Jira 범위에 부합한다. Identity에는 교차 저장소 계획·현재 상태만 기록한다.
- 위험 요소: Learning Core 구현은 아직 commit·push·PR merge 전이므로 Jira 완료 전환 조건은 충족되지 않았다.
- 다음 작업: Learning Core 변경을 선택적으로 commit·push하고 PR을 merge한 뒤 `TMI-109`를 완료로 전환한다. 이후 Identity producer 작업을 별도 Jira로 진행한다.

## 2026-08-28 — Learning Core 병합 및 Jira 번호·완료 상태 재검증

<!-- codex-turn:01a04615-597b-77b2-b30f-094fd66e6a6f -->

- 날짜: 2026-08-28
- 브랜치: Identity `develop`; 확인 대상 Learning Core도 `develop`이다.
- Jira: Learning Core `TMI-109`, Identity 후속 `TMI-111`을 읽기 전용 조회했다. 댓글·본문·필드·링크·상태 전환은 수행하지 않았다.
- 승인 여부: 사용자가 Learning Core 구현 확인 후 해당 Jira 종료를 명시적으로 요청했다.
- 작업 목표: Learning Core 구현의 원격 병합 여부와 정확한 Jira 번호를 확인하고 필요한 경우 완료 상태로 전환한다.
- 변경 파일: Identity `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- Git 검증: Learning Core local `develop`과 `origin/develop`은 모두 PR #23 merge commit `4baa4f20b7b179290dd743325ef7b251a408da47`을 가리키며 구현 commit은 `96e5c2021ef4ffa6a3e1069c69c08d31e974bb31`이다. withdrawal 구현·테스트가 merge 이력에 존재한다.
- Jira 검증: Learning Core 이슈 번호는 계속 `TMI-109`이며 status `완료`, Resolution `완료`로 이미 닫혀 있다. `TMI-111`은 `[Identity] UserWithdrawn outbox·publisher·backfill 구현`이라는 별도 후속 이슈이고 status `해야 할 일`, Resolution 없음이다.
- Identity 코드 검증: `UserWithdrawnOutbox`, publisher와 backfill production 구현은 현재 Identity `develop`에 없어 `TMI-111` 완료 조건을 충족하지 않는다.
- 실행한 테스트와 결과: 이번 turn은 Git·Jira·코드 존재 여부를 읽기 전용 확인해 테스트를 재실행하지 않았다. Learning Core 구현 작업 기록에는 병합 전 전체 회귀 테스트 성공이 기록돼 있다.
- 유지한 계약: PR 병합 확인 전 Jira 완료 금지, 잘못된 이슈 종료 방지, Jira mutation 전 승인과 민감정보 비노출 규칙을 유지했다.
- 결정사항: 종료 대상인 `TMI-109`가 이미 완료라 추가 transition을 실행하지 않는다. 번호가 다른 `TMI-111`은 Identity producer 구현 전이므로 열어 둔다.
- 위험 요소: `TMI-109` 완료만으로 Identity가 withdrawal event를 발행하지 않는다. producer·backfill과 실제 workload 인증·replica set staging E2E가 끝나기 전에는 end-to-end 즉시 차단이 완성되지 않는다.
- 다음 작업: 다음 고정 순서인 Identity `TMI-111` 구현 계획을 확인하고 별도 브랜치에서 outbox·publisher·bounded backfill을 구현한다.

## 2026-08-28 — 다음 작업 TMI-111 Identity UserWithdrawn producer 설명

<!-- codex-turn:01a04644-f172-7403-8afb-a38204f9cce2 -->

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: `TMI-111`; 기존 Jira Description을 기준으로 설명했으며 댓글·본문·필드·링크·상태를 변경하지 않았다.
- 작업 목표: 완료된 Learning Core `TMI-109` 다음 단계인 Identity `UserWithdrawn` outbox·publisher·bounded backfill 작업의 목적과 구현 흐름을 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 현재 공백: `UserWithdrawalTransactionService`는 User tombstone, lifecycle·eligibility와 활성 RefreshSession 폐기를 Mongo Transaction으로 처리하지만 `UserWithdrawnOutbox`는 만들지 않는다. 이미 WITHDRAWN인 멱등 응답은 새 Transaction을 실행하지 않으므로 과거 탈퇴 User도 자동으로 event가 생기지 않는다.
- 설명 내용: 신규 탈퇴와 outbox insert를 같은 Transaction으로 commit하고, 별도 publisher가 atomic lease·재시도·dead-letter로 `POST /internal/v1/events/withdrawn`에 at-least-once 전달한다. capture 배포 전후의 최근 WITHDRAWN User만 고정 lower/upper bound와 dry-run을 사용하는 bounded backfill로 보완한다.
- 결정사항: 외부 Learning Core HTTP 호출은 탈퇴 Transaction 안에서 실행하지 않는다. outbox 저장 실패 시 탈퇴 전체를 rollback하고, commit 뒤 publisher가 네트워크 전달을 담당한다. publisher·scheduler·backfill은 기본 비활성으로 둔다.
- 유지한 계약: 공개 탈퇴 API와 `ACCOUNT_WITHDRAWN` Session 계약, v1 payload `eventId/schemaVersion/userId/withdrawnAt`, Learning Core의 duplicate 204·payload conflict, User tombstone과 개인정보 경계를 유지한다. Token·phone·Firebase UID·provider subject는 event에 넣지 않는다.
- 실행한 테스트와 결과: 설명과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았다. 현재 production code에는 `UserWithdrawnOutbox`, publisher와 backfill 구현이 없음을 확인했다.
- 위험 요소: Learning Core는 workload JWT verifier를 구현했지만 Identity의 production workload credential provider와 issuer·audience·principal·rotation 값은 아직 확정·구현되지 않았다. 실제 Mongo replica set Transaction, multi-instance lease와 staging E2E도 남아 있다.
- 다음 작업: 별도 `TMI-111` 브랜치에서 운영 인증 계약을 먼저 대조하고 outbox domain/index, withdrawal Transaction 연결, publisher, bounded backfill, 관측·runbook과 테스트 순서로 구현한다.

## 2026-08-28 — TMI-109 consumer와 TMI-111 producer 차이 설명

<!-- codex-turn:01a04681-6204-7661-aa92-9b1e55b00ecd -->

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: 완료된 Learning Core `TMI-109`, 후속 Identity `TMI-111`; Jira 조회·댓글·본문·필드·링크·상태 변경은 수행하지 않았다.
- 작업 목표: 이전 작업과 다음 작업이 동일한 `UserWithdrawn` 흐름을 다루면서도 왜 별도 구현인지 책임 경계를 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 차이: `TMI-109`는 Learning Core inbound consumer로 workload 인증, event validation, inbox·deny marker Transaction과 사용자 Access Token gate를 소유한다. `TMI-111`은 Identity outbound producer로 withdrawal Transaction outbox, publisher lease·retry·dead-letter와 과거 WITHDRAWN User bounded backfill을 소유한다.
- 현재 상태: Learning Core receiver는 구현·병합·Jira 완료됐지만 Identity sender가 없어 실제 탈퇴 시 event가 자동 전달되지 않는다. 따라서 end-to-end 즉시 차단은 아직 완성되지 않았다.
- 분리 이유: consumer를 먼저 배포해 event가 안전하게 도착할 곳을 만든 뒤 producer를 활성화해야 유실과 rollout 경합을 피할 수 있다. 양 서비스는 각자 Mongo Transaction을 갖고 네트워크 전달은 두 Transaction 사이의 at-least-once 경계다.
- 유지한 계약: 두 작업은 동일한 endpoint와 v1 payload·workload 인증 계약만 공유한다. Identity는 Learning Core marker·gate를 직접 저장하지 않고 Learning Core는 withdrawal outbox를 만들지 않는다.
- 실행한 테스트와 결과: 책임 경계 설명과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았다.
- 위험 요소: `TMI-111` 전에는 receiver가 비어 있고, workload credential 발급과 staging E2E 전 publisher를 켜면 전달 실패가 누적될 수 있다.
- 다음 작업: `TMI-111`에서 Identity sender를 구현해 완료된 `TMI-109` receiver와 end-to-end로 연결한다.

## 2026-08-28 — Stage 5 계획서와 TMI-111 관계 설명

<!-- codex-turn:01a04694-7e41-70a2-87b2-40332e352ece -->

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: 완료된 Learning Core `TMI-109`, 후속 Identity `TMI-111`; Jira 조회·댓글·본문·필드·링크·상태 변경은 수행하지 않았다.
- 작업 목표: `docs/contracts/user-withdrawn-downstream-deny-marker-stage-5-plan.md`가 다음 작업 `TMI-111`과 별개의 작업인지 관계를 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 계획서 본문과 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 확인 결과: Stage 5 계획서는 발행자 Identity와 소비자 Learning Core의 전체 end-to-end 설계다. 목적부터 Identity event 생성과 Learning Core deny gate 두 결과를 함께 정의하고, 포함 범위를 Identity·Learning Core·공통으로 분리한다.
- Jira 매핑: 계획서의 Learning Core endpoint·inbox·marker·gate·consumer 테스트 절은 `TMI-109`로 구현·완료됐다. Identity outbox 모델, withdrawal Transaction 연결, publisher, bounded backfill과 Identity 테스트 절은 `TMI-111` 구현 범위다. 공통 wire·workload 인증·rollout·staging E2E는 두 Jira의 연결 계약이다.
- 결정사항: 계획서는 세 번째 작업이 아니라 두 Jira의 상위 설계 기준이다. `TMI-111`은 계획서 전체를 다시 구현하는 것이 아니라 아직 남은 Identity 절을 구현한다.
- 문서 상태: 계획서 10행의 `Identity producer Jira: 후속 생성 예정`은 Jira 생성 전 작성된 문구이므로 현재는 `TMI-111`로 갱신할 필요가 있다. 사용자 요청이 설명이므로 이번 turn에서는 해당 과거 계획 문서를 수정하지 않았다.
- 실행한 테스트와 결과: 문서 관계 설명과 작업 기록만 수행해 Gradle 테스트는 실행하지 않았다. `git diff --check`로 기록 문서를 검증한다.
- 유지한 계약: v1 payload, consumer 선배포·producer 후배포, Identity와 Learning Core의 독립 Mongo Transaction, at-least-once 전달과 민감정보 비노출 계약을 유지했다.
- 위험 요소: 상위 계획서와 Jira 번호 메타데이터가 어긋나면 다음 구현자가 새로운 별도 작업으로 오해할 수 있다.
- 다음 작업: 사용자가 원하면 계획서의 Identity producer Jira 메타데이터를 `TMI-111`로 보정한 뒤, 그 문서의 Identity 절을 기준으로 구현 계획 또는 구현을 진행한다.

## 2026-08-28 — 과거 구현 완료 보고의 TMI-109 범위 명확화

<!-- codex-turn:01a04695-eb89-7452-aa4c-67d2383c44b8 -->

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: 완료된 Learning Core `TMI-109`, 미구현 Identity `TMI-111`; Jira 조회·댓글·본문·필드·링크·상태 변경은 수행하지 않았다.
- 작업 목표: 과거 `구현 완료` 보고가 Stage 5 전체 완료인지, Learning Core 구현만 완료한 것인지 명확히 구분한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 문서는 변경하지 않았다.
- 확인 결과: 과거 보고에 나열된 internal endpoint, inbox·deny marker Transaction, duplicate/conflict, `ACCOUNT_WITHDRAWN` gate, fail-closed 503, TTL과 security chain은 모두 Learning Core `TMI-109` 범위다. 해당 이슈는 구현·병합·Jira 완료됐다.
- 남은 범위: Identity에는 `UserWithdrawnOutbox` Entity·Repository·index, withdrawal Transaction 원자 저장, publisher lease·retry·dead-letter·replay, bounded backfill과 production workload credential provider가 없다. 따라서 `TMI-111`이 필요하다.
- 현재 동작: Learning Core receiver만 존재하며 Identity sender가 없으므로 실제 회원탈퇴로 deny marker가 자동 생성되지 않는다. 유효한 workload credential로 endpoint를 직접 호출한 경우에만 Learning Core가 event를 처리할 수 있다.
- 결정사항: Stage 5 전체 완료 표기는 `TMI-111`과 양 서비스 staging E2E가 끝난 뒤에만 사용한다. 과거 `구현 완료`는 `TMI-109 Learning Core 구현 완료`로 해석한다.
- 실행한 테스트와 결과: 상태 설명과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았다. `git diff --check`로 기록 문서를 검증한다.
- 유지한 계약: consumer 선배포·producer 후배포, 두 서비스의 독립 Mongo Transaction, at-least-once 전달과 민감정보 비노출 규칙을 유지했다.
- 위험 요소: receiver만 배포된 현재 상태를 end-to-end 탈퇴 차단 완료로 오인하면 기존 Access Token이 event 없이 만료 시점까지 허용될 수 있다.
- 다음 작업: Identity `TMI-111`을 구현하고 workload 인증·replica set·중복 전달·backfill staging E2E까지 검증한다.

## 2026-08-28 — Learning Core 범위 오해 정정

<!-- codex-turn:01a04697-1770-73e0-bfa7-60e7d0fb9ab8 -->

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: Learning Core `TMI-109`, Identity `TMI-111`; Jira 조회·댓글·본문·필드·링크·상태 변경은 수행하지 않았다.
- 작업 목표: 과거 구현 요청의 대상이 Learning Core였다는 잘못된 해석을 사용자 정정에 맞춰 바로잡는다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 문서는 변경하지 않았다.
- 정정 내용: 당시 요청은 Identity 작업 맥락이었으며 Learning Core 개발을 요청한 것이 아니었다. 과거 `구현 완료` 보고는 Codex가 `TMI-109`를 따라 별도 Learning Core 저장소를 수정한 결과일 뿐, 사용자가 의도한 Identity 작업 완료로 간주하지 않는다.
- 현재 상태: Learning Core `TMI-109` 구현과 merge 이력은 존재하지만 Identity `UserWithdrawnOutbox`, withdrawal Transaction 원자 저장, publisher, retry·dead-letter·replay와 bounded backfill은 구현되지 않았다.
- 결정사항: 다음 구현 범위는 Learning Core 추가 수정이 아니라 Identity `TMI-111`이다. Stage 5 전체 또는 사용자 요청 완료 표시는 Identity producer 구현 전에는 사용하지 않는다.
- 실행한 테스트와 결과: 범위 정정과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았다. `git diff --check`로 기록 문서를 검증한다.
- 유지한 계약: 기존 병합 이력을 임의로 되돌리지 않고, Git commit·push 금지와 Jira mutation 전 승인, 민감정보 비노출 규칙을 유지했다.
- 위험 요소: Learning Core 작업 존재 여부와 사용자가 요청한 작업 완료 여부를 혼동하면 Identity sender가 없는 상태로 end-to-end 차단이 완성됐다고 오판할 수 있다.
- 다음 작업: 사용자가 구현을 요청하면 Identity 저장소와 `TMI-111` Description을 기준으로 범위를 다시 고정한 뒤 구현한다.

## 2026-08-28 — TMI-111 남은 구현 범위와 순서 정리

<!-- codex-turn:01a04699-2db0-7e00-9ae8-34b71a1b2c19 -->

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: Identity `TMI-111`; Jira 조회·댓글·본문·필드·링크·상태 변경은 수행하지 않았다.
- 작업 목표: Stage 5 계획서에서 Learning Core `TMI-109` 완료 후 실제로 남은 Identity 구현 항목과 시작 순서를 구체화한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 문서는 변경하지 않았다.
- 구현 범위: `UserWithdrawnOutbox` Entity·상태·실패 코드·Repository·index, `UserWithdrawalTransactionService`의 필수 Repository 주입과 원자 저장, wire mapper·delivery port·HTTP adapter, atomic lease·retry·backoff·dead-letter·manual replay publisher, 기본 비활성 scheduler·configuration을 구현한다.
- backfill 범위: capture 완전 배포 시각을 기준으로 고정 lower/upper bound를 입력받고, 해당 범위의 WITHDRAWN User 중 outbox가 없는 대상만 dry-run 우선 bounded batch로 보완하며 재실행에 멱등해야 한다.
- 인증 선결 조건: Learning Core가 검증하는 workload issuer·JWKS·audience·Identity principal, Token lifetime·clock skew와 rotation을 확정하고 Identity의 production `WorkloadIdentityCredentialProvider`를 구현해야 한다. 사용자 Access Token은 재사용하지 않는다.
- 구현 순서: workload 계약 대조, Outbox domain/index, withdrawal Transaction 연결, publisher, bounded backfill, metric·startup validation·runbook, 전체 테스트, publisher 비활성 배포, staging E2E와 순차 활성화 순이다.
- 결정사항: 첫 코드 작업은 Identity Outbox capture다. 외부 Learning Core HTTP 호출을 탈퇴 Transaction 안에서 실행하지 않고 commit 이후 publisher가 담당한다. Learning Core는 이번 후속 구현의 수정 대상이 아니다.
- 실행한 테스트와 결과: 구현 순서 설명과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았다. `git diff --check`로 기록 문서를 검증한다.
- 유지한 계약: 공개 탈퇴 API, Session `ACCOUNT_WITHDRAWN`, v1 payload, consumer 선배포·producer 후배포, at-least-once 전달과 민감정보 비노출 규칙을 유지했다.
- 위험 요소: workload credential과 실제 replica set Transaction·multi-instance lease가 staging에서 검증되지 않은 상태로 publisher를 활성화하면 event 전달 실패 또는 backlog가 발생한다.
- 다음 작업: `TMI-111` 브랜치에서 workload 계약을 다시 확인한 뒤 `UserWithdrawnOutbox`와 withdrawal Transaction 연결부터 구현한다.

## 2026-08-28 — TMI-111 workload JWT 계약 권장안 제안

<!-- codex-turn:01a0469d-9e16-7f41-8a09-b49f8c807ac1 -->

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: Identity `TMI-111`; Jira 조회·댓글·본문·필드·링크·상태 변경은 수행하지 않았다.
- 작업 목표: Identity publisher가 Learning Core `POST /internal/v1/events/withdrawn`을 호출할 workload JWT 계약에서 확정해야 할 값과 권장안을 제안한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·Stage 5 계획서는 변경하지 않았다.
- 현재 검증 경계: Learning Core는 RS256, timestamp, exact issuer, audience, 설정 가능한 principal claim/value와 최대 Token lifetime을 검증하며 staging/prod에서 원격 HTTPS issuer·JWKS를 요구한다. Identity는 사용자 JWT용 RS256 encoder·JWKS와 workload credential provider port를 보유하지만 production workload 발급 구현은 없다.
- 권장안: 기존 Identity RSA signing infrastructure와 JWKS를 재사용하되 workload 전용 issuer, audience `learning-core-user-withdrawn`, `sub=identity-service`, `service=identity`, principal allowlist `service=identity`, 발급 TTL·최대 허용 수명 `PT2M`, clock skew `PT30S`를 사용한다.
- 발급·전송 정책: 공개 workload Token 발급 API와 Token cache를 만들지 않고 Identity 내부 provider가 전달 시마다 새 JWT를 발급한다. audience는 exact allowlist로 제한하며 `iat`, `exp`, canonical UUID `jti`, `kid`를 포함하고 사용자 ID·email·phone·Firebase UID·사용자 scope를 넣지 않는다. HTTPS exact endpoint와 Bearer Header를 사용하고 redirect는 따르지 않는다.
- 실패 정책: 2xx는 성공, 408·425·429·5xx·timeout·connection은 재시도, payload 400·409·422와 인증 401·403은 격리·dead-letter·경보 대상으로 제안했다. 필수 인증 설정 누락은 publisher 활성화 시 startup fail-fast한다.
- 환경·rotation: staging/prod issuer·key·kid를 분리하고 이전·신규 Public Key overlap을 사용자 Access Token 최대 수명 30분과 clock skew 1분 이상 유지하는 정책을 제안했다. 현재 JWKS가 단일 key만 노출하므로 실제 안전한 rotation에는 다중 Public Key 지원 보완이 필요하다.
- 승인 상태: 위 값은 Codex 권장안이며 사용자 최종 승인을 아직 받지 않았다. 승인 전 구현·환경값 고정·publisher 활성화를 수행하지 않는다.
- 실행한 테스트와 결과: 계약 대조와 문서 기록만 수행해 Gradle 테스트는 실행하지 않았다. `git diff --check`로 기록 문서를 검증한다.
- 유지한 계약: 사용자 JWT와 workload JWT의 issuer·audience·principal 분리, 사용자 Token 재사용 금지, 민감정보·credential 비기록과 consumer 선배포·producer 후배포 원칙을 유지했다.
- 위험 요소: 같은 signing infrastructure를 재사용하므로 claim allowlist 설정 오류를 contract test로 차단해야 하며, workload 전용 별도 키를 선택하면 설정·JWKS·rotation 구현 범위가 커진다.
- 다음 작업: 사용자가 issuer/key 재사용, exact audience·principal과 TTL·skew 값을 승인하거나 수정하면 Stage 5 계획서와 `TMI-111` 구현 계약에 반영한다.

## 2026-08-28 — TMI-111 workload JWT 계약 승인 반영

<!-- codex-turn:01a046a8-3ec1-7652-a25d-7ce469dd894e -->

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: Identity `TMI-111`; Jira 조회·댓글·본문·필드·링크·상태 변경은 수행하지 않았다.
- 작업 목표: 사용자가 제안한 workload JWT 검토안을 현재 Identity·Learning Core 경계에 대조하고 타당한 내용을 Stage 5 계획서의 확정 계약으로 반영한다.
- 변경 파일: `docs/contracts/user-withdrawn-downstream-deny-marker-stage-5-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 계약 반영: 기존 Identity RS256 signing infrastructure와 JWKS를 재사용하되 workload 전용 HTTPS issuer, exact audience `learning-core-user-withdrawn`, principal `sub=identity-service`, TTL·최대 lifetime `PT2M`, workload verifier skew `PT30S`, `nbf=iat`를 사용한다. 중복 `service` claim은 제거한다.
- 검증 경계: Learning Core는 `typ=JWT`, non-blank `kid`, `nbf` 존재와 `nbf=iat`를 production 활성화 전에 검증하도록 계획했다. Identity issuer는 canonical UUID `jti`를 발급하지만 `jti`는 인가·event 멱등성 필수 입력이 아니며 별도 validator·저장소·replay blacklist·로그·metric·DB key를 만들지 않는다. event 멱등성은 payload `eventId` inbox가 담당한다.
- 전달 오류 정책: 2xx는 성공, 408·425·429·5xx·timeout·connection failure는 재시도, 400·409·413·422는 payload 영구 실패, 401·403·404·405와 예상하지 못한 4xx는 인증·endpoint 배포 설정 오류로 격리·경보한다.
- rotation·E2E: 현재 단일-key JWKS를 production 활성화 전에 다중 key로 확장하고 구키를 최소 `PT31M` overlap 유지한다. HTTPS exact endpoint·redirect 미허용, 사용자 JWT와 workload JWT의 상호 사용 거절, 양 서비스 golden Token staging E2E를 완료 조건으로 반영했다.
- 실행한 테스트와 결과: 문서만 변경했으므로 Gradle 테스트는 실행하지 않았다. `git diff --check`와 turn marker 단일 존재 여부를 검증한다.
- 유지한 계약: 사용자 Access Token의 workload 인증 재사용 금지, 공개 workload Token API·Token cache 금지, consumer 선배포·producer 후활성화, 민감정보·credential 비노출과 payload `eventId` 기반 멱등성을 유지했다.
- 결정사항: 사용자 검토안은 타당해 계약으로 승인했다. workload JWT profile과 Learning Core 오류 계약은 확정됐으며, 사용자 Access JWT deny marker skew·inbox retention·backfill exact 범위는 별도 운영값으로 남긴다.
- 위험 요소: production `WorkloadIdentityCredentialProvider`, 다중 key JWKS와 Learning Core verifier 보완이 아직 구현되지 않았으므로 현재 상태에서 publisher를 활성화하면 안 된다.
- 다음 작업: `TMI-111` 구현 전에 Jira 본문과 승인된 계획서를 대조하고 Identity outbox capture부터 구현한다. Jira를 변경하려면 사용자에게 변경안을 먼저 제시하고 별도 승인을 받는다.

## 2026-08-28 — TMI-111 UserWithdrawn producer 구현

<!-- codex-turn:01a046b2-ac07-7451-a764-52d0f556102c -->

- 날짜: 2026-08-28
- 브랜치: Identity `feat/TMI-111-user-withdrawn-outbox-publisher`
- Jira: `TMI-111`; Atlassian 공식 도구로 제목·Description·상태 `해야 할 일`·Resolution 없음과 완료된 선행 `TMI-109` blocks 관계를 읽었다. Jira 댓글·본문·필드·링크·상태는 변경하지 않았다.
- 작업 목표: withdrawal commit과 `UserWithdrawn` event capture를 원자화하고 승인된 workload JWT로 Learning Core에 at-least-once 전달하며, 기존 WITHDRAWN User의 bounded backfill과 rotation 가능한 JWKS 기반을 구현한다.
- 변경 파일: `UserWithdrawalTransactionService`, 신규 `UserWithdrawnOutbox`·상태·실패 코드·Repository fragment, `domain/user/withdrawalevent`의 wire mapper·publisher·retry·replay·HTTP adapter·scheduler·backfill·configuration, workload JWT provider·properties·configuration, JWKS rotation properties·public key set·controller, `application.yml`, `application-test.yml`, `.env.example`, 관련 테스트와 Stage 5·Codex 상태 문서.
- Transaction 구현: User CAS tombstone, Firebase lifecycle, phone eligibility revocation, 활성 RefreshSession `ACCOUNT_WITHDRAWN` 폐기와 `UserWithdrawnOutbox` 저장을 같은 `mongoTransactionManager` Transaction에 포함했다. production constructor의 outbox Repository는 필수이며 저장 실패 시 withdrawal 전체가 rollback된다.
- outbox·publisher 구현: eventId와 userId canonical UUID, schemaVersion 1, unique userId, due·expired lease·dead-letter review·TTL index를 적용했다. atomic `findAndModify` lease, expired lease 회수, bounded exponential backoff와 jitter, 최대 시도, 2xx publish, 408·425·429·5xx·timeout·connection retry, 400·409·413·422 payload 실패와 401·403·404·405·예상 밖 4xx 배포 설정 오류 dead-letter, manual replay와 published cleanup을 구현했다.
- wire·보안 구현: `POST /internal/v1/events/withdrawn`에 eventId·schemaVersion·userId·withdrawnAt만 전송하며 4 KiB를 제한한다. HTTPS exact endpoint만 허용하고 redirect를 따르지 않는다. Token·credential·userId·eventId·raw payload를 로그·metric tag·오류에 넣지 않는다.
- workload JWT 구현: 기존 Identity `JwtEncoder`와 active signing key를 재사용해 전달마다 RS256 Token을 발급한다. workload HTTPS issuer, audience `learning-core-user-withdrawn`, `sub=identity-service`, `iat`, `nbf=iat`, `exp=iat+PT2M`, UUID `jti`, `typ=JWT`, current `kid`를 사용하며 임의 audience와 비승인 issuer·subject·TTL을 거절한다. 공개 credential endpoint와 Token cache는 만들지 않았다.
- JWKS rotation 구현: current Public Key와 comma-separated previous key ID·public key resource를 함께 표준 JWKS로 노출한다. 중복·active key 재등록·ID/location 개수 불일치를 startup에서 거절하고 Private Key parameter는 응답에 포함하지 않는다. 실제 구키는 최소 `PT31M` overlap 운영이 필요하다.
- backfill·관측 구현: 고정 lower inclusive·upper exclusive 범위, 최대 100건 batch, 기본 dry-run과 결정적 backfill eventId를 구현했다. 이미 outbox가 있는 User는 건너뛰고 unique conflict에 수렴한다. publisher outcome·failure, backlog, dead-letter, oldest pending age, delivery lag와 backfill outcome metric을 추가했다.
- 설정: workload JWT, publisher, backfill은 모두 기본 비활성이다. enabled 상태에서는 workload HTTPS issuer·exact audience·subject·TTL, endpoint exact path, positive duration·attempt·batch와 고정 backfill bounds를 fail-fast 검증한다. Secret, 실제 Token·Key·endpoint·전체 Mongo URI는 추가하지 않았다.
- 테스트: 집중 테스트 19개와 Spring context 회귀를 먼저 실행했다. 첫 전체 테스트는 Mongo auto-configuration이 제외된 integration test context에 신규 Repository mock이 없어 48개 context 실패가 발생했고 해당 네 test context에 mock을 명시해 수정했다. 이후 `./gradlew clean test` 성공, 109개 suite·591개 테스트, 실패 0·오류 0·건너뜀 0이다. `git diff --check`도 통과했다.
- 유지한 계약: 공개 API·BaseResponse·사용자 Access/Refresh Token·JWT `sub=userId`·audience `tosunsaeng-learning-core`, Firebase/Social/Phone lifecycle, Learning Core 독립 저장소, AI `user_id=examId`, S3·Redis 계약을 변경하지 않았다. Git commit·push와 Jira mutation은 수행하지 않았다.
- 결정사항: Learning Core `TMI-109` 코드는 이번 작업에서 수정하지 않았다. Identity producer capture는 항상 수행하지만 외부 전달·backfill은 운영 준비 전까지 꺼 둔다. `jti`는 발급 고유성만 담당하고 event 멱등성은 payload `eventId`와 Learning Core inbox가 담당한다.
- 위험 요소: 실제 replica set Transaction, multi-instance lease 회수, workload JWT golden Token 상호 사용 거절, HTTPS redirect 미허용, duplicate 204, real key overlap과 backfill exact bounds는 제공된 로컬 환경에서 E2E 검증하지 못했다. Learning Core의 `typ`·`kid`·`nbf=iat` 보완 배포 여부도 활성화 전에 재확인해야 한다.
- Jira 댓글 초안: `TMI-111 Identity 구현 완료. withdrawal Transaction과 UserWithdrawnOutbox 원자 저장, workload JWT 기반 publisher·retry·dead-letter·manual replay, bounded dry-run backfill, 다중 Public Key JWKS와 관측 metric을 추가했습니다. 전체 591개 테스트와 git diff --check가 통과했습니다. 실제 replica set·workload auth·multi-instance·duplicate·key overlap staging E2E와 운영 backfill bounds 확정은 남아 있으며 기능은 기본 비활성입니다.` 댓글은 자동 등록하지 않았다.
- 다음 작업: 사용자가 변경을 검토해 직접 commit·push·PR merge한 뒤 staging E2E와 운영값을 확인한다. merge 확인 후 별도 승인받아 Jira 댓글과 완료 전환을 수행한다.

## 2026-08-28 — TMI-111 병합 확인과 Jira 완료 전환안 준비

<!-- codex-turn:01a046f3-fe98-7a10-a336-8f8990a88f85 -->

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: `TMI-111`; Atlassian 공식 도구로 현재 상태 `해야 할 일`, Resolution 없음, 기존 댓글 1건과 완료 transition ID `41`을 읽기 전용 확인했다.
- 작업 목표: 사용자가 알린 PR 병합을 Git 이력에서 검증하고 Jira 종료 댓글·완료 전환의 정확한 변경안을 준비한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 병합 확인: local `develop`과 `origin/develop`이 모두 merge commit `776e8fa195c2ec86c548e0733537900d38d8efd6`을 가리킨다. 해당 commit은 PR #35 `feat/TMI-111-user-withdrawn-outbox-publisher` 병합이며 feature commit `43bb369`을 포함한다. 확인 시 작업 트리는 깨끗했다.
- Jira 현재 상태: 제목은 `[Identity] UserWithdrawn outbox·publisher·backfill 구현`, 상태는 `해야 할 일`, Resolution 없음이다. 선행 `TMI-109`는 완료 상태이며 blocks 관계가 유지돼 있다.
- 제안할 댓글: PR #35와 merge commit, withdrawal Transaction·outbox·publisher·workload JWT·bounded backfill·rotation JWKS 구현 요약, 주요 변경 영역, 전체 109개 suite·591개 테스트와 `git diff --check` 성공, 실제 replica set·workload auth·multi-instance·duplicate·key overlap staging E2E와 운영 backfill bounds가 남았고 기능이 기본 비활성이라는 위험만 기록한다.
- 제안할 상태 변경: 종료 댓글을 한 건 등록한 뒤 transition ID `41`만 적용해 `완료`로 전환한다. 담당자·우선순위·라벨·본문·링크와 기존 댓글은 변경하지 않는다.
- 실행한 테스트와 결과: 새 애플리케이션 변경이 없어 테스트를 재실행하지 않았다. 병합된 구현의 직전 검증은 `./gradlew clean test` 109개 suite·591개 테스트, 실패·오류·건너뜀 0개와 `git diff --check` 성공이다. 이번 문서 변경에도 `git diff --check`를 실행한다.
- 유지한 계약: Git commit·push 금지, Jira mutation 전 변경안 제시·승인, Secret·Token·Key·개인정보 비기록과 production 기능 기본 비활성 계약을 유지했다.
- 결정사항: 병합은 확인됐으므로 Jira 완료 전환의 merge 선결 조건은 충족됐다. 다만 댓글 등록과 상태 전환은 사용자에게 정확한 내용을 제시한 뒤 별도 승인받아 수행한다.
- 위험 요소: Jira를 완료해도 staging E2E와 운영 활성화 승인을 마친 것으로 해석하면 안 된다. publisher·backfill·production withdrawal flag는 계속 비활성 상태를 유지해야 한다.
- 다음 작업: 사용자가 제시한 댓글과 transition ID `41` 적용을 승인하면 댓글 등록, 완료 전환, 상태·Resolution 재조회를 수행하고 그 결과를 다음 WORKLOG·CURRENT_STATE에 기록한다.

## 2026-08-28 — TMI-111 Jira 완료 전환

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: `TMI-111`
- 작업 목표: 사용자가 승인한 종료 댓글을 등록하고 병합된 Identity producer 이슈를 완료로 전환한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- Jira 작업: PR #35 merge, withdrawal Transaction·outbox·publisher·workload JWT·bounded backfill·rotation JWKS 구현, 109개 suite·591개 테스트와 남은 staging 위험을 요약한 댓글 ID `10042`를 등록했다. 이어서 transition ID `41`만 적용했다.
- 최종 상태: 재조회 결과 Jira status ID `10003` 이름 `완료`, Resolution ID `10000` 이름 `완료`다. 댓글 총 2건 중 새 종료 댓글이 저장된 것도 확인했다.
- 승인 여부: 사용자가 제시한 댓글과 상태 전환안을 확인한 뒤 `어`라고 명시적으로 승인했다.
- 실행한 테스트와 결과: Jira mutation과 문서 기록만 수행해 테스트를 재실행하지 않았다. 병합 구현의 직전 결과는 전체 109개 suite·591개 테스트 성공, 실패·오류·건너뜀 0개다. 문서 변경은 `git diff --check`로 검증한다.
- 유지한 계약: 담당자·우선순위·라벨·본문·링크와 기존 댓글은 변경하지 않았고 Secret·Token·Key·개인정보를 Jira나 문서에 기록하지 않았다. Git commit·push도 수행하지 않았다.
- 위험 요소: Jira 완료는 production publisher·backfill 활성화 승인이 아니다. 실제 replica set·workload auth·multi-instance·duplicate·key overlap staging E2E와 운영 backfill bounds는 여전히 남아 있다.
- 다음 작업: 다음 withdrawal lifecycle 항목을 진행하되 production 기능 flag는 운영 검증 전까지 비활성으로 유지한다.

## 2026-08-28 — TMI-111 Jira 종료 기록 최종화

<!-- codex-turn:01a046f6-4ac5-7e62-9b03-e983b0f0e1a0 -->

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: `TMI-111`
- 작업 목표: 현재 turn의 Jira 종료 결과가 WORKLOG marker와 CURRENT_STATE에 빠짐없이 기록되도록 최종화한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 확인 결과: PR #35 merge commit `776e8fa195c2ec86c548e0733537900d38d8efd6`이 `develop`과 `origin/develop`에 반영됐고, Jira 종료 댓글 ID `10042` 등록 뒤 transition ID `41`이 적용됐다. 재조회한 status와 Resolution은 모두 `완료`다.
- 실행한 테스트와 결과: 문서 기록만 추가해 테스트를 재실행하지 않았다. 병합 구현의 최종 검증은 109개 suite·591개 테스트 성공, 실패·오류·건너뜀 0개이며 이번 문서는 `git diff --check`로 검증한다.
- 유지한 계약: Jira 담당자·우선순위·라벨·본문·링크와 기존 댓글은 변경하지 않았으며 Secret·Token·Key·개인정보를 기록하지 않았다. Git commit·push도 수행하지 않았다.
- 위험 요소: Jira 완료와 production 활성화는 별개다. replica set·workload auth·multi-instance·duplicate·key overlap staging E2E와 운영 backfill 범위 확정 전까지 관련 기능을 비활성으로 유지한다.
- 다음 작업: 다음 withdrawal lifecycle 작업을 별도 Jira와 계획서 기준으로 진행한다.

## 2026-08-28 — TMI-111 이후 Stage 6 작업 설명

<!-- codex-turn:01a046fa-2e86-7f52-b062-d714de4f8ac9 -->

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: 현재 진행 중인 Jira 없음. Jira 조회·생성·수정·댓글·상태 변경은 수행하지 않았다.
- 작업 목표: 완료된 withdrawal Stage 1~5 다음의 실제 개발 항목과 가입 중단 cleanup의 사용자·데이터 흐름을 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 확인 결과: 고정 순서의 다음 작업은 Stage 6 가입 중단 Firebase User cleanup이다. 현재 `FirebaseEnrollmentAttempt`는 PENDING·CONSUMED·EXPIRED와 cleanupAt TTL을 가지며 signup·Guest upgrade가 CAS consume하지만, attempt 만료·TTL 삭제는 외부 Firebase User를 disable·revoke·delete하지 않는다.
- 문제 정의: 사용자가 email·SNS 인증과 같은 UID phone link까지 마친 뒤 Mongo finalize 전에 이탈하면 내부 User·FirebaseIdentity·PhoneIdentity가 없더라도 외부 Firebase User와 phone credential 점유가 남을 수 있다. 이 때문에 다른 UID로 같은 번호를 연결하지 못하거나 오래된 미완료 계정이 계속 인증될 수 있다.
- 제안 흐름: resume 유예 기간에는 기존 PENDING attempt를 재사용한다. 유예가 끝난 expired·unconsumed attempt만 별도 cleanup lifecycle/outbox로 atomic claim하고, exact project·UID의 최신 Firebase 상태와 내부 FirebaseIdentity·SocialIdentity·PhoneIdentity·User owner, 새 PENDING/CONSUMED attempt 부재를 다시 확인한 뒤 disable→refresh revoke→delete를 멱등 실행한다.
- 안전 경계: DIRECT_SIGNUP과 GUEST_USER binding을 구분하고 cleanup claim과 enrollment consume을 CAS fencing한다. 기존 ACTIVE·SUSPENDED·WITHDRAWN·MERGED User, complete withdrawal lifecycle, 이미 새 owner에 연결된 UID·provider·phone은 삭제하지 않는다. owner mismatch·mixed state·외부 성공 여부 불명확은 retry 추측 대신 reconciliation으로 보낸다.
- 사용자 흐름: 유예 중에는 가입을 이어서 완료할 수 있다. cleanup이 시작된 뒤에는 기존 enrollment를 재사용하지 않고 가입을 다시 시작하도록 전용 오류를 반환한다. Firebase User 삭제 뒤에는 fresh 인증과 phone verification으로 새 enrollment를 만들며 내부 기존 User를 복구·자동 merge하지 않는다.
- 구현 범위 후보: Stage 6 계획서, cleanup lifecycle Entity·Repository·atomic lease, owner preflight guard, Firebase Admin inspect/disable/revoke/delete adapter 재사용 경계, retry·dead-letter·reconciliation, 기본 비활성 scheduler·metric·runbook, direct signup·Guest upgrade·race·응답 유실·외부 성공 후 내부 실패 테스트다.
- 제외 범위: complete withdrawal Stage 1~5 재구현, Billing Entitlement consumer, logout-all revoke, Provider unlink·phone 변경, ACTIVE 회원 rebind와 Mongo User 삭제·복구는 포함하지 않는다.
- 실행한 테스트와 결과: 코드 변경이 없는 설명·문서 작업이라 테스트를 실행하지 않았다. `git diff --check`로 문서 형식을 검증한다.
- 유지한 계약: Firebase는 credential broker이고 Identity는 canonical User·Session·identity mapping을 소유한다. phone은 자동 merge·기존 User 선택 기준이 아니며 Secret·Token·Firebase UID·phone·provider subject를 로그·metric·작업 기록에 남기지 않는다.
- 위험 요소: Stage 1~5 Jira 완료와 production 준비 완료는 다르다. 실제 Firebase/mobile·Mongo Transaction·workload auth·multi-instance staging E2E 전에는 기존 flag를 켜지 않는다. cleanup과 finalize race를 fencing하지 않으면 정상 가입 직전 Firebase User를 삭제할 수 있다.
- 다음 작업: 사용자가 요청하면 Stage 6 계획서를 작성해 resume grace·상태 전이·오류·완료 조건을 확정한 뒤 Jira 초안을 준비한다.

## 2026-08-28 — Stage 6 enrollment cleanup 유예기간 검토

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: 현재 진행 중인 Jira 없음. Jira mutation은 수행하지 않았다.
- 작업 목표: 가입 중단 Firebase User cleanup의 유예기간이 현재 어떤 값인지와 별도 유예가 필요한 이유를 코드 기준으로 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 현재 값: `FIREBASE_ENROLLMENT_TTL` 기본 `PT10M`은 PENDING attempt를 signup·Guest upgrade Transaction에서 consume할 수 있는 시간이다. `FIREBASE_ENROLLMENT_CLEANUP_RETENTION` 기본 `PT24H`는 `cleanupAt=expiresAt+retention`으로 attempt 문서를 Mongo TTL 삭제 전까지 남기는 시간이며 외부 Firebase User cleanup worker나 삭제 유예는 아직 없다.
- 권장 초안: 최근 enrollment attempt의 만료 시점 뒤 별도 `abandonedCleanupGrace=PT24H`를 기본 후보로 둔다. 10분 attempt가 만료돼도 grace 안에 돌아온 사용자는 exchange에서 새 10분 attempt를 만들고 가입을 재개할 수 있다. cleanup 완료·reconciliation 이력 retention은 grace와 같은 필드를 재사용하지 않고 더 길게 별도 정의한다.
- 필요 이유: Firebase provider·phone link는 Mongo finalize 전에 외부에서 성공할 수 있어 앱 백그라운드·네트워크 단절·SMS 지연·응답 유실만으로도 정상 사용자가 일시적으로 미완료처럼 보인다. 만료 즉시 delete하면 진행 중 finalize 또는 곧 재개할 정상 Firebase User를 삭제할 수 있다.
- 안전 판단: grace는 오판 가능성을 낮추는 시간 완충일 뿐 동시성 안전장치가 아니다. cleanup worker는 exact project·UID의 최신 PENDING/CONSUMED attempt와 FirebaseIdentity·SocialIdentity·PhoneIdentity·User owner를 재조회하고 CAS claim/fencing 뒤에만 외부 mutation을 수행해야 한다.
- 결정 상태: `PT24H`는 합리적인 기본 후보이며 아직 승인된 운영 계약은 아니다. 더 짧게 운영하려면 SMS·OAuth·모바일 이탈 후 복귀 지표와 support 정책이 필요하고, 더 길게 두면 phone credential 점유가 오래 유지되는 tradeoff가 있다.
- 실행한 테스트와 결과: 코드 변경이 없어 테스트를 실행하지 않았다. `git diff --check`로 문서 형식을 검증한다.
- 유지한 계약: 유예기간 경과만으로 기존 내부 User를 삭제·복구·자동 merge하지 않는다. Secret·Token·Firebase UID·phone·provider subject는 기록하지 않는다.
- 위험 요소: 현재 `cleanupRetention=PT24H`를 그대로 grace로 해석하면 TTL 삭제와 cleanup claim이 경쟁할 수 있다. Stage 6에서는 grace와 record retention을 반드시 분리해야 한다.
- 다음 작업: Stage 6 계획서에서 `PT24H` 기본 후보를 승인 또는 조정하고 마지막 활동 기준, 상태 전이, retry·reconciliation과 모바일 restart 오류 계약을 확정한다.

## 2026-08-28 — Stage 6 cleanup 유예기간 기록 최종화

<!-- codex-turn:01a046fc-a185-75a2-82ef-2cacd9a8223a -->

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: 현재 진행 중인 Jira 없음. Jira mutation은 수행하지 않았다.
- 작업 목표: 현재 turn에서 설명한 enrollment TTL·attempt retention·외부 Firebase User cleanup grace의 차이와 권장안을 지정 turn marker로 최종 기록한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 최종 정리: 현재 `PT10M`은 PENDING enrollment consume TTL이고 `PT24H`는 attempt 문서의 Mongo TTL 보존기간이다. 외부 Firebase User 삭제 유예는 아직 구현되지 않았다. Stage 6에서는 별도 `abandonedCleanupGrace=PT24H`를 기본 후보로 두고 terminal cleanup 이력 retention을 분리한다.
- 필요 조건: 유예기간은 앱 중단·SMS 지연·네트워크·응답 유실로 정상 사용자를 고아 계정으로 오인할 가능성을 낮춘다. 삭제 안전성은 grace만으로 보장하지 않으며 최신 attempt·내부 owner preflight와 cleanup/finalize CAS fencing을 함께 적용한다.
- 실행한 테스트와 결과: 문서 기록만 추가해 테스트를 실행하지 않았다. `git diff --check`로 문서 형식을 검증한다.
- 유지한 계약: 기존 내부 User를 cleanup 시간만으로 삭제·복구·자동 merge하지 않으며 Secret·Token·Firebase UID·phone·provider subject를 기록하지 않았다.
- 위험 요소: 현재 `cleanupRetention=PT24H`를 cleanup grace로 재사용하면 Mongo TTL 삭제와 worker claim이 경쟁할 수 있다.
- 다음 작업: Stage 6 계획서에서 grace·record retention·마지막 활동 기준·상태 전이·오류와 운영 조정 기준을 확정한다.

## 2026-08-28 — 회원탈퇴 Firebase User 삭제 시점 정정

<!-- codex-turn:01a04702-2713-7cb0-8691-4e154e0597c8 -->

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: 현재 진행 중인 Jira 없음. Jira 조회·생성·수정·댓글·상태 변경은 수행하지 않았다.
- 작업 목표: 회원탈퇴 Firebase User cleanup에 유예기간이 있다는 잘못된 설명을 현재 구현 기준으로 정정한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 확인 결과: `UserWithdrawalLifecycle`은 생성 시 `EXTERNAL_CLEANUP_PENDING`, `nextAttemptAt=requestedAt`으로 저장되어 회원탈퇴 commit 직후 바로 처리 가능하다. worker가 활성화돼 있으면 기본 `PT5S` scheduler 주기에 claim하고 target guard→Firebase inspect→disable→refresh token revoke→provider deletion obligation→delete→presence 확인 순서로 처리한다.
- 시간 계약: 회원탈퇴 Firebase User 삭제에는 별도 grace가 없다. 기본 `PT1M`은 worker lease이고, 최초 `PT5S`·최대 `PT1H` backoff와 최대 12회는 실패 재시도 정책이지 정상 삭제 유예가 아니다.
- 정정 사항: 앞서 언급한 `PT24H` grace 후보는 회원가입을 끝내지 않은 사용자의 고아 Firebase User를 정리하는 Stage 6 가입 중단 cleanup에만 해당한다. 현재 `FIREBASE_ENROLLMENT_CLEANUP_RETENTION=PT24H`도 enrollment attempt 문서의 Mongo TTL 보존기간이며 회원탈퇴 lifecycle과 무관하다.
- 실행한 테스트와 결과: 코드 변경이 없는 설명·문서 정정이라 테스트를 실행하지 않았다. `git diff --check`로 문서 형식을 검증한다.
- 유지한 계약: 회원탈퇴 대상은 exact project·UID와 중앙 target guard를 확인한 뒤 Firebase에서 비동기 삭제하며, Firebase UID·Token·provider subject·개인정보와 Secret을 기록하지 않는다. Git commit·push도 수행하지 않았다.
- 결정사항: 회원탈퇴는 복구 전제의 soft-delete 유예를 두지 않고 가능한 빨리 외부 Firebase User를 삭제한다. Stage 6의 가입 중단 grace와 명칭·설정·상태를 분리한다.
- 위험 요소: cleanup worker 기본 설정은 비활성이다. production에서 flag가 꺼져 있거나 worker가 장애 상태면 lifecycle은 즉시 due여도 실제 삭제는 지연될 수 있다. 실패 retry와 reconciliation 대상은 정상 grace로 해석하지 않는다.
- 다음 작업: Stage 6 계획서를 작성할 때 가입 중단 Firebase User에만 적용할 grace와 cleanup lifecycle을 별도로 확정한다.

## 2026-08-28 — 완료된 회원탈퇴 cleanup과 다음 Stage 6 범위 구분

<!-- codex-turn:01a04704-ee48-7a63-8b37-47f54630cc6a -->

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: 현재 진행 중인 Jira 없음. Jira mutation은 수행하지 않았다.
- 작업 목표: 기존 회원탈퇴 User cleanup이 이미 구현됐는지 확인하고 다음 작업의 대상을 명확히 구분한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 확인 결과: 회원탈퇴 시 내부 User는 삭제하지 않고 개인정보가 제거된 `WITHDRAWN` tombstone으로 보존한다. 모든 RefreshSession을 `ACCOUNT_WITHDRAWN`으로 폐기하고, 외부 cleanup worker가 Firebase User를 실제 삭제한 뒤 identity release Transaction이 exact FirebaseIdentity와 해당 User의 SocialIdentity를 삭제하며 PhoneIdentity·fingerprint alias를 release하고 활성 혜택 binding을 revoke한다. lifecycle은 최종 `CLEANED`로 수렴한다.
- 다음 범위: Stage 6은 이미 가입한 회원의 탈퇴 cleanup을 재구현하지 않는다. Firebase 인증·phone link 이후 Mongo signup/Guest upgrade finalize 전에 앱을 종료하거나 요청이 유실되어 내부 canonical User가 생성되지 않은 가입 중단 Firebase User만 별도 grace·owner preflight·CAS fencing을 거쳐 정리한다.
- 실행한 테스트와 결과: 코드 변경이 없는 확인·설명이라 테스트를 실행하지 않았다. `git diff --check`로 문서 형식을 검증한다.
- 유지한 계약: 내부 User tombstone은 감사·재가입 gate를 위해 보존하며 회원탈퇴 Firebase User와 가입 중단 고아 Firebase User lifecycle을 혼용하지 않는다. Secret·Token·Firebase UID·phone·provider subject는 기록하지 않았고 Git commit·push도 수행하지 않았다.
- 결정사항: 회원탈퇴 cleanup은 완료된 기반으로 취급하고, 다음 계획서는 가입이 완료되지 않아 User owner가 없는 외부 Firebase 계정 cleanup에 한정한다.
- 위험 요소: 두 경로 모두 Firebase User를 실제 삭제할 수 있지만 삭제 전제와 기준점이 다르다. Stage 6에서 withdrawal lifecycle을 재사용하면 정상 가입 finalize와 경합하거나 내부 owner가 있는 계정을 잘못 삭제할 수 있다.
- 다음 작업: Stage 6 계획서에서 고아 계정 판정, grace 기준, finalize 경합 차단, retry·reconciliation과 운영 활성화 조건을 확정한다.

## 2026-08-28 — 회원탈퇴와 가입 중단 cleanup 범위 재확인

<!-- codex-turn:01a04704-ee48-7663-a19f-13cf3256353b -->

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: 현재 진행 중인 Jira 없음. Jira 조회·수정·상태 변경은 수행하지 않았다.
- 작업 목표: 사용자의 확인 질문에 맞춰 이미 구현된 회원탈퇴 User cleanup과 다음 Stage 6 가입 중단 Firebase User cleanup의 차이를 명확히 한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·설정·테스트·계약 코드는 변경하지 않았다.
- 구현 확인: 가입 완료 회원의 탈퇴 경로는 내부 User를 개인정보가 제거된 `WITHDRAWN` tombstone으로 보존하고 RefreshSession을 폐기한다. 외부 Firebase User 실제 삭제가 확인되면 exact FirebaseIdentity와 User 소유 SocialIdentity를 삭제하고 PhoneIdentity·fingerprint alias·혜택 binding을 release/revoke한 뒤 lifecycle을 `CLEANED`로 전환한다.
- 다음 작업: Stage 6은 회원탈퇴 정리를 다시 구현하는 작업이 아니다. Firebase 인증 또는 phone link 뒤 내부 User 생성 Transaction을 완료하지 못해 외부 Firebase에만 남은 가입 중단 계정을 별도 grace와 owner preflight·CAS fencing으로 정리한다.
- 실행한 테스트와 결과: 코드 변경이 없는 범위 확인과 문서 기록이므로 테스트는 실행하지 않았다. 문서는 `git diff --check`로 검증한다.
- 유지한 계약: 내부 User tombstone 보존, Firebase credential broker 경계, phone 비자동병합, Secret·Token·Firebase UID·phone·provider subject 비기록과 Git commit·push 금지를 유지했다.
- 결정사항: 기존 withdrawal cleanup은 완료된 기반으로 취급하며 후속 계획과 구현은 가입 미완료 고아 Firebase User cleanup으로 한정한다.
- 위험 요소: withdrawal과 abandoned enrollment는 외부 Firebase User 삭제라는 결과가 같아도 내부 owner 존재 여부와 삭제 안전 조건이 다르므로 lifecycle과 설정을 공유하면 안 된다.
- 다음 작업: Stage 6 계획서에서 가입 중단 판정 시점, grace, 재개 흐름, finalize race, retry·reconciliation과 운영 활성화 조건을 확정한다.

## 2026-08-28 — Stage 6 가입 중단 Firebase User cleanup 계획서 작성

<!-- codex-turn:01a04706-bbe6-7643-9366-03fe70abd9b6 -->

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: 현재 진행 중인 Jira 없음. Jira 조회·생성·수정·댓글·상태 변경은 수행하지 않았다.
- 작업 목표: 고정 구현 순서의 다음 항목인 Stage 6 가입 중단 Firebase User cleanup의 구현·동시성·운영 계획을 저장소 문서로 확정한다.
- 변경 파일: `docs/contracts/firebase-abandoned-enrollment-cleanup-stage-6-plan.md`, `docs/contracts/firebase-auth-follow-up-implementation-order.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 계획: exact Firebase project·UID 단위의 `AbandonedFirebaseEnrollmentCleanup` lifecycle을 withdrawal과 분리한다. RESUMABLE·CLEANUP_IN_PROGRESS·RETRY_WAIT·FINALIZED·CLEANED·RECONCILIATION_REQUIRED 상태, target unique index, generation·version·claim별 lease token으로 가입 재개·finalize·cleanup 경쟁의 단일 승자를 만든다.
- 시간 계약: 기존 enrollment TTL `PT10M`, 신규 resume grace `PT24H`, lifecycle·attempt record retention `P30D`를 서로 다른 설정과 필드로 둔다. grace는 마지막 attempt 만료 시각부터 계산하고 fresh Firebase proof로 실제 재개할 때만 갱신한다.
- 안전 경계: DIRECT_SIGNUP은 canonical owner 부재를 요구하고 GUEST_USER는 bound User가 여전히 ACTIVE GUEST인지 추가 확인한다. FirebaseIdentity, linked SocialIdentity, verified phone ACTIVE alias, CONSUMED attempt, FINALIZED 또는 새 generation이 하나라도 있으면 자동 삭제하지 않는다.
- 외부 처리: exact account inspect 뒤 disable→refresh token revoke→provider deletion obligation→delete→absence confirm을 멱등 실행한다. Firebase SDK mutation primitive는 공통화할 수 있지만 withdrawal lifecycle·guard·failure 상태를 재사용하지 않는다.
- 사용자 계약: cleanup claim 이후 이전 enrollment는 `409 FIREBASE_ENROLLMENT_RESTART_REQUIRED`로 종료하고 모바일은 enrollment 임시 상태와 Firebase client session을 정리한 뒤 fresh 인증부터 다시 시작한다. 기존 Guest Session은 유지한다.
- 운영 계획: 신규 기능 기본 비활성, dry-run·고정 cutover·최대 100건 bounded legacy capture, lifecycle capture→모바일 계약→staging worker→실제 Firebase·Mongo replica set·multi-instance E2E→production 별도 승인 순서를 적용한다.
- 실행한 테스트와 결과: 문서 작업이므로 Gradle 테스트는 실행하지 않았다. 링크·필수 계약과 `git diff --check`를 검증한다.
- 유지한 계약: 내부 User hard delete·phone 자동 merge·Billing 소유 혜택 구현은 제외했다. 기존 entity FQCN, 공개 응답 shape, Firebase credential broker 경계, Secret·Token·Firebase UID·phone·provider subject 비기록과 Git commit·push 금지를 유지했다.
- 결정사항: Stage 6은 별도 lifecycle을 사용하고 attempt TTL을 cleanup 동시성 수단으로 사용하지 않는다. 가입 완료 withdrawal cleanup은 재구현하지 않는다.
- 위험 요소: 실제 Firebase inspect에서 linked provider·phone owner를 안전하게 확인할 ephemeral snapshot 경계, 기존 attempt TTL 이전 데이터 손실, 모바일 restart handling, Firebase provider obligation과 Mongo finalize 경합은 staging에서 반드시 검증해야 한다.
- 다음 작업: 사용자 검토 후 계획서를 Jira 구현 이슈 본문과 완료 조건으로 축약해 생성 승인을 받는다.

## 2026-08-28 — Stage 6 cleanup record retention 목적 검토

<!-- codex-turn:01a04724-9d09-7823-b5f7-2b26c027ddfb -->

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: 현재 진행 중인 Jira 없음. Jira mutation은 수행하지 않았다.
- 작업 목표: Stage 6 계획서의 `cleanupLifecycleRetention=P30D`와 `FirebaseEnrollmentAttempt.cleanupAt` 30일 보존이 필요한 이유와 안전한 TTL 적용 범위를 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·계획서·설정·테스트 코드는 변경하지 않았다.
- 확인 결과: lifecycle record는 cleanup 진행 중 generation·lease·retry·owner guard를 조정하는 필수 상태다. terminal 뒤 보존은 늦은 요청의 결과 분류, worker·Mongo 부분 실패 조사, bounded legacy capture 중복 방지와 운영 지표 확인을 위한 임시 이력이며 Firebase 삭제를 30일 미루는 grace가 아니다.
- 판단: `P30D`는 기능상 필수값이 아니라 운영 기본 후보다. active 상태인 RESUMABLE·CLEANUP_IN_PROGRESS·RETRY_WAIT에 절대 TTL을 걸면 미완료 작업이 사라질 수 있으므로 `cleanupAt`을 두지 않아야 한다. FINALIZED·CLEANED에만 terminal 시각 기준 TTL을 설정하고 RECONCILIATION_REQUIRED는 운영 해결 전 자동 삭제하지 않는 것이 안전하다.
- 추가 발견: lifecycle과 source attempt를 모두 동일한 30일에 삭제하면 Mongo TTL 지연 순서에 따라 lifecycle이 먼저 사라지고 attempt만 남아 bounded capture가 cleanup을 다시 만들 수 있다. source attempt는 lifecycle보다 먼저 삭제하거나 terminal lifecycle을 확인한 뒤 삭제해야 한다.
- 실행한 테스트와 결과: 코드 변경이 없는 문서 검토라 Gradle 테스트를 실행하지 않았다. `git diff --check`로 기록 형식을 검증한다.
- 유지한 계약: cleanup retention은 enrollment grace·인증 허용 시간·개인정보 보존 정책과 분리하고 Secret·Token·Firebase UID·phone·provider subject를 기록하지 않았다. Git commit·push도 수행하지 않았다.
- 결정사항: 30일의 필요성은 운영 요구로 별도 승인해야 하며 현재 계획서의 동일 P30D 두 값은 확정 계약으로 취급하지 않는다.
- 위험 요소: unresolved reconciliation을 TTL로 자동 삭제하거나 source attempt보다 lifecycle을 먼저 삭제하면 owner fencing·중복 방지 증거가 사라질 수 있다.
- 다음 작업: 사용자 결정에 따라 plan을 `terminal lifecycle retention`, `source attempt retention`, `reconciliation no-TTL`로 구분하고 구체 기간을 조정한다.

## 2026-08-28 — Stage 6 cleanup TTL 계약 수정

<!-- codex-turn:01a04726-ff4e-76e1-8e8c-3bad26b6df10 -->

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: 현재 진행 중인 Jira 없음. Jira mutation은 수행하지 않았다.
- 작업 목표: 사용자 요청에 따라 Stage 6 계획서의 lifecycle·attempt 동일 30일 보존과 모호한 `cleanupAt` 계약을 상태별 안전한 TTL 정책으로 수정한다.
- 변경 파일: `docs/contracts/firebase-abandoned-enrollment-cleanup-stage-6-plan.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 변경 내용: `cleanupLifecycleRetention=P30D`, `enrollmentRecordRetention=P30D`와 “최소 30일” 표현을 제거했다. 신규 기본값은 `terminalLifecycleRetention=P7D`, `terminalEnrollmentAttemptRetention=PT24H`이며 Firebase 삭제 grace `PT24H`와 별개다.
- 상태별 TTL: RESUMABLE·CLEANUP_IN_PROGRESS·RETRY_WAIT에는 cleanupAt을 설정하지 않는다. RECONCILIATION_REQUIRED도 운영 해결 전 자동 삭제하지 않는다. FINALIZED·CLEANED 전환 시에만 terminalAt 기준 lifecycle·attempt cleanupAt을 설정한다.
- 삭제 순서: exact target의 source attempt는 terminal 뒤 24시간, lifecycle은 7일 보존해 attempt가 먼저 제거되도록 했다. lifecycle이 더 오래 남아 늦은 요청 분류와 bounded capture 중복 방지를 담당한다.
- legacy 보완: 기존 nonterminal attempt를 capture할 때 lifecycle 생성과 같은 Transaction에서 과거 cleanupAt을 제거하고 terminal 전환 시 새 cleanupAt을 설정하도록 계획했다. 이미 TTL 삭제된 source는 자동 추정하지 않는다.
- 실행한 테스트와 결과: 문서 변경이므로 Gradle 테스트를 실행하지 않았다. 이전 P30D·30일 표현 제거, 설정·상태·테스트·완료 조건 일치와 `git diff --check`를 검증한다.
- 유지한 계약: enrollment TTL과 grace, terminal record retention을 분리하고 미해결 상태를 TTL로 삭제하지 않는다. Secret·Token·Firebase UID·phone·provider subject를 기록하지 않았고 Git commit·push도 수행하지 않았다.
- 결정사항: 7일·24시간은 기능상 최소값이나 법적 보존기간이 아닌 조정 가능한 운영 기본값이다. lifecycle retention은 항상 source attempt retention보다 길어야 한다.
- 위험 요소: Mongo TTL 삭제는 지연될 수 있으므로 요청·worker의 안전 판단은 cleanupAt이나 실제 삭제 여부가 아니라 lifecycle status·generation·owner guard를 기준으로 해야 한다.
- 다음 작업: 수정된 계획서를 사용자 검토 후 Jira 구현 이슈의 본문과 완료 조건으로 축약한다.

## 2026-08-28 — Stage 6 상태별 cleanupAt 의미 설명

<!-- codex-turn:01a0477c-53a8-7ac3-a350-d376663b4b66 -->

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: 현재 진행 중인 Jira 없음. Jira mutation은 수행하지 않았다.
- 작업 목표: Stage 6 계획서의 상태별 lifecycle·attempt `cleanupAt` 규칙과 terminalAt 기준을 사용자가 이해할 수 있도록 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·계획서·설정·테스트 코드는 변경하지 않았다.
- 핵심 의미: `cleanupAt`은 Firebase User 삭제를 시작하는 due time이 아니라 Mongo TTL이 해당 coordination record를 물리적으로 제거할 수 있는 시각이다. Firebase cleanup due는 `graceUntil`·`nextAttemptAt`·lease 상태가 결정한다.
- nonterminal 규칙: RESUMABLE은 가입 재개와 cleanup claim 경쟁, CLEANUP_IN_PROGRESS는 worker lease·generation fencing, RETRY_WAIT는 다음 retry를 위해 필요하므로 lifecycle과 관련 attempt에 cleanupAt을 두지 않는다.
- terminal 규칙: FINALIZED는 가입 완료, CLEANED는 Firebase 부재 확인 완료 상태다. terminalAt에 source attempt cleanupAt을 24시간 뒤, lifecycle cleanupAt을 7일 뒤로 설정해 source proof를 먼저 제거하고 outcome·generation·bounded capture 중복 방지 record를 더 오래 보존한다.
- reconciliation 규칙: RECONCILIATION_REQUIRED는 owner mismatch·partial state·결과 불명처럼 자동 판단이 끝나지 않은 상태이므로 자동 TTL을 금지한다. 운영 해결로 FINALIZED·CLEANED가 된 실제 해결 시각부터 각각의 retention을 계산한다.
- 실행한 테스트와 결과: 코드·계획서 변경이 없는 설명 기록이므로 Gradle 테스트를 실행하지 않았다. `git diff --check`로 문서 형식을 검증한다.
- 유지한 계약: TTL 삭제 지연은 허용하되 요청·worker의 안전 판단은 cleanupAt 또는 문서 물리 삭제 여부가 아니라 lifecycle status·generation·lease·owner guard를 기준으로 한다. Secret·Token·Firebase UID·phone·provider subject를 기록하지 않았고 Git commit·push도 수행하지 않았다.
- 결정사항: P7D는 7일, PT24H는 24시간이며 두 값은 terminal record retention이다. 가입 재개 grace `PT24H`와 이름·기준 시각·역할이 다르다.
- 위험 요소: cleanupAt을 Firebase 삭제 시각으로 오해하거나 nonterminal·reconciliation에 TTL을 설정하면 진행 중 작업과 안전 gate가 사라질 수 있다.
- 다음 작업: 사용자가 계획을 승인하면 Stage 6 Jira 초안을 작성한다.

## 2026-08-28 — Stage 6 Jira 생성 초안 및 승인 대기

<!-- codex-turn:01a04790-7731-7730-b7ba-fa5e5321abbf -->

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: 생성 후보는 `[Identity] 가입 중단 Firebase User cleanup lifecycle 구현`. 아직 이슈를 생성하지 않았다.
- 작업 목표: Stage 6 계획서를 TMI Jira 구현 이슈로 축약하고, Jira mutation 전 사용자에게 정확한 생성 내용을 제시한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·계획서·설정·테스트 코드는 변경하지 않았다.
- Jira 읽기 확인: Atlassian 공식 도구로 cloud 접근, TMI 프로젝트 이슈 유형, `TMI-111` 형식과 최근 이슈를 조회했다. 사용할 유형은 `작업` ID `10003`, 우선순위는 High ID `2`이며 최근 생성 키는 `TMI-113`이다. 신규 키는 Jira가 생성 시 결정한다.
- 중복 확인: TMI에서 가입 중단, Firebase User cleanup, abandoned cleanup 관련 summary·text를 검색했고 동일 범위 이슈는 0건이었다.
- 제안 본문: 목적, 선행 `TMI-111`과 계획서 경로, PT10M enrollment TTL·PT24H grace·terminal lifecycle P7D·attempt PT24H retention, 별도 lifecycle 상태, startOrReuse·finalize·cleanup 단일 승자 fencing, DIRECT_SIGNUP·GUEST_USER owner preflight, Firebase inspect·disable·revoke·provider obligation·delete·presence 확인, retry·reconciliation, restart-required 모바일 계약, bounded legacy capture, 기본 비활성과 staging E2E를 포함한다.
- 제외 범위: withdrawal 재구현, 내부 User hard delete·자동 merge, Billing consumer, logout-all revoke, provider unlink·전화번호 변경, Guest 응답 유실과 ACTIVE rebind를 포함하지 않는다.
- 제안 mutation: TMI 프로젝트에 담당자·라벨·컴포넌트 없이 `작업`, High 우선순위로 이슈 한 건만 생성한다. 댓글·상태 변경·이슈 링크는 이번 생성에 포함하지 않는다.
- 승인 여부: 사용자가 Jira 생성을 요청했지만 저장소 규칙에 따라 구체적인 제목·본문·완료 조건을 먼저 제시하고 최종 승인을 기다린다. 아직 mutation 승인은 완료되지 않았다.
- 실행한 테스트와 결과: Jira 읽기·문서 기록만 수행해 Gradle 테스트를 실행하지 않았다. `git diff --check`로 문서 형식을 검증한다.
- 유지한 계약: Jira에 Secret·Token·Firebase UID·phone·provider subject·개인정보를 포함하지 않고 Git commit·push를 수행하지 않았다.
- 위험 요소: 코드 병합만으로 production cleanup을 활성화하면 안 된다. 실제 Firebase/mobile·Mongo replica set·multi-instance lease·finalize race와 provider obligation staging E2E가 완료 조건이다.
- 다음 작업: 사용자가 제시한 Jira 생성안을 승인하면 이슈 한 건을 생성하고 key·상태·priority·description 저장 여부를 재조회한 뒤 WORKLOG·CURRENT_STATE에 기록한다.

## 2026-08-28 — TMI-114 Stage 6 Jira 생성

<!-- codex-turn:01a04790-7731-7730-b7ba-fa5e5321abbf -->

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: `TMI-114`
- 작업 목표: 사용자가 최종 승인한 Stage 6 가입 중단 Firebase User cleanup 구현 이슈를 TMI Jira에 생성하고 저장 결과를 검증한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·계획서·설정·테스트 코드는 변경하지 않았다.
- Jira 작업: Atlassian 공식 도구로 `[Identity] 가입 중단 Firebase User cleanup lifecycle 구현` 이슈 한 건을 TMI 프로젝트에 생성했다. 유형은 `작업`, 우선순위는 High이며 새 key는 `TMI-114`, issue ID는 `10145`다.
- 본문 저장: 목적, 선행 `TMI-111`, 계획서 경로, PT10M enrollment TTL·PT24H grace·P7D terminal lifecycle·PT24H source attempt retention, 상태·fencing·owner preflight·Firebase cleanup·restart-required·bounded capture·완료 조건과 rollout이 승인안대로 저장됐다.
- 재조회 결과: 상태 `해야 할 일`, Resolution 없음, 담당자 없음, 라벨·컴포넌트 빈 목록, 댓글 0건이다. 제목·유형·High 우선순위와 본문을 확인했다.
- 승인 여부: 구체적인 Jira 제목·본문·완료 조건·변경하지 않을 필드를 먼저 제시했고 사용자가 `어`라고 최종 승인한 뒤 생성했다.
- 추가 mutation: 댓글, 상태 전환, 이슈 링크, 담당자·라벨·컴포넌트 변경은 수행하지 않았다.
- 실행한 테스트와 결과: Jira 생성·조회와 문서 기록만 수행해 Gradle 테스트를 실행하지 않았다. `git diff --check`로 문서 형식을 검증한다.
- 유지한 계약: Jira에 Secret·Token·Firebase UID·phone·provider subject·개인정보를 기록하지 않았고 Git commit·push도 수행하지 않았다.
- 위험 요소: Jira 생성은 production cleanup 활성화 승인이 아니다. 실제 Firebase/mobile·Mongo replica set·multi-instance lease·finalize race·provider obligation staging E2E와 runbook 준비 전에는 worker를 비활성으로 유지한다.
- 다음 작업: `TMI-114` 브랜치에서 Jira와 Stage 6 계획서를 기준으로 lifecycle Entity·index·atomic Repository부터 구현한다.

## 2026-08-28 — TMI-114 Jira 생성 기록 최종화

<!-- codex-turn:01a04792-0a0b-7703-9fde-5ceda2350331 -->

- 날짜: 2026-08-28
- 브랜치: Identity `develop`
- Jira: `TMI-114`
- 작업 목표: 현재 승인·생성 turn의 Jira 결과가 지정 marker와 CURRENT_STATE에 빠짐없이 기록되도록 최종화한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·계획서·설정·테스트 코드는 변경하지 않았다.
- Jira 결과: 사용자 승인 뒤 `[Identity] 가입 중단 Firebase User cleanup lifecycle 구현`을 TMI `작업`, High 우선순위로 생성했다. 재조회 결과 상태 `해야 할 일`, Resolution 없음, 담당자 없음, 라벨·컴포넌트 빈 목록, 댓글 0건이다.
- 저장 확인: 계획서 경로, 시간·상태·fencing·owner preflight·Firebase cleanup·restart-required·bounded capture·완료 조건과 rollout이 승인안대로 저장됐다.
- 승인 여부: 구체적인 생성 내용을 먼저 제시했고 사용자가 `어`라고 승인한 뒤 mutation했다.
- 추가 mutation: 댓글·상태 전환·이슈 링크와 담당자·라벨·컴포넌트 변경은 수행하지 않았다.
- 실행한 테스트와 결과: Jira 생성·재조회와 문서 기록만 수행해 Gradle 테스트를 실행하지 않았다. `git diff --check`로 검증한다.
- 유지한 계약: Jira와 문서에 Secret·Token·Firebase UID·phone·provider subject·개인정보를 기록하지 않았고 Git commit·push도 수행하지 않았다.
- 위험 요소: Jira 생성은 production worker 활성화 승인이 아니며 실제 Firebase/mobile·Mongo replica set·multi-instance staging E2E 전에는 비활성을 유지한다.
- 다음 작업: `TMI-114` 구현 브랜치에서 Jira와 Stage 6 계획서를 읽고 lifecycle Entity·index·atomic Repository부터 구현한다.

## 2026-08-28 — TMI-114 가입 중단 Firebase User cleanup lifecycle 구현

<!-- codex-turn:01a0482d-4817-7ed3-92d5-f355ee83e0db -->

- 날짜: 2026-08-28
- 브랜치: Identity `feat/TMI-114-abandoned-enrollment-cleanup`
- Jira: `TMI-114`
- 작업 목표: Firebase 인증·phone/provider link 뒤 signup 또는 Guest upgrade finalize 전에 중단된 외부 Firebase User를 가입 재개 grace와 owner preflight·CAS fencing을 거쳐 안전하게 정리한다.
- 변경 파일: `AbandonedFirebaseEnrollmentCleanup` Entity·상태·failure enum, cleanup·attempt Repository custom 구현, enrollment coordinator·lifecycle·capture·owner guard·worker·retry·terminal Transaction service, Firebase SDK cleanup adapter, capture/worker configuration·properties·scheduler, `FirebaseEnrollmentAttempt`, signup·Guest upgrade Transaction, Firebase authentication configuration, 공개 오류·OpenAPI 설명, `application.yml`, 관련 domain·application·configuration 테스트와 Stage 6 계약·상태 문서.
- 구현 내용: exact Firebase project·UID target unique lifecycle과 RESUMABLE→CLEANUP_IN_PROGRESS/FINALIZED, RETRY_WAIT, CLEANED, RECONCILIATION_REQUIRED 상태를 구현했다. enrollment 재개 시 generation과 grace를 갱신하고 cleanup claim·lease·version과 finalize CAS가 단일 승자를 갖는다. cleanup이 claim됐거나 terminal/reconciliation 상태면 이전 proof를 `409 FIREBASE_ENROLLMENT_RESTART_REQUIRED`로 종료한다.
- cleanup 처리: 최신 source attempt가 만료·미소비인지, 새 active/consumed attempt가 없는지, exact FirebaseIdentity·linked SocialIdentity·verified phone ACTIVE alias owner가 없는지 확인한다. GUEST_USER는 bound User가 계속 `ACTIVE GUEST`인지 추가 확인한다. 통과한 target만 Firebase inspect→disable→refresh token revoke→provider deletion obligation→delete→absence confirmation 순서로 처리하며 외부 계정 부재가 확인된 경우에만 CLEANED로 전환한다.
- TTL·capture: nonterminal·reconciliation lifecycle과 attempt에는 cleanupAt을 두지 않는다. FINALIZED·CLEANED 시 lifecycle은 terminalAt+P7D, target attempt는 terminalAt+PT24H로 설정한다. 기존 attempt는 고정 lower/upper cutover, 한 번에 최대 100건, dry-run 기본으로 capture하며 lifecycle 생성과 과거 cleanupAt 제거를 같은 Mongo Transaction에 묶었다.
- 설정: `app.firebase-abandoned-cleanup` capture·worker는 모두 기본 false다. grace PT24H, lease PT1M, 최대 12회, backoff PT5S~PT1H, worker batch 20, capture batch 최대 100을 환경변수로 조정할 수 있다. capture 활성화에는 고정 lower/upper bound가 필수다.
- 실행한 테스트와 결과: 집중 domain·coordinator·properties·worker 테스트를 추가했고 `./gradlew clean test`가 성공했다. 전체 113개 suite·600개 테스트, 실패·오류·건너뜀 0개다. `git diff --check`도 통과했다.
- 유지한 계약: 내부 User hard delete·phone 기반 자동 merge·withdrawal lifecycle 재사용·Billing 구현은 추가하지 않았다. 기존 공개 응답 shape, Firebase credential broker, Guest Session 유지, Refresh Token hash 저장, Secret·Token·Firebase UID·phone·provider subject 비기록을 유지했다. Git commit·push는 수행하지 않았다.
- 결정사항: withdrawal cleanup과 abandoned enrollment cleanup은 Entity·상태·failure code를 분리하고 Firebase SDK의 안전한 primitive만 같은 패턴으로 구현한다. owner 또는 mixed state가 발견되면 성공으로 숨기거나 자동 삭제하지 않고 RECONCILIATION_REQUIRED에 남긴다.
- 위험 요소: Apple provider deletion obligation은 자동 완료 구현이 없어 해당 account를 reconciliation으로 보낸다. 실제 Firebase/mobile, Mongo replica set Transaction, multi-instance claim/finalize 경쟁, legacy capture 결과와 provider obligation runbook을 staging에서 검증하기 전에는 production flag를 활성화하면 안 된다.
- Jira 작업: 구현 전 Jira `TMI-114`를 읽어 범위 기준으로 사용했다. 이번 turn에는 Jira 댓글·상태·본문을 변경하지 않았고 승인도 요청하지 않았다. 완료 댓글 초안만 handoff에 제공한다.
- 다음 작업: 사용자가 commit·push·PR merge를 완료하면 병합을 확인하고, 별도 승인 뒤 Jira 완료 댓글 등록과 상태 전환을 수행한다.

## 2026-08-31 — 다음 작업 Stage 7 Billing eligibility 운영 연동 설명

<!-- codex-turn:01a05650-b4d4-75f1-aece-987a2ec2f5f4 -->

- 날짜: 2026-08-31
- 브랜치: Identity `develop`
- Jira: `TMI-114`를 읽기 전용으로 재조회했다. PR #36 병합 댓글 ID `10043`이 등록됐고 상태·Resolution 모두 `완료`다. Jira 생성·수정·댓글·상태 mutation은 수행하지 않았다.
- 작업 목표: 고정 구현 순서 7단계 `Billing 최소 Entitlement consumer 배포`의 현재 완료분과 실제 남은 작업을 저장소·계약·Billing 구현에 대조해 설명한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·계약·테스트 코드는 변경하지 않았다. Billing 저장소의 기존 사용자 변경도 수정하지 않았다.
- 확인 결과: Billing `TMI-110` consumer 애플리케이션은 이미 구현돼 있다. exact endpoint는 `POST /internal/v1/eligibility/trial/events`이며 strict schema v1, 16 KiB 제한, canonical digest, `inbound_event_inbox`와 `trial_eligibility` Mongo Transaction, duplicate·stale·revision·eventId conflict 처리를 제공한다.
- 실제 잔여 범위: Billing의 transaction 가능한 Mongo replica set·index capability와 VPC Lattice AWS_IAM ingress를 Identity보다 먼저 staging에 배포한다. Identity ECS task role만 exact POST route를 호출하도록 IAM을 제한하고 unsigned·wrong role·wrong environment·direct bypass를 거절한다.
- Identity 보완: 현재 `JdkPhoneEligibilityBindingDeliveryAdapter`는 audience 기반 Bearer workload credential과 HTTP status만 사용하므로 목표 계약과 다르다. ECS task role 임시 credential로 SigV4 service `vpc-lattice-svcs`, region `ap-northeast-2`에 서명하고, redirect 금지·동일 eventId/payload retry를 유지하면서 429·503의 제한된 `Retry-After`와 eligibility 409 `EVENT_ID_CONFLICT` 영구 격리를 반영해야 한다.
- 활성화·E2E: Billing consumer·인프라 선배포 → verified/revoked·duplicate·stale·conflict·timeout/503·권한 거절 E2E → Identity publisher staging 활성화 → outbox backlog·dead-letter 관찰 → 별도 승인 후 production 순서다. `PHONE_ELIGIBILITY_PUBLISHER_ENABLED`는 그전까지 기본 `false`를 유지한다.
- 도메인 계약: eligibility event 수신은 무료권 지급이 아니다. Billing은 `TrialEligibility` projection만 갱신하고 실제 `TrialClaim`과 1-unit Grant는 최초 reserve Transaction에서 lazy 생성한다. REVOKED는 현재 binding을 끊지만 기존 Claim·소비 이력을 삭제하거나 무료권을 복원하지 않는다. Identity에 임시 무료혜택 필드를 추가하지 않는다.
- 실행한 테스트와 결과: 코드 변경이 없는 분석·문서 기록이므로 Gradle 테스트를 실행하지 않았다. `git diff --check`로 문서 형식을 검증한다.
- 유지한 계약: Billing이 무료시험 Claim·Reservation을 소유하고 Identity는 phone eligibility event만 생산한다. raw phone·candidate·userId·credential을 로그나 문서에 노출하지 않으며 Git commit·push를 수행하지 않았다.
- 결정사항: Stage 7을 Billing consumer 재구현으로 시작하지 않는다. Billing 선배포·Lattice/IAM 경계, Identity SigV4 transport 보정, staging activation을 하나의 운영 연동 작업으로 계획한다.
- 위험 요소: 실제 AWS Lattice·IAM과 Mongo replica set 없이 로컬 테스트만으로 publisher를 켜면 unsigned 우회, Transaction 미지원, Retry-After 폭주, consumer 미배포 outbox 적체가 발생할 수 있다.
- 다음 작업: Stage 7 저장소 계획서를 Identity publisher transport 변경, Billing/AWS 배포 책임과 E2E·rollout으로 나눠 작성하고 Jira 범위를 확정한다.

## 2026-08-31 — C-02 Identity→Billing workload transport 진단 검토

<!-- codex-turn:01a056a7-befe-7393-9593-f19f7849c9de -->

- 날짜: 2026-08-31
- 브랜치: Identity `develop`
- Jira: 현재 진행 중인 Jira mutation 없음. Jira 조회·생성·수정·댓글·상태 변경을 수행하지 않았다.
- 작업 목표: 제시된 C-02 진단의 사실관계·영향·우선순위와 권고 범위를 Identity, Billing, Learning Core 구현 및 승인 ADR에 대조한다.
- 변경 파일: `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`. 애플리케이션·설정·계약·테스트 코드는 변경하지 않았고 다른 저장소도 수정하지 않았다.
- 확인 결과: 진단은 유효하며 High 우선순위의 Billing production activation gate가 맞다. Identity의 `JdkPhoneEligibilityBindingDeliveryAdapter`는 audience로 Bearer workload JWT를 발급해 전송하지만, Billing 승인 운영 경계는 VPC Lattice `AWS_IAM`에서 ECS application task role의 SigV4와 exact method/path를 검증한다. 현재 조합으로는 운영 ingress를 통과할 수 없다.
- 비교 근거: Learning Core의 `SigV4BillingReservationClient`는 AWS SDK v2 `AwsV4HttpSigner`, 기존 `DefaultCredentialsProvider` bean, signing service `vpc-lattice-svcs`, region `ap-northeast-2`, redirect 금지와 제한된 `Retry-After` 파싱을 구현했다. Identity에는 AWS SDK v2 signer dependency와 phone eligibility SigV4 adapter가 없다.
- 추가 공백: 현재 `PhoneEligibilityBindingDeliveryPort`는 `int` status만 반환하므로 ADR이 요구하는 검증된 `Retry-After`를 publisher에 전달할 수 없다. publisher는 429·503을 재시도하지만 자체 backoff만 사용한다. 설정도 full endpoint와 audience를 요구해 environment-specific Lattice base URL·고정 path·region 계약과 다르며 connect/read timeout 기본값도 승인 ADR의 1초/3초와 현재 3초/5초가 다르다.
- 권고 보정: 단순히 adapter를 추가하는 데 그치지 않고 delivery result를 status+bounded Retry-After로 확장하고 retry scheduling에 `max(localBackoff, retryAfter)`를 적용한다. phone eligibility runtime binding은 SigV4 하나로 교체하고 기존 full endpoint·audience 설정을 제거한다. 단, 공용 workload JWT provider는 UserMerged·UserWithdrawn publisher가 계속 사용하므로 전역 폐기하지 않는다. local/test는 이전 JWT transport가 아니라 fake port/WireMock을 사용한다.
- 상태 계약: eligibility endpoint의 409는 승인 계약상 `EVENT_ID_CONFLICT` 전용이며 현재 publisher도 409를 non-retryable dead-letter로 처리한다. 따라서 response body 파싱을 새로 도입할 필요는 없고 status와 bounded Retry-After만 읽으며 body는 저장하지 않는다. 401·403은 scope pause·경보, 3xx는 redirect 없이 영구 격리를 유지한다.
- 인프라 조건: Identity task role은 같은 환경 Billing service의 exact `POST /internal/v1/eligibility/trial/events`만 invoke하고 Learning Core role·반대 환경·미서명·direct target 접근은 거절한다. Billing application은 Lattice mode에서 edge 검증을 전제로 route를 permit하므로 SG·Lattice-only target과 negative E2E가 production gate다.
- 실행한 테스트와 결과: 코드 변경 없는 정적 진단이므로 Gradle 테스트를 실행하지 않았다. `git diff --check`로 문서 형식을 검증한다.
- 유지한 계약: 같은 eventId/payload at-least-once retry, response body·AWS credential·SigV4 Authorization·candidate·userId 비저장/비로그, Billing consumer 선배포와 publisher 기본 비활성을 유지한다. Git commit·push를 수행하지 않았다.
- 결정사항: C-02를 그대로 채택하되 권고 문구를 `SigV4 adapter 추가`가 아니라 `phone eligibility JWT adapter 교체 + delivery result/Retry-After 계약 확장 + route/IAM/E2E`로 구체화한다.
- 위험 요소: AWS SDK signer 추가만 하고 port·retry·설정·IAM을 그대로 두면 컴파일상 SigV4여도 Retry-After 무시, 잘못된 환경 호출, direct bypass와 route 과권한이 남는다. 반대로 workload JWT 기반 전체를 삭제하면 unrelated UserMerged·UserWithdrawn delivery가 깨진다.
- 다음 작업: Stage 7 계획서에서 Identity 코드 변경, Billing/AWS 인프라, contract test와 staging rollout을 분리해 완료 조건을 확정한 뒤 별도 승인으로 Jira를 생성한다.

## 2026-08-31 — 웹 제외 앱 서버 통합 구조 조사 참여 기록

- 날짜: 2026-08-31
- 브랜치·snapshot: `feat/TMI-116-billing-reservation-exam-saga@8c4f3ca`
- Jira: 이번 분석의 신규 Jira는 없다. 기존 `TMI-115`·`TMI-116` 문맥을 읽기 전용으로 사용했고 Jira mutation은 수행하지 않았다.
- 작업 목표: Learning Core·Identity·Billing 전체 컨셉맵, 아키텍처, Feature Map, IA와 네이밍·컨벤션·진화수렴 비교를 위해 Identity의 실제 책임과 구현 상태를 조사한다.
- 변경 파일: 통합 산출물은 Learning Core의 `docs/architecture`에 작성했고 이 저장소에서는 `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md` 기록만 갱신했다. 애플리케이션·설정·계약·테스트 코드는 변경하지 않았다.
- 확인 내용: Identity는 User·인증수단·JWT/JWKS·RefreshSession·프로필/동의/탈퇴와 PhoneEligibility/UserMerged/UserWithdrawn outbox를 소유한다. 시험·채점·사용권은 소유하지 않는다.
- 구조 판단: use case·port/adapter·outbox·worker와 redaction은 강점이다. auth subtree 탐색 비용과 현재 Bearer workload JWT 기반 phone eligibility adapter를 Billing 목표 VPC Lattice AWS_IAM·SigV4로 전환하는 작업이 주요 간극이다.
- 테스트·검증: 코드 변경이 없는 분석이므로 Gradle 테스트를 실행하지 않았다. 중앙 draw.io XML과 문서 whitespace 검증을 수행했다.
- 유지 계약: RS256/JWKS, `sub=userId`, 사용자 Token과 workload credential 분리, raw phone·Token 비노출, Identity event만으로 무료 Claim/Grant를 발급하지 않는 경계를 유지했다.
- 위험·다음 작업: publisher 기본 disabled를 유지하고 Billing 선배포, SigV4 adapter·Retry-After/contract test와 staging E2E를 완료한 뒤 활성화한다. Git commit·push와 Secret/Token 기록은 수행하지 않았다.

## 2026-08-31 — C-02 검토 기록 종료 동기화

<!-- codex-turn:01a056a7-befe-7393-9593-f19f7849c9de -->

- 날짜: 2026-08-31
- 브랜치: Identity `develop`
- 작업 목표: 동시 문서 갱신 이후에도 이번 C-02 검토 결과와 지정 turn marker를 WORKLOG 끝에 보존한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·설정·계약·테스트 코드는 변경하지 않았다.
- 검토 결론: C-02는 유효한 High/production gate다. phone eligibility 전용 JWT adapter를 SigV4로 교체하고 port의 status-only 결과를 bounded Retry-After 포함 결과로 확장해야 한다. 공용 workload JWT 기반 전체는 삭제하지 않는다.
- 실행한 테스트와 결과: 정적 검토이므로 Gradle 테스트는 실행하지 않았고 `git diff --check`를 통과했다.
- 유지한 계약: publisher 기본 비활성, Billing consumer 선배포, exact route/task role, same-event retry와 민감값 비기록을 유지한다. Jira mutation과 Git commit·push는 수행하지 않았다.
- 다음 작업: Stage 7 계획서와 Jira 범위에서 Identity SigV4 코드, Billing/AWS 인프라와 staging E2E 완료 조건을 분리해 명시한다.

## 2026-08-31 — C-02 실제 수정 범위 쉬운 설명

<!-- codex-turn:01a056a7-befe-7393-9593-f19f7849c9de -->

- 날짜: 2026-08-31
- 브랜치: Identity `develop`
- 작업 목표: C-02가 도메인 로직이 아니라 Identity→Billing 요청의 인증·전송 계층을 바꾸는 작업임을 구체적인 변경 전후 흐름으로 설명한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·설정·계약·테스트 코드는 변경하지 않았다.
- 변경 전: Identity publisher가 기존 event JSON을 만들고 JDK adapter가 workload JWT를 발급해 Bearer Authorization header로 Billing에 POST한다. adapter는 응답 status만 반환한다.
- 변경 후: 같은 eventId와 JSON을 그대로 사용하되 adapter가 ECS task role 임시 credential로 요청 전체를 SigV4 서명해 Lattice endpoint에 POST한다. 응답은 status와 검증된 Retry-After를 publisher에 전달한다.
- 함께 수정할 부분: delivery port result type, SigV4 adapter와 최소 AWS SDK v2 dependency, phone eligibility publisher configuration/base URL·region validation, Retry-After scheduling과 transport contract test다.
- 변경하지 않는 부분: PhoneEligibility outbox·event schema·Billing consumer·무료시험 Claim 정책, UserMerged·UserWithdrawn workload JWT transport다.
- 실행한 테스트와 결과: 코드 변경 없는 설명 작업이라 Gradle 테스트를 실행하지 않았고 `git diff --check`로 문서를 검증한다.
- 유지한 계약: publisher 기본 비활성, Billing consumer 선배포, 동일 event 재시도, 민감값 비기록과 Git commit·push 금지를 유지한다.
- 다음 작업: 사용자가 원하면 이 범위를 파일 단위 구현 계획서로 작성한다.

## 2026-08-31 — C-02 수정 대상 설명 종료 기록

<!-- codex-turn:01a056aa-1240-7b42-ab49-13506080e6c2 -->

- 날짜: 2026-08-31
- 브랜치: Identity `develop`
- 작업 목표: Identity→Billing C-02가 수정하는 범위를 JWT 출입증에서 AWS SigV4 출입증으로 바꾸는 전송 계층 작업으로 설명한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·설정·계약·테스트 코드는 변경하지 않았다.
- 설명 결과: phone eligibility의 eventId·JSON·outbox와 Billing consumer·무료시험 정책은 유지한다. Identity의 Billing 전용 HTTP adapter, delivery result의 Retry-After 전달, Lattice base URL·region 설정과 exact route task role 권한만 변경 대상이다.
- 제외 확인: UserMerged·UserWithdrawn workload JWT 발급·전송은 이번 교체 대상이 아니며 공용 JWT 기반을 전역 삭제하지 않는다.
- 실행한 테스트와 결과: 코드 변경이 없는 설명·기록 작업이므로 Gradle 테스트를 실행하지 않았고 `git diff --check`로 문서 형식을 검증한다.
- 유지한 계약: publisher 기본 비활성, Billing 선배포, 동일 eventId/payload 재시도, 민감정보 비기록과 Git commit·push 금지를 유지한다.
- 다음 작업: 요청 시 Stage 7 파일 단위 구현 계획서를 작성한다.

## 2026-08-31 — Billing transport를 JWT에서 SigV4로 바꾸는 이유 설명

<!-- codex-turn:01a056cc-a23b-7fe3-9e2f-eb59204f69dd -->

- 날짜: 2026-08-31
- 브랜치: Identity `develop`
- 작업 목표: phone eligibility 호출에서 workload JWT 대신 AWS SigV4를 써야 하는 이유와 JWT 유지 대안의 비용을 설명한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·설정·계약·테스트 코드는 변경하지 않았다.
- 핵심 이유: JWT가 원천적으로 부적합해서가 아니라 Billing 운영 ingress를 VPC Lattice `AWS_IAM`으로 확정했기 때문이다. Lattice는 SigV4 요청의 ECS application task role과 same-environment service, method, exact path를 Billing 도달 전에 검증한다.
- 운영 이점: AWS SDK `DefaultCredentialsProvider`가 ECS task role의 자동 회전 임시 credential을 사용하므로 이 route를 위해 별도 workload signing key·issuer·audience·JWKS rotation과 Billing JWT decoder를 운영하지 않는다. Learning Core→Billing과도 같은 trust model을 사용한다.
- JWT 유지 대안: 기술적으로 가능하지만 Billing workload JWT decoder/filter, issuer·audience·principal 검증, JWKS rotation, route별 authorization과 직접 target 차단을 새로 구현하고 Lattice AWS_IAM ADR을 변경해야 한다. 단순히 현재 Bearer header를 유지하는 것만으로는 Lattice에서 인증되지 않는다.
- 경계: 이 결정은 Billing을 호출하는 phone eligibility transport에만 적용한다. Learning Core가 JWT를 검증하도록 설계된 UserMerged·UserWithdrawn publisher는 그대로 유지한다.
- 실행한 테스트와 결과: 코드 변경 없는 설명·문서 기록이라 Gradle 테스트를 실행하지 않았고 `git diff --check`로 검증한다.
- 유지한 계약: publisher 기본 비활성, 환경·route별 최소 권한, 자동 회전 임시 credential, 민감정보 비기록과 Git commit·push 금지를 유지한다.
- 결정사항: 현재 승인된 Billing Lattice AWS_IAM 아키텍처를 유지한다면 Identity phone eligibility는 SigV4로 교체한다. JWT를 유지하려면 별도 아키텍처 변경 결정이 선행되어야 한다.
- 다음 작업: Stage 7 계획서에 이 선택의 근거와 JWT 유지 대안의 제외 사유를 기록한다.

## 2026-08-31 — 세 앱 서버 문서 계층·완료 보고 규칙 통일

- 날짜: 2026-08-31
- 브랜치: `develop`
- Jira: 별도 Jira 이슈 키가 없으며 Jira를 조회하거나 변경하지 않았다.
- 작업 목표: Identity를 포함한 세 앱 서버의 계획·조사 문서와 구현 완료 보고 형식을 읽기 쉬운 공통 계층으로 통일한다.
- 변경 파일: `AGENTS.md`, `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`.
- 변경 내용: 5줄 결론부터 상세 부록까지의 6단계 문서 구조와 파일 근거·구현 사실/계획/추론 구분을 추가했다. 구현 완료 보고에는 변경·계약·테스트·위험·배포 전 확인·예상 밖 diff·다음 확인을 포함한다.
- 유지한 계약: Identity 공개 API, JWT/JWKS, Refresh Token, downstream event와 보안 계약을 변경하지 않았다.
- 테스트·검증: 규칙·기록 문서만 변경해 Gradle 테스트는 실행하지 않고 `git diff --check`로 검증한다.
- 위험·다음 작업: 새 규칙이 이후 계획과 구현 보고에 실제 적용되는지 확인한다. 애플리케이션 배포 전 확인 사항은 없다.
- 예상 밖 diff: 이번 작업 전부터 존재하던 기록 문서 변경 외 예상 밖 애플리케이션 파일 변경은 없다.
- Git commit·push를 수행하지 않았고 Secret, Token, Password를 기록하지 않았다.

## 2026-09-02 — Stage 7 즉시 다음 작업 확인

<!-- codex-turn:01a06105-4be5-7540-b49f-f0ef965d5f75 -->

- 날짜: 2026-09-02
- 브랜치: Identity `develop`
- Jira: 현재 진행 중인 Jira 없음. Jira 조회·생성·수정·댓글·상태 변경은 수행하지 않았다.
- 작업 목표: 완료된 Stage 6 다음의 고정 구현 순서와 Stage 7에서 이미 완료된 Billing 범위를 구분해 지금 착수할 작업을 확정한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 기존 사용자 변경인 `AGENTS.md`와 애플리케이션·설정·계약·테스트 코드는 변경하지 않았다.
- 구현 내용: 코드 구현은 수행하지 않았다. Billing `TMI-110` eligibility consumer가 이미 있으므로 다음 산출물은 Identity phone eligibility SigV4 transport 구현 계획서다. 계획에는 delivery result의 status+bounded Retry-After, AWS SDK v2 signer·task credential, Lattice base URL·고정 path·region 설정, JWT adapter 제거 경계와 contract test를 포함한다.
- 후속 순서: 계획서 확정 → 사용자에게 Jira 생성안 제시·승인 → Identity 코드 구현 → Billing/Lattice/IAM·transaction Mongo 선배포 → staging positive/negative E2E → publisher 활성화다.
- 실행한 테스트와 결과: 코드 변경이 없는 순서 확인·문서 기록이므로 Gradle 테스트를 실행하지 않았다. `git diff --check`로 문서 형식을 검증한다.
- 유지한 계약: Billing consumer를 재구현하지 않고 event schema·outbox·무료시험 Claim 정책과 UserMerged·UserWithdrawn workload JWT transport를 유지한다. publisher는 E2E 전까지 기본 비활성이다.
- 결정사항: 즉시 다음 작업은 Stage 7 계획서 작성이며, 첫 구현 Jira는 Identity phone eligibility SigV4 transport로 한정한다. AWS 인프라 배포와 staging 활성화는 완료 조건에 포함하되 코드 책임과 구분한다.
- 위험 요소: Identity 코드만 완료하고 Billing/Lattice/IAM 선배포·negative E2E 없이 publisher를 활성화하면 운영 요청이 401/403으로 차단되거나 direct bypass·환경 교차 호출 위험이 남는다.
- 다음 작업: 사용자 요청 시 `docs/contracts`에 Stage 7 구현 계획서를 새로 작성한다.

## 2026-09-02 — Identity durable owner fan-out 구현 순서 분류

<!-- codex-turn:01a0610a-5de2-7790-9990-586cb1bf9161 -->

- 날짜: 2026-09-02
- 브랜치: Identity `develop`
- Jira: Billing owner-rebind consumer의 관련 Jira는 `TMI-120`이나 이번 turn에는 읽기 전용 저장소 문맥만 확인했다. Jira 조회·생성·수정·댓글·상태 변경은 수행하지 않았다.
- 작업 목표: `UserMerged`와 `TrialOwnerRebindApproved`를 Billing/Learning Core에 독립 전달하는 Identity durable fan-out 구현이 현재 고정 순서의 몇 번째인지 판정한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 기존 사용자 변경인 `AGENTS.md`, 애플리케이션·설정·계약·테스트와 다른 저장소는 변경하지 않았다.
- 구현 내용: 코드 구현은 수행하지 않았다. 이 fan-out은 Billing 최소 Entitlement 연동을 확장해 탈퇴·재가입 또는 Guest merge 뒤 retained trial owner를 이전하는 cross-service 작업이므로 Stage 7의 하위 단계로 분류했다.
- 세부 순서: Stage 7-A phone eligibility SigV4 전송 기반 완성 → 7-B Billing `TMI-120` 두 owner event consumer 비활성 선배포 → 7-C Identity immutable event core와 consumer별 `BILLING`/`LEARNING_CORE` delivery 저장·retry·dead-letter·flag 구현 → 7-D Learning Core owner migration/source deny consumer → 7-E staging 순서 역전 E2E와 canary 활성화다.
- 구분: 현재 목록 Stage 12 `기존 ACTIVE 회원의 Firebase rebind 정책`은 기존 내부 회원에게 Firebase credential을 안전하게 연결하는 인증수단 정책이다. 질문의 owner fan-out은 무료시험의 기존 logical owner를 새 canonical userId에 연결하는 downstream 소유권 이전이므로 Stage 12가 아니다.
- 선행 조건: Billing consumer가 producer보다 먼저 준비돼야 한다. `UserMerged` 기존 단일 Learning Core delivery를 Billing endpoint로 단순 변경하지 않고 event core 하나와 `(eventId, consumer)`별 독립 delivery로 reader-first 전환한다. `TrialOwnerRebindApproved`는 phone 재가입 전용 event로 별도 의미를 유지한다.
- 실행한 테스트와 결과: 코드 변경 없는 순서 분석·문서 기록이므로 Gradle 테스트를 실행하지 않았다. `git diff --check`로 문서 형식을 검증한다.
- 유지한 계약: 한 consumer 성공이 다른 consumer 성공을 대신하지 않으며 Billing 두 route는 SigV4, Learning Core route는 승인된 별도 workload 인증을 사용한다. event schema·무료 Claim/consumption 이력과 민감정보 비기록을 유지하고 Git commit·push는 수행하지 않았다.
- 결정사항: 질문의 작업은 Stage 7-C다. 현 12단계 번호를 재편하지 않는 한 `7.3 Identity durable owner fan-out`으로 기록하는 것이 가장 명확하다.
- 위험 요소: fan-out producer를 Billing·Learning Core consumer보다 먼저 활성화하면 독립 delivery가 dead-letter로 쌓인다. 기존 `UserMergedOutbox` 단일 상태를 즉시 파괴적으로 바꾸면 미전송 legacy event를 잃을 수 있어 reader-first migration이 필요하다.
- 다음 작업: 우선 Stage 7-A SigV4 전송 계획·구현을 완료하고 Billing `TMI-120` consumer readiness를 확인한 뒤 Stage 7-C Identity fan-out 계획서와 Jira를 별도로 작성한다.

## 2026-09-02 — 첨부 요구 기준 Stage 7 Identity 작업 범위 설명

<!-- codex-turn:01a0610e-b223-7e02-9581-f5e406cd91c8 -->

- 날짜: 2026-09-02
- 브랜치: Identity `develop`
- Jira: Billing consumer 관련 키는 `TMI-120`이다. 이번 turn에는 Jira를 조회하거나 생성·수정·댓글·상태 변경하지 않았다.
- 작업 목표: 사용자가 첨부한 durable fan-out 요구 201줄과 Billing ADR-003, 현재 Identity `UserMergedOutbox`·publisher·Guest merge·phone eligibility/withdrawal 코드를 대조해 Stage 7에서 수행할 일을 설명한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 기존 사용자 변경인 `AGENTS.md`, 애플리케이션·설정·계약·테스트와 다른 저장소는 변경하지 않았다.
- 구현 내용: 코드 구현은 수행하지 않았다. Stage 7 Identity 작업을 SigV4 transport 기반, owner event core/delivery 저장, lifecycle Transaction 연결, consumer별 publisher, legacy migration/preflight, contract test·rollout으로 분해했다.
- 확인된 구현 사실: 기존 `UserMerged` v1은 Guest merge Transaction에서 이미 생성되지만 `UserMergedOutbox` 한 문서가 immutable payload와 단일 PENDING/lease/retry/published 상태를 함께 가진다. `UserMergedPublisher`도 하나의 delivery port만 호출한다. Phone eligibility VERIFIED/REVOKED revision/outbox는 존재하지만 `TrialOwnerRebindApproved` event와 source→target durable delivery는 없다.
- Stage 7 저장 변경: immutable event core와 `(eventId, consumer)` unique delivery를 분리하고 신규 event마다 `BILLING`, `LEARNING_CORE` delivery를 원자 저장한다. consumer별 status·attempt·nextAttemptAt·lease·failure·terminal/cleanup을 독립 관리하며 payload를 delivery마다 복제하거나 global PUBLISHED를 사용하지 않는다.
- lifecycle 연결: Guest merge는 기존 `UserMerged` payload를 바꾸지 않고 source MERGED·source Session 폐기·target Session·event core·두 delivery를 같은 Mongo Transaction에 묶는다. Phone 재가입은 source inactive/CLEANED, source binding REVOKED, target verified, source/target revision과 서로 다른 UUID를 검증한 뒤 별도 `TrialOwnerRebindApproved` core와 두 delivery를 저장한다.
- 전송·실패 처리: Billing과 Learning Core의 승인 route에 ECS task role 기반 Lattice SigV4로 전송하고 traceparent를 서명 전에 넣으며 baggage·redirect를 금지한다. 2xx는 해당 delivery만 성공, timeout·408·425·429·5xx와 503 pending은 same event retry, 400·409·422는 dead-letter, 401·403은 해당 consumer circuit pause로 처리한다. status+bounded Retry-After delivery result를 사용한다.
- legacy 전환: 기존 `user_merged_outbox`를 즉시 삭제하거나 일괄 변환하지 않는다. reader-first로 기존 Learning Core delivery를 수렴시키고 신규 merge부터 두 delivery를 쓴다. historical merge를 Billing에 자동 backfill하지 않으며 모순 legacy row가 있으면 publisher 활성화를 중단한다.
- 외부 선행 조건: Billing `TMI-120` 두 owner route와 Learning Core owner migration/source deny consumer를 각각 flag off로 먼저 배포한다. Identity는 downstream DB를 직접 수정하지 않고 signup/merge 응답이 HTTP delivery를 기다리지 않도록 Transactional outbox만 저장한다.
- 사용자 결정 필요: 동일 phone의 fresh verification만으로 과거 시험·Session owner를 새 계정에 자동 이전하면 번호 재할당 시 타인의 학습 데이터가 노출될 수 있다. `TrialOwnerRebindApproved` 발행에 old-account proof를 추가할지, 명시적 복구 승인으로 제한할지, 제품이 same-phone 자동 이전 위험을 수용할지 Stage 7 계획서에서 확정해야 한다. Billing ADR의 Identity 승인 event 자체는 이 proof를 대신하지 않는다.
- 실행한 테스트와 결과: 코드 변경 없는 분석·문서 기록이므로 Gradle 테스트를 실행하지 않았다. 첨부 전문과 관련 코드·ADR을 정적으로 대조했고 `git diff --check`로 문서 형식을 검증한다.
- 유지한 계약: `UserMerged` v1 wire 불변, 두 lifecycle strict 분리, 무료 Claim·Grant·consumption 불변, consumer-first 배포, publisher 기본 OFF, 민감 phone/candidate/credential·source/target userId 비로그와 Git commit·push 금지를 유지한다.
- 결정사항: Stage 7은 단일 SigV4 adapter 작업이 아니라 owner event 생산·durable fan-out까지 포함하는 cross-service integration 묶음이다. 구현 Jira는 최소 transport 기반과 Identity owner fan-out을 분리하고, Billing/Learning Core consumer readiness를 activation gate로 둔다.
- 위험 요소: 단일 generic `OwnerEventCore`로 두 lifecycle 의미를 성급히 합치면 schema·trigger가 혼동될 수 있다. 논리적으로 core/delivery 패턴은 공통화하되 `UserMerged`와 `TrialOwnerRebindApproved` payload·생성 gate는 분리해야 한다. 번호 재할당 proof, legacy 미전송 row, consumer 순서 역전과 Lattice direct bypass가 주요 미확인 위험이다.
- 다음 작업: Stage 7 계획서를 새로 작성해 phone 재가입 approval proof와 코드/Jira 분할을 먼저 확정한 뒤 SigV4 transport 기반부터 구현한다.

## 2026-09-02 — phone 재가입 owner 이전 정책 선택지 비교

<!-- codex-turn:01a0613b-85a8-7a32-991d-7fc37dbd729b -->

- 날짜: 2026-09-02
- 브랜치: Identity `develop`
- Jira: 관련 Billing consumer Jira는 `TMI-120`이나 이번 turn에는 Jira 조회·생성·수정·댓글·상태 변경을 수행하지 않았다.
- 작업 목표: `TrialOwnerRebindApproved`를 어떤 proof로 발행하고 Billing 무료 권리와 Learning Core 시험·결과를 어디까지 이전할지 선택지와 장단점을 분석한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 기존 사용자 변경인 `AGENTS.md`, 애플리케이션·설정·계약·테스트와 다른 저장소는 변경하지 않았다.
- 구현 내용: 코드 구현은 수행하지 않았다. 결정을 `phone-scoped Billing entitlement owner`와 `개인 Learning Core 학습 데이터 owner` 두 축으로 분리하고 다섯 정책을 비교했다.
- 선택지 A 전체 자동 이전: 동일 phone fresh proof와 source CLEANED/REVOKED, target VERIFIED만으로 Billing owner와 Learning Core 이력을 모두 이전한다. UX와 자동 복구는 가장 좋고 현재 양 consumer fan-out 계약과 단순하게 맞지만, 번호 재할당 시 타인의 시험·결과가 노출되는 최고 수준의 개인정보 위험이 있다.
- 선택지 B 제한 자동 이전: Billing은 같은 retained candidate의 Claim owner link만 target으로 옮기되 Claim·Grant·consumption을 복원하지 않고, Learning Core의 과거 시험·결과는 이전하지 않는다. phone당 1회 정책과 미사용권 연속성·개인정보 보호의 균형이 좋지만, 진행 중/재응시 데이터 연속성이 끊기고 현재 두 consumer 고정 fan-out 계약을 조정해야 한다.
- 선택지 C strong proof 전체 이전: 새 phone proof와 함께 old-account session/re-auth 또는 사전 발급된 one-time recovery proof가 있을 때만 Billing과 Learning Core를 모두 이전한다. 사용자 연속성과 개인정보 보호가 가장 균형적이나 withdrawal 전후 proof lifecycle, hash 저장·만료·일회성 소비, 모바일 UX와 응답 유실 복구를 새로 구현해야 한다.
- 선택지 D 운영자 수동 이전: 자동으로는 이력을 이전하지 않고 고객센터가 별도 검증 후 승인한다. 자동 오판 위험과 초기 구현은 작지만 개인정보 취급 운영, 감사·SLA·인력 비용과 좋지 않은 사용자 경험이 생기며 privileged repair 계약이 필요하다.
- 선택지 E 자동 이전 금지: 동일 번호 재가입에도 과거 owner를 절대 이전하지 않는다. 보안·구현은 가장 단순하지만 미사용 무료권도 새 계정에서 사용하지 못할 수 있고 탈퇴 후 재가입 연속성이 사실상 사라진다.
- 권장안: B를 기본으로 하고 C를 선택적 복구 경로로 둔다. Billing은 phone-scoped 정책에 따라 source CLEANED/REVOKED·target VERIFIED·candidate/revision fencing을 만족할 때 owner link만 옮기고 사용 이력을 불변으로 유지한다. Learning Core 이력은 phone proof만으로 자동 이전하지 않고 old-account-derived strong proof가 있을 때만 이전한다.
- 계약 영향: 권장안을 채택하면 현재 `TrialOwnerRebindApproved` event 하나에 Billing·Learning Core delivery 두 건을 무조건 생성하는 ADR-003 계약을 그대로 사용할 수 없다. Billing-only event와 full-history approval event를 분리하거나, immutable event core에 승인 scope를 versioned schema로 추가하고 consumer별 delivery 생성 조건을 명시해야 한다. 기존 v1 의미를 조용히 바꾸면 안 된다.
- 실행한 테스트와 결과: 코드 변경 없는 정책 분석·문서 기록이라 Gradle 테스트를 실행하지 않았다. `git diff --check`로 문서 형식을 검증한다.
- 유지한 계약: phone당 무료시험 1회, Claim·Grant·consumption 비복원, source/target 자동 추정 금지, consumer별 독립 delivery, 민감 phone·candidate·credential 비기록과 Git commit·push 금지를 유지한다.
- 결정사항: 아직 사용자 최종 선택 전이다. Codex 권장은 제한 자동 Billing rebind + strong-proof Learning Core 이전이다.
- 위험 요소: phone proof를 사람 identity proof로 취급하면 번호 재할당 시 학습 데이터 유출이 발생한다. 반대로 Billing owner도 전혀 옮기지 않으면 정상 재가입자가 미사용권을 잃는다. strong proof는 withdrawal 뒤 기존 credential이 삭제되므로 탈퇴 전 발급 또는 별도 복구 lifecycle 없이는 만들 수 없다.
- 다음 작업: 사용자가 기본 정책과 strong proof 도입 여부를 선택하면 Stage 7 계획서와 Billing ADR-003의 event/delivery 계약 보정안을 먼저 작성한다.

## 2026-09-02 — Stage 7 owner 이전 제품 정책 확정과 잔여 계약 분류

- 날짜: 2026-09-02
- 브랜치: Identity `develop`
- Jira: 관련 Billing consumer Jira는 `TMI-120`이나 이번 turn에는 Jira 조회·생성·수정·댓글·상태 변경을 수행하지 않았다.
- 작업 목표: phone 재가입과 Guest merge에서 Billing 권리와 Learning Core 학습 데이터를 어디까지 이전할지 확정하고, Stage 7 구현 전에 추가로 고정할 기술 계약을 분류한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 기존 사용자 변경인 `AGENTS.md`, 애플리케이션·설정·테스트와 Billing/Learning Core 저장소는 변경하지 않았다.
- 구현 내용: 코드 구현은 수행하지 않았다. `UserMerged`는 Billing과 Learning Core 양쪽에 durable delivery를 만들고, 동일 phone proof 기반 `TrialOwnerRebindApproved` v1은 Billing에만 전달하며, Learning Core 과거 시험·결과는 별도의 old-account-derived strong-proof continuity event가 생기기 전까지 이전하지 않는 정책을 확정했다.
- Billing 불변 조건: owner link만 source에서 target으로 이전하고 기존 Claim·Grant·allocation·consumption과 ledger는 초기화·복원하지 않아 phone당 무료시험 1회 정책을 유지한다.
- 후속 분리: strong proof의 종류·발급·만료·일회성 소비와 Learning Core continuity wire는 Stage 7에서 제외하고 별도 후속 Jira로 다룬다. 기존 `TrialOwnerRebindApproved` v1 의미를 확장하거나 Learning Core로 조용히 재사용하지 않는다.
- 추가 기술 기본값: released alias의 최신값을 임의 source로 선택하지 않고 Identity local predecessor lineage로 exact source를 확정하며 모호하면 fail-closed/reconciliation 처리한다. consumer별 predecessor가 성공하기 전 후속 delivery를 claim하지 않고, predecessor 영구 실패 시 후속 chain도 운영 해결 전 보류한다.
- delivery 기본값: `2xx`만 PUBLISHED, timeout·connection·408·425·429·5xx와 bounded `Retry-After`는 재시도, 400·409·422는 dead-letter, 401·403·404·405·3xx는 consumer circuit pause·경보·수동 재개로 분류한다. Billing delivery는 SigV4, Learning Core delivery는 별도 변경 승인 전 기존 workload JWT 경계를 유지한다.
- 보존·rollout 기본값: PUBLISHED delivery는 P30D 보존하고 dead-letter는 P90D review 시각만 두되 자동 삭제하지 않는다. event core는 모든 required delivery가 성공 또는 명시적 운영 종결되기 전 삭제하지 않는다. Billing `UserMerged`, Learning Core `UserMerged`, Billing `TrialOwnerRebindApproved` publisher flag와 circuit 상태를 각각 독립 관리하고 historical Guest merge의 Billing 자동 backfill은 하지 않는다.
- 실행한 테스트와 결과: 코드 변경 없는 계약 분석이므로 Gradle 테스트는 실행하지 않았다. 관련 Identity entity/repository·기존 fan-out 전달문·Billing ADR-003을 정적으로 대조했고 `git diff --check`로 문서 형식을 검증한다.
- 유지한 계약: `UserMerged` v1 wire, phone당 무료시험 1회, Claim·Grant·consumption 불변, consumer-first 배포, publisher 기본 OFF, raw phone·candidate·credential·payload 비로그와 Git commit·push 금지를 유지한다.
- 결정사항: 제품 수준에서 Stage 7을 막는 추가 선택은 없다. 다만 Billing ADR-003의 phone rejoin Learning Core delivery·route 문구를 승인 정책에 맞게 고친 뒤 Identity 계획서·Jira를 작성해야 한다.
- 위험 요소: 현재 released `PhoneFingerprintAlias`는 userId·fingerprint·releasedAt을 보존하지만 exact predecessor lineage 조회 계약이 없다. 단순 최신 alias 선택과 `occurredAt` 정렬만으로는 번호 재할당·A→B→C chain·다중 publisher 경쟁을 안전하게 막지 못한다.
- 다음 작업: Stage 7 계획서에서 위 기본값의 exact entity/index/state transition/flag/retention을 명세하고, Billing 동시 `TMI-120` 변경이 정리된 뒤 ADR-003 보정안을 사용자에게 먼저 제시해 승인받는다.

## 2026-09-02 — Stage 7 정책 확정 종료 기록 동기화

<!-- codex-turn:01a0614c-196a-73d0-8680-51c497cc3e03 -->

- 날짜: 2026-09-02
- 브랜치: Identity `develop`
- Jira: 관련 Billing 이슈는 `TMI-120`이며 Jira 조회·생성·수정·댓글·상태 변경은 수행하지 않았다.
- 작업 목표: 이번 turn에서 승인된 owner 이전 정책과 잔여 기술 계약을 turn marker가 포함된 WORKLOG 마지막 항목으로 동기화한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·설정·테스트와 다른 저장소는 변경하지 않았다.
- 구현 내용: 코드 구현은 없다. `UserMerged`는 Billing·Learning Core 양쪽, 동일 phone proof 기반 `TrialOwnerRebindApproved`는 Billing에만 전달하고 Learning Core 과거 데이터 이전은 strong-proof 후속 작업으로 분리하는 결정을 기록했다.
- 실행한 테스트와 결과: 문서만 변경해 Gradle 테스트는 실행하지 않았다. `git diff --check`로 형식을 검증한다.
- 유지한 계약: phone당 무료시험 1회, Claim·Grant·consumption 불변, `UserMerged` v1 wire, consumer-first 배포, publisher 기본 비활성, 민감정보 비기록과 Git commit·push 금지를 유지한다.
- 결정사항: Stage 7 제품 선택은 완료됐다. local predecessor lineage, consumer별 predecessor fencing, 독립 publisher flag·circuit, published P30D와 dead-letter P90D review/no auto-delete를 계획서 기술 기본값으로 사용한다.
- 위험 요소: 현재 Billing ADR-003의 phone rejoin Learning Core delivery 요구가 승인 정책과 충돌하며, released phone alias만으로 exact predecessor를 추측하면 번호 재할당과 owner chain 오판 위험이 있다.
- 다음 작업: Identity Stage 7 계획서와 Billing ADR-003 보정안을 작성하되 Billing의 동시 `TMI-120` 변경을 덮어쓰지 않고 사용자 승인 후 반영한다.

## 2026-09-03 — Stage 7 Billing SigV4·owner durable fan-out 계획서 작성

<!-- codex-turn:01a064cc-2884-7650-818f-06c298ffad97 -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`
- Jira: 관련 Billing consumer 이슈는 `TMI-120`이며 이번 작업에서 Jira 조회·생성·수정·댓글·상태 변경은 수행하지 않았다.
- 작업 목표: 승인된 phone 재가입 Billing-only 정책과 Guest merge 양 consumer fan-out을 기준으로 Stage 7 전체의 저장 구조, Transaction, transport, 순서, 보존, rollout과 테스트 계획을 저장소 문서로 확정한다.
- 변경 파일: `docs/contracts/billing-entitlement-owner-fanout-stage-7-plan.md` 신규 작성, `docs/contracts/firebase-auth-follow-up-implementation-order.md`, `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md` 갱신. 기존 사용자 변경인 `AGENTS.md`와 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: 코드 구현은 없다. Stage 7을 7-A Identity→Billing SigV4, 7-B Billing `TMI-120` readiness·비활성 선배포, 7-C Identity owner event core·consumer delivery·phone lineage, 7-D Learning Core `UserMerged` consumer, 7-E staging E2E·canary로 분해했다.
- 데이터 계획: `owner_event_cores`, `(eventId, consumer)` unique인 `owner_event_deliveries`, consumer sequence·circuit cursor, exact source를 보존하는 `phone_rejoin_lineages`의 필드·인덱스·TTL과 Transaction 경계를 명세했다. latest RELEASED alias 추측은 금지하고 lineage 0건은 event 없음, 1건만 자동 rebind, 복수·모순은 가입을 허용하되 reconciliation으로 처리한다.
- 전달 계획: phone eligibility와 Billing owner route는 ECS application task role·VPC Lattice AWS_IAM·`vpc-lattice-svcs` SigV4를 사용하고 status+1~300초 bounded Retry-After를 publisher에 전달한다. Learning Core `UserMerged`는 별도 변경 승인 전 기존 workload JWT 경계를 유지한다.
- 순서·실패 계획: consumer-wide monotonic sequence와 `lastPublishedSequence+1` claim으로 A→B보다 B→C가 먼저 전달되지 않게 한다. 2xx는 PUBLISHED, retryable transport·408·425·429·5xx는 retry, 400·409·413·422는 DEAD_LETTER, 401·403·404·405·unexpected 4xx·3xx는 consumer circuit pause로 분류한다.
- 보존·migration 계획: PUBLISHED delivery P30D, DEAD_LETTER P90D review/no auto-delete, 모든 required delivery 성공 전 core TTL 금지와 core cleanup safety margin PT24H를 정의했다. 기존 `user_merged_outbox`는 Learning Core legacy reader로 drain하고 historical Guest merge와 pre-cutover CLEANED phone lineage를 자동 backfill하지 않는다.
- 실행한 테스트와 결과: 계획·순서·기록 문서만 변경해 Gradle 테스트는 실행하지 않았다. 관련 Identity entity·Repository·Transaction·publisher, Identity ADR-002, Stage 3 계획과 Billing ADR-003·PLAN-006을 정적으로 대조했고 `git diff --check`가 통과했다.
- 유지한 계약: 기존 공개 API·BaseResponse, RS256/JWKS와 JWT `sub=userId`, Refresh Token 원문 비저장, raw phone 비저장, Python AI `user_id=examId`, `UserMerged` v1과 phone eligibility v1 wire, Billing Claim·Grant·consumption 소유권과 consumer-first 배포를 유지한다.
- 결정사항: `UserMerged`는 Billing·Learning Core 두 delivery, 동일 phone proof 기반 `TrialOwnerRebindApproved`는 Billing-only다. Learning Core 과거 데이터 continuity는 strong-proof 별도 Jira로 미루며 Stage 7을 시작하기 위한 추가 제품 결정은 없다.
- 위험 요소: Billing ADR-003의 phone rejoin Learning Core delivery·SigV4 전제가 현재 승인 정책과 충돌한다. consumer-wide FIFO는 안전한 대신 dead-letter·disabled channel의 head-of-line blocking이 있으며, 실제 Mongo replica set·Lattice IAM·direct bypass·환경 교차 거절은 staging에서만 검증할 수 있다.
- 다음 작업: Billing 동시 `TMI-120` 변경 상태를 확인해 ADR-003 보정안을 먼저 사용자에게 제시하고, 승인 후 Stage 7-A Identity SigV4 transport Jira를 생성한다.

## 2026-09-03 — Billing ADR-003 보정 의미 설명

<!-- codex-turn:01a064eb-fb65-7653-a62e-10415b4d813f -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`
- Jira: 관련 Billing 구현 이슈는 `TMI-120`이며 Jira 조회·생성·수정·댓글·상태 변경은 수행하지 않았다.
- 작업 목표: Stage 7 계획서의 “Billing ADR-003 보정”이 Billing 구현 삭제인지, cross-service delivery 정책 수정인지 사용자 관점에서 명확히 구분한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·계약·계획·테스트와 Billing 저장소 파일은 변경하지 않았다.
- 확인 내용: Billing `feat/TMI-120-trial-owner-rebind-consumer`에는 ADR-003·PLAN-006과 owner-rebind 코드가 아직 untracked/modified 상태다. ADR-003은 phone 재가입과 Guest merge 모두 Learning Core로 보내고 두 delivery를 SigV4로 전송하는 기존 전제를 담고 있다.
- 설명 결과: Billing이 받는 `POST /internal/v1/eligibility/trial/owner/events`와 `POST /internal/v1/owners/merge/events`, owner link CAS와 Claim·Grant·consumption 불변 정책은 유지한다. 변경하는 것은 Identity fan-out과 Learning Core 범위다. `UserMerged`는 Billing·Learning Core 양쪽, `TrialOwnerRebindApproved`는 Billing-only로 보내며 phone proof만으로 Learning Core 시험·결과를 이전하지 않는다.
- 인증 구분: Identity→Billing은 VPC Lattice AWS_IAM·SigV4를 사용하고, Identity→Learning Core `UserMerged`는 별도 migration 승인 전 기존 workload JWT를 유지한다. ADR의 “두 delivery 모두 SigV4”로 읽히는 문구를 destination별 계약으로 나눠야 한다.
- 실행한 테스트와 결과: 코드 변경 없는 설명 작업이라 Gradle 테스트를 실행하지 않았다. Billing working tree와 ADR-003 관련 문구를 읽기 전용으로 확인하고 Identity 문서 변경에 `git diff --check`를 실행한다.
- 유지한 계약: Billing의 두 owner consumer route, 무료시험 owner mapping만 이전하는 동작, Claim·Grant·consumption 불변, consumer-first 배포와 phone proof 기반 Learning Core history 이전 금지를 유지한다.
- 결정사항: ADR 보정은 Billing TMI-120 consumer를 제거하거나 되돌리는 작업이 아니다. Learning Core phone route·자동 history migration 요구와 포괄적인 SigV4 fan-out 표현만 승인된 destination별 정책으로 수정한다.
- 위험 요소: 현재 Billing 작업 트리에 대량의 사용자 변경과 untracked ADR이 있으므로 다른 작업에서 파일 전체를 덮어쓰면 TMI-120 구현 문맥을 잃을 수 있다. 실제 수정 전 exact diff를 사용자에게 제시하고 승인받아야 한다.
- 다음 작업: 사용자가 원하면 Billing ADR-003에서 바꿀 문단과 유지할 문단의 exact 수정안을 먼저 제시하며, 승인 전 Billing 파일은 수정하지 않는다.

## 2026-09-03 — Billing 코드 보정 필요 범위 추가 확인

<!-- codex-turn:01a064f3-46bf-7c82-9980-fecb01b9f406 -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`
- Jira: 관련 Billing 구현 이슈는 `TMI-120`이며 Jira mutation은 수행하지 않았다.
- 작업 목표: “Billing을 나중에 수정해야 하는가”를 문서와 실제 `TMI-120` 구현 동작으로 나눠 확인한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. Identity·Billing 애플리케이션, 계약·계획·테스트 파일은 변경하지 않았다.
- 확인된 구현: Billing `OwnerRebindService`는 PHONE_REJOIN과 USER_MERGED 모두 source의 active `BillingSubjectLink` owner를 target으로 CAS 이전한다. nonterminal AttemptGroup과 active Session이 있으면 `SubjectOwnerRebind` legacy-source fence도 생성해 source userId로 늦게 도착하는 Learning Core 상태 event를 한시 허용한다.
- 영향 분석: phone 재가입 event를 Learning Core에 보내지 않으면 target은 Billing owner가 되지만 과거 AttemptGroup·시험 결과는 Learning Core의 source에 남을 수 있다. 특히 `RETAKE_AVAILABLE`처럼 기존 attempt 연속성이 필요한 권리를 target이 Billing에서 승인받아도 Learning Core 소유권과 어긋날 수 있다.
- 권고 보정: `UserMerged`는 canonical account merge이므로 현재 owner 이전·fence를 유지한다. `TrialOwnerRebindApproved`는 기존 AttemptGroup이 없는 미사용 subject만 owner 이전하고, AttemptGroup·retake·결과 이력이 있으면 Claim·사용량과 source owner를 그대로 둔 채 event inbox를 성공 NOOP로 commit한다. 이 경우 새 무료권을 만들지 않아 phone당 1회 정책도 유지된다.
- 실행한 테스트와 결과: 코드 변경 없는 정적 분석이라 Gradle 테스트를 실행하지 않았다. Billing owner-rebind service·controller·AttemptGroup/Reservation 관련 코드를 읽기 전용으로 확인했고 Identity 문서 변경에 `git diff --check`를 실행한다.
- 유지한 계약: Billing 두 inbound route, `UserMerged` 양 consumer fan-out, phone rejoin Billing-only, Claim·Grant·consumption 비복원, Learning Core phone history 자동 이전 금지를 유지한다.
- 결정사항: Billing 전체를 나중에 재구현할 필요는 없다. 다만 production activation 전에 ADR-003과 PHONE_REJOIN history-bound 처리의 제한된 code/test 보정이 필요하다. exact NOOP 조건은 계획서와 Billing ADR에 반영한 뒤 구현한다.
- 위험 요소: 문서만 수정하고 현 Billing code를 그대로 활성화하면 Billing owner와 Learning Core 과거 이력 owner가 달라져 retake·late status 흐름이 부자연스러울 수 있다. 반대로 모든 phone rejoin을 막으면 미사용권 연속성도 잃으므로 AttemptGroup 존재 여부로 범위를 제한한다.
- 다음 작업: 사용자 승인 시 Stage 7 계획서의 phone rejoin 조건을 “AttemptGroup 없음”으로 보정하고, Billing ADR-003·TMI-120에서 바꿀 exact 문서·code/test 범위를 먼저 제시한다.

## 2026-09-03 — Stage 7 Billing 담당 전달사항 정리

<!-- codex-turn:01a06545-540c-77b2-825c-f581e2449e0d -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`
- Jira: 관련 Billing 이슈는 `TMI-120`이며 Jira 조회·생성·수정·댓글·상태 변경은 수행하지 않았다.
- 작업 목표: Identity Stage 7에서 Billing 담당자에게 전달해야 할 확정 정책, 코드 보정 범위, 유지 계약과 검증 조건을 실행 가능한 handoff로 정리한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. Identity·Billing 애플리케이션과 계약·계획·테스트 파일은 변경하지 않았다.
- 확인 내용: Billing `OwnerRebindService`는 현재 `PHONE_REJOIN`과 `USER_MERGED` 모두 active owner link를 target으로 이전하고 nonterminal AttemptGroup에 legacy-source fence를 만든다. ADR-003은 phone rejoin도 Learning Core로 전달하는 전제를 포함한다.
- 전달 정책: `UserMerged`만 Billing·Learning Core 두 consumer로 전달하고, `TrialOwnerRebindApproved`는 Billing-only로 제한한다. `PHONE_REJOIN`은 AttemptGroup·retake·결과 이력이 없는 미사용 subject만 이전하고 history-bound subject는 source owner와 기존 권리를 유지한 성공 `NOOP`로 처리한다.
- 유지 계약: Billing route와 strict decoder, event inbox 멱등성, current owner CAS, Claim·Grant·ledger·consumption 불변, phone당 무료시험 1회, `UserMerged` fence, consumer-first/flag-off rollout을 유지한다.
- 문서 보정: ADR-003·PLAN-006의 Learning Core phone route·자동 history migration·모든 event 양 consumer delivery 전제를 제거하고 Identity→Billing SigV4와 Identity→Learning Core workload JWT를 destination별로 구분한다.
- 실행한 테스트와 결과: 코드 변경 없는 분석·전달문 작성이라 Gradle 테스트를 실행하지 않았다. Billing ADR-003과 `OwnerRebindService`·관련 테스트를 읽기 전용으로 대조한 뒤 `git diff --check`를 실행한다.
- 위험 요소: history-bound `PHONE_REJOIN`에서 Billing owner만 target으로 옮기면 Learning Core의 source-owned 이력과 불일치한다. 성공 `NOOP`의 exact history 판정 범위와 조회 성능은 Billing 구현 전에 테스트로 고정해야 한다.
- 다음 작업: 사용자가 Billing 전달 정책을 승인하면 Identity Stage 7 계획서에 history-bound `NOOP`을 반영하고, Billing ADR-003·PLAN-006·service/test exact 변경안을 사용자에게 먼저 제시한다.

## 2026-09-03 — Billing Stage 7 전달문 제공

<!-- codex-turn:01a064f5-95b3-7da2-a2e8-3af392c3fbcd -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`
- Jira: 관련 Billing 작업은 `TMI-120`이며 Jira 조회·생성·수정·댓글·상태 변경을 수행하지 않았다.
- 작업 목표: Billing 담당 작업에 그대로 전달할 수 있도록 Stage 7 이벤트 목적지, `PHONE_REJOIN` 보정, 유지 계약, 문서·테스트·배포 조건을 정리한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. Identity와 Billing 애플리케이션 코드, Billing ADR·계획·테스트는 변경하지 않았다.
- 구현 내용: 코드 구현은 없다. `UserMerged`는 Billing·Learning Core 양쪽에 전달하고 기존 owner CAS·legacy-source fence를 유지하며, `TrialOwnerRebindApproved`는 Billing-only로 전달하도록 handoff를 확정했다.
- Billing 보정 범위: `PHONE_REJOIN`은 terminal 상태를 포함한 AttemptGroup이 하나도 없는 미사용 subject만 owner를 target으로 이전한다. 이력이 있으면 owner·Claim·Grant·ledger·consumption을 유지하고 fence 없이 inbox `NOOP`, `affectedSubjectCount=0`, HTTP 204로 처리한다.
- 문서·인증: ADR-003·PLAN-006에서 Learning Core phone route와 phone 기반 history migration, 모든 event의 양 consumer fan-out 전제를 제거한다. Identity→Billing은 VPC Lattice AWS_IAM·SigV4, Identity→Learning Core `UserMerged`는 기존 workload JWT를 유지한다.
- 실행한 테스트와 결과: 분석·전달문과 작업 기록만 변경했으므로 Gradle 테스트는 실행하지 않았다. Billing의 현재 `OwnerRebindService`, ADR-003과 관련 테스트 범위를 읽기 전용으로 확인했으며 `git diff --check`를 실행한다.
- 유지한 계약: 기존 Billing inbound route, strict decoder, event digest·inbox 멱등성, `UserMerged` 처리, phone당 무료시험 1회, Claim·Grant·consumption 불변, consumer-first·feature-flag-off 배포를 유지한다.
- 위험 요소: 현재 Billing 조회는 nonterminal AttemptGroup 중심이므로 완료·재응시 이력을 빠뜨리지 않는 any-history 조회와 index/성능 검증이 필요하다. Billing 작업 트리의 기존 `TMI-120` 변경을 파일 단위로 덮어쓰면 안 된다.
- 다음 작업: 사용자 승인에 따라 Identity Stage 7 계획서에 history-bound `NOOP` 계약을 반영하고 Billing 측 exact ADR·service·test 변경안을 별도 승인 후 수행한다.

## 2026-09-03 — Stage 7 위험 4.3·4.4·4.7 설명

<!-- codex-turn:stage7-risks-20260903 -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`
- Jira: 관련 Billing 작업은 `TMI-120`이며 Jira 작업은 수행하지 않았다.
- 작업 목표: Stage 7 계획서의 pre-cutover 자동 복구 제한, FIFO head-of-line blocking, pseudonymous data 보존 위험을 사용자 관점에서 설명한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·계약·계획 본문과 다른 저장소는 변경하지 않았다.
- 구현 내용: 코드 구현은 없다. 4.3은 exact predecessor lineage가 없는 과거 CLEANED 계정의 owner를 추측하지 않는 규칙, 4.4는 앞 sequence가 해결되기 전 뒤 sequence를 보내지 않는 consumer-wide FIFO의 대가, 4.7은 fingerprint·alias·lineage도 개인과 연결 가능한 가명정보이므로 raw phone 비저장만으로 무기한 보존이 정당화되지 않는다는 의미로 정리했다.
- 실행한 테스트와 결과: 설명·작업 기록만 변경해 Gradle 테스트를 실행하지 않았고 `git diff --check`를 실행한다.
- 유지한 계약: 과거 source 자동 추측 금지, 신규 가입 자체 허용, consumer별 순서 보장, cursor 자동 건너뛰기 금지, raw phone·credential 비저장, AVAILABLE lineage의 기능상 보존과 접근 통제를 유지한다.
- 결정사항: 세 항목은 신규 기능 요구가 아니라 Stage 7 운영 안전성의 제약과 잔여 위험이다.
- 위험 요소: pre-cutover 사용자는 미사용권 연속성을 잃을 수 있고, FIFO는 한 실패로 같은 consumer 전달 전체를 막을 수 있으며, TTL 없는 lineage는 접근 통제와 보존 목적 검토가 없으면 개인정보 위험이 누적된다.
- 다음 작업: history-bound `PHONE_REJOIN` 보정을 Stage 7 본문에 반영한 뒤, 필요하면 pre-cutover 대상 규모 집계와 승인된 migration을 별도 작업으로 설계한다.

## 2026-09-03 — Stage 7 위험 항목 사용자 설명 동기화

<!-- codex-turn:01a064f7-4a45-75d1-88c2-032bc50a7177 -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`
- Jira: 관련 Billing 작업은 `TMI-120`이며 Jira 변경은 수행하지 않았다.
- 작업 목표: Stage 7 계획서 4.3, 4.4, 4.7이 실제 사용자와 운영에 미치는 의미를 예시로 설명한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션 코드와 Stage 7 계획서 본문은 변경하지 않았다.
- 구현 내용: 코드 구현은 없다. pre-cutover 계정은 exact lineage가 없어 신규 가입만 허용하고 자동 Billing owner 이전은 하지 않으며, consumer-wide FIFO는 선행 sequence 실패 시 같은 consumer의 후속 전송을 중단하고, phone alias·lineage는 raw phone이 아니어도 개인과 연결 가능한 가명정보로 관리해야 함을 설명했다.
- 실행한 테스트와 결과: 설명·기록만 변경해 Gradle 테스트는 실행하지 않았으며 `git diff --check`를 실행한다.
- 유지한 계약: 과거 source 추측 금지, 신규 가입 허용, 동일 eventId·payload replay, cursor skip 금지, consumer별 독립 sequence, raw phone·fingerprint payload 비로그와 AVAILABLE lineage 접근 통제를 유지한다.
- 결정사항: 4.3·4.4·4.7은 기능 확장이 아니라 잘못된 권리 이전, 순서 역전과 가명정보 오남용을 막기 위한 안전 경계다.
- 위험 요소: pre-cutover 정상 사용자가 미사용권을 이어받지 못할 수 있고 Billing 채널에 head-of-line blocking이 발생할 수 있으며 TTL 없는 AVAILABLE lineage는 정책·접근 통제 없이 장기 누적될 수 있다.
- 다음 작업: Stage 7 본문에 Billing history-bound `PHONE_REJOIN NOOP` 보정을 반영할지 사용자 결정을 받아 진행한다.

## 2026-09-03 — 동일 phone 재가입의 기록·무료시험 UX 비대칭 검토

<!-- codex-turn:01a064fa-6bc0-7180-bef3-c40b1e1f01f5 -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`
- Jira: 관련 Billing 작업은 `TMI-120`이며 Jira 변경은 수행하지 않았다.
- 작업 목표: 동일 전화번호 재가입 시 과거 시험 기록은 보이지 않지만 무료시험 재지급은 막히는 정책이 사용자에게 부자연스러운지 검토한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·계약·계획 본문은 변경하지 않았다.
- 구현 내용: 코드 구현은 없다. 학습 기록은 계정·개인정보 소유권에 속하고 무료시험 eligibility는 phone-scoped 프로모션 남용 방지에 속하므로 기술적으로 다른 보존 정책을 가질 수 있지만 사용자에게는 비대칭으로 보인다고 판단했다.
- 상태별 판단: 시험 이력이 없는 미사용 혜택은 owner rebind로 이어주고, 이미 완료·소비한 혜택은 재지급하지 않는다. 시작 후 중단·재응시 상태는 phone proof만으로 기록을 노출할 수 없고 Learning Core 이전 없이 Billing 권리만 옮길 수도 없어 현재 `NOOP`에서 UX 공백이 가장 크다.
- 실행한 테스트와 결과: 정책 분석과 기록만 변경해 Gradle 테스트를 실행하지 않았으며 `git diff --check`를 실행한다.
- 유지한 계약: phone당 무료시험 1회, phone proof 기반 Learning Core history 자동 이전 금지, Claim·Grant·consumption 비복원과 신규 가입 허용을 유지한다.
- 결정사항: 현 정책을 유지하려면 탈퇴 전 경고와 재가입 후 `동일 번호 무료 혜택 사용 완료` 안내가 필수다. 중단·재응시 사용자는 strong-proof 복구 또는 운영 지원 경로를 별도 설계하는 것이 권장된다.
- 위험 요소: 안내 없이 적용하면 사용자는 데이터는 삭제됐는데 혜택 사용 기록만 선택적으로 보존됐다고 느낄 수 있다. 반대로 재가입마다 무료시험을 재지급하면 반복 탈퇴·가입으로 phone당 1회 정책을 우회할 수 있다.
- 다음 작업: 사용자가 정책 방향을 승인하면 Stage 7 계획서에 상태별 UX 계약과 history-bound 중단 사례의 후속 strong-proof 복구 요구를 반영한다.

## 2026-09-03 — phone 재가입 시 RETAKE_AVAILABLE 승계 가능성 재검토

<!-- codex-turn:retake-rejoin-20260903 -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`
- Jira: 관련 Billing 작업은 `TMI-120`이며 Jira 변경은 수행하지 않았다.
- 작업 목표: 탈퇴 전 `RETAKE_AVAILABLE`이었던 무료시험 권리가 동일 phone 재가입 시 왜 자동 승계되지 않는다고 판단했는지 양 서비스 실제 예약 흐름으로 재검증한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. Identity·Billing·Learning Core 애플리케이션과 Stage 7 계획서 본문은 변경하지 않았다.
- 확인된 구현: Billing `ReserveService`는 `RETAKE_AVAILABLE` AttemptGroup을 찾아 같은 group·mockExam의 `REPLACEMENT`를 반환한다. Learning Core `ExamSessionManager`는 현재 userId의 in-progress/retake session을 찾아야 expected group을 준비하며, `BillingExamCreationSaga`는 로컬에서 replacement를 예상하지 않았는데 Billing이 `REPLACEMENT`를 반환하면 contract mismatch로 실패시킨다.
- 분석 결과: phone rejoin에서 Billing owner만 target으로 바꾸고 Learning Core 과거 세션을 이전하지 않으면 권리가 Billing에는 남아도 target이 정상적으로 사용할 수 없다. 이전에 권고한 history-bound `NOOP`은 이 split-brain을 막는 보수적 처리이며 `RETAKE_AVAILABLE` 권리 자체가 소멸한 것은 아니다.
- 개선 방향: 과거 답안·결과를 이전하지 않으면서 target에게 exact attemptGroup·mockExam의 일회성 retake continuity만 제공하는 별도 projection/event를 설계하면 권리를 승계할 수 있다. 이는 기존 `TrialOwnerRebindApproved` Billing-only 계약과 별개의 명시적 cross-service 계약·보안 검토가 필요하다.
- 실행한 테스트와 결과: 코드 변경 없는 정적 흐름 검증이라 Gradle 테스트를 실행하지 않았다. Billing `ReserveService`·`AttemptGroupRepository`와 Learning Core `ExamSessionManager`·`BillingExamCreationSaga`를 읽기 전용으로 대조하고 `git diff --check`를 실행한다.
- 유지한 계약: phone당 무료시험 1회, 기존 답안·결과의 phone 기반 자동 이전 금지, Claim·Grant·consumption 비복원과 신규 가입 허용을 유지한다.
- 결정사항: 사용자 기대상 `RETAKE_AVAILABLE` 승계가 자연스럽다는 지적은 타당하다. Stage 7의 history-bound 전면 `NOOP`을 최종 확정하기 전에 sanitized retake continuity를 Stage 7 또는 별도 후속 단계로 포함할지 결정해야 한다.
- 위험 요소: Billing owner만 옮기면 INITIAL/REPLACEMENT 계약 불일치가 발생한다. 반대로 기존 Learning Core 시험 전체를 phone proof로 이전하면 번호 재할당 시 개인정보 노출 위험이 있다.
- 다음 작업: 사용자가 원하면 과거 기록은 이전하지 않고 재응시 가능 상태만 안전하게 승계하는 event·projection·일회성 소비 계약안을 작성한다.

## 2026-09-03 — RETAKE_AVAILABLE 승계 설명 종료 기록

<!-- codex-turn:01a064fc-ca45-7452-b71a-ff43a5b4f181 -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`
- Jira: 관련 Billing 작업은 `TMI-120`이며 Jira 변경은 수행하지 않았다.
- 작업 목표: 동일 phone 재가입에서 `RETAKE_AVAILABLE`이 본질적으로 남은 권리인데도 현재 단순 owner rebind만으로 사용할 수 없는 원인을 설명하고 개선 방향을 정리한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 세 서비스 애플리케이션과 계획서 본문은 변경하지 않았다.
- 확인된 동작: Learning Core는 target user 소유의 기존 ExamSession이 있어야 replacement source와 expected attemptGroup을 준비한다. phone 재가입에서는 해당 세션이 source user에 남으므로 Learning Core는 INITIAL을 예상하지만 Billing은 retained `RETAKE_AVAILABLE` group을 보고 REPLACEMENT를 반환해 계약 불일치가 발생한다.
- 분석 결과: 기존 history-bound `NOOP` 권고는 split-brain 방지책일 뿐 재응시 자격이 사라졌다는 의미가 아니다. 사용자 기대상 기록 전체를 넘기지 않으면서 남은 재응시 자격은 승계하는 편이 자연스럽다.
- 개선 방향: target 전용 일회성 continuity marker가 exact attemptGroup·mockExam을 제공하고 과거 답안·결과·피드백 조회 권한은 부여하지 않는 별도 versioned event/projection을 검토한다.
- 실행한 테스트와 결과: 코드 변경 없는 정적 분석이라 Gradle 테스트는 실행하지 않았다. Billing과 Learning Core 흐름을 읽기 전용으로 대조했으며 `git diff --check`를 실행한다.
- 유지한 계약: 무료시험 재지급 금지, phone당 1회, 과거 답안·결과의 phone 기반 자동 이전 금지, Claim·Grant·consumption 비복원을 유지한다.
- 결정사항: Stage 7의 모든 AttemptGroup 존재 시 전면 `NOOP`은 최종 확정하지 않고 `RETAKE_AVAILABLE` 전용 승계 계약과 비교해야 한다.
- 위험 요소: 기존 v1을 묵시적으로 확장하거나 marker가 과거 기록 조회에 사용되면 개인정보 경계가 깨질 수 있다. Billing owner 이전과 Learning Core marker 생성의 전달 순서·응답 유실도 멱등하게 처리해야 한다.
- 다음 작업: `RETAKE_AVAILABLE` 전용 continuity event의 producer, payload, 소비 Transaction, 일회성·TTL·순서·실패 정책을 설계하고 Stage 7 범위 반영 여부를 결정한다.

## 2026-09-03 — phone 재가입 미완료 group owner rebind 수정 검토

<!-- codex-turn:01a0651e-b4ff-7551-a760-270b8223d6f4 -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`; 검토한 Billing `develop`은 PR #7 merge commit `9239a62`, 구현 commit `e3b2842`다.
- Jira: Billing `TMI-120` 관련 구현을 읽기 전용으로 확인했으며 Jira mutation은 수행하지 않았다.
- 작업 목표: 재가입 시 과거 시험 기록은 연결하지 않고, 미완료 무료시험 group의 current owner를 새 user로 바꿔 재응시시키도록 수정한 내용이 세 서비스 흐름에서 실제 동작하는지 검토한다.
- 변경 파일: 이번 검토가 직접 변경한 파일은 `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`뿐이다. Billing·Learning Core 애플리케이션과 Identity Stage 7 계획서는 수정하지 않았다.
- 확인된 Billing 구현: `TrialOwnerRebindApproved`는 Billing-only이고 phone group 없음·OPEN·RETAKE_AVAILABLE은 owner CAS APPLIED, GRADING은 503 pending, COMPLETED는 owner/fence 불변 NOOP다. 기존 Claim·Grant·consumption과 source Learning Core 기록은 변경하지 않는다.
- 용어 확인: Billing `AttemptGroup`에는 userId owner field가 없다. 실제 변경 대상은 group이 참조하는 stable `subjectRefId`의 `BillingSubjectLink.userId`이며, 그 결과 target이 같은 group에 대한 reserve 권한을 얻는 구조다.
- 발견한 차단점: Learning Core `ExamSessionManager`는 target user의 기존 Session이 없으면 INITIAL을 준비하고 `BillingExamCreationSaga`는 예상하지 않은 REPLACEMENT 응답을 거절한다. Billing `ReserveService`도 요청 mockExamId가 기존 group과 다르면 state conflict를 반환하므로 target이 old group metadata를 모르는 현 wire에서는 정상 재응시가 보장되지 않는다.
- 문서 불일치: Billing ADR-003·PLAN-006은 새 상태표와 target replacement를 반영했지만 Identity Stage 7 계획서는 아직 미사용권 중심 문구와 phone replacement 처리 부재를 유지한다.
- 실행한 테스트와 결과: Billing `./gradlew clean test`는 124개 중 Docker가 필요한 Mongo integration suite 4개의 initializationError로 실패했고 나머지 120개는 실패가 없었다. Docker 비의존 `OwnerRebindServiceTest`와 `OwnerRebindEventControllerTest` 집중 실행은 성공했다. `git diff --check`를 실행한다.
- 유지한 계약: phone당 무료시험 1회, COMPLETED 기록·답안·피드백 비이전, 새 무료 unit 미생성, Claim·Grant·consumption 불변, source→target exact owner CAS와 event 멱등성을 유지한다.
- 결정사항: Billing 변경은 의도한 상태 판정을 구현했지만 cross-service 기능 완료로 판단할 수 없다. production 활성화 전 Learning Core의 target replacement 채택과 old group mockExam discovery 계약이 필요하다.
- 위험 요소: 현재 그대로 활성화하면 owner rebind는 성공하지만 target 시험 생성이 계약 오류로 실패해 권리가 존재하면서 사용할 수 없는 상태가 된다. source Session 정리와 늦은 event fencing도 staging E2E에서 확인해야 한다.
- 다음 작업: Identity Stage 7 계획서를 Billing 상태표에 맞게 고치고, Learning Core가 과거 결과를 노출하지 않으면서 Billing의 existing group·mockExam을 안전하게 채택하는 exact wire/Transaction 계획을 작성한다.

## 2026-09-03 — Billing·Learning Core phone continuation 완료 여부 재검토

<!-- codex-turn:01a065a0-022a-74c2-9a73-edc96741a53e -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`; Billing `develop` merge commit `7138810`/구현 `b61ebb9`; Learning Core `feat/TMI-122-phone-rejoin-continuation` commit `233b63e`.
- Jira: Billing `TMI-120`, Learning Core `TMI-122` 관련 구현을 읽기 전용으로 확인했으며 Jira mutation은 수행하지 않았다.
- 작업 목표: Billing과 Learning Core의 phone 재가입 continuation 수정이 앞서 발견한 기존 mockExam discovery와 unexpected REPLACEMENT 차단점을 닫았는지 확인하고, 다음 작업이 Identity만인지 범위를 판정한다.
- 변경 파일: 이번 검토가 직접 변경한 파일은 Identity `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`다. 세 저장소의 애플리케이션·계약·테스트 코드는 변경하지 않았다.
- Billing 확인: owner CAS가 transition reason/id를 함께 저장하고 Learning Core 전용 `POST /internal/v1/reservations/continuations/phone`이 OPEN/RETAKE_AVAILABLE group의 exact continuation id, attemptGroupId, mockExamId를 반환한다. reserve는 continuation 3-field echo와 current owner epoch/group/mock을 Transaction에서 재검증하고 response/status에 context를 보존한다.
- Learning Core 확인: 새 target에 ExamSession이 전혀 없을 때만 continuation을 조회하고 결과를 `ExamCreationOperation`에 immutable snapshot한다. phone reserve·status의 reason/id/group/mock을 strict 검증한 뒤 source Session을 이전하지 않고 target userId의 새 examId를 같은 group의 REPLACEMENT로 생성하며 응답 유실은 status-first로 복구한다.
- 검토 결과: 앞서 발견한 “target이 old mockExamId를 모름”과 “예상하지 않은 REPLACEMENT 거절” 문제는 명시적 discovery+echo 계약으로 닫혔다. phone continuation 코드에서 추가 blocking finding은 확인하지 못했다.
- 실행한 테스트와 결과: Billing continuation 관련 service/controller/decoder/security 집중 테스트는 성공했다. Billing `./gradlew clean test`는 136개 중 Docker daemon 미가동으로 Testcontainers Mongo integration 4개가 initializationError였고 나머지에는 실패가 없었다. Learning Core `./gradlew clean test`는 457개 전체 성공했다. Identity `git diff --check`를 실행한다.
- 유지한 계약: phone당 무료시험 1회, COMPLETED NOOP, GRADING pending, Claim·Grant·consumption 비복원, source 시험·답안·결과 owner 비이전, target 새 Session 생성, idempotency/status-first와 feature flag 기본 OFF를 유지한다.
- 남은 범위: Learning Core `TMI-122`는 아직 develop 미병합이다. phone rejoin vertical slice는 merge 뒤 Identity Stage 7-A/7-C의 SigV4, exact lineage, TrialOwnerRebindApproved Billing-only capture/delivery가 남는다. Stage 7 전체에는 Learning Core `UserMerged` consumer와 Lattice/IAM·Mongo replica-set·staging E2E/canary도 남는다.
- 위험 요소: Billing Docker integration 4개를 실제 daemon/replica set에서 재검증해야 하며 continuation route IAM, 양 서비스 flag 순서, owner rebind→discovery→reserve 사이 stale epoch와 응답 유실을 staging에서 확인해야 한다.
- 예상 밖 변경: Billing worktree는 clean이다. Learning Core에는 현재 feature commit 외 기존 문서·draw.io working tree 변경이 있으나 이번 검토에서 수정하지 않았다. Identity의 기존 `AGENTS.md`와 Stage 7 문서/작업 기록 변경도 보존했다.
- 다음 작업: Learning Core TMI-122를 develop에 merge한 뒤 Identity Stage 7 계획서를 새 continuation 계약으로 갱신하고 Identity 구현 Jira를 승인받아 7-A와 7-C를 구현한다. 전체 Stage 7 완료 전 Learning Core UserMerged consumer와 staging E2E를 별도로 닫는다.

## 2026-09-03 — Stage 7 계획서 phone continuation 계약 동기화

<!-- codex-turn:8c573502-d2c3-44cd-a775-d70221f6a6bb -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`; 기준 Billing `develop` PR #8 merge commit `7138810`/구현 `b61ebb9`; 기준 Learning Core `feat/TMI-122-phone-rejoin-continuation` commit `233b63e`.
- Jira: Billing `TMI-120`, Learning Core `TMI-122`를 구현 근거로 참조했으며 Jira 조회·생성·수정·댓글·상태 변경은 수행하지 않았다.
- 작업 목표: Identity Stage 7 계획서를 Billing과 Learning Core에서 확정·구현한 phone 재가입 continuation 계약에 맞게 갱신한다.
- 변경 파일: `docs/contracts/billing-entitlement-owner-fanout-stage-7-plan.md`, `docs/contracts/firebase-auth-follow-up-implementation-order.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`.
- 구현 내용: 애플리케이션 코드는 변경하지 않았다. 계획서의 기존 “미사용권 중심” 표현을 없음·OPEN·RETAKE_AVAILABLE·GRADING·COMPLETED 상태표로 바꾸고, Billing read-only continuation discovery, exact continuation/group/mock snapshot과 reserve echo, target 새 Session, status-first 응답 유실 복구를 cross-service 계약에 추가했다.
- 책임 경계: Identity는 exact `PhoneRejoinLineage`에서 `TrialOwnerRebindApproved`를 만들어 Billing에만 durable delivery한다. Learning Core로 phone event를 보내거나 group·mockExamId를 전달하지 않으며, Learning Core가 Billing을 조회한다. source Session·답안·결과·Summary owner는 이전하지 않는다.
- 현재 상태: Billing `TMI-120` 코드·ADR 병합은 확인됐고 Learning Core `TMI-122`는 457개 테스트 통과 후 feature 브랜치에 있어 `develop` 병합이 남았다. 별도 Learning Core `UserMerged` consumer와 staging 운영 gate는 phone continuation 구현과 구분했다.
- 실행한 테스트와 결과: 문서만 변경해 Gradle 테스트는 실행하지 않았다. Markdown 계약의 구문·중복·오래된 전제는 정적 검색으로 검토하고 `git diff --check`를 실행한다.
- 유지한 계약: phone당 무료시험 1회, Claim·Grant·ledger·consumption 비복원, COMPLETED NOOP, GRADING pending, source history 비이전, `TrialOwnerRebindApproved` Billing-only, `UserMerged` Billing·Learning Core fan-out, feature flag 기본 OFF를 유지한다.
- 결정사항: phone continuation은 과거 history migration이 아니라 기존 nonterminal AttemptGroup을 이용해 target 명의의 새 Session을 처음부터 만드는 제한된 사용 상태 승계다. strong-proof 후속 범위는 source history migration으로 한정한다.
- 위험 요소: pre-cutover lineage 부재, consumer FIFO head-of-line blocking, AVAILABLE lineage 장기 보존, Billing Docker integration 4개 미검증, Learning Core 미병합, Lattice IAM·Mongo replica set·staging E2E 미검증이 남는다.
- 예상 밖 변경: 이번 작업은 위 문서 네 개만 변경했다. 기존 `AGENTS.md`와 선행 WORKLOG/CURRENT_STATE 변경은 보존했고 애플리케이션·Billing·Learning Core 파일은 수정하지 않았다.
- 다음 작업: Learning Core `TMI-122` 병합 확인 후 Identity 7-A·7-C 구현 Jira 초안을 사용자에게 먼저 제시하고 승인받아 생성한다. 이후 별도 Learning Core `UserMerged`와 Stage 7-E 운영 검증을 진행한다.

## 2026-09-03 — Stage 7 계획서 수정 turn 종료 동기화

<!-- codex-turn:01a065a4-19a9-7830-acd1-c817b971c945 -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`.
- Jira: Billing `TMI-120`과 Learning Core `TMI-122`를 구현 근거로 참조했으며 Jira mutation은 수행하지 않았다.
- 작업 목표: 종료 훅 기준으로 이번 Stage 7 계획서 수정 작업의 식별자와 최종 상태를 작업 기록에 동기화한다.
- 변경 파일: `docs/contracts/billing-entitlement-owner-fanout-stage-7-plan.md`, `docs/contracts/firebase-auth-follow-up-implementation-order.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`.
- 구현 내용: 애플리케이션 코드는 변경하지 않았다. Billing 상태표와 Learning Core phone continuation discovery·reserve echo·target 새 Session 계약을 Identity Stage 7 계획에 반영한 앞선 동일 turn 기록을 지정된 turn 식별자로 연결했다.
- 실행한 테스트와 결과: 문서 변경이므로 Gradle 테스트는 실행하지 않았고 `git diff --check`와 후행 공백 검사를 통과했다.
- 유지한 계약: `TrialOwnerRebindApproved` Billing-only, `UserMerged` 양 consumer fan-out, source history 비이전, Claim·Grant·consumption 비복원과 feature flag 기본 OFF를 유지한다.
- 결정사항: Identity는 owner event만 Billing에 전달하고 Learning Core는 Billing read-only continuation으로 exact group·mockExamId를 발견한다.
- 위험 요소: Learning Core `TMI-122`의 `develop` 병합과 `UserMerged` consumer, Lattice IAM·Mongo replica set·staging E2E 검증이 남았다.
- 다음 작업: Learning Core 병합 확인 후 Identity 7-A·7-C Jira 초안을 제시하고 승인 뒤 생성·구현한다.

## 2026-09-03 — Stage 7 Identity 예정 구현 설명

<!-- codex-turn:01a065ab-426e-7940-9744-fc1ab4c5312f -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`.
- Jira: Billing `TMI-120`과 Learning Core `TMI-122`를 구현 배경으로 참조했으며 Jira mutation은 수행하지 않았다.
- 작업 목표: 수정된 Stage 7 계획서를 기준으로 다음 Identity 구현의 목적, 사용자 흐름, 서비스별 책임과 안전장치를 설명한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션과 계약·계획 본문은 변경하지 않았다.
- 구현 내용: 코드 구현은 아직 시작하지 않았다. 예정 범위는 7-A phone eligibility Billing 전송의 Bearer JWT→SigV4 교체와 bounded Retry-After, 7-C exact `PhoneRejoinLineage`, immutable owner event core, consumer별 durable delivery·sequence·retry·dead-letter·feature flag다.
- 사용자 흐름: 동일 phone 재가입 시 exact lineage가 한 건이면 `TrialOwnerRebindApproved`를 Billing에만 발행한다. Billing은 group 없음·OPEN·RETAKE_AVAILABLE·GRADING·COMPLETED를 판정하고, Learning Core는 Billing의 read-only continuation을 통해 같은 group·mockExamId의 target 새 Session을 만든다. source 기록은 이전하지 않는다.
- 유지한 계약: phone당 무료시험 1회, Claim·Grant·consumption 비복원, COMPLETED NOOP, GRADING retry, phone event Billing-only, `UserMerged` 양 consumer fan-out, raw phone 비저장과 기본 OFF flag를 유지한다.
- 실행한 테스트와 결과: 설명과 기록만 변경해 Gradle 테스트는 실행하지 않았으며 `git diff --check`를 실행한다.
- 결정사항: 가입 자체와 과거 권리 자동 이전을 분리한다. lineage가 없거나 모호해도 가입은 성공시키되 owner event는 만들지 않고 reconciliation 대상으로 남긴다.
- 위험 요소: Learning Core `TMI-122`의 `develop` 병합, 별도 `UserMerged` consumer, staging IAM·Mongo replica set·E2E가 완료되기 전에는 publisher를 활성화할 수 없다.
- 다음 작업: Identity 구현 Jira 범위를 7-A와 7-C1~C3으로 제시하고 사용자 승인 뒤 생성·구현한다.

## 2026-09-03 — 복수 무료시험 권리와 phone lineage cardinality 경계 검토

<!-- codex-turn:01a065ad-ff2f-7460-ba67-1269c6bf8c11 -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`.
- Jira: 관련 Billing `TMI-120`, Learning Core `TMI-122`를 문맥으로만 참조했으며 Jira mutation은 수행하지 않았다.
- 작업 목표: 향후 무료 모의고사 응시권이 여러 종류로 늘어날 때 Stage 7의 AVAILABLE lineage 2건 이상 판정을 그대로 사용할 수 있는지 검토한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·계약·계획 본문은 변경하지 않았다.
- 분석 결과: lineage 수는 Billing 응시권 수가 아니라 동일 `consumerScopeId`에서 가능한 exact predecessor account 경로 수다. 서로 다른 무료시험 프로그램이 각기 다른 scope를 사용하면 scope별 한 lineage와 한 event로 독립 확장할 수 있고, 서로 다른 scope의 여러 건은 모순이 아니다.
- 확장 경계: 여러 프로그램을 같은 `consumerScopeId` 안에 넣거나 event 하나가 여러 Billing subject를 선택해야 한다면 v1 의미를 묵시적으로 넓히지 않고 stable benefit/program key, bounded fan-out, partial failure와 멱등성 계약을 별도 version 또는 event로 설계해야 한다.
- 실행한 테스트와 결과: 정책 분석과 기록만 변경해 Gradle 테스트는 실행하지 않았으며 `git diff --check`를 실행한다.
- 유지한 계약: Identity는 Billing 권리 목록을 소유하거나 추측하지 않고 exact source→target lifecycle과 scope만 승인한다. 같은 scope에서 AVAILABLE predecessor가 2건 이상이면 자동 이전을 중단하되 가입은 허용한다.
- 결정사항: 현재 단일 `FREE_EXAM_ONCE` MVP에는 scope별 0/1/2+ gate를 유지할 수 있다. 구현 시 “전체 권리 중 2건”이 아니라 “동일 scope의 predecessor lineage 2건”임을 코드·테스트 이름에 고정하는 것이 필요하다.
- 위험 요소: scope를 단순 환경 문자열이나 모든 무료 혜택의 공통 상수로 사용하면 향후 상품 추가 시 구분할 수 없다. 새 프로그램이 생기기 전에 scope naming·stable key와 Billing subject cardinality를 별도 ADR에서 확정해야 한다.
- 다음 작업: 사용자가 승인하면 Stage 7 계획서의 2건 이상 문구를 동일 consumer scope의 predecessor ambiguity로 명확히 하고 future multi-benefit 확장 규칙을 추가한다.

## 2026-09-03 — 반복 탈퇴·재가입 lineage chain 검토

<!-- codex-turn:01a065b3-6f01-7053-a34c-82b41ce10140 -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`; Billing `develop` 구현을 읽기 전용으로 확인했다.
- Jira: 관련 Billing `TMI-120`, Learning Core `TMI-122`를 문맥으로만 참조했으며 Jira mutation은 수행하지 않았다.
- 작업 목표: 동일 사용자가 탈퇴와 재가입을 여러 번 반복할 때 과거 계정 lineage가 여러 건 조회돼 정상 사용자를 ambiguity로 오판하는지 검토한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·계약·계획 본문은 변경하지 않았다.
- 분석 결과: A 탈퇴 lineage가 B 가입에서 `CONSUMED`되고 B 탈퇴가 새 `AVAILABLE` lineage를 만들면, C 가입 시 fingerprint로 A·B 과거 alias가 모두 조회돼도 동일 scope의 AVAILABLE은 B 한 건뿐이다. 과거 User tombstone과 CONSUMED lineage가 여러 건 남는 것은 정상이며 자동 이전 후보 수와 다르다.
- 순서 안전성: A→B delivery가 아직 PENDING인 동안 B→C가 생겨도 Billing consumer sequence가 A→B 이후 B→C만 발행하게 한다. 선행 event가 DEAD_LETTER이면 후행 chain도 정지해 운영 해결 전 잘못된 owner 도약을 막는다.
- Billing 확인: 선행 A→B가 COMPLETED 때문에 NOOP여서 owner가 A에 남으면 후속 B→C는 source B의 active subject link가 없어 NOOP로 수렴한다. 새 무료권이나 잘못된 owner 이전은 만들지 않는다.
- 실행한 테스트와 결과: 코드 변경 없는 정적 분석으로 Gradle 테스트는 실행하지 않았고 Billing `OwnerRebindService`의 source owner 조회·COMPLETED NOOP 처리 순서를 확인했으며 `git diff --check`를 실행한다.
- 유지한 계약: 동일 scope에서 다음 successor가 소비할 AVAILABLE predecessor는 한 건이어야 하고, 과거 CONSUMED lineage는 후보에서 제외한다. 가입은 ambiguity 때문에 막지 않으며 자동 권리 이전만 fail-closed한다.
- 결정사항: 반복 탈퇴·재가입 자체는 2건 이상 모순을 만들지 않는다. 다만 현재 계획에 A→B→C 반복 chain, capture OFF·pre-cutover·dead-letter 경계 테스트를 명시적으로 추가해야 한다.
- 위험 요소: `(sourcePhoneIdentityId, consumerScopeId)` unique만으로 전체 phone chain의 AVAILABLE 한 건을 DB 차원에서 보장하지는 않는다. 상태 CAS·조회 gate와 반복 chain 테스트가 누락되면 비정상 다중 AVAILABLE이 reconciliation으로 빠질 수 있다.
- 다음 작업: 사용자 승인 시 Stage 7 계획서에 AVAILABLE/CONSUMED 반복 chain 예시와 A→B→C, pending/dead-letter/completed NOOP 테스트를 추가한다.

## 2026-09-03 — Stage 7 반복 재가입·복수 benefit 확장 계획 반영

<!-- codex-turn:01a065b3-6f01-7053-a34c-82b41ce10140 -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`.
- Jira: 관련 Billing `TMI-120`, Learning Core `TMI-122`를 계획 근거로 참조했으며 Jira mutation은 수행하지 않았다.
- 작업 목표: 사용자 승인에 따라 반복 탈퇴·재가입에서 과거 lineage가 여러 건 남는 정상 상태와 동일 scope 다중 AVAILABLE 모순을 구분하고 향후 복수 무료시험 확장 경계를 Stage 7 계획에 반영한다.
- 변경 파일: `docs/contracts/billing-entitlement-owner-fanout-stage-7-plan.md`, `docs/contracts/firebase-auth-follow-up-implementation-order.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`.
- 구현 내용: 애플리케이션 코드는 변경하지 않았다. A 탈퇴→B 가입→B 탈퇴→C 가입에서 predecessor lineage가 AVAILABLE→CONSUMED로 전이되고 successor 탈퇴가 새 AVAILABLE을 만드는 단일 chain, consumer sequence 기반 pending/dead-letter 순서, COMPLETED NOOP 후속 NOOP를 계획에 추가했다.
- 복수 benefit 경계: 0/1/2+ 판정은 전체 권리 수가 아니라 동일 `consumerScopeId`의 AVAILABLE predecessor 수에 적용한다. 서로 다른 benefit scope는 독립 처리하고, 같은 scope에서 여러 subject를 선택적으로 이전해야 하는 확장은 stable program key와 별도 versioned 계약으로 분리한다.
- 테스트 계획: A→B→C 정상 chain, 과거 CONSUMED 제외, 동일 scope AVAILABLE 2건 reconciliation, 서로 다른 scope 독립 event, 선행 PENDING 순서, DEAD_LETTER 차단, COMPLETED NOOP chain을 필수 매트릭스에 추가했다.
- 실행한 테스트와 결과: 문서만 변경해 Gradle 테스트는 실행하지 않았으며 `git diff --check`와 후행 공백 검사를 실행한다.
- 유지한 계약: 최신 RELEASED alias 추측 금지, 가입 허용·자동 이전 fail-closed, phone당 benefit scope별 정책, source history 비이전, consumer FIFO와 기본 OFF flag를 유지한다.
- 결정사항: 과거 User tombstone과 CONSUMED lineage가 여러 건 남는 것은 정상이다. 동일 scope에서 다음 successor가 사용할 AVAILABLE predecessor가 여러 건일 때만 모순으로 처리한다.
- 위험 요소: source별 unique index만으로 chain 전체의 AVAILABLE 한 건을 물리적으로 보장하지 못하므로 CAS, resolver fail-closed와 반복 chain 통합 테스트가 필요하다.
- 예상 밖 변경: 위 문서 네 개 외 애플리케이션·Billing·Learning Core 파일은 수정하지 않았으며 기존 사용자 변경을 보존했다.
- 다음 작업: 수정된 계획을 기준으로 Identity 7-A·7-C Jira 범위를 작성하고 사용자 승인 뒤 구현한다.

## 2026-09-03 — 반복 재가입 계획 보강 turn 종료 동기화

<!-- codex-turn:01a065b4-f219-7961-940e-16ff7ca986dd -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`.
- Jira: 관련 Billing `TMI-120`, Learning Core `TMI-122`를 계획 근거로 참조했으며 Jira mutation은 수행하지 않았다.
- 작업 목표: 종료 훅이 지정한 turn 식별자로 반복 탈퇴·재가입과 복수 benefit scope 계획 보강 결과를 최종 동기화한다.
- 변경 파일: `docs/contracts/billing-entitlement-owner-fanout-stage-7-plan.md`, `docs/contracts/firebase-auth-follow-up-implementation-order.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`.
- 구현 내용: 애플리케이션 코드는 변경하지 않았다. 동일 scope에서는 직전 AVAILABLE lineage 한 건만 successor가 소비하고, 과거 CONSUMED lineage와 서로 다른 benefit scope의 lineage는 모순으로 보지 않도록 계획과 테스트 매트릭스를 보강했다.
- 실행한 테스트와 결과: 문서 변경이므로 Gradle 테스트는 실행하지 않았고 `git diff --check`와 후행 공백 검사를 통과했다.
- 유지한 계약: A→B→C consumer sequence, DEAD_LETTER 후행 차단, COMPLETED NOOP, 가입 허용·자동 이전 fail-closed, source history 비이전을 유지한다.
- 결정사항: 동일 `consumerScopeId`의 AVAILABLE predecessor가 2건 이상인 경우에만 reconciliation 대상으로 처리한다.
- 위험 요소: source별 unique index만으로 전체 chain의 단일 AVAILABLE을 보장하지 못하므로 구현 시 CAS와 반복 chain 테스트가 필요하다.
- 다음 작업: Identity 7-A·7-C Jira 범위를 사용자에게 제시하고 승인 후 생성·구현한다.

## 2026-09-03 — Stage 7 Identity 구현 Jira 생성 전 검토

<!-- codex-turn:jira-stage7-identity-draft-20260903 -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`.
- Jira: Atlassian 공식 연동으로 TMI 프로젝트, 작업 유형과 최근 이슈를 읽기 전용 조회했다. Jira 생성·수정·댓글·상태 전환은 수행하지 않았다.
- 작업 목표: Identity Stage 7의 SigV4 transport와 owner event durable fan-out 구현을 단일 Jira 작업으로 생성하기 전에 중복 여부, 유형, 우선순위와 본문 초안을 확정한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·계약·계획 본문은 변경하지 않았다.
- 조회 결과: TMI의 생성 가능한 `작업` 유형 ID는 `10003`이며, 관련 SigV4·owner event·phone rejoin·durable fan-out 제목의 중복 이슈는 확인되지 않았다. 최근 이슈는 `TMI-122`이고 생성될 key는 서버가 확정한다.
- 제안 범위: 한 작업 안에서 7-A Billing SigV4·bounded Retry-After, 7-C1 event core·consumer delivery·sequence, 7-C2 exact phone lineage·반복 A→B→C·trial rebind capture, 7-C3 Billing/Learning Core publisher·circuit·legacy cutover를 체크리스트로 분리한다.
- 제안 Jira 속성: 프로젝트 TMI, 유형 작업, 제목 `[Identity] Billing SigV4 및 owner event durable fan-out 구현`, 우선순위 High, 기본 상태 해야 할 일, 담당자·라벨·스프린트·에픽 없음.
- 실행한 테스트와 결과: Jira 읽기 전용 검토와 문서 기록만 수행해 Gradle 테스트는 실행하지 않았으며 `git diff --check`를 실행한다.
- 유지한 계약: Jira mutation 전에 사용자에게 정확한 내용을 제시하고 승인받으며 Secret·Token·개인정보를 기록하지 않는다.
- 결정사항: 사용자의 단수 요청에 맞춰 Identity application 범위를 한 Jira로 묶고 Billing·Learning Core 소비자 재구현과 staging 운영 활성화는 제외한다.
- 위험 요소: 단일 이슈 범위가 크므로 구현 시 phase별 검증이 필요하다. Learning Core `TMI-122`는 Jira상 완료지만 로컬 `develop` 병합 여부는 구현 직전에 다시 확인해야 한다.
- 다음 작업: 사용자가 제안한 Jira payload를 승인하면 이슈 한 건을 생성하고 key·제목·유형·우선순위·상태를 재조회해 기록한다.

## 2026-09-03 — Stage 7 Identity Jira 초안 turn 종료 동기화

<!-- codex-turn:01a065b7-ae2f-7303-903e-2329b32663b8 -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`.
- Jira: Atlassian 공식 연동으로 읽기 전용 사전 검토만 수행했으며 이슈 생성·수정·댓글·상태 전환은 하지 않았다.
- 작업 목표: 종료 훅이 지정한 turn 식별자로 Stage 7 Identity Jira 생성 초안과 승인 대기 상태를 기록한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`.
- 수행 내용: TMI 프로젝트 `작업`, High, 제목 `[Identity] Billing SigV4 및 owner event durable fan-out 구현`으로 7-A·7-C1~C3을 묶은 상세 본문을 사용자에게 제시했다.
- 실행한 테스트와 결과: Jira 사전 검토와 문서 기록만 수행해 Gradle 테스트는 실행하지 않았고 `git diff --check`를 통과했다.
- 유지한 계약: Jira mutation 전 사용자 승인, feature flag 기본 OFF, Secret·Token·개인정보 비기록을 유지한다.
- 결정사항: 담당자·라벨·스프린트·에픽은 지정하지 않고 Billing·Learning Core consumer 재구현과 production 활성화는 제외한다.
- 위험 요소: 아직 사용자 최종 승인이 없어 Jira는 생성되지 않았다.
- 다음 작업: 사용자가 초안을 승인하면 Jira 한 건을 생성하고 결과를 재조회한다.

## 2026-09-03 — TMI-123 Stage 7 Identity 구현 Jira 생성

<!-- codex-turn:jira-created-tmi-123-20260903 -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`.
- Jira: `TMI-123`.
- 작업 목표: 사용자에게 사전 제시한 Stage 7 Identity 7-A·7-C 구현 범위를 TMI 프로젝트의 단일 Jira 작업으로 생성한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·계약·계획 본문은 변경하지 않았다.
- Jira 작업: 사용자 승인에 따라 `[Identity] Billing SigV4 및 owner event durable fan-out 구현`을 유형 `작업`, 우선순위 `High`, 기본 상태 `해야 할 일`로 생성했다.
- Jira 본문 목적: Billing SigV4·bounded Retry-After, owner event core·consumer delivery·sequence, exact phone lineage와 반복 A→B→C, Billing/Learning Core publisher·legacy cutover·관측성과 완료 조건을 기록했다.
- 재조회 결과: key `TMI-123`, 유형 `작업`, 우선순위 `High`, 상태 `해야 할 일`, 담당자 없음, 라벨 없음과 승인된 설명 저장을 확인했다.
- 승인 여부: 사용자가 Jira 초안을 확인한 뒤 “어 해줘”로 생성에 명시적으로 승인했다.
- 추가 mutation: 댓글, 상태 전환, 이슈 링크, 담당자·라벨·스프린트·에픽 설정은 수행하지 않았다.
- 실행한 테스트와 결과: Jira 생성과 기록만 수행해 Gradle 테스트는 실행하지 않았으며 `git diff --check`를 실행한다.
- 유지한 계약: Secret·Token·개인정보 비기록, Billing·Learning Core consumer 재구현 제외, capture·publisher 기본 OFF와 source history 비이전을 유지한다.
- 위험 요소: 단일 이슈 범위가 크고 Learning Core 병합·staging 운영 gate는 별도 확인이 필요하다.
- 다음 작업: `TMI-123` 구현 전에 이슈 본문을 읽고 현재 브랜치와 Learning Core `develop` 병합 상태를 확인한 뒤 상태 변경 승인을 별도로 받는다.

## 2026-09-03 — TMI-123 Jira 생성 turn 종료 동기화

<!-- codex-turn:01a065ba-d496-74b2-8f8b-db47b59f2b25 -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`.
- Jira: `TMI-123`.
- 작업 목표: 종료 훅이 지정한 turn 식별자로 Stage 7 Identity Jira 생성 결과를 최종 동기화한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`.
- Jira 작업: 사용자 승인에 따라 `[Identity] Billing SigV4 및 owner event durable fan-out 구현`을 `작업`, High, `해야 할 일`로 생성했다.
- 재조회 결과: 승인된 설명, 담당자 없음, 라벨 없음을 확인했다. 댓글·상태 전환·이슈 링크·스프린트·에픽 변경은 없다.
- 실행한 테스트와 결과: Jira 생성·기록 작업이므로 Gradle 테스트는 실행하지 않았고 `git diff --check`를 통과했다.
- 유지한 계약: Jira 사전 승인, Secret·Token·개인정보 비기록과 feature flag 기본 OFF를 유지한다.
- 위험 요소: Learning Core 병합 상태와 staging 운영 gate는 구현·활성화 전에 별도 확인해야 한다.
- 다음 작업: `TMI-123` 구현 착수 전 Jira 본문과 브랜치를 확인하고 상태 변경은 별도 승인 후 수행한다.

## 2026-09-03 — TMI-123 Billing SigV4 및 owner event durable fan-out 구현

<!-- codex-turn:01a065c0-4a73-70d1-927b-309d0dbdf832 -->

- 날짜: 2026-09-03
- 브랜치: `feat/TMI-123-owner-event-fanout-sigv4`
- Jira: `TMI-123`. 구현 전에 공식 Atlassian 연동으로 본문과 완료 조건을 읽었으며 이번 작업에서는 댓글·상태·필드를 변경하지 않았다.
- 작업 목표: Stage 7 계획의 Identity 7-A·7-C 범위인 Billing SigV4 transport, owner event core와 consumer별 durable delivery, exact phone rejoin lineage, 신규 UserMerged fan-out과 publisher 운영 경계를 구현한다.
- 변경 파일: `build.gradle`, `.env.example`, `README.md`, `src/main/resources/application.yml`, phone eligibility delivery port·publisher·SigV4 adapter·properties/configuration, Firebase signup·Guest upgrade·Guest merge Transaction과 configuration, withdrawal identity release Transaction/configuration, phone alias·binding revision Repository, 신규 `domain/auth/ownerevent/**`, 신규 owner event·lineage entity와 enum, `global/workload/**`, 관련 테스트, `docs/contracts/owner-event-fanout-runbook.md`, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`.
- 구현 내용: AWS SDK v2 BOM `2.29.52`의 credential·HTTP SigV4 모듈을 추가하고 Billing JSON POST를 `vpc-lattice-svcs`, 기본 region `ap-northeast-2`로 exact body와 최종 header를 서명하도록 했다. HTTPS origin과 코드 고정 route, redirect 금지, 기본 connect `PT1S`·read `PT3S`, W3C traceparent, 제한 header 제외, delta-seconds `Retry-After` 1~300 경계를 적용했다. phone eligibility Bearer audience runtime 의존은 제거했지만 Learning Core용 workload JWT 기반은 유지했다.
- durable fan-out: immutable `OwnerEventCore`, consumer별 `OwnerEventDelivery`, `OwnerEventConsumerState`를 추가했다. `UserMerged`는 Billing·Learning Core delivery를 각각 만들고 `TrialOwnerRebindApproved`는 Billing delivery만 만든다. consumer sequence allocation, exact next cursor, lease 회수, retry/dead-letter, ACTIVE/PAUSED circuit, manual head replay·resume, PUBLISHED P30D·DEAD_LETTER review P90D·core +24h retention을 구현했다. 신규 UserMerged capture가 켜지면 legacy writer와 동시에 쓰지 않으며 legacy publisher는 기존 row를 계속 처리할 수 있다.
- phone rejoin: withdrawal identity release Transaction에서 exact source withdrawal·PhoneIdentity·consumer scope·REVOKED revision으로 AVAILABLE lineage를 생성한다. direct Firebase signup과 Guest upgrade는 새 retained fingerprint에 대응하는 RELEASED alias를 조회하고, 동일 scope AVAILABLE 한 건의 source User WITHDRAWN·lifecycle CLEANED·source REVOKED·target VERIFIED revision을 재검증한 경우에만 Billing event를 생성하고 lineage를 CONSUMED로 CAS 저장한다. 0건은 신규 번호로 처리하며 2건 이상이나 gate 모순은 가입을 막지 않고 자동 이전만 `RECONCILIATION_REQUIRED`로 격리한다.
- publisher 정책: `2xx`는 delivery와 cursor를 Mongo Transaction으로 완료하고, `408`·`425`·`429`·`5xx`와 transport 실패는 동일 event로 재시도한다. server Retry-After와 local exponential jitter 중 큰 지연을 사용한다. `400`·`409`·`413`·`422`는 DEAD_LETTER, redirect·인증·권한·route 계열 오류는 delivery를 PENDING으로 되돌리고 consumer circuit을 PAUSED로 만든다. event type별 publisher flag가 꺼진 exact head는 건너뛰지 않는다.
- 설정: `PHONE_ELIGIBILITY_PUBLISHER_BASE_URL/REGION`으로 기존 endpoint/audience를 교체했다. `OWNER_EVENT_USER_MERGED_CAPTURE_ENABLED`, `OWNER_EVENT_TRIAL_REBIND_CAPTURE_ENABLED`과 Billing/Learning Core channel별 publisher flag를 추가했으며 모두 기본값은 `false`다.
- 실행한 테스트와 결과: `./gradlew compileJava` 성공, 반복 집중·전체 `./gradlew test` 성공, 최종 `./gradlew clean test` 성공. 총 621개 테스트, 실패 0, 오류 0, 건너뜀 0. `git diff --check` 성공.
- 테스트 범위: SigV4 method·path·body·region·service와 traceparent·Retry-After 경계, event wire와 consumer 집합, sequence/index/TTL, exact head publisher·retry·circuit, delivery/lineage/core retention, legacy/new capture 상호 배타, exact lineage 1건과 동일 scope 다중 AVAILABLE reconciliation, withdrawal lineage capture를 검증했다.
- 유지한 계약: 공개 API와 `BaseResponse`, 사용자 JWT RS256/JWKS·issuer·audience·UUID `sub`, Refresh Token 원문 비저장, Python AI `user_id=examId`, raw phone 비저장, `UserMerged` wire v1과 eligibility VERIFIED/REVOKED wire v1, Billing TrialClaim·Grant·consumption 소유권, source 시험 기록 비이전 계약을 유지했다. phone rejoin event를 Learning Core로 보내지 않는다.
- 결정사항: 신규 capture와 publisher는 downstream readiness와 무관하게 독립 flag로 제어한다. A→B delivery가 PENDING이어도 B→C capture는 가능하지만 Billing sequence가 순서를 강제하며, 선행 DEAD_LETTER/circuit pause를 자동 skip하지 않는다. 과거 CONSUMED lineage와 서로 다른 benefit scope는 ambiguity로 보지 않는다.
- 위험 요소: 로컬 인메모리 Mongo 테스트는 실제 replica set의 transaction write conflict와 unknown commit result를 증명하지 못한다. 실제 VPC Lattice route·task role IAM, 환경별 DNS, credential rotation, Billing/Learning Core inbox, Learning Core UserMerged consumer와 staging E2E는 미검증이다. pre-cutover CLEANED 계정의 historical lineage backfill은 포함하지 않았다.
- 배포 전 확인: Billing·Learning Core consumer를 flag OFF로 선배포하고 신규 index·replica set Transaction을 확인한다. exact task role의 승인 POST route와 반대 환경·direct endpoint 거절을 검증한 뒤 consumer ON → capture ON → channel별 publisher canary 순서로 활성화한다. backlog·sequence gap·dead-letter·circuit·privacy 지표와 운영 runbook replay를 확인한다.
- 예상 밖 변경: 이번 구현 범위 밖의 `AGENTS.md`, 기존 `docs/contracts/firebase-auth-follow-up-implementation-order.md`, 기존 WORKLOG/CURRENT_STATE 내용과 사용자가 작성한 Stage 7 계획서 변경은 보존했다. Billing과 Learning Core 저장소 파일, 기존 공개 API, commit·push는 변경하지 않았다.
- 다음 작업: 사용자가 PR 병합을 확인한 뒤 별도 승인으로 Jira 댓글·상태를 갱신한다. 그 전에는 Learning Core `UserMerged` consumer readiness와 Stage 7 staging IAM·Mongo Transaction·E2E/canary를 진행한다.

## 2026-09-03 — TMI-123 병합 및 Jira 완료 전 검증

<!-- codex-turn:01a065d9-a7ab-7bc1-894a-a74ab7f77018 -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`.
- Jira: `TMI-123`을 Atlassian 공식 연동으로 읽기 전용 조회했다. 현재 상태는 `해야 할 일`, Resolution은 미설정이며 `완료` transition ID `41`을 사용할 수 있음을 확인했다. 댓글 등록과 상태 변경은 아직 수행하지 않았다.
- 작업 목표: TMI-123을 닫기 전에 구현 PR 병합 여부, Jira 현재 상태와 정확한 완료 transition을 검증하고 사용자에게 적용할 완료 댓글과 상태 변경 내용을 제시한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·계약·계획 문서는 변경하지 않았다.
- 확인 결과: `develop`과 `origin/develop`이 commit `391b55f`로 일치하며, 해당 commit은 PR #37 `feat/TMI-123-owner-event-fanout-sigv4` 병합 commit이다. 구현 commit `ab433a3`이 `develop`에 포함된 것도 확인했다.
- 실행한 테스트와 결과: 이번 작업은 병합·Jira 상태 확인과 문서 기록만 수행해 Gradle 테스트를 다시 실행하지 않았다. 구현 turn에서 최종 `./gradlew clean test` 621개 성공이 기록되어 있으며 이번 문서 변경에는 `git diff --check`를 실행한다.
- 유지한 계약: PR 병합 확인 전 Jira 완료 금지, Jira mutation 전 변경 내용 제시와 별도 사용자 승인, Secret·Token·개인정보 비기록 규칙을 유지했다.
- 결정사항: 완료 댓글에는 Billing SigV4, bounded Retry-After, owner event durable fan-out, phone rejoin lineage, 621개 테스트 결과와 staging 운영 gate를 기록하고, 승인 후 transition ID `41`만 적용한다.
- 위험 요소: staging VPC Lattice IAM, 실제 Mongo replica set Transaction, Billing·Learning Core consumer E2E는 아직 미검증이다. Jira 완료는 코드 병합 완료를 뜻하며 운영 feature 활성화 완료를 뜻하지 않는다.
- 다음 작업: 사용자가 제시된 댓글과 `해야 할 일` → `완료` 변경을 승인하면 Jira 댓글을 등록하고 transition ID `41`을 적용한 뒤 상태와 Resolution을 재조회한다.

## 2026-09-03 — TMI-123 Learning Core UserMerged 후속 계약 검토

<!-- codex-turn:01a065df-c911-7a32-beda-eece1ecefaff -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`.
- Jira: Identity `TMI-123`, 후속 Learning Core `TMI-125`. 이번 작업에서는 Jira 조회·댓글·상태·필드 변경을 수행하지 않았다.
- 작업 목표: Learning Core가 전달한 UserMerged endpoint, 전용 workload audience/JWT, consumer별 fan-out, wire, HTTP 분류, 인증 방식, feature flag와 완료 증빙 요구를 현재 Identity `develop` 구현에 대조한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·계약·계획 문서는 변경하지 않았다.
- 5줄 결론: 요청 방향은 타당하다. 현재 Learning Core adapter의 exact path는 `/internal/v1/owners/merge/events`여서 확정 경로 `/internal/v1/events/user-merged`와 다르다. workload JWT 발급기는 `learning-core-user-withdrawn` 한 audience만 허용해 `learning-core-user-merged`를 발급할 수 없다. HTTP `415`는 현재 payload 격리가 아니라 예상하지 못한 4xx circuit pause로 처리된다. 이 세 항목과 누락된 transport·cross-consumer 증빙 테스트를 보완하기 전 TMI-123을 완료 처리하지 않는다.
- 확인된 구현: UserMerged core는 Billing·Learning Core를 required consumer로 갖고 각각 sequence/delivery를 만들며, TrialOwnerRebindApproved는 Billing-only다. UserMerged wire v1의 다섯 필드와 source/target 상이 조건, legacy/new capture 상호 배타, HTTPS·query/fragment/user-info 금지, redirect 금지, JSON Bearer 전송, 2xx 완료, 408·425·429·5xx/timeout/connection retry, 401·403 circuit pause, bounded Retry-After, 기본 false feature flag가 구현돼 있다.
- JWT 확인: 기존 provider는 RS256, workload issuer, `aud`, `sub=identity-service`, `iat`, `nbf=iat`, `exp=iat+PT2M`, 매 발급 UUID `jti`, `typ=JWT`, active `kid`를 생성하고 임의 audience를 거절한다. 다중 공개키 JWKS도 구현돼 있다. 다만 allowlist가 탈퇴 audience 하나뿐이며 owner-event 설정의 임의 문자열 audience와 구조적으로 충돌한다.
- 권고 설계: raw audience 문자열 대신 목적 enum/profile을 provider 입력으로 사용하고 `USER_WITHDRAWN`과 `USER_MERGED`를 각각 고정 audience에 매핑한다. Learning Core owner-event 설정은 `learning-core-user-merged` exact 값만 허용하거나 설정 자체를 typed profile로 대체한다. 기존 user-withdrawn 발급 계약은 유지한다.
- 테스트 공백: Learning Core adapter exact endpoint·Authorization·Content-Type·redirect·Retry-After contract test, UserMerged audience 정상 발급과 타 audience 거절, 호출마다 새 jti, 사용자 식별정보 claim 부재, HTTP 415 dead-letter, 동일 eventId/payload 재전송, Billing 성공·Learning Core 실패의 독립 재시도 증빙이 없다. fan-out cardinality와 기존 workload JWT claim·JWKS rotation 테스트는 존재한다.
- 책임 보정: 잘못된 `iss`·`aud`·`sub`·`exp`·`kid` Token을 거절하는 검증은 consumer인 Learning Core TMI-125의 필수 테스트다. Identity는 정확한 UserMerged Token 발급, 고정 목적 외 audience 거절, active/previous JWKS 노출과 golden-token 호환 증빙을 담당한다. staging/prod issuer·key·kid 분리는 배포 설정/IaC 검증 항목이다.
- 실행한 테스트와 결과: owner event capture/publisher, workload JWT provider, JWKS rotation 집중 테스트 10개를 실행해 실패·오류·건너뜀 없이 통과했다. 전체 Gradle 테스트는 애플리케이션을 변경하지 않은 계약 검토이므로 다시 실행하지 않았다. `git diff --check`를 실행한다.
- 유지한 계약: Billing owner event는 SigV4, Learning Core UserMerged는 Bearer workload JWT를 유지한다. TrialOwnerRebindApproved는 Learning Core로 보내지 않고, wire v1·동일 event 재시도·feature flag 기본 false·Secret 및 사용자 개인정보 비기록을 유지한다.
- 위험 요소: 기존 legacy UserMerged outbox를 계속 drain할 경우 구 `JdkUserMergedDeliveryAdapter`도 새 exact endpoint를 사용하도록 배포 설정과 validation을 맞춰야 한다. 이를 놓치면 신규 fan-out은 정상이어도 legacy backlog가 구 route 또는 느슨한 URI로 전달될 수 있다.
- 다음 작업: 새 TMI-123 후속 브랜치에서 endpoint/path 문서, typed workload purpose, 415 분류와 증빙 테스트를 보완하고 전체 `./gradlew clean test` 및 `git diff --check`를 통과한 뒤 PR을 병합한다. 이후에만 완료 댓글과 Jira `완료` 전환을 다시 승인받는다.

## 2026-09-03 — TMI-123 UserMerged 후속 보완 방향 결정

<!-- codex-turn:01a065e6-7fed-7461-a88b-e99f1826a6e1 -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`.
- Jira: Identity `TMI-123`, 후속 Learning Core `TMI-125`. Jira mutation은 수행하지 않았다.
- 작업 목표: endpoint와 HTTP 415 보완 필요성을 확정하고, UserMerged 전용 workload audience를 어떤 발급 구조로 추가할지 선택지를 정리한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·계약·테스트 코드는 변경하지 않았다.
- 결정사항: 1번 Learning Core exact endpoint를 `/internal/v1/events/user-merged`로 변경하고, 3번 HTTP `415`를 payload/contract 오류 dead-letter로 분류하는 방향을 채택한다.
- 2번 권고: 외부 HTTP Token 발급 route나 독립적인 두 번째 발급 시스템을 추가하지 않는다. 기존 내부 `JwtEncoder`와 signing key·workload issuer·TTL을 재사용하되, raw audience 문자열 대신 `USER_WITHDRAWN`과 `USER_MERGED` 같은 typed purpose를 입력받아 각각 고정 audience로 매핑하는 단일 provider 구조를 권장한다.
- 선택지 비교: raw 문자열 allowlist는 변경량이 작지만 호출부 오타와 목적 혼동을 컴파일 시점에 막지 못한다. 목적별 provider Bean 두 개는 격리가 강하지만 설정·qualifier·테스트가 중복된다. 단일 typed-purpose provider는 발급 코드를 공유하면서 임의 audience를 구조적으로 차단해 현재 두 workload 목적에 가장 적합하다.
- 유지한 계약: 공개 credential endpoint는 만들지 않고 전달 직전에 내부에서 새 JWT를 발급한다. UserWithdrawn과 UserMerged는 서로 다른 audience를 사용하되 RS256, workload issuer, `sub=identity-service`, `iat=nbf`, TTL `PT2M`, UUID `jti`, `typ=JWT`, active `kid`와 JWKS를 공유한다.
- 실행한 테스트와 결과: 설계 판단과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았다. 애플리케이션 변경은 없으며 `git diff --check`를 실행한다.
- 위험 요소: 목적 enum을 추가하면서 기존 UserWithdrawn, legacy UserMerged와 신규 owner-event adapter 중 하나라도 raw audience 경로에 남으면 설정과 실제 발급이 다시 어긋날 수 있다. 모든 workload 호출부와 설정·테스트를 함께 전환해야 한다.
- 다음 작업: 사용자가 단일 typed-purpose provider 방향을 승인하면 1·2·3을 한 후속 구현으로 적용하고 transport/JWT/415/회귀 테스트와 전체 테스트를 실행한다.

## 2026-09-03 — TMI-123 Learning Core 연동 후속 Jira 업데이트 초안

<!-- codex-turn:jira-tmi-123-followup-update-proposal-20260903 -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`.
- Jira: `TMI-123`, 관련 `TMI-125`.
- 작업 목표: 확정한 UserMerged endpoint·typed workload purpose·HTTP 415 보완 범위와 테스트 완료 조건을 TMI-123 본문에 추가하기 전에 현재 이슈와 정확한 변경 초안을 확인한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·계약·테스트 코드는 변경하지 않았다.
- Jira 확인: TMI-123 제목은 `[Identity] Billing SigV4 및 owner event durable fan-out 구현`, 상태는 `해야 할 일`, Resolution은 미설정이며 기존 본문은 최초 Stage 7 범위를 유지하고 있다.
- 제안 변경: 기존 설명을 보존하고 끝에 `TMI-125 연동 전 후속 보완` 섹션을 추가한다. exact Learning Core route `/internal/v1/events/user-merged`, 외부 발급 API 없는 단일 typed-purpose provider와 두 고정 audience, UserMerged HTTP 415 dead-letter, legacy backlog의 동일 endpoint/audience, 기본 false flag와 신규 증빙 테스트를 기록한다.
- 제안 제외: Jira 상태·Resolution·우선순위·담당자·라벨은 변경하지 않고, 댓글과 별도 이슈 링크도 추가하지 않는다.
- 승인 여부: Jira mutation 전 사용자에게 추가할 본문을 제시하고 승인을 기다린다. 현재 Jira 수정은 수행하지 않았다.
- 실행한 테스트와 결과: Jira 읽기 전용 확인과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았으며 `git diff --check`를 실행한다.
- 유지한 계약: Secret·Token·개인정보를 Jira에 기록하지 않고, Billing SigV4·Learning Core Bearer workload JWT·TrialOwnerRebindApproved Billing-only·feature flag 기본 false를 유지한다.
- 위험 요소: 본문을 부분 patch하는 API가 아니므로 승인 후에는 최신 설명 전체를 보존한 상태로 후속 섹션을 append하고 즉시 재조회해 기존 내용 유실 여부를 확인해야 한다.
- 다음 작업: 사용자가 제안 본문을 승인하면 TMI-123 설명만 갱신하고 재조회한 뒤 Jira 작업 내역을 WORKLOG/CURRENT_STATE에 기록한다.

## 2026-09-03 — TMI-123 후속 Jira 업데이트 초안 turn 종료 동기화

<!-- codex-turn:01a065e7-9da1-74a1-bd35-37aad05224aa -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`.
- Jira: `TMI-123`, 관련 `TMI-125`.
- 작업 목표: 종료 훅이 지정한 turn 식별자로 TMI-123 후속 Jira 설명 업데이트 초안과 승인 대기 상태를 기록한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·계약·테스트 코드는 변경하지 않았다.
- 제시한 변경: 기존 Jira 설명을 보존하고 exact Learning Core endpoint `/internal/v1/events/user-merged`, 단일 typed-purpose workload provider와 두 고정 audience, HTTP 415 dead-letter, legacy UserMerged drain, 추가 증빙 테스트와 TMI-125 책임 경계를 후속 섹션으로 append하는 안을 사용자에게 제시했다.
- Jira 작업: 사용자 승인 전이므로 설명·댓글·상태·Resolution·우선순위·담당자·라벨을 변경하지 않았다.
- 실행한 테스트와 결과: Jira 읽기 전용 확인과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았고 `git diff --check`를 통과했다.
- 유지한 계약: Jira mutation 전 정확한 변경 내용 제시와 승인, Secret·Token·개인정보 비기록, Billing SigV4·Learning Core Bearer JWT·TrialOwnerRebindApproved Billing-only·feature flag 기본 false를 유지한다.
- 위험 요소: 승인 후 최신 Jira 설명 전체를 보존한 채 append해야 하며, 수정 직전 최신 updated 시각과 본문을 다시 조회해 동시 변경을 덮어쓰지 않아야 한다.
- 다음 작업: 사용자가 초안을 승인하면 최신 TMI-123을 재조회한 뒤 설명만 갱신하고 저장 결과를 재검증한다.

## 2026-09-03 — TMI-123 Learning Core 연동 후속 Jira 설명 업데이트

<!-- codex-turn:jira-tmi-123-followup-updated-20260903 -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`.
- Jira: `TMI-123`, 관련 `TMI-125`.
- 작업 목표: 사용자에게 사전 제시하고 승인받은 Learning Core UserMerged 후속 보완 범위를 TMI-123 설명에 추가한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·계약·테스트 코드는 변경하지 않았다.
- Jira 작업: 최신 TMI-123 설명을 재조회한 뒤 기존 본문을 그대로 보존하고 `TMI-125 연동 전 후속 보완` 섹션을 끝에 추가했다.
- 추가 내용: exact endpoint `/internal/v1/events/user-merged`, 외부 발급 API 없는 단일 typed-purpose provider, `USER_WITHDRAWN`·`USER_MERGED`의 고정 audience, HTTP 415 dead-letter, legacy UserMerged drain, transport/JWT/독립 retry/feature flag 완료 조건과 Learning Core Mongo 책임 경계를 기록했다.
- 재조회 결과: 후속 섹션, 신규 endpoint, typed purpose와 HTTP 415 내용이 모두 저장됐다. 상태는 `해야 할 일`, Resolution은 미설정이며 우선순위·담당자·라벨·댓글은 변경하지 않았다.
- 승인 여부: 사용자가 제시된 전체 추가 본문을 확인한 뒤 “어 해줘”로 설명 변경을 명시적으로 승인했다.
- 실행한 테스트와 결과: Jira 설명 변경과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았으며 `git diff --check`를 실행한다.
- 유지한 계약: Secret·Token·개인정보 비기록, Billing SigV4, Learning Core Bearer workload JWT, TrialOwnerRebindApproved Billing-only, feature flag 기본 false와 Jira 상태 유지 계약을 지켰다.
- 위험 요소: Jira 본문 갱신만 완료됐고 endpoint·typed purpose·HTTP 415 및 추가 증빙 테스트는 아직 애플리케이션에 구현되지 않았다.
- 다음 작업: TMI-123 후속 브랜치에서 승인된 범위를 구현하고 전체 테스트·PR 병합을 확인한 뒤에만 Jira 완료 댓글과 상태 전환을 진행한다.

## 2026-09-03 — TMI-123 Jira 설명 업데이트 turn 종료 동기화

<!-- codex-turn:01a065e8-e604-7851-a660-7c74ad6e67bc -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`.
- Jira: `TMI-123`, 관련 `TMI-125`.
- 작업 목표: 종료 훅이 지정한 turn 식별자로 승인된 TMI-123 후속 설명 업데이트 결과를 최종 기록한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·계약·테스트 코드는 변경하지 않았다.
- Jira 작업: 기존 설명을 보존한 채 TMI-125 연동 전 후속 보완 섹션을 추가했고, exact endpoint, typed-purpose 고정 audience, HTTP 415 dead-letter, legacy drain과 추가 완료 조건을 기록했다.
- 재조회 결과: 추가 본문이 정상 저장됐고 상태 `해야 할 일`, Resolution 미설정이 유지됐다. 댓글·우선순위·담당자·라벨은 변경하지 않았다.
- 승인 여부: 사용자의 “어 해줘” 승인 후 설명 변경을 수행했다.
- 실행한 테스트와 결과: Jira·문서 작업이므로 Gradle 테스트는 실행하지 않았고 `git diff --check`를 통과했다.
- 유지한 계약: Secret·Token·개인정보 비기록, Jira 사전 승인, Billing SigV4·Learning Core Bearer JWT·TrialOwnerRebindApproved Billing-only·feature flag 기본 false를 유지한다.
- 위험 요소: Jira 설명만 업데이트됐으며 후속 애플리케이션 구현과 전체 테스트·PR 병합은 남아 있다.
- 다음 작업: TMI-123 후속 구현을 완료하고 병합한 뒤 Jira 완료 여부를 다시 검토한다.

## 2026-09-03 — TMI-123 Learning Core UserMerged 후속 계약 구현

<!-- codex-turn:tmi-123-learning-core-user-merged-followup-implementation-20260903 -->

- 날짜: 2026-09-03
- 브랜치: 로컬 `feat/TMI-123-owner-event-fanout-sigv4`. 원격 브랜치는 PR #37 병합 후 삭제된 상태다. 최신 `develop`에서 `codex/TMI-123-learning-core-user-merged-contract`를 만들려 했으나 작업 환경의 `.git/index.lock` 쓰기 제한으로 Git ref는 변경하지 않았으며, 현재 HEAD의 TMI-123 구현 commit 위에 후속 변경을 적용했다.
- Jira: `TMI-123`, 후속 Learning Core `TMI-125`. 구현 전에 공식 Atlassian 연동으로 업데이트된 TMI-123 설명과 상태 `해야 할 일`을 읽었다. 이번 구현에서는 Jira 댓글·상태·필드를 변경하지 않았다.
- 작업 목표: Learning Core UserMerged endpoint를 `/internal/v1/events/user-merged`로 통일하고, raw audience 문자열 없는 typed workload purpose를 도입하며, HTTP 415를 payload 계약 오류로 격리하고 요청된 증빙 테스트를 추가한다.
- 변경 파일: `.env.example`, `README.md`, `src/main/resources/application.yml`, `src/test/resources/application-test.yml`, workload credential provider·JWT properties/provider, 신규 `global/workload/WorkloadIdentityPurpose.java`, Learning Core owner-event adapter/config/properties, legacy UserMerged adapter/config/properties/publisher/failure code, UserWithdrawn adapter/config/properties, owner-event publisher/failure code, 관련 테스트, Stage 5·Stage 7·Learning Core handoff·owner-event runbook 계약 문서, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`.
- endpoint 구현: 신규 `LearningCoreOwnerEventDeliveryAdapter`와 legacy `JdkUserMergedDeliveryAdapter`가 HTTPS exact path `/internal/v1/events/user-merged`만 허용한다. host가 필요하고 user-info·query·fragment를 거절하며 redirect를 따르지 않고 JSON POST와 Bearer workload credential을 사용한다. owner-event properties도 같은 exact path로 fail-fast한다. Billing UserMerged route `/internal/v1/owners/merge/events`는 변경하지 않았다.
- typed workload 구현: `WorkloadIdentityPurpose`는 `USER_WITHDRAWN`을 `learning-core-user-withdrawn`, `USER_MERGED`를 `learning-core-user-merged`에 고정 매핑한다. provider API는 raw String 대신 이 enum만 받아 기존 단일 `JwtEncoder`, active signing key, workload issuer·subject·TTL로 요청마다 새 JWT를 발급한다. UserWithdrawn, legacy UserMerged와 신규 Learning Core owner-event 호출부를 모두 typed purpose로 전환했다.
- 설정 변경: 더 이상 사용되지 않는 `WORKLOAD_JWT_AUDIENCE`, `USER_MERGED_PUBLISHER_AUDIENCE`, `USER_WITHDRAWN_PUBLISHER_AUDIENCE`, `OWNER_EVENT_LEARNING_CORE_AUDIENCE` 설정을 애플리케이션·테스트·예시·README에서 제거했다. audience는 배포 입력이 아니라 코드의 승인 목적에 고정된다.
- JWT 유지 계약: RS256, 환경별 HTTPS workload issuer, `sub=identity-service`, `iat`, `nbf=iat`, `exp=iat+PT2M`, 요청별 canonical UUID `jti`, `typ=JWT`, active non-blank `kid`, 기존 active/previous JWKS를 유지한다. userId·email·phone·Firebase UID·provider subject·credential claim을 넣지 않는다.
- 오류 분류: 신규 owner-event publisher에 `HTTP_415`를 추가하고 400·409·413·415·422를 DEAD_LETTER 처리한다. legacy UserMerged publisher도 415를 명시적인 `HTTP_415` 영구 실패로 기록한다. 기존 2xx 완료, 408·425·429·5xx/timeout/connection retry, 401·403와 route 오류의 신규 consumer circuit pause, bounded Retry-After는 유지한다.
- 테스트 추가·보강: exact endpoint·구 path/query/fragment/user-info/http 거절, redirect NEVER, JSON POST·Bearer header, bounded Retry-After, 신규·legacy UserMerged의 `USER_MERGED` purpose와 UserWithdrawn의 `USER_WITHDRAWN` purpose, 두 audience claim, 요청별 새 jti, 개인정보 claim 부재, null/비승인 purpose 경계, 415 dead-letter, 동일 core 재직렬화 payload 안정성, Billing 성공·Learning Core 실패의 독립 retry를 검증했다.
- 실행한 테스트와 결과: `./gradlew compileJava compileTestJava` 성공, 관련 집중 테스트 24개 성공, 최종 `./gradlew clean test` 성공. 총 630개 테스트, 실패 0, 오류 0, 건너뜀 0. 첫 clean test는 sandbox의 사용자 Gradle cache lock 접근 제한으로 시작 전에 실패해 승인된 외부 실행으로 동일 명령을 다시 수행했고 성공했다. `git diff --check`를 실행한다.
- 유지한 외부 계약: Billing owner event는 VPC Lattice AWS_IAM·SigV4와 기존 Billing route를 유지한다. Learning Core UserMerged만 Bearer workload JWT의 전용 audience를 사용한다. `TrialOwnerRebindApproved`는 Billing-only, UserMerged wire v1과 eventId/payload retry, legacy/new capture 상호 배타, 공개 API·BaseResponse·사용자 JWT·Refresh Session·Python AI `user_id=examId` 계약은 변경하지 않았다. 모든 관련 feature flag 기본값은 false다.
- 결정사항: 별도 외부 Token 발급 endpoint나 목적별 중복 provider Bean을 만들지 않고 단일 typed-purpose provider를 사용한다. audience 환경변수를 제거해 호출부와 배포 설정에서 임의 문자열을 주입할 수 없게 했다.
- 위험 요소: 실제 Learning Core TMI-125 consumer, workload verifier의 잘못된 iss·aud·sub·exp·kid 거절, staging/prod issuer·key·kid 분리, golden-token E2E는 Learning Core·배포 환경에서 아직 검증되지 않았다. legacy outbox가 있으면 legacy publisher에도 새 endpoint가 배포 설정으로 주입돼야 한다.
- 배포 전 확인: Learning Core consumer OFF 배포 후 workload JWT 인증과 신규 route를 확인하고, Learning Core Mongo migration·Transaction 검증 뒤 consumer ON → Identity capture ON → Learning Core publisher ON 순서를 지킨다. 제거한 audience 환경변수에 의존하는 배포 manifest를 정리하고 feature flag는 검증 전 false로 유지한다.
- 예상 밖 변경: 작업 시작 전 존재하던 WORKLOG/CURRENT_STATE의 이전 turn 기록은 보존했다. Billing·Learning Core 저장소, Jira, commit·push는 변경하지 않았다. Git branch 생성 실패 외에 예상 밖 애플리케이션 파일 변경은 없다.
- 다음 작업: 사용자가 새 브랜치 또는 현재 로컬 브랜치를 정리해 commit·push하고 PR을 병합한다. 병합 후 TMI-123 완료 댓글과 상태 전환을 별도 승인받고, Learning Core TMI-125에서 consumer·verifier·Mongo staging 증빙을 완료한다.

## 2026-09-03 — TMI-123 UserMerged 후속 구현 turn 종료 동기화

<!-- codex-turn:01a065eb-9cfd-7f92-9e1a-041ae302d121 -->

- 날짜: 2026-09-03
- 브랜치: 로컬 `feat/TMI-123-owner-event-fanout-sigv4`; 연결된 원격 브랜치는 PR #37 병합 뒤 삭제된 상태다.
- Jira: `TMI-123`, 관련 `TMI-125`. Jira mutation은 수행하지 않았다.
- 작업 목표: 종료 훅이 지정한 turn 식별자로 Learning Core UserMerged 후속 구현과 최종 검증 결과를 기록한다.
- 변경 파일: typed workload purpose·JWT provider와 세 workload 호출 adapter, UserMerged endpoint/config, owner-event 및 legacy failure 분류, 환경 설정·README, 관련 단위·계약 테스트, Stage 5·7 계약과 runbook, `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`.
- 구현 결과: Learning Core 신규·legacy UserMerged 전송을 exact `/internal/v1/events/user-merged`로 고정했다. `USER_WITHDRAWN`과 `USER_MERGED` 목적을 두 고정 audience에 매핑해 raw audience 입력과 관련 환경변수를 제거했다. HTTP 415는 신규·legacy UserMerged에서 명시적인 계약 오류 dead-letter로 처리한다.
- 보안·전송: HTTPS host와 exact path를 요구하고 user-info·query·fragment를 거절하며 redirect를 따르지 않는다. UserMerged는 Bearer workload JWT, Billing은 기존 SigV4를 유지한다. 요청별 새 jti와 민감 claim 부재, 단일·bounded Retry-After만 수용하는 경계를 테스트했다.
- 증빙 테스트: UserMerged 두 consumer와 TrialOwnerRebindApproved Billing-only 기존 테스트에 더해 exact endpoint, typed purpose, 두 audience, legacy/withdrawal 회귀, 동일 payload 재직렬화, Billing 성공·Learning Core 실패 독립 retry, 415 dead-letter를 검증했다.
- 실행한 테스트와 결과: 최종 `./gradlew clean test` 성공, 총 630개 테스트, 실패 0, 오류 0, 건너뜀 0. 최종 `git diff --check` 성공. Gradle cache lock에 대한 최초 sandbox 실패는 테스트 실행 전 발생했고 승인된 재실행 두 번은 모두 성공했다.
- 유지한 계약: 공개 API, BaseResponse, 사용자 JWT, Refresh Session, Firebase, phone eligibility Billing SigV4, UserMerged v1, TrialOwnerRebindApproved Billing-only, Python AI `user_id=examId`, feature flag 기본 false를 유지한다.
- 결정사항: 별도 Token 발급 API나 중복 provider Bean 없이 단일 typed-purpose provider를 사용한다. Learning Core용 구 path 참조는 제거했고 남은 `/internal/v1/owners/merge/events`는 Billing route와 Learning Core의 구 path 거절 테스트뿐이다.
- 위험 요소: 실제 Learning Core TMI-125 consumer와 workload verifier, staging/prod key 분리, golden-token·staging E2E는 미검증이다. 현재 로컬 브랜치의 원격이 삭제돼 사용자가 후속 PR용 브랜치를 정리해야 한다.
- 다음 작업: 사용자 환경에서 후속 브랜치를 만들거나 현재 브랜치를 새 원격으로 push해 PR을 병합한 뒤, TMI-123 완료 댓글·상태 전환을 별도 승인 후 수행한다.

## 2026-09-03 — TMI-123 UserMerged 후속 구현 독립 검토

- 날짜: 2026-09-03
- 브랜치: 로컬 `feat/TMI-123-owner-event-fanout-sigv4`; `develop`은 PR #37 merge commit `391b55f`, 현재 후속 변경은 미커밋·미추적 상태
- Jira: `TMI-123`, 관련 Learning Core `TMI-125`; Jira를 조회만 했고 수정·댓글·상태 변경은 수행하지 않았다.
- 작업 목표: Learning Core에서 요청한 endpoint, typed workload audience, fan-out 독립성, 오류 분류와 회귀 테스트가 Identity 후속 구현에 반영됐는지 검토한다.
- 확인 결과: 신규·legacy UserMerged adapter가 exact `/internal/v1/events/user-merged`와 `USER_MERGED` purpose를 사용하고, UserWithdrawn은 `USER_WITHDRAWN`을 유지한다. provider는 두 enum purpose를 고정 audience로 매핑해 임의 audience 입력을 제거했고 RS256·subject·TTL·요청별 jti 계약을 유지한다.
- fan-out·오류: 기존 core는 UserMerged BILLING·LEARNING_CORE 두 delivery와 TrialOwnerRebindApproved BILLING-only를 유지한다. 새 publisher는 Billing 성공과 Learning Core 실패를 독립 처리하고 415를 contract dead-letter, 401/403·route 오류를 circuit pause, retryable status를 기존 retry로 분류한다.
- 테스트: `./gradlew clean test` 전체 630개 성공, 실패 0·오류 0·건너뜀 0. `git diff --check`도 성공했다.
- 발견사항: `OwnerEventCoreTests.retrySerializationKeepsTheSameEventIdAndPayload`는 같은 event 객체를 즉시 두 번 serialize해 equality만 비교한다. 실제 publisher가 첫 전송 실패 후 delivery를 재claim했을 때 동일 eventId와 byte payload를 다시 전송하는 완료 조건은 직접 검증하지 않으므로 후속 PR 전에 capture port 기반 retry test로 보강하는 것을 권장한다.
- 상태 판단: 요청한 구현은 로컬에서 기능상 반영됐고 현재 검토에서 차단급 코드 결함은 발견하지 못했다. 다만 후속 변경은 아직 commit·push·PR merge 전이며 TMI-123 Jira도 `해야 할 일`이다.
- 유지 계약: 사용자 Access JWT, UserMerged v1 wire, Billing SigV4, TrialOwnerRebindApproved Billing-only, feature flag 기본 false와 민감정보 비로그를 유지한다.
- 변경 범위: 이번 검토에서는 Identity 애플리케이션·설정·테스트 코드를 수정하지 않고 `docs/codex/CURRENT_STATE.md`, `docs/codex/WORKLOG.md`만 갱신했다. Secret·Token을 기록하지 않았다.
- 다음 작업: 실제 publisher retry payload test를 보강한 뒤 사용자 주도로 후속 commit·push·PR merge를 수행하고, staging workload golden-token과 Learning Core TMI-125 E2E 후 Jira 완료를 검토한다.

## 2026-09-03 — TMI-123 병합 확인과 Jira 완료 승인 대기

<!-- codex-turn:01a06663-983d-70d2-b6a0-31404742a738 -->

- 날짜: 2026-09-03
- 브랜치: Identity `develop`; `HEAD`, `develop`, `origin/develop`이 merge commit `fa9843e`로 일치한다.
- Jira: `TMI-123`, 관련 Learning Core `TMI-125`.
- 작업 목표: TMI-123 구현 병합과 검증 증빙을 확인하고 Jira 완료 댓글 및 상태 전환 준비 상태를 점검한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·계약·테스트 코드는 변경하지 않았다.
- 확인 결과: PR #38이 `develop`에 병합됐고 후속 구현 commit `1110b8a`가 포함됐다. worktree의 애플리케이션·문서는 `develop`과 일치한다.
- Jira 조회: 공식 Atlassian 연동으로 TMI-123이 `해야 할 일`, Resolution 미설정이며 `완료` 전환 ID `41`이 사용 가능한 것을 확인했다.
- 제시할 Jira 작업: 구현·테스트·병합 결과와 배포 전 미검증 사항을 요약한 완료 댓글을 등록한 뒤 transition ID `41`로 `완료` 상태로 변경한다.
- 승인 여부: Jira mutation 전 사전 승인 규칙에 따라 사용자 승인을 기다리며 댓글·상태·Resolution·기타 필드는 변경하지 않았다.
- 실행한 테스트와 결과: 기존 최종 `./gradlew clean test`에서 총 630개 테스트가 실패·오류·건너뜀 없이 통과했고 `git diff --check`도 성공한 기록을 확인했다. 이번 조회 turn에서는 Gradle 테스트를 다시 실행하지 않았다.
- 유지한 계약: Billing SigV4, Learning Core Bearer workload JWT, TrialOwnerRebindApproved Billing-only, UserMerged wire v1, 관련 feature flag 기본 false와 Secret·Token·개인정보 비기록을 유지한다.
- 위험 요소: 실제 workload verifier, staging/prod key 분리와 cross-service staging E2E는 TMI-125 및 배포 환경에서 확인해야 한다.
- 다음 작업: 사용자가 제시된 완료 댓글과 상태 전환을 승인하면 댓글을 등록하고 완료 전환을 적용한 뒤 status와 Resolution을 재조회한다.

## 2026-09-04 — TMI-123 Jira 완료 처리

<!-- codex-turn:01a069f5-75ca-71e0-ad49-1283aceadec3 -->

- 날짜: 2026-09-04
- 브랜치: Identity `develop`; 확인된 병합 commit은 `fa9843e`다.
- Jira: `TMI-123`, 관련 Learning Core `TMI-125`.
- 작업 목표: 병합과 테스트 증빙이 확인된 TMI-123에 완료 댓글을 등록하고 Jira를 완료 상태로 전환한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·계약·테스트 코드는 변경하지 않았다.
- Jira 작업: PR #38 병합, Billing SigV4·owner event durable fan-out·phone rejoin lineage·Learning Core exact endpoint·목적별 workload audience·HTTP 415 격리, 전체 630개 테스트 성공과 남은 배포 검증을 요약한 완료 댓글 ID `10045`를 등록했다.
- 상태 변경: 사용자 승인 후 transition ID `41`을 적용해 `해야 할 일`에서 `완료`로 변경했다.
- 재조회 결과: status ID `10003`의 `완료`, Resolution ID `10000`의 `완료`, 댓글 ID `10045` 저장을 확인했다. 우선순위·담당자·라벨·설명 등 다른 필드는 변경하지 않았다.
- 승인 여부: 사전에 전체 완료 댓글과 상태 전환을 제시했고 사용자가 “완료해줘”로 승인했다.
- 실행한 테스트와 결과: 이번 Jira·문서 작업에서는 Gradle 테스트를 다시 실행하지 않았다. 병합 전 최종 `./gradlew clean test`는 총 630개 테스트가 실패·오류·건너뜀 없이 통과했고, 이번 문서 갱신 후 `git diff --check`를 실행한다.
- 유지한 계약: Billing SigV4, Learning Core Bearer workload JWT, TrialOwnerRebindApproved Billing-only, UserMerged wire v1, 관련 feature flag 기본 false와 Secret·Token·개인정보 비기록을 유지했다.
- 위험 요소: 실제 workload verifier, staging/prod key 분리와 cross-service staging E2E는 TMI-125 및 배포 환경에서 여전히 확인해야 한다.
- 다음 작업: Learning Core `TMI-125`의 consumer·workload verifier·Mongo Transaction·staging E2E를 완료한 뒤 승인된 순서로 feature flag를 활성화한다.

## 2026-09-07 — SNS/Firebase 로그인 구현 현황 점검

<!-- codex-turn:01a07a16-7094-77e2-8875-d74479625d4e -->

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`.
- Jira: 신규 조회·생성·수정·댓글·상태 변경 없음. 과거 완료 이슈 TMI-88~98, TMI-103·104·107·108·111·114·123의 병합 코드를 근거로 확인했다.
- 작업 목표: 현재 SNS 로그인 관련 구현 범위와 운영 활성화 전 남은 범위를 실제 코드·설정·계약 기준으로 정리한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·설정·계약·테스트 코드는 변경하지 않았다.
- 구현 확인: Firebase credential broker와 Google·Apple·Kakao provider mapping, Firebase ID Token의 revoke·disabled·issuer·audience·tenant·시간·recent-auth 검증, Firebase UID→canonical UUID User mapping, SocialIdentity provider+subject 소유권 검증이 구현돼 있다.
- 사용자 흐름 확인: 공개 exchange·signup, JWT 인증이 필요한 Guest prepare·upgrade·merge와 MEMBER auth-method sync API가 존재한다. 기존 FirebaseIdentity owner는 Identity Access/Refresh Token을 받고, 미등록 UID는 User를 즉시 만들지 않고 enrollment를 받은 뒤 verified phone·profile·consent와 함께 Transaction으로 가입을 완료한다.
- 계정·lifecycle 확인: 여러 SNS는 하나의 Firebase UID와 내부 User에 여러 SocialIdentity로 동기화된다. 다른 User가 provider subject를 소유하면 자동 병합하지 않고 conflict 또는 명시적 Guest merge 경계로 처리한다. 탈퇴 재인증, Firebase disable·revoke·delete worker, Firebase/Social 제거와 Phone release, CLEANED 재가입 gate, 가입 중단 Firebase User cleanup 및 downstream owner event 기반이 구현돼 있다.
- 현재 활성화 상태: `FIREBASE_AUTH_ENABLED`, Google·Apple·Kakao·phone provider flag, Firebase withdrawal/cleanup/identity-release, abandoned cleanup, Guest merge 및 관련 publisher flag는 기본값이 false다. disabled 구성에서는 Firebase API가 성공처럼 통과하지 않고 503으로 fail-closed한다.
- 남은 구현: logout-all의 Firebase refresh revoke, Refresh Token 응답 유실·rotation 원자성, Provider unlink·전화번호 변경, Guest 생성 응답 유실 복구, 기존 ACTIVE 회원의 Firebase rebind 정책이 남아 있다. 현재 logout-all은 Identity RefreshSession만 폐기한다.
- 운영 미검증: 실제 Firebase project와 모바일 SDK redirect/deep-link, Kakao Identity Platform·billing·stable provider UID, Apple revoke lifecycle, Mongo replica set Transaction, workload 인증과 Billing/Learning Core cross-service staging E2E가 완료되지 않았다. 따라서 운영 SNS 로그인 활성화 완료로 보지 않는다.
- 실행한 테스트와 결과: 상태 확인 작업이라 Gradle 테스트를 다시 실행하지 않았다. 현재 develop 병합 기준 마지막 전체 검증은 `./gradlew clean test` 630개 성공, 실패·오류·건너뜀 0과 `git diff --check` 성공이다.
- 유지한 계약: Firebase는 credential broker만 담당하고 Identity가 canonical User·자체 Access/Refresh Token을 소유한다. Firebase UID·provider subject·phone을 내부 userId나 자동 merge 키로 사용하지 않으며 Token·개인정보를 로그에 남기지 않는다.
- 위험 요소: `docs/contracts/firebase-auth-follow-up-implementation-order.md` 체크박스는 staging·배포까지 포함한 전체 완료 기준이라 코드 구현이 병합된 1~7단계도 미체크 상태다. 이를 코드 미구현으로 해석하면 안 되지만, 현 문서는 구현 완료와 운영 완료를 한눈에 구분하지 못한다.
- 다음 작업: 운영 활성화가 목표라면 실제 Firebase/mobile·Mongo·서비스 간 staging E2E와 feature flag 순차 활성화를 먼저 닫고, 기능 개발은 고정 순서의 8번 logout-all Firebase revoke부터 진행한다.

## 2026-09-07 — SNS 로그인과 Firebase 책임 경계 설명

<!-- codex-turn:01a07a16-7094-77e2-8875-d74479625d4e -->

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`.
- 작업 목표: Google·Apple·Kakao 로그인 중 Firebase와 Identity가 각각 담당하는 부분을 명확히 설명한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 확인 내용: 앱은 Firebase Auth SDK를 통해 Provider 인증을 수행하고 Firebase ID Token을 받는다. Identity는 Google·Apple·Kakao와 직접 OAuth Token 교환을 하지 않고 Firebase Admin으로 Token과 providerData를 검증한 뒤 자체 Access/Refresh Token을 발급한다.
- Provider 경계: Google은 `google.com`, Apple은 `apple.com`, Kakao는 승인된 Generic OIDC provider `oidc.kakao`로 mapping한다. Kakao production 검증과 모든 관련 feature flag 활성화는 아직 남아 있다.
- 실행한 테스트와 결과: 설명 및 문서 기록만 수행해 Gradle 테스트를 실행하지 않았고 `git diff --check`를 실행한다.
- 유지한 계약: Firebase는 credential broker이고 Identity가 canonical User와 자체 Session·Token을 소유한다. 실제 Token·credential·개인정보는 기록하지 않았다.
- 위험 요소: Firebase 로그인 구현과 운영 활성화는 다르며, 기본 flag가 false인 현재 배포 설정에서는 Firebase API가 fail-closed한다.
- 다음 작업: 실제 모바일 Firebase SDK와 Provider별 staging 인증을 검증한 뒤 Google·Apple·Kakao를 각각 독립적으로 활성화한다.

## 2026-09-07 — SNS 로그인 Firebase 책임 경계 turn 종료 동기화

<!-- codex-turn:01a07a1a-58d6-7b53-9be2-efed74746f3f -->

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`.
- 작업 목표: 종료 훅이 지정한 turn 식별자로 SNS 로그인 책임 경계 설명을 최종 기록한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·설정·테스트 코드는 변경하지 않았다.
- 구현 내용: 코드 구현은 없다. 앱과 Firebase Auth가 Google·Apple·Kakao 인증 및 Firebase ID Token 발급을 담당하고, Identity는 Firebase Admin으로 해당 Token과 providerData를 검증한 뒤 자체 Access/Refresh Token을 발급한다는 구조를 확인했다.
- 실행한 테스트와 결과: 문서 기록만 수행해 Gradle 테스트를 실행하지 않았고 `git diff --check`를 실행한다.
- 유지한 계약: Identity는 Provider와 직접 OAuth Token 교환하지 않으며 canonical User·RefreshSession·자체 JWT를 소유한다. 관련 feature flag 기본 false와 민감정보 비기록을 유지했다.
- 결정사항: Google은 `google.com`, Apple은 `apple.com`, Kakao는 Generic OIDC `oidc.kakao` mapping을 사용한다.
- 위험 요소: Kakao production 가능성과 모바일 redirect를 포함한 실제 Provider별 staging 검증은 완료되지 않았다.
- 다음 작업: 실제 모바일 Firebase SDK와 Provider별 staging 인증을 검증한 뒤 feature flag를 독립적으로 활성화한다.

## 2026-09-07 — Kakao Generic OIDC 외부 설정 필요성 확인

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`.
- 작업 목표: Firebase가 기본 제공하지 않는 Kakao 로그인을 사용하기 위해 필요한 설정 경계를 확인한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·런타임 설정·테스트 코드는 변경하지 않았다.
- 확인 내용: 현재 Identity 코드는 Kakao Firebase provider ID 기본값 `oidc.kakao`, providerData→KAKAO mapping과 `FIREBASE_KAKAO_ENABLED` kill switch를 제공한다. 이것만으로 Kakao 인증이 생성되지는 않는다.
- 필수 외부 설정: Kakao Developers에서 Kakao Login·OIDC와 callback redirect를 구성하고, Firebase project를 Identity Platform으로 업그레이드해 billing을 승인한 뒤 issuer `https://kauth.kakao.com`의 Generic OIDC provider를 동일 ID로 등록해야 한다. client ID·secret은 외부 콘솔의 안전한 credential 경계에서 관리한다.
- 클라이언트 조건: 모바일 앱은 Firebase SDK에서 `oidc.kakao` provider login/link를 수행하고 Android/iOS redirect·deep-link 복귀와 반복 login/link의 stable provider UID를 staging에서 검증해야 한다.
- Identity 활성화: 외부·모바일 검증 이후에만 `FIREBASE_AUTH_ENABLED`, `FIREBASE_KAKAO_ENABLED`와 정확한 `FIREBASE_KAKAO_PROVIDER_ID`를 환경별로 활성화한다.
- 실행한 테스트와 결과: 코드 변경 없는 설명·문서 작업이므로 Gradle 테스트를 실행하지 않았고 `git diff --check`를 실행한다.
- 유지한 계약: Identity는 Kakao credential이나 client secret을 직접 저장·검증하지 않고 Firebase ID Token과 승인된 providerData만 검증한다. Secret·Token·개인정보를 기록하지 않았다.
- 위험 요소: 공개 OIDC discovery 통과는 Firebase 등록 가능성, billing, 실제 모바일 redirect와 stable provider UID를 증명하지 않으므로 현재 Kakao production 상태는 `NO-GO`다.
- 다음 작업: 격리 staging Firebase project와 Kakao test app에서 Generic OIDC provider를 등록하고 실기기 login·link·logout·withdrawal lifecycle을 검증한 뒤 활성화 여부를 결정한다.

## 2026-09-07 — Kakao Generic OIDC 설정 확인 turn 종료 동기화

<!-- codex-turn:01a07a1b-57f0-7143-a35a-ab1e82ab1dcc -->

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`.
- 작업 목표: 종료 훅이 지정한 turn 식별자로 Kakao 로그인 외부 설정 필요성과 현재 활성화 상태를 최종 기록한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·런타임 설정·테스트 코드는 변경하지 않았다.
- 구현 내용: 코드 구현은 없다. Identity에는 `oidc.kakao` mapping과 feature flag가 있지만 실제 인증에는 Kakao Developers OIDC·redirect, Firebase Identity Platform Generic OIDC·billing, 모바일 login/link·deep-link 설정이 별도로 필요함을 확인했다.
- 실행한 테스트와 결과: 문서 기록만 수행해 Gradle 테스트를 실행하지 않았고 `git diff --check`를 실행한다.
- 유지한 계약: Kakao credential은 Firebase 외부 설정 경계에서 관리하고 Identity는 Firebase ID Token과 승인된 providerData만 검증한다. Secret·Token·개인정보를 기록하지 않았다.
- 결정사항: 실제 프로젝트 등록과 실기기 staging 검증 전 `FIREBASE_KAKAO_ENABLED=false`와 production `NO-GO`를 유지한다.
- 위험 요소: OIDC discovery 통과만으로 Firebase 등록, stable provider UID, Android/iOS 복귀와 account lifecycle은 보장되지 않는다.
- 다음 작업: 격리 staging 환경에서 `oidc.kakao`를 등록하고 login·link·logout·withdrawal을 검증한 뒤 Provider flag 활성화를 결정한다.

## 2026-09-07 — Kakao·Identity Platform Generic OIDC 설정 절차 확인

<!-- codex-turn:01a07a1d-20af-7712-b5ef-3ae0654f1fad -->

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`.
- 작업 목표: Kakao Developers와 Google Cloud Identity Platform에서 Kakao Generic OIDC를 구성하는 실제 메뉴·입력값·검증 순서를 안내한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·런타임 설정·테스트 코드는 변경하지 않았다.
- 공식 문서 확인: Kakao Developers의 카카오 로그인 설정 문서와 Google Cloud Identity Platform OIDC 로그인 문서를 읽기 전용으로 확인했다.
- Kakao 설정: 앱 관리의 카카오 로그인 사용 설정과 OpenID Connect를 ON으로 하고, 앱의 REST API 키 및 활성 client secret을 사용한다. Identity Platform이 표시하는 callback URL을 카카오 로그인 리다이렉트 URI에 exact 등록하며 필요한 최소 동의항목과 개인정보 국외이전 고지를 확인한다.
- Identity Platform 설정: billing이 연결되고 Identity Platform이 활성화된 Firebase project에서 ID 공급업체 추가 → OpenID Connect → 코드 흐름을 선택한다. provider ID `oidc.kakao`, Kakao REST API 키 client ID, issuer `https://kauth.kakao.com`, Kakao client secret과 승인 도메인을 설정한다.
- callback 계약: 기본 callback은 `https://<project-id>.firebaseapp.com/__/auth/handler` 형식이며 project별 실제 콘솔 표시값을 그대로 사용한다. 임의 앱 API endpoint나 Identity `/firebase/exchange`를 Kakao redirect URI로 등록하지 않는다.
- 활성화 순서: 외부 Provider 설정 후 모바일 Firebase SDK의 `oidc.kakao` login/link, redirect 복귀, 반복 로그인·연결의 Firebase UID와 provider UID 안정성을 staging에서 검증하고 나서 Identity의 Firebase·Kakao flag를 켠다.
- 실행한 테스트와 결과: 설명·공식 문서 확인 및 문서 기록만 수행해 Gradle 테스트를 실행하지 않았고 `git diff --check`를 실행한다.
- 유지한 계약: Kakao credential은 Kakao·Identity Platform의 보안 설정에만 저장하고 저장소나 Identity 로그에 기록하지 않는다. Identity는 Firebase ID Token과 승인된 providerData만 검증한다.
- 위험 요소: Identity Platform billing, 실제 모바일 redirect, account link collision, Kakao unlink·withdrawal과 stable provider UID가 검증되지 않으면 production 활성화할 수 없다.
- 다음 작업: 격리 staging project에 실제 provider를 설정한 뒤 성공·취소·중복계정·기존 Firebase User link·탈퇴 시나리오를 확인하고 `FIREBASE_KAKAO_ENABLED` 활성화를 결정한다.

## 2026-09-07 — Firebase 휴대폰 인증 현재 흐름 설명

<!-- codex-turn:01a07a22-8dc9-7113-8873-d9bb6d041526 -->

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`.
- 작업 목표: 현재 휴대폰 인증의 담당 주체, 모바일·Firebase·Identity 간 가입 흐름과 활성화 상태를 설명한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·런타임 설정·계약·테스트 코드는 변경하지 않았다.
- 구현 내용: SMS 발송과 OTP 검증은 Firebase Phone Authentication과 모바일 Firebase SDK가 담당한다. 앱은 SNS로 로그인된 동일 Firebase User에 `PhoneAuthCredential`을 `linkWithCredential`로 연결하고 UID 유지 여부를 확인한 뒤 강제 갱신한 Firebase ID Token을 enrollmentId와 함께 Identity signup 또는 Guest upgrade API에 전달한다. Identity는 OTP 원문을 받지 않고 Admin 검증 결과에서 동일 UID의 primary SNS credential과 verified phone provider를 확인한 후 User·FirebaseIdentity·SocialIdentity·PhoneIdentity·RefreshSession과 enrollment consume을 Transaction으로 저장한다.
- 보안·제품 경계: phone credential 직접 로그인과 phone-only Firebase User의 login exchange는 허용하지 않으며 전화번호는 로그인 ID나 자동 계정 병합 키가 아니라 가입 proof다. 번호 원문은 저장하지 않고 E.164 정규화 후 HMAC fingerprint를 저장하며 동일 번호는 동시에 한 ACTIVE MEMBER만 소유할 수 있다.
- 실행한 테스트와 결과: 코드 변경 없는 설명·문서 기록 작업이므로 Gradle 테스트는 재실행하지 않았다. 문서 변경에는 `git diff --check`를 실행한다.
- 유지한 계약: Firebase는 credential broker이고 Identity가 canonical UUID User와 자체 Access/Refresh Token을 소유한다. OTP, Firebase ID Token, 전화번호 원문과 실제 key는 기록하지 않았다.
- 결정사항: 신규 SNS 가입은 exchange → enrollment → 같은 Firebase UID에 phone link → force-refresh ID Token → signup 순서를 사용하고, Guest는 prepare → phone link → upgrade의 동일 원칙을 사용한다.
- 위험 요소: Firebase Auth·phone·fingerprint·eligibility 관련 feature flag는 기본 false이며, 실제 모바일 SDK·국내 SMS 도달률·quota·비용·App Check/reCAPTCHA와 staging E2E는 완료되지 않았다. 전화번호 변경 기능도 아직 구현되지 않았다.
- 다음 작업: 격리 staging 환경에서 실기기 SMS 인증, 동일 UID link, 중복 번호, 중단·재개·탈퇴 cleanup과 abuse 방어를 검증한 뒤 관련 flag를 순차 활성화한다.

## 2026-09-07 — Firebase 프로젝트 최초 연결 순서 정리

<!-- codex-turn:01a07a23-dca1-7db3-87db-9a44f76c218b -->

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`.
- Jira: 조회·생성·수정·댓글·상태 변경 없음.
- 작업 목표: Firebase가 아직 연결되지 않은 상태에서 Kakao Generic OIDC와 휴대폰 인증을 설정하기 전에 필요한 최초 프로젝트 연결 순서를 정리한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·런타임 설정·계약·테스트 코드는 변경하지 않았다.
- 구현 내용: 기존 Google Cloud 프로젝트를 환경별 Firebase 프로젝트로 등록하고 Firebase Authentication을 시작한 뒤 Android/iOS 앱을 등록한다. 이후 billing 연결과 Identity Platform 활성화를 수행하고 Google·Apple·phone 및 Generic OIDC `oidc.kakao`를 각각 설정하는 순서로 정리했다.
- 서버 연결: Identity는 활성화 시 정확한 `FIREBASE_PROJECT_ID`와 해당 프로젝트에 접근 가능한 Firebase Admin credential이 필요하다. 운영에서는 저장소의 service-account private key 파일보다 배포 workload의 Application Default Credentials와 최소 IAM 권한을 우선한다.
- 실행한 테스트와 결과: 외부 콘솔 설정 안내와 문서 기록만 수행해 Gradle 테스트는 재실행하지 않았다. 문서 변경에는 `git diff --check`를 실행한다.
- 유지한 계약: staging과 production 프로젝트·credential·provider 설정을 분리하고 실제 client secret·private key·Token을 저장소나 기록에 남기지 않는다. 관련 feature flag는 검증 전 기본 false를 유지한다.
- 결정사항: 이미 백엔드 배포에 사용하는 Google Cloud 프로젝트가 있다면 새 프로젝트를 중복 생성하지 않고 해당 프로젝트에 Firebase를 추가한다. 단, staging과 production은 서로 다른 프로젝트로 분리한다.
- 위험 요소: Firebase 프로젝트 추가는 되돌리기 쉬운 단순 코드 설정이 아니며 billing, OAuth redirect, authorized domain, 모바일 package/bundle ID와 SHA 인증서 값이 환경별로 정확해야 한다. 콘솔 생성·billing 변경은 이번 작업에서 수행하지 않았다.
- 다음 작업: 먼저 staging Google Cloud 프로젝트에 Firebase를 추가하고 Authentication을 초기화한 다음 모바일 앱 등록, Identity Platform 업그레이드, phone·Kakao provider 설정과 실기기 E2E를 순서대로 진행한다.

## 2026-09-07 — Firebase 최초 연결 안내 turn 종료 동기화

<!-- codex-turn:01a07a23-dca1-7640-a8ca-5f864dcff38c -->

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`.
- Jira: 조회·생성·수정·댓글·상태 변경 없음.
- 작업 목표: 종료 훅이 지정한 turn 식별자로 Firebase 미연결 상태의 최초 프로젝트 구성 안내를 최종 기록한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·런타임 설정·계약·테스트 코드는 변경하지 않았다.
- 구현 내용: 코드 구현은 없다. staging Google Cloud 프로젝트에 Firebase를 추가하고 Authentication·모바일 앱을 등록한 뒤 billing·Identity Platform, phone과 Kakao Generic OIDC를 구성하는 선행 순서를 확인했다.
- 실행한 테스트와 결과: 문서 기록만 수행해 Gradle 테스트를 실행하지 않았고 `git diff --check`를 실행한다.
- 유지한 계약: Identity는 exact `FIREBASE_PROJECT_ID`와 Firebase Admin ADC를 사용하며 실제 credential을 저장소에 두지 않는다. 환경별 프로젝트를 분리하고 검증 전 관련 feature flag를 false로 유지한다.
- 결정사항: 기존 staging Google Cloud 프로젝트가 있다면 새 프로젝트를 만들지 않고 그 프로젝트에 Firebase를 추가하며 production은 별도 프로젝트로 분리한다.
- 위험 요소: 모바일 package·bundle ID, Android SHA, iOS 검증 설정, callback URL과 Identity Platform billing이 실제 환경에서 아직 확인되지 않았다.
- 다음 작업: staging Firebase 프로젝트 생성·연결 후 Authentication, 모바일 앱, phone·Kakao provider 순으로 구성하고 실기기 E2E를 수행한다.

## 2026-09-07 — 삭제한 Firebase 앱 ID 충돌 복구 방법 확인

<!-- codex-turn:01a07a2c-b219-71e0-97ff-b9e4f147b84d -->

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`.
- Jira: 조회·생성·수정·댓글·상태 변경 없음.
- 작업 목표: Firebase 앱을 실수로 등록 후 삭제했을 때 동일 package ID 또는 bundle ID로 재등록할 수 없는 원인과 안전한 복구 방법을 확인한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·런타임 설정·계약·테스트 코드는 변경하지 않았다.
- 공식 문서 확인: Firebase Management REST API의 Android app remove·list·undelete 계약을 확인했다. 기본 remove는 앱을 즉시 영구 삭제하지 않고 30일 뒤 만료되는 `DELETED` 상태로 두며 그 기간에는 undelete할 수 있다. list의 `showDeleted=true`로 삭제 상태 앱을 조회할 수 있다.
- 구현 내용: 코드 구현은 없다. 동일 ID 신규 생성을 반복하지 않고 삭제된 exact Firebase App을 복원한 뒤 display name, Android SHA 또는 플랫폼 설정을 수정하는 절차를 권장했다. Android·iOS·Web은 각 플랫폼 app resource의 list·undelete API를 사용한다.
- 실행한 테스트와 결과: 외부 공식 문서 확인과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았다. 문서 변경에는 `git diff --check`를 실행한다.
- 유지한 계약: 프로젝트 ID·앱 ID·package/bundle ID만 대상으로 하며 access credential이나 실제 Token을 저장소·작업 기록에 남기지 않는다. 외부 Firebase project에는 변경을 수행하지 않았다.
- 결정사항: 기본 해결책은 30일 이내 삭제 앱 복원이다. 영구 삭제됐거나 복원 대상이 조회되지 않으면 활성 중복 앱 여부와 프로젝트 선택을 확인한 뒤 Firebase Support에 문의하며 임시 package ID 변경으로 우회하지 않는다.
- 위험 요소: 콘솔의 삭제 방식과 경과 시간에 따라 즉시 영구 삭제됐을 수 있고, 다른 Firebase 프로젝트 또는 동일 프로젝트의 ACTIVE app이 동일 ID를 점유하는 경우도 있다. 정확한 플랫폼과 Firebase project를 먼저 확인해야 한다.
- 다음 작업: 해당 staging project에서 deleted app을 포함해 목록을 조회하고 exact app resource를 복원한 뒤 모바일 구성 파일과 인증 Provider 설정을 다시 내려받아 검증한다.

## 2026-09-07 — 신규 MEMBER 휴대폰 credential link 의미 설명

<!-- codex-turn:01a07a2c-b219-71e0-97ff-b9e4f147b84d -->

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`.
- 작업 목표: 신규 MEMBER 가입 계약의 primary SNS 로그인, 동일 Firebase User phone credential link와 강제 ID Token 갱신 의미를 설명한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·설정·계약·테스트 코드는 변경하지 않았다.
- 구현 내용: 코드 구현은 없다. Google·Apple·Kakao 로그인으로 생성된 Firebase UID의 현재 사용자에 SMS 인증 결과인 `PhoneAuthCredential`을 `linkWithCredential`로 추가하고, 변경된 provider 정보를 포함하도록 ID Token을 강제 갱신한 뒤 Identity signup에 제출하는 흐름을 설명했다.
- 실행한 테스트와 결과: 설명과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았고 `git diff --check`를 실행한다.
- 유지한 계약: phone credential로 별도 Firebase User를 만들거나 phone-only Identity 로그인을 허용하지 않는다. 전화번호는 로그인 ID나 자동 merge key가 아니라 SNS 소유와 함께 확인하는 가입 proof다.
- 결정사항: Identity는 서로 다른 SNS Token과 phone Token을 조합하는 방식이 아니라 동일 Firebase UID에 두 인증수단이 연결된 강제 갱신 ID Token 하나를 검증한다.
- 위험 요소: 클라이언트가 `linkWithCredential` 대신 phone sign-in을 사용하면 별도 UID 또는 credential collision이 생길 수 있으며, link 후 구 ID Token을 보내면 서버가 phone proof를 확인하지 못한다.
- 다음 작업: 모바일 staging에서 SNS UID가 phone link 전후 동일하고 force-refresh Token과 Admin UserRecord에 primary provider·phone provider가 함께 보이는지 검증한다.

## 2026-09-07 — 신규 MEMBER phone link 설명 turn 종료 동기화

<!-- codex-turn:01a07a30-45ca-7d02-a79b-e43c4df640f7 -->

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`.
- 작업 목표: 종료 훅이 지정한 현재 turn 식별자로 신규 MEMBER의 동일 Firebase User phone credential link 설명을 최종 기록한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·설정·계약·테스트 코드는 변경하지 않았다.
- 구현 내용: 코드 구현은 없다. SNS 로그인으로 확보한 Firebase UID에 SMS 인증 결과를 연결하고, 강제 갱신 ID Token으로 Identity가 SNS와 phone의 동일 UID 귀속을 검증한다는 의미를 예시로 설명했다.
- 실행한 테스트와 결과: 설명과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았고 `git diff --check`를 실행한다.
- 유지한 계약: phone-only 로그인, 별도 phone UID 생성과 전화번호 기반 자동 merge는 허용하지 않으며 Firebase는 credential broker로만 사용한다.
- 결정사항: 모바일은 현재 Firebase User에 `linkWithCredential`을 수행하고 link 후 `getIdToken(true)`에 해당하는 강제 갱신 Token을 signup에 제출한다.
- 위험 요소: phone sign-in을 사용하거나 link 이전 Token을 보내면 UID가 분리되거나 서버의 phone proof 검증이 실패할 수 있다.
- 다음 작업: staging 실기기에서 link 전후 UID 유지와 강제 갱신 Token의 provider 반영을 확인한다.

## 2026-09-07 — Cloud Billing 연결과 Identity Platform 활성화 절차 확인

<!-- codex-turn:01a07a37-eab9-76b2-b49c-926cb187c176 -->

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`.
- Jira: 조회·생성·수정·댓글·상태 변경 없음.
- 작업 목표: Firebase가 추가된 staging Google Cloud 프로젝트에 Cloud Billing을 연결하고 Identity Platform으로 업그레이드하는 콘솔 절차와 주의사항을 안내한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·런타임 설정·계약·테스트 코드는 변경하지 않았다.
- 공식 문서 확인: Google Cloud Billing의 기존 프로젝트 결제 활성화 절차와 Identity Platform·Firebase Authentication 제품 비교를 확인했다. 프로젝트는 활성 Billing 계정에 연결돼야 하며, Identity Platform 활성화 후 기존 Firebase SDK와 앱은 계속 동작하지만 OIDC 등 추가 기능과 유료 서비스 계약이 적용된다.
- 구현 내용: 코드 구현은 없다. 프로젝트 선택 확인, Billing의 내 프로젝트에서 결제 계정 연결, 예산 알림 설정, Identity Platform Marketplace 활성화·Firebase Authentication 업그레이드 승인, ID 공급업체 화면의 OpenID Connect 항목 확인 순서로 정리했다.
- 권한: 결제 연결에는 프로젝트 소유자 또는 프로젝트 결제 관리자·뷰어 권한과 대상 Billing 계정의 결제 계정 사용자·뷰어 또는 관리자 권한이 필요함을 확인했다.
- 실행한 테스트와 결과: 외부 공식 문서 확인과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았다. 문서 변경에는 `git diff --check`를 실행한다.
- 유지한 계약: staging과 production의 Firebase·Billing·Identity Platform 설정을 분리하고 실제 결제정보·credential·Token을 저장소나 작업 기록에 남기지 않는다. Identity feature flag는 Provider·모바일 E2E 전 false를 유지한다.
- 결정사항: staging 프로젝트에서 먼저 Identity Platform을 활성화하고 budget alert를 설정한다. budget alert는 지출을 자동 차단하지 않으므로 quota·SMS 비용·OIDC MAU를 별도로 관찰한다.
- 위험 요소: 다른 프로젝트에 Billing을 연결하거나 production에서 먼저 업그레이드할 수 있으며, 권한 부족 시 결제 변경 버튼이 보이지 않는다. Identity Platform 활성화는 가격·quota 조건을 바꾸므로 승인 화면의 현재 조건을 확인해야 한다.
- 다음 작업: staging 프로젝트의 Billing 연결·Identity Platform 활성화를 확인한 뒤 ID 공급업체에서 `oidc.kakao`를 등록하고 모바일 staging 로그인·link를 검증한다.

## 2026-09-07 — Identity Platform·Kakao OIDC·한국 SMS 비용 확인

<!-- codex-turn:01a07a3b-9aff-70a1-976c-cf08747bf2b7 -->

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`.
- Jira: 조회·생성·수정·댓글·상태 변경 없음.
- 작업 목표: Cloud Billing 연결과 Identity Platform 활성화 시 비용 발생 여부를 Kakao Generic OIDC와 Firebase Phone Authentication 사용량 기준으로 설명한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·런타임 설정·계약·테스트 코드는 변경하지 않았다.
- 공식 가격 확인: Identity Platform은 월간 활성 사용자와 전송 SMS 기준 종량제다. Tier 1은 월 50,000 MAU까지 무료이고, OIDC·SAML Tier 2는 프로젝트당 월 50 MAU까지 무료이며 초과분은 현재 공식 USD 가격표상 MAU당 0.015달러다. 전화 인증은 매일 처음 10건의 전송 SMS가 무료이며 대한민국은 현재 전송 SMS당 0.01달러다.
- 구현 내용: 코드 구현은 없다. Billing 계정 연결과 Identity Platform 활성화 자체에는 고정 월요금이 없지만 Kakao OIDC 로그인 사용자와 실제 SMS 발송이 무료 구간을 넘으면 청구될 수 있음을 구분했다.
- 실행한 테스트와 결과: 외부 공식 가격표 확인과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았다. 문서 변경에는 `git diff --check`를 실행한다.
- 유지한 계약: 비용 정보에 실제 결제정보·사용자 개인정보·credential을 포함하지 않았으며 staging과 production을 분리하고 관련 feature flag를 검증 전 false로 유지한다.
- 결정사항: staging에서는 Firebase 테스트 전화번호 또는 Emulator를 우선 사용하고 실제 SMS, 허용 국가와 quota를 최소화한다. budget alert는 설정하되 hard cap이 아니라는 점을 운영 계약에 반영한다.
- 위험 요소: 가격, 환율과 세금은 변경될 수 있고 SMS abuse는 비용을 빠르게 증가시킬 수 있다. 하나의 연결 계정이 OIDC로 활동하고 SMS 인증도 수행하면 OIDC MAU와 SMS 발송 비용을 각각 고려해야 한다.
- 다음 작업: staging budget·billing report·SMS metric과 region restriction을 설정한 뒤 제한된 테스트 계정으로 Kakao OIDC와 phone link 비용을 관찰한다.

## 2026-09-07 — Google Cloud $300 무료 체험 크레딧 적용 범위 확인

<!-- codex-turn:01a07a3f-8d45-7383-8bce-b5d05cdce2ae -->

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`.
- Jira: 조회·생성·수정·댓글·상태 변경 없음.
- 작업 목표: 90일 $300 Google Cloud 무료 체험 크레딧으로 Identity Platform의 Kakao OIDC와 phone SMS 사용료를 처리할 수 있는지 확인한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·런타임 설정·계약·테스트 코드는 변경하지 않았다.
- 공식 문서 확인: 무료 체험 결제 계정은 포함 제품의 사용량 비용을 90일 또는 $300 소진 시점까지 크레딧으로 상계한다. 공개 제외 목록에는 Identity Platform이 명시되지 않았으므로 해당 결제 계정에 연결된 프로젝트의 적격 Identity Platform·Firebase Authentication SKU는 일반적으로 크레딧 적용 대상이다.
- 비용 적용: Kakao OIDC 월 50 MAU와 phone SMS 일 10건의 자체 무료 구간이 먼저 적용되고 초과 사용량이 비용으로 계산된 뒤 무료 체험 크레딧에서 차감된다. 정확한 적용은 Cloud Billing 보고서의 SKU와 credit 항목으로 확인한다.
- 실행한 테스트와 결과: 외부 공식 무료 프로그램·가격 문서 확인과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았다. 문서 변경에는 `git diff --check`를 실행한다.
- 유지한 계약: 실제 결제수단·결제 계정 ID·사용자정보·credential을 저장소나 기록에 남기지 않았고 외부 결제 설정을 변경하지 않았다.
- 결정사항: staging 프로젝트를 무료 체험 결제 계정에 연결하고 billing report·budget alert로 credit 차감을 확인한다. 무료 체험 제한으로 SMS 또는 특정 기능이 막히면 유료 계정 전환 여부를 별도로 판단한다.
- 위험 요소: 90일 만료 또는 $300 소진 시 무료 체험 계정은 유료 전환 전까지 서비스와 리소스가 중지될 수 있다. 유료 계정으로 전환하면 남은 크레딧은 원래 90일 기한까지 유지되지만 크레딧 초과·제외 사용량은 실제 결제수단에 청구된다.
- 다음 작업: Cloud Billing 개요에서 계정 유형·남은 credit·만료일과 staging 프로젝트 연결 상태를 확인하고 소량의 OIDC·테스트 SMS 사용 후 보고서의 적용 SKU를 검증한다.

## 2026-09-07 — 사용자 무료 사용권 조회 기능 경계 검토

<!-- codex-turn:01a07a49-c29e-76e1-9945-46ebbe6992ff -->

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`.
- Jira: 조회·생성·수정·댓글·상태 변경 없음.
- 작업 목표: 사용자가 보유한 무료 사용권 수량을 앱에서 조회하도록 만들 때의 서비스 소유권, 현재 구현 간극과 자연스러운 API 의미를 검토한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. Identity·Billing·Learning Core의 애플리케이션·설정·계약·테스트 코드는 변경하지 않았다.
- 확인한 구현 사실: Billing은 `BenefitDefinition`, `TrialEligibility`, `TrialClaim`, `BillingSubjectLink`, `EntitlementGrant`와 available·held·consumed unit을 소유한다. 현재 Controller와 Security는 internal workload API만 제공하고 public 사용자 entitlement 조회 API와 사용자 JWT verifier는 아직 없다.
- 핵심 판정: 무료 Claim·Grant는 verified eligibility event 시점이 아니라 최초 reserve Transaction에서 lazy 생성된다. 따라서 Grant의 availableUnits만 합산하면 아직 reserve하지 않은 정상 사용자를 0개로 오판하므로, read-only resolver가 eligibility와 Claim·Grant 상태를 함께 판단해야 한다.
- 권장 계약: 앱은 Billing의 `GET /api/v1/entitlements`를 사용자 JWT `sub`와 `billing:read`로 호출하고 benefit별 `benefitCode`, `displayName`, `unitType`, `availableUnits`, `state`를 받는다. 내부 Claim·Grant·candidate·phone fingerprint·Reservation ID는 노출하지 않는다.
- 상태 의미: VERIFIED이며 Claim이 없으면 읽기 부작용 없이 claimable 1개를 AVAILABLE로 표시하고, 기존 Grant가 있으면 available·held·consumed projection으로 AVAILABLE·IN_USE·USED를 계산한다. 구성 drift나 Repository 장애를 0개로 위장하지 않고 503으로 구분한다.
- 실행한 테스트와 결과: 저장소 간 읽기 전용 검토와 문서 기록만 수행해 Gradle 테스트는 실행하지 않았다. 문서 변경에는 `git diff --check`를 실행한다.
- 유지한 계약: Identity는 사용자·JWT를, Billing은 entitlement 진실 공급원을, Learning Core는 시험 lifecycle을 소유한다. 조회로 Claim·Grant를 생성하거나 소비하지 않고 client 제공 userId를 신뢰하지 않는다.
- 결정사항: Google Cloud 결제 계정과 Firebase OIDC 연결이 없어도 로컬 코드·계약 구현은 진행할 수 있다. public Billing reader를 먼저 배포한 뒤 Identity가 `tosunsaeng-billing` audience와 `billing:read` scope를 발급하는 reader-first 순서를 유지한다.
- 위험 요소: 서로 다른 unitType의 사용권을 하나의 총합으로 더하면 의미가 깨지므로 benefit별 수량을 기본 응답으로 사용한다. eligibility event 지연, held 상태, 재가입 owner rebind와 future paid entitlement를 같은 0개 값으로 뭉개면 UX와 장애 진단이 어려워진다.
- 다음 작업: Billing public entitlement read API ADR에서 exact DTO·상태·오류·rate limit과 JWT verifier를 확정하고 Billing reader → Identity audience/scope → 모바일 UI 순서로 구현한다.

## 2026-09-07 — 프론트 Firebase·SNS 로그인 연동 가이드 교정 및 API 응답 정리

<!-- codex-turn:01a07a51-b016-7963-88ee-84ed89d5fdb7 -->

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`.
- Jira: 조회·생성·수정·댓글·상태 변경 없음.
- 작업 목표: 기존에 프론트에 전달한 로그인·회원 전환 문서를 현재 Identity 구현과 대조해 잘못된 내용을 교정하고, 연동 대상 API별 요청·성공 응답·주요 실패 범위를 추가한다.
- 변경 파일: `docs/contracts/frontend-firebase-auth-integration-guide.md`, `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·런타임 설정·테스트 코드는 변경하지 않았다.
- 구현 내용: 프론트 전달용 가이드를 새로 저장했다. Google·Apple·Kakao Firebase exchange, 신규 MEMBER same-UID phone link와 signup, Guest 생성·prepare·upgrade·merge, MEMBER auth-method sync, Token reissue·logout, 프로필·Firebase 회원탈퇴 및 legacy email API를 공통 `BaseResponse` 예시와 함께 정리했다.
- 교정 내용: Kakao 흐름을 포함하고 Guest `ALREADY_LINKED`를 정상 MEMBER 진입으로 취급하지 않도록 수정했다. `FIREBASE_RECENT_AUTH_REQUIRED`는 단순 force refresh가 아니라 명시적 재인증이 필요하며, `ACCOUNT_MERGED_TOKEN_REJECTED`는 `/reissue` 대표 오류가 아니라 MERGED Guest Access Token의 보호 API 경계임을 구분했다. 현재 logout-all은 Identity RefreshSession만 전부 폐기하고 다른 기기의 Firebase refresh revoke는 아직 하지 않는다는 한계도 명시했다.
- API 계약: Firebase ID Token, Identity Access Token과 Refresh Token의 사용처를 분리하고 Token 만료시간 단위를 밀리초로 고정했다. API별 인증 방식, success result, 주요 HTTP 범위와 JSON 예시를 DTO 기준으로 기록했으며 `linkedProviders`·`missingRequirements`의 배열 순서에는 의존하지 않도록 했다.
- 실행한 테스트와 결과: 문서 전용 변경이라 Gradle 테스트는 실행하지 않았다. `git diff --check`와 변경 파일 범위 확인을 수행한다.
- 유지한 계약: Firebase는 credential broker이고 Identity가 canonical UUID User·자체 Access/Refresh Token을 소유한다. Firebase ID Token을 Learning Core에 보내지 않고 phone-only 로그인·phone 자동 merge·클라이언트 제공 userId를 허용하지 않으며 Secret·Token·Password·phone 원문을 기록하지 않는다.
- 결정사항: 인증 전 필수 정책 version 공급 방식, legacy password와 Firebase password의 출시 경로, Guest merge 확인 UX와 Provider button remote configuration은 프론트·제품·백엔드가 출시 전에 확정해야 한다.
- 위험 요소: Firebase·Google·Apple·Kakao·phone과 Guest merge 관련 feature flag는 기본 false다. 실제 모바일 redirect·deep-link·SMS, Mongo Transaction, withdrawal cleanup, Billing·Learning Core consumer E2E가 완료되지 않았고 logout-all Firebase revoke·Refresh Token 응답 유실 복구·Provider unlink·phone 변경·Guest 응답 유실 복구도 남아 있다.
- 다음 작업: 프론트가 문서의 결정 필요 항목을 확정한 뒤 staging 앱에서 Provider별 login, same-UID phone link, 신규 signup, Guest upgrade·merge, reissue single-flight, logout·withdrawal을 순서대로 종단 검증한다.

## 2026-09-07 — 프론트 인증 가이드를 전체 개발 완료 기준으로 전환

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`.
- Jira: 조회·생성·수정·댓글·상태 변경 없음.
- 작업 목표: 프론트 로그인·회원 전환 가이드를 현재 미완료 상태의 주의 문서가 아니라 모든 인증 후속 개발과 staging 검증이 완료된 최종 release 계약으로 다시 작성한다.
- 변경 파일: `docs/contracts/frontend-firebase-auth-integration-guide.md`, `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·런타임 설정·테스트 코드는 변경하지 않았다.
- 구현 내용: 최종 release에서는 Firebase email/password·Google·Apple·Kakao와 phone, Identity·Firebase·Billing·Learning Core 종단 연동이 활성화된 것으로 전제했다. logout-all은 내부 전체 Session 폐기와 Firebase refresh revoke를 함께 보장하고, reissue와 Guest 생성은 응답 유실을 복구하며, Provider unlink·phone 변경과 기존 LOCAL 회원 rebind도 완료된 상태로 정리했다.
- 프론트 계약: legacy email/password API는 신규 화면에서 사용하지 않고 Firebase exchange/signup 흐름으로 통일한다. bare `ALREADY_LINKED`를 MEMBER 성공으로 해석하지 않으며, final Guest recovery 결과를 받은 뒤에만 진행한다. 환경별 Provider capability와 서버 정책 metadata를 기준으로 UI를 구성한다.
- exact wire 경계: 아직 저장소에 별도 확정 문서가 없는 Provider unlink·phone 변경, Guest 응답 유실 복구와 reissue 응답 복구의 신규 URL·필드는 임의로 발명하지 않았다. 최종 구현 시 각 전용 API 문서를 이 가이드와 함께 제공한다.
- 실행한 테스트와 결과: 문서 전용 변경이라 Gradle 테스트는 실행하지 않았다. `git diff --check`와 변경 파일 범위 확인을 수행한다.
- 유지한 계약: Firebase는 credential broker이고 Identity가 canonical UUID User와 자체 Token을 소유한다. phone-only 로그인, phone·email 기반 자동 merge, 클라이언트 제공 userId와 민감정보 기록은 허용하지 않는다.
- 결정사항: 이 문서는 현재 배포 가능 여부를 설명하지 않고, 완료된 production release가 프론트에 제공해야 하는 동작만 정의한다. 부분 구현 환경은 capability가 보장한 기능만 노출한다.
- 위험 요소: 기존 API의 exact JSON 예시는 현재 DTO에 근거하지만 8~12단계에서 wire가 바뀌면 문서를 같은 변경에서 갱신해야 한다. 별도 계약 없이 목표 동작만 구현 완료로 간주하면 프론트와 서버의 retry·recovery 방식이 어긋날 수 있다.
- 다음 작업: Stage 8~12의 계획서에서 exact endpoint·request·response·idempotency key를 확정할 때 이 프론트 가이드의 API 카탈로그도 함께 갱신한다.

## 2026-09-07 — 프론트 인증 최종 release 가이드 turn 종료 동기화

<!-- codex-turn:01a07a57-f2eb-7b33-903a-3a63101e22b1 -->

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`.
- Jira: 조회·생성·수정·댓글·상태 변경 없음.
- 작업 목표: 종료 훅이 지정한 현재 turn 식별자로 프론트 로그인·회원 전환 가이드를 전체 개발 완료 기준으로 전환한 결과를 최종 기록한다.
- 변경 파일: `docs/contracts/frontend-firebase-auth-integration-guide.md`, `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·런타임 설정·테스트 코드는 변경하지 않았다.
- 구현 내용: 현재 미완료 경고 중심이던 가이드를 인증 후속 단계와 실제 Firebase·모바일·Mongo·Billing·Learning Core staging 검증이 모두 완료된 production release 계약으로 변경했다. Firebase email/password·Google·Apple·Kakao, phone link, Guest 전환·merge, 응답 유실 복구, 전체 로그아웃, 탈퇴·재가입과 인증수단 lifecycle의 최종 동작을 정리했다.
- API 계약: 현재 확정된 Firebase exchange/signup, Guest, session, profile과 withdrawal API의 요청·응답 예시는 유지했다. Provider unlink·phone 변경, Guest·reissue 응답 복구처럼 아직 exact wire 문서가 없는 신규 API의 URL·필드는 임의로 작성하지 않고 최종 전용 계약을 따르게 했다.
- 실행한 테스트와 결과: 문서 전용 변경이므로 Gradle 테스트는 실행하지 않았다. `git diff --check`를 실행해 통과 여부를 확인한다.
- 유지한 계약: Firebase는 credential broker이고 Identity가 canonical UUID User와 자체 Access/Refresh Token을 소유한다. Firebase ID Token의 downstream 전송, phone-only 로그인, phone·email 자동 merge, 클라이언트 제공 userId와 Secret·Token·Password·phone 원문 기록을 허용하지 않는다.
- 결정사항: 프론트 문서는 부분 구현 현황이 아니라 모든 release gate가 충족된 최종 동작을 설명한다. 부분 구현 환경은 서버 capability로 허용된 기능만 노출한다.
- 위험 요소: Stage 8~12의 exact wire가 이후 확정될 때 이 문서를 함께 갱신하지 않으면 목표 동작과 실제 request·response가 어긋날 수 있다.
- 다음 작업: 각 후속 API 계약 확정 시 이 가이드의 API 카탈로그와 프론트 오류·retry 표를 같은 변경에서 갱신한다.

## 2026-09-07 — Firebase signup nickname 출처 설명 및 프론트 가이드 보완

<!-- codex-turn:01a07a60-b8f6-7bf3-879a-60b04fd30127 -->

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`.
- Jira: 조회·생성·수정·댓글·상태 변경 없음.
- 작업 목표: `/api/v1/auth/firebase/signup` 요청의 nickname을 로그인 시점에 어떻게 확보하는지 명확히 설명하고 프론트 문서의 로그인·가입 구분을 보완한다.
- 변경 파일: `docs/contracts/frontend-firebase-auth-integration-guide.md`, `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·런타임 설정·테스트 코드는 변경하지 않았다.
- 구현 내용: `/firebase/signup`은 기존 MEMBER 로그인이 아니라 `/firebase/exchange`에서 `ENROLLMENT_REQUIRED`를 받은 신규 사용자의 가입 완료 API임을 명시했다. nickname은 가입 화면에서 사용자가 직접 입력·확정하며 Identity가 Firebase Provider 표시 이름을 자동 저장하지 않는다는 계약을 추가했다.
- 프론트 처리: Provider profile의 표시 이름은 편의상 nickname 입력 초기값으로 제안할 수 있지만 사용자가 확인·수정하게 한다. 기존 MEMBER가 `AUTHENTICATED`를 받으면 `/firebase/signup`을 호출하지 않고 nickname도 전송하지 않는다.
- 실행한 테스트와 결과: 문서 전용 변경이므로 Gradle 테스트는 실행하지 않았다. `git diff --check`를 실행해 통과 여부를 확인한다.
- 유지한 계약: nickname은 profile 입력이며 Firebase UID, Provider subject, email·phone과 canonical userId 선택 기준으로 사용하지 않는다. Token·credential·Password·개인정보를 작업 기록에 포함하지 않았다.
- 결정사항: 로그인과 신규 가입 완료를 UI·API 상태로 분리하고 `result.type`이 `ENROLLMENT_REQUIRED`일 때만 nickname 입력 단계를 노출한다.
- 위험 요소: Provider 표시 이름을 그대로 확정하면 빈 값·길이·금칙어·사용자 의도 불일치가 생길 수 있으므로 서버의 2~20자 검증과 프론트 입력 검증을 함께 적용해야 한다.
- 다음 작업: 프론트 signup 화면에서 `missingRequirements`에 따라 nickname·동의·phone verification 단계를 구성하고, 기존 MEMBER 로그인에서는 해당 화면이 나타나지 않는지 E2E로 검증한다.

## 2026-09-07 — 다음 작업 Stage 8 logout-all Firebase revoke 설명

<!-- codex-turn:01a07a66-efb1-7fd3-8bbf-18368842308b -->

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`.
- Jira: 조회·생성·수정·댓글·상태 변경 없음.
- 작업 목표: Stage 7 다음 고정 순서인 `logout-all Firebase refresh revoke`가 해결하는 문제와 구현 경계, 사용자에게 보이는 동작을 현재 코드·ADR·후속 순서 문서 기준으로 설명한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·계약·런타임 설정·테스트 코드는 변경하지 않았다.
- 확인한 현재 동작: `LogoutAllService`는 Identity JWT `sub`의 활성 RefreshSession을 조회해 `LOGOUT_ALL`로 폐기하지만 FirebaseIdentity를 조회하거나 Firebase Admin revoke를 요청하지 않는다. 따라서 다른 기기의 기존 Firebase client session이 다시 `/firebase/exchange`를 호출할 수 있는 공백이 있다.
- 권장 구현: 모든 Identity RefreshSession 폐기와 Firebase revoke job/outbox 생성을 하나의 Mongo Transaction으로 commit한다. worker는 exact project·Firebase UID를 preflight한 뒤 refresh token revoke를 멱등 실행하고, timeout·quota·일시 장애를 retry하며 권한·설정·불명확 결과를 reconciliation 대상으로 격리한다.
- 사용자 동작: 요청 기기는 `/logout-all` 성공 뒤 Identity Token·민감 cache를 삭제하고 Firebase client `signOut`을 수행한다. 다른 기기는 Identity Refresh가 즉시 막히며 Firebase revoke가 반영된 뒤 기존 Firebase credential로 exchange할 수 없다.
- 제외 범위: Firebase User disable·delete, SNS Provider unlink, 전화번호 변경, User 탈퇴, 일반 current-device logout 변경과 기존 stateless Identity Access Token의 즉시 blacklist는 포함하지 않는다. 기존 Access Token은 최대 TTL까지 유효할 수 있으며 필요하면 별도의 active-session deny 설계가 필요하다.
- 실행한 테스트와 결과: 읽기 전용 분석과 작업 기록만 수행해 Gradle 테스트는 실행하지 않았다. `git diff --check`를 실행해 문서 변경을 검증한다.
- 유지한 계약: 클라이언트 userId를 신뢰하지 않고 Identity JWT `sub`와 서버 FirebaseIdentity mapping으로 대상을 정한다. Firebase UID·Token·Provider credential을 외부 응답·로그·outbox에 불필요하게 노출하지 않는다.
- 결정사항: `/logout-all` HTTP 요청 안에서 Firebase 원격 호출 완료를 기다리지 않고 내부 폐기와 durable revoke 인계를 성공 기준으로 삼는 방식을 권장한다. 외부 실패가 내부 Session 폐기를 되돌리지 않는다.
- 위험 요소: Firebase revoke worker 처리 전 짧은 재로그인 창과 이미 발급된 Identity Access Token TTL 공백이 남을 수 있다. Transaction, exact target fencing, 중복 logout-all, Firebase user 없음과 Firebase 장애 시나리오를 staging에서 검증해야 한다.
- 다음 작업: Stage 8 계획서에서 job 상태·멱등 key·retry·dead-letter·응답 의미·feature flag·Access Token 잔여 정책을 확정한 뒤 Jira를 생성하고 구현한다.

## 2026-09-07 — 사용자 무료 사용권 조회 작업 우선순위 재확인

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`; Billing은 읽기 전용 상태 확인만 수행했다.
- Jira: 조회·생성·수정·댓글·상태 변경 없음.
- 작업 목표: 앞서 논의한 사용자의 무료 사용권 수량 조회 기능이 다음 인증 작업과 어떤 관계인지, 어느 서비스에서 어떤 순서로 구현해야 하는지 다시 정리한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. Identity·Billing 애플리케이션·계약·런타임 설정·테스트 코드는 변경하지 않았다.
- 확인 결과: 무료 사용권의 진실 공급원은 Billing이며 현재 Billing에는 internal event·reservation API만 있고 앱이 호출할 public entitlement reader와 사용자 JWT resource server가 없다. Firebase 고정 후속 Stage 8은 logout-all lifecycle 작업이므로 entitlement 조회와 직접적인 선후 의존성이 없다.
- 권장 구현: Billing이 side-effect 없는 `GET /api/v1/entitlements`에서 JWT `sub` 기준 benefit별 `benefitCode`, `displayName`, `unitType`, `availableUnits`, `state`를 반환한다. 아직 lazy Claim·Grant가 없는 verified eligibility도 0으로 오판하지 않되 조회 자체가 Claim·Grant를 생성하거나 소비하면 안 된다.
- 인증 순서: Billing reader·JWT verifier를 feature flag OFF로 먼저 배포한 뒤 Identity Access Token audience에 `tosunsaeng-billing`을 추가하고 Guest·MEMBER에 최소 `billing:read` scope를 발급한다. 이후 프론트가 같은 Identity Access Token으로 Billing public endpoint를 호출한다.
- 실행한 테스트와 결과: 읽기 전용 분석과 작업 기록만 수행해 Gradle 테스트는 실행하지 않았다. `git diff --check`를 실행해 문서 변경을 검증한다.
- 유지한 계약: Identity는 사용자와 Token 발급을, Billing은 Entitlement를 소유한다. 클라이언트 userId, phone fingerprint, Claim·Grant 내부 ID와 Reservation ID를 공개 조회 요청·응답에 추가하지 않는다.
- 결정사항: 무료 사용권 화면이 현재 제품 우선순위라면 Billing public reader 작업을 Stage 8보다 먼저 진행할 수 있다. Firebase·Google Cloud 결제 계정 설정 완료를 기다릴 필요가 없다.
- 위험 요소: 서로 다른 `unitType`의 권리를 하나의 총수량으로 합치면 의미가 깨진다. eligibility event 지연·IN_USE hold·USED·구성 장애를 모두 0개로 뭉개지 않도록 상태와 오류 계약을 먼저 확정해야 한다.
- 다음 작업: Billing 저장소에 public entitlement read API 계획서를 작성해 exact response, 상태 계산, JWT audience·scope, 오류·rate limit과 테스트를 확정한 뒤 Billing reader부터 구현한다.

## 2026-09-07 — 무료 사용권 조회 우선순위 turn 종료 동기화

<!-- codex-turn:01a07a69-0a38-7471-a429-f5ed45a79e14 -->

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`; Billing은 읽기 전용 확인만 수행했다.
- Jira: 조회·생성·수정·댓글·상태 변경 없음.
- 작업 목표: 종료 훅이 지정한 현재 turn 식별자로 사용자 무료 사용권 수량 조회 기능의 책임 서비스와 구현 순서를 최종 기록한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. Identity·Billing 애플리케이션·계약·런타임 설정·테스트 코드는 변경하지 않았다.
- 확인 결과: 무료 사용권 조회는 Firebase 인증 Stage 8에 포함되지 않는 Billing public reader 작업이다. 제품 우선순위에 따라 먼저 구현해도 Firebase 후속 단계의 내부 순서를 변경하지 않는다.
- 구현 순서: Billing의 side-effect 없는 `GET /api/v1/entitlements`와 사용자 JWT verifier를 먼저 구현하고, Identity Access Token에 `tosunsaeng-billing` audience와 최소 `billing:read` scope를 추가한 뒤 프론트 사용권 화면을 연동한다. 이후 Firebase Stage 8로 복귀한다.
- 응답 방향: benefit별 `benefitCode`, `displayName`, `unitType`, `availableUnits`, `state`를 반환한다. lazy Claim·Grant가 아직 없어도 verified eligibility를 0으로 오판하지 않으며 조회가 Claim·Grant를 생성하거나 소비하지 않는다.
- 실행한 테스트와 결과: 읽기 전용 분석과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았다. `git diff --check`를 실행해 통과 여부를 확인한다.
- 유지한 계약: Identity는 사용자·JWT를 소유하고 Billing은 Entitlement의 진실 공급원이다. client userId, phone fingerprint와 내부 Claim·Grant·Reservation 식별자를 공개 API에 노출하지 않는다.
- 결정사항: Google Cloud Billing 계정과 Kakao 설정 없이도 이 기능 개발을 시작할 수 있으며, 사용권 화면이 우선이면 Billing reader를 다음 작업으로 삼는다.
- 위험 요소: 다른 `unitType`을 총수량 하나로 합치지 않고 eligibility 지연·hold·consumed·장애를 서로 다른 상태와 오류로 표현해야 한다.
- 다음 작업: Billing public entitlement read API 계획서에서 exact DTO, projection 상태, JWT 검증, 오류·rate limit과 테스트를 확정한다.

## 2026-09-07 — 무료 사용권 조회와 Firebase Stage 8 우선순위 결정

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`.
- Jira: 조회·생성·수정·댓글·상태 변경 없음.
- 작업 목표: Billing 무료 사용권 조회와 Firebase Stage 8 logout-all revoke 중 어떤 작업을 먼저 진행할지 현재 활성화 상태와 제품 가치를 기준으로 결정한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·계약·런타임 설정·테스트 코드는 변경하지 않았다.
- 비교 결과: Firebase/SNS 기능은 production flag가 아직 비활성이므로 logout-all의 Firebase revoke 미구현이 현재 사용자에게 노출된 보안 회귀는 아니다. 무료 사용권 조회는 Google Cloud Billing·Kakao 설정 없이 Billing과 Identity 코드만으로 시작할 수 있고 프론트에 직접적인 사용자 기능을 제공한다.
- 권장 순서: Billing public entitlement reader 계획·구현 → Identity Billing audience·`billing:read` scope → 프론트 화면 연동 → Firebase Stage 8 계획·구현 순서로 진행한다.
- 예외 조건: 무료 사용권 화면보다 SNS 로그인을 먼저 production에 출시한다면 Stage 8을 선행 release blocker로 바꾸고 Firebase refresh revoke까지 완료한 뒤 로그인 기능을 활성화한다.
- 실행한 테스트와 결과: 우선순위 분석과 문서 기록만 수행해 Gradle 테스트는 실행하지 않았다. `git diff --check`를 실행해 문서 변경을 검증한다.
- 유지한 계약: Firebase 인증 후속 작업의 내부 순서 8→9→10→11→12는 유지한다. 별도 Billing 조회 트랙을 먼저 수행해도 이 순서를 건너뛰거나 Stage 8 완료로 간주하지 않는다.
- 결정사항: 현재 조건에서는 무료 사용권 조회를 먼저 진행하는 것을 권장한다.
- 위험 요소: entitlement reader를 만들면서 조회가 lazy Grant 생성·소비를 일으키거나 서로 다른 unitType을 한 총수량으로 합치면 안 된다. SNS production 활성화 일정이 앞당겨지면 우선순위를 즉시 재평가해야 한다.
- 다음 작업: Billing 저장소에서 public entitlement read API 계획서를 작성한다.

## 2026-09-07 — 무료 사용권 조회 우선 진행 결정 동기화

<!-- codex-turn:01a07a6a-8adf-71d1-b5b7-c1bd1b06a6dc -->

- 날짜: 2026-09-07
- 브랜치: Identity `develop`, HEAD `fa9843e`.
- Jira: 조회·생성·수정·댓글·상태 변경 없음.
- 작업 목표: 무료 사용권 조회와 Firebase Stage 8 중 먼저 진행할 작업을 최종 추천한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. 애플리케이션·계약·테스트 코드는 변경하지 않았다.
- 결정사항: 현재는 Billing public entitlement reader를 먼저 진행한다. 이후 Identity JWT에 Billing audience·read scope를 추가하고 프론트를 연동한 다음 Firebase Stage 8로 복귀한다.
- 근거: Firebase/SNS는 아직 production 비활성이며, 사용권 조회는 외부 Firebase·Kakao 설정 없이 개발할 수 있고 사용자에게 바로 보이는 기능이다.
- 예외 조건: SNS 로그인을 먼저 production에 공개하기로 일정이 바뀌면 Stage 8 logout-all Firebase revoke를 release blocker로 앞당긴다.
- 실행한 테스트와 결과: 문서 기록만 변경했으므로 Gradle 테스트는 실행하지 않았다. `git diff --check`로 문서 diff를 검증한다.
- 유지한 계약: Entitlement는 Billing이 소유하며 Identity는 사용자 JWT 발급만 담당한다. 조회는 Claim·Grant 생성이나 사용권 소비를 일으키지 않는다.
- 위험 요소: Billing reader 구현 시 lazy eligibility를 0개로 오판하거나 서로 다른 `unitType`을 하나의 총수량으로 합치지 않아야 한다.
- 다음 작업: Billing public entitlement read API 계획서를 작성한다.

## 2026-09-07 — 사용자 Access Token account_type claim 추가

<!-- codex-turn:01a07a71-4e58-7840-bfa5-ac540fb53fbb -->

- 날짜: 2026-09-07
- 브랜치: `develop`, 기준 HEAD `fa9843e`. 이번 변경은 미커밋 상태다.
- Jira: 이번 요청에 이슈 키가 없으며 조회·생성·수정·댓글·상태 변경을 수행하지 않았다.
- 작업 목표: Learning Core 10초 Challenge의 MEMBER 인가를 위해 모든 Identity 사용자 Access Token에 신뢰된 현재 계정 유형을 발급한다. 사용자 지시에 따라 무료 사용권 조회보다 먼저 구현했다.
- 변경 파일: `global/security/jwt/AccessTokenIssuer.java`, `JwtAccessTokenIssuer.java`; `GuestAuthService`, `LoginService`, `FirebaseSignupService`, `FirebaseExchangeService`, `TokenReissueService`, `FirebaseGuestUpgradeService`, `FirebaseGuestMergeService`; 대응 서비스·Controller·Security·JWT·Guest lifecycle 테스트 13개 파일; `docs/contracts/identity-learning-jwt.md`, `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. Java 경로는 `src/main/java/web/tosunsaeng/identity`와 `src/test/java/web/tosunsaeng/identity` 기준이다.
- 구현 내용: `AccessTokenIssuer.issue(userId, accountType, scopes)`에 UserAccountType 필수 인자를 추가하고 발급기는 `account_type`을 enum 이름의 문자열 MEMBER/GUEST로 넣는다. null 유형은 IllegalArgumentException으로 거절하며 유형 없는 이전 overload나 MEMBER 기본값은 제공하지 않는다.
- 전체 경로: Guest 생성은 새 Guest User, 로컬·Firebase 로그인은 검증된 현재 User, Firebase 가입·Guest 승격은 Transaction 성공 후 User, merge는 target User의 유형을 전달한다. refresh는 RefreshSession의 userId로 DB User를 다시 읽어 상태 검사를 통과한 현재 유형을 사용한다. 사용자 JWT 생성 지점을 검색해 공통 발급기와 7개 호출 경로를 확인했다.
- 유지한 계약: RS256, kid, JWKS, sub·iss·aud·iat·exp·jti·scope, 공개 URL·Request/Response, RefreshSession·Refresh Token 흐름과 profile `accountType` 이름을 유지했다. UserAccountType의 기존 Mongo provider 호환 해석과 WITHDRAWN·MERGED 발급 차단도 유지했다. workload JWT에 account_type을 추가하지 않았으며 Identity 전체 API에 새 claim 필수 validator를 추가하지 않았다.
- 테스트: MEMBER/GUEST 문자열 claim과 기존 claim 집합, null 유형 거절, 각 발급 경로의 정확한 유형 전달, upgrade→refresh 동일 userId·MEMBER, DB 현재 유형·legacy 문서 refresh, merge target MEMBER, MERGED·WITHDRAWN 발급 차단, 구형 claim 없는 Token의 Identity profile 접근, profile 필드명, workload 제외, 기존 응답·로그 비노출 회귀를 검증했다.
- 실행한 테스트와 결과: 첫 `./gradlew clean test`는 sandbox의 Gradle cache lock 접근 차단으로 실행되지 않아 승인된 권한으로 재실행했다. 최초 전체 실행은 640개 중 기존 User mock의 getAccountType 설정 누락 2건으로 실패했다. 해당 fixture 보완 후 `./gradlew clean test` 성공: 123개 suite, 640개 테스트, 실패·오류·건너뜀 0개. `git diff --check` 통과.
- 결정사항: 프론트나 기존 Token claim에서 유형을 복사하지 않는다. Challenge 인가와 활성화는 Learning Core 책임이다. 이번 변경은 Identity claim 발급 범위이며 audience·scope 확대는 후속 Billing 조회 작업으로 유지한다.
- 위험 요소·미확인: 운영 실제 최대 TTL, 사용자 JWT 검증 clock skew, 구버전 발급 instance 종료 UTC 시각은 확인되지 않았다. 저장소 기본 PT30M은 운영값 증빙이 아니며 배포 workflow는 기존 ECS task definition의 환경 설정을 재사용한다. 구형 Token에는 claim이 없으므로 Challenge 선활성화 시 기존 MEMBER도 거절될 수 있다.
- 배포 전 확인: 사용자의 commit·push·PR·merge 후 Identity를 먼저 배포하고, 모든 구버전 발급 instance 종료 시각 + 실제 구버전 최대 Access Token TTL + Learning Core 사용자 JWT clock skew 이후 Challenge를 활성화한다. 구버전 rollback·재기동 시 대기 경계를 다시 계산한다. 운영 인계 항목과 미확인 상태를 JWT 계약에 기록했다.
- 범위 검토: 예상 밖 변경 없음. 기존 미커밋 WORKLOG·CURRENT_STATE와 untracked 프론트 인증 가이드를 보존했다. Learning Core·Billing·배포 설정·외부 인프라를 변경하지 않았으며 commit·push·PR·merge·배포를 수행하지 않았다.
- Jira 댓글 초안(미등록): 사용자 Access Token 7개 발급 경로에 현재 UserAccountType 기반 account_type 추가. 공통 발급기·서비스·회귀 테스트·JWT 계약 변경. 전체 640개 테스트 통과. 운영 TTL·구버전 종료 시각·Learning Core skew 확인과 순차 배포가 남음.
- 다음 작업: 사용자 diff 검토·commit·PR·merge 후 운영 배포 증빙을 채워 Learning Core에 전달한다. 이후 무료 사용권 조회 작업을 재개한다.

## 2026-09-07 — account_type 변경 브랜치·커밋·PR 명령어 안내

<!-- codex-turn:01a07a7d-d04f-7973-a53e-221afbbf8e1d -->

- 날짜·브랜치: 2026-09-07, 현재 `develop`.
- 작업 목표: 사용자가 직접 account_type 변경을 브랜치·커밋·PR로 올릴 명령어를 제공한다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`.
- 구현 내용·결정사항: `codex/add-access-token-account-type` 브랜치 생성, 변경 소스·테스트·JWT 계약·작업 기록 staging, commit, origin push, develop 대상 PR 생성 순서로 안내한다. untracked 프론트 인증 가이드는 staging 대상에서 제외하며 작업 기록 파일에는 앞선 미커밋 기록도 함께 포함됨을 알린다.
- 실행한 테스트와 결과: git status·현재 브랜치·remote를 읽기 전용 확인했다. 코드 변경이 없어 Gradle은 재실행하지 않았고 직전 640개 테스트 통과 결과를 사용한다. `git diff --check`로 문서를 검증한다.
- 유지한 계약: 사용자 직접 commit·push 원칙, 기존 코드·JWT·API 계약을 유지한다. Git 변경·PR 생성·Jira 작업은 실행하지 않았다.
- 위험 요소: 실제 PR·merge·배포와 운영 TTL·구버전 instance 종료 시각은 아직 없다. 사용자는 commit 전에 staged diff 범위를 확인해야 한다.
- 다음 작업: 사용자가 명령어를 실행해 PR을 생성하고 검토·병합한다.

## 2026-09-07 — 다음 작업 무료 사용권 조회 설명

<!-- codex-turn:01a07a80-d818-74a2-852f-820969992e19 -->

- 날짜·브랜치: 2026-09-07, Identity `develop@fe9c7f6`.
- 작업 목표: account_type 구현 이후 무료 사용권 조회 작업의 책임 서비스·사용자 동작·구현 순서를 설명한다.
- 확인 결과: 로컬 Git에서 PR #39 merge commit `fe9c7f6`과 account_type 발급 코드 반영을 확인했다. 실제 배포는 확인하지 않았다. Billing 현재 소스에는 public entitlement 조회 Controller가 없고 internal API만 있다.
- 변경 파일: `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`. Billing은 읽기 전용 조사했다.
- 구현 내용·결정사항: 다음 작업은 Billing public entitlement reader 계획이다. 제안 경로는 `GET /api/v1/entitlements`; 사용자 JWT sub 기준 benefit별 사용 가능 수량과 진행·재응시 상태를 반환한다. exact DTO와 상태는 계획서에서 확정한다.
- 근거: Billing `EntitlementGrant`는 available·held·consumed units를 소유한다. `PLAN-002-free-exam-initial-reserve.md`는 최초 reserve에서 FREE_EXAM_ONCE grant를 lazy 생성하고 OPEN·RETAKE_AVAILABLE 재시작에는 추가 차감하지 않는 계약이다. 따라서 grant 부재를 곧바로 0개로 해석하지 않고 eligibility와 현재 권리·AttemptGroup projection을 함께 고려해야 한다.
- 유지한 계약: 조회는 Claim·Grant 생성이나 소비를 일으키지 않는다. 무료권 수량과 기존 group 재응시 가능성을 구분하며 서로 다른 unitType을 한 수량으로 합치지 않는다. Identity는 사용자 JWT, Billing은 혜택·권리, Learning Core는 시험을 소유한다.
- 권장 순서: Billing reader·사용자 JWT verifier 구현 → Identity audience에 tosunsaeng-billing 및 billing:read 추가 → 프론트 조회 화면 → Firebase Stage 8 복귀. 결제 구매 구현을 이번 조회 범위에 포함하지 않는다.
- 테스트: 코드 변경 없이 저장소·문서를 확인했으므로 Gradle 미실행. `git diff --check`로 문서 검증. 직전 account_type 구현의 전체 640개 통과 결과를 재실행한 것으로 보고하지 않는다.
- 위험 요소: eligibility 연동 지연·진행 중 hold·재응시·사용 완료·저장소 장애를 모두 0개로 표시하면 안 된다. Billing paid entitlement reader 계약과 무료 조회 응답 관계는 계획 시 대조한다. account_type 배포·구버전 종료 시각·운영 TTL 확인은 별도로 남아 있다.
- 다음 작업: Billing 저장소에 무료 사용권 조회 계획서를 작성해 응답·상태 계산·인증·오류·테스트를 확정한다. Jira 작업과 Git mutation은 수행하지 않았다.

## 2026-09-07 — Billing 무료 사용권 조회 인계 내용 정리

<!-- codex-turn:01a07a80-d818-74a2-852f-820969992e19 -->

- 날짜·브랜치: 2026-09-07, Identity develop.
- 작업 목표: Billing 작업에서 계획서를 작성할 수 있도록 사용자 무료 사용권 조회 범위·인증·조회 의미·재가입 정책·테스트·배포 순서를 전달용으로 정리한다.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 인계 본문은 사용자에게 제공하며 Billing 파일·외부 메시지·Jira는 변경하지 않는다.
- 결정사항: GET /api/v1/entitlements는 제안 경로다. benefit별 신규 사용 가능 수량과 기존 group 재응시 가능 여부를 분리한다. 정확한 DTO·state·wrapper는 Billing paid reader 계약과 대조해 계획서에서 확정한다.
- 구현 요구: 현재 JWT sub의 eligibility·retained Claim·Grant·hold·AttemptGroup projection과 owner 전환 상태를 읽되 조회가 Grant 생성·consume·owner 변경을 일으키지 않는다. verified eligibility만으로 이미 사용한 혜택을 새 권리로 오판하지 않는다.
- 인증: 사용자 JWT의 Billing audience·billing:read를 검증하고 Guest와 MEMBER 모두 조회를 허용하는 방향이다. Identity의 account_type은 PR #39에 병합됐으나 Billing audience·scope 발급은 아직 후속 작업이다. 기존 internal SigV4 경계는 유지한다.
- 테스트: 문서 작업이므로 Gradle 미실행. git diff --check 검증. 전체 발급 경로 테스트 결과를 이번 작업의 신규 실행으로 보고하지 않는다.
- 유지한 계약·위험: 재가입은 이전 시험 기록을 연결하지 않고 승인된 미완료 group의 재응시 가능성만 표현한다. 여러 benefit·group을 단일 총수량으로 합치지 않는다. 이벤트 지연·장애를 0개로 숨기지 않으며 조회 결과는 reserve 성공 보장이 아니다.
- 다음 작업: Billing에서 조회 계획서·응답 예시·상태 판정표·인증과 테스트를 정리하고 Identity에 필요한 발급 변경을 회신한다.

## 2026-09-07 — Billing 조회 인계 작업 종료 기록

<!-- codex-turn:01a07a83-7bbc-7603-87f1-c81a4a7f7f3d -->

- 날짜·브랜치: 2026-09-07, Identity develop.
- 작업 목표: 사용자에게 전달한 Billing 무료 사용권 조회 인계를 현재 turn 식별자로 기록한다.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 과거 WORKLOG는 수정하지 않았다.
- 구현 내용·결정사항: 제안 public 조회 API, 사용자 JWT 인증, lazy grant·retained 혜택 판정, 신규 수량과 재응시 구분, 재가입 owner 이전, 오류·테스트·배포 조건을 전달했다. exact DTO·state·paid reader 관계는 Billing 계획서에서 확정한다.
- 유지한 계약: Identity 사용자 JWT와 Billing internal SigV4 경계, 조회의 무변경성, 과거 시험 기록의 재가입 연결 금지, 혜택 중복 지급 금지를 유지한다.
- 테스트: 문서만 변경해 Gradle 미실행. git diff --check로 검증한다.
- 위험 요소: Billing audience·billing:read 발급과 실제 운영 배포 검증은 후속 작업이다. 사용 가능 조회를 reserve 성공 보장으로 해석하지 않는다.
- 다음 작업: 사용자가 Billing 작업에 인계하고 Billing에서 계획서와 Identity 후속 요구를 정리한다. 외부 메시지·Jira·Git commit·push는 수행하지 않았다.

## 2026-09-07 — Billing PLAN-007 계약 검토

<!-- codex-turn:01a07a94-8be6-7743-9f9c-a464ae4f57a6 -->

- 날짜·브랜치: 2026-09-07, Identity develop.
- 작업 목표: 사용자 첨부 PLAN-007을 기존 무료 조회 인계와 Identity·Billing 현재 코드에 대조해 검토한다.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 애플리케이션·첨부 계획서·Billing 파일·Jira·배포는 변경하지 않았다.
- 확인한 적합성: 신규 INITIAL 수량과 replacement 분리, Grant lazy 생성의 read-only 추론, current retained Claim 중복 사용 방지, LOCAL_PROJECTION 한계, null/PENDING과 503 구분, 재가입 source 기록 비노출, public 사용자 JWT/internal SigV4 격리와 reader-first 배포는 기존 요청과 일치한다.
- Identity 인계: 9.2의 기존 Learning Core audience 보존 + Billing audience 배열 추가, 기존 scope 보존 + Guest/MEMBER billing:read 추가는 타당하다. 현재 JwtProperties.audience는 String이며 JwtAccessTokenIssuer는 List.of로 한 audience만 발급한다. defaultScopes는 explicit scopes가 비어 있을 때만 적용하므로 모든 사용자 발급 경로와 명시 scope에서도 read 부여를 검증해야 한다. workload·billing:purchase는 변경 대상에서 제외한다.
- 주요 보완 요구: 8.2는 command evidence 만료 시 target Session 귀속을 PENDING으로 처리하지만 그 자료가 정상 retention으로 사라지면 재조회만으로 회복되지 않는다. Billing ReservationProperties의 terminalCommandRetention 기본값은 7일이고 ReservationLifecycleService confirm이 command purgeAt을 설정하며 TTL index가 있다. AttemptSession과 Reservation에는 userId 필드가 없다. 따라서 command가 유일한 귀속 근거가 되지 않도록 활성 group/Session 수명 동안 유지되는 정확한 owner 전이·Reservation 연결 증빙을 먼저 확정해야 한다.
- 권고: 기존 지속성 있는 continuationId/owner transition/session 연결로 검증 가능한 경우와 불가능한 경우를 구분하고, 필요하면 최소 귀속 증빙을 write 시점에 남기는 별도 변경 범위를 명시한다. 조회에서 보정 write나 시각 기반 owner 추정은 하지 않는다. target confirm 후 command TTL 삭제·다중 재가입·source Session 잔존 테스트를 추가한다. 이는 코드에서 이미 발생했다고 확정한 장애가 아니라 계획의 장기 가용성 위험이다.
- 추가 UX 검토: 자격 projection 없는 Guest의 PENDING은 정상 미인증과 연동 지연을 구분할 수 없으므로 이를 실제 처리 중·곧 완료로 안내하지 않는다. 문서의 LOCAL_PROJECTION 한계와 프론트 제한 polling을 유지한다.
- 테스트: 읽기 전용 검토와 기록만 수행해 Gradle 미실행. git diff --check로 문서 변경 검증.
- 유지 계약·결정사항: 발급기와 Billing reader 구현 승인을 임의로 추정하지 않고 검토 결과를 전달한다. 기존 문서 변경·과거 WORKLOG는 보존한다.
- 다음 작업: Billing 계획서의 8.2 장기 귀속 증빙과 T08 retention 경계 테스트를 보완한 뒤 구현 기준을 확정한다. Identity에는 별도 audience/read 발급 변경이 남는다.

## 2026-09-07 — Billing PLAN-007 구현 범위 사용자 설명

<!-- codex-turn:01a07abb-e96c-7383-a375-4c29f9271431 -->

- 날짜·브랜치: 2026-09-07, Identity develop.
- 작업 목표: Billing에서 구현하려는 무료 사용권 조회를 사용자 화면과 서버 동작 관점에서 설명한다.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md.
- 설명 내용: PLAN-007 기준 GET /api/v1/entitlements가 로그인 사용자 본인의 신규 무료 수량·진행·재응시·사용 완료·확인 불명 상태를 반환한다. Billing의 자격·사용권·예약·그룹 상태를 조회하며 앱은 수량과 action을 함께 표시한다.
- 유지한 계약: 조회는 사용권 생성·차감·owner 이전을 수행하지 않는다. 실제 시험 시작은 Learning Core와 기존 Billing reserve 흐름이 최종 판단한다. 재가입 시 기존 기록을 복원하지 않으며 승인된 미완료 그룹 재응시 가능성만 제공한다. 결제 구현은 범위 밖이다.
- 결정사항: Identity에는 기존 사용자 JWT audience를 유지하면서 Billing audience와 billing:read를 추가하는 별도 후속이 필요하다. account_type 추가만으로 Billing 연동이 완료된 것은 아니다.
- 테스트: 설명과 문서 기록만 수행해 Gradle 미실행. git diff --check 검증.
- 위험 요소: 이 설명은 제공된 계획 기준이며 Billing의 실시간 구현 완료·배포 상태를 확인한 보고가 아니다. 이전 검토에서 지적한 세션 귀속 증빙의 retention 문제는 보완 확인이 필요하다.
- 다음 작업: Billing 조회 구현·검증 결과와 확정 인증 계약을 전달받아 Identity JWT 후속 변경을 진행한다.

## 2026-09-07 — 중단 후 재응시의 사용자 표시 수량 검토

<!-- codex-turn:01a07ac6-3f29-7141-8243-c974feefea13 -->

- 날짜·브랜치: 2026-09-07, Identity develop.
- 작업 목표: 중단 후 재응시 가능 상태도 사용자에게 1회 남음으로 표시하자는 의견을 검토한다.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. Billing 계획서·코드·외부 시스템은 변경하지 않았다.
- 결정사항·권고: 사용자 화면에는 신규 INITIAL 수량보다 실제 이용 가능한 기회를 표시한다. 미사용과 중단 후 재응시 가능은 모두 무료 모의고사 1회 이용 가능으로 표현하고, 후자는 처음부터 다시 응시한다는 설명을 함께 제공한다. 완료하면 0회다.
- 유지 계약: 동일한 미완료 group의 반복 중단을 새 권리로 누적하지 않는다. 재응시는 기존 consumption을 재사용하며 Grant available 증가·새 지급·원장 복원 없이 표시한다. 진행·채점·예약·연동 대기는 실제 action 조건에 맞게 별도 상태로 표시한다.
- API 권고: 기존 PLAN-007 availableQuantity는 신규 INITIAL 미예약 수량이므로 조용히 의미를 바꾸지 않는다. 사용자 표시용 이용 가능 횟수를 별도 정의하거나 구현 전 필드 의미를 명시적으로 개정하고 신규/재응시 내부 판정은 보존한다. 여러 benefit/group에서는 같은 기회를 중복 합산하지 않는다.
- 테스트: 설명·기록만 수행해 Gradle 미실행. git diff --check 검증.
- 위험 요소: retake ALLOWED라는 이유만으로 신규 Grant 수량에 1을 더하면 다중 group·동일 권리 중복 계산이나 실제 시작 불가 상태의 과장 표시가 생길 수 있다. exact DTO와 상태별 계산은 Billing 계획서 보정이 필요하다.
- 다음 작업: Billing에 표시용 횟수와 신규/재응시 판정의 분리, 미사용·중단·완료·대기 시나리오 반영을 전달한다.

## 2026-09-07 — Billing 무료 reader 구현 검증

<!-- codex-turn:01a07ace-3281-78c1-b731-14ed60bc16bc -->

- 날짜·브랜치: 2026-09-07, Identity develop. 검토 대상 Billing develop@eb0ae14, PR #9 merge와 구현 commit dadb83f를 로컬 Git에서 확인했다.
- 작업 목표: PLAN-007 reader·재응시 표시·재가입 귀속 증빙·인증·기존 차감 회귀를 확인한다.
- 변경 파일: Identity 및 Billing docs/codex/WORKLOG.md·CURRENT_STATE.md의 검토 기록. 애플리케이션·계약·테스트 소스 수정 없음.
- 확인 결과: GET public reader, lazy eligibility 추론, retained Claim 판정, 신규 수량/재응시 분리, snapshot 읽기·무변경, 사용자 JWT와 별도 public port/internal SigV4 경계가 구현됐다. 검토한 범위에서 추가 차단급 결함은 확인하지 못했다.
- 표시 계약: evaluator는 Grant.available을 availableQuantity로 반환하고 RETAKE_AVAILABLE은 quantity=0·retake=ALLOWED다. DTO에 별도 사용자 표시 횟수는 없다. Billing CURRENT_STATE에는 newAttempt/retake ALLOWED를 프론트에서 무료 1회 남음으로 통합하는 방향이 명시되어 있어 신규 수량 0 자체를 구현 오류로 분류하지 않는다. 실제 프론트 반영은 이 검토 대상이 아니며 미확인이다.
- 이전 보완 확인: sessionOwnerEpoch를 link/Reservation/AttemptSession에 저장하고 PHONE_REJOIN에서 증가, USER_MERGED에서 유지한다. sessionBindingVersion CAS로 reserve/confirm과 owner 전이를 직렬화한다. 실제 Mongo 테스트는 command 삭제·8일 경과·반복 재가입 후 귀속 및 기존 Claim/Grant/consumption 유지, 읽기 시 업무 write 부재와 snapshot 경합을 검증한다.
- 테스트: Billing ./gradlew clean test 성공. 35개 suite, 236개 테스트, 실패·오류·건너뜀 0개. sandbox Gradle cache 접근 차단 후 승인된 실행으로 검증했다. git diff --check 통과. 실제 외부 OAuth/AWS/운영 배포 테스트는 수행하지 않았다.
- 유지 계약: 원장/사용권을 표시 목적으로 복구하지 않고 반복 재응시·재가입도 기존 consumption을 재사용한다. Guest·Member 모두 조회 가능하되 검증된 sub·Billing audience·billing:read를 요구한다. workload 및 Identity 발급 코드는 변경하지 않는다.
- 남은 위험·배포 gate: reader·public connector는 기본 OFF. Identity Billing audience/read 발급, 실제 legacy attribution coverage·migration, ALB/SG·JWKS·staging E2E와 화면 통합이 남는다. API 정보로 표시를 만들 수 있다는 사실과 앱에 이미 1회로 보인다는 사실을 구분한다.
- 다음 작업: Identity audience/read 후속 구현과 프론트 표시 규칙을 확정하고 Billing 운영 활성화 조건을 검증한다. Jira·commit·push·배포는 수행하지 않았다.

## 2026-09-07 — 다음 작업 Identity Billing 조회 인증 설명

<!-- codex-turn:01a07ace-3281-78c1-b731-14ed60bc16bc -->

- 날짜·브랜치: 2026-09-07, Identity develop.
- 작업 목표: Billing reader 검증 이후 Identity에서 수행할 JWT audience/read 권한 추가 범위를 설명한다.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 애플리케이션 구현·Jira·Git mutation 없음.
- 확인 결과: JwtProperties는 단일 String audience이고 JwtAccessTokenIssuer는 List.of(properties.audience())를 발급한다. defaultScopes는 explicit scopes가 비어 있을 때만 적용한다.
- 권장 변경: 기존 Learning Core audience와 scope를 보존하면서 사용자 JWT에 tosunsaeng-billing audience와 billing:read를 추가한다. Guest/MEMBER 전체 사용자 발급·refresh·upgrade·merge 경로에서 적용하고 workload JWT는 제외한다.
- 유지 계약: account_type, sub, issuer, RS256/kid/JWKS, API URL·응답·RefreshSession을 유지한다. billing:purchase·전화번호·수량 claim은 추가하지 않는다. 기존 토큰의 Identity/LC 검증도 유지한다.
- 배포 의미: 기존 토큰은 자동 갱신되지 않으므로 정상 refresh/재로그인으로 새 토큰을 받는다. Billing의 aud/scope 검증을 완화하지 않고 reader OFF 선배포 후 Identity 발급 배포·staging 검증·reader/프론트 활성화 순서를 따른다.
- 테스트: 코드 변경 없는 설명 작업이므로 Gradle 미실행. git diff --check 검증.
- 위험 요소·다음 작업: 기본 scope 설정만 바꾸면 explicit scope 경로가 누락될 수 있다. 기존 설정 호환·두 서비스 검증·전체 발급 경로 테스트를 계획서에서 확정한 뒤 구현한다.

## 2026-09-07 — Identity Billing 인증 후속 설명 종료 기록

<!-- codex-turn:01a07ad1-c40f-7e80-a74c-12ba45beed5e -->

- 날짜·브랜치: 2026-09-07, Identity develop.
- 작업 목표: 다음 작업인 사용자 JWT Billing audience·read 권한 추가 설명을 현재 turn 식별자로 기록한다.
- 변경 파일: docs/codex/WORKLOG.md, docs/codex/CURRENT_STATE.md. 과거 WORKLOG는 수정하지 않았다.
- 설명·결정사항: 기존 Learning Core audience와 scope에 tosunsaeng-billing·billing:read를 추가하고 Guest/MEMBER 전체 사용자 발급·재발급 경로를 검증하는 작업을 안내했다. 구현은 아직 시작하지 않았다.
- 유지 계약: account_type·기존 API·RefreshSession·서명 구조를 유지하고 workload JWT와 구매 권한은 변경하지 않는다.
- 테스트: 문서 변경만 수행해 Gradle 미실행. git diff --check로 검증한다.
- 위험·다음 작업: 기존 토큰은 정상 refresh/재로그인으로 교체해야 한다. 계획서 작성 후 Identity 구현·배포와 Billing staging 검증을 진행한다. 외부 전송·Jira·Git mutation은 수행하지 않았다.

## 2026-09-07 — Billing public reader용 사용자 JWT 구현 계획서 작성

<!-- codex-turn:01a07adc-10ec-7eb0-8825-608ff7614527 -->

- 날짜·브랜치: 2026-09-07, Identity develop@fe9c7f6.
- 작업 목표: 사용자 Access Token의 Billing audience·billing:read 확장을 구체적인 구현·테스트·배포 계획으로 저장한다.
- 변경 파일: docs/contracts/billing-public-reader-user-jwt-plan.md 신규, docs/codex/WORKLOG.md append, docs/codex/CURRENT_STATE.md 갱신. 기존 미커밋 문서와 프론트 가이드는 보존했다.
- 구현 내용: 5줄 결론·필독 사항·결정사항·위험·상세 구현·부록 계층으로 계획서를 작성했다. 7개 사용자 발급 경로, 변경 파일, 13개 테스트 항목, 완료 기준과 운영 인계 항목을 포함한다.
- 결정사항: 기존 JwtProperties.audience·JWT_AUDIENCE와 Identity 자체 required audience 검증을 유지한다. JwtAccessTokenIssuer의 사용자 발급 시 primary + 고정 Billing audience를 순서 고정·중복 제거하고 기존 base scope 선택 이후 billing:read를 합성한다. explicit scope에 default learning scope를 추가하지 않는다.
- 범위: 기존 API·account_type·RefreshSession·서명·TTL·구형 토큰 검증을 유지한다. workload JWT·billing:purchase 자동 부여·다른 저장소 코드·별도 발급 API·새 운영 dependency는 제외한다. Identity 신규 flag 없이 코드 배포 시 일관 발급하고 Billing 기존 reader flag로 노출을 제어한다.
- 검증: 계획서·코드 계약 대조와 로컬 링크 존재 확인, git diff --check. 문서 작업이므로 Gradle 테스트 미실행. 과거 Identity 640개·Billing 236개 통과를 이번 실행 결과로 보고하지 않는다.
- 위험 요소: 구버전 발급 instance 혼재, 기존 토큰 갱신 필요, Billing audience/read 누락의 무한 refresh 방지, 실제 운영 설정·키·staging·legacy coverage 미확인 상태를 명시했다.
- 예상 밖 변경: 없음. 애플리케이션·테스트·런타임 설정·Billing·Learning Core 파일을 변경하지 않았다. Jira·commit·push·배포도 수행하지 않았다.
- 다음 작업: 계획서를 기준으로 구현하고 전체 회귀 검증 후 사용자 PR·merge·Identity 배포와 Billing staging 연동을 진행한다. Jira가 배정되면 구현 전에 해당 범위를 확인한다.

## 2026-09-07 — Billing 사용자 JWT 확장 계획 쉬운 설명

<!-- codex-turn:01a07ae9-47f1-7dc3-aa94-78ed6dcbc776 -->

- 날짜·브랜치: 2026-09-07, Identity develop.
- 작업 목표: 작성된 계획서의 목적·변경 범위·사용자 흐름과 배포 조건을 쉬운 용어로 설명한다.
- 변경 파일: docs/codex/WORKLOG.md append, docs/codex/CURRENT_STATE.md 갱신. 계획서·애플리케이션·테스트는 변경하지 않았다.
- 설명 내용: 기존 사용자 인증에 Billing audience와 billing:read를 추가하여 앱이 본인 사용권 조회 API를 호출하게 한다. 7개 발급 경로는 공통 발급 정책을 사용하고 Guest/MEMBER 모두 조회하되 무료 사용 자격·수량은 Billing이 판단한다.
- 유지한 계약: 기존 로그인·재발급 API, Learning Core 접근, account_type, 서명·TTL·RefreshSession을 유지하며 workload JWT와 구매 권한은 제외한다.
- 결정사항: 별도 로그인·발급 API 없이 기존 인증을 확장한다. 재응시 1회 표시는 프론트의 상태 해석 작업이고 이번 Identity 구현은 수량을 변경하거나 무료권을 지급하지 않는다.
- 실행한 테스트와 결과: 문서 확인과 설명 작업으로 Gradle 미실행. git diff --check로 문서 공백 오류를 검증한다.
- 위험 요소: 기존 발급분은 자동 변경되지 않으며 정상 refresh/재로그인이 필요하다. 구버전 발급 instance 종료·staging 검증·Billing 운영 준비는 여전히 미확인이다.
- 예상 밖 변경: 없음. 기존 미커밋 문서를 보존했고 외부 시스템·Jira·commit·push·배포는 수행하지 않았다.
- 다음 작업: 사용자 구현 요청 후 계획서 기준 공통 발급기·테스트·계약을 수정하고 전체 테스트를 실행한다.


## 2026-09-08 다우기술 노션 포트폴리오 토선생 소재 선별

- 브랜치: develop. 신규 Jira 없음.
- 목표: 사용자 요청에 따라 토선생 상세 페이지에 넣을 내용을 선별.
- 조사: 기존 포트폴리오 소재·트러블슈팅 문서, 각 서비스 현재 상태와 Learning Core 복구·Saga 테스트 및 MDC 구현을 대조.
- 결정: 빈 종합 피드백과 선택적 채점 복구, 시험 생성·사용권 Reservation Saga를 대표 사례로 추천하고 구조화 로그를 보조 사례로 제안. 인증·저장 모델·S3는 구조 설명에 배치.
- 구분: 구현 및 과거 테스트 기록은 운영 활성화 증거와 다르며 실제 결제·환불은 현재 구현 성과로 사용하지 않음. 처리량·비용·장애 감소 수치 미측정.
- 변경 파일: 이 저장소의 docs/codex/WORKLOG.md와 CURRENT_STATE.md에 조사 기록만 추가. 기존 미커밋 작업 보존.
- 검증: 소스·테스트 정적 조회. 애플리케이션 변경이 없어 Gradle 테스트는 재실행하지 않음.
- 유지: API·AI 계약·feature flag·코드·외부 서비스·Git 이력 변경 없음.
- 다음: 본인 역할과 사례별 설명 가능 범위 확인 후 노션 본문 작성.

## 2026-09-08 — Billing 사용자 JWT 확장 Jira 생성안 준비

<!-- codex-turn:01a07ebe-2af5-7883-8912-ebf127806fc6 -->

- 날짜·브랜치: 2026-09-08, Identity develop.
- 작업 목표: 사용자 요청에 따라 Billing public reader 사용자 JWT 계획을 Jira 생성안으로 정리한다.
- 변경 파일: docs/codex/WORKLOG.md append, docs/codex/CURRENT_STATE.md 갱신. 기존 미커밋 변경은 보존했다.
- 구현 내용: 코드 구현 없음. 제목과 범위·완료 조건·제외 사항을 사용자에게 제시할 생성안을 준비했고 공식 Atlassian 도구 가용성을 확인했다.
- 제안 제목: [Identity] Billing 사용권 조회용 사용자 JWT audience·scope 확장.
- 유지 계약·결정사항: 기존 Learning Core audience와 scope에 고정 Billing audience·billing:read를 추가한다. 7개 발급 경로, explicit/default scope 호환, account_type·API·RefreshSession 유지, workload 및 구매 권한 제외를 완료 기준에 포함한다.
- Jira 작업·승인 여부: 생성안 사전 제시 후 승인 대기. 이슈 키 미발급, 실제 Jira 생성·수정·댓글·상태 변경 없음.
- 테스트: 문서 작업으로 Gradle 미실행. git diff --check 검증.
- 위험 요소: 실제 구현·배포·staging 연동은 아직 수행하지 않았으며 기존 사용자는 정상 재발급이 필요하다.
- 다음 작업: 사용자 승인 후 대상 프로젝트·이슈 유형·중복 이슈를 조회하고 승인된 내용으로 생성한 뒤 키를 계획서와 작업 기록에 반영한다.

## 2026-09-08 — TMI-127 Billing 사용자 JWT 확장 Jira 생성

<!-- codex-turn:01a07ec1-a96d-7612-b0d0-19321b0d89a7 -->

- 날짜·브랜치: 2026-09-08, Identity develop. 브랜치 생성·변경 없음.
- Jira: TMI-127
- 작업 목표: 승인된 Billing 사용자 JWT 확장 작업을 Jira에 등록하고 계획서와 연결한다.
- 변경 파일: docs/contracts/billing-public-reader-user-jwt-plan.md, docs/codex/CURRENT_STATE.md, docs/codex/WORKLOG.md. 기존 미커밋 문서 및 프론트 가이드는 보존했다.
- 구현 내용: 애플리케이션 변경 없음. 공식 Atlassian MCP에서 TMI 프로젝트·작업 유형 및 관련 중복 검색을 확인한 뒤 승인된 제목·범위·완료 조건·배포 인계·제외 사항으로 이슈를 생성했다. 생성 응답에서 키·본문·초기 상태를 확인했다.
- Jira 작업·승인 여부: 사용자의 '어 해줘' 승인 후 생성. 초기 상태 `해야 할 일`, 담당자 미지정. 별도 상태 전환·수정·댓글은 없으며 댓글 목적도 해당 없음.
- 유지한 계약·결정사항: 기존 사용자 JWT의 Learning Core 대상·권한을 보존하고 Billing audience·billing:read를 추가하는 범위다. workload·구매 권한·무료권 계산·타 저장소 구현·실제 운영 활성화는 제외한다.
- 테스트와 결과: Jira 생성 응답 확인, git diff --check 검증. 코드 변경 없는 이슈·문서 작업이므로 Gradle 테스트 미실행.
- 위험 요소: 구현·전체 회귀·배포 및 staging 연동은 미수행이다. 이슈 생성은 개발 완료나 운영 준비 완료를 의미하지 않는다.
- 예상 밖 변경: 없음. Git commit·push, 외부 배포 및 다른 이슈 변경을 수행하지 않았다.
- 다음 작업: 사용자 구현 요청 시 TMI-127을 다시 조회하고 작업 브랜치와 계획서 기준으로 공통 발급기·테스트·계약을 수정한다. 완료 댓글은 구현·검증 후 초안을 제시하고 별도 승인 없이 등록하지 않는다.

## 2026-09-08 — TMI-127 Billing public reader 사용자 JWT 확장 구현

<!-- codex-turn:01a07ec5-cbb8-7030-a5a9-f2d4e3aba272 -->

- 날짜·브랜치: 2026-09-08, feat/TMI-127-billing-reader-jwt. 기존 사용자 브랜치를 유지했다.
- Jira: TMI-127
- 작업 목표: 승인된 이슈와 계획서에 따라 모든 사용자 Access Token에 Billing audience·조회 scope를 추가한다.
- 변경 파일: JwtAccessTokenIssuer.java; JwtAccessTokenIssuerTests·JwtDecoderTests·JwtWorkloadIdentityCredentialProviderTests·SecurityIntegrationTests; AuthenticationUseCaseServicesTests·GuestAuthServiceTests·FirebaseSignupServiceTests·FirebaseExchangeServiceTests·FirebaseGuestUpgradeServiceTests·FirebaseGuestMergeServiceTests·GuestRefreshLifecycleTests; 신규 support/SignedUserTokenFixture.java; docs/contracts/identity-learning-jwt.md·billing-public-reader-user-jwt-plan.md 및 WORKLOG·CURRENT_STATE.
- 구현 내용: primary audience 이후 고정 Billing audience를 LinkedHashSet/List.copyOf로 중복 제거하고, 기존 explicit/default scope 선택 이후 TreeSet에 billing:read를 추가한다. 호출자 입력과 설정 집합을 수정하지 않는다. 실제 런타임 변경은 공통 issuer 한 파일이며 서비스와 검증·설정 클래스는 변경하지 않았다.
- 테스트 보강: 7개 서비스의 응답을 실제 RSA 발급·검증 fixture로 확인했다. upgrade→refresh는 같은 userId/MEMBER, merge→refresh는 target/MEMBER를 유지한다. GUEST·legacy DB 유형, default/explicit·중복·malformed scope, 동시 발급 20건·설정 불변성, 구형 토큰 Identity HTTP 호환과 workload 두 purpose의 교차 사용 거절을 검증했다.
- 실행한 테스트와 결과: ./gradlew clean test 성공, 123개 suite·648개 테스트, 실패·오류·건너뜀 0개. sandbox Gradle 캐시 잠금 파일 접근 거절 후 승인된 실행으로 검증했고 서비스 검증 보강 후 전체 테스트를 다시 실행했다. git diff --check 통과. 테스트 수치는 이번 실제 XML 결과에서 집계했다.
- 유지 계약: 기존 Learning Core audience·권한, account_type·sub·issuer·RS256·kid·JWKS·TTL, 공개 API·응답·RefreshSession·탈퇴/병합 source 차단을 유지한다. workload JWT·billing:purchase·혜택 수량·전화번호 claim과 타 저장소는 제외했다.
- 결정사항: 새 endpoint·dependency·필수 환경변수·Identity feature flag는 추가하지 않는다. 기존 JWT_AUDIENCE 단일 문자열과 자체 required audience 검증을 유지하며 Billing 기존 flag와 정상 refresh/재로그인으로 전환한다. JWT 계약의 오래된 단일 공개키 설명은 기존 rotation 코드 사실에 맞춰 정정했다.
- 위험·미확인: Billing HTTP/실제 Learning Core 소비자 E2E, 외부 JWKS·키 회전·운영 TTL·구버전 instance 종료 시각·Billing migration/ingress와 프론트 화면은 미검증이다. 로컬 audience/scope·workload fixture가 배포 검증을 대체하지 않는다.
- 배포 전 확인: 사용자 PR·병합 후 Identity 전체 rollout, 새 토큰 발급과 세 서비스 staging 접근, 운영 설정·Billing 준비 및 기존 앱 정상 재발급을 확인한 뒤 reader·화면을 활성화한다. Challenge account_type TTL/skew gate는 별도다.
- 예상 밖 변경: 없음. 기존 미커밋 작업 기록·계획서·프론트 가이드를 보존했다. 프론트 가이드·Billing·Learning Core 파일은 수정하지 않았다.
- Jira 작업·승인: 구현 전에 공식 MCP로 이슈를 읽었으며 댓글·상태 변경은 수행하지 않았다. 상태는 해야 할 일 유지. 완료 댓글 초안은 계획서 부록 6.7에만 작성했다. Git commit·push·PR·배포 없음.
- 다음 작업: 사용자가 commit·push·PR을 진행한다. 병합·배포 증빙 확인과 별도 승인 후 Jira 완료 처리 및 Billing·프론트에 인증 전환을 인계한다.

## 2026-09-08 — TMI-127 구현 코드 설명

<!-- codex-turn:01a07ed2-995b-7a80-bf93-7b02957fd0ba -->

- 날짜·브랜치: 2026-09-08, feat/TMI-127-billing-reader-jwt.
- Jira: TMI-127
- 작업 목표: 사용자 요청에 따라 구현한 Billing 사용자 JWT 확장을 실제 코드와 예시로 설명한다.
- 변경 파일: docs/codex/WORKLOG.md append, docs/codex/CURRENT_STATE.md 갱신. 애플리케이션·테스트·계약 소스 추가 변경 없음.
- 설명 내용: 고정 Billing 상수, LinkedHashSet의 중복 제거·입력 순서와 List.copyOf 불변 목록, explicit/default scope 선택 후 TreeSet 합성, claim 구성 및 기존 RS256 서명, 7개 발급 경로 공통 적용을 안내한다.
- 유지 계약·결정사항: audience는 사용 대상, scope는 조회 권한이며 실제 혜택 자격·횟수는 Billing이 판단한다. 현재 DB User 유형으로 refresh하고 구형 토큰은 기존 primary audience contains 검증을 유지한다. workload·구매 권한·API·RefreshSession 변경 없음.
- 테스트와 결과: 소스 및 실제 서명 테스트 fixture 정적 확인, git diff --check 검증. 설명 작업이므로 Gradle 미실행. 123개 suite·648개 통과는 직전 구현 작업 결과이며 이번 재실행 결과가 아니다.
- 위험 요소: 기존 발급분은 자동 변경되지 않아 정상 재발급이 필요하다. 실제 배포·Billing staging E2E·프론트 활성화는 여전히 미확인이다.
- 예상 밖 변경: 없음. 기존 미커밋 변경 보존. Jira 조회·댓글·상태 변경, commit·push·배포 없음.
- 다음 작업: 사용자 PR·병합과 배포 후 새 토큰을 통한 Billing 조회 연동을 검증한다.
