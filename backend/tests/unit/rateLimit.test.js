import test from 'node:test';
import assert from 'node:assert/strict';
import { createRateLimiter } from '../../src/middleware/rateLimit.js';

test('permite hasta max y luego bloquea', () => {
  const rl = createRateLimiter({ windowMs: 1000, max: 3, now: () => 0 });
  assert.equal(rl.hit('a').allowed, true);
  assert.equal(rl.hit('a').allowed, true);
  assert.equal(rl.hit('a').allowed, true);
  assert.equal(rl.hit('a').allowed, false);
});

test('la ventana se reinicia con el tiempo', () => {
  let t = 0;
  const rl = createRateLimiter({ windowMs: 1000, max: 1, now: () => t });
  assert.equal(rl.hit('a').allowed, true);
  assert.equal(rl.hit('a').allowed, false);
  t = 1001;
  assert.equal(rl.hit('a').allowed, true);
});

test('las claves son independientes y retryAfter es positivo', () => {
  const rl = createRateLimiter({ windowMs: 5000, max: 1, now: () => 0 });
  rl.hit('a');
  assert.equal(rl.hit('b').allowed, true);
  const blocked = rl.hit('a');
  assert.equal(blocked.allowed, false);
  assert.ok(blocked.retryAfterSec >= 1);
});
