import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

val appVersion = "1.1.2"
val appName = "SFV"
val packageName = "io.github.sor2171.superframevision"

compose.desktop {
    application {
        mainClass = "$packageName.MainKt"

        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            targetFormats(
                TargetFormat.Msi,    // Windows
                TargetFormat.Exe,    // Windows
                TargetFormat.Dmg,    // macOS
                TargetFormat.Pkg,    // macOS
                TargetFormat.Deb,    // Linux
                TargetFormat.Rpm     // Linux
            )
            packageName = appName
            packageVersion = appVersion

            windows {
                iconFile.set(project.file("icons/icon.ico"))
            }

            macOS {
                iconFile.set(project.file("icons/icon.icns"))
            }

            linux {
                iconFile.set(project.file("icons/icon.png"))
            }
        }
    }
}
