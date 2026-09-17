package com.coworker.jjikmuk.data.local.preference

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.productSearchDataStore by preferencesDataStore(
    name = "product_search_preferences",
)

@Singleton
class RecentSearchKeywordStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val gson = Gson()

    val keywords: Flow<List<String>> = context.productSearchDataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { preferences ->
            decodeKeywords(preferences[RECENT_SEARCH_KEYWORDS].orEmpty())
        }

    suspend fun addKeyword(keyword: String) {
        val trimmedKeyword = keyword.trim()
        if (trimmedKeyword.length < MIN_KEYWORD_LENGTH) return

        context.productSearchDataStore.edit { preferences ->
            val currentKeywords = decodeKeywords(preferences[RECENT_SEARCH_KEYWORDS].orEmpty())
            val updatedKeywords = buildList {
                add(trimmedKeyword)
                addAll(currentKeywords.filterNot { it == trimmedKeyword })
            }.take(MAX_KEYWORD_COUNT)

            preferences[RECENT_SEARCH_KEYWORDS] = gson.toJson(updatedKeywords)
        }
    }

    suspend fun deleteKeyword(keyword: String) {
        context.productSearchDataStore.edit { preferences ->
            val currentKeywords = decodeKeywords(preferences[RECENT_SEARCH_KEYWORDS].orEmpty())
            preferences[RECENT_SEARCH_KEYWORDS] = gson.toJson(
                currentKeywords.filterNot { it == keyword },
            )
        }
    }

    suspend fun clearKeywords() {
        context.productSearchDataStore.edit { preferences ->
            preferences.remove(RECENT_SEARCH_KEYWORDS)
        }
    }

    private fun decodeKeywords(rawKeywords: String): List<String> {
        if (rawKeywords.isBlank()) return emptyList()

        return runCatching {
            gson.fromJson(rawKeywords, Array<String>::class.java).toList()
        }.getOrDefault(emptyList())
            .map(String::trim)
            .filter { it.length >= MIN_KEYWORD_LENGTH }
            .distinct()
            .take(MAX_KEYWORD_COUNT)
    }

    private companion object {
        val RECENT_SEARCH_KEYWORDS = stringPreferencesKey("recent_search_keywords")
        const val MIN_KEYWORD_LENGTH = 2
        const val MAX_KEYWORD_COUNT = 8
    }
}
