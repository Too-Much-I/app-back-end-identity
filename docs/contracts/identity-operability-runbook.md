# TMI-176 운영 안정성 검증·키 전환 runbook

## 1. 5줄 결론

1. 코드 변경과 운영 배포/실제 키 교체는 별개이며 이번 작업에서는 외부 설정을 변경하지 않는다.
2. 공통 요청 로그는 `requestId`를 MDC에서 한 번만 출력하며 응답 `X-Request-ID`와 연결한다.
3. JWT 발급은 active 키만, 검증은 설정된 active+retained 공개키와 정확한 `kid`/RS256을 사용한다.
4. 세션 DB 장애는 기존 503을 유지하며 내부 로그에 안전한 `failureKind`/`operation`을 추가한다.
5. 실행 이미지 SHA와 Sentry release를 맞추고 별도 테스트 환경에서 로그·보호 API·Sentry를 확인한다.

## 2. 반드시 읽어야 하는 내용

- 기존 `.github/workflows/deploy-staging.yml`은 기존 서비스 대상이다. 별도 테스트 서버 배포용으로 그대로 실행하지 않는다.
- 코드 배포와 실제 RSA 서명 키 교체를 동시에 수행하지 않는다.
- `account_type` 전역 필수화, JWT TTL/clock skew, 회원·재시도 정책, 기능 flag는 변경하지 않는다.
- ERROR에 예외 원문을 추가하거나 전체 DEBUG를 활성화하지 않는다. 요청 body/query/credential은 수집 대상이 아니다.
- 코드 기준: [RequestLoggingFilter](../../src/main/java/web/tosunsaeng/identity/global/observability/RequestLoggingFilter.java), [JwtConfiguration](../../src/main/java/web/tosunsaeng/identity/global/security/jwt/JwtConfiguration.java), [FailureDiagnostic](../../src/main/java/web/tosunsaeng/identity/global/observability/FailureDiagnostic.java).

## 3. 배포 전 결정·확인할 사항

- 테스트 대상 서비스/불변 이미지 revision과 승인자.
- 실제 사용자 및 workload 최대 TTL, 각 verifier skew, JWKS 캐시 갱신/만료 정책.
- 전체 태스크 전환 완료와 마지막 구키 발급 인스턴스 종료 UTC 시각을 확인할 담당자.
- 별도 테스트 환경 CloudWatch/Sentry 검증 담당자. 실제 500/503 실패 주입은 승인받은 테스트 환경에서만 한다.

## 4. 위험과 미확인 사항

- 로그 복구로 수집량과 비용이 증가할 수 있다. 정상 health/JWKS 성공 생략은 유지한다.
- 로컬 테스트는 실제 Atlas rollback, Firebase, AWS 콘솔 수집, 실제 Sentry 전송 성공을 증명하지 않는다.
- Sentry 환경변수 매핑은 YAML 정적 검사로 검증한다. 외부 GitHub Action 실행 및 실제 task definition 렌더링/배포 결과는 별도 확인해야 한다.
- 신·구키 발급이 섞인 뒤 active-only 검증 버전으로 무조건 롤백하면 한쪽 토큰이 거절될 수 있다.

## 5. 검증 및 전환 절차

### 5.1 코드 배포 후 검증

1. 별도 테스트 Identity에 확정 이미지로 배포하고 health, 활성 기능/이벤트 목적지 분리를 확인한다.
2. 안전한 성공 요청과 인증 거절 요청을 보내 응답 `X-Request-ID`와 JSON 콘솔 로그의 `requestId`가 같고 한 필드인지 확인한다.
3. 200/401/403/409 및 통제된 500/503의 status/route/errorCode를 확인한다. 단일 요청의 ERROR는 한 번만 출력되어야 한다.
4. 정상 health/JWKS는 공통 성공 로그가 생략되고 실패는 남는지 확인한다. 로그 formatter/appender 오류가 없어야 한다.
5. 아래 실패 분류를 확인하되 원본 예외/연결 문자열/사용자 식별자는 수집·공유하지 않는다.
6. Sentry 활성 대상 컨테이너에 `SENTRY_RELEASE=app-back-end-identity@<실제 이미지 commit SHA>`를 지정한다. `SENTRY_AUTH_TOKEN`은 runtime에 넣지 않는다.
7. 통제된 unexpected 500 이벤트의 Sentry release가 해당 이미지와 일치하는지 확인한다. business 503의 자동 Sentry 수집은 추가하지 않는다.

