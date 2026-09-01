This is a Kotlin Multiplatform project targeting Desktop (JVM).

The purpose of this project is to provide a multi-platform, `FFmpeg Kit` and `ONNX Runtime` based super-resolution and frame interpolation solution, while offering as many options as possible and a beautiful, easy-to-read UI.

## About AI models

I will not publish the model files to GitHub, everyone can download them from the following links and place them in
`./shared/src/commonMain/composeResources/files/models`，don't forget to rename

[`rife2_26h.onnx`](shared/src/commonMain/composeResources/files/rife2_26h.onnx)

[`real-esrgan_x2.onnx`](shared/src/commonMain/composeResources/files/real-esrgan_x2.onnx)

[`real-esrgan_x4.onnx`](shared/src/commonMain/composeResources/files/real-esrgan_x4.onnx)

[`real-esrgan_x4plus_anime_6B.onnx`](shared/src/commonMain/composeResources/files/real-esrgan_x4plus_anime_6B.onnx)

[RIFE](https://huggingface.co/SOR2171/RIFE-ONNX/tree/main)

[Real-ESRGAN](https://huggingface.co/SceneWorks/real-esrgan-onnx/tree/main)

[Real-ESRGAN_x4plus_anime](https://huggingface.co/mhmtaufiq/realesrgan-onnx/tree/main)

## How to run

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Desktop app:
  - Hot reload: `./gradlew :desktopApp:hotRun --auto`
  - Standard run: `./gradlew :desktopApp:run`

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Desktop tests: `./gradlew :shared:jvmTest`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com.cn/en-us/help/kotlin-multiplatform-dev/get-started.html)…
