# Guest 앱 업데이트 전환 검토

## 1. 5줄 결론

- 기존 앱 main은 저장된 Guest 세션을 읽어 재발급하며, 업데이트라는 이유로 무조건 삭제하는 코드는 확인되지 않았다. 실제 APK 업데이트 보존은 미검증이다.
- 신 Identity의 기존 Refresh Session 처리에는 호환 경로가 있지만 동일 사용자·세션 저장소, 만료·폐기 상태, 보안 epoch 등 조건을 만족해야 한다.
- 구서버가 회전을 완료했지만 앱이 응답을 저장하지 못했다면 신서버의 복구 기능으로 구서버 응답을 되살릴 수 없다.
- 기존 앱 main에는 통신 오류 시 구주소로 바꾸는 코드가 없지만, 환경변수 누락 시 공통 주소를 선택하는 설정 fallback은 있다. 신규 SNS 앱 revision은 미확인이다.
- 사용자 결정: Learning Core도 구·신 주소로 분리하고 피드백 `result.updateRequired`를 구서버 true / 신서버 false로 제공한다. 이번에는 계약안만 정리했으며 Learning Core 구현·배포는 하지 않았다.

## 2. 사용자가 반드시 읽어야 하는 내용

네 항목 모두 운영 전환 검증 완료라고 볼 수 없다. 특히 업데이트 직전 재발급 응답 유실은 남아 있는 실패 경로다. 앱에서 요청을 중단하거나 10초 timeout이 발생해도 서버 Transaction이 취소됐다는 뜻은 아니다.

앱 main의 SecureStore 키는 `auth-session.v1`이다. 같은 패키지·서명으로 일반 업데이트했을 때 실제 세션이 유지되는지 기기에서 검증해야 한다. 앱 삭제·데이터 초기화는 별개다. 새 앱이 기존 키를 계속 읽고 저장 형식을 지원하는지도 확인해야 한다.

기존 Refresh Token은 난수이므로 같은 사용자/세션 데이터와 해시 규칙이 필요하다. 별도 테스트 DB에 운영 세션이 저절로 나타나지 않는다. 보호 API에서 기존 Access Token을 바로 사용하려면 JWT 신뢰 설정도 맞아야 한다. `/reissue`의 Refresh Token 조회와 기존 Access Token 서명 검증은 별개다.

## 3. 사용자 결정 및 인계 계약

확정: boolean은 `/reissue`가 아닌 피드백 조회의 `result`에 추가한다. Learning Core도 주소를 분리한다.

권장 구현안(Learning Core 소유, 아직 미구현): 배포별 `APP_UPDATE_REQUIRED` 설정, 기본 false. 구서비스만 명시적으로 true, 신서비스 false. 기존 result 필드는 유지하고 boolean 필드 하나만 추가한다. 요청에 앱 버전이나 userId를 추가하지 않는다.

관련 조회는 `GET /api/v1/exams/{examId}/summary`와 `GET /api/v1/exams/{examId}/questions`다. 요약/문항 피드백 진입 모두 안내하려면 두 result DTO에 동일 boolean을 추가하는 안을 인계한다. 공유 DB 사용자 속성으로 저장할 값이 아니다.

웹뷰는 true이면 업데이트 안내, false이면 정상 진행하도록 구현한다. 배포 전 필드가 없는 구응답의 처리도 프론트와 맞춘다. boolean만으로 업데이트가 강제되지는 않는다. 인증에 실패해 피드백까지 못 오는 사용자는 별도 진입 안내가 필요하다.

## 4. 주요 위험과 미확인 사항

