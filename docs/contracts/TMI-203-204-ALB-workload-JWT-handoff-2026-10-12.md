# Billing 인계 — 기존 ALB 재사용·서버 전용 JWT 인증으로 전환

- 작성일: 2026-10-12
- Identity 브랜치: `feat/TMI-203-withdrawal-snapshot-feed-recovery`
- 대상: TMI-203·204, 영향 검토 TMI-199·202·206 및 공통 Billing 이벤트 전송부
- 상태: **사용자가 기존 ALB + 서버 전용 JWT 방향을 승인했다. 아래 인증 세부값은 공동 검토안이며 구현·배포·권한 변경·판매·자동 삭제 활성화 승인은 아니다.**

## 1. 5줄 결론

1. Identity–Billing 통신은 신규 Lattice 도입 대신 **기존 ALB의 HTTPS 경로와 서버 전용 workload JWT**를 사용하는 방향으로 변경한다.
2. ALB는 요청 전달을 담당하고 수신 서버가 JWT와 서비스별 권한을 검증한다. ALB 자체가 Lattice SigV4 인증을 대신하는 것은 아니다.
3. `recovery-r1 / scalar-end-1`의 API 경로·본문·오류·snapshot/feed·보존 계약은 유지하고, 서버 인증 규격과 관련 테스트를 별도로 개정한다.
4. Identity→Billing 이벤트와 Billing→Identity 복구 호출 모두 점검한다. 사용자 로그인 JWT나 기존 LC 전용 토큰을 그대로 허용하지 않는다.
5. Billing은 실제 배포/클라이언트 영향, 양방향 발급·검증 방식, 환경별 주소·권한·키 운영안에 대해 회신해 달라. 공동 확정 전 기능은 OFF로 유지한다.

## 2. 사용자가 반드시 읽어야 하는 내용

### 2.1 변경 근거: 확인한 사실과 한계

2026-10-12 서울 리전 AWS ECS의 아래 두 서비스 구성/네트워킹 화면을 읽기 전용 확인했다.

| 서비스 | 확인한 설정 |
| --- | --- |
| `tosunsaeng-identity-test-service` | VPC Lattice 미구성, `tosunsaeng-staging-alb` 연결, task revision 42 |
| `tosunsaeng-identity-service` | VPC Lattice 미구성, 동일 ALB 연결, task revision 23 |

이는 **현재 해당 ECS 서비스의 통합 구성**에 대한 확인이다. 계정 전체 Lattice 부재, 별도 수동 네트워크 연결, 다른 서비스의 사용 여부나 과거 이력을 증명하지 않는다. Billing 배포 상태도 이번에 조회하지 않았다.

Lattice 전체 서비스 조회는 조직 SCP의 `vpc-lattice:ListServices` explicit deny로 실패했다. 거부를 우회하거나 권한을 변경하지 않았다. 코드에 Lattice 서명 전송부가 있다는 사실만으로 기존 운영 사용을 가정하지 않는다.

### 2.2 승인된 변경과 승인되지 않은 실행

승인된 것은 **기존 ALB 재사용 + workload JWT라는 인증 방향**이다. Lattice를 신규 도입해야 한다는 전제를 전환하되, 기존 exact 인증 문구의 수정본·JWT 세부값은 양 서버가 함께 확정한다.

이 문서만으로 다음을 실행하지 않는다.

- AWS listener/보안 그룹/IAM/Secret/JWKS 공개 범위 변경
- 운영 API 활성화·기존 인증 즉시 제거·무인증 fallback
- 사용자 JWT audience 확대 또는 LC workload 토큰 권한 확대
- 기존 fixture/hash 및 과거 테스트 수락 기록의 소급 변경

### 2.3 유지할 복구·이벤트 계약

- 네 POST: `/internal/v1/billing/withdrawals/snapshots`, `/snapshots/status`, `/snapshots/page`, `/feed/page` (뒤 세 경로도 같은 prefix).
- snapshot 고정 T/H, count·digest·만료, 서명 cursor·고정 H2·연속 feed, strict raw JSON/16KiB/페이지 한도.
- Identity source `replayFloor`/중간 range 및 원본 삭제와 증거 기록의 원자성.
- 원격 ACK 없음. Billing 로컬 checkpoint/epoch/적용 transaction/복원 격리 유지.
- snapshot 부재로 기존 WITHDRAWN을 해제하지 않으며 과거 원event 완전성을 주장하지 않는다.
- TMI-202 탈퇴 push exact204, 같은 eventId 재시도·멱등 처리·인증 오류 중단/재개 의미 유지.
- source120일/snapshot자료7일/최소operation120일 등 현행 보존과 삭제 안전성 조건 유지.
- 구매 권한 발급·탈퇴 fence, 기존 사용자 로그인/LC 계약은 변경하지 않는다.

