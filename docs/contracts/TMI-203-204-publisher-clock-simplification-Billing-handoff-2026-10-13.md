# Identity → Billing 인계: 별도 시간 증명 요구 제거·인프라 시간 동기화 전제

> 최신 구현(2026-10-13): 사용자 후속 구현 요청으로 Identity 발신의 RuntimeEvidence/clockTrusted 의존성을 제거했다. [구현·검증 결과](TMI-203-publisher-clock-simplification-implementation.md): 전체 2,096 통과/29 skip/실패·오류 0. 아래 미구현 표현은 인계·수신 당시 이력이며 이 결과로 갱신한다. 구매 보호와 legacy BLOCKED_CLOCK 호환 유지, 실제 배포·공동 연동·운영 자료 재개는 미수행이다.

- 날짜: 2026-10-13 / Identity 브랜치: feat/TMI-203-withdrawal-snapshot-feed-recovery
- 관련: TMI-202·203 / Billing 연계 TMI-204
- 상태: 사용자 방향 수락 후 공동 계약 정합화 요청. 이번은 인계 문서 작성이며 발신 코드 변경·Billing 수락·배포·활성화 완료가 아니다.

> 후속 회신 수신(2026-10-13): 사용자가 전달한 Billing 회신에서 이 좁은 시간 운영 전제 변경의 문서 수락 및 공동 계약 §5.4 개정을 보고했다. 위 상태와 본문의 회신 요청은 발신 당시 기록이다. 현재는 Billing 수락 회신을 수신했으며 Identity 발신 구현·변경 후 공동 검증은 미완료다. Billing 코드·1039개 테스트 성공은 상대 보고이며 이번에 독립 재실행하지 않았다.

### 회신에 따른 Identity 후속 범위

- 발신 구성의 RuntimeEvidence 주입과 publisher/retry의 clockTrusted 인자·증명 부재 차단을 제거하는 변경을 준비한다. 구매에서 사용하는 동일 인터페이스는 별도 범위로 유지한다.
- 기존 BLOCKED_CLOCK 자료와 enum 호환 읽기를 보존한다. 원 event/body·오류·attempt·notBefore를 유지하고 기존 권한 있는 선택 재개 절차를 따른다. 기존 자료 부재를 가정하거나 일괄reset하지 않는다.
- Identity push의20회budget/BLOCKED_RETRY_AFTER·중복header 차단과 Billing pull의backoff/probe 정책을 통일하지 않는다.
- JWT시간규격·exact204/401/403·Retry-After·중복/응답유실·lease 보호를 변경 후 검증한다. 기존 변경 전 테스트를 새 검증 결과로 재사용하지 않는다.
- 이번 회신은 구매준비 source binding 설계 수락이나 실제 배포·판매·자동삭제 승인으로 확대하지 않는다.

## 1. 5줄 결론

1. Identity 탈퇴 발신도 별도 시간 증명 provider를 요구하지 않고 인프라 시간 동기화와 운영 대응을 전제로 하는 방향을 채택한다.
2. 제거 대상은 앱의 추가 clock attestation 의존성과 그 증명 부재만을 이유로 하는 발신 차단이다. 시간 자체를 사용하지 않겠다는 뜻이 아니다.
3. workload JWT 시간 검증, Retry-After, exact204, 멱등성·lease·재시도 한도·구매 보호는 유지한다.
4. API/body/schema/hash/index·보존기간 변경 요청은 없다. 다만 앱의 독립 시계 오류 탐지 보장과 운영 책임은 계약에 명시적으로 개정해야 한다.
5. 현재 발신 코드는 아직 RuntimeEvidence/clockTrusted를 요구한다. 기존 BLOCKED_CLOCK 자료를 일괄 재개하거나 상태를 지우지 않는다.

## 2. 사용자가 반드시 읽어야 하는 내용

### 2.1 변경 배경

정리·저장·복구마다 별도 운영 증명·승인 시스템을 만드는 대신 DB 내부 정합성 검사와 일반 배포/복원 운영으로 단순화하는 방향을 사용자가 수락했다. 이번에는 탈퇴 발신의 별도 시간 증명 요구도 같은 방향으로 정합화하도록 인계를 요청했다.

