# TMI-191 연결 취소·실패 복구 구현·운영 계약

## 1. 5줄 결론

1. 신규 cancel/failure-report/pending API는 `FIREBASE_PROVIDER_LINK_RECOVERY_ENABLED=false`가 기본이며 기존 연결 플래그와 별개다.
2. PREPARED만 CANCELLED로 즉시 전환한다. STARTED 취소/실패 보고는 보호를 유지하고 ACTION_REQUIRED로 안내한다.
3. status/pending/start 재조회로 SDK 실행권한을 복원하지 않으며 정상 complete 검증은 유지한다.
4. STARTED 확정 종료는 공개 API가 아닌 제한된 운영 도구에서 별도 승인·SDK 종료 확인·dry-run digest 대조 후 수행한다.
5. 구현/격리 테스트와 실환경 검증은 다르다. 이번 변경으로 배포·실제 계정 복구·모바일 실증을 수행하지 않는다.

## 2. 반드시 읽어야 하는 내용

prefix `/api/v1/auth/firebase/providers/link`. 모든 요청에 사용자 Bearer와 최근 Firebase 본인 증명이 필요하다. 기존 no-store/ingress 및 per-user rate limit을 유지한다.

| POST 경로 | JSON body | result |
| --- | --- | --- |
| `/cancel` | linkAttemptId, firebaseIdToken | 기존 Status |
| `/failure-report` | 위 두 필드 + failureCode | 기존 Status |
| `/pending` | firebaseIdToken | attempts: Status[], hasMore: boolean |

Status는 기존 linkAttemptId/provider/status/expiresAt/linkAllowed 구조를 유지하며 CANCELLED/FAILED 종료 값만 추가한다. failureCode 허용 값은 CREDENTIAL_ALREADY_IN_USE, ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL, POPUP_CLOSED, NETWORK_ERROR, SDK_ERROR. 최초 실패 보고만 저장하며 자유 텍스트를 받지 않는다.

- cancel은 작업 ID를 받는 것으로 확정했다. prepare 응답 유실이면 기존 status에 원 requestId를 보내거나 pending으로 작업 ID를 찾는다.
- PREPARED 취소는 version 저장으로 start와 경합한다. 반복 취소는 쓰기 없이 같은 결과다. 새 시도는 새 Idempotency-Key로 prepare한다.
- STARTED 보고는 cancelRequestedAt/failureReportedAt/failureCode만 저장한다. 실제 DB state는 STARTED, 응답은 ACTION_REQUIRED다. slot/block/epoch/method revision을 변경하지 않는다.
- 취소 의도만 기록된 작업은 기한 내 정상 complete가 성공할 수 있다. 완료된 작업은 취소 요청으로 자동 unlink하지 않는다.
- stale 세대의 cancel/report는 거절될 수 있다. 상태 조회와 담당자 확인을 사용한다. 만료만으로 자동 해제하지 않는다.
- pending은 실제 link slot 우선, 없으면 현재 binding의 미만료 PREPARED 최대 20개(정렬 preparedAt/_id)를 반환한다. hasMore는 전체 목록이 아님을 뜻한다. 최신 작업 임의 선택 없음. 종료 이력은 원 ID의 status로 조회한다.
- 로컬 도구의 복원은 SDK 허가를 복구하지 않는다. 복원한 준비 작업은 cancel 후 새로 준비하고, STARTED는 담당자 결과 확인을 진행한다.

근거: [Service](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderLinkService.java), [Controller](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderLinkController.java), [Attempt](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderLinkAttempt.java).

### 로컬 테스트 화면

별도 tosunsaeng-integration-test 프로젝트에 취소 요청/SDK 실패 보고/진행 작업 복원 버튼을 추가한다. sessionStorage에는 requestId/attempt/target만 보존한다. UID/토큰/전화번호/SDK 허가는 저장하지 않는다. 새로고침 후 기존 SNS 인증과 Identity 로그인을 먼저 완료해야 한다.

SDK 대기 중 취소 요청 가능. 늦은 SDK 성공/start 응답이 취소된 로컬 권한을 되살리지 않는다. 팝업은 사용자가 닫으며 닫힘만으로 서버 잠금을 해제하지 않는다. 무한 polling 없이 명시적 상태 조회를 사용한다. 손상된 저장 정보나 다른 owner의 조회 거절 시 자동 새 작업을 만들지 않는다. 실제 모바일 앱 구현은 별도다.

## 3. 운영자가 결정·승인해야 하는 사항

