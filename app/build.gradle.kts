plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "id.hanzdev.apexclash"
    compileSdk = 34

    defaultConfig {
        applicationId = "id.hanzdev.apexclash"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore/apexclash-release.jks")
            storePassword = "apexclash2026"
            keyAlias = "apexclash"
            keyPassword = "apexclash2026"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    // Aset game tidak dikompres supaya bisa dibaca cepat dan mendukung seek audio/video.
    androidResources {
        noCompress += listOf("mp3", "mp4", "webp", "png", "ttf", "js", "css", "html", "json", "svg")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
