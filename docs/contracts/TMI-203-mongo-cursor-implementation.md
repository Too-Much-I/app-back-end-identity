# TMI-203 Mongo 조회·원천 삭제 증거·서명 cursor 구현 결과

- 날짜: 2026-10-12 / 브랜치: `feat/TMI-203-withdrawal-snapshot-feed-recovery`
- 상태: 부분 구현. 실제 HTTP API·durable snapshot worker·운영 활성화 완료 보고가 아니다.

## 1. 5줄 결론

1. `MongoRecoveryReader`는 Mongo snapshot session으로 같은 T의 source H와 WITHDRAWN User를 읽으며 live-read fallback이 없다.
2. `RecoveryCursor`는 전용 key ring의 HMAC-SHA256, 환경/소비자/용도/stream/run/범위 및 1시간 만료를 검증한다.
3. `RecoveryFeedService`는 조회·cursor·페이지 검증·exact 응답을 연결하고 이어받을 때 기존 H2와 cursor 만료를 유지한다.
4. `MongoSourceExpiryEvidence`는 호출자의 Mongo transaction 안에서 source version CAS와 선택 range 갱신을 수행하며 원본 삭제 건수 불일치도 rollback한다.
5. 전체 1,960개 중 1,954 통과/기존 6 skip/실패 0. 신규 37개에는 실제 로컬 Mongo replica set 검증 3개가 포함된다. 운영 Mongo/IAM/복원 검증은 아니다.

## 2. 사용자가 반드시 읽어야 하는 내용

새 클래스는 Bean/controller/scheduler로 자동 등록하지 않는다. 기존 플래그·보안 설정·외부 API를 열지 않았다. 전용 key ring 및 clock/source/cleanup 권한 근거는 명시적으로 주입해야 하며 허용 기본값이 없다.

snapshot reader가 생성하는 row는 builder 전용 임시 자료다. reader 성공만으로 READY가 되는 것이 아니다. durable run/lease, 불변 row 저장, source/run fence 기반 READY commit과 7일/120일 정리를 구현해야 공개할 수 있다.

Billing 수락은 recovery-r1/scalar-end-1 보완 자료 범위로 유지한다. 기존 4개 API shape·공유 fixture/hash·기존199/push/LC/token/보존 계약은 변경하지 않았다. snapshot은 T에 존재하는 WITHDRAWN 자료이며 이미 사라진 과거 tombstone 복원을 보장하지 않는다.

## 3. 사용자가 결정해야 하는 사항

새 제품 정책 결정은 없다. 배포 전에는 전용 cursor key ring, 인증된 서비스 주체 전달 경로 및 직접 ingress 차단, clock/capture evidence 연결이 필요하다. 이번 작업은 해당 Secret 생성이나 IAM/네트워크 설정 변경 승인이 아니다.

## 4. 주요 위험과 미확인 사항

- durable snapshot run/idempotency/lease/builder/READY seal/retention worker와 4개 HTTP route는 여전히 미구현이다.
- 새로운 reader는 MongoDB 5+ snapshot session이 필요하다. 실제 사용한 로컬 image는 mongo:7.0이며 운영 버전/history window/부하/query plan 검증을 대체하지 않는다.
- snapshot reader의 실패는 일반 `Unavailable`로 닫는다. worker에서 history loss/build timeout/build failure의 exact 상태 분류와 재시작 처리를 추가해야 한다.
- range 정규화 계산·expiry adapter는 있지만 잔여 range sweep scheduler, bounded conflict 재시도·unknown commit reconciliation·retired metadata 정리는 아직 없다. 현재 예외를 전파하여 삭제를 중단하며 무한 재시도하지 않는다.
- schema/index 선언과 실제 startup index·명시적 migration 검증 연결은 남아 있다. missing/null/int32/음수 availabilityVersion을 0으로 간주하지 않는다.
- 실제 source restore, 여러 worker/stream 교체/cleanup/claim의 동시성 전체 조합, Billing 공동 연동, IAM은 미검증이다. Mongo 3개 probe를 이 완료 조건 전체로 확대하지 않는다.

