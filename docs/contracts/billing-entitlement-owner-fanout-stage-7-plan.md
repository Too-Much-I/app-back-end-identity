# 7단계 구현 계획: Billing SigV4 eligibility와 owner event durable fan-out

- 상태: Billing·Learning Core phone continuation 구현 확인, Identity 구현 전 계획
- 작성일: 2026-09-03
- 대상 저장소: Identity 중심, Billing·Learning Core consumer와 AWS 배포 연동
- 선행 단계: Stage 1~6 구현 완료
- 관련 문서: [Phone eligibility ADR](../adr/ADR-002-phone-eligibility-binding-server-contract.md), [Stage 3 release 계획](firebase-withdrawal-identity-release-stage-3-plan.md), [후속 구현 순서](firebase-auth-follow-up-implementation-order.md)

## 1. 5줄 결론

1. Stage 7은 Identity→Billing phone eligibility 전송을 Bearer workload JWT에서 VPC Lattice `AWS_IAM`·SigV4로 바꾸고, owner 변경 이벤트를 consumer별 durable delivery로 확장하는 단계다.
2. `UserMerged` v1은 Billing과 Learning Core 모두에 전달하지만, 동일 phone proof 기반 `TrialOwnerRebindApproved` v1은 Billing에만 전달하며 Learning Core로 phone event를 fan-out하지 않는다.
3. Billing owner rebind는 기존 Claim·Grant·allocation·consumption을 복원하지 않고 current owner link만 옮기되, `OPEN`·`RETAKE_AVAILABLE` group은 read-only continuation으로 발견해 새 target Session에서 처음부터 이어 쓸 수 있게 한다.
4. phone 재가입 source는 최신 RELEASED alias를 추측하지 않고 Identity가 release 시 만든 exact local lineage가 한 건일 때만 사용하며, 모호하면 가입은 허용하되 자동 owner 이전은 하지 않는다.
5. Billing 구현은 `develop`에 병합됐고 Learning Core phone continuation은 구현·457개 테스트를 통과했지만 아직 feature 브랜치이므로, 병합 후 Identity 구현과 `UserMerged` consumer·staging 운영 gate를 순서대로 닫아야 한다.

## 2. 사용자가 반드시 읽어야 하는 내용

### 2.1 Stage 7은 다섯 단계이며 7-D는 두 트랙이다

```text
7-A Identity→Billing SigV4 transport 기반
7-B Billing owner-rebind·phone continuation TMI-120 구현 확인·비활성 선배포
7-C Identity owner event core·consumer별 durable delivery
7-D1 Learning Core phone continuation TMI-122 병합·비활성 선배포
7-D2 Learning Core UserMerged owner migration/source deny consumer
7-E staging E2E·consumer별 canary 활성화
```

7-A와 7-C는 Identity 코드 작업이다. 7-B는 Billing에서 구현·병합됐고, 7-D1은 Learning Core에서 구현과 로컬 검증을 마쳤으나 `develop` 병합이 남았다. 7-D2는 별도 Learning Core 작업이며, 7-E는 세 서비스와 AWS 운영 환경의 공동 완료 조건이다. 한 저장소의 테스트 성공만으로 Stage 7 전체를 완료 처리하지 않는다.

### 2.2 최종 event routing 정책

| lifecycle | Billing | Learning Core | 의미 |
| --- | --- | --- | --- |
| Guest→Member canonical merge | `UserMerged` v1 | `UserMerged` v1 | 두 내부 계정이 하나의 canonical User로 합쳐짐 |
| 동일 phone 재가입 | `TrialOwnerRebindApproved` v1 | event는 전달하지 않음 | phone-scoped 무료시험 owner link 이전만 승인 |
| 과거 시험·결과 복구 | 전달하지 않음 | 미래의 별도 continuity event | old-account-derived strong proof가 있을 때만 후속 구현 |

같은 전화번호를 다시 인증했다는 사실은 과거 시험·답안·피드백을 볼 수 있는 사람이라는 증거로 사용하지 않는다. 따라서 Billing 권리의 owner만 제한적으로 이전하고 Learning Core 개인 데이터는 이전하지 않는다. 다만 owner rebind 뒤 Billing이 제공하는 read-only phone continuation은 Learning Core가 새 target Session을 만들 때 사용할 수 있다. 이는 source Session이나 결과의 owner를 바꾸는 event 전달이 아니다.

### 2.3 owner rebind는 무료권 재지급이 아니다

Billing에서 변경할 대상은 stable subject의 current `userId` mapping뿐이다. 다음 데이터는 그대로 유지한다.

- 기존 `TrialClaim`과 claim 시각
- `subjectRefId`
- Grant의 total·available·held·consumed unit
- entitlement ledger와 consumption 기록
- 기존 Reservation·AttemptGroup·Session 식별자

상태별 사용자 동작은 다음과 같다.

| 기존 AttemptGroup 상태 | owner event 처리 | 새 사용자의 시험 생성 |
| --- | --- | --- |
| 없음 | owner CAS `APPLIED` | 기존 미사용권으로 일반 `INITIAL` 시작 |
| `OPEN` | owner CAS `APPLIED` | Billing continuation을 발견해 같은 group·mockExamId의 새 `REPLACEMENT` Session 생성 |
| `RETAKE_AVAILABLE` | owner CAS `APPLIED` | Billing continuation을 발견해 같은 group·mockExamId의 새 `REPLACEMENT` Session 생성 |
| `GRADING` | owner를 바꾸지 않고 retryable pending | terminal 판정 뒤 같은 event 재시도 |
| `COMPLETED` | owner를 바꾸지 않고 성공 `NOOP` | 새 무료권도 과거 결과 접근도 제공하지 않음 |

`OPEN`·`RETAKE_AVAILABLE`에서도 source의 기존 Session·답안·결과를 target으로 이전하지 않는다. Learning Core는 Billing이 반환한 exact `continuationId`·`attemptGroupId`·`mockExamId`를 operation에 저장하고 reserve에 그대로 되돌려 보낸 뒤, 새 target `examId`로 처음부터 응시하게 한다.

### 2.4 현재 구현과의 차이

2026-09-03 확인된 현재 구현은 다음과 같다.

