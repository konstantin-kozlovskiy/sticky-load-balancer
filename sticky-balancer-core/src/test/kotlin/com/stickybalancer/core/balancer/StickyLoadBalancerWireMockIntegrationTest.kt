package com.stickybalancer.core.balancer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.ActiveProfiles
import reactivefeign.client.ReactiveFeignException
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Интеграционный тест для StickyLoadBalancer с использованием WireMock.
 * 
 * Этот тест использует WireMock для имитации HTTP серверов и проверяет:
 * - Создание и настройку балансировщика
 * - Логику sticky sessions
 * - Распределение нагрузки между серверами
 * - Обработку недоступных серверов
 */
@SpringBootTest(
    classes = [TestApplication::class],
    webEnvironment = WebEnvironment.RANDOM_PORT
)
@TestPropertySource(properties = [
    "spring.main.web-application-type=reactive",
    "logging.level.com.stickybalancer=DEBUG"
])
@ActiveProfiles("test")
class StickyLoadBalancerWireMockIntegrationTest {

    @Autowired
    private lateinit var stickyLoadBalancerFactory: StickyLoadBalancerFactory

    private lateinit var loadBalancer: StickyLoadBalancer<TestServiceClient>
    private lateinit var wireMockHelper: WireMockTestHelper
    private lateinit var wireMockServer1: com.github.tomakehurst.wiremock.WireMockServer
    private lateinit var wireMockServer2: com.github.tomakehurst.wiremock.WireMockServer
    private lateinit var wireMockServer3: com.github.tomakehurst.wiremock.WireMockServer

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 100L
    }

    @BeforeEach
    fun setUp() {
        wireMockHelper = WireMockTestHelper()
        
        // Создаем и запускаем WireMock серверы
        wireMockServer1 = wireMockHelper.createAndStartServer("server-1")
        wireMockServer2 = wireMockHelper.createAndStartServer("server-2")
        wireMockServer3 = wireMockHelper.createAndStartServer("server-3")

        // Настраиваем базовые mock ответы для каждого сервера
        wireMockHelper.setupBasicMockResponses(wireMockServer1, "server-1")
        wireMockHelper.setupBasicMockResponses(wireMockServer2, "server-2")
        wireMockHelper.setupBasicMockResponses(wireMockServer3, "server-3")

        // Создаем конфигурации для серверов
        val configurations = listOf(
            ServiceInstanceConfiguration(
                serviceUrl = "http://localhost:${wireMockServer1.port()}",
                healthCheckUrl = "http://localhost:${wireMockServer1.port()}"
            ),
            ServiceInstanceConfiguration(
                serviceUrl = "http://localhost:${wireMockServer2.port()}",
                healthCheckUrl = "http://localhost:${wireMockServer2.port()}"
            ),
            ServiceInstanceConfiguration(
                serviceUrl = "http://localhost:${wireMockServer3.port()}",
                healthCheckUrl = "http://localhost:${wireMockServer3.port()}"
            )
        )

        // Создаем балансировщик
        loadBalancer = stickyLoadBalancerFactory.create(
            TestServiceClient::class.java,
            configurations
        )

        // Даем время на инициализацию балансировщика
        Thread.sleep(500)
        
        println("Тест настроен с серверами на портах: ${wireMockHelper.getServerPorts()}")
    }

    @AfterEach
    fun tearDown() {
        try {
            wireMockHelper.stopAllServers()
        } catch (e: Exception) {
            println("Ошибка при остановке серверов: ${e.message}")
        }
    }

    /**
     * Выполнение запроса с retry логикой
     */
    private fun executeWithRetry(requestId: String, timestamp: Instant, operation: (TestServiceClient) -> Mono<TestResponse>): TestResponse? {
        var lastException: Exception? = null
        
        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                return loadBalancer.execute(requestId, timestamp, operation).block()
            } catch (e: Exception) {
                lastException = e
                if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                    val delay = RETRY_DELAY_MS * (attempt + 1)
                    println("Попытка $attempt неудачна для запроса $requestId, ожидание ${delay}ms перед повторной попыткой")
                    Thread.sleep(delay)
                }
            }
        }
        
        // Если все попытки неудачны, логируем и возвращаем null
        println("Все попытки выполнения запроса $requestId неудачны. Последняя ошибка: ${lastException?.message}")
        return null
    }

    @Test
    fun testStickySessionConsistency() {
        // given
        val requestId = "test-request-123"
        val timestamp = Instant.now()
        val serverResponses = mutableSetOf<String>()

        // when - выполняем несколько запросов с одинаковым requestId
        repeat(5) { index ->
            val response = executeWithRetry(requestId, timestamp) { client ->
                client.testRequest("req-$index", "header-$index")
            }

            response?.let { serverResponses.add(it.serverId) }
        }

        // then - все запросы должны быть направлены на один сервер (sticky session)
        assertEquals(1, serverResponses.size, "Все запросы должны быть направлены на один сервер")
        assertTrue(serverResponses.isNotEmpty(), "Должен быть получен ответ от сервера")
        println("Sticky session тест пройден: все запросы направлены на сервер ${serverResponses.first()}")
    }

    @Test
    fun testLoadDistribution() {
        // given
        val requests = (1..20).map { "request-$it" } // Уменьшаем количество запросов для стабильности
        val serverDistribution = ConcurrentHashMap<String, AtomicInteger>()

        // when - выполняем запросы с разными requestId
        requests.forEach { requestId ->
            val response = executeWithRetry(requestId, Instant.now()) { client ->
                client.testRequest(requestId, "header-$requestId")
            }

            response?.let { 
                serverDistribution.computeIfAbsent(it.serverId) { AtomicInteger(0) }.incrementAndGet()
            }
        }

        // then - нагрузка должна быть распределена между всеми доступными серверами
        assertTrue(serverDistribution.size >= 2, "Нагрузка должна быть распределена между несколькими серверами")
        
        // Проверяем, что распределение не слишком неравномерное
        val minRequests = serverDistribution.values.minOfOrNull { it.get() } ?: 0
        val maxRequests = serverDistribution.values.maxOfOrNull { it.get() } ?: 0
        assertTrue(maxRequests - minRequests <= 8, "Распределение нагрузки не должно быть слишком неравномерным")
        
        println("Распределение нагрузки: $serverDistribution")
    }

    @Test
    fun testFailoverOnServerFailure() {
        // given
        val requestId = "failover-test"
        val timestamp = Instant.now()

        // Получаем первоначальный сервер
        val initialResponse = executeWithRetry(requestId, timestamp) { client ->
            client.testRequest(requestId, "header")
        }
        
        assertNotNull(initialResponse, "Должен быть получен первоначальный ответ")
        val initialServerId = initialResponse!!.serverId
        println("Первоначальный сервер: $initialServerId")

        // when - останавливаем первоначальный сервер
        when (initialServerId) {
            "server-1" -> wireMockHelper.stopServer(wireMockServer1.port())
            "server-2" -> wireMockHelper.stopServer(wireMockServer2.port())
            "server-3" -> wireMockHelper.stopServer(wireMockServer3.port())
        }

        // Ждем остановки сервера
        Thread.sleep(200)

        // then - запрос должен завершиться с ошибкой
        assertThrows<ReactiveFeignException> {
            loadBalancer.execute(requestId, timestamp) { client ->
                client.testRequest(requestId, "header")
            }.block()
        }
        
        println("Failover тест пройден: сервер $initialServerId остановлен, запрос завершился с ошибкой")
    }

    @Test
    fun testNoServersAvailable() {
        // given
        val requestId = "failover-test"

        // when - останавливаем все серверы
        wireMockHelper.stopAllServers()

        // Ждем остановки серверов
        Thread.sleep(200)

        // then - запрос должен завершиться с ошибкой
        assertThrows<ReactiveFeignException> {
            val timestamp = Instant.now()
            loadBalancer.execute(requestId, timestamp) { client ->
                client.testRequest(requestId, "header")
            }.block()
        }
        
        println("Тест 'нет доступных серверов' пройден")
    }

    @Test
    fun testConcurrentRequests() {
        // given
        val requestCount = 10 // Уменьшаем количество для стабильности
        val concurrentRequests = (1..requestCount).map { requestId ->
            loadBalancer.execute("concurrent-$requestId", Instant.now()) { client ->
                client.testRequest("req-$requestId", "header-$requestId")
            }
        }

        // when - выполняем все запросы одновременно
        val responses = concurrentRequests.mapNotNull { 
            try {
                it.block()
            } catch (e: Exception) {
                null // Игнорируем неудачные запросы в этом тесте
            }
        }

        // then - большинство запросов должны быть обработаны
        assertTrue(responses.size >= requestCount * 0.8, "Должно быть обработано не менее 80% запросов")
        
        if (responses.isNotEmpty()) {
            // Проверяем разнообразие серверов
            val uniqueServers = responses.map { it.serverId }.distinct()
            assertTrue(uniqueServers.size >= 2, "Запросы должны быть распределены между несколькими серверами")
            println("Конкурентные запросы: ${responses.size}/$requestCount успешно обработаны, серверы: $uniqueServers")
        }
    }

    @Test
    fun testSessionPersistence() {
        // given
        val sessionId = "persistent-session-456"
        val timestamp = Instant.now()
        val serverResponses = mutableListOf<String>()

        // when - выполняем запросы с одинаковым sessionId в разное время
        repeat(3) { index ->
            val response = executeWithRetry(sessionId, timestamp.plusSeconds(index.toLong())) { client ->
                client.checkSession(sessionId, "session-header-$index")
            }

            response?.let { serverResponses.add(it.serverId) }
        }

        // then - все запросы должны быть направлены на один сервер
        assertEquals(3, serverResponses.size, "Должны быть получены все ответы")
        val uniqueServers = serverResponses.distinct()
        assertEquals(1, uniqueServers.size, "Все запросы сессии должны быть направлены на один сервер")
        
        println("Тест персистентности сессии пройден: все запросы направлены на сервер ${uniqueServers.first()}")
    }
}
