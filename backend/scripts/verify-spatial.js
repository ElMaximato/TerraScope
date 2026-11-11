// Verifica que este MySQL soporta lo que TerraScope necesita:
// distancia esférica, punto-en-polígono, orden de ejes e índices espaciales.
// Las coordenadas son PUNTOS DE PRUEBA (no son datos reales de negocios).
import 'dotenv/config';
import mysql from 'mysql2/promise';

const conn = await mysql.createConnection({
  host: process.env.DB_HOST || '127.0.0.1',
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || 'root',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME || 'terrascope',
});

const results = [];
const check = (name, ok, detail) => {
  results.push(ok);
  console.log(`${ok ? 'OK   ' : 'FALLA'}  ${name}${detail ? ' - ' + detail : ''}`);
};

const pt = (lon, lat) => `ST_GeomFromText('POINT(${lon} ${lat})', 4326, 'axis-order=long-lat')`;

function haversine(lat1, lon1, lat2, lon2) {
  const R = 6371000, rad = (d) => (d * Math.PI) / 180;
  const a = Math.sin(rad(lat2 - lat1) / 2) ** 2 +
    Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(rad(lon2 - lon1) / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(a));
}

try {
  const [[v]] = await conn.query('SELECT VERSION() AS v');
  check('Versión de MySQL >= 8.0.14', /^8\.\d+/.test(v.v) || parseInt(v.v) > 8, v.v);

  // 1) Orden de ejes: lat debe salir 32.6 aunque insertamos POINT(lon lat)
  const [[ax]] = await conn.query(`SELECT ST_Latitude(${pt(-114.71, 32.6)}) AS lat, ST_Longitude(${pt(-114.71, 32.6)}) AS lon`);
  check('Orden de ejes (long-lat) correcto', Math.abs(ax.lat - 32.6) < 1e-9 && Math.abs(ax.lon + 114.71) < 1e-9, `lat=${ax.lat} lon=${ax.lon}`);

  // 2) ST_Distance_Sphere vs haversine calculado en JS
  const [[d]] = await conn.query(`SELECT ST_Distance_Sphere(${pt(-114.75, 32.57)}, ${pt(-114.67, 32.63)}) AS m`);
  const expected = haversine(32.57, -114.75, 32.63, -114.67);
  const err = Math.abs(d.m - expected) / expected;
  check('ST_Distance_Sphere coincide con haversine (<0.5%)', err < 0.005, `MySQL=${Math.round(d.m)} m, JS=${Math.round(expected)} m`);

  // 3) ST_Contains con un polígono de prueba
  const poly = `ST_GeomFromText('POLYGON((-114.75 32.57, -114.67 32.57, -114.67 32.63, -114.75 32.63, -114.75 32.57))', 4326, 'axis-order=long-lat')`;
  const [[c]] = await conn.query(`SELECT ST_Contains(${poly}, ${pt(-114.71, 32.60)}) AS dentro, ST_Contains(${poly}, ${pt(-114.71, 32.70)}) AS fuera`);
  check('ST_Contains: punto dentro = 1, punto fuera = 0', c.dentro === 1 && c.fuera === 0, `dentro=${c.dentro} fuera=${c.fuera}`);

  // 4) Área geodésica del polígono (m2) para confirmar soporte geográfico
  const [[a]] = await conn.query(`SELECT ST_Area(${poly}) AS m2`);
  check('ST_Area devuelve metros cuadrados', a.m2 > 1e7 && a.m2 < 1e9, `${Math.round(a.m2)} m2`);

  // 5) Índices espaciales creados por schema.sql
  const [idx] = await conn.query(
    `SELECT TABLE_NAME, INDEX_NAME FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND INDEX_TYPE = 'SPATIAL'`);
  const names = idx.map((r) => `${r.TABLE_NAME}.${r.INDEX_NAME}`).sort();
  check('Índices SPATIAL en zones y places', names.length >= 2, names.join(', ') || 'ninguno (¿corriste npm run db:setup?)');
} catch (err) {
  check('Ejecución sin errores', false, `${err.code || ''} ${err.message}`);
} finally {
  await conn.end();
}

const failed = results.filter((x) => !x).length;
console.log(failed === 0 ? '\nRESULTADO: todo correcto.' : `\nRESULTADO: ${failed} verificación(es) fallaron.`);
process.exitCode = failed === 0 ? 0 : 1;
