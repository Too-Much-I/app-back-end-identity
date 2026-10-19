# 운영 인증 호환 및 로컬 백업 점검

## 1. 5줄 결론

1. 사용자 승인으로 운영 Identity/LC57컬렉션,24228문서를 로컬 제한 폴더에 논리 백업했다. 파일 무결성·건수·권한 검증은 통과했다.
2. 가동 중 백업이므로 동일 시점 snapshot/PITR 보장이나 실제 복원 시험 완료는 아니다. 백업 전후 건수 일치가 동시 update 부재를 증명하지 않는다.
3. 구 Identity7f2188d는 새 병합 enum과 응답복구 metadata를 모르며, ROTATED 재시도에 활성 세션 일괄 폐기를 수행한다.
4. 구 LC89defb7은 JWT 검증은 하지만 새 탈퇴/병합 deny 필터가 없다. 구주소를 통한 만료 전 접근 위험은 UI 피드백 차단으로 해결되지 않는다.
5. 사용자는 점검 결과 설명만 선택했다. 호환 코드 수정·배포·서버 기동·운영 플래그 변경 없이 중단한다.

## 2. 사용자가 반드시 읽어야 하는 내용

### 실제 문제와 발생 조건

| 항목 | 확인된 구현 | 영향/조건 |
| --- | --- | --- |
| 구 Identity의 refresh 재시도 | ROTATED이면 사용자 활성 세션을 폐기, 새 응답복구 판별 없음 | 신서버 회전 후 옛 토큰이 다른 기기/재시도로 구주소에 도달하면 정상 신규 세션까지 폐기될 위험. 정상 단일 기기 업데이트마다 발생한다는 뜻은 아님 |
| 새 상태 읽기 | 구 UserStatus에 MERGED 없음, RevocationReason에 GUEST_MERGED 없음 | 병합된 사용자/세션을 구 binary가 읽으면 enum 역직렬화 오류 가능. 운영에서 실제 사용자 요청으로 재현하지 않음 |
| 구 LC의 접근 차단 | JWT 서명/issuer/audience/sub/만료 검증만 존재, 신 deny 필터 없음 | 신서버에서 탈퇴/병합해도 아직 유효한 토큰으로 구주소를 직접 호출할 때 차단 보장 없음. 토큰 만료 이후까지 영구 허용된다는 뜻은 아님 |
| 구주소에 최신 코드 단순 배치 | recovery ON에서 정확한 UUID v4 Idempotency-Key 하나 필요 | 구앱 무헤더 재발급을 그대로 보내면 INVALID_REISSUE_REQUEST_ID. 공통 최신 코드도 호환 경로 검증 없이 단순 교체하면 안 됨 |

이는 사용자가 수용한 좁은 채점 callback/병합 동시 저장 경합이나 구제출 미추적으로 인한 알림 가능성과 별개의 보안·세션 문제다. 신앱을 설치한 기기 하나만 보고 다른 구앱 기기/진행 요청/구주소 직접 요청이 없다고 가정하지 않는다.

## 3. 사용자가 결정해야 하는 사항

- 현재 결정: **점검 결과만 먼저 설명**, 코드·테스트 수정안 구현이나 배포는 승인하지 않음.
- 후속 선택은 구주소에 공통 코드의 구앱 호환 경로를 준비할지, 구 binary에 필요한 최소 보안 호환을 이식할지다. 어느 안도 이번에 자동 선택/실행하지 않았다.
- 복원 검증을 하려면 별도 격리 DB와 데이터 접근 범위/보관·삭제 정책을 정해야 한다. 기존 운영 DB에 restore하지 않는다.

## 4. 주요 위험과 미확인 사항

- 백업은 canonical Extended JSON lines와 collection options/index metadata다. mongodump BSON archive나 oplog 포함 dump가 아니며 기존 서버 쓰기를 중단하지 않았다.
- 실제 restore는 미실행. EJSON 파싱·SHA256·문서 수 검증 성공만으로 서비스 간 일관성이나 복구 성공을 단정하지 않는다.
- 백업 파일은 접근권한으로 제한했지만 별도 파일 암호화는 하지 않았다. macOS FileVault 여부는 미확인이다. 외부 공유·업로드·Git 포함 금지, 같은 OS 사용자/관리자의 접근까지 막는 방식은 아니다.
- ECS Secret 원문/DB 접속 host 동일성, 신규 운영 encryption keyring·Firebase 권한·AI callback/queue namespace, 실제 출시 앱 빌드는 이번에 최종 검증하지 않았다. 운영 설정표의 조건부 값은 배포 확정값이 아니다.
- 신규 운영 서비스는 기동하지 않았다. 저장된 task definition 확인과 실제 running task digest/최종 runtime 설정 검증은 구분한다.

