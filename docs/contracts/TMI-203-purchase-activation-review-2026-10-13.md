# Identity 구매 활성화 조건 확인

- 날짜: 2026-10-13 / 브랜치: feat/TMI-203-withdrawal-snapshot-feed-recovery
- 관련: TMI-203·TMI-200·TMI-202 / 범위: 로컬 정적 조사 및 기존 회귀. 코드·설정·운영 DB 변경 없음.

## 1. 5줄 결론

1. Identity의 구매 활성화는 결제를 직접 실행하는 것이 아니라 사용자 JWT에 `billing:purchase`를 합성할 수 있게 하는 것이다.
2. 계정별 ACTIVE MEMBER/User-control 일치·발급 transaction·TTL·replay 보호는 구현되어 있다.
3. 전역 준비 조건의 RuntimeEvidence/PurchaseActivationEvidence 실제 production 제공자는 src/main에서 찾지 못했다.
4. 따라서 현재 기본 코드 경로에서 플래그만 ON으로 바꾸면 준비 검사에서 기동이 실패한다. 운영 배포값은 확인하지 않았다.
5. 다음은 이 두 준비 인터페이스의 책임을 DB 검사·운영 전제로 구분해 연결하는 설계다. 상시true나 구매 보호 제거는 이번 조사 결과가 아니다.

## 2. 사용자가 반드시 읽어야 하는 내용

### 전역 설정과 현재 빈 연결

| 조건 | 현행 동작/상태 |
| --- | --- |
| AUTH_PURCHASE_CONTROL_WRITES_ENABLED | 신규/상태변경 control 기록 준비. 단독ON으로 구매권한 부여하지 않음 |
| AUTH_PURCHASE_FENCE_ENABLED | control writer와 AUTH_SESSION_FENCE_ENABLED 필요. 발급/탈퇴 경합 경계 설치 |
| AUTH_PURCHASE_SCOPE_ENABLED | fence·세션보호·PurchaseReadiness가 모두 준비됐을 때만 기동 허용 |
| Access Token TTL | scope ON 시 양수·최대30분 |
| WITHDRAWAL_CAPTURE_REQUIRED | 기본 PurchaseReadiness bean 생성 조건. 정상 capture용 DB/인덱스/설정 필요 |
| RuntimeEvidence | captureReady와 clockTrusted 모두true 요구. 인터페이스와 소비처는 있지만 production 제공자 없음 |
| PurchaseActivationEvidence | 현재 ACTIVE source streamId에 verifiedFor=true 필요. 구현 제공자 없음 |

`application.yml`의 purchase 세 플래그와 capture.required 기본값은 false다. 이는 저장소 기본값이지 실제 ECS 설정 확인 결과가 아니다. 커스텀 외부 모듈이 공급할 가능성까지 운영 조회 없이 부정하지 않는다.

최근 추가한 CaptureReadiness는 저장/복구의 단일ACTIVE·DB상태 검사이며 **구매 활성화에 자동 연결되어 있지 않다.** 새 source 검사가 있다고 PurchaseActivationEvidence까지 충족된 것으로 처리하지 않는다.

### 사용자별 실제 발급 조건

- 입력/config에서 온 `billing:purchase`는 먼저 제거한다. Guest는 해당 권한을 받지 못한다.
- MEMBER에 대해 scope ON·준비검사·TTL을 확인하고 DB의 User가 ACTIVE인지 검사한다.
- UserSessionControl이 존재하고 version이 있으며 사용자 상태/유형과 일치해야 한다. 누락된 control을 발급 중 자동 생성하지 않는다.
- 같은 transaction에서 control을 갱신해 탈퇴와 경합시키며 원래 만료시각을 추적한다. commit 결과 불명은 성공으로 보고하거나 새 서명을 반복하지 않는다.
- 기존 구매권한 응답 replay도 준비 조건과 control을 재확인한다. 신규 scope OFF만으로 이미 발급된 응답의 보호를 우회하지 않는다.

