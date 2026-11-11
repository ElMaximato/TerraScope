import { UUID_RE, isLat, isLon, toMysqlDateTime } from './geo.js';

export const FIELD_TAGS = ['land_potential', 'commercial_space', 'construction', 'competitor',
  'infrastructure', 'road_access', 'observed_risk', 'needs_research'];
export const MAX_BATCH = 100;

function validateSession(s) {
  const errors = {};
  const o = s && typeof s === 'object' ? s : {};
  if (typeof o.id !== 'string' || !UUID_RE.test(o.id)) errors.id = 'id debe ser un UUID';
  const startedAt = toMysqlDateTime(o.startedAt);
  if (!startedAt) errors.startedAt = 'startedAt debe ser una fecha ISO 8601';
  let endedAt = null;
  if (o.endedAt !== undefined && o.endedAt !== null) {
    endedAt = toMysqlDateTime(o.endedAt);
    if (!endedAt) errors.endedAt = 'endedAt debe ser una fecha ISO 8601';
    else if (startedAt && endedAt < startedAt) errors.endedAt = 'endedAt no puede ser anterior a startedAt';
  }
  let device = null;
  if (o.device !== undefined && o.device !== null) {
    if (typeof o.device !== 'string' || o.device.length > 40) errors.device = 'Máximo 40 caracteres';
    else device = o.device;
  }
  return Object.keys(errors).length
    ? { id: typeof o.id === 'string' ? o.id : null, errors }
    : { value: { id: o.id.toLowerCase(), startedAt, endedAt, device } };
}

function validateObservation(s) {
  const errors = {};
  const o = s && typeof s === 'object' ? s : {};
  if (typeof o.id !== 'string' || !UUID_RE.test(o.id)) errors.id = 'id debe ser un UUID';
  let sessionId = null;
  if (o.sessionId !== undefined && o.sessionId !== null) {
    if (typeof o.sessionId !== 'string' || !UUID_RE.test(o.sessionId)) errors.sessionId = 'sessionId debe ser un UUID';
    else sessionId = o.sessionId.toLowerCase();
  }
  if (!FIELD_TAGS.includes(o.tag)) errors.tag = `tag debe ser uno de: ${FIELD_TAGS.join(', ')}`;
  let note = null;
  if (o.note !== undefined && o.note !== null) {
    if (typeof o.note !== 'string' || o.note.length > 500) errors.note = 'Máximo 500 caracteres';
    else note = o.note.trim() || null;
  }
  if (!isLat(o.lat)) errors.lat = 'Latitud no válida';
  if (!isLon(o.lon)) errors.lon = 'Longitud no válida';
  const observedAt = toMysqlDateTime(o.observedAt);
  if (!observedAt) errors.observedAt = 'observedAt debe ser una fecha ISO 8601';
  return Object.keys(errors).length
    ? { id: typeof o.id === 'string' ? o.id : null, errors }
    : { value: { id: o.id.toLowerCase(), sessionId, tag: o.tag, note, lat: o.lat, lon: o.lon, observedAt } };
}

// Devuelve { error } si el cuerpo completo es inválido, o { sessions, observations }
// donde cada elemento es { value } o { id, errors } (los inválidos no bloquean a los válidos).
export function validateSyncPayload(body) {
  const b = body && typeof body === 'object' && !Array.isArray(body) ? body : null;
  if (!b) return { error: 'El cuerpo debe ser un objeto JSON' };
  const sessions = b.sessions === undefined ? [] : b.sessions;
  const observations = b.observations === undefined ? [] : b.observations;
  if (!Array.isArray(sessions) || !Array.isArray(observations)) return { error: 'sessions y observations deben ser arreglos' };
  if (sessions.length > MAX_BATCH || observations.length > MAX_BATCH) return { error: `Máximo ${MAX_BATCH} elementos por arreglo` };
  if (sessions.length + observations.length === 0) return { error: 'No hay nada que sincronizar' };
  return { sessions: sessions.map(validateSession), observations: observations.map(validateObservation) };
}
