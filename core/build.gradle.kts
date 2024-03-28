plugins {
    id("java-test-fixtures")
}

dependencies {
    implementation("org.springframework.security:spring-security-crypto")
    implementation("software.amazon.awssdk:secretsmanager:2.17.276")
    implementation("software.amazon.awssdk:sts:2.17.276")
    implementation("com.google.firebase:firebase-admin:9.1.1")
    implementation("com.slack.api:bolt-jetty:1.37.0")
    implementation("com.slack.api:slack-api-model-kotlin-extension:1.21.0")
    implementation("com.slack.api:slack-api-client-kotlin-extension:1.21.0")

    testFixturesImplementation("org.testcontainers:mongodb:1.19.0")
    testFixturesImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-test")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    testFixturesImplementation("org.springframework.boot:spring-boot-testcontainers")
}
