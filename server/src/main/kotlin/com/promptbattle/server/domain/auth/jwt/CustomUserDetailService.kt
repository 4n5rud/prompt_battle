package com.promptbattle.server.domain.auth.jwt

import com.promptbattle.server.domain.user.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val userId = username.toLongOrNull()
            ?: throw RuntimeException("Invalid user id: $username")

        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("loadUserByUsername에서 에러") }

        return UserPrincipal(user)
    }
}
