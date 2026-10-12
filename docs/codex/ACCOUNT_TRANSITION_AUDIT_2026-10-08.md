# 구·신 서버 계정·DB·세션 및 알림 전환 점검

> 최신 후속: 알림 조건은 LC3fda35b에 구현·test19 배포 완료. 아래 미구현/구 writer 종료/과거 시험 제외 설명은 조사 당시 이력이며 현재 정책이 아니다. 새 실측·구 binary 호환 문제·신규 운영 설정표는 [후속 점검](NEW_PRODUCTION_CONFIGURATION_MATRIX_2026-10-08.md)을 우선한다.

> 후속 사용자 결정(2026-10-08): 구앱 완료 후 신앱 전환 시 구 제출 미관측으로 일일 알림이 발송될 가능성은 허용한다. 따라서 아래 구 writer 추적/종료 조건은 현재 코드의 조건이지 출시 정책상 필수 요구가 아니다. 구서버 추적이나 전환 당일 일괄 제외는 요구하지 않는다. trackingReadyAt 및 과거 불확실 시험 제외 조건은 새 정책에 맞춰 별도 검토해야 하며 아직 변경하지 않았다. 탈퇴·동의·소유권·중복 방지 조건은 유지한다.

## 1. 5줄 결론

1. 현재 배포 설정과 소스 기준으로 전환 완료 판정은 보류다. 운영 데이터 직접 대조·실기기 업데이트는 미검증이다.
2. 기존 Identity/LC와 테스트는 DB명·JWT issuer가 다르다. 테스트 설정 복사는 기존 계정·학습 기록·세션 승계를 제공하지 않는다.
3. 기존 refresh 세션 호환 경로는 있지만 데이터·해시·epoch·만료 조건과 앱 재시도 계약을 함께 만족해야 한다([복구 서비스](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/ReissueRecoveryService.java)).
4. 알림 출시가 추가됐으며 테스트는 API/추적 ON, 발송 OFF/dry-run ON이다. 구 제출 writer와 병행하는 발송 준비가 핵심 미확인 사항이다.
5. 다른 AI의 진행 설정은 수정하지 않고 읽기 점검만 수행했다. 새 운영 서비스/앱 빌드/실제 DB 인덱스 검증은 남아 있다.

## 2. 사용자가 반드시 읽어야 하는 내용

- 계정 승계에는 기존 UUID와 users/refresh_sessions 및 보안 상태를 일관되게 유지해야 한다. 별도 DB 복사 후 양쪽 쓰기를 계속 허용하면 이후 변경이 자동 동기화되지 않는다. 같은 DB를 쓰더라도 구 코드가 병합·탈퇴 차단과 epoch 경계를 준수하는지 검증이 필요하다.
- Access 신뢰와 Refresh 조회는 별개다. 새 주소라는 이유만으로 JWT issuer를 치환하면 기존 Access가 거절될 수 있다. 기존 issuer/JWKS 유지 또는 명시적인 이행 신뢰 정책이 필요하며 검증 비활성화는 대안이 아니다.
- 새 앱은 기존 SecureStore 세션과 설치 정보를 읽어야 한다. 과거 앱 조사 결과를 신규 SNS 빌드의 검증으로 대신할 수 없다.
- 알림 발송 준비 시각은 모든 구 제출 writer가 빠진 뒤의 시각이어야 한다는 소스 계약이다. 구서버를 계속 열 경우 구서버에도 동일 제출 추적이 있는지 검증하거나 실제 발송을 뒤로 미뤄야 한다. 단순히 flag를 켜는 것으로 해결되지 않는다.

## 3. 사용자가 결정해야 하는 사항

- 신규 운영 주소 및 기존 DB 공유/일관된 이관 방식. 테스트 DB를 운영 데이터 원본으로 사용하지 않는다.
- 구서버 병행 중 알림 추적 호환을 보장할지, 구 제출 writer 종료 후 첫 온전한 KST 추적일을 확보해 발송할지.
- 다른 AI의 결과 검토를 위해 적용 대상 서비스/revision 또는 변경 파일을 특정해야 최종 변경안까지 판정 가능하다. 이번 결과는 현재 확인 가능한 배포·로컬 소스 기준이다.

## 4. 주요 위험과 미확인 사항

