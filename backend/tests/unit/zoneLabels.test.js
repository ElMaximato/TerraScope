import test from 'node:test';
import assert from 'node:assert/strict';
import { describeTracts, centroidOf, directionOf, pointInGeometry } from '../../src/lib/zoneLabels.js';

const sq = (x0, y0, x1, y1) => ({ type: 'Polygon', coordinates: [[[x0, y0], [x1, y0], [x1, y1], [x0, y1], [x0, y0]]] });
const I = (pop, inc, hh, den) => [{ code: 'population', value: pop }, { code: 'median_income', value: inc }, { code: 'households', value: hh }, { code: 'population_density', value: den }];

test('centroide y punto en polígono', () => {
  const g = sq(0, 0, 2, 2);
  assert.deepEqual(centroidOf(g).map((n) => Math.round(n * 1e6) / 1e6), [1, 1]);
  assert.equal(pointInGeometry([1, 1], g), true);
  assert.equal(pointInGeometry([3, 1], g), false);
});

test('direcciones cardinales', () => {
  assert.equal(directionOf({ east: 0, north: 5 }), 'norte');
  assert.equal(directionOf({ east: 5, north: 0 }), 'este');
  assert.equal(directionOf({ east: -3, north: -3 }), 'suroeste');
});

test('etiquetas: centro, residencial al norte y afueras al este', () => {
  const place = { geometry: sq(-114.72, 32.58, -114.68, 32.62) };
  const tracts = [
    { id: 1, code: 'a', name: 'Census Tract 1.01', geometry: sq(-114.705, 32.595, -114.695, 32.605), indicators: I(3000, 40000, 900, 2500) },
    { id: 2, code: 'b', name: 'Census Tract 2.01', geometry: sq(-114.715, 32.608, -114.685, 32.618), indicators: I(4000, 50000, 1200, 1500) },
    { id: 3, code: 'c', name: 'Census Tract 3.01', geometry: sq(-114.60, 32.58, -114.50, 32.62), indicators: I(2000, 35000, 600, 40) },
  ];
  const m = describeTracts(place, tracts);
  assert.equal(m.get(1).kind, 'Centro');
  assert.equal(m.get(2).kind, 'Residencial');
  assert.equal(m.get(2).direction, 'norte');
  assert.equal(m.get(3).kind, 'Afueras y campo');
  assert.equal(m.get(3).direction, 'este');
  assert.equal(m.get(2).highlights.length, 4);
  assert.match(m.get(3).description, /fuera de los límites/);
});

test('nombres repetidos se distinguen con el número del Census', () => {
  const place = { geometry: sq(0, 0, 1, 1) };
  const mk = (id, y) => ({ id, code: 'x', name: `Census Tract ${id}.01`, geometry: sq(5, y, 6, y + 0.01), indicators: I(1, 1, 1, 1) });
  const m = describeTracts(place, [mk(1, 0.5), mk(2, 0.52)]);
  assert.notEqual(m.get(1).title, m.get(2).title);
});

test('sin geometría de la ciudad: cae a "Zona N" sin inventar', () => {
  const m = describeTracts(null, [{ id: 1, code: 'a', name: 'Census Tract 9.09', geometry: null, indicators: I(1, 1, 1, 1) }]);
  assert.equal(m.get(1).title, 'Zona 9.09');
  assert.equal(m.get(1).kind, null);
});
