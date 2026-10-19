# 4단계 구현 계획: 탈퇴 Session 전용 오류와 모바일 logout·안내 UX

## 1. 목적

이 단계의 목적은 회원 탈퇴로 폐기된 RefreshSession을 일반적인 만료·로그아웃·잘못된 Refresh Token과 구분하고, 다른 기기를 포함한 모바일 클라이언트가 탈퇴 사실을 안전하고 일관되게 처리할 수 있는 서버·모바일 계약을 만드는 것이다.

현재 회원 탈퇴 Transaction은 해당 User의 활성 RefreshSession을 모두 `ACCOUNT_WITHDRAWN`으로 폐기한다. 그러나 재발급 API는 이 사유를 외부에 구분하지 않고 `INVALID_REFRESH_TOKEN`으로 반환하므로 클라이언트가 일반적인 인증 만료와 계정 탈퇴를 구분할 수 없다.

Stage 4는 Refresh Token 재발급 경계와 모바일 로컬 인증 상태 정리를 다룬다. 이미 발급된 stateless Access Token을 Learning Core에서 즉시 차단하는 작업은 Stage 5의 `UserWithdrawn` event와 downstream deny marker 범위다.

Jira: `TMI-108`

## 2. 선행 상태

Stage 1~3 완료 뒤 탈퇴 계정은 다음 상태다.

- User: `WITHDRAWN` tombstone
- 탈퇴 당시 활성 RefreshSession: `revokedAt` 존재, `revocationReason=ACCOUNT_WITHDRAWN`
- Firebase 외부 User: Stage 2에서 삭제 확인
- FirebaseIdentity·SocialIdentity: Stage 3에서 제거
- PhoneIdentity·PhoneFingerprintAlias: Stage 3에서 `RELEASED`
- withdrawal lifecycle: `CLEANED`
- 같은 credential 재가입: 새 UUID User로 가능
- 탈퇴 전에 발급된 Identity Access Token: 만료 전까지 downstream에서 서명상 유효할 수 있음

Stage 4는 User·identity cleanup 상태를 다시 변경하지 않고, 남아 있는 클라이언트 Session이 탈퇴 사실을 인식하는 방법만 추가한다.

## 3. 현재 동작과 문제

현재 `TokenReissueService`의 오류 우선순위는 다음과 같다.

1. Refresh Token hash로 Session 조회
2. `ROTATED`면 `REFRESH_TOKEN_REUSE_DETECTED`
3. 그 밖의 revoked Session이면 `INVALID_REFRESH_TOKEN`
4. 만료 Session이면 `REFRESH_TOKEN_EXPIRED`
5. User가 ACTIVE가 아니면 `ACCOUNT_NOT_ACTIVE`
6. 나머지만 Rotation과 새 Token 발급

따라서 `ACCOUNT_WITHDRAWN` Session도 다음과 같이 일반 오류로 보인다.

```text
다른 기기 Refresh Token 재발급
→ Session 조회 성공
→ revocationReason=ACCOUNT_WITHDRAWN
→ INVALID_REFRESH_TOKEN 401
→ 앱은 탈퇴·로그아웃·세션 만료를 구분하지 못함
```

이 상태에서는 다음 문제가 생긴다.

- 다른 기기에 “탈퇴 처리된 계정입니다”를 정확히 안내할 수 없다.
- Firebase client session과 Identity Token을 함께 정리해야 하는지 판단하기 어렵다.
- 일반 Session 만료 UX와 계정 전체 탈퇴 UX가 동일하게 처리된다.
- 여러 동시 API 요청이 같은 오류를 받으면 중복 안내·화면 전환·재발급 반복이 발생할 수 있다.
- 탈퇴 Transaction과 재발급 요청이 교차하면 active Session snapshot과 WITHDRAWN User가 함께 관찰될 수 있다.

## 4. 포함 범위

