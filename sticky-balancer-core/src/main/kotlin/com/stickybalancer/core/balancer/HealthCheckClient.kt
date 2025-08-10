package com.stickybalancer.core.balancer

import org.springframework.web.bind.annotation.GetMapping
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

@ReactiveFeignClient("health-check", url = "")
interface HealthCheckClient {
    @GetMapping("/actuator/health")
    fun check(): Mono<HealthCheckResponseDto>
}

