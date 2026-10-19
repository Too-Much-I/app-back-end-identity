# TMI-203 구매 준비 조건 분리·연결 설계

> 2026-10-13 후속: 사용자 구현 요청에 따라 아래 DB/config·source binding 연결을 로컬 구현했다. [구현 결과](TMI-203-purchase-readiness-implementation.md)를 최신 상태로 따른다. 본문의 후보/미구현 표현은 제안 당시 이력이다. `AUTH_PURCHASE_SOURCE_STREAM_ID`가 실제 설정으로 추가됐으며 구매 기본 OFF, 운영 이관·구writer 종료·시간 동기화·판매 준비 확인 책임은 유지한다. 발신 RuntimeEvidence는 선행 작업에서 제거했고 이번에는 구매 사용처와 불필요해진 두 증명 인터페이스를 제거했다. 공동 연동·실배포·판매 승인은 아니다.

- 날짜: 2026-10-13 / 브랜치: feat/TMI-203-withdrawal-snapshot-feed-recovery
- 연계: TMI-200·202 / 상태: 조사 후 연결 설계안. 코드 적용·공동 계약 개정·이관·구매 활성화·판매 승인 아님.

## 1. 5줄 결론

1. 별도 승인 저장소/갱신 시스템을 만들지 않고, 구매 준비를 DB/config 자동 검사와 배포/복원 체크리스트로 나눈다.
2. 사용자별 ACTIVE MEMBER·User/control 일치·transaction·TTL·재발급/replay 보호는 기존 구현을 유지한다.
3. 전역 준비는 CaptureReadiness와 필요한 설정을 재사용하되, 현재 source가 바뀌었다고 자동으로 구매를 허용하지 않는다.
4. 현재 PurchaseActivationEvidence가 맡던 이관/구writer/공동검증 사실은 명시 운영 절차로 옮기는 제안이다. 설정 하나가 그 사실을 증명한다고 주장하지 않는다.
5. 구매 쪽 기존 계약의 책임 변경을 확인한 뒤 구현해야 한다. 이번 작업은 코드 연결 방식을 문서화했고 실제 provider 제거/기능ON은 하지 않았다.

## 2. 사용자가 반드시 읽어야 하는 내용

### 2.1 자동 검사와 운영 확인

| 조건 | 어디서 확인 | 구현/운영 방향 |
| --- | --- | --- |
| capture/control writer/발급fence/세션보호 설정 | 기동 시 config | 기존 필수 조합 유지 |
| JWT TTL 양수·최대30분 | 기동 및 발급 시 | 기존 PurchaseProperties/PurchaseTokenFence 유지 |
| 해당 환경의 정상 ACTIVE source가 정확히1개 | 기동 및 실행 중 DB | CaptureReadiness의 bounded 조회 재사용 |
| 활성화 대상 source와 현재 source 일치 | 기동 및 실행 중 | 아래 source binding 제안. 복원 후 자동 재승인 방지 |
| 개별 사용자가 ACTIVE MEMBER이며 control 상태 일치 | 발급/replay transaction | 기존 PurchaseControlService 유지. 누락control 자동생성 금지 |
| 기존 사용자 control 이관 완료 | 활성화 전 운영 진단 | 기존 PurchaseControlMigration의 bounded dry-run/apply·재진단. 새 승인DB 불필요 |
| 구writer 종료·호환 버전만 실행 | 관련 배포 완료 후 운영 | 기존 배포/복원 절차. DB source 존재만으로 종료를 증명하지 않음 |
| 시간 동기화·이상 대응 | 인프라 운영 | 별도 앱 시간증명 서비스를 만들지 않는 방향. JWT시간검증은 유지 |
| 실제 Billing 판매·환불/권리회수 준비 | 공동 검증 및 판매 승인 | Identity readiness만으로 실제판매 허용하지 않음 |

매 로그인마다 전체 회원 이관 상태를 스캔하지 않는다. 요청 대상의 control 검사를 유지하고 전체 이관 확인은 활성화 전 운영에 맡긴다. 이는 전체 이관 완료를 앱이 계속 증명하던 시스템을 구현한다는 뜻이 아니며, 운영 확인을 생략해도 된다는 뜻도 아니다.

### 2.2 source binding 권장안

새 승인 collection/API/만료·갱신 worker는 만들지 않는다. 대신 기존 구매 활성화 설정에 **대상 source streamId를 명시하는 설정 한 개**를 추가하는 방향을 제안한다. 후보 이름은 `AUTH_PURCHASE_SOURCE_STREAM_ID`이며 아직 코드/환경에 존재하는 확정 설정이 아니다.

- scope ON에는 유효한 UUID 설정이 필요하고 실제 DB의 단일 ACTIVE stream과 일치해야 한다.
- source가 교체되거나 사라지면 준비검사는 false/오류로 닫힌다. 새 source 값을 자동으로 학습하거나 저장하지 않는다.
- scope OFF여도 기존 purchase 응답 replay에는 동일 binding 보호를 적용한다. 단순 scope OFF로 기존 replay를 허용하지 않는다.
- 복원 시 기존 writer를 격리하고 새 source를 설치한 뒤 운영 점검/별도 재개 판단으로 설정을 바꾼다. 같은 sourceId를 그대로 복원한 경우를 이 설정만으로 탐지한다고 주장하지 않는다.
- 설정의 의미는 **활성화 범위 선택**이지 이관·시계·구writer 종료의 증거가 아니다. 새 원천으로 바뀌어도 자동허용되는 문제를 막기 위한 최소 fence다.

