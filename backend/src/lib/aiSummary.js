// Etapa 9C: resumen redactado por IA, OPCIONAL y controlado.
//  - Solo se activa si existe GEMINI_API_KEY (Google AI Studio, tiene nivel gratuito) o ANTHROPIC_API_KEY en el
//    entorno del backend (nunca en el código ni en la app). Si hay ambas, se usa Gemini.
//  - La IA recibe únicamente los datos del reporte y se le pide no usar ninguna cifra que no esté ahí.
//  - Después se VALIDA: si el texto contiene un número que no está en los datos, se descarta.
//  - Sin clave, con error o texto inválido -> { status, text: null } y la app muestra solo el reporte de plantilla.

const ANTHROPIC_URL = 'https://api.anthropic.com/v1/messages';
const GEMINI_BASE = 'https://generativelanguage.googleapis.com/v1beta/models';
// El nombre del modelo cambia con el tiempo: se puede fijar con GEMINI_MODEL (cópialo de Google AI Studio).
const GEMINI_DEFAULT_MODEL = 'gemini-3.8-flash';
const TTL_MS = 60 * 60 * 1000;
import { analysisAllowedNumbers, analysisPayload } from './analysis.js';

const cache = new Map();

const SYSTEM = `Eres un asistente que redacta un resumen breve (3 a 4 oraciones, en español) de un reporte de zona para un inversionista.
Reglas estrictas:
- Usa SOLO la información del JSON que recibes. No agregues datos, fuentes, precios, tendencias ni cifras que no estén ahí.
- Si mencionas una cifra, cópiala tal cual aparece en el JSON.
- Menciona al menos una limitación o advertencia. No des consejos de compra ni garantías.
- Escribe un solo párrafo, sin listas ni títulos.`;

/** Números que el texto de la IA tiene permitido mencionar. */
export function allowedNumbers(report) {
  const set = new Set([100]);
  const add = (n) => {
    if (!Number.isFinite(n)) return;
    set.add(n);
    set.add(Math.round(n));
    set.add(Number(n.toFixed(1)));
  };
  if (report.score) {
    add(report.score.value);
    if (report.score.rank) { add(report.score.rank.position); add(report.score.rank.of); }
  }
  for (const i of report.indicators || []) {
    add(i.value); add(i.marginOfError); add(i.variationPct);
    if (i.rank) { add(i.rank.position); add(i.rank.of); }
    const y = String(i.referencePeriod || '').match(/\d{4}/g);
    if (y) y.forEach((v) => add(Number(v)));
  }
  for (const f of report.score?.factors || []) { add(f.weight); add(f.points); }
  return set;
}

export function numbersIn(text) {
  return (String(text).match(/\d[\d.,]*\d|\d/g) || []).map((s) => Number(s.replace(/,/g, ''))).filter(Number.isFinite);
}

/** true si el texto no contiene cifras fuera de los datos. */
export function isGrounded(text, report) {
  const allowed = allowedNumbers(report);
  return numbersIn(text).every((n) => allowed.has(n));
}

function payloadOf(report) {
  return {
    zona: report.zone.name,
    perfil: report.profile.label,
    resumen: report.summary,
    puntaje: report.score ? { valor: report.score.value, confianza: report.score.confidence } : null,
    indicadores: report.indicators.map((i) => ({
      nombre: i.name, valor: i.value, unidad: i.unit, margenDeError: i.marginOfError, posicion: i.rank,
    })),
    aFavor: report.strengths.map((s) => s.text),
    advertencias: report.warnings.map((w) => w.text),
    datosFaltantes: report.missingData,
    limitaciones: report.limitations,
  };
}

function resolveProvider(env) {
  if (env.GEMINI_API_KEY) return { name: 'gemini', key: env.GEMINI_API_KEY, model: env.GEMINI_MODEL || GEMINI_DEFAULT_MODEL };
  if (env.ANTHROPIC_API_KEY) return { name: 'anthropic', key: env.ANTHROPIC_API_KEY, model: env.AI_MODEL || 'claude-haiku-5-5' };
  return null;
}