## 5. 변경 동작과 다음 작업

| 파일 | 구현 동작 |
| --- | --- |
| [RecoveryCursor](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalrecovery/RecoveryCursor.java) | opaque r1 cursor, key rotation, canonical base64url, 안전한 MAC 비교, 2048자 제한, 1시간 TTL, owner/route/stream/run/범위 바인딩, 비로깅용 toString |
| [MongoRecoveryReader](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalrecovery/MongoRecoveryReader.java) | source/user 동일 snapshot session, T/H/count/digest, stream 단일성·BSON int64 검증, feed의 순번별 containment 및 journal 조회, query/clock 실패 시 fail-closed |
| [MongoSourceExpiryEvidence](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalrecovery/MongoSourceExpiryEvidence.java) | 같은 Mongo transaction 참여 확인, 환경/stream/status/version/head/floor CAS, exact seed 선조회, 최대99개 선택 원본의 조건부 삭제와 대체 range 저장 |
| [RecoveryFeedService](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalrecovery/RecoveryFeedService.java) | cursor 검증 후 조회, 고정H2, 전체page 오류, 410/409 details와503 생략, 16KiB serialization, 원래 cursor TTL 유지 |
| [WithdrawalSourceCleanup](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalcapture/WithdrawalSourceCleanup.java) | journal 삭제가 정확히1건이 아니면 예외를 던져 증거·delivery 삭제 함께 rollback |

후속 구현 순서: durable snapshot 저장·빌더/만료 → source sweep/reconciliation/index preflight → 인증된 ingress와 strict HTTP adapter → Billing 공동 연동/복원 및 활성화 검증. 신뢰되지 않은 consumer 헤더나 사용자 JWT로 인증 빈자리를 대신하지 않는다.

## 6. 부록 — 테스트·범위·Jira 댓글 초안

- 신규 `RecoveryCursorTests` 13, `MongoRecoveryReaderTests` 8, `MongoSourceExpiryEvidenceTests` 6, `RecoveryFeedServiceTests` 6, `WithdrawalOperationsTests` 추가1, `RecoveryReplicaSetTests` 3 = 37개.
- 최종 `env -u SENTRY_AUTH_TOKEN TMI203_LOCAL_MONGO=true ./gradlew clean test`: 25초 성공. 전체1,960 / 통과1,954 / skip6 / 실패·오류0. 기존 support replica set6개는 해당 opt-in을 켜지 않아 skip했다.
- replica set probe: (1) 41행/3page 조회 중 늦은 transaction commit이 발생해도 H=1/41행 고정, 이후live에는 H=2/42행 확인 (2) 증거 기록 후 강제 실패 rollback과 정상 삭제/floor commit (3) 101번째 구간 point 조회 성공.
- opt-in 테스트는 고정 localhost와 무작위 `tmi203_probe_` 합성 DB만 사용한다. 테스트 종료 시 해당 DB를 삭제하고 이번에 만든 `--rm` 컨테이너도 종료했다. 실제 Atlas·OAuth·Billing·AWS 호출 없음.
- 기존 dirty 변경은 보존했다. 기존 cleanup Javadoc 수정은 이전 작업이며 이번에는 삭제 건수 검증만 추가했다. 새 의존성/공유 wire 변경 없음. 예상 밖 파일 변경 없음. commit/push/Jira 댓글·상태변경/배포 없음.
- Jira 댓글 초안(미등록): TMI-203 Mongo snapshot/feed reader·전용 HMAC cursor·source expiry evidence·feed application 구현. 신규37개 포함1960개 전체실행(1954통과/6skip),로컬 replica set3개통과. durable snapshot lifecycle/HTTP/IAM/source sweep 및복원 공동 검증은 미완료. 기존계약·OFF 경계유지.
