# TMI-203 탈퇴 발신 시간 증명 제거 구현

- 날짜: 2026-10-13 / 브랜치: feat/TMI-203-withdrawal-snapshot-feed-recovery / 연계 TMI-202·204

## 1. 5줄 결론

1. 탈퇴 publisher 구성의 RuntimeEvidence 주입과 publisher/retry의 clockTrusted 인자를 제거했다. 상수 true adapter를 추가하지 않았다.
2. 별도 증명 없이 JWT 설정이 유효하면 발신 구성을 시작한다. 키·HTTPS origin·audience 검증과 기본 OFF는 유지한다.
3. 신규 BLOCKED_CLOCK 발생은 제거했지만 enum·저장 기록·권한 있는 선택 재개는 보존했다. 자동 재개나 데이터 이관은 없다.
4. 구매 readiness의 RuntimeEvidence·PurchaseActivationEvidence 요구는 수정하지 않았다.
5. 전체 clean test 2,125개 중 2,096개 통과, 실패/오류 0, 조건부 skip 29. 실환경·양 서버 공동 검증 완료는 아니다.

## 2. 사용자가 반드시 읽어야 하는 내용

이제 탈퇴 발신에 별도 시간 증명 공급자는 필요 없다. Clock 자체, JWT 시간 검증, source 만료 및 lease 시간, Retry-After 계산은 그대로 사용한다. 인프라 시간 동기화와 이상 알림·영향 작업 중단/복구를 운영 전제로 하며, 앱이 절대시각 오차를 독립 탐지한다는 보장은 없다.

기존 BLOCKED_CLOCK 자료는 자동 claim 대상이 아니다. 원천 유효성·권한·notBefore·버전 확인과 감사 기록을 포함한 기존 명시 재개 절차를 유지한다. 재개 시 기존 계약대로 generation attempt만 초기화하며 lifetime attempt와 기존 오류·원 event·notBefore를 일괄 삭제하지 않는다.

## 3. 사용자가 결정해야 하는 사항

이번 코드 수정에 추가 제품 선택은 없다. 실제 배포/활성화, 기존 blocked 자료의 선택 재개는 별도다. 구매 준비 조건 분리 설계는 이 작업의 승인 범위가 아니다. Jira TMI-202의 기존 SigV4 표기 등은 남아 있으며 승인 없이 수정하지 않았다.

## 4. 주요 위험과 미확인 사항

- 큰 시계 오차는 재시도·lease·만료를 오판하게 할 수 있다. 배포 전 시간 동기화/알림, ALB→task TLS, 키 교체와 양 서버 연동을 확인한다.
- 실제 blocked 자료를 조회하거나 변경하지 않았다. 이번 실행의 Mongo 23개 및 support 6개 조건부 테스트는 skip이다.
- Billing 보고 1,039개 성공을 재사용하지 않았다. 이번 결과는 Identity 전체 로컬 테스트이며 실제 Billing HTTP·인프라 검증이 아니다.

## 5. 변경과 유지 범위

- `WithdrawalCaptureConfiguration.PublisherConfiguration`: 발신 bean의 RuntimeEvidence 의존성만 제거.
- `BillingWithdrawalPublisher`: 증명 부재 차단과 retry 인자 제거. durable dispatch, exact204, auth 차단, lease/source 보호 유지.
- `BillingWithdrawalRetry`: clockTrusted 인자/분기 제거. 429/503의 정수·HTTP-date·0/과거·24시간/초과·중복·overflow, 20회 한도 불변.
- 테스트 4개 파일: BillingWithdrawalConfigurationTests, BillingWithdrawalPublisherTests, BillingWithdrawalRetryTests, WithdrawalOperationsTests. 증명 없는 ON, false 증명 bean 미호출, 잘못된 origin/audience 실패, 401/403, Retry-After, 응답 유실 후 동일 body, legacy BLOCKED_CLOCK 재개를 확인한다.
- TMI-202 계획의 독립 시간 증명 문구를 합의된 운영 전제로 개정했다. API/body/schema/hash/index·JWT 규격·구매/LC 계약·보존기간은 변경하지 않았다.
- 기존 dirty 변경을 유지했다. 이번 범위 외 의도하지 않은 편집 없음. commit/push/Jira 쓰기/배포/외부 데이터 변경 없음.
- 최종 상태 검사에서 시작 시 없던 AppVersionController.java 변경이 관찰됐다. 이번 작업에서 편집하지 않은 외부 병행 변경으로 보존했으며, 그 변경에 전체 테스트 결과를 귀속하지 않는다.

## 6. 부록 — 근거와 실행

- [공동 수락 인계](TMI-203-204-publisher-clock-simplification-Billing-handoff-2026-10-13.md)
- [TMI-202 계획](TMI-202-withdrawal-capture-billing-delivery-plan.md)
- 실행: `env -u SENTRY_AUTH_TOKEN ./gradlew clean test`, BUILD SUCCESSFUL. XML 합산 2,125 / 통과 2,096 / 실패 0 / 오류 0 / skip 29.
- `git diff --check` 통과. 발신 및 retry의 clockTrusted 사용처 없음, 구매 사용처 유지 정적 확인.
- Jira 댓글 초안(미등록): 탈퇴 발신의 외부 시간 증명 의존성 제거, 구매 보호 및 legacy 재개 유지. production 3개 파일/관련 테스트 4개와 계획·인계·기록 변경. 전체 2,096 통과/29 skip, 실환경·공동검증 및 선택 재개 미수행.
