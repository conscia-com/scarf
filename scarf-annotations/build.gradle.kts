import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    kotlin("jvm") version "1.2.21"
    id("kotlinx-serialization") version "0.4"
}

repositories {
    mavenCentral()

    maven {
        url = URI("https://kotlin.bintray.com/kotlinx")
    }
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("com.amazonaws", "aws-java-sdk-dynamodb",  "1.9.0")
    compile("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.4")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}