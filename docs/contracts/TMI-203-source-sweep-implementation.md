# TMI-203 source sweep·index preflight 구현 결과

## 1. 5줄 결론

1. `MongoSourceSweep`에 최대 99개 신규 range와 exact carry 1개의 Mongo 트랜잭션 정리를 구현했다.
2. 원본 range 변경·source version CAS·진행 기록은 함께 commit하며 실제 journal/delivery는 삭제하지 않는다.
3. `RecoveryIndexes`는 수락 manifest의 16개 index와 `_id_`, 명시 source 이관 필드를 읽기 전용으로 검증한다.
4. 전체 2,000개 중 1,994개 통과, 기존 opt-in 6개 skip, 실패·오류 0이다. 이번 신규 14개 중 실제 로컬 Mongo 테스트 2개를 포함한다.
5. HTTP/IAM·운영 bean/scheduler·보존 적체 경보·공동 복원 검증은 남아 있다. TMI-203 전체 완료나 배포 결과가 아니다.

## 2. 사용자가 반드시 읽어야 하는 내용

이번 변경은 호출 가능한 저장 계층 구현이다. 자동 등록/기동/DDL/운영 데이터 이관을 하지 않는다. 기존 capture의 cutover가 만드는 source에는 새 이관 필드가 없을 수 있으므로 활성화 전에 명시 이관해야 한다. 누락 값을 0으로 간주하지 않는다.

저장소의 AWS 서명 전송 구현은 수신 서비스 인증 구현과 다르다. 기존 사용자 JWT 또는 임의 헤더를 신뢰하는 대체 인증을 추가하지 않았다. 실제 신뢰 ingress와 IAM 인증 주체 연결이 없으므로 복구 HTTP 기능은 아직 제공하지 않는다.

## 3. 사용자가 결정해야 하는 사항

새 제품 정책 결정은 없다. API 활성화 전에 배포망의 신뢰 ingress/IAM 검증 경계와 직접 접근 차단을 확인해야 한다. 이관·index DDL·배포는 별도 작업이며 이번에 실행하지 않았다.

## 4. 주요 위험과 미확인 사항

- 실제 primary failover, unknown commit의 운영 재조정, source 복원과 Billing worker 경합은 아직 검증하지 않았다.
- 알려진 transaction abort만 최대 3회 재시도한다. unknown commit 결과는 실패로 전파하며 같은 작업을 자동 재실행하지 않는다. 트랜잭션당 timeout은 30초이며 재시도 시작 여부의 30초 검사와 별개다. 전체 호출의 엄격한 30초 deadline 보장은 아니다.
- MongoSourceSweep에는 명시 Authority가 필요하다. 운영 권한 구현이나 스케줄러에 자동 연결하지 않았다.
- 단위 테스트의 transaction rollback 호출 검증은 실제 장애 주입 rollback 증명이 아니다. 이번 실제 Mongo 테스트는 정상 commit·배치 경계·carry 소실 재시작을 검증한다.
- 보존기간의 물리 준수는 실행 주기·적체 처리·경보 운영 검증이 남는다.

## 5. 구현 및 다음 작업

- carry 소실: 증거 삭제 없이 cursor 0/carry null을 명시 CAS하고 재탐색한다.
- CAS 불일치/선택 원본 삭제 건수 불일치: 실패를 전파해 transaction을 rollback한다.
- 100개 밖 range: keyset 진행으로 다음 배치에서 처리한다. 실제 정상 sequence 간격은 연결하지 않으며 floor를 임의 승격하지 않는다.
- preflight: 기존 index 이름·key 순서·unique·partial·collation, TTL/hidden/불필요 unique와 정상 `_id_`를 검사한다. source BSON int64·상태·closedAt·carry 범위와 단일 ACTIVE를 확인한다.
- 다음: 엄격 HTTP adapter와 독립 서비스 인증, 명시 runtime wiring, snapshot/source 정리 스케줄과 경보, staging IAM/Billing/복원 검증.

## 6. 근거 및 전체 변경 파일

- `src/main/java/web/tosunsaeng/identity/domain/user/withdrawalrecovery/RecoveryIndexes.java`
- `src/main/java/web/tosunsaeng/identity/domain/user/withdrawalrecovery/MongoSourceSweep.java`
- `src/test/java/web/tosunsaeng/identity/domain/user/withdrawalrecovery/RecoveryIndexesTests.java`
- `src/test/java/web/tosunsaeng/identity/domain/user/withdrawalrecovery/MongoSourceSweepTests.java`
- `src/test/java/web/tosunsaeng/identity/domain/user/withdrawalrecovery/RecoveryReplicaSetTests.java`
- 본 문서 및 `docs/codex/WORKLOG.md`, `docs/codex/CURRENT_STATE.md`.

`env -u SENTRY_AUTH_TOKEN TMI203_LOCAL_MONGO=true ./gradlew clean test` 성공(34초). XML 집계 tests=2000/failures=0/errors=0/skipped=6. mock 단위 12개와 실제 Mongo 2개 신규. shared manifest 16개 key 순서/옵션 일치 테스트 포함. 합성 DB는 테스트 후 삭제하고 이번 전용 임시 컨테이너만 제거했다. 실제 Atlas/AWS/Billing 호출 없음.

외부 recovery-r1/scalar-end-1 wire·fixture·hash·보존, 기존 JWT/LC/202 push 계약은 불변. 기존 dirty 변경을 보존했으며 이번 예상 밖 변경 없음. commit/push/Jira 쓰기/배포 없음.

Jira 댓글 초안(미등록): TMI-203 source range bounded sweep와 exact index/source migration preflight 구현. 신규14개 포함 전체2000개(1994통과/6skip),실로컬Mongo 배치경계·carry소실검증 통과. HTTP/IAM/runtime wiring/스케줄·경보/공동복원은 미완료. 변경파일과 검증한계는 본 문서 참조.
