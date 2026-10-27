# 광고성 푸시 선택 동의 계약

## 핵심 계약

- 앱 푸시 전용이며 문자·이메일 동의를 의미하지 않는다.
- 선택 동의다. 기본 미선택이며 미동의로 가입·로그인·서비스 이용을 차단하지 않는다.
- 현재 버전 기본값은 `marketing-push-v1`, 설정은 `MARKETING_PUSH_CONSENT_VERSION`이다.
- 기존 필수약관·품질 검토 선택 동의와 독립적이다. 버전 변경은 명시적으로 운영한다.
- 캠페인 대상 선정·실제 푸시 발송·Learning Core 연동은 포함하지 않는다.

## 가입 및 갱신 요청

Firebase 신규 가입, Guest 승격, 이메일 가입, 기존 `PUT /api/v1/users/me/consents`에 추가한다.

```json
{
  "isMarketingPushConsented": true,
  "marketingPushConsentVersion": "marketing-push-v1"
}
```

위는 추가 필드만의 예시다. 기존 API 필수 필드도 함께 전달해야 한다.

| 입력 | 신규 가입 | Guest 승격·동의 갱신 |
|---|---|---|
| 누락 또는 null | 미동의 | 기존 상태 유지 |
| false | 미동의 | 철회 |
| true + 현재 버전 | 동의 저장 | 동의 또는 재동의 |
| true + 누락/구버전 | 400 | 400 |

- true에는 서버 현재 버전이 필수다. 불일치 오류는 `MARKETING_PUSH_CONSENT_VERSION_MISMATCH`.
- 버전은 trim 후 100자 이하 영숫자 시작, 영숫자·`.`·`_`·`-`만 허용. 형식 오류는 기존 validation 오류 응답을 따른다.
- false/null에는 버전 생략 가능. 제공한 버전이 현재 버전인지 검증하지 않지만 형식 검증은 적용한다.
- 기존 PUT은 필수약관 필드가 필요하고, 품질 검토 동의 생략은 기존 계약대로 false다. 광고성 토글 갱신 시 유지할 품질 검토 상태도 함께 보낸다.
- 인증된 사용자만 본인 상태를 변경하며 클라이언트 userId를 받지 않는다. 기존 ACTIVE/updatedAt 조건부 쓰기를 유지한다.

## 프론트 조회

가입 전 `GET /api/v1/policies/consents`에서 `marketingPushConsentVersion`을 받는다. 프론트가 표시한 문구의 버전과 함께 제출하며, 동의하지 않은 문구의 버전을 자동 치환하여 재전송하지 않는다.

인증 후 `GET /api/v1/users/me/consents`의 추가 항목:

```json
{
  "marketingPush": {
    "currentVersion": "marketing-push-v1",
    "consented": false,
    "consentedVersion": null,
    "consentedAt": null,
    "requiresConsent": false
  }
}
```

- `consented`는 저장된 동의가 true이고 현재 버전과 일치하며 동의 시각이 존재할 때만 true.
- 구버전 동의나 철회 시 `consented=false`. 이전 버전·시각이 남아 있어도 발송 허용으로 해석하지 않는다.
- `requiresConsent`는 선택 동의라 항상 false. 별도 강제 동의 화면을 띄우지 않는다.
- PUT 응답에는 저장 상태 `marketingPushConsented`, `marketingPushConsentVersion`, `marketingPushConsentedAt`, `marketingPushRevokedAt`을 추가한다. 버전 유효성 판정은 GET 상태를 따른다.

## 저장·호환·운영

- 기존 users.consents에 필드를 추가하며 필드 없는 문서는 미동의. 일괄 동의 마이그레이션이나 새 인덱스/컬렉션 불필요.
- 동의·철회 시각은 서버 기준. 같은 버전 동의/동일 철회 재요청은 시각을 덮어쓰지 않는다.
- 마지막 동의 증빙과 마지막 철회 시각을 저장한다. 전체 변경 이력 감사 저장소는 이번 범위에 포함하지 않는다.
- 필수약관·품질 검토 갱신과 Guest 승격은 기존 광고성 상태를 보존한다. 회원 병합 시 Guest의 동의를 대상 회원에게 새로 부여하지 않는다.
- 테스트 배포만 요청된 범위다. 광고 문구의 법적 검토, 화면 표시·철회 UX, 캠페인 발송 시 최신 동의 검증은 별도 출시 확인 사항이다.
