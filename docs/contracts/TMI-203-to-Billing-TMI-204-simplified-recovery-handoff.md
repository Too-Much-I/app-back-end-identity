# Billing 인계 — TMI-203·204 탈퇴 누락 복구 단순화

- 작성일: 2026-10-12 / 작성 측: Identity / 대상: Billing TMI-204, 연계 TMI-205.
- 상태: **사용자가 단순화 방향과 Identity 계획 개정을 승인했다. Billing 기술 수락·공동 exact 규격 개정 요청이며 구현·배포·판매·삭제 활성화 승인은 아니다.**
- Identity는 계획서를 개정했으며 이번 인계로 API·코드·운영 설정을 변경하지 않았다. 기존 공동 exact/Jira 본문은 아직 이전안이다.
- 후속: Billing의 문서 개정 지침 수신 후 [exact 미결안 회신](TMI-203-single-authority-exact-amendment-response.md)을 추가했다. 생성 body·availability·index 후보는 그 회신을 함께 검토하되 동결본으로 취급하지 않는다.

## 1. 5줄 결론

1. Identity는 복구 가능한 탈퇴 자료를 제공하고, Billing은 적용 진행점과 복구 epoch를 단독 관리하는 방향으로 계획을 수정했다.
2. Identity의 별도 recovery generation 발급·checkpoint ACK·checkpoint 상태 조회를 신규 구현 범위에서 제외한다. Billing ACK outbox/worker·원격 완료 대사도 제외하는 방향이다.
3. 고정 T/H snapshot·digest·고정 H2 feed·source 만료/누락 구분과 Billing local epoch/transaction fence는 유지한다.
4. 정상 지연은 자동 재시도·장애 단위 경보로 다루고, 실제 GAP·DB 복원·205 삭제 안전성은 별도로 판단한다.
5. 아래 책임·wire/index/fixture 변경을 Billing이 검토·수락한 후 공동 계약을 개정한다. 상대 수락 전 기존 API를 단독 폐기하지 않는다.

## 2. 반드시 읽어야 하는 내용

### 2.1 사용자 승인과 최초 출시 전제

사용자는 이번이 최초 결제 출시이며 기존 Billing 구매 계정·purchaseAccountRefId 발급·실제 거래가 없다고 확인했다. 외부 DB/Store 독립 조사 결과라고 표현하지 않는다.

- 과거 전체 탈퇴 이력의 legacy COMPLETE 증명은 최초 출시 필수 조건에서 제외한다.
- 증거 없는 legacy는 PARTIAL/UNKNOWN을 유지하고, 기존 Identity 탈퇴자를 snapshot에서 제외하거나 빈 목록으로 만들지 않는다.
- 결제 시작 기준점과 그 이후 탈퇴의 capture·전달·복구 연속성을 검증한다. 이후 GAP/증거 소실은 예외로 통과시키지 않는다.
- 새 기준점이나 snapshot 완료가 사라진 과거 eventId 수신 증거를 복원한 것은 아니다.
- 실제 배포·판매·자동삭제 활성화는 별도다.200·201·202 기능을 임의로 제거하지 않는다.

### 2.2 제거하는 ACK와 유지하는 응답을 구분

**제거 방향:** Billing이 snapshot/feed 반영 후 Identity에 별도로 보내던 BASELINE/RESYNC/FEED checkpoint ACK.

**유지:** Identity202의 실시간 UserWithdrawn push, Billing201의 멱등 적용·DB commit 뒤204 응답, 기존4필드 event 및204-only 성공 계약.

feed가 먼저 반영한 event가 나중에 push로 오면 inbox NOOP로 수렴한다. Identity delivery는 여전히 실제 push 응답으로 전진하며, feed 반영을 추측해 DELIVERED로 바꾸지 않는다.

### 2.3 핵심 책임 이동

Identity는 Billing이 어디까지 적용했는지 권위 있는 기록을 보유하지 않는다. 적용 진행과 consumer coverage의 authority는 Billing이다. 따라서 remote 완료값을 가져와 local checkpoint를 승격시키는 처리도 없다.

이 변경은 단순한 검사 삭제가 아니다. **Billing은 적용과 진행 저장의 원자성, local epoch 쓰기 fence, 복원 시 구worker 격리**를 보장해야 한다. lease 만료나 프로세스 재시작만으로 이를 대체하지 않는다.

## 3. Billing의 결정·회신 요청

새 제품 정책을 반복 결정하기보다 다음 기술안을 확인해 달라.

