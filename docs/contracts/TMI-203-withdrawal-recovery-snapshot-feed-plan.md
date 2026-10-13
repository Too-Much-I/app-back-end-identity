# TMI-203 — 탈퇴 누락 복구 API 구현 계획: Billing 로컬 진행 관리

> 2026-10-13 원천 정리 최소안: 사용자 수락에 따라 별도 승인 저장소·만료/갱신·외부 운영증명 공급 시스템을 요구하지 않는다. §5.6.1·§5.8이 해당 운영 요구를 대체한다. 보존·lease·source CAS·삭제/가용성 기록 원자성·참조 보호는 유지한다. TMI-203 설명은 미리보기 승인 후 반영·재조회했으며 제목/상태/댓글은 변경하지 않았다. 최소안 코드 적용·배포 완료를 의미하지 않는다.

> 2026-10-12 인증 방향 개정: 사용자가 **기존 ALB + 전용 workload JWT + 전용 connector/security chain + 배포형 public JWK set 초기안**을 채택했다. 본 계획의 Identity–Billing 인증 요구는 아래 §5.2.1을 따른다. 이전 Lattice/SigV4 전제는 이 범위에서 대체하며 실제 실행 코드/배포가 전환됐다는 뜻은 아니다. LC 인증과 과거 수락 기록은 유지한다.
>
> 데이터 규격은 [recovery-r1 exact](TMI-203-recovery-r1-exact.md) 및 scalar-end-1 공유 자료를 따른다. 사용자 전달 Billing 회신의 해당 보완 수락과 전체203/204 완료를 구분한다. 구현 현황은 CURRENT_STATE가 기준이며, 아래 과거 후보 표보다 exact 자료가 우선한다.

- 개정일: 2026-10-13 / 현재 브랜치: feat/TMI-203-withdrawal-snapshot-feed-recovery / Jira: TMI-203, 연계 TMI-204.
- 상태: **사용자 승인 단순화 방향을 반영한 현행 계획. 구현·배포·판매·자동삭제 승인이 아니다.**
- 핵심 변경: Identity 원격 ACK/checkpoint/consumer generation을 제외하고 Billing 로컬 checkpoint/epoch로 적용 진행을 관리한다.
- 인증 exact의 환경 매핑/포트/TLS 등은 §5.2.1에 따라 공동 동결한다. 사용자 방향 승인을 모든 exact 값의 양 서버 수락으로 확대하지 않는다. Jira TMI-203 설명은 수정안 제시와 후속 사용자 승인 후 원격 반영·재조회했다. 제목/상태/댓글은 변경하지 않았다.
- 코어·Mongo·HTTP/runtime/snapshot jobs·JWT/connector·SigV4 제거·source 정리 jobs는 로컬 구현됐고, 직전 실DB 포함 회귀는2102통과/6skip이다. 이번 최소안 코드 정합화·실경보·운영 연결·공동복원은 미완료다. [최근 로컬 검증](TMI-203-source-maintenance-local-verification.md)과 CURRENT_STATE를 따른다.
- 최신 사용자 채택(2026-10-12): 미구현 복구 API는 기존 예정 `/internal/v1/billing/withdrawals`로 최초 구현하며 v2 URL 분리는 하지 않는다. source 소실은 replayFloor와 별도 중간 range 문서·인접 병합·prefix 편입으로 관리한다. [통합 Billing 인계](TMI-203-Billing-consolidated-handoff-2026-10-12.md)가 이전 versioned URL 권장과 interval 미선택 상태를 대체한다. 세부 schema/index/fixture는 후속 동결 대상이다.
- 후속 공동 검토: [Billing 인계서](TMI-203-to-Billing-TMI-204-simplified-recovery-handoff.md), [exact 개정 미결안](TMI-203-single-authority-exact-amendment-response.md). 미결안의 snapshot 생성 body·멱등 index 등은 수락 후 본문에 확정 반영하며 현재 후보와 차이를 구분한다.
- 기준: [단순화 검토 회신](TMI-203-204-recovery-simplification-review.md), [공동 계약](../../../billing/docs/contracts/IDENTITY-BILLING-PAYMENT-LIFECYCLE-TECHNICAL-CONTRACT.md), [기존 exact](billing-c0-exact-wire-index-proposal-2026-10-07.md), [Billing204 계획](../../../billing/docs/plans/TMI-204-withdrawal-recovery.md).
- [개정 전 보관본](TMI-203-withdrawal-recovery-plan-before-simplification.md)은 상세 조사·기술 비교 이력이며 현행 구현 지침이 아니다.

## 1. 5줄 결론

1. Identity는 탈퇴 원천과 snapshot/feed를 제공하고 Billing만 자기 DB의 적용 진행과 복구 epoch를 관리한다([회신 §5](TMI-203-204-recovery-simplification-review.md)).
2. Identity recoveries/checkpoints/checkpoints-status와 원격 generation은 제외한다.202 push204·Billing commit 후 응답과 멱등 처리는 유지한다.
3. 고정 T/H snapshot·digest·고정 H2 서명 cursor는 유지한다. live scan 삭제 경합을 해결할 새 장치를 추가하지 않는다.
4. source120일 정리는 Identity가 재생 가능 범위를 기록하고, consumer 적용 여부·GAP 판단은 Billing으로 이동한다. local epoch fence·복원 격리가 필수다.
   별도 정리 승인·갱신 시스템은 제외하고 DB 내부 보호와 일반 배포/복원 운영으로 최소화한다(§5.6.1).
5. 기존 ALB에 서버 전용 JWT·connector를 연결하는 방향으로 인증을 개정한다. 최초 출시 legacy COMPLETE 예외·기준점 이후 검증·판매/삭제 분리는 유지한다.

## 2. 사용자가 반드시 읽어야 하는 내용

### 2.1 정상 동작과 책임

Identity202가 탈퇴 사실을 원자 저장·push한다. Billing201은 적용 commit 뒤204를 반환한다. Billing204는 자신의 checkpoint 이후 feed를 주기적으로 읽고 **탈퇴 반영과 진행점 저장을 같은 transaction으로 확정**한다. Identity에 별도 복구 ACK를 보내지 않는다.

Identity는 Billing의 적용 완료를 권위 있는 상태로 저장하지 않는다. 따라서 remote 높은 순번을 Billing 복원본에 복사하지 않는다. local 증거가 유실되면 복원 절차로 확인한다. 초기화/특수 복구에는 snapshot, 평상시에는 push+feed를 사용하며 매 poll마다 snapshot·새 epoch를 만들지 않는다.

- 유지:200/201/202 발급 보호·탈퇴/journal/delivery 원자성·push 재시도·204-only·멱등 inbox·source stream·보존.
- 유지:T/H snapshot·불변 rows/digest·H2 feed·source 누락/만료, Billing local epoch/version/restore barrier.
- 제외:Identity remote consumer checkpoint/control/recovery operation, BASELINE/FEED ACK, 원격 세대 발급, Billing ACK jobs·원격 대사.
- 대체:consumer coverage는 Billing local로, Identity cleanup은 source availability 기록으로 분리.

원격 generation 제거는 Billing old-worker 보호 제거가 아니다. lease 만료·프로세스 재시작만으로 restore 안전을 보장하지 않는다.

### 2.2 최초 출시 전제와 한계

사용자는 기존 Billing 구매 계정·purchaseAccountRefId 발급·실제 거래가 없다고 확인했다. 과거 legacy COMPLETE 증명은 최초 출시 필수에서 제외하고 동일 전제 확인을 반복 요구하지 않는다. 외부 DB 조사 결과라고 표현하지 않는다.

기존 탈퇴자는 snapshot에 포함한다. 증거 없는 legacy PARTIAL/UNKNOWN을 유지하고 기준점 이후 GAP·복원 증거 소실을 이 예외로 통과시키지 않는다. 결제 시작 기준점에는 환경·stream·순번·관측 시각·capture 준비 근거·최초 판매 활성화 순서를 기록한다. 날짜만으로 commit 경계를 대체하지 않는다.

원천과 User tombstone이 모두 소실되면 완전 복구할 수 없다. 새 snapshot으로 과거 원 event 수신 증거가 생성되지 않는다. 비동기 지연 중 이미 발급된 token이 즉시 무효화됐다고 가정하지 않는다.

## 3. 결정 사항과 구현 전 공동 동결

단순화 방향은 승인됐다. 다음은 새 제품 선택이 아니라 구현 전 기술 합의다.

1. Billing이 local checkpoint/epoch/consumer coverage authority를 맡고 restore 때 구worker DB 접근 제거·drain·격리를 보장한다.
2. 공동 계약/TMI-199 exact revision에 제거 route/필드, snapshot 멱등 scope, source availability DTO, 오류·index를 반영한다.
3. 배포된 구client 유무를 확인해 versioned 경로 또는 명시 전환을 확정한다. endpoint를 조용히 바꾸거나 가짜204를 반환하지 않는다.
4. Billing205는 local 증거·거래 확인·source 가용 범위로 삭제 안전성을 판단한다. UNKNOWN을 정상화하거나 전역 purge 안전장치를 제거하지 않는다.
5. 이번 최소안의 Jira 본문/완료 조건 수정안을 먼저 제시하고 승인 후 반영한다. 제목·상태·댓글은 변경 대상이 아니다.

