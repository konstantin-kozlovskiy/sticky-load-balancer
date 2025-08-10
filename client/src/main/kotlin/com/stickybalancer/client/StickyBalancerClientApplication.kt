package com.stickybalancer.client

import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EnableFeignClients(basePackages = ["com.stickybalancer.client"])
class StickyBalancerClientApplication

fun main(args: Array<String>) {
    runApplication<StickyBalancerClientApplication>(*args)
}
