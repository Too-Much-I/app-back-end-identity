# Stage 11: Guest 생성 응답 유실 복구 구현 계획

- 작성일: 2026-09-16
- 상태: 미구현 취소 / 이력 보존 (2026-09-16 신규 Guest 종료·구버전 업데이트 정책)
- 기준 브랜치: `develop`
- Jira: TMI-133 — 2026-09-16 사용자 직접 삭제 보고. 별도 원격 재확인 없음.
- 승인: 4번 A, 나머지는 권장안. 아래 신규 이름·필드·설정은 구현 대상 계약이며 현재 제공되는 API라고 해석하지 않는다.

이하 내용은 취소된 설계의 이력이며 현재 구현 요구사항이 아니다. 기존 Guest의 SNS 전환은 사용자 설명상 구현돼 있고, 서버 신규 SNS 가입은 MEMBER를 직접 생성하여 Guest 생성에 의존하지 않는다.

## 1. 5줄 결론

1. 기존 `POST /api/v1/auth/guest`에 요청 ID와 앱이 미리 만든 별도 복구 증명을 추가하여, 응답이 유실돼도 같은 Guest·최초 Token 응답을 최대 2분간 복구한다. [현재 생성 코드](../../src/main/java/web/tosunsaeng/identity/domain/auth/registration/application/GuestAuthService.java)
2. User·최초 RefreshSession·최소 요청 기록·암호화 응답을 하나의 Mongo Transaction으로 저장한다. [현재 Transaction](../../src/main/java/web/tosunsaeng/identity/domain/auth/registration/application/GuestRegistrationTransactionService.java)
3. 2분이 지나면 전용 만료 오류를 반환하고 앱은 SNS 로그인/가입으로 이동한다. 새 Guest나 replacement Session을 자동 발급하지 않는다. [승인 기록](../codex/CURRENT_STATE.md)
4. 복구할 때마다 현재 ACTIVE GUEST와 세션 상태를 재검증하고 실제 쓰기 충돌로 로그아웃·승격·병합·탈퇴와 순서를 정한다. [현재 세션 보호](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/SessionSecurityService.java)
5. Stage 9 keyring을 재사용하되 Guest용 암호문·AAD를 분리한다. 신규 헤더를 지원하는 앱부터 적용하고 기능은 기본 OFF다. [Stage 9 계획](refresh-token-response-recovery-stage-9-plan.md)

## 2. 사용자가 반드시 읽어야 하는 내용

### 2.1 사용자에게 달라지는 점

현재는 서버가 Guest와 Session을 저장한 뒤 응답만 유실되면 앱에 Token이 없다. 같은 설치 ID로 다시 요청하면 `GUEST_ALREADY_EXISTS`가 되어 접근할 수 없다.

구현 후 신규 앱은 요청 전에 요청 ID·복구 증명·요청 본문을 안전하게 저장한다. 통신이 끊기면 같은 값을 다시 보내고, 서버는 최대 2분 안에서 처음 발급한 Token을 그대로 돌려준다. 이미 결과를 받았는지는 서버가 알 수 없으므로, 재전달을 한 번만 허용하지 않고 기한 내 동일 요청을 멱등 처리한다.

기간이 지나거나 복구 증명을 잃으면 SNS 로그인/회원 흐름으로 안내한다. SNS MEMBER 로그인 자체는 Guest 복구 기능과 독립적이다. SNS 로그인만으로 접근 불가능한 Guest가 자동 병합되거나 시험 기록이 이전되지는 않는다.

### 2.2 범위

- 포함: Guest 최초 응답의 단기 복구, 암호화 저장, 원자성, 동시성, 앱 재시도 계약, 구버전 호환, 운영·테스트 문서.
- 제외: 장기 Guest 로그인 credential, 새 Guest 자동 재생성, 기간 이후 세션 교체, orphan Guest 자동 삭제, 자동 SNS 병합, 기존 ACTIVE 회원 Firebase rebind.
- 신규 worker는 필요하지 않다. 요청 Transaction과 Mongo TTL로 처리한다.
- Learning Core·Billing의 시험·혜택·데이터 소유권을 변경하지 않는다.
- 기존 앱에는 두 신규 값을 요구하지 않는다. 복구 기능이 없는 구버전의 응답 유실 문제는 그대로 남는다.

### 2.3 승인한 암호화 보관의 의미

RefreshSession에는 계속 Refresh Token hash만 저장한다. 별도 복구 문서에는 사용자 승인에 따라 응답 전체를 복호화 가능한 암호문으로 잠시 저장한다. 이것은 “모든 저장소에 hash만 보관한다”는 의미가 아니며 평문 DB 저장은 금지한다.

