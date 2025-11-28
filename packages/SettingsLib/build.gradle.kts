plugins {
    alias(libs.plugins.android.library)
}

val libMinSdk: Int = rootProject.extra["libMinSdk"] as Int
val libCompileSdk: Int = rootProject.extra["libCompileSdk"] as Int

android {
    namespace = "com.android.settingslib"
    compileSdk = libCompileSdk
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":settingslib:BarChartPreference"))
    api(project(":settingslib:Category"))
    api(project(":settingslib:CollapsingToolbarBaseActivity"))
    api(project(":settingslib:Preference"))
    api(project(":settingslib:SliderPreference"))
}