실제 index 생성·이관·cleanup apply·배포·판매는 별도 승인이다. 보존 연장·추가 개인정보·새 이용 제한이 필요하면 영향과 대안을 먼저 제시한다.

## 4. 주요 위험과 미확인 사항

| 위험 | 대응 |
| --- | --- |
| checkpoint만 적용 데이터보다 앞서 복원 | account/lifecycle/inbox/control 일관 복원 검증. 선택적/불명 복원은 C 신뢰 중단 |
| 옛 worker가 복원 DB에 접근 | 구접근 제거·drain·격리 후 새 local epoch 설치,같은 control 실제 CAS 쓰기 |
| Identity가 consumer lag를 모름 | Billing 지표+독립 heartbeat,Identity는 원천 만료/가용성 관측만 제공 |
| 고정 T 미지원/history 소실 | Mongo/driver spike,live 조회 fallback 금지 |
| cleanup과 feed 경합 | 짧은 일관 읽기로 counter/floor/interval/journal 연속성 확인,H2 이전 빈 성공 금지 |
| source restore/옛 endpoint 잔존 | 새 stream·source topology 격리,cross-stream 숫자 비교 금지 |
| 임시자료 기한과 worker 경합 | run lease/version·expiry CAS,7일/120일 임의 연장 금지 |
| 대상별 GAP을 알 수 없음 | Billing C와 대조,특정 불가 시 넓은 삭제 보류 가능 |
| 기존202 Evidence가 remote CAS 요구 | port 책임·회귀 테스트 개정 필요,그대로 완성됐다고 보고하지 않음 |

실제 ALB/JWT/복원과 Billing204 구현·배포 상태는 이번 문서 수정에서 검증하지 않았다. 기존 Identity 두 ECS 서비스의 ALB 연결 확인을 Billing 배포 증거로 일반화하지 않는다.

## 5. 현행 구현 계획

### 5.1 순서와 PR 단위

| 단계 | 산출물 | 완료 조건 |
| --- | --- | --- |
| 0. 공동 exact·spike | 책임 이동 수락,새 wire/index fixture,고정 T/H·source 일관 읽기 검증 | 미동결 필드 임의 구현 금지,지원 불가 보고 |
| PR1. 조회 기반 | internal auth/strict DTO,availability,snapshot 멱등·index/preflight | auth/stream/expiry/숫자 경계,기본OFF |
| PR2. snapshot·feed | builder/page/digest,H2 cursor,연속 feed/누락 구분 | late commit·부분 build·history loss·cleanup 경합 |
| PR3. cleanup·관측·연동 |202 Evidence 개정·floor/interval·보존 job,Billing local 진행/복원 연동 | crash·old-worker·복원·상황별 gate 검증 |
| 인증 개정 | §5.2.1 exact·auth fixture → receiver → sender OFF 구현 | 환경/port/키/오류/시간 합의,기존사용처조사·자동fallback금지 |
| 활성화 검증 | 운영 동등 Mongo/ALB/JWT·복원·기준점·dry-run | 개별 실행 승인,코드 테스트만으로 판매 허가하지 않음 |

세 PR은 검토 단위다. 기본 테스트는 mock,실제 Mongo 실험은 격리 harness이며 application-test.yml에서 Atlas/실제 Provider를 호출하지 않는다.

### 5.2 Identity API 경계

내부 API4개를 유지하는 방향이며 최종 revision/field는 공동 동결 대상이다.

| 역할 | 유지 동작 | 변경 |
| --- | --- | --- |
| snapshot 생성 | Idempotency-Key,BUILDING202/Retry-After5,종료 동일 run200 | generation header 제거,consumer/environment/stream/key scope |
| snapshot 상태 | READY만 manifest,FAILED만 failureCode | remote generation 제거,source 사실만 제공 |
| snapshot page | immutable items/nextCursor/done,20건/16KiB | source/run/소유자 cursor binding 유지 |
| feed page | stream/after/cursor,H2·연속 sequence·원 event | generation 제거,source availability 요약 추가 |

기존 /recoveries, /checkpoints, /checkpoints/status는 개정 계약에서 제외한다. push204는 유지한다. consumer status는 Billing 운영 조회로 제공하며 Identity에 사실상 같은 ACK 저장소를 다시 만들지 않는다.

