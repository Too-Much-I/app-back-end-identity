# Codex Current State

<!-- codex-turn:01a0cd90-1e27-76f1-a7fb-81e2903fa55f -->

- 테스트 인프라 생성 작업의 현재 식별 기록 보완 완료. 테스트 SG/ALB 규칙/ECS 서비스 생성 완료 및 desired0 상태 유지. 추가 외부 변경 없음. 원격 workflow 사용자 반영과 DNS 연결 후 실제 서버 기동 검증 필요.

- 2026-09-23 승인된 테스트 네트워크/service 생성 완료: test SG sg-03c6bd60c4026a227(ALB→8081, 송신443/27017), ALB priority30 exact test host→test TG, ECS tosunsaeng-identity-test-service ACTIVE/task test:1/desired0/running0 확인. private subnet 2개/public IP·Exec·AutoScaling OFF. 실제 기동 전 차단: 원격 develop workflow는 여전히 운영 대상으로 로컬 변경의 사용자 commit/push 필요. DNS test 호스트 NXDOMAIN으로 가비아 CNAME 연결 필요. 기존 운영 service/role/Secret은 변경하지 않음. 이번 실제 task 실행 없음.

<!-- codex-turn:01a0cd8e-0183-7a71-b125-f58325824bdb -->

- 프론트 접근 조건 안내의 현재 작업 식별 기록 보완 완료. ALB 경유 접근 구조 설명 완료, DNS/서버 기동/health 및 필요 시 CORS 검증은 남아 있다. 추가 외부 변경 없음.

- 2026-09-23 프론트 접근 조건 설명: 공인 ALB HTTPS를 통해 사설 테스트 ECS로 접근 가능하나 현재 desired0 생성 계획은 준비 단계다. DNS 연결, 승인된 SG/ALB/service 구성, 이미지 배포 및 desired1 기동/health 정상 확인 필요. Android 네이티브 HTTP와 달리 브라우저/WebView JS 요청은 CORS 별도 검증 필요(현재 Java 소스 검색에서 명시적 CORS 설정 없음). 이번 외부 변경 없음.

<!-- codex-turn:01a0cd8b-c543-7640-93f7-9b966322cf9b -->

- 테스트 네트워크 조사 작업 식별 기록 보완 완료. ALB/SG 확인 및 접근 변경안 작성 완료, 사용자 최종 승인 대기. 추가 외부 변경 없이 테스트 SG/ALB 규칙/service 미생성 상태 유지.

- 2026-09-23 테스트 서비스 네트워크 확인: ALB SG 식별 및 HTTPS 규칙10/20/default404 확인, 테스트 호스트 규칙 없음. 신규 test SG(ALB에서만8081, 송신443/27017), host exact 우선순위30→test TG, private subnet/desired0 테스트 service 구성안 작성. 접근 범위 변경 직전 사용자 승인 대기, AWS 변경 없음. NAT/endpoint/Atlas 및 실제 통신 검증은 남음.

<!-- codex-turn:01a0cd87-fb5b-70b3-8aa0-27a1763424cd -->

- 테스트 Task Definition 등록 작업의 현재 식별 기록 보완 완료. tosunsaeng-identity-test:1 등록 및 저장 설정 확인 완료 상태 유지. 추가 외부 변경 없이 서비스 생성 및 실제 기동 검증이 남아 있다.

- 2026-09-23 승인된 테스트 Task Definition tosunsaeng-identity-test:1 등록 완료. AWS 성공 메시지 및 컨테이너/JSON 저장 설정 확인(테스트 execution role, 0.5vCPU/1GiB, 테스트 DB/issuer, Google/phone ON, Stage9 및 외부 발행 OFF). Task Definition만 등록했으며 service/실행 수 설정/task 기동/라우팅 변경 없음. Atlas DB 권한·이미지 pull·실제 연결 검증은 남음. 초기 desired=0 service는 후속 구성 대상.

<!-- codex-turn:01a0cd84-68a9-7f42-aa4e-5d6c03b09e5a -->

- 초기 테스트 Task Definition 준비 작업 식별 기록 보완 완료. 초안과 JSON 검사 완료, DB명/권한 및 AWS 등록 승인 확인 대기. 추가 외부 변경 없이 task/service 미등록 상태 유지.

- 2026-09-23 테스트 task definition 초안 작성: 기존 운영 service/revision23 사양 조회(0.5vCPU/1GiB/Linux X86_64/private network), 운영 값 복사 없이 테스트 issuer/kid/DB/Secret/log 구성. Google/phone/fingerprint ON, Stage9 및 외부 publish OFF 제안. 미사용 recovery keyring 미주입. DB 권한/약관 버전/설정 승인, 이미지/네트워크/ALB 준비 후 등록 필요. AWS task/service 미생성, 실제 배포 미수행. JSON/diff 검사만 수행.

<!-- codex-turn:01a0cd7f-9fb8-7201-bfdb-61742a426b9d -->

- Secret 형식 확인 작업의 현재 식별 기록 보완 완료. 혼합 저장 형식에 맞는 ECS secrets 배열 준비 및 검사 완료 상태 유지. 추가 외부 변경 없이 task 등록/기동 검증이 남아 있다.

- 2026-09-23 테스트 Secret 저장 형식 확인 완료: MongoDB/fingerprint는 JSON key selector, Firebase 서비스 계정은 JSON 전체, JWT PEM 2개와 재발급 keyring은 일반 텍스트 전체. docs/contracts/identity-test-container-secrets.json 준비 및 JSON 검사 완료. 키 이름/형식만 출력·기록, 원문 파일 저장 없음. AWS 값/정책 미변경. task 등록 및 실제 연결 검증은 아직 미수행.

<!-- codex-turn:01a0cd7c-a139-7671-a9c7-6c89a0e1fe68 -->

- 테스트 로그 그룹 생성 작업 식별 기록 보완 완료. 30일 보존 설정 확인, ECS Secret 저장 형식에 대한 사용자 확인 대기. 추가 외부 변경 및 서버 기동 없음.

- 2026-09-23 테스트 로그 그룹 /ecs/tosunsaeng-identity-test 생성 완료(서울/표준/30일 보존). 성공 메시지·목록 확인. ECS Secret 주입 변수와 entrypoint 계약 검토 및 문서화. Secret 저장 형식(전체 원문 vs 상위 JSON 키) 확인 필요로 task/service 등록 전 중단. Secret 원문 미조회, 운영 변경 없음. 실제 서버/로그 전송 미검증.

<!-- codex-turn:01a0cd7a-0363-7810-b0c2-41eee45d0375 -->

- 테스트 실행 역할 생성 작업의 현재 식별 기록 보완 완료. 역할 및 저장 권한 검증 완료 상태 유지. 추가 외부 변경 없이 로그 그룹/task/service 준비와 실제 기동 검증이 남아 있다.

- 2026-09-23 실행 역할 생성 완료: 사용자 승인 후 tosunsaeng-identity-test-execution-role 및 IdentityTestExecution 정책 생성. AWS 성공 메시지, ARN, 저장된 정확한 6개 Secret 읽기/ECR pull/테스트 로그 스트림 쓰기 및 동일 계정 서울 ECS 신뢰 확인. 운영 role/Secret/GitHub 변수 미변경. 실제 ECS 실행 및 로그 그룹/task/service 구성은 미완료. 제품 변경 없음, 문서 갱신 및 diff 검사만 수행.

<!-- codex-turn:01a0cd77-0b9b-7433-a4ca-fb6294c8ca19 -->

- 2026-09-23 테스트 execution role 준비: Secrets Manager에서 테스트 Identity Secret 6개 ARN 확인(원문 미조회). 동일 계정/서울 ECS task 신뢰, 정확한 6개 Secret 읽기, Identity ECR pull, 제안 로그 그룹 /ecs/tosunsaeng-identity-test 스트림 쓰기 정책 JSON 작성. 역할 생성·권한 부여 직전 승인 대기, 외부 변경 없음. 로그 그룹 생성/보존 기간 및 task/service 준비는 후속. JSON/diff 검증, 제품 변경 없어 Gradle 미실행.

<!-- codex-turn:01a0cd6f-0d88-7c52-af7f-ccc9b1ac7f06 -->

- 2026-09-23 사용자 승인 후 테스트 CI 역할 tosunsaeng-github-identity-test-deploy-role 생성 및 IdentityTestDeploy 인라인 정책 적용 완료. AWS 저장 화면에서 develop exact immutable subject, sts audience, 정책 및 ARN 확인. GitHub AWS_TEST_ROLE_ARN repository variable 생성 후 GET 일치 확인. 기존 운영 role/AWS_ROLE_ARN/서비스는 변경하지 않음. 실제 OIDC 수임/배포 미실행. 테스트 execution role 및 Secret 최소권한은 별도 승인·구성이 필요하며 초기 task/service/이미지/DNS·라우팅도 남음. 공유 ECR 및 운영 role wildcard 신뢰 위험 유지. 이번은 외부 설정 및 문서만 변경, Gradle 재실행 없음.

<!-- codex-turn:01a0cd67-8814-7460-83fc-bc6cb5d1fef7 -->

- 2026-09-23 테스트 IAM 준비: 기존 운영 역할의 repo wildcard OIDC 신뢰 확인(운영 main 전용 아님). GitHub OIDC use_immutable_subject=true 및 실제 sub prefix 조회 후 develop exact 신뢰/권한 JSON 초안 작성. 새 테스트 배포 역할 및 AWS_TEST_ROLE_ARN 등록은 권한 생성 승인 대기, AWS/GitHub 변경 미수행. 공유 ECR 쓰기 및 기존 운영 역할 wildcard 위험 명시. execution role/Secret 권한/task/service는 별도 준비 필요. JSON jq 검사/diff check 통과.

<!-- codex-turn:01a0cd58-b624-7302-bc90-eaa3b78ea5d8 -->

- main/develop workflow 분리 구현의 현재 작업 식별 기록 보완. 전체 957 tests 통과, 실제 배포 미수행. 테스트 IAM 및 최초 ECS 서비스 준비 필요 상태 유지.

- 2026-09-23 기존 deploy-staging.yml 수정 완료: main 기존 서비스/staging 태그, develop 테스트 서비스/test 태그로 분기. 테스트 전용 AWS_TEST_ROLE_ARN 필수, 기타 ref 거절, ACTIVE 서비스 및 task family/container 선행 검사, 운영 fallback 없음. [배포 준비 문서](../contracts/identity-branch-deployment.md). 전체 clean test 147 suites/957 tests 성공, diff check 통과. commit/push/workflow 실행/AWS 변경 없음. 테스트 IAM/최초 task/service/DNS·라우팅 준비 후 적용 필요.

<!-- codex-turn:01a0cd55-55fd-7db1-be8c-6efb91efdc5c -->

- 테스트 대상 그룹 생성 및 이미지 선행 조건 확인 작업 식별 기록 보완. 대상 그룹 생성 완료, develop 이미지 업로드와 로컬 AWS 인증 준비는 미완료. 운영 배포/라우팅 변경 없음.

- 2026-09-23 테스트 인프라 준비: tosunsaeng-identity-test-tg 생성 완료(IP/HTTP:8081/동일 staging VPC/health /actuator/health, 대상0, ALB 미연결). 기존 서비스 3개만 존재. ECR Identity 28개 이미지 목록 최신은 main 7f2188df/staging이며 현재 develop bd337be4 이미지는 없음. 기존 workflow는 운영 서비스 갱신 대상이므로 실행하지 않음. 로컬 AWS CLI 자격증명이 없어 이미지 업로드 전 사용자 인증 또는 별도 승인된 빌드 경로 필요. ECS 서비스/Task Definition/권한/라우팅/DNS 미변경.

<!-- codex-turn:01a0cd53-2f36-70b1-9b6a-1743151c5642 -->

- ALB 테스트 인증서 추가 작업 식별 기록 보완. HTTPS:443 SNI 추가 성공 및 기존 기본 인증서 유지 확인 완료. 테스트 서비스/호스트 라우팅/DNS 연결 작업은 별도로 남아 있다.

- 2026-09-23 사용자 승인으로 tosunsaeng-staging-alb HTTPS:443에 identity-test.to-teacher.com 인증서를 SNI 추가 완료. AWS 성공 메시지와 인증서 2개 목록 확인. 기존 identity-staging 기본 인증서 유지. DNS/리스너 규칙/보안 정책/대상 그룹/서비스 변경 없음. 테스트 호스트 라우팅은 미구성 상태로 관측했으며 테스트 배포 및 도메인 연결은 별도 남음.

<!-- codex-turn:01a0cd51-d10b-7e50-9788-13a034c1d8bc -->

- 인증서 발급 확인 작업 식별 기록 보완 완료. ACM 발급됨/도메인 검증 성공/미사용 확인 결과 유지. ALB 연결은 아직 수행하지 않았다.

- 2026-09-23 재로그인 후 ACM 확인: identity-test.to-teacher.com 인증서 발급됨, 도메인 검증 성공, 사용 중 아니요. 발급 시각 2026-09-22 17:14:54 KST. 다음은 기존 인증서를 유지하면서 ALB HTTPS 443 인증서 목록에 추가하는 단계. 이번은 조회만 수행, ALB 변경 없음.

<!-- codex-turn:01a0cd4d-6c1e-7e11-a5d5-73866f13e334 -->

- 2026-09-23 테스트 인증서 검증 CNAME의 DNS 응답이 ACM 안내 값과 일치함을 확인. ACM 발급 여부는 AWS 세션 만료로 미확인. 사용자 재로그인용 ISB 페이지를 열고 탭 유지. DNS/ALB/인증서 설정 변경 없음. 현재 브랜치 develop.

<!-- codex-turn:01a0c824-820a-7552-89e7-d07cb6586ae1 -->

- 2026-09-22 ACM 현재 인증서 확인: identity-test.to-teacher.com 인증서는 DNS 검증 대기 중이며 사용 중 아님. 현재 화면의 검증 CNAME을 가비아 DNS에 등록하고 발급 후 ALB HTTPS listener에 추가하는 순서 안내. DNS/인증서/ALB 변경 및 탭 종료 없음.

<!-- codex-turn:01a0c7e0-658a-7753-b50a-0512c96c992b -->

- 2026-09-22 작업 순서 확인: 운영과 분리된 테스트 배포 및 SNS 인증/챌린지 검증을 먼저 진행할 수 있다. 구·신 주소 전환, 피드백 업데이트 안내, 기존 Guest 계승·재발급 응답 유실 검증은 실제 앱 업데이트 전 완료한다. 테스트는 운영 DB/키/이벤트 목적지와 격리하며 이번에 배포나 flag 변경을 수행한 것은 아니다.

