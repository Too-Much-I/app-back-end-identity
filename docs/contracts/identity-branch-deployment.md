# Identity main/develop 배포 분리

## 1. 5줄 결론

- 기존 deploy-staging.yml 하나를 수정하며 새 workflow는 추가하지 않는다.
- main은 기존 서비스와 staging 이미지 별칭을 유지한다.
- develop은 테스트 서비스와 test 이미지 별칭으로만 배포한다.
- 테스트 역할/서비스가 없거나 Task Definition family가 다르면 중단하고 운영으로 대체하지 않는다.
- workflow 수정만으로 최초 테스트 인프라가 생성되지는 않는다. 이번에는 AWS 배포를 실행하지 않았다.

## 2. 반드시 읽어야 하는 내용

| 항목 | main | develop |
| --- | --- | --- |
| ECS cluster | tosunsaeng-staging-cluster | tosunsaeng-staging-cluster |
| ECS service | tosunsaeng-identity-service | tosunsaeng-identity-test-service |
| Task Definition family | tosunsaeng-identity | tosunsaeng-identity-test |
| 컨테이너 이름 | tosunsaeng-identity | tosunsaeng-identity |
| 이미지 별칭 | staging | test |
| GitHub Actions repository variable | AWS_ROLE_ARN | AWS_TEST_ROLE_ARN |
| health URL | https://identity-staging.to-teacher.com/actuator/health | https://identity-test.to-teacher.com/actuator/health |

ECR 저장소는 기존 tosunsaeng-identity를 공유하며 배포 참조는 기존처럼 commit SHA 태그다. 채널 별칭은 분리하지만 SHA 태그의 ECR 변경 불가능 정책을 새로 설정하는 작업은 포함하지 않는다. 동시 실행 잠금은 브랜치별이다. 수동 실행도 main/develop만 허용하며 다른 branch/tag는 AWS 인증 전에 실패한다. 사용자가 언급한 devlop는 실제 저장소 브랜치 develop으로 반영했다.

## 3. 사용자 결정 및 배포 전 준비

1. 테스트 전용 OIDC 역할을 준비하고 AWS_TEST_ROLE_ARN을 GitHub repository variable로 등록한다. 기존 역할로 자동 fallback하지 않는다.
2. 테스트 역할 trust는 해당 저장소의 refs/heads/develop과 sts.amazonaws.com audience로 제한한다. main 역할도 refs/heads/main 제한을 확인한다. workflow 문자열 분기만으로 IAM 경계를 대신하지 않는다.
3. 테스트 역할에는 해당 ECR 업로드, 테스트 ECS service 갱신, Task Definition 등록/조회와 필요한 테스트 실행 역할에 대한 iam:PassRole만 허용한다. IAM 변경은 별도 검토/승인이 필요하다.
4. 테스트 family/task definition과 service를 최초 1회 준비한다. container name은 tosunsaeng-identity로 맞춘다. 운영 task를 복사한 뒤 DB/키/issuer/Secret/이벤트 목적지를 그대로 두어서는 안 된다.
5. 테스트 ALB 대상 그룹, 서비스 연결, 도메인 host rule, DNS를 준비한다. 최초에는 desired count 0 서비스를 준비하는 방법을 검토할 수 있지만 이미지/Task Definition 등록과 유효한 서비스 연결은 여전히 선행되어야 한다.
6. 이후 develop push로 테스트·빌드·업로드·테스트 서비스 갱신·health 검증이 실행된다. main push는 기존 서버 배포다.

## 4. 주요 위험과 미확인

