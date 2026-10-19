# Identity·Learning Core 테스트 사전 점검

## 1. 5줄 결론

1. 2026-09-28 두 테스트 서버 health는 HTTPS 200/UP으로 확인했다.
2. Identity 프로필과 Learning Core 챌린지는 무토큰/잘못된 토큰 요청을 401로 거절했다.
3. Firebase exchange 및 refresh의 잘못된 인증정보 거절을 확인했다. 실제 사용자 생성·세션 회전은 하지 않았다.
4. Postman 앱 제어 권한이 없어 동일 HTTP 요청을 curl로 실행했다. 첨부 collection은 Postman 실행 완료 결과가 아니다.
5. Firebase 테스트 계정이 없어 MEMBER 가입·로그인·재발급·LC 성공 접근 및 GUEST403은 미검증이다.

## 2. 반드시 읽을 내용

[collection](identity-learning-smoke.postman_collection.json)을 Postman Import 후 실행한다. 테스트 전용 HTTPS base URL 두 개만 포함하며 실제 비밀값은 없다. 사용자 생성/삭제/정상 세션 회전 요청은 넣지 않았다. 재발급 오류 기대값은 현재 Stage9 OFF 기준이다.

| 요청 | 실제 관측 |
| --- | --- |
| Identity /actuator/health | 200 UP |
| Learning Core /actuator/health | 200 UP |
| Identity /.well-known/jwks.json | RSA/RS256, 테스트 kid 제공 |
| Identity /api/v1/users/me 무토큰·잘못된 토큰 | 각각 401 COMMON_UNAUTHORIZED |
| Identity /api/v1/auth/firebase/exchange 빈 body·잘못된 토큰 | 각각 401 INVALID_FIREBASE_ID_TOKEN |
| Identity /api/v1/auth/reissue 잘못된 refresh | 401 INVALID_REFRESH_TOKEN |
| LC /api/v1/challenges/today 무토큰·잘못된 토큰 | 각각 401 COMMON401 |

## 3. 사용자가 준비할 사항

Firebase SDK로 Google 인증 후 같은 UID에 테스트 전화번호 credential을 연결할 테스트 사용자가 필요하다. 프론트 전체 구현 대신 별도 최소 SDK 테스트 화면으로도 준비할 수 있다. Firebase 콘솔에 사용자만 수동 생성하거나 custom token으로 우회하면 실제 primary 로그인 검증을 대체하지 못한다. 실제 토큰은 채팅/문서/collection export에 넣지 않는다.

## 4. 위험과 미확인 사항

- Identity live OpenAPI servers[0].url이 http://identity-test.to-teacher.com으로 반환됐다. HTTPS Swagger에서 mixed content 차단 또는 비보안 요청 위험이 있어, Postman에서는 명시적 HTTPS를 사용한다. 수정하지 않았으며 배포의 프록시 헤더/명세 서버 설정 후속 검토 필요.
- LC live OpenAPI server URL은 상대경로 /이다.
- 401 성공만으로 두 서버 issuer/audience/JWKS 신뢰 연동을 검증했다고 판단하지 않는다.
- collection의 Postman runner 실행/스크립트 호환 검증은 미수행. 실제 HTTP 관측 결과로 기대값을 구성했고 JSON 구문과 요청 개수만 로컬 검사했다.

## 5. 다음 성공 경로

기존 [프론트 인증 계약](../contracts/frontend-firebase-auth-integration-guide.md)을 따라 exchange의 enrollment 발급, 같은 UID phone link, 강제 갱신한 Firebase ID Token 및 동의/닉네임으로 signup을 수행한다. 이후 exchange 로그인과 refresh를 확인하고, 두 Access Token 모두 UUID sub/MEMBER account_type/LC audience 및 서명을 검증한다. 그 토큰으로 LC today에 접근해야 실제 연결 확인이 된다. 데이터 생성은 테스트 사용자로 제한한다.

## 6. 근거

- Identity: https://identity-test.to-teacher.com/v3/api-docs
- Learning Core: https://api-test.to-teacher.com/v3/api-docs
- 두 서버 공개 명세 및 위 HTTP 요청만 조회/실행했다. 내부 DB·권한·기능 설정은 변경하지 않았다.
