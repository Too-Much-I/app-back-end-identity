# Learning Core `UserMerged` consumer 구현 인계서

- 작성일: 2026-08-20
- 발행자: Identity Service
- 소비자: Learning Core
- 계약 버전: `UserMerged` schema version `1`
- 상태: Learning Core 구현 요청

## 1. 작업 목적

Identity에서 Guest 계정이 기존 MEMBER 계정으로 canonical merge되면 두 개의 userId가 다음 관계가 된다.

```text
sourceUserId: 병합 전 ACTIVE GUEST의 userId
targetUserId: 병합 후 최종 소유자가 되는 기존 ACTIVE MEMBER의 userId
```

Identity는 source User를 `MERGED` tombstone으로 전환하고 source RefreshSession을 폐기한 뒤 `UserMerged` outbox event를 발행한다.

Learning Core는 이 event를 받아 다음 두 결과를 **같은 Learning Core local Transaction**으로 확정해야 한다.

1. source userId가 소유하던 Learning Core 데이터를 target userId 소유로 이전한다.
2. source userId를 더 이상 actor로 인정하지 않는 deny marker를 저장한다.

source userId를 target userId의 authorization alias로 사용하면 안 된다. 과거 source JWT를 target JWT처럼 해석하거나 source 요청에 target 데이터를 반환하지 않는다.

## 2. Identity에서 이미 구현된 범위

Identity의 Guest merge Transaction에는 다음이 포함된다.

- source User를 `MERGED` tombstone으로 전환
- source의 `mergedIntoUserId=targetUserId`, `mergedAt` 기록
- source Guest credential 제거
- source RefreshSession 전체를 `GUEST_MERGED`로 폐기
- target RefreshSession 저장
- `UserMergedOutbox` 저장

Identity publisher는 outbox를 atomic lease로 claim해 Learning Core endpoint에 HTTPS POST로 전달한다.

- 전달 보장: at-least-once
- 인증: workload identity Bearer credential
- redirect: 따르지 않음
- `Content-Type`: `application/json`
- 최대 payload: 4 KiB
- 기본 publisher 상태: 비활성
- 성공 판정: 모든 `2xx`
- 재시도: `408`, `425`, `429`, `5xx`, timeout, connection failure
- 영구 실패 격리: 그 외 `3xx`·`4xx`, 잘못된 payload, 최대 재시도 초과

따라서 Learning Core consumer는 중복 전달과 성공 응답 유실을 정상 상황으로 처리해야 한다.

## 3. Wire event v1

요청 본문은 정확히 다음 필드만 포함한다.

```json
{
  "eventId": "9a88bc80-d73a-4a3d-8f68-492641d27208",
  "schemaVersion": 1,
  "sourceUserId": "73a18ed4-1d56-4c4f-afd6-b39175b82a86",
  "targetUserId": "45c05c3f-ae7f-4ca7-af88-3ab8aa8f428e",
  "occurredAt": "2026-08-20T02:00:00Z"
}
```

예시 UUID는 계약 형태를 설명하기 위한 가짜 값이다.

| 필드 | 타입 | 필수 | 의미 |
| --- | --- | --- | --- |
| `eventId` | canonical UUID string | 필수 | event의 영구적인 멱등성 key |
| `schemaVersion` | integer | 필수 | JSON 계약 버전. v1은 정확히 `1` |
| `sourceUserId` | canonical UUID string | 필수 | 병합되어 더 이상 actor가 될 수 없는 Guest userId |
| `targetUserId` | canonical UUID string | 필수 | 최종 데이터 소유자인 MEMBER userId |
| `occurredAt` | UTC ISO-8601 instant | 필수 | Identity merge Transaction에서 기록한 발생 시각 |

v1 불변식:

- `sourceUserId != targetUserId`
- 모든 UUID는 canonical UUID 문자열이어야 한다.
- email, phone, Firebase UID, provider subject, credential, JWT, consent 정보는 event에 없다.
- v1 payload에는 `eventType`이 없으며 전용 endpoint와 `schemaVersion`으로 계약을 식별한다.
- `occurredAt`은 Learning Core 처리 시각이나 정렬 순번이 아니다.
- 필드 순서에 의존하지 않는다.
- 알 수 없는 `schemaVersion`을 추측해서 처리하지 않는다.
- 같은 v1의 호환 가능한 optional field가 향후 추가되면 의미를 추측해 사용하지 않고 무시할 수 있어야 한다. 기존 필수 필드 검증은 유지한다.

