# Identity Swagger 공유본

1. `identity-swagger.zip`을 풀고 폴더의 `index.html`을 브라우저로 엽니다.
2. 인터넷·서버 기동 없이 API 요청/응답과 인증 조건을 볼 수 있습니다.
3. `identity-openapi.json`은 OpenAPI 지원 도구로 가져올 수 있습니다.
4. 이 공유본은 읽기 전용이며 API 실행 버튼을 제공하지 않습니다.
5. 서버 주소와 실제 배포·기능 활성화 여부는 백엔드 담당자에게 확인합니다.

학습 기록만 삭제하는 기능은 Learning Core 담당 미확정 요구사항이며 Identity API로 포함하지 않았습니다. Guest 가입 재개의 성공 타입은 `ENROLLMENT_REQUIRED`와 `MERGE_REQUIRED`입니다. MEMBER의 SNS 추가 연결에서는 별도 `ALREADY_LINKED`가 유지됩니다.

현재 Identity Controller 전체 **23개 경로 / 24개 HTTP 작업**을 포함하며 자동 테스트가 실제 Controller 등록 목록과 대조합니다. Actuator health, Swagger 자체 리소스는 업무 API 목록에서 제외합니다. 이전 공유본에서 회색으로 보였던 `POST /api/v1/auth/firebase/providers/relink/prepare`는 폐기 API여서 서버 라우트와 Swagger에서 제거했습니다. `/providers/link/prepare → start → complete`를 사용하세요. `users/me`의 하위 호환 `provider` 필드는 별개로 유지되며 계정 구분은 `accountType`을 사용합니다.

각 API의 **Responses → Example Value**에서 응답 예시를 볼 수 있습니다. 예시 선택 목록에서 Guest 가입 재개/merge, MEMBER·Guest 프로필, Provider 연결 상태와 주요 오류를 선택하세요. 모든 날짜·ID·정책 버전·유효 기간은 예시이며 실제 응답값을 우선합니다. 인증 문자열과 JWKS modulus는 사용 불가능한 자리표시자입니다. Provider `unlink`는 **202 접수**이고 완료가 아니며, `linkAllowed=true`는 최초 start 성공 예시에만 나옵니다. 예시가 모든 오류·상태 조합을 열거하는 것은 아닙니다.

배포 서버에서 직접 보려면 전달받은 base URL 뒤에 `/swagger-ui.html`을 붙입니다. OpenAPI JSON 경로는 `/v3/api-docs`이며 서버에서 `SWAGGER_ENABLED=true`여야 합니다. 로컬의 localhost URL은 다른 사람에게 공유 가능한 서버 주소가 아닙니다.

저장소에서 갱신: `./gradlew shareSwagger`. 결과 ZIP은 `build/distributions/identity-swagger.zip`이며 API가 바뀌면 다시 생성합니다. Mock Repository와 테스트 프로필을 사용하므로 운영 DB나 Firebase를 호출하지 않습니다. 생성물의 서버 주소는 예시 주소이고 인증정보는 포함하지 않습니다.

Swagger UI JavaScript/CSS는 프로젝트의 `org.webjars:swagger-ui` 의존성에서 가져옵니다. Swagger UI의 Apache-2.0 원문은 `LICENSE-swagger-ui.txt`에 포함했습니다. 출처: [Swagger UI v5.18.2](https://github.com/swagger-api/swagger-ui/tree/v5.18.2). 생성 시점은 `GENERATED_AT.txt`에 기록됩니다.
