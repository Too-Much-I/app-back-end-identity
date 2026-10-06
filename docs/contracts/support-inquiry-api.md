# 문의 API 및 운영 계약 — TMI-197

## 1. 5줄 결론

1. `POST /api/v1/support/inquiries`가 DB 접수 후 응답하며 Slack은 별도 worker가 보낸다.
2. MEMBER/GUEST는 검증된 인증과 현재 DB 계정으로 식별하고, 비로그인은 userId 없이 접수한다.
3. 동일 요청 키/정규화된 본문은 7일간 같은 접수번호를 반환한다. Slack 전송의 exactly-once는 보장하지 않는다.
4. 문의/알림은 최대90일, 요청 식별 기록은7일 보관한다. TTL 삭제는 비동기다.
5. 기본값은 모두 OFF다. 실제 ECS Secret 연결·Slack 전송·프론트 화면은 이번 구현에 포함되지 않는다.

## 2. 반드시 읽어야 하는 내용

### 프론트 요청

`Content-Type: application/json`, `Idempotency-Key: <문의별 소문자 UUID v4>`가 필요하다. 로그인한 경우에만 Identity Access Token을 Authorization Bearer로 보낸다. Firebase ID Token을 보내지 않는다. 응답 유실/503 재시도는 **동일 키와 내용**을 유지한다. 내용을 변경하면 새 키를 사용한다.

```json
{
  "category": "AUTH",
  "message": "로그인 오류가 반복되어 문의합니다.",
  "replyEmail": "tester@example.com",
  "context": {
    "screen": "login",
    "errorCode": "PROVIDER_CHANGE_CONFLICT",
    "requestId": "synthetic-request",
    "appVersion": "1.0.0",
    "platform": "ANDROID"
  }
}
```

정확한 필드 제한/enum은 [SupportRequest](../../src/main/java/web/tosunsaeng/identity/domain/support/SupportRequest.java) 기준이다. 본문은10~2000자, 전체 요청은16KiB 이하. 선택 필드는 생략 가능하며 `userId` 등 정의되지 않은 필드, 중복 JSON 키, 후속 JSON은 거절한다. 본문/이메일 앞뒤 공백은 제거한다.

신규201, 중복200. 기존 BaseResponse의 `result`는 `{"inquiryId":"<UUID>","status":"RECEIVED"}`다. RECEIVED는 DB 접수이며 Slack 성공/문제 해결을 뜻하지 않는다. 모든 응답은 no-store다.

사용자 안내 예시: “비밀번호, 인증번호, 카드정보 등 민감정보를 입력하지 마세요.” 별도 Slack 전달 문구는 화면에 넣지 않는 승인안이다. 개인정보 처리방침·처리위탁/국외 이전 등 필요한 고지 검토를 면제하는 의미는 아니다.

### 인증과 한계

인증이 없으면 익명, 잘못된 인증이 있으면 인증 오류다. 임의 본문 userId는 받지 않는다. 실제 userId는 현재 활성 DB 계정에서 가져오며 병합·탈퇴·비활성 상태는 기존 인증/계정 오류로 거절한다.

**기존 Access JWT에는 sessionId/폐기 epoch가 없어 개별 로그아웃 세션 폐기를 추가로 확인하지 못한다.** 만료 전 토큰의 세션 단위 즉시 차단은 별도 인증 계약 변경이 필요하다. 이번 변경은 JWT/JWKS/로그인·병합 계약을 바꾸지 않는다.

## 3. 배포 시 결정·확인할 사항

| 환경변수 | 기본값/설명 |
| --- | --- |
| SUPPORT_INQUIRY_ENABLED | false, 접수 활성화 |
| SUPPORT_SLACK_WORKER_ENABLED | false, 전송만 독립 활성화/중지 |
| SUPPORT_INQUIRY_HMAC_KEY | 비어 있음. 별도 Secret, base64 인코딩된 최소32바이트 난수 |
| SUPPORT_SLACK_WEBHOOK_URL | 비어 있음. Secret의 webhookUrl 키를 값으로 주입 |
| SUPPORT_TRUSTED_PROXIES | 비어 있음. 실제 직전 프록시 CIDR만 쉼표로 지정 |

사용자가 등록했다고 보고한 테스트 Secret의 실제 내용/권한은 읽거나 검증하지 않았다. HMAC 키는 webhook과 별개이며 저장소/로그에 기록하지 않는다. 임의 키 교체는 멱등 키와 quota scope를 바꾸므로 7일 중복 방지 구간과 제한 초기화를 고려해 별도 계획한다.

`server.forward-headers-strategy=none`이어야 접수를 켤 수 있다. 실제 socket peer를 기준으로 신뢰한 프록시에서만 X-Forwarded-For를 역순 검증한다. ALB 뒤라면 실제 ALB 프록시 네트워크와 target 직접 접근 차단을 확인한다. 전체 인터넷 CIDR를 신뢰하지 않는다. 기존 forwarded 설정/Swagger HTTPS URL 요구와 충돌하면 켜기 전에 해결해야 한다.

Mongo replica set와 트랜잭션이 필요하다. 기동 시 인덱스 생성 권한, runtime CRUD 권한을 검증한다. 테스트 환경부터 배포하고 접수만 켜 DB 검증 → 합성 문의에 한해 worker 켜 Slack 본문 표시를 확인한다. 롤백은 두 flag를 끄며 컬렉션을 삭제하지 않는다.