- 테스트 CI 역할과 GitHub 변수는 등록 완료했지만 ECS 서비스/execution role은 아직 준비되지 않았다. 서비스가 없으면 이 workflow는 이미지 업로드 전 중단한다. 최초 이미지 업로드만 필요한 경우 별도 빌드 경로가 필요하다.
- 별도 family 검사는 운영 Task Definition을 그대로 사용하는 실수를 차단하지만 DB/키/권한/네트워크 격리를 증명하지 않는다. 실제 설정은 별도 검증한다.
- workflow를 원격에 반영하기 전에는 자동 배포가 바뀌지 않는다. Git commit/push는 사용자가 수행한다.
- 테스트 Role이 없어서 develop 배포가 실패하는 것은 의도한 fail-closed 동작이다.
- 기존 요청에 따라 API/JWT/기능 flag는 변경하지 않는다. Stage 9 ON은 지원 앱과 테스트 설정 검증 후 별도 결정한다.

## 5. 현재 변경 동작

브랜치 선택 → 테스트 → OIDC 인증 → 선택 서비스의 ACTIVE task 조회 → family/container 검증 → 이미지 업로드 → 동일 family의 이미지와 SENTRY_RELEASE만 갱신 → 해당 서비스 안정화 대기 → 해당 도메인 health 검사.

Task Definition은 선택 서비스에서 가져오고 secrets나 일반 환경변수를 운영 값으로 덮어쓰지 않는다. 설정 오류는 shell/jq 실패로 중단한다. 기존 main 서비스 family가 기대값과 다르면 마찬가지로 실패하므로 실제 family 확인이 배포 전 필요하다.

## 6. 검증 근거

### 2026-09-23 테스트 네트워크 및 ECS 서비스 생성 완료 — 기동 전

- 사용자 승인 후 `tosunsaeng-identity-test-sg` 생성: `sg-03c6bd60c4026a227`. VPC는 기존 staging VPC. 인바운드 TCP8081/소스 ALB SG만, 아웃바운드 IPv4 TCP443 및 TCP27017/0.0.0.0/0만 구성. 생성 성공 및 수신1/송신2 규칙 확인.
- 기존 HTTPS443에 priority30 exact host `identity-test.to-teacher.com` → test TG 전달 규칙 추가 성공. 규칙 ARN suffix `aeb32ca50779a105`. 기존 운영 호스트 규칙10/20와 기본404는 변경하지 않았다.
- `tosunsaeng-identity-test-service` 생성 완료, AWS 성공/ACTIVE 확인. task `tosunsaeng-identity-test:1`, desired0/running0/pending0, Fargate1.4.0, private subnet `subnet-0fa638ca8effafa81`/`subnet-0dc3e0343bb98e3e3`, 새 test SG만 사용. public IP OFF, ECS Exec OFF, Auto Scaling OFF, health grace300초, test TG 연결을 저장 화면에서 확인.
- 실제 API 서버는 아직 실행하지 않았다. 등록된 image의 pull 가능 여부와 Atlas 권한/Secret 주입/로그 수집은 아직 검증하지 않았다.
- 기동 차단사항: GitHub API로 읽은 원격 develop workflow는 아직 기존 운영 service/role/health 주소를 사용한다. 로컬 workflow 변경은 사용자 commit/push가 필요하며 현재 원격 workflow를 실행하지 않았다.
- DNS 조회 결과 `identity-test.to-teacher.com`은 NXDOMAIN이다. 가비아 DNS에 CNAME 호스트 `identity-test`, 값 `tosunsaeng-staging-alb-447454057.ap-northeast-2.elb.amazonaws.com` 연결 필요. 인증서 검증 CNAME은 유지한다.
- 다음 순서: 사용자 workflow commit/push 및 DNS 연결 → 새 develop 이미지/테스트 revision 확인 → desired1로 기동 → health/JWKS/로그/인증 검증. 초기 desired0 상태에서는 workflow health 검증이 실패할 수 있다. 기존 운영 배포로 대체하지 않는다.

### 2026-09-23 테스트 서비스 네트워크 확인 — 변경 승인 대기

