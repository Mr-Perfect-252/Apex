plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.apexhub.sample"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.apexhub.sample"
        minSdk = 21
        targetSdk = 34
        versionCode = 3
        versionName = "1.0.1"
    }

    // Demo signing key, committed on purpose so every build is signed with the SAME
    // key. A stable key is required for in-place OTA updates (ApexHub pins the
    // signing certificate fingerprint). Replace this with your own keystore (kept
    // out of version control) before shipping anything real.
    signingConfigs {
        create("apex") {
            storeFile = file("${rootDir}/keystore/apex-sample.keystore")
            storePassword = "apexhub"
            keyAlias = "apex-sample"
            keyPassword = "apexhub"
            storeType = "PKCS12"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("apex")
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("apex")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // ApexHub OTA SDK (official) — updates, background checks + built-in analytics
    implementation("io.github.mr-perfect-252:sdk:1.0.1")

    // apex-analytics — sessions, screen views, offline batching, crash reports
    implementation("io.github.mr-perfect-252:apex-analytics:1.0.0")
}
