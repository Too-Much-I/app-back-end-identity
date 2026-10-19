# 신규 운영 설정표와 구·신 전환 점검

> 최신 실행 확인(2026-10-08 19:10 KST): Identity4/LC3/AI1/챌린지AI1 모두 ECS COMPLETED·실행1/대기0, 공개 Identity/LC health200. 탈퇴 네 플래그ON 및 `/internal/v1/events/withdrawn` 경로 적용 완료. 기존 대기 건 포함 승인 후 조회한 lifecycle/outbox는 배포 전후0건. 실제 탈퇴·전파 E2E는 미검증이며 Apple provider obligation 미구현에 따른 삭제 보류 가능성은 남아 있다. 아래는 준비 단계 이력이다.

> 최신 설정 결정: [운영 설정 확정안](PRODUCTION_SETTINGS_DECISION_2026-10-08.md) 및 [정확한 설정 명세](deployment/production-settings-2026-10-08.json). 운영 두 DB의 Secret host 일치 확인, 기존 JWT 참조 유지, 신 AI 큐/콜백·worker 실행 목표를 정리했다. AWS 적용/기동은 미실행이며 운영 Secret·권한·라우팅 준비는 남았다. 아래 공통 코드 교체 필수/DB 미조회 표기는 과거 분석이며, 현재는 구서버 교체를 자동 필수 조건으로 추가하지 않는다. 조건부 인증 위험은 미해결로 유지한다.

> 최신 상태: [추가전용 DDL 완료](PRODUCTION_DB_DDL_APPLIED_2026-10-08.md) 및 [인증·로컬 백업 점검](PRODUCTION_AUTH_BACKUP_REVIEW_2026-10-08.md). 기본 DB 구조 추가와 접근제한 로컬 논리 백업은 완료했지만 실제 restore/동일시점 복구 보장 및 구신 보안 호환은 미완료다. 사용자는 결과 설명만 선택하여 코드/기동 설정 변경은 하지 않았다. 아래 과거 DB 미준비 표기는 당시 기록이다.

> 운영 DB 실측 업데이트(2026-10-08): [기동 전 읽기 점검](PRODUCTION_PRESTART_AUDIT_2026-10-08.md)에서 Atlas 운영 컬렉션·주요 인덱스 및 계정/세션 집계를 확인했다. 신규 병합·탈퇴·알림 컬렉션, 결과/요약 owner 인덱스와 recovery unique 인덱스 등이 없다. 기존 계정416개(Guest ACTIVE409/WITHDRAWN7), 세션965개 중 미폐기·미만료/ACTIVE 소유229개 및 orphan0 확인. 아래 “DB 미조회”는 과거 시점 기록이며 이번 주요 조회로 갱신한다. 전체 predicate/TTL/DB endpoint·카탈로그·owner backfill 검증 및 실제 준비 적용은 여전히 미완료다. 테스트 설정 그대로 운영 기동하지 않는다.

> TLS 완료(2026-10-08): 사용자 명시 승인으로 두 prod 호스트를 포함하는 ACM 인증서 `3064fa95-591a-4cdc-94e0-a7d540a65d53`를 서울 리전에 발급하고 가비아 검증 CNAME2개(TTL600)를 추가했다. 기존18개 DNS와 ALB 기본/추가 인증서를 보존하면서 새 인증서를 HTTPS443 추가 SNI로 연결했다. ACM ISSUED/두 도메인 SUCCESS, 권한 DNS 응답, 두 호스트의 직접 ALB TLS 검증(ssl_verify_result=0)을 확인했다. 응답404는 아직 prod 라우팅이 없기 때문이며 서비스 정상 기동을 의미하지 않는다. 검증 CNAME은 자동갱신용으로 유지한다. **서비스용 DNS·라우팅·신규 ECS 서비스·DB 준비는 미완료**이며 아래 착수 시점의 TLS 미준비 기록만 이번 결과로 대체한다.

> 구성 착수 결과(2026-10-08): 사용자가 `identity-prod.to-teacher.com` / `api-prod.to-teacher.com`을 확정하고 AI도 별도 운영 서비스로 분리 승인. 공개 NS는 가비아이며 AWS Route53 hosted zone 목록은 비어 있다. 기존 ALB의 기본·추가 SNI 인증서는 staging/test만 커버하여 prod용 TLS 준비가 필요하다. app-ai-learning은 10초 챌린지 전용이며 모의고사 eval_agent와 별도이므로 신규 구성은 Identity·LC·모의고사 AI·챌린지 AI의 4서비스 기준으로 준비한다. 실제 생성/기동/권한/라우팅 변경은 아직 없으며 DNS 접근과 운영 DB 기동 전 검증이 남아 있다.

