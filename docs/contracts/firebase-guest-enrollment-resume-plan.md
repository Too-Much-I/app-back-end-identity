# Firebase Guest enrollment 재개 계약 구현 계획

- 작성일: 2026-09-21
- 상태: 구현 및 검증 완료 (배포 전)
- Jira: `TMI-169` (parent: `TMI-136` — sns 로그인)
- 적용 저장소: Identity Service
- 구현 기준: `develop` / `origin/develop` commit `8c624ffe`
- 작성 브랜치: `codex/TMI-134-guest-session-recovery-hotfix`
- 구현 브랜치: `feat/TMI-169-guest-enrollment-resume`
- 전제: 운영·테스트 환경에 Guest가 `FirebaseIdentity` 또는 `SocialIdentity`를 소유한 legacy 데이터가 없음

## 1. 목적

Guest 사용자가 SNS/Firebase 인증 후 회원가입을 중단했다가 다시 돌아와도, 프론트가 `/users/me`와 내부 상태를 조합해 추측하지 않고 `POST /api/v1/auth/firebase/guest/prepare` 한 번으로 다음 정보를 확인하게 한다.

- 신규 승격을 계속할 수 있는지
- 기존 MEMBER와 merge해야 하는지
- 현재 사용할 수 있는 `enrollmentId`
- enrollment의 남은 유효시간
- email·phone·profile·필수 동의 중 아직 필요한 요건
- `/guest/upgrade`에 사용할 현재 필수 동의 정책 버전

Guest prepare 전용 `ALREADY_LINKED`는 제거한다. Provider 연결/해제 기능에서 사용하는 별도의 멱등 `ALREADY_LINKED`는 이번 범위에서 변경하지 않는다.

여기서 Provider 연결 기능은 **이미 MEMBER인 사용자**가 Google·Apple·Kakao 같은 SNS를 추가로 연결하는 `/api/v1/auth/firebase/providers/link/prepare` 흐름이다. 예를 들어 현재 MEMBER에게 Google이 Firebase와 Identity DB 양쪽에 이미 같은 사용자 소유로 연결되어 있는데 Google 연결을 다시 요청하면, 서버는 `status=ALREADY_LINKED`, `linkAllowed=false`를 반환한다. 이는 가입 미완료 상태가 아니라 이미 끝난 작업의 안전한 재호출이므로 추가 Firebase SDK link 없이 성공 종료하는 것이 맞다.

두 `ALREADY_LINKED`는 이름만 같고 도메인 의미가 다르다.

| 위치 | 사용자 상태 | 의미 | 계획 |
| --- | --- | --- | --- |
| `/firebase/guest/prepare` | GUEST | 가입 완료도 아니고 enrollment도 없는 진행 불가 상태 | 제거하고 불변식 위반은 409 처리 |
| `/firebase/providers/link/prepare` | MEMBER | 요청한 SNS가 이미 같은 MEMBER에게 정상 연결됨 | 멱등 완료 상태로 유지 |

## 2. 현재 문제

현재 `develop`의 Guest prepare는 다음과 같이 동작한다.

| 서버 소유권 판정 | 현재 결과 | 문제 |
| --- | --- | --- |
| owner 없음 | `ENROLLMENT_REQUIRED` | enrollment는 주지만 미충족 요건과 정책 버전이 없음 |
| 다른 ACTIVE MEMBER owner | `MERGE_REQUIRED` | 정상 |
| 현재 Guest owner | `ALREADY_LINKED` | enrollmentId가 없고 Guest는 MEMBER도 아니어서 진행할 수 없는 dead-end |

`/users/me`에는 `accountType`, nickname, 동의 snapshot은 있지만 다음 정보가 없다.

- fresh Firebase proof 기준 email·phone 검증 상태
- 현재 Firebase UID에 묶인 활성 enrollment
- enrollment 만료·재발급 상태
- 가입 완료에 필요한 다음 요건

따라서 `/users/me`를 enrollment 상태 API로 확장하거나 프론트가 phone 상태를 추정하게 해서는 안 된다. Guest JWT와 fresh Firebase ID Token을 함께 검증하는 `/guest/prepare`가 이 판단을 소유한다.

## 3. 목표 상태

### 3.1 결과 분기

