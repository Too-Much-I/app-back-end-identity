# 운영 DB 준비 변경 목록과 적용 경계

> 최신 상태: [추가전용 DDL 실행 결과](PRODUCTION_DB_DDL_APPLIED_2026-10-08.md). Identity29컬렉션/70인덱스 및 LC16컬렉션/25인덱스 추가 완료. guard 보정·백업·신규 서비스 기동은 미실행. 아래는 적용 전 계획이다.

> 최신 결과: [실행 목록 생성·운영 읽기 검증](PRODUCTION_DB_DRY_RUN_2026-10-08.md)에서 Identity31컬렉션/75개 인덱스 resolver manifest를 생성했다. 운영 LC에 orphan 결과4건·UUID 형식 오류 owner 문서2건을 발견하여 guard 적용을 보류했다. 아래 준비 계획은 유지하되 전체 dry-run/최종 적용 승인은 미완료다.

## 1. 5줄 결론

1. 운영 원본 DB 두 개를 유지하고, **없는 컬렉션·인덱스 추가와 필요한 보호 문서의 insert-only 보정**을 분리한다. 계정/학습 기록 삭제·초기화는 하지 않는다.
2. LC의 병합·탈퇴·알림·챌린지 준비 대상은 합쳐서 신규 컬렉션15개와 보조 인덱스 정의25개다. 25개는 요구 정의 수이며 전부 신규라는 뜻은 아니다. 직전 Atlas 조회와 적용 직전 실제 diff로 최종 생성 수를 결정한다.
3. Identity는 회전 요청 partial unique와 응답 복구 구조를 우선 준비하되, 이미지의 전체 mapped entity 인덱스도 비교해야 한다. SNS/탈퇴 기능 ON에 필요한 저장 구조를 복구3개만으로 대체하지 않는다.
4. guard 보정은 계정416개 전체나 accountType/epoch 일괄 수정이 아니다. 학습 기록의 유효한 owner 집합을 검증한 뒤 없는 guard만 추가하는 절차다.
5. 이번은 적용 계획과 로컬 검증만 완료했다. DB 변경/서버 중단/배포는 하지 않았다. 기존 스크립트의 writer-drain 조건을 무시하지 않는다.

## 2. 사용자가 반드시 읽어야 하는 내용

### 적용 단위

| 단계 | 변경 범위 | 승인·완료 조건 |
| --- | --- | --- |
| A: 사전 검사 | 실제 namespace/인덱스 옵션·중복·타입·owner 관계 조회, DB 백업/복구 수단 확인 | 읽기 전용. 집계만 출력 |
| B: DDL | 없는 컬렉션과 명시된 인덱스만 추가. 기존 인덱스 drop/rebuild·TTL 변경 금지 | 최종 DDL diff 승인. 기존 쓰기 유지 가능한 항목은 별도 검증한 additive 실행 경로 사용 |
| C: guard | owner별 `_id=userId`의 누락 guard만 `$setOnInsert` upsert | 정합성 검사 및 기존 준비 스크립트의 쓰기 정지 조건 충족/별도 승인 |
| D: 기동 | 신규 서비스 설정과 worker activation | 별도 배포 승인. DDL 승인이 cleanup/backfill/전파/푸시 발송 승인 아님 |

기존 일괄 prepare는 B와 C를 묶거나 apply에 writer-drain을 요구한다. B만 온라인으로 적용하려면 해당 범위로 분리된 실행 경로와 검증을 먼저 준비해야 한다. 지금의 스크립트를 그대로 실행하면서 `*_WRITERS_DRAINED=true`를 허위 설정하지 않는다.

### 기존 데이터 보존 원칙

- users, refresh_sessions, 시험/결과/요약 문서는 보정 대상에 포함하지 않는다. accountType 누락은 provider fallback, sessionEpoch 누락은 기존 epoch0 호환으로 처리한다.
- 기존 guard가 있으면 state/revision을 덮어쓰지 않는다. MERGED 및 예상하지 않은 상태는 자동으로 ACTIVE로 바꾸지 않는다.
- 이미 존재하는 인덱스가 같은 이름인데 정의가 다르면 중단한다. unique 충돌을 해결하려고 문서를 삭제하거나 unique/partial 조건을 약화하지 않는다.
- TTL은 승인된 만료/정리 메타데이터에만 둔다. 시험 결과·계정·학습 이력에 새 TTL을 추가하지 않는다. 기존 TTL 값도 바꾸지 않는다.
- 기존 Guest 탈퇴7건의 이벤트 backfill·Firebase 삭제는 이번 준비와 분리한다. 유효기간과 대상 확인 없이 실행하지 않는다.

## 3. 사용자가 결정해야 하는 사항

