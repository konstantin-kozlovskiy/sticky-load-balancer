package com.stickybalancer.core.balancer

import feign.FeignException.ServiceUnavailable
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import kotlin.math.abs

class StickyLoadBalancer<T>(
    private val clientName: String,
    private val instances: List<ServiceInstance<T>>
) {
    private val logger = LoggerFactory.getLogger(StickyLoadBalancer::class.java)

    fun <R> execute(requestId: String, requestTimestamp: Instant, action: (T) -> Mono<R>): Mono<R> {
        return action(selectClient(requestId, requestTimestamp))
    }

    @Scheduled(cron = "*/15 * * * * *")
    fun healthCheckScheduler() = processHealthCheck().block()

    private fun processHealthCheck(): Mono<Unit> = Flux
        .fromIterable(instances)
        .flatMap { instance ->
            instance.healthCheckClient
                .check()
                .map { healthResponse ->
                    instance.changeStatus(healthResponse.healthStatus)
                    logger.debug("{} Health check finished with status={}", instance, healthResponse.healthStatus)
                }
                .onErrorResume { error ->
                    when (error.javaClass) {
                        // 503 Service Unavailable - это нормальное поведение при health.status: DOWN
                        ServiceUnavailable::class.java -> {
                            instance.changeStatus("DOWN")
                            logger.debug("{} Health check finished with status=DOWN (ServiceUnavailable)", instance)
                        }

                        else -> {
                            logger.error("$instance Health check failure. ${error.message}")
                            instance.changeStatus("DOWN")
                            logger.debug("{} Health check finished with status=DOWN (error)", instance)
                        }
                    }

                    Mono.empty()
                }
        }
        .collectList()
        .thenReturn(Unit)

    private fun selectClient(requestId: String?, requestTimestamp: Instant?): T {
        // Фильтруем клиентов, которые были доступны на момент времени requestTimestamp
        val availableInstances = instances.filter { client ->
            val statusAtTimestamp = client.getStatusAt(requestTimestamp ?: Instant.now())
            statusAtTimestamp == "UP"
        }

        if (availableInstances.isEmpty()) {
            logger.warn("[$clientName] ($requestId) No available clients at timestamp $requestTimestamp, using all clients")
            // Если нет доступных клиентов на указанное время, используем все
            val hash = requestId.hashCode()
            val serverIndex = abs(hash) % instances.size
            val selectedInstance = instances[serverIndex]
            logger.debug(
                "{} ({}) Selected client (fallback): index={}, totalAvailable={} (using all clients)",
                selectedInstance,
                requestId,
                serverIndex,
                instances.size
            )
            return selectedInstance.serviceClient
        }

        // Выбираем из доступных клиентов
        val hash = requestId.hashCode()
        val serverIndex = abs(hash) % availableInstances.size
        val selectedInstance = availableInstances[serverIndex]
        logger.debug(
            "{} ({}) Selected client: index={}, totalAvailable={} at timestamp {}",
            selectedInstance,
            requestId,
            serverIndex,
            availableInstances.size,
            requestTimestamp
        )
        return selectedInstance.serviceClient
    }
}
