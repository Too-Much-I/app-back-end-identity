# TMI-200 구현 결과 — 구매 scope와 공통 발급 경계

- 날짜: 2026-10-12 / 브랜치: `feat/TMI-200-purchase-scope-control-fencing`
- 기준: [구현 계획](TMI-200-purchase-scope-control-fencing-plan.md), [TMI-199 exact 계약](billing-c0-exact-wire-index-proposal-2026-10-07.md)
- 상태: OFF 코드·로컬 회귀 검증 완료. 실제 이관·배포·구매 활성화·Jira Done 아님.

## 1. 5줄 결론

1. default/explicit scope의 `billing:purchase`를 제거하고, 공통 경계에서 DB의 ACTIVE MEMBER를 확인한 경우만 합성한다.
2. 기존 control에 상태·계정 유형·최대 구매 token 만료를 추가하고, 기존 epoch/logout/Firebase 보호 정보를 유지한다.
3. 로그인·SNS 회원 발급·두 재발급 경로를 연결하고, 재발급 replay는 원 응답을 재서명하거나 만료 연장하지 않는다.
4. 신규 계정/control 생성과 상태 전환은 기존 Mongo 트랜잭션에 참여한다. 기존 계정 이관은 명시적으로 호출하는 최대100건 서비스다.
5. `./gradlew clean test` 성공: 1,404건 중 1,398 통과·6 skipped. 실제 Mongo 경합과 TMI-202 준비 연결은 별도 검증 대상이다.

## 2. 사용자가 반드시 읽어야 하는 내용

**지금 설정을 ON해서 결제를 출시하는 작업은 아니다.** 세 설정은 모두 기본 false다. 현재 운영용 `PurchaseReadiness` 구현은 없으며, 따라서 scope ON은 시작 단계에서 거절된다. TMI-202에서 capture·이관·구 writer 배제 증거를 확인하는 구현을 연결해야 한다. 테스트의 `() -> true`를 운영 bean으로 복사하면 안 된다.

가입·승격·병합이 commit된 뒤 토큰 발급만 실패할 수 있다. `503 AUTH_ISSUANCE_UNRESOLVED`는 계정 변경 rollback을 의미하지 않는다. 프론트는 병합·가입을 무조건 반복하지 않고 로그인/현재 상태 확인으로 수렴해야 한다. 재발급 복구 요청은 같은 Idempotency-Key를 유지한다.

기존 토큰을 즉시 폐기하는 기능이 아니다. 발급 승인이 탈퇴보다 먼저 commit되었다면 응답이 늦게 도착할 수 있다. 실제 구매 차단에는 Billing의 탈퇴 수신과 token drain 검증도 필요하다.

## 3. 사용자가 결정해야 하는 사항

- 현재 추가 제품 정책 결정은 없다. commit/push는 사용자가 수행한다.
- DB 이관 실행, 테스트/운영 배포, 세 설정 활성화는 별도 승인한다.
- Jira 댓글은 아래 초안만 작성했으며 등록·상태 변경하지 않았다. PR 병합 전 Done 처리하지 않는다.

## 4. 주요 위험과 미확인 사항

- 신규 저장소 테스트는 Mock과 Spring의 테스트 트랜잭션 관리자를 사용했다. 실제 replica-set 다중 writer·write conflict·unknown commit·내구성·지연 응답은 검증하지 않았다.
- 기존 가입·로그인 회귀는 전체 테스트에 포함되지만, 모든 서비스 조합을 실제 구매 ON 상태와 Mongo로 실행한 증거는 아니다.
- 이관은 내부 서비스만 제공한다. 승인된 운영 실행기/접근 제한·checkpoint 보관·재진단 절차는 실제 이관 전에 준비해야 한다. 자동 이관이나 공개 관리 API는 없다.
- 이관의 conflicts는 완료가 아니다. 대문자/비정상 legacy ID는 변경하지 않는다. 비정상 cursor는 중단되므로 별도 진단 후 재개하며 순회 종료만으로 준비 완료를 선언하지 않는다.
- 활성 logout/markingRequired, control drift는 임의 덮어쓰지 않는다. 진행 보안 작업 해결 후 재검사해야 한다.
- 과거 최대TTL·old writer·기존 암호화 응답·skew·Billing 진행 요청 수명은 미조사다. `lastPurchaseTokenExpiresAt`만으로 과거 토큰 소멸을 증명하지 않는다.
- 일반 발급은 별도 durable receipt나 business 자동 재시도를 도입하지 않았다. commit 불명은503으로 종료한다. 기존 재발급 복구의 bounded retry는 유지한다. 별도 driver commit retry 확장은 미구현이며 통합 검증 시 검토한다.
- strict fence를 켠 후 OFF로 되돌리면 기존 purchase replay 보호도 제거되므로 단순 rollback 금지. 신규 scope만 OFF하고 fence/capture와 과거 token drain을 유지해야 한다.

