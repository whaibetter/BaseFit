# ============================================
# BaseFit APK Interactive Packaging Script
# ============================================

param(
    [switch]$Help
)

$Script:Version = "1.0.0"
$Script:ProjectRoot = $PSScriptRoot
$Script:GradleWrapper = Join-Path $ProjectRoot "gradlew.bat"
$Script:DefaultOutputDir = Join-Path $ProjectRoot "output"
$Script:VersionFile = Join-Path $ProjectRoot ".apk_version"
$Script:BuildType = "release"

function Show-Header {
    Clear-Host
    Write-Host "=============================================" -ForegroundColor Cyan
    Write-Host "    BaseFit APK Interactive Packaging Tool" -ForegroundColor Cyan
    Write-Host "              Version $Script:Version" -ForegroundColor Cyan
    Write-Host "=============================================" -ForegroundColor Cyan
    Write-Host
}

function Show-Step {
    param([int]$Number, [string]$Description)
    Write-Host
    Write-Host "[STEP $Number/6] $Description" -ForegroundColor Yellow
    Write-Host "-----------------------------------------" -ForegroundColor Cyan
}

function Get-ValidInput {
    param(
        [string]$Prompt,
        [string]$DefaultValue,
        [scriptblock]$Validator,
        [string]$ErrorMessage = "Invalid input, please try again."
    )

    while ($true) {
        $input = Read-Host $Prompt
        if ([string]::IsNullOrWhiteSpace($input)) {
            return $DefaultValue
        }

        if ($Validator) {
            $isValid = & $Validator $input
            if ($isValid) {
                return $input
            } else {
                Write-Host $ErrorMessage -ForegroundColor Red
            }
        } else {
            return $input
        }
    }
}

function Get-Version {
    Write-Host
    $input = Read-Host "请输入版本号 (例如: 1.0.0) [留空自动自增]"

    if ([string]::IsNullOrWhiteSpace($input)) {
        if (Test-Path $VersionFile) {
            $lastVersion = Get-Content $VersionFile -Raw.Trim()
            $parts = $lastVersion -split '\.'
            if ($parts.Count -eq 3) {
                $major = [int]$parts[0]
                $minor = [int]$parts[1]
                $patch = [int]$parts[2] + 1
                $Script:VersionName = "$major.$minor.$patch"
                Write-Host "[自动] 使用自增版本号: $Script:VersionName" -ForegroundColor Green
                return
            }
        }
        $Script:VersionName = "1.0.0"
        Write-Host "[自动] 使用自增版本号: $Script:VersionName" -ForegroundColor Green
    } else {
        $Script:VersionName = $input
        Write-Host "[确认] 使用指定版本号: $Script:VersionName" -ForegroundColor Green
    }
}

function Get-DateOption {
    Write-Host
    $input = Read-Host "是否在文件名中包含日期? (Y/N) [默认: N]"

    if ([string]::IsNullOrWhiteSpace($input) -or $input -eq "N" -or $input -eq "n") {
        $Script:IncludeDate = $false
        Write-Host "[默认] 文件名不包含日期" -ForegroundColor Green
    } elseif ($input -eq "Y" -or $input -eq "y") {
        $Script:IncludeDate = $true
        $Script:BuildDate = Get-Date -Format "yyyyMMdd"
        Write-Host "[确认] 文件名将包含日期: $Script:BuildDate" -ForegroundColor Green
    } else {
        Write-Host "[错误] 无效选择，请输入 Y 或 N" -ForegroundColor Red
        Get-DateOption
    }
}

function Get-OutputDirectory {
    Write-Host
    $input = Read-Host "请输入APK输出目录 [默认: $DefaultOutputDir]"

    if ([string]::IsNullOrWhiteSpace($input)) {
        $Script:OutputDir = $DefaultOutputDir
    } else {
        $Script:OutputDir = $input
    }

    if (-not (Test-Path $Script:OutputDir)) {
        Write-Host "[警告] 目录不存在，正在创建: $Script:OutputDir" -ForegroundColor Yellow
        try {
            New-Item -ItemType Directory -Path $Script:OutputDir -Force | Out-Null
        } catch {
            Write-Host "[错误] 无法创建输出目录: $Script:OutputDir" -ForegroundColor Red
            Get-OutputDirectory
            return
        }
    }
    Write-Host "[确认] APK将输出到: $Script:OutputDir" -ForegroundColor Green
}

function Get-BuildType {
    Write-Host
    Write-Host "请选择构建类型:"
    Write-Host "  1) Release (发布版本)"
    Write-Host "  2) Debug (调试版本)"

    $input = Read-Host "请选择 [默认: 1]"

    if ([string]::IsNullOrWhiteSpace($input) -or $input -eq "1") {
        $Script:BuildType = "release"
    } elseif ($input -eq "2") {
        $Script:BuildType = "debug"
    } else {
        Write-Host "[错误] 无效选择，请输入 1 或 2" -ForegroundColor Red
        Get-BuildType
        return
    }
    Write-Host "[确认] 构建类型: $Script:BuildType" -ForegroundColor Green
}

function Show-Summary {
    Write-Host
    Write-Host "=============================================" -ForegroundColor Yellow
    Write-Host "           配置信息确认" -ForegroundColor Yellow
    Write-Host "=============================================" -ForegroundColor Yellow
    Write-Host
    Write-Host "  项目路径:    $ProjectRoot"
    Write-Host "  构建类型:    $Script:BuildType"
    Write-Host "  版本号:      $Script:VersionName"
    Write-Host "  包含日期:    $(if($Script:IncludeDate){$Script:BuildDate}else{'无'})"
    Write-Host "  输出目录:    $Script:OutputDir"
    Write-Host
    Write-Host "=============================================" -ForegroundColor Yellow
}

