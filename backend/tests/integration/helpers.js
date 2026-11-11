// Prepara el entorno de pruebas ANTES de cargar la app (la conexión se lee al importar).
import 'dotenv/config';

process.env.DB_NAME = process.env.DB_NAME_TEST || 'terrascope_test';
process.env.JWT_SECRET = process.env.JWT_SECRET || 'secreto-solo-para-pruebas-'.padEnd(48, 'x');
process.env.RATE_LIMIT_MAX = '1000';
// Las pruebas usan correos de dominios inventados (*.terrascope.invalid).
process.env.EXTRA_EMAIL_DOMAINS = 'terrascope.invalid';

export const { migrate } = await import('../../src/migrate.js');
export const { pool } = await import('../../src/db.js');
export const { default: app } = await import('../../src/app.js');

export function startServer() {
  return new Promise((resolve) => {
    const server = app.listen(0, () => resolve({ server, base: `http://127.0.0.1:${server.address().port}` }));
  });
}

export function makeClient(base) {
  return (path, { method = 'GET', body, token } = {}) =>
    fetch(base + path, {
      method,
      headers: { ...(body ? { 'content-type': 'application/json' } : {}), ...(token ? { authorization: `Bearer ${token}` } : {}) },
      body: body ? JSON.stringify(body) : undefined,
    });
}

export async function registerUser(call, email, displayName = 'Prueba') {
  const res = await call('/api/auth/register', { method: 'POST', body: { email, password: 'ContraseñaSegura123', displayName } });
  if (res.status !== 201) throw new Error(`No se pudo registrar ${email}: ${res.status}`);
  return (await res.json()).token;
}