1. local checkpoint/epoch/consumer coverage의 단독 관리 책임을 수락할 수 있는가? 적용 데이터·inbox·진행점의 같은 transaction 및 일관 backup/restore 범위를 명시해 달라.
2. ACK worker/outbox·remote confirmation 상태를 제외하고, 현재 scan은 local control/job에 통합하도록204 계획을 개정해 달라. 별도 저장이 필요한 최소 operation/감사 metadata는 보존 근거와 함께 제시해 달라.
3. source availability 응답의 필수 정보·크기 제한·GAP 분류와205 사용 조건을 합의해 달라. 정확 필드 이름은 아직 동결하지 않았다.
4. local restore epoch 설치·구worker DB 접근 제거·stream 변경 감지·재개 조건을 정리해 달라. checkpoint 유실을 원격 값으로 복구하는 방식은 사용하지 않는다.
5. 공동 계약/TMI-199 exact/API·DTO·index·fixture 및204/205 참조 중 수정 대상을 정리해 달라. 기존 client/배포가 있는지 확인하고 revision/전환 방식을 합의해 달라.

권장 회신 형식: **수락/보완 필요 항목 →204 저장/transaction 경계 → exact 변경안 → 실패/복원 테스트 → 남은 운영 gate.**

원격 세대 제거는 Billing 기술 수락과 새 계약 동결 후 적용한다. 기존 이름의 consumerRecoveryGeneration을 로컬 epoch라는 다른 의미로 조용히 재사용하지 않는다.

## 4. 주요 위험과 제한

- Billing의 높은 checkpoint만 남고 실제 반영 데이터가 사라진 선택적/불명 복원은 진행점 신뢰를 중단해야 한다.
- 복원된 DB에 이전 epoch·lease가 남아 있어도 이를 현재 권한으로 간주하지 않는다. 구프로세스 접근 제거 후 새 local epoch를 설치한다.
- Identity도 복원된 경우 새 source stream을 사용한다. 서로 다른 stream 순번을 비교하거나 옛 checkpoint를 복사하지 않는다. 옛 source endpoint도 격리해야 한다.
- 원천과 필요한 User tombstone이 모두 사라지면 완전 복구할 수 없다. snapshot으로 상태를 수렴시켜도 원 event 누락은 별도 기록한다.
- Identity가 consumer 적용 상태를 모르므로 Billing 정지 중 consumer별 GAP 판단은 늦어진다. 독립 heartbeat와 source 보존 임박 경보가 필요하다.
- 비동기 전달 지연 중 이미 발급된 token이 즉시 무효화된다고 가정하지 않는다. 단순 lag의 판매 정책과 실제 탈퇴 차단 실패/복원 정책은 구분한다.

## 5. 양 서버 구현 흐름과 범위

### 5.1 정상 처리

1. Identity: 탈퇴·journal·sequence·delivery를 원자 저장하고 push한다.
2. Billing: 기존 inbox/applier로 반영한 뒤204를 반환한다.
3. Billing204: local checkpoint C 이후 feed를 주기 조회한다. 요청 전 epoch·lease/version·C를 context에 고정한다.
4. 응답 검증 후 **현재 local epoch/control CAS 쓰기 → event/inbox 적용 → 연속 checkpoint/job 진행**을 같은 transaction으로 commit한다. DUPLICATE/NOOP도 fence를 우회하지 않는다.
5. 원격 ACK 없이 다음 페이지로 진행한다. commit 뒤 crash면 durable C를 확인하고, 결과 불명이면 반영/진행 증거를 대사한 뒤 재조회한다.

### 5.2 snapshot·feed는 유지

- 초기화/기준점 상실 등에 snapshot을 사용하며 평소 poll마다 만들지 않는다.
- Identity는 동일 T의 User WITHDRAWN과 H를 읽어 불변 rows/count/digest/page를 만든다.5분 build budget,부분 READY 공개 금지,실패·만료와 멱등 key 구분은 유지한다.
- Billing은 page 적용+진행을 transaction으로 저장하고 전체 count/order/digest 검증 후 local baseline H를 확정한다. remote baseline ACK를 기다리지 않는다.
- H 이후 feed는 고정 H2까지 연속으로 조회한다. H2는 해당 scan의 관측 끝점이지 판매/삭제 자동 허가가 아니다.
- scan cursor1시간·소유/stream/서명·범위 검증 유지. 만료하면 현재 local C에서 새 scan을 열며 옛 scan 완료를 주장하지 않는다.
- snapshot facts에 임의 eventId를 만들지 않는다. 기존 digest canonical/숫자 fixture와 최대20건·16KiB 규칙은 재사용한다.

