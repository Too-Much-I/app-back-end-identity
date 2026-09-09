# Billing public reader용 Identity 사용자 JWT 확장 계획

- 작성일: 2026-09-07
- 상태: 2026-09-08 구현·전체 테스트·PR #40 develop 병합 완료. merge commit `03e4c5a34e4496e1e1329a95a522350070fbaf5c`. 운영 배포·staging E2E 미확인.
- Jira: [TMI-127](https://to-teacher.atlassian.net/browse/TMI-127), 사용자 승인 후 완료 댓글 등록 및 상태·Resolution 모두 `완료` 확인. 댓글 ID `10081`.
- 기준: Identity `develop@fe9c7f6` (account_type PR #39), Billing `develop@eb0ae14` (무료 reader PR #9, 직전 교차 검토 기준).
- 목적: 앱이 기존 Identity 사용자 Access Token으로 Billing `GET /api/v1/entitlements`를 호출하도록 audience와 조회 권한을 추가한다.

## 1. 5줄 결론

1. 모든 사용자 Access Token에 기존 audience와 `tosunsaeng-billing`을 함께 발급한다. 기존 `JWT_AUDIENCE` 설정과 Identity 자체 validator는 유지한다. [현재 구현](../../src/main/java/web/tosunsaeng/identity/global/security/jwt/JwtConfiguration.java)
2. Guest·MEMBER 모두 기존 scope에 `billing:read`를 추가한다. defaultScopes뿐 아니라 명시 scope도 공통 발급기에서 합성한다. [발급기](../../src/main/java/web/tosunsaeng/identity/global/security/jwt/JwtAccessTokenIssuer.java)
3. 기존 7개 발급 경로의 `account_type`·사용자 상태 검사·응답·RefreshSession 흐름을 보존하고 신규 발급 API는 만들지 않는다. [경로표](#61-전체-사용자-발급-경로)
4. workload JWT·`billing:purchase`·혜택 수량/전화번호 claim은 추가 대상이 아니다. [workload 발급기](../../src/main/java/web/tosunsaeng/identity/global/security/jwt/JwtWorkloadIdentityCredentialProvider.java)
5. Billing OFF 선배포 이후 Identity를 배포하고 새 토큰으로 staging 검증을 완료한 뒤 Billing·프론트를 활성화한다. 기존 토큰은 정상 refresh/재로그인으로 교체한다. [배포](#54-배포와-기존-앱-전환)

## 2. 사용자가 반드시 읽어야 하는 내용

### 2.1 구현 결과

현재 표준 설정에서는 다음 사용자 claim을 발급한다. 예시는 추가·유지할 필드의 부분 표현이며 실제 credential은 아니다.

```json
{
  "aud": ["tosunsaeng-learning-core", "tosunsaeng-billing"],
  "scope": "billing:read learning:read learning:write",
  "account_type": "MEMBER"
}
```

Guest는 동일 audience와 read 권한을 받으며 `account_type`만 `GUEST`다. `billing:read`는 본인 사용권 조회 권한이지 무료권 지급·시험 시작·구매 권한이 아니다. 혜택 자격과 이용 가능성은 Billing이 판단한다.

### 2.2 포함·제외 범위

포함:

- 공통 사용자 토큰 발급기의 audience 배열 확장 및 scope 합성.
- 기존 설정·Identity 자체 검증·Learning Core audience 소비와의 호환 검증.
- Guest 생성, 로컬 로그인, Firebase 가입·로그인, 승격·병합, refresh의 전체 경로 회귀.
- JWT 계약 문서·인계 문서와 테스트·배포 체크리스트 갱신.

제외:

- Billing·Learning Core 코드, public ingress·AWS·Mongo migration 변경.
- Billing reader의 DTO·이용 횟수 계산, 프론트 화면 구현.
- 구매 권한 자동 부여, 결제 SDK·API, 무료권·전화번호 정보의 JWT 포함.
- workload 발급 목적·audience·scope·issuer 변경.
- Refresh Token 원자성·응답 유실·logout-all Firebase revoke와 다른 후속 인증 작업.
- 기존 Identity API 전체에 Billing audience·scope 또는 account_type 필수 검증 추가.

새 토큰으로 두 서비스를 호출할 수 있게 되지만, 기존 토큰의 내용은 바뀌지 않는다. Identity·Learning Core는 기존 검증을 유지하고 Billing은 필요한 audience/read가 없는 토큰을 계속 거절한다.

## 3. 사용자 결정사항과 구현 선택

추가 제품 정책 선택은 필요하지 않다. 기존 설정과 사용자를 유지하기 위한 구현 기준은 다음과 같다.

| 항목 | 선택 및 이유 |
| --- | --- |
| audience 설정 | `JwtProperties.audience`와 `JWT_AUDIENCE`는 기존 primary audience 문자열로 유지. 자체 validator가 같은 설정을 사용하므로 이를 배열 문자열로 대체하지 않음 |
| 발급 audience | primary audience 다음에 고정 `tosunsaeng-billing` 추가, 중복 제거, 순서 고정. 일반 운영에서는 LC/Billing 두 값 |
| 확장 범위 | 임의 audience를 받는 API나 범용 목록 설정은 만들지 않음. 이번에 승인된 Billing 대상만 공통 발급 정책에 추가 |
| scope | 기존 선택 규칙을 적용한 집합에 고정 `billing:read`를 합집합으로 추가. 정렬·중복 제거·기존 유효성 검증 유지 |
| 계정 유형 | 기존 필수 UserAccountType 인자를 유지. GUEST/MEMBER 모두 조회 권한, null 유형은 계속 거절 |
| feature flag | Identity에 별도 발급 flag는 추가하지 않음. 코드 배포 후 사용자 토큰에 일관 적용하고 공개 노출은 Billing의 기존 reader/connector flag로 제어 |
| API | 기존 발급·refresh 응답 사용. Billing 전용 사용자 토큰 교환 endpoint나 추가 로그인 절차 없음 |

환경 입력인 issuer·JWKS 주소, 실제 TTL·키 회전 상태, Billing 배포 준비 여부는 운영 활성화 전에 확인한다. 이를 저장소 기본값으로 확정하지 않는다.

## 4. 주요 위험과 미확인 사항

| 위험 | 대응·확인 기준 |
| --- | --- |
| `JWT_AUDIENCE`를 쉼표 문자열로 변경 | 두 audience가 아니라 쉼표를 포함한 한 값이 되므로 금지. 기존 설정 그대로 배열을 코드에서 구성 |
| 자체 JWT validator도 Billing 필수로 변경 | 구형 토큰 일괄 거절이 발생하므로 기존 primary audience 포함 검사 유지 |
| defaultScopes만 수정 | 명시 scope 경로에는 적용되지 않으므로 선택 이후 공통 합집합으로 보장 |
| 명시 scope를 defaultScopes와 전부 합침 | 기존 최소 권한 요청에 learning 권한을 추가할 수 있으므로 explicit scope가 있으면 기존처럼 그것을 기초 집합으로 사용 |
| 사용자·workload가 같은 encoder/설정을 사용 | 합성은 JwtAccessTokenIssuer 안에서만 수행. 공용 JwtEncoder나 서명 설정에 자동 claim 추가 금지 |
| 기존·신규 발급 instance 혼재 | 구버전 instance에서 refresh하면 Billing 권한이 다시 빠질 수 있으므로 rollout 완료 전 앱 기능 활성화 금지 |
| Billing 401/403에서 반복 refresh | DB·환경·scope 설정 오류일 수 있음. 앱에서 무한 재발급하지 않고 제한된 갱신 이후 실패로 안내 |
| 실제 운영 계약과 로컬 테스트 차이 | 표준 fixture 테스트 외 실제 staging JWT/JWKS·두 서비스 호출·키 회전 gate 별도 확인 |

Billing reader는 기존 stateless 사용자 JWT의 즉시 탈퇴 차단을 새로 보장하지 않는다. 이 변경으로 deny marker·수명·권한 철회 정책을 확장하지 않는다.

## 5. 현재 작업과 직접 관련된 구현 설명

### 5.1 audience 합성

`JwtAccessTokenIssuer`에서 private helper 또는 동등한 내부 로직으로 사용자 audience를 만든다.

```text
user audiences = distinct-in-order [properties.audience(), "tosunsaeng-billing"]
```

- 기존 primary audience는 보존하고 추가 값을 포함하는 JSON 배열로 encode한다.
- `JwtProperties`의 기존 record 생성자·바인딩 필드와 `JwtConfiguration.jwtDecoder`의 required audience는 변경하지 않는다.
- `JwtAudienceValidator`는 이미 배열의 contains를 사용하므로 신규 배열을 허용한다. 이를 배열 전체 일치 검사나 두 값 모두 필수 검사로 바꾸지 않는다.
- 표준 배포에서 `JWT_AUDIENCE=tosunsaeng-learning-core`가 유지되는지 확인한다. primary 설정을 Billing으로 교체하는 배포는 본 계획이 아니다.
- 반환 목록은 호출마다 독립적이거나 immutable이어야 하며 설정 객체를 수정하지 않는다.

### 5.2 scope 합성

```text
base scopes = request scopes가 null/empty이면 기존 defaultScopes
              그렇지 않으면 기존 explicit scopes
issued scopes = sorted-distinct(base scopes ∪ {"billing:read"})
```

- blank/null/whitespace를 포함한 기존 잘못된 scope 입력의 거절을 유지한다. 사용자 제공 Request DTO에 scope 입력을 추가하지 않는다.
- `billing:read`가 이미 있으면 한 번만 발급한다.
- 예를 들어 explicit `{profile:read}`는 `billing:read profile:read`가 되며 default learning scope를 덧붙이지 않는다.
- GUEST/MEMBER 모두 동일 read 추가 규칙을 사용한다. 기존 UserAccountType null 거절과 계정 상태 검사를 유지한다.
- `billing:purchase`를 기본 설정이나 신규 합성 정책에 넣지 않는다. 현재 정상 앱 발급 경로의 토큰에 해당 권한이 없는지 검사한다.
- caller의 Set과 properties.defaultScopes를 변경하지 않아 후속 요청·workload 호출에 영향이 없어야 한다.

### 5.3 유지할 외부 계약

| 계약 | 처리 |
| --- | --- |
| `sub` | 현재 신뢰된 User UUID 유지. merge는 target, upgrade는 같은 user |
| `account_type` | 현재 UserAccountType의 MEMBER/GUEST. refresh는 DB 현재 User 기반 |
| `iss`, RS256, `typ=JWT`, `kid`, JWKS | 기존 발급·서명 구조 유지 |
| `iat`, `exp`, `jti`, TTL | 현재 계산·UUID 생성·운영 설정 유지. 사용자 nbf 필수 추가 없음 |
| `aud` | 기존 primary 보존 + Billing 추가 |
| `scope` | 기존 선택 scope 보존 + billing:read 추가 |
| 공개 URL·Request/Response | 변경 없음. BaseResponse, 만료 응답 단위, profile `accountType` 유지 |
| RefreshSession·Refresh Token | 생성·회전·폐기·오류·보관 규칙 변경 없음 |
| workload JWT | USER_WITHDRAWN/USER_MERGED 고정 audience·issuer·sub·TTL·nbf 유지. Billing 사용자 audience·scope·account_type 비포함 |

### 5.4 배포와 기존 앱 전환

1. **Billing 준비:** PLAN-007 reader·public connector OFF 배포 상태와 legacy 귀속 증빙 이관·인프라 검증 담당을 확인한다. 이 작업에서 해당 flag를 켜거나 이관을 실행하지 않는다.
2. **Identity 구현 검증:** 전체 발급 경로와 기존 소비자 호환 테스트를 통과한다. 사용자가 commit·push·PR·merge한다.
3. **Identity 배포:** 신규 버전이 Guest·MEMBER 모두 Billing audience/read를 발급하는지 확인하고 모든 구버전 발급 instance의 종료 시각과 배포 버전을 기록한다.
4. **staging 검증:** 제한된 환경에서 Billing reader를 활성화하고 신규 발급·refresh 토큰으로 Identity profile, Learning Core 기존 보호 API, Billing GET의 성공을 확인한다. LC-only·scope 누락·workload·잘못된 서명/issuer·만료 토큰의 거절도 확인한다.
5. **프론트 토큰 갱신:** 기존 사용자는 기존 refresh API 또는 재로그인으로 갱신한다. 동일 계정의 concurrent refresh와 응답 유실은 기존 정책을 따르며 Billing 호출 실패로 무제한 refresh하지 않는다.
6. **운영 활성화:** Billing ALB/SG/JWKS·replica set·legacy coverage·consumer readiness 조건을 확인하고 reader, 이후 앱 화면을 점진적으로 활성화한다.

Billing은 audience 누락을 401, 유효한 인증에서 billing:read 누락을 403으로 분리한다. 신뢰된 JWKS 장애나 조회 503을 사용자 로그아웃·즉시 재발급으로 해결하려고 하지 않는다.

Billing 전환은 새 토큰 획득으로 진행할 수 있으므로 일괄 TTL 대기를 필수로 두지 않는다. 앱이 별도 갱신 없이 모든 구형 토큰 소멸을 기다리는 출시 방식을 선택한다면 마지막 구버전 발급 instance 종료 + 실제 구버전 최대 TTL + 해당 verifier skew로 경계를 계산한다. 이와 별개인 Learning Core Challenge account_type 활성화의 기존 TTL·skew gate를 완료한 것으로 간주하지 않는다.

rollback은 우선 Billing reader·화면을 OFF로 전환한다. Identity를 구버전으로 되돌리면 이후 발급에 Billing 권한이 빠질 수 있음을 운영 인계에 포함한다. Billing에서 LC-only 토큰을 허용하는 fallback은 만들지 않는다. 추가 audience가 들어간 기존 사용자 토큰은 기존 Identity validator와 호환되어야 한다.

### 5.5 완료 인계 항목

- 사용자 commit/PR/merge와 배포 환경·버전, 구버전 발급 instance 종료 UTC 시각.
- 전체 테스트 결과 및 7개 발급 경로·workload 제외·구형 사용자 토큰 호환 증빙.
- 운영 `JWT_AUDIENCE`, 실제 Access Token TTL과 대상 verifier skew의 확인 여부.
- staging에서 새 사용자 토큰의 aud/scope/account_type 검사와 세 서비스 호출 결과. 실제 Token 원문·credential·키는 공유하지 않는다.
- Billing reader 활성화·legacy coverage·프론트 갱신 및 재응시 1회 표시의 완료/미완료 상태를 별도 구분한다.

## 6. 부록 — 구현 경로·검증 기준·근거

### 6.1 전체 사용자 발급 경로

`AccessTokenIssuer.issue(userId, accountType, scopes)`의 signature와 서비스별 인증·상태 검사 순서는 유지한다. 모든 서비스는 공통 발급 정책을 통과하므로 권한을 각 서비스에서 따로 조립하지 않는다.

| 경로 | 서비스 | 반드시 확인할 값 |
| --- | --- | --- |
| Guest 생성 | `GuestAuthService` | 새 userId·GUEST·두 audience·read |
| 로컬 로그인 | `LoginService` | 인증된 현재 User·account_type·두 audience·read |
| Firebase 가입 | `FirebaseSignupService` | 가입 성공한 MEMBER·두 audience·read |
| Firebase 로그인 | `FirebaseExchangeService` | 현재 ACTIVE MEMBER·두 audience·read |
| 재발급 | `TokenReissueService` | 현재 DB User의 유형·두 audience·read. 구형 토큰 값을 복사하지 않음 |
| Guest 승격 | `FirebaseGuestUpgradeService` | 같은 userId·MEMBER·두 audience·read |
| Guest 병합 | `FirebaseGuestMergeService` | target userId·MEMBER·두 audience·read; source 발급 없음 |

서비스 소스는 `src/main/java/web/tosunsaeng/identity/domain/auth` 하위에 있다. 구현 시작 시 추가 발급 경로와 별도 JwtClaimsSet 생성 지점을 다시 검색한다.

### 6.2 변경 예상 파일

| 파일 | 변경 내용 |
| --- | --- |
| [JwtAccessTokenIssuer.java](../../src/main/java/web/tosunsaeng/identity/global/security/jwt/JwtAccessTokenIssuer.java) | 고정 Billing audience/read 상수와 공통 합성 |
| [JwtAccessTokenIssuerTests.java](../../src/test/java/web/tosunsaeng/identity/global/security/jwt/JwtAccessTokenIssuerTests.java) | 양 계정 유형·default/explicit scope·기존 claim·불변성 테스트 |
| [JwtDecoderTests.java](../../src/test/java/web/tosunsaeng/identity/global/security/jwt/JwtDecoderTests.java) | primary 포함 배열·LC-only legacy 호환, primary 없는 배열 거절 |
| [SecurityIntegrationTests.java](../../src/test/java/web/tosunsaeng/identity/global/config/SecurityIntegrationTests.java) | 기존 공개 API·profile·scope 변환 회귀 |
| auth 서비스 테스트·필요시 신규 발급 경로 contract 테스트 | 실제 공통 issuer가 생성한 claim 검사와 서비스 인자 검증 조합으로 7경로 누락 방지 |
| [JwtWorkloadIdentityCredentialProviderTests.java](../../src/test/java/web/tosunsaeng/identity/global/security/jwt/JwtWorkloadIdentityCredentialProviderTests.java) | 두 workload purpose 모두 사용자 audience/read 비포함 |
| [identity-learning-jwt.md](identity-learning-jwt.md) | 사용자 aud·scope 계약과 전환·운영 인계 업데이트 |
| 본 계획서·WORKLOG·CURRENT_STATE | 실제 구현·검증·미확인 항목 기록 |

`JwtProperties`, `JwtConfiguration`, `application.yml`의 기존 설정 형식 변경은 예정하지 않는다. 임의 audience 목록·새 필수 환경변수·새 dependency를 도입하지 않는다. untracked 프론트 가이드는 기존 사용자 변경이므로 구현 시 수정 필요 부분을 확인하고 보존하며 전체 재작성하지 않는다.

### 6.3 테스트 매트릭스

| ID | 검증 | 기대 |
| --- | --- | --- |
| T01 | GUEST/MEMBER 실서명 토큰 | 기존 primary·Billing audience가 중복 없이 포함, read scope 정확히 한 번, 기존 account_type 유지 |
| T02 | null/empty scope | 기존 defaultScopes + billing:read |
| T03 | explicit scope·read 이미 포함 | explicit 집합 보존 + read, default learning scope 강제 추가 없음, 중복 없음 |
| T04 | 잘못된 scope·null accountType | 기존 안전한 발급 거절 유지, MEMBER 기본 처리 없음 |
| T05 | 반복 발급·동시 요청·설정/caller Set | 입력 집합과 defaultScopes 불변, claim 오염 없음, scope 순서 안정 |
| T06 | 7개 전체 경로 | 신규 claim 누락 없음, endpoint/응답/RefreshSession 회귀 유지 |
| T07 | upgrade→refresh / merge→refresh | 동일 승격 user 또는 merge target, MEMBER/read/두 audience 유지 |
| T08 | WITHDRAWN/MERGED source·폐기 refresh | 기존 발급 차단·오류 유지 |
| T09 | 신규 다중 aud와 기존 LC-only 토큰 | Identity 기존 API와 LC primary validator 호환, Billing-only로 Identity 기존 required audience 우회 불가 |
| T10 | Billing 소비자 경계 fixture | 두 audience/read 정상, LC-only 401 의미·read 누락 403 의미, Guest read 가능. Billing 실제 구현 검증은 staging에서 수행 |
| T11 | workload 두 purpose | 기존 전용 audience만, billing:read/account_type 없음; 사용자와 workload 교차 사용 거절 |
| T12 | 기존 claim·서명·API·개인정보 | RS256/kid/iss/sub/iat/exp/jti/TTL 유지, 구매 권한 자동 추가 없음, 원문 Token·credential 비로깅 |
| T13 | 기존 설정 바인딩 | 기존 JWT_AUDIENCE 단일 값과 환경변수 미변경으로 기동, 불필요한 새 설정 요구 없음 |

Mock issuer 인자 검증만으로 새 JWT claim 검증을 대신하지 않는다. 실서명/검증 테스트와 각 서비스 경로 테스트를 함께 사용한다. 외부 Atlas·Firebase·Billing·Learning Core·AWS를 로컬 단위 테스트에서 호출하지 않는다. Billing 검증 소스를 Identity로 복사하지 않고 승인 계약에 따른 테스트 fixture와 실제 staging 증빙을 구분한다.

### 6.4 완료 기준

1. 모든 정상 사용자 발급·재발급에 두 audience와 billing:read가 포함되고 기존 scope·account_type이 유지된다.
2. explicit/default scope와 legacy 설정·구형 토큰 호환 및 workload 분리 테스트가 통과한다.
3. `./gradlew clean test`와 `git diff --check`를 실행하고 총 테스트·실패·오류·skip을 실제 결과로 기록한다.
4. 변경 파일과 예상 밖 diff를 검토하고 기존 사용자 변경을 보존한다. commit·push는 사용자가 수행한다.
5. 코드 완료와 운영 활성화를 분리한다. 배포되지 않았거나 staging 미검증이면 미완료 gate를 명시하고 운영 사용 가능으로 보고하지 않는다.

### 6.5 현재 구현 근거와 상대 계약

- [AccessTokenIssuer](../../src/main/java/web/tosunsaeng/identity/global/security/jwt/AccessTokenIssuer.java): 신뢰된 계정 유형 인자를 갖는 공통 사용자 발급 경계.
- [JwtProperties](../../src/main/java/web/tosunsaeng/identity/global/security/jwt/JwtProperties.java): 기존 단일 audience와 defaultScopes 설정.
- [JwtAudienceValidator](../../src/main/java/web/tosunsaeng/identity/global/security/jwt/JwtAudienceValidator.java): required audience의 배열 포함 검사.
- [application.yml](../../src/main/resources/application.yml): 기존 JWT_AUDIENCE·JWT_ACCESS_TOKEN_TTL 설정. fallback은 운영 유효값 증빙이 아님.
- [Billing PLAN-007](../../../billing/docs/plans/PLAN-007-public-free-entitlement-query.md): public GET, 사용자 인증과 Identity 후속 요구.
- [Billing 배포 runbook](../../../billing/docs/runbooks/PLAN-007-public-reader-rollout.md): 기본 OFF·public ingress·legacy attribution coverage와 rollout.
- [Billing JWT decoder](../../../billing/src/main/java/web/tosunsaeng/billing/global/config/security/IdentityUserJwtDecoder.java): RS256·typ/kid·issuer·Billing audience·필수 시간/식별 claim·최대 60초 skew 계약. account_type으로 MEMBER-only를 강제하지 않음.

상대 저장소 링크는 로컬 sibling checkout 기준이며 각 저장소 외부 파일을 이 작업에서 수정하지 않는다. 직전 Identity 640개·Billing 236개 테스트 통과는 과거 검증 결과이며 이번 계획서 작성에서 테스트를 재실행한 결과가 아니다.

### 6.6 TMI-127 구현 결과 — 2026-09-08

- 브랜치: `feat/TMI-127-billing-reader-jwt`. 사용자 작업 브랜치를 유지했고 commit·push·PR·배포는 수행하지 않았다.
- 런타임 변경은 `JwtAccessTokenIssuer.java` 한 파일이다. 사용자 audience를 기존 primary + 고정 Billing으로 구성하고 기존 선택 scope에 billing:read를 합성한다. 서비스·설정·decoder·workload 런타임 코드는 변경하지 않았다.
- `SignedUserTokenFixture`를 추가하여 Guest·로컬 로그인·Firebase 가입·exchange·upgrade·merge·refresh의 기존 서비스 테스트가 실제 공통 issuer와 RSA 서명/검증을 사용하게 했다. 기존 Mock Repository·외부 인증 경계는 유지했다.
- `./gradlew clean test`: 123개 suite, 648개 테스트, 실패·오류·건너뜀 0개. 첫 sandbox 실행의 Gradle 캐시 접근 제한 후 승인된 실행으로 검증했다. `git diff --check`도 통과했다.

| 계획 검증 | 이번 근거 |
| --- | --- |
| T01–T05 | JwtAccessTokenIssuerTests: GUEST/MEMBER 실제 claim, default/null/explicit, 중복 제거, malformed scope·null 유형 거절, 동시 발급 20건 및 입력·설정 불변성 |
| T06–T08 | 7개 서비스 테스트와 SignedUserTokenFixture, upgrade→refresh와 merge→refresh, 기존 탈퇴·병합·폐기 세션 거절 회귀 |
| T09–T10 | JwtDecoderTests의 다중 audience·legacy·Billing-only 거절 및 audience/read 분리 fixture, SecurityIntegrationTests의 실제 Identity HTTP 접근·권한 변환 |
| T11 | JwtWorkloadIdentityCredentialProviderTests: 같은 encoder 사용 전후 사용자/workload claim 분리, 두 workload purpose와 사용자 토큰의 교차 사용 거절 fixture |
| T12–T13 | 기존 RS256·kid·TTL·API·민감정보 비노출 테스트와 기존 설정의 Spring 테스트 기동. 런타임 설정 변경 없음 |

Billing HTTP 응답 401/403, 실제 Learning Core consumer, 외부 JWKS·키 회전, 운영 배포 및 staging E2E는 이번 로컬 테스트로 완료된 것으로 간주하지 않는다. 운영 TTL·구버전 발급 instance 종료 시각도 미확인이다. Billing reader 활성화·프론트 정상 재발급·재응시 1회 화면 표시는 후속 작업이다.

기존 미커밋 WORKLOG·CURRENT_STATE·계획서·프론트 인증 가이드는 보존했다. 계획된 범위 밖 런타임 변경은 없으며, JWT 계약 문서의 오래된 단일 공개키 설명은 기존 다중 공개키 지원 코드에 맞게 바로잡았다.

### 6.7 Jira 완료 댓글 작성 근거 — 2026-09-08 병합 증빙을 포함하여 등록 완료

TMI-127 사용자 JWT Billing audience·billing:read 확장 로컬 구현 완료. 공통 JwtAccessTokenIssuer, 7개 발급 경로 테스트와 SignedUserTokenFixture, JWT decoder/workload/security 테스트 및 계약 문서를 수정했다. 전체 123개 suite·648개 테스트와 git diff --check 통과. 기존 API·account_type·RefreshSession·workload 경계는 유지했다. PR 병합·운영 배포·구버전 발급 종료·운영 TTL 및 Billing staging E2E는 미확인이다.