2분은 API 복구 허용 기한이다. TTL 삭제 지연이나 백업 암호문을 그 시점에 물리적으로 지운다는 뜻은 아니다. keyring 공급·회전 구조는 이미 있지만 실제 AWS 공급·운영 검증 완료를 뜻하지 않는다.

## 3. 사용자가 결정해야 하는 사항

추가 제품 선택 없이 아래 승인안을 구현 기준으로 사용한다. 구현 중 범위 변경이 필요한 문제가 발견되면 해당 차이만 다시 제시한다.

| 항목 | 확정 기준 |
| --- | --- |
| 복구 증명 | 앱이 요청 전 생성한 고엔트로피 secret과 Idempotency-Key. installationId 단독 복구 금지 |
| 저장 방식 | 최초 응답을 암호화해 Guest/User/Session과 같은 Transaction에 저장 |
| 복구 기간 | 최대 `PT2M`, 요청 재시도로 연장하지 않음 |
| 4번 A | 만료 오류 후 SNS 흐름, 자동 새 Guest·replacement Session 없음 |
| 적용 방식 | 두 신규 값을 모두 제공하는 앱부터 단계 적용 |
| 요청 일치 | 정규화한 전체 요청 내용 hash 검사, 동의 변경도 충돌 |
| 7번 A | 매 복구 Transaction에서 현재 User·Session·epoch·계정 변경 검증 |
| 8번 A | Stage 9 keyring 재사용, Guest 전용 collection/schema·purpose/AAD |

헤더명, 오류 코드명, 아래 최소 기록의 배치 등은 이 계획에서 정하는 구현 설계다. Jira 작성 시 같은 계약을 사용한다. 배포 환경별 secret mount 경로와 실제 키 등은 운영 단계에서 주입하며 문서에 기록하지 않는다.

## 4. 주요 위험과 미확인 사항

1. **snapshot 조회만으로는 경합을 막지 못한다.** 복구와 계정 변경이 같은 `UserSessionControl` 및 최초 Session을 쓰도록 해야 한다. Mock 테스트만으로 Mongo의 실제 충돌을 입증하지 않는다.
2. **Guest 승격은 현재 `guestInstallationIdHash`를 비운다.** 복구 문서도 TTL로 사라진 뒤 과거 요청을 신규 생성으로 오인하지 않도록 최소 요청 기록을 User에 별도로 유지한다. 이를 기존 승격·병합·탈퇴 update가 보존하는지 검증한다.
3. **유효기간이 늘어났다는 앱 오해를 막아야 한다.** 본문의 최초 `expiresIn`은 그대로 재전달하고 실제 절대 만료 헤더를 신규 앱이 사용한다.
4. **Mongo commit 결과 불명과 rollback은 다르다.** 결과 불명 이후 같은 서버 처리에서는 신규 발급 경로를 다시 실행하지 않는다. 후속 HTTP 요청도 같은 식별자로 조회하며 unique 제약이 두 번째 성공 생성을 막아야 한다.
5. **receipt가 지워진 뒤의 판단**은 User의 최소 기록을 사용한다. 최소 기록까지 임의 TTL 삭제하지 않는다. User 자체의 물리 삭제·보존 정책 변경은 별도 검토 대상이다.
6. **암호화 목적 분리는 실제 키 유출 영향을 분리하지 않는다.** nonce 충돌 위험·키별 암호화량·교체·백업 보존은 Stage 9 운영 검증에 포함한다.
7. **전체 로그아웃이 이미 발급된 Access Token을 모든 서비스에서 즉시 차단하는 것은 아니다.** 이 계획의 epoch 검사는 복구·Refresh 권한을 보호한다. downstream 탈퇴 차단은 Stage 5 계약을 따른다.
8. 기존 앱·신규 앱 혼재, 앱 강제 종료, 실제 replica set rollback/commit 불명, secret mount와 재배포 검증은 아직 하지 않았다.

## 5. 현재 작업과 직접 관련된 설명

### 5.1 요청 계약과 단계 적용

URL과 Body는 현재 `POST /api/v1/auth/guest`, `GuestAuthRequest`를 유지한다. 신규 Request Body `userId`는 추가하지 않는다.

| 신규 요청 헤더 | 형식·규칙 |
| --- | --- |
| `Idempotency-Key` | 논리 요청별 canonical lowercase UUID v4. 재시도·앱 재시작에도 동일 값 |
| `Guest-Recovery-Proof` | 앱 CSPRNG의 32-byte 난수를 Base64URL 무패딩으로 인코딩한 43자. decode 후 길이 및 canonical 재인코딩 검사 |

