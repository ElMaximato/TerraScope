import { tooManyRequests } from '../lib/errors.js';

// Límite simple en memoria (ventana fija). Suficiente para un prototipo local.
export function createRateLimiter({ windowMs, max, now = Date.now, keyOf = (req) => `${req.ip}:${req.path}` }) {
  const hits = new Map(); // clave -> { count, resetAt }

  function hit(key) {
    const t = now();
    if (hits.size > 1000) {
      for (const [k, v] of hits) if (v.resetAt <= t) hits.delete(k);
    }
    let entry = hits.get(key);
    if (!entry || entry.resetAt <= t) {
      entry = { count: 0, resetAt: t + windowMs };
      hits.set(key, entry);
    }
    entry.count += 1;
    return {
      allowed: entry.count <= max,
      retryAfterSec: Math.max(1, Math.ceil((entry.resetAt - t) / 1000)),
    };
  }

  function middleware(req, res, next) {
    const result = hit(keyOf(req));
    if (!result.allowed) {
      res.set('Retry-After', String(result.retryAfterSec));
      return next(tooManyRequests());
    }
    next();
  }

  return { hit, middleware };
}

// Límite para registro e inicio de sesión (configurable por entorno).
export const authLimiter = createRateLimiter({
  windowMs: 15 * 60 * 1000,
  max: Number(process.env.RATE_LIMIT_MAX || 10),
}).middleware;

// Límite por usuario para las llamadas que usan IA (?ai=1): cuidan la cuota del proveedor.
const aiLimiter = createRateLimiter({
  windowMs: 10 * 60 * 1000,
  max: Number(process.env.AI_RATE_LIMIT_MAX || 10),
  keyOf: (req) => `ai:${req.user?.id ?? req.ip}`,
}).middleware;

export function aiRateLimit(req, res, next) {
  return req.query.ai === '1' ? aiLimiter(req, res, next) : next();
}
