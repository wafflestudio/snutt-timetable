plugins {
    id("java-test-fixtures")
}

dependencies {
    implementation("org.springframework.security:spring-security-crypto")
    implementation("software.amazon.awssdk:s3:2.25.15")
    implementation("software.amazon.awssdk:secretsmanager:2.25.15")
    implementation("software.amazon.awssdk:sts:2.25.15")
    implementation("software.amazon.awssdk:ses:2.25.15")
    implementation("com.google.firebase:firebase-admin:9.1.1")

    testFixturesImplementation("org.testcontainers:mongodb:1.19.0")
    testFixturesImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-test")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    testFixturesImplementation("org.springframework.boot:spring-boot-testcontainers")
}
