# TMI-203 recovery-r1 문자열 종료 검사 보완 인계

- 날짜: 2026-10-12 / Jira: TMI-203, 연계 TMI-204
- 브랜치: `feat/TMI-203-withdrawal-snapshot-feed-recovery`
- 보완 식별자: `recovery-r1 / scalar-end-1`
- 상태: **Identity 수정·로컬 검증 완료. Billing 재검증/공동 수락 및 실제 API 배포 완료는 아니다.**

## 1. 5줄 결론

1. Java pattern 검색에서 끝 개행을 허용하던 schema의 `$`를 엄격한 문자열 종료 검사로 교체했다.
2. D0/D1/U32·UserId/NewId·Instant·Cursor·contentDigest의 8개 패턴을 모두 보완했다.
3. 공통 `scalar-cases.json`에 정상8개와 개행/제어문자 삽입264개를 추가했다.
4. 실제 networknt 1.5.6 schema 검증 및 기존 decoder 회귀를 포함한 전체 테스트가 성공했다.
5. 공유 schema/fixture hash를 갱신했으며 기존199 fixture, 요청·응답 shape, runtime 코드는 변경하지 않았다.

## 2. 반드시 읽어야 하는 내용

패턴 끝의 `$`를 `(?![\s\S])`로 바꿨다. 어떤 문자도 더 남지 않았을 때만 성공하도록 하며 Java 전용 `\z`를 공유 JSON Schema에 넣지 않았다. 숫자 범위·UUID 버전·기존 허용문자 규칙은 유지했다.

이 보완은 **원래 canonical 계약에 맞게 schema를 강화**한 것이다. 기존 strict scalar decoder나 날짜/숫자 관계 검증을 제거해도 된다는 뜻이 아니다. 달력 유효성, Instant canonical, event의 integer token, 연속 순번 등은 여전히 별도 검증한다.

Production 취약점이나 배포 사고가 확인된 것은 아니다. 현재 실제 recovery HTTP/DB adapter도 아직 구현되지 않았다.

## 3. 사용자가 결정해야 하는 사항

새 제품 정책 결정은 없다. Billing 담당은 다음 작업을 수행하면 된다.

1. `recovery-r1`의 수정된 `wire.schema.json`, `hashes.json`, 신규 `scalar-cases.json` 및 README를 동기화한다.
2. 기존 “schema 허용 / strict 거절” known-gap 테스트를 **schema도 거절**하는 assertion으로 변경한다.
3. 공통264개 실패·정상8개 및 기존 계약 테스트를 독립 실행한다.
4. 새 hash와 실행 결과를 확인한 뒤 공동 exact 수락을 기록한다.

이번에는 Billing 저장소를 직접 수정하거나 메시지를 전송하지 않았다.

## 4. 주요 위험과 미확인 사항

- 다른 검증기의 negative lookahead 지원은 각 소비자가 확인해야 한다. Identity networknt와 ECMAScript RegExp에서는 검증했다.
- Node 검사는 정규식 호환성 검사이며 전체 JSON Schema validator 실행으로 보고하지 않는다.
- Billing의 새 파일 재검증은 아직 받지 않았다. 이전451개 통과 결과를 수정본 검증 결과로 재사용하지 않는다.
- 실제 Mongo snapshot·삭제/evidence 원자성·IAM·복원 검증은 이 schema 보완과 별개로 남아 있다.

## 5. 구현·검증 결과

### 파일

- [wire.schema.json](../../src/test/resources/contracts/payment-lifecycle/recovery-r1/wire.schema.json): 8개 패턴 종료 조건 보완.
- [scalar-cases.json](../../src/test/resources/contracts/payment-lifecycle/recovery-r1/scalar-cases.json): 언어 독립 회귀 입력.
- [hashes.json](../../src/test/resources/contracts/payment-lifecycle/recovery-r1/hashes.json): 입력8개 hash 및 이전 schema hash 보존.
- [RecoveryScalarBoundaryTests.java](../../src/test/java/web/tosunsaeng/identity/domain/user/withdrawalrecovery/RecoveryScalarBoundaryTests.java): networknt·decoder 회귀364개.
- 기존 `RecoverySchemaTests`, README, exact 안내 및 작업 기록 갱신. runtime Java 코드는 이번에 변경하지 않았다.

### 테스트

| 검사 | 결과 |
| --- | --- |
| networknt 1.5.6: 8종 ×11개 문자열 ×앞/중간/끝 | 264개 모두 거절 |
| 기존 요청 decoder: D0/NewId/Cursor의 같은 삽입 사례 | 99개 모두 거절 |
| 정상8개·모든 패턴 커버리지·cursor printable ASCII | 통과 |
| ECMAScript RegExp: 정상8개·실패264개 | 통과 |
| `env -u SENTRY_AUTH_TOKEN ./gradlew clean test` | 1,923개 중1,917통과/6skip/실패0, 24초 |
| `git diff --check` 및 공통8개 hash 검사 | 통과 |

삽입 문자열은 LF,CR,CRLF,NEL,U+2028,U+2029,TAB,NUL,BOM,SPACE,DEL이다. 위치는 prefix,첫문자 뒤 infix,suffix다. 기존 strict decoder는 이미 거절하고 있었으며 이번에 회귀 테스트로 고정했다.

기존 dirty 변경은 보존했다. 이번 범위 밖 변경, DB/설정/배포/Jira 수정·댓글·상태 변경, commit/push는 없다.

## 6. 부록 — hash와 다음 작업

이전 schema SHA-256:

`9f5bbf6d7b9a7429ba2cfba1eb41dcd4df317670b85bde3f4e4c351fa3561863`

보완 schema SHA-256:

`4602a059c8b05fbbee88175faac5d734e74040a8bfe1a9c4564f45f0544cd953`

신규 scalar-cases SHA-256:

`ad146b15daf6e7bb430c21ddb6707da18fbc6b8a7c23ef5d43584b14a94bf827`

기존 공통 입력6개의 hash는 그대로다. hashes.json은 무결성 대조용이며 출처 인증·공동 수락 서명이 아니다.

다음은 Billing 재검증·공동 확인이다. 실제 API/DB 구현과 배포 전 Mongo/IAM/복원 검증은 기존 순서를 유지한다.

Jira 댓글 초안(미등록): recovery-r1 scalar-end-1로8개schema패턴을보완하고공통scalar fixture/hash를갱신했다. networknt·decoder 신규364개 포함 전체1923개 중1917통과/6skip/실패0. runtime/기존199계약불변, Billing독립재검증과실환경검증은남아있다.
