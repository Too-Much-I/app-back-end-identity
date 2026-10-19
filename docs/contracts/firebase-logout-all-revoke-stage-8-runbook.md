# TMI-129 — 전체 로그아웃 Firebase revoke 통합 운영 검증

- 작성일: 2026-09-08
- 상태: 코드 기본 OFF / 병합·배포·실제 운영 검증 전
- 기준: [Stage 8 계획·구현 결과](firebase-logout-all-revoke-stage-8-plan.md), [Stage 7 계획](billing-entitlement-owner-fanout-stage-7-plan.md)

## 1. 5줄 결론

1. Stage 7·8 코드는 먼저 개발하고 운영 활성화 전 검증을 한 묶음으로 수행한다. production에서 일반 사용자로 처음 실험한다는 뜻이 아니다.
2. Session fence·capture·worker는 기본 false다. 모든 발급 instance의 fence 적용을 확인하기 전 capture를 켜지 않는다.
3. 새 인증 로그인은 작업 대기·격리 상태만으로 막지 않는다. 확인된 revoke에 영향받은 Firebase 세션만 다시 로그인해야 한다.
4. 결과 불명 mutation은 자동 재전송하지 않는다. 보관된 actor를 조사하고 exact 대상·종료 증거를 확인해야 한다.
5. 실제 Mongo rollback·Firebase·모바일·경보 검증은 아직 수행하지 않았다. 아래 증빙 없이는 production 활성화하지 않는다.

## 2. 사용자가 반드시 읽어야 하는 내용

- `200`은 내부 세션 무효화와 durable 접수 완료다. 원격 Firebase 완료·다른 기기 UI 변경을 뜻하지 않는다.
- 전체 logout은 계정 삭제·disable·SNS unlink가 아니다. 현재 기기 `/logout`은 Firebase revoke를 하지 않는다.
- 앱은 성공 시 Firebase signOut과 자체 Token 삭제를 수행한다. `401 SESSION_LOGGED_OUT`은 자동 exchange 루프 대신 정리·로그인 안내로 처리한다. `FIREBASE_RECENT_AUTH_REQUIRED`는 강제 Token 갱신만으로 해결되지 않는다.
- 네트워크 응답 유실은 같은 유효 Access Token/jti로 제한 재시도한다. 새 Token으로 자동 logout 재요청하면 별도 cycle을 만든다.
- worker 중단과 fence 중단은 다르다. 이미 생성된 epoch·watermark는 worker를 끄더라도 유지해야 한다. fence를 무시하는 구버전 rollback은 금지한다.
- 기존 자체 Access Token의 downstream 즉시 차단은 이번 범위가 아니다. 기존 만료·탈퇴 deny 정책을 유지한다.

## 3. 사용자가 결정해야 하는 사항

제품 정책은 승인됐다. 활성화 전에 운영 담당자가 다음을 확정·기록한다.

- staging 실행 창·테스트 계정·책임자와 production canary 승인자.
- 실제 사용자 Access Token 최대 TTL 및 Identity 검증 skew. receipt 보존 설정이 실제 skew 이상인지 확인한다.
- pending 60초 초과·reconciliation 발생 경보의 수신자, 운영 대응 시간, Firebase 관리 권한과 actor 조사 절차.
- 결과 불명 해소 절차의 리허설과 승인 주체. 새 공개 관리자 API는 구현하지 않았으므로 안전한 CAS 복구 도구/작업은 별도 검토 후 실행한다. 수동 DB 상태 변경을 미리 허용하는 문서가 아니다.

## 4. 주요 위험과 미확인 사항

| 위험 | 활성화 전 조건 |
| --- | --- |
| in-memory Mongo 테스트의 Transaction 한계 | replica-set에서 실제 rollback·write conflict·commit 응답 유실을 별도 검증 |
| Firebase HTTP mutation 응답 유실 | 재전송 0회, read-only 조회 및 격리, actor 종료 증거 조사 리허설 |
| 원격 UID 재생성 | Firebase 콘솔·타 자동화에서 동일 UID 강제 재생성 금지. DB CAS가 외부 변경을 원자 차단하지 못함 |
| 초 단위 auth_time 경계 | 같은 초 거절·다음 초 새 인증·늦은 조회·LOCAL 보존을 실제 Firebase에서 확인 |
| 구버전 writer 혼재 | 모든 instance가 공통 fence로 저장함을 확인하기 전 capture NO-GO |
| 미해결 작업 누적 | pending/격리에는 TTL 없음. count·oldest age·queue/marking backlog 감시 및 담당자 확보 |
| 탈퇴와 미해결 actor | 외부 cleanup 대기/소진 격리, release·CLEANED 차단 확인. 일반 로그인 허용과 혼동 금지 |

