# Identity–Learning Core JWT 계약

이 문서는 Identity Service가 발급하는 Access Token을 Learning Core가 검증하고 사용하는 서버 간 계약을 정의한다.

## 사용자 식별 계약

- 실제 `userId`는 UUID 문자열이다.
- JWT의 `sub` Claim은 실제 `userId`다.
- Learning Core는 검증을 마친 JWT의 `sub`에서 사용자 식별자를 얻는다.
- 클라이언트는 `userId`를 별도로 전송하지 않는다.
- 클라이언트가 임의로 전송한 `userId`는 인증 근거로 신뢰하지 않는다.

## Access Token 형식

Signature Algorithm은 RS256이며 Access Token의 기본 TTL은 `PT30M`이다. Identity Service만 RSA Private Key를 보유하며, 외부 서비스에는 Private Key를 배포하지 않는다.

JWT Header에는 다음 값이 필요하다.

| Header | 계약 |
| --- | --- |
| `alg` | `RS256` |
| `typ` | `JWT` |
| `kid` | 필수. 서명에 사용한 RSA Key를 식별하는 값 |

JWT에는 다음 Claim이 반드시 포함되어야 한다.

| Claim | 계약 |
| --- | --- |
| `sub` | UUID 문자열 형식의 실제 `userId` |
| `iss` | Identity Service의 발급자 식별값 |
| `aud` | `tosunsaeng-learning-core` 하나를 포함하는 JSON 배열 |
| `iat` | 토큰 발급 시각 |
| `exp` | 토큰 만료 시각 |
| `jti` | Access Token의 고유 식별값 |
| `scope` | 허용된 접근 범위를 공백으로 구분한 문자열 |

JWT에는 검증과 인가에 필요한 최소 Claim만 포함한다. 이메일, 닉네임 전체, 비밀번호 또는 비밀번호 해시, Refresh Token 같은 개인정보와 자격증명은 포함하지 않는다.

## Public Key 배포와 검증

- Identity Service는 `GET /.well-known/jwks.json`에서 BaseResponse로 감싸지 않은 표준 JWKS를 제공한다.
- Learning Core는 JWKS에서 `kid`에 대응하는 Public Key를 조회해 RS256 서명을 검증한다.
- Learning Core는 서명뿐 아니라 예상한 `issuer`와 `audience`를 모두 검증한다.
- Learning Core가 허용하는 `audience`는 정확히 `tosunsaeng-learning-core`다.
- Learning Core는 매 요청마다 Identity Service의 토큰 확인 API를 호출하지 않고 JWKS 기반으로 토큰을 검증한다.
- RSA Private Key는 Identity Service 밖으로 공유하지 않는다.

현재 JWKS에는 단일 Active Key의 Public Key만 제공한다. 향후 Key Rotation 시에는 새 Active Key로 발급을 전환한 뒤에도 이미 발급된 Access Token의 최대 유효 기간과 캐시 정책을 고려해 이전 Public Key를 JWKS에 일정 기간 유지해야 한다.

JWKS endpoint의 배포 호스트와 허용할 `issuer` 값은 환경별 설정으로 관리하며, 실제 Key나 Secret을 이 문서에 기록하지 않는다.

## 클라이언트 요청

클라이언트는 보호된 Learning Core API를 호출할 때 다음 Header를 사용한다.

`Authorization: Bearer <ACCESS_TOKEN>`

클라이언트는 별도 Header, Query Parameter 또는 Request Body로 `userId`를 보내지 않는다. Learning Core는 검증된 Access Token의 `sub`만 실제 사용자 식별 근거로 사용한다.

## Python AI 경계

Python AI 연동의 `user_id`는 인증 사용자 ID가 아니라 기존 시험 식별자 의미를 유지한다.

| 연동 경로 | `user_id` 값 |
| --- | --- |
| Learning Core → Python AI | `examId` |
| Python AI → AI Callback | `examId` |

- Python AI의 `user_id`는 실제 `userId`가 아니라 `examId`다.
- AI Callback의 `user_id`도 `examId`다.
- 실제 `userId`를 Python AI 서버나 AI Callback payload로 보내지 않는다.
