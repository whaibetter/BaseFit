# BaseFit Android APK 自动化打包脚本
# PowerShell Script for Automated Android APK Build and Release

param(
    [Parameter(Mandatory = $false)]
    [string]$Version,
    
    [Parameter(Mandatory = $false)]
    [string]$Output
)

function Write-Header {
    param([string]$Message)
    Write-Host ""
    Write-Host "╔═══════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
    Write-Host "║  $Message" -ForegroundColor Cyan
    Write-Host "╚═══════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
    Write-Host ""
}

function Write-Success {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor Green
}

function Write-Error {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "  $Message" -ForegroundColor Yellow
}

function Test-SemanticVersion {
    param([string]$Version)
    $pattern = '^\d+\.\d+\.\d+$'
    return $Version -match $pattern
}

function Get-CurrentVersion {
    $buildGradlePath = Join-Path $PSScriptRoot "app\build.gradle.kts"
    if (-not (Test-Path $buildGradlePath)) {
        Write-Error "找不到 build.gradle.kts 文件"
        return "1.0.0"
    }
    
    $content = Get-Content $buildGradlePath -Raw
    if ($content -match 'versionName\s*=\s*"([^"]+)"') {
        return $matches[1]
    }
    return "1.0.0"
}

function Increment-Version {
    param([string]$CurrentVersion)
    
    if (-not (Test-SemanticVersion $CurrentVersion)) {
        return "1.0.0"
    }
    
    $parts = $CurrentVersion -split '\.'
    $major = [int]$parts[0]
    $minor = [int]$parts[1]
    $patch = [int]$parts[2]
    
    $patch++
    
    return "$major.$minor.$patch"
}

