# TMI-199 Billing 공통 fixture 결과 — Identity 검토

> 최신 실행 결과: 아래 §7 참고. §1~6은 최초 정적 검토 당시의 기록이며, 이후 Identity 독립 테스트 44건을 추가·실행했다. 신규 복구 production API와 실제 replica-set 검증은 여전히 미완료다.

- 날짜: 2026-10-07 / 브랜치: develop / Jira: TMI-199
- 범위: 첨부 결과 보고서, Billing 로컬 fixture 및 Identity 현행 코드 읽기. 제품 구현·계약 확정·운영 검증 아님.

## 1. 5줄 결론

1. Billing의 test-only 준비 결과는 공동 계약 방향과 부합한다. 그러나 TMI-199 전체 완료는 아니다.
2. 실제 JSON 4개를 읽고 SHA-256을 기록했으며, Node로 고정 행/빈 목록 digest를 재계산해 일치를 확인했다.
3. Identity의 기존 탈퇴는 같은 withdrawnAt으로 User와 outbox를 저장한다. 현재 코드만으로 양쪽 시각 불일치가 발생한다고 단정할 근거는 없다.
4. 새 journal의 저장 정밀도, wire DTO와 내부 오류 응답은 Identity serializer/DB 왕복 검증 및 양 서버 합의가 남았다.
5. Billing 보고의 285개 테스트 결과는 인계받은 결과다. 이번 검토에서 Gradle/Mongo 실험을 재실행하거나 운영 기능을 켜지 않았다.

## 2. 사용자가 반드시 읽어야 하는 내용

지금은 결제·탈퇴 복구를 구현하기 위한 공통 기반을 검증한 단계다. 실제 결제 API·탈퇴 수신·복구 API가 완성됐다는 의미가 아니다. 기존 승인 정책을 다시 선택할 필요는 없지만, Identity의 독립 교차 테스트를 마치기 전에 C0 전체 완료나 판매/삭제 가능으로 표현하면 안 된다.

이번 Node 검증은 제공된 canonical 문자열을 그대로 UTF-8 직렬화하고 길이 prefix를 붙여 digest를 비교한 것이다. Java Instant의 비정규 입력 정규화, Identity ObjectMapper, production strict decoder 및 상태 전이 검증을 대체하지 않는다.

## 3. 사용자가 결정해야 하는 사항

새 정책 결정 요청은 없다. 후속 구현 요청 시 TMI-199의 Identity 교차 테스트부터 진행하는 것을 권장한다. 본 검토를 TMI-200/202/203 구현·이관·배포 승인으로 취급하지 않는다. Jira 댓글/상태도 변경하지 않았다.

## 4. 주요 위험과 미확인 사항

- BSON Date 밀리초 정밀도 손실은 Billing 실험 결과다. Identity의 실제 MappingMongoConverter 왕복과 현재/신규 writer 경계를 별도로 테스트해야 한다.
- 새 journal을 저장 전 고정밀 값으로 직렬화하고 snapshot은 BSON Date에서 읽는 구조는 digest 불일치를 만들 수 있다. 하나의 authoritative Instant를 정하고 기존 이벤트를 사후 반올림하지 않는다.
- T의 seconds/increment, count/pageCount/version의 문자열 표현, nullable 필수 필드 목록은 후보이며 최종 DTO로 확정하지 않았다.
- wire sequence가 decimal string인 것과 Mongo 정렬 자료형은 별개다. 영속 sequence를 일반 문자열로 정렬하면 10이 2보다 앞설 수 있다. I2/I3에서 숫자 순서를 보장하는 타입·상한·변환을 명시하고 9/10 경계 테스트를 포함한다.
- Identity 전역 오류는 BaseResponse다. 새 internal recovery 경로만의 성공/오류 DTO와 인증 필터 오류 매핑을 합의해야 한다. 기존 public 응답을 바꾸지 않는다.
- 다중 노드 failover, 실제 history eviction, 성공 commit 응답 유실, 운영 snapshot budget·권한·데이터량은 미검증이다. 추가 로컬 실험 또는 승인된 staging 검증에 명시적으로 배정해야 한다.

## 5. 현재 작업과 직접 관련된 설명

### 시각 경로 확인

