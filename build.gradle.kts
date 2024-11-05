plugins {
    application
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "1.8.20"
}

group = "smith.adam"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

repositories {
    mavenCentral()
}

dependencies {
    val vertxVersion = "4.5.9"
    val kTorVersion = "3.0.0"
    implementation("io.vertx:vertx-core:${vertxVersion}")
    implementation("io.vertx:vertx-kotlin:${vertxVersion}")
    implementation("io.vertx:vertx-kotlin-coroutines:${vertxVersion}")
    implementation("io.ktor:ktor-server-config-yaml:${kTorVersion}")
    implementation("io.ktor:ktor-server-core:${kTorVersion}")
    implementation("io.ktor:ktor-server-netty:${kTorVersion}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${kTorVersion}")
    implementation("io.ktor:ktor-server-html-builder:${kTorVersion}")
    implementation("io.ktor:ktor-server-content-negotiation:${kTorVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host-jvm:3.0.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}