각 헤더는 정확히 하나여야 한다. 중복 헤더, 쉼표 결합, 공백 보정에 의존하는 값, 길이 초과를 거절한다. proof는 서버가 최초 응답에서 만들어 주지 않는다. 앱은 요청 전 secure storage에 저장하고 서버는 원문 대신 목적을 구분한 SHA-256 digest만 저장·비교한다. 고엔트로피 난수이므로 비밀번호 hashing과 구분한다. proof digest 비교는 constant-time 비교를 사용한다.

| 입력·서버 상태 | 동작 |
| --- | --- |
| 두 헤더 모두 없음 | 기존 Guest 생성 경로. 기존 오류·동의 정책 유지 |
| 하나만 있거나 형식 오류 | `400 INVALID_GUEST_RECOVERY_REQUEST`, 생성 없음 |
| 둘 다 유효, 기능 ON | 신규 생성 또는 동일 요청 복구 |
| 둘 다 유효, 기능 OFF 또는 maintenance | `503 GUEST_RECOVERY_UNAVAILABLE`, legacy로 묵시 전환 금지 |
| 헤더 없는 요청이 이미 복구 모드로 생성된 설치 ID 사용 | 최소 기록까지 조회하여 `409 GUEST_ALREADY_EXISTS`. 복구 증명 없는 Token 반환·새 Guest 생성 금지 |

기능 OFF여도 이미 생성된 최소 기록의 중복 방지는 유지한다. 기능을 켤 때는 신규 앱 배포 이전에 모든 서버가 이 요청 분기와 최소 기록을 이해해야 한다.

### 5.2 정규화와 충돌

서버는 아래 값을 field boundary가 보존되는 고정 순서 형식으로 직렬화해 SHA-256 hash를 만든다. 임의 Map 순서나 원본 JSON 바이트 순서에 의존하지 않는다.

- schema version, canonical 설치 UUID
- `isPrivacyConsented`, trim된 `privacyConsentVersion`
- `isTermConsented`, trim된 `termConsentVersion`
- `isQualityReviewConsented`의 누락/null은 현재 DTO처럼 false
- trim된 `qualityReviewConsentVersion`의 null과 실제 값은 구분. 빈 문자열·부정확한 형식은 기존 검증대로 거절

JSON field 순서와 설치 UUID 대소문자 차이는 같은 의미로 처리한다. 동의 값·버전이 실제로 바뀌면 같은 요청으로 취급하지 않는다. 미동의라도 품질 검토 버전을 명시했다면 그 값을 hash에 포함한다.

신규 생성은 현재 `ConsentPolicy`를 검사한다. 기존 요청 복구는 처음 승인·저장한 본문 hash와 일치하는지 검증하며 동의를 새로 저장하지 않는다. 짧은 복구 기간 중 서버의 현재 정책 버전이 바뀌었다고 최초 승인된 응답 복구를 신규 가입으로 다시 처리하지 않는다.

### 5.3 데이터 구조와 보존

설계상 두 종류를 분리한다.

**A. User의 내장 `guestCreationRecovery` 최소 기록 (신규)**

| 필드 | 의미 |
| --- | --- |
| `schemaVersion` | 최초 `1` |
| `requestKeyHash`, `proofHash`, `payloadHash` | 요청·증명·정규화 본문 digest. 원문 없음 |
| `installationIdHash` | 기존 알고리즘으로 만든 설치 hash |
| `responseId`, `initialSessionId`, `sessionEpoch` | exact 최초 결과 연결 |
| `createdAt`, `recoveryUntil` | 서버 논리 생성 시각과 연장 불가 기한 |
| `accessExpiresAt`, `refreshExpiresAt` | 실제 Token 절대 만료 |

User의 실제 `userId`가 owner다. 이 필드는 최초 생성 후 불변으로 두며 신규 복구 모드 User에만 존재한다. User가 유지되는 동안 기록도 유지하고, 승격 때 기존 `guestInstallationIdHash`를 비워도 이 기록은 제거하지 않는다. MERGED/WITHDRAWN 상태에서도 보존해 과거 요청을 재실행하지 않는다. 새로운 TTL을 User에 걸거나 인증정보 원문을 장기 보관하지 않는다.

User의 `guestCreationRecovery.requestKeyHash`와 `guestCreationRecovery.installationIdHash`에 각각 string 존재 조건의 partial unique index를 둔다. 기존 User의 설치 hash unique index도 유지한다. 신규·legacy 생성의 최초 경쟁은 기존 설치 index가, 승격 이후 과거 복구 요청은 내장 기록의 unique index와 조회가 보호한다. 별도 recovery 조회 repository 메서드를 추가한다.

