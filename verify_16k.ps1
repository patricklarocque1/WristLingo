# 16KB Alignment Verification Script for WristLingo
# This script extracts APK, checks each .so file with llvm-readelf, and reports alignment status

param(
    [string]$ApkPath = "app\build\outputs\apk\debug\app-debug.apk",
    [string]$ExtractDir = "tmp_16k_check"
)

Write-Host "🔍 16KB Alignment Verification" -ForegroundColor Cyan
Write-Host "APK: $ApkPath" -ForegroundColor Gray

# Resolve SDK/NDK tools from project files
$root = (Get-Location).Path
Write-Host "Project root: $root" -ForegroundColor Gray

# Read SDK path from local.properties
$localProps = Join-Path $root "local.properties"
if (-not (Test-Path $localProps)) {
    Write-Host "❌ local.properties not found. Please ensure Android SDK is configured." -ForegroundColor Red
    exit 1
}
$sdkLine = Get-Content $localProps | Where-Object { $_ -match '^sdk\.dir=' }
if (-not $sdkLine) {
    Write-Host "❌ sdk.dir not found in local.properties" -ForegroundColor Red
    exit 1
}
$sdk = $sdkLine -replace '^sdk\.dir=','' -replace '\\\\','\\' -replace '\\','/'
Write-Host "SDK: $sdk" -ForegroundColor Gray

# Read NDK version from gradle.properties
$gradleProps = Join-Path $root "gradle.properties"
if (-not (Test-Path $gradleProps)) {
    Write-Host "❌ gradle.properties not found" -ForegroundColor Red
    exit 1
}
$ndkLine = Get-Content $gradleProps | Where-Object { $_ -match '^android\.ndkVersion=' }
if (-not $ndkLine) {
    Write-Host "❌ android.ndkVersion not found in gradle.properties" -ForegroundColor Red
    exit 1
}
$ndkVersion = $ndkLine -replace '^android\.ndkVersion=',''
Write-Host "NDK Version: $ndkVersion" -ForegroundColor Gray

# Construct readelf path
$readelf = Join-Path $sdk "ndk/$ndkVersion/toolchains/llvm/prebuilt/windows-x86_64/bin/llvm-readelf.exe"
if (-not (Test-Path $readelf)) {
    Write-Host "❌ llvm-readelf not found at: $readelf" -ForegroundColor Red
    Write-Host "Please verify NDK installation and version" -ForegroundColor Yellow
    exit 1
}
Write-Host "Readelf: $readelf" -ForegroundColor Gray

# Check if APK exists
if (-not (Test-Path $ApkPath)) {
    Write-Host "❌ APK not found: $ApkPath" -ForegroundColor Red
    Write-Host "Please build the app first: .\gradlew :app:assembleDebug" -ForegroundColor Yellow
    exit 1
}

# Clean up previous extraction
if (Test-Path $ExtractDir) {
    Remove-Item -Recurse -Force $ExtractDir
}

# Extract APK
Write-Host "`n📦 Extracting APK..." -ForegroundColor Cyan
try {
    Expand-Archive -Path $ApkPath -DestinationPath $ExtractDir -Force
    Write-Host "✅ APK extracted to $ExtractDir" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to extract APK: $_" -ForegroundColor Red
    exit 1
}

# Check arm64-v8a directory
$arm64Dir = Join-Path $ExtractDir "lib\arm64-v8a"
if (-not (Test-Path $arm64Dir)) {
    Write-Host "❌ No arm64-v8a directory found in APK" -ForegroundColor Red
    exit 1
}

# Get all .so files
$soFiles = Get-ChildItem "$arm64Dir\*.so"
if ($soFiles.Count -eq 0) {
    Write-Host "❌ No .so files found in arm64-v8a" -ForegroundColor Red
    exit 1
}

Write-Host "`n🔍 Checking $($soFiles.Count) native libraries..." -ForegroundColor Cyan

# Check each .so file
$bad = $false
$total = $soFiles.Count
$current = 0

foreach ($so in $soFiles) {
    $current++
    $name = $so.Name
    $progress = "[$current/$total]"
    
    Write-Host "`n$progress === $name ===" -ForegroundColor White
    
    # Run readelf
    try {
        $out = & $readelf -lW $so.FullName 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Host "❌ readelf failed: $out" -ForegroundColor Red
            $bad = $true
            continue
        }
    } catch {
        Write-Host "❌ Failed to run readelf: $_" -ForegroundColor Red
        $bad = $true
        continue
    }
    
    # Parse LOAD lines
    $loadLines = $out -split "`n" | Where-Object { $_.TrimStart().StartsWith("LOAD") }
    if (-not $loadLines) {
        Write-Host "❌ No LOAD program headers found" -ForegroundColor Red
        $bad = $true
        continue
    }
    
    # Check alignment - each LOAD line's last token should be 0x4000 or 16384
    $allAligned = $true
    foreach ($line in $loadLines) {
        $tokens = $line.Trim() -split "\s+"
        $lastToken = $tokens[-1]
        if ($lastToken -ne "0x4000" -and $lastToken -ne "16384") {
            $allAligned = $false
            Write-Host "  ❌ LOAD segment not aligned: $line" -ForegroundColor Red
        }
    }
    
    if ($allAligned) {
        Write-Host "  ✅ All LOAD segments aligned to 0x4000" -ForegroundColor Green
    } else {
        Write-Host "  ❌ One or more LOAD segments not 16KB aligned" -ForegroundColor Red
        $bad = $true
    }
}

# Check ZIP alignment
Write-Host "`n📏 Checking APK ZIP alignment..." -ForegroundColor Cyan
$zipalign = Join-Path $sdk "build-tools\35.0.0\zipalign.exe"
if (Test-Path $zipalign) {
    try {
        $zipOut = & $zipalign -v -c -P 16 4 $ApkPath 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ APK ZIP alignment verified (16KB)" -ForegroundColor Green
        } else {
            Write-Host "❌ APK ZIP alignment failed" -ForegroundColor Red
            Write-Host $zipOut -ForegroundColor Yellow
            $bad = $true
        }
    } catch {
        Write-Host "❌ Failed to check ZIP alignment: $_" -ForegroundColor Red
        $bad = $true
    }
} else {
    Write-Host "⚠️ zipalign not found, skipping ZIP alignment check" -ForegroundColor Yellow
}

# Cleanup
Remove-Item -Recurse -Force $ExtractDir

# Final result
Write-Host "`n" + "="*50 -ForegroundColor Cyan
if ($bad) {
    Write-Host "❌ VERIFICATION FAILED" -ForegroundColor Red
    Write-Host "One or more libraries are not 16KB aligned" -ForegroundColor Red
    exit 1
} else {
    Write-Host "✅ VERIFICATION PASSED" -ForegroundColor Green
    Write-Host "All native libraries are 16KB aligned" -ForegroundColor Green
    Write-Host "Ready for Google Play Store (Nov 1, 2025 deadline)" -ForegroundColor Green
}
