// Importador oficial de TIGERweb y Census ACS para Somerton, Arizona.
// Solo este script administra importación/descripción. No ejecuta migraciones.
import 'dotenv/config';
import fs from 'node:fs/promises';
import path from 'node:path';
import { createHash } from 'node:crypto';
import { createInterface } from 'node:readline/promises';
import { stdin as input, stdout as output } from 'node:process';
import { fileURLToPath } from 'node:url';
import mysql from 'mysql2/promise';
import { polygonGeoJsonToWkt, isLat, isLon } from '../src/lib/geo.js';

export const STATE = '04';
export const COUNTY = '027';
export const PLACE = '68080';
export const PLACE_GEOID = '0468080';
export const APPROVED_ZONE_GEOIDS = [
  '04027011506', '04027011505', '04027011802', '04027011503', '04027011501', '0468080',
];
export const ACS_REFERENCE_PERIOD = 'ACS 5-year 2020–2024';
export const TIGER_REFERENCE_PERIOD = 'TIGERweb ACS 2024';
export const EXPECTED_TRACTS = ['04027011501', '04027011502', '04027011503', '04027011403'];
export const OFFICIAL_ZONE_NAMES = {
  '04027011501': 'Census Tract 115.01',
  '04027011502': 'Census Tract 115.02',
  '04027011503': 'Census Tract 115.03',
  '04027011403': 'Census Tract 114.03',
  '0468080': 'Somerton city',
};
const REJECT_VALUES = new Set([-666666666, -888888888, -999999999]);
const TIGER_ORIGIN = 'https://tigerweb.geo.census.gov/arcgis/rest/services/TIGERweb';
const ACS_ORIGIN = 'https://api.census.gov/data';
const USER_AGENT = 'TerraScope-Somerton-Project/1.0 (academic development)';

function parseArgs(argv) {
  const args = new Set(argv.filter((a) => a.startsWith('--')));
  const yearArg = argv.find((a) => a.startsWith('--year='));
  const hashArg = argv.find((a) => a.startsWith('--expect-hash='));
  const year = Number(yearArg ? yearArg.split('=')[1] : '2024');
  if (!Number.isInteger(year) || year < 2000 || year > 2100) {
    throw new Error('--year debe ser un año entero válido entre 2000 y 2100');
  }
  return {
    dryRun: args.has('--dry-run'),
    yes: args.has('--yes'),
    year,
    expectHash: hashArg ? hashArg.split('=', 2)[1] : null,
  };
}

export function cleanAcsNumber(value) {
  if (value === null || value === undefined || value === '') return null;
  const n = Number(value);
  if (!Number.isFinite(n) || REJECT_VALUES.has(n)) return null;
  return n;
}

function canonicalize(value) {
  if (Array.isArray(value)) return value.map(canonicalize);
  if (value && typeof value === 'object') {
    return Object.keys(value).sort().reduce((out, key) => {
      out[key] = canonicalize(value[key]);
      return out;
    }, {});
  }
  return value;
}

export function computeCanonicalHash(value) {
  return createHash('sha256').update(JSON.stringify(canonicalize(value))).digest('hex');
}

export function calculatePopulationDensity(population, areaLandM2) {
  const cleanPopulation = cleanAcsNumber(population);
  const cleanArea = cleanAcsNumber(areaLandM2);
  if (cleanPopulation === null || cleanArea === null || cleanArea <= 0) return null;
  return Number((cleanPopulation / (cleanArea / 1_000_000)).toFixed(2));
}

export function validateExpectedHash(actualHash, expectedHash) {
  if (!expectedHash) return true;
  if (!/^[a-f0-9]{64}$/i.test(expectedHash)) {
    throw new Error('--expect-hash debe ser un SHA-256 de 64 caracteres hexadecimales.');
  }
  if (actualHash.toLowerCase() !== expectedHash.toLowerCase()) {
    throw new Error(`Hash de lote distinto al aprobado: esperado=${expectedHash.toLowerCase()}, recalculado=${actualHash}. Abortando sin escribir ni crear respaldos.`);
  }
  return true;
}

function computeTextHash(text) {
  return createHash('sha256').update(text).digest('hex');
}

async function fetchJson(url, label) {
  const response = await fetch(url, { headers: { 'User-Agent': USER_AGENT } });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(`${label} respondió HTTP ${response.status}: ${text.slice(0, 180)}`);
  }
  const text = await response.text();
  let parsed;
  try {
    parsed = JSON.parse(text);
  } catch {
    if (text.startsWith('{') && text.includes('"error"')) {
      throw new Error(`${label} devolvió un error ArcGIS/Census`);
    }
    throw new Error(`${label} respondió contenido que no es JSON (posible bloque, página de clave o HTML)`);
  }
  if (parsed?.error) {
    throw new Error(`${label} devolvió un error ArcGIS/Census: ${parsed.error.message || parsed.error.code || 'desconocido'}`);
  }
  return parsed;
}

function pointInRing(lon, lat, ring) {
  let inside = false;
  for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
    const a = ring[i];
    const b = ring[j];
    const intersect = ((a[1] > lat) !== (b[1] > lat))
      && (lon < (b[0] - a[0]) * (lat - a[1]) / ((b[1] - a[1]) || 1e-12) + a[0]);
    if (intersect) inside = !inside;
  }
  return inside;
}

function pointInPolygon(lon, lat, polygon) {
  if (!polygon || polygon.type !== 'Polygon') return false;
  const [outer, ...holes] = polygon.coordinates;
  if (!outer || !pointInRing(lon, lat, outer)) return false;
  return !holes.some((hole) => pointInRing(lon, lat, hole));
}

function geometryPolygons(geometry) {
  if (!geometry) return [];
  if (geometry.type === 'Polygon') return [{ type: 'Polygon', coordinates: geometry.coordinates }];
  if (geometry.type === 'MultiPolygon') {
    return geometry.coordinates.map((coordinates) => ({ type: 'Polygon', coordinates }));
  }
  return [];
}

function boundsIntersects(a, b) {
  const boxes = (geometry) => geometryPolygons(geometry).map((poly) => {
    const coords = poly.coordinates.flat();
    const lons = coords.map((c) => c[0]);
    const lats = coords.map((c) => c[1]);
    return { minLon: Math.min(...lons), maxLon: Math.max(...lons), minLat: Math.min(...lats), maxLat: Math.max(...lats) };
  });
  return boxes(a).some((x) => boxes(b).some((y) =>
    x.minLon <= y.maxLon && x.maxLon >= y.minLon && x.minLat <= y.maxLat && x.maxLat >= y.minLat));
}

export function polygonIntersects(a, b) {
  if (!boundsIntersects(a, b)) return false;
  const polygonsA = geometryPolygons(a);
  const polygonsB = geometryPolygons(b);
  for (const pa of polygonsA) {
    for (const pb of polygonsB) {
      const ra = pa.coordinates[0] || [];
      const rb = pb.coordinates[0] || [];
      for (const p of ra) if (pointInPolygon(p[0], p[1], pb)) return true;
      for (const p of rb) if (pointInPolygon(p[0], p[1], pa)) return true;
      for (let i = 0; i < ra.length - 1; i++) {
        for (let j = 0; j < rb.length - 1; j++) {
          const [ax1, ay1] = ra[i];
          const [ax2, ay2] = ra[i + 1];
          const [bx1, by1] = rb[j];
          const [bx2, by2] = rb[j + 1];
          const den = (ax1 - ax2) * (by1 - by2) - (ay1 - ay2) * (bx1 - bx2);
          if (den === 0) continue;
          const px = ((ax1 * ay2 - ay1 * ax2) * (bx1 - bx2) - (ax1 - ax2) * (bx1 * by2 - by1 * bx2)) / den;
          const py = ((ax1 * ay2 - ay1 * ax2) * (by1 - by2) - (ay1 - ay2) * (bx1 * by2 - by1 * bx2)) / den;
          const onA = px >= Math.min(ax1, ax2) - 1e-12 && px <= Math.max(ax1, ax2) + 1e-12
            && py >= Math.min(ay1, ay2) - 1e-12 && py <= Math.max(ay1, ay2) + 1e-12;
          const onB = px >= Math.min(bx1, bx2) - 1e-12 && px <= Math.max(bx1, bx2) + 1e-12
            && py >= Math.min(by1, by2) - 1e-12 && py <= Math.max(by1, by2) + 1e-12;
          if (onA && onB) return true;
        }
      }
    }
  }
  return false;
}

