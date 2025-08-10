package com.stickybalancer.client.client

import com.stickybalancer.client.dto.PaymentRequestDto
import com.stickybalancer.client.dto.PaymentResponseDto
import com.stickybalancer.core.balancer.StickyLoadBalancer
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class PaymentsStickyLoadBalancedClient(
    val stickyLoadBalancer: StickyLoadBalancer<PaymentClient>
) {

    fun checkPayment(request: PaymentRequestDto): Mono<PaymentResponseDto> =
        stickyLoadBalancer.execute(request.paymentId, request.operationStartTime) { client ->
            client.checkPayment(request)
        }

    fun processPayment(request: PaymentRequestDto): Mono<PaymentResponseDto> =
        stickyLoadBalancer.execute(request.paymentId, request.operationStartTime) { client ->
            client.processPayment(request.paymentId)
        }

}