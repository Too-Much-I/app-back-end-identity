# Identity–Learning Core JWT 계약

이 문서는 Identity Service가 발급하는 사용자 Access Token을 Learning Core와 Billing public reader가 검증하고 사용하는 계약을 정의한다. 서버 간 workload JWT는 별도 계약을 따른다.

## 사용자 식별 계약

- 실제 `userId`는 UUID 문자열이다.
- JWT의 `sub` Claim은 실제 `userId`다.
- Learning Core는 검증을 마친 JWT의 `sub`에서 사용자 식별자를 얻는다.
- 클라이언트는 `userId`를 별도로 전송하지 않는다.
- 클라이언트가 임의로 전송한 `userId`는 인증 근거로 신뢰하지 않는다.

## Access Token 형식

Signature Algorithm은 RS256이며 Access Token의 기본 TTL은 `PT30M`이다. Identity Service만 RSA Private Key를 보유하며, 외부 서비스에는 Private Key를 배포하지 않는다.

JWT Header에는 다음 값이 필요하다.

| Header | 계약 |
| --- | --- |
| `alg` | `RS256` |
| `typ` | `JWT` |
| `kid` | 필수. 서명에 사용한 RSA Key를 식별하는 값 |

신규 발급·재발급하는 사용자 JWT에는 다음 Claim이 반드시 포함되어야 한다. `account_type` 및 Billing audience/read 추가 이전의 토큰은 아래 호환·배포 계약을 따른다.

| Claim | 계약 |
| --- | --- |
| `sub` | UUID 문자열 형식의 실제 `userId` |
| `iss` | Identity Service의 발급자 식별값 |
| `aud` | 기존 primary audience 다음에 `tosunsaeng-billing`을 포함하는 JSON 배열. 표준 설정은 `["tosunsaeng-learning-core", "tosunsaeng-billing"]`. 중복 제거·순서 고정 |
| `iat` | 토큰 발급 시각 |
| `exp` | 토큰 만료 시각 |
| `jti` | Access Token의 고유 식별값 |
| `scope` | 기존 선택 권한에 `billing:read`를 합성한 공백 구분 문자열. 정렬·중복 제거 |
| `account_type` | 문자열 `MEMBER` 또는 `GUEST`. Identity의 현재 UserAccountType에서 결정 |

JWT에는 검증과 인가에 필요한 최소 Claim만 포함한다. 이메일, 닉네임 전체, 비밀번호 또는 비밀번호 해시, Refresh Token 같은 개인정보와 자격증명은 포함하지 않는다.

## Billing public reader 사용자 인증 확장 — TMI-127

- 앱은 기존 로그인·재발급 API에서 받은 같은 사용자 Access Token으로 Billing `GET /api/v1/entitlements`를 호출한다. 별도 로그인·발급 API는 없다.
- `JwtAccessTokenIssuer`는 모든 사용자 발급 경로에 고정 `tosunsaeng-billing` audience와 `billing:read`를 추가한다. GUEST/MEMBER 모두 본인 조회가 가능하며 무료권 지급·구매 권한을 뜻하지 않는다. 혜택 자격과 이용 상태는 Billing이 판단한다.
- 기존 `JWT_AUDIENCE`와 `JwtProperties.audience`는 primary audience 단일 문자열로 유지한다. 쉼표 문자열로 변경하거나 Billing으로 교체하지 않는다. Identity·Learning Core의 primary audience 포함 검증은 그대로 유지한다.
- scope 인자가 null/empty이면 기존 defaultScopes를, 아니면 explicit scopes를 기초로 선택한 뒤 `billing:read`만 추가한다. explicit scope에 default learning 권한을 강제로 넣지 않으며 입력·설정 집합도 변경하지 않는다.
- `billing:purchase` 자동 추가, 전화번호·혜택 수량 claim, workload 발급 경로 변경은 없다. 기존 API 응답·RefreshSession·서명·TTL은 유지한다.
- 기존 사용자 토큰에 Billing audience/read가 없어도 Identity 기존 API는 이를 이유로 일괄 거절하지 않는다. Billing에서는 audience 부재는 인증 실패, 유효한 인증에서 read 권한 부재는 인가 실패이며 이를 우회하는 fallback은 없다.
- 공통 issuer·decoder·SecurityIntegrationTests와 7개 서비스 경로에서 실제 서명/검증을 사용한다. `SignedUserTokenFixture`는 서비스의 발급 호출을 실제 issuer에 위임하여 응답 claim을 확인한다. upgrade→refresh, merge→refresh 및 legacy DB account type도 검증한다.

### Billing 배포·프론트 전환 계약