- `AuthErrorStatus`에 탈퇴 Session 전용 외부 오류 추가
- `POST /api/v1/auth/reissue`의 revocation reason 분류
- active Session과 WITHDRAWN User가 교차 관찰되는 race-safe fallback
- 기존 `BaseResponse` 401 오류 응답 유지
- 기존 Refresh Token rotation·reuse detection·만료·일반 폐기 오류의 회귀 유지
- OpenAPI 401 설명과 오류 예시 보완
- 서버 application·controller·withdrawal lifecycle·로그 보안 테스트
- 모바일이 탈퇴 API 성공 또는 전용 오류를 받은 뒤 수행할 로컬 정리 계약
- 여러 동시 401의 단일 terminal auth transition
- 앱 재시작·네트워크 실패·Firebase signOut 실패 처리 계약
- 모바일 선배포와 서버 활성화 순서
- Identity·모바일 staging E2E 절차

## 5. 제외 범위

- `UserWithdrawn` outbox·publisher
- Learning Core의 event inbox·deny marker·Access Token 즉시 차단
- Access Token blacklist 또는 매 요청 Identity introspection
- Firebase Admin User disable·revoke·delete 재실행
- logout-all의 Firebase refresh revoke
- Refresh Token rotation 자체의 Transaction·응답 유실 복구
- Firebase Provider unlink·전화번호 변경
- 가입 중단 Firebase User cleanup
- User tombstone·identity cleanup 정책 변경
- 모바일 소스 코드의 Identity 저장소 직접 구현
- 새로운 공개 logout·withdrawal 조회 API

## 6. 핵심 결정

### 6.1 외부 오류는 `401 ACCOUNT_WITHDRAWN`으로 고정한다

권장 오류 계약은 다음과 같다.

| 항목 | 값 |
| --- | --- |
| HTTP status | `401 Unauthorized` |
| code | `ACCOUNT_WITHDRAWN` |
| message | `탈퇴 처리된 계정입니다.` |
| result | `null` |

Refresh Token은 계정 탈퇴로 더 이상 인증 credential로 사용할 수 없으므로 401을 사용한다. 403은 유효한 인증 주체에 대한 권한 부족 의미로 해석될 수 있어 재발급 credential 거절에는 사용하지 않는다.

오류 응답에는 userId, sessionId, withdrawalId, 폐기 시각, Firebase UID와 Provider 정보를 포함하지 않는다.

### 6.2 실제 Session을 찾은 경우에만 탈퇴 사실을 노출한다

다음 조건에서만 `ACCOUNT_WITHDRAWN`을 반환한다.

- Refresh Token hash와 일치하는 Session이 존재함
- 그 Session의 `revocationReason == ACCOUNT_WITHDRAWN`

존재하지 않는 Token, 임의 문자열, 다른 사용자의 Token을 이용해 계정 탈퇴 여부를 조회할 수 없어야 한다. Session을 찾지 못한 경우는 계속 `INVALID_REFRESH_TOKEN`이다.

### 6.3 기존 reuse detection 우선순위를 유지한다

`ROTATED` Session 재사용은 credential 탈취 가능성을 나타내는 보안 사건이므로 기존 `REFRESH_TOKEN_REUSE_DETECTED` 경로를 유지한다.

Stage 1은 탈퇴 시점에 활성 상태인 Session을 `ACCOUNT_WITHDRAWN`으로 폐기한다. 이미 `ROTATED`·`LOGOUT` 등으로 폐기된 과거 Session의 reason을 탈퇴 사유로 덮어쓰지 않는다.

오류 우선순위는 다음과 같이 고정한다.

```text
Session 없음                    → INVALID_REFRESH_TOKEN
Session reason ROTATED          → REFRESH_TOKEN_REUSE_DETECTED
Session reason ACCOUNT_WITHDRAWN→ ACCOUNT_WITHDRAWN
그 밖의 revoked Session         → INVALID_REFRESH_TOKEN
active이지만 만료               → REFRESH_TOKEN_EXPIRED
active Session + WITHDRAWN User → ACCOUNT_WITHDRAWN
active Session + 기타 비활성 User→ 기존 ACCOUNT_NOT_ACTIVE
```

### 6.4 모바일 로컬 정리는 Firebase signOut 성공 여부와 분리한다

Firebase client signOut이 예외를 반환하거나 앱 프로세스가 중단되더라도 Identity Token과 로컬 계정 상태 삭제를 건너뛰면 안 된다.

모바일 구현은 다음 불변식을 만족해야 한다.

