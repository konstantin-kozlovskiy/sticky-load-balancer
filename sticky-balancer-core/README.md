# Sticky Balancer Core

Основная библиотека для sticky load balancing между серверами.

## Описание

Этот модуль содержит всю логику для балансировки трафика между инстансами серверов с гарантией того, что запросы с одинаковым `transactionId` и временем начала операции попадут на один и тот же сервер.

## Основные компоненты

### Модели

- `ServerInfo` - информация о сервере (id, url, name)
- `ServerConfig` - конфигурация сервера

### Сервисы

- `StickyLoadBalancerService` - основной сервис для выбора сервера
- `ServerHealthManager` - интерфейс для управления здоровьем серверов
- `SpringServerHealthManager` - Spring-специфичная реализация

### Конфигурация

- `StickyBalancerConfig` - Spring конфигурация для автоматического создания бинов

## Использование

### Добавление зависимости

```kotlin
dependencies {
    implementation(project(":sticky-balancer-core"))
}
```

### Конфигурация в application.yml

```yaml
sticky-balancer:
  servers:
    - id: "server1"
      url: "http://localhost:9091"
      name: "Server 1"
    - id: "server2"
      url: "http://localhost:9092"
      name: "Server 2"
  health-check-interval-seconds: 5
```

### Использование в коде

```kotlin
@Service
class PaymentService(
    private val stickyLoadBalancerService: StickyLoadBalancerService
) {
    
    fun processPayment(transactionId: String, operationStartTime: Instant) {
        val selectedServer = stickyLoadBalancerService.selectServer(transactionId, operationStartTime)
        // Используем выбранный сервер для отправки запроса
    }
}
```

## Принцип работы

1. **Sticky Routing**: Запросы с одинаковым `transactionId` и временем начала операции всегда попадают на один сервер
2. **Health Checking**: Автоматическая проверка здоровья серверов через `/actuator/health` endpoint
3. **Death Timestamp**: Запоминание времени выхода из балансировки одного из серверов для корректной обработки операций, начатых до падения
4. **Hash-based Selection**: Выбор сервера на основе хеша от `transactionId`

## Требования

- Java 17+
- Spring Boot 3.2.0+
- Kotlin 1.9.0+