- 콘솔 확인: ALB 보안 그룹 `sg-0e5462fa65a22ded8`(tosunsaeng-alb-sg), 인바운드 HTTP80/HTTPS443, 아웃바운드 모든 트래픽. 기존 SG는 수정하지 않는다.
- HTTPS443 규칙은 우선순위10 identity-staging, 20 api-staging 및 기본404이다. 테스트 호스트 규칙은 없다.
- 승인 요청안: 동일 VPC에 `tosunsaeng-identity-test-sg` 생성. 인바운드 TCP8081은 ALB SG에서만 허용. 아웃바운드는 IPv4 TCP443/27017만 전체 목적지로 허용(각각 AWS/Firebase HTTPS, Atlas MongoDB 연결). 포트 제한이지 목적지별 격리는 아니며 NAT/endpoint/Atlas allowlist/DB 권한은 별도 검증 대상이다. 불필요한 기본 all-egress 규칙을 남기지 않는다.
- 승인 요청안: HTTPS443에 우선순위30, exact host `identity-test.to-teacher.com` → `tosunsaeng-identity-test-tg` 전달 규칙 추가. 기존10/20/default/cert 설정 유지. 인터넷 접근 가능한 ALB 경로가 추가되며 앱 인가는 서버가 수행한다. 대상이 없으면 테스트 호스트는503을 반환할 수 있다.
- 이후 테스트 service `tosunsaeng-identity-test-service`, task `tosunsaeng-identity-test:1`, Fargate Linux platform1.4.0, 기존 사설 subnet 2개, 새 SG만 연결, public IP OFF, desired0, ECS Exec/Auto Scaling OFF로 생성한다. 인바운드 범위는 별도 명시 승인 후 적용한다.
- 이번에는 읽기 전용 조사와 문서화만 수행했다. 실제 SG/ALB/service 생성은 보안 접근 변경에 대한 사용자 최종 확인 대기다. DNS/라우트/NAT/endpoint 및 실제 송수신 성공은 아직 검증하지 않았다.

### 2026-09-23 테스트 Task Definition 등록 완료

- 사용자 승인으로 `tosunsaeng-identity-test:1` 등록 완료. ARN: `arn:aws:ecs:ap-northeast-2:889384901776:task-definition/tosunsaeng-identity-test:1`.
- AWS 생성 성공 메시지와 등록된 컨테이너/JSON 화면에서 테스트 execution role, 0.5vCPU/1GiB, DB 이름, issuer, 이미지 참조 및 Google/phone ON·Stage9/외부 발행 OFF 설정 확인.
- `identity-test-task-definition.draft.json`은 승인·등록한 입력을 보존한다. 파일명은 초안 작성 당시 이름이며 AWS가 추가한 기본값/등록 메타데이터까지 담은 export 파일은 아니다.
- DB 이름 `to-teacher-identity-test` 사용 승인을 받았으나 Atlas readWrite 권한과 실제 연결은 아직 검증하지 않았다. 이미지 태그는 준비용 참조이며 pull 가능 여부를 검증하지 않았다.
- Task Definition은 실행 설정 등록일 뿐이다. 테스트 service 생성, desired count 설정, task 실행, ALB/SG/DNS 변경은 이번에 수행하지 않았다. 따라서 테스트 서버 배포 완료나 현재 실행 수 0인 서비스가 존재한다고 해석하지 않는다.
- 다음 단계는 테스트 네트워크/대상 그룹의 ALB 연결 확인과 필요한 접근 변경 승인, desired=0 테스트 service 생성이다. 이후 실제 develop 이미지 배포와 기동 검증을 진행한다.

### 2026-09-23 초기 Task Definition 초안 — AWS 미등록

