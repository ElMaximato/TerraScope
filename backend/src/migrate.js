import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import mysql from 'mysql2/promise';

const dir = path.join(path.dirname(fileURLToPath(import.meta.url)), '..', 'db', 'migrations');

// Aplica en orden las migraciones .sql que aún no se hayan aplicado.
// Lee la conexión de process.env en el momento de llamarla.
export async function migrate({ log = () => {} } = {}) {
  const conn = await mysql.createConnection({
    host: process.env.DB_HOST || '127.0.0.1',
    port: Number(process.env.DB_PORT || 3306),
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'terrascope',
    multipleStatements: true,
  });
  const applied = [];
  try {
    await conn.query(`CREATE TABLE IF NOT EXISTS schema_migrations (
      name VARCHAR(100) NOT NULL PRIMARY KEY,
      applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)`);
    const [rows] = await conn.query('SELECT name FROM schema_migrations');
    const done = new Set(rows.map((r) => r.name));
    const files = fs.readdirSync(dir).filter((f) => f.endsWith('.sql')).sort();
    for (const file of files) {
      if (done.has(file)) continue;
      await conn.query(fs.readFileSync(path.join(dir, file), 'utf8'));
      await conn.query('INSERT IGNORE INTO schema_migrations (name) VALUES (?)', [file]);
      applied.push(file);
      log(`Migración aplicada: ${file}`);
    }
    if (applied.length === 0) log('Sin migraciones pendientes.');
    return applied;
  } finally {
    await conn.end();
  }
}
