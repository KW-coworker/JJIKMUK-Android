package com.coworker.jjikmuk.feature.scanner.data

import com.coworker.jjikmuk.domain.model.ProductSearchResult
import com.coworker.jjikmuk.feature.scanner.domain.*
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.UUID

/** Strict new contract. Malformed results never become a successful empty list. */
internal object ScanListPayloadParser {
    fun parse(scanId: String, status: Int, raw: String?, requestId: String?, retryAfter: String?): ScanListResult {
        val trace = OcrTrace(relayRequestId = requestId?.takeIf { runCatching { UUID.fromString(it).toString() == it }.getOrDefault(false) })
        fun failure(reason: OcrFailure, code: String? = null) = ScanListResult.Failure(
            OcrResult.Failure(reason, status, code, trace, retryAfter?.toIntOrNull()?.takeIf { it in 1..300 }))
        if (status != 200) {
            val code = runCatching { JsonParser.parseString(raw).asJsonObject["code"].asString }
                .getOrNull()?.takeIf { it.matches(Regex("[A-Z_]{1,80}")) }
            return failure(when (status) {
                401 -> OcrFailure.Unauthorized
                429 -> if (code == "RATE_LIMITED") OcrFailure.RateLimited else OcrFailure.Busy
                409, 504 -> OcrFailure.OutcomeUnknown
                502, 503 -> OcrFailure.ServiceUnavailable
                400, 413, 415, 422 -> OcrFailure.InvalidImage
                else -> OcrFailure.Rejected
            }, code)
        }
        return try {
            require(raw != null && raw.toByteArray(Charsets.UTF_8).size <= 1024 * 1024 && trace.relayRequestId != null)
            val root = JsonParser.parseString(raw).asJsonObject
            text(root, "message", 200)
            val data = root["data"].asJsonObject
            require(text(data, "schemaVersion", 30) == "scan-list-v1")
            require(text(data, "scanId", 36) == scanId && text(data, "requestId", 36) == trace.relayRequestId)
            text(data, "catalogVersion", 200)
            val state = text(data, "status", 40)
            require(state in setOf("MATCHES_FOUND", "MANUAL_SEARCH_REQUIRED"))
            val method = text(data, "method", 40)
            require(method in setOf("LOCAL_OCR_MATCHING", "IMAGE_GPT_MATCHING"))
            require(bool(data, "gptUsed") == (method == "IMAGE_GPT_MATCHING"))
            require(!bool(data, "dbAbsenceConfirmed"))
            val rows = data["products"].asJsonArray
            require(rows.size() <= 50 && (state == "MATCHES_FOUND") == (rows.size() > 0))
            val ids = mutableSetOf<Long>(); val barcodes = mutableSetOf<String>()
            val products = rows.map { element ->
                val row = element.asJsonObject
                require(row.has("analysis") && row["analysis"].isJsonNull)
                val p = row["product"].asJsonObject
                val id = p["productId"]
                require(id.isJsonPrimitive && id.asJsonPrimitive.isNumber && id.toString().matches(Regex("[1-9][0-9]{0,18}")))
                require(ids.add(id.asString.toLong()))
                val barcode = text(p, "barcode", 255)
                require(barcodes.add(barcode))
                val name = text(p, "productName", 1000)
                val manufacturer = nullableText(p, "manufacturer", 1000)
                val image = nullableText(p, "imageUrl", 2000)
                val allergy = nullableText(p, "allergy", 100000)
                ProductSearchResult(barcode, name, manufacturer.orEmpty(), image,
                    allergy?.split(',', '|')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty())
            }
            if (state == "MATCHES_FOUND") ScanListResult.Products(products, trace)
            else ScanListResult.ManualSearchRequired(trace)
        } catch (_: Exception) { failure(OcrFailure.InvalidResponse) }
    }
    private fun text(o: JsonObject, key: String, max: Int): String {
        val value = o[key]
        require(value?.isJsonPrimitive == true && value.asJsonPrimitive.isString)
        return value.asString.also { require(it.isNotBlank() && it.length <= max) }
    }
    private fun nullableText(o: JsonObject, key: String, max: Int): String? {
        require(o.has(key))
        val value = o[key]
        if (value.isJsonNull) return null
        require(value.isJsonPrimitive && value.asJsonPrimitive.isString && value.asString.length <= max)
        return value.asString
    }
    private fun bool(o: JsonObject, key: String): Boolean {
        val value = o[key]
        require(value?.isJsonPrimitive == true && value.asJsonPrimitive.isBoolean)
        return value.asBoolean
    }
}
