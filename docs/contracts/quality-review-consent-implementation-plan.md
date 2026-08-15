# Quality review 선택 동의 구현 계획

- 작성일: 2026-08-15
- 상태: 구현 완료 (`hotfix/quality-review-consent`, 2026-08-15)
- 적용 저장소: Identity Service
- 적용 기준: `main` (`b6eb73e`)에서 분기한 독립 hotfix
- 병합 대상: `main`만 사용하며 `develop` 병합은 이번 작업에서 금지
- 외부 연동: 실제 답안·음성·시험 데이터를 소유한 서비스와 별도 계약 필요

## 1. 목적

사용자가 학습 결과·답안·음성 등의 품질 검토 이용에 선택적으로 동의하거나 기존 동의를 철회할 수 있도록 Identity의 현재 동의 상태와 API 계약을 확장한다.

개인정보 처리방침과 이용약관은 계속 필수 동의다. Quality review는 선택 동의이므로 `false`여도 Guest 생성, 회원가입, 로그인과 일반 학습 기능을 차단하지 않는다.

Identity는 동의의 현재 상태와 서버 시각을 소유한다. 실제 품질 검토 대상 데이터와 검토 작업은 Identity가 소유하지 않으므로, 철회 이후 이용 중지·보존·삭제는 데이터 소유 서비스와 별도 lifecycle 계약으로 처리한다.

## 2. 고정 외부 필드명

요청 필드명은 다음 이름을 유지한다.

- `isQualityReviewConsented`
- `qualityReviewConsentVersion`

조회 응답은 `qualityReview` 아래에 다음 필드를 사용한다.

- `currentVersion`
- `consented`
- `consentedVersion`
- `consentedAt`
- `requiresConsent`

이번 계획에서는 위 외부 JSON 필드명을 변경하거나 별칭을 추가하지 않는다.

## 3. API 계약

### 3.1 `POST /api/v1/auth/guest`

새 프론트 요청은 두 quality review 필드를 항상 포함한다.

```json
{
  "installationId": "550e8400-e29b-41d4-a716-446655440000",
  "isPrivacyConsented": true,
  "privacyConsentVersion": "privacy-v1",
  "isTermConsented": true,
  "termConsentVersion": "term-v1",
  "isQualityReviewConsented": false,
  "qualityReviewConsentVersion": "quality-review-v1"
}
```

검증 규칙은 다음과 같다.

| 요청 | 처리 |
| --- | --- |
| privacy 또는 terms가 `true`가 아님 | 기존 필수 동의 오류 |
| privacy 또는 terms version 불일치 | 기존 version mismatch 오류 |
| quality review `true` + 현재 version 일치 | 동의 상태 저장 |
| quality review `true` + version 누락·불일치 | quality review version mismatch 오류 |
| quality review `false` | Guest 생성 허용, quality review 미동의 저장 |
| quality review `false` + stale version | 철회를 차단하지 않고 미동의 처리 |
| quality review 필드 전체 누락 | 하위 호환 기간에는 `false`로 처리 |

새 프론트 계약에서는 `qualityReviewConsentVersion`을 항상 보내지만, backend는 철회와 구버전 client 보호를 위해 `false` 요청의 version 불일치를 차단 근거로 사용하지 않는다. 문자열이 존재하면 trim 후 최대 100자와 `^[A-Za-z0-9][A-Za-z0-9._-]{0,99}$` 형식을 검증한다.

### 3.2 `PUT /api/v1/users/me/consents`

같은 두 필드를 추가한다.

```json
{
  "isPrivacyConsented": true,
  "privacyConsentVersion": "privacy-v1",
  "isTermConsented": true,
  "termConsentVersion": "term-v1",
  "isQualityReviewConsented": false,
  "qualityReviewConsentVersion": "quality-review-v1"
}
```

이 API는 quality review 철회 경로다.

```text
isQualityReviewConsented=false
→ 현재 quality review 동의를 철회 또는 미동의 상태로 유지
```

privacy와 terms는 기존대로 `true`와 현재 version 일치를 요구한다. Quality review만 `true`와 `false`를 모두 허용한다.

