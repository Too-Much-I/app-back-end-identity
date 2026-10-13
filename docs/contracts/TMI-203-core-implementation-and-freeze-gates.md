# TMI-203 내부 검증 코어 구현과 공동 확정 필요 항목

- 날짜: 2026-10-12
- 브랜치: `feat/TMI-203-withdrawal-snapshot-feed-recovery`
- Jira: TMI-203 / 연계 TMI-204
- 상태: **내부 계산·검증 코어만 구현. TMI-203 전체/API 구현 완료가 아니다. 아래 저장 규격은 제안이며 운영 DDL·공동 수락본이 아니다.**

## 1. 5줄 결론

1. 승인된 floor/별도 range 방향의 손실 없는 bounded 정규화를 구현했다.
2. feed의 고정 목표·20건 범위·누락/만료 전체 실패·MAX 경계 검증을 구현했다.
3. snapshot canonical digest를 streaming 구현하고 기존 TMI-199 golden fixture와 대조한다.
4. HTTP controller·Mongo recovery adapter·cursor·snapshot builder·cleanup adapter는 아직 없다. 새 코어는 Bean으로 등록하지 않았다.
5. 양 서버 문서의 공동 exact 선행 조건이 남아 있어 외부 wire를 임의 확정하지 않았다. 기존 서비스 동작·flags·DB·배포는 유지한다.

## 2. 사용자가 반드시 읽어야 하는 내용

이번 요청으로 구현은 시작했지만, 구현 전제까지 해소된 것은 아니다. 공식 Jira TMI-203에는 새 schema/error/index/fixture를 먼저 양 서버가 맞추도록 되어 있다. Billing 최신 TMI-204 계획 §4도 새 exact 동결 전 production adapter 구현을 금지한다. 사용자 구현 요청을 상대의 기술 수락으로 간주하지 않는다.

따라서 현재 변경을 배포해도 snapshot/feed API가 생기지 않는다. `FeedPageVerifier.Observation`은 **같은 Mongo snapshot에서 읽었다는 전제의 내부 입력**이다. 이 타입을 만든 것만으로 실제 일관 읽기가 보장되지 않는다. 코어 테스트 통과는 동시 cleanup·Mongo rollback·같은 T 읽기·IAM·Billing 복원 검증을 대신하지 않는다.

기존 cleanup의 `Evidence` 설명만 source 책임으로 고쳤다. 실제 원자 기록 adapter나 삭제 허용 기본 구현을 추가하지 않았다. 현재 fail-closed 경계는 유지된다.

## 3. 사용자가 결정해야 하는 사항

새 제품 정책 선택은 필요하지 않다. 다음 기술안을 Billing에 전달하고 수락/보완을 받아 동일 revision으로 기록해야 한다. 이 문서 작성은 실제 전달이나 수락이 아니다.

1. 최신 v1 후보의 body/멱등성·availability·만료 의미를 최종 확정하고 새 fixture를 양 서버에서 실행한다.
2. 아래 source 저장 규격과 `FAILED` page·429 처리안을 수락/보완한다.
3. 그 뒤 controller/저장 어댑터 구현을 이어간다. 실환경 Mongo·IAM 검증과 실제 활성화 승인은 별도다.

## 4. 주요 위험과 미확인 사항

- 고정 `atClusterTime`의 실제 replica-set 검증은 미실행. 로컬 `mongod` 명령은 발견되지 않았으며 Docker 환경 준비/실험은 이번 범위에서 하지 않았다. live scan으로 대체하지 않는다.
- source 정규화는 선택된 최대100구간의 정확한 집합만 변환한다. DB 전체 구간을 읽거나 삭제하는 구현이 아니다. 선택 밖 문서는 유지해야 한다.
- feed validator는 조회 결과의 완전성을 증명하지 못한다. Mongo adapter가 active stream/head/floor/range/journal을 같은 짧은 snapshot에서 읽고 clock 증거를 확인해야 한다.
- source range/version 초기 이관, CAS·cleanup 동시성, 소실 증거 보존과 retired stream 정리는 아직 구현하지 않았다.
- 기존 snapshot User 자료의 정밀도와 신규 journal BSON 저장 정밀도는 실제 builder에서 다시 검증해야 한다. digest 코어 자체는 nanos를 절삭하지 않는다.
- IAM/Lattice 인증 주체, 직접 ingress 차단, cursor 전용 HMAC 주입은 미구현이다. JWT/임의 헤더/항상 true evidence로 우회하지 않는다.

