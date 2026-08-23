package com.example.aicollect.data.items

import com.example.aicollect.application.items.Item
import com.example.aicollect.application.items.ItemEdits
import com.example.aicollect.application.items.ItemRepository
import com.example.aicollect.application.items.NewItem
import com.example.aicollect.application.items.PricePoint
import com.example.aicollect.application.items.ValuationResult
import com.example.aicollect.application.items.ValuationSearch
import com.example.aicollect.data.items.local.ItemDao
import com.example.aicollect.data.items.local.toDomain
import com.example.aicollect.data.items.local.toEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Room ([ItemDao]) is the single source of truth the UI observes — [observeItems] returns a Room
 * query, not the raw Firestore snapshot. Firestore stays the source of truth for writes
 * (create/update/delete) and its snapshot listener is what keeps Room in sync in the background
 * (2026-08-24, requested explicitly to have a local cache, brief-adjacent — the schema itself
 * didn't change, this only adds a local mirror). [getItem] checks Room first and only falls back
 * to a one-shot Firestore fetch (caching the result) for a cold id it hasn't seen yet, e.g. a
 * detail deep-link opened before [observeItems] ever ran.
 *
 * **Simplificación consciente**: el listener de Firestore que alimenta Room nunca se cancela
 * explícitamente (no hay `awaitClose`) — vive tanto como el propio `Singleton`, es decir, tanto
 * como el proceso de la app. Para una app de un único usuario activo a la vez esto es aceptable;
 * si se cierra sesión, la regla de seguridad de Firestore acaba rechazando ese listener (uid ya no
 * coincide) y simplemente deja de actualizar Room, sin crash — no se implementó una baja limpia
 * atada a `observeAuthState()` por alcance/tiempo.
 */
class FirestoreItemRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseStorage: FirebaseStorage,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFunctions: FirebaseFunctions,
    private val itemDao: ItemDao,
) : ItemRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncedOwnerIds = mutableSetOf<String>()

    override suspend fun createItem(newItem: NewItem, imageBytes: List<ByteArray>): Result<String> = runCatching {
        val uid = requireUid()
        val itemRef = itemsCollection(uid).document()
        val imageUrls = uploadImages(uid, itemRef.id, imageBytes)
        val now = System.currentTimeMillis()
        itemRef.set(newItem.toCreateMap(imageUrls, createdAt = now, updatedAt = now)).await()
        itemRef.id
    }

    override suspend fun updateItem(itemId: String, edits: ItemEdits): Result<Unit> = runCatching {
        val uid = requireUid()
        itemsCollection(uid).document(itemId)
            .update(edits.toUpdateMap(updatedAt = System.currentTimeMillis()))
            .await()
        Unit
    }

    override suspend fun deleteItem(itemId: String): Result<Unit> = runCatching {
        val uid = requireUid()
        val folderRef = firebaseStorage.reference.child("users/$uid/items/$itemId")
        // Best effort: an item with no photos (or already-orphaned files) shouldn't block deletion.
        runCatching { folderRef.listAll().await() }.getOrNull()?.items?.forEach { file ->
            runCatching { file.delete().await() }
        }
        itemsCollection(uid).document(itemId).delete().await()
        Unit
    }

    override suspend fun getItem(itemId: String): Result<Item> = runCatching {
        itemDao.getById(itemId)?.toDomain() ?: run {
            val uid = requireUid()
            val snapshot = itemsCollection(uid).document(itemId).get().await()
            val item = snapshot.toItem() ?: throw IllegalStateException("El artículo ya no existe.")
            itemDao.upsertAll(listOf(item.toEntity(uid)))
            item
        }
    }

    override suspend fun refreshValuation(itemId: String): Result<Item> = runCatching {
        val uid = requireUid()
        firebaseFunctions
            .getHttpsCallable(REFRESH_VALUATION_FUNCTION)
            .call(mapOf("itemId" to itemId))
            .await()

        // The Cloud Function already wrote the new valuation fields straight to Firestore — re-read
        // that document directly (bypassing Room) so we return the canonical fresh state, and push
        // it into Room ourselves instead of waiting for the snapshot listener to eventually catch
        // up. Without this, a caller that immediately re-reads via getItem() (Room-first) would see
        // the stale pre-refresh value.
        val snapshot = itemsCollection(uid).document(itemId).get().await()
        val item = snapshot.toItem() ?: throw IllegalStateException("El artículo ya no existe.")
        itemDao.upsertAll(listOf(item.toEntity(uid)))
        item
    }

    override suspend fun searchValuation(
        nombre: String,
        marca: String,
        modelo: String,
        edicion: String?,
    ): Result<ValuationResult> = runCatching {
        val response = firebaseFunctions
            .getHttpsCallable(SEARCH_VALUATION_FUNCTION)
            .call(mapOf("nombre" to nombre, "marca" to marca, "modelo" to modelo, "edicion" to edicion))
            .await()

        @Suppress("UNCHECKED_CAST")
        val body = response.data as? Map<String, Any?> ?: emptyMap()
        @Suppress("UNCHECKED_CAST")
        val rawFuentes = body["fuentes"] as? List<Map<String, Any?>> ?: emptyList()
        ValuationResult(
            precio = (body["precio"] as? Number)?.toDouble(),
            min = (body["min"] as? Number)?.toDouble(),
            max = (body["max"] as? Number)?.toDouble(),
            moneda = body["moneda"] as? String ?: "EUR",
            fuentes = rawFuentes.mapNotNull { entry ->
                val label = entry["label"] as? String ?: return@mapNotNull null
                val url = entry["url"] as? String ?: return@mapNotNull null
                ValuationSearch(label, url)
            },
        )
    }

    override fun observeItems(): Flow<List<Item>> {
        val uid = firebaseAuth.currentUser?.uid ?: return emptyFlow()
        startFirestoreSync(uid)
        return itemDao.observeAll(uid).map { entities -> entities.map { it.toDomain() } }
    }

    /** Starts (once per [uid]) the Firestore listener that keeps Room in sync — [observeItems]
     * itself only reads from Room, this is the write side of that cache. */
    private fun startFirestoreSync(uid: String) {
        if (!syncedOwnerIds.add(uid)) return
        itemsCollection(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val items = snapshot?.documents?.mapNotNull { it.toItem() } ?: emptyList()
                repositoryScope.launch {
                    itemDao.replaceAll(uid, items.map { it.toEntity(uid) })
                }
            }
    }

    private suspend fun uploadImages(uid: String, itemId: String, imageBytes: List<ByteArray>): List<String> =
        imageBytes.mapIndexed { index, bytes ->
            val photoRef = firebaseStorage.reference.child("users/$uid/items/$itemId/foto${index + 1}.jpg")
            photoRef.putBytes(bytes).await()
            photoRef.downloadUrl.await().toString()
        }

    private fun requireUid(): String =
        firebaseAuth.currentUser?.uid ?: throw IllegalStateException("No hay sesión activa.")

    private fun itemsCollection(uid: String) =
        firestore.collection(USERS_COLLECTION).document(uid).collection(ITEMS_COLLECTION)

    private fun NewItem.toCreateMap(imageUrls: List<String>, createdAt: Long, updatedAt: Long): Map<String, Any?> =
        commonFields(imageUrls) + mapOf(
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            // Seeds the history with the value entered at creation time, same shape refreshValuation
            // (PROJECT_CONTEXT roadmap item 2) will append to later — no entry at all if left blank.
            "historialPrecios" to if (valoracionActual != null) {
                listOf(mapOf("fecha" to createdAt, "precio" to valoracionActual))
            } else {
                emptyList()
            },
        )

    private fun ItemEdits.toUpdateMap(updatedAt: Long): Map<String, Any?> = mapOf(
        "nombre" to nombre,
        "descripcion" to descripcion,
        "deporte" to deporte,
        "estado" to estado,
        "updatedAt" to updatedAt,
    )

    private fun NewItem.commonFields(imageUrls: List<String>?): Map<String, Any?> = buildMap {
        put("nombre", nombre)
        put("descripcion", descripcion)
        put("marca", marca)
        put("modelo", modelo)
        put("edicion", edicion)
        put("procedencia", procedencia)
        put("deporte", deporte)
        put("estado", estado)
        put("valoracionActual", valoracionActual)
        put("valoracionMin", valoracionMin)
        put("valoracionMax", valoracionMax)
        put("valoracionMoneda", valoracionMoneda)
        put("fuenteValoracion", fuenteValoracion)
        put("valoracionBusquedas", valoracionBusquedas.map { mapOf("label" to it.label, "url" to it.url) })
        put("confianzaIdentificacion", confianzaIdentificacion)
        if (imageUrls != null) put("imageUrls", imageUrls)
    }

    private fun DocumentSnapshot.toItem(): Item? {
        if (!exists()) return null
        val historial = (get("historialPrecios") as? List<*>)?.mapNotNull { entry ->
            val point = entry as? Map<*, *> ?: return@mapNotNull null
            val fecha = (point["fecha"] as? Number)?.toLong() ?: return@mapNotNull null
            val precio = (point["precio"] as? Number)?.toDouble() ?: return@mapNotNull null
            PricePoint(fecha, precio)
        } ?: emptyList()

        return Item(
            id = id,
            nombre = getString("nombre").orEmpty(),
            descripcion = getString("descripcion"),
            marca = getString("marca").orEmpty(),
            modelo = getString("modelo").orEmpty(),
            edicion = getString("edicion"),
            procedencia = getString("procedencia"),
            deporte = getString("deporte").orEmpty(),
            estado = getString("estado").orEmpty(),
            imageUrls = (get("imageUrls") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            valoracionActual = getDouble("valoracionActual"),
            valoracionMin = getDouble("valoracionMin"),
            valoracionMax = getDouble("valoracionMax"),
            valoracionMoneda = getString("valoracionMoneda").orEmpty(),
            fuenteValoracion = getString("fuenteValoracion"),
            valoracionBusquedas = (get("valoracionBusquedas") as? List<*>)?.mapNotNull { entry ->
                val map = entry as? Map<*, *> ?: return@mapNotNull null
                val label = map["label"] as? String ?: return@mapNotNull null
                val url = map["url"] as? String ?: return@mapNotNull null
                ValuationSearch(label, url)
            } ?: emptyList(),
            historialPrecios = historial,
            confianzaIdentificacion = getDouble("confianzaIdentificacion"),
            createdAt = getLong("createdAt") ?: 0L,
            updatedAt = getLong("updatedAt") ?: 0L,
        )
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val ITEMS_COLLECTION = "items"
        const val REFRESH_VALUATION_FUNCTION = "refreshValuation"
        const val SEARCH_VALUATION_FUNCTION = "searchValuation"
    }
}
