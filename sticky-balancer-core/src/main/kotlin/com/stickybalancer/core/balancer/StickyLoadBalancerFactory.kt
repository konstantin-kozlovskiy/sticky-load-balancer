package com.stickybalancer.core.balancer

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.codec.CodecProperties
import org.springframework.cloud.openfeign.support.SpringMvcContract
import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactivefeign.ReactiveContract
import reactivefeign.spring.config.ReactiveFeignClient
import reactivefeign.webclient.WebReactiveFeign
import reactivefeign.webclient.WebReactiveOptions

/**
 * Фабрика для создания экземпляров StickyLoadBalancer.
 *
 * Этот класс инкапсулирует логику создания и настройки балансировщиков нагрузки,
 * включая создание WebClient, настройку таймаутов и инициализацию Feign клиентов.
 *
 * Основные возможности:
 * - Автоматическое создание WebClient с фильтрами
 * - Настройка таймаутов из конфигурации
 * - Создание Feign клиентов для сервисов и health check
 * - Интеграция с Spring Boot конфигурацией
 *
 * @param applicationContext Spring ApplicationContext для доступа к бинам и свойствам
 * @param codecProperties настройки кодека для WebClient
 */
@Component
class StickyLoadBalancerFactory(
    private val applicationContext: ApplicationContext,
    private val codecProperties: CodecProperties
) {
    private val environment: Environment = applicationContext.environment
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Создать новый экземпляр StickyLoadBalancer для указанного клиента.
     *
     * Метод автоматически создает все необходимые компоненты:
     * - WebClient с настроенными фильтрами и кодеком
     * - Feign клиент для основного сервиса
     * - Feign клиент для health check
     * - ServiceInstance для каждого URL
     *
     * @param T тип клиента для взаимодействия с серверами
     * @param clientClass класс клиента (должен быть помечен @ReactiveFeignClient)
     * @param urls список конфигураций серверных экземпляров
     * @return настроенный StickyLoadBalancer
     * @throws IllegalArgumentException если аннотация @ReactiveFeignClient некорректна
     */
    fun <T> create(
        clientClass: Class<T>,
        urls: List<ServiceInstanceConfiguration>
    ): StickyLoadBalancer<T> {

        val instances = urls.map { createServiceInstance(clientClass, it) }.toList()

        return StickyLoadBalancer(
            clientName = clientClass.simpleName,
            instances = instances,
        )
    }

    /**
     * Создать экземпляр ServiceInstance для указанной конфигурации.
     *
     * Метод выполняет полную настройку:
     * - Создание WebClient с фильтрами и кодеком
     * - Извлечение настроек таймаутов из @ReactiveFeignClient аннотации
     * - Создание Feign клиента для основного сервиса
     * - Создание Feign клиента для health check
     * - Логирование созданного экземпляра
     *
     * @param T тип клиента для взаимодействия с серверами
     * @param clientClass класс клиента
     * @param config конфигурация серверного экземпляра
     * @return настроенный ServiceInstance
     * @throws IllegalArgumentException если аннотация @ReactiveFeignClient некорректна
     */
    private fun <T> createServiceInstance(
        clientClass: Class<T>,
        config: ServiceInstanceConfiguration
    ): ServiceInstance<T> {
        val clientName: String = clientClass.simpleName

        val maxInMemoryBytes = codecProperties.maxInMemorySize?.toBytes()?.toInt()

        val webClient = WebClient.builder()
            .filter(HeaderPropagationFilterFunction(applicationContext))
            .codecs { configurer -> maxInMemoryBytes?.let { configurer.defaultCodecs().maxInMemorySize(it) } }

        val reactiveFeignClientAnnotation = clientClass.annotations
            .filterIsInstance<ReactiveFeignClient>()
            .firstOrNull()

        val configName = reactiveFeignClientAnnotation?.let { annotation ->
            if (annotation.value.isNotBlank()) annotation.value
            else if (annotation.name.isNotBlank()) annotation.name
            else null
        }
            ?: throw IllegalArgumentException("Invalid @ReactiveFeignClient, value or name is null or blank: $clientClass, fix annotation")

        val readTimeoutMillis = environment.getProperty(
            "reactive.feign.client.config.$configName.options.read-timeout-millis",
            "10000"
        ).toLong()

        val writeTimeoutMillis = environment.getProperty(
            "reactive.feign.client.config.$configName.options.write-timeout-millis",
            "10000"
        ).toLong()

        val connectTimeoutMillis = environment.getProperty(
            "reactive.feign.client.config.$configName.options.connect-timeout-millis",
            "5000"
        ).toLong()

        val serviceClient = WebReactiveFeign.builder<T>(webClient)
            .contract(ReactiveContract(SpringMvcContract()))
            .options(
                WebReactiveOptions.Builder()
                    .setReadTimeoutMillis(readTimeoutMillis)
                    .setWriteTimeoutMillis(writeTimeoutMillis)
                    .setConnectTimeoutMillis(connectTimeoutMillis)
                    .build()
            )
            .target(clientClass, config.serviceUrl)

        val healthCheckClient = WebReactiveFeign
            .builder<HealthCheckClient>()
            .contract(ReactiveContract(SpringMvcContract()))
            .target(HealthCheckClient::class.java, config.healthCheckUrl)

        val instance = ServiceInstance(serviceClient, healthCheckClient, clientName, config.serviceUrl)
        logger.info("$instance Initialized StickyLoadBalancer with readTimeout=${readTimeoutMillis}ms, writeTimeout=${writeTimeoutMillis}ms, connectTimeout=${connectTimeoutMillis}ms")
        return instance
    }

}
