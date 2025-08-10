package com.stickybalancer.client.service

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PaymentStartupService(
    private val multiThreadedPaymentService: MultiThreadedPaymentService
) {
    
    private val logger = LoggerFactory.getLogger(PaymentStartupService::class.java)
    
    @EventListener(ApplicationReadyEvent::class)
    fun startPaymentTestingOnStartup() {
        logger.info("Payment client started at ${LocalDateTime.now()}")
        logger.info("Starting multi-threaded payment testing...")
        
        multiThreadedPaymentService.startMultiThreadedPaymentTesting()
            .subscribe(
                { result -> 
                    logger.debug(result)
                },
                { error -> 
                    logger.error("REQUEST FAILED: ${error.message}")
                }
            )
    }
}
