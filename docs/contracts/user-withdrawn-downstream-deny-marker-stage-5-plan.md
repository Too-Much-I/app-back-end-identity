# 5단계 구현 계획: `UserWithdrawn` event와 downstream Access Token deny marker

- 작성일: 2026-08-27
- 발행자: Identity Service
- 소비자: Learning Core
- 계약 버전: `UserWithdrawn` schema version `1`
- 상태: 구현 계획
- 선행 작업: Stage 4, Jira `TMI-108`
- Learning Core 구현 Jira: `TMI-109`
- Identity producer Jira: 후속 생성 예정

## 1. 목적

회원 탈퇴 Transaction은 User를 `WITHDRAWN` tombstone으로 바꾸고 당시 활성 RefreshSession을 `ACCOUNT_WITHDRAWN`으로 폐기한다. Stage 4는 남아 있는 Refresh Token으로 재발급을 시도한 앱에 `401 ACCOUNT_WITHDRAWN`을 반환한다.

그러나 탈퇴 전에 이미 발급된 stateless Access Token은 자체 `exp`까지 Learning Core의 issuer·audience·signature·expiration 검증을 통과할 수 있다. 현재 기본 Access Token TTL은 30분이며, Learning Core는 매 요청마다 Identity를 조회하지 않는다.

Stage 5의 목적은 다음 두 결과를 만드는 것이다.

1. Identity 탈퇴 commit과 함께 `UserWithdrawn` event를 유실 없이 생성한다.
2. Learning Core가 event를 멱등 수신하고 old userId를 Access Token 잔여 수명 동안 로컬에서 거절한다.

```text
Identity withdrawal Transaction
→ User WITHDRAWN + RefreshSession ACCOUNT_WITHDRAWN + UserWithdrawnOutbox
→ at-least-once delivery
→ Learning Core event inbox + userId deny marker Transaction
→ JWT 검증 뒤 local deny gate
```

이 단계는 탈퇴 사실 전파 지연 안에서 수렴하는 비동기 차단이다. Identity 동기 introspection이나 token별 blacklist를 도입하지 않는다.

## 2. 포함 범위

### Identity

- `UserWithdrawnOutbox` domain·Repository·index
- 기존 withdrawal Mongo Transaction 안의 outbox 생성
- event mapper·wire contract v1
- atomic lease, retry·backoff, dead-letter, replay를 지원하는 publisher
- 기본 비활성 publisher configuration·scheduler
- 기존 `WITHDRAWN` User를 위한 dry-run 가능한 backfill/cutover 도구
- 민감정보 비노출 로그·낮은 cardinality metric

### Learning Core

- workload-authenticated internal event endpoint
- v1 payload validation과 semantic payload digest
- eventId unique inbox와 payload conflict 탐지
- userId unique deny marker와 TTL index
- inbox·marker 단일 Mongo Transaction
- 기존 사용자 JWT 인증 뒤 실행되는 local deny gate
- `401 ACCOUNT_WITHDRAWN` 외부 오류와 deny store 장애의 fail-closed 응답
- consumer·gate configuration, observability, contract·concurrency 테스트

### 공통

- at-least-once 전달과 duplicate 2xx 계약
- 유한 marker·inbox retention
- consumer 선배포, producer 후배포, backfill, staging E2E, activation gate
- 장애·dead-letter·manual replay·rollback 절차

## 3. 제외 범위

- Access Token 원문 또는 hash 기반 blacklist
- 매 Learning Core 요청의 Identity introspection
- Identity Access Token TTL 자체 변경
- Refresh Token rotation 원자성·응답 유실 복구
- 시험·결과·음성·채점 데이터 삭제 또는 익명화
- 탈퇴 User 데이터의 새 User 이전 또는 복구
- Firebase Admin cleanup 재실행
- 모바일 Firebase signOut·local cache 구현
- Billing 등 다른 JWT 소비 서비스의 consumer 구현
- AI callback을 사용자 Access Token 요청으로 재분류하는 변경

Learning Core 데이터 삭제·보존 정책은 별도 개인정보 lifecycle 작업이다. Stage 5 deny marker는 authorization 차단만 수행한다.

