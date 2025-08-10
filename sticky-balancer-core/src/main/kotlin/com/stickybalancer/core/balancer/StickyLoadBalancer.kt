package com.stickybalancer.core.balancer

import feign.FeignException.ServiceUnavailable
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import kotlin.math.abs

/**
 * Балансировщик нагрузки с поддержкой "липких" сессий.
 *
 * Этот класс обеспечивает стабильное распределение запросов между серверами,
 * гарантируя, что запросы с одинаковым requestId всегда направляются на один и тот же сервер
 * (если он доступен на момент выполнения запроса).
 *
 * Основные особенности:
 * - Sticky sessions: запросы с одинаковым ID направляются на один сервер
 * - Graceful shutdown: последующие запросы с тем же ID продолжают работать на том же сервере
 * - Graceful start: запросы, начатые до добавления новых инстансов, продолжаются там же
 * - Автоматическая проверка здоровья серверов каждые 15 секунд
 * - Failover на доступные серверы при недоступности основного
 *
 * @param T тип клиента для взаимодействия с серверами
 * @param clientName имя клиента для логирования
 * @param instances список доступных серверных экземпляров
 */
class StickyLoadBalancer<T>(
    private val clientName: String,
    private val instances: List<ServiceInstance<T>>
) {
    private val logger = LoggerFactory.getLogger(StickyLoadBalancer::class.java)

    /**
     * Выполнить запрос через выбранный сервер с учетом sticky session.
     *
     * Метод автоматически выбирает подходящий сервер на основе requestId и timestamp,
     * обеспечивая консистентность сессии для пользователя.
     *
     * @param requestId уникальный идентификатор запроса для sticky session
     * @param requestTimestamp временная метка запроса для определения доступности сервера
     * @param action функция, которая будет выполнена с выбранным клиентом
     * @return результат выполнения действия в виде Mono
     */
    fun <R> execute(requestId: String, requestTimestamp: Instant, action: (T) -> Mono<R>): Mono<R> {
        return action(selectClient(requestId, requestTimestamp))
    }

    /**
     * Планировщик проверки здоровья серверов.
     *
     * Запускается каждые 15 секунд для мониторинга состояния всех серверных экземпляров.
     * Обновляет статус каждого сервера на основе результатов health check.
     */
    @Scheduled(cron = "*/15 * * * * *")
    fun healthCheckScheduler() = processHealthCheck().block()

    /**
     * Обработать проверку здоровья всех серверных экземпляров.
     *
     * Метод последовательно проверяет каждый сервер и обновляет его статус.
     * Обрабатывает различные типы ошибок и логирует результаты проверки.
     *
     * @return Mono<Unit> завершается после проверки всех серверов
     */
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

    /**
     * Выбрать подходящий сервер для выполнения запроса.
     *
     * Алгоритм выбора:
     * 1. Фильтрует серверы, доступные на момент requestTimestamp
     * 2. Если доступных серверов нет, использует все серверы (fallback)
     * 3. Выбирает сервер на основе хеша requestId для обеспечения sticky session
     *
     * @param requestId идентификатор запроса для sticky routing
     * @param requestTimestamp временная метка для определения доступности сервера
     * @return выбранный клиент для выполнения запроса
     */
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
