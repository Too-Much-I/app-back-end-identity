# Identity–Billing 공동 기술 계약 초안 검토

2026-10-07 / develop / 사용자 첨부 초안 §5~6 검토. 제품·외부 계약·운영 변경 없음. 검토 우선순위는 제안 계약의 보완 필요도이며 현재 운영 장애 발생을 의미하지 않는다.

## 1. 5줄 결론

1. 공통 control 쓰기를 통한 발급/탈퇴 commit 직렬화, 별도 journal, exact204, 고정 snapshot 방향은 타당하다.
2. snapshot 완료 시 H를 baseline checkpoint로 설정하는 계약이 빠져 있어 feed를 시작하거나 누락 없이 완료했다고 증명하기 어렵다(§2 R1).
3. feed API에 고정 목표 H2와 완결성 규칙이 없어 소비자가 서로 다른 CAUGHT_UP 판단을 할 수 있다(§2 R2).
4. 정상 원천 만료와 미적용 누락을 구분하지 않으면 보관기간 경과만으로 이미 검증한 coverage를 무효화할 수 있다(§2 R3).
5. control 초기화·일반 로그인 commit 불명 처리와 신규 상태 필드를 보완한 뒤 계약 동결을 권고한다. 구현 승인/개인정보 보존 승인으로 보지 않는다.

## 2. 사용자가 반드시 읽어야 하는 내용

### R1 [P1] snapshot → feed의 H 초기 checkpoint 승인 절차가 없음 — 초안 §5.3 B/D

초안은 snapshot H 이후 feed를 연속 적용하고 checkpoint를 뛰어넘지 않게 한다. 그러나 신규 Billing checkpoint는 아직0/미생성인데 H는0보다 클 수 있다. snapshots/checkpoints 요청은 throughSequence와 snapshotId만 있고, H를 snapshot baseline으로 인정하는 전이·선행 조건을 명시하지 않았다. 정상 baseline 설정을 불법 jump로 막거나, 반대로 snapshotId만 넣으면 임의 jump가 가능한 구현으로 갈릴 수 있다.

권고: Billing이 snapshot의 전 page/count/digest 적용과 로컬 baseline H를 같은 transaction으로 확정한다. Identity ACK는 인증된 consumer와 READY/유효 snapshot/stream/H를 검증해 최초 baseline ACK 또는 기존 위치에서의 명시적 재동기화 ACK로 구분한다. snapshotId만으로 consumer의 실제 적용을 Identity가 증명할 수는 없으므로 적용 증거/담당 책임도 명시한다. legacy PARTIAL/UNKNOWN은 baseline을 설정해도 그대로 유지한다. 이후 feed는 sequence>H다. 과거 eventId/digest 검증을 snapshot이 대체하지 않는다는 제한도 유지한다.

추가로 out-of-order ACK(N+1 도착 뒤 N 도착)는 서버 checkpoint를 낮추지 않아야 한다. 같거나 낮은 ACK의 성공/무시/명시409 중 무엇인지 정하고, 정상 지연 ACK를 coverage 장애로 오판하지 않게 한다.

### R2 [P1] 고정 H2를 feed 요청에 바인딩하지 않음 — 초안 §5.3 B/D

응답에는 highWatermark가 있지만 요청에는 streamId/afterSequence만 있다. 설명은 고정 H2까지 적용하라고 하므로 신규 탈퇴가 계속 발생하면 매 page highWatermark가 바뀌거나 H2를 넘어 처리할 수 있다. done/scannedThrough/nextAfterSequence가 어떤 경계를 의미하는지도 미확정이다.

권고: 첫 page에서 고정한 throughSequence=H2를 이후 요청에 포함하거나 서명된 opaque scan cursor에 바인딩한다. afterSequence는 exclusive, 반환은 오름차순, H2는 inclusive로 명시한다. done은 H2까지 연속 범위를 검증한 경우만 true. highWatermark의 live 관측치가 필요하면 target과 별도 필드로 구분한다. 빈 items인데 after<H2라면 정상 완료로 취급하지 말고 원천 floor/gap 상태를 판정한다. scan 도중 cleanup과 경합해도 누락을 건너뛰지 않는다.

### R3 [P1] 정상 retention floor와 실제 consumer coverage GAP이 혼재 — 초안 §5.2 B/§5.3 D

