# ADR-002: Phone eligibility binding은 consumer-scoped versioned event로 전달한다

- Jira: `TMI-95`
- 결정일: 2026-08-14
- transport 보정일: 2026-08-27
- 상태: 조건부 채택
- production 상태: 비활성
- 관련 문서: `docs/adr/ADR-001-firebase-authentication-broker.md`, `docs/contracts/phone-identity-key-rotation.md`, `docs/contracts/social-login-implementation-plan.md`

## 배경

Identity는 Firebase 신규 MEMBER 가입 Transaction에서 검증된 phone으로 `PhoneIdentity`와 별도의 eligibility candidate를 만들고 `PhoneEligibilityBindingOutbox`를 저장한다. 전화번호 원문은 저장하지 않으므로 가입이 끝난 뒤 같은 candidate를 임의로 복원할 수 없다. 따라서 production 가입을 활성화하기 전에 producer와 consumer가 전달, 중복, 역순, key rotation, 삭제를 같은 방식으로 처리해야 한다.

이 이벤트는 전화번호를 인증했다는 consumer-scoped binding만 준비한다. 이벤트 수신 자체는 TrialClaim, Entitlement 또는 시험 사용권 지급이 아니다. 혜택의 종류, 중복 방지 기간과 지급 Transaction은 Identity가 아닌 Entitlement/Billing consumer가 소유한다.

## 결정 요약

- Identity는 `PhoneEligibilityBindingVerified`와 `PhoneEligibilityBindingRevoked` versioned event의 유일한 producer다.
- 외부 전달은 VPC Lattice AWS_IAM·ECS task role·SigV4로 인증한 HTTPS push와 at-least-once delivery를 사용한다.
- consumer는 `eventId` inbox, `(userId, consumerScopeId)` revision high-water mark와 current binding을 하나의 로컬 Transaction으로 갱신한다.
- 같은 `eventId`의 같은 canonical payload는 성공 no-op, 다른 payload는 poison event다.
- 이벤트 순서는 시간이나 수신 순서가 아니라 `bindingRevision`으로 판단한다.
- candidate HMAC key는 Identity만 소유한다. consumer에는 `keyVersion`과 candidate만 전달한다.
- `consumerScopeId`는 client 입력이 아니라 Identity와 consumer의 배포 설정 allowlist에서만 선택한다.
- PhoneIdentity fingerprint와 raw phone은 payload, header, 로그, trace, metric으로 전달하지 않는다.
- consumer 계약과 publisher가 staging에서 검증되기 전 Firebase signup 관련 flag는 계속 비활성이다.

## 소유권 경계

| 책임 | 소유자 |
| --- | --- |
| verified phone 확인과 canonical `userId` | Identity |
| eligibility candidate key와 생성 | Identity |
| outbox와 `bindingRevision` 원자 생성 | Identity |
| 인증된 전달, retry와 dead-letter | Identity publisher |
| inbox 멱등성, revision high-water mark와 current binding | Entitlement/Billing consumer |
| TrialClaim, Entitlement와 중복 혜택 정책 | Entitlement/Billing consumer |
| 시험 생성·채점·결과 | Identity 외부 서비스 |

consumer가 아직 확정되지 않았거나 아래 계약을 구현하지 않았다면 Identity publisher가 존재하더라도 production signup gate는 통과하지 못한다.

v1 consumer owner는 별도 Entitlement/Billing bounded context와 배포 서비스다. 이 서비스는 inbox, current binding, revision high-water와 abuse/claim ledger를 자신의 datastore와 Transaction 경계에서 소유한다. Learning Core는 이 데이터를 직접 저장하지 않고 추후 Entitlement/Billing의 reserve/confirm 계약만 사용한다. 실제 저장소 생성, 인프라 provisioning과 consumer 구현은 Identity 저장소 밖의 별도 Jira로 진행한다.

## wire schema v1

### verified event

