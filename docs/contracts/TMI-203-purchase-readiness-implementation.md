# TMI-203 구매 준비 DB/config 연결 구현

- 날짜: 2026-10-13 / 브랜치: feat/TMI-203-withdrawal-snapshot-feed-recovery / 연계 TMI-200·202

## 1. 5줄 결론

1. 구매 준비를 `SourceBoundPurchaseReadiness`에 연결하고 외부 RuntimeEvidence/PurchaseActivationEvidence 의존성과 인터페이스를 제거했다.
2. `AUTH_PURCHASE_SOURCE_STREAM_ID`는 canonical UUID이며 현재 환경의 검증된 단일 ACTIVE 원천과 정확히 같아야 한다. 기본값은 비어 있고 구매 기본 OFF다.
3. CaptureReadiness는 같은 조회에서 검증한 source를 반환한다. 불명/중복/불일치/DB 오류는 허용으로 바뀌지 않는다.
4. 구매 scope OFF에도 기존 구매 응답 replay는 readiness를 검사한다. 개별 계정·탈퇴·control/발급 transaction 보호는 수정하지 않았다.
5. 이관 완료·구writer 종료·시간 동기화는 별도 증명 서비스를 개발하는 대신 배포/복원 운영 책임으로 유지한다. 실제 이관·배포·판매 활성화는 수행하지 않았다.

## 2. 사용자가 반드시 읽어야 하는 내용

새 설정은 **어느 원천을 대상으로 구매를 활성화할지** 지정할 뿐 운영 점검 완료를 증명하지 않는다. 원천이 교체됐을 때 새 ID를 자동 학습하지 않는다. 현재 원천이 정상이어도 설정과 다르면 준비 거절이다. 요청마다 전체 사용자 이관을 스캔하거나 누락 control을 자동 생성하지 않는다.

구매 scope ON은 source binding, capture, control writer, 발급 fence, session fence, TTL 양수·최대30분을 만족해야 한다. 기존 준비 상태 상실 시 MEMBER 발급 오류 동작은 유지하며 조용히 구매 scope만 빼고 로그인 성공시키는 변경이 아니다. Guest·탈퇴·control 불일치 보호는 기존 코드와 테스트를 유지한다.

## 3. 사용자가 결정해야 하는 사항

추가 제품 선택은 없다. source ID 실환경 주입·실제 구매 ON·이관·복원 후 재개는 별도 승인 범위다. Jira TMI-200/203은 읽기만 수행했다. Jira 설명 정합화가 필요하면 문구를 제시하고 별도 승인 후 수정한다.

## 4. 주요 위험과 미확인 사항

- 동일 source ID까지 되돌린 DB 복원은 binding만으로 탐지하지 못한다. 기존 writer 격리·새 source 설치·계정 복원 영향 확인 후 재개해야 한다.
- 조회 이후 source를 강제로 바꾸는 관리자 작업과 이미 진행 중인 요청을 이 읽기 검사 하나로 원자화하지 않는다. 복원 중 old writer/진행 발급 격리는 필수 운영 책임이다. source교체와 동시 commit의 완전한 fence를 새로 구현했다고 주장하지 않는다.
- 앱의 독립 절대시각 오차 검출은 없다. 시간 동기화/알림과 장애 시 영향 작업 중단·복구가 필요하다.
- 실제 이관·운영 TTL·ALB/TLS·키 교체·Billing 연동·restore는 미검증이다. 로컬 source 변경 검증과 공동 복원 검증은 다르다.

## 5. 변경 파일과 배포 전 확인

- production: PurchaseProperties(설정/문법), SourceBoundPurchaseReadiness(설정/source 비교), CaptureReadiness(검증한 source 반환), WithdrawalCaptureConfiguration(bean 연결/증명 인터페이스 제거), application.yml(빈 기본값 설정).
- tests: PurchaseSourceReadinessTests 신규, PurchaseConfigurationTests/PurchaseControlTests, WithdrawalConfigurationTests/BillingWithdrawalConfigurationTests, RecoveryCombinedConfigurationTests/RecoveryReplicaSetTests 정합화.
- PurchaseTokenFence/PurchaseControlService/issuer/reissue runtime 코드는 변경하지 않았다. API/body/schema/hash/index/JWT claim·TTL/보존·LC 계약 불변.
- 배포 순서: scope OFF 배포 → index/control 진단·필요 시 승인 이관 → 호환 writer/구writer 종료 확인 → capture/source·시간·Billing 점검 → 대상source 설정 → 별도 scope ON. 기존 커스텀 증명 bean을 별도 모듈로 배포했다면 제거된 인터페이스 참조 여부를 먼저 확인한다.
- 기존 dirty 변경과 별도 AppVersionController 변경을 보존했다. 이번 의도 외 파일 편집 없음. commit/push/Jira 쓰기/실환경 데이터 변경 없음.

## 6. 부록 — 근거와 검증

- [설계](TMI-203-purchase-readiness-separation-plan.md), [TMI-200 책임 정합화](TMI-200-purchase-scope-control-fencing-plan.md).
- mock 회귀: 정상 단일source·정확binding, null/비정규UUID, 누락·교체·중복·DB오류, OFF replay 보호, 설정 누락/불일치 기동 거절 및 증명 없는 ON.
- 로컬 Mongo 회귀: 실제 원천 폐쇄 후 false, 새 ACTIVE 생성 시 capture는 true지만 기존 구매 binding은 false. 합성 DB/임시 컨테이너만 사용한다.
- 최종 테스트 결과는 아래 후속 검증 기록 및 CURRENT_STATE/WORKLOG를 따른다. Billing 실환경 호출 결과가 아니다.

### 최종 검증 기록

- `env -u SENTRY_AUTH_TOKEN TMI203_LOCAL_MONGO=true ./gradlew clean test`: BUILD SUCCESSFUL. XML 총2,137개/통과2,131/실패0/오류0/skip6(support 조건부).
- 로컬 Mongo 조건부 회귀도 활성화했다. RecoveryReplicaSetTests 10개 모두 실행했으며 이번 원천 폐쇄/교체 구매 binding 검증을 포함한다. 동시 source 교체와 발급 commit 전체 경합을 증명하는 테스트는 아니다.
- 합성 테스트 DB는 각 테스트 종료 시 삭제했고, 이번 전용 Mongo 컨테이너는 종료/자동 제거했다. 다른 실행 중 컨테이너는 변경하지 않았다.
- `git diff --check` 통과. RuntimeEvidence/PurchaseActivationEvidence production 참조 없음 확인. 기존 발급/replay/탈퇴 회귀도 전체 실행에 포함된다.
- Jira 댓글 초안(미등록): 구매 준비 DB/config/source binding 연결, 별도 증명 인터페이스 제거 및 운영 책임 명시. production5개 파일·테스트7개와 계획/구현 결과/기록 갱신. 전체2,131통과/6skip. 구매 OFF 유지, 실제 DB 이관·Billing 연동·복원·활성화 미수행.
