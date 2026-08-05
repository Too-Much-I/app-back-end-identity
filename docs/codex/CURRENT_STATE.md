# Codex Current State

이 문서는 Codex 세션 시작 시 추가 개발자 컨텍스트로 읽힌다. Secret, Token, Password, 실제 Key, 전체 MongoDB URI를 기록하지 않는다.

## 프로젝트

- 이름: `app-back-end-identity`
- 현재 단계: 인증된 사용자의 개인정보 처리방침·이용약관 동의 상태 조회 API 구현과 전체 회귀 검증 완료
- 상태 기준일: 2026-08-05

## 완료

- Spring Boot 프로젝트 및 Identity용 의존성 구성
- 저장소 Codex 작업 규칙과 Identity–Learning Core JWT 계약 문서화
- CURRENT_STATE/WORKLOG 작업 기록 체계와 Codex Hook 구성
- Codex 사용자 전역 설정에 Atlassian Remote MCP(`atlassian`) 등록 및 OAuth 연결 확인
- Atlassian MCP의 읽기 전용 조회로 `to-teacher` 사이트의 접근 가능 Jira 프로젝트 1개(`TMI`)와 이슈 생성 권한, 사용 가능한 이슈 유형 `에픽`·`하위 작업`·`작업`·`스토리`를 생성·수정 호출 없이 확인
- 승인된 Refresh Token 재발급·Rotation·재사용 탐지·멱등 로그아웃 Payload로 TMI `작업` 이슈 `TMI-6`을 `High` 우선순위와 기본 상태 `해야 할 일`로 생성하고 담당자·라벨·상태 전환은 적용하지 않음
- Jira `TMI-6`의 사용 가능한 전환을 재확인한 뒤 사용자 승인에 따라 transition ID `21`만 전송해 `해야 할 일`에서 `진행 중`으로 변경하고 다른 필드·댓글·이슈는 수정하지 않음
- Jira `TMI-6` 구현 완료 댓글 초안을 사용자 승인에 따라 댓글 ID `10000`으로 등록하고 상태 `진행 중`과 다른 필드·이슈는 변경하지 않음
- 사용자가 `TMI-6` 구현 PR의 main 병합과 전체 테스트 성공을 확인한 뒤, `완료` 전환 ID `41`을 재확인해 `진행 중`에서 `완료`로 변경하고 다른 필드·댓글·이슈는 수정하지 않음
- Atlassian MCP 읽기 전용 조회로 TMI의 이슈 생성 권한, `작업` 유형 ID `10003`과 생성 필드 20개를 확인하고 `High` 우선순위 지원을 검증한 뒤 JWT 인증·내 프로필·전체 로그아웃 작업의 최종 Payload 초안을 작성했으며 Jira 이슈는 생성하지 않음
- 승인된 JWT 인증·내 프로필·전체 로그아웃 Payload로 TMI `작업` 이슈 `TMI-9`를 `High` 우선순위와 기본 상태 `해야 할 일`로 생성하고 담당자·스프린트·에픽·라벨·상태 전환은 적용하지 않음
- Jira `TMI-9`의 방금 확인한 `진행 중` transition ID `21`만 사용자 승인에 따라 적용해 `해야 할 일`에서 `진행 중`으로 변경하고 다른 필드·댓글·이슈는 수정하지 않음
- Atlassian 공식 MCP로 Jira `TMI-9`의 설명·완료 조건·상태를 구현 전에 읽기 전용 재조회하고 AGENTS.md 및 JWT 계약과 충돌이 없음을 확인했으며 댓글·상태·필드는 변경하지 않음
- 사용자가 PR의 main 병합과 제시된 Jira 변경을 확인·승인한 뒤 TMI-9의 현재 상태와 사용 가능한 전환을 재조회하고 transition ID `41`만 적용해 `진행 중`에서 `완료`로 변경했으며, 후속 조회에서 status ID `10003`과 Resolution `완료`를 확인하고 댓글·필드는 변경하지 않음
- Atlassian 공식 MCP의 읽기 전용 조회로 TMI 이슈 생성 권한, `작업` 유형 ID `10003`, 생성 필드 20개와 `High` 우선순위 ID `2` 지원을 재확인하고 `[Learning Core] Identity JWKS 기반 JWT 인증 연동` 최종 Payload 초안을 작성했으며 Jira 이슈는 생성하지 않음
- 사용자 승인에 따라 `[Learning Core] Identity JWKS 기반 JWT 인증 연동`을 TMI `작업` 이슈 `TMI-10`으로 `High` 우선순위와 기본 상태 `해야 할 일`로 생성하고, 후속 조회에서 제목·유형·우선순위·상태와 담당자 없음·빈 라벨을 확인했으며 스프린트·에픽·상태 전환은 적용하지 않음
- Atlassian 공식 MCP로 `TMI-10`의 제목과 현재 상태 `해야 할 일`을 읽기 전용 재조회하고 사용 가능한 전환 `해야 할 일` ID `11`, `검토 중` ID `31`, `진행 중` ID `21`, `완료` ID `41`을 확인했으며 Jira는 수정하지 않음
- Jira `TMI-10`의 현재 상태와 `진행 중` 전환 ID `21` 사용 가능 여부를 재확인한 뒤 사용자 승인에 따라 transition ID `21`만 적용하고, 후속 조회에서 status ID `10001`의 `진행 중`을 확인했으며 다른 필드·댓글·이슈는 수정하지 않음
- 환경변수 기반 애플리케이션 이름, MongoDB 데이터베이스 및 서버 포트 설정
- Swagger UI `/swagger-ui.html` 및 OpenAPI `/v3/api-docs` 설정과 `SWAGGER_ENABLED` 환경변수 기반 활성화 제어
- Actuator health endpoint 노출
- 외부 MongoDB 연결을 생성하지 않는 격리된 테스트 프로필
- STATELESS·CSRF 비활성화를 유지하면서 명시한 공개 경로만 허용하고 나머지를 인증하는 Security 구성
- 폼 로그인, Basic 인증 및 Spring Security 기본 생성 계정 비활성화
- 제네릭 `BaseResponse` 성공·실패 Factory
- `ErrorCode`, `CommonErrorStatus`, `BusinessException` 오류 기반
- Validation 상세 민감값 제거와 예상하지 못한 오류 정보 비노출을 포함한 전역 예외 처리
- `.env.example`, 실제 환경 파일 ignore 및 README 실행·경계 문서
- 외부 인프라를 호출하지 않는 Bootstrap 테스트 9개 통과
- `users` 컬렉션의 UUID `userId` 기반 User Document와 `ACTIVE`, `SUSPENDED`, `WITHDRAWN` 상태 모델
- 원본 대소문자를 보존하면서 앞뒤 공백을 제거한 표시용 이메일과 `Locale.ROOT` 소문자 정규화 이메일 분리
- `normalizedEmail`의 `uk_users_normalized_email` unique index 및 MongoDB 자동 index 생성 설정
- BCrypt `PasswordEncoder` Bean과 평문을 Entity에 전달하지 않는 최소 `UserFactory`
- `Instant` 기반 생성·수정 시각과 신규 사용자 `ACTIVE` 초기화
- 정규화 이메일 단건 조회·존재 확인만 제공하는 `UserRepository`
- 실제 MongoDB 연결 없이 User 도메인 기반을 검증하는 테스트를 포함해 전체 23개 통과
- `git diff --check`, 평문 필드·토큰 필드·비소유 도메인 및 생성 경계 정적 검사 통과
- `POST /api/v1/auth/check-email` 이메일 중복 확인 API와 정상 응답 기반 사용 가능 여부 반환
- `POST /api/v1/auth/signup` 일반 이메일 회원가입 API와 Request/Response validation 계약
- 이메일과 닉네임의 앞뒤 공백 제거 후 validation 및 기존 `EmailNormalizer` 기반 중복 조회
- 정규화 이메일 사전 중복 확인과 MongoDB `DuplicateKeyException`의 `EMAIL_ALREADY_EXISTS` 변환
- 비밀번호 8~64자 길이 정책과 별도 복잡도 정규식 없는 BCrypt 해시 저장
- `EMAIL_ALREADY_EXISTS`, 개인정보·약관 동의 필수 및 정책 버전 불일치 도메인 오류와 공통 `BaseResponse` 오류 응답
- User 문서에 개인정보 처리방침·이용약관 상태, 버전과 서버 동의 시각을 `UserConsents` embedded 객체 하나로 저장
- LOCAL 회원가입과 Guest 생성은 두 동의가 true이고 서버 현재 버전과 일치할 때만 진행하며 같은 서버 시각으로 두 동의를 원자적으로 저장
- Guest 동의가 포함된 User와 최초 RefreshSession 저장은 기존 Mongo Transaction에 함께 참여
- `POST /api/v1/auth/guest`는 UUID v4 `installationId`와 개인정보·약관 동의 네 필드를 검증해 최초 Guest와 Token을 생성하며, 설치 ID는 인증 수단이 아니므로 동일 설치 재요청은 기존 Token 복구 없이 409로 거절하고 이후 실행은 저장한 Refresh Token으로 `/api/v1/auth/reissue`를 사용
- `GET /api/v1/users/me/consents`는 JWT `sub`의 ACTIVE 사용자만 조회해 서버 현재 필수 버전, 저장된 동의 상태·버전·시각과 정확한 문자열 비교 기반 `requiresConsent`를 개인정보·약관별로 반환
- `PUT /api/v1/users/me/consents`는 JWT `sub` 사용자만 대상으로 현재 필수 두 정책 동의를 한 User 문서 저장으로 갱신하고, 동일 버전 재요청은 저장과 동의 시각 변경 없이 멱등 성공
- Guest·LOCAL 생성, 동의 상태 조회·검증·갱신·멱등성, 기존 Mongo 문서 호환, 프로필·Security·OpenAPI와 기존 인증 회귀를 포함한 전체 249개 테스트 성공
- Repository를 Mock 처리한 Service·Controller 테스트와 전체 44개 테스트 통과
- 회원가입 성공 응답 및 오류 응답의 해시·정규화 이메일·자격증명·MongoDB 내부 정보 비노출 검증
- `app.jwt` 기반 issuer, audience, keyId, Access Token TTL, RSA Key Resource 경로 및 기본 scope 설정
- PKCS#8 Private Key와 X.509 Public Key만 지원하고 오류에 키 내용을 포함하지 않는 RSA Resource 로더
- `RsaKeyLoader`의 모호한 `ResourceLoader` 컴포넌트 자동 주입을 제거하고 `JwtConfiguration`에서 유일한 `ApplicationContext`를 명시적으로 전달해 `GridFsTemplate`과의 IDE 빈 후보 충돌을 해소하면서 기존 `rsaKeyLoader` 빈 이름과 Resource 해석 동작 유지
- RSA Key 타입과 Private/Public Key 쌍 일치 검증 및 민감값을 숨기는 Key Material 문자열 표현
- `RSAKey`, `JWKSet`, `JWKSource<SecurityContext>`, `NimbusJwtEncoder`를 사용하는 Spring Security 6.4 호환 서명 구성
- `RS256`, `SIGNATURE`, `kid` 메타데이터가 고정된 서버 내부 RSA JWK와 `Clock.systemUTC()` Bean
- UUID `userId` 검증, 고정 순서 scope 및 기본 scope fallback을 제공하는 내부 `AccessTokenIssuer`
- `sub`, `iss`, 단일 원소 배열 `aud`, `iat`, `exp`, UUID `jti`, 공백 구분 `scope`와 `alg=RS256`, `kid`, `typ=JWT`를 갖춘 Access Token 발급
- `GET /.well-known/jwks.json`에서 `toPublicJWK()` 결과만 표준 JWKS로 반환하고 BaseResponse를 적용하지 않는 공개 endpoint
- 로컬 RSA 2048비트 키 생성 스크립트, Private/Public Key 권한 설정, 명시적 `--force` 교체 및 `.local/` Git ignore
- Java `KeyPairGenerator`와 고정 Clock을 사용하는 JWT·JWKS·Key Loader 테스트 10개 추가 및 전체 54개 테스트 통과
- 실제 PEM 본문·서명 토큰·자격증명 포함 URI·민감 로그 부재와 JWKS/Access Token의 Private Key·자격증명 정보 비노출 검증
- 회원가입 응답과 외부 임시 endpoint에는 Access Token을 연결하지 않음
- `POST /api/v1/auth/login` 일반 이메일 로그인 API와 `LoginRequest`/`LoginResponse` validation·응답 계약
- 정규화 이메일 조회, BCrypt 검증, `ACTIVE` 상태 확인 후에만 토큰 발급을 진행하는 `LoginService.login`
- 존재하지 않는 이메일과 불일치 자격증명을 동일한 `INVALID_CREDENTIALS` 401로 처리하고 비활성 계정을 상태 구분 없는 `ACCOUNT_NOT_ACTIVE` 403으로 처리
- 로그인 성공 시 검증된 User의 UUID를 기존 `AccessTokenIssuer`에 전달해 기존 RS256 Header·Claim·기본 scope·TTL 계약을 그대로 사용
- 기존 `IssuedAccessToken`의 `issuedAt`과 `expiresAt` 차이를 milliseconds로 계산해 `accessTokenExpiresIn`에 반환하고 `grantType`은 `Bearer`로 고정
- `app.refresh-token`의 `Duration` TTL과 최소 32바이트 난수 길이 설정, 기본값 `P14D`와 32 및 32 미만 시작 거부
- `SecureRandom`과 Base64 URL-safe without padding을 사용하는 JWT가 아닌 Opaque Refresh Token 생성기
- UTF-8 원문에 SHA-256을 적용한 뒤 Base64 URL-safe without padding으로 인코딩하는 일관된 Refresh Token 해시기
- `refresh_sessions` 컬렉션의 UUID 기반 세션·사용자·회전 패밀리 관계, 생성·만료·최근 사용·폐기 시각, 폐기 사유와 `@Version` 기반 Optimistic Lock 모델
- Refresh Token 해시 unique index와 `expiresAt`의 `expireAfter = "0s"` TTL index, 해시 단건 조회 및 사용자별 미폐기 Session 조회만 제공하는 `RefreshSessionRepository`
- 공용 `Clock` 기준 최초 Session과 Rotation 후속 Session을 발급하고 Refresh Token 원문은 내부 발급 결과로만 반환하는 `RefreshSessionIssuer`
- `POST /api/v1/auth/reissue`에서 해시 조회, ROTATED 재사용 판별, 명시적 만료 경계 검사, `ACTIVE` 사용자 확인, 기존 Session Optimistic Lock 폐기 성공 후 새 Access Token과 Refresh Token 발급
- Rotation 시 기존 Session은 `ROTATED` 사유와 후속 관계를 저장하고 새 Session은 같은 회전 패밀리와 이전 관계를 유지하며, 최초 로그인 Session은 새 UUID 회전 패밀리를 생성
- 같은 Refresh Token의 동시 재발급에서 기존 Session 저장 충돌을 `INVALID_REFRESH_TOKEN`으로 변환하고 충돌 요청에는 Access Token 발급이나 후속 Session 생성을 수행하지 않음
- 이미 Rotation된 Refresh Token 재사용 시 사용자별 미폐기 Session 전체를 `REUSE_DETECTED` 사유로 폐기하고 `REFRESH_TOKEN_REUSE_DETECTED` 401 반환
- 존재하지 않는 Refresh Token은 `INVALID_REFRESH_TOKEN` 401, `expiresAt <= Clock`은 `REFRESH_TOKEN_EXPIRED` 401, 비활성 사용자는 기존 `ACCOUNT_NOT_ACTIVE` 403 정책 적용
- `POST /api/v1/auth/logout`에서 활성·미만료 Session만 `LOGOUT` 사유로 폐기하고 없는·이미 폐기된·만료된 Refresh Token은 성공 처리하는 멱등 흐름 구현
- 재발급·로그아웃 Request의 `NotBlank`와 최대 512자 제한, Refresh Token validation 값 마스킹 및 Request·Response 문자열 redaction 적용
- Reissue 응답의 Access Token·Refresh Token 만료 기간을 milliseconds로 반환하고 내부 사용자·세션·해시·비밀번호 관련 필드를 외부 응답에 포함하지 않음
- 실제 Atlas와 운영 키를 사용하지 않는 Service·Controller·도메인 회귀 테스트를 포함해 전체 97개 통과, 실패·오류·건너뜀 0개
- RefreshSession 원문 필드·Access Token 영속화·민감 로그·운영 자격증명 하드코딩 부재와 외부 응답 내부 필드 비노출 검증
- 기존 `RSAPublicKey`, `JwtProperties`, `Clock`을 재사용하고 자기 JWKS를 HTTP 호출하지 않는 `NimbusJwtDecoder` 구성
- Decoder에서 RS256 서명, 엄격한 `typ=JWT`, 현재 `kid`, 필수 `sub`·`exp`, 주입 Clock 기반 `exp`·`nbf`, issuer와 audience를 `DelegatingOAuth2TokenValidator`로 검증
- 공개 인증 POST 5개, JWKS·health·Swagger/OpenAPI GET만 `permitAll`로 두고 `anyRequest().authenticated()`를 적용하며 Form Login과 Basic 인증은 계속 비활성화
- Security Filter Chain의 401 `COMMON_UNAUTHORIZED`와 403 `COMMON_FORBIDDEN`을 UTF-8 `BaseResponse` JSON으로 반환하고 내부 JWT 예외나 입력 자격증명을 노출하지 않음
- `JwtCurrentUserProvider`가 인증된 `JwtAuthenticationToken` 또는 JWT principal의 `sub`만 읽고 canonical UUID로 검증해 내부 사용자 식별자로 제공
- `GET /api/v1/users/me`에서 JWT `sub`로 User를 조회하고 `ACTIVE` 상태를 확인한 뒤 provider와 개인정보·약관 동의 상태·버전·서버 시각을 전용 DTO로 반환
- User에 최소 `UserProvider.LOCAL` 모델을 추가하고 기존 문서의 null provider는 LOCAL로 읽어 현재 이메일 계정과 호환
- `POST /api/v1/auth/logout-all`에서 JWT 사용자의 미폐기 RefreshSession만 조회해 같은 Clock 시각과 `LOGOUT_ALL` 사유로 `saveAll`하며 빈 Session 목록과 반복 요청은 성공 처리
- logout-all은 새 Access Token이나 Refresh Token을 발급하지 않고 Repository 오류를 성공으로 숨기지 않으며 기존 `@Version` Optimistic Lock 구조를 유지
- Spring Security test 지원을 추가하고 실제 외부 인프라 없이 공개·보호 경로, Decoder 실패, scope 권한 변환, 프로필 노출 경계와 전체 로그아웃 사용자 격리를 검증
- `domain.auth`와 `domain.user` 아래에 API·application·DTO·domain·exception을 배치하고 공통 설정·응답·예외·Spring Security 기술 구현을 `global` 아래로 이동
- MongoDB 상태 객체 `RefreshSession`·폐기 enum·Repository를 Auth 도메인에 두고 Refresh Token 난수 생성·SHA-256 해싱·설정은 `global.security.refresh`로 분리
- 비대했던 `AuthService`를 `EmailAvailabilityService`, `SignupService`, `LoginService`, `TokenReissueService`, `LogoutService`로 분리하고 기존 `LogoutAllService`와 함께 Controller가 유스케이스만 호출하도록 구성
- 로그인·재발급의 다중 토큰 응답 조합은 `AuthResponseConverter`로 통합하고 `AuthException`·`UserException`은 기존 `BusinessException`과 오류 코드/HTTP 상태 계약을 유지
- OpenAPI 제목·설명·버전과 `bearerAuth` JWT 스키마를 추가하고 Auth·User·JWKS Tag, API 응답, DTO Schema를 문서화하며 보호 API 두 개에만 Bearer 요구사항 적용
- 공개 API의 Swagger 인증 표시 부재, 보호 API의 Bearer 표시, 비밀번호 write-only와 비밀번호 예시 부재를 `/v3/api-docs` 계약 테스트로 검증
- malformed JSON, 미존재 리소스, 지원하지 않는 Method와 Media Type을 각각 안전한 공통 400·404·405·415 응답으로 변환하고 요청 원문·내부 예외 정보를 노출하지 않도록 전역 예외 처리 보완
- malformed JSON 보안 테스트의 미완성 JSON 문자열을 지역 변수로 분리해 IDE 파서 혼동을 줄이면서 기존 400 응답·민감 입력 비노출 검증 의미를 유지
- 계정 활성 상태 오류 소유권을 User 도메인으로 일원화하고 converter의 application 내부 결과 타입 역참조를 제거해 Auth → User와 application → converter의 단방향 의존으로 정리
- 리팩토링 신규 파일과 기존 삭제를 함께 stage해 104개 논리 변경 파일, rename 68개, untracked 0개 상태를 구성하고 삭제 전용 index 문제를 해소
- 관련 테스트와 기존 signup·login·reissue·logout·JWT·JWKS 회귀를 포함한 전체 146개 통과, 실패·오류·건너뜀 0개이며 `./gradlew build` 성공
- 실제 JAR 기동으로 health·Swagger/OpenAPI 200, 공개·보호 operation 구분, malformed JSON 400, 미존재 경로 404, 무인증 보호 API 401, 공개 로그인 validation 접근과 415를 확인하고 Swagger 비활성 문서 경로의 404를 검증
- main 병합 후 인증 유스케이스·RefreshSession·JWT·Security·예외 처리의 비자명한 의도에만 한국어 한 줄 주석 25개를 추가하고 실행 코드는 변경하지 않은 채 전체 146개 테스트와 build를 재검증

