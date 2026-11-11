// Reglas de correo y contraseña para REGISTRARSE (el login no las aplica, para no dejar fuera cuentas existentes).
// Debe coincidir con android/.../ui/screen/AuthRules.kt.

export const EMAIL_MAX = 80;
export const PASSWORD_MIN = 8;
export const PASSWORD_MAX = 20;

// Proveedores de correo reconocidos.
const DOMAINS = new Set([
  'gmail.com', 'googlemail.com',
  'outlook.com', 'outlook.es', 'hotmail.com', 'hotmail.es', 'hotmail.com.mx', 'live.com', 'live.com.mx', 'msn.com',
  'yahoo.com', 'yahoo.com.mx', 'yahoo.es', 'ymail.com',
  'icloud.com', 'me.com', 'mac.com',
  'aol.com', 'proton.me', 'protonmail.com', 'zoho.com',
]);
// Correos institucionales (universidades).
const SUFFIXES = ['.edu', '.edu.mx'];

const EMAIL_RE = /^[a-z0-9._%+-]{1,64}@([a-z0-9-]+\.)+[a-z]{2,}$/;

function extraDomains() {
  return (process.env.EXTRA_EMAIL_DOMAINS || '').split(',').map((s) => s.trim().toLowerCase()).filter(Boolean);
}

export function isAllowedDomain(domain) {
  if (DOMAINS.has(domain)) return true;
  if (SUFFIXES.some((s) => domain.endsWith(s))) return true;
  return extraDomains().some((d) => domain === d || domain.endsWith(`.${d}`));
}

/** Mensaje de error o null si el correo es válido. `email` ya viene en minúsculas y sin espacios. */
export function emailError(email) {
  if (!email || email.length > EMAIL_MAX) return `El correo debe tener entre 1 y ${EMAIL_MAX} caracteres`;
  if (!EMAIL_RE.test(email)) return 'Correo no válido';
  if (!isAllowedDomain(email.slice(email.indexOf('@') + 1))) {
    return 'Usa un correo de un proveedor reconocido (Gmail, Outlook, Hotmail, Yahoo, iCloud…)';
  }
  return null;
}

/** Mensaje de error o null si la contraseña cumple. */
export function passwordError(password) {
  if (password.length < PASSWORD_MIN || password.length > PASSWORD_MAX) {
    return `La contraseña debe tener entre ${PASSWORD_MIN} y ${PASSWORD_MAX} caracteres`;
  }
  if (!/\p{Lu}/u.test(password)) return 'La contraseña debe tener al menos una mayúscula';
  if (!/[0-9]/.test(password)) return 'La contraseña debe tener al menos un número';
  return null;
}
