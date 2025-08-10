package com.stickybalancer.core.balancer

import com.fasterxml.jackson.annotation.JsonProperty

data class HealthCheckResponseDto(
    @JsonProperty("status")
    val healthStatus: String = ""
)