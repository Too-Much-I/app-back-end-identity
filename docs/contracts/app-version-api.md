# 최신 출시 앱 버전 조회 — TMI-195

## 계약

`GET /api/v1/app/version?platform=android` 또는 `?platform=ios`

인증 없이 호출한다. Authorization 헤더는 보내지 않는다. 잘못된 Bearer 토큰을 보내면 기존 보안 필터가 401을 반환한다. 공개 허용은 이 GET 경로에 한정된다.

```json
{
  "isSuccess": true,
  "code": "SUCCESS",
  "message": "요청에 성공했습니다.",
  "result": {
    "platform": "android",
    "latestVersion": "1.2.0"
  }
}
```

예시 버전은 실제 출시값이 아니다. platform은 소문자 `android` 또는 `ios` 하나만 허용한다. latestVersion은 출시 버전 문자열이며 빌드 번호·OTA 버전·최소 지원 버전이 아니다.

| 조건 | HTTP | code |
| --- | --- | --- |
| 조회 성공 | 200 | SUCCESS |
| platform 누락/빈 값/잘못된 값/중복 | 400 | INVALID_REQUEST |
| 요청한 플랫폼의 버전 미설정 | 503 | APP_VERSION_UNAVAILABLE |

오류도 기존 BaseResponse를 사용한다. 컨트롤러의 성공·400·503 응답에는 `Cache-Control: no-store`를 적용한다. 한 플랫폼이 미설정이어도 다른 플랫폼은 정상 조회할 수 있다. 503을 업데이트 필요 신호로 해석하지 않는다.

## 설정과 갱신

- `APP_ANDROID_LATEST_VERSION`: Android 최신 출시 버전
- `APP_IOS_LATEST_VERSION`: iOS 최신 출시 버전
- 두 값 모두 기본값은 빈 문자열. 미설정 상태로 서버 기동은 가능하지만 해당 플랫폼 조회는 503이다.
- 설정 형식은 숫자 `major.minor.patch`, 최대 32자이며 각 숫자에는 불필요한 선행 0을 허용하지 않는다. 예: `1.10.0`, `2.0.1`. 앞뒤 공백은 제거한다.
- 비어 있지 않은 잘못된 설정은 기동 실패로 발견한다. 프리릴리스 접미사, build metadata, `v` 접두사는 지원하지 않는다.
- 실제 스토어에 공개된 버전을 확인한 뒤 환경변수를 변경하고 서버를 재배포/재시작한다. DB·관리자 API 및 스토어 자동 조회는 없다.
- Android/iOS 출시 시점과 단계적 배포 대상 차이를 확인하고 값을 갱신한다. 서버 코드 변경이나 새 인증 키는 필요 없다.

## 책임과 검증 범위

서버는 최신 버전만 제공한다. 설치 버전과의 비교, 업데이트 안내·차단 및 스토어 이동은 프론트 책임이며 이 API는 해당 정책을 제공하지 않는다. 버전은 단순 문자열 순서로 비교하지 않는다. 기존 구앱이 이 API를 자동으로 호출하게 되지는 않는다.

- 구현: `domain/appversion` 및 SecurityConfig의 exact GET 공개 설정
- 테스트: AppVersionTests의 플랫폼·설정·오류 계약, SecurityIntegrationTests의 공개 GET 및 기존 인증 경계
- 배포 전: 플랫폼별 실제 출시 버전 설정 및 무인증 GET 응답 확인 필요. 이번 구현은 실제 배포를 포함하지 않는다.