function Update-BuildGradleVersion {
    param([string]$NewVersion)
    
    $buildGradlePath = Join-Path $PSScriptRoot "app\build.gradle.kts"
    if (-not (Test-Path $buildGradlePath)) {
        Write-Error "找不到 build.gradle.kts 文件"
        return $false
    }
    
    $content = Get-Content $buildGradlePath -Raw
    
    # 更新 versionName
    $content = $content -replace 'versionName\s*=\s*"[^"]*"', "versionName = `"$NewVersion`""
    
    # 更新 versionCode (递增)
    if ($content -match 'versionCode\s*=\s*(\d+)') {
        $oldCode = [int]$matches[1]
        $newCode = $oldCode + 1
        $content = $content -replace 'versionCode\s*=\s*\d+', "versionCode = $newCode"
    }
    
    Set-Content -Path $buildGradlePath -Value $content -NoNewline
    Write-Success "已更新 build.gradle.kts: versionName=$NewVersion"
    return $true
}

function Invoke-AndroidBuild {
    Write-Header "开始构建 Android APK"
    
    $gradlewPath = Join-Path $PSScriptRoot "gradlew.bat"
    if (-not (Test-Path $gradlewPath)) {
        Write-Error "找不到 gradlew.bat 文件"
        return $false
    }
    
    Write-Info "执行 Gradle 构建..."
    Push-Location $PSScriptRoot
    
    try {
        & .\gradlew.bat clean assembleRelease
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Gradle 构建失败"
            Pop-Location
            return $false
        }
        Write-Success "Gradle 构建成功"
        Pop-Location
        return $true
    }
    catch {
        Write-Error "构建过程出错: $_"
        Pop-Location
        return $false
    }
}

function Copy-BuildOutput {
    param(
        [string]$OutputPath,
        [string]$Version
    )
    
    Write-Header "复制构建产物"
    
    $apkSourcePath = Join-Path $PSScriptRoot "app\build\outputs\apk\release\app-release.apk"
    
    if (-not (Test-Path $apkSourcePath)) {
        Write-Error "找不到构建的 APK 文件: $apkSourcePath"
        return $false
    }
    
    # 创建输出目录
    if (-not (Test-Path $OutputPath)) {
        Write-Info "创建输出目录: $OutputPath"
        New-Item -ItemType Directory -Path $OutputPath -Force | Out-Null
    }
    
    # 复制 APK
    $apkDestName = "BaseFit-v$Version.apk"
    $apkDestPath = Join-Path $OutputPath $apkDestName
    
    Write-Info "复制 APK: $apkDestName"
    Copy-Item -Path $apkSourcePath -Destination $apkDestPath -Force
    
    if (-not (Test-Path $apkDestPath)) {
        Write-Error "APK 复制失败"
        return $false
    }
    
    Write-Success "APK 已复制到: $apkDestPath"
    return $true
}

function New-VersionInfoFile {
    param(
        [string]$OutputPath,
        [string]$Version
    )
    
    Write-Header "生成版本信息文件"
    
    $versionInfoPath = Join-Path $OutputPath "version.txt"
    
    $buildTime = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $gitCommit = ""
    try {
        $gitCommit = git rev-parse --short HEAD 2>$null
    }
    catch {
        $gitCommit = "unknown"
    }
    
    $content = @"
# BaseFit 版本信息
====================

版本号: $Version
构建时间: $buildTime
Git Commit: $gitCommit

构建环境:
- 操作系统: $([System.Environment]::OSVersion.VersionString)
- PowerShell: $($PSVersionTable.PSVersion)

APK 文件: BaseFit-v$Version.apk
"@
    
    Set-Content -Path $versionInfoPath -Value $content -Encoding UTF8
    Write-Success "版本信息文件已生成: $versionInfoPath"
    
    return $versionInfoPath
}

function Invoke-InteractiveMode {
    Write-Header "BaseFit Android APK 打包工具 - 交互模式"
    
    # 获取当前版本
    $currentVersion = Get-CurrentVersion
    $defaultVersion = Increment-Version $currentVersion
    
    # 版本号输入
    do {
        Write-Host ""
        $inputVersion = Read-Host "请输入版本号 (当前: $currentVersion, 默认: $defaultVersion)"
        
        if ([string]::IsNullOrWhiteSpace($inputVersion)) {
            $Version = $defaultVersion
            Write-Info "使用默认版本号: $Version"
            break
        }
        
        if (Test-SemanticVersion $inputVersion) {
            $Version = $inputVersion
            Write-Info "使用自定义版本号: $Version"
            break
        }
        
        Write-Error "版本号格式无效，请使用 x.y.z 格式 (如 1.0.0)"
    } while ($true)
    
    # 输出路径输入
    $defaultOutput = Join-Path $PSScriptRoot "dist\$Version"
    
    do {
        Write-Host ""
        $inputOutput = Read-Host "请输入输出路径 (默认: $defaultOutput)"
        
        if ([string]::IsNullOrWhiteSpace($inputOutput)) {
            $Output = $defaultOutput
            Write-Info "使用默认输出路径: $Output"
            break
        }
        
        # 处理相对路径
        if (-not [System.IO.Path]::IsPathRooted($inputOutput)) {
            $Output = Join-Path $PSScriptRoot $inputOutput
        }
        else {
            $Output = $inputOutput
        }
        
        Write-Info "使用自定义输出路径: $Output"
        break
    } while ($true)
    
    return @{
        Version = $Version
        Output = $Output
    }
}

function Main {
    $startTime = Get-Date
    
    Write-Header "BaseFit Android APK 自动化打包工具"
    Write-Host "  项目路径: $PSScriptRoot" -ForegroundColor Gray
    Write-Host ""
    
    # 确定模式
    if ([string]::IsNullOrWhiteSpace($Version) -and [string]::IsNullOrWhiteSpace($Output)) {
        $result = Invoke-InteractiveMode
        $Version = $result.Version
        $Output = $result.Output
    }
    else {
        # 参数模式
        Write-Header "参数模式"
        
        # 处理版本号
        if ([string]::IsNullOrWhiteSpace($Version)) {
            $currentVersion = Get-CurrentVersion
            $Version = Increment-Version $currentVersion
            Write-Info "自动生成版本号: $Version"
        }
        elseif (-not (Test-SemanticVersion $Version)) {
            Write-Error "版本号格式无效: $Version"
            Write-Info "请使用 x.y.z 格式 (如 1.0.0)"
            exit 1
        }
        else {
            Write-Info "使用指定版本号: $Version"
        }
        
        # 处理输出路径
        if ([string]::IsNullOrWhiteSpace($Output)) {
            $Output = Join-Path $PSScriptRoot "dist\$Version"
            Write-Info "使用默认输出路径: $Output"
        }
        elseif (-not [System.IO.Path]::IsPathRooted($Output)) {
            $Output = Join-Path $PSScriptRoot $Output
            Write-Info "使用输出路径: $Output"
        }
        else {
            Write-Info "使用输出路径: $Output"
        }
    }
    
    # 执行打包流程
    try {
        # 1. 更新 build.gradle.kts
        if (-not (Update-BuildGradleVersion -NewVersion $Version)) {
            throw "更新版本号失败"
        }
        
        # 2. 执行构建
        if (-not (Invoke-AndroidBuild)) {
            throw "构建失败"
        }
        
        # 3. 复制输出
        if (-not (Copy-BuildOutput -OutputPath $Output -Version $Version)) {
            throw "复制构建产物失败"
        }
        
        # 4. 生成版本信息
        $versionInfoFile = New-VersionInfoFile -OutputPath $Output -Version $Version
        
        # 完成
        $endTime = Get-Date
        $duration = ($endTime - $startTime).TotalSeconds
        
        Write-Header "打包完成！"
        Write-Host ""
        Write-Success "版本号: $Version"
        Write-Success "输出路径: $Output"
        Write-Success "APK 文件: BaseFit-v$Version.apk"
        Write-Success "版本信息: $versionInfoFile"
        Write-Host ""
        Write-Info "耗时: $([math]::Round($duration, 2)) 秒"
        Write-Host ""
        
        return 0
    }
    catch {
        Write-Header "打包失败"
        Write-Error $_.Exception.Message
        Write-Host ""
        return 1
    }
}

# 运行主函数
exit Main
