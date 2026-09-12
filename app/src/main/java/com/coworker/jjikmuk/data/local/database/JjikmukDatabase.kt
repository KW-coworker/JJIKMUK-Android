package com.coworker.jjikmuk.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.coworker.jjikmuk.data.local.dao.ChatHistoryDao
import com.coworker.jjikmuk.data.local.entity.ChatConversationEntity
import com.coworker.jjikmuk.data.local.entity.ChatMessageEntity

@Database(
    entities = [
        ChatConversationEntity::class,
        ChatMessageEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class JjikmukDatabase : RoomDatabase() {
    abstract fun chatHistoryDao(): ChatHistoryDao
}
