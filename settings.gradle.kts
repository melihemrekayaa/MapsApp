pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven {
            credentials{
                username = "darxreflex"
                password = "Birikim1."
            }
            url = uri("https://repositories.tomtom.com/artifactory/maven")
        }

    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://repositories.tomtom.com/artifactory/maven")
        }

    }
}

rootProject.name = "MapsApp"
include(":app")
 