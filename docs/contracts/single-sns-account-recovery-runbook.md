# TMI-192 단일 SNS·SMS 계정 찾기 구현 계약과 배포 점검

## 1. 5줄 결론

1. SNS 회원은 승인된 provider/subject 하나만 사용하며 `social_identities.userId` unique로 강제한다. [모델](../../src/main/java/web/tosunsaeng/identity/domain/auth/domain/entity/SocialIdentity.java)
2. 회원 SNS 추가 연결·해제 API와 exchange 자동 등록은 폐지했다. Guest prepare/upgrade/merge, LOCAL/PASSWORD, 기존 보안 차단은 유지한다. [공통 정책](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/SingleSocialIdentityPolicy.java)
3. 공개 prepare → 기존 Firebase SMS PHONE 인증 → lookup은 SNS·마스킹 이메일만 안내하며 로그인 토큰/회원/enrollment를 만들지 않는다. [컨트롤러](../../src/main/java/web/tosunsaeng/identity/domain/auth/accountrecovery/AccountRecoveryController.java)
4. 원자 소비·proof unique·재조회·분산 요청 제한을 구현했으며 기능은 기본 OFF다. [저장소](../../src/main/java/web/tosunsaeng/identity/domain/auth/accountrecovery/MongoRecoveryStore.java)
5. 코드·격리 테스트와 실제 배포 검증은 다르다. 실제 Firebase/Atlas/앱 연동 및 데이터·인덱스 확인 후 같은 출시 단위로 활성화한다. 외부 배포는 수행하지 않았다.

## 2. 반드시 읽어야 하는 내용

- 기존 Firebase 프로젝트/tenant/Admin 자격증명과 SMS 설정을 재사용한다. 새 프로젝트·전화번호 회원 로그인·정지 해제·이전 완료 조회 API는 추가하지 않았다.
- 계정 찾기 PHONE sign-in은 SDK 증명 획득 과정일 뿐이다. 일반 exchange/merge의 SNS 증명 조건을 완화하지 않는다. 기존 Identity Guest/MEMBER 토큰을 유지한다.
- SNS 추가 연결/해제 화면은 제거한다. `providers/link/*`, `providers/unlink`, `providers/unlink/status`는 유효 요청에 410 `PROVIDER_LINK_RETIRED`. 기존 보안·본문 검증은 선행할 수 있다. 운영 보안 이력·worker는 삭제하지 않았다.
- 같은 Firebase UID에 원격 SNS를 추가해도 Identity 승인은 생기지 않는다. 현재 로그인 provider/subject가 기존 유일한 승인과 같아야 한다. 가입·승격에는 SNS 둘 이상을 허용하지 않는다.
- `User.email`은 그대로 둔다. 가입·승격 시 provider별 `UserInfo.getEmail()`을 마스킹해 SocialIdentity에 저장한다. 로그인 중 자동 갱신은 하지 않는다. 기존 힌트가 없는 데이터는 UNAVAILABLE로 안내하며 백필/전체 이메일 저장은 하지 않는다.
- 정지/진행 보안 작업/blocked/LOCAL·PASSWORD-only/승인 SNS 부재는 ACTION_REQUIRED다. 탈퇴·해제 번호 또는 회원 부재는 NOT_FOUND. 다중 owner나 PhoneIdentity 무결성 이상은 503이며 추측하지 않는다.

## 3. 사용자가 결정해야 하는 사항

구현 범위의 추가 결정은 없다. 출시 담당자는 앱 배포 시점·실제 데이터 점검·환경별 기능 활성화 일정을 정해야 한다. Jira TMI-192 상태는 자동 변경하지 않았다. 실제 앱 저장소 구현은 별도이며 아래 QA 통과 전 전체 기능 출시 완료로 간주하지 않는다.

## 4. 주요 위험과 미확인 사항

