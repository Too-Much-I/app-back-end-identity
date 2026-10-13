# TMI-203 원천 정리 로컬 실DB 검증과 운영 연결 경계

- 날짜: 2026-10-13 / 브랜치: feat/TMI-203-withdrawal-snapshot-feed-recovery

## 1. 5줄 결론

1. 임시 로컬 MongoDB 7.0.14 replica-set에서 기존 복구 테스트와 신규 폐기 정리 테스트를 실행한다.
2. 신규 검증은 101개 range 분할 정리·재시작 재조회, 승인 철회 rollback, 동시 source 변경 충돌, journal/snapshot 참조 보류다.
3. 운영 Approval 및 RuntimeEvidence의 실제 제공자는 저장소에서 찾지 못했다. 이번에는 무조건 허용 구현을 추가하지 않았다.
4. 실행 권한·시계 근거·writer 종료를 제공할 신뢰 원천과 갱신/폐기 절차가 결정돼야 실제 연결할 수 있다.
5. 기능 기본 OFF·외부 API·보존기간을 유지한다. 운영 DB/배포/키/인프라 변경은 없다.

## 2. 사용자가 반드시 읽어야 하는 내용

로컬 테스트에서는 합성 자료와 테스트 전용 Authority를 사용한다. 이는 운영 승인 증명을 구현하거나 실제 복원 안전성 전체를 입증한 것이 아니다. `RecoverySourceJobsConfiguration.Approval`과 `WithdrawalCaptureConfiguration.RuntimeEvidence`에 운영 증거를 공급할 구현은 아직 없다.

이번 요청 중 실DB 검증 일부는 진행했지만, 운영 근거 연결은 미완료다. 설정을 true로 바꾸거나 로컬 시계가 정상이라고 가정하는 방식으로 이 공백을 숨기지 않는다.

## 3. 결정·확인이 필요한 사항

먼저 배포 담당과 아래 근거의 실제 제공 위치/책임을 정해야 한다. 이 문서는 새로운 DB collection이나 wire를 확정하지 않는다.

| 근거 | 필요한 내용 | 불명일 때 |
| --- | --- | --- |
| 정리 승인 | 환경·source incarnation·허용 작업 범위, 유효기간과 철회 방법 | 해당 정리 거절 |
| capture 준비 | 모든 writer의 capture 적용 버전, 이관 검증, 진행 중 구 writer 종료 | 정리 거절 |
| 시계·시간 범위 | 허용 오차와 최근 관측 시각, capturedAt/expiry 기준의 신뢰 근거 | 정리 거절 |
| 폐기 stream | 정확 closedAt·교체 stream·구 writer 종료/복원 경계 | metadata 정리 거절 |
| 복원 이후 | 이전 승인/시계 관측 재사용 차단, 새 incarnation 검증 | 검증 전 정리 OFF |

관리자가 설정한 true 값만으로 위 사실을 증명하지 않는다. 운영 근거의 원자적 철회/실행 중 재확인과 유효기간 테스트를 함께 설계해야 한다.

## 4. 주요 위험과 미확인 사항

- 신규 concurrent 테스트는 source 문서의 버전 경합을 재현한다. 실제 다중 서버 restore·old writer 전체 경합을 대신하지 않는다.
- commit 응답 유실 주입, 실제 delivery claim 경합, 운영 데이터 분포의 query plan/처리량, 양 서버 복원/ALB/JWT 검증은 별도다.
- 테스트용 Authority는 application bean으로 등록하지 않는다. 실제 증거가 없으면 현재 fail-closed 동작을 유지한다.

## 5. 구현·검증 근거

- 테스트: `src/test/java/web/tosunsaeng/identity/domain/user/withdrawalrecovery/RecoveryReplicaSetTests.java`.
- 구현: `MongoRetiredSourceCleanup`, `RecoverySourceJobsConfiguration`, `MongoSourceExpiryEvidence`.
- 최초 실행의 신규 2건 실패는 합성 source의 availabilityVersion 초기값과 assertion 불일치였다. 신규 helper의 초기값을 명시한 뒤 전체 재실행한다.
- 테스트 DB는 loopback 전용 컨테이너와 UUID별 임시 DB만 사용한다. 테스트 종료 시 정확한 임시 DB 패턴만 삭제하며 컨테이너는 작업 후 종료한다.
- 최종 `env -u SENTRY_AUTH_TOKEN TMI203_LOCAL_MONGO=true ./gradlew clean test`: 전체 2108개, 통과 2102개, 실패/오류 0, support opt-in 6개 skip. 복구 replica-set 기존18개와 신규4개 실행 완료. 임시 컨테이너 종료·자동 제거 완료.

## 6. 부록 — 운영 연결의 다음 단계

실제 증거 제공자 확인 → 권한 범위/신선도/철회/복원 결합 설계 → fail-closed adapter 및 회귀 → Billing 공동 검증 → 별도 승인 활성화 순서다. Jira 댓글·상태 전환·commit/push는 이번 범위에 없다.
