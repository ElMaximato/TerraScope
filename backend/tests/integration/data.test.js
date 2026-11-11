// Requiere MySQL y la base de pruebas (terrascope_test). Usa su propio dominio de correos
// (@data.terrascope.invalid) y una zona de prueba, y los borra al terminar.
import test, { before, after } from 'node:test';
import assert from 'node:assert/strict';
import { app, makeClient, migrate, pool, registerUser, startServer } from './helpers.js';

const stamp = Date.now();
const EMAIL_DOMAIN = '@data.terrascope.invalid';
const ZONE_CODE = `TEST-ZONE-${stamp}`;
const U = (n) => `${String(n).padStart(8, '0')}-aaaa-4aaa-8aaa-${String(stamp).padStart(12, '0').slice(-12)}`; // UUID únicos por corrida
const SESSION = U(1), OBS1 = U(2), OBS2 = U(3);

let server, call, tokenA, tokenB, zoneId;
const inside = { name: 'Lote dentro', propertyType: 'land', lat: 32.6, lon: -114.71, areaValue: 1.5, areaUnit: 'acre', askingPrice: 85000, notes: 'prueba' };
const outside = { name: 'Lote fuera', lat: 32.7, lon: -114.71 };

before(async () => {
  await migrate();
  ({ server, call } = await (async () => { const s = await startServer(); return { server: s.server, call: makeClient(s.base) }; })());
  tokenA = await registerUser(call, `a-${stamp}${EMAIL_DOMAIN}`, 'A');
  tokenB = await registerUser(call, `b-${stamp}${EMAIL_DOMAIN}`, 'B');
  const [r] = await pool.query(
    `INSERT INTO zones (code, name, level, boundary) VALUES (?, 'Zona de prueba', 'tract',
     ST_GeomFromText('POLYGON((-114.72 32.59, -114.70 32.59, -114.70 32.61, -114.72 32.61, -114.72 32.59))', 4326, 'axis-order=long-lat'))`,
    [ZONE_CODE]);
  zoneId = r.insertId;
});

after(async () => {
  await pool.query(`DELETE FROM users WHERE email LIKE '%${EMAIL_DOMAIN}'`);
  await pool.query('DELETE FROM zones WHERE code = ?', [ZONE_CODE]);
  if (server) await new Promise((resolve) => server.close(resolve));
  await pool.end();
});

test('la sesión de MySQL trabaja en UTC', async () => {
  const [[row]] = await pool.query('SELECT @@session.time_zone AS tz');
  assert.equal(row.tz, '+00:00');
});

test('todas las rutas de datos exigen token (401)', async () => {
  const routes = [['GET', '/api/zones'], ['GET', '/api/zones/indicators'], ['GET', '/api/zones/1'], ['GET', '/api/properties'], ['POST', '/api/properties'],
    ['GET', '/api/properties/1'], ['PUT', '/api/properties/1'], ['DELETE', '/api/properties/1'],
    ['GET', '/api/watchlist'], ['POST', '/api/watchlist'], ['DELETE', '/api/watchlist/1'],
    ['POST', '/api/field/sync'], ['GET', '/api/field/sessions'], ['GET', '/api/field/observations']];
  for (const [method, path] of routes) {
    const res = await call(path, { method, body: method === 'GET' || method === 'DELETE' ? undefined : {} });
    assert.equal(res.status, 401, `${method} ${path}`);
  }
});

// ---------- Propiedades ----------
let propA, propOut;

test('crear propiedad dentro de una zona la asocia automáticamente', async () => {
  const res = await call('/api/properties', { method: 'POST', token: tokenA, body: inside });
  assert.equal(res.status, 201);
  const { property } = await res.json();
  assert.equal(property.zoneId, zoneId);
  assert.equal(property.dataType, 'user');
  assert.equal(property.askingPrice, 85000);
  assert.ok(Math.abs(property.lat - 32.6) < 1e-6 && Math.abs(property.lon + 114.71) < 1e-6);
  propA = property.id;
});

test('crear propiedad fuera de toda zona deja zoneId en null', async () => {
  const res = await call('/api/properties', { method: 'POST', token: tokenA, body: outside });
  assert.equal(res.status, 201);
  const { property } = await res.json();
  assert.equal(property.zoneId, null);
  propOut = property.id;
});

test('propiedad inválida da 400 con details', async () => {
  const res = await call('/api/properties', { method: 'POST', token: tokenA, body: { name: '', lat: 999, lon: 0 } });
  assert.equal(res.status, 400);
  const json = await res.json();
  assert.ok(json.error.details.name && json.error.details.lat);
});

test('cada usuario solo ve sus propiedades', async () => {
  const a = await (await call('/api/properties', { token: tokenA })).json();
  const b = await (await call('/api/properties', { token: tokenB })).json();
  assert.equal(a.properties.length, 2);
  assert.equal(b.properties.length, 0);
});

