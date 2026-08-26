package io.github.sor2171.superframevision

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform