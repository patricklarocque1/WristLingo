plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.wristlingo.wear"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wristlingo.wear"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        val isCi = System.getenv("CI") == "true"
        buildConfigField("boolean", "CI", isCi.toString())

        // Ensure offline flavor is used by default if not specified
        missingDimensionStrategy("mode", "offline")
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
        compose = true
        buildConfig = true
    }

    flavorDimensions += "mode"
    productFlavors {
        create("offline") {
            dimension = "mode"
            buildConfigField("boolean", "USE_CLOUD_TRANSLATE", "false")
            buildConfigField("boolean", "USE_CLOUD_STT", "false")
        }
        create("hybrid") {
            dimension = "mode"
            buildConfigField("boolean", "USE_CLOUD_TRANSLATE", "true")
            buildConfigField("boolean", "USE_CLOUD_STT", "false")
        }
        create("cloudstt") {
            dimension = "mode"
            buildConfigField("boolean", "USE_CLOUD_TRANSLATE", "false")
            buildConfigField("boolean", "USE_CLOUD_STT", "true")
        }
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
    implementation(platform(libs.compose.bom))
    implementation(libs.wear.compose.material)
    implementation(libs.androidx.compose.foundation)
    implementation(project(":core"))
    implementation(libs.play.services.wearable)
}

