dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    implementation("org.springframework.security:spring-security-crypto")
    implementation("software.amazon.awssdk:secretsmanager:2.17.276")
    implementation("software.amazon.awssdk:sts:2.17.276")

    testRuntimeOnly("de.flapdoodle.embed:de.flapdoodle.embed.mongo.spring30x:4.3.2")
}
