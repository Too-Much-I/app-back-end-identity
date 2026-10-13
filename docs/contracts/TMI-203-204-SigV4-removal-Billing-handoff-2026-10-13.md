# Billing 인계 — Identity–Billing SigV4 제거·JWT 단일화

- 작성일: 2026-10-13 / Identity 브랜치: `feat/TMI-203-withdrawal-snapshot-feed-recovery`
- 대상: TMI-203·204, 영향 확인 TMI-202 및 Billing 이벤트 전송 공통부
- 상태: 사용자 요청에 따른 제거 방향 인계. **SigV4 삭제 구현·실배포·기능 활성화 완료가 아니다.** Billing 측 제거 범위와 이행안에 대한 회신을 요청한다.

## 1. 5줄 결론

1. Identity–Billing 인증은 기존 ALB + 전용 workload JWT 하나로 정리하고, 이 통신에 사용하던 SigV4 선택 경로를 제거하는 방향이다.
2. 인증 방식 선택과 기능 ON/OFF를 분리한다. JWT를 끈다고 SigV4가 선택되는 구조는 없애되 publisher·복구 기능 ON/OFF는 유지한다.
3. 전송 방식만 변경한다. event body·멱등성·응답·재시도·snapshot/feed·보존·삭제 안전성은 그대로 유지한다.
4. 실제 활성 SigV4 client/receiver는 아직 확인하지 않았다. 미사용이면 최초 JWT 적용, 사용 중이면 발송 중지·미처리 기록 보존을 포함한 명시적 전환이 필요하다.
5. Billing은 기존 IAM 신뢰 모드와 SDK·설정 사용처를 확인하고 JWT 단일화·실패 시 차단·배포 순서를 회신해 달라. 계정 전체 IAM이나 다른 AWS 서비스 인증을 제거하는 요청은 아니다.

## 2. 사용자가 반드시 읽어야 하는 내용

### 현재 확인된 Identity 상태

[JWT 구현 결과](TMI-203-workload-jwt-implementation.md)에 따라 수신 검증·전용 HTTPS connector·발신 이벤트4종의 JWT 모드는 로컬 구현됐다. 다만 현재 `BILLING_WORKLOAD_JWT_ENABLED=false`는 **발송 중지**가 아니라 **기존 SigV4 transport 선택**을 의미한다. 실제 발송 여부는 각 publisher 플래그가 결정한다.

현재 JWT 코드에서도 결과·예외·Retry-After 처리를 `BillingSigV4JsonTransport`의 타입/함수로 참조한다. 따라서 클래스만 지우면 안 되며, 인증과 무관한 동작을 공통 transport로 먼저 옮겨야 한다.

이 문서는 Billing 수신 구현이나 실환경을 독립 검증한 결과가 아니다. 이전 Billing 회신의 Lattice/IAM 신뢰 모드에 대한 설명도 상대 보고다. **기존 permitAll 경로를 ALB로 그대로 노출해서는 안 된다.**

### 제거 이후 목표 동작

| 상태 | 목표 동작 |
| --- | --- |
| Billing 발신 기능 모두 OFF | 발신 signing key 없이 기동 가능, 새 전송 없음 |
| Billing 발신 기능 하나 이상 ON | JWT만 사용. 필요한 키·issuer·목적별 audience·HTTPS origin 누락/오류 시 기동 실패 |
| 복구 HTTP OFF | 복구 경로 접근 거절, 복구 수신용 trust/TLS 설정 없이 기동 가능 |
| 복구 HTTP ON | 수신 공개키·권한·포트/TLS 및 runtime 필수 설정 검증. 오류 시 기동 실패 |
| 실행 중 서명·검증·인증 장애 | 무인증/SigV4로 전환하지 않음. 기존 오류 분류와 중단·재개 정책 적용 |

발신 OFF여도 다른 ON 기능에 필요한 설정은 별도로 검증한다. 기능 OFF는 durable capture를 끄거나 미전송 event를 삭제한다는 뜻이 아니다.

## 3. Billing에 확인·회신을 요청하는 사항

1. 현재 SigV4로 실행 중인 recovery client 또는 Identity 이벤트 receiver가 있는가? 환경별 코드/설정/배포 증거를 확인하되 키나 토큰을 회신에 포함하지 않는다.
2. Identity–Billing 범위의 `LATTICE_AWS_IAM` 등 신뢰 모드, IAM 의존 permitAll, 인증 선택 분기를 제거하고 JWT만 검증할 수 있는가?
3. 관련 AWS SDK/credential/region/service-name 설정이 다른 AWS 기능에도 사용되는가? 다른 사용처가 있으면 유지하고 이 통신 전용 설정만 제거한다.
4. 발신/수신 기능별 OFF 기동과 ON 설정 오류 기동 실패, 자동 fallback 부재를 어떤 테스트로 확인할 것인가?
5. 양방향 issuer/audience·환경 매핑, origin/port/TLS/health check, 공개키 선배포 및 전환 순서를 확인해 달라.
6. 신규 [workload-jwt-r1 fixture](../../src/test/resources/contracts/payment-lifecycle/workload-jwt-r1/README.md)와 시간 경계를 동기화·독립 실행했는지 회신해 달라.

새 제품 정책 변경보다는 기술 정리와 배포 호환성 확인 요청이다. Billing 저장소 문서/ADR/Jira 변경은 해당 담당이 승인 절차에 따라 진행한다. 이 인계서로 상대 저장소 규칙을 자동 폐기하지 않는다.

## 4. 주요 위험과 미확인 사항

