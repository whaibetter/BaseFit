# Android应用打包与发布教程

## 目录
1. [环境准备与检查](#1-环境准备与检查)
2. [版本号更新规范](#2-版本号更新规范)
3. [代码提交与审核流程](#3-代码提交与审核流程)
4. [APK构建步骤](#4-apk构建步骤)
5. [签名配置管理](#5-签名配置管理)
6. [发布前测试清单](#6-发布前测试清单)
7. [应用商店上传流程](#7-应用商店上传流程)
8. [发布后监控与回滚策略](#8-发布后监控与回滚策略)
9. [常见问题解决方案](#9-常见问题解决方案)
10. [最佳实践建议](#10-最佳实践建议)

---

## 1. 环境准备与检查

### 1.1 开发环境要求
- **JDK版本**: Java 17 或更高版本
- **Android Studio**: 最新稳定版（推荐 Hedgehog 或更高）
- **Android SDK**: 
  - compileSdk: 34
  - targetSdk: 34
  - minSdk: 24
- **Gradle**: 8.2.0 或兼容版本
- **Kotlin**: 1.9.20

### 1.2 环境检查清单

#### 1.2.1 Java环境检查
```bash
# 检查Java版本
java -version

# 预期输出应包含 Java 17 或更高版本
# 例如: openjdk version "17.0.16" 2024-07-16 LTS
```

#### 1.2.2 Android SDK检查
```bash
# 检查Android SDK路径配置
# Windows: 检查 local.properties 文件
# 内容应包含: sdk.dir=C:\\Users\\用户名\\Android\\Sdk

# 验证SDK工具是否可用
# 检查 build-tools 目录
ls -la ~/Android/Sdk/build-tools/
```

#### 1.2.3 Gradle环境检查
```bash
# 检查Gradle Wrapper是否可用
./gradlew --version

# Windows:
.\gradlew.bat --version
```

#### 1.2.4 项目依赖检查
```bash
# 清理并检查依赖
./gradlew clean

# 验证项目能否正常编译
./gradlew assembleDebug
```

### 1.3 环境配置文件检查

检查以下关键配置文件是否正确：

1. **gradle.properties**
   ```properties
   org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
   android.useAndroidX=true
   android.enableJetifier=true
   org.gradle.java.home=C:/Users/用户名/.jdks/ms-17.0.16
   ```

2. **local.properties** (不要提交到版本控制)
   ```properties
   sdk.dir=C:/Users/用户名/Android/Sdk
   ```

3. **build.gradle.kts** (项目根目录)
4. **app/build.gradle.kts** (应用模块)

---

## 2. 版本号更新规范

### 2.1 版本号命名规则

使用语义化版本控制 (Semantic Versioning):

```
versionName = "MAJOR.MINOR.PATCH"
versionCode = 递增的整数
```

- **MAJOR**: 重大功能更新，不兼容的API修改
- **MINOR**: 新功能添加，向下兼容
- **PATCH**: Bug修复，向下兼容

### 2.2 版本号更新示例

| 版本类型 | versionName | versionCode | 说明 |
|---------|------------|------------|------|
| 初始版本 | 1.0.0 | 1 | 应用首次发布 |
| Bug修复 | 1.0.1 | 2 | 修复小问题 |
| 新功能 | 1.1.0 | 3 | 添加新功能 |
| 重大更新 | 2.0.0 | 4 | 重大架构变更 |

### 2.3 版本号更新步骤

#### 2.3.1 修改版本号

编辑 `app/build.gradle.kts` 文件：

```kotlin
android {
    defaultConfig {
        applicationId = "com.basefit.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 2        // 更新版本号
        versionName = "1.0.1"  // 更新版本名称
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
}
```

#### 2.3.2 创建版本变更日志

在项目根目录创建 `CHANGELOG.md` 文件：

```markdown
# Changelog

## [1.0.1] - 2026-03-18
### Fixed
- 修复HomeScreen进度显示问题
- 优化应用启动性能

## [1.0.0] - 2026-03-17
### Added
- 初始版本发布
- 健身计划管理功能
- 每日打卡功能
- 数据统计图表
```

---

## 3. 代码提交与审核流程

### 3.1 Git工作流程

#### 3.1.1 分支策略

```
main (主分支)
  └── develop (开发分支)
        ├── feature/login (功能分支)
        ├── feature/user-profile (功能分支)
        └── bugfix/crash-on-launch (修复分支)
```

#### 3.1.2 提交代码

```bash
# 1. 检查当前状态
git status

# 2. 添加修改的文件
git add app/build.gradle.kts
git add CHANGELOG.md

# 3. 提交变更（使用规范的提交信息）
git commit -m "chore: 更新版本号到1.0.1

- 更新versionCode为2
- 更新versionName为1.0.1
- 添加变更日志"

# 4. 推送到远程仓库
git push origin develop
```

### 3.2 提交信息规范

使用Conventional Commits规范：

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type类型**:
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建/工具相关

**示例**:
```bash
# 修复bug
git commit -m "fix: 修复HomeScreen进度显示问题"

# 新功能
git commit -m "feat: 添加数据导出功能"

# 版本更新
git commit -m "chore: 更新版本号到1.0.1"
```

### 3.3 代码审核流程

1. **创建Pull Request**
   - 从功能分支向develop分支发起PR
   - 填写详细的PR描述
   - 关联相关的issue

2. **自动化检查**
   - 代码风格检查 (Lint)
   - 单元测试
   - 构建验证

3. **人工审核**
   - 至少1名团队成员审核
   - 检查代码质量
   - 验证功能实现

4. **合并代码**
   - 审核通过后合并到develop分支
   - 删除临时功能分支

---

## 4. APK构建步骤

### 4.1 调试版 (Debug) vs 正式版 (Release)

| 特性 | Debug版本 | Release版本 |
|-----|----------|------------|
| 用途 | 开发测试 | 发布到应用商店 |
| 签名 | 自动使用debug密钥 | 使用正式发布密钥 |
| 调试信息 | 包含调试符号 | 通常移除调试符号 |
| 代码优化 | 不优化 | 启用代码混淆和优化 |
| 日志输出 | 完整日志 | 可配置移除日志 |
| 安装要求 | 可直接安装 | 需签名验证 |

### 4.2 构建调试版APK

#### 4.2.1 使用命令行构建

```bash
# 清理之前的构建
./gradlew clean

# 构建Debug APK
./gradlew assembleDebug

# Windows:
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

#### 4.2.2 输出位置
```
app/build/outputs/apk/debug/app-debug.apk
```

#### 4.2.3 安装到设备
```bash
# 安装到连接的设备
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 或者使用Gradle安装
./gradlew installDebug
```

### 4.3 构建正式版APK

#### 4.3.1 前提条件
- 已配置签名密钥 (见第5章)
- 已更新版本号
- 代码已通过审核

#### 4.3.2 构建步骤

```bash
# 1. 清理项目
./gradlew clean

# 2. 构建Release APK
./gradlew assembleRelease

# Windows:
.\gradlew.bat clean
.\gradlew.bat assembleRelease
```

#### 4.3.3 输出位置
```
app/build/outputs/apk/release/app-release.apk
```

#### 4.3.4 验证APK签名

使用apksigner验证：

```bash
# 使用Android SDK的apksigner工具
# Windows:
C:\Users\用户名\Android\Sdk\build-tools\34.0.0\apksigner.bat verify --verbose app-release.apk

# 预期输出:
# Verifies
# Verified using v1 scheme (JAR signing): false
# Verified using v2 scheme (APK Signature Scheme v2): true
# Verified using v3 scheme (APK Signature Scheme v3): false
# Number of signers: 1
```

### 4.4 构建Android App Bundle (AAB)

对于Google Play，推荐使用AAB格式：

```bash
# 构建AAB
./gradlew bundleRelease

# 输出位置:
# app/build/outputs/bundle/release/app-release.aab
```

---

## 5. 签名配置管理

### 5.1 密钥库创建

#### 5.1.1 使用keytool创建密钥库

```bash
# 生成密钥库命令
keytool -genkey -v \
  -keystore basefit-key.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias basefit \
  -dname "CN=BaseFit, OU=Development, O=BaseFit, L=City, ST=State, C=CN" \
  -keypass your-key-password \
  -storepass your-store-password

# Windows (使用完整Java路径):
"C:\Users\用户名\.jdks\ms-17.0.16\bin\keytool.exe" -genkey -v -keystore basefit-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias basefit -dname "CN=BaseFit, OU=Development, O=BaseFit, L=City, ST=State, C=CN" -keypass basefit123 -storepass basefit123
```

**参数说明**:
- `-keystore`: 密钥库文件名
- `-keyalg`: 加密算法 (RSA)
- `-keysize`: 密钥长度 (2048)
- `-validity`: 有效期 (天)
- `-alias`: 密钥别名
- `-keypass`: 密钥密码
- `-storepass`: 密钥库密码

#### 5.1.2 密钥库信息

创建后的密钥库信息：
- **文件名**: basefit-key.jks
- **密钥别名**: basefit
- **有效期**: 10000天 (~27年)
- **算法**: RSA 2048位

### 5.2 Gradle签名配置

#### 5.2.1 在build.gradle.kts中配置

编辑 `app/build.gradle.kts`:

```kotlin
android {
    namespace = "com.basefit.app"
    compileSdk = 34

    // 签名配置
    signingConfigs {
        create("release") {
            storeFile = file("../basefit-key.jks")
            storePassword = "your-store-password"
            keyAlias = "basefit"
            keyPassword = "your-key-password"
        }
    }

    defaultConfig {
        applicationId = "com.basefit.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 应用签名配置
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
    }
}
```

#### 5.2.2 安全的签名配置 (推荐)

不要将密码硬编码在build.gradle.kts中，使用环境变量或properties文件：

**方法1: 使用local.properties**

在 `local.properties` 中添加：
```properties
storeFile=basefit-key.jks
storePassword=your-store-password
keyAlias=basefit
keyPassword=your-key-password
```

在 `app/build.gradle.kts` 中读取：
```kotlin
import java.util.Properties

android {
    // ...
    
    val keystoreProperties = Properties()
    val keystorePropertiesFile = rootProject.file("local.properties")
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(keystorePropertiesFile.inputStream())
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
    
    // ...
}
```

**方法2: 使用环境变量**

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(System.getenv("KEYSTORE_FILE") ?: "../basefit-key.jks")
        storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
        keyAlias = System.getenv("KEY_ALIAS") ?: "basefit"
        keyPassword = System.getenv("KEY_PASSWORD") ?: ""
    }
}
```

### 5.3 密钥库安全管理

#### 5.3.1 重要安全注意事项

⚠️ **绝对不要**:
- 将密钥库文件提交到Git仓库
- 在代码中硬编码密码
- 使用过于简单的密码
- 共享密钥库给不信任的人

✅ **应该**:
- 将密钥库备份到安全的地方（离线存储）
- 使用强密码保护密钥库
- 限制密钥库访问权限
- 记录密钥库信息并存档

#### 5.3.2 .gitignore配置

确保以下文件不被提交：

```gitignore
# 密钥库文件
*.jks
*.keystore
*.p12
*.pfx

# 本地配置
local.properties

# 构建输出
build/
*.apk
*.aab

# 敏感配置文件
secrets.properties
*.env
```

#### 5.3.3 密钥库备份策略

1. **本地备份**: 保存到加密的USB驱动器
2. **云备份**: 上传到加密的云存储（如Google Drive加密文件夹）
3. **纸质备份**: 记录密钥库密码和重要信息，保存在保险柜
4. **团队共享**: 使用密码管理工具（如1Password、Bitwarden）安全共享

---

## 6. 发布前测试清单

### 6.1 功能测试清单

- [ ] **核心功能测试**
  - [ ] 应用启动正常
  - [ ] 首页数据加载正确
  - [ ] 健身计划创建/编辑/删除功能正常
  - [ ] 打卡功能正常工作
  - [ ] 数据统计显示正确
  - [ ] 设置页面功能正常

- [ ] **用户交互测试**
  - [ ] 按钮点击响应正常
  - [ ] 列表滚动流畅
  - [ ] 表单输入验证正常
  - [ ] 导航跳转正确
  - [ ] 对话框显示正常

- [ ] **数据持久化测试**
  - [ ] 数据保存到数据库正常
  - [ ] 应用重启后数据不丢失
  - [ ] 数据更新正确同步

### 6.2 兼容性测试清单

- [ ] **Android版本兼容性**
  - [ ] Android 7.0 (API 24) - 最低支持版本
  - [ ] Android 8.0 (API 26)
  - [ ] Android 9.0 (API 28)
  - [ ] Android 10 (API 29)
  - [ ] Android 11 (API 30)
  - [ ] Android 12 (API 31)
  - [ ] Android 13 (API 33)
  - [ ] Android 14 (API 34) - 目标版本

- [ ] **屏幕尺寸兼容性**
  - [ ] 小屏手机 (5.0" 以下)
  - [ ] 普通手机 (5.0" - 6.0")
  - [ ] 大屏手机 (6.0" 以上)
  - [ ] 平板设备 (7.0" - 10.0")
  - [ ] 横屏/竖屏切换正常

- [ ] **设备厂商兼容性**
  - [ ] Google Pixel
  - [ ] Samsung Galaxy
  - [ ] Xiaomi
  - [ ] Huawei
  - [ ] OPPO/Vivo

### 6.3 性能测试清单

- [ ] **启动性能**
  - [ ] 冷启动时间 < 2秒
  - [ ] 暖启动时间 < 1秒
  - [ ] 无ANR (Application Not Responding)

- [ ] **运行时性能**
  - [ ] UI流畅度 60fps
  - [ ] 内存占用合理
  - [ ] 无内存泄漏
  - [ ] 电量消耗正常

- [ ] **网络性能**
  - [ ] 弱网环境下正常运行
  - [ ] 断网时提示友好
  - [ ] 网络请求超时处理

### 6.4 安全测试清单

- [ ] **数据安全**
  - [ ] 敏感数据加密存储
  - [ ] 日志中不泄露敏感信息
  - [ ] HTTPS通信正常

- [ ] **权限测试**
  - [ ] 只申请必要的权限
  - [ ] 权限拒绝时应用不崩溃
  - [ ] 权限说明清晰

### 6.5 测试命令示例

```bash
# 运行单元测试
./gradlew test

# 运行Android仪器化测试
./gradlew connectedAndroidTest

# 运行Lint检查
./gradlew lint

# 构建并安装到设备进行测试
./gradlew installDebug
```

---

## 7. 应用商店上传流程

### 7.1 Google Play发布流程

#### 7.1.1 准备工作

1. **注册Google Play开发者账号**
   - 访问: https://play.google.com/console
   - 支付一次性注册费 ($25)
   - 完善开发者资料

2. **准备应用信息**
   - 应用名称
   - 应用描述 (简短描述 + 完整描述)
   - 应用图标 (512x512 PNG)
   - 应用截图 (手机、平板、7寸平板、10寸平板)
   - 功能截图
   - 宣传图 (1024x500)
   - 视频链接 (可选)
   - 应用分类
   - 内容分级
   - 隐私政策链接
   - 联系方式

3. **准备发布文件**
   - APK 或 AAB 文件
   - 版本更新说明
   - 目标国家/地区

#### 7.1.2 创建应用

1. 登录Google Play Console
2. 点击"创建应用"
3. 填写应用详情
   - 默认语言
   - 应用名称
   - 声明类型 (应用或游戏)
   - 是否免费
   - 接受服务条款
4. 点击"创建应用"

#### 7.1.3 填写商店列表

在"商品详情"页面填写：
- **应用详情**
  - 应用名称
  - 简短描述 (最多80字符)
  - 完整描述 (最多4000字符)
  
- **图形资源**
  - 应用图标 (512x512, 32位PNG)
  - 功能截图 (手机: 最多8张, 1080x1920或1920x1080)
  - 7寸平板截图 (最多8张)
  - 10寸平板截图 (最多8张)
  - 宣传图 (1024x500)
  - 宣传视频 (YouTube链接, 可选)

- **分类和联系方式**
  - 应用类型 (应用/游戏)
  - 应用类别
  - 内容分级
  - 电子邮件
  - 网站 (可选)
  - 电话 (可选)

- **隐私政策**
  - 隐私政策URL (必填)

#### 7.1.4 配置应用版本

1. 进入"发布" → "生产"
2. 点击"创建新版本"
3. 上传APK或AAB文件
   - 点击"上传"
   - 选择 `app-release.aab` 或 `app-release.apk`
   - 等待上传完成

4. 填写版本信息
   - 版本名称 (会自动从APK/AAB读取)
   - 版本代码 (会自动从APK/AAB读取)
   - 更新说明 (用目标语言填写)

5. 配置发布范围
   - 选择发布国家/地区
   - 设置分阶段发布比例 (可选)

#### 7.1.5 完成应用配置

在发布前需要完成：

- [ ] **定价和分发范围**
  - 设置应用是免费还是付费
  - 选择分发的国家/地区

- [ ] **内容分级**
  - 完成内容分级问卷
  - 获取IARC分级证书

- [ ] **目标受众和内容**
  - 选择目标年龄段
  - 声明是否包含广告

- [ ] **应用签名** (使用Play App Signing)
  - 选择使用Google Play应用签名
  - 上传应用签名密钥

#### 7.1.6 审核与发布

1. 检查所有必填项是否完成
2. 点击"保存"
3. 点击"审核发布"
4. 确认发布信息无误
5. 点击"开始发布到生产环境"

**审核时间**: 通常需要几小时到几天不等

### 7.2 国内应用商店发布流程

#### 7.2.1 国内主要应用商店

| 应用商店 | 网址 | 注册费用 |
|---------|------|---------|
| 腾讯应用宝 | https://open.tencent.com/ | 免费 |
| 华为应用市场 | https://developer.huawei.com/consumer/cn/ | 免费 |
| 小米应用商店 | https://dev.mi.com/ | 免费 |
| OPPO软件商店 | https://open.oppomobile.com/ | 免费 |
| vivo应用商店 | https://dev.vivo.com.cn/ | 免费 |
| 百度手机助手 | https://app.baidu.com/ | 免费 |
| 360手机助手 | http://dev.360.cn/ | 免费 |

#### 7.2.2 通用准备材料

无论发布到哪个国内应用商店，都需要准备：

**必需材料**:
- [ ] APK文件 (已签名)
- [ ] 应用图标 (512x512 PNG)
- [ ] 应用截图 (3-5张, 1080x1920)
- [ ] 应用名称
- [ ] 应用简介
- [ ] 应用详细描述
- [ ] 应用分类
- [ ] 隐私政策链接
- [ ] 开发者资质证明
  - 企业开发者: 营业执照
  - 个人开发者: 身份证

**可能需要**:
- [ ] 软件著作权证书 (部分商店要求)
- [ ] ICP备案号 (部分商店要求)
- [ ] 应用宣传视频
- [ ] 版权证明文件

#### 7.2.3 腾讯应用宝发布流程

1. **注册开发者账号**
   - 访问 https://open.tencent.com/
   - 使用微信/QQ登录
   - 完成开发者认证 (个人/企业)

2. **创建应用**
   - 点击"管理中心" → "创建应用"
   - 选择"移动应用"
   - 填写应用基本信息

3. **上传应用信息**
   - 上传APK文件
   - 填写应用详情
   - 上传应用图标和截图
   - 设置应用分类和标签

4. **提交审核**
   - 确认信息无误
   - 提交审核
   - 等待审核结果 (通常1-3个工作日)

#### 7.2.4 华为应用市场发布流程

1. **注册华为开发者账号**
   - 访问 https://developer.huawei.com/consumer/cn/
   - 注册华为账号
   - 完成实名认证

2. **创建应用**
   - 进入AppGallery Connect
   - 点击"我的应用" → "新建应用"
   - 选择"安卓 (HMS/APK)"
   - 填写应用信息

3. **配置应用**
   - 设置应用包名
   - 上传APK
   - 填写应用详情
   - 上传应用素材

4. **提交审核**
   - 完成内容分级
   - 设置发布信息
   - 提交审核

#### 7.2.5 小米应用商店发布流程

1. **注册小米开发者账号**
   - 访问 https://dev.mi.com/
   - 注册小米账号
   - 完成开发者认证

2. **创建应用**
   - 点击"管理控制台"
   - 选择"应用商店"
   - 点击"创建新应用"

3. **填写应用信息**
   - 上传APK
   - 填写应用基本信息
   - 上传图标和截图
   - 选择应用分类

4. **提交审核**
   - 确认信息完整
   - 提交审核
   - 等待审核结果

#### 7.2.6 多渠道发布技巧

**使用渠道包管理**:

在 `app/build.gradle.kts` 中配置产品风味：

```kotlin
android {
    flavorDimensions += "store"
    
    productFlavors {
        create("googleplay") {
            dimension = "store"
            applicationIdSuffix = ".googleplay"
            versionNameSuffix = "-googleplay"
            buildConfigField("String", "STORE_NAME", "\"googleplay\"")
        }
        
        create("huawei") {
            dimension = "store"
            applicationIdSuffix = ".huawei"
            versionNameSuffix = "-huawei"
            buildConfigField("String", "STORE_NAME", "\"huawei\"")
        }
        
        create("xiaomi") {
            dimension = "store"
            applicationIdSuffix = ".xiaomi"
            versionNameSuffix = "-xiaomi"
            buildConfigField("String", "STORE_NAME", "\"xiaomi\"")
        }
        
        create("oppo") {
            dimension = "store"
            applicationIdSuffix = ".oppo"
            versionNameSuffix = "-oppo"
            buildConfigField("String", "STORE_NAME", "\"oppo\"")
        }
        
        create("vivo") {
            dimension = "store"
            applicationIdSuffix = ".vivo"
            versionNameSuffix = "-vivo"
            buildConfigField("String", "STORE_NAME", "\"vivo\"")
        }
    }
}
```

**构建所有渠道包**:
```bash
./gradlew assembleRelease

# 输出:
# app/build/outputs/apk/googleplay/release/app-googleplay-release.apk
# app/build/outputs/apk/huawei/release/app-huawei-release.apk
# ...
```

---

## 8. 发布后监控与回滚策略

### 8.1 发布后监控清单

#### 8.1.1 关键指标监控

- [ ] **用户反馈**
  - [ ] 应用商店评分
  - [ ] 用户评论
  - [ ] 社交媒体反馈
  - [ ] 客服反馈

- [ ] **技术指标**
  - [ ] Crash率 (目标: < 0.1%)
  - [ ] ANR率 (目标: < 0.1%)
  - [ ] 启动时间
  - [ ] 错误日志

- [ ] **业务指标**
  - [ ] 下载量
  - [ ] 安装量
  - [ ] 日活跃用户 (DAU)
  - [ ] 留存率
  - [ ] 功能使用情况

#### 8.1.2 监控工具推荐

**崩溃监控**:
- Firebase Crashlytics (推荐)
- Bugly
- Sentry
- Bugsnag

**性能监控**:
- Firebase Performance Monitoring
- New Relic
- Datadog

**用户分析**:
- Firebase Analytics
- Umeng (友盟)
- TalkingData

#### 8.1.3 Firebase Crashlytics集成示例

在 `app/build.gradle.kts` 中添加：

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.1"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
}
```

在 `Application` 类中初始化：

```kotlin
import com.google.firebase.crashlytics.FirebaseCrashlytics

class BaseFitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 启用Crashlytics
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        
        // 设置用户ID (可选)
        FirebaseCrashlytics.getInstance().setUserId("user-123")
        
        // 记录自定义日志
        FirebaseCrashlytics.getInstance().log("App started")
    }
}
```

### 8.2 分阶段发布策略

#### 8.2.1 Google Play分阶段发布

1. **初始阶段** (1-10%)
   - 发布给1-10%的用户
   - 持续1-2天
   - 密切监控崩溃率和用户反馈

2. **扩展阶段** (10-50%)
   - 如果第一阶段表现良好
   - 扩展到50%的用户
   - 持续2-3天

3. **全面发布** (100%)
   - 确认无严重问题
   - 发布给所有用户

#### 8.2.2 分阶段发布配置

在Google Play Console中：
1. 进入"发布" → "生产"
2. 创建新版本或编辑现有版本
3. 在"发布范围"中选择"分阶段发布"
4. 设置初始发布比例
5. 保存并发布

### 8.3 回滚策略

#### 8.3.1 何时需要回滚

**严重问题**:
- [ ] Crash率 > 1%
- [ ] 核心功能无法使用
- [ ] 数据丢失或损坏
- [ ] 安全漏洞
- [ ] 大量用户投诉

**中度问题**:
- [ ] Crash率 0.5-1%
- [ ] 次要功能异常
- [ ] 性能明显下降
- [ ] 部分用户反馈问题

#### 8.3.2 回滚步骤

**Google Play回滚**:

1. **停止当前版本**
   - 进入Google Play Console
   - 进入"发布" → "生产"
   - 找到当前发布的版本
   - 点击"暂停发布"或调整发布比例为0%

2. **准备回滚版本**
   - 降低versionCode (注意: versionCode必须递增!)
   - ⚠️ 重要: Google Play不允许versionCode回退
   - 解决方案: 使用之前的versionCode + 1, 但使用旧代码
   - 例如: 当前版本2.0 (versionCode 10), 回滚到1.9 (versionCode 11)

3. **发布回滚版本**
   - 构建包含旧代码但新版本号的APK/AAB
   - 上传到Google Play
   - 发布给受影响的用户

**国内应用商店回滚**:

大多数国内应用商店支持直接下架旧版本并上传新版本：

1. 下架有问题的版本
2. 上传之前稳定的版本 (使用新的versionCode)
3. 提交审核
4. 审核通过后发布

#### 8.3.3 回滚预防措施

1. **保留所有历史版本**
   - 保存每个发布版本的APK/AAB
   - 保存对应的代码快照 (Git tag)

2. **使用Git标签管理版本**
   ```bash
   # 创建版本标签
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin v1.0.0
   
   # 查看标签
   git tag
   
   # 签出特定版本
   git checkout v1.0.0
   ```

3. **数据库迁移保护**
   - 数据库升级时保留降级路径
   - 使用Room的迁移机制
   - 测试数据库降级场景

4. **功能开关**
   - 使用远程配置控制新功能
   - 出现问题时可远程关闭新功能
   - Firebase Remote Config示例:

   ```kotlin
   val firebaseConfig = Firebase.remoteConfig
   val configSettings = remoteConfigSettings {
       minimumFetchIntervalInSeconds = 3600
   }
   firebaseConfig.setConfigSettingsAsync(configSettings)
   
   // 获取功能开关
   val newFeatureEnabled = firebaseConfig.getBoolean("new_feature_enabled")
   if (newFeatureEnabled) {
       // 启用新功能
   }
   ```

---

## 9. 常见问题解决方案

### 9.1 构建相关问题

#### 问题1: Gradle构建失败

**症状**:
```
* What went wrong:
Execution failed for task ':app:compileDebugKotlin'.
```

**解决方案**:
```bash
# 1. 清理项目
./gradlew clean

# 2. 删除.gradle文件夹
rm -rf .gradle/

# 3. 重新构建
./gradlew build

# Windows:
.\gradlew.bat clean
.\gradlew.bat build
```

#### 问题2: 依赖下载失败

**症状**:
```
Could not resolve com.android.support:appcompat-v7:28.0.0
```

**解决方案**:
1. 检查网络连接
2. 配置国内镜像源

在 `settings.gradle.kts` 中:
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 添加阿里云镜像
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://jitpack.io") }
    }
}
```

#### 问题3: Java版本不兼容

**症状**:
```
Android Gradle plugin requires Java 11 to run. You are currently using Java 8.
```

**解决方案**:

在 `gradle.properties` 中指定Java路径:
```properties
org.gradle.java.home=C:/Users/用户名/.jdks/ms-17.0.16
```

或设置环境变量:
```bash
# Linux/Mac
export JAVA_HOME=/path/to/jdk-17

# Windows PowerShell
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
```

### 9.2 签名相关问题

#### 问题4: 密钥库密码忘记

**症状**:
```
Keystore was tampered with, or password was incorrect
```

**解决方案**:
⚠️ 如果密钥库密码丢失，且没有备份，将无法更新应用！

**预防措施**:
- 定期备份密钥库
- 使用密码管理器保存密码
- 记录密码并安全存储

**如果有备份**:
1. 从备份恢复密钥库
2. 使用备份时的密码

#### 问题5: APK签名验证失败

**症状**:
```
INSTALL_PARSE_FAILED_NO_CERTIFICATES
```

**解决方案**:
1. 检查build.gradle.kts中的签名配置
2. 确认密钥库文件存在
3. 验证密码是否正确
4. 重新构建签名APK:
   ```bash
   ./gradlew clean assembleRelease
   ```

### 9.3 应用商店相关问题

#### 问题6: Google Play审核被拒

**常见原因**:
1. 违反内容政策
2. 隐私政策缺失或不完整
3. 应用功能描述与实际不符
4. 存在安全漏洞
5. 权限使用不当

**解决方案**:
1. 仔细阅读拒绝邮件中的具体原因
2. 修复相关问题
3. 重新提交审核
4. 如有疑问，联系Google Play支持

#### 问题7: 国内应用商店需要软著

**症状**:
部分应用商店要求提供软件著作权证书

**解决方案**:
1. 申请软件著作权登记
   - 访问: http://www.ccopyright.com.cn/
   - 准备材料: 源代码、文档、申请表
   - 办理时间: 通常1-3个月

2. 使用第三方代理机构
   - 可以找专业代理机构加快办理
   - 费用通常在几百到几千元不等

### 9.4 设备兼容性问题

#### 问题8: 应用在某些设备上崩溃

**解决方案**:
1. 使用Firebase Crashlytics收集崩溃信息
2. 检查设备特定问题
3. 使用Android Studio的Device Manager测试不同设备
4. 添加设备特定的修复代码

#### 问题9: 应用安装失败

**症状**:
```
INSTALL_FAILED_UPDATE_INCOMPATIBLE
```

**解决方案**:
```bash
# 卸载旧版本
adb uninstall com.basefit.app

# 安装新版本
adb install app-release.apk
```

---

## 10. 最佳实践建议

### 10.1 开发流程最佳实践

#### 10.1.1 版本管理
- ✅ 使用语义化版本号 (Semantic Versioning)
- ✅ 每个发布版本创建Git标签
- ✅ 维护CHANGELOG.md记录变更
- ✅ 定期发布小版本，避免一次性大变更

#### 10.1.2 代码质量
- ✅ 运行Lint检查并修复警告
- ✅ 编写单元测试和仪器化测试
- ✅ 代码审查流程
- ✅ 使用静态代码分析工具

#### 10.1.3 构建自动化
- ✅ 使用CI/CD自动化构建和测试
- ✅ 自动化签名过程
- ✅ 自动化发布流程

### 10.2 性能优化最佳实践

#### 10.2.1 APK大小优化
- ✅ 使用Android App Bundle (AAB)格式
- ✅ 启用代码混淆 (ProGuard/R8)
- ✅ 压缩资源文件
- ✅ 移除未使用的资源
- ✅ 使用WebP格式图片

**启用代码混淆示例**:

```kotlin
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

#### 10.2.2 启动速度优化
- ✅ 延迟初始化非必要组件
- ✅ 使用App Startup库
- ✅ 避免在Application.onCreate中做耗时操作
- ✅ 优化布局层次

### 10.3 安全最佳实践

#### 10.3.1 密钥管理
- ✅ 密钥库文件不提交到Git
- ✅ 使用环境变量或加密文件存储密码
- ✅ 定期备份密钥库
- ✅ 限制密钥库访问权限

#### 10.3.2 应用安全
- ✅ 使用HTTPS进行网络通信
- ✅ 加密存储敏感数据
- ✅ 移除日志中的敏感信息
- ✅ 定期更新依赖库修复安全漏洞

### 10.4 发布策略最佳实践

#### 10.4.1 分阶段发布
- ✅ 先发布给小部分用户 (1-5%)
- ✅ 监控24-48小时
- ✅ 确认无问题后逐步扩大范围
- ✅ 准备回滚计划

#### 10.4.2 用户沟通
- ✅ 在更新说明中清晰描述变更
- ✅ 重大变更提前通知用户
- ✅ 提供反馈渠道
- ✅ 及时响应用户问题

#### 10.4.3 持续改进
- ✅ 收集用户反馈
- ✅ 分析应用性能数据
- ✅ 定期迭代优化
- ✅ A/B测试新功能

### 10.5 团队协作最佳实践

#### 10.5.1 文档维护
- ✅ 维护更新发布文档
- ✅ 记录常见问题和解决方案
- ✅ 团队共享发布经验

#### 10.5.2 知识共享
- ✅ 定期进行发布流程培训
- ✅ 建立发布检查清单
- ✅ 多人审核发布

---

## 附录

### A. 快速参考命令

```bash
# 清理项目
./gradlew clean

# 构建Debug APK
./gradlew assembleDebug

# 构建Release APK
./gradlew assembleRelease

# 构建AAB
./gradlew bundleRelease

# 安装到设备
./gradlew installDebug

# 运行测试
./gradlew test
./gradlew connectedAndroidTest

# 运行Lint检查
./gradlew lint

# 查看任务列表
./gradlew tasks
```

### B. 发布检查清单模板

```markdown
# [版本号] 发布检查清单

## 发布前检查
- [ ] 代码已合并到main分支
- [ ] 版本号已更新
- [ ] CHANGELOG已更新
- [ ] 所有测试通过
- [ ] Lint检查无警告
- [ ] 代码审查已完成

## 构建检查
- [ ] APK/AAB构建成功
- [ ] 签名验证通过
- [ ] APK大小合理
- [ ] 在测试设备上安装成功

## 测试检查
- [ ] 核心功能测试通过
- [ ] 兼容性测试通过
- [ ] 性能测试通过
- [ ] 安全检查通过

## 应用商店准备
- [ ] 应用截图准备完成
- [ ] 更新说明已撰写
- [ ] 隐私政策链接有效
- [ ] 所有材料已审核

## 发布后监控
- [ ] 设置分阶段发布
- [ ] 配置崩溃监控
- [ ] 准备回滚方案
- [ ] 安排人员监控

## 发布执行人: __________
## 发布日期: __________
```

### C. 相关资源链接

- **Android开发者文档**: https://developer.android.com/
- **Google Play Console**: https://play.google.com/console/
- **Firebase文档**: https://firebase.google.com/docs
- **语义化版本控制**: https://semver.org/
- **Conventional Commits**: https://www.conventionalcommits.org/

---

**版本**: 1.0  
**最后更新**: 2026-03-18  
**维护者**: BaseFit开发团队
