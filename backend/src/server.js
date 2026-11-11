import 'dotenv/config';
import { getJwtConfig } from './config.js';

try {
  getJwtConfig(); // falla pronto si falta JWT_SECRET
} catch (err) {
  console.error(`Configuración inválida: ${err.message}`);
  process.exit(1);
}

const { default: app } = await import('./app.js');
const port = Number(process.env.PORT || 3000);

// 0.0.0.0 permite que el emulador de Android (10.0.2.2) alcance el servidor.
app.listen(port, '0.0.0.0', () => {
  console.log(`TerraScope backend escuchando en http://localhost:${port}`);
  console.log(`Salud: http://localhost:${port}/api/health`);
});
