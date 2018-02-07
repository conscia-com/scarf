import java.net.URI

//rootProject.name = 'serverlesskotlingfw'
//include 'serverlesskotlinfw-annotations'
//include 'serverlesskotlinfw-core'

include("scarf-annotations", "scarf-core")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            url = URI("https://kotlin.bintray.com/kotlinx")
        }
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "kotlinx-serialization") {
                useModule("org.jetbrains.kotlinx:kotlinx-gradle-serialization-plugin:${requested.version}")
            }
        }
    }
}