plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

val libMinSdk: Int = rootProject.extra["libMinSdk"] as Int
val libCompileSdk: Int = rootProject.extra["libCompileSdk"] as Int

android {
    namespace = "com.android.settingslib.datastore"
    compileSdk = libCompileSdk
    defaultConfig {minSdk = libMinSdk}
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets.getByName("main") {
        val src: List<String> = listOf("src", "src-gradle")
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
    api("androidx.collection:collection-ktx:1.5.0")
    api(libs.androidx.core.ktx)
    api("com.google.guava:guava:33.5.0-android")
}
