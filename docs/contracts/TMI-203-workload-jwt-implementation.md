# TMI-203 Identity–Billing JWT 수신·발신 구현

> 2026-10-13 후속: [SigV4 제거 구현 결과](TMI-203-SigV4-removal-implementation-2026-10-13.md)가 인증 선택 플래그/기본 SigV4/4목적 필수 설정 설명을 대체한다. 현재는 JWT 단일 방식이며 활성 publisher 목적의 audience만 필수다. 아래 내용은 최초 구현 시점 기록이다.

- 날짜: 2026-10-12 / 브랜치: `feat/TMI-203-withdrawal-snapshot-feed-recovery`
- 사용자에게 Billing의 인증 규격 수락을 전달받아 로컬 구현했다. 원격 병합·배포·실환경 인증 검증은 아니다.

## 1. 5줄 결론

1. 복구 네 API에 전용 RS256 JWT 검증기를 연결했다. 서명/시간 실패401, 서명 검증 후 권한 불일치403, 내부 장애503을 구분한다.
2. 복구 기능 ON일 때 전용 HTTPS connector를 설치하고 실제 Tomcat connector 기준으로 public/internal을 격리한다.
3. Billing 이벤트4종의 JWT 발신 모드를 추가했다. 매 시도 새로운 jti/토큰을 발급하며 본문/응답/재시도 의미는 유지한다.
4. 수신 기본 OFF, 발신 JWT 기본 OFF다. 기존 SigV4 코드를 유지하며 JWT 실행 실패를 SigV4로 자동 전환하지 않는다.
5. 코드 회귀와 운영 인증 검증은 다르다. 실제 환경·키·ALB/SG·TLS 설정과 Billing 공동 실행 후 별도 승인으로 활성화해야 한다.

## 2. 사용자가 반드시 읽어야 하는 내용

이 구현만 배포해도 새 JWT 방식이 자동으로 켜지지 않는다. 기존 publisher 활성 플래그는 그대로이며 새 발신 플래그는 **활성화된 Billing publisher들의 인증 방식만** 선택한다. 사용자 로그인과 LC 토큰은 전환 대상이 아니다.

복구 포트에는 JWT뿐 아니라 별도의 HTTPS 인증서가 필요하다. JWT 서명키와 TLS private key는 서로 다른 용도이며 재사용하지 않는다. ALB→task에 평문 HTTP를 자동 허용하는 설정은 넣지 않았다. 이 구현은 HTTPS 방식의 배포 후보이며 실제 네트워크 방식 승인을 대체하지 않는다.

단순 Bearer JWT는 본문이나 Idempotency-Key를 서명하지 않는다. 기존 `Grant.verifyPayload`를 제거하고 검증된 호출자 Owner를 반환하는 인터페이스로 바꿨다. 동일한 보장을 제공하는 척하는 no-op payload 검증은 없다.

## 3. 배포 전 확인·결정할 사항

- Identity/Billing 실제 trust environment와 source environment 대응, issuer 및 목적별 audience 문자열을 양 서버 동일하게 설정한다. 환경 default/wildcard/자동변환 없음.
- 전용 workload 포트(1~65535, public 포트와 다름), HTTPS target group, ALB origin/경로와 SG·직접 task 접근 제한을 검증한다. context path는 root만 허용한다.
- workload 포트는 네 POST만 허용하므로 일반 `/actuator/health`는403이다. ALB health check는 기존 public health 포트로 지정하는 등 별도의 검증된 설정이 필요하다. health 예외를 암묵적으로 열지 않았다.
- 수신 Billing public JWK set과 발신 Identity private JWK는 기존 user/LC/cursor/support 키와 분리해 주입한다. 실제 키 생성·등록은 이번 작업에서 하지 않았다.
- 모든 replica를 포함한 old/new 키 교체·긴급 폐기, clock 오차 관측, 401/403 중단·수동 재개, 응답 유실/복원 공동 검증 후 활성화한다.

## 4. 주요 위험과 미확인 사항