## 5. 현재 구현과 남은 기술 제안

### 5.1 구현한 내부 동작

| 파일 | 동작 | 하지 않는 일 |
| --- | --- | --- |
| `UnavailableRanges` | 최대100구간 정렬·중복/인접/겹침 병합, prefix 편입, MAX overflow 방지, head 밖 증거 거절 | DB 삭제/CAS·무한 목록 입력·가용 구간을 포함하는 임의 합치기 |
| `FeedPageVerifier` | `(after, after+min(20,H2-after)]` 전체 검증, 최초 hole/만료 실패, same-stream head rewind 거절 | cursor 서명/HTTP 상태 직렬화·첫 요청 H2 고정·실제 일관 읽기 |
| `SnapshotDigest` | ordered userId→withdrawnAt UTF8 compact JSON, unsigned32BE 길이 prefix, incremental SHA256, strict order·중복 거절 | snapshot 취득/영속화/READY 공개·기존 기록 변경 |
| `WithdrawalSourceCleanup.Evidence` Javadoc | remote consumer ACK가 아닌 source stream/version/floor/range 책임 명시 | 실제 cleanup 동작 변경 |

feed 입력의 row 수·구간 수·stream·sequence·head/floor·미래 capturedAt·중복 순번 모순은 source 불명으로 거절한다. 한 페이지의 prefix가 정상이어도 뒤에 hole이 있으면 결과 객체를 반환하지 않는다. after==H2만 빈 완료가 가능하며 MAX에서도 +1 overflow가 없다. 가용성은 consumer 적용 완료 증거가 아니다.

### 5.2 source 저장 규격 제안 — 아직 adapter/DDL 없음

새 collection `withdrawal_unavailable_ranges`:

| 필드 | 제안 |
| --- | --- |
| `_id` | 내부 식별자; 외부 DTO에 노출하지 않음 |
| `streamId` | lowercase canonical source UUID |
| `fromSequence` / `toSequence` | BSON int64, 1≤from≤to≤같은 source head |

userId/event payload·consumer checkpoint·개별 token은 저장하지 않는다. 가용성 사실에 별도 임의 TTL을 두지 않는다. current stream에서 prefix 편입되면 제거하고, retired stream은 종료+120일의 승인된 정리 경계에 따른다.

제안 index는 `ux_withdrawal_unavailable_from(streamId,fromSequence)` unique와 `ix_withdrawal_unavailable_to(streamId,toSequence)` non-unique다. 둘 다 ascending/simple collation/sparse=false/partial 없음/TTL 없음. 기존 I2 7개+snapshot 후보3개+range2개는 **12개 기능 index 후보**이며 expiry/scheduler 조회용 보조 index 검토 뒤 총수를 확정해야 한다. 기존13개199 manifest를 이 숫자로 소급 덮어쓰지 않는다.

stream에는 `availabilityVersion` BSON int64≥0을 추가하는 안이다. 기존 stream은 명시 이관으로0을 설치하고, missing/null/음수/overflow는 활성화 차단한다. 런타임 암묵 upsert·없는 version을 신뢰값으로 변환하지 않는다.

각 cleanup transaction은 source version/status/stream CAS, 유효 lease·expiry/timing 검증, journal/delivery 삭제, floor/range 갱신을 함께 commit한다. capture/restore도 동일 stream 문서를 쓰므로 실제 Mongo write-conflict 검증이 필요하다. source version overflow 시 삭제도 rollback한다.

한 정규화 호출은 최대100구간이다. 신규 singleton을 포함하면 기존 DB에서 선택할 구간은 최대99개다. 연결된 구간이 더 있으면 선택된 증거만 정규화하고 나머지는 다음 batch에 남긴다. 선택 밖 구간을 `deleteMany(streamId)`로 지우지 않는다. 임시 겹침도 exact 증거로 유지하고 이어지는 bounded 정규화에서 제거한다. `_id`/원값 CAS·unique 충돌 시 전체 transaction 재시도하며 일부 삭제만 성공시키지 않는다.

전체 stream의 range 수에 임의 hard cap을 걸어 증거를 버리지 않는다. 원천의 실제 만료가 희소하면 구간 수도 커질 수 있으므로 문서당 고정 필드·페이지당 제한·batch 제한으로 메모리/응답을 제한하고 cardinality/정리 지연 지표를 둔다. 운영 경보 임계값은 부하 측정 후 설정해야 하며 무기한 보존 연장을 허용하지 않는다.

### 5.3 남은 wire 항목 권장안 — 상대 수락 필요

