# 운영 DB 추가 전용 구조 준비 결과

## 1. 5줄 결론

1. 사용자 승인에 따라 백업을 기동 전 단계로 분리하고, 운영 Identity에 컬렉션29개/보조 인덱스70개를 추가했다.
2. 운영 Learning Core에 컬렉션16개/보조 인덱스25개를 추가했다. 병합 transaction probe 빈 컬렉션을 포함한다.
3. 전체 재조회에서 이번 manifest의 누락/정의 충돌0, Identity 보조 인덱스75개 일치, 챌린지100일/300문항 검증 통과를 확인했다.
4. 기존 문서 수정/삭제, 기존 인덱스 교체/TTL 변경, guard 보정, 신규 서버 기동/worker 활성화는 하지 않았다.
5. 백업·실제 기동 설정·구신 서버 보안 호환 검증은 남았다. 이번 성공은 DB 구조 준비 완료이지 출시 가능 판정이 아니다.

## 2. 사용자가 반드시 읽어야 하는 내용

- 범위는 **없는 구조만 추가**다. `user_ownership_guards` 문서 수는 최종0으로, 보호 문서 선삽입이나 테스트 예외 문서 수정은 실행하지 않았다.
- 기존 Identity 인덱스5개는 TTL/partial를 포함해 manifest와 일치했다. 고유키 중복0을 확인한 뒤 추가했다. 새 TTL은 적용 직전에도 빈 컬렉션인지 확인했으며 기존 만료 정책은 바꾸지 않았다.
- Learning Core 학습기록 삭제의 별도5컬렉션/24인덱스, Identity 문의의 programmatic5컬렉션/7인덱스는 이번 기본 묶음에 포함하지 않았다. 기동 시 켤 기능에 따라 별도 준비가 필요하다.
- 저장된 Compass 연결로 운영 이름의 두 DB를 확인하고 내장 mongosh로 실행했다. URI나 비밀번호를 읽어 저장소에 복사하지 않았다. ECS Secret의 host와 이 연결의 동일성 검증은 아직 별도다.

## 3. 사용자가 결정해야 하는 사항

- 신서버 기동 전 백업/복구 수단, 실제 worker 활성화 범위, guard 선보정 필요성 및 쓰기 정지 여부를 결정한다. 이번 승인으로 기동·보정·정리를 실행하지 않는다.

## 4. 주요 위험과 미확인 사항

- 백업은 여전히 미실행이며 직전 Atlas 표시는 Backups Inactive였다.
- 구주소 refresh shared-writer, 병합/탈퇴 접근 차단 호환은 단순 인덱스 생성으로 해결되지 않는다. 좁은 채점 callback 경합 수용과 구분한다.
- 신규 서비스가 꺼진 상태에서 신규 namespace를 준비했다. 빈 컬렉션 확인과 TTL 생성 사이의 임의 동시 writer를 원자적으로 차단하는 도구는 아니다. 이 도구를 기동 후 범용 무중단 migration 도구로 사용하지 않는다.
- 사후 db.stats는 두 DB의 물리 storage/index 크기이며 Atlas 전체 클러스터 quota 지표와 동일하지 않다. 운영 용량/health는 기동 전에 별도 확인한다.
- 장애 시 자동 drop/rollback하지 않는다. 재조회 후 생성된 구조를 보존하고 필요한 다음 작업을 결정한다.

## 5. 현재 작업과 직접 관련된 설명

- [추가 전용 도구](../../scripts/mongodb-additive-ddl.cjs): 정확한 DB 이름과 manifest digest, 읽기 plan을 다시 비교한 뒤 createCollection/createIndex만 실행. 비표준 collection options, 같은 이름 다른 정의, 동일 key 다른 이름, 데이터 있는 컬렉션 신규 TTL은 중단한다. drop/insert/update/delete/collMod 없음.
- [Identity 읽기 도구](../../scripts/identity-index-audit.cjs)는 Compass 내장 shell의 비동기 aggregate cursor에 맞게 보완했다. 브라우저 UI에서 확인 불가했던 full index options를 실제 getIndexes로 검증했다.
- Identity 정의는 [31컬렉션/75인덱스 manifest](identity-index-manifest-2026-10-08.json), LC는 [20대상 컬렉션/25인덱스 manifest](learning-core-index-manifest-2026-10-08.json)로 보존했다. LC 도메인 코드를 이 저장소로 옮기지 않고 기존 prepare의 exported 정의를 읽어 JSON으로 만들었다.
- 기존 prepare apply의 writer-drain 확인을 위조하지 않았다. 문서 backfill이 없는 별도 DDL 경로만 실행했다.

## 6. 부록 — 실행 및 검증 근거

| DB | 신규 컬렉션 | 신규 보조 인덱스 | 사후 컬렉션 전체 | 사후 인덱스 전체(_id 포함) | manifest 잔여 누락 |
| --- | ---: | ---: | ---: | ---: | ---: |
| to-teacher-identity | 29 | 70 | 31 | 106 | 0 |
| to-teacher-app | 16 | 25 | 26 | 58 | 0 |

- Identity storageSize598016bytes/indexSize897024bytes, LC storageSize37093376bytes/indexSize1957888bytes. 관측 시점 수치이며 클러스터 전체 사용량이 아님.
- 최종 Identity existingMatched75, 충돌 없음. LC plan 추가 대상0. catalog days100/questions300, 타입·중복·필수값 검증 성공. guardDocuments0.
- 실제 실행 manifest SHA256: Identity `5b27ecba953346acfc529095db429ff0a8c4fb5da1da01e60bb4f88643ea8b41`, LC `fff2440d2ca9a905733292718fe4b0f7253e6270d2e45c6337a5c90c77ae77c3`. credential hash가 아닌 공개 가능한 구조 정의 digest다.
- 실행 세션 보조 파일은 `/private/tmp/tosunsaeng-lc-ddl-manifest-20261008.cjs`, `/private/tmp/tosunsaeng-production-ddl-session-20261008.cjs`. 자격증명 없이 기존 연결을 받아 실행하며 저장소의 JSON snapshot이 영속 근거다.
- Node13개 통과: 읽기8/추가전용5. 변경 plan·대상 DB 불일치·TTL 데이터 존재·동시 채움·부분 실패 보존·멱등성 포함.
- `./gradlew clean test`: 캐시 sandbox 제한 후 승인 재실행 성공. XML1310개/성공1304/skipped6/fail0/error0. `git diff --check` 통과.
- 변경 파일: 위 도구2개, 해당 test.cjs2개, LC manifest, 이 결과 문서, CURRENT_STATE/WORKLOG 및 이전 조사 문서의 최신 결과 안내. 기존 동시 정책/전환 문서 변경은 보존했다. 예상 밖 main 코드/API 계약 변경 없음. commit/push/배포 없음.