test('otro usuario no puede leer, editar ni borrar la propiedad (404) y no se altera', async () => {
  assert.equal((await call(`/api/properties/${propA}`, { token: tokenB })).status, 404);
  assert.equal((await call(`/api/properties/${propA}`, { method: 'PUT', token: tokenB, body: { ...inside, name: 'HACKEADA' } })).status, 404);
  assert.equal((await call(`/api/properties/${propA}`, { method: 'DELETE', token: tokenB })).status, 404);
  const mine = await (await call(`/api/properties/${propA}`, { token: tokenA })).json();
  assert.equal(mine.property.name, 'Lote dentro');
});

test('id no numérico da 400', async () => {
  assert.equal((await call('/api/properties/abc', { token: tokenA })).status, 400);
});

test('editar mueve la propiedad y recalcula la zona', async () => {
  const res = await call(`/api/properties/${propA}`, { method: 'PUT', token: tokenA, body: { ...inside, lat: 32.7, askingPrice: 90000 } });
  assert.equal(res.status, 200);
  const { property } = await res.json();
  assert.equal(property.zoneId, null);
  assert.equal(property.askingPrice, 90000);
});

test('borrar propiedad propia da 204 y luego 404', async () => {
  assert.equal((await call(`/api/properties/${propOut}`, { method: 'DELETE', token: tokenA })).status, 204);
  assert.equal((await call(`/api/properties/${propOut}`, { token: tokenA })).status, 404);
});

// ---------- Zonas y Watchlist ----------
test('lista de zonas incluye la de prueba y no expone geometría', async () => {
  const { zones } = await (await call('/api/zones', { token: tokenA })).json();
  const z = zones.find((x) => x.id === zoneId);
  assert.ok(z);
  assert.equal(z.boundary, undefined);
});

test('catálogo de indicadores incluye demografía e ingresos', async () => {
  const res = await call('/api/zones/indicators', { token: tokenA });
  assert.equal(res.status, 200);
  const { indicators } = await res.json();
  assert.ok(Array.isArray(indicators) && indicators.length >= 4);
  const codes = indicators.map((i) => i.code);
  assert.ok(codes.includes('population'));
  assert.ok(codes.includes('median_income'));
});

test('detalle de zona devuelve geometría GeoJSON y estructura de indicadores', async () => {
  const res = await call(`/api/zones/${zoneId}`, { token: tokenA });
  assert.equal(res.status, 200);
  const { zone } = await res.json();
  assert.equal(zone.id, zoneId);
  assert.equal(zone.code, ZONE_CODE);
  assert.ok(zone.boundary);
  assert.equal(zone.boundary.type, 'Polygon');
  assert.ok(Array.isArray(zone.boundary.coordinates));
  assert.ok(Array.isArray(zone.indicators));
});

test('detalle de zona inexistente da 404 zone_not_found', async () => {
  const res = await call('/api/zones/2147483000', { token: tokenA });
  assert.equal(res.status, 404);
  assert.equal((await res.json()).error.code, 'zone_not_found');
});

test('watchlist: guardar (201), repetir (200 sin duplicar) y aislamiento entre usuarios', async () => {
  const first = await call('/api/watchlist', { method: 'POST', token: tokenA, body: { zoneId, note: 'primera' } });
  assert.equal(first.status, 201);
  const again = await call('/api/watchlist', { method: 'POST', token: tokenA, body: { zoneId, note: 'actualizada' } });
  assert.equal(again.status, 200);
  const { items } = await again.json();
  assert.equal(items.length, 1);
  assert.equal(items[0].note, 'actualizada');
  assert.equal(items[0].zone.code, ZONE_CODE);
  const b = await (await call('/api/watchlist', { token: tokenB })).json();
  assert.equal(b.items.length, 0);
});

test('watchlist: zona inexistente da 404 zone_not_found', async () => {
  const res = await call('/api/watchlist', { method: 'POST', token: tokenA, body: { zoneId: 2147483000 } });
  assert.equal(res.status, 404);
  assert.equal((await res.json()).error.code, 'zone_not_found');
});

test('watchlist: otro usuario no puede quitarla; el dueño sí', async () => {
  assert.equal((await call(`/api/watchlist/${zoneId}`, { method: 'DELETE', token: tokenB })).status, 404);
  assert.equal((await call(`/api/watchlist/${zoneId}`, { method: 'DELETE', token: tokenA })).status, 204);
  assert.equal((await call(`/api/watchlist/${zoneId}`, { method: 'DELETE', token: tokenA })).status, 404);
});

// ---------- Sincronización de campo ----------
const sentAt = '2026-11-06T08:15:30.123Z';
const batch = () => ({
  sessions: [{ id: SESSION, startedAt: '2026-11-06T08:00:00.000Z', device: 'reloj' }],
  observations: [
    { id: OBS1, sessionId: SESSION, tag: 'competitor', note: 'Tienda nueva', lat: 32.6, lon: -114.71, observedAt: sentAt },
    { id: OBS2, sessionId: SESSION, tag: 'road_access', lat: 32.601, lon: -114.711, observedAt: '2026-11-06T08:16:00.000Z' },
  ],
});
const count = async (table, id) => (await pool.query(`SELECT COUNT(*) AS n FROM ${table} WHERE id = ?`, [id]))[0][0].n;

