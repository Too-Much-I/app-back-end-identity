# Billing 구매 권한·탈퇴 전달·누락 복구 인계 검토

작성일: 2026-10-07. develop 로컬 코드 정적 검토. 사용자 첨부 인계서 검토이며 구현/배포 승인이나 외부 계약 확정이 아니다. 첨부의 ADR-004/PLAN-009/ADR-002 원문과 Billing 구현·AWS 환경은 이번 검토에서 확인하지 않았다.

## 1. 5줄 결론

1. 책임 경계와 단계별 활성화 방향은 타당하나, 설정만으로 적용할 수 없고 Identity 추가 구현이 필요하다.
2. purchase는 현재 기본 발급되지 않지만 명시/default scope로 주입되면 Guest도 차단하지 않는다. ACTIVE MEMBER 전용 정책이 필요하다([issuer](../../src/main/java/web/tosunsaeng/identity/global/security/jwt/JwtAccessTokenIssuer.java)).
3. LC 탈퇴 publisher를 그대로 복제·재사용하면 2xx 전체 성공·인증 오류 dead-letter·Retry-After 미지원으로 Billing 제안과 다르다([publisher](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalevent/application/UserWithdrawnPublisher.java)).
4. 원 event는 성공 후 기본30일 정리되며 기존 backfill은 별도 ID를 합성한다. 원천 보존·snapshot/feed 계약부터 확정해야 한다([backfill](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalevent/application/UserWithdrawnBackfillService.java)).
5. 실제 최장 token 수명·탈퇴/발급 경합·과거 탈퇴 coverage가 미확정이므로 구매 활성화 및 Billing purge 안전성을 승인할 수 없다([TTL 설정](../../src/main/java/web/tosunsaeng/identity/global/security/jwt/JwtProperties.java)).

## 2. 사용자가 반드시 읽어야 하는 내용

### P1 — 구매 scope를 기본 목록에만 추가하면 안 됨

확인 사실: issuer는 userId/accountType/scopes를 받으며 DB 상태를 조회하지 않는다. null/empty scope는 설정 기본값, 명시 scope는 그대로 선택하고 billing:read를 추가한다. 따라서 billing:purchase를 설정이나 내부 호출 인자로 넣으면 Guest도 해당 scope를 얻을 수 있다. 이는 공개 API의 임의 scope 입력 취약점을 확인했다는 뜻은 아니다. 현재 정상 호출의 purchase 발급도 확인되지 않았다.

권고: purchase 예약 권한은 임의 scope 선택과 분리하고 신뢰할 수 있는 ACTIVE MEMBER 상태/발급 경계에서만 허용한다. Guest의 default/explicit scope 합성도 거절 또는 제거하는 정책을 계약에 명시한다. 기존 Learning Core/Billing audience, read, account_type, workload 분리 유지.

### P1 — 탈퇴 이후 발급 상한을 현재 코드로 확정할 수 없음

확인 사실: 세션 fence에는 ACTIVE 재검사와 control 쓰기 경합 방어가 있다. 그러나 signup/upgrade/merge는 관련 transaction이 반환한 뒤 별도 access issuer를 호출한다. issuer 자체에는 상태 재검사나 발급 deadline이 없다. 따라서 모든 경로의 ‘탈퇴 후 발급 불가’를 보장했다고 답하면 안 된다. 프로세스 정지/지연을 포함한 최대 지연 시간도 이번 검토에서 증명되지 않았다.

권고: 서명 생성, 세션 transaction commit, HTTP 응답 반환을 구분해 발급의 기준 시점을 정의한다. purchase용 발급도 공통 상태/fence 경계로 모으고, rollback된 결과가 외부로 반환되지 않는지 검증한다. 단순 DB 재조회는 조회 직후 탈퇴 경합을 없애지 못한다. 회전 응답 복구는 새 발급뿐 아니라 기존 암호화 응답 replay 경로도 검증한다.

