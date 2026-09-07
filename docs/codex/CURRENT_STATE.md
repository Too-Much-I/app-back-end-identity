# Codex Current State

이 문서는 Codex 세션 시작 시 추가 개발자 컨텍스트로 읽힌다. Secret, Token, Password, 실제 Key, 전체 MongoDB URI를 기록하지 않는다.

## 프로젝트

- 이름: `app-back-end-identity`
- 현재 단계: Stage 7 Identity `TMI-123` 구현과 Learning Core `TMI-125` 연동 후속 보완이 PR #38의 merge commit `fa9843e`로 `develop`과 `origin/develop`에 반영됐다. 전체 630개 테스트와 `git diff --check`가 통과했고 관련 feature flag는 모두 `false`다. Jira `TMI-123`에는 승인된 완료 댓글을 등록했으며 상태와 Resolution 모두 `완료`로 전환됐다.
- 상태 기준일: 2026-09-07

## 현재 작업 — 사용자 JWT account_type (2026-09-07)

- 브랜치·커밋·PR 명령어를 사용자에게 안내했다. 제안 브랜치는 `codex/add-access-token-account-type`, PR base는 `develop`이며 실제 Git 변경과 PR 생성은 사용자가 수행한다. 프론트 인증 가이드는 별도 변경으로 남기고 작업 기록에는 기존 미커밋 기록도 포함된다.
- 사용자 요청으로 무료 사용권 조회보다 먼저 `account_type` 발급을 구현했다. `develop@fa9843e` 기준 로컬 미커밋 변경이며 이번 작업의 Jira·PR·merge·배포는 없다.
- 공통 AccessTokenIssuer에 UserAccountType 필수 인자를 추가하고 7개 사용자 발급 서비스가 현재 `User.getAccountType()`을 전달한다. JWT는 문자열 MEMBER/GUEST를 발급하며 null 유형을 거절한다. refresh는 현재 DB 유형을 사용하고 upgrade는 같은 userId·MEMBER, merge는 target userId·MEMBER를 유지한다.
- 공개 API·기존 claim·RefreshSession·프로필 `accountType`·legacy User 호환·탈퇴/병합 차단은 유지한다. workload JWT와 Identity의 구형 Token 검증에는 새 claim 요구를 추가하지 않았다.
- 검증: `./gradlew clean test` 성공, 123개 suite·640개 테스트, 실패·오류·건너뜀 0개. `git diff --check` 통과. 기존 문서 미커밋 변경과 프론트 인증 가이드를 보존했고 예상 밖 변경은 없다.
- [JWT 계약](../contracts/identity-learning-jwt.md)에 전체 발급 경로와 인계 항목을 반영했다. 실제 운영 TTL·Learning Core 사용자 JWT skew·구버전 발급 instance 종료 시각은 미확인이다. Identity 배포 후 `구버전 종료 시각 + 구버전 최대 실제 TTL + 사용자 JWT skew`가 지난 뒤 Challenge를 활성화한다. PT30M fallback을 운영 TTL로 확정하지 않는다.
- 다음 작업: 사용자의 commit·PR·merge 및 배포 후 위 증빙을 Learning Core에 전달하고 무료 사용권 조회 작업으로 복귀한다.

## 완료

- 2026-09-07 현재 우선순위는 Billing public entitlement reader → Identity Billing audience·`billing:read` scope → 프론트 연동 → Firebase Stage 8 순서다. 단, SNS 로그인을 먼저 production에 공개할 경우 Stage 8 logout-all Firebase revoke를 선행 release blocker로 전환한다. (`codex-turn:01a07a6a-8adf-71d1-b5b7-c1bd1b06a6dc`)

- 2026-09-07 무료 사용권 조회와 Firebase Stage 8의 우선순위를 비교해 현재는 Billing public entitlement reader를 먼저 진행하기로 권장했다. Firebase/SNS production flag가 아직 비활성이라 logout-all Firebase revoke 공백이 외부 사용자에게 노출되지 않은 반면, entitlement 조회는 Firebase 외부 설정 없이 구현 가능하고 프론트 기능 가치를 바로 제공한다. 단, SNS 로그인을 먼저 production에 활성화한다면 Stage 8을 release blocker로 앞당긴다.

- 2026-09-07 종료 훅 기준으로 무료 사용권 조회를 Billing public reader 별도 트랙으로 우선 진행할 수 있다는 결정을 현재 turn 식별자와 함께 WORKLOG에 동기화했다. 권장 순서는 Billing의 side-effect 없는 entitlement 조회와 사용자 JWT verifier, Identity의 Billing audience·`billing:read` scope, 프론트 화면 연동이며 완료 후 Firebase Stage 8로 복귀한다.

- 2026-09-07 사용자가 요청한 무료 사용권 수량 조회 기능이 Firebase 후속 Stage 8에 포함되는 작업이 아니라 Billing public reader 별도 트랙임을 재확인했다. 제품 우선순위상 이 기능을 다음 작업으로 선택해도 인증 고정 순서를 위반하지 않으며 Google Cloud Billing·Kakao 설정에도 의존하지 않는다. 구현 순서는 Billing의 side-effect 없는 `GET /api/v1/entitlements`와 사용자 JWT verifier 선배포, Identity Access Token의 `tosunsaeng-billing` audience 및 account type별 `billing:read` scope 추가, 프론트 사용권 화면 연동이다.

- 2026-09-07 다음 고정 작업인 Stage 8 `logout-all Firebase refresh revoke`의 목적과 범위를 현재 구현 기준으로 정리했다. 현재 `/api/v1/auth/logout-all`은 Identity RefreshSession만 모두 폐기하므로 Firebase/SNS 회원이 다른 기기의 Firebase 세션으로 다시 exchange할 수 있다. 권장 구현은 내부 Session 전체 폐기와 exact FirebaseIdentity 대상 revoke job/outbox를 원자적으로 저장하고 worker가 Firebase refresh token을 멱등 revoke하며 실패를 retry·reconciliation하는 방식이다. User disable·delete, Provider unlink와 기존 stateless Identity Access Token 즉시 차단은 이 단계 범위가 아니다.

- 2026-09-07 Firebase signup 요청의 `nickname` 출처를 프론트 가이드에 명확히 했다. `/firebase/signup`은 기존 MEMBER 로그인 요청이 아니라 `/firebase/exchange`의 `ENROLLMENT_REQUIRED` 이후 신규 가입 완료 요청이며, nickname은 Provider에서 서버가 자동 결정하지 않고 가입 화면에서 사용자가 직접 입력·확정한다. Provider 표시 이름은 프론트 초기 제안값으로만 사용할 수 있다.

- 2026-09-07 종료 훅 기준으로 프론트 인증 가이드의 전체 개발 완료·production release 계약 전환을 현재 turn 식별자와 함께 WORKLOG에 동기화했다. 목표 계약은 Firebase 기반 email/password·Google·Apple·Kakao, same-UID phone link, Guest·reissue 응답 유실 복구, Identity·Firebase 전체 로그아웃, 탈퇴·재가입·downstream 차단과 인증수단 변경·rebind 완료 상태를 전제로 하며 신규 wire는 확정된 전용 API 문서 없이 임의로 만들지 않는다.

- 2026-09-07 프론트 Firebase·SNS 로그인 가이드를 현재 구현 현황 보고서에서 인증 후속 단계 1~12와 외부 staging 검증이 모두 완료된 최종 release 계약으로 전환했다. 최종 계약은 Firebase email/password·Google·Apple·Kakao, same-UID phone link, Guest 복구·merge, 응답 유실 안전 재발급, Identity·Firebase 전체 로그아웃, 탈퇴와 재가입, Provider unlink·phone 변경 전용 경계를 전제로 한다. 아직 exact wire가 별도 문서에 없는 후속 API URL·DTO는 임의로 만들지 않고 전용 공개 계약을 따르도록 명시했다.

- 2026-09-07 프론트 전달용 Firebase·SNS 로그인 및 회원 전환 연동 가이드를 실제 Controller·DTO·오류 코드 기준으로 작성했다. 기존 전달본의 Kakao 누락, Guest `ALREADY_LINKED` 오처리, recent-auth와 force refresh 혼동, `/reissue`의 merged 오류 기대, logout-all의 Firebase revoke 과장을 바로잡고 Firebase exchange/signup, Guest 생성·승격·merge, auth-method sync, reissue/logout, 프로필·탈퇴와 legacy API의 요청·응답 예시를 추가했다. 관련 기능은 여전히 기본 비활성이며 실제 Firebase·모바일·Mongo·downstream staging E2E 전 production에 노출하지 않는다.

- 2026-09-07 사용자 무료 사용권 조회 기능을 cross-service 구조로 검토했다. Entitlement 진실 공급원인 Billing이 public `GET /api/v1/entitlements`를 제공하고 사용자 JWT `sub`만으로 benefit별 available unit을 반환하는 방향이 현재 C1-A/C2-A 결정과 맞다. 무료 Grant는 최초 reserve에서 lazy 생성되므로 eligibility·Claim·Grant를 함께 읽는 부작용 없는 resolver가 필요하며, 현재 Billing은 internal API만 있어 public JWT reader·DTO·Security 구현이 남아 있다. Firebase Billing 계정 없이도 개발은 진행할 수 있다.

- 2026-09-07 Google Cloud $300 무료 체험 크레딧 적용을 공식 무료 프로그램 문서로 확인했다. Identity Platform은 공개 제외 목록에 없어 무료 체험 결제 계정에 연결된 staging 프로젝트의 적격 OIDC·SMS 초과 사용료는 일반적으로 90일/$300 범위에서 자동 상계된다. 무료 구간이 먼저 적용되며 정확한 credit 적용은 Billing 보고서에서 확인하고, 만료·소진 시 서비스 중지 또는 유료 전환 위험을 관리해야 한다.

- 2026-09-07 Identity Platform 비용을 공식 가격표로 확인했다. Billing 연결·활성화 자체에는 고정 월요금이 없지만 Kakao Generic OIDC는 프로젝트당 월 50 MAU 초과분이 현재 0.015달러/MAU이고, 한국 전화 인증 SMS는 일 10건 무료 이후 현재 0.01달러/전송 건으로 과금될 수 있다. Tier 1은 월 50,000 MAU까지 무료이며 staging은 테스트 번호·Emulator, KR region 제한, quota·budget alert를 우선한다.

- 2026-09-07 Cloud Billing 연결과 Identity Platform 활성화 절차를 공식 문서 기준으로 정리했다. staging 프로젝트를 선택해 Billing의 내 프로젝트에서 활성 결제 계정을 연결하고 budget alert를 설정한 뒤 Identity Platform Marketplace에서 활성화·Firebase Authentication 업그레이드를 승인한다. 기존 Firebase 앱·SDK는 유지되지만 유료 서비스·quota가 적용되며, OpenID Connect 공급업체가 보이는지 확인하기 전 Kakao 설정으로 진행하지 않는다.

- 2026-09-07 종료 훅 기준으로 신규 MEMBER phone credential link 설명을 현재 turn 식별자로 WORKLOG 끝에 동기화했다. SNS 로그인 Firebase UID에 SMS credential을 link하고 강제 갱신 Token 하나로 가입을 완료하며 phone-only 로그인과 별도 UID 생성을 금지한다는 계약은 변경하지 않았다.

- 2026-09-07 신규 MEMBER의 phone credential link 계약을 설명했다. Google·Apple·Kakao로 로그인된 현재 Firebase User에 SMS 인증 결과를 `linkWithCredential`로 추가해 Firebase UID를 유지하고, provider 변경을 반영한 강제 갱신 ID Token 하나로 Identity 가입을 완료한다. phone sign-in으로 별도 UID를 만들거나 구 Token을 제출하는 흐름은 허용하지 않는다.

- 2026-09-07 삭제한 Firebase 앱의 동일 package/bundle ID 재등록 충돌을 공식 Management API로 확인했다. 기본 삭제는 30일간 app을 `DELETED` 상태로 보존하므로 같은 ID로 새 앱을 만들지 말고 `showDeleted=true` 목록에서 exact app을 찾아 플랫폼별 undelete로 복원해야 한다. 영구 삭제 또는 다른 ACTIVE app 점유라면 프로젝트·app 목록을 확인하고 임시 ID 변경 없이 Support 경계로 처리한다.

- 2026-09-07 종료 훅 기준으로 Firebase 최초 연결 안내를 현재 turn 식별자로 WORKLOG 끝에 동기화했다. 기존 staging Google Cloud 프로젝트에 Firebase를 추가하고 Authentication·모바일 앱·billing·Identity Platform·phone·Kakao provider 순으로 구성하며, 검증 전 feature flag를 false로 유지한다는 결론은 변경하지 않았다.

- 2026-09-07 Firebase 미연결 상태의 최초 설정 순서를 정리했다. 기존 staging Google Cloud 프로젝트에 Firebase를 추가하고 Authentication·모바일 앱을 먼저 등록한 다음 billing과 Identity Platform을 활성화하며, 그 뒤 phone과 Generic OIDC `oidc.kakao`를 설정한다. Identity에는 exact `FIREBASE_PROJECT_ID`와 배포 workload의 Firebase Admin 접근 권한을 연결하되 실제 credential은 저장소에 두지 않고 모든 관련 flag는 staging E2E 전 false로 유지한다.

- 2026-09-07 현재 휴대폰 인증 흐름을 재확인했다. Firebase Phone Authentication과 모바일 SDK가 SMS·OTP를 처리하고, 앱은 SNS로 로그인된 동일 Firebase User에 phone credential을 link한 뒤 강제 갱신 ID Token을 Identity signup/Guest upgrade에 전달한다. Identity는 OTP를 직접 받지 않고 동일 UID의 SNS credential·verified phone을 Admin으로 검증하며 전화번호 원문 대신 HMAC fingerprint를 저장한다. phone-only login과 전화번호 자동 merge는 금지되고 관련 feature flag는 기본 false라 실기기 staging 검증 전 운영 활성화할 수 없다.

- 2026-09-07 Kakao Generic OIDC 콘솔 설정 절차를 최신 공식 문서로 확인했다. Kakao 앱의 REST API 키·client secret을 준비하고, Google Cloud Identity Platform의 ID 공급업체에서 코드 흐름·provider ID `oidc.kakao`·issuer `https://kauth.kakao.com`을 등록한 뒤, 표시되는 `https://<project-id>.firebaseapp.com/__/auth/handler` callback을 Kakao 리다이렉트 URI에 exact 등록해야 한다. Kakao Login과 OpenID Connect를 모두 ON으로 하고 모바일 login/link·UID 안정성을 staging에서 확인하기 전 Identity flag는 false로 유지한다.

- 2026-09-07 종료 훅 기준으로 Kakao Generic OIDC 외부 설정 필요성과 production `NO-GO` 판단을 현재 turn 식별자로 WORKLOG 끝에 동기화했다. Identity의 `oidc.kakao` mapping·kill switch 외에 Kakao Developers, Firebase Identity Platform·billing, 모바일 redirect 설정이 필요하다는 기존 결론은 변경하지 않았다.

- 2026-09-07 Kakao 로그인 운영 조건을 재확인했다. 서버는 `oidc.kakao` providerData mapping과 kill switch만 준비돼 있으며, 실제 사용에는 Kakao Developers의 OIDC·redirect 설정, Firebase project의 Identity Platform 업그레이드·billing 및 Generic OIDC provider 등록, 모바일 SDK의 `oidc.kakao` login/link·deep-link 검증, Identity의 동일 provider ID와 feature flag 설정이 모두 필요하다. 외부 프로젝트 설정은 아직 완료된 것으로 확인되지 않아 Kakao는 production `NO-GO`와 기본 비활성을 유지한다.

- 2026-09-07 종료 훅 기준으로 SNS 로그인 책임 경계 설명을 현재 turn 식별자로 WORKLOG 끝에 동기화했다. 앱·Firebase가 Google·Apple·Kakao 인증과 Firebase ID Token 발급을 담당하고 Identity가 Firebase Admin 검증 후 자체 Token을 발급한다는 기존 계약은 변경하지 않았다.

- 2026-09-07 SNS 인증 책임을 재확인했다. Google·Apple·Kakao의 사용자 인증과 Firebase ID Token 발급은 앱과 Firebase Auth가 담당하며, Identity는 각 Provider와 직접 OAuth 교환하지 않고 Firebase Admin으로 ID Token과 providerData를 검증한 뒤 자체 Access/Refresh Token으로 교환한다. Kakao는 Firebase Generic OIDC provider `oidc.kakao` 경계이며 운영 flag는 아직 비활성이다.

- 2026-09-07 SNS/Firebase 인증 구현 현황을 `develop@fa9843e` 기준으로 재점검했다. Google·Apple·Kakao provider mapping, Firebase ID Token 검증과 Identity Token 교환, 신규 MEMBER 가입, Guest 승격·기존 MEMBER merge, 다중 SocialIdentity 동기화, 탈퇴 재인증·외부 Firebase User 정리·identity release·CLEANED 재가입 gate, 가입 중단 cleanup과 owner event 기반은 코드에 구현돼 있다. 다만 Firebase·각 Provider·탈퇴/cleanup/merge/publisher 기능은 기본 비활성이며 실제 Firebase/mobile·Mongo replica set·서비스 간 staging E2E가 완료되지 않아 운영 SNS 로그인은 아직 활성화 준비 단계다. 후속 구현은 logout-all Firebase revoke, Refresh Token 응답 유실 원자성, Provider unlink·전화번호 변경, Guest 응답 유실 복구, 기존 ACTIVE 회원 Firebase rebind다.

- 2026-09-04 사용자 승인에 따라 Jira `TMI-123`에 PR #38 병합, 630개 테스트 성공, 적용 계약과 남은 staging 검증을 요약한 완료 댓글 ID `10045`를 등록했다. 이어 transition ID `41`을 적용했으며 재조회에서 상태 ID `10003` `완료`와 Resolution ID `10000` `완료`를 확인했다. 다른 Jira 필드는 변경하지 않았다.

- 2026-09-03 `TMI-123` 후속 구현이 PR #38의 merge commit `fa9843e`로 `develop`과 `origin/develop`에 반영된 것을 확인했다. Jira는 `해야 할 일`, Resolution 미설정이고 `완료` 전환 ID `41`을 사용할 수 있다. 완료 댓글과 상태 변경은 사용자 승인 전이라 수행하지 않았다.

- 2026-09-03 Learning Core 측에서 TMI-123 후속 구현을 독립 검토했다. exact `/internal/v1/events/user-merged`, typed `USER_MERGED` audience, legacy/new publisher와 UserWithdrawn 회귀, 415 분류 및 consumer 독립 재시도는 계약과 일치하고 전체 630개 테스트와 `git diff --check`가 성공했다. 다만 `retrySerializationKeepsTheSameEventIdAndPayload`는 같은 객체를 두 번 직렬화하는 비교만 수행해 실제 publisher retry에서 동일 eventId·payload가 유지되는 완료 조건을 직접 검증하지 않으므로 후속 PR 전 테스트 보강을 권장한다. 원 TMI-123은 PR #37로 `develop`에 병합됐지만 현재 후속 변경은 삭제된 원격을 가리키는 로컬 feature branch의 미커밋·미추적 상태라 아직 develop에 반영되지 않았다.

- 2026-09-03 종료 훅 기준으로 TMI-123 Learning Core UserMerged 후속 구현과 최종 630개 테스트 성공을 지정된 turn 식별자로 WORKLOG 끝에 동기화했다. Jira·commit·push는 변경하지 않았고 원격이 삭제된 현재 로컬 브랜치의 후속 PR 정리가 남아 있다.

- 2026-09-03 TMI-123 후속 구현으로 Learning Core UserMerged exact endpoint `/internal/v1/events/user-merged`, `USER_WITHDRAWN`·`USER_MERGED` typed workload purpose와 고정 audience, 신규·legacy HTTP 415 dead-letter를 적용했다. raw audience 환경변수를 제거하고 요청별 새 JWT·개인정보 claim 부재·legacy/withdrawal 회귀·Billing 성공과 Learning Core 실패의 독립 재시도 테스트를 추가했다. `./gradlew clean test` 전체 630개가 실패·오류·건너뜀 없이 통과했으며 Jira mutation과 commit·push는 수행하지 않았다.

- 2026-09-03 종료 훅 기준으로 승인된 TMI-123 후속 Jira 설명 업데이트 결과를 지정된 turn 식별자로 WORKLOG 끝에 동기화했다. 추가 Jira mutation과 애플리케이션 변경은 없다.

- 2026-09-03 사용자 승인에 따라 TMI-123 기존 설명 끝에 `TMI-125 연동 전 후속 보완` 섹션을 추가했다. exact Learning Core endpoint, typed-purpose workload JWT, HTTP 415 dead-letter, legacy backlog와 추가 완료 조건을 저장했으며 재조회로 반영을 확인했다. 상태는 `해야 할 일`, Resolution은 미설정이고 다른 Jira 필드·댓글은 변경하지 않았다.

- 2026-09-03 종료 훅 기준으로 TMI-123 후속 Jira 설명 업데이트 초안과 승인 대기 상태를 지정된 turn 식별자로 WORKLOG 끝에 동기화했다. Jira와 애플리케이션은 변경하지 않았다.

- 2026-09-03 TMI-123 현재 본문과 상태를 공식 Atlassian 연동으로 재확인하고, 기존 설명 끝에 TMI-125 연동 후속 보완 섹션만 추가하는 Jira 업데이트 초안을 준비했다. exact endpoint, typed-purpose 고정 audience, HTTP 415 dead-letter, legacy backlog와 테스트 조건을 포함하며 상태·Resolution·우선순위·담당자·댓글은 변경하지 않는 안이다. 사용자 승인 전이라 Jira mutation은 수행하지 않았다.

- 2026-09-03 TMI-123 후속 보완 중 Learning Core UserMerged endpoint 변경과 HTTP 415 dead-letter 분류를 채택했다. workload JWT는 발급 API나 별도 발급 시스템을 늘리지 않고 기존 내부 JwtEncoder를 공유하면서 typed purpose별 고정 audience를 발급하는 단일 provider 구조를 권장했다. 애플리케이션·테스트·Jira는 변경하지 않았다.

- 2026-09-03 Learning Core `TMI-125`가 전달한 TMI-123 후속 계약을 Identity `develop`과 대조했다. UserMerged의 Billing·Learning Core 독립 fan-out, TrialOwnerRebindApproved Billing-only, wire v1, Bearer workload JWT 경계와 기본 false flag는 구현돼 있다. 반면 Learning Core adapter는 구 path `/internal/v1/owners/merge/events`를 요구하고 workload provider는 탈퇴 audience 하나만 허용하며 HTTP 415가 circuit pause로 분류돼 후속 수정이 필요하다. 관련 집중 테스트 10개는 모두 통과했고 Jira mutation은 수행하지 않았다.

- 2026-09-03 `TMI-123` 구현 PR #37이 `develop`과 `origin/develop`의 commit `391b55f`에 병합됐고 구현 commit `ab433a3`이 포함된 것을 확인했다. Jira는 `해야 할 일`, Resolution 미설정이고 `완료` transition ID `41`을 사용할 수 있다. 완료 댓글과 상태 변경은 사용자 승인 전이라 수행하지 않았다.

- 2026-09-03 `feat/TMI-123-owner-event-fanout-sigv4`에서 TMI-123 Identity 구현을 완료했다. phone eligibility Bearer transport를 VPC Lattice SigV4와 bounded Retry-After로 교체하고, `UserMerged` Billing·Learning Core 및 phone rejoin Billing-only durable delivery를 consumer FIFO로 분리했다. 탈퇴 release에서 AVAILABLE lineage를 만들고 direct Firebase signup·Guest upgrade에서 동일 scope의 exact predecessor 한 건만 CONSUMED 처리하며, 다중 후보는 가입을 허용하고 reconciliation으로 격리한다. 모든 신규 capture/publisher flag는 기본 OFF다. `./gradlew clean test` 전체 621개와 `git diff --check`를 통과했으며 Jira mutation은 수행하지 않았다.

- 2026-09-03 종료 훅 기준으로 `TMI-123` Jira 생성 결과를 지정된 turn 식별자로 WORKLOG 끝에 동기화했다. 이슈는 `작업`, High, `해야 할 일`이며 추가 Jira mutation은 없다.

- 2026-09-03 사용자 승인에 따라 `TMI-123` `[Identity] Billing SigV4 및 owner event durable fan-out 구현`을 TMI 프로젝트 `작업`, High, 기본 상태 `해야 할 일`로 생성했다. 재조회에서 승인된 본문과 담당자 없음·라벨 없음을 확인했으며 댓글·상태 전환·링크·스프린트·에픽은 변경하지 않았다.

- 2026-09-03 종료 훅 기준으로 Stage 7 Identity Jira 초안과 승인 대기 상태를 지정된 turn 식별자로 WORKLOG 끝에 동기화했다. Jira는 아직 생성하지 않았다.

- 2026-09-03 Atlassian 공식 연동으로 Stage 7 Identity 구현 Jira 생성 전 검토를 수행했다. TMI의 작업 유형과 관련 제목 중복 부재를 확인하고 7-A·7-C1~C3을 한 작업으로 묶은 High 우선순위 초안을 준비했다. 사용자에게 정확한 본문을 제시하고 승인받기 전이라 Jira mutation은 수행하지 않았다.

- 2026-09-03 종료 훅 기준으로 반복 재가입·복수 benefit scope 계획 보강 결과를 지정된 turn 식별자로 WORKLOG 끝에 동기화했다. 애플리케이션·Jira 변경은 없다.

- 2026-09-03 Stage 7 계획서에 반복 탈퇴·재가입과 복수 benefit 확장 규칙을 반영했다. A→B→C에서 이전 lineage는 successor 가입 때 CONSUMED되고 successor 탈퇴가 새 AVAILABLE을 만들며, 같은 fingerprint의 과거 CONSUMED lineage가 여러 건 남아도 정상으로 본다. 모순은 동일 `consumerScopeId`의 AVAILABLE predecessor 2건 이상으로 한정했고, 서로 다른 benefit scope는 독립 처리한다. pending·dead-letter·COMPLETED NOOP 순서와 필수 테스트도 추가했으며 애플리케이션·Jira는 변경하지 않았다.

- 2026-09-03 반복 탈퇴·재가입 lineage chain을 검토했다. A 탈퇴의 AVAILABLE lineage는 B 가입에서 CONSUMED되고 B 탈퇴가 새 AVAILABLE lineage를 만들므로 C 가입 시 과거 계정·alias가 여러 건이어도 동일 scope의 자동 이전 후보는 직전 B 한 건이다. A→B delivery 미완료 중 B→C가 생기면 consumer sequence가 순서를 보장하고, A→B가 COMPLETED NOOP면 B→C도 source link 없음 NOOP로 수렴한다. 현재 계획에는 이 A→B→C 반복과 capture OFF·dead-letter 경계 테스트를 추가할 필요가 있으며 코드·계획 본문·Jira는 변경하지 않았다.

- 2026-09-03 복수 무료시험 권리와 phone lineage cardinality를 구분했다. 현재 0/1/2+ 판정은 전체 응시권 개수가 아니라 동일 `consumerScopeId`에서 가능한 predecessor account lineage 수에 적용된다. 향후 서로 다른 프로그램은 stable scope별 lineage/event로 독립 확장할 수 있으며, 같은 scope에서 여러 benefit subject를 옮겨야 하면 v1을 확장하지 않고 별도 versioned 계약이 필요하다. 코드·계획 본문·Jira는 변경하지 않았다.

- 2026-09-03 수정된 Stage 7 계획 기준으로 Identity 예정 구현을 설명했다. 7-A는 Billing eligibility 전송만 SigV4로 바꾸고, 7-C는 exact phone lineage에서 Billing-only rebind event를 원자 생성해 durable delivery한다. Billing이 상태를 판정하고 Learning Core는 read-only continuation으로 target 새 Session을 만들며 source 학습 기록은 이전하지 않는다. 코드·계획 본문·Jira는 변경하지 않았다.

- 2026-09-03 종료 훅 기준으로 Stage 7 계획서 phone continuation 동기화 완료 상태를 재확인했다. 지정된 turn 식별자를 WORKLOG 끝에 append했으며, 애플리케이션·Billing·Learning Core 코드는 변경하지 않았다.

- 2026-09-03 Stage 7 계획서를 현재 cross-service 구현에 맞게 갱신했다. Billing `develop`의 `TMI-120`은 없음·OPEN·RETAKE_AVAILABLE이면 owner CAS, GRADING이면 retryable pending, COMPLETED이면 NOOP를 적용하고 read-only continuation discovery와 exact reserve echo를 제공한다. Learning Core `TMI-122`는 target에 기존 Session이 없을 때만 이를 발견해 같은 AttemptGroup·mockExamId의 새 target Session을 만들며 source 기록은 이전하지 않고 전체 457개 테스트가 통과했지만 `develop` 병합은 남았다. Identity 계획에는 해당 상태표·discovery/echo·status-first 복구, Billing-only event와 Learning Core event 부재를 반영했다. 애플리케이션 코드는 변경하지 않았다.

- 2026-09-03 Stage 7 구현 계획서를 작성하고 고정 구현 순서 7단계에 연결했다. Stage 7을 7-A Identity→Billing SigV4 transport, 7-B Billing `TMI-120` readiness·선배포, 7-C Identity owner event core/delivery/phone lineage, 7-D Learning Core `UserMerged` consumer, 7-E staging E2E·canary로 분해했다. `UserMerged`는 Billing·Learning Core 두 delivery, `TrialOwnerRebindApproved`는 Billing-only로 확정하고, consumer-wide sequence FIFO, exact phone rejoin lineage, P30D published retention·P90D dead-letter review/no auto-delete, 독립 capture/publisher flag, legacy·pre-cutover backfill 금지와 전체 테스트·활성화 gate를 명세했다. 애플리케이션 코드는 변경하지 않았고 `git diff --check`가 통과했다.

- 2026-09-02 turn 종료 기준으로 Stage 7 owner 이전 정책 확정 기록을 동기화했다. 제품 정책은 `UserMerged`의 Billing·Learning Core fan-out, `TrialOwnerRebindApproved`의 Billing-only delivery, phone proof만으로 Learning Core 과거 데이터 이전 금지와 strong-proof continuity 후속 분리다. 구현 전 local predecessor lineage·consumer별 순서 fencing·독립 flag/circuit·보존 계약을 계획서에 고정하고 Billing ADR-003의 phone rejoin Learning Core delivery 요구를 보정해야 한다.

- 2026-09-02 Stage 7 owner 이전의 제품 정책을 확정했다. `UserMerged`는 Guest가 기존 Member로 합쳐지는 canonical merge이므로 Billing과 Learning Core에 각각 durable delivery를 만든다. 동일 phone proof 기반 `TrialOwnerRebindApproved` v1은 Billing에만 전달해 retained 무료시험 owner link만 이전하고 Claim·Grant·consumption은 초기화하거나 복원하지 않는다. Learning Core의 과거 시험·결과는 phone proof만으로 자동 이전하지 않으며, old-account-derived strong proof를 정의한 별도 continuity event·후속 Jira에서만 다룬다. 따라서 현 Billing ADR-003의 phone rejoin Learning Core delivery·route 요구는 구현 전에 이 정책에 맞게 보정해야 한다. Stage 7 계획서에는 local predecessor lineage의 모호성 fail-closed, consumer별 predecessor ordering, published P30D·dead-letter P90D review/no auto-delete, consumer별 publisher flag·circuit pause와 legacy Billing backfill 금지를 기술 기본값으로 확정해야 한다.

- 2026-09-02 `TrialOwnerRebindApproved` 발행 정책의 선택지를 Billing 권리와 Learning Core 개인 학습 데이터로 분리해 분석했다. 선택지는 동일 phone proof만으로 양쪽 전체 자동 이전, Billing만 제한 자동 rebind하고 Learning Core 이력은 이전하지 않는 방식, old-account-derived strong proof가 있을 때만 전체 이전, 운영자 수동 이전, 자동 이전 전면 금지다. 권장안은 phone-scoped Billing Claim은 source CLEANED·REVOKED, target VERIFIED와 동일 retained candidate를 조건으로 owner link만 자동 rebind하되 Claim·사용량을 복원하지 않고, Learning Core 시험·결과는 phone proof만으로 이전하지 않으며 old-account proof가 있을 때만 별도 승인하는 혼합 정책이다. 이 권장안을 채택하면 현재 ADR-003의 모든 `TrialOwnerRebindApproved`가 Billing·Learning Core 두 delivery를 가진다는 계약을 조정하거나 Billing-only event/approval scope를 별도로 정의해야 한다.

- 2026-09-02 첨부된 Identity durable fan-out 전달문과 Billing ADR-003을 현재 Identity 코드에 대조해 Stage 7의 실제 작업 범위를 정리했다. Stage 7 Identity 범위는 공통 SigV4 transport/result, immutable owner event core와 consumer별 delivery, 기존 `UserMerged` writer의 reader-first 전환, 신규 `TrialOwnerRebindApproved` 생성 gate, Billing/Learning Core별 publisher·circuit·flag, legacy preflight와 atomicity/lease/retry/privacy contract test다. 현재 `UserMerged` event 생성은 이미 존재하지만 event payload와 단일 delivery 상태가 `UserMergedOutbox` 한 문서에 결합돼 있어 분리가 필요하고, `TrialOwnerRebindApproved`는 아직 코드에 없다. Billing `TMI-120`과 Learning Core consumer는 producer 활성화 전에 비활성 선배포해야 한다. 동일 phone proof만으로 과거 시험 owner를 자동 이전하면 번호 재할당 시 개인정보가 노출될 수 있으므로, Identity가 source→target 이전을 승인할 충분한 proof를 무엇으로 볼지는 Stage 7 계획서의 명시적 product/security gate로 남긴다.

- 2026-09-02 `UserMerged`·`TrialOwnerRebindApproved` consumer별 durable fan-out의 순서를 분류했다. 이 작업은 기존 12단계의 Stage 12 ACTIVE 회원 Firebase credential rebind가 아니라 Billing의 retained trial owner 이전을 완성하는 Stage 7 후속 범위다. 세부 순서는 7-A phone eligibility SigV4 기반 연동, 7-B Billing owner-rebind consumer `TMI-120` 비활성 선배포, 7-C Identity event core+`BILLING`/`LEARNING_CORE` 독립 delivery fan-out, 7-D Learning Core owner migration/source deny consumer, 7-E cross-service staging E2E·순차 활성화다. 따라서 질문의 Identity durable fan-out은 Stage 7-C이며 전체 번호를 새로 매기면 현재 Stage 7과 Stage 8 사이에 들어간다.

