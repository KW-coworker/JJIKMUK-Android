package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.remote.api.*
import com.coworker.jjikmuk.domain.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import mockwebserver3.*
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ProductSelectionTest {
    private class Targets : AnalysisTargetRepository {
        val account = FamilyAccount(5, 1)
        var chosen = AnalysisTargetSelection(account, false, emptyList(), "1")
        var stale = false
        var unavailable = false
        override val state = MutableStateFlow(AnalysisTargetsState())
        override fun captureAccount() = account
        override suspend fun setSelected(id: String, checked: Boolean, account: FamilyAccount?) = false
        override suspend fun awaitSelection(account: FamilyAccount?) = if (unavailable) error("Profiles unavailable") else chosen
        override fun isCurrent(selection: AnalysisTargetSelection) = !stale && selection == chosen
    }
    private val detail = """{"data":{"product":{"productId":123,"barcode":"880","productName":"포키","allergy":"우유"},"analysis":{"status":"DANGER","isDangerous":true,"dangerousIngredients":["우유"]}}}"""
    private inline fun checkRequest(block: (MockWebServer, Retrofit, Targets, ProductRepositoryImpl) -> Unit) {
        MockWebServer().use { server ->
            server.start()
            val retrofit=Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build()
            val targets=Targets()
            block(server,retrofit,targets,ProductRepositoryImpl(retrofit.create(ProductApi::class.java), targets))
        }
    }
    @Test fun `none detail explicitly requests no personal analysis and keeps label facts`() = checkRequest { server, _, _, repo -> runBlocking {
        server.enqueue(MockResponse.Builder().body(detail).build())
        val product=repo.getProductDetail("880").getOrThrow()
        assertEquals("none", server.takeRequest().url.queryParameter("analysisMode"))
        assertFalse(product.personalConditionsApplied); assertFalse(product.isDangerous)
        assertTrue(product.dangerousIngredients.isEmpty()); assertEquals(listOf("우유"), product.allergyLabels)
    } }
    @Test fun `detail uses selected family ids while catalog search is unrestricted`() = checkRequest { server, _, targets, repo -> runBlocking {
        targets.chosen=targets.chosen.copy(includeSelf=true, familyProfileIds=listOf(31,32))
        server.enqueue(MockResponse.Builder().body(detail).build())
        assertTrue(repo.getProductDetailById(123).getOrThrow().personalConditionsApplied)
        val byId=server.takeRequest()
        assertEquals("selected",byId.url.queryParameter("analysisMode"))
        assertEquals(listOf("31","32"),byId.url.queryParameterValues("familyProfileIds"))
        server.enqueue(MockResponse.Builder().body("""{"data":[]}""").build())
        repo.searchProducts("포키", listOf("우유")).getOrThrow()
        val search=server.takeRequest()
        assertEquals("none",search.url.queryParameter("analysisMode"))
        assertNull(search.url.queryParameter("includeSelf"))
        assertNull(search.url.queryParameter("familyProfileIds"))
        assertNull(search.url.queryParameter("allergies"))
    } }
    @Test fun `capacity lookup also keeps none and alternatives do not use implicit self`() = checkRequest { server, retrofit, _, repo -> runBlocking {
        server.enqueue(MockResponse.Builder().body("""{"data":{"options":[{"key":"a","data":${detail.substringAfter("\"data\":").dropLast(1)}}]}}""").build())
        val options=ProductOptionsRepository(retrofit.create(ProductOptionsApi::class.java),repo)
        assertFalse(options.capacities("880").options.single().product.personalConditionsApplied)
        assertEquals("none",server.takeRequest().url.queryParameter("analysisMode"))
        assertTrue(options.alternatives("880").products.isEmpty())
        assertEquals(1,server.requestCount)
    } }
    @Test fun `catalog search succeeds even when profiles are unavailable or selection is stale`() = checkRequest { server, _, targets, repo -> runBlocking {
        targets.unavailable=true; targets.stale=true
        server.enqueue(MockResponse.Builder().body("""{"data":[{"product":{"barcode":"880","productName":"포키"}}]}""").build())
        assertEquals("포키",repo.searchProducts("포키").getOrThrow().single().productName)
        val request=server.takeRequest()
        assertEquals("none",request.url.queryParameter("analysisMode"))
        assertNull(request.url.queryParameter("familyProfileIds"))
    } }
    @Test fun `results from old selection are rejected`() = checkRequest { server, _, targets, repo -> runBlocking {
        targets.stale=true
        server.enqueue(MockResponse.Builder().body(detail).build())
        assertTrue(repo.getProductDetail("880").isFailure)
    } }
}
