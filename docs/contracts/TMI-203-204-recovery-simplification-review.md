# TMI-203·204 단순화 공동 검토 회신 — 진행 상태의 Billing 단일 소유

- 작성일: 2026-10-12 / 조사 브랜치 develop / Jira: TMI-203, 연계 TMI-204.
- 상태: **공동 검토용 기술 권장안. 기존 exact 계약을 대체하지 않는다. 구현·배포·판매·삭제 승인이 아니다.**
- 후속 2026-10-12: 사용자가 권장 방향으로 계획 수정을 승인하여 [현행 TMI-203 계획](TMI-203-withdrawal-recovery-snapshot-feed-plan.md)에 반영했다. 본문은 당시 비교 검토 근거로 유지한다. 공동 exact/상대 기술 수락·구현 승인은 별도다.
- 사용자 승인: 기존 구매 계정·purchaseAccountRefId·실거래 없음, 최초 출시 legacy COMPLETE 증명 제외 및 단순화 방향. 아래 API 제거·책임 변경은 아직 상대 수락 전이다.
- 기준: [현 Identity 계획](TMI-203-withdrawal-recovery-snapshot-feed-plan.md), [Billing 계획](../../../billing/docs/plans/TMI-204-withdrawal-recovery.md), [공동 계약](../../../billing/docs/contracts/IDENTITY-BILLING-PAYMENT-LIFECYCLE-TECHNICAL-CONTRACT.md), [수락 exact](billing-c0-exact-wire-index-proposal-2026-10-07.md).

## 1. 5줄 결론

1. 권장안은 **Identity가 복구 자료를 제공하고, Billing만 적용 checkpoint·복구 epoch를 소유**하는 구조다. 원격 ACK 왕복·양쪽 완료 상태 대사를 줄인다.
2. Identity `/recoveries`, `/checkpoints`, `/checkpoints/status`와 원격 consumer generation은 제거 후보로 둔다. 대신 Billing의 로컬 epoch·transaction fence·복원 격리는 반드시 유지한다.
3. 고정 T/H snapshot·digest와 고정 H2 서명 cursor는 유지한다. live scan의 삭제/조회 경합을 해결하는 추가 장치보다 현재 보장과 fixture를 재사용하는 편이 명확하다.
4. 정상 운영은 push+주기적 feed가 자동으로 처리하고, 전체 snapshot은 초기화/기준점 상실 등 필요한 때만 쓴다. 일시 장애는 서비스 단위 자동 재시도·경보이며 건별 사용자 심사가 아니다.
5. 비용 절감의 대가는 **Identity가 Billing의 적용 완료·consumer별 GAP을 판정하지 못하는 것**이다. 이 책임을 Billing으로 옮기는 변경이므로 공동 계약·I2 cleanup port·I4/205 gate를 함께 개정해야 한다.

## 2. 사용자가 반드시 읽어야 하는 내용

### 2.1 이번에 줄이는 것은 이중 기록이다

현재안은 Billing이 적용 진행을 저장하고, ACK 작업을 저장·전송하고, Identity도 진행을 저장하고, 응답 유실 시 양쪽 상태를 다시 비교한다. 권장안은 Billing의 적용 transaction에서 진행까지 확정하고 거기서 끝낸다. 다음 조회는 그 로컬 진행점에서 한다.

Identity의 원천120일 보존은 ACK를 받으면 단축하거나 ACK가 없으면 연장하는 정책이 아니다. 따라서 보존기간을 고정한 채 **Identity의 소비자별 완료 증거를 제거할 여지**가 있다. 대신 원천 재생 가능 범위를 Identity가 정확히 제공하고, Billing은 자기 적용 범위와 비교해야 한다.

기존202의 실시간 push204 응답은 그대로다. 제거 후보는203의 **별도 복구 완료 ACK API**이며 push 응답·멱등 inbox·서명 인증을 없애는 것이 아니다.

### 2.2 여전히 유지되는 보장

