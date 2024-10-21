val dockerLogin: String by project
val dockerPassword: String by project

plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    id("io.ktor.plugin") version "3.0.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

    val ktorVersion = "3.0.0"
    val exposedVersion = "0.55.0"

    testImplementation(kotlin("test"))

    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation(project.dependencies.platform("io.insert-koin:koin-bom:4.0.0"))
    implementation("io.insert-koin:koin-core:4.0.0")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation(project(":shared_domain"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(19)
}
ktor {
    docker {

        localImageName.set("madprojects-messenger-docker-image")
        jib {
            from {
                image = "openjdk:17-jdk-alpine"
            }
            to {
                image = "${dockerLogin}/madprojects"
                tags = setOf("${project.version}")
            }
        }

        externalRegistry.set(
            io.ktor.plugin.features.DockerImageRegistry.dockerHub(
                appName = provider { "ktor-app" },
                username = providers.environmentVariable(dockerLogin),
                password = providers.environmentVariable(dockerPassword)
            )
        )
    }
}