> 최신 확인(2026-10-08): 서울 리전 ECS를 다시 읽었으며 클러스터1개/서비스6개(구운영 Identity23·LC23·AI17, 테스트 Identity34·LC19·AI2), 각 running1이다. 별도 신규 운영 서비스는 아직 없다. 아래의 구 callback ownership 경합 보완은 사용자가 위험을 수용하여 이번 범위에서 제외했다. 이 수용은 세션 재발급·탈퇴 접근 차단 위험까지 포함하지 않는다.
>
> 프론트 최신 main6495940은 v1 세션 읽기→v2 보존, 재발급 Idempotency-Key, 요청 전 ID 영속화와 절대 만료 처리 구현을 확인했다. 실제 출시 빌드/환경은 미확정. dev에는 무헤더 옛 경로가 있으므로 브랜치만 보고 동일하다고 판단하지 않는다. 운영은 Identity DB `to-teacher-identity`, LC DB `to-teacher-app`, issuer `https://identity-staging.to-teacher.com`; 테스트는 별도 DB와 test issuer다. 테스트 LC의 `AI_SERVER_URL=http://127.0.0.1:9`도 재확인했으므로 운영 복사 금지. 실제 DB 문서/인덱스와 EAS 환경값, 키 재대조, 앱 E2E는 이번에 검증하지 않았다.

> 전환 정책 확정(2026-10-08): 유예 중 구앱 시험 응시·제출·채점은 유지하고 피드백 열람만 업데이트 안내로 제한한다. 업데이트 후 기존 시험 결과를 이력에서 확인할 수 있어야 한다. 안내-only 또는 시험/refresh 전면 차단은 채택하지 않는다. 구·신 writer 최소 호환, 동일 examId/소유권 및 업데이트 중 도착하는 callback의 결과 보존을 필수 검증한다. 이는 아직 구현·배포 완료를 의미하지 않는다.

## 1. 5줄 결론

1. **현재 구 이미지 그대로 공유 DB 병행 운영은 보류**다. 구 Identity는 신규 세션 복구/병합 enum을 모르고 구 LC는 병합·탈퇴 deny/ownership guard를 구현하지 않았다(부록 소스 비교).
2. 기존 계정·기록 유지에는 운영 Identity/LC 데이터의 연속성이 필요하다. 테스트 DB를 복사해 운영 계정 승계가 되지는 않는다.
3. 신규 호스트와 JWT issuer는 별개다. 구·신 운영 issuer 및 공개키 신뢰를 일관되게 유지하는 안을 우선 검토한다([JWT 계약](../contracts/identity-learning-jwt.md)). 실제 키는 이 문서에 기록하지 않는다.
4. 테스트 Identity34/LC19는 안정화 완료. 버전은 Android/iOS 최소·최신 모두1.0.0, 알림 추적/API ON·발송 OFF/dry-run ON. 모의고사 AI localhost9 설정은 운영 복사 금지.
5. **운영 DB 내용/인덱스 검증은 미완료**다. Atlas 탭은 다른 작업에서 사용 중이고 CloudShell에는 Mongo 드라이버가 없다. 운영 데이터에 접근한 것처럼 표시하지 않는다.

## 2. 사용자가 반드시 읽어야 하는 내용

### 2.1 지금 가장 먼저 해결할 것은 구·신 코드 혼용

- 구 Identity image7f2188d: `/reissue`는 ROTATED 토큰을 발견하면 해당 사용자의 활성 세션을 폐기한다. 신서버 복구 방식의 동일 요청 재전달을 인식하지 않는다. 신규 UserStatus.MERGED와 RevocationReason.GUEST_MERGED도 구 enum에 없다. 공유 문서 읽기에서 역직렬화 오류 가능성이 있다(실제 운영 재현 미실행).
- 구 LC image89defb7: JWT 서명/issuer/audience/sub 검증은 있지만 withdrawal deny/merge source deny·owner guard가 없다. 같은 issuer/key를 신뢰하는 동안 아직 유효한 기존 토큰으로 구주소를 호출하면 신규 보안 차단을 우회할 위험이 있다. 동시에 구 writer가 병합 이후 데이터에 쓰는 경합도 남는다.
- 이는 구서버의 **알림 제출 추적 생략을 허용한 결정과 별개**다. 일일 알림 오발송 허용을 계정 보안 경계 완화로 해석하지 않는다.

