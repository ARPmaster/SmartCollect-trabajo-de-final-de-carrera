import * as functions from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { GoogleGenerativeAI } from "@google/generative-ai";

// Guardado así (no admin.initializeApp() directo) para que este archivo no dependa de que
// recognizeItem.ts se cargue primero — admin.initializeApp() lanza si se llama dos veces.
if (admin.apps.length === 0) {
  admin.initializeApp();
}
const db = admin.firestore();
const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY!);

/** Una búsqueda que Gemini ejecutó de verdad para llegar al precio — NO es un enlace directo al
 * anuncio de eBay/Vinted que miró (esta API no expone eso, ver `extractGroundingSources`), es un
 * enlace de búsqueda de Google con la misma query. Sigue siendo verificable (el usuario puede ver
 * resultados comparables), pero no es "aquí está la fuente exacta". */
interface GroundingSource {
  label: string;
  url: string;
}

interface ValuationResult {
  precio: number | null;
  min: number | null;
  max: number | null;
  moneda: string;
  fuentes: GroundingSource[];
}

interface ProductsCacheDoc extends ValuationResult {
  actualizadoEn: number;
}

// Si el caché tiene menos de esto, se reutiliza sin volver a llamar a Gemini — sustituye al
// Cloud Scheduler descartado (PROJECT_CONTEXT.md): la frescura se comprueba al pulsar el botón,
// no con un cron.
const CACHE_MAX_AGE_MS = 30 * 24 * 60 * 60 * 1000; // 30 días

/** Misma clave para "el mismo producto" en toda la app: marca+modelo+edición normalizados (cae al
 * nombre libre completo si esos campos están vacíos, caso de un item creado a mano). Usada tanto
 * por [refreshValuation] (item ya guardado) como por [searchValuation] (2026-08-24, antes de que
 * el item exista) — así ambas rutas comparten el mismo `products_cache`, sea cual sea el uid que
 * pregunte. 2026-08-24: encontrado en pruebas reales — la misma zapatilla subida dos veces con el
 * nombre escrito distinto daba precios distintos porque nunca coincidía con la clave anterior. */
function buildSearchKey(
  nombre: string,
  marca?: string | null,
  modelo?: string | null,
  edicion?: string | null
): { searchQuery: string; cacheKey: string } {
  const searchQuery = [nombre, marca, modelo, edicion]
    .filter((v) => typeof v === "string" && v.trim().length > 0)
    .join(" ");
  const structuredKey = [marca, modelo, edicion]
    .filter((v) => typeof v === "string" && v.trim().length > 0)
    .join(" ");
  return { searchQuery, cacheKey: normalizeKey(structuredKey || nombre) };
}

/** Mira `products_cache` antes de gastar una llamada a Gemini; si no hay nada fresco, busca y
 * cachea el resultado (best-effort: si falla el caché, no debe tumbar la respuesta al usuario). */
async function getOrComputeValuation(cacheKey: string, searchQuery: string): Promise<ValuationResult> {
  const cacheRef = db.collection("products_cache").doc(cacheKey);
  const cacheSnap = await cacheRef.get();
  const cached = cacheSnap.data() as ProductsCacheDoc | undefined;
  if (cached && Date.now() - cached.actualizadoEn < CACHE_MAX_AGE_MS) {
    return cached;
  }
  const valuation = await searchValuationWithGemini(searchQuery);
  try {
    await cacheRef.set({ ...valuation, actualizadoEn: Date.now() } satisfies ProductsCacheDoc);
  } catch (err) {
    console.error("getOrComputeValuation: no se pudo escribir products_cache", err);
  }
  return valuation;
}

/**
 * 2026-08-24: llamada ANTES de crear el item (desde "Nueva Publicación"), a diferencia de
 * [refreshValuation] que opera sobre uno ya guardado — el cliente espera esta respuesta y escribe
 * el item en Firestore ya con el precio puesto, en una sola operación, en vez de crear vacío y
 * actualizar después (ese hueco "Sin valorar" temporal era confuso). No toca ningún documento de
 * item, solo lee/escribe el `products_cache` compartido.
 */
