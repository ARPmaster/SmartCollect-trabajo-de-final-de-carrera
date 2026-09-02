// Define la base de datos Room de la app (caché local de ítems) y su esquema/versión.
package com.example.aicollect.data.items.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ItemEntity::class, ItemPhotoEntity::class, ValuationSearchEntity::class, PricePointEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
}
