// Análisis comparativo de las ubicaciones del usuario (puntos del reloj y propiedades registradas).
// Función pura: cada ubicación se asocia a una zona censal y se ordena por el puntaje del perfil elegido.

import { computeTractScores, getProfile } from './score.js';

const TAG_LABELS = {
  land_potential: 'Terreno potencial', commercial_space: 'Local comercial', construction: 'Construcción',
  competitor: 'Competencia', infrastructure: 'Infraestructura', road_access: 'Acceso vial',
  observed_risk: 'Riesgo observado', needs_research: 'Investigar',
};
export const tagLabel = (t) => TAG_LABELS[t] || t;

/**
 * @param locations [{ kind: 'point'|'property', id, name, tag?, lat, lon, tractId|null }]
 * @param tracts    tracts con indicadores
 */
export function buildAnalysis({ locations, tracts, profileId, generatedAt = new Date() }) {
  const profile = getProfile(profileId);
  const scores = computeTractScores(tracts, profile.id);
  const names = new Map(tracts.map((t) => [t.id, t.name]));

  const items = locations.map((l) => {
    const r = l.tractId ? scores.get(l.tractId) : null;
    const ok = r && r.status === 'ok';
    return {
      kind: l.kind, id: l.id, name: l.name, tag: l.tag || null, lat: l.lat, lon: l.lon,
      zoneId: l.tractId || null, zoneName: l.tractId ? names.get(l.tractId) : null,
      score: ok ? r.score : null, confidence: ok ? r.confidence : null,
      status: !l.tractId ? 'outside' : (ok ? 'ok' : 'insufficient'),
    };
  }).sort((a, b) => (b.score ?? -1) - (a.score ?? -1));

  const scored = items.filter((i) => i.score !== null);
  const outside = items.filter((i) => i.status === 'outside').length;

  let summary;
  if (!items.length) summary = 'Aún no tienes ubicaciones. Marca puntos con el reloj o registra propiedades.';
  else if (!scored.length) summary = 'Ninguna de tus ubicaciones cae en una zona con puntaje.';
  else {
    const best = scored[0];
    summary = `De ${items.length} ubicaciones, la mejor para "${profile.label}" es "${best.name}" (${best.score}/100).`;
    if (scored.length > 1) {
      const worst = scored[scored.length - 1];
      summary += ` La más baja es "${worst.name}" (${worst.score}/100).`;
    }
  }

  const notes = [];
  if (outside) notes.push(`${outside} ubicación(es) quedan fuera de las zonas de Somerton y no tienen puntaje.`);
  notes.push('El puntaje es de la zona censal donde cae cada ubicación, no del predio en sí: dos ubicaciones en la misma zona tienen el mismo puntaje.');
  notes.push('Es demográfico y relativo; no considera competencia, precios de renta o venta ni tráfico. Verifica en campo antes de decidir.');

  return {
    generatedAt: generatedAt.toISOString(),
    profile: { id: profile.id, label: profile.label },
    summary, items, notes,
  };
}

/** Cifras que el texto de la IA puede mencionar. */
export function analysisAllowedNumbers(a) {
  const set = new Set([100, a.items.length]);
  for (let i = 1; i <= a.items.length; i++) set.add(i);
  for (const it of a.items) if (it.score !== null) set.add(it.score);
  return set;
}

export function analysisPayload(a) {
  return {
    perfil: a.profile.label,
    ubicaciones: a.items.map((i) => ({
      nombre: i.name, tipo: i.kind === 'point' ? 'punto marcado' : 'propiedad', etiqueta: i.tag ? tagLabel(i.tag) : null,
      zona: i.zoneName, puntaje: i.score, confianza: i.confidence, estado: i.status,
    })),
    limitaciones: a.notes,
  };
}
