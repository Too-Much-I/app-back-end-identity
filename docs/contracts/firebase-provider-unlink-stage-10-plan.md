# Stage 10 — SNS 연결 해제 구현 계획

- 작성일: 2026-09-10
- 상태: 제품 정책 및 최초/재연결 공통 흐름 구현 승인(2026-09-11) / TMI-131 서버 코드·격리 테스트 구현 / 운영 활성화 전
- 조사 기준: Identity `develop`, Stage 9 PR #42 병합 이후. 운영 배포·Firebase 설정 완료를 의미하지 않는다.
- Jira: [TMI-131](https://to-teacher.atlassian.net/browse/TMI-131) — 조회 상태 해야 할 일, 이번 구현에서 상태 변경 없음. 아래는 설계 근거이며 실제 구현 API·상태·설정·잔여 제한은 [구현/runbook](firebase-provider-unlink-stage-10-runbook.md)을 우선 확인한다. 운영 제공 완료를 뜻하지 않는다.
- 범위: Google·Apple·Kakao 연결 해제, 남는 수단 재인증, 전체 자체 세션 종료·Firebase revoke, 비동기 복구 및 모바일 계약. 전화번호 변경은 제외한다.

## 1. 5줄 결론

2026-09-11 구현 매핑: `ProviderChangeService`는 해제·상태와 legacy sync 검증, `ProviderLinkService`는 최초/재연결 공통 prepare/start/complete/status를 담당한다. 기존 versioned `UserSessionControl.activeLogoutId`를 logout UUID / `unlink:` / `link:` slot으로 공유한다. PREPARED는 slot 없이 만료 가능하며 STARTED부터 결과 확정 전 보호 상태를 유지한다. 구형 `relink:` 미소비 기록은 자동 변환하지 않는다. 정확한 API·보존·모바일 응답 유실 계약은 [runbook](firebase-provider-unlink-stage-10-runbook.md)을 따른다.

1. 사용자는 해제 후 남는 Google·Apple·Kakao 중 하나로 재인증해야 하며 마지막 허용 로그인 수단은 제거하지 않는다. [승인 정책](firebase-auth-follow-up-implementation-order.md#stage-10-승인-정책-2026-09-10)
2. 접수 시 자체 세션을 모두 무효화하고, Firebase 연결 해제·revoke·내부 exact SocialIdentity 해제를 비동기 작업으로 완료한다. User·FirebaseIdentity·전화번호·시험 기록은 삭제하지 않는다. [기존 세션 경계](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/domain/UserSessionControl.java)
3. 전체 로그아웃 후의 상태 조회는 남는 수단의 Firebase 재인증으로 보호한다. 요청 ID만으로 조회하거나 세션을 재생성하지 않는다. [기존 Firebase 검증](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/infrastructure/firebase/FirebaseAdminAuthenticationVerifier.java)
4. 기존 sync의 신규 저장을 폐기하고 공통 연결 완료만 단일 대상 SocialIdentity를 저장하도록 한다. 오래된 sync·Firebase 직접 연결로 승인을 우회하지 못하게 한다. [현재 sync](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseAuthMethodsSyncService.java)
5. 전화번호 셀프 변경과 번호 재할당 예외 처리는 당분간 미지원이다. 모든 신규 기능은 기본 OFF이며 실제 Firebase·모바일·Mongo 경합 검증 전 활성화하지 않는다. [구현 순서](firebase-auth-follow-up-implementation-order.md)

## 2. 사용자가 반드시 읽어야 하는 내용

### 2.1 사용자 흐름

Google·Apple·Kakao가 연결된 사용자가 Google을 해제하는 예:

1. 앱이 삭제가 아닌 **로그인 수단 연결 해제**임을 안내한다. 모든 기기에서 다시 로그인해야 한다는 점도 확인받는다.
2. 같은 Firebase User의 Apple 또는 Kakao로 재인증하고 강제 갱신한 ID Token을 받는다. 남는 수단 전부를 인증할 필요는 없다.
3. Identity Access Token과 재인증 증거로 Google 해제를 요청한다. 성공 응답은 완료가 아니라 내구성 있는 접수다.
4. 서버가 해제 차단 기록·작업·자체 세션 무효화를 한 Transaction으로 확정한다. 앱은 자체 Access/Refresh Token을 삭제하고 Firebase signOut한다.
5. 서버 worker가 Firebase의 Google 연결 해제와 UID 단위 revoke를 수행·검증한다.
6. 앱은 남는 수단으로 다시 인증하여 진행 상태를 확인한다. 남는 수단을 이용한 새 로그인은 아래 안전 조건을 충족하면 처리 중에도 가능하다.
7. Firebase 처리와 내부 연결 해제가 모두 확인되면 완료를 표시한다. 지연 revoke에 영향받은 새 Firebase 기반 세션은 한 번 더 재인증이 필요할 수 있다.

마지막 수단만 있거나 남는 수단으로 재인증할 수 없으면 접수하지 않는다. 전화번호는 단독 로그인 수단이 아니다. 이번 재인증 UI는 Google·Apple·Kakao를 대상으로 하며, 독립 LOCAL 비밀번호나 Firebase PASSWORD를 대체 증거로 사용하는 확장은 별도 정책 검토 대상으로 둔다. 기존 이메일 로그인 자체는 제거하지 않는다.

### 2.2 유지·제외 범위

- User·userId와 다른 로그인 수단은 유지한다. SNS 회사의 계정 자체를 삭제하거나 해당 사이트의 로그인 쿠키를 지우는 기능이 아니다.
- 전화번호 셀프 변경 API/화면, 번호 변경·재할당 수동 예외 처리는 보류한다. 기존 번호 점유 때문에 새 소유자가 가입하지 못할 수 있음을 안내한다.
- 번호 소유 증명만으로 다른 계정을 넘기거나 점유를 강제 해제하지 않는다. 지원 미제공은 unlink 작업의 장애 복구까지 없앤다는 뜻이 아니다.
- 신규 가입의 phone link, 탈퇴·중단 가입 cleanup은 유지한다. 탈퇴 정리가 완료된 후 같은 SNS·새 번호로 **새 User**를 만드는 재가입도 유지한다. 이전 기록 복구나 무료권 초기화를 의미하지 않는다.
- Billing·Learning Core 변경, 혜택 이전 이벤트, 새로운 workload JWT, Access Token 즉시 downstream 차단은 제외한다. 기존 자체 Access Token은 기존 만료·차단 계약을 따른다.
- 앱의 화면·SDK 구현은 프론트 담당이며 Identity 저장소에서는 서버와 연동 문서·테스트를 작성한다.

## 3. 사용자가 결정해야 하는 사항

### 3.1 이미 승인된 제품 정책

| 항목 | 확정 내용 |
| --- | --- |
| 1-A | 전화번호 셀프 변경 보류 |
| 2-B | 번호 변경·재할당 예외 처리도 당분간 미지원 |
| 3-A | 해제 후 남는 수단 중 하나로 재인증 |
| 4-A | 현재 기기 포함 모든 자체 세션 종료 + Firebase UID revoke |
| 5-A | 비동기 진행 상태 표시 및 실패 복구 |
| 공통 연결 (2026-09-11 최종 승인) | 최초/재연결 구분 없이 prepare/start/complete/status. 기존 계정·대상 양쪽 인증 및 중단 단계 구분 포함 |

동일 User 유지, 마지막 수단 보호, 자동 merge 금지, 오래된 sync로 연결 부활 금지, 재연결 시 명시적 새 인증은 유지할 조건이다.

### 3.2 이 계획의 제안과 구현 전 확인

- API와 설정값은 아래 설계안을 기준으로 구현 승인받는다. 이번 문서 작성 자체로 구현·배포·Jira 쓰기를 승인받은 것은 아니다.
- 재인증 유효기간 기본 PT5M, 한 사용자당 동시 인증수단 변경 1건, 완료 작업 보존 P7D를 제안한다. 운영 활성화 시 실제 설정값을 기록한다.
- 2026-09-11 후속 승인으로 최초 연결도 공통 절차로 전환한다. legacy sync는 신규 저장을 하지 않는다. 서버 구현 범위는 승인됐지만 프론트 구현·Provider 운영 검증·Jira 변경·배포는 별도다. Jira 원문에는 구형 relink 전용 설계가 남아 있어 승인 후 갱신해야 한다.
- 결과 불명인 외부 변경에 대해 '실패가 확정됐다'고 간주하지 않는다. 안전한 재연결 시점이 입증되지 않으면 해당 Provider의 변경만 제한하고 나머지 정상 로그인은 유지한다.
- Firebase SDK/실제 Provider 검증에서 재인증 출처 또는 외부 변경의 안전 경계를 입증하지 못하면 해당 Provider 활성화를 중단한다. 제품 정책을 임의로 완화하지 않는다.

## 4. 주요 위험과 미확인 사항

1. **Firebase에는 이 서비스의 Mongo version 조건을 적용할 수 없다.** DB lease 만료는 원격 요청 취소가 아니다. 이전 worker가 늦게 unlink/revoke할 수 있는 동안 새 연결·binding 교체·소유권 해제를 진행하지 않는다.
2. **Firebase unlink와 revoke는 다른 작업이다.** Provider 해제가 기존 Firebase 로그인 유지 정보를 확실히 폐기한다고 가정하지 않는다. revoke 검증도 별도로 수행한다.
3. **`auth_time`과 실제 재인증 Provider를 검증해야 한다.** `iat` 갱신만으로 재인증으로 인정하지 않는다. 현재 `signInMethod`와 SDK의 reauthenticate 결과를 Provider별 실제 환경에서 확인한다.
4. **남는 Provider snapshot도 stale할 수 있다.** 동시 unlink·sync·Firebase 외부 변경을 함께 검사하며 앱 화면의 연결 개수는 신뢰하지 않는다. Firebase 외부 조작까지 DB lock이 차단한다고 주장하지 않는다.
5. **이미 발급된 Access Token은 즉시 사라지지 않는다.** 인증수단 변경 API는 자체 epoch/사용자 상태/최근 인증을 추가 검사하고 downstream의 일반 요청은 기존 계약을 따른다.
6. **직접 Firebase phone 변경·unlink는 UI 부재로 막을 수 없다.** 검증된 Firebase phone과 내부 fingerprint의 불일치를 감지하고 자동 저장·혜택 재계산을 금지한다. 로그인 전체 차단 여부는 별도 위험 분석으로 확정하며 이번 번호 보류 정책만으로 새 로그인 차단을 추가하지 않는다.
7. **Apple·Kakao 동의 철회와 Firebase 연결 해제는 동일하지 않다.** 이번 목표는 로그인 연결 해제다. Provider별 의무·SDK 동작을 검증하며 필수 외부 철회가 확인되면 별도 승인 없이는 민감 authorization code 저장이나 원격 철회 기능을 추가하지 않는다. 검증 전 해당 Provider는 OFF다.
8. **Mock/in-memory Mongo는 실제 Transaction·partial unique·늦은 원격 호출 검증 증거가 아니다.** 로컬과 staging 증거를 분리한다.

## 5. 현재 작업과 직접 관련된 설명 — 상세 설계안

### 5.1 기존 기반과 신규 경계

기존 `UserSessionControl`의 epoch·Firebase 최소 인증 시각·확인된 revoke 경계와 `SessionSecurityService`의 발급 Transaction 경계를 재사용한다. 기존 `activeLogoutId`는 logout 전용이므로 서로 다른 mutation을 조정하는 기능이 이미 있다고 가정하지 않는다.

신규 구성요소 제안:

- `ProviderUnlinkService`, `ProviderUnlinkOperation`, `ProviderUnlinkWorker`, Repository 및 Controller.
- `FirebaseProviderMutationPort`: exact Firebase 대상 inspect 및 지정 Provider unlink만 제공. 계정 delete/disable/phone 변경 권한은 제공하지 않는다.
- `AuthMethodChangeControl`: 사용자·binding별 Provider 차단 및 변경 revision. 모든 관련 작성자가 같은 문서를 조건부 갱신한다.
- `FirebaseMutationCoordinator`: unlink/revoke와 기존 logout/withdrawal/rebind의 원격 변경 slot을 공통 조정한다. 기존 worker의 확인·호출 시작·완료 저장까지 연동한다.
- `ProviderRelinkAttempt`: 이미 해제한 수단의 명시적 재연결 준비를 위한 단기 일회성 기록.

기존 `FirebaseSessionRevocationPort`는 revoke-only 경계를 유지한다. 신규 worker가 `LogoutAllService`나 다른 worker를 HTTP처럼 호출해 별도의 epoch 증가를 반복하지 않는다. revoke 관측·대상 검증을 공통화하되 작업 식별자와 queue 순서는 명확히 분리한다.

### 5.2 API 설계

신규 URL에는 하이픈을 사용하지 않는다. 기존 URL·Request/Response JSON·BaseResponse는 유지한다. 아래 오류명은 신규 설계 이름으로, 구현 시 기존 오류와 충돌 여부를 확인한다.

#### 접수: `POST /api/v1/auth/firebase/providers/unlink`

- 사용자 Access Token 필수. workload JWT 불가. `sub`에서 사용자 결정, 요청 body에 userId/Firebase UID/provider subject 없음.
- `Idempotency-Key`: 단일 canonical lowercase UUID v4, 새 사용자 행위에 생성하고 응답 유실 시 유지.
- 요청: `provider`는 GOOGLE/APPLE/KAKAO, `firebaseIdToken`은 남는 수단 재인증 후 갱신한 증거. 문서 예시는 실제 credential을 넣지 않는다.
- ACTIVE MEMBER, exact FirebaseIdentity, 최근 인증, 남는 수단·소유권, sessionEpoch, 기존 탈퇴 gate를 모두 검증한다.
- 202 BaseResponse 성공의 result: `operationId`, `provider`, `status=PROCESSING`, `acceptedAt`, `nextPollAfterSeconds=3`.
- 같은 owner·요청 ID·대상으로 이미 접수됐으면 기존 작업 반환. 다른 provider/binding에 같은 ID를 쓰면 409. 재전달 때 달라진 Firebase Token 원문은 의미상 payload 충돌로 취급하지 않는다.
- 검증 실패 또는 Transaction rollback이면 작업 생성·세션 종료 모두 없어야 한다. commit 결과 불명은 새 작업을 만들지 않고 기존 요청 ID로 상태 확인한다.

#### 상태·응답 유실 복구: `POST /api/v1/auth/firebase/providers/unlink/status`

- POST를 써서 Firebase 증거가 query·URL에 들어가지 않게 한다. 사용자 Access Token은 요구하지 않는다. 일반 보안 필터의 permitAll은 무인증 허용이 아니라 이 endpoint 내부 Firebase 검증을 위한 설정이다.
- 요청: `requestId`(접수에 쓴 Idempotency-Key), `firebaseIdToken`. operationId를 응답받지 못해도 조회 가능하다.
- 서버가 검증된 project/UID에서 exact binding·ACTIVE User를 찾은 후 `(userId, requestIdHash)`로 조회한다. 요청 ID만으로 조회 불가, owner가 다른 작업은 404로 통일한다. 어떠한 Token도 발급하지 않는다.
- 최초 접수 때 남겼던 허용 remaining Provider 중 현재도 연결된 수단으로 재인증해야 한다. 해제 대상·PHONE 증거 거절. 신규 로그인과 동일한 최신 epoch/Firebase 인증 시각 경계를 확인하되 사용자 Access Token을 요구하지 않는다.
- 폐기된 Firebase 증거면 앱은 남는 수단으로 다시 인증한다. 동일 ID로 POST를 반복해도 unlink/revoke 재실행이나 epoch 증가 없음.
- 200 result: `operationId`, `provider`, `status`, `acceptedAt`, `completedAt`(nullable), `nextPollAfterSeconds`(종료 시 null). `PROCESSING`, `COMPLETED`, `ACTION_REQUIRED`, `SUPERSEDED`만 외부 공개한다. 원격 내부 오류·UID·subject 노출 금지.
- `RECONCILIATION_REQUIRED`는 ACTION_REQUIRED, 탈퇴 등 우선 lifecycle로 중단된 작업은 SUPERSEDED다. 탈퇴한 사용자는 상태보다 기존 ACCOUNT_WITHDRAWN 오류가 우선한다.
- 조회 보존 만료·미존재는 404. 미존재 응답을 동일 요청 ID의 mutation 재실행 허가로 해석하지 않는다.

#### 최초/재연결 공통: `POST /api/v1/auth/firebase/providers/link/*`

- `prepare`: 사용자 Access Token + 기존 SNS fresh proof + 대상 provider + Idempotency-Key. owner/binding/epoch/revision 및 미해결 작업 검사. 일회성 PREPARED 기록만 생성하며 slot을 잡지 않는다. 이미 정상 연결돼 있으면 ALREADY_LINKED receipt를 반환하고 SDK 작업은 하지 않는다.
- `start`: linkAttemptId + 남는 SNS fresh proof. PREPARED/PT5M/epoch/revision/정확한 binding/ACTIVE MEMBER를 다시 검사하고 같은 Transaction에서 slot 확보·대상 차단(revision 증가)·STARTED 전환. **최초 성공 응답만 linkAllowed=true**이며 재전송은 다시 SDK 실행을 허가하지 않는다.
- 앱은 start 허가 뒤에만 동일 Firebase User에 대상 credential을 한 번 link한다. 성공/기존 실행 종료 확인 후 대상 SNS로 start 이후 재인증하고 강제 갱신한 Token을 제출한다.
- `complete`: 대상 수단·auth_time·현재 owner·binding·epoch/revision·STARTED/PT5M·slot을 검증한다. 대상 SocialIdentity 한 건 저장·차단 해제·attempt 완료·slot 해제를 같은 Transaction으로 확정한다. 원격 증거에 포함된 다른 SNS는 추가하지 않는다. 응답 유실 재전송은 현재 완료 상태만 검증한다.
- `status`: 사용자 Access Token + 원래 prepare requestId + 동일 owner fresh SNS 증거. 요청 ID만으로 조회하지 않으며 어떤 경우에도 SDK 재실행을 허가하거나 Token을 발급하지 않는다.
- PREPARED 중단은 안전한 논리 만료/TTL 가능. STARTED 결과 불명·만료는 ACTION_REQUIRED로 유지하며 잠금/TTL 자동 해제 금지. start 응답 유실/SDK 미호출 crash도 포함된다. 앱은 상태·외부 성공/실행 종료 확인 또는 운영 복구를 수행한다.
- 구형 `/relink/prepare`는 503으로 폐기하고 legacy 미소비 기록은 격리 보존한다. 기존 `/auth-methods/sync`는 승인된 목록 검증만 수행하며 신규 저장/permit 소비를 금지한다. 프론트 동시 전환 및 구형 writer 종료가 rollout gate다.
- 코드 근거: [공통 연결 서비스](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderLinkService.java), [attempt](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderLinkAttempt.java), [API](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderLinkController.java). 정확한 요청/응답·상태·설정은 runbook 5절을 따른다.

공통: `Cache-Control: no-store`, `Pragma: no-cache`, 본문/Authorization 로깅 금지, 요청 크기 상한·속도 제한·검증된 owner별 제한. 오류 설계: 400 형식, 401 재인증/인증 실패, 403 마지막 수단/허용 불가, 409 변경 경합·owner 충돌·relink 필요, 404 조회 불가, 429 제한, 503 기능 OFF/DB/Firebase 장애. 기존 탈퇴·병합 오류 우선순위 유지. 신규 정확한 code는 테스트·프론트 명세에 함께 고정한다.

### 5.3 인증 검증

- 신규 `PROVIDER_UNLINK`, `PROVIDER_UNLINK_STATUS`, `PROVIDER_RELINK` 목적을 분리하거나 동등한 명시적 정책을 둔다. 단순 enum 추가로 모든 기존 목적의 검증을 바꾸지 않는다.
- issuer/audience/project/tenant·서명·만료·revoked/disabled는 기존 검증기를 사용한다. 기본 high-risk age PT5M, auth_time 미래 여부와 iat 관계 검사도 유지한다.
- `signInMethod`가 실제 남는 Google/Apple/Kakao와 일치해야 한다. linkedMethods에 Apple이 있다는 사실만으로 Apple 재인증으로 인정하지 않는다.
- 서버가 현재 Admin UserRecord의 Provider와 내부 SocialIdentity를 대조한다. 해제 대상과 남는 수단의 exact subject는 내부 검증에만 쓰고 body에서 받지 않는다.
- 외부 조회는 DB Transaction 밖에서 수행하고, Transaction에서 snapshot을 취득할 때의 binding/change revision·User 상태를 다시 검사한다. DB 재시도 중 오래된 snapshot으로 무제한 재실행하지 않는다.
- 두 Provider를 동시에 각각 해제하려 하면 사용자 변경 slot의 원자 claim으로 한 요청만 접수한다. 완료 뒤 두 번째 요청은 최신 남는 수단 조건부터 다시 검사한다.

### 5.4 데이터·인덱스·보존

| 문서 | 필드 설계 |
| --- | --- |
| ProviderUnlinkOperation | operationId, userId, requestIdHash, provider, bindingId, bindingCreatedAt, targetSocialIdentityId, remainingProviders, acceptedAt, acceptedAuthTime, acceptedEpoch, changeRevision, state, phase, leaseOwner, leaseUntil, dispatchAttempt, dispatchStartedAt, dispatchAcknowledgedAt, observedAt, nextAttemptAt, failureCode, completedAt, cleanupAt, @Version |
| 외부 target 증거 | exact project/tenant/UID·remote creation은 기존 binding 참조를 우선 사용. 삭제 후에도 필요하면 최소 snapshot만 접근 제한된 작업 문서에 저장. raw phone·email·SNS credential·Token 원문은 저장하지 않음 |
| AuthMethodChangeControl | userId/bindingId, revision, activeOperationId, provider별 blocked/change generation, relinkAllowedAfter, unresolvedRemoteActor. 인증 차단에 필요한 상태이며 operation TTL과 분리 |
| ProviderLinkAttempt | attemptId, userId, requestIdHash, exact binding snapshot, provider, epoch/revision, PREPARED/STARTED/COMPLETED/ALREADY_LINKED, preparedAt/expiresAt/startedAt/completedAt, socialIdentityId, cleanupAt, @Version |
| ProviderRelinkAttempt | 구형 기록 전용. 신규 생성/소비 금지, 미소비 기록 자동 해제 금지 |

- 작업 unique `(userId, requestIdHash)`와 사용자별 active slot. 같은 key의 중복이 두 epoch 증가를 만들지 않게 한다. claim/query용 `(state,nextAttemptAt,leaseUntil)` 인덱스와 cleanupAt TTL.
- active slot은 versioned control 원자 갱신을 우선 사용한다. 추가 partial unique를 쓰면 정의·실제 Mongo 동작을 모두 검증한다.
- SocialIdentity는 exact id/user/provider/binding 증거를 확인한 뒤 삭제한다. User 소유 SocialIdentity 전체 삭제 금지. 기존 `(provider,providerSubject)` unique index는 유지한다.
- 완료·안전한 superseded 작업 `cleanupAt=terminalAt+P7D`. PROCESSING/RETRY_WAIT/RECONCILIATION_REQUIRED·미확정 remote actor는 cleanupAt 없음. 해결 후 terminal 시각 기준으로 설정한다.
- provider 차단 기록은 작업 삭제 후에도 유지한다. 명시적 안전한 relink 또는 기존 탈퇴 cleanup으로만 해제/정리한다. 최소 enum/revision 위주로 보관하고 raw subject를 장기 tombstone으로 복제하지 않는다.
- 신규 link PREPARED는 expiresAt+PT24H, COMPLETED/ALREADY_LINKED는 completedAt+PT24H, STARTED는 cleanupAt 없음. 구형 미소비 relink도 TTL 없음. 논리 기한 검사는 DB 삭제 지연과 무관하다.

### 5.5 접수 Transaction

외부 검증을 끝낸 뒤 같은 Mongo Transaction에서:

1. requestIdHash 기존 작업 확인 및 exact owner·의미상 대상 일치 확인.
2. ACTIVE MEMBER·binding·남는 수단·변경 revision·기존 탈퇴/원격 mutation slot 재검사.
3. active operation slot 획득, 대상 Provider 차단, operation PENDING 저장.
4. `UserSessionControl.logout`와 동등한 epoch 증가·최소 Firebase auth_time 경계 설정을 **한 번만** 실행.
5. 기존 자체 세션 무효화와 작업을 함께 확정. 물리적 마킹이 batch여도 모든 발급/refresh/replay가 새 epoch를 검사하여 즉시 논리 무효화돼야 한다.

성공 commit 이후에만 202를 반환한다. 실패하면 작업·차단·epoch 변경은 모두 rollback된다. commit 결과 불명에서는 `(owner,requestIdHash)` 확인만 하며 재접수로 새 작업을 만들지 않는다. 다른 작업이 만든 세션을 구 회전 replay로 되살리지 않도록 Stage 9의 기존 경계를 유지한다.

### 5.6 worker와 외부 변경 순서

정상 경로 설계:

`PENDING → CLAIMED → UNLINK_DISPATCHED → UNLINK_VERIFIED → REVOKE_DISPATCHED → REVOKE_VERIFIED → FINALIZING → COMPLETED`

- 각 phase는 version·owner·generation으로 조건부 갱신한다. safe read/pre-dispatch 실패만 RETRY_WAIT, 결과 불명은 VERIFYING 또는 RECONCILIATION_REQUIRED. 자동으로 원래 상태로 돌려 재호출하지 않는다.
- 호출 직전 User·exact FirebaseIdentity·remote creation·현재 Provider target과 남는 수단·mutation slot을 다시 확인한다. 다른 Firebase UID나 새 incarnation으로 자동 치환하지 않는다.
- unlink adapter는 대상 Provider만 제거한다. Admin SDK의 `providersToUnlink` 또는 동등한 지원 API를 구현 시 실제 dependency와 대조한다. 연결 목록 전체 덮어쓰기·phone 제거·delete/disable 금지.
- SDK/HTTP 자동 mutation 재시도 여부를 확인하고 중복 호출 가능성을 통제한다. timeout이나 전송 후 5xx는 기본 결과 불명이다. transport가 호출 전 실패임을 증명한 경우만 재dispatch할 수 있다.
- 현재 Provider가 안 보인다는 inspect만으로 늦은 이전 actor가 없다고 결론내리지 않는다. 이전 호출의 확정 응답·로컬 실행 종료 및 자동 재시도 부재를 확인해야 relink/slot release가 가능하다.
- unlink 확인 후 Firebase revoke를 수행하고 `tokensValidAfterTime`을 재조회한다. 기존 Stage 8의 보수적 초 단위 경계와 exact target validation을 재사용한다.
- 지연 revoke에 영향받은 Firebase 기반 새 세션만 관측 경계로 추가 무효화한다. 완료 단계에서 user epoch를 무조건 또 증가시키지 않는다.
- 마지막 Transaction에서 target SocialIdentity exact 삭제·provider block 유지·operation 완료·active slot 해제를 함께 수행한다. FirebaseIdentity와 User·PhoneIdentity는 유지한다.
- 남는 수단 유실·target 교체·예상 밖 원격 Provider 변화·확인 불가 상태는 완료하지 않고 격리한다. Provider별 재인증 실패와 작업 단위 경보를 구분한다.

### 5.7 진행 중 로그인·다른 lifecycle과의 관계

- 대상 Provider로 새 로그인/가입/merge/sync가 우회되지 않게 중앙 guard를 적용한다. remaining Provider로 auth_time이 접수 경계 이후인 새 로그인은 정상 User/binding/ownership 조건이 성립하면 허용한다.
- 허용되지 않은 Provider의 추가·불일치가 보이면 이를 자동 등록하지 않는다. 안전한 remaining Provider 인증만 허용할 수 있도록 verifier의 linked snapshot과 실제 인증 출처를 분리한다. 불일치 전체를 무조건 정상 처리하는 fallback 금지.
- unlink 처리 중에는 추가 unlink·relink·phone 변경·binding 교체를 409로 막는다. status와 사용자 로그아웃 자체는 계속 가능해야 한다.
- logout-all의 자체 보안 처리는 즉시 가능하되 외부 revoke는 공통 mutation coordinator에서 순서를 지킨다. 기존 unresolved revoke가 있으면 unlink 원격 시작을 막고 상태로 안내한다.
- 탈퇴 접수·자체 세션 폐기는 막지 않는다. 이미 시작된 외부 actor는 취소됐다고 가정하지 않고 탈퇴 cleanup이 그 증거를 인수한다. 인수 확인 없이 SUPERSEDED로 terminal 처리하거나 인증 연결을 release하지 않는다.
- 기존 ACTIVE 회원 rebind는 Stage 12 범위다. 이번에는 경합 차단 hook만 설계하고 신규 rebind 기능을 만들지 않는다.

### 5.8 자동 재연결 방지와 phone 변경 보류 검증

적용 대상: Firebase exchange, 직접 signup owner precheck/unique conflict, Guest prepare/upgrade/merge owner resolution, auth methods sync, unlink/relink, withdrawal release, 관련 세션 발급·refresh/replay.

- 각 경로는 latest control/binding 상태를 확인한다. 외부 조회 뒤 내부 쓰기까지 동일 change revision을 조건으로 사용하고 새 작업과 충돌하면 재조회한다.
- 기존 auth methods sync의 '목록에서 누락이면 추가'는 모든 Provider에서 폐기한다. 최초/재연결 모두 공통 link 완료에서 대상 한 건만 저장한다. 오래된 proof나 완료 전 snapshot으로 전체 목록을 upsert하지 않는다.
- social owner mapping을 물리 삭제한 뒤에도 binding-level provider 차단 기록으로 동일 User의 old-proof 재연결을 막는다. 작업 TTL 삭제가 차단 해제를 뜻하지 않는다.
- 다른 User의 credential owner 충돌은 기존 gate·unique index로 거절한다. 탈퇴·merge 완료처럼 보이게 owner를 자동 치환하지 않는다.
- phone이 바뀐 Firebase 증거를 sync·exchange에서 읽었다고 PhoneIdentityService.linkOrReplace를 자동 호출하지 않는다. 기존 등록된 번호는 남는 로그인 수단이 아니며 변경 지원 API를 추가하지 않는다.

### 5.9 설정·운영 설계값

| 설정안 | 기본값·의미 |
| --- | --- |
| FIREBASE_PROVIDER_UNLINK_ENABLED | false, 새 접수 허용 |
| FIREBASE_PROVIDER_UNLINK_WORKER_ENABLED | false, worker 실행 |
| FIREBASE_PROVIDER_RELINK_ENABLED | false, 명시적 재연결 준비 |
| PROVIDER_UNLINK_RECENT_AUTH_MAX_AGE | PT5M; 기존 verifier와 더 엄격한 경계 사용 |
| PROVIDER_UNLINK_LEASE | PT60S |
| PROVIDER_UNLINK_CONNECT_TIMEOUT / READ_TIMEOUT | PT3S / PT5S, 전체 호출 deadline·SDK retry 포함 검증 |
| PROVIDER_UNLINK_MAX_ATTEMPTS | 12, 안전한 phase별 retry만 집계 |
| PROVIDER_UNLINK_BACKOFF_INITIAL / MAX | PT5S / PT5M, full jitter |
| PROVIDER_UNLINK_FIXED_DELAY / BATCH_SIZE | PT5S / 20 |
| PROVIDER_UNLINK_TERMINAL_RETENTION | P7D |
| PROVIDER_RELINK_ATTEMPT_TTL / TERMINAL_RETENTION | PT5M / PT24H |

기존 Firebase·세션 fencing 기능과 Mongo Transaction이 준비되지 않으면 신규 기능 ON startup을 실패시킨다. 신규 접수만 OFF해도 이미 접수한 작업의 상태 조회·보안 guard는 유지한다. worker 중단은 새 dispatch를 멈추되 진행 중 요청이 취소됐다고 해석하지 않는다. guard를 제거하는 legacy rollback은 금지한다.

운영 지표는 pending age·phase latency·safe retry·unknown mutation·reconciliation·provider blocked 로그인·마지막 수단 거절·stale sync 차단을 고정 label로 집계한다. userId/UID/requestId/credential을 metric label에 넣지 않는다. mutation 결과 불명과 target 불일치는 경보·운영 확인 대상이다. generic public 수동 재시도 API를 추가하지 않는다.

### 5.10 구현 순서와 완료 기준

1. 승인 정책·API·프론트 single-flight/로그아웃/상태 조회 계약 확정 및 Provider adapter 검증 계획 작성.
2. operation/control/permit·인덱스·접수 Transaction·상태 인증 구현.
3. 모든 owner/발급/sync 경로 guard와 Stage 9 replay 회귀 테스트 작성.
4. 공통 mutation coordinator를 기존 logout/withdrawal worker에 연계하고 단계별 unlink/revoke worker 구현.
5. 명시적 재연결 경계·SDK adapter 격리 테스트·OpenAPI·프론트 문서·운영 runbook 작성.
6. 전체 테스트와 실제 diff 검토, 코드 완료와 외부 검증 잔여 분리 인계. commit/push는 사용자 수행.
7. staging Provider·모바일·Mongo·다중 instance·지연 actor 증빙 확인 후 신규 flag를 단계적으로 활성화. 실제 카카오 Identity Platform 설정이 없으면 Kakao ON 불가.

## 6. 부록 — 근거·전체 검증 표

### 6.1 확인한 코드와 변경 예상

| 근거 | 현재 확인 사실 / 변경 예상 |
| --- | --- |
| [SocialProvider](../../src/main/java/web/tosunsaeng/identity/domain/auth/domain/enums/SocialProvider.java) | GOOGLE/KAKAO/APPLE 정의 |
| [VerifiedFirebasePrincipal](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/VerifiedFirebasePrincipal.java) | signInMethod/authTime/linkedMethods 제공; 상태 조회 인증·remaining proof에 활용 |
| [검증기](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/infrastructure/firebase/FirebaseAdminAuthenticationVerifier.java) | revoke/disabled 및 목적별 age 검사 존재; unlink 목적·실제 재인증 출처 추가 검증 |
| [sync](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseAuthMethodsSyncService.java) | 누락 SocialIdentity 추가 및 owner 확인; provider block·relink permit 없음 |
| [SocialIdentity](../../src/main/java/web/tosunsaeng/identity/domain/auth/domain/entity/SocialIdentity.java) | provider/subject unique; 전용 unlink 상태 없음 |
| [UserSessionControl](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/domain/UserSessionControl.java) | epoch·auth-time boundary·activeLogoutId; 범용 원격 mutation coordinator 아님 |
| [SessionSecurityService](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/SessionSecurityService.java) | 세션 발급 공통 write 경계; error 변환 뒤 재시도 의미 보존 필요 |
| [revoke worker](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/FirebaseSessionRevocationWorker.java) | exact target·dispatch 증거·관측 및 결과 불명 처리 기반 |
| [Stage 8](firebase-logout-all-revoke-stage-8-plan.md), [Stage 9](refresh-token-response-recovery-stage-9-plan.md) | 기존 계약·테스트 유지; 새 revoke와 refresh 복구 경합 검증 |
| [프론트 가이드](frontend-firebase-auth-integration-guide.md) | unlink를 sync로 대체하지 않도록 이미 명시; 전용 API 명세 추가 예정 |
| [BaseResponse](../../src/main/java/web/tosunsaeng/identity/global/response/BaseResponse.java) | isSuccess/code/message/result 구조 유지 |

위 표는 설계 당시 조사 기준이다. 실제 변경 파일과 구현 차이는 [구현/runbook](firebase-provider-unlink-stage-10-runbook.md) 및 TMI-131 WORKLOG를 따른다. 계획의 운영 검증까지 완료했다고 해석하지 않는다.

### 6.2 필수 검증 매트릭스

| ID | 검증 항목 | 로컬 증거 / 실제 환경 확인 |
| --- | --- | --- |
| U01 | Google·Apple·Kakao 각각 남는 수단 하나로 정상 접수 | parameterized unit / Provider별 재인증 |
| U02 | 해제 대상·PHONE·지원하지 않는 수단 재인증 거절 | verifier/service tests |
| U03 | 마지막 수단·disabled Provider 제외, 동시 2개 해제 | CAS/Transaction tests / replica set |
| U04 | auth_time/iat/exp·issuer/audience/tenant·revoked·disabled | verifier tests / real SDK |
| U05 | 다른 User·UID·subject·binding incarnation 거절 | target/ownership tests |
| U06 | 같은 요청 ID는 한 작업·한 epoch, 다른 payload는 409 | concurrency tests |
| U07 | 최초 저장 단계별 rollback, commit 불명 read-only 복구 | fake fault tests / real Transaction |
| U08 | 현재·다른 기기의 LOCAL/Firebase 자체 세션 전부 무효화 | session tests / 모바일 |
| U09 | 접수와 issue/refresh/Stage 9 replay 경합 | regression / multi-instance |
| U10 | 응답 유실 후 requestId+fresh remaining proof로 상태 복구 | HTTP tests / 앱 crash 복구 |
| U11 | status 무증거·다른 owner·workload·탈퇴/병합 거절 | HTTP/security tests |
| U12 | 상태 조회가 Token을 발급하거나 mutation을 반복하지 않음 | issuer/port zero-call assertions |
| U13 | 대상만 unlink, 남는 Provider·phone·User 유지 | adapter unit / Provider inspect |
| U14 | unlink 성공+DB 실패, timeout·늦은 응답·SDK hidden retry | fault injection / remote transport |
| U15 | remote actor 불명 동안 relink·slot release 금지 | lifecycle tests / 지연 호출 실험 |
| U16 | revoke 별도 수행·validAfter 검증·초 경계 | worker tests / real Firebase |
| U17 | 남는 수단 새 로그인 허용 및 지연 revoke 영향만 무효화 | session tests / 모바일 |
| U18 | 다른 logout-all과 coordinator 순서·중복 epoch 방지 | integration / multi-instance |
| U19 | 탈퇴 우선 처리·actor 인수·CLEANED 안전 경계 | withdrawal regression / staging |
| U20 | old proof·old sync·operation TTL 이후 Provider 부활 거절 | service/repository tests |
| U21 | relink permit one-time·expiry·owner·revision·post-start proof | permit tests / 실제 link 흐름 |
| U22 | relink 외부 성공 후 응답 유실·지연 link·재해제 경합 | fault tests / 모바일·Firebase |
| U23 | 타 계정 credential 충돌 자동 merge 없음 | owner/unique regression |
| U24 | 전화번호 외부 변경·unlink가 내부 번호/혜택을 자동 변경하지 않음 | service tests / Firebase |
| U25 | 기존 가입·탈퇴·동일 SNS 새 번호 재가입 유지 | regression / 기존 staging gate |
| U26 | exact SocialIdentity만 삭제, control 차단 유지 | rollback/identity tests |
| U27 | TTL·논리 만료·미확정 기록 무TTL·partial unique | metadata tests / real indexes |
| U28 | 신규 flags 기본 OFF·의존성 startup·접수 중지 후 guard 유지 | config/HTTP tests / runbook rehearsal |
| U29 | no-store·로그 비노출·rate limit·공개 오류·OpenAPI | HTTP/log tests / ingress |
| U30 | 전체 Gradle 회귀·diff·동시성·지연/P99·미확인 증빙 인계 | clean test / staging load |

로컬 기본 명령은 `./gradlew clean test`, `git diff --check`. 실제 Atlas/OAuth Provider는 로컬 테스트에서 호출하지 않는다. 외부 검증은 별도 승인된 staging에서 수행하고 증빙 없는 항목을 완료 처리하지 않는다.

### 6.3 완료 인계

변경 파일·외부 계약·테스트 개수/결과·실제 diff 범위·남은 운영 위험·flag·PR/merge commit을 인계한다. 프론트에는 재인증 Provider 선택, requestId 안전한 보관, 전체 로그아웃, 상태 조회 재인증, 늦은 응답 무시 및 relink 신규 절차를 전달한다. Jira 생성/수정/종료는 사용자 승인 후 별도 수행한다.
