// Nombres descriptivos para las zonas censales: "Residencial · norte", "Afueras · este", "Centro".
// Se calculan a partir de la geometría y la densidad (sin datos inventados). Ver docs/ARQUITECTURA_Y_LIMITACIONES.md.
// El número del Census (p. ej. 115.03) se conserva como dato secundario.

export const DENSE_PER_KM2 = 1000; // por encima: "Residencial"; por debajo (dentro de la ciudad): "Urbana de baja densidad"
const CENTER_RADIUS_KM = 0.5;      // a menos de esto del centro no se indica dirección

function polygonsOf(g) {
  if (!g) return [];
  if (g.type === 'Polygon') return [g.coordinates];
  if (g.type === 'MultiPolygon') return g.coordinates;
  return [];
}

function inRing([x, y], ring) {
  let inside = false;
  for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
    const [xi, yi] = ring[i];
    const [xj, yj] = ring[j];
    if ((yi > y) !== (yj > y) && x < ((xj - xi) * (y - yi)) / (yj - yi) + xi) inside = !inside;
  }
  return inside;
}

/** ¿El punto [lon, lat] está dentro del Polygon/MultiPolygon (respetando huecos)? */
export function pointInGeometry(pt, geometry) {
  return polygonsOf(geometry).some((poly) => inRing(pt, poly[0]) && !poly.slice(1).some((h) => inRing(pt, h)));
}

/** Centroide ponderado por área del polígono(s) [lon, lat]; plano local (suficiente para una ciudad). */
export function centroidOf(geometry) {
  let A = 0, cx = 0, cy = 0;
  for (const poly of polygonsOf(geometry)) {
    const r = poly[0];
    for (let i = 0, j = r.length - 1; i < r.length; j = i++) {
      const f = r[j][0] * r[i][1] - r[i][0] * r[j][1];
      A += f; cx += (r[j][0] + r[i][0]) * f; cy += (r[j][1] + r[i][1]) * f;
    }
  }
  if (!A) return null;
  return [cx / (3 * A), cy / (3 * A)];
}

export function offsetKm(from, to) {
  const lat = ((from[1] + to[1]) / 2) * (Math.PI / 180);
  return { east: (to[0] - from[0]) * 111.32 * Math.cos(lat), north: (to[1] - from[1]) * 110.57 };
}

const DIRS = ['este', 'noreste', 'norte', 'noroeste', 'oeste', 'suroeste', 'sur', 'sureste'];
export function directionOf({ east, north }) {
  const deg = (Math.atan2(north, east) * 180) / Math.PI;
  return DIRS[Math.round((((deg % 360) + 360) % 360) / 45) % 8];
}

const ind = (z, code) => z.indicators.find((i) => i.code === code)?.value ?? null;
const fmt = (n) => Math.round(n).toLocaleString('en-US');

function compare(value, avg) {
  if (value == null || !avg) return null;
  const pct = Math.round(((value - avg) / avg) * 100);
  if (Math.abs(pct) < 10) return 'Similar al promedio de las zonas de Somerton';
  return pct > 0 ? `${pct}% más que el promedio de las zonas de Somerton` : `${-pct}% menos que el promedio de las zonas de Somerton`;
}

/**
 * place: { geometry } de la ciudad. tracts: [{ id, code, name, geometry, indicators }].
 * Devuelve Map(id -> friendly).
 */
