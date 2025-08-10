package com.stickybalancer.server.controller

import com.stickybalancer.server.dto.PaymentRequestDto
import com.stickybalancer.server.dto.PaymentResponseDto
import com.stickybalancer.server.service.PaymentService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/v1")
class PaymentController(
    private val paymentService: PaymentService
) {

    private val logger = LoggerFactory.getLogger(PaymentController::class.java)

    @PostMapping("/check")
    fun checkPayment(@RequestBody request: PaymentRequestDto): Mono<PaymentResponseDto> {
        logger.debug("Received check payment request: ${request.paymentId}")
        return paymentService.checkPayment(request)
    }

    @PostMapping("/pay")
    fun processPayment(@RequestParam paymentId: String): Mono<PaymentResponseDto> {
        logger.debug("Received process payment request: $paymentId")
        return paymentService.processPayment(paymentId)
    }
}


