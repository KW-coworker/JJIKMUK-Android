package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.remote.api.ChatApi
import com.coworker.jjikmuk.data.remote.dto.ChatApiRequest
import com.coworker.jjikmuk.data.remote.dto.ChatApiResponse
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
                productCandidates = result.value.toProductCandidates())
            is AuthResult.Failure -> ChatResponse(answer = result.error.userMessage())
        }
    }

}

/** The deployed server uses product_candidates; older responses use recommended_products. */
internal fun ChatApiResponse.toProductCandidates(): List<ChatProductCandidate> =
    (product_candidates.orEmpty() + recommended_products.orEmpty()
        .filter { it.selection_type == "product_candidate" })
        .mapNotNull { product ->
            val name = product.product_name?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            ChatProductCandidate(
                productName = name,
                brandName = product.prdlst_dcnm.orEmpty().ifBlank { "제품" },
                barcode = product.barcode,
                rawMaterials = product.rawmtrl_nm,
                allergyLabels = (product.allergy_info.orEmpty() + product.allergy.orEmpty())
                    .map(String::trim).filter(String::isNotEmpty).distinct(),
                reason = product.reason,
            )
        }.distinctBy { it.barcode to it.productName }
