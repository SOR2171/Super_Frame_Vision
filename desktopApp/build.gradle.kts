import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.compose.components.resources)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

val appVersion = "0.1.0"
val appName = "Super Frame Vision"
val packageName = "io.github.sor2171.superframevision"

compose.desktop {
    application {
        mainClass = "$packageName.MainKt"

        nativeDistributions {
            targetFormats(
                // Windows
                TargetFormat.Msi,
                TargetFormat.Exe,
                // macOS
                TargetFormat.Dmg,
                // Linux
                TargetFormat.Deb,
                TargetFormat.Rpm ,
                TargetFormat.AppImage
            )
            packageName = appName
            packageVersion = appVersion

            buildTypes.release.proguard {
                configurationFiles.from(files("proguard-rules.pro"))
            }

            windows {
                iconFile.set(project.file("icons/icon.ico"))
                menuGroup = appName
                shortcut = true
                dirChooser = true
            }

            macOS {
                iconFile.set(project.file("icons/icon.icns"))
                bundleID = packageName
            }

            linux {
                iconFile.set(project.file("icons/icon.png"))
                menuGroup = appName
                shortcut = true
            }
        }
    }
}