- 2026-09-02 Stage 6 이후의 즉시 다음 작업을 확정했다. Stage 7 전체 목표는 Billing eligibility 운영 연동이지만 Billing `TMI-110` consumer 구현은 이미 완료됐다. 따라서 지금 먼저 할 일은 Identity의 phone eligibility Bearer JWT adapter를 AWS SigV4로 교체하고 status+bounded Retry-After, Lattice base URL·고정 path·region 검증, 최소 AWS SDK v2 dependency와 contract test 범위를 정하는 저장소 계획서 작성이다. 그 뒤 Jira 생성·Identity 코드 구현을 진행하고, 별도 Billing/AWS 인프라 선배포와 staging E2E를 거쳐 publisher를 활성화한다.

- 2026-08-31 phone eligibility transport를 JWT에서 AWS SigV4로 바꾸는 이유를 명확히 했다. JWT 자체의 보안 문제 때문이 아니라 Billing 운영 ingress가 VPC Lattice `AWS_IAM`을 policy enforcement point로 채택했기 때문이다. SigV4를 사용하면 Lattice가 Billing 애플리케이션 도달 전에 ECS task role·환경·HTTP method·exact path를 검증하고 SDK가 자동 회전 task credential을 사용한다. JWT를 유지하려면 Billing에 별도 workload JWT decoder/filter, issuer·audience·JWKS rotation, principal-route authorization과 장애 운영을 추가하고 Lattice AWS_IAM 선택을 변경해야 한다. 따라서 현재 승인 아키텍처에서는 SigV4가 일관된 선택이며, JWT 유지도 불가능한 것은 아니지만 별도 ADR 변경 범위다.

- 2026-08-31 현재 turn 기준으로 C-02의 실제 수정 대상을 쉬운 흐름으로 확정했다. phone eligibility event 생성·outbox·Billing consumer·무료시험 정책은 그대로 유지하고, Identity가 Billing에 POST하는 마지막 전송 구간만 Bearer workload JWT에서 ECS task role 기반 AWS SigV4로 교체한다. 함께 delivery 응답을 status+bounded Retry-After로 확장하고 Lattice base URL·고정 route·region 설정과 route별 IAM을 맞춘다. UserMerged·UserWithdrawn workload JWT 전송은 변경 대상이 아니다.

- 2026-08-31 C-02 수정 범위를 사용자 관점으로 다시 설명했다. 이벤트 JSON, outbox, Billing eligibility 처리와 무료시험 정책은 그대로 두고 Identity의 Billing 호출 직전 전송 계층만 바꾼다. 현재 `Authorization: Bearer ...`를 만드는 `JdkPhoneEligibilityBindingDeliveryAdapter` 대신 동일 payload를 ECS task role로 SigV4 서명하는 adapter를 연결하고, delivery port 반환값을 status 단일 값에서 status+bounded Retry-After로 확장해 429·503 재시도 시각에 반영한다. phone eligibility 설정의 audience는 제거하고 환경별 Lattice base URL·고정 endpoint path·region을 검증한다. UserMerged·UserWithdrawn의 workload JWT 전송과 outbox/event schema는 변경하지 않는다.

- 2026-08-31 C-02 Identity→Billing workload transport 불일치를 세 저장소에 대조해 유효한 High/production gate 진단으로 확정했다. Identity phone eligibility adapter는 현재 audience 기반 Bearer workload JWT를 보내고 delivery port가 HTTP status `int`만 반환해 `Retry-After`를 전달하지 못한다. Billing 목표 ingress는 VPC Lattice `AWS_IAM`이며 Identity task role의 exact eligibility POST만 허용하고, Learning Core는 이미 AWS SDK v2 `AwsV4HttpSigner`·`vpc-lattice-svcs`·`ap-northeast-2` 경로를 구현했다. 수정 범위는 phone eligibility 전용 adapter를 SigV4로 교체하고 delivery result를 status+bounded Retry-After로 확장하며, full endpoint/audience 설정을 environment-specific Lattice base URL·고정 path·region validation으로 보정하는 것이다. phone eligibility의 이전 JWT runtime transport는 제거하되 UserMerged·UserWithdrawn에 쓰이는 공용 workload JWT 발급 기반은 유지한다. local/test는 이전 JWT가 아니라 fake/WireMock port를 사용한다. 애플리케이션·계약·Jira mutation은 수행하지 않았다.

- 2026-08-31 다음 고정 작업 Stage 7의 실제 잔여 범위를 확인했다. Billing `TMI-110`의 `/internal/v1/eligibility/trial/events` consumer와 inbox·`TrialEligibility` projection Transaction은 이미 구현돼 있으므로 consumer 애플리케이션을 다시 만들지 않는다. Billing의 transaction 가능한 Mongo·index·Lattice AWS_IAM ingress를 Identity producer보다 먼저 staging에 배포하고, Identity의 현재 Bearer workload credential 기반 JDK adapter를 ECS task role SigV4(`vpc-lattice-svcs`, `ap-northeast-2`) transport로 교체하며, 제한된 `Retry-After`·409 conflict 분류·same-event retry를 보완한 뒤 positive/negative E2E를 수행한다. eligibility event는 무료권 발급이 아니라 projection 갱신이며 실제 Claim·1-unit Grant는 최초 reserve Transaction에서 lazy 생성한다. Jira `TMI-114`는 PR #36 병합 댓글 ID `10043`이 등록됐고 상태·Resolution 모두 `완료`임을 읽기 전용으로 재확인했다. 애플리케이션·Jira mutation은 수행하지 않았다.

- 2026-08-28 `TMI-114` Stage 6를 구현했다. exact Firebase project·UID별 `AbandonedFirebaseEnrollmentCleanup`과 RESUMABLE·CLEANUP_IN_PROGRESS·RETRY_WAIT·FINALIZED·CLEANED·RECONCILIATION_REQUIRED 상태, generation·version·lease CAS, terminal lifecycle P7D·attempt PT24H TTL을 추가했다. enrollment start/reuse와 signup·Guest upgrade finalize를 Mongo Transaction lifecycle에 연결하고 cleanup claim 이후 `FIREBASE_ENROLLMENT_RESTART_REQUIRED` 409를 반환한다. worker는 최신 attempt·ACTIVE GUEST·FirebaseIdentity·linked SocialIdentity·verified phone alias를 preflight한 뒤 inspect→disable→refresh revoke→provider obligation→delete→absence confirm 순서로 처리하며 retry·reconciliation에 수렴한다. 고정 cutover·최대 100건·dry-run 우선 legacy capture와 기본 비활성 설정도 추가했다. `./gradlew clean test` 전체 113개 suite·600개 테스트와 `git diff --check`가 통과했다. Git commit·push와 Jira 댓글·상태 변경은 수행하지 않았다.

