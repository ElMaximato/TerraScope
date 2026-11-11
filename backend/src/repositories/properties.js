import { pool } from '../db.js';

const SELECT = `SELECT id, name, property_type, address, ST_Latitude(location) AS lat,
  ST_Longitude(location) AS lon, area_value, area_unit, asking_price, rent_price, notes,
  source_note, review_status, zone_id, created_at, updated_at FROM properties`;

const num = (v) => (v === null || v === undefined ? null : Number(v));

export function publicProperty(r) {
  return {
    id: r.id, name: r.name, propertyType: r.property_type, address: r.address,
    lat: r.lat, lon: r.lon, areaValue: num(r.area_value), areaUnit: r.area_unit,
    askingPrice: num(r.asking_price), rentPrice: num(r.rent_price), notes: r.notes,
    sourceNote: r.source_note, reviewStatus: r.review_status, zoneId: r.zone_id,
    dataType: 'user', // siempre dato aportado por el usuario
    createdAt: r.created_at, updatedAt: r.updated_at,
  };
}

export async function listProperties(userId, { limit, offset }) {
  const [rows] = await pool.query(`${SELECT} WHERE user_id = ? ORDER BY id DESC LIMIT ? OFFSET ?`,
    [userId, limit, offset]);
  return rows.map(publicProperty);
}

// Todas las consultas filtran por user_id: un usuario nunca alcanza datos de otro.
export async function getProperty(userId, id) {
  const [rows] = await pool.execute(`${SELECT} WHERE user_id = ? AND id = ?`, [userId, id]);
  return rows.length ? publicProperty(rows[0]) : null;
}

export async function createProperty(userId, v, wkt, zoneId) {
  const [r] = await pool.execute(
    `INSERT INTO properties (user_id, name, property_type, address, location, area_value, area_unit,
       asking_price, rent_price, notes, source_note, review_status, zone_id)
     VALUES (?, ?, ?, ?, ST_GeomFromText(?, 4326, 'axis-order=long-lat'), ?, ?, ?, ?, ?, ?, ?, ?)`,
    [userId, v.name, v.propertyType, v.address, wkt, v.areaValue, v.areaUnit,
      v.askingPrice, v.rentPrice, v.notes, v.sourceNote, v.reviewStatus, zoneId]);
  return r.insertId;
}

export async function updateProperty(userId, id, v, wkt, zoneId) {
  await pool.execute(
    `UPDATE properties SET name = ?, property_type = ?, address = ?,
       location = ST_GeomFromText(?, 4326, 'axis-order=long-lat'), area_value = ?, area_unit = ?,
       asking_price = ?, rent_price = ?, notes = ?, source_note = ?, review_status = ?, zone_id = ?
     WHERE id = ? AND user_id = ?`,
    [v.name, v.propertyType, v.address, wkt, v.areaValue, v.areaUnit, v.askingPrice,
      v.rentPrice, v.notes, v.sourceNote, v.reviewStatus, zoneId, id, userId]);
}

export async function deleteProperty(userId, id) {
  const [r] = await pool.execute('DELETE FROM properties WHERE id = ? AND user_id = ?', [id, userId]);
  return r.affectedRows > 0;
}
