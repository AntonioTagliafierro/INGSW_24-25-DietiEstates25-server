
val kotlin_version: String by project
val logback_version: String by project
val mongo_version: String by project
val commons_codec_version: String by project

plugins {
    kotlin("jvm") version "2.1.0"
    id("io.ktor.plugin") version "3.0.2"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
}

group = "com"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-serialization-jackson:3.0.2")
    implementation("org.litote.kmongo:kmongo-coroutine-serialization:4.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.7.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    // Allinea tutte le lib Ktor alla stessa versione della plugin
    implementation(platform("io.ktor:ktor-bom:3.0.2"))

    // Ktor server
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")

    // (Solo se questo modulo fa anche da client HTTP)
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-cio")
    implementation("io.ktor:ktor-client-content-negotiation")

    // Coroutines (incluso REACTIVE per Mongo)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.8.1")

    // Serialization (coerente con ktor-serialization)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Mongo Kotlin Coroutine Driver
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:$mongo_version")
    implementation("org.mongodb:bson-kotlinx:$mongo_version")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("org.fusesource.jansi:jansi:2.4.1") // colori ANSI

    // Extra
    implementation("commons-codec:commons-codec:$commons_codec_version")
    implementation("com.sun.mail:javax.mail:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // Test
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}

