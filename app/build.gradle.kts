plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.barcodescanner"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.barcodescanner"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}

dependencies {
    // compose
    implementation(platform("androidx.compose:compose-bom:2026.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")

    // Utilise bien des parenthèses et des guillemets doubles
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0-alpha01")
    implementation("com.google.android.material:material:1.11.0")

    // CameraX
    val camerax_version = "1.4.0-alpha01"
    implementation("androidx.camera:camera-core:1.7.0-alpha01")
    implementation("androidx.camera:camera-camera2:1.7.0-alpha01")
    implementation("androidx.camera:camera-lifecycle:1.7.0-alpha01")
    implementation("androidx.camera:camera-view:1.7.0-alpha01")
    implementation("androidx.camera:camera-view:1.7.0-alpha01")

    // ML Kit
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // Fragments
    implementation("androidx.fragment:fragment-ktx:1.7.0")
}