export const searchValuation = functions.onCall(
  { region: "europe-west1", timeoutSeconds: 60, secrets: ["GEMINI_API_KEY"] },
  async (request) => {
    if (!request.auth) {
      throw new functions.HttpsError("unauthenticated", "Se requiere sesión iniciada");
    }
    const { nombre, marca, modelo, edicion } = request.data as {
      nombre?: string;
      marca?: string;
      modelo?: string;
      edicion?: string;
    };
    const { searchQuery, cacheKey } = buildSearchKey(nombre ?? "", marca, modelo, edicion);
    if (!searchQuery) {
      throw new functions.HttpsError("invalid-argument", "Faltan datos para buscar el precio");
    }
    return await getOrComputeValuation(cacheKey, searchQuery);
  }
);

export const refreshValuation = functions.onCall(
  // "secrets" es obligatorio para que Firebase enlace el secreto de Secret Manager (el mismo que
  // ya usa recognizeItem) a ESTA función en concreto — cada función necesita su propia
  // declaración, no se hereda solo por vivir en el mismo proyecto (2026-08-24, causa real del
  // error "API key not valid" en el primer despliegue).
  { region: "europe-west1", timeoutSeconds: 60, secrets: ["GEMINI_API_KEY"] },
  async (request) => {
    if (!request.auth) {
      throw new functions.HttpsError("unauthenticated", "Se requiere sesión iniciada");
    }
    const uid = request.auth.uid;

    const { itemId } = request.data as { itemId: string };
    if (!itemId) {
      throw new functions.HttpsError("invalid-argument", "Falta itemId");
    }

    // ---------- 1. Leer el item ya guardado y confirmado por el usuario ----------
    const itemRef = db.collection("users").doc(uid).collection("items").doc(itemId);
    const itemSnap = await itemRef.get();
    if (!itemSnap.exists) {
      throw new functions.HttpsError("not-found", "El artículo no existe");
    }
    const item = itemSnap.data()!;
    const { searchQuery, cacheKey } = buildSearchKey(item.nombre, item.marca, item.modelo, item.edicion);
    if (!searchQuery) {
      throw new functions.HttpsError("failed-precondition", "El artículo no tiene datos suficientes para buscar precio");
    }

    // ---------- 2. Mirar products_cache antes de gastar una llamada a Gemini ----------
    const valuation = await getOrComputeValuation(cacheKey, searchQuery);

    // ---------- 3. Escribir en el item: valoracionActual + nueva entrada en historialPrecios ----------
    // valoracionActual sigue en null si no se encontró nada fiable — nunca se inventa un número
    // (misma regla que en la creación manual, ver NewItem kdoc en el cliente Android).
    const now = Date.now();
    const updates: Record<string, unknown> = {
      valoracionActual: valuation.precio,
      valoracionMin: valuation.min,
      valoracionMax: valuation.max,
      valoracionMoneda: valuation.moneda,
      fuenteValoracion: valuation.fuentes.length > 0 ? "gemini_grounded_search" : null,
      // Renombrado de valoracionFuentesUrls (2026-08-24): confirmado con logs reales que esta API
      // no expone URLs directas a los anuncios que Gemini miró, solo las búsquedas que ejecutó —
      // "búsquedas", no "fuentes", para no dar a entender algo que no es cierto.
      valoracionBusquedas: valuation.fuentes.map((f) => ({ label: f.label, url: f.url })),
      updatedAt: now,
    };
    if (valuation.precio !== null) {
      updates.historialPrecios = admin.firestore.FieldValue.arrayUnion({ fecha: now, precio: valuation.precio });
    }
    await itemRef.update(updates);

    return valuation;
  }
);