- 2026-08-28 종료 훅 기준으로 `TMI-114` 생성 결과를 최종 동기화했다. 사용자 승인 뒤 작업·High로 생성됐고 재조회 상태는 `해야 할 일`, Resolution 없음이며 담당자·라벨·컴포넌트·댓글은 없다. 승인한 Stage 6 본문이 저장됐고 별도 댓글·상태·링크 mutation은 없었다.
- 2026-08-28 사용자 최종 승인 뒤 Atlassian 공식 도구로 `TMI-114` `[Identity] 가입 중단 Firebase User cleanup lifecycle 구현`을 TMI 프로젝트 `작업`, High 우선순위로 생성했다. 재조회 결과 상태 `해야 할 일`, Resolution 없음, 담당자·라벨·컴포넌트·댓글 없음이며 승인한 목적·시간·상태·포함·안전·제외·완료·rollout 본문이 저장됐다. 이슈 링크와 상태 전환은 수행하지 않았다.
- 2026-08-28 Stage 6 Jira 생성 전 읽기 전용 검토를 완료했다. Atlassian 공식 도구로 TMI 프로젝트의 `작업` 유형 ID `10003`, High 우선순위 ID `2`, 최근 Identity 이슈 형식과 최신 이슈 `TMI-113`을 확인했고, 가입 중단·Firebase User cleanup·abandoned cleanup 검색 결과 중복 이슈는 0건이었다. 제안 이슈는 `[Identity] 가입 중단 Firebase User cleanup lifecycle 구현`, 작업·High이며 계획서의 별도 lifecycle, generation·lease·version fencing, state별 no-TTL/terminal retention, owner preflight, Firebase 멱등 삭제, restart-required 모바일 계약, bounded capture와 staging E2E를 포함한다. 사용자에게 정확한 본문을 제시하고 최종 승인받기 전이라 Jira mutation은 수행하지 않았다.
- 2026-08-28 Stage 6 상태별 `cleanupAt` 의미를 설명했다. `cleanupAt`은 Firebase User 삭제 실행 시각이 아니라 Mongo TTL이 coordination record를 제거해도 되는 시각이다. RESUMABLE·CLEANUP_IN_PROGRESS·RETRY_WAIT는 가입 재개·lease·retry에 필요하고 RECONCILIATION_REQUIRED는 운영 판단이 끝나지 않았으므로 lifecycle과 관련 attempt에 TTL을 두지 않는다. FINALIZED·CLEANED에서만 terminalAt을 확정해 source attempt는 24시간 뒤, generation·결과·중복 방지 근거인 lifecycle은 7일 뒤 제거한다. reconciliation이 해결되면 해결 시각부터 retention을 계산하며 Mongo TTL 지연은 허용하고 안전 판단은 status·generation·owner guard를 기준으로 한다. 애플리케이션·계획서·Jira는 변경하지 않았다.
- 2026-08-28 Stage 6 계획서의 TTL 계약을 수정했다. lifecycle·attempt 동일 `P30D`와 “최소 30일” 표현을 제거하고, RESUMABLE·CLEANUP_IN_PROGRESS·RETRY_WAIT·RECONCILIATION_REQUIRED에는 `cleanupAt`을 두지 않는다. FINALIZED·CLEANED 전환 시에만 terminal 시각 기준 lifecycle `P7D`, exact target source attempt `PT24H` cleanupAt을 설정해 attempt가 먼저 사라지고 lifecycle이 중복 방지 기록으로 더 오래 남도록 했다. reconciliation은 운영 해결 전 자동 TTL 삭제하지 않으며 7일·24시간은 조정 가능한 운영 기본값이지 grace·기능상 최소값이 아니다. 애플리케이션·Jira는 변경하지 않았다.
- 2026-08-28 `docs/contracts/firebase-abandoned-enrollment-cleanup-stage-6-plan.md`를 작성하고 고정 구현 순서 6단계에 연결했다. enrollment TTL `PT10M`, 가입 재개 grace `PT24H`, terminal lifecycle retention `P7D`, terminal attempt retention `PT24H`를 분리하고 exact Firebase project·UID 단위의 별도 lifecycle, generation·lease·version fencing, startOrReuse·finalize·cleanup claim의 Mongo Transaction 조정, DIRECT_SIGNUP·GUEST_USER owner preflight, disable→revoke→provider obligation→delete→absence confirm, restart-required 모바일 계약, legacy attempt bounded capture와 staging 활성화 조건을 정의했다. withdrawal lifecycle은 재사용하지 않고 Firebase SDK primitive만 공통화하며 애플리케이션·Jira는 변경하지 않았다.
- 2026-08-28 현재 turn 기준으로 회원탈퇴 cleanup과 다음 가입 중단 cleanup의 경계를 재확인했다. 가입 완료 회원의 withdrawal 경로는 내부 User를 `WITHDRAWN` tombstone으로 보존하면서 외부 Firebase User와 로그인·전화번호 identity 연결을 정리하는 구현이 완료돼 있다. 다음 Stage 6은 내부 User 생성 전 이탈하여 Firebase에만 남은 가입 중단 계정의 별도 cleanup이다. 애플리케이션·Jira는 변경하지 않았다.
- 2026-08-28 다음 Stage 6의 범위를 회원탈퇴 cleanup과 다시 구분했다. 기존 회원탈퇴 lifecycle은 내부 User를 개인정보가 제거된 `WITHDRAWN` tombstone으로 남기고 모든 RefreshSession을 폐기하며, 외부 Firebase User를 실제 삭제한 뒤 exact FirebaseIdentity와 User 소유 SocialIdentity를 삭제하고 PhoneIdentity·fingerprint alias·혜택 binding을 release/revoke하여 `CLEANED`에 수렴한다. 다음 작업은 이 완료된 회원탈퇴 경로를 다시 만드는 것이 아니라, 가입 finalize 전에 이탈해 내부 canonical User가 아직 없는 고아 Firebase User를 안전하게 찾고 grace 뒤 정리하는 별도 lifecycle이다. 코드·Jira는 변경하지 않았다.
- 2026-08-28 회원탈퇴 Firebase User cleanup과 Stage 6 가입 중단 cleanup의 시간 계약을 정정했다. 회원탈퇴 Transaction이 만드는 `UserWithdrawalLifecycle`은 `EXTERNAL_CLEANUP_PENDING`과 `nextAttemptAt=requestedAt`으로 시작하므로 별도 grace 없이 즉시 due다. worker 기능이 활성화돼 있으면 기본 `PT5S` scheduler 주기에 claim되어 target guard와 Firebase inspect를 거친 뒤 disable→refresh token revoke→provider deletion obligation→delete→삭제 확인을 수행한다. 실패 시에만 기본 `PT5S`부터 최대 `PT1H`까지 backoff하며 최대 12회 시도한다. 앞서 논의한 `PT24H` grace 후보는 아직 회원가입을 완료하지 않은 고아 Firebase User를 처리하는 Stage 6에만 해당하고, 현재 `FIREBASE_ENROLLMENT_CLEANUP_RETENTION=PT24H` 역시 enrollment 문서 보존기간이지 회원탈퇴 삭제 유예가 아니다. 코드·Jira는 변경하지 않았다.
- 2026-08-28 Stage 6 cleanup 시간 계약 기록을 최종 동기화했다. 현재 `PT10M`은 enrollment consume TTL, `PT24H`는 attempt Mongo TTL 보존기간이며 외부 Firebase User 삭제 유예는 미구현이다. 별도 `abandonedCleanupGrace=PT24H`를 기본 후보로 두되 record retention과 분리하고 owner preflight·CAS fencing을 함께 적용하는 안이 다음 계획서 기준이다.
- 2026-08-28 Stage 6 유예기간을 현재 코드와 구분했다. 현재 `FIREBASE_ENROLLMENT_TTL` 기본 `PT10M`은 PENDING attempt의 consume 가능 시간이고 `FIREBASE_ENROLLMENT_CLEANUP_RETENTION` 기본 `PT24H`는 `cleanupAt=expiresAt+retention`으로 Mongo attempt 문서를 TTL 보존하는 시간일 뿐 외부 Firebase User 삭제 유예가 아니다. Stage 6 권장 초안은 최근 attempt 만료 뒤 `PT24H`의 별도 abandoned cleanup grace를 두고, 그 안에 돌아온 사용자는 새 10분 attempt로 재개하며, cleanup terminal 이력은 grace와 분리해 더 길게 보존하는 것이다. grace는 Firebase link와 Mongo finalize 사이의 네트워크·앱 중단·동시 실행 오판을 줄이지만 CAS cleanup claim과 finalize fencing을 대체하지 않는다. 코드·Jira는 변경하지 않았다.
- 2026-08-28 `TMI-111` 다음 개발 순서를 고정 체크리스트와 현재 enrollment 코드에 대조했다. 다음은 Stage 6 가입 중단 Firebase User cleanup이다. Firebase exchange가 `FirebaseIdentity` 없는 UID에 PENDING enrollment를 만들고 signup·Guest upgrade Transaction이 이를 consume하지만, 사용자가 phone link 뒤 finalize 전에 이탈하면 attempt TTL만 정리될 뿐 외부 Firebase User와 phone 점유는 남는다. Stage 6은 resume 유예 뒤 expired·unconsumed attempt만 별도 cleanup lifecycle로 claim하고, exact project·UID에 내부 Firebase/Social/Phone owner나 완료 User mapping이 없음을 재검증한 뒤 disable·refresh revoke·delete에 멱등 수렴해야 한다. 기존 ACTIVE·WITHDRAWN·MERGED User와 complete withdrawal lifecycle은 절대 삭제하지 않고 mixed owner는 reconciliation으로 보낸다. 코드·Jira는 변경하지 않았다.
- 2026-08-28 `TMI-111` Jira 종료 작업 기록을 최종 확인했다. PR #35 merge commit `776e8fa195c2ec86c548e0733537900d38d8efd6`, 종료 댓글 ID `10042`, status·Resolution `완료`가 현재 상태이며 production publisher·backfill은 staging 검증 전까지 계속 비활성이다.
- 2026-08-28 사용자 승인에 따라 Identity `TMI-111`에 PR #35 병합, 구현 범위, 109개 suite·591개 테스트와 남은 staging 위험을 담은 종료 댓글 ID `10042`를 등록하고 완료 transition ID `41`만 적용했다. 재조회 결과 상태 ID `10003`과 Resolution 모두 `완료`이며 담당자·우선순위·라벨·본문·링크와 기존 댓글은 변경하지 않았다.
- 2026-08-28 Identity `TMI-111` PR #35가 merge commit `776e8fa195c2ec86c548e0733537900d38d8efd6`로 local·origin `develop`에 반영됐고 작업 트리가 깨끗함을 확인했다. Jira는 여전히 `해야 할 일`, Resolution 없음이고 완료 transition ID `41`이 사용 가능하다. Jira 규칙에 따라 구현 요약·변경 파일·591개 테스트·남은 staging 위험을 담은 댓글 등록과 상태 `완료` 전환안을 사용자에게 제시하며 승인 전에는 mutation하지 않는다.
- 2026-08-28 `TMI-111` Identity producer를 구현했다. 탈퇴 User CAS·lifecycle·phone eligibility revocation·RefreshSession `ACCOUNT_WITHDRAWN` 폐기와 `UserWithdrawnOutbox` 저장을 같은 Mongo Transaction으로 묶고, unique userId·due·expired lease·dead-letter review·TTL index를 추가했다. v1 payload는 eventId·schemaVersion·userId·withdrawnAt만 직렬화하며 atomic lease, 408·425·429·5xx 재시도, payload·배포 설정 오류 dead-letter, manual replay와 publisher outcome·backlog·oldest age·delivery lag metric을 제공한다. workload JWT는 RS256, workload issuer, exact audience, `sub=identity-service`, `nbf=iat`, TTL `PT2M`, `typ=JWT`, `kid`, UUID `jti`로 매 전달마다 발급하고 사용자 Token을 재사용하지 않는다. 현재·구 Public Key를 함께 노출할 JWKS rotation 구성과 고정 lower·upper bound·최대 100건·dry-run 우선 backfill을 구현했다. 모든 기능은 기본 비활성이며 실제 credential·key·endpoint는 기록하지 않았다. 전체 109개 suite·591개 테스트가 실패·오류·건너뜀 없이 통과했고 `git diff --check`도 통과했다. commit·push·Jira mutation은 수행하지 않았다.
- 2026-08-28 `TMI-111` workload JWT 계약을 승인해 Stage 5 계획서에 반영했다. 기존 Identity RS256/JWKS를 재사용하되 workload 전용 HTTPS issuer, exact audience `learning-core-user-withdrawn`, `sub=identity-service`, TTL·최대 lifetime `PT2M`, workload skew `PT30S`, `nbf=iat`를 사용하고 중복 `service` claim은 제거한다. `typ=JWT`, non-blank `kid`, `nbf=iat`는 consumer 검증 경계이며 `jti`는 Identity가 canonical UUID로 발급하지만 인가·event 멱등성 필수 입력이나 저장 key로 사용하지 않는다. 408·425·429·5xx 등은 재시도하고 payload 오류와 인증·endpoint 배포 오류는 구분해 격리한다. production 활성화 전 production credential provider, 다중 key JWKS와 최소 `PT31M` overlap rotation, 상호 사용 거절·golden Token staging E2E가 필요하다. 애플리케이션·Jira는 변경하지 않았다.
- 2026-08-28 Stage 5에서 실제 남은 Identity 구현을 순서대로 정리했다. `TMI-111`은 `UserWithdrawnOutbox` domain·Repository·index, withdrawal Transaction 원자 연결, workload-authenticated publisher의 lease·retry·dead-letter·replay, 고정 cutover 범위의 dry-run bounded backfill, 관측·startup validation·runbook과 staging E2E를 구현해야 한다. 첫 코드 작업은 publisher가 아니라 Outbox와 `UserWithdrawalTransactionService` 연결이며, Learning Core는 추가 수정 대상이 아니다. 코드·Jira는 변경하지 않았다.
- 2026-08-28 사용자가 당시 요청은 Learning Core 개발이 아니었다고 명확히 정정했다. 따라서 과거 `TMI-109` Learning Core 구현·병합은 실제 존재하는 별도 결과이지만, 사용자가 요청한 Identity 작업의 완료나 올바른 범위 수행으로 간주하지 않는다. Identity의 `UserWithdrawnOutbox`, withdrawal Transaction 연결, publisher, retry·dead-letter와 backfill은 여전히 미구현이며 다음 작업은 Identity 저장소의 `TMI-111`이다. 코드·Jira는 변경하지 않았다.
- 2026-08-28 과거 “구현 완료” 보고의 범위를 재확인했다. 해당 구현은 Learning Core `TMI-109` consumer·inbox·deny marker·Access Token gate 완료를 의미하며 Stage 5 전체 완료가 아니다. 현재 Learning Core는 event를 받을 수 있지만 Identity가 `UserWithdrawn` event를 생성·전달하지 않으므로 자동 deny marker 생성은 아직 동작하지 않는다. Stage 5 완성에는 Identity `TMI-111`의 outbox·publisher·bounded backfill·workload credential과 양 서비스 staging E2E가 남아 있다. 코드·Jira는 변경하지 않았다.
- 2026-08-28 Stage 5 계획서와 `TMI-111`의 관계를 설명했다. 계획서는 Identity producer와 Learning Core consumer, 공통 wire·배포·E2E를 모두 묶은 상위 설계 문서이며 별도 세 번째 구현 작업이 아니다. 계획서의 Learning Core 절은 완료된 `TMI-109`, Identity outbox·publisher·backfill 절은 다음 `TMI-111`에 해당한다. 계획서 10행의 `Identity producer Jira: 후속 생성 예정`은 Jira 생성 전 문구라 현재 `TMI-111`로 갱신할 필요가 있지만 이번 설명에서는 계획서·코드·Jira를 변경하지 않았다.
- 2026-08-28 완료된 `TMI-109`와 다음 `TMI-111`의 차이를 설명했다. `TMI-109`는 Learning Core가 이미 발생한 `UserWithdrawn` event를 인증·멱등 수신하고 inbox·deny marker를 저장해 사용자 요청을 차단하는 consumer 작업이다. `TMI-111`은 Identity가 탈퇴 commit과 outbox를 원자적으로 저장하고 publisher·retry·dead-letter·bounded backfill로 그 event를 전달하는 producer 작업이다. 현재는 receiver만 있고 sender가 없는 상태이며 코드·Jira는 변경하지 않았다.
- 2026-08-28 다음 작업인 Identity `TMI-111`의 범위를 현재 탈퇴 Transaction, 기존 UserMerged outbox/publisher와 완료된 Learning Core `TMI-109` consumer에 대조해 설명했다. 신규 탈퇴 commit과 `UserWithdrawnOutbox`를 원자적으로 저장하고, lease·retry·dead-letter publisher로 Learning Core에 at-least-once 전달하며, capture 배포 전후의 최근 WITHDRAWN User만 고정 cutover 범위로 backfill하는 작업이다. workload 인증 profile과 운영 TTL·retention 승인, replica set staging E2E 전에는 publisher를 활성화하지 않는다. 코드·Jira는 변경하지 않았다.
- 2026-08-28 Learning Core `TMI-109` 구현 완료를 재검증했다. PR #23 merge commit `4baa4f20b7b179290dd743325ef7b251a408da47`가 local·origin `develop`에 반영됐고 Jira `TMI-109`는 이미 status·Resolution `완료`다. 사용자가 추정한 `TMI-111`은 Identity outbox·publisher·backfill 후속 이슈로서 상태 `해야 할 일`이고 대응 production 코드가 없어 닫지 않았다. 이번 확인에서는 Jira mutation을 수행하지 않았다.
- 2026-08-27 `TMI-109`의 저장소 범위를 재확인했다. 이 이슈는 Learning Core가 `UserWithdrawn` event를 수신해 inbox·유한 deny marker를 저장하고 기존 Access Token을 차단하는 consumer 작업이므로 Learning Core 애플리케이션 수정이 본래 범위다. Identity의 outbox·publisher·bounded backfill은 후속 Jira로 분리돼 있으며 이번 설명에서는 코드·Jira를 변경하지 않았다.
- 2026-08-27 Jira `TMI-109` 완료 전환 요청을 받았으나 Learning Core의 구현 파일이 아직 working tree의 미커밋·미추적 변경이고 local `develop`, `origin/develop` 모두 기존 커밋 `514fb49`를 가리키는 것을 확인했다. Jira도 `해야 할 일`, Resolution 없음 상태다. PR 병합 확인 전 완료 전환 금지 규칙에 따라 Jira mutation을 수행하지 않았다. 구현 파일을 선택적으로 commit·push하고 PR을 merge한 뒤 다시 완료 전환해야 한다.
- 2026-08-27 Learning Core 저장소에서 Jira `TMI-109` consumer를 구현했다. workload JWT 전용 `/internal/v1/events/withdrawn`, v1 semantic inbox와 유한 userId deny marker의 Mongo Transaction, duplicate 204·payload conflict 409, 사용자 JWT 이후 `401 ACCOUNT_WITHDRAWN`, marker store 장애 fail-closed 503, TTL index와 기본 비활성 설정을 추가했다. Learning Core 전체 389개 테스트가 성공했으며 기존 공개 API·BaseResponse·AI `user_id=examId` 계약을 유지했다. 실제 workload profile·TTL 설정 승인과 replica set staging E2E, Identity outbox·publisher·bounded backfill은 남아 있다. Jira 댓글·상태와 Git commit·push는 변경하지 않았다.
- 2026-08-27 사용자 최종 승인 뒤 Stage 5 Learning Core consumer 선행 이슈 `TMI-109` `[Learning Core] UserWithdrawn inbox·deny marker·Access Token 차단 gate 구현`을 TMI 프로젝트 `작업`, High 우선순위로 생성했다. exact endpoint `/internal/v1/events/withdrawn`, v1 inbox·deny marker Transaction, JWT 이후 `ACCOUNT_WITHDRAWN`, marker store 장애 fail-closed, TTL·workload chain·consumer 선배포를 포함하고 Identity producer·backfill은 후속 Jira로 제외했다. 재조회 결과 상태 `해야 할 일`, Resolution 없음, 담당자·라벨·댓글 없음과 중복 0건을 확인했다.
- 2026-08-27 사용자가 `TMI-109` 작성 내용이 보이지 않는다고 알려 Jira Description을 다시 조회했다. API의 원문 Description과 renderedFields HTML에는 목적, 구현 범위, 제외 범위, 완료 조건과 선행·후속 관계가 모두 저장되어 있다. 별도 브라우저 화면은 Atlassian 로그인 페이지로 전환돼 사용자 세션의 실제 issue layout은 확인하지 못했다. Jira 수정은 수행하지 않았으며 동일 Description 재저장 또는 comment 복제는 변경 내용을 제시하고 별도 승인받은 뒤에만 진행한다.
- 2026-08-27 사용자가 요청해 `TMI-109`에 저장된 Description의 목적, 구현 범위, 제외 범위, 완료 조건과 선행·후속 관계를 대화에 그대로 다시 제공했다. Jira mutation과 애플리케이션·계약 코드 변경은 없었고 문서 형식 검증이 통과했다.
- 2026-08-27 사용자 요청에 따라 Stage 5 Learning Core internal endpoint를 하이픈 없는 `/internal/v1/events/withdrawn`으로 단순화했다. 계획서와 현재 상태에서 이전 endpoint가 남지 않았고 문서 형식 검증이 통과했다. semantic digest domain separator와 문서 파일명은 URL이 아니므로 변경하지 않았으며 애플리케이션 코드·Jira는 수정하지 않았다.
- 2026-08-27 저장소의 하이픈 포함 HTTP 경로를 Controller, Security allowlist, request logging, Springdoc 설정과 계약 문서에서 전수 점검했다. 실제 Identity 비즈니스 API에는 `/api/v1/auth/check-email`, `/api/v1/auth/logout-all`, `/api/v1/auth/firebase/auth-methods/sync`가 남아 있다. `/.well-known/jwks.json`은 표준 discovery 경로이고 `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs`는 Springdoc 경로라 별도로 분류했다. 계획 문서에는 아직 구현되지 않은 `/internal/v1/phone-eligibility-bindings/events`가 있으며 UserMerged publisher의 실제 endpoint는 환경 설정값이라 코드에 고정되지 않았다. 기존 공개 API 변경은 호환성 결정이 필요하므로 이번 점검에서는 코드·경로를 수정하지 않았고 `git diff --check`가 통과했다.
- 2026-08-27 Stage 5의 첫 구현 이슈인 Learning Core `UserWithdrawn` inbox·deny marker·JWT gate Jira 초안을 준비했다. TMI 프로젝트에서 URL·API·endpoint 관련 중복 후보를 검색했으나 동일 범위 이슈는 없었고 `작업` 유형을 사용할 수 있음을 확인했다. 제안은 High 우선순위이며 exact endpoint `/internal/v1/events/withdrawn`, v1 payload, local Mongo Transaction, `401 ACCOUNT_WITHDRAWN`, fail-closed `503 WITHDRAWAL_DENY_GATE_UNAVAILABLE`, 유한 TTL과 consumer 선배포를 포함한다. workload 인증 실제 profile과 명시적 clock skew 등 미확정 운영값은 임의로 만들지 않고 승인 전 production 비활성 조건으로 둔다. Jira mutation 규칙에 따라 제목·본문·완료 조건을 사용자에게 제시했으며 최종 승인 전에는 이슈를 생성하지 않는다.
- 2026-08-27 `docs/contracts/user-withdrawn-downstream-deny-marker-stage-5-plan.md`를 작성하고 고정 구현 순서 5단계에서 연결했다. v1 wire event를 eventId·schemaVersion·userId·withdrawnAt으로 고정하고 Identity withdrawal Transaction outbox, lease·retry·dead-letter publisher, Learning Core semantic digest inbox·유한 deny marker·JWT 이후 보안 filter와 `ACCOUNT_WITHDRAWN`/gate 장애 fail-closed 오류를 계획했다. consumer→capture→고정 cutover backfill→publisher 순서, workload 인증 선결 조건, rollback·관측·양 서비스 테스트와 Jira 2개 분리를 정의했으며 애플리케이션 코드·Jira는 변경하지 않았다.
- 2026-08-27 Stage 5 `UserWithdrawn` event와 downstream deny marker의 다음 작업 범위를 설명 목적으로 확인했다. Identity는 탈퇴 Transaction과 함께 최소 식별·버전·userId·withdrawnAt을 가진 outbox를 생성하고 at-least-once로 전달하며, Learning Core는 consumer를 먼저 배포해 eventId inbox와 userId deny marker를 로컬 Transaction으로 저장한다. 보호 요청은 기존 JWKS 검증 뒤 marker를 확인하고, marker는 최대 Access Token 수명과 clock skew 안전 여유 뒤 TTL 정리하며 inbox는 replay 기간보다 길게 별도 보존한다. 매 요청 Identity introspection, Token 원문 blacklist, 시험·결과 삭제는 이 단계의 방식이나 범위가 아니다. 코드·계획서·Jira는 변경하지 않았다.
- 2026-08-27 PR #33이 merge commit `7fc92c6`으로 Jira `TMI-108` 구현을 `develop`과 `origin/develop`에 반영한 것을 확인했다. 사용자 요청에 따라 완료 transition ID `41`만 적용했고 후속 조회에서 status ID `10003`과 Resolution ID `10000`이 모두 `완료`임을 확인했다. Jira 댓글과 다른 필드는 변경하지 않았으며 애플리케이션 코드·테스트도 추가 변경하지 않았다.
- 2026-08-27 Jira `TMI-108` Stage 4의 Identity 서버 범위를 구현했다. `POST /api/v1/auth/reissue`는 실제 hash 일치 Session의 reason이 `ACCOUNT_WITHDRAWN`이면 기존 `BaseResponse` shape의 `401 ACCOUNT_WITHDRAWN`과 `탈퇴 처리된 계정입니다.`를 반환한다. active Session이지만 User가 이미 `WITHDRAWN`인 교차 관찰도 같은 오류로 fail-closed 처리한다. `ROTATED` reuse detection을 우선하고 unknown·일반 revoked·expired·User 없음·SUSPENDED 동작을 유지하며 오류 경로에서 User 조회·Session save·새 Token 발급이 일어나지 않음을 테스트했다. OpenAPI 401 code 목록과 탈퇴 lifecycle 기대값을 갱신했고 전체 103개 suite·578개 테스트 및 `git diff --check`가 성공했다. 모바일 코드는 추가하지 않았고 Jira 댓글·상태, Git commit·push도 변경하지 않았다.
- 2026-08-26 사용자 승인에 따라 Jira `TMI-108` `[Identity] 탈퇴 Session 전용 오류 및 모바일 logout·안내 UX 계약`을 TMI 프로젝트의 `작업`·High로 생성했다. 계획서의 `401 ACCOUNT_WITHDRAWN`, 실제 탈퇴 Session 한정 노출, reuse detection 회귀, race fallback, 모바일 terminal handler·선배포, staging E2E와 Stage 5 제외 범위를 본문에 저장했다. 재조회 결과 상태 `해야 할 일`, Resolution 없음, 담당자·라벨·댓글 없음을 확인했으며 별도 상태 전환은 수행하지 않았다.
- 2026-08-26 `docs/contracts/withdrawal-session-mobile-ux-stage-4-plan.md`를 작성하고 고정 구현 순서 4단계에서 연결했다. `401 ACCOUNT_WITHDRAWN` 오류를 실제 탈퇴 Session에만 반환하고 ROTATED reuse detection·일반 revoked·unknown·expired 계약을 유지한다. active Session과 WITHDRAWN User의 race fallback, 모바일의 멱등 terminal handler·Firebase signOut 실패와 무관한 Identity Token 삭제, multi-device·앱 재시작 테스트와 모바일 선배포 순서를 정의했다. Stage 5 전 Access Token downstream 즉시 차단은 보장하지 않으며 코드·Jira는 변경하지 않았다.
- 2026-08-26 TMI-107 완료 뒤 다음 고정 순서인 Stage 4를 현재 코드와 대조해 설명했다. `TokenReissueService`는 `ROTATED`만 재사용 전용 오류로 구분하고 `ACCOUNT_WITHDRAWN`을 포함한 나머지 revoked Session은 모두 `INVALID_REFRESH_TOKEN`으로 반환한다. Stage 4는 실제로 조회된 Session의 revocation reason이 `ACCOUNT_WITHDRAWN`이거나 race-safe fallback에서 User가 `WITHDRAWN`이면 전용 401 오류로 분류하고, 모바일이 Firebase signOut·자체 Access/Refresh Token과 계정 상태 삭제·탈퇴 안내를 수행하도록 계약한다. 이미 발급된 Access Token의 downstream 차단은 Stage 5 범위로 유지하며 코드·Jira는 변경하지 않았다.
- 2026-08-26 PR #32가 merge commit `1fa1141`로 Jira `TMI-107` 구현을 `develop`과 `origin/develop`에 반영한 것을 확인했다. 사용자 요청에 따라 완료 transition ID `41`만 적용했고 후속 조회에서 status ID `10003`과 Resolution ID `10000`이 모두 `완료`임을 확인했다. Jira 댓글과 다른 필드는 변경하지 않았다.
- 2026-08-26 사용자가 Billing workload 인증 C3-D를 최종 승인했다. Identity는 기존 사용자 RS256/JWKS issuer를 workload로 확장하지 않고, 별도 Identity ECS task role의 임시 credential로 Billing Lattice endpoint 요청을 SigV4 서명한다. 기존 Load Balancer 사용자 API는 유지하고 phone eligibility publisher의 Bearer credential port는 SigV4 request signer 경계로 후속 변경해야 한다. 코드와 TMI-107 범위는 변경하지 않았다.
- 2026-08-26 Billing workload 인증을 기존 서비스와 맞추기 위해 Identity·Learning Core 구현을 대조했다. 실제 공통 구현은 Identity RS256 사용자 JWT 발급과 Learning Core의 Identity JWKS 로컬 검증이며, Identity downstream publisher의 workload credential은 port만 있고 production provider가 없어 비활성이다. 동일 메커니즘을 적용하려면 사용자 token 재사용이 아니라 workload 전용 audience·service subject·scope·5분 TTL과 client-credentials를 새로 구현해야 한다. 코드와 TMI-107 범위는 변경하지 않았다.
- 2026-08-26 사용자가 실제 배포 플랫폼이 AWS ECS라고 후속 확인해 Billing C3의 `배포 플랫폼 발급 JWT` 선택은 재검토 상태가 됐다. ECS task role은 OIDC JWT·JWKS를 자동 제공하지 않으므로 VPC Lattice/API Gateway SigV4·AWS_IAM 또는 별도 issuer가 필요하다. ADR-002 phone eligibility publisher의 `WorkloadIdentityCredentialProvider`도 동일 인프라 결정의 영향을 받으며 코드와 TMI-107 범위는 변경하지 않았다.
- 2026-08-26 Billing 무료시험 계약 설명을 위해 workload credential 경계를 읽기 전용 점검했다. 현재 사용자 Access Token issuer는 존재하지만 Learning Core용 client-credentials/workload issuer 구현은 없으며, ADR-002 phone eligibility push는 배포 플랫폼 발급 5분 이하 service identity JWT를 요구하고 실제 `WorkloadIdentityCredentialProvider` 구현은 배포 adapter 과제로 남아 있다. 코드와 기존 계약은 변경하지 않았다.
- 2026-08-26 Jira `TMI-107` Stage 3을 로컬 구현했다. `IDENTITY_RELEASE_PENDING` lifecycle을 version-fenced Transaction에서 재검증하고, external deletion 증적·WITHDRAWN User·exact Firebase owner·phone identity/alias 정합성을 통과한 경우 active eligibility binding REVOKED revision/outbox, FirebaseIdentity·모든 SocialIdentity hard delete, PhoneIdentity·alias soft release와 lifecycle `CLEANED`를 한 Mongo Transaction으로 commit한다. partial·owner mismatch는 안전 code로 `RECONCILIATION_REQUIRED` 처리하고 worker·batch scheduler는 기본 비활성이다. 중앙 `WithdrawalEnrollmentGate`를 Firebase exchange, direct signup precheck/unique conflict, Guest ownership·merge, auth methods sync와 phone owner conflict 경계에 연결했다. 전체 103개 suite·572개 테스트와 `git diff --check`가 성공했으며 commit·push·Jira 댓글·상태 전환은 수행하지 않았다.
- 2026-08-26 종료 훅 기준으로 사용자 승인에 따른 Jira `TMI-107` 생성과 재조회 검증, Stage 3 계획서 Jira 키 연결 결과를 지정 turn marker로 WORKLOG에 동기화했다. 댓글·상태 전환과 애플리케이션 코드 변경은 수행하지 않았다.
- 2026-08-26 사용자 승인에 따라 공식 Atlassian Rovo로 Jira `TMI-107` `[Identity] 탈퇴 Identity release 및 CLEANED 재가입 gate 구축`을 TMI 프로젝트의 `작업`·`High`로 생성했다. 승인한 목표, FirebaseIdentity·SocialIdentity hard delete, PhoneIdentity·alias RELEASED, 단일 Mongo Transaction CLEANED, 중앙 gate, rollback·멱등·reconciliation·staging 완료 조건과 Stage 4 이후 제외 범위를 저장했다. 재조회 결과 상태 `해야 할 일`, 담당자 없음, 라벨 없음과 본문을 확인했으며 댓글·상태 전환·다른 이슈 변경은 수행하지 않았다. Stage 3 계획서에 Jira 키를 연결했고 애플리케이션 코드는 변경하지 않았다.
- 2026-08-26 종료 훅 기준으로 Stage 3 Jira 생성안과 Atlassian Rovo 403 연결 실패, 실제 Jira mutation 0건 및 사용자 승인 대기 상태를 지정 turn marker로 WORKLOG에 동기화했다. 애플리케이션 코드·Jira는 변경하지 않았다.
- 2026-08-26 Stage 3 identity release와 `CLEANED` 재가입 gate의 Jira 생성 요청에 따라 TMI 프로젝트 작업 생성안을 준비했다. 제목은 `[Identity] 탈퇴 Identity release 및 CLEANED 재가입 gate 구축`, 유형은 `작업`, 우선순위는 `High`, 담당자·라벨 없음으로 제안하며, 현재 계획의 FirebaseIdentity·SocialIdentity exact hard delete와 PhoneIdentity·alias RELEASED, 단일 Mongo Transaction, 중앙 withdrawal gate, 멱등·rollback·reconciliation·staging 완료 조건을 본문에 포함한다. 저장소 규칙에 따라 실제 생성 전 사용자 승인을 기다린다. 공식 Atlassian Rovo로 `TMI-104` 읽기 확인을 시도했으나 `app is not installed on this instance` 403이 반환됐고 Jira 생성·수정·댓글·상태 전환은 수행하지 않았다.
- 2026-08-26 종료 훅 기준으로 변경된 12단계 순서의 실제 다음 작업이 Stage 3임을 설명한 결과와 Stage 3 계획서의 후속 단계 번호를 Stage 6·7로 바로잡은 결과를 지정 turn marker로 WORKLOG에 동기화했다. 애플리케이션 코드·Jira는 변경하지 않았다.
- 2026-08-26 두 후속 작업 추가 뒤의 실제 다음 순서를 재확인했다. 목록은 12단계로 확장됐지만 `TMI-104` Stage 2 다음 작업은 여전히 Stage 3 FirebaseIdentity·SocialIdentity·PhoneIdentity release와 `CLEANED` 재가입 gate다. Stage 3은 외부 Firebase 삭제 증적, WITHDRAWN User와 exact owner를 검증한 뒤 FirebaseIdentity·모든 SocialIdentity 제거, PhoneIdentity·alias RELEASED, eligibility REVOKED 보장과 CLEANED 전환을 한 Mongo Transaction으로 commit하며 실패 시 전체 rollback한다. CLEANED 전 같은 SNS·phone은 `WITHDRAWAL_CLEANUP_PENDING`, 이후에는 기존 User 복구 없이 새 UUID 재가입이다. 새 4·5단계 삽입으로 Stage 3 계획서의 기존 후속 `Stage 4·5` 참조가 낡아져 가입 중단 cleanup·Billing consumer의 새 번호 `Stage 6·7`로 정정했다. 애플리케이션 코드·Jira는 변경하지 않았다.
- 2026-08-26 사용자 결정에 따라 고정 Firebase 인증 후속 구현 순서에 4단계 `탈퇴 Session 전용 오류와 모바일 logout·안내 UX 계약`, 5단계 ``UserWithdrawn` event와 downstream Access Token deny marker`를 추가하고 기존 4~10단계를 6~12단계로 이동했다. 4단계는 `ACCOUNT_WITHDRAWN` 전용 외부 오류, 모바일 Firebase signOut, 자체 Access·Refresh Token 전체 삭제와 탈퇴 안내를 계약한다. 5단계는 Learning Core 멱등 consumer·로컬 userId deny gate를 Identity publisher보다 먼저 배포하고 매 요청 Identity introspection 없이 JWKS 검증 뒤 로컬 marker를 확인한다. marker는 `withdrawnAt + 시스템 최대 Access Token 수명 + verifier clock skew 안전 여유`까지 유지하고 TTL 정리하며, event inbox는 outbox 재전달·dead-letter·수동 replay 기간보다 길게 별도 TTL 보존한다. 1~5단계와 양 서비스·모바일 staging E2E 전에는 withdrawal production flag를 활성화하지 않는 순서를 고정했다. 애플리케이션 코드·Jira는 변경하지 않았다.
- 2026-08-26 Learning Core의 withdrawal deny marker와 event inbox가 계속 누적되는지 설명했다. 영구 저장하면 누적되는 것이 맞지만 deny marker의 목적은 탈퇴 전에 발급된 Access Token의 잔여 수명 동안만 old userId를 막는 것이므로 영구 보존하지 않는다. Learning Core의 서비스 공용 영속 저장소에 `blockedUntil/expireAt = withdrawnAt + 시스템이 허용하는 최대 Access Token 수명 + verifier clock skew 안전 여유`를 기록하고 TTL index로 자동 정리하며, pod별 메모리 목록을 권위 저장소로 사용하지 않는다. 현재 기본 TTL이 30분이더라도 실제 운영 최대 TTL과 clock skew 설정을 계약값으로 사용해야 한다. eventId inbox는 deny marker와 목적이 달라 source outbox의 최대 재전달·dead-letter 수동 replay 기간보다 길게 보존한 뒤 별도 TTL로 정리한다. TTL 삭제가 지연돼도 authorization은 `blockedUntil`을 직접 검사한다. 재가입은 새 UUID라 기존 marker와 무관하며 코드·계획서·Jira는 변경하지 않았다.
- 2026-08-26 탈퇴한 Access Token의 downstream 차단에서 `UserWithdrawn` event와 Learning Core deny marker가 수행하는 일을 설명했다. Identity의 모든 RefreshSession 폐기는 새 Access Token 재발급만 막고 탈퇴 전에 발급된 RS256 Access Token은 기본 최대 30분 동안 서명·만료 검증을 통과할 수 있다. 즉시 차단 정책을 선택하면 Identity는 withdrawal 내부 commit과 함께 outbox에 최소 eventId·schemaVersion·userId·withdrawnAt을 기록해 at-least-once 전달하고, Learning Core는 eventId inbox로 중복을 멱등 처리하면서 해당 userId의 로컬 deny marker를 저장한다. 이후 보호 요청은 JWT 로컬 검증 뒤 `sub`의 로컬 deny 여부를 확인해 학습 데이터 처리 전에 전용 탈퇴 오류로 거절하며 Identity에 매 요청 네트워크 조회하지 않는다. 이는 사용자별 차단이며 토큰 원문 저장·토큰별 blacklist·학습 데이터 삭제가 아니다. event 전달 지연만큼의 짧은 공백과 Learning Core 외 다른 JWT 소비 서비스별 consumer 필요성은 남는다. 코드·계획서·Jira는 변경하지 않았다.
- 2026-08-26 다중 SNS 계정 탈퇴 후 다른 기기 안내와 Access Token 즉시 차단 계약이 후속 구현 범위에 정식 포함됐는지 재확인했다. 이 문제는 2026-08-25 작업 기록에서 이미 논의됐지만 현재 10단계 고정 구현 체크리스트에는 독립 작업으로 등록돼 있지 않다. 서버는 탈퇴 Transaction에서 모든 RefreshSession에 `ACCOUNT_WITHDRAWN`을 저장하지만 `TokenReissueService`는 `ROTATED` 외 모든 revoked Session을 `INVALID_REFRESH_TOKEN`으로 반환하고 lifecycle 테스트도 이 일반 오류를 현재 계약으로 검증한다. 따라서 전용 탈퇴 오류, 앱의 Firebase signOut·모든 로컬 Token 삭제·탈퇴 안내 UX는 아직 미구현·미확정이다. stateless Access Token의 최대 30분 downstream 유효 문제도 Stage 1 제외 범위이며 `UserWithdrawn` event·Learning Core deny marker는 별도 이슈 필요성만 기록돼 있고 구현 순서와 계약서가 없다. 코드·계획서·Jira는 변경하지 않았다.
- 2026-08-26 여러 SNS가 하나의 canonical User에 연결된 상태의 탈퇴가 후속 구현 범위에 포함됐는지 Stage 1~3 계획서와 고정 구현 순서에서 재확인했다. Stage 1은 탈퇴 요청에 사용된 Provider와 무관하게 User 전체를 `WITHDRAWN`으로 만들고 모든 내부 RefreshSession을 폐기한다. Stage 2는 해당 Firebase User를 disable·refresh revoke·delete해 같은 Firebase UID에 연결된 Provider 전체의 재인증 기반을 제거한다. Stage 3은 `findAllByUserId`로 SocialIdentity 0개·1개·여러 개를 모두 수집해 FirebaseIdentity와 함께 한 Transaction에서 제거하고 Phone identity를 release한 뒤에만 `CLEANED`로 전환한다. cleanup 중에는 어느 linked SNS로 접근해도 중앙 gate에서 pending으로 차단하고, 특정 SNS 하나만 제거하는 기능은 탈퇴가 아니라 Stage 8 Provider unlink로 분리한다. 현재 Stage 3 계획에는 다중 SocialIdentity 삭제 테스트가 있으나 linked Provider 각각의 cleanup 전·후 로그인 시도 매트릭스는 구현 시 명시적으로 보강할 필요가 있다. 코드·계획서·Jira는 변경하지 않았다.
- 2026-08-25 현재 단일·전체 로그아웃의 Firebase/SNS 처리 여부를 코드와 테스트로 재확인했다. `POST /logout`은 요청 Refresh Token의 Identity RefreshSession 한 건만 `LOGOUT` 폐기하고 Firebase client signOut·Admin revoke를 호출하지 않는다. 현재 기기의 Firebase signOut과 로컬 Token 삭제는 모바일 client 책임이 맞다. `POST /logout-all`도 userId의 Identity RefreshSession만 모두 `LOGOUT_ALL` 폐기하며 Firebase refresh revoke는 구현되지 않아, Firebase SDK 세션이 남으면 새 ID Token으로 exchange해 Identity Session을 다시 만들 수 있다. 이 공백은 고정 순서 Stage 6 `logout-all Firebase refresh revoke` 범위이며 코드는 변경하지 않았다.
- 2026-08-25 여러 SNS가 같은 canonical User에 연결된 상태의 탈퇴·로그아웃 의미를 점검했다. 회원탈퇴 Transaction은 로그인 Provider와 무관하게 해당 userId의 모든 RefreshSession을 `ACCOUNT_WITHDRAWN`으로 폐기하고 Stage 2 Firebase User 삭제가 같은 Firebase UID에 연결된 모든 Provider의 향후 인증을 막으므로 계정 전체 탈퇴가 맞다. 다른 기기는 Refresh 재발급 또는 User 상태 확인 시 탈퇴를 인지하지만, 자체 stateless Access Token은 기본 TTL 30분 동안 downstream에서 유효할 수 있어 즉시 전역 차단이 필요하면 별도 withdrawal deny event/token-version 정책이 필요하다. 현재 세션 로그아웃은 RefreshSession 한 건을 폐기하며 SNS별 로그아웃이 아니고, 특정 SNS만 제거하는 기능은 후속 Stage 8 Provider unlink로 구분한다. 코드와 계획서는 변경하지 않았다.
- 2026-08-25 Stage 3 계획의 중앙 `WithdrawalEnrollmentGate` 의미를 현재 인증 경로별로 설명했다. 이는 HTTP filter나 신규 공개 API가 아니라 FirebaseIdentity·SocialIdentity·phone alias owner를 발견한 application 경계에서 owner User와 withdrawal lifecycle을 공통 분류하는 내부 서비스다. ACTIVE owner는 기존 login·merge·conflict 계약을 유지하고, WITHDRAWN owner가 CLEANED 전이면 exchange·direct signup·Guest prepare/upgrade·Guest merge·auth methods sync 모두 `WITHDRAWAL_CLEANUP_PENDING`으로 수렴하며, CLEANED인데 mapping이 남으면 불변식 위반으로 fail-closed 처리한다. 계획서와 코드는 변경하지 않았다.
- 2026-08-25 Stage 3 계획의 `exact FirebaseIdentity 삭제`와 `User 소유 SocialIdentity 전체 삭제`가 실제 MongoDB mapping document hard delete를 의미함을 재확인했다. User tombstone, 외부 SNS 원본 계정, Learning Core 데이터는 삭제 대상이 아니며 PhoneIdentity·alias는 RELEASED로 보존한다. hard delete는 외부 Firebase 삭제 증적과 exact owner 검증 뒤 단일 Transaction에서만 수행하는 현재 계획이며, mapping 이력을 보존하려면 status·releasedAt과 ACTIVE partial unique index, active-only Repository를 도입하는 별도 soft-release 설계 변경이 필요하다. 계획서와 코드는 이번 설명에서 변경하지 않았다.
- 2026-08-25 `docs/contracts/firebase-withdrawal-identity-release-stage-3-plan.md`를 작성하고 고정 구현 순서 3단계에서 연결했다. 외부 삭제 증적·WITHDRAWN owner·version 검증, FirebaseIdentity·SocialIdentity hard delete, PhoneIdentity·alias RELEASED, eligibility REVOKED 보장과 lifecycle CLEANED를 한 Mongo Transaction으로 commit하는 계약을 정의했다. 별도 in-progress lease 없이 Transaction rollback·write conflict·version CAS로 동시성을 처리하고, cleanup 중 credential owner를 중앙 gate에서 `WITHDRAWAL_CLEANUP_PENDING`으로 통합하며 CLEANED 뒤에만 새 UUID 재가입을 허용한다. Jira와 애플리케이션 코드는 변경하지 않았다.
- 2026-08-25 다음 작업인 Stage 3 identity release와 `CLEANED` 재가입 gate의 현재 코드 공백과 구현 경계를 정리했다. Stage 2 terminal `IDENTITY_RELEASE_PENDING`에서 외부 삭제 증적과 WITHDRAWN owner를 검증한 뒤 한 Mongo Transaction으로 FirebaseIdentity·SocialIdentity 점유를 제거하고 PhoneIdentity·PhoneFingerprintAlias를 RELEASED 처리하며 lifecycle을 `CLEANED`로 전환해야 한다. 이 전까지 동일 SNS·전화번호 enrollment는 `WITHDRAWAL_CLEANUP_PENDING`으로 일관되게 막고, CLEANED commit 뒤에만 새 UUID User 가입을 허용한다. User tombstone과 과거 데이터·Billing TrialClaim은 복구하거나 삭제하지 않는다. 코드와 Jira는 변경하지 않았다.
- 2026-08-25 종료 훅 기준으로 Atlassian Rovo 재연결과 Jira `TMI-104` 완료 전환 결과를 WORKLOG에 지정 turn marker로 동기화했다. Jira 추가 변경과 애플리케이션 코드 변경은 수행하지 않았다.
- 2026-08-25 Atlassian Rovo 재연결 후 Jira `TMI-104`의 현재 상태 `해야 할 일`과 사용 가능한 `완료` transition ID `41`을 재조회했다. 사용자가 앞서 승인한 범위대로 상태만 `완료`로 변경했으며, 후속 조회에서 상태와 Resolution이 모두 `완료`임을 확인했다. 댓글·담당자·우선순위·라벨·설명은 변경하지 않았다.
- 2026-08-25 종료 훅 기준으로 Atlassian Rovo 설치·재연결 공식 안내 결과를 WORKLOG에 별도 동기화했다. Codex Plugins/Connectors에서 connector를 재인증하고 새 작업에서 검증해야 하며 Jira `TMI-104` 상태 변경은 수행되지 않았다.
- 2026-08-25 Atlassian Rovo가 설치됐는데도 `app is not installed on this instance` 403이 발생하는 원인과 공식 재연결 경로를 확인했다. OpenAI 공식 플러그인 문서는 Codex Desktop의 Plugins 탭에서 설치·connector 인증 후 새 작업을 시작하도록 안내한다. Atlassian 공식 Rovo MCP 문서는 Codex Desktop의 Plugins 또는 Connectors에서 Atlassian Rovo를 설치하고 인증하며, access denied 시 연결 흐름 재실행·재인증·scope 승인·Jira product 권한을 확인하도록 안내한다. 현재 Jira 변경은 없다.
- 2026-08-25 Jira `TMI-104` 종료 여부 질문에 공식 Atlassian 연결로 현재 상태를 다시 조회했으나 연결 앱 미설치 403이 동일하게 반환됐다. 완료 transition은 전송된 적이 없으므로 Codex 기준으로는 아직 닫히지 않았으며 Jira 댓글·상태·다른 필드 변경도 없다.
- 2026-08-25 로컬 `develop`과 `origin/develop`이 같은 merge commit `ca18fdc`를 가리키고 PR #31이 TMI-104 구현 commit `7f1a113`을 포함하는 것을 확인했다. Jira `TMI-104` 완료 요청에 따라 공식 Atlassian 연결로 읽기 전용 조회를 시도했으나 연결 앱 미설치 403이 반복돼 현재 상태와 완료 transition을 확인하거나 적용하지 못했다. Jira 댓글·담당자·우선순위·라벨·설명과 상태는 변경하지 않았다.
- 2026-08-25 Jira `TMI-104` Stage 2 로컬 구현을 완료했다. `MongoOperations.findAndModify` 기반 due/expired lifecycle atomic claim, claim별 lease token·version fencing, lease renewal, retry/reconciliation/completed CAS와 local handoff를 추가했다. User·FirebaseIdentity preflight mismatch는 Firebase 호출 없이 reconciliation으로 보내고 LOCAL/GUEST null target은 외부 호출 없이 완료한다. cleanup 전용 Firebase App은 독립 connect/read timeout을 사용하며 inspect→disable→refresh revoke→Provider 의무→delete→presence 확인을 수행한다. NOT_FOUND는 멱등 성공, delete 결과 불명은 presence로 확인하고 retryable 오류는 jitter backoff, max attempt와 비재시도 오류는 reconciliation으로 처리한다. Apple provider는 안전한 revoke material 경로 전까지 `PROVIDER_OBLIGATION_REQUIRED`로 차단한다. worker·scheduler는 기본 비활성이고 safe metric·구조화 로그에 식별자를 포함하지 않는다. 신규 36개를 포함한 `./gradlew clean test` 전체 541개가 failure·error·skip 없이 성공했다. Atlassian 읽기 전용 재조회는 연결 앱 미설치 403으로 실패했으며 Jira 변경은 없다.
- 2026-08-25 `TMI-104` 계획의 target preflight guard와 Firebase account inspect 책임을 구분했다. preflight는 MongoDB의 lifecycle·User·FirebaseIdentity가 동일한 userId·project·UID를 가리키고 User가 `WITHDRAWN`이며 worker lease·version이 유효한지 확인하는 내부 안전장치다. inspect는 이 검증을 통과한 project·UID로 Firebase `getUser`를 호출해 외부 User의 존재·disabled 상태와 Provider 의무 판정용 provider 집합을 확인하는 외부 read다. email·phone·provider subject로 삭제 대상을 추정하지 않으며 mismatch는 Firebase 호출 없이 reconciliation으로 보낸다. 코드와 Jira는 변경하지 않았다.
- 2026-08-25 사용자 정정에 따라 `TMI-104` 계획은 Jira 본문 추가가 아니라 저장소 구현 계획서 작성 요청이었음을 명확히 했다. 기존에 생성한 `docs/contracts/firebase-withdrawal-external-cleanup-stage-2-plan.md`와 구현 순서 문서의 2단계 링크를 재확인했으며 Jira에는 조회·수정·댓글·상태 전환을 수행하지 않았다.
- 2026-08-25 종료 훅 기준으로 `TMI-104` Stage 2 구현 계획서 작성 결과를 WORKLOG에 별도 동기화했다. 계획서·구현 순서 링크와 Apple revoke activation gate를 유지하며 코드와 Jira는 변경하지 않았다.
- 2026-08-25 `docs/contracts/firebase-withdrawal-external-cleanup-stage-2-plan.md`에 Jira `TMI-104` 구현 계획을 작성하고 10단계 구현 순서 문서의 2단계에서 연결했다. claim별 lease token과 version 기반 fencing, expired lease recovery, User·FirebaseIdentity target guard, Firebase disable→refresh revoke→Provider 의무→delete, timeout 뒤 presence 확인, safe failure code·backoff·reconciliation, completed→identity release handoff, properties·scheduler·metrics·테스트·staging activation을 정의했다. Apple revoke material 경로가 확정되기 전 Apple 또는 전체 Firebase withdrawal production flag를 열지 않는 gate를 포함했다. 코드와 Jira는 변경하지 않았다.
- 2026-08-25 탈퇴 Transaction의 phone eligibility binding 해제 이벤트 의미를 확인했다. 이는 Firebase 전화번호 unlink나 PhoneIdentity release, 무료시험 사용 기록 삭제가 아니라, 이미 VERIFIED로 전달된 `userId + consumerScopeId`의 현재 혜택 자격 연결을 새 revision의 `REVOKED` outbox로 무효화하는 통지다. active binding이 있을 때만 User 탈퇴와 같은 Transaction에서 생성하며 Billing/Entitlement는 revision 순서로 stale event를 거절한다. TrialClaim은 유지돼 재가입 후 혜택이 복구되지 않는다. 코드와 Jira는 변경하지 않았다.
- 2026-08-25 종료 훅 기준으로 `TMI-104` worker 흐름 설명 결과를 WORKLOG에 별도 동기화했다. Stage 2는 외부 Firebase cleanup까지만 담당하고 Stage 3 release 전에는 완전 탈퇴나 재가입을 허용하지 않는다는 범위를 유지했다. 코드와 Jira는 변경하지 않았다.
- 2026-08-25 다음 작업 `TMI-104`의 역할을 사용자 관점과 worker 흐름으로 재확인했다. Stage 1이 내부 User를 `WITHDRAWN`으로 확정하고 lifecycle을 만든 뒤, Stage 2는 due lifecycle을 atomic lease로 claim해 Firebase disable→refresh revoke→Provider 의무→User delete를 Mongo Transaction 밖에서 멱등 실행한다. 성공하면 Stage 3 identity release가 이어받도록 `IDENTITY_RELEASE_PENDING`으로 넘기며, 재시도 가능 오류는 backoff, 불명확·반복 실패는 reconciliation으로 분리한다. 코드와 Jira는 변경하지 않았다.
- 2026-08-25 종료 훅 기준으로 `TMI-103` 완료·Resolution 완료와 Stage 2 Jira `TMI-104` 생성 결과를 WORKLOG에 별도 동기화했다. Jira 추가 변경과 애플리케이션 코드 변경은 수행하지 않았다.
- 2026-08-25 사용자 승인에 따라 Atlassian Rovo 공식 연결을 설치·사용해 Jira `TMI-103`의 현재 상태와 전환을 재조회하고 transition ID `41`만 적용했다. 후속 조회에서 상태와 Resolution 모두 `완료`임을 확인했다. 이어 Stage 2 Jira `TMI-104` `[Identity] Firebase 탈퇴 외부 cleanup worker 구축`을 `작업`·`High`·`해야 할 일`로 생성하고 승인한 설명·완료 조건·제외 범위, 담당자 없음·빈 라벨을 확인했다. 댓글과 다른 필드는 변경하지 않았다.
- 2026-08-25 구현 순서 2단계 Firebase disable·refresh revoke·delete worker의 Jira 생성안을 준비했다. `EXTERNAL_CLEANUP_PENDING` claim·lease, disable→refresh revoke→Provider 의무→delete, retry/backoff·reconciliation, external completion fencing, 기본 비활성 worker와 staging gate를 범위로 정했고 identity release·`CLEANED`는 3단계로 제외했다. Atlassian 공식 도구가 없어 Jira 생성은 수행하지 않았으며 사용자에게 정확한 payload 승인을 요청한다.
- 2026-08-25 Jira `TMI-103` 종료 요청을 확인했다. 현재 turn에는 Atlassian 공식 도구가 노출되지 않아 현재 상태·전환 ID 재조회와 실제 상태 변경을 수행하지 않았다. 변경안은 상태만 `완료`로 전환하고 댓글·담당자·우선순위·라벨 등 다른 필드는 유지하는 것으로 제한했으며 사용자 최종 승인과 도구 가용성을 기다린다.
- 2026-08-25 탈퇴 구현 범위를 재확인했다. 완전 탈퇴는 1단계 내부 `WITHDRAWN`·lifecycle 생성, 2단계 Firebase disable·refresh revoke·delete worker, 3단계 Firebase/Social/Phone release·`CLEANED` 전환과 재가입 gate의 묶음이다. 현재 코드 완료 범위는 1단계이고 2·3단계는 고정 구현 순서에 남아 있어, 아직 Firebase 외부 User 삭제와 identity 점유 해제까지 끝난 상태는 아니다. 코드와 Jira는 변경하지 않았다.
- 2026-08-25 Jira `TMI-103` Stage 1을 구현했다. 기존 withdraw 요청에 write-only `firebaseIdToken`과 응답 `cleanupStatus`를 호환 추가하고, `WITHDRAWAL` recent-auth·Firebase/Social owner 검증, `UserWithdrawalLifecycle` 상태·인덱스·Repository, User tombstone·모든 RefreshSession revoke·active eligibility revoke·lifecycle insert의 Mongo Transaction을 연결했다. cleanup 중 Firebase exchange는 `WITHDRAWAL_CLEANUP_PENDING`으로 차단하며 `FIREBASE_WITHDRAWAL_ENABLED=false`가 기본이라 Stage 2 worker 전 production 활성화를 막는다. 외부 delete와 identity release는 범위에서 제외했고 `./gradlew clean test` 전체 505개 테스트가 성공했다. Jira 변경은 없다.
- 2026-08-24 종료 훅 기준으로 Jira `TMI-103` 생성 결과와 승인 여부를 WORKLOG에 별도 동기화했다. 이슈는 `작업`·`High`·`해야 할 일`이며 담당자·라벨·댓글·상태 전환은 없다. 애플리케이션 코드는 변경하지 않았다.
- 2026-08-24 사용자 승인에 따라 Jira `TMI-103` `[Identity] Firebase/SNS 탈퇴 재인증 및 withdrawal lifecycle 구축`을 TMI 프로젝트의 `작업`·`High`로 생성했다. 재조회 결과 승인된 목표·범위·완료 조건·제외 범위와 참고 문서가 저장됐고 초기 상태는 `해야 할 일`, 담당자 없음, 라벨 없음이다. 댓글과 상태 전환은 수행하지 않았다.
- 2026-08-24 결제 구현을 후속으로 미루고 SNS 로그인, verified-phone당 무료시험 1회와 10초 챌린지를 우선하는 개정 계획을 확인했다. Identity에는 Firebase exchange/signup·Guest prepare/upgrade/merge·auth-method sync와 SocialIdentity·PhoneIdentity·eligibility publisher가 이미 있으므로 신규 SNS endpoint보다 탈퇴 lifecycle 1~3, 실제 Google/Apple/Phone 모바일 연동과 staging E2E가 우선이다. 무료 TrialClaim은 기존 결정대로 최소 Billing/Entitlement consumer가 소유하고 10초 챌린지는 Learning Core가 소유한다. Jira 키는 없다.
- 2026-08-24 `docs/contracts/firebase-withdrawal-lifecycle-stage-1-plan.md`에 1단계 Firebase/SNS 탈퇴 재인증과 withdrawal lifecycle 구현 계획을 작성하고 10단계 순서 문서에서 연결했다. 기존 withdraw API 호환 확장, 실제 credential 소유 기반 GUEST/LOCAL/Firebase 분기, WITHDRAWAL recent-auth와 owner 검증, User·Session·eligibility·lifecycle 단일 Transaction, EXTERNAL_CLEANUP_PENDING부터 CLEANED까지의 후속 상태 계약, 멱등·동시성·응답 유실·오류·테스트·배포 gate를 정의했다. 1단계는 외부 Firebase mutation과 identity release를 수행하지 않으며 2단계 worker 전 단독 production 활성화를 금지한다. 코드와 Jira는 변경하지 않았다.
- 2026-08-24 `docs/contracts/firebase-auth-follow-up-implementation-order.md`에 Firebase 인증 후속 작업을 10단계 고정 체크리스트로 저장했다. 순서는 탈퇴 재인증/lifecycle, Firebase revoke/delete, 내부 release와 CLEANED gate, 가입 중단 cleanup, Billing 최소 consumer, logout-all revoke, Refresh rotation 복구, provider/phone lifecycle, Guest response-loss 복구, ACTIVE 회원 rebind다. 앞 단계 완료 전 후속 production 기능을 활성화하지 않고 consumer 선배포와 staging E2E를 완료 기준으로 적용한다. 코드와 Jira는 변경하지 않았다.
- 2026-08-24 인증 전체 흐름의 정상 사용자 오류 가능성을 감사했다. 핵심 출시 차단은 federated MEMBER 탈퇴 불가, Firebase revoke/delete·Firebase/Social/Phone release와 CLEANED 재가입 gate 부재, abandoned Firebase enrollment cleanup 부재다. 추가로 logout-all 뒤 Firebase exchange로 세션 재생성 가능, provider unlink·phone replacement 미반영, 새 Firebase UID active-account rebind 부재, Refresh rotation 응답 유실 재시도의 전체 Session 폐기, Guest 생성 응답 유실 복구 불가를 확인했다. Firebase 가입은 eligibility outbox를 항상 생성하므로 Billing consumer 선배포가 실질 activation gate다. 관련 5개 test class는 성공했지만 현재 conflict 동작을 고정할 뿐 이 복구 경로는 검증하지 않는다. 코드와 Jira는 변경하지 않았다.
- 2026-08-24 탈퇴 후 같은 SNS 계정 재사용 동작을 확정했다. Firebase User 삭제는 Google·Kakao·Apple 원본 계정을 삭제하지 않으며 같은 credential 재인증 시 새 Firebase User·UID가 생길 수 있다. cleanup이 CLEANED 전이면 로그인과 enrollment를 차단하고, CLEANED 후에는 기존 로그인이나 계정 복구가 아니라 전화번호 재검증·약관 동의를 거쳐 새 canonical UUID User를 만드는 신규 가입으로 처리한다. 기존 프로필·시험은 연결하지 않고 Billing TrialClaim은 유지해 동일 번호의 무료혜택 중복을 막는다. 코드 변경과 Jira 작업은 없다.
- 2026-08-24 탈퇴·재가입·무료시험 최소 Billing 정책을 통합했다. fresh proof 기반 내부 WITHDRAWN 확정 후 Firebase 외부 User 삭제와 Firebase/Social/Phone 점유 release를 완료한 CLEANED에서만 동일 번호의 새 Firebase User·새 UUID 재가입을 허용한다. 전화번호당 무료시험 1회는 Billing의 최소 Entitlement slice가 eligibility binding, TrialClaim unique와 reserve/confirm/cancel을 소유하고 Identity와 Learning Core는 각각 검증 event 생산과 시험 생성만 담당한다. Billing consumer 선배포 전 producer는 비활성으로 유지하며 실제 결제 기능은 후속이다. 코드 변경과 Jira 작업은 없다.
- 2026-08-24 Billing 최소 Entitlement 선행 결정의 현재 turn Hook 기록을 WORKLOG EOF에 동기화했다. 서비스 책임과 구현 범위의 변경은 없으며 애플리케이션 코드와 Jira는 변경하지 않았다.
- 2026-08-24 Billing 서버에 결제 기능보다 먼저 무료시험용 최소 Entitlement vertical slice를 구현하는 방향을 확정했다. 범위는 eligibility inbox/current binding/high-water, benefit-scoped fingerprint, TrialClaim unique, reserve/confirm/cancel과 reconciliation이며 PG·주문·구독·환불은 제외한다. Identity는 VERIFIED/REVOKED binding 생산, Billing은 중복 claim과 reservation, Learning Core는 reserve 후 시험 생성과 confirm/cancel을 담당한다. 코드 변경과 Jira 작업은 없다.
- 2026-08-24 전화번호당 무료 모의고사 1회 정책의 최소 선행 조건을 정리했다. durable TrialClaim ledger가 없으면 탈퇴·재가입 후 동일 번호의 과거 사용을 판정할 수 없어 정책을 보장할 수 없다. 결제 전체는 미뤄도 되지만 별도 Entitlement bounded context에 verified binding consumer, benefit-scoped fingerprint, TrialClaim unique와 reserve/confirm/cancel을 먼저 구현해야 하며 Identity에는 임시 혜택 필드를 추가하지 않는다. 이 최소 트랙도 미루면 무료시험 출시를 미루거나 전화번호당 1회 보장을 포기해야 한다. 코드 변경과 Jira 작업은 없다.
- 2026-08-24 Entitlement/Billing consumer가 없는 동안의 phone eligibility outbox 정책을 정리했다. ADR-002상 전달 대상은 별도 Entitlement/Billing 서비스이고 Identity는 TrialClaim·UserEntitlement를 소유하지 않는다. consumer가 없는 현재에는 publisher와 VERIFIED/REVOKED outbox 생성을 모두 비활성으로 두고, 탈퇴 시 Identity 소유 PhoneIdentity·aliases release만 수행한다. 단 production에서 VERIFIED가 발행된 이력이 있다면 같은 consumer에 REVOKED 전달은 필수다. 향후 consumer를 먼저 배포한 뒤 producer를 활성화하고 기존 가입자는 필요 시 전화번호를 다시 검증한다. 코드 변경과 Jira 작업은 없다.
- 2026-08-24 Firebase 외부 User 삭제를 전제로 회원 탈퇴 정책을 확정했다. fresh WITHDRAWAL Firebase proof로 소유권을 검증한 뒤 내부 Transaction에서 User WITHDRAWN·모든 Session revoke·lifecycle outbox를 먼저 확정하고, worker가 Firebase disable·refresh revoke·Provider별 철회·Firebase User delete를 멱등 재시도한다. 외부 삭제 성공 후 Firebase/Social unique 점유와 PhoneIdentity/aliases를 release하고 CLEANED가 된 뒤에만 새 Firebase User·새 UUID 재가입을 허용하며 과거 계정 데이터는 자동 연결하지 않는다. 코드 변경과 Jira 작업은 없다.
- 2026-08-24 탈퇴 후 Firebase 외부 User 보존 가능성을 검토했다. 즉시 삭제 대신 disabled와 refresh token revoke로 짧은 유예·재처리 기간 동안 보존할 수는 있지만 email·phone·provider linkage와 unique 점유가 남으므로 cleanup 완료로 간주하지 않는다. 완전 탈퇴와 새 UUID 재가입 정책에서는 terminal cleanup 때 삭제하는 것이 기본안이며, 장기 보존은 법적 근거·기간·재가입/복구 정책과 Provider별 철회 의무가 명시된 경우에만 허용한다. 코드 변경과 Jira 작업은 없다.
- 2026-08-24 탈퇴 cleanup의 물리 삭제 범위와 안전 조건을 검토했다. User는 WITHDRAWN tombstone을 유지하고, Firebase User는 내부 탈퇴와 session revoke 이후 retry 가능한 lifecycle에서 실제 삭제할 수 있다. FirebaseIdentity·SocialIdentity는 현재 status/releasedAt 없이 unconditional unique index를 사용하므로 외부 삭제 전 조기 hard delete는 금지하며, terminal cleanup 뒤 durable audit을 둔 hard delete 또는 ACTIVE/RELEASED·partial unique index 재설계가 필요하다. PhoneIdentity·PhoneFingerprintAlias는 기존 RELEASED 전이를 사용해 soft release한다. 코드 변경과 Jira 작업은 없다.
- 2026-08-24 Firebase·Social·Phone withdrawal cleanup의 구체적 책임을 정리했다. Firebase cleanup은 refresh revoke와 Firebase User delete 및 Provider별 revoke를 retry 가능한 외부 lifecycle로 처리하고, Social cleanup은 old user의 provider mapping을 release/delete해 unique 점유를 해제하며, Phone cleanup은 PhoneIdentity와 active fingerprint aliases를 RELEASED 처리하고 eligibility revoke를 전파한다. User WITHDRAWN tombstone과 별도 benefit abuse ledger는 유지하며 모든 cleanup terminal 성공 뒤에만 재가입을 허용한다. 현재 코드는 User tombstone·RefreshSession 폐기·eligibility revoke outbox까지만 구현됐고 나머지 lifecycle은 미구현이다. Jira 작업은 없다.
- 2026-08-24 탈퇴 후 동일 휴대폰 번호의 재가입·기존 계정 연결 정책을 정리했다. cleanup 완료와 fresh Firebase phone verification 후 같은 번호로 새 UUID 계정을 만드는 것은 허용할 수 있지만 phone은 번호 재할당·공용 사용 가능성이 있어 canonical identity·로그인·자동 merge·WITHDRAWN tombstone 복구 키로 사용하지 않는다. 기존 계정 복구가 필요하면 탈퇴 철회/복구 기간과 원래 Firebase credential의 fresh recent-auth를 요구하는 명시적 reactivation 경로로 분리하고 phone은 보조 proof로만 사용한다. 코드 변경과 Jira 작업은 없다.
- 2026-08-24 Firebase 탈퇴·재가입 구현 순서를 정리했다. 제품 정책을 새 UUID 재가입 또는 기존 canonical User 재활성화 중 먼저 확정하고, 현재 LOCAL 정책과 설계 기준으로는 새 UUID 재가입을 우선안으로 본다. 구현은 fresh Firebase proof 기반 탈퇴 재인증과 내부 tombstone·Session/eligibility revoke·lifecycle outbox, 외부 Firebase revoke/delete worker와 retry/reconciliation, 성공 후 Firebase/Social mapping 및 PhoneIdentity/alias release를 먼저 완결한 뒤 cleanup 완료 계정만 신규 enrollment에 진입시키는 순서다. 코드 변경과 Jira 작업은 없다.
- 2026-08-24 Firebase MEMBER 탈퇴 후 재가입 lifecycle을 정적 진단했다. 현재 일반 `UserWithdrawalService`는 social-only/Firebase MEMBER의 `hasLocalCredential=false`를 `INVALID_WITHDRAWAL_CREDENTIALS`로 거절해 Firebase 재인증 탈퇴 경로가 없고, `UserWithdrawalTransactionService`도 User tombstone·eligibility revoke outbox·RefreshSession 폐기만 수행하며 FirebaseIdentity·SocialIdentity·PhoneIdentity/alias 또는 Firebase 계정을 정리하지 않는다. 따라서 어떤 경로로 User가 WITHDRAWN이 되더라도 exchange는 기존 FirebaseIdentity를 따라 비활성 User 로그인으로 거절하고 신규 enrollment로 분기하지 않으며, mapping/phone unique 점유 때문에 재가입도 막히는 lifecycle 공백이 확인됐다. 코드 변경과 Jira 작업은 없다.
- 2026-08-24 `FirebaseExchangeService`의 로그인 교환·신규 enrollment 분기와 SocialIdentity 소유권 방어를 설명 목적으로 확인했다. `LOGIN_EXCHANGE` Firebase proof의 project·UID에 기존 FirebaseIdentity가 있으면 ACTIVE MEMBER와 linked social owner 일치를 확인한 뒤 Identity Access/Refresh를 발급하고, mapping이 없으면 기존 SocialIdentity owner가 전혀 없는 경우에만 DIRECT_SIGNUP attempt와 email·phone·profile·consent 요구사항을 반환한다. 이 경로에서 User·Session은 생성하지 않는다. 코드 변경과 Jira 작업은 없다.
- 2026-08-24 `FirebaseEnrollmentAttemptService`의 생성·재사용·만료·일회 소비 동시성 흐름을 설명 목적으로 확인했다. 동일 Firebase project·UID·binding에는 PENDING partial unique index로 하나만 허용하고, active attempt는 재사용하며 expired attempt는 조건부 EXPIRED 전환 후 교체한다. 동시 insert loser는 최대 4회 재조회해 winner를 재사용하고 consume은 id·전체 binding·PENDING·미만료 조건을 한 atomic update로 검사한다. 코드 변경과 Jira 작업은 없다.
- 2026-08-24 `FirebaseAuthMethodsSyncTransactionService`의 역할을 설명 목적으로 확인했다. 호출자가 계산한 누락 SocialIdentity 목록을 null·null element 없는 불변 snapshot으로 만든 뒤, 비어 있지 않을 때만 `mongoTransactionManager` Transaction에서 `saveAll`한다. 별도 Bean 경계로 Transaction proxy 적용과 write 구간 최소화를 명확히 하며 코드 변경과 Jira 작업은 없다.
- 2026-08-24 `FirebaseAuthMethodsSyncService`의 전체 흐름을 설명 목적으로 검토했다. Identity JWT의 ACTIVE MEMBER와 기존 FirebaseIdentity를 기준으로 fresh Firebase proof의 project·UID 소유권을 확인하고, Firebase에 연결됐지만 Identity에 없는 SocialIdentity만 Transaction으로 추가하며 동일 요청·동시 unique 충돌은 최종 소유권 재조회로 멱등 처리한다. 코드 변경과 Jira 작업은 없다. 사용자는 향후 단순 코드 설명·리뷰를 작업 기록에서 제외하기를 선호하지만 현재 저장소의 강제 기록 규칙은 유지 중이다.
- 2026-08-24 `DisabledFirebaseAuthMethodsSyncUseCase`의 역할을 저장소 wiring 기준으로 확인했다. `FIREBASE_AUTH_ENABLED`가 기본값 `false`이거나 명시적으로 비활성일 때 실제 `FirebaseAuthMethodsSyncService` 대신 주입되는 fail-closed 구현이며, `/api/v1/auth/firebase/auth-methods/sync` 호출을 무동작 성공으로 처리하지 않고 `FIREBASE_UNAVAILABLE` 503으로 일관되게 거절한다. 코드 변경과 Jira 작업은 없다.
- 2026-08-24 Learning Core와 Identity의 Sentry event·메일 경계를 재확인했다. Identity는 기본 `SENTRY_ENABLED=false`이며 enabled와 DSN이 함께 주입된 환경에서 GlobalExceptionHandler의 예상 밖 500을 명시 capture한다. validation·parse·not found·method/media type·BusinessException과 단순 ERROR 로그는 Sentry event가 아니고 tracing/profiling/Sentry Logs/Logback integration도 꺼져 있다. 실제 메일은 저장소 코드가 아니라 Sentry Alert Rule·environment·수신자 개인 notification에 의해 결정되며 현재 규칙은 저장소에서 확인할 수 없다. Jira 키는 없다.
- Spring Boot 프로젝트 및 Identity용 의존성 구성
- 저장소 Codex 작업 규칙과 Identity–Learning Core JWT 계약 문서화
- CURRENT_STATE/WORKLOG 작업 기록 체계와 Codex Hook 구성
- `origin/develop` 기준 동기화 브랜치에서 `origin/main`을 `--no-ff --no-commit`으로 병합하고, develop의 Firebase·TMI-96 구조와 main의 Quality review 선택 동의·철회 계약을 함께 유지하도록 충돌을 해결함
- Guest POST와 인증된 consent GET·PUT에 `isQualityReviewConsented`, `qualityReviewConsentVersion` 계약을 통합하고 필수 runtime 설정 `QUALITY_REVIEW_CONSENT_VERSION` 및 기존 Mongo 문서·구버전 client 호환 테스트를 develop에 반영함
- 병합 뒤 Firebase 전용 `ConsentPolicy` test fixture와 동의 성공 로그를 조정해 동의 시각을 로그에 남기지 않도록 유지했으며 `./gradlew clean test` 전체 457개 테스트 성공
- Codex 사용자 전역 설정에 Atlassian Remote MCP(`atlassian`) 등록 및 OAuth 연결 확인
- Atlassian MCP의 읽기 전용 조회로 `to-teacher` 사이트의 접근 가능 Jira 프로젝트 1개(`TMI`)와 이슈 생성 권한, 사용 가능한 이슈 유형 `에픽`·`하위 작업`·`작업`·`스토리`를 생성·수정 호출 없이 확인
- 승인된 Refresh Token 재발급·Rotation·재사용 탐지·멱등 로그아웃 Payload로 TMI `작업` 이슈 `TMI-6`을 `High` 우선순위와 기본 상태 `해야 할 일`로 생성하고 담당자·라벨·상태 전환은 적용하지 않음
- Jira `TMI-6`의 사용 가능한 전환을 재확인한 뒤 사용자 승인에 따라 transition ID `21`만 전송해 `해야 할 일`에서 `진행 중`으로 변경하고 다른 필드·댓글·이슈는 수정하지 않음
- Jira `TMI-6` 구현 완료 댓글 초안을 사용자 승인에 따라 댓글 ID `10000`으로 등록하고 상태 `진행 중`과 다른 필드·이슈는 변경하지 않음
- 사용자가 `TMI-6` 구현 PR의 main 병합과 전체 테스트 성공을 확인한 뒤, `완료` 전환 ID `41`을 재확인해 `진행 중`에서 `완료`로 변경하고 다른 필드·댓글·이슈는 수정하지 않음
- Atlassian MCP 읽기 전용 조회로 TMI의 이슈 생성 권한, `작업` 유형 ID `10003`과 생성 필드 20개를 확인하고 `High` 우선순위 지원을 검증한 뒤 JWT 인증·내 프로필·전체 로그아웃 작업의 최종 Payload 초안을 작성했으며 Jira 이슈는 생성하지 않음
- 승인된 JWT 인증·내 프로필·전체 로그아웃 Payload로 TMI `작업` 이슈 `TMI-9`를 `High` 우선순위와 기본 상태 `해야 할 일`로 생성하고 담당자·스프린트·에픽·라벨·상태 전환은 적용하지 않음
- Jira `TMI-9`의 방금 확인한 `진행 중` transition ID `21`만 사용자 승인에 따라 적용해 `해야 할 일`에서 `진행 중`으로 변경하고 다른 필드·댓글·이슈는 수정하지 않음
- Atlassian 공식 MCP로 Jira `TMI-9`의 설명·완료 조건·상태를 구현 전에 읽기 전용 재조회하고 AGENTS.md 및 JWT 계약과 충돌이 없음을 확인했으며 댓글·상태·필드는 변경하지 않음
- 사용자가 PR의 main 병합과 제시된 Jira 변경을 확인·승인한 뒤 TMI-9의 현재 상태와 사용 가능한 전환을 재조회하고 transition ID `41`만 적용해 `진행 중`에서 `완료`로 변경했으며, 후속 조회에서 status ID `10003`과 Resolution `완료`를 확인하고 댓글·필드는 변경하지 않음
- Atlassian 공식 MCP의 읽기 전용 조회로 TMI 이슈 생성 권한, `작업` 유형 ID `10003`, 생성 필드 20개와 `High` 우선순위 ID `2` 지원을 재확인하고 `[Learning Core] Identity JWKS 기반 JWT 인증 연동` 최종 Payload 초안을 작성했으며 Jira 이슈는 생성하지 않음
- 사용자 승인에 따라 `[Learning Core] Identity JWKS 기반 JWT 인증 연동`을 TMI `작업` 이슈 `TMI-10`으로 `High` 우선순위와 기본 상태 `해야 할 일`로 생성하고, 후속 조회에서 제목·유형·우선순위·상태와 담당자 없음·빈 라벨을 확인했으며 스프린트·에픽·상태 전환은 적용하지 않음
- Atlassian 공식 MCP로 `TMI-10`의 제목과 현재 상태 `해야 할 일`을 읽기 전용 재조회하고 사용 가능한 전환 `해야 할 일` ID `11`, `검토 중` ID `31`, `진행 중` ID `21`, `완료` ID `41`을 확인했으며 Jira는 수정하지 않음
- Jira `TMI-10`의 현재 상태와 `진행 중` 전환 ID `21` 사용 가능 여부를 재확인한 뒤 사용자 승인에 따라 transition ID `21`만 적용하고, 후속 조회에서 status ID `10001`의 `진행 중`을 확인했으며 다른 필드·댓글·이슈는 수정하지 않음
- 환경변수 기반 애플리케이션 이름, MongoDB 데이터베이스 및 서버 포트 설정
- Swagger UI `/swagger-ui.html` 및 OpenAPI `/v3/api-docs` 설정과 `SWAGGER_ENABLED` 환경변수 기반 활성화 제어
- Actuator health endpoint 노출
- 외부 MongoDB 연결을 생성하지 않는 격리된 테스트 프로필
- STATELESS·CSRF 비활성화를 유지하면서 명시한 공개 경로만 허용하고 나머지를 인증하는 Security 구성
- 폼 로그인, Basic 인증 및 Spring Security 기본 생성 계정 비활성화
- 제네릭 `BaseResponse` 성공·실패 Factory
- `ErrorCode`, `CommonErrorStatus`, `BusinessException` 오류 기반
- Validation 상세 민감값 제거와 예상하지 못한 오류 정보 비노출을 포함한 전역 예외 처리
- `.env.example`, 실제 환경 파일 ignore 및 README 실행·경계 문서
- 외부 인프라를 호출하지 않는 Bootstrap 테스트 9개 통과
- `users` 컬렉션의 UUID `userId` 기반 User Document와 `ACTIVE`, `SUSPENDED`, `WITHDRAWN` 상태 모델
- Jira `TMI-75` Task(작업)·High 이슈 범위로 보호된 `POST /api/v1/users/withdraw`를 구현했으며 Jira 상태·댓글·필드는 변경하지 않음
- 회원 탈퇴는 JWT `sub`, 요청 Refresh Token 해시의 Session 소유권·미폐기·미만료 상태를 검증하고 LOCAL만 현재 비밀번호를 재확인하며 GUEST는 비밀번호 없이 처리
- User 문서는 삭제하지 않고 `WITHDRAWN` tombstone으로 유지하며 서버 UTC `withdrawnAt`·`updatedAt`, 익명 nickname을 기록하고 email·normalizedEmail·passwordHash·guestInstallationIdHash는 Mongo `$unset`, 기존 userId·provider·createdAt·consents는 유지
- User tombstone 조건부 update와 사용자별 모든 미폐기 RefreshSession의 `ACCOUNT_WITHDRAWN` 폐기는 기존 `mongoTransactionManager` Transaction 하나로 처리하고, 충돌 시 최신 상태를 확인해 멱등 성공하거나 한 번 안전하게 재시도한 뒤 남은 충돌은 `WITHDRAWAL_CONFLICT` 409로 변환
- User 탈퇴 CAS와 동의 ACTIVE+updatedAt partial update로 stale User 전체 저장이 WITHDRAWN을 ACTIVE로 되돌리는 경로를 차단
- Guest 탈퇴 후 같은 installationId는 기존 WITHDRAWN User를 복구하지 않고 새 UUID·RefreshSession을 생성하며, LOCAL 탈퇴 후 같은 이메일도 새 UUID로 가입 가능하고 ACTIVE Guest 중복 409는 유지
- 원본 대소문자를 보존하면서 앞뒤 공백을 제거한 표시용 이메일과 `Locale.ROOT` 소문자 정규화 이메일 분리
- `normalizedEmail`의 `uk_users_normalized_email` unique index 및 MongoDB 자동 index 생성 설정
- BCrypt `PasswordEncoder` Bean과 평문을 Entity에 전달하지 않는 최소 `UserFactory`
- `Instant` 기반 생성·수정 시각과 신규 사용자 `ACTIVE` 초기화
- 정규화 이메일 단건 조회·존재 확인만 제공하는 `UserRepository`
- 실제 MongoDB 연결 없이 User 도메인 기반을 검증하는 테스트를 포함해 전체 23개 통과
- `git diff --check`, 평문 필드·토큰 필드·비소유 도메인 및 생성 경계 정적 검사 통과
- `POST /api/v1/auth/check-email` 이메일 중복 확인 API와 정상 응답 기반 사용 가능 여부 반환
- `POST /api/v1/auth/signup` 일반 이메일 회원가입 API와 Request/Response validation 계약
- 이메일과 닉네임의 앞뒤 공백 제거 후 validation 및 기존 `EmailNormalizer` 기반 중복 조회
- 정규화 이메일 사전 중복 확인과 MongoDB `DuplicateKeyException`의 `EMAIL_ALREADY_EXISTS` 변환
- 비밀번호 8~64자 길이 정책과 별도 복잡도 정규식 없는 BCrypt 해시 저장
- `EMAIL_ALREADY_EXISTS`, 개인정보·약관 동의 필수 및 정책 버전 불일치 도메인 오류와 공통 `BaseResponse` 오류 응답
- User 문서에 개인정보 처리방침·이용약관 상태, 버전과 서버 동의 시각을 `UserConsents` embedded 객체 하나로 저장
- LOCAL 회원가입과 Guest 생성은 두 동의가 true이고 서버 현재 버전과 일치할 때만 진행하며 같은 서버 시각으로 두 동의를 원자적으로 저장
- Guest 동의가 포함된 User와 최초 RefreshSession 저장은 기존 Mongo Transaction에 함께 참여
- `POST /api/v1/auth/guest`는 UUID v4 `installationId`와 개인정보·약관 동의 네 필드를 검증해 최초 Guest와 Token을 생성하며, 설치 ID는 인증 수단이 아니므로 동일 설치 재요청은 기존 Token 복구 없이 409로 거절하고 이후 실행은 저장한 Refresh Token으로 `/api/v1/auth/reissue`를 사용
- `GET /api/v1/users/me/consents`는 JWT `sub`의 ACTIVE 사용자만 조회해 서버 현재 필수 버전, 저장된 동의 상태·버전·시각과 정확한 문자열 비교 기반 `requiresConsent`를 개인정보·약관별로 반환
- `PUT /api/v1/users/me/consents`는 JWT `sub` 사용자만 대상으로 현재 필수 두 정책 동의를 한 User 문서 저장으로 갱신하고, 동일 버전 재요청은 저장과 동의 시각 변경 없이 멱등 성공
- Guest·LOCAL 생성, 동의·프로필·탈퇴, Guest·LOCAL 재가입, Transaction rollback, Security·OpenAPI, JWT·JWKS와 기존 인증 회귀를 포함한 38개 suite의 전체 284개 테스트 성공
- Repository를 Mock 처리한 Service·Controller 테스트와 전체 44개 테스트 통과
- 회원가입 성공 응답 및 오류 응답의 해시·정규화 이메일·자격증명·MongoDB 내부 정보 비노출 검증
- `app.jwt` 기반 issuer, audience, keyId, Access Token TTL, RSA Key Resource 경로 및 기본 scope 설정
- PKCS#8 Private Key와 X.509 Public Key만 지원하고 오류에 키 내용을 포함하지 않는 RSA Resource 로더
- `RsaKeyLoader`의 모호한 `ResourceLoader` 컴포넌트 자동 주입을 제거하고 `JwtConfiguration`에서 유일한 `ApplicationContext`를 명시적으로 전달해 `GridFsTemplate`과의 IDE 빈 후보 충돌을 해소하면서 기존 `rsaKeyLoader` 빈 이름과 Resource 해석 동작 유지
- RSA Key 타입과 Private/Public Key 쌍 일치 검증 및 민감값을 숨기는 Key Material 문자열 표현
- `RSAKey`, `JWKSet`, `JWKSource<SecurityContext>`, `NimbusJwtEncoder`를 사용하는 Spring Security 6.4 호환 서명 구성
- `RS256`, `SIGNATURE`, `kid` 메타데이터가 고정된 서버 내부 RSA JWK와 `Clock.systemUTC()` Bean
- UUID `userId` 검증, 고정 순서 scope 및 기본 scope fallback을 제공하는 내부 `AccessTokenIssuer`
- `sub`, `iss`, 단일 원소 배열 `aud`, `iat`, `exp`, UUID `jti`, 공백 구분 `scope`와 `alg=RS256`, `kid`, `typ=JWT`를 갖춘 Access Token 발급
- `GET /.well-known/jwks.json`에서 `toPublicJWK()` 결과만 표준 JWKS로 반환하고 BaseResponse를 적용하지 않는 공개 endpoint
- 로컬 RSA 2048비트 키 생성 스크립트, Private/Public Key 권한 설정, 명시적 `--force` 교체 및 `.local/` Git ignore
- Java `KeyPairGenerator`와 고정 Clock을 사용하는 JWT·JWKS·Key Loader 테스트 10개 추가 및 전체 54개 테스트 통과
- 실제 PEM 본문·서명 토큰·자격증명 포함 URI·민감 로그 부재와 JWKS/Access Token의 Private Key·자격증명 정보 비노출 검증
- 회원가입 응답과 외부 임시 endpoint에는 Access Token을 연결하지 않음
- `POST /api/v1/auth/login` 일반 이메일 로그인 API와 `LoginRequest`/`LoginResponse` validation·응답 계약
- 정규화 이메일 조회, BCrypt 검증, `ACTIVE` 상태 확인 후에만 토큰 발급을 진행하는 `LoginService.login`
- 존재하지 않는 이메일과 불일치 자격증명을 동일한 `INVALID_CREDENTIALS` 401로 처리하고 비활성 계정을 상태 구분 없는 `ACCOUNT_NOT_ACTIVE` 403으로 처리
- 로그인 성공 시 검증된 User의 UUID를 기존 `AccessTokenIssuer`에 전달해 기존 RS256 Header·Claim·기본 scope·TTL 계약을 그대로 사용
- 기존 `IssuedAccessToken`의 `issuedAt`과 `expiresAt` 차이를 milliseconds로 계산해 `accessTokenExpiresIn`에 반환하고 `grantType`은 `Bearer`로 고정
- `app.refresh-token`의 `Duration` TTL과 최소 32바이트 난수 길이 설정, 기본값 `P14D`와 32 및 32 미만 시작 거부
- `SecureRandom`과 Base64 URL-safe without padding을 사용하는 JWT가 아닌 Opaque Refresh Token 생성기
- UTF-8 원문에 SHA-256을 적용한 뒤 Base64 URL-safe without padding으로 인코딩하는 일관된 Refresh Token 해시기
- `refresh_sessions` 컬렉션의 UUID 기반 세션·사용자·회전 패밀리 관계, 생성·만료·최근 사용·폐기 시각, 폐기 사유와 `@Version` 기반 Optimistic Lock 모델
- Refresh Token 해시 unique index와 `expiresAt`의 `expireAfter = "0s"` TTL index, 해시 단건 조회 및 사용자별 미폐기 Session 조회만 제공하는 `RefreshSessionRepository`
- 공용 `Clock` 기준 최초 Session과 Rotation 후속 Session을 발급하고 Refresh Token 원문은 내부 발급 결과로만 반환하는 `RefreshSessionIssuer`
- `IssuedRefreshSession`은 RefreshSession 저장 성공 후 응답 계층에 전달하는 immutable 발급 결과 record이며 Token 원문·발급·만료 시각의 non-null과 `expiresAt > issuedAt`을 강제한다. 자동 문자열 표현은 Token을 redaction하고 DB에는 계속 Refresh Token hash만 저장한다
- `PreparedRefreshSession`과 `IssuedRefreshSession`의 엔티티 참조 관계를 바로잡았다. `PreparedRefreshSession`은 저장 전 Token 원문과 실제 `RefreshSession` entity 객체를 `session` component로 직접 포함한다. 반대로 `IssuedRefreshSession`은 저장된 entity를 참조하지 않고 `save`가 반환한 entity에서 발급·만료 시각만 복사하고 기존 Token 원문을 함께 담은 저장 후 결과 record다. 두 객체 모두 DB entity 자체는 아니며 DB 영속 대상은 `RefreshSession`뿐이다
- `LoginService`의 LOCAL 이메일 로그인 흐름을 설명했다. 이메일을 공통 정규화해 User를 조회하고 사용자 부재·비밀번호 불일치를 같은 `INVALID_CREDENTIALS`로 처리한 뒤 ACTIVE 상태만 허용한다. 검증 성공 후 canonical userId를 `sub`로 하는 빈 scope RS256 Access Token과 hash만 DB에 저장하는 RefreshSession을 발급해 redacted `LoginResponse`를 반환하고 안전한 성공 로그만 남긴다. 현재 이 메서드에는 Mongo Transaction이 없어 RefreshSession 저장 이후 응답 변환·로그 단계 실패를 함께 rollback하지 않는다
- `POST /api/v1/auth/reissue`에서 해시 조회, ROTATED 재사용 판별, 명시적 만료 경계 검사, `ACTIVE` 사용자 확인, 기존 Session Optimistic Lock 폐기 성공 후 새 Access Token과 Refresh Token 발급
- Rotation 시 기존 Session은 `ROTATED` 사유와 후속 관계를 저장하고 새 Session은 같은 회전 패밀리와 이전 관계를 유지하며, 최초 로그인 Session은 새 UUID 회전 패밀리를 생성
- 같은 Refresh Token의 동시 재발급에서 기존 Session 저장 충돌을 `INVALID_REFRESH_TOKEN`으로 변환하고 충돌 요청에는 Access Token 발급이나 후속 Session 생성을 수행하지 않음
- 이미 Rotation된 Refresh Token 재사용 시 사용자별 미폐기 Session 전체를 `REUSE_DETECTED` 사유로 폐기하고 `REFRESH_TOKEN_REUSE_DETECTED` 401 반환
- 존재하지 않는 Refresh Token은 `INVALID_REFRESH_TOKEN` 401, `expiresAt <= Clock`은 `REFRESH_TOKEN_EXPIRED` 401, 비활성 사용자는 기존 `ACCOUNT_NOT_ACTIVE` 403 정책 적용
- `POST /api/v1/auth/logout`에서 활성·미만료 Session만 `LOGOUT` 사유로 폐기하고 없는·이미 폐기된·만료된 Refresh Token은 성공 처리하는 멱등 흐름 구현
- 재발급·로그아웃 Request의 `NotBlank`와 최대 512자 제한, Refresh Token validation 값 마스킹 및 Request·Response 문자열 redaction 적용
- `POST /api/v1/auth/logout`은 Bearer 인증 없이 요청한 Opaque Refresh Token의 해시로 세션 한 건을 찾고, 활성·미만료 세션만 현재 시각과 `LOGOUT` 사유로 폐기하며 없는·이미 폐기된·만료된 세션은 200 성공으로 멱등 처리
- Reissue 응답의 Access Token·Refresh Token 만료 기간을 milliseconds로 반환하고 내부 사용자·세션·해시·비밀번호 관련 필드를 외부 응답에 포함하지 않음
- 실제 Atlas와 운영 키를 사용하지 않는 Service·Controller·도메인 회귀 테스트를 포함해 전체 97개 통과, 실패·오류·건너뜀 0개
- RefreshSession 원문 필드·Access Token 영속화·민감 로그·운영 자격증명 하드코딩 부재와 외부 응답 내부 필드 비노출 검증
- 기존 `RSAPublicKey`, `JwtProperties`, `Clock`을 재사용하고 자기 JWKS를 HTTP 호출하지 않는 `NimbusJwtDecoder` 구성
- Decoder에서 RS256 서명, 엄격한 `typ=JWT`, 현재 `kid`, 필수 `sub`·`exp`, 주입 Clock 기반 `exp`·`nbf`, issuer와 audience를 `DelegatingOAuth2TokenValidator`로 검증
- 공개 인증 POST 5개, JWKS·health·Swagger/OpenAPI GET만 `permitAll`로 두고 `anyRequest().authenticated()`를 적용하며 Form Login과 Basic 인증은 계속 비활성화
- Security Filter Chain의 401 `COMMON_UNAUTHORIZED`와 403 `COMMON_FORBIDDEN`을 UTF-8 `BaseResponse` JSON으로 반환하고 내부 JWT 예외나 입력 자격증명을 노출하지 않음
- `JwtCurrentUserProvider`가 인증된 `JwtAuthenticationToken` 또는 JWT principal의 `sub`만 읽고 canonical UUID로 검증해 내부 사용자 식별자로 제공
- `GET /api/v1/users/me`에서 JWT `sub`로 User를 조회하고 `ACTIVE` 상태를 확인한 뒤 provider와 개인정보·약관 동의 상태·버전·서버 시각을 전용 DTO로 반환
- User에 최소 `UserProvider.LOCAL` 모델을 추가하고 기존 문서의 null provider는 LOCAL로 읽어 현재 이메일 계정과 호환
- `POST /api/v1/auth/logout-all`에서 JWT 사용자의 미폐기 RefreshSession만 조회해 같은 Clock 시각과 `LOGOUT_ALL` 사유로 `saveAll`하며 빈 Session 목록과 반복 요청은 성공 처리
- logout-all은 새 Access Token이나 Refresh Token을 발급하지 않고 Repository 오류를 성공으로 숨기지 않으며 기존 `@Version` Optimistic Lock 구조를 유지
- `LogoutAllService`의 현재 동작을 설명했다. 보호된 요청의 JWT `sub`를 `CurrentUserProvider`에서 얻어 해당 userId의 `revokedAt=null` RefreshSession만 조회하고, 공통 Clock 시각으로 `lastUsedAt`·`revokedAt`과 `LOGOUT_ALL` 사유를 설정해 `saveAll`한다. 활성 Session이 없거나 반복 호출이면 저장 없이 성공하는 멱등 명령이며 안전한 건수 로그만 남긴다. RefreshSession 폐기는 새 재발급을 막지만 이미 발급된 stateless Access Token을 즉시 무효화하지 않고, 여러 Session 저장은 현재 Mongo Transaction이 아니다
- 전체 로그아웃 후 재로그인 Session 동작을 설명했다. ACTIVE LOCAL 사용자가 이메일·비밀번호 인증에 다시 성공하면 `RefreshSessionIssuer.issue(userId)`가 새 Refresh Token 원문·hash, 새 sessionId·rotationFamilyId와 새 만료 시각을 가진 `RefreshSession`을 insert한다. `LOGOUT_ALL`로 폐기된 기존 Session은 복구·재활성화하지 않고 폐기 이력으로 남으며 새 Session만 향후 재발급에 사용할 수 있다. 로그인 횟수마다 새 Session을 허용하므로 전체 로그아웃과 동시에 발생한 로그인은 조회 snapshot 밖의 새 Session으로 남을 수 있다
- 이동된 `docs/contracts/social-login-implementation-plan.md` 전체 계획을 설명했다. Firebase는 email/password·Google·Apple·phone과 PoC 통과 시 Kakao credential 인증·ID Token 발급만 담당하고 Identity는 canonical UUID User, FirebaseIdentity·SocialIdentity·PhoneIdentity, enrollment·Guest 승격·merge, 동의·탈퇴와 자체 RS256 Access/RefreshSession을 소유한다. Firebase 인증 성공만으로 MEMBER를 만들지 않고 같은 UID에 link된 verified phone·필수 동의와 Mongo finalize Transaction을 요구한다. 단계 1 SocialIdentity와 단계 2 UserAccountType만 완료됐고 즉시 다음 범위는 production API가 아닌 단계 0 Firebase ADR·격리 PoC이며, broker foundation·PhoneIdentity·가입·Guest 승격·merge·Entitlement 연동은 후속 단계다
- 무료 모의고사 1회 확인 엔티티의 존속 여부를 명확히 했다. 검증 번호별 혜택 지급 이력인 `TrialClaim`은 없어지지 않았고 별도 Entitlement/Billing이 계속 소유한다. 가입 시에는 TrialClaim을 만들지 않고 Identity의 generic `PhoneEligibilityBindingOutbox`를 통해 benefit-scoped candidate를 전달해 Entitlement가 `VerifiedPhoneBenefitBinding`을 저장하며, 첫 무료시험 요청에서 TrialClaim 부재를 확인해 TrialClaim과 1회 `UserEntitlement`를 원자 생성한다. 시험 생성 중에는 `EntitlementReservation`으로 reserve·confirm/cancel하며 네 모델 모두 현재 계획 단계이고 Identity 저장소 구현 대상이 아니다
- Atlassian 공식 MCP로 TMI-88·TMI-89의 기존 Identity Jira 형식, TMI `작업` 유형 ID `10003`과 생성 필드 20개를 읽기 전용으로 재확인했다. 다음 Jira 초안은 `[Identity] Firebase 인증 broker ADR 및 격리 PoC`이며 Stage 0의 책임 경계·email/password/Google/Apple·동일 UID phone link·목적별 Token 검증·Kakao Identity Platform OIDC·Guest merge proof·중단 가입 lifecycle을 검증하고 production API·실제 User 모델/Repository·PhoneIdentity·merge·Entitlement 구현은 제외한다. 생성·댓글·상태 변경은 사용자 승인 전 수행하지 않는다
- Spring Security test 지원을 추가하고 실제 외부 인프라 없이 공개·보호 경로, Decoder 실패, scope 권한 변환, 프로필 노출 경계와 전체 로그아웃 사용자 격리를 검증
- `domain.auth`와 `domain.user` 아래에 API·application·DTO·domain·exception을 배치하고 공통 설정·응답·예외·Spring Security 기술 구현을 `global` 아래로 이동
- MongoDB 상태 객체 `RefreshSession`·폐기 enum·Repository를 Auth 도메인에 두고 Refresh Token 난수 생성·SHA-256 해싱·설정은 `global.security.refresh`로 분리
- 비대했던 `AuthService`를 `EmailAvailabilityService`, `SignupService`, `LoginService`, `TokenReissueService`, `LogoutService`로 분리하고 기존 `LogoutAllService`와 함께 Controller가 유스케이스만 호출하도록 구성
- 로그인·재발급의 다중 토큰 응답 조합은 `AuthResponseConverter`로 통합하고 `AuthException`·`UserException`은 기존 `BusinessException`과 오류 코드/HTTP 상태 계약을 유지
- OpenAPI 제목·설명·버전과 `bearerAuth` JWT 스키마를 추가하고 Auth·User·JWKS Tag, API 응답, DTO Schema를 문서화하며 보호 API 두 개에만 Bearer 요구사항 적용
- 공개 API의 Swagger 인증 표시 부재, 보호 API의 Bearer 표시, 비밀번호 write-only와 비밀번호 예시 부재를 `/v3/api-docs` 계약 테스트로 검증
- malformed JSON, 미존재 리소스, 지원하지 않는 Method와 Media Type을 각각 안전한 공통 400·404·405·415 응답으로 변환하고 요청 원문·내부 예외 정보를 노출하지 않도록 전역 예외 처리 보완
- malformed JSON 보안 테스트의 미완성 JSON 문자열을 지역 변수로 분리해 IDE 파서 혼동을 줄이면서 기존 400 응답·민감 입력 비노출 검증 의미를 유지
- 계정 활성 상태 오류 소유권을 User 도메인으로 일원화하고 converter의 application 내부 결과 타입 역참조를 제거해 Auth → User와 application → converter의 단방향 의존으로 정리
- 리팩토링 신규 파일과 기존 삭제를 함께 stage해 104개 논리 변경 파일, rename 68개, untracked 0개 상태를 구성하고 삭제 전용 index 문제를 해소
- 관련 테스트와 기존 signup·login·reissue·logout·JWT·JWKS 회귀를 포함한 전체 146개 통과, 실패·오류·건너뜀 0개이며 `./gradlew build` 성공
- 실제 JAR 기동으로 health·Swagger/OpenAPI 200, 공개·보호 operation 구분, malformed JSON 400, 미존재 경로 404, 무인증 보호 API 401, 공개 로그인 validation 접근과 415를 확인하고 Swagger 비활성 문서 경로의 404를 검증
- main 병합 후 인증 유스케이스·RefreshSession·JWT·Security·예외 처리의 비자명한 의도에만 한국어 한 줄 주석 25개를 추가하고 실행 코드는 변경하지 않은 채 전체 146개 테스트와 build를 재검증
- 환경변수로 조정 가능한 ECS 구조화 stdout과 `X-Request-ID` 검증·생성·응답 전달·MDC 정리를 구현하고, HTTP 완료 로그에 event·outcome·requestId·method·route template·status·duration·errorCode를 기록하며 health·Swagger/OpenAPI·JWKS 정상 요청은 제외
- 예상 밖 5xx의 단일 ERROR 소유자를 요청 완료 filter로 두고, `GlobalExceptionHandler`는 원본 message·Throwable 없이 예외·cause 타입과 최대 24개 stack frame의 안전한 오류 문맥만 request attribute로 전달하며 `errorLogged` guard로 ERROR/Error dispatch 중복을 방지
- 회원가입·Guest 생성·로그인·Refresh Token 재발급·재사용 탐지·동시 Rotation 거절·단일/전체 로그아웃·동의 갱신·회원 탈퇴 성공/멱등/충돌과 Mongo Transaction capability 검증을 저장 또는 transactional proxy 반환 이후의 구조화 상태 전이 이벤트로 기록
- 요청 로그와 상태 전이 로그의 레벨·필드·MDC 정리·민감값 비노출, 예상 밖 5xx ERROR 1건과 INFO 0건, BusinessException ERROR 0건, Security 401 requestId 전파를 포함해 전체 40개 suite·292개 테스트 성공
- PR #15의 main 병합 후 실제 애플리케이션 기동에서 Spring Boot의 ECS 설정이 애플리케이션뿐 아니라 Spring·Tomcat·MongoDB 드라이버의 모든 콘솔 로그를 한 줄 JSON으로 변환하고, `mongodb.transaction_capability.verified` 사용자 정의 event가 구조화 필드와 함께 출력되는 것을 확인
- `LogoutAllService`의 Token issuer 비의존성 테스트에서 `Stream<Class<?>>`와 `List<Class<?>>` 대입 모두에 발생한 wildcard capture 문제를 제거하기 위해 reflection field를 `anyMatch`로 직접 비교하고 boolean을 AssertJ로 검증하며, 같은 두 의존성 부재 의미를 유지한 채 전체 40개 suite·292개 테스트 재통과
- 애플리케이션이 직접 기록하는 18개 구조화 로그 `message`를 일관된 한글 문장으로 변경하고, 운영 검색 계약인 `event`·`outcome`·`errorCode`와 ECS 표준 key는 영어로 유지했으며 HTTP 정상/실패와 주요 상태 전이의 정확한 한글 message·민감정보 비노출을 테스트
- Sentry 공식 Spring Boot 문서에서 Gradle plugin `6.18.0`이 Spring Boot 3에 맞는 Jakarta starter를 자동 선택함을 확인하고, runtime DSN·environment·release와 PII·request body·trace sampling·test 비활성 설정이 build plugin 외에 별도로 필요함을 검토
- 현재 `GlobalExceptionHandler`가 예상 밖 예외까지 처리하므로 Sentry 기본값의 unhandled-only 수집에서는 5xx가 누락될 수 있고, exception resolver 순서를 앞당기면 Business·Validation 4xx까지 수집될 수 있으며 SentryAppender 기본 ERROR issue와 기존 `http.request.failed`가 중복될 수 있음을 확인
- Sentry 구현에는 실제 DSN 전달이 필요하지 않으며 저장소에는 `${SENTRY_DSN}` placeholder와 비밀값 없는 `.env.example` 이름만 추가하고, 실제 DSN은 사용자가 로컬 비추적 환경 파일 또는 배포 Secret에 직접 주입하는 작업 경계를 확정
- Sentry 적용을 dependency 확인, 안전한 runtime 기본값, 예상 밖 오류 단일 capture, event 정제, 격리 테스트, CI source context, staging 검증·점진 활성화 순서로 나누고 expected 4xx 0건·unexpected 5xx 1건·민감정보 0건을 완료 조건으로 확정
- Sentry JVM Gradle plugin `6.18.0`과 SDK `8.42.0`을 적용하고 Spring Boot 3 Jakarta starter·Logback 모듈의 runtime dependency 해석을 확인했으며 source context는 비공백 build 인증 값이 있는 CI에서만 opt-in
- Runtime은 기본 disabled·빈 DSN, PII false, request body `NONE`, tracing·profiling·Sentry Logs off와 Logback integration off로 구성하고 test profile도 명시적으로 비활성화했으며 실제 값 대신 환경변수 이름만 추가
- `GlobalExceptionHandler`가 처리한 예상 밖 Exception만 request scope를 비운 뒤 안전한 requestId·errorCode·HTTP method·route template·500 tag와 함께 명시 capture하며 Business·Validation·Security 4xx 및 handler resolver 순서는 변경하지 않음
- `beforeSend`는 SDK가 조립한 event를 부분 마스킹하지 않고 새 event로 재구성해 exception message, request/response 정보, Header·Cookie·query·body, user, breadcrumb, extra, thread, context, modules와 unknown field를 제거하고 message 없는 예외 type·정제 stack frame, 안전한 tag·설정 environment/release 및 source context용 UUID 형식 JVM debug bundle ID만 유지
- 실제 Sentry pipeline의 event processor가 모든 민감 위치에 동일 sentinel을 삽입한 뒤 test transport가 받은 최종 event JSON 전체에서 sentinel 0건을 검증하고, 기존 `http.request.failed` ERROR가 발생해도 handled 5xx event 정확히 1건·expected 4xx 0건·Sentry Logback initializer 부재를 확인해 전체 41개 suite·295개 테스트 성공
- Sentry 배포 시 Runtime에는 `SENTRY_ENABLED=true`, 배포 Secret의 `SENTRY_DSN`, 환경 구분용 `SENTRY_ENVIRONMENT`, 배포 식별용 immutable `SENTRY_RELEASE`를 설정하며 실제 비밀값 성격의 필수 입력은 DSN 하나임을 확인. `SENTRY_AUTH_TOKEN`은 source context 업로드를 승인한 CI build에서만 선택적으로 사용하고 Runtime에는 주입하지 않음
- 외부 통신 없는 `SentryCaptureIntegrationTests`로 handled 5xx 한 건·expected 4xx 0건·민감 sentinel 0건을 확인하고, 임시 staging one-shot trigger로 실제 DSN·SDK transport·Sentry project 수신 경계를 확인했다. 사용자가 project 표시를 확인해 실제 연결 검증이 완료됨
- 실제 검증 완료 직후 `SentryStagingSmokeTrigger`, 전용 조건·통합 테스트, smoke 설정·환경변수와 README 안내를 제거하고 `sentry.enabled=false`, `sentry.environment=local` 안전 기본값을 복원했다. 임시 trigger가 Runtime 또는 test artifact에 남지 않으며 전체 41개 suite·295개 테스트 성공
- 비HTTP event에서 `http.method` tag가 없을 때 `beforeSend`가 실패하지 않도록 한 null-safe allowlist 보완은 일반 Sentry event 안정성에 유효하므로 유지했다. Runtime event 대상 project는 계속 주입된 DSN이 결정하고 source context용 Gradle project 설정과 분리됨
- `docs/contracts/social-login-implementation-plan.md`를 Firebase 인증 broker 기준으로 전면 개편했다. Firebase는 email/password·Google·Apple·phone credential과 PoC를 통과한 Kakao OIDC 인증을 담당하고, Identity는 Firebase ID Token 교환, Firebase UID 매핑, canonical User·SocialIdentity·PhoneIdentity, 가입·Guest 승격·merge·탈퇴와 자체 Access/Refresh Token을 소유한다. 이전의 Identity 직접 Provider verifier·SocialLoginChallenge·자체 OTP 계획은 기본 구현 대상에서 제외했다
- 무료 모의고사는 `ACTIVE MEMBER`만 시작할 수 있고 Guest에는 `MEMBERSHIP_REQUIRED`를 반환한다. 신규 MEMBER는 같은 Firebase account의 검증된 phone credential과 필수 동의를 Identity 최종 Transaction에서 확인해야 하며, 기존 MEMBER 로그인에는 전화 인증을 반복하지 않는다. 전화 인증·회원가입 성공은 무료체험 지급이나 사용이 아니고 TrialClaim은 첫 무료시험 요청에서만 별도 서비스가 생성한다
- PhoneIdentity와 fingerprint만으로 번호 소유가 검증되는 것은 아니며 SMS 발송·code 검증은 Firebase Phone Auth가 담당한다. Identity는 검증된 Firebase ID Token의 phone 관련 Claim과 동일 Firebase UID 연결 상태를 확인한 뒤 E.164 번호를 즉시 HMAC fingerprint로 변환해 PhoneIdentity를 확정하며, phone-only Firebase 로그인을 앱 로그인으로 허용하지 않는다
- Firebase Auth를 공통 authentication broker로 사용하는 대안을 검토했다. Google·Apple·전화번호는 Firebase의 통합 인증 경계로 처리할 수 있고 Kakao는 기본 provider와 동일한 무설정 경로가 아니라 Identity Platform의 generic OIDC 가능성 검증 또는 Kakao 검증 후 Firebase Custom Token 경로가 필요하다. Firebase를 채택해도 User·SocialIdentity·PhoneIdentity·Guest 승격·merge·무료체험 정책과 자체 Access/Refresh 발급은 Identity가 계속 소유한다
- Firebase로 LOCAL email/password까지 완전 이전하면 이메일·비밀번호 일치 검증도 Firebase Auth가 소유한다. 일반 흐름은 client의 Firebase email/password sign-in, Firebase ID Token 획득, Identity token exchange, Firebase UID에서 기존 canonical userId 조회, 자체 RS256 Access/Refresh 발급이며 Identity의 기존 BCrypt 비교는 migration 기간 뒤 제거한다. 로그인 화면과 내부 User는 유지되지만 credential 저장·검증 주체만 바뀐다
- 사용자가 아직 운영 기존 회원이 없다고 확인해 BCrypt hash import·lazy migration·비밀번호 reset 이관은 불필요한 것으로 계획한다. Firebase 채택 시 신규 가입부터 Firebase credential을 사용하고 기존 직접 signup/login 코드와 passwordHash는 공개 전환 전에 제거 또는 비활성화한다. 다만 실제 운영 User 0건은 cutover 전에 read-only 확인한다
- Firebase broker안과 기존 direct안 비교 결과, 전자는 email/password·Google·Apple·phone의 SDK·비밀번호 reset·email verification·credential 보안 운영을 위임해 MVP 속도와 일관성이 좋고, 후자는 vendor 독립성·서버 제어·Kakao 직접 연동·원자적 enrollment 모델은 좋지만 Provider별 nonce/JWKS·password lifecycle·SMS OTP·abuse 방어를 모두 직접 구현·운영해야 한다. 기존 회원이 없는 현재에는 Kakao PoC가 통과하면 Firebase broker안의 순효과가 더 크다
- Firebase 사용 시 내부 User와 내부 JWT의 필요성을 분리했다. 내부 User는 Firebase credential과 별개로 canonical UUID, GUEST/MEMBER, ACTIVE/SUSPENDED/WITHDRAWN/MERGED, 약관·프로필, Guest 승격·merge와 서비스 데이터 소유권을 표현하므로 유지한다. 내부 JWT는 이론상 Firebase ID Token을 모든 서비스가 직접 검증하도록 전면 전환하면 제거할 수 있지만 현재 Identity issuer·UUID sub·`tosunsaeng-learning-core` audience·scope·JWKS·RefreshSession 계약을 보존하려면 Firebase credential을 자체 Token으로 exchange한다
- Firebase/Identity 목표 책임 경계를 확인했다. Firebase는 email/password·Google·Apple·phone과 검증 가능한 Kakao 경로의 credential 등록·소유 검증, password reset·email verification·SMS verification 및 Firebase ID Token 발급을 담당한다. Identity는 Firebase ID Token을 검증·교환하고 canonical UUID User, 가입 완료·필수 전화/동의, status·profile, SocialIdentity/PhoneIdentity, Guest 승격·merge, 탈퇴와 자체 RS256 Access/RefreshSession을 담당한다. Firebase ID Token은 Learning Core에 직접 전달하지 않는다
- 앱 첫 진입부터 Firebase email/password 또는 SNS로 인증할 수 있다. FirebaseIdentity가 이미 있으면 Guest 생성 없이 canonical ACTIVE MEMBER로 로그인하고, 미등록 Firebase UID면 내부 User를 즉시 만들지 않고 짧은 TTL의 `FirebaseEnrollmentAttempt`를 만든다. 같은 Firebase UID에 phone credential을 link하고 필수 동의를 제출한 뒤 attempt 조건부 소비와 함께 User·FirebaseIdentity·PhoneIdentity·SocialIdentity·최초 RefreshSession을 원자적으로 생성한다
- Provider별 nonce·state·PKCE와 credential replay 방어는 Firebase client/provider 인증 흐름이 담당하고, Identity는 Firebase ID Token의 서명·issuer·audience·만료·폐기 여부·인증 시각·sign-in provider와 enrollment purpose/binding을 검증한다. Identity가 직접 발급하던 `SocialLoginChallenge`는 기본 구현하지 않는다
- TrialClaim과 UserEntitlement의 역할을 분리해 문서화했다. TrialClaim은 `benefitType + benefit-scoped phoneFingerprint` 기준의 무료혜택 1회 지급 ledger이고, UserEntitlement는 canonical userId에 귀속돼 reserve·consume·보상되는 실제 사용권이다. 첫 무료시험 claim에서 둘을 원자적으로 만들고 사용 시 entitlement만 변경하며 claim은 재지급 방지를 위해 유지한다
- `providerSubject`는 1~255자 계약을 유지한다. 새 구현 순서는 Firebase ADR·PoC, Firebase broker foundation, PhoneIdentity, Firebase exchange/direct signup, Guest 승격·인증수단 sync, Guest merge·outbox, Kakao·Apple lifecycle 순으로 재편했으며 merge 단계 전 다른 owner를 발견하면 mutation 없이 `MERGE_REQUIRED`만 반환한다
- merge source JWT를 target actor로 승격하거나 authorization alias로 해석하지 않는 공통 정책을 확정했다. Identity는 `ACCOUNT_MERGED_TOKEN_REJECTED`로 거절하고 downstream은 ownership migration·source deny marker를 원자 저장하며, 모든 참여 서비스가 준비되기 전에는 merge flag를 열지 않는다
- FirebaseEnrollmentAttempt는 짧은 TTL·purpose·Firebase UID·Guest binding을 가지며 조건부 `PENDING → CONSUMED` CAS의 승자만 최종 가입·승격을 진행한다. 가입 proof 누락과 기존 MEMBER의 보완 onboarding 오류는 서로 다른 코드로 유지한다
- 전화번호 정책을 `검증 번호당 동시에 ACTIVE MEMBER 1개`로 확정했다. phone은 로그인 ID·canonical 선택·자동 merge 키가 아니지만 회원가입 uniqueness constraint이며, 반드시 먼저 인증한 같은 Firebase UID에 credential을 link한다. 다른 Firebase User 또는 PhoneIdentity가 번호를 소유하면 `PHONE_ALREADY_LINKED`로 거절하고, SUSPENDED 계정은 점유를 유지하며 가입 중단 Firebase User의 번호 점유는 resume 유예 후 unlink/delete cleanup한다
- 여러 기기 Guest merge는 source Guest의 Identity JWT와 기존 SNS owner Firebase User의 fresh ID Token을 동시에 검증한다. credential ownership 충돌 시 임시 Firebase User로 link하지 않고 기존 owner로 sign-in하며, email·phone으로 target을 추정하지 않는다
- 같은 Firebase project·UID·binding에는 유효한 PENDING FirebaseEnrollmentAttempt 하나만 허용하고 반복 exchange는 기존 enrollmentId를 재사용한다. status partial unique index, application `expiresAt`, 만료 CAS와 duplicate winner 재조회로 동시성을 처리하며 TTL은 cleanup에만 사용한다
- Firebase revoke·disabled remote 검사는 login exchange와 signup·upgrade·merge·link/unlink·withdrawal 같은 목적별 경로에 적용하고, Learning Core 일반 요청과 Identity RefreshSession reissue에는 Firebase 호출을 추가하지 않는다. Kakao Generic OIDC는 Identity Platform 업그레이드·billing·provider 등록·모바일 redirect까지 PoC한다
- PhoneIdentity fingerprint와 FREE_MOCK_EXAM fingerprint는 서로 다른 key·version·domain separator로 파생한다. raw phone과 PhoneIdentity fingerprint를 Entitlement에 전달하지 않고 가입·phone 교체 시 consumer-scoped binding outbox를 만들며, TrialClaim·UserEntitlement는 첫 무료시험 요청 전에는 생성하지 않는다
- `EmailAvailabilityService`의 현재 책임을 설명했다. 공개 `POST /api/v1/auth/check-email`의 검증된 이메일을 공통 `EmailNormalizer`로 trim·`Locale.ROOT` 소문자화한 뒤 `existsByNormalizedEmail`을 조회해 사용 가능 boolean과 안내 메시지를 반환한다. 이는 조회 시점의 안내일 뿐 이메일 예약이나 가입 성공 보장이 아니며, 최종 중복 방지는 `SignupService`의 재검사와 `normalizedEmail` partial unique index·`DuplicateKeyException` 변환이 담당한다
- 회원 탈퇴 tombstone의 의미를 설명했다. `users` 문서를 물리 삭제하지 않고 같은 `userId`의 상태를 `WITHDRAWN`으로 바꾸며 nickname을 탈퇴 표시값으로 치환하고 탈퇴·수정 시각을 남긴다. 동시에 email·normalizedEmail·passwordHash·guestInstallationIdHash 필드는 MongoDB `$unset`으로 문서에서 제거하므로 자격증명 사용과 중복 index 점유가 끝난다. 따라서 `tombstone에서 제거`는 tombstone 문서를 지운다는 뜻이 아니라 tombstone 안의 해당 필드를 없앤다는 뜻이다
- `GuestAuthService`의 현재 책임과 실패 경계를 설명했다. 공개 `POST /api/v1/auth/guest` 요청의 UUID v4·필수 동의를 검증하고 설치 UUID의 SHA-256 hash로 중복을 사전 확인한 뒤 서버 UUID의 ACTIVE GUEST, RS256 Access Token과 원문 비저장 RefreshSession을 준비한다. 별도 Mongo Transaction service가 User·RefreshSession 저장과 응답 구성을 함께 commit하며 설치 hash unique index가 동시 요청을 최종 차단한다. 설치 UUID는 인증·복구 수단이 아니므로 기존 Guest의 Token을 재발급하지 않고 중복 요청은 `GUEST_ALREADY_EXISTS`로 거절한다
- Guest 설치 UUID hash와 응답 유실 상태를 명확히 했다. `GuestInstallationIdHasher`는 UUID v4를 trim·canonical lowercase 문자열로 정규화하고 UTF-8 bytes에 SHA-256을 적용한 32 bytes digest를 padding 없는 Base64URL 43자로 저장한다. 이는 동일 입력을 같은 값으로 만드는 단방향 fingerprint이며 암호화·복호화나 salt/HMAC 방식은 아니다. commit 후 응답 유실을 서버가 감지하거나 Token을 복구하는 코드는 없고, 이미 구현된 중복 검사에 따라 같은 설치 UUID 재시도는 `GUEST_ALREADY_EXISTS`가 된다. 이 현재 동작은 단위 테스트로 고정되어 있으나 별도 복구 API는 미구현이다
- raw 번호 없는 HMAC rotation을 위해 `PhoneFingerprintAlias`와 `ACTIVE_WRITE → LOOKUP_ONLY → RETIRED` key lifecycle을 채택했다. retained phone·benefit version candidate 전체로 TrialClaim을 조회하고 참조가 남은 legacy key 폐기와 mixed writer를 차단한다
- 무료시험 시작은 `reserve → exam 생성 → confirm`으로 고정하고 확정 실패는 cancel, 결과 불명 timeout은 `RECONCILIATION_REQUIRED` 후 exam 존재 여부를 대조하도록 정리했다. provider null의 MEMBER fallback 전 운영 read-only aggregate를 필수화하고 Identity/Social과 Entitlement/Billing을 독립 트랙으로 표시했다
- 사용자 이해 확인용으로 전체 합의를 사용자 흐름 중심으로 다시 요약했다. User는 canonical 계정, SocialIdentity는 Google·Kakao·Apple 연결, PhoneIdentity는 가입 시 검증된 번호 소유 관계, TrialClaim은 번호별 무료혜택 수령 이력, UserEntitlement는 실제 사용권이라는 역할을 유지한다. Guest 둘러보기, 처음부터 SNS 로그인·가입, Guest의 미연결 SNS 승격, 기존 SNS owner 발견 시 단계 8 merge, 회원가입 전화 인증, 무료시험 lazy claim과 reserve/create/confirm 흐름을 하나의 설명으로 정리했으며 설계나 코드는 추가 변경하지 않았다
- 목표 모델을 수명과 소유 경계별로 재정리했다. Identity의 장기 엔티티는 User·FirebaseIdentity·SocialIdentity·PhoneIdentity·PhoneFingerprintAlias·RefreshSession, 단기 엔티티는 FirebaseEnrollmentAttempt, 전달 엔티티는 PhoneEligibilityBindingOutbox·UserMergedOutbox다. VerifiedPhoneBenefitBinding·TrialClaim·UserEntitlement·EntitlementReservation은 별도 Entitlement/Billing 소유이며 Firebase 검증 결과와 ID Token 원문은 영속 계정 엔티티가 아니다
- 첫 구현 범위를 최소 SocialIdentity 데이터 계층으로 재확인했다. `SocialProvider(GOOGLE, KAKAO, APPLE)`, `SocialIdentity(socialIdentityId, userId, provider, providerSubject[1..255], createdAt)`, provider+subject unique index, userId non-unique index, Repository 조회 계약과 도메인·Repository·index 테스트만 포함한다. email·updatedAt, Provider Token 검증·API·challenge, User/accountType·Guest 승격·병합, PhoneIdentity·OTP, TrialClaim·Entitlement, outbox·Learning Core와 Access/Refresh 변경은 첫 범위에서 제외하며 설계나 코드는 추가 변경하지 않았다
- Atlassian 공식 MCP로 TMI 프로젝트와 기존 Identity 작업 형식·생성 필드를 읽기 전용 확인했다. 첫 범위용 신규 Jira 초안은 `작업` 유형, 제목 `[Identity] SocialIdentity 모델·인덱스·Repository 기반 구축`, 기본 상태·보고자 사용, 담당자·우선순위·라벨 미지정으로 준비했으며 본문은 최소 모델·index·Repository·테스트와 명시적 제외 범위만 포함했다. 이 단계에서는 사용자 사전 승인을 받기 위해 생성하지 않았다
- 사용자 승인 후 Jira `TMI-88` `[Identity] SocialIdentity 모델·인덱스·Repository 기반 구축`을 TMI 프로젝트의 `작업`으로 생성하고 재조회했다. 상태는 `해야 할 일`, 담당자는 없고 프로젝트 기본 우선순위 `Medium`이 적용됐으며 승인한 설명·완료 조건·테스트·제외 범위가 저장됐다. 댓글과 상태 변경은 수행하지 않았다
- Jira `TMI-88` 범위로 Auth 도메인에 `SocialProvider(GOOGLE, KAKAO, APPLE)`, `social_identities` Mongo document와 `SocialIdentityRepository`를 구현했다. SocialIdentity는 서버 생성 UUID id, canonical UUID 문자열 userId, non-null provider, 원문 보존 1~255자 providerSubject와 createdAt만 저장하고 email·updatedAt·Token·Claim·User 객체·`@DBRef`를 보유하지 않는다
- `uk_social_identities_provider_subject` compound unique index와 `ix_social_identities_user_id` non-unique index를 선언했다. `(userId, provider)` unique 제약은 추가하지 않아 한 canonical User에 Google·Kakao·Apple을 함께 연결할 수 있고 동일 문자열 subject도 Provider namespace가 다르면 허용한다
- 외부 Atlas를 호출하지 않는 `mongo-java-server` test dependency를 추가해 실제 Spring Data Repository proxy와 index resolver를 사용했다. provider+subject canonical owner 조회, userId 전체 조회, 대소문자 구분, 복수 Provider 연결, 동일 provider+subject duplicate insert의 `DuplicateKeyException`, 실제 index 이름·unique·key 순서를 검증했으며 전체 `./gradlew clean test` 44개 suite·308개 테스트가 skip·실패·오류 없이 성공했다
- GitHub PR #16의 `develop` 병합 commit `94d0e61a5cd9d4c8b72ce131143756fece95cd39`을 확인한 뒤 사용자 승인으로 Jira `TMI-88`을 transition ID `41`로 `완료` 전환했고 Resolution도 `완료`임을 재확인했다. 댓글·담당자·우선순위와 다른 필드는 변경하지 않았다
- 사용자 승인으로 Jira `TMI-89` `[Identity] UserAccountType 분리 및 legacy User 호환 기반 구축`을 `작업` 유형으로 생성했다. 기본 상태 `해야 할 일`, 기본 우선순위 `Medium`, 담당자 없음이며 댓글과 상태 전환은 수행하지 않았다
- GitHub PR #17이 `develop`에 병합된 merge commit `10b1fa0ac23015191ec41fb62b7e9f54f4cedc28`을 로컬 `develop`과 `origin/develop`에서 확인했다. Jira `TMI-89`은 여전히 `해야 할 일`·Resolution 없음이고 종료 댓글도 없으며, 사용 가능한 `완료` transition ID `41`을 읽기 전용으로 확인했다
- 사용자 승인에 따라 Jira `TMI-89`에 UserAccountType·legacy fallback·프로필 호환·탈퇴 분리, 변경 파일, 전체 45개 suite·317개 테스트 결과와 운영 legacy 데이터 점검 위험을 담은 종료 댓글 ID `10003`을 등록했다. transition ID `41`만 적용해 상태와 Resolution이 모두 `완료`임을 재확인했으며 다른 필드는 변경하지 않았다
- 사용자 승인에 따라 Jira `TMI-90` `[Identity] Firebase 인증 broker ADR 및 격리 PoC`를 TMI `작업` 유형과 기본 우선순위 `Medium`으로 생성하고 제목·설명·유형·우선순위·상태를 재조회했다. 상태는 `해야 할 일`, Resolution은 없으며 담당자·라벨·컴포넌트·상위 항목을 지정하지 않았고 댓글이나 상태 전환도 수행하지 않았다
- Jira `TMI-90`을 구현 전에 Atlassian 공식 MCP로 재조회해 책임 경계·검증 범위·산출물·완료 조건·제외 범위가 AGENTS.md와 기존 JWT 계약에 충돌하지 않음을 확인했다. Jira 댓글·필드·상태는 변경하지 않았다
- `ADR-001`에서 Firebase credential broker·Identity canonical account owner, password email verification, same-UID phone link·phone-only login 거절, 목적별 revoke·recent-auth, active enrollment 재사용, Guest merge dual proof, abandoned enrollment cleanup, lifecycle·장애·vendor exit와 production gate를 조건부 채택했다
- `poc/firebase-auth`를 Java production artifact와 분리된 Node devDependency 모듈로 추가했다. demo Firebase Auth Emulator만 사용해 email/password, Google·Apple provider mapping, Admin Token verify·disabled·revoke·delete, phone link UID 유지·phone-only User·번호 충돌·owner delete 후 재사용을 검증하고 Kakao 공개 OIDC discovery는 별도 opt-in 테스트로 분리했다
- PoC contract 7개, Auth Emulator 5개, Kakao discovery 1개와 `./gradlew clean test` 전체 45개 suite·317개 테스트가 failure·error·skip 없이 성공했다. 실제 credential·운영 Firebase project·외부 Google/Apple·SMS·Atlas를 사용하지 않았고 production Firebase API·Bean·dependency·feature flag를 추가하지 않았다
- TMI-90 구현 정리 요청에 따라 ADR·PoC 결과·실행 가능한 contract와 테스트·현재 worktree·Jira 상태를 다시 대조했다. 현재 산출물은 Stage 0 의사결정과 격리 검증이며 Spring Boot production Firebase adapter·entity·공개 API 구현으로 해석하지 않는다
- TMI-90 검증 결과의 증거 수준을 설명했다. local policy contract 7개 통과는 provider 허용·phone-only 거절·recent-auth·enrollment binding·Guest dual proof라는 자체 정책 함수가 명세대로 동작한다는 뜻이고, Auth Emulator 5개 통과는 email/password·Admin verify/revoke/delete·federated mapping·same-UID phone link·번호 충돌/cleanup이라는 Firebase 동작 가설을 로컬 모사 환경에서 확인했다는 뜻이다. Kakao discovery 1개는 공개 OIDC metadata 최소 조건만 확인했고 Java 317개는 기존 코드 회귀 부재를 확인했을 뿐 production Firebase 통합 검증이 아니다. 따라서 Stage 0 local PoC는 성공했지만 실제 Provider·모바일·SMS·Identity Platform과 Spring production 기능은 미검증·미구현이다
- TMI-90 구현 정리 turn의 Hook 지정 식별자를 WORKLOG EOF에 별도 기록하고 CURRENT_STATE를 동기화했다. 구현 코드·ADR·PoC·계획서와 Jira 상태는 추가로 변경하지 않았다
- GitHub PR #18의 `develop` 병합 commit `77804e63166c1c2cb5a60a9d1454085f4661144c`을 로컬 `develop`과 `origin/develop`에서 확인했다. Jira `TMI-90`은 아직 `해야 할 일`·Resolution 없음이며, 종료 댓글 초안과 사용 가능한 `완료` transition ID `41`을 사용자에게 사전 제시하고 별도 승인을 기다린다
- 사용자 승인에 따라 Jira `TMI-91` `[Identity] Firebase 인증 broker foundation 구축`을 TMI `작업` 유형으로 생성했다. 기본 우선순위 `Medium`, 기본 상태 `해야 할 일`, Resolution 없음, 담당자·라벨·컴포넌트 없음이며 댓글과 상태 전환은 수행하지 않았다
- TMI-91 범위는 disabled-by-default Firebase Admin adapter·안전한 설정, 목적별 Token 검증/recent-auth/오류 mapping, `FirebaseIdentity` unique mapping, `FirebaseEnrollmentAttempt` application 만료·CAS·PENDING partial unique·TTL cleanup과 외부 인프라 없는 테스트다. 공개 exchange/signup, `PhoneIdentity`, Guest 승격·merge, 실제 모바일 OAuth와 production flag 활성화는 제외했다
- TMI-91을 구현 전에 Atlassian 공식 MCP로 다시 읽어 설명·완료 조건·제외 범위가 AGENTS.md, ADR과 기존 JWT 계약에 충돌하지 않음을 확인했다. 구현 종료 시에도 Jira 상태는 `해야 할 일`, Resolution 없음이며 댓글·상태·필드는 변경하지 않았다
- Firebase Admin SDK `9.4.3`을 내부 infrastructure adapter 뒤에 추가했다. 전체 및 Google·Apple·Kakao·Phone feature flag는 모두 기본 `false`이고, 활성화 시 project·선택 tenant·ADC/Workload Identity credential·timeout을 fail-closed로 구성하며 같은 named FirebaseApp의 중복 초기화를 방지한다
- application에는 Firebase SDK 객체를 노출하지 않는 `FirebaseAuthenticationVerifier`, 목적 enum과 최소 `VerifiedFirebasePrincipal`·`VerifiedSocialPrincipal`만 추가했다. Admin adapter는 revoke/disabled user, project issuer·audience·tenant, 시간 Claim·recent-auth, provider allowlist와 phone/email enrollment proof를 확인하고 Google·Apple·Kakao provider subject만 최소 결과로 변환한다. raw Firebase ID Token·전체 Claim·email·phone과 SDK 원문 오류는 저장·반환·로그하지 않는다
- `FirebaseIdentity`는 `(firebaseProjectId, firebaseUid)`와 canonical `userId`를 각각 unique로 매핑한다. `FirebaseEnrollmentAttempt`는 DIRECT_SIGNUP/GUEST_USER binding, application `expiresAt`, cleanup TTL, status PENDING partial unique를 가지며 active attempt 재사용·만료 CAS·duplicate insert winner 재조회와 project·UID·binding 전체 일치 조건의 consume CAS를 제공한다
- Firebase 설정·adapter·Provider mapping·오류 변환·민감정보 redaction, entity 불변식, Repository unique/index·만료/consume CAS와 enrollment 동시성 서비스를 외부 Firebase·Atlas 없이 검증했다. 최종 `./gradlew clean test`는 54개 suite·353개 테스트가 skip·failure·error 0으로 성공했고 공개 Firebase Controller나 기존 JWT/RefreshSession 변경은 추가하지 않았다
- GitHub PR #19 `feat(TMI-91): build Firebase auth broker foundation`이 `develop`에 merge commit `e25adbcec9ffb4c5bd3c5c432a7029fc1e77e8ff`로 병합된 것을 확인했다. 사용자 승인으로 Jira TMI-91에 구현·테스트·남은 위험을 기록한 댓글 ID `10004`를 등록하고 transition ID `41`을 적용했으며, 후속 조회에서 상태와 Resolution이 모두 `완료`임을 확인했다
- 사용자 승인에 따라 Jira `TMI-92` `[Identity] PhoneIdentity 및 versioned HMAC fingerprint 기반 구축`을 TMI `작업` 유형과 우선순위 `Medium`으로 생성했다. 후속 조회에서 승인된 설명·완료 조건·제외 범위, 기본 상태 `해야 할 일`, Resolution 없음, 담당자·라벨·컴포넌트 없음임을 확인했으며 댓글이나 상태 전환은 수행하지 않았다
- TMI-92 범위로 formatting을 허용하되 결과를 strict E.164로 재검증하는 `PhoneNumberNormalizer`, 정확히 하나의 `ACTIVE_WRITE`와 0..N `LOOKUP_ONLY`를 강제하는 외부 key ring 설정, `tosunsaeng:identity:phone-identity:v1` domain과 NUL separator를 사용하는 HMAC-SHA-256 fingerprint 기반을 구현했다. 기능과 key ring은 기본 비활성이며 실제 key material은 저장소에 추가하지 않았다
- `PhoneIdentity`와 retained version별 `PhoneFingerprintAlias`를 추가했다. ACTIVE User당 identity 하나, ACTIVE `(keyVersion, fingerprint)`와 `(phoneIdentityId, keyVersion)` uniqueness, user/status lookup index, PhoneIdentity `@Version`, ACTIVE/RELEASED lifecycle을 선언하고 raw phone·last4·Firebase UID를 영속 모델에 넣지 않았다
- `PhoneIdentityService`와 Mongo Transaction 경계를 분리해 신규 claim, 동일 User 재시도 멱등 성공, 이전 alias로 소유권을 확인한 rotation backfill, 번호 교체 시 기존 identity·aliases release 후 신규 claim, 다른 User 충돌의 `PHONE_ALREADY_LINKED`, duplicate/optimistic 경쟁 재시도를 구현했다. phone은 자동 merge·로그인 key로 사용하지 않는다
- E.164·key registry·domain separation·redaction, entity lifecycle, 실제 Spring Data Repository proxy와 index metadata, version 교차 candidate 조회, 동시 alias claim, 서비스 멱등·rotation·replacement·충돌, disabled/enabled 설정을 외부 Firebase·SMS·Atlas 없이 검증했다. 최종 `./gradlew clean test`는 62개 suite·381개 테스트가 skip·failure·error 0으로 성공했다
- `UserAccountType(GUEST, MEMBER)`과 `User.accountType`을 추가했다. 신규 LOCAL은 `MEMBER + LOCAL`, 신규 Guest는 `GUEST + GUEST`를 함께 저장하고, accountType이 없는 기존 문서는 `provider=GUEST → GUEST`, `LOCAL/null → MEMBER`로 호환 해석한다. accountType이 존재하면 legacy provider보다 우선한다
- User에 `isGuest()`, `isMember()`, `hasLocalCredential()` 의미 기반 판단을 추가했다. MEMBER 자체는 passwordHash나 SocialIdentity 존재를 강제하지 않고 LOCAL factory만 세 local credential 필드를 필수로 유지해 향후 social-only MEMBER를 수용한다
- 프로필 응답에 `accountType`을 추가하고 기존 `provider`는 OpenAPI deprecated 하위 호환 필드로 유지했다. 회원 탈퇴는 Guest 여부와 LOCAL credential 보유 여부를 분리해 Guest는 비밀번호 없이 기존 계약을 유지하고 LOCAL credential MEMBER만 비밀번호를 검증하며 social-only MEMBER는 LOCAL 비밀번호 경로로 오분류하지 않는다
- 회원가입·로그인에서는 로그인 수단용 provider와 accountType을 구분해 기록하고, Guest 생성·동의·탈퇴처럼 계정 유형만 필요한 로그는 accountType을 사용한다. 민감정보·Token·Hash·요청 본문은 추가하지 않았다
- 신규 accountType dual write, accountType 우선순위, legacy GUEST·LOCAL·null fallback, social-only MEMBER, 프로필·OpenAPI 호환과 기존 signup·Guest·login·RefreshSession·withdrawal 회귀를 검증했으며 `./gradlew clean test` 전체 45개 suite·317개 테스트가 skip·failure·error 0으로 성공했다
- 사용자 승인에 따라 Jira `TMI-93` `[Identity] Auth 패키지 capability-first vertical slice 리팩터링`을 TMI `작업` 유형과 우선순위 `Medium`으로 생성했다. 후속 조회에서 승인된 설명·완료 조건·제외 범위, 기본 상태 `해야 할 일`, Resolution 없음, 담당자 없음과 빈 라벨을 확인했으며 댓글이나 상태 전환은 수행하지 않았다
- TMI-93 범위로 Auth 운영 코드와 테스트를 `common`, `registration`, `local`, `session`, `federation`, `phoneidentity` capability 아래로 재배치했다. HTTP endpoint·DTO JSON·오류 코드·설정 이름은 변경하지 않았고 공통 Auth Controller는 여러 capability를 조합하는 transport 경계로 `common.api`에 유지했다
- Mongo `_class`에 기록될 수 있는 `FirebaseEnrollmentAttempt`, `FirebaseIdentity`, `PhoneFingerprintAlias`, `PhoneIdentity`, `RefreshSession`, `SocialIdentity`의 FQCN은 기존 `domain.auth.domain.entity`에 유지했다. package 문서와 실제 `MappingMongoConverter` write/read 회귀 테스트로 legacy 판별자 호환을 고정했다
- Firebase·Social Repository를 `federation.repository`, RefreshSession Repository를 `session.repository`, PhoneIdentity Repository와 custom fragment 구현을 `phoneidentity.repository`로 함께 이동했다. 실제 Spring `@EnableMongoRepositories` 스캔으로 FirebaseEnrollmentAttempt·PhoneFingerprintAlias custom fragment와 RefreshSession Repository 자동 wiring을 검증했다
- registration이 session의 prepared 발급 계약을 사용할 수 있도록 redacted `PreparedRefreshSession`과 `RefreshSessionIssuer.prepare/savePrepared`의 가시성을 slice 간 최소 public 계약으로 조정했다. Guest 등록의 named Mongo Transaction과 rollback 테스트는 유지했다
- TMI-93 타깃 호환·Repository·Transaction 테스트와 최종 `./gradlew clean test` 전체 64개 suite·384개 테스트가 failure·error·skip 없이 성공했다
- TMI-93 재배치 뒤 남은 빈 테스트 디렉터리 `domain/auth/local/application`, `user/controller`, `user/service`와 비어 버린 상위 디렉터리를 제거했다. `.git`·`.gradle`·`build`·`.idea`는 정리 대상에서 제외했고 저장소 소스·문서 영역에 빈 디렉터리가 남지 않았음을 확인했다
- 빈 디렉터리 정리 turn의 Hook 지정 식별자를 WORKLOG EOF에 별도 기록하고 CURRENT_STATE를 동기화했다. 소스 구조와 테스트 결과는 추가로 변경하지 않았다
- GitHub PR #21 `refactor(TMI-93): reorganize auth package by capability`가 merge commit `063fdc7`로 `develop`에 병합된 것을 확인했다. 사용자 요청에 따라 Jira TMI-93에 transition ID `41`만 적용했고 후속 조회에서 상태와 Resolution이 모두 `완료`임을 확인했다. Jira 댓글과 다른 필드는 변경하지 않았다
- Stage 5A `POST /api/v1/auth/firebase/exchange`를 공개 route로 구현했다. enabled 경로는 Firebase credential을 `LOGIN_EXCHANGE`로 검증하고 기존 FirebaseIdentity의 ACTIVE MEMBER owner에만 자체 RS256 Access Token과 hash-only Opaque RefreshSession을 발급한다. Firebase credential·UID·project·provider subject는 응답이나 내부 JWT에 포함하지 않는다
- 미등록 Firebase UID는 User·PhoneIdentity·SocialIdentity·RefreshSession을 생성하지 않고 기존 `FirebaseEnrollmentAttemptService.startOrReuse`로 DIRECT_SIGNUP attempt를 생성 또는 재사용한다. password email 미검증, verified phone 부재, profile, consents를 명시적 missing requirement enum으로 반환하고 원래 attempt 만료 시각 기준 milliseconds `expiresIn`을 계산한다
- exchange 응답은 `AUTHENTICATED`와 `ENROLLMENT_REQUIRED`의 명시적 sealed union으로 고정했다. 기존 SocialIdentity owner 불일치는 자동 rebind·merge 없이 `SOCIAL_IDENTITY_CONFLICT`, 깨진 FirebaseIdentity owner mapping은 `FIREBASE_IDENTITY_CONFLICT`로 fail-closed 처리한다. disabled 기본값에서는 controller route를 유지하되 verifier·Repository·Session을 wiring하거나 호출하지 않고 안정적인 `503 FIREBASE_UNAVAILABLE`을 반환한다
- Firebase exchange 요청의 credential은 OpenAPI `writeOnly`·required로 표시하고 request·token response 문자열 표현에서 redaction한다. OpenAPI 200 응답은 공통 envelope의 result가 두 결과 schema를 `oneOf`로 참조하며 401·403·409·429·503을 문서화한다. Security 공개 경로와 안전한 route template logging 목록도 함께 갱신했다
- Stage 5A application·controller·configuration·Security 테스트와 최종 `./gradlew clean test` 전체 66개 suite·399개 테스트가 failure·error·skip 없이 성공했다. 실제 Firebase·Atlas는 호출하지 않았고 기능과 모든 provider flag는 test/local 기본 비활성 상태를 유지했다
- Atlassian 공식 MCP로 Firebase 관련 Jira를 읽기 전용 재조회한 결과 Stage 5A 전용 이슈는 없고 TMI-93은 이미 완료, 별도 범위인 TMI-92 PhoneIdentity와 TMI-90 ADR·PoC만 `해야 할 일` 상태임을 확인했다. 두 이슈 모두 완료 transition ID `41`을 사용할 수 있지만 사용자가 종료 대상을 특정하기 전에는 Jira를 변경하지 않는다
- Atlassian 공식 MCP로 Stage 5B signup finalize·outbox 관련 중복 Jira가 없음을 확인하고 TMI `작업` 유형 ID `10003`, 생성 필드 20개, `High` 우선순위 ID `2`를 읽기 전용 재확인했다. 정확한 제목·설명·완료 조건·제외 범위 payload를 사용자에게 제시하기 전에는 이슈를 생성하지 않는다
- 현재 Atlassian 계정이 TMI-90과 TMI-92의 reporter임을 확인하고, 저장소의 PR #18·#20 병합 commit을 대조한 뒤 사용자 승인에 따라 두 이슈에 완료 transition ID `41`만 적용했다. 후속 조회에서 두 이슈의 상태와 Resolution이 모두 `완료`임을 확인했고 댓글과 다른 필드는 변경하지 않았다
- 승인된 Stage 5B payload로 Jira `TMI-94` `[Identity] Stage 5B Firebase 신규 MEMBER signup finalize Transaction`을 TMI `작업`, 우선순위 `High`, 기본 상태 `해야 할 일`로 생성했다. 담당자·라벨·컴포넌트는 없고 Resolution도 없으며 설명·완료 조건·제외 범위가 승인안대로 저장됐음을 재조회했다
- TMI-90·TMI-92 완료 전환과 TMI-94 생성 turn의 Hook 지정 식별자를 WORKLOG EOF에 별도 기록하고 CURRENT_STATE를 동기화했다. Jira·애플리케이션 코드·테스트 결과는 추가로 변경하지 않았다
- TMI-94로 공개 `POST /api/v1/auth/firebase/signup`을 구현했다. 요청은 enrollmentId·fresh write-only Firebase credential·nickname·현재 개인정보/약관 동의만 받으며 외부 userId·phone은 받거나 신뢰하지 않는다. Firebase Admin UserRecord의 동일 UID verified phone만 redacted 최소 principal을 통해 메모리 경계로 전달한다
- signup은 fresh `DIRECT_ENROLLMENT` proof와 PENDING·미만료 DIRECT_SIGNUP attempt의 project·UID를 대조하고 primary credential·verified phone·nickname·필수 동의를 검증한다. 순차 duplicate finalize는 Session을 준비하기 전에 고정 `FIREBASE_ENROLLMENT_CONFLICT` 409로 거절하고 exchange 재진입을 안내한다
- canonical UUID `FEDERATED` MEMBER User, FirebaseIdentity, PhoneIdentity와 retained aliases, SocialIdentity 0..N, generic PhoneEligibilityBindingOutbox, prepared RefreshSession 저장과 attempt consume CAS를 `mongoTransactionManager` Transaction 하나로 처리한다. Access Token은 commit 반환 뒤 canonical UUID userId로 발급하며 Firebase UID를 JWT나 응답에 넣지 않는다
- eligibility fingerprint는 PhoneIdentity와 별도 key ring·domain separator·opaque consumer scope로 파생하고 raw phone·fingerprint·key material을 저장·응답·로그·문자열 표현에 노출하지 않는다. outbox는 반복 phone 변경 이벤트를 허용하고 publisher용 status+createdAt 및 user+scope+createdAt index를 갖는다
- Firebase·Social·phone owner 충돌은 자동 rebind·merge 없이 고정 conflict로 분류한다. outbox·SocialIdentity·RefreshSession 저장 또는 enrollment consume 실패는 모두 Transaction rollback하며 실제 Firebase/provider와 두 phone fingerprint 기능은 계속 기본 비활성이다
- Stage 5B application·Transaction·adapter·domain·configuration·controller·Security/OpenAPI·Repository 테스트를 추가했다. 최종 전체 테스트는 421개 기준으로 성공했고 실제 Firebase·Atlas·외부 OAuth·Entitlement consumer는 호출하지 않았다
- GitHub PR #22 `feat(TMI-94): implement Firebase member signup finalize transaction`이 merge commit `455db00`으로 `develop`에 병합됐고 feature commit `f68d7e8`이 이력에 포함된 것을 확인했다. 승인된 구현 요약·테스트·잔여 위험 댓글 ID `10005`를 TMI-94에 등록하고 transition ID `41`만 적용했으며 후속 조회에서 상태와 Resolution이 모두 `완료`임을 확인했다
- 승인된 Stage 5C 서버 간 계약 ADR Payload로 Jira `TMI-95` `[Identity] Stage 5C Phone eligibility binding 서버 간 계약 ADR`을 TMI `작업`, 우선순위 `High`, 기본 상태 `해야 할 일`로 생성했다. 담당자·라벨·컴포넌트는 없고 Resolution도 없으며 댓글과 상태 전환은 적용하지 않았다
- Stage 5C 서버 간 계약 ADR의 권장 내용을 구체화했다. Identity는 server-configured opaque consumer scope와 별도 HMAC key/domain으로 verified phone의 retained candidate를 파생해 versioned `PhoneEligibilityBindingVerified` event만 전달하고, 외부 consumer는 eventId inbox unique와 binding 갱신을 같은 로컬 Transaction으로 처리한다. 중복 delivery는 성공 no-op, 같은 eventId의 다른 payload는 poison event로 격리하며 가입 이벤트만으로 TrialClaim이나 혜택을 지급하지 않는다
- 권장 event envelope는 eventId, eventType, schemaVersion, producer, occurredAt, consumerScopeId, canonical userId, verifiedAt, user+scope별 단조 증가 bindingRevision과 순서 의미가 없는 fingerprintCandidates(keyVersion, value)를 포함한다. raw phone·last4·Firebase UID·provider subject·PhoneIdentity fingerprint와 HMAC key material은 payload·로그·metric·오류에 포함하지 않는다
- `schemaVersion`·`keyVersion`·`bindingRevision`을 서로 독립된 버전으로 설명했다. `eventType+schemaVersion`은 소비자가 JSON 송장 양식을 해석할 수 있는지 판단하는 계약 식별자이고, `keyVersion`은 각 eligibility candidate를 어느 HMAC key로 생성했는지 알려주는 조회용 표식이며, `bindingRevision`은 같은 user+consumer scope의 phone binding이 몇 번째 상태 변경인지 나타내 역순 도착 이벤트가 최신 상태를 덮지 못하게 한다. 선택 필드 추가는 호환 유지할 수 있지만 삭제·타입·의미 변경은 새 schemaVersion과 구버전 병행이 필요하고 unknown version은 추측 처리하지 않는다
- Stage 5C 서버 간 계약 ADR 전체 목적을 사용자 흐름으로 설명했다. 가입 Transaction이 verified phone의 consumer 전용 fingerprint candidate와 PENDING outbox를 함께 저장하고, 추후 publisher가 lease로 한 건을 점유해 versioned event를 at-least-once 전달한다. 외부 Entitlement/Billing consumer는 eventId inbox와 최신 binding을 같은 로컬 Transaction으로 저장한 뒤에만 성공 응답하며, 중복은 no-op·역순은 bindingRevision으로 무시·일시 실패는 retry/backoff·영구 실패는 dead-letter 처리한다. 이 event는 혜택 지급이 아니라 first free-trial 시 TrialClaim 조회에 쓸 준비 정보이며 outbox 미도착은 fail-closed다
- eligibility key rotation은 새 version을 retained lookup candidate로 먼저 배포하고 writer 동기화 뒤 active write를 전환하며 이전 version은 lookup-only로 유지한다. raw phone을 저장하지 않으므로 기존 old-only binding을 임의 backfill할 수 없고, consumer reference가 만료되거나 재인증으로 새 candidate가 생기기 전에는 legacy version과 key를 제거하지 않는다. producer outbox와 consumer binding/claim의 보존·탈퇴 삭제는 서로 다른 목적·기간으로 ADR에서 명시해야 한다
- 계획서의 Entitlement/Billing은 Learning Core 자체를 뜻하지 않고 TrialClaim·UserEntitlement·EntitlementReservation을 소유하는 별도 논리적 bounded context다. Learning Core는 사용권을 reserve한 뒤 exam 생성과 confirm을 조율한다. 장기 권장안은 별도 서비스·저장소지만, 초기 배포를 Learning Core 애플리케이션 내부 독립 모듈로 시작할지는 TMI-95 ADR과 외부 저장소 설계에서 확정해야 하며 동일 배포물이어도 데이터·Transaction·패키지·API 경계를 분리한다
- 현재 Identity 작업은 Entitlement/Billing 구현이 이미 존재한다고 전제하지 않는다. 외부 별도 저장소에서 consumer가 구현될 것을 목표 구조로 두고 Identity가 verified phone candidate와 outbox를 안전하게 생성·전달할 producer 계약을 먼저 준비한다. consumer와 staging end-to-end 전달이 준비되기 전에는 production signup eligibility 연동을 활성화하지 않는다
- 현재 데이터 모델을 역할별로 구분하면 계정 root는 `User`와 embedded `UserConsents`, 인증수단 연결은 `FirebaseIdentity`·`SocialIdentity`·`PhoneIdentity`·`PhoneFingerprintAlias`, 로그인 지속은 `RefreshSession`, 가입 전 단기 절차는 `FirebaseEnrollmentAttempt`, 외부 eligibility 전달·운영은 `PhoneEligibilityBindingOutbox`·`PhoneEligibilityBindingRevision`·`PhoneEligibilityBindingDeliveryScopeState`다. `PreparedRefreshSession`·`IssuedRefreshSession`·`IssuedAccessToken`은 DB entity가 아니라 처리 중 결과 객체다
- Identity의 이후 계획 후보는 Guest merge용 `UserMergedOutbox`와 탈퇴·Firebase cleanup용 lifecycle outbox/saga 상태이며 아직 모두 확정·구현된 entity는 아니다. 별도 Entitlement/Billing 저장소는 inbox·revision high-water와 `VerifiedPhoneBenefitBinding`·`TrialClaim`·`UserEntitlement`·`EntitlementReservation`을 소유하고, Learning Core는 시험 entity만 소유한다
- Phone eligibility 용어는 다음처럼 구분한다. fingerprint는 원문 phone을 목적별 HMAC으로 바꾼 비교용 가명값, candidate는 Entitlement가 비교할 수 있도록 만든 eligibility 전용 fingerprint 한 개, `fingerprintCandidates`는 key rotation 동안 retained key version별 candidate를 모두 담은 현재 binding의 완전한 집합이다. `keyVersion`은 candidate 생성 key 판본, `consumerScopeId`는 어느 consumer 목적용인지 나타내는 서버 설정의 opaque 식별자, `bindingRevision`은 user+scope 상태 변경 순번이다
- 전달 용어에서 outbox는 producer 발송함, publisher는 outbox 전달 작업자, inbox는 consumer 수신 장부, current binding은 최신 검증 연결, high-water mark는 이미 처리한 최고 revision이다. at-least-once는 중복 가능 전달, eventId 멱등성은 같은 event 중복 무효화, lease는 worker의 임시 점유권, retry/backoff는 재시도와 간격 증가, dead-letter는 자동 재시도를 중단한 격리 상태, fail-closed는 확인 불가 시 혜택을 허용하지 않는 정책이다
- 선택적 quality review 동의 추가안은 필수 privacy·terms와 다른 정책이 필요하다. 동의 `true`는 현재 quality-review version 일치를 요구하지만 철회 `false`는 stale client version 때문에 거절하지 않아야 하며, 저장 snapshot은 false일 때 consentedVersion·consentedAt을 null로 만들고 별도 changedAt 또는 append-only audit로 철회 시점을 남기는 방향을 권장한다. 현재 `ConsentPolicyStatusResponse`에는 이미 `consented`가 있고 기존 계산은 false를 `requiresConsent=true`로 만들기 때문에 optional 전용 factory/DTO 또는 `required` flag가 필요하다
- quality review가 시험 답안·음성의 사람 검토나 품질 개선 이용을 제어한다면 Identity snapshot 변경만으로 철회가 완료되지 않는다. 실제 데이터를 소유한 Learning Core·저장 서비스로 versioned consent changed/revoked event를 전달하고 이후 이용 중지·보존/삭제 정책을 별도 계약으로 구현해야 한다. Guest 외 LOCAL signup·Firebase signup 등 모든 생성 경로와 PUT 성공 응답·profile 노출 범위도 함께 결정해야 한다
- quality review 프론트 계약은 Guest·consent PUT에 선택 boolean과 정책 version을 보내고 GET의 `qualityReview` 상태를 표시하며 false로 철회하는 additive 변경이다. 다만 새 request 필드를 즉시 `@NotNull`로 강제하면 구버전 client가 400이 되므로, 안전한 rollout은 backend가 누락을 false로 처리하는 호환 기간을 먼저 배포하고 frontend 전환 후 필수화 여부를 재결정하는 순서다
- 이 저장소의 현재 통합 기준은 `develop` `31130fd`이며 `main` `b6eb73e`는 develop의 ancestor로 16개 commit 뒤에 있다. TMI-96 작업 트리에는 아직 커밋되지 않은 다수 변경이 있으므로 main으로 직접 checkout해 새 기능을 섞지 않는다. 사용자가 TMI-96 변경을 먼저 commit·push한 뒤 최신 develop에서 별도 feature/hotfix branch를 만들어 quality review 변경을 진행하는 것이 안전하다
- `docs/contracts/quality-review-consent-implementation-plan.md`에 외부 필드명 `isQualityReviewConsented`·`qualityReviewConsentVersion`과 GET `qualityReview` 구조를 고정한 구현 계획을 추가했다. true만 current version 일치를 요구하고 false 철회는 stale version으로 차단하지 않으며, false 저장은 version/time null, 반복 요청 멱등, 기존 client 누락=false 호환, 외부 데이터 소유 서비스의 철회 consumer 준비 전 실제 품질 검토 비활성을 기준으로 한다
- 사용자가 quality review 변경만 main에 즉시 반영하고 develop은 main에 포함하지 않는다고 배포 경계를 확정했다. `main` `b6eb73e`에도 Guest·LOCAL signup과 consent API 기반이 있으므로 `origin/main`에서 별도 `hotfix/quality-review-consent` worktree를 만들고 main 패키지 구조에서 지정 endpoint만 수정한다. PR base는 main이며 TMI-95·TMI-96·Firebase·refactor 등 develop 전용 diff가 0인지 검증한다
- 별도 worktree `/Users/msde76/identity-quality-review-hotfix`가 생성됐고 `hotfix/quality-review-consent` branch가 `origin/main`의 `b6eb73e`에 checkout된 것을 확인했다. 기존 `/Users/msde76/identity`는 `feat/TMI-96-phone-eligibility-outbox-publisher`와 미커밋 변경을 그대로 유지하므로 여기서 branch switch하지 않고 hotfix 폴더로 이동해 작업한다
- 사용자가 hotfix worktree에서 직접 `git branch --show-current`와 `git status --short`를 실행해 `hotfix/quality-review-consent`와 clean status를 확인했다. 터미널·IDE 작업 경로는 `/Users/msde76/identity-quality-review-hotfix`를 사용하고 기존 Identity worktree와 계속 분리한다
- 별도 hotfix worktree에서 Quality review 구현 후 커밋 전 재검증을 완료했다. 변경은 관련 API·도메인·설정·문서·테스트에 한정되고 develop의 TMI-95·TMI-96 파일은 없으며 `./gradlew clean test` 42개 suite·319개 테스트와 `git diff --check`가 성공했다. 아직 staged·commit·push는 사용자가 수행하기 전 상태다
- 사용자가 Quality review hotfix를 commit `4745652`로 생성해 `origin/hotfix/quality-review-consent`에 push했고 worktree는 clean하다. 배포 환경에는 필수 비밀이 아닌 정책 식별자 `QUALITY_REVIEW_CONSENT_VERSION=quality-review-v1`을 추가해야 하며 frontend가 보내는 version과 정확히 일치해야 한다. 설정 누락·공백은 기동 실패이고 test profile은 repository의 가짜 test value를 사용한다
- Staging frontend의 `EXPO_PUBLIC_IDENTITY_API_BASE_URL`은 기존 Identity staging domain, `EXPO_PUBLIC_LEARNING_API_BASE_URL`은 기존 Learning staging domain을 계속 사용한다. Quality review hotfix에는 별도 점검 서버 endpoint나 외부 HTTP 호출이 없으므로 새 공개 domain을 만들지 않는다. Identity staging runtime에 URL이 아닌 `QUALITY_REVIEW_CONSENT_VERSION=quality-review-v1`만 추가한다
- TMI-95로 `ADR-002-phone-eligibility-binding-server-contract.md`를 작성해 Entitlement/Billing을 별도 bounded context·배포 서비스의 consumer owner로 확정했다. wire schema v1은 verified/revoked state event, eventId immutable payload 멱등성, user+scope 단조 revision, opaque scope와 Identity 소유 HMAC key, HTTPS push·workload identity, publisher lease/retry/dead-letter, 목적별 30/90/120일 보존과 민감정보 금지를 정의한다
- ADR-002 작성 시 확인했던 producer의 eventType·schemaVersion·bindingRevision, revoke와 lease/retry/dead-letter publisher 공백은 TMI-96에서 구현했다. consumer inbox/high-water Transaction과 staging E2E는 계속 별도 production gate다
- GitHub PR #23 `docs(TMI-95): define phone eligibility binding contract ADR`이 merge commit `31130fd`로 `develop`에 병합됐고 feature commit `cfefbec`이 이력에 포함된 것을 확인했다. Jira TMI-95의 댓글 없음·상태 `해야 할 일`·Resolution 없음과 완료 transition ID `41` 사용 가능 여부를 읽기 전용 확인했으며, 승인 전에는 Jira를 변경하지 않았다
- 사용자 승인에 따라 TMI-95에 ADR 결정·변경 문서·70개 suite·421개 테스트와 잔여 publisher/consumer 위험을 담은 종료 댓글 ID `10006`을 등록하고 완료 transition ID `41`만 적용했다. 후속 조회에서 상태 ID `10003`과 Resolution이 모두 `완료`임을 확인했다
- 승인된 Payload로 Jira `TMI-96` `[Identity] Stage 5D Phone eligibility binding outbox publisher`를 TMI `작업`, 우선순위 `High`, 기본 상태 `해야 할 일`로 생성했다. 담당자·라벨·컴포넌트는 없고 Resolution도 없으며 댓글과 상태 전환은 적용하지 않았다
- TMI-96에서 ADR-002 schema v1 verified/revoked wire event, `(userId, consumerScopeId)`별 atomic `bindingRevision`, 가입 verified와 전화 교체·회원 탈퇴 revoke를 각 lifecycle Mongo Transaction에 연결했다
- outbox는 `PENDING`·`IN_FLIGHT`·`PUBLISHED`·`DEAD_LETTER`, 60초 atomic lease, 만료 lease reclaim, 지수 backoff와 ±20% jitter, 최대 12회, HTTP/전송 실패 분류, 401/403 scope pause, 동일 event 수동 replay를 지원한다
- HTTPS-only JDK delivery adapter와 최대 5분 workload identity credential provider port를 추가했다. redirect와 response body 보관을 금지하고 publisher·Firebase·phone eligibility 기능은 기본 비활성, 설정 누락은 fail-closed로 유지한다
- PUBLISHED event는 cleanupAt TTL·scheduler로 30일 뒤 정리하고 DEAD_LETTER는 90일 review 시각만 기록해 자동 삭제하지 않는다. metric은 event type·schema·outcome·failure code의 제한된 tag만 사용하며 candidate·userId·eventId·credential은 포함하지 않는다
- TMI-96 구현 후 `./gradlew clean test` 73개 suite·433개 테스트, failure 0·error 0·skipped 0과 `git diff --check`를 통과했다. 실제 consumer·workload identity 발급 인프라·staging E2E는 구현하지 않았고 Jira 댓글·상태도 변경하지 않았다
- GitHub PR #25가 merge commit `6f02f4a`로 TMI-96 구현을 `develop`에 반영했고, Quality review 역병합 PR #26도 merge commit `24275a5`로 반영됐다. 통합 기준 전체 457개 테스트가 성공했으며 `git merge-base --is-ancestor origin/main origin/develop`도 종료 코드 0이다
- 사용자 최종 승인에 따라 TMI-96에 구현·테스트·남은 consumer/workload identity/staging 위험을 담은 종료 댓글 ID `10007`을 등록하고 transition ID `41`만 적용했다. 후속 조회에서 상태 ID `10003`과 Resolution이 모두 `완료`임을 확인했다
- 승인된 Stage 6 Payload로 Jira `TMI-97` `[Identity] Stage 6 Guest MEMBER 승격 및 인증수단 동기화`를 TMI `작업`, 우선순위 `High`, 기본 상태 `해야 할 일`로 생성했다. 담당자·라벨·컴포넌트는 없고 Resolution도 없으며 설명·완료 조건·제외 범위가 승인안대로 저장됐음을 재조회했다
- TMI-97 구현 전에 Jira·ADR-001·social login 계획과 코드를 대조했다. 계획의 보호 API는 `/api/v1/auth/firebase/guest/prepare`, `/api/v1/auth/firebase/guest/upgrade`, `/api/v1/auth/firebase/auth-methods/sync`이며 Guest JWT `sub`를 source로 고정하고, 당시 Guest 전용 API/service·in-place 승격·전용 Session 폐기·`MERGE_REQUIRED`·MEMBER sync 공백을 확인했다
- TMI-97에서 Bearer 보호 API `/api/v1/auth/firebase/guest/prepare`, `/guest/upgrade`, `/auth-methods/sync`를 추가했다. Request Body userId 없이 JWT `sub`를 사용하며 prepare는 phone link 전 fresh proof로 Guest-bound attempt를 만들고, upgrade는 verified phone·동의·owner를 재검증한다
- Guest upgrade는 기존 UUID를 유지한 User CAS 승격, guest installation credential 제거, FirebaseIdentity·PhoneIdentity/alias·SocialIdentity·eligibility revision/outbox, 기존 Session의 `GUEST_UPGRADED` 폐기, 신규 RefreshSession과 attempt CAS consume을 하나의 Mongo Transaction으로 처리한다. 다른 ACTIVE MEMBER owner는 mixed Firebase/Social 소유 조합도 mutation 없이 `MERGE_REQUIRED`로 분류한다
- MEMBER auth-method sync는 JWT User와 기존 FirebaseIdentity UID 및 fresh Firebase proof를 대조해 누락 SocialIdentity만 멱등 추가하고, 다른 User owner는 `SOCIAL_IDENTITY_CONFLICT`로 거절한다. Firebase·provider 기능은 기본 비활성이고 실제 Guest merge·UserMergedOutbox는 Stage 7로 남겼다
- TMI-97 도메인·service·Transaction·Controller·Security·OpenAPI·설정 테스트를 보강했으며 `./gradlew clean test` 전체 479개 테스트와 `git diff --check`, 신규 경계의 명시적 로그 호출 부재 검사가 성공했다
- GitHub PR #27이 merge commit `5801868`로 TMI-97 구현을 `develop`에 반영했다. 사용자 승인에 따라 Jira TMI-97에 구현·변경 범위·479개 테스트·staging 잔여 위험을 담은 종료 댓글 ID `10008`을 등록하고 transition ID `41`만 적용했으며, 후속 조회에서 상태와 Resolution이 모두 `완료`임을 확인했다
- 다음 Identity 범위를 social login 계획의 Stage 7 Guest source→MEMBER target merge, source token gate와 `UserMergedOutbox`로 정리했다. Atlassian 공식 MCP 검색에서 중복 Jira가 없음을 확인하고 TMI `작업`·High 우선순위의 제목·설명·완료 조건·제외 범위 초안을 준비했으며 승인 전에는 생성하지 않았다
- 사용자 승인에 따라 `[Identity] Stage 7 Guest canonical merge 및 UserMerged outbox`를 Jira `TMI-98`, TMI `작업`, High, 기본 상태 `해야 할 일`로 생성했다. 담당자·라벨·컴포넌트·기한·댓글·Resolution은 없고 별도 상태 전환도 적용하지 않았으며 저장된 본문과 필드를 후속 조회했다
- TMI-98은 TMI-97의 동일 UUID in-place 승격과 달리, 현재 Guest source와 이미 Firebase identity를 소유한 기존 MEMBER target이라는 두 User를 하나의 canonical MEMBER로 합치는 작업이다. Identity Transaction에는 source MERGED tombstone·credential 제거·Session 폐기, target Session, UserMerged outbox를 포함하고 실제 Learning Core 데이터 이전 consumer는 별도 범위로 유지한다
- 현재 `JwtCurrentUserProvider`는 검증된 JWT `sub`의 canonical UUID 형식만 확인하고 User 상태를 조회하지 않는다. TMI-98 source token gate는 merge 전에는 sub로 ACTIVE GUEST source를 식별하되 merge 후에는 같은 sub의 MERGED User를 `ACCOUNT_MERGED_TOKEN_REJECTED`로 거절해야 하며, source를 target actor alias로 변환해서는 안 된다
- 현재 RS256 JWT는 서명된 JWS이지 암호화된 JWE가 아니므로 token 보유자는 payload의 실제 UUID `sub`를 읽을 수 있다. 현 계약은 userId를 비밀 credential로 보지 않으며 권한은 서명·audience·상태·resource ownership 검사로 보호한다. client에게 내부 userId 자체를 숨겨야 한다는 제품·위협 모델이 확정되면 실제 userId 대신 외부용 opaque/pairwise subject를 도입하는 별도 계약 migration이 필요하다
- 사용자는 현재 UUID userId의 JWT sub 노출을 허용하기로 했다. Access Token payload는 `sub`, `iss`, `aud`, `iat`, `exp`, `jti`, 공백 구분 `scope`만 포함하고 이메일·닉네임·accountType·provider·Firebase UID·전화번호·동의 상태는 포함하지 않는다. JWS header의 `alg=RS256`, `kid`, `typ=JWT`는 payload claim이 아니다
- TMI-98에서 보호 API `POST /api/v1/auth/firebase/guest/merge`를 추가했다. Request Body는 write-only `firebaseIdToken`만 받으며 source/target userId, email, phone, nickname을 merge selector로 받지 않는다
- source User는 CAS로 `MERGED` tombstone이 되며 `mergedIntoUserId`·`mergedAt`을 저장하고 Guest installation credential을 제거한다. 모든 source RefreshSession은 `GUEST_MERGED`로 폐기하고 target RefreshSession과 schema v1 `UserMergedOutbox`를 같은 Mongo Transaction에 저장한 뒤 commit 이후 target Access Token만 발급한다
- Firebase UID와 linked SocialIdentity의 기존 owner 집합이 정확히 하나인 ACTIVE MEMBER일 때만 target으로 인정한다. owner 없음·source 자신·mixed owner·비활성·Guest target은 mutation 없이 충돌로 거절하고 credential·identity mapping은 source에서 target으로 이전하지 않는다
- `JwtCurrentUserProvider`는 검증된 JWT `sub`가 DB의 `MERGED` User인지 조회해 `ACCOUNT_MERGED_TOKEN_REJECTED`로 거절하며 source를 target actor alias로 변환하지 않는다. JWT claim 구성은 기존 최소 계약을 유지한다
- `UserMerged` publisher는 원자적 lease claim, 지수 backoff, dead-letter, replay와 at-least-once 전달을 구현하고 운영 메트릭에는 userId를 tag로 쓰지 않는다. merge 기능 `GUEST_MERGE_ENABLED`와 publisher `USER_MERGED_PUBLISHER_ENABLED`는 모두 기본 false다
- TMI-98 도메인·CAS·Transaction·API·Security/OpenAPI·publisher·설정 테스트를 추가하고 `./gradlew clean test` 전체 성공, `git diff --check`, 신규 merge 경계의 명시적 로그 호출·민감정보 노출 정적 검사를 통과했다
- GitHub PR #29 `feat/TMI-98-guest-canonical-merge`가 merge commit `91671ce`로 `develop`과 `origin/develop`에 반영됐고 구현 commit `b8df69f`가 이력에 포함된 것을 확인했다. 구현 당시 `./gradlew clean test` 전체 496개와 후속 `./gradlew test`가 성공했다
- 공식 Atlassian MCP로 TMI-98의 현재 상태와 완료 전환을 재조회하고 사용자 승인에 따라 transition ID `41`만 적용했다. 후속 조회에서 status ID `10003`과 Resolution ID `10000`이 모두 `완료`임을 확인했으며 댓글과 다른 필드는 변경하지 않았다
- TMI-98 다음 작업을 Learning Core의 `UserMerged` v1 멱등 consumer·source actor deny marker 구현, 양 서비스 staging E2E, consumer→publisher→merge feature 순차 활성화로 정리해 사용자에게 설명하고 현재 turn 작업 기록을 남겼다. Learning Core 소유 코드는 Identity 저장소에 추가하지 않는다
- Learning Core에 그대로 전달할 `docs/contracts/learning-core-user-merged-consumer-handoff.md`를 작성했다. 실제 Identity v1 wire payload·workload identity HTTPS·2xx/재시도/영구 실패 분류, eventId inbox·payload conflict, aggregate inventory·충돌 정책, source deny marker와 ownership migration Transaction, 테스트·관측·배포 순서를 하나의 구현 요청서로 고정했다
- 외부 리뷰의 6개 핵심 제안을 검토해 Learning Core 인계서를 보강했다. v1을 direct Transaction·commit 후 `204`로 고정하고 semantic field SHA-256 digest, 사용자 JWT와 분리된 workload identity 필수 프로파일, DB Transaction 안의 external cache invalidation intent, 모든 write가 CAS/touch하는 per-user ownership guard, 별도 선행 ownership inventory, delivery·processing·total lag metric과 구체적인 배포 gate를 추가했다