## 4. 주요 위험과 미확인 사항

- 실제 Slack·ECS·프록시 환경은 미검증. 전송 성공200의 상태 코드를 기준으로 판단한다.
- 응답 유실/lease 만료/수동 재처리는 Slack 중복을 만들 수 있다. 접수번호로 식별한다.
- 고정 UTC 시간/일 버킷(rolling window 아님): 사용자 또는 익명 IP당5/시간,10/일. 모든 접수 시도에 IP30/분 제한도 적용한다. 공용 NAT 오탐·IP 교체 남용은 가능하다.
- 429의 Retry-After는 보수적으로86400초다. 실제 남은 quota 시간 계산은 하지 않는다.
- 익명에서 로그인으로 주체가 바뀌거나7일 이후 같은 키를 보내면 새 접수가 될 수 있다.
- Slack 복제본은 DB TTL로 삭제되지 않는다. 비공개 채널·최소 접근·별도 보관/삭제 담당자가 필요하다. Incoming Webhook만으로 삭제하지 못한다.
- 운영 알림 연결은 별도다. 낮은 카디널리티의 `identity.support.receipts`, `identity.support.delivery` counter를 제공한다. backlog/가장 오래된 대기 시간/FAILED 건수는 DB 운영 조회로 확인하며 자동 gauge/CloudWatch 경보는 미구성이다. delivery counter는 완료 기록 시도이며 lease 충돌 시 DB 상태가 최종 기준이다.

## 5. 운영 절차

### 저장과 재시도

문의·멱등 기록·outbox·신규 접수 quota는 한 트랜잭션으로 저장한다. DB 실패는 전체 트랜잭션 최대3회 후503이다. 별도 burst quota는 유효하지 않은 본문에도 소비된다. 실패 로그에 본문/이메일/토큰/DB 예외 원문을 출력하지 않는다. 인프라 HTTP/DB debug logging도 활성화하지 않는다.

worker는5초마다1건 claim, lease60초, 외부 요청10초/connect3초. 최대5회 전송, 재시도 간격30/120/600/1800초+0~15초 jitter. 408/429/5xx 및 통신 오류를 재시도하며 429 Retry-After를 존중한다. 1일 초과 대기나 만료는 FAILED. 성공SENT, 대기RETRY_WAIT, 영구실패FAILED. outbox에는 본문을 복제하지 않는다.

### 제한된 수동 복구와 삭제

[SupportOperations](../../src/main/java/web/tosunsaeng/identity/domain/support/SupportOperations.java)는 공개 endpoint/자동 bean이 아닌 내부 운영 primitive다. 운영자는 승인된 별도 runner에서 정확한 DB·문의 UUID·승인 이슈 키를 확인한 뒤 호출한다. 배포용 CLI/관리 UI는 제공하지 않는다. 직접 임의 DB 수정은 피한다.

- `replayFailed(id, expectedAttempts, approvalReference, now)`: 만료되지 않은 본문, FAILED, 정확한 시도 횟수, 아직 수동 재시도하지 않은 건만 CAS로 허용. 문의당 수동 재시도1회(새로운 최대5회 묶음). 트랜잭션 내 감사 기록을 함께 남긴다. SENT는 재전송하지 않는다.
- `deleteInquiry(id, approvalReference, now)`: 모든 worker를 중지하고 진행 HTTP 종료를 먼저 확인. SENDING 상태는 거절한다. 본문·delivery·관련 멱등 기록을 트랜잭션으로 삭제하고 본문 없는 감사 기록을 남긴다. quota는 개인식별 HMAC뿐이며 보관 만료를 따른다. 중단된 SENDING은 임의 해제하지 말고 별도 승인된 lease 복구를 진행한다.
- 감사 기록에는 접수번호/작업/승인 이슈 키/시각만 저장하며90일 TTL. 운영자 신원은 접근 감사와 승인 이슈로 추적한다. 티켓에 실제 개인정보를 복사하지 않는다.
- 삭제는 이미 전송된 메시지나 네트워크에 나간 요청을 회수하지 못한다. Slack 관리자 삭제/보관 규칙을 별도로 수행한다. 만료 데이터는 운영 조회에서도 expiresAt을 필터링한다.

## 6. 부록 — 오류와 검증 근거

| HTTP | 코드 |
| --- | --- |
| 400 | INVALID_REQUEST, INVALID_SUPPORT_REQUEST_ID |
| 409 | SUPPORT_INQUIRY_REQUEST_CONFLICT |
| 413 | SUPPORT_INQUIRY_TOO_LARGE |
| 429 | SUPPORT_INQUIRY_RATE_LIMITED |
| 503 | SUPPORT_INQUIRY_UNAVAILABLE |

기존 인증401/계정403·404 등의 오류 및 미지원 Content-Type415 등 프레임워크 오류는 기존 계약을 따른다. 이메일 자동 발송·첨부·문의 조회 공개 API·사용자 계정 자동 복구는 없다.

구현: [support 모듈](../../src/main/java/web/tosunsaeng/identity/domain/support). 테스트: [support 테스트](../../src/test/java/web/tosunsaeng/identity/domain/support). 실제 Mongo 트랜잭션 테스트는 `SUPPORT_TEST_LOCAL_MONGO=true`로 opt-in하며 localhost27029의 별도 replica set `support-test`와 무작위 합성 DB만 사용한다. 실제 Atlas/OAuth/Slack을 테스트에서 호출하지 않는다.
