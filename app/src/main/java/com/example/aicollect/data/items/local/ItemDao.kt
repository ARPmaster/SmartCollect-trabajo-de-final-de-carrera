// Operaciones de acceso a la tabla local de ítems y sus tablas relacionadas (fotos, búsquedas de
// valoración, histórico de precios): lectura, observación en tiempo real, alta y reemplazo
// completo tras sincronizar con Firestore. El borrado de un ítem elimina en cascada sus filas
// hijas (ForeignKey.CASCADE); el upsert de un ítem existente reemplaza sus filas hijas por
// completo, ya que Firestore sigue siendo la fuente de verdad de esas colecciones.
package com.example.aicollect.data.items.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.aicollect.application.items.Item
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Transaction
    @Query("SELECT * FROM items WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    fun observeAll(ownerId: String): Flow<List<ItemWithRelations>>

    @Transaction
    @Query("SELECT * FROM items WHERE itemId = :itemId LIMIT 1")
    suspend fun getById(itemId: String): ItemWithRelations?

    @Upsert
    suspend fun upsertItem(item: ItemEntity)

    @Insert
    suspend fun insertPhotos(photos: List<ItemPhotoEntity>)

    @Insert
    suspend fun insertValuationSearches(searches: List<ValuationSearchEntity>)

    @Insert
    suspend fun insertPricePoints(points: List<PricePointEntity>)

    @Query("DELETE FROM item_photos WHERE itemId = :itemId")
    suspend fun deletePhotos(itemId: String)

    @Query("DELETE FROM valuation_searches WHERE itemId = :itemId")
    suspend fun deleteValuationSearches(itemId: String)

    @Query("DELETE FROM price_points WHERE itemId = :itemId")
    suspend fun deletePricePoints(itemId: String)

    @Query("DELETE FROM items WHERE ownerId = :ownerId")
    suspend fun clearForOwner(ownerId: String)

    @Query("DELETE FROM items WHERE itemId = :itemId")
    suspend fun deleteById(itemId: String)

    @Transaction
    suspend fun upsertAll(ownerId: String, items: List<Item>) {
        items.forEach { item ->
            upsertItem(item.toEntity(ownerId))
            deletePhotos(item.id)
            insertPhotos(item.toPhotoEntities())
            deleteValuationSearches(item.id)
            insertValuationSearches(item.toValuationSearchEntities())
            deletePricePoints(item.id)
            insertPricePoints(item.toPricePointEntities())
        }
    }

    @Transaction
    suspend fun replaceAll(ownerId: String, items: List<Item>) {
        clearForOwner(ownerId)
        upsertAll(ownerId, items)
    }
}
