package com.coworker.jjikmuk.domain.repository

import com.coworker.jjikmuk.domain.model.FamilyProfile
import kotlinx.coroutines.flow.Flow

interface FamilyProfileRepository {
    fun observeProfiles(): Flow<List<FamilyProfile>>
    suspend fun ensureDefaultProfiles()
    suspend fun updateProfile(
        profileId: String,
        name: String,
        emoji: String,
        relation: String,
    )
    suspend fun updateDietCondition(
        profileId: String,
        vegetarian: String,
        allergies: Set<String>,
        preferences: Set<String>,
    )
    suspend fun deleteProfile(profileId: String)
}