- 로컬 RSA/Mock HTTP/Tomcat valve 테스트는 실 ALB TLS handshake, SG 차단이나 Billing 배포를 입증하지 않는다.
- ALB의 HTTPS target 사용은 task 인증서 검증까지 자동 보장한다는 뜻이 아니다. 인증서 정책·네트워크 경계는 운영 검증 대상이다.
- 공개 JWK 배포 방식은 상대가 새 공개키를 먼저 배포해야 한다. jti는 재사용 차단 DB가 아니며 유효시간 내 credential 재사용은 가능하다.
- 신규 인증 fixture의 Billing 측 동기화/실행은 아직 확인하지 않았다. 기존 recovery-r1/scalar-end-1 body hash는 변경하지 않았다.
- 기존 TMI-203 source cleanup/sweep 운영 연결·폐기 stream metadata·실경보/공동복원 등 별도 미완료 범위는 이 인증 구현으로 완료되지 않는다.

## 5. 구현 상세

### 수신

- `RecoveryJwtVerifier`: strict UTF-8/중복 JSON/trailing 검증, 단일 Authorization, compact token, RS256/kid, RSA≥2048, 고정 issuer/subject/문자열audience, UUIDv4 jti, 정수 NumericDate/TTL≤60/skew30/nbf 관계. remote key header/crit/alg 혼용 거절. key 설정 오류 원인문자열은 노출하지 않는다.
- 시간은 overflow-safe BigInteger 비교. clock 예외는 인증 오류로 바꾸지 않고503으로 전달한다. 실제 clock drift 관측은 별도다.
- `RecoveryWorkloadConfiguration`: 별도 TLS1.2/1.3 connector 및 engine valve. `request.getConnector().getPort()`로 실제 포트를 읽고 내부 attribute를 설정한다. X-Forwarded-Port나 프록시가 바꾼 localPort는 신뢰하지 않는다.
- public 포트는 `/internal` 및 그 하위 경로를 거절한다. workload 포트는 네 정확 POST만 허용한다. 요청 경로의 정규화 별칭도 최종 verifier의 raw 경로 비교를 통과해야 한다.
- Owner는 기존 `WithdrawalCaptureProperties.environment`와 고정 consumer `billing`으로 생성한다. caller body/header로 바꾸지 않는다.
- `RecoveryHttpFilter`의 strict body/16KiB·DTO/error는 유지한다. 인증 실패는 업무 service 호출 전에 종료한다.

### 발신

- `BillingJsonTransport`로 transport 경계를 분리했다. 기존 publisher 호환을 위해 기존 DetailedResult/TransportException 타입은 유지한다.
- `BillingJwtJsonTransport.Factory`는 RSA private/public pair 기동 self-check와4목적 audience 구성을 검증한다. 각 HTTP 시도 직전60초 JWT 발급, 개별 목적 audience, identity-service sub, 새 jti 및 traceparent를 사용한다.
- 고정 HTTPS origin+네 route allowlist, redirect NEVER. 본문 bytes·eventId·operationId를 변경하지 않는다. Retry-After 개수/길이 bound 및 기존 해석은 유지한다.
- withdrawal, trial eligibility, trial owner, owner merge 설정에 연결했다. JWT factory가 활성화되면 그 모드만 사용하며 실패 후 fallback하지 않는다. 기본 OFF일 때만 기존 SigV4 경로를 선택한다.
- TLS/connection/timeout/interruption 결과는 기존 publisher 분류에 전달한다. interrupt 상태를 복구하고 토큰이 포함될 수 있는 원 예외를 보관하지 않는다.

### 설정명 (값은 배포 담당자가 확정)

