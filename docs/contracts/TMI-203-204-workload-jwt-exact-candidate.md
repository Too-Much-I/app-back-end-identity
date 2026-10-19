# TMI-203·204 workload JWT 인증 exact 개정안

> 후속 상태: 2026-10-12 사용자가 Billing 수락을 전달하고 구현을 요청했다. [로컬 구현 결과](TMI-203-workload-jwt-implementation.md)에 수신/발신 구현과 실제 배포 전 미확인 항목을 분리했다. 아래 후보 작성 시점 기록은 보존한다. 신규 fixture의 Billing 실행이나 실제 환경값 확정을 의미하지 않는다.

- 날짜: 2026-10-12 / 브랜치: `feat/TMI-203-withdrawal-snapshot-feed-recovery`
- 상태: ALB·전용 JWT 방향은 승인됨. 아래 세부 규격은 **공동 검토 후보**이며 Billing 수락·구현·배포 완료가 아니다.
- 근거: [계획 §5.2.1](TMI-203-withdrawal-recovery-snapshot-feed-plan.md), [Billing 회신 검토](TMI-203-ALB-JWT-Billing-response-review-2026-10-12.md).

## 1. 5줄 결론

1. 각 서비스가 자기 전용 RSA 키로 발급하고 상대는 배포된 공개 JWK set으로 검증한다.
2. public 포트는 복구 API를 거절하고 workload 포트는 허용된 POST만 처리한다.
3. 아래 시간·헤더·키·권한 규칙을 별도 인증 fixture로 양 서버에서 검증한 뒤 동결한다.
4. 환경·issuer/audience·실제 포트/TLS 값은 확인 전 채우지 않으며 기존 source 환경을 바꾸지 않는다.
5. recovery-r1/scalar-end-1 본문·hash, LC/user JWT, 재시도·보존·삭제 조건은 유지한다.

## 2. 사용자가 반드시 읽어야 하는 내용

이번 수정은 방향만 있던 인증 계획을 구현 가능한 검토안으로 구체화한다. 기존 `RecoveryIngressVerifier`/HTTP 오류 흐름의 실제 교체는 아직 하지 않는다. JWT는 method/path/body를 서명하지 않는다. 전송 보호와 route 권한을 별도로 적용하며 jti는 재사용 방지 장치가 아니다.

실제 ALB 설정·키 생성/주입·기능 활성화는 이 문서로 승인되지 않는다. 미확정 값을 임의 기본값으로 채워 기동하는 구현은 금지한다. 필수 신뢰 설정이 없거나 모순되면 ON 기동을 실패시킨다.

## 3. 결정·공동 수락이 필요한 사항

| 항목 | 권장 후보 / 확인 책임 |
| --- | --- |
| 환경 | 배포 서비스별 Identity source environment와 Billing SANDBOX/PRODUCTION, JWT trust environment 대응을 양측 확인. test/staging을 자동으로 합치지 않는다. |
| issuer | `urn:tosunsaeng:workload:identity:{trustEnv}`, `urn:tosunsaeng:workload:billing:{trustEnv}` 후보. trustEnv 실제 값은 미확정. |
| audience | 환경·수신 서비스·목적별 단일 문자열. 아래 목적별 서로 다른 값으로 공동 동결. 문자열은 아직 미확정. |
| 네트워크 | 양측 담당자가 workload 포트·HTTPS origin·target group·SG·public internal 경로 노출 여부·ALB→task TLS/격리를 확인한다. |
| 검증 규칙 | §5의 시간/키/header/status 순서와 경계를 Billing 독립 테스트로 수락한다. |

제품 정책을 다시 고르는 단계는 아니다. 기술 항목은 Billing에 이 문서로 확인할 수 있다. 이 문서 작성 자체는 외부 전달이 아니다.

## 4. 주요 위험과 미확인 사항

