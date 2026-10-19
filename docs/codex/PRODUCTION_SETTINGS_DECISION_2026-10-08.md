# 신규 운영 설정 확정안

> 탈퇴 설정 적용 완료(2026-10-08 19:10 KST): Identity revision4 ECS COMPLETED·실행1/대기0·새 target healthy. 탈퇴 API/cleanup/identity release/publisher ON, 실제 endpoint는 `/internal/v1/events/withdrawn`. 배포 전후 lifecycle/outbox0건으로 실제 계정 탈퇴/전파 E2E는 미검증. Apple 삭제 보류 가능성과 병합/백필OFF는 유지한다. 아래는 단계별 이력이다.

> 탈퇴 경로 정정: 준비 명세의 user-withdrawn 경로 오기로 revision3 시작 검증 실패. 실제 양쪽 코드의 /internal/v1/events/withdrawn으로 정정하여 Identity revision4 재배포 요청. 정책/인증 완화나 다른 서버 경로 변경 없음. 아래 revision3 기록은 첫 시도 이력이다.

> 탈퇴 활성화 후속(2026-10-08): 기존 대기 건 포함 사용자 승인 후 Identity revision3에 탈퇴 API/cleanup/identity release/user-withdrawn publisher 네 플래그만ON. 기존 대기 lifecycle/outbox는 모두0건, Identity Firebase get/update/delete 권한을 비파괴 방식으로 확인했다. 배포 결과는 CURRENT_STATE 참조. 병합·백필OFF 유지. Apple provider obligation은 미구현으로 삭제 보류 가능, 전체 탈퇴 E2E 완료로 해석하지 않는다.

> 최종18:49 KST: Identity2/LC3/AI1/챌린지AI1 네 서비스 모두 ECS COMPLETED·실행1/대기0. 두 API health200. 실제 푸시ON, FCM 인증/validate_only200. 실기기 수신 및 실제 채점·S3·callback E2E는 미검증, Identity 병합·탈퇴OFF 유지. 아래 진행 중 기록은 중간 이력이다.

> 재개 진행(2026-10-08): 신규 앱 ECS 기동 유예를 기존 테스트와 동일한300초로 보완했다. Identity revision2 및 두 AI revision1은 steady state, LC revision2도 target healthy 확인. FCM 전용 역할 저장 및 사용자 JSON 키 생성 후 전용 AWS Secret/실행 역할에 연결했다. Google OAuth 및 FCM validate_only는 각각200이며 실제 기기 발송 테스트는 아니다. 실제 발송ON/dry-run false인 LC revision3 배포를 요청했고 최종 결과는 CURRENT_STATE를 참조한다. Identity 병합·탈퇴·이벤트 발행OFF는 유지한다.

> 최신 기동 상태(2026-10-08): 사용자 테스트 기동 승인 후 Identity/LC revision2 desiredCount1 적용. Identity 전화번호 키 상태 표기 오류를 키 소재 변경 없이 수정했으나 최종 공개 health는 Identity503/LC502다. AI2개는 아직0이며, 실제 푸시 발송은 추가 승인됐지만 전용 FCM 계정 생성·역할 선택까지만 진행됐다. 역할 저장/키 발급/LC 연결 및 실제 발송은 미완료다. 브라우저 제어 복구 후 ECS health 원인을 해결해야 하며 아래 실행수0 기록은 이전 단계의 이력이다. 현재 상태는 CURRENT_STATE 및 deployment JSON의 bootstrapObservation 참조.

> 신규 외부 구성 적용(2026-10-08): 사용자 구체 승인 후 prod task revision1 네 개 및 ECS 서비스4개를 desiredCount0으로 등록했다. target group3개와 ALB60/70/80 규칙 추가, 가비아 서비스 CNAME2개(TTL600) 저장 완료. DNS 기존20개 행은 추가 후에도 모두 동일함을 비교했다(총22개). **등록 완료이지 서버 기동/출시 완료가 아니다.** Identity/LC launchTargetFlags는 실제 등록본에서 전부false, 알림 API/추적/발송OFF, 앱 버전 공란, 기존 운영 동의 버전 계승이다. 두 AI worker 컨테이너는 존재하지만 태스크0으로 실행되지 않는다. 실행 시점 설정과 API-only 검증 계획은 후속 필수다.

> ECS·라우팅 준비(2026-10-08): 실제 실행 태스크와 ECR에서 승격 대상 이미지 digest4개를 대조하고 [초기 구성안](deployment/production-topology-2026-10-08.json)을 작성했다. 앱/AI 포트·health 경로, 신규 역할/SG, target group3개 및 ALB 신규 우선순위60/70/80을 연결한 **아직 적용하지 않은 명세**다. 신규 서비스 실행 수0, autoscaling/Exec 미설정으로 준비하며 task definition 완성·등록 및 외부 구성은 남아 있다. 두 prod DNS는 CloudShell 조회에서 해석되지 않았다. 기존 AI 태스크는 API와 worker가 동시 시작되므로 실제 기동 전에 API-only 단계 또는 worker 제어를 별도로 설계해야 한다.