export function compareExpectedCoverage(foundGeoids) {
  const unique = [...new Set(foundGeoids)];
  return {
    found: unique,
    missing: EXPECTED_TRACTS.filter((geoid) => !unique.includes(geoid)),
    additional: unique.filter((geoid) => !EXPECTED_TRACTS.includes(geoid)),
    matchesExpected: EXPECTED_TRACTS.every((geoid) => unique.includes(geoid))
      && unique.every((geoid) => EXPECTED_TRACTS.includes(geoid)),
  };
}

function validateGeometry(feature) {
  const geoid = feature?.properties?.GEOID;
  const geometry = feature?.geometry;
  if (!geoid) throw new Error('GeoJSON TIGER sin GEOID');
  if (!geometry || !['Polygon', 'MultiPolygon'].includes(geometry.type)) {
    throw new Error(`Geometría inválida para ${geoid}: ${geometry?.type || 'sin geometría'}`);
  }
  const coords = geometry.type === 'Polygon' ? geometry.coordinates.flat(1) : geometry.coordinates.flat(2);
  if (!coords.length) throw new Error(`Geometría vacía para ${geoid}`);
  for (const point of coords) {
    if (point.length < 2 || !isLon(point[0]) || !isLat(point[1])) {
      throw new Error(`Coordenada inválida en ${geoid}`);
    }
  }
  return polygonGeoJsonToWkt(geometry);
}

async function fetchTigerService(year, kind) {
  const candidates = [
    `${TIGER_ORIGIN}/tigerWMS_ACS${year}/MapServer`,
    kind === 'tracts' ? `${TIGER_ORIGIN}/Tracts_Blocks/MapServer` : `${TIGER_ORIGIN}/Places_CouSub/MapServer`,
  ];
  const attemptedLayers = [];
  for (const base of candidates) {
    try {
      const serviceUrl = `${base}?f=pjson`;
      const serviceResponse = await fetch(serviceUrl, { headers: { 'User-Agent': USER_AGENT } });
      const serviceStatus = serviceResponse.status;
      const serviceText = await serviceResponse.text();
      let service;
      try { service = JSON.parse(serviceText); } catch { service = null; }
      attemptedLayers.push({
        serviceUrl,
        httpStatus: serviceStatus,
        featureCount: null,
        layers: service?.layers?.map((layer) => ({ id: layer.id, name: layer.name })) || [],
      });
      const wanted = kind === 'tracts' ? ['tract'] : ['place'];
      let layers = service && Array.isArray(service.layers) ? service.layers : [];
      let chosen = null;
      const exactName = kind === 'tracts' ? 'census tracts' : 'incorporated places';
      chosen = layers.find((layer) => String(layer.name || '').toLowerCase().trim() === exactName) || null;
      for (const layer of layers) {
        if (chosen) break;
        const name = String(layer.name || '').toLowerCase();
        if (wanted.some((word) => name.includes(word))) { chosen = layer; break; }
      }
      if (!chosen) {
        for (const id of kind === 'tracts' ? ['0', '8', '1'] : ['4', '8', '28']) {
          try {
            const layer = await fetchJson(`${base}/${id}?f=pjson`, `TIGERweb capa ${id}`);
            const name = String(layer.name || '').toLowerCase();
            if (kind === 'tracts' && name.includes('tract')) chosen = { ...layer, id };
            if (kind === 'place' && (name.includes('place') || id === '4')) chosen = { ...layer, id };
            if (chosen) break;
          } catch { /* probar siguiente capa */ }
        }
      }
      if (!chosen?.id && chosen) chosen.id = String(chosen.id ?? 0);
      if (!chosen) continue;
      return {
        base,
        service,
        layer: chosen,
        serviceName: base.split('/').at(-1),
        vintageMatched: base.includes(String(year)),
        attemptedLayers,
      };
    } catch { /* probar servicio siguiente */ }
  }
  throw new Error(`TIGERweb no respondió una capa válida para ${kind} en ${year}`);
}

function geometryToArcgisPolygon(geometry) {
  if (geometry.type === 'Polygon') {
    return { rings: geometry.coordinates, spatialReference: { wkid: 4326 } };
  }
  if (geometry.type === 'MultiPolygon') {
    return { rings: geometry.coordinates.flat(), spatialReference: { wkid: 4326 } };
  }
  throw new Error(`Tipo de geometría TIGERweb no soportado para consulta espacial: ${geometry?.type}`);
}

export function geometryGeometrySummary(geometry) {
  if (geometry?.type === 'Polygon') {
    return {
      type: 'Polygon',
      rings: geometry.coordinates.length,
      vertices: geometry.coordinates.reduce((sum, ring) => sum + ring.length, 0),
    };
  }
  if (geometry?.type === 'MultiPolygon') {
    return {
      type: 'MultiPolygon',
      rings: geometry.coordinates.reduce((sum, polygon) => sum + polygon.length, 0),
      vertices: geometry.coordinates.reduce(
        (sum, polygon) => sum + polygon.reduce((ringSum, ring) => ringSum + ring.length, 0),
        0
      ),
    };
  }
  return { type: geometry?.type || 'desconocido', rings: 0, vertices: 0 };
}

export function approximateOverlapMetrics(tractGeometry, placeGeometry, gridSize = 80) {
  const polygons = geometryPolygons(tractGeometry);
  const placePolygons = geometryPolygons(placeGeometry);
  if (!polygons.length || !placePolygons.length) {
    return { tractWithinPlacePct: 0, placeContributionPct: 0 };
  }
  const allCoords = polygons.flatMap((polygon) => polygon.coordinates.flat());
  const placeCoords = placePolygons.flatMap((polygon) => polygon.coordinates.flat());
  const lons = allCoords.map((point) => point[0]);
  const lats = allCoords.map((point) => point[1]);
  const minLon = Math.min(...lons);
  const maxLon = Math.max(...lons);
  const minLat = Math.min(...lats);
  const maxLat = Math.max(...lats);
  if (maxLon <= minLon || maxLat <= minLat) {
    return { tractWithinPlacePct: 0, placeContributionPct: 0 };
  }
  let insideTract = 0;
  let insideTractAndPlace = 0;
  for (let y = 0; y < gridSize; y++) {
    const lat = minLat + ((y + 0.5) / gridSize) * (maxLat - minLat);
    for (let x = 0; x < gridSize; x++) {
      const lon = minLon + ((x + 0.5) / gridSize) * (maxLon - minLon);
      if (polygons.some((polygon) => pointInPolygon(lon, lat, polygon))) {
        insideTract++;
        if (placePolygons.some((polygon) => pointInPolygon(lon, lat, polygon))) insideTractAndPlace++;
      }
    }
  }
  const placeLons = placeCoords.map((point) => point[0]);
  const placeLats = placeCoords.map((point) => point[1]);
  const placeMinLon = Math.min(...placeLons);
  const placeMaxLon = Math.max(...placeLons);
  const placeMinLat = Math.min(...placeLats);
  const placeMaxLat = Math.max(...placeLats);
  let insidePlace = 0;
  let placeOverlapWithTract = 0;
  for (let y = 0; y < gridSize; y++) {
    const lat = placeMinLat + ((y + 0.5) / gridSize) * (placeMaxLat - placeMinLat);
    for (let x = 0; x < gridSize; x++) {
      const lon = placeMinLon + ((x + 0.5) / gridSize) * (placeMaxLon - placeMinLon);
      if (placePolygons.some((polygon) => pointInPolygon(lon, lat, polygon))) {
        insidePlace++;
        if (polygons.some((polygon) => pointInPolygon(lon, lat, polygon))) placeOverlapWithTract++;
      }
    }
  }
  return {
    tractWithinPlacePct: insideTract ? Number(((insideTractAndPlace / insideTract) * 100).toFixed(2)) : 0,
    placeContributionPct: insidePlace ? Number(((placeOverlapWithTract / insidePlace) * 100).toFixed(2)) : 0,
  };
}

