// Error de aplicación con código HTTP y código estable para el cliente.
export class AppError extends Error {
  constructor(status, code, message, details) {
    super(message);
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

export const badRequest = (message, details) => new AppError(400, 'validation_error', message, details);
export const unauthorized = (message = 'No autorizado') => new AppError(401, 'unauthorized', message);
export const notFound = (message = 'No encontrado') => new AppError(404, 'not_found', message);
export const conflict = (code, message) => new AppError(409, code, message);
export const tooManyRequests = (message = 'Demasiados intentos, espera un momento') =>
  new AppError(429, 'rate_limited', message);

// Forma única de las respuestas de error: { error: { code, message, details? } }
export function toErrorBody(err) {
  const error = { code: err.code, message: err.message };
  if (err.details) error.details = err.details;
  return { error };
}

// Envuelve handlers async para que Express 4 reciba sus errores.
export const asyncHandler = (fn) => (req, res, next) => Promise.resolve(fn(req, res, next)).catch(next);
