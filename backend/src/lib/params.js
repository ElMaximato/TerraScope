// Id numérico positivo (como string de la URL) -> número, o null.
export function parseId(value) {
  if (typeof value !== 'string' || !/^\d{1,10}$/.test(value)) return null;
  const n = Number(value);
  return n > 0 && n <= 4294967295 ? n : null;
}

// ?limit=&offset= con valores por defecto y tope.
export function parsePaging(query = {}) {
  const limit = Number.parseInt(query.limit, 10);
  const offset = Number.parseInt(query.offset, 10);
  return {
    limit: Number.isInteger(limit) && limit > 0 ? Math.min(limit, 500) : 100,
    offset: Number.isInteger(offset) && offset > 0 ? offset : 0,
  };
}