export function describeTracts(place, tracts) {
  const center = place?.geometry ? centroidOf(place.geometry) : null;
  const avg = (code) => {
    const v = tracts.map((t) => ind(t, code)).filter((x) => x != null);
    return v.length ? v.reduce((a, b) => a + b, 0) / v.length : null;
  };
  const avgs = { population: avg('population'), median_income: avg('median_income'), households: avg('households'), population_density: avg('population_density') };

  const out = new Map();
  for (const t of tracts) {
    const c = t.geometry ? centroidOf(t.geometry) : null;
    const density = ind(t, 'population_density');
    let kind = null, direction = null, distanceKm = null, insideCity = null, containsCenter = false;
    if (center && c) {
      const off = offsetKm(center, c);
      distanceKm = Math.round(Math.hypot(off.east, off.north) * 10) / 10;
      direction = distanceKm >= CENTER_RADIUS_KM ? directionOf(off) : null;
      insideCity = pointInGeometry(c, place.geometry);
      containsCenter = pointInGeometry(center, t.geometry);
      if (containsCenter) kind = 'Centro';
      else if (insideCity) kind = density != null && density >= DENSE_PER_KM2 ? 'Residencial' : 'Urbana, baja densidad';
      else kind = 'Afueras y campo';
    }
    const shortCode = String(t.name).replace(/^Census Tract\s+/i, '');
    const base = kind ? (kind === 'Centro' ? 'Centro' : `${kind}${direction ? ` · ${direction}` : ''}`) : `Zona ${shortCode}`;
    const pop = ind(t, 'population'), hh = ind(t, 'households'), inc = ind(t, 'median_income');

    const highlights = [];
    if (pop != null) highlights.push({ code: 'population', title: 'Personas', value: fmt(pop), help: 'Habitantes de la zona. Más gente significa más clientes potenciales.', comparison: compare(pop, avgs.population) });
    if (hh != null) highlights.push({ code: 'households', title: 'Hogares', value: fmt(hh), help: 'Familias o grupos que viven juntos. Cada hogar es un cliente potencial.', comparison: compare(hh, avgs.households) });
    if (inc != null) highlights.push({ code: 'median_income', title: 'Ingreso típico por hogar', value: `$${fmt(inc)} al año`, help: 'La mitad de los hogares gana más y la otra mitad menos. Indica poder de compra.', comparison: compare(inc, avgs.median_income) });
    if (density != null) highlights.push({ code: 'population_density', title: 'Gente por km²', value: fmt(density), help: 'Qué tan junta vive la gente. Mayor densidad significa clientes más concentrados.', comparison: compare(density, avgs.population_density) });

    const where = containsCenter ? 'Es la zona del centro de Somerton.'
      : distanceKm == null ? ''
      : `Está ${direction ? `al ${direction} del centro` : 'junto al centro'} de Somerton${distanceKm >= CENTER_RADIUS_KM ? `, a unos ${distanceKm} km` : ''}${insideCity ? ', dentro de los límites de la ciudad' : ', fuera de los límites de la ciudad'}.`;
    const who = pop != null && hh != null ? ` Viven unas ${fmt(pop)} personas en ${fmt(hh)} hogares.` : '';

    out.set(t.id, { kind, direction, distanceKm, insideCity, title: base, codeLabel: `Zona censal ${shortCode}`, description: `${where}${who}`.trim(), highlights });
  }
  // Si dos zonas quedan con el mismo nombre, se distinguen con su número del Census.
  const counts = new Map();
  for (const f of out.values()) counts.set(f.title, (counts.get(f.title) || 0) + 1);
  for (const t of tracts) {
    const f = out.get(t.id);
    if (counts.get(f.title) > 1) f.title = `${f.title} (${String(t.name).replace(/^Census Tract\s+/i, '')})`;
  }
  return out;
}

let cache = { at: 0, map: null };
const TTL_MS = 10 * 60 * 1000;

/** Mapa id -> friendly para todas las zonas censales (caché 10 min; los datos cambian solo al reimportar). */
export async function loadZoneLabels() {
  if (cache.map && Date.now() - cache.at < TTL_MS) return cache.map;
  const { pool } = await import('../db.js'); // import diferido: las funciones puras se prueban sin MySQL
  const [zones] = await pool.query("SELECT id, code, name, level, ST_AsGeoJSON(boundary) AS g FROM zones WHERE level IN ('tract','place')");
  const parse = (g) => (typeof g === 'string' ? JSON.parse(g) : g || null);
  const [vals] = await pool.query(
    `SELECT iv.zone_id, i.code, iv.value FROM indicator_values iv JOIN indicators i ON iv.indicator_id = i.id
     WHERE i.code IN ('population','median_income','households','population_density')`);
  const byZone = new Map();
  for (const v of vals) { if (!byZone.has(v.zone_id)) byZone.set(v.zone_id, []); byZone.get(v.zone_id).push({ code: v.code, value: Number(v.value) }); }
  const placeRow = zones.find((z) => z.level === 'place');
  const tracts = zones.filter((z) => z.level === 'tract').map((z) => ({ id: z.id, code: z.code, name: z.name, geometry: parse(z.g), indicators: byZone.get(z.id) || [] }));
  const map = describeTracts(placeRow ? { geometry: parse(placeRow.g) } : null, tracts);
  cache = { at: Date.now(), map };
  return map;
}

export function clearZoneLabelsCache() { cache = { at: 0, map: null }; }
