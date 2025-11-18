package com.promptbattle.server.domain.auth.jwt

import lombok.Getter


enum class JwtRule(val value: String) {
    JWT_ISSUE_HEADER("Set-Cookie"),
    ACCESS_PREFIX("access"),
    REFRESH_PREFIX("refresh")
}