// Requiere MySQL y una base de pruebas vacía llamada terrascope_test (o DB_NAME_TEST).
// Crea y borra sus propios usuarios (@test.terrascope.invalid). No toca la base real.
import 'dotenv/config';
import test, { before, after } from 'node:test';
import assert from 'node:assert/strict';

process.env.DB_NAME = process.env.DB_NAME_TEST || 'terrascope_test';
process.env.JWT_SECRET = process.env.JWT_SECRET || 'secreto-solo-para-pruebas-'.padEnd(48, 'x');
process.env.RATE_LIMIT_MAX = '1000';

const { migrate } = await import('../../src/migrate.js');
const { pool } = await import('../../src/db.js');
const { default: app } = await import('../../src/app.js');

let server, base;
const email = `fer-${Date.now()}@test.terrascope.invalid`;
const password = 'ContraseñaSegura123';

const call = (path, { method = 'GET', body, token, raw } = {}) =>
  fetch(base + path, {
    method,
    headers: { ...(body || raw ? { 'content-type': 'application/json' } : {}), ...(token ? { authorization: `Bearer ${token}` } : {}) },
    body: raw ?? (body ? JSON.stringify(body) : undefined),
  });

before(async () => {
  await migrate();
  await new Promise((resolve) => { server = app.listen(0, resolve); });
  base = `http://127.0.0.1:${server.address().port}`;
});

after(async () => {
  await pool.query("DELETE FROM users WHERE email LIKE '%@test.terrascope.invalid'");
  await new Promise((resolve) => server.close(resolve));
  await pool.end();
});

let token;

test('registro crea usuario y no expone el hash', async () => {
  const res = await call('/api/auth/register', { method: 'POST', body: { email, password, displayName: 'Fer' } });
  assert.equal(res.status, 201);
  const json = await res.json();
  assert.ok(json.token);
  assert.equal(json.user.email, email);
  assert.equal(json.user.password_hash, undefined);
  assert.equal(JSON.stringify(json).includes('scrypt$'), false);
  token = json.token;
});

test('la contraseña se guarda hasheada, no en texto plano', async () => {
  const [[row]] = await pool.query('SELECT password_hash FROM users WHERE email = ?', [email]);
  assert.ok(row.password_hash.startsWith('scrypt$'));
  assert.equal(row.password_hash.includes(password), false);
});

test('registrar el mismo correo da 409 email_taken', async () => {
  const res = await call('/api/auth/register', { method: 'POST', body: { email, password, displayName: 'Otro' } });
  assert.equal(res.status, 409);
  assert.equal((await res.json()).error.code, 'email_taken');
});

test('registro con datos inválidos da 400 con details', async () => {
  const res = await call('/api/auth/register', { method: 'POST', body: { email: 'x', password: '1', displayName: '' } });
  assert.equal(res.status, 400);
  const json = await res.json();
  assert.equal(json.error.code, 'validation_error');
  assert.ok(json.error.details.email);
});

test('login correcto devuelve token', async () => {
  const res = await call('/api/auth/login', { method: 'POST', body: { email, password } });
  assert.equal(res.status, 200);
  assert.ok((await res.json()).token);
});

test('login con contraseña mala y con correo inexistente dan el mismo 401', async () => {
  const bad = await call('/api/auth/login', { method: 'POST', body: { email, password: 'incorrecta123' } });
  const ghost = await call('/api/auth/login', { method: 'POST', body: { email: 'nadie@test.terrascope.invalid', password } });
  assert.equal(bad.status, 401);
  assert.equal(ghost.status, 401);
  assert.deepEqual(await bad.json(), await ghost.json());
});

test('/me sin token da 401', async () => {
  assert.equal((await call('/api/auth/me')).status, 401);
});

test('/me con token válido devuelve el usuario', async () => {
  const res = await call('/api/auth/me', { token });
  assert.equal(res.status, 200);
  assert.equal((await res.json()).user.email, email);
});

test('/me con token alterado da 401', async () => {
  const res = await call('/api/auth/me', { token: token.slice(0, -2) + 'xx' });
  assert.equal(res.status, 401);
});

test('JSON roto da 400 invalid_json y ruta inexistente da 404 con el mismo formato', async () => {
  const bad = await call('/api/auth/login', { method: 'POST', raw: '{no es json' });
  assert.equal(bad.status, 400);
  assert.equal((await bad.json()).error.code, 'invalid_json');
  const nf = await call('/api/no-existe');
  assert.equal(nf.status, 404);
  assert.equal((await nf.json()).error.code, 'not_found');
});
