package com.coworker.jjikmuk.feature.chat.presentation

/** 챗봇 오류 말풍선. 대화 메시지가 아니므로 messages와 대화 기록에 들어가지 않는다. */
data class ChatErrorUiModel(
    val title: String,
    val description: String,
    val retryable: Boolean,
    val isRetrying: Boolean = false,
)

object ChatErrorCodes {
    const val NETWORK_UNAVAILABLE = "NETWORK_UNAVAILABLE"
    const val CHAT_REQUEST_FAILED = "CHAT_REQUEST_FAILED"
    const val ANALYSIS_SELECTION_UNAVAILABLE = "ANALYSIS_SELECTION_UNAVAILABLE"
    const val ANALYSIS_SELECTION_CHANGED = "ANALYSIS_SELECTION_CHANGED"
    const val AUTHENTICATION_REQUIRED = "AUTHENTICATION_REQUIRED"
}

private val selectionInvalidCodes = setOf(
    "FAMILY_NOT_FOUND",
    "CHAT_ANALYSIS_SELECTION_UNSUPPORTED",
    "ANALYSIS_SELECTION_INVALID",
)

private val loginRequiredCodes = setOf(
    "AUTH_TOKEN_EXPIRED",
    "AUTH_TOKEN_INVALID",
    ChatErrorCodes.AUTHENTICATION_REQUIRED,
    "REFRESH_TOKEN_INVALID",
    "REFRESH_TOKEN_REUSED",
)

/** errorCode를 사용자에게 보여 줄 오류 말풍선 문구와 재시도 가능 여부로 바꾼다. */
fun chatErrorFor(errorCode: String?): ChatErrorUiModel = when (errorCode) {
    ChatErrorCodes.NETWORK_UNAVAILABLE -> ChatErrorUiModel(
        title = "인터넷에 연결되지 않았어요",
        description = "연결을 확인한 뒤 다시 시도해 주세요.",
        retryable = true,
    )
    ChatErrorCodes.ANALYSIS_SELECTION_UNAVAILABLE -> ChatErrorUiModel(
        title = "분석 대상을 불러오지 못했어요",
        description = "다시 시도하면 가족 목록을 다시 불러와요.",
        retryable = true,
    )
    ChatErrorCodes.ANALYSIS_SELECTION_CHANGED -> ChatErrorUiModel(
        title = "분석 대상이 바뀌었어요",
        description = "새 대상 기준으로 다시 질문해 주세요.",
        retryable = false,
    )
    in selectionInvalidCodes -> ChatErrorUiModel(
        title = "선택한 가족을 찾을 수 없어요",
        description = "분석 대상을 다시 선택해 주세요.",
        retryable = false,
    )
    in loginRequiredCodes -> ChatErrorUiModel(
        title = "다시 로그인해 주세요",
        description = "로그인 정보가 만료됐어요.",
        retryable = false,
    )
    else -> ChatErrorUiModel(
        title = "답변을 가져오지 못했어요",
        description = "잠시 후 다시 시도해 주세요.",
        retryable = true,
    )
}