- prefix는 `/internal/v1/billing/withdrawals`, 기존4기능/필드와 latest 후보 의미를 유지한다. remote3기능과 generation header를 다시 추가하지 않는다.
- FAILED run의 `/snapshots/page`는 `409 SNAPSHOT_FAILED`, `retryable=false`, source details 생략을 제안한다. 원인은 status의 기존 failureCode로 확인한다. BUILDING은 기존409 SNAPSHOT_NOT_READY/true, EXPIRED는410 SNAPSHOT_EXPIRED/false를 유지한다.
- 429는 gateway/인프라 응답일 수 있으므로 앱 JSON을 필수로 가정하지 않고 Billing이 HTTP status와 유효 Retry-After로 처리하는 안을 권장한다. 앱 제한기를 추가할 경우 별도 scope/분산 집계와 정확 error body를 먼저 정한다. 이번에 임의 제한 횟수를 제품 동작으로 넣지 않는다.
- code별 고정 message·required/null/생략 schema·operation index 최종 이름은 새 revision fixture에서 일괄 확정한다. 기존 Java 내부 `Failure` enum은 wire code 계약을 확정하는 용도가 아니다.

## 6. 부록 — 근거·검증·보고

### 6.1 변경 파일

- [UnavailableRanges.java](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalrecovery/UnavailableRanges.java)
- [FeedPageVerifier.java](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalrecovery/FeedPageVerifier.java)
- [SnapshotDigest.java](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalrecovery/SnapshotDigest.java)
- [WithdrawalSourceCleanup.java](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalcapture/WithdrawalSourceCleanup.java) — 주석만
- 같은 이름의 `src/test/java/.../withdrawalrecovery/*Tests.java` 3개
- 본 문서, WORKLOG/CURRENT_STATE

기존 dirty 문서7개와 이전 기록은 보존했다. API/DB index/환경설정/외부 계약/Billing 소스에 예상 밖 변경은 없다. commit/push/Jira 수정·댓글·상태 전환·배포는 하지 않았다.

### 6.2 테스트 범위

- 범위 정규화: prefix·가용 구간 보존·중복/인접/겹침·MAX·부분 batch·입력 상한. 고정 seed 2,000개 집합에 대해 각 순번의 소실 여부가 변하지 않고 정규화가 멱등임을 검사한다.
- feed: head가 늘어도 H2 유지·20개 경계·hole 전체 실패·최초 실패 우선·prefix/range/logical expiry·빈 완료/MAX·모순/잘못된 stream.
- digest: 변경하지 않은 TMI-199 golden fixture(nanos 포함/빈값), 순서/중복/UUID·seal 뒤 append 금지.
- 최종 `env -u SENTRY_AUTH_TOKEN ./gradlew clean test` 성공: 총1,492개,1,486통과/6skip/실패0. 신규17개 모두 통과. 최초 테스트 컴파일 오류 수정과 재실행 이력은 WORKLOG에 기록했다. 실제 Atlas/OAuth/Mongo replica-set/IAM/Billing 연동 검증은 수행하지 않았다.

### 6.3 다음 구현과 배포 전 확인

공동 exact/새 fixtures → 같은 T Mongo 실현성 → source range/CAS/cleanup adapter → snapshot run/worker/보존 → signed cursor/feed adapter → fail-closed IAM controller/strict decoder → 운영 동등 경합·복원/Billing 연동 순이다. 기존 capture/push204·LC·로그인 token 계약과 default OFF를 유지한다.

배포 전에는 코드 일부 구현과 기능 완료를 구분하고, index preflight/명시 준비·운영 evidence·권한/ingress·7일/120일 정리·독립 경보·실제 복원 검증을 모두 확인해야 한다. 현재 변경만으로 활성화하지 않는다.

### 6.4 Jira 댓글 초안 — 미등록

TMI-203의 범위 정규화·feed 페이지 검증·snapshot streaming digest 내부 코어 및 단위 테스트를 추가했다. cleanup Evidence 주석은 source 단독 책임으로 갱신했다. HTTP/DB recovery adapter는 공동 exact 선행 조건으로 아직 구현하지 않았다. 변경 파일은 withdrawalrecovery 3클래스/3테스트, cleanup 주석, 구현 범위 문서와 작업 기록이다. 실제 Mongo/IAM/복원·공동 연동은 미검증이며 전체 테스트 결과는 이번 작업 기록을 참조한다. 이슈 완료/상태 변경 요청이 아니다.
