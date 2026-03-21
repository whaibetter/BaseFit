# BaseFit - Fitness Tracking App

<div align="center">

![BaseFit Logo](app_icon.jpg)

**A modern fitness tracking app built with Jetpack Compose**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-blue.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2024+-green.svg)](https://developer.android.com)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202023.10.01-blue.svg)](https://developer.android.com/jetpack/compose)

[English](#english) | [中文](#中文)

</div>

---

## English

### 📱 About BaseFit

BaseFit is a modern, elegant fitness tracking application designed to help you stay consistent with your workout routines. Built with the latest Android technologies including Jetpack Compose, Room Database, and Material Design 3, BaseFit provides a smooth and intuitive user experience for managing your fitness goals.

### ✨ Features

- **Workout Plan Management** - Create and manage weekly workout plans
- **Exercise Tracking** - Record your daily workouts with sets and reps
- **Progress Visualization** - Beautiful charts and statistics to track your progress
- **Quick Check-in** - One-tap check-in for completed exercises
- **Data Persistence** - All data saved locally with Room Database
- **Dark Mode Support** - Beautiful dark theme for comfortable night use
- **Notification Reminders** - Never miss a workout with timely reminders

### 🛠️ Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 1.9.20 |
| UI Framework | Jetpack Compose |
| Design System | Material Design 3 |
| Architecture | MVVM + Repository Pattern |
| Database | Room 2.6.1 |
| Navigation | Navigation Compose 2.7.6 |
| Charts | Vico Charts 1.2.0 |
| Build Tool | Gradle 8.2.0 |
| Min SDK | API 24 (Android 7.0) |
| Target SDK | API 34 (Android 14) |

### 📦 Installation

#### Option 1: Download Pre-built APK (Recommended)

You can download the latest pre-built APK directly from the releases section:

[![Download APK](https://img.shields.io/badge/Download-APK-green?style=for-the-badge)](https://github.com/whaibetter/BaseFit/releases/latest)

**Installation Steps:**
1. Download the latest `app-release.apk` from the [Releases](https://github.com/whaibetter/BaseFit/releases) page
2. On your Android device, enable "Unknown Sources" in Settings > Security
3. Open the APK file and follow the installation prompts
4. Once installed, open BaseFit and start tracking your fitness!

#### Option 2: Build from Source

**Prerequisites:**
- Android Studio Hedgehog or later
- JDK 17 or later
- Android SDK (API 34)

**Build Steps:**

```bash
# 1. Clone the repository
git clone https://github.com/whaibetter/BaseFit.git
cd BaseFit

# 2. Open in Android Studio
# or build from command line

# 3. Build Debug APK
./gradlew assembleDebug

# 4. Build Release APK (requires signing configuration)
./gradlew assembleRelease

# 5. Install to connected device
./gradlew installDebug
```

**Windows Users:**
```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

### 🚀 Usage Guide

#### Getting Started

1. **Launch the App** - Open BaseFit on your Android device
2. **Create Your First Plan** - Navigate to the Plan tab and add exercises
3. **Start Tracking** - Go to Home to see your daily workout plan
4. **Check In** - Tap the check button when you complete an exercise
5. **View Stats** - Check the Stats tab to see your progress over time

#### Key Screens

- **Home** - View today's workout plan and quick check-in
- **Plan** - Manage your weekly exercise routines
- **Record** - View detailed workout history
- **Stats** - Visualize your progress with charts
- **Settings** - Customize app preferences

### ⚙️ Configuration

#### Signing Configuration (for Release Builds)

To build a signed release APK, configure the signing settings in `app/build.gradle.kts`:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../basefit-key.jks")
        storePassword = "your-password"
        keyAlias = "basefit"
        keyPassword = "your-password"
    }
}
```

**Important:** Never commit your keystore file or passwords to version control!

#### Building Variants

The project supports multiple build variants:

```bash
# Debug build (for development)
./gradlew assembleDebug

# Release build (signed, for distribution)
./gradlew assembleRelease

# Build all variants
./gradlew build
```

### 🔧 Troubleshooting

#### Common Issues

**Problem: Gradle build fails**
```bash
# Solution: Clean and rebuild
./gradlew clean
./gradlew build
```

**Problem: Java version mismatch**
- Ensure you're using Java 17 or later
- Set `org.gradle.java.home` in `gradle.properties`

**Problem: APK installation fails**
```bash
# Uninstall existing version first
adb uninstall com.basefit.app
# Then install new version
adb install app-release.apk
```

**Problem: Dependencies won't download**
- Check your internet connection
- Try using a VPN if behind a firewall
- Configure alternative Maven repositories

#### Getting Help

If you encounter any issues:
1. Check the [Issues](https://github.com/whaibetter/BaseFit/issues) page
2. Search for existing solutions
3. Create a new issue with detailed information

### 🤝 Contributing

We welcome contributions to BaseFit! Here's how you can help:

#### Development Setup

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes
4. Run tests: `./gradlew test`
5. Commit your changes: `git commit -m 'feat: Add some amazing feature'`
6. Push to the branch: `git push origin feature/amazing-feature`
7. Open a Pull Request

#### Code Style Guidelines

- Follow Kotlin coding conventions
- Use Jetpack Compose best practices
- Write meaningful commit messages using Conventional Commits
- Add comments for complex logic
- Ensure all tests pass before submitting

#### Pull Request Process

1. Update the README.md with details of changes if applicable
2. Update documentation for new features
3. Ensure your code follows the project's style guidelines
4. Link any relevant issues in your PR description
5. Request a review from the maintainers

### 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

### 🙏 Acknowledgments

- [Jetpack Compose](https://developer.android.com/jetpack/compose) for the modern UI framework
- [Vico](https://github.com/patrykandpatrick/vico) for the beautiful charts
- [Material Design 3](https://m3.material.io/) for the design system
- All contributors who help improve this project

---

## 中文

### 📱 关于 BaseFit

BaseFit 是一款现代化、优雅的健身打卡应用，旨在帮助你保持锻炼计划的连贯性。采用最新的 Android 技术构建，包括 Jetpack Compose、Room 数据库和 Material Design 3，BaseFit 为管理你的健身目标提供流畅直观的用户体验。

### ✨ 功能特性

- **健身计划管理** - 创建和管理每周健身计划
- **运动打卡记录** - 记录你的日常锻炼，包括组数和次数
- **进度可视化** - 精美的图表和统计数据，追踪你的进度
- **快速打卡** - 一键打卡已完成的运动
- **数据持久化** - 所有数据通过 Room 数据库本地保存
- **深色模式支持** - 精美的深色主题，夜间使用更舒适
- **通知提醒** - 及时提醒，不错过任何一次锻炼

### 🛠️ 技术栈

| 组件 | 技术 |
|-----|-----|
| 编程语言 | Kotlin 1.9.20 |
| UI 框架 | Jetpack Compose |
| 设计系统 | Material Design 3 |
| 架构模式 | MVVM + 仓库模式 |
| 数据库 | Room 2.6.1 |
| 导航 | Navigation Compose 2.7.6 |
| 图表库 | Vico Charts 1.2.0 |
| 构建工具 | Gradle 8.2.0 |
| 最低 SDK | API 24 (Android 7.0) |
| 目标 SDK | API 34 (Android 14) |

### 📦 安装

#### 选项 1：下载预构建 APK（推荐）

你可以直接从发布部分下载最新的预构建 APK：

[![下载 APK](https://img.shields.io/badge/下载-APK-green?style=for-the-badge)](https://github.com/whaibetter/BaseFit/releases/latest)

**安装步骤：**
1. 从 [Releases](https://github.com/whaibetter/BaseFit/releases) 页面下载最新的 `app-release.apk`
2. 在你的 Android 设备上，在设置 > 安全中启用"未知来源"
3. 打开 APK 文件并按照安装提示操作
4. 安装完成后，打开 BaseFit 开始记录你的健身！

#### 选项 2：从源代码构建

**前置要求：**
- Android Studio Hedgehog 或更高版本
- JDK 17 或更高版本
- Android SDK（API 34）

**构建步骤：**

```bash
# 1. 克隆仓库
git clone https://github.com/whaibetter/BaseFit.git
cd BaseFit

# 2. 在 Android Studio 中打开
# 或从命令行构建

# 3. 构建 Debug APK
./gradlew assembleDebug

# 4. 构建 Release APK（需要签名配置）
./gradlew assembleRelease

# 5. 安装到连接的设备
./gradlew installDebug
```

**Windows 用户：**
```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

### 🚀 使用指南

#### 入门指南

1. **启动应用** - 在你的 Android 设备上打开 BaseFit
2. **创建你的第一个计划** - 导航到计划标签页并添加运动
3. **开始记录** - 前往首页查看你的每日锻炼计划
4. **完成打卡** - 当你完成一项运动时，点击打卡按钮
5. **查看统计** - 查看统计标签页，了解你随时间的进度

#### 主要页面

- **首页** - 查看今日锻炼计划和快速打卡
- **计划** - 管理你的每周运动安排
- **记录** - 查看详细的锻炼历史
- **统计** - 通过图表可视化你的进度
- **设置** - 自定义应用偏好

### ⚙️ 配置

#### 签名配置（用于 Release 构建）

要构建签名的 Release APK，请在 `app/build.gradle.kts` 中配置签名设置：

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../basefit-key.jks")
        storePassword = "your-password"
        keyAlias = "basefit"
        keyPassword = "your-password"
    }
}
```

**重要提示：** 永远不要将密钥库文件或密码提交到版本控制！

#### 构建变体

项目支持多种构建变体：

```bash
# Debug 构建（用于开发）
./gradlew assembleDebug

# Release 构建（已签名，用于分发）
./gradlew assembleRelease

# 构建所有变体
./gradlew build
```

### 🔧 故障排除

#### 常见问题

**问题：Gradle 构建失败**
```bash
# 解决方案：清理并重新构建
./gradlew clean
./gradlew build
```

**问题：Java 版本不匹配**
- 确保你使用的是 Java 17 或更高版本
- 在 `gradle.properties` 中设置 `org.gradle.java.home`

**问题：APK 安装失败**
```bash
# 先卸载现有版本
adb uninstall com.basefit.app
# 然后安装新版本
adb install app-release.apk
```

**问题：依赖无法下载**
- 检查你的网络连接
- 如果在防火墙后面，尝试使用 VPN
- 配置备用的 Maven 仓库

#### 获取帮助

如果你遇到任何问题：
1. 检查 [Issues](https://github.com/whaibetter/BaseFit/issues) 页面
2. 搜索现有的解决方案
3. 创建新的 issue 并提供详细信息

### 🤝 贡献

我们欢迎对 BaseFit 的贡献！以下是你可以帮助的方式：

#### 开发设置

1. Fork 这个仓库
2. 创建功能分支：`git checkout -b feature/amazing-feature`
3. 进行你的更改
4. 运行测试：`./gradlew test`
5. 提交你的更改：`git commit -m 'feat: 添加一些很棒的功能'`
6. 推送到分支：`git push origin feature/amazing-feature`
7. 打开一个 Pull Request

#### 代码风格指南

- 遵循 Kotlin 编码约定
- 使用 Jetpack Compose 最佳实践
- 使用 Conventional Commits 编写有意义的提交信息
- 为复杂逻辑添加注释
- 在提交前确保所有测试通过

#### Pull Request 流程

1. 如果适用，更新 README.md 中的更改详情
2. 更新新功能的文档
3. 确保你的代码遵循项目的风格指南
4. 在 PR 描述中链接任何相关的 issue
5. 请求维护者进行审查

### 📄 许可证

本项目采用 MIT 许可证 - 有关详细信息，请参阅 [LICENSE](LICENSE) 文件。

### 🙏 致谢

- [Jetpack Compose](https://developer.android.com/jetpack/compose) 提供现代化 UI 框架
- [Vico](https://github.com/patrykandpatrick/vico) 提供精美的图表
- [Material Design 3](https://m3.material.io/) 提供设计系统
- 所有帮助改进这个项目的贡献者

---

<div align="center">

**Made with ❤️ by Luo Wenhai**

[GitHub](https://github.com/whaibetter) | [Report Bug](https://github.com/whaibetter/BaseFit/issues) | [Request Feature](https://github.com/whaibetter/BaseFit/issues)

</div>
