# TMI-134 — 기존 Guest 세션 긴급 복구

## 5줄 결론

1. main 3894627 기반이며 SNS develop 기능은 포함하지 않는다.
2. `GUEST_RECOVERY_ENABLED` 기본 false. true일 때만 동일 installationId의 ACTIVE GUEST에 같은 userId로 새 토큰을 발급한다.
3. API URL/요청/응답은 유지하며 기존 프로필·동의·세션을 덮어쓰지 않는다.
4. 자동 종료와 별도 횟수 제한은 없다. SNS 전환 후 운영 설정을 수동 false로 바꿔 재배포한다.
5. OFF만으로 기존 세션을 폐기하지 않는다. 정상 Refresh 재발급·기존 만료/폐기 정책을 유지한다.

## 반드시 읽을 내용

- `POST /api/v1/auth/guest`의 기존 동의 검증은 유지한다. 정책 버전 불일치는 별도 오류이며 본 hotfix가 우회하지 않는다.
- 미등록 설치는 기존 신규 생성 흐름이다. 등록 설치는 복구 서비스에서 hash/provider/status를 재검증한다.
- `findAndModify`는 `guestInstallationIdHash` 일치 + `provider=GUEST` + `status=ACTIVE`만 대상으로, upsert 없이 `guestRecoveryFence`를 1 증가시킨다. 이 필드는 인증 비밀이나 횟수 제한이 아니라 동시 사용자 상태 변경과 쓰기 충돌을 만들기 위한 내부 필드다.
- 사용자 조건부 쓰기·토큰 발급·Refresh 저장·응답 구성을 하나의 `mongoTransactionManager` 트랜잭션으로 묶는다. 트랜잭션이 실패하면 응답을 반환하지 않는다. 충돌은 자동 무제한 재시도하지 않고 앱 재시도를 허용한다.
- 신규 등록 unique 충돌도 원래 트랜잭션이 rollback된 후 동일 hash가 존재하면 별도 복구 트랜잭션으로 진입한다.
- main에는 accountType/SNS 병합 모델이 없다. LOCAL/알 수 없는 provider 및 ACTIVE 외 상태는 DB 조건에서 거절한다. develop 이식 시 accountType과 병합 상태를 추가 검토해야 한다.
- 탈퇴가 설치 hash를 해제한 뒤 같은 ID로 신규 가입하는 기존 동작은 유지한다. 이는 탈퇴 User 복구가 아니다.

## 결정 사항

- 사용자 승인: 설치 ID 보유자를 신뢰하는 한시적 인증 위험 수용, 자동 종료 시각·별도 요청 제한 제외, 종료 후 유효 세션 유지.
- 기본 OFF는 안전한 코드 배포 기본값이다. 실제 긴급 복구를 제공하려면 운영자가 true를 명시하고 새 태스크에 반영해야 한다.
- 환경변수는 런타임 동적 토글이 아니다. false 전환도 모든 태스크의 재배포 완료를 확인한다. 종료 직전 시작된 요청이 완료될 수 있다.

## 위험 및 미확인 사항

- 설치 ID 유출 시 계정 접근 위험, 반복 발급으로 세션이 누적될 위험, 수동 OFF 누락 위험이 있다.
- 삭제된 세션의 과거 폐기 사유는 알 수 없으며 복구가 과거 logout/reuse 보호 효과를 약화할 수 있다. 기존 토큰 재사용 검증 로직 자체는 변경하지 않는다.
- 로컬 테스트는 Mock Repository/Mongo와 Spring 트랜잭션 경계를 검증한다. 실제 replica set의 write conflict/rollback은 배포 전 별도 검증한다. 운영 데이터로 부하·경쟁 테스트를 하지 않는다.
- 최초 장애 401 원인과 전체 도메인 라우팅은 미확정이다. 코드 변경과 운영 복구 완료를 구분한다.

## 배포·검증·롤백

1. main 대상 PR에서 hotfix 파일만 포함되는지 확인한다. `poc/`와 develop 기록 stash는 제외한다. commit/push는 사용자 수행.
2. 전체 테스트와 diff check, Mongo replica set/기존 transaction probe 성공을 확인한다.
3. 비운영 검증 계정으로 같은 installationId 재요청 시 같은 userId/기존 기록 유지, 새 토큰 발급, 동의 비변경을 확인한다. LOCAL/SUSPENDED/WITHDRAWN은 복구되지 않아야 한다.
4. 실제 DB 트랜잭션에서 세션 저장 실패 시 fence/session rollback, 두 복구 충돌 및 탈퇴와 복구 경합을 검증한다. 실패 요청에서 토큰이 반환되면 배포하지 않는다.
5. `GUEST_RECOVERY_ENABLED=true`를 실행 태스크 환경에 반영해 배포한다. 배포 이미지 커밋·모든 태스크 정상 상태 확인 후 기존 앱을 완전 종료/재실행한다. 화면의 Guest 재시도도 복구 요청을 수행한다.
6. `identity.guest.recovered` 이벤트/오류율과 사용자 동의하의 기존 기록 접근을 확인한다. 토큰·설치 ID·해시 원문을 수집하지 않는다.
7. 복구가 안정화되면 SNS 전환 준비. 새 앱 버튼 제거만으로 종료하지 않는다. 서버 false 재배포로 복구를 닫고, 구버전 신규 Guest 차단/업데이트 안내는 후속 작업으로 진행한다.
8. false 배포 후 기존 유효 Refresh의 재발급이 계속 성공하는지 확인한다. 세션 일괄 삭제/폐기나 TTL 인덱스 변경은 하지 않는다.
9. 긴급 롤백은 false 재배포 또는 이전 main 이미지로 복귀한다. 발급된 세션은 기존 스키마와 호환된다. fence 필드는 남겨도 무방하며 삭제 작업은 필요 없다.
10. main 병합 후 develop에 안전장치와 정책을 포팅한다. SNS 전환 완료 계정/병합 source가 Guest 복구 대상이 되지 않도록 회귀 검증한다.

## Jira 댓글 초안 (자동 등록하지 않음)

TMI-134 main 기반 Guest 긴급 복구를 구현했습니다. GuestAuthService와 신규 복구 트랜잭션 서비스, 기본 OFF 환경설정, API 설명 및 회귀 테스트를 추가했습니다. 자동 종료/추가 요청 제한은 제외하고 기존 세션 유지 정책을 반영했습니다. 전체 테스트 결과는 WORKLOG 최신 항목을 참고합니다. 실제 replica set 경쟁/rollback 및 운영 배포 검증은 별도 필요합니다. commit/push/배포는 수행하지 않았습니다.
