package com.promptbattle.server.domain.auth.jwt


import com.promptbattle.server.domain.user.entity.User
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.stereotype.Component
import java.security.Key
import java.util.Date

@Component
class JwtGenerator {

    companion object {
        private val HEADER: Map<String, Any> = mapOf(
            "typ" to "JWT",
            "alg" to "HS256"
        )
    }

    // 1) Access Token 생성
    fun generateAccessToken(secret: Key, expMillis: Long, user: User): String {
        val now = System.currentTimeMillis()
        return Jwts.builder()
            .setHeader(HEADER)
            .setSubject(user.id.toString())               // sub: 사용자 PK(Id)
            .setExpiration(Date(now + expMillis))         // 만료시간
            .signWith(secret, SignatureAlgorithm.HS256)   // 서명
            .compact()
    }

    // 2) Refresh Token 생성
    fun generateRefreshToken(secret: Key, expMillis: Long, user: User): String {
        val now = System.currentTimeMillis()
        return Jwts.builder()
            .setHeader(HEADER)
            .setSubject(user.id.toString())               // sub: 사용자 PK(Id)
            .setExpiration(Date(now + expMillis))         // 만료시간
            .signWith(secret, SignatureAlgorithm.HS256)   // 서명
            .compact()
    }
}