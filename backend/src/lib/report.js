// Reporte ejecutivo por zona (Etapa 9A). Es una función pura: mismos datos -> mismo reporte.
// Toda cifra del reporte sale de los indicadores del Census o del puntaje (score.js); el texto es plantilla,
// sin IA. Cada afirmación cuantitativa lleva `ref` con el código del indicador del que sale.

import { getProfile, SCORE_VERSION } from './score.js';

const CONF_LABEL = { high: 'alta', medium: 'media', low: 'baja' };
const FACTOR_CODES = ['population', 'median_income', 'households', 'population_density'];
const STRONG = 0.67; // factor normalizado >= 0.67 -> a favor
const WEAK = 0.33;   // <= 0.33 -> advertencia

// Qué verificar en campo según el perfil (texto fijo: no contiene cifras).
const CHECKLISTS = {
  real_estate: [
    'Confirmar precio por metro cuadrado de terrenos y viviendas similares en la zona.',
    'Verificar uso de suelo y zonificación con el municipio.',
    'Revisar servicios (agua, drenaje, electricidad) y acceso vial del predio.',
    'Preguntar por riesgo de inundación y estado legal/escrituras.',
  ],
  restaurant: [
    'Contar restaurantes y comida rápida a pie en un radio cercano (competencia directa).',
    'Observar tráfico peatonal y vehicular a la hora de comida y en fin de semana.',
    'Verificar estacionamiento, visibilidad del local y renta mensual de locales comparables.',
    'Confirmar permisos sanitarios y de venta de alimentos.',
  ],
  retail: [
    'Contar comercios similares cercanos y su movimiento de clientes.',
    'Verificar renta de locales comparables y tiempo que llevan vacíos.',
    'Observar el tráfico en la vía principal y el acceso desde otras zonas.',
    'Confirmar permisos comerciales y horarios permitidos.',
  ],
  services: [
    'Identificar servicios equivalentes cercanos y qué ofrecen.',
    'Verificar renta de locales y cercanía a zonas habitacionales.',
    'Preguntar a vecinos y comercios qué servicio hace falta.',
    'Confirmar licencias necesarias para el giro.',
  ],
};

function indicatorOf(zone, code) {
  return (zone.indicators || []).find((i) => i.code === code && Number.isFinite(i.value)) || null;
}

function cvOf(ind) {
  if (!ind || ind.marginOfError === null || ind.marginOfError === undefined || !(ind.value > 0)) return null;
  return ind.marginOfError / 1.645 / ind.value;
}

function round(n, d = 3) { return n === null || n === undefined ? null : Number(n.toFixed(d)); }

/** Posición (1 = la más alta) del valor entre los tracts que tienen ese indicador. */
function rankOf(tracts, zoneId, code) {
  const vals = tracts
    .map((t) => ({ id: t.id, v: indicatorOf(t, code)?.value }))
    .filter((x) => Number.isFinite(x.v));
  const mine = vals.find((x) => x.id === zoneId);
  if (!mine) return null;
  return { position: vals.filter((x) => x.v > mine.v).length + 1, of: vals.length };
}

/**
 * @param zone    { id, code, name, level, source, indicators[] }
 * @param tracts  [{ id, indicators[] }] todos los tracts (para ranking comparativo)
 * @param score   resultado de computeTractScores / endpoint de score (status, score, confidence, factors, reason)
 * @param profile id del perfil
 */