| 조건 | 목표 결과 | mutation |
| --- | --- | --- |
| Firebase/Social owner 없음 + 활성 enrollment 있음 | `ENROLLMENT_REQUIRED`와 같은 enrollmentId | 기존 attempt 재사용 |
| Firebase/Social owner 없음 + enrollment 없음 | `ENROLLMENT_REQUIRED`와 새 enrollmentId | 새 attempt 생성 |
| Firebase/Social owner 없음 + enrollment 만료 | `ENROLLMENT_REQUIRED`와 새 enrollmentId | 기존 attempt를 `EXPIRED`로 전환하고 새 attempt 생성 |
| 다른 ACTIVE MEMBER가 owner | `MERGE_REQUIRED` | enrollment 생성 금지 |
| 현재 Guest가 owner | `409 IDENTITY_STATE_CONFLICT` | enrollment 생성·자동 승격·자동 merge 금지 |
| owner가 여러 User로 갈리거나 비활성 owner가 섞임 | 기존 identity conflict 정책 | 자동 복구 금지 |

정상 최초 진입과 재개를 별도 result type으로 나누지 않는다. 프론트 진행에 필요한 것은 서버가 반환한 유효한 enrollmentId와 현재 미충족 요건이며, 내부 attempt 생성 여부를 외부 필수 계약으로 만들지 않는다.

### 3.2 Guest prepare 성공 응답

`ENROLLMENT_REQUIRED` 응답은 다음 필드를 사용한다.

```json
{
  "isSuccess": true,
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "result": {
    "type": "ENROLLMENT_REQUIRED",
    "enrollmentId": "550e8400-e29b-41d4-a716-446655440000",
    "missingRequirements": [
      "PHONE_VERIFICATION",
      "PROFILE"
    ],
    "privacyConsentVersion": "privacy-v1",
    "termConsentVersion": "term-v1",
    "expiresIn": 600000
  }
}
```

계약 규칙은 다음과 같다.

- `expiresIn` 단위는 기존과 같이 milliseconds다.
- `missingRequirements` 배열 순서는 계약으로 사용하지 않는다.
- 정책 버전은 서버의 현재 `ConsentPolicy` 값이며 앱 빌드에 하드코딩하지 않는다.
- 정책 버전 두 필드는 `ENROLLMENT_REQUIRED`일 때 항상 반환한다.
- Firebase ID Token, phone 원문, provider subject, 내부 userId는 응답하지 않는다.

`MERGE_REQUIRED`는 기존처럼 enrollment 관련 필드를 포함하지 않는다.

```json
{
  "type": "MERGE_REQUIRED"
}
```

### 3.3 `missingRequirements` 판정

가능한 값은 기존 enum을 유지한다.

| 값 | Guest prepare 판정 |
| --- | --- |
| `EMAIL_VERIFICATION` | `PASSWORD` 인증수단이 연결되어 있고 fresh proof의 email이 미인증일 때 |
| `PHONE_VERIFICATION` | 같은 Firebase User에 `PHONE`이 연결되지 않았거나 fresh proof에서 verified phone을 확인할 수 없을 때 |
| `PROFILE` | Guest의 기본 nickname은 완료된 MEMBER profile이 아니므로 Guest 승격에서는 항상 포함 |
| `CONSENTS` | 저장된 개인정보 처리방침·이용약관 동의가 현재 서버 정책 버전의 유효한 동의가 아닐 때 |

필수 동의의 현재성은 각 항목에 대해 다음 조건을 모두 만족할 때만 인정한다.

```text
consented == true
AND consentedVersion == current server version
AND consentedAt != null
```

품질 검토 동의는 선택 동의이므로 `CONSENTS` 누락 여부에 포함하지 않는다.

`missingRequirements`는 prepare 시점의 snapshot이다. 이후 phone link나 email verification을 완료한 뒤 같은 Firebase User의 ID Token을 강제 갱신해 prepare를 다시 호출하면, 활성 enrollmentId는 유지하면서 요건만 다시 계산할 수 있다.

### 3.4 Guest-owned identity 불변식 위반

정상 Guest upgrade는 Guest→MEMBER 전환과 Firebase/Social identity 저장을 하나의 Mongo Transaction에서 처리한다. Provider link도 MEMBER만 허용한다. 따라서 identity owner가 현재 userId인데 User가 계속 Guest인 상태는 정상 가입 단계가 아니다.

legacy 데이터가 없다는 전제에 따라 다음을 적용한다.

