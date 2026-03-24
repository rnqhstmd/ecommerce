dependencies {
    // add-ons
    implementation(project(":modules:jpa"))
    implementation(project(":modules:kafka"))
    implementation(project(":modules:redis"))
    implementation(project(":supports:jackson"))
    implementation(project(":supports:logging"))
    implementation(project(":supports:monitoring"))

    // web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // security + jwt
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${project.properties["springDocOpenApiVersion"]}")

    // redisson (distributed lock)
    implementation("org.redisson:redisson-spring-boot-starter:${project.properties["redissonVersion"]}")

    // resilience4j
    implementation("io.github.resilience4j:resilience4j-spring-boot3:${project.properties["resilience4jVersion"]}")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // querydsl
    annotationProcessor("com.querydsl:querydsl-apt::jakarta")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")

    // test-fixtures
    testImplementation(testFixtures(project(":modules:jpa")))
    testImplementation(testFixtures(project(":modules:redis")))

    // kafka test
    testImplementation("org.springframework.kafka:spring-kafka-test")

    // security test
    testImplementation("org.springframework.security:spring-security-test")
}