<!-- codex-turn:01a0c7d6-ee64-73a3-ac1b-abdc66b23e35 -->

- 2026-09-22 전환 방안 가능 여부 재확인: 구/신 주소 분리와 피드백 result.updateRequired 기반 웹뷰 안내는 구현 가능하다. 구현·배포 완료 또는 세션 전환 안전성 검증 완료를 뜻하지 않는다. 기존 Guest 보존·진행 중 회전 응답 유실·신앱 Stage 9 지원 검증은 선행 과제로 유지. 이번은 설명/기록만 변경.

<!-- codex-turn:01a0c7c9-a219-76c3-9b27-3197f34475fb -->

- 2026-09-22 Guest 업데이트 전환 검토: 프론트 최신 main의 세션 보존 코드·오류 시 URL fallback 부재 확인. 신규 SNS 앱 revision/실기기 업데이트/운영 설정은 미확인. 구서버 회전 응답 유실 후 원 토큰은 신 복구 서비스에서도 재사용 감지 대상이며 안전 전환 완료로 판정하지 않는다.
- 사용자 확정: Learning Core도 구·신 주소 분리, 피드백 result.updateRequired는 구 true/신 false. Identity 도메인 밖이므로 LC 제품 코드는 수정하지 않았고 [검토 및 인계안](../contracts/guest-app-update-transition-review.md)에 정리. 재발급/세션 기존 테스트 두 클래스 성공, 전체 테스트 재실행 없음. 기존 미커밋 구현 보존, 배포·flag·Jira 변경 없음.

<!-- codex-turn:01a0c7c7-512c-71f3-8f05-64255056a05a -->

- 단일 기기 업데이트 전환 설명의 작업 식별 기록 보완 완료. 주소 분리 자체가 동일 세션의 혼합 처리를 뜻하지 않음을 정정했으며 실제 전환·공유 저장소 호환성 검증은 미수행이다. 제품 코드 및 운영 설정 변경 없음.

- 2026-09-22 업데이트 전환 위험 설명 보정: 한 기기에서 구 앱 종료 후 신 앱이 새 주소만 호출하고 동일 세션의 기존 요청이 남지 않는다면 두 서버가 그 세션을 번갈아 처리하는 상황은 아니다. 앞선 예시는 조건부이며 주소 분리만으로 자동 발생하지 않는다. 복구 ON을 반드시 유예 종료까지 미뤄야 한다는 일반화 대신 실제 단방향 전환·진행 요청·다중 기기와 계정 매핑·공유 데이터 호환성 검증 결과에 따라 판단하도록 안내. 코드/외부 설정 변경 없음.

<!-- codex-turn:01a0c7c5-9c23-7201-9ffa-a85cc14b8d36 -->

- 2026-09-22 주소 분리와 세션 공유 차이를 설명했다. 두 서버 운영 자체가 위험한 것은 아니며, 같은 계정·세션 DB를 쓰는 경우 구버전의 재사용 탐지가 해당 계정의 신규 세션까지 폐기할 수 있다는 조건부 위험이다. 실제 구/신 서버의 저장소 공유 및 사용자 매핑은 미확인. 코드/외부 설정 변경 없음.

<!-- codex-turn:01a0c7be-4404-7e62-9e0f-4b57fae620e7 -->

- 2026-09-22 구/신 앱 주소 분리와 1주 업데이트 유예 방안 검토: 가능하지만 주소 분리는 세션 격리가 아니다. 같은 계정/세션 DB에서 구 코드와 recovery ON writer 혼재는 승인하지 않으며 유예 중 recovery OFF 유지, 호환 서버·업데이트 안내·종료 후 정책 검증 뒤 활성화하는 방안을 권고했다. 웹뷰 안내는 로그인 실패 전에도 도달 가능해야 한다. Guest 승격/병합에 필요한 유효 세션 보존과 장기 미접속자 전환 경로 확인 필요. 실제 서버/웹뷰/flag 변경 없음.

<!-- codex-turn:01a0c7a4-fcb0-74f1-8d76-6579c3794f39 -->

- 2026-09-22 /reissue Idempotency-Key 필수 여부 재확인: 응답 복구 ON일 때만 필수, 기본 OFF에서는 필수 아님. 실제 배포 환경 설정은 미조회이므로 현재 운영 필수 여부는 단정하지 않는다. 코드/설정 변경 없음.

<!-- codex-turn:01a0c7a1-0787-7593-bad3-e3e2e3673c62 -->

- 2026-09-22 /reissue 만료 조건 확인: 기존 경로와 응답 복구 경로 모두 원 Refresh Token 만료 시 REFRESH_TOKEN_EXPIRED로 거절한다. Access Token 만료와 구분하며 복구 기간이 Refresh Token 수명을 연장하지 않음을 안내했다. 코드/외부 설정 변경 없음.

<!-- codex-turn:01a0c79a-b4c8-7e33-a1de-9aba35f232ec -->

- 재발급 계약 설명 작업 식별 기록 보완 완료. Guest/Member 공통 API, 응답 복구 flag와 동일 요청 ID 재사용 규칙, 구버전 전환 전 운영 활성화 금지 권고를 안내했다. 실제 운영 flag는 미조회이며 코드/외부 설정 변경 없음.

- 2026-09-22 재발급 프론트 계약 설명: /reissue는 Guest/Member 공통이며 Stage 9는 AUTH_REISSUE_RECOVERY_ENABLED로 선택하는 응답 복구 기능 이름이다. 기본 false와 활성 시 전 클라이언트 UUID v4 헤더 필수 조건을 코드에서 확인. 현재 배포 환경 ON/OFF는 이번에 조회하지 않았다. 동일 재발급 재전송은 원 Refresh Token+동일 ID, 다음 재발급은 새 ID. 구버전이 같은 API를 호출하면 ON 후 헤더 누락 400이므로 테스트 환경 검증과 구버전 업데이트 전환 선행 필요. 문서 상대 링크 대신 로컬 직접 링크 안내, 제품 코드/외부 설정 변경 없음.

<!-- codex-turn:01a0c78b-1ed3-79a2-83d8-7b5987912baf -->

- 2026-09-22 TMI-176 로컬 구현 완료, 브랜치 fix/TMI-176-identity-operability. R1 공통 JSON 요청 로그 중복 제거, R2 JWKS/자체 decoder 공개키 집합 공유 및 exact kid/RS256, R3 안전 DB 진단과 worker/scheduler 경고, R4 runtime Sentry release 매핑 반영. 최종 ./gradlew clean test --no-daemon 146 suites/952 tests 성공(실패·오류·skip 0), git diff --check 통과. 기존 미커밋 작업 보존, 예상 밖 변경 없음.
- [구현 결과/댓글 초안](../contracts/identity-operability-remediation-plan.md), [배포·키 전환 runbook](../contracts/identity-operability-runbook.md). 실제 외부 GitHub Action 렌더링/배포/CloudWatch/Sentry·Firebase/Atlas E2E는 미검증. commit/push/실제 키 교체/기능 flag/Jira 댓글·상태 변경 없음. 다음 단계는 사용자 diff 검토·commit/PR, 별도 테스트 배포 승인 및 검증이다.

<!-- codex-turn:01a0c787-e3d2-7e32-9ae0-34f490eb54ca -->

- 2026-09-22 Jira TMI-176 생성 완료: [Identity] 운영 안정성 보완 — 요청 로그·JWT 키 회전·DB 진단·Sentry release. 상위 TMI-136 에픽 연결과 해야 할 일 상태를 재조회 확인했다. 사용자가 이번 범위를 Jira 생성만으로 한정하여 구현 미착수; 제품 코드/배포/기능 설정 변경 없음.

<!-- codex-turn:01a0c786-950f-7712-8312-46a83682576e -->

- TMI-136 하위 작업 생성 준비 기록의 현재 작업 식별자 보완 완료. 생성안 승인 대기이며 Jira 생성/코드 구현은 아직 수행하지 않았다.

- 2026-09-22 운영 안정성 R1~R4 구현 요청 접수. 상위 Jira TMI-136(sns 로그인)은 에픽으로 확인했으며 하위 일반 작업 1건 생성안을 제시하고 승인 대기 중이다. Jira 생성 선행 요청에 따라 구현은 아직 시작하지 않았다. Jira 조회만 수행, 이슈/댓글/상태 변경 없음.

<!-- codex-turn:01a0c784-86b7-79e1-a022-6db91de18a0c -->

- 2026-09-22 운영 안정성 수정 계획 설명: 로그 출력 복구·키 교체 중 기존 토큰 검증·안전한 DB 장애 분류·Sentry 배포 버전 식별의 목적과 사용자 영향을 정리했다. 구현 승인 대기 유지. 이번은 설명 및 작업 기록만 변경했으며 제품 코드/외부 설정/배포 변경 없음.

<!-- codex-turn:01a0c777-e6a7-7963-9a79-0897e2e33423 -->

- 2026-09-22 [운영 안정성 수정 계획](../contracts/identity-operability-remediation-plan.md) 작성 완료, 구현 승인 대기. R1 requestId 중복 제거/실제 console 검증 → R2 로컬 active+retained 공개키 검증 → R3 안전한 장애 분류 → R4 runtime Sentry release mapping 순서. 제품 코드/기능 flag/외부 설정 변경 없음. API/JWT 유지, 실제 키 교체/배포는 별도 단계, 기존 미커밋 변경 보존.

<!-- codex-turn:01a0c76f-42cf-7c82-b086-06f227192ef7 -->

- develop 코드 리뷰 작업 식별 기록 보완 완료. 4건의 리뷰 결과 및 순수 커밋 896 tests 성공 기록 유지, 수정 승인 대기.

- 2026-09-22 순수 develop 5f37c135 코드 리뷰 완료: 별도 git archive 사본 전체 896 tests/139 suites 성공. 콘솔 로그 중복 결함과 **JWKS에 구키를 유지해도 Identity decoder가 active key/kid만 허용해 기존 토큰을 거절하는 회전 결함**을 임시 진단으로 재현. DB 원인 소실 및 SENTRY_RELEASE 전달 누락 포함 4건 정리. 제품 수정/외부 변경 없음, 승인 대기. [리뷰 상세](develop-code-review-2026-09-22.md). 이전 919개는 미커밋 포함 작업 트리 결과이며 이번 기준과 다름.

<!-- codex-turn:01a0c76d-0b9c-7263-9daf-eb601b7964ee -->

- develop 재검토 작업 식별 기록 보완 완료. 동일 로그 결함 확인, 제품 수정은 승인 대기 상태 유지.

- 2026-09-22 develop 재확인: 로컬 HEAD 5f37c135. 공통 로그/session/application.yml/workflow는 커밋과 작업 트리 동일하므로 requestId 중복 결함은 develop에도 존재. 직전 919 tests와 콘솔 재현은 develop+미커밋 작업 트리에서 수행한 결과다. recovery/fence 구현은 있으며 기본 OFF/설정 조합 확인 필요. 이번은 비교만, 제품 수정/추가 테스트/원격 fetch 없음. 상세 검토 문서에 기준 구분 보완.

<!-- codex-turn:01a0c764-4820-70f0-96e6-b68ede8bca35 -->

- 2026-09-22 운영성 검토: **P1 공통 요청 로그 누락 원인 재현 완료**. RequestLoggingFilter의 MDC/requestId key-value 중복으로 ECS JSON writer가 이벤트를 버린다. 200/401/500 모두 확인. 실제 CloudWatch에는 최근 24시간 서비스 성공 로그 15건만 관측, awslogs 연결 자체는 동작. ECS revision 23/이미지 7f2188df, task 1/ALB 정상 1. 제품 수정·운영 변경 없음; main hotfix 우선 권장.
- 검증: 임시 콘솔 진단 후 제거, 전체 ./gradlew clean test 최종 919 tests/141 suites 모두 성공. 기존 LogCapture는 실제 encoder를 거치지 않아 결함을 검출하지 못한다. DB 503 원인 정보 소실, 재발급 보호 OFF 경로 및 SENTRY_RELEASE runtime 전달도 후속 점검 대상. 상세: [운영성 검토](identity-operability-review-2026-09-22.md).
- 인증서 작업: 사용자 요청으로 생성된 identity-test.to-teacher.com 인증서가 검증 대기 상태임을 탭 유지 중 확인. 인증서/DNS는 이번 작업에서 변경하지 않았다.

<!-- codex-turn:01a0c741-c3d2-7551-89a2-d8a3111e9630 -->

- 인증서 요청 안내 작업 식별 기록 보완 완료. 사용자 직접 요청 대기이며 외부 설정 변경 없음.

- 2026-09-22 테스트 ACM 인증서 직접 요청 방법 안내: 서울/공개/identity-test.to-teacher.com/DNS 검증/RSA 2048/내보내기 비활성. 실제 요청 및 DNS 변경은 수행하지 않았으며 사용자 요청 후 검증 CNAME 등록 단계가 남는다.

<!-- codex-turn:01a0c73e-a183-7653-938c-90b2359ae202 -->

- 2026-09-22 재로그인 후 서울 ACM 확인 완료: 기존 발급/사용 중 인증서는 identity-staging.to-teacher.com과 api-staging.to-teacher.com만 포함하며 tosunsaeng-staging-alb에 연결돼 있다. wildcard 및 identity-test.to-teacher.com 미포함으로 테스트 주소에 재사용 불가. 별도 테스트 인증서와 가비아 DNS 검증이 다음 단계다. 이번 인증서/DNS/ALB 변경 없음; listener/rule 상세 미조회.

- 2026-09-22 사용자 지정 ISB URL을 새 탭으로 열어 AWS 로그인 화면 확인 및 탭 유지 완료. 사용자 로그인 대기, 인증서 조회/설정 변경 없음.

<!-- codex-turn:01a0c73d-0890-7892-b659-cff669962d5f -->

- 2026-09-22 서울 ACM 조회는 AWS 세션 만료로 중단. 재로그인 화면을 열어 두었으며 사용자 로그인 후 인증서 도메인/상태 확인 필요. 인증서/DNS/ALB 변경 없음.

<!-- codex-turn:01a0c735-a39f-7392-b717-2a8077edc548 -->

