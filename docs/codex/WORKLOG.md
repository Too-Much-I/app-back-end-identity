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
