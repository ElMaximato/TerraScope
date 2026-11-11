import test from 'node:test';
import assert from 'node:assert/strict';
import { buildAnalysis, analysisAllowedNumbers } from '../../src/lib/analysis.js';
import { generateAiAnalysis } from '../../src/lib/aiSummary.js';

const mk = (id, p, inc, h, d) => ({ id, name: `Z${id}`, indicators: [
  { code: 'population', value: p, marginOfError: null }, { code: 'median_income', value: inc, marginOfError: null },
  { code: 'households', value: h, marginOfError: null }, { code: 'population_density', value: d, marginOfError: null }] });
const tracts = [mk(1, 100, 10, 10, 10), mk(2, 200, 20, 20, 20), mk(3, 300, 30, 30, 30)];
const locs = [
  { kind: 'point', id: 'a', name: 'Terreno A', tag: 'land_potential', lat: 1, lon: 1, tractId: 1 },
  { kind: 'point', id: 'b', name: 'Local B', tag: 'commercial_space', lat: 1, lon: 1, tractId: 3 },
  { kind: 'property', id: '9', name: 'Casa C', lat: 1, lon: 1, tractId: null },
];

test('ordena por puntaje y marca las que quedan fuera', () => {
  const a = buildAnalysis({ locations: locs, tracts, profileId: 'retail', generatedAt: new Date(0) });
  assert.deepEqual(a.items.map((i) => i.name), ['Local B', 'Terreno A', 'Casa C']);
  assert.equal(a.items[0].score, 100);
  assert.equal(a.items[2].status, 'outside');
  assert.match(a.summary, /"Local B" \(100\/100\)/);
});

test('sin ubicaciones: mensaje y nada que analizar con IA', async () => {
  const a = buildAnalysis({ locations: [], tracts, profileId: 'retail' });
  assert.equal(a.items.length, 0);
  const r = await generateAiAnalysis(a, { env: { GEMINI_API_KEY: 'k' }, fetchImpl: async () => { throw new Error('no debe llamarse'); } });
  assert.equal(r.status, 'skipped');
});

test('IA: acepta cifras de los datos, rechaza inventadas', async () => {
  const a = buildAnalysis({ locations: locs, tracts, profileId: 'retail' });
  const f = (text) => async () => ({ ok: true, json: async () => ({ candidates: [{ content: { parts: [{ text }] } }] }) });
  const good = await generateAiAnalysis(a, { env: { GEMINI_API_KEY: 'k' }, fetchImpl: f('Local B (100) es la mejor; Terreno A tiene 0.'), now: 1e12 });
  assert.equal(good.status, 'ok');
  const bad = await generateAiAnalysis(a, { env: { GEMINI_API_KEY: 'k' }, fetchImpl: f('Local B rinde 37 % anual.'), now: 2e12 });
  assert.equal(bad.status, 'rejected');
  assert.ok(analysisAllowedNumbers(a).has(100));
});