## 4. 현재 코드와 필요한 변경

### 4.1 Identity 현재 상태

- `UserWithdrawalTransactionService`는 User CAS, lifecycle, phone eligibility revocation outbox와 모든 활성 RefreshSession 폐기를 `mongoTransactionManager` Transaction에서 처리한다.
- `UserMergedOutbox`와 publisher는 eventId, schema version, atomic lease, retry, dead-letter, published retention, TTL cleanup 패턴을 이미 제공한다.
- `UserMerged` HTTP adapter는 workload Bearer credential port를 사용하지만 production credential provider와 최종 Learning Core workload 인증 profile은 아직 완료되지 않았다.
- `UserWithdrawalService`의 이미 `WITHDRAWN`인 멱등 응답 경로는 새 Transaction을 실행하지 않으므로 과거 탈퇴 User는 별도 backfill이 필요하다.

### 4.2 Learning Core 현재 상태

- `NimbusJwtDecoder`가 RS256, issuer, audience, subject, timestamp를 로컬 검증한다.
- JWT 검증 후 userId는 `JwtCurrentUserProvider`가 `sub`에서 읽는다.
- 현재 deny marker Repository·보안 filter·전용 오류가 없다.
- Mongo Repository scan은 현재 exams repository package에 한정되어 있어 internal event repository package를 명시적으로 추가해야 한다.
- event inbox와 marker를 함께 commit할 Mongo Transaction manager도 현재 없다.

따라서 기존 JWT decoder를 교체하지 않고, 정상 JWT 인증이 확정된 다음 userId marker를 검사하는 별도 gate를 추가한다.

## 5. 핵심 결정

### 5.1 event는 withdrawal commit과 원자적으로 생성한다

`UserWithdrawnOutbox` 저장은 선택적인 후처리가 아니다. User tombstone·RefreshSession 폐기와 같은 Mongo Transaction에 참여한다.

```text
User CAS 성공
→ withdrawal lifecycle/eligibility 처리
→ RefreshSession ACCOUNT_WITHDRAWN
→ UserWithdrawnOutbox 저장
→ COMMIT
```

outbox 저장이 실패하면 withdrawal 전체가 rollback되어야 한다. 탈퇴 성공 뒤 event가 없는 상태를 정상 상태로 허용하지 않는다.

같은 User는 한 번만 `WITHDRAWN`이 될 수 있으므로 outbox의 `userId`에 unique index를 둔다. 동시 탈퇴 요청은 기존 User CAS와 unique constraint의 승자 하나만 event를 만든다.

### 5.2 전달은 at-least-once다

Identity publisher는 응답 유실과 lease 만료 때문에 같은 event를 여러 번 보낼 수 있다. 정확히 한 번 전달을 가정하지 않는다.

- 신규 event 처리 성공: `204 No Content`
- 같은 eventId와 같은 payload 재수신: `204 No Content`
- 같은 eventId와 다른 payload: `409 Conflict` 또는 `422 Unprocessable Entity`, 보안 경보와 producer dead-letter
- 일시 장애: `425`, `429`, `5xx`
- workload 인증 실패: `401` 또는 `403`, 자동 재시도하지 않고 운영 격리

duplicate에 `409`를 반환하면 성공 응답 유실을 복구할 수 없으므로 동일 payload duplicate는 반드시 2xx다.

### 5.3 Learning Core local Transaction으로 inbox와 marker를 확정한다

v1 consumer는 작은 두 document만 저장하므로 HTTP 요청 안의 direct Transaction을 사용한다.

```text
workload credential 검증
→ payload validation·digest
→ 기존 inbox/marker 확인
→ deny marker upsert
→ inbox PROCESSED 저장
→ COMMIT
→ 204
```

marker 저장과 inbox 성공이 분리되면 event는 처리된 것으로 보이지만 차단이 누락될 수 있다. 두 저장은 반드시 같은 Learning Core Mongo Transaction에 참여한다.

### 5.4 deny gate는 유효한 JWT 뒤에 실행한다

권장 구현은 `OncePerRequestFilter` 기반 `WithdrawnUserAccessGateFilter`다.