- terminal signed-out state는 멱등함
- Refresh 재시도 loop를 즉시 중단함
- Identity Access·Refresh Token을 영속 저장소에서 삭제함
- 로컬 사용자·권한·민감 cache를 삭제함
- Firebase client signOut을 시도함
- signOut 실패와 관계없이 로컬 삭제와 로그인 화면 이동을 완료함
- 동일 오류 여러 건에서 안내는 한 번만 표시함

### 6.5 Stage 4는 기존 Access Token을 즉시 차단하지 않는다

Stage 4 오류는 앱이 Refresh 재발급을 시도했을 때만 전달된다. 탈퇴 전에 발급된 Access Token을 가진 앱이 재발급 없이 Learning Core를 호출하면 Access Token 만료까지 접근이 가능할 수 있다.

이 공백은 Stage 5에서 다음 방식으로 처리한다.

- Identity `UserWithdrawn` outbox·publisher
- Learning Core eventId 멱등 consumer
- userId 기반 local deny marker
- JWKS 검증 뒤 local marker 확인

Stage 4 구현에 downstream introspection이나 blacklist를 섞지 않는다.

## 7. 서버 API 계약

### 7.1 대상 endpoint

기존 endpoint만 변경한다.

```text
POST /api/v1/auth/reissue
```

Request DTO와 성공 응답은 변경하지 않는다. 외부 Request Body에 userId나 withdrawalId를 추가하지 않는다.

### 7.2 실패 응답

```json
{
  "isSuccess": false,
  "code": "ACCOUNT_WITHDRAWN",
  "message": "탈퇴 처리된 계정입니다.",
  "result": null
}
```

응답은 기존 `GlobalExceptionHandler`와 `BaseResponse.failure` 경계를 사용한다. 별도 Controller 분기나 예외 응답 DTO를 추가하지 않는다.

### 7.3 오류 매트릭스

| Session/User 상태 | HTTP/code |
| --- | --- |
| hash와 일치하는 Session 없음 | `401 INVALID_REFRESH_TOKEN` |
| `ROTATED` | `401 REFRESH_TOKEN_REUSE_DETECTED` |
| `ACCOUNT_WITHDRAWN` | `401 ACCOUNT_WITHDRAWN` |
| `LOGOUT`, `LOGOUT_ALL`, `REUSE_DETECTED` | `401 INVALID_REFRESH_TOKEN` |
| `GUEST_UPGRADED`, `GUEST_MERGED` | `401 INVALID_REFRESH_TOKEN` |
| active이고 만료 | `401 REFRESH_TOKEN_EXPIRED` |
| active Session, User 없음 | `401 INVALID_REFRESH_TOKEN` |
| active Session, User `WITHDRAWN` | `401 ACCOUNT_WITHDRAWN` |
| active Session, User `SUSPENDED` 등 | 기존 `403 ACCOUNT_NOT_ACTIVE` |

`ACCOUNT_WITHDRAWN` Session은 만료 시각보다 폐기 사유를 먼저 판정한다. 탈퇴 Session이 TTL monitor 지연으로 DB에 남아 있는 동안에도 같은 전용 오류를 반환한다. TTL로 삭제된 뒤에는 Session을 찾을 수 없으므로 `INVALID_REFRESH_TOKEN`으로 돌아가는 것이 정상이다.

## 8. application 구현 계획

`TokenReissueService`에 작은 분류 경계를 둔다.

권장 구조는 다음과 같다.

```java
private void rejectRevokedSession(RefreshSession session, Instant now) {
    if (session.getRevocationReason() == RevocationReason.ROTATED) {
        // 기존 reuse detection과 활성 Session 폐기
    }
    if (session.getRevocationReason() == RevocationReason.ACCOUNT_WITHDRAWN) {
        throw new AuthException(AuthErrorStatus.ACCOUNT_WITHDRAWN);
    }
    if (session.isRevoked()) {
        throw new AuthException(AuthErrorStatus.INVALID_REFRESH_TOKEN);
    }
}
```

실제 구현에서는 기존 reuse side effect와 logging을 보존하고 중복 분기만 최소화한다.

User 조회 뒤 상태 분류도 다음처럼 분리한다.

