This is a Kotlin Multiplatform project targeting Desktop (JVM).

The purpose of this project is to provide a multi-platform super-resolution frame interpolation
scheme based on
[`FFmpeg Kit`](https://github.com/akashskypatel/ffmpeg-kit-builders)
and [`NCNN`](https://github.com/Tencent/ncnn).
It also aims to offer as many options as possible and a visually appealing and easy-to-read UI.

It includes a solution that encapsulates the NCNN library and implements model loading, tiled
inference, and coroutine-based execution.

## Supported Platforms

|         | X64 | Arm64 |
|---------|:---:|:-----:|
| Windows | ✅  |       |
| Linux   | ✅  |       |
| MacOS   |     |  ✅   |

## About AI models

I used the following ONNX model to generate the NCNN model:

[RIFE](https://huggingface.co/SOR2171/RIFE-ONNX/tree/main)

[Real-ESRGAN](https://huggingface.co/SceneWorks/real-esrgan-onnx/tree/main)

[Real-ESRGAN_x4plus_anime](https://huggingface.co/mhmtaufiq/realesrgan-onnx/tree/main)
(not yet)

The parameters are `inputshape=[1,6,1024,1024],[1,1,1,1]`, and 16-bit quantization is performed
using `ncnnoptimize`.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these
commands and options:

- Hot reload: `./gradlew :desktopApp:hotRun --auto`
- Standard run: `./gradlew :desktopApp:run`

Learn more
about [Kotlin Multiplatform](https://www.jetbrains.com.cn/en-us/help/kotlin-multiplatform-dev/get-started.html)…

## License & FFmpeg Notice

This project uses [FFmpeg](https://ffmpeg.org/) (via [`FFmpeg Kit KMP`](https://github.com/SOR2171/FFmpeg-Kit_KMP)) and includes components compiled under the GPL license.

Accordingly, this project is licensed under the **[GNU General Public License v3.0 (GPL-3.0)](./LICENSE)**.

- FFmpeg is a trademark and open-source project of the FFmpeg developers/team, and its copyright belongs to its original authors.
- For more licensing information, legal notices, and source code of FFmpeg, please visit the [FFmpeg Official Website](https://ffmpeg.org/) and [FFmpeg Legal Information](https://ffmpeg.org/legal.html).

## Agents & Developer Guidelines

For more details on codebase architecture, Composable UI structure, domain services (ProcessLauncher,
MediaProcessor, NcnnRunner, FFmpegRunner), and build/test commands, please refer to **[AGENTS.md](./AGENTS.md)** for a comprehensive project overview.
