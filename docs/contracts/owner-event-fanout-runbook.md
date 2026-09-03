# Identity owner event durable fan-out 운영 절차

## 1. 5줄 결론

1. 모든 capture와 publisher 플래그는 기본값 `false`이며 consumer가 준비되기 전에는 활성화하지 않는다.
2. `UserMerged`는 Billing과 Learning Core로, `TrialOwnerRebindApproved`는 Billing로만 전달한다.
3. consumer별 sequence의 exact head만 전송하므로 DEAD_LETTER나 circuit pause를 임의로 건너뛰지 않는다.
4. 재전송은 동일 `eventId`와 동일 payload를 사용하고, cursor나 downstream DB를 수동 변경하지 않는다.
5. 실제 ARN, DNS, task role, credential과 사용자 식별자는 이 문서나 일반 로그에 기록하지 않는다.

## 2. 활성화 순서

1. Billing과 Learning Core consumer를 feature flag OFF로 먼저 배포한다.
2. MongoDB가 replica set Transaction과 신규 unique/TTL index를 지원하는지 staging에서 확인한다.
3. VPC Lattice 정책이 Identity application task role에 승인된 POST route만 허용하는지 확인한다.
4. downstream consumer를 먼저 켠다.
5. Identity capture를 켜고 `owner_event_cores`, `owner_event_deliveries`가 함께 생성되는지 확인한다.
6. Billing trial rebind, Billing user merge, Learning Core user merge publisher를 한 채널씩 켠다.
7. pending age, sequence gap, dead-letter, circuit 상태와 legacy outbox 잔량을 확인한다.

phone eligibility SigV4 publisher도 Billing consumer와 IAM 검증 뒤 별도로 활성화한다.

## 3. 중단과 복구

- 장애 시 publisher 플래그를 먼저 끈다. capture가 만든 delivery는 TTL 없이 보존된다.
- `408`, `425`, `429`, `5xx`, timeout과 connection failure는 자동 재시도된다.
- `400`, `409`, `413`, `422`는 payload 계약 실패이므로 DEAD_LETTER 원인을 먼저 해결한다.
- `3xx`, 인증·권한·route 관련 `4xx`는 circuit을 PAUSED로 만들므로 DNS, route, IAM, audience 설정을 먼저 고친다.
- 원인 해결 뒤 exact head DEAD_LETTER를 같은 event로 replay하고, PAUSED circuit을 resume한다.
- downstream commit 여부가 불명확해도 새 event를 만들지 않는다. consumer inbox 멱등성에 기대어 같은 event를 재전송한다.

## 4. 금지 사항

- DEAD_LETTER를 건너뛰기 위해 `lastPublishedSequence`를 직접 증가시키지 않는다.
- 이미 적용된 owner 변경을 반대 방향 event 없이 DB에서 원복하지 않는다.
- phone rejoin event를 Learning Core에 보내지 않는다.
- released alias의 최신 시각만 보고 predecessor를 추측하거나 historical backfill을 실행하지 않는다.
- source/target userId, phone fingerprint, Firebase UID, payload, workload JWT, AWS 서명 header를 로그에 남기지 않는다.

## 5. 배포 전 확인 목록

- Billing SigV4 signing name `vpc-lattice-svcs`, 배포 region, HTTPS origin
- phone eligibility와 owner event의 exact route
- Learning Core `UserMerged` workload issuer, audience, subject와 HTTPS endpoint
- consumer별 inbox 멱등 처리와 schema v1 fixture
- Mongo Transaction, unique index, nullable TTL index
- PUBLISHED P30D, DEAD_LETTER review P90D, core cleanup +24h
- 없음/OPEN/RETAKE_AVAILABLE/GRADING/COMPLETED staging 시나리오
- A→B→C 반복 재가입의 Billing sequence 순서