### P1 — 기존 outbox를 Billing 복구 원천으로만 쓰면 누락 위험

확인 사실: 탈퇴 transaction은 User tombstone/세션 폐기/원 outbox를 함께 저장한다. publisher OFF 여부와 무관하게 outbox 저장은 수행된다. 하지만 LC PUBLISHED의 cleanupAt 기본값은 성공 처리+30일이며 TTL 및 scheduler 삭제 대상이다. Billing만 OFF였던 기간의 사실은 LC 완료/정리와 독립적으로 보존되어야 한다.

기존 backfill은 WITHDRAWN User를 withdrawnAt/_id로 정렬해 최대100건 선택하고, outbox가 없으면 userId+withdrawnAt 기반 name UUID를 생성한다. 이 ID는 원래 random eventId 복원이 아니다. 같은 시간 범위 재실행은 계속 첫 page를 선택하고 nextCursor가 없어 전체 bounded snapshot 완료 프로토콜도 아니다. 기존 LC 도구를 잘못된 구현이라고 단정하는 것이 아니라 새 Billing 계약에 그대로 사용할 수 없다는 뜻이다.

권고: capture와 publisher ON/OFF를 분리하고, 승인된 durable 사실 원천 및 destination delivery를 둔다. 원 event가 없고 tombstone만 있는 과거분은 별도 snapshot 사실 DTO/식별 계약을 먼저 합의한다. 삭제된 데이터까지 복구 가능하다고 주장하지 않는다.

### P2 — 응답·인증·재시도 계약 차이

Billing은 204만 성공이어야 하며 현재 LC publisher의 2xx 전체 성공 조건을 그대로 사용하면 안 된다. 401/403의 BLOCKED_AUTH 상태·담당 확인·동일 event 재개가 추가로 필요하다. 404/405, 409, 422 및 기타 영구 오류의 복구 운영 절차도 정의한다. 기존 LC 정책은 이번 요구로 일괄 변경하지 않는다.

BillingSigV4JsonTransport는 이미 있어 재사용 후보이나 현재 Retry-After는 정수1~300초만 지원한다. 인계서의 ‘Retry-After 존중’에 HTTP-date 또는 긴 대기값을 포함할지 합의해야 한다. 서명 transport 존재는 AWS 권한/격리 검증 완료의 증거가 아니다.

### P2 — lowercase sub 및 최대 수명은 기존 보장으로 단정 금지

User 신규 생성은 lowercase random UUID이고 탈퇴 tombstone은 동일 ID를 유지한다. 하지만 JWT issuer 검증은 equalsIgnoreCase여서 uppercase UUID도 통과하며 subject를 그대로 쓴다. lowercase 계약을 엄격 적용하려면 기존 저장 데이터 조사와 issuer 검증/테스트 보완 필요. 단순 소문자 치환으로 기존 소유 ID를 임의 변경하지 않는다.

JwtProperties는 양수 TTL만 검사하며 상한이 없다. 기본 PT30M은 운영 최대/과거 최장 토큰 증거가 아니다. 환경별 현행·과거 설정과 실제 발급 경계를 확인해야 한다. 삭제 안전성에는 ‘최종 발급 가능 시점+최대 TTL+Billing skew+Billing 처리 중 요청 deadline’을 포함한다. 15일 경과만으로 purge 허용하지 않는다.

## 3. 사용자가 결정해야 하는 사항

- 인계서가 기존 승인으로 설명한 15일/운영 주기는 이번 검토에서 재선택하거나 구현하지 않는다.
- 새 사실 원천의 개인정보 최소 필드·보존기간, 기존 tombstone 보존 정책, 과거 coverage 공백에 따른 제한은 별도 승인 대상이다. 현재 코드에 tombstone이 남는다는 사실은 무기한 보존 승인 근거가 아니다.
- 구현 전에 두 서비스가 route/DTO, ACK204, BLOCKED_AUTH·dead-letter 재개, Retry-After 범위, snapshot/feed 완료·gap 규칙을 동결해야 한다.
- 판매·자동 purge 및 실제 IAM/host 변경은 별도 승인/검증 대상이다.

