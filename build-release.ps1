# Android应用打包与发布脚本
# 功能：自动化构建、签名和发布Android应用
# 作者：BaseFit开发团队
# 版本：1.0.0

param(
    [string]$Version = "",
    [string]$OutputPath = "",
    [string]$BuildType = "release",
    [switch]$SkipVersionPrompt,
    [switch]$Help
)

$ProjectRoot = $PSScriptRoot
$AppModule = "app"
$DefaultVersion = "1.0.0"
$DefaultOutputPath = Join-Path $PSScriptRoot "dist"
$Gradlew = Join-Path $PSScriptRoot "gradlew.bat"
$BuildGradle = Join-Path $PSScriptRoot "app\build.gradle.kts"
$KeystoreProperties = Join-Path $PSScriptRoot "keystore.properties"

function Write-Success {
    param([string]$Message)
    Write-Host "[OK] $Message" -ForegroundColor Green
}

function Write-ErrorMsg {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

function Write-WarningMsg {
    param([string]$Message)
    Write-Host "[WARNING] $Message" -ForegroundColor Yellow
}

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Cyan
}

function Write-Title {
    param([string]$Message)
    Write-Host ""
    Write-Host "============================================================" -ForegroundColor White
    Write-Host $Message -ForegroundColor White
    Write-Host "============================================================" -ForegroundColor White
}

function Test-VersionFormat {
    param([string]$Version)
    $pattern = '^\d+\.\d+\.\d+$'
    return $Version -match $pattern
}

function Get-CurrentVersion {
    $content = Get-Content $BuildGradle -Raw
    if ($content -match 'versionName\s*=\s*"([^"]+)"') {
        return $Matches[1]
    }
    return $DefaultVersion
}

function Get-NextVersion {
    param([string]$CurrentVersion)
    $parts = $CurrentVersion.Split('.')
    if ($parts.Length -eq 3) {
        $patch = [int]$parts[2] + 1
        return "$($parts[0]).$($parts[1]).$patch"
    }
    return $DefaultVersion
}

