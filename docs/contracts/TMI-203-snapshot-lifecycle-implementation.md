# TMI-203 snapshot 작업 저장·빌더·만료 정리 구현 결과

- 날짜: 2026-10-12 / 브랜치: `feat/TMI-203-withdrawal-snapshot-feed-recovery`
- 상태: 저장·application 계층 구현 및 로컬 검증. HTTP/IAM 연결·운영 활성화 완료 아님.

## 1. 5줄 결론

1. [MongoSnapshotStore](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalrecovery/MongoSnapshotStore.java)에 멱등 작업 생성·소유자 조회·lease·불변 row 저장·READY 전환·정리를 구현했다.
2. [SnapshotBuilder](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalrecovery/SnapshotBuilder.java)는 동일 T의 자료만 쓰고 저장된 row의 count/digest를 다시 확인한 뒤 source/run CAS로 공개한다.
3. [RecoverySnapshotService](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalrecovery/RecoverySnapshotService.java)는 create/status/page 응답과 오류·Retry-After·서명 keyset cursor를 연결한다.
4. 만료 시 논리 접근은 즉시 차단하며, 물리 자료는 bounded 정리 메서드로 삭제한다. 실제 주기 실행·지연 경보 연결은 아직 없다.
5. 신규 26개 포함 전체 1,986개 중 1,980 통과/기존 6 skip/실패·오류 0. 신규 실제 로컬 Mongo 13개와 기존 Mongo 3개도 실행했다.

## 2. 사용자가 반드시 읽어야 하는 내용

이제 snapshot lifecycle이 계산 함수만 있는 상태는 아니다. 실제 Mongo transaction으로 작업과 row를 저장하며, 완성되지 않은 row는 공개하지 않는다. 다만 Bean/controller/scheduler로 자동 등록하지 않았으므로 실제 복구 HTTP API나 정리 작업이 배포 후 자동으로 켜지는 구현은 아니다.

snapshot은 현재 원천에 남아 있는 WITHDRAWN 상태 복구 자료다. 이 구현은 과거 완전성 증명 원천이 없으므로 `legacyCoverage=UNKNOWN`과 `LEGACY_HISTORY`를 남긴다. source의 replayFloor와 중간 range도 동일 T에서 읽어 knownGaps에 담고, 크기 초과 시 잘라서 성공시키지 않는다. Billing은 이를 과거 전체 이벤트 수신 증거로 바꾸거나 snapshot 부재를 활성 계정 근거로 사용하지 않는다.

7일 이후에는 page를 거절하고 status/create는 EXPIRED를 반환한다. 물리 정리는 run 최대100개, run당 row 최대100개씩 수행하고 manifest/digest/빌드 자료를 지운다. 최소 operation binding은 120일까지 유지한다. **물리 보존 상한을 운영에서 지키려면 정리 scheduler·실패/적체 경보를 반드시 연결해야 한다.** 이번 로컬 메서드 구현만으로 운영 보존 준수를 선언하지 않는다.

## 3. 사용자가 결정해야 하는 사항

추가 제품 정책 선택은 없다. 환경의 clock/capture 신뢰 근거·전용 cursor key·서비스 인증/직접 ingress 차단과 승인된 index/migration을 연결한 뒤 실제 활성화 검증을 해야 한다. 현재 API·보존 계약을 변경하지 않았다.

## 4. 주요 위험과 미확인 사항

- HTTP route·IAM principal 전달·직접 ingress 차단, startup index/migration preflight, scheduler 및 보존 적체 경보는 여전히 남아 있다.
- source range sweep·cleanup 충돌 재시도/unknown commit reconciliation·retired source 정리는 이전 미완료 범위다.
- MongoSnapshotStore는 snapshot read concern/majority write concern의 Mongo transaction manager를 주입해야 한다. 내부 transaction timeout은 최대30초, 조회는 최대5초이며 build 전체는 생성부터5분 lease로 제한한다.
- source version 변경 시 READY 전환을 보수적으로 거절한다. concurrent create/cleanup/다른 seal과 경합하면 안전한 실패가 증가할 수 있어 운영 부하·재시도 검증이 필요하다.
- restart 시 과거 T를 복원해 이어 읽는 기능은 없다. 이미 시작된 run은 재claim하지 않고 기한 후 FAILED로 닫는다. 사용자 자료를 새로운 T로 이어 붙이지 않는다.
- replica set 테스트는 단일 로컬 Mongo7 합성 환경이다. 실제 history window 소실·primary failover·장기 장애·운영 DB 복원·Billing 적용 연동·IAM 검증을 대신하지 않는다. history-loss와 응답 유실 일부는 합성 오류 주입으로 확인했다.
- 새 pending worker 탐색과 maintenance query의 실제 index plan/대량 처리 검증이 필요하다. 자동 DDL은 없다.

## 5. 구현 동작 및 내부 저장 필드

### 생성·소유자·멱등

