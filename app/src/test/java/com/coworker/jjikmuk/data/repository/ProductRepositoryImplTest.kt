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
        assertEquals(listOf("wheat"), api.allergies)
    }

    @Test fun `cancellation propagates from search detail and recommendations`() = runTest {
        val cancellation = CancellationException("cancelled")
        val repository = ProductRepositoryImpl(FakeApi(error = cancellation))
        val requests: List<suspend () -> Any> = listOf(
            { repository.searchProducts("포키", emptyList()) },
            { repository.getProductDetail("id", emptyList()) },
            { repository.getSafeRecommendations(emptyList(), emptyList(), 6) },
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

    private class FakeApi(
        private val error: Exception? = null,
        private val products: List<ProductSearchItemDto> = emptyList(),
    ) : ProductApi {
        var calls = 0
        var allergies: List<String> = emptyList()
        private fun checkRequest() { calls++; error?.let { throw it } }
        override suspend fun searchProducts(keyword: String, userId: Long?, allergies: List<String>): ProductSearchApiResponse {
            checkRequest()
            this.allergies = allergies
            return ProductSearchApiResponse(data = products)
        }
        override suspend fun getProductDetail(barcode: String, userId: Long?, allergies: List<String>): ProductDetailApiResponse {
            checkRequest()
            return ProductDetailApiResponse()
        }
        override suspend fun getSafeRecommendations(filters: List<String>, allergies: List<String>, limit: Int, userId: Long?): ProductSearchApiResponse {
            checkRequest()
            return ProductSearchApiResponse(data = products)
        }
    }
}