function Update-VersionInBuildFile {
    param(
        [string]$NewVersion,
        [int]$NewVersionCode
    )

    $content = Get-Content $BuildGradle -Raw

    $content = $content -replace 'versionName\s*=\s*"[^"]+"', "versionName = `"$NewVersion`""
    $content = $content -replace 'versionCode\s*=\s*\d+', "versionCode = $NewVersionCode"

    Set-Content -Path $BuildGradle -Value $content -NoNewline
}

function Get-NextVersionCode {
    $content = Get-Content $BuildGradle -Raw
    if ($content -match 'versionCode\s*=\s*(\d+)') {
        return [int]$Matches[1] + 1
    }
    return 1
}

function Test-Environment {
    Write-Title "Checking Development Environment"

    $errors = @()

    if (-not (Test-Path $Gradlew)) {
        $errors += "Gradle Wrapper not found: $Gradlew"
    } else {
        Write-Success "Gradle Wrapper found"
    }

    if (-not (Test-Path $BuildGradle)) {
        $errors += "build.gradle.kts not found: $BuildGradle"
    } else {
        Write-Success "build.gradle.kts found"
    }

    if (Test-Path $KeystoreProperties) {
        Write-Success "Keystore configuration found"
    } else {
        Write-WarningMsg "Keystore configuration not found, using default config"
    }

    $javaHome = $env:JAVA_HOME
    if (-not $javaHome) {
        $javaHome = "C:\Users\whaif\.jdks\ms-17.0.16"
    }

    if (Test-Path $javaHome) {
        Write-Success "Java environment configured: $javaHome"
    } else {
        $errors += "Java environment not found: $javaHome"
    }

    if ($errors.Count -gt 0) {
        Write-Title "Environment Check Failed"
        foreach ($err in $errors) {
            Write-ErrorMsg $err
        }
        return $false
    }

    Write-Success "Environment check passed"
    return $true
}

function Invoke-Build {
    param(
        [string]$Type = "release"
    )

    Write-Title "Building $Type APK"

    $cleanCmd = "$Gradlew clean"
    Write-Info "Cleaning project..."

    $cleanResult = Invoke-Expression $cleanCmd 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-ErrorMsg "Failed to clean project"
        Write-ErrorMsg $cleanResult
        return $null
    }
    Write-Success "Project cleaned"

    $buildCmd = "$Gradlew assemble$Type"

    Write-Info "Building $Type version..."
    Write-Host "Command: $buildCmd" -ForegroundColor Gray

    $buildResult = Invoke-Expression $buildCmd 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-ErrorMsg "Build failed"
        Write-ErrorMsg $buildResult
        return $null
    }

    Write-Success "Build successful"

    $outputDir = Join-Path $ProjectRoot "$AppModule\build\outputs\apk\$Type"

    if (Test-Path (Join-Path $outputDir "app-$Type.apk")) {
        return Join-Path $outputDir "app-$Type.apk"
    }

    $apkFiles = Get-ChildItem -Path $outputDir -Filter "*.apk" -ErrorAction SilentlyContinue
    if ($apkFiles.Count -gt 0) {
        return $apkFiles[0].FullName
    }

    return $null
}

function Copy-OutputFile {
    param(
        [string]$SourceFile,
        [string]$DestinationPath,
        [string]$Version
    )

    if (-not (Test-Path $DestinationPath)) {
        New-Item -ItemType Directory -Path $DestinationPath -Force | Out-Null
    }

    $extension = [System.IO.Path]::GetExtension($SourceFile)
    $fileName = "BaseFit-v$Version$extension"
    $destFile = Join-Path $DestinationPath $fileName

    Copy-Item -Path $SourceFile -Destination $destFile -Force

    return $destFile
}

function Write-VersionInfo {
    param(
        [string]$Version,
        [string]$OutputPath,
        [string]$BuildType
    )

    $versionFile = Join-Path $OutputPath "version.txt"

    $content = @"
BaseFit Build Information
=========================
Version: $Version
Build Type: $BuildType
Build Time: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
Build Host: $env:COMPUTERNAME
"@

    Set-Content -Path $versionFile -Value $content
}

function Show-Help {
    $helpText = @"

================================================================================
                Android Application Build & Release Script - Help
================================================================================

Usage:
    .\build-release.ps1 [-Version <version>] [-OutputPath <path>]
                        [-BuildType <release|debug>] [-SkipVersionPrompt]

Parameters:
    -Version <version>       Specify version (format: x.y.z), auto-increment if not provided
    -OutputPath <path>      Specify output directory, default is dist folder
    -BuildType <type>       Build type: release (default) or debug
    -SkipVersionPrompt       Skip version confirmation prompt
    -Help                   Show this help message

Examples:
    # Interactive mode (will prompt for version)
    .\build-release.ps1

    # Specify version
    .\build-release.ps1 -Version 1.1.0

    # Specify output path
    .\build-release.ps1 -Version 1.1.0 -OutputPath C:\output

    # Skip version confirmation (auto-increment)
    .\build-release.ps1 -SkipVersionPrompt

    # Build Debug version
    .\build-release.ps1 -BuildType debug

Version Rules:
    - Use Semantic Versioning
    - Format: MAJOR.MINOR.PATCH
    - Example: 1.0.0 -> 1.0.1 -> 1.1.0 -> 2.0.0

Output Files:
    - APK: BaseFit-v{x.y.z}.apk
    - Version Info: version.txt

================================================================================
"@
    Write-Host $helpText -ForegroundColor Cyan
}

function Start-BuildProcess {
    param(
        [string]$InputVersion = "",
        [string]$InputOutputPath = "",
        [string]$InputBuildType = "release",
        [switch]$AutoConfirm
    )

    Write-Title "Android Application Build & Release"
    Write-Host "Project Directory: $ProjectRoot" -ForegroundColor Gray
    Write-Host ""

    if (-not (Test-Environment)) {
        Write-ErrorMsg "Environment check failed, please configure development environment first"
        return $false
    }

    $buildType = $InputBuildType.ToLower()
    if ($buildType -ne "release" -and $buildType -ne "debug") {
        Write-ErrorMsg "Invalid build type: $buildType, please use release or debug"
        return $false
    }

    $version = $InputVersion
    if ([string]::IsNullOrEmpty($version)) {
        $currentVersion = Get-CurrentVersion
        $version = Get-NextVersion -CurrentVersion $currentVersion
        Write-Info "Current version: $currentVersion"
        Write-Info "Auto-incremented version: $version"
    } else {
        if (-not (Test-VersionFormat -Version $version)) {
            Write-ErrorMsg "Invalid version format, please use x.y.z format (e.g., 1.0.0)"
            return $false
        }
    }

    if (-not $AutoConfirm) {
        Write-Host ""
        Write-Host "Version: $version" -ForegroundColor White
        Write-Host "Build Type: $buildType" -ForegroundColor White
        Write-Host ""

        $confirm = Read-Host "Confirm to start build? (Y/N)"
        if ($confirm -ne "Y" -and $confirm -ne "y") {
            Write-WarningMsg "Build cancelled by user"
            return $false
        }
    }

    $versionCode = Get-NextVersionCode
    Write-Info "Updating version info: versionName=$version, versionCode=$versionCode"
    Update-VersionInBuildFile -NewVersion $version -NewVersionCode $versionCode

    $outputPath = $InputOutputPath
    if ([string]::IsNullOrEmpty($outputPath)) {
        $outputPath = $DefaultOutputPath
    }

    Write-Host ""

    $outputFile = Invoke-Build -Type $buildType

    if (-not $outputFile) {
        Write-ErrorMsg "Build failed, please check error messages"
        return $false
    }

    Write-Host ""
    Write-Title "Copying Output File"

    try {
        $finalFile = Copy-OutputFile -SourceFile $outputFile -DestinationPath $outputPath -Version $version
        Write-Success "Output file copied to: $finalFile"

        Write-VersionInfo -Version $version -OutputPath $outputPath -BuildType $buildType
        Write-Success "Version info saved"

    } catch {
        Write-ErrorMsg "Failed to copy file: $_"
        Write-Info "Original build file location: $outputFile"
    }

    Write-Host ""
    Write-Title "Build Complete"
    Write-Host "Version: $version" -ForegroundColor White
    Write-Host "Build Type: $buildType" -ForegroundColor White
    Write-Host "Output Path: $outputPath" -ForegroundColor White
    Write-Host ""

    return $true
}

if ($Help) {
    Show-Help
    exit 0
}

$autoConfirm = $SkipVersionPrompt

$result = Start-BuildProcess -InputVersion $Version -InputOutputPath $OutputPath -InputBuildType $BuildType -AutoConfirm:$autoConfirm

if ($result) {
    exit 0
} else {
    exit 1
}
