// Operaciones de acceso a la tabla local de ítems (lectura, observación en tiempo real, alta, borrado y reemplazo completo tras sincronizar con Firestore).
package com.example.aicollect.data.items.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query("SELECT * FROM items WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    fun observeAll(ownerId: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE itemId = :itemId LIMIT 1")
    suspend fun getById(itemId: String): ItemEntity?

    @Upsert
    suspend fun upsertAll(items: List<ItemEntity>)

    @Query("DELETE FROM items WHERE ownerId = :ownerId")
    suspend fun clearForOwner(ownerId: String)

    @Query("DELETE FROM items WHERE itemId = :itemId")
    suspend fun deleteById(itemId: String)

    @Transaction
    suspend fun replaceAll(ownerId: String, items: List<ItemEntity>) {
        clearForOwner(ownerId)
        upsertAll(items)
    }
}
