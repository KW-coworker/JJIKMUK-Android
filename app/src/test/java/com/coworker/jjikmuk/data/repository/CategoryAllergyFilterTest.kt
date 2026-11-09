package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.domain.model.ProductSearchResult
import org.junit.Assert.*
import org.junit.Test

class CategoryAllergyFilterTest {
    private fun product(raw: String?, labels: List<String> = emptyList()) =
        ProductSearchResult("880", "상품", "제조사", null, labels, raw)

    @Test fun `any selected family allergy excludes a product`() {
        val filter = CategoryAllergyFilter(setOf("우유", "땅콩"))
        assertFalse(filter.allows(product("대두, 우유")))
        assertFalse(filter.allows(product("땅콩")))
        assertTrue(filter.allows(product("대두, 밀")))
    }

    @Test fun `all targets off leaves the catalogue unrestricted including unknown labels`() {
        val filter = CategoryAllergyFilter(emptySet())
        assertFalse(filter.applied)
        assertTrue(filter.allows(product(null)))
        assertTrue(filter.allows(product("우유, 땅콩")))
    }

    @Test fun `unknown information is not accepted as an absence of allergens`() {
        val filter = CategoryAllergyFilter(setOf("milk"))
        assertFalse(filter.allows(product(null)))
        assertFalse(filter.allows(product("정보 없음")))
        assertFalse(filter.allows(product("밀, 미확인성분")))
        assertTrue(filter.allows(product("없음")))
        assertFalse(CategoryAllergyFilter(setOf("미확인성분")).allows(product("밀")))
    }

    @Test fun `canonical ids aliases and parentheses are matched`() {
        assertFalse(CategoryAllergyFilter(setOf("egg")).allows(product("알류(가금류), 대두 함유")))
        assertFalse(CategoryAllergyFilter(setOf("소고기")).allows(product("쇠고기")))
        assertTrue(CategoryAllergyFilter(setOf("crab")).allows(product("달걀, 밀")))
    }

    @Test fun `shellfish includes named shellfish and broad labels remain conservative`() {
        assertFalse(CategoryAllergyFilter(setOf("조개류")).allows(product("전복")))
        assertFalse(CategoryAllergyFilter(setOf("oyster")).allows(product("조개류")))
        assertTrue(CategoryAllergyFilter(setOf("oyster")).allows(product("우유")))
    }

    @Test fun `allergen after the three display chips is still checked`() {
        val item = product("밀,대두,계란,우유", listOf("밀", "대두", "계란"))
        assertFalse(CategoryAllergyFilter(setOf("우유")).allows(item))
    }
}
