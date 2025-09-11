
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

    implementation("io.ktor:ktor-server-core:2.3.3")
    implementation("io.ktor:ktor-server-netty:2.3.3")
    implementation("io.ktor:ktor-server-call-logging:2.3.3")

    implementation("io.ktor:ktor-client-core:2.3.3")
    implementation("io.ktor:ktor-client-cio:2.3.3") // Oppure `ktor-client-okhttp` o altri engine
    implementation("io.ktor:ktor-client-content-negotiation:2.3.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.3")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    implementation("io.ktor:ktor-server-auth:2.x.x")
    implementation("io.ktor:ktor-server-auth-jwt:2.x.x")
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:5.2.1")
    implementation("org.mongodb:bson-kotlinx:5.2.1")
    implementation("io.ktor:ktor-server-auth-jwt:2.x.x")
    implementation("io.ktor:ktor-server-call-logging-jvm")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-auth-jwt-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    implementation("commons-codec:commons-codec:$commons_codec_version")
    implementation("com.sun.mail:javax.mail:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

}