- `PhoneEligibilityBindingOutbox`는 VERIFIED·REVOKED, binding revision, lease·retry·dead-letter·P30D/P90D 운영 필드를 이미 가진다.
- `JdkPhoneEligibilityBindingDeliveryAdapter`는 아직 audience 기반 Bearer workload JWT를 보내고 응답 status만 반환한다.
- `UserMerged` v1은 Guest merge Mongo Transaction에서 이미 생성된다.
- `UserMergedOutbox`는 immutable payload와 단일 delivery 상태가 한 문서에 결합돼 있다.
- `UserMergedPublisher`는 endpoint 한 곳만 호출한다.
- `TrialOwnerRebindApproved`와 phone 재가입 predecessor lineage는 아직 없다.
- RELEASED `PhoneFingerprintAlias`는 userId·phoneIdentityId·fingerprint·releasedAt을 보존하지만 exact predecessor를 가리키는 lifecycle 계약이 없다.
- Billing `develop`에는 `TMI-120` phone owner rebind와 continuation discovery·reserve echo가 병합됐다.
- Billing은 없음/`OPEN`/`RETAKE_AVAILABLE`/`GRADING`/`COMPLETED` 상태표와 exact `ownerTransitionReason`·`ownerTransitionId`를 적용한다.
- Learning Core `feat/TMI-122-phone-rejoin-continuation`은 target에 기존 Session이 없을 때만 continuation을 조회하고 새 target Session을 만드는 흐름을 구현했으며 457개 테스트가 통과했다.
- Learning Core `TMI-122`는 아직 `develop`에 병합되지 않았고, 별도 `UserMerged` consumer는 구현 범위가 남아 있다.

따라서 phone rejoin vertical slice에서 남은 애플리케이션 구현은 Learning Core `TMI-122` 병합과 Identity 7-A·7-C다. Stage 7 전체는 여기에 Learning Core `UserMerged` consumer와 운영 gate까지 포함하므로, 기존 adapter의 Authorization header만 바꾸거나 `UserMerged` endpoint를 Billing으로 교체하는 것으로는 완료되지 않는다.

### 2.5 producer보다 consumer가 먼저다

다음 순서를 바꾸지 않는다.

1. 병합된 Billing eligibility·owner-rebind·phone continuation consumer를 feature flag OFF로 배포한다.
2. Learning Core `TMI-122`를 `develop`에 병합하고 phone continuation과 별도 `UserMerged` consumer를 feature flag OFF로 배포한다.
3. Identity event capture와 publisher를 모두 OFF로 배포한다.
4. Mongo index·Transaction capability와 AWS route/IAM을 검증한다.
5. staging에서 consumer를 먼저 켠다.
6. Identity capture를 켜고 delivery가 쌓이는지 확인한다.
7. publisher를 consumer·event type별로 하나씩 canary 활성화한다.

가입·Guest merge 요청은 downstream HTTP 응답을 동기적으로 기다리지 않는다. 사용자 요청 Transaction은 local event와 delivery를 저장한 뒤 끝나며 publisher가 비동기로 전달한다.

## 3. 사용자가 결정한 사항

### D1. event별 consumer — 확정

- `UserMerged`: `BILLING`, `LEARNING_CORE` delivery 각각 한 건
- `TrialOwnerRebindApproved`: `BILLING` delivery 한 건
- phone proof 기반 Learning Core delivery: 생성 금지

기존 `TrialOwnerRebindApproved` v1의 의미를 나중에 조용히 확장하지 않는다. 과거 Learning Core history migration이 필요하면 새 event 이름과 schema, 별도 proof lifecycle을 사용한다. Billing read-only phone continuation은 이 migration에 해당하지 않는다.

### D2. phone 재가입 source lineage — 확정

Identity는 RELEASED alias 중 가장 최근 값을 source로 추측하지 않는다. Stage 3 identity release Transaction에서 다음 사실을 보존한 명시적 `PhoneRejoinLineage`를 만든다.

- exact source user와 source PhoneIdentity
- source withdrawal lifecycle
- consumer scope
- REVOKED source binding revision
- release 시각

신규 signup·Guest upgrade 시 새 phone fingerprint set으로 RELEASED alias를 조회하되, 동일 `consumerScopeId`에서 연결되는 `AVAILABLE` lineage가 정확히 한 건일 때만 자동 owner-rebind event를 만든다. 과거 `CONSUMED`·`RECONCILIATION_REQUIRED` lineage는 자동 이전 후보에서 제외한다.

| 동일 consumer scope의 조회 결과 | 가입 처리 | owner rebind |
| --- | --- | --- |
| 0건 | 정상 진행 | 생성하지 않음 |
| 정확히 1건, 모든 gate 통과 | 정상 진행 | Billing event 생성 |
| AVAILABLE 2건 이상 또는 owner/revision 모순 | 정상 진행 | 생성하지 않고 reconciliation 기록 |

lineage 이상 때문에 사용자의 신규 가입 자체를 막지 않는다. fail-closed 대상은 과거 권리의 자동 이전이다. 서로 다른 `consumerScopeId`에 각각 AVAILABLE lineage가 있는 것은 복수 혜택 프로그램을 위한 정상 상태이며 scope별로 독립 처리한다.

반복 탈퇴·재가입은 다음 단일 chain으로 수렴해야 한다.

```text
A 탈퇴: lineage A AVAILABLE
B 가입: lineage A CONSUMED, A→B event 생성
B 탈퇴: lineage B AVAILABLE
C 가입: lineage B CONSUMED, B→C event 생성
```

C 가입 시 같은 phone fingerprint에 연결된 A·B의 RELEASED alias가 모두 검색될 수 있지만 lineage A는 CONSUMED이므로 후보가 아니다. 과거 User tombstone과 CONSUMED lineage가 여러 건 존재하는 것은 정상이고, 동일 scope의 AVAILABLE predecessor가 여러 건 존재하는 경우만 ambiguity다.

### D3. consumer별 전달 순서 — 확정

consumer마다 단조 증가하는 `consumerSequence`를 할당한다. publisher는 해당 consumer의 `lastPublishedSequence + 1`만 claim할 수 있다.

```text
BILLING sequence 10: A → B
BILLING sequence 11: B → C

10이 PUBLISHED되기 전에는 11을 claim하지 않음
```

단순 `occurredAt` 정렬은 사용하지 않는다. 여러 Identity instance가 동시에 event를 만들거나 publisher lease를 경쟁해도 Mongo Transaction의 sequence allocation과 cursor CAS가 최종 순서를 결정한다.

이 방식은 unrelated event도 앞선 dead-letter에 의해 잠시 막힐 수 있다. owner 변경 event의 예상량이 낮고 잘못된 순서로 권리를 옮기는 위험이 더 크므로 Stage 7에서는 안전한 consumer-wide FIFO를 선택한다.

### D4. transport 인증 — 확정

- Identity→Billing: VPC Lattice `AWS_IAM`, ECS application task role, SigV4
- SigV4 signing service: `vpc-lattice-svcs`
- 기본 region: `ap-northeast-2`
- Identity→Learning Core `UserMerged`: Bearer workload JWT 경계를 유지하고 typed purpose
  `USER_MERGED`를 전용 audience `learning-core-user-merged`에 고정 매핑
- 사용자 Access Token이나 Firebase ID Token을 workload 호출에 사용하지 않음

Learning Core ingress까지 SigV4로 통일하는 작업은 별도 ADR과 Jira 없이 Stage 7에 끼워 넣지 않는다.

### D5. 보존·dead-letter — 확정

