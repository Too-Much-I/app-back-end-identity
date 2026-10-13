# TMI-203·204 recovery-r1: v1 복구 API·저장·조회 exact 제안

> 2026-10-12 `scalar-end-1` 보완: 공유 schema8개pattern의끝개행허용차이를수정했다. 신규scalar-cases와갱신hash는 [보완 인계](TMI-203-recovery-r1-scalar-end-correction.md)를따른다. 이전아래테스트수치는작성당시결과이며최신검증결과는보완문서/WORKLOG에기록한다. Billing 재검증·공동수락은별도다.

- 작성일: 2026-10-12 / 브랜치: `feat/TMI-203-withdrawal-snapshot-feed-recovery`
- Jira: TMI-203, 연계 TMI-204·199·202·205·206
- 상태: **Identity 측 구현·공통 검증용 규격 작성본. Billing의 이 revision 독립 검증/수락 전이며 공동 동결·API 배포 완료가 아니다.**
- HTTP prefix: `/internal/v1/billing/withdrawals`. `recovery-r1`은 문서/fixture revision이며 event schemaVersion=1이나 URL v1을 바꾸지 않는다.
- 기준: [통합 인계](TMI-203-Billing-consolidated-handoff-2026-10-12.md), [내부 코어 구현](TMI-203-core-implementation-and-freeze-gates.md), 사용자 첨부 Billing 후속 회신 §7.
- 이 작성본이 이전 v2 후보/예상10·12 index 표의 **새 검토 대상**이다. 기존199 수락 이력·fixture를 소급 교체하지 않는다.

## 1. 5줄 결론

1. 네 POST의 정확 shape·오류 message·nullable/생략을 [wire schema](../../src/test/resources/contracts/payment-lifecycle/recovery-r1/wire.schema.json)와 [오류표](../../src/test/resources/contracts/payment-lifecycle/recovery-r1/errors.json)에 고정 제안했다.
2. FAILED page는409 SNAPSHOT_FAILED/false,429는 인프라 body 비가정/Retry-After 준수로 정리한다. 임의 앱 요청 횟수 제한은 추가하지 않는다.
3. range 정리는 최대100구간,feed는 최대20순번별 containment 조회로 분리한다. 전체100개만 읽고 없는 것으로 판단하지 않는다.
4. source CAS·정확 키 선조회·선택 원본 조건·bounded 재시도로 같은 from 충돌을 수렴시키며 원본 삭제와 증거 갱신은 원자 commit 대상이다.
5. 내부 codec/상태규칙/조회·변경 계획과 공통 fixture를 구현했다. 실제 Mongo adapter/HTTP controller/고정T builder/IAM/배포는 아직 없다.

## 2. 반드시 읽어야 하는 내용

### 2.1 정확 규격의 기준 파일

[recovery-r1 폴더](../../src/test/resources/contracts/payment-lifecycle/recovery-r1/)의 다음 파일이 검토 단위다.

- `wire.schema.json`: JSON Schema draft2020-12의 route별 `$defs`. 루트 자체로 검증하지 말고 대상 `$defs`를 선택한다.
- `request-cases.json`: raw JSON 문자열. 중복 키·trailing JSON은 파싱 전 정보이므로 일반 JSON Schema만으로 검사할 수 없다.
- `response-cases.json`: 상태별 성공/실패·숫자 경계의 schema 예제.
- `errors.json`: HTTP status,code,message,retryable,details 규칙. wire에는 status 필드를 추가하지 않는다.
- `snapshot-state-cases.json`: 멱등 binding→stream→expiry→run 상태 우선순위.
- `range-cases.json`: 101번째 소실·100개초과겹침·MAX·prefix/가용구간 보존의 언어 독립 입력/기대값과 남은 adapter 실험 목록.
- `identity-indexes.json`: **명시 index16개**(기능12+보조4),각 collection의 기본 `_id_` 별도. 선언/검증 대상이지 운영 initializer가 아니다.

필드 간 부등식·연속성·시각 canonical·body byte limit·duplicate/trailing·실제 인증은 JSON Schema를 통과한 뒤에도 검증해야 한다. schema 성공을 API/트랜잭션 안전성의 증명으로 사용하지 않는다.

### 2.2 변경 없는 경계

Identity는 source/가용성만 소유한다. Billing은 local checkpoint/epoch/coverage를 단독 소유한다. recoveries/checkpoints/checkpoints-status 및 consumer generation header는 없다. 기존202 push4필드/204·재시도/resumeGeneration과 LC·token·무료 권리·보존 정책은 변경하지 않는다.