- 공개 ALB의 경로 이름 또는 X-Forwarded-* 값은 내부 신뢰 증거가 아니다. 실제 connector 포트 판별이 프록시 header로 바뀌지 않는지 검증해야 한다.
- ALB→task HTTPS만으로 task 인증서 검증까지 보장된다고 주장하지 않는다. 네트워크 경계와 threat model을 함께 확인한다.
- 기존 SigV4 client의 실배포 여부는 미확인이다. 자동 fallback/무인증 허용/기존 코드의 선제 삭제는 금지한다.
- 401/403 분류는 아래 제안이며 현재 HTTP filter의 일괄403과 다르다. 이전 테스트 결과를 새 인증 검증으로 재사용하지 않는다.
- 공개키 배포 교체는 모든 replica의 수신/발급 상태 확인이 필요하다. JWT가 유효한 동안 탈취 토큰 재사용 가능성이 남는다.

## 5. 구체적인 인증 후보 규격

### 5.1 발급·키·strict 파싱

- JWS compact RS256만 허용, RSA 최소2048bit, 비어 있지 않은 kid 필수. alg=none/HS256 등은 거절한다.
- 수신 key set은 public RSA만 허용한다. private 필드, 중복 kid, 상충 alg/use/key_ops, 손상된 키는 ON 기동 오류다. use가 있으면 sig, alg가 있으면 RS256, key_ops가 있으면 verify만 허용한다. 정확 파서 예제는 fixture로 확정한다.
- 사용자 JWT/LC/cursor/support 키와 분리한다. token의 jku/x5u/jwk/x5c로 검증 키를 선택하거나 원격 URL을 조회하지 않는다. 초기 후보는 해당 header와 crit를 거절한다.
- JOSE header와 claims의 중복 JSON key, 잘못된 UTF-8, trailing JSON을 거절한다. claims의 필수값 누락/null/형 강제변환을 허용하지 않는다.
- 필수 claims: iss/sub/aud/iat/exp/jti. aud는 문자열 하나만 허용하고 배열은 거절한다. jti는 canonical lowercase UUIDv4이며 HTTP 재시도마다 새로 발급한다. 동일 이벤트/operation/body는 유지한다.
- nbf는 선택이며 null은 거절한다. 이외 claim으로 권한을 확대하지 않는다. 부가 claim 허용 범위는 공동 fixture에 명시하고 서버가 사용하지 않는다.

### 5.2 시간 경계 후보

N은 검증기의 UTC epoch second, iat/exp/nbf는 0 이상 signed64 범위의 **JSON 정수 token**이다. 문자열/소수/지수 표기/overflow는 거절한다. 차이 계산은 overflow-safe로 수행한다.

- `0 < exp - iat <= 60`
- `iat <= N + 30`
- `N < exp + 30` (같으면 만료)
- nbf가 있으면 `iat <= nbf < exp` 및 `nbf <= N + 30`
- 검증 clock을 얻을 수 없으면 업무 처리를 하지 않고 일시 장애로 분류한다. 서버 시계가 잘못됐다는 사실을 라이브러리 예외 없이 알아낼 수 있다고 가정하지 않는다. drift 관측/차단은 운영 gate다.
- 정상 교체: old+new 검증 배포 → replica 확인 → new 발급 → old 발급 종료 확인 → 최소120초 및 clock 근거 확인 → old 제거. 긴급 유출은 old 신뢰 제거가 우선이다.

### 5.3 권한 표

모든 행은 POST만 허용한다. 각 목적은 별도 audience를 가진다. 이 표의 목적명은 설명용이며 아직 실제 audience 문자열이 아니다.

| 호출 주체 sub | 목적 | 수신 route |
| --- | --- | --- |
| identity-service | purchase-withdrawal | `/internal/v1/payments/accounts/withdrawal/events` |
| identity-service | trial-eligibility | `/internal/v1/eligibility/trial/events` |
| identity-service | trial-owner | `/internal/v1/eligibility/trial/owner/events` |
| identity-service | owner-merge | `/internal/v1/owners/merge/events` |
| billing-service | withdrawal-recovery | `/internal/v1/billing/withdrawals/snapshots` |
| billing-service | withdrawal-recovery | `/internal/v1/billing/withdrawals/snapshots/status` |
| billing-service | withdrawal-recovery | `/internal/v1/billing/withdrawals/snapshots/page` |
| billing-service | withdrawal-recovery | `/internal/v1/billing/withdrawals/feed/page` |

