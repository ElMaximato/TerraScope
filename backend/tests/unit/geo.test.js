import test from 'node:test';
import assert from 'node:assert/strict';
import { isLat, isLon, pointWkt, toMysqlDateTime, polygonGeoJsonToWkt } from '../../src/lib/geo.js';
import { parseId, parsePaging } from '../../src/lib/params.js';

test('pointWkt va en orden lon lat y sin notación científica', () => {
  assert.equal(pointWkt(-114.71, 32.6), 'POINT(-114.7100000 32.6000000)');
  assert.equal(pointWkt(1e-9, 0), 'POINT(0.0000000 0.0000000)');
});

test('límites de latitud y longitud', () => {
  assert.ok(isLat(90) && isLat(-90) && !isLat(90.1) && !isLat('32') && !isLat(NaN));
  assert.ok(isLon(180) && isLon(-180) && !isLon(181) && !isLon(null));
});

test('toMysqlDateTime convierte a UTC con milisegundos', () => {
  assert.equal(toMysqlDateTime('2026-11-06T08:15:30.123Z'), '2026-11-06 08:15:30.123');
  assert.equal(toMysqlDateTime('2026-11-06T01:15:30-07:00'), '2026-11-06 08:15:30.000');
});

test('toMysqlDateTime rechaza lo que no es ISO 8601 razonable', () => {
  for (const bad of ['hoy', '2026-13-45T00:00:00Z', '2026-11-06', 12345, null, undefined, '1999-01-01T00:00:00Z', '2200-01-01T00:00:00Z']) {
    assert.equal(toMysqlDateTime(bad), null, String(bad));
  }
});

test('parseId acepta solo enteros positivos', () => {
  assert.equal(parseId('42'), 42);
  for (const bad of ['0', '-1', '1.5', 'abc', '', '99999999999', undefined, '1; DROP TABLE users']) {
    assert.equal(parseId(bad), null, String(bad));
  }
});

test('parsePaging usa valores por defecto y tope de 500', () => {
  assert.deepEqual(parsePaging({}), { limit: 100, offset: 0 });
  assert.deepEqual(parsePaging({ limit: '10', offset: '20' }), { limit: 10, offset: 20 });
  assert.deepEqual(parsePaging({ limit: '99999', offset: '-5' }), { limit: 500, offset: 0 });
  assert.deepEqual(parsePaging({ limit: 'x' }), { limit: 100, offset: 0 });
});

test('polygonGeoJsonToWkt convierte Polygon a WKT', () => {
  const polygon = {
    type: 'Polygon',
    coordinates: [
      [
        [-114.72, 32.59],
        [-114.70, 32.59],
        [-114.70, 32.61],
        [-114.72, 32.61],
        [-114.72, 32.59],
      ],
    ],
  };
  const wkt = polygonGeoJsonToWkt(polygon);
  assert.equal(
    wkt,
    'POLYGON((-114.7200000 32.5900000, -114.7000000 32.5900000, -114.7000000 32.6100000, -114.7200000 32.6100000, -114.7200000 32.5900000))'
  );
});

test('polygonGeoJsonToWkt convierte MultiPolygon a WKT', () => {
  const multiPoly = {
    type: 'MultiPolygon',
    coordinates: [
      [
        [
          [-114.72, 32.59],
          [-114.70, 32.59],
          [-114.70, 32.61],
          [-114.72, 32.61],
          [-114.72, 32.59],
        ],
      ],
    ],
  };
  const wkt = polygonGeoJsonToWkt(multiPoly);
  assert.equal(
    wkt,
    'MULTIPOLYGON(((-114.7200000 32.5900000, -114.7000000 32.5900000, -114.7000000 32.6100000, -114.7200000 32.6100000, -114.7200000 32.5900000)))'
  );
});

test('polygonGeoJsonToWkt rechaza geometrías inválidas', () => {
  assert.throws(() => polygonGeoJsonToWkt(null), /Geometría inválida/);
  assert.throws(() => polygonGeoJsonToWkt({ type: 'Point' }), /Tipo de geometría no soportado/);
  assert.throws(() => polygonGeoJsonToWkt({ type: 'Polygon', coordinates: [] }), /sin coordenadas/);
  assert.throws(
    () => polygonGeoJsonToWkt({ type: 'Polygon', coordinates: [[[-114.72, 32.59], [-114.70, 32.59]]] }),
    /al menos 4 puntos/
  );
});