사용자 확인상 과거 결제 대상 없음으로 legacy COMPLETE 증명이 최초 출시 필수 조건에서 빠졌지만 기존 Identity 탈퇴 목록을 비우지 않는다. UNKNOWN/PARTIAL을 COMPLETE로 만들지 않는다. snapshot은 상태 baseline일 뿐 이전 원event 누락을 해소한 증거가 아니다.

## 3. 사용자가 결정해야 하는 사항

추가 제품 정책 선택은 없다. Billing은 이 revision의 파일과 아래 exact를 독립 검증하고 수락/불일치를 회신하면 된다. 코드 구현 요청을 상대 수락·실환경 검증·활성화 승인으로 확대하지 않는다.

새롭게 구체화한 항목은 cursor 입력 상한2048 ASCII문자,correlationId lowercase UUIDv4,operation index `_r1` 이름,보조index4개,source transaction conflict 재시도 최대3회다. 내부 구현 제안으로 명시하며 상대 확인 전 외부 endpoint를 열지 않는다.

## 4. 주요 위험과 미확인

- index16개는 이 query 설계의 선언본이다. 실제 query plan/부하/Mongo DDL 검증 결과가 아니다. scan이 비싸면 timeout을 늘리거나 결과를 잘라 성공시키지 말고 index/query를 revision으로 보완한다.
- `PageRangeWitnesses`가 같은 snapshot을 강제하는 DB 구현은 아직 없다. port는 일관성 의무를 명시하지만 mock/in-memory 테스트가 replica-set 증거를 대신하지 않는다.
- `RangeMutationPlan`은 원본/대체 범위 계산만 한다. DB rollback/unknown commit/lease/capture 동시성은 production adapter에서 별도 검증해야 한다.
- 고정T snapshot,5분 history budget·재시작·7일 자료 삭제,120일 최소 metadata,source restore·IAM/직접 ingress 차단은 미구현 또는 운영 검증 미완료다.
- 애플리케이션 codec은 인증 장치가 아니다. 사용자JWT/임의 consumer 헤더로 서비스 주체를 대신하지 않는다.

## 5. exact 본문

### 5.1 공통 입력/보안

모두 POST,UTF8 JSON만 사용하며 raw request/serialized response 각각≤16,384 bytes. decoder에 넘기기 **전에** HTTP adapter가 bounded stream read로 상한을 적용해야 한다. duplicate key/trailing root/추가 필드/누락/타입 coercion/BOM·잘못된UTF8는400. public BaseResponse 및 전역 Jackson 설정은 변경하지 않는다.

새 stream/snapshot/operation/correlation ID는 canonical lowercase UUIDv4/RFC variant2,사용자 및 기존 event ID는 canonical UUID로 버전 제한 없음. D0/D1은 signed int64 범위의 canonical 십진 **문자열**,U32는0..4294967295 문자열. `schemaVersion`은 JSON integer1이며 `1.0` coercion 불가. UTC 시각은 `Instant.parse(value).toString().equals(value)`로 확인한다. 기존 timestamp nanos를 자르지 않는다.

cursor는 opaque printable ASCII(`!`..`~`)1..2048자. nullable 키는 반드시 존재해야 한다. codec은 서명을 검증하지 않으며 production adapter가 인증된 environment/consumer·stream/run·시작/목표·발급/만료 바인딩을 검증해야 한다. 전용 HMAC key ring·안전한비교·TTL1시간이며 JWT/문의용 Secret을 재사용하지 않는다. 원문 cursor/token을 로그에 남기지 않는다.

IAM/SigV4 및 환경/consumer/route 권한은 앞단에서 검증하고 직접 ingress를 차단한다.401/403/429 인프라 응답은 앱 JSON으로 가정하지 않는다. redirect는 따라가지 않는다. header cardinality(Idempotency-Key 중복 등)는 HTTP adapter가 strict하게 검증한다.

### 5.2 네 API와 응답

| suffix | request definition | 성공 |
| --- | --- | --- |
| `/snapshots` | CreateRequest + Idempotency-Key UUIDv4 | 신규/BUILDING202 SnapshotStatus +Retry-After:5,기타 기존run200 |
| `/snapshots/status` | StatusRequest |200 SnapshotStatus,BUILDING이면Retry-After:5 |
| `/snapshots/page` | SnapshotPageRequest |200 SnapshotPage |
| `/feed/page` | FeedPageRequest |200 FeedPage |

상태 응답은 snapshotId/status 공통. READY만manifest,FAILED만failureCode를 포함하며 다른 상태는 해당키를 **생략**한다. failureCode는 SNAPSHOT_HISTORY_LOST/BUILD_TIMEOUT/BUILD_FAILED의 full code3종이다.