## 진행 중

- 현재 브랜치는 `develop`이며 HEAD와 `origin/develop`은 PR #33 merge commit `7fc92c6`이다. Codex는 commit·push를 수행하지 않는다
- 현재 진행 중인 Jira 이슈는 `TMI-114`다. 상태는 `해야 할 일`, Resolution 없음이며 계획서 기준 구현을 시작하기 전이다

## 다음 작업

- `TMI-114` 구현 전에 Jira 본문과 Stage 6 계획서를 읽고 lifecycle Entity·상태·index와 atomic Repository부터 구현한다. 이후 enrollment coordinator, finalize fencing, API gate, owner preflight, Firebase worker, retry·reconciliation, bounded capture 순서로 진행한다
- Stage 6은 `enrollmentTtl=PT10M`, abandoned cleanup grace `PT24H`, terminal lifecycle retention `P7D`, terminal attempt retention `PT24H`를 분리한다. nonterminal·reconciliation에는 cleanupAt을 두지 않고 FINALIZED·CLEANED에서만 terminal 기준 TTL을 설정한다
- Stage 1~5 production 활성화와 Stage 6 개발은 분리한다. 실제 Firebase/mobile, workload auth, Mongo replica set Transaction, multi-instance lease·duplicate·key overlap과 backfill 범위 staging E2E 전에는 withdrawal·publisher·backfill flag를 계속 false로 유지한다
- Transaction 지원 staging MongoDB에서 Stage 2 handoff→Stage 3 release, 실제 rollback·동시 worker write conflict, Firebase/Social/Phone unique 점유 해제와 같은 credential의 새 UUID 재가입을 E2E 검증한다. Stage 3 worker flag는 검증 전 false로 유지한다
- 별도 Learning Core Jira·저장소에서 `UserMerged` v1 수신 endpoint, eventId inbox, source ownership의 target 이전, source actor deny marker를 하나의 로컬 Transaction으로 구현한다. 중복 event는 멱등 성공하고 같은 eventId의 다른 payload는 충돌로 거절하며 source를 target authorization alias로 사용하지 않는다
- consumer 배포 후 staging에서 정상 전달, 중복·재시도, timeout/5xx, publisher lease 회수·dead-letter/replay, source 거절과 target 데이터 조회를 E2E 검증한다. 검증 뒤 consumer, Identity publisher, Guest merge 순서로 활성화하며 그 전에는 `GUEST_MERGE_ENABLED`와 `USER_MERGED_PUBLISHER_ENABLED`를 false로 유지한다
- Transaction 지원 staging MongoDB에서 source User·Session·target Session·outbox rollback, CAS/write conflict, lease 경쟁·TTL/index 생성과 성공 응답 유실 뒤 target `/exchange` 복구를 검증한다
- 격리 Firebase/mobile과 Transaction 지원 staging MongoDB에서 Guest prepare→same-UID phone link→upgrade, existing MEMBER owner `MERGE_REQUIRED`, concurrent upgrade CAS, outbox·Session·attempt rollback과 성공 응답 유실 뒤 `/exchange` 복구 흐름을 E2E 검증한다
- Firebase link는 Mongo Transaction 밖에서 먼저 일어나므로 중단 enrollment cleanup·resume 정책과 성공 후 재시도 UX를 확정한다. Guest source/target merge와 `UserMergedOutbox`는 Stage 7에서 구현됐으며 이제 Learning Core consumer와 staging E2E가 production 활성화의 선결 조건이다
- 별도 Entitlement/Billing Jira는 eventId inbox, canonical payload digest, current binding·revision high-water Transaction과 abuse ledger 보존을 구현한다. Identity Stage 6와 병렬 진행할 수 있지만 consumer와 staging E2E가 준비되기 전 production publisher/Firebase flag는 활성화하지 않는다
- staging에서 실제 HTTPS consumer mock과 workload identity provider, Mongo replica set Transaction·lease 경쟁·TTL/index 생성, publisher 장애·scope pause·manual replay 운영 절차를 검증한다. 이 검증과 외부 consumer 준비 전 production publisher/Firebase flag를 활성화하지 않는다
- Stage 5C에서도 Identity에는 TrialClaim·UserEntitlement·시험 코드를 추가하지 않는다. 실제 consumer 구현은 소유 서비스의 별도 Jira로 분리하고, 가입 성공만으로 혜택을 지급하지 않는 계약을 유지한다
- 격리 Firebase/mobile과 transaction 지원 staging MongoDB에서 same-UID phone link, 국내 SMS·quota/abuse, revoke·recent-auth, unique/write conflict, outbox/Session/consume 주입 rollback 및 운영 index 생성을 재검증한다
- production provider 활성화와 기존 password signup/login/check-email 종료는 Stage 5A·5B 코드와 분리한다. 격리 Firebase/mobile/SMS 검증, outbox consumer 계약, 운영 index와 mixed-writer gate가 준비되기 전에는 Firebase flag를 활성화하거나 legacy credential writer를 닫지 않는다
- `ADR-001`의 조건부 결정을 검토하고, 격리 Firebase project·Android/iOS test app으로 실제 email·Google·Apple redirect, 공식 SDK same-UID phone link, 국내 SMS·quota·abuse, Identity Platform Kakao 등록·billing·deep-link와 Apple revoke를 검증한다. 그 전에는 production Firebase 기능을 활성화하지 않는다
- Stage 4는 E.164 재검증, domain-separated HMAC-SHA-256, 정확히 하나의 ACTIVE_WRITE와 0..N LOOKUP_ONLY key version, `PhoneIdentity`·`PhoneFingerprintAlias`, User당 번호 하나와 version 교차 동일 번호 중복 차단 index, `PHONE_ALREADY_LINKED`·자동 merge 금지와 raw phone·fingerprint 비로그 테스트까지만 포함한다
- Firebase SMS·OTP 처리, 공개 exchange/signup, User·RefreshSession finalize Transaction, Guest 승격·merge, TrialClaim·Entitlement와 benefit fingerprint는 Stage 4에서 제외한다
- 실제 운영 회원 0건을 read-only로 재확인한 뒤 BCrypt import·dual verifier 없이 신규 credential writer를 Firebase로 전환하고, 기존 직접 email/password signup·login endpoint와 `passwordHash` writer의 비활성화·제거 순서를 확정한다
- TMI-92 운영 반영 전 실제 MongoDB에서 partial unique index의 ACTIVE→RELEASED 후 재삽입, Transaction rollback·write conflict, mixed writer 차단과 key rotation 배포 순서를 staging으로 재검증한다
- 단계 5부터 Firebase exchange와 Guest를 거치지 않는 신규 가입을 열고, 같은 Firebase UID의 verified phone·필수 동의와 enrollment attempt를 최종 Transaction에서 소비해 User·FirebaseIdentity·PhoneIdentity·SocialIdentity·RefreshSession을 확정한다
- Firebase email/password·SNS 가입 뒤 전화 인증은 별 Firebase user를 만들지 않고 현재 Firebase user에 phone credential을 명시적으로 link한다. link 전후 UID를 검증하고 collision을 자동 merge로 처리하지 않으며 중단 가입은 resume·unlink/delete cleanup 정책으로 관리한다
- Entitlement 단계 전 PhoneIdentity와 별도 key·domain의 benefit fingerprint, consumer-scoped binding outbox·멱등 수신과 outbox 미도착 시 fail-closed eligibility 계약을 서버 간 ADR로 확정한다
- TMI-89 운영 배포 전에 실제 User 문서의 `accountType`·`provider`와 email·normalizedEmail·passwordHash·guestInstallationIdHash 조합을 read-only aggregate로 확인한다. 이번 로컬 구현은 실제 Atlas를 조회하거나 backfill하지 않았다
- 운영 배포 전에 `social_identities`의 두 index가 실제 MongoDB에서 생성 가능한지 staging으로 확인하고, 운영 자동 index 생성 권한·기존 데이터 중복·무중단 index rollout 정책을 확정한다
- 6단계 CI·staging: 임시 trigger가 제거된 errors-only artifact에 enabled·배포 Secret DSN·환경·immutable release를 주입해 실제 handled 5xx 한 건, expected 4xx 0건, 민감정보 부재와 중복 여부를 검증한다. Source context를 사용할 때만 build 인증 값을 CI Secret으로 제공
- 로컬 시험에 사용한 IntelliJ 실행 설정의 임시 staging profile·smoke·Sentry diagnostic 환경변수는 저장소 밖 설정이므로 사용자가 제거한다. 저장소 설정은 Sentry disabled·local 기본값으로 복원돼 환경변수 미주입 시 event를 전송하지 않음
- 7단계 운영 활성화: tracing·profiling·Sentry Logs는 0/off로 시작하고 오류 수집만 점진 활성화하며 event volume·중복·민감정보를 확인한 뒤 alert와 sampling을 별도 승인으로 조정
- staging ECS 수집·표시 과정에서 애플리케이션 한글 `message`의 UTF-8 보존과 `event` 기반 기존 검색·대시보드·알림 쿼리 불변을 확인
- 운영 수집 전 MongoDB 드라이버 INFO가 계정 식별자와 클러스터 endpoint·topology를 출력하지 않도록 `org.mongodb.driver` logger를 WARN으로 제한할지 확정하고, 필요한 연결 진단 INFO는 로컬에서만 일시 활성화
- staging 로그 수집기에서 ECS JSON 파싱, requestId 검색, route template 보존, 민감정보 마스킹과 Servlet container·APM의 예상 밖 5xx 중복 기록 여부를 확인
- event·outcome·errorCode·provider 같은 낮은 cardinality 필드 기반 대시보드·메트릭·알림 임계값과 로그 보존 기간을 실제 운영 수집 경로에 맞춰 확정
- TMI-75의 `해야 할 일 → 완료` 전환 ID `41` 적용 내용을 확인한 뒤 사용자가 최종 승인하면 상태 전환만 수행하고 상태·Resolution을 재조회
- staging replica set에서 회원 탈퇴 User/Session 실제 Transaction rollback, custom repository fragment 연결과 Guest·LOCAL 재가입을 운영 index 조건으로 검증
- `UserWithdrawn` outbox와 Learning Core 시험·결과 데이터 삭제 또는 익명화, stateless Access Token의 서비스 간 즉시 폐기는 별도 이슈로 설계
- 사용자가 동의 상태 조회 API와 문서의 unstaged diff를 검토한 뒤 필요하면 직접 commit·push하며 Jira 댓글이나 상태 변경은 별도 승인 전까지 수행하지 않음
- ECS Task Definition에 필수 개인정보 처리방침·이용약관 버전을 비공백 값으로 설정한 뒤 staging에서 GET 조회와 PUT 갱신 흐름을 검증
- MongoDB replica set·Transaction 지원 여부를 확인해 RefreshSession 회전과 다중 폐기의 원자성·동시 재발급 통합 테스트를 별도 작업으로 설계
- 운영 `refresh_sessions`의 `{userId, revokedAt}` 실행계획과 `_class` 외부 소비 여부를 확인하고 필요한 경우에만 승인된 index/migration으로 처리
- OpenAPI 오류 응답의 일반 오류·Validation 배열 schema 구체화와 UserFactory의 `Clock` 주입·application 이동 여부를 후속 개선으로 검토
- Learning Core 저장소에서 Jira `TMI-10`을 기준으로 Identity JWKS를 조회해 RS256 서명·issuer·`tosunsaeng-learning-core` audience를 로컬 검증하고 JWT `sub`를 실제 userId로 사용하는 연동 작업
- Identity 보호 API용 별도 audience 또는 다중 audience 도입 여부와 scope 기반 세부 인가 정책을 후속 보안 설계에서 확정
- 운영 배포 전에 기존 RefreshSession 문서의 Optimistic Lock 버전과 회전 패밀리 필드 이행 정책 확정
- MongoDB Transaction 없이 기존 Session 폐기 후 후속 Session 저장이 실패하는 경우의 복구 또는 재로그인 UX 정책 확정
- 배포 환경의 issuer/JWKS URL 합의와 이전 Public Key 유지 기간을 포함한 다중 키 Rotation 전략 확정

