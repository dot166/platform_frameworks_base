import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
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
    namespace = "com.android.settingslib"
    compileSdk = libCompileSdk
    defaultConfig {minSdk = libMinSdk}
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("17")
    }
}

dependencies {
    api(project(":settingslib:BannerMessagePreference"))
    api(project(":settingslib:BarChartPreference"))
    api(project(":settingslib:ButtonPreference"))
    api(project(":settingslib:CardPreference"))
    api(project(":settingslib:Category"))
    api(project(":settingslib:CollapsingToolbarBaseActivity"))
    api(project(":settingslib:Preference"))
    api(project(":settingslib:SelectorWithWidgetPreference"))
    api(project(":settingslib:SettingsSpinner"))
    api(project(":settingslib:SliderPreference"))
}

val nameVal = "SettingsLib"

mavenPublishing {
    coordinates(group.toString(), nameVal, version.toString())

    pom {
        name = nameVal
        description = "SettingsLib from GrapheneOS"
        inceptionYear = "2025"
        url = "https://github.com/dot166/platform_frameworks_base/tree/16-qpr2/packages/SettingsLib"
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
}
