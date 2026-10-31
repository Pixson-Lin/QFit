import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Upload signing (Play App Signing). Load gitignored keystore.properties at repo root.
// When absent (fresh clone / no secrets), release falls back to the debug keystore.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}
val hasUploadKeystore = keystorePropertiesFile.exists()

android {
    namespace = "com.pixsonlin.qfit"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pixsonlin.qfit"
        minSdk = 31
        targetSdk = 36
        // versionCode = YYYYMMDDNN (Taipei calendar day + daily serial). See docs/Versioning.md
        versionCode = 2026103101
        versionName = "1.0.0-prerelease"
    }

    signingConfigs {
        if (hasUploadKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            // R8 minify → proguard.map ships in AAB for Play crash deobfuscation.
            isMinifyEnabled = true
            isShrinkResources = true
            // Extract native symbols when .so files still have a symbol table.
            // Compose's androidx.graphics.path ships pre-stripped; Play may still warn.
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            signingConfig = if (hasUploadKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.health.connect.client)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.ui.tooling)
}