> S3·방화벽 적용 완료(2026-10-08): 사용자 구체 범위 승인 후 신규 runtime 역할2개, SG4개, 공유 Redis 수신6379 규칙3개를 추가했다. 역할 정책·ECS 신뢰, SG ingress/egress exact 일치, Redis 기존 규칙 보존을 재조회 확인했다. 기존 버킷 공개 정책은 사용자 결정대로 유지하며 객체 읽기/쓰기/삭제는 수행하지 않았다. **서비스 연결/배포/기동은 아직 하지 않았다.** 적용 코드와 실제 SG ID는 [스크립트](../../scripts/production-runtime-network.py) 및 [설정 JSON](deployment/production-settings-2026-10-08.json)의 runtimeNetworkProvisioning을 참조한다. 아래 승인 대기안은 과거 상태다.

> S3·네트워크 후속 점검(2026-10-08): 기존 버킷을 유지하라는 사용자 결정을 반영했다. `to-teacher-exam/*` 전체에 익명 GetObject가 허용되고 PublicAccessBlock4개가 false다. 신규 IAM 제한이 이 공개 읽기를 차단하지는 않는다. 정책/버킷은 변경하지 않았다. 운영 S3 runtime 역할2개와 서비스별 SG4개, 공유 Redis 수신 규칙3개 추가안을 아래 부록에 준비했으며 실제 권한 부여 전 승인 대기다.

> 최신 적용 상태(2026-10-08): 사용자 승인 후 신규4서비스 실행 역할과 로그 그룹 준비 완료. [적용 스크립트](../../scripts/production-execution-roles.py)의 서비스별 정확한 Secret/ECR/로그 범위 및 ECS 신뢰 조건을 재조회 검증했다. Secret·ECR 시뮬레이션은 통과했으나 로그 허용8개는 implicitDeny이며 Resource=* 대조 정책도 동일했다. 실제 로그 쓰기 가능 여부는 미확정이다. **권한 정책 저장 완료이지 배포 완료가 아니다.** 기존 서비스/역할/Secret 값은 그대로이며 신규 runtime S3 역할·네트워크·task definition 연결·기동은 남아 있다. 아래 초기 역할 기록은 과거 상태다.

> AWS Firebase 연결 준비 완료(2026-10-08): 승인된 JSON을 소유자 전용0600으로 제한 후 동일 AWS 서울 계정의 `tosunsaeng/prod/identity/firebase`에 저장했다. 운영 계정 식별/JSON·PEM 외형 및 AWSCURRENT 원본 일치 검증, CloudShell 임시본 제거 완료. 신규 `tosunsaeng-identity-prod-execution-role`은 ECS 신뢰 조건과 해당 Secret 정확한 ARN의 GetSecretValue만 가진다. 시뮬레이션 운영 allowed/테스트 implicitDeny, 기존 역할 수정 없음. **DB/JWT/keyring·ECR/로그 권한과 task definition 연결은 아직 없으므로 이 역할로 즉시 기동할 수 없다.** 실제 Google 인증 및 서버 배포도 미완료다.

> Firebase 키 발급 확인(2026-10-08): 사용자 직접 생성 후 GCP 다운로드 완료 메시지 및 로컬 안내 파일 존재 확인. 원문은 읽거나 전송하지 않았다. AWS 서울 동일 계정의 `tosunsaeng/prod/identity/firebase` 저장 및 신규 Identity 실행 역할의 제한된 읽기 연결 범위를 확인받은 뒤 진행한다. 로컬 다운로드 파일 권한 제한과 CloudShell 임시 파일 정리가 필요하며 아직 AWS 연결/배포는 미실행이다.

> Firebase 권한 적용 완료(2026-10-08): 행동 직전 사용자 승인 후 identity-prod에 Firebase 인증 뷰어와 Identity Firebase User Cleanup 역할2개를 부여했다. 정책 업데이트 성공 및 저장된 두 역할 재조회 확인. 추가 위임·관리자·토큰 생성자 역할 부여 없음. JSON 키 발급 대화상자를 열고 사용자 직접 만들기/다운로드 단계로 인계했으며 키 발급·AWS 연결은 아직 미완료다.

> Firebase 분리 진행(2026-10-08): 사용자 승인으로 동일 프로젝트에 `identity-prod@to-teacher-firebase.iam.gserviceaccount.com` 생성 완료. 아직 프로젝트 역할 적용·키 발급·AWS 연결은 하지 않았다. 인증 뷰어와 기존 `projects/to-teacher-firebase/roles/identityFirebaseUserCleanup`(firebaseauth.users.update/delete)의 부여 직전 확인 대기. 관리자/토큰 생성자 역할은 선택하지 않았다. 같은 프로젝트이므로 운영/테스트 사용자 자체가 IAM으로 격리되는 것은 아니다.