## 4. 주요 위험과 미확인 사항

- Billing 코드/ADR 및 AWS Lattice·ALB 차단/IAM 정책, 원격 flag/TTL/기존 데이터 상태는 미확인.
- 소스 생성 경로에서는 신규 UUID를 사용하나 관리자 수동 복구/DB 수정까지 포함한 절대 비재사용 보장은 운영 절차 확인이 필요하다. Guest 승격은 같은 사람의 기존 ID 유지이며 탈퇴 사용자 ID 재사용과 다르다.
- snapshot watermark를 withdrawnAt 단독 또는 단순 시각+ID 커서만으로 정하면, watermark 전에 정한 시각을 가진 transaction이 나중에 commit할 때 누락될 수 있다. 커서 tie-breaker 외에 commit 경계/스냅샷 일관성/증분 원천 연결을 명시해야 한다.
- at-least-once delivery는 timeout 뒤 재전송이 정상이다. consumer commit 이후만 ACK하고 checkpoint를 전진시키는 계약은 타당하다.
- PENDING/DEAD_LETTER 무기한 방치도 보존 정책 문제가 된다. 리뷰 시각은 자동 삭제 시각과 구분한다.

## 5. 현재 작업과 직접 관련된 회신안

‘Identity 관점에서 방향은 수용 가능하나 현재 구현 완료가 아니다. 기존 UserWithdrawn 4필드/4KiB 및 LC workload JWT는 유지한다. Billing 전용 delivery와 SigV4 adapter를 분리하고 204-only, BLOCKED_AUTH, Retry-After 및 독립 flag/보존을 추가해야 한다. purchase는 ACTIVE MEMBER 신뢰 상태와 공통 발급 경계로 제한하며 전체 발급/replay 경합을 검증한다. 원 event가 없는 과거 데이터는 기존 backfill을 Billing에 사용하지 않고 별도 snapshot 계약을 합의한다. TTL 실측·과거 설정/coverage 확인 전 purge 및 판매 활성화는 승인하지 않는다.’

권장 작업 순서: 계약/원천 보존 합의 → 공통 purchase 발급 정책·경합 설계 → destination capture/전달 구현(OFF) → snapshot/feed → fake 단위/replica-set 경합 검증 → staging Lattice/consumer 검증 → consumer/publisher/coverage 확인 → purchase 단계 활성. 실제 판매/purge는 별도 gate.

## 6. 부록 — 상세 근거 및 회신표

### A. 현재 구현과 요청 대비

| 항목 | 현재 코드 사실 | 추가 작업/회신 |
| --- | --- | --- |
| JWT | RS256/kid/iat/exp/jti/account_type, 기본 audience와 billing audience·read 추가 | ACTIVE MEMBER purchase 정책과 lowercase 보장 보완 |
| 원 탈퇴 | transaction 내 tombstone/세션/phone revoke/outbox 저장 | Billing capture도 동일 원자 경계에 포함 |
| wire | eventId/schemaVersion/userId/withdrawnAt, 최대4096bytes | 유지 가능, 원 시각/ID 유지 |
| 목적지 | LC 단일 endpoint/단일 outbox status | (eventId,destination) 독립 delivery/lease/flag/재개 |
| LC timeout | connect3초, request timeout5초 | readTimeout 이름과 달리 JDK request timeout임. Billing 별도 확정 |
| LC 실행 | lease60초, fixedDelay5초, batch20, maxAttempts12 | 독립 설정, 최대시도 도달 이후 보존/수동재개 정의 |
| LC backoff | base5초, 지수증가, 기준 cap15분 뒤 ±20% jitter(최대18분 가능) | Billing 제안5초~1시간 full jitter와 다름 |
| 보존 | PUBLISHED 성공+30일 cleanup; dead-letter review90일 | review는 삭제 아님. Billing 원천 별도 보존 합의 |
| LC ACK | 모든2xx 완료; 408/425/429/5xx retry; 나머지 dead-letter | Billing204-only/BLOCKED_AUTH/헤더 반환 계약 필요 |
| SigV4 | 기존 공통 transport·redirect NEVER·응답 헤더 일부 지원 | withdrawal adapter/route/IAM/staging 추가 |
| 복구 | bounded local backfill(최대100), name UUID 생성 | snapshot/feed/cursor/watermark/gap protocol 새로 필요 |
| tombstone | User에 userId/withdrawnAt 유지; 검토 범위에 User TTL/삭제 경로 없음 | 원 eventId는 User에 없음. 실제 존재·보존 승인 별도 |