function Confirm-Build {
    $input = Read-Host "确认开始打包? (Y/N) [默认: N]"

    if ([string]::IsNullOrWhiteSpace($input) -or $input -eq "N" -or $input -eq "n") {
        Write-Host "已取消打包操作" -ForegroundColor Yellow
        exit 0
    } elseif ($input -ne "Y" -and $input -ne "y") {
        Write-Host "[错误] 请输入 Y 或 N" -ForegroundColor Red
        Confirm-Build
    }
}

function Test-Environment {
    if (-not (Test-Path $GradleWrapper)) {
        Write-Host "[错误] Gradle Wrapper 未找到: $GradleWrapper" -ForegroundColor Red
        return $false
    }
    Write-Host "[OK] Gradle Wrapper 存在" -ForegroundColor Green

    $buildFile = Join-Path $ProjectRoot "build.gradle.kts"
    if (-not (Test-Path $buildFile)) {
        Write-Host "[错误] build.gradle.kts 未找到" -ForegroundColor Red
        return $false
    }
    Write-Host "[OK] build.gradle.kts 存在" -ForegroundColor Green

    return $true
}

function Invoke-Build {
    Write-Host "正在执行: gradlew.bat clean assemble$BuildType" -ForegroundColor Cyan

    Push-Location $ProjectRoot
    try {
        $result = & .\gradlew.bat clean assemble$BuildType --stacktrace 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Host "[错误] Gradle 构建失败!" -ForegroundColor Red
            Write-Host $result -ForegroundColor Red
            return $false
        }
        Write-Host "[OK] Gradle 构建成功" -ForegroundColor Green
        return $true
    } finally {
        Pop-Location
    }
}

function Find-Apk {
    $apkPath = Join-Path $ProjectRoot "app\build\outputs\apk\$BuildType\app-$BuildType.apk"

    if (Test-Path $apkPath) {
        $Script:ApkSource = $apkPath
        Write-Host "[OK] 找到APK: $Script:ApkSource" -ForegroundColor Green
        return $true
    }

    Write-Host "[警告] 未找到标准路径的APK，正在搜索..." -ForegroundColor Yellow
    $apks = Get-ChildItem -Path (Join-Path $ProjectRoot "app\build\outputs\apk") -Recurse -Filter "*.apk" -ErrorAction SilentlyContinue
    if ($apks) {
        $Script:ApkSource = $apks[0].FullName
        Write-Host "[OK] 找到APK: $Script:ApkSource" -ForegroundColor Green
        return $true
    }

    Write-Host "[错误] 无法找到生成的APK文件" -ForegroundColor Red
    return $false
}

function New-OutputFilename {
    $filename = "basefit-$Script:VersionName"
    if ($Script:IncludeDate) {
        $filename = "$filename-$Script:BuildDate"
    }
    $Script:OutputApk = "$filename.apk"
}

function Copy-ApkToOutput {
    $finalApk = Join-Path $Script:OutputDir $Script:OutputApk

    try {
        Copy-Item -Path $Script:ApkSource -Destination $finalApk -Force
        Write-Host "[OK] APK已复制到: $finalApk" -ForegroundColor Green
    } catch {
        Write-Host "[错误] 复制APK失败: $_" -ForegroundColor Red
        return $false
    }

    $Script:VersionName | Set-Content $VersionFile -NoNewline

    $fileInfo = Get-Item $finalApk
    $Script:ApkSize = $fileInfo.Length

    return $true
}

function Show-Success {
    Write-Host
    Write-Host "=============================================" -ForegroundColor Green
    Write-Host "       APK 打包完成!" -ForegroundColor Green
    Write-Host "=============================================" -ForegroundColor Green
    Write-Host
    Write-Host "  文件名:   $Script:OutputApk"
    Write-Host "  路径:     $(Join-Path $Script:OutputDir $Script:OutputApk)"
    Write-Host "  版本:     $Script:VersionName"
    Write-Host "  大小:     $Script:ApkSize bytes"
    Write-Host
    Write-Host "提示: APK版本号 $Script:VersionName 已保存，下次打包时可自动自增" -ForegroundColor Cyan
    Write-Host
    Read-Host "按 Enter 键退出"
}

function Main {
    Show-Header

    Show-Step -Number 1 -Description "收集构建参数"
    Get-Version
    Get-DateOption
    Get-OutputDirectory
    Get-BuildType

    Show-Summary
    Confirm-Build

    if (-not (Test-Environment)) {
        Read-Host "按 Enter 键退出"
        exit 1
    }

    if (-not (Invoke-Build)) {
        Read-Host "按 Enter 键退出"
        exit 1
    }

    if (-not (Find-Apk)) {
        Read-Host "按 Enter 键退出"
        exit 1
    }

    New-OutputFilename

    if (-not (Copy-ApkToOutput)) {
        Read-Host "按 Enter 键退出"
        exit 1
    }

    Show-Success
}

if ($Help) {
    Write-Host @"
使用方法:
  .\Build-Apk.ps1

功能说明:
  1. 交互式收集构建参数（版本号、日期、输出目录、构建类型）
  2. 自动验证构建环境
  3. 执行 Gradle 构建
  4. 查找并重命名 APK 文件
  5. 复制到指定输出目录

默认值:
  - 版本号: 自动自增 (首次为 1.0.0)
  - 日期: 不包含
  - 输出目录: 项目根目录下的 output 文件夹
  - 构建类型: release

输出文件名格式:
  basefit-{版本号}[-{日期}].apk
  例如: basefit-1.0.0.apk 或 basefit-1.0.0-20260323.apk
"@
    exit 0
}

Main