## 4. Consumer endpoint 계약

Learning Core가 실제 내부 경로를 정한 뒤 Identity 배포 환경에 다음 값을 공유한다.

```text
USER_MERGED_PUBLISHER_ENDPOINT=https://<learning-core-host>/internal/v1/events/user-merged
```

Identity는 외부 Token 발급 API 없이 내부 typed purpose `USER_MERGED`를
고정 audience `learning-core-user-merged`로 매핑한다.

실제 host, credential, token은 저장소나 이 문서에 기록하지 않는다.

Learning Core endpoint 요구사항:

1. 외부 사용자 Access JWT가 아니라 server-to-server workload identity credential을 검증한다.
2. issuer, audience, signature, expiry와 허용된 Identity workload principal을 확인한다.
3. HTTPS만 사용한다.
4. `application/json`과 허용 payload 크기를 검사한다.
5. 인증 실패는 `401` 또는 `403`으로 응답한다.
6. response body는 Identity가 사용하지 않으므로 민감정보를 넣지 않는다.

### 4.1 Workload identity 필수 프로파일

기존 사용자용 Identity Access JWT를 내부 event 전달 credential로 재사용하면 안 된다.

```text
사용자 Access JWT
aud = tosunsaeng-learning-core
sub = userId

≠

workload credential
aud = UserMerged 내부 consumer 전용 audience
principal = Identity Service
```

Learning Core 구현 전에 아래 표를 별도 서버 간 인증 계약으로 채우고 Identity 팀과 함께 승인한다. `TBD`가 남아 있으면 staging publisher도 활성화하지 않는다.

| 항목 | 확정값 |
| --- | --- |
| credential type | `TBD` — OIDC JWT 등 실제 형식 |
| issuer | `TBD` |
| JWKS 또는 검증 key 배포 경로 | `TBD` |
| signing algorithm | `TBD` |
| audience | `TBD` — UserMerged consumer 전용 값 |
| Identity service principal claim/value | `TBD` |
| token TTL | `TBD` |
| 허용 clock skew | `TBD` |
| key rotation·overlap 절차 | `TBD` |
| credential 발급·refresh 책임 | Identity runtime credential provider |

검증기는 `kid`가 있는 JWT 형식을 선택하면 active/retiring key overlap을 지원하고, issuer·audience·principal·algorithm을 allowlist로 고정한다. 사용자 userId를 workload principal로 해석하지 않는다.

### 4.2 v1 처리 모델과 응답

v1은 **HTTP 요청 안에서 migration을 완료하는 direct local Transaction**으로 고정한다.

```text
POST UserMerged
→ workload credential·payload 검증
→ source write guard 획득
→ ownership migration
→ source deny marker
→ inbox PROCESSED
→ COMMIT
→ 204
```

`202 Accepted`로 inbox만 저장하고 나중에 worker가 처리하는 모델은 v1에서 사용하지 않는다. 신규 owner-event publisher의 기본 read timeout은 3초이므로 ownership inventory와 staging 부하 검증에서 transaction의 P99가 설정 timeout 안에 충분한 여유로 끝나는지 증명해야 한다. 충족하지 못하면 timeout만 임의로 늘려 활성화하지 않고 durable inbox + worker 계약으로 문서를 다시 승인한다.

응답 규칙:

| 상황 | 응답 | Identity 처리 |
| --- | --- | --- |
| 신규 event Transaction commit | `204` | 발행 완료 |
| 동일 `eventId` + 동일 payload의 PROCESSED 재수신 | `204` | 멱등 성공 |
| 일시적인 DB·내부 장애 | `5xx` | 재시도 |
| 일시적인 rate limit | `429` | 재시도 |
| 잘못된 JSON·필수 필드·UUID | `400` 또는 `422` | 영구 실패 격리 |
| 알 수 없는 `schemaVersion` | `422` | 영구 실패 격리 |
| 동일 `eventId` + 다른 payload | `409` 또는 `422`와 보안 경보 | 영구 실패 격리 |
| payload 크기 초과 | `413` | 영구 실패 격리 |
| 지원하지 않는 media type | `415` | 영구 실패 격리 |
| workload 인증 실패 | `401` 또는 `403` | consumer circuit pause·운영 격리 |

