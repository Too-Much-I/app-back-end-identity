# TMI-203 실행 구성·snapshot 자동 작업 및 IAM 조회 결과

## 1. 5줄 결론

1. runtime 활성화 시 전용 cursor key ring·capture/clock 근거·실제 index/source 이관 상태를 검증하고 Mongo/application 서비스를 구성한다.
2. snapshot 생성과 만료 정리를 전용 2-thread scheduler로 분리했다. 모든 신규 기능은 기본 OFF다.
3. 생성/정리 실패와 개인정보 만료 적체 메트릭을 추가했다. 실제 알림 채널·경보 정책은 아직 연결하지 않았다.
4. 전체 2,032개 중 2,008개 통과, 기존 opt-in 24개 skip, 실패/오류 0. 신규 12개이며 실제 AWS/Mongo 연동 테스트가 아니다.
5. AWS 서울 콘솔의 Lattice ListServices 조회는 조직 SCP explicit deny로 실패했다. 구성 유무를 확인하지 못했으며 실제 인증 연결·배포는 하지 않았다.

## 2. 사용자가 반드시 읽어야 하는 내용

사용자 요청으로 AWS 설정을 읽기 전용 확인했다. 로컬 CLI는 자격증명이 없었고, 기존 Chrome의 로그인된 AWS 콘솔을 사용했다. 서울 리전 Lattice 서비스 화면에서 `vpc-lattice:ListServices`가 조직 Service Control Policy의 명시 거부로 차단됐다. 빈 서비스 목록을 확인한 것이 아니다. 다른 인증이나 경로로 정책을 우회하지 않았다.

조직 관리자가 해당 정책의 적용 범위와 Lattice 사용 가능 여부를 확인해야 한다. 개별 IAM Allow만 추가해 SCP Deny를 해소할 수는 없다. 조회 실패만으로 서비스 생성·호출까지 모두 차단된다고 단정하지 않는다. 다른 계정/리전 구성 여부 역시 확인하지 않았다.

## 3. 사용자가 결정해야 하는 사항

현재는 조직 관리자에게 Lattice 조회/사용 가능 여부와 기존 ingress 구성을 확인받아야 한다. AWS 인증 방식 대체나 권한 확대는 이번 코드 구현 요청으로 승인된 것으로 취급하지 않는다. 기존 계약의 Lattice 경로가 불가능한 경우 Billing과 대안을 먼저 합의한다.

## 4. 주요 위험과 미확인 사항

- 실제 ingress verifier는 여전히 미구현이다. 사용자 JWT/임의 헤더를 신뢰하도록 대체하지 않았다.
- RuntimeEvidence는 기존 명시 주입 계약이며 운영 clock/capture 근거 제공이 필요하다. 기본 true 구현은 없다.
- source journal cleanup 권한/Evidence, range sweep 자동 스케줄, 폐기 source metadata 운영 정리는 이번 snapshot jobs와 별개다. source 삭제를 자동 활성화하지 않았다.
- 주기 실행과 메트릭만으로 물리 7일 보존 상한 준수를 입증할 수 없다. 적체 부하·장애 복구·실제 경보 수신 검증이 필요하다.
- 실행 구성 테스트의 index 검사는 mock construction을 사용했다. 운영 index, 실제 DB 연결/transaction, IAM, Billing 복원 검증을 대신하지 않는다.

## 5. 설정·동작

| 설정 | 기본/조건 |
| --- | --- |
| app.withdrawal-recovery.runtime-enabled | false. true일 때 기존 capture.required 및 신뢰 runtime 근거와 명시 index/이관 준비 필요 |
| app.withdrawal-recovery.http-enabled | 기존 false. 실제 ingress verifier 및 서비스 bean 필요 |
| app.withdrawal-recovery.snapshot-jobs-enabled | false. runtime과 builder 필요. 빠지면 기동 실패 |
| app.withdrawal-recovery.cursor-active-key | 전용 활성 key ID, 기본값 없음 |
| app.withdrawal-recovery.cursor-keyring-json | 전용 key ID→Base64 문자열 object, 최대8개/4096문자, key32~128bytes. JWT/support 키 재사용 금지 |

키 파싱 오류는 입력/원래 cause를 노출하지 않는 고정 메시지로 실패한다. actual 키/Secret은 생성·주입하지 않았다. 이전 검증키는 발급된 cursor TTL을 고려해 보존해야 한다.

runtime은 기존 Mongo factory와 명시 SNAPSHOT/MAJORITY/primary transaction manager, timeout30초를 사용한다. preflight는 read-only이며 DDL/기존 source 필드 자동 보정은 없다. HTTP/application bean과 SnapshotBuilder를 연결한다.

build는 fixedDelay1초마다 최대1개 후보를 처리한다. maintenance는 별도 thread에서 fixedDelay5초마다 최대100개 run, 기존 store의 run당최대100행 정리 한도를 따른다. 진행중 worker는 기존 durable lease/fence로 보호하며 shutdown 뒤 새T로 이어붙이지 않는다.

메트릭 prefix `identity.withdrawal.recovery`:

- `build{outcome}`, `build_failure`
- `maintenance_success`, `maintenance_failure`
- `privacy_overdue_seconds`: 정상0, 미확인/실패-1, 만료 미정리 적체가 있으면 최소1초
- `privacy_overdue`: 적체 관측 횟수

메트릭에 사용자 식별자/본문/키를 넣지 않는다. 실제 alert rule/notification은 미설정이다.

## 6. 근거·변경 파일·검증

신규 파일은 withdrawalrecovery package의 `RecoveryRuntimeConfiguration.java`, `RecoveryJobsConfiguration.java`, `RecoverySnapshotJobs.java`, 테스트 `RecoveryRuntimeTests.java`, `RecoverySnapshotJobsTests.java`다. 본 문서와 WORKLOG/CURRENT_STATE도 갱신했다.

`env -u SENTRY_AUTH_TOKEN ./gradlew clean test` 성공26초. XML tests2032/failure0/error0/skip24. 신규12개: OFF/no dependencies, ON missing dependency, 전용key/중복/오류비노출, preflight연결, capture근거없을때DB접근차단, boundedjob/실패/적체/unknown 지표. 실제로컬Mongo18개와기존support6개 opt-in은이번에미실행.

기존 exact/fixture/hash/JWT/LC/202push/보존계약 유지. 기존dirty보존·예상밖변경없음. 운영DB/IAM정책/Secret수정·배포·Jira쓰기·commit/push없음.

Jira 댓글 초안(미등록): TMI-203 runtime/preflight/key/service wiring 및 snapshot scheduler/적체 메트릭 구현. 신규12개 포함2032개중2008통과/24skip. 실제IAM조회는조직SCP ListServices명시거부로구성유무미확인. ingress/source cleanup운영연결/경보수신/Billing복원검증미완료. 기능OFF유지.