1. 최종 DDL diff가 준비되면 **없고 호환되는 항목 추가만** 승인할 것인지. 적용 전 중복/옵션 검증이 실패하면 변경하지 않는다.
2. guard 일괄 보정은 구 writer의 잠깐의 정지·진행 작업 소진을 승인받아 기존 runbook대로 실행할지, 별도 온라인 보정안을 구현·검증할지 결정이 필요하다. 이번에는 정지나 온라인 우회를 선택하지 않았다.
3. 학습 기록 삭제/문의 등 이번 출시에서 켤 부가 기능은 task 설정 확정에 맞춰 준비 범위를 추가한다. 테스트에서 켜져 있다는 이유로 기능과 worker를 운영에 복제하지 않는다.

## 4. 주요 위험과 미확인 사항

- [직전 실측](PRODUCTION_PRESTART_AUDIT_2026-10-08.md)은 주요 인덱스와 계정/세션 집계까지만 확인했다. 이 계획은 실제 DB dry-run 통과 보고서가 아니다.
- owner UUID/동시 활성 시험 중복/결과 orphan·owner mismatch, 챌린지 카탈로그 BSON int 및 unique 검증이 남았다. 검증 실패 시 별도 진단하며 자동 복구하지 않는다.
- 운영에서 신규 인덱스 생성도 CPU/IO/스토리지와 동시 요청에 영향을 줄 수 있다. 백업 완료·복구 가능성 및 자원 여유를 적용 전에 확인한다. 실패 시 이미 생성된 additive 항목은 기록하고 재조회하며 자동 drop하지 않는다.
- Identity auto-index=true 때문에 feature OFF만으로 모든 인덱스 생성 부작용이 사라진다고 가정하지 않는다. image 전체의 mapping-resolved 정의를 명시 DDL과 대조해야 한다. 자동 생성을 끌지는 배포 설정 결정이며 이번에는 변경하지 않았다.
- 구 callback 경합 수용은 유지한다. 그것이 guard 정합성 검사 생략, writer-drain 허위 확인 또는 구주소 세션/탈퇴 보안 위험 수용을 의미하지 않는다.

## 5. 현재 작업과 직접 관련된 설명

### Identity 우선 항목

| 대상 | 정의/처리 |
| --- | --- |
| refresh_sessions | `uk_refresh_user_rotation_request`: `{userId:1,rotationRequestKeyHash:1}`, unique=true, partial=`{rotationRequestKeyHash:{$type:'string'}}`. 기존4개 인덱스 보존 |
| refresh_reissue_responses | `_id_`, `uk_reissue_source` on sourceSessionId unique, `ttl_reissue_response` on cleanupAt TTL0 |
| user_session_controls | `_id_`; 전 계정 epoch 문서 일괄 삽입은 하지 않음 |
| SNS·가입·병합·탈퇴 저장 구조 | 부록A mapped entity별 원본 annotation 정의를 manifest로 대조. 아직 확인하지 않은 전체 인덱스 수를 확정했다고 보고하지 않음 |

정확한 정의는 [RefreshSession](../../src/main/java/web/tosunsaeng/identity/domain/auth/domain/entity/RefreshSession.java), [RefreshReissueResponse](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/domain/RefreshReissueResponse.java), [응답 복구 runbook](../contracts/refresh-token-response-recovery-stage-9-runbook.md)을 따른다.

### Learning Core 필수 묶음

| 기능 | 신규 컬렉션(직전 조회 대비) | 요구 보조 인덱스 | 원본 |
| --- | --- | ---: | --- |
| 병합 | user_ownership_guards, user_merged_inbox_events | 2: exam_results의 idx_exam_results_user, exam_summaries의 idx_exam_summaries_user, 모두 userId ASC/nonunique | [prepare](/Users/msde76/app-back-end-learning-core/scripts/mongodb/user-merged-prepare.js) |
| 탈퇴 | user_withdrawn_event_inbox, withdrawn_user_access_denies, user_withdrawn_transaction_probe | 2: cleanupAt TTL0의 ttl_user_withdrawn_inbox_cleanup, expireAt TTL0의 ttl_withdrawn_user_access_deny_expire | [validator](/Users/msde76/app-back-end-learning-core/src/main/java/web/tosunsaeng/domain/withdrawal/config/UserWithdrawnIndexValidator.java) |
| 알림 | notification_devices, exam_submission_receipts, daily_exam_reminder_deliveries, daily_reminder_suppressions, notification_control | 12; 공유 user_ownership_guards도 필요 | [전체12개 정의](/Users/msde76/app-back-end-learning-core/scripts/mongodb/notification-prepare.js) |
| 10초 챌린지 | challenge_10s_catalog_state, challenge_10s_attempts, challenge_10s_submit_receipts, challenge_10s_grading_jobs, challenge_10s_callback_receipts | 9; 기존 challenge_10s_questions의2개 unique 포함, TTL 없음 | [전체9개 정의](/Users/msde76/app-back-end-learning-core/scripts/mongodb/challenge-10s-prepare.js) |

추가로 `user_merged_transaction_probe` 컬렉션을 사전 생성해야 하는지 기동 probe와 사용 MongoDB 트랜잭션 제약에 따라 확인한다. 보수적 명시 준비안에는 이 빈 컬렉션을 더해 **신규16개 후보**로 잡는다. probe 실행은 insert/rollback을 포함하므로 읽기 점검에 포함하지 않는다. `_id_` 기본 인덱스는 위25개 수에 포함하지 않았다.

