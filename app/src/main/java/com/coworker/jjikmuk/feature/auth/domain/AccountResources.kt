package com.coworker.jjikmuk.feature.auth.domain

/** Full submission, unlike PATCH: absent optional conditions are cleared by the server. */
class ProfileSubmission(val nickname: String, val allergies: String? = null,
    val specialDiet: String? = null, val diseases: String? = null, val dislikedIngredients: String? = null)

data class FamilyProfile(val id: Long, val nickname: String, val allergies: String?,
    val specialDiet: String?, val diseases: String?, val dislikedIngredients: String?, val version: Long)

/** Kept in memory only. Never use the shared /me/photo URL as an image-cache key. */
class ProfilePhoto(val bytes: ByteArray, val contentType: String) {
    companion object { const val MAX_BYTES = 2 * 1024 * 1024 }
}
data class PhotoUploadReceipt(val photoUrl: String, val authenticationRequired: Boolean)