## 진행 중

- Jira `TMI-10` — `진행 중`, 승인된 Learning Core JWT 연동 이슈 생성과 상태 전환 완료, 구현 미착수

## 다음 작업

- 사용자가 동의 상태 조회 API와 문서의 unstaged diff를 검토한 뒤 필요하면 직접 commit·push하며 Jira 댓글이나 상태 변경은 별도 승인 전까지 수행하지 않음
- ECS Task Definition에 필수 개인정보 처리방침·이용약관 버전을 비공백 값으로 설정한 뒤 staging에서 GET 조회와 PUT 갱신 흐름을 검증
- MongoDB replica set·Transaction 지원 여부를 확인해 RefreshSession 회전과 다중 폐기의 원자성·동시 재발급 통합 테스트를 별도 작업으로 설계
- 운영 `refresh_sessions`의 `{userId, revokedAt}` 실행계획과 `_class` 외부 소비 여부를 확인하고 필요한 경우에만 승인된 index/migration으로 처리
- OpenAPI 오류 응답의 일반 오류·Validation 배열 schema 구체화와 UserFactory의 `Clock` 주입·application 이동 여부를 후속 개선으로 검토
- Learning Core 저장소에서 Jira `TMI-10`을 기준으로 Identity JWKS를 조회해 RS256 서명·issuer·`tosunsaeng-learning-core` audience를 로컬 검증하고 JWT `sub`를 실제 userId로 사용하는 연동 작업
- Identity 보호 API용 별도 audience 또는 다중 audience 도입 여부와 scope 기반 세부 인가 정책을 후속 보안 설계에서 확정
- 운영 배포 전에 기존 RefreshSession 문서의 Optimistic Lock 버전과 회전 패밀리 필드 이행 정책 확정
- MongoDB Transaction 없이 기존 Session 폐기 후 후속 Session 저장이 실패하는 경우의 복구 또는 재로그인 UX 정책 확정
- 배포 환경의 issuer/JWKS URL 합의와 이전 Public Key 유지 기간을 포함한 다중 키 Rotation 전략 확정