상태 전이와 멱등성은 다음과 같이 고정한다.

| 기존 상태 | 요청 | 결과 |
| --- | --- | --- |
| false | false | 저장하지 않는 성공 no-op |
| false | true/current version | 서버 시각으로 신규 동의 |
| true/current version | true/current version | 기존 동의 시각을 유지하는 성공 no-op |
| true/old version | true/current version | 현재 version과 새 서버 시각으로 갱신 |
| true | false | version·동의 시각을 제거하고 미동의 전환 |

동일 상태 반복 요청은 User의 `updatedAt`과 동의 시각을 불필요하게 변경하지 않는다.

### 3.3 `GET /api/v1/users/me/consents`

응답에 `qualityReview`를 추가한다.

```json
{
  "privacy": {
    "currentVersion": "privacy-v1",
    "consented": true,
    "consentedVersion": "privacy-v1",
    "consentedAt": "2026-08-15T00:00:00Z",
    "requiresConsent": false
  },
  "terms": {
    "currentVersion": "term-v1",
    "consented": true,
    "consentedVersion": "term-v1",
    "consentedAt": "2026-08-15T00:00:00Z",
    "requiresConsent": false
  },
  "qualityReview": {
    "currentVersion": "quality-review-v1",
    "consented": false,
    "consentedVersion": null,
    "consentedAt": null,
    "requiresConsent": false
  }
}
```

`qualityReview.requiresConsent`는 선택 동의이므로 항상 `false`다. 이 값이 `false`여도 `consented`가 `true`라는 의미는 아니다.

Quality review의 유효한 동의는 다음 조건을 모두 만족할 때만 인정한다.

```text
저장된 consented == true
AND consentedVersion == currentVersion
AND consentedAt != null
```

정책 version이 변경되면 이전 동의를 현재 품질 검토의 근거로 사용하지 않는다. 응답의 `consented`는 위 조건을 반영한 현재 유효 상태로 계산한다. 선택 동의이므로 갱신하지 않아도 `requiresConsent`는 `false`이며 일반 기능을 차단하지 않는다.

### 3.4 PUT 성공 응답과 프로필

현재 `PUT` 성공 응답인 `UserConsentResponse`에도 다음 저장 결과를 추가해 클라이언트가 별도 GET 없이 결과를 확인할 수 있게 한다.

- `qualityReviewConsented`
- `qualityReviewConsentVersion`
- `qualityReviewConsentedAt`

`UserProfileResponse`에 quality review 상태를 포함할지는 API 중복을 고려해 구현 전에 확정한다. 기본 권장은 동의 상태의 단일 조회 계약을 `GET /api/v1/users/me/consents`로 유지하고 프로필에는 신규 필드를 추가하지 않는 것이다.

## 4. 저장 모델

`User.consents` embedded object에 다음 필드를 추가한다.

```json
{
  "qualityReviewConsented": false,
  "qualityReviewConsentVersion": null,
  "qualityReviewConsentedAt": null
}
```

불변식은 다음과 같다.

```text
qualityReviewConsented == true
→ version은 현재 server version
→ consentedAt은 서버가 기록한 Instant

qualityReviewConsented == false
→ version은 null
→ consentedAt은 null
```

요청 시각은 받지 않고 주입된 서버 `Clock`만 사용한다.

기존 MongoDB 문서에 새 필드가 없으면 다음처럼 읽는다.

```text
qualityReviewConsented=false
qualityReviewConsentVersion=null
qualityReviewConsentedAt=null
```

스키마리스 embedded 필드 추가이므로 파괴적 migration과 신규 index는 필요하지 않다. 운영 데이터 backfill은 기본적으로 수행하지 않는다.

### 4.1 철회 감사

위 embedded model은 현재 상태 snapshot만 표현한다. false snapshot만으로는 최초 미동의와 true 이후 철회를 구분할 수 없다.

품질 검토 동의·철회 이력의 법적 증명이 필요하면 production 활성화 전에 별도 append-only consent history를 설계한다.

```text
policyType=QUALITY_REVIEW
policyVersion
action=CONSENTED / WITHDRAWN
occurredAt
canonical userId
```

