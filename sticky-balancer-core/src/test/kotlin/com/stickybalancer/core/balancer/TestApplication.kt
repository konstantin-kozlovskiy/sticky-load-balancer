package com.stickybalancer.core.balancer

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.boot.autoconfigure.codec.CodecProperties

@SpringBootApplication
class TestApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(TestApplication::class.java, *args)
        }
    }
}

@Configuration
class TestConfiguration {
    
    @Bean
    fun codecProperties(): CodecProperties {
        return CodecProperties()
    }
    
    @Bean
    fun webClient(): WebClient {
        return WebClient.builder().build()
    }
}
