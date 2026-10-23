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
    "OTP_INVALID" -> "인증번호를 다시 확인해 주세요"
    "OTP_EXPIRED", "OTP_NOT_REQUESTED" -> "인증번호를 다시 받아 주세요"
    "OTP_RATE_LIMITED" -> "인증 요청이 제한됐어요. 잠시 후 다시 시도해 주세요"
    "VERIFICATION_EXPIRED", "VERIFICATION_REQUIRED", "VERIFICATION_TOKEN_INVALID" -> "이메일 인증을 다시 진행해 주세요"
    "LOGIN_FAILED" -> "이메일 또는 비밀번호를 다시 확인해 주세요"
    "AUTH_TOKEN_EXPIRED", "AUTH_TOKEN_INVALID", "AUTHENTICATION_REQUIRED" -> "다시 로그인해 주세요"
    "ACCESS_DENIED" -> "이 기능을 이용할 권한이 없어요"
    "VALIDATION_FAILED" -> "입력 내용을 확인해 주세요"
    else -> when (kind) {
        AuthFailureKind.NotConfigured -> "지금은 인증 서비스를 이용할 수 없어요. 잠시 후 다시 시도해 주세요"
        AuthFailureKind.Network -> "연결을 확인하고 다시 시도해 주세요"
        else -> "요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요"
    }
}
