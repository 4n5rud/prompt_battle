package com.promptbattle.server.domain.auth.controller

import com.promptbattle.server.domain.auth.dto.SignInRequest
import com.promptbattle.server.domain.auth.dto.SignInResponse
import com.promptbattle.server.domain.auth.dto.SignUpRequest
import com.promptbattle.server.domain.auth.jwt.UserPrincipal
import com.promptbattle.server.domain.auth.service.AuthService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/signup")
    fun signUp(@RequestBody req: SignUpRequest): ResponseEntity<Any> {
        val created = authService.signUp(req)
        return ResponseEntity.ok(created)
    }

    @PostMapping("/signin")
    fun login(@RequestBody req: SignInRequest, response: HttpServletResponse): ResponseEntity<SignInResponse> {
        val result = authService.signIn(req, response)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/logout")
    fun logout(@AuthenticationPrincipal principal: UserPrincipal, response: HttpServletResponse): ResponseEntity<Void> {
        authService.logout(principal.user, response)
        return ResponseEntity.noContent().build()
    }
}