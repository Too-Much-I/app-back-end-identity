# Firebase 인증 후속 구현 순서

이 문서는 Firebase/SNS 인증을 운영에 안전하게 활성화하기 전에 진행할 후속 작업의 고정 순서를 정의한다.

아래 작업은 원칙적으로 번호 순서대로 구현한다. 각 단계의 코드·테스트·운영 계약이 완료되기 전에는 다음 단계의 production 기능을 활성화하지 않는다. 다른 저장소에서 구현하는 항목도 선행 단계 완료 여부를 동일하게 확인한다.

2026-09-08 사용자 승인 예외: Stage 7 production 활성화 완료를 기다리지 않고 Stage 8의 기능 OFF 코드·격리 테스트 개발을 먼저 진행한다. Stage 7·8 운영 활성화 전 검증은 통합 수행하되, 실제 production 활성화는 staging E2E·Firebase/mobile·Mongo·권한 검증과 consumer 준비 순서 확인 후에만 허용한다.

## 구현 체크리스트

1. [ ] [Firebase/SNS 탈퇴 재인증과 withdrawal lifecycle](firebase-withdrawal-lifecycle-stage-1-plan.md)
2. [ ] [Firebase disable·refresh revoke·delete worker](firebase-withdrawal-external-cleanup-stage-2-plan.md)
3. [ ] [FirebaseIdentity·SocialIdentity·PhoneIdentity release와 `CLEANED` 재가입 gate](firebase-withdrawal-identity-release-stage-3-plan.md)
4. [ ] [탈퇴 Session 전용 오류와 모바일 logout·안내 UX 계약](withdrawal-session-mobile-ux-stage-4-plan.md)
5. [ ] [`UserWithdrawn` event와 downstream Access Token deny marker](user-withdrawn-downstream-deny-marker-stage-5-plan.md)
6. [ ] [가입 중단 Firebase User cleanup](firebase-abandoned-enrollment-cleanup-stage-6-plan.md)
7. [ ] [Billing SigV4 eligibility와 owner event durable fan-out](billing-entitlement-owner-fanout-stage-7-plan.md)
8. [ ] [logout-all Firebase refresh revoke](firebase-logout-all-revoke-stage-8-plan.md)
9. [ ] [Refresh Token 응답 유실 복구와 rotation 원자성 개선](refresh-token-response-recovery-stage-9-plan.md) — TMI-130 코드·격리 테스트 구현, [운영·모바일 검증](refresh-token-response-recovery-stage-9-runbook.md) 대기
10. [ ] [Provider unlink](firebase-provider-unlink-stage-10-plan.md) — TMI-131 서버 구현·격리 테스트, [운영/모바일 검증과 STARTED 결과 불명 복구 gate](firebase-provider-unlink-stage-10-runbook.md) 대기. 전화번호 셀프 변경·예외 처리 보류
11. [ ] Guest 생성 응답 유실 복구
12. [ ] 기존 ACTIVE 회원의 Firebase rebind 정책

## 순서 적용 원칙

