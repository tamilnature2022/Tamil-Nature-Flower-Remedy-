
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
        versionCode = 3
        versionName = "1.2"
    }
}

dependencies {
    // Ephemera temporarily disabled for testing
}
