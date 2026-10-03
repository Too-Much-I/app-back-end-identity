# Guest 병합 후 전체 이전 완료 조회 API 구현 계획

- 작성일: 2026-10-03. 조사 브랜치: `feat/TMI-192-single-sns-account-recovery`.
- 상태: **2026-10-03 Identity 저장소 구현 반영. 실제 배포·LC 종단 검증은 미수행. 확정 구현 계약은 [API·운영 문서](guest-merge-progress-api.md)를 따른다.** 아래 조사 당시 현재 구현/제안 표는 계획 수립 근거로 보존한다.
- 작업 구분: TMI-192 단일 SNS/계정 찾기와 별도 후속 범위. 새 Jira 이슈는 아직 생성하지 않았다.
- 대상: Identity 병합 이후 `UserMerged`에 따른 Learning Core 기록 이전과 Billing owner 처리 완료 확인. Guest 승격, 계정 찾기, 정지 해제는 대상이 아니다.
- 사용자 확정 반영: 첫 출시는 Billing 미배포이므로 Learning Core만 필수. Billing 도입 시 과거 병합 재실행/backfill 없음. 화면·polling 구현은 프론트 책임이며 Identity 구현을 위한 추가 사용자 결정사항으로 두지 않는다.

## 1. 5줄 결론

