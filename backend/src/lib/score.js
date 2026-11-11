// Opportunity Finder v2: puntaje 0-100 explicable según el perfil de inversión elegido.
//
// Método (todo es transparente y reproducible):
//  1. Se comparan solo los tracts entre sí (el place Somerton city no se puntúa: no es comparable).
//  2. Cada factor se normaliza con min-max entre los tracts: 0 = el más bajo, 1 = el más alto.
//  3. Puntos del factor = normalizado × peso. Puntaje = suma de puntos (los pesos suman 100).
//  4. Si a un tract le falta cualquier factor, o hay menos de 3 tracts comparables, NO se da puntaje
//     ("datos insuficientes") en vez de inventar uno.
//  5. Confianza = peor coeficiente de variación (CV = MOE/1.645/valor) entre los factores con margen
//     de error del ACS: < 12 % alta, 12-40 % media, > 40 % baja.
// Cada perfil cambia solo los pesos (ver PROFILES). La edad mediana no entra en la fórmula: no hay una dirección clara de "mejor" para este perfil.

export const SCORE_VERSION = 'v2';
export const MIN_TRACTS = 3;

const LABELS = {
  population: 'Población (demanda potencial)',
  median_income: 'Ingreso mediano (poder de compra)',
  households: 'Hogares (clientes potenciales)',
  population_density: 'Densidad (concentración de clientes)',
};

// Perfiles de inversión: mismos 4 indicadores del Census, con pesos distintos que siempre suman 100.
// `keywords` sirve para sugerir un perfil a partir de una descripción libre (regla simple, sin IA).
export const PROFILES = {
  real_estate: {
    id: 'real_estate',
    label: 'Bienes raíces (vivienda)',
    description: 'Pesa más el ingreso y los hogares: poder adquisitivo y demanda de vivienda.',
    weights: { median_income: 40, households: 30, population: 20, population_density: 10 },
    keywords: ['casa', 'casas', 'vivienda', 'departamento', 'terreno', 'lote', 'inmueble', 'bienes raices', 'real estate', 'renta', 'rentar', 'construir', 'airbnb'],
  },
  restaurant: {
    id: 'restaurant',
    label: 'Restaurante y comida',
    description: 'Pesa más la población y la densidad: mucha gente cerca y tráfico de clientes.',
    weights: { population: 35, population_density: 30, median_income: 20, households: 15 },
    keywords: ['restaurante', 'taqueria', 'comida', 'cafe', 'cafeteria', 'panaderia', 'fonda', 'cocina', 'food', 'bar', 'pizzeria', 'loncheria', 'paleteria', 'heladeria'],
  },
  retail: {
    id: 'retail',
    label: 'Comercio minorista',
    description: 'Equilibra demanda (población, hogares) y poder de compra (ingreso).',
    weights: { population: 30, median_income: 30, households: 20, population_density: 20 },
    keywords: ['tienda', 'comercio', 'ropa', 'abarrotes', 'minisuper', 'retail', 'boutique', 'papeleria', 'farmacia', 'negocio', 'vender', 'venta', 'mercado', 'supermercado'],
  },
  services: {
    id: 'services',
    label: 'Servicios locales',
    description: 'Pesa más los hogares: servicios que la gente usa cerca de su casa.',
    weights: { households: 40, population: 25, median_income: 20, population_density: 15 },
    keywords: ['servicio', 'servicios', 'salon', 'estetica', 'barberia', 'lavanderia', 'clinica', 'consultorio', 'gimnasio', 'gym', 'taller', 'escuela', 'guarderia', 'dentista', 'veterinaria'],
  },
};
export const DEFAULT_PROFILE = 'retail';

export function getProfile(id) {
  return PROFILES[id] || null;
}

function factorsOf(profile) {
  return Object.entries(profile.weights).map(([code, weight]) => ({ code, label: LABELS[code], weight }));
}

// Compatibilidad: factores del perfil por defecto.
export const FACTORS = factorsOf(PROFILES[DEFAULT_PROFILE]);

function normalizeText(t) {
  return String(t || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
}

/** Sugiere un perfil a partir de texto libre. Devuelve { profile, matched } o null si no reconoce nada. */
export function matchProfile(text) {
  const norm = ` ${normalizeText(text).replace(/[^a-z0-9ñ ]/g, ' ')} `;
  let best = null;
  for (const p of Object.values(PROFILES)) {
    const matched = p.keywords.filter((k) => norm.includes(` ${k} `) || norm.includes(` ${k}s `));
    // Las palabras genéricas ("negocio", "venta") valen menos que las específicas.
    const generic = ['negocio', 'venta', 'vender', 'mercado', 'comercio'];
    const strength = matched.reduce((a, k) => a + (generic.includes(k) ? 0.5 : 1), 0);
    if (matched.length && (!best || strength > best.strength)) best = { profile: p.id, matched, strength };
  }
  return best ? { profile: best.profile, matched: best.matched } : null;
}

function valueOf(tract, code) {
  const ind = (tract.indicators || []).find((i) => i.code === code);
  return ind && Number.isFinite(ind.value) ? ind : null;
}

export function confidenceOf(indicators) {
  let worst = 0;
  let any = false;
  for (const ind of indicators) {
    if (!ind || ind.marginOfError === null || ind.marginOfError === undefined || !(ind.value > 0)) continue;
    any = true;
    worst = Math.max(worst, ind.marginOfError / 1.645 / ind.value);
  }
  if (!any) return null;
  if (worst < 0.12) return 'high';
  if (worst <= 0.4) return 'medium';
  return 'low';
}

/** tracts: [{ id, indicators: [{ code, value, marginOfError }] }]  ->  Map(id -> resultado) */
export function computeTractScores(tracts, profileId = DEFAULT_PROFILE) {
  const profile = getProfile(profileId);
  if (!profile) throw new Error(`Perfil desconocido: ${profileId}`);
  const FACTORS = factorsOf(profile);
  const results = new Map();
  const complete = tracts.filter((t) => FACTORS.every((f) => valueOf(t, f.code)));

  for (const t of tracts) {
    if (complete.includes(t) && complete.length >= MIN_TRACTS) continue;
    const missing = FACTORS.filter((f) => !valueOf(t, f.code)).map((f) => f.code);
    results.set(t.id, {
      status: 'insufficient',
      score: null,
      confidence: null,
      factors: [],
      missing,
      reason: missing.length
        ? 'Faltan indicadores para calcular el puntaje.'
        : `Se necesitan al menos ${MIN_TRACTS} zonas con datos completos para comparar.`,
    });
  }
  if (complete.length < MIN_TRACTS) return results;

  const range = {};
  for (const f of FACTORS) {
    const vals = complete.map((t) => valueOf(t, f.code).value);
    range[f.code] = { min: Math.min(...vals), max: Math.max(...vals) };
  }

  for (const t of complete) {
    let total = 0;
    const factors = FACTORS.map((f) => {
      const { value } = valueOf(t, f.code);
      const { min, max } = range[f.code];
      const normalized = max === min ? 0.5 : (value - min) / (max - min);
      const points = normalized * f.weight;
      total += points;
      return {
        code: f.code,
        label: f.label,
        weight: f.weight,
        value,
        normalized: Number(normalized.toFixed(3)),
        points: Number(points.toFixed(1)),
      };
    });
    results.set(t.id, {
      status: 'ok',
      profile: profile.id,
      score: Math.round(total),
      confidence: confidenceOf(FACTORS.map((f) => valueOf(t, f.code))),
      factors,
      missing: [],
      reason: null,
    });
  }
  return results;
}
