package com.stickybalancer.core.balancer

import org.springframework.context.ApplicationContext
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono

/**
 * Функция-фильтр для передачи и генерации заголовков при выполнении WEB запроса.
 *
 * Этот класс реализует ExchangeFilterFunction для WebClient, позволяя
 * модифицировать запросы и ответы на уровне фильтра. В текущей реализации
 * фильтр просто передает запрос без изменений, но может быть расширен
 * для добавления пользовательских заголовков, логирования, метрик и т.д.
 *
 * Основные возможности:
 * - Перехват всех исходящих HTTP запросов
 * - Модификация заголовков запроса
 * - Добавление пользовательской логики (логирование, метрики)
 * - Интеграция с Spring ApplicationContext
 *
 * @param applicationContext Spring ApplicationContext для доступа к бинам и свойствам
 * @see ExchangeFilterFunction для базового интерфейса фильтра
 */
class HeaderPropagationFilterFunction(
    private val applicationContext: ApplicationContext
) : ExchangeFilterFunction {

    /**
     * Обработать HTTP запрос через фильтр.
     *
     * Метод перехватывает исходящий запрос и может модифицировать его
     * перед отправкой. В текущей реализации запрос передается без изменений,
     * но может быть расширен для:
     * - Добавления пользовательских заголовков
     * - Логирования запросов
     * - Добавления метрик
     * - Модификации URL или параметров
     *
     * @param clientRequest исходный HTTP запрос
     * @param exchangeFunction функция для выполнения запроса
     * @return Mono<ClientResponse> результат выполнения запроса
     */
    override fun filter(clientRequest: ClientRequest, exchangeFunction: ExchangeFunction): Mono<ClientResponse> =
        exchangeFunction.exchange(
            ClientRequest
                .from(clientRequest)
                .build()
        )
}
