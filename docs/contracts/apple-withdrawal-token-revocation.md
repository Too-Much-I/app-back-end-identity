# Apple 회원 탈퇴: 프론트 철회와 서버 후속 정리

## 5줄 결론

1. Apple 연결 계정은 앱에서 Apple authorization code를 확보하고 Firebase SDK `revokeToken`을 성공시킨 뒤 탈퇴 API를 호출한다.
2. 기존 `POST /api/v1/users/withdraw`의 입력·응답을 유지한다. 서버에 Apple code·receipt·철회 boolean을 보내지 않는다.
3. 서버 code 필수·최근 5분 Apple 재인증·서버 중복 철회 구현은 2026-10-09 사용자 결정으로 되돌렸다.
4. 서버는 프론트 철회를 계약 전제로 받아들여 Apple obligation을 `CLIENT_MANAGED`로 처리하고 기존 Firebase 삭제 및 identity release를 계속한다.
5. 서버가 SDK 철회 성공을 독립 검증하는 것은 아니다. 배포와 실기기 최종 `CLEANED` 검증은 별도다.

## 반드시 읽을 내용

Apple이 연결된 iOS 계정은 앱에서 Apple 로그인 창을 열어 새 authorization code를 받고 Firebase SDK로 철회한다. 사용자 취소·SDK 실패 시 서버 탈퇴 요청을 보내지 않는다. 성공 후 기존 refreshToken/firebaseIdToken 입력으로 서버 탈퇴를 요청한다. 프론트 계약상 Apple 연결 여부를 확인해야 하며 현재 로그인 provider가 Apple인 경우만으로 제한하지 않는다.

서버는 기존 Access/Refresh/Firebase credential 유효성과 계정 소유권 검증을 유지한다. WITHDRAWAL에 최근 인증 시간 제한을 추가하지 않는다. Firebase ID Token이나 연결 provider 목록은 Apple 철회 완료 증거가 아니다. 직접 API 호출자가 앱 철회를 생략하는 경우 서버가 이를 탐지하지 못한다는 신뢰 경계를 사용자 승인에 따라 수용한다.

## 결정 사항

- 신규 API 필드, Apple 철회 flag/API key/redirect 설정, provider credential 저장과 완료 시각 schema를 추가하지 않는다.
- Apple obligation은 서버 철회 성공을 의미하는 `SATISFIED`와 구분하여 `CLIENT_MANAGED`로 반환한다.
- worker의 owner guard·lease·retry·presence 검증을 유지한다. Firebase disable → refresh revoke → client-managed obligation → delete → absence 확인 → 외부 cleanup 완료 → 기존 identity release → `CLEANED` 순서다.
- 탈퇴 API 성공은 내부 탈퇴 접수/확정을 의미하며 비동기 cleanup의 `CLEANED`를 보장하지 않는다.
- 기존 `RECONCILIATION_REQUIRED/PROVIDER_OBLIGATION_REQUIRED` 데이터는 자동 해제·재처리하지 않는다. 운영 보정은 별도 승인 대상이다.

## 위험과 미확인 사항

현재 앱 Android 경로는 Apple 철회를 `unsupported`로 반환하고 탈퇴 요청을 계속한다. Android에서 Apple 연결 계정 탈퇴를 제공한다면 프론트 지원/차단 정책을 별도 보완해야 한다. 이번 서버 변경이 플랫폼별 철회를 입증하지 않는다.

확인은 고정 iOS 소스와 로컬 Mock 테스트 기준이며 설치된 앱 build, 실제 Apple/Firebase 철회, 서버 최종 `CLEANED`와 재가입은 실기기 E2E 대상이다. 기존 worker/release flag와 권한이 올바르게 설정되어 있어야 한다. 운영 데이터·설정·배포는 이번 작업에서 변경하지 않는다.

## 구현과 조사 근거

- 서버: `FirebaseSdkWithdrawalCleanupAdapter.satisfyProviderDeletionObligations` 및 `ProviderObligationResult.CLIENT_MANAGED`.
- 회귀: `FirebaseSdkWithdrawalCleanupAdapterTests`, `UserWithdrawalExternalCleanupWorkerTests`.
- 앱 고정 main commit: `526c877b0bfeb7630dcd0e2f201cdde932432aac`, `src/features/auth/firebase-auth-sdk.ts`, `account-withdrawal.ts`, `api/withdraw-account.ts`, `auth-runtime.ts`.
- [앱 SDK 구현](https://github.com/Too-Much-I/app-front-end/blob/526c877b0bfeb7630dcd0e2f201cdde932432aac/src/features/auth/firebase-auth-sdk.ts#L155)
- [Firebase 공식 Apple token revocation](https://firebase.google.com/docs/auth/ios/apple#token_revocation)