```text
User 없음       → INVALID_REFRESH_TOKEN
WITHDRAWN       → ACCOUNT_WITHDRAWN
ACTIVE          → rotation 진행
그 밖의 상태    → ACCOUNT_NOT_ACTIVE
```

User가 WITHDRAWN인 active Session fallback은 탈퇴 Transaction과 재발급의 교차 관찰 또는 운영 불일치에서도 새 Token 발급을 fail-closed로 막는다. 이 경로는 Session을 임의로 수정하거나 withdrawal lifecycle을 생성하지 않는다.

## 9. 동시성 계약

### 9.1 탈퇴가 먼저 commit된 경우

```text
withdrawal Transaction commit
→ User WITHDRAWN + active Session ACCOUNT_WITHDRAWN
→ reissue가 Session 조회
→ ACCOUNT_WITHDRAWN
→ Token 발급 0건
```

### 9.2 reissue가 active Session을 먼저 읽은 경우

가능한 결과는 다음 중 하나로 안전하게 수렴해야 한다.

- User도 ACTIVE snapshot으로 읽고 Session rotation save가 먼저 성공하면 withdrawal Transaction이 version/write conflict로 실패해 탈퇴 성공 응답을 반환하지 않음
- withdrawal commit 뒤 User를 읽으면 `WITHDRAWN` fallback으로 전용 오류
- withdrawal commit 뒤 Session save를 시도하면 optimistic lock/write conflict로 Token 발급 차단

탈퇴 성공 응답과 새 RefreshSession 발급이 동시에 확정되는 상태를 허용하지 않는다. 실제 replica set에서 이 교차 순서를 staging concurrency test로 검증한다.

### 9.3 여러 기기의 동시 재발급

각 기기의 active RefreshSession은 Stage 1 Transaction에서 `ACCOUNT_WITHDRAWN`으로 폐기된다. 여러 기기가 동시에 재발급해도 모두 전용 오류로 수렴하고 새로운 Access·Refresh Token을 만들지 않는다.

## 10. 모바일 계약

### 10.1 트리거

모바일은 두 경우에 동일한 terminal withdrawal handler를 호출한다.

- 현재 기기에서 회원탈퇴 API가 2xx로 성공함
- 어느 기기에서든 재발급 API가 `401 ACCOUNT_WITHDRAWN`을 반환함

일반 `INVALID_REFRESH_TOKEN`, `REFRESH_TOKEN_EXPIRED`와 네트워크 오류는 탈퇴 안내를 표시하지 않는다.

### 10.2 처리 순서

권장 순서는 다음과 같다.

1. 전역 auth state를 `withdrawn/terminating`으로 원자 전환
2. 진행 중인 Refresh single-flight와 인증 API retry 취소
3. Firebase client signOut 시도
4. `finally` 또는 동등한 보장 경계에서 Identity Access Token·Refresh Token 삭제
5. 로컬 사용자 프로필·권한·계정별 cache 삭제
6. auth state를 signed-out terminal로 확정
7. navigation stack을 초기화하고 로그인/가입 화면으로 이동
8. “탈퇴 처리된 계정입니다” one-shot 안내

Firebase signOut 실패를 이유로 4~8단계를 건너뛰지 않는다. 모바일 SDK가 local-only signOut을 제공한다면 네트워크 재시도와 분리한다.

### 10.3 멱등성과 중복 이벤트

- 여러 API가 동시에 `ACCOUNT_WITHDRAWN`을 받아도 handler는 한 번만 실행한다.
- Token 저장소가 이미 비어 있어도 성공한다.
- 화면 전환과 안내는 중복되지 않는다.
- 앱 재시작 후 terminal cleanup marker가 남아 있으면 남은 local cache를 다시 정리하고 로그인 화면으로 시작한다.
- 삭제 완료 뒤 terminal marker는 정리하되 old Token을 복구하지 않는다.

### 10.4 사용자 안내

기본 문구는 서버 message에 의존하지 않고 모바일 localization resource로 관리한다.

```text
탈퇴 처리된 계정입니다.
```

서버 `code`를 분기 기준으로 사용하고 message 문자열 비교로 로직을 만들지 않는다.

## 11. 보안·개인정보 계약

