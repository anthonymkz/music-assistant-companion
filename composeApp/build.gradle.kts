import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            binaryOption("bundleId", "io.music_assistant.client.composeapp")
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            implementation(libs.ktor.client.android)

            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)

            implementation(libs.androidx.media)
            implementation("androidx.browser:browser:1.8.0")

            implementation(libs.coil)
            implementation(libs.concentus)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            // implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.navigation3.ui)
            implementation(libs.androidx.navigation3.material3.adaptive)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.client.json)
//            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.coroutines.core)

            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.navigation.compose)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.coil.svg)

            implementation(libs.material.icons.core)
            implementation(libs.material.icons.extended)
            implementation(libs.icons.fontawesome)
            implementation(libs.icons.tabler)
            implementation(libs.settings.multiplatform)
            implementation(libs.reorderable)

            implementation(libs.kermit)

        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "io.music_assistant.client"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.music_assistant.client"
        minSdk { version = release(libs.versions.android.minSdk.get().toInt()) }
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 3
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            // CI: read from environment variables (GitHub Actions secrets)
            // Local: read from keystore.properties or fall back to project-root keystore
            val envStoreFile = System.getenv("RELEASE_KEYSTORE_PATH")
            val envStorePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
            val envKeyAlias = System.getenv("RELEASE_KEY_ALIAS")
            val envKeyPassword = System.getenv("RELEASE_KEY_PASSWORD")

            if (envStoreFile != null && envStorePassword != null) {
                storeFile = file(envStoreFile)
                storePassword = envStorePassword
                keyAlias = envKeyAlias ?: "mass-companion"
                keyPassword = envKeyPassword ?: envStorePassword
            } else {
                // Local development: try keystore.properties, then defaults
                val keystorePropsFile = rootProject.file("keystore.properties")
                if (keystorePropsFile.exists()) {
                    val props = Properties().apply { load(keystorePropsFile.inputStream()) }
                    storeFile = file(props.getProperty("storeFile", "../release.keystore"))
                    storePassword = props.getProperty("storePassword")
                    keyAlias = props.getProperty("keyAlias", "mass-companion")
                    keyPassword = props.getProperty("keyPassword")
                } else {
                    // Direct fallback for local builds
                    storeFile = rootProject.file("release.keystore")
                    storePassword = "masscompanion2026"
                    keyAlias = "mass-companion"
                    keyPassword = "masscompanion2026"
                }
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildToolsVersion = "36.0.0"

    lint {
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}
