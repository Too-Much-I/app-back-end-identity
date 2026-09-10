# TMI-130 — 재발급 응답 복구 연동·운영 가이드

## 1. 5줄 결론

1. `AUTH_REISSUE_RECOVERY_ENABLED` 기본값은 `false`이며 이번 구현은 운영 활성화가 아니다.
2. ON에서는 같은 원 Refresh Token과 같은 `Idempotency-Key`에 최초 결과를 최대 2분간 반환한다. 새 결과 발급·만료 연장은 없다.
3. 회전 증거·child hash 세션·암호문을 동일 Mongo Transaction에 저장하고 replay도 child/control에 CAS 쓰기를 수행한다.
4. 별도 AES-256-GCM 키는 읽기 전용 파일 mount에서 시작 시 로드한다. AWS 리소스 생성·SDK secret 조회·자동 키 reload는 구현하지 않았다.
5. 실제 replica-set·키 교체/백업·모바일 장애 검증이 끝나기 전 기능을 켜지 않는다. 아래 테스트 표에서 자동 테스트와 운영 증빙을 구분한다.

근거: [서비스](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/ReissueRecoveryService.java), [설정](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/infrastructure/ReissueRecoveryConfiguration.java), [계획](refresh-token-response-recovery-stage-9-plan.md).

## 2. 반드시 읽어야 하는 내용 — 프론트 계약

### 요청 및 응답

- URL: 기존 `POST /api/v1/auth/reissue`, body는 기존 `refreshToken` 하나다.
- ON에서는 `Idempotency-Key` 헤더가 정확히 한 개 필요하다. 소문자 canonical UUID v4, 길이 36. 여러 값·쉼표 결합·대문자·다른 UUID 버전·빈 값은 거절한다.
- 인증정보·사용자 식별자를 ID 생성에 사용하지 않는다. 논리 재발급 작업마다 CSPRNG UUID를 생성하며, 응답 유실 재시도와 앱 재시작에는 같은 ID/원 credential을 재사용한다.
- 이 엔드포인트는 Refresh credential로 인증한다. 만료된 Access Token을 공통 interceptor가 자동 첨부하지 않도록 한다. 로그인용 사용자 JWT 및 workload JWT 구조는 바뀌지 않는다.
- 성공 JSON은 기존 `BaseResponse<ReissueResponse>`다. 최초/재전달의 `accessToken`, `refreshToken`, `grantType`, 두 `ExpiresIn`(밀리초) 모두 동일하다.
- 두 응답 헤더 `Reissue-Access-Expires-At`, `Reissue-Refresh-Expires-At`은 실제 UTC ISO-8601 절대 만료 시각이다. 최초 duration을 재시도 수신 시각에 다시 더하지 않는다.
- JWT 실제 `exp`와 Mongo에 저장되는 밀리초 정밀도에 맞춘 만료를 사용한다. 복구 기한은 서버 논리 회전 시각에서 고정되며 원 credential 만료로 더 짧아질 수 있다.
- 성공·오류 응답은 `Cache-Control: no-store`, `Pragma: no-cache`. 웹 클라이언트가 추가되면 exact origin별 요청 헤더 허용 및 두 응답 헤더 노출을 별도로 검증한다. 이번 변경으로 wildcard CORS를 추가하지 않았다.

### 앱 구현 순서

1. 계정별 재발급을 한 작업으로 합친다(single-flight). 모든 401을 무조건 재발급 대상으로 취급하지 않는다.
2. 네트워크 전송 전에 계정/로컬 세션 세대·원 credential 참조·요청 ID·시작 시각을 안전하게 함께 저장한다.
3. 성공 시 두 credential·절대 만료·pending 제거를 원자적 또는 crash 복구 가능한 방식으로 저장한다.
4. timeout/connection/429/503만 같은 ID로 제한 재시도한다. 권장 1/2/4/8초 backoff, 이후 최대 10초+jitter, 최대 5회·로컬 총 2분 이내. 서버 기한이 우선이다.
5. 로그아웃·계정 전환·새 로그인 후에는 이전 요청의 늦은 성공을 저장하지 않는다. source logout은 해당 회전의 exact direct child와 응답 복구를 취소한다.
6. 최신 credential을 이미 다른 로컬 저장 위치에서 받았는지 확인하고, 오래된 결과로 덮어쓰지 않는다.