- Refresh Token 원문은 기존처럼 Request에서만 받고 DB에는 hash만 저장한다.
- Token 원문·hash·sessionId·rotationFamilyId를 로그, metric, 오류 응답에 포함하지 않는다.
- 탈퇴 전용 오류는 hash와 일치하는 Session을 찾은 경우에만 반환한다.
- 모바일 로그·analytics에 Access Token·Refresh Token·Firebase ID Token을 기록하지 않는다.
- Firebase signOut 오류의 SDK 원문 credential이나 사용자 식별자를 analytics에 넣지 않는다.
- metric tag에는 userId·sessionId를 사용하지 않는다.
- Sentry event 정제와 Request Body 미수집 계약을 유지한다.

## 12. 관측 계약

서버 구조화 event 후보:

```text
event=auth.refresh.reissue.rejected
outcome=account_withdrawn
errorCode=ACCOUNT_WITHDRAWN
```

허용 metric 후보:

- `auth.refresh.reissue.rejected{reason=account_withdrawn}` count
- withdrawal 성공 뒤 최초 전용 오류까지의 집계 가능한 지연은 user 식별 tag 없이 별도 운영 분석으로 제한

모바일 관측 후보:

- terminal withdrawal handler 시작·완료·Firebase signOut 실패 count
- 중복 handler suppression count

오류 원문, Token, Firebase UID와 userId는 metric tag로 사용하지 않는다.

## 13. OpenAPI·클라이언트 handoff

`POST /api/v1/auth/reissue`의 401 설명에 다음 code를 명시한다.

- `INVALID_REFRESH_TOKEN`
- `REFRESH_TOKEN_EXPIRED`
- `REFRESH_TOKEN_REUSE_DETECTED`
- `ACCOUNT_WITHDRAWN`

모바일 handoff에는 최소 다음 내용을 포함한다.

- 신규 code와 HTTP status
- code 비교 기준, message 비교 금지
- 탈퇴 성공과 전용 오류가 같은 terminal handler를 사용한다는 계약
- Firebase signOut 실패와 무관한 Identity Token 삭제
- concurrent 401 단일 처리
- Stage 5 전에는 기존 Access Token의 downstream 즉시 차단이 보장되지 않는다는 제한

## 14. 테스트 계획

### 14.1 application 테스트

- `ACCOUNT_WITHDRAWN` Session은 `ACCOUNT_WITHDRAWN` 오류
- 해당 경로에서 User 조회·Token issuer·rotation save 호출 0건
- `ROTATED`는 계속 reuse detection과 활성 Session 폐기
- `LOGOUT`·`LOGOUT_ALL`·`REUSE_DETECTED`는 계속 invalid
- `GUEST_UPGRADED`·`GUEST_MERGED`는 계속 invalid
- active expired Session은 계속 expired
- active Session + WITHDRAWN User는 전용 오류
- active Session + SUSPENDED User는 기존 account-not-active
- User 없음은 invalid
- optimistic lock conflict는 기존 invalid와 Token 발급 0건

### 14.2 API 계약 테스트

- 전용 오류는 HTTP 401과 기존 `BaseResponse` shape 사용
- `isSuccess=false`, `code=ACCOUNT_WITHDRAWN`, `result=null`
- 응답·로그에 Refresh Token 원문·hash·내부 id 없음
- unknown Token은 계속 generic invalid
- validation 오류의 rejected value masking 유지
- OpenAPI 401 설명과 code 문서화

### 14.3 withdrawal lifecycle 회귀 테스트

- 탈퇴 User의 모든 active Session reason이 `ACCOUNT_WITHDRAWN`
- 각 active Refresh Token 재발급은 전용 오류
- 다른 User Session은 영향 없음
- LOCAL·GUEST·Firebase MEMBER 탈퇴 모두 같은 Session 외부 계약
- 반복 탈퇴와 이미 폐기된 과거 Session reason 불변

### 14.4 동시성·staging 테스트

- 탈퇴 commit과 같은 Session reissue 경쟁
- 여러 active Session의 동시 reissue
- 탈퇴 성공 응답 유실 뒤 같은 기기 앱 재시작
- Mongo replica set write conflict·rollback
- Token 발급 또는 후속 Session insert가 탈퇴 성공과 함께 남지 않는지 확인

