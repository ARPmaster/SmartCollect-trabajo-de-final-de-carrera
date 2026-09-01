// Doble de prueba de ItemRepository para las pruebas de instrumentación: expone el estado por campos mutables que cada test configura, sin llamar a Firestore/Storage.
package com.example.aicollect.testutil

import com.example.aicollect.application.items.Item
import com.example.aicollect.application.items.ItemRepository
import com.example.aicollect.application.items.ValuationResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@Singleton
class FakeItemRepository @Inject constructor() : ItemRepository {

    val itemsFlow = MutableStateFlow<List<Item>>(emptyList())
    var createItemResult: Result<String> = Result.success("fake-item-id")
    var searchValuationResult: Result<ValuationResult> = Result.success(
        ValuationResult(precio = null, min = null, max = null, moneda = "EUR", fuentes = emptyList()),
    )
    var getItemResult: (String) -> Result<Item> = { Result.failure(IllegalStateException("No configurado en el fake")) }

    var lastCreatedItem: Item? = null
    var lastCreatedImageCount: Int? = null
    var createItemCallCount: Int = 0

    var updateItemResult: Result<Unit> = Result.success(Unit)
    var lastUpdatedItemId: String? = null
    var lastUpdatedItem: Item? = null
    var updateItemCallCount: Int = 0

    var deleteItemResult: Result<Unit> = Result.success(Unit)
    var lastDeletedItemId: String? = null
    var deleteItemCallCount: Int = 0

    override suspend fun createItem(item: Item, imageBytes: List<ByteArray>): Result<String> {
        lastCreatedItem = item
        lastCreatedImageCount = imageBytes.size
        createItemCallCount++
        return createItemResult
    }

    override suspend fun updateItem(itemId: String, item: Item): Result<Unit> {
        lastUpdatedItemId = itemId
        lastUpdatedItem = item
        updateItemCallCount++
        return updateItemResult
    }

    override suspend fun deleteItem(itemId: String): Result<Unit> {
        lastDeletedItemId = itemId
        deleteItemCallCount++
        return deleteItemResult
    }

    override suspend fun getItem(itemId: String): Result<Item> = getItemResult(itemId)

    override fun observeItems(): Flow<List<Item>> = itemsFlow

    override suspend fun refreshValuation(itemId: String): Result<Item> = getItemResult(itemId)

    override suspend fun searchValuation(
        nombre: String,
        marca: String,
        modelo: String,
        edicion: String?,
    ): Result<ValuationResult> = searchValuationResult
}