- `FirebaseGuestPrepareResultType.ALREADY_LINKED` 제거
- `FirebaseGuestPrepareResponse.alreadyLinked()` 제거
- `OWNED_BY_CURRENT_USER`이면 `409 IDENTITY_STATE_CONFLICT`
- 해당 분기에서 enrollment 생성, MEMBER 승격, merge 호출 금지
- 오류 응답에 Firebase UID, provider subject, userId를 포함하지 않음
- 낮은 cardinality의 outcome counter와 안전한 구조화 event만 기록

이 오류를 받는 프론트는 성공이나 가입 재개로 해석하지 않는다. Firebase 재인증 후에도 반복되면 지원 안내로 종료한다.

## 4. Enrollment 만료와 삭제 계약

기본 enrollment 유효시간은 `FIREBASE_ENROLLMENT_TTL=PT10M`이다.

```text
PENDING + expiresAt > now
→ 같은 enrollmentId 재사용

PENDING + expiresAt <= now
→ 기존 attempt를 EXPIRED로 CAS 변경
→ 새 PENDING attempt와 새 enrollmentId 생성

만료된 enrollmentId로 guest/upgrade
→ 409 FIREBASE_ENROLLMENT_CONFLICT
```

만료와 물리 삭제는 분리한다.

- `expiresAt`에는 Mongo TTL index를 추가하지 않는다.
- `cleanupAt`의 기존 TTL index를 유지한다.
- 성공 finalize 또는 abandoned cleanup terminal 처리에서만 기존 정책에 따라 `cleanupAt`을 설정한다.
- 기본 terminal enrollment retention은 24시간이다.
- Mongo TTL 삭제는 비동기이므로 정확한 삭제 시각을 API 계약으로 보장하지 않는다.
- 새 enrollment가 시작되면 해당 Firebase target의 기존 `cleanupAt`을 해제하는 현재 lifecycle을 유지한다.

따라서 프론트는 attempt 문서의 존재·삭제 여부를 알 필요가 없다. 없음과 만료는 모두 새 유효 enrollment를 반환하는 `ENROLLMENT_REQUIRED`로 처리한다.

## 5. 프론트 처리 계약

### 5.1 정상 승격

```text
Guest Identity Access Token 유지
→ SNS/Firebase 재인증
→ POST /firebase/guest/prepare
→ ENROLLMENT_REQUIRED
→ missingRequirements에 필요한 화면만 수행
→ phone link 후 같은 UID인지 확인
→ Firebase ID Token 강제 갱신
→ POST /firebase/guest/upgrade
→ 성공 Token으로 교체
```

- `MERGE_REQUIRED`이면 신규 승격을 중단하고 사용자 확인 뒤 `/guest/merge`로 이동한다.
- `/users/me.accountType`은 계정 표시용이며 enrollmentId나 phone proof의 근거로 사용하지 않는다.
- Guest prepare 뒤 임의로 `/firebase/signup`의 direct enrollment를 섞지 않는다.

### 5.2 만료 복구

`409 FIREBASE_ENROLLMENT_CONFLICT` 또는 `FIREBASE_ENROLLMENT_RESTART_REQUIRED`를 받으면 다음처럼 처리한다.

1. 로컬의 기존 enrollmentId를 폐기한다.
2. 현재 Guest Identity Token을 유지한다.
3. Firebase proof가 stale하면 Provider 재인증 또는 ID Token 강제 갱신을 수행한다.
4. `/firebase/guest/prepare`를 다시 호출한다.
5. 새 enrollmentId와 requirements로 승격을 재개한다.

같은 만료 ID로 `/guest/upgrade`를 무한 재시도하거나, Guest를 새로 만들거나, `/firebase/exchange`의 direct signup enrollment로 전환하지 않는다.

## 6. Backend 변경 범위

### 6.1 응답 DTO

`FirebaseGuestPrepareResponse`에 다음을 추가한다.

- `Set<FirebaseEnrollmentRequirement> missingRequirements`
- `String privacyConsentVersion`
- `String termConsentVersion`

type별 불변식을 생성자에서 검증한다.

- `ENROLLMENT_REQUIRED`: enrollmentId, expiresIn, requirements, 두 정책 버전 필수
- `MERGE_REQUIRED`: 위 enrollment 전용 필드 모두 null

Guest prepare enum과 factory에서 `ALREADY_LINKED`를 제거한다.

### 6.2 요건 계산기

`FirebaseExchangeService`에 들어 있는 email·phone requirement 계산을 독립 application component로 추출한다. 예시 책임은 다음과 같다.

```text
FirebaseEnrollmentRequirementResolver
├─ directSignup(principal)
└─ guestUpgrade(principal, guest, consentPolicy)
```