function hideCensusKey(url) {
  const key = process.env.CENSUS_API_KEY;
  return key ? url.replaceAll(encodeURIComponent(key), '<CENSUS_API_KEY>').replaceAll(key, '<CENSUS_API_KEY>') : url;
}

async function fetchTigerFeatures(year) {
  const place = await fetchTigerService(year, 'place');
  const placeLayer = place.layer.id ?? place.layer.layerId ?? 4;
  const fields = 'GEOID,NAME,BASENAME,CENTLAT,CENTLON,AREALAND,AREAWATER';
  const placeUrl = `${place.base}/${placeLayer}/query?where=${encodeURIComponent(`GEOID='${PLACE_GEOID}'`)}&outFields=${fields}&returnGeometry=true&outSR=4326&f=geojson`;
  const placeResponse = await fetch(placeUrl, { headers: { 'User-Agent': USER_AGENT } });
  const placeText = await placeResponse.text();
  let placeData;
  try { placeData = JSON.parse(placeText); } catch {
    throw new Error(`TIGERweb place respondió contenido que no es JSON (HTTP ${placeResponse.status})`);
  }
  if (!placeData.features?.length) {
    throw new Error(`TIGERweb no devolvió el límite oficial place ${PLACE_GEOID}`);
  }
  const placeFeature = placeData.features.find((feature) => feature.properties?.GEOID === PLACE_GEOID) || placeData.features[0];
  if (!placeFeature?.geometry || placeFeature.properties?.GEOID !== PLACE_GEOID) {
    throw new Error(`TIGERweb place no identificó el GEOID oficial ${PLACE_GEOID}`);
  }

  const tracts = await fetchTigerService(year, 'tracts');
  const tractLayer = tracts.layer.id ?? tracts.layer.layerId ?? 8;
  const placeArcgisGeometry = geometryToArcgisPolygon(placeFeature.geometry);
  const tractUrl = `${tracts.base}/${tractLayer}/query?where=${encodeURIComponent(`STATE='${STATE}' AND COUNTY='${COUNTY}'`)}`
    + `&geometry=${encodeURIComponent(JSON.stringify(placeArcgisGeometry))}`
    + `&geometryType=esriGeometryPolygon&inSR=4326&spatialRel=esriSpatialRelIntersects`
    + `&outFields=${fields}&returnGeometry=true&outSR=4326&f=geojson`;
  const tractResponse = await fetch(tractUrl, { headers: { 'User-Agent': USER_AGENT } });
  const tractText = await tractResponse.text();
  let tractData;
  try { tractData = JSON.parse(tractText); } catch {
    tractData = { error: { message: tractText.slice(0, 180) }, features: [] };
  }
  if (tractData.error) {
    throw new Error(`TIGERweb tracts devolvió un error ArcGIS/Census: ${tractData.error.message || 'desconocido'} (HTTP ${tractResponse.status})`);
  }
  return {
    tracts,
    place,
    tractData,
    placeData,
    tractQueryUrl: tractUrl,
    placeQueryUrl: placeUrl,
    tractHttpStatus: tractResponse.status,
    placeHttpStatus: placeResponse.status,
    tractFeatureCount: tractData.features?.length ?? 0,
    placeFeatureCount: placeData.features?.length ?? 0,
    tractFileHash: computeTextHash(tractText),
    placeFileHash: computeTextHash(placeText),
  };
}

async function fetchAcs(year) {
  const variables = [
    'NAME', 'B01003_001E', 'B01003_001M',
    'B11001_001E', 'B11001_001M',
    'B19013_001E', 'B19013_001M',
    'B01002_001E', 'B01002_001M',
  ].join(',');
  const key = process.env.CENSUS_API_KEY;
  const keyParam = key ? '&key=' : '';
  if (!key) {
    return {
      tracts: [],
      place: null,
      error: 'CENSUS_API_KEY no está definida en el entorno',
      tractUrl: `${ACS_ORIGIN}/${year}/acs/acs5?get=${variables}&for=tract:*&in=state:${STATE}+county:${COUNTY}`,
      placeUrl: `${ACS_ORIGIN}/${year}/acs/acs5?for=place:${PLACE}&in=state:${STATE}`,
      tractFileHash: null,
      placeFileHash: null,
      tractHttpStatus: null,
      placeHttpStatus: null,
    };
  }

  async function fetchRows(url, label) {
    const response = await fetch(url, { headers: { 'User-Agent': USER_AGENT } });
    if (!response.ok) {
      const text = await response.text();
      throw new Error(`${label} respondió HTTP ${response.status}: ${text.slice(0, 180)}`);
    }
    const text = await response.text();
    let data;
    try {
      data = JSON.parse(text);
    } catch {
      throw new Error(`${label} respondió contenido que no es JSON (posible página "Missing Key" o bloqueo HTML)`);
    }
    if (!Array.isArray(data) || data.length < 2) throw new Error(`${label} devolvió una respuesta vacía`);
    const [headers, ...rows] = data;
    return {
      rows: rows.map((row) => Object.fromEntries(headers.map((header, index) => [header, row[index]]))),
      fileHash: computeTextHash(text),
      httpStatus: response.status,
    };
  }

  const tractUrl = `${ACS_ORIGIN}/${year}/acs/acs5?get=${variables}&for=tract:*&in=state:${STATE}+county:${COUNTY}${keyParam}${keyParam ? encodeURIComponent(key) : ''}`;
  const placeUrl = `${ACS_ORIGIN}/${year}/acs/acs5?get=${variables}&for=place:${PLACE}&in=state:${STATE}${keyParam}${keyParam ? encodeURIComponent(key) : ''}`;
  const [tractResult, placeResult] = await Promise.all([
    fetchRows(tractUrl, `Census ACS 5-Year ${year} tracts`),
    fetchRows(placeUrl, `Census ACS 5-Year ${year} place`),
  ]);
  return {
    tracts: tractResult.rows,
    place: placeResult.rows[0] || null,
    tractUrl: hideCensusKey(tractUrl),
    placeUrl: hideCensusKey(placeUrl),
    tractFileHash: tractResult.fileHash,
    placeFileHash: placeResult.fileHash,
    tractHttpStatus: tractResult.httpStatus,
    placeHttpStatus: placeResult.httpStatus,
  };
}