- 기존 SigV4 기본 선택을 삭제하면 publisher ON인 환경의 기동 요구가 바뀐다. 기존 플래그가 남아 있어도 무시하지 말고 배포 설정에서 명시적으로 정리해야 한다.
- JWT 검증 실패 시 무인증이나 기존 IAM 신뢰 경로로 통과할 수 없어야 한다. 단순히 SigV4 signer만 삭제하는 것으로 수신 보안이 완성되지 않는다.
- 이미 발급된 Bearer JWT의 유효시간 내 재사용은 가능하다. jti는 replay 방지 DB가 아니며 method/path/body 서명은 제공하지 않는다.
- 전환 중 구버전 replica/worker의 발송과 claim·lease를 고려해야 한다. delivery 상태 초기화나 임의 완료 처리로 전환하지 않는다.
- 롤백은 호환된 설정/버전으로 명시적으로 진행한다. 인증 실패에 따른 자동 모드 전환은 롤백이 아니다. 필요하면 발신을 중지하고 durable 기록을 보존한다.
- 실제 ALB/SG/TLS 및 Billing 연동, 키 교체/복원 검증은 남아 있다. 본 인계로 판매·자동삭제를 허용하지 않는다.

## 5. 제거 범위·유지 계약·진행 순서

### Identity 후속 제거 범위

- `BillingSigV4JsonTransport`의 공통 결과/예외/Retry-After를 `BillingJsonTransport` 또는 별도 중립 타입으로 이동한다. 인증 종류와 무관한 기존 재시도 의미는 유지한다.
- SigV4 구현·전용 AWS credential bean·이벤트 adapter의 SigV4 생성자/선택 분기를 제거한다.
- `BILLING_WORKLOAD_JWT_ENABLED`를 인증 선택 용도로 사용하는 구조를 제거하고 활성 publisher에 필요한 JWT factory를 조건부 생성한다. 다른 publisher 활성 플래그는 유지한다.
- 발신 ON일 때 더 이상 의미 없는 SigV4 region 필수 검증을 정리한다. AWS SDK 명시 의존성은 저장소 전체 사용처 확인 후 불필요한 것만 제거한다. 다른 라이브러리의 AWS 사용까지 금지하지 않는다.
- 관련 테스트/설정 설명을 개정한다. 과거 SigV4 검증 기록과 수락 이력은 삭제·소급 수정하지 않는다.

### 변경하지 않는 계약

- Identity→Billing 네 이벤트: `/internal/v1/payments/accounts/withdrawal/events`, `/internal/v1/eligibility/trial/events`, `/internal/v1/eligibility/trial/owner/events`, `/internal/v1/owners/merge/events`.
- Billing→Identity 네 POST: `/internal/v1/billing/withdrawals` 아래 `/snapshots`, `/snapshots/status`, `/snapshots/page`, `/feed/page`.
- 기존 event/operation ID·본문·strict 입력·멱등 처리, 탈퇴 push exact204, Retry-After와 인증 오류 중단·재개. 각 이벤트의 기존 성공 응답 정책을 무조건204로 일괄 바꾸지 않는다.
- `recovery-r1/scalar-end-1` body/schema/hash, 고정 T/H·digest/H2·cursor·source availability·Billing 로컬 checkpoint/epoch와 commit 경계.
- source/snapshot 보존과 삭제 안전성, 사용자/LC 인증. AWS Secrets Manager·ECS 역할 등 별도 운영 인증은 변경 대상이 아니다.

### 권장 진행 순서

1. 양측 실제 사용처와 활성 설정을 확인하고 제거 범위·이행 순서를 맞춘다.
2. 공통 타입 분리 → JWT 단일 경로 및 OFF/ON 조건 정리 → SigV4 전용 코드/설정/의존성 제거.
3. 기존 재시도/멱등/본문·응답 회귀, 키 누락/손상·wrong purpose·구인증 거절·포트 위조·키 교체를 로컬 검증한다.
4. 수신 trust와 네트워크를 먼저 준비한다. 사용 중인 구 sender가 있다면 새 claim/발신 중지 및 진행 중 요청 정리 후 버전 전환하고, 미처리 기록을 유지한 채 JWT로 재개한다. source capture와 LC 전달을 함께 끄지 않는다.
5. 양방향 실환경 인증·중복/응답 유실·인증 중단과 재개를 검증하고 승인된 기능만 활성화한다.

## 6. 부록 — 근거·검증 범위

- [JWT 구현 및 설정표](TMI-203-workload-jwt-implementation.md), [인증 세부 규격](TMI-203-204-workload-jwt-exact-candidate.md), [현행 계획](TMI-203-withdrawal-recovery-snapshot-feed-plan.md).
- [공통 transport](../../src/main/java/web/tosunsaeng/identity/global/workload/BillingJsonTransport.java), [기존 SigV4](../../src/main/java/web/tosunsaeng/identity/global/workload/BillingSigV4JsonTransport.java), [JWT 설정](../../src/main/java/web/tosunsaeng/identity/global/workload/BillingJwtConfiguration.java).
- 이전 로컬 구현 검증: 2026-10-12 전체2073개 중2049통과/24 opt-in skip/실패0. **SigV4 제거 후 테스트 결과가 아니며 이번 문서 작성에서 재실행하지 않았다.**
- 이번 작업은 인계 문서와 작업 기록만 작성했다. SigV4 삭제·AWS 변경·Jira 쓰기·외부 전송·commit/push·배포는 하지 않았다.
- Jira 댓글 초안(미등록): SigV4 제거/JWT 단일화 인계서 작성. 공통 타입 분리·기능 ON/OFF 보존·양측 활성 사용처와 배포 전환 확인 요청. 문서 정적 검사만 수행, 제거 구현/실연동은 미완료.
