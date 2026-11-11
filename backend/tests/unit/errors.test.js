import test from 'node:test';
import assert from 'node:assert/strict';
import { AppError, asyncHandler, badRequest, toErrorBody, unauthorized } from '../../src/lib/errors.js';

test('toErrorBody usa la forma { error: { code, message } }', () => {
  assert.deepEqual(toErrorBody(unauthorized('x')), { error: { code: 'unauthorized', message: 'x' } });
});

test('toErrorBody incluye details cuando existen', () => {
  const body = toErrorBody(badRequest('mal', { email: 'no' }));
  assert.deepEqual(body.error.details, { email: 'no' });
  assert.equal(body.error.code, 'validation_error');
});

test('asyncHandler envía el error rechazado a next', async () => {
  const boom = new AppError(418, 'teapot', 'soy tetera');
  let received;
  await asyncHandler(async () => { throw boom; })({}, {}, (e) => { received = e; });
  await new Promise((r) => setImmediate(r));
  assert.equal(received, boom);
});
