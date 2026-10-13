# Identity 회신 — 단일 진행 관리 exact 개정 미결안

- 작성일: 2026-10-12 / develop / TMI-203·204, 영향199·202·205·206.
- 상태: **첨부 문서 개정 지침에 대한 기술 제안. exact 동결본·구현·Jira 변경·배포 승인이 아니다.** 아래 새 필드/값은 모두 Billing 수락 대상이다.
- 연계: [전달 인계서](TMI-203-to-Billing-TMI-204-simplified-recovery-handoff.md), [현행 계획](TMI-203-withdrawal-recovery-snapshot-feed-plan.md), [기존 exact](billing-c0-exact-wire-index-proposal-2026-10-07.md).

## 1. 5줄 결론

1. Identity source 제공/Billing local checkpoint·epoch·consumer coverage 단독 소유를 수락한 계획을 유지한다.
2. revision은 별도 versioned 내부 경로를 권장하되 정확 경로·번호는 양측 수락 후 확정한다. 구경로 성공 위장과 무차별 ACK/generation 삭제는 금지한다.
3. snapshot 생성에는 명시 streamId body를 제안하고, 멱등 key는 consumer/환경 범위에서 요청 stream에 바인딩하여 stream 변경 시 조용한 재사용을 막는다.
4. availability는 성공 페이지에서 실제 검사한 범위,410에서는 최초 재생 불가 위치/구간을 제공하는 bounded DTO를 제안한다. 전체 interval 무한 배열은 제공하지 않는다.
5. C는 원 event0~C 처리 증거가 아니다. state baseline H와 그 뒤 연속 event 적용 범위를 Billing/205에서 분리해야 한다.

## 2. 반드시 읽어야 하는 내용

### 2.1 확인된 상태와 배포 미확인 구분

- 로컬 src/main에서 recovery route/package 검색 결과203 snapshot/feed/recoveries/checkpoints 구현은 찾지 못했다. 이것이 다른 브랜치·실배포에 없다는 증명은 아니다.
-202 source record/cleanup/config는 로컬에 존재한다. Evidence port의 consumer generation/coverage 책임은 새 계약에 맞게 바꿔야 한다.
- Billing204 문서의 본문이 local authority 계획으로 바뀐 것을 읽어 확인했다. Billing 코드·실제 서비스 배포는 확인하지 않았다.
- AWS task definition·image digest·commit·실경로/IAM 증거를 이번에 조회하지 않았다. **배포 호환성은 미확인 gate**이며 미배포라고 단정해 v1을 덮어쓰지 않는다.

### 2.2 유지하는 기존 의미

push4필드/eventId·digest/commit 후204,202 capture·resumeGeneration/leaseVersion,source stream/run fence,ACTIVE MEMBER/token/drain,Lattice IAM/SigV4,T/H·canonical digest·H2/1시간 cursor,16KiB/page20,7일/120일/15일·금융자료 보존은 유지한다.

제거는 복구 전용 remote API3개·generation header·Identity consumer 완료 상태뿐이다. local epoch를 remote generation의 새 wire 의미로 만들지 않는다.

## 3. Billing과 동결할 제안

1. versioned read4기능 경로의 정확 prefix/revision. 처음에는 worker OFF,각 서버 build/route 증거 확인 후 명시 전환한다. 실배포가 없는 것이 확인되면 통제된 전환도 가능하지만 확인 전에는 versioned 안을 기본 권장한다.
2. snapshot 생성 body/멱등 scope와 old stream run 재조회 규칙.
3. 아래 availability/오류 detail DTO,검사 범위와 source 일관 읽기,크기 초과 처리.
4. snapshot operation unique index 개정 및 정확 이름/key order. 명세 수락 전 manifest fixture는 변경하지 않는다.
5. Billing5개 collection은 상대 후보이며 wire로 전파하지 않는다. local restore 권한·일관 복원 범위·watchdog/205·retry 초기값은 Billing 회신과 공동 운영 계약으로 동결한다.

## 4. 주요 위험

- head 순번만 보고 가용성을 선언하면 만료 hole을 숨길 수 있다. 관측 head와 실제 검사 범위를 분리한다.
- 같은 C라도 snapshot baseline만 반영했는지 원 event를 연속 적용했는지가 다르다.205가 C 숫자만으로 purge 허가하면 안 된다.
- key의 unique에 stream을 넣으면 같은 key로 새 stream의 다른 run이 생성될 수 있다. 명시 요청 바인딩과 충돌 검증을 선택한다.
- 사라진 row가 expiry 기록에도 없으면410으로 추측하지 않는다. 조사 중503 또는 확인된GAP409로 구분한다.
- 로컬 코드 부재만으로 운영 endpoint/index를 삭제하지 않는다. API 검색 실패와 실제 배포 검증을 구분한다.

## 5. exact 미결안과 본문 수정 제안

### 5.1 snapshot stream·멱등 scope

새 snapshot 생성 요청은 body 없는 구형과 달리 다음을 제안한다. 추가 키 금지,streamId lowercase UUIDv4,Idempotency-Key lowercase UUIDv4.

```json
{"streamId":"00000000-0000-4000-8000-000000000001"}
```

