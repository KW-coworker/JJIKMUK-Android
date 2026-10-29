package com.coworker.jjikmuk.feature.auth.data

import com.coworker.jjikmuk.feature.auth.domain.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/** Only PATCH preserves explicit nulls. Other request serializers retain their existing behavior. */
internal fun authJson(): Gson = GsonBuilder()
    .registerTypeAdapter(ProfilePatch::class.java, ProfilePatchAdapter())
    .create()

private class ProfilePatchAdapter : TypeAdapter<ProfilePatch>() {
    override fun write(out: JsonWriter, value: ProfilePatch) {
        val previous = out.serializeNulls
        out.serializeNulls = true
        try {
            out.beginObject()
            value.nickname?.let { out.name("nickname").value(normalizeAuthNickname(it)) }
            out.field("allergies", value.allergies)
            out.field("diseases", value.diseases)
            out.field("specialDiet", value.specialDiet)
            out.field("dislikedIngredients", value.dislikedIngredients)
            out.endObject()
        } finally { out.serializeNulls = previous }
    }

    private fun JsonWriter.field(name: String, value: ProfileField) {
        when (value) {
            ProfileField.Unchanged -> Unit
            ProfileField.Clear -> name(name).nullValue()
            is ProfileField.Value -> name(name).value(value.value)
        }
    }

    override fun read(input: JsonReader): ProfilePatch = error("ProfilePatch is a request-only model")
}
