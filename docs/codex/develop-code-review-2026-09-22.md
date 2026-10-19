# develop 코드 리뷰 — 2026-09-22

## 1. 5줄 결론

1. 기준은 로컬 develop 커밋 `5f37c13563f4d777e9870e8668f13164ed85fe9c`이며, 미커밋 코드를 제외한 별도 사본을 검사했다.
2. P1: 공통 HTTP 로그의 requestId 중복으로 200/401/500 콘솔 출력이 버려지는 결함을 커밋 사본에서도 재현했다.
3. P1: 이전 키를 JWKS에 유지해도 Identity 자체 decoder가 구키 토큰을 거절하는 키 회전 결함을 추가 재현했다.
4. P2: DB 장애 원인 없는 503 변환과 SENTRY_RELEASE 런타임 전달 누락이 장애 진단을 어렵게 한다.
5. 순수 커밋 전체 테스트 896개 및 별도 결함 재현 2개를 실행했다. 제품 코드 수정·배포는 수행하지 않았다.

## 2. 사용자가 반드시 읽어야 하는 내용

### R1 [P1] MDC와 이벤트의 requestId 중복으로 콘솔 로그를 유실한다

- 위치: [RequestLoggingFilter.java:116](../../src/main/java/web/tosunsaeng/identity/global/observability/RequestLoggingFilter.java:116), ERROR 경로 139행, MDC 설정 68행.
- 조건: application.yml 기본값인 Spring Boot 3.4.2 ECS JSON console formatter 사용.
- 동작: 같은 JSON 이름을 MDC와 fluent key-value에 두 번 추가해 `The name 'requestId' has already been written` 예외가 발생하고 CONSOLE appender가 해당 이벤트를 버린다.
- 영향: 정상/거절/서버 오류의 공통 로그가 빠져 인증 실패 비율과 장애 원인을 확인하기 어렵다. 요청 자체의 실패를 유발한다고 판단한 것은 아니다.
- 검증: logger effectiveLevel=INFO/additive=true에서 200, 401, 500 각각을 실행해 중복 이름 오류 3건 확인. 기존 ListAppender 테스트는 실제 encoder를 우회한다.
- 제안: requestId 출력 출처를 MDC로 통일하고 JSON encoder/stdout 경로를 검증하는 회귀 테스트를 추가한다.

### R2 [P1] 키 회전 시 이전 토큰이 Identity 보호 API에서 즉시 거절된다

- 위치: [JwtConfiguration.java:130](../../src/main/java/web/tosunsaeng/identity/global/security/jwt/JwtConfiguration.java:130), active kid 단독 검사 151/165행.
- 조건: JWT active key를 새 키로 교체하고 이전 공개키를 rotation 설정으로 계속 노출하는 정상적인 회전 절차.
- 동작: JWKS는 이전 키를 포함하지만 `jwtDecoder`는 `withPublicKey(activePublicKey)`로 생성되며 `kid`도 현재 active ID 하나만 허용한다. decoder가 rotation key set을 사용하지 않는다.
- 영향: 만료 전 기존 사용자 토큰으로 `/api/v1/users/me` 등 Identity 보호 API 호출이 401이 된다. 공개 JWKS 기반 소비자가 구키 토큰을 허용할 수 있는 것과 Identity 자체 검증 동작이 다르다. 키 회전 중 태스크별 active key가 다르면 요청 목적지에 따라 실패할 수 있다.
- 검증: 임시 생성한 구키로 MEMBER 토큰 발급 → 구 decoder에서 검증 및 유효기간 확인 → 신/구 공개키를 JWKS에 게시 → 신 Identity decoder에서 동일 토큰 거절을 assertion으로 재현.
- 제안: 로컬 신뢰 key set의 kid로 active/retained key를 선택하도록 검증기를 구성한다. RS256/issuer/audience/type 검증은 유지하고, 알 수 없는 kid 및 제거한 구키는 계속 거절한다. 무중단 전환에는 새 공개키 사전 배포도 검토한다.
- 회귀 테스트: 회전 후 active/retained 모두 허용, unknown/retired 거절, 실제 사용자 API 인증 검증. [기존 JwksRotationTests](../../src/test/java/web/tosunsaeng/identity/global/security/jwt/JwksRotationTests.java)는 공개키 게시만 검사한다.

### R3 [P2] DB/Transaction 장애를 원인 없이 공통 503으로 바꾼다

- 위치: [SessionSecurityService.java:55](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/SessionSecurityService.java:55), 60~68행의 관련 변환도 동일.
- 조건: 로그인/세션 보안 경계에서 Mongo timeout, 연결/transaction 실패 등 DataAccessException/TransactionException 발생.
- 동작: exception의 원인/안전한 분류를 보존하지 않고 새 SESSION_SECURITY_UNAVAILABLE만 반환한다. BusinessException 처리 경로는 unexpected exception의 Sentry capture와도 분리돼 있다.
- 영향: R1 수정 후에도 503 발생 자체만 알 수 있고 DB 연결 장애와 경쟁 충돌 등을 구분하기 어렵다. 외부 503 응답 계약이 잘못됐다는 지적은 아니다.
- 제안: 사용자 응답은 그대로 두되 내부에서는 exceptionType, 제한된 실패 분류, operation, requestId로 추적한다. 예외 원문, URI, credential을 남기지 않는다. 원인 소실 및 민감값 비노출 테스트를 함께 추가한다.