## 5. 현재 구현과 배포 전 확인

### 5.1 적용 경계

- `JwtAccessTokenIssuer`: scope 정제, MEMBER 구매 승인, lowercase UUID 검증(fence ON), 고정 iat/exp를 사용한 서명.
- `PurchaseTokenFence`: 기존 트랜잭션 참여 또는 자체 트랜잭션, DB/control 승인, commit 전후 만료 검사. ThreadLocal에는 deadline만 보관하고 finally에서 제거한다.
- `PurchaseControlService`: User 상태/type 재확인과 versioned control 쓰기. 누락 ACTIVE를 발급 경로에서 upsert하지 않는다.
- `UserSessionControl`: `purchaseState`, `purchaseAccountType`, `lastPurchaseTokenExpiresAt`. terminal 재활성화/유형 변경 금지, 최초 terminal 전환에 epoch 증가. 기존 보호 필드 보존.
- `PurchaseControlMigration`: `batch(afterUserId, limit, apply)`. dry-run 기본 호출은 조회만 수행; apply는 User status/updatedAt 조건부 실제 revision 증가와 control 갱신을 같은 트랜잭션에서 수행. 결과 cursor는 사용자 ID이므로 일반 로그로 남기지 않는다(`toString`은 가림).
- Firebase 가입/승격/병합 후 발급: 앞서 저장한 refresh를 다시 조회하여 폐기/만료/세션 보호 검사 후 응답한다.
- Guest 신규 발급은 계정 저장 이전에 서명될 수 있으므로 MEMBER 승인 조회를 하지 않는다. 어떤 입력 scope에서도 purchase를 받을 수 없으며 User/control은 등록 트랜잭션에서 함께 생성된다.
- 탈퇴: 기존 outer transaction 안에 control terminal 전환 추가. 신규 withdrawnAt 밀리초 정규화. TMI-202 journal/counter/delivery는 이 경계에 참여해야 하며 이번에 구현하지 않았다.

### 5.2 설정 순서

| 설정 | 기본값 | 적용 전 조건 |
| --- | --- | --- |
| `AUTH_PURCHASE_CONTROL_WRITES_ENABLED` | false | 호환 writer 배포·구 writer 대조, 준비 중 승격 영향 검증 |
| `AUTH_PURCHASE_FENCE_ENABLED` | false | control 쓰기 ON, 기존 `AUTH_SESSION_FENCE_ENABLED` ON, 이관·재진단 완료 |
| `AUTH_PURCHASE_SCOPE_ENABLED` | false | fence ON, 최대TTL30분 이내, 실제 readiness 구현과 TMI-202/capture·공동 검증 완료 |

control 쓰기를 켜면 공유 Mongo manager에 snapshot/majority+j를 적용한다. 동일 세션에서 기존 트랜잭션에 참여하기 위한 선택이며, 다른 인증 트랜잭션의 성능·timeout도 staging에서 확인해야 한다. OFF에서는 기존 manager 설정을 유지한다.

준비 단계에서도 승격은 누락 control을 자동 생성하지 않는다. 이관 전 승격 트래픽 처리 및 짧은 쓰기 제한 여부를 배포 계획에 포함한다. `scope=false`는 strict fence의 준비 부족 오류를 무시하는 옵션이 아니다.

### 5.3 외부 계약