- `BearerTokenAuthenticationFilter` 뒤, authorization·Controller 앞에 배치한다.
- 인증이 완료된 `JwtAuthenticationToken`에만 적용한다.
- 정상 issuer·audience·signature·expiration·UUID subject 검증을 통과하기 전에는 marker를 조회하지 않는다.
- JWT `sub`와 일치하는 active marker가 있으면 SecurityContext를 비우고 전용 401을 반환한다.
- public AI callback과 workload event endpoint에는 사용자 deny gate를 적용하지 않는다.

잘못된 임의 Token으로 userId 탈퇴 여부를 조회할 수 없도록 JWT 검증이 항상 먼저다.

### 5.5 외부 오류는 `401 ACCOUNT_WITHDRAWN`으로 통일한다

Learning Core의 사용자 요청 거절 응답은 Stage 4와 같은 의미를 사용한다.

```json
{
  "isSuccess": false,
  "code": "ACCOUNT_WITHDRAWN",
  "message": "탈퇴 처리된 계정입니다.",
  "result": null
}
```

marker Repository나 MongoDB 조회 장애는 사용자를 탈퇴로 오분류하지 않는다. 이 경우 보호 요청은 fail-closed `503 WITHDRAWAL_DENY_GATE_UNAVAILABLE`로 거절한다. 정상 처리를 허용하는 fail-open은 사용하지 않는다.

### 5.6 marker와 inbox는 유한 보존한다

deny marker는 탈퇴 User의 영구 목록이 아니다. 목적은 탈퇴 전에 발급된 Access Token의 잔여 유효기간을 덮는 것이다.

```text
blockedUntil
= withdrawnAt
+ maxAcceptedAccessTokenLifetime
+ allowedVerifierClockSkew
```

v1 기본 계약 후보:

- `maxAcceptedAccessTokenLifetime`: `PT30M`
- `allowedVerifierClockSkew`: Learning Core JWT timestamp validator와 동일한 명시적 값
- marker `expireAt`: `blockedUntil`
- inbox retention: Identity의 최대 자동 재시도·dead-letter·수동 replay 기간보다 긴 값

운영 활성화 전에 두 서비스 설정의 최대 Access Token TTL과 clock skew를 대조한다. 기본값이 같다는 가정만으로 활성화하지 않는다.

Mongo TTL monitor는 삭제가 지연될 수 있으므로 authorization은 document 존재만 보지 않고 `now < blockedUntil`을 직접 검사한다. `now >= blockedUntil`이면 marker가 아직 남아 있어도 요청을 허용한다.

event가 `blockedUntil` 뒤에 도착하면 inbox는 `PROCESSED`로 저장하지만 만료 marker는 새로 만들지 않는다.

## 6. Wire event v1

제안 internal endpoint:

```text
POST /internal/v1/events/withdrawn
```

요청 본문은 정확히 다음 의미를 가진다.

```json
{
  "eventId": "9a88bc80-d73a-4a3d-8f68-492641d27208",
  "schemaVersion": 1,
  "userId": "73a18ed4-1d56-4c4f-afd6-b39175b82a86",
  "withdrawnAt": "2026-08-27T02:00:00Z"
}
```

예시는 계약 형태를 위한 가짜 값이다.

| 필드 | 타입 | 필수 | 의미 |
| --- | --- | --- | --- |
| `eventId` | canonical UUID string | 필수 | 영구적인 event 멱등 key |
| `schemaVersion` | integer | 필수 | v1은 정확히 `1` |
| `userId` | canonical UUID string | 필수 | 탈퇴한 기존 User의 UUID |
| `withdrawnAt` | UTC ISO-8601 instant | 필수 | Identity withdrawal Transaction 시각 |

v1 불변식:

- UUID는 lowercase canonical 형식이다.
- `withdrawnAt`은 미래 허용 오차를 넘는 값이면 거절한다.
- email, phone, Firebase UID, Provider subject, Access/Refresh Token, credential은 포함하지 않는다.
- 전용 endpoint와 schemaVersion으로 event type을 식별하므로 v1 payload에 별도 `eventType`을 넣지 않는다.
- JSON field 순서에 의존하지 않는다.
- 알 수 없는 schema version을 추측 처리하지 않는다.