function buildBatch(tiger, acs, year) {
  const tractFeatures = tiger.tractData.features || [];
  const placeFeatures = tiger.placeData.features || [];
  if (!placeFeatures.length) throw new Error('TIGERweb no devolvió el límite oficial de Somerton place 0468080');

  const placeFeature = placeFeatures.find((feature) => feature.properties?.GEOID === PLACE_GEOID) || placeFeatures[0];
  if (placeFeature.properties?.GEOID !== PLACE_GEOID) {
    throw new Error(`El place TIGERweb devolvió GEOID ${placeFeature.properties?.GEOID}, se esperaba ${PLACE_GEOID}`);
  }

  const acsTracts = new Map(acs.tracts.map((row) => [`${row.state}${row.county}${row.tract}`, row]));
  const discoveredTracts = tractFeatures
    .filter((feature) => feature.properties?.GEOID && feature.geometry)
    .map((feature) => {
      const geoid = feature.properties.GEOID;
      const overlap = approximateOverlapMetrics(feature.geometry, placeFeature.geometry);
      const percentInPlace = overlap.tractWithinPlacePct;
      return {
        geoid,
        name: feature.properties.NAME || `Census Tract ${feature.properties.BASENAME || geoid}`,
        percentInPlace,
        placeContributionPct: overlap.placeContributionPct,
        almostOutside: percentInPlace < 10,
        hasAcs: acsTracts.has(geoid),
      };
    });
  const discoveredGeoids = discoveredTracts.map((tract) => tract.geoid);
  const acsGeoids = [...acsTracts.keys()];

  const zones = [];
  for (const tract of discoveredTracts) {
    const feature = tractFeatures.find((item) => item.properties?.GEOID === tract.geoid);
    const wkt = validateGeometry(feature);
    zones.push({
      code: tract.geoid,
      name: tract.name,
      level: 'tract',
      geometry: feature.geometry,
      geometrySummary: geometryGeometrySummary(feature.geometry),
      wkt,
      areaLandM2: cleanAcsNumber(feature.properties?.AREALAND),
    });
  }
  const placeWkt = validateGeometry(placeFeature);
  zones.push({
    code: PLACE_GEOID,
    name: placeFeature.properties.NAME || OFFICIAL_ZONE_NAMES[PLACE_GEOID],
    level: 'place',
    geometry: placeFeature.geometry,
    geometrySummary: geometryGeometrySummary(placeFeature.geometry),
    wkt: placeWkt,
    areaLandM2: cleanAcsNumber(placeFeature.properties?.AREALAND),
  });

  const indicatorCodes = {
    population: ['B01003_001E', 'B01003_001M'],
    households: ['B11001_001E', 'B11001_001M'],
    median_income: ['B19013_001E', 'B19013_001M'],
    median_age: ['B01002_001E', 'B01002_001M'],
  };
  for (const zone of zones) {
    const sourceRow = zone.level === 'tract' ? acsTracts.get(zone.code) : acs.place;
    if (!sourceRow) {
      zone.indicators = [];
      zone.acsMissing = true;
      zone.indicators.push({
        code: 'population_density',
        value: null,
        marginOfError: null,
      });
      continue;
    }
    zone.indicators = Object.entries(indicatorCodes).map(([code, [valueKey, moeKey]]) => ({
      code,
      value: cleanAcsNumber(sourceRow[valueKey]),
      marginOfError: cleanAcsNumber(sourceRow[moeKey]),
    }));
    zone.acsNullFields = zone.indicators.flatMap((indicator) => {
      const fields = [];
      const pair = indicatorCodes[indicator.code];
      const rawValue = sourceRow[pair[0]];
      const rawMoe = sourceRow[pair[1]];
      if (cleanAcsNumber(rawValue) === null) fields.push(`${indicator.code}.value`);
      if (cleanAcsNumber(rawMoe) === null) fields.push(`${indicator.code}.marginOfError`);
      return fields;
    });
    const population = zone.indicators.find((item) => item.code === 'population')?.value;
    zone.indicators.push({
      code: 'population_density',
      value: calculatePopulationDensity(population, zone.areaLandM2),
      marginOfError: null,
    });
  }

  const placeZone = zones.find((zone) => zone.level === 'place');
  const tractZones = zones.filter((zone) => zone.level === 'tract');
  const tractPopulations = tractZones.map((zone) => {
    const population = zone.indicators?.find((item) => item.code === 'population')?.value;
    return population === null || population === undefined ? null : population;
  });
  const tractPopulationSum = tractPopulations.length === 5 && tractPopulations.every((population) => population !== null)
    ? tractPopulations.reduce((sum, population) => population === null ? sum : sum + population, 0)
    : null;
  const placePopulation = placeZone?.indicators?.find((item) => item.code === 'population')?.value ?? null;
  const populationDifference = placePopulation === null
    ? null
    : tractPopulationSum - placePopulation;
  const placeContributionSum = discoveredTracts.reduce(
    (sum, tract) => sum + Number(tract.placeContributionPct || 0),
    0
  );

  return {
    year,
    tiger: {
      tractService: `${tiger.tracts.base}/${tiger.tracts.layer.id ?? 0}`,
      placeService: `${tiger.place.base}/${tiger.place.layer.id ?? 4}`,
      tractLayer: tiger.tracts.layer.name || String(tiger.tracts.layer.id ?? 0),
      placeLayer: tiger.place.layer.name || String(tiger.place.layer.id ?? 4),
      tractVintageMatched: tiger.tracts.vintageMatched,
      placeVintageMatched: tiger.place.vintageMatched,
      tractQueryUrl: tiger.tractQueryUrl,
      placeQueryUrl: tiger.placeQueryUrl,
      tractHttpStatus: tiger.tractHttpStatus,
      placeHttpStatus: tiger.placeHttpStatus,
      tractFeatureCount: tiger.tractFeatureCount,
      placeFeatureCount: tiger.placeFeatureCount,
      tractFileHash: tiger.tractFileHash,
      placeFileHash: tiger.placeFileHash,
      attemptedLayers: [...(tiger.tracts.attemptedLayers || []), ...(tiger.place.attemptedLayers || [])],
    },
    zones,
    discovery: {
      place: {
        geoid: placeFeature.properties.GEOID,
        name: placeFeature.properties.NAME,
        areaLandM2: cleanAcsNumber(placeFeature.properties.AREALAND),
      },
      placeContributionSumPct: Number(placeContributionSum.toFixed(2)),
      placeContributionDifferencePct: Number((placeContributionSum - 100).toFixed(2)),
      tractPopulationSum,
      placePopulation,
      populationDifference,
      tracts: discoveredTracts,
      expectedGeoids: [...EXPECTED_TRACTS],
      foundGeoids: discoveredGeoids,
      missingExpected: EXPECTED_TRACTS.filter((geoid) => !discoveredGeoids.includes(geoid)),
      additionalFromDiscovery: discoveredGeoids.filter((geoid) => !EXPECTED_TRACTS.includes(geoid)),
      acsGeoids,
      discoveredWithoutAcs: discoveredGeoids.filter((geoid) => !acsTracts.has(geoid)),
      acsWithoutDiscovery: acsGeoids.filter((geoid) => !discoveredGeoids.includes(geoid)),
    },
    coverage: compareExpectedCoverage(discoveredGeoids),
    acs: {
      origin: `${ACS_ORIGIN}/${year}/acs/acs5`,
      variableNames: Object.keys(indicatorCodes),
      tractUrl: acs.tractUrl || `${ACS_ORIGIN}/${year}/acs/acs5`,
      placeUrl: acs.placeUrl || null,
      error: acs.error || null,
      tractFeatureCount: acs.tracts?.length || 0,
      tractHttpStatus: acs.tractHttpStatus ?? null,
      placeHttpStatus: acs.placeHttpStatus ?? null,
      tractFileHash: acs.tractFileHash ?? null,
      placeFileHash: acs.placeFileHash ?? null,
    },
  };
}

