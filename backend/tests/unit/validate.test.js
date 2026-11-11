import test from 'node:test';
import assert from 'node:assert/strict';
import { validateLogin, validateRegister } from '../../src/lib/validate.js';

test('registro válido normaliza el correo', () => {
  const r = validateRegister({ email: '  Fer@Gmail.COM ', password: 'Abcdef12', displayName: ' Fer ' });
  assert.deepEqual(r.value, { email: 'fer@gmail.com', password: 'Abcdef12', displayName: 'Fer' });
});

test('registro con datos malos reporta cada campo', () => {
  const r = validateRegister({ email: 'no-es-correo', password: 'corta', displayName: '' });
  assert.deepEqual(Object.keys(r.errors).sort(), ['displayName', 'email', 'password']);
});

test('registro con cuerpo vacío o no objeto no truena', () => {
  assert.ok(validateRegister(undefined).errors);
  assert.ok(validateRegister('texto').errors);
  assert.ok(validateRegister({ email: 123, password: {}, displayName: [] }).errors);
});

test('contraseña de más de 20 caracteres se rechaza', () => {
  const r = validateRegister({ email: 'a@gmail.com', password: 'Aa1' + 'x'.repeat(18), displayName: 'A' });
  assert.ok(r.errors.password);
});

test('login exige correo y contraseña', () => {
  assert.ok(validateLogin({}).errors.email);
  assert.ok(validateLogin({ email: 'a@b.co' }).errors.password);
  assert.deepEqual(validateLogin({ email: 'A@B.co', password: 'x' }).value, { email: 'a@b.co', password: 'x' });
});
