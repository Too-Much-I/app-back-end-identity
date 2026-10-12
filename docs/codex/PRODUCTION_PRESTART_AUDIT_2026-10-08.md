# 신규 운영 기동 전 읽기 점검 — 2026-10-08

## 1. 5줄 결론

1. **현재 운영 DB에 신이미지의 기능을 모두 켜서 바로 기동할 준비는 안 됐다.** Atlas에서 병합·탈퇴·알림용 컬렉션과 일부 필수 인덱스 부재를 실제 확인했다.
2. 기존 Identity 계정416개는 모두 provider=GUEST이며 ACTIVE409/WITHDRAWN7이다. 세션965개 중 미폐기·미만료/ACTIVE 소유 세션229개, 소유 계정 미발견 집계0개다.
3. accountType와 sessionEpoch 누락은 현행 코드의 Guest/epoch0 호환 대상이다. 누락만으로 일괄 보정하지 않는다([User](../../src/main/java/web/tosunsaeng/identity/domain/user/domain/entity/User.java), [SessionSecurityService](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/SessionSecurityService.java)).
4. 운영과 테스트는 DB/issuer/공개키가 다르다. 테스트 **이미지**를 승격하되 설정·키·DB 연결을 복사하지 않는다.
5. 다음은 누락 DDL과 guard 준비 범위를 확정하고 승인 후 적용하는 단계다. 이번에는 조회만 했으며 기동·DDL·backfill·권한 변경은 하지 않았다.

## 2. 사용자가 반드시 읽어야 하는 내용

- Identity의 `spring.data.mongodb.auto-index-creation=true`는 새 서버 기동 자체로 인덱스를 생성할 수 있다는 뜻이다. DB readiness 확인을 위해 서버를 먼저 띄우지 않았다([설정](../../src/main/resources/application.yml)).
- 신규 LC의 병합 validator는 `user_ownership_guards`, `user_merged_inbox_events`의 `_id_` 및 결과/요약의 userId 인덱스를 요구한다. 현재 모두 해당 신규 준비가 없다.
- 알림은 발송 OFF여도 API/추적 ON이면 startup validator가 실행되어 6개 컬렉션과 12개 인덱스가 필요하다. 실제 운영에는 알림 전용5개 컬렉션과 공유 guard가 없고, exam_sessions의 `notification_submission_date`도 없다.
- 테스트 Identity에는 탈퇴 cleanup/release, 이벤트 전파, Slack 문의 worker까지 ON이다. 테스트 LC에는 학습기록 삭제 worker도 ON이다. 운영에 그대로 복사하면 조회 서버가 아니라 실제 처리 서버가 된다.
- 탈퇴된 Guest7개가 있다는 사실만으로 Firebase 삭제나 전파 backfill을 실행하지 않는다. 기존 요청 시점/Access Token 수명/이벤트 계약에 따라 적용 대상과 필요성을 별도 결정한다.

## 3. 사용자가 결정해야 하는 사항

- 누락 컬렉션·인덱스의 **추가만** 수행하는 DDL 승인 범위와 guard backfill의 별도 승인 범위. 데이터 삭제/초기화는 필요하다고 판단하지 않았다.
- 기존 writer를 유지하면서 적용 가능한 준비와 짧은 쓰기 정지가 필요한 준비를 구분할 것. 기존 `user-merged-prepare.js`는 apply에 `USER_MERGED_LEGACY_WRITERS_DRAINED=true`를 요구한다. 사용자 경합 수용을 이유로 실제 중단하지 않은 writer를 중단했다고 표시하지 않는다.
- 운영 기동 시 worker별 활성화 및 기존 대기 건 처리 범위. 테스트의 ON 값을 기본 승인으로 해석하지 않는다.

## 4. 주요 위험과 미확인 사항