## 5. 현재 작업과 직접 관련된 설명

### 5.1 설정과 배포 순서

| 설정 | 기본값 | 역할 |
| --- | --- | --- |
| AUTH_SESSION_FENCE_ENABLED | false | 공통 인증 epoch·proof 경계 |
| FIREBASE_LOGOUT_ALL_CAPTURE_ENABLED | false | 기존 logout-all의 원자적 operation 접수 |
| FIREBASE_LOGOUT_ALL_WORKER_ENABLED | false | 같은 Spring 앱의 scheduled worker |
| FIREBASE_LOGOUT_ALL_RETENTION | P7D | terminal operation 최소 보존 |
| FIREBASE_LOGOUT_ALL_VERIFIER_SKEW | PT1M | request exp 이후 receipt 보존 여유 |
| FIREBASE_LOGOUT_ALL_LEASE | PT1M | DB lease, 원격 취소를 보장하지 않음 |
| FIREBASE_LOGOUT_ALL_POLL_DELAY | PT5S | batch 간격 |
| FIREBASE_LOGOUT_ALL_BATCH_SIZE | 20 | due scan 상한 |
| FIREBASE_LOGOUT_ALL_MAX_ATTEMPTS | 8 | 안전한 read/preflight 실패 시도 상한 |
| FIREBASE_CONNECT_TIMEOUT | PT3S | 고정 HTTPS 전송 connect |
| FIREBASE_READ_TIMEOUT | PT5S | read 및 write timeout |

`capture 또는 worker ON + fence OFF`, `worker ON + capture OFF`는 startup에서 거절한다. capture에는 기존 Firebase 프로젝트/tenant 설정도 필요하다.

1. Stage 7 consumer readiness·인증/route·재전송·독립 delivery 검증 항목과 이 runbook을 같은 운영 검증 목록에 등록한다. 인증 방식을 임의 변경하지 않는다.
2. 세 flags OFF로 새 버전을 배포한다. Mongo 인덱스와 실제 Transaction capability/rollback을 확인한다.
3. fence-only 상태로 전체 writer를 전환한다. legacy epoch 0·증빙 누락 세션 영향과 새 401/503 모바일 대응을 검증한다.
4. 구 writer 종료와 모든 인스턴스 fence ON을 확인한다. staging에서 capture/worker를 함께 켜 아래 E2E를 수행한다.
5. 결과·환경·설정·시간·병합 commit을 기록한 뒤 별도 승인으로 production canary를 진행한다. 이번 코드 작업에서는 어떤 환경도 활성화하지 않았다.

### 5.2 전송·저장 경계

- 신규 [HTTP adapter](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/infrastructure/firebase/FirebaseSessionRevocationHttpAdapter.java)는 ADC를 사용하고 고정 `identitytoolkit.googleapis.com`의 project/tenant `accounts:lookup`과 `accounts:update`만 호출한다. 임의 host·redirect를 허용하지 않는다.
- Admin SDK 9.4.3의 자동 503 retry를 피하기 위해 이 경로는 HTTP retry 0으로 분리했다. 기존 ID Token 검증 SDK는 변경하지 않았다. 테스트는 fake transport이며 실제 IAM 권한·API 활성화는 운영 검증 대상이다.
- mutation은 영속화한 dispatchAt을 초 단위 validSince로 전송한다. post-inspect의 validSince와 동일 creationTime을 확인한 뒤 control 경계를 단조 갱신한다. 조회 완료 시각을 경계로 사용하지 않는다.
- `user_session_controls`는 TTL 없이 인증 상태를 보존한다. `logout_all_operations`는 requestFingerprint unique, userId/epoch unique, due index, terminal cleanupAt TTL을 가진다. 별도 receipt collection은 없다.
- terminal TTL은 `max(terminalAt + retention, requestExpiresAt + verifierSkew)`다. pending·retry·격리 작업을 TTL로 지우지 않는다. Mongo TTL 삭제 지연은 정상이며 보안 판단에 사용하지 않는다.
- 물리 마킹은 접수 1,000개, sweep 20 control × 100 session으로 제한한다. 그보다 많은 세션도 논리적 epoch 검사로 거절한다. 다른 actor를 기다리는 queue는 lease 길이만큼 다음 확인을 늦춰 한 사용자가 전체 due batch를 계속 차지하지 않게 한다.