- PENDING·IN_FLIGHT: terminal까지 TTL 없음
- PUBLISHED delivery: `publishedAt + P30D`에 cleanup
- DEAD_LETTER: `deadLetteredAt + P90D`를 review 시각으로만 사용하고 자동 삭제하지 않음
- event core: 모든 required delivery가 PUBLISHED된 뒤 가장 늦은 delivery cleanup 시각보다 24시간 뒤 cleanup
- reconciliation과 consumer circuit pause: 운영 해결 전 TTL 없음

Mongo TTL 실행 시각과 삭제 순서는 보장되지 않으므로 권한·전송 판단은 status와 cursor로 수행한다.

### D6. legacy와 backfill — 확정

- 기존 `user_merged_outbox`는 즉시 삭제하거나 일괄 변환하지 않는다.
- 기존 row는 기존 Learning Core publisher가 reader-first 방식으로 끝까지 처리한다.
- 신규 capture cutover 이후 merge부터 새 event core와 두 delivery를 만든다.
- 과거 Guest merge를 Billing으로 자동 backfill하지 않는다.
- Stage 7 배포 전에 이미 CLEANED된 phone account도 released alias만 보고 lineage를 자동 생성하지 않는다.
- historical backfill은 대상·중복 권리·개인정보 영향을 따로 검토한 승인된 migration에서만 수행한다.

### D7. source history migration과 phone continuation의 경계 — 확정

Stage 7에는 다음 제한된 phone continuation을 포함한다.

- Billing이 current owner transition과 기존 nonterminal AttemptGroup을 read-only로 발견
- Learning Core가 exact group·mockExamId·continuation ID를 snapshot하고 reserve에 echo
- target 소유의 새 Session을 `cycleNumber=1`로 생성
- source Session·답안·결과·Summary는 기존 source 소유로 유지

이 continuation은 남은 무료시험 사용 상태를 잇는 기능이지 과거 학습 기록을 복구하거나 이전하는 기능이 아니다.

다음 항목은 Stage 7에 포함하지 않는다.

- old account re-auth 또는 recovery proof의 종류
- proof 발급·hash 저장·만료·일회성 소비
- 과거 시험·답안·피드백 owner 이전
- 모바일 복구 UX
- source history migration용 Learning Core continuity wire event

해당 history migration 기능은 별도 제품·보안 결정과 Jira로 진행한다. 향후 새 event를 도입하더라도 기존 Billing-only `TrialOwnerRebindApproved` 의미를 확장하지 않는다.

### D8. 반복 재가입과 복수 benefit 확장 — 확정

- lineage cardinality는 Billing 응시권 개수가 아니라 `consumerScopeId`별 predecessor account 경로 수다.
- 같은 phone에 서로 다른 benefit scope의 AVAILABLE lineage가 여러 건 있어도 모순이 아니며 scope별 event를 독립 생성할 수 있다.
- 현재 `FREE_EXAM_ONCE` MVP는 한 scope의 AVAILABLE predecessor 최대 한 건을 논리 invariant로 사용한다.
- 향후 여러 무료시험 프로그램은 서로 구분되는 stable scope를 사용하고 모든 무료 혜택을 하나의 공통 scope에 넣지 않는다.
- 한 scope 안에서 여러 Billing subject를 선택적으로 이전해야 하는 제품으로 확장되면 v1 event 의미를 묵시적으로 넓히지 않고 stable benefit/program key, bounded fan-out, 부분 실패와 멱등성 계약을 별도 ADR·version에서 정의한다.

반복 chain의 event가 동시에 전달될 필요는 없다. A→B가 PENDING인 동안 B→C가 생성돼도 Billing consumer sequence가 A→B 뒤에 B→C를 전달한다. A→B가 DEAD_LETTER이면 B→C도 정지한다. A→B가 COMPLETED 때문에 NOOP여서 Billing owner가 A에 남으면 B→C는 source B의 active subject가 없어 NOOP로 수렴하며 새 무료권을 만들지 않는다.

현재 Stage 7 구현을 시작하기 위해 추가로 선택해야 하는 제품 정책은 없다. 환경별 ARN·DNS·task role처럼 실제 배포 inventory에서 확정할 값은 저장소 문서에 실제 값을 적지 않고 배포 설정에서 주입한다.

## 4. 주요 위험과 미확인 사항

### 4.1 Billing 계약 보정과 phone continuation 구현은 완료됐다

Billing `ADR-003-retained-trial-owner-rebind-contract.md`와 `develop` 구현은 다음 승인 정책을 반영했다.

- `UserMerged`만 Billing·Learning Core 두 consumer delivery
- `TrialOwnerRebindApproved`는 Billing-only
- phone rejoin의 없음/`OPEN`/`RETAKE_AVAILABLE`/`GRADING`/`COMPLETED` 상태표
- Learning Core의 read-only continuation discovery와 exact reserve echo
- source history migration 없이 target 새 Session 생성

따라서 과거 계획에 있던 “Billing ADR 보정 필요”는 더 이상 선행 차단점이 아니다. 다만 실제 AWS route·IAM, staging Mongo Transaction과 feature flag OFF 배포 증적은 코드 병합과 별개다.

### 4.2 Learning Core phone 구현은 병합 전이며 `UserMerged`는 별도다

Learning Core `TMI-122` phone continuation 구현은 로컬 전체 457개 테스트를 통과했지만 확인 시점에는 `feat/TMI-122-phone-rejoin-continuation` 브랜치에 있다. `develop` 병합 전에는 upstream readiness로 간주하지 않는다.

또한 이 구현은 `UserMerged` consumer를 대체하지 않는다. phone continuation은 Billing을 통해 nonterminal group을 발견하고 target 새 Session을 만드는 흐름이고, `UserMerged`는 canonical 계정 통합에 따라 source deny와 소유권 migration을 수행하는 별도 lifecycle이다. 두 Learning Core 경로가 모두 OFF 배포되기 전에는 관련 Identity publisher를 켜지 않는다.

### 4.3 pre-cutover 사용자는 자동 복구되지 않는다

명시적 lineage 도입 전에 CLEANED된 계정은 자동 phone rejoin 대상이 아니다. 신규 가입은 가능하지만 기존 Billing owner가 자동 이전되지 않아 미사용 무료권을 이어 쓰지 못할 수 있다.

이는 잘못된 source를 추측하는 것보다 안전한 기본값이다. 실제 대상 규모를 집계한 뒤 별도 migration 필요성을 판단한다.

### 4.4 FIFO의 head-of-line blocking

한 Billing delivery가 DEAD_LETTER 또는 circuit pause 상태면 뒤의 Billing owner event도 전송되지 않는다. 자동 skip은 금지한다.

운영자는 원인을 수정한 뒤 같은 eventId와 동일 payload를 replay한다. consumer가 이미 적용했는지 확인되지 않은 상태에서 cursor만 앞으로 이동하거나 새 eventId로 실패를 숨기지 않는다.

### 4.5 Mongo Transaction과 sequence cursor