Identity–Billing은 기존 ALB + 서버 전용 workload JWT와 환경/주체/route/connector 권한 분리로 전환한다. 사용자JWT·LC토큰·무인증 또는 SigV4/JWT 실패 시 자동fallback은 금지한다. snapshot worker와 source 관리/cleanup apply 권한을 분리한다. caller가 consumer/environment/userId body로 임의 범위를 고르지 못한다.

전용 strict DTO/decoder 사용,public Jackson/BaseResponse 전역 변경 금지. 필수 nullable/상태별 생략·중복/추가 키 거절·D0/D1/U32·UUID/Instant·16KiB/20건 유지. 정확 필드와 availability 상한은 새 exact에서 동결한다.

### 5.2.1 ALB·workload JWT 전환 계획 — 이번 채택 범위

**2026-10-13 후속:** 사용자 제거 요청과 Billing 회신에 따라 [SigV4 제거·JWT 단일화](TMI-203-SigV4-removal-implementation-2026-10-13.md)를 로컬 구현했다. 기존 선택 플래그/region은 잔존 시 기동 오류이며 publisher ON/OFF는 유지한다. 실배포 구인증 사용처·전환 검증 조건은 유지하고, 이전 SigV4 보존 설명은 최초 이행안 기록으로 구분한다.

**후속 구현:** 사용자 전달 Billing 수락과 구현 요청에 따라 [JWT 수신·발신 로컬 구현](TMI-203-workload-jwt-implementation.md)을 추가했다. 기본 OFF, 실제 키/환경/ALB·TLS 구성과 공동 연동 검증은 별도다. 아래 미구현/공동수락 대기 설명은 계획 작성 시점이며 최신 구현 상태는 위 문서를 따른다.

후속 [인증 exact 후보](TMI-203-204-workload-jwt-exact-candidate.md)에 시간 경계·키/header 검증·목적별 route·오류 순서와 회귀 목록을 구체화했다. 공동 수락 전 검토안이며 미확정 환경/포트/TLS 값을 대체하지 않는다. 실제 코드 전환과 배포는 아직 수행하지 않았다.

근거: [Billing 인계](TMI-203-204-ALB-workload-JWT-handoff-2026-10-12.md), [Billing 회신 검토](TMI-203-ALB-JWT-Billing-response-review-2026-10-12.md).

**채택 방향**

- 신규Lattice도입을필수로하지않고기존ALB의HTTPS경로를재사용한다. workload 전용targetgroup/connector/securitychain을두고publicport에서는internal경로를거절한다. Identity/Billing의실제port번호는충돌조사후정한다.
- 각서버는환경/서비스별전용RSA키로RS256JWT를발급한다. 상대에는배포형public JWK set만제공하며private key를공유하지않는다. 사용자JWT/cursor/support키와분리한다. remoteJWKS자동fallback없음.
- Identity발신4이벤트: withdrawal,trial eligibility,trial owner,user merge. Billing발신은Identity복구4POST. 기존LC호출은이번전환에포함하지않는다.
- 매HTTP시도새JWT,동일eventId/operationId/body재시도. 무인증·사용자JWT/LC토큰·Lattice permitAll을새ALB workload경로에허용하지않는다.
- body서명없는TLS+짧은JWT+route권한을초기권장구조로채택하되SigV4와보장이같다고하지않는다. 유효Bearer재사용가능성과credential유출위험을공동exact보안수락에명시한다.

**구현 전 공동 확정 항목**

| 항목 | 계획 |
| --- | --- |
| 환경 | Identity test/staging/production 실제배포,WITHDRAWAL_ENVIRONMENT,Billing SANDBOX/PRODUCTION,issuer/audience/key의대응표. test/staging자동동일화금지,source환경자동이관금지 |
| issuer/audience | Billing회신의환경별URN·목적별audience후보를기준으로정확문자열동결. identity-service/billing-service주체와route고정매핑 |
| JWT | RS256/kid,별도RSA최소2048bit,iss/sub/aud/iat/exp/jti. 단일문자열aud/정수NumericDate,TTL최대60초/skew30초권장. nbf관계·경계·overflow·중복claim fixture동결 |
| 공개키 | kid유일/공개키만/alg·use·key_ops검증,누락·손상기동실패,unknownkid401. old+new수신배포→새발급→old발급종료확인→최소120초와clock근거확인→old제거. 긴급폐기는신뢰제거우선 |
| 네트워크 | 실제origin/port/targetgroup/SG,공개ALB internal노출허용여부,ALB→task TLS/격리조건. ALB SG만으로caller신원을신뢰하지않음 |
| 오류 | 인증401/검증된주체의권한403과기존auth중단/재개정책정합화. 토큰/원문로그없음 |