기존 User에는 이 기록을 backfill하지 않는다. 응답이나 proof를 알 수 없으므로 과거 Guest를 복구 가능한 것으로 간주하지 않는다. hash도 가명 식별정보이므로 User 보존·접근 통제를 따른다. 추후 User hard delete 시 이 기록의 재실행 방지 역할을 함께 검토한다.

**B. `guest_creation_responses` 암호화 응답 collection (신규)**

| 필드 | 의미 |
| --- | --- |
| `_id` / `responseId`, `schemaVersion` | User 최소 기록과 exact 연결, 최초 schema `1` |
| `userId`, `initialSessionId` | 소유자·최초 Session 일치 검사 |
| `encryptionKeyId`, `nonce`, `ciphertext` | 키 ID·nonce·인증 tag 포함 암호문 |
| `accessExpiresAt`, `refreshExpiresAt`, `recoveryUntil` | 암호화 envelope 및 User 기록과 교차 검증 |
| `cleanupAt` | `recoveryUntil`과 동일. `expireAfterSeconds=0` TTL |

`initialSessionId`에도 unique index를 둔다. 암호문 envelope는 최초 `GuestAuthResponse`와 절대 만료·복구 기한을 포함한다. 응답 크기 상한은 16 KiB다. Session에는 원문 대신 기존 Token hash만 저장한다.

`recoveryUntil = min(createdAt + PT2M, accessExpiresAt, refreshExpiresAt)`다. 생성·저장 시각은 UTC millisecond 정밀도로 통일하고 JWT의 실제 `exp` 정밀도를 존중한다. 물리 commit 지연으로 남은 기간이 짧아질 수 있지만 commit 이후 기한을 재설정하지 않는다.

TTL 삭제 여부로 복구 권한을 판단하지 않는다. 기한 후 암호문이 남아도 거절한다. 기한 전에 암호문이 사라지면 정상 만료로 숨기거나 새 Token을 만들지 않고 `503`과 무결성 경보로 처리한다.

### 5.4 생성·재시도·복구 순서

1. 요청 형식·헤더를 검증하고 digest를 만든다. request hash와 설치 hash로 기존 최소 기록을 먼저 찾는다.
2. 기존 기록이 있으면 proof를 검증한 후 request/payload/설치 식별자를 비교한다. 충돌은 생성으로 전환하지 않는다.
3. 기존 기록이 없는 최초 요청만 현재 동의 정책·기존 Guest 중복을 확인한다. User·Access Token·최초 Session·응답을 준비한다.
4. 하나의 Mongo Transaction 안에서 User 최소 기록, Guest proof를 가진 최초 Session, 세션 control, 암호화 응답을 함께 저장한다. 응답 암호화·저장 실패는 모두 rollback한다.
5. commit 성공 후에만 성공 응답을 반환한다. 반환 직전 논리 복구 기한 및 두 Token 만료를 다시 검사한다.
6. 같은 요청은 아래 5.5 검증과 충돌 쓰기를 포함하는 새 Transaction으로 복구한다. Token 생성·서명 코드를 호출하지 않는다.

동시 최초 요청은 unique 제약의 승자 하나만 commit한다. 패자는 rollback 뒤 별도 Transaction에서 승자 기록을 조회해 동일 proof/payload이면 복구한다. 다른 요청·증명이면 충돌한다. 준비 과정에서 rollback된 임시 Token이 존재할 수 있으나 사용자에게 반환하거나 세션으로 남기지 않는다.

Mongo `TransientTransactionError`·optimistic conflict 등 확실히 rollback된 경합은 최대 3회로 제한한다. 재시도 대기는 Transaction 밖에서 10~50ms jitter를 사용한다. `UnknownTransactionCommitResult`는 원인 chain의 label까지 확인하고 이후 해당 서버 처리에서 `replayOnly`로 고정한다. 저장된 결과가 확인되지 않으면 `503`, 새 User·Session·응답 생성은 하지 않는다.

다음 HTTP 재시도도 동일 식별자로 primary 조회와 unique 제약을 거친다. 이전 HTTP 호출의 메모리 상태가 자동 유지된다고 가정하지 않는다. 결과 불명과 재요청 경쟁에서도 두 생성 Transaction이 모두 commit할 수 없음을 replica set 테스트로 입증한다. 증거를 확인할 수 없는 장애는 계속 `503`으로 처리하고 앱이 다른 설치 ID/요청 ID로 우회하지 않게 한다.

### 5.5 현재 상태 재검증과 경합

정상 replay Transaction은 다음을 모두 검사한다.

