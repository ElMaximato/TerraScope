// Datos de demostración, claramente marcados con "[DEMO]".
// Crea (o reinicia) el usuario demo con propiedades, zonas guardadas y puntos de campo dentro de Somerton.
//   npm run db:seed-demo
// Requiere la base ya migrada e importada (zonas de Somerton). No toca los datos de otros usuarios.
import 'dotenv/config';
import { randomUUID } from 'node:crypto';
import { pool } from '../src/db.js';
import { hashPassword } from '../src/lib/password.js';
import { pointsInside } from '../src/lib/geoPoints.js';
import { pointWkt } from '../src/lib/geo.js';
import { createProperty } from '../src/repositories/properties.js';
import { upsertObservation } from '../src/repositories/field.js';
import { addToWatchlist } from '../src/repositories/watchlist.js';
import { findByEmail, createUser } from '../src/repositories/users.js';
import { getZoneById, listZones, findZoneIdForPoint } from '../src/repositories/zones.js';

const EMAIL = process.env.DEMO_EMAIL || 'demo@terrascope.local';
const PASSWORD = process.env.DEMO_PASSWORD || 'Demo-TerraScope-2026';

try {
  const tracts = (await listZones()).filter((z) => z.level === 'tract');
  if (tracts.length < 2) throw new Error('No hay zonas censales en la base. Ejecuta primero la importación de Somerton (npm run db:import-somerton).');

  // Puntos dentro de cada zona (el primero es el más céntrico).
  const spots = [];
  for (const t of tracts) {
    const z = await getZoneById(t.id, { includeBoundary: true });
    spots.push({ zone: t, pts: pointsInside(z.boundary, 6) });
  }
  const usable = spots.filter((s) => s.pts.length >= 3);
  if (usable.length < 2) throw new Error('No se pudieron calcular puntos dentro de las zonas.');
  const at = (zi, pi) => { const s = usable[zi % usable.length]; return s.pts[pi % s.pts.length]; };

  let user = await findByEmail(EMAIL);
  if (!user) {
    const id = await createUser({ email: EMAIL, passwordHash: await hashPassword(PASSWORD), displayName: 'Usuario Demo' });
    user = { id };
    console.log(`Usuario demo creado: ${EMAIL}`);
  } else {
    for (const table of ['watchlist_items', 'properties', 'field_observations', 'field_sessions']) {
      await pool.execute(`DELETE FROM ${table} WHERE user_id = ?`, [user.id]);
    }
    console.log(`Usuario demo existente: se reinician sus datos (${EMAIL}).`);
  }

  const props = [
    ['[DEMO] Lote esquina Main St', 'land', 0, 0, 48000, 1200, 'm2', 'Terreno plano con acceso a calle pavimentada.'],
    ['[DEMO] Local comercial renta', 'commercial', 1, 0, 185000, 90, 'm2', 'Local con estacionamiento al frente.'],
    ['[DEMO] Terreno agrícola', 'land', 2, 1, 120000, 2.5, 'acre', 'Parcela cerca de canal de riego.'],
    ['[DEMO] Casa para remodelar', 'residential', 3, 0, 98000, 1500, 'sqft', 'Requiere remodelación completa.'],
    ['[DEMO] Lote en esquina comercial', 'land', 0, 3, 75000, 0.3, 'acre', null],
  ];
  for (const [name, type, zi, pi, price, area, unit, notes] of props) {
    const p = at(zi, pi);
    const wkt = pointWkt(p.lon, p.lat);
    await createProperty(user.id, {
      name, propertyType: type, address: null, areaValue: area, areaUnit: unit, askingPrice: price, rentPrice: null,
      notes, sourceNote: 'Dato de demostración', reviewStatus: 'pending',
    }, wkt, await findZoneIdForPoint(wkt));
  }

  for (const s of usable.slice(0, 2)) await addToWatchlist(user.id, s.zone.id, '[DEMO] Zona de interés');

  const obs = [
    ['land_potential', '[DEMO] Terreno vacío en venta', 0, 2],
    ['commercial_space', '[DEMO] Local con letrero "Se renta"', 1, 2],
    ['competitor', '[DEMO] Tienda de conveniencia nueva', 2, 2],
    ['needs_research', '[DEMO] Obra en construcción', 3, 1],
  ];
  let minutesAgo = 15;
  for (const [tag, note, zi, pi] of obs) {
    const p = at(zi, pi);
    await upsertObservation(user.id, {
      id: randomUUID(), sessionId: null, tag, note, lat: p.lat, lon: p.lon,
      observedAt: new Date(Date.now() - minutesAgo * 60000).toISOString().slice(0, 23).replace('T', ' '),
    });
    minutesAgo += 40;
  }

  console.log(`Listo: ${props.length} propiedades, 2 zonas guardadas y ${obs.length} puntos de campo [DEMO].`);
  console.log(`Entra con  ${EMAIL}  /  ${PASSWORD}`);
} catch (err) {
  console.error('Error:', err.message);
  process.exitCode = 1;
} finally {
  await pool.end();
}