## 7. Learning Core inbox와 payload digest

권장 collection: `user_withdrawn_event_inbox`

```text
eventId          @Id, unique
schemaVersion
payloadDigest
userId
withdrawnAt
receivedAt
processedAt
status           PROCESSED
cleanupAt        TTL
```

raw JSON 전체가 아니라 v1 semantic field로 digest를 계산한다.

```text
normalizedUserId      = canonical lowercase UUID
normalizedWithdrawnAt = Instant.parse 후 Instant.toString()

digestInput = UTF-8(
    "tosunsaeng:user-withdrawn"
    + NUL + "1"
    + NUL + normalizedUserId
    + NUL + normalizedWithdrawnAt
)

payloadDigest = lowercase hex(SHA-256(digestInput))
```

eventId는 unique lookup key라 digest 입력에서 제외한다.

처리 규칙:

1. 기존 eventId가 없으면 marker와 inbox를 Transaction으로 저장한다.
2. 기존 eventId의 digest가 같으면 mutation 없이 204를 반환한다.
3. 기존 eventId의 digest가 다르면 marker를 변경하지 않고 conflict로 격리한다.
4. 동시 insert는 Mongo unique constraint를 최종 승자 결정 경계로 사용한다.
5. loser는 winner commit을 bounded 재조회해 같은 digest면 204, 아직 확정되지 않으면 425/503을 반환한다.

inbox `cleanupAt`은 source outbox의 published retention과 dead-letter 수동 replay 최장 기간보다 길어야 한다. Identity 기본 dead-letter review가 90일이므로 v1 기본 후보는 120일이며 운영 replay 정책과 함께 확정한다.

## 8. Learning Core deny marker

권장 collection: `withdrawn_user_access_denies`

```text
userId           @Id, unique
sourceEventId
withdrawnAt
blockedUntil
expireAt         TTL
createdAt
```

처리 불변식:

- 한 userId에는 marker 하나만 존재한다.
- 같은 userId의 다른 eventId가 들어오면 기존 withdrawnAt과 payload 관계를 확인하고 자동으로 더 최신 상태를 추정하지 않는다.
- `blockedUntil`은 consumer의 계약 설정으로 계산하며 producer가 임의 값을 보내지 않는다.
- marker에는 token, token hash, sessionId, Firebase UID가 없다.
- 재가입 User는 새 UUID이므로 old userId marker와 충돌하지 않는다.

요청 gate 판정:

```text
유효한 JWT 아님                  → 기존 401, marker 조회 안 함
유효한 JWT + marker 없음         → 기존 요청 진행
유효한 JWT + now < blockedUntil  → 401 ACCOUNT_WITHDRAWN
유효한 JWT + now >= blockedUntil → 기존 요청 진행
marker store 조회 실패           → 503, application 진입 차단
```

v1에서는 negative cache를 권위 데이터로 사용하지 않는다. consumer commit 직후 모든 instance가 동일한 결과를 보도록 공용 MongoDB의 `_id=userId` 조회를 사용한다. 성능 측정 뒤 cache를 추가한다면 event-driven invalidation과 짧은 TTL을 별도 설계한다.

## 9. Identity outbox 모델

권장 collection: `user_withdrawn_outbox`

```text
eventId              @Id
schemaVersion         1
userId                unique
withdrawnAt
status                PENDING | IN_FLIGHT | PUBLISHED | DEAD_LETTER
leaseOwner
leaseExpiresAt
attemptCount
nextAttemptAt
lastFailureCode
publishedAt
deadLetteredAt
retentionReviewAt
cleanupAt             TTL
```

index:

- unique `{ userId: 1 }`
- due `{ status: 1, nextAttemptAt: 1, withdrawnAt: 1 }`
- expired lease `{ status: 1, leaseExpiresAt: 1, withdrawnAt: 1 }`
- dead-letter review `{ status: 1, retentionReviewAt: 1 }`
- TTL `{ cleanupAt: 1 } expireAfter=0`

