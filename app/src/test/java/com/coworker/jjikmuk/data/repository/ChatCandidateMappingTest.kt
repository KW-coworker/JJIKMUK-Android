package com.coworker.jjikmuk.data.repository

import com.google.gson.Gson
import com.coworker.jjikmuk.data.remote.dto.ChatApiResponse
import org.junit.Assert.*
import org.junit.Test

class ChatCandidateMappingTest {
    @Test fun deployedCandidatesAreShownWithManufacturer() {
        val response = Gson().fromJson("""{"answer":"선택해 주세요","product_candidates":[
            {"product_name":"포키","manufacturer_name":"해태","barcode":"8801019309830","allergy":null}],
            "recommended_products":[]}""", ChatApiResponse::class.java)
        val products = response.toProductCandidates()
        assertEquals(1, products.size)
        assertEquals("포키", products.single().productName)
        assertEquals("해태", products.single().brandName)
        assertEquals("8801019309830", products.single().barcode)
        assertTrue(products.single().allergyLabels.isEmpty())
    }

    @Test fun legacyCandidatesRemainSupportedAndRecommendationsAreExcluded() {
        val response = Gson().fromJson("""{"answer":"선택","recommended_products":[
            {"product_name":"포키","selection_type":"product_candidate"},
            {"product_name":"대체상품","selection_type":"recommendation"}]}""", ChatApiResponse::class.java)
        assertEquals(listOf("포키"), response.toProductCandidates().map { it.productName })
    }

    @Test fun duplicateAndNamelessCandidatesAreRemoved() {
        val response = Gson().fromJson("""{"answer":"선택","product_candidates":[
            {"product_name":"포키","barcode":"123"},{}],"recommended_products":[
            {"product_name":"포키","barcode":"123","selection_type":"product_candidate"}]}""", ChatApiResponse::class.java)
        assertEquals(1, response.toProductCandidates().size)
    }
}