**동일 event 재수신에 `409`를 반환하면 안 된다.** 동일 payload의 duplicate는 이미 성공한 요청이므로 `2xx`를 반환한다.

동일 event가 다른 요청에서 처리 중이면 bounded wait 뒤 inbox를 다시 읽는다. winner가 commit한 `PROCESSED`이면 `204`, winner가 rollback했거나 제한 시간 안에 결과를 확인할 수 없으면 `425` 또는 `503`으로 재시도를 유도한다. 처리 중 상태를 target 성공으로 추측하지 않는다.

## 5. Inbox와 멱등성

Learning Core는 최소한 다음 의미를 가진 inbox를 소유한다. 실제 entity·table·collection 이름은 Learning Core 규칙에 맞춘다.

```text
eventId              unique
schemaVersion
payloadDigest        동일 eventId의 payload 불변성 검사
sourceUserId
targetUserId
occurredAt
receivedAt
processedAt
status               v1 direct 모델에서는 PROCESSED
```

처리 규칙:

1. `eventId`에 unique constraint를 둔다.
2. raw JSON 전체가 아니라 아래 v1 semantic field만으로 payload digest를 계산한다.
3. 처음 받은 event만 신규 처리한다.
4. 같은 `eventId`와 같은 digest면 현재 처리 결과를 재사용하고 `2xx`를 반환한다.
5. 같은 `eventId`와 다른 digest면 producer 계약 위반 또는 tampering으로 간주하고 처리하지 않는다.
6. eventId 선조회만 믿지 말고 DB unique constraint를 최종 동시성 경계로 사용한다.

v1 digest 입력은 다음과 같이 고정한다.

```text
normalizedSourceUserId = canonical lowercase UUID
normalizedTargetUserId = canonical lowercase UUID
normalizedOccurredAt   = RFC-3339 입력을 Instant로 parse한 뒤 UTC Instant.toString() 형태

digestInput = UTF-8(
    "tosunsaeng:user-merged"
    + NUL + "1"
    + NUL + normalizedSourceUserId
    + NUL + normalizedTargetUserId
    + NUL + normalizedOccurredAt
)

payloadDigest = lowercase hex(SHA-256(digestInput))
```

`eventId`는 unique lookup key이므로 digest 입력에서 제외한다. 알 수 없는 optional field와 JSON property 순서·공백도 digest 입력에서 제외한다. 따라서 같은 v1 의미 event에 호환 optional field가 추가돼도 payload conflict로 오판하지 않는다.

v1 direct 모델은 inbox `PENDING`을 먼저 commit하지 않는다. ownership migration·deny marker와 함께 최종 `PROCESSED` inbox가 commit되며, 실패하면 전체 Transaction이 rollback된다.

## 6. Learning Core 데이터 이전

consumer 구현 전에 **Phase 0 선행 작업**으로 Learning Core의 userId ownership inventory를 완료한다. inventory가 끝나지 않으면 endpoint·migration 구현을 완료로 처리하지 않는다.

권장 작업 분리:

```text
1. Learning Core userId ownership inventory
2. UserMerged endpoint + inbox + source ownership guard/deny marker
3. Exam·result ownership migration
4. Streak ownership migration
5. Vocabulary ownership migration
6. 나머지 aggregate·projection·cache migration
7. Identity ↔ Learning Core staging E2E
```

Learning Core가 userId로 소유권을 표현하는 모든 aggregate와 write 경로를 조사한다. 최소 검토 대상은 실제 저장소 구조를 기준으로 확정한다.

- 시험 및 모의고사 응시
- 시험 결과와 채점 결과
- 문제 풀이 진행 상태
- 10초 챌린지
- 스트릭
- 단어장
- 그 밖의 userId 기반 학습 데이터와 projection/cache

직접 userId를 가진 document만 찾지 않는다. 예를 들어 grading result가 userId가 아니라 `examId`를 통해 시험에 귀속된다면 exam ownership 이전만으로 함께 이동하는지 확인하고 불필요한 rewrite를 하지 않는다.