## 중요 결정

- Java 21
- Jira `TMI-6`은 사용자 확인 기준 main 병합·전체 테스트 성공 후 명시적 승인으로 `완료` 전환됐으며, 추가 댓글이나 상태 변경은 별도 명시적 승인 후 수행
- Jira `TMI-9`는 사용자 확인·승인 후 transition ID `41`만 적용해 `완료`로 전환됐고 Resolution도 `완료`로 확인했으며, 댓글·필드와 다른 이슈는 변경하지 않음
- Jira `TMI-10`은 사용자 승인에 따라 `High` 우선순위로 생성한 뒤 별도 승인으로 transition ID `21`만 적용해 `진행 중`으로 전환했으며 담당자·스프린트·에픽·라벨·댓글과 다른 필드는 변경하지 않음
- Atlassian 연동은 저장소 설정이 아닌 Codex 사용자 전역 MCP 설정으로 관리하며 Remote MCP URL은 `https://mcp.atlassian.com/v1/mcp/authv2`를 사용
- Spring Boot 3.4.2
- MongoDB
- 현재 작업 기준 브랜치는 `main`이고 HEAD는 `231e06d`이며 이번 동의 상태 조회 API 변경은 commit·stage하지 않음
- 주석은 비자명한 인증·세션·보안 의도에만 한 줄로 추가하고 DTO 필드·getter·단순 대입에는 추가하지 않음
- 애플리케이션 코드는 `domain.auth`, `domain.user`, `global`의 세 최상위 역할로 나누고 실제 클래스가 없는 빈 패키지는 만들지 않음
- Controller는 Repository를 직접 참조하지 않고 유스케이스 application service만 호출하며 단일 구현체를 위한 `Service`/`ServiceImpl` 인터페이스는 만들지 않음
- `RefreshSession`과 Repository는 Auth 도메인이 소유하고 Refresh Token 생성·해싱·설정은 `global.security.refresh`의 기술 구현이 소유
- `MongoTransactionManager`는 Guest User와 최초 RefreshSession 저장에 적용하며, 기존 Rotation·재사용 탐지·logout-all의 다중 Session 저장 원자성은 별도 후속 범위로 유지
- OpenAPI Bearer 스키마는 전역 적용하지 않고 `GET /api/v1/users/me`, `GET`·`PUT /api/v1/users/me/consents`와 `POST /api/v1/auth/logout-all`에 operation 단위로 적용
- Swagger/OpenAPI는 기본 활성화하되 배포 환경에서 `SWAGGER_ENABLED=false`로 비활성화 가능하고 테스트 프로필은 문서 계약 검증을 위해 명시적으로 활성화
- 실제 `userId`는 UUID 문자열
- User Document는 `users` 컬렉션을 사용하고 UUID 문자열 `userId`를 MongoDB `@Id`로 저장
- 이메일 정규화는 null 거부, 앞뒤 공백 제거, `Locale.ROOT` 소문자 변환만 수행
- Provider별 점 제거와 plus addressing 제거는 수행하지 않음
- `normalizedEmail`은 명시적인 unique index를 사용하며 현재 애플리케이션에서 자동 index 생성을 활성화
- 비밀번호 해시는 Spring Security의 기본 cost를 사용하는 BCrypt로 생성하고 User에는 `passwordHash`만 저장
- 회원가입 비밀번호 validation은 8~64자이고, 로그인 입력은 `NotBlank`와 최대 64자만 확인해 가입 복잡도·최소 길이 정책을 다시 적용하지 않음
- 이메일과 닉네임은 앞뒤 공백을 제거한 값으로 validation하며 비밀번호는 공백을 포함한 입력값을 임의 변환하지 않음
- 이메일 중복 확인은 가입 여부와 관계없이 성공 응답을 사용하며 `isAvailable`로 결과를 구분
- 회원가입의 사전 중복과 unique index 저장 충돌은 모두 `EMAIL_ALREADY_EXISTS` 409 오류로 통일
- 회원가입과 Guest 생성의 개인정보·약관 동의는 모두 반드시 true이며 false와 서버 현재 버전 불일치는 구분된 400 도메인 오류로 처리
- 현재 정책 버전은 필수 `app.consent.privacy-version`/`PRIVACY_CONSENT_VERSION`과 `app.consent.term-version`/`TERM_CONSENT_VERSION`으로 관리하고 누락·공백 시 기동에 실패하며 `AUDIO_POLICY_VERSION`은 제거
- 현재 동의 상태는 User 문서 내부의 `UserConsents`로 저장하고 별도 이력 컬렉션은 만들지 않음
- 기존 Mongo 문서에 `consents`가 없으면 두 동의를 false, 버전과 시각을 null로 읽고 과거 `audioConsent`를 새 동의로 자동 변환하지 않음
- User 생성·수정 시각 타입은 `Instant` 사용
- User provider는 `LOCAL`과 `GUEST`를 지원하며 기존 null provider 문서는 LOCAL로 해석
- Access Token은 RSA Private Key를 가진 Identity에서만 JWT RS256으로 서명
- Access Token 기본 TTL은 `PT30M`이며 issuer, audience, keyId, TTL과 키 Resource 위치는 환경변수로 교체 가능
- JWT Header는 `alg=RS256`, `typ=JWT`, 필수 `kid`를 사용
- JWT Claim은 UUID `sub`, `iss`, 단일 대상 배열 `aud`, `iat`, `exp`, UUID `jti`, 공백 구분 `scope`로 최소화
- 요청 scope는 정렬해 결정적으로 직렬화하고 null 또는 빈 scope에는 `learning:read learning:write` 기본값 사용
- Access Token에는 이메일, 닉네임 전체, 비밀번호 관련 값, Refresh Token 또는 Private Key 정보를 포함하지 않음
- RSA Private Key는 PKCS#8 `PRIVATE KEY`, Public Key는 X.509 `PUBLIC KEY` PEM 형식만 지원
- `RsaKeyLoader`는 Spring stereotype이 없는 로더로 유지하고 `JwtConfiguration`이 `ApplicationContext`를 `ResourceLoader`로 전달해 빈을 구성
- Spring Security 6.4 호환을 위해 `RSAKey` → `JWKSet` → `JWKSource<SecurityContext>` → `NimbusJwtEncoder` 구성을 사용
- JWKS는 `/.well-known/jwks.json`에서 Public JWK만 표준 `keys` 배열로 공개하며 BaseResponse로 감싸지 않음
- 현재 단일 Active Key를 사용하고 향후 Rotation에서는 기존 토큰 만료와 캐시를 고려해 이전 Public Key를 일정 기간 유지
- Access Token 발급 시간은 주입된 `Clock`을 사용하고 운영 기본값은 UTC 시스템 Clock
- Identity Resource Server는 기존 RSA Public Key Bean만 사용하고 자기 JWKS endpoint를 HTTP로 호출하지 않으며 Private Key를 검증에 사용하지 않음
- Identity Decoder는 RS256, 정확한 `typ=JWT`, 현재 `kid`, 필수 `sub`·`exp`, zero-skew `exp`·`nbf`, 설정 issuer와 audience 포함 여부를 검증
- 현재 Identity 보호 API도 기존 `tosunsaeng-learning-core` audience Access Token을 허용하며 Identity 전용 audience 또는 다중 audience는 이번 범위에 도입하지 않음
- 기본 `scope` 문자열은 Spring Security에서 `SCOPE_` 권한으로 변환하지만 이번 API는 특정 scope를 강제하지 않고 인증 여부만 적용
- 공개 경로는 지정된 인증 API, JWKS, health와 Swagger/OpenAPI로 제한하고 나머지는 기본적으로 인증
- 로그인은 `EmailNormalizer` 조회, BCrypt 일치 확인, `ACTIVE` 상태 확인 순서로 처리하고 실패 시 Access Token 발급기와 RefreshSession 저장소를 호출하지 않음
- 로그인 Access Token은 `AccessTokenIssuer.issue(userId, empty scopes)`를 호출해 설정된 기본 scope를 사용하며 Service에서 JWT를 직접 조립하지 않음
- 로그인 응답은 `accessToken`, `refreshToken`, `grantType`, milliseconds 단위 `accessTokenExpiresIn`만 포함하고 내부 사용자·세션 정보를 포함하지 않음
- Refresh Token 기본 TTL은 14일이고 난수 길이는 최소 32바이트이며 두 값은 `app.refresh-token` 환경 설정으로 교체 가능
- Refresh Token은 `SecureRandom` 기반 Opaque 값이고 Base64 URL-safe without padding으로 인코딩하며 UUID나 JWT를 대체 형식으로 사용하지 않음
- Refresh Token 원문은 `RefreshSession`에 저장하지 않고 UTF-8 SHA-256의 Base64 URL-safe without padding 해시만 저장
- `RefreshSession.createdAt`과 최초 `lastUsedAt`은 주입된 공용 `Clock`의 같은 시각이며 `expiresAt`은 해당 시각에 설정 TTL을 더함
- MongoDB TTL index는 만료 문서 정리 용도이며 향후 재발급은 문서 존재 여부와 별개로 `expiresAt`과 `revokedAt`을 직접 검증
- 토큰을 보유하는 내부 발급 결과와 로그인 응답의 문자열 표현은 토큰 값을 redaction 처리
- 재발급은 폐기 사유 확인 후 `expiresAt <= Clock`을 만료로 판정하고, 사용자 상태 확인 뒤 기존 Session의 Optimistic Lock 저장이 성공한 요청만 후속 토큰을 발급
- Rotation 재사용 탐지는 해당 사용자의 미폐기 RefreshSession 전체를 폐기하며 LOGOUT 또는 다른 폐기 사유는 일반 `INVALID_REFRESH_TOKEN`으로 구분
- 단일 로그아웃은 한 RefreshSession을 `LOGOUT`, 전체 로그아웃은 현재 JWT 사용자의 미폐기 RefreshSession 전체를 `LOGOUT_ALL`로 폐기
- 로그아웃은 Access Token 블랙리스트를 만들지 않으므로 기존 Access Token은 만료 시각까지 유효할 수 있으며 클라이언트는 성공 즉시 로컬 두 토큰을 삭제
- 로컬 키는 저장소에서 무시하고 테스트 키는 매 테스트 런타임에 메모리에서 생성
- Learning Core `audience`는 `tosunsaeng-learning-core`
- 실제 `userId`를 Python AI 서버로 보내지 않으며 Python AI의 `user_id`는 `examId` 유지
- 공통 응답 필드는 `isSuccess`, `code`, `message`, `result`
- 공통 오류 enum의 HTTP 상태와 응답 코드를 분리해 관리
- Validation 오류 상세는 `result` 배열로 반환하고 민감한 `rejectedValue`는 `null` 처리
- 테스트 프로필에서는 MongoDB 클라이언트·데이터·Repository 자동 설정 제외
- Security Filter Chain 내부 401/403은 공통 UTF-8 `BaseResponse` JSON Handler가 처리하고 Form Login과 Basic 인증은 비활성화

