# SNS 연결 취소·실패 복구 수정 계획서

- 작성 기준: 2026-10-02, develop.
- Jira: [TMI-191](https://to-teacher.atlassian.net/browse/TMI-191), 부모 TMI-136.
- 상태: 구현 전 계획. 현재 배포 계약을 바꾸는 문서가 아니며, 아래 추가 API/필드는 제안이다.
- 구현 반영(2026-10-02): 아래는 원 계획을 보존한다. 실제 API/플래그 및 남은 검증은 [구현·운영 계약](provider-link-recovery-operations.md)을 우선한다. 배포 미수행.
- 목표: 안전하게 종료된 작업이 회원의 연결 잠금을 계속 점유하지 않게 하고, 새로고침 이후에도 상태를 복구한다.

## 1. 5줄 결론

1. 정상 연결·로그인은 유지하고, 실패 종료·취소·새로고침 복구를 추가한다. 아직 구현된 계약이 아닌 제안이다.
2. PREPARED는 start와의 원자적 경합 검사 후 즉시 취소할 수 있다.
3. STARTED는 클라이언트 오류·팝업 종료·시간 경과만으로 안전하게 취소 완료를 확정할 수 없다.
4. 원격 실행 종료와 연결 결과를 입증하지 못하면 ACTION_REQUIRED를 유지하고, 인증된 상태 복구 및 운영 복구 경로를 제공한다.
5. 모든 STARTED 실패의 즉시 자동 재시도를 요구한다면 현 Firebase SDK 직접 연결 구조의 추가 설계가 필요하며, 이번 개선만으로 보장하지 않는다.

## 2. 사용자가 반드시 읽어야 하는 내용

### 확인된 현재 동작

- [ProviderLinkService.start](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderLinkService.java): SDK 실행 전 provider 차단 및 회원 작업 slot 확보. 중복 start는 실행 허가를 다시 주지 않는다.
- [ProviderLinkAttempt](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderLinkAttempt.java): PREPARED/STARTED/COMPLETED/ALREADY_LINKED만 저장한다. STARTED는 cleanupAt=null이며 만료 시에도 작업은 남는다.
- [ProviderLinkService.status](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderLinkService.java): 원래 요청 ID로 조회. 만료/세대 변경 STARTED는 ACTION_REQUIRED로 보인다.
- 로컬 테스트 도구 provider-link.mjs: SDK 호출 전 LINK_UNKNOWN으로 변경하고, 예외 발생 시 안전한 종료 API가 없다. 메모리 전용이므로 새로고침하면 원래 요청 ID를 잃는다.

### 변경 후 사용자 동작

- 연결 준비 중 취소: 서버 취소 확인 후 다른 계정 선택 가능.
- 진행 중 취소: 추가 SDK 호출/자동 재시도를 멈추고 취소 요청을 서버에 기록한다. 화면에서 나가기는 허용하되 연결 취소 완료와 구분한다.
- 이미 연결된 타 회원 SNS: 원인 안내. 실패를 보고하되 클라이언트 오류 코드를 권한 해제 증거로 사용하지 않는다.
- 응답 유실/새로고침: 기존 작업 조회로 복원한다. 새 요청 생성이나 SDK 재실행부터 하지 않는다.
- 완료: 성공 안내. 완료된 연결을 취소 명목으로 자동 unlink하지 않는다.

## 3. 사용자가 결정해야 하는 사항

### 권고하는 구현 범위

| 구분 | 이번 구현 계획에 포함 | 포함하지 않음 |
| --- | --- | --- |
| 서버 | 준비 취소, 진행 중 취소/실패 보고, 본인 작업 복원, 감사 가능한 조건부 운영 복구 | 모든 STARTED 자동 해제, 시간만으로 잠금 삭제 |
| 테스트 화면 | 취소·상태 조회·새로고침 복원·안전한 종료 뒤 새 요청 | 다른 회원의 SNS 강제 이전, 자동 unlink |
| 실제 앱 | API/상태/저장/오류 처리 가이드 및 앱 검증 항목 | 별도 앱 저장소 코드를 확인하지 않은 채 구현 완료 선언 |
| 운영 | dry-run 우선 제한 복구 절차와 배포 체크리스트 | 운영 DB 일괄 정리, 과거 STARTED 자동 migration |

서버 구현과 로컬 도구 변경은 별도 변경 단위로 관리한다. 이 계획서 작성은 코드 구현·배포·Jira 변경 승인이 아니다.

- 권고 범위: 준비 단계 즉시 취소 + 진행 단계 취소 요청/상태 복원 + 조건부 운영 복구 자동화 도구. STARTED 전부를 자동 복구한다고 약속하지 않는다.
- 모든 오류에서 운영자 없이 즉시 다른 계정으로 재시도해야 하는 요구는 별도 원격 실행 제어 설계의 범위다. 현재 방식으로 안전성을 약화해서 맞추지 않는다.
- 아래 API/상태 이름은 제안이며 프론트 계약과 함께 확정한다. 기존 5분 인증 정책과 연결 허가 TTL을 이 작업에서 임의 변경하지 않는다.

## 4. 주요 위험과 미확인 사항

- Firebase와 Mongo 사이에는 원자 트랜잭션이 없다. 원격 조회 직후 지연 SDK 연결이 성공할 수 있다.
- 서버가 모바일 SDK 실행 종료를 신뢰성 있게 입증할 수 있는 신호는 현재 프로토콜에 없다. 실패 보고/팝업 종료/부재 조회를 조합해도 악의적·지연 실행을 일반적으로 배제하지 못한다.
- 오래된 STARTED 문서를 시간만으로 일괄 종료하지 않는다. 기존 운영 절차는 [Stage 10 runbook §5.5](firebase-provider-unlink-stage-10-runbook.md)를 유지한다.
- 서버 완료 호출의 세대 검증은 Identity 등록을 막을 수 있지만 Firebase 자체의 늦은 연결을 취소하는 수단은 아니다.
- 기존 SNS 로그인/프로필/재발급이 보호 상태와 어떻게 상호작용하는지 회귀 검증한다. UI를 닫을 수 있다는 사실을 모든 서버 작업 허용으로 설명하지 않는다.

## 5. 현재 작업과 직접 관련된 수정안

### 서버 상태 및 트랜잭션

- 추가 종료 상태 제안: CANCELLED(안전한 취소 완료), FAILED(실패 결과 확정 및 정리 완료).
- 취소 요청은 cancelRequestedAt 등의 메타데이터로 저장하고, 불명 STARTED를 종료 상태로 위장하지 않는다. 필요 시 응답에 취소 대기 사유를 추가한다.
- PREPARED 취소: 인증된 owner/binding/epoch/revision/version 확인 후 조건부 전환. start와 경쟁하면 한쪽만 성공한다. 이미 STARTED면 취소 요청 단계로 안내한다.
- STARTED 복구: exact owner/attempt/slot/version/phase, Firebase UID/binding, 기존 SNS/전화번호 보존, 대상 소유권, 다른 보안 작업 유무, 이전 원격 실행 종료 증거를 대조한다.
- 증거가 확보된 승인 복구만 감사 기록 + 종료 상태 전환 + 해당 slot 해제를 동일 Mongo 트랜잭션으로 수행한다. 대상 provider 차단과 인증 floor는 보존한다. 다음 정상 연결 완료에서만 기존 검증대로 대상 차단을 해제한다.
- 실패 작업을 삭제하지 않는다. 종료 시점을 기준으로 보존/cleanup 정책을 정하고, 감사 기록의 별도 보존·접근 제한을 명시한다.
- 반복 취소/종료 요청은 같은 최종 상태를 반환한다. old complete는 종료 상태·세대 검사로 거절한다. completed에 대한 취소는 완료 상태 반환, 자동 해제 없음.

#### 상태 전환 기준

| 현재 상태/상황 | 요청 | 저장/응답 및 다음 행동 |
| --- | --- | --- |
| PREPARED, start 미실행 | cancel | CAS 성공 시 CANCELLED. 시작 허가 없음. 새 prepare는 새로운 요청 ID 사용 |
| PREPARED, start와 경합 | cancel/start | 작업 version을 기준으로 하나만 승리. start가 선행하면 STARTED 취소 요청으로 처리 |
| STARTED, SDK 실패 보고 | failure-report | STARTED 유지, 허용된 실패 분류만 저장. 실행 종료 확정 전 재시도 불가 |
| STARTED, 취소 의도 | cancel | cancelRequestedAt 기록, SDK 실행 허가 추가 발급 없음 |
| STARTED, 결과 불명/기한 초과 | status/pending | ACTION_REQUIRED 안내. 잠금·차단·원 작업 보존 |
| STARTED, 원격 연결 존재 | 상태 확인 | 취소 완료로 전환하지 않음. 기한 내 정상 대상 재인증/complete 또는 승인 복구 안내 |
| STARTED, 안전한 종료 증거 확보 | 제한된 운영 복구 | 감사 기록과 함께 CANCELLED 또는 FAILED 전환, exact slot 해제, provider 차단/floor 유지 |
| CANCELLED/FAILED | 중복 cancel/복구 | 같은 결과 반환. 새로운 변경/SDK 허가 없음 |
| COMPLETED/ALREADY_LINKED | cancel | 완료 상태 반환. 자동 연결 해제 없음 |

취소 요청만 기록된 STARTED의 기존 complete는 현 소유권·기한·대상 인증 검증을 계속 충족해야 한다. 먼저 완료된 경우 사용자에게 '이미 연결 완료'를 표시한다. 실제 CANCELLED/FAILED로 종료된 뒤에는 complete를 거절한다. 최종 정책은 프론트 문구와 함께 고정한다.

#### 데이터 변경안

- ProviderLinkAttempt에 CANCELLED/FAILED 및 nullable cancelRequestedAt, failureCode, resolvedAt, resolutionReason, recoveryAuditId를 추가하는 안을 검토한다. 기존 문서에 필드가 없어도 읽을 수 있어야 한다.
- failureCode는 서버 허용 목록의 분류 값이며 raw SDK 오류 문자열을 저장하지 않는다. 최초 보고를 보존하고 반복 보고가 보안 revision을 무한히 바꾸지 않게 한다.
- 취소/실패 보고는 진행 중 보호 의도를 바꾸지 않으므로 불필요하게 method revision을 증가시키지 않는다. 실제 복구에서만 control/method/attempt 버전을 일관되게 변경한다.
- cleanupAt은 확정 종료 후 기존 permitRetention 정책과 맞추되, 감사 이력 보존 기간은 별도 결정한다. unresolved STARTED에는 TTL을 새로 부여하지 않는다.
- 현재 Guard는 STARTED 존재 여부도 검사한다. slot만 비우고 작업을 STARTED로 남기면 계속 차단되므로 상태/slot/감사 저장을 원자적으로 처리한다. 종료 상태 추가 후 모든 상태 분기와 조회 조건을 함께 점검한다.
- pending 조회의 실제 접근 패턴을 먼저 확정하고 필요한 userId/state 인덱스는 explain 검증 후 추가한다. 무제한 전체 이력 스캔을 허용하지 않는다.

### API 제안

기존 prefix `/api/v1/auth/firebase/providers/link` 및 prepare/start/complete/status는 유지한다.

| 제안 API | 목적 | 권한 및 제한 |
| --- | --- | --- |
| POST /cancel | 취소 의도 기록 또는 PREPARED 취소 확정 | 기존 사용자 Bearer 및 적절한 기존 제공자 증명. 원 요청 ID로 유실 prepare도 대조. STARTED를 무조건 종료하지 않음 |
| POST /failure-report | 허용된 SDK 오류 종류 보고 | 정보성 증거일 뿐 잠금 해제 권한 아님. raw 오류/이메일/credential 저장 금지 |
| POST /pending | 새로고침 후 현재 회원 작업 복구 | 인증/소유권 확인. 임의 userId 입력 없음. 필요한 최소 작업 정보만 반환 |

복구 적용은 제한된 운영 명령/관리 경로로 분리하며 일반 앱 cancel과 동일 권한으로 노출하지 않는다. 기존 status로 원 ID 조회 가능한 경우 pending 호출을 우선할 필요는 없다. 여러 PREPARED가 존재할 수 있으므로 pending 조회는 단일 최신 문서를 임의 선택하지 않고 원 ID 우선 및 STARTED slot 대조/후보 처리를 정의한다.

응답 추가 필드 후보: nextAction, retryAllowed, cancelRequested. retryAllowed는 안내 값이며 서버 권한 검증을 대체하지 않는다. 어떤 status 응답도 linkAllowed=true를 새로 부여하지 않는다. SDK 1회 허가는 최초 start 응답에만 유지한다.

#### 요청/응답 및 멱등성 세부안

- cancel: 기존 requestId와 fresh firebaseIdToken으로 원 작업을 찾는다. linkAttemptId도 받는다면 같은 작업인지 교차 검증한다. userId는 JWT에서만 얻는다.
- failure-report: 같은 소유권 검증과 requestId, 허용된 failureCode를 받는다. 보고 내용만으로 FAILED로 전환하지 않는다.
- pending: Bearer와 Firebase 증명으로 본인 binding을 검증한다. 진행 중 slot 대상 우선 및 제한된 PREPARED 후보를 반환한다. 응답에 Firebase UID/전화번호/provider subject를 노출하지 않는다.
- 같은 요청 ID의 재전달은 기존 작업 상태를 반환한다. 확정 종료 후 새로운 시도에만 새 요청 ID를 만든다.
- retryAllowed=true도 새 prepare 시점의 전체 보안 검증을 생략하지 않는다. 다른 보안 작업/계정 상태 변화가 있으면 재시도는 거절될 수 있다.
- 상태 조회에서 재인증 필요와 작업 부재를 구분하고, 404를 새 SDK 실행 허가로 해석하지 않는다.

### 프론트·로컬 테스트 도구

- 시작 전 원 요청 ID/작업 ID/대상 provider/로컬 세션 세대를 보존한다. 토큰·전화번호·이메일은 복구 메타데이터에 저장하지 않는다. 저장값은 신뢰하지 않고 서버로 대조한다.
- 웹 테스트는 제한된 sessionStorage 등으로 메타데이터 복원, 실제 모바일은 기존 안전한 저장소/세션 계약을 따른다. 기존 Firebase 인증을 복원하거나 재인증한 후 본인 작업만 조회한다.
- 취소 버튼은 추가 실행을 중단시키고 상태를 조회한다. fetch 중단/팝업 닫힘을 원격 취소 성공으로 표시하지 않는다.
- bounded polling 종료 후 수동 상태 확인/화면 나가기 제공. 백그라운드 무한 요청이나 자동 새 prepare 금지. 구체적 간격/상한은 별도 확정한다.
- 다른 회원 소유 계정, 네트워크 불명, 연결 완료, 안전한 취소 완료를 서로 다른 문구로 안내한다.
- 오래된 탭/요청의 응답은 세션·작업 세대가 다르면 현재 UI를 덮어쓰지 않게 한다. 종료된 작업의 늦은 SDK 성공은 재조회/보호 대상으로 처리하고 자동 등록하지 않는다.
- 테스트 도구는 저장소 밖 별도 프로젝트이며 실제 앱 프론트 반영은 별도 전달·구현이 필요하다.

### 로그·배포

- 구조화 이벤트: cancel_requested, cancelled, failure_reported, recovery_required, recovered 등 고정 분류. 원문 credential/전화번호/이메일/SDK 오류 메시지 제외.
- 메트릭 label에 사용자/작업 ID를 넣지 않는다. 감사 이력은 제한된 DB 접근으로 분리하고 현재 애플리케이션 로그 마스킹 규칙을 따른다.
- 서버와 프론트가 새로운 상태를 함께 이해하도록 단계 배포한다. 구 클라이언트가 새로운 상태를 어떻게 처리하는지 확인하고 필요하면 신규 경로 플래그를 둔다.
- 기존 STARTED의 자동 이행/삭제 없음. 운영 기능 활성화 전 테스트 환경 경합·복구 실증 필요.

## 6. 부록 — 회귀 테스트 및 구현 순서

### 변경 대상 및 작업 분할

| 단계 | 주요 파일/대상 | 완료 조건 |
| --- | --- | --- |
| A. 계약 확정 | 이 계획, Stage 10 runbook, frontend-firebase-auth-integration-guide/appendix | 상태/취소 경합/소유 증명/복구 권한/보존 정책 명시 |
| B. 서버 상태·API | ProviderLinkAttempt, ProviderLinkService, ProviderLinkController, 관련 오류/설정 | cancel/failure-report/pending 입력 검증·소유 검사·멱등성 구현 |
| C. 보호·복구 | ProviderChangeGuard, session/method control 연동, 제한된 운영 복구 도구 | dry-run 무변경, 정확한 작업만 원자 종료, 차단/floor 보존 |
| D. 클라이언트 | 별도 로컬 provider-link.mjs/app.js/index.html 및 테스트 | 취소 표시·원 요청 복원·stale 응답 무시·중복 SDK 실행 방지 |
| E. 회귀 검증 | ProviderLinkHttpTests, ProviderChangeTests 및 신규 복구 테스트 | 아래 테스트 표와 전체 clean test 통과 |
| F. 테스트 배포 | 앱/서버 호환 확인 및 실제 Provider 실증 | 정상 로그인 유지, 승인 복구 후 새 연결 가능, 불명 결과는 보호 유지 |

기존 서버·클라이언트 성공 테스트를 보존한다. 운영 복구 도구를 일반 공개 API로 노출하지 않으며 실제 계정/데이터 변경은 별도 승인 절차를 따른다.

| 테스트 | 기대 결과 |
| --- | --- |
| PREPARED 취소 및 중복 취소 | CANCELLED, SDK 허가 없음, 멱등 응답 |
| 취소/start 동시 실행 | 하나의 전환만 성공, 이미 STARTED면 취소 완료로 거짓 보고하지 않음 |
| 타 회원 소유 credential SDK 오류 | 명확한 안내, 기존 계정 불변, 종료 증거 없으면 잠금 유지 |
| 조작된 실패 보고/타 회원 작업 ID | 다른 사용자 접근 및 무조건 해제 불가 |
| cancel/prepare/status 응답 유실 | 같은 원 ID로 조회, 중복 SDK 호출 없음 |
| 새로고침/재실행/계정 전환 | 서버 상태 복원, 다른 계정 작업 노출/재개 금지 |
| 원격 조회 실패 또는 대상 부재만 확인 | 자동 종료 불가, ACTION_REQUIRED 유지 |
| 취소 요청 뒤 SDK 늦은 성공 | 원격/서버 불일치 탐지, 자동 승인 금지 |
| complete/cancel 경합 | 완료된 연결 자동 해제 없음, 종료 작업 complete 거절 |
| 승인된 복구 트랜잭션 실패 | 감사/작업/slot 변경 전부 rollback |
| 정상 Google/Apple/Kakao 연결 | 기존 MEMBER/전화번호/SNS 보존 및 로그인 성공 |
| 로그·감사 검사 | 비밀값 비노출, 시도/종료 원인 추적 가능 |

순서: 계약/증거 요건 확정 → 서버 상태·API/테스트 → 로컬 도구 및 프론트 가이드 → 전체 clean test → 테스트 배포 → 실제 Provider·취소/지연·응답 유실 검증 → 별도 운영 판단.

### 완료 판정 체크리스트

- [ ] 준비 취소 후 기존 요청은 시작 불가, 새 요청으로 정상 연결 가능.
- [ ] 종료 증거가 부족한 STARTED는 실패 보고/취소/시간 경과로 자동 해제되지 않음.
- [ ] 승인된 복구는 DB 문서 직접 수동 삭제 없이 감사·상태·slot을 함께 처리함.
- [ ] 종료된 작업은 pending/guard에서 미완료 작업으로 남지 않으며, 대상 provider 보호는 유지됨.
- [ ] 새로고침/계정 전환/응답 유실 후 본인 원 작업으로 복원되고 SDK를 중복 실행하지 않음.
- [ ] 동일 요청 재전달, cancel/start/complete 및 늦은 원격 성공 경합 테스트 통과.
- [ ] 기존 SNS·전화번호·회원 UUID 보존과 Google/Apple/Kakao 정상 로그인 회귀 통과.
- [ ] 비밀값 없는 구조화 로그와 제한된 감사 이력으로 요청/종료 원인을 추적할 수 있음.
- [ ] 실환경 검증과 mock 검증을 구분하여 기록하고, 모바일 미검증을 완료로 보고하지 않음.

### 배포 전/중단 기준

- 새 상태를 읽지 못하는 구 서버와 혼재하지 않도록 코드 호환 배포 후 신규 경로를 활성화한다. 프론트는 먼저 새 상태를 안전하게 표시할 수 있어야 한다.
- 취소 성공인데 slot 잔존, 종료된 작업의 complete 성공, 다른 회원 작업 접근, 기존 SNS 로그인 회귀 중 하나라도 발견되면 신규 기능 활성화를 중단한다.
- 중단 시 데이터나 provider 차단을 일괄 삭제하지 않는다. 기존 보안 보호를 유지하면서 신규 취소/복구 변경 접수를 제한한다. 새 enum을 읽지 못하는 구 버전으로 무조건 rollback하지 않는다.
- 정책 수치 미확정 항목(감사 보존 기간, 폴링 간격/횟수)은 계약 확정 단계에서 결정한다. 임의 timeout으로 자동 해제하는 기능은 포함하지 않는다.

이 문서는 분석·제안이며 구현·배포 완료 보고가 아니다.