| HTTP / code | 앱 대응 |
| --- | --- |
| 400 `INVALID_REISSUE_REQUEST_ID` | 계약 오류. ID를 매번 바꿔 반복하지 않는다 |
| 409 `REISSUE_REQUEST_CONFLICT` | 다른 회전에서 사용한 ID. pending/계정 저장 상태 확인 |
| 409 `REISSUE_RECOVERY_EXPIRED` | 최초 결과 복구 만료. MEMBER는 정상 재인증, Guest는 기존 증명·정보 보존과 복구 한계 안내 |
| 409 `REISSUE_RESULT_SUPERSEDED` | 후속 회전 또는 계정 유형 변경. 로컬 최신 결과 확인 |
| 401 `SESSION_LOGGED_OUT` | 폐기·복구 취소·epoch/Firebase 경계 위반. 자동 exchange/reissue 루프 금지 |
| 401 `ACCOUNT_WITHDRAWN` 등 기존 계정 오류 | 기존 탈퇴/병합 안내 우선 |
| 401 `REFRESH_TOKEN_REUSE_DETECTED` | 유효 rotated credential의 다른 ID 재사용. 기존 전체 활성 세션 폐기 정책 |
| 401 `REFRESH_TOKEN_EXPIRED` / `INVALID_REFRESH_TOKEN` | 원 credential 만료/부재. 자동 새 Guest 생성 금지 |
| 503 `SESSION_SECURITY_UNAVAILABLE` | DB/키/암호문/commit 불명·충돌 미해결·maintenance. 같은 ID로 제한 재시도 |

현재 Guest-only 앱의 업데이트 후 SNS 유도와 위 장애 처리는 별개다. 기존 Guest 인증으로 승격/병합할 수 있는지 확인해야 하며 SNS만으로 과거 Guest 소유권을 인정하거나 기록 이전을 약속하지 않는다. 강제 회원 전환·Guest 폐지는 이번 범위가 아니다.

## 3. 운영 담당자가 확정·준비할 사항

### 설정과 키 공급

| 환경변수 | 기본 / 제약 |
| --- | --- |
| `AUTH_REISSUE_RECOVERY_ENABLED` | false |
| `AUTH_REISSUE_RECOVERY_MAINTENANCE` | false, ON이면 새 회전/replay 503; 단일 logout은 허용 |
| `AUTH_REISSUE_RECOVERY_WINDOW` | PT2M, 0 초과·2분 이하·밀리초 정밀도 |
| `AUTH_REISSUE_RESPONSE_MAX_BYTES` | 16384, 1024~16384 |
| `AUTH_REISSUE_CONCURRENCY_ATTEMPTS` | 3, 1~3 |
| `AUTH_REISSUE_ENCRYPTION_ACTIVE_KEY_ID` | 필수, 영문/숫자/underscore/hyphen 1~64자 |
| `AUTH_REISSUE_ENCRYPTION_KEYRING_LOCATION` | 필수, 읽기 전용 secret mount의 절대 파일 경로 |
| `AUTH_REISSUE_ENCRYPTION_ENVIRONMENT` | 필수, 환경별 구분자, 영문/숫자/underscore/hyphen 1~64자 |
| `AUTH_SESSION_FENCE_ENABLED` | Stage 9 ON의 필수 선행, true 필요 |

키 파일 형식은 Java properties의 `key-version-id=Base64로 표현한 32바이트 AES 키`다. 실제 값은 저장소·Jira·로그에 작성하지 않는다. 최대 8개 키/16 KiB 파일이며 중복 ID·잘못된 키 길이·활성 키 부재·상대 경로는 시작 실패한다. 사용하지 않는 구 키도 올바른 형식이어야 한다.

- secret store(예: Secrets Manager)와 최소 IAM/task role 및 read-only mount 제공은 인프라 책임이다. AWS IAM Secret Access Key를 AES 키로 사용하지 않는다.
- 키 파일 소유권·프로세스 읽기 권한·이미지/백업/로그 제외·환경별 키 분리를 확인한다. 애플리케이션에는 네트워크 secret fetch/reload가 없다.
- 모든 instance에 신·구 decrypt 키 배포 → 활성 encrypt 키 변경 rolling restart → 구 키로 암호화한 응답 기한과 진행 요청 소진 → 구 키 제거 rolling restart 순서다.
- cryptoperiod/키별 최대 암호화량·백업/secret version 보존·사고 시 폐기 담당자와 기준을 운영 전에 결정한다. 2분 만료가 백업 암호문 삭제를 뜻하지 않는다.

### Mongo와 배포

