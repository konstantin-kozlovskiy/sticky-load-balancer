package com.stickybalancer.server.model

import java.time.LocalDateTime

data class Payment(
    val paymentId: String,
    val amount: Double,
    val currency: String,
    val description: String?,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)


