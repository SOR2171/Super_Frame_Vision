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
//            implementation("io.github.sor2171:ffmpeg-kit-kmp:0.11.2")
            implementation(files("./libs/ffmpeg-kit-kmp-jvm.jar"))

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