검증된 issuer/sub/aud를 서버 설정의 source environment/consumer로 매핑한다. body·Host·forwarding header의 주장을 매핑 근거로 쓰지 않는다. prefix 일치만으로 임의 하위 경로를 허용하지 않는다.

### 5.4 ingress·오류 순서 후보

1. public connector의 internal 요청과 workload connector의 비허용 path/method는403, 업무 실행 없이 거절한다. workload 포트에서 일반 로그인/Swagger/관리 API를 노출하지 않는다. health route가 필요하면 별도로 명시한다.
2. Authorization은 한 헤더, 하나의 Bearer credential만 허용한다. 대소문자 무관 Bearer scheme + 단일 SP + compact token을 후보로 한다. 중복/쉼표 결합/공백 credential/초과크기는401이다. 헤더 상한은 양 서버 container 설정과 함께 동결해야 한다.
3. 토큰 파싱·신뢰 issuer·kid·서명·시간 실패는401. 신뢰된 issuer의 서명/시간 검증 뒤 sub/aud/route 권한 불일치는403. 미검증 claim을 근거로403 상세정보를 반환하지 않는다.
4. 인증 후에만 기존 strict body·크기·idempotency 검증을 수행한다. 실패한 인증 요청은 snapshot/feed service를 호출하지 않는다.
5. 인프라401/403 응답은 비어 있는 body와 no-store를 후보로 하며 recovery JSON을 강제하지 않는다. 기존 허용 인프라429/업무 오류 계약은 유지한다. 응답/로그에 토큰·키·raw parser 예외를 포함하지 않는다.
6. 내부 clock/검증기 장애는 인증 실패로 위장하지 않고503 일시 장애로 처리한다. 키 설정 손상은 기동 실패다. 업무 실행 전 fail-closed가 전제다.

### 5.5 구현 순서·완료 증거

1. 위 후보와 환경/신뢰/네트워크 표를 공동 수락한다. 인증 fixture는 새로운 디렉터리/revision으로 추가하고 기존 body fixture/hash는 수정하지 않는다.
2. Identity receiver: connector 격리 → JWT verifier →401/403/503 매핑 → 기존 recovery service 연결. 기본 OFF.
3. Identity sender: 이벤트4종의 명시적인 인증 모드 전환. 매 시도 새 JWT, HTTPS origin allowlist, redirect 금지, 토큰 저장 금지. 기존 event body/응답 처리/lease와 resumeGeneration 불변.
4. 포트/forwarding 위조/사용자·LC·타환경 토큰/키 교체/시간 경계/인증 OFF 및 기존 로그인 회귀를 수행한다.
5. 실제 ALB/SG·양방향 JWT·응답 유실·중복·인증 중단과 재개·복원 공동 검증 후 별도로 활성화한다.

## 6. 부록 — 검증 목록·현행 코드 경계

- 시간: TTL0/1/60/61, 미래iat30/31, exp+skew 직전/동일/직후, nbf 관계/경계, signed64 overflow.
- 파서: 중복 header/claim, aud 배열, NumericDate 1.0/1e0/문자열/null, JSON 개행·trailing/UTF-8, 비정상 compact와 대형 header.
- 키: unknown/중복 kid, wrong alg, 작은RSA, private JWK 혼입, 두 키 overlap, old 제거, 신규키 미배포 replica.
- 경로: 네 복구/네 이벤트와 wrong purpose, wrong method/path/port, public→internal 및 workload→public 거절. 헤더 위조로 connector 변경 불가.
- 실제 [RecoveryHttpConfiguration](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalrecovery/RecoveryHttpConfiguration.java)은 현재 path 격리만 제공한다.
- 실제 [RecoveryHttpFilter](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalrecovery/RecoveryHttpFilter.java)는 SecurityException을403으로 처리한다. 이 문서는 새 구현이 아니라 변경 기준 후보다.
- Jira 댓글 초안(미등록): 인증 exact 후보에 시간/권한/오류/키 규칙과 검증 목록 추가. 문서 정적 검증만 수행. 환경·네트워크·상대 수락과 실제 인증 구현/연동은 미완료.
