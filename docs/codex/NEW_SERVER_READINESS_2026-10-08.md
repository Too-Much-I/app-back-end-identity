# 신규 앱 서버 출시 준비 점검 — 2026-10-08

## 1. 5줄 결론

1. **현재 테스트 이미지는 배포 후보로 식별했지만, 테스트 설정 그대로의 신규 운영 출시는 NO-GO다.**
2. AWS에서 Identity test:29와 Learning Core test:17은 각각 실행1/보류0, 배포 성공, ALB 정상1을 확인했다. 이는 기능 E2E 통과와 다르다.
3. Identity 최신 버전 API는 Android/iOS 모두 실제 HTTP503 APP_VERSION_UNAVAILABLE다. [버전 설정 계약](../contracts/app-version-api.md#설정과-갱신)을 적용해야 한다.
4. Learning Core는 테스트 DB·S3·Identity·AI를 참조하며 모의고사 AI 주소는 로컬127.0.0.1:9다. 기존 사용자/기록 유지 및 실제 AI 연동을 준비해야 한다.
5. 신규 두 서비스의 주소·운영 설정·데이터 전환안 확정과 가입→챌린지→무료 모의고사→탈퇴/병합 검증 전에는 출시 준비 완료로 판정하지 않는다.

## 2. 사용자가 반드시 읽어야 하는 내용

이미지 승격과 테스트 task 복사는 다르다. 같은 이미지를 사용하더라도 DB, JWT 신뢰, 사용자 세션, 이벤트 목적지, AI callback, S3/Redis 및 기능 flag는 신규 운영 환경 기준으로 맞춰야 한다. 테스트 DB에서 운영 계정/기록이 자동으로 이어지지 않는다. 구·신 writer를 공유 DB에 동시에 붙이는 경우에도 호환성·인덱스·fence를 검증해야 한다.

| 항목 | 확인 결과 | 배포 전 조치 |
| --- | --- | --- |
| 최신 앱 버전 | 두 플랫폼 모두503 | 실제 출시 버전 환경변수 설정 후200 확인 |
| 무료 모의고사 채점 | AI_SERVER_URL=http://127.0.0.1:9 | 접근 가능한 실제 AI 및 callback 연결·E2E 확인 |
| 10초 챌린지 | CHALLENGE_ENABLED=true, AI는 ai-test | 신규 LC를 향한 AI callback 및 방향별 인증, catalog/index/contentBaseDate 검증 |
| 기존 계정·학습 기록 | Identity/LC 모두 테스트 DB | 운영 데이터 승계 방법 및 구·신 서버 동시 운영 검증 |
| 탈퇴 전파 | Identity UserWithdrawn publisher OFF, LC consumer/deny OFF | 출시 탈퇴 범위와 계정 차단·전파 조건 확인, 검증 없이 flag만 켜지 않음 |
| Guest 병합 | 테스트 Identity→api-test 이벤트 발행 ON | 신규 endpoint·JWT workload 신뢰·운영 guard/index 및 구 writer 혼재 검증 |
| 결제 후속 분리 | LC Billing creation/continuation/reconciliation OFF | 1차 무료 방향과 부합. 실제 무료 시험 생성·채점 성공은 별도 검증 |

기존 서버 boolean 기능은 구현되어 있으나, 현재 LC main task:23의 APP_UPDATE_REQUIRED는 false다. 실제 구현 필드는 **summary의 result.appUpdateRequired**이며 이전 계획 문서의 result.updateRequired와 다르다. 문항별 결과에도 동일 필드가 구현됐다고 확인한 것은 아니다. 구앱/웹뷰가 실제 필드명을 처리하는지와 전환 시true 적용 시점을 맞춰야 한다.

## 3. 사용자가 결정해야 하는 사항

- 신규 Identity와 Learning Core의 각각의 base URL 및 ECS service 이름. 현재 점검한 서울 클러스터에는 기존/test Identity·LC 및 AI서비스2개, 총6개가 표시되며 별도 신규 운영 서비스는 식별하지 못했다.
- 기존 운영 사용자·Guest 세션·학습 기록을 이어갈 저장소/전환 방식. JWT issuer/JWKS, 세션 해시/복구 keyring은 단순 도메인 치환으로 결정하지 않는다.
- Android/iOS 실제 공개 버전, 구서버 업데이트 안내를 켤 시점.
- 모의고사 AI와 챌린지 AI의 신규 환경 endpoint/callback, 1차 무료 기능 범위.

## 4. 주요 위험과 미확인 사항

- 새 주소가 지정되지 않아 DNS/TLS/ALB host rule/target group/IAM/Secret/S3 CORS·권한을 새 서버 기준으로 검증하지 못했다.
- 실행 task의 실제 digest와 서명/취약점/CI 결과까지 독립 검증한 것은 아니다. 아래는 서비스가 참조하는 task definition의 이미지다.
- 기존 사용자 앱 업데이트 후 세션 유지, refresh 회전 응답 유실, merge, 탈퇴 및 데이터 삭제 E2E 미실행. [전환 검토](../contracts/guest-app-update-transition-review.md) 참조.
- AI_SERVER_URL 설정은 코드상 시험 GradingDispatchService의 실제 요청 URL이다. 로컬9번 포트가 응답 가능한 서비스인지 네트워크 probe는 수행하지 않았다. 해당 설정을 검증 없이 복사하면 무료 시험 채점 준비 완료로 볼 수 없다.
- test notification API/tracking은 ON, sending OFF/dry-run true다. 알림 출시까지 포함하려면 별도 gate가 있다. 이번 요청에 알림 발송 활성화는 포함하지 않았다.
- 운영 DB index/backfill은 테스트 DB 준비와 별개다. 원격 DB mutation/기능 활성화/기동·배포는 수행하지 않았다.

## 5. 현재 작업과 직접 관련된 설명

추천 순서는 이미지 참조 고정 → 두 신규 주소·데이터/보안/AI 연동 확정 → 운영 대상 DB의 읽기 전용 readiness 점검 및 필요한 migration 별도 승인 → 신규 task 설정 작성 → 초기 배포 → health/버전 API/사용자 E2E → 구서버 안내 활성화다. 기존 서버 서비스나 주소를 덮어쓰는 배포와 구분한다.

이미지 버전과 주요 설정을 아래에 보존했다. 비밀값 및 전체 DB URI는 조회 결과나 문서에 출력하지 않았다.

## 6. 부록 — 상세 근거

### AWS 실측: Identity

- 서비스: tosunsaeng-identity-test-service, task tosunsaeng-identity-test:29, 실행1/보류0, 성공, ALB 정상1/비정상0.
- 이미지: tosunsaeng-identity:34af0234fec8b9becb237675f1a2555fc9d39f4e. 로컬 HEAD도34af0234.
- MONGODB_DATABASE=to-teacher-identity-test, JWT_ISSUER=https://identity-test.to-teacher.com.
- FIREBASE_AUTH_ENABLED, FIREBASE_WITHDRAWAL_ENABLED, FIREBASE_WITHDRAWAL_CLEANUP_ENABLED, FIREBASE_WITHDRAWAL_IDENTITY_RELEASE_ENABLED, GUEST_MERGE_ENABLED=true.
- OWNER_EVENT_LEARNING_CORE_USER_MERGED_PUBLISHER_ENABLED=true, 목적지 https://api-test.to-teacher.com/internal/v1/events/user-merged.
- USER_WITHDRAWN_PUBLISHER_ENABLED=false.
- APP_ANDROID_LATEST_VERSION/APP_IOS_LATEST_VERSION task 환경변수 미표시. 공개 GET /api/v1/app/version?platform=android 및 ios를 인증 없이 호출하여 각각503/APP_VERSION_UNAVAILABLE 확인.

### AWS 실측: Learning Core

- 서비스: tosunsaeng-learning-core-test-service, task tosunsaeng-learning-core-test:17, 실행1/보류0, 성공, ALB 정상1/비정상0.
- 이미지: tosunsaeng-learning-core@sha256:22a878a5eff35f8c773394eb24682795653e573e04740b718a9cb7c3656b6d06.
- MONGODB_DATABASE=to-teacher-learning-core-test, AWS_S3_BUCKET_NAME=tosunsaeng-test-audio, SPRING_DATA_REDIS_DATABASE=2.
- IDENTITY_ISSUER=https://identity-test.to-teacher.com, IDENTITY_JWK_SET_URI=https://identity-test.to-teacher.com/.well-known/jwks.json, IDENTITY_AUDIENCE=tosunsaeng-learning-core.
- APP_UPDATE_REQUIRED=false, CHALLENGE_ENABLED=true, CHALLENGE_AI_ENDPOINT=https://ai-test.to-teacher.com/v1/challenges/evaluations.
- AI_SERVER_URL=http://127.0.0.1:9. SPRING_APPLICATION_JSON에는 notification 설정5개만 있으며 이 AI URL을 덮어쓰지 않는다.
- BILLING_CREATION_SAGA_ENABLED/BILLING_PHONE_CONTINUATION_ENABLED/BILLING_RECONCILIATION_ENABLED=false.
- USER_MERGED_CONSUMER_ENABLED/USER_MERGED_WRITER_ENABLED=true, USER_WITHDRAWN_CONSUMER_ENABLED/USER_WITHDRAWN_DENY_GATE_ENABLED=false.
- 기존 LC main task:23 APP_UPDATE_REQUIRED=false를 별도로 확인.

### 소스·문서 및 검증 범위

- [앱 버전 계약](../contracts/app-version-api.md), [구·신 전환 검토](../contracts/guest-app-update-transition-review.md), [Identity 배포 분리](../contracts/identity-branch-deployment.md).
- LC 로컬 develop a77a53f의 src/main/java/web/tosunsaeng/domain/exams/application/GradingDispatchService.java:116은 gradingProperties.aiServerUrl()로 요청한다.
- LC ExamRestController.java:114~117은 summary 응답의 appUpdateRequired를 조립한다. ExamResponseDTO.java:130에 해당 boolean 필드 존재.
- LC docs/codex/CURRENT_STATE.md는 test:17(a77a53f) 배포 및 main:23 flag=false 변경을 기록한다. 이번에는 해당 상태를 AWS UI와 대조했으며 기록만으로 운영 성공을 가정하지 않았다.
- 공개 API GET2건·AWS UI 읽기 조회·소스/계약 대조·git diff --check 수행. 제품 코드 변경이 없어 Gradle 재실행 없음. 인증 필요 사용자 기능은 호출하지 않았고 데이터/설정/배포/Git 변경 없음.
