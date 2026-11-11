import { Router } from 'express';
import { asyncHandler, badRequest, AppError, notFound } from '../lib/errors.js';
import { parseId } from '../lib/params.js';
import { validateWatchlistAdd } from '../lib/validateProperty.js';
import { zoneExists } from '../repositories/zones.js';
import { addToWatchlist, listWatchlist, removeFromWatchlist } from '../repositories/watchlist.js';

const router = Router();

router.get('/', asyncHandler(async (req, res) => {
  res.json({ items: await listWatchlist(req.user.id) });
}));

// 201 si se guardó la zona; 200 si ya estaba (solo se actualiza la nota).
router.post('/', asyncHandler(async (req, res) => {
  const { value, errors } = validateWatchlistAdd(req.body);
  if (errors) throw badRequest('Datos no válidos', errors);
  if (!(await zoneExists(value.zoneId))) throw new AppError(404, 'zone_not_found', 'La zona no existe');
  const created = await addToWatchlist(req.user.id, value.zoneId, value.note);
  res.status(created ? 201 : 200).json({ items: await listWatchlist(req.user.id) });
}));

router.delete('/:zoneId', asyncHandler(async (req, res) => {
  const zoneId = parseId(req.params.zoneId);
  if (!zoneId) throw badRequest('Id de zona no válido');
  if (!(await removeFromWatchlist(req.user.id, zoneId))) throw notFound('Esa zona no está en tu Watchlist');
  res.status(204).end();
}));

export default router;