## 아직 구현되지 않은 것

- 다중 Active/Retiring Key를 지원하는 Key Rotation
- 소셜 로그인
- 사용자 프로필 수정 API
- 정책 버전별 append-only 동의 감사 이력과 동의 철회 정책

## 남아 있는 위험 요소

- `RefreshSession`에는 token hash unique와 만료 TTL index만 선언되어 있어 사용자별 미폐기 Session 조회가 운영 데이터 규모에서 collection scan이 되는지는 실제 운영 index와 `explain` 확인이 필요하다.
- 패키지 이동으로 신규 Mongo 문서의 `_class` FQCN이 바뀔 수 있으므로 외부 시스템이 `_class`를 조회 조건으로 사용하는지는 운영 데이터와 소비자에서 확인해야 한다.
- OpenAPI 오류 응답은 raw `BaseResponse` schema를 사용해 Validation의 `result` 배열을 완전히 구체화하지 못하므로 문서 전용 wrapper 도입 범위를 후속 검토해야 한다.
- `UserFactory`는 Spring `PasswordEncoder`·설정 주입과 `Instant.now()`에 직접 결합되어 있어 `Clock` 주입 및 application 계층 이동 여부를 후속 검토해야 한다.
- Atlassian MCP는 사용자 계정 권한으로 외부 서비스에 접근하므로 허용 범위와 연결 해제 필요성을 Codex 사용자 설정 및 Atlassian 계정에서 별도로 관리해야 한다.
- 현재 자동 index 생성은 초기 개발 편의를 위한 설정이며, 운영에서는 권한·데이터 규모·무중단 배포를 고려한 별도 index 관리 정책이 필요하다.
- 기존 User 문서는 migration 없이 읽을 수 있지만 새 정책 미동의로 취급되므로 프론트의 재동의 유도와 정책 전환 시점 합의가 필요하다.
- 기존 User 문서의 provider가 없으면 LOCAL로 읽지만 소셜 로그인 도입 전에는 provider 필드 명시적 이행과 계정 연결 정책이 필요하다.
- 이메일 중복 확인·회원가입·로그인·재발급·로그아웃 공개 API에는 rate limit, credential stuffing 방어, 자동화 요청 방어 및 abuse 관측 기준이 필요하다.
- 비밀번호 복잡도, 유출 비밀번호 차단 및 변경 정책은 제품·보안 명세 확정 후 추가해야 한다.
- 현재는 최신 개인정보·약관 동의 상태만 저장하므로 철회와 정책 버전 변경의 전체 감사를 요구하면 append-only 이력 관리가 필요하다.
- 사용자 정보 변경 기능을 추가할 때 `updatedAt` 갱신 책임과 동시 수정 정책을 명확히 해야 한다.
- 민감한 validation 필드명이 추가되면 마스킹 목록도 갱신해야 한다.
- 운영 환경의 MongoDB 연결과 health 상태는 배포 환경에서 별도로 검증해야 한다.
- 예외 타입만 기록하는 현재 정책을 보완할 운영 관측성 기준이 필요하다.
- 운영 RSA Key의 생성·주입·파일 권한·백업·교체는 저장소 밖의 Secret 관리 및 배포 절차로 확정해야 한다.
- 현재 JWKS는 단일 Active Key만 제공하므로 Rotation 전에 복수 Public Key 제공과 캐시 전파 기간을 구현해야 한다.
- 배포 환경의 `issuer`와 Learning Core 검증 설정이 정확히 일치해야 하며 HTTPS 배포 URL과 환경별 값을 함께 확정해야 한다.
- 서버 간 Clock 차이가 Access Token 검증에 미치는 영향을 고려해 Learning Core의 허용 오차 정책을 정해야 한다.
- Identity 보호 API가 현재 Learning Core audience 토큰을 함께 허용하므로 Identity 전용 audience 또는 다중 audience와 토큰 용도 분리 여부를 후속 검토해야 한다.
- scope는 `SCOPE_` 권한으로 변환되지만 endpoint별 scope 인가를 강제하지 않으므로 권한 모델 확정 후 세부 정책을 추가해야 한다.
- MongoDB TTL 삭제는 비동기 정리이므로 만료 문서가 일시적으로 남을 수 있으며 재발급은 계속 `expiresAt`과 폐기 상태를 애플리케이션에서 검사해야 한다.
- MongoDB Transaction을 도입하지 않았으므로 기존 Session의 Rotation 폐기 저장 이후 Access Token 발급 또는 후속 RefreshSession 저장이 실패하면 사용자가 현재 Session을 잃고 다시 로그인해야 할 수 있다.
- 기존 RefreshSession 문서에 `@Version` 또는 회전 패밀리 필드가 없다면 운영 적용 전에 데이터 이행 또는 기존 Session 만료·재로그인 정책이 필요하다.
- 재사용 탐지에서 여러 활성 Session을 폐기하는 저장은 Transaction으로 묶이지 않으므로 중간 저장 실패 시 일부 Session만 폐기될 가능성이 남아 있다.
- logout-all의 여러 Session `saveAll`도 Transaction이 아니므로 중간 실패 시 일부 Session만 폐기될 수 있으며 오류는 호출자에게 전파된다.
- 단일·전체 로그아웃은 Access Token을 즉시 무효화하지 않으므로 클라이언트의 로컬 토큰 삭제와 짧은 Access Token TTL을 함께 유지해야 한다.
- 자격증명 실패의 외부 code와 message는 통일했지만 사용자 부재 경로와 BCrypt 검증 경로의 실행 시간 차이에 대한 완화 정책은 rate limit·관측 기준과 함께 검토해야 한다.
- Java 패키지 경로가 전면 변경됐으므로 이 저장소 내부 테스트는 통과했지만, 패키지 FQCN을 직접 참조하는 별도 모듈이 존재한다면 새 `domain`·`global` 경로로 import를 갱신해야 한다.
- OpenAPI 응답 설명은 런타임 계약을 보조하는 문서이므로 후속 API 오류 코드나 보안 정책 변경 시 Controller 어노테이션과 문서 계약 테스트를 함께 갱신해야 한다.

## Codex Hook 운영 메모

- 프로젝트 로컬 Hook은 이 저장소와 각 Hook 정의가 신뢰된 경우에만 실행된다.
- Codex CLI에서 `/hooks`를 열어 `.codex/hooks.json`의 명령을 검토하고 신뢰해야 한다.
- Hook 명령이 변경되면 정의의 hash가 달라지므로 `/hooks`에서 다시 검토하고 신뢰한다.
- 모든 작업 종료 전에 WORKLOG를 append하고 이 문서를 최신 상태로 갱신한다.