- 기존 manager와 동일 databaseFactory를 사용하는 전용 manager: primary read preference, snapshot read concern, majority write concern, maxCommitTime 5초. TransactionTemplate은 REQUIRES_NEW·timeout 10초다.
- 실제 replica set 지원, majority commit 응답 유실, rollback, write conflict, failover, 처리시간·동시성·보안 폐기 대상 세션 수 상한을 staging에서 검증한다.
- `refresh_sessions`: 기존 TTL/hash 인덱스 유지. 신규 partial unique `(userId, rotationRequestKeyHash)`의 predicate는 `rotationRequestKeyHash` BSON string이다. 기존 누락 문서 여러 건이 공존하는지 실제 Mongo에서 확인한다.
- `refresh_reissue_responses`: `_id=responseId`, `sourceSessionId` unique, `cleanupAt` TTL 0초. 자동 index 생성 설정을 껐다면 배포 migration으로 먼저 생성·검증한다.
- Consumer 변경이나 Billing/Learning Core 배포는 이번 코드 변경에 포함하지 않는다. 기존 통합 gate는 유지한다.

## 4. 위험·장애 대응

- 구 코드 writer와 신규 recovery writer를 혼재 활성화하지 않는다. 기존 코드가 정상 재시도를 공격으로 분류할 수 있다.
- 긴급 중단은 **recovery enabled 유지 + maintenance true**로 재발급을 503 제한한다. 키/fence/회전 증거는 유지하고 구형 코드로 rollback하지 않는다.
- 새 코드가 OFF로 잘못 배포되어도 recovery metadata가 있는 source를 legacy reuse로 처리하지 않는 방어를 추가했다. 이것이 이전 버전 rollback 안전성을 보장하지는 않는다.
- commit 결과 불명은 새 결과를 생성하지 않고 replay-only 재조회한다. 결과를 확정하지 못하면 503. 동일 요청을 안전하게 다시 제출한다.
- key unknown/tag 오류/기한 전 payload 부재는 503, 평문 fallback·새 결과 교체 금지. 키와 암호문을 함께 출력하지 않는다.
- TTL 삭제 지연·백업/메모리 사본은 존재할 수 있다. 애플리케이션은 기한을 직접 검사한다.
- `auth.reissue.recovery{outcome=...}` counter: first_issue, replayed, recovery_expired, superseded, id_conflict, suspected_reuse, crypto_failure, transaction_retry, transaction_unresolved. 사용자·원 credential·요청 ID·key ID label 없음.
- crypto_failure/기한 전 payload 부재 즉시 경보, 503/만료 비율·TTL cleanup lag·키별 잔존량·ingress rate limit은 운영 대시보드/배포 설정에서 검증한다. 이번에 외부 경보나 rate-limit 인프라를 생성하지 않았다.
- 정상 회전/replay metric은 commit 성공 이후 집계한다. commit 응답 불명 후 재조회는 replay로 집계될 수 있어 정확한 최초 회전 수와 같지 않다.

## 5. 구현 근거와 선후 관계

- [ReissueRecoveryService](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/ReissueRecoveryService.java): 원자 회전/replay, bounded conflict reread, unknown commit replay-only, source/child logout.
- [RefreshSession](../../src/main/java/web/tosunsaeng/identity/domain/auth/domain/entity/RefreshSession.java): 원 세션 TTL까지 요청/회전 증거 유지, recoveryDisabledAt, child CAS touch.
- [RefreshReissueResponse](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/domain/RefreshReissueResponse.java): 비밀정보 없는 메타데이터와 인증된 암호문, 별도 TTL.
- [AES-GCM adapter](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/infrastructure/AesGcmReissueResponseCipher.java), [키 mount adapter](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/infrastructure/MountedReissueEncryptionKeys.java).
- [AuthController](../../src/main/java/web/tosunsaeng/identity/domain/auth/common/api/AuthController.java): 헤더/응답. [ReissueNoStoreFilter](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/infrastructure/ReissueNoStoreFilter.java): controller 이전 오류에도 cache 금지.

replay/단일 logout/회전은 child version 충돌로 선후 관계를 확보한다. logout-all/withdrawal/upgrade/merge는 기존 공통 user control 및 계정 검사를 재사용한다. commit 뒤 네트워크 전송 중인 성공 응답을 완전히 취소할 수는 없으므로 앱의 로컬 세대 검사와 서버 후속 세션 검증을 함께 사용한다.

## 6. 부록 — 테스트 및 운영 증빙 매핑

자동 테스트 파일:

- [ServiceTests](../../src/test/java/web/tosunsaeng/identity/domain/auth/session/application/ReissueRecoveryServiceTests.java): repository/TransactionTemplate mock, 실패 시 테스트용 snapshot rollback. 실제 Mongo rollback 증거 아님.
- [CipherTests](../../src/test/java/web/tosunsaeng/identity/domain/auth/session/infrastructure/ReissueCipherTests.java): 실제 JCE AEAD/임시 가짜 키 mount 검증.
- [ConfigurationTests](../../src/test/java/web/tosunsaeng/identity/domain/auth/session/infrastructure/ReissueRecoveryConfigurationTests.java): 조건부 구성·키/fence 필수·기본 OFF.
- [HttpTests](../../src/test/java/web/tosunsaeng/identity/domain/auth/common/api/ReissueRecoveryHttpTests.java): standalone MVC·기존 JSON/헤더/오류.
- [RepositoryTests](../../src/test/java/web/tosunsaeng/identity/domain/auth/session/repository/ReissueRecoveryRepositoryTests.java): 인메모리 Mongo mapping/unique/CAS, 실제 Transaction 아님.