- 최소 기록의 exact user/request/proof/payload/설치 ID 연결
- `now < recoveryUntil`, `now < accessExpiresAt`, `now < refreshExpiresAt`
- User `ACTIVE` 및 현재 `UserAccountType.GUEST` (호환 getter 유지)
- 최초 Session owner·ID·epoch 및 `authentication.source=GUEST`
- Session 미만료·미폐기·미회전, 현재 control epoch와 일치
- 복호화 결과의 Refresh hash가 최초 Session hash와 일치
- JWT `sub=userId`, `account_type=GUEST`, `exp`와 저장 만료 정보 일치
- envelope, User 최소 기록, 응답 메타데이터의 절대 만료·복구 기한 일치

`SessionSecurityService.checkAndTouch`로 control을 실제 갱신하고, 최초 Session도 `touchForRecovery`와 version CAS 저장으로 변경한다. 단순 read-only Transaction이나 값이 바뀌지 않는 update를 경합 장치로 삼지 않는다. 경합 시 새 snapshot으로 재검증하며 기한을 연장하지 않는다.

| 겹치는 작업 | 순서·결과 |
| --- | --- |
| 최초 Session logout 또는 refresh rotation | 같은 Session의 version 쓰기 충돌. 해당 작업이 먼저 commit하면 replay 거절 |
| logout-all | 같은 control epoch/버전과 경합. 증가한 epoch로 복구 거절 |
| Guest 승격 | 기존 User 승격·Guest Session 폐기·control 경계와 경합. 먼저 승격됐으면 Guest 응답 복구 거절 |
| Guest merge | source User/control/Session 경계 검증. target Token으로 바꿔 복구하지 않음 |
| withdrawal | User 상태·control·폐기 세션 재검증. 탈퇴한 Guest의 응답 복구 금지 |
| replay 먼저 commit, 이후 lifecycle commit | replay 성공은 가능하나 뒤 작업의 기존 세션 무효화가 적용됨. 이미 나간 Access Token의 보편적 즉시 차단을 주장하지 않음 |

구현 시 각 lifecycle writer가 같은 control/Session에 참여하는지 다시 점검하고 부족한 경계만 보완한다. 복구기능을 위해 별도 광범위 인증 구조 변경은 하지 않는다.

### 5.6 암호화·키 공급

- AES-256-GCM, 암호화마다 CSPRNG 96-bit nonce, 128-bit tag. nonce를 요청 ID나 시각에서 만들지 않는다.
- AAD는 길이 경계가 있는 정규 형식으로 `guest-creation-recovery`, 환경, schema, key ID, response/user/Session ID, epoch, 요청·설치·proof·payload digest, 논리 생성·복구·절대 만료를 결합한다. 서버가 검증한 저장 snapshot만 사용한다.
- Stage 9 암호문과 Guest 암호문, 다른 User·Session·환경 간 치환을 거절한다. 전용 Guest cipher가 Stage 9의 고정 `identity/reissue` AAD를 그대로 사용해서는 안 된다.
- keyring 공급 구현과 설정은 재사용한다. 기존 key provider bean이 Stage 9 ON에만 묶여 있으므로 공급 bean 조건을 두 복구 기능 중 하나라도 ON인 경우로 분리한다. Stage 11 사용 때문에 Stage 9 발급 동작까지 자동 활성화하지 않는다.
- 신규 응답은 active key 하나로 암호화하고 저장 key ID로 복호화한다. keyring은 최대 8개, 읽기 전용 mount, startup 로드라는 기존 구현을 유지한다. 자동 reload·AWS 리소스 생성은 범위 밖이다.
- 신·구 키를 모든 instance에 공급 → active 전환 → 구 키를 사용하는 instance·진행 요청·양 복구 기능의 최장 논리 기한 소진 확인 → 구 키 제거 순서다.
- 키 설정 오류는 기능 ON startup 실패. 복호화 오류·unknown key·tag 불일치는 `503`과 고정 사유 지표로 처리하며 새 Token/평문 fallback 없음.
- 실제 키·proof·Token·암호문·nonce·tag·요청 본문을 로그/Sentry/trace/예외/객체 출력에 남기지 않는다. secret store 연동 자체와 KMS envelope adapter 신설은 별도 작업이다.

### 5.7 응답·오류·모바일 계약

성공은 기존 `200 BaseResponse<GuestAuthResponse>`다. 최초와 replay의 `result`는 동일한 Token 및 동일한 값이다. JSON 공백이나 field 순서까지 바이트 단위로 같다는 의미는 아니다. 기존 다섯 필드는 유지한다.

| 성공 result 필드 | 계약 |
| --- | --- |
| `accessToken`, `refreshToken` | 최초 발급 값 그대로 |
| `grantType` | `Bearer` |
| `accessTokenExpiresIn`, `refreshTokenExpiresIn` | 최초 발급 때 계산한 밀리초 값. replay 시 다시 늘리지 않음 |

복구 모드 성공에는 아래 UTC Instant 헤더를 추가한다.