### 5.2 실제 서명 키 교체 — 별도 승인 후 수행

1. 이전 active 키로 계속 발급하는 동안 신규 공개키/kid를 retained 목록에 추가한다. `JWT_PREVIOUS_KEY_IDS`와 `JWT_PREVIOUS_PUBLIC_KEY_LOCATIONS`는 순서대로 대응해야 하며 중복 kid를 허용하지 않는다.
2. 모든 Identity 태스크가 양쪽 키를 검증할 수 있는 코드/설정으로 전환됐는지 확인하고 소비자 JWKS 전파·캐시 준비를 확인한다.
3. 신규 active private/public key와 kid로 전환하되 이전 공개키를 retained 목록에 유지한다. 혼합 배포 중 양쪽 토큰이 검증되는지 확인한다.
4. 마지막 구키 발급 인스턴스 종료 UTC 시각부터 실제 최대 Token TTL + 관련 verifier skew 이상 대기한다. JWKS 캐시 정책도 충족해야 하며 기본 30분을 하드코딩하지 않는다.
5. 이전 공개키를 제거하고 현재 키의 정상 인증 및 제거 키 거절을 확인한다. 토큰 원문은 증빙으로 남기지 않는다.

### 5.3 롤백

- 키 교체 전 코드만 배포했다면 승인된 이전 이미지/환경으로 복귀할 수 있다. 다만 R1 로그 유실이 다시 발생할 수 있다.
- 키 교체 후에는 이미 발급된 양쪽 토큰을 모두 검증하는 코드와 공개키 목록을 유지한다. 발급 키를 되돌려도 새 키의 미만료 토큰을 검증해야 한다.
- 실행 이미지를 되돌릴 때 Sentry release도 그 이미지 SHA로 맞춘다.

## 6. 부록: 진단 및 증빙

### 진단 필드

| 필드 | 규칙 |
|---|---|
| operation | SESSION_TRANSACTION, SESSION_EPOCH_READ, SESSION_REVOCATION_BATCH, PROVIDER_CHANGE_BATCH |
| failureKind | DB_TIMEOUT, DB_UNAVAILABLE, DB_CONFLICT, TRANSACTION_FAILURE, DB_FAILURE, UNKNOWN |
| exceptionType | 예외 클래스 이름만, 메시지 제외 |
| causeTypes | 최대 5개의 원인 클래스, 순환 감지, suppressed 제외 |
| stackTrace | 최대 24개 클래스/메서드/행 번호, 파일 경로·지역 변수·메시지 제외 |

분류는 exception 타입, Mongo WriteConflict 코드 112, transaction error label로 결정한다. 래퍼보다 구체적인 DB 원인이 우선한다. 원본 Throwable을 AuthException cause/suppressed로 보관하지 않으며 response result에도 진단을 포함하지 않는다. 기존 unique conflict 전파와 transaction rollback/retry 의미는 유지한다.

### 완료 증빙

- 테스트 결과/테스트 수, 배포 환경·이미지 revision, requestId 연계 성공 여부, formatter 오류 유무.
- 사용한 공개 kid와 보존 정책, 신·구키 정상/비정상 요청 결과. 키 내용/토큰 원문은 제외.
- Sentry release 일치 여부. 코드 완료, 테스트 배포 완료, 운영 적용 완료를 각각 구분한다.
- 관련 테스트: RequestConsoleTests, JwtRotationVerificationTests, SessionSecurityDiagnosticTests, ProviderSchedulerDiagnosticTests, SessionRevocationConfigurationTests, SentryReleaseDeploymentTests, SentryCaptureIntegrationTests.