미ACK 만료에 GAP manifest를 남기는 방향은 맞다. 반면 ‘ledger seq 만료는 GAP’, ‘gap은 과거 전체 COMPLETE 무효화’는 어느 consumer·어느 증명 범위에 적용되는지 제한이 없다. 이미 ACK/적용한1~100 원천을 정상 정리해도, 현재100 이후를 읽는 Billing의 과거 적용 증거까지 자동 무효화하는 것으로 해석될 수 있다. 신규 consumer의 재생 불가와 기존 consumer의 적용 누락은 다르다.

권고: (a) 소스 replay availability floor, (b) consumer별 검증된 baseline/checkpoint, (c) 실제 미적용 범위 GAP을 분리한다. cleanup된 범위로 되감는 요청은 명시 재생 불가를 반환한다. 해당 범위를 이미 검증한 consumer의 coverage는 정상 source cleanup만으로 취소하지 않는다. checkpoint가 범위 이전이거나 적용 증거가 복원 과정에서 유실되면 GAP/UNKNOWN으로 전환한다. 만료 여부 판단 시 push delivery ACK뿐 아니라 feed/snapshot 적용 ACK가 어떤 완료 근거인지 명시한다. 특정 snapshot 적용은 원 event inbox/digest 수신과 동치가 아님을 유지한다.

### R4 [P1] control 누락 fail-closed 전환에 생성·이행 절차가 빠짐 — 초안 §5.1 A/§5.5

현행 [SessionSecurityService.control](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/SessionSecurityService.java)은 문서가 없으면 새 control 객체를 반환한다. 초안은 누락 시 거절하므로 신규 가입/기존 control 없는 MEMBER/legacy Guest 승격이 언제 준비되는지 없으면 정상 사용자가 token을 받지 못한다. 실제 누락 사용자 수는 미조회다.

권고: 신규 가입 transaction에서 control을 생성하고, 승격 시 타입/epoch를 동일 원자 경계에서 갱신한다. 기존 사용자 이행은 상태/버전 조건부로 수행해 탈퇴 writer와 경합시키며 WITHDRAWN을 ACTIVE로 만들지 않는다. 완료율·누락 진단·capture 및 fence 기능 flag의 필수 조합을 cutover gate로 명시한다. 기존 보호 작업 activeLogoutId 등 제어 상태를 새 schema가 잃지 않도록 한다.

### R5 [P2] 일반 발급의 unknown commit 조회 대상이 정의되지 않음 — 초안 §5.1 A/B

현행 [ReissueRecoveryService](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/ReissueRecoveryService.java)는 요청 식별자/암호화 결과와 replayOnly 경로가 있다. 하지만 [LoginService](../../src/main/java/web/tosunsaeng/identity/domain/auth/local/application/LoginService.java)의 일반 로그인은 동일 구조가 아니다. ‘기존 issuance/rotation operation을 조회’라는 문장만으로 모든 경로의 조회 식별자/권한/보존/동일 bytes 복구를 구현할 수 없다.

권고: 경로별로 (1) 기존 durable 결과로 복구, (2) commit 불명은503으로 종료하고 새 인증 요청으로 새 발급을 허용, (3) 새 issuance receipt가 필요한 경우를 구분한다. 2번은 결과 미반환 token/session이 남을 수 있음을 관리하고 원 요청이 rollback됐다고 주장하지 않는다. receipt 추가 시 raw JWT 보관을 기본 선택하지 말고 목적·암호화·기한을 합의한다. 보존기간 연장은 별도 승인이다. expired replay의 오류 code 및 프론트 재로그인/재발급 동작도 정한다.

## 3. 사용자가 결정해야 하는 사항

- 원천120일·snapshot7일은 여전히 신규 개인정보 보존 제안이다. 본 검토로 승인하지 않는다.
- 최대30분 TTL과 barrier 중단 영향도 기존 gate 유지. 현행 최대·과거 token 확인을 대체하지 않는다.
- R1~R5의 API/상태 전이는 개발 권장안으로 먼저 확정할 사항이다. 사용자에게 구현 세부를 모두 선택하도록 요구할 필요는 없다.
- 새로운 issuance receipt 보존이나 consumer별 coverage 증거의 개인정보 범위가 생기면 승인안에 포함한다.

## 4. 주요 위험과 미확인 사항

