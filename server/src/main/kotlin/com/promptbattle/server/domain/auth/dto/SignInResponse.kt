package com.promptbattle.server.domain.auth.dto

data class SignInResponse(
    val userId: Long,
    val accessToken: String
)