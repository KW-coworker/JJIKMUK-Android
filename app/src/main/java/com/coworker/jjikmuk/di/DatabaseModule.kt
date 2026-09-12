package com.coworker.jjikmuk.di

import android.content.Context
import androidx.room.Room
import com.coworker.jjikmuk.data.local.dao.ChatHistoryDao
import com.coworker.jjikmuk.data.local.database.JjikmukDatabase
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
    fun provideJjikmukDatabase(
        @ApplicationContext context: Context,
    ): JjikmukDatabase {
        return Room.databaseBuilder(
            context,
            JjikmukDatabase::class.java,
            DATABASE_NAME,
        ).build()
    }

    @Provides
    @Singleton
    fun provideChatHistoryDao(
        database: JjikmukDatabase,
    ): ChatHistoryDao {
        return database.chatHistoryDao()
    }

    private const val DATABASE_NAME = "jjikmuk.db"
}
