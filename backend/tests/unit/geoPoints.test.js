import test from 'node:test';
import assert from 'node:assert/strict';
import { pointsInside } from '../../src/lib/geoPoints.js';

// Polígono en forma de "L": el centro de su caja envolvente cae FUERA.
const L = { type: 'Polygon', coordinates: [[[0, 0], [10, 0], [10, 2], [2, 2], [2, 10], [0, 10], [0, 0]]] };
const inL = ([x, y]) => (x >= 0 && x <= 10 && y >= 0 && y <= 2) || (x >= 0 && x <= 2 && y >= 0 && y <= 10);

test('todos los puntos quedan dentro, incluso en un polígono cóncavo', () => {
  const pts = pointsInside(L, 6);
  assert.ok(pts.length >= 3);
  for (const p of pts) assert.ok(inL([p.lon, p.lat]), `fuera: ${p.lon},${p.lat}`);
});

test('MultiPolygon: usa el polígono principal', () => {
  const multi = { type: 'MultiPolygon', coordinates: [[[[100, 100], [100.1, 100], [100.1, 100.1], [100, 100]]], L.coordinates] };
  for (const p of pointsInside(multi, 4)) assert.ok(inL([p.lon, p.lat]));
});

test('geometría vacía o desconocida no rompe', () => {
  assert.deepEqual(pointsInside(null), []);
  assert.deepEqual(pointsInside({ type: 'Point', coordinates: [0, 0] }), []);
});
