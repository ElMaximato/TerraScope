import test from 'node:test';
import assert from 'node:assert/strict';
import { validateProperty, validateWatchlistAdd } from '../../src/lib/validateProperty.js';

const ok = { name: 'Lote esquina', lat: 32.6, lon: -114.71 };

test('propiedad mínima válida usa valores por defecto', () => {
  const { value } = validateProperty(ok);
  assert.equal(value.propertyType, 'other');
  assert.equal(value.reviewStatus, 'pending');
  assert.equal(value.areaValue, null);
  assert.equal(value.askingPrice, null);
});

test('propiedad completa válida', () => {
  const { value, errors } = validateProperty({ ...ok, propertyType: 'land', areaValue: 1.5, areaUnit: 'acre',
    askingPrice: 85000, rentPrice: 0, address: ' Calle 1 ', notes: 'n', sourceNote: 'visita', reviewStatus: 'verified' });
  assert.equal(errors, undefined);
  assert.equal(value.address, 'Calle 1');
  assert.equal(value.areaUnit, 'acre');
});

test('reporta cada campo inválido', () => {
  const { errors } = validateProperty({ name: '', lat: 95, lon: 'x', propertyType: 'castillo', askingPrice: -1, reviewStatus: 'listo' });
  assert.deepEqual(Object.keys(errors).sort(), ['askingPrice', 'lat', 'lon', 'name', 'propertyType', 'reviewStatus']);
});

test('superficie y unidad deben ir juntas', () => {
  assert.ok(validateProperty({ ...ok, areaValue: 10 }).errors.areaUnit);
  assert.ok(validateProperty({ ...ok, areaUnit: 'acre' }).errors.areaValue);
  assert.ok(validateProperty({ ...ok, areaValue: 0, areaUnit: 'acre' }).errors.areaValue);
});

test('rechaza precios como texto, enormes o no finitos', () => {
  assert.ok(validateProperty({ ...ok, askingPrice: '100' }).errors.askingPrice);
  assert.ok(validateProperty({ ...ok, askingPrice: 1e12 }).errors.askingPrice);
  assert.ok(validateProperty({ ...ok, rentPrice: Infinity }).errors.rentPrice);
});

test('cuerpo vacío, arreglo o nulo no truena', () => {
  for (const b of [undefined, null, [], 'texto', 5]) assert.ok(validateProperty(b).errors, String(b));
});

test('watchlist: zoneId entero positivo y nota opcional', () => {
  assert.deepEqual(validateWatchlistAdd({ zoneId: 3, note: ' hola ' }).value, { zoneId: 3, note: 'hola' });
  assert.deepEqual(validateWatchlistAdd({ zoneId: 3 }).value, { zoneId: 3, note: null });
  assert.ok(validateWatchlistAdd({ zoneId: '3' }).errors.zoneId);
  assert.ok(validateWatchlistAdd({ zoneId: 0 }).errors.zoneId);
  assert.ok(validateWatchlistAdd({ zoneId: 1, note: 'x'.repeat(501) }).errors.note);
});