scope ON 후 readiness가 실패하면 해당 MEMBER 발급은 오류가 될 수 있다. 구매권한만 조용히 빼고 로그인 성공으로 처리하는 설계가 아니다.

## 3. 사용자가 결정해야 하는 사항

이번에는 구매를 켤지 결정할 필요가 없다. 먼저 미구현 증명 interface가 의미하는 조건을 분해해 **코드가 확인할 DB/config 조건**과 **일반 배포·복원 운영 전제**로 정리하는 것을 권장한다. 별도 승인 저장소/갱신 시스템 재도입은 권장하지 않는다.

구매 권한 발급 허용과 실제 Billing 판매·환불·권리회수 준비는 별개다. Identity의 readiness가true라는 이유로 실제 판매를 자동 승인하지 않는다.

## 4. 주요 위험과 미확인

- 실DB control 이관 여부·인덱스·현재 활성 source·실제 TTL/과거 토큰·운영 writer 상태는 조회하지 않았다.
- PurchaseControlMigration은 최대100건 명시 batch/dry-run/apply 도구다. 자동 startup migration이나 실제 완료 증명은 없다. 충돌이 있으면 재조사해야 하며 cursor 끝이 곧 준비완료라는 뜻이 아니다.
- 구매 readiness의 ACTIVE 조회는 local CaptureReadiness의 단일ACTIVE/cutover 검사를 직접 재사용하지 않는다. 후속 연결 시 scope 및 DB 불변식 중복/누락을 점검해야 한다.
- RuntimeEvidence의 발신 시간 정책 개정은 Billing 인계 중이며 구매의 동일이름 의존성을 함께 삭제할 권한으로 확대하지 않는다.
- 현재 코드가 임의 구매권한을 발급한다는 취약점 보고가 아니라, 아직 켤 수 없는 준비 연결 공백의 확인이다.

## 5. 권장 후속 작업

1. 실제 필요한 활성화 조건을 문서/코드 기준으로 분해: control 준비·capture/source·발급/세션보호·운영 전제.
2. 필요한 DB 검사는 재사용하고 운영 전제는 배포절차로 명시하되, 증명 interface를 true로 채워 완료 처리하지 않는다.
3. 준비 없는ON 기동실패, 정상 준비ON, Guest/탈퇴/누락control 거절, 준비 상실·replay·동시탈퇴 회귀를 추가/유지한다.
4. 실제 test 환경 dry-run과 Billing 공동 검증 후 별도 승인으로 scope 활성화한다. 이번에는 이관·판매·설정 변경을 수행하지 않았다.

## 6. 부록 — 코드·테스트 근거

- `src/main/java/web/tosunsaeng/identity/domain/auth/purchase/PurchaseConfiguration.java`, `PurchaseProperties.java`: 설정/TTL/readiness 검증.
- 같은 패키지 `PurchaseTokenFence.java`, `PurchaseControlService.java`, `PurchaseControlMigration.java`: 발급·replay·DB검사·이관.
- `src/main/java/web/tosunsaeng/identity/domain/user/withdrawalcapture/WithdrawalCaptureConfiguration.java`: 실제 PurchaseReadiness bean과 두 증명 인터페이스.
- `src/main/java/web/tosunsaeng/identity/global/security/jwt/JwtAccessTokenIssuer.java`: 입력권한 제거·서버합성.
- `src/main/resources/application.yml`: 기본OFF 및 환경변수.
- 조사 시 `src/main` 전체 검색에서 별도 RuntimeEvidence/PurchaseActivationEvidence 구현 또는 공급 bean은 발견하지 못했다. 테스트의 mock/true readiness는 운영 구현이 아니다.
- 실행 명령: `env -u SENTRY_AUTH_TOKEN ./gradlew test --tests 'web.tosunsaeng.identity.domain.auth.purchase.*' --tests 'web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalConfigurationTests'`. 최종 결과는 작업 기록 참조. 전체 clean test 또는 실제DB 검증으로 과장하지 않는다.
