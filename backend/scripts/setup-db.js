// Aplica las migraciones pendientes sobre la base configurada en .env.
import 'dotenv/config';
import { migrate } from '../src/migrate.js';

try {
  await migrate({ log: console.log });
  console.log('Base de datos al día.');
} catch (err) {
  console.error('Error aplicando migraciones:', err.code || '', err.message);
  process.exitCode = 1;
}