1. Billing reader/public connector OFF 선배포와 인프라·legacy attribution 준비 상태를 확인한다.
2. Identity 새 버전 배포를 완료하고 구버전 발급 instance가 모두 종료됐는지 확인한다. Identity 발급용 별도 feature flag나 새 필수 환경변수는 추가하지 않는다.
3. staging에서 새 토큰으로 Identity profile·Learning Core 보호 API·Billing reader를 검증한다. 로컬 audience/scope fixture는 실제 Billing HTTP E2E 증빙을 대신하지 않는다.
4. 기존 앱은 정상 refresh/재로그인으로 새 토큰을 받는다. Billing 실패 시 무한 refresh하지 않으며 503 등 서버 장애를 로그아웃으로 해결하지 않는다.
5. Billing 운영 준비와 staging 검증 후 reader와 앱 화면을 활성화한다. 재응시 가능 시 1회 이용 가능 표시는 Billing 응답을 해석하는 프론트 후속 범위다.

새 토큰을 획득하면 Billing 전환이 가능하므로 일괄 TTL 대기는 필수가 아니다. 구형 토큰의 자연 만료를 기다리는 출시 방식이라면 구버전 발급 종료 시각 + 실제 최대 TTL + 대상 verifier skew를 적용한다. 아래 Challenge account_type 활성화 gate와는 별개다.

구현·PR 병합·운영 배포를 구분하며 실제 TTL·issuer/JWKS·키 회전·구버전 종료 시각과 staging 검증 상태를 인계한다. 상세 기준은 [TMI-127 계획서](billing-public-reader-user-jwt-plan.md)를 따른다.

## account_type 발급과 호환 계약

- 모든 사용자 발급 경로는 현재 신뢰된 `User.getAccountType()`을 공통 `AccessTokenIssuer`에 필수 인자로 전달한다. `JwtAccessTokenIssuer`는 enum 이름을 문자열로 발급하고 null 입력을 거절한다. 계정 유형을 생략하는 기존 발급 overload는 제공하지 않는다.
- 프론트 Request, Firebase claim 또는 기존 Identity Token claim으로 유형을 정하지 않는다. 특히 refresh는 RefreshSession의 userId로 현재 DB User를 조회하고 상태를 검사한 뒤 유형을 결정한다.
- 기존 Mongo 문서의 `accountType` 누락은 기존 `User.getAccountType()`의 provider 호환 해석을 유지한다. 발급기 자체가 null을 MEMBER로 기본 처리하는 것은 금지한다.
- `account_type`은 발급 시점의 snapshot이다. Guest 승격 이후 신규 발급·refresh는 같은 userId와 MEMBER를 사용하지만 이미 발급된 GUEST Token 자체가 바뀌지는 않는다.
- 기존 Identity API 전체에 `account_type` 필수 validator를 추가하지 않는다. 유형 claim 없는 구형 Token도 기존 서명·issuer·audience·만료·사용자 상태 검증을 따른다.
- Learning Core의 Challenge 인가 계약은 `MEMBER`만 허용하고 `GUEST`, 누락, 알 수 없는 값은 `403 COMMON403`으로 거절하는 것이다. 실제 Challenge 인가 구현·활성화는 Learning Core가 담당한다.
- 서비스 간 workload JWT 발급 경로는 이 계약 대상이 아니며 `account_type`을 추가하지 않는다.
- RS256·kid·JWKS, 기존 claim, 공개 API URL·Request/Response, RefreshSession·Refresh Token 흐름, 프로필 응답의 `accountType` 이름은 유지한다.

### 전체 발급 경로와 검증 근거

| 발급 서비스 | account_type의 근거 | 회귀 테스트 |
| --- | --- | --- |
| `GuestAuthService` | 새 Guest User의 GUEST | `GuestAuthServiceTests` |
| `LoginService` | 인증·상태 검사를 통과한 DB User | `AuthenticationUseCaseServicesTests`, `AuthControllerTests` |
| `FirebaseSignupService` | 가입 Transaction 완료한 User의 MEMBER | `FirebaseSignupServiceTests` |
| `FirebaseExchangeService` | 현재 ACTIVE MEMBER User | `FirebaseExchangeServiceTests` |
| `TokenReissueService` | 현재 DB User. 구형 문서 호환 유형 포함 | `RefreshTokenUseCaseServicesTests`, `GuestRefreshLifecycleTests` |
| `FirebaseGuestUpgradeService` | 승격 Transaction 성공 후 같은 User의 MEMBER | `FirebaseGuestUpgradeServiceTests` (upgrade→refresh 포함) |
| `FirebaseGuestMergeService` | merge Transaction 성공 후 target User의 MEMBER | `FirebaseGuestMergeServiceTests` |

공통 서명·claim 형식·유형 누락 거절은 `JwtAccessTokenIssuerTests`, 구형 Token의 Identity profile 접근과 `accountType` 응답은 `SecurityIntegrationTests`, workload claim 제외는 `JwtWorkloadIdentityCredentialProviderTests`에서 검증한다. 테스트 소스는 `src/test/java/web/tosunsaeng/identity` 아래에 있다.