### 14.5 모바일 contract/E2E

- 탈퇴 API 성공 직후 terminal handler
- 다른 기기의 `ACCOUNT_WITHDRAWN` 처리
- Firebase signOut 성공·실패 모두 local Token 삭제
- concurrent API 401에서 안내·navigation 한 번
- 앱 강제 종료와 재시작 중 cleanup 재개
- 다른 401에는 탈퇴 안내 없음
- 네트워크 5xx·timeout은 terminal withdrawal로 오분류하지 않음

## 15. 배포 순서

1. 모바일이 `ACCOUNT_WITHDRAWN` code와 terminal handler를 먼저 지원
2. 구버전 모바일의 미지원 code 처리 방식을 확인
3. 서버 code·service 분기·OpenAPI를 배포하되 전체 withdrawal production flag는 유지
4. staging에서 현재 기기·다른 기기·다중 기기·앱 재시작 E2E 수행
5. Firebase signOut 실패 주입과 local Token 삭제 검증
6. Stage 5 Learning Core consumer·deny gate 선배포
7. Stage 1~5 전체 activation gate 충족 뒤 Firebase/SNS withdrawal production 활성화

새 error code를 서버에서 먼저 반환해도 구버전 앱이 일반 401로 안전하게 로그아웃하는지 확인해야 한다. 보장할 수 없다면 모바일 최소 지원 버전 또는 서버 rollout gate를 둔다.

## 16. 예상 변경 파일

서버 구현 시 최소 변경 후보는 다음과 같다.

- `domain/auth/common/exception/AuthErrorStatus.java`
- `domain/auth/session/application/TokenReissueService.java`
- `domain/auth/common/api/AuthController.java` OpenAPI 설명
- `RefreshTokenUseCaseServicesTests.java`
- `AuthControllerTests.java`
- `UserWithdrawalLifecycleTests.java`
- 필요 시 구조화 로그 계약 테스트

모바일 코드는 별도 저장소에서 구현한다. Identity 저장소에는 모바일 구현체를 복사하지 않고 계약과 handoff만 유지한다.

## 17. 완료 조건

- `ACCOUNT_WITHDRAWN` 외부 오류의 HTTP status·code·message·응답 shape가 고정됨
- 실제 `ACCOUNT_WITHDRAWN` Session에만 전용 오류를 반환함
- unknown·일반 revoked·expired·rotated Session의 기존 계약이 유지됨
- active Session + WITHDRAWN User race fallback에서 Token 발급이 차단됨
- 탈퇴 Session 오류 경로의 Access·Refresh Token 발급과 Session mutation이 0건임
- 모바일이 탈퇴 API 성공과 전용 오류를 같은 멱등 terminal handler로 처리함
- Firebase signOut 실패에도 Identity Token·local user cache 삭제가 완료됨
- concurrent 오류·앱 재시작에서 안내와 navigation이 중복되지 않음
- 서버·모바일 contract test와 staging multi-device E2E가 통과함
- Token·credential·사용자 식별 정보가 응답·로그·metric에 노출되지 않음
- Stage 5 전에는 Access Token 즉시 차단이 보장되지 않음을 운영·모바일 문서에 명시함
- 모바일 호환성과 Stage 5 선행 배포 전에는 withdrawal production flag를 활성화하지 않음

## 18. 다음 단계와의 연결

Stage 4가 완료되면 Refresh 재발급을 시도한 앱은 탈퇴 사실을 인지하고 로컬 인증 상태를 정리할 수 있다. 그러나 재발급 없이 기존 Access Token을 사용하는 요청은 아직 남는다.

Stage 5는 다음을 구현한다.

- Identity `UserWithdrawn` outbox와 publisher
- Learning Core eventId inbox와 payload 멱등성
- old userId의 `blockedUntil/expireAt` deny marker
- JWT 로컬 검증 뒤 downstream local deny gate
- marker와 inbox의 유한 TTL·replay 계약

Stage 4와 5를 합쳐야 다른 기기의 Refresh 재발급 UX와 기존 Access Token의 downstream 차단을 모두 처리할 수 있다.