- 구 서버가 새 enum을 읽을 수 있도록 전체 코드 호환 배포 후 신규 플래그를 활성화한다. 실제 프론트도 새 상태를 처리해야 한다.
- 복구는 티켓별 승인 및 모든 이전 SDK 실행/팝업/자동 재시도 종료의 담당자 확인이 필요하다. 클라이언트 주장만으로 apply하지 않는다.
- 감사 컬렉션은 자동 TTL을 생성하지 않는다. 개인정보 보존 정책에 맞는 기간/삭제 담당자를 확정하고 운영 역할로 접근을 제한한 뒤 운영 도입한다. 내부 식별자 snapshot을 일반 로그/티켓으로 복사하지 않는다.
- IAM/네트워크/DB 권한 변경은 이번 구현에 없다. 실제 도구 실행과 계정 복구는 별도 승인이다.

## 4. 위험과 미확인 사항

- dry-run 성공은 SDK 종료 증거가 아니다. Firebase와 Mongo의 원자성은 없으며 digest도 원격 지연 실행을 막지 못한다.
- 도구는 대상 제공자가 원격/Identity 모두에 없는 경우만 지원한다. 기존 소셜 계정 및 전화번호가 필요하며 drift/다른 미완료 작업이면 중단한다.
- tenant 없는 프로젝트, Kakao ID oidc.kakao 기준이다. 다른 매핑/tenant는 도구를 그대로 사용하지 않는다.
- pending은 기존 userId 인덱스를 사용한다. 대규모 환경 explain/인덱스 및 실제 replica-set rollback/경합은 별도 검증한다.
- 로컬/모의 테스트는 모바일 종료·복귀 검증이 아니다. 원 작업이 TTL로 없어졌다면 담당자가 현재 pending을 대조한 뒤 판단하며 로컬 정보 강제 초기화로 우회하지 않는다.

## 5. 제한된 운영 복구 절차

도구: [provider_link_recovery.py](../../scripts/provider_link_recovery.py). 공식 pymongo/google-auth/requests를 설치한 격리 관리 환경에서 사용한다. MONGODB_URI와 GOOGLE_APPLICATION_CREDENTIALS는 승인된 secret 공급 경로로 주입하고 실제 값은 명령 인자/로그/공유 파일에 넣지 않는다.

1. 정확한 회원/작업과 모든 SDK 실행 중단, 자동 재시도 부재를 담당자가 확인한다.
2. `--database <대상 DB> --project <Firebase 프로젝트> --attempt-id <작업 UUID>`로 dry-run한다. 기본 쓰기 0건이며 검증 digest만 출력한다.
3. 관리 화면에서 기존 계정/번호 및 원격 결과를 검토하고 승인한다. dry-run/apply 사이 새로운 연결 시도를 금지한다.
4. 동일 인자에 `--apply --expected-digest <digest> --approval-reference <승인 티켓> --sdk-stopped-confirmed --resolution FAILED`(또는 CANCELLED)를 추가한다. 기한이 남은 STARTED는 거절한다.
5. fresh Firebase 결과와 전체 DB snapshot digest를 대조하고, 원격을 다시 읽어 동일 상태를 확인한다. snapshot/majority 트랜잭션으로 감사 삽입+attempt 종료+exact slot 해제+control/method version/revision 증가를 수행한다. 기존 block/floor/epoch/계정/SNS/전화번호는 수정하지 않는다.
6. 동일 감사 ID가 있으면 재실행하지 않는다. commit 불명/오류 시 원문 예외 없이 고정 경고를 출력한다. 담당자가 감사/최종 상태를 확인하며 맹목적 재시도를 하지 않는다.
7. 종료 attempt는 7일 cleanupAt을 부여한다. 다음 연결의 정상 complete만 대상 차단을 해제한다. 종료된 old complete는 상태 검사로 거절한다.

공개 endpoint/scheduler에서 자동 호출하지 않는다. 종료 증거가 없으면 기존 Stage 10 runbook §5.5 격리 상태를 유지한다.

## 6. 부록 — 검증 및 Jira 댓글 초안

- ProviderChangeTests: 준비 취소/멱등성/버전 충돌, STARTED 보호 유지, pending 소유/우선순위, 취소 요청 후 지연 complete.
- ProviderLinkHttpTests: 신규 body 검증/allowlist/owner 전달/no-store.
- scripts/test_provider_link_recovery.py: 전제 거절·검증 무변경·exact CAS/보호 필드 보존. 실제 DB rollback 증거는 아님.
- 로컬 provider-link 테스트: 취소/유실/실패 보고/메타데이터/다른 owner/다중 후보/대기 SDK 취소·지연 성공.

Jira 댓글 초안(미등록): TMI-191 취소·실패 보고·pending API, 제한된 운영 복구 도구, 로컬 복원 UI 구현. 정상 연결/보호 유지. 테스트 결과는 WORKLOG 참조. 실환경 복구·모바일·replica-set 경합 및 배포 미수행, 신규 기능 기본 OFF.
