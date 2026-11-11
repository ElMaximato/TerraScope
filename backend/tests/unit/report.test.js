import test from 'node:test';
import assert from 'node:assert/strict';
import { buildZoneReport } from '../../src/lib/report.js';
import { computeTractScores } from '../../src/lib/score.js';

const mk = (id, p, inc, h, d, moe = null) => ({
  id, code: `c${id}`, name: `Zona ${id}`, level: 'tract', source: null,
  indicators: [
    { code: 'population', name: 'Población', value: p, marginOfError: moe, unit: 'personas', referencePeriod: '2023', source: { organization: 'Census', name: 'ACS 5-year' } },
    { code: 'median_income', name: 'Ingreso', value: inc, marginOfError: null, unit: 'USD', referencePeriod: '2023', source: null },
    { code: 'households', name: 'Hogares', value: h, marginOfError: null, unit: 'hogares', referencePeriod: '2023', source: null },
    { code: 'population_density', name: 'Densidad', value: d, marginOfError: null, unit: 'hab/km2', referencePeriod: '2023', source: null },
  ],
});
const tracts = [mk(1, 100, 10, 10, 10), mk(2, 200, 20, 20, 20), mk(3, 300, 30, 30, 30)];
const build = (id, profileId = 'retail') => {
  const all = computeTractScores(tracts, profileId);
  const score = { ...all.get(id), allScores: tracts.map((t) => ({ id: t.id, score: all.get(t.id).score })) };
  return buildZoneReport({ zone: tracts.find((t) => t.id === id), tracts, score, profileId, generatedAt: new Date(0) });
};

test('la mejor zona: resumen con puntaje y posición, factores a favor', () => {
  const r = build(3);
  assert.match(r.summary, /100\/100/);
  assert.match(r.summary, /1\.º de 3/);
  assert.equal(r.strengths.length, 4);
  assert.equal(r.warnings.filter((w) => w.ref).length, 0);
});

test('el puntaje trae su posición entre las zonas', () => {
  assert.deepEqual(build(3).score.rank, { position: 1, of: 3 });
  assert.deepEqual(build(1).score.rank, { position: 3, of: 3 });
});

test('la peor zona: advertencias por factor bajo', () => {
  const r = build(1);
  assert.equal(r.score.value, 0);
  assert.equal(r.warnings.filter((w) => w.ref).length, 4);
});

test('es reproducible: mismos datos, mismo reporte', () => {
  assert.deepEqual(build(2), build(2));
});

test('confianza baja genera advertencia', () => {
  const t = [mk(1, 100, 10, 10, 10, 90), mk(2, 200, 20, 20, 20), mk(3, 300, 30, 30, 30)];
  const all = computeTractScores(t, 'retail');
  const r = buildZoneReport({ zone: t[0], tracts: t, score: all.get(1), profileId: 'retail' });
  assert.ok(r.warnings.some((w) => /Confianza baja/.test(w.text)));
});

test('sin puntaje: no inventa cifras', () => {
  const two = tracts.slice(0, 2);
  const all = computeTractScores(two, 'retail');
  const r = buildZoneReport({ zone: two[0], tracts: two, score: all.get(1), profileId: 'retail' });
  assert.equal(r.score, null);
  assert.doesNotMatch(r.summary, /\d+\/100/);
});

test('toda cifra del resumen sale de los datos', () => {
  const r = build(2);
  const nums = (r.summary.match(/\d+/g) || []).map(Number);
  const allowed = new Set([r.score.value, 100, 1, 2, 3]);
  for (const n of nums) assert.ok(allowed.has(n), `cifra inesperada ${n}`);
});
