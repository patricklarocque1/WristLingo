plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.wristlingo.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.wristlingo.app"
        minSdk = 26
        targetSdk = 34
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.foundation)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    implementation(project(":core"))
    implementation(libs.play.services.wearable)
    implementation(libs.mlkit.translate)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
}