publisher v1은 기존 `UserMergedPublisher`의 검증된 정책을 같은 의미로 적용하되 entity·Repository·metric 이름은 분리한다.

- lease: atomic `findAndModify`
- redirect: 따르지 않음
- HTTPS만 허용
- 성공: 모든 2xx
- 재시도: 408, 425, 429, 5xx, timeout, connection failure
- 영구 격리: 그 밖의 3xx·4xx, invalid payload, 최대 시도 초과
- published retention 기본 후보: 30일
- dead-letter review 기본 후보: 90일
- scheduler·publisher 기본 비활성
- eventId·userId를 metric tag로 사용하지 않음

기존 UserMerged 클래스를 무리하게 generic framework로 먼저 추상화하지 않는다. 두 event에서 반복되는 안정된 정책이 확인된 뒤 공통화하고 Stage 5에서는 계약 명확성과 회귀 격리를 우선한다.

## 10. withdrawal Transaction 연결

`UserWithdrawalTransactionService`의 production constructor에는 `UserWithdrawnOutboxRepository`를 필수 의존성으로 추가한다. production에서 Repository가 없을 때 outbox 생성을 조용히 건너뛰는 nullable 경로를 두지 않는다.

Transaction 내부 순서 후보:

1. credential RefreshSession 재검증
2. User ACTIVE snapshot CAS → WITHDRAWN tombstone
3. Firebase target·withdrawal lifecycle 검증 및 생성
4. phone eligibility revocation revision·outbox
5. 활성 RefreshSession `ACCOUNT_WITHDRAWN` 폐기
6. `UserWithdrawnOutbox.create(userId, withdrawnAt)` 저장
7. commit

어느 단계든 실패하면 User·Session·lifecycle·eligibility·UserWithdrawnOutbox 전체가 rollback된다.

LOCAL, GUEST, Firebase MEMBER 탈퇴가 같은 outbox 계약을 사용한다. Firebase external cleanup 완료를 기다린 뒤 event를 만들지 않는다. Access Token 차단은 withdrawal commit 직후 시작해야 하므로 Stage 1 Transaction 시각의 `withdrawnAt`을 사용한다.

## 11. 기존 탈퇴 User backfill과 cutover

배포 전에 다음 read-only inventory를 수행한다.

- `status=WITHDRAWN` User 수
- `withdrawnAt`이 없는 비정상 tombstone 수
- UserWithdrawnOutbox가 없는 WITHDRAWN User 수
- 현재 시각 기준 `withdrawnAt + max TTL + skew`가 남은 User 수

backfill은 기본 비활성·dry-run 우선의 bounded batch로 구현한다. 매 실행마다 임의의 전체 WITHDRAWN User를 다시 찾지 않고, 배포 시 고정한 cutover 범위를 입력으로 사용한다.

```text
backfillLowerBound = captureFullyDeployedAt - maxAccessTokenLifetime - allowedClockSkew
backfillUpperBound = captureFullyDeployedAt
```

1. `backfillLowerBound <= withdrawnAt < backfillUpperBound`이고 outbox가 없는 WITHDRAWN User만 조회한다.
2. userId unique index를 최종 동시성 경계로 outbox를 삽입한다.
3. 같은 cutover 범위 재실행은 이미 있는 userId를 건너뛴다.
4. upper bound 이후 탈퇴는 live Transaction outbox만 담당한다.
5. lower bound 이전 탈퇴는 기존 Access Token이 모두 만료됐으므로 Stage 5 차단 backfill 대상이 아니다.
6. withdrawnAt 누락·미래값·중복 충돌은 추측 보정하지 않고 reconciliation report로 보낸다.

고정 upper bound가 없으면 published outbox TTL 정리 뒤 과거 WITHDRAWN User를 새 eventId로 반복 생성할 수 있으므로 금지한다. 운영 cutover 중 새 탈퇴 event 유실을 막기 위해 outbox capture 코드를 먼저 배포하고 publisher는 비활성으로 둔다. consumer 준비 뒤 최근 cutover backfill과 publisher를 순서대로 실행한다.

## 12. 서버 간 인증

