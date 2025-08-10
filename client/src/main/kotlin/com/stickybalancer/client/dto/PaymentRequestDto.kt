package com.stickybalancer.client.dto

import java.time.Instant

data class PaymentRequestDto(
    val paymentId: String,
    val amount: Double,
    val currency: String = "USD",
    val description: String? = null,
    val operationStartTime: Instant = Instant.now()
)