- [UserWithdrawalService](../../src/main/java/web/tosunsaeng/identity/domain/user/application/UserWithdrawalService.java)는 clock.instant()를 트랜잭션 서비스에 전달한다.
- [UserWithdrawalTransactionService](../../src/main/java/web/tosunsaeng/identity/domain/user/application/UserWithdrawalTransactionService.java)는 동일 withdrawnAt으로 tombstone과 outbox를 같은 트랜잭션에서 저장한다.
- [UserRepositoryCustomImpl](../../src/main/java/web/tosunsaeng/identity/domain/user/domain/repository/UserRepositoryCustomImpl.java)은 withdrawnAt을 Mongo update에 전달한다. outbox 필드도 Instant다. src/main에서 별도 MongoCustomConversions/WritingConverter/ReadingConverter 선언은 검색되지 않았다.
- [UserWithdrawnEventMapper](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalevent/application/UserWithdrawnEventMapper.java)는 입력 outbox의 시각을 직렬화한다. 이 mapper 자체에 저장 정밀도를 강제하는 기능은 없다.

따라서 기존 코드의 결함 확정이 아니라, 신규 journal/snapshot 설계에서 저장 후 값과 wire 값의 일치를 테스트해야 한다는 결론이다. 기존 LC 전송 계약은 변경하지 않는다.

### 권장 후속 순서

1. 아래 해시로 fixture 입력을 고정하고 Identity 독립 serializer/strict decoder 테스트를 추가한다.
2. User/outbox/journal의 시간 왕복과 canonical digest를 검증하고 저장 정밀도 규칙을 합의한다.
3. exact DTO/null/숫자 범위, internal 오류·인증 경계, Identity index 후보를 동결한다.
4. Mongo 미검증 시나리오의 실행 환경·담당 gate를 배정한 뒤 TMI-199 완료 여부를 양측 확인한다.
5. 그 결과를 기반으로 I1/I2 구현으로 이동한다. 실제 활성화는 별도다.

## 6. 부록 — 근거와 검증 범위

Billing 입력 위치: `/Users/msde76/billing/src/test/resources/contracts/payment-lifecycle/v1/`.

| 파일 | SHA-256 |
| --- | --- |
| snapshot-digest.json | bd8606d7d5304b2a59399c1c1eeb68a2ef39b1cf9abcb2bb4dbd18fec0a4582d |
| protocol-cases.json | d7238d963ef786f3304e0e00cc3d677af66b85700f2aa96557111ab905a5cab1 |
| wire-examples.json | 6c1f4611373d08b65196998a2d6ad1d37da03414b2a1b4e8c761d1765860fb1b |
| billing-indexes.json | 6395954e0c7ea62d050c1b0bdcb3bd8887a00721adcee7876a1bbc62ff834aff |

- JSON 4개 parse 성공. 고정4행 digest 및 빈 목록 digest 모두 일치, 명령 exit 0.
- canonical 행은 userId, withdrawnAt 순서 JSON UTF-8, 각 행 앞 unsigned 4-byte big-endian byte length를 붙여 SHA-256 산출. 테스트 oracle의 해당 구현과 대조했다.
- protocol-cases는 strict decode/ACK/feed/HTTP 분기 예제이며 숫자형 테스트 입력을 포함한다. production wire string DTO와 혼동하지 않는다. 전체 상태 전이 실행은 이번에 하지 않았다.
- TMI-199 공식 Jira 조회: 해야 할 일. 완료 기준은 양 서버의 동일 fixture 결과 합의와 schema/외부 gate 기록이다.
- 실행한 검증: 정적 코드 조회, Node digest 재계산, git diff --check. Gradle/외부 Mongo/OAuth/AWS 호출 없음. 제품 변경 없는 검토이므로 전체 회귀 테스트는 실행하지 않았다.
- Jira 댓글 초안(미등록): Billing fixture 4개 해시 및 고정/빈 digest 확인. Identity 시각 전달·저장 경로 정적 확인. 변경 파일은 검토 문서와 작업 기록. 독립 Identity serializer/DB 왕복·DTO/오류 경계 합의·실환경 gate는 미완료. Jira 상태 유지.

## 7. 후속 요청에 따른 Identity 독립 실행 결과

### 7.1 5줄 결론

1. Identity에 동일 JSON4개와 독립 Java 테스트를 추가했으며 신규44건 모두 통과했다.
2. 실제 User/outbox 엔티티의 MappingMongoConverter 왕복 결과는 동일 밀리초로 일치했다.
3. 기존 UserWithdrawnEventMapper와 Boot Jackson 기본 설정은 기존4필드 wire 및 golden 예제를 유지한다.
4. 저장 전 마이크로/나노초와 저장 후 밀리초 값을 섞으면 wire와 digest가 달라짐을 재현했다. 현행 DB 조회 기반 전송 결함을 입증한 것은 아니다.
5. 전체 clean test는1233건/실패0/오류0/기존skip6. 새 recovery API·실제 Mongo replica-set·운영 gate까지 검증 완료한 것은 아니다.

### 7.2 사용자가 반드시 읽어야 하는 내용

