# Sticky Load Balancer

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.java.net/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.8+-blue.svg)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0+-green.svg)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-8.0+-purple.svg)](https://gradle.org/)

## Описание / Description

**Sticky Load Balancer** - POC балансировки нагрузки с поддержкой "липких" сессий, построенная на Kotlin, Spring Boot,
Reactive Feign Client. Система обеспечивает стабильное распределение запросов между серверами с сохранением контекста
пользовательских сессий.

## 🏗️ Архитектура / Architecture

Проект состоит из трех основных модулей:

```
sticky-balancer/
├── sticky-balancer-core/     # Библиотека с основной логикой
├── client/                   # Клиентское приложение для генерации нагрузки (проведение платежей из 2ух этапов)
└── server/                   # Серверное stateful приложение для обработки запросов (из 2ух этапов)
```

### Core Module (`sticky-balancer-core`)

Основная библиотека, содержащая:

- `StickyLoadBalancer` - основной класс балансировщика
- `StickyLoadBalancerFactory` - фабрика для создания балансировщиков
- `ServiceInstance` - модель серверного экземпляра
- `HealthCheckClient` - клиент для проверки здоровья серверов
- Авто-конфигурация Spring Boot

### Client Module

Демонстрационное клиентское приложение с:

- Многопоточным сервисом для тестирования
- Конфигурацией sticky load balancer
- Примером использования

### Server Module

Демонстрационный сервер с:

- REST API для платежей
- Health check endpoints
- Моделями данных

## 🚀 Быстрый старт / Quick Start

### Требования / Requirements

- Java 17+
- Kotlin 1.8+
- Spring Boot 3.0+
- Gradle 8.7+

### Установка / Installation

1. **Клонируйте репозиторий:**

```powershell
git clone git@github.com:konstantin-kozlovskiy/sticky-load-balancer.git
cd sticky-load-balancer
```

2. **Соберите проект:**

```powershell
.\gradlew.bat build
```

### Создание балансировщика / Creating Load Balancer

```kotlin
@ReactiveFeignClient("payment", url = "http://localhost:9091")
interface PaymentClient {
    @PostMapping("/api/v1/check")
    fun checkPayment(@RequestBody request: PaymentRequestDto): Mono<PaymentResponseDto>

    @PostMapping("/api/v1/pay")
    fun processPayment(@PathVariable paymentId: String): Mono<PaymentResponseDto>
}


@Configuration
class LoadBalancerConfig {

    @Bean
    fun stickyLoadBalancer(
        factory: StickyLoadBalancerFactory
    ): StickyLoadBalancer<PaymentClient> {
        return factory.create(
            PaymentClient::class.java,
            listOf(
                ServiceInstanceConfiguration(
                    serviceUrl = "http://localhost:9091",
                    healthCheckUrl = "http://localhost:9091"
                ),
                ServiceInstanceConfiguration(
                    serviceUrl = "http://localhost:9092",
                    healthCheckUrl = "http://localhost:9092"
                )
            )
        )
    }
}
```

### Использование балансировщика / Using Load Balancer

```kotlin
@Service
class PaymentService(
    private val loadBalancer: StickyLoadBalancer<PaymentClient>
) {

    fun processPayment(requestId: String, request: PaymentRequest): Mono<PaymentResponse> {
        return loadBalancer.execute(
            requestId = requestId,
            requestTimestamp = Instant.now()
        ) { client ->
            client.processPayment(request)
        }
    }
}
```

## ⚙️ Конфигурация / Configuration

### Настройки таймаутов / Timeout Settings

```yaml
reactive:
  feign:
    client:
      config:
        payment:
          options:
            read-timeout-millis: 10000
            write-timeout-millis: 10000
            connect-timeout-millis: 5000
```

### Настройки кодека / Codec Settings

```yaml
spring:
  codec:
    max-in-memory-size: 10MB
```

## 🔧 Особенности / Features

- **Sticky Sessions** - запросы с одинаковым ID направляются на один сервер
- **Graceful shutdown** - последующие запросы с тем же ID будут отправляться на тот же сервер (без учета health-check)
- **Graceful start** - запросы начатые до ввода новых инстансов в эксплуатацию будут продолжены там же где и начаты
- **Отсутствие внешних зависимостей** - не требуется внешнее хранилище для sticky-sessions, умный алгоритм на основе
  журнала статусов решает эту проблему
- **Health Checks** - автоматическая проверка здоровья серверов каждые 15 секунд
- **Failover** - автоматическое переключение на доступные серверы
- **Reactive Support** - поддержка реактивного программирования
- **Web filters** - поддержка веб-фильтров
- **Timeout** - поддержка настройки таймаутов

## 🧪 Тестирование / Testing

1. **Запустите сервер:**

```powershell
java "-Dserver.port=9091" -jar .\server\build\libs\server-1.0.0.jar
java "-Dserver.port=9092" -jar .\server\build\libs\server-1.0.0.jar
```

2. **Запустите клиент:**

```powershell
java -jar .\client\build\libs\client-1.0.0.jar
```

3. **Трафик равномерно будет распределяться между двумя серверами**

4. **Вывести один из серверов из балансировки через отключение health-check**

```powershell
Invoke-RestMethod -Uri "http://localhost:9091/health/down" -Method POST
```

5. **Клиент закончит текущие запросы и перестанет отправлять новые на сервер с отключенным health-check**

6. **Перезапустить отключенный инстанс - трафик восстановится на оба экземпляра сервера**

## 📄 Лицензия / License

Этот проект распространяется под лицензией MIT. См. файл `LICENSE` для деталей.

## 👨‍💻 Автор / Author

**Константин Козловский** / **Konstantin Kozlovskiy**

- GitHub: [@konstantin-kozlovskiy](https://github.com/konstantin-kozlovskiy)

## 🔗 Ссылки / Links

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Reactive Feign](https://github.com/Playtika/reactivefeign)
- [Spring WebFlux](https://docs.spring.io/spring-framework/reference/web/webflux.html)

