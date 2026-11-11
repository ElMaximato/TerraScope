import { Router } from 'express';
import { asyncHandler, badRequest, notFound } from '../lib/errors.js';
import { UUID_RE } from '../lib/geo.js';
import { parsePaging } from '../lib/params.js';
import { validateSyncPayload, FIELD_TAGS } from '../lib/validateSync.js';
import { aiRateLimit } from '../middleware/rateLimit.js';
import { pointWkt } from '../lib/geo.js';
import { buildAnalysis, tagLabel } from '../lib/analysis.js';
import { generateAiAnalysis } from '../lib/aiSummary.js';
import { getProfile, DEFAULT_PROFILE } from '../lib/score.js';
import { loadTracts } from '../lib/tractData.js';
import { findTractIdForPoint } from '../repositories/zones.js';
import { listProperties } from '../repositories/properties.js';
import { deleteObservation, listObservations, listSessions, updateObservation, upsertObservation, upsertSession } from '../repositories/field.js';

const router = Router();

const MESSAGES = {
  conflict: 'Ese id ya pertenece a otro usuario',
  invalid_session: 'La sesión indicada no existe o no es tuya',
};

// Sincronización por lotes e idempotente. Cada elemento recibe su propio estado:
// created | updated | duplicate | conflict | invalid. Los inválidos no bloquean a los válidos.
router.post('/sync', asyncHandler(async (req, res) => {
  const parsed = validateSyncPayload(req.body);
  if (parsed.error) throw badRequest(parsed.error);

  const summary = { created: 0, updated: 0, duplicate: 0, conflict: 0, invalid: 0 };
  const result = { sessions: [], observations: [] };

  const record = (list, id, status, message, details) => {
    const key = status === 'invalid_session' ? 'invalid' : status;
    summary[key] += 1;
    const entry = { id, status: key };
    if (message) entry.message = message;
    if (details) entry.details = details;
    list.push(entry);
  };

  // Las sesiones van primero: una observación puede apuntar a una sesión del mismo lote.
  for (const item of parsed.sessions) {
    if (item.errors) { record(result.sessions, item.id, 'invalid', 'Datos no válidos', item.errors); continue; }
    const status = await upsertSession(req.user.id, item.value);
    record(result.sessions, item.value.id, status, MESSAGES[status]);
  }
  for (const item of parsed.observations) {
    if (item.errors) { record(result.observations, item.id, 'invalid', 'Datos no válidos', item.errors); continue; }
    const status = await upsertObservation(req.user.id, item.value);
    record(result.observations, item.value.id, status, MESSAGES[status]);
  }
  res.json({ summary, results: result });
}));

router.get('/sessions', asyncHandler(async (req, res) => {
  res.json({ sessions: await listSessions(req.user.id, parsePaging(req.query)) });
}));

router.get('/observations', asyncHandler(async (req, res) => {
  const sessionId = req.query.sessionId;
  if (sessionId !== undefined && !UUID_RE.test(String(sessionId))) throw badRequest('sessionId debe ser un UUID');
  res.json({
    observations: await listObservations(req.user.id, {
      ...parsePaging(req.query), sessionId: sessionId ? String(sessionId).toLowerCase() : undefined,
    }),
  });
}));

// Análisis de todas las ubicaciones del usuario (puntos del reloj + propiedades), con IA opcional.
router.get('/analysis', aiRateLimit, asyncHandler(async (req, res) => {
  const profile = getProfile(req.query.profile ? String(req.query.profile) : DEFAULT_PROFILE);
  if (!profile) throw badRequest('Perfil no válido');
  const [obs, props, tracts] = await Promise.all([
    listObservations(req.user.id, { limit: 50, offset: 0 }),
    listProperties(req.user.id, { limit: 50, offset: 0 }),
    loadTracts(),
  ]);
  const locations = [];
  for (const o of obs) {
    locations.push({ kind: 'point', id: o.id, name: o.note || tagLabel(o.tag), tag: o.tag, lat: o.lat, lon: o.lon,
      tractId: await findTractIdForPoint(pointWkt(o.lon, o.lat)) });
  }
  for (const p of props) {
    locations.push({ kind: 'property', id: String(p.id), name: p.name, lat: p.lat, lon: p.lon,
      tractId: await findTractIdForPoint(pointWkt(p.lon, p.lat)) });
  }
  const analysis = buildAnalysis({ locations, tracts, profileId: profile.id });
  analysis.ai = req.query.ai === '1' ? await generateAiAnalysis(analysis) : { status: 'disabled', text: null };
  res.json({ analysis });
}));

router.put('/observations/:id', asyncHandler(async (req, res) => {
  const id = String(req.params.id).toLowerCase();
  if (!UUID_RE.test(id)) throw badRequest('id debe ser un UUID');
  const b = req.body && typeof req.body === 'object' ? req.body : {};
  const patch = {};
  if (b.tag !== undefined) {
    if (!FIELD_TAGS.includes(b.tag)) throw badRequest(`tag debe ser uno de: ${FIELD_TAGS.join(', ')}`);
    patch.tag = b.tag;
  }
  if (b.note !== undefined) {
    if (b.note !== null && (typeof b.note !== 'string' || b.note.length > 80)) throw badRequest('note: máximo 80 caracteres');
    patch.note = b.note === null ? null : (b.note.trim() || null);
  }
  if (!(await updateObservation(req.user.id, id, patch))) throw notFound('Punto no encontrado');
  res.json({ ok: true });
}));

router.delete('/observations/:id', asyncHandler(async (req, res) => {
  const id = String(req.params.id).toLowerCase();
  if (!UUID_RE.test(id)) throw badRequest('id debe ser un UUID');
  if (!(await deleteObservation(req.user.id, id))) throw notFound('Punto no encontrado');
  res.json({ ok: true });
}));

export default router;