- Mongo 동일 atClusterTime snapshot 페이지와 global counter가 실제 배포 driver/readConcern/writeConcern 조건에서 성립하는지 미검증. replica-set failover·unknown commit·history expiry·restore까지 fixture 필요.
- journal 순서 보장은 모든 writer가 동일 counter를 같은 transaction으로 갱신하고, 성공 commit/read의 durability 및 snapshot visibility 조건이 맞을 때 성립한다. readConcern/writeConcern을 계약에 적고 counter commit 후 별도 journal insert 방식은 금지한다.
- 운영 snapshot5분 budget 및 page20으로 충분한지는 사용자 수·데이터 크기 측정 필요. 실패 시 재시작하는 안전한 정책 자체는 적절하다.
- stream의 단순 숫자 H는 백업 복원 뒤 재사용될 수 있다. 복원 시 기존 streamId의 sequence rewind 금지, incarnation/새 stream 및 복원 coverage 절차를 정한다.
- Billing 코드/원격 인프라·실제 보존 정책은 이번 검토에서도 확인하지 않았다. 이전 인계 검토의 배포 미확인 상태 유지.

## 5. 현재 작업과 직접 관련된 추가 명세 보완

1. **commit 기준120일과 capturedAt:** transaction 안의 wall clock은 실제 commit timestamp가 아니다. 120일 기산을 관측 시각으로 할지 commit 이후 확정 시각으로 할지 정한다. 전자를 쓰면 허용 commit 지연과 보존 오차를 명시한다. withdrawnAt은 기존 원 탈퇴시각을 유지한다.
2. **지연값·시도 횟수 schema:** delivery 목록에 없는 resumeGeneration/lifetimeAttemptCount/generationAttemptCount/notBefore 및 leaseVersion 검증을 추가한다. 원천 expiry와 긴 Retry-After의 충돌은 보존 우선/GAP로 처리하고 자동 연장하지 않는다.
3. **HTTP 전 범위:** 413·기타4xx·잘못된 status 처리, duplicate Retry-After, HTTP-date 시계 기준·overflow·0초 의미를 정한다. unknown outcome도 실제 전송 시도 budget에 포함하고 lease claim 횟수와 구별한다.
4. **snapshot 무결성:** item canonical bytes의 UTF-8/필드 순서/UUID/Instant 정밀도·length prefix byte order·빈 목록 digest를 고정한다. page 경계·순서·count/digest 검증을 일관되게 정의한다. data manifest 불변과 실행 상태/만료·ACK metadata 갱신은 분리한다.
5. **snapshot request 멱등:** 동일 키의 바인딩(환경·consumer·요청 범위), FAILED/EXPIRED 재호출 응답과 새 run 요청 규칙, operation record 보존기간을 정한다. 민감 rows7일 삭제와 operation 메타데이터 보존을 구분한다.

## 6. 부록 — 유지할 설계·검증 범위

### 수용 가능한 방향

| 항목 | 판단 |
| --- | --- |
| 발급 기준 | 서명/commit/응답 구분 및 동일 control 쓰기 방향 타당. 늦은 응답에 원 exp 유지 |
| Guest 권한 | default/explicit에서 purchase 제거 후 trusted ACTIVE MEMBER만 합성 타당 |
| journal | LC 원천 정리와 분리, publisher OFF에도 capture 유지 타당 |
| snapshot | User와H의 동일T, 늦은 commit은>H, READY 전 부분 데이터 미공개 타당 |
| coverage | SCANNED와 legacy COMPLETE 구분, 증거 없는 과거 범위 UNKNOWN/PARTIAL 유지 타당 |
| 전달 | Billing204-only, 인증 차단/수동재개·원 ID 유지, LC 무변경 타당 |
| 개인정보 | 승인 없는 자동 보존 연장 금지, 임시 snapshot 정리 타당 |

### 추가 테스트

- 최초 snapshot H>0의 baseline 전이, 기존 checkpoint에서 재동기화, 위조/다른 consumer snapshotId, 지연 ACK.
- feed 도중 H2 이후 신규 event 생성, 빈 중간 page·cleanup 경합, 정상 ACK 범위 원천 삭제와 실제 누락 구분.
- 신규 가입/control 없는 legacy 사용자 이행/동시 탈퇴, control 타입 불일치, fence OFF 혼합 배포 거절.
- 일반 로그인 commit 불명·응답 유실, 재발급 receipt replay, snapshot 요청 expiry/retry.
- majority commit/읽기 조건, counter rollback/failover/backup restore 시 stream identity.

