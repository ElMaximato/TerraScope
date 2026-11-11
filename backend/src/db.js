import mysql from 'mysql2/promise';

// Pool de conexiones. mysql2 no abre conexión hasta la primera consulta,
// así que el servidor puede arrancar aunque MySQL esté apagado.
// Todo se maneja en UTC: el driver y la sesión de MySQL (ver más abajo).
export const pool = mysql.createPool({
  host: process.env.DB_HOST || '127.0.0.1',
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || 'root',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME || 'terrascope',
  waitForConnections: true,
  connectionLimit: 5,
  connectTimeout: 3000,
  timezone: 'Z',
});

// Sin esto, los TIMESTAMP (created_at/updated_at) se devolverían desplazados
// según la zona horaria de la computadora donde corre MySQL.
pool.pool.on('connection', (conn) => {
  conn.query("SET time_zone = '+00:00'");
});

// Devuelve { connected, version?, error? } sin lanzar excepciones.
export async function checkDb() {
  try {
    const [rows] = await pool.query('SELECT VERSION() AS version');
    return { connected: true, version: rows[0].version };
  } catch (err) {
    return { connected: false, error: err.code || err.message };
  }
}