`PhoneEligibilityBindingVerified`의 schema v1은 다음 JSON object다. 예시의 candidate는 형식 검증용 가짜 값이며 실제 값이 아니다.

```json
{
  "eventId": "018f6f36-2f42-4bf5-8c17-0be35de4872c",
  "eventType": "PhoneEligibilityBindingVerified",
  "schemaVersion": 1,
  "producer": "identity",
  "occurredAt": "2026-08-14T05:00:00.000Z",
  "consumerScopeId": "opaque-scope-v1",
  "userId": "e8b37a41-bae6-47f1-a770-052e6c5786d4",
  "verifiedAt": "2026-08-14T04:59:58.000Z",
  "bindingRevision": 1,
  "fingerprintCandidates": [
    {
      "keyVersion": "v2",
      "value": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
    },
    {
      "keyVersion": "v1",
      "value": "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB"
    }
  ]
}
```

| 필드 | 계약 |
| --- | --- |
| `eventId` | producer가 한 번 생성한 lowercase UUID v4. wire event와 immutable payload를 식별한다. |
| `eventType` | verified event는 정확히 `PhoneEligibilityBindingVerified`다. |
| `schemaVersion` | JSON 구조와 필드 의미의 양의 정수 버전이며 v1은 `1`이다. |
| `producer` | v1에서는 정확히 `identity`다. transport principal과 함께 producer를 검증한다. |
| `occurredAt` | outbox event를 생성한 UTC `Instant`. RFC 3339 `Z`, millisecond precision을 사용한다. |
| `consumerScopeId` | 1~128자의 `[A-Za-z0-9._:-]` opaque token. client 또는 요청 body에서 받지 않는다. |
| `userId` | Identity가 발급한 lowercase canonical UUID 문자열이다. Firebase UID가 아니다. |
| `verifiedAt` | Firebase Admin proof를 Identity가 검증한 UTC 시각이다. event 생성 시각과 같을 필요는 없다. |
| `bindingRevision` | 같은 `(userId, consumerScopeId)`에서 단조 증가하는 1 이상의 JSON integer다. 2^53-1을 넘지 않는다. |
| `fingerprintCandidates` | 현재 binding의 완전한 retained candidate 집합이다. 일부 patch가 아니며 1~8개를 허용한다. |
| `keyVersion` | scope의 eligibility key ring 안에서 유일한 1~32자 `[A-Za-z0-9._-]` token이다. |
| `value` | HMAC-SHA-256 결과를 padding 없는 Base64URL로 인코딩한 43자 token이다. |

candidate 배열의 순서는 의미가 없다. 같은 `keyVersion`은 배열에 한 번만 나타나야 하며 `(keyVersion, value)` 중복도 허용하지 않는다. producer는 재현 가능한 payload를 위해 `keyVersion`, `value` 순으로 정렬하지만 consumer는 배열을 set으로 처리한다.

verified event는 해당 revision의 current binding 전체를 교체하는 state event다. consumer는 후보 중 하나만 일치해도 retained key version 기준으로 같은 eligibility phone proof로 판단할 수 있지만, 이 비교만으로 혜택을 지급해서는 안 된다.

### revoked event

phone binding을 대체하지 않고 해제하거나 계정을 탈퇴시키는 경우 더 높은 revision의 `PhoneEligibilityBindingRevoked`를 사용한다.

```json
{
  "eventId": "13702e7d-aa52-44dc-848b-59af50a296c5",
  "eventType": "PhoneEligibilityBindingRevoked",
  "schemaVersion": 1,
  "producer": "identity",
  "occurredAt": "2026-08-14T06:00:00.000Z",
  "consumerScopeId": "opaque-scope-v1",
  "userId": "e8b37a41-bae6-47f1-a770-052e6c5786d4",
  "revokedAt": "2026-08-14T06:00:00.000Z",
  "bindingRevision": 2
}
```

