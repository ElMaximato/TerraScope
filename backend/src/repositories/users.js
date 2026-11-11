import { pool } from '../db.js';

export async function createUser({ email, passwordHash, displayName }) {
  const [r] = await pool.execute(
    'INSERT INTO users (email, password_hash, display_name) VALUES (?, ?, ?)',
    [email, passwordHash, displayName]);
  return r.insertId;
}

export async function findByEmail(email) {
  const [rows] = await pool.execute(
    'SELECT id, email, password_hash, display_name, created_at FROM users WHERE email = ?', [email]);
  return rows[0] || null;
}

export async function findById(id) {
  const [rows] = await pool.execute(
    'SELECT id, email, display_name, created_at FROM users WHERE id = ?', [id]);
  return rows[0] || null;
}

// Nunca expone password_hash.
export function publicUser(u) {
  return { id: u.id, email: u.email, displayName: u.display_name, createdAt: u.created_at };
}