이력에는 답안·음성 원문이나 요청 본문을 저장하지 않는다. 이력 보존 기간과 접근 권한은 개인정보·법무 기준으로 별도 확정한다.

## 5. 도메인과 application 변경

### 5.1 `ConsentPolicy`

- `app.consent.quality-review-version` 설정을 추가한다.
- 기동 시 null·blank version을 거절한다.
- privacy·terms 필수 검증은 변경하지 않는다.
- quality review `true`에는 current version exact match를 요구한다.
- quality review `false`는 stale version 때문에 거절하지 않는다.

### 5.2 `UserConsents`

- quality review 세 필드를 추가한다.
- 신규 User 생성 시 요청의 선택 상태를 함께 만든다.
- 기존 필수 동의 renew와 quality review 전이를 하나의 immutable 결과 생성 규칙으로 처리한다.
- 동일 상태·동일 version 요청이면 기존 instance를 반환해 저장과 시각 변경을 피한다.
- true→false에서는 version과 consentedAt을 null로 만든다.

### 5.3 `User`

- `updateConsents`가 필수 동의 갱신과 quality review 선택 전이를 함께 처리한다.
- 실제 변경이 있을 때만 `updatedAt`을 서버 시각으로 변경한다.
- 기존 ACTIVE 조건과 optimistic timestamp CAS를 유지한다.

### 5.4 `UserConsentService`

- request 검증 후 현재 ACTIVE User만 갱신한다.
- 저장 실패 시 기존 `USER_UPDATE_CONFLICT` 정책을 유지한다.
- 동일 요청은 저장 없는 성공으로 반환한다.
- 로그에는 userId·accountType·outcome만 사용하고 동의 세부값이나 요청 본문은 추가하지 않는다.

## 6. `main` hotfix 적용 범위

필수 적용 경로:

- `GuestAuthRequest`
- `UserConsentUpdateRequest`
- `UserConsentStatusResponse`
- `UserConsentResponse`

현재 `main`에는 Guest와 LOCAL signup이 있고 Firebase signup·Phone eligibility publisher는 없다. 이번 hotfix에는 `develop`의 Firebase·TMI-95·TMI-96 코드를 가져오지 않는다.

이번 외부 request 변경은 사용자가 지정한 다음 세 endpoint로 제한한다.

- `POST /api/v1/auth/guest`
- `PUT /api/v1/users/me/consents`
- `GET /api/v1/users/me/consents`

`main`의 LOCAL `POST /api/v1/auth/signup`에는 이번 hotfix에서 새 request 필드를 추가하지 않는다. 해당 경로로 생성되는 User의 quality review 상태는 명시적으로 false/null/null로 초기화하고, 가입 뒤 인증된 PUT에서 사용자가 선택할 수 있게 한다. 어느 생성 경로도 묵시적으로 true를 저장하지 않는다.

LOCAL signup 화면에도 즉시 선택 동의를 노출해야 한다는 별도 제품 요구가 확인되면 `SignupRequest` 추가를 같은 hotfix 범위에 명시적으로 포함한다. 요구 확인 없이 Firebase signup DTO나 `develop` 전용 코드를 가져오지 않는다.

현재 `main` 패키지 구조에서 예상되는 주요 수정 파일은 다음과 같다.

- `domain/auth/dto/request/GuestAuthRequest.java`
- `domain/auth/application/GuestAuthService.java`
- `domain/user/domain/ConsentPolicy.java`
- `domain/user/domain/UserFactory.java`
- `domain/user/domain/entity/UserConsents.java`
- `domain/user/domain/entity/User.java`
- `domain/user/application/UserConsentService.java`
- `domain/user/dto/request/UserConsentUpdateRequest.java`
- `domain/user/dto/response/ConsentPolicyStatusResponse.java` 또는 optional 전용 응답 factory/DTO
- `domain/user/dto/response/UserConsentStatusResponse.java`
- `domain/user/dto/response/UserConsentResponse.java`
- `domain/user/domain/repository/UserRepositoryCustomImpl.java`
- `domain/user/exception/UserErrorStatus.java`
- `domain/auth/api/AuthController.java`
- `domain/user/api/UserController.java`

