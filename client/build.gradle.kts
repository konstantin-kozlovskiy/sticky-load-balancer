dependencies {
    implementation(project(":sticky-balancer-core"))
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Playtika Reactive Feign
    implementation("com.playtika.reactivefeign:feign-reactor-spring-configuration:4.2.1")
    implementation("com.playtika.reactivefeign:feign-reactor-cloud:4.2.1")
    implementation("com.playtika.reactivefeign:feign-reactor-webclient:4.2.1")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.3")
    }
}

