package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.remote.api.ProductApi
import com.coworker.jjikmuk.data.remote.dto.*
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ProductRepositoryImplTest {
    @Test fun `search failure stays a failure even for a formerly sampled product`() = runTest {
        val error = IOException("offline")
        val api = FakeApi(error = error)
        val result = ProductRepositoryImpl(api).searchProducts("포키", emptyList())
        assertSame(error, result.exceptionOrNull())
        assertEquals(1, api.calls)
    }

    @Test fun `detail failure is never replaced by a sample`() = runTest {
        val error = IOException("offline")
        val result = ProductRepositoryImpl(FakeApi(error = error)).getProductDetail("8801111111111", emptyList())
        assertSame(error, result.exceptionOrNull())
    }

    @Test fun `successful empty search stays an empty success`() = runTest {
        val result = ProductRepositoryImpl(FakeApi()).searchProducts("포키", emptyList())
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test fun `server product is mapped without substituting local data`() = runTest {
        val api = FakeApi(products = listOf(ProductSearchItemDto(product = ProductDto(
            barcode = "server-id", productName = "서버 상품", manufacturer = "서버 제조사"))))
        val product = ProductRepositoryImpl(api).searchProducts("서버", listOf("wheat")).getOrThrow().single()
        assertEquals("server-id", product.barcode)
        assertEquals("서버 상품", product.productName)
        assertTrue(api.allergies.isEmpty())
    }

    @Test fun `cancellation propagates from search and detail`() = runTest {
        val cancellation = CancellationException("cancelled")
        val repository = ProductRepositoryImpl(FakeApi(error = cancellation))
        val requests: List<suspend () -> Any> = listOf(
            { repository.searchProducts("포키", emptyList()) },
            { repository.getProductDetail("id", emptyList()) },
        )
        for (request in requests) {
            try {
                request()
                fail("Cancellation must not be wrapped as a server failure")
            } catch (actual: CancellationException) {
                assertSame(cancellation, actual)
            }
        }
    }

    @Test fun `detail preserves stable server id and id lookup verifies identity`() = runTest {
        val detail = ProductDetailApiResponse(data = ProductDetailDataDto(product = ProductDto(
            productId = 123, barcode = "880", productName = "서버 상품")))
        val repository = ProductRepositoryImpl(FakeApi(detail = detail))
        assertEquals(123L, repository.getProductDetail("880").getOrThrow().productId)
        assertEquals(123L, repository.getProductDetailById(123).getOrThrow().productId)
        assertTrue(repository.getProductDetailById(456).isFailure)
    }

    @Test fun `wire package nutrients override legacy and per100g without another conversion`() {
        val data = com.google.gson.Gson().fromJson("""{
          "product": {"productName":"포키 녹차","totalWeight":"44g","energyKcal":500,
            "proteinG":10,"nutrition":{
              "packageCalculationStatus":"CALCULATED",
              "per100g":{"amount":100,"unit":"g","values":{"energyKcal":500,"proteinG":10}},
              "perPackage":{"amount":44,"unit":"g","values":{
                "energyKcal":220,"proteinG":4.4,"carbsG":30.8,"fatG":8.8,
                "sugarG":15.4,"sodiumMg":150,"cholesterolMg":3}}
          }}}
        """, ProductDetailDataDto::class.java)
        val product = ProductRepositoryImpl(FakeApi()).mapDetail(data)
        assertEquals("44g",product.totalWeight)
        assertEquals(220.0,product.nutrition.energyKcal!!,0.001)
        assertEquals(4.4,product.nutrition.proteinG!!,0.001)
        assertEquals(30.8,product.nutrition.carbsG!!,0.001)
        assertEquals(8.8,product.nutrition.fatG!!,0.001)
        assertEquals(15.4,product.nutrition.sugarG!!,0.001)
        assertEquals(150.0,product.nutrition.sodiumMg!!,0.001)
        assertEquals(3.0,product.nutrition.cholesterolMg!!,0.001)
        assertTrue(product.nutrition.packageBasisKnown)
        assertEquals("총열량",product.nutrition.energyLabel)
    }

    @Test fun `missing package values use explicitly provided per100g with basis label`() {
        val product=ProductDto(productName="상품",energyKcal=999.0,
            nutrition=ProductNutritionDto(packageCalculationStatus="UNAVAILABLE",
                per100g=ProductNutritionPortionDto(100.0,"g",ProductNutrientValuesDto(energyKcal=500.0,proteinG=10.0))))
        val nutrition=ProductRepositoryImpl(FakeApi()).mapDetail(ProductDetailDataDto(product=product)).nutrition
        assertEquals("100g 기준",nutrition.basisLabel)
        assertEquals("100g 기준 열량",nutrition.energyLabel)
        assertFalse(nutrition.packageBasisKnown)
        assertEquals(500.0,nutrition.energyKcal!!,0.001)
        assertEquals(10.0,nutrition.proteinG!!,0.001)
    }

    @Test fun `API confirmed missing basis defaults to per100g and scales all nutrients to 44g`() {
        val source=ProductNutrientValuesDto(500.0,70.45,6.82,20.45,36.36,341.0,6.82)
        for (contract in listOf(null,ProductNutritionDto(sourceBasis=ProductNutritionBasisDto("UNKNOWN"),
            sourceValues=source,packageQuantity=ProductNutritionQuantityDto(44.0,"g"),packageCalculationStatus="UNAVAILABLE"))) {
            val product=ProductDto(productName="포키 녹차",totalWeight="44g",energyKcal=500.0,
                carbsG=70.45,proteinG=6.82,fatG=20.45,sugarG=36.36,sodiumMg=341.0,cholesterolMg=6.82,nutrition=contract)
            val mapped=ProductRepositoryImpl(FakeApi()).mapDetail(ProductDetailDataDto(product=product)).nutrition
            assertTrue(mapped.packageBasisKnown)
            assertEquals(220.0,mapped.energyKcal!!,0.001)
            assertEquals(30.998,mapped.carbsG!!,0.001)
            assertEquals(3.0008,mapped.proteinG!!,0.001)
            assertEquals(8.998,mapped.fatG!!,0.001)
            assertEquals(15.9984,mapped.sugarG!!,0.001)
            assertEquals(150.04,mapped.sodiumMg!!,0.001)
            assertEquals(3.0008,mapped.cholesterolMg!!,0.001)
        }
    }

    @Test fun `missing weight keeps legacy values clearly labeled per100g`() {
        val product=ProductDto(productName="상품",energyKcal=500.0,proteinG=10.0)
        val mapped=ProductRepositoryImpl(FakeApi()).mapDetail(ProductDetailDataDto(product=product)).nutrition
        assertEquals("100g 기준",mapped.basisLabel)
        assertEquals(500.0,mapped.energyKcal!!,0.001)
        assertFalse(mapped.packageBasisKnown)
    }

    @Test fun `explicit known basis does not fall back to default100g`() {
        val product=ProductDto(productName="상품",totalWeight="44g",energyKcal=220.0,
            nutrition=ProductNutritionDto(sourceBasis=ProductNutritionBasisDto("KNOWN"),packageCalculationStatus="UNAVAILABLE"))
        val mapped=ProductRepositoryImpl(FakeApi()).mapDetail(ProductDetailDataDto(product=product)).nutrition
        assertEquals("영양정보 기준량 확인 불가",mapped.basisLabel)
        assertNull(mapped.energyKcal)
    }

    @Test fun `server parsed multipack quantity scales per100g and does not reuse one bag weight`() {
        val product=ProductDto(productName="묶음",totalWeight="44g x 4봉",energyKcal=500.0,
            nutrition=ProductNutritionDto(packageQuantity=ProductNutritionQuantityDto(176.0,"g")))
        val mapped=ProductRepositoryImpl(FakeApi()).mapDetail(ProductDetailDataDto(product=product)).nutrition
        assertTrue(mapped.packageBasisKnown)
        assertEquals(880.0,mapped.energyKcal!!,0.001)
    }

    @Test fun `missing package nutrient is not filled from per100g and zero remains zero`() {
        val product=ProductDto(productName="상품",nutrition=ProductNutritionDto(packageCalculationStatus="CALCULATED",
            perPackage=ProductNutritionPortionDto(44.0,"g",ProductNutrientValuesDto(energyKcal=null,sugarG=0.0,fatG=-1.0)),
            per100g=ProductNutritionPortionDto(100.0,"g",ProductNutrientValuesDto(energyKcal=500.0))))
        val mapped=ProductRepositoryImpl(FakeApi()).mapDetail(ProductDetailDataDto(product=product)).nutrition
        assertTrue(mapped.packageBasisKnown)
        assertNull(mapped.energyKcal);assertNull(mapped.fatG)
        assertEquals(0.0,mapped.sugarG!!,0.001)
    }

    @Test fun `liquid fallback is explicitly per100ml rather than per100g`() {
        val product=ProductDto(productName="음료",nutrition=ProductNutritionDto(
            per100ml=ProductNutritionPortionDto(100.0,"ml",ProductNutrientValuesDto(energyKcal=40.0))))
        val mapped=ProductRepositoryImpl(FakeApi()).mapDetail(ProductDetailDataDto(product=product)).nutrition
        assertEquals("100ml 기준",mapped.basisLabel)
        assertEquals("100ml 기준 열량",mapped.energyLabel)
        assertEquals(40.0,mapped.energyKcal!!,0.001)
    }

    private class FakeApi(
        private val error: Exception? = null,
        private val products: List<ProductSearchItemDto> = emptyList(),
        private val detail: ProductDetailApiResponse = ProductDetailApiResponse(),
    ) : ProductApi {
        var calls = 0
        var allergies: List<String> = emptyList()
        private fun checkRequest() { calls++; error?.let { throw it } }
        override suspend fun getProductDetailById(productId: Long, analysisMode: String?, includeSelf: Boolean?, familyProfileIds: List<Long>?): ProductDetailApiResponse { checkRequest(); return detail }
        override suspend fun searchProducts(keyword: String, userId: Long?, allergies: List<String>, analysisMode: String?, includeSelf: Boolean?, familyProfileIds: List<Long>?): ProductSearchApiResponse {
            checkRequest()
            this.allergies = allergies
            return ProductSearchApiResponse(data = products)
        }
        override suspend fun getProductDetail(barcode: String, userId: Long?, allergies: List<String>, analysisMode: String?, includeSelf: Boolean?, familyProfileIds: List<Long>?): ProductDetailApiResponse {
            checkRequest()
            return detail
        }

    }
}