## 중요 결정

- Java 21
- Jira `TMI-6`은 사용자 확인 기준 main 병합·전체 테스트 성공 후 명시적 승인으로 `완료` 전환됐으며, 추가 댓글이나 상태 변경은 별도 명시적 승인 후 수행
- Jira `TMI-9`는 사용자 확인·승인 후 transition ID `41`만 적용해 `완료`로 전환됐고 Resolution도 `완료`로 확인했으며, 댓글·필드와 다른 이슈는 변경하지 않음
- Jira `TMI-10`은 사용자 승인에 따라 `High` 우선순위로 생성한 뒤 별도 승인으로 transition ID `21`만 적용해 `진행 중`으로 전환했으며 담당자·스프린트·에픽·라벨·댓글과 다른 필드는 변경하지 않음
- Jira `TMI-88`은 PR #16의 `develop` 병합 확인 후 사용자 승인으로 transition ID `41`만 적용해 상태·Resolution `완료`를 확인했으며 댓글과 다른 필드는 변경하지 않음
- Jira `TMI-89`는 PR #17의 `develop` 병합 확인 후 사용자 승인으로 종료 댓글 ID `10003`을 등록하고 transition ID `41`만 적용했으며 상태·Resolution이 모두 `완료`임을 재확인했다
- Jira `TMI-90`은 사용자 승인에 따라 Stage 0 Firebase 인증 broker ADR·격리 PoC 범위의 `작업`으로 생성했으며 현재 상태 `해야 할 일`, 우선순위 `Medium`, Resolution 없음이다. local PoC 구현 뒤에도 별도 승인 없이 댓글이나 상태를 변경하지 않았다
- Atlassian 연동은 저장소 설정이 아닌 Codex 사용자 전역 MCP 설정으로 관리하며 Remote MCP URL은 `https://mcp.atlassian.com/v1/mcp/authv2`를 사용
- Spring Boot 3.4.2
- MongoDB
- 현재 작업 기준 브랜치는 `feat/TMI-96-phone-eligibility-outbox-publisher`, 기준 commit은 PR #23 merge commit `31130fd`다. Codex는 commit·push를 수행하지 않음
- 주석은 비자명한 인증·세션·보안 의도에만 한 줄로 추가하고 DTO 필드·getter·단순 대입에는 추가하지 않음
- 애플리케이션 코드는 `domain.auth`, `domain.user`, `global`의 세 최상위 역할로 나누고 실제 클래스가 없는 빈 패키지는 만들지 않음
- 현재 `domain.auth`는 78개 파일에서 local login/signup, Session, Firebase federation·enrollment, SocialIdentity와 PhoneIdentity를 horizontal layer별로 함께 담아 탐색 비용이 커졌다. `firebase`·`phone`을 `auth`와 동급 최상위 도메인으로 올리지는 않고, 후속 구조 개선에서는 `auth/local`, `auth/session`, `auth/firebase`, `auth/phoneidentity`처럼 business capability별 vertical slice 안에 application·domain·infrastructure를 모으는 방향을 우선 검토한다. Firebase는 외부 인증 기술 adapter이고 phone은 독립 통신 도메인이 아니라 verified identity ownership이므로 최상위 이름만으로 bounded context를 만들지 않는다
- Controller는 Repository를 직접 참조하지 않고 유스케이스 application service만 호출하며 단일 구현체를 위한 `Service`/`ServiceImpl` 인터페이스는 만들지 않음
- `RefreshSession`과 Repository는 Auth 도메인이 소유하고 Refresh Token 생성·해싱·설정은 `global.security.refresh`의 기술 구현이 소유
- `MongoTransactionManager`는 Guest User·최초 RefreshSession 생성과 회원 탈퇴 User tombstone·전체 RefreshSession 폐기에 적용하며, 기존 Rotation·재사용 탐지·logout-all의 다중 Session 저장 원자성은 별도 후속 범위로 유지
- 운영 로그는 Controller·Repository·Entity에 중복 추가하지 않고 공통 요청 완료 경계와 application service의 실제 상태 변경 완료 지점에 기록하며, Transaction 성공 이벤트는 transactional proxy 반환 이후에만 남김
- 로그 공통 필드는 `event`, `outcome`, `requestId`, route template, status, duration, 안전한 경우의 `userId`·provider·errorCode·처리 건수로 제한하고 userId는 메트릭 tag로 사용하지 않으며 실제 자격증명·개인정보·Token·Hash·Header·Key·DB URI와 요청/응답 본문은 기록하지 않음
- 예상 밖 5xx는 요청 완료 filter가 application ERROR의 단일 소유자이며, Service·Controller·`GlobalExceptionHandler`는 같은 예외를 ERROR로 중복 기록하지 않는다. Handler 또는 filter 바깥에서 탈출한 예외는 안전한 오류 문맥을 request attribute에 넣고 `errorLogged` guard로 ERROR/Error dispatch 중복을 막으며, 실패 요청에는 별도 INFO 완료 로그를 남기지 않음
- 로그 언어는 machine-readable 검색·집계 식별자인 ECS key와 `event`·`outcome`·`errorCode`를 안정적인 영어 계약으로 유지하고, 사람이 읽는 애플리케이션 `message`는 한글을 사용할 수 있으며 대시보드 표시명도 한글로 구성
- Sentry build plugin의 source context 업로드와 runtime 오류 수집은 별도 책임이며 build 인증 값은 CI에만 두고 runtime에서는 DSN·environment·release를 환경별로 주입한다. SentryAppender issue 전송과 handled exception resolver 순서 변경은 사용하지 않고, 기존 handler가 처리하는 예상 밖 Exception만 명시적으로 capture하며 unhandled 오류는 SDK 기본 경계에 맡김
- 실제 Sentry DSN이나 인증 값을 채팅·저장소·WORKLOG에 전달하거나 기록하지 않고 환경변수 contract만 구현한다. 실제 project 수신은 사용자가 로컬 비추적 설정으로 확인했으며 배포 Secret·배포망 경계는 staging에서 별도로 검증
- Sentry `beforeSend`는 필드별 blacklist가 아니라 안전한 새 event를 만드는 whitelist 경계로 유지하고 원본 exception message와 모든 request·user·확장 context는 폐기한다. Source context는 경로·파일정보 없이 UUID 형식 JVM debug bundle ID만 허용한다. Logback integration은 계속 끄며 SDK/integration을 포함한 중복 여부는 handler 요청의 최종 transport event 개수로 검증한다.
- 실제 연결 확인에 사용한 one-shot startup trigger는 Sentry project 표시 확인 후 코드·설정·테스트·문서에서 제거했으며 production artifact에 포함하지 않는다. Handler·중복·4xx 제외·민감정보 비노출 계약은 외부 통신 없는 기존 통합 테스트로 계속 검증한다.
- 회원 탈퇴의 즉시 제거 범위는 User의 email·normalizedEmail·passwordHash·guestInstallationIdHash `$unset`과 nickname 익명화이며, User 문서·userId·provider·createdAt·동의 기록은 보존한다. RefreshSession 문서는 즉시 삭제하지 않고 모두 `ACCOUNT_WITHDRAWN`으로 폐기하며 만료 시 TTL 정리 대상이 되고, Learning Core 데이터와 기존 stateless Access Token은 이 API가 직접 삭제·폐기하지 않는다.
- OpenAPI Bearer 스키마는 전역 적용하지 않고 `GET /api/v1/users/me`, `GET`·`PUT /api/v1/users/me/consents`, `POST /api/v1/users/withdraw`와 `POST /api/v1/auth/logout-all`에 operation 단위로 적용
- Swagger/OpenAPI는 기본 활성화하되 배포 환경에서 `SWAGGER_ENABLED=false`로 비활성화 가능하고 테스트 프로필은 문서 계약 검증을 위해 명시적으로 활성화
- 실제 `userId`는 UUID 문자열
- User Document는 `users` 컬렉션을 사용하고 UUID 문자열 `userId`를 MongoDB `@Id`로 저장
- 이메일 정규화는 null 거부, 앞뒤 공백 제거, `Locale.ROOT` 소문자 변환만 수행
- Provider별 점 제거와 plus addressing 제거는 수행하지 않음
- `normalizedEmail`은 명시적인 unique index를 사용하며 현재 애플리케이션에서 자동 index 생성을 활성화
- 비밀번호 해시는 Spring Security의 기본 cost를 사용하는 BCrypt로 생성하고 User에는 `passwordHash`만 저장
- 회원가입 비밀번호 validation은 8~64자이고, 로그인 입력은 `NotBlank`와 최대 64자만 확인해 가입 복잡도·최소 길이 정책을 다시 적용하지 않음
- 이메일과 닉네임은 앞뒤 공백을 제거한 값으로 validation하며 비밀번호는 공백을 포함한 입력값을 임의 변환하지 않음
- 이메일 중복 확인은 가입 여부와 관계없이 성공 응답을 사용하며 `isAvailable`로 결과를 구분
- 회원가입의 사전 중복과 unique index 저장 충돌은 모두 `EMAIL_ALREADY_EXISTS` 409 오류로 통일
- 회원가입과 Guest 생성의 개인정보·약관 동의는 모두 반드시 true이며 false와 서버 현재 버전 불일치는 구분된 400 도메인 오류로 처리
- 현재 정책 버전은 필수 `app.consent.privacy-version`/`PRIVACY_CONSENT_VERSION`과 `app.consent.term-version`/`TERM_CONSENT_VERSION`으로 관리하고 누락·공백 시 기동에 실패하며 `AUDIO_POLICY_VERSION`은 제거
- 현재 동의 상태는 User 문서 내부의 `UserConsents`로 저장하고 별도 이력 컬렉션은 만들지 않음
- 기존 Mongo 문서에 `consents`가 없으면 두 동의를 false, 버전과 시각을 null로 읽고 과거 `audioConsent`를 새 동의로 자동 변환하지 않음
- User 생성·수정 시각 타입은 `Instant` 사용
- User provider는 `LOCAL`, `FEDERATED`, `GUEST`를 지원하며 기존 null provider 문서는 LOCAL로 해석
- User accountType은 `GUEST`와 `MEMBER`를 지원한다. 신규 LOCAL·Guest는 legacy provider와 accountType을 dual write하고 기존 accountType 누락 문서는 provider GUEST만 GUEST, LOCAL/null은 MEMBER로 읽으며 명시 accountType을 우선한다
- `UserProvider`와 프로필 provider는 rolling deployment와 기존 클라이언트를 위해 이번 expand 단계에서 유지하고 프로필 provider만 deprecated로 표시한다. UserProvider에 GOOGLE·KAKAO·APPLE을 추가하지 않으며 로그인 수단은 LOCAL credential과 별도 SocialIdentity가 소유한다
- MEMBER는 계정 유형일 뿐 LOCAL 비밀번호 보유를 뜻하지 않는다. local credential 세 필드가 모두 존재할 때만 `hasLocalCredential()`이며, social-only MEMBER는 허용하되 LOCAL factory는 세 필드를 계속 필수로 검증한다
- 현재 `UserProvider`는 가입 형태와 회원 탈퇴 자격증명 분기에 사용되므로 이번 단계에서는 이름 변경이나 `GOOGLE`·`APPLE`·`KAKAO` 추가를 하지 않는다. 외부 SNS 연결은 별도 `SocialProvider(GOOGLE, APPLE, KAKAO)`와 `SocialIdentity`가 소유해 한 User의 다중 SNS 연결을 허용한다
- `SocialIdentity`는 `@DBRef` 없이 canonical UUID 문자열 `userId`만 저장하고 provider subject를 case-sensitive opaque 식별자로 취급한다. 첫 모델에는 email과 변경 의미가 없는 `updatedAt`을 저장하지 않고 provider email은 검증 유스케이스 종료 시 폐기한다
- Kakao는 OIDC `sub`를 canonical `providerSubject`로 사용하는 안을 권장하며, 사용자 정보 API의 별도 `id`와 같은 namespace에서 혼용하지 않는다
- 동일 외부 계정이 둘 이상의 User에 연결되는 것을 막는 최종 동시성 경계는 애플리케이션 선조회가 아니라 MongoDB의 `(provider, providerSubject)` unique compound index로 둔다. userId 역조회에는 별도 non-unique index를 둔다
- 이번 단계에서는 요구된 외부 계정 유일성만 강제하고 `(userId, provider)` unique index는 추가하지 않는다. 한 User가 같은 provider의 여러 계정을 연결하지 못하게 할지는 실제 연결 API 전에 제품 정책으로 별도 확정한다
- 실제 Firebase 교환 API 공개 전 `UserProvider`의 계정 유형·로그인 수단 혼합을 해소하고 `UserAccountType(GUEST, MEMBER)`와 FirebaseIdentity·SocialIdentity를 분리한다. 기존 `provider=GUEST`는 GUEST, LOCAL 또는 null은 MEMBER로 호환 이행한다
- 공개 login/signup/link는 검증된 Firebase ID Token만 credential proof로 받는다. Identity는 Firebase UID mapping과 내부 User 상태를 확인해 기존 identity면 Guest 없이 즉시 인증하고 미등록 UID면 User 생성 없이 `SIGNUP_REQUIRED`와 짧은 TTL의 FirebaseEnrollmentAttempt를 반환한다
- 여러 Guest가 같은 외부 identity를 제시하면 기존 SocialIdentity owner를 canonical User로 선택한다. 자동 merge는 ACTIVE GUEST → ACTIVE MEMBER만 허용하고 MEMBER → MEMBER를 금지하며 chain·순환을 검증한다. Identity는 Learning Core 데이터를 직접 수정하지 않고 lease·retry·at-least-once `UserMerged` outbox로 이전을 요청한다
- 전화번호는 로그인·canonical 선택·자동 merge 키가 아니라 소유 검증, MEMBER 가입 uniqueness와 검증 번호당 무료체험 중복 방지 입력이다. 한 verified phone은 동시에 한 Firebase User·ACTIVE MEMBER만 소유하며 충돌은 `PHONE_ALREADY_LINKED`로 거절한다. Firebase가 SMS·code 검증을 소유하고 Identity는 검증된 Claim에서 E.164 번호를 받아 versioned domain-separated HMAC fingerprint와 `PhoneIdentity`를 소유하며 raw 번호·OTP·fingerprint를 로그에 남기지 않는다
- Guest 생성·둘러보기에는 전화 인증을 요구하지 않지만 무료 모의고사는 `MEMBERSHIP_REQUIRED`로 차단한다. Guest를 거치지 않는 신규 가입과 Guest 승격 모두 동일 Firebase UID에 link된 phone credential과 필수 동의를 요구하고 MEMBER·FirebaseIdentity·PhoneIdentity·SocialIdentity를 최종 Transaction에서 확정한다. 기존 identity 로그인·merge와 기존 MEMBER 로그인에는 전화 인증을 반복하지 않는다
- `TrialClaim`, `UserEntitlement`, 무료시험 grant·consume과 결제는 별도 Entitlement/Billing 및 Learning Core 경계가 소유한다. TrialClaim은 검증 번호별 혜택 지급 이력이고 UserEntitlement는 User별 사용권이므로 하나의 boolean이나 document로 합치지 않는다. Identity에는 `freeTrialUsed`나 시험·상품 코드를 추가하지 않으며 PhoneIdentity fingerprint와 다른 key·domain의 benefit-scoped candidate/proof만 consumer-scoped 계약으로 제공한다
- 전화 인증·회원가입 성공은 무료체험 지급이나 사용으로 보지 않는다. `TrialClaim`과 무료 entitlement는 사용자의 첫 무료 모의고사 요청에서 기존 PhoneIdentity를 이용해 silent claim하며 시험 시작 consume과도 별도 상태로 관리한다
- 관측 식별자는 application logs에서 필요한 internal userId만 허용하고 Sentry와 metrics에는 userId를 보내지 않으며 metrics label은 provider·outcome·errorCode 같은 낮은 cardinality로 제한한다
- Access Token은 RSA Private Key를 가진 Identity에서만 JWT RS256으로 서명
- Access Token 기본 TTL은 `PT30M`이며 issuer, audience, keyId, TTL과 키 Resource 위치는 환경변수로 교체 가능
- JWT Header는 `alg=RS256`, `typ=JWT`, 필수 `kid`를 사용
- JWT Claim은 UUID `sub`, `iss`, 단일 대상 배열 `aud`, `iat`, `exp`, UUID `jti`, 공백 구분 `scope`로 최소화
- 요청 scope는 정렬해 결정적으로 직렬화하고 null 또는 빈 scope에는 `learning:read learning:write` 기본값 사용
- Access Token에는 이메일, 닉네임 전체, 비밀번호 관련 값, Refresh Token 또는 Private Key 정보를 포함하지 않음
- RSA Private Key는 PKCS#8 `PRIVATE KEY`, Public Key는 X.509 `PUBLIC KEY` PEM 형식만 지원
- `RsaKeyLoader`는 Spring stereotype이 없는 로더로 유지하고 `JwtConfiguration`이 `ApplicationContext`를 `ResourceLoader`로 전달해 빈을 구성
- Spring Security 6.4 호환을 위해 `RSAKey` → `JWKSet` → `JWKSource<SecurityContext>` → `NimbusJwtEncoder` 구성을 사용
- JWKS는 `/.well-known/jwks.json`에서 Public JWK만 표준 `keys` 배열로 공개하며 BaseResponse로 감싸지 않음
- 현재 단일 Active Key를 사용하고 향후 Rotation에서는 기존 토큰 만료와 캐시를 고려해 이전 Public Key를 일정 기간 유지
- Access Token 발급 시간은 주입된 `Clock`을 사용하고 운영 기본값은 UTC 시스템 Clock
- Identity Resource Server는 기존 RSA Public Key Bean만 사용하고 자기 JWKS endpoint를 HTTP로 호출하지 않으며 Private Key를 검증에 사용하지 않음
- Identity Decoder는 RS256, 정확한 `typ=JWT`, 현재 `kid`, 필수 `sub`·`exp`, zero-skew `exp`·`nbf`, 설정 issuer와 audience 포함 여부를 검증
- 현재 Identity 보호 API도 기존 `tosunsaeng-learning-core` audience Access Token을 허용하며 Identity 전용 audience 또는 다중 audience는 이번 범위에 도입하지 않음
- 기본 `scope` 문자열은 Spring Security에서 `SCOPE_` 권한으로 변환하지만 이번 API는 특정 scope를 강제하지 않고 인증 여부만 적용
- 공개 경로는 지정된 인증 API, JWKS, health와 Swagger/OpenAPI로 제한하고 나머지는 기본적으로 인증
- 로그인은 `EmailNormalizer` 조회, BCrypt 일치 확인, `ACTIVE` 상태 확인 순서로 처리하고 실패 시 Access Token 발급기와 RefreshSession 저장소를 호출하지 않음
- 로그인 Access Token은 `AccessTokenIssuer.issue(userId, empty scopes)`를 호출해 설정된 기본 scope를 사용하며 Service에서 JWT를 직접 조립하지 않음
- 로그인 응답은 `accessToken`, `refreshToken`, `grantType`, milliseconds 단위 `accessTokenExpiresIn`만 포함하고 내부 사용자·세션 정보를 포함하지 않음
- Refresh Token 기본 TTL은 14일이고 난수 길이는 최소 32바이트이며 두 값은 `app.refresh-token` 환경 설정으로 교체 가능
- Refresh Token은 `SecureRandom` 기반 Opaque 값이고 Base64 URL-safe without padding으로 인코딩하며 UUID나 JWT를 대체 형식으로 사용하지 않음
- Refresh Token 원문은 `RefreshSession`에 저장하지 않고 UTF-8 SHA-256의 Base64 URL-safe without padding 해시만 저장
- `RefreshSession.createdAt`과 최초 `lastUsedAt`은 주입된 공용 `Clock`의 같은 시각이며 `expiresAt`은 해당 시각에 설정 TTL을 더함
- MongoDB TTL index는 만료 문서 정리 용도이며 향후 재발급은 문서 존재 여부와 별개로 `expiresAt`과 `revokedAt`을 직접 검증
- 토큰을 보유하는 내부 발급 결과와 로그인 응답의 문자열 표현은 토큰 값을 redaction 처리
- 재발급은 폐기 사유 확인 후 `expiresAt <= Clock`을 만료로 판정하고, 사용자 상태 확인 뒤 기존 Session의 Optimistic Lock 저장이 성공한 요청만 후속 토큰을 발급
- Rotation 재사용 탐지는 해당 사용자의 미폐기 RefreshSession 전체를 폐기하며 LOGOUT 또는 다른 폐기 사유는 일반 `INVALID_REFRESH_TOKEN`으로 구분
- 단일 로그아웃은 요청 Refresh Token에 해당하는 한 RefreshSession을 `LOGOUT`, 전체 로그아웃은 현재 JWT 사용자의 미폐기 RefreshSession 전체를 `LOGOUT_ALL`로 폐기
- 로그아웃은 Access Token 블랙리스트를 만들지 않으므로 기존 Access Token은 만료 시각까지 유효할 수 있으며 클라이언트는 성공 즉시 로컬 두 토큰을 삭제
- 로컬 키는 저장소에서 무시하고 테스트 키는 매 테스트 런타임에 메모리에서 생성
- Learning Core `audience`는 `tosunsaeng-learning-core`
- 실제 `userId`를 Python AI 서버로 보내지 않으며 Python AI의 `user_id`는 `examId` 유지
- 공통 응답 필드는 `isSuccess`, `code`, `message`, `result`
- 공통 오류 enum의 HTTP 상태와 응답 코드를 분리해 관리
- Validation 오류 상세는 `result` 배열로 반환하고 민감한 `rejectedValue`는 `null` 처리
- 테스트 프로필에서는 MongoDB 클라이언트·데이터·Repository 자동 설정 제외
- Security Filter Chain 내부 401/403은 공통 UTF-8 `BaseResponse` JSON Handler가 처리하고 Form Login과 Basic 인증은 비활성화