일반 live scan으로 바꾸면 S 이전 탈퇴자의 tombstone이 조회 전에 삭제되는 경우 scan과 S 이후 feed에서 모두 빠질 수 있다. 삭제 barrier/추가 journal 등 새 장치가 필요하므로 이번에는 고정 T/H·digest를 유지한다.

### 5.3 복원·local epoch

- 일반 retry/재시작: 같은 epoch와 durable C에서 재개,새 운영 승인이나 원격 세대 발급 불필요.
- Billing restore: worker·관련 쓰기 격리/drain·구DB 접근 제거 후 새 local epoch 설치. 적용 데이터와 C가 일관 복원됐고 원천이 남아 있으면 C부터 replay한다.
- 선택적 복원/진행 증거 유실: C를 신뢰하지 않고 bootstrap/대사. snapshot 성공으로 과거 GAP을 자동 해제하지 않는다.
- source stream 교체: 다른 stream 숫자 비교 금지,새 baseline 절차와 source 공백 평가.
- 오래된 HTTP 결과를 새 epoch로 다시 포장하지 않는다. 요청 당시 epoch/job binding을 commit 시 검증한다. Identity에는 old ACK가 변경할 consumer 상태 자체를 두지 않는다.

### 5.4 source 정리와 Billing 판단

Identity cleanup은 source expiry·journal/delivery 삭제·replayFloor/unavailableIntervals를 같은 transaction으로 처리한다. prefix만 floor를 전진하고 중간 만료는 interval로 남긴다.

Billing은 local C와 필요한 가용 구간을 대조한다. C 이전 원천 만료는 이미 commit된 적용 증거를 무효화하지 않는다. 필요한 구간 소실은 GAP로 남기고 같은 요청을 무한 반복하지 않는다.

- 원천:capturedAt+120일. ACK가 없다고 늘리거나 있다고 조기 삭제하지 않는다.
- snapshot rows/digest/staging/cursor 매핑:run 시작+7일 이내.
- 필요한 최소 operation metadata:생성+120일.
- 현 stream 최소 source/consumer 증거:각 주체가 필요한 최소 범위 유지,폐기 stream metadata 종료+120일.
- 기존 clock/capture bound·유효 lease·cleanup 권한·개인정보 비로깅 유지. User tombstone/205 금융자료 보존은 변경하지 않는다.

Identity202의 Evidence port가 원격 consumer generation/coverage를 요구하고 있으므로 이를 source 가용성 책임으로 개정할 예정이다. 코드 변경은 아직 하지 않았다.

### 5.5 오류·제한 구분

| 상태 | 복구 처리 | 판매·205 삭제 |
| --- | --- | --- |
| 출시 이전 legacy 불명 | PARTIAL/UNKNOWN 유지,완전성만 위한 반복 없음 | 최초 출시 단독 blocker 아님,후보별 근거 별도 |
| 일시 network/5xx·lag | 같은 job/C 자동 backoff+jitter,장기화 장애 단위 경보 | 정상 사용자 전체 구매/무료 자동 차단 금지,삭제 증거 부족 후보 보류 |
|401/403·계약 오류 | 빠른 반복 중단,권한/배포 장애 단위 조치 | 실제 수신/탈퇴 차단 손상 범위 평가 |
| GAP/원천 만료 | 범위·원인 기록,가능한 상태 복구,무한 동일 retry 금지 | 영향 범위 안전성 판단,특정 불가하면 넓은 삭제 보류 |
| DB 복원/증거 소실 | 격리/barrier·새 epoch/필요시 stream·대사 | 영향 구매 쓰기/삭제는 근거 확인 후 재개 |

204 retry budget·느린 probe의 정확 값은 공동 개정에서 동결한다.202 기존20회/관리 재개 정책을 이번에 변경하지 않는다.205 매시간 점검·매일 재확인·7일 검토는 모든 feed 실패의 건별 승인 규칙이 아니다. 무기한 개인정보 보존은 승인하지 않는다.

## 6. 부록 — exact 영향·검증·근거

### 6.1 exact 변경 목록