> 후속 상태(2026-10-08): 사용자 직접 실행으로 운영 Secret6개 생성 완료. AWS 읽기 검증에서 모두 AWSCURRENT·규격/32-byte 길이 정상, 6개 키 소재가 서로 다름을 확인했다. 원문 출력/파일 저장 없음. 아래 생성 전 인계 기록은 과거 상태다. Firebase 운영 서비스 계정 선택·권한 부여 및 ECS/라우팅/기동은 아직 미완료다.

## 1. 5줄 결론

1. 기존 운영 DB와 JWT 키 참조·issuer를 유지한다. AWS에서 두 운영 DB Secret의 호스트가 준비·백업한 클러스터와 일치함을 읽기 검증했다.
2. 신 Identity·Learning Core·모의고사 AI·챌린지 AI의 4서비스 설정 명세를 작성했다. [정확한 값과 플래그](deployment/production-settings-2026-10-08.json)는 등록 가능한 ECS task definition이 아니다.
3. 신 AI의 큐·job prefix·콜백을 구/테스트와 분리한다. 챌린지 HTTPS 요청은 새 API 도메인의 전용 경로로 라우팅하는 설계다.
4. SNS·병합·탈퇴·챌린지 처리 worker는 출시 목표로 지정하고, 실제 알림 발송·결제·과거 탈퇴 backfill·추가 정리 작업은 보류한다. 현재 켰다는 뜻이 아니다.
5. 운영 전용 Secret 준비·IAM·이미지 digest·네트워크·콜백 검증 후 실제 구성/기동이 필요하다. 이번 작업에서 AWS 설정과 기존 서비스는 변경하지 않았다.

## 2. 사용자가 반드시 읽어야 하는 내용

### 기존 계정과 인증 유지

- Identity DB `to-teacher-identity`, Learning Core DB `to-teacher-app`을 그대로 사용한다. 테스트 DB를 복제하지 않는다.
- 운영 Identity task23의 `MONGODB_URI`, `JWT_PRIVATE_KEY_PEM`, `JWT_PUBLIC_KEY_PEM` Secret **참조**와 `JWT_KEY_ID`를 계승한다. 실제 값은 문서에 저장하지 않는다.
- 신주소는 `identity-prod.to-teacher.com`이지만 JWT issuer는 `https://identity-staging.to-teacher.com`, audience는 `tosunsaeng-learning-core`, Access TTL은 `PT30M`으로 유지한다. 구 issuer/JWKS 주소는 계속 접근 가능해야 한다.
- PEM 주입은 기존 [entrypoint](../../docker-entrypoint.sh)를 유지한다. 테스트 키를 운영에 복제하지 않는다. [JWT 계약](../contracts/identity-learning-jwt.md)은 변경하지 않는다.

### AI 분리

- 모의고사: LC → Service Connect `http://tosunsaeng-ai-prod:8000` → 신 LC feedback/azure callback.
- 챌린지: LC → `https://api-prod.to-teacher.com/v1/challenges/evaluations` → challenge API. ALB에서 해당 host+정확한 path 규칙을 일반 LC host 규칙보다 우선시한다. 기존 두 도메인 인증서를 사용하며 새 도메인은 만들지 않는 설계다.
- 챌린지 callback은 `https://api-prod.to-teacher.com/internal/v1/challenges/grading/callback`. 요청용과 callback용 인증값은 서로 다르게 생성하고 각 방향의 양 끝에서 같은 Secret 참조를 사용한다.
- 기존 Redis 연결과 DB 번호(모의고사1, 챌린지2)를 유지하되 모든 API/worker에서 큐와 job prefix를 함께 분리한다. 정확한 문자열은 JSON 명세를 따른다.
- 위 alias, ALB rule, target group, 서비스 DNS는 **아직 생성하지 않았다**. 큐 분리만으로 공유 Mongo의 재시도 job이 올바른 서버로 가는지까지 입증한 것은 아니다.

## 3. 사용자가 결정해야 하는 사항

- 다음 외부 적용 범위: 운영 Secret 생성/참조 확정 및 최소 IAM 권한, 신규4서비스/네트워크/라우팅 생성과 기동. 이번 명세 작성에는 실제 적용을 포함하지 않는다.
- 알림 실제 발송 시점과 대상: 현재 목표는 API·추적 ON, 발송 OFF, dry-run ON이다. 프론트 실기기 검증 후 tracking-ready-at을 실제 검증 시각으로 설정하고 발송을 별도로 활성화한다.
- 운영 minimumVersion/latestVersion은 실제 출시 빌드·스토어 상태를 확인한 뒤 결정한다. 테스트1.0.0 또는 과거 계획1.1.0을 자동 적용하지 않는다.
- 구주소 공통 코드 교체는 이번 필수 작업으로 추가하지 않는다. 사용자가 수용한 좁은 채점·병합 저장 경합과 별개인 교차주소 세션/탈퇴 위험은 미해결로 공개한다.

## 4. 주요 위험과 미확인 사항

