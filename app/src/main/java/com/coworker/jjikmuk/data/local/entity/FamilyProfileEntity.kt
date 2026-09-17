package com.coworker.jjikmuk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_profiles")
data class FamilyProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String,
    val relation: String,
    val isMe: Boolean,
    val vegetarian: String,
    val allergies: String,
    val preferences: String,
    val createdAt: Long,
    val updatedAt: Long,
)
