plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.register("apiCheck") {
    group = "help"
    description = "Scans :core for forbidden Android imports and lists hits. (Does not fail)"
    doLast {
        println("=== :core:apiCheck ===")
        val coreDir = project.layout.projectDirectory.dir("src/main/java").asFile
        val hits = mutableListOf<String>()
        coreDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            file.readLines().forEachIndexed { idx, line ->
                if (line.contains("android.") || line.contains("androidx.")) {
                    hits += "${file.relativeTo(coreDir).path}:${idx + 1}: ${line.trim()}"
                }
            }
        }
        if (hits.isEmpty()) println("No forbidden Android imports found.") else {
            println("Found "+hits.size+" forbidden imports:")
            hits.forEach { println(it) }
        }
    }
}