### 2.2 권장 전환 방식(아직 구현/승인 아님)

1. 기존 계정·세션·기록의 원본 운영 DB를 유지하는 방향으로 설계한다. 별도 DB 복사 후 양쪽 쓰기는 동기화 설계 없이는 채택하지 않는다.
2. 구주소를 유지하되 먼저 호환 패치를 배포해 **모든 운영 Identity session writer와 LC ownership writer가 새 상태·보안 경계를 인식**하도록 한다. 방법은 최소 backport 또는 구앱 호환 모드의 신코드 서비스로 구주소를 라우팅하는 방안이며 영향 검증 후 선택한다.
3. 구앱이 Idempotency-Key를 제공하지 못하는 상태에서 recovery ON endpoint로 단순 라우팅하지 않는다. 구앱 계약 지원과 동일 세션의 혼합 writer 안전성이 검증된 설계를 먼저 확정한다. 기능 OFF만으로 신 메타데이터를 모르는 구 binary가 안전해지지 않는다.
4. 위 준비 전에는 공유 운영 DB에서 신규 병합/탈퇴 기능을 켜지 않는다. 기능 보류는 출시 완료가 아니라 안전 준비 단계다.
5. 인덱스/보안 검증 → 신규 운영 배포 → 서버간 검증 → 스토어 공개 → 구서버 안내 → 약1주 유예·진행 작업 소진 후 구주소 종료. 자동 rollback으로 구 binary를 다시 writer로 투입하지 않는다.

## 3. 사용자가 결정해야 하는 사항

- 사용자 주소 확정: https://identity-prod.to-teacher.com 및 https://api-prod.to-teacher.com (2026-10-08 명시 승인). TLS 발급·검증 CNAME·ALB 추가 인증서 연결 완료. 서비스용 DNS/라우팅은 아직 생성하지 않았다. ECS service 이름은 미정, 기존/test 서비스와 별도 이름·ALB rule/target group 필요.
- 구주소 호환 방식: 최소 패치 또는 공통 신코드의 구앱 호환 라우팅. 무중단 여부/짧은 전환 정지 허용 범위를 함께 결정.
- 운영 DB는 기존 원본 유지 구성으로 진행 요청을 받음. 운영 인덱스/guard backfill·기동 시 자동 처리 영향은 별도 검증·승인이 필요하며 아직 데이터 변경 없음.
- AI 별도 운영 서비스 분리 승인. 구LC→구AI→구LC callback, 신LC→신AI→신LC callback로 각각 고정. 챌린지 app-ai-learning은 모의고사와 별도 프로그램이므로 두 AI 서비스의 queue namespace·callback·credential과 스토리지를 각각 확인한다.
- 출시 앱 버전: 계획1.1.0은 스토어와 호환성 확인 후 확정. 테스트1.0.0을 운영 최소선으로 자동 채택하지 않는다.
- 알림 실기기 검증은 프론트 담당. 서버 FCM 자격증명/권한·대상 제한·발송 활성화 책임은 별도 확인하며 이번에는 변경하지 않음.

## 4. 주요 위험과 미확인 사항

| 항목 | 판정/근거 | 남은 조치 |
| --- | --- | --- |
| 구 Identity mixed writer | 소스에서 비호환 확인 | recovery 계약/enum/epoch/쓰기 경계를 공통 지원한 후 경합 테스트 |
| 구 LC 계정 차단 | 구 SecurityConfig에 deny 없음 | 양 주소 차단·owner guard·writer 호환 적용/검증 |
| Refresh 기본 해시 | 두 버전 소스 동일(SHA-256/base64url no padding) | 실제 원본 DB/유효 세션/epoch/만료 일치 확인 |
| 기존 accountType 누락 | 신 User.getAccountType() provider fallback 있음 | 운영 provider/status 분포 및 비정상 값 집계 |
| Mongo 실제 DB 연결·인덱스 | task DB명만 확인, 원문 URI 미조회 | 동일 cluster/namespace 여부, unique/partial/TTL/transaction 지원 읽기 검증 |
| 앱 세션 보존 | 과거 검토만 존재 | 프론트 신규 빌드 저장소·일반 업데이트/응답 유실 검증 |
| JWT | 구·test issuer 다르고 공개키 겹침 없음 | 신규 운영 발급자/키 사전 신뢰 정책 확정 |
| AI | 테스트 시험 AI localhost9 | 접근 가능한 운영 endpoint와 callback 인증·복구 E2E |
| 이미지 digest | task definition의 이미지 확인 | 최종 승격 시 실행 task digest·릴리스 SHA 추가 대조 |