- 탈퇴·journal·순번·delivery 원자 저장, publisher OFF와 무관한 capture.
- Billing 적용·inbox/중복 처리·로컬 checkpoint의 같은 transaction commit. commit 전 다음 순번으로 진행 금지.
- WITHDRAWN을 ACTIVE로 되돌리지 않는 적용. snapshot과 원 event의 증거는 구분.
- ACTIVE MEMBER 발급, 기존 token/진행 요청과 탈퇴 경합 보호. 이미 발급된 token이 자동 무효화된다고 가정하지 않음.
- Billing 로컬 복구 epoch/version의 쓰기 CAS. lease 시간 만료만으로 옛 worker의 쓰기를 차단했다고 보지 않음.
- source 복원 새 stream, Billing 복원 시 구프로세스 차단·새 로컬 epoch. 사용자 자료·보존기간 유지.
- 누락·만료·restore를 새 기준점으로 덮어 정상화하지 않음. legacy UNKNOWN/PARTIAL은 사실대로 유지.

### 2.3 보장하지 않는 것

실시간 push·주기적 feed는 서로 다른 DB 사이의 즉시 원자 동기화가 아니다. 이벤트 적용 전의 짧은 지연 구간은 존재한다. 정상 지연 중 판매 계속 여부는 기존 token/발급 보호와 제품 정책의 경계이지, 복구 API를 단순화해서 즉시 차단을 증명할 수 있는 문제가 아니다.

원천이 만료되고 필요한 User tombstone도 사라졌다면 원 사건도 탈퇴 상태도 완전히 복원할 수 없다. snapshot 성공만으로 이 공백이 사라지지 않는다. 반대로 원 사건은 없어도 현재 tombstone이 있으면 탈퇴 상태를 수렴시킬 수 있지만 eventId 수신 이력까지 복원한 것은 아니다.

## 3. 결정과 회신이 필요한 사항

추가 제품 선택을 반복 요청하지 않는다. 양 서버가 아래 기술 책임 이동에 동의해야 기존 계약을 개정할 수 있다.

1. Billing을 로컬 checkpoint/복구 epoch/consumer coverage의 유일한 authority로 정한다. Identity에는 재생 가능 범위만 남긴다.
2. 신규 계약 revision을 명시한다. 기존 endpoint에서 필드만 조용히 생략하거나 옛 generation 이름을 새 의미로 재사용하지 않는다. versioned 경로 또는 배포 전 원자 전환 중 호환성 조사로 하나를 확정한다.
3. IAM/복원 runbook에서 Billing의 구프로세스 DB 접근 차단과 source 교체 확인 책임을 명시한다. 이것이 없으면 원격 generation 제거안은 수락 불가다.
4. 205가 원격 checkpoint가 아니라 Billing 로컬 증거와 거래 확인을 사용하도록 한다. source 소실·consumer 증거 소실 때 대상 범위를 모르면 광범위 보류 가능성을 유지한다.
5. 기존 보존 범위 안에서 Billing 통합 control/job 최소 필드·index와 source availability 응답을 exact fixture로 동결한다. 새로운 보존기간/무기한 pending 보존은 도입하지 않는다.

이번 공식 Jira 조회에서는 TMI-203이 아직 기존 원격 ACK/generation 범위와 완료 조건을 갖고 있었다. **이 문서만으로 해당 이슈 구현 범위를 바꾸지 않는다.** 사용자 요청은 기술 검토이므로 코드·Jira·Billing 문서를 수정하지 않았다.

## 4. 주요 위험과 미확인 사항

