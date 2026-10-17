# 구 Identity·Learning Core staging 중단 기록

## 1. 5줄 결론

1. 사용자가 운영 LC 공개키 주소 이전·재배포와 구 staging 두 서비스 중단을 명시 승인했다.
2. staging/prod Identity 공개 JWKS는 HTTP200 및 JSON 전체 일치를 재확인했다.
3. 운영 LC prod:7 → prod:8은 공개키 URI 3개만 변경한다. 기존 issuer·고정 이미지·기타 설정을 유지한다.
4. 운영 배포 COMPLETED 확인 후 staging 자동 확장을 일시정지한 상태에서 desiredCount=0으로 내렸다. 양 서비스 desired/running/pending 모두 0이다.
5. DB·Secret·공용 ALB·test 변경 없음. 최종 확인 시 ALB 기존 타깃은 draining으로 연결 정리 중이다.

## 2. 반드시 읽어야 하는 내용

- 중단 대상: tosunsaeng-staging-cluster의 tosunsaeng-identity-service, tosunsaeng-learning-core-service. 서비스 삭제가 아닌 복구 가능한 실행 수 0 변경이다.
- issuer의 staging 문자열은 검증 계약이므로 유지한다. 공개키 조회 주소만 https://identity-prod.to-teacher.com/.well-known/jwks.json 으로 변경한다.
- 실제 사용자 로그인 E2E는 별도이며 공개키 동일성·ECS 배포·ALB health 검증과 구분한다.

## 3. 결정사항

- 사용자 승인: 운영 LC 공개키 주소 3개 변경·재배포, staging 양 서비스 자동 확장 일시정지/desired0.
- 구매 권한·탈퇴 capture·자동삭제 활성화, DB 이관, 키 변경, IAM 변경은 이번 범위가 아니다.

## 4. 위험과 후속

- 2026-10-17 후속 로컬 수정으로 .github/workflows/deploy-staging.yml의 main push 트리거와 구 staging 분기를 제거했다. main 수동 실행도 AWS 인증 전에 거절한다. develop→test는 유지. 사용자 commit/push/main 반영 전에는 원격 옛 workflow가 남으므로 실행하지 않는다. 새 prod 자동 배포는 추가하지 않았다.
- 구 staging 도메인을 직접 사용하는 앱/도구는 중단 후 이용할 수 없다. 구 호스트를 운영으로 자동 전달하지 않는다.
- capture cutover 이후 구 서버를 단순 재가동하면 안 된다. 향후 재가동 시 데이터 계약 호환을 다시 확인한다.

## 5. 실행 결과

- 운영 LC prod:8 배포 COMPLETED, desired/running1, pending0. 이미지 sha256:0b566cc1cd7b73ac87a0be133aa834f84333bb16b1a44db12d36b09bc9958fd0 유지. 정규화 task definition 대조에서 URI 3개만 변경된 것을 확인.
- 최초 조사: staging 양 서비스 desired/running1, autoscaling min1/max3, 세 suspension false, scheduled action 없음.
- 2026-10-17 23:34 KST 확인: 구 Identity:23, 구 LC:23 모두 desired/running/pending0, desiredStatus RUNNING 태스크 조회0. ALB 타깃은 draining으로 후속 연결 정리 중이며 물리적 태스크 STOPPED 및 타깃 완전 제거까지는 이 확인으로 보장하지 않는다.
- 두 서비스 autoscaling dynamic in/out, scheduled suspension 모두 true. min1/max3와 기존 정책은 유지.
- 운영 Identity prod:13 및 LC prod:8, test Identity:47 및 LC:45 모두 desired/running1, pending0. Identity:13/test LC:45는 다른 작업의 배포이며 본 작업 변경이 아니다.
- 운영 api-prod/identity-prod health HTTP200, prod JWKS HTTP200. 공개키 일치 재검증 및 새 LC 컨테이너 HEALTHY/ALB healthy/기동 오류0 확인. 실제 사용자 로그인 요청은 수행하지 않았다.
- 문서 정적 검사만 실행. 코드 변경 없어 Gradle 미실행. commit/push/Jira 변경 없음.

## 6. 상세 변경 범위

| 설정 | 변경 |
| --- | --- |
| IDENTITY_JWK_SET_URI | staging host → prod host |
| USER_MERGED_WORKLOAD_JWK_SET_URI | staging host → prod host |
| USER_WITHDRAWN_WORKLOAD_JWK_SET_URI | staging host → prod host |
| issuer 3개 | 불변 |
| 구 staging 자동 확장 | dynamic in/out 및 scheduled 일시정지 완료 |
| 구 staging desiredCount | 1 → 0 완료 |