revoked event에는 phone, candidate, last4 또는 provider 정보를 넣지 않는다. consumer는 current binding의 candidate를 제거하되 revision high-water mark와 제한된 tombstone은 보존한다. phone 교체가 하나의 Identity Transaction에서 성공하면 더 높은 revision의 verified event 하나가 이전 current binding을 완전히 대체하므로 별도 revoke event가 필요하지 않다.

## 세 가지 version의 책임

| 값 | 관리 대상 | 변경 예시 |
| --- | --- | --- |
| `schemaVersion` | wire JSON 구조와 의미 | 필드 타입 변경, 필수 필드 추가, event 의미 변경 |
| `keyVersion` | eligibility HMAC candidate를 만든 key | 정상 rotation, key compromise 대응 |
| `bindingRevision` | user+scope current binding의 순서 | phone 교체, binding 해제, 탈퇴 |

세 값을 하나로 합치지 않는다. schema v1 이벤트 안에 key v1과 v2 candidate가 동시에 존재할 수 있고, 동일 schema와 key 집합으로 binding revision만 증가할 수도 있다.

Identity는 `(userId, consumerScopeId)`별 revision을 phone binding 변경과 같은 Mongo Transaction에서 원자적으로 할당하고 outbox를 저장한다. `(userId, consumerScopeId, bindingRevision)`은 unique여야 하며, commit된 revision을 다른 event에 재사용하지 않는다. revision은 연속성보다 단조 증가가 불변식이므로 consumer는 gap을 관측·경보하되 최신의 완전한 state event를 적용한다.

## schema 호환성과 전환

- optional 필드 추가처럼 기존 consumer가 무시해도 보안과 의미가 변하지 않는 additive change만 같은 `schemaVersion`에서 허용한다.
- 필드 삭제·rename·타입 변경, 필수 필드 추가, 기본 해석 변경과 보안 의미 변경은 새 `schemaVersion`이다.
- consumer는 알 수 없는 `eventType` 또는 `schemaVersion`을 추측해 처리하지 않고 non-retryable contract error로 격리한다.
- producer는 같은 `eventId`의 schemaVersion이나 payload를 재발행 중 변경하지 않는다.
- 전환 시 consumer가 구·신 버전을 먼저 함께 읽고, 그다음 producer가 새로 생성하는 event만 신버전으로 바꾼다. 이미 생성된 구버전 outbox는 구버전 그대로 drain한다.
- 같은 consumer route에 동일한 논리 변경을 구·신 schema로 이중 발행하지 않는다. 구버전 replay 기간과 inbox/high-water retention이 끝난 뒤에만 구버전 reader를 제거한다.

Java class, package 이름, MongoDB document 이름과 내부 enum은 wire contract가 아니다. 내부 refactor는 위 업무 필드가 유지되는 한 schema 변경을 요구하지 않는다.

## `eventId` 멱등성과 payload 충돌

producer는 event 생성 시 payload를 동결한다. consumer는 validation 뒤 candidate 배열을 `(keyVersion, value)`로 정렬하고 JSON Canonicalization Scheme 방식으로 정규화한 전체 object의 SHA-256 digest를 inbox에 저장한다. JSON duplicate field는 정규화 전에 거절한다. candidate 원문을 멱등성 확인 로그에 남기지 않는다.

| 수신 상황 | 처리 |
| --- | --- |
| 처음 보는 `eventId` | inbox, high-water mark와 binding 변경을 한 Transaction으로 처리한다. |
| 같은 `eventId`, 같은 canonical payload | 이미 성공한 것으로 2xx를 반환하고 no-op 처리한다. |
| 같은 `eventId`, 다른 canonical payload | 기존 값을 덮어쓰지 않고 poison event로 격리·경보한 뒤 409를 반환한다. |
| 다른 `eventId`, 낮은 revision | stale event로 inbox에 기록하고 binding은 변경하지 않은 채 2xx를 반환한다. |
| 다른 `eventId`, 같은 revision | producer invariant 위반으로 격리·경보하고 409를 반환한다. |
| 다른 `eventId`, 높은 revision | 새 current state를 적용한다. revision gap은 경보하되 state event가 완전하므로 적용한다. |

