package com.stickybalancer.core.balancer

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * DTO для ответа health check запроса.
 *
 * Этот класс представляет структуру ответа от health endpoint сервера.
 * Используется для десериализации JSON ответов от Spring Boot Actuator
 * или аналогичных health check endpoints.
 *
 * Структура ответа:
 * - healthStatus: строка, содержащая статус сервера ("UP", "DOWN", "UNKNOWN")
 *
 * @see HealthCheckClient для выполнения health check запросов
 * @see ServiceInstance для использования в экземплярах серверов
 */
data class HealthCheckResponseDto(
    /**
     * Статус здоровья сервера.
     *
     * Ожидаемые значения:
     * - "UP" - сервер работает нормально
     * - "DOWN" - сервер недоступен или имеет проблемы
     * - "UNKNOWN" - статус не может быть определен
     *
     * По умолчанию пустая строка для случаев, когда ответ не содержит статус.
     */
    @JsonProperty("status")
    val healthStatus: String = ""
)