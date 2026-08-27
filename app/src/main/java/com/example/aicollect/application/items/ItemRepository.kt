/** Define el contrato CRUD sobre la colección de ítems del usuario (crear, editar, eliminar,
leer y observar en tiempo real) y las operaciones de valoración de mercado, independiente de
 la implementación concreta (Firestore).*/
package com.example.aicollect.application.items

import kotlinx.coroutines.flow.Flow

interface ItemRepository {

    suspend fun createItem(item: Item, imageBytes: List<ByteArray>): Result<String>

    suspend fun updateItem(itemId: String, item: Item): Result<Unit>

    suspend fun deleteItem(itemId: String): Result<Unit>

    suspend fun getItem(itemId: String): Result<Item>

    fun observeItems(): Flow<List<Item>>

    suspend fun refreshValuation(itemId: String): Result<Item>

    suspend fun searchValuation(nombre: String, marca: String, modelo: String, edicion: String?): Result<ValuationResult>
}
