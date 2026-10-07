# 단일 SNS 계정 정책·전화 인증 계정 찾기 통합 구현 계획

- 작성일: 2026-10-03. 기준 브랜치: `develop`.
- 상태: 2026-10-03 TMI-192 저장소 구현 반영. 아래 본문은 승인 당시 계획/조사 기록이며 최신 구현 차이·정확한 설정·출시 gate는 [구현 계약·배포 런북](single-sns-account-recovery-runbook.md)을 따른다. 실제 앱·외부 인프라 배포 검증은 별도다.
- 사용자 요청: SNS 추가 연결 폐지, 전화번호당 활성 회원 하나, 전화 인증 후 SNS 종류와 마스킹 이메일 안내를 **하나의 개발 범위와 출시 단위**로 진행한다.
- Jira: [TMI-192](https://to-teacher.atlassian.net/browse/TMI-192), 상위 에픽 [TMI-136](https://to-teacher.atlassian.net/browse/TMI-136). 구현 요청에 따라 작업 진행. Jira 조회 당시 상태 `해야 할 일`이며 이번 구현에서 상태를 자동 변경하지 않았다.

## 1. 5줄 결론

1. SNS 회원은 하나의 `(provider, providerSubject)`만 소유하도록 가입·승격·로그인과 DB 제약을 함께 수정한다. [현재 저장 모델](../../src/main/java/web/tosunsaeng/identity/domain/auth/domain/entity/SocialIdentity.java)
2. MEMBER의 SNS 추가 연결과 exchange 자동 추가 등록은 폐지하고 Guest prepare·upgrade·merge는 제공한다. [현재 자동 등록](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderLoginRegistrationService.java)
3. 전화 인증 후 기존 활성 회원의 SNS 종류와 마스킹 이메일을 안내하는 계정 찾기를 이번 구현에 포함한다. [재사용할 번호 조회 계약](../../src/main/java/web/tosunsaeng/identity/domain/auth/phoneidentity/repository/PhoneFingerprintAliasRepositoryCustom.java)
4. 현재 SNS 이메일을 보관하지 않으므로 Firebase의 해당 SNS 프로필에서 이메일 힌트를 수집·마스킹·저장하는 기능도 함께 만든다. [현재 provider 데이터](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/infrastructure/firebase/FirebaseLinkedProviderData.java)
5. 서버·프론트 연동 계약·테스트 도구·설정·배포 점검을 한 계획으로 완료한다. 본 문서 작성 시 제품 코드·실제 앱·외부 인프라·배포는 변경하지 않았다.

## 2. 사용자가 반드시 읽어야 하는 내용

### 이번에 완성할 사용자 경험

| 상황 | 변경 후 동작 |
| --- | --- |
| 신규 SNS 가입 | SNS 하나와 전화 인증으로 MEMBER 생성 |
| Guest에서 신규 회원 가입 | 기존 Guest userId를 유지하며 SNS 하나를 가진 MEMBER로 승격 |
| Guest에서 기존 회원 로그인 | 해당 회원의 등록된 SNS로 인증하고 기존 Guest 병합 절차 진행 |
| 기존 회원에 다른 SNS 추가 | 추가 연결 기능 제거, 서버도 등록 거절 |
| 어떤 계정으로 가입했는지 모름 | 전화 OTP 인증 후 `Google / a***@example.com` 형태의 힌트 표시 |
| 이메일을 제공하지 않는 SNS | SNS 종류만 표시하고 이메일 힌트 없음으로 처리 |
| Apple 비공개 이메일 | `Apple로 가입한 계정입니다` 및 비공개 이메일 사용 안내; 실제 개인 이메일을 추정하지 않음 |
| 전화번호에 조회 가능한 회원 없음 | 계정 찾기 결과 `NOT_FOUND`, 가입 경로 안내 |

계정 찾기는 전화 인증 후 가입 SNS와 이메일 힌트를 보여주면 완료되는 독립 기능이다. Guest 토큰이나 병합 API가 필요하지 않고, 조회 성공이 로그인·승격·병합을 자동 실행하지 않는다. 이후 사용자가 로그인을 선택하면 기존 SNS 로그인 흐름으로 진입한다. Guest로 사용하던 사람이 별도로 기존 회원 로그인을 진행하는 경우에만 기존 Guest 병합 정책이 적용될 수 있다. 이는 계정 찾기의 처리 단계나 완료 조건이 아니다. 전화 인증만으로 MEMBER 토큰을 발급하거나 Guest를 병합하지 않는다.

이번 범위에 마스킹 이메일까지 포함한다. 이메일 미제공은 지원해야 하는 정상 결과이며, 해당 사용자를 가입 실패로 만들지 않는다. 회원당 SNS 하나 정책은 SNS 계정에 적용하며 기존 LOCAL 이메일/비밀번호 회원가입 기능 삭제를 뜻하지 않는다.

### 현재 구현에서 확인한 사실

- `SocialIdentity`는 `provider`, `providerSubject`, `userId`를 저장하고 `(provider, providerSubject)`만 unique다. 현재 `userId`에는 일반 인덱스가 있으므로 회원당 SNS 하나는 DB가 보장하지 않는다.
- `FirebaseSignupService`와 `FirebaseGuestUpgradeService`는 여러 `linkedSocialPrincipals`를 저장할 수 있다.
- `ProviderLoginRegistrationService`는 exchange에서 Google/Apple/Kakao의 새 SocialIdentity를 등록할 수 있다. link API 차단만으로는 정책이 완성되지 않는다.
- SNS 회원의 `User.email`은 현재 null이고 LOCAL 자격증명 필드들과 연관된 불변조건이 있다. 이 필드를 SNS 안내용으로 재사용하지 않는다.
- `FirebaseSdkAdminClient`는 Admin SDK의 provider 목록을 읽지만 현재 `FirebaseLinkedProviderData`에 이메일을 담지 않는다. `VerifiedFirebasePrincipal`의 `emailVerified`는 이메일 주소가 아니다.
- 기존 번호 소유 검증은 다른 활성 userId의 전화번호 연결을 `PHONE_ALREADY_LINKED`로 거절한다. 기존 HMAC 전화 fingerprint와 retained key 조회를 재사용할 수 있다.
- 현재 일반 Firebase verifier는 primary 인증 수단을 요구한다. 새로운 전화 전용 계정 찾기에 그대로 사용하거나 일반 로그인 검증 조건을 완화해서는 안 된다.

## 3. 사용자가 결정해야 하는 사항

2026-10-03 대화 기준, 계획 진행을 막는 필수 사용자 결정은 남아 있지 않다. 기존 Firebase/SMS 재사용까지 방향이 정리되었다. 아래 이메일 표시·인증 유효기간·요청 제한은 계획서 기본안으로 두고, SDK 연동 및 실제 데이터/인덱스 확인은 구현·배포 검증으로 처리한다. 이번 계정 찾기는 기존 SNS 접근 권한 복구나 다른 SNS로의 교체를 포함하지 않는다.

### 이미 확정된 범위

- 단일 SNS 제한과 계정 찾기·마스킹 이메일을 분리 출시하지 않고 한 번에 개발한다.
- Guest prepare/upgrade/merge를 유지한다.
- 테스트 서버에 회원이 없다는 사용자 설명을 전제로 기존 복수 SNS 회원 이행 기능은 만들지 않는다. 실제 데이터 확인은 배포 전 수행한다.
- 계정 찾기로 SNS 교체·추가, MEMBER 간 병합, 전화번호 이전을 수행하지 않는다.

### 적용할 기본안과 구현·배포 확인 사항

인증 상태 구분: Guest 인증은 Identity가 발급한 자체 JWT이고 Firebase SDK의 currentUser와 별개다. 전화 로그인 자체가 Guest JWT를 폐기하거나 Guest 데이터를 없애지는 않는다. 보존할 것은 앱이 관리하는 Guest 토큰/source 정보이며, Firebase 인증 상태 변경을 이유로 앱이 이를 지우거나 덮어쓰지 않게 해야 한다. 별도 Auth 인스턴스는 클라이언트 Firebase 로그인 상태를 분리하고, 별도 프로젝트는 Firebase 서버의 사용자/전화 credential 공간까지 분리한다. 같은 프로젝트의 두 Auth 인스턴스는 서버 계정 공간을 공유한다. 이번 구현은 사용자의 요청에 따라 기존 프로젝트를 재사용한다.

1. **2026-10-03 사용자 방향 반영: 기존 Firebase 프로젝트/tenant/서버 자격증명을 재사용한다. 새 프로젝트 생성은 범위에서 제외한다.** 계정 찾기의 전화 증명을 검증하는 서버 로직을 추가하며 기존 Firebase 설정을 사용한다.
2. 앱은 기존 SMS 인증 기능으로 전화번호 소유를 확인하고, Firebase SDK에서 검증 가능한 전화 인증 결과(ID Token)를 받아 lookup에 전달한다. SDK 내부에서 전화 credential로 sign-in하는 단계가 있어도 앱의 전화번호 회원 로그인 기능을 추가한다는 뜻은 아니다. 계정 찾기 때문에 현재 SNS 계정에 `linkWithCredential`로 번호를 추가하지 않는다. 클라이언트 Auth 인스턴스 재사용/별도 인스턴스 선택은 SDK의 상태 관리 구현 사항이며 새 Firebase 프로젝트나 새 서버를 뜻하지 않는다. 기존 전화 소유 UID 선택·신규 phone-only UID 생성·후속 가입·cleanup의 상호작용은 통합 테스트에서 검증한다.
3. 이메일 표시 기본안은 마스킹 이메일, Apple relay는 주소 대신 비공개 사용 표시다. 실제 이메일 원문 공개는 범위에 없다.
4. 실제 앱은 별도 저장소이므로 이 저장소에서는 API 계약과 테스트 화면을 구현한다. 전체 기능 완료에는 앱 저장소의 OTP 화면·Firebase 인증 상태 관리·오류 처리 구현 및 통합 QA도 포함한다.

## 4. 주요 위험과 미확인 사항

- Firebase provider별 `UserInfo.getEmail()` 제공 여부와 Apple/Kakao의 미제공 사례는 실제 앱·테스트 환경에서 검증해야 한다. 루트 `UserRecord.email`을 다른 SNS의 이메일로 간주하지 않는다.
- 전화번호 인증은 현재 번호 소유를 증명한다. 번호 재할당/SIM 탈취에 대비해 안내를 최소화하고, SNS 재인증 없이 로그인 권한을 부여하지 않는다.
- 클라이언트가 Firebase SDK를 직접 호출할 수 있으므로 Identity의 link API 제거가 Firebase 원격 계정의 복수 provider 연결 자체를 막는 것은 아니다. 원격 상태와 Identity의 승인 상태가 달라도 추가 권한이 생기지 않도록 한다.
- 기존 회원·진행 중 link/unlink 작업이 없다는 사실은 DB/Firebase 실조회로 확인하지 않았다. 발견 시 배포를 멈추고 개별 정리 대상을 보고한다. 전체 DB 삭제·무조건적인 block 해제는 계획에 없다.
- unique 인덱스 생성, 여러 서버 인스턴스에서 요청 제한·challenge 소비 경합, Mongo replica set 트랜잭션은 mock 테스트만으로 검증 완료라고 볼 수 없다.
- SNS 이메일은 표시 힌트일 뿐이며 소유권 판단·회원 중복 판별·인증 성공의 근거로 사용하지 않는다.
- 기존 LOCAL 및 Firebase PASSWORD 로그인 경로는 별도 회귀 대상이다. SNS 회원의 단일 승인 SNS 검사를 PASSWORD/PHONE 인증으로 우회할 수 없어야 한다.

## 5. 현재 작업과 직접 관련된 설명

### 5.1 단일 SNS 정책과 저장 제약

`SingleSocialIdentityPolicy`(신규 제안)에 가입/승격 검증과 기존 회원 인증 검증을 분리해 둔다.

- **가입·승격:** 지원되는 SNS principal이 정확히 하나이고 해당 provider subject가 유효해야 한다. 전화 provider는 SNS 개수에 포함하지 않는다. SNS가 둘 이상이면 요청을 거절한다. Firebase raw provider 목록에서 비활성화된 지원 SNS까지 파악하여 설정 OFF 때문에 복수 연결이 단일로 오인되지 않게 한다.
- **기존 회원 로그인:** 현재 인증한 SNS의 `(provider, subject)`가 그 회원의 유일한 SocialIdentity와 정확히 일치해야 한다. Firebase UID 일치만으로 다른 SNS를 승인하지 않는다. 저장된 SNS가 없거나 둘 이상이면 임의 생성·선택하지 않고 불일치로 처리한다.
- 원격 계정에 추가 provider가 존재해도 기존 승인 SNS로 한 로그인은 허용하는 방향으로 한다. 추가 provider로 한 로그인은 거절한다. 이를 통해 원격 SDK의 자동 link가 기존 정상 SNS 로그인까지 막지 않게 한다.
- 기존 `ProviderChangeGuard`의 block/authentication floor와 세션 epoch 검증은 유지하고, exchange에서 누락된 provider를 자동 등록하도록 통과시키던 예외만 제거한다.
- `social_identities.userId`에 unique 인덱스 `uk_social_identities_user_id`를 추가한다. 기존 일반 인덱스 대체 절차와 `(provider, providerSubject)` unique 유지 여부를 명시한 인덱스 적용 스크립트/런북을 제공한다.
- 가입·승격의 SocialIdentity 저장은 기존 원자적 트랜잭션 안에서 수행한다. 사전 count 조회만으로 동시성 제약을 구현하지 않는다. duplicate key는 충돌한 인덱스에 따라 소유권 충돌 또는 단일 정책 위반으로 매핑하고 모든 중간 쓰기를 롤백한다.
- 다른 사용자의 활성 전화번호 중복 검증과 retained key alias 조회는 유지한다.

### 5.2 API 제거·유지 및 Guest 흐름

| 현재 경로/구성요소 | 이번 변경 |
| --- | --- |
| `/api/v1/auth/firebase/providers/link/*` | 정상 제품의 추가 연결 API/프론트 호출 제거. 폐지 응답 adapter만 유지하여 `410 PROVIDER_LINK_RETIRED` 반환, SDK 실행·DB 등록 없음 |
| `ProviderLinkService`의 link 실행 코드 | 런타임 신규 실행 경로에서 제거. 진행 작업이 없는지 검증 후 불필요한 service/DTO/config/test 제거 |
| `/providers/unlink`, `/providers/unlink/status` | 단일 SNS 제품에서 사용하지 않는 공개 흐름도 폐지 응답으로 전환. 마지막 로그인 수단을 제거하는 대체 API는 만들지 않음 |
| `ProviderLoginRegistrationService` | 세션/소유권 검증과 발급 트랜잭션은 유지하고 SocialIdentity 자동 추가만 제거. 역할에 맞는 이름으로 정리 |
| `/auth-methods/sync` | 조회·검증 전용 유지. 배열 형태의 기존 응답은 유지하되 SNS 회원에서 원소는 하나. 추가 등록/재연결 기능 없음 |
| `/guest/prepare`, `/guest/upgrade` | 경로·Guest 인증·enrollment 계약 유지. 신규 회원 생성에는 단일 SNS 검증 적용 |
| `/guest/merge` | 현재 인증 SNS로 대상 소유권 증명, 기존 Guest 병합 계약 유지. 다른 연결 SNS 전체를 순회해 대상을 선택하지 않도록 resolver 검토·수정 |
| 탈퇴 cleanup·세션 보호 | 내부 block/floor·탈퇴 정리 worker와 증거 보존. 공개 link 기능 제거를 이유로 삭제하지 않음 |

Guest merge에서는 현재 인증 SNS가 대상의 유일한 승인 SNS이고 Firebase binding과 모순되지 않는지 확인한다. 별도 phone proof를 merge의 SNS proof로 사용하지 않는다. 기존 source/target 상태 오류, 중복 병합 및 outbox/하위 서비스 처리는 그대로 회귀 검증한다.

`PROVIDER_RELINK_REQUIRED`를 모든 상황에서 새로운 단일 정책 오류로 치환하지 않는다. 내부 block/보안 상태 불일치와 새 SNS 추가 시도를 구분한다. 남아 있는 해당 오류의 프론트 안내는 자동 link/prepare 호출 대신 계정 찾기 또는 고객지원이며, 계정 찾기가 block을 해제한다는 의미는 아니다.

폐지 adapter의 410은 기존 보안 체인을 통과한 요청에 적용한다. 원래 인증이 필요했던 endpoint를 410 응답만을 위해 공개하지 않으며, 자격증명이 없거나 잘못된 요청의 기존 401/403 우선순위는 유지한다.

### 5.3 SNS 이메일 힌트 수집·저장

1. `FirebaseSdkAdminClient`에서 **선택된 SNS의 providerId와 providerUid가 일치하는 provider 프로필**의 이메일만 읽는다. 클라이언트가 보낸 이메일, 전화 OTP 사용자의 이메일, 다른 provider의 루트 이메일은 사용하지 않는다.
2. `FirebaseLinkedProviderData` → 검증된 social principal까지 표시용 이메일을 전달한다. DTO의 `toString`, 예외, 요청/응답 로깅에서 이메일을 제거한다. 이메일 검증 여부가 보장되지 않은 값을 검증 완료 이메일이라고 명명하지 않는다.
3. 이메일 원문은 처리 중에만 사용하고 `SocialIdentity`에는 `maskedEmail`(nullable), `emailHintKind`(`EMAIL`, `APPLE_PRIVATE_RELAY`, `UNAVAILABLE`), `emailHintUpdatedAt`(nullable)을 추가한다. `User.email`과 로그인/중복 확인 규칙은 변경하지 않는다.
4. 가입·Guest 승격 트랜잭션에서 함께 저장한다. 정상 기존 SNS 로그인 시 같은 provider subject를 확인한 경우 힌트를 갱신할 수 있다. 오래된 principal이 최신 snapshot을 덮어쓰지 않도록 snapshot 관측 시각 비교를 적용한다. 해당 필드는 권한 증거가 아니다.
5. 계정 찾기는 저장된 힌트를 조회한다. 조회 요청마다 다른 회원의 Firebase 계정에서 이메일을 원격 수집하지 않는다. 탈퇴 즉시 결과에서 제외하며 SocialIdentity 삭제 시 힌트도 함께 삭제된다.

마스킹 규칙(이번 구현 기본안): 정상 이메일은 local-part가 2글자 이상이면 첫 글자만 남기고 고정 `***`를 붙인다. 1글자이면 local-part 전체를 `***`로 숨긴다. 실제 길이를 별표 수로 드러내지 않는다. 도메인은 표시하되 형식 오류/제어문자/길이 제한 초과는 `UNAVAILABLE`로 처리한다. Apple의 relay 도메인이면 이메일 표시 없이 `APPLE_PRIVATE_RELAY`를 반환한다. HTML로 렌더링하지 않는다.

### 5.4 계정 찾기 API 초안

경로는 신규 제안이다. 모든 응답은 기존 `BaseResponse`의 `isSuccess`, `code`, `message`, `result`를 사용한다. 아래 예시는 **result 본문만** 보여준다. 두 경로 모두 Identity JWT 없이 호출할 수 있지만 결과 조회에는 최근 전화 인증이 필수다. `Cache-Control: no-store`를 적용한다.

**POST `/api/v1/auth/account-recovery/prepare`**

- 본문 `{}`. 전화번호·email·userId를 받지 않는다.
- 결과: `recoveryId`(불투명 UUID), `expiresAt`(UTC ISO-8601).
- prepare만으로 계정 존재 여부를 알 수 없다. 서버가 SMS를 보내는 API는 아니며 SDK의 전화 인증 시작 전에 호출한다.

**POST `/api/v1/auth/account-recovery/lookup`**

- 본문 필드: `recoveryId`, `firebaseIdToken`(계정 찾기 전용 전화 인증의 증명).
- 전화번호는 서버가 검증한 증명에서만 얻는다. request에 전화번호·email·조회 대상 userId를 추가하지 않는다.
- `result` 예:

```json
{
  "status": "FOUND",
  "provider": "GOOGLE",
  "maskedEmail": "a***@example.com",
  "emailHintKind": "EMAIL"
}
```

| `status` | `provider` | `maskedEmail` | `emailHintKind` | 프론트 |
| --- | --- | --- | --- | --- |
| `FOUND` | `GOOGLE` / `APPLE` / `KAKAO` | 문자열 또는 null | `EMAIL` / `APPLE_PRIVATE_RELAY` / `UNAVAILABLE` | SNS 로그인 버튼과 힌트 표시 |
| `NOT_FOUND` | null | null | null | 조회 가능한 계정 없음 안내 |
| `ACTION_REQUIRED` | null | null | null | 계정 상태 확인/지원 안내; 자동 가입·연결 금지 |

번호와 연결된 활성 SNS MEMBER가 없거나 탈퇴/해제된 번호만 있으면 `NOT_FOUND`. 활성 alias가 가리키는 계정이 정지/정리 중이거나 승인 SNS가 block/불일치 상태이면 `ACTION_REQUIRED`. 동일 번호로 서로 다른 활성 owner가 조회되면 내부 무결성 오류로 처리하고 구체 계정 정보는 반환하지 않는다. LOCAL/PASSWORD-only 계정은 이번 SNS 안내에서 `ACTION_REQUIRED`로 처리하며 그 계정의 로그인/번호 소유를 삭제하거나 새 SNS를 자동 연결하지 않는다.

응답에는 이메일 원문, 전화번호, userId, Firebase UID, provider subject, fingerprint, Access/Refresh Token을 포함하지 않는다. `provider`는 `User.provider=FEDERATED`가 아니라 `SocialProvider`를 뜻한다.

### 5.5 전화 증명 검증·challenge·응답 유실

- `AccountRecoveryPhoneVerifier`를 별도 검증 로직으로 만들되 기존 Firebase 프로젝트/tenant와 Admin 연결을 재사용하여 서명·issuer·audience·만료·revoke·disabled 상태를 검증한다. 같은 프로젝트의 Firebase 토큰 자체에 계정 찾기 전용 목적이 자동 삽입되는 것은 아니다. lookup의 challenge/전화 method/최근 인증을 서버에서 검증하며, 일반 exchange/merge의 승인 SNS 조건과 기존 signup/upgrade의 enrollment·소유권·전화 인증 조건을 완화하지 않는다. lookup은 회원 세션/enrollment를 생성하지 않는다.
- `sign_in_provider=phone`이어야 한다. 단순히 계정에 전화번호가 연결되어 있다는 `phoneVerified=true`만으로 조회를 허용하지 않는다.
- 토큰 phone claim과 현재 Admin 계정의 E.164 전화번호가 일치해야 한다. `auth_time`은 prepare 이후(초 단위 절삭 기준)이며 현재부터 최대 10분 이내여야 한다. 강제 토큰 refresh로 `iat`만 바뀐 오래된 인증은 거절한다. 미래 시간은 최대 30초 허용오차를 적용하고 exp 만료도 별도로 검사한다.
- Firebase 전화 인증에 서버 challenge nonce가 자동 바인딩된다고 가정하지 않는다. challenge는 서버의 유효기간·인증시각·원자 소비·전화 proof 재사용 기록으로 연결한다.
- 신규 `AccountRecoveryAttempt`: `recoveryId`, `createdAt`, `expiresAt`, `state=PENDING/CONSUMED`, `proofFingerprint`, `consumedAt`, `retryUntil`, `cleanupAt`. 원문 전화번호/증명/이메일 저장 없음.
- 기본 유효기간은 prepare부터 10분. 유효한 전화 proof를 원자적으로 소비하고 lookup을 수행한다. 소비 실패/조회 실패가 중간 상태를 남기지 않도록 소비 및 로컬 결과 읽기의 트랜잭션 경계를 테스트한다.
- proof fingerprint는 전용 HMAC으로 `(project, tenant, uid, auth_time, normalized phone)`를 식별하며 원문 증명은 저장하지 않는다. 같은 인증으로 여러 challenge를 소비하지 못하도록 별도 unique 소비 기록을 사용한다. refresh로 바뀌는 `iat`는 proof 식별 기준에 넣지 않는다.
- HMAC 회전 시 유효한 proof/재시도 기간에 걸치는 이전 키를 유지하고 모든 retained 버전의 소비 기록을 검사한다. 신규 소비는 버전별 alias를 동일 트랜잭션에서 기록하여 키 변경 전후 동시 소비도 중복 허용하지 않는다.
- 응답 유실 시 **같은 recoveryId와 같은 인증 proof**로 재시도한다. 소비 후 최대 5분의 retry 기간 동안 현재 계정/번호 상태를 다시 검증해 결과를 재생성한다. 이미 탈퇴했다면 이전 힌트를 재전송하지 않는다. 다른 proof로 재사용하면 409를 반환한다. proof가 만료되면 재인증한다.
- TTL 정리는 만료 판정을 대신하지 않는다. attempt는 retry 종료 후 24시간 이내 정리, proof 소비 기록은 auth_time 재사용 가능 기간을 넘겨 최소 24시간 보관 후 정리하는 기본안이다.
- 전화번호는 기존 `PhoneNumberNormalizer` → `PhoneFingerprintHasher` → retained fingerprint aliases 순서로 조회한다. 같은 회원의 여러 keyVersion alias는 한 owner로 합친다. aliases와 PhoneIdentity/User의 현재 상태를 함께 확인한다.

### 5.6 요청 제한·설정·운영

다음은 신규 설정 기본안이며 기존 환경변수라고 주장하지 않는다.

| 설정 범위 | 기본안 |
| --- | --- |
| 계정 찾기 기능 | 준비 완료 전 OFF, 통합 출시 시 ON; 단일 SNS 정책은 서버 불변조건으로 적용 |
| Firebase | 기존 project ID/tenant/Admin 자격증명 재사용. 새 프로젝트·추가 service account는 요구하지 않음; 콘솔 Phone/SMS/앱 검증 설정 확인 |
| challenge / 전화 최근 인증 / 응답 재시도 | 10분 / 10분 / 5분, 서버 Clock 사용 |
| prepare 제한 | IP당 15회/분 |
| lookup 사전 제한 | IP당 20회/분, proof 검증 전에 적용 |
| lookup 증명 후 제한 | 인증 UID 및 인증 번호당 각각 5회/15분; FOUND/NOT_FOUND/재시도 모두 계산 |
| 제한 응답 | 429, 로컬 제한은 거절된 고정 시간창의 남은 초를 올림한 `Retry-After`; Firebase 자체 제한처럼 해제 시각 미상이면 헤더 생략 |
| Firebase/SMS 남용 방지 | SDK 앱 검증, SMS 발송 제한/할당량 설정. Identity 제한만으로 SDK 직접 SMS 호출이 제한된다고 보지 않음 |

기존 `ProviderRequestBudget`는 프로세스 내부 제한이다. 신규 공개 계정 찾기는 Mongo 원자 counter/TTL 등 인스턴스 간 공유 제한을 적용한다. 장애 시 무제한 허용하지 않는다. IP는 신뢰하는 프록시에서 전달된 값만 사용한다. 번호·UID·IP 제한 키에는 분리된 HMAC을 쓰며 key rotation 시 이전/현재 버전의 카운터를 함께 검사해 제한을 우회하지 않게 한다. 최종 환경변수 이름·한도는 설정 클래스/문서/테스트를 함께 맞춘다.

같은 프로젝트에서 전화 로그인하면 해당 전화번호의 기존 Firebase 사용자로 로그인하거나, 번호가 미등록이면 phone-only 사용자가 생성될 수 있다. lookup만으로 Identity 회원/enrollment를 생성하지 않는다. Firebase UID를 전화 조회 대상의 Identity userId로 간주하지 않고 검증된 번호의 활성 alias로 조회한다. phone-only 계정의 후속 SNS 가입에서 전화 credential 중복 문제가 생기지 않도록 재사용/정리 흐름을 통합 검증한다. 계정 찾기 종료 때 기존 Firebase 사용자를 무조건 삭제하거나 공용 프로젝트에 30일 일괄 삭제를 적용하지 않는다. 정리가 필요하면 회원 binding/enrollment/진행 인증 부재와 생성 출처가 입증된 대상만 처리하는 별도 조건을 정한다.

Firebase 설정: 사용자가 기존 SMS 설정이 이미 있음을 확인했으므로 기존 설정을 재사용하며 별도 Phone 제공업체 신규 설정 작업을 요구하지 않는다. Firebase 콘솔의 Phone 로그인 제공업체는 SMS 전화 인증 기능의 명칭이며 앱의 전화번호 로그인 메뉴 추가를 뜻하지 않는다. 기존과 다른 플랫폼/환경에서 동작 문제가 있을 때만 SMS 허용 정책·할당량·결제 상태와 앱 검증 설정을 확인한다. 실제 콘솔/배포 설정은 문서 작성 중 조회하지 않았다.

### 5.7 오류 계약과 프론트 대응

아래 신규 이름은 구현 시 enum/OpenAPI/프론트 문서/테스트에 함께 반영할 제안이다. 기존 오류의 의미를 덮어쓰지 않는다.

| HTTP / code | 발생 조건 | 프론트 동작 |
| --- | --- | --- |
| 410 `PROVIDER_LINK_RETIRED` (신규) | 폐지한 link/unlink 제품 경로 호출 | SDK link 실행 중단, 앱 갱신 안내 |
| 409 `SINGLE_SNS_REQUIRED` (신규) | 가입·승격 증명에 SNS 복수 연결 | 단일 SNS 가입 조건 안내; 자동 unlink 금지 |
| 409 `SNS_ACCOUNT_MISMATCH` (신규) | 기존 회원에 현재 인증 SNS/subject가 승인 수단과 불일치 | 다른 SNS 추가/대체 거절, 계정 찾기 선택 제공 |
| 401 `INVALID_RECOVERY_PROOF` (신규) | 잘못된 프로젝트/서명/phone method/번호 일치/폐기·차단 증명 | 전화 인증 다시 시작, 힌트 없음 |
| 401 `RECOVERY_RECENT_AUTH_REQUIRED` (신규) | 오래된 인증 또는 challenge 이전 인증 | OTP 재인증, 토큰 refresh 반복 금지 |
| 410 `RECOVERY_EXPIRED` (신규) | challenge/retry 유효기간 만료 | prepare부터 재시작 |
| 409 `RECOVERY_CONFLICT` (신규) | 다른 proof로 소비된 ID, 다른 challenge에서 사용한 proof | 새 prepare와 새 OTP 인증 |
| 400 `INVALID_RECOVERY_REQUEST` (신규) | ID/본문 형식 오류 또는 없는 ID | 요청 수정 또는 새 prepare |
| 429 `RECOVERY_RATE_LIMITED` (신규) | 요청 예산 초과 | Retry-After 이후 사용자 재시도 |
| 503 `RECOVERY_UNAVAILABLE` (신규) | 기능 OFF, 검증/조회/제한 저장소 장애, 다중 활성 owner 무결성 위반 | 잠시 후 재시도/지원; NOT_FOUND로 바꾸지 않음 |
| 기존 `PHONE_ALREADY_LINKED` | 가입·승격의 전화 소유 충돌 | 계정 찾기로 기존 SNS 확인 후 정상 로그인·Guest merge |
| 기존 `PROVIDER_RELINK_REQUIRED` | 내부 block/기존 보안 상태 문제 | 자동 link 금지, 계정 찾기 또는 지원 |

인증 실패에서는 먼저 계정 존재를 조회하지 않는다. 사용자 상태별 상세한 정지 사유/탈퇴 기록은 계정 찾기 응답에 넣지 않는다. 예상치 못한 서버 오류를 가입 가능/회원 없음으로 해석하지 않는다.

### 5.8 프론트 작업

1. 회원 설정의 SNS 추가·연결 해제 버튼과 link/relink SDK/API 호출을 제거한다. Guest 로그인·승격·병합 버튼은 유지한다.
2. 로그인 화면과 `PHONE_ALREADY_LINKED`/`SNS_ACCOUNT_MISMATCH` 안내에 계정 찾기 진입점을 제공한다.
3. prepare → 기존 프로젝트에서 전화 OTP → lookup → SNS 종류/힌트 안내로 계정 찾기를 완료한다. 사용자가 로그인 선택 시 기존 SNS 로그인 화면으로 이동한다. Firebase Auth 인스턴스 운영 방식은 앱 SDK에 맞추고 기존 Identity 세션은 보존한다.
4. Recovery Auth에서 인증했다고 앱의 Identity 토큰이나 기존 Guest 인증을 교체하지 않는다. 기존 Guest 토큰·병합 source를 보존하고 실제 upgrade/merge/exchange 성공 계약에 따라 교체한다.
5. 응답 유실은 같은 ID/proof로 재조회하고 화면 취소/완료 시 recovery 세션을 종료한다. 앱 재시작으로 proof를 잃었다면 OTP를 다시 시작한다. credential은 일반 로컬 저장소·분석 로그에 저장하지 않는다.
6. 이메일 null, Apple private relay, NOT_FOUND, ACTION_REQUIRED, 429/503 화면을 제공한다. 복수 Google 계정 선택에서는 힌트를 안내할 뿐 클라이언트 입력 이메일로 서버 소유권을 확정하지 않는다.

### 5.9 구현 순서와 완료 조건

아래는 **동일 개발/출시 안의 내부 작업 순서**다. 계정 찾기나 이메일을 후속 출시로 제외하지 않는다.

1. 기존 Firebase의 전화 인증 설정·API/오류/힌트 규칙 확인, 현 DB/작업/인덱스 사전 점검 방법 작성.
2. 단일 SNS 정책 서비스·unique 인덱스·가입/승격/로그인/merge/sync 변경, link/unlink 폐지 adapter 정리.
3. 이메일 provider 데이터 전달·마스킹 snapshot 저장 및 탈퇴 정리 검증.
4. Recovery verifier·challenge/replay·번호 조회·공유 요청 제한·API 구현.
5. OpenAPI·프론트 인계 문서·테스트 화면·배포 설정/런북 갱신, 실제 앱 구현 연동.
6. 단위·통합 회귀 및 `./gradlew clean test`, 변경한 테스트 도구의 Node 테스트 실행. Provider/Repository는 mock 또는 저장소의 격리 테스트 기반을 사용하고 자동 테스트에서 실제 Atlas/외부 OAuth에 접속하지 않는다.
7. staging 통합 QA에서 기존 Firebase OTP, 기존 전화 UID/신규 phone-only UID 및 후속 가입, provider별 힌트, 원격 자동 link, 동시 요청, 응답 유실, Guest 보존을 검증하고 서버·앱을 조율해 출시한다.

완료 기준: 정상 SNS 가입/로그인/Guest 승격·병합 성공, 어떤 공개 API도 두 번째 SocialIdentity를 저장하지 않음, 전화 인증 전 힌트 비노출, 전화 인증 후 올바른 단일 SNS와 마스킹 힌트 또는 정상 미제공 표시, 회수/탈퇴·유실·동시 요청 검증, 앱 통합 확인까지 모두 만족해야 한다.

## 6. 부록 — 근거·변경 대상·검증 전체 표

### A. 현재 코드 근거와 예정 변경

| 근거 | 확인 사실 / 변경 |
| --- | --- |
| [SocialIdentity](../../src/main/java/web/tosunsaeng/identity/domain/auth/domain/entity/SocialIdentity.java) | userId 일반 인덱스 → unique; nullable 이메일 힌트 추가 |
| [User](../../src/main/java/web/tosunsaeng/identity/domain/user/domain/entity/User.java) | FEDERATED email null, LOCAL 자격증명 불변조건 유지 |
| [FirebaseSdkAdminClient](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/infrastructure/firebase/FirebaseSdkAdminClient.java) / [FirebaseLinkedProviderData](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/infrastructure/firebase/FirebaseLinkedProviderData.java) | provider별 이메일 전달이 현재 없음; 검증된 출처 힌트 전달 추가 |
| [FirebaseAdminAuthenticationVerifier](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/infrastructure/firebase/FirebaseAdminAuthenticationVerifier.java) | primary/목적별 검증 유지; recovery verifier 별도 구성 |
| [FirebaseSignupService](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseSignupService.java) / [FirebaseGuestUpgradeService](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseGuestUpgradeService.java) | 복수 SNS 생성 경로 → 단일 정책 및 힌트 적용 |
| [ProviderChangeGuard](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderChangeGuard.java) / [ProviderLoginRegistrationService](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderLoginRegistrationService.java) | exchange 자동 등록 예외/저장 제거, 현재 SNS 소유권 검사 |
| [ProviderLinkController](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderLinkController.java) / [ProviderChangeController](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderChangeController.java) | 폐지 adapter와 제품 API 문서 정리 |
| [FirebaseAuthMethodsSyncService](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseAuthMethodsSyncService.java) | 현재 검증 전용 서비스 유지, single SNS 적용; 미사용 transaction 저장 메서드의 우회 경로 제거 |
| [FirebaseGuestMergeTargetResolver](../../src/main/java/web/tosunsaeng/identity/domain/auth/federation/application/FirebaseGuestMergeTargetResolver.java) | 전체 linked principal 순회 대신 현재 승인 SNS 기준으로 대상 검증 |
| [PhoneIdentityTransactionService](../../src/main/java/web/tosunsaeng/identity/domain/auth/phoneidentity/application/PhoneIdentityTransactionService.java) | 타 활성 owner 거절 유지 |
| [PhoneFingerprintHasher](../../src/main/java/web/tosunsaeng/identity/domain/auth/phoneidentity/domain/PhoneFingerprintHasher.java) / [alias 조회](../../src/main/java/web/tosunsaeng/identity/domain/auth/phoneidentity/repository/PhoneFingerprintAliasRepositoryCustom.java) | HMAC/keyVersion retained 조회 재사용 |
| [UserWithdrawalIdentityReleaseTransactionService](../../src/main/java/web/tosunsaeng/identity/domain/user/application/UserWithdrawalIdentityReleaseTransactionService.java) | SocialIdentity 삭제와 힌트 제거/즉시 조회 제외 검증 |
| [SecurityConfig](../../src/main/java/web/tosunsaeng/identity/global/config/SecurityConfig.java) | 새 공개 경로만 명시 허용, 다른 Guest/MEMBER 인가 범위 유지 |
| [ProviderRequestBudget](../../src/main/java/web/tosunsaeng/identity/domain/auth/providerchange/ProviderRequestBudget.java) | 기존 프로세스 내부 제한을 recovery의 분산 제한 보장으로 오인하지 않음 |
| [테스트 화면](../../tools/auth-test/README.md) | SNS 추가 UI 제거 및 recovery API/설정/상태 표시 추가 |

신규 패키지 제안: `domain.auth.accountrecovery` 아래 API/DTO, application, domain, repository, infrastructure 계층. `AccountRecoveryPhoneVerifier`, `AccountRecoveryAttempt`, proof 소비 기록, rate limit counter, `EmailHintMasker`, `SingleSocialIdentityPolicy`를 역할에 따라 배치한다. 파일/클래스명은 구현 시 기존 규칙에 맞추되 위 외부 계약의 의미를 유지한다.

### B. 검증 매트릭스

| 영역 | 필수 사례 |
| --- | --- |
| 단일 정책 | Google/Apple/Kakao 각 1개 성공; 2개 이상 거절; 전화 provider 제외; 비활성 provider 필터 우회 방지; 같은 userId 동시 insert unique 충돌 |
| exchange | 기존 승인 SNS 성공; 같은 UID의 새 provider 및 같은 provider의 다른 subject 거절; 자동 저장 없음; block/floor/epoch/탈퇴 검증 유지 |
| 가입·승격 | 전화 중복 기존 오류; User/Phone/Social/Session 원자 롤백; Guest userId 보존; 동의/프로필/enrollment 만료·재시도 회귀 |
| merge | 현재 승인 SNS만 대상 증명; PHONE/PASSWORD로 SNS 대상 인증 우회 불가; 다른 linked SNS로 대상 오선택 방지; source 경합/target 탈퇴·정지/응답 유실 기존 계약 유지 |
| API 폐지 | 모든 link/unlink mutation/complete 호출이 폐지 응답; DB/원격 SDK 부작용 없음; Guest prepare는 정상 유지 |
| 이메일 | provider subject 일치, 타 provider/루트 이메일 혼입 방지, null/짧은 주소/형식 오류/relay/Unicode, 원문 로그 비노출, 갱신 시각 경합, 탈퇴 제거 |
| 전화 proof | 서명/프로젝트/tenant/aud/exp/revoke/disabled 오류; linked phone만 있는 SNS token 거절; phone claim/Admin 불일치; auth_time 과거/미래 경계; force refresh 우회 불가 |
| challenge | 만료 TTL 지연, 같은 proof 동시 소비, 다른 challenge 재사용, 다른 proof로 재시도, 응답 유실 동일 proof 재조회, 재시도 중 탈퇴/번호 해제 |
| 번호 조회 | retained key 여러 alias의 동일 owner 합치기, 활성 owner 없음, 다중 owner 무결성 오류, 정지/탈퇴/정리 중/LOCAL 분기, 원문 번호 무저장 |
| abuse/장애 | 다중 인스턴스 제한, Retry-After, 제한 저장소 장애, 인증 전 조회 없음, SDK SMS 발송 제한 별도 확인, PII 없는 metrics |
| 계약 | BaseResponse/OpenAPI nullable/status 일치, no-store, JWT 없는 recovery 허용·다른 API 인가 유지, recovery 증명 일반 로그인 불가 |
| 프론트 통합 | OTP 취소/오류, 이메일 없음, Apple relay, 429/503, 앱 재시작, Guest 토큰 보존, 실제 SNS 로그인 후만 MEMBER 교체 |

주요 기존 테스트 수정 대상은 `FirebaseSignupServiceTests`, `FirebaseGuestUpgradeServiceTests`, `FirebaseExchangeServiceTests`, `FirebaseGuestMergeTargetResolverTests`, `FirebaseAuthMethodsSyncServiceTests`, `ProviderLoginRegistrationServiceTests`, `ProviderLinkHttpTests`, `ProviderChangeHttpTests`, `FirebaseSdkAdminClientTests`, `FirebaseAdminAuthenticationVerifierTests`, `SocialIdentityRepositoryIntegrationTests`, `SecurityIntegrationTests`다. 신규 recovery/마스킹/경합 테스트를 추가한다. 기존 다중 SNS 허용 테스트는 새 정책 거절 테스트로 교체하며 보안 경합 검증은 제거하지 않는다.

### C. 배포·문서 갱신

- [프론트 Firebase 연동 가이드](frontend-firebase-auth-integration-guide.md), [provider 연결 운영 계약](provider-link-recovery-operations.md), [unlink runbook](firebase-provider-unlink-stage-10-runbook.md)에 폐지/대체 및 계정 찾기 규칙을 동기화한다. 과거 계획서는 역사로 보존하고 현재 계약 우선 링크를 붙인다.
- [Identity–Learning JWT 계약](identity-learning-jwt.md)은 변경하지 않는다. 이메일/전화/힌트를 JWT나 Learning Core/Billing 이벤트에 추가하지 않는다. Guest merge outbox의 소비 방식도 유지한다.
- 배포 전 활성 회원/복수 SocialIdentity/진행 link·unlink/원격 Firebase 상태와 기존 인덱스를 read-only 점검한다. 예상과 다른 데이터가 있으면 무단 정리 없이 보고한다.
- 기존 다중 쓰기 서버 인스턴스를 종료한 상태에서 새 unique 인덱스와 단일 정책 코드의 호환성을 확인한다. 테스트 환경에서는 인증 쓰기를 잠시 중지하고 인덱스·서버·앱을 순서대로 적용하는 절차를 사용한다.
- 기존 Firebase 프로젝트의 Phone/앱 검증/SMS 제한과 기존 Admin 설정, 공유 제한 저장소를 확인한 후 서버와 앱 기능을 함께 활성화한다. 기능 flag는 배포 제어용이며 계정 찾기를 별도 출시로 빼는 수단이 아니다.
- 롤백 시 recovery를 OFF로 두되 단일 정책/unique 인덱스를 제거하거나 이전 다중 등록 코드를 재가동하지 않는다. 수정 전 코드로 단순 롤백하는 절차는 호환성 검토가 필요하다.
- 이 계획서 작성에서는 실제 인덱스 변경, Firebase 프로젝트 생성, 계정 삭제, commit/push, Jira 변경, 배포를 수행하지 않았다.

### D. 이번 계획서 작성 검증

- 위 표의 코드·계약을 정적으로 확인하고 로컬 문서 링크 및 `git diff --check`를 검사한다.
- 제품 코드 변경이 없는 문서 작업이므로 Gradle/외부 인프라 테스트는 실행하지 않는다. 구현 완료/배포 성공을 의미하지 않는다.
- Jira 댓글 초안(미등록): 단일 SNS 제한·전화 인증 계정 찾기·마스킹 이메일을 동일 출시 범위로 계획. 현재 SNS 이메일 미저장과 exchange 자동 추가 경로 확인. 전용 전화 verifier/힌트 저장/unique 인덱스/Guest 회귀·API 폐지·프론트 연동 포함. 실제 환경 검증 및 구현은 다음 작업.

### E. Jira 승인·등록 본문 — 2026-10-03

- 생성 대상: TMI 프로젝트, 이슈 유형 `작업`.
- 제목: `[Identity] 단일 SNS 계정 정책 및 SMS 인증 기반 계정 찾기 구현`
- 상위 에픽: `TMI-136 — sns 로그인` (생성 후 부모 필드 확인).
- 담당자/기한/우선순위: 별도 지정 없이 프로젝트 기본값 사용.
- 상태: 생성 기본 상태 사용, 기존 이슈 상태 변경·댓글 등록 없음.
- 생성 결과: 사용자 승인 후 `TMI-192` 생성. 재조회하여 제목·부모·상태와 아래 본문 일치 확인. Rovo 검색 권한 오류로 기존 중복 이슈 여부는 미확인. 추가 댓글/상태 전환 없음.

등록할 설명 본문:

> 목표와 배경
>
> 회원당 SNS 계정 하나 및 전화번호당 활성 회원 하나 정책을 적용하고, 기존 Firebase SMS 인증 후 가입 SNS와 마스킹 이메일을 안내한다. 단일 SNS 제한과 계정 찾기를 하나의 개발·출시 범위로 완료한다. 현재 exchange 자동 SNS 추가와 가입·Guest 승격의 복수 SNS 저장 경로가 있고, SNS 이메일 힌트 저장 기능은 없다.
>
> 구현 범위
>
> 1. SNS 추가 연결/해제 공개 흐름 폐지 및 exchange 자동 추가 등록 제거. 가입·승격·기존 회원 로그인에 단일 SNS 정책을 적용하고 SocialIdentity.userId unique 제약을 추가한다. 기존 보안 block/floor/세션 검증을 유지한다.
> 2. Guest prepare/upgrade/merge와 기존 LOCAL 인증 유지. Guest 병합 대상은 현재 인증한 승인 SNS로 검증한다. 계정 찾기는 Guest 없이 사용 가능한 독립 조회 기능이다.
> 3. 기존 Firebase 프로젝트·SMS 설정·Admin 연결을 재사용한다. 새 Firebase 프로젝트나 앱의 전화번호 회원 로그인 기능을 추가하지 않는다.
> 4. POST /api/v1/auth/account-recovery/prepare 및 /lookup 구현. 최근 전화 인증을 검증하고 인증된 번호의 활성 owner를 조회한다. 요청의 임의 전화번호/userId로 계정을 선택하지 않는다. challenge 만료·원자 소비·응답 유실 재시도·분산 요청 제한을 포함한다.
> 5. Firebase의 해당 SNS 프로필에서 이메일 힌트를 수집해 마스킹 상태로 SocialIdentity에 저장한다. 이메일 미제공은 SNS만 안내하고 Apple relay는 비공개 사용을 안내한다. 이메일 원문/번호/내부 식별자/회원 인증 자격증명은 조회 응답에 넣지 않는다.
> 6. OpenAPI·오류 응답·프론트 연동 문서·로컬 테스트 화면·설정/인덱스 배포 절차 갱신. 실제 앱 화면 및 SDK 연동과 통합 QA를 함께 조율한다.
>
> 완료 조건
>
> - 가입·승격·로그인 및 동시 요청으로 두 번째 SNS가 저장되지 않는다.
> - 기존 SNS 로그인, Guest 승격·병합, 전화번호 중복 방지와 LOCAL 인증 회귀가 통과한다.
> - 전화 인증 전에는 계정 정보가 노출되지 않고, 인증 후 올바른 SNS와 마스킹 힌트 또는 미제공 안내를 반환한다.
> - 계정 찾기 성공만으로 회원 세션 발급·SNS 연결·Guest 병합이 실행되지 않는다.
> - 인증 만료/폐기, challenge 중복·만료, 응답 유실, 이메일 미제공, Apple relay, 요청 제한, 조회 중 탈퇴/정리 상태를 테스트한다.
> - 같은 Firebase 프로젝트에서 기존 전화 소유 UID와 신규 phone-only UID 및 후속 가입이 충돌하지 않음을 통합 검증한다.
> - 변경한 로직 테스트와 ./gradlew clean test 및 테스트 도구 검증이 통과한다. 자동 테스트는 실제 Atlas/외부 Provider를 호출하지 않는다.
> - 앱 연동 계약 및 staging 통합 QA, 실제 인덱스·진행 연결 작업 사전 점검을 완료한다. 사용자 진술상 테스트 회원은 없지만 실제 데이터는 배포 전에 확인한다.
>
> 제외 범위
>
> 새 Firebase 프로젝트, 전화번호만으로 회원 로그인, SNS 교체/접근 복구, MEMBER 간 병합, Learning Core 기능 변경, 무조건적인 Firebase 계정 삭제는 포함하지 않는다.
>
> 구현 근거
>
> identity 저장소 docs/contracts/single-sns-account-recovery-implementation-plan.md. 세부 필드·오류·TTL·재시도·보안 및 전체 테스트 매트릭스는 해당 계획서에 따른다. 작성 시점에는 구현/배포를 수행하지 않았다.