학습 기록 삭제 기능을 켠다면 [별도 준비 스크립트](/Users/msde76/app-back-end-learning-core/scripts/mongodb/learning-record-deletion-prepare.js)의24개 인덱스와 추가5개 컬렉션(`learning_record_deletion_operations/commands/targets`, `learning_record_billing_continuations`, `learning_activity_daily_aggregates`)까지 대조한다. 이는 실제 학습 기록 삭제 요청·worker 활성화와 다르다. 문의 기능은 [MongoSupportStore](../../src/main/java/web/tosunsaeng/identity/domain/support/MongoSupportStore.java)의 별도5개 컬렉션/7개 인덱스 준비를 조건부로 포함한다.

### 소유권 보호 데이터

- 기존 script 대상 owner는 LC exam_sessions/exam_results/exam_summaries의 distinct userId 합집합이다. Identity 전체416명과 같은 수라고 가정하지 않는다.
- canonical UUID, 활성 시험 중복, orphan result/summary, session과 child owner 일치, 기존 MERGED guard를 검사한다. 실패 시 apply 중단.
- 없는 guard만 `_id=userId, state=ACTIVE, revision=NumberLong(0), createdAt/updatedAt=실행시각`으로 삽입. 기존 기록 소유자를 변경하거나 계정을 활성화하는 작업이 아니다.
- 기존 스크립트는 500개 단위 ordered bulkWrite의 `$setOnInsert`를 사용한다. 실제 생성 수·기존 보존 수·남은 누락 수를 개인정보 없이 기록한다.
- [touchActive](/Users/msde76/app-back-end-learning-core/src/main/java/web/tosunsaeng/domain/usermerge/application/UserOwnershipGuardService.java)는 guard가 없으면 런타임 생성하고, [병합 transaction](/Users/msde76/app-back-end-learning-core/src/main/java/web/tosunsaeng/domain/usermerge/application/UserMergedTransactionService.java)은 markMerged 전에 source/target 양쪽 touchActive를 호출한다. 따라서 전416계정 선삽입이 항상 필수라는 설명은 하지 않는다. 단, 이 사실만으로 기존 보정/rollout 절차를 생략하지 않는다.

## 6. 부록 — Identity 전체 후보와 검증

### A. 현재 이미지의 @Document 후보(31개)

기존2개 `users`, `refresh_sessions`는 유지한다. 나머지29개는 신규 mapping 후보이며 인덱스 유무·기능 의존을 검토한다. 활성 기능만 보고 index resolver 대상에서 자동 제외된다고 가정하지 않는다.

- 세션: refresh_reissue_responses, user_session_controls, logout_all_operations.
- provider 변경: provider_unlink_operations, provider_link_attempts, provider_relink_attempts, auth_method_change_controls.
- 계정 복구: account_recovery_attempts, account_recovery_proofs, account_recovery_budgets.
- 가입/identity: social_identities, firebase_identities, firebase_enrollment_attempts, phone_identities, phone_fingerprint_aliases, phone_rejoin_lineages, abandoned_firebase_enrollment_cleanups.
- 병합/진행/전파: user_merged_outbox, user_merge_progress, merge_query_quotas, merge_query_cursors, owner_event_cores, owner_event_deliveries, owner_event_consumer_states.
- 탈퇴: user_withdrawal_lifecycles, user_withdrawn_outbox.
- 전화번호 자격: phone_eligibility_binding_revisions, phone_eligibility_binding_delivery_scopes, phone_eligibility_binding_outbox. 저장 구조와 결제/1회 무료 기능 활성화는 별개다.

원본은 `src/main/java/web/tosunsaeng/identity/domain`의 @Document/@Indexed/@CompoundIndex. 실제 실행 가능한 Identity 전체 DDL manifest는 추가 작성·검증 대상이다. 이 문서는 없는 정의를 추정해 자동 실행하는 스크립트가 아니다.

### B. 수행한 검증

- 2026-10-08 Identity develop HEAD57192f45 및 LC HEAD3fda35b 소스 기준.
- LC의 user-merged, notification, challenge-10s, learning-record-deletion prepare 테스트를 `node --test`로 실행: **27개 통과, 실패0**. Mock 기반이며 운영 MongoDB에 접속하지 않았다.
- counts는 각 prepare 모듈의 INDEXES/COLLECTIONS export를 로컬로 읽어 확인했다. 본문25개는 병합2+탈퇴2+알림12+챌린지9이며 실제 누락25개라는 뜻은 아니다.
- 운영 DB/인프라 재조회·쓰기·기동 없음. 직전 실측을 계획의 기준으로 사용했으며 실제 apply 직전 재조회 필수.
- 문서만 변경하여 Gradle 전체 테스트는 생략. git diff --check 통과. 기존 사용자 변경 보존, 예상 밖 코드 변경 없음.
