import java.net.URI



pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven { url = uri("https://jitpack.io") }
        maven {
            url = uri("https://esri.jfrog.io/artifactory/arcgis")
        }
    }
}

rootProject.name = "getapp-sdk"
include(":system-test")
include(":sdk")
include(":example-app")