consumer Transaction의 논리 순서는 다음과 같다.

```text
begin transaction
  validate envelope and calculate canonical payload digest
  if inbox contains eventId:
    compare digest and return duplicate success or poison conflict

  read revision high-water mark for userId + consumerScopeId
  if revision is lower:
    insert inbox and return stale success
  if revision is equal:
    isolate invariant conflict
  if revision is higher:
    replace current binding or apply revocation
    update high-water mark
    insert inbox
commit
return 2xx
```

consumer는 local Transaction이 commit된 뒤에만 2xx를 반환한다. timeout이나 connection reset은 commit 여부를 알 수 없다는 뜻이므로 producer는 같은 `eventId`와 같은 payload를 다시 보낸다.

## opaque consumer scope와 candidate 생성

Identity의 scope registry는 `consumerScopeId`를 consumer endpoint와 workload identity audience에 매핑한다. scope는 배포 설정 allowlist에서만 읽으며 다음 입력에서 선택하지 않는다.

- public API request body, query와 header
- Firebase claim 또는 provider subject
- client가 보낸 제품·혜택 enum
- nickname, email 또는 phone 속성

v1 candidate input은 현재 구현과 동일한 아래 domain separation을 사용한다.

```text
HMAC-SHA-256(
  eligibilityKey(keyVersion),
  "tosunsaeng:identity:phone-eligibility-binding:v1"
    || 0x00 || consumerScopeId
    || 0x00 || normalizedE164
)
```

같은 phone이라도 scope가 다르면 candidate가 달라야 한다. eligibility key와 domain은 `PhoneIdentity` fingerprint key/domain과 공유하지 않는다. scope 이름은 Identity가 제품 의미를 해석할 수 있는 enum으로 사용하지 않고 consumer가 문서화한 opaque routing identifier로만 취급한다.

## candidate key 소유권과 rotation

eligibility HMAC key material은 Identity writer의 Secret 경계에만 둔다. consumer는 key material을 받거나 candidate를 새로 계산하지 않는다. candidate HMAC는 event 서명이 아니며 producer 인증은 transport credential이 담당한다.

정상 rotation 순서는 다음과 같다.

1. 새 key version을 Identity의 `LOOKUP_ONLY` retained key로 모든 writer에 배포한다.
2. 모든 writer가 기존 version과 새 version candidate를 함께 생성하는지 확인한다.
3. consumer가 새 `keyVersion` candidate를 저장·조회하고 reference count를 보고할 수 있는지 확인한다.
4. 새 version을 `ACTIVE_WRITE`, 기존 version을 `LOOKUP_ONLY`로 전환한 동일 설정을 모든 writer에 배포한다.
5. rollback 시 새 version을 삭제하지 않고 `LOOKUP_ONLY`로 유지한 채 이전 version을 다시 `ACTIVE_WRITE`로 바꾼다.
6. current binding과 abuse/claim ledger의 기존 version reference가 0이거나 승인된 보존 기간이 모두 지난 뒤에만 legacy key를 제거한다.

Identity는 raw phone을 저장하지 않으므로 old-only binding을 새 key로 임의 backfill하지 않는다. legacy key가 제거되기 전에 기존 binding이 만료되거나 phone 재인증으로 새 retained candidate가 생성되어야 한다. consumer reference 확인 없이 배포 설정에서 legacy key를 제거하는 작업은 금지한다.

key material은 저장소, event, DB, Jira, 로그, trace와 metric에 넣지 않는다. version과 저 cardinality 상태만 운영 지표로 사용할 수 있다.

## transport와 service authentication

