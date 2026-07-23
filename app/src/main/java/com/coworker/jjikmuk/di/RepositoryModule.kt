package com.coworker.jjikmuk.di

import com.coworker.jjikmuk.data.repository.ChatRepositoryImpl
import com.coworker.jjikmuk.domain.repository.ChatRepository
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
    abstract fun bindChatRepository(
        impl: ChatRepositoryImpl,
    ): ChatRepository
}