event core, required delivery, consumer sequence allocation과 lifecycle mutation은 같은 Mongo Transaction에서 저장해야 한다. 로컬 인메모리 Mongo 테스트만으로 실제 replica set의 write conflict·unknown commit result를 증명할 수 없으므로 staging 검증이 필요하다.

### 4.6 AWS direct bypass와 환경 교차 호출

코드가 SigV4로 서명하더라도 다음 인프라 조건이 없으면 production gate를 통과하지 못한다.

- staging/prod 별도 Lattice service와 auth policy
- exact Identity application task role principal
- exact service ARN, POST method와 승인 route만 허용
- task direct endpoint를 security group으로 차단
- execution role·deploy role·반대 환경 role의 invoke 거절

### 4.7 pseudonymous data와 로그

event payload와 phone alias·lineage에는 pseudonymous identifier가 포함된다. raw phone은 저장하지 않지만 무기한 보존이 자동으로 정당화되지는 않는다. AVAILABLE lineage는 기능상 successor가 생길 때까지 필요하므로 TTL을 두지 않되 접근 통제와 현황 지표를 운영해야 한다.

### 4.8 AVAILABLE 한 건은 논리 invariant다

`(sourcePhoneIdentityId, consumerScopeId)` unique index는 같은 source의 중복 lineage만 막고, 서로 다른 과거 source가 같은 phone chain에서 동시에 AVAILABLE이 되는 것까지 DB 하나의 index로 차단하지는 않는다.

정상 반복 chain은 signup Transaction의 lineage CONSUMED CAS와 다음 withdrawal의 신규 AVAILABLE 생성으로 한 건에 수렴한다. resolver는 동일 scope의 AVAILABLE이 2건 이상이면 최신 값을 추측하지 않고 reconciliation으로 보낸다. 구현 테스트는 정상 A→B→C와 비정상 다중 AVAILABLE을 모두 검증해야 한다.

## 5. 현재 작업과 직접 관련된 구현 계획

### 5.1 포함 범위

- phone eligibility Billing delivery의 JWT→SigV4 교체
- status와 bounded `Retry-After`를 포함하는 delivery result
- Billing 공용 SigV4 JSON transport
- immutable owner event core와 consumer별 delivery
- consumer sequence allocation과 publish cursor
- consumer circuit pause·수동 replay 기반
- 기존 `UserMerged` writer의 reader-first cutover
- `PhoneRejoinLineage` 생성·소비·reconciliation
- `TrialOwnerRebindApproved` v1 생성
- Billing phone continuation 상태표·discovery·reserve echo와의 contract fixture
- direct Firebase signup과 Guest upgrade Transaction 연결
- Billing·Learning Core별 publisher와 독립 feature flag
- retention, TTL index, metric, 구조화 로그와 runbook
- domain·Repository·Transaction·publisher·transport·legacy 테스트
- cross-service staging E2E와 canary 절차

### 5.2 제외 범위

- Billing Claim·Grant·ledger 구현 또는 직접 DB 수정
- Learning Core source 시험·답안·결과·Summary의 phone 기반 owner 이전
- strong-proof recovery lifecycle
- historical Guest merge·CLEANED phone lineage 자동 backfill
- Provider unlink·전화번호 변경
- logout-all Firebase refresh revoke
- Refresh Token rotation 원자성 개선
- Guest 생성 응답 유실 복구
- 기존 ACTIVE 회원 Firebase credential rebind
- public owner-rebind API 또는 운영자 UI
- 실제 AWS ARN·DNS·credential을 저장소에 기록하는 작업

### 5.3 Phase 7-A — Billing SigV4 transport 기반

#### dependency

Identity에 AWS SDK v2 BOM과 최소 signer·credential dependency를 추가한다. Learning Core에서 검증된 계열과 맞춰 초기 구현은 다음 기준을 사용한다.

```text
software.amazon.awssdk:bom:2.29.52
software.amazon.awssdk:http-auth-aws
AWS SDK default credentials provider
```

실제 구현 시 dependency tree를 확인해 직접 사용하지 않는 S3·STS client를 불필요하게 추가하지 않는다.

#### transport port

기존 `int deliver(byte[] payload)`를 다음 의미의 결과로 확장한다.

```text
WorkloadDeliveryResult
- statusCode
- retryAfterSeconds nullable
```

`Retry-After`는 delta-seconds integer 1~300만 신뢰한다. HTTP-date, 음수, 0, 300 초과, 중복·잘못된 값은 무시한다. publisher는 `max(localBackoff, retryAfter)` 시각을 사용한다.

#### SigV4 adapter

공용 transport는 다음 순서로 요청을 만든다.

1. 배포 설정의 HTTPS base URL과 코드의 고정 route를 결합한다.
2. `Content-Type: application/json`과 새 W3C `traceparent`를 넣는다.
3. baggage는 넣지 않는다.
4. exact body bytes를 포함해 `vpc-lattice-svcs`와 configured region으로 최종 서명한다.
5. redirect를 따르지 않고 connect `PT1S`, read `PT3S` 기본값으로 POST한다.
6. status와 bounded `Retry-After`만 반환하고 response body·Authorization header를 저장하지 않는다.

서명 뒤 header나 URI를 변경하지 않는다. `Host`와 `Content-Length`처럼 JDK가 관리하는 제한 header는 중복 삽입하지 않는다.

#### phone eligibility 설정 전환

`app.phone-eligibility-publisher`는 다음 방향으로 바꾼다.

```text
enabled=false
base-url=<environment Lattice HTTPS origin>
region=ap-northeast-2
route=/internal/v1/eligibility/trial/events   # 코드 고정
connect-timeout=PT1S
read-timeout=PT3S
```

기존 eligibility `audience`와 Bearer credential runtime 의존은 제거한다. 공용 workload JWT provider 자체는 Learning Core `UserMerged`와 `UserWithdrawn`에 계속 필요하므로 삭제하지 않는다.

### 5.4 owner event core

신규 collection 이름은 `owner_event_cores`로 한다.

```text
OwnerEventCore
- eventId                                  UUID v4, _id
- eventType                                USER_MERGED | TRIAL_OWNER_REBIND_APPROVED
- schemaVersion                            1
- sourceUserId
- targetUserId
- occurredAt
- producer nullable                        trial event에서 identity
- consumerScopeId nullable                 trial event 전용
- lifecycleReason nullable                 trial event에서 PHONE_REJOIN
- sourceBindingRevision nullable           trial event 전용
- targetBindingRevision nullable           trial event 전용
- phoneRejoinLineageId nullable            internal only, wire 제외
- requiredConsumers                        immutable set
- allPublishedAt nullable
- cleanupAt nullable
- createdAt
```

event type별 invariant:

- `USER_MERGED`: trial 전용 field는 모두 null, required consumer는 Billing·Learning Core
- `TRIAL_OWNER_REBIND_APPROVED`: trial 전용 field 모두 존재, required consumer는 Billing만
- 모든 event: source와 target은 서로 다른 lowercase canonical UUID
- immutable wire field는 생성 뒤 수정 금지
- `requiredConsumers`와 실제 delivery 집합이 정확히 일치해야 함