| 구규격 | 개정 방향 | 동결 필요 |
| --- | --- | --- |
| recoveries/checkpoints/checkpoints-status | Identity 구현 범위 제외 | 기존 배포/client 조사·revision 전환 |
| X-Consumer-Recovery-Generation | 제거 | local epoch는 Billing 내부,인증 대체 아님 |
| manifest/cursor generation | 제거 | source/run/소유자 binding 유지 |
| snapshot operation scope | environment/consumer/stream/operation key | 같은 key scope 충돌·응답 유실 exact 처리 |
| snapshot create/status/page | 유지 | 원격 세대 없는 DTO/오류·필수 nullable 규칙 |
| feed page | H2/cursor/연속성 유지 | bounded availability summary·관측 일관성·16KiB overflow |
| remote verified baseline/feed/coverage | Billing local로 이동 | source legacy와 consumer 적용 상태 혼동 금지 |
| Identity remote3개 컬렉션/index | 신규 구현 제외 | 운영 존재 미확인,drop 지시 아님 |
| Identity13개 manifest | I2의7개+snapshot3개 중심 | operation unique key 변경·새 hash/fixture,성능 index 별도 |
| Billing ACK jobs·별도 scans | 제외/통합 | local control/job·snapshot/staging/receipts·필요 최소 operation 설계 |

가짜204로 구client를 성공시키거나 같은 API를 양 의미로 혼용하지 않는다. 새 revision schema/error/index fixture를 만들고 기존 event/digest/numeric golden은 재사용한다. 동일 이름 index key 변경은 명시 migration 검토,자동 drop/recreate 금지. IAM/서명·direct ingress 차단과 strict 입력 검증은 유지한다.

### 6.2 공동 필수 테스트

1. 동일 T/H·늦은 commit·동일 timestamp: snapshot 이후 feed에서 빠지지 않음.
2. push/feed/page 중복·내용 충돌: NOOP는 안전,충돌 시 checkpoint 진행 금지.
3. 적용 뒤 crash/unknown commit: 적용과 C 함께 저장,외부 ACK 없이 재조회·재개.
4. local checkpoint 유실/부분 restore: 미반영 구간 skip 금지,remote 값으로 승격하지 않음.
5. 새 local epoch/lease 재할당 뒤 old response·NOOP·account/push 쓰기 CAS 거절.
6. source 중간 만료·prefix floor·cleanup 동시 조회: H2 전 빈 완료 금지.
7. snapshot history loss/expiry/부분 rows/digest 불일치: baseline 확정 금지,WITHDRAWN 역전 금지.
8. source 새 stream·Billing restore 격리: cross-stream 비교 금지,restore 불명은 정상 lag로 처리하지 않음.
9. legacy 불명·일시 lag만으로 전역 판매 차단 금지,실제 GAP/복원과205 보류는 유지.
10.7일/120일 보존·auth·strict16KiB/20건·cursor expiry·비로깅·기존 token/LC/push 회귀.

### 6.3 기준 문서와 현재 상태

- Identity 현행 계획: [TMI-203](TMI-203-withdrawal-recovery-snapshot-feed-plan.md).
- 선택지 비교/비용·한계: [단순화 검토 회신](TMI-203-204-recovery-simplification-review.md).
- Billing 현 계획: [TMI-204](../../../billing/docs/plans/TMI-204-withdrawal-recovery.md).
- 공동 원문: [기술 계약](../../../billing/docs/contracts/IDENTITY-BILLING-PAYMENT-LIFECYCLE-TECHNICAL-CONTRACT.md), [exact wire/index](../../../billing/docs/contracts/TMI-199-EXACT-WIRE-AND-INDEX-SPEC.md).

이 링크들은 현재 Identity 저장소 기준이다. 문서를 Billing 저장소에 옮길 때 상대경로를 조정해야 한다. 문서만 전달하는 경우 위 문서명으로 찾으면 된다.

실제 Mongo/IAM/복원 실험, Billing 코드·배포 상태는 이번 인계 작성에서 재검증하지 않았다. 이번 변경은 인계서·WORKLOG·CURRENT_STATE뿐이며 기존 계획/exact/코드·DB·배포·Jira 변경 없음. 문서 링크·diff 정적 검사,Gradle 미실행(문서 작업).

### 6.4 Jira 댓글 초안 — 미등록

Billing204 전달용 인계서 작성. 승인된 local checkpoint/epoch 책임 이동,remote ACK 제외·fixed snapshot/feed 유지,exact/index/fixture 개정 항목과restore/cleanup/205 검증을 정리. 변경은 인계서/작업기록3파일,문서 정적 검증만 수행. Billing 기술 수락·공동 규격 동결 전 구현 불가,실인프라 미검증. Jira·코드·배포 변경 없음.
