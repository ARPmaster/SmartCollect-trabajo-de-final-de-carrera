// Módulo Hilt que provee la base de datos Room y su DAO de ítems.
package com.example.aicollect.data.di

import android.content.Context
import androidx.room.Room
import com.example.aicollect.data.items.local.AppDatabase
import com.example.aicollect.data.items.local.ItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "aicollect.db")
            // Estrategia deliberada: Room es únicamente caché de lectura de Firestore (fuente de verdad).
            // Ante un cambio de esquema se descarta la copia local y se re-sincroniza desde el servidor,
            // lo que evita mantener migraciones para datos que se pueden reconstruir íntegramente.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideItemDao(database: AppDatabase): ItemDao = database.itemDao()
}