- 2026-09-22 공개 DNS 확인: to-teacher.com은 가비아 네임서버 사용. 제안된 identity-test.to-teacher.com A/CNAME 결과 없음. 다음은 서울 ACM 기존 인증서 적용 가능 여부 확인 및 필요시 DNS 인증/ALB 호스트 라우팅 준비. 네임서버/DNS/AWS는 변경하지 않았다.

<!-- codex-turn:01a0c732-25d8-7e73-9095-31dd4fc63f6e -->

- 2026-09-22 테스트 서버 준비 순서 안내: Firebase Blaze/SMS·Secret 준비 이후 테스트 주소/issuer 확정, 실행 역할, 확정 revision 이미지, Task Definition, 별도 서비스/HTTPS, DB·인증 E2E가 남는다. 로컬 미커밋 변경 존재 확인. 기존 workflow는 기존 서비스 대상이므로 테스트 배포에 그대로 쓰지 않는다. 이번 외부 설정 변경 없음.

<!-- codex-turn:01a0c6ec-4fd5-7422-ab54-b6287363a51a -->

- 2026-09-22 사용자 승인 후 Firebase SMS 리전 정책을 허용/KR 단독으로 저장했다. 처리 완료 후 미저장 변경 표시 제거 및 저장 비활성 확인. 예산 알림은 기존 설정 유지. 실제 SMS 발송/앱 E2E 및 테스트 번호 등록 상태는 미검증, 다른 외부 설정 변경 없음.

<!-- codex-turn:01a0c6e5-ce55-7301-bf91-b42dd3bc7966 -->

- 예산/SMS 준비 작업 식별 기록 보완 완료. 예산 알림은 저장됐으며 대한민국 SMS 허용 정책은 선택만 한 상태로 저장 승인 대기다. 이번 보완은 기록만 변경했다.

- 2026-09-22 Firebase 프로젝트 월 10000원/50·90·100% 이메일 예산 알림 생성·저장 확인(크레딧 차감 전, 알림만). 최신 provider 화면에서 Google/Phone 활성 확인. SMS 허용 목록은 비어 있어 KR만 선택했으나 저장 전 사용자 확인 대기. Phone 활성화/Identity Platform 변경은 agent가 수행하지 않았다. 테스트 번호 등록/앱 E2E 미확인, 비밀값 조회 없음.

<!-- codex-turn:01a0c6e4-3a39-76d1-b8e9-62fd9d3ec9a1 -->

- 2026-09-22 to-teacher-firebase의 기존 결제 계정 연결 및 Blaze 전환 완료를 UI에서 확인했다. 현재 Firebase 계정은 예산 조회 권한이 없어 예산 존재 여부는 미확인. 다음은 결제 관리자 계정의 예산 알림 확인, SMS 정책/테스트 번호 확인, 별도 테스트 ECS 준비다. 이번 외부 설정 변경 없음.

<!-- codex-turn:01a0c6e0-828d-7cc3-bc67-51a288311df5 -->

- 결제 계정 표시 확인 작업 식별 기록 보완 완료. Firebase에서 계정 선택은 가능하지만 Blaze 연결 완료는 아직 미확인이다. 이번 보완은 기록만 변경했다.

- 2026-09-22 Firebase 결제 계정 선택 창에 기존 계정이 나타난 것을 확인했다. 권한 반영은 확인됐지만 최종 연결/Blaze 전환 완료는 아직 아니다. 사용자 계정 선택 및 최종 과금 승인 단계가 남았고 외부 변경은 수행하지 않았다.

<!-- codex-turn:01a0c6de-40ff-7f52-9c41-ed67b8cffd2a -->

- IAM 저장 확인 작업 식별 기록 보완 완료. 권한 추가는 확인했으나 Firebase 결제 계정 목록 반영과 Blaze 연결은 아직 확인되지 않았다. 이번 보완은 기록만 변경했다.

- 2026-09-22 사용자 직접 IAM 저장 완료 확인: Firebase 관리 계정에 결제 계정 사용자 역할 및 업데이트 성공 알림 확인. Firebase 재조회에서는 아직 연결 가능한 계정 없음. 권한 전파 가능성 있으나 원인 미확정, 추가 권한 변경/실제 결제 연결 없음. 잠시 후 재확인 필요.

<!-- codex-turn:01a0c6db-925e-7461-8daa-6fd08ea964d4 -->

- 2026-09-22 결제 계정 IAM에 Firebase 관리 계정이 없는 것을 확인했다. 결제 계정 사용자 역할을 추가 양식에 선택했으나 저장하지 않았다. 권한 확대에 대한 저장 직전 사용자 확인 대기. 실제 IAM/Blaze 연결 변경 없음, 탭 유지.

<!-- codex-turn:01a0c6cd-6695-7cd2-aed7-32c27ddc67ff -->

- Firebase 결제 연결 준비 식별 기록 보완. 현재 연결 가능한 결제 계정이 없어 로그인/권한 확인 대기 상태이며 실제 연결/IAM 변경은 수행하지 않았다.

- 2026-09-22 Firebase 결제 연결 준비 중: Blaze 선택 창에 연결 가능한 Cloud Billing 계정이 없다고 표시된다. 기존 결제 계정은 확인됐으므로 새 계정을 만들지 않고 로그인/권한 확인이 우선이다. 실제 결제 연결/과금 승인/IAM 변경 없음. 사용자 후속 작업용 탭 유지.

<!-- codex-turn:01a0c6cb-b374-7aa1-881b-1ee58eabe9c8 -->

- 2026-09-22 콘솔 확인: Cloud Billing 결제 반영 및 무료 체험 활성 상태 확인. to-teacher-firebase는 여전히 Spark이므로 해당 결제 계정 연결/Blaze 전환이 남아 있다. 요금제/결제/IAM 변경은 하지 않았고 탭 유지. 실제 연결 가능한 계정 및 권한은 미확인이다.

<!-- codex-turn:01a0c30d-c642-75d2-8ec8-b5233eb982a2 -->

- 2026-09-21 배포 gate 확인: 현재 deploy-staging.yml은 main push/수동 실행 시 기존 tosunsaeng-identity-service를 갱신한다. 별도 테스트 배포가 아니므로 바로 실행하면 안 된다. 테스트 역할/Task Definition/Secret 매핑/서비스/HTTPS와 배포 revision 준비 필요. 이번 workflow/AWS 변경 및 배포 없음.

<!-- codex-turn:01a0c2fe-bbca-7ca3-80ae-f3966ec62b86 -->

- 2026-09-21 `develop`: 사용자 요청으로 폐기된 `POST /api/v1/auth/firebase/providers/relink/prepare` 서버 라우트·service stub·Swagger를 제거했다. 유효한 사용자 인증 요청은 404이며 기존 Security/미해결 시도 차단 로직은 유지한다. 현재 OpenAPI는 실제 Identity Controller 전체 23 paths/24 operations와 정상·재개·상태별·주요 오류 응답 예시 65개를 포함한다. DTO 직렬화 기반 예시는 live Swagger와 정적 공유본에 함께 반영된다. Provider unlink의 실제 202 문서화, link/unlink Status 모델 충돌 분리, Guest 생성 누락 schema와 FEDERATED 문서 enum을 보완했다. 기존 Provider link의 MEMBER용 ALREADY_LINKED와 JWT/세션 계약은 유지한다.
- 검증: `./gradlew clean test shareSwagger` 최종 성공(919 tests/141 suites, 실패·오류·skip 0), 별도 export 1 test 성공. Controller 전체 등록 목록 대조, 제거 경로 미노출/인증 시 404, 정상 응답 예시·schema 필드와 내부 참조, SDK 최초 start 허가/재시도 불허, ZIP 무결성·diff 검사를 통과했다. 결과는 `build/distributions/identity-swagger.zip`, JSON은 `build/frontend-swagger/identity-openapi.json`; `./gradlew shareSwagger`로 갱신한다. 학습 기록 삭제는 미구현 Learning Core 요구사항으로 포함하지 않는다.
- 다음 확인: 수신자의 ZIP 압축 해제 후 화면 렌더링, 구 relink 호출 미사용, 실제 배포 base URL/버전·feature flag 활성화. 이전 앱 브라우저의 로컬 HTML 접근 차단 때문에 화면 검증은 하지 않았다. 별도 Jira 쓰기·배포·commit·push 없음. 기존 Docker/entrypoint·배포 테스트/문서 및 선행 Swagger 변경 보존, 예상 밖 변경 없음.

<!-- codex-turn:01a0c2ee-e72a-7d52-9e6b-4debc18832d7 -->

- Bootstrap 재검토의 작업 식별 기록 보완 완료. 집중 21개 테스트 재실행 성공, 실제 ECS 연결은 미검증 상태 유지. 이번 보완은 기록만 변경했다.

- 2026-09-21 Secret bootstrap 재검토: 집중 21개 테스트 재실행 성공, 기존 전체 917개 성공 결과 확인. 복구 활성화 시 AUTH_SESSION_FENCE_ENABLED=true 필요를 코드로 확인. 이번 코드 수정/외부 변경 없음. 실제 Docker/ECS 권한/Secret 연결 및 인증 E2E는 아직 미검증이다.

<!-- codex-turn:01a0c2e8-57b2-71e1-8c67-6c8880cf85c8 -->

- 2026-09-21 develop: 컨테이너 Firebase JSON/복구 keyring 파일 공급 구현 완료. entrypoint는 기능별 필수값/원문-경로 충돌을 검사하고 기동별 700 폴더/600 파일과 JVM 원문 환경변수 제거를 적용한다. 기존 JWT/API/feature flag 유지. 집중 21개 및 전체 917개 테스트(140 suites) 통과, 실패/오류/skip 0. sh -n 및 diff 검사 통과. 기존·동시 프론트 문서 변경 보존. 실제 Docker/ECS/인증 E2E 미검증, AWS 변경/commit/push/배포 없음. 다음은 docs/contracts/identity-container-secret-bootstrap.md에 따라 테스트 Task Definition 매핑·권한·새 이미지 준비다.

<!-- codex-turn:01a0c2e5-515b-7321-8afe-9cccff56f1b1 -->

- 2026-09-21 `develop`: 프론트의 학습 기록만 삭제 요구사항을 가이드 5.4와 부록에 추가했다. 계정·로그인·SNS 연결·프로필·동의 유지, Learning Core 소유 기능으로 구분했다. 삭제 범위·API·배포는 미확정이며 해당 서비스 구현은 조회하지 않았다. 학습 통계/스트릭/단어장/파일 포함 여부, 처리 중 AI와 새 기록 경계, 완료 확인·멱등성을 후속 결정 항목으로 인계한다. 구매·이용권/체험 자격 초기화 제외는 제안으로 표시했다. 로컬 링크 57개·JSON 40개·diff 검사 통과, 문서만 변경해 Gradle 미실행. TMI-169 구현 범위를 확장하거나 새 Jira를 생성하지 않았다.

<!-- codex-turn:01a0c2e5-b424-7023-b6fc-d93df2c08068 -->

- 2026-09-21 다음 작업은 저장소 시작 스크립트 보완이며 인증 API 변경은 아님을 설명했다. 구현/배포는 아직 시작하지 않았고 기록만 갱신했다.

<!-- codex-turn:01a0c2db-619f-7b30-9a42-07c72d664808 -->

- 2026-09-21 TMI-169 프론트 인계 갱신 완료. 현재 브랜치는 `develop`이며 새 prepare 구현을 코드에서 확인했다. 프론트 가이드·부록에 중단 후 재개, 활성 ID 재사용/만료 교체, PROFILE 상시 반환, CONSENTS 화면 생략과 upgrade 필수 필드 구분, 정책 변경·recent-auth 오류, QA 판정표·코드 근거를 보완했다. 문서 로컬 링크 52개·JSON 예시 40개와 diff 검사 통과. 문서만 변경하여 Gradle 재실행 없음. 배포 버전/약관 본문 공급/실제 모바일 E2E 확인은 남아 있으며 Jira 댓글·상태 변경 및 commit·push는 수행하지 않았다. 기존 동시 작업 기록은 보존했다.

<!-- codex-turn:01a0c2db-14c8-7751-9894-61ae262ee8f2 -->

- 2026-09-21 develop에서 시작 스크립트 보완 범위를 분석했다. JWT 파일 공급은 기존 지원, Firebase JSON/복구 keyring은 추가 필요. 기능별 실패 처리/기존 파일 경로 호환/700 디렉터리와 600 파일/비밀값 로그 방지 및 회귀 테스트를 권장한다. 코드 구현과 AWS 변경은 하지 않았다.

- 2026-09-21 다음 테스트 배포 작업 안내: 현재 entrypoint는 JWT PEM 파일 생성만 지원한다. Firebase JSON과 응답 복구 keyring의 안전한 파일 공급을 먼저 준비하고 테스트 execution role/Task Definition/서비스/HTTPS 및 실제 인증 검증을 진행해야 한다. 이번 코드/외부 설정 변경 없음.

<!-- codex-turn:01a0c2d6-4241-7cf1-9ccd-5a3c419d50fe -->

- 테스트 복구 키 형식 확인 작업 식별 기록 보완. 사용자 예시 문법은 적합하며 실제 Secret 로딩은 미검증. 비밀값 기록/외부 변경 없이 동시 작업을 보존한다.

- 2026-09-21 사용자 제공 비실제 예시의 복구 keyring properties 형식 적합 확인. 예시 값은 기록하지 않음. 실제 Secret 값/서버 로딩 검증은 별개이며 ECS 파일 공급 준비가 남아 있다.

## 현재 작업 — TMI-169

<!-- codex-turn:01a0c2c6-6e44-7153-8451-e055695994f8 -->

