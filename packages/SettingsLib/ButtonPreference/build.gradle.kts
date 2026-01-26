import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val Ver: String = rootProject.extra["libVersion"] as String
val libMinSdk: Int = rootProject.extra["libMinSdk"] as Int
val libCompileSdk: Int = rootProject.extra["libCompileSdk"] as Int

plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
    alias(libs.plugins.maven.publish)
}

group = "io.github.dot166"
version = Ver

android {
    namespace = "com.android.settingslib.widget.preference.button"
    compileSdk = libCompileSdk
    defaultConfig {minSdk = libMinSdk}
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets.getByName("main") {
        val src: List<String> = listOf("src")
        java.directories.addAll(src)
        kotlin.directories.addAll(src)
        val resDirs: List<String> = listOf("res")
        res.directories.addAll(resDirs)
        manifest.srcFile("AndroidManifest.xml")
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("17")
    }
}

dependencies {
    api(project(":settingslib:SettingsTheme"))
    api(libs.androidx.preference)
}

val nameVal = "SettingsLibButtonPreference"

mavenPublishing {
    coordinates(group.toString(), nameVal, version.toString())

    pom {
        name = nameVal
        description = "SettingsLib from GrapheneOS"
        inceptionYear = "2025"
        url = "https://github.com/dot166/platform_frameworks_base/tree/16-qpr1/packages/SettingsLib"
        licenses {
            license {
                name.set("Apache License")
                url.set("https://choosealicense.com/licenses/apache-2.0/")
            }
        }
        developers {
            developer {
                id = "dot166"
                name = "._______166"
                url = "https://dot166.github.io"
            }
            developer {
                id = "graphene"
                name = "GrapheneOS"
                url = "https://grapheneos.org"
            }
            developer {
                id = "aosp"
                name = "The Android Open Source Project"
                url = "https://source.android.com"
            }
        }
        scm {
            url = "https://github.com/dot166/platform_frameworks_base"
            connection = "scm:git:git://github.com/dot166/platform_frameworks_base.git"
            developerConnection = "scm:git:ssh://git@github.com/dot166/platform_frameworks_base.git"
        }
    }
    configure(com.vanniktech.maven.publish.AndroidSingleVariantLibrary(
        variant = "release",
        sourcesJar = true,
        publishJavadocJar = false,
    ))
}
