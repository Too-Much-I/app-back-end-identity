# Identity 운영 안정성·로그 검토 — 2026-09-22

## 1. 5줄 결론

1. **P1: 공통 HTTP 요청 로그가 ECS JSON 변환에서 버려지는 결함을 로컬 재현했다.** MDC와 개별 필드의 `requestId` 중복이 원인이다.
2. 현재 AWS 서비스는 태스크 1개/ALB 정상 대상 1개이지만, 정상 health는 인증 기능과 로그 정상의 증거가 아니다.
3. 현재 develop 작업 트리 전체 테스트 919개는 통과했다. 기존 로그 테스트는 실제 콘솔 인코더를 거치지 않아 이번 결함을 놓친다.
4. DB 예외 원인 소실, 재발급 보호 기능 OFF 경로, Sentry release 전달 누락도 보완/배포 점검 대상이다.
5. 이번은 진단만 수행했다. 운영 변경·배포·제품 코드 수정 없이, main 기반 별도 로그 hotfix를 먼저 권장한다.

## 2. 사용자가 반드시 읽어야 하는 내용

### P1 — 공통 요청 로그 누락: 확인된 코드 결함

[RequestLoggingFilter](../../src/main/java/web/tosunsaeng/identity/global/observability/RequestLoggingFilter.java:68)는 `MDC.put("requestId", ...)` 후 INFO/ERROR 이벤트에 같은 이름의 `addKeyValue`를 다시 추가한다(116/139행).
[application.yml](../../src/main/resources/application.yml:13)의 기본 콘솔 형식은 ECS JSON이다. Spring Boot 3.4.2 JSON writer는 중복 이름을 허용하지 않는다.

로컬 진단의 결과:

```text
effectiveLevel=INFO, additive=true
Appender [CONSOLE] failed to append.
java.lang.IllegalStateException: The name 'requestId' has already been written
```

200, 401, 500 각각에 대해 동일 원인의 appender 오류 3건을 확인했다. 대조군인 서비스 INFO 로그는 출력된다. 따라서 요청은 처리되더라도 완료/거절/서버 오류의 공통 로그는 빠질 수 있다. 이 결함 자체를 인증 실패 원인이라고 판단하지는 않는다.

운영 이미지 태그에 대응하는 로컬 Git 객체 `7f2188df97d2adc5faef8ddec6cb6ffa6ee8b0d4`에도 동일 코드가 있다. 현재 CloudWatch 관측과 일치하는 설명이다. 운영 JVM 내부 appender status를 직접 조회한 것은 아니다.

권장 수정: requestId의 출력 출처를 MDC 하나로 통일하고, 실제 ECS JSON 인코더/콘솔 출력에 대한 성공·거절·서버 오류 회귀 검증을 추가한다. 토큰·요청 body·예외 원문 로그 추가는 해결책이 아니다.

### P2 — 로그 테스트가 실제 콘솔 경로를 우회

[LogCapture](../../src/test/java/web/tosunsaeng/identity/support/LogCapture.java:20)는 TRACE로 강제 설정하고 `additive=false`인 ListAppender로 이벤트를 수집한다. [기존 테스트](../../src/test/java/web/tosunsaeng/identity/global/observability/RequestLoggingFilterTests.java)는 이벤트 생성/민감값 제거는 검증하지만 JSON 직렬화나 stdout 도달 여부는 검증하지 않는다. 단위 테스트는 유지하되 콘솔 인코더 검증을 추가해야 한다.

### P2 — 일부 장애는 원인 분류가 사라짐

[SessionSecurityService](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/SessionSecurityService.java:55)는 DataAccessException/TransactionException을 원인 없이 SESSION_SECURITY_UNAVAILABLE(503)로 변환한다. [GlobalExceptionHandler](../../src/main/java/web/tosunsaeng/identity/global/exception/GlobalExceptionHandler.java)의 BusinessException 경로는 Sentry capture를 호출하지 않는다. 로그 누락을 고쳐도 이 경로는 errorCode/status만 있고 DB timeout·충돌·transaction 실패를 구별하기 어렵다.

[ReissueRecoveryService](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/ReissueRecoveryService.java)는 일부 원인을 counter로 분류하지만, 현재 저장소 의존성/설정에서 외부 metrics exporter는 확인되지 않았다. 운영 수집/경보 연결 완료로 간주할 수 없다.

