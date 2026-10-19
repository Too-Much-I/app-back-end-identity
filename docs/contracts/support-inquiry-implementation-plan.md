# 문의 접수 및 운영 알림 구현 계획

작성일: 2026-10-05 · 갱신일: 2026-10-06 · 상태: 코드 구현 완료, 실제 배포·Slack 검증 대기 · 관련 Jira: [TMI-197](https://to-teacher.atlassian.net/browse/TMI-197), 상위 TMI-136

구현 결과와 정확한 외부 계약/운영 제한은 [문의 API 및 운영 계약](support-inquiry-api.md)을 따른다. 아래는 승인 당시 계획을 보존한다. 차이: 기존 JWT에 sessionId/폐기 epoch가 없어 개별 로그아웃 세션의 즉시 폐기 확인은 구현하지 못하며 기존 JWT 검증+현재 ACTIVE 계정 검증을 적용한다. 수동 복구/삭제는 감사·CAS가 있는 내부 primitive이며 실행용 CLI/관리 UI는 별도다. backlog gauge/자동 운영 경보와 실제 Slack 테스트는 배포 전 남은 작업이다.

2026-10-06 승인 반영: 사용자가 남은 권장안을 승인했다. Identity 독립 support 모듈, 선택 이메일·수동 회신, 문의90일·멱등7일 보관, 사용자/익명 IP별 시간5건·일10건, Slack 접수번호+본문으로 확정한다. 앞서 선택한 Slack/DB 상세 조회/횟수 제한만/화면 주의 문구를 유지한다. 실제 채널·담당자·Secret·개인정보 처리 고지와 Slack 별도 보관/삭제 준비는 출시 전 확인사항이며 이번 승인은 구현·배포·외부 전송 실행 요청이 아니다.

## 1. 5줄 결론

1. 인증 실패 화면과 설정 화면에서 공통 `POST /api/v1/support/inquiries`로 문의한다.
2. 유효한 Identity 인증에서 서버가 userId를 식별하며 비로그인은 null로 접수한다. 본문 userId는 받지 않는다.
3. 문의와 알림 outbox를 하나의 MongoDB 트랜잭션으로 저장한 뒤 접수 성공을 반환한다.
4. Slack에 접수번호와 본문을 비동기로 전송하며 실패해도 문의를 지우지 않는다. 중복 접수와 알림 중복은 별도로 제어한다.
5. 기능·운영 기본안은 승인됐으며 구현 요청 후 개발한다. 이 문서는 API나 배포 설정을 변경하지 않는다.

## 2. 사용자가 반드시 읽어야 하는 내용

- 문의 접수와 계정 복구는 별개다. 문의 전송이 계정 연결·병합·인증 제한을 해제하지 않는다.
- 로그인 불가 사용자도 문의할 수 있어야 한다. 만료/위조 토큰을 조용히 익명으로 바꾸지 않고 401을 반환하며, 앱에서 사용자에게 안내한 뒤 Authorization 없는 비로그인 문의로 전환한다.
- 서버가 DB 저장을 확인해야만 접수 완료다. 웹훅 성공을 기다리거나 웹훅 실패로 이미 저장한 문의를 실패 표시하지 않는다.
- Slack 표시 확정: 접수번호와 AI 요약 없는 문의 본문을 표시한다. 이메일/userId/진단 필드는 DB에만 보관하고 자동 첨부하지 않는다. 본문 자체에 개인정보가 포함될 수 있으므로 개인정보 처리 고지 및 보관 정책 검토 완료 전 실제 전송하지 않는다.
- 2026-10-06 입력 화면 선택: 별도의 'Slack으로 전달됩니다' 문구는 넣지 않고 개인정보 입력 주의 문구를 표시한다. 예시: '문의 내용에 비밀번호, 인증번호, 결제정보 등 민감한 정보를 입력하지 마세요. 답변받을 이메일은 별도 입력란을 이용해 주세요.' 이메일 안내는 실제 회신 입력란을 제공할 때만 사용한다. 개인정보 처리방침 링크는 제공하며, 실제 계약/처리 위치에 따른 위탁·국외 이전 등 필요한 고지·동의 여부는 별도로 검토한다. 주의 문구가 필요한 법적 고지·동의를 대체한다고 가정하지 않는다.
- 첫 버전은 텍스트 접수 및 운영자 알림이다. 첨부파일, 앱 내 답변/조회, 자동 계정 복구, AI 요약은 제외한다.
- 사용자는 이번 문의 접수에 한해 Identity의 독립 support 모듈 배치를 승인했다. 기존 인증 도메인과 분리하고 Learning Core의 학습 데이터를 직접 읽거나 수정하지 않는다.

## 3. 확정된 선택과 출시 전 준비사항

| 결정 | 사용자 승인안 | 구현/출시 조건 |
|---|---|---|
| 소유 서비스 | Identity의 독립 support 모듈, 인증 도메인과 분리 | 실제 구현은 별도 요청 후 |
| 알림 채널 | Slack 선택 완료 | 비공개 채널·수신자·Secret 설정 필요 |
| 회신 | 선택 입력 이메일로 담당자가 수동 회신 | 수집 안내·운영 책임자 지정. 이메일 미입력 시 개별 회신 불가 안내 |
| 운영 상세 조회 | 제한된 DB 조회 선택 완료, 공개 조회 API 없음 | 담당자 최소권한/조회 감사 확보 |
| 보관 | 문의90일·멱등 기록7일 | 법적 의무 검토 및 개인정보 안내 반영, Slack 별도 보관/삭제 준비 |
| 스팸 제한 | 신규 접수 사용자당 시간5건/일10건, 익명 IP당 시간5건/일10건. CAPTCHA 없음 | 분산 집계 구현 및 NAT 공유 영향 검증 |
| Slack 표시 | 접수번호+본문, 요약 및 다른 필드 없음 | 비공개 채널·멘션/링크 미리보기 억제 |

미지정 채널·담당자·자격증명을 임의로 선택하지 않는다. 법적 검토 완료를 사용자 기능 승인으로 대체하지 않는다. 요청 제한의 시간 창/구체 오류 계약 등 기술 세부안은 구현 시 문서·테스트로 고정한다.

## 4. 주요 위험과 미확인 사항

- 공개 접수는 스팸/알림 폭주 및 비용 공격에 노출된다. 프로세스 메모리만의 제한은 다중 ECS에서 충분하지 않다.
- 문의 본문에 사용자가 개인정보를 입력할 수 있다. 입력 안내, 최소 접근 권한, 보관 만료, 삭제 운영을 함께 준비한다. 자동 마스킹만으로 완전한 비밀 제거를 보장하지 않는다.
- 웹훅 성공 후 응답 유실/작업자 종료 시 중복 알림 가능성이 있다. exactly-once 전송을 보장하지 않고 동일 접수번호로 식별한다.
- 인증 경로의 장애와 문의 API가 같은 서버/DB를 공유하면 함께 실패할 수 있다. 접수 실패 시 대체 연락 방법을 앱에 제공한다.
- 현재 계획은 소스 패턴을 참고한 설계이며 운영 webhook/WAF/인덱스/배포 상태를 확인한 결과가 아니다.
- 세션 폐기 및 탈퇴/병합 계정 검사는 JWT 서명 검증만으로 대체하지 않는다. 인증 문의 정책을 별도로 테스트한다.

## 5. 현재 작업과 직접 관련된 설명

### 5.1 요청 계약 제안

`POST /api/v1/support/inquiries`, `Content-Type: application/json`, `Idempotency-Key: <소문자 UUID v4>`.
Authorization은 선택이지만 존재하면 정상적인 JWT 검증을 반드시 통과해야 한다.

```json
{
  "category": "AUTH",
  "message": "게스트 병합 후 다음 화면으로 진행되지 않습니다.",
  "replyEmail": "user@example.com",
  "context": {
    "screen": "GUEST_MERGE",
    "errorCode": "GUEST_MERGE_CONFLICT",
    "requestId": "example-request-id",
    "appVersion": "1.2.0",
    "platform": "ANDROID"
  }
}
```

제안 검증값:

| 필드 | 규칙 |
|---|---|
| category | 필수, AUTH 또는 GENERAL |
| message | 필수, trim 후 10~2,000자 |
| replyEmail | 선택, 최대254자 및 이메일 형식. 소유권 확인된 연락처로 간주하지 않음 |
| context | 선택. 자유 map이 아니라 고정 필드 DTO |
| screen | 최대64자, 제한 문자 집합 |
| errorCode | 최대100자, 영문 대문자/숫자/밑줄 |
| requestId | 최대128자, 제한 문자 집합. 조회 권한이나 계정 소유 증명 아님 |
| appVersion | 최대32자, 표시용이며 사용자 정책 결정에 사용하지 않음 |
| platform | ANDROID / IOS / WEB / UNKNOWN |
| 요청 크기 | JSON UTF-8 최대16KiB, 문자 제한과 별도로 검사 |

userId, Firebase UID, 토큰, 비밀번호, 전화번호, raw request/response, 임의 webhook URL 필드는 허용하지 않는다. 문의 DTO의 알려지지 않은 필드 거부 여부는 전역 설정 변경 없이 이 API에 적용한다. 내용/연락처를 DTO toString이나 검증 오류 로그에 출력하지 않는다.

### 5.2 사용자 식별 및 인증 경계

- MEMBER/GUEST: 검증된 JWT sub와 서버 계정 상태를 확인하여 userId와 accountType 스냅샷 저장.
- ANONYMOUS: Authorization 없음. userId=null, actorType=ANONYMOUS.
- 만료/위조 토큰, 폐기된 세션, MERGED/WITHDRAWN 등 유효하지 않은 인증 주체: 해당 인증 오류 반환. 인증된 사용자라고 기록하지 않는다.
- 익명 문의의 본문·추적번호에 특정 계정 정보가 있어도 소유자라고 확정하지 않는다.
- 조회/복구/연락처 변경 권한을 문의 접수 권한으로 부여하지 않는다.
- 현재 공개 endpoint 방식과 동일하게 정확한 POST 경로만 허용한다. `/api/v1/support/**` 전체 공개 금지. Bearer가 있을 때 Resource Server 검증이 유지되는지 통합 테스트한다.

### 5.3 원자적 접수와 응답

1. 크기/기본 형식/인증/요청 제한 검증.
2. 인증 범위와 멱등 키로 기존 접수 대조.
3. 신규이면 Mongo 트랜잭션으로 inquiry와 delivery outbox 생성.
4. 커밋 성공 후 HTTP 201 및 BaseResponse 반환. 동일 요청 재조회는 200.
5. 트랜잭션 결과가 불명확하면 성공을 추정하지 않는다. 앱은 같은 키·동일 본문으로 재시도한다.

```json
{
  "isSuccess": true,
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "result": { "inquiryId": "<접수번호>", "status": "RECEIVED" }
}
```

접수번호는 서버 생성 추측 어려운 UUID로 하며 조회 인증 수단이 아니다. 결과에 본문/이메일/userId를 반환하지 않는다. 응답은 Cache-Control: no-store. 접수 실패 시 자동으로 키를 바꾸지 않는다.

### 5.4 멱등성

- 범위: 검증된 사용자 ID 또는 ANONYMOUS + Idempotency-Key. 익명 키는 충분한 난수여야 하며 내용 반환/공개 조회를 허용하지 않는다.
- 동일 범위/키/정규화 payload digest → 기존 접수번호 반환. 다른 내용 → 409 SUPPORT_INQUIRY_REQUEST_CONFLICT.
- digest는 서버가 검증·정규화한 필드 전체를 고정 순서로 직렬화하여 산출. 원문 연락처를 키/로그에 포함하지 않는다.
- DB unique 인덱스로 동시 요청 중복을 막고 DuplicateKey 뒤 승자 문서를 재조회한다. 단순 조회 후 insert만으로 구현하지 않는다.
- 멱등 보존 기간은 승인된 7일. 만료 뒤 같은 키를 재사용하면 신규 접수될 수 있음을 계약에 명시한다.
- 인증→익명/다른 계정 전환은 범위가 달라 중복 방지되지 않는다. 결과가 불명확한 전송을 자동으로 다른 범위로 재전송하지 않는다.
- 중복 replay도 기본 남용 제한을 적용하되 신규 접수 quota와 분리하여 정상 복구를 막지 않도록 한다.

### 5.5 DB 모델 및 인덱스 제안

`support_inquiries`:
inquiryId, actorType, userId(nullable), category, message, replyEmail(nullable), context,
status(RECEIVED), createdAt, expiresAt.

`support_inquiry_requests`:
scopeKey, requestKey, payloadDigest, inquiryId, createdAt, expiresAt.
unique(scopeKey, requestKey). TTL(expiresAt). 문의 본문은 복제하지 않는다.

`support_inquiry_deliveries`:
deliveryId, inquiryId, destination(SLACK), status, attempts, nextAttemptAt,
leaseToken, leaseUntil, lastErrorCategory, createdAt, sentAt, expiresAt.
unique(inquiryId,destination), 작업 조회용(status,nextAttemptAt), TTL(expiresAt).

문의90일·멱등7일 보관은 승인됐다. 알림 기록도 문의 보관 기간을 넘기지 않는 최대90일로 설계한다. 실제 출시 전 관련 의무와 개인정보 안내를 검토한다. TTL은 즉시 삭제가 아니므로 앱/운영 조회에서 만료를 검사하고 개인정보 삭제 요구는 별도로 처리한다. 만료된 문의의 알림을 새로 보내지 않으며 outbox에도 본문을 중복 보관하지 않는다.

### 5.6 비동기 알림

- 상태: PENDING → SENDING → SENT. 일시 실패는 RETRY_WAIT, 영구 오류/재시도 소진은 FAILED.
- 다중 ECS 작업자가 CAS로 한 건을 claim하고 leaseToken이 일치할 때만 완료 상태를 갱신한다. 작업자 종료 후 lease 만료 시 재시도 가능하나 중복 알림 가능성은 남는다.
- 제안: 최초 포함 최대5회, 연결 timeout 3초/전체 호출 timeout 10초, lease60초, 재시도 기본30초→2분→10분→30분 + jitter. 429의 Retry-After를 준수하며 너무 긴 지연은 운영 실패/대기로 명시 처리한다.
- 네트워크/408/429/5xx 재시도, 그 외 4xx는 기본 FAILED. 공급자별 성공 HTTP/응답 계약을 각각 검증한다.
- 웹훅 endpoint는 운영자가 설정한 HTTPS 공급자 allowlist만 사용, 리다이렉트 추적 금지. 요청에서 URL을 받지 않는다.
- Slack 텍스트는 구조화 템플릿으로 생성하고 멘션/markdown 주입을 차단한다. 첫 버전에 Discord adapter는 구현하지 않는다.
- 표시 확정: 접수번호와 요약 없는 plain text 본문을 전송. 별도 연락처/userId/진단 메타데이터는 제외. 링크 미리보기 및 멘션 해석을 비활성화한다. Slack에 복제된 본문은 DB TTL 삭제로 지워지지 않으므로 별도 보관/삭제 절차를 마련한다. Incoming Webhook만으로 메시지 삭제까지 해결된다고 가정하지 않는다.
- FAILED 수동 재전송은 내부 운영 도구에서 승인·감사·CAS로 처리. 성공한 문의 삭제/새 문의 생성으로 우회하지 않는다.

### 5.7 오류 계약 제안

| HTTP/code | 처리 |
|---|---|
| 400 INVALID_REQUEST | 필드 형식·크기 오류 수정 |
| 400 INVALID_SUPPORT_REQUEST_ID | 키 누락/형식 오류 수정 |
| 401 기존 인증 오류 | 정상 인증 복구 또는 명시적 비로그인 접수 선택 |
| 403 기존 계정/접근 오류 | 계정 상태 확인, 익명으로 자동 강등 금지 |
| 409 SUPPORT_INQUIRY_REQUEST_CONFLICT | 같은 키에 다른 내용. 기존 접수 시도 상태 확인 |
| 413 요청 크기 초과 | 앞단/앱 응답 형식 일치 여부 테스트 |
| 429 SUPPORT_INQUIRY_RATE_LIMITED | Retry-After 후 재시도 |
| 503 SUPPORT_INQUIRY_UNAVAILABLE | 미접수 또는 결과 불명. 동일 키로 재시도 |
| 500 INTERNAL_SERVER_ERROR | 추적번호로 조사, 동일 키 유지 |

새 code는 모두 제안이다. webhook 실패는 접수 API의 실패 응답으로 소급하지 않는다. HTML/프록시 오류 및 네트워크 응답 없음도 프론트에서 처리한다.

### 5.8 남용 방지와 관측

- 승인된 초기 제한: 신규 접수 사용자당 시간5건/일10건, 익명 IP당 시간5건/일10건. 공유망 영향을 검증하고 운영 중 변경 시 별도 결정한다. CAPTCHA/봇 검증은 추가하지 않는다.
- 모든 요청의 짧은 burst 제한은 앞단에서 적용하고 신규 접수 quota는 분산 원자 카운터로 관리한다. 기존 분산 저장소 유무 조사 후 Mongo TTL 카운터/기존 인프라 중 선택. 무조건 새 Redis 추가하지 않는다.
- 신뢰한 프록시 체인에서만 클라이언트 IP를 산출한다. 임의 X-Forwarded-For 신뢰 금지. 제한 식별자는 별도 Secret 기반 HMAC 등으로 최소화하며 원 IP를 문의 본문에 저장하지 않는다.
- 로그: 접수번호/서버 추적번호/오류 분류/소요 시간. 본문·이메일·토큰·웹훅 URL·공급자 raw 응답 제외.
- 메트릭: 접수 성공/실패/replay/conflict, 대기 최장 시간, 전송 성공/실패/재시도, FAILED 건수. 사용자·이메일·접수번호를 메트릭 label로 쓰지 않는다.
- 알림 채널 자체 장애는 동일 채널로만 알리지 않는다. CloudWatch 등 별도 운영 경보 필요.

### 5.9 구현 순서

1. 승인된 범위 기준 구현 요청 확인 및 필요 시 Jira 등록. 개인정보/실제 채널 출시 준비는 별도 병행.
2. DTO·인증 주체 resolver·검증·응답 및 OpenAPI 계약 작성.
3. 컬렉션/인덱스·원자 접수·멱등 충돌 처리 구현.
4. 분산 요청 제한과 공개 경로 보안 테스트.
5. 알림 claim/lease/retry 및 선택 공급자 adapter 구현.
6. 제한된 운영 조회/실패 재전송 수단과 로그/메트릭/보관 삭제 구현.
7. 프론트 문의 폼·동일 키 유지·명시적 익명 전환·대체 연락 안내 적용.
8. 격리 테스트→staging 실Mongo→테스트 채널 검증 후 출시. 실제 외부 전송은 별도 승인.

## 6. 부록 — 근거 및 완료 기준

### 6.1 확인된 기존 구현과 제안의 구분

확인된 사실:
- [SecurityConfig](../../src/main/java/web/tosunsaeng/identity/global/config/SecurityConfig.java): 명시된 경로만 permitAll, 나머지 authenticated, JWT Resource Server 사용.
- [BaseResponse](../../src/main/java/web/tosunsaeng/identity/global/response/BaseResponse.java): 기존 응답 envelope 재사용 대상.
- [UserMergedOutbox](../../src/main/java/web/tosunsaeng/identity/domain/auth/domain/entity/UserMergedOutbox.java), [OwnerEventRetryPolicy](../../src/main/java/web/tosunsaeng/identity/domain/auth/ownerevent/application/OwnerEventRetryPolicy.java): 저장 후 비동기 전송/재시도 패턴 참고 대상. 문의를 회원 통합 이벤트에 섞지 않는다.
- [RequestLogContext](../../src/main/java/web/tosunsaeng/identity/global/observability/RequestLogContext.java): 기존 요청 추적 연계 후보. 기존 추적번호의 프론트 노출 계약은 구현 전 확인한다.

서비스 배치·회신·보관·신규 접수 제한·Slack 표시 범위는 승인된 구현 목표다. API 세부 계약·컬렉션·설정·신규 오류는 구현 설계안이며 기존 기능이라고 주장하지 않는다.

### 6.2 예상 변경 범위

- 구현 요청 시 `domain/support/{api,application,domain,infrastructure,dto}` 신규 모듈.
- 정확한 공개 POST 경로만 SecurityConfig에 추가, 기존 보호 API 무변경 회귀 검증.
- 설정 클래스 및 가짜 예시 환경변수, 신규 인덱스 생성/검증 절차.
- 단위/웹/통합 테스트, OpenAPI/프론트 계약/운영 가이드.
- 실제 웹훅 Secret·실제 이메일·사용자 ID·개인정보는 저장소에 기록하지 않는다.

### 6.3 테스트 및 완료 체크리스트

- [ ] MEMBER/GUEST userId 서버 식별, ANONYMOUS null, 임의 body userId 거부.
- [ ] invalid/expired Bearer 401, 폐기/병합/탈퇴 인증 상태별 거절, 익명 자동 강등 없음.
- [ ] 필드 경계값/Unicode/16KiB/알 수 없는 필드/로그 비노출.
- [ ] 같은 키 같은 본문 replay, 다른 본문409, 계정 범위 격리, 익명 응답 개인정보 미노출.
- [ ] 실제 replica set에서 동시 접수 한 건, outbox 실패 rollback, commit 응답 유실 후 동일 키 복구.
- [ ] 다중 작업자 claim 경합, lease 만료, stale worker 완료 CAS 거절, 프로세스 재시작.
- [ ] 2xx/400/401/403/404/408/429/5xx/timeout, backoff/Retry-After/최대 시도 검증.
- [ ] 리다이렉트/임의 URL 차단, 멘션 억제, 공급자 오류에 Secret 출력 없음.
- [ ] 분산 요청 제한 및 프록시 IP 위조 방지, 정상 replay의 과도한 차단 방지.
- [ ] 개인정보 만료/삭제, 관련 delivery/멱등 데이터 정리, 만료 문의 알림 차단.
- [ ] 전체 `./gradlew clean test`, OpenAPI 생성 및 diff 검사. 기본 테스트는 외부 Atlas/Firebase/webhook 미호출.
- [ ] 별도 승인 staging 테스트 채널에서 합성 본문으로 실제 알림 확인. 본문 표시/멘션 억제/개인정보 안내/Slack 별도 삭제 정책 검증. 실제 사용자 문의 전송은 수신 범위 확정 후 진행.

### 6.4 배포·중단 기준

- 접수와 worker 활성화를 별도 설정으로 관리한다. 정확한 환경변수 명칭은 구현 시 계약화한다.
- 인덱스·분산 제한·보관 정책·운영 상세 조회·Secret·알림 채널 준비 후 접수 활성화.
- 신규 접수 중단 시 이미 접수된 기록과 전송 대기열은 보존한다. worker 중단을 접수 실패로 바꾸지 않는다.
- 개인정보/Secret 노출, 중복 문의 급증, 알림 폭주, 지속적 저장 실패 시 해당 기능을 중단하고 기존 인증 API는 유지한다.
- rollback 시 컬렉션 삭제나 사용자 데이터 삭제 금지. 새 상태/인덱스 호환성을 확인한다.

### 6.5 운영/Jira 기록

2026-10-06 사용자 생성안 승인 후 TMI-136 하위 작업 TMI-197 생성 및 parent 재확인 완료. 초기 상태는 해야 할 일. 별도 댓글/상태 변경은 하지 않았다. 후속 수정/댓글/상태 변경은 별도 승인 없이 하지 않는다.
향후 댓글 초안: 문의 API 기본안 승인 및 설계 정리, 변경 파일은 본 계획서와 작업 기록, 테스트는 문서 검사, 남은 위험은 공개 접수 남용·개인정보·실제 운영 채널/보관·삭제 준비다.