사용자 Access JWT를 event endpoint 인증에 재사용하지 않는다.

```text
user Access JWT
aud = tosunsaeng-learning-core
sub = userId

≠

workload credential
principal = Identity Service
audience = internal UserWithdrawn consumer
```

Learning Core에는 internal endpoint 전용 우선순위 SecurityFilterChain을 두고 사용자 JWT chain과 분리한다. exact path, method, audience, Identity principal만 허용한다.

현재 저장소의 `WorkloadIdentityCredentialProvider`는 port이고 production 구현이 없다. 따라서 아래 항목이 `TBD`인 동안 staging·production publisher를 활성화하지 않는다.

| 항목 | 활성화 전 확정값 |
| --- | --- |
| ECS ingress | VPC Lattice/API Gateway/내부 ingress 중 실제 경로 |
| credential type | SigV4 또는 workload OIDC 등 승인 방식 |
| Identity task principal | 허용할 exact service principal |
| audience/resource | internal endpoint 전용 값 |
| clock skew·credential TTL | 검증기와 issuer가 공유하는 값 |
| key/role rotation | overlap·rollback 절차 |
| local/staging provider | 실제 credential을 저장소에 넣지 않는 공급 방식 |

AWS ECS 환경에서 VPC Lattice SigV4를 사용한다면 기존 Bearer adapter를 그대로 복사하지 않고 request signer port와 IAM auth policy를 구현한다. OIDC workload credential을 선택하면 issuer·JWKS·audience·principal을 allowlist로 고정한다.

## 13. Learning Core 구성 변경

필요한 구성 경계:

- internal workload SecurityFilterChain
- 기존 user JWT SecurityFilterChain + deny gate
- local/test legacy chain
- `MongoTransactionManager`
- internal event repository package scan
- `UserWithdrawnConsumerProperties`
  - enabled
  - maxAcceptedAccessTokenLifetime
  - allowedClockSkew
  - inboxRetention
  - maximumFutureEventSkew

filter chain 순서는 명시적 `@Order`로 고정하고 internal endpoint가 user JWT chain이나 public callback permit rule에 섞이지 않게 테스트한다.

consumer가 활성인데 Transaction manager, repository index, workload verifier 또는 필수 TTL 설정이 없으면 startup을 fail-fast한다.

## 14. 보안·개인정보 계약

- event와 저장 document에 userId는 필요 최소 식별자로 포함한다.
- userId, eventId, endpoint, workload principal을 일반 로그 message나 metric tag에 넣지 않는다.
- Access Token, Refresh Token, Firebase ID Token, token hash, Header, Cookie, raw request body를 로그·metric·Sentry에 넣지 않는다.
- payload conflict 로그는 schema version, outcome, failure code 같은 낮은 cardinality 값만 사용한다.
- internal endpoint 응답에 document id, digest, stack trace, DB 정보를 포함하지 않는다.
- 알 수 없는 Token이나 유효하지 않은 JWT에는 탈퇴 여부를 노출하지 않는다.
- marker 조회는 인증된 UUID subject에만 수행한다.

## 15. 관측 계약

Identity metric 후보:

```text
identity.user_withdrawn.publisher{schemaVersion,outcome,failureCode}
identity.user_withdrawn.outbox.backlog
identity.user_withdrawn.outbox.oldest_age
```

Learning Core metric 후보:

```text
learning_core.user_withdrawn.consumer{schemaVersion,outcome}
learning_core.user_withdrawn.deny_gate{outcome}
learning_core.user_withdrawn.delivery_lag
```

허용 outcome 예:

- consumer: `PROCESSED`, `DUPLICATE`, `PAYLOAD_CONFLICT`, `VALIDATION_REJECTED`, `TRANSACTION_FAILED`
- gate: `ALLOWED_NO_MARKER`, `ALLOWED_EXPIRED`, `DENIED`, `STORE_UNAVAILABLE`

요청마다 `ALLOWED_NO_MARKER`를 INFO 로그로 남기지 않는다. 고빈도 정상 경로는 metric도 sampling 또는 timer 중심으로 제한한다.

운영 alert:

- oldest pending age가 Access Token 잔여 차단 목표를 초과함
- dead-letter 1건 이상
- payload conflict 1건 이상
- consumer 5xx·gate store unavailable 지속
- delivery lag가 rollout SLO 초과

## 16. 테스트 계획

### 16.1 Identity domain·Transaction

- v1 outbox 생성과 UUID·시각 불변식
- userId unique index
- LOCAL·GUEST·Firebase MEMBER withdrawal이 outbox 한 건 생성
- User·Session·lifecycle·eligibility·outbox 동시 commit
- outbox save 실패 시 전체 rollback
- 동시 withdrawal 승자 한 건·멱등 응답
- 이미 WITHDRAWN인 반복 요청이 중복 event를 만들지 않음
- Token·credential·식별자 로그 비노출

### 16.2 Identity publisher

- atomic lease winner 한 개
- expired lease 회수
- 2xx published
- 408·425·429·5xx·timeout·connection retry
- 3xx·그 밖의 4xx·invalid payload dead-letter
- 최대 시도·backoff·manual replay
- 성공 응답 유실 뒤 duplicate delivery
- 기본 비활성 configuration과 enabled fail-fast

### 16.3 backfill

- dry-run mutation 0건
- 고정 cutover 범위의 missing outbox만 bounded insert
- 재실행 멱등
- concurrent live withdrawal unique conflict 수렴
- invalid withdrawnAt reconciliation
- lower bound 이전 User 제외와 upper bound 이후 live capture 분리

### 16.4 Learning Core consumer

- workload 인증 성공·실패와 user JWT 오용 거절
- payload field·UUID·schemaVersion·future skew validation
- 신규 event marker+inbox 단일 commit과 204
- 동일 event duplicate 204·mutation 0건
- 동일 eventId 다른 digest 409/422·marker 불변
- 동시 duplicate unique winner와 loser 수렴
- marker save·inbox save failure 전체 rollback
- event가 blockedUntil 이후 도착하면 inbox만 저장
- TTL index와 explicit time comparison

### 16.5 Learning Core request gate

- invalid JWT는 기존 401이고 marker 조회 0건
- valid JWT + no marker 허용
- valid JWT + active marker `401 ACCOUNT_WITHDRAWN`
- exact blockedUntil 경계
- expired marker가 TTL 지연으로 남아 있어도 허용
- marker store 장애 `503 WITHDRAWAL_DENY_GATE_UNAVAILABLE`
- application service·Repository mutation 진입 0건
- public AI callbacks와 internal workload endpoint 비적용
- 여러 application instance가 공용 marker를 즉시 관찰
- 오류 응답·로그에 Token·userId·eventId 비노출

### 16.6 staging E2E

- 탈퇴 직전 발급한 Access Token으로 탈퇴 전 성공·event 처리 후 거절
- 다른 기기의 기존 Access Token 거절
- publisher response loss와 duplicate 204
- timeout·429·5xx backoff, lease 회수, dead-letter, manual replay
- Transaction rollback과 Mongo write conflict
- clock skew·blockedUntil 경계
- marker·inbox TTL 실제 생성과 지연 삭제
- consumer→capture→backfill→publisher 활성화 순서
- Stage 4 모바일 Refresh 오류와 Stage 5 Access deny 결합 흐름

## 17. 배포 순서

1. wire schema·오류·TTL·workload auth profile 승인
2. Learning Core Mongo Transaction manager·Repository scan·index 배포
3. Learning Core internal consumer와 deny gate를 empty-store 상태로 배포
4. workload 인증 staging 검증
5. Identity outbox capture를 publisher 비활성 상태로 배포
6. 기존 WITHDRAWN User inventory와 dry-run backfill
7. staging backfill·publisher 활성화와 duplicate/장애 E2E
8. Learning Core consumer·gate production 활성화 확인
9. Identity backfill 후 publisher production 활성화
10. backlog·delivery lag·dead-letter 0건 확인
11. 모바일 Stage 4 호환성과 Stage 1~5 전체 E2E 확인
12. 마지막에 Firebase/SNS withdrawal production flag 활성화

