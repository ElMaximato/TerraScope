// TerraScope - Etapa 0.5: validación de datos reales para Somerton, AZ
// Uso:  node scripts/validate-data.mjs
// Requiere Node 18+ (usa fetch nativo). Sin dependencias.
// Opcional: CENSUS_API_KEY=tu_clave  (gratis en https://api.census.gov/data/key_signup.html)
// Opcional: ACS_YEAR=2023            (por defecto prueba 2024 y luego 2023)

import { mkdir, writeFile } from "node:fs/promises";

const KEY = process.env.CENSUS_API_KEY ?? "";
const YEARS = process.env.ACS_YEAR ? [process.env.ACS_YEAR] : ["2024", "2023"];
const STATE = "04"; // Arizona
const COUNTY = "027"; // Yuma County
const PLACE = "68080"; // Somerton (FIPS 04-68080)

// Caja aproximada alrededor de Somerton: sur, oeste, norte, este. AJUSTAR si hace falta.
const BBOX = [32.57, -114.75, 32.63, -114.67];

const OUT = "data/samples";
const UA = "TerraScope-student-validation/0.1 (school project)";
const report = { generatedAt: new Date().toISOString(), census: {}, osm: {}, errors: [] };

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const clean = (v) => {
  const n = Number(v);
  return v === null || v === undefined || Number.isNaN(n) || n <= -111111111 ? null : n;
};

async function censusGet(year, get, forPart, inPart) {
  let url = `https://api.census.gov/data/${year}/acs/acs5?get=${get}&for=${forPart}`;
  if (inPart) url += `&in=${inPart}`;
  if (KEY) url += `&key=${KEY}`;
  const safeUrl = KEY ? url.replace(KEY, "***") : url;
  const res = await fetch(url, { headers: { "User-Agent": UA } });
  const text = await res.text();
  let rows;
  try {
    rows = JSON.parse(text);
  } catch {
    const snippet = text
      .replace(/<style[\s\S]*?<\/style>/gi, "")
      .replace(/<[^>]+>/g, " ")
      .replace(/\s+/g, " ")
      .trim()
      .slice(0, 300);
    throw new Error(`HTTP ${res.status} (${res.headers.get("content-type")}) no es JSON: "${snippet}" :: ${safeUrl}`);
  }
  if (!res.ok) throw new Error(`HTTP ${res.status} :: ${safeUrl}`);
  const [header, ...data] = rows;
  return data.map((r) => Object.fromEntries(header.map((h, i) => [h, r[i]])));
}

const VARS = "NAME,B01003_001E,B01003_001M,B19013_001E,B19013_001M,B01002_001E,B11001_001E";

async function tryQuery(label, fn) {
  try {
    return await fn();
  } catch (e) {
    report.errors.push(`${label}: ${e.message}`);
    console.warn(`[${label}] ${e.message}`);
    return null;
  }
}

async function runCensus() {
  const slim = (r) => ({
    name: r.NAME,
    poblacion: clean(r.B01003_001E),
    poblacion_moe: clean(r.B01003_001M),
    ingreso_mediano: clean(r.B19013_001E),
    ingreso_moe: clean(r.B19013_001M),
    edad_mediana: clean(r.B01002_001E),
    hogares: clean(r.B11001_001E),
  });
  for (const year of YEARS) {
    const place = await tryQuery(`place ${year}`, () => censusGet(year, VARS, `place:${PLACE}`, `state:${STATE}`));
    const bgs = await tryQuery(`blockgroups ${year}`, () => censusGet(year, VARS, "block%20group:*", `state:${STATE}+county:${COUNTY}`));
    const tracts = await tryQuery(`tracts ${year}`, () => censusGet(year, VARS, "tract:*", `state:${STATE}+county:${COUNTY}`));
    if (!place && !bgs && !tracts) continue;
    report.census = {
      acsYear: year,
      place: place ? place.map(slim) : "FALLÓ",
      tractsInYumaCounty: tracts ? tracts.length : "FALLÓ",
      blockGroupsInYumaCounty: bgs ? bgs.length : "FALLÓ",
    };
    if (place) await writeFile(`${OUT}/census_place_somerton.json`, JSON.stringify(place.map(slim), null, 2));
    if (bgs) await writeFile(`${OUT}/census_blockgroups_yuma.json`, JSON.stringify(bgs.map((r) => ({ geoid: r.state + r.county + r.tract + r["block group"], ...slim(r) })), null, 2));
    console.log(`Census ACS ${year}: al menos una consulta respondió`);
    if (place) return;
  }
}

const OVERPASS = ["https://overpass-api.de/api/interpreter", "https://overpass.private.coffee/api/interpreter"];

async function overpass(query) {
  for (const ep of OVERPASS) {
    try {
      const res = await fetch(ep, {
        method: "POST",
        headers: { "User-Agent": UA, "Content-Type": "application/x-www-form-urlencoded" },
        body: "data=" + encodeURIComponent(query),
      });
      if (!res.ok) throw new Error(`${ep} -> ${res.status}`);
      return (await res.json()).elements ?? [];
    } catch (e) {
      report.errors.push(String(e));
      console.warn(`Overpass falló: ${e.message}`);
      await sleep(1500);
    }
  }
  return null;
}

function topCounts(elements, keys) {
  const c = {};
  for (const el of elements) {
    const t = el.tags ?? {};
    const k = keys.find((x) => t[x]);
    if (k) c[`${k}=${t[k]}`] = (c[`${k}=${t[k]}`] ?? 0) + 1;
  }
  return Object.entries(c).sort((a, b) => b[1] - a[1]).slice(0, 15);
}

async function runOsm() {
  const bb = BBOX.join(",");
  const poiQ = `[out:json][timeout:60];(nwr["shop"](${bb});nwr["amenity"](${bb});nwr["office"](${bb});nwr["craft"](${bb}););out center tags;`;
  const pois = await overpass(poiQ);
  await sleep(2000);
  const roadQ = `[out:json][timeout:60];way["highway"~"^(motorway|trunk|primary|secondary|tertiary)$"](${bb});out tags center;`;
  const roads = await overpass(roadQ);
  if (pois) {
    const named = pois.filter((e) => e.tags?.name);
    report.osm.poiTotal = pois.length;
    report.osm.poiConNombre = named.length;
    report.osm.topCategorias = topCounts(pois, ["shop", "amenity", "office", "craft"]);
    await writeFile(`${OUT}/osm_pois_somerton.json`, JSON.stringify(pois, null, 2));
  }
  if (roads) {
    report.osm.viasPrincipales = roads.length;
    report.osm.nombresVias = [...new Set(roads.map((r) => r.tags?.name).filter(Boolean))].slice(0, 20);
  }
  report.osm.bbox = BBOX;
  report.osm.atribucion = "© OpenStreetMap contributors (ODbL 1.0)";
}

await mkdir(OUT, { recursive: true });
await runCensus();
await runOsm();
await writeFile(`${OUT}/validation-report.json`, JSON.stringify(report, null, 2));
console.log("\n===== RESUMEN =====");
console.log(JSON.stringify(report, null, 2));
console.log(`\nReporte guardado en ${OUT}/validation-report.json`);
