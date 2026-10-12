# 운영 DB 변경 목록 생성 및 읽기 검증 결과

> 후속 적용 완료: 사용자 추가전용 승인 후 기존 전체 옵션을 Compass shell로 검증하고 운영 기본 구조를 추가했다. [최신 실행 결과](PRODUCTION_DB_DDL_APPLIED_2026-10-08.md)를 우선 참고한다. 아래는 적용 전 조사 기록이며 당시의 미확인 상태를 담는다.

> 사용자 결정 갱신: 확인된 orphan 결과4건·UUID 형식 오류 owner 문서2건은 사용자가 만든 테스트 데이터이며 이번 점검에서 넘어가도 된다고 명시했다. 아래 발견 당시의 blocker는 **이 확인된 건들에 한해 수용된 예외**로 전환한다. 삭제/자동 보정 없음. 다른 오류까지 포괄 면제하지 않으며, 실제 적용 경로에는 정확히 한정된 예외 식별과 정상 owner 필터가 필요하다. 전체 옵션 검증과 적용 승인 등 나머지 조건은 그대로 남는다.

## 1. 5줄 결론

1. Identity 실제 Spring Data resolver로 **31개 mapped collection/75개 보조 인덱스 정의**를 [JSON 목록](identity-index-manifest-2026-10-08.json)에 생성했다. 기본 `_id_`와 programmatic 문의 인덱스는 별도다.
2. 기존 조회의 인덱스 이름 기준으로 신규29컬렉션/70인덱스 후보지만, 기존5개 정의의 전체 옵션 비교가 끝나지 않았으므로 최종 실행 승인 목록으로 간주하지 않는다.
3. 운영 LC 집계의 **세션을 찾지 못한 결과4건, 표준 UUID가 아닌 owner 값을 가진 학습 문서2건**은 사용자가 확인한 테스트 예외다. 보존하되 실제 실행의 한정 allowlist는 아직 준비되지 않았다.
4. 활성 시험 owner 중복0, 세션이 존재하는 결과·요약 owner 불일치0, users 고유키 중복0을 확인했다. 이것만으로 전체 migration 안전성이 증명되지는 않는다.
5. DB 쓰기·삭제·기동은 하지 않았다. Java 전체 테스트는 총1310개 중1304개 성공/6개 skipped/실패0, 읽기 검사기 Node 테스트7개 성공이다.

### 후속 읽기 검증 — 2026-10-08

<!-- codex-turn:01a11a4d-66e3-7110-a02f-82d562326bec -->

- **카탈로그 통과:** 운영 `challenge_10s_questions`는100일/300문항, Day1 문서1개. 날짜 양의 BSON int·일별3문항·문항번호 BSON int 1..3·difficulty BSON int·필수 문자열 비공백 검사를 모두 통과했다. 날짜/일별 문항번호/전체 questionId 중복 그룹 각각0. UI 인덱스는 `_id_` 하나뿐이어서 챌린지 unique2개는 누락 확인, TTL 없음.
- **백업은 미준비:** Project0 Cluster0 목록에 `Backups Inactive`, Free cluster, 저장공간334.57MB/512MB(65%)가 표시됐다. Backup 페이지는 복구 지점 대신 daily/continuous backup 업그레이드 안내를 표시했다. 외부 수동 백업의 존재·복구 가능성은 확인하지 못했다. 이전 azure_results 복사만으로 Identity/LC 전체 복구를 보장하지 않는다.
- **옵션 조회 한계:** refresh TTL 행을 펼쳐 `expiresAt` ASC를 확인했지만 TTL 초나 full index spec은 제공되지 않았다. `getIndexes()` 전체 옵션을 얻을 연결된 shell이 필요하다. UI 배지만으로 기존5개 Identity 인덱스를 호환 판정하지 않는다.
- **적용안은 조건부:** Identity 신규29컬렉션/70인덱스 후보, LC 기본15컬렉션(+병합 probe1개 후보)/25개 요구 인덱스 정의를 유지한다. optional 학습삭제/문의 범위는 [준비 계획](PRODUCTION_DB_PREPARATION_PLAN_2026-10-08.md)을 따른다. 전체 옵션 diff 전 최종 생성 수나 즉시 실행 목록으로 확정하지 않는다.
- **다음 필요한 결정:** (1) 암호화·접근제한된 수동 전체 백업과 별도 DB 복원 검증, 또는 (2) 유료 Atlas 백업 지원 tier 전환 중 경로 선택. 비용·권한·DB export·서버 정지는 이번에 승인받거나 실행하지 않았다. 백업 및 full-options 검사 이후 additive DDL만 먼저 승인 요청하고 guard/backfill·worker 기동은 분리한다.

