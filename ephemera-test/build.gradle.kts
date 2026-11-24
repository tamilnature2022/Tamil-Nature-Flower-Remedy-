
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.tamilnature.ephemeratest"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tamilnature.ephemeratest"
        minSdk = 23
        targetSdk = 35
        versionCode = 4
        versionName = "1.3"
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
}
