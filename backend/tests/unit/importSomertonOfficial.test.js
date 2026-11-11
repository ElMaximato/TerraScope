import test from 'node:test';
import assert from 'node:assert/strict';
import {
  cleanAcsNumber,
  computeCanonicalHash,
  compareExpectedCoverage,
  polygonIntersects,
  approximateOverlapMetrics,
  geometryGeometrySummary,
  EXPECTED_TRACTS,
  OFFICIAL_ZONE_NAMES,
  calculatePopulationDensity,
  validateExpectedHash,
  ACS_REFERENCE_PERIOD,
  TIGER_REFERENCE_PERIOD,
} from '../../scripts/import-somerton-official-data.js';

test('cleanAcsNumber convierte nulos y valores de rechazo en null', () => {
  assert.equal(cleanAcsNumber(null), null);
  assert.equal(cleanAcsNumber(''), null);
  assert.equal(cleanAcsNumber('-666666666'), null);
  assert.equal(cleanAcsNumber('-888888888'), null);
  assert.equal(cleanAcsNumber('-999999999'), null);
  assert.equal(cleanAcsNumber('2163'), 2163);
  assert.equal(cleanAcsNumber('31.3'), 31.3);
});

test('computeCanonicalHash es estable sin respetar orden de objetos', () => {
  const hashA = computeCanonicalHash({ b: 2, a: [1, 2] });
  const hashB = computeCanonicalHash({ a: [1, 2], b: 2 });
  assert.equal(hashA, hashB);
  assert.match(hashA, /^[a-f0-9]{64}$/);
});

test('compareExpectedCoverage separa faltantes y adicionales', () => {
  const result = compareExpectedCoverage(
    ['04027011501', '04027011502', '04027011503', '04027011403', '04027099999']
  );
  assert.deepEqual(result.missing, []);
  assert.deepEqual(result.additional, ['04027099999']);
  assert.equal(result.matchesExpected, false);
});

test('compareExpectedCoverage detecta tracts faltantes', () => {
  const result = compareExpectedCoverage(['04027011501', '04027011502']);
  assert.deepEqual(result.missing, ['04027011503', '04027011403']);
  assert.deepEqual(result.additional, []);
  assert.equal(result.matchesExpected, false);
});

test('polygonIntersects detecta geometrías superpuestas y separadas', () => {
  const a = { type: 'Polygon', coordinates: [[
    [-114.72, 32.58], [-114.68, 32.58], [-114.68, 32.62], [-114.72, 32.62], [-114.72, 32.58],
  ]] };
  const b = { type: 'Polygon', coordinates: [[
    [-114.70, 32.60], [-114.66, 32.60], [-114.66, 32.64], [-114.70, 32.64], [-114.70, 32.60],
  ]] };
  const c = { type: 'Polygon', coordinates: [[
    [-114.60, 32.50], [-114.58, 32.50], [-114.58, 32.52], [-114.60, 32.52], [-114.60, 32.50],
  ]] };
  assert.equal(polygonIntersects(a, b), true);
  assert.equal(polygonIntersects(a, c), false);
});

test('los GEOID y nombres oficiales están definidos sin texto inventado', () => {
  assert.deepEqual(EXPECTED_TRACTS, [
    '04027011501', '04027011502', '04027011503', '04027011403',
  ]);
  assert.deepEqual(OFFICIAL_ZONE_NAMES, {
    '04027011501': 'Census Tract 115.01',
    '04027011502': 'Census Tract 115.02',
    '04027011503': 'Census Tract 115.03',
    '04027011403': 'Census Tract 114.03',
    '0468080': 'Somerton city',
  });
});

test('resumen de geometría cuenta tipos, anillos y vértices', () => {
  const polygon = { type: 'Polygon', coordinates: [[[0, 0], [1, 0], [1, 1], [0, 0]]] };
  assert.deepEqual(geometryGeometrySummary(polygon), { type: 'Polygon', rings: 1, vertices: 4 });
  const multi = { type: 'MultiPolygon', coordinates: [[[[0, 0], [1, 0], [0, 0]], [[2, 2], [3, 3], [2, 2]]]] };
  assert.deepEqual(geometryGeometrySummary(multi), { type: 'MultiPolygon', rings: 2, vertices: 6 });
});

test('métricas de traslape devuelven ambas fracciones del área', () => {
  const tract = { type: 'Polygon', coordinates: [[
    [-114.72, 32.58], [-114.68, 32.58], [-114.68, 32.62], [-114.72, 32.62], [-114.72, 32.58],
  ]] };
  const place = { type: 'Polygon', coordinates: [[
    [-114.70, 32.60], [-114.66, 32.60], [-114.66, 32.64], [-114.70, 32.64], [-114.70, 32.60],
  ]] };
  const metrics = approximateOverlapMetrics(tract, place, 40);
  assert.ok(metrics.tractWithinPlacePct >= 0 && metrics.tractWithinPlacePct <= 100);
  assert.ok(metrics.placeContributionPct >= 0 && metrics.placeContributionPct <= 100);
});

test('calcula population_density por AREALAND en km² con MOE nulo', () => {
  assert.equal(calculatePopulationDensity(3000, 1_000_000), 3000);
  assert.equal(calculatePopulationDensity('-666666666', 1_000_000), null);
  assert.equal(calculatePopulationDensity(3000, 0), null);
});

test('usa los períodos ACS y TIGER aprobados', () => {
  assert.equal(ACS_REFERENCE_PERIOD, 'ACS 5-year 2020–2024');
  assert.equal(TIGER_REFERENCE_PERIOD, 'TIGERweb ACS 2024');
});

test('permite omitir el hash y rechaza un hash distinto antes de escribir', () => {
  assert.equal(validateExpectedHash('a'.repeat(64), null), true);
  assert.throws(
    () => validateExpectedHash('a'.repeat(64), 'b'.repeat(64)),
    /Hash de lote distinto/
  );
  assert.equal(validateExpectedHash('A'.repeat(64), 'a'.repeat(64)), true);
});
