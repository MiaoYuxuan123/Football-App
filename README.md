# 足智多谋

<!-- Index / TOC -->
## Table of Contents / 目录

- English 
  - [Project Overview](#project-overview)
  - [Key Features](#key-features)
  - [Tech Stack & Requirements](#tech-stack--requirements)
  - [Quick Start (Developer)](#quick-start-developer)
  - [Backend Integration](#backend-integration)
  - [Project Layout & Key Files](#project-layout--key-files)
  - [Troubleshooting & Debugging](#troubleshooting--debugging)
  - [Testing & Logs](#testing--logs)
  - [Roadmap & Suggestions](#roadmap--suggestions)
  - [Contributing](#contributing)

- 中文
  - [项目概述](#项目概述)
  - [主要功能](#主要功能)
  - [技术栈与环境要求](#技术栈与环境要求)
  - [快速开始（开发者）](#快速开始开发者)
  - [后端对接说明](#后端对接说明)
  - [项目结构与关键文件](#项目结构与关键文件)
  - [调试与排错](#调试与排错)
  - [测试与日志查看](#测试与日志查看)
  - [改进建议与待办](#改进建议与待办)
  - [贡献指南](#贡献指南)

---

## Project Overview

Football Training Companion is an Android app that helps football enthusiasts record practice sessions, perform real-time pose analysis with MediaPipe, run offline pose-overlay post-processing, upload videos to a backend for advanced analysis/LLM feedback, and store structured training records locally for visualization and tracking.

The README is bilingual (English above, 中文 below). Use the Table of Contents to jump to any section.


## Key Features

- CameraX live preview + Recorder for session capture (handles orientation changes).
- MediaPipe PoseLandmarker for live frame-level inference and lightweight scoring.
- Offline post-processing pipeline (`PoseVideoProcessor`) that decodes MP4, runs pose inference per-frame, draws overlays, and re-encodes output MP4 with audio preserved.
- Backend integration: upload recorded videos (multipart `file`) for server analysis; server returns structured JSON (`feedback`, `metrics`) that is saved and shown in UI.
- Local persistence: training records and milestones stored in SQLite; avatars and videos saved to app-private storage.
- Video playback with ExoPlayer-based UI that shows backend feedback alongside video.


## Tech Stack & Requirements

- Languages: Java (primary) and optional Kotlin
- Libraries: CameraX, MediaPipe Tasks (PoseLandmarker), ExoPlayer, OkHttp
- Storage: SQLite (MilestoneDbHelper) + SharedPreferences (SPUtils)
- Build: Gradle Kotlin DSL (AGP 9+ recommended)

Recommended environment:
- Android Studio Iguana (or newer)
- JDK 11
- Device: Android 10 (API 29) or later (real device recommended for CameraX recording)


## Quick Start (Developer)

1. Clone and open the project

```bash
git clone <repo-url>
cd Football-App
# Open in Android Studio
```

2. Build & install (debug)

```bash
./gradlew clean assembleDebug
./gradlew installDebug
adb shell am start -n com.example.football/.ui.splash.SplashActivity
```

3. Notes
- Grant runtime permissions for Camera / Microphone / Storage the first time.
- Prefer testing on a real device for camera/recorder behavior.


## Backend Integration

The client uploads video files (multipart/form-data) using the `file` field. The server should accept the file, run analysis (MediaPipe / custom models / LLMs) and return JSON similar to:

```json
{
  "request_id":"...",
  "metrics":{ /* numeric metrics */ },
  "feedback":{
    "overall_score": 84.0,
    "action_summary": "...",
    "improvements": [ /* ... */ ]
  }
}
```

Client expectations and behavior:
- Endpoint constant: `ANALYZE_FAST_ENDPOINT` in `TrainFragment.java` (client trims whitespace before request).
- Client sends POST multipart with `file` (mime e.g. `video/mp4`).
- On success client: saves `feedbackJson` to the training record, updates average score calculation, shows result dialog, and allows viewing the processed overlay video (if provided).

Testing the endpoint:
- Use Postman / curl / Apipost to POST a sample MP4 as `file` and verify server response.


## Project Layout & Key Files

```
app/src/main/java/com/example/football/
  ├─ data/           # AppRepository, AppRepositoryImpl, data sync
  ├─ database/       # MilestoneDbHelper, entities (TrainRecord, MilestoneData)
  ├─ ui/             # Activities, Fragments (TrainFragment, ResultDetailActivity, ...)
  ├─ video/          # PoseVideoProcessor, PoseFrameDrawer (offline processing)
  └─ utils/          # HttpUtil, SPUtils, ToastUtils, VideoUtil
```

Key classes:
- `TrainFragment` — camera flow, recording, uploading, result dialog.
- `PoseVideoProcessor` — offline frame inference + overlay + re-encode.
- `AppRepositoryImpl` — unified storage API (SharedPreferences + SQLite).
- `MilestoneDbHelper` — DB schema and operations for training records.
- `ResultDetailActivity` — show analysis JSON, metrics, strengths/improvements/drills.


## Troubleshooting & Debugging

- View logs (adb) to debug frontend behavior:

```bash
adb logcat -v time | findstr "TrainFragment ResultDetailActivity VideoPlayerActivity"
```

- Common problems:
  - Upload fails silently: verify device network access and that server is reachable; check `ANALYZE_FAST_ENDPOINT` and server logs.
  - MediaPipe timestamp / rotation errors: ensure ImageAnalysis, Preview and VideoCapture share the same target rotation; the fragment registers a display listener and rebinds on rotation.
  - Video playback orientation: player uses metadata to adapt; check `MediaMetadataRetriever` on the file.


## Testing & Logs

- Unit tests (JVM):

```bash
./gradlew testDebug
```

- Instrumented tests (device/emulator):

```bash
./gradlew connectedDebugAndroidTest
```

- Quick log filtering:

```bash
adb logcat -v time | findstr "com.example.football"
```


## Roadmap & Suggestions

- Move backend URL config out of code (use `local.properties` or BuildConfig fields).
- Improve HttpUtil with retry/timeout handling and clearer UI alerts on failure.
- Clean `.idea` from VCS or add to `.gitignore` to reduce IDE noise.
- Upgrade or replace deprecated ExoPlayer APIs as needed.
- Add end-to-end integration tests covering upload → server response → record saving → playback.


## Contributing

Fork → branch → PR. Describe changes and testing steps in PR description.

---


<a name="项目概述"></a>
## 项目概述

足智多谋（Football Training Companion）是一款 Android 应用，帮助用户记录训练视频、使用 MediaPipe 做实时动作识别、对录制视频做离线姿态叠加、把视频上传后端获取基于 LLM 的训练反馈，并将结构化结果保存到本地供可视化展示与成长统计。


<a name="主要功能"></a>
## 主要功能

- CameraX 预览与录制，支持横竖屏适配。
- MediaPipe PoseLandmarker 实时帧级推理与打分（用于计数和本地辅助反馈）。
- 离线后处理（`PoseVideoProcessor`）：解码 MP4、逐帧推理、绘制关键点、编码输出并保留音频。
- 后端分析：录制完成自动或手动上传视频（multipart `file` 字段），后端返回 `feedback` 与 `metrics`，前端保存并在结果页展示。
- 本地持久化：SQLite 存储训练记录与里程碑，头像/视频保存在应用文件夹。


<a name="技术栈与环境要求"></a>
## 技术栈与环境要求

- 语言：Java 为主（项目中也可包含 Kotlin）
- 依赖：CameraX、MediaPipe Tasks、ExoPlayer、OkHttp
- 构建：Gradle（Kotlin DSL）
- 建议环境：Android Studio Iguana / JDK11 / Android 10+ 设备（真机优先）


<a name="快速开始开发者"></a>
## 快速开始（开发者）

1. 克隆仓库并用 Android Studio 打开：

```bash
git clone <repo-url>
cd Football-App
```

2. 构建并安装：

```bash
./gradlew clean assembleDebug
./gradlew installDebug
adb shell am start -n com.example.football/.ui.splash.SplashActivity
```

3. 注意事项：
- 首次运行授予摄像头/麦克风/存储权限；
- 模拟器摄像头能力有限，推荐真机测试录制与 MediaPipe 推理。


<a name="后端对接说明"></a>
## 后端对接说明

- 客户端上传字段：multipart/form-data，`file` 字段携带 MP4。
- 在 `TrainFragment.java` 中配置上传端点（`ANALYZE_FAST_ENDPOINT`），客户端会对常量做 `trim()`；建议把地址放到 `local.properties` 或 BuildConfig。
- 后端应返回 JSON（示例见上英部分），客户端将解析并保存 `feedbackJson` 到训练记录以用于平均分与统计。


<a name="项目结构与关键文件"></a>
## 项目结构与关键文件

（参考英文部分）


<a name="调试与排错"></a>
## 调试与排错

- 查看日志：

```bash
adb logcat -v time | findstr "TrainFragment ResultDetailActivity VideoPlayerActivity"
```

- 常见问题：网络不可达、端点错误、MediaPipe timestamp/rotation 异常（需确保 rotation 一致）等。请同时检查后端 uvicorn 日志与客户端 logcat。


<a name="测试与日志查看"></a>
## 测试与日志查看

见上文 `Testing & Logs`。


<a name="改进建议与待办"></a>
## 改进建议与待办

- 配置化后端地址、增强网络重试、清理 `.idea`、升级 ExoPlayer、增加集成测试。


<a name="贡献指南"></a>
## 贡献

欢迎提交 issue 或 PR，说明改动与测试步骤。



