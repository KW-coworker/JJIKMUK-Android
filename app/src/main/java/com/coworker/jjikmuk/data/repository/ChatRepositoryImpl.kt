package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.remote.api.ChatApi
import com.coworker.jjikmuk.data.remote.dto.ChatApiProduct
import com.coworker.jjikmuk.data.remote.dto.ChatApiRequest
import com.coworker.jjikmuk.data.remote.dto.ChatApiRecommendedProduct
import com.coworker.jjikmuk.domain.model.ChatProductCandidate
import com.coworker.jjikmuk.domain.model.ChatResponse
import com.coworker.jjikmuk.domain.repository.ChatRepository
import javax.inject.Inject
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.AuthorizedOperations
import com.coworker.jjikmuk.feature.auth.presentation.common.userMessage
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

class ChatRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val operations: AuthorizedOperations,
) : ChatRepository {

    override suspend fun sendMessage(
        message: String,
        selectedProduct: ChatProductCandidate?,
    ): ChatResponse {
        val result = operations.execute { accessToken ->
            try {
                AuthResult.Success(chatApi.sendMessage("Bearer $accessToken", ChatApiRequest(
                    message = message, barcode = selectedProduct?.barcode,
                    product = selectedProduct?.toRequestProduct(),
                    // Consent UI/answer/candidate contract is owned by the chat feature team.
                    externalAiConsent = false,
                )))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: HttpException) { AuthResult.Failure(AuthFailure(AuthFailureKind.Server, error.code(),
                code = if (error.code() == 401) "AUTH_TOKEN_INVALID" else null)) }
            catch (_: IOException) { AuthResult.Failure(AuthFailure(AuthFailureKind.Network)) }
            catch (_: Exception) { AuthResult.Failure(AuthFailure(AuthFailureKind.InvalidResponse)) }
        }
        return when (result) {
            is AuthResult.Success -> ChatResponse(answer = result.value.answer,
                productCandidates = result.value.recommended_products
                    .filter { it.selection_type == PRODUCT_CANDIDATE_TYPE }.mapNotNull { it.toDomain() })
            is AuthResult.Failure -> ChatResponse(answer = result.error.userMessage())
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
