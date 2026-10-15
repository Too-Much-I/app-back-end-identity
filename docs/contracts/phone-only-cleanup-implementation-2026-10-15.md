# Phone-only 정리 — prod 보호·운영 규칙

## 1. 5줄 결론

1. prod DB만 참조하여 미연결 phone-only를 판정한다. test DB는 사용자 결정으로 제외한다.
2. 생성/로그인/토큰 갱신 중 최근 활동부터 30분 보호하고 5분마다 최대 100명을 점검한다.
3. SNS/알 수 없는 provider, 이메일, custom claims, 비활성 계정 및 prod 참조가 있으면 제외한다.
4. 가입 준비와 정리 claim은 같은 Mongo fence를 쓰는 Transaction으로 순서화한다.
5. 삭제는 별도 기본 OFF 스위치로 제어한다. 실패/상태 변경/응답 불명은 보류하며 자동 강행하지 않는다.

## 2. 반드시 읽어야 하는 내용

점검: `FIREBASE_PHONE_ONLY_CLEANUP_AUDIT_ENABLED=true`.
삭제: `FIREBASE_PHONE_ONLY_CLEANUP_DELETE_ENABLED=true`.
둘 다 기본 false이며 prod 프로필에서만 실행된다. 첫 실행과 페이지 간격은 5분이다. 점검만 ON이면 Firebase 변경은 없다. 공개 API/JWT/기존 미완료 가입 정리의 24시간 grace는 바뀌지 않는다.

삭제 경로: 최신 후보 조회 → prod 참조 확인과 fence claim → Firebase 재조회 → disable → refresh revoke → provider/전화번호/생성시각/최근 활동/prod 참조 재확인 → 단건 delete → USER_NOT_FOUND로 삭제 확인.

claim 이후에는 그 UID에 새 enrollment를 만들지 않는다. 먼저 만들어진 enrollment는 상태와 무관하게 정리에서 제외한다. capture ON/OFF 두 enrollment 경로 모두 같은 fence를 사용한다. 오래된 서버는 이 fence를 사용하지 않으므로 **새 코드의 prod 배포 완료 및 구 task 종료를 확인한 뒤 삭제 ON**한다.

## 3. 결정과 범위

- SNS-only/SNS+phone 계정은 제외한다. Firebase UID와 Identity userId를 합치거나 이전하지 않는다.
- 기존 회원/enrollment/번호 소유/cleanup·withdrawal·provider 변경/logout 참조를 상태 제한 없이 보호한다.
- OTP 완료 전 재인증 시도는 관측하지 못해 사용자가 계정 찾기를 다시 시도해야 할 수 있다. 사용자와 합의한 제한이다.
- test 전용 phone-only는 prod에 참조가 없으면 후보가 될 수 있다. 공유 Firebase의 test 데이터를 보호 대상으로 삼지 않는 사용자 지정 범위다.

## 4. 보류·복구·보존

- `CHANGED_BEFORE_DISABLE`: 계정 상태 변경 감지. 삭제/disable 하지 않고 fence 보류.
- `CHANGED_AFTER_DISABLE`: disable 이후 상태 변경 감지. 삭제하지 않는다. 계정이 비활성 상태일 수 있으며 자동 재활성화하지 않는다.
- `RESULT_UNKNOWN`/`DELETE_NOT_CONFIRMED`: 외부 결과 불명. 자동 반복 삭제하지 않고 보류한다.
- 보류 재개 API는 추가하지 않는다. 관리자가 현재 Firebase 및 prod 기록을 확인하고 UID별 fence와 disabled 상태를 함께 검토한다. 임의 fence 삭제/전체 재활성화 금지.
- fence `_id`는 tenant/project/UID의 SHA-256 조합이며 전화번호/원문 token을 저장하지 않는다. 일반 enrollment/삭제 완료 기록은 갱신 후 24시간 TTL, 미해결 blocked 기록은 자동 만료하지 않는다. 장기 보류는 운영 확인 대상이다.
- 이미 비활성인 계정은 새 대상이 아니다. 비활성화 이후 실패한 계정은 `phone_only_cleanup_fences`의 blocked/outcome과 집계 로그로 확인한다.

## 5. 주요 한계와 배포 전 확인

- 페이지가 100명이라 전체 sweep은 5분 이상 걸릴 수 있다. 모든 계정의 30~35분 삭제를 보장하지 않는다. 커서는 프로세스 메모리이며 재시작하면 처음부터 조회한다.
- retained fingerprint 키 밖의 과거 소유 이력은 복원하지 않는다. 삭제 전 운영 참조 상태·키 보존 범위를 확인한다.
- 관리자/Admin SDK의 외부 동시 변경은 앱의 가입 fence를 따르지 않는다. 운영 수동 계정 변경 중에는 삭제 OFF.
- 기존 Firebase 프로젝트/tenant의 read/update/delete 권한 필요. 권한 부족 시 자동 확대하지 않는다.
- rollout: 새 코드 delete OFF → 구 task 종료/정상 기동/읽기 점검 확인 → 승인된 delete ON. 롤백 시 삭제 OFF를 먼저 적용하고 보류 fence는 보존한다.

## 6. 근거와 검증

- [정책](../../src/main/java/web/tosunsaeng/identity/domain/auth/accountrecovery/PhoneOnlyCleanupPolicy.java)
- [점검](../../src/main/java/web/tosunsaeng/identity/domain/auth/accountrecovery/PhoneOnlyCleanupAudit.java)
- [삭제](../../src/main/java/web/tosunsaeng/identity/domain/auth/accountrecovery/PhoneOnlyCleanupDeletion.java)
- [공유 fence](../../src/main/java/web/tosunsaeng/identity/domain/auth/accountrecovery/PhoneOnlyCleanupFence.java)
- [가입 연결](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseEnrollmentAttemptService.java)

로컬 Docker replica set에서 먼저 가입/먼저 삭제/동시 실행/rollback/완료 차단 유지 테스트를 수행한다. `PHONE_CLEANUP_LOCAL_MONGO=true`는 고정 localhost 합성 DB만 사용한다. 외부 Firebase/Atlas를 테스트에서 호출하지 않는다. 최종 실행 결과·운영 배포 여부는 CURRENT_STATE와 WORKLOG를 따른다.
