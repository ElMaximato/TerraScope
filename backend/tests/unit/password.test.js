import test from 'node:test';
import assert from 'node:assert/strict';
import { hashPassword, verifyPassword } from '../../src/lib/password.js';

test('el hash no contiene la contraseña y usa formato scrypt', async () => {
  const h = await hashPassword('MiClaveSegura1');
  assert.ok(h.startsWith('scrypt$'));
  assert.ok(!h.includes('MiClaveSegura1'));
});

test('dos hashes de la misma contraseña son distintos (sal aleatoria)', async () => {
  assert.notEqual(await hashPassword('igual12345'), await hashPassword('igual12345'));
});

test('verifica la contraseña correcta e incorrecta', async () => {
  const h = await hashPassword('correcta123');
  assert.equal(await verifyPassword('correcta123', h), true);
  assert.equal(await verifyPassword('incorrecta1', h), false);
});

test('un hash malformado devuelve false sin lanzar', async () => {
  assert.equal(await verifyPassword('x', 'basura'), false);
  assert.equal(await verifyPassword('x', ''), false);
  assert.equal(await verifyPassword('x', 'bcrypt$a$b$c$d$e'), false);
});
