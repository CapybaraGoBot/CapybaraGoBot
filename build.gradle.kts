import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.21"
}

group = "net.capybarago"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()

    maven("https://jitpack.io")

    maven("https://m2.dv8tion.net/releases")
}

dependencies {
    // Discord
    implementation("net.dv8tion:JDA:5.2.2")
    implementation("club.minnced:jda-ktx:0.12.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}