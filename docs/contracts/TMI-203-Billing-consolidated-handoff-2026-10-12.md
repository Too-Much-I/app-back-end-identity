# Billing 전달 — TMI-203·204 단일 진행 관리 최종 방향 정리

- 작성일: 2026-10-12 / Identity develop / TMI-203·204, 영향199·202·205·206.
- 상태: **사용자 승인 방향 및 Billing 회신을 반영한 통합 인계. 세부 exact·index·fixture 공동 동결, 구현·배포·판매·자동삭제 승인은 별도다.**
- 이 문서가 이전 전달문 중 **v2 URL 분리 권장과 source interval 미선택 상태**를 대체한다. 기존 검토·수락 이력과199 fixture는 삭제하지 않는다.

## 1. 5줄 결론

1. Identity는 durable 탈퇴 원천·snapshot/feed·재생 가능 범위를 제공하고, Billing만 적용 checkpoint·local epoch·consumer coverage를 관리한다.
2. 복구 API는 아직 구현하지 않았다는 사용자 전제에 따라 **기존 예정 `/internal/v1/billing/withdrawals`를 사용**하며 v2 URL을 추가하지 않는다. 문서 revision은 별도 관리한다.
3. 원천 소실은 **기존 replayFloor + 별도 중간 소실 구간 문서**로 저장하고, 인접 구간 병합·prefix 편입으로 정리하는 방향을 사용자가 채택했다.
4. 고정 T/H snapshot·digest·H2 feed, Billing local commit/fence·restore 격리, 기존202 push204·보존·token 보호는 유지한다.
5. Identity는 source 구간 저장 세부 schema/index를 정리하고, Billing은204 pull 재시도·circuit·경보·운영 권한 값을 제안해 달라. 기존202 재시도 정책은 변경하지 않는다.

## 2. 반드시 읽어야 하는 승인 범위

### 2.1 최초 결제 출시

사용자는 기존 Billing 구매 계정·purchaseAccountRefId 발급·실제 거래가 없다고 확인했다. 과거 전체 탈퇴 이력의 legacy COMPLETE 증명은 최초 출시 필수 조건에서 제외한다. 기존 대상 부재는 사용자 전제이며 실환경 독립 조회 결과가 아니다.

기존 Identity 탈퇴 목록은 snapshot에 그대로 포함한다. 증거 없는 과거는 PARTIAL/UNKNOWN으로 유지하고 결제 시작 기준점 이후 capture·전달·복구 연속성을 검증한다. 새 snapshot이나 기준점으로 원event GAP을 지우지 않는다.205 거래 확인·탈퇴 근거·보존 정책은 유지한다.

### 2.2 주소와 계약 revision

새 복구 API를 최초 구현하는 것이므로 구client 호환만을 위한 `/v2` 분리는 하지 않는다. 사용할 예정 prefix는 `/internal/v1/billing/withdrawals`,기능은 다음4개다.

- POST `/snapshots` — streamId body와 Idempotency-Key로 run 생성/재조회
- POST `/snapshots/status` — 생성 상태와 READY manifest
- POST `/snapshots/page` — 완성된 고정 목록 조회
- POST `/feed/page` — 고정 H2까지 연속 사건 조회

snapshot 생성은 DB에 run을 만드는 POST이므로 네 기능 전체를 무상태 read-only라고 부르지 않는다. remote `/recoveries`, `/checkpoints`, `/checkpoints/status`와 X-Consumer-Recovery-Generation은 새 구현에서 제외한다.

URL v1 유지와 문서·fixture revision 보존은 별개다. 이전199의 body 없음/remote generation 계약을 새 규격으로 덮어쓴 뒤 과거 테스트 결과를 재사용하지 않는다. 정확 문서 revision 이름은 공동 정리하고 실제 예상과 달리 기존 consumer가 발견되면 그 영향 범위만 다시 검토한다.

## 3. 역할·회신 요청

### 3.1 Identity 담당

-202 탈퇴·journal·sequence·delivery 원자 저장,실시간 push·재시도 유지.
-203 고정 snapshot/feed·source availability 제공,소실 구간 저장과cleanup 원자성 구현 설계.
- 새 source range 컬렉션의 필드·index·bounded batch·보존/경합 테스트 정리. 이전 예상10index는 확정 총수가 아니며 추가 index를 명세에 반영한다.
-202 Evidence port에서 remote consumer 증거를 제거하고 source stream/expiry/floor/interval 책임으로 개정.코드 변경은 아직 하지 않았다.

### 3.2 Billing 담당

