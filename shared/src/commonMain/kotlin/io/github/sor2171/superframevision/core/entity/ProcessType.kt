package io.github.sor2171.superframevision.core.entity

import io.github.sor2171.superframevision.core.utils.LabelResolvable
import org.jetbrains.compose.resources.StringResource
import superframevision.shared.generated.resources.Res
import superframevision.shared.generated.resources.image_sr
import superframevision.shared.generated.resources.video_fi
import superframevision.shared.generated.resources.video_sr
import superframevision.shared.generated.resources.video_srfi

enum class ProcessType(
    override val label: StringResource,
) : LabelResolvable {
    ImageSR(
        label = Res.string.image_sr
    ),
    VideoSR(
        label = Res.string.video_sr
    ),
    VideoFI(
        label = Res.string.video_fi
    ),
    VideoSRFI(
        label = Res.string.video_srfi
    ),
}