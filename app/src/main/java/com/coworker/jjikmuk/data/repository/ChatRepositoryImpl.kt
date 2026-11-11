package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.remote.api.ChatApi
import com.coworker.jjikmuk.data.remote.dto.ChatApiRequest
import com.coworker.jjikmuk.data.remote.dto.ChatApiResponse
import com.coworker.jjikmuk.domain.model.ChatProductCandidate
import com.coworker.jjikmuk.domain.model.ChatResponse
import com.coworker.jjikmuk.domain.repository.ChatRepository
import com.coworker.jjikmuk.domain.repository.AnalysisTargetRepository
import com.coworker.jjikmuk.domain.repository.AnalysisTargetSelection
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
    private val targets: AnalysisTargetRepository,
) : ChatRepository {

    override suspend fun sendMessage(message: String, selectedProduct: ChatProductCandidate?, selection: AnalysisTargetSelection?): ChatResponse {
        val chosen = selection ?: targets.awaitSelection()
            ?: return ChatResponse("분석 대상을 불러오지 못했어요. 가족 목록과 로그인 상태를 확인해 주세요.", errorCode = "ANALYSIS_SELECTION_UNAVAILABLE")
        if (!targets.isCurrent(chosen)) return changed()
        val result = operations.execute { accessToken ->
            if (!targets.isCurrent(chosen)) return@execute AuthResult.Failure(AuthFailure(AuthFailureKind.Server, code = "ANALYSIS_SELECTION_CHANGED"))
            try {
                // Backend resolves owned profiles from JWT and query IDs. Consent remains server-owned.
                AuthResult.Success(chatApi.sendMessage("Bearer $accessToken", ChatApiRequest(message = message, barcode = selectedProduct?.barcode),
                    analysisMode = chosen.mode, includeSelf = chosen.queryIncludeSelf, familyProfileIds = chosen.queryFamilyIds))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: HttpException) { AuthResult.Failure(com.coworker.jjikmuk.feature.auth.data.AuthErrorMapper.decode(
                error.code(), error.response()?.errorBody()?.use { it.string() }, null, System.currentTimeMillis())) }
            catch (_: IOException) { AuthResult.Failure(AuthFailure(AuthFailureKind.Network)) }
            catch (_: Exception) { AuthResult.Failure(AuthFailure(AuthFailureKind.InvalidResponse)) }
        }
        if (!targets.isCurrent(chosen)) return changed()
        return when (result) {
            is AuthResult.Success -> ChatResponse(result.value.answer, result.value.toProductCandidates(),
                recommendedQuestions = result.value.toRecommendedQuestions())
            is AuthResult.Failure -> ChatResponse(when (result.error.code) {
                "ANALYSIS_SELECTION_CHANGED" -> "분석 대상이 바뀌었어요. 다시 질문해 주세요."
                "FAMILY_NOT_FOUND" -> "선택한 가족을 확인할 수 없어요. 가족 목록을 새로고침해 주세요."
                "CHAT_ANALYSIS_SELECTION_UNSUPPORTED", "ANALYSIS_SELECTION_INVALID" -> "서버에서 선택한 대상을 처리하지 못했어요. 분석 대상과 서버 배포 상태를 확인해 주세요."
                else -> result.error.userMessage()
            }, errorCode = result.error.code
                ?: if (result.error.kind == AuthFailureKind.Network) "NETWORK_UNAVAILABLE" else "CHAT_REQUEST_FAILED")
        }
    }
    private fun changed() = ChatResponse("분석 대상이나 계정이 바뀌었어요. 다시 질문해 주세요.", errorCode = "ANALYSIS_SELECTION_CHANGED")

}

/** 후속 질문 칩: 앞뒤 공백 제거 → 빈 문자열 제거 → 중복 제거 → 최대 3개. 서버가 null을 보내도 안전하다. */
@Suppress("USELESS_CAST") // Gson은 Kotlin 기본값을 무시하고 null을 넣을 수 있다.
internal fun ChatApiResponse.toRecommendedQuestions(): List<String> =
    (recommended_questions as List<String?>?).orEmpty()
        .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
        .distinct()
        .take(MAX_RECOMMENDED_QUESTIONS)

private const val MAX_RECOMMENDED_QUESTIONS = 3

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
