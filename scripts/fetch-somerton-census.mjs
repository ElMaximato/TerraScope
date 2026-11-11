// TerraScope - Etapa 4: Descarga de geometrías TIGERweb y datos demográficos Census ACS para Somerton, AZ.
// Uso: node scripts/fetch-somerton-census.mjs
// Requiere conexión a internet (o BypassSandbox).
// Opcional: CENSUS_API_KEY=tu_clave

import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';

const KEY = process.env.CENSUS_API_KEY ?? '';
const STATE = '04'; // Arizona
const COUNTY = '027'; // Yuma County
const PLACE = '68080'; // Somerton city
const OUT_DIR = 'data';
const UA = 'TerraScope-Somerton-Project/1.0 (academic development)';

// Tracts clave de Somerton y su valle inmediato:
// 115.01, 115.02, 115.03 (Somerton urbano) y 114.03 (Valle circundante/Somerton CCD)
const SOMERTON_TRACT_PREFIXES = ['04027011501', '04027011502', '04027011503', '04027011403'];

async function fetchJson(url) {
  const res = await fetch(url, { headers: { 'User-Agent': UA } });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`HTTP ${res.status}: ${text.slice(0, 200)} (URL: ${url})`);
  }
  return await res.json();
}

async function fetchTigerTracts() {
  console.log('1. Descargando geometrías de Census Tracts de Yuma County desde TIGERweb...');
  const url = `https://tigerweb.geo.census.gov/arcgis/rest/services/TIGERweb/Tracts_Blocks/MapServer/0/query?where=STATE%3D'${STATE}'+AND+COUNTY%3D'${COUNTY}'&outFields=GEOID,NAME,BASENAME,CENTLAT,CENTLON,AREALAND,AREAWATR&returnGeometry=true&outSR=4326&f=geojson`;
  const geojson = await fetchJson(url);
  console.log(`   Total de tracts descargados en el condado: ${geojson.features?.length || 0}`);
  return geojson;
}

async function fetchTigerPlace() {
  console.log('2. Descargando límite municipal de Somerton (Incorporated Place) desde TIGERweb...');
  // Capa 4 o 8 en Places_CouSub
  const url = `https://tigerweb.geo.census.gov/arcgis/rest/services/TIGERweb/Places_CouSub/MapServer/4/query?where=STATE%3D'${STATE}'+AND+PLACE%3D'${PLACE}'&outFields=GEOID,NAME,BASENAME,CENTLAT,CENTLON,AREALAND,AREAWATR&returnGeometry=true&outSR=4326&f=geojson`;
  try {
    return await fetchJson(url);
  } catch (err) {
    console.warn('   No se pudo obtener la capa 4 de Places_CouSub, intentando capa alternativa...', err.message);
    const altUrl = `https://tigerweb.geo.census.gov/arcgis/rest/services/TIGERweb/tigerWMS_Current/MapServer/28/query?where=STATE%3D'${STATE}'+AND+PLACE%3D'${PLACE}'&outFields=GEOID,NAME,BASENAME,CENTLAT,CENTLON,AREALAND,AREAWATR&returnGeometry=true&outSR=4326&f=geojson`;
    return await fetchJson(altUrl);
  }
}

const VARS = 'NAME,B01003_001E,B01003_001M,B11001_001E,B11001_001M,B19013_001E,B19013_001M,B01002_001E,B01002_001M';

async function fetchCensusAcs(year) {
  console.log(`3. Consultando indicadores de Census ACS 5 años (${year})...`);
  const keyParam = KEY ? `&key=${KEY}` : '';

  // Tracts
  const tractUrl = `https://api.census.gov/data/${year}/acs/acs5?get=${VARS}&for=tract:*&in=state:${STATE}+county:${COUNTY}${keyParam}`;
  const tractRes = await fetch(tractUrl, { headers: { 'User-Agent': UA } });
  if (!tractRes.ok) {
    throw new Error(`ACS Tracts ${year} fallo: HTTP ${tractRes.status}`);
  }
  const tractData = await tractRes.json();
  const [tractHeader, ...tractRows] = tractData;
  const tracts = tractRows.map((r) => Object.fromEntries(tractHeader.map((h, i) => [h, r[i]])));

  // Place (Somerton)
  const placeUrl = `https://api.census.gov/data/${year}/acs/acs5?get=${VARS}&for=place:${PLACE}&in=state:${STATE}${keyParam}`;
  const placeRes = await fetch(placeUrl, { headers: { 'User-Agent': UA } });
  let place = null;
  if (placeRes.ok) {
    const placeData = await placeRes.json();
    const [placeHeader, ...placeRows] = placeData;
    place = placeRows.map((r) => Object.fromEntries(placeHeader.map((h, i) => [h, r[i]])))[0];
  }

  return { year, tracts, place };
}

async function main() {
  await mkdir(OUT_DIR, { recursive: true });

  const tigerTracts = await fetchTigerTracts();
  let tigerPlace = null;
  try {
    tigerPlace = await fetchTigerPlace();
  } catch (e) {
    console.warn('   Aviso: límite de place omitido:', e.message);
  }

  // Filtrar los tracts de Somerton
  const somertonFeatures = (tigerTracts.features || []).filter((f) => {
    const geoid = f.properties?.GEOID || '';
    return SOMERTON_TRACT_PREFIXES.some((prefix) => geoid.startsWith(prefix));
  });

  console.log(`   Tracts de Somerton identificados: ${somertonFeatures.length}`);

  // Guardar geometrías
  await writeFile(
    path.join(OUT_DIR, 'somerton_tracts_tiger.json'),
    JSON.stringify({ type: 'FeatureCollection', features: somertonFeatures }, null, 2)
  );

  if (tigerPlace?.features?.length) {
    await writeFile(
      path.join(OUT_DIR, 'somerton_place_tiger.json'),
      JSON.stringify(tigerPlace, null, 2)
    );
  }

  // Descargar indicadores ACS
  let acsResult = null;
  for (const year of ['2023', '2024', '2022']) {
    try {
      acsResult = await fetchCensusAcs(year);
      console.log(`   ACS ${year} descargado con éxito.`);
      break;
    } catch (e) {
      console.warn(`   ACS ${year} no disponible: ${e.message}`);
    }
  }

  if (acsResult) {
    await writeFile(
      path.join(OUT_DIR, 'somerton_acs_data.json'),
      JSON.stringify(acsResult, null, 2)
    );
  }

  console.log('\nDescarga completada. Archivos guardados en data/');
}

main().catch((err) => {
  console.error('\nError en la descarga:', err.message);
  process.exit(1);
});