시간 증명 provider가 없다는 사실은 서버 시간이 실제로 틀렸다는 증거가 아니다. 현행 발신은 provider 부재 시 ON 기동이 실패하고, clockTrusted=false이면 HTTP 발신을 막아 BLOCKED_CLOCK으로 기록한다. 이 추가 의존성을 제거하는 것이 목표다.

### 2.2 유지하는 계약

| 항목 | 유지 내용 |
| --- | --- |
| 인증 | ALB·HTTPS·전용 workload JWT, RS256/kid·issuer/subject/audience·route 권한, 기존 시간 claim 규격 및 만료 검증. 무인증/구SigV4 fallback 없음 |
| 요청 | 기존 탈퇴 eventId·schemaVersion·userId·withdrawnAt 및 body/경로 불변. 재전송 시 원 eventId/body 유지 |
| 성공 | Billing commit 후 exact204만 성공. 다른2xx를 성공으로 바꾸지 않음 |
| 재시도 | 현행 backoff/jitter·generation20회 한도·응답 유실/중복 처리·dispatch marker 유지 |
| 오류 | 401/403 BLOCKED_AUTH, 계약/영구오류 처리, 권한 있는 재개 유지 |
| Retry-After | 429/503 적용, 정수초 및 HTTP-date 지원. backoff 종료와 header 시각 중 늦은 시각 사용. 중복/잘못된 header 및24시간 초과 보류 정책 유지 |
| 경합 | leaseOwner/leaseVersion/resumeGeneration, source 유효성 및 만료 검사 유지 |
| 복구 | snapshot 고정T/H·digest·H2 feed·source range, Billing 로컬 checkpoint/epoch, 원격ACK 없음 |
| 삭제 | 원천120일·snapshot7일·operation120일·폐기stream 종료+120일. 구매 계정 삭제/거래 확인은 별도 |

정확 필드/상태/경계는 기존 계약 원문을 유지한다. 이 표는 새로운 wire 규격이나 Billing pull 재시도 값을 정의하지 않는다.

### 2.3 달라지는 보장

- 인프라의 정상적인 시각 동기화를 운영 전제로 삼는다. 앱이 별도 관측 provider로 절대시각 오차를 독립 탐지·차단한다는 보장은 요구하지 않는다.
- 시각이 크게 틀리면 JWT가 거절되거나 재시도·lease·만료 판단이 어긋날 위험은 남는다. 일반 인프라 모니터링/장애 대응으로 다룬다. 수신 JWT 검사만으로 이 모든 위험이 해결된다고 주장하지 않는다.
- 동기화/이상 대응이 실제 환경에서 확인됐다는 뜻은 아니다. 환경별 운영 확인은 배포 단계에서 수행한다.
- 별도 승인 collection/API·승인 TTL/갱신 worker·clock 관측 서비스의 신규 개발은 요청하지 않는다.

## 3. Billing에 확인·회신을 요청하는 사항

1. 기존 API/DTO·JWT 시간 claim 검증·Retry-After를 유지하면서 외부 clock attestation 요구만 제거하는 운영 전제 변경을 수락할 수 있는지 확인한다.
2. Billing에 별도 clock 증명 provider 또는 그 부재를 이유로 하는 기동/전송 차단이 있는지 실제 코드 기준으로 알려준다. 같은 구조가 있다고 가정하거나 Billing 전체 시간 검사를 일괄 제거하지 않는다.
3. 공동 계약·운영 문서에서 독립 clock 증명 의무로 읽히는 부분을 식별하고 이 범위만 수정한다. 정리 최소화와 별개인 JWT 검증·복원 worker 차단·구매/삭제 보호는 유지한다.
4. 기존 BLOCKED_CLOCK 또는 동등한 영속 상태가 있다면 호환 읽기와 명시 재개 절차를 확인한다. 상태명/enum 삭제·일괄 PENDING 전환·attempt 초기화는 이번 요청이 아니다.
5. Billing 독립 테스트 결과와 미확인 사항을 회신한다. 회신 전 공동 수락/실배포 완료로 기록하지 않는다.