snapshot item은 userId/withdrawnAt만,feed item은 sequence와 기존4필드event다. 마지막snapshot nextCursor는null/done=true,마지막feed scanCursor는non-null이다. latter는 H2 고정과TTL내반복조회용이며 ACK가 아니다. Snapshot page의 done=false이면nextCursor non-null,done=true이면null이며 전체순서/count/digest와pageCount를대조한다. 빈snapshot은1개빈최종page다.

### 5.3 멱등성과 만료 우선순위

인증/strict→기존 `(environment,consumer,requestOperationId)` binding→active stream→논리expiry→run상태 순이다. stream은 unique 구성요소가 아니라 요청 body binding이다. scope는ALL_WITHDRAWN 고정이다.

- 같은key/newstream은OPERATION_CONFLICT가먼저. 같은key/oldbody인데inactive이면STREAM_REPLACED.
- status/page는인증된owner가볼수있는record만조회한다. 없으면SNAPSHOT_NOT_FOUND이며타owner정보를공개하지않는다.
- 동시create의duplicate-key는기존binding을읽어같은snapshotId로수렴한다. 불명commit은새key로우회하지않는다.
- run시작+7일은privacyExpiresAt,최초operation생성+120일은operationExpiresAt. 후속요청으로기한을갱신하지않는다.
- privacy만료뒤currentstream create/status는200 EXPIRED,page는410 SNAPSHOT_EXPIRED. 이전FAILED도EXPIRED우선. 개인정보rows/manifest/digest/cursor매핑은7일이내정리하고최소binding/종료상태만120일까지유지한다.
- metadata제거뒤status/page는404. 새논리작업은항상새UUID. 기존120일기록없는key를영구기억한다고주장하지않는다.
- PAGE에서BUILDING은409 SNAPSHOT_NOT_READY/true,FAILED는409 SNAPSHOT_FAILED/false다. FAILED동일page영구polling금지,status원인과Billing기존새runbudget으로판단한다.

### 5.4 snapshot T/H와 digest

동일 Mongo atClusterTime T에서 WITHDRAWN User 목록과 source counter H를 읽는다. User와H를따로live조회하거나벽시계watermark로대체하지않는다. ASCII userId순,불변rows/count/digest검증뒤source/run version CAS로READY를seal한다. 일부build는공개하지않는다. build5분·history loss/재시작시같은T를보장못하면FAILED다.

digest는각row compactUTF8 `{"userId":"...","withdrawnAt":"..."}`의byte길이를unsigned32BE로앞에붙인전체stream의SHA256 lowerhex다. page경계영향없음,빈값은zero-byte SHA256. manifest가16KiB를넘으면BUILD_FAILED이며knownGaps를잘라READY로만들지않는다. pageCount는max(1,ceil(itemCount/20))로정수계산하고overflow를피한다.

### 5.5 feed 순번과 source 조회

첫 same-snapshot committedhead를H2로고정한다. after>head는400,이후유효cursor의startExclusive≤after≤H2를확인한다. 같은active stream head<H2는503 SOURCE_AVAILABILITY_UNCERTAIN,source교체는409 STREAM_REPLACED다.

E=after+min(20,H2-after). 검사대상은(after,E]전체이며결과수에맞춰E를줄이지않는다. after==H2일때만빈완료이고MAX_VALUE에서after+1을계산하지않는다. nextAfter=scannedThrough=E,done=(E==H2).

**한페이지의한짧은snapshot에서** active stream/head/floor/ranges/journal을읽는다. clock/timing근거가확인되지않으면가용성을확정하지않는다. rows는streamId와sequence범위로조회하고20건이내연속성과unique를검사한다.

range조회는정규화batch와별개다. 각필요순번s에대해다음조건을먼저적용한뒤limit(1)한다.

```text
filter: streamId = currentStream AND fromSequence <= s AND toSequence >= s
order: fromSequence ASC, toSequence ASC, _id ASC
limit: 1
```

최대20번의점포함조회라전체range가101개이상이거나100개넘게겹쳐도해당점의증거를놓치지않는다. 일치1개면충분하고empty는해당snapshot에서실제없음이어야한다. DB에먼저limit100후애플리케이션에서filter하는구현은금지한다. timeout/권한실패/불명읽기를empty로변환하지않는다. 사용한같은range증거는중복제거해최대20개만코어에전달한다.