운영 DB 문서나 인덱스를 이번에 검사하지 않았다. 빈 테스트 DB/테스트 가입 결과로 운영 호환성을 대신하지 않는다. 다른 작업의 테스트 계정 초기화도 운영 데이터 승계 완료 증거가 아니다.

## 5. 현재 작업과 직접 관련된 설명 — 신규 운영 설정표

`<신LC>` 등은 미정 값을 표시하는 자리표시자이며 배포 가능한 실제 값이 아니다. Secret 원문·실제 Key·전체 MongoDB URI를 포함하지 않는다.

### Identity

| 설정 | 신규 운영 제안/조건 |
| --- | --- |
| MONGODB_DATABASE | 기존 to-teacher-identity 연속성 유지 제안. 모든 writer 호환 전 연결/기능 활성화 금지 |
| MONGODB_URI | 운영 Secret 참조. test Secret 재사용 금지, 실제 cluster 일치 확인 |
| JWT_ISSUER | 기존 https://identity-staging.to-teacher.com 유지안 우선 검토. 호스트 신설만으로 변경하지 않음 |
| JWT_AUDIENCE | tosunsaeng-learning-core 유지. Billing 추가 claim은 코드 계약 유지 |
| JWT_ACCESS_TOKEN_TTL | 기존 task PT30M 확인. 새 운영 동일 값 제안, verifier skew/deny 보관 경계 별도 대조 |
| JWT_PRIVATE_KEY_LOCATION / JWT_PUBLIC_KEY_LOCATION / JWT_KEY_ID | 운영 관리체계 유지/승계, 값은 비공개. 신키 전환 시 공개키 사전 게시·구키 TTL 보존 |
| JWT_PREVIOUS_KEY_IDS / JWT_PREVIOUS_PUBLIC_KEY_LOCATIONS | rollover 시 필요. 구주소 JWKS 종료 전에 소비자 경로/캐시 검증 |
| AUTH_SESSION_FENCE_ENABLED | 신규 보안 경계 적용. 모든 운영 session writer 호환 조건 필요 |
| AUTH_REISSUE_RECOVERY_ENABLED | 앱 헤더 계약·인덱스·공통 writer 호환 검증 후 ON. test true를 무조건 복사하지 않음 |
| AUTH_REISSUE_ENCRYPTION_* | 운영 전용 keyring·environment. 동일 응답을 처리할 운영 인스턴스 간 decrypt 호환 필요 |
| FIREBASE_AUTH_ENABLED 및 provider별 *_ENABLED | 출시 SNS 범위에 맞게, Firebase 실제 프로젝트/tenant 및 자격증명 확인 |
| FIREBASE_WITHDRAWAL_ENABLED / CLEANUP_ENABLED / IDENTITY_RELEASE_ENABLED | 내부/외부 삭제 및 양 LC 주소 차단 검증 후 활성화. 테스트 IAM 성공을 운영 계정에 적용됐다고 가정 금지 |
| GUEST_MERGE_ENABLED | 운영 LC guard/index/backfill 및 구 writer 호환 검증 이후 활성화 |
| OWNER_EVENT_LEARNING_CORE_ENDPOINT | <신LC>/internal/v1/events/user-merged. 기존 fan-out과 병행할 경우 단일 처리/멱등 구조 명확화 |
| OWNER_EVENT_LEARNING_CORE_USER_MERGED_PUBLISHER_ENABLED | 새 consumer 인증·저장 준비 후 ON. 기존 대기 outbox 처리 범위 확인 |
| USER_WITHDRAWN_PUBLISHER_ENDPOINT | <신LC>/internal/v1/events/withdrawn |
| USER_WITHDRAWN_PUBLISHER_ENABLED | 양쪽 접근 차단 구조 검증 후 ON. 공유 DB consumer 하나만으로 구 binary deny가 생기지 않음 |
| WORKLOAD_JWT_* | 운영 issuer, sub identity-service, audience는 이벤트별 고정 계약 |
| APP_ANDROID_MINIMUM_VERSION / APP_IOS_MINIMUM_VERSION | 실제 지원 중단선 결정. 스토어 설치 가능 전 강제선 상승 금지 |
| APP_ANDROID_LATEST_VERSION / APP_IOS_LATEST_VERSION | 실제 플랫폼별 출시 버전, minimum과 별도 관리 |

