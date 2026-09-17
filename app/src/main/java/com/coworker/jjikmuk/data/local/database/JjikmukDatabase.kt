package com.coworker.jjikmuk.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.coworker.jjikmuk.data.local.dao.ChatHistoryDao
import com.coworker.jjikmuk.data.local.dao.FamilyProfileDao
import com.coworker.jjikmuk.data.local.entity.ChatConversationEntity
import com.coworker.jjikmuk.data.local.entity.ChatMessageEntity
import com.coworker.jjikmuk.data.local.entity.FamilyProfileEntity

@Database(
    entities = [
        ChatConversationEntity::class,
        ChatMessageEntity::class,
        FamilyProfileEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class JjikmukDatabase : RoomDatabase() {
    abstract fun chatHistoryDao(): ChatHistoryDao
    abstract fun familyProfileDao(): FamilyProfileDao
}