**PR 및 완료 기준**

1. 인증전용revision/fixture추가,Identity–Billing범위SigV4전제개정. body/schema/hash/과거199수락보존. Billing AGENTS/ADR은Billing담당이동일범위정리.
2. 수신전용connector/JWT검증/route권한기본OFF. Identity의현재경로전용chain에port격리추가,기존IAM전제port는새인증계약으로명시변경.
3. 양방향발급기/HTTPS전송adapter기본OFF. 기존SigV4사용처·배포모드조사전삭제금지. 신규trial경로누락금지.
4. 사용자/LC/wrongenv·issuer·aud·sub·alg·kid·시간/무토큰/중복header/wrongport·method·path/forwarding위조·직접task접근거절검증. 정상/긴급rotation·키손상·clock장애·응답유실/중복·인증중단재개검증.
5. staging배포망·Billing복원공동검증과실경보수신확인후개별승인활성화. 코드테스트만으로배포/판매/삭제승인하지않음.

TMI-202의 인증 전송부와 공통 이벤트 경로도 영향 대상이나, 그 이슈의 상태/완료 이력이나 LC 계약을 이번 계획 수정으로 변경하지 않는다.

### 5.3 snapshot

- 같은 snapshot read T의 WITHDRAWN User와 lastSequence H를 읽는다. BSON Timestamp를 Date로 대체하지 않는다.
- 모든 User keyset page는 동일 T. canonical ID/simple collation/userId ASCII 순서 검증,기존 ID 임의 소문자 변환 금지.
- durable rows를 bounded write한 뒤 count/order/page/digest 검증,run lease/version·source stream fence와 READY 원자 확정. consumer generation CAS는 제외.
-5분 budget,history 소실/동일 T 재개 불가/timeout은 FAILED. 부분 공개나 live 조회 전환 금지.
- 사실 DTO는 userId/withdrawnAt,synthetic eventId 금지. canonical field 순서 userId→withdrawnAt,UTF8 noBOM,item 앞 unsigned32 BE byte 길이,전체 SHA256 lower64hex. Instant0/3/6/9자리·정밀도 유지.
- 빈 snapshot은 zero-byte digest·빈 최종 page1개. READY page 경계 불변. metadata16KiB 초과 시 조용한 절삭 금지.
- 동일 operation은 동일 run,scope가 다른 key 재사용은 충돌하도록 동결. 실패/만료 재시작은 새 key지만 같은 Billing 논리 job의 bounded 자동 예약 가능.
- rows/digest/staging/cursor 매핑은 run 시작+7일 이내,operation 최소 metadata120일. retry로 연장 금지.

Billing은 page 적용+진행을 원자 저장하고 마지막 digest/count 검증 뒤 local baseline H를 확정한다. remote baseline ACK 없이 feed로 진행한다. 같은 epoch에서 높은 진행을 낮은 H로 덮어쓰지 않는다.

### 5.4 feed·cursor·availability

첫 majority 관측 inclusive H2를 고정하고 environment/consumer/stream/startExclusive/H2/issuedAt/expiresAt에 서명한다. TTL1시간,이후 목표 불변이다.

- 다음 순번부터 H2까지 최대20건,nextAfterSequence=scannedThrough,done은 H2 도달과 동치.
- after<H2인데 다음 순번 없음은 floor/interval 확인 후410/409/조사중503. 빈 완료 금지.
- 짧고 일관된 source 읽기에서 stream/counter/availability/journal을 검증한다. 삭제와 availability 갱신 원자성도 검증.
- availability 후보는 replayFloor·중간 unavailable interval 요약·관측 시각. consumer 적용 여부를 추측하지 않는다. 표현 상한/overflow는 exact 동결.
- Billing 단일 feed job/control에 현재 H2/cursor/C/lease를 저장하고 별도 과거 scan 이력은 기본 요구하지 않는다.
- 만료 시 local C에서 새 scan,이전 scan 완료로 표시 금지. 위조/타 consumer/stream 거절,cursor를 URL로 해석하지 않음.
- 전용 Secret key ring/HMAC·key id 권장,기존 JWT/다른 키 재사용 금지. snapshot7일/feed1시간 rotation overlap 검증.

원 event4필드 유지. feed가 먼저 반영된 뒤 push가 오면 기존 inbox로 NOOP. Identity delivery는 실제 push 응답으로만 전진한다.

### 5.5 Billing local checkpoint·epoch 의존 계약

Identity 저장소에서 Billing 코드를 구현하지 않는다.

