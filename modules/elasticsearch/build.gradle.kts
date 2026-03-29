plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-data-elasticsearch")

    testFixturesImplementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
    testFixturesImplementation("org.testcontainers:elasticsearch")
}