- [등록용 초안](identity-test-task-definition.draft.json)은 실행 가능한 설정 제안이지만 승인 및 아래 선행 검증 전 등록/기동하지 않는다. image는 현재 develop HEAD 참조이며 해당 태그가 ECR에 존재한다고 검증한 것은 아니다. 실제 첫 빌드 SHA가 달라지면 교체한다.
- 기존 운영 service의 revision 23에서 Linux/X86_64, CPU 512/메모리 1024MiB 확인. 기존 서비스는 desired/running 1, 사설 subnet 2개, public IP OFF, platform 1.4.0이다. 운영 task의 환경변수를 복사하지 않고 테스트 값으로 새로 구성했다.
- 제안: DB `to-teacher-identity-test`(Atlas 계정 readWrite 권한 확인 필요), issuer `https://identity-test.to-teacher.com`, kid `tosunsaeng-identity-test-rsa-1`, Access TTL PT30M, Refresh TTL P14D. 약관 버전 privacy-v1/term-v1/quality-review-v1은 예시 기반 제안이므로 프론트 테스트 동의 설정과 맞춰 승인한다.
- Google/phone/Firebase 및 phone fingerprint ON. Apple/Kakao, Stage 9 복구, session fence, Guest merge, Billing binding/publish, owner/withdrawn/merged 이벤트 발행 OFF. 기본 false인 나머지 worker는 켜지 않는다. Sentry/Swagger OFF, ECS JSON 로그 및 Mongo driver WARN. 사용자 API/JWT 발급 코드는 변경하지 않는다.
- Stage 9 OFF이므로 복구 keyring은 이 초기 task에 주입하지 않는다. 6개 전체 매핑 파일은 후속 활성화 참고용으로 유지한다. 활성화 시 session fence/active key/environment 및 프론트 계약을 별도 검증한다.
- 테스트 executionRole만 지정하고 taskRole은 부여하지 않는다. non-root 앱이 runtime secret 파일을 쓰므로 read-only root filesystem은 임의로 켜지 않는다. 로그는 non-blocking/10m buffer이며 과부하 시 유실 가능하므로 실제 수집 확인 필요.
- 생성 순서: 설정 승인 → task 등록 → 테스트 네트워크/ALB 연결 확인 후 desired=0 서비스 생성 → 사용자 workflow commit/push 및 이미지 배포 → 실제 이미지/Secret/DB 확인 후 desired=1 → health/JWKS/가입/로그 확인. desired=0에서는 health 검증 성공을 기대하지 않는다. 현 workflow는 마지막 public health 단계에서 실패할 수 있으므로 최초 기동 전 이를 성공 배포로 오인하지 않는다.
- 서비스 생성에는 테스트 TG의 ALB 연결과 SG 검토가 필요하다. 운영 SG를 무검증 재사용하지 않는다. 이번에는 task/service/라우팅/권한 변경 및 실제 기동을 수행하지 않았다.

### 2026-09-23 테스트 로그 그룹 완료 및 ECS 설정 준비

#### 저장 형식 확인 완료

- 사용자 요청에 따라 Secret 값 조회 UI를 열되 도구 출력에는 키 이름과 선택된 저장 형식만 반환했다. 원문을 기록하거나 파일에 저장하지 않았으며 확인 후 목록으로 이동했다.
- mongodb는 `MONGODB_URI`, phone-fingerprint는 `PHONE_IDENTITY_FINGERPRINT_KEY_RING` JSON key selector를 사용한다.
- firebase는 서비스 계정의 표준 필드가 최상위에 있는 JSON 전체를 주입한다. jwt-private-key, jwt-public-key, reissue-encryption-keyring은 일반 텍스트 전체를 주입한다.
- [ECS 컨테이너 secrets 배열](identity-test-container-secrets.json)을 실제 저장 형식에 맞춰 준비했다. Task Definition의 해당 컨테이너 `secrets`에 넣는 조각이며 독립적인 task 등록 문서가 아니다. JSON key selector는 Linux Fargate platform 1.4.0 이상을 사용한다.
- 저장 형식에 대한 이전 보류 사유는 해소됐다. Secret 값의 유효성, RSA 키 쌍 일치, Firebase 권한, Mongo 연결 및 실제 환경 설정은 별도 검증해야 한다. 이번에는 Secret 수정/task 등록/서비스 배포를 하지 않았다.

