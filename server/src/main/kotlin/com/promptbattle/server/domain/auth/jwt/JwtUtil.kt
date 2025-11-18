package com.promptbattle.server.domain.auth.jwt

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.Cookie
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.Key

@Component
class JwtUtil {

    private val log = LoggerFactory.getLogger(JwtUtil::class.java)

    /**
     * 1) 토큰 상태 확인
     */
    fun getTokenStatus(token: String, key: Key): TokenStatus {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)

            log.info("JWT 토큰 인증 성공: {}", token)
            TokenStatus.AUTHENTICATED

        } catch (e: ExpiredJwtException) {
            log.warn("JWT 토큰 만료: {}", token, e)
            TokenStatus.EXPIRED

        } catch (e: JwtException) {
            log.error("JWT 토큰 무효: {}", token, e)
            TokenStatus.INVALID
        }
    }

    /**
     * 2) 쿠키에서 토큰 값만 뽑기
     */
    fun resolveTokenFromCookie(cookies: Array<Cookie>?, prefix: JwtRule): String {
        if (cookies == null) return ""

        val targetCookieName = prefix.value
        for (cookie in cookies) {
            if (cookie.name == targetCookieName) {
                return cookie.value
            }
        }
        return ""
    }

    /**
     * 3) 시크릿 문자열 → Key 변환
     * (application.yml에 평문 secret 저장 시 사용)
     */
    fun getSigningKey(secret: String): Key {
        return Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * 4) 쿠키 삭제용 (로그아웃 시)
     */
    fun resetToken(prefix: JwtRule): Cookie {
        val c = Cookie(prefix.value, null)
        c.path = "/"
        c.maxAge = 0 // 즉시 만료
        return c
    }
}