- 2026-09-21 `feat/TMI-169-guest-enrollment-resume`에서 Guest SNS 가입 재개 및 enrollment 만료 계약 구현·검증 완료. commit·push·배포와 Jira 댓글·상태 변경은 수행하지 않았다.
- `/api/v1/auth/firebase/guest/prepare` 전용 `ALREADY_LINKED`를 제거했다. owner 없음은 활성 attempt 재사용 또는 만료 attempt 교체 후 `ENROLLMENT_REQUIRED`, 다른 ACTIVE MEMBER owner는 mutation 없는 `MERGE_REQUIRED`, 현재 Guest owner는 `409 IDENTITY_STATE_CONFLICT`로 처리한다.
- `ENROLLMENT_REQUIRED`에 `missingRequirements`, `privacyConsentVersion`, `termConsentVersion`을 추가했다. email·phone은 fresh Firebase proof, profile은 Guest 승격 시 항상 필요, 필수 동의는 저장 상태·현재 version·동의 시각을 모두 사용해 계산한다.
- `identity.firebase.guest.prepare{outcome=...}` 저카디널리티 counter와 Guest-owned conflict의 식별자 없는 구조화 WARN을 추가했다. Token, Firebase UID, provider subject, phone, userId, enrollmentId는 관측 값에 포함하지 않는다.
- 만료 attempt는 기존 `expiresAt` 애플리케이션 판정과 `cleanupAt` TTL 정책을 유지한다. 만료 ID upgrade는 기존 `FIREBASE_ENROLLMENT_CONFLICT`이며 프론트는 `/guest/prepare`를 다시 호출한다. Provider link의 MEMBER용 `ALREADY_LINKED`는 유지한다.
- 최종 `./gradlew clean test` 성공: 896 tests, 실패 0. 실제 Atlas·Firebase Provider는 호출하지 않았다.
- 관련 구현·계약: `FirebaseEnrollmentRequirementResolver`, Guest prepare service/DTO/result type/configuration/controller, `AuthErrorStatus`, 관련 단위·Controller·구성 테스트, `docs/contracts/firebase-guest-enrollment-resume-plan.md`와 Firebase 프론트 가이드·부록.
- 남은 작업: 사용자가 diff를 검토해 commit·push하고 PR을 생성한다. staging에서 활성 attempt 재사용, 만료 교체, MEMBER merge, `IDENTITY_STATE_CONFLICT` 0건 관측과 구버전 앱의 409 안전 중단을 확인한다. Jira 댓글은 초안만 제공하며 등록과 상태 전환은 별도 승인 후 수행한다.

- 2026-09-21 서울 Secrets Manager에서 reissue-encryption-keyring 저장 리소스 및 기본 KMS 암호화 확인. phone-fingerprint 목록 존재도 확인. 비밀값은 열지 않아 properties 형식/32바이트 검증은 미완료이며 실제 서버 주입과 구분한다. 외부 설정 변경 없음.

<!-- codex-turn:01a0c2d4-18c8-7302-8f17-1ac5b3747332 -->

- 2026-09-21 복구 keyring 저장 형식 재확인: 현재 코드는 JSON이 아닌 properties 원문을 읽으며 Base64 디코딩 결과 32바이트를 요구한다. 사용자 저장 완료 보고 후 형식 의문이 있어 비밀값 없이 증상을 확인하는 중. 실제 Secret 조회/수정 및 ECS 공급 검증 없음.

<!-- codex-turn:01a0c2d0-e07f-7251-bbcf-074b14febafb -->

- 2026-09-21 HMAC Secret 저장 완료는 사용자 보고 기준으로 정리. 다음 응답 복구 AES-256 키는 독립 생성하여 properties 원문으로 보관하도록 안내. 코드의 32바이트/절대 파일 경로 요구 확인. 실제 키 조회/외부 변경 없음; ECS 파일 공급과 기능 활성화는 미완료.

<!-- codex-turn:01a0c2cc-2837-7cf3-84ea-ae44913b8fe7 -->

- 2026-09-21 테스트 전화번호 fingerprint 키 준비: 로컬 develop에서 `v1,ACTIVE_WRITE,<표준 Base64>` 및 최소 32바이트 계약 확인. 사용자에게 독립된 테스트 키 생성/Secrets Manager key-value 저장 방법 안내. 실제 생성/저장/ECS 주입은 미확인. 현재 브랜치는 feat/TMI-169-guest-enrollment-resume이며 동시 구현 변경은 보존한다. 키 원문 조회/기록 및 외부 변경 없음.

<!-- codex-turn:01a0c2cb-0620-75a0-8c82-4db1668db613 -->

- 2026-09-21 서울 Secrets Manager에서 테스트 jwt-private-key/jwt-public-key 생성 확인. 테스트 mongodb/firebase 포함 네 리소스 존재 확인. 비밀값은 열지 않았으므로 PEM 형식/키 쌍 일치/실제 서명은 미검증이다. 다음은 HMAC/재발급 복구 암호화 키 준비 및 ECS 주입 설정. 이번 외부 변경 없음, 탭 유지 처리.

<!-- codex-turn:01a0c2c8-29d3-7ff0-bdbb-f6acd343cf72 -->

- 2026-09-21 RSA 키 안내 보완: 개인키는 Secret 보관 필수지만 서버 실행에는 현재 개인키/공개키 PEM 모두 필요하다. 공개키는 일반 설정으로 전달 가능하며 관리 편의상 별도 Secret에 보관하는 선택지도 안내했다(추가 비용 가능). 실제 키 조회/등록 없음. Learning Core는 JWKS로 공개키를 조회한다.

<!-- codex-turn:01a0c2c4-6779-7622-a251-e8674a484157 -->

- 2026-09-21 테스트 JWT RSA 키 준비 안내 기록 보완. 사용자 로컬 키 생성 및 개인키 Secret 보관 절차를 안내했으며 실제 키 생성/내용 조회/저장 검증은 수행하지 않았다. 다음은 키 준비 완료 후 테스트 ECS 주입 및 나머지 보안 설정 준비다.

이 문서는 Codex 세션 시작 시 추가 개발자 컨텍스트로 읽힌다. Secret, Token, Password, 실제 Key, 전체 MongoDB URI를 기록하지 않는다.

## 프로젝트

- 이름: `app-back-end-identity`
- 현재 단계: `hotfix/quality-review-consent`에 Guest 생성과 인증된 동의 PUT·GET의 Quality review 선택 동의·철회, 구버전 client·기존 Mongo 문서 호환, OpenAPI·README·설정·테스트를 구현했으며 전체 42개 suite·319개 테스트 성공
- 상태 기준일: 2026-08-15

## 현재 작업 — TMI-134

- 사용자에게 현재 hotfix 브랜치의 커밋/push 명령을 안내했다. tracked 변경과 신규 세 파일만 stage하고 poc/는 제외하도록 했다. 직접 stage/commit/push하지 않았으며 PR base는 main이다.

<!-- codex-turn:01a0b376-60f8-7b60-872e-8256489c0173 -->

- 구현 완료 작업의 현재 식별자를 보완했다. 전체 테스트 329개 통과 결과 유지, 실제 replica set 검증과 운영 활성화 배포는 미수행이다. 이번 보완은 기록만 변경했다.

- 최신 상태(2026-09-18): TMI-134 main 기반 구현 완료, 아직 commit/push/배포 없음. GuestAuthService가 기존 설치 및 등록 unique 충돌 뒤 GuestRecoveryTransactionService를 호출한다. GUEST_RECOVERY_ENABLED 기본 false, GUEST+ACTIVE+hash 조건 실제 사용자 쓰기와 세션 저장을 동일 Mongo transaction에 묶는다. 감사 이벤트 identity.guest.recovered에는 원문/해시/사용자 식별자를 넣지 않는다.
- 검증: ./gradlew clean test 43 suites/329 tests, 실패·오류·skip 0. diff check 통과. OFF 후 발급 세션 정상 Refresh 회전 및 병렬 복구/충돌/transaction 경계는 Mock 기반 검증. 실제 replica set 충돌/rollback은 배포 전 확인 필요. 절차와 Jira 댓글 초안: docs/contracts/guest-session-recovery-hotfix-TMI-134.md.
- 현재 변경 범위: 복구 서비스/호출부, 설정/OpenAPI/README/runbook, 회귀 테스트 및 기록. 기존 poc/ 보존·커밋 제외. 자동 종료/추가 횟수 제한 없음, 운영 true 명시와 수동 false 종료 절차 필요. 아래 미구현 상태 문구는 이전 시점 기록이다.

<!-- codex-turn:01a0b374-3825-74b3-ac79-fd02d7a27ec2 -->

- 브랜치 생성 작업 식별자를 보완했다. main 기반 hotfix 브랜치 전환 완료, 기존 기록 stash 보존 및 정책 인계 완료. 아직 구현·배포하지 않았다.

- 2026-09-18: main 3894627aa4d77740fe1e20ea882e349a48c738ab에서 codex/TMI-134-guest-session-recovery-hotfix 생성·전환 완료. 아직 구현·배포 없음. 위 quality review 내역은 기존 main 상태다.
- Jira TMI-134 구현 전 이슈 재조회. 정상 기존 Guest는 동일 userId에 새 Access/Refresh 발급, 미등록 설치는 기존 생성. Member/탈퇴/병합 제외를 main 모델에 맞게 검증. API/기록 보존.
- 기본 OFF 복구 flag/민감정보 없는 감사 기록. 자동 종료 시각·별도 요청 횟수 제한은 제외. SNS 전환 후 서버 수동 OFF. 기존 유효 세션과 정상 Refresh 회전 유지, 만료/로그아웃/탈퇴/보안 폐기는 유지. 설치 ID 신뢰 위험 수용, 삭제 세션의 과거 폐기 사유 확인 한계 존재.
- main과 실행 이미지 태그 일치 확인 완료. 앱은 특정 Refresh 401 이후 Guest 생성 409를 반복한다. Atlas TTL READY/잔존 532건/만료 0건/유효기간 14일 확인, 특정 장애 최초 401 원인은 미확정.
- develop 미커밋 기록 두 파일은 'TMI-134 preserve develop work records before main hotfix'라는 Git stash로 보존했다. 전환 후 기존 로컬 poc/가 미추적으로 표시되며 수정/삭제하지 않았다. hotfix에 포함하지 않는다. commit/push는 사용자 수행.

## 완료

