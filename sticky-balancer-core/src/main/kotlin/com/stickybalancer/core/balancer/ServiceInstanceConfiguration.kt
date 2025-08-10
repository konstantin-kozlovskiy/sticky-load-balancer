package com.stickybalancer.core.balancer

/**
 * Конфигурация сервера
 */
data class ServiceInstanceConfiguration(
    val serviceUrl: String,
    val healthCheckUrl: String,
)
