plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10"
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            // https://github.com/SOR2171/FFmpeg-Kit_KMP
            implementation("io.github.sor2171:ffmpeg-kit-kmp:0.11.5")

            implementation("io.github.vinceglb:filekit-core:0.15.0")
            implementation("io.github.vinceglb:filekit-dialogs:0.15.0")
            implementation("io.github.vinceglb:filekit-dialogs-compose:0.15.0")
            implementation("com.squareup.okio:okio:3.18.1")
            implementation("com.materialkolor:material-kolor:4.1.1")

            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation("androidx.compose.material:material-icons-extended:1.6.8")
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.jna)
            implementation(libs.slf4j.simple)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

compose.resources {
    publicResClass = true
}

val targetPlatform = run {
    val prop = (project.findProperty("targetPlatform") as? String)
        ?: System.getenv("TARGET_PLATFORM")
    when (prop?.lowercase()) {
        "windows", "win", "win32", "win64" -> "windows"
        "linux" -> "linux"
        "macos", "mac", "darwin", "osx" -> "macos"
        "all" -> "all"
        null, "" -> {
            val osName = System.getProperty("os.name").lowercase()
            when {
                osName.contains("win") -> "windows"
                osName.contains("linux") -> "linux"
                osName.contains("mac") || osName.contains("darwin") -> "macos"
                else -> "all"
            }
        }
        else -> prop.lowercase()
    }
}

tasks.withType<ProcessResources>().configureEach {
    when (targetPlatform) {
        "windows" -> {
            exclude("linux-x86-64/**")
            exclude("darwin-aarch64/**")
        }
        "linux" -> {
            exclude("win32-x86-64/**")
            exclude("darwin-aarch64/**")
        }
        "macos" -> {
            exclude("win32-x86-64/**")
            exclude("linux-x86-64/**")
        }
    }
}
