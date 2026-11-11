import { pool } from '../db.js';

export async function listWatchlist(userId) {
  const [rows] = await pool.execute(
    `SELECT w.id, w.zone_id, w.note, w.created_at, z.code, z.name, z.level
     FROM watchlist_items w JOIN zones z ON z.id = w.zone_id
     WHERE w.user_id = ? ORDER BY w.created_at DESC, w.id DESC`, [userId]);
  return rows.map((r) => ({
    id: r.id, zoneId: r.zone_id, note: r.note, createdAt: r.created_at,
    zone: { code: r.code, name: r.name, level: r.level },
  }));
}

// Devuelve true si se creó, false si ya existía (en ese caso solo actualiza la nota).
export async function addToWatchlist(userId, zoneId, note) {
  try {
    await pool.execute('INSERT INTO watchlist_items (user_id, zone_id, note) VALUES (?, ?, ?)',
      [userId, zoneId, note]);
    return true;
  } catch (err) {
    if (err.code !== 'ER_DUP_ENTRY') throw err;
    await pool.execute('UPDATE watchlist_items SET note = ? WHERE user_id = ? AND zone_id = ?',
      [note, userId, zoneId]);
    return false;
  }
}

export async function removeFromWatchlist(userId, zoneId) {
  const [r] = await pool.execute('DELETE FROM watchlist_items WHERE user_id = ? AND zone_id = ?', [userId, zoneId]);
  return r.affectedRows > 0;
}
