package io.github.sor2171.superframevision.core.entity

data class Platform(
    val os: Os,
    val architecture: Architecture
) {
    enum class Os {
        Windows,
        Linux,
        MacOS
    }

    enum class Architecture {
        X86,
        X86_64,
        Arm64
    }
}

expect val currentPlatform: Platform