# Stage 9 — Refresh Token 응답 유실 복구와 rotation 원자성 개선 계획

- 작성일: 2026-09-09
- 상태: TMI-130 코드·격리 테스트 구현 / 실제 Mongo·키 운영·모바일 검증 및 배포 대기 / 기능 기본 OFF
- Jira: [TMI-130](https://to-teacher.atlassian.net/browse/TMI-130), 유형 작업, 구현 전 조회 상태 해야 할 일. 구현 브랜치 `feat/TMI-130-refresh-token-response-recovery`. 이번 구현에서 Jira 댓글·상태 변경 없음.
- 선행: [Stage 8](firebase-logout-all-revoke-stage-8-plan.md)의 코드 병합 완료. 실제 Mongo·Firebase·모바일 및 Stage 7·8 통합 운영 검증은 미완료다.
- 기준: [고정 구현 순서](firebase-auth-follow-up-implementation-order.md). 이 계획은 production 활성화 승인이 아니다.
- 표기: 아래 **현재**는 계획 당시 조사한 코드, **승인**은 사용자 결정, **설계안**은 원 설계다. 2026-09-09 구현 결과·정확한 실제 이름·검증 범위는 [연동·운영 가이드](refresh-token-response-recovery-stage-9-runbook.md)와 6.5절을 우선한다. 운영 활성화 완료를 의미하지 않는다.

## 1. 5줄 결론

1. 같은 Refresh Token과 같은 `Idempotency-Key`의 재시도에는 처음 발급한 결과를 최대 2분간 복구한다. 새 토큰을 계속 만들지 않는다. [현재 reissue](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/TokenReissueService.java)
2. 난수 토큰 생성과 세션의 해시 저장을 유지하고, 별도 키로 암호화한 응답만 단기 보관한다. 세션 회전·후속 세션·복구 정보는 하나의 Mongo Transaction으로 저장한다. [현재 Session](../../src/main/java/web/tosunsaeng/identity/domain/auth/domain/entity/RefreshSession.java)
3. 복구보다 사용자 상태·epoch·폐기 경계·후속 세션 상태를 먼저 확인한다. 전체 로그아웃·탈퇴·승격·병합·단일 로그아웃 뒤 과거 응답으로 인증을 되살리지 않는다. [공통 fence](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/SessionSecurityService.java)
4. 기존 앱이 이 API를 사용하지 않는다는 사용자 확인에 따라 요청 ID를 최초부터 필수화한다. 기존 URL/JSON은 유지하되 필수 요청 헤더와 만료 시각 응답 헤더를 설계한다. [현재 DTO](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/dto/response/ReissueResponse.java)
5. 정상 복구 만료와 의심 재사용을 구분하며 Guest의 자동 재생성·토큰 삭제를 금지한다. 키/백업·실제 Transaction·모바일 검증 전 운영 활성화하지 않는다. [Guest 발급](../../src/main/java/web/tosunsaeng/identity/domain/auth/registration/application/GuestAuthService.java)

## 2. 사용자가 반드시 읽어야 하는 내용

### 2.1 해결할 문제와 정상 흐름

현재는 재발급으로 기존 세션 A가 `ROTATED`가 된 후 앱이 응답을 못 받으면, A의 재제출이 재사용 탐지와 활성 세션 전체 폐기로 이어질 수 있다. DB Transaction만으로 이미 전송 중인 HTTP 응답의 유실은 해결할 수 없다.

새 흐름은 다음과 같다.

1. 앱은 논리적 재발급 작업마다 요청 ID를 하나 생성하고 기존 credential과 묶어 보관한다.
2. 서버는 A 회전·후속 B 세션 생성·요청 식별 증거·암호화된 최초 응답을 한 Transaction으로 commit한다.
3. 앱이 응답을 못 받으면 같은 A와 같은 요청 ID로 제한 재시도한다.
4. 현재 보안 상태가 유효하고 복구 기한 이내이면 최초 B 응답을 복호화해 반환한다. JWT의 jti/iat/exp와 Refresh Token을 새로 만들지 않는다.
5. 앱은 새 credential을 안전하게 저장한 뒤 진행 중인 요청을 정리한다. 다음 정상 rotation에서는 새 요청 ID를 쓴다.

### 2.2 암호화 보관의 의미

**승인:** 복호화 가능한 응답을 짧게 보관한다. RefreshSession의 credential 해시 저장은 그대로이고 평문 DB/로그 저장은 금지한다. 그러나 DB 암호문과 복호화 키가 함께 노출되면 응답을 복원할 수 있으므로 기존 해시-only 모델보다 보안 책임이 늘어난다.

**2분은 응답 반환 허용 기간이지, 모든 저장소와 백업에서 2분 뒤 흔적이 사라진다는 보장이 아니다.** 애플리케이션 만료 검사, primary DB TTL/purge, 백업 보존, 구 복호화 키 폐기는 별도 제어다. RSA JWT 서명 키·phone fingerprint 키는 암호화 키로 재사용하지 않는다.

### 2.3 변경되는 외부 계약

- 유지: `POST /api/v1/auth/reissue`, body의 `refreshToken`, 기존 `BaseResponse<ReissueResponse>` 필드, RS256/JWKS/사용자 `sub`·`account_type`·Billing audience/scope, workload 분리.
- 변경 설계: `Idempotency-Key` 필수. 최초 성공과 replay에 절대 만료 시각 헤더를 추가한다. 공개 인증 진입점이며 유효 Access Token이나 그 jti를 요구하지 않는다.
- 기존 앱이 이 API를 사용하지 않는다는 것은 **사용자 제공 사실**이다. 모바일 저장소를 검증했다는 뜻이 아니다. 새 앱과 서버를 함께 계약 테스트하고 구 keyless 요청을 지원하는 호환 분기는 만들지 않는다.
- 앱은 replay의 `ExpiresIn`을 응답 수신 시각에 다시 더하지 않는다. 같은 응답을 반환해도 실제 만료는 늦춰지지 않는다.

### 2.4 업데이트 후 SNS 전환 유도와 Guest 복구 제한을 구분한다

2026-09-09 사용자 정정: 현재 배포 앱은 Guest 로그인만 제공하며, 업데이트 후 기존 Guest 사용자에게 SNS 로그인/회원 전환을 유도한다는 제품 방향이다. 이를 **2분 응답 복구 만료 시의 전용 대응 정책을 승인한 것**으로 해석하지 않는다. SNS 전환의 강제 여부·Guest 기능 폐지·기존 Guest 데이터 포기는 승인된 사항이 아니다.

업데이트 전환 안내와 Stage 9의 통신 장애 복구는 별개다. Stage 9는 승인된 필수 요청 ID·암호화 응답 보관·최대 2분 복구 설계를 유지하고, 새 장기 Guest 복구 수단을 추가하지 않는다. 기존 앱의 Guest 인증이 현행 Identity 승격/병합에서 유효한 증명으로 인정되는지는 모바일 연동에서 별도로 확인해야 한다. 기존 앱이 이번 reissue API를 사용하지 않는다는 사실만으로 인증 호환성을 가정하지 않는다.

승인한 2분 복구가 끝나고 새 Refresh Token도 없는 Guest는 안전한 본인 증명 없이 기존 계정 접근을 복구할 수 없다. installation ID만으로 기존 credential을 재발급하거나 자동으로 새 Guest를 만들어 기록 접근을 바꾸지 않는다.

현재 Guest 승격은 인증된 Guest의 Access Token에서 얻은 userId와 Firebase 인증·enrollment 등의 조건을 사용한다. Guest Access Token이 여전히 유효하고 기존 승격/병합 조건을 통과한다면 기존 전환 흐름을 이용할 수 있다. Guest를 증명할 인증까지 잃었다면 SNS 인증만으로 그 Guest가 동일 사용자 소유였다고 판정하지 않는다. 이 경우 정상 SNS 로그인/가입으로 안내하되 기존 Guest 기록이 자동 연결된다고 약속하지 않는다. 신규 MEMBER의 phone link·동의 등 기존 가입 조건도 유지한다.

- 단순 통신/복구 만료 오류에서 로컬 credential·계정 정보를 자동 삭제하지 않는다.
- 화면에는 인증 복구가 필요함을 표시하고 기존 Guest 인증이 필요한 새 요청을 중단한다. 사용자 선택에 따른 정상 SNS 로그인/가입은 허용한다. 기존 credential을 보존하는 것은 서버에서 다시 유효하게 만든다는 뜻이 아니다.
- 다른 안전한 저장 위치에 이미 최신 결과가 있는지 앱 내부에서 먼저 확인한다. 없으면 자동 복구 불가를 안내한다.
- 지원 문의가 곧 계정 복구 보장은 아니다. 사용자 동의 없는 Guest 초기화·history 이전은 금지한다.
- Guest 장기 세션 유실 복구를 제품상 반드시 보장하려면 별도 검증된 복구 증명이 필요하다. Stage 11의 **Guest 생성 응답 유실** 복구가 이 문제 전체를 자동 해결한다고 설명하지 않는다. 이 계획에서는 새로운 Guest 인증 수단을 추가하지 않는다.

## 3. 사용자가 결정해야 하는 사항

### 3.1 승인된 정책 — 다시 선택할 필요 없음

| 항목 | 확정 내용 |
| --- | --- |
| 요청 식별 | 앱의 재발급 작업별 Idempotency-Key, 동일 작업 재시도는 같은 ID |
| 복구 방식 | 별도 키로 암호화한 최초 응답 보관·재전달; 새 토큰 교체안 미채택 |
| 기간 | 최초 회전 성공 기준 최대 PT2M, 재시도/서버 재시작으로 연장하지 않음 |
| 정상 복구 만료 | 전체 세션 폐기 없이 해당 클라이언트 재인증/복구 안내; Guest는 위 제한 적용 |
| 의심 재사용 | 안전한 동일 요청 복구로 분류할 수 없는 유효 rotated credential의 다른 요청 ID 재사용에는 기존 활성 세션 전체 폐기 정책 유지 |
| rollout | 처음부터 ID 필수, 구 앱 keyless 호환 분기 없음 |

### 3.2 구현 제안과 운영 확인 사항

본문의 AES-GCM/키 공급 인터페이스, 데이터 구조, 오류/헤더 이름, bounded retry 수치 등은 위 정책을 실현하는 **상세 설계안**이다. 다음 사항은 운영 활성화 전 실제 환경으로 확인한다.

- 전용 암호화 키의 안전한 공급 주체·접근 권한·회전 절차·구 키 제거·백업 보존 정책과 담당자.
- Guest의 복구 불가능 안내 UX를 프론트/제품에서 확인. 신규 인증수단이 필요하다고 결정되면 별도 범위 승인.
- 실제 Mongo replica set Transaction/rollback·인덱스·일시 장애 검증, 새 앱의 pending 저장/동시성/재시작 검증.
- Stage 8과 분리된 TMI-130 생성·재조회 및 전용 브랜치 코드 구현 완료. 다른 저장소나 종료 이슈를 변경하지 않는다.

## 4. 주요 위험과 미확인 사항

| 위험 | 설계 대응 / 남는 한계 |
| --- | --- |
| 원 credential+요청 ID 동시 탈취 | ID는 인증 증명이 아님. 짧은 기간·보안 fence·단일 후속 세션 검사로 제한하지만 정상 요청과 완벽 구별 불가 |
| 2분 payload TTL 뒤 같은 요청 재시도 | 암호문과 요청 식별 메타데이터 수명을 분리; payload 부재만으로 탈취로 분류하지 않음 |
| source Session 원래 만료/TTL | 만료를 연장해 복구하지 않음. 원 credential 만료가 더 빠르면 2분 전체를 보장하지 않음 |
| 같은 응답의 ExpiresIn 재사용 | 절대 만료 응답 헤더를 함께 제공. 최초 jti/exp 불변, 앱 수신 시각 기준 재연장 금지 |
| 회전 성공 후 commit 응답 유실 | 같은 요청 재조회, 새 난수 결과 재생성 금지. 결과 확정 못 하면 안전한 503 |
| child 회전/단일 logout과 replay 경합 | child CAS 쓰기 및 공통 control Transaction으로 선후 관계 확보; read-only 조회만으로 허용하지 않음 |
| 키 유실·암호문 변조·조기 삭제 | 503과 안전한 경보, 재발급/평문 fallback 금지; 기한 만료 후 새 결과 생성 금지 |
| 시간차/복구 경계 | 주입 Clock, 서버 간 시간 동기화; 논리 rotation 시각을 고정하고 응답 지연으로 연장하지 않음 |
| 대량 활성 세션 reuse 폐기 | 기존 전 사용자 범위를 유지하되 Transaction 처리량·상한/장애 측정 필요. 부분 폐기 후 완료로 응답하지 않음 |
| 구 구현 rollback | 기존 코드가 same-key replay를 공격으로 판정할 수 있음. 복구 도입 후 old writer 혼재/무조건 rollback 금지 |

## 5. 현재 작업과 직접 관련된 구현 설명

### 5.1 현행 구현에서 유지·보완할 경계

- `TokenReissueService`는 fence ON일 때 회전 전체를 `SessionSecurityService.transaction`으로 감싸고, 재사용 탐지의 폐기는 commit 후 오류로 반환한다. 이 구조를 유지하며 동일 요청 판정을 ROTATED 공격 처리보다 앞에 둔다.
- `RefreshSession`의 `@Version`, `rotationFamilyId`, `rotatedFromSessionId`, `replacedBySessionId`, `sessionEpoch`와 immutable `SessionAuthentication`을 활용한다.
- `RefreshSessionIssuer.issueRotated`는 난수 결과를 생성하고 현재 호출에서는 raw Refresh Token을 DB에 저장하지 않는다. 후속 Session 식별자·정확한 만료 시각을 application 내부 결과로 넘길 수 있도록 보완하되 public DTO에 내부 user/session ID를 추가하지 않는다.
- `LogoutService`는 현재 이미 ROTATED인 source를 성공 NOOP로 처리한다. 응답 유실 중 같은 source로 logout하면 복구가 살아남는 간극이 있으므로 5.6의 최소 cancellation을 추가한다. Firebase revoke나 전체 로그아웃으로 확대하지 않는다.

### 5.2 API·프론트 wire 설계안

**요청**

- URL/body 유지. `Idempotency-Key` 단일 헤더 필수, canonical lowercase UUID v4 문자열 길이 36. 빈 값·다중 헤더·쉼표로 결합된 값·다른 형식은 400 `INVALID_REISSUE_REQUEST_ID` 제안.
- request ID는 userId·Firebase UID·이메일·기기 ID에서 만들지 않고 CSPRNG 기반 UUID로 생성한다. 한 논리 재발급에 한 ID, 재시도/앱 재시작에도 동일 ID.
- `Authorization`은 필요 없다. 공통 interceptor가 만료된 Access Token을 자동 첨부하여 Resource Server에서 먼저 거절되지 않도록 reissue 요청을 분리한다.
- key 원문을 URL/query/body·로그·추적 태그에 추가하지 않는다. userId는 credential 해시로 조회한 실제 Session에서 얻는다.

**성공 응답**

- 기존 `200 BaseResponse`와 `accessToken`, `refreshToken`, `grantType`, `accessTokenExpiresIn`, `refreshTokenExpiresIn`을 동일 값으로 재전달한다. 최초 생성 값과 결과 schemaVersion을 암호화 envelope에 포함한다.
- 최초와 replay 모두 `Reissue-Access-Expires-At`, `Reissue-Refresh-Expires-At` 헤더를 UTC ISO-8601 Instant로 제공한다(제안). 발급 결과의 실제 exp/Session expiresAt에서 얻으며 재조회 시점으로 계산하지 않는다.
- 기존 converter는 Access/Refresh 발급 시각이 다를 수 있고 JWT issuedAt은 초 단위이므로 하나의 공통 issuedAt에 두 duration을 더해 정확한 만료를 추정하지 않는다.
- `Cache-Control: no-store`, `Pragma: no-cache`를 reissue 성공·오류 응답에 적용하고 ingress 캐시/HTTP body 로깅도 OFF. 웹 클라이언트가 있으면 exact origin CORS에서 요청 key 허용·두 응답 헤더 노출을 검증한다. wildcard credentials 허용 금지.
- 내부에서 최초/replay를 집계하되 새 공개 Session ID나 ciphertext/key ID를 반환하지 않는다.

### 5.3 데이터·보존·인덱스

**A. 기존 RefreshSession의 비밀정보 없는 회전 증거 확장**

| 제안 필드 | 역할 |
| --- | --- |
| rotationRequestKeyHash | canonical ID의 SHA-256 식별자; 사용자 범위 unique 및 동일 요청 비교 |
| rotationResponseId | 단기 암호문 문서의 UUID; 임의 클라이언트 입력 금지 |
| rotationCommittedAt | 최초 성공 Transaction에 저장한 논리 rotation 시각; 재시도 불변 |
| recoveryUntil | 최초 복구 deadline, 증가 금지 |
| recoveryDisabledAt | logout 등으로 해당 응답 복구를 취소한 증거 |
| issuedAccountType | 최초 응답의 계정 유형 snapshot; 현재 유형이 달라지면 구 응답 재전달 금지 |

기존 userId/tokenHash/epoch/family/child ID/RevocationReason/Version을 재사용한다. `rotationRequestKeyHash`는 원 Refresh Token이나 복호화 키가 아니지만 민감한 요청 연관 정보로 접근 통제한다. 요청 ID만으로 Session을 찾거나 결과를 반환하지 않는다.

- partial unique `(userId, rotationRequestKeyHash)`: 필드가 유효한 문자열인 신규 증거에만 적용. 기존 누락 문서 migration과 실제 partial index 지원 검증.
- source 한 건의 회전은 `@Version`과 `replacedBySessionId`로 단일 child를 보장. 요청 ID를 재사용해 다른 유효 source에서 발급하려 하면 409 conflict, 기존 결과 노출·자동 회전 없음.
- 요청 증거는 source의 기존 `expiresAt` TTL까지 유지한다. 원 세션의 인증 수명/TTL을 2분 payload TTL로 줄이지 않는다. 원 source가 만료/삭제됐으면 401 expired/invalid이며 payload만으로 인증을 우회하지 않는다.
- 기존 ROTATED 문서에 요청 증거가 없으면 same-key 복구로 추정하지 않는다. 본인 인증을 통과한 유효 legacy source의 재사용은 기존 정책, 만료 source는 단순 expired/invalid 처리한다. 운영 미사용 전제를 staging 데이터에도 확인한다.

**B. 신규 `RefreshReissueResponse` 문서 — `refresh_reissue_responses`(제안)**

| 필드 | 역할 |
| --- | --- |
| responseId (_id), sourceSessionId | 서버 생성 문서 ID, 원 Session exact 연결 |
| schemaVersion | 인증된 암호문/응답 schema 버전, 초기 1 |
| encryptionKeyId, nonce, ciphertext | 전용 키 버전·GCM nonce·인증 tag를 포함한 암호문 |
| accessExpiresAt, refreshExpiresAt | 실제 절대 만료 시각의 비밀정보 없는 AAD 메타데이터; 내부 envelope 값과 일치 검사 |
| recoveryUntil, cleanupAt | immutable deadline 및 TTL 시각; cleanupAt은 recoveryUntil |

- `sourceSessionId` unique, `cleanupAt` TTL(expireAfter 0), 필요한 키별 backlog 조회 인덱스를 운영 확인한다.
- 인증 기한 검사는 DB TTL 삭제 여부와 독립한다. cleanup 지연/백업의 ciphertext도 API로 반환하지 않는다.
- envelope 안에는 최초 ReissueResponse와 실제 Access/Refresh 절대 만료 시각을 넣는다. 평문 credential은 source/메타데이터/오류 문서에 저장하지 않는다.
- 응답 크기 상한 16 KiB 제안, 과대 응답은 최초 Transaction을 안전하게 실패시켜 기존 credential을 보존한다.

**시각 계약의 구현상 정밀도**

- 승인 정책은 최초 성공 회전 이후 최대 PT2M다. 실제 Mongo commit 완료 시각을 application 필드에 같은 Transaction으로 정확히 넣는 것은 일반 TransactionTemplate만으로 보장할 수 없다.
- 설계안은 최초 성공 Transaction의 서버 논리 시각을 `rotationCommittedAt`에 고정하고 `recoveryUntil = rotationCommittedAt + PT2M`로 저장한다. 물리 commit/네트워크가 늦으면 실사용 가능 기간은 더 짧아질 수 있지만 절대 길어지지 않는다.
- rollback된 시도는 성공 회전이 아니다. commit 불명 시각을 새 now로 재기록하지 않고 원 증거를 재조회한다. 물리 commit 후 별도 저장으로 deadline을 연장하는 two-phase 보정은 하지 않는다.
- source/child/Access의 원래 만료가 더 빠르면 그 만료를 우선한다. 복구를 위해 인증 수명이나 `ExpiresIn`을 늘리지 않는다.

### 5.4 응답 암호화와 키 관리

- 제안 port `ReissueResponseCipher`와 전용 `ReissueEncryptionKeyProvider`. 기존 JWT encoder 및 Firebase/workload 발급기와 분리한다.
- 기본 설계 AES-256-GCM, 암호학적 난수 96-bit nonce, 128-bit tag. nonce는 암호화마다 새로 만들고 request ID/시각에서 파생하지 않는다. 라이브러리의 표준 AEAD를 사용하고 독자 암호 방식 금지.
- AAD는 version, 서비스/환경/용도, response ID, source/child ID, 실제 userId, request hash, epoch/account type, immutable 기한과 실제 만료 시각을 길이 구분한 정규 형식으로 결합한다. 외부 요청이 아닌 검증된 DB snapshot으로 구성한다. 행/사용자/환경 간 ciphertext 치환을 거절한다.
- active encrypt key 하나와 제한된 decrypt-only 구 키를 관리한다. 새 키를 모든 instance에 배포→신규 암호화 키 전환→구 key로 만든 live 응답의 기한/진행 요청 소진 확인→구 키 제거 순서다. cryptoperiod·key별 암호화량·nonce 충돌 위험의 운영 상한을 설정하고 검증한다.
- 배포 환경의 secret store/읽기 전용 secret mount로 키를 공급하는 adapter를 기본 제안한다. Mongo와 같은 문서에 키를 저장하거나 환경별 키를 공유하지 않는다. KMS/envelope 암호화가 필요하면 이 port 뒤의 별도 adapter로 명시 설계하며 현재 계획이 KMS 인프라를 배포하는 것은 아니다.
- 키 로드는 startup/안전한 reload에서 수행하고 Transaction 안에 원격 secret/KMS 요청을 넣지 않는다. 키 ID 중복·잘못된 길이·active key 부재는 새 기능 활성화 startup 실패. 선택 cipher key ID는 복구가 끝날 때까지 식별 가능해야 한다.
- encryption 실패는 회전을 rollback한다. decryption 실패/unknown key/tag 불일치는 safe 503과 고정 오류 코드 경보이며 새 결과 생성/평문 fallback/보안 오류로 가장 금지.
- 키 문자열·ciphertext·tag·nonce·credential·요청 body를 toString/로그/Sentry/트레이스에 포함하지 않는다. JVM 메모리의 모든 사본 삭제를 보장한다고 주장하지 않는다.
- 구 키와 백업이 함께 보존되면 과거 응답은 복호화 가능하다. primary TTL과 백업 정책·secret version 보존을 분리해 위험을 인계한다. API 2분 만료를 cryptographic erasure로 설명하지 않는다.

### 5.5 원자 처리와 요청 분류

**공통 선행 검사**

1. 요청 형식 검사→기존 credential 해시 조회. ID만 보낸 요청은 허용하지 않는다.
2. 실제 User·Session의 WITHDRAWN/MERGED 및 epoch/확인된 Firebase 폐기 경계를 확인한다. 현재 UserAccountType은 DB의 호환 getter를 사용한다.
3. 원 credential 만료는 공격 탐지보다 먼저 검사한다. 이미 만료된 source만으로 현재 새 세션 전체를 폐기하지 않는다.
4. source의 terminal 사유·복구 취소·요청 메타데이터에 따라 아래 표로 분류한다. ROTATED는 특정 복구 분기에서만 예외이며 일반 활성 세션으로 되돌리지 않는다.

| source/요청 상태 | 처리 |
| --- | --- |
| ACTIVE + 새 ID | 최초 회전 Transaction |
| ACTIVE + 같은 사용자의 다른 source에서 이미 사용한 ID | 409 요청 ID 충돌, 결과 반환/회전 없음 |
| ROTATED + 같은 ID + 기한 내 + 유효한 exact child | 기존 응답 복구 |
| ROTATED + 같은 ID + 복구 기한 지남 | 409 복구 만료; 전 사용자 세션 폐기 없음 |
| ROTATED + 같은 ID + child가 다음 세대로 회전 | 409 결과 대체됨; 과거 결과/최신 descendant 원문 반환 없음 |
| ROTATED + 같은 ID + child 단일 logout/폐기 | 세션 종료 오류; 복구 금지 |
| ROTATED + 다른 유효 ID, 현재 epoch/credential 기한 유효 | 기존 의심 재사용 처리; 사용자 활성 세션 전체 폐기 |
| source/사용자 자체 종료·withdrawn·merged·구 epoch | 기존 계정/세션 오류 우선; replay/새 원격 revoke 없음 |
| 메타데이터는 정상인데 기한 전 payload가 없음/변조/키 장애 | 503; 같은 ID 제한 재시도, 절대 새 결과 생성하지 않음 |

**최초 회전 Transaction**

- 공통 control을 CAS 갱신하고 source version을 확인한다. 후속 B ID·난수 Refresh Token과 사용자 Access Token을 생성한다.
- B는 source의 epoch/source/binding/authTime/family를 상속한다. 현재 DB 계정 유형으로 Access Token을 발급하며 workload JWT는 건드리지 않는다.
- 최초 응답·만료 시각을 암호화한다. source ROTATED+metadata, B의 hash 세션, ciphertext를 같은 Mongo Transaction에 저장한다. commit 성공 전 응답을 반환하지 않는다.
- source save 직후 실패, B save 직후 실패, ciphertext 저장 실패, encryption 실패 모두 rollback한다. 부분 성공을 정상 응답으로 만들지 않는다.

**복구 Transaction**

- source 증거·request hash·child 관계·user/family/epoch/proof를 재검증한다. B는 ACTIVE·미만료·미회전이어야 하며 issuedAccountType과 현재 DB account type이 같아야 한다. 단순 userId 일치만 검사하지 않는다.
- `SessionSecurityService.checkAndTouch(..., false)`의 확인된 폐기 경계를 사용한다. 재발급 자체를 새 Firebase 인증으로 취급하거나 현재 시각을 authTime으로 넣지 않는다.
- 공통 control뿐 아니라 B도 version CAS 쓰기(touch)를 수행하여 단일 logout/후속 회전과 충돌시킨다. 조회 후 decrypt만 하는 read-only Transaction은 금지한다.
- 인증된 기한/AAD를 검증하고 복호화한다. 결과의 실제 Refresh hash·JWT 대상/저장 메타데이터가 맞는지 검증하며 새 서명/새 token generation은 호출하지 않는다.
- commit 성공 뒤에만 최초 응답을 반환한다. 반환 직전에도 복구/토큰 기한을 넘겼다면 거절한다. commit 뒤 발생하는 logout과 이미 전송 중인 응답의 완전한 동기 취소는 보장하지 않으며 모바일 cancellation과 서버 후속 검사를 함께 사용한다.

**충돌·commit 불명**

- 동일 source/동일 ID 동시 요청은 unique/CAS 충돌 뒤 새 Transaction에서 기존 결과를 다시 읽는다. 최대 3회, 짧은 jitter(10~50 ms, Transaction 밖) 재시도 제안. 무제한 재실행 금지.
- 다른 ID 경쟁은 commit 승자 증거를 조회한 뒤 위 의심 재사용 정책으로 분류한다. 단순 write conflict만으로 즉시 공격이라고 단정하지 않는다.
- Mongo transient abort는 확실히 abort된 경우만 제한 재시도한다. UnknownTransactionCommitResult는 가능하면 동일 driver transaction commit 확인을 우선하고, 새 Transaction에서 최초 회전을 무작정 재실행하지 않는다. receipt/parent의 확정 결과를 찾지 못하면 503으로 종료하며 앱은 같은 ID로 재시도한다.
- 기존 `SessionSecurityService.transaction`의 예외 일괄 503 매핑을 무분별하게 해제하지 않는다. reissue 전용 경계에서 필요한 Mongo label/duplicate/CAS를 안전하게 분류한다. 원본 DB 예외를 외부/로그에 복사하지 않는다.
- 의심 재사용 폐기는 Transaction 안에서 저장하고 commit 뒤 `REFRESH_TOKEN_REUSE_DETECTED`를 반환한다. 오류 throw로 폐기가 rollback되지 않도록 기존 패턴을 유지한다. 미완료 폐기를 성공한 보안 조치로 기록하지 않는다.

### 5.6 logout·탈퇴·승격·병합 연계

- Stage 8 session fence가 전제다. replay에도 User ACTIVE/현재 epoch/confirmed Firebase boundary를 검사하여 기존 proof를 되살리지 않는다.
- 직접 B로 단일 logout하면 B version 변경과 replay touch가 충돌한다. logout 먼저 commit되면 replay는 거절된다.
- **응답 유실 중 A로 단일 logout하는 간극 보완(범위 내 설계):** 미만료 A가 새 recovery metadata를 가진 ROTATED이며 복구 기한 내일 때, recoveryDisabledAt을 저장하고 exact direct child B가 활성이라면 함께 폐기한다. 현재 request credential이 증명하는 해당 회전만 취소한다. 다른 로그인 family·user·다음 descendant를 무조건 순회 폐기하지 않는다.
- 취소와 회전/replay는 공통 control/source/child CAS Transaction으로 묶는다. B가 이미 다음 세대로 회전했다면 A의 복구만 차단하고 오래된 결과를 반환하지 않는다. 기존 `/logout` URL/200 멱등 응답은 유지한다. Firebase remote revoke는 호출하지 않는다.
- `/logout-all`, withdrawal, upgrade/merge는 기존 control·세션 폐기를 재사용한다. 정상 source 회전 사유를 임의 변경해 history를 덮어쓰지 않는다. 계정 유형 변경/target user로 구 응답을 변환하지 않는다.
- 전체 logout 뒤 새 로그인에 이전 ROTATED source가 재사용돼도 현재 epoch 검사가 우선하므로 새 로그인까지 공격 탐지로 폐기하지 않는다.

### 5.7 오류·모바일 계약 설계안

| 상황 | 응답 제안 | 앱 동작 |
| --- | --- | --- |
| 최초 성공 / 같은 요청 복구 | 기존 200 | 같은 credential 결과와 절대 만료 저장 |
| ID 누락/형식/다중 값 오류 | 400 INVALID_REISSUE_REQUEST_ID | 자동 ID 변경 재시도 금지, 구현 오류 처리 |
| 다른 source에 이미 쓴 ID | 409 REISSUE_REQUEST_CONFLICT | 요청/계정 저장 상태 점검, 자동 반복 금지 |
| 확인된 같은 요청의 복구 기간 종료 | 409 REISSUE_RECOVERY_EXPIRED | MEMBER 재인증 안내, Guest는 정보 보존·안전한 복구 안내 |
| child가 이미 다음 세대로 회전/계정 유형 변경 | 409 REISSUE_RESULT_SUPERSEDED | 로컬 최신 결과 확인, 오래된 응답으로 덮어쓰기 금지 |
| 세션 종료·epoch 또는 Firebase 경계 위반 | 기존 SESSION_LOGGED_OUT 등 401 | 자동 exchange/reissue 루프 금지, 계정별 안내 |
| WITHDRAWN/MERGED | 기존 ACCOUNT_WITHDRAWN/계정 비활성 오류 | 기존 탈퇴/병합 UX 우선; target으로 자동 변환 없음 |
| 유효 rotated credential의 다른 ID 재사용 | 기존 401 REFRESH_TOKEN_REUSE_DETECTED | 기존 전체 세션 폐기 안내; 저장 결과는 commit됐을 때만 처리 |
| 원 credential 만료/부재 | 기존 REFRESH_TOKEN_EXPIRED/INVALID_REFRESH_TOKEN | 자동 새 Guest 생성 금지 |
| DB/키/암호문 장애·충돌 미해결 | 503 SESSION_SECURITY_UNAVAILABLE | 같은 ID·같은 source로 제한 재시도 |

오류 이름/HTTP 코드는 신규 상세 제안이다. Guest 여부를 클라이언트 요청값으로 판단하지 않으며 원본 예외·key ID·credential·내부 session ID를 에러에 넣지 않는다.

프론트 필수 사항:

1. 재발급 작업 생성 시 계정/로컬 세션 세대·source credential 참조·request ID·최초 시작 시각을 안전한 저장소에 한 묶음으로 기록하고 전송한다. 서버 응답이 없을 때 다른 ID를 생성하지 않는다.
2. 여러 API의 재발급 필요 신호는 계정별 single-flight로 합친다. 401 전체를 무조건 재발급으로 처리하지 않는다. 백그라운드 작업·웹 multi-tab 등이 있으면 앱 프로세스 한 mutex만으로 충분하다고 가정하지 않는다.
3. 성공 결과의 두 credential·절대 만료·pending 정리를 원자적/복구 가능한 저장으로 처리한다. 앱 crash가 old credential+새 ID 조합을 만들지 않도록 한다.
4. 로그아웃/계정 전환/새 로그인 시 local generation을 증가시켜 이미 진행 중인 과거 응답을 저장하지 않는다. 필요 시 위 단일 logout 취소와 연결한다.
5. timeout·connection·429/503 등 재시도 가능한 응답만 같은 ID로 retry한다. 초기 제안 지수 backoff 1/2/4/8초 이후 cap 10초+jitter, 총 5회 이하·전체 로컬 2분 이하. 서버 deadline이 권위이며 앱 clock이나 Retry-After로 복구 기간을 늘리지 않는다.
6. 복구 응답에는 최초 전체 duration이 그대로 있으므로 절대 만료 헤더를 기준으로 스케줄링한다. Access JWT exp의 단순 decode는 UI 시간 계산용이지 서버 인증 검증 대체가 아니다.
7. Guest의 재인증 불가능과 MEMBER의 재로그인을 구분한다. 자동 계정 초기화·연결·history 이동은 하지 않는다.

### 5.8 설정·관측·배포

| 신규 설정 제안 | 기본 | 의미 |
| --- | --- | --- |
| AUTH_REISSUE_RECOVERY_ENABLED | false | Stage 9 전체 경로 활성화 |
| AUTH_REISSUE_RECOVERY_WINDOW | PT2M | 승인된 기본 최대 복구 기간; 확대는 정책 재검토 |
| AUTH_REISSUE_RESPONSE_MAX_BYTES | 16384 | 암호화 전 envelope 상한 |
| AUTH_REISSUE_CONCURRENCY_ATTEMPTS | 3 | 확실한 충돌의 제한 재조회 |
| AUTH_REISSUE_ENCRYPTION_ACTIVE_KEY_ID | 설정 필요 | 실제 비밀키가 아닌 전용 키 버전 식별자 |
| AUTH_REISSUE_ENCRYPTION_KEYRING_LOCATION | 설정 필요 | 안전한 mount/공급자 참조, 실제 경로·키 값은 이 문서에 기록하지 않음 |

- ON이면 Stage 8 fence ON·실제 Transaction manager·키 공급·인덱스 전제가 필요하다. 복구 활성화 후 ID가 없는 요청을 legacy 방식으로 처리하는 fallback은 없다.
- OFF 개발 배포 시 legacy 코드가 존재하는 것과 운영 keyless 호환을 제공하는 것을 구분한다. 공개 신규 앱은 새 경로 준비 뒤 연동한다. live recovery가 생긴 후 단순 flag OFF로 old reissue로 돌아가면 same-key 재시도를 공격으로 판정할 수 있어 금지한다.
- 비상 중단은 reissue를 안전한 503/maintenance로 제한하고 기존 fence·증거를 유지하는 절차를 사용한다. old source를 재활성화하거나 암호문/메타데이터를 지워 복구하지 않는다. 2분만 지났다는 이유로 keyless 구현 rollback을 안전하다고 단정하지 않는다.
- 초기 metrics: first_issue, replayed, recovery_expired, superseded, id_conflict, suspected_reuse, crypto_failure, transaction_retry/unresolved, payload_cleanup_lag. commit 결과와 실패 시도를 구분한다. 사용자/credential/request ID/key ID는 고카디널리티 label에 넣지 않는다.
- crypto_failure/조기 payload 누락은 즉시 경보, 503/복구 만료 비율과 TTL purge 지연은 baseline 수집 후 임계값 확정. 운영 담당자·응답 시간·중단 절차를 activation gate로 둔다.
- HTTP/SDK/Sentry/DB profiler 및 proxy 요청 헤더/body 로그 설정을 확인한다. 인증정보는 metric·Jira·테스트 출력에 남기지 않는다.

## 6. 부록 — 전체 근거·검증·구현 순서

### 6.1 관련 코드와 변경 파일 제안

| 현재 파일/신규 제안 | 역할·작업 |
| --- | --- |
| [AuthController](../../src/main/java/web/tosunsaeng/identity/domain/auth/common/api/AuthController.java) | 필수 헤더·캐시 방지·절대 만료 헤더·OpenAPI |
| [ReissueRequest](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/dto/request/ReissueRequest.java) / [ReissueResponse](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/dto/response/ReissueResponse.java) | public JSON 유지, 내부 결과와 HTTP metadata 분리 |
| [TokenReissueService](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/TokenReissueService.java) | 최초/복구/의심 재사용 분류와 원자 처리 |
| [RefreshSession](../../src/main/java/web/tosunsaeng/identity/domain/auth/domain/entity/RefreshSession.java) / [Repository](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/repository/RefreshSessionRepository.java) | parent 회전 증거·partial unique·child CAS |
| [RefreshSessionIssuer](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/RefreshSessionIssuer.java) | 내부 ID/실제 만료 전달, proof 전파 유지 |
| [SessionSecurityService](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/SessionSecurityService.java) | 공유 control·현재 계정/fence, 전용 충돌 분류 경계 연계 |
| [LogoutService](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/LogoutService.java) | exact active child/복구 취소와 Transaction 경합 보완 |
| [AuthResponseConverter](../../src/main/java/web/tosunsaeng/identity/domain/auth/common/converter/AuthResponseConverter.java) | 최초 payload 보존·duration 의미 유지 |
| [JwtAccessTokenIssuer](../../src/main/java/web/tosunsaeng/identity/global/security/jwt/JwtAccessTokenIssuer.java) | 재서명 없이 최초 결과 사용, 현재 발급 계약 회귀 |
| [SecurityConfig](../../src/main/java/web/tosunsaeng/identity/global/config/SecurityConfig.java) | reissue 공개 진입점 유지; 웹 사용 시 exact CORS 계약 |
| [RequestLoggingFilter](../../src/main/java/web/tosunsaeng/identity/global/observability/RequestLoggingFilter.java) | 새 헤더/body 비노출 확인 |
| 신규 RefreshReissueResponse/Repository | 단기 ciphertext·TTL·source unique |
| 신규 ReissueResponseCipher/KeyProvider/Properties/Configuration | 별도 AEAD·키 공급·fail-fast·flags |
| 신규 reissue transaction/result/domain types | 외부 wire와 내부 credential-containing 결과 분리, redacted toString |
| 계약/프론트 가이드·runbook·WORKLOG/CURRENT_STATE | 승인 정책·배포 gate·검증 기록 |

파일 이름은 상세 설계 제안이며 역할별 분리는 실제 저장소 관례를 따른다. 기존 Billing/LC·Firebase worker 자체 로직은 복사하거나 변경하지 않는다. 새 외부 이벤트·Jira·Git 브랜치도 이 문서 작성에서 생성하지 않는다.

### 6.2 테스트 매트릭스

| ID | 검증 |
| --- | --- |
| T01 | 필수 UUID v4 ID의 누락/빈 값/대소문자/길이/다중 헤더 처리, body/URL 유지 |
| T02 | Guest/LOCAL/Firebase 최초 회전, 현재 DB 유형과 기존 JWT/audience/scope 유지 |
| T03 | 같은 source+ID 응답 유실 재시도: 최초 token/jti/iat/exp 및 JSON 동일, 새 issuer/generator 호출 없음 |
| T04 | 다른 source+같은 ID 충돌, 다른 사용자 범위 분리, ID만으로 결과 접근 불가 |
| T05 | 같은 source+같은 ID 동시 요청: single child/payload, loser 재조회, max retry |
| T06 | 같은 source+다른 ID 경쟁: 승자 증거 확인 뒤 기존 의심 재사용 폐기, 오류 응답에도 commit |
| T07 | encryption/source save/child save/payload save 각 단계 실패 rollback, 기존 세션 생존 |
| T08 | commit 성공+응답 유실, commit 결과 불명, driver 확인/receipt 재조회 및 안전한 503 |
| T09 | PT2M 직전/동일/직후 경계, replay로 deadline 불변, 오래 걸린 commit/앱 재시작 |
| T10 | source 만료/TTL 부재, child/Access 조기 만료, 복구 목적으로 수명 연장 없음 |
| T11 | payload TTL 삭제 후 parent metadata 유지: 같은 ID는 복구 만료, 전 사용자 폐기 없음 |
| T12 | 기한 전 payload 누락/태그 변조/unknown key/키 공급 장애: 안전한 503·경보·새 발급 금지 |
| T13 | AEAD roundtrip·AAD의 각 field 치환·타 사용자/행/환경 replay 거절·nonce/key 회전 |
| T14 | plaintext/token/key/body/ID의 DB 문서·toString·예외·로그/트레이스 비노출 |
| T15 | 절대 만료 헤더와 최초 duration, replay 수신 시 만료 연장 없음, no-store |
| T16 | replay와 child 후속 rotation의 barrier 경합: old 결과로 최신 상태 변경 없음 |
| T17 | replay와 B 단일 logout 경합: child CAS, 선행 logout 뒤 반환 금지 |
| T18 | 응답 유실 중 A 단일 logout: exact B/복구 취소, 타 family·Firebase remote 호출 영향 없음 |
| T19 | logout-all epoch, 확인된 Firebase 폐기 경계와 replay 경합; LOCAL/정상 최신 세션 보존 |
| T20 | withdrawal/Guest upgrade/merge 선행·후행과 replay, ACCOUNT_WITHDRAWN 우선 및 구 계정 유형 응답 금지 |
| T21 | expired source/old epoch로 최신 전체 세션을 재사용 탐지 폐기하지 않음 |
| T22 | legacy metadata 없음 처리, partial unique 기존 누락 데이터와 공존, 실 인덱스 동작 |
| T23 | 정상 복구 만료/결과 대체됨과 의심 재사용 오류 구분, Guest 자동 초기화 없음 |
| T24 | flags 기본 OFF, enabled+fence OFF/키 없음/잘못된 설정 startup 실패 |
| T25 | 모바일 single-flight, 원자적 credential/pending 저장·앱 crash·백그라운드/계정 전환·늦은 응답 무시 |
| T26 | 모바일 400/409/401/503 bounded retry 분기, 같은 ID 유지·새 논리 회전만 새 ID |
| T27 | 실제 replica-set rollback/write conflict/commit 불명, transaction 지연·크기·다중 instance |
| T28 | actual key rotation/구 키 제거·backup restore/TTL 지연·비상 중단 리허설 |
| T29 | JWT/기존 API·단일 logout·withdrawal/owner events 및 전체 회귀 |
| T30 | 동일 원 credential+ID 동시 탈취의 한계 문서화, 다른 ID 충돌/반복 요청 rate limit·관측 |

Mock/in-memory Mongo 통과는 T27의 실제 Transaction 원자성 증거가 아니다. 기본 테스트는 외부 Provider/Atlas를 호출하지 않고 mock을 사용한다. 실제 인프라 테스트는 별도 승인된 staging/격리 replica set에서 수행한다.

### 6.3 구현·검증 순서

1. 승인 정책과 이 상세 계획을 검토하고 새 Jira 등록 내용을 사용자에게 제시한다. 생성 승인 후 발급 키를 계획·브랜치·WORKLOG에 연결한다.
2. 데이터 모델/시각 계약/오류·header DTO 경계·AEAD 인터페이스와 fake key 테스트 작성.
3. partial unique/TTL·키 공급/fail-fast·원자적 최초 rotation 구현. 기존 fence와 최소 회전 Transaction을 재사용한다.
4. replay 검증/child CAS/commit 불명·bounded 충돌 재조회 구현. 정상 만료와 의심 재사용의 저장·오류 순서를 검증한다.
5. single logout 복구 취소, logout-all/withdrawal/upgrade/merge 회귀, safe logging/metrics 구현.
6. 프론트 exact wire·절대 만료·single-flight·Guest 복구 만료 안내 및 신규 runbook 인계.
7. 집중 테스트, `./gradlew clean test`, `git diff --check`, 실제 변경 범위·credential 비노출 검토. flags OFF 유지.
8. 사용자 commit/PR/merge 및 기능 OFF 배포 후 키/인덱스/실제 Mongo·모바일 검증. Stage 7·8 운영 후속과 함께 추적하되 미완료를 면제하지 않는다.
9. 새 앱·서버 계약과 비상 중단 절차 확인 후 별도 승인으로 활성화. Jira 코드 완료와 운영 활성화 완료를 분리한다.

### 6.4 이번 계획서 작성 검증

- 현행 소스·Stage 8 계약·고정 순서와 사용자 승인 사항을 대조했다. 핵심 추가 조사 사항은 기존 duration 응답의 replay 시간 해석, ROTATED source 단일 logout NOOP, payload TTL과 요청 증거 수명 차이다.
- 문서 상대 링크·`git diff --check` 검증. 코드 변경 없이 문서만 작성하므로 Gradle 테스트는 재실행하지 않는다. 기존 695개 테스트 통과를 Stage 9 검증으로 인용하지 않는다.
- 기존 CURRENT_STATE/WORKLOG 및 Stage 8 계획서의 미커밋 변경을 보존한다. 애플리케이션·feature flag·Jira·다른 저장소·Git 이력·배포 변경 없음.

### 6.5 TMI-130 구현 결과 (2026-09-09)

- 최초 rotation/replay, 전용 AES-GCM·read-only key mount, 원 Session의 증거와 별도 ciphertext TTL, source/child/control CAS, bounded retry·unknown commit replay-only, 정확한 child logout 취소를 구현했다.
- 기존 controller에 필수 요청 ID·절대 만료 헤더 및 no-store를 연결했다. 기존 public JSON·JWT·workload·도메인 경계는 유지했다. cancelled recovery와 구 epoch/만료 source는 의심 재사용보다 먼저 차단한다.
- Mongo 저장 시각은 밀리초, Access 절대 만료는 실제 JWT exp를 기준으로 한다. 전용 transaction은 primary/snapshot/majority, maxCommitTime 5초·timeout 10초다.
- `AUTH_REISSUE_RECOVERY_MAINTENANCE` 및 환경별 AAD 구분 설정을 추가했다. keyring은 최대 8개 키를 startup에 로드하며 자동 reload·AWS 리소스 생성은 범위 밖이다.
- 테스트 대역의 partial index 미지원이 확인되어 프로덕션 인덱스는 그대로 유지하고, 인메모리 검증 범위를 인덱스 정의·대상 행 unique/CAS로 구분했다. 기존 Stage 8 fixture는 해당 신규 인덱스만 제외한다.
- 실제 운영 T25~T28과 부분 인덱스 legacy 다건 공존·실제 동시성/경보/rate limit 등 미검증 항목은 [T01~T30 매핑](refresh-token-response-recovery-stage-9-runbook.md#6-부록--테스트-및-운영-증빙-매핑)에 남겼다. mock 성공을 운영 검증으로 대신하지 않는다.
- 집중 테스트와 전체 `./gradlew clean test --console=plain` 통과. 전체 수치·최종 diff 검증은 WORKLOG에 기록한다. 사용자 기존 WORKLOG/미추적 계획서를 보존하고 commit·push·배포·Jira 상태 변경은 수행하지 않았다.
