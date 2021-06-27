import org.jetbrains.kotlin.config.KotlinCompilerVersion
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.3.72"
    id("com.diffplug.gradle.spotless") version "3.29.0"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

description = "Provides wonderful wands for your Paper/Spigot server."
group = "dev.bloodstone"
version = "0.1.3"

val mcVersion = "1.16.1-R0.1-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/public/")
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://repo.codemc.org/repository/maven-public") //bstats
}

/**
 * Special configuration to be included in resulting shadowed jar, but not added to the generated pom and gradle
 * metadata files.
 */
val shadowImplementation by configurations.creating
configurations["compileOnly"].extendsFrom(shadowImplementation)
configurations["testImplementation"].extendsFrom(shadowImplementation)

dependencies {
    compileOnly(group = "org.spigotmc", name = "spigot-api", version = mcVersion)
    shadowImplementation(kotlin("stdlib", KotlinCompilerVersion.VERSION))
    shadowImplementation(group = "co.aikar", name = "acf-paper", version = "0.5.0-SNAPSHOT")
    shadowImplementation(group = "dev.bloodstone", name = "mcutils", version = "0.0.2")
    shadowImplementation(group = "ch.jalu", name = "configme", version = "1.1.0")
    shadowImplementation(group = "org.bstats", name = "bstats-bukkit", version = "1.7")
}

val shadowJarTask = tasks.named<ShadowJar>("shadowJar")
val relocateShadowJarTask = tasks.register<ConfigureShadowRelocation>("relocateShadowJar") {
    target = shadowJarTask.get()
    prefix = "${rootProject.property("group")}.${rootProject.property("name").toString().toLowerCase()}.lib"
}

shadowJarTask.configure {
    dependsOn(relocateShadowJarTask)
    minimize()
    archiveClassifier.set("")
    configurations = listOf(shadowImplementation)
}

tasks {
    build {
        dependsOn(shadowJar)
    }
    compileJava {
        options.encoding = "UTF-8" // Encode your source code with UTF-8
        options.compilerArgs.addAll(listOf("-parameters"))
        sourceCompatibility = "1.8"
    }
    compileKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
            javaParameters = true
        }
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    spotless {
        format("misc") {
            target(listOf("**/*.gradle", "**/*.md"))
            trimTrailingWhitespace()
            indentWithSpaces(4)
        }
        kotlin {
            ktlint("0.36.0")
            licenseHeader("/* Licensed under MIT */")
        }
    }
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to rootProject.version)
        }
    }
}