- 회원이 없다는 설명은 실제 DB 조회로 확인하지 않았다. 복수 SNS 회원·진행 link/unlink/relink 작업이 발견되면 배포 중단. 자동 사용자 삭제/차단 해제/계정 합치기 금지.
- 인메모리 Mongo/Mockito 테스트는 Atlas replica-set commit/rollback 및 여러 서버의 실경합 검증을 대신하지 않는다.
- 같은 프로젝트의 전화 sign-in은 기존 전화 소유 UID에 로그인하거나 새 phone-only UID를 생성한다. 이후 신규 SNS 가입 시 credential-already-in-use 가능성을 실제 SDK로 확인해야 한다. 이를 해결하려고 기존 Firebase 사용자를 무조건 삭제하지 않는다.
- 번호 재할당/SIM 탈취에 대비해 힌트만 제공한다. 전화 인증으로 SNS 접근 권한을 복구하거나 기존 SNS를 교체하지 않는다.
- IP 제한은 `request.getRemoteAddr()` 기준이다. 신뢰할 수 있는 프록시만 실 IP를 전달하도록 구성/검증한다. 임의 X-Forwarded-For를 파싱하지 않는다. 프록시 IP로 집계되면 여러 사용자가 한도를 공유한다.
- Firebase SDK 직접 SMS 요청은 Identity HTTP 한도로 제한되지 않는다. Firebase 앱 검증/쿼터/SMS 정책을 별도 확인한다.

## 5. 프론트 계약

응답은 기존 BaseResponse. 모든 경로에 `Cache-Control: no-store`. Identity Bearer 불필요하며 가능하면 보내지 않는다(잘못된 Bearer는 보안 필터에서 거절될 수 있다).

### 요청과 결과

`POST /api/v1/auth/account-recovery/prepare`, 본문 `{}`. 결과 `{ "recoveryId": "<UUID>", "expiresAt": "<UTC ISO-8601>" }`. SMS 시작 전에 호출하며 서버가 SMS를 보내지는 않는다.

`POST /api/v1/auth/account-recovery/lookup`, 본문 `{ "recoveryId": "<같은 UUID>", "firebaseIdToken": "<PHONE 인증 ID Token>" }`. 클라이언트 전화번호/email/userId 입력을 조회 조건으로 사용하지 않는다.

| result.status | provider | maskedEmail | emailHintKind |
| --- | --- | --- | --- |
| FOUND | GOOGLE / APPLE / KAKAO | 마스킹 문자열 또는 null | EMAIL / APPLE_PRIVATE_RELAY / UNAVAILABLE |
| NOT_FOUND | null | null | null |
| ACTION_REQUIRED | null | null | null |

마스킹은 `alice@example.com` → `a***@example.com`, 한 글자 local-part는 `***@example.com`. Apple relay는 주소를 주지 않는다. 이메일 미제공/잘못된 형식은 UNAVAILABLE이며 가입 실패가 아니다. 결과는 textContent 등으로 표시한다. userId/Firebase UID/subject/전화번호/원문 이메일/토큰은 반환하지 않는다.

### 만료·재시도·제한

- challenge 5분, auth_time 최근 5분, 소비 후 retry 최대 5분(기본값). 재시도에서도 토큰 exp와 최근 인증 제한을 만족해야 하므로 실제 재조회 가능 시간은 더 짧을 수 있다.
- 전화 `auth_time >= prepare.createdAt`(초 단위 절삭), 미래 허용 최대 30초. Firebase의 기존 high-risk 인증시간/clock skew 검증도 적용하여 더 엄격한 설정이 우선한다.
- token phone_number와 최신 Admin phone 번호 일치, 프로젝트/tenant/서명/issuer/audience/revoked/disabled 검증 필수.
- 응답 유실: 같은 recoveryId + 같은 PHONE 인증으로 재조회. ID Token 강제 refresh로 iat가 바뀌어도 proof는 같다. 재조회 시 현재 로컬 계정 상태를 다시 읽으며 예전 힌트를 캐싱하지 않는다.
- 다른 challenge에서 동일 proof를 쓰거나 같은 challenge에 다른 proof를 쓰면 409. 새 prepare와 **새 OTP 인증**이 필요하다.
- prepare IP 10/분, lookup IP 20/분, 검증 후 UID 및 전화번호 각각 5/15분. 재시도/NOT_FOUND도 차감한다. Mongo 고정 시간창 counter를 공유하며 키 회전 시 모든 retained 버전을 검사한다.
- DB 요청 제한은 429 + Retry-After 900초(안전한 상한). 별도 프로세스 ingress 120/분 제한은 Retry-After 60초. 본문 24,576bytes 초과는 413, DTO token 길이는 16,384자 이하.