## 아직 구현되지 않은 것

- 구조화 운영 로그를 사용하는 수집 플랫폼 대시보드·메트릭·알림과 환경별 보존 정책
- 다중 Active/Retiring Key를 지원하는 Key Rotation
- 실제 격리 Firebase project·모바일 client의 email/password·Google·Apple·same-UID phone link, 국내 SMS와 Identity Platform Kakao Generic OIDC·billing·deep-link·Apple revoke 및 production 배포의 ADC/Workload Identity·timeout/quota 동작 검증
- TMI-98의 Learning Core 멱등 `UserMerged` consumer, source deny marker와 실제 학습 데이터 이전
- 기존 직접 email/password signup·login과 passwordHash writer의 Firebase cutover·제거, Firebase unlink·disable·withdrawal·orphan cleanup·reconciliation
- 별도 Entitlement/Billing의 benefit-scoped VerifiedPhoneBenefitBinding·TrialClaim·UserEntitlement·무료시험 consume과 결제 연동
- 사용자 프로필 수정 API
- 정책 버전별 append-only 동의 감사 이력과 동의 철회 정책

## 남아 있는 위험 요소

- TMI-98의 Mongo Transaction·CAS·outbox lease는 로컬 mock과 인메모리 회귀로 검증했지만 실제 replica set의 다중 collection rollback, source/target 경쟁 변경, unique/TTL index 생성과 publisher 다중 인스턴스 lease는 staging에서 재검증해야 한다.
- Identity의 MERGED source gate는 Identity 보호 API에서 DB User 상태를 조회하지만 Learning Core는 매 요청 Identity introspection을 하지 않는다. downstream consumer가 source deny marker와 ownership migration을 원자적으로 반영하기 전에 merge 기능을 켜면 기존 source JWT가 Learning Core에서 만료 전까지 받아들여질 수 있다.
- `UserMerged` publisher의 실제 HTTPS endpoint와 workload identity 발급 인프라, consumer commit 뒤 2xx·멱등 inbox·timeout/429/5xx·401/403·manual replay 운영 절차는 제공 환경에서 E2E 검증이 필요하다. 따라서 merge와 publisher flag는 계속 기본 비활성으로 유지한다.
- `RefreshSession`의 사용자별 미폐기 조회를 위한 `{userId, revokedAt}` compound index 계약을 추가했지만 운영 배포 전 실제 index 생성 상태와 데이터 규모별 `explain`을 확인해야 한다.
- 패키지 이동으로 신규 Mongo 문서의 `_class` FQCN이 바뀔 수 있으므로 외부 시스템이 `_class`를 조회 조건으로 사용하는지는 운영 데이터와 소비자에서 확인해야 한다.
- OpenAPI 오류 응답은 raw `BaseResponse` schema를 사용해 Validation의 `result` 배열을 완전히 구체화하지 못하므로 문서 전용 wrapper 도입 범위를 후속 검토해야 한다.
- `UserFactory`는 Spring `PasswordEncoder`·설정 주입과 `Instant.now()`에 직접 결합되어 있어 `Clock` 주입 및 application 계층 이동 여부를 후속 검토해야 한다.
- Atlassian MCP는 사용자 계정 권한으로 외부 서비스에 접근하므로 허용 범위와 연결 해제 필요성을 Codex 사용자 설정 및 Atlassian 계정에서 별도로 관리해야 한다.
- 현재 자동 index 생성은 초기 개발 편의를 위한 설정이며, 운영에서는 권한·데이터 규모·무중단 배포를 고려한 별도 index 관리 정책이 필요하다.
- TMI-91의 인메모리 Mongo는 unique key와 CAS query를 검증하지만 partial unique filter의 상태 변경 후 재삽입 의미를 완전히 구현하지 않는다. PENDING partial filter는 annotation/index resolver 계약과 service 경쟁 테스트로 고정했으며 실제 MongoDB의 partial unique·TTL index 생성과 상태 전환 후 새 attempt 허용은 staging에서 다시 검증해야 한다.
- test profile의 MongoDB 자동설정 제외는 유지하되 SocialIdentity Repository·실제 unique insert는 순수 Java 인메모리 Mongo를 테스트마다 임의 로컬 포트에 기동해 검증한다. 이는 Atlas 비의존 자동 회귀를 제공하지만 운영 MongoDB의 버전·권한·기존 데이터·index build 영향까지 증명하지 않으므로 staging index 생성 검증은 별도로 필요하다.
- 현재 회원 탈퇴는 User 자격증명과 RefreshSession만 처리하므로 향후 `SocialIdentity` 저장 전에 외부 subject 삭제 또는 tombstone, Apple revoke와 같은 SNS 재가입 정책을 확정해야 한다.
- TMI-89 코드는 legacy provider null을 MEMBER로 안전하게 읽지만 운영 데이터의 provider·accountType·credential 필드 분포를 실제로 조회하지 않았다. 배포 전에 read-only aggregate로 partial LOCAL credential, provider/accountType 불일치와 예상 밖 값을 확인해야 한다.
- accountType이 존재하면 provider보다 우선하므로 향후 Guest 승격 후 legacy provider가 GUEST로 남을 수 있다. 신규 소비자는 accountType을 계정 유형 기준으로 사용하고 deprecated provider를 MEMBER 판정이나 비밀번호 보유 추론에 사용하면 안 된다.
- TMI-92는 `PhoneFingerprintAlias`, retained candidate, ACTIVE_WRITE/LOOKUP_ONLY registry와 rotation 배포 계약을 구현했지만 key reference count·비상 rotation 자동화·기존 raw phone 없는 일괄 alias backfill은 제공하지 않는다. 모든 writer의 retained version overlap을 어기거나 legacy key를 조기 제거하면 version 교차 중복 귀속 위험이 남는다.
- TMI-92의 인메모리 Mongo는 partial unique index 정의·ACTIVE 중복 거절·release update를 검증하지만 상태 변경 뒤 같은 unique 값 재삽입 의미는 완전히 지원하지 않는다. 실제 MongoDB의 ACTIVE→RELEASED 후 신규 claim, 다중 collection Transaction rollback과 write conflict 재시도는 staging에서 다시 검증해야 한다.
- Firebase Phone Auth의 abuse가 무료체험보다 SMS 비용 문제를 먼저 만들 수 있으므로 Firebase quota·App Check/reCAPTCHA 등 client anti-abuse, 국가·번호 유형 정책, Identity exchange rate limit, provider kill switch와 비용 alert를 확인하기 전 공개하면 안 된다.
- Firebase Phone Auth는 client 인증과 Firebase user lifecycle을 추가하므로 가입 중단 시 고아 Firebase user, bearer ID Token 재사용 방지, 동일 UID phone link, 회원 탈퇴·disable·unlink cleanup과 국내 번호 도달률·비용·발신 규제를 PoC와 운영 정책으로 검증해야 한다.
- Firebase에 phone이 link된 중단 가입 user는 Identity PhoneIdentity가 없어도 번호를 점유한다. resume 유예·최근 활동·내부 mapping 부재를 확인하지 않은 unlink/delete는 정상 가입을 훼손할 수 있고, cleanup이 없으면 다른 사용자의 가입을 장기간 차단할 수 있다.
- 검증 번호당 ACTIVE MEMBER 1개 정책은 Firebase와 DB 제약을 일치시키지만 가족 공용 번호·번호 재할당·복수 계정 요구를 거절한다. 이는 자동 merge 근거가 아니라 가입 uniqueness 정책이며 제품 안내·계정 복구·탈퇴 후 번호 해제 절차가 필요하다.
- Firebase를 통합 broker로 채택해도 Kakao는 generic OIDC 지원 조건·redirect·Claim 계약 PoC 또는 Custom Token 발급용 Kakao 서버 검증이 필요하다. Firebase의 이메일 기반 자동 계정 연결이나 phone sign-in 결과를 canonical merge 근거로 신뢰하면 기존 `email·phone은 자동 merge 키가 아님` 계약과 충돌하므로 link는 소유권 재검증 후 Identity가 명시적으로 승인해야 한다.
- Kakao Generic OIDC는 Firebase Authentication만으로 사용할 수 있다고 가정하지 않고 Identity Platform 업그레이드·billing·provider 등록과 Android/iOS redirect·deep-link 동작을 확인해야 한다. 조건이 맞지 않으면 Kakao flag를 끄고 Custom Token bridge는 별도 ADR 없이는 추가하지 않는다.
- 채택한 Firebase broker 구조는 Firebase 장애·quota·가격·정책 변경에 신규 인증 전체가 영향을 받고 Firebase user와 내부 User의 이중 lifecycle, token exchange, 고아 enrollment cleanup이 필요하다. 장애 시 기존 내부 RefreshSession 재발급 허용 범위와 vendor exit를 ADR에서 고정해야 한다.
- 현재 Firebase 검증 adapter는 모든 Firebase 목적에서 revoke 확인과 최신 UserRecord 확인을 수행하므로 인증 교환마다 원격 호출·latency·quota 영향을 받는다. 공개 전 timeout·429/503 mapping, circuit/alert 기준과 기존 Identity RefreshSession reissue의 Firebase 비호출 정책을 실제 배포 환경에서 확인해야 한다.
- 회원가입 필수 OTP는 가입 전환율을 낮추고 무료시험을 사용하지 않는 회원에게도 SMS 비용·개인정보 수집을 발생시키므로 funnel·발송 비용·동의 고지를 함께 관찰해야 한다.
- FirebaseEnrollmentAttempt는 짧은 TTL·Firebase UID·purpose·Guest binding·조건부 일회성 소비가 없으면 ID Token 재사용이나 가입 흐름 혼합 위험이 있다. 동시 신규 가입은 FirebaseIdentity·SocialIdentity·PhoneIdentity unique 충돌 시 User·RefreshSession까지 전체 rollback돼야 한다.
- TMI-97의 Guest 승격 원자성은 모의 TransactionManager와 Repository mock으로 검증했지만 실제 Mongo replica set의 nested `REQUIRED` 참여·write conflict·unique race는 staging에서 재검증해야 한다. Firebase provider/phone link는 Mongo 밖에서 먼저 완료되므로 finalize 중단 cleanup·resume과 성공 응답 유실 뒤 로그인 교환 복구 UX도 운영 계약이 필요하다.
- binding별 PENDING partial unique는 만료 document를 application CAS로 EXPIRED 전환하기 전 새 attempt를 막는다. 만료 판정·상태 전환·동시 insert loser 재조회가 일관되지 않으면 여러 attempt 또는 가입 재개 실패가 생길 수 있으며 TTL 삭제 시각을 correctness에 사용하면 안 된다.
- Identity가 Firebase ID Token의 서명·issuer·audience·만료·폐기 여부·auth_time·sign-in provider를 검증하지 않거나 raw Token을 장기 grant로 재사용하면 replay와 목적 혼합 위험이 있다. Provider redirect의 nonce·state·PKCE 방어는 Firebase client/provider 설정을 PoC에서 별도로 확인한다.
- TrialClaim fingerprint를 탈퇴 후 유지하면 pseudonymous personal data 보존과 번호 재할당 오탐 문제가 남으므로 목적·기간·삭제·재가입 정책을 개인정보 처리방침과 법무 기준으로 확정해야 한다.
- benefit-scoped fingerprint binding도 pseudonymous data다. PhoneIdentity와 key·domain을 분리해도 가입 시 파생 outbox·Entitlement binding의 접근·보존·rotation·삭제와 outbox 지연 시 fail-closed 재시도 계약이 없으면 correlation 또는 무료체험 우회 위험이 남는다.
- TMI-96 publisher는 실제 consumer endpoint와 workload identity 발급 인프라를 제공하지 않는다. production 활성화 전 credential audience·HTTPS endpoint allowlist, consumer commit 뒤 2xx, timeout/429/5xx 재시도, 401/403 scope pause와 운영자 manual replay를 staging E2E로 확인해야 한다.
- Entitlement consume과 Learning Core exam 생성은 분산 Transaction이므로 문서의 reserve/create/confirm/cancel/reconciliation 상태 머신, CAS와 idempotency를 양쪽 서비스가 동일하게 구현하지 않으면 무료 권리가 중복되거나 유실될 수 있다.
- 기존 User 문서는 migration 없이 읽을 수 있지만 새 정책 미동의로 취급되므로 프론트의 재동의 유도와 정책 전환 시점 합의가 필요하다.
- 기존 User 문서의 provider가 없으면 LOCAL로 읽지만 소셜 로그인 도입 전에는 provider 필드 명시적 이행과 계정 연결 정책이 필요하다.
- 이메일 중복 확인·회원가입·로그인·재발급·로그아웃 공개 API에는 rate limit, credential stuffing 방어, 자동화 요청 방어 및 abuse 관측 기준이 필요하다.
- 비밀번호 복잡도, 유출 비밀번호 차단 및 변경 정책은 제품·보안 명세 확정 후 추가해야 한다.
- 현재는 최신 개인정보·약관 동의 상태만 저장하므로 철회와 정책 버전 변경의 전체 감사를 요구하면 append-only 이력 관리가 필요하다.
- 사용자 정보 변경 기능을 추가할 때 `updatedAt` 갱신 책임과 동시 수정 정책을 명확히 해야 한다.
- 민감한 validation 필드명이 추가되면 마스킹 목록도 갱신해야 한다.
- 운영 환경의 MongoDB 연결과 health 상태는 배포 환경에서 별도로 검증해야 한다.
- ECS 구조화 설정은 제3자 logger에도 동일하게 적용되며 현재 MongoDB 드라이버 INFO에는 계정 식별자와 클러스터 endpoint·topology가 포함될 수 있으므로 운영 수집 전에 logger level과 보존·접근 정책을 제한해야 한다.
- 사용자가 공유한 로컬 전체 기동 로그에서도 MongoDB 드라이버 INFO가 비밀번호 원문 없이 계정 식별자·클러스터 endpoint·replica topology를 포함하는 것이 재확인됐다. 후속 진단 공유는 필요한 애플리케이션 event 또는 고정 startup 오류만 최소 발췌하고 MongoDB driver 로그는 제외해야 한다.
- Sentry 최종 event는 whitelist로 정제되므로 원본 예외 message와 request context를 운영에서 볼 수 없으며, 문제 분석은 예외 type·stack frame·requestId·errorCode와 별도 안전한 구조화 로그에 의존한다. 실제 project 수신·표시는 로컬에서 확인했지만 staging 배포망·alert·보존정책 및 선택적 CI source context 업로드는 별도로 검증해야 한다.
- 저장소의 임시 smoke 설정은 제거됐지만 IntelliJ 실행 설정에 추가한 staging profile이나 Sentry diagnostic 환경변수는 저장소 밖에 남아 있을 수 있으므로 로컬 정상 실행 전에 사용자가 제거해야 한다.
- 예상 밖 오류 로그는 민감정보 비노출을 위해 exception/cause 타입과 message 없는 최대 24개 stack frame만 보존하므로 원본 예외 message가 필요한 진단은 재현·메트릭·추적 도구와 함께 수행해야 한다.
- 요청 filter가 application ERROR를 한 번만 기록해도 Servlet container나 외부 APM이 별도로 같은 Throwable을 기록할 수 있으므로, 실제 배포 후 logger category와 error dispatch를 확인해야 전체 수집 화면의 중복 여부를 판단할 수 있다.
- 운영 RSA Key의 생성·주입·파일 권한·백업·교체는 저장소 밖의 Secret 관리 및 배포 절차로 확정해야 한다.
- 현재 JWKS는 단일 Active Key만 제공하므로 Rotation 전에 복수 Public Key 제공과 캐시 전파 기간을 구현해야 한다.
- 배포 환경의 `issuer`와 Learning Core 검증 설정이 정확히 일치해야 하며 HTTPS 배포 URL과 환경별 값을 함께 확정해야 한다.
- 서버 간 Clock 차이가 Access Token 검증에 미치는 영향을 고려해 Learning Core의 허용 오차 정책을 정해야 한다.
- Identity 보호 API가 현재 Learning Core audience 토큰을 함께 허용하므로 Identity 전용 audience 또는 다중 audience와 토큰 용도 분리 여부를 후속 검토해야 한다.
- scope는 `SCOPE_` 권한으로 변환되지만 endpoint별 scope 인가를 강제하지 않으므로 권한 모델 확정 후 세부 정책을 추가해야 한다.
- MongoDB TTL 삭제는 비동기 정리이므로 만료 문서가 일시적으로 남을 수 있으며 재발급은 계속 `expiresAt`과 폐기 상태를 애플리케이션에서 검사해야 한다.
- MongoDB Transaction을 도입하지 않았으므로 기존 Session의 Rotation 폐기 저장 이후 Access Token 발급 또는 후속 RefreshSession 저장이 실패하면 사용자가 현재 Session을 잃고 다시 로그인해야 할 수 있다.
- 기존 RefreshSession 문서에 `@Version` 또는 회전 패밀리 필드가 없다면 운영 적용 전에 데이터 이행 또는 기존 Session 만료·재로그인 정책이 필요하다.
- 재사용 탐지에서 여러 활성 Session을 폐기하는 저장은 Transaction으로 묶이지 않으므로 중간 저장 실패 시 일부 Session만 폐기될 가능성이 남아 있다.
- logout-all의 여러 Session `saveAll`도 Transaction이 아니므로 중간 실패 시 일부 Session만 폐기될 수 있으며 오류는 호출자에게 전파된다.
- 단일·전체 로그아웃은 Access Token을 즉시 무효화하지 않으므로 클라이언트의 로컬 토큰 삭제와 짧은 Access Token TTL을 함께 유지해야 한다.
- 회원 탈퇴도 stateless Access Token을 즉시 폐기하지 않으므로 기존 Token은 설정된 최대 TTL까지 Learning Core에서 암호학적으로 유효할 수 있으며 서비스 간 즉시 폐기는 별도 계약이 필요하다.
- 격리 test profile은 외부 MongoDB를 사용하지 않아 Spring Transaction proxy rollback을 검증했지만 실제 Atlas·replica set의 다중 collection rollback과 custom repository fragment wiring은 staging에서 재검증해야 한다.
- Identity 회원 탈퇴는 Learning Core 데이터를 직접 삭제하지 않으며 `UserWithdrawn` outbox와 시험·결과 데이터 삭제 또는 익명화 정책은 아직 구현되지 않았다.
- 같은 활성 RefreshSession의 단건 로그아웃이 정확히 동시에 실행되면 `@Version` 저장 충돌을 `LogoutService`가 별도로 멱등 성공으로 변환하지 않아 한 요청이 일반 500으로 끝날 수 있으므로 동시성 정책과 예외 변환을 후속 검토해야 한다.
- 자격증명 실패의 외부 code와 message는 통일했지만 사용자 부재 경로와 BCrypt 검증 경로의 실행 시간 차이에 대한 완화 정책은 rate limit·관측 기준과 함께 검토해야 한다.
- Java 패키지 경로가 전면 변경됐으므로 이 저장소 내부 테스트는 통과했지만, 패키지 FQCN을 직접 참조하는 별도 모듈이 존재한다면 새 `domain`·`global` 경로로 import를 갱신해야 한다.
- OpenAPI 응답 설명은 런타임 계약을 보조하는 문서이므로 후속 API 오류 코드나 보안 정책 변경 시 Controller 어노테이션과 문서 계약 테스트를 함께 갱신해야 한다.

