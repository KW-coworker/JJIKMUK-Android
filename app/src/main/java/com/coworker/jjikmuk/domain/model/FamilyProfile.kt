package com.coworker.jjikmuk.domain.model

data class FamilyProfile(
    val id: String,
    val name: String,
    val emoji: String,
    val relation: String,
    val isMe: Boolean,
    val vegetarian: String,
    val allergies: Set<String>,
    val preferences: Set<String>,
)
