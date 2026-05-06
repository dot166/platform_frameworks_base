import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val libMinSdk: Int = 31
val libCompileSdk: Int = 36
val buildTime: String = LocalDateTime.now()
    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
val Ver: String = "1$libCompileSdk.1.$buildTime"

plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.aconfig) apply false
}

extra.apply {
    set("libVersion", Ver)
    set("libMinSdk", libMinSdk)
    set("libCompileSdk", libCompileSdk)
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
    api(project(":BannerMessagePreference"))
    api(project(":BarChartPreference"))
    api(project(":ButtonPreference"))
    api(project(":CardPreference"))
    api(project(":Category"))
    api(project(":CollapsingToolbarBaseActivity"))
    api(project(":Preference"))
    api(project(":SelectorWithWidgetPreference"))
    api(project(":SettingsSpinner"))
    api(project(":SliderPreference"))
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
    configure(com.vanniktech.maven.publish.AndroidSingleVariantLibrary(
        variant = "release",
        sourcesJar = SourcesJar.Sources(),
        javadocJar = JavadocJar.None(),
    ))
}