인덱스:

- `_id=eventId`
- `USER_MERGED`에 대한 `sourceUserId` partial unique
- `TRIAL_OWNER_REBIND_APPROVED`에 대한 `phoneRejoinLineageId` partial unique
- nullable `cleanupAt` TTL

### 5.5 consumer별 delivery와 순서 cursor

신규 collection 이름은 `owner_event_deliveries`로 한다.

```text
OwnerEventDelivery
- deliveryId                               UUID v4, _id
- eventId
- consumer                                 BILLING | LEARNING_CORE
- consumerSequence                         1 이상의 long
- status                                   PENDING | IN_FLIGHT | PUBLISHED | DEAD_LETTER
- attemptCount
- nextAttemptAt nullable
- leaseOwner nullable
- leaseExpiresAt nullable
- lastFailureCode nullable
- publishedAt nullable
- deadLetteredAt nullable
- retentionReviewAt nullable
- cleanupAt nullable
- createdAt
```

인덱스:

- `(eventId, consumer)` unique
- `(consumer, consumerSequence)` unique
- `(consumer, status, nextAttemptAt, consumerSequence)` due 조회
- `(consumer, status, leaseExpiresAt, consumerSequence)` expired lease 조회
- `(status, retentionReviewAt)` dead-letter review
- nullable `cleanupAt` TTL

`owner_event_consumer_states`에는 다음 상태를 둔다.

```text
OwnerEventConsumerState
- consumer                                 _id
- lastAllocatedSequence
- lastPublishedSequence
- circuitStatus                            ACTIVE | PAUSED
- pauseFailureCode nullable
- pausedAt nullable
- updatedAt
- version
```

event capture Transaction은 `lastAllocatedSequence`를 원자 증가시켜 delivery에 할당한다. publisher는 exact next sequence만 claim하고, 2xx 뒤 delivery PUBLISHED와 `lastPublishedSequence` 증가를 짧은 Mongo Transaction으로 함께 commit한다.

외부 consumer가 commit한 뒤 Identity의 local commit 결과를 받지 못하면 같은 eventId·payload를 다시 보낸다. consumer inbox 멱등성이 최종 안전망이다.

### 5.6 phone rejoin lineage

신규 collection 이름은 `phone_rejoin_lineages`로 한다.

```text
PhoneRejoinLineage
- lineageId                                UUID v4, _id
- sourceWithdrawalId
- sourceUserId
- sourcePhoneIdentityId
- consumerScopeId
- sourceBindingRevision
- status                                   AVAILABLE | CONSUMED | RECONCILIATION_REQUIRED
- releasedAt
- claimedEventId nullable
- targetUserId nullable
- targetBindingRevision nullable
- consumedAt nullable
- failureCode nullable
- retentionReviewAt nullable
- cleanupAt nullable
- version
```

인덱스:

- `(sourcePhoneIdentityId, consumerScopeId)` unique
- `claimedEventId` partial unique
- `(status, consumerScopeId, releasedAt)` lookup·운영 집계
- `(targetUserId, status)` 후속 상태 확인
- nullable `cleanupAt` TTL

raw phone, phone last4, Firebase UID, provider subject, eligibility candidate와 HMAC key는 lineage에 저장하지 않는다. phone lookup은 현재 요청에서 새로 계산한 retained `PhoneFingerprintSet`과 기존 RELEASED `PhoneFingerprintAlias`를 사용한다.

lineage retention:

- AVAILABLE: successor 판단에 필요하므로 TTL 없음
- RECONCILIATION_REQUIRED: 운영 해결 전 TTL 없음
- CONSUMED: Billing delivery가 PUBLISHED된 뒤 `P30D` cleanup
- Billing delivery가 DEAD_LETTER이거나 circuit pause이면 cleanupAt 없음

반복 chain invariant:

- successor signup은 선택한 AVAILABLE lineage를 eventId·target과 함께 CONSUMED로 CAS한다.
- CONSUMED lineage는 이후 같은 phone 재가입의 predecessor 후보에서 제외한다.
- successor가 다시 탈퇴하면 successor PhoneIdentity를 source로 하는 새 AVAILABLE lineage를 만든다.
- 과거 CONSUMED lineage의 TTL 삭제 여부는 최신 predecessor 선택의 correctness에 사용하지 않는다.
- 동일 scope의 AVAILABLE이 2건 이상이면 `releasedAt` 최신값을 선택하지 않고 reconciliation으로 보낸다.

### 5.7 withdrawal identity release 변경

`UserWithdrawalIdentityReleaseTransactionService`는 PhoneIdentity·aliases를 RELEASED로 바꾸고 lifecycle을 CLEANED로 만드는 같은 Transaction에서 lineage를 생성한다.

순서:

1. 기존 lifecycle·User·Firebase·Social·Phone preflight 유지
2. source User가 WITHDRAWN이고 lifecycle이 `IDENTITY_RELEASE_PENDING`인지 확인
3. exact active PhoneIdentity와 alias snapshot 확인
4. consumer scope별 binding revision 조회
5. active binding이면 기존대로 REVOKED revision·outbox 생성
6. 이미 inactive면 마지막 revision이 REVOKED 상태인지 확인
7. source PhoneIdentity와 REVOKED revision으로 AVAILABLE lineage 저장
8. Firebase/Social 삭제와 PhoneIdentity/alias RELEASED 수행
9. lifecycle CLEANED CAS
10. 한 Mongo Transaction commit

lineage unique conflict나 revision 모순이 있으면 임의 overwrite하지 않고 기존 Stage 3 reconciliation 경계로 보낸다.

PhoneIdentity가 없거나 해당 scope binding 이력이 없는 User는 lineage를 만들지 않고 기존 cleanup을 완료한다.

### 5.8 direct signup·Guest upgrade 변경

다음 두 Transaction 경로에 동일한 `PhoneRejoinLineageResolver`를 적용한다.

- `FirebaseSignupTransactionService`
- `FirebaseGuestUpgradeTransactionService`

target User·PhoneIdentity·VERIFIED binding outbox를 저장한 뒤 같은 Transaction에서 다음을 수행한다.

1. 새 retained phone fingerprints와 일치하는 RELEASED alias의 phoneIdentityId 집합 조회
2. 해당 집합·consumer scope의 AVAILABLE lineage 조회
3. 0건이면 신규 phone으로 처리하고 owner event를 만들지 않음
4. 1건이면 source User WITHDRAWN, source lifecycle CLEANED, source revision REVOKED를 재검증
5. source와 target UUID가 다른지 확인
6. `TrialOwnerRebindApproved` core와 Billing delivery 저장
7. lineage를 eventId·target·target revision과 함께 CONSUMED CAS
8. signup·enrollment consume·Session 저장과 함께 commit

