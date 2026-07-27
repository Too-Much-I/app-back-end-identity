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
