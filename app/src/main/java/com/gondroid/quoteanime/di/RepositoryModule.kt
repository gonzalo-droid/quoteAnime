package com.gondroid.quoteanime.di

import com.gondroid.quoteanime.data.repository.QuoteRepositoryImpl
import com.gondroid.quoteanime.data.repository.UserPreferencesRepositoryImpl
import com.gondroid.quoteanime.domain.repository.QuoteRepository
import com.gondroid.quoteanime.domain.repository.UserPreferencesRepository
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
    abstract fun bindQuoteRepository(impl: QuoteRepositoryImpl): QuoteRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository
}