검증 방식: 사용자 첨부 전 문서와 기존 검토서 대조, SessionSecurityService/RefreshSessionIssuer/ReissueRecoveryService 및 앞선 issuer 경로 조사 근거 확인. 정적 검토만 수행했으며 신규 알고리즘 실행 테스트·Gradle·외부 호출은 수행하지 않았다. 제품 파일 변경 없음. 기존 검토서: [구매·탈퇴 인계 검토](billing-purchase-withdrawal-handoff-review-2026-10-07.md).

## 7. 2026-10-07 개정본 재검토 — 위 R1~R5 판정 갱신

### 7.1 5줄 결론

1. 사용자 후속 첨부 개정본은 R1~R5에 대한 설계 대응을 제시했다. 앞선 누락 지적을 그대로 미해결 목록으로 반복하지 않는다.
2. BASELINE/RESYNC·고정 scanCursor·consumer 증거 분리·control 이행·발급 경로별503 정책은 문서 수준에서 수용 가능하다.
3. snapshot ACK의 manifestDigest는 요청 필드로 추가됐으나 산출/응답 정의가 없어 DTO 동결 전에 보완해야 한다.
4. source incarnation 복원 규칙에 더해 Billing consumer 복원/RESYNC 전후 오래된 worker/ACK를 차단하는 규칙을 명시해야 한다.
5. 위 보완과 신규 보존·TTL·중단 영향 승인 후 구현 범위를 나눌 수 있다. 실제 Mongo/배포·안전성 검증 완료나 구현 승인으로 보지 않는다.

### 7.2 사용자가 반드시 읽어야 하는 내용

| 이전 지적 | 개정 반영 | 문서 판정 |
| --- | --- | --- |
| R1 snapshot H baseline | BASELINE/RESYNC·manifest/H 검증·checkpoint status·동일/낮은 FEED ACK | 핵심 전이 반영. digest 정의는 아래 보완 |
| R2 고정 H2 | scanCursor에 고정 H2/start 바인딩, 빈 중간 page 오류, targetThroughSequence | 반영 |
| R3 만료와 GAP 혼재 | replayFloor/interval·state baseline·feed checkpoint·consumer GAP 분리 | 반영 |
| R4 control 이행 | 신규 User와 생성, 기존 조건부 이행, terminal 처리 및 flag gate | 반영. readiness 표현은 아래 명확화 |
| R5 unknown commit | 일반 발급503/새 인증, reissue 기존 결과 복구, 만료 replay 오류 제안 | 반영. 외부 오류는 프론트 합의 전 미확정 |

**[P2] manifestDigest 정의 누락 — 개정 §5.3 B/C/D.** ACK는 manifestDigest를 받지만 불변 manifest 필드 목록에는 contentDigest만 있고 canonical 산출 규칙도 item stream의 contentDigest만 정의돼 있다. 둘을 같은 것으로 볼지 별도 digest로 볼지 구현자가 추측해야 한다. contentDigest는 rows만 해시하므로 stream/H/expiry 등 metadata 바인딩까지 의미한다고 간주하면 안 된다. 권고는 manifestDigest를 status/manifest 응답에 명시하고 고정 필드·버전·직렬화·T 표현·nullable/배열 정렬·digest 자체 제외 규칙 및 golden fixture를 동결하는 것이다. 또는 별도 digest가 불필요하면 ACK를 contentDigest로 명명하고 Identity 저장 manifest의 소유/stream/H 검증에 의존한다고 명확히 쓴다. 어느 선택도 원격 Billing 적용을 암호학적으로 증명하는 수단은 아니다.

**[P1] consumer 복원 세대의 오래된 ACK 차단 — 개정 §5.3 D/§5.6.** source backup restore에는 새 streamId가 있으나 Billing만 복원하거나 같은 stream에서 RESYNC하면 authenticatedConsumer/stream은 그대로다. 기존 scanCursor에는 consumer recovery generation/coverage version이 없고 FEED ACK도 RESYNC version을 확인하지 않는다. 이전 프로세스/큐가 살아 있거나 지연 ACK가 새 복구 상태 이후 도착하면, 복원된 Billing DB에 없는 예전 적용 결과를 현재 checkpoint 증거로 다시 올릴 수 있다. 모든 worker/큐/진행 ACK가 완전히 drain됐음을 보장하는 규칙도 아직 명시돼 있지 않다.