test('sync: primer envío crea todo', async () => {
  const res = await call('/api/field/sync', { method: 'POST', token: tokenA, body: batch() });
  assert.equal(res.status, 200);
  const json = await res.json();
  assert.equal(json.summary.created, 3);
  assert.equal(json.summary.invalid, 0);
});

test('sync idempotente: reenviar el mismo lote no duplica nada', async () => {
  const res = await call('/api/field/sync', { method: 'POST', token: tokenA, body: batch() });
  const json = await res.json();
  assert.equal(json.summary.duplicate, 3);
  assert.equal(json.summary.created, 0);
  assert.equal(await count('field_observations', OBS1), 1);
  assert.equal(await count('field_sessions', SESSION), 1);
});

test('las observaciones conservan la hora exacta enviada (UTC, milisegundos)', async () => {
  const { observations } = await (await call(`/api/field/observations?sessionId=${SESSION}`, { token: tokenA })).json();
  assert.equal(observations.length, 2);
  const o = observations.find((x) => x.id === OBS1);
  assert.equal(new Date(o.observedAt).toISOString(), sentAt);
  assert.equal(o.tag, 'competitor');
  assert.ok(Math.abs(o.lat - 32.6) < 1e-6);
});

test('sync: cerrar la sesión después actualiza ended_at', async () => {
  const body = { sessions: [{ id: SESSION, startedAt: '2026-11-06T08:00:00.000Z', endedAt: '2026-11-06T09:00:00.000Z' }] };
  const json = await (await call('/api/field/sync', { method: 'POST', token: tokenA, body })).json();
  assert.equal(json.results.sessions[0].status, 'updated');
  const { sessions } = await (await call('/api/field/sessions', { token: tokenA })).json();
  assert.equal(new Date(sessions[0].endedAt).toISOString(), '2026-11-06T09:00:00.000Z');
  assert.equal(sessions[0].device, 'reloj'); // no se pierde el dispositivo
});

test('sync: otro usuario con el mismo id recibe conflict y no altera el original', async () => {
  const hostile = { observations: [{ id: OBS1, tag: 'observed_risk', note: 'SOBRESCRITA', lat: 1, lon: 1, observedAt: sentAt }] };
  const json = await (await call('/api/field/sync', { method: 'POST', token: tokenB, body: hostile })).json();
  assert.equal(json.results.observations[0].status, 'conflict');
  const [[row]] = await pool.query('SELECT note, tag FROM field_observations WHERE id = ?', [OBS1]);
  assert.equal(row.note, 'Tienda nueva');
  assert.equal(row.tag, 'competitor');
  const sessionConflict = await (await call('/api/field/sync', { method: 'POST', token: tokenB,
    body: { sessions: [{ id: SESSION, startedAt: sentAt, endedAt: '2026-11-06T10:00:00.000Z' }] } })).json();
  assert.equal(sessionConflict.results.sessions[0].status, 'conflict');
});

test('sync: no se puede colgar una observación de la sesión de otro usuario', async () => {
  const body = { observations: [{ id: U(9), sessionId: SESSION, tag: 'competitor', lat: 32.6, lon: -114.71, observedAt: sentAt }] };
  const json = await (await call('/api/field/sync', { method: 'POST', token: tokenB, body })).json();
  assert.equal(json.results.observations[0].status, 'invalid');
  assert.equal(await count('field_observations', U(9)), 0);
});

test('sync: un elemento inválido no bloquea a los válidos', async () => {
  const body = { observations: [
    { id: U(5), tag: 'inventado', lat: 32.6, lon: -114.71, observedAt: sentAt },
    { id: U(6), tag: 'construction', lat: 32.6, lon: -114.71, observedAt: sentAt }] };
  const res = await call('/api/field/sync', { method: 'POST', token: tokenA, body });
  assert.equal(res.status, 200);
  const json = await res.json();
  assert.equal(json.summary.invalid, 1);
  assert.equal(json.summary.created, 1);
  assert.ok(json.results.observations[0].details.tag);
});

test('sync: más de 100 elementos o lote vacío dan 400', async () => {
  const one = { id: U(7), tag: 'competitor', lat: 32.6, lon: -114.71, observedAt: sentAt };
  assert.equal((await call('/api/field/sync', { method: 'POST', token: tokenA, body: { observations: Array(101).fill(one) } })).status, 400);
  assert.equal((await call('/api/field/sync', { method: 'POST', token: tokenA, body: {} })).status, 400);
});

test('cada usuario solo lee sus propias observaciones y sesiones', async () => {
  const b = await (await call('/api/field/observations', { token: tokenB })).json();
  const bs = await (await call('/api/field/sessions', { token: tokenB })).json();
  assert.equal(b.observations.length, 0);
  assert.equal(bs.sessions.length, 0);
});

// ---------- Documentación ----------
test('la especificación OpenAPI se sirve sin token', async () => {
  const res = await call('/api/openapi.yaml');
  assert.equal(res.status, 200);
  assert.match(await res.text(), /^openapi: 3\.0/m);
});