## 2. 사용자가 반드시 읽어야 하는 내용

- 실제 DB 적용 전 **백업·기존 인덱스 전체 옵션·한정 예외 실행 경로를 확보할 단계**다. 사용자 확인 테스트 데이터는 보존한다. 고아 결과4건을 자동 삭제하거나 다른 시험에 연결하지 않았고 비정상 owner2건도 임의 UUID로 바꾸지 않았다.
- 4건과2건은 각각 결과 문서와 owner 형식 검사 결과다. 전체 고유 사용자6명을 뜻하지 않는다. 결과/요약 invalidOwner는0이었으므로 현재 관측상 owner 형식 오류는 exam_sessions 측에 있다. 정확한 문서 대응·발생 원인은 추가 조사 대상이다.
- 이전에 수용한 callback/병합의 좁은 동시성 위험과 이미 존재하는 orphan/형식 오류는 별개다. 그 승인을 기존 데이터 오류를 무시하는 승인으로 해석하지 않는다.
- 생성한 검사기는 getCollectionInfos/getIndexes/aggregate만 사용한다. `createIndex`, 수정, 삭제, apply 기능이 없다. 중복 값·사용자 식별자는 보고하지 않는다.

## 3. 사용자가 결정해야 하는 사항

- 사용자 확인 테스트 예외는 보존한다. 실제 apply에 필요한 제한적 식별 방식은 미완료이며 전체 오류를 무시하는 조건으로 대체하지 않는다.
- 구조만 추가하는 DDL은 이 문제와 분리 가능하지만, 기존 옵션 비교·백업/자원 여유 확인·최종 diff 승인 전 실행하지 않는다.

## 4. 주요 위험과 미확인 사항

- 이 결과는 **전체 준비 스크립트의 운영 dry-run 성공**이 아니다. Atlas UI에서 핵심 read-only 집계만 실행했다. 전체 Identity 검사기는 운영 연결에 실행하지 않았다.
- `$indexStats`에서 `spec`만 project한 결과가 빈 객체3개여서 이 경로로는 TTL/partial/collation 옵션을 확인할 수 없었다. 옵션이 없다는 뜻으로 해석하지 않는다. 기존 인덱스 전체 옵션 검증이 남았다.
- challenge 카탈로그/BSON int 검증은 후속 집계로 통과했다. attempt/job 등 신규 컬렉션의 적용 직전 재검사, Secret 연결 host 일치, 백업/복구, 전체 unique/TTL 검사, 기동 worker 확인은 남았다. 저장공간은 후속 조회334.57MB/512MB이며 신규 인덱스 생성 후 용량은 미확인이다.
- 운영은 조회 중에도 쓰기가 발생한다. 아래 숫자는 이번 조회 시점 값이며 이전 summary765 미만 집계와 차이가 날 수 있다.

## 5. 현재 작업과 직접 관련된 설명

### 생성·재실행 방법

```sh
./gradlew test --tests '*MongoIndexManifestTests' --rerun-tasks
node --test scripts/identity-index-audit.test.cjs
```

첫 명령은 DB/앱 context 없이 `build/generated/mongodb/identity-index-manifest.json`을 생성한다. 이번 결과를 docs/codex의 날짜 고정 JSON으로 보존했다. 코드 변경 뒤에는 재생성하여 비교해야 한다. 31개 단언은 새 mapped entity 누락을 검토하도록 의도적으로 고정했다.

읽기 전용 Mongo shell 연결이 별도로 승인·준비된 경우, 저장소 루트에서 해당 shell의 다음 호출로 실제 inspector를 실행할 수 있다. 이번에는 실행하지 않았다. 연결 URI/자격증명은 명령이나 문서에 넣지 않는다.