1. 요청 전 local epoch·lease version·C를 context에 고정한다.
2. 응답 검증 뒤 같은 transaction에서 epoch/control 실제 CAS 쓰기,event/inbox,연속 checkpoint/job 진행을 commit한다. NOOP도 fence 통과.
3. commit 후 crash는 저장된 C로 재개,미commit은 재조회. unknown commit은 durable 확인 없이 전진 금지.
4. 일반 retry/재시작은 같은 epoch. restore/재초기화만 구worker 격리 후 새 epoch 설치.
5. 적용 데이터+C가 일관 복원됐고 원천이 남으면 C부터 재개. 선택적/불명 복원은 C를 신뢰하지 않고 bootstrap/대사.
6. old payload에 새 epoch를 붙이지 않는다. old response는 local CAS 거절,Identity에는 old ACK가 바꿀 consumer 상태가 없음.

source restore는202 새 stream/sequence0 유지. 다른 stream 숫자 비교·ACK 복사 금지. endpoint/DB 격리 없이 UUID만으로 restore 안전을 주장하지 않는다.

### 5.6 source cleanup·보존 개정

사용자가 채택한 저장 방향은 기존 stream의 replayFloor + 별도 range 컬렉션이다. 인접/겹치는 range는 병합하고 prefix와 연결되면 floor에 편입한 뒤 중복 range를 제거한다. 가용 구간을 건너뛴 병합과 무한 embedded 배열은 금지한다. bounded batch·source CAS·삭제/증거 동일 transaction을 유지한다. 새 컬렉션/index 정확 명세는 후속 작업이며 아래 예상10개 index에 추가 영향이 있을 수 있다.

[WithdrawalSourceCleanup.Evidence](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalcapture/WithdrawalSourceCleanup.java)의 consumer generation CAS/coverage 요구를 source timing/stream fence·replay availability 원자 기록으로 변경한다.202 영향이므로 코드/테스트 계약을 함께 개정한다.

- capturedAt+120일·source stream·유효 lease·명시 apply 설정은 유지한다. 별도 운영 승인/시간 증명 공급 요구는 §5.6.1의 인프라·운영 책임으로 변경한다. capture 자체의 원자성·기존 시간 예산 검사를 제거하는 변경은 아니다.
- expiry와 journal/delivery 삭제,floor/interval 기록을 같은 transaction으로 처리. ACK로 조기 삭제/기한 연장하지 않음.
- prefix만 floor 증가,중간 만료는 interval. 원천 부재와 Billing 미적용을 같은 뜻으로 취급하지 않음.
- Billing은 C와 가용 범위 대조.C 이전 만료는 정상 적용 증거 유지,필요 구간 소실은 GAP·동일 무한 retry 금지.
- Billing 정지 중 consumer 판단이 늦어지는 한계는 독립 heartbeat/원천 만료 경보로 보완.
- metadata overflow로 누락을 숨기거나 무기한 보존하지 않음.

| 대상 | 보존 |
| --- | --- |
| journal/delivery | capturedAt+120일 |
| snapshot rows/digest/staging/cursor 매핑 | run 시작+7일 이내 |
| snapshot/필요 최소 operation metadata | 생성+120일,개인정보/digest 장기 복제 금지 |
| current stream 최소 source 증거·Billing local 증거 | 각 주체가 운영 중 필요한 최소 범위 유지 |
| 폐기 stream metadata | 종료+120일 |

명시 bounded cleanup/dry-run/apply,business TTL 추가 없음. User tombstone 및205 거래/계정 보존 정책 불변.

### 5.6.1 사용자 수락 최소 정리안 — 2026-10-13

**실행 흐름:** 주기적 bounded 조회 → capturedAt+120일 경과 확인 → 유효 전송 lease면 보류 → source CAS와 journal/delivery 삭제·floor/range 기록을 같은 transaction으로 commit. 충돌은 전체 rollback 후 다음 실행에서 새 조회한다. unknown commit을 동일 작업의 무조건 즉시 재시도로 처리하지 않는다. 반복 오류는 장애 단위 경보 대상으로 둔다.