- Spring Boot 프로젝트 및 Identity용 의존성 구성
- 저장소 Codex 작업 규칙과 Identity–Learning Core JWT 계약 문서화
- CURRENT_STATE/WORKLOG 작업 기록 체계와 Codex Hook 구성
- Codex 사용자 전역 설정에 Atlassian Remote MCP(`atlassian`) 등록 및 OAuth 연결 확인
- Atlassian MCP의 읽기 전용 조회로 `to-teacher` 사이트의 접근 가능 Jira 프로젝트 1개(`TMI`)와 이슈 생성 권한, 사용 가능한 이슈 유형 `에픽`·`하위 작업`·`작업`·`스토리`를 생성·수정 호출 없이 확인
- 승인된 Refresh Token 재발급·Rotation·재사용 탐지·멱등 로그아웃 Payload로 TMI `작업` 이슈 `TMI-6`을 `High` 우선순위와 기본 상태 `해야 할 일`로 생성하고 담당자·라벨·상태 전환은 적용하지 않음
- Jira `TMI-6`의 사용 가능한 전환을 재확인한 뒤 사용자 승인에 따라 transition ID `21`만 전송해 `해야 할 일`에서 `진행 중`으로 변경하고 다른 필드·댓글·이슈는 수정하지 않음
- Jira `TMI-6` 구현 완료 댓글 초안을 사용자 승인에 따라 댓글 ID `10000`으로 등록하고 상태 `진행 중`과 다른 필드·이슈는 변경하지 않음
- 사용자가 `TMI-6` 구현 PR의 main 병합과 전체 테스트 성공을 확인한 뒤, `완료` 전환 ID `41`을 재확인해 `진행 중`에서 `완료`로 변경하고 다른 필드·댓글·이슈는 수정하지 않음
- Atlassian MCP 읽기 전용 조회로 TMI의 이슈 생성 권한, `작업` 유형 ID `10003`과 생성 필드 20개를 확인하고 `High` 우선순위 지원을 검증한 뒤 JWT 인증·내 프로필·전체 로그아웃 작업의 최종 Payload 초안을 작성했으며 Jira 이슈는 생성하지 않음
- 승인된 JWT 인증·내 프로필·전체 로그아웃 Payload로 TMI `작업` 이슈 `TMI-9`를 `High` 우선순위와 기본 상태 `해야 할 일`로 생성하고 담당자·스프린트·에픽·라벨·상태 전환은 적용하지 않음
- Jira `TMI-9`의 방금 확인한 `진행 중` transition ID `21`만 사용자 승인에 따라 적용해 `해야 할 일`에서 `진행 중`으로 변경하고 다른 필드·댓글·이슈는 수정하지 않음
- Atlassian 공식 MCP로 Jira `TMI-9`의 설명·완료 조건·상태를 구현 전에 읽기 전용 재조회하고 AGENTS.md 및 JWT 계약과 충돌이 없음을 확인했으며 댓글·상태·필드는 변경하지 않음
- 사용자가 PR의 main 병합과 제시된 Jira 변경을 확인·승인한 뒤 TMI-9의 현재 상태와 사용 가능한 전환을 재조회하고 transition ID `41`만 적용해 `진행 중`에서 `완료`로 변경했으며, 후속 조회에서 status ID `10003`과 Resolution `완료`를 확인하고 댓글·필드는 변경하지 않음
- Atlassian 공식 MCP의 읽기 전용 조회로 TMI 이슈 생성 권한, `작업` 유형 ID `10003`, 생성 필드 20개와 `High` 우선순위 ID `2` 지원을 재확인하고 `[Learning Core] Identity JWKS 기반 JWT 인증 연동` 최종 Payload 초안을 작성했으며 Jira 이슈는 생성하지 않음
- 사용자 승인에 따라 `[Learning Core] Identity JWKS 기반 JWT 인증 연동`을 TMI `작업` 이슈 `TMI-10`으로 `High` 우선순위와 기본 상태 `해야 할 일`로 생성하고, 후속 조회에서 제목·유형·우선순위·상태와 담당자 없음·빈 라벨을 확인했으며 스프린트·에픽·상태 전환은 적용하지 않음
- Atlassian 공식 MCP로 `TMI-10`의 제목과 현재 상태 `해야 할 일`을 읽기 전용 재조회하고 사용 가능한 전환 `해야 할 일` ID `11`, `검토 중` ID `31`, `진행 중` ID `21`, `완료` ID `41`을 확인했으며 Jira는 수정하지 않음
- Jira `TMI-10`의 현재 상태와 `진행 중` 전환 ID `21` 사용 가능 여부를 재확인한 뒤 사용자 승인에 따라 transition ID `21`만 적용하고, 후속 조회에서 status ID `10001`의 `진행 중`을 확인했으며 다른 필드·댓글·이슈는 수정하지 않음
- 환경변수 기반 애플리케이션 이름, MongoDB 데이터베이스 및 서버 포트 설정
- Swagger UI `/swagger-ui.html` 및 OpenAPI `/v3/api-docs` 설정과 `SWAGGER_ENABLED` 환경변수 기반 활성화 제어
- Actuator health endpoint 노출
- 외부 MongoDB 연결을 생성하지 않는 격리된 테스트 프로필
- STATELESS·CSRF 비활성화를 유지하면서 명시한 공개 경로만 허용하고 나머지를 인증하는 Security 구성
- 폼 로그인, Basic 인증 및 Spring Security 기본 생성 계정 비활성화
- 제네릭 `BaseResponse` 성공·실패 Factory
- `ErrorCode`, `CommonErrorStatus`, `BusinessException` 오류 기반
- Validation 상세 민감값 제거와 예상하지 못한 오류 정보 비노출을 포함한 전역 예외 처리
- `.env.example`, 실제 환경 파일 ignore 및 README 실행·경계 문서
- 외부 인프라를 호출하지 않는 Bootstrap 테스트 9개 통과
- `users` 컬렉션의 UUID `userId` 기반 User Document와 `ACTIVE`, `SUSPENDED`, `WITHDRAWN` 상태 모델
- Jira `TMI-75` Task(작업)·High 이슈 범위로 보호된 `POST /api/v1/users/withdraw`를 구현했으며 Jira 상태·댓글·필드는 변경하지 않음
- 회원 탈퇴는 JWT `sub`, 요청 Refresh Token 해시의 Session 소유권·미폐기·미만료 상태를 검증하고 LOCAL만 현재 비밀번호를 재확인하며 GUEST는 비밀번호 없이 처리
- User 문서는 삭제하지 않고 `WITHDRAWN` tombstone으로 유지하며 서버 UTC `withdrawnAt`·`updatedAt`, 익명 nickname을 기록하고 email·normalizedEmail·passwordHash·guestInstallationIdHash는 Mongo `$unset`, 기존 userId·provider·createdAt·consents는 유지
- User tombstone 조건부 update와 사용자별 모든 미폐기 RefreshSession의 `ACCOUNT_WITHDRAWN` 폐기는 기존 `mongoTransactionManager` Transaction 하나로 처리하고, 충돌 시 최신 상태를 확인해 멱등 성공하거나 한 번 안전하게 재시도한 뒤 남은 충돌은 `WITHDRAWAL_CONFLICT` 409로 변환
- User 탈퇴 CAS와 동의 ACTIVE+updatedAt partial update로 stale User 전체 저장이 WITHDRAWN을 ACTIVE로 되돌리는 경로를 차단
- Guest 탈퇴 후 같은 installationId는 기존 WITHDRAWN User를 복구하지 않고 새 UUID·RefreshSession을 생성하며, LOCAL 탈퇴 후 같은 이메일도 새 UUID로 가입 가능하고 ACTIVE Guest 중복 409는 유지
- 원본 대소문자를 보존하면서 앞뒤 공백을 제거한 표시용 이메일과 `Locale.ROOT` 소문자 정규화 이메일 분리
- `normalizedEmail`의 `uk_users_normalized_email` unique index 및 MongoDB 자동 index 생성 설정
- BCrypt `PasswordEncoder` Bean과 평문을 Entity에 전달하지 않는 최소 `UserFactory`
- `Instant` 기반 생성·수정 시각과 신규 사용자 `ACTIVE` 초기화
- 정규화 이메일 단건 조회·존재 확인만 제공하는 `UserRepository`
- 실제 MongoDB 연결 없이 User 도메인 기반을 검증하는 테스트를 포함해 전체 23개 통과
- `git diff --check`, 평문 필드·토큰 필드·비소유 도메인 및 생성 경계 정적 검사 통과
- `POST /api/v1/auth/check-email` 이메일 중복 확인 API와 정상 응답 기반 사용 가능 여부 반환
- `POST /api/v1/auth/signup` 일반 이메일 회원가입 API와 Request/Response validation 계약
- 이메일과 닉네임의 앞뒤 공백 제거 후 validation 및 기존 `EmailNormalizer` 기반 중복 조회
- 정규화 이메일 사전 중복 확인과 MongoDB `DuplicateKeyException`의 `EMAIL_ALREADY_EXISTS` 변환
- 비밀번호 8~64자 길이 정책과 별도 복잡도 정규식 없는 BCrypt 해시 저장
- `EMAIL_ALREADY_EXISTS`, 개인정보·약관 동의 필수 및 정책 버전 불일치 도메인 오류와 공통 `BaseResponse` 오류 응답
- User 문서에 개인정보 처리방침·이용약관 상태, 버전과 서버 동의 시각을 `UserConsents` embedded 객체 하나로 저장
- LOCAL 회원가입과 Guest 생성은 두 동의가 true이고 서버 현재 버전과 일치할 때만 진행하며 같은 서버 시각으로 두 동의를 원자적으로 저장
- Guest 동의가 포함된 User와 최초 RefreshSession 저장은 기존 Mongo Transaction에 함께 참여
- `POST /api/v1/auth/guest`는 UUID v4 `installationId`와 개인정보·약관 동의 네 필드를 검증해 최초 Guest와 Token을 생성하며, 설치 ID는 인증 수단이 아니므로 동일 설치 재요청은 기존 Token 복구 없이 409로 거절하고 이후 실행은 저장한 Refresh Token으로 `/api/v1/auth/reissue`를 사용
- `GET /api/v1/users/me/consents`는 JWT `sub`의 ACTIVE 사용자만 조회해 서버 현재 필수 버전, 저장된 동의 상태·버전·시각과 정확한 문자열 비교 기반 `requiresConsent`를 개인정보·약관별로 반환
- `PUT /api/v1/users/me/consents`는 JWT `sub` 사용자만 대상으로 현재 필수 두 정책 동의를 한 User 문서 저장으로 갱신하고, 동일 버전 재요청은 저장과 동의 시각 변경 없이 멱등 성공
- Guest·LOCAL 생성, 동의·프로필·탈퇴, Guest·LOCAL 재가입, Transaction rollback, Security·OpenAPI, JWT·JWKS와 기존 인증 회귀를 포함한 38개 suite의 전체 284개 테스트 성공
- Repository를 Mock 처리한 Service·Controller 테스트와 전체 44개 테스트 통과
- 회원가입 성공 응답 및 오류 응답의 해시·정규화 이메일·자격증명·MongoDB 내부 정보 비노출 검증
- `app.jwt` 기반 issuer, audience, keyId, Access Token TTL, RSA Key Resource 경로 및 기본 scope 설정
- PKCS#8 Private Key와 X.509 Public Key만 지원하고 오류에 키 내용을 포함하지 않는 RSA Resource 로더
- `RsaKeyLoader`의 모호한 `ResourceLoader` 컴포넌트 자동 주입을 제거하고 `JwtConfiguration`에서 유일한 `ApplicationContext`를 명시적으로 전달해 `GridFsTemplate`과의 IDE 빈 후보 충돌을 해소하면서 기존 `rsaKeyLoader` 빈 이름과 Resource 해석 동작 유지
- RSA Key 타입과 Private/Public Key 쌍 일치 검증 및 민감값을 숨기는 Key Material 문자열 표현
- `RSAKey`, `JWKSet`, `JWKSource<SecurityContext>`, `NimbusJwtEncoder`를 사용하는 Spring Security 6.4 호환 서명 구성
- `RS256`, `SIGNATURE`, `kid` 메타데이터가 고정된 서버 내부 RSA JWK와 `Clock.systemUTC()` Bean
- UUID `userId` 검증, 고정 순서 scope 및 기본 scope fallback을 제공하는 내부 `AccessTokenIssuer`
- `sub`, `iss`, 단일 원소 배열 `aud`, `iat`, `exp`, UUID `jti`, 공백 구분 `scope`와 `alg=RS256`, `kid`, `typ=JWT`를 갖춘 Access Token 발급
- `GET /.well-known/jwks.json`에서 `toPublicJWK()` 결과만 표준 JWKS로 반환하고 BaseResponse를 적용하지 않는 공개 endpoint
- 로컬 RSA 2048비트 키 생성 스크립트, Private/Public Key 권한 설정, 명시적 `--force` 교체 및 `.local/` Git ignore
- Java `KeyPairGenerator`와 고정 Clock을 사용하는 JWT·JWKS·Key Loader 테스트 10개 추가 및 전체 54개 테스트 통과
- 실제 PEM 본문·서명 토큰·자격증명 포함 URI·민감 로그 부재와 JWKS/Access Token의 Private Key·자격증명 정보 비노출 검증
- 회원가입 응답과 외부 임시 endpoint에는 Access Token을 연결하지 않음
- `POST /api/v1/auth/login` 일반 이메일 로그인 API와 `LoginRequest`/`LoginResponse` validation·응답 계약
- 정규화 이메일 조회, BCrypt 검증, `ACTIVE` 상태 확인 후에만 토큰 발급을 진행하는 `LoginService.login`
- 존재하지 않는 이메일과 불일치 자격증명을 동일한 `INVALID_CREDENTIALS` 401로 처리하고 비활성 계정을 상태 구분 없는 `ACCOUNT_NOT_ACTIVE` 403으로 처리
- 로그인 성공 시 검증된 User의 UUID를 기존 `AccessTokenIssuer`에 전달해 기존 RS256 Header·Claim·기본 scope·TTL 계약을 그대로 사용
- 기존 `IssuedAccessToken`의 `issuedAt`과 `expiresAt` 차이를 milliseconds로 계산해 `accessTokenExpiresIn`에 반환하고 `grantType`은 `Bearer`로 고정
- `app.refresh-token`의 `Duration` TTL과 최소 32바이트 난수 길이 설정, 기본값 `P14D`와 32 및 32 미만 시작 거부
- `SecureRandom`과 Base64 URL-safe without padding을 사용하는 JWT가 아닌 Opaque Refresh Token 생성기
- UTF-8 원문에 SHA-256을 적용한 뒤 Base64 URL-safe without padding으로 인코딩하는 일관된 Refresh Token 해시기
- `refresh_sessions` 컬렉션의 UUID 기반 세션·사용자·회전 패밀리 관계, 생성·만료·최근 사용·폐기 시각, 폐기 사유와 `@Version` 기반 Optimistic Lock 모델
- Refresh Token 해시 unique index와 `expiresAt`의 `expireAfter = "0s"` TTL index, 해시 단건 조회 및 사용자별 미폐기 Session 조회만 제공하는 `RefreshSessionRepository`
- 공용 `Clock` 기준 최초 Session과 Rotation 후속 Session을 발급하고 Refresh Token 원문은 내부 발급 결과로만 반환하는 `RefreshSessionIssuer`
- `POST /api/v1/auth/reissue`에서 해시 조회, ROTATED 재사용 판별, 명시적 만료 경계 검사, `ACTIVE` 사용자 확인, 기존 Session Optimistic Lock 폐기 성공 후 새 Access Token과 Refresh Token 발급
- Rotation 시 기존 Session은 `ROTATED` 사유와 후속 관계를 저장하고 새 Session은 같은 회전 패밀리와 이전 관계를 유지하며, 최초 로그인 Session은 새 UUID 회전 패밀리를 생성
- 같은 Refresh Token의 동시 재발급에서 기존 Session 저장 충돌을 `INVALID_REFRESH_TOKEN`으로 변환하고 충돌 요청에는 Access Token 발급이나 후속 Session 생성을 수행하지 않음
- 이미 Rotation된 Refresh Token 재사용 시 사용자별 미폐기 Session 전체를 `REUSE_DETECTED` 사유로 폐기하고 `REFRESH_TOKEN_REUSE_DETECTED` 401 반환
- 존재하지 않는 Refresh Token은 `INVALID_REFRESH_TOKEN` 401, `expiresAt <= Clock`은 `REFRESH_TOKEN_EXPIRED` 401, 비활성 사용자는 기존 `ACCOUNT_NOT_ACTIVE` 403 정책 적용
- `POST /api/v1/auth/logout`에서 활성·미만료 Session만 `LOGOUT` 사유로 폐기하고 없는·이미 폐기된·만료된 Refresh Token은 성공 처리하는 멱등 흐름 구현
- 재발급·로그아웃 Request의 `NotBlank`와 최대 512자 제한, Refresh Token validation 값 마스킹 및 Request·Response 문자열 redaction 적용
- `POST /api/v1/auth/logout`은 Bearer 인증 없이 요청한 Opaque Refresh Token의 해시로 세션 한 건을 찾고, 활성·미만료 세션만 현재 시각과 `LOGOUT` 사유로 폐기하며 없는·이미 폐기된·만료된 세션은 200 성공으로 멱등 처리
- Reissue 응답의 Access Token·Refresh Token 만료 기간을 milliseconds로 반환하고 내부 사용자·세션·해시·비밀번호 관련 필드를 외부 응답에 포함하지 않음
- 실제 Atlas와 운영 키를 사용하지 않는 Service·Controller·도메인 회귀 테스트를 포함해 전체 97개 통과, 실패·오류·건너뜀 0개
- RefreshSession 원문 필드·Access Token 영속화·민감 로그·운영 자격증명 하드코딩 부재와 외부 응답 내부 필드 비노출 검증
- 기존 `RSAPublicKey`, `JwtProperties`, `Clock`을 재사용하고 자기 JWKS를 HTTP 호출하지 않는 `NimbusJwtDecoder` 구성
- Decoder에서 RS256 서명, 엄격한 `typ=JWT`, 현재 `kid`, 필수 `sub`·`exp`, 주입 Clock 기반 `exp`·`nbf`, issuer와 audience를 `DelegatingOAuth2TokenValidator`로 검증
- 공개 인증 POST 5개, JWKS·health·Swagger/OpenAPI GET만 `permitAll`로 두고 `anyRequest().authenticated()`를 적용하며 Form Login과 Basic 인증은 계속 비활성화
- Security Filter Chain의 401 `COMMON_UNAUTHORIZED`와 403 `COMMON_FORBIDDEN`을 UTF-8 `BaseResponse` JSON으로 반환하고 내부 JWT 예외나 입력 자격증명을 노출하지 않음
- `JwtCurrentUserProvider`가 인증된 `JwtAuthenticationToken` 또는 JWT principal의 `sub`만 읽고 canonical UUID로 검증해 내부 사용자 식별자로 제공
- `GET /api/v1/users/me`에서 JWT `sub`로 User를 조회하고 `ACTIVE` 상태를 확인한 뒤 provider와 개인정보·약관 동의 상태·버전·서버 시각을 전용 DTO로 반환
- User에 최소 `UserProvider.LOCAL` 모델을 추가하고 기존 문서의 null provider는 LOCAL로 읽어 현재 이메일 계정과 호환
- `POST /api/v1/auth/logout-all`에서 JWT 사용자의 미폐기 RefreshSession만 조회해 같은 Clock 시각과 `LOGOUT_ALL` 사유로 `saveAll`하며 빈 Session 목록과 반복 요청은 성공 처리
- logout-all은 새 Access Token이나 Refresh Token을 발급하지 않고 Repository 오류를 성공으로 숨기지 않으며 기존 `@Version` Optimistic Lock 구조를 유지
- Spring Security test 지원을 추가하고 실제 외부 인프라 없이 공개·보호 경로, Decoder 실패, scope 권한 변환, 프로필 노출 경계와 전체 로그아웃 사용자 격리를 검증
- `domain.auth`와 `domain.user` 아래에 API·application·DTO·domain·exception을 배치하고 공통 설정·응답·예외·Spring Security 기술 구현을 `global` 아래로 이동
- MongoDB 상태 객체 `RefreshSession`·폐기 enum·Repository를 Auth 도메인에 두고 Refresh Token 난수 생성·SHA-256 해싱·설정은 `global.security.refresh`로 분리
- 비대했던 `AuthService`를 `EmailAvailabilityService`, `SignupService`, `LoginService`, `TokenReissueService`, `LogoutService`로 분리하고 기존 `LogoutAllService`와 함께 Controller가 유스케이스만 호출하도록 구성
- 로그인·재발급의 다중 토큰 응답 조합은 `AuthResponseConverter`로 통합하고 `AuthException`·`UserException`은 기존 `BusinessException`과 오류 코드/HTTP 상태 계약을 유지
- OpenAPI 제목·설명·버전과 `bearerAuth` JWT 스키마를 추가하고 Auth·User·JWKS Tag, API 응답, DTO Schema를 문서화하며 보호 API 두 개에만 Bearer 요구사항 적용
- 공개 API의 Swagger 인증 표시 부재, 보호 API의 Bearer 표시, 비밀번호 write-only와 비밀번호 예시 부재를 `/v3/api-docs` 계약 테스트로 검증
- malformed JSON, 미존재 리소스, 지원하지 않는 Method와 Media Type을 각각 안전한 공통 400·404·405·415 응답으로 변환하고 요청 원문·내부 예외 정보를 노출하지 않도록 전역 예외 처리 보완
- malformed JSON 보안 테스트의 미완성 JSON 문자열을 지역 변수로 분리해 IDE 파서 혼동을 줄이면서 기존 400 응답·민감 입력 비노출 검증 의미를 유지
- 계정 활성 상태 오류 소유권을 User 도메인으로 일원화하고 converter의 application 내부 결과 타입 역참조를 제거해 Auth → User와 application → converter의 단방향 의존으로 정리
- 리팩토링 신규 파일과 기존 삭제를 함께 stage해 104개 논리 변경 파일, rename 68개, untracked 0개 상태를 구성하고 삭제 전용 index 문제를 해소
- 관련 테스트와 기존 signup·login·reissue·logout·JWT·JWKS 회귀를 포함한 전체 146개 통과, 실패·오류·건너뜀 0개이며 `./gradlew build` 성공
- 실제 JAR 기동으로 health·Swagger/OpenAPI 200, 공개·보호 operation 구분, malformed JSON 400, 미존재 경로 404, 무인증 보호 API 401, 공개 로그인 validation 접근과 415를 확인하고 Swagger 비활성 문서 경로의 404를 검증
- main 병합 후 인증 유스케이스·RefreshSession·JWT·Security·예외 처리의 비자명한 의도에만 한국어 한 줄 주석 25개를 추가하고 실행 코드는 변경하지 않은 채 전체 146개 테스트와 build를 재검증
- 환경변수로 조정 가능한 ECS 구조화 stdout과 `X-Request-ID` 검증·생성·응답 전달·MDC 정리를 구현하고, HTTP 완료 로그에 event·outcome·requestId·method·route template·status·duration·errorCode를 기록하며 health·Swagger/OpenAPI·JWKS 정상 요청은 제외
- 예상 밖 5xx의 단일 ERROR 소유자를 요청 완료 filter로 두고, `GlobalExceptionHandler`는 원본 message·Throwable 없이 예외·cause 타입과 최대 24개 stack frame의 안전한 오류 문맥만 request attribute로 전달하며 `errorLogged` guard로 ERROR/Error dispatch 중복을 방지
- 회원가입·Guest 생성·로그인·Refresh Token 재발급·재사용 탐지·동시 Rotation 거절·단일/전체 로그아웃·동의 갱신·회원 탈퇴 성공/멱등/충돌과 Mongo Transaction capability 검증을 저장 또는 transactional proxy 반환 이후의 구조화 상태 전이 이벤트로 기록
- 요청 로그와 상태 전이 로그의 레벨·필드·MDC 정리·민감값 비노출, 예상 밖 5xx ERROR 1건과 INFO 0건, BusinessException ERROR 0건, Security 401 requestId 전파를 포함해 전체 40개 suite·292개 테스트 성공
- PR #15의 main 병합 후 실제 애플리케이션 기동에서 Spring Boot의 ECS 설정이 애플리케이션뿐 아니라 Spring·Tomcat·MongoDB 드라이버의 모든 콘솔 로그를 한 줄 JSON으로 변환하고, `mongodb.transaction_capability.verified` 사용자 정의 event가 구조화 필드와 함께 출력되는 것을 확인
- `LogoutAllService`의 Token issuer 비의존성 테스트에서 `Stream<Class<?>>`와 `List<Class<?>>` 대입 모두에 발생한 wildcard capture 문제를 제거하기 위해 reflection field를 `anyMatch`로 직접 비교하고 boolean을 AssertJ로 검증하며, 같은 두 의존성 부재 의미를 유지한 채 전체 40개 suite·292개 테스트 재통과
- 애플리케이션이 직접 기록하는 18개 구조화 로그 `message`를 일관된 한글 문장으로 변경하고, 운영 검색 계약인 `event`·`outcome`·`errorCode`와 ECS 표준 key는 영어로 유지했으며 HTTP 정상/실패와 주요 상태 전이의 정확한 한글 message·민감정보 비노출을 테스트
- Sentry 공식 Spring Boot 문서에서 Gradle plugin `6.18.0`이 Spring Boot 3에 맞는 Jakarta starter를 자동 선택함을 확인하고, runtime DSN·environment·release와 PII·request body·trace sampling·test 비활성 설정이 build plugin 외에 별도로 필요함을 검토
- 현재 `GlobalExceptionHandler`가 예상 밖 예외까지 처리하므로 Sentry 기본값의 unhandled-only 수집에서는 5xx가 누락될 수 있고, exception resolver 순서를 앞당기면 Business·Validation 4xx까지 수집될 수 있으며 SentryAppender 기본 ERROR issue와 기존 `http.request.failed`가 중복될 수 있음을 확인
- Sentry 구현에는 실제 DSN 전달이 필요하지 않으며 저장소에는 `${SENTRY_DSN}` placeholder와 비밀값 없는 `.env.example` 이름만 추가하고, 실제 DSN은 사용자가 로컬 비추적 환경 파일 또는 배포 Secret에 직접 주입하는 작업 경계를 확정
- Sentry 적용을 dependency 확인, 안전한 runtime 기본값, 예상 밖 오류 단일 capture, event 정제, 격리 테스트, CI source context, staging 검증·점진 활성화 순서로 나누고 expected 4xx 0건·unexpected 5xx 1건·민감정보 0건을 완료 조건으로 확정
- Sentry JVM Gradle plugin `6.18.0`과 SDK `8.42.0`을 적용하고 Spring Boot 3 Jakarta starter·Logback 모듈의 runtime dependency 해석을 확인했으며 source context는 비공백 build 인증 값이 있는 CI에서만 opt-in
- Runtime은 기본 disabled·빈 DSN, PII false, request body `NONE`, tracing·profiling·Sentry Logs off와 Logback integration off로 구성하고 test profile도 명시적으로 비활성화했으며 실제 값 대신 환경변수 이름만 추가
- `GlobalExceptionHandler`가 처리한 예상 밖 Exception만 request scope를 비운 뒤 안전한 requestId·errorCode·HTTP method·route template·500 tag와 함께 명시 capture하며 Business·Validation·Security 4xx 및 handler resolver 순서는 변경하지 않음
- `beforeSend`는 SDK가 조립한 event를 부분 마스킹하지 않고 새 event로 재구성해 exception message, request/response 정보, Header·Cookie·query·body, user, breadcrumb, extra, thread, context, modules와 unknown field를 제거하고 message 없는 예외 type·정제 stack frame, 안전한 tag·설정 environment/release 및 source context용 UUID 형식 JVM debug bundle ID만 유지
- 실제 Sentry pipeline의 event processor가 모든 민감 위치에 동일 sentinel을 삽입한 뒤 test transport가 받은 최종 event JSON 전체에서 sentinel 0건을 검증하고, 기존 `http.request.failed` ERROR가 발생해도 handled 5xx event 정확히 1건·expected 4xx 0건·Sentry Logback initializer 부재를 확인해 전체 41개 suite·295개 테스트 성공
- Sentry 배포 시 Runtime에는 `SENTRY_ENABLED=true`, 배포 Secret의 `SENTRY_DSN`, 환경 구분용 `SENTRY_ENVIRONMENT`, 배포 식별용 immutable `SENTRY_RELEASE`를 설정하며 실제 비밀값 성격의 필수 입력은 DSN 하나임을 확인. `SENTRY_AUTH_TOKEN`은 source context 업로드를 승인한 CI build에서만 선택적으로 사용하고 Runtime에는 주입하지 않음
- 외부 통신 없는 `SentryCaptureIntegrationTests`로 handled 5xx 한 건·expected 4xx 0건·민감 sentinel 0건을 확인하고, 임시 staging one-shot trigger로 실제 DSN·SDK transport·Sentry project 수신 경계를 확인했다. 사용자가 project 표시를 확인해 실제 연결 검증이 완료됨
- 실제 검증 완료 직후 `SentryStagingSmokeTrigger`, 전용 조건·통합 테스트, smoke 설정·환경변수와 README 안내를 제거하고 `sentry.enabled=false`, `sentry.environment=local` 안전 기본값을 복원했다. 임시 trigger가 Runtime 또는 test artifact에 남지 않으며 전체 41개 suite·295개 테스트 성공
- 비HTTP event에서 `http.method` tag가 없을 때 `beforeSend`가 실패하지 않도록 한 null-safe allowlist 보완은 일반 Sentry event 안정성에 유효하므로 유지했다. Runtime event 대상 project는 계속 주입된 DSN이 결정하고 source context용 Gradle project 설정과 분리됨
- Quality review 선택 동의를 `POST /api/v1/auth/guest`, `PUT /api/v1/users/me/consents`, `GET /api/v1/users/me/consents`에 추가하고 LOCAL signup request와 프로필 응답은 유지했다. request 필드 누락은 false로 호환하고 true일 때만 current version exact match를 요구하며 false는 stale version으로 차단하지 않음
- `UserConsents`에 quality review 상태·version·서버 시각을 추가하고 false→true, true/current→true no-op, true/old→true/current 갱신, true→false 정리, false→false no-op을 immutable 결과로 구현했다. LOCAL signup은 false/null/null, Guest는 요청 선택값으로 생성하며 기존 embedded 문서 필드 누락도 false/null/null로 읽음
- 실제 변경일 때만 User `updatedAt`을 변경하고 기존 ACTIVE+`updatedAt` CAS로 `consents` embedded object 전체를 교체해 철회 후 과거 quality review version/time이 남지 않게 했다. 동일 상태 요청은 저장과 동의 시각 변경 없이 성공하며 로그에는 동의 상세값·시각·요청 본문을 추가하지 않음
- GET의 `qualityReview`는 저장 true·current version·서버 시각을 모두 만족할 때만 `consented=true`이며 선택 동의이므로 `requiresConsent=false`를 유지한다. PUT 응답에는 저장된 quality review 세 필드를 추가하고 request version은 trim·최대 100자·영문/숫자/점/밑줄/하이픈 형식으로 검증
- `QUALITY_REVIEW_CONSENT_VERSION`, `QUALITY_REVIEW_CONSENT_VERSION_MISMATCH`, OpenAPI·README·계획 문서를 갱신하고 신규 `UserConsentsTests`와 Guest·서비스·Controller·Mongo mapping·OpenAPI 회귀를 추가했다. 최종 `./gradlew clean test`는 42개 suite·319개 테스트, 실패·오류·건너뜀 0개이며 `git diff --check` 성공
- 종료 Hook 재검사에서 Quality review 구현 결과, 전체 42개 suite·319개 테스트 성공, commit·push 미수행 상태와 작업 기록을 현재 turn marker로 다시 동기화했으며 애플리케이션 코드는 추가 변경하지 않음

