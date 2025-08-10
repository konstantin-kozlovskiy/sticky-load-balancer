package com.stickybalancer.server.service

import com.stickybalancer.server.dto.PaymentRequestDto
import com.stickybalancer.server.dto.PaymentResponseDto
import com.stickybalancer.server.model.Payment
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

@Service
class PaymentService {

    private val logger = LoggerFactory.getLogger(PaymentService::class.java)
    private val paymentCache = ConcurrentHashMap<String, Payment>()

    fun checkPayment(request: PaymentRequestDto): Mono<PaymentResponseDto> {
        return Mono.fromCallable {
            try {
                // Проверяем платеж (симуляция)
                val payment = Payment(
                    paymentId = request.paymentId,
                    amount = request.amount,
                    currency = request.currency,
                    description = request.description,
                )

                // Сохраняем в кеш
                paymentCache[request.paymentId] = payment

                val response = PaymentResponseDto(
                    status = "CHECK_OK",
                    paymentId = request.paymentId,
                    message = "Payment checked successfully"
                )

                logger.info("<${request.paymentId} CHECK_OK>")
                response
            } catch (e: Exception) {
                val response = PaymentResponseDto(
                    status = "CHECK_ERROR",
                    paymentId = request.paymentId,
                    message = "Error checking payment: ${e.message}"
                )

                logger.info("<${request.paymentId} CHECK_ERROR> - ${e.message}")
                response
            }
        }
    }

    fun processPayment(paymentId: String): Mono<PaymentResponseDto> {
        return Mono.fromCallable {
            val payment = paymentCache[paymentId]

            if (payment != null) {

                val response = PaymentResponseDto(
                    status = "PAY_OK",
                    paymentId = paymentId,
                    message = "Payment processed successfully"
                )

                logger.info("<${paymentId} PAY_OK>")
                response
            } else {
                val response = PaymentResponseDto(
                    status = "PAY_ERROR",
                    paymentId = paymentId,
                    message = "Payment not found in cache"
                )

                logger.info("<${paymentId} PAY_ERROR> - Payment not found in cache")
                response
            }
        }
    }
}