### Learning Core와 AI

| 설정 | 신규 운영 제안/조건 |
| --- | --- |
| MONGODB_DATABASE | 기존 to-teacher-app 유지 제안, schema/guard/동시 writer 검증 선행 |
| IDENTITY_ISSUER / IDENTITY_JWK_SET_URI | 선택한 운영 issuer/운영 공개키 집합. test issuer/JWKS 복사 금지 |
| IDENTITY_AUDIENCE | tosunsaeng-learning-core |
| USER_MERGED_WRITER_ENABLED / CONSUMER_ENABLED / SOURCE_DENY_ENABLED | 운영 guard/index와 구 writer 호환 이후 ON |
| USER_MERGED_WORKLOAD_ISSUER / JWK_SET_URI | 운영 workload 신뢰. merge audience learning-core-user-merged |
| USER_WITHDRAWN_CONSUMER_ENABLED / DENY_GATE_ENABLED | 운영 inbox/deny index와 TTL 경계 검증 후 ON, 구주소에도 동일 차단 필요 |
| USER_WITHDRAWN_WORKLOAD_* | 운영 issuer/JWKS, audience learning-core-user-withdrawn, principal identity-service |
| USER_WITHDRAWN_MAX_ACCESS_TOKEN_LIFETIME / ALLOWED_VERIFIER_CLOCK_SKEW / INBOX_RETENTION 등 | 실제 발급 최대 TTL·검증 skew와 이벤트 계약에 맞춤. test 수치 맹목 복사 금지 |
| AI_SERVER_URL | 실제 시험 AI 주소. localhost9 금지. 기존 진행 job/callback 처리 주체 분리 |
| CHALLENGE_ENABLED / CHALLENGE_AI_ENDPOINT | catalog·index·고정 contentBaseDate 및 신LC callback/인증 준비 후 ON |
| AWS_S3_BUCKET_NAME | 기존 음성 접근을 유지할 운영 버킷/권한 검증. test 버킷 사용 금지 |
| Redis 설정 | 기존 job/lock/TTL 충돌·승계 점검. DB번호만 바꾸어 중복 실행 위험을 해결했다고 간주 금지 |
| APP_UPDATE_REQUIRED | 신false. 구는 스토어 공개 후true, 실제 필드 result.appUpdateRequired |
| BILLING_CREATION_SAGA_ENABLED / PHONE_CONTINUATION_ENABLED / RECONCILIATION_ENABLED | 1차 결제·전화번호당1회 후속 분리 정책에 따라 OFF, 무료 시험 E2E 별도 |
| app.notifications.api-enabled / tracking-enabled | 운영 인덱스/기기 인증/제출 transaction 준비 후 ON |
| app.notifications.tracking-ready-at | 신서버 제출 추적 준비 시각. 구서버 종료나 다음 날 대기 불필요 |
| app.notifications.sending-enabled / dry-run | 최초 OFF/true, 대상·FCM·프론트 검증 이후 별도 승인해 ON/false |
| app.notifications.project-id / credential-file | 운영 FCM 발송 권한. Firebase 사용자 삭제 권한과 다른 권한 |

### 운영 DB 읽기 체크리스트(미실행)

