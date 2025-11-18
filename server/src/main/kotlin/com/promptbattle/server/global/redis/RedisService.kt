package com.promptbattle.server.global.redis


import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class RedisService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) {

    /**
     * DTO 객체를 그대로 Redis에 저장합니다.
     *
     * @param key Redis 키 (예: "user:alice")
     * @param value 저장할 DTO 객체
     * @param expirationSeconds TTL(초 단위)
     */
    fun save(key: String, value: Any, expirationSeconds: Long) {
        redisTemplate.opsForValue()
            .set(key, value, expirationSeconds, TimeUnit.SECONDS)
    }

    /**
     * Redis에서 꺼내올 때는 호출자가 원하는 타입으로 캐스팅합니다.
     *
     * @param key Redis 키
     * @param type 꺼낼 DTO 클래스 타입
     * @param T DTO 타입 파라미터
     * @return 저장된 DTO 인스턴스 (없으면 null)
     */
    fun <T> get(key: String, type: Class<T>): T? {
        val obj = redisTemplate.opsForValue().get(key) ?: return null
        return objectMapper.convertValue(obj, type)
    }

    /**
     * 키 삭제
     */
    fun delete(key: String) {
        redisTemplate.delete(key)
    }

    /**
     * 키 존재 여부 확인
     */
    fun hasKey(key: String): Boolean {
        return redisTemplate.hasKey(key) == true
    }
}