## 3. 결정해야 하는 사항

권장안은 위 source binding과 기존 플래그/DB검사, 운영 체크리스트를 조합하는 것이다. 운영 증명 provider를 제거하려면 TMI-200의 이관/구writer 검증 요구를 운영 책임으로 남긴다는 점과, 구매의 별도 시간 증명 요구도 인프라 전제로 바꾸는 점을 함께 확인해야 한다.

현재 TMI-203의 정리 최소안은 구매 보호 일괄 제거를 승인하지 않는다. 따라서 이번 설계를 현행 계약이 이미 허용한 구현으로 취급하지 않는다. Jira 설명 수정이 필요하면 수정문구를 먼저 제시하고 승인 후 처리한다. Billing의 발신 시간 인계 회신은 구매 활성화 범위의 자동 승인이 아니다.

## 4. 주요 위험과 미확인 사항

- 이 설계는 운영 점검의 책임을 명확히 하는 것이지 동일한 독립 증명을 더 간단히 구현하는 것이 아니다. 이관 누락은 해당 사용자 발급 오류로 이어질 수 있다.
- 절대시각 오차를 별도 provider로 차단하지 않으면 오차에 따른 만료/발급 판단 위험은 인프라 운영 전제로 남는다. JWT검증이 모든 영향을 제거하지 않는다.
- capture/매입 회원상태 readiness를 서로 다른 시점에 읽으면 source 교체 경합을 놓칠 수 있다. source 정보와 준비 판정은 같은 조회 결과로 묶고 기존 transaction/fence와 함께 검증해야 한다.
- 실제 DB restore에서 User/control까지 과거로 돌아간 상태는 source ID 비교만으로 복구되지 않는다. 구writer 격리·새 source·Billing 복원 검증 전 구매 재개 금지 원칙 유지.
- 실제 운영 설정/TTL/control 이관/인덱스/기존구매응답은 이번에 조사하지 않았다. 모든 발급/replay 경로의 실DB 경합은 별도 검증이다.

## 5. 후속 구현 범위와 완료 기준

### 5.1 코드 연결

1. CaptureReadiness에 단일 ACTIVE source의 검증된 정보를 얻는 내부 경계를 마련한다. boolean검사 뒤 별도 findOne으로 다른 source를 읽는 패턴을 피한다.
2. 구매 설정에 대상 source binding을 추가하고 문법/기동 조건을 검증한다. 기본scope OFF 유지, 기존 control/fence/session 플래그를 합치거나 자동ON하지 않는다.
3. PurchaseReadiness를 config·검증된 source binding으로 구성한다. 구매용 RuntimeEvidence/PurchaseActivationEvidence 책임을 분리하고, 남은 발신용 RuntimeEvidence를 함께 제거하지 않는다.
4. PurchaseTokenFence와 PurchaseControlService의 개별사용자 ACTIVE/control/CAS/expiry/replay·unknown commit 보호는 그대로 둔다.
5. 기존 인터페이스 제거 시 참조/테스트/문서를 정합화하되 trueadapter나 가짜 운영완료 문서를 만들지 않는다. 실제 환경에서 커스텀 provider가 사용됐는지 배포 전에 확인한다.

### 5.2 필수 회귀

- OFF 기동 및 writer 준비만ON일 때 purchase 미부여.
- scope ON+source설정 누락/불일치/중복ACTIVE/손상source/DB실패/잘못된플래그/TTL초과 → 기동 또는 발급 차단.
- 준비된 synthetic source·설정과 ACTIVE MEMBER/control → 정상 권한 합성; Guest/탈퇴/control누락·drift → 거절.
- 실행 중 source폐쇄/교체·scope OFF 후 기존purchase replay → 보호 유지.
- 탈퇴가 먼저commit/발급이 먼저commit/rollback/unknown commit·재발급 결과 유실·기존exp 유지 회귀.
- 실제Mongo bounded source 조회와 source교체 경합, HTTP응답까지의 발급 경계는 Mock결과와 분리 보고.

### 5.3 배포 순서

scope OFF 코드 배포 → index/control 진단·승인된 필요 이관 → 구writer 종료 확인 → capture/source·시간동기화·Billing연동 점검 → 대상source설정 → 별도scope ON 승인. DB복원은 별도 격리·재진단 후 재개하며 자동구매허용으로 처리하지 않는다.

## 6. 부록 — 확인 근거 및 이번 작업 범위

- [구매 활성화 조사](TMI-203-purchase-activation-review-2026-10-13.md)
- [TMI-200 기존 계획](TMI-200-purchase-scope-control-fencing-plan.md): §5.5·5.6·5.7의 이관/구writer/capture/활성화 책임.
- 코드: PurchaseConfiguration, PurchaseProperties, PurchaseTokenFence, PurchaseControlService, PurchaseControlMigration, CaptureReadiness, WithdrawalCaptureConfiguration.withdrawalPurchaseReadiness.
- 공식 Jira TMI-200·203 설명 조회 후 설계 작성. 원격 쓰기 없음.
- 이번은 설계 문서와 작업 기록만 변경, Gradle 미실행. 직전 조사 대상38개 통과는 새source binding 구현/검증 결과가 아니다.
- 실제 키·환경변수 주입·DB쓰기·외부메시지·commit/push·배포 없음. 제품 권한·외부API 변경을 실행하지 않았다.
