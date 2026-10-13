# TMI-203 원천 정리 스케줄·폐기 metadata 구현

- 날짜: 2026-10-13 / 브랜치: `feat/TMI-203-withdrawal-snapshot-feed-recovery`
- 상태: 로컬 구현·Mock 회귀 완료. 실제 DB 삭제·배포·권한 승인 실행 아님.

## 1. 5줄 결론

1. 기존 원본 expiry cleanup과 range sweep을 기본 OFF인 전용 스케줄에 연결했다.
2. 폐기 stream은 closedAt+120일 이후, 활성 대체 stream이 있고 관련 journal/snapshot run이 없을 때만 정리한다.
3. 한 transaction에서 source version CAS와 최대100개 range 정리를 수행하고, 마지막 range가 없어진 뒤 stream metadata를 제거한다.
4. cleanup 플래그·capture/clock 증명·명시적 stream별 Approval이 없으면 ON 기동/실행을 차단한다. 실제 승인 증명을 임의 boolean으로 대체하지 않는다.
5. 전체2104개 중2080통과/24skip/실패·오류0. 실제 replica-set 경합·운영 Approval 연결·경보 및 Billing 복원 검증은 남아 있다.

## 2. 사용자가 반드시 읽어야 하는 내용

새 환경변수 `WITHDRAWAL_RECOVERY_SOURCE_JOBS_ENABLED`의 기본값은 false다. 기존 `WITHDRAWAL_SOURCE_CLEANUP_ENABLED`도 true여야 하지만, 두 플래그만 켜는 것으로 실행을 허용하지 않는다.

실행에는 `RecoverySourceJobsConfiguration.Approval` 구현이 필요하다. 배포 담당이 승인된 환경/stream, 과거 writer 종료·복원 경계 및 timestamp/capture bounds를 확인하는 구현을 제공해야 한다. 이 클래스는 기존 cleanup/sweep 권한과 retired source 정리 권한을 묶은 경계이며 **기본 허용 구현을 제공하지 않았다.** RuntimeEvidence의 captureReady/clockTrusted도 함께 검사한다.

따라서 이번에 완료한 것은 **정리 알고리즘과 스케줄·검증 연결**이다. 실제 운영 승인 증명을 만들어 주입하거나 작업을 활성화한 것은 아니다.

## 3. 배포 전 확인 사항

- Approval의 권한·환경/stream 범위·writer drain·capture 시간 상한 근거를 운영 절차와 연결한다. 요청 body나 단순 ON 플래그를 근거로 승인하지 않는다.
- runtime index16개와 source migration 상태가 유효한지 확인한다. 기동 시 조회 검증만 하며 DDL/자동 이관은 하지 않는다.
- snapshot run이 남아 있으면 폐기 metadata 정리는 보류한다. 기존 snapshot maintenance를 별도로 가동해 row 삭제 후 run 삭제 순서를 유지해야 한다.
- 전용 DB의 journal 선택 쿼리, environment/stream 조회의 실제 실행 계획과 처리량을 확인한다. source 후보 순회는 `_id` 기준이며 매 쿼리5초 상한을 둔다.
- failure/REFERENCED/lease 지표를 실제 경보에 연결하고, 장기 보류는 장애 단위로 조치한다. 실패로 보존기간이 무기한 연장된 것으로 승인하지 않는다.

## 4. 주요 위험과 미확인 사항

- 이번 테스트는 Mock transaction의 commit/rollback 호출과 조건을 검증한다. 실제 MongoDB의 동시 cleanup/claim/restore·unknown commit 정합성은 새 코드로 추가 검증해야 한다. 기존 replica-set opt-in 테스트18개는 이번 실행에서 skip했다.
- Approval 구현과 실제 시계/복원 증명의 유효성은 외부 운영 연결이 필요하다. 신규 플래그가 false인 상태로 배포되며 실제 삭제는 일어나지 않는다.
- snapshot rows에는 streamId가 없다. 존재하지 않는 필드 조회로 부재를 주장하지 않고, rows를 먼저 지운 뒤 run을 제거하는 기존 MongoSnapshotStore 불변식에 의존한다. 수동 변경/DB 손상으로 orphan rows가 생겼다면 별도 복원·정합성 확인이 필요하다.
- deliveries에는 streamId가 없다. journal과 deliveries를 같은 transaction에서 정리하는 기존 경로를 유지한다. orphan delivery를 임의로 전역 삭제하는 코드는 추가하지 않는다.
- source metadata 정리는 consumer 적용 완료의 증명이 아니며, 과거 이력 불명을 COMPLETE로 바꾸거나 사용자 탈퇴 상태를 복원하지 않는다.

