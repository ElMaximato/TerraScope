import { AppError, notFound, toErrorBody } from '../lib/errors.js';

export function notFoundHandler(_req, _res, next) {
  next(notFound('Ruta no encontrada'));
}

// Express reconoce un manejador de errores por sus 4 parámetros.
export function errorHandler(err, _req, res, _next) {
  if (err instanceof AppError) return res.status(err.status).json(toErrorBody(err));
  if (err.type === 'entity.parse.failed') {
    return res.status(400).json({ error: { code: 'invalid_json', message: 'El cuerpo no es JSON válido' } });
  }
  if (err.type === 'entity.too.large') {
    return res.status(413).json({ error: { code: 'payload_too_large', message: 'Cuerpo demasiado grande' } });
  }
  console.error(err); // nunca se registran contraseñas: no se loguea el cuerpo de la petición
  res.status(500).json({ error: { code: 'internal_error', message: 'Error interno' } });
}