실제 구현 시작 시 반드시 `main` 파일을 다시 읽고 위 목록을 확정한다. `develop`의 refactor 이후 경로를 main에 복사하지 않는다.

실제 구현에서는 `UserRepositoryCustomImpl`의 기존 embedded `consents` 전체 교체와 ACTIVE+`updatedAt` CAS가 요구사항을 이미 충족해 실행 코드는 변경하지 않고 회귀 테스트만 보강했다.

## 7. 외부 데이터 소유 서비스 연동

Identity의 false 저장만으로 실제 품질 검토 철회가 완료되지는 않는다. 답안·음성·시험 결과는 Identity 소유 데이터가 아니므로 다음 계약이 필요하다.

```text
Identity quality review 상태 변경 commit
→ versioned consent changed/revoked outbox event
→ 데이터 소유 서비스의 멱등 consumer
→ 이후 품질 검토 대상 선정 중지
→ 승인된 기존 데이터 보존·삭제 정책 적용
```

이 lifecycle event와 consumer 구현은 Identity의 현재 API 변경과 분리된 ADR·Jira로 진행한다. 외부 consumer가 준비되기 전에 quality review 데이터를 실제로 수집·검토하는 기능을 활성화하지 않는다.

철회 시 이미 완료된 통계·모델 결과에 대한 처리, 원본 데이터 삭제 여부, 법정 보존 예외와 처리 완료 SLA는 제품·개인정보·법무 기준으로 확정한다.

## 8. 하위 호환과 배포 순서

새 GET response object는 additive 변경이므로 기존 client는 알 수 없는 `qualityReview` 필드를 무시할 수 있다.

반면 POST·PUT의 신규 request 필드를 즉시 `@NotNull`·`@NotBlank`로 강제하면 구버전 client가 400을 받을 수 있다. 초기 backend는 다음 호환 규칙을 사용한다.

```text
isQualityReviewConsented 누락
→ false

qualityReviewConsentVersion 누락
AND qualityReview consent가 true가 아님
→ 허용
```

기능 배포 순서는 다음과 같다.

1. Backend를 누락=false 호환 모드로 배포한다.
2. GET response에 qualityReview를 추가한다.
3. Frontend가 두 요청 필드를 항상 전송하고 선택 UI·철회 UI를 사용하도록 배포한다.
4. 구버전 client 비율과 4xx를 관찰한다.
5. 명시적 선택값 강제가 필요한 경우 별도 버전 또는 승인된 cutoff 뒤 validation을 강화한다.
6. 외부 lifecycle consumer 검증 전에는 실제 품질 검토 이용을 활성화하지 않는다.

### 8.1 Git hotfix 격리 절차

현재 기본 작업 트리에는 TMI-96의 tracked·untracked 변경이 많으므로 그 작업 트리에서 `main`으로 checkout하지 않는다. 별도 worktree를 `origin/main`에서 만든다.

사용자가 실행할 권장 순서는 다음과 같다.

```shell
git fetch origin
git worktree add ../identity-quality-review-hotfix \
  -b hotfix/quality-review-consent origin/main
```

새 worktree에서만 quality review 계획서와 hotfix 코드를 추가한다.

```shell
cd ../identity-quality-review-hotfix
git status --short
./gradlew clean test
```

PR 생성 전에는 다음을 확인한다.

```shell
git diff --stat origin/main...HEAD
git diff --name-only origin/main...HEAD
```

diff에는 quality review 동의, 관련 설정·문서·테스트만 있어야 한다. 다음 변경이 포함되면 안 된다.

- TMI-95 ADR 병합 전체
- TMI-96 Phone eligibility publisher
- Firebase signup foundation 이후의 `develop` 전용 기능
- auth vertical slice 전체 refactor
- 그 밖의 `main`에 없는 develop commit

hotfix PR의 base는 `main`으로 지정한다. `develop`을 `main`으로 merge하거나 hotfix branch에 `develop`을 merge하지 않는다.

