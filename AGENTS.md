# 토선생 Identity Service Codex 작업 규칙

이 규칙은 저장소 전체에 적용한다. 이 저장소는 토선생 앱의 Identity Service이며, Codex는 아래 경계와 계약을 유지한다.

## 기술 환경

- Java 21
- Spring Boot 3.4.2
- Gradle Groovy
- MongoDB
- Spring Security
- Spring Security OAuth2 JOSE
- Spring Security OAuth2 Resource Server
- 기본 테스트 명령: `./gradlew clean test`

## 프로젝트 역할과 도메인 경계

Identity Service가 소유하는 기능은 다음과 같다.

- 사용자 계정
- 이메일 회원가입
- 이메일 로그인
- 소셜 로그인
- 비밀번호 해시
- Access Token 발급
- Refresh Token 세션
- 로그아웃
- 사용자 프로필
- 음성 데이터 수집 동의

Identity Service가 소유하지 않는 기능은 다음과 같다.

- 시험
- 시험 문제
- AI 채점
- 시험 결과
- 10초 챌린지
- 스트릭
- 단어장
- 음성 파일
- AWS S3 업로드

Identity 도메인 이외의 시험, 채점, 챌린지, 스트릭, 단어장 코드를 추가하지 않는다. Learning Core 코드를 이 저장소로 복사하지 않는다.

## 사용자 ID 규칙

- 실제 사용자 ID는 UUID 문자열이다.
- JWT `sub`에는 실제 `userId`를 넣는다.
- 클라이언트가 보낸 `userId`를 신뢰하지 않는다.
- 외부 Request Body에 임의로 `userId`를 추가하지 않는다.

## JWT 계약

- Access Token은 RS256으로 서명한다.
- Identity Service만 RSA Private Key를 가진다.
- JWT Header에는 `kid`를 포함한다.
- Learning Core는 JWKS를 통해 Public Key를 조회한다.
- Learning Core는 `issuer`와 `audience`를 검증한다.
- `audience`는 `tosunsaeng-learning-core`다.
- 매 Learning Core 요청마다 Identity 서버의 토큰 확인 API를 호출하지 않는다.
- Python AI 서버의 `user_id`는 계속 `examId`다.
- 실제 `userId`를 Python AI 서버로 보내지 않는다.

상세 서버 간 계약은 `docs/contracts/identity-learning-jwt.md`를 따른다.

## 보안 규칙

- 실제 MongoDB URI를 저장소에 작성하지 않는다.
- 실제 RSA Private Key를 저장소에 작성하지 않는다.
- 비밀번호, Access Token, Refresh Token을 로그에 남기지 않는다.
- Refresh Token 원문을 DB에 저장하지 않는다.
- 환경변수 이름과 가짜 테스트 값만 커밋한다.
- `application-test.yml`에서 실제 외부 인프라를 호출하지 않는다.

## Git 규칙

Codex는 다음 작업을 직접 수행하지 않는다.

- `git commit`
- `git push`
- force push
- `git reset --hard`
- GitHub Actions 배포 추가

커밋과 push는 사용자가 직접 수행한다.

## 테스트 규칙

- 변경한 비즈니스 로직에 테스트를 작성한다.
- 기본 검증으로 `./gradlew clean test`를 실행한다.
- 실제 Atlas나 외부 OAuth Provider를 테스트에서 호출하지 않는다.
- 외부 Provider와 Repository는 Mock으로 처리한다.

## 작업 기록 규칙

모든 Codex 작업이 끝나기 전에 다음을 수행한다.

1. `docs/codex/WORKLOG.md` 끝에 새 항목을 append한다.
2. `docs/codex/CURRENT_STATE.md`를 최신 상태로 갱신한다.
3. WORKLOG의 과거 기록은 수정하거나 삭제하지 않는다.
4. 코드 변경이 없는 분석 작업도 기록한다.
5. 새 기록에 다음 항목을 포함한다.
   - 날짜
   - 브랜치
   - 작업 목표
   - 변경 파일
   - 구현 내용
   - 실행한 테스트와 결과
   - 유지한 계약
   - 결정사항
   - 위험 요소
   - 다음 작업
6. Secret, Token, Password, 전체 MongoDB URI를 기록하지 않는다.

