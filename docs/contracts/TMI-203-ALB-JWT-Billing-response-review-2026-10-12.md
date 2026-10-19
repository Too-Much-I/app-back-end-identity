# Identity 검토 — Billing ALB·workload JWT 회신

- 날짜: 2026-10-12 / 브랜치: feat/TMI-203-withdrawal-snapshot-feed-recovery
- 대상: 사용자 첨부 Billing 기술 검토 회신. 문서 검토이며 exact 동결·구현·배포 승인이 아니다.

## 1. 5줄 결론

1. 기존 ALB + 전용 workload JWT 방향에 부합하며 별도 connector/chain과 배포된 public JWK set 초기안을 권장한다.
2. 누락된 `/internal/v1/eligibility/trial/events`는 Identity 코드에도 존재한다. 이벤트 전환 대상은 4개다.
3. Billing Lattice 모드의 permitAll은 상대 보고로 취급한다. 이를 유지한 채 ALB로 노출하지 않아야 한다는 지적은 타당하다.
4. test/staging/production 신뢰 환경 매핑과 Identity 측 포트 격리 범위는 exact 확정 전에 명시해야 한다.
5. body fixture·복구 코어는 유지할 수 있다. 이번 검토는 코드·공유 hash·AWS·Jira를 변경하지 않는다.

## 2. 사용자가 반드시 읽어야 하는 내용

Billing이 제안한 전용 target group/connector는 기존 ALB를 버리고 새 ALB를 만들자는 뜻이 아니다. 같은 ALB에서 workload 트래픽을 전용 서버 포트로 보내고, 그 포트에서도 JWT를 검사하는 방식이다. ALB SG 제한은 호출자 인증을 대신하지 않는다.

배포된 public JWK set 방식은 공개키를 검증 서버 설정에 제공하는 것이다. private key를 상대에게 공유하지 않는다. 원격 JWKS 조회가 없어 초기 운영은 단순해지지만 키 교체/긴급 폐기에 양측 배포가 필요하다. 이는 초기안으로 합리적이나 신속 폐기 요건과 배포 시간을 검증해야 한다.

기존848개 Billing 테스트는 상대의 이전 SigV4 구현 보고다. 새 JWT 검증 성공이나 실환경 ALB 안전성의 근거로 사용하지 않는다.

## 3. 공동 확정이 필요한 사항

| 항목 | Identity 검토 의견 |
| --- | --- |
| 환경 매핑 | `{env}=staging/production` 제안에 실제 Identity test/staging 배포, Billing SANDBOX/PRODUCTION, WITHDRAWAL_ENVIRONMENT를 모두 대응시킨 표가 필요하다. test와staging을 같은신뢰영역으로볼지 명시하고, 다르면issuer/audience/key를분리한다. 기존source stream/cursor의환경값을자동변경하지않는다. |
| 포트 격리 | Billing 전용 workload connector안 권장. Identity도복구용connector/targetgroup과publicport거절을적용할지공동확정한다. 현재Identity RecoveryHttpConfiguration은path matcher만있어포트격리는미구현이다. |
| Bearer 보장 | 별도body서명없이TLS+짧은JWT+route권한을사용하는보안선택을명시수락해야한다. 기존SigV4 payload검증메서드를no-op으로바꾸는방식으로숨기지않는다. |
| 시간 규격 | TTL60초/skew30초·단일문자열aud·정수NumericDate에동의권장. optional nbf의정확관계(예: iat<=nbf<exp)와경계값/overflow/중복claim처리를공동fixture로고정한다. 이는현재확정값이아닌보완권장이다. |
| 정상 교체 | 120초는제안한최대수명/미래iat/skew기반최소경계다. 전체old발급종료와receiver갱신확인및clock오차관측이전제이며배포소요시간이아니다. |
| 오류 분류 | 미검증issuer/서명/키오류401과검증된주체의route/audience/subject권한거절403을구현라이브러리기본동작과대조해exact testcase로고정한다. raw오류/키/토큰노출금지. |

새 ALB나 인증방식의 추가 제품 선택을 요청하는 것이 아니다. 위 세부 규격을 Billing과 정리한 뒤 기존 SigV4-only 문구를 Identity–Billing 범위에 한해 명시 개정한다. Billing 저장소 AGENTS/ADR 내용은 상대 보고이며 이 저장소에서 수정하지 않는다.

## 4. 주요 위험과 미확인 사항

- Billing 배포 모드/실제 ALB/targetgroup/SG/활성 sender는 독립 조회하지 않았다. 저장소 기본OFF만으로 미배포로 판단하지 않는다.
- Identity기존/테스트 ECS의ALB연결은확인했지만공개ALB workload노출승인과ALB→task TLS/격리방침은확정되지않았다.
- 동일토큰의유효기간내재사용은가능하다. jti와eventId멱등성은credential탈취에대한재전송/임의요청차단을대신하지않는다.
- 전용키설정은RSA크기/kid유일/alg/use/key_ops일관성및private필드거절을검증해야한다. damaged/missing 설정은기동실패,unknownkid는401. 실제키생성은이번작업범위가아니다.
- 현재HTTP adapter는인증오류403및IAM전제interface이므로새401/403·JWT/connector규격에맞춰수정과회귀가필요하다. 새방식이이미구현됐다고보지않는다.

## 5. Billing에 전달할 요지 초안

> ALB + 전용 workload JWT, 별도 connector/security chain, 초기 배포형 public JWK set 방향은 타당하다고 검토했습니다. `/internal/v1/eligibility/trial/events`도 Identity 코드에서 확인했으며 4개 이벤트를 전환 목록에 포함하겠습니다.
>
> exact 확정 전에 실제 test/staging/production 배포와 SANDBOX/PRODUCTION·Identity source environment의 매핑, Identity 복구 receiver의 전용 port/public port 거절 범위, ALB→task TLS/격리 조건을 표로 맞춰 주세요. body 서명 없는 Bearer 방식의 보장 차이와 시간·오류 경계도 새 인증 fixture에 명시하는 것을 권장합니다.
>
> 이후 인증 범위만 기존 SigV4 규칙을 개정하고 OFF 구현을 진행하는 순서가 적절합니다. 기존 body/hash/복구·보존·LC/user JWT 계약은 유지합니다. 이 회신은 검토 초안이며 실제 공동동결/구현/배포승인이나외부메시지발송을뜻하지않습니다.

## 6. 부록 — 정적 근거

- [기존 인계서](TMI-203-204-ALB-workload-JWT-handoff-2026-10-12.md)
- [누락 trial 경로의 실제 전송부](../../src/main/java/web/tosunsaeng/identity/domain/auth/phoneidentity/infrastructure/JdkPhoneEligibilityBindingDeliveryAdapter.java): ROUTE와BillingSigV4JsonTransport의존.
- [복구 보안 chain](../../src/main/java/web/tosunsaeng/identity/domain/user/withdrawalrecovery/RecoveryHttpConfiguration.java): 경로매칭·기본OFF·포트조건없음.
- [설정](../../src/main/resources/application.yml): WITHDRAWAL_ENVIRONMENT는외부설정이며첨부의staging/production매핑이자동적용되지않음.
- [배포 workflow](../../.github/workflows/deploy-staging.yml): main/develop의기존/테스트서비스분리. 실제토큰신뢰영역이같다는증거는아님.

문서와작업기록만갱신. 링크/diff정적검사,Gradle미실행. Secret/실제키/토큰기록없음. Jira댓글초안(미등록): Billing JWT회신검토,trial4번째route확인,환경매핑·Identity포트격리·시간/오류exact보완권장. 코드/인프라변경없고새인증검증미완료.