- 운영 두 DB는 Project 0/Cluster0에서 확인했다. ECS DB명과 일치하지만 Secret 원문의 연결 host는 조회하지 않았으므로 실제 task→cluster endpoint 일치까지 증명하지 않았다.
- 이번은 주요 메타데이터·계정/세션 집계 확인이다. 모든 인덱스의 TTL 초/partial predicate/collation 원문 대조, 시험 owner orphan/중복 UUID, 챌린지 카탈로그 BSON int 검증은 남았다.
- 읽기 중에도 운영 데이터는 변할 수 있다. 건수는 조회 시점 값이며, 과거 화면의 rounded Document 수와 정확한 aggregate를 구분한다.
- 구주소 Refresh 혼용·탈퇴 후 접근 차단 위험은 기존 조사대로 미해결. 수용한 구 callback/병합의 좁은 metadata 경합과 구분한다. 이 이유만으로 구서버 전면 교체를 확정하지 않는다.
- 신규 서비스 미생성. 운영 전용 Secret 구성/IAM/Firebase 권한, AI queue/callback 분리, 실제 앱 업데이트 E2E 및 최종 readiness 검증 미완료.

## 5. 이번 작업과 직접 관련된 설명

### 안전한 다음 순서

1. 아래 누락 내역을 배포할 이미지의 DDL/validator와 대조하여 생성 목록을 확정한다. 기존 데이터 중복/타입 검증을 추가한다.
2. 기존 writer 가동 조건과 backup/복구 방안을 확인하고, 없는 항목 추가 및 필요한 guard 준비를 승인받는다.
3. 운영 DB·기존 JWT 신뢰·신규 AI/queue/callback을 명시한 task 설정을 준비한다. worker는 기능 의존 조건을 지키며 승인된 범위로만 켠다.
4. 신규 서비스 기동과 health 후 DNS/라우팅 및 E2E 검증. 스토어 공개 전 구 APP_UPDATE_REQUIRED 변경 없음.

### 기동 부작용 구분

| 구현 | 조건 및 실제 동작 | 이번 실행 |
| --- | --- | --- |
| Identity MongoTransactionCapabilityVerifier | !test에서 hello topology/session 지원 조회 | 미실행, 소스 확인 |
| Identity 자동 인덱스 | 기본 true, mapped entity 인덱스 생성 가능 | 미실행 |
| SupportConfiguration | 문의 API 또는 worker ON이면 ensureIndexes; worker ON이면 Slack 전송 작업 | 미실행 |
| FirebaseAbandonedEnrollmentCaptureRunner | capture-enabled일 때 bounded 검사/capture, dry-run에 따라 DB 기록 | 미실행 |
| UserWithdrawnBackfillConfiguration | enabled일 때 기간 한정 과거 탈퇴 outbox 보정, dry-run 별도 | 미실행 |
| 탈퇴 cleanup/release 및 이벤트 scheduler | 활성화 시 기본5초 주기, 기존 대상 처리·발행 완료 기록 정리 가능 | 미실행 |
| LC UserMerged/UserWithdrawn validator | 운영에서 필수 기존 인덱스 검사, 누락 시 fail-closed | 미실행 |
| LC merge/withdrawal transaction probe | 조건부 startup에서 canary insert 후 rollback, 순수 조회는 아님 | 미실행 |
| LC NotificationStartupValidator | API/추적/발송 중 하나 ON이면 컬렉션·인덱스 검사 및 insert/rollback probe | 미실행 |

## 6. 부록 — 실제 조회 결과와 근거

### A. AWS/JWKS 읽기 결과

- 서울 리전 기존 클러스터6서비스: old Identity23/LC23/시험AI17, test Identity34/LC19/챌린지AI2. 각 running1/pending0, 신규 prod 서비스 없음.
- 운영 DB명 `to-teacher-identity` / `to-teacher-app`, issuer `https://identity-staging.to-teacher.com`, audience `tosunsaeng-learning-core`. 기존 LC의 JWKS도 staging 주소. 구 APP_UPDATE_REQUIRED=false.
- 테스트 DB명 `to-teacher-identity-test` / `to-teacher-learning-core-test`, issuer identity-test. 테스트 LC 시험AI localhost:9 재확인.
- 공개 JWKS 양쪽 조회 성공, 각 키1개, RSA 공개키 material 교집합 없음. 실제 Key 값은 출력/기록하지 않았다.
- 테스트 Identity: withdrawal/cleanup/release, reissue recovery/fence, guest merge, user-withdrawn publisher, user-merged publisher, owner-event LC publisher, support worker ON. 운영 설정으로 자동 채택하지 않는다.
- 테스트 LC: merge/withdrawal gates, 학습기록 삭제 worker ON, billing saga/reconciliation OFF. SPRING_APPLICATION_JSON 있음(이번 내용 전체 덤프하지 않음), 외부 environmentFiles0. 기타 자동 작업의 전체 유효설정을 확인한 것은 아님.

