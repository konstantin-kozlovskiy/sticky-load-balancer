package com.stickybalancer.server.dto

data class PaymentRequestDto(
    val paymentId: String,
    val amount: Double,
    val currency: String = "USD",
    val description: String? = null
)