// Devuelve el texto generado, o lanza/retorna null si la respuesta no es utilizable.
async function callProvider(provider, payload, system, fetchImpl, signal) {
  const userText = `Datos del reporte (JSON):\n${JSON.stringify(payload)}`;
  if (provider.name === 'gemini') {
    const res = await fetchImpl(`${GEMINI_BASE}/${provider.model}:generateContent`, {
      method: 'POST', signal,
      headers: { 'content-type': 'application/json', 'x-goog-api-key': provider.key },
      body: JSON.stringify({
        systemInstruction: { parts: [{ text: system }] },
        contents: [{ role: 'user', parts: [{ text: userText }] }],
        generationConfig: { maxOutputTokens: 600, temperature: 0.3 },
      }),
    });
    if (!res.ok) { const err = new Error(`Gemini HTTP ${res.status}: ${(await res.text()).slice(0, 200)}`); err.status = res.status; throw err; }
    const data = await res.json();
    const parts = data.candidates?.[0]?.content?.parts || [];
    return parts.map((p) => p.text || '').join('').trim();
  }
  const res = await fetchImpl(ANTHROPIC_URL, {
    method: 'POST', signal,
    headers: { 'content-type': 'application/json', 'x-api-key': provider.key, 'anthropic-version': '2023-06-01' },
    body: JSON.stringify({ model: provider.model, max_tokens: 400, system, messages: [{ role: 'user', content: userText }] }),
  });
  if (!res.ok) throw new Error(`Anthropic HTTP ${res.status}: ${(await res.text()).slice(0, 200)}`);
  const data = await res.json();
  return (data.content || []).filter((b) => b.type === 'text').map((b) => b.text).join('').trim();
}

const RETRYABLE = new Set([429, 500, 503]);

// 503/429 de Gemini suelen ser picos de demanda pasajeros: reintenta 2 veces con espera y, si se define
// GEMINI_FALLBACK_MODEL, prueba ese modelo al final.
async function callWithRetry(provider, payload, system, fetchImpl, signal, env, sleep) {
  const models = [provider.model];
  if (provider.name === 'gemini' && env.GEMINI_FALLBACK_MODEL && env.GEMINI_FALLBACK_MODEL !== provider.model) models.push(env.GEMINI_FALLBACK_MODEL);
  let lastErr;
  for (const model of models) {
    for (let attempt = 0; attempt < 3; attempt++) {
      try {
        return await callProvider({ ...provider, model }, payload, system, fetchImpl, signal);
      } catch (e) {
        lastErr = e;
        if (!RETRYABLE.has(e.status)) throw e;
        console.warn(`[ai] ${model}: HTTP ${e.status}, reintento ${attempt + 1}/2`);
        if (attempt < 2) await sleep(1500 * (attempt + 1));
      }
    }
  }
  throw lastErr;
}

async function runAi({ env, fetchImpl, now, sleep = (ms) => new Promise((r) => setTimeout(r, ms)), system, payload, allowed, kind }) {
  const provider = resolveProvider(env);
  if (!provider) return { status: 'disabled', text: null };

  const key = `${kind}|${provider.name}|${provider.model}|${JSON.stringify(payload)}`;
  const hit = cache.get(key);
  if (hit && now - hit.at < TTL_MS) return hit.value;

  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), 40000);
  try {
    const text = await callWithRetry(provider, payload, system, fetchImpl, ctrl.signal, env, sleep);
    if (!text || text.length > 1200) { console.warn('[ai] respuesta vacía o demasiado larga'); return { status: 'invalid', text: null }; }
    if (!numbersIn(text).every((n) => allowed.has(n))) { console.warn('[ai] texto descartado: mencionó una cifra que no está en los datos'); return { status: 'rejected', text: null }; }
    const value = { status: 'ok', text, model: provider.model };
    cache.set(key, { at: now, value });
    return value;
  } catch (e) {
    console.warn(`[ai] falló la llamada: ${e.message}`);
    return { status: 'error', text: null };
  } finally {
    clearTimeout(timer);
  }
}

export async function generateAiSummary(report, { env = process.env, fetchImpl = fetch, now = Date.now(), sleep } = {}) {
  if (!report.score) return { status: resolveProvider(env) ? 'skipped' : 'disabled', text: null }; // sin puntaje no hay nada que redactar
  return runAi({ env, fetchImpl, now, sleep, system: SYSTEM, payload: payloadOf(report), allowed: allowedNumbers(report), kind: 'zone' });
}

const SYSTEM_ANALYSIS = `Eres un asistente que ayuda a un inversionista a comparar ubicaciones que marcó en Somerton, Arizona.
Redacta un solo párrafo en español (4 a 6 oraciones) que diga: cuál ubicación conviene más y por qué, cuáles convienen menos y por qué, y qué conviene verificar antes de decidir.
Reglas estrictas:
- Usa SOLO la información del JSON. No inventes datos, precios, competencia ni cifras; si citas un puntaje, cópialo tal cual.
- Si una ubicación no tiene puntaje, dilo en vez de suponer.
- Deja claro que el puntaje es demográfico y relativo. No des garantías ni consejos financieros definitivos.`;

export async function generateAiAnalysis(analysis, { env = process.env, fetchImpl = fetch, now = Date.now(), sleep } = {}) {
  if (!analysis.items.some((i) => i.score !== null)) return { status: resolveProvider(env) ? 'skipped' : 'disabled', text: null };
  return runAi({ env, fetchImpl, now, sleep, system: SYSTEM_ANALYSIS, payload: analysisPayload(analysis), allowed: analysisAllowedNumbers(analysis), kind: 'analysis' });
}
