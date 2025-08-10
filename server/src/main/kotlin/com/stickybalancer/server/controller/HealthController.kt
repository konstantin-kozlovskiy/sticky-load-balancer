package com.stickybalancer.server.controller

import com.stickybalancer.server.health.CustomHealthIndicator
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/health")
class HealthController {
    
    @PostMapping("/down")
    fun setHealthStatus(): ResponseEntity<Map<String, Any>> {
        CustomHealthIndicator.setHealthy(false)
        return ResponseEntity.ok(
            mapOf(
                "status" to "DOWN",
                "timestamp" to System.currentTimeMillis()
            )
        )
    }
}
