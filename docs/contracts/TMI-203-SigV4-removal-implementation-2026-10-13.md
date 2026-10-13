# TMI-203 SigV4 제거·JWT 단일화 구현 결과

- 날짜: 2026-10-13 / 브랜치: `feat/TMI-203-withdrawal-snapshot-feed-recovery`
- 범위: 사용자 후속 구현 요청에 따른 Identity 로컬 변경. Billing 저장소·실제 배포는 변경하지 않았다.

## 1. 5줄 결론

1. Identity–Billing 발신4종에서 SigV4 구현·credential bean·선택 분기를 제거하고 JWT만 사용하도록 변경했다.
2. 공통 결과·오류·Retry-After 처리를 `BillingJsonTransport`로 이동해 기존 publisher 정책을 유지했다.
3. JWT 선택 플래그 없이 기존 publisher ON/OFF로 signer 생성을 결정한다. 모두 OFF면 signing key 없이 기동한다.
4. 제거된 인증 선택/region 설정이 남으면 값과 관계없이 명시 기동 오류다. 실제 배포 설정 제거가 선행돼야 한다.
5. 전체2089개 중2065통과/24 opt-in skip/실패·오류0. 실제 사용처·ALB/TLS/키·Billing 연동 및 복원 검증은 별도다.

## 2. 사용자가 반드시 읽어야 하는 내용

**이 버전을 배포하기 전에 아래 구 설정을 삭제해야 한다. false나 빈 값으로 바꾸는 것으로는 부족하다.**

| 제거할 외부 설정 | 제거할 Spring property |
| --- | --- |
| `BILLING_WORKLOAD_JWT_ENABLED` | `app.billing-workload.jwt-enabled` |
| `BILLING_WITHDRAWAL_REGION` | `app.withdrawal-capture.region` |
| `PHONE_ELIGIBILITY_PUBLISHER_REGION` | `app.phone-eligibility-publisher.region` |
| `OWNER_EVENT_BILLING_REGION` | `app.owner-event.billing-region` |

canonical property 및 Spring의 대응 system-environment 이름도 검사한다. 오류에는 설정 이름만 포함하며 값은 기록하지 않는다. 저장소의 main/test YAML에 있던 기본 선언은 제거했다. 실제 ECS/Secret/deployment 환경은 수정하지 않았다.

아래 기능 플래그는 **유지**한다. 하나 이상 ON이면 signing key·issuer 및 ON인 목적의 audience가 필수다. OFF인 목적의 audience는 요구하지 않으며 그 목적의 토큰을 발급하지 않는다.

- `BILLING_WITHDRAWAL_PUBLISHER_ENABLED`
- `PHONE_ELIGIBILITY_PUBLISHER_ENABLED`
- `OWNER_EVENT_BILLING_USER_MERGED_PUBLISHER_ENABLED`
- `OWNER_EVENT_BILLING_TRIAL_REBIND_PUBLISHER_ENABLED`

각 기능의 기존 capture/origin/runtime 등 필수 조건도 유지한다. 모든 Billing 발신이 OFF인 경우 signer/발신 HTTP client를 생성하지 않는다. LC 발신만 ON인 경우에도 Billing signing key는 필요 없다. 복구 수신 ON/OFF와 발신은 독립적이며 source capture나 미전송 기록을 삭제하지 않는다.

## 3. 배포 전에 확인할 사항

- 양측 실제 활성 SigV4 sender/receiver·구버전 replica 존재 여부 확인. 미사용으로 단정하지 않는다.
- 구 설정 제거와 필요한 JWT 설정/키/환경·origin·전용 포트/TLS 준비를 하나의 배포 절차로 묶는다. 새 설정 없이 기존 ON 플래그만 유지하면 기동이 실패한다.
- 기존 활성 sender가 있다면 새 claim/발신을 중지하고 진행 요청을 정리한 뒤 수신 JWT 준비→발신 재개 순서로 전환한다. pending delivery와 source capture·LC는 보존한다.
- Billing의 신규 fixture 실행, 양방향 인증/키 교체·인증 중단/재개·응답유실/복원을 공동 검증한다. 실제 판매·자동 삭제 활성화는 별도 승인이다.

## 4. 주요 위험과 미확인 사항