async function testSpatialValidity(conn, batch) {
  const results = [];
  for (const zone of batch.zones) {
    if (!conn) {
      results.push({ geoid: zone.code, valid: null, reason: 'MySQL no disponible; validación local completada' });
      continue;
    }
    const [[row]] = await conn.execute(
      "SELECT ST_IsValid(ST_GeomFromText(?, 4326, 'axis-order=long-lat')) AS valid",
      [zone.wkt]
    );
    results.push({ geoid: zone.code, valid: row.valid === 1 || row.valid === true, reason: null });
  }
  return results;
}

async function compareDatabase(conn, batch) {
  if (!conn) return null;
  const codes = batch.zones.map((zone) => zone.code);
  const placeholders = codes.map(() => '?').join(',');
  const [zones] = await conn.execute(
    `SELECT z.code, z.name, z.level, s.name AS source_name, s.reference_period
     FROM zones z LEFT JOIN data_sources s ON s.id = z.source_id
     WHERE z.code IN (${placeholders})`,
    codes
  );
  const [values] = await conn.execute(
    `SELECT z.code, i.code AS indicator, iv.value, iv.margin_of_error, iv.reference_period
     FROM indicator_values iv
     JOIN zones z ON z.id = iv.zone_id
     JOIN indicators i ON i.id = iv.indicator_id
     WHERE z.code IN (${placeholders})
     ORDER BY z.code, i.code`,
    codes
  );
  const staleCodes = ['04027011502', '04027011403'];
  const stalePlaceholders = staleCodes.map(() => '?').join(',');
  const [staleZones] = await conn.execute(
    `SELECT z.id, z.code,
            (SELECT COUNT(*) FROM watchlist_items w WHERE w.zone_id = z.id) AS watchlist_count,
            (SELECT COUNT(*) FROM properties p WHERE p.zone_id = z.id) AS property_count,
            (SELECT COUNT(*) FROM indicator_values iv WHERE iv.zone_id = z.id) AS indicator_value_count
     FROM zones z
     WHERE z.code IN (${stalePlaceholders})`,
    staleCodes
  );
  return { zones, values, staleZones };
}

async function compareRealOverlap(conn, batch) {
  if (!conn) return null;
  const place = batch.zones.find((zone) => zone.level === 'place');
  if (!place) return null;
  let intersectionArea = 0;
  const tracts = [];
  for (const zone of batch.zones.filter((item) => item.level === 'tract')) {
    const [[row]] = await conn.execute(
      `SELECT ST_AsText(ST_Intersection(
         ST_GeomFromText(?, 4326, 'axis-order=long-lat'),
         ST_GeomFromText(?, 4326, 'axis-order=long-lat')
       ), 'axis-order=long-lat') AS intersection_wkt`,
      [place.wkt, zone.wkt]
    );
    let area = 0;
    if (row.intersection_wkt) {
      const [[meta]] = await conn.execute(
        `SELECT ST_GeometryType(ST_GeomFromText(?, 4326, 'axis-order=long-lat')) AS geometry_type,
                ST_NumGeometries(ST_GeomFromText(?, 4326, 'axis-order=long-lat')) AS geometry_count`,
        [row.intersection_wkt, row.intersection_wkt]
      );
      const count = Number(meta.geometry_count || (meta.geometry_type === 'POLYGON' ? 1 : 0));
      for (let index = 1; index <= count; index++) {
        const [[component]] = await conn.execute(
          `SELECT ST_GeometryType(ST_GeometryN(ST_GeomFromText(?, 4326, 'axis-order=long-lat'), ?)) AS component_type,
                  CASE
                    WHEN ST_GeometryType(ST_GeometryN(ST_GeomFromText(?, 4326, 'axis-order=long-lat'), ?)) IN ('POLYGON','MULTIPOLYGON')
                    THEN ST_Area(ST_GeometryN(ST_GeomFromText(?, 4326, 'axis-order=long-lat'), ?))
                    ELSE 0
                  END AS component_area`,
          [
            row.intersection_wkt, index,
            row.intersection_wkt, index,
            row.intersection_wkt, index,
          ]
        );
        if (component.component_type === 'POLYGON' || component.component_type === 'MULTIPOLYGON') {
          area += Number(component.component_area || 0);
        }
      }
    }
    intersectionArea += area;
    tracts.push({ code: zone.code, intersectionArea: area });
  }
  const [[placeRow]] = await conn.execute(
    `SELECT ST_Area(ST_GeomFromText(?, 4326, 'axis-order=long-lat')) AS spherical_area`,
    [place.wkt]
  );
  const sphericalArea = Number(placeRow.spherical_area || 0);
  const differenceVsSphericalPct = sphericalArea > 0
    ? Number((((sphericalArea - intersectionArea) / sphericalArea) * 100).toFixed(4))
    : null;
  const differenceVsArealandPct = place.areaLandM2 > 0
    ? Number((((place.areaLandM2 - intersectionArea) / place.areaLandM2) * 100).toFixed(4))
    : null;
  return { tracts, intersectionArea, sphericalArea, differenceVsSphericalPct, differenceVsArealandPct };
}

