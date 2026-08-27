# Firebase 인증 후속 구현 순서

이 문서는 Firebase/SNS 인증을 운영에 안전하게 활성화하기 전에 진행할 후속 작업의 고정 순서를 정의한다.

아래 작업은 원칙적으로 번호 순서대로 구현한다. 각 단계의 코드·테스트·운영 계약이 완료되기 전에는 다음 단계의 production 기능을 활성화하지 않는다. 다른 저장소에서 구현하는 항목도 선행 단계 완료 여부를 동일하게 확인한다.

## 구현 체크리스트

1. [ ] [Firebase/SNS 탈퇴 재인증과 withdrawal lifecycle](firebase-withdrawal-lifecycle-stage-1-plan.md)
2. [ ] [Firebase disable·refresh revoke·delete worker](firebase-withdrawal-external-cleanup-stage-2-plan.md)
3. [ ] [FirebaseIdentity·SocialIdentity·PhoneIdentity release와 `CLEANED` 재가입 gate](firebase-withdrawal-identity-release-stage-3-plan.md)
4. [ ] [탈퇴 Session 전용 오류와 모바일 logout·안내 UX 계약](withdrawal-session-mobile-ux-stage-4-plan.md)
5. [ ] [`UserWithdrawn` event와 downstream Access Token deny marker](user-withdrawn-downstream-deny-marker-stage-5-plan.md)
6. [ ] 가입 중단 Firebase User cleanup
7. [ ] Billing 최소 Entitlement consumer 배포
8. [ ] logout-all Firebase refresh revoke
9. [ ] Refresh Token 응답 유실 복구와 rotation 원자성 개선
10. [ ] Provider unlink와 전화번호 변경
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
- 7단계는 Billing 저장소에서 consumer를 먼저 배포한 뒤 Identity eligibility publisher를 활성화하는 순서를 포함한다.
- 8~9단계는 정상 로그아웃·네트워크 재시도가 세션 재생성 또는 전체 세션 유실로 이어지지 않게 한 뒤 완료한다.
- 10~12단계는 자동 merge 없이 fresh proof와 명시적 사용자 행위로만 인증수단을 변경·복구하도록 구현한다.
- 각 단계에는 정상 흐름뿐 아니라 동시 요청, 외부 성공 후 내부 실패, 내부 성공 후 응답 유실, retry와 reconciliation 테스트를 포함한다.
- 전체 선행 조건과 staging E2E가 완료되기 전에는 Firebase·Guest merge·eligibility publisher 관련 production flag를 활성화하지 않는다.

## 완료 기준

각 체크 항목은 다음 조건을 모두 충족했을 때만 완료로 변경한다.

- 책임 서비스와 데이터 소유권이 계약 문서에 명시됨
- API·event·오류·멱등성과 상태 전이 계약이 확정됨
- 비즈니스 로직과 실패·동시성 테스트가 통과함
- 필요한 consumer가 producer보다 먼저 배포됨
- staging에서 실제 Firebase/mobile, MongoDB Transaction과 서비스 간 재시도 흐름을 검증함
- 운영 feature flag 활성화·중단·reconciliation 절차가 준비됨