## 5. 구현 상세

### 스케줄

`RecoverySourceJobs`는 전용2thread scheduler에서 두 작업을 각각5초 fixed-delay로 실행한다.

- expire: 기존 `WithdrawalSourceCleanup.batch(20, true)`. capturedAt+120일/유효 lease/원천 증거 CAS 및 journal·delivery 삭제 원자성을 유지한다.
- maintain: 환경별 source를 한 번에1개 순회한다. 현재/미만료 stream은 기존 sweep, 만료된 CLOSED_RESTORE는 metadata 정리를 시도한다. 보류/오류가 특정 source 이후의 작업을 굶기지 않도록 후보 `_id`를 다음 조회 힌트로 사용하고 끝에서 초기화한다. 힌트 유실은 재조회일 뿐 checkpoint 유실이 아니다.

### 폐기 metadata

`MongoRetiredSourceCleanup`은 SNAPSHOT/MAJORITY/primary transaction에서 다음을 수행한다.

1. 정확 environment/stream을 조회해 schema·version을 검증한다. ACTIVE 또는120일 미경과는 변경하지 않는다.
2. 명시 Approval과 활성 대체 stream 존재를 확인한다. journal 또는 해당 환경/Billing의 snapshot run이 있으면 REFERENCED로 보류한다.
3. source의 상태/closedAt/head/version CAS를 갱신한 뒤 range를 최대100개 선택해 정확 ID/범위 조건으로 제거한다.
4. range가 남으면 source는 유지한다. 모두 정리됐으면 갱신 version/상태/closedAt 조건으로 source를 제거한다. source 존재 부재는 재실행 시 ABSENT로 확인한다.
5. CAS/정확 삭제 건수 불일치는 rollback한다. unknown commit을 즉시 동일 transaction으로 재시도하지 않고 다음 실행의 새 조회로 확인한다.

원천 journal cleanup의 transaction도 SNAPSHOT/MAJORITY/primary 및30초 timeout을 명시했다. lifecycle의120일/7일 기간과 API/DTO/index manifest/body hash는 바꾸지 않았다.

### 지표

- `identity.withdrawal.source.removed`, `leased`, `cleanup_failure`
- `identity.withdrawal.source.maintenance` (고정 enum outcome만 tag)
- `identity.withdrawal.source.maintenance_failure`

user/event/stream ID 또는 exception 메시지를 metric/tag/log에 남기지 않는다. 실제 외부 경보 수신은 별도다.

## 6. 부록 — 파일·검증·다음 작업

- 신규 production: `domain/user/withdrawalrecovery/MongoRetiredSourceCleanup.java`, `RecoverySourceJobs.java`, `RecoverySourceJobsConfiguration.java`.
- 변경: `domain/user/withdrawalcapture/WithdrawalCaptureConfiguration.java`, `src/main/resources/application.yml`.
- 신규 테스트: `MongoRetiredSourceCleanupTests.java`, `RecoverySourceJobsTests.java` (15개). 코드 root는 `src/main/java/web/tosunsaeng/identity/`다.
- 테스트 내용:120일 경계·ACTIVE 보존·원본/snapshot 참조 차단·활성 대체 부재·권한 거절·CAS rollback·bounded range 정리·unknown commit 비재시도·OFF 기동/Approval 부재·trust 상실·후보 순회·실패지표.
- 실행: `env -u SENTRY_AUTH_TOKEN ./gradlew clean test` 성공: 총2104/통과2080/skip24/실패0/오류0. 24개는 기존 Mongo18/support6 opt-in이다. 기존 recovery-r1 manifest8개 SHA-256 일치 및 diff 검사.
- 다음: 운영 Approval 구현·증명 연결, replica-set 경합/복원 테스트, query plan·외부 경보·Billing 공동 검증 후 별도 활성화 승인.
- 기존 dirty 보존, 예상 밖 변경 없음. 실제 DB/인프라/키/외부 메시지/Jira 수정/commit/push/배포 없음.
- Jira 댓글 초안(미등록): 원천 expiry/sweep 기본OFF 스케줄과 폐기stream metadata bounded 정리 구현. 전체2080통과/24skip, 원본fixture8개hash 유지. 실제 운영 Approval/replica-set/경보·복원 검증은 미완료.
