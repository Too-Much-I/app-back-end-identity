import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

test('UI flow with mocked Firebase and HTTP: consent, same UID, rotation and safe output', async () => {
  const elements = new Map();
  const el = id => {
    if (!elements.has(id)) elements.set(id, { value: '', checked: false, disabled: false, textContent: '' });
    return elements.get(id);
  };
  const originalDocument = globalThis.document, originalFetch = globalThis.fetch;
  const requests = [];
  let failRefresh = false;
  globalThis.document = { getElementById: el };
  const jwt = `fake.${Buffer.from(JSON.stringify({ sub: '00000000-0000-4000-8000-000000000000', account_type: 'MEMBER', aud: ['tosunsaeng-learning-core'], exp: 1900000000 })).toString('base64url')}.fake`;
  globalThis.fetch = async (path, options) => {
    requests.push({ path, options });
    if (path.endsWith('/reissue') && failRefresh) throw new Error('sensitive response must not appear');
    const result = path.endsWith('/exchange') ? { type: 'ENROLLMENT_REQUIRED', enrollmentId: 'fake-enrollment', expiresIn: 600000 } : { accessToken: jwt, refreshToken: 'fake-refresh' };
    return new Response(JSON.stringify({ isSuccess: true, result }));
  };
  const mock = `
    const user = { uid: 'fake-uid', phoneNumber: null, getIdToken: async () => 'fake-firebase' };
    const auth = { currentUser: null, settings: {} };
    export const initializeApp = x => x, getAuth = () => auth;
    export const setPersistence = async () => {}, inMemoryPersistence = {};
    export class GoogleAuthProvider { setCustomParameters() {} }
    export const signInWithPopup = async () => { auth.currentUser = user; };
    export const reauthenticateWithPopup = async () => {};
    export const signOut = async () => { auth.currentUser = null; };
    export class RecaptchaVerifier { clear() {} }
    export class PhoneAuthProvider { async verifyPhoneNumber() { return 'fake-verification'; } static credential() { return {}; } }
    export const linkWithCredential = async () => { user.phoneNumber = 'fake-linked'; return {user}; };
  `;
  const moduleUrl = `data:text/javascript;base64,${Buffer.from(mock).toString('base64')}`;
  const source = (await readFile(new URL('./app.js', import.meta.url), 'utf8'))
    .replace("'./session.mjs'", JSON.stringify(new URL('./session.mjs', import.meta.url).href))
    .replaceAll(/'https:\/\/www\.gstatic\.com\/firebasejs\/10\.14\.1\/firebase-(app|auth)\.js'/g, JSON.stringify(moduleUrl));
  try {
    await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`);
    assert.equal(el('google').disabled, true);
    el('config').value = JSON.stringify({ apiKey: 'fake', authDomain: 'to-teacher-firebase.firebaseapp.com', projectId: 'to-teacher-firebase', appId: 'fake' });
    await el('init').onclick(); await el('google').onclick(); await el('exchange').onclick();
    assert.equal(el('send').disabled, false); assert.equal(el('signup').disabled, true);
    el('phone').value = '+16505550123'; await el('send').onclick();
    el('otp').value = '000000'; await el('link').onclick();
    assert.equal(el('otp').value, ''); assert.equal(el('signup').disabled, false);
    await el('signup').onclick(); assert.equal(el('status').textContent, 'CONSENT_REQUIRED');
    assert.equal(requests.length, 1);
    el('privacy').checked = el('terms').checked = true;
    el('privacyVersion').value = 'fake-privacy'; el('termsVersion').value = 'fake-terms'; el('nickname').value = '테스트';
    await el('signup').onclick();
    assert.equal(el('refresh').disabled, false);
    assert.equal(el('status').textContent.includes(jwt), false);
    assert.equal(JSON.parse(requests[1].options.body).firebaseIdToken, 'fake-firebase');
    await el('refresh').onclick();
    assert.match(requests[2].options.headers['Idempotency-Key'], /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
    failRefresh = true; await el('refresh').onclick();
    assert.equal(el('refresh').disabled, true); assert.equal(el('me').disabled, true);
    assert.equal(el('status').textContent.includes('sensitive response'), false);
    await el('clear').onclick(); assert.equal(el('exchange').disabled, true);
  } finally { globalThis.document = originalDocument; globalThis.fetch = originalFetch; }
});