- 새 세션 복구 암호화 keyring·전화번호 fingerprint/eligibility keyring·계정 recovery keyring·Firebase 운영 자격증명·챌린지 양방향 인증값이 아직 확정/주입되지 않았다. Firebase 프로젝트는 동일하게 유지하되 테스트 서비스 계정의 자격증명과 권한을 무조건 복제하지 않는다.
- worker를 켜면 공유 DB의 대기 이벤트를 처리할 수 있다. 기동 직전 대상·건수와 처리 영향을 확인하고 적용 범위 승인을 받아야 한다. 과거 탈퇴 backfill OFF가 모든 기존 대기 작업을 차단하는 것은 아니다.
- 구주소의 옛 세션 요청·병합 enum·탈퇴 deny 부재는 조건부 위험으로 남는다. 기존 키 유지가 이 문제를 해결하지 않는다. 상세 [인증 점검](PRODUCTION_AUTH_BACKUP_REVIEW_2026-10-08.md).
- AI 소스 자체를 독립 재검증하지 않았다. ECS 환경값과 LC 계약을 기준으로 설계했으며 실제 AI 인증·health·콜백 성공은 기동 후 검증해야 한다.
- [로컬 백업](PRODUCTION_AUTH_BACKUP_REVIEW_2026-10-08.md)은 live 논리 백업이며 동일시점 복구·실제 restore 검증은 완료하지 않았다.
- Support/학습기록 삭제는 해당 선택 기능의 스키마·권한 준비 전까지 보류한다. 제품 기능을 제거하는 결정은 아니다. auto-index 등 기동 기본 동작까지 feature OFF로 모두 막는다고 보장하지 않는다.

## 5. 현재 작업과 직접 관련된 설명

### 실행 목표와 보류

| 영역 | 출시 설정 목표 | 보류 |
| --- | --- | --- |
| Identity 인증 | session fence/recovery, SNS/phone 검증, provider link/recovery, guest merge | unlink/relink, logout-all 정리 worker |
| 병합 이벤트 | legacy user-merged publisher와 owner-event capture/progress/LC publisher를 테스트 조합대로 함께 지정, `LEARNING_CORE_ONLY` | Billing publisher/trial rebind |
| 탈퇴 | API, Firebase cleanup/identity release, LC publisher 및 LC consumer/deny | 과거 탈퇴 backfill |
| 챌린지/모의고사 | 새 큐의 AI API+worker, 신 callback | callback 도달 검증 전 consumer 시작 |
| 알림 | API/추적 ON | 실제 발송 OFF, dry-run ON |
| 기타 | 전화번호 연결 정보 바인딩 | 결제·전화번호 1회 무료 발급 publisher·abandoned cleanup·Support·학습기록 삭제 |

`PHONE_ELIGIBILITY_BINDING_CONSUMER_SCOPE_ID=tosunsaeng-billing-prod`는 선택한 안정적 운영 namespace다. Billing 서비스가 생성됐거나 1회 무료 기능을 켰다는 뜻이 아니다. 이후 임의 변경하지 않는다.

LC 알림 설정은 yml의 전용 환경변수 placeholder가 아니라 Spring configuration property override로 주입한다. JSON의 `springPropertyOverrides`를 최종 `SPRING_APPLICATION_JSON` 등으로 변환할 때 다른 설정과 병합한다.

구서버의 `APP_UPDATE_REQUIRED`는 스토어 공개 후 피드백 WebView 정책에 맞춰 별도 변경한다. 이번 신규 LC 명세는 false이며 구서비스 환경값은 변경하지 않았다.

### 검증

- `node --test scripts/production-settings.test.cjs`: 6/6 성공. 운영 인증 연속성·AI 주소/큐·플래그 의존성과 Identity 설정 이름 검증.
- `./gradlew clean test`: 총1310, 성공1304, skipped6, 실패/오류0.
- 위 테스트는 명세/로컬 코드 검증이다. 운영 Secret 권한·ECS 기동·실제 계정 승계·알림 발송 성공을 의미하지 않는다.

## 6. 부록 — 조사 근거와 설정 원문

| 근거 | 확인 내용 |
| --- | --- |
| AWS ECS read-only, 운영 Identity23/LC23 | DB Secret host가 준비·백업한 클러스터와 일치(true만 출력), 운영 키 참조/issuer 유지 |
| AWS ECS read-only, AI17/learning-test2 | API/worker 구성, 기존 Redis DB1/2, 큐/콜백 값 |
| ECS PRIMARY deployment Service Connect | namespace `tosunsaeng-staging`, 기존 exam AI alias와 LC client 구성 |
| [Identity application.yml](../../src/main/resources/application.yml) | 기능 플래그/endpoint 환경변수 원문 |
| [설정 명세](deployment/production-settings-2026-10-08.json) | 정확한 flag·endpoint·TTL/skew·Secret 입력 목록·등록 전 체크 |
| [정적 테스트](../../scripts/production-settings.test.cjs) | 명세 불변조건 자동 확인 |
| LC `domain/challenge/ChallengeProperties.java` | 운영 HTTPS와 `/v1/challenges/evaluations`, 양방향 별도 인증값 계약 |
| LC `domain/notification/NotificationStartupValidator.java` | 발송 선행 추적/ready-at/병합·탈퇴 gate 조건 |
| LC `UserWithdrawnConsumerProperties` | consumer/deny 의존성, Access TTL와 verifier skew 일치 |

