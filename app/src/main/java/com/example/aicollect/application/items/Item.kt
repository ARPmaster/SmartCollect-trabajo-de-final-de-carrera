package com.example.aicollect.application.items

/** One entry in `historialPrecios` (brief Sección 4): a valuation snapshot at a point in time. */
data class PricePoint(val fecha: Long, val precio: Double)

/** One entry in `valoracionBusquedas` — a search `refreshValuation` actually ran (Gemini +
 * grounding en Google Search), not a direct link to the specific eBay/Vinted listing it read.
 * Confirmed 2026-08-24 via real logs: that API doesn't expose per-listing citation URLs, only
 * `webSearchQueries` + a rendered "search entry point" widget — [url] is a Google Search results
 * link for [label], still useful for the user to verify comparable listings themselves. */
data class ValuationSearch(val label: String, val url: String)

/** Result of a market-price lookup (`searchValuation`/`refreshValuation` Cloud Functions) that
 * hasn't been attached to any [Item] yet — used at publish time, before the item has an id. */
data class ValuationResult(
    val precio: Double?,
    val min: Double?,
    val max: Double?,
    val moneda: String,
    val fuentes: List<ValuationSearch>,
)

/** Mirrors the published `users/{uid}/items/{itemId}` schema (brief Sección 4), plus
 * [descripcion] — added 2026-08-21 for the officially accepted "Nueva Publicación - Objeto"
 * design, which asks for one free-text field instead of separate marca/modelo/edición/procedencia
 * inputs. Firestore has no rigid schema to migrate for this (additive field on existing docs). */
data class Item(
    val id: String,
    val nombre: String,
    val descripcion: String?,
    val marca: String,
    val modelo: String,
    val edicion: String?,
    val procedencia: String?,
    val deporte: String,
    val estado: String,
    val imageUrls: List<String>,
    val valoracionActual: Double?,
    /** [valoracionMin]/[valoracionMax] y [valoracionBusquedas] solo los rellena
     * `refreshValuation` (Gemini con grounding en Google Search, 2026-08-24 — sustituye a la idea
     * original de eBay Browse API) — null/vacío hasta que el usuario pulsa "Actualizar valor" en
     * el Detalle. Nunca los rellena `NewItem`/la creación manual. */
    val valoracionMin: Double?,
    val valoracionMax: Double?,
    val valoracionMoneda: String,
    val fuenteValoracion: String?,
    val valoracionBusquedas: List<ValuationSearch>,
    val historialPrecios: List<PricePoint>,
    val confianzaIdentificacion: Double?,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Payload for [ItemRepository.createItem]/[updateItem] — no id/timestamps, the repository
 * assigns those. [valoracionActual]/[valoracionMin]/[valoracionMax]/[valoracionBusquedas] stay
 * null/empty when no market value was found — never inferred or defaulted to zero.
 *
 * 2026-08-24: [NewPostViewModel] now calls `searchValuation` and waits for the result *before*
 * building this, so [valoracionActual] arrives already populated at creation time — a single
 * Firestore write instead of create-then-`refreshValuation`-update (replaces the previous
 * fire-and-forget flow, which left a visible "Sin valorar" gap and could race with a concurrent
 * lookup for the same product). */
data class NewItem(
    val nombre: String,
    val descripcion: String?,
    val marca: String,
    val modelo: String,
    val edicion: String?,
    val procedencia: String?,
    val deporte: String,
    val estado: String,
    val valoracionActual: Double?,
    val valoracionMin: Double?,
    val valoracionMax: Double?,
    val valoracionMoneda: String,
    val fuenteValoracion: String?,
    val valoracionBusquedas: List<ValuationSearch>,
    val confianzaIdentificacion: Double?,
)

/** Payload for [ItemRepository.updateItem] — deliberately narrower than [NewItem]: no
 * marca/modelo/edición/procedencia (fijan la identidad del producto que usan el caché de precios
 * compartido y el aviso de duplicados, no son editables tras crear el item) y ningún campo de
 * valoración/historialPrecios/confianzaIdentificacion (calculados, nunca a mano — 2026-08-24,
 * pedido explícito: "lo único no modificable del CRUD es el precio"). El `.update()` de Firestore
 * solo toca las claves que aparecen en el mapa que genera esto, así que cualquier campo que no
 * esté aquí queda garantizado intacto, no solo "no mostrado en el formulario". */
data class ItemEdits(
    val nombre: String,
    val descripcion: String?,
    val deporte: String,
    val estado: String,
)
