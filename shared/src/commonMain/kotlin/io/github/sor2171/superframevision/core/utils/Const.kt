package io.github.sor2171.superframevision.core.utils

object Const {
    private val appDataPath = createFileUtils().appDataPath

    val CONFIG_FILE = appDataPath / "config.json"

     val TEMP_DIR = appDataPath / "temp"
     val DATA_DIR = appDataPath / "data"

     val FFMPEG_DIR = DATA_DIR / "ffmpeg"

    const val FULL_APP_NAME = "Super Frame Vision"
    const val SHORT_APP_NAME = "SFV"
}