| 위험 | 권장안의 대응·한계 |
| --- | --- |
| 복원된 checkpoint만 실제 반영보다 앞섬 | account/lifecycle/inbox/control을 동일 복원 단위로 보장. 선택적 복원/확인 불가면 checkpoint 신뢰 중단·초기 복구. remote 높은 값으로 승격하지 않음 |
| 복원본에도 옛 epoch가 남음 | 복원된 값 자체를 신뢰하지 않음. 구프로세스 격리 후 새 epoch를 로컬 설치, 일반 worker의 자동 채택 금지 |
| 원격 세대 없이 옛 요청이 Identity에 도착 | 읽기/run 생성만 가능, consumer 완료 상태를 바꿀 권한 자체가 없음. Billing이 요청에 캡처한 epoch/run으로 결과 commit을 거절. 오래된 run 비용은 rate limit/expiry로 제한 |
| 원천 cleanup 도중 feed 조회 | 짧은 일관 읽기에서 stream·counter·floor/interval·journal 연속성 확인. 관측 목표 이전 구멍은 GAP/410, 빈 성공 금지 |
| Identity가 소비자 lag를 모름 | Billing 측 지표·경보와 독립 watchdog 필요. Identity는 원천 만료 경보만 제공. Billing 정지 기간의 consumer별 판단은 불가능하다는 한계 인정 |
| snapshot 필요 시 Mongo 고정 T 지원 불가 | 기존 spike 유지. live 조회로 fallback하지 않음. 별도 대안 공동 검토 |
| 정상 지연 중 기존 token 사용 | 구매 gate와 token 계약 문제로 별도 구분. 복구 기능은 즉시 무효화 보장 아님 |
| 임시자료 정리와 builder 경합 | run lease/version CAS·expiresAt 확인,7일 기한과 불변 seal 유지. remote generation을 없애도 source stream/run fence는 필요 |

## 5. 권장 흐름과 설계 비교

### 5.1 정상 흐름 — 전체 snapshot을 매번 돌리지 않는다

1. Identity202가 탈퇴 기록을 저장하고 기존 push를 자동 재시도한다.
2. Billing201은 기존 transaction으로 탈퇴를 반영하고 commit 뒤204를 반환한다.
3. Billing204 주기 worker가 durable local checkpoint `C` 이후 feed를 조회한다. 요청 전 local epoch·job lease/version을 캡처한다.
4. 원 event를 기존 inbox/applier로 중복 안전하게 적용하고, 연속 적용 지점과 job 진행을 **같은 transaction**에서 갱신한다. local epoch/lease version 쓰기 CAS를 통과해야 한다.
5. 외부 ACK를 보내지 않는다. crash/commit 결과 불명 시 durable checkpoint를 다시 읽어 같은 페이지를 재조회하거나 다음 페이지로 진행한다.
6. 해당 H2까지 따라잡으면 local 관측 상태만 갱신하고 다음 poll을 예약한다. 이것은 전역 legacy COMPLETE도 판매/삭제 자동 허가도 아니다.

pull이 반영한 사건의 Identity push가 나중에 다시 와도 기존 inbox로 NOOP 처리한다. 원격 ACK 제거 후 Identity delivery 상태를 임의로 DELIVERED로 바꾸지 않는다. 실제 push 응답으로만 기존202 상태가 전진한다.

### 5.2 초기화·snapshot — 안전한 경계는 유지

- Billing이 로컬 복구 job/epoch를 정하고 Idempotency-Key를 영속화한 뒤 snapshot을 요청한다. Identity는 인증된 consumer/환경/stream/key에 run을 바인딩한다. caller의 epoch를 인증 수단이나 Identity 현재 세대로 취급하지 않는다.
- 같은 T의 User WITHDRAWN과 H, immutable rows/count/digest/pages,5분 build budget·7일 expiry는 그대로다. snapshot에는 실제 기존 탈퇴자를 포함한다.
- Billing은 각 page 적용+진행을 원자 저장하고 마지막에 digest/count를 검증한 후 local baseline H를 확정한다. 같은 epoch에서 더 높은 진행을 덮어쓰지 않으며 초기 bootstrap 또는 명시 새 epoch에서만 baseline을 설치한다.
- H 이후 feed를 이어 읽는다. 원격 BASELINE ACK를 기다리지 않는다. 적용을 완료하기 전에 H를 로컬 checkpoint로 미리 쓰지 않는다.
- snapshot failure/expiry가 일시적이면 같은 논리 job 안에서 새 run을 자동 예약할 수 있다. 이전 실패 기록을 지우거나 epoch를 매 retry마다 바꾸지 않는다. 반복 budget 소진 시 job/circuit 단위 경보, 개인정보 사본 기한 연장 금지.

