plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.wristlingo.core"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        targetSdk = 35

        val isCi = System.getenv("CI") == "true"
        buildConfigField("boolean", "CI", isCi.toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        debug {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.wearable)
}

