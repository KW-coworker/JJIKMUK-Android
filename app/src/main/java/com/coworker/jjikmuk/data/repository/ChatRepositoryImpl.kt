package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.remote.api.ChatApi
import com.coworker.jjikmuk.data.remote.dto.ChatApiProduct
import com.coworker.jjikmuk.data.remote.dto.ChatApiRequest
import com.coworker.jjikmuk.data.remote.dto.ChatApiRecommendedProduct
import com.coworker.jjikmuk.domain.model.ChatProductCandidate
import com.coworker.jjikmuk.domain.model.ChatResponse
import com.coworker.jjikmuk.domain.repository.ChatRepository
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
) : ChatRepository {

    override suspend fun sendMessage(
        message: String,
        selectedProduct: ChatProductCandidate?,
    ): ChatResponse {
        return runCatching {
            val response = chatApi.sendMessage(
                ChatApiRequest(
                    message = message,
                    barcode = selectedProduct?.barcode,
                    product = selectedProduct?.toRequestProduct(),
                ),
            )
            ChatResponse(
                answer = response.answer,
                productCandidates = response.recommended_products
                    .filter { product -> product.selection_type == PRODUCT_CANDIDATE_TYPE }
                    .mapNotNull { product -> product.toDomain() },
            )
        }.getOrElse { throwable ->
            ChatResponse(
                answer = "AI 서버 연결에 실패했어요. 서버가 실행 중인지 확인해주세요.\n\n${throwable.message.orEmpty()}",
            )
        }
    }

    private fun ChatProductCandidate.toRequestProduct(): ChatApiProduct? {
        val productBarcode = barcode?.takeIf(String::isNotBlank) ?: return null

        return ChatApiProduct(
            productName = productName,
            barcode = productBarcode,
            allergy = allergyLabels,
            rawMaterials = rawMaterials.orEmpty(),
        )
    }

    private fun ChatApiRecommendedProduct.toDomain(): ChatProductCandidate? {
        val name = product_name?.takeIf(String::isNotBlank) ?: return null
        val allergies = (allergy_info + allergy)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()

        return ChatProductCandidate(
            productName = name,
            brandName = prdlst_dcnm.orEmpty().ifBlank { "제품" },
            barcode = barcode,
            rawMaterials = rawmtrl_nm,
            allergyLabels = allergies,
            reason = reason,
        )
    }

    private companion object {
        const val PRODUCT_CANDIDATE_TYPE = "product_candidate"
    }
}
