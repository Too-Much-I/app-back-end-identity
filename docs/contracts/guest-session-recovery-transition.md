# 업데이트 후 기존 Guest 복구

## 1. 5줄 결론

1. main의 한시적 설치 ID 기반 복구를 develop의 계정 유형·병합·세션 보안 모델에 맞게 이식한다.
2. `POST /api/v1/auth/guest` 요청·응답 구조는 유지하며 `GUEST_RECOVERY_ENABLED=true`일 때만 복구한다. 기본값은 false다.
3. 동일 설치 해시의 ACTIVE GUEST에 같은 userId로 새 토큰을 발급한다. 프로필·동의·학습 기록은 변경하지 않는다.
4. 멤버·탈퇴·정지·병합된 사용자는 거절한다. 기존 로그아웃·폐기 토큰의 `/reissue` 거절 정책은 유지한다.
5. 코드 배포와 운영 플래그 활성화 및 앱의 복구 API 호출 연결을 모두 확인해야 실제 업데이트 복구가 동작한다.

## 2. 반드시 읽을 내용

`SESSION_LOGGED_OUT`은 `/auth/reissue`에서 폐기·보안 경계 불일치 등을 나타낸다. 이 오류만으로 Guest라고 판정할 수 없다. 앱에 저장된 기존 Guest 세션과 동일한 installationId가 있을 때에만 복구 UX로 진행한다. 멤버는 재로그인 경로를 사용한다. 무한 재시도나 installationId 재생성은 하지 않는다.

복구 요청은 기존 `/auth/guest` 계약대로 필수 동의와 현재 정책 버전을 포함한다. 서버는 요청의 동의를 검증하지만 복구된 사용자의 기존 동의를 덮어쓰지 않는다. 정책 동의가 실제 필요한 경우 기존 동의 API/UX를 따른다. 복구 성공 시 응답의 토큰 쌍을 교체하고 같은 userId의 기록을 유지한다. 이 API는 과거 응답을 재현하는 멱등 재시도가 아니라 새 세션 발급이다.

## 3. 결정 사항

- 사용자 요청에 따라 누락된 복구 경로를 구현한다. `/reissue`를 통한 폐기 토큰 허용은 하지 않는다.
- 기본 OFF를 유지한다. 배포 후 `GUEST_RECOVERY_ENABLED=true`를 명시해야 한다. 버전 설정만 변경한 prod 배포에는 이번 코드가 포함되지 않는다.
- 종료 시 false로 재배포한다. OFF 전 발급된 정상 세션은 자동 폐기하지 않는다.

## 4. 위험과 미확인 사항

- installationId 보유를 한시적 인증으로 신뢰하므로 유출 시 Guest 계정 접근 위험이 있다. 멤버 인증에는 사용할 수 없다.
- 반복 호출 시 새 세션이 누적될 수 있다. 자동 종료/별도 횟수 제한은 이번 main 호환 이식에 추가하지 않는다.
- 최신 앱에서 401 후 같은 설치 ID로 `/auth/guest`를 호출하는지, 정책 동의·응답 저장·기존 기록 표시까지 실기기 확인이 필요하다.
- 이번 12건의 오류가 Guest인지 실제 폐기 사유가 무엇인지는 요청 로그만으로 확정하지 않았다.

## 5. 구현 및 검증

[GuestRecoveryTransactionService](../../src/main/java/web/tosunsaeng/identity/domain/auth/registration/application/GuestRecoveryTransactionService.java)는 사용자 조건부 쓰기, 토큰 발급, Refresh 저장, 응답 구성을 Mongo 트랜잭션으로 묶는다. 동일 사용자에 대한 승격·병합·탈퇴 쓰기와 충돌하게 하며 오류 시 롤백한다. 현재 session epoch로 GUEST 인증 증거를 붙이고 기존 세션 보안 검증을 통과해야 한다.

[GuestAuthService](../../src/main/java/web/tosunsaeng/identity/domain/auth/registration/application/GuestAuthService.java)는 기존 설치 및 신규 생성 unique 경합 후 복구를 호출한다. 성공 로그는 트랜잭션 반환 후에만 기록하며 설치 ID·토큰을 남기지 않는다.

## 6. 부록: 정확한 복구 대상

| 필드 | 조건 |
| --- | --- |
| guestInstallationIdHash | 요청 installationId의 기존 해시와 일치 |
| provider | GUEST |
| status | ACTIVE |
| accountType | GUEST 또는 null/누락(구버전 호환) |
| mergedIntoUserId / mergedAt | 모두 null/누락 |

조건 불일치 또는 플래그 OFF는 `409 GUEST_ALREADY_EXISTS`. 계정 없음은 기존 신규 생성 경로다. 정상 동의 검증·기존 오류 계약·UUID JWT sub·RS256·Refresh 해시 저장을 유지한다. 프론트가 임의 userId를 보내는 필드는 추가하지 않는다.
