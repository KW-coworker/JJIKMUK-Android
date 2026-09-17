package com.coworker.jjikmuk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.coworker.jjikmuk.data.local.entity.FamilyProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyProfileDao {

    @Query(
        """
        SELECT *
        FROM family_profiles
        ORDER BY isMe DESC, createdAt ASC
        """,
    )
    fun observeProfiles(): Flow<List<FamilyProfileEntity>>

    @Query("SELECT COUNT(*) FROM family_profiles")
    suspend fun countProfiles(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfiles(profiles: List<FamilyProfileEntity>)

    @Query(
        """
        UPDATE family_profiles
        SET
            name = :name,
            emoji = :emoji,
            relation = :relation,
            updatedAt = :updatedAt
        WHERE id = :profileId
        """,
    )
    suspend fun updateProfile(
        profileId: String,
        name: String,
        emoji: String,
        relation: String,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE family_profiles
        SET
            vegetarian = :vegetarian,
            allergies = :allergies,
            preferences = :preferences,
            updatedAt = :updatedAt
        WHERE id = :profileId
        """,
    )
    suspend fun updateDietCondition(
        profileId: String,
        vegetarian: String,
        allergies: String,
        preferences: String,
        updatedAt: Long,
    )

    @Query("DELETE FROM family_profiles WHERE id = :profileId AND isMe = 0")
    suspend fun deleteProfile(profileId: String)
}
