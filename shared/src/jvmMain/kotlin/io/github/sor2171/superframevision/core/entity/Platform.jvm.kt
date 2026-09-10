package io.github.sor2171.superframevision.core.entity

import com.sun.jna.Platform.is64Bit
import com.sun.jna.Platform.isARM
import com.sun.jna.Platform.isIntel
import com.sun.jna.Platform.isLinux
import com.sun.jna.Platform.isMac
import com.sun.jna.Platform.isWindows

actual fun currentPlatform(): Platform {

    return Platform(
        os = when {
            isWindows() -> Platform.Os.Windows
            isLinux() -> Platform.Os.Linux
            isMac() -> Platform.Os.MacOS
            else -> error("Unsupported OS: ${System.getProperty("os.name")}")
        },
        architecture = when {
            !is64Bit() -> Platform.Architecture.X86
            isIntel() && is64Bit() -> Platform.Architecture.X86_64
            isARM() && is64Bit() -> Platform.Architecture.Arm64
            else -> error("Unsupported architecture: ${System.getProperty("os.arch")}")
        }
    )
}