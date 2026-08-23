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

    /** Firestore's snapshot always carries the *current full list* for the user, not a diff — so
     * syncing means "this is now the whole truth", not "add these on top of what's there". A
     * plain upsert would never remove a document deleted server-side. */
    @Transaction
    suspend fun replaceAll(ownerId: String, items: List<ItemEntity>) {
        clearForOwner(ownerId)
        upsertAll(items)
    }
}