LC 근거 저장소는 `/Users/msde76/app-back-end-learning-core`이며 이번에 해당 제품 코드를 변경하지 않았다. Python `user_id=examId`, UUID JWT sub, RS256/kid 계약도 유지한다.

### 후속 적용 착수 — Secret 생성 직전 인계

2026-10-08 사용자 진행 요청 후 AWS Secret 목록과 실행 역할 정책을 읽기 점검했다. `tosunsaeng/` 목록에 prod 전용 Secret은 없으며, 운영 공용 실행 역할의 조회한 inline policy는 staging Identity/LC/AI Secret 3개에만 GetSecretValue를 허용한다. 신규 prod Secret의 읽기 권한은 준비되지 않았다. 기존 역할의 권한을 넓히기보다는 신규 서비스별 실행 역할에 정확한 Secret ARN만 부여하는 안을 우선 검토한다. IAM 변경은 미실행이다.

[사용자 실행 도구](../../scripts/provision-production-secrets.py)를 CloudShell에 업로드하고 `--apply`의 생성 전 확인 프롬프트까지 실행했다. 해당 시점에는 6개 MISSING이며 **Secret 생성 전**이다. 사용자가 CloudShell에 `CREATE-PROD-SECRETS`를 직접 입력하고 제출하면 아래 6개만 생성한다. 중단하려면 Ctrl+C를 누른다. 새 인증정보의 생성·저장 단계는 사용자에게 인계한다.

| Secret 경로 (`tosunsaeng/prod/` 아래) | 저장 형식 | 사용할 설정 |
| --- | --- | --- |
| identity/reissue-encryption-keyring | `prod-v1=<32-byte base64>` properties | AUTH_REISSUE_ENCRYPTION_KEYRING_CONTENT; active ID=prod-v1 |
| identity/phone-fingerprint | `prod-v1,ACTIVE,<32-byte base64>` | PHONE_IDENTITY_FINGERPRINT_KEY_RING |
| identity/phone-eligibility-binding | 위와 같은 형식, 별도 난수 | PHONE_ELIGIBILITY_BINDING_KEY_RING |
| identity/account-recovery-key-ring | `prod-v1:<32-byte base64>` | ACCOUNT_RECOVERY_KEY_RING |
| challenge/outbound | 32-byte 난수의 base64url 문자열 | LC outbound / AI inbound 동일 참조 |
| challenge/callback | 위와 같은 형식, 별도 난수 | AI callback / LC callback 동일 참조 |

- 도구는 계정/리전을 고정 확인하고, 기본 실행은 metadata 조회만 한다. 기존 Secret은 미검증으로 표시하고 값 조회·덮어쓰기·회전하지 않는다. 생성 중 일부 실패하면 다음 조회에서 성공한 것과 누락된 것을 구분해 재개하며, 삭제 대기 항목은 수동 해결이 필요하다.
- 원문은 실행 프로세스 메모리에서 생성해 같은 AWS 계정 Secrets Manager로 직접 전달하며 출력/로컬 파일에 저장하지 않는다. 기본 AWS 관리형 암호화와 일반 Secret 저장/API 과금이 적용된다. 성공 로그도 이름만 출력한다.
- Firebase 서비스 계정, 기존 DB/JWT/AI vendor 키, IAM·ECS·라우팅은 이 도구가 만들거나 변경하지 않는다. Firebase 운영 자격증명 선택/권한 검증은 별도로 남는다.
- 업로드한 파일에는 실제 키·토큰·URI가 없다. 서버 기동·worker 활성화는 아직 하지 않았다. 생성 후 존재 여부/형식 검증, 최소 권한 연결 및 기동 준비를 이어가야 한다.
- 오프라인 Mock 테스트5개 통과: 형식·독립 난수·무쓰기 plan·재실행 무덮어쓰기/원문 비출력·삭제대기 차단. 실제 Secret 생성 성공 검증은 사용자 실행 후 수행한다.

### 실행 역할 적용 및 후속 S3·네트워크 조회(2026-10-08)

확인된 사실: 신규 실행 역할4개의 정책·신뢰와 로그 그룹을 재조회했다. [스크립트](../../scripts/production-execution-roles.py)의 exact allowlist와 일치한다. Identity Secret6개, LC3개, 모의고사 AI1개, 챌린지 AI3개이며 중복 참조를 포함한 서비스별 개수다. IAM49건 중41건 기대 일치, 로그 허용8건 implicitDeny. 로그 전체 리소스 허용 대조 정책도 implicitDeny라 시뮬레이터 결과만으로 실제 권한 오류/정상을 확정하지 않는다. 대조 정책을 실제 역할에 저장하지 않았다.

