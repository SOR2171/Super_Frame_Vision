package io.github.sor2171.superframevision.core.utils

import androidx.compose.ui.graphics.Color
import io.github.sor2171.superframevision.core.entity.Themes

object Const {
    const val CONFIG_FILE = "config.json"

    const val TEMP_DIR = "tmp"
    const val ORIGIN_FRAME_DIR = "origin"
    const val UPSCALED_FRAME_DIR = "upscaled"
    const val INFERRED_FRAME_DIR = "inferred"
    const val DATA_DIR = "data"
    const val FULL_APP_NAME = "Super Frame Vision"
    const val SHORT_APP_NAME = "SFV"

    const val GITHUB_LINK = "https://github.com/SOR2171/Super_Frame_Vision"
    const val BILIBILI_LINK = "https://space.bilibili.com/398577276"
    const val QQ_GROUP_LINK_SLV = "https://qm.qq.com/q/GxugmAFtS0"
    const val QQ_GROUP_LINK = "https://qm.qq.com/q/bZ0IGesRQQ"

    const val SOFTWARE_INFO = "以下内容硬编码于源码中：\n" +
            "The following content is hard-coded into the source code:" +
            "|软件完全免费且开源于GitHub平台，所有付费下载皆为盗版。\n" +
            "The software is completely free and open-source on GitHub; all paid downloads are pirated." +
            "|如果这个软件帮到了你，可以用以下方式表达你的心意。\n" +
            "If this software has been helpful to you, you can show your appreciation in the following ways."

    val colorList = listOf(
        Themes(
//            label = ,
            color = Color(227, 187, 181)
        ),
        Themes(
//            label = ,
            color = Color(24, 170, 99)
        ),
        Themes(
//            label = ,
            color = Color(103, 58, 183)
        ),
        Themes(
//            label = ,
            color = Color(9, 29, 185)
        )
    )
}