## Billing phone eligibility transport 계약 보정 (2026-08-27)

- 관련 기존 Jira는 `TMI-95`이며 Jira 댓글·상태·필드는 변경하지 않았다.
- Identity ADR-002의 기존 `/internal/v1/phone-eligibility-bindings/events` + workload Bearer JWT를 Billing C3-D의 `/internal/v1/eligibility/trial/events` + VPC Lattice AWS_IAM·ECS task role·SigV4 목표로 대체했다.
- eligibility 409는 EVENT_ID_CONFLICT 전용이며 429·503의 유효한 Retry-After를 자체 backoff보다 우선하는 delivery 계약을 추가했다.
- 현재 `JdkPhoneEligibilityBindingDeliveryAdapter`와 publisher port는 아직 audience/Bearer와 status-only 구현이다. publisher는 기본 disabled를 유지하며 SigV4 adapter·Retry-After·contract test 완료 전 staging에서 활성화하지 않는다.
- 애플리케이션 코드·설정·테스트·외부 인프라는 이번 문서 보정에서 변경하지 않았다.

## Codex Hook 운영 메모

- 프로젝트 로컬 Hook은 이 저장소와 각 Hook 정의가 신뢰된 경우에만 실행된다.
- Codex CLI에서 `/hooks`를 열어 `.codex/hooks.json`의 명령을 검토하고 신뢰해야 한다.
- Hook 명령이 변경되면 정의의 hash가 달라지므로 `/hooks`에서 다시 검토하고 신뢰한다.
- 모든 작업 종료 전에 WORKLOG를 append하고 이 문서를 최신 상태로 갱신한다.

