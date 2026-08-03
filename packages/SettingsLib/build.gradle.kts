import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val libMinSdk: Int = 31
val libCompileSdkMajor: Int = 37
val libCompileSdkMinor: Int = 0
val Ver: String = "${libCompileSdkMajor + 100}.$libCompileSdkMinor.${providers.environmentVariable("BUILD_NUMBER").orElse("0").get()}"

plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.aconfig) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}

extra.apply {
    set("libVersion", Ver)
    set("libMinSdk", libMinSdk)
    set("libCompileSdkMajor", libCompileSdkMajor)
    set("libCompileSdkMinor", libCompileSdkMinor)
}

group = "io.github.dot166"
version = Ver

android {
    namespace = "com.android.settingslib"
    compileSdk {
        version = release(libCompileSdkMajor) {
            minorApiLevel = libCompileSdkMinor
        }
    }
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

fun getModuleVersion(moduleName: String): String {
    return "${libCompileSdkMajor + 100}.$libCompileSdkMinor.${
        providers.gradleProperty("${moduleName}_VERSION").orElse(version.toString()).get()
    }"
}

dependencies {
    if (providers.gradleProperty("publish_color").map { it.toBoolean() }.getOrElse(false)) {
        api(project(":Color"))
    } else {
        api("io.github.dot166:SettingsLibColor:${getModuleVersion("color")}")
    }

    if (providers.gradleProperty("publish_datastore").map { it.toBoolean() }.getOrElse(false)) {
        api(project(":DataStore"))
    } else {
        api("io.github.dot166:SettingsLibDataStore:${getModuleVersion("datastore")}")
    }

    if (providers.gradleProperty("publish_spa").map { it.toBoolean() }.getOrElse(false)) {
        api(project(":Spa"))
    } else {
        api("io.github.dot166:SpaLib:${getModuleVersion("spa")}")
    }
}

val nameVal = "SettingsLib"

mavenPublishing {
    coordinates(group.toString(), nameVal, version.toString())

    pom {
        name = nameVal
        description = "SettingsLib from GrapheneOS"
        inceptionYear = "2025"
        url = "https://github.com/dot166/platform_frameworks_base/tree/17/packages/SettingsLib"
        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
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
