/** Implementación de ItemRepository con Firestore como origen de verdad para las escrituras, y
 * Room como caché local que la UI observa: gestiona el CRUD de ítems, la subida de fotos a
 * Storage, la sincronización en segundo plano entre Firestore y Room, y la valoración de
 * mercado mediante las Cloud Functions correspondientes.*/
package com.example.aicollect.data.items

import com.example.aicollect.application.items.Item
import com.example.aicollect.application.items.ItemRepository
import com.example.aicollect.application.items.PricePoint
import com.example.aicollect.application.items.ValuationResult
import com.example.aicollect.application.items.ValuationSearch
import com.example.aicollect.data.callFunctionWithRetry
import com.example.aicollect.data.items.local.ItemDao
import com.example.aicollect.data.items.local.toDomain
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirestoreItemRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseStorage: FirebaseStorage,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFunctions: FirebaseFunctions,
    private val itemDao: ItemDao,
) : ItemRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val firstSyncSignals = mutableMapOf<String, CompletableDeferred<Unit>>()

    override suspend fun createItem(item: Item, imageBytes: List<ByteArray>): Result<String> = runCatching {
        val uid = requireUid()
        val itemRef = itemsCollection(uid).document()
        val imageUrls = uploadImages(uid, itemRef.id, imageBytes)
        val now = System.currentTimeMillis()
        itemRef.set(item.toCreateMap(imageUrls, createdAt = now, updatedAt = now)).await()
        itemRef.id
    }

    override suspend fun updateItem(itemId: String, item: Item): Result<Unit> = runCatching {
        val uid = requireUid()
        val updatedAt = System.currentTimeMillis()
        itemsCollection(uid).document(itemId)
            .update(item.toUpdateMap(updatedAt = updatedAt))
            .await()
        itemDao.upsertAll(uid, listOf(item.copy(id = itemId, updatedAt = updatedAt)))
        Unit
    }

    override suspend fun deleteItem(itemId: String): Result<Unit> = runCatching {
        val uid = requireUid()
        val folderRef = firebaseStorage.reference.child("users/$uid/items/$itemId")
        runCatching { folderRef.listAll().await() }.getOrNull()?.items?.forEach { file ->
            runCatching { file.delete().await() }
        }
        itemsCollection(uid).document(itemId).delete().await()
        itemDao.deleteById(itemId)
        Unit
    }

    override suspend fun getItem(itemId: String): Result<Item> = runCatching {
        itemDao.getById(itemId)?.toDomain() ?: run {
            val uid = requireUid()
            val snapshot = itemsCollection(uid).document(itemId).get().await()
            val item = snapshot.toItem() ?: throw IllegalStateException("El artículo ya no existe.")
            itemDao.upsertAll(uid, listOf(item))
            item
        }
    }

    override suspend fun refreshValuation(itemId: String): Result<Item> = runCatching {
        val uid = requireUid()
        callFunctionWithRetry {
            firebaseFunctions
                .getHttpsCallable(REFRESH_VALUATION_FUNCTION)
                .call(mapOf("itemId" to itemId))
                .await()
        }

        val snapshot = itemsCollection(uid).document(itemId).get().await()
        val item = snapshot.toItem() ?: throw IllegalStateException("El artículo ya no existe.")
        itemDao.upsertAll(uid, listOf(item))
        item
    }

    override suspend fun searchValuation(
        nombre: String,
        marca: String,
        modelo: String,
        edicion: String?,
    ): Result<ValuationResult> = runCatching {
        val response = callFunctionWithRetry {
            firebaseFunctions
                .getHttpsCallable(SEARCH_VALUATION_FUNCTION)
                .call(mapOf("nombre" to nombre, "marca" to marca, "modelo" to modelo, "edicion" to edicion))
                .await()
        }

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
        val firstSync = startFirestoreSync(uid)
        return flow {
            firstSync.await()
            emitAll(itemDao.observeAll(uid).map { entities -> entities.map { it.toDomain() } })
        }
    }

    private fun startFirestoreSync(uid: String): CompletableDeferred<Unit> {
        firstSyncSignals[uid]?.let { return it }
        val signal = CompletableDeferred<Unit>()
        firstSyncSignals[uid] = signal
        itemsCollection(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    signal.complete(Unit)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { it.toItem() } ?: emptyList()
                repositoryScope.launch {
                    itemDao.replaceAll(uid, items)
                    signal.complete(Unit)
                }
            }
        return signal
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

    private fun Item.toCreateMap(imageUrls: List<String>, createdAt: Long, updatedAt: Long): Map<String, Any?> =
        commonFields(imageUrls) + mapOf(
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "historialPrecios" to if (valoracionActual != null) {
                listOf(mapOf("fecha" to createdAt, "precio" to valoracionActual))
            } else {
                emptyList()
            },
        )

    private fun Item.toUpdateMap(updatedAt: Long): Map<String, Any?> = mapOf(
        "nombre" to nombre,
        "descripcion" to descripcion,
        "deporte" to deporte,
        "estado" to estado,
        "updatedAt" to updatedAt,
    )

    private fun Item.commonFields(imageUrls: List<String>?): Map<String, Any?> = buildMap {
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