User와 outbox를 모두 저장한 뒤 읽어 비교하면 서로 같은 시각이다. 반면 새 journal이 저장 전 고정밀 payload를 보존하면서 snapshot이 기존 User의 밀리초 시각을 사용하면 달라진다. 따라서 신규 journal은 User/snapshot과 동일 authoritative 시각을 사용해야 한다. 저장소 과거 이벤트를 임의로 반올림하거나 다시 생성하지 않는다.

실제 데이터베이스 왕복 대신 Spring Data Mongo 변환기 write/read를 실행했다. BSON Date 표현의 정밀도는 확인했지만 네트워크·트랜잭션·replica-set 내구성 검증은 아니다. Billing의 로컬 Mongo 결과는 인계 자료로 유지한다.

### 7.3 사용자가 결정해야 하는 사항

추가 제품 정책 선택은 없다. 신규 source 구현 전에 양 서버가 시간 규칙과 exact DTO/오류·인증 경계를 기술적으로 동결해야 한다. 이번 테스트 통과로 TMI-199 Done, 구매 허용 또는 자동삭제를 활성화하지 않는다.

### 7.4 위험과 미확인 사항

- 테스트용 decoder/ACK/feed/HTTP 분기는 production 구현이 아니다. 실제 endpoint·보안 필터·서명 cursor·엄격한 미래 시각/요청 크기 검증은 후속 구현 테스트 대상이다.
- Boot 기본 ObjectMapper로 검증했으며 배포 시 주입된 별도 설정은 조회하지 않았다.
- Billing index manifest는 byte hash만 확인했다. Identity candidate index 생성·성능·이관은 미검증이다.
- 문자열 숫자 DTO를 테스트했지만 영속 sequence의 숫자 타입·범위·정렬은 I2/I3 설계에서 확정해야 한다.
- 다중 노드 failover/실제 history eviction/성공 commit 응답 유실/운영 snapshot5분·control 이관 진단은 별도 검증 gate로 남는다.

### 7.5 변경 및 유지 계약

- 추가 파일: [공통 fixture 테스트](../../src/test/java/web/tosunsaeng/identity/contract/PaymentLifecycleFixtureTests.java), [시각 호환 테스트](../../src/test/java/web/tosunsaeng/identity/contract/WithdrawalTimestampCompatibilityTests.java), test resources JSON4개·README.
- 동일 입력 해시, typed record 기반 canonical digest, strict v1 decode, generation 우선 ACK, 고정 feed 누락 감지, Billing204-only/auth/retry 후보를 독립 실행했다. Billing 테스트 Java 코드를 복사하지 않았다.
- 시각0/3/6/9자리에서 실제 엔티티 변환·wire·digest 비교 및 canonical text 보존 후보를 실행했다. 고정 시각 예제는 기존 mapper 출력과 공유 withdrawal JSON 전체를 비교한다.
- src/main, build.gradle, API 계약, JWT/LC/무료 기능 및 운영 설정은 미변경. Billing 저장소도 변경하지 않았다.

### 7.6 실행 근거·인계 초안

| 검증 | 결과 | 한계 |
| --- | --- | --- |
| PaymentLifecycleFixtureTests |40건 통과 | 새 복구 기능은 test oracle |
| WithdrawalTimestampCompatibilityTests |4건 통과 | 실제 Mongo 서버 없이 converter/mapper 실행 |
| ./gradlew clean test |170 suites,1233 tests, failures0, errors0, skipped6 | skip6은 기존 테스트; 신규skip0 |
| fixture SHA-256 |4개 모두 이전 Billing 입력과 동일 | 계약 변경 시 양측 재검토 필요 |

초기 sandbox 실행은 Gradle cache lock 권한으로 중단됐다. 승인된 실행으로 전체 테스트를 완료했다. diff 검사 통과. 기존 알림 작업의 동시 문서 기록은 보존했으며 예상 밖 제품 변경 없음.

다음 작업/배포 전: 신규 source의 authoritative 시각 규칙 및 DTO/오류/auth 합의 → 실제 Mongo 미검증 시나리오 담당·환경 배정 → C0 공동 완료 판단 → I1/I2 구현. 현행 테스트 결과만으로 배포하거나 활성화할 제품 변경은 없다.

Jira 댓글 초안(미등록): TMI-199 Identity 교차 테스트44건 및 전체1233건 실패0/skip6 확인. 테스트2파일·fixture4개/README·검토/기록 문서 변경. 실제 mapper와 converter에서 User/outbox 저장 시각 일치, 저장 전 고정밀 값과 혼합 시 digest 불일치 재현. production recovery API·index·replica-set 및 운영 gate 미검증, Jira 상태 유지.