snapshot 이후 필요한 feed 원천이 만료됐으면 새 snapshot으로 일부 상태를 수렴시킬 수 있다. 그러나 누락 원사건 구간은 별도로 남으며, snapshot 완료만으로 GAP/삭제 보류를 자동 해제하지 않는다. 같은 복구 불가 구간을 무한 재시도하지 않는다.

### 5.3 왜 live scan 겹침 대안은 이번 권장안에서 제외하는가

가능한 대안 자체는 있다. **먼저** committed 시작 순번 S를 잡고 User를 keyset으로 읽은 뒤 S 이후 feed를 겹쳐 적용한다. S 이후 탈퇴가 이미 지나간 userId 위치에서 일어나도 feed가 보충하고 중복은 NOOP가 된다. 그러나 다음 조건이 모두 필요하다.

1. S 시점에 이미 탈퇴한 사용자는 scan에서 해당 위치를 읽을 때까지 삭제·ID 변경·WITHDRAWN 해제가 없어야 한다. 또는 해당 변화까지 복구 가능한 원천이 있어야 한다.
2. S 이후 발생한 탈퇴 journal은 소비 완료까지 남아 있어야 한다. 고정 보존을 연장하지 않으려면 deadline·expiry/interval 검증 실패 시 완료를 거절해야 한다.
3. scan 끝까지 source incarnation·조회 정렬·page 연속성을 검증하고 로컬 baseline과 feed 진행을 원자 설치해야 한다. snapshot 시점처럼 보이는 거짓 manifest를 반환해서는 안 된다.
4. S 이전 사건이 만료된 사용자 tombstone이 조회 전에 정리되면 scan과 feed 양쪽에서 빠진다. 단순히 S 이후 feed를 읽는 것으로는 해결할 수 없다.

현재 좁은 User 경로 검색에서는 Firebase/소셜 연결 삭제를 확인했지만, User tombstone의 모든 삭제·수동 운영·보존 경로가 없다는 증명은 하지 않았다. 이를 해결하려고 삭제 barrier·별도 delete journal·보존 pin을 추가하면 단순화 이익이 줄고 정책 변경 가능성도 생긴다. **따라서 이번 최소 권장안은 검증 가능한 T/H 경계를 유지한다.** digest도 기존 golden fixture와 부분 전송 검증을 재사용한다.

### 5.4 고정 H2는 유지하되 영속 scan 저장소를 줄인다

H2는 “이번 배치 묶음을 어디까지 따라잡았나”라는 관측 경계로 유용하며 Identity 별도 scan 컬렉션 없이 서명 cursor로 처리 가능하다. 원격 ACK를 없애면 H2가 원격 완료 증거일 필요도 없다.

- 현재 H2·cursor·마지막 적용 순번·lease는 Billing의 단일 feed job/control 문서에 둔다. 별도 과거 scan 이력 컬렉션은 두지 않는 안이다.
- cursor TTL1시간/소유자·stream 검증은 유지한다. 만료하면 **현재 로컬 C**에서 새 scan을 열고 이전 H2 완료를 주장하지 않는다.
- 마지막 page에도 연속성 검증을 유지한다. H2에 도달했을 때만 해당 scan caught-up 관측을 기록한다. 정상 판매의 매 요청에 caught-up 상태를 필수 연결하지 않는다.
- 단순 live bounded batch로도 기본 replay는 가능하지만 끝점 부재로 lag 측정 의미가 흐려진다. 이미 값 몇 개와 cursor만으로 유지 가능한 장치를 제거하는 이익은 작다.

