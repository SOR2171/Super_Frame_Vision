package io.github.sor2171.superframevision.core.entity

actual fun currentPlatform(): Platform {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()

        return Platform(
            os = when {
                os.contains("windows") -> Platform.Os.Windows
                os.contains("linux") -> Platform.Os.Linux
                os.contains("mac") || os.contains("darwin") -> Platform.Os.MacOS
                else -> error("Unsupported OS: $os")
            },
            architecture = when (arch) {
                "x86" -> Platform.Architecture.X86
                "amd64", "x86_64" -> Platform.Architecture.X86_64
                "aarch64", "arm64" -> Platform.Architecture.Arm64
                else -> error("Unsupported architecture: $arch")
            }
        )
    }