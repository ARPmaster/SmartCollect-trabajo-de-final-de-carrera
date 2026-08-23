package com.example.aicollect.data.items.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// version 3 (2026-08-24): valoracionFuentesUrls (List<String>) renombrado/re-tipado a
// valoracionBusquedas (List<ValuationSearch>) — misma razón de siempre, sin Migration porque el
// proyecto no está en producción, fallbackToDestructiveMigration() en DatabaseModule basta.
@Database(entities = [ItemEntity::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
}
