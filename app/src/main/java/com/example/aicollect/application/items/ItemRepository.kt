package com.example.aicollect.application.items

import kotlinx.coroutines.flow.Flow

/**
 * CRUD interface for the user's collection, born complete on purpose even though only
 * [createItem] has a screen calling it today (Nueva Publicación) — decided 2026-08-19, see
 * PROJECT_CONTEXT.md: Editar/Eliminar will reuse this same repository once those screens exist,
 * not a redesign later.
 */
interface ItemRepository {

    /** Uploads [imageBytes] (JPEG, already compressed) to Storage under
     * `users/{uid}/items/{itemId}/fotoN.jpg`, then writes the Firestore doc. Returns the new
     * item's id. */
    suspend fun createItem(newItem: NewItem, imageBytes: List<ByteArray>): Result<String>

    /** Only touches [ItemEdits]'s fields — marca/modelo/edición/procedencia and every valuation
     * field (precio, min/max, búsquedas, historial, confianza) are guaranteed untouched, see
     * [ItemEdits] kdoc. */
    suspend fun updateItem(itemId: String, edits: ItemEdits): Result<Unit>

    /** Also best-effort deletes every file under the item's Storage folder. */
    suspend fun deleteItem(itemId: String): Result<Unit>

    suspend fun getItem(itemId: String): Result<Item>

    /** Real-time listener on the current user's items, newest first — Home/My Vault/Detail all
     * read through this instead of a one-shot fetch, so a save from Nueva Publicación (or any
     * other write) shows up immediately without a manual refresh. */
    fun observeItems(): Flow<List<Item>>

    /** Calls the `refreshValuation` Cloud Function (Gemini + grounding en Google Search,
     * 2026-08-24) for an already-saved item — never for an unconfirmed candidate. Returns the
     * item with its valuation fields updated; implementations must also update any local cache
     * (Room) so a caller re-reading the item right after this returns doesn't see stale data. */
    suspend fun refreshValuation(itemId: String): Result<Item>

    /** Same lookup as [refreshValuation] (products_cache + Gemini grounding, shared across every
     * user so the same real-world product prices consistently) but for an item that doesn't exist
     * yet — called from Nueva Publicación right before [createItem] so the item can be written
     * already priced, in one Firestore write. Never touches any item document itself. */
    suspend fun searchValuation(nombre: String, marca: String, modelo: String, edicion: String?): Result<ValuationResult>
}