| 항목 | 결과 |
| --- | --- |
| RS256/kid/실제 UUID sub/account_type/audience/read | 유지 |
| 요청 body의 임의 userId | 추가 없음 |
| scope | 입력/config의 purchase 제거; 조건 충족 시 서버에서만 추가 |
| `AUTH_ISSUANCE_UNRESOLVED` | 신규503. 저장/발급 결과 미확정 또는 commit 경계에서 응답 만료 |
| control/readiness 불일치 | 기존 `SESSION_SECURITY_UNAVAILABLE` 사용 |
| 기존 durable recovery 만료 | `409 REISSUE_RECOVERY_EXPIRED` 유지; 별도 `AUTH_REPLAY_EXPIRED`401 미추가 |
| replay | 원 bytes/iat/exp, 헤더, 암호화 응답 계약 유지 |
| Swagger | 관련 Controller503 설명 갱신, ZIP/오프라인 공유본 미생성 |

예시: `{"code":"AUTH_ISSUANCE_UNRESOLVED"}`는 코드 발췌이며 응답 envelope는 기존 BaseResponse 계약을 따른다. 프론트는 실제 명세의 envelope를 사용한다.

## 6. 부록 — 변경 파일·검증 근거

### 6.1 변경 파일

- 신규: `domain/auth/purchase/`의 PurchaseProperties, PurchaseReadiness, PurchaseConfiguration, PurchaseControlService, PurchaseControlMigration, PurchaseTokenFence.
- 신규: `global/security/jwt/TokenIssuanceBoundary.java`.
- 변경: JwtAccessTokenIssuer, MongoTransactionConfig, UserSessionControl, User, application.yml.
- 변경: LoginService, SignupService, GuestRegistrationTransactionService; FirebaseExchangeService, FirebaseSignupService/TransactionService, FirebaseGuestUpgradeService/TransactionService, FirebaseGuestMergeService/TransactionService.
- 변경: TokenReissueService, ReissueRecoveryService, UserWithdrawalService/TransactionService, AuthErrorStatus, AuthController, FirebaseExchangeController.
- 테스트: 신규 `domain/auth/purchase/` 4개 테스트 클래스, 기존 JwtAccessTokenIssuerTests.
- 문서: 본 결과, 기존 계획의 상태 링크, WORKLOG append, CURRENT_STATE 최신 항목.

위 Java 경로 기준은 `src/main/java/web/tosunsaeng/identity/`다. 다른 운영·알림·전환 관련 기존 dirty 파일은 보존했고 이번 변경으로 취급하지 않았다. 이번 구현의 예상 밖 파일 변경은 발견하지 않았다.

### 6.2 실행한 검증

- 최종 `./gradlew clean test`: BUILD SUCCESSFUL, XML 합계1,404 / failures0 / errors0 / skipped6. 구매 패키지33건 포함. skipped는 통과로 계산하지 않는다.
- 중간 실행에서 설정 테스트3건이 Spring context 주입을 DB 호출로 오인해 실패했다. 초기화 호출만 허용하고 나머지 호출이 없음을 검증하도록 수정 후 전체 재실행 성공.
- 단위 검증: 입력 scope 제거, trusted 승인/Guest 거절, control 보호·max 만료·terminal, 누락/드리프트, 이관 dry-run/재실행/CAS 실패, commit rollback/불명/만료, 원 replay 무재서명, 준비 OFF·readiness 없는 ON 거절.
- `git diff --check` 통과. 외부 Atlas/OAuth·실제 이관·배포·판매·Jira 수정·commit/push 없음.

### 6.3 Jira 댓글 초안 — 미등록

TMI-200 OFF 구현: purchase scope 정제와 ACTIVE MEMBER/control 승인, 로그인·재발급/replay 트랜잭션 경계, 신규 control 생성/상태 동기화 및 bounded 이관 서비스 추가. 변경 파일은 purchase 패키지, JWT/인증·탈퇴 서비스, 설정·명세·테스트·구현 문서. clean test 1,404건(1,398통과/6skipped), 실패0. 실제 replica-set 경합·이관·old-writer/TTL 검증 및 TMI-202 readiness 연결은 남아 있음. 구매 ON·배포·Jira Done 미수행.
