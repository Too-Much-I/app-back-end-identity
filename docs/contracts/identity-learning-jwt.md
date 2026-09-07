# Identity–Learning Core JWT 계약

이 문서는 Identity Service가 발급하는 Access Token을 Learning Core가 검증하고 사용하는 서버 간 계약을 정의한다.

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

신규 발급·재발급하는 사용자 JWT에는 다음 Claim이 반드시 포함되어야 한다. `account_type` 추가 이전의 토큰은 아래 호환·배포 계약을 따른다.

| Claim | 계약 |
| --- | --- |
| `sub` | UUID 문자열 형식의 실제 `userId` |
| `iss` | Identity Service의 발급자 식별값 |
| `aud` | `tosunsaeng-learning-core` 하나를 포함하는 JSON 배열 |
| `iat` | 토큰 발급 시각 |
| `exp` | 토큰 만료 시각 |
| `jti` | Access Token의 고유 식별값 |
| `scope` | 허용된 접근 범위를 공백으로 구분한 문자열 |
| `account_type` | 문자열 `MEMBER` 또는 `GUEST`. Identity의 현재 UserAccountType에서 결정 |

JWT에는 검증과 인가에 필요한 최소 Claim만 포함한다. 이메일, 닉네임 전체, 비밀번호 또는 비밀번호 해시, Refresh Token 같은 개인정보와 자격증명은 포함하지 않는다.

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
- Learning Core가 허용하는 `audience`는 정확히 `tosunsaeng-learning-core`다.
- Learning Core는 매 요청마다 Identity Service의 토큰 확인 API를 호출하지 않고 JWKS 기반으로 토큰을 검증한다.
- RSA Private Key는 Identity Service 밖으로 공유하지 않는다.

현재 JWKS에는 단일 Active Key의 Public Key만 제공한다. 향후 Key Rotation 시에는 새 Active Key로 발급을 전환한 뒤에도 이미 발급된 Access Token의 최대 유효 기간과 캐시 정책을 고려해 이전 Public Key를 JWKS에 일정 기간 유지해야 한다.

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
