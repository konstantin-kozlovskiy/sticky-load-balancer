package com.stickybalancer.core.balancer

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.codec.CodecProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnClass(StickyLoadBalancer::class)
@EnableConfigurationProperties(CodecProperties::class)
class StickyLoadBalancerAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    fun stickyLoadBalancerFactory(
        applicationContext: ApplicationContext, 
        codecProperties: CodecProperties
    ): StickyLoadBalancerFactory {
        return StickyLoadBalancerFactory(applicationContext, codecProperties)
    }
}