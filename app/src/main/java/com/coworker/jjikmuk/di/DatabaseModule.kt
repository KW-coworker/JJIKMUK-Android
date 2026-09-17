package com.coworker.jjikmuk.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.coworker.jjikmuk.data.local.dao.ChatHistoryDao
import com.coworker.jjikmuk.data.local.dao.FamilyProfileDao
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
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideChatHistoryDao(
        database: JjikmukDatabase,
    ): ChatHistoryDao {
        return database.chatHistoryDao()
    }

    @Provides
    @Singleton
    fun provideFamilyProfileDao(
        database: JjikmukDatabase,
    ): FamilyProfileDao {
        return database.familyProfileDao()
    }

    private const val DATABASE_NAME = "jjikmuk.db"

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS family_profiles (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    emoji TEXT NOT NULL,
                    relation TEXT NOT NULL,
                    isMe INTEGER NOT NULL,
                    vegetarian TEXT NOT NULL,
                    allergies TEXT NOT NULL,
                    preferences TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }
}
