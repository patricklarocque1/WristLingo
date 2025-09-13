// NDK ABI filters normalized to arm64-v8a only.
import java.util.Properties
import java.io.File
import java.io.ByteArrayOutputStream
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.wristlingo.app"
    compileSdk = (project.findProperty("android.compileSdk") as String).toInt()
    buildToolsVersion = project.property("android.buildToolsVersion") as String
    ndkVersion = project.property("android.ndkVersion") as String

    defaultConfig {
        applicationId = "com.wristlingo.app"
        minSdk = (project.findProperty("android.minSdk") as String).toInt()
        targetSdk = (project.findProperty("android.targetSdk") as String).toInt()
        versionCode = 1
        versionName = "1.0"

        val isCi = System.getenv("CI") == "true"
        buildConfigField("boolean", "CI", isCi.toString())

        // Ensure offline flavor is used by default if not specified
        missingDimensionStrategy("mode", "offline")
        
        // Helpful Room options for either pipeline
        javaCompileOptions {
            annotationProcessorOptions {
                argument("room.incremental", "true")
                argument("room.schemaLocation", "$projectDir/schemas")
                argument("room.expandProjection", "true")
            }
        }

        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        // Pass CMake arguments for 16KB page-size support based on NDK version
        // The Gradle DSL doesn't expose cmake.arguments under defaultConfig in Kotlin DSL.
        // Instead, we pass variables via buildConfigField and handle conditions in CMake.
        val ndkVer = (project.property("android.ndkVersion") as String)
        val ndkMajor = ndkVer.substringBefore('.').toIntOrNull() ?: 0
        buildConfigField("int", "NDK_MAJOR", ndkMajor.toString())
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

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
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

// --- Toggle which processor to apply ---
val useKsp = providers.gradleProperty("useKsp").map { it.equals("true", ignoreCase = true) }.getOrElse(false)
// KSP plugin will be applied when compatible version is available

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material.iconsExtended)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    
    // Room compiler - toggle between KAPT and KSP
    if (useKsp) {
        ksp(libs.room.compiler)
    } else {
        kapt(libs.room.compiler)
    }

    implementation(project(":core"))
    implementation(libs.play.services.wearable)
    implementation(libs.mlkit.translate)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.room:room-testing:2.8.0")
}

