import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.time.LocalDateTime

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

val appVersion = System.getenv("TAG")?.removePrefix("v")
    ?: run {
        val now = LocalDateTime.now()

        val major = now.year - 2000
        val minor = now.monthValue
        val build = now.dayOfMonth * 100 + now.hour

        "$major.$minor.$build"
    }
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
                TargetFormat.Rpm,
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
                appStore = false
                signing {
                    sign.set(false)
                }
            }

            linux {
                iconFile.set(project.file("icons/icon.png"))
                menuGroup = appName
                shortcut = true
            }
        }
    }
}