1. 프론트는 **Identity API에서 이번 병합에 필요한 서비스의 완료 여부**를 확인한다. 첫 출시는 Learning Core만 필수이며 Billing은 `NOT_REQUIRED`다.
2. 기존 consumer 계약의 **실제 DB commit 후 성공 응답**을 완료 증거로 활용한다. 새 완료 콜백은 우선 추가하지 않는다. [기존 LC 계약](learning-core-user-merged-consumer-handoff.md#42-v1-처리-모델과-응답)
3. 현재 publisher는 모든 2xx를 성공 처리하므로, 추적 대상 UserMerged는 합의된 **204 완료 ACK만** 인정하도록 강화한다. 202 접수는 완료가 아니다. [현재 publisher](../../src/main/java/web/tosunsaeng/identity/domain/auth/ownerevent/application/OwnerEventPublisher.java)
4. 병합 응답에 `mergeId`를 추가하고, 회원 인증으로 단건 조회와 내 작업 목록 조회를 제공한다. 응답 유실/앱 재시작도 목록으로 복구한다.
5. 해당 출시에서 필수인 consumer의 실제 완료 계약·통합 테스트가 선행돼야 한다. 화면 처리·polling은 프론트 인계 사항이다. Billing 도입 이후에도 과거 병합을 다시 실행하지 않는다.

## 2. 사용자가 반드시 읽어야 하는 내용

### 쉽게 말하면: Identity가 병합 작업의 확인표를 보관한다

현재 merge 성공은 “회원 계정으로 전환했고, 다른 서버에 보낼 일을 저장했다”는 뜻이다. 새 조회 기능은 여기에 다음 확인표를 붙인다.

| 확인 항목 | 누가 일을 수행하는가 | Identity가 완료를 확인하는 시점 |
| --- | --- | --- |
| 계정 병합 | Identity | merge 트랜잭션 commit |
| 학습 기록 이전 | Learning Core | 기록 이전·접근 차단·inbox 처리를 DB에 commit한 뒤 보낸 204를 기록 |
| Billing 처리 | Billing | 첫 출시에는 NOT_REQUIRED. Billing 도입 후 생성된 대상 작업에만 commit 뒤 204 확인 |

Identity가 학습 기록이나 결제 데이터를 직접 옮기는 것은 아니다. **각 서버가 확정한 결과를 모아 알려주는 역할**만 추가한다.

**생성 당시 필수로 저장한 칸만** 모두 확인되면 `COMPLETED`, 그중 미확인이 있으면 `PROCESSING`, 자동 진행이 막혀 운영 확인이 필요하면 `ACTION_REQUIRED`다. 첫 출시는 LC 확인만으로 완료되고 Billing은 완료가 아니라 대상 제외(NOT_REQUIRED)로 표시한다. PROCESSING은 “서버가 지금 실행 중임”이 아니라 “필수 작업 전체 완료가 아직 확인되지 않음”이다.

### 완료 콜백 없이도 가능한 이유

Identity가 이벤트를 보내는 작업 자체는 사용자 HTTP 요청과 분리된 비동기 publisher다. 하지만 기존 Learning Core consumer 계약은 이벤트 HTTP 요청을 받으면 **그 요청 안에서 로컬 이전 트랜잭션을 끝내고 204를 반환**하도록 정해져 있다. Billing 계약도 commit 후 성공 응답을 요구한다.

따라서 publisher가 그 완료 응답을 받았다는 사실을 저장하면 된다. 새 `consumer → Identity callback`이나 프론트의 서비스별 직접 조회를 추가할 필요가 없다. 첫 출시는 LC 계약 준수를 확인하며 Billing 미배포를 출시 장애로 취급하지 않는다. Billing을 도입할 때는 정확한 성공 코드와 실제 구현을 확인해 그 이후 생성되는 대상 작업의 완료 ACK도 **204**로 맞춘다.

**한계:** 하위 서비스가 commit했어도 HTTP 응답 또는 Identity 저장이 유실되면 잠시 PROCESSING으로 보인다. 같은 eventId를 재전송하면 consumer가 이미 처리한 동일 이벤트임을 확인하고 다시 204를 보내며 상태가 수렴한다. 모르는 상태를 완료라고 오인하는 것보다 보수적으로 늦게 확인하는 방식이다.

### 앱은 어떻게 움직이나

아래는 **프론트 구현 담당자에게 전달할 권장 흐름**이다. Identity 작업 요청자가 화면 구현이나 UI 선택을 별도로 수행해야 한다는 뜻이 아니다. Identity는 상태·권한·재시도 힌트를 제공하고 서버의 데이터/권한 안전성을 보장한다.

1. merge 성공 시 응답의 회원 Access/Refresh Token으로 교체하고 `mergeId`를 보관한다.
2. 새 회원 토큰으로 단건 조회한다. 초기 로딩은 짧게 표시하고 기본 회원 화면 진입은 허용한다.
3. Learning Core 완료 전에는 “이전 기록 반영 중”을 표시한다. 빈 목록을 데이터 유실/전체 이전 완료로 해석하지 않는다.
4. Learning Core가 완료되면 학습 기록 API를 다시 조회한다. 첫 출시에는 이것으로 전체 병합 이전 완료다.
5. Billing 도입 후 해당 작업에 Billing이 필수인 경우에만 완료 시 권리/이용권 조회를 갱신한다. NOT_REQUIRED인 경우 Billing을 기다리거나 호출하지 않는다.
6. 필수 서비스가 모두 완료되면 전체 이전 완료 안내를 표시하고 polling을 종료한다.

학습 기록/이용권 조회 자체가 별도의 권한·정지·탈퇴 오류를 반환하면 그 응답을 따른다. 이 상태 API가 서비스 권한 검사를 대신하지 않는다. 기존 기록 열람과 신규 시험 생성 등 권리 관련 쓰기는 구분한다. 진행 중 쓰기 안전성은 consumer ownership guard 및 해당 출시의 서버 권한 검증으로 보장하며 UI 잠금만으로 해결하지 않는다. Billing NOT_REQUIRED는 무료 이용권 발급이나 권한 검증 우회를 의미하지 않는다. Billing에 의존하는 기능의 출시는 별도 기능 gate를 따른다.

Guest **승격**은 기존 userId를 유지하므로 이 Guest **병합** 작업을 생성하지 않는다. 승격 후 별도 Billing 반영이 필요해도 UserMerged 조회에 섞지 않는다.

## 3. 사용자가 결정해야 하는 사항

현재 대화에서 필요한 제품 방향은 정리됐으며 **Identity 계획 진행을 막는 추가 사용자 결정은 없다.** 실제 consumer 검증과 배포 준비는 개발·운영 작업이고, 프론트 화면 처리도 별도 프론트 구현 작업이다.

확정된 방향:

- 첫 출시 Billing 미배포: Learning Core만 필수, Billing NOT_REQUIRED.
- 향후 Billing 도입: 신규 병합부터 두 서비스 필수. 첫 출시의 병합을 다시 실행하거나 Billing에 backfill하지 않는다. 과거 완료 상태도 바꾸지 않는다.
- 화면 구성·로딩·polling 구현은 프론트 담당. Identity가 UI를 직접 구현해야 하거나 사용자가 UI 방식을 추가 결정해야 하는 조건으로 두지 않는다.

기술 권장 기본안:

- 상태 조회의 소유 서비스는 Identity. consumer 작업 완료 확인은 기존 event HTTP 응답 활용.
- 완료 ACK는 해당 작업의 필수 consumer에서 204. Billing 도입 시 다른 코드/접수 후 worker 구조라면 먼저 서버 간 계약을 재합의한다. 첫 출시의 선행 조건은 LC만 해당한다.
- 조회 기록은 완료 후 30일 보관. 미완료/지원 필요 건은 자동 삭제하지 않고 90일 시점에 운영 검토한다. 탈퇴 시에는 기존 개인정보 정리 정책과 연계한다.
- 정상 polling은 5초, 1분 이후 15초, foreground 연속 2분 이후 자동 polling 중지. 화면 재진입/앱 foreground 복귀/사용자 새로고침 때 재조회한다. 사용자 대기시간은 처리 성공/실패 판정 기준이 아니다.
- 별도 Jira 이슈로 관리한다. TMI-192 작업 완료 조건을 이 기능으로 소급 변경하지 않는다.

새 consumer 작업 큐/worker로의 전환은 범위 밖이다. 과거 병합의 Billing 재전송은 사용자 결정에 따라 수행하지 않는다. 단, 이미 Billing이 필수로 생성된 미완료 작업의 동일 event 재시도는 복구이므로 금지 대상이 아니다.

## 4. 주요 위험과 미확인 사항

1. **문서상 계약 ≠ 실제 서비스 검증:** 이번에는 Identity 저장소만 확인했다. LC/Billing의 consumer가 전체 대상 데이터를 commit한 뒤 204를 반환하는지 확인해야 한다. 특히 외부 결제 처리·비동기 검색 인덱스/통계 반영까지 완료에 포함하는지 owner inventory로 명시한다. 이번 완료는 UserMerged에 합의된 이전 범위이지 모든 외부 시스템의 영구 동기화를 보장하는 것이 아니다.
2. **202 오인 위험:** 현재 publisher의 모든 2xx 성공 판정과 과거 PUBLISHED를 그대로 완료 근거로 쓰면 안 된다. 새 엄격 ACK 기록으로만 완료를 표시한다.
3. **시간 제한:** 기존 owner publisher read timeout은 기본 3초다. 이전 규모가 이를 안정적으로 넘으면 consumer의 durable inbox + worker + 별도 완료 전달 계약이 필요하다. 202를 완료로 처리하거나 timeout만 무작정 늘리지 않는다.
4. **FIFO 지연:** consumer별 앞선 다른 이벤트의 DEAD_LETTER/PAUSED 때문에 내 작업도 막힐 수 있다. 자신의 delivery만 보고 “진행 중”으로 영구 표시하지 않도록 consumer cursor/head 상태도 확인한다.
5. **보관기간:** 기존 delivery/core는 TTL로 제거된다. 그 문서만 직접 노출하면 완료 기록을 잃거나 일부 삭제를 전체 완료로 오판할 수 있어 별도 최소 조회 요약을 둔다.
6. **응답 유실/복수 기기:** 하나의 회원에 여러 Guest가 병합될 수 있다. “최근 1건”만 조회하면 다른 기기 작업을 잘못 표시하므로 event별 ID와 목록을 제공한다.
7. **이미 저장된 이벤트:** 과거 PUBLISHED에는 strict 204 증거가 없다. 자동 완료 backfill 및 과거 Guest merge의 Billing 자동 재전송은 하지 않는다.
8. **탈퇴와 동시 실행:** 완료 조회를 위해 MERGED Guest 토큰을 허용하거나 탈퇴 회원의 데이터를 계속 노출하지 않는다. 기존 UserMerged/UserWithdrawn 순서·source write guard·tombstone/삭제 책임을 관련 서비스와 통합 검증한다.
9. **출시 대상과 장애 구분:** Billing 제외는 생성 시 설정한 명시적 출시 profile로 결정한다. 현재 publisher OFF/장애를 보고 기존 작업의 Billing을 빼면 안 된다. 기존 OwnerEventCore는 두 consumer를 고정하고 있으므로 이를 그대로 둔 채 조회에서만 Billing을 무시하는 구현은 금지한다.

## 5. 현재 작업과 직접 관련된 구현 설계

### 5.1 변경할 구성 요소

| 구성 요소 | 역할/변경 |
| --- | --- |
| `FirebaseGuestMergeService` / TransactionService | 실제 eventId와 연결된 조회 요약을 병합 트랜잭션 안에 저장하고 그 ID 반환 |
| 신규 `FirebaseGuestMergeResponse` | 기존 토큰 필드 유지 + `mergeId` 추가. signup/upgrade 공용 DTO는 변경하지 않음 |
| 신규 `UserMergeProgress` / repository | target 소유권·필수 consumer 목록·consumer별 완료 확인·보관 시각 저장 |
| `OwnerEventCore` / CaptureService / 생성 gate | 명시적 출시 profile에 따른 필수 consumer snapshot·실제 delivery 생성·sequence 할당 일치. LC-only에서는 Billing delivery/sequence를 생성하지 않음 |
| `OwnerEventPublisher` | 추적 대상 USER_MERGED만 strict 204 판정. 예상하지 못한 2xx는 완료로 기록하지 않음 |
| `OwnerEventPublishTransactionService` | delivery PUBLISHED/cursor 전진과 progress 완료 표시를 동일 Mongo 트랜잭션으로 처리 |
| 신규 `UserMergeQueryService` / Controller | 본인 작업 단건/목록 read-only 조회, 상태 합산, polling 힌트 |
| LC/Billing consumer (각 저장소) | 첫 출시 LC의 commit/멱등 204 확인. Billing은 추후 도입 시 별도 검증 |
| 프론트/테스트 도구/문서 | 서버 계약·로컬 테스트 도구 제공, 실제 앱의 mergeId 저장/화면/polling은 프론트 저장소에서 구현 |

서버에 학습 기록/과금 업무 코드를 복사하지 않는다. 상태 조회도 GET마다 LC/Billing HTTP 호출을 하지 않고 Identity에 저장한 확인 결과를 읽는다.

### 5.2 ID와 트랜잭션 경계

**현재 코드에서 주의할 점:** GuestMergeService가 legacy `UserMergedOutbox` ID를 만들지만 fanout ON이면 `OwnerEventCaptureService`가 다른 `OwnerEventCore.eventId`를 생성한다. 따라서 현재 outbox 객체의 ID를 그대로 클라이언트에 반환하면 실제 발행 이벤트와 다른 ID가 될 수 있다.

신규 추적 경로에서는:

1. 기존 source Guest 전이·세션 폐기·target 세션 저장을 그대로 수행한다.
2. `captureUserMerged(...)`가 반환하는 **실제 OwnerEventCore.eventId**를 얻는다.
3. `mergeId = 그 eventId`로 progress를 저장한다. requiredConsumers는 서버가 선택한 출시 profile의 불변 snapshot이며 OwnerEventCore·실제 delivery 집합과 정확히 같아야 한다. 첫 출시는 `{LEARNING_CORE}`, Billing 도입 후 신규 작업은 `{LEARNING_CORE, BILLING}`이다.
4. 해당 트랜잭션 결과를 `issuedRefreshSession + mergeId` 내부 결과 객체로 반환한다. Controller 응답은 commit 뒤 생성한다.
5. progress 저장 실패도 병합/세션/event/delivery 저장과 함께 rollback한다.

legacy 단일 LC publisher로는 신규 progress 계약을 충족했다고 추정하지 않는다. 신규 추적은 수정된 owner fanout capture를 사용하며 **해당 profile의 필수 consumer만** 엄격 ACK/기동 gate를 적용한다. 첫 출시에는 Billing 주소·credential·publisher가 없어도 LC-only 병합을 허용한다. 필수 LC가 준비되지 않았다면 **병합 상태 변경 전** fail-closed한다.

출시 profile 제안: `LEARNING_CORE_ONLY`, `LEARNING_CORE_AND_BILLING`. 추적 ON 배포에서는 운영자가 profile을 명시해야 하며 누락/미지원 값은 fail-closed한다. 클라이언트는 이 값을 지정할 수 없다. profile은 신규 event 생성 시 한 번 읽고 core/progress/requiredConsumers에 함께 저장한다. 조회·재전송은 현재 설정이 아니라 저장된 snapshot을 따른다. 기존 core의 두 consumer 고정 불변식은 이 두 형태만 허용하도록 변경하며 빈 집합/Billing-only는 금지한다. 외부 UserMerged v1 JSON 필드는 유지한다. 기존 Stage 7의 필수 두 consumer 규칙은 이 후속 변경으로 보완할 대상이며, 구현 시 관련 계약/테스트도 함께 갱신한다.

### 5.3 내부 저장 모델

새 컬렉션 제안: `user_merge_progress`.

| 필드 | 의미 |
| --- | --- |
| `_id` / mergeId | 실제 USER_MERGED eventId, canonical UUID |
| targetUserId | 조회 권한 소유자. 클라이언트에서 받지 않고 병합 결과에서 저장 |
| sourceUserId | 내부 원천 Guest 식별·무결성/운영 추적 전용, 외부 응답에는 제외 |
| createdAt | Identity 병합 시각 |
| completionProfile / requiredConsumers | 첫 출시 LEARNING_CORE_ONLY에 대응하는 `{LEARNING_CORE}` 또는 향후 LEARNING_CORE_AND_BILLING의 양쪽 필수 집합. 생성 후 불변 |
| learningCoreConfirmedAt / billingConfirmedAt | 유효한 commit ACK를 Identity DB에 기록한 시각, 미확인 null |
| completedAt | 저장된 필수 consumer 전부가 확인된 시각; consumer DB의 정확한 commit 시간이라는 의미는 아님 |
| progressContractVersion | 1; strict 204 증거를 사용하는 조회 계약 버전 |
| updatedAt / version | 동시 갱신 CAS 및 stale write 방지 |
| retentionReviewAt / cleanupAt | 미완료 운영 점검/완료 TTL 정리 시각 |

인덱스: `_id` unique, `(targetUserId, createdAt desc, _id desc)` 목록 조회, `(targetUserId, completedAt, createdAt desc, _id desc)` 미완료 조회, cleanupAt TTL 0초, retentionReviewAt 운영 점검 인덱스. 만료 판정은 서버 시각으로 하며 Mongo TTL 삭제 지연에 의존하지 않는다.

한 consumer의 ACK가 다른 consumer의 확인을 덮어쓰지 않도록 해당 필드 CAS와 필수 집합의 완료 조건을 트랜잭션에서 처리한다. LC-only는 LC ACK로 완료, 양쪽 필수는 동시 ACK도 모두 보존하고 completedAt을 한 번만 확정한다. 한 번 확인된 consumer는 PENDING으로 되돌리지 않는다. NOT_REQUIRED인 consumer의 confirmedAt은 null이며 ACK를 인위적으로 생성하지 않는다.

보관: 완료 시 cleanupAt=completedAt+30일. 미완료는 cleanupAt=null, createdAt+90일 운영 검토(자동 폐기 아님). target 탈퇴 시 공개 조회는 즉시 차단하고 개인정보 삭제/진행 중 event와의 참조 보존 정책에 따라 최소 운영 데이터 정리 작업에 포함한다. 기록이 없거나 보관 만료면 완료라고 추정하지 않고 동일 404를 반환한다.

### 5.4 완료 증거와 실패 처리

- tracked USER_MERGED: 인증된 고정 consumer endpoint에서 받은 **204**만 완료 ACK다. 단순히 소켓 전송 성공·HTTP 202·inbox 접수·일정 시간 경과로 확인하지 않는다.
- consumer의 “옮길 데이터 없음”도 실제 검사와 멱등 inbox commit 뒤 204이면 정상 완료다. 서비스 장애/미구현을 “대상 없음”으로 대체하지 않는다.
- 기존 허용 retry 상태 408/425/429/5xx, timeout, connection 실패는 같은 eventId/payload로 재시도한다. HTTP 응답 유실 후 duplicate도 consumer의 commit된 inbox 확인 뒤 204다.
- tracked USER_MERGED의 200/201/202 등 다른 2xx는 계약 위반으로 consumer circuit를 PAUSED, delivery 완료/cursor 전진/progress 확인 금지. 사용자 상태는 ACTION_REQUIRED. 운영 확인 후 동일 이벤트 replay하며 새 ID를 만들지 않는다.
- 400/409/413/415/422는 기존 DEAD_LETTER, 401/403/404/405/3xx는 기존 circuit pause를 유지한다. 재시도 최대 12회·기존 backoff(5초~15분)·Retry-After 처리도 유지한다.
- delivery 업데이트/cursor/progress 갱신이 하나라도 실패하면 local 트랜잭션 전체를 rollback한다. consumer commit이 이미 끝났어도 재전송으로 수렴한다. lease를 잃은 worker는 progress를 완료 처리하지 않는다.
- `TRIAL_OWNER_REBIND_APPROVED`의 기존 성공 코드/권리 의미는 이 작업에서 바꾸지 않는다. 같은 publisher를 공유하더라도 event type 및 추적 계약으로 분기한다.
- publisher 중단, 해당 consumer의 circuit pause, 내 delivery의 DEAD_LETTER, 앞선 FIFO head의 DEAD_LETTER/필수 채널 비활성은 **필수 집합에 속하는 미완료 consumer**의 ACTION_REQUIRED 사유다. LC-only 작업은 Billing 장애/미설정/cursor 때문에 차단되지 않는다. 자동 재시도가 예약된 오류와 정상 선행 이벤트 대기는 PENDING이다.
- 조회 시 requiredConsumers/confirmation 필드가 손상됐거나 진행에 필요한 delivery/state가 사라졌으면 `503 MERGE_STATUS_UNAVAILABLE`로 닫고 경보한다. 빈 delivery 목록을 “모두 완료”로 해석하지 않는다.

### 5.5 외부 API 제안

#### A. 기존 merge 성공 응답에 mergeId 추가

`POST /api/v1/auth/firebase/guest/merge`의 기존 Access/Refresh Token·grantType·만료 필드는 그대로 유지한다. 신규 merge 전용 DTO에 `mergeId`(UUID)를 추가한다. signup/upgrade 응답에는 추가하지 않는다.

새 클라이언트 rollout에서는 추적 feature ON을 필수로 한다. 호환 기간의 legacy/feature OFF 응답에서 mergeId가 없으면 **추적 미지원**이며 전체 완료로 해석하지 않는다. 한 번 생성된 progress 조회는 capture flag를 꺼도 계속 지원한다.

#### B. 단건 조회

`GET /api/v1/users/me/merges/{mergeId}`

- target MEMBER의 Identity Bearer 인증 + 현재 User가 ACTIVE MEMBER인지 서버 조회.
- `JWT.sub == progress.targetUserId` 검증. URL ID를 안다는 사실은 권한이 아니다.
- 임의 userId/sourceUserId/targetUserId를 query/body로 받지 않는다. MERGED Guest의 옛 토큰은 허용하지 않는다.
- 모든 응답 no-store. 내부 예외·시도 횟수·다른 사용자 ID·Billing 내부 subject·provider 정보는 응답하지 않는다.
- 성공은 처리 중이어도 HTTP 200 + 기존 BaseResponse. 다음은 **첫 출시의 LC 이전 완료** result 예시다.

```json
{
  "mergeId": "11111111-1111-4111-8111-111111111111",
  "status": "COMPLETED",
  "createdAt": "2026-10-03T01:00:00Z",
  "completedAt": "2026-10-03T01:00:03Z",
  "learningCore": { "status": "COMPLETED", "confirmedAt": "2026-10-03T01:00:03Z" },
  "billing": { "status": "NOT_REQUIRED", "confirmedAt": null },
  "nextPollAfterSeconds": null
}
```

상태 합산 우선순위:

| 조건 | 전체 status | nextPollAfterSeconds |
| --- | --- | --- |
| 생성 당시 필수 집합 모두 확인 | COMPLETED | null |
| 미완료 중 하나라도 운영 확인 필요 | ACTION_REQUIRED | null |
| 나머지 정상 대기/자동 retry | PROCESSING | 생성 후 1분 이내 5, 이후 15 |

consumer 상태는 PENDING / COMPLETED / ACTION_REQUIRED / NOT_REQUIRED. NOT_REQUIRED는 생성 시 필수 집합에 없었던 경우에만 사용하며 서비스 장애나 현재 flag OFF를 이 상태로 바꾸지 않는다. 전체 status에는 NOT_REQUIRED를 사용하지 않는다. ACTION_REQUIRED는 병합 취소나 실패 rollback을 뜻하지 않는다. 회원 로그인은 유지하고 서버 작업 재개 뒤 PROCESSING/COMPLETED로 바뀔 수 있다. 완료된 칸은 유지된다.

첫 출시에서 아직 LC 미확인이면 전체 PROCESSING, learningCore=PENDING, billing=NOT_REQUIRED다. 추후 양쪽 필수 작업에서 LC만 완료면 전체 PROCESSING, learningCore=COMPLETED, billing=PENDING이다. 기존 LC-only 작업을 조회했을 때 Billing 도입 이후에도 기존 NOT_REQUIRED/전체 완료 상태를 유지한다.

#### C. 응답 유실·앱 재시작 복구용 목록

`GET /api/v1/users/me/merges?activeOnly=true&limit=20&cursor=...`

- activeOnly 기본 true: completedAt=null인 PROCESSING/ACTION_REQUIRED만 조회. false면 보관 중 완료 기록도 포함한다.
- limit 기본 20, 최대 50. `(createdAt desc, _id desc)`의 안정적 keyset pagination, 불투명 cursor. cursor는 인증 사용자/필터에 바인딩해 검증하며 클라이언트 임의 사용자 조회 조건으로 쓰지 않는다.
- result: `{ "items": [단건 result], "nextCursor": null 또는 문자열 }`. 정렬 순서를 “이번 기기의 작업 ID”로 해석하지 않는다. 한 회원에 여러 Guest 병합이 있을 수 있다.
- merge 응답을 잃었다면 기존 계약대로 SNS exchange로 회원 토큰을 복구한 뒤 내 진행 작업을 조회한다. 이미 완료된 작업도 확인해야 하면 activeOnly=false를 사용한다.
- 첫 페이지가 비었거나 특정 작업이 안 보인다고 “내 이전이 전부 끝났다”고 단정하지 않는다. 이 목록은 새 추적 계약/보관기간 내 데이터다. 토큰 발급/merge 재실행/기존 Guest 복원을 수행하지 않는다.

#### 오류·보호 규칙

| HTTP / code 제안 | 의미 |
| --- | --- |
| 400 INVALID_MERGE_STATUS_REQUEST | UUID/limit/cursor/필터 형식 오류 |
| 401 COMMON_UNAUTHORIZED | 인증 없음/유효하지 않은 사용자 JWT |
| 403 ACCOUNT_NOT_ACTIVE | 현재 target 계정 이용 불가; 기존 보안 오류가 선행 가능 |
| 404 MERGE_STATUS_NOT_FOUND | 미존재/보관 만료/다른 회원 소유 동일 응답 |
| 429 MERGE_STATUS_RATE_LIMITED | 조회 한도 초과, Retry-After 제공 |
| 503 MERGE_STATUS_UNAVAILABLE | DB/상태 무결성 등으로 조회를 확정할 수 없음 |

JWT role/account_type만 보고 허용하지 않고 서버의 현재 User를 확인한다. source/target/이벤트 ID는 metric label이나 원문 개인정보 로그에 넣지 않는다. 조회는 회원당 두 경로 합산 30회/분의 공유 한도와 일반 ingress 제한을 기본안으로 한다. 한도 초과 시 다음 시간창까지의 delta-seconds를 Retry-After로 반환하고 앱은 서버 힌트보다 빨리 반복하지 않는다. 네트워크 오류/503은 상태 변경이 아니라 조회 실패다.

### 5.6 프론트 polling과 화면 정책

**프론트 인계 권고안이며 Identity 구현자가 화면을 만들거나 사용자에게 추가 UI 결정을 요구하는 완료 조건이 아니다.** 서버 담당은 상태/조회 API·안전한 권한 검증·오류/한도 계약을 제공한다. 실제 화면·polling 적용은 프론트에서 수행한다.

- 성공 직후 단건 조회, 이후 nextPollAfterSeconds 이상 대기하고 0~20% 양의 jitter를 더한다. 동일 작업 중복 요청 금지, 오래된 응답이 최신 COMPLETED를 되돌리지 않게 한다.
- background/화면 이탈 시 polling 중지. 2분 foreground cap 이후 “반영 중, 나중에 자동 확인” 안내 및 수동 새로고침 제공. 재진입/foreground에 목록 1회 재조회 후 필요한 단건만 갱신.
- 복수 작업은 목록으로 묶어 확인하고 각 작업별 무제한 병렬 polling하지 않는다. 정상 화면에서 전체 30회/분 한도를 넘지 않게 한다.
- ACTION_REQUIRED는 “일부 기록 반영에 시간이 걸려 확인이 필요합니다” 등 지원 안내. 클라이언트가 merge를 다시 호출하거나 계정을 다시 생성하지 않는다.
- 기존 회원 토큰으로 일반 화면을 제공한다. 기록/권리 영역만 진행 안내, 각 서비스 API 갱신 및 서버 권한 검증을 따른다. 완료했다고 캐시를 그대로 두지 않고 정상 학습 기록/권리 조회를 새로 한다.

### 5.7 과거 데이터·rollout·운영

- 새 추적 기능 이전의 legacy UserMergedOutbox 및 기존 OwnerEventCore PUBLISHED는 자동 progress 생성/완료 변환하지 않는다. 과거 UserMerged의 Billing backfill 금지 계약 유지.
- 예외적으로 과거 완료 상태를 복구하려면 consumer inbox의 동일 eventId/payload commit 증거를 직접 검증하는 **별도 승인된 read/reconciliation 작업**으로 설계한다. 새 이벤트 발행이나 cursor 수동 전진으로 상태를 꾸미지 않는다.
- 첫 출시 순서: LC strict 204 확인/필요 배포 → Identity profile별 core/delivery/index·조회/ACK 지원 배포 → Identity+LC E2E(Billing 완전 미설정 포함) → LEARNING_CORE_ONLY 신규 capture 추적 ON → 프론트 진행 UI 연동.
- Billing 도입 순서: Billing commit/멱등 204·권리 inventory 확인 → 양쪽 consumer E2E → 신규 병합의 profile을 LEARNING_CORE_AND_BILLING으로 전환. 기존 LC-only core/progress를 수정하거나 Billing delivery/sequence를 추가하지 않는다. 과거 병합 replay/backfill은 하지 않는다.
- 추적 ON 시 owner fanout capture와 **선택 profile의 필수 consumer만** 준비/해당 publisher 활성 조건을 검증한다. consumer 또는 publisher 일시 장애는 기존 작업을 삭제하지 않고 조회 상태/경보로 드러낸다. 예전부터 Billing 필수로 저장된 미완료 작업이 있다면 새 profile로 덮어쓰지 않고 기존 운영 복구 절차로 처리한다.
- rollback은 새 추적 생성/새 merge 진입을 통제하되, 기존 progress 읽기 및 이미 생성된 이벤트의 안전한 재시도/완료 기록은 유지한다. 이미 commit된 LC/Billing 데이터를 자동 역이전하지 않는다.
- 장기 pending/consumer pause/dead letter/ACK contract violation/progress 갱신 실패/조회 오류 지표 추가. 기존 운영 replay만 사용하고 사용자용 재실행 mutation API는 만들지 않는다.

### 5.8 구현 순서와 검증 완료 조건

1. 첫 출시는 LC 이전 대상 inventory·완료 ACK/duplicate/timeout 계약 확정. Billing 검증은 도입 시 수행하며 첫 출시를 막지 않는다.
2. profile별 requiredConsumers·delivery/sequence 생성 및 기존 core 불변식 보완, progress entity/index/repository와 aggregate 규칙, UserMerged 실제 eventId 연결 및 merge 응답 DTO 구현.
3. publisher의 tracked USER_MERGED strict ACK 및 local 완료 트랜잭션 연계, FIFO blocker/feature flag/retention 처리.
4. 인증된 GET 단건/목록, IDOR 방지·현재 User 검증·pagination/no-store/공유 한도·오류 계약 구현.
5. 프론트 계약·Swagger·로컬 UI·설정·운영 런북/작업 기록 갱신. 실제 앱 저장소는 별도 작업.
6. 첫 출시는 격리 테스트·실제 Mongo replica set/Identity+LC E2E 통과가 필요하다. Billing 도입 시 세 서비스 E2E를 추가 통과해야 해당 profile을 켤 수 있다.

필수 테스트:

- merge/progress/event/delivery/session 저장의 원자성, 실제 eventId 일치, source 중복 병합 차단.
- 첫 출시 Billing 설정/credential/서비스 없음에도 LC-only 생성·전달·조회 완료, Billing delivery/sequence 미생성. NOT_REQUIRED는 confirmedAt=null.
- profile 변경 후 과거 LC-only snapshot/완료 불변 및 자동 replay/backfill 없음, 신규 작업만 양쪽 필수. Billing 필수 기존 작업이 장애/flag OFF로 NOT_REQUIRED가 되지 않음.
- LC 먼저/Billing 먼저/동시 ACK, 두 개 중 하나만 완료, requiredConsumer 누락, delivery TTL 부분 삭제, progress 완료 단조성.
- 204/200/202/5xx/timeout/429/영구 실패/circuit pause/다른 이벤트의 FIFO blocker/flag OFF 분류. trial rebind 동작 불변.
- consumer commit 직후 응답 유실, Identity ACK 저장 rollback, lease 경합과 같은 eventId duplicate 재전송 시 정확히 한 번 이전.
- 다른 회원 ID/위조 cursor/다른 필터 cursor/Guest·탈퇴·정지 토큰 접근, legacy 미추적/만료/없음에서 완료를 추측하지 않는지.
- 같은 target의 서로 다른 Guest 병합, 앱 종료·토큰 응답 유실·exchange 복구·목록 pagination, stale 응답/과도한 polling 방지.
- 하위 서비스의 실제 기록·권리와 API COMPLETED 일치, source actor 금지·source in-flight write 경합·중간 탈퇴·target 정상 신규 쓰기.
- 완료/미완료 보관 정책과 탈퇴 privacy 정리. PII/credential 비로그 및 no-store.

## 6. 부록 — 확인된 구현 사실과 설계 근거

| 구분 | 근거 | 확인 내용 |
| --- | --- | --- |
| 현재 구현 | [GuestMergeTransactionService](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseGuestMergeTransactionService.java) | User/Session 및 legacy 또는 fanout event 저장은 같은 Mongo 트랜잭션, 상태 조회 요약은 없음 |
| 현재 구현 | [OwnerEventCaptureService](../../src/main/java/web/tosunsaeng/identity/domain/auth/ownerevent/application/OwnerEventCaptureService.java) / [OwnerEventCore](../../src/main/java/web/tosunsaeng/identity/domain/auth/domain/entity/OwnerEventCore.java) | 새 eventId 생성, UserMerged 필수 consumer BILLING/LEARNING_CORE |
| 현재 구현 | [OwnerEventPublisher](../../src/main/java/web/tosunsaeng/identity/domain/auth/ownerevent/application/OwnerEventPublisher.java) | 모든 2xx 성공, consumer별 FIFO/circuit/retry; 204 strict 판정은 아직 없음 |
| 현재 구현 | [OwnerEventPublishTransactionService](../../src/main/java/web/tosunsaeng/identity/domain/auth/ownerevent/application/OwnerEventPublishTransactionService.java) | delivery/cursor 완료 트랜잭션, allPublishedAt 및 TTL 관리; 사용자 조회용 완료 증거와 다름 |
| 현재 DTO | [FirebaseSignupResponse](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/dto/response/FirebaseSignupResponse.java) | signup/upgrade/merge 공용 토큰 응답, mergeId 없음 |
| 문서상 consumer 계약 | [LC handoff §4.2](learning-core-user-merged-consumer-handoff.md#42-v1-처리-모델과-응답) | direct local transaction commit/동일 duplicate 뒤 204, 202 금지 |
| 문서상 fanout 계약 | [Stage 7 §5.11](billing-entitlement-owner-fanout-stage-7-plan.md#511-publisher-상태오류-분류) / [운영 런북](owner-event-fanout-runbook.md) | consumer commit 뒤 2xx, 동일 event replay, 과거 Billing 자동 backfill 없음 |
| 현재 프론트 계약 | [프론트 가이드 §4.5](frontend-firebase-auth-integration-guide.md#45-guest를-기존-member로-통합) | merge 성공은 downstream 전체 완료 아님, 조회 API 미구현 |
| 본 계획의 제안 | 본문 §5 | 신규 요약·GET 2개·mergeId·strict ACK·profile별 필수 consumer/NOT_REQUIRED·retention. 프론트 polling은 인계 권고. 아직 구현되지 않음 |

**추후 변경 조건:** consumer가 durable inbox에 접수만 하고 202를 반환하는 구조로 전환된다면 본 계획의 204 ACK 기반 완료 모델은 그대로 사용할 수 없다. 그때는 서버 간 완료 이벤트/신뢰된 receipt 또는 내부 처리 상태 조회를 별도 버전으로 설계해야 한다. 사용자에게는 동일한 public 조회 형태를 유지할 수 있어도, 내부 완료 근거는 새로 합의해야 한다.
