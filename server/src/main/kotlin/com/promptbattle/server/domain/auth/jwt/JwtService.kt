package com.promptbattle.server.domain.auth.jwt

import com.promptbattle.server.domain.user.entity.User

import io.jsonwebtoken.Jwts
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.Key

import com.promptbattle.server.domain.auth.jwt.JwtRule.*
import com.promptbattle.server.global.redis.RedisService

@Service
class JwtService(
    private val userDetailsService: CustomUserDetailsService,
    private val generator: JwtGenerator,
    private val util: JwtUtil,
    private val redisService: RedisService,
    @Value("\${spring.jwt.access-token.secret}") accessSecret: String,
    @Value("\${spring.jwt.refresh-token.secret}") refreshSecret: String,
    @Value("\${spring.jwt.access-token.expiration}") accessExpiration: Long,
    @Value("\${spring.jwt.refresh-token.expiration}") refreshExpiration: Long
) {

    private val ACCESS_KEY: Key = util.getSigningKey(accessSecret)
    private val REFRESH_KEY: Key = util.getSigningKey(refreshSecret)
    private val ACCESS_EXP: Long = accessExpiration
    private val REFRESH_EXP: Long = refreshExpiration

    @Value("\${spring.data.redis.key.refresh_token_base}")
    private lateinit var REFRESH_TOKEN_KEY: String

    @Transactional
    fun generateAccessToken(res: HttpServletResponse, u: User): String {
        val at = generator.generateAccessToken(ACCESS_KEY, ACCESS_EXP, u)
        val cookie = ResponseCookie.from(ACCESS_PREFIX.value, at)
            .path("/")
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .maxAge(ACCESS_EXP / 1000)
            .build()
        res.addHeader(JWT_ISSUE_HEADER.value, cookie.toString())
        return at
    }

    @Transactional
    fun generateRefreshToken(res: HttpServletResponse, u: User): String {
        val rt = generator.generateRefreshToken(REFRESH_KEY, REFRESH_EXP, u)
        val cookie = ResponseCookie.from(REFRESH_PREFIX.value, rt)
            .path("/")
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .maxAge(REFRESH_EXP / 1000)
            .build()
        res.addHeader(JWT_ISSUE_HEADER.value, cookie.toString())
        return rt
    }

    fun validateAccessToken(t: String): Boolean {
        return util.getTokenStatus(t, ACCESS_KEY) == TokenStatus.AUTHENTICATED
    }

    fun validateRefreshToken(t: String, identifier: Long): Boolean {
        val ok = util.getTokenStatus(t, REFRESH_KEY) == TokenStatus.AUTHENTICATED
        if (!ok) return false

        val key = REFRESH_TOKEN_KEY + identifier
        val stored = redisService.get(key, String::class.java)
        return t == stored
    }

    fun getAuthentication(token: String): Authentication {
        val userId = Jwts.parserBuilder()
            .setSigningKey(ACCESS_KEY)
            .build()
            .parseClaimsJws(token)
            .body
            .subject
        val principal: UserDetails = userDetailsService.loadUserByUsername(userId)
        return UsernamePasswordAuthenticationToken(principal, "", principal.authorities)
    }

    fun resolveToken(req: HttpServletRequest, p: JwtRule): String {
        val cs = req.cookies ?: throw RuntimeException("JWT 쿠키가 존재하지 않습니다.")
        return util.resolveTokenFromCookie(cs, p)
    }

    @Transactional
    fun logout(res: HttpServletResponse) {


        res.addCookie(util.resetToken(ACCESS_PREFIX))
        res.addCookie(util.resetToken(REFRESH_PREFIX))
    }
}