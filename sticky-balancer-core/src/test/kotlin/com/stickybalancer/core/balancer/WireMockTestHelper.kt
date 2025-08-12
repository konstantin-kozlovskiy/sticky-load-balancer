package com.stickybalancer.core.balancer

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import java.net.Socket
import java.net.InetSocketAddress
import java.time.Duration
import java.time.Instant

/**
 * Вспомогательный класс для управления WireMock серверами в интеграционных тестах.
 * 
 * Этот класс обеспечивает:
 * - Надежный запуск и остановку серверов
 * - Ожидание готовности серверов
 * - Автоматическую очистку ресурсов
 * - Настройку mock ответов
 */
class WireMockTestHelper {
    
    companion object {
        private const val DEFAULT_STARTUP_TIMEOUT_MS = 10000L
        private const val DEFAULT_READY_CHECK_INTERVAL_MS = 100L
        private const val DEFAULT_CONNECTION_TIMEOUT_MS = 1000
    }
    
    private val servers = mutableListOf<WireMockServer>()
    
    /**
     * Создает и запускает WireMock сервер с оптимизированными настройками.
     * 
     * @param serverId идентификатор сервера для логирования
     * @return запущенный сервер
     */
    fun createAndStartServer(serverId: String): WireMockServer {
        val config = WireMockConfiguration.wireMockConfig()
            .dynamicPort() // Используем динамические порты
            .asynchronousResponseEnabled(false) // Отключаем асинхронные ответы
            .jettyAcceptors(1) // Минимальное количество акцепторов
            .jettyAcceptQueueSize(10) // Минимальный размер очереди
            .stubCorsEnabled(false) // Отключаем CORS для ускорения
            .gzipDisabled(true) // Отключаем gzip для упрощения

        val server = WireMockServer(config)
        server.start()
        
        // Ждем готовности сервера
        waitForServerReady(server.port(), serverId)
        
        servers.add(server)
        return server
    }
    
    /**
     * Ожидание готовности сервера.
     * 
     * @param port порт сервера
     * @param serverId идентификатор сервера для логирования
     * @param timeoutMs таймаут ожидания в миллисекундах
     */
    private fun waitForServerReady(port: Int, serverId: String, timeoutMs: Long = DEFAULT_STARTUP_TIMEOUT_MS) {
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("localhost", port), DEFAULT_CONNECTION_TIMEOUT_MS)
                    println("Сервер $serverId готов на порту $port")
                    return
                }
            } catch (e: Exception) {
                // Сервер еще не готов, ждем
                Thread.sleep(DEFAULT_READY_CHECK_INTERVAL_MS)
            }
        }
        
        throw RuntimeException("Сервер $serverId на порту $port не готов в течение ${timeoutMs}ms")
    }
    
    /**
     * Настройка базовых mock ответов для сервера.
     * 
     * @param server WireMock сервер
     * @param serverId идентификатор сервера
     */
    fun setupBasicMockResponses(server: WireMockServer, serverId: String) {
        // Health check endpoint
        server.stubFor(
            WireMock.get(WireMock.urlEqualTo("/actuator/health"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "status": "UP",
                                "timestamp": "${Instant.now()}"
                            }
                        """.trimIndent())
                        .withFixedDelay(10)
                )
        )
        
        // Общий endpoint для тестирования
        server.stubFor(
            WireMock.get(WireMock.urlMatching("/test/.*"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "requestId": "test-request",
                                "serverId": "$serverId",
                                "timestamp": "${Instant.now()}",
                                "message": "Response from $serverId"
                            }
                        """.trimIndent())
                        .withFixedDelay(50)
                )
        )
        
        // Session endpoint
        server.stubFor(
            WireMock.get(WireMock.urlMatching("/session/.*"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "requestId": "session-request",
                                "serverId": "$serverId",
                                "timestamp": "${Instant.now()}",
                                "message": "Session response from $serverId"
                            }
                        """.trimIndent())
                        .withFixedDelay(50)
                )
        )
    }
    
    /**
     * Добавление кастомного mock ответа.
     * 
     * @param server WireMock сервер
     * @param urlPattern паттерн URL
     * @param responseBody тело ответа
     * @param statusCode HTTP статус код
     * @param delayMs задержка ответа в миллисекундах
     */
    fun addCustomMockResponse(
        server: WireMockServer,
        urlPattern: String,
        responseBody: String,
        statusCode: Int = 200,
        delayMs: Int = 0
    ) {
        server.stubFor(
            WireMock.get(WireMock.urlMatching(urlPattern))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(statusCode)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)
                        .let { if (delayMs > 0) it.withFixedDelay(delayMs) else it }
                )
        )
    }
    
    /**
     * Остановка всех серверов.
     */
    fun stopAllServers() {
        servers.forEach { server ->
            try {
                if (server.isRunning) {
                    println("Сервер на порту ${server.port()} остановлен")
                    server.stop()
                }
            } catch (e: Exception) {
                println("Ошибка при остановке сервера на порту: ${e.message}")
            }
        }
        servers.clear()
    }
    
    /**
     * Проверка, что все серверы работают.
     * 
     * @return true если все серверы работают
     */
    fun areAllServersRunning(): Boolean {
        return servers.all { it.isRunning }
    }
    
    /**
     * Получение списка портов всех серверов.
     * 
     * @return список портов
     */
    fun getServerPorts(): List<Int> {
        return servers.map { it.port() }
    }
    
    /**
     * Остановка конкретного сервера.
     * 
     * @param port порт сервера для остановки
     */
    fun stopServer(port: Int) {
        servers.find { it.port() == port }?.let { server ->
            try {
                if (server.isRunning) {
                    server.stop()
                    println("Сервер на порту $port остановлен")
                }
            } catch (e: Exception) {
                println("Ошибка при остановке сервера на порту $port: ${e.message}")
            }
        }
    }
}
