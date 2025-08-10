package com.stickybalancer.core.balancer

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
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
    private lateinit var wireMockServer1: WireMockServer
    private lateinit var wireMockServer2: WireMockServer
    private lateinit var wireMockServer3: WireMockServer

    @BeforeEach
    fun setUp() {
        // Создаем WireMock серверы
        wireMockServer1 = WireMockServer(WireMockConfiguration.wireMockConfig().port(9081))
        wireMockServer2 = WireMockServer(WireMockConfiguration.wireMockConfig().port(9082))
        wireMockServer3 = WireMockServer(WireMockConfiguration.wireMockConfig().port(9083))

        // Запускаем серверы
        wireMockServer1.start()
        wireMockServer2.start()
        wireMockServer3.start()

        // Настраиваем mock ответы для каждого сервера
        setupMockResponses(wireMockServer1, "server-1")
        setupMockResponses(wireMockServer2, "server-2")
        setupMockResponses(wireMockServer3, "server-3")

        // Создаем конфигурации для серверов
        val configurations = listOf(
            ServiceInstanceConfiguration(
                serviceUrl = "http://localhost:9081",
                healthCheckUrl = "http://localhost:9081"
            ),
            ServiceInstanceConfiguration(
                serviceUrl = "http://localhost:9082",
                healthCheckUrl = "http://localhost:9082"
            ),
            ServiceInstanceConfiguration(
                serviceUrl = "http://localhost:9083",
                healthCheckUrl = "http://localhost:9083"
            )
        )

        // Создаем балансировщик
        loadBalancer = stickyLoadBalancerFactory.create(
            TestServiceClient::class.java,
            configurations
        )
    }

    @AfterEach
    fun tearDown() {
        wireMockServer1.stop()
        wireMockServer2.stop()
        wireMockServer3.stop()
    }

    private fun setupMockResponses(server: WireMockServer, serverId: String) {
        // Настраиваем ответы для тестовых запросов
        server.stubFor(
            WireMock.get(WireMock.urlMatching("/test/.*"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "requestId": "${WireMock.urlPathEqualTo("/test/.*").toString()}",
                                "serverId": "$serverId",
                                "timestamp": "${Instant.now()}",
                                "message": "Response from $serverId"
                            }
                        """.trimIndent())
                )
        )

        // Настраиваем ответы для проверки сессий
        server.stubFor(
            WireMock.get(WireMock.urlMatching("/session/.*"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "requestId": "${WireMock.urlPathEqualTo("/session/.*").toString()}",
                                "serverId": "$serverId",
                                "timestamp": "${Instant.now()}",
                                "message": "Session response from $serverId"
                            }
                        """.trimIndent())
                )
        )

        // Настраиваем health check endpoint
        server.stubFor(
            WireMock.get(WireMock.urlEqualTo("/actuator/health"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "status": "UP"
                            }
                        """.trimIndent())
                )
        )
    }

    @Test
    fun testStickySessionConsistency() {
        // given
        val requestId = "test-request-123"
        val timestamp = Instant.now()
        val serverResponses = mutableSetOf<String>()

        // when - выполняем несколько запросов с одинаковым requestId
        repeat(5) { index ->
            val response = loadBalancer.execute(requestId, timestamp) { client ->
                client.testRequest("req-$index", "header-$index")
            }.block()

            response?.let { serverResponses.add(it.serverId) }
        }

        // then - все запросы должны быть направлены на один сервер (sticky session)
        assertEquals(1, serverResponses.size, "Все запросы должны быть направлены на один сервер")
        assertTrue(serverResponses.isNotEmpty(), "Должен быть получен ответ от сервера")
    }

    @Test
    fun testLoadDistribution() {
        // given
        val requests = (1..30).map { "request-$it" }
        val serverDistribution = ConcurrentHashMap<String, AtomicInteger>()

        // when - выполняем запросы с разными requestId
        requests.forEach { requestId ->
            val response = loadBalancer.execute(requestId, Instant.now()) { client ->
                client.testRequest(requestId, "header-$requestId")
            }.block()

            response?.let { 
                serverDistribution.computeIfAbsent(it.serverId) { AtomicInteger(0) }.incrementAndGet()
            }
        }

        // then - нагрузка должна быть распределена между всеми доступными серверами
        assertTrue(serverDistribution.size >= 2, "Нагрузка должна быть распределена между несколькими серверами")
        
        // Проверяем, что распределение не слишком неравномерное
        val minRequests = serverDistribution.values.minOfOrNull { it.get() } ?: 0
        val maxRequests = serverDistribution.values.maxOfOrNull { it.get() } ?: 0
        assertTrue(maxRequests - minRequests <= 10, "Распределение нагрузки не должно быть слишком неравномерным")
    }

    @Test
    fun testFailoverOnServerFailure() {
        // given
        val requestId = "failover-test"
        val timestamp = Instant.now()

        loadBalancer.healthCheckScheduler()

        // Получаем первоначальный сервер
        val initialResponse = loadBalancer.execute(requestId, timestamp) { client ->
            client.testRequest(requestId, "header")
        }.block()
        
        assertNotNull(initialResponse, "Должен быть получен первоначальный ответ")
        val initialServerId = initialResponse!!.serverId

        // when - останавливаем первоначальный сервер
        when (initialServerId) {
            "server-1" -> wireMockServer1.stop()
            "server-2" -> wireMockServer2.stop()
            "server-3" -> wireMockServer3.stop()
        }

        loadBalancer.healthCheckScheduler()

        // then - запрос должен быть направлен на недоступный сервер (продолжен)
        org.junit.jupiter.api.assertThrows<ReactiveFeignException> {
            loadBalancer.execute(requestId, timestamp) { client ->
                client.testRequest(requestId, "header")
            }.block()
        }
    }

    @Test
    fun testNoServersAvailable() {
        // given
        val requestId = "failover-test"

        loadBalancer.healthCheckScheduler()

        // when - останавливаем все серверы
        wireMockServer1.stop()
        wireMockServer2.stop()
        wireMockServer3.stop()

        loadBalancer.healthCheckScheduler()

        Thread.sleep(1)

        // then - запрос должен быть направлен на недоступный сервер (продолжен)
        org.junit.jupiter.api.assertThrows<ReactiveFeignException> {
            val timestamp = Instant.now()
            loadBalancer.execute(requestId, timestamp) { client ->
                client.testRequest(requestId, "header")
            }.block()
        }

    }

    @Test
    fun testConcurrentRequests() {
        // given
        val requestCount = 20
        val concurrentRequests = (1..requestCount).map { requestId ->
            loadBalancer.execute("concurrent-$requestId", Instant.now()) { client ->
                client.testRequest("req-$requestId", "header-$requestId")
            }
        }

        // when - выполняем все запросы одновременно
        val responses = concurrentRequests.mapNotNull { it.block() }

        // then - все запросы должны быть обработаны
        assertEquals(requestCount, responses.size, "Все запросы должны быть обработаны")
        
        // Проверяем разнообразие серверов
        val uniqueServers = responses.map { it.serverId }.distinct()
        assertTrue(uniqueServers.size >= 2, "Запросы должны быть распределены между несколькими серверами")
    }

    @Test
    fun testSessionPersistence() {
        // given
        val sessionId = "persistent-session-456"
        val timestamp = Instant.now()
        val serverResponses = mutableListOf<String>()

        // when - выполняем запросы с одинаковым sessionId в разное время
        repeat(3) { index ->
            val response = loadBalancer.execute(sessionId, timestamp.plusSeconds(index.toLong())) { client ->
                client.checkSession(sessionId, "session-header-$index")
            }.block()

            response?.let { serverResponses.add(it.serverId) }
        }

        // then - все запросы должны быть направлены на один сервер
        assertEquals(3, serverResponses.size, "Должны быть получены все ответы")
        val uniqueServers = serverResponses.distinct()
        assertEquals(1, uniqueServers.size, "Все запросы сессии должны быть направлены на один сервер")
    }
}
