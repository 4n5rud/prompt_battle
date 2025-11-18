package com.promptbattle.server.domain.auth.dto

data class SignUpRequest(
    val name: String = "",
    val password: String = "",
    val email: String = ""
)