- identity는 인증된 environment/consumer와 현재 active stream을 검증한다. caller epoch는 받지 않는다.
- 멱등 lookup unique는 environment/consumer/requestOperationId. canonical body의 streamId와 scope=ALL_WITHDRAWN을 저장한다. 같은 key/같은 body는 같은 run,다른 stream/body는409 OPERATION_CONFLICT(신규 후보).
- 처리 순서: 기존 operation binding 확인 → mismatch면409 → 요청 stream이 현재가 아니면409 STREAM_REPLACED → 같은 current stream의 기존 run 재응답 또는 신규 생성.
- stream 교체 뒤 old key/old body는 STREAM_REPLACED이며 새 run으로 변환하지 않는다. 새 stream 요청은 새 key. old run의 status/page도 stream 검사로 거절하며 사용자 자료는 원래7일 expiry로 정리한다.
- commit 응답 유실은 같은 key/body 재호출로 수렴한다. operation metadata120일 뒤 key 재사용을 무기한 기억하지 않으므로 새 작업에는 항상 새 key를 사용한다.

이 안은 이전 계획의 stream 포함 operation unique key 후보를 **보완하는 신규 제안**이다. 관련 계획/index 본문은 수락 후 함께 맞춘다.

### 5.2 성공 feed availability 후보

feed 기존 의미는 items,scanCursor,targetThroughSequence,nextAfterSequence,scannedThrough,done을 유지한다. scanCursor는 마지막 page에도 non-null을 유지하는 안을 권장한다. 용도는 이번 scan의 고정 범위·일관 binding 확인과 TTL 내 중복 조회이며 **ACK용 증거가 아니다.** H2 이후는 새 cursor로 시작한다.

성공 응답에는 availability를 필수로 추가하는 안이다. 아래 모든 키 필수/non-null,숫자는 canonical D0 문자열,observedAt은 UTC Instant. 값은 가짜 예시다.

```json
{
  "streamId":"00000000-0000-4000-8000-000000000001",
  "replayFloor":"80",
  "observedThroughSequence":"150",
  "checkedFromExclusive":"100",
  "checkedThroughSequence":"120",
  "observedAt":"2026-10-12T00:00:00Z"
}
```

- checkedFromExclusive=request.afterSequence,checkedThroughSequence=response.scannedThrough. 성공은 이번 페이지 범위가 연속으로 재생 가능함을 뜻하며 head/H2 전체 가용성을 선언하지 않는다.
- observedThroughSequence는 같은 일관 읽기의 committed head이며 H2 이상이어야 한다. source rewind/다른 stream이면 성공을 반환하지 않는다. replayFloor는 연속 재생 불가 prefix의 끝이며 consumer checkpoint가 아니다.
- 빈 정상 page는 after==H2이고 검사 범위 길이0. after<H2의 빈 page는 성공 금지. MAX_VALUE에서 +1 overflow를 피한다.
- 해당 범위 다음 hole은 다음 요청에서 반환한다. 전체 unavailable interval 목록을 한 응답에 실으려 하지 않는다. 장기 lag 경보는 source timestamps와 consumer C/watchdog를 함께 사용하며 이 DTO가 미래 가용성을 보증하지 않는다.

### 5.3 410/409/503 detail과 크기 상한

기존 error 필수 code/message/retryable/correlationId는 유지하고 **새 revision에서만** details를 추가한다. source 재생 오류에는 details 필수,그 외에는 생략한다. details는 아래 shape 후보이며 consumer/사용자 목록을 담지 않는다.

```json
{
  "streamId":"00000000-0000-4000-8000-000000000001",
  "replayFloor":"80",
  "observedThroughSequence":"150",
  "missingSequence":"101",
  "unavailableRange":{"fromSequence":"101","toSequence":"105"},
  "observedAt":"2026-10-12T00:00:00Z"
}
```

-410 REPLAY_UNAVAILABLE: 요청에 필요한 최초 소실 sequence와 그에 해당하는 확정 range1개. prefix/interval 보존 증거가 있어야 한다. **전체 gap 목록이 아니며 나머지 범위가 정상이라는 뜻도 아니다.**
-409 COVERAGE_GAP: 소실이 확인됐지만 expiry range 증거가 없는 경우 unavailableRange는 키 필수/null,missingSequence는D1.503 SOURCE_AVAILABILITY_UNCERTAIN(신규 후보)은 일시 읽기/검증 조사 실패를 나타내며 detail 신뢰 가능 시만 같은 shape를 제공하도록 별도 variant를 동결한다.
- 일반 저장소503은 details 생략. known missing이 없는 실패에 임의 missingSequence를 만들지 않는다.401/403 인프라 응답에는 앱 JSON을 가정하지 않는다.
- 각 범위는 D1 from<=to<=관측head,전체request/response16KiB. 배열이 아닌1구간이라 wire interval 개수 상한은1. success metadata 크기 예산을 먼저 확보하고 items를≤20에서 줄이되1건도 들어가지 않으면 명시 오류,조용한 절삭/빈 완료 금지.
- 서버가 유지하는 전체 interval 데이터에는 별도 bounded 저장·정리 설계가 필요하다. 위 응답의1구간 제한이 원천 hole 기록을 삭제/병합해 정상 구간을 잃어도 된다는 뜻은 아니다.