| 환경변수 | 용도 / 기본 |
| --- | --- |
| `WITHDRAWAL_RECOVERY_HTTP_ENABLED` | 복구 수신 ON / false |
| `WITHDRAWAL_RECOVERY_WORKLOAD_PORT` | 전용 포트 / 0(ON이면 오류) |
| `WITHDRAWAL_RECOVERY_BILLING_PUBLIC_JWKS` | Billing 공개 JWK set / 빈 값 |
| `WITHDRAWAL_RECOVERY_BILLING_ISSUER` | 허용 issuer / 빈 값 |
| `WITHDRAWAL_RECOVERY_AUDIENCE` | 복구 전용 audience / 빈 값 |
| `WITHDRAWAL_RECOVERY_TLS_CERTIFICATE_FILE` | TLS PEM certificate 경로 / 빈 값 |
| `WITHDRAWAL_RECOVERY_TLS_PRIVATE_KEY_FILE` | 별도 TLS PEM private key 경로 / 빈 값 |
| `BILLING_WORKLOAD_JWT_ENABLED` | 기존 활성 Billing publisher의 JWT 모드 / false |
| `BILLING_WORKLOAD_PRIVATE_JWK` | Identity 전용 private JWK / 빈 값 |
| `BILLING_WORKLOAD_ISSUER` | Identity workload issuer / 빈 값 |
| `BILLING_WORKLOAD_WITHDRAWAL_AUDIENCE` | 탈퇴 이벤트 목적 / 빈 값 |
| `BILLING_WORKLOAD_TRIAL_AUDIENCE` | trial eligibility 목적 / 빈 값 |
| `BILLING_WORKLOAD_TRIAL_OWNER_AUDIENCE` | trial owner 목적 / 빈 값 |
| `BILLING_WORKLOAD_MERGE_AUDIENCE` | owner merge 목적 / 빈 값 |

기존 runtime/capture/clock/index preflight와 cursor key 설정도 별도로 필요하다. JWT 설정만으로 recovery 서비스를 만들거나 DB를 이관하지 않는다. JWT Authorization 상한은8192자이며 실제 container/ALB header 상한도 별도 확인해야 한다. JOSE typ는 생략 또는 JWT, 미사용 추가 claim은 권한에 반영하지 않는다.

## 6. 부록 — 변경 파일·테스트·다음 작업

- 수신 코드: `domain/user/withdrawalrecovery/RecoveryIngressVerifier.java`, `RecoveryHttpFilter.java`, 신규 `RecoveryJwtVerifier.java`, `RecoveryWorkloadConfiguration.java`.
- 발신 코드: `global/workload/BillingJsonTransport.java`, `BillingJwtJsonTransport.java`, `BillingJwtConfiguration.java`, 기존 `BillingSigV4JsonTransport.java` 인터페이스 구현.
- 연결부: `WithdrawalCaptureConfiguration.java`, `BillingOwnerEventDeliveryAdapter.java`, `OwnerEventConfiguration.java`, `JdkPhoneEligibilityBindingDeliveryAdapter.java`, `PhoneEligibilityPublisherConfiguration.java`.
- 설정: `src/main/resources/application.yml`. 전체 경로 기준 코드 root는 `src/main/java/web/tosunsaeng/identity/`다.
- 테스트: 기존 RecoveryHttpFilter/Configuration 테스트 개정, 신규 RecoveryJwtVerifier/RecoveryWorkloadConfiguration/BillingJwtJsonTransport 테스트. 신규 `contracts/payment-lifecycle/workload-jwt-r1/`에 합성 시간 fixture16종 추가.
- 검증: 실제 로컬 RSA 서명/키교체, 시간 경계/strict JSON/권한/포트 위조, connector 기본OFF/설정오류, 발신 body·fresh token·traceparent·redirect/interrupt·legacy 회귀. 실제 외부 API 호출 없음.
- 전체 실행 결과와 건수는 이번 WORKLOG/CURRENT_STATE에 기록한다. 첫 로컬 Gradle 실행은 sandbox 캐시 제한, 이후 허용된 실행에서 검증했다. 초기 테스트 컴파일 오류1건은 수정 후 재실행했다.
- 다음: 신규 fixture/설정표를 Billing과 맞추고 실환경 네트워크/키를 별도 승인으로 구성한 뒤 공동 검증. Jira 댓글/상태 변경·commit/push/배포는 하지 않았다.
- Jira 댓글 초안(미등록): 전용 JWT 수신·HTTPS connector·Billing4이벤트 발신 모드 구현, 기존 body/재시도/LC 계약 유지. 전체 로컬 회귀 결과는 WORKLOG 참조. 실제 ALB/TLS/키 운영·공동복원 검증은 남아 있음.
