import test from 'node:test';
import assert from 'node:assert/strict';
import { emailError, passwordError, EMAIL_MAX } from '../../src/lib/credentialPolicy.js';

test('acepta proveedores reconocidos', () => {
  for (const e of ['fer@gmail.com', 'fer.b+x@outlook.com', 'a_b@hotmail.com', 'x@yahoo.com.mx', 'x@icloud.com', 'alumno@ut.edu.mx', 'x@mit.edu'])
    assert.equal(emailError(e), null, e);
});

test('rechaza dominios no reconocidos o inventados', () => {
  for (const e of ['fer@example.com', 'fer@hola', 'fer@hola.com', 'fer@gmial.com', 'fer@test.com', 'fer@gmail.com.evil.io'])
    assert.ok(emailError(e), e);
});

test('rechaza formato roto', () => {
  for (const e of ['', 'sin-arroba', '@gmail.com', 'a b@gmail.com', 'a@@gmail.com', 'a@gmail', 'a@.com'])
    assert.ok(emailError(e), e);
});

test('límite de 80 caracteres en el correo', () => {
  const ok = 'a'.repeat(EMAIL_MAX - '@gmail.com'.length) + '@gmail.com';
  assert.equal(ok.length, EMAIL_MAX);
  // 80 exactos con parte local de 70 supera los 64 permitidos: se rechaza por formato, no por largo.
  assert.ok(emailError(ok));
  assert.ok(emailError('a'.repeat(80) + '@gmail.com'));
  assert.equal(emailError('a'.repeat(64) + '@gmail.com'), null);
});

test('contraseña: 8 a 20, una mayúscula y un número', () => {
  assert.equal(passwordError('Abcdef12'), null);
  assert.equal(passwordError('Ñandu1234'), null);
  assert.equal(passwordError('A1' + 'b'.repeat(18)), null);
  assert.ok(passwordError('Abc12'), 'muy corta');
  assert.ok(passwordError('A1' + 'b'.repeat(19)), 'más de 20');
  assert.ok(passwordError('abcdefg1'), 'sin mayúscula');
  assert.ok(passwordError('Abcdefgh'), 'sin número');
});

test('EXTRA_EMAIL_DOMAINS permite dominios de prueba', () => {
  process.env.EXTRA_EMAIL_DOMAINS = 'terrascope.invalid';
  assert.equal(emailError('a@test.terrascope.invalid'), null);
  delete process.env.EXTRA_EMAIL_DOMAINS;
  assert.ok(emailError('a@test.terrascope.invalid'));
});
