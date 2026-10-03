# SuperFrameVision - Agent Guidelines & Engineering Specifications

Welcome to **SuperFrameVision**. This document serves as the single source of truth for AI agents
(and human contributors) interacting with this codebase. All agents **must** adhere strictly to the
guidelines, architecture patterns, and constraints described herein.

---

## 1. Project Overview & Positioning (项目概况与定位)

- **Name**: SuperFrameVision
- **Core Purpose**: A Kotlin Multiplatform (KMP) application primarily targeting Desktop
  (JVM), focusing on video super-resolution (超分辨率), frame interpolation (插帧), high-performance
  video frame extraction, and computer vision / media processing.
- **Media Engine**: Powered by [`ffmpeg-kit-kmp`](https://github.com/SOR2171/FFmpeg-Kit_KMP) for
  demuxing, decoding, frame extraction, media probing, and stream transcoding.
- **Target Platforms**: Desktop JVM (Windows x64, Linux x64, macOS Apple Silicon)

---

## 2. Agent Operational Constraints (Agent 核心行为准则与红线)

All agents working on this project must strictly comply with the following principles:

1. **Strict Scope Control ("不干多余的事")**:
    - Focus exclusively on the specific task assigned by the user.
    - Do not scan irrelevant files, explore unrelated directories, or make unsolicited changes.
    - Do not modify files outside the direct scope of the user's request.
2. **No Inspection of External Library Binaries**:
    - **Never** attempt to decompile, read, or parse third-party dependency binaries, library
      packages, or compiled artifacts (such as `.aar`, `.jar`, `.so`, `.dylib`, `.dll`, or Gradle
      dependency caches).
    - Base all API interactions on official public interfaces, project documentation, or user input.
3. **Consult When Unclear ("看不懂就问")**:
    - If requirements, contracts, or intent are ambiguous, ask the user directly for clarification
      rather than guessing or fabricating assumptions.
4. **Mandatory Release Version Date Consistency Check (Release 发版版本号与日期一致性校验)**:
    - Whenever the AI agent is instructed to perform or prepare a **Release build** (e.g. building native desktop distributions via `:desktopApp`), the agent **MUST** explicitly verify that the release version matches the build's actual calendar date.
    - The project version formula is defined in [`desktopApp/build.gradle.kts`](./desktopApp/build.gradle.kts):
      - `major = currentYear - 2000`
      - `minor = currentMonth`
      - `build = currentDay * 100 + currentHour`
      - Format: `"$major.$minor.$build"` (e.g., date `2026-10-03 17:xx` -> `26.10.317`).
    - If a release tag/version is passed via `TAG` environment variable (e.g. `TAG=v26.10.317`), the agent **must** cross-check that the `TAG`'s major, minor, and day values match today's date. Mismatched dates or stale versions must never be built into a release without warning or correcting!

---

## 3. FFmpeg Kit KMP Integration & Guidelines (音视频组件调用规范)

The project integrates `ffmpeg-kit-kmp` for media processing:

### 3.1 Dependency Declaration

```kotlin
commonMain.dependencies {
    // https://github.com/SOR2171/FFmpeg-Kit_KMP
    implementation("io.github.sor2171:ffmpeg-kit-kmp:0.11.5")
}
```

### 3.2 Standard API Usage

```kotlin
import io.github.sor2171.ffmpegkitkmp.FFmpegRunner
```

#### Media Information Probing (`FFprobe`)

Always use structured JSON output when retrieving video parameters (dimensions, framerate, codec,
duration, streams):

```kotlin
val probeResult = FFmpegRunner.ffprobe(
    "-v quiet",
    "-print_format json",
    "-show_format",
    "-show_streams",
    videoPath
)
```

#### Media Execution & Frame Extraction (`FFmpeg`)

Execute FFmpeg commands asynchronously off the main/UI thread:

```kotlin
// Example: Synthetic source or command execution
val output = FFmpegRunner.execute(
    "-re",
    "-f lavfi",
    "-i testsrc=duration=10:size=1920x1080:rate=60",
    "-f null -"
)
```

### 3.3 Best Practices for FFmpeg Calls

- **Threading & Coroutines**: Always dispatch FFmpeg/FFprobe operations onto dedicated background
  dispatchers (`Dispatchers.IO` / `Dispatchers.Default`). Never block UI threads (e.g., Compose UI / EDT).
- **Process & Resource Safety**: Ensure proper timeout controls and process cancellation handling
  when processing large video files or infinite streams.
- **Log Levels**: Use `-v error` or `-v warning` in production pipelines to avoid saturating memory
  buffers with verbose FFmpeg logging.
- **Path Handling**: Handle file paths with proper platform abstraction; escape paths containing
  spaces or special characters correctly.

---

## 4. Open Source & Licensing Compliance (开源许可与合规防线)

> [!IMPORTANT]
> **Strict compliance with GPL-3.0 is mandatory.**

1. **Kotlin Codebase License**: MIT License (applies to original Kotlin source code).
2. **Bundled FFmpeg Artifact License**:
    - Media dependency [`ffmpeg-kit-kmp`](https://github.com/SOR2171/FFmpeg-Kit_KMP) (`io.github.sor2171:ffmpeg-kit-kmp:0.11.5`) bundles `bundle-video_hw-shared-gpl-release`.
    - **Consequently, the distributed binary containing this artifact is subject to GPL-3.0**.
3. **Distribution Rules**:
    - When introducing new dependencies or restructuring modules, preserve licensing headers and do not combine proprietary closed-source code in a manner that violates GPL terms.
    - Retain full license texts ([`LICENSE`](./LICENSE)) where applicable.

---

## 5. Technology Stack & Architectural Patterns (技术栈与架构模式)

- **Language**: Kotlin Multiplatform (KMP, latest stable Kotlin).
- **UI Framework**: Jetpack Compose / Compose Multiplatform (declarative, reactive UI).
- **Asynchronous Flow**: Kotlin Coroutines (`CoroutineScope`, `Flow`, `StateFlow`, `SharedFlow`).
- **Vision & Processing Pipeline**:
    - Demuxing / Extraction: `ffmpeg-kit-kmp`
    - Processing / Inference: Frame interpolation & super-resolution algorithms (model runners /
      NDK / shader-based processing).
    - Memory Management: Avoid memory leaks during high-resolution frame processing. Recycle
      bitmaps, byte buffers, and native memory pointers promptly.

---

## 6. Composable UI Architecture & Services (UI 组件与服务架构)

The UI layer is built with Compose Multiplatform, using a unidirectional data flow (UDF) pattern.
Starting from the top-level container [`App.kt`](./shared/src/commonMain/kotlin/io/github/sor2171/superframevision/App.kt), the interface
is modularized into discrete screens backed by specialized domain services:

### 6.1 Top-Level Entry & Screens

#### 1. `App()` ([`App.kt`](./shared/src/commonMain/kotlin/io/github/sor2171/superframevision/App.kt))

- **Responsibilities & Functionality**:
    - The root Composable container of the application.
    - Manages global navigation structure via `NavigationRail` across `Screens.Home`,
      `Screens.Process`, `Screens.Settings`, and `Screens.Info`, animated using `AnimatedContent`.
    - Dynamically configures Material 3 theming (`rememberDynamicColorScheme`) based on user
      preferences and system dark mode.
    - Hosts and coordinates application-wide state: active screen, processing state
      (`isProcessing`), task queue (`queueFileList`), and frame inference progress / remaining time
      metrics.
    - Initializes and provides cross-platform dialog launchers using FileKit (`filePickerLauncher`,
      `saverPickerLauncher`, `directoryPickerLauncher`).
- **Services & Repositories Utilized**:
    - `ProcessLauncher`: Orchestrates queue processing jobs, cancellation, and execution lifecycle.
    - `SettingsRepository`: Loads persisted user settings at startup (`SettingsRepository.load()`)
      and exposes the `settings` StateFlow.
    - `FileKit`: Handles cross-platform file selection, saving, and directory picking.
    - `currentPlatform()`: Resolves current runtime environment (Desktop JVM / Android).

#### 2. `HomeScreen()` ([`HomeScreen.kt`](./shared/src/commonMain/kotlin/io/github/sor2171/superframevision/ui/screens/HomeScreen.kt))

- **Responsibilities & Functionality**:
    - The main input dashboard and job dispatch screen.
    - Provides a welcome banner and media ingestion targets: supports drag-and-drop file imports
      (`dragAndDropTarget`) and native file chooser dialogs.
    - Offers a segmented button control (`SingleChoiceSegmentedButtonRow`) to toggle between
      processing modes (`ProcessType`: `ImageSR`, `VideoSR`, `VideoFI`, `VideoSRFI`).
    - Displays the pending job queue (`queueFileList`) in an editable scroll list (`ScrollColumn`),
      allowing individual item deletion.
    - Hosts the primary execution trigger button with animated color transitions, switching between
      starting a queue job and stopping active tasks.
- **Services & Repositories Utilized**:
    - `ProcessLauncher`: Initiates (`start()`) or aborts (`cancel()`) the queue execution pipeline.
    - `FileKit`: Facilitates multi-file picking for incoming media tasks.

#### 3. `ProcessScreen()` ([`ProcessScreen.kt`](./shared/src/commonMain/kotlin/io/github/sor2171/superframevision/ui/screens/ProcessScreen.kt))

- **Responsibilities & Functionality**:
    - Real-time task progress monitoring and live execution console.
    - Visualizes overall task progress via `LinearProgressIndicator`, displaying completed vs. total
      frame counts and dynamic remaining duration estimation (`formatDuration`).
    - Lists individual queue items with live status badges (Pending, Processing).
    - Integrates an in-app console log viewer (`rememberConsoleState`) rendering standard output and
      error streams in real time with text selectable support (`SelectionContainer`).
    - Provides action buttons to clear console output buffers and export logs to disk via a file
      saver dialog.
- **Helper Composables**:
    - `rememberConsoleState(redirectSystemOut = true)`: Captures `System.out` and `System.err`
      streams via custom `OutputStream` redirects into memory for UI log display.
- **Services & Repositories Utilized**:
    - `ProcessLauncher`: Provides live progress statistics and handles runtime cancellation.
    - `FileUtils`: Exports collected log text into user-specified destination files
      (`FileUtils.write`).
    - `FileKit`: Launches native file saver picker for log export.

#### 4. `SettingsScreen()` ([`SettingsScreen.kt`](./shared/src/commonMain/kotlin/io/github/sor2171/superframevision/ui/screens/SettingsScreen.kt))

- **Responsibilities & Functionality**:
    - Application configuration and processing pipeline tuning interface.
    - **Working Directory**: Selects cache directory type (`WorkingDirType`: SystemTemp, etc.) and
      provides a one-click button to purge intermediate frames and cache (`FileUtils.clearTmp`).
    - **Appearance**: Selects theme seed colors from predefined palettes (`Const.colorList`).
    - **Compute Resources**: Configures worker threads for super-resolution (`upscaleThread`) and
      frame interpolation (`inferThread`).
    - **AI Device Selection**: Selects physical Vulkan GPU devices or falls back to CPU execution.
    - **Video Encoding Parameters**: Customizes target container (`VideoFormat`), encoder codec
      (`VideoCodec`: libx264, libx265), quality preset (`VideoQuality`), and custom video output
      directory.
    - Provides save confirmation and reset actions.
- **Services & Repositories Utilized**:
    - `SettingsRepository`: Supplies active configurations and persists changes via
      `SettingsRepository.save(newSettings)`.
    - `NcnnRunner`: Probes available Vulkan compute devices (`NcnnRunner.getAvailableDevices()`).
    - `FileUtils`: Manages temporary workspace cleanup.
    - `FileKit`: Launches directory picker for customized export folder selection.

#### 5. `InfoScreen()` ([`InfoScreen.kt`](./shared/src/commonMain/kotlin/io/github/sor2171/superframevision/ui/screens/InfoScreen.kt))

- **Responsibilities & Functionality**:
    - Project information, version metadata, and developer attribution screen.
    - Displays software specifications, functional highlights, and license notices from
      `Const.SOFTWARE_INFO`.
    - Renders interactive external link cards (`LinkCard`) navigating to the project's GitHub
      repository, Bilibili channel, community QQ groups, and sponsor portals.
- **Services & Repositories Utilized**:
    - Platform URI / URL launcher to open external links in browser.

### 6.2 Core Domain Services Reference

- **`ProcessLauncher`**: Top-level job queue manager; orchestrates batch tasks, caches reuse
  checking, and dispatches processing sessions.
    - Key methods: `start()`, `cancel()`
- **`MediaProcessor`**: High-level media processing coordinator; integrates frame extraction, frame
  rate probing, renumbering, AI inference, and video encoding inside a disposable session.
    - Key methods: `createSession()`, `extractFrames()`, `processSuperResolution()`,
      `inferLeftFrames()`, `encodeToVideo()`
    - Subordinate engines:
        - **`NcnnRunner`**: Native NCNN AI inference engine; manages Vulkan GPU devices and executes
          Real-ESRGAN and RIFE (frame interpolation) models.
            - Key methods: `getAvailableDevices()`, `executeUpscale()`, `executeInterpolation()`
            - Native C API file location: [`c_api.cpp`](./shared/src/jvmMain/kotlin/io/github/sor2171/superframevision/core/service/c_api.cpp)
        - **`FFmpegRunner`**: Media foundation engine from `ffmpeg-kit-kmp`; handles video demuxing,
          frame extraction, probing, and final video transcoding.
            - Key methods: `execute()`, `ffprobe()`
- **`SettingsRepository`**: StateFlow-backed repository for application settings persistence using
  JSON serialization.
    - Key methods / properties: `settings`, `load()`, `save()`
- **`FileUtils`**: Okio-based file system utilities for cache clearing, file copying/moving, and log
  exporting.
    - Key methods: `clearTmp()`, `write()`, `isSameFile()`

---

## 7. Code Style & Engineering Standards (编码与工程规范)

1. **Kotlin Conventions**: Follow standard Kotlin coding conventions and Google Android style
   guides.
2. **State Management**:
    - Keep UI components stateless wherever feasible.
    - Use unidirectional data flow (UDF / MVI / MVVM).
    - Expose immutable states (`StateFlow<T>`) to the view layer.
3. **Error Handling**:
    - Avoid empty `catch` blocks.
    - Return typed results (`Result<T>` or custom domain sealed classes) for video processing and
      FFmpeg executions.
    - Distinguish user-correctable errors (e.g., unsupported codec, missing file) from system fatal
      errors.
4. **Documentation**:
    - Write clear KDoc for public APIs, pipeline contracts, and native interop functions.
    - Keep comments concise and strictly relevant.

---

## 8. Build & Verification Commands (构建与验证)

Based on [`shared/build.gradle.kts`](./shared/build.gradle.kts), the `shared` module is configured
as a Kotlin Multiplatform project with a Desktop JVM target (`kotlin.jvm()`) and Compose
Multiplatform support.

### 8.1 Compilation & Assembly Tasks

Execute build tasks via Gradle wrapper:

```bash
# Compile and verify Kotlin JVM source sets
./gradlew :shared:compileKotlinJvm

# Assemble all shared module artifacts
./gradlew :shared:assemble
```

### 8.2 Testing & Verification Tasks

Run unit tests defined in `shared/src/commonTest/kotlin`.

> [!IMPORTANT]
> **Function-Level Testing Required (必须指定测试函数)**:
> When executing tests, agents **must explicitly specify the targeted test class and
function/method** using the `--tests` flag. Do **not** run the entire test suite indiscriminately.

Concrete example based on [`shared/src/commonTest/kotlin/io/github/sor2171/superframevision/FFmpegTest.kt`](./shared/src/commonTest/kotlin/io/github/sor2171/superframevision/FFmpegTest.kt):

```bash
# Run a specific test function/method (MANDATORY: specify package, class, and function name)
./gradlew :shared:jvmTest --tests "io.github.sor2171.superframevision.FFmpegTest.FFmpegVersion"

# Pattern matching for a specific test function
./gradlew :shared:jvmTest --tests "*FFmpegTest.FFmpegVersion*"
```

### 8.3 Desktop App Packaging & Release Builds (桌面应用打包与 Release 构建)

The desktop application module (`desktopApp`) handles multiplatform native packaging (Windows MSI/EXE, macOS DMG, Linux DEB/RPM/AppImage) configured in [`desktopApp/build.gradle.kts`](./desktopApp/build.gradle.kts).

#### Versioning Rule (`appVersion`)
The application package version is dynamically calculated from the build timestamp or the `TAG` environment variable:

```kotlin
val appVersion = System.getenv("TAG")?.removePrefix("v")
    ?: run {
        val now = LocalDateTime.now()

        val major = now.year - 2000
        val minor = now.monthValue
        val build = now.dayOfMonth * 100 + now.hour

        println(build)

        "$major.$minor.$build"
    }
```

> [!CAUTION]
> **Strict Rule for AI Release Builds (AI 构建 Release 时的强制日期校验)**:
> When the AI agent is instructed to perform a Release build or generate distribution packages:
> - **Must verify consistency**: The agent **MUST** explicitly verify that the version string (`appVersion` or `TAG`) matches the **actual current date**:
>   - `major == currentYear - 2000`
>   - `minor == currentMonth`
>   - `(build / 100) == currentDayOfMonth`
> - If `TAG` is supplied (e.g. `TAG=v26.10.317`), ensure the tag matches today's date (e.g. October 3, 2026, 17:00 ~ 18:00). Never build a release artifact with an outdated or mismatched version tag!

#### Packaging Commands
```bash
# Package Release distribution with ProGuard obfuscation & minification
./gradlew :desktopApp:packageReleaseDistributionForCurrentOS

# Platform-specific packaging targets
./gradlew :desktopApp:packageMsi        # Windows MSI installer
./gradlew :desktopApp:packageExe        # Windows EXE installer
./gradlew :desktopApp:packageDmg        # macOS DMG disk image
./gradlew :desktopApp:packageDeb        # Linux DEB package
```

Release artifacts are generated in: `desktopApp/build/compose/binaries/main-release/`