권장: 예외 message/credential 대신 허용된 실패 분류, exceptionType, operation, 재시도 여부 등을 내부 진단에 남긴다. 예상된 사용자 4xx를 전부 Sentry 오류로 올리지는 않는다.

## 3. 사용자가 결정해야 하는 사항

- 우선순위 권장: main hotfix로 공통 로그 누락 수정 → 기존 서비스 배포/CloudWatch 검증 → develop에도 반영 → 별도 테스트 Identity 준비 계속.
- 이번 검토 요청은 수정/배포 승인으로 간주하지 않았다. 현재 작업 트리에는 다른 미커밋 변경이 많으므로 전체 develop을 기존 서비스에 바로 배포하지 않는다.
- 503 진단 보강 및 외부 경보 연결은 로그 hotfix와 분리할지 확정한다.

## 4. 주요 위험과 미확인 사항

### 재발급 보호 설정에 따른 장애 위험

[TokenReissueService](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/TokenReissueService.java:53)는 recovery와 session fence가 없으면 transaction 없이 기존 session 회전을 먼저 저장하고 새 token/session을 만든다(125행 이후). 이후 단계 실패 시 기존 token은 이미 회전돼 있을 수 있다. 두 기능 기본값은 false다. fence ON은 DB 원자성, recovery ON은 응답 유실 복구를 담당하므로 둘을 같은 보호로 보지 않는다.

운영은 이번 develop과 다른 main hotfix 이미지다. 기존 앱 호환성 검토 없이 신규 recovery를 ON으로 바꾸면 안 된다(Idempotency-Key 계약 필요). 테스트 Identity는 프론트 계약과 함께 fence/recovery 조합을 확정하고 실제 replica set rollback을 검증해야 한다.

### Sentry 배포 revision 연결

[workflow](../../.github/workflows/deploy-staging.yml:24)의 SENTRY_RELEASE는 CI 환경에만 선언돼 있다. [task rendering](../../.github/workflows/deploy-staging.yml:108)은 image만 교체하고 SENTRY_RELEASE를 컨테이너 환경에 전달하지 않는다. Dockerfile도 해당 값을 전달하지 않는다. 현재 task definition 23의 환경 변수 목록에도 SENTRY_RELEASE가 없었다. 따라서 이 경로로는 실제 commit이 Sentry release에 연결되지 않는다. Sentry 전송 자체의 실패를 의미하는 것은 아니다.

### 검토 한계

- 새 Firebase 가입/연결/재발급의 실제 외부 E2E, Atlas transaction rollback, 권한·키 실값, 경보 수신은 미검증.
- 운영 요청/데이터를 생성하거나 장애를 주입하지 않았다. 운영 JWT 원문·비밀값은 조회하지 않았다.
- 최근 24시간 보이는 로그에 오류가 없다는 사실만으로 장애가 없다고 결론내릴 수 없다. 공통 로그 결함이 확인됐다.
- 요청 filter의 안전한 로깅/Sentry 정제 코드는 있어도 모든 외부 라이브러리·컨테이너 로그의 민감값 비노출까지 보장한 검토는 아니다.
- 시작 시 Mongo verifier는 hello의 replica set/session 지원을 검사한다. 실제 다중 collection rollback probe를 대신하지 않는다.

## 5. 현재 작업과 직접 관련된 설명

서버 생존, 애플리케이션 로그 생성, 콘솔 직렬화, CloudWatch 수집은 서로 다른 단계다. 이번에는 CloudWatch 연결이 동작해도 그 이전 JSON 변환에서 특정 로그가 버려졌다. Container Insights 설정을 켜는 것으로 이 코드 결함이 해결되지는 않는다.

안전한 배포 완료 기준:

1. 실제 콘솔 JSON에서 requestId가 한 번만 나오고 2xx/4xx/5xx 이벤트가 기대한 횟수로 출력된다.
2. 테스트 환경에서 동일 requestId의 응답과 CloudWatch 로그를 연결한다. 로그의 토큰/body/credential 비노출도 검증한다.
3. 실제 5xx 알림은 테스트 환경에서 통제된 방법으로 검증한다. 운영 서비스에 일부러 500을 유발하지 않는다.
4. 로그 배포 수정과 SNS/재발급 기능 활성화는 분리한다.

## 6. 부록 — 조사 근거와 진단 결과

### develop 기준 재확인

