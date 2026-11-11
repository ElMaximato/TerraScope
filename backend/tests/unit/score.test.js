import test from 'node:test';
import assert from 'node:assert/strict';
import { computeTractScores, confidenceOf, FACTORS, PROFILES, matchProfile } from '../../src/lib/score.js';

const mk = (id, population, income, households, density, moe = {}) => ({
  id,
  indicators: [
    { code: 'population', value: population, marginOfError: moe.population ?? null },
    { code: 'median_income', value: income, marginOfError: moe.income ?? null },
    { code: 'households', value: households, marginOfError: null },
    { code: 'population_density', value: density, marginOfError: null },
  ],
});

test('los pesos suman 100', () => {
  assert.equal(FACTORS.reduce((a, f) => a + f.weight, 0), 100);
});

test('la zona más alta en todo saca 100 y la más baja 0', () => {
  const r = computeTractScores([mk(1, 100, 10, 10, 10), mk(2, 200, 20, 20, 20), mk(3, 300, 30, 30, 30)]);
  assert.equal(r.get(3).score, 100);
  assert.equal(r.get(1).score, 0);
  assert.equal(r.get(2).score, 50);
  assert.equal(r.get(2).factors.reduce((a, f) => a + f.points, 0), 50);
});

test('menos de 3 zonas comparables: datos insuficientes, sin puntaje', () => {
  const r = computeTractScores([mk(1, 1, 1, 1, 1), mk(2, 2, 2, 2, 2)]);
  for (const v of r.values()) assert.deepEqual([v.status, v.score], ['insufficient', null]);
});

test('a una zona con un indicador faltante no se le inventa puntaje', () => {
  const incompleta = { id: 9, indicators: [{ code: 'population', value: 5, marginOfError: null }] };
  const r = computeTractScores([mk(1, 1, 1, 1, 1), mk(2, 2, 2, 2, 2), mk(3, 3, 3, 3, 3), incompleta]);
  assert.equal(r.get(9).status, 'insufficient');
  assert.deepEqual(r.get(9).missing, ['median_income', 'households', 'population_density']);
  assert.equal(r.get(3).status, 'ok');
});

test('factor sin variación entre zonas aporta la mitad de sus puntos', () => {
  const r = computeTractScores([mk(1, 5, 1, 1, 1), mk(2, 5, 2, 2, 2), mk(3, 5, 3, 3, 3)]);
  assert.equal(r.get(1).factors.find((f) => f.code === 'population').points, 15);
});

test('confianza según el peor coeficiente de variación', () => {
  assert.equal(confidenceOf([{ value: 1000, marginOfError: 50 }]), 'high');
  assert.equal(confidenceOf([{ value: 1000, marginOfError: 50 }, { value: 1000, marginOfError: 400 }]), 'medium');
  assert.equal(confidenceOf([{ value: 1000, marginOfError: 1500 }]), 'low');
  assert.equal(confidenceOf([{ value: 1000, marginOfError: null }]), null);
});

test('todos los perfiles suman 100 y usan los 4 indicadores', () => {
  for (const p of Object.values(PROFILES)) {
    assert.equal(Object.values(p.weights).reduce((a, b) => a + b, 0), 100, p.id);
    assert.deepEqual(Object.keys(p.weights).sort(), ['households', 'median_income', 'population', 'population_density']);
  }
});

test('el perfil cambia el puntaje y el ranking', () => {
  // Zona 1: mucho ingreso, poca gente. Zona 3: mucha gente y densidad, poco ingreso.
  const data = [mk(1, 100, 300, 100, 10), mk(2, 200, 200, 200, 20), mk(3, 300, 100, 300, 30)];
  const vivienda = computeTractScores(data, 'real_estate');
  const comida = computeTractScores(data, 'restaurant');
  assert.ok(vivienda.get(1).score > comida.get(1).score);
  assert.ok(comida.get(3).score > vivienda.get(3).score);
  assert.equal(comida.get(3).profile, 'restaurant');
});

test('perfil desconocido lanza error', () => {
  assert.throws(() => computeTractScores([mk(1, 1, 1, 1, 1)], 'nada'));
});

test('matchProfile reconoce descripciones libres', () => {
  assert.equal(matchProfile('Quiero abrir una taquería').profile, 'restaurant');
  assert.equal(matchProfile('comprar un terreno para construir casas').profile, 'real_estate');
  assert.equal(matchProfile('un salón de belleza o una barbería').profile, 'services');
  assert.equal(matchProfile('una tienda de ropa').profile, 'retail');
  assert.equal(matchProfile('Cafetería').profile, 'restaurant');
});

test('matchProfile devuelve null si no entiende', () => {
  assert.equal(matchProfile('xyz'), null);
  assert.equal(matchProfile(''), null);
});
