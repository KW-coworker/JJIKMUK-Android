package com.coworker.jjikmuk.feature.scanner.data

import com.coworker.jjikmuk.feature.scanner.domain.*
import org.junit.Assert.*
import org.junit.Test

class ScanListPayloadParserTest {
    private val scanId = "d4e760c2-977a-4f06-989e-3ee1f3716268"
    private val requestId = "f360f06f-04f5-44a8-a053-6c754dc98829"
    private val row = """{"product":{"productId":2,"barcode":"NO_BARCODE_ROW_2","productName":"새우칩","manufacturer":null,"imageUrl":null,"allergy":null},"analysis":null}"""
    private fun body(rows: String = row, status: String = "MATCHES_FOUND") = """{"message":"스캔 결과","data":{"schemaVersion":"scan-list-v1","scanId":"$scanId","requestId":"$requestId","catalogVersion":"fixture-v1","status":"$status","method":"LOCAL_OCR_MATCHING","gptUsed":false,"dbAbsenceConfirmed":false,"products":[$rows]}}"""
    private fun parse(raw: String) = ScanListPayloadParser.parse(scanId,200,raw,requestId,null)
    @Test fun `nested public contract preserves barcode and response order`() {
        val second = row.replace("\"productId\":2", "\"productId\":1").replace("NO_BARCODE_ROW_2", "barcode1").replace("새우칩", "다른 이름")
        val result = parse(body("$row,$second")) as ScanListResult.Products
        assertEquals(listOf("NO_BARCODE_ROW_2","barcode1"),result.products.map { it.barcode })
        assertEquals("",result.products.first().brandName)
    }
    @Test fun `manual search is distinct from transport failure and never proves DB absence`() {
        assertTrue(parse(body("","MANUAL_SEARCH_REQUIRED")) is ScanListResult.ManualSearchRequired)
        assertTrue(parse(body("")) is ScanListResult.Failure)
    }
    @Test fun `malformed contract coerced IDs duplicate and trace mismatches are rejected`() {
        val valid=body()
        for (bad in listOf(valid.replace("\"productId\":2","\"productId\":\"2\""),
            valid.replace("\"productId\":2","\"productId\":2.5"),valid.replace("\"gptUsed\":false","\"gptUsed\":true"),
            valid.replace("\"dbAbsenceConfirmed\":false","\"dbAbsenceConfirmed\":true"),valid.replace(requestId,scanId),
            valid.replace(scanId,requestId),body("$row,$row"),valid.replace("\"analysis\":null","\"analysis\":{}"),
            valid.replace("NO_BARCODE_ROW_2",""),valid.replace("scan-list-v1","future-v2"))) {
            val failure=parse(bad) as ScanListResult.Failure
            assertEquals(OcrFailure.InvalidResponse,failure.failure.reason)
        }
    }
    @Test fun `flat service errors stay actionable and bounded`() {
        val failure=ScanListPayloadParser.parse(scanId,429,"""{"status":429,"code":"SCAN_BUSY","message":"busy"}""",requestId,"3") as ScanListResult.Failure
        assertEquals(OcrFailure.Busy,failure.failure.reason);assertEquals(3,failure.failure.retryAfterSeconds)
        val timeout=ScanListPayloadParser.parse(scanId,504,"{}",requestId,null) as ScanListResult.Failure
        assertEquals(OcrFailure.OutcomeUnknown,timeout.failure.reason)
    }
}