2건 이상 또는 partial/mismatched lineage면 owner event를 만들지 않고 비식별 reconciliation 상태를 저장한다. 신규 target 가입과 VERIFIED binding은 정상 commit한다.

정확한 lineage 한 건을 선택한 뒤 event/delivery/lineage 저장이 실패하면 signup Transaction도 rollback한다. 성공한 가입에서 rebind event만 유실되는 상태를 만들지 않는다.

반복 재가입에서 A→B delivery가 아직 PENDING이어도 B→C capture는 허용한다. 두 event는 Billing `consumerSequence`를 연속 할당받으며 B→C publisher는 A→B가 PUBLISHED되기 전 claim할 수 없다. 선행 event가 DEAD_LETTER 또는 circuit pause면 후행 event와 lineage를 임의로 건너뛰지 않는다.

### 5.9 Guest merge writer 전환

기존 `UserMerged` wire v1은 변경하지 않는다.

```json
{
  "eventId": "<lowercase UUID v4>",
  "schemaVersion": 1,
  "sourceUserId": "<canonical UUID>",
  "targetUserId": "<canonical UUID>",
  "occurredAt": "<UTC Instant>"
}
```

신규 capture가 활성화되면 Guest merge Transaction은 다음을 원자 저장한다.

```text
source User → MERGED
source RefreshSession 전체 폐기
target RefreshSession 저장
UserMerged event core
Billing delivery + Billing sequence
Learning Core delivery + Learning Core sequence
```

기존 legacy capture가 선택된 경우에는 기존 `UserMergedOutbox` 한 건을 저장한다. 한 merge에서 legacy와 신규 형식을 동시에 쓰지 않는다.

cutover 절차:

1. 신규 collection과 index 선배포
2. consumer state와 sequence invariant preflight
3. legacy row를 상태별 집계하고 IN_FLIGHT lease 회수 가능 여부 확인
4. 신규 capture flag ON
5. legacy publisher는 기존 row가 terminal이 될 때까지 유지
6. 신규 publisher는 consumer readiness 뒤 별도 ON
7. legacy collection 제거는 Stage 7 완료 후 별도 migration

### 5.10 wire event와 route

#### Billing phone rejoin

```http
POST /internal/v1/eligibility/trial/owner/events
```

```json
{
  "eventId": "<lowercase UUID v4>",
  "eventType": "TrialOwnerRebindApproved",
  "schemaVersion": 1,
  "producer": "identity",
  "consumerScopeId": "<expected opaque scope>",
  "occurredAt": "<UTC Instant>",
  "sourceUserId": "<canonical UUID>",
  "targetUserId": "<canonical UUID>",
  "lifecycleReason": "PHONE_REJOIN",
  "sourceBindingRevision": 2,
  "targetBindingRevision": 1
}
```

Identity의 책임은 위 event를 Billing에 durable delivery하는 데서 끝난다. Identity가 Billing continuation을 대신 조회하거나 Learning Core에 group·mockExamId를 전달하지 않는다.

#### Billing→Learning Core phone continuation discovery

`TrialOwnerRebindApproved`가 `APPLIED`된 뒤, Learning Core는 target 사용자에게 기존 `ExamSession`이 하나도 없고 phone continuation flag가 켜진 경우에만 Billing의 read-only route를 호출한다.

```http
POST /internal/v1/reservations/continuations/phone
```

```json
{
  "userId": "<target canonical UUID>"
}
```

응답 계약:

- 적용 가능한 continuation이 없으면 body 없는 `204`
- current owner transition이 `PHONE_REJOIN`이고 기존 group이 `OPEN` 또는 `RETAKE_AVAILABLE`이면 `200`
- `GRADING`이면 retryable `409 COMMAND_PROCESSING`
- 중복 context나 projection 모순이면 `503 BILLING_TEMPORARILY_UNAVAILABLE`

```json
{
  "continuationReason": "PHONE_REJOIN",
  "continuationId": "<TrialOwnerRebindApproved eventId>",
  "attemptGroupId": "<Billing existing group>",
  "mockExamId": "<Billing existing mock exam>"
}
```

Learning Core는 `200` 결과를 `ExamCreationOperation`에 불변 snapshot으로 저장하고 새 target `examId`를 준비한다. reserve에는 다음 세 context를 모두 exact echo한다.

```json
{
  "continuationReason": "PHONE_REJOIN",
  "continuationId": "<discovery value>",
  "expectedAttemptGroupId": "<discovery value>"
}
```

Billing은 같은 Transaction에서 current owner, `ownerTransitionReason/ownerTransitionId`, group·mockExamId와 Claim을 재검증한다. 성공한 `PHONE_REJOIN REPLACEMENT`만 Learning Core가 새 target Session으로 저장한다. discovery와 reserve 사이 상태가 바뀌거나 응답이 유실되면 Learning Core가 status-first 복구를 수행하며 Identity가 새 event를 임의 생성하지 않는다.

#### Billing Guest merge

```http
POST /internal/v1/owners/merge/events
```

payload는 기존 `UserMerged` v1 그대로다.

#### Learning Core Guest merge

```http
POST /internal/v1/events/user-merged
```

payload는 기존 `UserMerged` v1 그대로다. Learning Core가 받는 phone rejoin **event route**는 Stage 7 계약에 없다. 위 continuation discovery는 Learning Core가 Billing을 조회하는 별도 read-only 계약이다.

### 5.11 publisher 상태·오류 분류

| 결과 | 처리 |
| --- | --- |
| consumer Transaction commit 뒤 `2xx` | 해당 delivery만 PUBLISHED, cursor 전진 |
| timeout·connection·`408`·`425`·`429`·`5xx` | 같은 eventId/payload로 retry |
| Billing `503 OWNER_REBIND_PENDING` | bounded Retry-After 이후 retry |
| `400`·`409`·`413`·`415`·`422` | DEAD_LETTER, cursor 정지 |
| `401`·`403`·`404`·`405`·그 밖의 예상하지 못한 `4xx` | delivery를 PENDING으로 되돌리고 consumer circuit PAUSED |
| `3xx` | redirect 금지, consumer circuit PAUSED |
| local payload serialization 실패 | DEAD_LETTER |
| SigV4 credential resolution·서명 일시 실패 | retry, 최대 횟수 후 DEAD_LETTER |

기본 retry:

```text
leaseDuration      = PT60S
maxAttempts        = 12
initialBackoff     = PT5S
maxBackoff         = PT15M
jitter             = ±20%
Retry-After bound  = 1..300 seconds
```

consumer circuit pause는 delivery를 terminal로 만들지 않는다. pause Transaction은 현재 IN_FLIGHT delivery의 lease를 해제해 PENDING으로 되돌리고 consumer state를 PAUSED로 저장한다. 설정 수정 뒤 운영자가 circuit을 재개하면 exact next sequence부터 다시 보낸다.

### 5.12 feature flag

최소 다음 flag를 독립적으로 둔다. 모두 기본값은 `false`다.

