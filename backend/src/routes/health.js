import { Router } from 'express';
import { checkDb } from '../db.js';

const router = Router();

// GET /api/health -> estado del servidor y de la base de datos.
// Responde 200 aunque MySQL esté caído (el servidor sí está vivo); el
// campo "db.connected" indica el estado real.
router.get('/', async (_req, res) => {
  const db = await checkDb();
  res.json({
    status: db.connected ? 'ok' : 'degraded',
    service: 'terrascope-backend',
    time: new Date().toISOString(),
    db,
  });
});

export default router;
