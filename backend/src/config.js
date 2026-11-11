export function getJwtConfig() {
  const secret = process.env.JWT_SECRET || '';
  if (secret.length < 32) {
    throw new Error('JWT_SECRET debe tener al menos 32 caracteres (ver backend/.env.example)');
  }
  return { secret, expiresIn: process.env.JWT_EXPIRES_IN || '7d' };
}
