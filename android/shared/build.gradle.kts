plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.squatchsports.training.shared"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    testImplementation(libs.junit)
    // Android's org.json is a stub in local unit tests; use the real implementation.
    testImplementation(libs.org.json)
}