- `Guest-Access-Expires-At`
- `Guest-Refresh-Expires-At`
- `Guest-Recovery-Until`

신규 앱은 절대 만료 헤더를 기준으로 저장하며 응답 수신 시각에 본문 `expiresIn`을 더하지 않는다. 브라우저 클라이언트가 있으면 CORS allow/expose 설정도 점검한다. Guest endpoint의 성공·validation/security 오류 전체에 `Cache-Control: no-store`, `Pragma: no-cache`를 적용한다.

아래 오류들은 신규 계약이며 기존 `BaseResponse` envelope를 사용한다. `result`에는 credential이나 기존 owner 정보를 담지 않는다.

| HTTP / code | 조건 | 앱 동작 |
| --- | --- | --- |
| 400 `INVALID_GUEST_RECOVERY_REQUEST` | 누락 조합·헤더 형식 오류 | 요청 생성·저장 구현 점검, 자동 재시도 중지 |
| 400 기존 동의·요청 오류 | 최초 가입의 입력 오류 | 동의/입력 화면 유지 |
| 409 `GUEST_RECOVERY_REQUEST_CONFLICT` | 다른 proof 또는 request/payload/설치 연결 충돌 | 다른 Guest를 자동 생성하지 않음, SNS 흐름 안내 |
| 409 `GUEST_RECOVERY_EXPIRED` | 일치하는 최소 기록의 논리 기한 만료 | 복구 재시도 종료, SNS 로그인/가입 |
| 409 `GUEST_RECOVERY_SUPERSEDED` | proof가 맞지만 logout/rotation/승격/병합 등으로 결과 폐기 | 이미 보유한 최신 인증 상태 유지, 없으면 SNS 흐름 |
| 401 `ACCOUNT_WITHDRAWN` | 올바른 복구 증명으로 확인한 User가 탈퇴 상태 | 기존 탈퇴 UX 계약 적용 |
| 409 기존 `GUEST_ALREADY_EXISTS` | legacy 중복 또는 기존 Guest에 복구 기록 없음 | 기존 계약, 신규 앱은 SNS 흐름 |
| 503 `GUEST_RECOVERY_UNAVAILABLE` | OFF/maintenance, DB·키 장애, 결과 불명·경합 소진, 기한 내 receipt 손실 | 동일 요청·proof·본문으로 제한 재시도 |

proof가 틀리면 상태·만료·탈퇴 정보를 구분해 노출하지 않고 일반 충돌을 반환한다. 올바른 proof의 오류 우선순위는 탈퇴 → 논리 만료 → 다른 superseded 상태 → 응답 무결성 순서로 고정하고 테스트한다. 저장소 장애를 만료·충돌로 위장하지 않는다.

앱은 요청을 single-flight로 실행한다. 재시도 간격은 0.5초부터 지수 증가, 최대 5초에 jitter를 적용하고 로컬 최초 요청 이후 2분을 넘겨 무한 재시도하지 않는다. 서버 기한이 최종 판단 기준이다. 응답 성공 시 Token들을 안전하게 저장한 후 bootstrap proof/요청을 삭제한다. 만료/증명 유실 시 SNS로 전환하며 새 installationId로 우회하지 않는다.

늦은 Guest 응답이 이미 완료된 SNS 로그인이나 refresh 결과를 덮어쓰지 않도록 앱은 인증 흐름 식별자를 비교한다. SNS 전환이 서버의 orphan Guest를 자동 병합·삭제·무효화한다는 의미는 아니다. 앱 저장 원자성·강제 종료 시나리오는 모바일 인계 항목이다.

### 5.8 설정·구현 순서·배포

| 신규 설정 | 기본값/검증 |
| --- | --- |
| `AUTH_GUEST_RECOVERY_ENABLED` | `false` |
| `AUTH_GUEST_RECOVERY_MAINTENANCE` | `false`; ON이면 복구 모드 요청 `503` |
| `AUTH_GUEST_RECOVERY_WINDOW` | `PT2M`; 양수, millisecond 단위, 최대 PT2M |
| `AUTH_GUEST_RECOVERY_RESPONSE_MAX_BYTES` | `16384`; 범위 1024~16384 |
| `AUTH_GUEST_RECOVERY_CONCURRENCY_ATTEMPTS` | `3`; 범위 1~3 |

keyring은 기존 `AUTH_REISSUE_ENCRYPTION_ACTIVE_KEY_ID`, `AUTH_REISSUE_ENCRYPTION_KEYRING_LOCATION`, `AUTH_REISSUE_ENCRYPTION_ENVIRONMENT`를 공용 공급 설정으로 재사용한다. 설정명 호환은 유지하고 Guest에서도 사용됨을 운영 문서에 명시한다. 기존 Stage 9 암호문/AAD/schema를 변경하지 않는다.

