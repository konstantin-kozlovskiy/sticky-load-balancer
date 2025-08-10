package com.stickybalancer.core.balancer

import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Представляет экземпляр сервера в системе балансировки нагрузки.
 *
 * Этот класс содержит всю необходимую информацию о серверном экземпляре:
 * - Клиент для взаимодействия с основным сервисом
 * - Клиент для проверки здоровья
 * - Имя клиента для логирования
 * - URL сервера
 * - История статусов для поддержки sticky sessions
 *
 * Основные возможности:
 * - Отслеживание статуса сервера во времени
 * - Поддержка graceful shutdown и graceful start
 * - Автоматическое обновление статуса через health check
 *
 * @param T тип клиента для взаимодействия с сервером
 * @param serviceClient Feign клиент для основного сервиса
 * @param healthCheckClient клиент для проверки здоровья
 * @param name имя клиента для логирования
 * @param url URL сервера
 */
class ServiceInstance<T>(
    var serviceClient: T,
    var healthCheckClient: HealthCheckClient,
    private val name: String,
    private val url: String,
    private var status: String = "UP",
) {

    private val logger = LoggerFactory.getLogger(ServiceInstance::class.java)

    // Журнал состояний: время -> статус
    private val statusHistory = mutableMapOf<Instant, String>().apply {
        put(Instant.now(), status) // Изначально в состоянии UP
    }

    /**
     * Изменить статус сервера и добавить запись в историю.
     *
     * Метод автоматически добавляет временную метку изменения статуса,
     * что позволяет отслеживать состояние сервера во времени.
     *
     * @param newStatus новый статус сервера (обычно "UP" или "DOWN")
     */
    fun changeStatus(newStatus: String) {
        val oldStatus = status

        if (oldStatus != newStatus) {
            status = newStatus
            val timestamp = Instant.now()
            statusHistory[timestamp] = newStatus

            // Если записей больше 1000, удаляем самые старые
            if (statusHistory.size > 1000) {
                val toRemove = statusHistory.entries
                    .sortedBy { it.key }
                    .take(statusHistory.size - 1000)
                for (entry in toRemove) {
                    statusHistory.remove(entry.key)
                }
            }

            logger.info("[$name: $url] Health status changed from $oldStatus to $newStatus")
        }
    }

    /**
     * Получить статус сервера на указанный момент времени.
     *
     * Метод анализирует историю статусов и возвращает актуальный статус
     * на момент указанного timestamp. Это критично для sticky sessions,
     * так как позволяет определить, был ли сервер доступен в момент
     * начала обработки запроса.
     *
     * @param timestamp момент времени, для которого нужно определить статус
     * @return статус сервера на указанный момент времени
     */
    fun getStatusAt(timestamp: Instant): String {
        // Находим последний статус до указанного времени
        return statusHistory.entries
            .filter { it.key <= timestamp }
            .maxByOrNull { it.key }
            ?.value ?: "UP" // По умолчанию UP если нет истории
    }

    fun getStatuses(): List<String> {
        return statusHistory.values.toList()
    }

    override fun toString(): String {
        return "[$name: $url]"
    }


}