사용자에게 신규 승인 시스템을 선택하게 하려는 요청이 아니다. 양 서버가 실제 코드 영향과 보장 차이를 맞추기 위한 기술 확인이다.

## 4. 주요 위험과 미확인 사항

- 현재 Identity 발신 구현은 아직 변경 전이다. provider 요구 제거·Retry-After의 clock 인자 정합화 및 테스트 수정이 후속 작업이다. 빈 true adapter로 기존 증명이 충족된 것처럼 처리하지 않는다.
- 기존 RuntimeEvidence는 구매 활성화에도 쓰인다. 발신 단순화를 이유로 구매 활성화 보호를 제거하거나 구매 scope를 자동 허용하지 않는다.
- 기존202 문서의 clock 오차60초 초과 차단/관측 연결 요구는 이번 변경 영향 대상이다. JWT의 별도 skew/TTL 기준과 혼동하지 않는다. 해당 발신 요구를 어떻게 대체했는지 명시적으로 남긴다.
- 실제 환경의 blocked 자료·인프라 동기화·ALB/TLS·키 교체·Billing 연동 상태는 이번에 조회하지 않았다.
- 실제 판매·구매 계정 자동삭제·capture OFF·보존기간 연장·운영 재개 승인으로 확대하지 않는다.

## 5. Identity 현재 구현과 후속 순서

### 확인된 로컬 구현

- 정리 jobs: 별도 Approval/runtime 주입 대신 config/scope·DB preflight, 보존/lease/CAS/삭제증거 원자성·참조 보호 유지.
- capture/snapshot/feed: CaptureReadiness로 단일ACTIVE·환경·cutover·순번 검사. 외부RuntimeEvidence와 분리됐지만 절대시각 신뢰 증명은 하지 않는다.
- 탈퇴 발신: 여전히 RuntimeEvidence.clockTrusted를 요구. 구매 활성화는 기존 증명 부재 시 거절.
- 발신 구성4회귀 추가: OFF 무의존, ON 증명부재 실패, 합성 증명/JWT 정상 기동, 잘못된origin/audience 실패. 실제 외부 HTTP 전송 없음.

### 권장 적용 순서

공동 운영 전제 수락 → Identity 발신의 시간 증명 의존성/테스트 정합화 → Billing 영향이 있을 경우 해당 코드/문서 정합화 → 양방향 JWT·204/오류/Retry-After/중복/응답유실 검증 → 기존blocked 자료 확인 및 필요한 건만 승인 재개 → 별도 배포·활성화 승인.

필수 회귀는 별도 증명 없는 정상 구성, JWT 만료/미래시각 거절 유지, 정수초/HTTP-date·과거/중복/긴 Retry-After, 인증 오류 및 unknown commit·중복·lease 보호다. 기존 BLOCKED_CLOCK 자료가 없어졌다고 가정하지 않는다.

## 6. 부록 — 근거·검증 범위

- [발신 구성 확인](TMI-203-publisher-configuration-check.md)
- [로컬 준비 조건 분리](TMI-203-local-readiness-implementation.md)
- [최소 정리 구현](TMI-203-minimal-cleanup-implementation.md)
- [TMI-203 현행 계획](TMI-203-withdrawal-recovery-snapshot-feed-plan.md)
- [TMI-202 계획](TMI-202-withdrawal-capture-billing-delivery-plan.md): Retry-After/clock 관측 항목은 이번 후속 개정 대상.
- 코드: `WithdrawalCaptureConfiguration.PublisherConfiguration`, `BillingWithdrawalPublisher`, `BillingWithdrawalRetry`; 발신 경로 `/internal/v1/payments/accounts/withdrawal/events`.
- 직전 실행: 전체2120개/통과2091/실패0/오류0/skip29(Mongo23/support6). 이번 문서 작성에서는 Gradle 재실행 없음. 해당 결과는 변경 후 발신 단순화 검증이 아니다.
- 이번은 문서와 작업 기록만 변경. Billing 파일/코드·Jira·외부 메시지·실제 데이터·배포·commit/push 변경 없음.
