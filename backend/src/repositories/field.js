import { pool } from '../db.js';
import { pointWkt } from '../lib/geo.js';

// ---- Escritura idempotente: reenviar lo mismo nunca duplica ----

// -> 'created' | 'updated' | 'duplicate' | 'conflict'
export async function upsertSession(userId, s) {
  try {
    await pool.execute(
      'INSERT INTO field_sessions (id, user_id, started_at, ended_at, device) VALUES (?, ?, ?, ?, ?)',
      [s.id, userId, s.startedAt, s.endedAt, s.device]);
    return 'created';
  } catch (err) {
    if (err.code !== 'ER_DUP_ENTRY') throw err;
  }
  const [rows] = await pool.execute('SELECT user_id FROM field_sessions WHERE id = ?', [s.id]);
  if (!rows.length || rows[0].user_id !== userId) return 'conflict'; // el id es de otro usuario
  if (s.endedAt) {
    await pool.execute('UPDATE field_sessions SET ended_at = ?, device = COALESCE(?, device) WHERE id = ? AND user_id = ?',
      [s.endedAt, s.device, s.id, userId]);
    return 'updated';
  }
  return 'duplicate';
}

// -> 'created' | 'duplicate' | 'conflict' | 'invalid_session'
export async function upsertObservation(userId, o) {
  if (o.sessionId) {
    const [s] = await pool.execute('SELECT user_id FROM field_sessions WHERE id = ?', [o.sessionId]);
    if (!s.length || s[0].user_id !== userId) return 'invalid_session';
  }
  try {
    await pool.execute(
      `INSERT INTO field_observations (id, user_id, session_id, tag, note, location, observed_at)
       VALUES (?, ?, ?, ?, ?, ST_GeomFromText(?, 4326, 'axis-order=long-lat'), ?)`,
      [o.id, userId, o.sessionId, o.tag, o.note, pointWkt(o.lon, o.lat), o.observedAt]);
    return 'created';
  } catch (err) {
    if (err.code !== 'ER_DUP_ENTRY') throw err;
  }
  const [rows] = await pool.execute('SELECT user_id FROM field_observations WHERE id = ?', [o.id]);
  if (!rows.length || rows[0].user_id !== userId) return 'conflict';
  // Mismo id reenviado: el reloj pudo cambiar la etiqueta después del primer envío.
  const [r] = await pool.execute(
    'UPDATE field_observations SET tag = ?, note = COALESCE(?, note) WHERE id = ? AND user_id = ? AND (tag <> ? OR (? IS NOT NULL AND note <=> ? IS FALSE))',
    [o.tag, o.note, o.id, userId, o.tag, o.note, o.note]);
  return r.affectedRows ? 'updated' : 'duplicate';
}

// ---- Lectura (solo lo propio) ----
export async function listSessions(userId, { limit, offset }) {
  const [rows] = await pool.query(
    `SELECT id, started_at, ended_at, device, created_at FROM field_sessions
     WHERE user_id = ? ORDER BY started_at DESC LIMIT ? OFFSET ?`, [userId, limit, offset]);
  return rows.map((r) => ({ id: r.id, startedAt: r.started_at, endedAt: r.ended_at, device: r.device, createdAt: r.created_at }));
}

export async function listObservations(userId, { sessionId, limit, offset }) {
  const params = [userId];
  let where = 'WHERE user_id = ?';
  if (sessionId) { where += ' AND session_id = ?'; params.push(sessionId); }
  params.push(limit, offset);
  const [rows] = await pool.query(
    `SELECT id, session_id, tag, note, ST_Latitude(location) AS lat, ST_Longitude(location) AS lon,
       observed_at, created_at FROM field_observations ${where}
     ORDER BY observed_at DESC LIMIT ? OFFSET ?`, params);
  return rows.map((r) => ({
    id: r.id, sessionId: r.session_id, tag: r.tag, note: r.note, lat: r.lat, lon: r.lon,
    observedAt: r.observed_at, createdAt: r.created_at,
  }));
}

// ---- Editar / borrar (solo lo propio). La ubicación no se edita. ----
// -> true si existía y era del usuario
export async function updateObservation(userId, id, { tag, note }) {
  const sets = []; const params = [];
  if (tag !== undefined) { sets.push('tag = ?'); params.push(tag); }
  if (note !== undefined) { sets.push('note = ?'); params.push(note); }
  const [exists] = await pool.execute('SELECT id FROM field_observations WHERE id = ? AND user_id = ?', [id, userId]);
  if (!exists.length) return false;
  if (sets.length) await pool.execute(`UPDATE field_observations SET ${sets.join(', ')} WHERE id = ? AND user_id = ?`, [...params, id, userId]);
  return true;
}

export async function deleteObservation(userId, id) {
  const [r] = await pool.execute('DELETE FROM field_observations WHERE id = ? AND user_id = ?', [id, userId]);
  return r.affectedRows > 0;
}
