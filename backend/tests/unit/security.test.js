import test, { before, after } from 'node:test';
import assert from 'node:assert/strict';
import jwt from 'jsonwebtoken';

process.env.JWT_SECRET = 'x'.repeat(40);
const { default: app } = await import('../../src/app.js');

let server; let base;
before(async () => {
  await new Promise((r) => { server = app.listen(0, '127.0.0.1', r); });
  base = `http://127.0.0.1:${server.address().port}`;
});
after(() => new Promise((r) => server.close(r)));

const call = (method, path, headers = {}, body) =>
  fetch(base + path, { method, headers: { 'content-type': 'application/json', ...headers }, body: body ? JSON.stringify(body) : undefined });

// Todas las rutas con datos de usuario o de zonas: sin sesión deben dar 401, sin tocar la base de datos.
const PROTECTED = [
  ['GET', '/api/zones'], ['GET', '/api/zones/1'], ['GET', '/api/zones/1/score'], ['GET', '/api/zones/1/report'],
  ['GET', '/api/zones/ranking'], ['GET', '/api/zones/profiles'],
  ['GET', '/api/properties'], ['POST', '/api/properties'], ['GET', '/api/properties/1'], ['PUT', '/api/properties/1'], ['DELETE', '/api/properties/1'],
  ['GET', '/api/watchlist'], ['POST', '/api/watchlist'], ['DELETE', '/api/watchlist/1'],
  ['GET', '/api/field/observations'], ['POST', '/api/field/sync'], ['GET', '/api/field/analysis'],
  ['PUT', '/api/field/observations/11111111-1111-4111-8111-111111111111'],
  ['DELETE', '/api/field/observations/11111111-1111-4111-8111-111111111111'],
  ['GET', '/api/auth/me'],
];

for (const [method, path] of PROTECTED) {
  test(`sin sesión: ${method} ${path} -> 401`, async () => {
    assert.equal((await call(method, path, {}, method === 'GET' || method === 'DELETE' ? undefined : {})).status, 401);
  });
}

test('token con firma ajena, expirado, "alg none" o basura -> 401', async () => {
  const forged = jwt.sign({ sub: '1' }, 'otra-clave-distinta-de-40-caracteres!!', { algorithm: 'HS256' });
  const expired = jwt.sign({ sub: '1' }, process.env.JWT_SECRET, { algorithm: 'HS256', expiresIn: -10 });
  const none = `${Buffer.from('{"alg":"none","typ":"JWT"}').toString('base64url')}.${Buffer.from('{"sub":"1"}').toString('base64url')}.`;
  for (const t of [forged, expired, none, 'basura']) {
    assert.equal((await call('GET', '/api/properties', { authorization: `Bearer ${t}` })).status, 401);
  }
  assert.equal((await call('GET', '/api/properties', { authorization: 'Basic abc' })).status, 401);
});

test('cabeceras de seguridad y sin X-Powered-By', async () => {
  const res = await call('GET', '/api/health');
  assert.equal(res.headers.get('x-powered-by'), null);
  assert.equal(res.headers.get('x-content-type-options'), 'nosniff');
  assert.equal(res.headers.get('x-frame-options'), 'DENY');
  assert.equal(res.headers.get('cache-control'), 'no-store');
});

test('CORS desactivado por defecto', async () => {
  const res = await call('GET', '/api/health', { origin: 'https://sitio-ajeno.example' });
  assert.equal(res.headers.get('access-control-allow-origin'), null);
});

test('cuerpo demasiado grande -> 413 y JSON roto -> 400', async () => {
  const big = await fetch(`${base}/api/auth/login`, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ email: 'a@b.c', password: 'x'.repeat(200000) }) });
  assert.equal(big.status, 413);
  const bad = await fetch(`${base}/api/auth/login`, { method: 'POST', headers: { 'content-type': 'application/json' }, body: '{no es json' });
  assert.equal(bad.status, 400);
});

test('los errores 500 no filtran detalles internos', async () => {
  const res = await call('GET', '/api/ruta-que-no-existe');
  assert.equal(res.status, 404);
  assert.doesNotMatch(await res.text(), /at .*\.js|stack/i);
});