| HTTP / code | 프론트 처리 |
| --- | --- |
| 410 PROVIDER_LINK_RETIRED | SNS link/unlink 호출·SDK 실행 중단, 앱 갱신 |
| 409 SINGLE_SNS_REQUIRED | 가입·승격의 복수 SNS 거절, 자동 unlink 금지 |
| 409 SNS_ACCOUNT_MISMATCH | 승인 SNS로 재로그인 또는 계정 찾기 |
| 401 INVALID_RECOVERY_PROOF | 잘못된/폐기된 증명, 전화 인증 다시 시작 |
| 401 RECOVERY_RECENT_AUTH_REQUIRED | 오래된/prepare 이전 인증, OTP 다시 수행 |
| 410 RECOVERY_EXPIRED | prepare부터 다시 시작 |
| 409 RECOVERY_CONFLICT | 새 prepare + 새 OTP, 무한 재시도 금지 |
| 400 INVALID_RECOVERY_REQUEST | 없는/잘못된 ID, 새 prepare 또는 요청 수정 |
| 400 INVALID_REQUEST | JSON/Bean Validation 실패 |
| 413 RECOVERY_REQUEST_TOO_LARGE | 본문 크기 초과 |
| 429 RECOVERY_RATE_LIMITED | Retry-After 이후 사용자 재시도 |
| 503 RECOVERY_UNAVAILABLE | OFF/검증·DB 장애/무결성 오류, 회원 없음으로 해석 금지 |

기존 PHONE_ALREADY_LINKED는 계정 찾기 진입점이다. 기존 PROVIDER_RELINK_REQUIRED는 보안 block을 자동 해제하라는 뜻이 아니며 지원 안내로 처리한다.

## 6. 배포 설정·인덱스 절차

### 설정

[.env.example](../../.env.example), [application.yml](../../src/main/resources/application.yml) 참조.

| 환경변수 | 기본값 |
| --- | --- |
| ACCOUNT_RECOVERY_ENABLED | false |
| ACCOUNT_RECOVERY_KEY_RING | 빈 값, ON 시 필수; `v2:<base64>,v1:<retained-base64>` |
| ACCOUNT_RECOVERY_CHALLENGE_TTL | PT5M |
| ACCOUNT_RECOVERY_RECENT_AUTH | PT5M |
| ACCOUNT_RECOVERY_RETRY_TTL | PT5M |
| ACCOUNT_RECOVERY_PREPARE_PER_MINUTE | 10 |
| ACCOUNT_RECOVERY_LOOKUP_PER_MINUTE | 20 |
| ACCOUNT_RECOVERY_PROOF_PER_QUARTER_HOUR | 5 |

각 시간은 0초 초과~15분 이하. 키는 서로 다른 32바이트 이상 난수, 1~4개, 기존 JWT/전화 fingerprint/재발급 키와 분리하여 Secret Manager로 공급한다. 키 값은 문서/로그/저장소에 넣지 않는다. 기존 Firebase enabled/Phone enabled/phone-fingerprint enabled 및 키 저장소가 있어야 ON 가능하다. 누락 시 기동 실패로 닫힌다.

회전은 old만 있는 인스턴스와 new만 있는 인스턴스를 겹쳐 배포하지 않는다. 모든 인스턴스에 old+new를 먼저 배포한 뒤 전환하고, 마지막 old-only 인스턴스 중단 후 최소 24시간 old를 유지한다. 버전명을 같은 키로 바꾸거나 조기 삭제하면 replay/한도 우회 위험이 있다.

### 사전 확인 및 인덱스 전환 (실행하지 않은 운영 절차)