inventory 산출물에는 aggregate마다 다음을 포함한다.

- 직접·간접 ownership 경로
- source와 target의 모든 create/update/delete write 진입점
- unique index와 target 충돌 가능성
- migration query와 예상 문서 수·처리시간
- transaction 참여 가능 여부
- DB projection과 외부 cache 의존성
- audit 원본 보존 규칙

각 aggregate마다 다음 충돌 정책을 문서화하고 테스트한다.

- 단순히 owner userId를 source에서 target으로 변경할 수 있는가?
- source와 target 양쪽에 동일 unique key 데이터가 있으면 합칠지, 하나를 유지할지, 별도 이력을 보존할지?
- 최신값 선택이 필요하면 어떤 서버 시각 또는 revision을 기준으로 하는가?
- 통계·projection·cache를 재계산해야 하는가?
- immutable audit record의 actorUserId도 바꿀 것인가, 원래 actor를 보존할 것인가?

정책이 없는 충돌을 임의로 overwrite하거나 삭제하지 않는다. aggregate별 결정이 필요한 경우 해당 항목을 별도 Jira로 분리하고 merge feature 활성화를 막는다.

## 7. Source actor deny marker

Learning Core는 Identity JWT를 로컬 검증하므로 매 요청마다 Identity에 source 상태를 조회하지 않는다. 따라서 merge event 처리 후 source JWT가 암호학적으로 만료 전이어도 Learning Core에서 거절할 local marker가 필요하다.

source deny marker는 모든 user-owned write와 경쟁할 수 있는 **per-user ownership guard**로 구현한다. 실제 이름은 Learning Core 규칙을 따르되 최소 의미는 다음과 같다.

```text
userId             unique
state              ACTIVE | MERGED
revision           write와 merge의 CAS/write-conflict 경계
targetUserId       MERGED일 때 migration 추적용
mergedAt           MERGED일 때 필수
eventId            MERGED일 때 필수
```

기존 학습 데이터 owner에는 merge feature 활성화 전에 ACTIVE guard를 backfill하거나, 첫 write가 ACTIVE guard를 unique insert하도록 한다. source 데이터가 전혀 없으면 merge Transaction이 MERGED guard를 직접 만들 수 있다.

모든 user-owned write는 aggregate 변경과 같은 DB Transaction에서 guard의 `state=ACTIVE`를 확인하고 `revision`을 CAS 증가 또는 touch해야 한다. 단순 read-only 조회 뒤 별도 write하는 방식은 허용하지 않는다. 이렇게 해야 이미 진행 중인 source write와 merge가 같은 guard document/row에서 충돌한다.

Authorization 규칙:

```text
JWT 서명·issuer·audience·expiry 검증
→ sub 추출
→ ownership guard 조회
→ state=MERGED이면 요청 거절
→ targetUserId로 actor를 치환하지 않음
```

응답은 Learning Core의 기존 인증 오류 envelope를 따르되 안정적인 code를 둔다. Identity와 의미를 맞추려면 `ACCOUNT_MERGED_TOKEN_REJECTED`가 권장된다.

금지 사항:

- source JWT를 target JWT처럼 취급
- source 요청을 target ownership query로 자동 rewrite
- source userId를 target authorization alias로 사용
- source token에 target 데이터 반환

`sourceUserId -> targetUserId` mapping은 migration, idempotency, 감사 추적용이지 권한 승격용이 아니다.

## 8. Transaction 경계

다음 변경은 하나의 Learning Core local Transaction에서 함께 성공하거나 함께 rollback해야 한다.

```text
source ownership migration
+ aggregate별 충돌 정책 적용
+ DB projection 일관성 변경
+ external cache invalidation intent/outbox 저장
+ source ownership guard를 MERGED deny 상태로 CAS 전환
+ inbox PROCESSED 전환
```

Redis 같은 외부 cache의 실제 삭제를 DB Transaction에 포함한다고 표현하지 않는다. Transaction에는 cache invalidation intent/outbox 또는 DB marker만 저장하고 commit 후 별도 실행한다. cache가 없다면 이 단계는 생략한다.