- 유지: 정상 구간을 건너뛰지 않는 range/floor 계산, index/schema preflight, source incarnation·version 보호, 만료/경계/rollback/중복/복원 회귀.
- 폐기 stream 정리 유지: closedAt+120일, 대체 ACTIVE 존재, journal/snapshot run 참조 부재, bounded range 삭제 후 metadata 제거. 원본이 남은 영역을 전역 삭제하지 않는다.
- 제외: 별도 승인 레코드 collection/API, 승인 TTL·갱신·철회 worker, 정리마다 외부 승인/clock/writer 증명을 요청하는 시스템, 전용 구writer 자동 조사 시스템.
- 운영 책임: 인프라 시간 동기화와 기존 모니터링/장애 대응, 관련 배포 완료 후 구task·진행writer 종료 확인, DB 복원 시 구writer 격리 및 새 source 경계 확인. 별도 안전증명 시스템의 신규 개발을 완료 조건으로 두지 않는다.
- 보장 차이: 앱이 절대시각 오차를 독립적으로 탐지·차단한다고 보장하지 않는다. 큰 시간 오차 시 조기 만료 판정 위험이 남으며, 실제 환경의 동기화/운영 절차는 배포 전에 확인한다. 정리OFF는 무기한 보존 승인이 아니다.
- 적용 경계: 정리 전용 `RecoverySourceJobsConfiguration.Approval` 및 RuntimeEvidence 주입은 로컬 코드에서 제거하고 설정/scope/DB preflight로 정합화했다([구현 기록](TMI-203-minimal-cleanup-implementation.md)). 공용 `RuntimeEvidence`의 capture·snapshot/feed·구매 소비처는 유지했다. 상시true 구현이나 전역 삭제 없이 정리와 분리했으며 공용 운영 연결은 여전히 미완료다.
- 비대상: Billing checkpoint/epoch, snapshot T/H/digest/H2, 원격ACK 부재, workload JWT 시간 claim 검증, 구매 권한·탈퇴 원자성 및 LC/user 인증. 기존4개 API/body/schema/hash/index/보존기간 변경 없음.

완료 기준: 별도 승인 provider 없이는 켤 수 없는 정리 구조를 최소안에 맞게 정합화하고, OFF/ON 구성·120일 경계·lease 보류·동시 변경 rollback·unknown commit 재조회·참조 보류·bounded batch를 회귀한다. 외부 운영증명 장애를 흉내 낸 permissive adapter를 완성으로 처리하지 않는다. Billing에는 운영 전제 변경을 공유하되 API 변경으로 보고하지 않는다.

### 5.7 실패·판매·삭제 분리

| 상황 | 복구 처리 | 판매/삭제 판단 |
| --- | --- | --- |
| 출시 이전 legacy 불명 | PARTIAL/UNKNOWN 유지,완전성만 위한 반복 없음 | 최초 출시 단독 차단 사유 아님,205 후보 증거 별도 |
| 일시 network/5xx·lag | 같은 job/C backoff+jitter,장기화 장애 단위 경보 | 전체 구매/무료 자동 차단 금지,삭제 증거 부족 후보 보류 |
|401/403·계약 오류 | 반복 중단,인증/배포 장애 단위 조치 | 탈퇴 차단 손상 범위 평가,단순 lag로 취급 금지 |
| 실제 GAP/원천 만료 | 범위/사유 기록,가능한 상태 복구;과거 event 공백 유지 | 영향 구매 보호 확인,삭제 범위 불명확하면 넓게 보류 |
| Billing/source restore | 격리/barrier·새 epoch/필요시 stream·대사 | 영향 구매 쓰기/삭제는 근거 확인 후 재개,전체 Identity와 자동 연결 금지 |

204 retry는 bounded job budget 후 느린 probe/경보 권장,정확 값은204에서 동결한다.202 기존20회/재개 정책은 이번에 변경하지 않는다.205 매시간·매일·7일 검토를 모든203 오류의 개별 승인으로 확장하지 않는다.

### 5.8 운영 준비

기본OFF → 인증exact/fixture·환경/port/TLS합의 → index preflight/명시 준비 → 운영 동등 Mongo/ALB/JWT → Billing 연동 → cleanup dry-run → 개별 승인 순서다. capture 시작 뒤 OFF rollback/미지원 writer 복귀 금지. 복구 API OFF와 원천 기록 OFF는 다르다.

별도 정리 승인/증명 시스템은 미완료 gate에서 제외한다. 다만 현재202 RuntimeEvidence의 공용 소비처와 정리 Approval을 최소안에 맞게 코드 정합화하는 작업은 남아 있다. PurchaseActivationEvidence는 이번 정리 단순화의 일괄 제거 대상이 아니며 lag와 실제 손실/복원을 구분한다. true adapter로 미확인을 정상화하지 않는다. metrics는 build/history 실패·gap/expiry·source 만료·Billing lag/heartbeat,로그는 사용자 원문/token/cursor/서명/Secret 제외. JWT의 시간 검증/키 교체 조건은 별도 인증 계약대로 유지한다.

## 6. 부록 — 변경 명세·검증·근거

### 6.1 API/DB/index/fixture