- 서울 CloudWatch에 `/ecs/tosunsaeng-identity-test` 생성 완료. 표준 클래스, 30일 보존을 생성 성공 메시지와 목록에서 확인했다. 운영 로그 그룹은 변경하지 않았다.
- 실행 역할의 로그 리소스와 그룹 이름이 일치한다. 실제 awslogs 전송은 task 기동 후 확인해야 한다.
- 다음 Secret 연결은 비밀값을 문서나 task environment에 직접 넣지 않고 ECS `secrets.valueFrom` ARN으로 지정한다. 저장 형식이 원문이면 ARN 전체, 키-값 JSON이면 해당 키 selector를 사용해야 하므로 실제 저장 형식 확인 전 task 등록을 보류한다.

| Secret 이름 끝부분 | 컨테이너 주입 환경변수 |
| --- | --- |
| mongodb | MONGODB_URI |
| firebase | FIREBASE_SERVICE_ACCOUNT_JSON |
| jwt-private-key | JWT_PRIVATE_KEY_PEM |
| jwt-public-key | JWT_PUBLIC_KEY_PEM |
| phone-fingerprint | PHONE_IDENTITY_FINGERPRINT_KEY_RING |
| reissue-encryption-keyring | AUTH_REISSUE_ENCRYPTION_KEYRING_CONTENT |

- Firebase 서비스 계정 JSON 전체를 저장한 경우는 JSON 내용 그 자체를 주입한다. 서비스 계정 JSON을 별도 상위 키의 문자열 값으로 저장한 경우만 그 상위 키 selector를 사용한다.
- [entrypoint](../../docker-entrypoint.sh)가 PEM·Firebase JSON·재발급 keyring을 private runtime 파일로 준비한다. `application-test.yml`은 단위 테스트용이므로 ECS 실행 profile로 사용하지 않는다.
- task/service는 아직 생성하지 않았다. 테스트 DB 이름/권한, 실제 동의 버전, issuer/kid, 기능 flag 및 Secret 형식, 최초 develop 이미지, 네트워크/라우팅을 맞춘 뒤 생성한다. 기존 main 이미지를 테스트용 develop 구현이라고 간주하지 않는다.

### 테스트 execution role — 2026-09-23 승인 후 생성 완료

- 제안 역할: `tosunsaeng-identity-test-execution-role`. [신뢰 정책](identity-test-execution-trust.json)은 동일 계정·서울 ECS task만 수임하도록 제한한다. 특정 task family로 수임을 제한하는 정책은 아니므로 PassRole 부여 대상을 관리해야 한다.
- [권한 정책](identity-test-execution-policy.json): Identity ECR 다운로드, 테스트 로그 스트림 생성/쓰기, 콘솔에서 ARN을 확인한 테스트 Secret 6개 GetSecretValue만 허용한다. Secret 원문은 열지 않았다.
- `/ecs/tosunsaeng-identity-test` 로그 그룹은 제안명이며 생성 여부 미확인이다. 실행 역할에는 CreateLogGroup을 주지 않으므로 배포 전에 별도로 생성하고 보존 기간을 정해야 한다.
- 광범위한 관리형 실행 정책, Secret 수정/삭제, 운영 Secret 읽기, 이미지 업로드, IAM 변경 권한을 주지 않는다. KMS Decrypt 추가 권한은 포함하지 않으며 고객 관리형 KMS 키 사용 시 해당 키 확인·별도 승인이 필요하다.
- ECS task definition의 executionRoleArn에 연결할 역할이며 애플리케이션 taskRoleArn으로 사용하지 않는다. 사용자 최종 승인 후 역할을 생성하고 `IdentityTestExecution` 인라인 정책을 연결했다. 저장된 신뢰 정책 및 권한 전문이 두 JSON과 일치함을 콘솔에서 확인했다.
- 생성 ARN: `arn:aws:iam::889384901776:role/tosunsaeng-identity-test-execution-role`. 실제 ECS 수임/Secret 주입/로그 전송은 아직 테스트하지 않았다. 이전 조사 항목의 execution role 미생성 상태는 이 완료 기록으로 대체한다. 로그 그룹·초기 task/service·배포는 여전히 후속 작업이다.

