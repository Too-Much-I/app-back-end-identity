# TMI-203 capture·복구 공용 준비 조건 분리

- 날짜: 2026-10-13 / 브랜치: feat/TMI-203-withdrawal-snapshot-feed-recovery
- 상태: 로컬 구현·기동 구성 검증. 전체 운영 application 실기동/배포 완료 아님.

## 1. 5줄 결론

1. capture와 snapshot/feed가 외부 RuntimeEvidence를 요구하던 부분을 DB/config 기반 CaptureReadiness로 분리했다.
2. capture 활성 설정·환경·정확히1개 ACTIVE source·cutover 존재·순번 범위를 확인하며 매 사용 시 다시 읽는다.
3. capture/recovery/cleanup을 함께 구성해도 외부 운영증명 bean을 요구하지 않는 테스트를 추가했다.
4. 구매 권한은 기존 증명 없으면 false다. publisher의 기존 clock 정책과 JWT 시간 검증은 변경하지 않았다.
5. 시각 정확성·구writer 격리는 운영 전제이며 DB readiness가 이를 증명한다고 주장하지 않는다.

## 2. 사용자가 반드시 읽어야 하는 내용

`CaptureReadiness`는 true 상수가 아니다. 유효한 설정과 실제 DB source 상태를 검사하며 DB 장애는 성공으로 바꾸지 않는다. source 조회는 환경/ACTIVE 필터·최대2건·5초 상한이다. 인덱스 preflight는 bean 생성 시 수행한다. 기존 capture transaction의 ACTIVE source 순번 갱신·journal/delivery 저장·30초 시간 예산과 복구 T/H·서명cursor·digest·CAS는 유지한다.

새 결합 기동 테스트는 세 Spring 구성(capture/recovery runtime/source jobs)을 묶고 외부 서비스와 index 검증을 Mock 처리한 것이다. 전체 애플리케이션 HTTP/TLS connector, 실제 IAM/ALB/키/Billing 연결을 검증했다는 의미는 아니다. 별도 replica-set 테스트는 실제 source 폐쇄/교체 시 readiness 변경을 확인한다.

## 3. 결정 사항

- 구매 `PurchaseReadiness`: RuntimeEvidence 주입을 ObjectProvider로 바꿨지만 증명이 없으면 구매 허용이 아니라 false다. PurchaseActivationEvidence도 여전히 필요하다.
- outbound BillingWithdrawalPublisher: 기존 RuntimeEvidence clock 정책을 그대로 유지한다. publisher ON에는 기존 의존성과 workload JWT 설정이 필요하며 이번에 외부 발송/활성화하지 않았다.
- snapshot/store reader 내부 공급자 이름을 clockTrusted에서 runtimeReady로 변경했다. 로컬DB 상태를 시계 증명으로 오해하지 않도록 의미를 명확히 했다.
- 환경설정·키·DB collection/index·외부 wire/API/body fixture/보존기간·JWT 계약 변경 없음.

## 4. 주요 위험과 미확인

- 서버 Clock의 절대 오차는 새 readiness가 탐지하지 않는다. 시간동기화/구writer 종료·격리는 인프라 및 배포/복원 절차다.
- readiness마다 bounded DB read가 추가된다. 실제 운영 부하/쿼리 실행계획은 아직 검증하지 않았다.
- 활성 source 확인은 구writer drain이나 정상 구매 활성화를 증명하지 않는다. 구매 및 publisher 정책을 임의 제거하지 않았다.
- 공용RuntimeEvidence는 이제 capture/recovery가 아니라 구매/publisher의 남은 경계다. 해당 증명 공급 및 실제 외부 연동은 별도 미완료로 명시한다.

## 5. 변경 파일·회귀

- 신규 CaptureReadiness.java.
- WithdrawalCaptureConfiguration.java: local bean·capture 연결, 구매 증명 부재 fail-closed.
- RecoveryRuntimeConfiguration.java: local readiness와 index/migration 확인.
- MongoSnapshotStore.java, MongoRecoveryReader.java: 공급자 명칭 정합화(시간/transaction 알고리즘 불변).
- WithdrawalConfigurationTests.java, RecoveryRuntimeTests.java: 분리된 타입에 맞춘 기존 회귀.
- 신규 RecoveryCombinedConfigurationTests.java: 결합ON/증명부재 구매거절, source누락·control비활성 차단, source중복/손상/DB장애·OFF 검증.
- RecoveryReplicaSetTests.java: 실제 source 폐쇄·대체에 따른 local readiness 검증 추가.

## 6. 부록 — 다음 작업

최종 테스트 수치는 CURRENT_STATE/WORKLOG의 이번 실행 결과를 따른다. 다음은 publisher/구매의 미완료 경계를 별도 확인하고 Billing 공동 연동·운영 환경/TLS/키/복원/경보 검증을 수행하는 것이다. 별도 승인 저장소/갱신 시스템 재도입, Jira쓰기, commit/push, 배포는 하지 않았다.
