import { isLat, isLon } from './geo.js';

export const PROPERTY_TYPES = ['land', 'commercial', 'residential', 'other'];
export const AREA_UNITS = ['sqft', 'acre', 'm2'];
export const REVIEW_STATUSES = ['pending', 'verified'];

const isNum = (n) => typeof n === 'number' && Number.isFinite(n);
const optText = (v, max, field, errors) => {
  if (v === undefined || v === null || v === '') return null;
  if (typeof v !== 'string' || v.length > max) { errors[field] = `Texto de máximo ${max} caracteres`; return null; }
  return v.trim() || null;
};
const optMoney = (v, field, errors) => {
  if (v === undefined || v === null) return null;
  if (!isNum(v) || v < 0 || v >= 1e12) { errors[field] = 'Debe ser un número entre 0 y 999999999999'; return null; }
  return v;
};

// Se usa igual para crear y para reemplazar (PUT).
export function validateProperty(body) {
  const b = body && typeof body === 'object' && !Array.isArray(body) ? body : {};
  const errors = {};

  const name = typeof b.name === 'string' ? b.name.trim() : '';
  if (name.length < 1 || name.length > 80) errors.name = 'El nombre debe tener entre 1 y 80 caracteres';

  const propertyType = b.propertyType === undefined ? 'other' : b.propertyType;
  if (!PROPERTY_TYPES.includes(propertyType)) errors.propertyType = `Debe ser uno de: ${PROPERTY_TYPES.join(', ')}`;

  if (!isLat(b.lat)) errors.lat = 'Latitud no válida (-90 a 90)';
  if (!isLon(b.lon)) errors.lon = 'Longitud no válida (-180 a 180)';

  let areaValue = null, areaUnit = null;
  if (b.areaValue !== undefined && b.areaValue !== null) {
    if (!isNum(b.areaValue) || b.areaValue <= 0 || b.areaValue >= 1e12) errors.areaValue = 'Debe ser un número mayor que 0';
    else areaValue = b.areaValue;
    if (!AREA_UNITS.includes(b.areaUnit)) errors.areaUnit = `Obligatoria con la superficie: ${AREA_UNITS.join(', ')}`;
    else areaUnit = b.areaUnit;
  } else if (b.areaUnit !== undefined && b.areaUnit !== null) {
    errors.areaValue = 'Falta la superficie para esa unidad';
  }

  const reviewStatus = b.reviewStatus === undefined ? 'pending' : b.reviewStatus;
  if (!REVIEW_STATUSES.includes(reviewStatus)) errors.reviewStatus = `Debe ser uno de: ${REVIEW_STATUSES.join(', ')}`;

  const value = {
    name, propertyType, lat: b.lat, lon: b.lon, areaValue, areaUnit,
    askingPrice: optMoney(b.askingPrice, 'askingPrice', errors),
    rentPrice: optMoney(b.rentPrice, 'rentPrice', errors),
    address: optText(b.address, 255, 'address', errors),
    notes: optText(b.notes, 500, 'notes', errors),
    sourceNote: optText(b.sourceNote, 255, 'sourceNote', errors),
    reviewStatus,
  };
  return Object.keys(errors).length ? { errors } : { value };
}

export function validateWatchlistAdd(body) {
  const b = body && typeof body === 'object' && !Array.isArray(body) ? body : {};
  const errors = {};
  if (!Number.isInteger(b.zoneId) || b.zoneId <= 0) errors.zoneId = 'zoneId debe ser un entero positivo';
  let note = null;
  if (b.note !== undefined && b.note !== null) {
    if (typeof b.note !== 'string' || b.note.length > 500) errors.note = 'Máximo 500 caracteres';
    else note = b.note.trim() || null;
  }
  return Object.keys(errors).length ? { errors } : { value: { zoneId: b.zoneId, note } };
}
