/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val Ver: String = rootProject.extra["libVersion"] as String
val libMinSdk: Int = rootProject.extra["libMinSdk"] as Int
val libCompileSdkMajor: Int = rootProject.extra["libCompileSdkMajor"] as Int
val libCompileSdkMinor: Int = rootProject.extra["libCompileSdkMinor"] as Int

plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.compose.compiler)
    jacoco
}

group = "io.github.dot166"
version = Ver

android {
    namespace = "com.android.settingslib.spa"
    compileSdk {
        version = release(libCompileSdkMajor) {
            minorApiLevel = libCompileSdkMinor
        }
    }

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        minSdk = libMinSdk
    }

    sourceSets {
        getByName("main") {
            kotlin.directories.addAll(listOf("src"))
            res.directories.addAll(listOf("res"))
            manifest.srcFile("AndroidManifest.xml")
        }
        getByName("androidTest") {
            kotlin.directories.addAll(listOf("../tests/src"))
            res.directories.addAll(listOf("../tests/res"))
            manifest.srcFile("../tests/AndroidManifest.xml")
        }
    }
    buildTypes {
        getByName("debug") {
            enableAndroidTestCoverage = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("17")
    }
}

dependencies {
    api(project(":Color"))
    api(libs.androidx.appcompat)
    api(libs.androidx.material3)
    api(libs.androidx.material.icons.extended)
    api(libs.androidx.ui.tooling.preview)
    api(libs.androidx.graphics.shapes.android)
    api(libs.androidx.lifecycle.runtime.compose)
    api(libs.androidx.navigation.compose)
    api(libs.androidx.window)
    api(libs.mpandroidchart) // external/MPAndroidChart
    api(libs.material) // prebuilts/sdk/current/extras/material-design-x
    api(libs.lottie.compose) // external/lottie
    debugApi(libs.androidx.ui.tooling)

    androidTestImplementation(project(":Spa:testutils"))
    androidTestImplementation(libs.dexmaker.mockito)
}

val nameVal = "SpaLib"

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

tasks.register<JacocoReport>("coverageReport") {
    group = "Reporting"
    description = "Generate Jacoco coverage reports after running tests."
    dependsOn("connectedDebugAndroidTest")
    sourceDirectories.setFrom(files("src"))
    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
            setExcludes(
                listOf(
                    "com/android/settingslib/spa/debug/**",

                    // Excludes files forked from AndroidX.
                    "com/android/settingslib/spa/widget/scaffold/CustomizedAppBar*",

                    // Excludes files forked from Accompanist.
                    "com/android/settingslib/spa/framework/compose/DrawablePainter*",

                    // Excludes inline functions, which is not covered in Jacoco reports.
                    "com/android/settingslib/spa/framework/util/Collections*",
                    "com/android/settingslib/spa/framework/util/Flows*",

                    // Excludes debug functions
                    "com/android/settingslib/spa/framework/compose/TimeMeasurer*",
                )
            )
        }
    )
    executionData.setFrom(
        fileTree(layout.buildDirectory.dir("outputs/code_coverage/debugAndroidTest/connected"))
    )
}
