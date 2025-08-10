package com.stickybalancer.core.balancer

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

/**
 * Тестовый Feign клиент для интеграционного тестирования StickyLoadBalancer.
 * 
 * Этот клиент используется для проверки логики балансировки нагрузки
 * и sticky sessions в интеграционных тестах.
 */
@ReactiveFeignClient(
    name = "test-service",
    url = "http://localhost:8080"
)
interface TestServiceClient {
    
    /**
     * Простой GET запрос для проверки балансировки.
     * 
     * @param requestId идентификатор запроса для sticky session
     * @return ответ от сервера
     */
    @GetMapping("/test/{requestId}")
    fun testRequest(
        @PathVariable requestId: String,
        @RequestHeader("X-Request-ID") requestHeader: String
    ): Mono<TestResponse>
    
    /**
     * Проверка sticky session - запрос должен вернуться к тому же серверу.
     * 
     * @param sessionId идентификатор сессии
     * @return ответ с информацией о сервере
     */
    @GetMapping("/session/{sessionId}")
    fun checkSession(
        @PathVariable sessionId: String,
        @RequestHeader("X-Session-ID") sessionHeader: String
    ): Mono<TestResponse>
}

/**
 * Тестовый ответ от сервера.
 */
data class TestResponse(
    val requestId: String,
    val serverId: String,
    val timestamp: String,
    val message: String
)
