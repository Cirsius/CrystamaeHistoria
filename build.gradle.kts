plugins {
    java
    id("com.gradleup.shadow") version "8.3.6"
}

group = "io.github.sefiraat.crystamaehistoria"
version = "MODIFIED"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        content {
            includeGroup("io.papermc.paper")
            includeGroup("net.md-5")
        }
    }
    maven("https://libraries.minecraft.net") {
        content { includeGroup("com.mojang") }
    }
    maven("https://jitpack.io") {
        content {
            includeGroupByRegex("com\\.github\\..*")
            includeGroup("io.github.mooy1")
        }
    }
    maven("https://nexus.neetgames.com/repository/maven-public/") {
        content { includeGroup("com.gmail.nossr50.mcMMO") }
    }
    maven("https://hub.jeff-media.com/nexus/repository/jeff-media-public/") {
        content { includeGroup("com.jeff-media") }
    }
    maven("https://sefiraat.jfrog.io/artifactory/default-maven-local") {
        content { includeGroup("com.github.Sefiraat") }
    }
    maven("https://repo.bg-software.com/repository/api/") {
        content { includeGroup("com.bgsoftware") }
    }
    maven("https://repo.rosewooddev.io/repository/public/") {
        content { includeGroup("dev.rosewood") }
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    compileOnly("com.github.Cirsius:Slimefun4:51d4c41bf")

    implementation("org.bstats:bstats-bukkit:3.0.0")
    implementation("com.google.code.findbugs:annotations:3.0.1u2")
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    implementation("com.jeff-media:MorePersistentDataTypes:2.4.0")

    compileOnly("com.github.TheBusyBiscuit:ExoticGarden:a2c4b6d")
    compileOnly("com.gmail.nossr50.mcMMO:mcMMO:2.1.217") {
        isTransitive = false
    }
    compileOnly("com.github.Sefiraat:Networks:3de3c9d608")
    implementation("com.elmakers.mine.bukkit:EffectLib:10.4")
    compileOnly("com.bgsoftware:WildStackerAPI:2022.6")
    compileOnly("dev.rosewood:rosestacker:1.5.1")
    compileOnly("com.github.Sefiraat:Netheopoiesis:8d1af6c570")
    implementation("io.github.mooy1:InfinityLib:7e03c79")
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    processResources {
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand("project" to project)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("${project.name} v${project.version}.jar")
        relocate("io.github.mooy1.infinitylib", "io.github.sefiraat.crystamaehistoria.infinitylib")
        relocate("org.bstats", "io.github.sefiraat.crystamaehistoria.bstats")
        relocate("de.slikey", "io.github.sefiraat.crystamaehistoria.slikey")
        exclude("META-INF/**")
    }
}
