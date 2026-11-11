import { getZoneById, listZones } from '../repositories/zones.js';

/** Todas las zonas censales (tracts) con sus indicadores, para calcular puntajes comparativos. */
export async function loadTracts() {
  const rows = (await listZones()).filter((z) => z.level === 'tract');
  const tracts = [];
  for (const z of rows) {
    const d = await getZoneById(z.id, { includeBoundary: false });
    tracts.push({ id: z.id, code: z.code, name: z.name, indicators: d.indicators });
  }
  return tracts;
}
