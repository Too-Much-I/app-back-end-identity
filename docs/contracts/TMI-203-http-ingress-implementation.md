# TMI-203 복구 HTTP adapter·인증 경계 구현

## 1. 5줄 결론

1. 합의된 네 POST 경로를 strict codec과 snapshot/feed application service에 연결했다.
2. 별도 우선순위 security chain을 항상 설치하여 일반 사용자 JWT와 분리했다. 기본 OFF는 403이다.
3. 명시 `app.withdrawal-recovery.http-enabled=true`에는 인증 검증기와 두 application service bean이 필요하며, 없으면 기동 실패한다.
4. 실제 IAM 검증기는 아직 없다. 인터페이스/호출 순서/실패 차단을 구현한 것이며 AWS 인증 완료가 아니다.
5. runtime Mongo/index/키 구성, 스케줄·경보, 실제 ingress/IAM와 Billing 복원 검증이 남아 있다. 자동 활성화/배포는 하지 않았다.

## 2. 사용자가 반드시 읽어야 하는 내용

`RecoveryIngressVerifier`는 신뢰 ingress에서 환경·서비스 주체·route·서명 헤더·시간을 검증한 뒤 bounded raw body의 서명까지 확인해야 하는 명시적인 확장 지점이다. 사용자 JWT나 전달받은 consumer 헤더를 신뢰하는 구현은 제공하지 않는다. payload는 서명 확인 후 JSON으로 해석한다. 실제 검증기 구현 없이 테스트용 mock을 운영 bean으로 등록하면 안 된다.

HTTP filter는 security chain 안에서만 생성되며 일반 servlet filter로 중복 등록하지 않는다. global Jackson/public BaseResponse를 변경하지 않는다. 기존 JWT 로그인 API 설정은 그대로 유지한다.

## 3. 사용자가 결정해야 하는 사항

새 제품 정책 결정은 없다. 활성화 전 실제 배포 ingress의 SigV4 검증 방식, 검증된 주체의 환경/consumer 매핑, 직접 서버 접근 차단 근거를 확인해야 한다. IAM/네트워크 설정 변경과 배포는 이번 범위에서 실행하지 않았다.

## 4. 주요 위험과 미확인 사항

- 현재 OFF이므로 배포만으로 복구 호출이 성공하지 않는다. 활성화는 실제 검증기와 runtime 서비스 연결 이후다.
- mock 보안 테스트는 실제 SigV4 replay/time/payload 검증·AWS 정책·직접 ingress 차단을 증명하지 않는다.
- bounded read는 요청 크기를 제한하며 slow client 시간 제한/속도 제한은 배포 ingress/container 확인이 필요하다.
- application service/Mongo/index preflight의 운영 wiring과 snapshot/source maintenance scheduler, 보존 적체 경보가 남아 있다.
- HTTP filter 기반이라 Spring MVC 자동 OpenAPI 탐색 대상이 아니다. 내부 wire 명세는 기존 exact/공유 schema가 기준이며 공개 Swagger에 사용자 JWT 호출 endpoint로 추가하지 않았다. Swagger ZIP은 생성하지 않는다.

## 5. HTTP 계약과 처리 순서

모든 경로 prefix: `/internal/v1/billing/withdrawals`. POST/UTF-8 `application/json`, request/response 각 16,384 bytes 이하. [기존 exact](TMI-203-recovery-r1-exact.md)와 공유 schema/fixture는 변경하지 않는다.

| 경로 | 요청 | 처리 |
| --- | --- | --- |
| `/snapshots` | streamId, 단일 Idempotency-Key | create, BUILDING 202/Retry-After 5 |
| `/snapshots/status` | snapshotId | status |
| `/snapshots/page` | snapshotId, cursor | page |
| `/feed/page` | streamId, afterSequence, scanCursor | feed |

인증 헤더 검증 → Content-Type/Idempotency-Key cardinality·query/encoding 검사 → bounded body read → payload 서명/검증 주체 반환 → strict JSON decode → application service → 전체 직렬화/크기 검사 후 응답. create 이외 Idempotency-Key, query와 Content-Encoding은 거절한다. 서비스 오류 응답은 그대로 보존하고, 예상 밖 runtime 예외는 고정 SOURCE_UNAVAILABLE로 변환한다. 인증 실패는 body 비강제 403. 모든 경로 no-store, 로그에 본문/서명 헤더/예외 원문을 추가하지 않는다.

## 6. 변경 파일·검증 근거

- 신규 `RecoveryIngressVerifier.java`, `RecoveryHttpFilter.java`, `RecoveryHttpConfiguration.java` (withdrawalrecovery package).
- 신규 `RecoveryHttpFilterTests.java`, `RecoveryHttpConfigurationTests.java`; 기존 `global/config/SecurityIntegrationTests.java`에 OFF 상태 사용자 JWT 거절 회귀 추가.
- 본 문서 및 WORKLOG/CURRENT_STATE.
- 테스트: 네 route dispatch, verified owner, 인증/서명 실패, 중복 헤더, UTF-8/duplicate JSON, 16KiB 경계, response 상한, 오류 비노출, enabled/off/missing dependency 실제 Spring security chain. 외부 서비스·실 Mongo 실행은 이번에 하지 않는다. 최종 전체 결과는 WORKLOG에 기록한다.

유지: recovery-r1/scalar-end-1 shape/hash,199/202push/LC/JWT/보존/원격ACK없음. 기존 dirty 보존·예상밖변경 없음. commit/push/배포/Jira 변경 없음.

Jira 댓글 초안(미등록): strict 복구 HTTP adapter와 별도 fail-closed security chain 구현. 사용자JWT 격리·raw byte상한·검증기누락기동실패·서비스연결 테스트. 실제 IAM/runtime wiring/스케줄·경보/공동복원은 미완료. 변경파일/검증결과/위험은 본 문서와 WORKLOG 참조.