- local epoch/version CAS,기존 inbox/applier와 checkpoint/page receipt를 같은 ClientSession/Transaction으로 commit. HTTP는 밖에서 처리.
- local state baseline H와 이후 실제 원event 적용 범위(H,C],unresolved gap 구분. C 숫자만으로0~C 전체 사건을 처리했다고 해석하지 않음.
- control/job·snapshot run·staging rows·page receipts·최소 operation의5개 후보로 세부 schema를 정리. ACK jobs/remote confirmation/별도 scans는 제외.
- restore 시구worker DB 접근 제거·drain·새 local epoch 설치.일관 복원된 적용 데이터/C만 신뢰,선택적/불명 복원은 bootstrap/대사.

### 3.3 Billing에 요청하는 운영값

다음은 **TMI-204 pull/복구 job 기준**으로 권장값을 제안해 달라.

1. 정상 feed poll 간격·빈 결과 시 다음 poll 간격.
2. network/408/425/429/5xx 재시도의 최소/최대 지연·jitter·budget·Retry-After 처리.
3. budget 소진 후 circuit/probe 간격·복구 조건·중복 경보 억제 기준.
4. snapshot BUILDING poll과 실패 횟수 분리,실패/만료 run의 bounded 자동 재생성 기준.
5. worker heartbeat/lag·source 만료 임박의 경보 기준·독립 watchdog·수신 담당.
6. local epoch 설치/restore 운영 권한과 실제 상태 소실 때의 재개 조건.

정상 일시 오류는 자동 재시도·장애 단위 경보이며 사용자별 수동 승인으로 만들지 않는다.401/403·계약 오류·확정 GAP은 무조건 반복하지 않는다. 기존202 publisher20회/관리 재개 정책과205 매시간·매일·7일 검토를204 규칙으로 섞지 않는다.

## 4. 위험·보존·제한

- Identity 가용성은 원천 존재 증거이지 Billing 적용 증거가 아니다. source 만료가 적용 취소를 뜻하지 않는다.
- 일시 lag만으로 정상 사용자 전체 구매/무료 기능을 자동 차단하지 않는다. 실제 capture/탈퇴 차단 실패·GAP·restore는 별도 영향 평가와 barrier 대상이다.
-205는 local baseline/event 범위/gap·provider 거래 근거를 함께 판단한다. 대상 특정이 어려우면 삭제 보류 범위가 넓어질 수 있다.
- capturedAt+120일 원천,run 시작+7일 snapshot 사용자 자료/digest,최소 operation120일,현 stream 최소 증거 및 폐기 stream 종료+120일을 유지한다.
- 자료 저장 장애로 삭제를 rollback하는 것은 승인 없는 보존 연장 정책이 아니다. 재시도·기한 경보·운영 조치로 다루고 무기한 방치하거나 증거 없이 삭제하지 않는다.
- 실제 구현/Mongo/IAM/복원·배포 검증은 미완료이며 이 인계가 이를 대신하지 않는다.

## 5. 채택한 source 소실 구간 설계

### 5.1 저장

기존 stream의 replayFloor는 **처음부터 연속으로 원본을 재전송할 수 없는 마지막 순번**이다. 그 뒤 중간에 생긴 소실은 별도 컬렉션에 구간별 문서로 저장한다. 하나의 stream 문서에 무한 배열을 붙이지 않는다.

예: replayFloor=100,중간 range=[105,108]이면101~104는 남아 있을 수 있다. 따라서 floor를108로 올리지 않는다. 나중에101~104도 소실되면 floor=108로 편입하고 중복 range 문서를 제거한다. 인접/겹치는 구간은 병합하되 정상 가용 구간을 건너뛰어 합치지 않는다.

구간 문서는 streamId·시작/끝 순번 등 최소 가용성 증거만 담고 userId·event 원문·token을 복제하지 않는다. 컬렉션/field/index 정확 이름은 Identity 세부 명세에서 제시한다. 새 collection/index 추가는 설계 선택이며 운영 DDL 실행 승인이 아니다.

### 5.2 원자성·동시성

원천 journal/delivery 삭제와 range/floor 갱신을 같은 transaction으로 처리한다. source version/CAS로 동시 cleanup·stream 교체와 경합시키고 결과 불명 시 증거 확인 없이 범위를 전진시키지 않는다. 유효 lease·clock/capture bound 보호는 유지한다.

배치와 transaction의 처리량을 제한한다. 한 번에 모든 range를 합치지 않아도되며정확한 증거가 남아 있는 상태에서 후속 배치로 이어간다. range 수가 많다고 임의 절삭하지 않는다. 필드/조회index·경보 임계값·정규화 batch 크기와 retired stream 정리는 구현 전 명세/테스트에서 확정한다.