Stage 11 ON은 `AUTH_SESSION_FENCE_ENABLED=true`와 유효한 keyring을 필수로 요구한다. SessionSecurityService 없는 복구는 startup 실패한다. Mongo Transaction은 primary/snapshot/majority, 최대 commit 5초, Transaction timeout 10초로 Stage 9와 정렬한다. 실제 경합 경계가 동일 DB/session에 참여하는지 검증하고 예외 포장으로 commit 불명 label이 유실되지 않게 한다.

구현 순서:

1. 헤더·정규화·오류·설정 계약 및 테스트.
2. User 최소 기록·repository 조회·partial unique index, 암호문 collection/TTL.
3. 공용 key provider 조건 분리와 Guest cipher, Stage 9 호환 테스트.
4. Guest 생성 Transaction 확장, replay·commit 결과 불명 분기.
5. lifecycle 쓰기 경합, no-store/redaction, legacy 전환·앱 절대 만료 계약.
6. 집중 테스트와 전체 `./gradlew clean test`, `git diff --check`, 프론트 가이드·runbook 갱신.

배포는 기능 OFF 코드 배포 → 모든 instance의 새 분기 지원 및 index 확인 → staging 모바일/replica set/키 검증 → 서버 ON → 신규 앱의 두 헤더 활성화 순서다. 구버전은 헤더 없는 기존 흐름을 유지한다. 서버 ON과 Stage 9 ON은 별개다.

중단은 우선 maintenance로 신규 복구 요청을 차단한다. 기존 기록을 지우거나 신규 요청을 legacy로 전환하지 않는다. 최소 기록을 모르는 구 바이너리로 즉시 rollback하면 승격 후 중복 방지가 깨질 수 있으므로 새 데이터 호환을 확인한다. 코드 개발·로컬 테스트 성공과 운영 활성화 완료를 구분한다.

## 6. 부록 — 상세 조사 근거와 전체 검증표

### 6.1 확인된 현재 구현

| 확인 사실 | 근거 |
| --- | --- |
| Guest 중복 검사 후 Access/Refresh 준비, DuplicateKey를 중복으로 재분류 | [GuestAuthService](../../src/main/java/web/tosunsaeng/identity/domain/auth/registration/application/GuestAuthService.java) |
| User·Session·응답 구성의 Transaction, 복구 receipt 없음 | [GuestRegistrationTransactionService](../../src/main/java/web/tosunsaeng/identity/domain/auth/registration/application/GuestRegistrationTransactionService.java) |
| 설치 ID trim·UUID v4, 필수/선택 동의 입력과 응답 다섯 필드 | [Request](../../src/main/java/web/tosunsaeng/identity/domain/auth/registration/dto/request/GuestAuthRequest.java), [Response](../../src/main/java/web/tosunsaeng/identity/domain/auth/registration/dto/response/GuestAuthResponse.java) |
| Guest 승격 시 설치 hash 비움 | [User](../../src/main/java/web/tosunsaeng/identity/domain/user/domain/entity/User.java) |
| 응답 복구 control 쓰기·child Session CAS·replayOnly 선례 | [ReissueRecoveryService](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/ReissueRecoveryService.java) |
| 공통 epoch/control 검사·쓰기 | [SessionSecurityService](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/SessionSecurityService.java) |
| Stage 9 key provider는 현재 Stage 9 enabled에 종속 | [ReissueRecoveryConfiguration](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/infrastructure/ReissueRecoveryConfiguration.java) |
| startup mount 로드와 AES-GCM AAD | [키 공급](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/infrastructure/MountedReissueEncryptionKeys.java), [Cipher](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/infrastructure/AesGcmReissueResponseCipher.java) |
| no-store filter의 현재 대상에는 Guest가 없음 | [ReissueNoStoreFilter](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/infrastructure/ReissueNoStoreFilter.java) |

계획의 신규 필드·헤더·오류·타임아웃·index·앱 계약은 아직 구현 사실이 아니다. 기존 테스트 통과 기록을 Stage 11 테스트 결과로 사용하지 않는다.

### 6.2 변경 후보

| 범위 | 변경 후보 |
| --- | --- |
| registration application | GuestAuthService, GuestRegistrationTransactionService, 신규 GuestCreationRecoveryService/Result·proof 및 요청 digest 구성요소 |
| User/domain/repository | 내장 GuestCreationRecoveryEvidence, User 생성·조회·index, 승격/merge/withdrawal 보존 테스트 |
| recovery 저장·암호화 | GuestCreationResponse·Repository, GuestResponseCipher와 AES-GCM 구현 |
| 설정 | GuestRecoveryConfiguration/Properties, 기존 key provider 공급 조건, application.yml/test 설정 |
| HTTP | AuthController Guest 헤더/만료 헤더, AuthErrorStatus, no-store/redaction/OpenAPI |
| 문서 | 본 계획, 구현 순서, frontend-firebase-auth-integration-guide, 신규 Stage 11 runbook, WORKLOG/CURRENT_STATE |