아래 표는 단순화 시점의 변경 비교 이력이다. 실제 operation unique key·16개 index·body규격은 recovery-r1/scalar-end-1 exact 자료가 우선하며 이 표의 예상10개/13개 및stream포함key후보를새구현지시로사용하지않는다. 이번개정은인증범위이며데이터fixture를수정하지않았다.

| 기존 | 현행 계획 |
| --- | --- |
| remote API3개 | 제외,구client 조사/명시 revision 후 전환 |
| generation header/manifest/cursor | 제거,인증 소유자·stream/run binding 유지 |
| Identity checkpoints/recovery controls/recovery operations | 새 구현 제외,운영 존재 미확인으로 drop 지시 아님 |
|13개 index manifest | I2의7개 유지,snapshot3개 중 operation key 개정,remote3개 제외 방향 |
| snapshot operation unique key | environment/consumer/stream/operationId scope로 exact 개정 |
| Billing ACK jobs/별도 scans | 제외·control/job 통합,세부204 동결 |
| consumer coverage/remote baseline | Billing local 상태로 이동 |
| source availability | bounded floor/interval/관측 정보 추가,exact 동결 |
| fixture | event/digest/numeric 재사용,새 revision/error/index/local fence 추가 |

manifest/hash를 조용히 덮어쓰지 않는다. 동일 이름 index key 변경은 additive migration 명세 후 실행,자동 drop/recreate 금지. startup은 검증만. 기본 _id_,simple collation,sparse/partial/TTL 검증 유지. 예상10개는 성능 index 추가 필요까지 확정한 숫자가 아니다.

### 6.2 필수 테스트

| 묶음 | 근거 |
| --- | --- |
| exact/auth | nullable/생략·추가/중복키·D0/D1/U32·UUID·16KiB/20건·권한/직접 ingress |
| snapshot | 동일 T/H,late commit·동일 timestamp·H>0·빈/다중 page·digest golden |
| builder | history loss/timeout/중복 worker/restore/7일 만료 시 부분 READY·baseline 금지 |
| feed/cleanup | H2·연속 sequence·MAX·중간 만료/prefix floor·동시 삭제 빈 완료 금지 |
| commit/crash | Billing 적용+C 원자성,unknown 대사,재조회 NOOP,ACK 없이 진행 |
| local fence | lease 재할당/new epoch/restore 뒤 old response·NOOP·account 쓰기 거절 |
| restore | 일관 backup C 재사용 조건,선택적 증거 소실 skip 금지,source 새 stream |
| 복구 한계 | snapshot 성공으로 과거 GAP/삭제 보류 자동 해제 금지 |
| 정책/회귀 | lag와 GAP/restore 분리,205 보류,보존120일/7일,기존 LC/push/token/fence |

구현 후 기본 ./gradlew clean test. 실제 Mongo/ALB/JWT·복원 증거는 별도이며 mock으로 대체하지 않는다.

### 6.3 비용·개정 이력

remote API3개·Identity 상태3종,Billing ACK worker/outbox·대사가 줄고 consumer 판단/경보 책임이 Billing으로 이동한다. 일정/금액 절감 수치는 미측정이다.

live overlap·lease-only 비교와 fixed T/H/digest/H2 유지 근거는 [검토 회신](TMI-203-204-recovery-simplification-review.md)에 보존했다. 기존 exact field·13개 index·remote 테스트 표는 [개정 전 보관본](TMI-203-withdrawal-recovery-plan-before-simplification.md)의 역사적 기준이며 현재 구현 범위가 아니다.

이전 승인: 최초 출시 legacy COMPLETE 증명 제외. 이번 승인: Billing local authority 중심 계획 수정. 공동 exact/Billing 계획/Jira·제품·DB·배포·판매/삭제는 이번에 변경하지 않았다.

### 6.4 변경·검증·Jira 초안

변경 파일: 현행 계획·개정 전 보관본·검토 회신 상태 안내·WORKLOG/CURRENT_STATE. 기존 미커밋 초안·동시 기록 보존. 링크/git diff --check 검사,문서 작업으로 Gradle/실제 DB/IAM 미실행. 예상 밖 제품 변경 없음.

Jira 댓글 초안(미등록): 사용자 승인 단순화 방향으로203 계획 개정. Identity snapshot/feed·source availability와 Billing local checkpoint/epoch로 분리. remote API3개·상태3종 제외 계획,T/H·digest/H2·I2 유지.202 cleanup Evidence 개정·Billing restore fencing·exact/index/fixture 동결이 구현 전제. 문서만 변경·정적 검사,코드/DB/Jira/배포 미변경.