- direct signup의 현재 `PROFILE`, `CONSENTS` 상시 요구 동작은 유지한다.
- Guest에서는 fresh Firebase proof와 서버 User/ConsentPolicy를 함께 사용한다.
- client가 주장한 phone/email/consent 상태는 입력으로 받지 않는다.

### 6.3 Guest prepare service

`FirebaseGuestPrepareService`는 다음 순서를 유지한다.

1. JWT `sub`로 User 조회
2. ACTIVE GUEST 확인
3. Firebase ID Token을 `GUEST_ENROLLMENT_PREPARE` 목적으로 검증
4. session security 검증
5. identity ownership 판정
6. 결과에 따라 conflict, merge 또는 enrollment 생성·재사용
7. fresh proof와 서버 User를 기준으로 requirements 계산
8. 남은 시간을 주입된 `Clock`으로 계산해 응답

ownership 판정 전에 enrollment를 만들지 않는다.

### 6.4 오류·관측

`AuthErrorStatus`에 409 `IDENTITY_STATE_CONFLICT`를 추가한다. 외부 message는 재인증 후에도 반복되면 지원이 필요하다는 일반 문구만 사용한다.

관측 정보는 다음으로 제한한다.

- event: `identity.firebase.guest.prepare`
- outcome: `ENROLLMENT_REQUIRED`, `MERGE_REQUIRED`, `IDENTITY_STATE_CONFLICT`
- enrollment action: `REUSED`, `CREATED`, `REPLACED_EXPIRED`가 안전하게 구분 가능할 때만 낮은 cardinality tag로 사용

Token, Firebase UID, provider subject, phone, enrollmentId는 로그·metric tag에 넣지 않는다.

### 6.5 문서

다음을 함께 갱신한다.

- `FirebaseExchangeController` OpenAPI 설명과 example
- `docs/contracts/frontend-firebase-auth-integration-guide.md`
- 필요한 경우 부록의 Guest prepare 예시
- `docs/codex/WORKLOG.md`
- `docs/codex/CURRENT_STATE.md`

Provider link/unlink 문서의 `ALREADY_LINKED` 설명은 변경하지 않는다.

## 7. 예상 수정 파일

주요 파일은 다음과 같다. 실제 구현 시 패키지 변경이 있으면 동일 책임의 최신 파일을 기준으로 한다.

- `domain/auth/common/exception/AuthErrorStatus.java`
- `domain/auth/federation/application/FirebaseGuestPrepareService.java`
- `domain/auth/federation/application/FirebaseExchangeService.java`
- 신규 `FirebaseEnrollmentRequirementResolver.java`
- `domain/auth/federation/dto/response/FirebaseGuestPrepareResponse.java`
- `domain/auth/federation/dto/response/FirebaseGuestPrepareResultType.java`
- `domain/auth/federation/api/FirebaseExchangeController.java`
- `domain/auth/federation/infrastructure/firebase/FirebaseAuthenticationConfiguration.java`
- 관련 service·DTO·Controller·repository integration tests
- 프론트 Firebase 연동 가이드

`FirebaseEnrollmentAttempt`의 `expiresAt` TTL index나 collection migration은 추가하지 않는다.

## 8. 테스트 계획

### 8.1 Service 단위 테스트

- owner 없음 + 활성 attempt: 같은 enrollmentId와 감소한 expiresIn 반환
- owner 없음 + attempt 없음: 새 enrollmentId 반환
- owner 없음 + 만료 attempt: 기존 attempt `EXPIRED`, 새 enrollmentId 반환
- 다른 ACTIVE MEMBER owner: `MERGE_REQUIRED`, attempt 미생성
- 현재 Guest owner: 409 `IDENTITY_STATE_CONFLICT`, attempt 미생성
- ACTIVE GUEST가 아닌 User: 기존 `GUEST_UPGRADE_NOT_ALLOWED`
- password email 미인증/인증 조합
- phone 미연결·미검증·검증 조합
- 현재 consent와 stale/missing consent 조합
- policy version이 prepare 사이에 바뀐 경우 새 `CONSENTS`와 새 version 반환
- requirements 재계산 시 활성 enrollmentId 유지

### 8.2 DTO·Controller 계약 테스트

- `ENROLLMENT_REQUIRED` JSON에 새 필드가 정확히 직렬화됨
- `MERGE_REQUIRED`에는 enrollment 전용 필드가 없음
- Guest prepare에서 `ALREADY_LINKED`가 더 이상 직렬화되지 않음
- Firebase credential, userId, phone, provider subject가 응답·`toString()`에 없음
- OpenAPI example과 runtime JSON 필드 일치
- requirements 배열 순서에 의존하지 않는 assertion