- unique 범위는 기존 exact의 `(environment, consumer, requestOperationId)`. stream/scope는 binding 비교 대상이다.
- 같은 key의 다른 stream/scope는 source 상태보다 먼저 OPERATION_CONFLICT.
- 동시 생성/불명 commit은 같은 key를 재조회하며, 임의 새 operation으로 우회하지 않는다. winner가 아직 확인되지 않으면 일시 실패로 닫는다.
- status/page는 인증된 owner 범위로 조회한다. 다른 owner는404이며 식별 정보를 노출하지 않는다.
- source fence는 active source/version CAS 실제 쓰기로 restore와 경합한다. metadata가120일을 지나도 물리 정리가 남아 있으면 동일 key create는 일시 실패할 수 있다. 새 논리 작업은 새 UUID를 사용한다.

### 빌드·공개

- run: snapshotId/environment/consumer/requestOperationId/streamId/scope/status/version/createdAt/privacyExpiresAt/operationExpiresAt/privacyCleaned.
- BUILDING: phase(PENDING→READING→VERIFYING), buildDeadline, leaseOwner, leaseUntil, pinned(T/H/knownGaps/expiry), sourceVersion, rowCount, lastUser.
- lease는 한 번만 배정하고 buildDeadline까지 유효하다. append20개와 run version 갱신은 한 transaction이다. 다른 worker, 만료, phase 불일치는 쓰기 거절.
- row: 내부 `_id`, snapshotId, userId, withdrawnAt canonical 문자열. 기존 unique `(snapshotId,userId)`와 keyset 조회를 이용하며 ordinal용 신규 index/collection을 만들지 않는다.
- row 쓰기를 VERIFYING으로 동결한 뒤 저장 자료를20개씩 재조회하여 count/digest를 검증한다. source version/T/H 및 run version을 확인하여 READY manifest를 저장한다. 일부 실패 자료를 READY로 바꾸지 않는다.
- READY commit 응답 유실 뒤 fail 처리는 이미 READY인 상태를 변경하지 않고 durable 상태를 재조회한다.
- private build failure 원인은 exact의 HISTORY_LOST/BUILD_TIMEOUT/BUILD_FAILED 3종으로 제한하며 예외 원문은 외부로 보내지 않는다.

### 조회·cursor·만료

- create BUILDING은202, status BUILDING은200, 둘 다 Retry-After5. READY/FAILED/EXPIRED 필드 생략 규칙 유지.
- page는 최대20개, 최종 cursor는null. 빈 snapshot은 빈 최종page1개다.
- opaque 서명 cursor 내부에 page ordinal/pageCount/afterUser를 결합해 keyset으로 조회한다. 후속 cursor는 기존 발급 시각을 유지하여1시간TTL을 갱신하지 않는다. 공유 cursor 문자열 shape/hash는 변경하지 않았다.
- 부분 행 소실/행수 모순/범위 오류는 짧은 성공 page로 처리하지 않는다. snapshot 상태/만료 확인이 cursor 오류보다 우선한다.
- EXPIRED 전환과 bounded row 삭제는 run version으로 worker와 경합한다. 민감한 빌드/manifest 필드는 제거한다. 모든 row가 사라진 뒤120일 지난 operation을 제거한다.

## 6. 부록 — 테스트·변경 범위·다음 단계

- 신규: SnapshotBuilderTests5, RecoverySnapshotServiceTests8, SnapshotLifecycleReplicaSetTests13 =26개.
- 실제 로컬 추가13개: 멱등/owner격리/충돌우선순위,41행3page/digest/replay,빈목록,중간history실패,deadline stale worker,owner소실 재시작 금지,7일/120일bounded정리,READY응답유실,저장행변조,source변경,만료cursor/행소실,동시create,knownGaps크기초과.
- 최종 `env -u SENTRY_AUTH_TOKEN TMI203_LOCAL_MONGO=true ./gradlew clean test`: 31초 성공. 전체1986/통과1980/기존support opt-in6skip/실패오류0. 공유fixture hash 검사 포함. `git diff --check` 통과.
- 수정: 기존 MongoRecoveryReader의 snapshot 시작 callback/knownRanges/failure 분류, RecoveryCursor의 snapshot keyset·TTL연속성. 신규 main3개/test3개와 이 문서·WORKLOG/CURRENT_STATE. 기존 dirty 파일·의존성·fixture 변경은 보존했고 이번 범위의 예상 밖 변경은 없다.
- 테스트 DB는 무작위 이름의 localhost 합성 자료만 사용하고 종료 후 삭제했다. 이번 임시 Docker 컨테이너도 정리했다. 운영 DB/설정/Jira쓰기/배포/commit/push 없음.
- 다음: source sweep/reconciliation와 index preflight → strict HTTP/IAM 경계 및 scheduler/보존 경보 연결 → Billing 공동 복원·운영 동등 검증. 전체 TMI-203 완료/활성화로 표시하지 않는다.
- Jira 댓글 초안(미등록): snapshot durable lifecycle·builder·create/status/page application 연결 구현. 신규26개 포함1986개(1980통과/6skip),신규로컬Mongo13개통과. keysetcursor/7일·120일정리/부분실패·멱등·READY응답유실검증. HTTP/IAM/scheduler/index/source sweep 및공동복원은남음. 외부계약/공유hash/기존202·LC·token유지.