```javascript
const manifest = EJSON.parse(require('fs').readFileSync(
  'docs/codex/identity-index-manifest-2026-10-08.json', 'utf8'));
const audit = require('./scripts/identity-index-audit.cjs');
printjson(await audit.inspect(db.getSiblingDB('to-teacher-identity'), manifest));
```

- 검사기는 ordered keys, name, unique, sparse, hidden, TTL, partialFilterExpression, collation을 비교한다.
- 기존 같은 이름의 불일치는 conflict, 없는 항목만 missing으로 반환한다. unique 검사는 partial/sparse 대상에 한정하고 null/누락을 같은 그룹으로 비교한다. array/특수 collation은 자동 합격시키지 않고 수동 검토로 보낸다.
- 각 aggregate는 maxTimeMS15000으로 제한한다. query 오류는 성공 보고서로 변환하지 않는다.
- `noDetectedIndexConflicts`는 해당 검사에서 충돌을 찾지 못했다는 뜻일 뿐 배포/적용 승인이나 concurrent writer 안전성을 의미하지 않는다.

## 6. 부록 — 실제 집계 및 변경 파일

### A. Atlas 운영 읽기 결과

대상은 Project0/Cluster0, `to-teacher-app`, `to-teacher-identity`. 원본 문서와 개인정보·토큰·해시 값은 출력하지 않고 집계했다.

| 검사 | 결과 |
| --- | ---: |
| exam_results 문서 | 9834 |
| 결과 examId→exam_sessions._id 미매칭 | 4 |
| 매칭 결과의 owner 불일치 | 0 |
| 결과 owner UUID 형식 오류 | 0 |
| exam_summaries 문서 | 765 |
| 요약 session 미매칭/owner 불일치/UUID 오류 | 각각0 |
| orphan 결과의 examId 및 userId BSON 타입 | 모두 string,4건 |
| session/result/summary owner 합집합 그룹 수 | 320(유효·비정상 값 모두 포함) |
| 위 합집합 입력 문서 중 owner UUID 형식 오류 | 2건(고유 owner 수 아님) |
| effective active session owner 중복 그룹 | 0 |
| users normalizedEmail string 대상 중복 그룹 | 0 |
| users guestInstallationIdHash string 대상 중복 그룹 | 0 |
| 위 users 고유키 array 타입 | 0 |

LC 집계는 기존 user-merged-prepare.js의 session lookup/owner 검사 및 effective-active 조건과 대조했다. active는 IN_PROGRESS/ENTITLEMENT_CONFIRMING 또는 status null/누락 + active true/null/누락 + completedAt null/누락 조건이다. UUID 검사는 소문자 canonical 버전1~5/variant8~b 조건이다.

### B. 변경 파일/검증

- [MongoIndexManifestTests](../../src/test/java/web/tosunsaeng/identity/global/config/MongoIndexManifestTests.java): annotation scanner/MongoMappingContext/index resolver로 목록 생성, 회전 partial unique 계약 검증. 외부 Mongo/Firebase 호출 없음.
- [읽기 검사기](../../scripts/identity-index-audit.cjs), [테스트](../../scripts/identity-index-audit.test.cjs): 메타데이터 비교/중복 집계, read-only fixture 테스트7개.
- [생성 JSON](identity-index-manifest-2026-10-08.json): annotation-derived31컬렉션/75개 정의, 개인정보 없음. Support manual 인덱스는 기존 계획 원본 참조.
- `./gradlew clean test`: 기본 sandbox의 Gradle cache lock 접근 거부 후 승인된 권한으로 재실행 성공. XML 총1310/fail0/error0/skipped6. Node7/7, git diff --check 통과.
- API/런타임 main 코드·JWT·DB schema 실제 상태 변경 없음. Swagger 공유본 생성/commit/push 없음. 기존 동시 작업의 정책 문서/기록은 보존했다.

### C. 다음 단계

1. 예외4결과/2owner 문서의 생성·참조 관계를 읽기 전용으로 조사한다. 어떤 내용도 자동 삭제/귀속 변경하지 않는다.
2. 승인된 접속 환경에서 전체 인덱스 옵션 read-only 비교 및 나머지 카탈로그 검증을 마친다.
3. 원인과 복구 선택을 합의한 뒤 정확한 additive DDL/guard 변경 diff를 승인받는다. 기존 writer-drain 조건과 구주소 보안 경계는 계속 유지한다.