긴급 main 반영 이후 develop에도 같은 quality review 수정이 필요하면, 이는 별도 후속 작업으로 main hotfix commit만 cherry-pick하거나 develop 구조에 맞게 다시 구현한다. 이 동기화는 이번 main hotfix PR과 분리하며 develop의 다른 commit을 main에 포함시키지 않는다.

## 9. 설정과 문서

추가 설정 이름:

```text
app.consent.quality-review-version
QUALITY_REVIEW_CONSENT_VERSION
```

실제 정책 내용이나 개인정보를 설정에 넣지 않고 version 식별자만 주입한다.

다음 문서를 함께 갱신한다.

- README의 Guest·동의 PUT·GET 예시
- `.env.example`
- `application.yml`
- `application-test.yml`의 가짜 version
- Controller OpenAPI 예시와 schema
- 프론트 API 계약 문서

## 10. 테스트 계획

### 요청 검증

- Guest privacy·terms 필수 규칙 회귀
- quality review false로 Guest 생성 성공
- quality review true와 current version으로 성공
- quality review true와 누락·잘못된 version 거절
- 호환 기간의 quality review 필드 누락을 false로 처리
- false와 stale version이 철회를 차단하지 않음

### 도메인 상태

- 기존 문서 누락 필드를 false/null로 읽음
- false→true에서 current version과 서버 시각 저장
- true→true 동일 version은 no-op과 기존 시각 유지
- true old version→true current version은 시각 갱신
- true→false에서 version·시각 null
- false→false는 no-op
- privacy 또는 terms 한쪽 version만 변경될 때 기존 시각 보존 회귀

### API 응답

- GET qualityReview false/null/null/false 응답
- current version에 유효한 true 응답
- old version 저장 상태를 현재 유효 동의로 사용하지 않음
- PUT 성공 응답에 저장 결과 포함
- nullable OpenAPI schema 검증

### 저장·동시성

- ACTIVE User만 갱신
- CAS 충돌은 `USER_UPDATE_CONFLICT`
- false 전이 이후 DB에 과거 quality review version/time이 남지 않음
- 저장 실패 시 User 상태가 외부에 성공으로 노출되지 않음

### 보안·관측

- 요청·응답 본문과 동의 상세값을 로그에 남기지 않음
- 사용자 입력 시각을 저장하지 않음
- 기존 인증·Guest·회원가입·프로필·탈퇴 테스트 회귀
- 외부 Atlas나 실제 데이터 소유 서비스를 테스트에서 호출하지 않음

기본 최종 검증은 `./gradlew clean test`다.

## 11. 완료 조건

- 고정한 외부 필드명으로 Guest·PUT·GET 계약이 문서와 코드에서 일치한다.
- privacy·terms는 계속 필수이고 quality review false는 가입·일반 기능을 차단하지 않는다.
- true만 current version과 서버 시각으로 유효하게 저장된다.
- false 철회는 stale client version으로 차단되지 않는다.
- false 저장 상태의 version·consentedAt은 null이다.
- 반복 요청은 멱등이며 기존 동의 시각을 불필요하게 변경하지 않는다.
- 기존 문서와 구버전 client가 안전하게 false로 호환된다.
- OpenAPI·README·환경변수 예시와 테스트가 갱신된다.
- 전체 테스트가 성공한다.
- 외부 데이터 이용 중지는 별도 owner·event·consumer·보존 정책이 준비되기 전 활성화되지 않는다.
- PR base가 `main`이고 diff에 quality review 관련 변경만 포함된다.
- `develop`의 TMI-95·TMI-96·Firebase·refactor commit이 main hotfix에 포함되지 않는다.

## 12. 제외 범위

- Learning Core의 답안·시험 결과 구현
- 음성 파일 저장·삭제 구현
- 품질 검토 운영 도구와 검토자 권한
- 모델 학습·통계 결과의 삭제 정책 구현
- 법적 보존 기간의 임의 결정
- Git commit·push와 운영 배포
- `develop`을 `main`으로 병합하는 작업
- TMI-96 작업 트리의 변경 이동·stash·reset

위 항목은 별도 owner와 Jira에서 처리한다.