2026-08-26 Billing C3-D 승인에 따라 v1 wire schema와 at-least-once 의미는 유지하고 transport authentication은 `VPC Lattice + AWS_IAM + ECS application task role + SigV4`로 확정한다. 기존 workload Bearer JWT 계약은 대체된다.

```text
POST /internal/v1/eligibility/trial/events
Content-Type: application/json
Authorization: <AWS SigV4 signed request>
```

- Identity는 ECS application task role의 임시 credential과 AWS SDK v2 signer를 사용한다.
- SigV4 signing service는 `vpc-lattice-svcs`, region은 `ap-northeast-2`다.
- VPC Lattice Billing service의 `AWS_IAM` auth policy는 같은 환경 Identity task role에 위 POST route만 허용한다.
- Identity user Access Token, Firebase ID Token, workload Bearer JWT, static shared API key와 caller-provided identity header를 사용하지 않는다.
- production/staging task role과 Lattice service network·service·policy를 분리하며 반대 환경 호출을 허용하지 않는다.
- Billing task 직접 접근은 security group으로 차단하고 network 위치만으로 producer를 신뢰하지 않는다.
- AWS credential, SigV4 Authorization·session token과 인증 오류 원문은 로그에 남기지 않는다.
- redirect는 따라가지 않는다. request body 상한은 16 KiB이며 BaseResponse wrapper를 사용하지 않는다.

현재 `JdkPhoneEligibilityBindingDeliveryAdapter`는 audience 기반 workload Bearer credential을 사용하는 이전 구현이다. publisher는 기본 disabled 상태를 유지하며, staging 연동 전에 SigV4 adapter·설정·contract test로 교체하고 이전 audience/credential provider 경로를 제거해야 한다.

| 결과 | publisher 처리 |
| --- | --- |
| consumer Transaction commit 뒤 2xx | `PUBLISHED` |
| timeout, connection error, 408, 425, 429, 5xx | 같은 eventId/payload로 backoff retry; 429·503의 유효한 `Retry-After`보다 이르게 재시도하지 않음 |
| malformed payload 400, event conflict 409, unsupported contract 422 | `DEAD_LETTER`, 격리와 경보 |
| 401, 403 | event를 `DEAD_LETTER`로 격리하고 scope delivery를 중지한다. 보안·설정 수정 후 수동 replay |
| 3xx 또는 그 밖의 응답 | redirect하지 않고 설정 오류로 격리 |

eligibility endpoint의 409는 `EVENT_ID_CONFLICT` 전용이다. `COMMAND_PROCESSING`은 Reservation command 계약이므로 Identity publisher가 error body를 읽어 두 409를 구분하지 않는다. delivery port는 HTTP status와 검증된 `Retry-After`를 publisher에 전달하되 response body는 저장하지 않는다.

## producer outbox와 retry

publisher 구현은 최소 다음 상태와 필드를 가져야 한다.

```text
status: PENDING | IN_FLIGHT | PUBLISHED | DEAD_LETTER
leaseOwner
leaseExpiresAt
attemptCount
nextAttemptAt
lastFailureCode
publishedAt
```

- due `PENDING` 또는 lease가 만료된 `IN_FLIGHT` event 하나를 조건부 update로 claim한다.
- lease 시간은 전체 network timeout보다 길어야 하며 기본 60초로 시작한다.
- retry는 `min(5초 * 2^(attempt-1), 15분)`에 ±20% jitter를 적용한다. 429·503에 유효한 `Retry-After`가 있으면 자체 계산 시각과 비교해 더 늦은 시각을 사용하며 비정상·과도한 값은 승인된 상한으로 제한한다.
- 12회 실패하거나 non-retryable 응답을 받으면 `DEAD_LETTER`로 전환하고 경보한다.
- 수동 replay는 기존 immutable eventId와 payload를 사용한다. 새 eventId로 실패를 숨기지 않는다.
- `lastFailureCode`에는 정해진 저 cardinality 분류만 저장한다. response body, candidate, credential과 내부 exception message는 저장하지 않는다.