- Jira의 기존 사용처 확인 조건은 원격 변경하지 않았다. 후속 사용자 요청으로 로컬 제거를 구현했으며 실제 사용처 확인은 배포 전 필수 조건으로 유지한다. Jira 설명 개정은 필요하면 별도 승인 후 수행한다.
- SigV4 전용 클래스와 테스트 파일은 삭제했다. 기존 Git 추적 파일이므로 저장소 이력에서 복구할 수 있다. 구 인증으로 런타임 자동 fallback하는 경로는 없다.
- AWS SDK 명시 BOM/auth/http-auth-aws 의존성을 제거했다. ECS role/Secrets Manager 권한이나 다른 라이브러리의 AWS 기능을 제거한 것은 아니다.
- 현재 보고하는 성공은 로컬 테스트다. Atlas opt-in18개/support opt-in6개는 실행하지 않았으며 실제 AWS/Billing를 호출하지 않았다.
- 기존 TMI-203 source cleanup/sweep 운영 연결·폐기metadata·실경보·공동복원 미완료 항목은 이번 인증 정리와 별개다.

## 5. 변경한 동작과 유지 계약

- `BillingJsonTransport`: DetailedResult/TransportException/FailureKind와 기존1~300초 단일 정수 Retry-After 해석을 소유한다. 상세 원본 헤더는 탈퇴 전용 날짜/복수값 판정에 계속 전달한다.
- `BillingJwtConfiguration`: 활성 publisher 조건에 따라 factory 생성. 구 설정은 publisher OFF에서도 거절한다. 활성 목적 audience만 구성한다.
- `BillingJwtJsonTransport.Factory`: 비어 있지 않은 허용 route 부분집합과 활성 audience 유일성을 검증한다. 구성되지 않은 route는 토큰 발급 실패이며 전송하지 않는다.
- 세 publisher 설정부는 JWT factory를 필수 의존한다. optional factory 부재에 따른 구 transport 선택을 제거했다.
- 세 region property와 필수 검증, credential bean, SigV4 adapter 생성자를 제거했다. 기존 publisher·RetryPolicy의 업무 상태 분기 자체는 변경하지 않았다.
- 복구 HTTP OFF denyAll과 JWT 포트/권한 검증은 유지하고, 구 AWS Authorization 거절 회귀를 추가했다.
- body/event/operation ID·멱등·exact204·Retry-After·TLS/redirect·JWT 규격·recovery-r1/scalar-end-1 및 LC/user JWT 계약은 유지한다. 기존8개 body fixture hash 일치 확인.

## 6. 부록 — 파일·검증·인계

주요 변경 파일(root: `src/main/java/web/tosunsaeng/identity/`):

- 삭제: `global/workload/BillingSigV4JsonTransport.java` 및 대응 테스트 파일.
- 공통/발신: `global/workload/BillingJsonTransport.java`, `BillingJwtJsonTransport.java`, `BillingJwtConfiguration.java`.
- 연결/속성: `domain/user/withdrawalcapture/WithdrawalCaptureConfiguration.java`, `WithdrawalCaptureProperties.java`, `BillingWithdrawalPublisher.java`; owner-event 및 phone-eligibility의 configuration/properties/delivery adapter.
- 설정: `build.gradle`, `src/main/resources/application.yml`, `src/test/resources/application-test.yml`.
- 테스트: 신규 BillingJsonTransportTests/BillingJwtConfigurationTests, 기존 BillingJwtJsonTransportTests/RecoveryJwtVerifierTests 수정. 기존 SigV4 서명 테스트의 body/origin/재시도 역할은 JWT/공통 회귀로 유지한다.

최종 실행: `env -u SENTRY_AUTH_TOKEN ./gradlew clean test` 성공(2089/실패0/오류0/skip24). 구 설정의 main/test YAML 잔존 검사, source의 AWS SDK/SigV4 참조 제거 검사, manifest8개 SHA-256 확인, `git diff --check` 통과. 초기 대상 테스트는 test YAML에 남은 region을 새 guard가 거절해 실패했고, 해당 구 기본 설정을 제거한 뒤 전체를 재실행했다.

이전 dirty 변경은 보존했다. build.gradle의 기존 schema test dependency 및 unrelated cleanup/기존 테스트 변경은 이번 작업에 의해 제거하지 않았다. 이번 예상 밖 변경은 없다.

관련 문서: [Billing 제거 인계서](TMI-203-204-SigV4-removal-Billing-handoff-2026-10-13.md), [이전 JWT 구현](TMI-203-workload-jwt-implementation.md), [현행 계획](TMI-203-withdrawal-recovery-snapshot-feed-plan.md). 과거 SigV4 수락·검증 기록은 보존한다.

Jira 댓글 초안(미등록): Identity–Billing SigV4/전용 credential·의존성 제거, JWT 단일화 및 구설정 기동 오류/활성 목적별 signer 조건 구현. 전체2089 중2065통과/24skip, body hash8개 일치. 실제 활성 구인증 조사·배포 설정 제거·Billing 공동검증은 미완료. Jira 수정/상태 변경·commit/push·배포 없음.
