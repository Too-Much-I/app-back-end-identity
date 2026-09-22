# 프론트 Firebase·SNS 연동 부록 — 배포·QA·구버전 참고

- 기준일: 2026-09-21 (TMI-169 Guest 가입 재개 QA 반영)
- 주 문서: [프론트 API 명세](frontend-firebase-auth-integration-guide.md)
- API 요청·응답과 앱 처리 규칙은 주 문서를 먼저 읽는다. 이 문서는 배포 확인, QA, 구버전 참고 자료다.

## 5줄 결론

1. 코드가 구현돼 있어도 해당 환경에서 활성화·검증됐다는 뜻은 아니다.
2. 서버 주소·지원 SNS·약관 버전·업데이트 기준을 출시 전에 확정한다.
3. Google과 같은 Firebase UID의 phone link부터 검증하고 실제 Provider 검증을 이어간다.
4. 응답 유실·중단·동시 요청 등 실패 사례도 QA에 포함한다.
5. 구버전 Guest·이메일 예시는 호환 참고이며 신규 앱에서 사용하는 기능이 아니다.

<a id="deployment"></a>

## 출시 전 확인사항

### 제공 범위와 미확정 항목

| 구분 | 현재 기준 |
| --- | --- |
| 신규 회원 | SNS → Firebase exchange → same-UID phone link → signup. 임시 Guest 생성 없음 |
| 기존 Guest | 기존 Identity Token을 유지하고 prepare → upgrade 또는 merge |
| 학습 기록만 삭제 | 프론트 추가 요구사항. 계정·로그인 유지, Learning Core 담당. 삭제 범위·API·배포 상태 미확정 |
| 신규 Guest | 신규 앱에서는 제공하지 않음. 서버 `/auth/guest`는 코드에 남아 있으며 이번 문서 작업에서 차단하지 않음 |
| Guest 최초 응답 복구 | 미구현·취소. 새 복구 헤더/API를 구현하지 않음 |
| SNS 추가/재연결 | link prepare/start/complete/status 공통 흐름. sync로 신규 연결 금지 |
| 전화번호 변경 | 셀프 변경 및 번호 재할당 예외 변경 보류 |
| 기존 이메일 회원 이전 | 대상 없음. 신규 이메일 로그인 화면 추가도 이번 인계 범위 아님 |
| 공개 정책·capability API | 현재 없음. 아래 공급 방식 확정 전 존재를 가정하지 않음 |

출시 담당자가 확정해 프론트에 전달할 값: 환경별 Identity/Learning Core base URL, Firebase project/app 설정, 실제 지원 Provider, 약관 URL·현재 버전, 최소 지원 앱 버전·업데이트 링크, 활성 feature 목록. Guest 승격의 두 필수 version은 prepare 응답으로 공급한다. 약관 본문·URL과 direct 신규 가입의 version 공급은 앱 빌드 설정 또는 합의된 원격 설정 등으로 별도 확정하며, 공개 공급 API가 있다고 가정하지 않는다. 이 문서의 `privacy-v1` 등은 예시이며 배포 서버 설정과 일치해야 한다.

아직 미확인인 항목은 실제 모바일 Google/Apple/Kakao redirect, phone link/SMS, Mongo Transaction, 비동기 Billing/Learning Core 전달, 키 공급/회전이다. 모든 기능은 해당 환경의 검증된 범위만 노출한다.

## 기능별 활성화와 QA 인계

### 서버 구현과 배포 설정 구분

아래 설정은 백엔드 담당자 확인용이며 앱이 환경변수를 직접 읽거나 변경하는 계약이 아니다. 표의 대표 flag만 켜면 충분하다는 뜻도 아니다. 키·Transaction·consumer·worker 등의 의존 조건을 함께 확인한다.

