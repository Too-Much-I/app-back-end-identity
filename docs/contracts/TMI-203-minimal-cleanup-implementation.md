# TMI-203 최소 원천 정리 구성 구현

- 날짜: 2026-10-13 / 브랜치: feat/TMI-203-withdrawal-snapshot-feed-recovery
- 범위: 승인된 Jira 설명 기준 정리 전용 구성 변경. 배포/활성화 없음.

## 1. 5줄 결론

1. 정리 jobs의 외부 `Approval` 및 공용 `RuntimeEvidence` 주입 요구를 제거했다.
2. 설정·환경·인덱스/이관 preflight 및 실행 시 scope 검사는 남겼다.
3. 보존기간·lease·CAS·삭제/가용성 기록 transaction·참조 보호 알고리즘은 변경하지 않았다.
4. capture·snapshot/feed·구매 권한의 공용 RuntimeEvidence는 변경하지 않았으며 true 구현도 추가하지 않았다.
5. 시간 동기화와 배포/복원 writer 종료는 운영 전제다. 정리용 별도 증명 시스템을 새로 만들지 않는다.

## 2. 사용자가 반드시 읽어야 하는 내용

정리 전용 구성은 별도 승인 provider 없이 생성된다. `source-jobs-enabled` 기본OFF와 capture.required/cleanup-enabled 및 유효 environment 요구는 유지한다. index/source 이관 상태를 확인하지 못하면 기동 실패한다. source가 실제로 삭제 가능한지는 기존 DB 내부 검사와 transaction으로 결정한다.

**전체 application의 준비 조건을 모두 제거한 것은 아니다.** capture·구매·복구가 사용하는 RuntimeEvidence의 기존 운영 연결 공백은 여전히 남는다. 이번에는 정리 전용 의존성을 분리했으며 전체 서버 ON 기동이나 실배포 준비 완료를 주장하지 않는다.

## 3. 결정 사항

- 원격 승인/clock 증명 호출·승인 저장소·갱신 worker는 추가하지 않는다.
- `requireConfiguration`은 capture 및 cleanup 활성 설정과 환경 문법을 검사한다. 실행 때도 재검사한다.
- `requireScope`는 동일 환경과 UUID stream을 검사한다. DB 상태/버전 확인과 CAS는 기존 알고리즘 책임이다.
- 만료 원본과 종료 stream 보존 경계는 서버 Clock을 사용한다. 앱이 절대시각의 정확성을 독립 증명하지 않으며 시간 오차로 조기 만료 판단할 위험은 운영 전제로 남는다.

## 4. 주요 위험과 미확인

- 기본 기능OFF 유지. 실제 인프라 시간 동기화·구writer 종료·복원·Billing 연동·외부 경보 수신은 검증하지 않았다.
- 별도 운영 승인 철회 테스트의 저수준 Authority callback은 알고리즘 테스트용으로 유지한다. 배포가 외부 승인 bean을 제공해야 한다는 의미는 아니다.
- 새로운 무조건 true RuntimeEvidence/구매 승인 fallback은 없다. 공용 소비처의 정책 변경은 별도 범위다.

## 5. 변경·검증 내용

- `RecoverySourceJobsConfiguration.java`: 중첩 Approval 인터페이스 제거, runtime 주입 제거, 로컬 설정/scope/보존 검사로 연결.
- `RecoverySourceJobsTests.java`: 외부 승인/runtime 없이 ON 구성 가능·preflight 호출, 설정 해제/환경 누락 차단, 다른 환경/잘못된 stream 차단, 잘못된 index 차단 회귀. 스케줄은 Mock cleanup으로만 실행한다.
- 저수준 journal cleanup·range/sweep·retired cleanup 및 공용 capture/복구 코드 변경 없음.
- 외부 API/DTO/body fixture/hash/index/120일·7일 보존/LC/user·workload 인증 불변.

## 6. 부록 — 실행 결과·다음 작업

기본 `env -u SENTRY_AUTH_TOKEN ./gradlew clean test` 성공. 이어서 `TMI203_LOCAL_MONGO=true`로 clean test 재실행 성공: 전체2110개/통과2104개/실패0/오류0/support opt-in skip6. 기존 실제 replica-set22개 포함. 임시 DB 외 실제 데이터는 사용하지 않았고 테스트용 컨테이너는 종료·자동 제거했다. git diff --check 통과.

다음은 공용 runtime 준비 조건의 실제 운영 연결 범위를 별도로 확인하고 Billing 공동 테스트·인프라 전제·경보 검증 뒤 승인된 기능만 활성화하는 것이다. Jira 상태/댓글·commit/push·배포 변경 없음.
