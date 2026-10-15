# Phone-only 정리 사전 점검 — 부분 구현

## 1. 5줄 결론

1. 사용자 지정 범위는 prod DB 기준의 미연결 phone-only이며 test DB는 조회하지 않는다.
2. 생성/로그인/토큰 갱신 중 최근 활동부터 30분 보호하고 5분마다 최대 100명을 점검한다.
3. SNS/알 수 없는 provider, 이메일, custom claims, 비활성 계정 및 prod 참조가 있으면 후보에서 제외한다.
4. 이번 코드는 읽기 전용 후보 점검이다. **Firebase 삭제·비활성화·세션 폐기는 구현하지 않았다.**
5. 전체 테스트는 성공했으나 실제 Firebase/prod 조회·삭제 정합성·배포 검증 완료가 아니다.

## 2. 반드시 읽어야 하는 내용

`CANDIDATE`는 삭제 허가가 아니다. 개별 스냅샷을 확인한 결과일 뿐 이후 인증/연결/가입 상태가 변할 수 있다. 자동 삭제 실행부의 보호·중단 복구가 연결되기 전에는 이 기능을 자동 정리 완료로 표시하지 않는다.

점검은 기본 OFF이며 `prod` 프로필과 `app.phone-only-cleanup.audit-enabled=true`를 모두 만족해야 실행된다. 환경변수 표기는 `APP_PHONE_ONLY_CLEANUP_AUDIT_ENABLED`다. ON이어도 삭제하지 않는다. 새로운 공개 API는 없다. 기존 미완료 가입 정리와 24시간 grace 설정은 변경하지 않는다.

## 3. 결정/남은 작업

- 30분 보호·5분 점검·prod 단독 참조 판정은 사용자 지정 방향이다.
- 자동 삭제 실행부와 가입 경합 보호, Firebase 오류/응답 유실 복구 및 운영 검증은 미완료다.
- 점검 결과만 보고 계정을 일괄 삭제하지 않는다. 이 브랜치를 prod 자동 삭제 릴리스로 사용하지 않는다.

## 4. 위험과 미확인

- 100명씩 분할하므로 전체 점검 주기는 계정 수에 따라 5분보다 길다. 30~35분 삭제를 보장하지 않는다.
- 페이지 커서는 프로세스 메모리이며 재시작하면 처음부터 조회한다. 중복 점검은 읽기만 하므로 계정 상태에 영향을 주지 않는다.
- 로그는 페이지 집계만 기록한다. UID/전화번호/provider subject/credential/예외 본문을 기록하지 않는다.
- 현재 audit는 Firebase metadata만 사용한다. 정책 클래스의 recoveryActivityAt 입력은 감사 adapter에서 아직 연결되지 않았다. OTP 완료 이전 시도는 metadata로 관측할 수 없다.
- 회원·enrollment·번호 소유·기존 cleanup/withdrawal/provider 변경/logout 참조를 상태 제한 없이 보호한다. 보수적이라 이미 끝난 기록도 후보를 제외할 수 있다.
- retained 전화 fingerprint 키 범위 밖 과거 소유 이력 및 전체 보안 상태를 완전히 증명한 것은 아니다. 따라서 audit 결과를 삭제 권한으로 사용하지 않는다.

## 5. 구현과 근거

- [정책](../../src/main/java/web/tosunsaeng/identity/domain/auth/accountrecovery/PhoneOnlyCleanupPolicy.java): provider 정확 일치, prod 보호 상태, 30분 경계 및 미래/미확인 시각 제외.
- [감사](../../src/main/java/web/tosunsaeng/identity/domain/auth/accountrecovery/PhoneOnlyCleanupAudit.java): bounded list 후 getUser 재조회, prod Mongo 참조 확인, 오류 UNKNOWN 처리.
- [구성](../../src/main/java/web/tosunsaeng/identity/domain/auth/accountrecovery/PhoneOnlyCleanupAuditConfiguration.java): prod 전용/기본 OFF, 5분 initial/fixed delay.

## 6. 검증

`env -u SENTRY_AUTH_TOKEN ./gradlew clean test --no-daemon`: 219 suites / 2,155 tests / failures 0 / errors 0 / skipped 29. 신규 policy 12개와 audit 6개 모두 통과. 외부 Firebase/Atlas는 호출하지 않았다. 29 skip은 성공 실행으로 간주하지 않는다.

`git diff --check` 통과. 삭제 없는 점검까지의 부분 구현이며 전체 요청의 완료 근거가 아니다.
