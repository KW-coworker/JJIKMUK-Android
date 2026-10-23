package com.coworker.jjikmuk.data.repository
import com.coworker.jjikmuk.data.remote.api.*
import com.coworker.jjikmuk.data.remote.dto.*
import com.coworker.jjikmuk.domain.model.ProductSafetyStatus
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class ProductOptionsRepositoryTest {
    private val mapper = ProductRepositoryImpl(object: ProductApi {
        override suspend fun searchProducts(keyword:String,userId:Long?,allergies:List<String>):ProductSearchApiResponse = error("unused")
        override suspend fun getProductDetail(barcode:String,userId:Long?,allergies:List<String>):ProductDetailApiResponse = error("unused")
    })
    @Test fun `capacity options carry their own calories and conservative safety analysis`() = runBlocking {
        val api = object:ProductOptionsApi {
            override suspend fun variants(barcode:String) = ProductVariantsResponse(ProductVariantsDto("small", listOf(
                ProductCapacityDto("small","25g",25.0,"g","a",ProductDetailDataDto(
                    product=ProductDto(barcode="a",productName="상품",energyKcal=140.0,totalWeight="25g"),analysis=ProductAnalysisDto(status="PASS"))),
                ProductCapacityDto("large","41g",41.0,"g","b",ProductDetailDataDto(
                    product=ProductDto(barcode="b",productName="상품",energyKcal=230.0,totalWeight="41g"),analysis=ProductAnalysisDto(status="DANGER",isDangerous=true)))
            )))
            override suspend fun alternatives(barcode:String,limit:Int) = error("unused")
        }
        val result=ProductOptionsRepository(api,mapper).capacities("a")
        assertEquals("small",result.selectedKey)
        assertEquals(140.0,result.options[0].product.nutrition.energyKcal!!,0.01)
        assertEquals(230.0,result.options[1].product.nutrition.energyKcal!!,0.01)
        assertEquals(ProductSafetyStatus.Danger,result.options[1].product.safetyStatus)
    }
    @Test fun `unsafe alternatives and original product are excluded and unknowns require verification`() = runBlocking {
        fun item(code:String,status:String)=AlternativeItemDto(ProductDto(barcode=code,productName=code),AlternativeSafetyDto(status))
        val api=object:ProductOptionsApi {
            override suspend fun variants(barcode:String)=error("unused")
            override suspend fun alternatives(barcode:String,limit:Int)=ProductAlternativesResponse(ProductAlternativesDto(
                items=listOf(item("source","PASS"),item("danger","DANGER"),item("safe","PASS")),
                fallbackItems=listOf(item("safe","PASS"),item("unknown","UNKNOWN"))))
        }
        val result=ProductOptionsRepository(api,mapper).alternatives("source")
        assertEquals(listOf("safe","unknown"),result.products.map{it.product.barcode})
        assertFalse(result.products[0].needsVerification)
        assertTrue(result.products[1].needsVerification)
    }
}
