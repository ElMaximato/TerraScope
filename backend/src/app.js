import path from 'node:path';
import { fileURLToPath } from 'node:url';
import express from 'express';
import cors from 'cors';
import healthRouter from './routes/health.js';
import authRouter from './routes/auth.js';
import zonesRouter from './routes/zones.js';
import propertiesRouter from './routes/properties.js';
import watchlistRouter from './routes/watchlist.js';
import fieldRouter from './routes/field.js';
import { requireAuth } from './middleware/auth.js';
import { errorHandler, notFoundHandler } from './middleware/errorHandler.js';
import { notFound } from './lib/errors.js';

const app = express();
const openapiFile = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..', 'docs', 'api', 'openapi.yaml');

app.disable('x-powered-by');

// Cabeceras de seguridad básicas (sin dependencias). Las respuestas de la API no deben guardarse en caché.
app.use((req, res, next) => {
  res.set({
    'X-Content-Type-Options': 'nosniff',
    'X-Frame-Options': 'DENY',
    'Referrer-Policy': 'no-referrer',
    'Cross-Origin-Resource-Policy': 'same-site',
  });
  if (req.path.startsWith('/api/') && req.path !== '/api/openapi.yaml') res.set('Cache-Control', 'no-store');
  next();
});

// La app móvil no usa CORS. Solo se habilita si se define CORS_ORIGIN (por ejemplo, una página web de pruebas).
if (process.env.CORS_ORIGIN) app.use(cors({ origin: process.env.CORS_ORIGIN.split(',').map((s) => s.trim()) }));
app.use(express.json({ limit: '100kb' }));

app.use('/api/health', healthRouter);
app.get('/api/openapi.yaml', (_req, res, next) => {
  res.type('text/yaml').sendFile(openapiFile, (err) => { if (err) next(notFound('Documentación no disponible')); });
});
app.use('/api/auth', authRouter);

// Todo lo siguiente exige sesión (token) y trabaja solo con los datos del usuario.
app.use('/api/zones', requireAuth, zonesRouter);
app.use('/api/properties', requireAuth, propertiesRouter);
app.use('/api/watchlist', requireAuth, watchlistRouter);
app.use('/api/field', requireAuth, fieldRouter);

app.use(notFoundHandler);
app.use(errorHandler);

export default app;
