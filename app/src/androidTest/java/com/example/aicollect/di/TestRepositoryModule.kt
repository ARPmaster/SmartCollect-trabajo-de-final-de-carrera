// Módulo de Hilt para las pruebas de instrumentación: sustituye el módulo de repositorios real por dobles en memoria, así ningún test toca Firebase.
package com.example.aicollect.di

import com.example.aicollect.application.auth.AuthRepository
import com.example.aicollect.application.items.ItemRepository
import com.example.aicollect.application.recognition.RecognitionRepository
import com.example.aicollect.data.di.RepositoryModule
import com.example.aicollect.testutil.FakeAuthRepository
import com.example.aicollect.testutil.FakeItemRepository
import com.example.aicollect.testutil.FakeRecognitionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [RepositoryModule::class])
abstract class TestRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FakeAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindItemRepository(impl: FakeItemRepository): ItemRepository

    @Binds
    @Singleton
    abstract fun bindRecognitionRepository(impl: FakeRecognitionRepository): RecognitionRepository
}
