package com.stickybalancer.server

import com.stickybalancer.server.dto.PaymentRequestDto
import com.stickybalancer.server.service.PaymentService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.test.StepVerifier

@SpringBootTest
class PaymentServiceTest {
    
    @Autowired
    private lateinit var paymentService: PaymentService
    
    @Test
    fun `should check payment successfully`() {
        val request = PaymentRequestDto(
            paymentId = "test-payment-1",
            amount = 100.0,
            currency = "USD",
            description = "Test payment"
        )
        
        StepVerifier.create(paymentService.checkPayment(request))
            .expectNextMatches { response ->
                response.status == "CHECK_OK" && 
                response.paymentId == "test-payment-1"
            }
            .verifyComplete()
    }
    
    @Test
    fun `should process payment successfully`() {
        // Сначала проверяем платеж
        val checkRequest = PaymentRequestDto(
            paymentId = "test-payment-2",
            amount = 200.0,
            currency = "USD"
        )
        
        paymentService.checkPayment(checkRequest).block()
        
        // Затем обрабатываем платеж
        StepVerifier.create(paymentService.processPayment("test-payment-2"))
            .expectNextMatches { response ->
                response.status == "PAY_OK" && 
                response.paymentId == "test-payment-2"
            }
            .verifyComplete()
    }
    
    @Test
    fun `should return PAY_ERROR for non-existent payment`() {
        StepVerifier.create(paymentService.processPayment("non-existent-payment"))
            .expectNextMatches { response ->
                response.status == "PAY_ERROR"
            }
            .verifyComplete()
    }
}