### 8.3 Repository·lifecycle 테스트

- `expiresAt == now`를 만료로 처리
- 만료 PENDING의 CAS 승자만 EXPIRED 전환
- 동시 prepare가 하나의 활성 PENDING만 남김
- 새 attempt 생성 시 target의 `cleanupAt` 해제
- 만료만으로 문서가 즉시 삭제되지 않음
- terminal 처리 후에만 `cleanupAt`이 설정됨

### 8.4 회귀 테스트

- direct `/firebase/exchange`의 requirement 결과 유지
- `/guest/upgrade`의 Guest→MEMBER Transaction 유지
- `/guest/merge`의 기존 MEMBER owner 흐름 유지
- provider link/prepare의 별도 `ALREADY_LINKED` 유지
- `account_type` JWT claim과 `/users/me.accountType` 유지
- Session security와 recent-auth 검증 순서 유지

### 8.5 실행 명령

```text
./gradlew clean test
git diff --check
```

실제 Atlas·Firebase Provider는 테스트에서 호출하지 않고 Repository와 Provider port를 mock 또는 격리 Mongo test profile로 검증한다.

## 9. 완료 조건

- Guest prepare 정상 응답에서 `ALREADY_LINKED`가 제거된다.
- owner가 없는 Guest는 최초·재개·만료 후 모두 유효한 enrollmentId를 받는다.
- 만료 attempt는 재사용되지 않으며 새 attempt가 발급된다.
- 응답만으로 phone/email/profile/consent 필요 여부와 현재 필수 정책 버전을 알 수 있다.
- 다른 MEMBER owner는 mutation 없이 `MERGE_REQUIRED`다.
- 불가능한 Guest-owned identity는 자동 복구되지 않고 409 및 안전한 관측으로 처리된다.
- `/users/me`를 enrollment 진행 상태 API로 확장하지 않는다.
- Provider 연결/해제의 `ALREADY_LINKED` 계약은 유지된다.
- 전체 테스트와 diff check가 통과한다.
- 프론트 가이드와 OpenAPI가 구현과 일치한다.

## 10. 배포·호환·롤백

### 배포 순서

1. Backend 응답·오류·테스트·문서를 먼저 배포한다.
2. 프론트가 `missingRequirements`와 정책 버전을 사용하도록 배포한다.
3. 비운영 Guest로 활성 재사용, 만료 교체, merge 분기를 검증한다.
4. `IDENTITY_STATE_CONFLICT` metric이 0인지 확인해 legacy 없음 전제를 재검증한다.

새 응답 필드는 additive다. Guest prepare의 `ALREADY_LINKED` 제거는 legacy 데이터가 없다는 전제에서 정상 사용자에게 관측되지 않아야 한다. 구버전 앱은 알 수 없는 409를 성공으로 처리하지 않고 안전하게 중단해야 한다.

### 롤백

- DB schema migration이 없으므로 애플리케이션 롤백만으로 복귀할 수 있다.
- 새로 생성된 `EXPIRED` 이력은 기존 enum과 호환되며 삭제할 필요가 없다.
- 롤백을 위해 enrollment collection을 삭제하거나 TTL index를 변경하지 않는다.

## 11. 제외 범위

- Guest-owned identity legacy reconciliation
- 자동 MEMBER 승격 또는 자동 merge
- `/users/me`에 Firebase phone/email proof 추가
- enrollment 진행단계를 저장하는 새 별도 API
- enrollmentId 응답 복구를 위한 별도 idempotency collection
- Provider 연결/해제의 `ALREADY_LINKED` 변경
- Firebase Admin credential·운영 Secret 변경
- main hotfix 브랜치로 develop SNS 코드를 이식하는 작업

## 12. 구현 전 확인사항

- `IDENTITY_STATE_CONFLICT` 외부 code 명칭 최종 확정
- 모바일 구버전이 알 수 없는 409를 성공으로 처리하지 않는지 확인
- 대상 환경 read-only 진단에서 Guest-owned identity가 실제로 0건인지 재확인
- metric·로그 수집 플랫폼에서 제안 outcome만으로 운영 확인이 가능한지 검토

Jira `TMI-169`는 사용자 승인 후 TMI-136 하위 `작업`으로 생성했다. 댓글·상태 변경은 수행하지 않았다.