async function searchValuationWithGemini(searchQuery: string): Promise<ValuationResult> {
  const model = genAI.getGenerativeModel({
    model: "gemini-3-flash-preview", // mismo modelo que recognizeItem.ts, mismo aviso: verificar
    // nombre exacto de la herramienta de grounding contra la documentación vigente del SDK
    // (@google/generative-ai ^0.24.1) — "googleSearch" es la mejor suposición actual, puede
    // que tu versión instalada espere "googleSearchRetrieval" u otro nombre.
    tools: [{ googleSearch: {} } as unknown as never],
  });

  // No se fuerza responseMimeType: "application/json" a propósito — combinar salida JSON forzada
  // con la herramienta de grounding no es compatible en algunas versiones de la API. Se pide el
  // JSON en el propio prompt y se parsea de forma tolerante (quitando fences de markdown si Gemini
  // los añade, cosa habitual cuando además está citando fuentes web).
  const prompt = `
Busca en internet el precio de este objeto de coleccionismo deportivo:
"${searchQuery}"

ORDEN DE PRIORIDAD (síguelo estrictamente):
1. Primero busca en tiendas OFICIALES: la web oficial de la marca, o retailers autorizados
   vendiendo ese mismo modelo y esa misma edición exactos, como producto nuevo.
   Si lo encuentras ahí, usa ESE precio como "precio" — es el más fiable que existe, no lo
   promedies con precios de segunda mano ni lo bajes por ellos.
2. Solo si NO encuentras ese modelo y edición exactos en ninguna tienda oficial (por ejemplo
   porque ya no se fabrica o está agotado en todas partes), busca en plataformas de segunda
   mano/coleccionismo (eBay, Vinted, StockX, foros especializados) y calcula una media
   razonable de precios de segunda mano.

En cualquiera de los dos casos, calcula también el rango (mínimo/máximo) de los precios que
hayas visto en esa misma categoría de fuente (no mezcles precio oficial con precios de segunda
mano en el mismo rango).

Si no encuentras ninguna referencia fiable en ningún sitio, no inventes ni extrapoles un número.

Devuelve EXCLUSIVAMENTE este JSON, sin texto adicional ni bloques de código:
{
  "precio": number | null,
  "min": number | null,
  "max": number | null,
  "moneda": "EUR"
}
`.trim();

  let result;
  try {
    result = await model.generateContent(prompt);
  } catch (err) {
    console.error("refreshValuation: Gemini generateContent falló", err);
    throw new functions.HttpsError(
      "internal",
      `Gemini falló: ${(err as Error).message}`,
    );
  }
  const rawText = result.response.text();

  let parsed: { precio: number | null; min: number | null; max: number | null; moneda: string };
  try {
    parsed = JSON.parse(stripMarkdownFences(rawText));
  } catch (err) {
    console.error("refreshValuation: respuesta de Gemini no parseable", err, rawText);
    throw new functions.HttpsError("internal", "Respuesta de Gemini no parseable");
  }

  const fuentes = extractGroundingSources(result.response);

  return {
    precio: parsed.precio,
    min: parsed.min,
    max: parsed.max,
    moneda: parsed.moneda || "EUR",
    fuentes,
  };
}

/**
 * 2026-08-24: confirmado con una respuesta real (logs de producción) que esta API/versión del SDK
 * NO devuelve `groundingChunks` (el campo que se había supuesto originalmente, sin verificar) — el
 * único rastro de qué buscó Gemini está en `groundingMetadata.searchEntryPoint.renderedContent`,
 * un bloque HTML con enlaces `<a class="chip" href="https://www.google.com/search?q=...">texto</a>`.
 * Son enlaces de búsqueda de Google con la misma query, NO la URL del anuncio concreto que Gemini
 * leyó — no hay forma de conseguir eso con esta API tal como está expuesta hoy. Se parsean con
 * regex en vez de un DOM parser (no hay ninguno en el proyecto, y es HTML simple y controlado por
 * Google, no contenido arbitrario de usuario).
 */
function extractGroundingSources(response: unknown): GroundingSource[] {
  try {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const candidate = (response as any)?.candidates?.[0];
    const html = candidate?.groundingMetadata?.searchEntryPoint?.renderedContent as string | undefined;
    if (!html) return [];
    const matches = [...html.matchAll(/<a class="chip" href="([^"]+)">([^<]*)<\/a>/g)];
    return matches.map((m) => ({ url: decodeHtmlEntities(m[1]), label: decodeHtmlEntities(m[2]) }));
  } catch (err) {
    console.error("refreshValuation: no se pudo extraer groundingMetadata", err);
    return [];
  }
}

function decodeHtmlEntities(text: string): string {
  return text
    .replace(/&#39;/g, "'")
    .replace(/&quot;/g, "\"")
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">");
}

function stripMarkdownFences(text: string): string {
  return text.trim().replace(/^```json\s*/i, "").replace(/^```\s*/, "").replace(/```\s*$/, "").trim();
}

/** Ordena las palabras alfabéticamente antes de unirlas — así "Nike Air Jordan 1" y "Air Jordan 1
 * Nike" caen en la misma clave. Sigue sin ser matching semántico: dos descripciones con palabras
 * realmente distintas (sinónimos, datos que faltan en una de las dos) seguirán sin coincidir —
 * eso necesitaría embeddings/búsqueda difusa, fuera de alcance para esta entrega. */
function normalizeKey(text: string): string {
  return text
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9\s]+/g, " ")
    .split(/\s+/)
    .filter((token) => token.length > 0)
    .sort()
    .join("_")
    .slice(0, 200);
}