| 계획 ID | 이번 자동 검증 / 남는 증빙 |
| --- | --- |
| T01 | Service/HTTP: UUID 형식·헤더 multiplicity·누락·body/URL |
| T02 | Service + 기존 JWT/발급 회귀: DB 유형·proof 전파 |
| T03 | Service: 최초/replay 동일, issuer/generator 한 번 |
| T04 | Service 다른 source 충돌·ID 단독 거절, Repository 사용자별 unique |
| T05 | Service 충돌 후 승자 재조회·단일 child, Repository CAS. 실제 다중 instance 동시 요청은 staging |
| T06 | Service 다른 ID 재사용·폐기 commit, 실제 서로 다른 ID 동시 경쟁은 staging |
| T07 | Service encryption/source/child/payload 실패 snapshot rollback. 실제 rollback은 T27 |
| T08 | Service commit 성공 후 불명·증거 없음·새 발급 금지. 실제 driver/failover는 T27 |
| T09 | Service 119/120/121초·지연 commit·재시도 불변·sub-ms clock. 앱 재시작은 T25 |
| T10 | Service 원 만료 및 고정 기한 검사, 원 만료로 기간 단축 |
| T11 | Service 기한 후 payload 삭제·다른 세션 보존, Repository 증거 TTL 분리 |
| T12 | Service payload 부재·키/저장 실패, Cipher 변조·unknown key |
| T13 | Cipher roundtrip·각 AAD/행/환경 치환·nonce·신구 키 |
| T14 | Cipher DB 직렬화 원문 없음·redaction·safe exception, HTTP 오류 비노출. 실제 ingress/profiler 로그 점검은 운영 |
| T15 | HTTP 절대 만료·duration·no-store, Service 결과 불변 |
| T16 | Service child 선행 회전·superseded, Repository rotation winner CAS. 실제 barrier 경합은 T27 |
| T17 | Service child logout 이후 replay 거절, Repository logout winner CAS |
| T18 | Service source logout·멱등·exact child·타 family/descendant 보존 |
| T19 | Service epoch/confirmed Firebase 경계 거절 + 기존 SessionRevocationTests. 실제 통합 경합은 T27 |
| T20 | Service withdrawal/type 변경 + 기존 Guest upgrade/merge 및 세션 보안 회귀. 실제 통합 경합은 T27 |
| T21 | Service expired source/old epoch/cancelled recovery가 최신 세션 폐기하지 않음 |
| T22 | Repository partial index 정의/predicate·대상 행 unique. 실제 legacy 다건 공존은 staging |
| T23 | Service 정상 만료·superseded·reuse 분리, Guest 초기화 미추가. 모바일 UX는 T26 |
| T24 | Configuration 기본 OFF·fence/키 없음·잘못된 설정 실패 |
| T25 | 모바일 single-flight·crash·계정 전환·늦은 응답 저장 방지: 프론트 실제 검증 대기 |
| T26 | HTTP 오류 계약 자동 검증. 모바일 bounded retry/안내 실제 검증 대기 |
| T27 | 실제 replica-set rollback/commit 불명/경합/성능 검증 대기 |
| T28 | Cipher 가짜 mount 신구 키 자동 테스트. 실제 키 교체·백업 복구·TTL·비상중단 리허설 대기 |
| T29 | 전체 Gradle 회귀. 신규 앱과 서비스 통합은 staging |
| T30 | 원 credential+ID 동시 탈취 한계 및 고정-label metrics 문서화. 실제 ingress rate limit/경보 검증 대기 |

`mongo-java-server 1.47.0`은 partialFilterExpression을 실제 unique 검사에 적용하지 않는다. 그래서 신규 인덱스 정의/predicate와 대상 행 uniqueness는 따로 검증하고, 기존 Stage 8 인메모리 fixture에서는 해당 인덱스만 설치하지 않는다. 프로덕션 인덱스 조건을 테스트 대역에 맞춰 약화하지 않았다. 실제 legacy 다건 공존을 입증한 것으로 보고하지 않는다.

운영 증빙이 필요한 항목을 mock 성공으로 체크하지 않는다. 코드 병합 후에도 기능 OFF를 유지하고 별도 승인된 환경에서 위 대기 항목을 완료한다.
