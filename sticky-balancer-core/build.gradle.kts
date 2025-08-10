/**
 * Конфигурация сборки для модуля sticky-balancer-core.
 *
 * Этот модуль содержит основную логику sticky load balancer,
 * включая балансировщик, фабрику и вспомогательные классы.
 */

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin стандартная библиотека
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Spring Boot WebFlux для реактивных HTTP клиентов
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Spring Boot Actuator для health check endpoints
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Reactor Kotlin расширения для удобной работы с Mono/Flux
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")

    // Playtika Reactive Feign для создания реактивных HTTP клиентов
    implementation("com.playtika.reactivefeign:feign-reactor-spring-configuration:4.2.1")
    implementation("com.playtika.reactivefeign:feign-reactor-cloud:4.2.1")
    implementation("com.playtika.reactivefeign:feign-reactor-webclient:4.2.1")

    // SLF4J для логирования
    implementation("org.slf4j:slf4j-api:2.0.9")

    // Тестовые зависимости
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    
    // WireMock для интеграционного тестирования
    testImplementation("org.wiremock:wiremock-standalone:3.4.2")
}

// Отключаем bootJar для библиотеки (не создаем исполняемый JAR)
tasks.bootJar {
    enabled = false
}

// Включаем создание обычного JAR файла для библиотеки
tasks.jar {
    enabled = true
}

// Настройки компиляции Kotlin
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        // Строгая проверка JSR-305 аннотаций для null safety
        freeCompilerArgs += "-Xjsr305=strict"
        // Целевая версия JVM
        jvmTarget = "17"
    }
}

// Настройки тестирования
tasks.withType<Test> {
    useJUnitPlatform()
}