| 기능 | 대표 서버 설정 (저장소 기본 OFF) | 앱 노출 조건 |
| --- | --- | --- |
| Firebase 인증 | `FIREBASE_AUTH_ENABLED`, Provider별 `FIREBASE_GOOGLE_ENABLED`/`FIREBASE_APPLE_ENABLED`/`FIREBASE_KAKAO_ENABLED`/`FIREBASE_PHONE_ENABLED` | 프로젝트·SDK·Provider·가입 phone 설정 검증 |
| Guest merge | `GUEST_MERGE_ENABLED` | 기존 Guest 보호와 Billing/Learning Core 전달·이전 검증 |
| 안전한 세션 처리 | `AUTH_SESSION_FENCE_ENABLED` | epoch·회원 전환·로그아웃 경합 검증 |
| 재발급 응답 복구 | `AUTH_REISSUE_RECOVERY_ENABLED` | 암호화 keyring·Mongo·모바일 pending 복구 검증 |
| 전체 로그아웃 Firebase revoke | `FIREBASE_LOGOUT_ALL_CAPTURE_ENABLED`, `FIREBASE_LOGOUT_ALL_WORKER_ENABLED` | remote revoke·지연 처리·운영 대응 검증 |
| 탈퇴/정리 | `FIREBASE_WITHDRAWAL_ENABLED`, `FIREBASE_WITHDRAWAL_CLEANUP_ENABLED`, `FIREBASE_WITHDRAWAL_IDENTITY_RELEASE_ENABLED` | 실제 외부 삭제·내부 release·재가입 검증 |
| SNS 연결 변경 | `FIREBASE_PROVIDER_CHANGE_FENCE_ENABLED`, `FIREBASE_PROVIDER_LINK_ENABLED`, `FIREBASE_PROVIDER_UNLINK_ENABLED`, `FIREBASE_PROVIDER_UNLINK_WORKER_ENABLED` | link/unlink 상태·중단 복구·실제 Provider 검증 |

전화번호 셀프 변경, 신규 Guest 응답 복구, 기존 LOCAL migration은 이번 출시 필수 구현으로 요구하지 않는다. 같은 Firebase UID의 Provider 연결과 UID 자체의 교체는 다르며 공개 rebind는 제공하지 않는다.

<a id="integration"></a>

### 첫 통합 검증 순서

1. 테스트 Firebase 프로젝트·Android/iOS 앱 연결, Google 로그인 확인.
2. `/exchange`에서 신규 사용자의 `ENROLLMENT_REQUIRED` 확인.
3. 테스트 전화번호 credential을 같은 Firebase User에 link, UID 유지 및 ID Token 강제 갱신.
4. `/signup` 성공 및 `/users/me`의 MEMBER/userId 확인. 재로그인은 `AUTHENTICATED`여야 함.
5. 기존 Guest Token을 가진 사용자로 승격/병합 확인. 신규 Guest 생성은 새 앱의 흐름에 넣지 않음.
6. 재발급 응답 유실·동시 요청·로그아웃 및 다른 기기의 재발급 거절 확인.
7. SNS 연결/해제 및 앱 중단, 탈퇴·cleanup·동일 SNS 재가입 확인.
8. Apple/Kakao·실제 SMS·서비스 간 비동기 연동을 추가 확인.

Emulator/테스트 번호 통과는 실제 OAuth redirect·국내 SMS·과금/쿼터 검증을 대신하지 않는다. 운영 확인 결과는 이 문서의 구현 사실과 별도로 기록한다.

### 꼭 재현할 실패 사례