// KAPT-specific niceties
if (!useKsp) {
    configure<org.jetbrains.kotlin.gradle.plugin.KaptExtension> {
        correctErrorTypes = true
        arguments {
            arg("room.incremental", "true")
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }
}

// Verify 16 KB page-size alignment for all arm64-v8a .so and APK zip alignment
tasks.register("verify16k") {
    group = "verification"
    description = "Checks ELF p_align == 0x4000 and zipalign -P 16 on the built APK."

    // Use -PapkVariant=cloudsttDebug to override; defaults to "debug"
    val variant = providers.gradleProperty("apkVariant").orNull ?: "debug"

    fun resolveApkFile(variantName: String): File {
        // Handle "debug" / "release"
        if (variantName.equals("debug", true) || variantName.equals("release", true)) {
            return layout.buildDirectory.file("outputs/apk/${variantName.lowercase()}/app-${variantName.lowercase()}.apk").get().asFile
        }
        // Handle combined like cloudsttDebug
        val buildTypeMatch = Regex("(Debug|Release)$").find(variantName)
        if (buildTypeMatch != null) {
            val buildType = buildTypeMatch.value.lowercase()
            val flavor = variantName.removeSuffix(buildTypeMatch.value)
            val flavorDir = flavor.replaceFirstChar { it.lowercase() }
            return layout.buildDirectory.file("outputs/apk/${flavorDir}/${buildType}/app-${flavorDir}-${buildType}.apk").get().asFile
        }
        // Fallback: flat path
        return layout.buildDirectory.file("outputs/apk/${variantName}/app-${variantName}.apk").get().asFile
    }

    val apkFile = resolveApkFile(variant)

    inputs.file(apkFile)

    // Ensure we package the chosen variant before verification
    val variantCap = variant.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    val packageTaskName = "package${variantCap}"
    if (tasks.names.contains(packageTaskName)) {
        dependsOn(packageTaskName)
    }

    doLast {
        // Read SDK/NDK from local/gradle properties
        val localProps = Properties().apply {
            file("${rootDir}/local.properties").inputStream().use { load(it) }
        }
        val sdkDir = File(localProps.getProperty("sdk.dir")
            ?: error("sdk.dir missing in local.properties"))
        val ndkVersion = (project.property("android.ndkVersion") as String)

        // Resolve llvm-readelf for current host
        val osName = System.getProperty("os.name").lowercase()
        val host = when {
            osName.contains("windows") -> "windows-x86_64"
            osName.contains("mac") -> "darwin-x86_64"
            else -> "linux-x86_64"
        }
        val exeSuffix = if (host.startsWith("windows")) ".exe" else ""
        val readelf = File(
            sdkDir,
            "ndk/$ndkVersion/toolchains/llvm/prebuilt/$host/bin/llvm-readelf$exeSuffix"
        ).also { require(it.exists()) { "llvm-readelf not found at: ${it.absolutePath}" } }

        // Unzip APK using Gradle (avoids java.util.zip boilerplate)
        val tmpDir = File(buildDir, "tmp/verify16k/${variant}").apply {
            deleteRecursively(); mkdirs()
        }
        copy {
            from(zipTree(apkFile))
            into(tmpDir)
        }

        // Check all arm64 .so program headers for Align: 0x4000
        val libsDir = File(tmpDir, "lib/arm64-v8a")
        val sos = libsDir.listFiles()?.filter { it.extension == "so" }?.sortedBy { it.name }
            ?: emptyList()
        require(sos.isNotEmpty()) { "No .so files found under ${libsDir.absolutePath}" }

        var bad = false
        // Built CMake targets for reference
        val builtNames = listOf("wristlingo_native", "whisper", "ggml", "whisper_bridge")
        
        sos.forEach { so ->
            println("\n=== ${so.name} ===")
            
            // Check if this is a prebuilt dependency
            if (builtNames.none { so.name.startsWith(it) }) {
                println("ℹ️ Prebuilt/native from dependency: ${so.name}")
            }
            
            val out = ByteArrayOutputStream()
            project.exec {
                executable = readelf.absolutePath
                args("-lW", so.absolutePath)
                standardOutput = out
                errorOutput = System.err
                isIgnoreExitValue = false
            }
            val lines = out.toString(Charsets.UTF_8).lineSequence()
                .filter { it.trimStart().startsWith("LOAD") }
                .toList()

            if (lines.isEmpty()) {
                logger.error("❌ ${so.name}: no LOAD program headers found")
                bad = true
            } else {
                // Each LOAD line: last whitespace-separated token should be alignment
                val allOk = lines.all { ln ->
                    val last = ln.trim().split(Regex("\\s+")).lastOrNull() ?: ""
                    last.equals("0x4000", true) || last == "16384"
                }
                if (!allOk) {
                    logger.error("❌ ${so.name}: one or more LOAD segments not aligned to 0x4000")
                    lines.forEach { println(it) }
                    bad = true
                } else {
                    println("✅ ${so.name}: all LOAD segments aligned to 0x4000")
                }
            }
        }
        if (bad) error("One or more .so files are not 16 KB aligned.")

        // Check APK zip alignment (-P 16)
        val buildToolsVersion = (project.property("android.buildToolsVersion") as String)
        val zipalign = File(sdkDir, "build-tools/${buildToolsVersion}/zipalign$exeSuffix")
            .takeIf { it.exists() }
            ?: error("zipalign not found under build-tools/${buildToolsVersion}")
        project.exec {
            executable = zipalign.absolutePath
            args("-v", "-c", "-P", "16", "4", apkFile.absolutePath)
        }
        println("\n✅ zipalign -c -P 16 passed")
    }
}

// Optional: automatically verify after assembling the chosen variant
tasks.matching { it.name.equals("assembleDebug", ignoreCase = true) }.configureEach {
    finalizedBy("verify16k")
}

// Make verify16k depend on assembleDebug
tasks.named("verify16k") {
    dependsOn("assembleDebug")
}

// Read-only status dump: flavors and default toggles
tasks.register("printStatus") {
    group = "help"
    description = "Prints app flavors, default toggle values, and model path existence (runtime)."
    doLast {
        println("=== :app:printStatus ===")
        val appExt = extensions.findByType(com.android.build.api.dsl.ApplicationExtension::class.java)
        val flavors = appExt?.productFlavors?.map { it.name } ?: emptyList()
        println("Flavors: ${flavors.joinToString()}")
        // Known defaults from DSL
        flavors.forEach { name ->
            val toggles = when (name) {
                "offline" -> "USE_CLOUD_TRANSLATE=false, USE_CLOUD_STT=false"
                "hybrid" -> "USE_CLOUD_TRANSLATE=true, USE_CLOUD_STT=false"
                "cloudstt" -> "USE_CLOUD_TRANSLATE=false, USE_CLOUD_STT=true"
                else -> "(unknown toggles)"
            }
            println("Flavor ${name}: ${toggles}")
        }
        println("Model path exists: unknown (runtime-only)")
    }
}

