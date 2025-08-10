package com.stickybalancer.client.configuration

import com.stickybalancer.client.client.PaymentClient
import com.stickybalancer.core.balancer.StickyLoadBalancer
import com.stickybalancer.core.balancer.StickyLoadBalancerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class StickyLoadBalancerConfiguration(
    private val clientConfiguration: ClientConfiguration,
    private val stickyLoadBalancerFactory: StickyLoadBalancerFactory,
) {
    @Bean
    fun paymentClientStickyLoadBalancer(): StickyLoadBalancer<PaymentClient> {
        return stickyLoadBalancerFactory.create(PaymentClient::class.java, clientConfiguration.servers)
    }
}