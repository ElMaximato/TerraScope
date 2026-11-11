import test from 'node:test';
import assert from 'node:assert/strict';
import { generateAiSummary, isGrounded, numbersIn } from '../../src/lib/aiSummary.js';

const report = {
  zone: { name: 'Zona 9' }, profile: { label: 'Comercio minorista' }, summary: 'x',
  score: { value: 75, confidence: 'high', factors: [{ weight: 30, points: 22.5 }] },
  indicators: [{ name: 'Población', value: 4210, unit: 'personas', marginOfError: 310, variationPct: 4.5, referencePeriod: '2023', rank: { position: 1, of: 5 } }],
  strengths: [], warnings: [], missingData: [], limitations: ['l'],
};
const okFetch = (text) => async () => ({ ok: true, json: async () => ({ candidates: [{ content: { parts: [{ text }] } }] }) });
let t = 0;
const opts = (f) => ({ env: { GEMINI_API_KEY: 'k' }, fetchImpl: f, now: (t += 7200000) });

test('lee cifras con comas y decimales', () => {
  assert.deepEqual(numbersIn('4,210 personas y 4.5 %'), [4210, 4.5]);
});

test('sin clave: deshabilitado, sin texto', async () => {
  const r = await generateAiSummary(report, { env: {} });
  assert.deepEqual(r, { status: 'disabled', text: null });
});

test('texto con cifras de los datos: aceptado', async () => {
  const r = await generateAiSummary(report, opts(okFetch('La zona tiene 4,210 personas y obtiene 75 de 100, 1.º de 5. Hay margen de error.')));
  assert.equal(r.status, 'ok');
});

test('cifra inventada: rechazado', async () => {
  const r = await generateAiSummary(report, opts(okFetch('La zona tiene 9,999 personas y puntaje 75.')));
  assert.deepEqual(r, { status: 'rejected', text: null });
});

test('error del proveedor: sin texto, no rompe', async () => {
  assert.equal((await generateAiSummary(report, opts(async () => ({ ok: false })))).status, 'error');
  assert.equal((await generateAiSummary(report, opts(async () => { throw new Error('red'); }))).status, 'error');
});

test('isGrounded permite texto sin cifras', () => {
  assert.ok(isGrounded('Es una zona con buena demanda pero datos limitados.', report));
});

test('Gemini: usa x-goog-api-key y el endpoint generateContent', async () => {
  let seen;
  const f = async (url, init) => { seen = { url, init }; return { ok: true, json: async () => ({ candidates: [{ content: { parts: [{ text: 'Zona con 75 puntos.' }] } }] }) }; };
  const r = await generateAiSummary(report, { env: { GEMINI_API_KEY: 'abc', GEMINI_MODEL: 'm-test' }, fetchImpl: f, now: (t += 7200000) });
  assert.equal(r.status, 'ok');
  assert.match(seen.url, /generativelanguage\.googleapis\.com\/v1beta\/models\/m-test:generateContent$/);
  assert.equal(seen.init.headers['x-goog-api-key'], 'abc');
});

test('503 pasajero: reintenta y termina bien', async () => {
  let calls = 0;
  const f = async () => {
    calls++;
    if (calls < 3) return { ok: false, status: 503, text: async () => 'busy' };
    return { ok: true, json: async () => ({ candidates: [{ content: { parts: [{ text: 'Zona con 75 puntos.' }] } }] }) };
  };
  const r = await generateAiSummary(report, { env: { GEMINI_API_KEY: 'k' }, fetchImpl: f, now: (t += 7200000), sleep: async () => {} });
  assert.equal(r.status, 'ok');
  assert.equal(calls, 3);
});

test('503 persistente: usa el modelo de respaldo si existe', async () => {
  const urls = [];
  const f = async (url) => {
    urls.push(url);
    if (/primario/.test(url)) return { ok: false, status: 503, text: async () => 'busy' };
    return { ok: true, json: async () => ({ candidates: [{ content: { parts: [{ text: 'Zona con 75 puntos.' }] } }] }) };
  };
  const r = await generateAiSummary(report, { env: { GEMINI_API_KEY: 'k', GEMINI_MODEL: 'primario', GEMINI_FALLBACK_MODEL: 'respaldo' }, fetchImpl: f, now: (t += 7200000), sleep: async () => {} });
  assert.equal(r.status, 'ok');
  assert.match(urls.at(-1), /respaldo/);
});

test('error no reintentable (clave mala): falla de inmediato', async () => {
  let calls = 0;
  const f = async () => { calls++; return { ok: false, status: 403, text: async () => 'no' }; };
  const r = await generateAiSummary(report, { env: { GEMINI_API_KEY: 'k' }, fetchImpl: f, now: (t += 7200000), sleep: async () => {} });
  assert.equal(r.status, 'error');
  assert.equal(calls, 1);
});