### 5.3 feed와 Billing의 사용

Identity는 한 페이지의 일관 읽기에서 stream/head/floor/range/journal을 확인한다. 성공 availability는 그 페이지 범위만 설명하고,410은 요청의 최초 소실 위치와 확정 range1개를 제공한다. 전체 interval을 응답에 담지 않는다.

Billing은 필요 범위와 교차해판단한다. C 이전 만료는 기존 적용 증거를 무효화하지 않는다. 필요한 원천이 소실됐으면GAP을 기록하며같은 요청 무한 retry를 중단한다. snapshot 상태복구만으로 누락된 원event 구간이나205 보류를 자동 해제하지 않는다.

## 6. API 후보·검증·후속 문서

### 6.1 앞선 Billing 보완안 반영

아래는 직전 후보에 반영된 세부안이다. 사용자 방향 승인과 **양 서버 exact 전 항목 동결**은 구분한다.

| 항목 | 정리된 방향/후보 |
| --- | --- |
| snapshot 생성 | 명시 streamId body,unique(environment,consumer,requestOperationId)와canonical body stream binding |
| 충돌 순서 | 인증/strict →key binding mismatch409 OPERATION_CONFLICT →inactive stream409 STREAM_REPLACED |
| run 보존 |7일후EXPIRED 최소상태,operation120일까지 동일key 재생성 금지;metadata 제거후404 후보 |
| snapshot | 동일T/H,불변rows/count/digest,5분build·부분공개금지,READY 뒤local baseline 확정 |
| feed | 고정H2·연속 sequence,후보page범위20 안hole이면전체오류/C무변경 |
| cursor | 마지막feed도non-null,H2 binding/TTL내중복조회용,ACK아님;TTL1시간 |
| availability | 이번page만검사,head전체가용성선언아님;410 최초range1개,409 range=null,503 details생략 |
| 논리만료 | 물리row잔존이어도expiry지나면410,singleton range근거 인정 후보 |
| 크기 |request/response16KiB·page20,응답생성초과500 RESPONSE_LIMIT_EXCEEDED 후보;잘린JSON/빈성공금지 |

기존 push/event schemaVersion=1·4필드·commit후204와canonical digest/numeric 규칙은 유지한다. operation index 이름의 revision 표기는 URL 버전과 별개이며 이전 `_v2` 후보명을 최종 이름으로 확정한 것은 아니다.

### 6.2 공동 테스트

- 동시 탈퇴/late commit/같은 timestamp가snapshot과feed 사이에서빠지지 않음.
- range 인접병합·prefix편입·중간가용구간보존·cleanup rollback/동시성·MAX_VALUE.
- logical expiry/410·확정hole409·불명503,페이지부분성공금지·metadata모순시C무변경.
- local apply+C 원자성·unknown commit·중복push/feed/NOOP·stale epoch·restore.
-7일/120일/key/stream경계·strict/auth/16KiB·source와consumer증거분리·205보류.

이전199fixture/hash/검증이력은보존하고새문서revision별schema/index/fixture를생성한다. 실제테스트통과는구현뒤별도보고한다.

### 6.3 다음 작업과 전달 기준

1. Billing은 §3.3 운영값과local복원/감시책임을 회신한다.
2. Identity는 채택된floor+별도range의schema/index·batch/보존조건을 구체화한다.
3. 양측은 **v1주소의최초구현 규격**으로 DTO/error/index/fixture를정리하고공동본문·204/205/206을같은revision으로연결한다.
4. 이후 별도구현요청으로203·204 개발/검증을진행한다. 이번에는코드/DB/Jira/배포/타채팅메시지를변경하지않는다.

참고문서는 [현행203계획](TMI-203-withdrawal-recovery-snapshot-feed-plan.md), [기술후보](TMI-203-recovery-v2-exact-candidate.md), [기존인계](TMI-203-to-Billing-TMI-204-simplified-recovery-handoff.md)다. 기술후보의파일명/v2/index예상총수는작성당시후보이며 **주소는이문서의v1,range는이문서의채택방향**이우선한다. 세부명세까지확정됐다는의미는아니다.

### 6.4 이번 작업·Jira 초안

문서 정리만 수행했다.로컬링크/diff검사,Gradle·실Mongo·실배포검증미실행. 인계작성은실제전송이나상대수락을뜻하지않는다.

Jira 댓글 초안(미등록): 사용자 채택 floor+중간range,미구현v1유지,Billing204 운영값요청을통합인계에정리. 기존capture/push·보존/local fence유지,range schema/index와공동exact는후속동결.문서/기록만변경·정적검사,코드/DB/배포변경없음.
