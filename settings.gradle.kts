import org.gradle.authentication.http.BasicAuthentication
import org.gradle.api.artifacts.repositories.PasswordCredentials

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
        maven("https://jitpack.io")

        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")

            credentials(PasswordCredentials::class) {
                username = "mapbox"
                password = providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN").get()
            }

            authentication {
                create<BasicAuthentication>("basic")
            }

            // Optional, but recommended to avoid leaking this repo into other deps:
            content {
                includeGroupByRegex("com\\.mapbox.*")
            }
        }
    }
}

rootProject.name = "Gps Navigation"
include(":app")