## 3. 결정·회신이 필요한 사항

새 제품 선택보다는 다음 기술안의 양 서버 공동 확정이 필요하다. **표의 값은 제안이며 확정값이 아니다.**

| 항목 | 권장안 / Billing 회신 요청 |
| --- | --- |
| 발급 주체 | 각 호출 서버가 자기 workload 전용 비대칭 키로 발급하고 상대는 공개키만 검증. Billing의 발급기/키 운영 구현 가능 여부 확인 |
| 알고리즘 | RS256만 허용, kid 필수. 사용자 access-token 키·support HMAC·cursor HMAC과 분리 |
| issuer | 환경별 고정 issuer를 양측 등록. 실제 문자열과 JWKS 주소는 인프라 확인 후 동결 |
| Identity→Billing | sub=`identity-service`; 이벤트 목적별 audience·허용 route를 분리. 정확 audience 문자열은 공동 명세에 확정 |
| Billing→Identity | sub=`billing-service`; 복구 전용 audience, 위 네 POST만 허용. 서명 검증 후 환경/consumer를 고정 매핑 |
| 수명 | 제안: 최대60초, 허용 clock skew30초. `iat`/`exp` 필수, 최대수명·미래발급 검사, `nbf`가 있으면 검사. clock 근거·호출 지연과 함께 수락 여부 회신 |
| 키 조회·교체 | 사전 등록한 HTTPS JWKS 또는 배포된 public key만 사용. token의 임의 jku/x5u/issuer로 URL 선택 금지. cache/timeout/unknown-kid 갱신 제한·폐기키 정책·전파 겹침기간 공동 확정 |
| 헤더 | `Authorization: Bearer …` 단일 헤더. 서비스 환경/consumer를 요청 body나 임의 forwarding 헤더에서 신뢰하지 않음 |
| 주소/네트워크 | 검증된 ALB HTTPS origin에만 전송, redirect 추종 금지. Billing의 실제 ALB origin은 미확인. 직접 task 접근 및 공개 ALB internal 경로 노출 정책 확인 |
| 오류 | 인증 불가401/권한 불일치403을 구분하고 무한 재시도 금지. 인프라401/403/429는 recovery JSON 필수 아님. 기존202/204 중단·재개 계약 유지 |

Identity가 Billing의 private key를 보유하거나 전달받는 안은 권장하지 않는다. Billing이 자체 발급을 지원하기 어렵다면 신뢰 가능한 workload 발급 방식부터 공동 설계하며, 사용자 로그인/refresh-token 흐름을 대체 발급 수단으로 쓰지 않는다.

Billing 회신에 포함할 내용:

1. 현재 실행 중인 SigV4 client/server, 활성 플래그·이미 배포된 대상 유무. 없다면 최초 적용으로 처리할 수 있는지.
2. 양방향 issuer/audience/subject/route 표, 최대 TTL/skew, key 발급·JWKS 제공/캐시/rotation 책임.
3. 환경별 HTTPS origin과 ALB→task 접근 제한안. 실제 키나 토큰은 회신에 포함하지 않는다.
4. 아래 영향 route 전부 또는 일부에 대한 수정 범위와 PR 순서.
5. 이 문서의 방향 수락 또는 변경이 필요한 항목. 그 후 공동 계약/TMI-199 인증 보완 및 관련 Jira 개정 내용을 확정한다.

## 4. 주요 위험과 미확인 사항

### 4.1 SigV4와 Bearer JWT는 보장이 완전히 같지 않다

SigV4의 요청 서명과 달리 일반 Bearer JWT는 HTTP method/path/body를 암호학적으로 묶지 않는다. 짧은 수명·TLS·route 권한·키 관리·재시도 멱등성은 필요하지만 같은 보장을 제공한다고 표현하지 않는다. JWT의 jti 존재만으로 재전송 방지가 되는 것도 아니다.

추천 초기안은 **TLS + 짧은 workload JWT + 수신 route 권한 + 기존 멱등 처리**다. 요청 본문 서명까지 필수인지 공동 보안 검토에서 명시적으로 결정한다. 필요하다면 별도 요청 바인딩 규격/테스트를 합의하며 검증 공백을 임의로 허용하지 않는다.

### 4.2 ALB·경로 이름만으로 내부 API가 되지 않는다

공개 ALB에서 `/internal/...` 경로를 제공하더라도 인증은 서버에서 항상 수행한다. task 직접 접근은 보안 그룹 등으로 제한하고, 헤더로 프록시 경유를 주장하는 요청은 신뢰하지 않는다. 공개 노출이 허용되지 않는 경우 내부 ALB 등 별도 경로의 비용/구성을 검토한다. 기존 ALB 사용 방향이 공개 접근 확대 승인은 아니다.