- 1~3단계는 완전 탈퇴와 동일 SNS·전화번호 재가입을 위한 하나의 lifecycle 묶음이다.
- 4단계는 `ACCOUNT_WITHDRAWN`으로 폐기된 RefreshSession을 일반 invalid token과 구분하는 전용 외부 오류를 정의한다. 모바일은 이 오류를 받으면 Firebase client signOut, 자체 Access·Refresh Token 전체 삭제와 “탈퇴 처리된 계정입니다” 안내를 수행한다.
- 5단계는 Identity의 `UserWithdrawn` outbox·publisher보다 Learning Core의 eventId 멱등 consumer와 userId 기반 로컬 deny gate를 먼저 배포한다. Learning Core는 매 요청 Identity introspection을 호출하지 않고 JWKS 검증 뒤 자기 저장소의 marker를 확인한다.
- deny marker의 `blockedUntil/expireAt`은 `withdrawnAt + 시스템이 허용하는 최대 Access Token 수명 + verifier clock skew 안전 여유`로 계산하고 TTL로 정리한다. TTL 삭제 지연에 의존하지 않도록 요청 gate는 `blockedUntil`을 직접 검사한다.
- event inbox는 source outbox의 최대 자동 재전달·dead-letter·운영 수동 replay 기간보다 길게 보존한 뒤 deny marker와 별도의 TTL로 정리한다. Token 원문이나 Provider credential을 event·marker·inbox에 저장하지 않는다.
- 1~5단계와 양 서비스·모바일 staging E2E가 완료되기 전에는 Firebase/SNS withdrawal production flag를 활성화하지 않는다.
- 6단계가 완료되기 전에는 phone을 연결하고 가입을 중단한 Firebase User의 자동 cleanup을 활성화하지 않는다.
- 7단계는 7-A Identity→Billing SigV4 기반, 7-B Billing owner rebind·phone continuation, 7-C Identity owner event durable fan-out, 7-D1 Learning Core phone continuation, 7-D2 Learning Core `UserMerged` consumer, 7-E staging E2E·canary 순서로 진행한다. Billing 7-B는 `develop` 병합됐고 Learning Core 7-D1은 구현·검증 후 `develop` 병합 대기 상태다.
- `UserMerged`는 Billing과 Learning Core에 각각 전달하고, 동일 phone proof 기반 `TrialOwnerRebindApproved`는 Billing에만 전달한다. phone proof만으로 Learning Core 시험·결과를 이전하지 않는다.
- phone 재가입의 없음·`OPEN`·`RETAKE_AVAILABLE`은 Billing owner rebind 대상이며, Learning Core는 Billing의 exact continuation을 조회해 source 기록을 이전하지 않고 target 명의의 새 Session을 만든다. `GRADING`은 terminal까지 retry하고 `COMPLETED`는 새 무료권 없이 NOOP다.
- 반복 탈퇴·재가입에서는 직전 AVAILABLE lineage를 successor 가입이 CONSUMED하고 successor 탈퇴가 새 AVAILABLE lineage를 만든다. 과거 CONSUMED lineage가 여러 건인 것은 정상이며 동일 benefit scope의 AVAILABLE predecessor가 여러 건일 때만 자동 이전을 중단한다. 서로 다른 benefit scope는 독립 처리한다.
- Billing owner event는 consumer별 단조 sequence로 직렬화하고, exact predecessor가 PUBLISHED되기 전 후속 owner event를 전송하지 않는다. phone 재가입 source lineage가 없거나 모호하면 가입은 허용하되 자동 권리 이전은 하지 않는다.
- 8~9단계는 정상 로그아웃·네트워크 재시도가 이전 인증의 무단 세션 재생성 또는 의도하지 않은 전체 세션 유실로 이어지지 않게 한 뒤 완료한다.
- 8단계 승인 정책(2026-09-08): Firebase 폐기 대기·결과 불명 중에도 새 인증 로그인은 허용하고, 지연 폐기에 영향받은 Firebase 기반 자체 세션만 추가 무효화한다. 결과 불명 mutation 자동 재전송은 하지 않고 확인 조회·운영 조사와 로그인 허용을 분리한다. 기존 200은 자체 보안 처리+durable 접수이며 같은 검증 jti는 같은 요청이다. 기존 탈퇴/release 보호 및 선행 운영 gate는 유지한다. 상세 데이터·모바일·검증 조건은 Stage 8 계획서를 따른다.
- 10~12단계는 자동 merge 없이 fresh proof와 명시적 사용자 행위로만 인증수단을 변경·복구하도록 구현한다.
- 각 단계에는 정상 흐름뿐 아니라 동시 요청, 외부 성공 후 내부 실패, 내부 성공 후 응답 유실, retry와 reconciliation 테스트를 포함한다.
- 전체 선행 조건과 staging E2E가 완료되기 전에는 Firebase·Guest merge·eligibility publisher 관련 production flag를 활성화하지 않는다.

## Stage 10 승인 정책 (2026-09-10)

사용자는 제시된 선택지 중 2번 B, 나머지 1·3·4·5번 A를 승인했다. 이번 확정은 제품 정책과 개발 범위이며 구현 완료를 의미하지 않는다.

