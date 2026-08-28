package io.github.sor2171.superframevision.core.utils

import okio.FileSystem

object Const {
    const val CONFIG_FILE = "config.json"

    const val TEMP_DIR = "tmp"
    const val DATA_DIR = "data"

    const val FFMPEG_DIR = "ffmpeg"
    const val NCNN_DIR = "ncnn"

    const val FULL_APP_NAME = "Super Frame Vision"
    const val SHORT_APP_NAME = "SFV"

    const val SOFTWARE_INFO = "以下内容硬编码于源码中：\n" +
            "The following content is hard-coded into the source code:" +
            "|软件完全免费且开源于GitHub平台，如果你是付费下载，则说明你已上当受骗。\n" +
            "The software is completely free and open-source on GitHub; " +
            "if you paid to download it, you have been scammed."
}