| 읽기 확인 대상 | 확인된 현재 값/범위 | 후속 판단 |
| --- | --- | --- |
| 운영 LC runtime S3 | GetObject/PutObject, `arn:aws:s3:::to-teacher-exam/*` | 신규 runtime 역할 별도 승인·연결 필요; DeleteObject는 현재 없음 |
| 운영 LC 추가 inline | ssmmessages 제어/데이터 채널4개 액션, Resource=* | 기존 ECS Exec 권한을 신규 역할에 자동 복제하지 않음 |
| 테스트 챌린지 AI runtime | GetObject/PutObject/DeleteObject, `arn:aws:s3:::tosunsaeng-test-ai-audio/challenge/*` | 운영 저장 위치·prefix를 확정하기 전 재사용 금지 |
| 기존6개 ECS 서비스 | 같은 subnet2개, assignPublicIp=DISABLED | 새 서비스의 public IP 비활성 유지 후보; NAT/route/endpoint는 미검증 |
| VPC/subnet | vpc-04068339482839ff1; subnet-0fa638ca8effafa81, subnet-0dc3e0343bb98e3e3 | 기존값 조회만 수행, 변경 없음 |
| 기존 Identity SG | sg-03ef324f668083758, ALB SG에서 TCP8081 | 신규 SG는 ALB에서만 앱 포트 허용하는 구성 후보 |
| 기존 LC SG | sg-00bc88844e6f59f15, ALB SG에서 TCP8080 | 동일 원칙; 기존 SG 변경 안 함 |
| 기존 모의고사 AI SG | sg-0aac51a28efa84789, 기존 LC SG에서 TCP8000 | 신규 LC→신규 AI 접근 규칙 필요 |
| 테스트 챌린지 AI SG | sg-00b8d3208c3fd7705, ALB SG에서 TCP8000 | 운영용으로 복제 적용하지 않음 |
| 위 ALB/Redis SG 참조 | ALB sg-0e5462fa65a22ded8; Redis sg-0f8528393d749b3a9 | 새 SG를 만들 때 Redis 수신 규칙 영향도 별도 확인 |
| egress | 기존 Identity/LC/모의고사 AI는 전체 IPv4 outbound; 테스트 챌린지 AI는 Redis SG TCP6379 및 IPv4 HTTPS443 | 실제 신규 외부 통신 경로 확인 후 최소 범위 확정 |

아직 하지 않은 작업: 신규 runtime S3 권한, SG/ALB/DNS 변경, ECS task definition/서비스 등록, 실제 로그 쓰기, worker 기동, callback E2E. 위 후속 판단은 제안이며 실제 적용 사실이 아니다. Python7/Node6 및 Gradle clean test 성공. 실행 역할 준비와 배포 완료를 구분한다.

### S3·네트워크 적용 제안 — 승인 전(2026-10-08)

#### 5줄 결론

1. 사용자 결정에 따라 기존 운영 버킷을 그대로 사용하고 공개 정책은 변경하지 않는다.
2. 버킷 전체 익명 읽기 정책을 실제 조회했으며 객체 주소를 알면 음성을 읽을 수 있는 잔여 위험이 있다.
3. 신규 LC는 기존과 같은 객체 읽기/쓰기, 챌린지 AI는 `challenge/*` 읽기/쓰기/삭제만 부여하는 runtime 역할을 제안한다.
4. 기존 private subnet2개와 NAT/S3 endpoint 경로를 재사용하고 신규 SG4개로 앱 포트 수신 대상을 제한한다.
5. 공유 Redis에는 신규 LC·두 AI의 TCP6379 수신 규칙만 추가한다. 아직 적용/배포/기동하지 않았다.

#### 반드시 읽을 내용·결정 사항

- 사용자: 비공개 버킷 분리 거절, 기존 공개 버킷 유지. 기존 데이터 이동·삭제·정책 변경 없음.
- 아래 IAM 및 SG/Redis 추가는 민감 접근을 새로 부여하므로 구체 범위의 행동 직전 승인이 필요하다. 버킷 유지 승인은 이 권한 부여 승인과 구분한다.
- 로그 시뮬레이션 불일치, 실제 callback, 시작 시 기존 대기 이벤트 처리 영향은 여전히 미검증이다. 이번 구성만으로 worker를 시작하지 않는다.

#### 제안하는 정확한 범위