순번오름차순으로첫문제를반환한다. floor안이면410[1,floor],점조회range면410그range,존재row의capturedAt+120일경과면410singleton,range근거도row도없으면409 COVERAGE_GAP. 내부메타데이터모순/불명은503/details생략. 필요한최초hole보다뒤range를먼저반환하지않는다. 어느경우도정상prefix만성공시키지않는다.

### 5.6 source availability/error와429

success availability6키와sourceerror details6키는schema참조. 성공은이번page검사범위만증명한다. after≤E≤H2≤observedhead,floor≤observedhead,비어있지않은성공범위는floor뒤여야한다.

410은unavailableRange non-null,409 GAP은키필수null,503은details키생략이다. missing은이번필요page안이며410range안에포함되고range끝≤관측head여야한다. knownrange끝이H2보다큰것은가능하다. Billing은필요구간과교차해해석한다. 새snapshot으로과거GAP을해결했다고하지않는다.

정확status/code/message/retryable은errors.json을따른다. 응답byte초과는500 RESPONSE_LIMIT_EXCEEDED/false이며413이나빈성공이아니다. serialization을끝낸뒤size를확인한후에만HTTP에쓴다. code별고정message에예외/개인정보/Secret을삽입하지않는다.

429에는이revision의앱code/JSON을필수로하지않는다. Billing은HTTP status로분류하고유효Retry-After정수초/HTTP-date를backoff보다앞당기지않는다. 무효/모호헤더는원문비로깅경보+기본backoff.24시간초과는기존Billing운영안대로PAUSED_UNTIL이며즉시반복하지않는다. 앱rate limiter나새정책숫자를이번에추가하지않는다.

### 5.7 source 저장·원자 정리·100개 이후 진행

`withdrawal_unavailable_ranges`: `_id`내부문자열,streamId UUID,fromSequence/toSequence BSON int64,D1/from≤to≤관측head. user/event payload복제없음. stream에는명시이관한availabilityVersion int64≥0,closedAt(nullable ACTIVE,retired필수)추가안이다. missing/null/음수version은활성화차단,MAX overflow는쓰기거절. 암묵0전환/upsert금지.

cleanup은기존권한/clock/capture bound/120일expiry/유효lease검사를유지한다. 새seed=s singleton을추가할때:

1. 같은transaction에서active/승인된retired source의version·상태·head/floor를읽는다. `(streamId,fromSequence=s)` exact조회로기존동일키를먼저얻는다.
2. 후보기존range최대99개를선택한다. exact동일키가후보밖이면반드시포함하고다른후보1개를다음batch에남긴다. 신규seed포함총≤100이다.
3. 선택원본만정규화한다. 결과의from은선택원본from또는seedfrom중하나이므로exact선조회와함께선택밖동일키충돌을방지한다. 기존증거/버전모순은불명오류이며반복으로숨기지않는다.
4. source version/status CAS,선택원본 `_id/streamId/from/to` 조건의삭제또는치환,newrange삽입,floor전진,journal/delivery삭제를같은transaction에넣는다. 영향건수가예상과다르면전체rollback. 선택밖문서삭제금지.
5. 동시writer/unique/write-conflict이면새snapshot에서exact조회와후보선택을**다시**한다. 최대3회,각시도전에budget확인,소진하면삭제없이일시오류/경보다. unknowncommit은같은seed·원본·증거를조회해확정전추가삭제하지않는다. 동일batch무한retry금지.

잔여정규화는stream별단조 `normalizationAfterSequence` D0와 `normalizationCarryFromSequence` D1|null 및availabilityVersion CAS로공정한sweep을수행한다. from>cursor순최대99개와직전batch의마지막결과range를exactcarry조회하여최대100개로처리한다. carry는batch경계를가로지르는인접/겹침을병합하는데필요하다. carry키가사라졌거나sourceversion/원본이바뀌면전체rollback후새상태로재계획하며,carry없는것을추측해삭제하지않는다. 안전한sweep재시작은cursor0/carrynull로명시CAS하되증거는보존한다.

완료cursor는새선택의최대from,carry는정규화결과마지막range의from(결과없으면null)이다. 새후속range가없으면cursor0/carrynull로한sweep을끝내prefix와뒤늦은낮은from삽입을다시다룬다. cursor=MAX도overflow없이종료/리셋한다. 같은99개가변화없더라도cursor가전진해뒤쪽을영구방치하지않는다. cursor/carry는가용성authority가아니며0초기화가증거를삭제하지않는다. 계산코어RangeSweepPlan은구현했으며실제scheduler/CAS는미구현이다. 기존자료의cursor0/carrynull설치도명시이관대상이다.

