plugins {
    id("com.android.application")
}

android {
    namespace = "com.tamilnature.ephemeratest"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tamilnature.ephemeratest"
        minSdk = 23
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
    }
}

dependencies {
    // Ephemera dependency temporarily removed for crash testing
}
