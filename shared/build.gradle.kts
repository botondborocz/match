import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("plugin.serialization") version "2.3.0"
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    jvm()

    js {
        outputModuleName = "shared"
        browser()
        binaries.library()
        generateTypeScriptDefinitions()
        compilerOptions {
            target = "es2015"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.runtime:runtime:1.10.0")
            implementation("org.jetbrains.compose.components:components-resources:1.10.0")
            // Your base Koin libraries
            implementation("io.insert-koin:koin-core:4.1.0") // Or your current version (e.g., 3.5.6)
            implementation("io.insert-koin:koin-compose:4.1.0")

            // 👇 ADD THESE TWO FOR KMP VIEWMODELS
            implementation("io.insert-koin:koin-core-viewmodel:4.1.0")    // Fixes AppModule.kt
            implementation("io.insert-koin:koin-compose-viewmodel:4.1.0") // Fixes LoginScreen.kt

            // Official KMP ViewModel & Coroutines
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:2.8.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

            implementation("io.ktor:ktor-client-core:3.3.0")
            implementation("io.ktor:ktor-client-content-negotiation:3.3.0")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.0")
            implementation("io.ktor:ktor-client-auth:3.3.0")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation("com.liftric:kvault:1.12.0")
        }
        iosMain.dependencies {
            implementation("com.liftric:kvault:1.12.0")
        }
    }
}

android {
    namespace = "org.ttproject.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "org.ttproject.shared.resources"
}