floor에이미포함된range만제거가능하고정상구간을건너뛴병합은금지한다. currentstream증거는필요최소범위유지,retiredstream은closedAt+120일후정리한다. source본문은capturedAt+120일,snapshot자료는run시작+7일,최소operation120일이다. 저장실패rollback은승인없는무기한보존근거가아니며경보/운영조치대상이다.

### 5.8 index와snapshot 최소 저장 경계

manifest는기존I2 7개그대로,새snapshot3개(멱등index이름`ux_withdrawal_snapshot_operation_r1`),range2개,보조4개로16개다. key순서/options는JSON원문이기준이다. 보조4개는snapshot privacyExpiresAt·operationExpiresAt·status/leaseUntil,retiredstream status/closedAt 조회다. 모든명시index TTL없음/simple/sparse=false,partial은active환경unique하나뿐이다.

snapshot run의새index필드는privacyExpiresAt/operationExpiresAt/status/leaseUntil이며시간은BSON Date. BUILDING 생성시leaseUntil을생성시각으로두어claim가능하게하고version/leaseOwner와함께CAS한다. run에는owner/binding/snapshotId/streamId/createdAt/2expiry/status/version,BUILDING lease/buildDeadline/T/H,READY manifest,FAILED code를상태별저장한다. privacy정리는자료제거후최소binding/EXPIRED로만바꾸고operation기한까지unique를유지한다. snapshot rows는snapshotId/userId unique와withdrawnAt canonical text로기존정밀도를보존한다. 물리키·세부worker필드는adapter작성때이의미를유지해문서에추가한다.

sourceVersion/closedAt/sweepcursor의기존자료이관은명시운영작업으로검증한다. `_id_`포함실index옵션과기존구unique충돌여부를진단하며startup은검증만한다. drop/recreate/운영DDL을이번작업에서실행하지않는다.

## 6. 부록 — 구현·테스트·인계

### 6.1 이번 변경

- RecoveryProtocol:별도strictcodec/고정error/UTF8·16KiB경계. 전역Jackson/HTTProute미변경.
- SnapshotAccessRules:멱등·stream·7일/120일lookup상태우선순위계산.
- PageRangeWitnesses:page별최대20점조회port. FeedPageVerifier범위입력은완전한점증거로명확화하고20개로제한.
- RangeMutationPlan:exact동일키우선포함·선택밖보존·최대99기존+seed변경계획. DB쓰기없음.
- RangeSweepPlan:변화없는앞부분을지나가는keyset진행·batch경계carry·MAX/끝reset. DB쓰기없음.
- 공통schema/fixture/index파일과테스트. JSONSchema검증용networknt1.5.6을**testImplementation**에만추가,production의존성추가없음.

### 6.2 검증과남은것

raw요청·schema/null/생략·D0/U32경계·오류message일치·상태우선순위·101번째range·100개초과겹침·동일from선택밖·부분batch/MAX를검증한다. 기존199fixture/hash는변경하지않고전체Gradle결과는작업기록에남긴다. Java테스트의in-memory범위조회/변경은실Mongo트랜잭션/성능검증이아니다.

최종 `env -u SENTRY_AUTH_TOKEN ./gradlew clean test`는 성공했다(24초). 총1,559개 중1,553통과/6skip/실패0이며 이번 추가67개는 모두통과했다. recovery-r1 JSON7개 SHA256 고정 검사 및 JSONSchema draft2020-12 검증을 포함한다. 로컬 Python 환경에는 jsonschema가없어설치하지않고Java test-only검증기를사용했다. `git diff --check`도통과했다.

시작시존재한dirty문서/내부코어/cleanup주석을보존했다. 이번변경은내부5클래스추가·FeedPageVerifier입력증거계약보완·3테스트·새규격묶음·계획링크·기록및testImplementation이다. 예상밖변경이나production설정/DB/Jira/배포/commit/push는없다.

다음: Billing은이폴더의같은revision을독립검증/수락→필요보완후공동본문연결→고정T실험/sourceCAS·보존adapter/signedcursor/HTTP인증연결→실Mongo/IAM/복원·staging. 현재runtimeAPI나배포완료로표시하지않는다.

### 6.3 Jira 댓글 초안 — 미등록

TMI-203 recovery-r1 exact/schema/fixture/index선언과내부strictcodec·상태규칙·page점조회/범위변경계획을추가했다. 100개정규화제한과feed정확조회분리,동일키우선선택을테스트한다. 기존199/LC/push/token계약보존. 공동수락·실Mongo/IAM/복원·HTTP/DBadapter는아직남아있으며이번작업으로이슈완료나활성화를요청하지않는다.
