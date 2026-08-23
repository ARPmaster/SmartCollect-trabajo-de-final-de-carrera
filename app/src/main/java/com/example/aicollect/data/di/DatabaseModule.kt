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
            // Room es solo caché de Firestore, nunca la fuente de verdad — perder el contenido
            // local en un bump de versión de esquema es seguro, se repuebla solo.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideItemDao(database: AppDatabase): ItemDao = database.itemDao()
}