## 진행 중

- Jira `TMI-75` — 구현 commit `672b631`과 GitHub PR #14의 main 병합을 확인했으며 Jira 상태는 `해야 할 일`로 유지 중이다. 회원 탈퇴 관측 로그까지 구현했고 Jira 댓글·상태는 변경하지 않음
- Quality review 선택 동의 hotfix — 구현과 로컬 전체 검증 완료, 사용자 diff 검토와 commit·push·main 대상 PR 생성 대기 중이며 연결된 Jira 키는 확인되지 않음

## 다음 작업

- 사용자가 Quality review hotfix diff를 검토한 뒤 직접 commit·push하고 PR base를 `main`으로 지정하며 `develop`의 Firebase·TMI-95·TMI-96·refactor 변경이 포함되지 않았는지 PR diff에서 재확인
- 배포 환경에 비공백 `QUALITY_REVIEW_CONSENT_VERSION`을 추가하고 backend 호환 배포 후 프론트가 두 Quality review request 필드를 항상 보내며 GET·선택·철회 UI를 사용하는 순서로 staging 검증
- 회원 탈퇴 tombstone에서 quality review true snapshot을 보존할지 자동 철회할지 제품·개인정보 기준으로 확정하고, 현재 hotfix는 기존 탈퇴 동작을 변경하지 않음
- Quality review의 실제 답안·음성 이용은 versioned outbox event, 데이터 소유 서비스의 멱등 consumer, 철회 이후 보존·삭제 정책과 처리 SLA가 별도 계약과 테스트로 준비될 때까지 활성화하지 않음
- 6단계 CI·staging: 임시 trigger가 제거된 errors-only artifact에 enabled·배포 Secret DSN·환경·immutable release를 주입해 실제 handled 5xx 한 건, expected 4xx 0건, 민감정보 부재와 중복 여부를 검증한다. Source context를 사용할 때만 build 인증 값을 CI Secret으로 제공
- 로컬 시험에 사용한 IntelliJ 실행 설정의 임시 staging profile·smoke·Sentry diagnostic 환경변수는 저장소 밖 설정이므로 사용자가 제거한다. 저장소 설정은 Sentry disabled·local 기본값으로 복원돼 환경변수 미주입 시 event를 전송하지 않음
- 7단계 운영 활성화: tracing·profiling·Sentry Logs는 0/off로 시작하고 오류 수집만 점진 활성화하며 event volume·중복·민감정보를 확인한 뒤 alert와 sampling을 별도 승인으로 조정
- staging ECS 수집·표시 과정에서 애플리케이션 한글 `message`의 UTF-8 보존과 `event` 기반 기존 검색·대시보드·알림 쿼리 불변을 확인
- 운영 수집 전 MongoDB 드라이버 INFO가 계정 식별자와 클러스터 endpoint·topology를 출력하지 않도록 `org.mongodb.driver` logger를 WARN으로 제한할지 확정하고, 필요한 연결 진단 INFO는 로컬에서만 일시 활성화
- staging 로그 수집기에서 ECS JSON 파싱, requestId 검색, route template 보존, 민감정보 마스킹과 Servlet container·APM의 예상 밖 5xx 중복 기록 여부를 확인
- event·outcome·errorCode·provider 같은 낮은 cardinality 필드 기반 대시보드·메트릭·알림 임계값과 로그 보존 기간을 실제 운영 수집 경로에 맞춰 확정
- TMI-75의 `해야 할 일 → 완료` 전환 ID `41` 적용 내용을 확인한 뒤 사용자가 최종 승인하면 상태 전환만 수행하고 상태·Resolution을 재조회
- staging replica set에서 회원 탈퇴 User/Session 실제 Transaction rollback, custom repository fragment 연결과 Guest·LOCAL 재가입을 운영 index 조건으로 검증
- `UserWithdrawn` outbox와 Learning Core 시험·결과 데이터 삭제 또는 익명화, stateless Access Token의 서비스 간 즉시 폐기는 별도 이슈로 설계
- 사용자가 동의 상태 조회 API와 문서의 unstaged diff를 검토한 뒤 필요하면 직접 commit·push하며 Jira 댓글이나 상태 변경은 별도 승인 전까지 수행하지 않음
- ECS Task Definition에 개인정보 처리방침·이용약관·Quality review 현재 버전을 비공백 값으로 설정한 뒤 staging에서 Guest와 GET·PUT 선택·철회 흐름을 검증
- MongoDB replica set·Transaction 지원 여부를 확인해 RefreshSession 회전과 다중 폐기의 원자성·동시 재발급 통합 테스트를 별도 작업으로 설계
- 운영 `refresh_sessions`의 `{userId, revokedAt}` 실행계획과 `_class` 외부 소비 여부를 확인하고 필요한 경우에만 승인된 index/migration으로 처리
- OpenAPI 오류 응답의 일반 오류·Validation 배열 schema 구체화와 UserFactory의 `Clock` 주입·application 이동 여부를 후속 개선으로 검토
- Learning Core 저장소에서 Jira `TMI-10`을 기준으로 Identity JWKS를 조회해 RS256 서명·issuer·`tosunsaeng-learning-core` audience를 로컬 검증하고 JWT `sub`를 실제 userId로 사용하는 연동 작업
- Identity 보호 API용 별도 audience 또는 다중 audience 도입 여부와 scope 기반 세부 인가 정책을 후속 보안 설계에서 확정
- 운영 배포 전에 기존 RefreshSession 문서의 Optimistic Lock 버전과 회전 패밀리 필드 이행 정책 확정
- MongoDB Transaction 없이 기존 Session 폐기 후 후속 Session 저장이 실패하는 경우의 복구 또는 재로그인 UX 정책 확정
- 배포 환경의 issuer/JWKS URL 합의와 이전 Public Key 유지 기간을 포함한 다중 키 Rotation 전략 확정

