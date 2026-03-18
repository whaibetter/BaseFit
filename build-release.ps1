# BaseFit Android APK Build Script
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
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  $Message" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
}

function Write-Success {
    param([string]$Message)
    Write-Host "[OK] $Message" -ForegroundColor Green
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "  > $Message" -ForegroundColor Yellow
}

function Test-SemanticVersion {
    param([string]$Version)
    $pattern = '^\d+\.\d+\.\d+$'
    return $Version -match $pattern
}

function Get-CurrentVersion {
    $buildGradlePath = Join-Path $PSScriptRoot "app\build.gradle.kts"
    if (-not (Test-Path $buildGradlePath)) {
        Write-Error "build.gradle.kts not found"
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
        Write-Error "build.gradle.kts not found"
        return $false
    }
    
    $content = Get-Content $buildGradlePath -Raw
    
    # Update versionName
    $content = $content -replace 'versionName\s*=\s*"[^"]*"', "versionName = `"$NewVersion`""
    
    # Update versionCode
    if ($content -match 'versionCode\s*=\s*(\d+)') {
        $oldCode = [int]$matches[1]
        $newCode = $oldCode + 1
        $content = $content -replace 'versionCode\s*=\s*\d+', "versionCode = $newCode"
    }
    
    Set-Content -Path $buildGradlePath -Value $content -NoNewline
    Write-Success "Updated build.gradle.kts: versionName=$NewVersion"
    return $true
}

function Invoke-AndroidBuild {
    Write-Header "Starting Android APK Build"
    
    $gradlewPath = Join-Path $PSScriptRoot "gradlew.bat"
    if (-not (Test-Path $gradlewPath)) {
        Write-Error "gradlew.bat not found"
        return $false
    }
    
    Write-Info "Executing Gradle build..."
    Push-Location $PSScriptRoot
    
    try {
        & .\gradlew.bat clean assembleRelease
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Gradle build failed"
            Pop-Location
            return $false
        }
        Write-Success "Gradle build successful"
        Pop-Location
        return $true
    }
    catch {
        Write-Error "Build error: $_"
        Pop-Location
        return $false
    }
}

function Copy-BuildOutput {
    param(
        [string]$OutputPath,
        [string]$Version
    )
    
    Write-Header "Copying Build Output"
    
    $apkSourcePath = Join-Path $PSScriptRoot "app\build\outputs\apk\release\app-release.apk"
    
    if (-not (Test-Path $apkSourcePath)) {
        Write-Error "APK file not found: $apkSourcePath"
        return $false
    }
    
    # Create output directory
    if (-not (Test-Path $OutputPath)) {
        Write-Info "Creating output directory: $OutputPath"
        New-Item -ItemType Directory -Path $OutputPath -Force | Out-Null
    }
    
    # Copy APK
    $apkDestName = "BaseFit-v$Version.apk"
    $apkDestPath = Join-Path $OutputPath $apkDestName
    
    Write-Info "Copying APK: $apkDestName"
    Copy-Item -Path $apkSourcePath -Destination $apkDestPath -Force
    
    if (-not (Test-Path $apkDestPath)) {
        Write-Error "APK copy failed"
        return $false
    }
    
    Write-Success "APK copied to: $apkDestPath"
    return $true
}

function New-VersionInfoFile {
    param(
        [string]$OutputPath,
        [string]$Version
    )
    
    Write-Header "Generating Version Info"
    
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
BaseFit Version Info
====================
Version: $Version
Build Time: $buildTime
Git Commit: $gitCommit
APK File: BaseFit-v$Version.apk
"@
    
    Set-Content -Path $versionInfoPath -Value $content -Encoding UTF8
    Write-Success "Version info file: $versionInfoPath"
    
    return $versionInfoPath
}

function Invoke-InteractiveMode {
    Write-Header "BaseFit APK Build Tool - Interactive Mode"
    
    # Get current version
    $currentVersion = Get-CurrentVersion
    $defaultVersion = Increment-Version $currentVersion
    
    # Version input
    $validVersion = $false
    while (-not $validVersion) {
        Write-Host ""
        $inputVersion = Read-Host "Enter version (current: $currentVersion, default: $defaultVersion)"
        
        if ([string]::IsNullOrWhiteSpace($inputVersion)) {
            $Version = $defaultVersion
            Write-Info "Using default version: $Version"
            $validVersion = $true
        }
        elseif (Test-SemanticVersion $inputVersion) {
            $Version = $inputVersion
            Write-Info "Using version: $Version"
            $validVersion = $true
        }
        else {
            Write-Error "Invalid format, use x.y.z (e.g. 1.0.0)"
        }
    }
    
    # Output path input
    $defaultOutput = Join-Path $PSScriptRoot "dist\$Version"
    $validOutput = $false
    
    while (-not $validOutput) {
        Write-Host ""
        $inputOutput = Read-Host "Enter output path (default: $defaultOutput)"
        
        if ([string]::IsNullOrWhiteSpace($inputOutput)) {
            $Output = $defaultOutput
            Write-Info "Using output path: $Output"
            $validOutput = $true
        }
        elseif (-not [System.IO.Path]::IsPathRooted($inputOutput)) {
            $Output = Join-Path $PSScriptRoot $inputOutput
            Write-Info "Using output path: $Output"
            $validOutput = $true
        }
        else {
            $Output = $inputOutput
            Write-Info "Using output path: $Output"
            $validOutput = $true
        }
    }
    
    return @{
        Version = $Version
        Output = $Output
    }
}

function Main {
    $startTime = Get-Date
    
    Write-Header "BaseFit Android APK Build Tool"
    Write-Host "  Project: $PSScriptRoot" -ForegroundColor Gray
    Write-Host ""
    
    # Determine mode
    if ([string]::IsNullOrWhiteSpace($Version) -and [string]::IsNullOrWhiteSpace($Output)) {
        $result = Invoke-InteractiveMode
        $Version = $result.Version
        $Output = $result.Output
    }
    else {
        Write-Header "Parameter Mode"
        
        # Handle version
        if ([string]::IsNullOrWhiteSpace($Version)) {
            $currentVersion = Get-CurrentVersion
            $Version = Increment-Version $currentVersion
            Write-Info "Auto version: $Version"
        }
        elseif (-not (Test-SemanticVersion $Version)) {
            Write-Error "Invalid version format: $Version"
            Write-Info "Use x.y.z format (e.g. 1.0.0)"
            exit 1
        }
        else {
            Write-Info "Version: $Version"
        }
        
        # Handle output path
        if ([string]::IsNullOrWhiteSpace($Output)) {
            $Output = Join-Path $PSScriptRoot "dist\$Version"
            Write-Info "Output: $Output"
        }
        elseif (-not [System.IO.Path]::IsPathRooted($Output)) {
            $Output = Join-Path $PSScriptRoot $Output
            Write-Info "Output: $Output"
        }
        else {
            Write-Info "Output: $Output"
        }
    }
    
    # Execute build process
    try {
        # 1. Update build.gradle.kts
        if (-not (Update-BuildGradleVersion -NewVersion $Version)) {
            throw "Failed to update version"
        }
        
        # 2. Execute build
        if (-not (Invoke-AndroidBuild)) {
            throw "Build failed"
        }
        
        # 3. Copy output
        if (-not (Copy-BuildOutput -OutputPath $Output -Version $Version)) {
            throw "Failed to copy APK"
        }
        
        # 4. Generate version info
        $versionInfoFile = New-VersionInfoFile -OutputPath $Output -Version $Version
        
        # Complete
        $endTime = Get-Date
        $duration = ($endTime - $startTime).TotalSeconds
        
        Write-Header "Build Complete!"
        Write-Host ""
        Write-Success "Version: $Version"
        Write-Success "Output: $Output"
        Write-Success "APK: BaseFit-v$Version.apk"
        Write-Success "Info: $versionInfoFile"
        Write-Host ""
        Write-Info "Time: $([math]::Round($duration, 2)) seconds"
        Write-Host ""
        
        return 0
    }
    catch {
        Write-Header "Build Failed"
        Write-Error $_.Exception.Message
        Write-Host ""
        return 1
    }
}

# Run main function
exit Main