### 5.5 Billing 로컬 epoch와 복원

**lease/version만으로 대체하지 않는다.** 재시작한 작업자가 만료 전 요청 결과를 늦게 반영하거나 DB가 오래된 lease로 복원될 수 있기 때문이다.

- Billing 로컬 control의 epoch(UUID)·CAS version과 restore barrier를 유지한다. page/inbox/중복 NOOP/checkpoint/account 생성/push 적용 경로가 같은 현재 control 쓰기 경계를 통과하도록204에서 검증한다.
- 일반 timeout·프로세스 재시작은 같은 epoch의 durable checkpoint에서 재개한다. 자동 retry마다 운영 승인이나 새 세대를 요구하지 않는다.
- Billing DB restore는 이전 worker의 DB 접근 제거·drain·격리 후 새 local epoch 설치가 필수다. 일관 백업의 C와 적용 데이터가 함께 복원됐고 원천 가용 범위가 충분하면 C부터 replay한다. 그렇지 않으면 새 snapshot 기반 복구로 전환하며 증거 소실은 기록한다.
- restore 과정에서 source stream이 바뀌었으면 순번을 비교하지 않고 새 baseline 절차를 사용한다. 옛 source 서버/DB endpoint도 격리해야 하며 stream 값만 확인했다고 topology 안전을 증명하지 않는다.
- Identity의 원격 epoch가 없어도 old 결과는 Billing에서 차단할 수 있다. 단, 이 논리는 Identity에 old ACK가 갱신할 원격 consumer 상태가 **없어진 경우에만** 성립한다. API만 삭제하고 원격 완료 상태를 내부에 남기는 혼합안은 제외한다.

### 5.6 원천 cleanup과 보존 — 적용 여부는 Billing 책임

Identity는 기존 원천120일을 유지하고 삭제와 함께 replayFloor·unavailableIntervals를 원자 저장한다. 연속 prefix만 floor로 전진하고 중간 만료는 interval로 남긴다. 출처 timing/clock·source stream fence·in-flight lease·명시 apply 권한은 유지한다.

consumer checkpoint가 없으므로 Identity는 “Billing이 적용했나”를 보고 지우지 않는다. **기한 전 임의 삭제도, 소비자 응답을 기다리는 기한 연장도 하지 않는다.** source availability는 다음 feed 응답의 관측 metadata 또는 별도 read-only status 중 하나로 exact 동결한다. 제안은 feed 응답에 bounded availability 요약을 포함하고 증거가 너무 커지면 실패·검토하며 조용히 절삭하지 않는 방식이다.

Billing은 local C와 unavailable 구간을 대조한다. C 이전의 만료는 정상 완료 증거를 무효화하지 않는다. C 이후 필요한 구간 만료는 GAP로 저장한다. Billing이 꺼져 있으면 consumer별 GAP 계산은 재시작 후 수행되므로 독립 heartbeat/원천 만료 경보로 보완한다.

현 [WithdrawalSourceCleanup.Evidence](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalcapture/WithdrawalSourceCleanup.java)의 계약은 consumer generation CAS/coverage 기록을 요구한다. 따라서 **이 port는 공동 합의 후 수정해야 하며 기존 계약의 구현 완료라고 주장할 수 없다.** 새 책임은 source timing/stream·floor/interval 원자 기록이고 consumer 증거는 Billing으로 이동한다. source cleanup의 transaction·delivery 삭제·lease 보호 구현은 재사용한다.

snapshot rows/digest/staging7일, 최소 operation120일, 현 stream 최소 증거·폐기 stream120일은 유지한다. 삭제하는 remote 저장소를 대신해 개인정보를 장기 metric/audit에 복제하지 않는다. Identity User tombstone/205 금융자료 보존은 변경하지 않는다.

### 5.7 상황별 자동 처리·제한 권장표

아래는 공동 검토용 적용 구분이다. 새로운 전역 차단이나 무조건 판매 지속을 승인한 표가 아니다. 제한 범위를 특정할 증거가 없으면 범위가 넓어질 수 있다.