## 5. 현재 작업과 직접 관련된 설명

### 운영 설정 확인

- AWS 콘솔의 구 LC task23은 image89defb7, 운영 DB명to-teacher-app, prod profile, JWT issuer/JWKS는 identity-staging, audience tosunsaeng-learning-core, AI 주소는 기존 tosunsaeng-ai:8000, APP_UPDATE_REQUIRED=false를 표시했다. 이번에 이 값을 바꾸지 않았다.
- 운영 주소 신설만으로 issuer/key를 테스트 값으로 바꾸면 기존 Access 신뢰가 이어지지 않는다. 기존 운영 DB·운영 issuer/key 연속성 유지안과 [JWT 계약](../contracts/identity-learning-jwt.md)을 기준으로 별도 기동값 확정 필요.
- 테스트 LC의 localhost9 AI 주소는 직전 확인된 잘못된 운영 복사 후보다. 이번에는 테스트 task를 다시 수정하거나 신규 AI를 만들지 않았다.
- CloudShell에서 서비스/태스크의 allowlist 환경변수 조회를 요청했지만 이 보고서는 그 요청의 최종 출력 확인을 근거로 삼지 않는다. 위 값은 실제 열린 task23 UI 확인 범위다.

### 로컬 백업

- 위치: `/Users/msde76/Backups/tosunsaeng-preprod-2026-10-08/logical-2026-10-08T07-19-30-598Z`.
- 저장소 밖 폴더, 디렉터리0700/파일0600. 접속 URI/키를 백업 실행 코드나 작업 기록에 넣지 않았다. 백업 내용은 사용자 승인된 DB 문서 자체로만 로컬 저장했고 출력하지 않았다.
- Identity31컬렉션/1389문서/916346bytes, LC26컬렉션/22839문서/115765137bytes. 합계24228문서/116681483bytes, data/metadata 파일114개 및 manifest/완료 표식.
- canonical EJSON의 Long 표기와 문서별 EJSON.parse 성공을 확인했다. 독립 검증으로 모든 데이터 파일 JSON line 수·바이트 수·SHA256, metadata SHA256/JSON 파싱, 디렉터리/파일 권한을 대조했다. 모든 컬렉션의 백업 전/후 count 일치, namespace 목록 변화 없음.
- 임시 실행 도구: `/private/tmp/tosunsaeng-logical-backup-20261008.cjs`, 검증 도구: `/private/tmp/tosunsaeng-verify-backup-20261008.cjs`. DB 쓰기 메서드는 사용하지 않았다. manifest.complete=true가 완성 여부의 기준이며 EXPORT_FINISHED는 진행 표식의 이름 변경이다.

## 6. 부록 — 코드와 검증 근거

- 구 Identity `git show 7f2188d:src/main/java/web/tosunsaeng/identity/domain/auth/application/TokenReissueService.java`: ROTATED 검사와 revokeActiveSessionsForReuse, saveAll 경로.
- 구 enums: 같은 commit의 domain/user/domain/enums/UserStatus.java와 domain/auth/domain/enums/RevocationReason.java. 신 enums와 대조해 MERGED/GUEST_MERGED 부재 확인.
- 현행 [TokenReissueService](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/TokenReissueService.java:87)는 rotationResponseId 존재 시 legacy 처벌로 넘어가지 않지만 이 방어는 구 image에 없다.
- 현행 [ReissueRecoveryService](../../src/main/java/web/tosunsaeng/identity/domain/auth/session/application/ReissueRecoveryService.java:188)는 단일 lowercase UUIDv4 request ID를 강제한다. [recovery runbook](../contracts/refresh-token-response-recovery-stage-9-runbook.md)도 이전 binary rollback 안전성을 보장하지 않는다고 명시한다.
- LC 구 commit89defb7의 global/config/SecurityConfig.java에는 현행 [두 deny 필터 설치](/Users/msde76/app-back-end-learning-core/src/main/java/web/tosunsaeng/global/config/SecurityConfig.java:71)가 없다. 실제 운영 토큰을 사용한 우회 재현은 하지 않았다.
- 테스트: 임시 백업 스크립트 node --check, 실제 백업 후 독립 검증 성공(114파일/24228문서), git diff --check. 제품 코드 미변경이므로 Gradle 재실행 없음. 직전 DDL 작업의 테스트 결과와 구분한다.
- 변경 파일은 이 보고서, CURRENT_STATE, WORKLOG, 운영 설정표의 최신 안내뿐이다. 백업과 임시 도구는 저장소 밖에 둔다. 기존 동시 변경 보존, 예상 밖 제품 변경 없음, commit/push/배포 없음.