function printReport(batch, hash, spatial, current, dbAvailable, realOverlap) {
  console.log('=== TerraScope: importación oficial de Somerton ===');
  console.log(`Modo: ${batch.dryRun ? 'DRY-RUN (solo lectura)' : 'REAL'}`);
  console.log(`Año ACS solicitado: ${batch.year}`);
  console.log(`Servicio TIGERweb: ${batch.tiger.tractService}`);
  console.log(`Capa TIGERweb de tracts: ${batch.tiger.tractLayer}`);
  console.log(`Servicio TIGERweb place: ${batch.tiger.placeService}`);
  console.log(`Capa TIGERweb place: ${batch.tiger.placeLayer}`);
  console.log(`Vintage coincidente ACS/TIGER: tracts=${batch.tiger.tractVintageMatched ? 'sí' : 'no'}, place=${batch.tiger.placeVintageMatched ? 'sí' : 'no'}`);
  console.log(`HTTP tracts: ${batch.tiger.tractHttpStatus}, features=${batch.tiger.tractFeatureCount}`);
  console.log(`HTTP place: ${batch.tiger.placeHttpStatus}, features=${batch.tiger.placeFeatureCount}`);
  console.log(`Where tracts: STATE='04' AND COUNTY='027'`);
  console.log(`Where place: GEOID='0468080'`);
  console.log(`Filtro espacial place: esriSpatialRelIntersects, inSR/outSR=4326`);
  const placeZone = batch.zones.find((zone) => zone.level === 'place');
  console.log(`Vértices del filtro place: ${placeZone?.geometrySummary?.vertices ?? 0}`);
  console.log(`Fuente ACS: ${batch.acs.origin}`);
  console.log(`ACS HTTP/features: tracts=${batch.acs.tractHttpStatus ?? 'n/a'}/${batch.acs.tractFeatureCount}, place=${batch.acs.placeHttpStatus ?? 'n/a'}/1`);
  console.log(`ACS clave: ${process.env.CENSUS_API_KEY ? 'definida en variable de entorno (oculta)' : 'no definida'}`);
  if (batch.acs.error) console.log(`ACS advertencia: ${batch.acs.error}`);
  console.log(`Hash SHA-256 del lote completo: ${hash}`);
  console.log('\nZonas en el lote:');
  for (const zone of batch.zones) {
    console.log(`- ${zone.code} | ${zone.name} | ${zone.level} | areaLand=${zone.areaLandM2 ?? 'null'} m²`);
  }
  console.log('\nPlace oficial descubierto:');
  console.log(`- ${batch.discovery.place.geoid} | ${batch.discovery.place.name} | AREALAND=${batch.discovery.place.areaLandM2 ?? 'null'} m²`);
  console.log('\nTracts descubiertos que intersectan el place:');
  for (const tract of batch.discovery.tracts) {
    console.log(`- ${tract.geoid} | ${tract.name} | tract dentro del place=${tract.percentInPlace}% | aportación al place=${tract.placeContributionPct}% | ACS=${tract.hasAcs ? 'sí' : 'no'} | casi fuera=${tract.almostOutside ? 'sí' : 'no'}`);
  }
  console.log(`Suma de aportación al place (b): ${batch.discovery.placeContributionSumPct}%`);
  console.log(`Diferencia contra el área del place (100% - suma): ${Number((100 - batch.discovery.placeContributionSumPct).toFixed(2))}%`);
  if (realOverlap) {
    console.log('\nComparación real de áreas por ST_Intersection / ST_Area (SRID 4326):');
    console.log(`- Suma de áreas de intersección tract/place: ${Number(realOverlap.intersectionArea.toFixed(2))} m²`);
    console.log(`- ST_Area(place): ${Number(realOverlap.sphericalArea.toFixed(2))} m²`);
    console.log(`- AREALAND(place): ${batch.discovery.place.areaLandM2 ?? 'null'} m²`);
    console.log(`- Diferencia contra ST_Area(place): ${realOverlap.differenceVsSphericalPct ?? 'null'}%`);
    console.log(`- Diferencia contra AREALAND(place): ${realOverlap.differenceVsArealandPct ?? 'null'}%`);
  } else {
    console.log('\nComparación real de áreas: no disponible sin conexión MySQL; dry-run continúa.');
  }
  console.log('\nComparación con los GEOID previamente suministrados (solo referencia, no fuente de verdad):');
  console.log(`- Esperados: ${batch.discovery.expectedGeoids.join(', ')}`);
  console.log(`- Encontrados: ${batch.discovery.foundGeoids.length ? batch.discovery.foundGeoids.join(', ') : 'ninguno'}`);
  console.log(`- Faltantes respecto a la referencia anterior: ${batch.discovery.missingExpected.length ? batch.discovery.missingExpected.join(', ') : 'ninguno'}`);
  console.log(`- Adicionales descubiertos: ${batch.discovery.additionalFromDiscovery.length ? batch.discovery.additionalFromDiscovery.join(', ') : 'ninguno'}`);
  console.log('\nCruce ACS:');
  console.log(`- Total GEOID ACS de Yuma County: ${batch.discovery.acsGeoids.length}`);
  console.log(`- Descubiertos sin ACS: ${batch.discovery.discoveredWithoutAcs.length ? batch.discovery.discoveredWithoutAcs.join(', ') : 'ninguno'}`);
  console.log(`- ACS sin descubrimiento TIGER: ${batch.discovery.acsWithoutDiscovery.length ? batch.discovery.acsWithoutDiscovery.join(', ') : 'ninguno'}`);
  console.log('\nTabla ACS por zona (valor | margen de error):');
  const acsLabels = {
    population: 'Población',
    households: 'Hogares',
    median_income: 'Ingreso mediano',
    median_age: 'Edad mediana',
  };
  for (const zone of batch.zones) {
    if (zone.acsMissing || !zone.indicators?.length) {
      console.log(`- ${zone.code} | sin fila ACS: población=null; hogares=null; ingreso mediano=null; edad mediana=null`);
      continue;
    }
    const values = zone.indicators.map((indicator) =>
      `${acsLabels[indicator.code]}=${indicator.value ?? 'null'} (MOE=${indicator.marginOfError ?? 'null'})`
    );
    const nullFields = zone.acsNullFields?.length ? zone.acsNullFields.join(', ') : 'ninguno';
    console.log(`- ${zone.code} | ${values.join('; ')} | nulls=${nullFields}`);
  }
  console.log('\nCoherencia de población ACS:');
  console.log(`- Suma de poblaciones de los 5 tracts: ${batch.discovery.tractPopulationSum ?? 'null'}`);
  console.log(`- Población ACS del place: ${batch.discovery.placePopulation ?? 'null'}`);
  console.log(`- Diferencia tract(s) menos place: ${batch.discovery.populationDifference ?? 'null'} personas`);
  console.log('- Los tracts pueden incluir población fuera de los límites de la ciudad; la diferencia no se ajusta ni se fuerza.');
  console.log('\nGeometría TIGERweb:');
  for (const zone of batch.zones) {
    console.log(`- ${zone.code}: ${zone.geometrySummary.type}, ${zone.geometrySummary.rings} anillo(s), ${zone.geometrySummary.vertices} vértices`);
  }
  console.log('\nAdvertencias:');
  const almostOutside = batch.discovery.tracts.filter((tract) => tract.almostOutside);
  if (almostOutside.length) console.log(`- Tracts casi fuera (<10% dentro): ${almostOutside.map((tract) => tract.geoid).join(', ')}`);
  else console.log('- Ningún tract queda casi fuera de Somerton (<10% dentro).');
  console.log('- La lista final para importación debe aprobarse explícitamente; este dry-run no escribe MySQL ni crea archivos.');
  console.log('\nST_IsValid SRID 4326 (axis-order=long-lat):');
  for (const item of spatial) {
    console.log(`- ${item.geoid}: ${item.valid === null ? `no evaluado - ${item.reason}` : item.valid ? '1 (válida)' : '0 (inválida)'}`);
  }
  console.log(`\nMySQL: ${dbAvailable ? 'disponible para comparación de solo lectura' : 'no disponible; dry-run continúa sin comparación'}`);
  if (current) {
    console.log('Comparación con la base actual:');
    console.log(`- Zonas existentes coincidentes: ${current.zones.length}`);
    console.log(`- Valores actuales de indicadores coincidentes: ${current.values.length}`);
    console.log('- Zonas falsas/no pertenecientes al lote descubierto:');
    if (current.staleZones?.length) {
      for (const zone of current.staleZones) {
        console.log(`  - id=${zone.id}, GEOID=${zone.code}, watchlist_items=${Number(zone.watchlist_count)}, properties=${Number(zone.property_count)}, indicator_values_actuales_potencialmente_a_borrar=${Number(zone.indicator_value_count)} (no eliminados)`);
      }
    } else {
      console.log('  - ninguna encontrada');
    }
  }
  console.log('\nIndicadores derivados propuestos (no se escriben ni migran):');
  for (const tract of batch.discovery.tracts) {
    const zone = batch.zones.find((item) => item.code === tract.geoid);
    const population = zone?.indicators?.find((item) => item.code === 'population')?.value;
    const density = population !== null && population !== undefined && zone?.areaLandM2 > 0
      ? Number((population / (zone.areaLandM2 / 1_000_000)).toFixed(2))
      : null;
    console.log(`- ${tract.geoid}: population_density=${density ?? 'null'} hab/km²; margin_of_error=null`);
  }
  console.log('\nHash por archivo descargado:');
  console.log(`- TIGER tracts GeoJSON: ${batch.tiger.tractFileHash || 'no disponible'}`);
  console.log(`- TIGER place GeoJSON: ${batch.tiger.placeFileHash || 'no disponible'}`);
  console.log(`- ACS tracts JSON: ${batch.acs.tractFileHash || 'no disponible'}`);
  console.log(`- ACS place JSON: ${batch.acs.placeFileHash || 'no disponible'}`);
}

