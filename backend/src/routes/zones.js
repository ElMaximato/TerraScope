import { Router } from 'express';
import { asyncHandler, AppError } from '../lib/errors.js';
import { parseId } from '../lib/params.js';
import { listZones, getZoneById, listIndicators } from '../repositories/zones.js';
import { buildZoneReport } from '../lib/report.js';
import { aiRateLimit } from '../middleware/rateLimit.js';
import { loadTracts } from '../lib/tractData.js';
import { generateAiSummary } from '../lib/aiSummary.js';
import { loadZoneLabels } from '../lib/zoneLabels.js';
import { computeTractScores, matchProfile, getProfile, PROFILES, DEFAULT_PROFILE, SCORE_VERSION } from '../lib/score.js';

const router = Router();

// Lista las zonas (resumen)
router.get('/', asyncHandler(async (_req, res) => {
  const [zones, labels] = await Promise.all([listZones(), loadZoneLabels().catch(() => new Map())]);
  res.json({ zones: zones.map((z) => ({ ...z, friendly: labels.get(z.id) ? { title: labels.get(z.id).title, kind: labels.get(z.id).kind, direction: labels.get(z.id).direction } : null })) });
}));

// Lista el catálogo de indicadores disponibles
router.get('/indicators', asyncHandler(async (_req, res) => {
  res.json({ indicators: await listIndicators() });
}));

// ── Opportunity Finder (ver src/lib/score.js) ──────────────────────────────

function profileFromQuery(req) {
  const id = req.query.profile ? String(req.query.profile) : DEFAULT_PROFILE;
  const profile = getProfile(id);
  if (!profile) throw new AppError(400, 'invalid_profile', `Perfil no válido. Usa: ${Object.keys(PROFILES).join(', ')}`);
  return profile;
}

const SCORE_NOTE = 'Estimación basada solo en demografía del Census (población, ingreso, hogares y densidad). No considera competencia, precios ni tráfico.';

// Perfiles de inversión disponibles
router.get('/profiles', (_req, res) => {
  res.json({
    profiles: Object.values(PROFILES).map((p) => ({ id: p.id, label: p.label, description: p.description, weights: p.weights })),
  });
});

// Sugiere un perfil a partir de una descripción libre (regla de palabras clave)
router.get('/profiles/match', (req, res) => {
  const q = String(req.query.q || '').slice(0, 100);
  const m = matchProfile(q);
  res.json({ match: m ? { profile: m.profile, label: PROFILES[m.profile].label, matched: m.matched } : null });
});

// Ranking de zonas para un perfil
router.get('/ranking', asyncHandler(async (req, res) => {
  const profile = profileFromQuery(req);
  const tracts = await loadTracts();
  const labels = await loadZoneLabels().catch(() => new Map());
  const scores = computeTractScores(tracts, profile.id);
  const items = tracts.map((t) => {
    const r = scores.get(t.id);
    return { zoneId: t.id, code: t.code, name: t.name, friendlyTitle: labels.get(t.id)?.title ?? null, status: r.status, score: r.score, confidence: r.confidence };
  }).sort((a, b) => (b.score ?? -1) - (a.score ?? -1));
  res.json({ ranking: { profile: profile.id, label: profile.label, description: profile.description, version: SCORE_VERSION, note: SCORE_NOTE, items } });
}));

// Puntaje de una zona con desglose
router.get('/:id/score', asyncHandler(async (req, res) => {
  const id = parseId(req.params.id);
  if (!id) throw new AppError(404, 'zone_not_found', 'Zona no encontrada');
  const profile = profileFromQuery(req);
  const zone = await getZoneById(id, { includeBoundary: false });
  if (!zone) throw new AppError(404, 'zone_not_found', 'Zona no encontrada');
  const base = { zoneId: id, version: SCORE_VERSION, profile: profile.id, profileLabel: profile.label, note: SCORE_NOTE };
  if (zone.level !== 'tract') {
    return res.json({ score: { ...base, status: 'not_applicable', score: null, confidence: null, factors: [], missing: [], comparedWith: 0,
      reason: 'El puntaje se calcula solo para zonas censales (tracts), no para la ciudad completa.' } });
  }
  const tracts = await loadTracts();
  const result = computeTractScores(tracts, profile.id).get(id);
  res.json({ score: { ...base, ...result, comparedWith: tracts.length } });
}));

// Reporte ejecutivo de una zona (plantilla determinista, sin IA)
router.get('/:id/report', aiRateLimit, asyncHandler(async (req, res) => {
  const id = parseId(req.params.id);
  if (!id) throw new AppError(404, 'zone_not_found', 'Zona no encontrada');
  const profile = profileFromQuery(req);
  const zone = await getZoneById(id, { includeBoundary: false });
  if (!zone) throw new AppError(404, 'zone_not_found', 'Zona no encontrada');
  const tracts = await loadTracts();
  let score;
  if (zone.level !== 'tract') {
    score = { status: 'not_applicable', score: null, confidence: null, factors: [], missing: [],
      reason: 'El puntaje se calcula solo para zonas censales (tracts), no para la ciudad completa.' };
  } else {
    const all = computeTractScores(tracts, profile.id);
    score = { ...all.get(id), allScores: tracts.map((t) => ({ id: t.id, score: all.get(t.id).score })) };
  }
  const report = buildZoneReport({ zone, tracts, score, profileId: profile.id });
  // ?ai=1 -> intenta añadir un resumen redactado por IA (opcional; sin clave o con error, status != 'ok' y text null).
  if (req.query.ai === '1') report.ai = await generateAiSummary(report);
  res.json({ report });
}));

// Detalle de una zona con polígono GeoJSON e indicadores reales trazables
router.get('/:id', asyncHandler(async (req, res) => {
  const id = parseId(req.params.id);
  if (!id) throw new AppError(404, 'zone_not_found', 'Zona no encontrada');
  const zone = await getZoneById(id);
  if (!zone) throw new AppError(404, 'zone_not_found', 'Zona no encontrada');
  const labels = await loadZoneLabels().catch(() => new Map());
  const friendly = labels.get(id) || (zone.level === 'place'
    ? { kind: 'Ciudad', direction: null, distanceKm: null, insideCity: null, title: zone.name, codeLabel: 'Ciudad completa',
        description: 'Resumen de toda la ciudad de Somerton. Sirve de referencia para comparar sus zonas.', highlights: [] }
    : null);
  res.json({ zone: { ...zone, friendly } });
}));

export default router;
