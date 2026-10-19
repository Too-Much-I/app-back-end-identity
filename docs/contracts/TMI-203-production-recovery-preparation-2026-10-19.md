# TMI-203 운영 복구 연결 준비 — 실환경 확인과 로컬 보완

## 1. 5줄 결론

1. 2026-10-19 운영 Identity prod18 단일 COMPLETED, desired/running/pending 1/1/0을 직접 읽었다.
2. 복구 컬렉션3개와 stream4필드 미준비를 읽기 전용으로 재확인했다. DB 변경은 하지 않았다.
3. 현재 task는8081만 연결하고 TLS mount/entrypoint override가 없다. 복구 TLS 파일 주입과 환경변수 alias를 로컬 보완했다.
4. 전체 테스트2177개 중2143통과/34skip/실패0/오류0. 운영 인증·TLS·복구 성공 증거는 아니다.
5. commit/push·운영 DB·Secret·권한·배포 변경 없음. 원천 자동삭제OFF 및 기존 운영 경로 유지.

## 2. 반드시 읽을 내용

Billing 인계의 설정만 바로 켜면 필요한 DB와 TLS 파일이 없어 기동 실패할 수 있다. 먼저 이번 이미지 보완을 반영하고, 검증된 DB 보강과 전용 연결 준비 뒤 활성화한다. 기존 source/capture를 OFF로 되돌리지 않는다.

- [시작 스크립트](../../docker-entrypoint.sh): 전용 `WITHDRAWAL_RECOVERY_TLS_CERTIFICATE_PEM`/`WITHDRAWAL_RECOVERY_TLS_PRIVATE_KEY_PEM`에서 task-private 파일을 생성한다. 파일0600·디렉터리0700, raw 환경값 unset, 실패 시 생성 파일 정리. 기존 FILE 입력도 지원하며 PEM/FILE 동시 입력은 거절한다. 실제 PEM 의미 검증은 JVM TLS 기동 책임이다.
- [설정](../../src/main/resources/application.yml): `WITHDRAWAL_RECOVERY_RUNTIME_ENABLED`, `WITHDRAWAL_RECOVERY_SNAPSHOT_JOBS_ENABLED`, `WITHDRAWAL_RECOVERY_CURSOR_ACTIVE_KEY`, `WITHDRAWAL_RECOVERY_CURSOR_KEYRING_JSON` 명시 alias 추가. enabled 기본값false 유지.
- [회귀](../../src/test/java/web/tosunsaeng/identity/deployment/DockerEntrypointTests.java): TLS 파일 생성/권한·원문 제거·누락 실패 정리·기존 파일 보존·중복 입력 거절 검증.

## 3. 사용자가 결정해야 하는 사항

- 저장소 규칙상 commit/push는 사용자 수행이다. 이번 이미지 보완을 Codex가 올리려면 별도 한정 예외 승인이 필요하다. 과거 예외를 재사용하지 않는다.
- 전용 origin/connector/target group·ALB/SG 접근 확대, 새 cursor/TLS Secret 및 실행 역할 읽기 권한은 정확 적용안을 확정하고 적용 직전 승인한다. 기존 workload RSA는 재생성하지 않는다.
- Billing 최초 동기화·구매 제한 점검창/control 전환과 자동삭제는 이번 Identity 준비에 포함하지 않는다.

## 4. 위험과 미확인

운영 DB 전체 index 옵션 검증/verifyMigration, 조건부 이관 runner, 실제 인증서·cursor 키·권한과 최종 바인딩, ALB 제한 경로 및 TLS/JWT probe는 남아 있다. 기존 key pair 비교는 Billing 인계 결과로 이번 재실행하지 않았다. 실제 이미지 내부 entrypoint 파일을 추출 검산하지 않았고 현재 저장소/배포 task를 대조했다. 코드 변경만으로 준비 완료를 주장하지 않는다.

## 5. 실행 순서

1. 로컬 보완의 검토·원격 이미지 반영 준비. 일반 앱 인증/외부 API/body/fixture 변경 없음.
2. 최신 DB/배포 재확인 → 기존 index 보존하며 누락9개·컬렉션3개 보강 → stream 기존 식별자/head/floor/state 조건부4필드 보강 → 전체 검증. writer 경합/복구 이력 출현 시 멈춘다.
3. 전용 cursor/TLS Secret·최소 읽기 권한·ALB/SG/target 준비 및 승인. 실제 origin/port를 Billing에 회신한다.
4. runtime/snapshot worker/HTTP만 활성화하고 정상/오류 인증·앱포트 우회 차단·로그인/push 회귀 확인. snapshot worker의 자체7일/120일 정리는 포함되지만 source-jobs는OFF 유지.
5. Billing 후속 동기화는 별도 승인 범위다. 임의 baseline/checkpoint 설치하지 않는다.

## 6. 상세 확인 근거

| 대상 | 이번 직접 관측 |
| --- | --- |
| Identity | prod18 COMPLETED, count1/1/0, 서비스8081 target 하나, task port8081만 |
| Identity 복구 | HTTP/source jobs false, cursor/TLS Secret 참조 없음, recovery 공개 JWKS 참조 존재 |
| DB | withdrawal_stream1건, prod ACTIVE/head1/floor0; 복구3컬렉션 부재;4필드 projection에 없음; mongosh exit0 |
| Billing | prod19, 복구 HTTP/snapshot/feed/worker false, Identity와 같은 private subnet2개 |
| 네트워크 | Identity SG는 ALB SG에서8081만 허용; 양 subnet 기본 route는 기존 NAT. 새8443 허용 없음 |
| 시작 방식 | ECS entryPoint/command override 없음, mountPoints 빈 배열 |

첫 DB 조회 명령은 인용 오류로 Python 파싱 단계에서 실패했고 실행 전이었다. 인용을 수정해 읽기 전용 조회 성공. URI는 메모리 환경으로만 전달하고 출력하지 않았다. 전체 Gradle 첫 실행은 sandbox 캐시 잠금 접근 실패, 승인된 재실행은 BUILD SUCCESSFUL 28초. 실제 Atlas를 사용하는 제품 테스트는 실행하지 않았다.

Jira TMI-203 공식 조회만 수행, 상태 검토 중 유지. 댓글 초안(미등록): 운영 복구 미준비 재확인 및 TLS/env 주입 로컬 보완, 전체2177개/실패0/skip34. 실제 DB 이관·키/네트워크·연동 및 배포는 미완료. 변경은 entrypoint/application.yml/배포 테스트/준비 문서·작업 기록이다.
