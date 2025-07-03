plugins {
    id("java")
    kotlin("jvm") version "1.9.0" // Kotlin 支持
    id("net.minecrell.plugin-yml.bungee") version "0.6.0"
}

repositories {
    mavenCentral() // Maven Central
    maven("https://repo1.maven.org/maven2/") // BungeeCord
    maven("https://minevolt.net/repo/")
}

dependencies {
    implementation("net.md-5:bungeecord-api:1.21-R0.3") // 全版本兼容
}

bungee {
    name = "ServerShoutBridgeBungee"
    main = "com.houheya.bungee.ServerShoutBridgeBungee"
    author = "houheya"
    version = "1.0.0"
}