| 상황 | 자동 처리·운영 개입 | 정상 판매/기존 기능 |205 삭제 |
| --- | --- | --- | --- |
| 출시 이전 legacy PARTIAL/UNKNOWN만 존재 | 사실대로 보존, 과거 완전성만 위한 반복 복구 없음 | 사용자 승인된 최초 출시 범위에서 단독 차단 사유 아님 | 실제 후보 거래·탈퇴 근거 별도 판단 |
| network/5xx 등 정상 일시 지연, 상태 소실 징후 없음 | 같은 job/checkpoint 지수 backoff+jitter, 장기화 시 장애 단위 경보 | 단순 lag를 전체 구매/무료 서비스 차단에 자동 연결하지 않음. 기존 token/탈퇴 보호 유지 | 후보별 충분한 증거 없으면 보류 |
|401/403·계약 불일치 | 빠른 반복 중단, 인증/배포 장애 단위 담당 조치·설정 복구 후 재개 | 실제 수신/차단 기능 손상 범위를 평가, lag와 동일 취급 금지 | 증거 부족 범위 보류 |
| 실제 GAP/원천 만료 | 같은 구간 무한 retry 금지, 범위·원인 보존, 가능한 상태 snapshot 복구 | 영향 범위의 탈퇴 차단/발급 증거 평가 후 제한. 전역 정상이라고 가정하지 않음 | 식별 가능 범위 보류, 불명확하면 넓게 보류 |
| Billing restore/선택적 증거 소실 | 구worker 격리·새 local epoch·replay 또는 bootstrap, 복원 건 단위 운영 | 영향 구매 쓰기는 barrier 유지 후 근거 기반 재개. 무료/Identity 전체 중단과 자동 연결 안 함 | 복원 적용/거래 대사 전 해당 범위 보류 |
| Identity restore/stream 교체 | I2 source barrier·새 stream, Billing 새 기준 복구 | 탈퇴/발급/기존 token 경계 재검증 전 영향 구매 쓰기 제한 | source 공백 평가까지 보류 |

204 신규 poll 재시도 정책은 job 단위 bounded budget 후 느린 circuit probe/장애 경보를 제안한다. 새 operation을 무한 생성하거나 실패 기록을 지우지 않는다. 정확 횟수·시간은204 개정 때 동결하며202 기존20회/관리 재개 정책을 이번에 몰래 바꾸지 않는다.205의 매시간·매일·7일 담당 검토는205 후보 정책이며 모든 feed 오류의 건별 승인 규칙이 아니다.

## 6. 부록 — 변경 목록·비용·검증

### 6.1 API·DTO·fixture 변경 후보

| 항목 | 권장 변경 | 유지/주의 |
| --- | --- | --- |
| recoveries/checkpoints/checkpoints status | Identity API3개 제거 후보 | old client에 가짜204 성공 금지. reader-first 또는 명시 version 전환 |
| X-Consumer-Recovery-Generation | 제거 후보 | IAM principal/환경/route 인증은 유지. Billing 로컬 epoch는 request context/job에 보존 |
| snapshot manifest | remote generation 필드 제거 후보 | T,H,count,digest,expiry·legacy/knownGaps source 사실은 유지; baseline 완료 상태와 혼합 금지 |
| snapshot 멱등 scope | environment/consumer/stream/operation key 기반으로 개정 | 동일 key를 다른 요청에 재사용 시 충돌. 새 bootstrap은 새 key,120일 metadata 유지 |
| feed cursor | remote generation binding 제거, consumer/environment/stream/H2/start/TTL 서명 유지 | 로컬 old-response 차단은 Billing 책임 |
| feed availability | floor/interval/관측시각의 bounded source 정보 추가 제안 | 정확 key·상한·16KiB 처리·일관 읽기 규격은 공동 동결 전 구현 금지 |
| coverage | consumer 적용 coverage는 Billing local로 이동 | source legacy 사실은 그대로, UNKNOWN→COMPLETE 자동 변환 금지 |
| 오류 | remote checkpoint/generation 전용 오류 제거 후보 | GAP/REPLAY_UNAVAILABLE/STREAM_REPLACED/expiry/auth/strict 입력 오류 유지 |
| fixtures | 새 revision schema/API/error/index·old epoch local-fence 시나리오 추가 | 원4필드 event·canonical digest·numeric boundary fixture 재사용, 기존 fixture 조용히 덮어쓰기 금지 |

