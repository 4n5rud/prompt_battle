package com.promptbattle.server.domain.auth.jwt

enum class TokenStatus {
    AUTHENTICATED, // 서명·유효기간 모두 정상
    EXPIRED,       // 유효기간 지남
    INVALID        // 서명 불일치 등 구조 이상
}