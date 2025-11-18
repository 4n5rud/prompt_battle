package com.promptbattle.server.domain.auth.jwt

import com.promptbattle.server.domain.user.entity.User
import com.promptbattle.server.domain.user.repository.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.security.core.context.SecurityContextHolder
import java.io.IOException

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val uri = request.requestURI
        return uri.startsWith("/api/auth/signup") ||
                uri.startsWith("/api/auth/signin")
    }

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val uri = request.requestURI
        log.info("요청 URI: {}", uri)

        val accessToken = jwtService.resolveToken(request, JwtRule.ACCESS_PREFIX)
        if (accessToken.isNullOrBlank()) {
            log.debug("요청에 액세스 토큰 없음: {}", uri)
            chain.doFilter(request, response)
            return
        }

        if (jwtService.validateAccessToken(accessToken)) {
            SecurityContextHolder.getContext().authentication = jwtService.getAuthentication(accessToken)
            log.info("엑세스 토큰 검증 성공, 인증 객체 설정 완료")
            chain.doFilter(request, response)
            return
        }

        log.info("엑세스 토큰 만료됨, 리프레시 토큰 검증 시작")

        val refreshToken = jwtService.resolveToken(request, JwtRule.REFRESH_PREFIX)
        if (refreshToken.isNullOrBlank()) {
            jwtService.logout(response)
            chain.doFilter(request, response)
            return
        }

        val idLong: Long = try {
            jwtService.getAuthentication(refreshToken).name.toLongOrNull()
                ?: run {
                    log.warn("리프레시 토큰의 subject(id) 파싱 실패: {}", jwtService.getAuthentication(refreshToken).name)
                    jwtService.logout(response)
                    chain.doFilter(request, response)
                    return
                }
        } catch (ex: Exception) {
            log.warn("리프레시 토큰으로 인증 객체 추출 실패", ex)
            jwtService.logout(response)
            chain.doFilter(request, response)
            return
        }

        val user: User = try {
            userRepository.findById(idLong).orElseThrow { RuntimeException("사용자를 찾을 수 없습니다.") }
        } catch (ex: Exception) {
            log.warn("사용자 조회 실패: {}", idLong, ex)
            jwtService.logout(response)
            chain.doFilter(request, response)
            return
        }

        val userIdNonNull: Long = user.id ?: run {
            log.warn("조회된 사용자 id가 null 입니다: user={}", user)
            jwtService.logout(response)
            chain.doFilter(request, response)
            return
        }

        if (jwtService.validateRefreshToken(refreshToken, userIdNonNull)) {
            val newAccessToken = jwtService.generateAccessToken(response, user)
            jwtService.generateRefreshToken(response, user) // RTR
            SecurityContextHolder.getContext().authentication = jwtService.getAuthentication(newAccessToken)
            chain.doFilter(request, response)
            return
        }

        jwtService.logout(response)
        chain.doFilter(request, response)
    }
}