| 상황 | 기대 동작 |
| --- | --- |
| Firebase 기능 OFF | 503을 신규 회원 없음으로 오해하지 않음 |
| phone link 중 UID 변경 | 가입 중단, 다른 Firebase 계정에 signup하지 않음 |
| signup/upgrade/merge 응답 유실 | 현재 Firebase/Identity 상태 조회, 임의 새 계정 생성 금지 |
| Guest prepare에서 `IDENTITY_STATE_CONFLICT` | 자동 승격·merge 금지, 재인증 후 반복되면 지원 안내 |
| Guest enrollment 만료 | 기존 ID 폐기 후 `/guest/prepare`에서 새 ID와 requirements 수신 |
| 같은 refresh 요청 응답 유실 | Stage 9 활성 환경에서 같은 요청 ID+원 Token으로만 재시도 |
| link start 응답 유실 | SDK link 실행 허가가 없으면 실행하지 않음 |
| SDK link 후 complete 유실 | 같은 linkAttemptId로 완료 확인, 새 prepare 금지 |
| unlink 접수 후 앱 종료 | 보존한 requestId+남는 SNS 재인증으로 상태 조회 |
| 탈퇴 정리 중 즉시 재가입 | 정리 중 안내, phone/SNS 충돌 우회 금지 |
| 오래된 Guest 응답이 새 로그인 뒤 도착 | 현재 인증 상태를 덮어쓰지 않음 |

<a id="qa"></a>

## 프론트 구현 체크리스트

- [ ] Firebase ID Token과 Identity Access/Refresh Token을 타입 수준에서 분리한다.
- [ ] Identity Access Token만 Learning Core Bearer로 보낸다.
- [ ] Refresh Token을 OS 보안 저장소에 보관한다.
- [ ] reissue를 앱 전체 single-flight로 만들고 원 요청은 한 번만 재시도한다.
- [ ] Stage 9 활성 환경에서 원 Refresh Token과 요청 ID를 안전하게 저장한 뒤 보내고, 응답 유실 재시도에 같은 값을 사용한다.
- [ ] signup/upgrade 전 phone 요건이 있으면 같은 Firebase UID에 phone credential을 link한다. 이미 유효한 proof가 있으면 OTP를 반복하지 않는다.
- [ ] phone link 전후 UID를 비교하고 link 후 ID Token을 강제 갱신한다.
- [ ] `AUTHENTICATED`, `ENROLLMENT_REQUIRED`를 `result.type`으로 분기한다.
- [ ] `missingRequirements`와 `linkedProviders` 배열 순서에 의존하지 않는다.
- [ ] 신규 signup enrollment는 `/exchange`, 기존 Guest 전환 enrollment는 `/guest/prepare`에서 얻는다. 두 binding을 혼용하지 않는다.
- [ ] Guest `MERGE_REQUIRED`에서 사용자 확인을 받는다.
- [ ] Guest prepare의 `missingRequirements`와 privacy/terms version으로 재개 화면을 구성한다.
- [ ] `PROFILE`이 항상 남아 있어도 닉네임 확정 후 upgrade할 수 있으며 빈 requirements를 기다리지 않는다.
- [ ] `CONSENTS`가 없어 화면을 생략해도 upgrade의 필수 동의 boolean 두 개와 version 두 개를 전송한다.
- [ ] 중단 후 재개 시 prepare를 다시 호출하고 phone 요건이 없으면 OTP를 반복하지 않는다.
- [ ] 활성 enrollment 재조회가 TTL을 연장하지 않으며 expiresIn은 밀리초임을 반영한다.
- [ ] 만료 ID는 폐기하고 prepare 결과를 다시 분기한다. recent-auth는 Provider 재인증으로 해결한다.
- [ ] Guest `IDENTITY_STATE_CONFLICT`를 성공으로 취급하거나 자동 복구하지 않는다.
- [ ] logout·withdrawal terminal 처리에서 Firebase signOut 실패와 로컬 Token 삭제를 분리한다.
- [ ] Token·OTP·비밀번호·phone을 log, analytics, crash report에서 제거한다.
- [ ] 신규 가입의 현재 약관 URL/버전 공급을 배포 담당자와 확정한다. 인증 후 동의 조회는 `/users/me/consents`를 사용한다.
- [ ] 환경별 Firebase project, Provider button, backend feature flag를 함께 배포한다.
- [ ] Guest merge는 Billing·Learning Core consumer staging E2E 뒤에만 노출한다.
- [ ] 업데이트 후 기존 Guest Token을 삭제하지 않고, 신규 Guest API를 호출하는 초기화 코드를 제거한다.
- [ ] 최소 지원 버전·업데이트 URL·차단/유도 정책을 확정한다.
- [ ] 최초 연결/재연결 모두 공통 link를 사용하고 start 허가와 상태 조회를 구분한다.
- [ ] unlink 접수 후 원 requestId를 보존하여 자체 Token 없이 남는 Firebase proof로 상태를 조회한다.
- [ ] reissue replay의 절대 만료 헤더를 사용하고 늦은 응답이 최신 로그인/로그아웃 결과를 덮어쓰지 않게 한다.

