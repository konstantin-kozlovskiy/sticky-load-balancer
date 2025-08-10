package com.stickybalancer.client.configuration

import com.stickybalancer.core.balancer.ServiceInstanceConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "client")
data class ClientConfiguration(
    var serverUrl: String = "http://localhost:9091",
    var requestInterval: Long = 1000,
    var maxConcurrentRequests: Int = 10,
    var threadCount: Int = 5,
    var startupDelaySeconds: Long = 5,
    var minDelayBetweenCheckAndPaySeconds: Long = 5,
    var maxDelayBetweenCheckAndPaySeconds: Long = 15,

    var servers: List<ServiceInstanceConfiguration> = listOf(
        ServiceInstanceConfiguration("http://localhost:9091", "http://localhost:9091"),
        ServiceInstanceConfiguration("http://localhost:9092", "http://localhost:9092")
    ),
)
