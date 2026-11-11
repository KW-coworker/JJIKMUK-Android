package com.coworker.jjikmuk.domain.repository

import com.coworker.jjikmuk.domain.model.FamilyProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class FamilyAccount(val userId: Long, val generation: Long)
data class FamilyProfilesState(val account: FamilyAccount? = null, val profiles: List<FamilyProfile> = emptyList(),
    val loaded: Boolean = true, val busy: Boolean = false, val needsReload: Boolean = false, val message: String? = null)

interface FamilyProfileRepository {
    fun observeProfiles(): Flow<List<FamilyProfile>>
    fun observeState(): Flow<FamilyProfilesState> = observeProfiles().map { FamilyProfilesState(profiles = it) }
    fun captureAccount(): FamilyAccount? = null
    fun currentState(): FamilyProfilesState? = null
    suspend fun ensureDefaultProfiles()
    suspend fun refresh(account: FamilyAccount? = captureAccount()) { ensureDefaultProfiles() }
    suspend fun addProfile(name: String, emoji: String, relation: String, account: FamilyAccount? = captureAccount()): Boolean
    suspend fun updateProfile(profileId: String, name: String, emoji: String, relation: String,
        version: Long? = null, account: FamilyAccount? = captureAccount()): Boolean
    suspend fun updateDietCondition(profileId: String, vegetarian: String, allergies: Set<String>, preferences: Set<String>,
        version: Long? = null, account: FamilyAccount? = captureAccount()): Boolean
    suspend fun deleteProfile(profileId: String, version: Long? = null, account: FamilyAccount? = captureAccount()): Boolean
}
