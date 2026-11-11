// Validadores simples sin dependencias. Devuelven { value } o { errors }.
import { emailError, passwordError } from './credentialPolicy.js';

function str(v) { return typeof v === 'string' ? v : ''; }

export function validateRegister(body) {
  const b = body && typeof body === 'object' ? body : {};
  const errors = {};
  const email = str(b.email).trim().toLowerCase();
  const password = str(b.password);
  const displayName = str(b.displayName).trim();

  const eErr = emailError(email);
  if (eErr) errors.email = eErr;
  const pErr = passwordError(password);
  if (pErr) errors.password = pErr;
  if (displayName.length < 1 || displayName.length > 80) errors.displayName = 'El nombre debe tener entre 1 y 80 caracteres';

  return Object.keys(errors).length ? { errors } : { value: { email, password, displayName } };
}

export function validateLogin(body) {
  const b = body && typeof body === 'object' ? body : {};
  const errors = {};
  const email = str(b.email).trim().toLowerCase();
  const password = str(b.password);
  if (!email) errors.email = 'Falta el correo';
  if (!password) errors.password = 'Falta la contraseña';
  return Object.keys(errors).length ? { errors } : { value: { email, password } };
}