- Identity: users provider/accountType/status 집계, refresh_sessions 유효/폐기/ROTATED 분포 및 orphan userId 집계, user_session_controls epoch 존재·일관성. 토큰/해시/개인정보 원문 출력 금지.
- Identity: 기존 hash unique/expiry TTL, uk_refresh_user_rotation_request의 BSON-string partial predicate, refresh_reissue_responses source unique/cleanup TTL, logout/withdrawal/merge index를 실제 정의와 비교. [응답 복구 runbook](../contracts/refresh-token-response-recovery-stage-9-runbook.md)의 정확한 predicate 유지.
- LC: exam_sessions/results/summaries userId 참조·orphan 집계, ACTIVE ownership guard backfill 준비, user-merged inbox와 withdrawal inbox/deny 및 notification 12개 index 비교. 조회는 집계/metadata 위주, 학습 내용/FCM token 출력 금지.
- 모든 실제 migration/backfill/unique index 추가는 중복 데이터 보고·백업/복구·승인 후 별도 실행. readiness를 위해 startup validator를 운영에 먼저 실행하지 않음(기동 시 쓰기 가능).

## 6. 부록 — 이번 조사 근거와 전체 상태

### 6.1 2026-10-08 실시간 ECS 결과

| 서비스 | task | 이미지 참조 | 상태 |
| --- | --- | --- | --- |
| 기존 Identity | identity23 | 7f2188df | PRIMARY COMPLETED, running1/pending0 |
| 기존 LC | learning-core23 | 89defb7b | PRIMARY COMPLETED, running1/pending0 |
| 테스트 Identity | identity-test34 | 57192f45 | PRIMARY COMPLETED, running1/pending0 |
| 테스트 LC | learning-core-test19 | sha256:85931c7db9eeb3a5d9c24fe256db80f90a1ef28247329ec49037c57e44e838b4 | PRIMARY COMPLETED, running1/pending0 |

서울 대상 cluster는 기존/test Identity·LC 및 AI2개의 6서비스. 이 목록에서 별도 신규 운영 서비스를 식별하지 못했다. 다른 cluster/region 전체를 조사한 것은 아니다.

기존 DB env는 to-teacher-identity/to-teacher-app, 테스트는 to-teacher-identity-test/to-teacher-learning-core-test. 기존 JWT issuer는 identity-staging, 테스트는 identity-test. 공개 JWKS는 각1개, RSA 공개키 교집합 없음(실제 Key 원문 출력/기록 없음). 양 Identity task의 TTL env PT30M. 모든 Spring 외부 override를 덤프한 것은 아님.

공개 테스트 버전 API android/ios 모두 최소1.0.0/최신1.0.0 확인. 테스트 LC19 AI_SERVER_URL=http://127.0.0.1:9, challenge AI는 ai-test, S3는 test bucket, Redis DB2. 알림 JSON의 api/tracking=true, sending=false/dry-run=true 확인. 이번 점검에서 변경 없음.

### 6.2 소스 대조

- Identity git show7f2188d의 TokenReissueService, RefreshTokenHasher, UserStatus, RevocationReason. 해시는 현행과 diff없음, old enum에는 MERGED/GUEST_MERGED 없음.
- [현행 User fallback](../../src/main/java/web/tosunsaeng/identity/domain/user/domain/entity/User.java), [세션 복구](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/ReissueRecoveryService.java), [RefreshSession 인덱스](../../src/main/java/web/tosunsaeng/identity/domain/auth/domain/entity/RefreshSession.java).
- LC git show89defb7의 SecurityConfig 및 application.yml, 해당 tree에 usermerge/withdrawal 구현 없음. 신규3fda35b의 lifecycle gate와 비교. 실제 공격성 요청/사용자 토큰 재발급 미실행.
- [이전 전환 검토](../contracts/guest-app-update-transition-review.md), [응답 복구 runbook](../contracts/refresh-token-response-recovery-stage-9-runbook.md), [JWT 계약](../contracts/identity-learning-jwt.md). 문서의 과거 미배포 표기는 최신 실측과 구분.

### 6.3 접근·검증 한계

로컬 AWS CLI는 자격증명 없음으로 조회 실패. 기존 로그인된 CloudShell 재연결 후 boto3 읽기 조회 및 공개 HTTP API로 대체. Secret Manager 원문/비밀키/DB URI 조회 없음. 운영 Atlas 탭은 다른 작업이 점유하여 조작하지 않음. CloudShell pymongo 미설치로 직접 DB 연결도 실행하지 않음. 따라서 실제 DB index/data 체크는 남은 작업으로 명시한다. 제품 코드 변경 없어 Gradle 미실행, 문서 diff 공백 검사만 수행한다.
