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
        maven {
            url = uri("https://jitpack.io")
            content {
                includeGroup("com.github.PhilJay")
            }
        }
    }
}

rootProject.name = "SettingsLib"
include(":Color")
include(":DataStore")
include(":Spa")
project(":Spa").projectDir = File("Spa/spa")
include(":Spa:gallery")
project(":Spa:gallery").projectDir = File("Spa/gallery")
include(":Spa:testutils")
project(":Spa:testutils").projectDir = File("Spa/testutils")