중간 실패 예:

- 데이터 일부만 target으로 이전되고 source JWT는 계속 허용됨
- source JWT는 차단됐지만 target에서 데이터가 보이지 않음
- migration은 끝났지만 inbox가 미완료라 duplicate가 다시 변경함

위 상태가 commit되지 않도록 rollback injection test를 작성한다.

v1은 direct Transaction이므로 위 commit이 끝나기 전에 `2xx`를 반환하지 않는다. ownership inventory 결과 transaction이 producer timeout 안에 끝날 수 없으면 구현 중 임의로 worker 구조를 섞지 않고 문서를 개정한다.

## 9. 동시성 요구사항

- 같은 event의 동시 요청은 한 처리만 commit한다.
- 응답 유실 뒤 재전송되어도 결과가 바뀌지 않는다.
- 모든 user-owned write는 같은 per-user ownership guard를 Transaction 안에서 CAS/touch한다.
- merge Transaction은 source guard를 `ACTIVE -> MERGED`로 CAS하며 ownership migration과 같은 Transaction에 둔다.
- merge 전에 시작한 source write가 guard를 touch하면 merge 또는 write 중 하나가 write conflict로 실패·재시도되고, 재시도한 source write는 MERGED 상태를 보고 거절된다.
- guard가 없는 source에서 write의 ACTIVE insert와 merge의 MERGED insert가 경쟁하면 unique constraint로 한쪽만 성공하고 실패한 쪽은 상태를 다시 읽어 판단한다.
- migration commit 이후 source JWT write는 항상 거절한다.
- target의 정상 write와 migration이 경쟁해도 target 데이터를 overwrite하지 않는다.
- direct Transaction 중 process가 종료되면 전체 rollback되고 Identity retry가 처음부터 안전하게 재처리한다.

## 10. 관측과 개인정보

권장 metric:

- received event 수
- processed/duplicate/failed 수
- delivery lag: `receivedAt - occurredAt`
- processing lag: `processedAt - receivedAt`
- total lag: `processedAt - occurredAt`
- direct Transaction 처리시간과 timeout 수
- payload conflict 수
- source merged token 거절 수
- aggregate별 migration 실패 수

로그 규칙:

- raw Authorization header와 workload token을 로그에 남기지 않는다.
- source/target userId 원문을 일반 로그에 남기지 않는다.
- email, phone, Firebase UID, provider subject를 조회·event·로그에 추가하지 않는다.
- eventId, schemaVersion, bounded outcome/failure code 중심으로 기록한다.
- 예외 message에 payload 전체가 포함되지 않게 한다.

## 11. 필수 테스트

### 계약·인증

- workload credential 정상/만료/issuer 불일치/audience 불일치/허용되지 않은 principal
- content type과 payload size
- v1 정상 역직렬화
- unknown schema version
- null·blank·non-canonical UUID
- `sourceUserId == targetUserId`
- 알 수 없는 추가 필드 처리 정책 고정

### 멱등성·오류

- 동일 eventId·동일 payload 순차/동시 duplicate가 모두 `2xx`
- 동일 eventId·다른 payload는 mutation 없이 영구 오류
- consumer commit 뒤 HTTP 응답 유실 시 재전송 no-op
- transient DB failure가 `5xx` 후 재시도로 복구
- 처리 중 process 종료가 전체 rollback되고 Identity retry로 복구
- concurrent winner commit 뒤 duplicate는 `204`, 결과 미확정은 `425` 또는 `503`

### 데이터·권한

- source만 가진 aggregate가 target 소유로 이전
- source와 target 모두 데이터가 있는 aggregate별 충돌 정책
- migration 후 target JWT로 데이터 조회 가능
- migration 후 source JWT는 거절
- source JWT가 target actor로 치환되지 않음
- migration 중 source/target concurrent write
- merge 전에 시작한 source write와 source guard CAS가 경쟁해 한쪽이 conflict되고 재시도에서 MERGED가 거절됨
- guard 미존재 시 ACTIVE/MERGED 동시 insert의 unique 경쟁
- 각 저장 단계 failure injection 시 전체 rollback
- DB projection과 commit 후 external cache invalidation이 source 데이터를 재노출하지 않음

### 운영