1. 정확한 대상 DB/Firebase 프로젝트를 운영자가 확인한다. 백업 및 되돌림 계획을 준비한다. 쓰기를 중지한 유지보수 창에서 진행한다.
2. `social_identities`를 userId별 count 집계하여 count>1이면 중단한다. 유효 UUID userId 누락/중복, provider-subject 충돌, 활성 phone aliases의 복수 owner도 확인한다. 조회 결과에 개인정보를 출력하거나 공유하지 않는다.
3. 진행 중 ProviderLinkAttempt STARTED, 미소비 relink, 미완료 unlink 및 보안 block 상태를 확인한다. 존재하면 임의 삭제하지 않고 배포를 중지한다.
4. `social_identities.getIndexes()`에서 `uk_social_identities_provider_subject` unique를 유지한다. userId `{userId:1}` unique `uk_social_identities_user_id`를 만든다. Mongo 버전이 같은 key pattern의 기존 nonunique와 동시 생성을 허용하지 않으면, **쓰기 중단을 확인한 뒤 정확한 구 인덱스 `ix_social_identities_user_id`만 제거**하고 새 unique를 만든다. 생성 실패 시 서비스 쓰기를 재개하지 않는다. 사용자 문서는 삭제하지 않는다.
5. 새 unique의 key·name·unique를 재조회한다. 구 nonunique가 남으면 새 unique가 정상 생성된 뒤 불필요한 구 인덱스만 제거한다. 이 저장소의 auto-index 생성은 기존 인덱스 이행을 자동 처리하는 도구가 아니다.
6. 신규 `account_recovery_attempts`, `account_recovery_proofs`, `account_recovery_budgets`: 기본 `_id` unique 및 `cleanupAt` TTL expireAfterSeconds=0 확인. proof `_id`가 HMAC 버전별 unique claim이다. 기존 active phone fingerprint partial unique는 유지한다.
7. 코드·앱 배포와 인덱스 확인 후 계정 찾기 ON. 단일 SNS 정책만 먼저 사용자에게 출시하지 않는다. 준비가 안 되면 전체 출시를 보류한다.

### 출시 QA

- 실제 Mongo replica set에서 proof claim/attempt CAS/계정 읽기의 commit·rollback·응답 유실·동시 소비·키 회전 경합.
- 앱별 OTP와 PHONE auth_time, token refresh, 기존 Guest/주 SNS 세션 보존, 취소/재시작/429/503.
- FOUND/NOT_FOUND/정지/탈퇴/힌트 없음/Apple relay/Google·Apple·Kakao 가입·승격·등록 SNS 로그인·다른 SNS 거절.
- phone-only UID 후속 가입/credential 충돌, 인덱스 전환, 기존 PRIVATE KEY/JWKS/session epoch/전화 소유 계약 회귀.
- Guest merge UI는 기존 LC/Billing consumer E2E 출시 gate를 유지. 계정 찾기 성공과 기록 이전 완료는 무관하다.

## 부록: 구현과 검증 근거

- [전화 증명 서비스](../../src/main/java/web/tosunsaeng/identity/domain/auth/accountrecovery/AccountRecoveryService.java), [Firebase adapter](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/infrastructure/firebase/FirebaseSdkAdminClient.java).
- [현재 계정 resolver](../../src/main/java/web/tosunsaeng/identity/domain/auth/accountrecovery/RecoveryAccountResolver.java), [HMAC](../../src/main/java/web/tosunsaeng/identity/domain/auth/accountrecovery/RecoveryHasher.java).
- Attempt의 `proofIds=null`이 PENDING, 비null이 CONSUMED다. 별도 state/consumedAt 중복 필드 대신 proofIds/retryUntil을 원자 변경한다. TTL cleanup은 인증 유효기간 판정과 별개이며 비동기 삭제다.
- 전용 proof HMAC은 project + 설정된 tenant + uid + auth_time + 정규화 전화번호를 길이 접두어로 결합한다. iat/원문 token은 포함하지 않는다. 원문 개인정보는 저장하지 않는다.
- [계정 찾기 테스트](../../src/test/java/web/tosunsaeng/identity/domain/auth/accountrecovery), [단일 SNS 테스트](../../src/test/java/web/tosunsaeng/identity/domain/auth/federation/application/SingleSocialIdentityPolicyTests.java), [로컬 UI](../../tools/auth-test/README.md).
- legacy ProviderChangeTests의 복수 SNS fixture는 과거 보안 상태 머신 검증 전용이다. 이 fixture에서만 userId unique를 제외하며 실제 RepositoryIntegrationTests는 새 unique를 검증한다.
- 승인 계획 [TMI-192 계획서](single-sns-account-recovery-implementation-plan.md), 상위 TMI-136. 외부 API·Learning Core/Billing 구현을 이 저장소로 복사하지 않았다.
