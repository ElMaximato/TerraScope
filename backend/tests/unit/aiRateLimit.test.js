import test from 'node:test';
import assert from 'node:assert/strict';

process.env.AI_RATE_LIMIT_MAX = '2';
const { aiRateLimit } = await import('../../src/middleware/rateLimit.js');

const run = (req) => { let err = null; let passed = false; aiRateLimit(req, { set() {} }, (e) => { err = e || null; passed = true; }); return { err, passed }; };

test('sin ?ai=1 no limita', () => {
  for (let i = 0; i < 20; i++) assert.equal(run({ query: {}, user: { id: 1 }, ip: 'x' }).err, null);
});

test('con ?ai=1 limita por usuario, no por ruta', () => {
  const req = (id) => ({ query: { ai: '1' }, user: { id }, ip: 'x', path: '/p' });
  assert.equal(run(req(7)).err, null);
  assert.equal(run(req(7)).err, null);
  assert.ok(run(req(7)).err, 'la tercera llamada del mismo usuario se bloquea');
  assert.equal(run(req(8)).err, null, 'otro usuario no se ve afectado');
});
