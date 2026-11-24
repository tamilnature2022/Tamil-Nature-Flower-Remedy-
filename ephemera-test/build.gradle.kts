
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
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("com.github.intellecat:ephemera:0.4.1")
}
