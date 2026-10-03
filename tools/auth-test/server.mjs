import http from 'node:http';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';

const identity = 'https://identity-test.to-teacher.com';
const learning = 'https://api-test.to-teacher.com';
export const routes = new Map([
  ['POST /identity/api/v1/auth/account-recovery/prepare', identity],
  ['POST /identity/api/v1/auth/account-recovery/lookup', identity],
  ['POST /identity/api/v1/auth/firebase/exchange', identity],
  ['POST /identity/api/v1/auth/firebase/signup', identity],
  ['POST /identity/api/v1/auth/reissue', identity],
  ['GET /identity/api/v1/users/me', identity],
  ['GET /identity/api/v1/users/me/merges?activeOnly=false&limit=20', identity],
  ['GET /learning/api/v1/challenges/today', learning],
]);
const assets = new Map([['/', ['index.html', 'text/html']], ['/app.js', ['app.js', 'text/javascript']], ['/session.mjs', ['session.mjs', 'text/javascript']]]);

export function createServer(upstreamFetch = fetch) {
  return http.createServer(async (req, res) => {
    const send = (status, body, type = 'application/json') => {
      res.writeHead(status, { 'Content-Type': `${type}; charset=utf-8`, 'Cache-Control': 'no-store',
        'X-Content-Type-Options': 'nosniff', 'Referrer-Policy': 'no-referrer', 'X-Frame-Options': 'DENY' });
      res.end(body);
    };
    const deny = status => send(status, JSON.stringify({ code: 'LOCAL_REQUEST_REJECTED' }));
    try {
      const origin = `http://localhost:${res.socket.localPort}`;
      // Host validation prevents DNS rebinding; no CORS headers are emitted.
      if (req.headers.host !== new URL(origin).host) return deny(403);
      if (req.headers.origin && req.headers.origin !== origin) return deny(403);
      if (req.headers['sec-fetch-site'] === 'cross-site') return deny(403);
      const asset = assets.get(req.url);
      if (asset && req.method === 'GET') return send(200, await readFile(new URL(asset[0], import.meta.url)), asset[1]);
      const target = routes.get(`${req.method} ${req.url}`);
      if (!target) return deny(404);
      if (req.headers['x-local-auth-test'] !== '1') return deny(403);
      if (req.method === 'POST' && (req.headers.origin !== origin || req.headers['content-type'] !== 'application/json')) return deny(403);
      let body = '';
      for await (const chunk of req) {
        body += chunk.toString();
        if (Buffer.byteLength(body) > 32768) return deny(413);
      }
      const headers = { 'Content-Type': 'application/json' };
      for (const key of ['authorization', 'idempotency-key']) if (req.headers[key]) headers[key] = req.headers[key];
      const path = req.url.replace(/^\/(identity|learning)/, '');
      const response = await upstreamFetch(target + path, {
        method: req.method, headers, body: req.method === 'POST' ? body : undefined,
        redirect: 'error', signal: AbortSignal.timeout(20000),
      });
      // Never log request/response bodies, headers, credentials or upstream exceptions.
      send(response.status, await response.text());
    } catch {
      if (!res.headersSent) send(502, '{"code":"LOCAL_UPSTREAM_UNAVAILABLE"}');
      else res.end();
    }
  });
}
if (process.argv[1] === fileURLToPath(import.meta.url)) {
  createServer().listen(4173, '127.0.0.1', () => console.log('Auth test: http://localhost:4173 — stop with Ctrl+C'));
}
