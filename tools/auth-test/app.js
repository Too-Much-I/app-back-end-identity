import { tokenPair, claimSummary } from './session.mjs';
const $ = id => document.getElementById(id);
let sdk, auth, user, enrollment, pair, verifier, verification, busy = false;
let recoveryAuth, recovery, recoveryVerifier, recoveryVerification, recoveryUser;
const show = text => { $('status').textContent = text; };
const need = (condition, code) => { if (!condition) throw new Error(code); };
function controls() {
  for (const id of ['init','google','exchange','send','link','signup','me','merges','refresh','today','clear','recoverSend','recoverConfirm','recoverRetry']) $(id).disabled = busy;
  if (busy) return;
  $('init').disabled = !!auth;
  $('google').disabled = !auth;
  $('exchange').disabled = !user;
  $('send').disabled = !enrollment || !!user?.phoneNumber;
  $('link').disabled = !verification;
  $('signup').disabled = !enrollment || !user?.phoneNumber;
  for (const id of ['me','merges','refresh','today']) $(id).disabled = !pair;
  $('recoverSend').disabled = !recoveryAuth;
  $('recoverConfirm').disabled = !recoveryVerification;
  $('recoverRetry').disabled = !recoveryUser || !recovery;
}
async function api(path, body, access, key) {
  const headers = { 'X-Local-Auth-Test': '1' };
  if (body) headers['Content-Type'] = 'application/json';
  if (access) headers.Authorization = `Bearer ${access}`;
  if (key) headers['Idempotency-Key'] = key;
  const response = await fetch(path, { method: body ? 'POST' : 'GET', headers, body: body ? JSON.stringify(body) : undefined, cache: 'no-store', signal: AbortSignal.timeout(25000) });
  let data;
  try { data = await response.json(); } catch { throw new Error('INVALID_RESPONSE'); }
  if (!response.ok || data.isSuccess !== true) {
    // Only a constrained error code is rendered. Never render raw messages or results.
    const code = typeof data.code === 'string' && /^[A-Z][A-Z0-9_]{0,80}$/.test(data.code) ? data.code : 'API_ERROR';
    throw new Error(`HTTP ${response.status} ${code}`);
  }
  return data.result;
}
function accept(result) {
  const next = tokenPair(result);
  const summary = claimSummary(next.accessToken);
  pair = next; enrollment = null;
  show(`Identity 토큰 수신 (원문 숨김)\n${JSON.stringify(summary, null, 2)}\n이 표시는 디코딩 결과이며 서명 검증 결과가 아닙니다. 서버 호출로 확인하세요.`);
}
function action(id, fn) {
  $(id).onclick = async () => {
    if (busy) return;
    busy = true; controls(); show('처리 중…');
    try { await fn(); } catch (e) {
      const code = e?.code || e?.message;
      show(typeof code === 'string' && /^(auth\/[a-z-]+|[A-Z][A-Z0-9_ ]{0,100})$/.test(code) ? code : '요청 실패. 설정을 확인하거나 Google 재인증 후 다시 시작하세요.');
    } finally { busy = false; controls(); }
  };
}
async function clear() {
  pair = null; enrollment = null; verification = null; user = null;
  verifier?.clear(); verifier = null;
  if (auth) await sdk.signOut(auth);
  if (recoveryAuth) await sdk.signOut(recoveryAuth);
  recovery = null; recoveryUser = null; recoveryVerification = null;
  recoveryVerifier?.clear(); recoveryVerifier = null;
  for (const id of ['recoveryPhone','recoveryOtp']) $(id).value = '';
  for (const id of ['phone','otp','nickname']) $(id).value = '';
  show('로컬 인증정보 삭제 완료. 서버 세션은 폐기하지 않았습니다.');
}
action('init', async () => {
  need(sdk, 'SDK_NOT_READY');
  const config = JSON.parse($('config').value);
  need(config.projectId === 'to-teacher-firebase' && config.apiKey && config.authDomain && config.appId && !config.private_key && !config.client_email, 'INVALID_WEB_CONFIG');
  need(config.authDomain === 'to-teacher-firebase.firebaseapp.com', 'INVALID_AUTH_DOMAIN');
  const app = sdk.initializeApp({ apiKey: config.apiKey, authDomain: config.authDomain, projectId: config.projectId, appId: config.appId });
  auth = sdk.getAuth(app);
  await sdk.setPersistence(auth, sdk.inMemoryPersistence);
  // Fictional phone numbers only: no real SMS, no global production setting changed.
  auth.settings.appVerificationDisabledForTesting = true;
  // Same project, isolated in-memory Auth instance. Never overwrite the main/Guest session.
  recoveryAuth = sdk.getAuth(sdk.initializeApp({ apiKey: config.apiKey, authDomain: config.authDomain,
    projectId: config.projectId, appId: config.appId }, 'account-recovery'));
  await sdk.setPersistence(recoveryAuth, sdk.inMemoryPersistence);
  recoveryAuth.settings.appVerificationDisabledForTesting = true;
  $('config').value = '';
  show('설정 완료. Google 로그인 버튼을 누르세요.');
});
action('google', async () => {
  pair = null; enrollment = null; verification = null;
  const provider = new sdk.GoogleAuthProvider(); provider.setCustomParameters({ prompt: 'select_account' });
  if (auth.currentUser) await sdk.reauthenticateWithPopup(auth.currentUser, provider);
  else await sdk.signInWithPopup(auth, provider);
  user = auth.currentUser;
  show('Google 인증 완료. Identity 로그인 / 가입 준비를 누르세요.');
});
action('exchange', async () => {
  pair = null; enrollment = null;
  const started = Date.now();
  const result = await api('/identity/api/v1/auth/firebase/exchange', { firebaseIdToken: await user.getIdToken(true) });
  if (result.type === 'AUTHENTICATED') accept(result);
  else {
    need(result.type === 'ENROLLMENT_REQUIRED' && result.enrollmentId && Number.isFinite(result.expiresIn), 'INVALID_ENROLLMENT_RESPONSE');
    enrollment = { id: result.enrollmentId, expires: started + result.expiresIn, uid: user.uid };
    show('신규 가입 준비 완료. 테스트 전화번호 연결 후 가입하세요. 이미 전화가 연결되어 있다면 가입으로 진행하세요.');
  }
});
action('send', async () => {
  need(enrollment && Date.now() < enrollment.expires && user.uid === enrollment.uid, 'RESTART_EXCHANGE');
  need(/^\+[1-9]\d{7,14}$/.test($('phone').value.trim()), 'INVALID_PHONE_FORMAT');
  verification = null; verifier?.clear();
  verifier = new sdk.RecaptchaVerifier(auth, 'recaptcha', { size: 'invisible' });
  verification = { id: await new sdk.PhoneAuthProvider(auth).verifyPhoneNumber($('phone').value.trim(), verifier), uid: user.uid };
  $('phone').value = '';
  show('테스트 인증 준비 완료. 콘솔에 등록된 고정 코드를 입력하세요. 실제 SMS는 발송하지 않습니다.');
});
action('link', async () => {
  need(verification && user.uid === verification.uid && Date.now() < enrollment.expires, 'RESTART_EXCHANGE');
  need(/^\d{6}$/.test($('otp').value), 'INVALID_CODE_FORMAT');
  const credential = sdk.PhoneAuthProvider.credential(verification.id, $('otp').value); $('otp').value = '';
  const uid = user.uid;
  const result = await sdk.linkWithCredential(user, credential);
  need(result.user.uid === uid, 'UID_CHANGED_STOP');
  user = result.user; verification = null;
  await user.getIdToken(true);
  show('같은 Firebase 계정에 전화 연결 완료. 정책 확인·동의 후 가입하세요.');
});
action('signup', async () => {
  need(enrollment && user.uid === enrollment.uid && Date.now() < enrollment.expires, 'RESTART_EXCHANGE');
  need($('privacy').checked && $('terms').checked && $('privacyVersion').value.trim() && $('termsVersion').value.trim(), 'CONSENT_REQUIRED');
  const nickname = $('nickname').value.trim(); need(nickname.length >= 2 && nickname.length <= 20, 'INVALID_NICKNAME');
  accept(await api('/identity/api/v1/auth/firebase/signup', { enrollmentId: enrollment.id, firebaseIdToken: await user.getIdToken(true), nickname,
    isPrivacyConsented: true, privacyConsentVersion: $('privacyVersion').value.trim(), isTermConsented: true, termConsentVersion: $('termsVersion').value.trim() }));
});
action('refresh', async () => {
  const previous = pair; pair = null; // Ambiguous response must never trigger old-token reuse in Stage 9 OFF.
  show('재발급 중. 응답 실패 시 Google 로그인부터 다시 진행하세요.');
  accept(await api('/identity/api/v1/auth/reissue', { refreshToken: previous.refreshToken }, null, crypto.randomUUID()));
});
action('me', async () => { await api('/identity/api/v1/users/me', null, pair.accessToken); show('프로필 API 인증 성공. 개인정보는 표시하지 않습니다.'); });
action('merges', async () => {
  const result = await api('/identity/api/v1/users/me/merges?activeOnly=false&limit=20', null, pair.accessToken);
  need(Array.isArray(result.items), 'INVALID_MERGE_RESPONSE');
  const rows = result.items.map(item => {
    need(['PROCESSING','ACTION_REQUIRED','COMPLETED'].includes(item.status), 'INVALID_MERGE_RESPONSE');
    return { status: item.status, learningCore: item.learningCore?.status, billing: item.billing?.status,
      nextPollAfterSeconds: item.nextPollAfterSeconds };
  });
  show(JSON.stringify(rows, null, 2) + '\\n최근 20건. 빈 목록은 전체 이전 완료를 뜻하지 않습니다. 자동 polling/병합 재실행 없음.');
});
action('today', async () => { await api('/learning/api/v1/challenges/today', null, pair.accessToken); show('Learning Core today 호출 성공. 응답 본문은 표시하지 않습니다.'); });
action('clear', clear);
async function lookupRecovery() {
  need(recovery && recoveryUser, 'RESTART_RECOVERY');
  const result = await api('/identity/api/v1/auth/account-recovery/lookup', {
    recoveryId: recovery.recoveryId, firebaseIdToken: await recoveryUser.getIdToken(true) });
  need(['FOUND','NOT_FOUND','ACTION_REQUIRED'].includes(result.status), 'INVALID_RECOVERY_RESPONSE');
  if (result.status === 'FOUND') {
    need(['GOOGLE','APPLE','KAKAO'].includes(result.provider), 'INVALID_RECOVERY_RESPONSE');
    show(`가입 SNS: ${result.provider}\n이메일 힌트: ${result.maskedEmail || (result.emailHintKind === 'APPLE_PRIVATE_RELAY' ? 'Apple 비공개 이메일' : '제공되지 않음')}\n해당 SNS로 다시 로그인하세요. 기존 Identity 세션은 변경하지 않았습니다.`);
  } else show(result.status === 'NOT_FOUND' ? '가입 계정을 찾지 못했습니다.' : '추가 확인이 필요합니다. 고객 지원에 문의하세요.');
}
action('recoverSend', async () => {
  need(/^\+[1-9]\d{7,14}$/.test($('recoveryPhone').value.trim()), 'INVALID_PHONE_FORMAT');
  recovery = null; recoveryUser = null; recoveryVerification = null;
  await sdk.signOut(recoveryAuth);
  recovery = await api('/identity/api/v1/auth/account-recovery/prepare', {});
  need(recovery?.recoveryId, 'INVALID_RECOVERY_RESPONSE');
  recoveryVerifier?.clear();
  recoveryVerifier = new sdk.RecaptchaVerifier(recoveryAuth, 'recoveryRecaptcha', { size: 'invisible' });
  recoveryVerification = await new sdk.PhoneAuthProvider(recoveryAuth).verifyPhoneNumber($('recoveryPhone').value.trim(), recoveryVerifier);
  $('recoveryPhone').value = '';
  show('계정 찾기용 테스트 인증 준비 완료. 가상 번호의 고정 코드를 입력하세요.');
});
action('recoverConfirm', async () => {
  need(recoveryVerification && /^\d{6}$/.test($('recoveryOtp').value), 'INVALID_CODE_FORMAT');
  const credential = sdk.PhoneAuthProvider.credential(recoveryVerification, $('recoveryOtp').value);
  $('recoveryOtp').value = '';
  recoveryUser = (await sdk.signInWithCredential(recoveryAuth, credential)).user;
  recoveryVerification = null;
  await lookupRecovery();
});
action('recoverRetry', lookupRecovery);
try {
  const [appSdk, authSdk] = await Promise.all([import('https://www.gstatic.com/firebasejs/10.14.1/firebase-app.js'), import('https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js')]);
  sdk = { ...appSdk, ...authSdk }; show('Firebase 웹 구성 JSON을 입력하세요.');
} catch { show('Firebase SDK 로딩 실패. 인터넷 연결·브라우저 설정을 확인하세요.'); }
controls();
