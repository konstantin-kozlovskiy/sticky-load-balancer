package com.stickybalancer.client.client

import com.stickybalancer.client.dto.PaymentRequestDto
import com.stickybalancer.client.dto.PaymentResponseDto
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

@ReactiveFeignClient("payment", url = "http://localhost:9091")
interface PaymentClient {
    @PostMapping("/api/v1/check")
    fun checkPayment(@RequestBody request: PaymentRequestDto): Mono<PaymentResponseDto>
    
    @PostMapping("/api/v1/pay")
    fun processPayment(@PathVariable paymentId: String): Mono<PaymentResponseDto>
}