- 로컬 develop HEAD는 `5f37c13563f4d777e9870e8668f13164ed85fe9c`다. 원격 최신 여부는 이번에 fetch하지 않아 미확인이다.
- 앞선 919개 전체 테스트 및 콘솔 누락 재현은 main checkout이 아니라 **develop의 현재 미커밋 변경 포함 작업 트리**에서 수행했다. 운영 조회만 main hotfix 이미지와 비교한 것이다.
- `git show develop:<path>`와 `git diff develop -- <paths>`로 확인한 결과, observability 패키지/application.yml/session 패키지/배포 workflow는 커밋된 develop과 현재 작업 트리가 동일하다. requestId 중복 및 원인 소실/설정 의존 위험은 미커밋 변경 탓이 아니다.
- build.gradle 미커밋 변경은 Swagger 공유 task 추가이며 Spring Boot/Logback 의존 버전을 변경하지 않는다.
- develop에는 재발급 응답 복구 및 session fence 구현이 이미 있다. recovery ON bean은 SessionSecurityService를 필수 의존하여 fence OFF 복구 기동을 막는다. 따라서 '보호 구현 없음'이 아니라 '기본 OFF 및 배포 설정/프론트 계약 확인 필요'로 분류한다.
- 이번 추가 확인은 코드 비교이며 제품 코드를 변경하지 않았다. 순수 커밋 snapshot만 분리한 전체 테스트는 실행하지 않았다. 직전 작업 트리 전체 테스트와 동일 코드의 재현 증거를 재사용했다.

### AWS 읽기 확인 (2026-09-22 약 13:35–13:37 KST)

| 항목 | 관측 사실 |
|---|---|
| 클러스터/서비스 | tosunsaeng-staging-cluster / tosunsaeng-identity-service |
| task definition | tosunsaeng-identity:23 |
| 이미지 태그 | 7f2188df97d2adc5faef8ddec6cb6ffa6ee8b0d4 |
| 실행 상태 | desired 1, running 1, pending 0, 배포 성공 |
| ALB 상태 | 정상 1, 비정상 0 |
| 로그 드라이버 | awslogs |
| 로그 그룹/리전 | /ecs/tosunsaeng-identity / ap-northeast-2 |
| 최근 1시간 | 표시 로그 없음 |
| 최근 24시간 | 15건: 재발급 성공 10건, Guest 등록 성공 5건. 공통 HTTP 이벤트 없음 |
| Sentry | enabled=true, DSN은 Secret 참조. 실제 전달·알림 수신 미확인 |
| 실행 환경 | 0.5 vCPU / 1 GiB; 이번 부하 테스트 미실시 |

관측 로그의 사용자 ID, requestId 원문과 계정 개인정보는 문서에 복사하지 않았다. 인증서 작업 탭은 건드리지 않고 별도 읽기 탭을 사용했다.

### 로컬 진단 절차

- 임시 JUnit 진단에서 MockEnvironment의 logging.structured.format.console=ecs로 LogbackLoggingSystem을 초기화했다.
- LogCapture를 쓰지 않고 실제 CONSOLE appender에 RequestLoggingFilter를 연결했다.
- Mock request/response로 200, 401, 500을 각각 실행했다. 각 chain 내부의 대조용 service INFO 로그는 출력됐다.
- Logback StatusManager에서 중복 requestId IllegalStateException 3건을 assertion으로 확인했다. effectiveLevel=INFO 및 additive=true로 레벨/전파 차단과 구분했다.
- 진단은 외부 DB/Provider 호출 없이 수행했다. 재현에 사용한 임시 테스트 파일은 진단 후 제거했다. 제품 코드와 기존 테스트는 수정하지 않았다.
- 기존 전체 `./gradlew clean test`: 141 suites / 919 tests, failures=0/errors=0/skipped=0. 진단 테스트의 성공은 결함 부재가 아니라 결함 재현 성공을 의미한다.

### 기존 보호 장치 확인

- 공통 로그: requestId 검증 및 MDC 정리, route template 사용, body/query/header 원문 제외.
- 5xx: exception message 대신 제한된 type/stack frame 기록 의도.
- Sentry: 허용 필드만 새 event로 복사하며 request/user/breadcrumb/exception value 등을 제거.
- Docker secret bootstrap: 앞서 추가한 파일 권한·필수 설정 검사 테스트 포함. 실제 이미지/배포 검증은 여전히 별도다.