### 4.3 이행과 장애

Billing의 실제 미배포를 가정하지 않는다. 기존 client가 존재하면 명시 이행 계획을 세운다. JWT 실패 시 SigV4/무인증으로 자동 fallback하지 않는다. 키 발급·JWKS·clock 장애, 인증 플래그 OFF에 대한 fail-closed와 관측 기준을 검증한다. 키 cache가 정상인 경우에도 무제한 과거 키 신뢰는 허용하지 않는다.

## 5. 양 서버 작업 분담·권장 순서

### Identity

- 기존 `BillingSigV4JsonTransport` 의존 이벤트 전송부를 JWT 전송부로 전환하는 후보를 준비한다. 전역 치환하지 않고 이벤트별 상태/재시도 보존을 확인한다.
- 현재 `RecoveryIngressVerifier`는 IAM/SigV4 헤더·payload 검증을 전제한 interface다. JWT 방향 수락 후 해당 포트/주석/HTTP 검증 흐름을 개정한다. payload 검증 함수를 단순 no-op으로 바꿔 동일 보장을 주장하지 않는다.
- 별도 recovery security chain, default OFF, 사용자JWT 격리, 인증 뒤 strict decode, 고정 오류 비로깅은 유지한다.
- workload 발급 목적·audience와 검증기를 추가하되 기존 `WorkloadIdentityPurpose`의 LC 두 목적을 재사용/확장해 우회 허용하지 않는다.

### Billing

- Identity 이벤트의 전용 JWT 수신 검증/route 권한 및 Billing 복구 호출의 JWT 발급·HTTPS 전송을 구현한다.
- 기존 SigV4 오류 분류와 새401/403을 기존 중단/재개 정책에 연결한다. 동일 eventId/operationId/body 재시도를 유지한다.
- 실제 endpoint·키 운영·배포 순서를 확인하고 Identity와 인증 fixture를 공동 검증한다.

### 순서

1. 인증 exact 제안 공동 수락 및 영향 route/client 확인.
2. 인증 문서/fixture를 별도 revision으로 추가. 기존 recovery body fixture/hash는 그대로 유지.
3. 양 서버 OFF 구현·단위/HTTP 회귀. 사용자JWT·LC토큰·다른환경·다른audience·wrongsub·만료/미래iat·alg/kid·위조·중복헤더 거절 테스트.
4. staging HTTPS/ALB 경로·직접 task 접근 제한, 키 교체/조회 장애, 응답 유실/중복/인증 중단과 재개 검증.
5. Billing 공동 복원·삭제 안전성 gate와 별도로 기능별 활성화 승인. 실제 판매/자동삭제 승인은 별도 유지.

## 6. 부록 — 영향 범위와 근거

| 방향 | 현재 코드상 경로/기능 | 이번 인계의 영향 |
| --- | --- | --- |
| Identity→Billing | `/internal/v1/payments/accounts/withdrawal/events` | TMI-202 전송/수신 인증 변경,exact204/event body/재시도 유지 |
| Identity→Billing | `/internal/v1/owners/merge/events` | 공통 SigV4 전송부 사용; 실제 채택 범위·배포 상태 Billing 확인 필요 |
| Identity→Billing | `/internal/v1/eligibility/trial/owner/events` | 동일,다른 기능을 암묵적으로 활성화하지 않음 |
| Billing→Identity | recovery 네 POST | issuer·service권한 검증 추가 및 ingress 포트 변경 |
| Identity→LC | 기존 탈퇴/병합 workload JWT | 현 계약 변경 대상 아님 |

근거 파일:

- [기존 통합 인계](TMI-203-Billing-consolidated-handoff-2026-10-12.md)
- [recovery exact](TMI-203-recovery-r1-exact.md), [공유 fixture](../../src/test/resources/contracts/payment-lifecycle/recovery-r1/README.md)
- [HTTP adapter 구현 상태](TMI-203-http-ingress-implementation.md), [runtime·AWS 조회 결과](TMI-203-runtime-jobs-and-ingress-check.md)
- [Billing 서명 전송부](../../src/main/java/web/tosunsaeng/identity/global/workload/BillingSigV4JsonTransport.java)
- [기존 workload 목적](../../src/main/java/web/tosunsaeng/identity/global/workload/WorkloadIdentityPurpose.java)
- [Billing owner-event 경로](../../src/main/java/web/tosunsaeng/identity/domain/auth/ownerevent/infrastructure/BillingOwnerEventDeliveryAdapter.java)

기존 문서의 과거 구현/수락 상태는 당시 기록이다. 이 문서는 인증 방향 변경 인계이며 과거 검증 이력을 삭제하거나 데이터 계약을 자동 대체하지 않는다. 문서만 작성했고 코드·AWS·Jira·배포·Secret은 변경하지 않았다.