```text
PHONE_ELIGIBILITY_PUBLISHER_ENABLED
OWNER_EVENT_USER_MERGED_CAPTURE_ENABLED
OWNER_EVENT_TRIAL_REBIND_CAPTURE_ENABLED
OWNER_EVENT_BILLING_USER_MERGED_PUBLISHER_ENABLED
OWNER_EVENT_LEARNING_CORE_USER_MERGED_PUBLISHER_ENABLED
OWNER_EVENT_BILLING_TRIAL_REBIND_PUBLISHER_ENABLED
```

publisher flag는 delivery 생성 여부를 결정하지 않는다. capture flag가 만든 durable delivery는 publisher가 OFF여도 보존된다.

consumer-wide sequence를 사용하므로 exact next event type의 publisher flag가 OFF면 같은 consumer의 후속 event도 의도적으로 대기한다. 운영 metric에 `blockedByDisabledChannel`을 표시하되 event를 건너뛰지 않는다.

### 5.13 관측성과 개인정보

구조화 completion log는 다음 값만 허용한다.

```text
service=identity
consumer
eventType
eventId
outcome
failureCode
traceId
durationMs
```

metric tag에는 eventId도 사용하지 않는다. 허용 tag는 consumer, eventType, outcome, failureCode, attempt bucket이다.

다음 값은 일반 로그·Sentry breadcrumb·trace attribute·metric tag에 넣지 않는다.

- source/target userId
- raw phone, last4, phone fingerprint와 eligibility candidate
- Firebase UID, provider subject와 email
- request/response payload와 canonical digest
- workload JWT, AWS credential, SigV4 Authorization·session token
- Lattice actual ARN·DNS

필수 운영 지표:

- consumer별 pending·in-flight·dead-letter count
- oldest pending age와 delivery lag
- current sequence gap
- circuit pause 상태와 pause age
- available·consumed·reconciliation lineage count
- legacy outbox 잔량

### 5.14 cross-service 활성화 순서

1. Billing `TMI-120` 병합본과 ADR-003의 owner rebind·phone continuation 계약을 기준선으로 고정한다. 코드 병합은 확인됐으며 staging index·Transaction 검증은 별도로 수행한다.
2. Learning Core `TMI-122` phone continuation을 `develop`에 병합하고 flag OFF로 배포한다.
3. 7-A SigV4 transport를 구현하되 phone eligibility publisher는 OFF로 배포한다.
4. 7-C event core·delivery·lineage writer를 capture OFF, publisher OFF로 배포한다.
5. Learning Core `UserMerged` consumer를 workload auth와 local Transaction까지 구현해 OFF로 배포한다.
6. staging IAM에서 Identity role의 exact Billing event route만 허용하고, Learning Core role에는 Billing continuation·reserve route의 exact 권한만 부여한다.
7. phone eligibility consumer ON → Identity eligibility publisher ON 순서로 검증한다.
8. Billing owner consumer ON → Identity trial rebind capture ON → Billing publisher ON 순서로 phone rejoin canary를 수행한다.
9. 없음/`OPEN`/`RETAKE_AVAILABLE`/`GRADING`/`COMPLETED`, discovery 204/200, reserve 응답 유실과 status-first 복구를 검증한다.
10. `UserMerged`는 Billing·Learning Core consumer ON → Identity capture ON → consumer별 publisher ON 순서로 별도 canary한다.
11. 중복, consumer별 순서 역전, lease 만료, 401/403/404/405/3xx, Retry-After와 민감정보 비로그를 검증한다.
12. backlog·dead-letter·sequence gap·privacy 지표가 정상인지 확인한 뒤 production을 단계적으로 활성화한다.

rollback은 publisher flag를 먼저 OFF로 한다. 이미 consumer에 commit된 owner 이전을 역방향 event 없이 되돌리거나 DB에서 직접 원복하지 않는다. capture를 끄기 전 미전송 delivery의 보존과 재개 계획을 확인한다.

### 5.15 Jira 분할

권장 Jira는 다음과 같다.

1. **Identity 7-A** — phone eligibility SigV4 transport·Retry-After
2. **Identity 7-C1** — owner event core·delivery·sequence·legacy cutover
3. **Identity 7-C2** — phone rejoin lineage·`TrialOwnerRebindApproved` capture
4. **Identity 7-C3** — Billing/Learning Core publisher·circuit·runbook
5. **Learning Core 7-D1 / TMI-122** — phone continuation discovery·새 target Session; 구현·검증 완료, `develop` 병합 대기
6. **Learning Core 7-D2** — `UserMerged` consumer·owner migration·source deny
7. **Integration 7-E** — Lattice IAM·staging E2E·canary

Billing `TMI-120`은 `develop` 병합이 확인됐으므로 신규로 중복 생성하지 않는다. Identity Jira는 Billing·Learning Core 구현을 다시 포함하지 않고 producer·delivery 책임과 cross-service 완료 조건만 참조한다.

## 6. 상세 조사 근거와 부록

### A. 예상 Identity 변경 파일

기존 수정 대상:

- `build.gradle`
- `src/main/resources/application.yml`
- `src/main/java/.../phoneidentity/application/PhoneEligibilityBindingDeliveryPort.java`
- `src/main/java/.../phoneidentity/application/PhoneEligibilityBindingPublisher.java`
- `src/main/java/.../phoneidentity/infrastructure/JdkPhoneEligibilityBindingDeliveryAdapter.java`
- `src/main/java/.../phoneidentity/infrastructure/PhoneEligibilityPublisherProperties.java`
- `src/main/java/.../phoneidentity/infrastructure/PhoneEligibilityPublisherConfiguration.java`
- `src/main/java/.../federation/application/FirebaseSignupTransactionService.java`
- `src/main/java/.../federation/application/FirebaseGuestUpgradeTransactionService.java`
- `src/main/java/.../federation/application/FirebaseGuestMergeTransactionService.java`
- `src/main/java/.../user/application/UserWithdrawalIdentityReleaseTransactionService.java`
- `src/main/java/.../usermerge/*`

예상 신규 영역:

- Billing 공용 SigV4 JSON transport와 delivery result
- `OwnerEventCore`, `OwnerEventDelivery`, `OwnerEventConsumerState`
- `PhoneRejoinLineage`
- owner event Repository custom CAS·Transaction coordinator
- type별 exact mapper와 delivery adapter
- publisher scheduler·circuit·manual replay application service
- startup index/preflight와 운영 runbook

실제 package 이름은 구현 시 기존 vertical slice 규칙에 맞추되 event type별 wire mapper와 생성 gate를 generic map으로 합치지 않는다.

### B. 필수 테스트 매트릭스

#### SigV4 transport

- exact method·path·body·region·service signing name
- traceparent가 서명 전에 포함됨
- redirect 미추적과 baggage 부재
- fake rotating credential과 credential failure
- status·Retry-After 1/300 경계와 invalid 값 무시
- response body·Authorization·credential 비로그
- HTTPS, user-info, query, fragment와 base URL validation

