package com.stickybalancer.core.balancer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServiceInstanceTest {

    @Test
    fun testCreateInstanceWithCorrectParameters() {
        // given
        val mockServiceClient = mock<TestServiceClient>()
        val mockHealthCheckClient = mock<HealthCheckClient>()
        val name = "TestService"
        val url = "http://test-server:8080"

        // when
        val instance = ServiceInstance(mockServiceClient, mockHealthCheckClient, name, url)

        // then
        assertEquals(mockServiceClient, instance.serviceClient)
        assertEquals(mockHealthCheckClient, instance.healthCheckClient)
        assertEquals("UP", instance.getStatusAt(Instant.now()))
    }

    @Test
    fun testCreateInstanceWithCustomStatus() {
        // given
        val mockServiceClient = mock<TestServiceClient>()
        val mockHealthCheckClient = mock<HealthCheckClient>()
        val name = "TestService"
        val url = "http://test-server:8080"
        val customStatus = "DOWN"

        // when
        val instance = ServiceInstance(mockServiceClient, mockHealthCheckClient, name, url, customStatus)

        // then
        assertEquals(customStatus, instance.getStatusAt(Instant.now()))
    }

    @Test
    fun testChangeStatusAndAddToHistory() {
        // given
        val mockServiceClient = mock<TestServiceClient>()
        val mockHealthCheckClient = mock<HealthCheckClient>()
        val instance = ServiceInstance(mockServiceClient, mockHealthCheckClient, "TestService", "http://test-server:8080")
        val timestamp = Instant.now()

        // when
        instance.changeStatus("DOWN")
        val statusAtTimestamp = instance.getStatusAt(timestamp)

        // then
        assertEquals("DOWN", statusAtTimestamp)
    }

    @Test
    fun testShouldNotAddDuplicateStatusToHistory() {
        // given
        val mockServiceClient = mock<TestServiceClient>()
        val mockHealthCheckClient = mock<HealthCheckClient>()
        val instance = ServiceInstance(mockServiceClient, mockHealthCheckClient, "TestService", "http://test-server:8080")
        val initialTimestamp = Instant.now()

        // when
        instance.changeStatus("UP") // Тот же статус
        val statusAtTimestamp = instance.getStatusAt(initialTimestamp)

        // then
        assertEquals("UP", statusAtTimestamp)
    }

    @Test
    fun testShouldReturnCorrectStatusAtSpecifiedTime() {
        // given
        val mockServiceClient = mock<TestServiceClient>()
        val mockHealthCheckClient = mock<HealthCheckClient>()
        val instance = ServiceInstance(mockServiceClient, mockHealthCheckClient, "TestService", "http://test-server:8080")

        val timestamp1 = Instant.now()
        
        // when
        Thread.sleep(1)
        instance.changeStatus("DOWN")
        val timestamp2 = Instant.now()
        Thread.sleep(1)
        instance.changeStatus("UP")
        val timestamp3 = Instant.now()

        // then
        assertEquals("UP", instance.getStatusAt(timestamp1)) // Изначальный статус
        assertEquals("DOWN", instance.getStatusAt(timestamp2)) // Статус после первого изменения
        assertEquals("UP", instance.getStatusAt(timestamp3)) // Статус после второго изменения
    }

    @Test
    fun testShouldReturnUpByDefaultForTimeBeforeInstanceCreation() {
        // given
        val mockServiceClient = mock<TestServiceClient>()
        val mockHealthCheckClient = mock<HealthCheckClient>()
        val instance = ServiceInstance(mockServiceClient, mockHealthCheckClient, "TestService", "http://test-server:8080")
        val pastTimestamp = Instant.now().minusSeconds(3600) // 1 час назад

        // when
        val status = instance.getStatusAt(pastTimestamp)

        // then
        assertEquals("UP", status)
    }

    @Test
    fun testShouldHandleToStringCorrectly() {
        // given
        val mockServiceClient = mock<TestServiceClient>()
        val mockHealthCheckClient = mock<HealthCheckClient>()
        val name = "TestService"
        val url = "http://test-server:8080"
        val instance = ServiceInstance(mockServiceClient, mockHealthCheckClient, name, url)

        // when
        val result = instance.toString()

        // then
        assertEquals("[$name: $url]", result)
    }

    @Test
    fun testShouldLimitStatusHistorySize() {
        // given
        val mockServiceClient = mock<TestServiceClient>()
        val mockHealthCheckClient = mock<HealthCheckClient>()
        val instance = ServiceInstance(mockServiceClient, mockHealthCheckClient, "TestService", "http://test-server:8080")

        // when - добавляем больше 1000 статусов
        repeat(1101) { index ->
            instance.changeStatus("STATUS_$index")
            Thread.sleep(1)
        }

        // then - история должна быть ограничена
        assertEquals(1000, instance.getStatuses().size)
    }

    // Вспомогательный интерфейс для тестирования
    interface TestServiceClient
}
