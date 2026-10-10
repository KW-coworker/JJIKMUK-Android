package com.coworker.jjikmuk.feature.scanner.data

import com.coworker.jjikmuk.feature.scanner.domain.*
import org.junit.Assert.*
import org.junit.Test

class OcrResponseParserTest {
    @Test fun `keyword success preserves nullable extraction and both distinct trace IDs`() {
        val result = OcrResponseParser.parse(200, """{"data":{"requestId":"ocr-1","status":"OK","productName":"어성초","searchKeyword":"어성초","brand":null,"totalWeight":"500ml","warnings":[],"evidence":{}}}""", "relay-1", null) as OcrResult.Extracted
        assertEquals("500ml", result.extraction.totalWeight)
        assertNull(result.extraction.brand)
        assertEquals(OcrTrace("relay-1", "ocr-1"), result.trace)
        assertTrue(result.extraction.evidence.isEmpty())
    }

    @Test fun `no text and no keyword are business states even for HTTP 200`() {
        assertTrue(parse("""{"data":{"status":"NO_TEXT","evidence":{}}}""") is OcrResult.NoText)
        val result = parse("""{"data":{"status":"NO_SEARCH_KEYWORD","brand":"광동"}}""") as OcrResult.NoSearchKeyword
        assertEquals("광동", result.extraction.brand)
    }

    @Test fun `invalid success must not manufacture a keyword or product`() {
        for (body in listOf(
            "<html>proxy failure</html>", "", """{"data":[]}""",
            """{"data":{"status":"NEW_STATUS"}}""",
            """{"data":{"status":"OK","productName":"상품"}}""",
            """{"data":{"status":"OK","searchKeyword":" "}}""",
            """{"data":{"status":"OK","searchKeyword":"!!"}}""",
            """{"data":{"status":"OK","searchKeyword":1234}}""",
        )) assertEquals(OcrFailure.InvalidResponse, (parse(body) as OcrResult.Failure).reason)
    }

    @Test fun `HTTP status matters when upstream code is shared and body is absent`() {
        val expected = mapOf(401 to OcrFailure.Unauthorized, 413 to OcrFailure.InvalidImage,
            415 to OcrFailure.InvalidImage, 422 to OcrFailure.InvalidImage, 429 to OcrFailure.Busy,
            503 to OcrFailure.ServiceUnavailable, 504 to OcrFailure.OutcomeUnknown)
        expected.forEach { (status, reason) ->
            for (body in listOf(null, "<html>error</html>", """{"error":{"code":"OCR_UPSTREAM_REJECTED"}}""")) {
                assertEquals(reason, (OcrResponseParser.parse(status, body, "relay", null) as OcrResult.Failure).reason)
            }
        }
    }

    @Test fun `retry hint is retained only for relay rate limits`() {
        val rate = OcrResponseParser.parse(429, """{"error":{"code":"RATE_LIMITED","requestId":"relay"}}""", null, "3") as OcrResult.Failure
        assertEquals(OcrFailure.RateLimited, rate.reason)
        assertEquals(3, rate.retryAfterSeconds)
        assertEquals("relay", rate.trace.relayRequestId)
        assertNull((OcrResponseParser.parse(429, "{}", null, "3") as OcrResult.Failure).retryAfterSeconds)
    }

    private fun parse(body: String) = OcrResponseParser.parse(200, body, null, null)
}
