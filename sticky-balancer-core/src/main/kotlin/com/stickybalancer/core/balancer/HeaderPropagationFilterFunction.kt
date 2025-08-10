package com.stickybalancer.core.balancer

import org.springframework.context.ApplicationContext
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono

/**
 * Функция-фильтр для передачи и генерации заголовков при выполнении WEB запроса.
 */
class HeaderPropagationFilterFunction(
    private val applicationContext: ApplicationContext
) : ExchangeFilterFunction {


    override fun filter(clientRequest: ClientRequest, exchangeFunction: ExchangeFunction): Mono<ClientResponse> =
        exchangeFunction.exchange(
            ClientRequest
                .from(clientRequest)
                .build()
        )

}