<a id="guest-resume-qa"></a>

## TMI-169 부록 — 응답 판정·재개 QA·코드 근거

주 문서의 [Guest 재개 규칙](frontend-firebase-auth-integration-guide.md#guest-resume)과 [upgrade 요청](frontend-firebase-auth-integration-guide.md#guest-upgrade-request)을 먼저 적용한다. 아래는 현재 구현을 바탕으로 한 QA 기대값이며 대상 환경 검증 완료를 뜻하지 않는다.

### 응답 필드와 화면 판정

| 필드 | `ENROLLMENT_REQUIRED` | `MERGE_REQUIRED` |
| --- | --- | --- |
| `type` | `ENROLLMENT_REQUIRED` | `MERGE_REQUIRED` |
| `enrollmentId` | Guest 승격에 사용할 UUID | JSON 필드 없음 |
| `missingRequirements` | 아래 enum 집합, 순서에 의존하지 않음 | JSON 필드 없음 |
| `privacyConsentVersion`, `termConsentVersion` | 서버 현재 필수 정책 버전, 항상 제공 | JSON 필드 없음 |
| `expiresIn` | 남은 유효시간, 밀리초 | JSON 필드 없음 |

| 요건 | 서버 판정 | 프론트 충족 방법 |
| --- | --- | --- |
| `EMAIL_VERIFICATION` | PASSWORD가 연결돼 있고 fresh proof의 email 미인증 | email 인증 후 Firebase ID Token 갱신. SNS 이메일 문자열만 보고 추정하지 않음 |
| `PHONE_VERIFICATION` | PHONE 미연결 또는 fresh proof의 phone 미검증 | 같은 Firebase User에 phone link, UID 확인, ID Token 강제 갱신 |
| `PROFILE` | Guest 승격에서는 항상 포함 | 사용자가 MEMBER 닉네임을 확정하고 upgrade에 전달. 서버의 화면 진행 저장이 아님 |
| `CONSENTS` | privacy/terms 각각의 true·현재 version 일치·동의 timestamp 존재 조건 중 하나라도 미충족 | 현재 정책의 필수 동의를 받은 뒤 upgrade에 boolean과 version 전송 |

`CONSENTS`가 없으면 재동의 화면 생략이 가능하지만 요청 네 필드는 계속 필수다. 선택 품질 검토 동의는 이 판정에 영향을 주지 않는다. `/users/me.accountType`은 계정 표시용이고 phone proof나 enrollment 재개 정보의 근거로 사용하지 않는다.

### Guest 재개 QA 시나리오

| 준비 조건 / 수행 | 기대 응답과 앱 처리 |
| --- | --- |
| owner 없음, attempt 없음 | `ENROLLMENT_REQUIRED`, 새 ID와 현재 요건·version 수신 |
| 같은 Guest·Firebase UID의 활성 attempt로 재호출 | 동일 ID와 남은 expiresIn. TTL이 새로 10분으로 연장되지 않음 |
| phone link 전에 앱 종료 후 복귀 | prepare에서 phone 요건 확인 후 필요한 단계 재개 |
| same-UID phone link 완료 후 앱 종료·복귀 | fresh proof로 prepare, phone 요건 없음이면 OTP 생략. 활성 기간이면 ID 유지 |
| 닉네임만 앱에 입력 후 prepare 재호출 | `PROFILE`은 계속 존재. 입력 초안은 앱이 관리하고 최종 upgrade에 포함 |
| 필수 동의가 모두 current + timestamp 존재 | `CONSENTS` 없음. 화면을 생략해도 upgrade 동의 네 필드 포함 |
| 필수 동의 누락·구버전·timestamp 누락 | `CONSENTS` 존재. 현재 약관 제시·동의 후 upgrade |
| prepare 이후 서버 필수 version 변경 | upgrade version mismatch 처리 → prepare 재호출 → 새 version에 필요한 동의 |
| expiresAt과 서버 현재 시각이 같거나 이미 지남 | 만료 ID upgrade는 `409 FIREBASE_ENROLLMENT_CONFLICT`. prepare에서 새 유효 ID 수신 |
| enrollment는 만료됐지만 phone proof는 여전히 유효 | 새 ID 수신, phone 요건 없으면 인증 반복 안 함 |
| 다른 ACTIVE MEMBER owner | `MERGE_REQUIRED`, enrollment 필드 없음. 사용자 확인 후 merge |
| 현재 Guest가 identity owner인 비정상 fixture | `409 IDENTITY_STATE_CONFLICT`. 성공 표시·자동 upgrade/merge 안 함 |
| recent-auth 시간 초과 | Provider 재인증 후 다시 호출. ID Token 강제 갱신만 반복하지 않음 |
| Guest 응답과 MEMBER provider-link 응답 비교 | Guest prepare에는 `ALREADY_LINKED` 없음. MEMBER link의 `ALREADY_LINKED`, `linkAllowed=false`는 유지 |

불가능 상태 fixture·동시 prepare·TTL 경계는 격리 테스트에서 재현한다. 실제 서비스에서 Guest 소유 identity를 임의로 만들어 QA하지 않는다. 정상 prepare 이후 오류가 계속되면 자동 재시도를 중단하며, signup/upgrade/merge 응답 유실은 주 문서 4.5의 완료 여부 확인 절차를 따른다.

### 만료·관측·검증 근거

enrollment 기본 TTL은 `FIREBASE_ENROLLMENT_TTL=PT10M`, signup/upgrade의 recent-auth 기본값은 `FIREBASE_HIGH_RISK_MAX_AUTHENTICATION_AGE=PT5M`이며 별도 조건이다. [설정 원문](../../src/main/resources/application.yml), [Firebase 검증](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/infrastructure/firebase/FirebaseAdminAuthenticationVerifier.java)을 확인한다.

만료는 애플리케이션의 `expiresAt` 판정이며 즉시 문서 삭제가 아니다. `expiresAt` TTL을 추가하지 않고 terminal 처리 후의 기존 `cleanupAt` TTL을 유지한다. 앱은 문서 삭제 시각을 기다리지 않고 prepare 응답을 사용한다. [enrollment 조정](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseEnrollmentCoordinationTransactionService.java), [수명주기](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseEnrollmentLifecycleService.java), [상세 계약](firebase-guest-enrollment-resume-plan.md)을 따른다.

배포 담당자는 `identity.firebase.guest.prepare` counter의 `outcome` 세 값과 conflict WARN을 확인한다. `IDENTITY_STATE_CONFLICT` 0건 여부로 Guest-owned identity 없음 전제를 관측하고, metric·로그에 credential·개인정보·enrollmentId를 넣지 않는다. 대시보드·알림 구성과 실제 환경 0건 확인은 별도 작업이다.

확인된 구현·테스트 근거:

- [prepare 서비스](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseGuestPrepareService.java), [요건 계산기](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseEnrollmentRequirementResolver.java), [DTO](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/dto/response/FirebaseGuestPrepareResponse.java)
- [서비스 테스트](../../src/test/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseGuestPrepareServiceTests.java), [요건 테스트](../../src/test/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseEnrollmentRequirementResolverTests.java), [만료 attempt 테스트](../../src/test/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseEnrollmentAttemptServiceTests.java)
- [Controller 응답 테스트](../../src/test/java/web/tosunsaeng/identity/domain/auth/federation/api/FirebaseExchangeControllerTests.java), [DTO 불변식 테스트](../../src/test/java/web/tosunsaeng/identity/domain/auth/federation/dto/response/FirebaseGuestPrepareResponseTests.java)

구현 작업에서 보고된 전체 테스트는 896개 성공이다. 이번 인계 문서 갱신은 코드 대조·링크·JSON 예시·diff 검사를 대상으로 하며 실제 모바일·배포 E2E 검증을 대신하지 않는다.

<a id="learning-history-delete-scope"></a>

## 학습 기록만 삭제 — Learning Core·제품 담당자 인계 부록

요구사항은 [주 문서 5.4](frontend-firebase-auth-integration-guide.md#learning-history-delete)에 정리한다. 확인된 사실은 Identity의 인증·계정 도메인 경계와 [JWT 사용자 식별 계약](identity-learning-jwt.md)이다. 아래 삭제 범위와 구현 방식은 검토 항목이며 Learning Core의 현재 구현 사실로 간주하지 않는다. 이번 인계에서는 새 삭제 API를 구현하거나 실제 데이터를 삭제하지 않았다.

### 먼저 결정할 데이터 범위

| 항목 | 결정할 사항 |
| --- | --- |
| 시험·답안·점수·AI 피드백·결과 이력 | 전체 기록 삭제인지 개별 기록 삭제인지, 어느 데이터를 함께 지울지 |
| 학습 통계·진도·스트릭·10초 챌린지 | 기록 삭제와 함께 초기화할지, 파생 통계를 어떻게 재계산할지 |
| 단어장·즐겨찾기 등 사용자가 저장한 자료 | 삭제에 포함할지 보존할지 |
| 녹음·첨부 파일·AI 처리 사본 | 파일·외부 사본의 소유 서비스와 삭제 완료 범위, 백업·보존 정책 |
| 처리 중인 시험·AI 채점 | 삭제 요청 전에 시작한 작업의 취소·완료 처리와 늦은 결과의 재생성 방지 |
| 삭제 요청 이후 새로 시작한 학습 | 삭제 기준 시점과 새 데이터 보존 규칙 |
| 구매·이용권·무료 체험·사용 횟수 | 학습 삭제로 초기화하지 않는 방향 제안. Billing 정책 확인 필요 |
| 이용 가능 계정 | MEMBER만 지원할지 기존 GUEST도 지원할지, 추가 본인 확인 필요 여부 |

계정·SNS 연결·로그인·프로필·동의 유지가 요청의 기준이다. 위 학습 범위는 “학습 기록”이라는 문구만으로 전체 삭제를 가정하지 않는다. 사용자 확인 화면에는 확정한 삭제·보존 범위와 복구 가능 여부를 표시해야 한다.

### API 구현 전에 Learning Core에서 인계할 사항

- endpoint·HTTP method·권한 조건과 기존 Identity JWT 검증 적용.
- 동기 완료 또는 비동기 접수/완료 구분, 비동기라면 완료 확인·부분 실패 처리 방법.
- 중복 클릭·응답 유실·전송 재시도의 멱등 보장과 새 학습 기록을 재시도로 삭제하지 않는 기준.
- 진행 중 학습·AI callback·다른 기기·Guest merge·회원 탈퇴와 동시에 실행될 때의 처리 규칙.
- 원본·파생 통계·캐시·파일·외부 사본별 완료 기준, 복구/보존 제한, 오류 응답과 재시도 조건.

### 프론트·QA 완료 확인 항목 (제안)

- [ ] 정상 삭제 후 같은 계정으로 로그인 상태·프로필·SNS 연결·동의가 유지된다.
- [ ] 확정한 학습 데이터가 제거되고 제외한 항목은 유지된다. 구매·사용 이력이 의도치 않게 초기화되지 않는다.
- [ ] 취소하면 서버 삭제 요청을 보내지 않는다. 중복 클릭·응답 유실은 같은 논리 요청의 계약으로 처리한다.
- [ ] 비동기 작업은 접수만으로 완료 표시하지 않으며 실패 시 완료 상태를 허위로 표시하지 않는다.
- [ ] 삭제 완료 후 학습 화면·로컬 캐시를 갱신하고 다른 기기도 최신 상태를 조회한다.
- [ ] 삭제 이전 작업의 늦은 AI 결과가 삭제한 기록을 다시 만들지 않는다. 삭제 이후 생성한 기록은 합의된 기준에 따라 보존된다.
- [ ] 다른 사용자의 기록을 삭제할 수 없다. 로그인 만료는 기존 인증 오류 처리로 구분한다.

별도 Learning Core 작업의 범위를 정한 후 API 인계와 배포 검증을 진행한다. TMI-169의 Guest enrollment 구현 완료 범위에 학습 기록 삭제 구현이 포함됐다고 표시하지 않는다.

<a id="legacy"></a>

## 구버전·이메일 참고

### 이메일·비밀번호 경로 — 이번 신규 UI 범위 밖

서버에는 Firebase PASSWORD 및 legacy LOCAL 경로가 있으나 현재 제품의 신규 SNS 인계에 이메일 화면을 추가하지 않는다. 기존 이메일 회원이 없으므로 migration은 대상이 없다. 아래는 서버 호환 경로 설명이며 제품 지원을 의미하지 않는다.

```text
Firebase email/password 로그인 또는 가입
→ 신규 가입이면 email verification
→ Firebase ID Token
→ /api/v1/auth/firebase/exchange
→ 기존 MEMBER는 AUTHENTICATED
→ 신규 MEMBER는 same-UID phone link 후 /api/v1/auth/firebase/signup
```

프론트는 legacy `/api/v1/auth/check-email`, `/api/v1/auth/signup`, `/api/v1/auth/login`을 신규 인증 화면에서 사용하지 않는다. 다른 Firebase UID를 기존 ACTIVE 계정에 자동 rebind하지 않는다.

### `POST /api/v1/auth/guest` — 구버전 참고, 신규 앱 호출 금지

인증: 공개

요청:

```json
{
  "installationId": "550e8400-e29b-41d4-a716-446655440000",
  "isPrivacyConsented": true,
  "privacyConsentVersion": "privacy-v1",
  "isTermConsented": true,
  "termConsentVersion": "term-v1",
  "isQualityReviewConsented": false,
  "qualityReviewConsentVersion": null
}
```

성공:

```json
{
  "isSuccess": true,
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "result": {
    "accessToken": "<identity-access-token>",
    "refreshToken": "<identity-refresh-token>",
    "grantType": "Bearer",
    "accessTokenExpiresIn": 1800000,
    "refreshTokenExpiresIn": 1209600000
  }
}
```

서버의 현재 동작은 같은 installationId 재요청 시 `409 GUEST_ALREADY_EXISTS`다. 최초 응답 복구는 구현하지 않았으며 취소한 Stage 11의 헤더·만료 오류·암호화 응답은 제공되지 않는다. installationId는 인증 credential이 아니다. 이 예시는 구버전 동작 확인용으로만 보존한다.

## SNS 공통 연결 변경 참고

### TMI-131 추가 연동

SNS 해제·재연결은 [전용 API/응답·모바일 흐름·오류 계약](firebase-provider-unlink-stage-10-runbook.md#51-api와-응답)을 따른다. 신규 URL은 `/api/v1/auth/firebase/providers/unlink`, `/unlink/status`, `/link/prepare`, `/link/start`, `/link/complete`, `/link/status`다. 기존 `/relink/prepare`는 서버 라우트와 Swagger에서 제거했으며 sync에는 새 연결을 저장하지 않는다. 기능 기본 OFF이며 지금 배포돼 있다는 뜻이 아니다.

- 해제는 남는 Google·Apple·Kakao로 재인증하고 사용자 Access Token + `Idempotency-Key`로 접수한다. 202 후 현재 기기도 자체 Token 삭제·Firebase signOut한다.
- 응답 유실/로그아웃 후에는 같은 requestId와 남는 SNS의 fresh Firebase 증거로 status를 조회한다. 자체 Token은 없어도 되지만 무인증 조회는 아니다. 404를 해제 재실행 허가로 해석하지 않는다.
- 처음 연결하든 다시 연결하든 prepare → start → 같은 Firebase User에 명시적 link → 대상 SNS 재인증 → complete를 사용한다. 프론트는 해제 이력을 판단하지 않는다.
- 앱은 최초 start 응답의 linkAllowed=true일 때만 SDK link를 한 번 실행한다. 대상 재인증의 auth_time은 start 이후여야 하므로 Firebase 초 단위 경계를 넘겨야 한다.
- **PREPARED 중단은 만료 후 새 준비 가능, STARTED 결과 불명은 자동 해제 불가**다. start 응답 유실 시 SDK를 재실행하지 말고 status·현재 Firebase 연결/이전 호출 종료를 확인한다. [5분과 중단 복구 계약](firebase-provider-unlink-stage-10-runbook.md#22-공통-연결의-5분과-앱-중단)을 UI·QA와 함께 적용한다.
- 전화번호 셀프 변경·번호 재할당 예외 처리, SNS 회사 계정 삭제, 시험 기록/무료권 변경 기능은 추가하지 않았다.

<a id="sources"></a>

## 코드·계약 근거

- Firebase API: `src/main/java/web/tosunsaeng/identity/domain/auth/federation/api/FirebaseExchangeController.java`
- Firebase request·response DTO: `src/main/java/web/tosunsaeng/identity/domain/auth/federation/dto`
- LOCAL·Guest·Session API: `src/main/java/web/tosunsaeng/identity/domain/auth/common/api/AuthController.java`
- 사용자 조회·탈퇴 API: `src/main/java/web/tosunsaeng/identity/domain/user/api/UserController.java`
- Firebase 검증과 Provider mapping: `src/main/java/web/tosunsaeng/identity/domain/auth/federation/infrastructure/firebase/FirebaseAdminAuthenticationVerifier.java`
- 오류 code와 HTTP status: `src/main/java/web/tosunsaeng/identity/domain/auth/common/exception/AuthErrorStatus.java`
- 기본 설정: `src/main/resources/application.yml`
- Firebase broker ADR: `docs/adr/ADR-001-firebase-authentication-broker.md`
- 전체 SNS 구현 계약: `docs/contracts/social-login-implementation-plan.md`
- 탈퇴 모바일 UX: `docs/contracts/withdrawal-session-mobile-ux-stage-4-plan.md`

추가 확인 근거:

- [SNS 연결 Controller](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderLinkController.java), [해제 Controller](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderChangeController.java)
- [연결 상태·허가](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderLinkService.java), [해제 상태·멱등 키](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderChangeService.java)
- [Guest prepare 재개·충돌 처리](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseGuestPrepareService.java)
- [정책 상태 DTO](../../src/main/java/web/tosunsaeng/identity/domain/user/dto/response/UserConsentStatusResponse.java), [개별 정책 DTO](../../src/main/java/web/tosunsaeng/identity/domain/user/dto/response/ConsentPolicyStatusResponse.java), [동의 변경 DTO](../../src/main/java/web/tosunsaeng/identity/domain/user/dto/request/UserConsentUpdateRequest.java)
- [재발급 기능 분기](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/TokenReissueService.java), [탈퇴 cleanup enum](../../src/main/java/web/tosunsaeng/identity/domain/user/domain/enums/UserWithdrawalCleanupStatus.java)
- [보호/공개 route 설정](../../src/main/java/web/tosunsaeng/identity/global/config/SecurityConfig.java)

예시 값은 가짜 데이터·placeholder이며 실제 Token·credential을 문서에 붙여 넣지 않는다. 무료 사용권 조회·시험·결과 API는 해당 서비스의 별도 명세를 따른다.
