package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.domain.model.ProductSearchResult

/** Label matching only, not a claim that a product is safe to eat. */
internal class CategoryAllergyFilter(private val selected: Set<String>) {
    val applied: Boolean get() = selected.isNotEmpty()
    private val restrictions = parse(selected.joinToString(","))

    fun allows(product: ProductSearchResult): Boolean {
        if (!applied) return true
        val targets = restrictions ?: return false
        val labels = parse(product.allergyText ?: product.allergyLabels.joinToString(",")) ?: return false
        if (targets.intersect(labels).isNotEmpty()) return false
        val shellfish = setOf("shellfish", "oyster", "mussel", "abalone")
        return !("shellfish" in targets && labels.any { it in shellfish } ||
            "shellfish" in labels && targets.any { it in shellfish })
    }

    companion object {
        private val aliases = buildMap {
            FamilyConditions.allergies.forEach { (id, label) -> put(id, id); put(label, id) }
            put("달걀", "egg"); put("알류", "egg"); put("난류", "egg"); put("가금류", "egg")
            put("쇠고기", "beef"); put("아황산염", "sulfites"); put("아황산", "sulfites")
            put("겨자", "mustard")
        }
        private fun parse(raw: String): Set<String>? {
            val normalized = raw.trim().lowercase().replace("해당 없음", "해당없음")
            if (normalized in setOf("없음", "해당없음", "none", "무함유")) return emptySet()
            if (normalized.isBlank()) return null
            val tokens = normalized.replace("함유", "")
                .split(Regex("[,;/|\\s\\[\\](){}]+"))
                .map(String::trim).filter(String::isNotEmpty)
            if (tokens.isEmpty()) return null
            val ids = tokens.map { aliases[it] ?: return null }
            return ids.toSet()
        }
    }
}
