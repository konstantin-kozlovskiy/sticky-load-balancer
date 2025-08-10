package com.stickybalancer.core.balancer

/**
 * Конфигурация серверного экземпляра для StickyLoadBalancer.
 *
 * Этот класс содержит всю необходимую информацию для подключения к серверу:
 * - URL основного сервиса для выполнения бизнес-логики
 * - URL для проверки здоровья сервера
 *
 * Используется фабрикой StickyLoadBalancerFactory для создания ServiceInstance.
 * Каждый экземпляр представляет отдельный сервер в кластере, к которому
 * может подключаться клиент.
 *
 * @param serviceUrl URL основного сервиса (например, "http://server1:9091")
 * @param healthCheckUrl URL для проверки здоровья (например, "http://server1:9091")
 *
 * @see ServiceInstance для создания экземпляра сервера
 * @see StickyLoadBalancerFactory для использования в фабрике
 */
data class ServiceInstanceConfiguration(
    /**
     * URL основного сервиса для выполнения бизнес-логики.
     *
     * Этот URL используется для создания Feign клиента, который будет
     * выполнять все основные запросы к серверу. Должен указывать на
     * доступный endpoint сервера.
     *
     * Примеры:
     * - "http://server1:9091"
     * - "https://api.example.com"
     * - "http://localhost:8080"
     */
    val serviceUrl: String,

    /**
     * URL для проверки здоровья сервера.
     *
     * Этот URL используется для создания HealthCheckClient, который
     * будет периодически проверять доступность сервера. Обычно
     * указывает на health endpoint (например, /actuator/health).
     *
     * Примеры:
     * - "http://server1:9091"
     * - "https://api.example.com"
     * - "http://localhost:8080"
     */
    val healthCheckUrl: String
)
