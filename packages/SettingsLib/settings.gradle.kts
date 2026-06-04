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
include(":BannerMessagePreference")
include(":BarChartPreference")
include(":ButtonPreference")
include(":CardPreference")
include(":Category")
include(":CollapsingToolbarBaseActivity")
include(":Color")
include(":DataStore")
include(":MenuPreference")
include(":Metadata")
include(":Preference")
include(":SelectorWithWidgetPreference")
include(":SettingsSpinner")
include(":SettingsTheme")
include(":SettingsTransition")
include(":SliderPreference")
include(":Spa")
project(":Spa").projectDir = File("Spa/spa")
include(":Spa:gallery")
project(":Spa:gallery").projectDir = File("Spa/gallery")
include(":Spa:testutils")
project(":Spa:testutils").projectDir = File("Spa/testutils")
