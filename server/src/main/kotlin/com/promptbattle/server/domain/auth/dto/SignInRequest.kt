package com.promptbattle.server.domain.auth.dto

data class SignInRequest(
    val email: String = "",
    val password: String = "",
)
