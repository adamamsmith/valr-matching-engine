plugins {
    application
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "1.8.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "smith.adam"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("smith.adam.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    val vertxVersion = "4.5.9"
    implementation("io.vertx:vertx-core:${vertxVersion}")
    implementation("io.vertx:vertx-lang-kotlin:${vertxVersion}")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:${vertxVersion}")
    implementation("io.vertx:vertx-web:${vertxVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("com.charleskorn.kaml:kaml-jvm:0.61.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}