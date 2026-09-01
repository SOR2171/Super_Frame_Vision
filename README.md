# SFV

<img src="./shared/src/commonMain/composeResources/drawable/sfv.svg" alt="logo" align="right" width="135">

**Super Frame Vision**

![](https://img.shields.io/badge/Linux-FCC624?style=&logo=linux&logoColor=black)
![](https://img.shields.io/badge/Windows-10/11-2376bc?style=flat&logo=windows&logoColor=ffffff)
![](https://img.shields.io/badge/MacOS-333?style=flat&logo=apple&logoColor=ffffff)



![](https://img.shields.io/github/license/sor2171/Super_Frame_Vision.svg)
![](https://img.shields.io/badge/Kotlin_Multiplatform-7F52FF?style=flat&logo=kotlin&logoColor=white)

zh | [en](./README_en.md)


---

这是一个 Kotlin 多平台项目，目标平台为桌面端（JVM）。

本项目的目的是提供一个多平台的，基于 [`FFmpeg Kit`](https://github.com/akashskypatel/ffmpeg-kit-builders)
和 [`ONNX Runtime`](https://github.com/microsoft/onnxruntime) 的超分插帧方案。并尽可能地提供更多的选项与好看易读的 UI。

## 关于 AI 模型

我将不会把模型文件发布到 GitHub 上，大家可以在以下链接下载，并放置于
`./shared/src/commonMain/composeResources/files/models`，不要忘记重命名

[`rife2_26h.onnx`](shared/src/commonMain/composeResources/files/rife2_26h.onnx)

[`real-esrgan_x2.onnx`](shared/src/commonMain/composeResources/files/real-esrgan_x2.onnx)

[`real-esrgan_x4.onnx`](shared/src/commonMain/composeResources/files/real-esrgan_x4.onnx)

[`real-esrgan_x4plus_anime_6B.onnx`](shared/src/commonMain/composeResources/files/real-esrgan_x4plus_anime_6B.onnx)

[RIFE](https://huggingface.co/SOR2171/RIFE-ONNX/tree/main)

[RealESRGAN](https://huggingface.co/SceneWorks/real-esrgan-onnx/tree/main)

[RealESRGAN_x4plus_anime](https://huggingface.co/mhmtaufiq/realesrgan-onnx/tree/main)

## 如何运行

* [/shared](./shared/src) 用于存放跨 Compose 多平台应用共享的代码。
  它包含以下几个子文件夹：
  - [commonMain](./shared/src/commonMain/kotlin) 用于存放所有目标平台共享的代码。
  - 其他文件夹用于存放仅编译到对应平台的 Kotlin 代码。
    例如，如果你想修改桌面端（JVM）特有的部分，[jvmMain](./shared/src/jvmMain/kotlin)
    文件夹就是合适的位置。

### 运行应用

使用 IDE 工具栏中运行控件提供的运行配置。你也可以使用以下命令和选项：

- 桌面端应用：
  - 热重载：`./gradlew :desktopApp:hotRun --auto`
  - 标准运行：`./gradlew :desktopApp:run`

### 运行测试

使用 IDE 编辑器侧边的运行按钮，或使用 Gradle 任务运行测试：

- 桌面端测试：`./gradlew :shared:jvmTest`

---

了解更多关于 [Kotlin 多平台](https://www.jetbrains.com.cn/en-us/help/kotlin-multiplatform-dev/get-started.html)…