#### event core·delivery

- core와 required delivery Transaction atomicity
- `(eventId, consumer)`·`(consumer, sequence)` 중복 차단
- UserMerged exact 두 delivery, Trial event exact Billing 한 delivery
- event type별 nullable field invariant
- Billing 성공·Learning Core 실패의 독립 status
- exact next sequence claim과 multi-instance lease 경쟁
- A→B 완료 전 B→C claim 차단
- 외부 2xx 뒤 local unknown commit result의 동일 event replay
- retry, dead-letter, circuit pause·resume와 head-of-line block
- P30D/P90D/core+24h cleanup 계산

#### phone lineage

- withdrawal release와 AVAILABLE lineage 한 Transaction
- active/inactive binding revision 검증
- 0개 lineage signup 성공·event 없음
- exact 1개 lineage signup 성공·Billing delivery 생성
- 동일 scope AVAILABLE 2개 이상·owner mismatch signup 성공·reconciliation·event 없음
- 서로 다른 benefit scope의 AVAILABLE lineage는 scope별 독립 event 생성
- source WITHDRAWN/CLEANED, target VERIFIED와 revision fencing
- concurrent signup 두 건 중 lineage consume 한 건만 성공
- event 저장 실패 시 exact lineage path signup rollback
- raw phone·candidate·Firebase UID 비저장·비로그
- A 탈퇴→B 가입에서 lineage A CONSUMED, B 탈퇴에서 lineage B AVAILABLE
- C 가입 시 A의 CONSUMED lineage를 제외하고 B의 AVAILABLE lineage만 선택
- 과거 CONSUMED lineage가 여러 건이어도 남아 있는 유일한 AVAILABLE 한 건만 자동 이전 후보
- A→B delivery PENDING 중 B→C capture 허용과 Billing sequence 순서 보장
- A→B DEAD_LETTER·circuit pause 시 B→C claim 차단
- A→B COMPLETED NOOP 뒤 B→C도 source active subject 없음 NOOP
- 없음/`OPEN`/`RETAKE_AVAILABLE`이면 Identity event payload가 동일하고 상태 판정은 Billing이 담당
- `GRADING` 503에서 lineage·event가 새로 만들어지지 않고 같은 eventId delivery retry
- `COMPLETED` 204 NOOP에서도 Identity delivery가 PUBLISHED로 수렴하고 새 무료권을 만들지 않음

#### Guest merge·legacy

- legacy capture와 신규 capture 상호 배타
- 신규 merge User·Session·core·두 delivery all-or-nothing
- legacy PENDING/IN_FLIGHT/PUBLISHED/DEAD_LETTER reader-first 처리
- contradictory legacy/new row preflight fail
- historical Billing backfill 부재
- `UserMerged` v1 exact JSON fixture 불변

#### cross-service

- Billing eligibility VERIFIED→REVOKED→target VERIFIED projection
- `TrialOwnerRebindApproved`가 projection 선행 전 503, 이후 204
- Claim·Grant·consumption 값 불변
- A→B→C 반복 재가입 event가 Billing sequence 순서로 current owner를 전진
- 선행 COMPLETED NOOP chain에서 후속 event도 권리 생성 없이 NOOP
- group 없음이면 target이 일반 INITIAL로 시작
- `OPEN`·`RETAKE_AVAILABLE`이면 exact continuation discovery→echo→새 target REPLACEMENT Session
- `GRADING`이면 owner event retry 뒤 terminal 상태에 따라 재판정
- `COMPLETED`이면 owner 변경 없는 NOOP와 continuation 204
- target 기존 Session 존재 시 phone discovery를 호출하지 않음
- source Session·답안·결과·Summary owner와 데이터 불변
- reserve 응답 유실 뒤 status-first 복구와 같은 operation payload 재사용
- `UserMerged` 두 consumer 중복·순서 역전
- phone rejoin event가 Learning Core로 전달되지 않고 continuation은 Billing read-only route로만 발견됨
- Identity role만 exact route 성공
- unsigned·wrong role·wrong environment·wrong method/path 거절
- direct target 접근 거절

### C. 단계별 완료 조건

#### 7-A 완료

- eligibility Bearer runtime 경로 제거
- SigV4 signer와 bounded Retry-After 테스트 통과
- Billing staging route/IAM positive·negative 검증
- publisher 기본 OFF 유지

#### 7-B 완료

- Billing `TMI-120` contract·code·test와 수정된 ADR의 실제 route 일치 — 로컬 구현·병합 확인
- Billing owner consumer·phone continuation flag OFF 배포 — staging 확인 필요
- staging Mongo index·Transaction 검증 — 확인 필요

#### 7-C 완료

- core/delivery/sequence/lineage schema와 index 적용
- lifecycle Transaction atomicity·legacy preflight 통과
- consumer별 publisher·circuit·retention 구현
- 전체 `./gradlew clean test` 성공

#### 7-D 완료

- phone continuation discovery·operation snapshot·exact reserve echo·status-first 복구 — `TMI-122` 구현 및 457개 테스트 확인, `develop` 병합 필요
- source Session·결과 owner를 바꾸지 않는 target 새 Session 생성 검증
- `UserMerged` strict consumer와 inbox 멱등성 구현
- `UserMerged` source actor deny와 owner migration local Transaction 검증
- Learning Core phone rejoin event route가 활성 계약에 없음

#### 7-E 완료

- 세 서비스 staging E2E 통과
- backlog·dead-letter·sequence·privacy 지표 확인
- canary 중단·재개·replay runbook 검증
- consumer readiness 확인 뒤에만 production publisher 활성화

### D. 유지해야 하는 외부 계약

- 기존 공개 API와 `BaseResponse` 변경 없음
- JWT `sub`는 canonical userId 유지
- Access Token RS256·JWKS·issuer·audience 유지
- Refresh Token 원문 비저장 유지
- Python AI `user_id=examId` 유지
- raw phone 비저장 유지
- `UserMerged` v1 wire 불변
- phone eligibility VERIFIED·REVOKED v1 wire 불변
- Billing TrialClaim·Grant·consumption 소유권 경계 유지
- Identity는 Billing·Learning Core DB를 직접 수정하지 않음

### E. 다음 단계 전 확인사항

즉시 다음 순서는 Learning Core `TMI-122`의 `develop` 병합 확인 → Identity 7-A → Identity 7-C다. phone continuation vertical slice가 끝난 뒤에도 Learning Core 7-D2 `UserMerged` consumer와 7-E 운영 검증이 남는다.

Stage 7 전체 production 활성화가 끝난 뒤에만 고정 순서 Stage 8 `logout-all Firebase refresh revoke`로 진행한다. Stage 7 구현이 오래 걸리더라도 Stage 8을 위해 owner publisher를 임시 우회하거나 동기 HTTP로 바꾸지 않는다.