- 운영 DB 문서/인덱스 및 기존 배포 이미지의 소스 일치, 신규 앱 코드/실기기 저장소 보존, 실계정 승계 E2E 미확인.
- 구서버에서 회전 후 유실된 응답을 신서버 복구가 재생성하지 못하며 원 토큰 재사용은 활성 세션 폐기로 이어질 수 있다. 기존 [전환 검토](../contracts/guest-app-update-transition-review.md) 참조.
- 알림 일일 기기 중복 방지는 동일 DB의 delivery key/unique index를 전제로 한다. DB 복제 후 sender 두 개 운영은 전역 중복 방지를 보장하지 않는다.
- 알림은 과거 시험의 submissionCompletedAt이 없고 readyAt 이전 생성/생성시각 누락이면 해당 소유자 발송을 보수적으로 제외한다. 기존 사용자 대다수 제외 여부를 집계해야 한다. 임의 시각 backfill 금지.
- 구 LC에 새 탈퇴·병합 deny가 없으면 새 기능 이후 구주소 접근/쓰기 안전성을 보장할 수 없다. 운영 flag 미표시만으로 기능 부재를 확정하지 않았으며 구 이미지 코드 대조가 필요하다.

## 5. 현재 작업과 직접 관련된 설명

읽기 점검 다음 단계는 운영 컬렉션/인덱스·구 writer 호환성 대조와 신규 앱 빌드 검증이다. 이후 지정 테스트 계정으로 일반 업데이트, refresh 응답 유실/동시 재발급, Guest 병합·기록 소유권, 탈퇴 후 두 주소 차단, 알림 등록/로그아웃/권한 철회/중복 발송을 검증한다. 실제 사용자 알림 발송이나 DB 보정은 이번 요청에서 수행하지 않았다.

## 6. 부록 — 실측 및 소스 근거

### ECS 읽기 조회(2026-10-08)

| 대상 | revision | DB 환경변수 | issuer |
| --- | --- | --- | --- |
| 기존 Identity | 23 | to-teacher-identity | identity-staging 도메인 |
| 기존 LC | 23 | to-teacher-app | identity-staging 도메인 |
| 테스트 Identity | 34 배포 중 | to-teacher-identity-test | identity-test 도메인 |
| 테스트 LC | 18 | to-teacher-learning-core-test | identity-test 도메인 |

- 서울 대상 클러스터 목록은 기존/test Identity·LC와 AI 2개, 총 6개다. 별도 신규 운영 서비스는 이 목록에서 식별되지 않았다.
- 테스트 Identity AUTH_REISSUE_RECOVERY_ENABLED/GUEST_MERGE_ENABLED/USER_WITHDRAWN_PUBLISHER_ENABLED=true. 테스트 LC merge consumer/writer 및 withdrawal consumer/deny=true. 실제 운영 대상의 신규 설정과 구분한다.
- 테스트 LC SPRING_APPLICATION_JSON의 app.notifications.tracking-enabled/api-enabled=true, sending-enabled=false, dry-run=true, tracking-ready-at=2026-10-07T05:11:49.181835+00:00. 운영 대상 JSON에서는 해당 항목 미표시(최종 런타임 값 확정 아님).
- 직전 사용자 요청으로 테스트 버전 네 항목을 1.0.0으로 한 revision34 배포 요청이 이미 실행됨. 이번 중간 조회는 신규 running1/pending0 IN_PROGRESS와 구 revision33 running1, 공개 API 양 플랫폼은 아직 최소9.0.0/최신9.1.4였다. 완료로 오인하지 않으며 다른 AI 위임 이후 재변경 없음.

### 소스 근거

- Identity 57192f45: RefreshTokenHasher는 SHA-256 + URL-safe base64(no padding), UserSessionControl은 epoch/폐기 경계 검증. AuthController는 복구 ON 시 Idempotency-Key 필수를 명시한다. 이는 새 앱이 준수한다는 증거가 아니다.
- LC 로컬 a77a53f: /Users/msde76/app-back-end-learning-core/src/main/java/web/tosunsaeng/domain/notification/NotificationStartupValidator.java 는 컬렉션 6개와 인덱스 12개(exam_sessions 포함), transaction 지원 및 5개 lifecycle gate를 검사한다.
- 동일 폴더 NotificationProperties.java 의 trackingReadyAt 주석은 모든 구 submit writer 종료 및 온전한 KST 하루 조건을 명시한다. NotificationStore.java 는 기존 불확실 시험 소유자 제외 및 일일 installation별 claim을 구현한다.
- /Users/msde76/app-back-end-learning-core/docs/contracts/notification-device-api.md: installationId+X-Installation-Secret 보존, 로그인/FCM 갱신/foreground PUT, 로그아웃 전 DELETE. 일일 알림 MEMBER만 대상, 채점 완료 알림은 이 계약의 구현 범위 아님.
- 설정 조회/소스 대조만 수행. Gradle은 제품 변경 없어 미실행. 운영 DB 직접 스캔/인덱스 변경/재배포/알림 발송 없음.
