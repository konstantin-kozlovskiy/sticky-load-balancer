package com.stickybalancer.core.balancer

import org.slf4j.LoggerFactory
import java.time.Instant

class ServiceInstance<T>(
    var serviceClient: T,
    var healthCheckClient: HealthCheckClient,
    val name: String,
    val url: String,
    private var status: String = "UP",
) {

    private val logger = LoggerFactory.getLogger(ServiceInstance::class.java)
    
    // Журнал состояний: время -> статус
    private val statusHistory = mutableMapOf<Instant, String>().apply {
        put(Instant.now(), status) // Изначально в состоянии UP
    }

    fun changeStatus(newStatus: String) {
        val oldStatus = status

        if (oldStatus != newStatus) {
            status = newStatus
            val timestamp = Instant.now()
            statusHistory[timestamp] = newStatus
            logger.info("[$name: $url] Health status changed from $oldStatus to $newStatus")
        }
    }
    
    /**
     * Получить статус клиента на определенный момент времени
     */
    fun getStatusAt(timestamp: Instant): String {
        // Находим последний статус до указанного времени
        return statusHistory.entries
            .filter { it.key <= timestamp }
            .maxByOrNull { it.key }
            ?.value ?: "UP" // По умолчанию UP если нет истории
    }

    override fun toString(): String {
        return "[$name: $url]"
    }


}
