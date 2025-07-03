plugins {
    id("java")
    kotlin("jvm") version "1.9.0"
    id("net.minecrell.plugin-yml.paper") version "0.6.0"
    id("com.github.johnrengelman.shadow") version "8.1.1" // 新增 shadow 插件
}

repositories {
    mavenCentral() // Maven Central
    maven("https://repo.papermc.io/repository/maven-public/") // Paper
    maven("https://minevolt.net/repo/") // PlaceholderAPI
}

dependencies {
    implementation(kotlin("stdlib")) // 改为 implementation 确保打包
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.3")
    annotationProcessor("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
}

tasks {
    shadowJar {
        // 重命名 shadow 包以避免冲突
        archiveClassifier.set("")
        // 排除不必要的依赖
        minimize()
    }
    build {
        dependsOn(shadowJar)
    }
}

paper {
    name = "ServerShoutBridgeSpigot"
    main = "com.houheya.spigot.ServerShoutBridgeSpigot"
    author = "houheya"
    version = "1.0.0"
    apiVersion = "1.19"

    serverDependencies {
        register("PlaceholderAPI") {
            required = false
            load = net.minecrell.pluginyml.paper.PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
    }
}