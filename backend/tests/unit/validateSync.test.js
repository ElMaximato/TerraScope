import test from 'node:test';
import assert from 'node:assert/strict';
import { MAX_BATCH, validateSyncPayload } from '../../src/lib/validateSync.js';

const S1 = '11111111-1111-4111-8111-111111111111';
const O1 = '22222222-2222-4222-8222-222222222222';
const session = { id: S1, startedAt: '2026-11-06T08:00:00.000Z', device: 'reloj' };
const obs = { id: O1, sessionId: S1, tag: 'competitor', lat: 32.6, lon: -114.71, observedAt: '2026-11-06T08:15:30.123Z' };

test('lote válido normaliza fechas y ids', () => {
  const r = validateSyncPayload({ sessions: [{ ...session, id: S1.toUpperCase() }], observations: [obs] });
  assert.equal(r.sessions[0].value.id, S1);
  assert.equal(r.sessions[0].value.startedAt, '2026-11-06 08:00:00.000');
  assert.equal(r.observations[0].value.observedAt, '2026-11-06 08:15:30.123');
  assert.equal(r.observations[0].value.note, null);
});

test('un elemento inválido no bloquea a los válidos', () => {
  const r = validateSyncPayload({ observations: [{ ...obs, tag: 'inventado' }, obs] });
  assert.ok(r.observations[0].errors.tag);
  assert.equal(r.observations[0].id, O1);
  assert.ok(r.observations[1].value);
});

test('endedAt anterior a startedAt es inválido', () => {
  const r = validateSyncPayload({ sessions: [{ ...session, endedAt: '2026-11-06T07:00:00.000Z' }] });
  assert.ok(r.sessions[0].errors.endedAt);
});

test('ids que no son UUID se rechazan', () => {
  const r = validateSyncPayload({ sessions: [{ ...session, id: '123' }], observations: [{ ...obs, id: 'abc', sessionId: 'x' }] });
  assert.ok(r.sessions[0].errors.id);
  assert.ok(r.observations[0].errors.id && r.observations[0].errors.sessionId);
});

test('cuerpo inválido, vacío o demasiado grande da error global', () => {
  assert.ok(validateSyncPayload(null).error);
  assert.ok(validateSyncPayload([]).error);
  assert.ok(validateSyncPayload({}).error);
  assert.ok(validateSyncPayload({ sessions: 'x' }).error);
  assert.ok(validateSyncPayload({ observations: Array(MAX_BATCH + 1).fill(obs) }).error);
  assert.equal(validateSyncPayload({ observations: Array(MAX_BATCH).fill(obs) }).error, undefined);
});

test('elementos que no son objetos no truenan', () => {
  const r = validateSyncPayload({ sessions: [null, 5, 'x'], observations: [[]] });
  assert.ok([...r.sessions, ...r.observations].every((x) => x.errors));
});
