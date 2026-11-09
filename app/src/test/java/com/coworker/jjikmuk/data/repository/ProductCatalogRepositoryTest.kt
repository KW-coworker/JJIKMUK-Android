package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.remote.api.ProductApi
import kotlinx.coroutines.runBlocking
import mockwebserver3.*
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.HttpException

class ProductCatalogRepositoryTest {
    private fun repo(server: MockWebServer): ProductCatalogRepository {
        val retrofit=Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build()
        return ProductCatalogRepository(retrofit.create(ProductCatalogApi::class.java), ProductRepositoryImpl(retrofit.create(ProductApi::class.java)))
    }
    @Test fun `metadata uses exact server ids and removes duplicate categories`() = runBlocking {
        MockWebServer().use { server ->
            server.start(); server.enqueue(MockResponse.Builder().body("""{"data":{"foodCategories":[{"id":"snack","label":"과자·스낵"},{"id":"snack","label":"과자·스낵"},{"id":"unclassified","label":"미분류"}]}}""").build())
            assertEquals(listOf("snack","unclassified"),repo(server).categories().map { it.id })
            assertEquals("/api/products/classifications",server.takeRequest().url.encodedPath)
        }
    }
    @Test fun `category page and keyword use filters with none and server pagination`() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().body("""{"data":{"items":[{"product":{"barcode":"880","productName":"새우깡","manufacturer":"농심","allergy":"새우"}}],"page":1,"totalElements":21,"hasNext":false}}""").build())
            val result=repo(server).page("snack"," 새우깡 ",1)
            assertEquals("새우깡",result.products.single().productName); assertEquals(21,result.total); assertFalse(result.hasNext)
            val req=server.takeRequest()
            assertEquals("/api/products/filter",req.url.encodedPath)
            assertEquals("snack",req.url.queryParameter("categories")); assertEquals("새우깡",req.url.queryParameter("keyword"))
            assertEquals("none",req.url.queryParameter("analysisMode")); assertEquals("1",req.url.queryParameter("page"))
            assertEquals("20",req.url.queryParameter("size")); assertNull(req.url.queryParameter("familyProfileIds"))
        }
    }
    @Test fun `empty category success is distinct from server failure and omits empty keyword`() = runBlocking {
        MockWebServer().use { server ->
            server.start(); val repo=repo(server)
            server.enqueue(MockResponse.Builder().body("""{"data":{"items":[],"page":0,"totalElements":0,"hasNext":false}}""").build())
            assertTrue(repo.page("special","",0).products.isEmpty())
            assertNull(server.takeRequest().url.queryParameter("keyword"))
            server.enqueue(MockResponse.Builder().code(500).body("{}").build())
            try { repo.page("special","",0); fail("Failure must not become an empty search") } catch (_: HttpException) {}
        }
    }
    @Test fun `malformed pagination is rejected instead of silently stopping or repeating pages`() = runBlocking {
        MockWebServer().use { server ->
            server.start(); server.enqueue(MockResponse.Builder().body("""{"data":{"items":[],"page":0,"totalElements":1,"hasNext":true}}""").build())
            try { repo(server).page("snack","",0); fail("Cannot advance an empty next page") } catch (_: IllegalStateException) {}
        }
    }
}
