# ThoughtFlow AI - 1-Click Over-The-Air (OTA) Release Script
param (
    [string]$VersionName = "1.0.2",
    [string]$ReleaseNotes = "New features and performance improvements"
)

$ErrorActionPreference = "Stop"
$repoDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  ThoughtFlow AI - 1-Click Over-The-Air (OTA) Release" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "Target Version: $VersionName" -ForegroundColor Yellow
Write-Host "Release Notes: $ReleaseNotes" -ForegroundColor Yellow
Write-Host ""

# 1. Calculate next version code
$versionJsonPath = "$repoDir\backend\app\version_info.json"
$currentCode = 2
if (Test-Path $versionJsonPath) {
    $existing = Get-Content $versionJsonPath | ConvertFrom-Json
    if ($existing.version_code) {
        $currentCode = [int]$existing.version_code
    }
}
$newCode = $currentCode + 1

# 2. Sync versionCode & versionName in build.gradle.kts
Write-Host "[1/3] Syncing version in build.gradle.kts (Code $newCode, Name $VersionName)..." -ForegroundColor Green
$gradlePath = "$repoDir\apps\android\app\build.gradle.kts"
$gradleContent = Get-Content $gradlePath -Raw
$gradleContent = $gradleContent -replace 'versionCode\s*=\s*\d+', "versionCode = $newCode"
$gradleContent = $gradleContent -replace 'versionName\s*=\s*"[^"]+"', "versionName = `"$VersionName`""
Set-Content $gradlePath $gradleContent -NoNewline

# 3. Compile fresh Android APK
Write-Host "[2/3] Compiling fresh Android APK with Gradle..." -ForegroundColor Green
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
Set-Location "$repoDir\apps\android"
& .\gradlew.bat assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle compilation failed."
}

# 4. Update backend OTA metadata
Write-Host "[3/3] Publishing OTA metadata to backend..." -ForegroundColor Green
$versionData = @{
    version_name = $VersionName
    version_code = $newCode
    release_notes = $ReleaseNotes
    download_url = "/api/v1/app/download"
}
$versionData | ConvertTo-Json -Depth 5 | Set-Content $versionJsonPath -Encoding utf8

Write-Host ""
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host " SUCCESS: OTA Update Published!" -ForegroundColor Green
Write-Host " Version Code: $newCode" -ForegroundColor Yellow
Write-Host " Version Name: $VersionName" -ForegroundColor Yellow
Write-Host " All installed Android devices will now see:" -ForegroundColor Yellow
Write-Host " 'Update Available (v$VersionName)' with 'Update Now' button!" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Cyan
