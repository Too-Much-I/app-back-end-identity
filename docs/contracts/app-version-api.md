# 최소 지원·최신 출시 앱 버전 조회

## 5줄 결론

1. 기존 공개 GET 응답에 `minimumVersion`을 추가한다. 경로·인증·응답 envelope는 유지한다.
2. 설치 버전이 최소 미만일 때만 강제 업데이트한다.
3. 최소 이상·최신 미만은 선택적 권장 업데이트이며 최신 이상은 정상 이용한다.
4. 플랫폼별 최소·최신 값은 독립 설정한다. 최신 출시 때 최소값을 자동으로 올리지 않는다.
5. 코드 구현과 실제 배포는 별개다. 플랫폼별 설치 가능 여부 확인 후 설정한다.

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
    "latestVersion": "1.2.0",
    "minimumVersion": "1.1.0"
  }
}
```

예시 버전은 실제 출시값이 아니다. platform은 소문자 `android` 또는 `ios` 하나만 허용한다. latestVersion은 출시 버전 문자열이며 빌드 번호·OTA 버전·최소 지원 버전이 아니다.

minimumVersion은 호환성·보안상 지원하는 최소 앱 버전이다. 앱은 아래 기준을 숫자 major/minor/patch 순서로 비교한다(문자열 사전순 비교 금지).

| 설치 버전 조건 | 앱 동작 |
| --- | --- |
| 설치 < minimumVersion | 닫기·나중에 없는 강제 업데이트 화면 |
| minimumVersion <= 설치 < latestVersion | 나중에 가능한 권장 업데이트 또는 안내 생략 |
| latestVersion <= 설치 | 정상 이용 |

| 조건 | HTTP | code |
| --- | --- | --- |
| 조회 성공 | 200 | SUCCESS |
| platform 누락/빈 값/잘못된 값/중복 | 400 | INVALID_REQUEST |
| 요청한 플랫폼의 최소·최신 중 하나라도 미설정 | 503 | APP_VERSION_UNAVAILABLE |

오류도 기존 BaseResponse를 사용한다. 컨트롤러의 성공·400·503 응답에는 `Cache-Control: no-store`를 적용한다. 한 플랫폼이 미설정이어도 다른 플랫폼은 정상 조회할 수 있다. 503을 업데이트 필요 신호로 해석하지 않는다.

## 설정과 갱신

- `APP_ANDROID_LATEST_VERSION`: Android 최신 출시 버전
- `APP_IOS_LATEST_VERSION`: iOS 최신 출시 버전
- `APP_ANDROID_MINIMUM_VERSION`: Android 최소 지원 버전
- `APP_IOS_MINIMUM_VERSION`: iOS 최소 지원 버전
- 네 값 모두 기본값은 빈 문자열. 미설정 상태로 서버 기동은 가능하지만 해당 플랫폼의 최소·최신 중 하나라도 없으면 조회는 503이다. 최소값을 최신값으로 자동 보완하지 않는다.
- **기존 최신 버전만 설정한 배포도 최소 버전을 추가해야 한다.** 새 코드 배포 전 환경변수 두 개를 함께 준비하고 배포 후 두 플랫폼의 응답을 확인한다.
- 설정 형식은 숫자 `major.minor.patch`, 최대 32자이며 각 숫자에는 불필요한 선행 0을 허용하지 않는다. 예: `1.10.0`, `2.0.1`. 앞뒤 공백은 제거한다.
- 비어 있지 않은 잘못된 설정은 기동 실패로 발견한다. 프리릴리스 접미사, build metadata, `v` 접두사는 지원하지 않는다.
- 플랫폼별 minimumVersion > latestVersion이면 기동 실패한다. 같은 값은 허용한다.
- 실제 스토어에 공개된 버전을 확인한 뒤 환경변수를 변경하고 서버를 재배포/재시작한다. DB·관리자 API 및 스토어 자동 조회는 없다.
- Android/iOS 출시 시점과 단계적 배포 대상 차이를 확인하고 값을 갱신한다. 서버 코드 변경이나 새 인증 키는 필요 없다.

## 책임과 검증 범위

서버는 최소·최신 버전을 제공한다. 설치 버전과의 비교, 업데이트 안내·차단 및 스토어 이동은 프론트 책임이다. 이 API 자체가 다른 서버 API 요청을 차단하지는 않는다. 기존 구앱이 이 API를 자동으로 호출하게 되지는 않는다. 네트워크 실패·503은 강제 업데이트의 근거가 아니며 별도 재시도/오류 UX를 사용한다.

## 배포 전 결정사항과 위험

- 이번 출시 예정 1.1.0을 최소값으로 지정할지는 실제 최소 호환 버전을 확인한 뒤 결정한다. 두 값을 1.1.0으로 시작할 수 있지만, 실제 사용자가 설치할 수 있는 시점에만 적용한다.
- 이후 1.1.1 출시 시 최신만 1.1.1로 변경하고 호환되는 최소 1.1.0은 유지할 수 있다.
- Android/iOS 공개 시점과 단계적 배포를 각각 확인한다. 강제 대상 사용자가 설치할 수 없는 버전을 최소값으로 지정하면 앱 진입이 막힌다.
- 구서버의 업데이트 안내 boolean과 약 1주 유예는 별도 전환 정책이다. 최신값 상승만으로 유예 종료나 강제 전환을 자동 실행하지 않는다.
- 프론트 강제 화면·스토어 URL·조회 실패 UX 구현과 실기기 검증은 별도 필요하다. 이번 변경은 runtime 설정·배포를 포함하지 않는다.

- 구현 근거: [AppVersionService](../../src/main/java/web/tosunsaeng/identity/domain/appversion/AppVersionService.java), [AppVersionResponse](../../src/main/java/web/tosunsaeng/identity/domain/appversion/AppVersionResponse.java), [설정](../../src/main/resources/application.yml), SecurityConfig의 exact GET 공개 설정
- 테스트: AppVersionTests의 플랫폼·설정·범위·오류 계약, SecurityIntegrationTests의 공개 GET 및 기존 인증 경계, OpenApiSharingTests의 응답 필드 검증
- 배포 전: 플랫폼별 실제 출시 버전 설정 및 무인증 GET 응답 확인 필요. 이번 구현은 실제 배포를 포함하지 않는다.