- staging에서 실제 HTTPS endpoint와 workload identity 검증
- Identity publisher의 timeout/retry/duplicate delivery
- consumer 장애 후 복구와 backlog alert
- 배포 순서와 feature flag가 consumer 준비 전 merge를 차단

## 12. 완료 조건

- [ ] Learning Core 내부 endpoint와 workload audience가 확정됐다.
- [ ] workload credential type·issuer·JWKS·algorithm·audience·principal·TTL·clock skew·rotation 계약에서 `TBD`가 제거됐다.
- [ ] workload identity 인증과 network 접근 제한이 적용됐고 사용자 Access JWT 재사용이 금지됐다.
- [ ] schema version 1 validator가 구현됐다.
- [ ] v1 semantic field digest와 eventId unique inbox의 payload conflict 탐지가 구현됐다.
- [ ] 선행 ownership inventory와 모든 aggregate·write 경로·충돌 정책이 문서화됐다.
- [ ] 모든 user-owned write가 per-user ownership guard를 같은 Transaction에서 CAS/touch한다.
- [ ] source ownership migration과 source guard의 MERGED 전환, inbox PROCESSED가 같은 direct Transaction으로 처리된다.
- [ ] external cache가 있으면 invalidation intent가 Transaction에 저장되고 commit 후 실행된다.
- [ ] 신규·동일 duplicate 성공은 commit 확인 뒤 `204`를 반환하고 `202`를 사용하지 않는다.
- [ ] staging P99 처리시간이 Identity publisher read timeout 안에 충분한 여유로 들어온다.
- [ ] source JWT를 target actor로 해석하지 않는다.
- [ ] rollback·동시성·응답 유실·재시작 테스트가 통과한다.
- [ ] metric·alert·민감정보 비노출 로그가 준비됐다.
- [ ] staging에서 Identity publisher와 end-to-end 검증이 통과했다.
- [ ] consumer 준비 전 Identity merge/publisher production flag가 비활성임을 확인했다.

## 13. 배포 순서

1. Learning Core ownership inventory와 aggregate별 충돌 정책을 확정한다.
2. workload identity 프로파일의 모든 `TBD`를 확정한다.
3. per-user ownership guard를 배포·backfill하고 모든 user-owned write 경로가 guard를 같은 Transaction에서 touch하도록 전환한다.
4. semantic digest inbox·deny 상태·migration과 external cache invalidation intent를 구현한다.
5. direct consumer endpoint를 배포하되 Identity traffic은 아직 차단한다.
6. workload identity issuer·audience·principal과 network policy를 검증한다.
7. staging migration P99를 측정하고 Identity publisher read timeout을 안전한 값으로 합의한다.
8. Identity staging에 endpoint와 audience를 설정하고 publisher를 활성화한다.
9. duplicate·timeout·process restart·source write 경쟁·source deny·migration E2E를 수행한다.
10. delivery/processing/total lag·failure dashboard와 alert를 확인한다.
11. 모든 aggregate migration이 준비된 뒤에만 Identity merge feature를 production에서 활성화한다.

Rollback 시 Identity merge feature와 publisher를 먼저 끈다. 이미 `PROCESSED`로 commit된 inbox·ownership migration·source deny marker를 되돌리거나 source actor를 다시 활성화하지 않는다. 처리 중 process가 종료된 direct Transaction은 rollback되며 publisher retry로 복구되는지 확인한 뒤 consumer를 내린다.

## 14. Identity 팀과 합의가 필요한 항목

Learning Core 구현 전에 다음 값을 확정해 Identity 팀에 전달한다.

- 내부 HTTPS endpoint 경로
- workload credential type·issuer·JWKS·algorithm·audience·허용 principal·TTL·clock skew·rotation
- direct Transaction의 정상·duplicate 응답 `204`, 처리 중 미확정 응답 `425` 또는 `503`
- staging P99에 근거한 Identity publisher read timeout
- 처리 SLA와 latency alert 기준
- aggregate별 충돌 정책 중 제품 결정이 필요한 항목
- staging E2E 일정

Identity의 v1 wire payload 필드를 변경해야 한다면 consumer에서 추측하거나 임의 확장하지 않고 새 schema version 계약을 먼저 합의한다.