### 6.3 필수 테스트

| ID | 검증 |
| --- | --- |
| T01 | 첫 성공은 User·최초 Session·control·암호문·최소 기록이 함께 commit, JWT `account_type=GUEST` |
| T02 | 응답 유실·동일 요청 재전송은 동일 userId/Session/Token/result/기한. issuer 재호출 없음 |
| T03 | 동시 동일 생성의 성공 결과 하나, 다른 proof/request/payload 충돌, extra 세션 없음 |
| T04 | 암호화·저장·응답 구성 실패 시 전부 rollback, 외부 성공 응답 없음 |
| T05 | UnknownTransactionCommitResult 이후 replayOnly, 결과 확인 전 재발급 없음, HTTP 재시도 경합에서도 commit 하나 |
| T06 | PT2M 직전·정각·직후, commit 지연, TTL 지연/삭제 후 최소 기록의 동일 만료 판단 |
| T07 | 응답 암호문 조기 손실/키 부재/변조는 503, 새 Token fallback 없음 |
| T08 | logout·rotation·logout-all·upgrade·merge·withdrawal 각각과 replay의 양방향 commit 순서·실제 쓰기 충돌 |
| T09 | 승격으로 기존 설치 hash가 비워져도 최소 기록 유지, TTL 후 과거 Guest 재생성 없음 |
| T10 | header 없는 legacy 회귀, 하나만/중복/결합/과대/비정규 헤더 거절, flag OFF 요청의 묵시 legacy 전환 없음 |
| T11 | JSON 순서·설치 UUID 대소문자·선택 동의 누락 정규화, 필수/선택 동의·버전 변경 충돌 |
| T12 | 복구 기간 중 서버 동의 정책 버전 변경에도 최초 요청 hash 기준 복구, 동의 재저장 없음 |
| T13 | AAD 각 항목·다른 User/Session/환경/Stage 9 암호문 치환 거절, 신·구 키 복구 및 nonce 생성 |
| T14 | Stage 9 OFF/Stage 11 ON 및 역조합 key bean, 둘 다 OFF, fence/keyring invalid startup 실패 |
| T15 | BaseResponse·다섯 필드·절대 만료 헤더 일치, no-store가 성공/400/401/409/503 전체에 적용 |
| T16 | proof 틀릴 때 lifecycle 정보 비노출, 로그/Sentry/validation/toString credential 비노출 |
| T17 | 앱 강제 종료·요청 저장·응답 저장 중단·증명 유실·2분 만료 SNS 이동, 늦은 Guest 응답이 최신 로그인 미덮어쓰기 |
| T18 | Stage 9·회원 가입/로그인/승격/병합/탈퇴 기존 회귀 및 실제 Mongo partial unique·TTL·rollback 검증 |

단위 테스트는 외부 Provider/Repository mock, HTTP 테스트는 격리 환경을 사용한다. 실제 Atlas/OAuth를 기본 테스트에서 호출하지 않는다. replica set 경합·rollback/결과 불명 검증은 로컬 격리 또는 staging으로 분리하고 외부 인프라 없이는 통과했다고 기록하지 않는다.

### 6.4 완료 및 인계 기준

- 집중 테스트와 전체 `./gradlew clean test`, `git diff --check` 결과를 기록한다.
- commit 불명·동시 생성·lifecycle 순서·TTL 이후 중복 방지·Stage 9 key 호환 증빙을 남긴다.
- 신규 앱에 헤더 생성/보안 저장/동일 요청 재시도/절대 만료/오류별 SNS 전환 계약을 인계한다.
- 저카디널리티 `created/replayed/expired/conflict/superseded/unavailable` 집계와 `crypto_failure/transaction_unresolved` 경보를 준비한다. proof·요청 hash·Token을 metric label로 사용하지 않는다.
- runbook에 index 확인, replica set, keyring 배포·회전, 기능 ON/OFF·maintenance, 백업 보존, 구버전 rollback 제한을 기재한다.
- Jira 댓글 초안·PR 인계에는 변경·테스트·미확인 운영 항목만 포함한다. commit/push는 사용자 수행, Jira 쓰기는 별도 승인 절차를 따른다.
- 실제 모바일·Mongo·키 공급 검증 이전에는 운영 완료나 production 활성화로 표시하지 않는다.
