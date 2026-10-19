# Identity GitHub Actions 운영 배포 전환

## 1. 5줄 결론

1. 원격 main c9a0a2f6의 유일한 workflow는 구 staging 폐기 안내 후 실패한다. 자동 운영 배포는 없다.
2. 사용자 기존 workflow 수정 요청에 따라 main→prod, develop→test 분기를 로컬에서 구현했다.
3. 최초 조사에서 AWS_PROD_ROLE_ARN이 없었으나, 사용자 승인 후 운영 전용 역할 생성·변수 등록·대조를 완료했다.
4. 운영 역할이 없으면 실패하며 구 staging/test 역할로 대체하지 않는다. 기존 ECS 설정을 유지하고 이미지·Sentry release만 교체한다.
5. IAM/변수 설정만 완료했다. commit/push·실제 Actions 실행·운영 배포는 하지 않았다.

## 2. 반드시 읽을 내용

파일 이름 deploy-staging.yml은 기존 이력을 유지하기 위한 이름이며 구 staging 서비스로 보내지 않는다. main push 또는 main을 선택한 workflow_dispatch는 운영 배포를 실행한다. 별도의 GitHub Environment 승인 단계는 이번 변경에 포함되지 않는다. 자동 배포 전 main의 branch protection/검토 절차를 확인해야 한다.

현재 브랜치에는 운영에서 수동 배포했던 구매/capture 호환 코드가 있다. workflow만 오래된 main에 따로 옮겨 배포하지 말고 실제 배포 후보의 코드·테스트를 함께 반영해야 한다. source 설치 후 capture OFF 서버로 복귀하지 않는다. 기존 서비스의 rollback=false 및 capture/fence 설정을 유지한다.

## 3. 결정·준비 사항

- repository variable AWS_PROD_ROLE_ARN에 새 tosunsaeng-github-identity-prod-deploy-role을 등록했고 값 일치를 확인했다. 실제 ARN/키는 이 문서에 기록하지 않는다.
- GitHub OIDC 설정은 use_default=true/use_immutable_subject=true다. 조회한 고유 ID 포함 sub_claim_prefix에 :ref:refs/heads/main을 붙인 exact subject와 sts.amazonaws.com audience만 StringEquals로 허용했다. 기존/test 역할은 변경하지 않았다.
- Identity ECR push/read, prod ECS service 조회·갱신, task definition 등록·조회, prod execution role의 ECS 전달만 허용했다. 현재 prod:17의 taskRoleArn은 null이다. ECR 로그인 및 task definition 등록·조회는 Resource=*이며 service 갱신/PassRole/ECR repository는 특정 대상으로 제한한다. 등록 후 trust/권한 전체 JSON을 기대값과 대조해 모두 true 확인했다.
- 사용자가 commit/push 후 main에 검토·반영해야 원격 동작이 바뀐다. production environment 승인을 추가하려면 OIDC subject도 함께 별도 설계해야 한다.

## 4. 위험·미확인

OIDC assume-role 및 GitHub runner에서 실제 권한 행사·ECS 배포/health 실행은 미검증이다. revision 재검사는 외부 수동 배포와의 완전한 원자적 lock이 아니므로 배포 중 외부 설정 변경을 피해야 한다. 이 변경은 신규 기능 스위치/판매/자동삭제 활성화 승인이 아니다.

## 5. 구현과 검증

기존 workflow의 main trigger·prod service/family/health·전용 역할 분기를 추가했다. Buildx 결과 digest로 이미지를 고정하고, 렌더링 후 service revision이 기준 revision과 같고 단일 COMPLETED 상태인지 재확인한다. 변경됐으면 배포를 거절한다.

DeploymentTargetTests는 main/develop/미지원 ref·역할 누락과 외부 배포 경합을 합성자료로 검증한다. SentryReleaseDeploymentTests는 release 및 digest 이미지 입력을 확인한다. 실제 AWS 호출 없는 테스트다. 실행 결과는 CURRENT_STATE/WORKLOG의 최신 항목을 따른다.

## 6. 근거

- [workflow](../../.github/workflows/deploy-staging.yml)
- [대상 회귀](../../src/test/java/web/tosunsaeng/identity/deployment/DeploymentTargetTests.java)
- [release 회귀](../../src/test/java/web/tosunsaeng/identity/deployment/SentryReleaseDeploymentTests.java)
- 원격 main 조회: git ls-remote 및 fetch 후 해당 workflow 읽기. 로컬 main checkout/merge 없음.
- GitHub Actions 변수는 이름만 조회했으며 값/Secret은 출력하지 않았다.
