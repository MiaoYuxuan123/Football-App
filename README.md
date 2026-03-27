# 足智多谋

一款专为足球爱好者打造的 Android 训练辅助应用，基于 MediaPipe 实现实时姿态分析、离线视频后处理，支持训练记录本地存储与后端 AI 反馈获取，助力科学提升足球训练效果。

  <p>多端适配 | 实时推理 | 离线处理 | 数据化训练</p>

<p>Java为主 · Android原生开发 · Gradle构建</p>

## Table of Contents / 目录

### 🔠 English

- [Project Overview](#project-overview)
- [Key Features](#key-features)
- [Tech Stack & Requirements](#tech-stack--requirements)
- [Quick Start (Developer)](#quick-start-developer)
- [Backend Integration](#backend-integration)
- [Project Structure](#project-structure)
- [Troubleshooting & Debugging](#troubleshooting--debugging)
- [Testing & Logs](#testing--logs)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [Contributors](#contributors)

### 📝 中文

- [项目概述](#项目概述)
- [主要功能](#主要功能)
- [技术栈与环境要求](#技术栈与环境要求)
- [快速开始（开发者）](#快速开始开发者)
- [后端对接说明](#后端对接说明)
- [项目结构](#项目结构)
- [调试与排错](#调试与排错)
- [测试与日志查看](#测试与日志查看)
- [开发规划](#开发规划)
- [贡献指南](#贡献指南)
- [贡献者](#贡献者)

------

## Project Overview

Football Training Companion (Chinese Name: 足智多谋) is an Android native application designed for football enthusiasts and trainees. It integrates **CameraX** for video capture, **MediaPipe PoseLandmarker** for real-time human pose inference, and supports offline video post-processing with pose overlay. The app can upload training videos to the backend server for advanced AI/LLM analysis, obtain structured feedback and metrics, and store all training records locally through SQLite for visualization and training progress tracking.

The app supports screen orientation adaptation for all core functions, and all video processing retains original audio, ensuring a complete training recording and analysis experience.

## Key Features

- **CameraX Video Capture**: Real-time camera preview and training video recording, perfect adaptation to screen orientation changes, support for camera/microphone permission management.
- **Real-time Pose Analysis**: MediaPipe PoseLandmarker for frame-level human pose inference, lightweight real-time scoring and action counting for on-site training guidance.
- **Offline Video Post-processing**: Built-in `PoseVideoProcessor` for MP4 decoding, per-frame pose inference, key point overlay drawing, and re-encoding output (audio preserved).
- **Backend AI Integration**: Multipart/form-data video upload, obtain structured JSON feedback (overall score, action summary, improvement suggestions) and metrics from the server.
- **Local Persistence**: SQLite stores structured training records and milestones; SharedPreferences manages app configuration; private storage saves avatars and training videos.
- **Result Visualization**: ExoPlayer-based video playback, synchronous display of backend AI feedback and training metrics, support for detailed result viewing and data statistics.

## Tech Stack & Requirements

### Core Tech Stack

|    Category     |                   Technology/Library                    |
| :-------------: | :-----------------------------------------------------: |
|  Main Language  |                      Java (98.9%)                       |
|   Build Tool    |         Gradle Kotlin DSL (AGP 9+ recommended)          |
| Camera & Video  |     CameraX (capture/preview), ExoPlayer (playback)     |
| Pose Inference  |            MediaPipe Tasks (PoseLandmarker)             |
| Network Request |         OkHttp (multipart upload, HTTP request)         |
|  Local Storage  | SQLite (MilestoneDbHelper), SharedPreferences (SPUtils) |
|   Tool Class    |      Custom util (HttpUtil, ToastUtils, VideoUtil)      |

### Environment Requirements

- **Development Tool**: Android Studio Iguana or newer
- **JDK Version**: JDK 11
- **Android System**: Android 10 (API 29) or later (**real device recommended**)
- **Permissions**: Camera, Microphone, Storage (runtime permission required)
- **Network**: For backend video upload and AI analysis (offline functions available without network)

## Quick Start (Developer)

### 1. Clone the Repository

```bash
git clone https://github.com/MiaoYuxuan123/Football-App.git
cd Football-App
```

### 2. Open & Build in Android Studio

1. Open the project with Android Studio (Iguana+ recommended)
2. Wait for Gradle sync and dependency download to complete
3. Connect a real Android device (API 29+) and enable USB debugging

### 3. Build & Install Debug Version

```bash
# Clean and build debug apk
./gradlew clean assembleDebug
# Install apk to connected device
./gradlew installDebug
# Start the app directly
adb shell am start -n com.example.football/.ui.splash.SplashActivity
```

### 4. Key Notes

- Grant **Camera, Microphone, Storage** runtime permissions when opening the app for the first time
- Emulator has limited camera and pose inference capabilities, **real device testing is strongly recommended**
- Ensure the device network is available when using backend upload and analysis functions

## Backend Integration

### Client Upload Rules

1. **Request Type**: POST request with `multipart/form-data`
2. **Upload Field**: `file` (MP4 video file, mime: `video/mp4`)
3. **Endpoint Configuration**: Modify the `ANALYZE_FAST_ENDPOINT` constant in `TrainFragment.java` (the client automatically trims whitespace of the endpoint)
4. **Network Tool**: Custom `HttpUtil` for request sending and response parsing

### Backend Response Requirement (JSON)

The server needs to return structured JSON after video analysis (LLM/MediaPipe), the format is as follows:

```json
{
  "request_id": "unique-request-id",
  "metrics": {
    "speed": 8.5,
    "posture_standard_rate": 0.82,
    "action_count": 15
  },
  "feedback": {
    "overall_score": 84.0,
    "action_summary": "Your kicking posture is basically standard, but the knee bending angle is insufficient when taking off.",
    "improvements": [
      "Increase the knee bending angle to 90 degrees when taking off",
      "Keep your upper body stable during the kicking process",
      "Speed up the swing speed of your calf"
    ]
  }
}
```

### Client Post-processing

- Save the complete `feedbackJson` to the local SQLite training record
- Automatically calculate and update the average score of historical training
- Pop up the result dialog and jump to the detailed result page
- Support synchronous playback of training video and AI feedback

### Test the Endpoint

Use Postman/Apipost/curl to send a test request:

```bash
curl -X POST -F "file=@your-training-video.mp4" [your-analyze-endpoint]
```

## Project Structure

The project follows the **Android MVVM-like** directory design, with clear separation of data, UI, video processing and tool classes, easy to expand and maintain.

```
app/src/main/java/com/example/football/
 ├─ data/          # Data layer: AppRepository, AppRepositoryImpl (data sync & unified API)
 ├─ database/      # Local storage: MilestoneDbHelper, entity classes (TrainRecord, MilestoneData)
 ├─ ui/            # UI layer: Activities, Fragments (SplashActivity, TrainFragment, ResultDetailActivity...)
 ├─ video/         # Video processing: PoseVideoProcessor, PoseFrameDrawer (offline inference & overlay)
 └─ utils/         # Tool classes: HttpUtil, SPUtils, ToastUtils, VideoUtil (universal functions)
```

### Core Key Classes

- `TrainFragment`: Core page, integrates camera preview, recording, video upload, result dialog
- `PoseVideoProcessor`: Offline video processing core, responsible for decoding, pose inference, overlay drawing, re-encoding
- `MilestoneDbHelper`: SQLite database helper, manages training record table creation, addition, deletion, modification and query
- `AppRepositoryImpl`: Unified data storage API, encapsulates SQLite and SharedPreferences operations
- `ResultDetailActivity`: Training result detail page, displays AI feedback, metrics and video playback
- `HttpUtil`: Network tool class, encapsulates OkHttp multipart upload and general HTTP requests

## Troubleshooting & Debugging

### View Key Logs

Filter the log of core pages/functions through adb logcat for quick debugging:

```bash
# Windows
adb logcat -v time | findstr "TrainFragment ResultDetailActivity VideoPlayerActivity"
# Mac/Linux
adb logcat -v time | grep "TrainFragment ResultDetailActivity VideoPlayerActivity"
# View all app logs
adb logcat -v time | findstr "com.example.football"
```

### Common Problems & Solutions

1. **Video upload fails silently**
   - Check if the device network is available and the backend server is reachable
   - Verify the `ANALYZE_FAST_ENDPOINT` configuration (no extra whitespace)
   - Check backend server logs for request reception and analysis errors
2. **MediaPipe pose inference error (timestamp/rotation)**
   - Ensure ImageAnalysis, Preview and VideoCapture share the same target rotation
   - The app has a built-in display listener, which rebinds the camera on screen rotation
3. **Video playback orientation mismatch**
   - The player uses `MediaMetadataRetriever` to read video metadata for adaptive rotation
   - Check if the recorded video has normal orientation metadata
4. **Offline post-processing no audio**
   - The app retains the original audio by default during re-encoding
   - Check if the original recording has audio and the storage permission is granted
5. **Training records not saved**
   - Check SQLite database operation logs (MilestoneDbHelper)
   - Verify if the device storage is sufficient and the app has storage permission

## Testing & Logs

### Unit Tests (JVM)

Run all local unit tests (no need for a real device/emulator):

```bash
./gradlew testDebug
```

### Instrumented Tests (Device/Emulator)

Run instrumented tests on the connected real device/emulator:

```bash
./gradlew connectedDebugAndroidTest
```

### Log Filtering Quick Reference

|       Scenario       |  Command (Windows)   |                                | Command (Mac/Linux)  |                             |
| :------------------: | :------------------: | :----------------------------: | :------------------: | :-------------------------: |
|  Core function log   | `adb logcat -v time` |   `findstr "TrainFragment"`    | `adb logcat -v time` |   `grep "TrainFragment"`    |
| Network request log  | `adb logcat -v time` |      `findstr "HttpUtil"`      | `adb logcat -v time` |      `grep "HttpUtil"`      |
|     Database log     | `adb logcat -v time` | `findstr "MilestoneDbHelper"`  | `adb logcat -v time` | `grep "MilestoneDbHelper"`  |
| Video processing log | `adb logcat -v time` | `findstr "PoseVideoProcessor"` | `adb logcat -v time` | `grep "PoseVideoProcessor"` |

## Roadmap

### Short-term (v1.1)

-  Basic training recording, real-time analysis and offline processing
-  Move backend endpoint from hard code to `local.properties`/BuildConfig (configuration 化)
-  Enhance HttpUtil: add request timeout, retry mechanism and clear UI error prompts
-  Clean up `.idea` directory and add to `.gitignore` to reduce VCS noise

### Medium-term (v1.2)

-  Upgrade ExoPlayer and replace deprecated APIs
-  Add training record classification and search functions
-  Optimize MediaPipe inference speed and reduce battery consumption
-  Support custom pose analysis parameters (adjust inference accuracy/speed)

### Long-term (v2.0)

-  Add end-to-end integration tests (upload → server response → record save → playback)
-  Support multiple pose analysis models (custom MediaPipe models)
-  Add training plan customization and push functions
-  Support cloud synchronization of training records
-  Add multi-language support (English/Chinese)

------

## 项目概述

足智多谋（Football Training Companion）是一款面向足球爱好者的 Android 原生训练辅助应用，深度整合 CameraX 视频采集、MediaPipe 姿态识别技术，实现**实时训练姿态分析**、**离线视频姿态叠加后处理**，支持将训练视频上传至后端服务器获取 AI/LLM 智能分析反馈，并通过 SQLite 将所有训练记录本地持久化，助力用户数据化、科学化跟踪训练进度，提升足球训练效果。

应用所有核心功能均支持屏幕横竖屏适配，视频处理全程保留原始音频，保障完整的训练记录与分析体验。

## 主要功能

- **CameraX 视频采集**：实时相机预览与训练视频录制，完美适配屏幕横竖屏切换，支持相机 / 麦克风权限管理。
- **实时姿态分析**：基于 MediaPipe PoseLandmarker 实现帧级人体姿态推理，轻量级实时打分与动作计数，提供现场训练指导。
- **离线视频后处理**：内置`PoseVideoProcessor`，支持 MP4 解码、逐帧姿态推理、关键点叠加绘制，重编码输出并保留原始音频。
- **后端 AI 对接**：Multipart/form-data 格式视频上传，从服务端获取结构化 JSON 反馈（综合评分、动作总结、改进建议）与训练指标。
- **本地数据持久化**：SQLite 存储结构化训练记录与里程碑；SharedPreferences 管理应用配置；应用私有存储保存头像与训练视频。
- **结果可视化展示**：基于 ExoPlayer 的视频播放，同步展示后端 AI 反馈与训练指标，支持训练结果详情查看与数据统计。

## 技术栈与环境要求

### 核心技术栈

|     分类     |                       技术 / 依赖库                       |
| :----------: | :-------------------------------------------------------: |
| 主要开发语言 |                       Java (98.9%)                        |
|   构建工具   |             Gradle Kotlin DSL（推荐 AGP 9+）              |
| 相机 / 视频  |         CameraX（采集 / 预览）、ExoPlayer（播放）         |
|   姿态推理   |             MediaPipe Tasks（PoseLandmarker）             |
|   网络请求   |               OkHttp（分片上传、HTTP 请求）               |
|   本地存储   | SQLite（MilestoneDbHelper）、SharedPreferences（SPUtils） |
|    工具类    |       自定义工具（HttpUtil、ToastUtils、VideoUtil）       |

### 环境要求

- **开发工具**：Android Studio Iguana 或更高版本
- **JDK 版本**：JDK 11
- **运行系统**：Android 10（API 29）及以上（**推荐真机运行**）
- **权限要求**：相机、麦克风、存储（均为运行时权限）
- **网络要求**：后端视频上传与 AI 分析功能需网络（无网络可使用离线功能）

## 快速开始（开发者）

### 1. 克隆仓库

```bash
git clone https://github.com/MiaoYuxuan123/Football-App.git
cd Football-App
```

### 2. Android Studio 打开并构建

1. 使用 Android Studio（推荐 Iguana+）打开项目
2. 等待 Gradle 同步与依赖下载完成
3. 连接安卓真机（API 29+）并开启 USB 调试

### 3. 构建并安装 Debug 版本

```bash
# 清理并构建Debug包
./gradlew clean assembleDebug
# 将包安装到已连接设备
./gradlew installDebug
# 直接启动应用
adb shell am start -n com.example.football/.ui.splash.SplashActivity
```

### 4. 关键注意事项

- 首次打开应用需授予**相机、麦克风、存储**运行时权限
- 模拟器的相机与姿态推理能力有限，**强烈推荐真机测试**
- 使用后端上传与分析功能时，确保设备网络通畅

## 后端对接说明

### 客户端上传规则

1. **请求类型**：POST 请求，采用`multipart/form-data`格式
2. **上传字段**：`file`（MP4 视频文件，mime 类型为`video/mp4`）
3. **端点配置**：修改`TrainFragment.java`中的`ANALYZE_FAST_ENDPOINT`常量（客户端会自动去除端点首尾空格）
4. **网络工具**：基于 OkHttp 封装的自定义`HttpUtil`，负责请求发送与响应解析

### 后端响应规范（JSON）

服务端对视频完成分析（LLM/MediaPipe）后，需返回结构化 JSON，格式如下：

```json
{
  "request_id": "唯一请求ID",
  "metrics": {
    "speed": 8.5,
    "posture_standard_rate": 0.82,
    "action_count": 15
  },
  "feedback": {
    "overall_score": 84.0,
    "action_summary": "你的踢球姿势基本标准，但起脚时膝盖弯曲角度不足，发力效率偏低。",
    "improvements": [
      "起脚时将膝盖弯曲角度提升至90度左右",
      "踢球过程中保持上半身稳定，减少晃动",
      "加快小腿摆动速度，提升击球力量"
    ]
  }
}
```

### 客户端后续处理

- 将完整的`feedbackJson`保存至本地 SQLite 训练记录
- 自动计算并更新历史训练的平均分
- 弹出结果提示弹窗，并跳转至结果详情页
- 支持训练视频与 AI 反馈同步播放

### 端点测试

可使用 Postman/Apipost/curl 发送测试请求，验证后端接口可用性：

```bash
curl -X POST -F "file=@本地训练视频.mp4" [你的分析接口地址]
```

## 项目结构

项目遵循**类 Android MVVM**目录设计，数据层、UI 层、视频处理层、工具类分层清晰，易于扩展与维护。

```
app/src/main/java/com/example/football/
 ├─ data/          # 数据层：仓库封装，统一数据操作API（数据同步、读写）
 ├─ database/      # 本地存储：SQLite助手类、实体类（训练记录、里程碑数据）
 ├─ ui/            # UI层：所有页面（启动页、训练页、结果详情页等Activity/Fragment）
 ├─ video/         # 视频处理层：离线姿态推理、视频叠加绘制核心类
 └─ utils/         # 工具层：网络、存储、吐司、视频处理等通用工具类
```

### 核心关键类

- `TrainFragment`：应用核心页面，整合相机预览、录制、视频上传、结果弹窗
- `PoseVideoProcessor`：离线视频处理核心，负责解码、姿态推理、叠加绘制、重编码
- `MilestoneDbHelper`：SQLite 数据库助手，管理训练记录表的增删改查与表结构创建
- `AppRepositoryImpl`：统一数据存储 API，封装 SQLite 与 SharedPreferences 操作
- `ResultDetailActivity`：训练结果详情页，展示 AI 反馈、训练指标与视频同步播放
- `HttpUtil`：网络工具类，封装 OkHttp 分片上传与通用 HTTP 请求

## 调试与排错

### 查看核心日志

通过 adb logcat 过滤核心页面 / 功能的日志，快速定位问题：

```bash
# Windows系统
adb logcat -v time | findstr "TrainFragment ResultDetailActivity VideoPlayerActivity"
# Mac/Linux系统
adb logcat -v time | grep "TrainFragment ResultDetailActivity VideoPlayerActivity"
# 查看应用所有日志
adb logcat -v time | findstr "com.example.football"
```

### 常见问题与解决方案

1. **视频上传无响应 / 失败**
   - 检查设备网络是否通畅，后端服务器是否可达
   - 验证`ANALYZE_FAST_ENDPOINT`配置（无多余空格、地址正确）
   - 查看后端服务器日志，确认请求是否接收、分析是否报错
2. **MediaPipe 姿态推理报错（时间戳 / 旋转问题）**
   - 确保 ImageAnalysis、Preview、VideoCapture 使用相同的目标旋转角度
   - 应用内置显示器监听器，横竖屏切换时会重新绑定相机，解决旋转适配
3. **视频播放方向与录制方向不一致**
   - 播放器通过`MediaMetadataRetriever`读取视频元数据实现自适应旋转
   - 检查录制的视频是否包含正常的方向元数据
4. **离线后处理的视频无音频**
   - 应用重编码时默认保留原始音频，无需额外配置
   - 检查原始录制视频是否有音频，以及应用是否获取到存储权限
5. **训练记录保存失败 / 不显示**
   - 查看 SQLite 数据库操作日志（MilestoneDbHelper 相关）
   - 验证设备存储是否充足，应用是否已获取存储权限

## 测试与日志查看

### 单元测试（JVM）

运行所有本地单元测试（无需真机 / 模拟器）：

```bash
./gradlew testDebug
```

### 仪器化测试（真机 / 模拟器）

在已连接的真机 / 模拟器上运行仪器化测试：

```
./gradlew connectedDebugAndroidTest
```

### 日志过滤速查

|     调试场景     |     Windows 命令     |                                |    Mac/Linux 命令    |                             |
| :--------------: | :------------------: | :----------------------------: | :------------------: | :-------------------------: |
| 核心训练功能日志 | `adb logcat -v time` |   `findstr "TrainFragment"`    | `adb logcat -v time` |   `grep "TrainFragment"`    |
|   网络请求日志   | `adb logcat -v time` |      `findstr "HttpUtil"`      | `adb logcat -v time` |      `grep "HttpUtil"`      |
|  数据库操作日志  | `adb logcat -v time` | `findstr "MilestoneDbHelper"`  | `adb logcat -v time` | `grep "MilestoneDbHelper"`  |
|   视频处理日志   | `adb logcat -v time` | `findstr "PoseVideoProcessor"` | `adb logcat -v time` | `grep "PoseVideoProcessor"` |

## 开发规划

### 短期规划（v1.1）

-  基础训练录制、实时分析与离线处理功能
-  将后端接口地址从硬编码迁移至`local.properties`/BuildConfig（配置化）
-  增强 HttpUtil：增加请求超时、重试机制，添加清晰的 UI 错误提示
-  清理`.idea`目录并加入.gitignore，减少版本控制冗余

### 中期规划（v1.2）

-  升级 ExoPlayer，替换已废弃的 API
-  增加训练记录分类、搜索功能
-  优化 MediaPipe 推理速度，降低设备功耗
-  支持自定义姿态分析参数（调整推理精度 / 速度）

### 长期规划（v2.0）

-  增加端到端集成测试（上传→服务端响应→记录保存→播放）
-  支持多姿态分析模型（自定义 MediaPipe 模型）
-  增加训练计划定制与推送功能
-  支持训练记录云同步
-  增加多语言支持（英 / 中）

------

## Contributing

We welcome all forms of contributions to the project! Whether it's bug fixes, feature development, documentation improvement or suggestion submission, please follow the steps below:

1. **Fork** the repository to your own GitHub account
2. **Create a feature branch** based on the `main` branch: `git checkout -b feature/your-feature-name`
3. **Commit your changes** with clear commit messages: `git commit -m "feat: add xxx function"` / `git commit -m "fix: resolve xxx bug"`
4. **Push** the branch to your forked repository: `git push origin feature/your-feature-name`
5. **Create a Pull Request** to the original repository, and describe the changes, testing steps and expected effects in detail

### Contribution Guidelines

- Follow the project's code style and directory structure
- Add corresponding unit tests/instrumented tests for new functions
- Ensure the code can be compiled and run normally before submitting PR
- Keep the commit message concise and clear, following the [Conventional Commits](https://www.conventionalcommits.org/) specification

## 贡献指南

欢迎各位开发者为项目贡献代码、修复 Bug、完善文档或提出建议！所有贡献请遵循以下步骤：

1. **Fork**本仓库到你的 GitHub 账号
2. 基于`main`分支**创建功能分支**：`git checkout -b feature/你的功能名`
3. **提交代码**并编写清晰的提交信息：`git commit -m "feat: 新增xxx功能"` / `git commit -m "fix: 修复xxx问题"`
4. 将分支**推送**至你的 Fork 仓库：`git push origin feature/你的功能名`
5. 向原仓库**创建 Pull Request**，详细描述改动内容、测试步骤与预期效果

### 贡献规范

- 遵循项目的代码风格与目录结构
- 为新增功能添加对应的单元测试 / 仪器化测试
- 提交 PR 前确保代码可正常编译、运行
- 提交信息简洁清晰，遵循[Conventional Commits](https://www.conventionalcommits.org/)规范

## Contributors

Thanks to all the developers who contributed to the project!

- [superhhhsss (Caizimu Shen)](https://github.com/superhhhsss)
- [MiaoYuxuan123](https://github.com/MiaoYuxuan123)
- [33skrr](https://github.com/33skrr)

------

<div align="center">

  <p>© 2026 Football-App Team | Built for Football Enthusiasts</p>