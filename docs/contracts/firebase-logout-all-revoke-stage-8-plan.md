# Stage 8 — 전체 로그아웃과 Firebase 세션 폐기 구현 계획

- 작성일: 2026-09-08
- 상태: 2026-09-08 승인 정책 기준 로컬 구현·회귀 검증 완료. 기능 기본 OFF, 병합·배포·운영 검증 전. 세부 설계안과 실제 구현 매핑은 5.10절, 운영 활성화 조건은 [runbook](firebase-logout-all-revoke-stage-8-runbook.md)을 따른다.
- Jira: [TMI-129](https://to-teacher.atlassian.net/browse/TMI-129) — 해야 할 일. 등록 본문 사용자 승인 후 생성·재조회 확인.
- 조사 브랜치: Identity `develop`, 최근 사용자 JWT 작업 PR #40 병합 이후.
- 구현 브랜치: `feat/TMI-129-firebase-logout-all-revoke`.
- 범위: Identity 내부 RefreshSession 전체 폐기와 Firebase refresh revoke, 안전한 실패 복구 및 모바일 계약.
- 선행: 2026-09-08 사용자 승인으로 Stage 7 production 완료를 개발 착수 조건에서 제외한다. Stage 7·8은 기능 OFF 상태로 개발하고 운영 활성화 전 검증을 통합 수행한다. [고정 구현 순서](firebase-auth-follow-up-implementation-order.md)와 [Stage 7 계획](billing-entitlement-owner-fanout-stage-7-plan.md)의 consumer 준비·staging E2E·권한·Mongo·Firebase/mobile 검증은 활성화 조건으로 유지한다. 코드 구현은 승인됐으며 미검증 production 활성화는 승인되지 않았다.

## 1. 5줄 결론

1. 기존 `POST /api/v1/auth/logout-all`의 URL·무본문 요청·200 BaseResponse를 유지하고, 내부 세션 폐기와 Firebase 폐기 작업 저장을 하나의 Mongo Transaction으로 묶는다. [현재 서비스](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/LogoutAllService.java)
2. Firebase revoke 대기·결과 불명만으로 새 로그인을 막지 않는다. 요청 이후의 새 인증을 확인하고, logout과 발급이 같은 사용자별 문서를 CAS 갱신하여 이전 인증·동시 발급의 우회를 막는다. [현재 발급 저장](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/RefreshSessionIssuer.java)
3. 결과 불명인 revoke는 자동 재전송하지 않고 확인 조회·운영 조사로 분리한다. 조사 중에도 유효한 새 인증은 허용하며 DB lease를 원격 호출 취소 보장으로 설명하지 않는다. [신규 revoke adapter](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/infrastructure/firebase/FirebaseSessionRevocationHttpAdapter.java)
4. 지연 revoke가 영향을 준 Firebase 기반 자체 세션만 추가 무효화한다. LOCAL·영향 없는 새 인증까지 user epoch를 다시 올려 일괄 폐기하지 않는다. User 삭제·SNS unlink·혜택 이벤트·downstream Access Token 즉시 차단은 제외한다.
5. 200은 내부 보안 처리·durable 접수이며 Firebase 완료가 아니다. 같은 검증 jti는 같은 요청이다. Stage 7 운영 증빙과 모바일·Firebase staging 검증 전 production 활성화는 금지한다.

## 2. 사용자가 반드시 읽어야 하는 내용

### 2.1 지금과 달라지는 점

변경 전에는 내부 RefreshSession만 `LOGOUT_ALL`로 폐기했다. Firebase SDK의 로그인 유지 정보가 남아 있으면 Firebase 인증을 다시 제출하여 Identity 세션을 만들 수 있었다. 이번 구현은 아래 흐름을 추가하며, 기본 OFF인 동안에는 기존 동작을 유지한다. 이 문서의 설계 당시 조사·예상 변경은 운영 배포 상태를 뜻하지 않는다.

목표 흐름은 다음과 같다.

1. 로그인된 사용자가 전체 로그아웃을 요청한다.
2. DB Transaction에서 sessionEpoch를 올리고 기존 내부 세션을 폐기하며 요청 시각의 Firebase 인증 경계와 revoke 작업을 저장한다.
3. 200 응답 후 앱은 현재 기기의 자체 인증정보와 Firebase 로그인 상태를 정리한다.
4. 작업 대기 중에도 사용자는 새롭게 인증하여 로그인할 수 있다. 과거 Firebase 인증을 단순 갱신한 Token은 허용하지 않는다.
5. worker가 정확한 Firebase 계정을 검사하고 revoke한다. 확인된 폐기 경계를 저장하여 그 영향을 받은 Firebase 기반 자체 세션만 무효화한다. 영향받은 앱은 다음 인증 실패 시 정리·재로그인을 안내한다.
6. 결과 불명인 작업은 조사 대상으로 남기되 로그인 자체의 대기 조건으로 사용하지 않는다. Firebase 자체 장애·계정 손상·탈퇴 등의 기존 거절 조건은 유지한다.

**200은 내부 세션 무효화·이전 Firebase 인증 재사용 차단·외부 작업의 내구성 있는 접수 완료를 뜻한다. 모든 기기의 화면이 즉시 바뀌거나 Firebase 원격 작업까지 완료됐다는 뜻이 아니다.** 기존 stateless 사용자 Access Token은 각 서비스의 기존 만료·차단 정책을 따른다.

### 2.2 계정 유형별 동작

| 현재 User/연결 상태 | 목표 |
| --- | --- |
| ACTIVE Guest, FirebaseIdentity 없음 | 내부 세션 폐기만 원자 처리. Firebase 호출 없음 |
| ACTIVE LOCAL Member, FirebaseIdentity 없음 | 내부 세션 폐기만 원자 처리. 로그인 수단은 유지 |
| ACTIVE User에 유효한 FirebaseIdentity 있음 | 내부 폐기 + Firebase revoke 접수 + 새 인증 로그인 허용. UserProvider 이름만으로 분기하지 않음 |
| FEDERATED Member인데 FirebaseIdentity 없음 또는 연결 불일치 | 내부 세션은 폐기하고 원인 격리·경보. 손상된 연결로 Firebase 로그인은 허용하지 않음. 유효한 독립 LOCAL 인증까지 작업 상태만으로 차단하지 않음 |
| WITHDRAWN/MERGED source | 기존 계정 상태 오류 우선. successor/merge target으로 사용자를 자동 치환하지 않음 |
| 미폐기 내부 세션 0건, FirebaseIdentity 있음 | 조기 반환 금지. Firebase 세션은 존재할 수 있으므로 정상 revoke 절차 수행 |

한 Firebase UID에 Google·Apple·Kakao가 연결되어 있으면 같은 Firebase 계정 단위로 revoke한다. Provider별 로그아웃은 아니며 Google·Kakao 사이트의 로그인 쿠키나 외부 SNS 계정을 삭제하지 않는다.

### 2.3 이번 범위와 제외 범위

- 포함: logout Transaction, 사용자별 발급 fencing, durable revoke lifecycle, worker·확인·재시도·격리, 최소 관리 지표, 모바일 오류·재로그인 계약, 관련 테스트·운영 절차.
- 세션 발급/회전과 logout 경합을 막는 **최소 Transaction 보강**은 포함한다. 이 변경 없이는 logout 시 조회에서 빠진 새 Session이 살아남을 수 있다.
- Stage 9의 동일 Refresh Token 응답 재전달·원문 응답 보관·복구용 토큰 정책·재사용 탐지 정책 개편은 제외한다. 최소 원자성 보강과 응답 유실 복구를 혼동하지 않는다.
- 기존 `/logout` 단일 세션 폐기는 Firebase Admin revoke를 호출하지 않는다.
- 탈퇴·Firebase User 삭제·disable·Provider 삭제 의무·Social/Phone release·Billing/LC 이벤트·JWT 신규 claim·새 로그인 endpoint는 제외한다.
- 실제 모바일 구현은 프론트 담당이며 이 저장소에서는 계약과 서버 테스트를 제공한다.
- 기존 탈퇴 target guard·CLEANED gate·원자적 identity release는 재구현하지 않는다. 신규 revoke 작업과 기존 lifecycle의 경합 방지 연계 및 테스트는 포함한다.

## 3. 사용자가 결정해야 하는 사항 — 승인 정책과 남은 조건

2026-09-08 사용자가 선택지 1-A/2-A/3-A/4-A/5-A를 승인했다. 기존 D 번호를 유지하여 매핑하며 D6을 추가한다. 이전의 pending 전체 발급 차단안은 폐기한다.

| ID | 승인 정책 / 조건 | 이점 / 대가 |
| --- | --- | --- |
| D1 | 기존 200은 로컬 보안 처리 + durable 접수 완료로 정의. Firebase 완료까지 HTTP 요청을 붙잡지 않음 | 외부 장애와 HTTP timeout 분리 / Firebase 전체 폐기는 나중에 끝날 수 있음 |
| D2 (선택 1-A) | pending·결과 불명이어도 새 인증 로그인 허용. 요청 이전 Firebase auth_time 및 구 epoch는 거절 | 로그인 가용성 유지 / 지연 revoke로 추가 재로그인 가능 |
| D3 | 결과 불명 mutation은 자동 재전송하지 않고 reconciliation. 읽기 확인·명백한 호출 전 실패만 자동 재시도 | 늦은 재호출이 새 인증을 끊는 위험 감소 / 운영 조사 필요 가능 |
| D4 | 같은 검증된 Access Token의 jti는 같은 logout 요청으로 처리. 새로운 로그인에서 받은 새 jti는 새 요청 | 응답 유실·연속 클릭 중복 방지, 새 Request DTO 불필요 / 같은 Access Token으로 별도 logout cycle 생성 불가 |
| D5 (2026-09-08 순서 조정 승인) | Stage 7 production 완료 전 Stage 8 코드·격리 테스트 개발 허용. Stage 7·8 운영 활성화 전 통합 검증 | 개발 대기 제거 / 실제 기능 활성화는 통합 검증 및 consumer 준비 확인 이후 |
| D6 (선택 2-A) | 지연 revoke에 영향받은 Firebase 인증 기반 자체 세션만 추가 무효화. user epoch 재증가 금지 | LOCAL·영향 없는 세션 보존 / 세션의 인증 출처·시각 보존 및 회전 전파 필요 |

D1은 선택 4-A, D3은 선택 3-A, D4는 선택 5-A다. D3의 운영 조사는 사용자 로그인 차단과 분리한다. Admin SDK의 공개 revoke 메서드는 시각 인자를 받지 않지만, 설치된 9.4.3 내부 구현은 호출 시각을 초 단위 `validSince`로 전송한다. 이번 adapter도 영속화한 dispatchAt을 같은 방식으로 사용한다. logout 접수 이후 dispatch 이전의 새 인증은 영향을 받을 수 있으며 추가 재로그인이 정확히 한 번만 발생한다고 보장하지 않는다.

남은 조건: 실제 Firebase 정밀도·원격 종료 증거, 운영 timeout/backoff 검증, Stage 7·8 통합 운영 검증, 모바일 오류 처리. SDK hidden retry는 소스에서 확인하여 재시도 없는 별도 HTTP adapter로 분리했다. 신규 오류는 `SESSION_LOGGED_OUT`(401), `SESSION_SECURITY_UNAVAILABLE`(503)로 구현했다. 실제 배포·활성화는 별도 확인 대상이다.

## 4. 주요 위험과 미확인 사항

| 위험 | 대응·확정 기준 |
| --- | --- |
| DB lease 종료 뒤 늦은 Firebase mutation | 외부 API는 lease/generation을 모른다. mutation 시작 이후 자동 lease 회수로 재전송 금지. 결과 불명은 격리 |
| revoke 성공 직후 DB 저장 실패 | 같은 작업을 무조건 revoke 재실행하지 않음. `VERIFYING`에서 read-only 확인, 증거가 부족하면 격리 |
| 앱에서 pending 중 Firebase 재로그인 | 요청 시각 이후 새 인증 허용. 지연 revoke의 확인된 경계에 영향받은 자체 세션만 추가 무효화 |
| 확인 조회 지연 또는 중복 완료 | 조회 완료 시각을 폐기 경계로 사용하지 않음. 실제 validAfter 증거의 단조 갱신만 적용하여 영향 없는 새 인증과 LOCAL 보존 |
| 회전 중 인증 출처 유실 | exact binding·authTime을 refresh 자식에 그대로 전파. 누락을 LOCAL로 추정하여 우회시키지 않음 |
| `auth_time`과 Firebase validAfter 정밀도 차이 | 초 단위 보수적 경계, 경계 시각과 같은 인증 거절. SDK 9.4.3 동작과 실제 staging으로 검증 |
| 조회 후 Session insert의 Mongo write skew | 공통 guard 문서 CAS + Session 저장을 같은 Transaction으로 묶음. 단순 gate 조회는 불충분 |
| 구버전 instance가 gate를 무시하고 발급 | 모든 Session writer 전환 완료 전 capture ON 금지 |
| 타 worker·탈퇴 cleanup과 동일 UID 조작 | User·FirebaseIdentity exact target 및 공통 guard/handoff. 미해결 mutation 동안 release/rebind 금지 |
| Firebase 관리자가 UID를 강제로 재생성 | 외부 원자적 precondition 없음. 콘솔·외부 자동화의 UID 재사용 금지 및 운영 통제 필요; DB CAS만으로 방지 불가 |
| logout 응답 유실 | 같은 jti 재시도 조회. DB commit 결과 불명도 무작정 새 generation을 만들지 않음 |
| 과도한 Session 수로 Transaction timeout | epoch 무효화를 보안 기준으로 삼고 세션 물리 마킹은 bounded 처리. 아래 fallback 외 무제한 saveAll 금지 |

외부 사실의 참고 원문: [Firebase 세션 관리](https://firebase.google.com/docs/auth/admin/manage-sessions), [Admin Java FirebaseAuth](https://firebase.google.com/docs/reference/admin/java/reference/com/google/firebase/auth/FirebaseAuth), [UserRecord](https://firebase.google.com/docs/reference/admin/java/reference/com/google/firebase/auth/UserRecord). 이번 작업에서는 저장소 코드를 확인했으며 SDK 자동 retry·전송 timeout·원격 처리 종료 보장은 새로 실험하지 않았다. 구현 시작 시 설치된 9.4.3 코드/공식 문서와 staging으로 해당 조건을 확인한다.

## 5. 현재 작업과 직접 관련된 구현 설명

### 5.1 데이터 모델 — 신규 제안

이름은 구현 시 패키지 관례에 맞출 수 있지만 역할과 불변식은 유지한다.

#### A. `UserSessionControl` — User별 하나

| 필드 | 의미 |
| --- | --- |
| userId | `_id`, 실제 canonical UUID |
| sessionEpoch | 서로 다른 전체 로그아웃 요청 때 증가하는 전체 세션 경계. legacy 기본 0. 지연 revoke 확인 시에는 증가시키지 않음 |
| revision | 발급·logout·관련 계정 전환이 공유하는 CAS revision |
| activeLogoutId | exact binding에서 원격 dispatch/확인을 점유한 operation. 새 로그인 허용 여부와 독립적이며 새 cycle은 대기열에 저장 |
| firebaseBindingId | watermark가 적용되는 exact FirebaseIdentity ID |
| minimumFirebaseAuthTimeExclusive | 새 Firebase 발급용 경계: 요청 시각 경계와 확인된 원격 폐기 경계의 max. 단조 증가 |
| confirmedFirebaseRevocationBoundary | 해당 binding에서 증거로 확인한 원격 폐기 경계. 기존 Firebase 기반 세션의 선택적 무효화에 사용. 조회 완료 시각은 사용하지 않음 |
| updatedAt | 서버 시각 |

단순 운영 이력이 아니라 인증 상태이므로 ACTIVE User가 존재하는 동안 TTL로 지우지 않는다. epoch를 삭제하면 legacy Session이 다시 유효해질 수 있다. User 삭제 정책으로 정리하려면 모든 Session 만료와 unresolved 작업 없음 및 binding release를 먼저 확인한다.

epoch·watermark·인증 증빙은 Identity 내부 DB에서만 관리한다. Firebase/프론트/Billing/Learning Core에 sessionEpoch를 전달하거나 JWT claim을 추가하지 않는다. binding 변경 시 과거 증빙이 살아 있는 세션·작업을 먼저 해결해야 하며 단일 control의 binding 교체로 보호 상태를 덮어쓰지 않는다.

#### B. `LogoutAllOperation` — 내구성 있는 작업과 중복 요청 결과

| 필드 | 의미 |
| --- | --- |
| logoutId / userId / epoch | UUID 작업 ID와 immutable 대상·cycle |
| requestFingerprint | 검증된 issuer + userId + jti의 길이 구분 직렬화를 SHA-256 처리. 원문 Access Token 미저장 |
| requestExpiresAt | 중복 요청의 인증 유효 기간 산정용, 검증된 exp |
| firebaseIdentityId / projectId / tenantId / firebaseUid / bindingCreatedAt | 내부 접근 제한 대상 snapshot. tenant 없음은 명시적으로 표현 |
| remoteCreationTime | inspect에서 확보한 외부 계정 생성 시각. 자동 수정·대상 교체 금지 |
| requestedAt / revokeStartedAt / terminalAt | 내부 요청·외부 호출 시작·안전 종료 시각 |
| status / failureCode / attemptCount / nextAttemptAt | 작업 상태와 제한된 실패 분류 |
| leaseOwner / leaseExpiresAt / version / mutationStarted | CAS 및 미해결 외부 호출 식별 |
| observedValidAfter / completionAuthBoundary | exact target에서 확인한 Firebase 값 및 이에 근거한 폐기 경계. 조회/작업 완료 시각과 구분 |
| boundaryAppliedAt | 확인된 경계를 control에 반영한 증빙. 중복 확인은 같은 경계에 대해 NOOP이며 종료 확인과 독립 |
| cleanupAt | 완료된 작업만 설정하는 TTL |

Raw Token, 이메일, 전화번호, Provider credential, 원본 SDK 예외 메시지는 저장·로그하지 않는다. Firebase UID/target은 worker 수행에 필요한 내부 저장 필드이고 API·로그·metric label에는 노출하지 않는다.

인덱스: `requestFingerprint` unique, `(userId, epoch)` unique, due 조회 `(status,nextAttemptAt)`, `cleanupAt` TTL. 모든 신규 logout은 UserSessionControl CAS로 직렬화한다. 같은 jti의 receipt는 같은 operation을 참조하고 token 원문은 없다.

새 로그인 Token의 다른 jti로 요청하면 이전 작업 대기 중에도 별도의 epoch 증가·operation/receipt를 원자 생성한다. 새 요청을 이전 receipt로 흡수하여 그 사이 생긴 자체 세션을 남겨두지 않는다. 원격 dispatch는 exact binding당 하나만 허용하고 새 operation은 PENDING에서 대기한다. 미해결 actor는 후속 원격 dispatch를 막지만 새 인증 로그인은 막지 않는다. 실제로 여러 번 요청한 logout은 여러 번의 추가 재로그인을 유발할 수 있으므로 앱은 새 Token으로 자동 재요청하지 않는다. 큐 과다 요청 제한은 기존 요청 제한 정책과 구현 시 함께 검증하며 durable 접수 없이 200으로 흡수하지 않는다.

#### C. 기존 `RefreshSession` 확장

- `sessionEpoch` 추가. 기존 문서의 누락은 0으로만 호환한다.
- 서버 검증에서 만든 인증 출처(LOCAL/FIREBASE/GUEST 등), exact `firebaseBindingId`, Firebase `authTime`을 세션 인증 증빙으로 추가한다. UserAccountType·현재 UserProvider만으로 개별 세션의 인증 출처를 추정하지 않는다. 원문 Token/credential은 저장하지 않는다.
- 신규 세션은 준비 시점에 읽은 epoch를 가지고, 저장 Transaction에서 동일 epoch·User ACTIVE·해당 인증 proof의 유효성을 재확인한다. activeLogoutId 존재만으로 거절하지 않는다. Firebase proof는 최신 발급 watermark를 넘어야 한다.
- refresh는 계정 상태/기존 폐기 사유 확인 후 현재 epoch와 세션 epoch를 비교한다. mismatch는 전체 로그아웃된 세션으로 거절한다. 새 epoch로 복사하여 되살리지 않는다.
- epoch가 같아도 FIREBASE 세션의 exact binding·authTime이 확인된 폐기 경계에 해당하면 거절한다. LOCAL과 경계 이후 Firebase 인증은 유지한다. 각 rotation 자식에 원래 인증 증빙을 그대로 복사하며 재발급 시각을 authTime으로 만들지 않는다.
- legacy 출처/인증 시각 누락을 LOCAL로 기본 처리하지 않는다. 활성 폐기 경계가 적용되는 계정에서 해당 세션이 영향 없음을 입증하지 못하면 재인증을 요구한다. 신규 세션의 증빙 누락은 저장 실패로 처리하고, 구버전 writer 제거·legacy 영향 분석을 activation gate로 둔다.
- `RevocationReason`의 `LOGOUT_ALL`을 재사용한다. 기존 WITHDRAWN/MERGED/ROTATED 우선순위는 무분별하게 바꾸지 않는다.

### 5.2 전체 로그아웃 Transaction

1. 기존 JWT 인증에서 검증된 `sub`, `iss`, `jti`, `exp`를 얻는다. 요청 Body userId/Firebase UID를 받지 않는다. feature ON endpoint는 jti 누락/비정상 형식을 안전한 401로 거절하며 다른 기존 API에 jti 필수 검증을 확대하지 않는다. 현재 issuer는 UUID jti를 발급한다.
2. 동일 requestFingerprint가 있으면 기존 접수 결과로 200 반환한다. User가 WITHDRAWN/MERGED이면 해당 상태 오류를 우선하고 target으로 재해석하지 않는다.
3. ACTIVE User의 control을 생성/조회하고 CAS 갱신한다. 최초 생성 경쟁은 unique 충돌 후 bounded 재조회한다.
4. 같은 jti는 2번의 기존 결과를 유지한다. 다른 jti의 새 요청은 pending 여부와 무관하게 새 cycle이다. 앞선 actor가 있으면 원격 dispatch만 순서대로 대기시킨다.
5. 새 요청이면 sessionEpoch를 증가시키고 exact FirebaseIdentity 대상 snapshot 및 요청 시각을 확보한다. binding별 발급 watermark를 요청 시각의 초 단위 보수적 경계 이상으로 올려 과거 Firebase 인증의 재교환을 막는다. 전체 로그아웃·binding 생성/삭제·승격/병합은 같은 control의 충돌 경계를 사용한다.
6. 같은 Transaction에서 기존 미폐기 세션을 `LOGOUT_ALL` 마킹하고 operation/receipt를 저장한다. 원격 slot은 비어 있을 때만 해당 operation이 점유하며 기존 activeLogoutId를 덮어쓰지 않는다. Firebase 대상이 없는 정상 LOCAL/Guest는 내부 처리 완료 상태로 바로 종료한다.
7. commit된 경우에만 200. 실패/commit 불명은 503이고 같은 jti 재시도로 조회한다. 외부 Firebase HTTP는 DB Transaction 안에서 호출하지 않는다.

큰 세션 수: epoch 증가로 기존 세션은 즉시 재발급 불가가 된다. 단일 Transaction 마킹 상한의 제안은 1,000건이며 초과하면 첫 요청에서 전체 문서를 읽거나 수정하지 않고 epoch 폐기를 기준으로 제한된 batch worker가 나머지를 마킹한다. 상태상 epoch 폐기는 완료됐으므로 로그에 물리 마킹 건수와 논리 폐기를 구분한다. 이 fallback은 모든 refresh·credential 검증 경로가 epoch를 검사한 뒤에만 허용한다. 검증이 누락된 환경에서는 기능을 켜지 않는다.

### 5.3 Session 발급과 관련 mutation의 공통 fencing

pending 여부만으로 허용/거절하면 logout 직후 이전 인증으로 Session을 저장하는 경합을 막지 못한다. 다음을 같은 Mongo Transaction에서 수행한다.

- 최신 User 상태·expected epoch·인증 증빙과 최신 해당 binding의 watermark 확인. pending 자체는 새 로그인 거절 조건이 아니다.
- 같은 UserSessionControl의 revision을 조건부 증가시켜 logout과 쓰기 충돌 유발.
- prepared Session의 epoch를 유지한 저장. 충돌 시 이전 인증 컨텍스트를 새 epoch에 자동 승격하지 않음.
- 외부 검증은 Transaction 밖에서 수행하되 캡처한 proof/epoch를 commit 때 재검사한다.
- Access Token을 미리 계산하더라도 Session Transaction commit 실패 시 응답으로 반환하지 않는다. 이미 commit/응답 중인 Access Token과 logout의 경쟁은 기존 stateless TTL 경계이며 이 단계의 즉시 downstream 차단 보장이 아니다.

| 경로 | 적용 |
| --- | --- |
| Guest 등록 | User/control 초기 생성과 최초 Session 저장을 기존 Transaction에 합침 |
| LOCAL login / Firebase exchange | 인증 결과의 expected epoch를 Session 저장 Transaction으로 전달. Firebase는 auth_time watermark도 재검사 |
| Firebase signup | 신규 User/control/binding/Session을 기존 가입 Transaction에서 생성 |
| Guest upgrade | 같은 user control CAS, 승격·binding·Session 저장을 한 Transaction으로 유지 |
| Guest merge | source/target control을 userId 고정 순서로 CAS. target 새 인증·watermark 및 기존 ownership 규칙 확인. pending만으로 거절하지 않고 target에 source epoch를 복사하지 않음 |
| refresh | epoch와 Firebase 인증 증빙/확인된 폐기 경계 검증·회전·후속 저장을 공통 Transaction으로 묶음. 외부 API 호출·응답 유실 replay 저장은 추가하지 않음 |
| auth methods sync / Guest prepare·owner 판정 | 새 proof·watermark를 확인하고 뒤의 mutation에서 control 재검증. 일반 sync/로그인을 pending만으로 차단하지 않음. exact binding 교체·release 등은 별도 actor 보호 조건 적용 |
| withdrawal·release / 가입중단 cleanup | 계정 상태 우선. unresolved logout target을 새 binding에 넘기거나 삭제 완료로 숨기지 않음. 아래 handoff 적용 |

`RefreshSessionIssuer.savePrepared`/`issueRotated`만 수정하고 호출 전 인증 snapshot을 무시하면 안 된다. Repository 직접 save 경로 및 withdrawal credential 검증도 검색하여 epoch 검사 누락을 막는다. JVM synchronized, Redis 락, 무조건 `@Version` 존재만으로 보장한다고 하지 않는다.

### 5.4 Firebase revoke port와 worker

신규 최소 port 제안:

```java
FirebaseSessionSnapshot inspect(FirebaseSessionTarget target);
void revokeRefreshTokens(FirebaseSessionTarget target);
```

snapshot은 계정 존재·disabled·creationTime·tokensValidAfterTime만 반환하고 SDK 객체/개인정보는 application 계층에 노출하지 않는다. 기존 탈퇴 port는 disable/delete도 포함하므로 그대로 주입하지 않는다. SDK 초기화·tenant 선택·안전한 실패 분류 같은 검증된 공통 부분만 공유한다.

worker 순서:

1. due 작업 lease claim → User/control/activeLogoutId 및 exact binding 재조회. 더 최신 logout으로 control epoch가 올라도 이전 원격 actor를 없어진 것으로 처리하지 않는다. operation epoch는 immutable하고 exact slot·lease/version으로 현재 책임을 검증한다.
2. project·tenant·binding ID·UID·생성 시각 불일치는 외부 호출 없이 reconciliation.
3. inspect 결과 remote account disabled/not-found이면 ACTIVE 연결 손상으로 격리한다. 삭제 성공으로 간주하거나 새 UID를 찾아 revoke하지 않는다.
4. 원격 mutation 전에 DB에 `mutationStarted=true`, `revokeStartedAt`를 CAS 저장한다. 이 commit 성공을 확인한 worker만 단 한 번 dispatch한다.
5. Firebase revoke를 실행한다. SDK/HTTP 자동 mutation retry를 비활성화할 수 있는지 검증한다. 불가능하면 hidden retry/in-flight 종료 증거 없이 완료 처리하지 않고 activation gate로 둔다.
6. 응답 성공 뒤 `VERIFYING`으로 저장하고 read-only inspect로 tokensValidAfterTime과 exact target을 확인한다.
7. exact target의 유효한 validAfter 증거를 얻으면 control의 확인된 폐기 경계와 발급 watermark를 단조 갱신한다. Firebase 기반 세션의 선택적 무효화가 이 경계부터 적용된다. 요청 결과가 불명이어도 확인된 경계는 반영할 수 있으나 이것이 actor 종료 증거는 아니다.
8. 해당 dispatch 종료가 확인되고 다른 미해결 dispatch가 없으며 증거가 충분하면 `COMPLETED`와 slot 해제를 CAS Transaction으로 저장한다. 이후 대기 operation을 진행한다. 최신 epoch/요청 watermark를 덮어쓰지 않고 사용자 epoch를 추가 증가시키지 않는다. 중복 완료·동일 validAfter 확인은 추가 효과가 없다.

**불확실한 timeout/connection reset/process crash는 실패 확정이 아니다.** 서버에 도착했을 수 있는 요청은 결과 불명으로 격리한다. validAfter가 전진했다는 사실 하나만으로 뒤늦은 다른 호출이 없다고 추론하지 않는다. 외부 응답을 잃은 경우 원격 요청 종료를 증명할 수 없으면 자동 완료/재전송하지 않는다. 운영자가 조사 후 승인해야 하며 “일정 시간 기다렸으니 안전”을 증거로 삼지 않는다.

결과 불명 조사는 로그인 금지 상태가 아니다. control/인증 검증이 정상이고 요청·확인된 폐기 경계 이후 새 인증이면 발급한다. 실제 원격 폐기와 로컬 경계 반영 사이 지연은 있을 수 있으며 즉시 동기화를 보장하지 않는다. read-only 확인도 무한 고빈도 반복하지 않고 bounded 재시도 후 운영 조사로 전환한다.

성공 후 DB 장애만 발생하고 원 호출 종료 증거를 프로세스가 갖고 있으면 같은 operation에 확인 증거를 CAS 재저장할 수 있다. 프로세스 소실로 그 증거까지 잃으면 reconciliation으로 보수적으로 내려간다. 다른 worker가 lease를 인수해 revoke를 다시 보내지 않는다.

### 5.5 상태와 재시도

| 상태 | 허용 동작 | 새 발급 |
| --- | --- | --- |
| PENDING | slot 대기 또는 claim·target inspect | 유효한 새 인증 허용 |
| CLAIMED | mutation 시작 전 preflight; lease 소실 시 외부 호출 금지 | 유효한 새 인증 허용 |
| REVOKING | 단 한 worker의 추적된 원격 dispatch | 유효한 새 인증 허용 |
| VERIFYING | read-only 확인·DB 결과 저장 재시도 | 최신 경계 이후 새 인증 허용 |
| RETRY_WAIT | 외부 호출 전 안전한 실패 또는 read-only 확인 실패 후 대기 | 유효한 새 인증 허용 |
| RECONCILIATION_REQUIRED | 결과 불명·권한/target 오류·재시도 소진; 자동 mutation 없음 | 작업 상태만으로 차단하지 않음. 연결 손상·disabled·기존 계정 오류는 별도 거절 |
| COMPLETED | 외부 정상 확인 또는 정상 LOCAL/Guest 내부 처리 종료 | 신규 정상 인증 허용 |
| SUPERSEDED_BY_WITHDRAWAL | exact 탈퇴 lifecycle로 안전한 책임 인계 확인 | 탈퇴 gate로 차단 |

- claim 후 mutation 시작 기록이 없는 lease 만료는 재claim 가능. 시작 기록이 있으면 재claim으로 mutation 재실행하지 않는다. CAS는 DB 쓰기를 막을 뿐 일시 정지된 프로세스의 외부 요청 자체를 취소하지 못하므로 진행 중 actor를 확인 대기로 유지한다.
- retry는 exponential backoff + full jitter 제안: base `PT5S`, cap `PT5M`, 최대 8회. bounded Retry-After는 1~300초 범위만 사용. SDK가 제공하지 않는 HTTP 헤더를 임의로 추정하지 않는다.
- timeout·5xx·429를 모두 동일 재시도 대상으로 분류하지 않는다. mutation 요청 이후라면 provider가 미적용을 보증한 오류만 retry 가능; 보증이 없으면 결과 불명이다.
- configuration/permission/target 불일치·disabled/not-found는 즉시 격리. 기존 계정·작업 정보를 덮어쓰지 않는다.
- polling `PT5S`, batch 20, lease `PT60S`, transport connect `PT3S`, read `PT10S`를 제안한다. timeout은 원격 취소 보장이 아니다. 실제 SDK 설정 가능 여부가 수치보다 우선이다.

### 5.6 새 인증 시각 경계와 선택적 자체 세션 무효화

- 폐기 비교에는 Firebase `issuedAt`이 아니라 `authTime`을 사용한다. 강제 refresh는 auth_time을 새 로그인으로 바꾸지 않는다.
- logout 원자 접수 시 `minimumFirebaseAuthTimeExclusive = max(이전 watermark, floorToSecond(requestedAt))`를 저장한다. 요청 이전 Firebase 인증의 silent exchange를 차단한다. LOCAL 로그인도 인증 시작에 캡처한 epoch를 commit 시 재검사하여 logout 이전 인증 결과를 새 epoch로 승격하지 않는다.
- exact target의 폐기 증거 확인 시 `confirmedFirebaseRevocationBoundary = max(기존 확인 경계, ceilToSecond(observedValidAfter))`, 발급 watermark는 `max(이전 watermark, confirmedFirebaseRevocationBoundary)`로 갱신한다. **조회 완료 시각·작업 완료 시각은 경계에 넣지 않는다.** 조회가 늦었다는 이유로 영향 없는 새 인증까지 폐기하지 않기 위해서다.
- 로그인 허용 조건은 `principal.authTime > watermark`이며 equality를 허용하지 않는다. 정상 clock skew 허용치를 revocation 경계에서 빼서 옛 인증을 통과시키지 않는다. 동일 초 재로그인은 다음 초에 재인증이 필요할 수 있다.
- 기존 자체 세션은 현재 epoch와 비교한 뒤, FIREBASE 출처이고 동일 exact binding이며 `session.authTime <= confirmedFirebaseRevocationBoundary`인 경우 추가 거절한다. pending 중 생성됐다는 사실만으로 거절하지 않는다. LOCAL·경계 이후 인증은 유지한다. 경계 증거가 아직 없으면 임의로 완료 시각을 사용해 전체 세션을 끊지 않는다.
- 세션 발급/refresh/credential 소비와 경계 저장은 같은 control CAS로 직렬화한다. 논리적 차단을 먼저 적용하고 물리적 LOGOUT_ALL 마킹은 bounded batch로 할 수 있다. 모든 credential 소비 경로가 조건을 검사해야 하며 rotation family 전체에 원래 인증 증빙을 유지한다.
- 현재 verifier의 `verify(..., true)`는 유지한다. SDK 9.4.3 validAfter/authTime 정밀도와 경계 비교를 실제 검증하기 전 이 수식을 운영 확정 수치로 취급하지 않는다. 경계 초의 보수적 거절은 가능하지만 넓은 관측 완료 시간까지 차단 범위를 확대하지 않는다.
- watermark는 exact binding에 귀속한다. 향후 rebind 시 이전 UID의 watermark를 새 UID에 복사하거나 지우는 정책은 Stage 12에서 명시적으로 정한다.

### 5.7 탈퇴·release와의 상호작용

- logout 먼저 commit하면 기존 RefreshSession credential은 폐기되어 뒤의 탈퇴 요청이 기존 credential로 진행되지 않는다. 이미 검증 진행 중인 탈퇴도 commit 때 control/Session 상태를 확인해야 한다.
- 탈퇴 먼저 commit하면 logout은 WITHDRAWN 상태를 확인하고 새 작업을 생성하지 않는다.
- pending logout 이후 별도로 유효하게 승인된 탈퇴 lifecycle이 존재하는 경우: 원격 revoke 미시작 작업만 exact withdrawalId로 인계 가능. 시작/결과 불명 작업은 unresolved actor를 탈퇴 lifecycle에 연결하여 release/rejoin이 기다리게 한다.
- Stage 2 delete 완료만으로 unresolved Stage 8 actor를 소멸 처리하지 않는다. Stage 3 CLEANED/release 및 이후 같은 Firebase UID 재가입은 해당 actor가 더 이상 dispatch할 수 없다는 증거 확인 전 차단한다. Firebase 계정 자체 삭제는 Stage 2의 책임이다.
- 새 binding/phone owner를 조회해서 과거 logout target을 수정하지 않는다. 기존 Stage 6 abandoned cleanup은 active binding을 정리 대상에 포함하지 않는 경계를 유지한다.

### 5.8 API·모바일 계약 — 서버 구현 / 모바일 검증 대기

기존 API:

```http
POST /api/v1/auth/logout-all
Authorization: Bearer <사용자 Access Token>
```

요청 Body·새 URL 없음. 기존 URL의 하이픈은 이번에 변경하지 않으며 신규 하이픈 URL도 만들지 않는다. 정상 접수 응답은 현재와 같다.

```json
{"isSuccess":true,"code":"SUCCESS","message":"요청에 성공했습니다.","result":null}
```

| 상황 | 제안 응답 / 앱 동작 |
| --- | --- |
| logout 원자 접수·동일 요청 재시도 | 200. 현재 기기 자체 Token 삭제 + Firebase signOut. “전체 로그아웃을 요청했습니다” 안내 |
| pending/결과 불명 중 새 로그인 | 새 인증·최신 watermark·기존 계정/소유권 조건 통과 시 기존 정상 로그인 응답. 작업 상태만의 409/503은 추가하지 않음 |
| epoch 불일치 또는 실제 LOGOUT_ALL 폐기 세션 재발급 | 신규 401 `SESSION_LOGGED_OUT`. 로컬 인증정보 정리, 자동 exchange 금지, 로그인 안내 |
| 지연 revoke 경계에 영향받은 Firebase 기반 자체 세션 재발급 | 신규 401 `SESSION_LOGGED_OUT` 재사용 제안. Firebase signOut + 자체 Token 삭제 후 “전체 로그아웃 요청 처리로 다시 로그인이 필요합니다.” 안내. 영향 없는 LOCAL/새 인증은 유지 |
| watermark 이전 Firebase proof | 기존 401 `FIREBASE_RECENT_AUTH_REQUIRED`. 강제 refresh만 반복하지 말고 재인증 |
| Firebase가 먼저 revoked Token을 거절 | 기존 `INVALID_FIREBASE_ID_TOKEN` 유지 가능. 폐기 원인을 확인하지 못했는데 전용 logout 오류로 위장하지 않음 |
| unresolved 작업과 탈퇴/release/binding 변경 경합 | 기존 탈퇴·소유권 안전장치 유지. 원격 actor가 해결되기 전 release/rejoin 완료로 위장하지 않음. 일반 로그인 대기 오류와 분리 |
| DB/control 조회·commit 장애 | 안전한 503, 원본 오류 미노출. 접수 성공으로 보고하지 않음 |
| WITHDRAWN/MERGED | 기존 전용 계정 상태 오류 우선, 전체 로그아웃 오류로 덮어쓰지 않음 |

신규 오류는 [AuthErrorStatus](../../src/main/java/web/tosunsaeng/identity/domain/auth/common/exception/AuthErrorStatus.java)에 구현했다. DB/control Transaction 실패는 `SESSION_SECURITY_UNAVAILABLE`(503)이다. 이전안의 로그인용 `LOGOUT_ALL_IN_PROGRESS`·`LOGOUT_ALL_RECOVERY_REQUIRED`는 도입하지 않는다. logout 200 이후 원격 상황을 조회하는 새 공개 status API는 MVP에서 만들지 않는다. 일반 만료/폐기 오류로 지연 revoke 원인을 식별할 수 없다면 원인을 단정하지 않고 재로그인을 안내한다. 실제 Firebase/DB 장애에는 기존 안전한 오류와 제한된 재시도를 적용하며 무한 재인증·무한 refresh는 금지한다.

네트워크 응답 유실 시 앱이 보관 중인 같은 유효 Access Token으로 bounded 재시도한다. 이 용도로 Token을 로그/분석 도구에 저장하지 않는다. 로컬 Token을 이미 삭제한 경우 성공을 단정하지 않고 현재 기기 로그아웃만 완료됐다고 안내한다. 재발급한 새 토큰으로 자동 재시도하면 새로운 요청이 되므로 금지한다.

다른 기기의 로컬 정보·화면은 서버가 원격 삭제하지 않는다. 인증 실패 시 해당 앱이 signOut·토큰 정리를 수행한다. Google/Apple/Kakao 자체 세션은 남을 수 있으며 항상 비밀번호 재입력을 강제한다고 설명하지 않는다.

### 5.9 보존·운영·feature flag

| 대상 | 제안 보존 |
| --- | --- |
| UserSessionControl | ACTIVE User 동안 유지. unresolved·live Session 존재 시 TTL 없음 |
| PENDING/CLAIMED/REVOKING/VERIFYING/RETRY_WAIT/RECONCILIATION_REQUIRED | cleanupAt 없음 |
| COMPLETED/SUPERSEDED operation 및 dedup receipt | `max(terminalAt + P7D, 해당 receipt의 requestExpiresAt + 실제 verifier skew)` 이후 TTL |
| reconciliation 해결 | 실제 해결 시각을 terminalAt으로 사용. 원 요청 시각으로 TTL 계산 금지 |

운영 해결은 읽기 조사→dispatch actor/원격 결과 확인→exact operation CAS로 재개/완료/탈퇴 인계 순서다. control 문서를 직접 삭제하거나 완료된 logout을 replay하여 새 세션을 끊지 않는다. 수동 mutation도 승인·감사·동일 target/slot 보호를 통과해야 한다. 조사 중 로그인 허용은 원격 작업 포기·release 허용과 다르다. 이번에 공개 관리자 API는 만들지 않는다.

신규 flags 제안(기본 false): `AUTH_SESSION_FENCE_ENABLED`, `FIREBASE_LOGOUT_ALL_CAPTURE_ENABLED`, `FIREBASE_LOGOUT_ALL_WORKER_ENABLED`.

- fence 전용 버전으로 전체 Session writer를 먼저 배포한다. capture ON은 fence가 모든 instance에서 켜져 있고 이전 writer가 없을 때만 허용한다.
- capture 활성화 시 Firebase target·Transaction/인덱스·worker 실행 준비 검증 실패를 조용히 LOCAL 성공으로 바꾸지 않는다. 구성 불일치는 startup/activation을 차단한다.
- worker OFF는 보안 fence OFF가 아니다. 기존 watermark·epoch·인증 증빙·actor 보호는 항상 존중하며 pending만으로 로그인 차단하지 않는다. fence를 무시하는 구버전으로 rollback 금지.
- 메트릭: pending age/count 및 queue depth, completed, retry, unknown result, reconciliation, old-auth rejected, selective session invalidation, CAS conflict, physical session marking backlog. UID/userId/operationId를 metric label에 넣지 않는다.
- pending 60초 초과 경보, reconciliation 즉시 경보를 초기 제안으로 두고 staging 처리시간에 맞춰 조정한다.

### 5.10 로컬 구현 결과 — 설계 제안과의 정확한 매핑

- `SessionSecurityService` + `LogoutAllCoordinator`가 위 Transaction/control 역할을 담당한다. `MongoTemplate`과 `@Version`을 사용하며 별도 custom repository나 receipt collection은 만들지 않는다. 중복 요청 증거는 `LogoutAllOperation.requestFingerprint/requestExpiresAt`에 함께 보관한다. 동시 Mongo 충돌은 안전한 503으로 반환할 수 있으며 같은 유효 jti의 bounded 요청 재시도로 기존 접수를 확인한다. 자동으로 새 jti를 만들지 않는다.
- control의 실제 CAS는 `@Version`이며 `revision`을 함께 증가시킨다. `updatedAt` 대신 보안 epoch·경계와 markingRequired를 유지한다. operation의 실제 명칭은 `bindingId`, `dispatchAt`, `dispatchAcknowledged`, `observedValidAfter`다. 별도 completionAuthBoundary/boundaryAppliedAt은 없고 control과 operation을 같은 Transaction에서 저장한다.
- 공통 발급기 및 LOCAL·Guest·Firebase 가입/로그인·Guest 승격/병합·refresh를 연결했다. merge는 source/target control을 정렬한 순서로 갱신한다. auth-method sync·Guest prepare·탈퇴 credential 소비도 인증 경계를 검사한다. 기존 refresh 재사용 탐지의 폐기 결과는 오류 응답 시에도 commit하도록 보존했다.
- `FirebaseSessionRevocationPort.inspect(Target)` 및 `revoke(Target, dispatchAt)`만 제공한다. Admin SDK 9.4.3의 기본 503 재시도(최대 4회)를 피하려고 `FirebaseSessionRevocationHttpAdapter`에서 고정 Identity Toolkit HTTPS endpoint와 ADC를 사용한다. mutation body는 `localId`, 영속화한 dispatchAt의 초 단위 `validSince`뿐이다. redirect·HTTP 자동 retry·HTTP/curl 로그는 OFF다. 기존 Firebase 검증 SDK 경로는 유지한다.
- adapter는 project/tenant를 설정과 정확히 비교하고 lookup 응답은 64 KiB로 제한한다. connect/read/write timeout은 기존 Firebase 설정을 사용한다. read 429/5xx/IO만 bounded retry하며 mutation의 불명확한 모든 오류는 결과 불명이다. Retry-After는 현재 adapter에서 소비하지 않으며 1초 이상 jitter, base 5초·cap 300초·최대 8회 read/preflight 시도를 적용한다.
- 물리적 폐기 마킹은 접수 시 최대 1,000개, worker sweep은 최대 control 20개 × session 100개다. 보안상 거절은 즉시 epoch/인증 경계로 판단한다. 처리 중/격리 operation에는 TTL이 없고 terminalAt 기준 기본 7일 및 원 요청 만료+skew 중 늦은 시각에 TTL을 설정한다. control 자동 삭제는 구현하지 않았다.
- 탈퇴 Transaction은 미dispatch 작업만 `SUPERSEDED_BY_WITHDRAWAL`로 바꾼다. started/unknown 작업은 동일 user의 durable dependency로 유지한다. 별도 withdrawalId 필드는 없으며 WITHDRAWN User와 기존 withdrawal lifecycle의 단일 소유 관계를 이용한다. 기존 외부 cleanup은 `LOGOUT_REVOKE_PENDING`으로 bounded 대기, identity release는 `DEPENDENCY_PENDING`으로 대기한다. 미해결 actor를 삭제하고 CLEANED로 진행하지 않는다.
- 연계 검토 중 기존 `UserWithdrawalIdentityReleaseTransactionService`의 `final`이 클래스 기반 Transaction proxy를 막는 문제를 발견하여 제거하고 프록시 생성 회귀 테스트를 추가했다. 탈퇴 기능을 새로 구현한 것이 아니다.
- 최소 counter `identity.session.revocation{outcome=completed,retry,reconciliation,unknown}`는 Transaction commit 뒤 집계한다. pending age/queue depth/marking backlog/격리 경보는 운영 조회·대시보드 설정이 남아 있으며 모두 구현됐다고 간주하지 않는다.
- 아래 26개 매트릭스는 로컬·실제 Mongo·staging을 합친 완료 조건이다. in-memory Mongo mapping/CAS 및 mock Transaction 테스트는 실제 replica-set rollback·process crash·E2E를 입증하지 않는다. 미확인 항목은 [통합 운영 runbook](firebase-logout-all-revoke-stage-8-runbook.md)에 남긴다.

## 6. 부록 — 전체 근거·검증·작업 순서

### 6.1 설계 당시 구현 근거 — 실제 변경 결과는 5.10절 참조

| 파일 | 확인한 사실 / 예상 변경 |
| --- | --- |
| [AuthController](../../src/main/java/web/tosunsaeng/identity/domain/auth/common/api/AuthController.java) | 무본문 logout-all, BaseResponse.success(null). 전용 오류/OpenAPI 의미 보강 |
| [LogoutAllService](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/LogoutAllService.java) | 내부 saveAll, 0건 조기 반환, Transaction 없음. coordinator+Transaction 경계로 변경 |
| [RefreshSessionIssuer](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/RefreshSessionIssuer.java) | prepare/savePrepared/issueRotated. expected epoch 및 공통 저장 fencing 필요 |
| [TokenReissueService](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/TokenReissueService.java) | User 상태 검사·회전·재사용 탐지. 현재 Firebase 상태 조회 없음. epoch·인증 증빙 전파·확인된 폐기 경계와 최소 회전 Transaction 적용 |
| [RefreshSession](../../src/main/java/web/tosunsaeng/identity/domain/auth/domain/entity/RefreshSession.java) | @Version·expiresAt TTL·revocationReason. epoch 0 호환 및 서버 검증된 인증 출처·binding/authTime 추가 |
| [FirebaseIdentity](../../src/main/java/web/tosunsaeng/identity/domain/auth/domain/entity/FirebaseIdentity.java) | project/UID 및 userId unique, binding ID·createdAt 존재. tenant는 현재 설정과 함께 exact snapshot |
| [FirebaseAdminAuthenticationVerifier](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/infrastructure/firebase/FirebaseAdminAuthenticationVerifier.java) | 모든 목적 checkRevoked=true, auth_time 검증 존재. 새 control 경계 연결 |
| [VerifiedFirebasePrincipal](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/VerifiedFirebasePrincipal.java) | authTime 보유. 원문 proof 저장 불필요 |
| [FirebaseSdkAdminClient](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/infrastructure/firebase/FirebaseSdkAdminClient.java) | verifyIdToken 후 getUser, tenant client 선택. 현재 DTO에 validAfter는 없음 |
| [FirebaseGuestMergeTransactionService](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseGuestMergeTransactionService.java) | source 폐기+target Session 저장+event. 양쪽 control 충돌 경계 필요 |
| [UserWithdrawalTransactionService](../../src/main/java/web/tosunsaeng/identity/domain/user/application/UserWithdrawalTransactionService.java) | credential·User 상태·Session·outbox 원자 처리. epoch/active logout handoff 검증 추가 |
| [UserWithdrawalExternalCleanupWorker](../../src/main/java/web/tosunsaeng/identity/domain/user/application/UserWithdrawalExternalCleanupWorker.java) | disable·revoke·delete 및 retry. 로그인 가능한 계정의 새 revoke worker에 그대로 복사 금지 |
| [Stage 7](billing-entitlement-owner-fanout-stage-7-plan.md) | 마지막 절의 production 완료 gate 유지. 저장소 병합과 운영 완료를 혼동하지 않음 |

신규 예상 파일군: session application의 LogoutAllTransactionService·UserSessionControlService·FirebaseSessionRevocationWorker·RetryPolicy/Port, domain의 UserSessionControl·LogoutAllOperation·receipt 및 상태/오류 enum, custom Repository CAS·인덱스, Firebase SDK 전용 adapter, properties/scheduler, 테스트·계약·runbook. 정확한 파일 수보다 공통 보안 경계의 누락 여부를 검토한다.

### 6.2 테스트 매트릭스

| ID | 검증 |
| --- | --- |
| T01 | Guest/LOCAL 내부 처리, 다중 SNS의 동일 Firebase UID 단일 revoke |
| T02 | 내부 세션 0건+Firebase 연결에서도 작업 생성; dangling FEDERATED 연결 격리 |
| T03 | 세션 폐기·epoch·operation/receipt Transaction rollback, commit 불명 후 재조회 |
| T04 | 같은 jti 동시 요청/응답 유실/완료 후 재요청 NOOP; pending 중 새 로그인 jti는 새 epoch/cycle·독립 receipt. 최신 세션 폐기와 원격 queue 직렬화 |
| T05 | login/exchange와 logout barrier 경합: 선행 epoch 인증이 새 epoch Session으로 저장되지 않음 |
| T06 | refresh 회전과 logout 경합·legacy epoch 0·다중 세션·1,000건 초과 fallback. 모든 credential 소비 지점 차단 |
| T07 | upgrade/merge/signup/binding 생성 경합. source/target 혼동·deadlock 방지, 기존 event Transaction 보존 |
| T08 | stale lease actor, 시작 전 crash 재claim, mutation 시작 후 pause/crash 자동 재전송 금지 |
| T09 | 외부 성공+DB 실패, timeout/5xx 결과 불명, validAfter 확인만으로 in-flight 종료 오판 금지 |
| T10 | 안전한 pre-dispatch/read 실패 backoff·상한·소진, permission/target 오류 즉시 격리 |
| T11 | PENDING/REVOKING/VERIFYING/RETRY_WAIT/결과 불명 중 정상 새 인증 허용; 이전 auth_time은 새 iat여도 거절. 동일 초 경계·watermark 단조성 |
| T12 | 완료 후 정상 재로그인 및 다음 logout; 과거 완료 worker의 지연 실행이 원격 호출하지 못함 |
| T13 | WITHDRAWN/MERGED 우선 오류, 미해결 actor와 Stage 2/3 release/rejoin 경합 |
| T14 | 실제 User DB 유형 유지, 기존 JWT/Billing scope·audience/workload·단일 logout 무변경 |
| T15 | 200 envelope·신규 오류·Token 비노출·원본 Firebase 예외 비노출·모바일 재시도 계약 |
| T16 | terminal TTL·receipt 인증 유효기간·unresolved 무기한 유지·control 삭제로 epoch 부활 금지 |
| T17 | flags 기본 false·invalid 조합 fail-fast·구버전 writer 혼재 activation 차단·worker OFF에서 epoch/watermark 보호 및 새 로그인 허용 |
| T18 | process restart/lease expiration 후 CAS 복구; real replica set rollback·write conflict·인덱스 검증 |
| T19 | staging SDK 9.4.3 retry/timeout·tokensValidAfter/auth_time 정밀도·같은 UID 다중 기기 및 LOCAL/Firebase 혼합 로그인 |
| T20 | staging 모바일 200 접수/응답 유실/선택적 401/실제 장애 503 안내·수동 reconciliation 중 로그인 허용·실제 경보·중단/재개 |
| T21 | 지연 revoke 경계 이전 Firebase 기반 세션만 거절; 독립 LOCAL 및 경계 이후 Firebase 세션 유지. user epoch 추가 증가 없음 |
| T22 | 인증 출처·exact binding/authTime의 다중 refresh 회전 전파, 누락 신규 증빙 저장 거절, legacy 누락을 LOCAL로 오인하지 않음 |
| T23 | 조회/DB 완료 지연·중복 완료·같은 validAfter 재확인이 영향 없는 새 인증을 폐기하지 않음. 실제 확인 경계만 단조 갱신 |
| T24 | 원격 결과 불명 중 validAfter 증거 반영과 actor 종료 판정 분리. 후속 원격 queue 및 release는 대기하되 새 인증 허용 |
| T25 | 이전 operation 완료가 새 epoch/더 최신 요청 watermark/다른 slot을 덮어쓰지 않음. 서로 다른 실제 logout 요청과 단순 재전송 구분 |
| T26 | selective boundary 저장과 refresh/credential 소비 barrier 경합, 1,000건 초과 논리 차단·물리 batch 및 rotation family 우회 방지 |

기본 단위 테스트는 외부 Provider와 Repository Mock, 실제 Atlas·OAuth 호출 금지다. 별도 격리 replica-set 또는 staging에서 Transaction/경합을 검증하며 Mock 성공만으로 Mongo 원자성을 입증하지 않는다. 필요 테스트 인프라 변경은 구현 전에 범위와 실행 조건을 확인한다.

### 6.3 구현 순서와 배포 gate

1. 승인된 D1~D6과 TMI-129를 기준으로 기능 OFF 개발을 진행한다. Stage 7 production 완료는 더 이상 개발 착수 조건이 아니며 운영 활성화 전 통합 검증 항목을 추적한다.
2. SessionControl/epoch·인증 증빙·선택적 폐기 경계·operation/receipt/직렬 queue·CAS·인덱스·단위 테스트 구현.
3. 전체 Session writer와 credential 소비 지점의 최소 Transaction/fencing 전환, legacy 호환 검증.
4. logout coordinator·원자 접수·중복 요청·0건 처리 및 API/모바일 오류 계약 구현.
5. read-only inspect/revoke 전용 adapter·dispatch 추적·worker·결과 불명 격리·지표 구현.
6. 기존 withdrawal/release guard와 신규 actor handoff 연결·선택적 무효화·새 인증 허용·다중 logout 경합 검증.
7. 전체 `./gradlew clean test`, `git diff --check`, 예상 밖 diff 및 민감정보 검토. 설정 기본 OFF 유지.
8. 사용자 commit/PR/merge 이후 foundation OFF 선배포, 실제 replica-set·SDK·모바일 staging E2E 수행.
9. 모든 old writer 종료→fence ON 확인→worker 준비→capture ON canary. 권한·실패 경보와 gate 정상 작동 확인 후 확대.
10. 완료 인계는 코드·병합·배포·운영 활성화를 구분한다. 선행 gate 미충족 또는 unresolved dispatch 해결 절차 부재이면 production NO-GO.

### 6.4 최초 문서 작성 검증 — 구현 전 이력

- 2026-09-08 승인 정책을 전체 요약·상태/발급·worker·시각 경계·모바일·테스트에 반영했다. 이전의 pending 전체 로그인 차단, 완료 시각 기반 경계, 다른 jti를 pending receipt로 흡수하는 안은 대체했다.
- 소스·기존 계약 정적 조사 및 문서 링크·`git diff --check` 검증만 수행한다. 코드 변경이 없어 Gradle 테스트는 실행하지 않는다.
- Firebase SDK 원격 종료/자동 retry, Mongo 경합·모바일 동작·운영 설정은 아직 미검증이다.
- 계획서 작성 시 기존 미커밋 문서와 JWT 작업을 보존하고 애플리케이션·feature flag·외부 환경은 변경하지 않았다. 이후 별도 사용자 승인으로 Jira TMI-129 생성 및 제목·본문·해야 할 일 상태 재조회를 완료했다. 댓글·상태 전환은 수행하지 않았다.

### 6.5 구현 검증 결과 (2026-09-08)

- `./gradlew clean test --console=plain` 성공: 126개 suite, 695개 테스트, 실패·오류·건너뜀 0개. 집중 SessionRevocationTests도 통과했다.
- `git diff --check` 통과. 기존 사용자 미커밋 문서 변경 보존. commit·push·배포·Jira 댓글/상태 변경 없음.
- 로컬 검증에는 신규 session/worker·HTTP fake transport·설정/Transaction proxy 테스트와 기존 전체 회귀가 포함된다. T03/T18의 실제 Mongo rollback·write conflict 및 T19/T20의 Firebase/mobile/staging 검증은 아직 하지 않았다. 나머지 운영 시나리오도 runbook과 매트릭스 기준으로 별도 증빙한다.
- 기능은 기본 OFF이며 Stage 7·8 운영 검증을 통합 수행한 뒤 활성화한다. 코드 테스트 성공을 운영 준비 완료로 해석하지 않는다.

### 6.6 Jira 완료 보고 댓글 초안 — 미등록

> TMI-129 로컬 구현 완료: logout-all 원자 접수, sessionEpoch·인증 증빙, 새 로그인 허용 및 선택적 세션 무효화, 단일 Firebase revoke·결과 불명 격리, 탈퇴 cleanup/release 연계를 구현했습니다.
>
> 주요 변경: SessionSecurityService, LogoutAllCoordinator, FirebaseSessionRevocationWorker/HttpAdapter, UserSessionControl/LogoutAllOperation, 공통 발급·refresh와 탈퇴 연계, 설정·테스트·계획서/runbook·프론트 계약입니다. 기존 release Transaction proxy 문제도 보완했습니다.
>
> 검증: 전체 clean test 126개 suite·695개 테스트 통과, 실패·오류·건너뜀 0개, git diff --check 통과.
>
> 남은 위험: 기능 기본 OFF이며 실제 Mongo rollback·Firebase/mobile·운영 권한/경보/격리 복구 및 Stage 7·8 통합 검증은 미완료입니다. 코드 구현을 운영 활성화 완료로 간주하지 않습니다.
