package com.stickybalancer.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class StickyBalancerServerApplication

fun main(args: Array<String>) {
    runApplication<StickyBalancerServerApplication>(*args)
}