- 신규 SNS 앱 브랜치/빌드와 실제 빌드 환경변수 미확인. 원격 dev는 main보다 오래되어 신규 앱 근거로 사용할 수 없다.
- main은 `/reissue`에 Idempotency-Key를 보내지 않으며 복구 응답의 절대 만료 시각 처리도 확인되지 않는다. 그대로 복구 ON 서버에 연결하면 안 된다.
- 구서버 응답 유실 후 이미 ROTATED인 원 토큰을 보내면 재사용 감지로 활성 세션이 폐기될 수 있다. 복구 OFF도 이 문제 자체를 해결하지 않는다.
- 운영 DB/키/issuer/기능 flag와 실제 업데이트 기기 테스트는 이번 조사에서 조회·변경하지 않았다.
- 일주일 안내는 정책이며, 안내 종료 시점만으로 구앱 API가 자동 차단되는 것은 아니다.

## 5. 다음 검증 순서

1. 신규 SNS 앱 commit 및 빌드 환경을 확보한다. 기존 SecureStore 읽기, single-flight, 동일 요청 ID 재시도, 토큰 동시 저장과 절대 만료를 확인한다.
2. 구/신 Identity의 실제 저장소·해시·사용자 매핑 및 JWT 신뢰를 확인한다. 테스트 DB와 운영 전환용 공유 저장소를 혼동하지 않는다.
3. 일반 업데이트와 회전 응답 저장 전 강제 종료를 각각 재현한다. 응답 유실 시 기존 Guest 기록을 안전하게 인계할 대책 없이 무조건 세션을 새로 발급하지 않는다.
4. 앱 설정에 신 Identity/신 Learning Core 주소를 명시하고, 오류/timeout에도 구주소 요청이 없는지 관찰한다.
5. Learning Core에 boolean을 구현하고 구 true/신 false 직렬화 및 기존 payload 보존 테스트, 실제 웹뷰 안내를 확인한다.

## 6. 부록: 확인 근거와 범위

조사일 2026-09-22. 프론트 원격 main `f6ac7a02a152627efacea172f6b86757d412f0d6`, 원격 dev `e42409eb80e267dff69c61c25f5fe4dd36d66d06`. Identity 원격 main `7f2188df97d2adc5faef8ddec6cb6ffa6ee8b0d4`, 현재 develop 기반 `5f37c13563f4d777e9870e8668f13164ed85fe9c`와 미커밋 작업 트리 비교. 로컬 main은 오래된 ref이므로 운영 근거로 사용하지 않는다.

| 검증 항목 | 코드에서 확인한 사실 | 미확인 |
| --- | --- | --- |
| 세션 보존 | 프론트 `auth-session-storage.ts` 저장 키 유지, `auth-controller.ts` bootstrap 기존 세션 읽기 | 신규 앱 및 APK 업데이트 |
| 구 토큰 수용 | 동일 refresh_sessions 조회, 기존 sessionEpoch 누락은 primitive 0; UserSessionControl은 epoch/폐기 경계 확인 | 실제 데이터·환경 및 main 스키마 전체 E2E |
| 처리 중 요청 | 프론트 rotationPromise/pendingRotationSession은 메모리 상태; 구 ROTATED 응답 복구 불가 | 강제 종료 실기기 재현 및 전환 대책 |
| 구주소 fallback | transport 통신 실패로 주소 변경 없음; service-base-url 공통 환경변수 fallback 있음 | 신앱 코드·배포 env·네트워크 관찰 |

프론트 `src/features/exam/api/exam-grading-summary.ts`, `exam-question-feedback.ts`의 raw getter와 `native-data-bridge.ts`는 result를 웹뷰로 그대로 전달한다. 신규 필드 전달 가능성은 확인했으나 웹뷰 렌더링은 검증하지 않았다.

Identity 근거: [복구 서비스](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/ReissueRecoveryService.java), [보안 경계](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/domain/UserSessionControl.java), [복구 테스트](../../src/test/java/web/tosunsaeng/identity/domain/auth/session/application/ReissueRecoveryServiceTests.java)의 `legacyRotatedSourceStillUsesExistingReusePolicy`. 이 테스트는 구 회전 토큰 재사용 거절을 검증하며 실제 구/신 서버 혼합 배포 성공을 증명하지 않는다.