| 대상 | 적용안 |
| --- | --- |
| tosunsaeng-learning-core-prod-task-role | ECS Tasks 동일 계정/리전 신뢰, s3:GetObject/PutObject on arn:aws:s3:::to-teacher-exam/*; 삭제·ECS Exec·Secret 읽기 미부여 |
| tosunsaeng-ai-learning-prod-task-role | 같은 신뢰, s3:GetObject/PutObject/DeleteObject on arn:aws:s3:::to-teacher-exam/challenge/*; 버킷 전체 삭제 권한 없음 |
| 신규 Identity SG | ALB SG→TCP8081, outbound HTTPS443 및 Mongo TCP27017 |
| 신규 LC SG | ALB SG→TCP8080, outbound HTTPS443/Mongo27017, Redis SG6379, 신규 모의고사 AI SG8000 |
| 신규 모의고사 AI SG | 신규 LC SG→TCP8000, outbound HTTPS443 및 Redis SG6379 |
| 신규 챌린지 AI SG | ALB SG→TCP8000, outbound HTTPS443 및 Redis SG6379 |
| 공유 Redis SG | 신규 LC·모의고사 AI·챌린지 AI SG에서 TCP6379만 추가. 기존 규칙 유지 |

- HTTPS443/Mongo27017 목적지는 동적 AWS/외부 공급자/Atlas 주소이므로 IPv4 0.0.0.0/0으로 포트만 제한하는 안이다. 서비스별 실제 목적지/연결은 기동 전 확인한다. Public IP는 비활성, 인터넷에서 task로 직접 수신은 허용하지 않는다.
- 기존 ALB outbound는 전체 허용이므로 이 단계에서 기존 ALB 규칙 변경은 계획하지 않는다. ALB 라우팅·DNS·target group 등록은 별도 후속이다.

#### 조사 근거와 미확인 사항

- AWS S3: AES256 기본 암호화, IsPublic=true, PublicAccessBlock4개=false. 실제 정책 Principal=* / s3:GetObject / to-teacher-exam/*; 객체 본문 조회 없이 정책만 확인.
- LC 로컬 코드 `ChallengeModels.java:45`: `temp/challenges/{attemptId}/q_{number}.m4a`. `ChallengeAudioStorage.java:25`: 같은 앱 bucket의 presigned PUT, HEAD/GET 사용. `ChallengeWorker.java:56` 및 `ChallengeAiClient.java:31`: LC가 파일을 읽어 multipart로 AI에 전달한다. 따라서 LC 원본 경로와 AI 임시 `challenge/*` 역할을 혼동하지 않는다. 배포 이미지와 로컬 코드의 일치·AI 실제 생성 prefix는 추가 검증 필요.
- AI 테스트 task2 양 컨테이너: LEARNING_AUDIO_BUCKET=tosunsaeng-test-ai-audio, LEARNING_JOB_KEEP_AUDIO=0; prefix 환경변수 없음. `challenge/*` 범위는 기존 테스트 역할 정책을 근거로 한 운영 복제안이며 이미지 코드 확인으로 보완해야 한다.
- private subnet2개는 각각 route table rtb-01781bcd3ebc4cc7c/rtb-036c1c56bacc666ef에 연결. 동일 NAT nat-0a11238673df5ef5d로 default route active, S3 endpoint vpce-0a561e232dd9f509b route active. 라우팅 존재 확인이지 실제 연결/endpoint policy/NACL 검증은 아니다. 단일 NAT 의존성 유지.
- 기존 Redis SG 수신6379는 기존 LC/AI 및 테스트 LC/AI SG만 허용. 따라서 새 SG를 쓰면 수신 규칙3개가 필요하다. 기존 서버 동작 경로를 제거/교체하지 않는다.

### ECS·ALB·DNS 준비 근거(2026-10-08, 적용 전)

#### 5줄 결론

1. ECR 및 실제 실행 태스크의 앱 이미지 digest4개를 대조해 topology JSON에 고정했다.
2. Identity34/LC19/모의고사 AI17/챌린지 AI2를 source로 사용하며 테스트 Secret은 복제하지 않는다.
3. 신규 서비스4개는 초기 desiredCount=0, public IP/자동확장/Exec 없음으로 준비한다.
4. 기존 ALB 규칙10/20/30/40/50/51/default를 유지하고 새 호스트만60/70/80으로 추가하는 안이다.
5. 실제 서비스 등록·DNS/ALB 적용 및 worker 기동은 아직 하지 않았다. 다음 외부 적용 범위 승인 필요.

#### 반드시 읽을 내용 및 사용자 결정

- 다음 승인 범위: 새 task definition·실행 수0 서비스4개, IP target group3개, 새 호스트 ALB 규칙3개, 서비스 CNAME2개. 기존 이름/우선순위/DNS 충돌 시 덮어쓰지 않고 중단한다.
- 새 DNS를 게시해도 태스크가0이므로 초기 응답은503일 수 있다. 프론트 전환이나 운영 정상 가용성을 의미하지 않는다.
- 신규 challenge AI 외부 노출은 `api-prod`의 정확한 `/v1/challenges/evaluations` 경로만이다. 인증값 검증을 유지한다. `/ready`는 target group 내부 health용이지 AI 전체 공개 경로가 아니다.
- 신규 모의고사 AI는 ALB target 없이 Service Connect 전용 alias로 연결한다. LC를 namespace client로 구성해야 한다.
- task definition은 현재 환경 설정 명세를 검토해 완성해야 한다. topology JSON 자체를 ECS register API에 제출할 수 없다.

#### 주요 위험/미확인

- 두 AI의 API와 worker가 같은 태스크에 포함된다(모의고사4 worker/챌린지1 worker). 실행 수0은 시작 방지일 뿐, 이후 API만 검증할 worker 분리 설계가 아니다. 기동 전에 API-only 초기 revision 또는 검증된 worker 비활성 방법 필요.
- 운영 minimum/latest 및 필수 동의 버전, 실제 이미지 내 음성 prefix, Secret 환경변수 이름·필수 항목, 로그 실제 쓰기 및 기존 대기 이벤트 영향 확인은 남아 있다.
- 기존 모의고사 AI 실행 태스크의 digest 집합에 MISSING도 포함됨을 관찰했다. 앱 digest는 ECR 값과 일치하나 모든 보조 컨테이너 이미지까지 검증 완료로 해석하지 않는다.
- DNS 결과는 CloudShell resolver의 실패이며 권한 DNS 서버의 NXDOMAIN 판정으로 단정하지 않는다. 가비아 레코드 추가 전에 현재 zone을 확인한다.

#### 상세 관찰값

| 서비스 | source | CPU / MiB 제안 | health | 구성 |
| --- | --- | --- | --- | --- |
| Identity | test34 | 512 / 1024 | 8081 /actuator/health,200 | source 컨테이너 health 없음, ALB health 사용 |
| LC | test19 | 512 / 2048 | 8080 /actuator/health,200 | 운영23의 메모리 유지 제안; 테스트는1024 |
| 모의고사 AI | operating17 | 1024 / 2048 | 8000 /ready | ai-api 및 app.jobs.worker 명령4개 |
| 챌린지 AI | test2 | 512 / 1024 | 8000 /ready,200 | learning-api 및 app.jobs.worker1개 |

- ALB `tosunsaeng-staging-alb`, DNS `tosunsaeng-staging-alb-447454057.ap-northeast-2.elb.amazonaws.com`. 리스너 ARN 및 전체 digest는 topology JSON 참조.
- 기존 모의고사 Service Connect PRIMARY 설정: namespace ns-wisnr6tgrfy7prhf, portName ai-api, discoveryName/alias tosunsaeng-ai:8000. 신규 discovery/alias는 tosunsaeng-ai-prod로 분리 제안.
- CNAME2개 대상은 위 ALB, TTL600 제안. 인증서 검증용 CNAME은 계속 보존한다.
- 검증: 설정/토폴로지 Node10개 성공, JSON parse 및 git diff 검사. 외부 서비스 생성·기동 테스트는 하지 않았다.

### 실행 수0 등록 결과(2026-10-08)

- [적용 도구](../../scripts/production-zero-services.py)는 기존 task의 컨테이너 구조를 사용하되 앱 환경은 기존 운영23에서 계승하고 운영 명세를 적용했다. 테스트 Secret을 제거하고 기존 운영 DB/JWT/Sentry·AI 공급자 묶음 및 prod전용 키 참조로 교체했다. Secret 값은 조회/출력/로컬 저장하지 않았다. 실제 환경 전체를 파일로 내보내지 않았다.
- 등록 task: tosunsaeng-identity-prod:1, tosunsaeng-learning-core-prod:1, tosunsaeng-ai-prod:1, tosunsaeng-ai-learning-prod:1. 서비스 이름/이미지/역할/SG는 topology JSON과 같다. LC 메모리는 기존 운영과 동일2048MiB.
- 앱 출시 플래그는 등록 시 모두false로 재정의. 설정 JSON의 launchTargetFlags=true는 여전히 **출시 목표**이며 현재 ECS값을 뜻하지 않는다. 신규 앱버전4개는 공란이며 확인 없이 운영 최소버전을 정하지 않았다. 앱 동의 버전은 운영23에서 계승했다.
- target group: tosunsaeng-identity-prod-tg(8081/actuator/health), tosunsaeng-lc-prod-tg(8080/actuator/health), tosunsaeng-ai-learning-prod-tg(8000/ready), HTTP200/IP형식. 기존 target group 변경 없음.
- ALB:60 identity-prod→Identity,70 api-prod + 정확한 /v1/challenges/evaluations→챌린지 AI,80 api-prod→LC. 기존10/20/30/40/50/51/default 유지. 모의고사 AI는 새 Service Connect alias tosunsaeng-ai-prod:8000, LC는 동일 namespace client.
- DNS: identity-prod/api-prod→기존 ALB CNAME, TTL600. 저장 성공 및 기존20개 레코드 문자열 비교 보존, 총22개. 인증서 갱신용 CNAME 보존. 새 서비스 사용 전 전파와 health를 별도로 확인해야 한다.
- 테스트: 신규 도구 Mock5개, 관련 Python총19개, Node10개, Gradle clean test25초 성공. 실제 task 시작 및 S3/로그/Google 인증/업무 callback 테스트는 하지 않았다.
- 재실행 안전: 신규 서비스/TG/우선순위/호스트가 이미 존재하면 덮어쓰기 대신 중단하도록 작성했다. 부분 실패 복구용 무조건 재실행 도구가 아니며 다음 변경은 기존 생성 리소스를 먼저 조회해야 한다.
