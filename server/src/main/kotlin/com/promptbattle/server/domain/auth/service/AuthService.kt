package com.promptbattle.server.domain.auth.service

import com.promptbattle.server.domain.user.entity.User
import com.promptbattle.server.domain.user.repository.UserRepository
import com.promptbattle.server.domain.auth.dto.SignInRequest
import com.promptbattle.server.domain.auth.dto.SignInResponse
import com.promptbattle.server.domain.auth.dto.SignUpRequest
import com.promptbattle.server.domain.auth.jwt.JwtService
import com.promptbattle.server.global.redis.RedisService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val redisService: RedisService
) {

    @Value("\${spring.data.redis.key.refresh_token_base}")
    private lateinit var REFRESH_TOKEN_KEY: String

    @Transactional
    fun signUp(req: SignUpRequest): User {
        if (userRepository.existsByEmail(req.email)) {
            throw RuntimeException("이미 존재하는 이메일입니다.")
        }
        val user = User(
            username = req.name,
            email = req.email,
            password = passwordEncoder.encode(req.password)
        )
        return userRepository.save(user)
    }

    @Transactional
    fun signIn(req: SignInRequest, res: HttpServletResponse): SignInResponse {
        val user = userRepository.findByEmail(req.email)
            ?: throw RuntimeException("사용자를 찾을 수 없습니다.")

        if (!passwordEncoder.matches(req.password, user.password)) {
            throw RuntimeException("비밀번호가 일치하지 않습니다.")
        }

        val accessToken = jwtService.generateAccessToken(res, user)
        jwtService.generateRefreshToken(res, user) // RTR 방식으로 refresh도 발급

        return SignInResponse(userId = user.id ?: -1L, accessToken = accessToken)
    }

    fun logout(user : User, res: HttpServletResponse) {

        val refreshTokenKey = REFRESH_TOKEN_KEY + user.id
        redisService.delete(refreshTokenKey)

        jwtService.logout(res)
    }
}
