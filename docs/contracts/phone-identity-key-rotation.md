# PhoneIdentity fingerprint key rotation 계약

이 문서는 Identity 내부 PhoneIdentity fingerprint의 key lifecycle과 무중단 배포 순서를 정의한다. 실제 key material, 전화번호, fingerprint는 이 문서와 저장소에 기록하지 않는다.

## 불변식

- Registry에는 정확히 하나의 `ACTIVE_WRITE`가 있어야 한다.
- 이전 write key는 alias가 참조하는 동안 `LOOKUP_ONLY`로 유지한다.
- 전화번호 원문을 확인할 수 있는 연결·교체 시점에 모든 retained version의 alias를 만든다.
- 모든 writer는 같은 retained version 집합을 사용해야 한다. retained version이 겹치지 않는 mixed writer는 배포하지 않는다.
- `(fingerprintKeyVersion, phoneFingerprint)`의 ACTIVE alias는 하나만 허용한다.
- 전화번호와 fingerprint는 로그, 예외 message, Sentry, metric label에 넣지 않는다.
- PhoneIdentity fingerprint key와 Entitlement benefit fingerprint key/domain은 공유하지 않는다.

## 정상 rotation 순서

1. 새 key를 모든 인스턴스의 `LOOKUP_ONLY`로 배포하고 기존 `ACTIVE_WRITE`를 유지한다.
2. 모든 인스턴스가 동일 registry를 읽는지 확인한다.
3. 새 key를 `ACTIVE_WRITE`, 기존 key를 `LOOKUP_ONLY`로 바꾼 동일 설정을 전체 인스턴스에 배포한다.
4. 번호 원문을 다시 확인할 수 있는 정상 연결·교체 흐름은 새 current fingerprint와 retained alias를 함께 저장한다.
5. 기존 PhoneIdentity의 raw phone은 저장하지 않으므로 온라인 일괄 backfill을 임의로 수행하지 않는다. 재검증 또는 별도 승인된 개인정보 처리 흐름 없이 fingerprint를 복원하려 하지 않는다.
6. legacy alias와 이를 사용하는 모든 writer가 남아 있는 동안 기존 key를 제거하지 않는다.

## 배포 gate와 rollback

- 설정 검증에서 `ACTIVE_WRITE`가 0개 또는 2개 이상이면 애플리케이션은 fail-closed로 시작하지 않는다.
- key version 중복, 잘못된 Base64 또는 32바이트 미만 key도 시작 실패로 처리한다.
- rotation 도중 구버전 writer가 남아 있으면 새 writer 활성화를 중단한다.
- rollback 시 새 key를 `LOOKUP_ONLY`로 유지하고 이전 key를 다시 `ACTIVE_WRITE`로 지정한다. 이미 생성된 새 version alias는 삭제하지 않는다.
- key 제거 전 실제 MongoDB alias version별 참조 수, 배포된 writer version과 backup/복구 절차를 확인한다.

## 번호 교체와 해제

- 번호 교체는 기존 PhoneIdentity와 alias를 `RELEASED`로 바꾸고 새 PhoneIdentity와 retained alias를 같은 Mongo Transaction에서 생성한다.
- SUSPENDED User의 ACTIVE alias는 유지한다.
- WITHDRAWN 번호 해제는 Firebase unlink/delete와 개인정보 tombstone 정책을 포함하는 별도 lifecycle에서 수행한다. 이 Stage 4 기반은 회원 탈퇴에 자동 연결하지 않는다.
