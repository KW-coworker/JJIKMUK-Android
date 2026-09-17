package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.local.dao.FamilyProfileDao
import com.coworker.jjikmuk.data.local.entity.FamilyProfileEntity
import com.coworker.jjikmuk.domain.model.FamilyProfile
import com.coworker.jjikmuk.domain.repository.FamilyProfileRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FamilyProfileRepositoryImpl @Inject constructor(
    private val familyProfileDao: FamilyProfileDao,
) : FamilyProfileRepository {

    override fun observeProfiles(): Flow<List<FamilyProfile>> {
        return familyProfileDao.observeProfiles().map { profiles ->
            profiles.map { profile -> profile.toDomain() }
        }
    }

    override suspend fun ensureDefaultProfiles() {
        if (familyProfileDao.countProfiles() > 0) return

        val now = System.currentTimeMillis()
        familyProfileDao.upsertProfiles(
            listOf(
                FamilyProfileEntity(
                    id = "me",
                    name = "나",
                    emoji = "🙂",
                    relation = "본인",
                    isMe = true,
                    vegetarian = "해당 없음",
                    allergies = "복숭아|땅콩|소고기",
                    preferences = "저염|글루텐프리",
                    createdAt = now,
                    updatedAt = now,
                ),
                FamilyProfileEntity(
                    id = "dad",
                    name = "아빠",
                    emoji = "👨🏻",
                    relation = "아빠",
                    isMe = false,
                    vegetarian = "락토",
                    allergies = "새우|게",
                    preferences = "저당|고단백",
                    createdAt = now + 1,
                    updatedAt = now + 1,
                ),
                FamilyProfileEntity(
                    id = "baby",
                    name = "아기",
                    emoji = "👶🏻",
                    relation = "기타",
                    isMe = false,
                    vegetarian = "해당 없음",
                    allergies = "토마토|밀가루",
                    preferences = "저염",
                    createdAt = now + 2,
                    updatedAt = now + 2,
                ),
            ),
        )
    }

    override suspend fun updateProfile(
        profileId: String,
        name: String,
        emoji: String,
        relation: String,
    ) {
        familyProfileDao.updateProfile(
            profileId = profileId,
            name = name,
            emoji = emoji,
            relation = relation,
            updatedAt = System.currentTimeMillis(),
        )
    }

    override suspend fun updateDietCondition(
        profileId: String,
        vegetarian: String,
        allergies: Set<String>,
        preferences: Set<String>,
    ) {
        familyProfileDao.updateDietCondition(
            profileId = profileId,
            vegetarian = vegetarian,
            allergies = allergies.toStoredValues(),
            preferences = preferences.toStoredValues(),
            updatedAt = System.currentTimeMillis(),
        )
    }

    override suspend fun deleteProfile(profileId: String) {
        familyProfileDao.deleteProfile(profileId)
    }

    private fun FamilyProfileEntity.toDomain(): FamilyProfile {
        return FamilyProfile(
            id = id,
            name = name,
            emoji = emoji,
            relation = relation,
            isMe = isMe,
            vegetarian = vegetarian,
            allergies = allergies.toSetValues(),
            preferences = preferences.toSetValues(),
        )
    }

    private fun String.toSetValues(): Set<String> {
        return split("|")
            .map { value -> value.trim() }
            .filter { value -> value.isNotEmpty() }
            .toSet()
    }

    private fun Set<String>.toStoredValues(): String {
        return joinToString(separator = "|") { value -> value.trim() }
    }
}
