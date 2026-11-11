import { verifyToken } from '../lib/token.js';
import { unauthorized } from '../lib/errors.js';

// Exige "Authorization: Bearer <token>" y deja el id en req.user.id.
export function requireAuth(req, _res, next) {
  const header = req.get('authorization') || '';
  const [scheme, token] = header.split(' ');
  const decoded = scheme === 'Bearer' && token ? verifyToken(token) : null;
  if (!decoded) return next(unauthorized('Sesión no válida o expirada'));
  req.user = { id: decoded.userId };
  next();
}