### 2026-09-23 승인 후 적용 완료 — CI 역할 및 GitHub 변수

- 사용자 승인으로 `tosunsaeng-github-identity-test-deploy-role`을 생성하고 `IdentityTestDeploy` 인라인 정책을 연결했다. AWS 저장 화면에서 아래 조사에 연결된 JSON과 동일한 정책 및 develop exact 신뢰 조건을 확인했다.
- GitHub repository variable `AWS_TEST_ROLE_ARN`을 `arn:aws:iam::889384901776:role/tosunsaeng-github-identity-test-deploy-role`로 생성하고 API GET으로 값 일치를 검증했다. 기존 `AWS_ROLE_ARN` 및 운영 IAM 역할은 수정하지 않았다.
- 테스트 execution role은 정책에서 참조만 하며 아직 생성하지 않았다. Secret 권한, 최초 Task Definition/service, 이미지, DNS/ALB 라우팅은 후속 준비 대상이다. 실제 OIDC 수임 및 Actions 배포는 실행하지 않았다.
- 아래 적용 전 조사는 당시 기록으로 보존한다. 공유 ECR 이미지 쓰기, TaskDefinition API 범위 및 운영 역할 wildcard 신뢰의 잔여 위험은 그대로다.

### 2026-09-23 권한 준비 조사 — 아직 적용 전

- 기존 운영 역할 tosunsaeng-github-identity-deploy-role은 저장소 전체 wildcard subject를 허용한다. 따라서 현재 운영 역할이 main 전용이라고 볼 수 없다. 이번에는 기존 정책을 변경하지 않았으며 별도 축소 검토가 필요하다.
- GitHub OIDC 설정 조회 결과 use_default=true, use_immutable_subject=true. sub_claim_prefix의 저장소/조직 ID를 포함한 develop exact subject로 [신뢰 정책 초안](identity-test-deploy-trust.json)을 작성했다. 실제 OIDC role assumption은 아직 검증하지 않았다.
- 새 역할 제안명: tosunsaeng-github-identity-test-deploy-role. [권한 초안](identity-test-deploy-policy.json)은 테스트 ECS 서비스 갱신 및 테스트 execution role PassRole만 허용한다. execution role 자체와 Secret 읽기 권한은 별도 준비/확인 대상이며 아직 생성하지 않았다.
- ECR는 기존 공유 저장소를 사용하므로 이 역할의 이미지 쓰기 권한 자체는 staging 태그 쓰기를 격리하지 않는다. workflow의 test 태그 분기와 IAM 격리는 다르다. 완전한 이미지 쓰기 격리가 필요하면 테스트 ECR를 분리해야 한다. 기존 운영 역할 wildcard 신뢰도 별도 잔여 위험이다.
- ECS RegisterTaskDefinition/DescribeTaskDefinition은 초안에서 Resource=*이며 테스트 family만 등록하는 IAM 제한을 주장하지 않는다. 서비스 갱신/PassRole을 제한하고 workflow에서 family를 검사한다. AdministratorAccess, IAM 변경, Secret 직접 조회, 다른 ECS 서비스 갱신은 부여하지 않는다.
- 생성 승인 후 GitHub repository variable AWS_TEST_ROLE_ARN에 새 역할 ARN을 등록할 예정이다. 장기 AWS key는 만들지 않는다.

- [기존 workflow 수정](../../.github/workflows/deploy-staging.yml)
- [분기 테스트](../../src/test/java/web/tosunsaeng/identity/deployment/DeploymentTargetTests.java): workflow에서 shell을 추출하여 main/develop/미지원 ref/누락된 test role 경로 실행. 나머지는 YAML 계약 정적 검사다.
- [Sentry release 계약 테스트](../../src/test/java/web/tosunsaeng/identity/deployment/SentryReleaseDeploymentTests.java) 유지.
- 실제 GitHub Actions/AWS 배포 및 IAM 권한 검증은 이 로컬 테스트에 포함하지 않는다.