여러 publisher instance가 같은 event를 동시에 보내더라도 consumer 멱등성이 최종 안전망이다. lease는 중복 가능성을 줄일 뿐 exactly-once 보장이 아니다.

## 보존과 삭제

모든 값은 pseudonymous data로 취급하고 목적별로 분리한다. 아래 기간은 v1 기본 계약이며 더 길게 보존하려면 별도 개인정보·법무 승인과 문서화가 필요하다.

| 데이터 | 기본 보존 | 삭제·연장 조건 |
| --- | --- | --- |
| producer `PENDING`/`IN_FLIGHT` | terminal 상태까지 | retry 소진 시 `DEAD_LETTER`로 이동하며 active 상태에 TTL을 걸지 않는다. |
| producer `PUBLISHED` payload | `publishedAt`부터 30일 | replay·감사 기간 뒤 candidate 포함 payload를 삭제한다. |
| producer `DEAD_LETTER` payload | terminal 전환부터 90일 | replay 또는 명시적 폐기 결정을 기록한다. unresolved event를 TTL로 조용히 삭제하지 않는다. |
| consumer inbox digest | 수신부터 120일 | event payload 대신 canonical digest와 최소 metadata만 보존한다. |
| consumer current binding | 교체 또는 revoke까지 | 더 높은 verified event로 교체하거나 revoked event로 candidate를 제거한다. |
| consumer revision tombstone | revoke 후 120일 이상 | producer 최대 replay window보다 짧게 설정하지 않는다. |
| abuse/claim ledger | consumer 정책 기간 | 제품·개인정보·법무가 승인한 명시적 최대 기간이 필요하며 무기한을 기본값으로 두지 않는다. |

producer의 수동 replay window는 event 발생 후 최대 90일이다. 그 이후 replay가 필요하면 consumer tombstone과 inbox 보존 연장을 먼저 합의한다. backup과 복구본에서도 플랫폼 삭제 정책에 따라 candidate가 제거되어야 한다.

## phone 교체, 해제와 탈퇴

- phone 교체 성공은 같은 Transaction에서 더 높은 `bindingRevision`의 verified event를 생성한다. 이 event가 current binding 전체를 대체한다.
- phone 해제처럼 대체 binding이 없는 경우 더 높은 revision의 revoked event를 생성한다.
- 회원 탈퇴 Transaction은 current scope마다 revoked event를 생성하는 후속 lifecycle을 가져야 한다.
- revoked event가 먼저 도착한 뒤 과거 verified event가 도착해도 high-water mark 때문에 binding이 복원되지 않는다.
- consumer는 revoke 시 current candidate를 삭제하지만 abuse/claim ledger를 자동 삭제하지 않는다. 해당 ledger는 승인된 별도 목적과 기간에 따라 삭제한다.
- revision tombstone이 만료된 뒤 producer가 과거 event를 replay해서는 안 된다.

현재 Identity withdrawal과 phone change 경로에 이 event 생성이 연결되어 있지 않으므로 production gate 전에 별도 구현과 Transaction 테스트가 필요하다.

## 로그, trace와 metric 금지 항목

다음 값은 payload, header, 로그, exception message, Sentry breadcrumb, trace attribute와 metric label에 넣지 않는다.

- raw phone과 last4
- Firebase UID와 Firebase credential
- provider subject와 email
- `PhoneIdentity` fingerprint와 alias
- eligibility candidate `value`
- HMAC key material
- user Access Token, service credential과 Authorization header

운영 지표에는 event type, schema version, opaque scope, status, attempt bucket과 저 cardinality failure code만 사용할 수 있다. `userId`와 `eventId`는 일반 metric label에 사용하지 않는다. 제한된 incident 조회는 접근 통제된 감사 도구에서만 수행한다.

## 현재 구현과의 차이

현재 구현이 이미 만족하는 항목:

- `PhoneIdentity`와 별도 key ring·domain을 사용하는 candidate 생성
- config에서 주입되는 opaque `consumerScopeId`
- raw phone 비저장과 candidate 문자열 표현 redaction
- signup Transaction에 UUID `eventId`, canonical `userId`, scope, retained candidates와 `verifiedAt` outbox 저장
- Firebase와 eligibility binding 기본 비활성

후속 구현이 필요한 항목:

- wire `eventType`, `schemaVersion`, `producer`, `occurredAt` snapshot
- `(userId, consumerScopeId)`의 원자적 `bindingRevision` 저장·증가와 unique invariant
- revoked event와 withdrawal·phone lifecycle 연결
- `IN_FLIGHT`, `DEAD_LETTER`, lease, attempt, backoff와 failure classification
- immutable wire payload 또는 안정적인 mapper와 publisher delivery port
- workload identity 기반 transport adapter
- 목적별 TTL/cleanup과 manual replay 운영 절차
- 외부 consumer의 inbox, canonical digest, revision high-water/current binding Transaction
- consumer의 keyVersion reference count와 abuse ledger 보존 정책

현재 `PENDING`/`PUBLISHED` 두 상태만 있는 outbox를 이 ADR의 완료된 publisher로 해석하지 않는다.

## production 활성화 gate

다음 항목을 모두 충족하기 전 `FIREBASE_AUTH_ENABLED`, provider flag, phone과 eligibility binding 관련 flag를 production에서 켜지 않는다.

1. Identity와 Entitlement/Billing 소유자가 이 ADR을 승인한다.
2. consumer endpoint, workload identity issuer·audience와 owner on-call이 확정된다.
3. consumer inbox·binding·high-water update가 실제 지원 DB Transaction에서 검증된다.
4. publisher lease·retry·dead-letter와 timeout 후 같은 event 재전달을 staging에서 검증한다.
5. 중복, 같은 eventId의 다른 payload, 역순, revision gap과 unknown schema contract test가 통과한다.
6. normal·rollback·legacy removal key rotation을 staging에서 연습한다.
7. 보존·삭제 job과 consumer abuse ledger 최대 보존 기간이 승인된다.
8. outbox 미도착·지연 중 혜택 요청이 지급으로 우회되지 않고 retry 가능한 processing 상태로 실패한다.
9. candidate와 credential이 로그·trace·metric에 노출되지 않음을 확인한다.

## 결과와 trade-off

장점:

- phone 원문과 Identity 내부 fingerprint를 공유하지 않고 consumer가 중복 eligibility를 비교할 수 있다.
- at-least-once 전달의 중복과 역순에도 current binding이 과거 상태로 돌아가지 않는다.
- key rotation 중 구·신 candidate를 함께 전달해 무중단 비교가 가능하다.
- Identity가 TrialClaim·Entitlement 의미를 알지 않아도 domain 경계를 유지할 수 있다.

비용:

- producer outbox, service authentication, consumer inbox와 revision tombstone을 각각 운영해야 한다.
- raw phone 비저장 때문에 누락된 legacy candidate를 임의 backfill할 수 없다.
- candidate도 pseudonymous data이므로 보존·삭제와 접근 통제가 필요하다.
- consumer 준비가 늦어지면 Firebase signup production 활성화도 늦어진다.

## 제외 범위와 후속 작업

이 ADR은 publisher·consumer 코드, TrialClaim·Entitlement 지급, 시험 도메인, production feature 활성화와 legacy password API 종료를 구현하지 않는다. 후속 Jira는 최소 다음 두 작업으로 나눈다.

1. Identity outbox schema/revision 확장과 lease·retry·dead-letter HTTPS publisher
2. Entitlement/Billing inbox·current binding·high-water와 claim ledger 계약 구현

후속 구현 PR이 병합되고 production gate가 검증되기 전 이 ADR만으로 Jira 구현 상태를 production ready로 해석하지 않는다.
