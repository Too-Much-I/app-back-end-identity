# TMI-203 Billing 발신 구성 검증과 시간 정책 경계

- 날짜: 2026-10-13 / 브랜치: feat/TMI-203-withdrawal-snapshot-feed-recovery
- 범위: 구성 테스트 추가·계약 확인. production 수정/외부 발송/배포 없음.

## 1. 5줄 결론

1. publisher ON/OFF와 실제 JWT factory 조합의 구성 회귀4개를 추가했다.
2. OFF에는 DB/키/증명 bean이 필요 없고 ON에는 JWT key/audience/HTTPS origin과 현행 clock 근거가 필요하다.
3. 발신은 RuntimeEvidence의 captureReady가 아니라 clockTrusted만 사용한다. 구매 승인과도 독립이다.
4. 기존202 문서의 clock/Retry-After 차단 정책은 최소 정리안에서 자동 삭제되지 않는다.
5. 시간 정책 변경 승인 전에는 production의 의존성을 제거하거나 true로 우회하지 않는다.

## 2. 사용자가 반드시 읽어야 하는 내용

이번 확인으로 발신 구성의 미완료 이유를 좁혔다. JWT 설정만으로 기동되는 것이 아니라 RuntimeEvidence bean이 필요하다. 현재 발신은 clockTrusted=false일 때 BLOCKED_CLOCK으로 차단하고,429/503 Retry-After 분류에서도 이를 사용한다. 이를 제거하면 정리 구성 단순화뿐 아니라 기존 발신 동작이 변경된다.

## 3. 결정 요청

발신도 시간 신뢰를 인프라 운영 전제로 바꿀지 확인해야 한다. 변경한다면 앱의 외부 clock attestation 요구/그 부재에 따른 차단을 정합화하되 JWT 시간 claim 검증, Retry-After 초/HTTP-date 해석·최소 backoff·24시간 초과 보류·20회 budget·401/403 BLOCKED_AUTH·exact204·lease/멱등성은 유지하는 방향을 권장한다. 실제 clock 오차 독립탐지 보장은 하지 않는다. 기존 BLOCKED_CLOCK 자료의 재개는 별도이며 전역 reset하지 않는다.

## 4. 위험·미확인

- 범위 확대를 승인받기 전 기존202 발신 정책을 변경하지 않았다. TMI-202 이슈에는 오래된 SigV4/ACK 표기도 남아 있으나 이번에 임의 수정하지 않는다.
- 키는 테스트 실행 중 생성한 합성 자료만 사용했다. HTTP transport를 생성하되 store에서 claim을 반환하지 않아 실제 네트워크 전송 없음.
- 구성 회귀는 실제 운영 시계 신뢰·TLS/Billing 공동 연동을 증명하지 않는다.

## 5. 변경·검증

신규 `src/test/java/web/tosunsaeng/identity/domain/user/withdrawalcapture/BillingWithdrawalConfigurationTests.java`:

- OFF/no dependencies.
- ON/JWT 설정 정상이어도 clock 근거 없음 → 기동 실패.
- 합성 clock 근거와 JWT 설정 → 기동, 구매 활성화 증명 불필요, captureReady 호출 없음, dispatch 없음.
- HTTP origin/빈 audience → 기동 실패.

## 6. 부록 — 근거·다음

- WithdrawalCaptureConfiguration.PublisherConfiguration, BillingWithdrawalPublisher, BillingWithdrawalRetry.
- TMI-202-withdrawal-capture-billing-delivery-plan.md Retry-After·CLOCK_UNTRUSTED 항목.
- TMI-203 Jira 최소안은 정리 전용 증명 제거와 소비처별 구분을 명시한다. 양 이슈 공식 조회 완료, 수정/댓글/상태 변경 없음.
- 전체 테스트 결과는 이번 WORKLOG/CURRENT_STATE를 따른다. 시간 정책 결정 후에만 발신 코드를 정합화한다.