### 6.2 저장소·index와202 영향

- Identity I3에서 remote checkpoints/recovery controls/recovery operations **3개 컬렉션·각 unique index1개를 제거 후보**로 둔다. 아직 운영 존재 여부는 조사하지 않았으므로 실제 drop 명령을 제안/실행하지 않는다.
- snapshot runs/rows는 유지. `ux_withdrawal_snapshot_operation`의 generation 포함 key는 새로운 멱등 scope로 개정해야 한다. 나머지 snapshot index와 I2 index7개는 유지 후보다. 구13개 manifest hash와 initializer/test를 자동으로 새것처럼 취급하지 않는다.
- Billing은 기존 제안7개 복구 컬렉션 중 ACK jobs와 별도 scans를 제거하고, 복구 operation/control의 중복 데이터를 합쳐 **control/job, snapshot run, staging rows, page receipts 중심의4개 내외 구조**를 권장한다. 필드 크기·감사/120일 보존 때문에 필요한 최소 operation 문서는 분리할 수 있으며 최종 수는204 설계에서 확정한다.
-202 capture/sequence/delivery/publisher/204-only/Retry-After/resume/LC 계약은 유지한다. I2 cleanup Evidence와 monitor의 consumer 적용 판단, PurchaseActivationEvidence 구현 조건은 새 책임 분담에 맞춰 변경 필요하다.
- [WithdrawalCaptureConfiguration](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalcapture/WithdrawalCaptureConfiguration.java)의 runtime/purchase gate를 boolean true로 대체하지 않는다. 정상 lag와 소실/복원·capture 불가를 구분하도록 후속 공동 gate를 작성한다.

### 6.3 기존안 대비 비용과 남는 한계

| 선택 | 구현/운영 변화 | 판단 |
| --- | --- | --- |
| 기존안 전체 유지 | remote authority/ACK worker/outbox/두 DB 증거 비교·재개 절차 유지 | 가장 강한 양측 관측, 현재 단순화 목표와 비용 불일치 |
| 권장: Billing local checkpoint/epoch authority | Identity API3개/remote 상태3종, Billing ACK 작업·원격 대사 제거. snapshot·cursor·local fencing 재사용 | 가장 큰 중복 상태를 줄이되 원천/소비자 책임을 명확히 분리 |
| live scan까지 동시 대체 | Mongo snapshot 비용 감소 가능, tombstone 삭제 보호·overlap 검증·새 계약 추가 | 보존/삭제 불변식 미검증으로 이번에는 비권장 |
| generation 대신 lease만 | 상태는 작아지지만 restore/old worker 쓰기 차단 불충분 | 비권장 |

시간/금액 절감 수치는 측정하지 않아 제시하지 않는다. 현재 로컬 Identity203은 계획 상태이고202 source cleanup은 Evidence port이므로 구현 후 철거보다 지금 책임을 조정하는 재작업 비용이 작을 것으로 **추론**한다. Billing204 코드/원격 배포 상태는 이번에 재검증하지 않았으므로 상대 구현량 절감은 확정하지 않는다.

남는 비용은 snapshot materialization·digest·Mongo history 검증, Billing local transaction fence·restore runbook, IAM/서명·source expiry와 gap 검증이다. 이를 제거해서 테스트만 줄이는 안은 아니다. 장래 consumer가 여러 개면 source가 중앙 적용 현황을 모르는 비용이 커질 수 있어 별도 관측 설계를 재검토한다.

