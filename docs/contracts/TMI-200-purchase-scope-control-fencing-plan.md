# TMI-200 구현 계획 — 구매 권한·control 이관·발급/탈퇴 경합

- 작성일: 2026-10-12
- Jira: [TMI-200](https://to-teacher.atlassian.net/browse/TMI-200), 연계 [TMI-202](https://to-teacher.atlassian.net/browse/TMI-202)
- 브랜치: `feat/TMI-200-purchase-scope-control-fencing` (사용자가 준비한 현재 브랜치 유지)
- 상태: 아래는 승인 당시 계획이며 2026-10-12 OFF 구현·로컬 테스트를 수행했다. 실제 변경·차이·미검증 gate는 [구현 결과](TMI-200-implementation-result.md) 참조. DB 이관·배포·활성화는 미수행.
- 기준: TMI-199 공동 계약 및 [exact 규격 수락](billing-c0-exact-wire-index-proposal-2026-10-07.md), [교차 검증](billing-c0-fixture-review-2026-10-07.md), Billing 저장소 `docs/contracts/IDENTITY-BILLING-PAYMENT-LIFECYCLE-TECHNICAL-CONTRACT.md` §5.1/5.2/5.5/5.6. 기존 계약의 수치·보존·인증 규칙을 재정의하지 않는다.

## 1. 5줄 결론

1. 모든 일반 발급 경로에서 임의 purchase scope를 제거하고, DB에서 확인한 ACTIVE MEMBER에게만 공통 발급 경계가 이를 부여한다.
2. 기존 `user_session_controls`를 재사용하고, 구매 발급과 탈퇴가 같은 control을 조건부 쓰기하여 승인 순서를 확정한다.
3. 신규 control은 User와 함께 생성하고, 기존 누락/불일치는 별도 조건부 이관으로 처리한다. 로그인 중 임의 ACTIVE upsert는 금지한다.
4. 재발급 복구는 기존 암호화 결과를 그대로 사용하며 원 token bytes/iat/exp를 유지한다. 미확정 commit을 성공으로 응답하지 않는다.
5. 기본 OFF로 구현·회귀 검증한다. TMI-202 capture와 이관·구 writer 배제·운영 검증 전 구매 권한 ON 금지다.

## 2. 사용자가 반드시 읽어야 하는 내용

### 2.1 이번 작업이 하는 일과 하지 않는 일

이 작업은 결제 자체가 아니라 **구매 요청을 할 수 있는 회원의 인증 권한**을 안전하게 발급하는 작업이다. 상품/구매 계정·Store 결제·환불·LC 데이터 처리·snapshot/feed 구현은 포함하지 않는다. TMI-202의 journal/publisher/cleanup도 구현하지 않되, 같은 탈퇴 트랜잭션에 붙일 경계를 정한다.

기존 read/audience/account_type, RS256/kid/sub, workload 인증 및 공개 응답 기본 구조를 유지한다. 추가 사용자 claim으로 authEpoch를 강제하지 않는다. JWT 원문·refresh 원문·개인정보를 새 감사 로그에 남기지 않는다. 기존 암호화 재발급 응답 저장은 해당 보안/보존 계약을 유지한다.

### 2.2 성공의 의미

탈퇴보다 먼저 **발급 승인 commit**이 끝났다면 응답이 늦게 도착할 수 있다. 이를 모두 막겠다는 설계가 아니다. 원 만료시간을 늘리지 않고, 탈퇴가 먼저 commit된 경우 신규 purchase 발급·replay 승인을 막는 설계다. Billing이 이미 탈퇴를 수신한 계정의 구매 차단은 Billing 책임이다.

가입/승격/병합의 계정 변경이 성공한 뒤 별도 발급이 실패할 수 있다. 이를 가입 실패/계정 rollback으로 오인시키지 않으며 재로그인 또는 기존 복구 흐름으로 수렴한다. 프론트가 signup/merge를 무조건 재실행하도록 안내하지 않는다.

## 3. 사용자가 결정해야 하는 사항

- 지금 새 제품 정책 선택은 없다. 구현 시작은 별도 요청으로 진행한다.
- 운영 이관 대상·실행 시간·barrier 상한, 실제 purchase ON, 관련 서비스 배포·판매는 실측 후 별도 승인이다.
- 아래 클래스/설정명은 구현 제안이며 기존 명칭과 충돌 시 내부적으로 조정할 수 있다. 공개 오류 코드는 기존 프론트 계약과 매핑 확인 후 확정하며 임의로 동일 의미의 오류를 늘리지 않는다.
- 테스트 완료 후 사용자 commit/push 및 PR 병합을 거친다. Jira 댓글/상태 전환은 내용 제시·승인 후 수행한다.

## 4. 주요 위험과 미확인 사항

- 현행 `SessionSecurityService.control()`은 누락 시 미저장 기본 객체를 반환한다. 모든 호출을 한 번에 strict로 바꾸면 기존 인증/로그아웃이 깨질 수 있으므로 purchase 필수 경계와 기존 호환 경로를 분리한다.
- `@Version` 충돌을 피하려고 동일 control을 여러 서비스가 재조회/새 객체로 덮어쓰면 기존 logout/Firebase fence를 잃을 수 있다. 하나의 트랜잭션 컨텍스트에서 갱신 책임을 통합한다.
- User에는 현재 범용 optimistic version이 없으므로 이관의 User 조건부 쓰기를 어떻게 보장할지 기존 상태/updatedAt CAS와 모든 관련 writer를 대조해야 한다. 타임스탬프를 같은 값으로 set하는 no-op만으로 충돌 보장을 주장하지 않는다.
- 일반 발급의 unknown commit은 rollback 확정이 아니다. 새 durable receipt 없이 성공 결과를 꾸며내지 않는다. orphan 세션 정리는 기존 수명/제한 정책 범위에서 별도 검토한다.
- 실제 배포의 최장 과거 TTL·old writer·재발급 암호화 결과·clock skew·Billing 진행 요청 수명은 미조회다. 새 TTL30분 설정만으로 과거 권한 소멸을 증명할 수 없다.
- Mock 테스트는 replica-set의 실제 경합/commit 내구성 증거가 아니다. 기본 자동 테스트는 외부 DB 없이 실행하고, 승인된 격리 replica-set 통합 검증 및 TMI-206 staging을 별도 gate로 둔다.
- 현 작업 트리에 다른 운영/알림/전환 작업의 dirty 파일이 있다. 해당 파일은 이번 구현 범위로 편입하거나 정리하지 않는다.

## 5. 구현 계획

### 5.1 공통 모델과 역할

기존 User를 상태/type의 권위 원천으로 유지한다. control은 동일 사용자에 대한 조건부 쓰기 지점과 세션 보호 정보를 소유한다. 권장 추가 필드는 lifecycle state/accountType 및 `lastPurchaseTokenExpiresAt`이며 실제 명칭은 구현 시 고정한다. 상태/type를 control에 복제한다면 User와 함께 바꾸고 drift 시 purchase를 차단한다. 별도 authEpoch를 중복 도입하지 않고 기존 sessionEpoch의 의미와 재사용 가능성을 검증한다.

보존 필드: `@Version`, `sessionEpoch`, `revision`, `activeLogoutId`, `markingRequired`, `firebaseBindingId`, `minimumFirebaseAuthTimeExclusive`, `confirmedFirebaseRevocationBoundary`. missing field만 보완하고 replace/reset 금지. `lastPurchaseTokenExpiresAt`은 항상 max로 갱신하며 unknown을 0으로 해석하지 않는다.

내부 역할 제안:

- `PurchaseIssuancePolicy`: 요청 scope 정제 및 trusted 상태 기반 권한 판정. 클라이언트가 제출한 state/type/flag를 받지 않는다.
- `UserTokenIssuanceService`: User/control 검증·CAS·서명·refresh 저장을 조정하는 application 경계. 외부 트랜잭션에 참여하는 내부 메서드와 commit 후 반환하는 facade를 구분한다.
- `PurchaseReadiness`: 정적 설정의 모순과 실행 중 이관/capture 준비 상태를 구분한다. 단순 운영자 boolean 하나만으로 검증 증거를 대체하지 않는다.
- control migration/diagnostic: 조회 집계와 승인된 bounded 쓰기를 분리. API 요청 중 자동 이관 금지.

### 5.2 scope와 서명 경계

1. default와 explicit 두 경로에서 `billing:purchase`를 먼저 제거한다. scope 문법 검증은 유지한다.
2. 일반 `AccessTokenIssuer.issue(userId,type,scopes)` 호출만으로 purchase를 넣을 수 없게 한다. 새 purchase-aware 내부 경계는 동일 트랜잭션에서 읽은 User/control을 근거로 승인한다. 호출자가 임의 boolean을 전달해 검증을 우회하는 API를 만들지 않는다.
3. Guest는 purchase 없이 기존 `billing:read`와 audience를 유지한다. 비활성 계정은 기존 인증 거절 정책을 유지한다.
4. 서명에 사용할 iat/exp를 해당 시도에서 고정하고 signer 내부의 별도 Clock 재계산과 분리한다. purchase 수명은 최대30분, 원 만료를 기준으로 응답 직전 만료/deadline 확인.
5. 신규 sub canonical lowercase exact 검증. legacy 대문자를 묵시적으로 lowercase하여 소유 ID를 바꾸지 않는다. 진단/이관 전환 gate를 명시한다.

### 5.3 발급 트랜잭션

`User/control 읽기 → ACTIVE/type/기존 인증 보호 검증 → 같은 control CAS 쓰기 → 고정 시각 서명 → refresh/필요한 암호화 결과 및 max exp 저장 → commit → 응답 시각 재검사 → 반환`.

공통 Mongo 트랜잭션은 계약의 snapshot/majority+j를 요구한다. 기존 인증 전역 트랜잭션 설정을 이유 없이 변경하지 않고 새 필수 경계에 적용/검증한다. 가입/승격/병합 내 작업에 참여할 때 REQUIRES_NEW로 쪼개지 않는다. 내부 서명 결과는 outer commit 전에 Controller로 노출하지 않는다. self-invocation으로 @Transactional이 무시되는 구조도 금지한다.

외부 Firebase/HTTP 호출은 긴 DB 트랜잭션 안으로 옮기지 않는다. 외부 인증 결과의 계정 연결/epoch 유효성은 DB 트랜잭션에서 재확인한다. 충돌 retry는 bounded이며 매 시도 상태를 재검사하고 이전 미승인 token을 버린다. driver commit retry와 전체 business retry를 구분한다.

### 5.4 재발급 및 결과 불명

- `TokenReissueService`의 기존 rotation과 `ReissueRecoveryService`의 durable rotation/replay를 각각 유지하면서 공통 발급 fence를 적용한다. 단순히 한쪽만 바꾸지 않는다.
- durable replay: 원 request 바인딩·child 소유권·ACTIVE/type·epoch/control·기존 deadline을 동일 트랜잭션에서 확인하고 기존 결과만 반환. 원 bytes/iat/exp 불변; control max exp 감소 금지. 구 purchase 응답의 replay도 단순 purchase flag OFF만으로 안전해진다고 가정하지 않는다.
- unknown commit: 기존 durable 결과가 있는 재발급만 같은 operation으로 인증된 복구. 일반 로그인/계정 변경 후 발급은 bounded commit retry 뒤 여전히 불명이면503; 새 receipt 가정 금지.
- 계약 제안 `AUTH_ISSUANCE_UNRESOLVED`503/`AUTH_REPLAY_EXPIRED`401은 기존 `AuthErrorStatus`·재발급 오류/deadline과 매핑 표 작성 후 적용한다. deadline 만료를 새 token 재서명으로 해결하지 않는다. 이번 계획만으로 공개 오류 코드가 추가된 것은 아니다.

### 5.5 신규 생성·승격·이관

- 이메일/Guest/Firebase 가입의 User와 control을 같은 transaction에서 생성한다. unique 충돌/rollback 뒤 반쪽 문서가 남지 않게 한다.
- Guest 승격은 기존 control과 User type을 원자 변경. merge는 source Guest terminal 처리와 target MEMBER 발급을 구분하고 원 source에 purchase를 부여하지 않는다. 다중 사용자 control 쓰기 순서를 고정해 충돌·재시도 폭증을 줄인다.
- 이관 1단계: 개인정보 없는 집계로 상태/type별 control 누락·불일치·진행 보호 작업·UUID 형식 진단.
- 이관 2단계: 승인 범위 bounded keyset 순회, User CAS 쓰기+control insert/누락 필드 update를 같은 transaction으로 처리. 기존 logout/revocation 필드 유지. 탈퇴 우선이면 terminal로 수렴하거나 해당 건 격리, ACTIVE 복원 금지.
- 이관 3단계: 재시작/중복 실행·탈퇴/승격 경합 검증, skipped/conflict 처리 결과 재진단. no-op과 완료를 구분하고 대상 누락/불일치0을 확인한다. 운영 실행은 이번 구현과 분리한다.

### 5.6 TMI-202와 공통 수정 경계

| 영역 | TMI-200 책임 | TMI-202 책임 |
| --- | --- | --- |
| UserSessionControl | 필드/보존 규칙, 발급·탈퇴 CAS, 신규 생성·이관 | 동일 CAS 경계 재사용, 별도 ACTIVE 기본값 생성 금지 |
| 탈퇴 transaction | terminal User/control·세션 폐기 및 기존 보호 작업 handoff 유지 | 같은 transaction에 counter/journal/Billing delivery 추가 |
| 시간 | 신규 탈퇴 withdrawnAt 한 번 밀리초 정규화 후 모든 내부 저장에 전달 | 해당 동일 withdrawnAt 재사용, capturedAt과 혼동 금지 |
| 전송 | HTTP 호출 추가하지 않음 | commit 이후 publisher, LC 기존 outbox/cleanup과 독립 |
| 활성화 | capture 준비 증거 없으면 purchase ON 차단 | capture 호환 writer/cutover 보장, publisher OFF여도 capture 유지 |

구현 원칙: 기존 `UserWithdrawalTransactionService`가 outer transaction을 소유한다. I2 capture 참가자는 같은 Mongo session/transaction에 참여하고 실패를 전파한다. afterCommit/비동기 이벤트 리스너로 journal 저장을 넘기지 않는다. I1에서 임시 no-op capture를 준비 완료로 취급하지 않는다. I2가 없으면 OFF 개발은 가능하나 purchase ON은 불가능하다.

### 5.7 설정·배포·중단

설정 이름은 구현 시 확정하되 의미는 `purchaseScope.enabled` 기본false, 발급 fence 필수 여부, capture 준비·이관 검증 상태로 분리한다. 모순된 새 instance(purchase ON+fence/capture 미준비)는 startup 실패. 실행 중 purchase 전용 gate 이상은 purchase 발급을 차단하되 Identity 전체 ALB health를 자동으로 내리지 않는다. 일반 DB/보안 장애는 기존 fail-closed 정책 유지. 구매 전용 거절을 안전하지 않은 로그인 fallback으로 우회하지 않는다.

순서: additive schema/reader → 신규 생성·승격/탈퇴 writer 호환 → OFF 회귀 → 승인된 진단·이관 → 구 writer drain → I2 capture 검증 → staging → 별도 purchase ON 승인.

rollback은 purchase 신규 발급/publisher를 독립 OFF. 이미 발급된 token 소급 폐기는 불가. capture 시작 후 구 capture 미지원 writer로 복귀 금지. 기존 durable replay의 원 권한/만료 및 drain 증거 유지. 실제 token drain은 기록된 max exp·과거 TTL/마지막 구 writer 발급 가능 시각·clock skew 최대60초·Billing 진행 요청 수명까지 계산하고 불명 시 purge 차단.

### 5.8 구현 순서 및 완료 기준

1. 모든 진입점·User/control writer·오류·설정 표 확정, I2 공통 인터페이스 정의.
2. scope 우회 방지·iat/exp 입력 분리·TTL 검증과 단위 테스트.
3. control 필드/원자 초기화·terminal 처리·조건부 이관 도구 및 테스트.
4. 로그인/SNS/계정 변경 후 발급과 두 재발급 경로·Guest 복구 연결.
5. rollback/unknown/replay/양방향 탈퇴 경합·OFF 회귀·readiness 검증.
6. 문서/OpenAPI 오류 예시·운영 체크리스트 갱신, 전체 clean test, 변경 diff 검토. Swagger 공유 ZIP은 요청 없으면 생성하지 않음.

TMI-200 완료는 구현·테스트 및 미검증 운영 gate 기록과 병합 기준이다. 실제 이관/구매 ON이나 TMI-202/203 완성을 포함한 것으로 보고하지 않는다.

## 6. 부록 — 현재 근거·변경 후보·테스트

### 6.1 확인된 현재 코드

| 파일 | 확인 사실 | 계획 |
| --- | --- | --- |
| [JwtAccessTokenIssuer](../../src/main/java/web/tosunsaeng/identity/global/security/jwt/JwtAccessTokenIssuer.java) | default/explicit 선택 후 read 추가, equalsIgnoreCase UUID, 내부 Clock 서명 | purchase 제거/신뢰 경계·고정 시각 서명 분리 |
| [JwtProperties](../../src/main/java/web/tosunsaeng/identity/global/security/jwt/JwtProperties.java) | TTL positive 검증만 있음 | purchase 최대TTL gate |
| [UserSessionControl](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/domain/UserSessionControl.java) | version/epoch/revision/logout/Firebase 경계 소유 | additive 필드 및 보존 테스트 |
| [SessionSecurityService](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/SessionSecurityService.java) | control 부재 시 임시 객체, checkAndTouch는 ACTIVE/인증 검사 후 저장 | strict 구매 경계·기존 보호 로직 연계 |
| [ReissueRecoveryService](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/ReissueRecoveryService.java) | TransactionTemplate·unknown replayOnly·암호화 결과·child/control touch | 기존 복구를 보존하며 구매 승인/응답 경계 통합 |
| [UserWithdrawalTransactionService](../../src/main/java/web/tosunsaeng/identity/domain/user/application/UserWithdrawalTransactionService.java) | 기존 탈퇴 transaction 소유 | I1 fence 및 I2 capture 참여 경계 |

### 6.2 전체 발급/계정 생성 검토 목록

현재 `accessTokenIssuer.issue`/`accessIssuer.issue` 직접 호출 검색으로 9개 서비스 확인. 이 목록은 실행 증거가 아닌 정적 목록이며 구현 시 인터페이스 사용처 검색으로 재확인한다.

| 경로 | 현재 클래스 | 검증 포인트 |
| --- | --- | --- |
| 이메일 로그인 | LoginService | User 상태 재검사, refresh와 승인 commit |
| Firebase 기존 회원 | FirebaseExchangeService | provider/epoch 검증 유지 |
| Firebase 가입 후 | FirebaseSignupService / FirebaseSignupTransactionService | User/control 원자 생성, 가입과 발급 실패 구분 |
| Guest 승격 | FirebaseGuestUpgradeService | 승격 transaction과 발급 경계 분리, type drift |
| Guest 병합 | FirebaseGuestMergeService | source terminal과 target 발급, 새 scope 소유자 |
| Guest 생성 | GuestAuthService / GuestRegistrationTransactionService | User/control 원자 생성, Guest purchase 제거 |
| Guest 복구 | GuestRecoveryTransactionService | 이미 transaction 내 발급, nested 경계/commit 전 응답 노출 금지 |
| 기존 재발급 | TokenReissueService | rotation rollback/unknown, refresh 원문 비저장 |
| durable 재발급/replay | ReissueRecoveryService | bytes/exp 불변, epoch/deadline/unknown 결과 복구 |
| 이메일 계정 생성 | SignupService | 직접 token 발급과 별개로 User/control 원자 생성 확인 |

추가 변경 후보: AccessTokenIssuer 인터페이스/IssuedAccessToken, RefreshSessionIssuer, Guest upgrade/merge transaction 서비스, AuthErrorStatus, MongoTransactionConfig 및 session 설정·application.yml. 정확한 필수 변경은 해당 단계 구현 시 최소화하며 별도 Billing/LC 도메인 코드 추가 금지.

### 6.3 테스트 matrix

| 분류 | 필수 사례 |
| --- | --- |
| scope | default/explicit purchase 제거, OFF에서도 우회 불가, Guest/read 유지, ACTIVE MEMBER만 합성, inactive 거절, workload 불변 |
| control | 신규 원자 생성, absent strict 거절, conditional legacy insert, 보호 필드 보존, type/state drift, terminal race, version/수치 overflow |
| 발급 | 발급 먼저/탈퇴 먼저 commit, sign 후 rollback, commit 불명503, outer transaction rollback, 응답 전 expiry, max exp 단조성 |
| replay | 동일 원 bytes/iat/exp, old epoch/탈퇴 거절, child rotation/만료, type 불일치, deadline, unknown commit 중복 요청, flag OFF 후 기존 purchase replay/drain |
| lifecycle | 이메일/Firebase/Guest 신규 생성 rollback, 승격/병합·발급 사이 탈퇴, Guest 복구 경합, 기존 SNS 연결·로그아웃·전체 로그아웃 회귀 |
| migration | 재실행/중간 중단·재개, 탈퇴/승격 동시 CAS, absent terminal, 진행 logout/revocation 보존, 대문자 legacy 진단·묵시 변환 금지 |
| 운영 경계 | 모순 startup 실패, runtime purchase-only gate와 기존 health, I2 없는 ON 거절, old writer 차단·OFF rollback |
| 외부 계약 | RS256/kid/sub/audience/account_type/read, reissue 헤더/암호화 보존, 기존 public 오류, 민감 로그 비노출 |

기본 명령 `./gradlew clean test`. 외부 Atlas/OAuth 호출 없이 repository/provider mock. 실제 replica-set 경합·원자성은 별도 승인된 로컬 통합 환경과 공동 staging 증거로 구분하며 mock 통과를 대체 증거로 제시하지 않는다.

### 6.4 계획 작성 검증과 Jira 댓글 초안

이번 작업: 공식 TMI-200/202 설명·완료 기준 조회, 현재 issuer/control/reissue 및 발급/저장 진입점 정적 확인, 문서 링크·diff 검사. 제품 변경이 없어 Gradle은 실행하지 않았다. 운영 설정·실제 이관·코드 구현 완료를 주장하지 않는다.

Jira 댓글 초안(미등록): I1 구현 계획 작성. 변경 파일은 계획서와 WORKLOG/CURRENT_STATE. 전체 발급9경로 및 계정 생성, control 이관, 공통 transaction/I2 capture 경계, 오류/replay·OFF 활성화 gate와 테스트 matrix 정리. 정적 검토만 수행; 실제 replica-set/운영 TTL·이관·활성화 미검증. 자동 댓글/상태 변경 없음.
