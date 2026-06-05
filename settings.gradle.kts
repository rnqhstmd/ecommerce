// configurations
pluginManagement {
    val springBootVersion: String by settings
    val springDependencyManagementVersion: String by settings

    repositories {
        maven { url = uri("https://repo.spring.io/milestone") }
        maven { url = uri("https://repo.spring.io/snapshot") }
        gradlePluginPortal()
    }

    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "org.springframework.boot" -> useVersion(springBootVersion)
                "io.spring.dependency-management" -> useVersion(springDependencyManagementVersion)
            }
        }
    }
}

// JDK 21 toolchain 자동 프로비저닝 (로컬에 21이 없을 때 Gradle이 다운로드)
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "ecommerce"

include(
    ":apps:commerce-api",
    ":apps:commerce-streamer",
    ":modules:jpa",
    ":modules:redis",
    ":modules:kafka",
    ":modules:elasticsearch",
    ":supports:jackson",
    ":supports:logging",
    ":supports:monitoring",
)