### B. Atlas 컬렉션과 인덱스

| DB/대상 | 실제 관측 | 판단 |
| --- | --- | --- |
| Identity 컬렉션 | users, refresh_sessions만 있음 | 신규 recovery/session-control/outbox 등의 컬렉션 없음 |
| refresh_sessions | `_id_`, `uk_refresh_sessions_token_hash`(tokenHash ASC, unique), `ttl_refresh_sessions_expires_at`(expiresAt ASC, TTL), `ix_refresh_sessions_user_id_revoked_at`(userId/revokedAt ASC) | `uk_refresh_user_rotation_request` 없음. TTL 초는 화면에서 미확인 |
| 앱 DB 컬렉션 | azure_results, challenge_10s_questions, exam_results, exam_sessions, exam_summaries, mock_exams, model_answer, question_grading_jobs, speechace_results, summary_grading_jobs | 총10개, 병합·탈퇴·알림/삭제·챌린지 시도용 신규 컬렉션 없음 |
| exam_results | `_id_`, `idx_exam_results_exam_question_retry`만 있음 | `idx_exam_results_user` 없음 |
| exam_summaries | `_id_`, `idx_exam_summaries_exam_id_latest`만 있음 | `idx_exam_summaries_user` 없음 |
| exam_sessions | `_id_`, `idx_exam_sessions_user_completed_desc`, `uniq_exam_sessions_active_user`(userId ASC, unique/partial), `idx_exam_sessions_user_completed_mock_exam`(userId/completedAt/mockExamId ASC) | 기존 시험 배정 인덱스 존재. partial 원문 미확인. notification_submission_date 없음 |

Identity recovery unique predicate의 정확한 요구는 [RefreshSession](../../src/main/java/web/tosunsaeng/identity/domain/auth/domain/entity/RefreshSession.java)과 [runbook](../contracts/refresh-token-response-recovery-stage-9-runbook.md)을 따른다. `rotationRequestKeyHash` BSON string partial 조건을 임의 변경하지 않는다.

### C. 개인정보 없는 집계

users는 provider/accountType/status별 `$group`으로 확인했다. accountType MISSING은 `$ifNull`로 null 또는 필드 부재를 합친 값이다.

| provider | accountType | status | 건수 |
| --- | --- | --- | ---: |
| GUEST | MISSING | ACTIVE | 409 |
| GUEST | MISSING | WITHDRAWN | 7 |

refresh_sessions는 userId→users._id lookup 후 아래 상태만 group했다. expiresAt을 서버의 `$$NOW`와 비교했다. epoch MISSING도 null/필드 부재를 합친 값이다. 문서 원문·식별자·토큰·해시는 출력하지 않았다.

| reason | revokedAt 있음 | 만료 | owner status | epoch | 건수 |
| --- | --- | --- | --- | --- | ---: |
| ROTATED | true | false | ACTIVE | MISSING | 732 |
| NONE | false | false | ACTIVE | MISSING | 229 |
| ROTATED | true | false | WITHDRAWN | MISSING | 3 |
| ACCOUNT_WITHDRAWN | true | false | WITHDRAWN | MISSING | 1 |

합계965, ownerStatus MISSING 집계0. 신규 서버 재발급 요청을 실제로 실행하거나 현재 사용자 세션을 회전하지 않았다.

### D. 코드 근거와 검증 한계

- Identity HEAD57192f45, LC HEAD3fda35b, 이번 제품 코드 수정 없음.
- LC 근거: `domain/usermerge/config/UserMergedIndexValidator.java`, `UserMergedTransactionCapabilityProbe.java`, `domain/withdrawal/config/UserWithdrawnIndexValidator.java`, `UserWithdrawnTransactionCapabilityProbe.java`, `domain/notification/NotificationStartupValidator.java` 및 `scripts/mongodb/*-prepare.js`.
- 기존 prepare script의 생성 목록/검증 일부를 읽었으며 운영에 실행하지 않았다. dry-run 성공이나 전체 준비 완료라고 보고하지 않는다.
- AWS describe/list, 공개 JWKS 비교, Atlas 컬렉션/인덱스와 집계, 로컬 소스 대조 수행. 코드 변경 없어 Gradle 생략. 문서 `git diff --check` 수행.
