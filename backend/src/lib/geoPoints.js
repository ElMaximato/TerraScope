// Puntos que caen DENTRO de un polígono GeoJSON (sin depender de funciones espaciales de MySQL).
// Se usa para datos de demostración y pruebas: así siempre quedan dentro de la zona.

function ringsOf(geometry) {
  if (!geometry) return [];
  if (geometry.type === 'Polygon') return [geometry.coordinates[0]];
  if (geometry.type === 'MultiPolygon') return geometry.coordinates.map((p) => p[0]);
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

/** Hasta `count` puntos { lon, lat } dentro del polígono, ordenados del más céntrico al más lejano. */
export function pointsInside(geometry, count = 8, grid = 25) {
  const rings = ringsOf(geometry);
  if (!rings.length) return [];
  // Anillo exterior más grande (por caja envolvente).
  const bbox = (ring) => ring.reduce((b, [x, y]) => [Math.min(b[0], x), Math.min(b[1], y), Math.max(b[2], x), Math.max(b[3], y)], [Infinity, Infinity, -Infinity, -Infinity]);
  const main = rings.map((r) => ({ r, b: bbox(r) })).sort((a, b) => (b.b[2] - b.b[0]) * (b.b[3] - b.b[1]) - (a.b[2] - a.b[0]) * (a.b[3] - a.b[1]))[0];
  const [minX, minY, maxX, maxY] = main.b;
  const cx = (minX + maxX) / 2, cy = (minY + maxY) / 2;
  const pts = [];
  for (let i = 1; i < grid; i++) {
    for (let j = 1; j < grid; j++) {
      const p = [minX + ((maxX - minX) * i) / grid, minY + ((maxY - minY) * j) / grid];
      if (inRing(p, main.r)) pts.push({ lon: p[0], lat: p[1], d: (p[0] - cx) ** 2 + (p[1] - cy) ** 2 });
    }
  }
  pts.sort((a, b) => a.d - b.d);
  // Separa los puntos entre sí tomando uno de cada varios candidatos.
  const step = Math.max(1, Math.floor(pts.length / count / 2));
  const out = [];
  for (let k = 0; k < pts.length && out.length < count; k += step) out.push({ lon: pts[k].lon, lat: pts[k].lat });
  return out;
}
