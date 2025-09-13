// Fixed ABI for Wear OS: removed NDK; disabled ABI splits (arm64-only devices).
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.wristlingo.wear"
    compileSdk = (project.findProperty("android.compileSdk") as String).toInt()
    buildToolsVersion = project.property("android.buildToolsVersion") as String

    defaultConfig {
        applicationId = "com.wristlingo.wear"
        minSdk = (project.findProperty("android.wear.minSdk") as String).toInt()
        targetSdk = (project.findProperty("android.targetSdk") as String).toInt()
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

    // Disable ABI splits for Wear to avoid accidental 32-bit packaging; :app handles native/NDK
    splits {
        abi {
            isEnable = false
        }
    }

    // Packaging sanity for JNI libs
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
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
        isCoreLibraryDesugaringEnabled = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(platform(libs.compose.bom))
    implementation(libs.wear.compose.material)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(project(":core"))
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
}

// Local verify: gradlew clean :wear:assembleOfflineDebug
// Install to arm64 Wear emulator/device

tasks.register("checkAbi") {
    group = "help"
    description = "Prints enabled ABI splits and warns if anything other than none is present."
    doLast {
        println("=== :wear:checkAbi ===")
        val androidExt = extensions.findByName("android") as? com.android.build.gradle.internal.dsl.BaseAppModuleExtension
        val enabled = androidExt?.splits?.abi?.isEnable ?: false
        if (!enabled) {
            println("ABI splits: none (disabled)")
        } else {
            println("ABI splits enabled")
            println("Warning: Wear module should not enable ABI splits (app handles ABI)")
        }
    }
}