## 중요 결정

- Java 21
- Jira `TMI-6`은 사용자 확인 기준 main 병합·전체 테스트 성공 후 명시적 승인으로 `완료` 전환됐으며, 추가 댓글이나 상태 변경은 별도 명시적 승인 후 수행
- Jira `TMI-9`는 사용자 확인·승인 후 transition ID `41`만 적용해 `완료`로 전환됐고 Resolution도 `완료`로 확인했으며, 댓글·필드와 다른 이슈는 변경하지 않음
- Jira `TMI-10`은 사용자 승인에 따라 `High` 우선순위로 생성한 뒤 별도 승인으로 transition ID `21`만 적용해 `진행 중`으로 전환했으며 담당자·스프린트·에픽·라벨·댓글과 다른 필드는 변경하지 않음
- Atlassian 연동은 저장소 설정이 아닌 Codex 사용자 전역 MCP 설정으로 관리하며 Remote MCP URL은 `https://mcp.atlassian.com/v1/mcp/authv2`를 사용
- Spring Boot 3.4.2
- MongoDB
- 현재 작업 기준 브랜치는 `hotfix/quality-review-consent`, HEAD는 `b6eb73e`로 `origin/main`과 같고 Quality review 선택 동의를 working tree에 구현·검증했으며 commit·push는 수행하지 않음
- 커밋 전 재검증에서 변경 파일 31개가 quality review 동의·설정·문서·테스트 범위에 한정되고 develop의 TMI-95·TMI-96 파일이 없음을 확인했다. `git diff --check` 통과와 `./gradlew clean test` 42개 suite·319개 테스트 성공 상태이며 아직 staged 파일은 없으므로 사용자가 전체 diff를 stage·검토한 뒤 commit할 수 있다
- 주석은 비자명한 인증·세션·보안 의도에만 한 줄로 추가하고 DTO 필드·getter·단순 대입에는 추가하지 않음
- 애플리케이션 코드는 `domain.auth`, `domain.user`, `global`의 세 최상위 역할로 나누고 실제 클래스가 없는 빈 패키지는 만들지 않음
- Controller는 Repository를 직접 참조하지 않고 유스케이스 application service만 호출하며 단일 구현체를 위한 `Service`/`ServiceImpl` 인터페이스는 만들지 않음
- `RefreshSession`과 Repository는 Auth 도메인이 소유하고 Refresh Token 생성·해싱·설정은 `global.security.refresh`의 기술 구현이 소유
- `MongoTransactionManager`는 Guest User·최초 RefreshSession 생성과 회원 탈퇴 User tombstone·전체 RefreshSession 폐기에 적용하며, 기존 Rotation·재사용 탐지·logout-all의 다중 Session 저장 원자성은 별도 후속 범위로 유지
- 운영 로그는 Controller·Repository·Entity에 중복 추가하지 않고 공통 요청 완료 경계와 application service의 실제 상태 변경 완료 지점에 기록하며, Transaction 성공 이벤트는 transactional proxy 반환 이후에만 남김
- 로그 공통 필드는 `event`, `outcome`, `requestId`, route template, status, duration, 안전한 경우의 `userId`·provider·errorCode·처리 건수로 제한하고 userId는 메트릭 tag로 사용하지 않으며 실제 자격증명·개인정보·Token·Hash·Header·Key·DB URI와 요청/응답 본문은 기록하지 않음
- 예상 밖 5xx는 요청 완료 filter가 application ERROR의 단일 소유자이며, Service·Controller·`GlobalExceptionHandler`는 같은 예외를 ERROR로 중복 기록하지 않는다. Handler 또는 filter 바깥에서 탈출한 예외는 안전한 오류 문맥을 request attribute에 넣고 `errorLogged` guard로 ERROR/Error dispatch 중복을 막으며, 실패 요청에는 별도 INFO 완료 로그를 남기지 않음
- 로그 언어는 machine-readable 검색·집계 식별자인 ECS key와 `event`·`outcome`·`errorCode`를 안정적인 영어 계약으로 유지하고, 사람이 읽는 애플리케이션 `message`는 한글을 사용할 수 있으며 대시보드 표시명도 한글로 구성
- Sentry build plugin의 source context 업로드와 runtime 오류 수집은 별도 책임이며 build 인증 값은 CI에만 두고 runtime에서는 DSN·environment·release를 환경별로 주입한다. SentryAppender issue 전송과 handled exception resolver 순서 변경은 사용하지 않고, 기존 handler가 처리하는 예상 밖 Exception만 명시적으로 capture하며 unhandled 오류는 SDK 기본 경계에 맡김
- 실제 Sentry DSN이나 인증 값을 채팅·저장소·WORKLOG에 전달하거나 기록하지 않고 환경변수 contract만 구현한다. 실제 project 수신은 사용자가 로컬 비추적 설정으로 확인했으며 배포 Secret·배포망 경계는 staging에서 별도로 검증
- Sentry `beforeSend`는 필드별 blacklist가 아니라 안전한 새 event를 만드는 whitelist 경계로 유지하고 원본 exception message와 모든 request·user·확장 context는 폐기한다. Source context는 경로·파일정보 없이 UUID 형식 JVM debug bundle ID만 허용한다. Logback integration은 계속 끄며 SDK/integration을 포함한 중복 여부는 handler 요청의 최종 transport event 개수로 검증한다.
- 실제 연결 확인에 사용한 one-shot startup trigger는 Sentry project 표시 확인 후 코드·설정·테스트·문서에서 제거했으며 production artifact에 포함하지 않는다. Handler·중복·4xx 제외·민감정보 비노출 계약은 외부 통신 없는 기존 통합 테스트로 계속 검증한다.
- 회원 탈퇴의 즉시 제거 범위는 User의 email·normalizedEmail·passwordHash·guestInstallationIdHash `$unset`과 nickname 익명화이며, User 문서·userId·provider·createdAt·동의 기록은 보존한다. RefreshSession 문서는 즉시 삭제하지 않고 모두 `ACCOUNT_WITHDRAWN`으로 폐기하며 만료 시 TTL 정리 대상이 되고, Learning Core 데이터와 기존 stateless Access Token은 이 API가 직접 삭제·폐기하지 않는다.
- OpenAPI Bearer 스키마는 전역 적용하지 않고 `GET /api/v1/users/me`, `GET`·`PUT /api/v1/users/me/consents`, `POST /api/v1/users/withdraw`와 `POST /api/v1/auth/logout-all`에 operation 단위로 적용
- Swagger/OpenAPI는 기본 활성화하되 배포 환경에서 `SWAGGER_ENABLED=false`로 비활성화 가능하고 테스트 프로필은 문서 계약 검증을 위해 명시적으로 활성화
- 실제 `userId`는 UUID 문자열
- User Document는 `users` 컬렉션을 사용하고 UUID 문자열 `userId`를 MongoDB `@Id`로 저장
- 이메일 정규화는 null 거부, 앞뒤 공백 제거, `Locale.ROOT` 소문자 변환만 수행
- Provider별 점 제거와 plus addressing 제거는 수행하지 않음
- `normalizedEmail`은 명시적인 unique index를 사용하며 현재 애플리케이션에서 자동 index 생성을 활성화
- 비밀번호 해시는 Spring Security의 기본 cost를 사용하는 BCrypt로 생성하고 User에는 `passwordHash`만 저장
- 회원가입 비밀번호 validation은 8~64자이고, 로그인 입력은 `NotBlank`와 최대 64자만 확인해 가입 복잡도·최소 길이 정책을 다시 적용하지 않음
- 이메일과 닉네임은 앞뒤 공백을 제거한 값으로 validation하며 비밀번호는 공백을 포함한 입력값을 임의 변환하지 않음
- 이메일 중복 확인은 가입 여부와 관계없이 성공 응답을 사용하며 `isAvailable`로 결과를 구분
- 회원가입의 사전 중복과 unique index 저장 충돌은 모두 `EMAIL_ALREADY_EXISTS` 409 오류로 통일
- 회원가입과 Guest 생성의 개인정보·약관 동의는 모두 반드시 true이며 false와 서버 현재 버전 불일치는 구분된 400 도메인 오류로 처리
- 현재 정책 버전은 필수 `app.consent.privacy-version`/`PRIVACY_CONSENT_VERSION`과 `app.consent.term-version`/`TERM_CONSENT_VERSION`으로 관리하고 누락·공백 시 기동에 실패하며 `AUDIO_POLICY_VERSION`은 제거
- 현재 동의 상태는 User 문서 내부의 `UserConsents`로 저장하고 별도 이력 컬렉션은 만들지 않음
- 기존 Mongo 문서에 `consents`가 없으면 두 동의를 false, 버전과 시각을 null로 읽고 과거 `audioConsent`를 새 동의로 자동 변환하지 않음
- User 생성·수정 시각 타입은 `Instant` 사용
- User provider는 `LOCAL`과 `GUEST`를 지원하며 기존 null provider 문서는 LOCAL로 해석
- Access Token은 RSA Private Key를 가진 Identity에서만 JWT RS256으로 서명
- Access Token 기본 TTL은 `PT30M`이며 issuer, audience, keyId, TTL과 키 Resource 위치는 환경변수로 교체 가능
- JWT Header는 `alg=RS256`, `typ=JWT`, 필수 `kid`를 사용
- JWT Claim은 UUID `sub`, `iss`, 단일 대상 배열 `aud`, `iat`, `exp`, UUID `jti`, 공백 구분 `scope`로 최소화
- 요청 scope는 정렬해 결정적으로 직렬화하고 null 또는 빈 scope에는 `learning:read learning:write` 기본값 사용
- Access Token에는 이메일, 닉네임 전체, 비밀번호 관련 값, Refresh Token 또는 Private Key 정보를 포함하지 않음
- RSA Private Key는 PKCS#8 `PRIVATE KEY`, Public Key는 X.509 `PUBLIC KEY` PEM 형식만 지원
- `RsaKeyLoader`는 Spring stereotype이 없는 로더로 유지하고 `JwtConfiguration`이 `ApplicationContext`를 `ResourceLoader`로 전달해 빈을 구성
- Spring Security 6.4 호환을 위해 `RSAKey` → `JWKSet` → `JWKSource<SecurityContext>` → `NimbusJwtEncoder` 구성을 사용
- JWKS는 `/.well-known/jwks.json`에서 Public JWK만 표준 `keys` 배열로 공개하며 BaseResponse로 감싸지 않음
- 현재 단일 Active Key를 사용하고 향후 Rotation에서는 기존 토큰 만료와 캐시를 고려해 이전 Public Key를 일정 기간 유지
- Access Token 발급 시간은 주입된 `Clock`을 사용하고 운영 기본값은 UTC 시스템 Clock
- Identity Resource Server는 기존 RSA Public Key Bean만 사용하고 자기 JWKS endpoint를 HTTP로 호출하지 않으며 Private Key를 검증에 사용하지 않음
- Identity Decoder는 RS256, 정확한 `typ=JWT`, 현재 `kid`, 필수 `sub`·`exp`, zero-skew `exp`·`nbf`, 설정 issuer와 audience 포함 여부를 검증
- 현재 Identity 보호 API도 기존 `tosunsaeng-learning-core` audience Access Token을 허용하며 Identity 전용 audience 또는 다중 audience는 이번 범위에 도입하지 않음
- 기본 `scope` 문자열은 Spring Security에서 `SCOPE_` 권한으로 변환하지만 이번 API는 특정 scope를 강제하지 않고 인증 여부만 적용
- 공개 경로는 지정된 인증 API, JWKS, health와 Swagger/OpenAPI로 제한하고 나머지는 기본적으로 인증
- 로그인은 `EmailNormalizer` 조회, BCrypt 일치 확인, `ACTIVE` 상태 확인 순서로 처리하고 실패 시 Access Token 발급기와 RefreshSession 저장소를 호출하지 않음
- 로그인 Access Token은 `AccessTokenIssuer.issue(userId, empty scopes)`를 호출해 설정된 기본 scope를 사용하며 Service에서 JWT를 직접 조립하지 않음
- 로그인 응답은 `accessToken`, `refreshToken`, `grantType`, milliseconds 단위 `accessTokenExpiresIn`만 포함하고 내부 사용자·세션 정보를 포함하지 않음
- Refresh Token 기본 TTL은 14일이고 난수 길이는 최소 32바이트이며 두 값은 `app.refresh-token` 환경 설정으로 교체 가능
- Refresh Token은 `SecureRandom` 기반 Opaque 값이고 Base64 URL-safe without padding으로 인코딩하며 UUID나 JWT를 대체 형식으로 사용하지 않음
- Refresh Token 원문은 `RefreshSession`에 저장하지 않고 UTF-8 SHA-256의 Base64 URL-safe without padding 해시만 저장
- `RefreshSession.createdAt`과 최초 `lastUsedAt`은 주입된 공용 `Clock`의 같은 시각이며 `expiresAt`은 해당 시각에 설정 TTL을 더함
- MongoDB TTL index는 만료 문서 정리 용도이며 향후 재발급은 문서 존재 여부와 별개로 `expiresAt`과 `revokedAt`을 직접 검증
- 토큰을 보유하는 내부 발급 결과와 로그인 응답의 문자열 표현은 토큰 값을 redaction 처리
- 재발급은 폐기 사유 확인 후 `expiresAt <= Clock`을 만료로 판정하고, 사용자 상태 확인 뒤 기존 Session의 Optimistic Lock 저장이 성공한 요청만 후속 토큰을 발급
- Rotation 재사용 탐지는 해당 사용자의 미폐기 RefreshSession 전체를 폐기하며 LOGOUT 또는 다른 폐기 사유는 일반 `INVALID_REFRESH_TOKEN`으로 구분
- 단일 로그아웃은 요청 Refresh Token에 해당하는 한 RefreshSession을 `LOGOUT`, 전체 로그아웃은 현재 JWT 사용자의 미폐기 RefreshSession 전체를 `LOGOUT_ALL`로 폐기
- 로그아웃은 Access Token 블랙리스트를 만들지 않으므로 기존 Access Token은 만료 시각까지 유효할 수 있으며 클라이언트는 성공 즉시 로컬 두 토큰을 삭제
- 로컬 키는 저장소에서 무시하고 테스트 키는 매 테스트 런타임에 메모리에서 생성
- Learning Core `audience`는 `tosunsaeng-learning-core`
- 실제 `userId`를 Python AI 서버로 보내지 않으며 Python AI의 `user_id`는 `examId` 유지
- 공통 응답 필드는 `isSuccess`, `code`, `message`, `result`
- 공통 오류 enum의 HTTP 상태와 응답 코드를 분리해 관리
- Validation 오류 상세는 `result` 배열로 반환하고 민감한 `rejectedValue`는 `null` 처리
- 테스트 프로필에서는 MongoDB 클라이언트·데이터·Repository 자동 설정 제외
- Security Filter Chain 내부 401/403은 공통 UTF-8 `BaseResponse` JSON Handler가 처리하고 Form Login과 Basic 인증은 비활성화