export function buildZoneReport({ zone, tracts, score, profileId, generatedAt = new Date() }) {
  const profile = getProfile(profileId);
  const indicators = (zone.indicators || []).map((i) => ({
    code: i.code,
    name: i.name,
    value: i.value,
    unit: i.unit,
    marginOfError: i.marginOfError,
    variationPct: cvOf(i) === null ? null : round(cvOf(i) * 100, 1),
    referencePeriod: i.referencePeriod,
    source: i.source ? `${i.source.organization || ''} ${i.source.name || ''}`.trim() : null,
    rank: zone.level === 'tract' && FACTOR_CODES.includes(i.code) ? rankOf(tracts, zone.id, i.code) : null,
  }));

  const strengths = [];
  const warnings = [];
  const missing = [];

  const hasScore = score && score.status === 'ok';
  if (hasScore) {
    for (const f of score.factors) {
      const rank = rankOf(tracts, zone.id, f.code);
      const where = rank ? ` (${rank.position}.º de ${rank.of} zonas)` : '';
      if (f.normalized >= STRONG) strengths.push({ ref: f.code, text: `${f.label}: alto frente a las demás zonas${where}.` });
      else if (f.normalized <= WEAK) warnings.push({ ref: f.code, text: `${f.label}: bajo frente a las demás zonas${where}.` });
    }
    if (score.confidence === 'low') {
      warnings.push({ ref: null, text: 'Confianza baja: algunos indicadores del Census tienen un margen de error grande; el puntaje es poco firme.' });
    } else if (score.confidence === 'medium') {
      warnings.push({ ref: null, text: 'Confianza media: hay margen de error apreciable en algunos indicadores.' });
    }
  } else if (score) {
    warnings.push({ ref: null, text: score.reason || 'No hay puntaje para esta zona.' });
    for (const code of score.missing || []) missing.push(code);
  }

  // Avisos que aplican siempre (limitaciones del método).
  const limitations = [
    'El puntaje es relativo: compara las zonas censales de Somerton entre sí, no contra otras ciudades.',
    'Solo usa demografía del Census ACS (población, ingreso, hogares y densidad). No incluye competencia, precios de renta o venta, ni tráfico.',
    'La densidad se calcula contra el área total de la zona censal; en zonas extensas resulta baja.',
    'Los datos del ACS son estimaciones con margen de error, no conteos exactos.',
  ];
  for (const code of FACTOR_CODES) if (!indicatorOf(zone, code) && !missing.includes(code)) missing.push(code);

  // Resumen en plantilla (solo cifras del sistema).
  let summary;
  if (hasScore) {
    const rank = tracts.length ? scoreRank(tracts, zone.id, score, profileId) : null;
    summary = `${zone.name} obtiene ${score.score}/100 para "${profile.label}"` +
      (rank ? `, ${rank.position}.º de ${rank.of} zonas comparadas` : '') +
      (score.confidence ? `, con confianza ${CONF_LABEL[score.confidence]}` : '') + '.';
  } else {
    summary = `${zone.name}: no hay puntaje disponible. ${score?.reason || ''}`.trim();
  }

  return {
    version: 'r1',
    scoreVersion: SCORE_VERSION,
    generatedAt: generatedAt.toISOString(),
    zone: { id: zone.id, code: zone.code, name: zone.name, level: zone.level },
    profile: { id: profile.id, label: profile.label, description: profile.description },
    summary,
    score: hasScore ? { value: score.score, confidence: score.confidence, factors: score.factors, rank: scoreRank(tracts, zone.id, score) } : null,
    indicators,
    strengths,
    warnings,
    missingData: missing,
    verification: CHECKLISTS[profile.id] || [],
    limitations,
    sources: sourcesOf(zone, indicators),
    methodology: 'Cada factor se normaliza entre las zonas censales (0 = la más baja, 1 = la más alta) y se multiplica por el peso del perfil; los pesos suman 100. Sin los cuatro indicadores, o con menos de 3 zonas comparables, no se calcula puntaje. La confianza sale del coeficiente de variación (margen de error / 1.645 / valor) del peor indicador: menos de 12 % alta, 12-40 % media, más de 40 % baja.',
  };
}

// Posición de la zona por puntaje. Recalcula solo si el score trae `rankScores` (lo arma el endpoint).
function scoreRank(tracts, zoneId, score) {
  const all = score.allScores;
  if (!all) return null;
  const vals = all.filter((s) => Number.isFinite(s.score));
  const mine = vals.find((s) => s.id === zoneId);
  if (!mine) return null;
  return { position: vals.filter((s) => s.score > mine.score).length + 1, of: vals.length };
}

function sourcesOf(zone, indicators) {
  const out = [];
  if (zone.source) out.push({ what: 'Límites de la zona', organization: zone.source.organization, name: zone.source.name, period: zone.source.referencePeriod, license: zone.source.license });
  const seen = new Set();
  for (const i of indicators) {
    const key = `${i.source}|${i.referencePeriod}`;
    if (!i.source || seen.has(key)) continue;
    seen.add(key);
    out.push({ what: 'Indicadores demográficos', name: i.source, period: i.referencePeriod });
  }
  return out;
}
