# Workload JWT 인증 회귀 자료

2026-10-12 Identity 구현에 추가한 합성 시간 경계 16종. 기존 recovery-r1/scalar-end-1의 body fixture/hash와 별도다.
사용자가 Billing의 인증 규격 수락을 전달했으나 **이 신규 파일을 Billing에서 실행·동기화했다는 뜻은 아니다.**

`RecoveryJwtVerifierTests.sharedTimeCases`가 각 claims를 로컬 임시 RSA 키로 서명해 실제 검증기를 실행한다.
now는 검증 UTC epoch second, accepted는 인증 시간 규칙의 기대값이다. 추가 숫자 token/헤더/권한/키 회귀는 Java 테스트에 있다.
실제 issuer/audience/키/인프라 값은 포함하지 않는다. 양측 배포 전 이 파일과 오류/포트 회귀 결과를 공유해야 한다.