## 아직 구현되지 않은 것

- 구조화 운영 로그를 사용하는 수집 플랫폼 대시보드·메트릭·알림과 환경별 보존 정책
- 다중 Active/Retiring Key를 지원하는 Key Rotation
- 소셜 로그인
- 사용자 프로필 수정 API
- 정책 버전별 append-only 동의 감사 이력과 동의 철회 정책

## 남아 있는 위험 요소

- `RefreshSession`의 사용자별 미폐기 조회를 위한 `{userId, revokedAt}` compound index 계약을 추가했지만 운영 배포 전 실제 index 생성 상태와 데이터 규모별 `explain`을 확인해야 한다.
- 패키지 이동으로 신규 Mongo 문서의 `_class` FQCN이 바뀔 수 있으므로 외부 시스템이 `_class`를 조회 조건으로 사용하는지는 운영 데이터와 소비자에서 확인해야 한다.
- OpenAPI 오류 응답은 raw `BaseResponse` schema를 사용해 Validation의 `result` 배열을 완전히 구체화하지 못하므로 문서 전용 wrapper 도입 범위를 후속 검토해야 한다.
- `UserFactory`는 Spring `PasswordEncoder`·설정 주입과 `Instant.now()`에 직접 결합되어 있어 `Clock` 주입 및 application 계층 이동 여부를 후속 검토해야 한다.
- Atlassian MCP는 사용자 계정 권한으로 외부 서비스에 접근하므로 허용 범위와 연결 해제 필요성을 Codex 사용자 설정 및 Atlassian 계정에서 별도로 관리해야 한다.
- 현재 자동 index 생성은 초기 개발 편의를 위한 설정이며, 운영에서는 권한·데이터 규모·무중단 배포를 고려한 별도 index 관리 정책이 필요하다.
- 기존 User 문서는 migration 없이 읽을 수 있지만 새 정책 미동의로 취급되므로 프론트의 재동의 유도와 정책 전환 시점 합의가 필요하다.
- 기존 User 문서의 provider가 없으면 LOCAL로 읽지만 소셜 로그인 도입 전에는 provider 필드 명시적 이행과 계정 연결 정책이 필요하다.
- 이메일 중복 확인·회원가입·로그인·재발급·로그아웃 공개 API에는 rate limit, credential stuffing 방어, 자동화 요청 방어 및 abuse 관측 기준이 필요하다.
- 비밀번호 복잡도, 유출 비밀번호 차단 및 변경 정책은 제품·보안 명세 확정 후 추가해야 한다.
- 현재는 최신 개인정보·약관·Quality review 동의 snapshot만 저장하므로 철회와 정책 버전 변경의 전체 감사를 요구하면 append-only 이력 관리가 필요하다.
- 현재 회원 탈퇴 tombstone은 `consents` 전체를 보존하므로 Quality review true 상태 도입 후 탈퇴를 자동 철회로 볼지 감사 snapshot 보존으로 볼지 별도 결정이 필요하며, 외부 데이터 이용 중지는 outbox·consumer·보존 정책 없이는 보장되지 않는다.
- 사용자 정보 변경 기능을 추가할 때 `updatedAt` 갱신 책임과 동시 수정 정책을 명확히 해야 한다.
- 민감한 validation 필드명이 추가되면 마스킹 목록도 갱신해야 한다.
- 운영 환경의 MongoDB 연결과 health 상태는 배포 환경에서 별도로 검증해야 한다.
- ECS 구조화 설정은 제3자 logger에도 동일하게 적용되며 현재 MongoDB 드라이버 INFO에는 계정 식별자와 클러스터 endpoint·topology가 포함될 수 있으므로 운영 수집 전에 logger level과 보존·접근 정책을 제한해야 한다.
- 사용자가 공유한 로컬 전체 기동 로그에서도 MongoDB 드라이버 INFO가 비밀번호 원문 없이 계정 식별자·클러스터 endpoint·replica topology를 포함하는 것이 재확인됐다. 후속 진단 공유는 필요한 애플리케이션 event 또는 고정 startup 오류만 최소 발췌하고 MongoDB driver 로그는 제외해야 한다.
- Sentry 최종 event는 whitelist로 정제되므로 원본 예외 message와 request context를 운영에서 볼 수 없으며, 문제 분석은 예외 type·stack frame·requestId·errorCode와 별도 안전한 구조화 로그에 의존한다. 실제 project 수신·표시는 로컬에서 확인했지만 staging 배포망·alert·보존정책 및 선택적 CI source context 업로드는 별도로 검증해야 한다.
- 저장소의 임시 smoke 설정은 제거됐지만 IntelliJ 실행 설정에 추가한 staging profile이나 Sentry diagnostic 환경변수는 저장소 밖에 남아 있을 수 있으므로 로컬 정상 실행 전에 사용자가 제거해야 한다.
- 예상 밖 오류 로그는 민감정보 비노출을 위해 exception/cause 타입과 message 없는 최대 24개 stack frame만 보존하므로 원본 예외 message가 필요한 진단은 재현·메트릭·추적 도구와 함께 수행해야 한다.
- 요청 filter가 application ERROR를 한 번만 기록해도 Servlet container나 외부 APM이 별도로 같은 Throwable을 기록할 수 있으므로, 실제 배포 후 logger category와 error dispatch를 확인해야 전체 수집 화면의 중복 여부를 판단할 수 있다.
- 운영 RSA Key의 생성·주입·파일 권한·백업·교체는 저장소 밖의 Secret 관리 및 배포 절차로 확정해야 한다.
- 현재 JWKS는 단일 Active Key만 제공하므로 Rotation 전에 복수 Public Key 제공과 캐시 전파 기간을 구현해야 한다.
- 배포 환경의 `issuer`와 Learning Core 검증 설정이 정확히 일치해야 하며 HTTPS 배포 URL과 환경별 값을 함께 확정해야 한다.
- 서버 간 Clock 차이가 Access Token 검증에 미치는 영향을 고려해 Learning Core의 허용 오차 정책을 정해야 한다.
- Identity 보호 API가 현재 Learning Core audience 토큰을 함께 허용하므로 Identity 전용 audience 또는 다중 audience와 토큰 용도 분리 여부를 후속 검토해야 한다.
- scope는 `SCOPE_` 권한으로 변환되지만 endpoint별 scope 인가를 강제하지 않으므로 권한 모델 확정 후 세부 정책을 추가해야 한다.
- MongoDB TTL 삭제는 비동기 정리이므로 만료 문서가 일시적으로 남을 수 있으며 재발급은 계속 `expiresAt`과 폐기 상태를 애플리케이션에서 검사해야 한다.
- MongoDB Transaction을 도입하지 않았으므로 기존 Session의 Rotation 폐기 저장 이후 Access Token 발급 또는 후속 RefreshSession 저장이 실패하면 사용자가 현재 Session을 잃고 다시 로그인해야 할 수 있다.
- 기존 RefreshSession 문서에 `@Version` 또는 회전 패밀리 필드가 없다면 운영 적용 전에 데이터 이행 또는 기존 Session 만료·재로그인 정책이 필요하다.
- 재사용 탐지에서 여러 활성 Session을 폐기하는 저장은 Transaction으로 묶이지 않으므로 중간 저장 실패 시 일부 Session만 폐기될 가능성이 남아 있다.
- logout-all의 여러 Session `saveAll`도 Transaction이 아니므로 중간 실패 시 일부 Session만 폐기될 수 있으며 오류는 호출자에게 전파된다.
- 단일·전체 로그아웃은 Access Token을 즉시 무효화하지 않으므로 클라이언트의 로컬 토큰 삭제와 짧은 Access Token TTL을 함께 유지해야 한다.
- 회원 탈퇴도 stateless Access Token을 즉시 폐기하지 않으므로 기존 Token은 설정된 최대 TTL까지 Learning Core에서 암호학적으로 유효할 수 있으며 서비스 간 즉시 폐기는 별도 계약이 필요하다.
- 격리 test profile은 외부 MongoDB를 사용하지 않아 Spring Transaction proxy rollback을 검증했지만 실제 Atlas·replica set의 다중 collection rollback과 custom repository fragment wiring은 staging에서 재검증해야 한다.
- Identity 회원 탈퇴는 Learning Core 데이터를 직접 삭제하지 않으며 `UserWithdrawn` outbox와 시험·결과 데이터 삭제 또는 익명화 정책은 아직 구현되지 않았다.
- 같은 활성 RefreshSession의 단건 로그아웃이 정확히 동시에 실행되면 `@Version` 저장 충돌을 `LogoutService`가 별도로 멱등 성공으로 변환하지 않아 한 요청이 일반 500으로 끝날 수 있으므로 동시성 정책과 예외 변환을 후속 검토해야 한다.
- 자격증명 실패의 외부 code와 message는 통일했지만 사용자 부재 경로와 BCrypt 검증 경로의 실행 시간 차이에 대한 완화 정책은 rate limit·관측 기준과 함께 검토해야 한다.
- Java 패키지 경로가 전면 변경됐으므로 이 저장소 내부 테스트는 통과했지만, 패키지 FQCN을 직접 참조하는 별도 모듈이 존재한다면 새 `domain`·`global` 경로로 import를 갱신해야 한다.
- OpenAPI 응답 설명은 런타임 계약을 보조하는 문서이므로 후속 API 오류 코드나 보안 정책 변경 시 Controller 어노테이션과 문서 계약 테스트를 함께 갱신해야 한다.

## Codex Hook 운영 메모

- 프로젝트 로컬 Hook은 이 저장소와 각 Hook 정의가 신뢰된 경우에만 실행된다.
- Codex CLI에서 `/hooks`를 열어 `.codex/hooks.json`의 명령을 검토하고 신뢰해야 한다.
- Hook 명령이 변경되면 정의의 hash가 달라지므로 `/hooks`에서 다시 검토하고 신뢰한다.
- 모든 작업 종료 전에 WORKLOG를 append하고 이 문서를 최신 상태로 갱신한다.
