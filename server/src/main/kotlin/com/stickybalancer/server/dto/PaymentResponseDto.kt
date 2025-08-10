package com.stickybalancer.server.dto

data class PaymentResponseDto(
    val status: String,
    val paymentId: String,
    val message: String? = null
)