1. 전화번호 셀프 변경 API·화면은 초기 버전에서 제공하지 않는다. 영구 변경 금지가 아니라 기능 보류이며, 신규 가입의 번호 인증과 기존 탈퇴·가입 중단 cleanup은 유지한다.
2. 실제 번호 변경·재할당에 대한 예외 변경·복구 처리도 당분간 지원하지 않는다. 기존 번호 점유로 신규 소유자의 가입이 막힐 수 있고 현재 이를 해소하는 지원 절차를 제공하지 않는다는 제약을 안내한다. 번호 소유 증명만으로 기존 계정을 넘기거나 다른 User의 번호를 자동 해제하지 않는다. 이 보류는 Provider unlink 작업 자체의 장애 복구·운영 조정을 제외한다는 뜻이 아니다.
3. Provider unlink는 해제 후 남는 허용 로그인 수단으로 최근 재인증한 뒤 요청한다. 마지막 허용 로그인 수단 제거는 금지하며 phone-only는 남는 로그인 수단으로 계산하지 않는다.
4. 연결 해제 시 현재 기기를 포함한 모든 자체 세션을 종료하고 Firebase UID의 refresh revoke를 수행한다. 이후 남은 로그인 수단으로 재인증한다. Firebase revoke는 Provider별 선별 폐기가 아니며 이미 발급된 Access Token의 downstream 즉시 차단을 보장하는 정책도 아니다.
5. 외부 처리는 진행 상태를 노출하는 비동기 방식으로 설계한다. Firebase·Identity 처리가 끝나기 전에 완료로 표시하지 않으며 부분 실패·결과 불명은 저장된 작업을 바탕으로 확인·복구한다. 결과 불명 mutation을 무조건 재전송한다는 의미는 아니다.

유지할 안전 조건과 상세 계획의 후속 항목:

- User와 사용자 기록은 유지한다. 다른 owner와 자동 merge하지 않는다.
- 오래된 Firebase proof나 auth methods sync가 해제한 연결을 자동 복원하지 못하게 한다. 재연결은 명시적 사용자 행위와 새로운 인증 증거를 요구한다.
- 2026-09-11 최종 승인·구현: 최초 연결과 재연결을 공통 prepare/start/complete/status로 통일한다. PREPARED는 비차단 만료, STARTED는 실행 종료 확인 전 보호 상태 유지. 기존 sync 신규 저장과 relink 전용 prepare를 폐기한다. 프론트·운영 전환 gate는 Stage 10 runbook을 따르며 feature flag 기본 OFF다.
- 전화번호 변경 화면이 없어도 Firebase 외부 번호 변경은 발생할 수 있으므로, 불일치 자동 수용 금지 및 영향 경로 검증을 상세 계획에 포함한다. 변경 API 보류를 이유로 가입·탈퇴 내부 PhoneIdentity 기반을 제거하지 않는다.
- 전체 세션 종료 뒤 처리 상태 조회 권한, 남는 수단 재로그인 및 지연 revoke의 영향, 재인증 유효기간, mutation 순서·완료 조건·retry·운영 조정은 상세 계획에서 정의한다. 기존 Stage 8 정책을 새 unlink lifecycle에 무조건 그대로 적용하지 않는다.
- Billing의 전화번호 변경용 혜택 조정은 이번 범위에서 제외한다. 기존 가입·탈퇴·재가입의 혜택 계약은 유지한다.

## 완료 기준

각 체크 항목은 다음 조건을 모두 충족했을 때만 완료로 변경한다.

- 책임 서비스와 데이터 소유권이 계약 문서에 명시됨
- API·event·오류·멱등성과 상태 전이 계약이 확정됨
- 비즈니스 로직과 실패·동시성 테스트가 통과함
- 필요한 consumer가 producer보다 먼저 배포됨
- staging에서 실제 Firebase/mobile, MongoDB Transaction과 서비스 간 재시도 흐름을 검증함
- 운영 feature flag 활성화·중단·reconciliation 절차가 준비됨