### R4 [P2] CI Sentry release 값을 실제 태스크에 전달하지 않는다

- 위치: [.github/workflows/deploy-staging.yml:108](../../.github/workflows/deploy-staging.yml:108), workflow env 24행.
- 조건: 현재 workflow로 새 commit image 배포.
- 동작: CI에 SENTRY_RELEASE를 선언해도 task rendering에서는 image만 갱신한다. Docker 빌드에도 해당 값을 전달하지 않아 CI 환경값이 JVM runtime 값이 되지 않는다. `application.yml` 기본 release는 빈 값이다.
- 영향: 이미지가 바뀌어도 Sentry에 실제 배포 commit이 연결되지 않거나 기존 task 값이 유지돼 장애 시작 버전을 추적하기 어렵다. Sentry 전송 자체가 실패한다고 단정하지 않는다.
- 제안: 승인된 배포 절차에서 runtime SENTRY_RELEASE를 불변 이미지 SHA와 함께 설정하고, 테스트 이벤트의 release 일치를 확인한다. 이번 리뷰에서는 workflow를 수정하지 않았다.

## 3. 사용자가 결정해야 하는 사항

- 권장 수정 순서: R1과 실제 콘솔 회귀 테스트 → R2와 회전 호환 검증 → R3 진단 분류 → R4 배포 식별 전달.
- 리뷰는 변경 승인으로 간주하지 않았다. 수정은 별도 요청 후 진행한다.
- 기존 미커밋 작업의 커밋 범위와 별도 테스트 서버 배포 revision은 따로 확정한다.

## 4. 주요 위험과 미확인 사항

- 원격 fetch 없이 로컬 develop을 기준으로 했다. 원격 최신 develop과 일치한다고 보장하지 않는다.
- 896개 테스트 성공은 이번 재현 결함의 부재를 의미하지 않는다. 별도 진단은 결함의 존재를 확인하는 assertion이 성공한 것이다.
- Firebase/Atlas 실제 E2E, 실제 replica set rollback, 동시 부하와 AWS 신규 배포는 수행하지 않았다.
- 이번은 주요 인증·세션·SNS 변경·JWT·로그 경로의 위험 중심 리뷰이며 모든 파일/경합 조합의 무결성을 보증하지 않는다.
- Sentry 활성화/CloudWatch 수집의 이전 관측은 이전 검토 문서에 있고, 이번에는 AWS를 다시 변경/조회하지 않았다.

## 5. 현재 작업과 직접 관련된 설명

다음은 별도 신규 결함으로 분류하지 않았다.

- session fence/reissue recovery는 구현돼 있으나 기본 OFF다. recovery는 fence bean을 필수로 요구한다. 배포 flag와 프론트 Idempotency-Key 계약을 같이 확인해야 한다.
- recovery/fence 모두 OFF 경로의 비원자적 재발급 위험은 이미 기록된 호환 경로 위험이다. 이번 리뷰에서 새로 발견한 결함으로 중복 집계하지 않았다.
- SNS STARTED/외부 결과 불명 상태의 자동 재전송 금지는 의도된 안전 정책이다. 시간 만료만으로 외부 변경이 없었다고 판단하지 않는다.
- Firebase phone drift를 내부 PhoneIdentity/무료권 변경으로 전파하지 않는 것은 현재 runbook에 명시된 범위다.
- 사용자 Access Token의 account_type 및 workload 분리 계약은 변경하지 않았다.

## 6. 부록 — 검증 및 변경 범위

| 검증 | 결과 |
|---|---|
| git archive로 순수 develop 사본 준비 | 커밋 5f37c135, 미커밋 변경 제외 |
| 사본에서 ./gradlew clean test | 139 suites / 896 tests, 실패·오류·skip 0 |
| ConsoleReviewDiagnosticTests | 200/401/500의 duplicate requestId appender 오류 3건 확인 |
| RotationReviewDiagnosticTests | 아직 유효한 구키 토큰이 신 Identity decoder에서 거절됨 확인 |
| 발견 위치의 작업 트리/커밋 비교 | observability/JWT/session/workflow 동일 |
| 저장소 제품 코드 변경 | 없음 |
| 기존 사용자 미커밋 변경 | 보존 |

진단 사본은 `/private/tmp/identity-develop-review.LJXLZt`다. RSA 테스트 키는 실행 중 메모리에서만 생성하고 원문을 출력/저장하지 않았다. 진단 테스트 소스와 결과는 임시 사본에만 있으며, 저장소에는 이 문서와 작업 기록만 추가/갱신했다. 앞서 보고한 919개는 미커밋 변경 포함 작업 트리의 결과이므로 이번 896개와 구분한다.
