import { Router } from 'express';
import { asyncHandler, badRequest, notFound } from '../lib/errors.js';
import { parseId, parsePaging } from '../lib/params.js';
import { pointWkt } from '../lib/geo.js';
import { validateProperty } from '../lib/validateProperty.js';
import { findZoneIdForPoint } from '../repositories/zones.js';
import { createProperty, deleteProperty, getProperty, listProperties, updateProperty } from '../repositories/properties.js';

const router = Router();

function idOrFail(req) {
  const id = parseId(req.params.id);
  if (!id) throw badRequest('Id no válido');
  return id;
}

router.get('/', asyncHandler(async (req, res) => {
  res.json({ properties: await listProperties(req.user.id, parsePaging(req.query)) });
}));

router.post('/', asyncHandler(async (req, res) => {
  const { value, errors } = validateProperty(req.body);
  if (errors) throw badRequest('Datos no válidos', errors);
  const wkt = pointWkt(value.lon, value.lat);
  const zoneId = await findZoneIdForPoint(wkt); // asociación automática con la zona (RF-014)
  const id = await createProperty(req.user.id, value, wkt, zoneId);
  res.status(201).json({ property: await getProperty(req.user.id, id) });
}));

router.get('/:id', asyncHandler(async (req, res) => {
  const property = await getProperty(req.user.id, idOrFail(req));
  if (!property) throw notFound('Propiedad no encontrada'); // también cuando es de otro usuario
  res.json({ property });
}));

router.put('/:id', asyncHandler(async (req, res) => {
  const id = idOrFail(req);
  const { value, errors } = validateProperty(req.body);
  if (errors) throw badRequest('Datos no válidos', errors);
  if (!(await getProperty(req.user.id, id))) throw notFound('Propiedad no encontrada');
  const wkt = pointWkt(value.lon, value.lat);
  await updateProperty(req.user.id, id, value, wkt, await findZoneIdForPoint(wkt));
  res.json({ property: await getProperty(req.user.id, id) });
}));

router.delete('/:id', asyncHandler(async (req, res) => {
  if (!(await deleteProperty(req.user.id, idOrFail(req)))) throw notFound('Propiedad no encontrada');
  res.status(204).end();
}));

export default router;
