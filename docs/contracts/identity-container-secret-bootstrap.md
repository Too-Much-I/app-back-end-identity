# Identity 컨테이너 Secret 파일 공급

## 요약

- ECS Secrets Manager 주입은 환경변수 공급이며 파일 마운트가 아니다.
- entrypoint는 JWT PEM, 선택적 Firebase JSON/복구 keyring을 task-local 파일로 공급한다.
- 기존 JWT-only 실행은 유지한다. Firebase/복구가 OFF이면 해당 자격증명은 필수가 아니다.
- 새 이미지와 Task Definition 연결이 모두 필요하며 이 변경 자체는 AWS를 변경하지 않는다.
- 실제 자격증명/서버 연결/컨테이너 검증은 배포 전 별도로 수행한다.

## ECS 설정 매핑

아래 `secrets` 값은 Secret ARN 참조로 설정한다. 실제 값을 Task Definition 일반 `environment`에 작성하지 않는다.

| 컨테이너 환경변수 | Secret 저장 내용 | 공급 방식 |
| --- | --- | --- |
| `JWT_PRIVATE_KEY_PEM` | PKCS8 PEM 원문 | 기존 방식 유지 |
| `JWT_PUBLIC_KEY_PEM` | 대응 X509 공개 PEM 원문 | 기존 방식 유지 |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | 서비스 계정 JSON 전체 | JSON 개별 필드 선택 없이 Secret 전체 |
| `AUTH_REISSUE_ENCRYPTION_KEYRING_CONTENT` | properties 원문 | Secret 전체, JSON 포장 금지 |
| `PHONE_IDENTITY_FINGERPRINT_KEY_RING` | 별도 JSON Secret의 동일 이름 필드 | 해당 JSON 필드만 주입; 파일 생성 대상 아님 |

Secret 조회는 ECS **task execution role**에 필요한 테스트 ARN만 허용한다. 고객 관리 KMS 키를 사용하면 해당 키의 복호화 권한도 확인한다. 애플리케이션에 Secrets Manager SDK 조회 권한을 추가하지 않는다.

일반 설정:

```text
FIREBASE_AUTH_ENABLED=true
FIREBASE_PROJECT_ID=<test-project-id>
AUTH_REISSUE_RECOVERY_ENABLED=true
AUTH_REISSUE_ENCRYPTION_ACTIVE_KEY_ID=test-v1
AUTH_REISSUE_ENCRYPTION_ENVIRONMENT=test
```

이는 전체 기능 설정 목록이 아니다. Firebase provider/phone fingerprint, 복구의 session fence 등 기존 활성화 의존성을 별도로 만족해야 한다. 새 플래그를 자동 활성화하지 않는다.

## 파일 공급 규칙

- 기본 root: `/app/runtime/keys`; `IDENTITY_RUNTIME_SECRET_DIR`로 절대 경로 변경 가능.
- root와 부모 경로는 신뢰된 배포 설정으로만 지정한다. root symlink는 거절한다.
- root는 `700`, 각 기동의 새로운 `boot.*` 디렉터리는 `700`, 생성 파일은 `600`이다.
- non-root `app` 사용자로 실행한다. read-only root filesystem을 쓰면 지정한 root에 app이 쓸 수 있는 task-local volume이 필요하다.
- JWT 위치는 `file:/…/private.pem`, `file:/…/public.pem` Spring resource URI로 설정한다.
- Firebase는 `GOOGLE_APPLICATION_CREDENTIALS=/…/firebase.json`으로 설정한다.
- 복구는 `AUTH_REISSUE_ENCRYPTION_KEYRING_LOCATION=/…/reissue.properties`로 설정한다. 위 두 경로에는 `file:` 접두사를 붙이지 않는다.
- 파일 공급 후 원문 환경변수를 unset하고 `exec java`로 실행한다. 내용/길이/해시를 로그에 남기지 않는다.
- 셸 단계 실패 시 해당 기동이 만든 파일/디렉터리만 제거한다. JVM 실행 이후에는 파일이 task 수명 동안 남으므로 공유/영구 볼륨을 쓰지 않는다. JVM 실패 또는 task 재시작 시 이전 파일 자동 삭제를 보장하지 않는다.

### 기존 파일 공급과 충돌

Firebase/복구는 원문 대신 각각의 경로 변수로 외부에서 공급한 파일을 사용할 수 있다. 절대 경로의 읽을 수 있는 비어 있지 않은 일반 파일이어야 한다. 원문과 경로를 동시에 지정하면 거절한다. 외부 파일의 내용/권한은 변경하지 않는다. 배포자가 최소 권한과 task 간 격리를 보장해야 한다.

Firebase auth ON이면 명시적인 credentials 파일과 project ID가 필수다. 이 Docker entrypoint는 ambient ADC 검색이나 metadata 서버 fallback에 의존하지 않는다. WIF 등 다른 ADC 공급 방식을 도입하려면 해당 credential configuration 파일을 제공하고 별도 검증한다. Java ADC 구현 자체는 변경하지 않는다.

복구 ON이면 keyring, 활성 키 ID, 환경 이름이 필수다. JSON 유효성, RSA PEM 로딩, keyring의 32바이트 Base64 및 활성 key 존재 검증은 기존 Java 계층이 수행한다. entrypoint는 암호학적 유효성을 검증한 것으로 간주하면 안 된다.

## 보안 한계 및 배포 전 확인

- 환경변수 unset은 자식 JVM에 원문을 상속하지 않도록 하는 조치이지 ECS Exec/초기 프로세스 환경/호스트 관리자 접근까지 제거하는 보장은 아니다. ECS Exec 및 디버그/덤프 접근을 제한한다.
- Secret 변경은 실행 중 task에 자동 반영되지 않는다. 새 task 배포와 키 회전 호환성을 별도로 확인한다.
- 테스트 키/issuer/DB를 운영과 분리하고 테스트 이벤트가 운영 consumer로 전송되지 않게 한다.
- 실제 컨테이너의 `app` 사용자 파일 쓰기, mount 권한, Secret 매핑, Firebase 프로젝트 일치, JWKS, 가입/재발급을 검증한다.

## 검증

`DockerEntrypointTests`는 가짜 java 실행 파일과 가짜 credential만 사용하여 쉘 프로세스를 검증한다. 원문 보존, 파일 권한, 기존 파일 지원, 누락/충돌/잘못된 경로 거절, 실패 후 정리, JVM 환경에서 원문 제거와 출력 비노출을 확인한다. 외부 Firebase/Atlas/AWS를 호출하지 않는다.

```bash
./gradlew test --tests '*DockerEntrypointTests'
./gradlew clean test
```

Jira 댓글 초안(이슈 미지정, 자동 등록하지 않음): 테스트 Identity의 Firebase/복구 keyring 파일 공급 및 권한 제한을 추가하고 기존 JWT 주입/API 계약을 유지했다. 배포 전 실제 ECS Secret 매핑·파일 권한·인증 E2E 검증이 필요하다.
