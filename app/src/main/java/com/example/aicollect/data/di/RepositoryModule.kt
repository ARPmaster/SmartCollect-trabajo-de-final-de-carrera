// Módulo Hilt que enlaza cada interfaz de repositorio de la capa de dominio con su implementación concreta.
package com.example.aicollect.data.di

import com.example.aicollect.application.auth.AuthRepository
import com.example.aicollect.application.items.ItemRepository
import com.example.aicollect.application.recognition.RecognitionRepository
import com.example.aicollect.data.auth.FirebaseAuthRepository
import com.example.aicollect.data.items.FirestoreItemRepository
import com.example.aicollect.data.recognition.FirebaseRecognitionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindItemRepository(impl: FirestoreItemRepository): ItemRepository

    @Binds
    @Singleton
    abstract fun bindRecognitionRepository(impl: FirebaseRecognitionRepository): RecognitionRepository
}