### 6.4 최소 불변식 테스트

1. 동시 탈퇴: 동일 T의 User/H에 없는 늦은 commit이 H 이후 feed에 존재. 같은 timestamp도 누락 없음.
2. 중복 push/feed/page: 동일 inbox/applier로 수렴, eventId 다른 내용 충돌은 checkpoint 진행 금지.
3. 적용 후 crash/unknown commit: 적용과 checkpoint 모두 commit되거나 모두 미반영. 재조회·NOOP 안전, 원격 ACK 없이 진행 가능.
4. checkpoint 유실/부분 restore: 높은 C만 남았거나 증거가 없는 경우 건너뛰지 않음. fresh baseline이 과거 GAP을 지우지 않음.
5. old worker: lease 재할당·새 local epoch·restore 이후 늦은 HTTP 결과/NOOP/계정 생성도 CAS 거절. 옛 epoch에 새 번호만 붙여 재사용 금지.
6. cleanup: 중간 sequence 만료는 interval, prefix만 floor 증가. 동시에 feed를 읽어도 H2 전 빈 성공 금지. 원천 삭제와 source 가용성 기록 원자성.
7. snapshot expiry/history loss/부분 rows/digest mismatch: local baseline 확정 금지, 기존 WITHDRAWN 적용을 되돌리지 않음.
8. source/Billing 복원: 정상 failover와 restore 구분, 새 source stream cross-compare 금지. 일관 local backup만 C 재사용, 격리 미완료 때 barrier 유지.
9. 정책 분리: legacy UNKNOWN 또는 단순 lag만으로 전체 구매 차단하지 않음. 실제 손실/복원/탈퇴 차단 불가를 정상 지연으로 바꾸지 않음.205 불명 대상 보류 유지.
10. 보존/보안:7일·120일 경계, cursor expiry·strict16KiB/20건·서명/IAM·로그 비노출. 정상 재시도는 건별 승인 없이 진행하고 인증/영구 GAP은 무한 반복하지 않음.

### 6.5 근거와 이번 작업 결과

- 공식 Jira TMI-203 조회: 기존 snapshot/feed/remote checkpoint/generation 계약 유지 상태 확인, 쓰기 없음.
- [WithdrawalRecords](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalcapture/WithdrawalRecords.java): stream lastSequence/replayFloor, journal120일, delivery/resume 상태 확인.
- [WithdrawalSourceCleanup](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalcapture/WithdrawalSourceCleanup.java): Evidence 원격 consumer 관련 계약, lease 보호, 동일 transaction 삭제 확인.
- [Billing204 계획](../../../billing/docs/plans/TMI-204-withdrawal-recovery.md): remote ACK client/worker·7개 저장소 및 restore fence 계획 확인. 구현 확인과 구분.
- User 관련 제한된 삭제 경로 검색은 tombstone 영구 보존 증명이 아니다. 실제 DB/Atlas/IAM/외부 결제 호출 및 Mongo 실험 미수행.
- 변경: 본 회신과 WORKLOG/CURRENT_STATE만. 기존203 계획·exact·Billing 원문·제품 코드 변경 없음. 문서 링크·git diff --check 검사, 문서 작업이므로 Gradle 미실행. 예상 밖 변경 없음.

### 6.6 Jira 댓글 초안 — 미등록

단순화 공동 검토 회신 작성. Billing local checkpoint/epoch를 authority로 두고 Identity remote ACK/generation API·상태를 제거하는 후보 제안. T/H snapshot·digest·H2 cursor·I2 push/capture 유지, cleanup 소비자 증거 책임 이동 및 Billing restore fence가 수락 조건. 변경 파일은 회신/WORKLOG/CURRENT_STATE, 문서 정적 검증만 수행. exact/fixture/index 개정과 Billing 수락 전 구현 불가, 실제 Mongo/IAM/복원 미검증. Jira 상태·기존 계약·배포 변경 없음.
