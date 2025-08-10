package com.stickybalancer.core.balancer

import org.springframework.boot.autoconfigure.codec.CodecProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Автоконфигурация для StickyLoadBalancer.
 *
 * Этот класс обеспечивает автоматическое создание и настройку компонентов
 * sticky load balancer при наличии в classpath класса StickyLoadBalancer.
 *
 * Основные возможности:
 * - Автоматическое создание StickyLoadBalancerFactory
 * - Интеграция с Spring Boot CodecProperties для настройки WebClient
 * - Условное создание бинов только при необходимости
 *
 * Условия активации:
 * - Класс StickyLoadBalancer должен быть доступен в classpath
 * - Bean StickyLoadBalancerFactory не должен существовать в контексте
 *
 * @see StickyLoadBalancerFactory для создания экземпляров балансировщика
 * @see CodecProperties для настройки кодека WebClient
 */
@Configuration
@ConditionalOnClass(StickyLoadBalancer::class)
@EnableConfigurationProperties(CodecProperties::class)
class StickyLoadBalancerAutoConfiguration {

    /**
     * Создать фабрику для StickyLoadBalancer.
     *
     * Этот bean создается только если в контексте отсутствует
     * другой StickyLoadBalancerFactory. Фабрика используется для
     * создания экземпляров балансировщика с различными конфигурациями.
     *
     * @param applicationContext Spring ApplicationContext для доступа к бинам и свойствам
     * @param codecProperties настройки кодека для WebClient (размер буфера, кодировки)
     * @return настроенная фабрика для создания балансировщиков
     */
    @Bean
    @ConditionalOnMissingBean
    fun stickyLoadBalancerFactory(
        applicationContext: ApplicationContext,
        codecProperties: CodecProperties
    ): StickyLoadBalancerFactory {
        return StickyLoadBalancerFactory(applicationContext, codecProperties)
    }
}