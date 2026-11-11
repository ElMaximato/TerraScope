// Utilidades de coordenadas y fechas. Sin dependencias.
export const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export const isLat = (n) => typeof n === 'number' && Number.isFinite(n) && n >= -90 && n <= 90;
export const isLon = (n) => typeof n === 'number' && Number.isFinite(n) && n >= -180 && n <= 180;

// WKT en orden lon lat (se usa con 'axis-order=long-lat'). toFixed evita notación científica.
export function pointWkt(lon, lat) {
  return `POINT(${lon.toFixed(7)} ${lat.toFixed(7)})`;
}

function formatCoord([lon, lat]) {
  if (!isLon(lon) || !isLat(lat)) {
    throw new Error(`Coordenadas inválidas: [${lon}, ${lat}]`);
  }
  return `${lon.toFixed(7)} ${lat.toFixed(7)}`;
}

function formatRing(ring) {
  if (!Array.isArray(ring) || ring.length < 4) {
    throw new Error('Un anillo de polígono debe tener al menos 4 puntos');
  }
  return `(${ring.map(formatCoord).join(', ')})`;
}

// Convierte un objeto GeoJSON (Polygon o MultiPolygon) a WKT en orden lon lat
export function polygonGeoJsonToWkt(geom) {
  if (!geom || typeof geom !== 'object') {
    throw new Error('Geometría inválida');
  }
  if (geom.type === 'Polygon') {
    if (!Array.isArray(geom.coordinates) || geom.coordinates.length === 0) {
      throw new Error('Polígono sin coordenadas');
    }
    const rings = geom.coordinates.map(formatRing).join(', ');
    return `POLYGON(${rings})`;
  }
  if (geom.type === 'MultiPolygon') {
    if (!Array.isArray(geom.coordinates) || geom.coordinates.length === 0) {
      throw new Error('MultiPolígono sin coordenadas');
    }
    const polys = geom.coordinates.map((poly) => `(${poly.map(formatRing).join(', ')})`).join(', ');
    return `MULTIPOLYGON(${polys})`;
  }
  throw new Error(`Tipo de geometría no soportado: ${geom.type}`);
}

// ISO 8601 -> 'YYYY-MM-DD HH:MM:SS.mmm' en UTC, o null si no es válido.
export function toMysqlDateTime(value) {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}T/.test(value)) return null;
  const d = new Date(value);
  const t = d.getTime();
  if (Number.isNaN(t)) return null;
  const year = d.getUTCFullYear();
  if (year < 2000 || year > 2100) return null;
  return d.toISOString().replace('T', ' ').replace('Z', '');
}