### B. 발급 경로 목록

- [LoginService](../../src/main/java/web/tosunsaeng/identity/domain/auth/local/application/LoginService.java): ACTIVE 검사 → access 서명 → refresh 발급. fence 설정별 반환 안전성 검증 필요.
- [GuestAuthService](../../src/main/java/web/tosunsaeng/identity/domain/auth/registration/application/GuestAuthService.java): Guest purchase 금지 회귀 대상.
- [FirebaseExchangeService](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseExchangeService.java): ACTIVE MEMBER 검사 및 fence 경로 유지.
- [FirebaseSignupService](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseSignupService.java), [FirebaseGuestUpgradeService](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseGuestUpgradeService.java), [FirebaseGuestMergeService](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseGuestMergeService.java): transaction 이후 access 서명 경계 검토.
- [TokenReissueService](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/TokenReissueService.java): legacy/fence 회전 경로 검토.
- [ReissueRecoveryService](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/ReissueRecoveryService.java): transaction 회전 및 암호화 응답 replay 모두 확인 필요.
- [AccountRecoveryService](../../src/main/java/web/tosunsaeng/identity/domain/auth/accountrecovery/AccountRecoveryService.java): 현재 prepare/lookup은 계정 찾기이고 사용자 Access Token을 발급하지 않는다. 이를 신규 로그인 발급 경로라고 문서화하지 않는다.

### C. 추가 근거 및 테스트 상태

- [탈퇴 transaction](../../src/main/java/web/tosunsaeng/identity/domain/user/application/UserWithdrawalTransactionService.java), [outbox](../../src/main/java/web/tosunsaeng/identity/domain/user/domain/entity/UserWithdrawnOutbox.java), [wire](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalevent/application/UserWithdrawnWireEvent.java), [mapper](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalevent/application/UserWithdrawnEventMapper.java).
- [publisher 설정](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalevent/infrastructure/UserWithdrawnPublisherProperties.java), [retry](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalevent/application/UserWithdrawnRetryPolicy.java), [LC adapter](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalevent/infrastructure/JdkUserWithdrawnDeliveryAdapter.java).
- [SigV4 transport](../../src/main/java/web/tosunsaeng/identity/global/workload/BillingSigV4JsonTransport.java), [delivery result](../../src/main/java/web/tosunsaeng/identity/global/workload/WorkloadDeliveryResult.java), [User](../../src/main/java/web/tosunsaeng/identity/domain/user/domain/entity/User.java).
- 정적 확인 근거/관련 기존 테스트: JwtAccessTokenIssuerTests, UserWithdrawnPublisherTests(2xx 성공/영구 오류), UserWithdrawnBackfillServiceTests(dry-run/결정적 ID), BillingSigV4JsonTransportTests. 본 검토에서는 테스트를 실행하지 않았다.
- 추가 필수: default/explicit Guest purchase 차단, 비활성 사용자, 모든 발급/응답 replay의 탈퇴 경합, 실제 Mongo rollback/lease 경쟁, destination 독립 실패, 204 외2xx 거절, 원 event 소실/snapshot 완료·늦은 commit·동일 timestamp·cursor gap, SigV4 실패/직접 ALB 우회 차단. 외부 검증은 staging 승인 후 수행한다.
