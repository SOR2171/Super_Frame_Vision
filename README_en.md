This is a Kotlin Multiplatform project targeting Desktop (JVM).

The purpose of this project is to provide a multi-platform super-resolution frame interpolation
scheme based on
[`FFmpeg Kit`](https://github.com/akashskypatel/ffmpeg-kit-builders)
and [`NCNN`](https://github.com/Tencent/ncnn).
It also aims to offer as many options as possible and a visually appealing and easy-to-read UI.

## Supported Platforms

|         | X64 | Arm64 |
|---------|:---:|:-----:|
| Windows |  ✅  |       |
| Linux   |  ✅  |       |
| MacOS   |     |   ✅   |

## About AI models

I used the following ONNX model to generate the NCNN model:

[RIFE](https://huggingface.co/SOR2171/RIFE-ONNX/tree/main)

[Real-ESRGAN](https://huggingface.co/SceneWorks/real-esrgan-onnx/tree/main)

[Real-ESRGAN_x4plus_anime](https://huggingface.co/mhmtaufiq/realesrgan-onnx/tree/main)
(not yet)

## How to run

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform
  applications.
  It contains several subfolders:
    - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the
      folder name.
      For example, if you want to edit the Desktop (JVM) specific part,
      the [jvmMain](./shared/src/jvmMain/kotlin)
      folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these
commands and options:

- Desktop app:
    - Hot reload: `./gradlew :desktopApp:hotRun --auto`
    - Standard run: `./gradlew :desktopApp:run`

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Desktop tests: `./gradlew :shared:jvmTest`

---

Learn more
about [Kotlin Multiplatform](https://www.jetbrains.com.cn/en-us/help/kotlin-multiplatform-dev/get-started.html)…