consumer보다 publisher를 먼저 활성화하지 않는다. outbox capture는 publisher보다 먼저 배포해 cutover 중 event를 잃지 않게 한다.

## 18. rollback

문제 발생 시 순서:

1. Firebase/SNS withdrawal production flag 비활성
2. Identity publisher 비활성
3. pending outbox와 dead-letter 보존
4. Learning Core consumer endpoint와 deny gate는 가능한 한 유지
5. consumer 자체가 장애 원인이면 workload endpoint를 내리되 marker·inbox는 삭제하지 않음
6. 수정 배포 뒤 pending/dead-letter를 replay

이미 처리된 deny marker를 rollback하면서 old userId를 다시 활성화하지 않는다. marker는 Access Token 잔여 수명 뒤 자연 만료시킨다. outbox·inbox를 수동 삭제해 성공을 추측하지 않는다.

## 19. 예상 변경 파일

### Identity 저장소

- `domain/user/domain/entity/UserWithdrawnOutbox.java`
- `domain/user/domain/enums/UserWithdrawnOutboxStatus.java`
- `domain/user/domain/enums/UserWithdrawnFailureCode.java`
- `domain/user/domain/repository/UserWithdrawnOutboxRepository*.java`
- `domain/user/application/UserWithdrawalTransactionService.java`
- `domain/user/withdrawalevent/application/*`
- `domain/user/withdrawalevent/infrastructure/*`
- `application.yml`, `application-test.yml`, `.env.example`
- domain·Transaction·Repository·publisher·configuration·backfill 테스트

실제 package 이름은 현재 capability 구조를 따라 구현 전에 확정한다.

### Learning Core 저장소

- internal `UserWithdrawn` request/controller/transaction service
- event inbox·deny marker entity와 Repository
- Mongo Transaction·Repository scan configuration
- workload SecurityFilterChain
- `WithdrawnUserAccessGateFilter`와 전용 response handler
- `ErrorStatus`의 withdrawal·gate unavailable 오류
- application configuration과 contract·security·concurrency 테스트

Identity 저장소에 Learning Core 코드를 복사하지 않는다.

## 20. 완료 조건

- withdrawal 성공과 `UserWithdrawnOutbox` 생성이 같은 Identity Mongo Transaction으로 commit됨
- event v1 schema·digest·HTTP·workload 인증 계약이 양 저장소에서 동일함
- Learning Core consumer가 duplicate를 204로 멱등 처리하고 payload conflict를 격리함
- marker와 inbox가 같은 Learning Core Transaction으로 commit됨
- 유효한 old Access Token이 event 처리 후 application 진입 전에 `ACCOUNT_WITHDRAWN`으로 거절됨
- invalid JWT로 탈퇴 여부를 조회할 수 없음
- marker store 장애가 fail-closed 처리됨
- marker와 inbox가 합의한 TTL 뒤 정리되며 TTL 지연이 authorization 결과를 바꾸지 않음
- 기존 WITHDRAWN User backfill/cutover가 멱등 완료됨
- Token·credential·userId·eventId가 로그·metric·오류 응답에 노출되지 않음
- 양 서비스 전체 테스트와 실제 replica set·workload auth·multi-device staging E2E가 통과함
- consumer 선배포와 publisher 후활성화가 확인됨
- Stage 1~5와 모바일 호환성 완료 전 production withdrawal flag가 비활성으로 유지됨

## 21. Jira 분리와 다음 작업

구현 Jira는 소비자 선배포 순서를 드러내도록 최소 두 개로 분리한다.

1. Learning Core: `UserWithdrawn` inbox·deny marker·JWT gate
2. Identity: withdrawal outbox·publisher·backfill

Learning Core 이슈가 Identity 이슈를 선행 또는 blocks 관계로 연결한다. 공통 wire contract와 staging E2E는 두 이슈의 공통 완료 조건으로 둔다.

Jira 생성 전 다음 값을 최종 승인한다.

- workload 인증 방식과 internal endpoint
- Learning Core `allowedVerifierClockSkew`
- inbox retention과 manual replay 최장 기간
- 기존 WITHDRAWN User backfill 범위
- Learning Core 전용 오류 code와 gate store 장애 응답
