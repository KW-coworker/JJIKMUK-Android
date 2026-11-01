package com.coworker.jjikmuk.feature.auth.presentation.common

import com.coworker.jjikmuk.feature.auth.domain.AuthFailure
import com.coworker.jjikmuk.feature.auth.domain.AuthFailureKind

/** Presentation mapping only. An HTTP 401 here never deletes a session. */
fun AuthFailure.userMessage(): String = when (code) {
    "EMAIL_INVALID" -> "올바른 이메일 주소를 입력해 주세요"
    "EMAIL_ALREADY_REGISTERED" -> "이미 가입된 이메일이에요. 로그인해 주세요"
    "USER_NOT_FOUND" -> "가입된 이메일 주소를 확인해 주세요"
    "AUTH_METHOD_NOT_SUPPORTED" -> "가입할 때 사용한 로그인 방식을 이용해 주세요"
    "NICKNAME_ALREADY_EXISTS" -> "이미 사용 중인 닉네임이에요"
    "NICKNAME_INVALID" -> "닉네임 입력 조건을 확인해 주세요"
    "PASSWORD_INVALID" -> "비밀번호 입력 조건을 확인해 주세요"
    "OTP_INVALID" -> when (remainingAttempts) {
        0 -> "인증 시도 횟수를 모두 사용했어요. 인증번호를 다시 받아 주세요"
        null -> "인증번호를 다시 확인해 주세요"
        else -> "인증번호를 다시 확인해 주세요. 남은 시도 횟수: ${remainingAttempts}회"
    }
    "OTP_EXPIRED", "OTP_NOT_REQUESTED" -> "인증번호를 다시 받아 주세요"
    "OTP_RATE_LIMITED" -> "인증 요청이 제한됐어요. 잠시 후 다시 시도해 주세요"
    "VERIFICATION_EXPIRED", "VERIFICATION_REQUIRED", "VERIFICATION_TOKEN_INVALID" -> "이메일 인증을 다시 진행해 주세요"
    "LOGIN_RATE_LIMITED" -> "로그인 시도가 많아 잠시 제한됐어요. 잠시 후 다시 시도해 주세요"
    "PROFILE_INCOMPLETE" -> "추가 프로필 입력을 완료해 주세요"
    "LOGIN_FAILED" -> "이메일 또는 비밀번호를 다시 확인해 주세요"
    "AUTH_TOKEN_EXPIRED", "AUTH_TOKEN_INVALID", "AUTHENTICATION_REQUIRED" -> "다시 로그인해 주세요"
    "AUTH_SERVICE_UNAVAILABLE" -> "인증 서비스를 잠시 이용할 수 없어요. 잠시 후 다시 시도해 주세요"
    "REFRESH_TOKEN_INVALID", "REFRESH_TOKEN_REUSED" -> "다시 로그인해 주세요"
    "REFRESH_OUTCOME_UNKNOWN" -> "로그인 갱신 결과를 확인하지 못했어요. 다시 로그인해 주세요"
    "REFRESH_RETRY_UNCONFIRMED" -> "로그인 갱신을 완료하지 못했어요. 잠시 후 다시 로그인해 주세요"
    "SESSION_STORAGE_FAILED" -> "로그인 정보를 저장하지 못했어요. 다시 로그인해 주세요"
    "AUTH_REFRESHED_RETRY_REQUIRED" -> "로그인 정보를 갱신했어요. 작업 결과를 확인한 뒤 다시 시도해 주세요"
    "GOOGLE_LOGIN_UNAVAILABLE" -> "지금은 Google 로그인을 이용할 수 없어요. 잠시 후 다시 시도해 주세요"
    "GOOGLE_TOKEN_INVALID" -> "Google 로그인 정보를 확인하지 못했어요. 계정을 다시 선택해 주세요"
    "GOOGLE_LINK_PASSWORD_INVALID" -> "기존 계정의 비밀번호를 확인해 주세요"
    "GOOGLE_LINK_EXPIRED" -> "연결 시간이 만료됐어요. Google 로그인을 다시 진행해 주세요"
    "GOOGLE_ACCOUNT_ALREADY_LINKED" -> "이미 다른 Google 계정과 연결되어 있어요"
    "PROFILE_VERSION_CONFLICT" -> "다른 기기에서 정보가 변경됐어요. 최신 정보를 확인해 주세요"
    "FAMILY_NOT_FOUND" -> "가족 정보를 다시 확인해 주세요"
    "PHOTO_INVALID" -> "사용 가능한 사진 형식과 해상도를 확인해 주세요"
    "PHOTO_TOO_LARGE" -> "사진 용량이 너무 커요. 더 작은 사진을 선택해 주세요"
    "ANALYSIS_SELECTION_INVALID" -> "분석할 대상을 다시 확인해 주세요"
    "ACCESS_DENIED" -> "이 기능을 이용할 권한이 없어요"
    "VALIDATION_FAILED" -> "입력 내용을 확인해 주세요"
    else -> when (kind) {
        AuthFailureKind.NotConfigured -> "지금은 인증 서비스를 이용할 수 없어요. 잠시 후 다시 시도해 주세요"
        AuthFailureKind.Network -> "연결을 확인하고 다시 시도해 주세요"
        else -> "요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요"
    }
}
