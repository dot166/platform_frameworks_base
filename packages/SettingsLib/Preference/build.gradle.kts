plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

val libMinSdk: Int = rootProject.extra["libMinSdk"] as Int
val libCompileSdk: Int = rootProject.extra["libCompileSdk"] as Int

android {
    namespace = "com.android.settingslib.preference"
    compileSdk = libCompileSdk
    defaultConfig {minSdk = libMinSdk}
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets.getByName("main") {
        val src: List<String> = listOf("src")
        java.setSrcDirs(src)
        val resDirs: List<String> = listOf("res")
        res.setSrcDirs(resDirs)
        manifest.srcFile("AndroidManifest.xml")
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    api(project(":settingslib:DataStore"))
    api(project(":settingslib:Metadata"))
    api(project(":settingslib:SettingsTheme"))
    api(libs.androidx.preference)
}
