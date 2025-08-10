package com.stickybalancer.core.balancer

import org.springframework.web.bind.annotation.GetMapping
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

/**
 * Клиент для проверки здоровья серверных экземпляров.
 *
 * Этот интерфейс определяет контракт для выполнения health check запросов
 * к серверам. Используется StickyLoadBalancer для мониторинга состояния
 * всех доступных серверов.
 *
 * Особенности:
 * - Реактивный интерфейс с возвратом Mono
 * - Автоматическая настройка через Spring Cloud OpenFeign
 * - Интеграция с Spring Boot Actuator health endpoints
 *
 * @see StickyLoadBalancer для использования в балансировщике
 * @see ServiceInstance для интеграции с экземплярами серверов
 */
@ReactiveFeignClient("health-check", url = "")
interface HealthCheckClient {

    /**
     * Выполнить проверку здоровья сервера.
     *
     * Метод отправляет GET запрос на health check endpoint сервера
     * и возвращает результат проверки в виде HealthCheckResponseDto.
     *
     * Ожидается, что сервер предоставляет health endpoint, совместимый
     * с Spring Boot Actuator или аналогичным стандартом.
     *
     * @return Mono<HealthCheckResponseDto> результат проверки здоровья
     * @throws feign.FeignException при ошибках сети или недоступности сервера
     */
    @GetMapping("/actuator/health")
    fun check(): Mono<HealthCheckResponseDto>
}

