import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    kotlin("jvm") version "1.2.21"
    java
    id("org.jetbrains.kotlin.kapt") version "1.2.21"
    id("kotlinx-serialization") version "0.4"
}

repositories {
    mavenCentral()

    maven {
        url = URI("https://s3-us-west-2.amazonaws.com/dynamodb-local/release")
    }

    maven {
        url = URI("https://kotlin.bintray.com/kotlinx")
    }
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(kotlin("reflect"))

    compile(project(":scarf-annotations"))
    kapt(project(":scarf-annotations"))

    compile("com.amazonaws", "aws-java-sdk-dynamodb",  "1.11.+")
    compile("com.sparkjava", "spark-core",  "2.7.1")
    //testCompile("com.github.mlk","assortmentofjunitrules","1.5.36")
    compile("com.amazonaws","DynamoDBLocal","1.11.+")
    compile("com.fasterxml.jackson.module","jackson-module-kotlin","2.9.4")
    compile("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.4")

    kaptTest(project(":scarf-annotations"))
    compile("junit", "junit", "4.12")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}