권고: 복원/재동기화 시작 시 발급하는 consumerRecoveryGeneration(일반 ACK마다 바뀌는 checkpoint version과 구분)을 snapshot/scanCursor/ACK에 바인딩하고 현재 generation과 다른 ACK는 거절한다. 진행 중 과거 worker의 로컬 적용/증거 갱신도 generation CAS로 차단한다. 새 복구의 검증 marker가 확정되기 전 과거 ACK만으로 UNKNOWN을 해제하지 않는다. 대안으로 전체 worker·큐·진행 요청 drain과 옛 cursor 만료까지의 barrier를 계약/운영 검증으로 보장할 수 있지만 사용자 트래픽 차단만으로 충분하지 않다. 테스트는 Billing-only restore, 구 ACK 지연 도착, RESYNC 경합, source stream은 같은 경우를 포함한다.

### 7.3 사용자 결정 및 남은 위험

- 원천120일, snapshot7일, operation/coverage metadata 보존,30분 상한·clock/commit budget·barrier는 제안 승인 상태 유지. 임의 승인/실행하지 않는다.
- 문서 전체 구조 재작성은 불필요하다. 마지막 프로토콜 보완 뒤 Mongo snapshot/majority·control migration feasibility와 fixture 검증 단계로 이동할 수 있다.
- ‘불가능한 flag 조합은 startup/readiness 실패이며 purchase만 fail-closed’는 영향 범위가 다르다. Identity 전체 readiness 실패는 기존 로그인도 막는다. 잘못된 배포 조합의 전체 startup 실패와 실행 중 purchase 한정 거절을 구분하고 기존 서비스 유지/rollback 방식을 명시한다.
- 5줄 결론2의 ‘capture commit 기준120일’은 개정 §3/§5.2의 capturedAt 관측시각 기준과 불일치하므로 요약을 동기화한다.
- ‘SCANNED/LEGACY_PARTIAL’ 설명 용어와 최종 enum PARTIAL/UNKNOWN의 대응을 DTO 동결 시 정리한다. 추가 기능 요구가 아니라 이름/판정 혼동 방지다.

### 7.4 검증 범위

후속 첨부 전 문서(출력 잘린 구간은 별도 읽음)와 이전 검토 R1~R5 대조. 이번에는 코드 변경/재실행 테스트/외부 환경 조회 없이 문서 프로토콜만 검토했다. manifest digest·consumer generation은 신규 제안이며 현재 존재하는 구현이라고 주장하지 않는다.

## 8. 2026-10-07 사용자 합의 기준 채택

- 사용자가 후속 최종 첨부에 대해 “이렇게 하기로 했어”라고 명시했다. 해당 버전을 이후 설계·구현 계획의 기준으로 채택한다. 첨부 헤더의 초안 표기와 별개로 기술 방향 채택 의사는 확인했으며, 본문이 별도 승인으로 남긴 보존·운영 작업까지 승인됐다고 확대 해석하지 않는다.
- 기준 첨부: `/Users/msde76/.codex/attachments/96261d9f-c115-41e9-b8ae-14ae81d7ee6a/붙여넣은 텍스트.txt`. 문서 식별 SHA-256: `7de3e2a8def9ee60490bab160229ae732f1e76fcb6a8b6adfcee2d87426f9773` (인증 키가 아닌 문서 체크섬).
- §7 잔여 지적의 문서 반영 확인: ACK는 contentDigest로 통일 및 metadata 별도 검증. consumerRecoveryGeneration/운영 복구 시작·local/remote CAS·old worker/ACK 거절·복원 barrier 추가. startup 실패와 runtime purchase 전용 gate 분리, coverage enum 구분 반영.
- 이전 지적은 설계 문서상 대응 완료로 갱신한다. 실행 안전성 검증·구현 완료를 뜻하지 않는다. UserWithdrawn 4필드/LC 기존 동작/기존 read·audience 유지 기준도 계속 적용한다.
- 본문에 남은 별도 승인: 원천120일·snapshot7일·복구 metadata 보존, TTL상한·clock/commit budget 검증 및 운영 이관/중단 영향. 실제 판매·자동 purge·IAM/배포는 별도 gate 유지.
- 다음은 이 기준으로 Identity 작업을 발급 fence/control 이행, 탈퇴 journal/delivery, snapshot/feed/recovery generation, 검증/이관 단계로 나눈 구현 계획이다. 이번 턴에는 구현·Jira·배포를 시작하지 않았다.