async function openDbIfPossible() {
  try {
    const conn = await mysql.createConnection({
      host: process.env.DB_HOST || '127.0.0.1',
      port: Number(process.env.DB_PORT || 3306),
      user: process.env.DB_USER || 'root',
      password: process.env.DB_PASSWORD || '',
      database: process.env.DB_NAME || 'terrascope',
      timezone: 'Z',
      connectTimeout: 3000,
    });
    await conn.query("SET time_zone = '+00:00'");
    return conn;
  } catch (err) {
    return null;
  }
}

async function confirm(message) {
  const rl = createInterface({ input, output });
  try {
    const answer = await rl.question(`${message} (escribe IMPORTAR para continuar): `);
    return answer.trim() === 'IMPORTAR';
  } finally {
    rl.close();
  }
}

async function saveRealBackups(batch, hash, raw) {
  const backendDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
  const folder = path.join(backendDir, 'data', 'raw', String(batch.year));
  await fs.mkdir(folder, { recursive: true });
  const files = {
    'tracts-tiger.geojson': JSON.stringify(raw.tractData, null, 2),
    'place-tiger.geojson': JSON.stringify(raw.placeData, null, 2),
    'acs-tracts.json': JSON.stringify(raw.acsTracts, null, 2),
    'acs-place.json': JSON.stringify(raw.acsPlace, null, 2),
  };
  const fileHashes = {};
  for (const [name, text] of Object.entries(files)) {
    await fs.writeFile(path.join(folder, name), text);
    fileHashes[name] = computeTextHash(text);
  }
  await fs.writeFile(path.join(folder, 'manifest.json'), JSON.stringify({
    year: batch.year,
    retrievedAt: new Date().toISOString(),
    hash,
    fileHashes,
    batchHashes: batch.fileHashes || {},
    urls: {
      tigerTractsService: batch.tiger.tractService,
      tigerPlaceService: batch.tiger.placeService,
      acs: batch.acs.origin,
      keyIncluded: false,
    },
  }, null, 2));
  return folder;
}

async function importReal(conn, batch) {
  await conn.beginTransaction();
  try {
    const upsertSource = async (name, organization, url, license, referencePeriod, dataType, notes) => {
      await conn.execute(
        `INSERT INTO data_sources (name, organization, url, license, reference_period, retrieved_at, data_type, notes)
         VALUES (?, ?, ?, ?, ?, NOW(), ?, ?)
         ON DUPLICATE KEY UPDATE organization = VALUES(organization), url = VALUES(url),
           license = VALUES(license), reference_period = VALUES(reference_period),
           retrieved_at = VALUES(retrieved_at), data_type = VALUES(data_type), notes = VALUES(notes)`,
        [name, organization, url, license, referencePeriod, dataType, notes]
      );
      const [[row]] = await conn.execute('SELECT id FROM data_sources WHERE name = ? LIMIT 1', [name]);
      return row.id;
    };

    const tigerSourceId = await upsertSource(
      'U.S. Census Bureau - TIGERweb 2024',
      'U.S. Census Bureau',
      batch.tiger.tractService.replace(/\/query\b.*$/i, ''),
      'U.S. Public Domain',
      TIGER_REFERENCE_PERIOD,
      'official',
      'Límites oficiales TIGERweb usados para zones; sin clave'
    );
    const acsSourceId = await upsertSource(
      'American Community Survey (ACS) 5-Year Estimates',
      'U.S. Census Bureau',
      `${ACS_ORIGIN}/${batch.year}/acs/acs5`,
      'U.S. Public Domain',
      ACS_REFERENCE_PERIOD,
      'estimate',
      'Estimaciones ACS 5-Year; sin clave API en el registro'
    );
    const derivedSourceId = await upsertSource(
      'TerraScope - indicadores derivados 2024',
      'TerraScope',
      `${TIGER_ORIGIN}/${batch.year}/...`,
      'Derivado de datos públicos U.S. Census',
      ACS_REFERENCE_PERIOD,
      'derived',
      'Cálculo population_density derivado de TIGERweb y ACS'
    );

    for (const staleCode of ['04027011502', '04027011403']) {
      const [[stale]] = await conn.execute(
        `SELECT z.id,
          (SELECT COUNT(*) FROM watchlist_items w WHERE w.zone_id = z.id) AS watchlist_count,
          (SELECT COUNT(*) FROM properties p WHERE p.zone_id = z.id) AS property_count
         FROM zones z WHERE z.code = ? LIMIT 1`,
        [staleCode]
      );
      if (stale && (Number(stale.watchlist_count) > 0 || Number(stale.property_count) > 0)) {
        throw new Error(`Zona falsa ${staleCode} tiene dependencias nuevas; no se elimina ni se modifica MySQL.`);
      }
      if (stale) await conn.execute('DELETE FROM zones WHERE id = ?', [stale.id]);
    }

    const zoneIds = [];
    for (const zone of batch.zones) {
      await conn.execute(
        `INSERT INTO zones (code, name, level, boundary, source_id)
         VALUES (?, ?, ?, ST_GeomFromText(?, 4326, 'axis-order=long-lat'), ?)
         ON DUPLICATE KEY UPDATE name = VALUES(name), level = VALUES(level), boundary = VALUES(boundary), source_id = VALUES(source_id)`,
        [zone.code, zone.name, zone.level, zone.wkt, tigerSourceId]
      );
      const [[existingZone]] = await conn.execute('SELECT id FROM zones WHERE code = ?', [zone.code]);
      zoneIds.push(existingZone.id);
    }

    const placeholders = zoneIds.map(() => '?').join(',');
    await conn.execute(`DELETE FROM indicator_values WHERE zone_id IN (${placeholders})`, zoneIds);

    const [indicatorRows] = await conn.execute('SELECT id, code FROM indicators');
    const indicators = new Map(indicatorRows.map((row) => [row.code, row.id]));
    for (let i = 0; i < batch.zones.length; i++) {
      const zone = batch.zones[i];
      for (const indicator of zone.indicators) {
        const indicatorId = indicators.get(indicator.code);
        if (!indicatorId) throw new Error(`Indicador requerido no existe en la base: ${indicator.code}.`);
        if (indicator.value === null || indicator.value === undefined) continue;
        const sourceId = indicator.code === 'population_density'
          ? derivedSourceId
          : acsSourceId;
        const period = indicator.code === 'population_density'
          ? ACS_REFERENCE_PERIOD
          : ACS_REFERENCE_PERIOD;
        await conn.execute(
          `INSERT INTO indicator_values
             (zone_id, indicator_id, value, margin_of_error, source_id, reference_period)
           VALUES (?, ?, ?, ?, ?, ?)`,
          [zoneIds[i], indicatorId, indicator.value, indicator.marginOfError, sourceId, period]
        );
      }
    }

    async function verifyPostImport(conn, batch) {
      const placeholders = batch.zones.map(() => '?').join(',');
      const codes = batch.zones.map((zone) => zone.code);
      const [zones] = await conn.execute(
        `SELECT z.id, z.code, z.name, z.level,
                ST_IsValid(z.boundary) AS valid,
                ST_GeometryType(z.boundary) AS geometry_type,
                ST_AsGeoJSON(z.boundary) AS boundary_geojson
         FROM zones z WHERE z.code IN (${placeholders})`,
        codes
      );
      if (zones.length !== batch.zones.length) {
        throw new Error(`Verificación posterior falló: se esperaban ${batch.zones.length} zonas y se encontraron ${zones.length}.`);
      }
      const allowedNames = new Map(batch.zones.map((zone) => [zone.code, zone.name]));
      for (const zone of zones) {
        if (allowedNames.get(zone.code) !== zone.name) {
          throw new Error(`Nombre no coincide para ${zone.code}: esperado="${allowedNames.get(zone.code)}", actual="${zone.name}".`);
        }
        if (zone.valid !== 1 && zone.valid !== true) throw new Error(`Boundary inválida para ${zone.code}.`);
        const geojson = typeof zone.boundary_geojson === 'string'
          ? JSON.parse(zone.boundary_geojson)
          : zone.boundary_geojson;
        if (!geojson || !['Polygon', 'MultiPolygon'].includes(geojson.type)) {
          throw new Error(`ST_AsGeoJSON devolvió una geometría no válida para Android en ${zone.code}.`);
        }
      }
      const valuesPlaceholders = codes.map(() => '?').join(',');
      const [values] = await conn.execute(
        `SELECT z.code, i.code, iv.value, iv.margin_of_error, iv.reference_period
         FROM indicator_values iv
         JOIN zones z ON z.id = iv.zone_id
         JOIN indicators i ON i.id = iv.indicator_id
         WHERE z.code IN (${valuesPlaceholders})
         ORDER BY z.code, i.code`,
        codes
      );
      for (const row of values) {
        if (!Object.prototype.hasOwnProperty.call(row, 'code')) {
          throw new Error('Verificación del contrato Android falló: falta code de indicador.');
        }
        if (row.value !== null && !Number.isFinite(Number(row.value))) {
          throw new Error(`Verificación Android falló para valor de ${row.code}.`);
        }
      }
      return { zones: zones.length, values: values.length };
    }
    const zonePlaceholders = batch.zones.map(() => '?').join(',');
    const [invented] = await conn.execute(
      `SELECT code, name FROM zones WHERE code IN (${zonePlaceholders}) AND
       name NOT REGEXP '^Census Tract [0-9]+([.][0-9]+)?$' AND name <> 'Somerton city'`,
      batch.zones.map((zone) => zone.code)
    );
    if (invented.length) throw new Error(`Nombres inventados encontrados después de importar: ${invented.map((r) => r.name).join(', ')}`);
    const [knownSynthetic] = await conn.execute(
      `SELECT code, name FROM zones
       WHERE name LIKE '%Oportunidad%' OR name LIKE '%Agrícola%'
          OR name LIKE '%Somerton Norte%' OR name LIKE '%Somerton Sur%'
          OR name LIKE '%Valle de Somerton%' OR name LIKE '%La Villa%'`
    );
    if (knownSynthetic.length) {
      throw new Error(`Aún existen nombres inventados en la base: ${knownSynthetic.map((r) => r.name).join(', ')}`);
    }
    await conn.execute(
      `DELETE ds FROM data_sources ds
       WHERE ds.id NOT IN (SELECT source_id FROM zones WHERE source_id IS NOT NULL)
         AND ds.id NOT IN (SELECT source_id FROM indicator_values)
         AND (
           ds.reference_period LIKE '%TIGERweb 2023%'
           OR ds.reference_period LIKE 'ACS 2019-2023'
           OR ds.reference_period LIKE 'ACS 5-yr 2023'
           OR ds.reference_period LIKE '%2019-2023%'
         )`
    );
    await conn.commit();
    return { zones: zoneIds.length, deletedIndicatorValues: true };
  } catch (err) {
    await conn.rollback();
    throw err;
  }
}

