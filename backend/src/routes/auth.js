import { Router } from 'express';
import { asyncHandler, badRequest, conflict, unauthorized } from '../lib/errors.js';
import { validateLogin, validateRegister } from '../lib/validate.js';
import { hashPassword, verifyPassword } from '../lib/password.js';
import { signToken } from '../lib/token.js';
import { authLimiter } from '../middleware/rateLimit.js';
import { requireAuth } from '../middleware/auth.js';
import { createUser, findByEmail, findById, publicUser } from '../repositories/users.js';

const router = Router();

// Hash de relleno para que el login tarde parecido exista o no el correo.
const DUMMY_HASH = await hashPassword('relleno-no-usar');

router.post('/register', authLimiter, asyncHandler(async (req, res) => {
  const { value, errors } = validateRegister(req.body);
  if (errors) throw badRequest('Datos no válidos', errors);

  const passwordHash = await hashPassword(value.password);
  let id;
  try {
    id = await createUser({ email: value.email, passwordHash, displayName: value.displayName });
  } catch (err) {
    if (err.code === 'ER_DUP_ENTRY') throw conflict('email_taken', 'Ese correo ya está registrado');
    throw err;
  }
  const user = await findById(id);
  res.status(201).json({ user: publicUser(user), token: signToken(id) });
}));

router.post('/login', authLimiter, asyncHandler(async (req, res) => {
  const { value, errors } = validateLogin(req.body);
  if (errors) throw badRequest('Datos no válidos', errors);

  const user = await findByEmail(value.email);
  const ok = await verifyPassword(value.password, user ? user.password_hash : DUMMY_HASH);
  if (!user || !ok) throw unauthorized('Correo o contraseña incorrectos'); // mismo mensaje en ambos casos
  res.json({ user: publicUser(user), token: signToken(user.id) });
}));

router.get('/me', requireAuth, asyncHandler(async (req, res) => {
  const user = await findById(req.user.id);
  if (!user) throw unauthorized('Sesión no válida o expirada');
  res.json({ user: publicUser(user) });
}));

export default router;
