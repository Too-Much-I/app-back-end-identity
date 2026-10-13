# TMI-202 구현 계획 — 탈퇴 journal·Billing 독립 전송·재개

- 작성일: 2026-10-12
- Jira: [TMI-202](https://to-teacher.atlassian.net/browse/TMI-202), `[Identity I2] 탈퇴 journal·Billing 독립 delivery·SigV4 재시도`
- 조사 브랜치: `develop`. 현재 checkout을 유지하며 계획만 작성한다. 구현 착수 시 `codex/TMI-202-withdrawal-capture-delivery` 등 이슈 키 포함 브랜치를 사용한다.
- 상태: 아래는 승인 당시 계획이다. 2026-10-12 OFF 구현과 로컬 검증을 진행했으며 실제 구현/미완료 사항은 [구현 결과](TMI-202-implementation-result.md) 참조. 실제 DB 이관·IAM 변경·배포·전송·cleanup 활성화는 미수행.
- 기준: [TMI-199 exact 수락안](billing-c0-exact-wire-index-proposal-2026-10-07.md) §5~8, [TMI-200 구현 결과](TMI-200-implementation-result.md), Billing 저장소 [공동 기술 계약](../../../billing/docs/contracts/IDENTITY-BILLING-PAYMENT-LIFECYCLE-TECHNICAL-CONTRACT.md) §5.2~5.7, [탈퇴 수신 계약](../../../billing/docs/contracts/PLAN-009-withdrawal-reconciliation-and-purge-contract.md).

## 1. 5줄 결론

1. 탈퇴와 동시에 순번·최소 사실·Billing 전송 대상을 **하나의 Mongo 트랜잭션**에 남긴다. 일부만 저장되는 탈퇴는 허용하지 않는다(§5.2).
2. Billing 발송을 꺼도 기록은 계속 남기고, 기존 LC outbox·JWT·cleanup은 별개로 유지한다(§5.1, §5.7).
3. Billing은 SigV4로 전송하며 **204만 성공**이다. 일시 오류는 최대20회 자동 재시도, 인증 오류는 중단 후 권한 있는 재개로 처리한다(§5.3~5.4).
4. 기록은 capturedAt 기준120일이며, 미전송이라는 이유로 영구 보존하지 않는다. 실제 정리는 TMI-203의 복구 적용 증거와 연결한 뒤 활성화한다(§5.5).
5. Mock 테스트와 실제 다중노드 Mongo 장애 검증을 구분한다. TMI-202 구현만으로 구매 ON·판매·자동삭제가 가능해지는 것은 아니다(§5.6~5.8).

## 2. 사용자가 반드시 읽어야 하는 내용

### 2.1 사용자 관점의 동작

회원이 탈퇴하면 Identity는 계정을 탈퇴 처리하면서 “이 회원이 언제 탈퇴했는지”와 Billing에 보낼 작업을 함께 저장한다. Billing이 잠시 꺼져 있어도 탈퇴 기록은 남는다. 이후 Billing에 전달되면 Billing이 해당 구매 계정을 차단한다. 기존 LC에도 지금 방식대로 전달한다.

전송 장애와 저장 장애는 다르다. **Billing HTTP 장애는 탈퇴를 되돌리지 않지만, 필수 탈퇴 기록을 DB에 저장하지 못하면 탈퇴 전체가 실패해야 한다.** 전송은 DB commit 이후 worker가 수행하며 HTTP 호출을 탈퇴 트랜잭션에 넣지 않는다.

### 2.2 범위와 비범위

이번 범위는 Identity의 탈퇴 원천 저장·Billing 전송·오류 분류·재개·보존 정리 기반이다. Billing 구매 계정 차단/거래 조회/15일 후 조건부 삭제는 Billing 책임이다. snapshot/feed/복구 세대·ACK API 전체는 TMI-203, 실제 공동 운영 검증은 TMI-206으로 분리한다. 시험·채점·결제 처리 코드를 Identity에 추가하지 않는다.

원천120일은 탈퇴 사용자 구매 계정을120일 유지한다는 뜻이 아니다. Billing의 `withdrawnAt+15일` 조건부 삭제, snapshot 최대7일, 담당자 최초 검토7일은 서로 다른 정책이다. 기존 User tombstone 보존도 이번 작업에서 바꾸지 않는다.

### 2.3 TMI-200과의 연결

TMI-200은 토큰 발급과 탈퇴가 동일 control을 쓰도록 만들었다. TMI-202는 그 **기존 탈퇴 트랜잭션 안에** journal/counter/delivery를 추가한다. 별도 트랜잭션이나 비동기 listener에서 journal을 뒤늦게 저장하면 안 된다.

`PurchaseReadiness`에 capture 준비 증거를 연결하되, capture ON 하나만으로 true를 반환하지 않는다. control 이관·모든 writer 호환·cutover·내구성·공동 gate 증거가 부족하면 구매는 계속 OFF다.

## 3. 사용자가 결정해야 하는 사항

- 제품 정책은 추가 결정 없이 기존 승인값을 따른다. 구현 요청 후 아래 순서대로 개발한다.
- 실제 writer barrier 시간, 격리 다중노드 Mongo 실험 환경, cutover 실행 및 환경별 publisher/cleanup ON은 측정·검증 후 별도 승인한다.
- Lattice/IAM이 지원되지 않는 경우 unsigned/public/JWT fallback을 임의 도입하지 않고 대체 계약 승인을 요청한다.
- 원천120일을 넘는 금융·분쟁 case 보존은 최소 필드와 종료일을 별도 승인받는다. 실패 횟수 reset이나 원문 영구 보존으로 대체하지 않는다.
- 운영 재개는 **별도 권한을 가진 제한된 내부 실행기**를 우선 권장한다. 일반 앱 API나 대량 reset 화면은 이번 범위가 아니다. 신규 HTTP 관리 route가 필요하면 인증·감사·멱등 계약을 먼저 별도 검토한다.
- Jira 상태·댓글은 자동 변경하지 않는다. PR 병합 확인 전 Done 처리하지 않는다.

## 4. 주요 위험과 미확인 사항

1. **실제 Mongo 증거:** 현재 TMI-200 회귀 통과는 실제 failover/성공 commit 응답유실 검증이 아니다. TMI-202 완료 근거에 disposable 다중노드 replica-set 결과를 별도 남겨야 한다.
2. **global counter 병목:** 단일 stream counter가 탈퇴를 직렬화한다. 처리량·p95/p99·write conflict 및 transaction retry를 측정한다. 부적합하면 partition/vector watermark를 계약 변경으로 검토하며 몰래 sequence 예약 방식을 도입하지 않는다.
3. **commit 불명:** 오류를 rollback으로 간주해 새 eventId·순번을 발급하면 안 된다. 기존 트랜잭션 결과와 동일 탈퇴 사실로 수렴해야 한다.
4. **기존 transport 제한:** 현재 `BillingSigV4JsonTransport`는 Retry-After를 정수1~300초로 축약한다. HTTP-date·긴 값·중복 헤더 증거가 소실되므로 전용 결과 타입이 필요하다.
5. **LC 정책 오염:** 기존 LC publisher는 모든2xx를 성공 처리한다. Billing204-only를 공유 publisher에 일괄 적용하면 기존 계약이 달라진다.
6. **만료/복원 경합:** cleanup과 새로운 consumer recovery generation이 경쟁할 때 옛 ACK를 근거로 사용자 연결을 삭제하지 않도록 동일 control CAS를 설계해야 한다. I3 연결 전 실제 cleanup은 OFF다.
7. **cutover 이전 이력:** 기존 outbox가 이미 지워졌다면 원 eventId를 복구했다고 주장할 수 없다. 남은 tombstone은 I3의 별도 snapshot 사실이며 가짜 원 이벤트를 만들지 않는다.
8. **readiness·rollback:** DB marker만으로 capture 미지원 구프로세스의 쓰기를 차단할 수 없다. 배포·DB 접근 통제와 writer drain 증거가 함께 필요하다.
9. **보존 시각:** capturedAt은 commit timestamp가 아니다. 관측30초 budget/clock 오차60초를 증명하지 못한 건은 cleanup 차단·담당 확인 대상으로 분리하되 무기한 보존 승인으로 해석하지 않는다.

## 5. 구현 계획

### 5.1 모델과 인덱스

아래 collection은 신규 additive 모델이다. 기존 `user_withdrawn_outbox`를 이동/삭제하지 않는다.

| 모델 | 역할·필드 |
| --- | --- |
| `withdrawal_stream` | 환경별 incarnation `streamId`, `lastSequence`, `captureCutover`, `replayFloor`, `unavailableIntervals`, `status`. 정상 failover에서는 유지, 복원은 새 stream |
| `withdrawal_journal` | 불변 최소 사실 `streamId,sequence,eventId,userId,withdrawnAt,capturedAt`. schemaVersion1 고정. payload raw JSON·전화번호·SNS·구매 ref·인증정보 저장 금지 |
| `withdrawal_deliveries` | `eventId,destination,status,resumeGeneration,lifetimeAttemptCount,generationAttemptCount,notBefore,nextAttemptAt,leaseOwner,leaseUntil,leaseVersion,lastFailureCode,deliveredAt,reviewDueAt`. 본문은 journal에서 조회 |

`userId` 자체도 보호 대상 식별자다. 최소 사실 저장은 허용 범위지만 로그·메트릭 label·일반 오류 응답에는 기록하지 않는다. delivery와 dispatch marker에도 본문을 복제하지 않는다.

순번·version/count는 BSON int64/Java long, 증가 전 overflow 차단. sequence는1부터, 초기 lastSequence는0. 시간은 기존 계약의 UTC Instant, 신규 withdrawnAt은 영속 밀리초 값 하나를 공유한다. 이전 이벤트 정밀도는 보존한다. UUID는 lowercase canonical이며 사용자 UUID를v4로 제한하지 않는다.

[Identity13개 manifest](../../src/test/resources/contracts/payment-lifecycle/v1/identity-indexes.json) 중 I2는 stream2개/journal3개/delivery2개 **7개**를 우선 구현한다. 남은 snapshot/recovery6개는 I3 책임이다. 이름/key 순서/unique/partial/TTL 없음/simple collation/sparse=false를 정확히 검사한다. 불일치는 fail-fast, 자동 drop/recreate 금지. initializer는 승인된 준비 단계에만 생성하고 일반 시작에서 운영 DDL을 무조건 실행하지 않는다.

dispatch marker·cutover 증거·정리 상태에 내부 보조 필드가 필요하면 개인정보 없는 최소 shape와 보존기한을 명시한다. 새 collection/index가 필요할 경우 수락 manifest를 임의 확장하지 않고 검토한다.

### 5.2 탈퇴 capture와 commit 불명

권장 경계는 다음과 같다.

1. 인증·withdrawal credential 검증은 유지한다. 논리적 탈퇴 작업의 신규 eventId를 한 번 준비하고 재시도 동안 유지한다.
2. `UserWithdrawalTransactionService`의 기존 트랜잭션에서 User CAS, control terminal 전환, 세션 폐기, 기존 lifecycle/바인딩 해제/LC outbox 저장을 유지한다.
3. 동일 session 안에서 현재 ACTIVE stream을 확인하고 counter를 조건부 증가한다. 반환된 sequence로 journal과 Billing PENDING delivery를 insert한다. LC outbox도 같은 eventId/withdrawnAt을 재사용하도록 최소 변경한다.
4. 어떤 저장 실패도 전체 rollback한다. counter만 먼저 commit하거나 afterCommit에 journal을 넣지 않는다.
5. snapshot readConcern, majority+j commit을 요구한다. 현재 Mongo 설정이 purchase writer flag에 결합되어 있으므로 **capture required만 켜져도** 강한 내구성 설정을 보장하도록 조건과 테스트를 확장한다.
6. 성공 commit 이후에만 탈퇴 성공 응답을 반환하고 HTTP 전송은 별도 worker가 맡는다.

`capturedAt`은 최종 성공한 capture 트랜잭션 시도 시작의 관측시각이다. 확정 rollback 후 새 전체 시도라면 갱신 가능하지만, **동일 transaction의 commit retry·unknown 결과 확인·전송 retry에는 변경하지 않는다.** expiry는 이 값+120일이다.

TransientTransactionError 전체 재시도와 UnknownTransactionCommitResult commit 재시도를 분리한다. 내부 권장 초기값은 각 최대3회이되 합산 시도/commit 관측30초 deadline을 우선하고 무한 retry를 금지한다. 이는 구현 초기값 제안으로 실측 후 조정하며 계약의30초 budget을 늘리지 않는다.

unknown 이후에는 동일 transaction commit 결과를 먼저 확인한다. 새 요청은 영속 탈퇴 상태·기존 event와 unique 제약을 통해 동일 사실인지 확인하며 새 event로 우회하지 않는다. 결과가 확정되지 않으면 성공을 꾸미지 않고 기존 public 오류 매핑을 점검해503/현재 탈퇴 충돌 응답을 명세화한다. 회원 탈퇴로 refresh가 이미 폐기되었을 수 있으므로 임의 userId로 성공 조회하는 API를 만들지 않는다. 외부 응답이 미확정이어도 내부 사실은 하나로 수렴해야 한다.

TMI-200에서 최초 terminal 전환에 epoch가 증가하므로 I2가 다시 epoch를 증가시키거나 control을 재생성하지 않는다.

### 5.3 Billing 전송과 정확한 실패 분류

- route: `POST /internal/v1/payments/accounts/withdrawal/events`.
- HTTPS Lattice AWS_IAM + SigV4, signing service `vpc-lattice-svcs`. 환경별 고정 origin/route allowlist, workload role credential 사용, redirect NEVER. JWT fallback 없음.
- 본문은 기존 `eventId,schemaVersion=1,userId,withdrawnAt` **4필드만**. stream/sequence/generation 추가 금지. 기존 outbound4KiB·Billing inbound16KiB 구분 유지.
- transport는 원 응답 body를 저장/로깅하지 않고 status-first 분류한다. Lattice401/403이 JSON이라는 가정 금지.
- 기존 `BillingSigV4JsonTransport.post`의 결과 의미를 바꾸지 않고, 새 전용 응답 타입/메서드 또는 adapter에서 bounded Retry-After header values와 수신 시각을 보존한다. 기존 사용처 회귀를 포함한다.

| 응답/오류 | Billing delivery 처리 |
| --- | --- |
| 204 | DELIVERED. 20번째 시도라도 성공 |
| 나머지2xx | CONTRACT_ERROR → DEAD_LETTER |
| 401/403 | 즉시 BLOCKED_AUTH, 자동 반복 없음 |
| 408/425/429/5xx, network timeout/connection | bounded retry |
| 3xx | 따르지 않고 DEAD_LETTER |
| 400/404/405/409/413/422 및 위 예외 외 모든4xx | DEAD_LETTER, payload/ID 변경으로 우회 금지 |
| 최종1xx·범위 밖 status·HTTP protocol 파싱 실패 | PROTOCOL_ERROR,20회 budget 내 retry 및 경보 |
| credential 획득/서명 실패 | HTTP401로 꾸미지 않음. 전송 전 실패용 고정 reason과 BLOCKED_AUTH 권장; 운영 재개 필요, 상세 provider 오류 비노출 |

공동 계약의 초기값: connect3초, 전체 request5초, poll5초, batch20, lease60초. 순차20건을 미리 한꺼번에 lease 잡아 뒤 작업이 만료되게 하지 않는다. 전송 직전 건별 claim/검사하거나 제한 병렬성을 사용한다.

retry backoff는 `min(3600초, 5초 × 2^(attempt-1))` full jitter, 최소1초다. overflow 없는 포화 계산을 사용한다. 동일 generation의 자동 dispatch는 최대20회,20번째 실패는 DEAD_LETTER. lease 획득 자체는 시도가 아니다.

### 5.4 lease·재시도·수동 재개

모든 상태 변경에 event/destination, leaseOwner/leaseVersion, resumeGeneration과 유효 lease 조건을 사용한다. source 유효성도 재확인한다. stale worker가 성공/실패를 뒤늦게 써서 새 상태를 덮어쓰지 못하게 한다.

전송 직전 durable dispatch marker와 generation/lifetime attempt 증가를 **같은 조건부 쓰기**로 확정한다. marker 직후 crash는 실제 전송 여부가 불명이므로1회 소비한다. marker 저장 결과 자체가 불명이면 transport를 무조건 실행하지 않고 영속 marker를 확인한다. 응답204 후 상태 저장 실패/lease 소실은 다음 전송의 duplicate204로 수렴한다.

Retry-After는429/503에만 적용한다.

- 정수초(0 포함)와 RFC HTTP-date를 지원한다. 수신 UTC clock 기준 `notBefore=max(backoff 종료, header시각)`.
- 과거 date·정수0은 추가 지연0이지만 기본 최소1초는 유지한다.
-24시간 초과 미래값은 BLOCKED_RETRY_AFTER와 경보, notBefore 유지.300초로 잘라 조기 재전송 금지.
- 음수/overflow/parse 실패는 고정 reason을 기록하고 기본 backoff. 원 header 값 로그 금지.
- 여러 header values는 AMBIGUOUS_RETRY_AFTER로 차단·검토. HTTP-date 내부 쉼표를 여러 값으로 오인하지 않는다.
- 2026-10-13 사용자 승인·Billing 회신에 따라 발신의 별도 clock 증명/관측 provider 요구를 제거한다. 인프라 시각 동기화·이상 알림·영향 작업 중단/복구를 운영 전제로 하며 앱의 독립 절대시각 오차 탐지·차단은 보장하지 않는다. JWT 시간 검증과 Retry-After 계산은 유지한다. 기존 BLOCKED_CLOCK은 호환 읽기 및 권한 있는 선택 재개만 유지하고 일괄 재개하지 않는다. 구매 readiness 보호는 별도다. [합의 인계](TMI-203-204-publisher-clock-simplification-Billing-handoff-2026-10-13.md).

재개는 원천 유효·권한·notBefore를 확인한 뒤 expected resumeGeneration/leaseVersion CAS로 수행한다. 원 eventId/본문/lifetimeAttemptCount 유지, resumeGeneration 증가, generationAttemptCount만0으로 초기화한다. 재개 자체도 operation 식별자로 중복 실행을 방지하도록 내부 명령 계약을 고정한다. 옛 worker의 결과 쓰기는 실패해야 한다.

재개 감사에는 승인 주체의 비개인 권한 참조·사유 코드·설정 버전·건수·작업 ID만 최소 기록한다. 전건 reset·실패 내역 삭제 금지. 인증 실패/dead-letter 최초 검토7일 이내, 미전달24시간 경보는 시도 횟수와 독립이다.

`resumeGeneration`은 전송 작업 재개 횟수이며 I3의 UUID `consumerRecoveryGeneration`, source `streamId`, leaseVersion과 다르다. push4필드 wire에는 어느 generation도 추가하지 않는다.

### 5.5 보존·cleanup과 I3 계약

원천 business collection에 TTL을 걸지 않는다. I2에서 만료 후보 조회·dry-run·경보·최소 unavailable 범위 기록·조건부 정리 서비스의 기반을 구현하되, 실제 정리 scheduler는 기본 OFF다.

1. capturedAt+120일 기준, 만료7일 전에 미적용 가능 범위 경보와 담당 검토.
2. journal과 delivery의 사용자 재연결 참조를 함께 정리한다. 완료/실패 상태도 예외가 아니며 pending이라고 보존을 자동 연장하지 않는다.
3. sequence 연속 prefix만 replayFloor를 전진시킨다. capturedAt 순서가 달라 중간만 만료되면 unavailable interval로 남긴다.
4. push204의 개별 증거, feed ACK의 연속 원 이벤트 증거, snapshot의 상태 baseline을 구분한다. PENDING이라도 feed로 적용됐으면 미적용 GAP로 잘못 표시하지 않는다.
5. consumer 미검증 구간 소실은 최소 `{streamId,consumer,fromSequence,toSequence,reason,evidenceKind}` gap으로 남긴다. event/user 목록 복제 금지. source 정리가 기존 정상 적용 증거를 취소하지는 않는다.
6. 새로운 recovery generation/ACK와 cleanup이 같은 control CAS로 경쟁하도록 I3 포트를 고정한다. 예전 세대 ACK를 현재 증거로 보지 않는다. 증거 provider가 없으면 정리를 fail-closed하고 임의 COMPLETE를 만들지 않는다.
7. 이미 만료된 원천은 worker가 새 event로 재개하지 않는다. I3에서410 REPLAY_UNAVAILABLE 및 coverage 처리로 연결한다.

I2에서는 테스트용 evidence port로 정리 판단을 검증한다. **운영에서 항상 안전하다고 반환하는 임시 adapter는 제공하지 않는다.** I3 연동이 원천 만료 전에 완료되지 않으면 담당 검토·승인된 case 또는 GAP 기반 만료 처리가 필요하다. cleanup OFF가 무기한 보존 허가라는 뜻은 아니다.

### 5.6 capture cutover·readiness·복원

설정명은 구현 시 확정한다. 의미는 `capture.required`, `billing.publisher.enabled`, `source.cleanup.enabled`로 분리하고 신규 환경의 코드 기본값은 OFF다. cutover 완료 뒤에는 durable marker를 근거로 capture OFF 설정의 신규 instance를 거절한다. publisher OFF는 새 claim만 중단하고 진행 중 요청은 bounded 종료·결과 CAS를 허용한다.

cutover 실행 순서:

1. additive schema/index 진단, 호환 reader/writer 배포. 기존 실제 사용자/키로 fixture를 만들지 않는다.
2. 관련 탈퇴 writer barrier와 구 transaction drain, 구 instance/DB 쓰기 권한 배제.
3. 환경별 ACTIVE stream·sequence0·cutover marker를 commit. 자동 시작마다 새 stream 생성 금지.
4. 이후 탈퇴마다 원자 capture됨을 검증하고 barrier 해제. 시간 상한은 측정 후 승인.
5. publisher OFF 상태에서도 backlog 축적·경보를 확인. IAM/consumer 검증 후 별도 승인으로 전송 ON.

TMI-200 `PurchaseReadiness`에는 검증된 stream/capture 상태, control 이관·writer drain 결과, clock/commit budget 및 필요한 공동 gate를 조합한다. 단순 true bean·운영 boolean으로 증거를 대체하지 않는다. I3/snapshot·token gate 미완료 시 구매 활성화는 계속 차단한다.

정상 majority failover는 streamId 유지. backup restore/sequence rewind는 barrier에서 기존 stream을 CLOSED_RESTORE로 닫고 새 incarnation/sequence0을 만든다. 구 counter 값을 과거 최대값으로 간주하지 않는다. consumer 복원은 I3의 새 generation으로 연결하고 UNKNOWN/GAP를 재평가한다.

rollback은 신규 구매 발급/publisher만 독립 OFF. capture 호환 writer·journal·fence·Billing known-withdrawn 차단 유지. 구 capture 미지원 버전 재투입 금지.

### 5.7 구현 단위와 의존성

한 Jira 안에서 아래3개 PR 단위로 나누는 것을 권장한다. 이번 문서 작성으로 PR/브랜치를 생성하지 않는다.

| 순서 | 구현 | 완료 근거 |
| --- | --- | --- |
| A. 원천/원자 capture | 모델·7개 index·stream/counter·탈퇴 transaction 연결·unknown 수렴·readiness 기반 | publisher OFF에도 저장, LC와 공존, rollback/중복/숫자 경계·실제 Mongo 증거 |
| B. 전송/재개 | SigV4 adapter·status/Retry-After·lease/marker·20회 budget·권한 재개·경보 | HTTP matrix·crash·stale worker·같은 event 재개·기존 LC 회귀 |
| C. 보존/운영 경계 | expiry dry-run·coverage/evidence port·조건부 cleanup 기반·cutover/restore runbook |120일/중간 구멍·세대 경쟁의 단위 검증, I3 연동 gate와 staging 미확인 명시 |

I3는 A의 journal/stream과 증거·cleanup 포트를 바탕으로 병행 준비할 수 있다. I3 API를 I2에서 중복 구현하지 않는다. Billing consumer 소스도 이 저장소에서 수정하지 않는다.

### 5.8 완료 기준과 검증

기본 자동 테스트는 외부 Atlas/OAuth 없이 `./gradlew clean test`로 실행한다. 승인된 disposable 다중노드 Mongo 실험은 별도 명령/프로파일과 결과로 관리하며 일반 test profile에 실제 인프라 URI를 넣지 않는다.

| 영역 | 필수 사례 |
| --- | --- |
| 원자성 | capture 각 단계 실패 시 User/control/세션/LC outbox/counter/journal/delivery 전체 rollback; publisher OFF 저장 |
| 중복/경합 | 동시 탈퇴, unique 충돌, 순번 overflow, 발급/탈퇴 양방향 commit, LC 성공/Billing 실패 및 역방향 |
| 실제 Mongo | 다중노드 failover·write conflict·실제 성공 commit 응답유실 뒤 원 사실 수렴, 순번 구멍/중복 없음,30초 관측 bound |
| 전송 |204·다른2xx·모든4xx 분류·413·3xx·5xx·최종1xx/invalid protocol·인프라 비JSON·서명 실패 |
| 재시도 |20번째 성공/실패, lease만 획득0회, dispatch marker crash/불명, 응답유실 후 duplicate204 |
| Retry-After |0/과거date/미래date/24시간 경계/긴 값/음수/overflow/중복 header/date 쉼표/clock 이상 |
| 재개 | 승인 없음 거절·동일 operation 중복·generation 경쟁·stale lease 결과 거절·notBefore/expiry 유지·lifetime 보존 |
| 보존 |120일 전/정각·만료7일 경보·중간 interval·연속 floor·push/feed/snapshot 증거 차이·복원 세대 경쟁·개인 참조 제거 |
| 운영 gate | capture required 없이 구매 ON 거절·cutover 후 capture OFF 거절·publisher OFF 진행 요청 bounded 종료·복원 새 stream |
| 회귀/보안 | LC 기존2xx/JWT/cleanup 불변, 기존 Billing transport 사용처, RS256/기존 public 응답, 본문·인증정보·사용자 ID 로그 제외 |

Mock 통과만으로 실제 Mongo 완료 기준을 달성했다고 보고하지 않는다. 환경이 준비되지 않으면 코드 완료와 실험 미완료를 분리하고 해당 활성화 gate는 유지한다. 실제 삭제·판매는 이 Jira의 테스트 성공으로 자동 허가되지 않는다.

## 6. 부록 — 근거·변경 후보·이번 검증

### 6.1 현재 확인한 구현 사실

| 코드/문서 | 확인 내용 | 계획 영향 |
| --- | --- | --- |
| [UserWithdrawalTransactionService](../../src/main/java/web/tosunsaeng/identity/domain/user/application/UserWithdrawalTransactionService.java) | 기존 Mongo transaction 내 User CAS/control/세션/LC outbox 처리 | 같은 transaction에 capture 참가자 추가 |
| [UserWithdrawnOutbox](../../src/main/java/web/tosunsaeng/identity/domain/user/domain/entity/UserWithdrawnOutbox.java) | 새 event UUID, legacy backfill name UUID, cleanupAt TTL | 새 capture event 공유; LC legacy/cleanup 유지, 원천 대용 금지 |
| [UserWithdrawnPublisher](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalevent/application/UserWithdrawnPublisher.java) | 모든2xx 성공, 기존 retry/lease | Billing 전용 publisher 분리 |
| [UserWithdrawnEventMapper](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalevent/application/UserWithdrawnEventMapper.java) | 기존4필드/4KiB | 동일 wire serializer 검증·재사용 가능성 점검 |
| [BillingSigV4JsonTransport](../../src/main/java/web/tosunsaeng/identity/global/workload/BillingSigV4JsonTransport.java) | Lattice signing/HTTPS/no redirect, Retry-After1~300초 | richer response를 별도 경로로 추가, 기존 의미 유지 |
| [MongoTransactionConfig](../../src/main/java/web/tosunsaeng/identity/global/config/MongoTransactionConfig.java) | 강한 concern이 현재 purchase control writer flag에 의존 | capture-only 조건도 내구성 보장 |
| [PurchaseReadiness](../../src/main/java/web/tosunsaeng/identity/domain/auth/purchase/PurchaseReadiness.java) | 실제 증거 adapter 없으면 구매 ON 불가 | permissive stub 없이 capture+이관+공동 gate 연결 |
| [Identity manifest](../../src/test/resources/contracts/payment-lifecycle/v1/identity-indexes.json) |7개 I2/6개 I3, TTL 없음 | exact subset initializer·옵션 검증 |

### 6.2 변경 후보

- 신규 패키지 후보: `domain/user/withdrawalcapture/{domain,application,repository,infrastructure}`. stream/journal/delivery와 capture service, Billing publisher/dispatch/retry/resume, expiry/evidence port, config/initializer.
- 기존 수정: UserWithdrawalTransactionService/Service, UserWithdrawnOutbox 생성 factory, MongoTransactionConfig, PurchaseReadiness 구현 연결, application.yml 및 관련 명세·테스트.
- 공유 transport 확장 시 기존 호출자 소스와 테스트를 전수 확인한다. 변경하지 않아도 되는 LC publisher/backfill/TTL은 그대로 둔다.
- 운영 증거 runbook과 테스트 fixture는 synthetic 데이터만 사용한다. 실제 credential/개인정보·본문 로그 금지.

### 6.3 이번 계획 작성 검증

공식 Jira TMI-202 설명·완료 기준(조회 시 상태: 해야 할 일), 최신 공동 계약 관련 절·exact manifest, TMI-200 결과 및 현재 develop 소스를 대조했다. 이번 작업은 문서만 작성했으므로 Gradle/실제 Mongo/HTTP 호출은 하지 않는다. 문서 링크와 diff를 정적으로 검사한다. Jira·코드·외부 설정·배포 변경 없음.

### 6.4 Jira 댓글 초안 — 자동 등록하지 않음

TMI-202 구현 계획 작성. 변경 파일: TMI-202-withdrawal-capture-billing-delivery-plan.md, WORKLOG, CURRENT_STATE. 원자 capture·Billing 전용 SigV4/204-only·lease/dispatch budget·재개·120일 cleanup과 I3 증거 경계, TMI-200 readiness/cutover 연결 및 테스트 matrix 정리. 소스/계약·문서 링크/diff 정적 검사, 코드 변경 없어 Gradle 미실행. 실제 다중노드 Mongo·IAM·barrier·복원·cleanup 활성화는 미검증/별도 승인.