### 5.4 source 읽기/cleanup 경계

각 feed 요청은 같은 짧은 snapshot/transaction 읽기에서 active stream,counter,floor/interval과 journal을 일관 관측하는 안을 제안한다. 최초 H2도 이 관측에서 고정한다. 요청 전체의 과거5분 T가 아니라 **페이지 단위** 일관성이다.

cleanup은 availability evidence+삭제를 같은 transaction으로 commit한다. 삭제 전 관측의 응답은 그 시점에 존재한 자료이며 미래 재조회 가능성까지 보증하지 않는다. 다음 요청에서 사라졌으면410/GAP으로 드러나야 한다. 논리 expiry를 넘긴 row를 물리 cleanup이 늦다는 이유로 계속 replay하지 않는 조건도 검사한다.

stream restore 중 옛 instance 쓰기/endpoint는 운영 격리한다. 단순 필드 검사만으로 split-brain을 해결했다고 하지 않는다.

### 5.5 manifest·consumer 증거

snapshot manifest는 remote generation만 제외하고 snapshotId,streamId,T,H,itemCount,pageCount,contentDigest,legacyCoverage,knownGaps,expiresAt을 유지하는 안이다. knownGaps는 source가 알고 있는 역사/원천 사실이며 Billing 적용 coverage가 아니다. snapshot digest는 원래 canonical 알고리즘 그대로다.

Billing은 최소한 state baseline H와 **그 뒤 연속 원 event 적용 범위 (H,C]**를 구분한다. snapshot만 완료했으면 C=H일 수 있지만 [0,H] 원event 수신을 증명하지 않는다. 재bootstrap/원천 만료로 이전 gap이 있으면 unresolved evidence를 유지하며 새로운 H로 덮어 지우지 않는다.205는 baseline·실제 적용 범위·unresolved gap·provider 거래 근거를 함께 사용한다.

### 5.6 문서/index/fixture 변경표

| 대상 | 수락 후 수정 | 이번 상태 |
| --- | --- | --- |
| Identity203 계획 §3/5.2/5.4/6.1 | revision·생성body·availability·operation index 확정 | 현행 후보 유지,본 제안 연결 |
|202 계획/결과·Evidence 설명 | consumer remote CAS 대신 source expiry/floor/interval 원자성 | 코드·이전 구현 결과를 소급 수정하지 않음;영향 안내 연결 |
| 새 exact 문서/fixture revision |4API request/response/error 및모든 null/생략,index manifest | 이번은 후보 문서,fixture 미변경 |
| 기존19913개 manifest | I2의7개 유지,remote3개 제외,snapshot3개 중operation 변경 | 예상10개이며 실제 manifest 옵션 검증 후 확정 |
| snapshot operation index | unique(environment,consumer,requestOperationId),stream body binding 별도 | 정확 새 index 이름·migration 수락 필요 |
| Billing204·205·206/ADR/공동 본문 | local authority·복원·watchdog·새 exact 연결 | Billing 담당 변경 범위,이번 수정 없음 |

기존 fixture hash/수락 이력은 보존한다. 새 manifest에서 key order·unique·partial·TTL 없음·simple collation·sparse=false 및 기본 _id_를 검증한다. 운영 index drop/recreate 금지. unrelated ACK/resumeGeneration을 검색치환하지 않는다.

## 6. 검증·후속 완료 기준

- required/null/omit·schema type·D0/D1/U32 boundary·16KiB·old/new revision 혼용 거절.
- 같은 key/body 응답 유실,다른 stream/body409,stream 교체 뒤 old run 거절·expiry 정리.
- success 검사 범위와 실제 items 일치,head와H2·floor 관계,마지막 cursor non-null·TTL 내 재조회.
- 중간 hole/prefix expiry/확정GAP/조사503,논리 expiry·cleanup 경합에서 빈 성공 금지.
- baseline H와 원event (H,C] 분리,새 snapshot으로 unresolved gap 소거 금지,205 evidence 회귀.
- local commit crash/epoch·restore·IAM·digest·token/push 기존 회귀.

이번 검증은 문서/코드 검색·Billing 계획 읽기·링크/diff 정적 검사뿐이며 Gradle/Mongo/실배포 검증 미실행이다. 안내문 추가만으로 공동 exact 개정 완료라고 하지 않는다.

다음: Billing이 본 후보를 보완·수락 → 새 exact 문서/fixture revision 공동 작성·검증 → 활성 본문/계획/205·206 gate 링크 전환 → 별도 구현 요청. source availability의 새 필드/오류와 index 이름·revision이 미동결인 동안 임의 구현하지 않는다.

Jira 댓글 초안(미등록): Billing 문서 개정 지침에 대해 snapshot stream body/멱등 binding·bounded availability/410·revision/manifest 후보 회신. 변경은 문서/기록만,정적 검사. local203 route 검색상 미발견과 실배포 미확인 구분,공동 exact/코드/Jira/배포 미변경.
