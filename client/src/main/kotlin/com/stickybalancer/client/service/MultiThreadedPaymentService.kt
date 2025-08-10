package com.stickybalancer.client.service

import com.stickybalancer.client.client.PaymentsStickyLoadBalancedClient
import com.stickybalancer.client.configuration.ClientConfiguration
import com.stickybalancer.client.dto.PaymentRequestDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

@Service
class MultiThreadedPaymentService(
    private val paymentClient: PaymentsStickyLoadBalancedClient,
    private val clientConfiguration: ClientConfiguration
) {
    private val requestCounter = AtomicLong(0)
    private val logger = LoggerFactory.getLogger(MultiThreadedPaymentService::class.java.name)

    fun startMultiThreadedPaymentTesting(): Flux<String> {
        return Flux.range(1, clientConfiguration.threadCount)
            .flatMap { threadId ->
                startPaymentThread(threadId)
            }
            .onErrorResume { error ->
                Mono.just("Error: ${error.message}")
            }
    }

    private fun startPaymentThread(threadId: Int): Flux<String> {
        return Flux.interval(Duration.ofSeconds(clientConfiguration.startupDelaySeconds))
            .flatMap {
                executePaymentOperation(threadId)
            }
            .subscribeOn(Schedulers.boundedElastic())
    }

    private fun executePaymentOperation(threadId: Int): Mono<String> {
        val requestId = requestCounter.incrementAndGet()
        val paymentId = "payment-${threadId}-${requestId}"

        return Mono.defer {
            // Выполняем check операцию
            val request = PaymentRequestDto(
                paymentId = paymentId,
                amount = 100.0 + Random.nextDouble(0.0, 900.0),
                currency = "USD",
                description = "Thread $threadId - Payment $requestId"
            )

            paymentClient.checkPayment(request)
                .flatMap { checkResponse ->
                    // Генерируем случайную задержку между check и pay
                    val delaySeconds = Random.nextLong(
                        clientConfiguration.minDelayBetweenCheckAndPaySeconds,
                        clientConfiguration.maxDelayBetweenCheckAndPaySeconds + 1
                    )

                    Mono.delay(Duration.ofSeconds(delaySeconds))
                        .flatMap {
                            // Выполняем pay операцию
                            paymentClient.processPayment(request)
                                .map { payResponse ->
                                    "Thread $threadId - Request $requestId - Check: $checkResponse, Pay: $payResponse (Delay: ${delaySeconds}s)"
                                }
                        }
                }
        }
            .onErrorResume { error ->
                logger.warn("Thread $threadId - Request $requestId failed: ${error.message}")
                Mono.just("Thread $threadId - Request $requestId failed: ${error.message}")
            }
    }
}
