package com.coworker.jjikmuk.feature.scanner.data

import com.coworker.jjikmuk.feature.scanner.domain.*
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

internal object OcrResponseParser {
    fun parse(status: Int, body: String?, relayId: String?, retryAfter: String?): OcrResult {
        val root = try { JsonParser.parseString(body.orEmpty()).asJsonObject } catch (_: Exception) { null }
        if (status != 200) {
            val error = try { root?.getAsJsonObject("error") } catch (_: Exception) { null }
            val code = runCatching { error?.text("code") }.getOrNull()
            val trace = OcrTrace(relayId ?: runCatching { error?.text("requestId") }.getOrNull())
            val reason = when (status) {
                401 -> OcrFailure.Unauthorized
                413, 415, 422 -> OcrFailure.InvalidImage
                429 -> if (code == "RATE_LIMITED") OcrFailure.RateLimited else OcrFailure.Busy
                503 -> OcrFailure.ServiceUnavailable
                408, 504 -> OcrFailure.OutcomeUnknown
                else -> if (code == "INVALID_OCR_RESPONSE") OcrFailure.InvalidResponse else OcrFailure.Rejected
            }
            val retrySeconds = if (status == 429 && code in setOf("OCR_BUSY", "RATE_LIMITED")) {
                retryAfter?.toIntOrNull()?.takeIf { it >= 0 }
            } else null
            return OcrResult.Failure(reason, status, code, trace, retrySeconds)
        }
        return try {
            val data = requireNotNull(root?.getAsJsonObject("data"))
            val trace = OcrTrace(relayId, data.text("requestId"))
            val extraction = OcrExtraction(
                data.text("productName"), data.text("brand"), data.text("flavor"),
                data.text("totalWeight"), data.text("searchKeyword"),
                data.get("warnings").nullable()?.asJsonArray?.map { requireNotNull(it.stringOrNull()) } ?: emptyList(),
                data.get("evidence").nullable()?.asJsonObject?.entrySet()?.associate { (key, value) ->
                    key to value.asJsonArray.map { item ->
                        OcrEvidence(item.asJsonObject.text("sourceId"), item.asJsonObject.text("text"))
                    }
                } ?: emptyMap(),
            )
            when (data.text("status")) {
                "OK" -> {
                    val keyword = requireNotNull(extraction.searchKeyword)
                    require(keyword.length in 2..100 && keyword.count { it.isLetterOrDigit() } >= 2)
                    OcrResult.Extracted(extraction, trace)
                }
                "NO_TEXT" -> OcrResult.NoText(trace)
                "NO_SEARCH_KEYWORD" -> OcrResult.NoSearchKeyword(extraction, trace)
                else -> error("Unknown OCR status")
            }
        } catch (_: Exception) {
            OcrResult.Failure(OcrFailure.InvalidResponse, status, trace = OcrTrace(relayId))
        }
    }

    private fun JsonElement?.nullable() = this?.takeUnless { it.isJsonNull }
    private fun JsonElement?.stringOrNull(): String? = nullable()?.let {
        require(it.isJsonPrimitive && it.asJsonPrimitive.isString)
        it.asString
    }
    private fun JsonObject.text(name: String) = get(name).stringOrNull()
}
