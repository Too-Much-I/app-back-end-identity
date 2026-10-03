import { test } from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import { createServer } from './server.mjs';
import { tokenPair, claimSummary } from './session.mjs';

test('local gateway: fixed upstream, host/origin guards, no secret logs or redirects', async () => {
  const calls = [];
  const server = createServer(async (...args) => { calls.push(args); return new Response('{"isSuccess":true,"result":{}}'); });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const port = server.address().port;
  const origin = `http://localhost:${port}`;
  const request = (path, method = 'GET', headers = {}, body = '') => new Promise((resolve, reject) => {
    const req = http.request({ hostname: '127.0.0.1', port, path, method, headers: { Host: `localhost:${port}`, ...headers } }, res => {
      let text = ''; res.on('data', c => text += c); res.on('end', () => resolve({ status: res.statusCode, headers: res.headers, text }));
    }); req.on('error', reject); req.end(body);
  });
  try {
    assert.equal((await request('/')).status, 200);
    assert.equal((await request('/', 'GET', { Host: 'evil.example' })).status, 403);
    assert.equal((await request('/identity/api/v1/users/me')).status, 403);
    assert.equal((await request('/identity/api/v1/users/me', 'GET', { 'X-Local-Auth-Test': '1', Origin: 'https://evil.example' })).status, 403);
    assert.equal((await request('/identity/api/v1/users/me?redirect=evil', 'GET', { 'X-Local-Auth-Test': '1' })).status, 404);
    assert.equal((await request('/identity/api/v1/users/withdraw', 'POST')).status, 404);
    assert.equal((await request('/identity/api/v1/auth/reissue', 'POST', { 'X-Local-Auth-Test': '1', 'Content-Type': 'application/json' }, '{}')).status, 403);
    const result = await request('/identity/api/v1/auth/reissue', 'POST', { Origin: origin, 'X-Local-Auth-Test': '1', 'Content-Type': 'application/json', 'Idempotency-Key': 'fake-test-id', Cookie: 'not-forwarded' }, '{}');
    assert.equal(result.status, 200); assert.equal(result.headers['cache-control'], 'no-store');
    assert.equal(result.headers['access-control-allow-origin'], undefined);
    assert.equal(calls.length, 1); assert.equal(calls[0][0], 'https://identity-test.to-teacher.com/api/v1/auth/reissue');
    assert.equal(calls[0][1].redirect, 'error'); assert.equal(calls[0][1].headers.cookie, undefined);
    assert.equal(calls[0][1].headers['idempotency-key'], 'fake-test-id');
    assert.equal((await request('/identity/api/v1/users/me/merges?activeOnly=false&limit=20', 'GET',
      { 'X-Local-Auth-Test': '1', Authorization: 'Bearer fake-member' })).status, 200);
    assert.equal(calls.at(-1)[0], 'https://identity-test.to-teacher.com/api/v1/users/me/merges?activeOnly=false&limit=20');
    assert.equal(calls.at(-1)[1].headers.authorization, 'Bearer fake-member');
    assert.equal((await request('/identity/api/v1/users/me/merges?redirect=evil', 'GET', { 'X-Local-Auth-Test': '1' })).status, 404);
    assert.equal((await request('/identity/api/v1/auth/reissue', 'POST', { Origin: origin, 'X-Local-Auth-Test': '1', 'Content-Type': 'application/json' }, 'x'.repeat(33000))).status, 413);
  } finally { await new Promise(resolve => server.close(resolve)); }
});
test('token pair rejects missing fields and summaries do not reveal identifiers', () => {
  assert.throws(() => tokenPair({ accessToken: 'fake' }));
  assert.deepEqual(tokenPair({ accessToken: 'fake-a', refreshToken: 'fake-b' }), { accessToken: 'fake-a', refreshToken: 'fake-b' });
  const token = payload => `fake.${Buffer.from(JSON.stringify(payload)).toString('base64url')}.fake`;
  const summary = claimSummary(token({ sub: '00000000-0000-4000-8000-000000000000', account_type: 'MEMBER', aud: ['tosunsaeng-learning-core'], exp: 1900000000 }));
  assert.equal(summary.subIsUuid, true); assert.equal(summary.member, true); assert.equal(summary.learningAudience, true); assert.equal(summary.sub, undefined);
  assert.equal(claimSummary(token({})).member, false);
  assert.equal(claimSummary(token({ account_type: 'GUEST' })).member, false);
});
