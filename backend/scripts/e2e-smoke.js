// Prueba de extremo a extremo contra el backend EN MARCHA (sin dependencias, solo HTTP).
//   npm start            (en otra terminal)
//   npm run e2e
// Simula lo que hacen el reloj y el teléfono: sincronizar puntos, reenviarlos (red inestable), cambiar la etiqueta,
// registrar propiedades y pedir reportes; y comprueba que un segundo usuario NO ve los datos del primero.
import { randomUUID } from 'node:crypto';
import { pointsInside } from '../src/lib/geoPoints.js';

const BASE = (process.env.E2E_BASE_URL || 'http://127.0.0.1:3000/api').replace(/\/$/, '');
let passed = 0, failed = 0;

async function api(method, path, { token, body } = {}) {
  const res = await fetch(BASE + path, {
    method,
    headers: { 'content-type': 'application/json', ...(token ? { authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  let json = null;
  try { json = await res.json(); } catch { /* sin cuerpo */ }
  return { status: res.status, json };
}

async function step(name, fn) {
  try { await fn(); passed++; console.log(`  ✓ ${name}`); }
  catch (e) { failed++; console.log(`  ✗ ${name}\n      ${e.message}`); }
}
const eq = (a, b, msg) => { if (a !== b) throw new Error(`${msg || 'valor'}: esperado ${JSON.stringify(b)}, recibido ${JSON.stringify(a)}`); };
const ok = (c, msg) => { if (!c) throw new Error(msg); };

const stamp = Date.now();
const pw = 'E2e-Prueba-12345';
// El registro solo acepta proveedores reconocidos; no se envía ningún correo, son solo cadenas de prueba.
const A = { email: `e2e.a.${stamp}@gmail.com`, token: null };
const B = { email: `e2e.b.${stamp}@gmail.com`, token: null };
let tractId, spot, propId;
const obsId = randomUUID();

console.log(`E2E contra ${BASE}\n`);

await step('el servidor responde y la base está conectada', async () => {
  const r = await api('GET', '/health');
  eq(r.status, 200, 'health');
  ok(r.json?.db?.connected === true, 'la base no está conectada (revisa MySQL y el .env)');
});

for (const u of [A, B]) {
  await step(`registro de ${u.email.split('@')[0]}`, async () => {
    const r = await api('POST', '/auth/register', { body: { email: u.email, password: pw, displayName: 'E2E' } });
    eq(r.status, 201, 'registro');
    u.token = r.json.token;
    ok(u.token, 'sin token');
  });
}

await step('login con contraseña mala -> 401; buena -> token', async () => {
  eq((await api('POST', '/auth/login', { body: { email: A.email, password: 'mala-contraseña' } })).status, 401, 'login malo');
  const r = await api('POST', '/auth/login', { body: { email: A.email, password: pw } });
  eq(r.status, 200, 'login');
  ok(r.json.token, 'sin token');
});

await step('hay zonas censales con polígono y se calcula un punto dentro', async () => {
  const z = await api('GET', '/zones', { token: A.token });
  eq(z.status, 200, 'zonas');
  const tract = z.json.zones.find((x) => x.level === 'tract');
  ok(tract, 'no hay tracts: ¿importaste los datos de Somerton?');
  tractId = tract.id;
  const d = await api('GET', `/zones/${tractId}`, { token: A.token });
  spot = pointsInside(d.json.zone?.boundary, 3)[0];
  ok(spot, 'no se pudo calcular un punto dentro de la zona');
});

await step('reloj -> backend: sincroniza un punto', async () => {
  const body = { sessions: [], observations: [{ id: obsId, tag: 'needs_research', note: null, lat: spot.lat, lon: spot.lon, observedAt: new Date().toISOString() }] };
  const r = await api('POST', '/field/sync', { token: A.token, body });
  eq(r.status, 200, 'sync');
  eq(r.json.results.observations[0].status, 'created', 'estado');
});

await step('red inestable: reenviar lo mismo no duplica', async () => {
  const body = { sessions: [], observations: [{ id: obsId, tag: 'needs_research', note: null, lat: spot.lat, lon: spot.lon, observedAt: new Date().toISOString() }] };
  const r = await api('POST', '/field/sync', { token: A.token, body });
  eq(r.json.results.observations[0].status, 'duplicate', 'estado');
  const list = await api('GET', '/field/observations', { token: A.token });
  eq(list.json.observations.filter((o) => o.id === obsId).length, 1, 'copias del punto');
});

await step('el reloj cambia la etiqueta y reenvía: se actualiza', async () => {
  const body = { sessions: [], observations: [{ id: obsId, tag: 'land_potential', note: null, lat: spot.lat, lon: spot.lon, observedAt: new Date().toISOString() }] };
  const r = await api('POST', '/field/sync', { token: A.token, body });
  eq(r.json.results.observations[0].status, 'updated', 'estado');
  const list = await api('GET', '/field/observations', { token: A.token });
  eq(list.json.observations.find((o) => o.id === obsId).tag, 'land_potential', 'etiqueta');
});

await step('teléfono: edita el nombre del punto', async () => {
  eq((await api('PUT', `/field/observations/${obsId}`, { token: A.token, body: { note: 'Terreno de prueba E2E' } })).status, 200, 'PUT');
  const list = await api('GET', '/field/observations', { token: A.token });
  eq(list.json.observations.find((o) => o.id === obsId).note, 'Terreno de prueba E2E', 'nota');
});

await step('registra una propiedad y queda asociada a una zona', async () => {
  const r = await api('POST', '/properties', { token: A.token, body: { name: 'Propiedad E2E', propertyType: 'land', lat: spot.lat, lon: spot.lon, askingPrice: 50000, areaValue: 1000, areaUnit: 'm2' } });
  eq(r.status, 201, 'crear');
  propId = r.json.property.id;
  eq(r.json.property.zoneId, tractId, 'zona asociada');
});

await step('edita la propiedad (PUT)', async () => {
  const r = await api('PUT', `/properties/${propId}`, { token: A.token, body: { name: 'Propiedad E2E editada', propertyType: 'commercial', lat: spot.lat, lon: spot.lon } });
  eq(r.status, 200, 'PUT');
  eq(r.json.property.name, 'Propiedad E2E editada', 'nombre');
  eq(r.json.property.askingPrice, null, 'el precio vacío se borra');
});

await step('puntaje, ranking y reporte de la zona', async () => {
  eq((await api('GET', `/zones/${tractId}/score?profile=retail`, { token: A.token })).status, 200, 'score');
  const rk = await api('GET', '/zones/ranking?profile=retail', { token: A.token });
  eq(rk.status, 200, 'ranking');
  ok(rk.json.ranking.items.length >= 1, 'ranking vacío');
  const rep = await api('GET', `/zones/${tractId}/report?profile=retail`, { token: A.token });
  eq(rep.status, 200, 'reporte');
  ok(typeof rep.json.report.summary === 'string', 'sin resumen');
});

await step('análisis de mis ubicaciones (sin IA)', async () => {
  const r = await api('GET', '/field/analysis?profile=retail', { token: A.token });
  eq(r.status, 200, 'analysis');
  ok(r.json.analysis.items.length >= 2, 'debería incluir el punto y la propiedad');
});

await step('zonas guardadas: agregar y quitar', async () => {
  eq((await api('POST', '/watchlist', { token: A.token, body: { zoneId: tractId } })).status < 300, true, 'agregar');
  const w = await api('GET', '/watchlist', { token: A.token });
  ok(w.json.items.some((i) => i.zoneId === tractId), 'no aparece en la lista');
  eq((await api('DELETE', `/watchlist/${tractId}`, { token: A.token })).status < 300, true, 'quitar');
});

// Aislamiento entre usuarios
await step('el usuario B NO ve los puntos ni las propiedades de A', async () => {
  const obs = await api('GET', '/field/observations', { token: B.token });
  eq(obs.json.observations.length, 0, 'puntos visibles para B');
  const props = await api('GET', '/properties', { token: B.token });
  eq(props.json.properties.length, 0, 'propiedades visibles para B');
  eq((await api('GET', `/properties/${propId}`, { token: B.token })).status, 404, 'GET propiedad ajena');
});

await step('el usuario B NO puede editar ni borrar lo de A', async () => {
  eq((await api('PUT', `/properties/${propId}`, { token: B.token, body: { name: 'hack', lat: spot.lat, lon: spot.lon } })).status, 404, 'PUT ajeno');
  eq((await api('DELETE', `/properties/${propId}`, { token: B.token })).status, 404, 'DELETE ajeno');
  eq((await api('PUT', `/field/observations/${obsId}`, { token: B.token, body: { note: 'hack' } })).status, 404, 'PUT punto ajeno');
  eq((await api('DELETE', `/field/observations/${obsId}`, { token: B.token })).status, 404, 'DELETE punto ajeno');
  const body = { sessions: [], observations: [{ id: obsId, tag: 'competitor', lat: spot.lat, lon: spot.lon, observedAt: new Date().toISOString() }] };
  eq((await api('POST', '/field/sync', { token: B.token, body })).json.results.observations[0].status, 'conflict', 'sync con id ajeno');
});

await step('limpieza: A borra su punto y su propiedad', async () => {
  eq((await api('DELETE', `/field/observations/${obsId}`, { token: A.token })).status, 200, 'borrar punto');
  eq((await api('DELETE', `/properties/${propId}`, { token: A.token })).status, 204, 'borrar propiedad');
});

console.log(`\n${passed} correctos, ${failed} con fallas.`);
console.log('Los usuarios e2e.*@gmail.com quedan en la base (no hay endpoint para borrar cuentas).');
process.exit(failed ? 1 : 0);
