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
    }
}

rootProject.name = "SettingsLib"
include(":BannerMessagePreference")
include(":BarChartPreference")
include(":ButtonPreference")
include(":CardPreference")
include(":Category")
include(":CollapsingToolbarBaseActivity")
include(":DataStore")
include(":Metadata")
include(":Preference")
include(":SelectorWithWidgetPreference")
include(":SettingsSpinner")
include(":SettingsTheme")
include(":SettingsTransition")
include(":SliderPreference")
