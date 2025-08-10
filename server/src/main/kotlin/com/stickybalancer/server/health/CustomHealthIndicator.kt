package com.stickybalancer.server.health

import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.actuate.health.Health
import org.springframework.stereotype.Component

@Component
class CustomHealthIndicator : HealthIndicator {
    
    companion object {
        @Volatile
        private var isHealthy: Boolean = true
        
        fun setHealthy(healthy: Boolean) {
            isHealthy = healthy
        }
        
        fun getHealthy(): Boolean = isHealthy
    }
    
    override fun health(): Health {
        return if (isHealthy) {
            Health.up()
                .withDetail("status", "UP")
                .withDetail("message", "Service is healthy")
                .build()
        } else {
            Health.down()
                .withDetail("status", "DOWN")
                .withDetail("message", "Service is manually set to unhealthy")
                .build()
        }
    }
}