### 배포 순서와 인계해야 할 운영 증빙

1. Identity 신규 발급 코드를 먼저 배포한다.
2. 모든 구버전 발급 instance가 종료되어 구형 claim 구성의 Token을 더 이상 발급하지 않는 UTC 시각 `T_old_issuers_stopped`를 확인한다. 여러 배포 환경·발급 instance가 있으면 대상 환경 전체를 확인한다.
3. 마지막 구버전 instance들이 사용한 **실제 운영 Access Token 최대 TTL**과 Learning Core의 **사용자 Token 검증 clock skew**를 확인한다. 서로 다른 TTL이 있었다면 최대값을 사용한다. workload JWT skew를 대신 사용하지 않는다.
4. `T_old_issuers_stopped + max_old_access_token_ttl + learning_core_user_jwt_clock_skew` 이후 Challenge MEMBER gate를 활성화한다. 대기 중 구버전 rollback·재기동이 있으면 종료 시각과 대기 경계를 다시 계산한다.

`application.yml`의 `app.jwt.access-token-ttl`은 `${JWT_ACCESS_TOKEN_TTL:PT30M}`이며 PT30M은 fallback이다. 운영 환경변수·Spring override가 적용된 유효 설정을 확인하기 전에는 운영 TTL을 30분이라고 보고하지 않는다. 저장소의 staging workflow는 현재 ECS task definition을 읽어 image만 교체하므로 저장소만으로 실제 운영 TTL을 확정할 수 없다.

| 인계 항목 | 현재 확인 상태 |
| --- | --- |
| PR·merge commit | 로컬 구현 단계. 이번 변경의 PR·merge 없음 |
| 배포 환경·배포 결과 | 이번 작업에서 배포하지 않음 |
| 구버전 발급 instance 종료 UTC 시각 | 배포 후 확인 필요 |
| 운영 Access Token 최대 TTL | 운영 유효 설정 미확인 |
| Learning Core 사용자 JWT clock skew·Challenge 활성화 가능 시각 | 대상 환경 확인 및 위 식 계산 필요 |

운영 인계에는 설정값·UTC 시각·commit 등 증빙만 공유하며 실제 Token 원문·credential·Key는 공유하지 않는다.

## Public Key 배포와 검증

- Identity Service는 `GET /.well-known/jwks.json`에서 BaseResponse로 감싸지 않은 표준 JWKS를 제공한다.
- Learning Core는 JWKS에서 `kid`에 대응하는 Public Key를 조회해 RS256 서명을 검증한다.
- Learning Core는 서명뿐 아니라 예상한 `issuer`와 `audience`를 모두 검증한다.
- Learning Core가 요구하는 audience 값은 `tosunsaeng-learning-core`다. 배열의 포함 여부를 검사하며 Billing이 추가된 배열도 허용한다. Billing public reader는 별도로 `tosunsaeng-billing`과 `billing:read`를 요구한다.
- Learning Core는 매 요청마다 Identity Service의 토큰 확인 API를 호출하지 않고 JWKS 기반으로 토큰을 검증한다.
- RSA Private Key는 Identity Service 밖으로 공유하지 않는다.

현재 JWKS는 Active Key와 `JwksRotationProperties`로 설정한 이전 Public Key를 함께 제공할 수 있다. 실제 배포에서 이전 키가 설정됐는지는 별도 확인해야 한다. Key Rotation 시에는 새 Active Key로 발급을 전환한 뒤에도 이미 발급된 Access Token의 최대 유효 기간과 verifier skew·캐시 정책을 고려해 이전 Public Key를 유지해야 한다. 이 작업은 기존 키 로딩·회전 구현을 변경하지 않는다.

JWKS endpoint의 배포 호스트와 허용할 `issuer` 값은 환경별 설정으로 관리하며, 실제 Key나 Secret을 이 문서에 기록하지 않는다.

## 클라이언트 요청

클라이언트는 보호된 Learning Core API를 호출할 때 다음 Header를 사용한다.

`Authorization: Bearer <ACCESS_TOKEN>`

클라이언트는 별도 Header, Query Parameter 또는 Request Body로 `userId`를 보내지 않는다. Learning Core는 검증된 Access Token의 `sub`만 실제 사용자 식별 근거로 사용한다.

## Python AI 경계

Python AI 연동의 `user_id`는 인증 사용자 ID가 아니라 기존 시험 식별자 의미를 유지한다.

| 연동 경로 | `user_id` 값 |
| --- | --- |
| Learning Core → Python AI | `examId` |
| Python AI → AI Callback | `examId` |

- Python AI의 `user_id`는 실제 `userId`가 아니라 `examId`다.
- AI Callback의 `user_id`도 `examId`다.
- 실제 `userId`를 Python AI 서버나 AI Callback payload로 보내지 않는다.
