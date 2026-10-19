# Identity develop 운영 안정성 수정 계획

- 작성일: 2026-09-22
- 상태: 2026-09-22 코드 구현·로컬 검증 완료 / 실제 배포 검증 미수행
- Jira: [TMI-176](https://to-teacher.atlassian.net/browse/TMI-176) (상위 에픽 TMI-136, Jira 상태는 변경하지 않음)
- 기준: 로컬 develop `5f37c13563f4d777e9870e8668f13164ed85fe9c`
- 근거: [develop 코드 리뷰](../codex/develop-code-review-2026-09-22.md), [운영 로그 조사](../codex/identity-operability-review-2026-09-22.md)

## 1. 5줄 결론

현재 구현 결과는 부록 6.4를 참고한다. 아래 설계·승인 절차는 구현 당시의 계획이며 실제 배포·키 교체는 여전히 별도 승인 대상이다.

1. 공통 요청 로그 누락, JWT 키 회전 호환, DB 장애 진단, Sentry 배포 식별의 4건을 수정한다.
2. 우선 실제 ECS JSON 로그가 출력되도록 고치고, 이벤트 생성뿐 아니라 최종 JSON 출력까지 테스트한다.
3. Identity 검증기는 active 및 보존 공개키를 함께 신뢰하되 서명은 active private key로만 한다.
4. 사용자 API와 오류 응답은 유지하고, 내부 로그에만 제한된 장애 분류를 추가한다.
5. develop 기반 구현·검증과 실제 배포·키 교체는 분리한다. 기존 서비스 자동 배포나 기능 활성화는 하지 않는다.

## 2. 사용자가 반드시 읽어야 하는 내용

### 2.1 수정 결과

| 항목 | 현재 문제 | 수정 후 |
|---|---|---|
| R1 / P1 로그 | requestId 중복으로 JSON 변환 실패 | 성공·거절·서버 오류 공통 로그가 정상 출력 |
| R2 / P1 키 회전 | JWKS에는 구키가 있어도 자체 검증기는 신키만 신뢰 | 보존 기간 동안 기존 사용자 토큰도 Identity API에서 검증 |
| R3 / P2 DB 장애 | DB 실패 원인을 버리고 동일 503 반환 | 응답은 동일, 내부에서는 실패 종류·발생 작업 식별 |
| R4 / P2 release | CI의 배포 식별자가 컨테이너에 전달되지 않음 | 실제 실행 이미지와 Sentry release가 같은 commit을 가리킴 |

### 2.2 바꾸지 않는 것

- 로그인·가입·재발급·프로필 등의 URL, request/response, HTTP status, 공개 오류 code/message.
- 사용자 JWT의 RS256, typ=JWT, sub/iss/aud/iat/exp/jti/scope/account_type 및 발급 TTL.
- Identity 현재 timestamp 검증 허용 오차(0), issuer/audience 검증. Learning Core의 별도 skew는 변경하지 않는다.
- workload JWT issuer/audience 및 사용자 JWT와의 용도 분리.
- RefreshSession rotation·재사용 탐지·복구 기간·Idempotency-Key·sessionEpoch·transaction/rollback 의미.
- SNS 연결·해제, 탈퇴·재가입, 무료 사용권 정책과 모든 기능 flag 기본값.
- DB 스키마·보존 기간·인덱스. 이번 변경은 데이터 migration을 요구하지 않는 방향으로 구현한다.
- 실제 RSA 키/Secret 값, AWS/GCP 권한, 운영 도메인 및 실행 중 서비스.

### 2.3 작업 기준과 순서

1. 구현 직전 develop HEAD와 관련 diff를 재확인한다. 원격 최신 여부는 별도 확인하고 사용자 변경을 덮어쓰지 않는다.
2. R1 구현 및 콘솔 회귀 테스트.
3. R2 구현 및 JWT/실제 HTTP 인증 회귀 테스트.
4. R3 구현 및 오류 분류·민감값 비노출 테스트.
5. R4 기존 배포 정의의 runtime mapping 보완 및 정적 검증.
6. 전체 테스트/최종 diff 검토 → 사용자 commit·push·PR → 별도 테스트 배포 확인.

현재 미커밋 Docker bootstrap·Swagger·SNS 폐기 API 정리 변경은 이번 작업에 임의로 합치거나 되돌리지 않는다. 겹치는 파일은 필요한 최소 hunk만 변경하고 완료 보고에서 구분한다. 브랜치/커밋/PR 생성 자체는 이 문서 작성 범위가 아니다.

## 3. 사용자가 결정해야 하는 사항

### 구현 승인 시 적용할 권장안

- R1~R4를 같은 수정 계획으로 진행하되 항목별로 테스트와 diff를 분리한다.
- R3의 전달 대상은 우선 기존 CloudWatch 콘솔 로그다. 기존 unexpected 500의 Sentry 전송은 유지하되 모든 4xx/503을 새 Sentry issue로 보내는 확장은 하지 않는다.
- R4는 **기존 workflow의 환경변수 전달만 보완**한다. 새 GitHub Actions 배포 workflow/새 배포 대상/트리거 확대는 추가하지 않는다. 기존 workflow 변경도 작업 규칙상 허용하지 않는 의도라면 R4 코드는 보류하고 사용자 직접 설정 인계로 대체한다.
- 실제 키 교체, 운영 배포, 경보 생성은 자동 수행하지 않는다.

### 배포 전에 확정할 것 — 구현을 막지 않음

- 배포 대상(별도 테스트 Identity 먼저 권장)과 불변 image revision.
- 운영에 적용된 최대 사용자/workload 토큰 수명, 소비자 skew와 JWKS 캐시 정책. 구키 제거 시점을 계산하는 입력이다.
- CloudWatch 검증 담당자 및 Sentry 테스트 이벤트 확인 담당자.

새로운 회원 정책이나 프론트 API 변경 선택은 필요 없다.

## 4. 주요 위험과 미확인 사항

- 순수 develop 896개 테스트 성공만으로 안전을 보장할 수 없다. R1/R2는 기존 테스트가 놓친 결함이다.
- 로그 수정 후 이제까지 빠졌던 이벤트가 유입돼 로그량과 비용이 증가한다. 정상 health/JWKS 등의 기존 quiet 정책은 유지하고 전체 DEBUG를 켜지 않는다.
- 로그나 Sentry에 Throwable 원문을 그대로 추가하면 DB URI·토큰 등 민감값이 노출될 수 있다. R3는 예외 원문 보관이 아니라 안전한 메타데이터 전달이다.
- 키 회전은 코드 수정만으로 무중단이 되지 않는다. 신키 사전 신뢰 배포·캐시 전파·구키 보존이 필요하다.
- 기존 workflow는 main에서 기존 `tosunsaeng-identity-service`로 배포한다. 테스트 서비스 배포용으로 그대로 실행하지 않는다.
- Session fence/recovery OFF의 기존 위험, Firebase 권한/실제 인증 E2E 및 Atlas rollback은 별도 배포 검증 대상이다. 이번에 flag를 바꾸어 해결했다고 간주하지 않는다.
- 이전 919개 테스트는 미커밋 포함 작업 트리 결과다. 구현 후 실제 통합 상태의 테스트 수를 새로 기록한다.

## 5. 현재 작업과 직접 관련된 설명

### 5.1 R1 — 공통 로그 출력 복구

근거: [RequestLoggingFilter](../../src/main/java/web/tosunsaeng/identity/global/observability/RequestLoggingFilter.java:68), [기존 로그 테스트](../../src/test/java/web/tosunsaeng/identity/global/observability/RequestLoggingFilterTests.java), [LogCapture](../../src/test/java/web/tosunsaeng/identity/support/LogCapture.java:20).

구현:

- requestId는 MDC를 유일한 출력 출처로 사용한다. INFO/ERROR builder에서 중복 addKeyValue를 제거한다.
- 응답 X-Request-ID, 안전한 ID 검증/재생성, 이전 MDC 복원은 유지한다.
- event/outcome/method/route/status/durationMs/errorCode 및 안전한 exception 정보의 기존 이름을 유지한다.
- 기존 이벤트 단위 테스트는 requestId의 MDC 값까지 검증하도록 보완한다. 모든 테스트에서 LogCapture를 제거하는 작업은 하지 않는다.
- 운영과 같은 ECS formatter 및 실제 console encoder/appender 경로를 검증하는 독립 테스트를 추가한다. 테스트가 global logging context를 오염시키지 않도록 격리하고 설정을 복구한다.

완료 조건:

- 성공 200, 인증 거절 401/403, business 409, server 500/503이 유효한 JSON으로 출력된다.
- requestId 중복 여부는 중복 필드를 덮어쓰는 기본 JSON parser만으로 판단하지 않는다. strict duplicate 검출과 Logback appender error 부재를 함께 검사한다.
- 공통 ERROR 이벤트는 요청당 한 건이며 quiet endpoint 성공은 생략되고 실패는 남는다.
- body/query/Authorization/Refresh Token/Idempotency-Key 원문/예외 message가 출력되지 않는다.

### 5.2 R2 — JWKS와 자체 검증기의 신뢰 키 목록 통일

근거: [JwtConfiguration](../../src/main/java/web/tosunsaeng/identity/global/security/jwt/JwtConfiguration.java:95), [JwksPublicKeySet](../../src/main/java/web/tosunsaeng/identity/global/security/jwt/JwksPublicKeySet.java), [JwksRotationProperties](../../src/main/java/web/tosunsaeng/identity/global/security/jwt/JwksRotationProperties.java).

구현:

- active public key와 설정된 retained public keys로 구성한 불변 로컬 신뢰 집합을 JWKS 응답과 decoder가 공통 사용한다.
- 기존 JWT_PREVIOUS_KEY_IDS/JWT_PREVIOUS_PUBLIC_KEY_LOCATIONS 설정과 validation을 유지한다. 의미상 retained 키 목록이며 신키 사전 게시에도 사용할 수 있음을 runbook에 설명한다.
- Nimbus key selector에서 token header kid와 RS256으로 정확한 로컬 공개키를 선택한다. 없는/공백/알 수 없는 kid는 거절한다.
- active kid 하나만 허용하는 validator를 로컬 신뢰 목록에 대한 검증으로 대체한다. 모든 키로 무차별 검증하거나 token의 jku/x5u를 따라 외부 키를 가져오지 않는다.
- 공개키 집합에는 private material이 없어야 한다. encoder의 active private JWK source는 분리 유지해 기존처럼 active key 하나로만 발급한다.
- issuer/audience/subject/expiration/type/timestamp 검증은 그대로 둔다. account_type 필수 검증을 Identity 전 API에 새로 추가하지 않는다.

완료 조건:

- active와 retained 키로 발급한 미만료 사용자 토큰이 decoder 및 보호 API에서 통과한다.
- unknown/누락 kid, 다른 키 서명에 정상 kid를 붙인 토큰, 제거한 구키, HS256/비허용 algorithm, 잘못된 typ/issuer/audience, 만료 토큰은 거절한다.
- workload token은 사용자 경로에서 기존 경계에 따라 거절한다.
- retained 키를 추가해도 신규 발급은 active kid/서명만 사용하고 JWKS에 private key가 노출되지 않는다.

배포 절차(코드 수정 후 별도 승인):

1. 구키로 계속 서명하면서 신키 공개키를 모든 검증기/JWKS에 먼저 추가한다.
2. Identity 전체 태스크 반영과 외부 verifier의 키/캐시 전파를 확인한다.
3. active 서명을 신키로 전환하되 신·구 공개키를 모두 유지한다.
4. 마지막 구키 발급 인스턴스 종료 시각부터 실제 최대 토큰 TTL + 관련 verifier skew 이상 보존하고 캐시 정책도 충족시킨다. 기본 30분을 하드코딩하지 않는다.
5. 구키 제거 후 구키 거절을 확인한다.

### 5.3 R3 — 공개 오류 계약을 유지한 내부 진단 보강

근거: [SessionSecurityService](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/SessionSecurityService.java:55), [AuthException](../../src/main/java/web/tosunsaeng/identity/domain/auth/common/exception/AuthException.java), [GlobalExceptionHandler](../../src/main/java/web/tosunsaeng/identity/global/exception/GlobalExceptionHandler.java), [RequestLogContext](../../src/main/java/web/tosunsaeng/identity/global/observability/RequestLogContext.java).

구현:

- 변환 시 선택적으로 안전한 진단 객체를 전달하는 경계를 추가한다. 기존 AuthException 생성자는 호환 유지한다. 원본 Throwable을 새 예외의 cause/suppressed에 그대로 연결하지 않는다.
- diagnostic 데이터는 객체 생성 즉시 허용된 정보만 추출한다. JSON 응답 result에는 넣지 않는다.
- 실패 분류 후보: DB_TIMEOUT, DB_UNAVAILABLE, DB_CONFLICT, TRANSACTION_FAILURE, DB_FAILURE, UNKNOWN. 정확한 매핑은 실제 exception hierarchy와 Mongo error label을 테스트로 고정하고 message 문자열로 분류하지 않는다.
- operation은 enum/고정 값(예: SESSION_TRANSACTION, SESSION_EPOCH_READ)만 허용한다. userId/phone/UID/DB query/URI 등 입력값은 포함하지 않는다.
- exceptionType 및 제한된 cause type/stack frame만 기존 sanitizer 정책에 맞게 허용한다. cause 순환/깊이/프레임 수를 제한하며 예외 message·stack local 변수는 제외한다.
- HTTP는 handler가 안전한 데이터를 RequestLogContext에 전달하고 공통 filter가 기존 503 이벤트에 합쳐 한 번 출력한다. service와 handler에서 같은 장애를 중복 ERROR로 찍지 않는다.
- 기존 `transactionKeepingUniqueConflicts`의 DuplicateKeyException 전파, 트랜잭션 rollback, 재시도/commit 결과 처리 순서를 바꾸지 않는다.
- 동일 SessionSecurityService 예외를 삼키는 기존 session/provider scheduler·worker catch에서는 안전한 분류를 기존 경고에 첨부하는 범위까지만 보완한다. 재시도 정책 변경·새 worker/경보 시스템 추가는 제외한다.
- Sentry의 기존 unexpected 500 전송·정제 정책은 유지한다. business 503의 자동 Sentry 전송 확대는 이번 범위에서 제외한다.

완료 조건:

- mocked DB timeout/연결 실패/optimistic conflict/transaction failure 각각의 내부 분류와 동일한 외부 503을 확인한다.
- 기존 의도된 DuplicateKeyException 전파 및 business 4xx 처리가 변하지 않는다.
- 가짜 민감 문자열을 exception message/cause/suppressed에 넣어 response/JSON console/Sentry 결과에 노출되지 않음을 확인한다.
- HTTP 요청당 ERROR 1건, scheduler의 고정 분류 로그, 원인 불명 fallback을 테스트한다. 전체 라이브러리 로그를 전역 변경하지 않는다.

### 5.4 R4 — runtime release와 이미지 revision 일치

근거: [기존 workflow](../../.github/workflows/deploy-staging.yml:108), [SentryEventSanitizer](../../src/main/java/web/tosunsaeng/identity/global/observability/SentryEventSanitizer.java:48).

구현:

- 기존 task definition rendering에서 target container의 SENTRY_RELEASE를 `app-back-end-identity@<배포 이미지의 commit SHA>`로 명시적으로 전달한다.
- 새 workflow/배포 trigger/서비스/IAM/Secret 매핑은 추가하지 않는다. 기존 다른 environment/secrets/logConfiguration은 보존한다.
- 렌더링 검증은 가짜 task definition과 SHA로 수행한다. 실태스크 JSON이나 Secret을 저장소에 저장하지 않는다.
- `SENTRY_AUTH_TOKEN`은 build/업로드용이며 runtime으로 전달하지 않는다. release는 비밀값이 아니다.
- 별도 테스트 Identity를 수동/다른 경로로 배포할 때도 같은 image SHA와 release를 함께 지정하는 체크리스트를 제공한다.
- 롤백 시 실행하는 이전 이미지 SHA에 맞게 release도 함께 되돌린다. main/develop 같은 가변 문자열을 release로 사용하지 않는다.

완료 조건:

- 기존 환경변수 보존/동일 키 갱신/이미지 SHA와 release 일치를 정적으로 확인한다. 전체 task JSON을 로그 출력하지 않는다.
- Sentry sanitizer가 설정된 release를 유지하는 회귀 테스트가 통과한다.
- 실제 배포 후 target container 설정과 통제된 테스트 이벤트의 release가 일치해야 운영 완료다. 설정 코드 검증만으로 완료 처리하지 않는다.

### 5.5 배포 검증과 롤백

- 로컬 전체 테스트는 실제 Firebase/Atlas/Sentry에 접근하지 않는다.
- 별도 테스트 환경에서 안전한 성공·거절 요청을 보내 응답 X-Request-ID와 CloudWatch 이벤트가 연결되는지 확인한다.
- 실제 500/503 검증은 승인된 테스트 환경에서만 수행한다. 운영에 장애를 주입하거나 공개 에러 유발 endpoint를 추가하지 않는다.
- RS256/JWKS 변경 검증은 테스트 키로 먼저 수행하며 이번 수정 배포에서 운영 active key를 동시에 교체하지 않는다.
- 키 교체 이후에는 신·구키 중 하나만 신뢰하는 구버전 decoder로 무조건 롤백하지 않는다. 발급된 양쪽 토큰을 검증할 수 있는 revision/키 구성을 유지한다.
- health 정상 외에 로그 출력·JWT 보호 API·release 일치를 확인하고 증빙에는 주소/상태/개수/버전만 기록한다. 토큰 원문이나 개인정보는 인계하지 않는다.

## 6. 부록 — 변경 파일 및 전체 검증 표

### 6.1 예상 변경 파일

| 영역 | 파일/검토 대상 |
|---|---|
| R1 | RequestLoggingFilter, RequestLoggingFilterTests, LogCapture의 MDC 검증 보조, 신규 콘솔 출력 테스트 |
| R2 | JwtConfiguration, JwksPublicKeySet 또는 공통 public key source, JwtDecoderTests, JwksRotationTests, SecurityIntegrationTests |
| R3 | SessionSecurityService, AuthException/안전 진단 타입, RequestLogContext, GlobalExceptionHandler, RequestLoggingFilter, 관련 session/provider catch 및 테스트 |
| R4 | 기존 deploy-staging.yml의 runtime env mapping, 배포 설정 정적 검증, Sentry sanitizer/capture 테스트 |
| 문서 | identity-learning-jwt.md의 rotation 설명, 운영 점검/회전 runbook, 이번 계획서, WORKLOG/CURRENT_STATE |

이는 예정 범위이며 구현 중 필요성이 입증되지 않은 파일은 변경하지 않는다. SecurityIntegrationTests와 일부 문서는 사용자 미커밋 변경과 겹칠 수 있어 기존 내용을 보존한다.

### 6.2 테스트와 완료 기준

| 단계 | 필수 검증 | 통과 기준 |
|---|---|---|
| 사전 | 기존 상태 테스트와 git diff | 기준 revision/미커밋 범위 기록 |
| R1 | 실제 ECS console JSON | 2xx/4xx/5xx 출력, 필드 중복/appender 오류 0, 민감값 0 |
| R2 | 로컬 decoder + HTTP | active/retained 허용, unknown/retired/wrong signature 거절 |
| R2 | JWKS/encoder | public-only 게시, active-only 서명, 기존 JWT 계약 유지 |
| R3 | 실패 주입 mock | 오류 응답 불변, 안전 분류 보존, cause/message 비노출 |
| R3 | 트랜잭션 회귀 | unique conflict 처리/rollback/retry 의미 유지 |
| R4 | 렌더링 fixture | release=불변 image SHA, 다른 env/secrets/log 설정 보존 |
| 전체 | ./gradlew clean test, git diff --check | 전체 성공 및 예상 밖 변경 없음 |
| 외부 배포 | CloudWatch/JWT/Sentry | 응답·로그 연결/키 전환 호환/release 일치 확인 |

### 6.3 완료 인계

- 항목별 변경 파일, 유지한 외부 계약, 테스트 개수/결과와 재현 해소 증거.
- 예상 밖 diff 유무 및 기존 사용자 변경과의 구분.
- commit/PR/배포 revision은 실제 수행·확인된 것만 기재한다. commit/push는 사용자가 한다.
- 코드 완료와 테스트 배포 완료/운영 적용 완료를 별도로 표시한다.
- Jira 생성/수정/댓글은 이 계획 작성 범위에 없으며 필요 시 변경안을 보여주고 승인받는다.

### 6.4 구현 결과 — 2026-09-22 / TMI-176

- 브랜치: `fix/TMI-176-identity-operability` (작업 시작 시 이미 선택된 브랜치).
- R1: RequestLoggingFilter의 requestId key-value 중복을 제거했다. MDC 캡처 보조와 실제 ECS StructuredLogEncoder/ConsoleAppender 테스트를 추가해 200/401/403/409/500/503, 중복 JSON 필드·appender 오류 부재, 민감값 비노출, quiet 경로, MDC 복원 및 중복 ERROR 방지를 검증했다.
- R2: JwksPublicKeySet의 public-only 집합을 decoder와 공유한다. 정확한 kid/RS256 로컬 선택, active-only 발급, active/retained 보호 HTTP 통과 및 unknown/missing/blank/retired kid, wrong signature/type/issuer/audience/algorithm/time, workload 거절을 검증했다. 구형 account_type 없는 토큰 호환 테스트도 유지한다.
- R3: FailureDiagnostic은 원본 Throwable을 보관하지 않고 제한된 분류/operation/클래스/프레임만 전달한다. AuthException→handler→request context→단일 ERROR로 이어지며 session/provider scheduler와 worker 경고에도 안전 분류를 첨부한다. business 503은 Sentry 자동 수집으로 확장하지 않는다. 외부 응답과 unique conflict 전파는 유지한다.
- R4: 기존 ECS render step에 runtime SENTRY_RELEASE 전달만 추가했다. YAML 입력과 이미지 SHA 연결 및 Sentry sanitizer release 보존을 테스트했다. **외부 GitHub Action을 실행한 task definition fixture 렌더링은 수행하지 않았으며**, 실제 다른 env/secrets/logConfiguration 보존 및 runtime release 적용은 배포 단계에서 재확인해야 한다.
- 최종 `./gradlew clean test --no-daemon`: **146 suites / 952 tests, failures=0, errors=0, skipped=0**. 기존 미커밋 변경을 포함한 통합 작업 트리 결과다. `git diff --check` 통과. 초기 컴파일/테스트 보정 후 최종 전체 테스트를 재실행했다.
- [검증·키 전환 runbook](identity-operability-runbook.md) 작성, JWT 계약의 내부 검증/사전 게시 설명 갱신.
- 이번 예상 밖 변경 없음. 기존 Docker/entrypoint/Swagger/프론트 문서/SNS API 관련 미커밋 변경은 보존했다. workflow 파일 끝 개행 정리는 내용 외 차이다.
- commit/push/배포/실제 키 교체/기능 flag/Jira 댓글·상태 전환은 수행하지 않았다. 운영 로그량 증가, 실제 AWS/Sentry 및 Firebase/Atlas E2E는 별도 검증 대상이다.

### 6.5 Jira 댓글 초안 — 미등록

TMI-176 로컬 구현 완료. 요청 로그 중복 제거, JWKS/decoder 신뢰 키 집합 통일, 안전한 세션 DB 진단 및 scheduler/worker 로그 보강, 기존 ECS render step의 Sentry release 전달을 반영했습니다. 주요 변경 파일은 RequestLoggingFilter, JwtConfiguration/JwksPublicKeySet, SessionSecurityService/AuthException/FailureDiagnostic, GlobalExceptionHandler/RequestLogContext, session/provider worker·configuration, deploy-staging.yml 및 회귀 테스트/runbook입니다. 전체 952 tests 성공, 실패/오류/skip 0, diff 검사 통과. 기존 사용자 변경은 보존했습니다. 실제 task definition 렌더링·배포/CloudWatch/Sentry 및 Firebase/Atlas E2E, 실제 키 전환은 미수행이며 별도 승인 후 검증이 필요합니다.
