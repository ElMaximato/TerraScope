import { pool } from '../db.js';

export async function listZones() {
  const [rows] = await pool.query('SELECT id, code, name, level FROM zones ORDER BY level, name');
  return rows;
}

export async function listIndicators() {
  const [rows] = await pool.query('SELECT id, code, name, category, unit, description FROM indicators ORDER BY category, id');
  return rows;
}

export async function getZoneById(id, { includeBoundary = true } = {}) {
  const geomSelect = includeBoundary ? ', ST_AsGeoJSON(z.boundary) AS boundary_geojson' : '';
  const [rows] = await pool.execute(
    `SELECT z.id, z.code, z.name, z.level, z.source_id${geomSelect},
            s.name AS source_name, s.organization AS source_organization,
            s.license AS source_license, s.reference_period AS source_period
     FROM zones z
     LEFT JOIN data_sources s ON z.source_id = s.id
     WHERE z.id = ?`,
    [id]
  );
  if (!rows.length) return null;
  const r = rows[0];

  const [indRows] = await pool.execute(
    `SELECT i.code, i.name, i.category, i.unit, i.description,
            iv.value, iv.margin_of_error, iv.reference_period,
            s.name AS source_name, s.organization AS source_organization,
            s.license AS source_license
     FROM indicator_values iv
     JOIN indicators i ON iv.indicator_id = i.id
     LEFT JOIN data_sources s ON iv.source_id = s.id
     WHERE iv.zone_id = ?
     ORDER BY i.category, i.id`,
    [id]
  );

  return {
    id: r.id,
    code: r.code,
    name: r.name,
    level: r.level,
    boundary: typeof r.boundary_geojson === 'string'
      ? JSON.parse(r.boundary_geojson)
      : (r.boundary_geojson || undefined),
    source: r.source_name
      ? {
          name: r.source_name,
          organization: r.source_organization,
          license: r.source_license,
          referencePeriod: r.source_period,
        }
      : null,
    indicators: indRows.map((iv) => ({
      code: iv.code,
      name: iv.name,
      category: iv.category,
      unit: iv.unit,
      description: iv.description,
      value: Number(iv.value),
      marginOfError: iv.margin_of_error !== null ? Number(iv.margin_of_error) : null,
      referencePeriod: iv.reference_period,
      source: iv.source_name
        ? {
            name: iv.source_name,
            organization: iv.source_organization,
            license: iv.source_license,
          }
        : null,
    })),
  };
}

export async function zoneExists(id) {
  const [rows] = await pool.execute('SELECT id FROM zones WHERE id = ?', [id]);
  return rows.length > 0;
}

// Zona más específica que contiene el punto (block_group > tract > place), o null.
export async function findZoneIdForPoint(wkt) {
  const [rows] = await pool.execute(
    `SELECT id FROM zones
     WHERE ST_Contains(boundary, ST_GeomFromText(?, 4326, 'axis-order=long-lat'))
     ORDER BY FIELD(level, 'block_group', 'tract', 'place') LIMIT 1`, [wkt]);
  return rows.length ? rows[0].id : null;
}

// Zona censal (tract) que contiene el punto, o null. El puntaje solo existe a nivel tract.
export async function findTractIdForPoint(wkt) {
  const [rows] = await pool.execute(
    `SELECT id FROM zones WHERE level = 'tract'
     AND ST_Contains(boundary, ST_GeomFromText(?, 4326, 'axis-order=long-lat')) LIMIT 1`, [wkt]);
  return rows.length ? rows[0].id : null;
}