## Stage 7 Billing 전달사항 정리 (2026-09-03)

- `UserMerged`는 Billing과 Learning Core 양쪽으로 전달하지만 `TrialOwnerRebindApproved`는 Billing에만 전달한다. Learning Core가 받는 phone owner event route는 없고 Identity→Learning Core `UserMerged`는 기존 workload JWT 경계를 유지한다.
- Billing `develop` PR #8 구현은 phone AttemptGroup 없음·`OPEN`·`RETAKE_AVAILABLE`이면 owner CAS, `GRADING`이면 retryable pending, `COMPLETED`이면 owner/fence 불변 NOOP를 적용한다.
- Billing은 current owner epoch에 `PHONE_REJOIN` transition id를 저장하고 read-only discovery로 exact continuationId·attemptGroupId·mockExamId를 반환하며, Learning Core의 reserve echo를 같은 Transaction에서 재검증한다.
- Learning Core `TMI-122`는 새 user에게 기존 Session이 하나도 없을 때만 discovery를 수행하고 continuation을 `ExamCreationOperation`에 snapshot한 뒤 target 소유의 새 Session을 같은 group으로 생성한다. source Session·답안·결과·Summary owner는 바꾸지 않는다.
- Learning Core 전체 457개 테스트는 통과했지만 확인 시점에는 feature 브랜치이므로 `develop` 병합이 남았다. 이 phone 구현은 별도 `UserMerged` source deny·owner migration consumer를 대체하지 않는다.
- Identity Stage 7 계획서는 위 상태표, discovery 204/200, exact reserve echo, status-first 응답 유실 복구와 완료 history migration 금지를 반영했다.
- Identity에 남은 핵심은 7-A phone eligibility SigV4·bounded Retry-After와 7-C exact `PhoneRejoinLineage`, immutable owner event core, `TrialOwnerRebindApproved` Billing-only delivery, `UserMerged` 양 consumer durable fan-out이다.
- 새 무료권 생성, Claim·Grant·ledger·consumption 초기화/복원, historical backfill과 phone proof 기반 source history migration은 금지한다. consumer flag OFF 선배포와 staging IAM·Mongo Transaction·E2E 전 production 활성화도 금지한다.
- pre-cutover lineage 부재, consumer-wide FIFO head-of-line blocking, AVAILABLE lineage 장기 보존과 가명정보 접근 통제는 여전히 남는 위험이다.

## 2026-08-31 앱 서버 통합 구조 조사 반영

- 신규 Jira 없이 Learning Core·Identity·Billing 전체 구조 조사에 Identity `feat/TMI-116-billing-reservation-exam-saga@8c4f3ca` snapshot을 반영했다. 통합 draw.io와 본 문서는 Learning Core `docs/architecture`에 있다.
- Identity의 책임은 User·LOCAL/Guest/Firebase/Social/Phone 인증, JWT/JWKS, RefreshSession, profile/consent/withdrawal과 durable lifecycle outbox로 정리했다. 시험·채점·혜택/원장은 다른 서비스 경계다.
- 강점은 asymmetric JWT, port/adapter, Transaction·outbox, retry/dead-letter와 민감정보 redaction이다. 주요 간극은 auth capability 탐색 비용, Identity→Billing phone eligibility transport의 Bearer workload JWT→VPC Lattice SigV4 전환, UserMerged downstream consumer 미완성이다.
- 애플리케이션·계약·Jira·외부 인프라는 변경하지 않았고 코드 변경이 없어 Gradle 테스트는 실행하지 않았다.

## 2026-08-31 문서 계층·완료 보고 규칙

- 별도 Jira 없이 `AGENTS.md`에 계획·조사 문서의 6단계 읽기 계층과 구현 완료 보고 필수 항목을 추가했다.
- 결론별 파일 근거와 구현 사실·계획·추론 구분을 요구하며, 상세 목록과 표는 부록으로 보존한다.
- 구현 완료 후 변경·계약·테스트·위험·배포 전 확인·예상 밖 diff·다음 확인을 보고한다.
- Identity 애플리케이션과 외부 계약은 변경하지 않았고 Gradle 테스트를 실행하지 않았다.
