package com.coworker.jjikmuk.data.local.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.chatDisplayPreferences by preferencesDataStore("account_chat_display")
data class ChatDisplayOverride(val title: String? = null, val pinned: Boolean = false)

/** Device-only display overrides, isolated by account and server room ID. */
@Singleton
class ChatDisplayPreferences internal constructor(private val storage: DataStore<Preferences>) {
    @Inject constructor(@ApplicationContext context: Context) : this(context.chatDisplayPreferences)
    private fun prefix(userId: Long) = "account_${userId}_room_"
    private fun key(userId: Long, roomId: String, field: String): String {
        require(userId > 0 && (roomId.toLongOrNull() ?: 0) > 0)
        return prefix(userId) + roomId + "_" + field
    }
    fun observe(userId: Long): Flow<Map<String, ChatDisplayOverride>> = storage.data.map { prefs ->
        require(userId > 0)
        val result = mutableMapOf<String, ChatDisplayOverride>()
        prefs.asMap().forEach { (key, value) ->
            if (key.name.startsWith(prefix(userId))) {
                val suffix = key.name.removePrefix(prefix(userId))
                val roomId = suffix.substringBefore('_')
                if ((roomId.toLongOrNull() ?: 0) > 0) {
                    val before = result[roomId] ?: ChatDisplayOverride()
                    when (suffix.substringAfter('_')) {
                        "title" -> if (value is String) result[roomId] = before.copy(title = value)
                        "pinned" -> if (value is Boolean) result[roomId] = before.copy(pinned = value)
                    }
                }
            }
        }
        result
    }
    suspend fun title(userId: Long, roomId: String, title: String) {
        val normalized = title.trim()
        require(normalized.isNotEmpty() && normalized.codePointCount(0, normalized.length) <= 100 && normalized.none { it.isISOControl() })
        storage.edit { it[stringPreferencesKey(key(userId, roomId, "title"))] = normalized }
    }
    suspend fun pin(userId: Long, roomId: String, pinned: Boolean) {
        storage.edit { it[booleanPreferencesKey(key(userId, roomId, "pinned"))] = pinned }
    }
    suspend fun remove(userId: Long, roomId: String) {
        storage.edit {
            it.remove(stringPreferencesKey(key(userId, roomId, "title")))
            it.remove(booleanPreferencesKey(key(userId, roomId, "pinned")))
        }
    }
}
