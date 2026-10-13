# TMI-203 Jira 수정안 — 승인 후 반영 완료

> 2026-10-12 사용자의 후속 승인 후 아래 수정안을 TMI-203 설명에 반영하고 재조회했다. 제목과 상태(해야 할 일)는 유지했다. 댓글·다른 이슈는 변경하지 않았다. 아래 승인 대기 표현은 실행 전 제안 이력이다.

- 날짜: 2026-10-12. 공식 Jira 조회 기준, 현재 상태 `해야 할 일`.
- 수정 대상: **TMI-203 설명만**. 제목·상태·담당자·상위 이슈·다른 이슈·댓글은 변경하지 않는다.
- 목적: 사용자 채택 ALB/workload JWT 방향을 현행 계획과 맞춘다. 실행 전 아래 수정 내용의 승인 필요.

## 1. 기존 문구 교체

### 구현 범위의 인증 항목

기존 `IAM/SigV4·환경/주체/route 권한 분리…` 항목을 다음으로 교체한다.

> Identity–Billing 인증은 기존 ALB + 서버 전용 workload JWT로 전환한다. 전용 target group/connector/security chain으로 사용자 public/LC 경로와 분리하고 환경·주체·목적별 route 권한, 직접 task 접근 제한, strict 입력/응답·개인정보 비로깅을 유지한다. 기본 OFF이며 운영 index drop/recreate나 자동 DDL은 포함하지 않는다. 이전 Lattice/SigV4 필수 전제는 이 범위에서 개정하되 기존 배포 client와 사용처 확인 없이 해당 구현을 제거하지 않는다.

### 복구 경로 설명

`미구현 복구 API는 기존 예정 prefix`를 `복구 API는 기존 prefix`로 수정한다. 로컬 HTTP adapter 구현과 실제 배포 완료를 혼동하지 않는다.

### 완료 기준 6번

> 권한/strict DTO·개인정보 비로깅·기존 token/push/LC 회귀를 검증한다. 사용자JWT·LC토큰·다른환경/issuer/audience/subject·잘못된 alg/kid·서명·시간·중복 Authorization·wrong method/path/port·forwarding 위조·무토큰을 거절한다. 직접 task 접근 제한, 정상/긴급 키 교체, 키 손상·clock 장애·인증 중단/재개를 검증한다. 실제 Mongo snapshot/replica-set·ALB/JWT·복원 검증과 mock 결과를 구분한다.

## 2. 설명에 추가할 절

### 2026-10-12 ALB·workload JWT 채택

- 사용자 승인 방향: 기존 ALB, workload 전용 connector/chain, 서버별·환경별 전용 RS256 JWT, 초기 배포형 public JWK set. private key는 호출 서버만 보유하고 공유하지 않는다. 사용자 signing/cursor/support 키와 분리한다.
- Identity 발신 영향 경로는 withdrawal `/internal/v1/payments/accounts/withdrawal/events`, trial eligibility `/internal/v1/eligibility/trial/events`, trial owner `/internal/v1/eligibility/trial/owner/events`, user merge `/internal/v1/owners/merge/events`다. Billing 발신은 Identity recovery 네 POST다. TMI-202 및 다른 이벤트 전송부 영향은 관련 작업과 조율하며 LC 경로를 자동 전환하지 않는다.
- 각 HTTP 시도 직전 새 JWT를 발급하되 동일 eventId/operationId/body를 유지한다. 토큰 저장/로깅, SigV4/JWT 자동 fallback, Lattice permitAll을 ALB로 그대로 노출하는 방식은 금지한다.
- Bearer JWT는 method/path/body 서명을 제공하지 않는다. 초기 TLS+짧은JWT+route권한 선택의 보장 차이를 인증 exact에 명시한다. jti 또는 event 멱등성을 credential 재사용 차단으로 주장하지 않는다.
- 구현 전 공동 확정: 실제 test/staging/production 배포와 Billing SANDBOX/PRODUCTION·Identity source environment 매핑; issuer/audience 문자열; connector port·origin·SG·공개 경로 노출과 ALB→task TLS/격리; 오류/time/header/key 규격.
- JWT 세부 권장 후보는 RS256/kid·RSA최소2048bit, iss/sub/단일문자열aud/정수iat·exp/jti, TTL최대60초/skew30초다. nbf 관계·경계 검증은 fixture로 동결한다. 후보값을 이미 양 서버 exact 수락된 값으로 기록하지 않는다.
- 공개키 교체는 old+new 수신배포 → 모든 replica 확인 → new 발급 → old 발급종료 확인 → 최소120초 및 clock 근거 → old 제거 순서다. 긴급 유출은 신뢰 제거를 우선한다. 실제 키 생성/주입·권한 변경은 별도 승인이다.
- 인증전용 새 fixture/revision을 추가하고 기존 recovery-r1/scalar-end-1 body/schema/hash·TMI-199 수락이력은 보존한다. T/H·digest/H2·source range·checkpoint/epoch·exact204·보존·삭제안전성은 불변이다.
- 순서: 인증 계약 개정 → 수신 OFF 구현 → 발신 OFF 구현 → ALB/키교체/복원 공동 검증 → 승인 활성화. 현재 핵심 저장/HTTP/runtime/snapshot jobs는 로컬 구현됐지만 신규 JWT/connector 전환·원천 운영 연결·실경보·공동복원은 미완료다.
- 근거 문서: `docs/contracts/TMI-203-withdrawal-recovery-snapshot-feed-plan.md` §5.2.1, `TMI-203-204-ALB-workload-JWT-handoff-2026-10-12.md`, `TMI-203-ALB-JWT-Billing-response-review-2026-10-12.md`.

## 3. 유지·검증 경계

그 외 기존 설명/완료 조건/보존 정책/역할 분리는 유지한다. TMI-204/202 등 다른 이슈 수정과 댓글/상태 전환은 이번 제안에 포함하지 않는다. 실행 전 최신 본문을 다시 읽고 동시 변경을 보존한다. 문서 작성만 수행했고 Jira 원격 쓰기는 아직 없다.