### 5.3 결과 불명·탈퇴 대응

1. 상태·failureCode·attemptCount·lease·dispatchAcknowledged·mutationStarted·관측 시각을 접근 통제된 DB에서 읽기 조사한다. 원문 Token/credential이나 UID를 로그·Jira·metric label에 복사하지 않는다.
2. worker 재시작·lease 만료·timeout만으로 원격 호출 종료를 확정하지 않는다. validSince 전진도 아직 살아 있는 actor가 없다는 증거는 아니다.
3. 종료 증거가 없으면 격리를 유지한다. 확인된 인증 경계 이후 새 로그인은 계속 허용하되 후속 원격 dispatch와 탈퇴 release는 기다린다.
4. 복구가 가능하다고 판단되면 exact operation/version·control slot·binding/creationTime·User/withdrawal 상태를 재확인하는 CAS 복구 변경안을 먼저 검토·승인한다. 필요 시 관측 경계와 terminal 상태·slot 해제·TTL을 하나의 Transaction으로 반영한다. control 삭제·target 변경·blind replay는 금지한다.
5. 미dispatch 작업은 탈퇴 Transaction에서만 supersede할 수 있다. started/unknown actor는 남는다. 기존 외부 cleanup은 LOGOUT_REVOKE_PENDING으로 bounded 대기 후 필요 시 운영 격리하며 identity release는 DEPENDENCY_PENDING을 반환한다.

## 6. 부록 — 증빙 체크리스트

### 6.1 격리 로컬 검증과 실제 운영 검증 구분

- 로컬: 전체 Gradle 회귀, in-memory Mongo mapping/index/@Version, mock Transaction·HTTP, flags OFF/invalid 조합, 클래스 기반 release Transaction proxy 검증.
- 미수행: 실제 Mongo rollback·동시성/장애 주입, 실제 Firebase API·다중 기기·모바일 UI, 운영 IAM/네트워크·경보·프로세스 재시작, Stage 7·8 통합 E2E.
- 코드 참조: [worker](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/FirebaseSessionRevocationWorker.java), [공통 fence](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/SessionSecurityService.java), [회귀 테스트](../../src/test/java/web/tosunsaeng/identity/domain/auth/session/application/SessionRevocationTests.java).

### 6.2 staging 검증 항목

- 정상: Guest/LOCAL·Firebase, 내부 세션 0건, 다중 SNS의 동일 UID, 새 로그인 및 다음 logout.
- 원자성: operation/epoch/세션 저장 중 실패 rollback, 최초 control 경쟁, 동일 jti 경쟁/응답 유실, refresh/login/upgrade/merge/탈퇴와 logout 경합.
- 경계: pending·격리 중 fresh login, old auth_time+fresh iat 거절, 지연 revoke의 영향받은 Firebase 세션만 거절, LOCAL/경계 이후 새 인증 유지, 다중 refresh 증빙 전파.
- worker: pre-dispatch crash 재claim, dispatch 이후 crash/응답 유실 격리 및 재전송 없음, 성공 후 DB 실패, read retry 소진, remote creation/tenant 불일치, 다른 사용자의 queue 진행.
- 탈퇴: undispatched handoff, unresolved actor와 disable/delete/release/CLEANED/rejoin 경합. 새 UID로 과거 작업 target을 변경하지 않음.
- 보존/규모: unique·due·TTL 인덱스, legacy 증빙 누락, 1,000개 초과 세션, terminal만 TTL, pending age·queue·marking backlog 경보.
- 모바일: 200의 접수 의미, 동일 jti 제한 재시도, 401 정리·재인증, 일시 503, Firebase signOut/자체 Token 삭제, 자동 exchange 루프 없음.
- 보안: 실제 권한 최소화, redirect/hidden mutation retry 없음, 로그/트레이스의 Token·Firebase 응답 원문 비노출.

### 6.3 운영 완료 인계 양식

- 병합 commit / 환경 / 설정 및 old writer 종료 시각 / 실제 JWT TTL·skew.
- Mongo rollback·경합·인덱스 결과 / Firebase 정밀도·응답 유실 결과 / 모바일 다중 기기 결과.
- Stage 7 독립 delivery 및 consumer readiness / pending·격리 경보 수신 증빙.
- unresolved actor 복구 리허설과 담당자 / canary 승인 / 실패 시 worker 중단 및 fence 유지 절차.

원문 Token·Password·RSA Key·Firebase credential·Mongo URI는 인계하지 않는다.