async function main() {
  const options = parseArgs(process.argv.slice(2));
  console.log(`Descargando lote oficial para el año ${options.year}...`);
  const tiger = await fetchTigerFeatures(options.year);
  const acs = await fetchAcs(options.year);
  const batch = buildBatch(tiger, acs, options.year);
  batch.dryRun = options.dryRun;
  const hash = computeCanonicalHash({
    year: batch.year,
    tiger: batch.tiger,
    zones: batch.zones.map((zone) => ({
      code: zone.code, name: zone.name, level: zone.level, geometry: zone.geometry,
      areaLandM2: zone.areaLandM2, indicators: zone.indicators,
    })),
    acs: batch.acs,
  });

  let db = await openDbIfPossible();
  let spatial = [];
  let current = null;
  let realOverlap = null;
  try {
    spatial = await testSpatialValidity(db, batch);
    current = await compareDatabase(db, batch);
    realOverlap = await compareRealOverlap(db, batch);
  } finally {
    if (options.dryRun && db) await db.end();
  }
  printReport(batch, hash, spatial, current, Boolean(db), realOverlap);

  if (options.dryRun) {
    console.log('\nDRY-RUN completado: no se ejecutaron migraciones ni escrituras, y no se crearon archivos.');
    return;
  }

  const invalid = spatial.filter((item) => item.valid === false);
  if (invalid.length) throw new Error(`Hay geometrías inválidas: ${invalid.map((item) => item.geoid).join(', ')}`);
  if (!db) throw new Error('Se requiere conexión MySQL para la importación real; el reporte no escribió archivos ni datos.');
  if (batch.discovery.discoveredWithoutAcs.length > 0 || !batch.discovery.placePopulation) {
    throw new Error('Ejecutión real bloqueada: faltan datos ACS requeridos para uno o más tracts/place. Corrige el acceso a CENSUS_API_KEY o una fuente aprobada; no se escribieron respaldos ni datos.');
  }
  validateExpectedHash(hash, options.expectHash);
  if (!options.expectHash) {
    console.log('\nHash del lote y archivos descargados para revisión:');
    console.log(`- Lote completo: ${hash}`);
    console.log(`- TIGER tracts GeoJSON: ${batch.tiger.tractFileHash || 'no disponible'}`);
    console.log(`- TIGER place GeoJSON: ${batch.tiger.placeFileHash || 'no disponible'}`);
    console.log(`- ACS tracts JSON: ${batch.acs.tractFileHash || 'no disponible'}`);
    console.log(`- ACS place JSON: ${batch.acs.placeFileHash || 'no disponible'}`);
  }
  if (options.yes && process.stdin.isTTY) {
    throw new Error('--yes solo puede usarse en terminal sin entrada interactiva. Ejecución cancelada.');
  }

  let approved = false;
  if (options.yes) {
    approved = true;
  } else {
    approved = await confirm('CONFIRMACIÓN REAL: confirma la importación oficial de Somerton');
  }
  if (!approved) {
    console.log('Importación cancelada por el usuario. No se escribieron respaldos ni datos.');
    return;
  }

  const raw = {
    tractData: tiger.tractData,
    placeData: tiger.placeData,
    acsTracts: acs.tracts,
    acsPlace: acs.place,
  };
  const recalculatedHash = computeCanonicalHash({
    year: batch.year,
    tiger: batch.tiger,
    zones: batch.zones.map((zone) => ({
      code: zone.code, name: zone.name, level: zone.level, geometry: zone.geometry,
      areaLandM2: zone.areaLandM2, indicators: zone.indicators,
    })),
    acs: batch.acs,
  });
  validateExpectedHash(recalculatedHash, options.expectHash);
  await saveRealBackups(batch, recalculatedHash, raw);
  const result = await importReal(db, batch);
  const verification = await verifyPostImport(db, batch);
  console.log(`Importación real completada y verificada: ${result.zones} zonas; ${verification.values} valores.`);
  await db.end();
}

const invoked = process.argv[1] ? path.resolve(process.argv[1]) : '';
const modulePath = path.resolve(fileURLToPath(import.meta.url));
if (invoked.toLowerCase() === modulePath.toLowerCase()) {
  main().catch(async (err) => {
    console.error(`ERROR: ${err.code || ''} ${err.message}`.trim());
    process.exitCode = 1;
  });
}
