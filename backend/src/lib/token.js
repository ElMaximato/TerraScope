import jwt from 'jsonwebtoken';
import { getJwtConfig } from '../config.js';

export function signToken(userId) {
  const { secret, expiresIn } = getJwtConfig();
  return jwt.sign({ sub: String(userId) }, secret, { algorithm: 'HS256', expiresIn });
}

// Devuelve { userId } o null si el token no es válido o expiró.
export function verifyToken(token) {
  const { secret } = getJwtConfig();
  try {
    const payload = jwt.verify(token, secret, { algorithms: ['HS256'] });
    const userId = Number(payload.sub);
    return Number.isInteger(userId) && userId > 0 ? { userId } : null;
  } catch {
    return null;
  }
}
