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
Write-Host "Releasing Version: $VersionName" -ForegroundColor Yellow
Write-Host "Release Notes: $ReleaseNotes" -ForegroundColor Yellow
Write-Host ""

# 1. Compile Gradle APK
Write-Host "[1/3] Compiling fresh Android APK..." -ForegroundColor Green
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
Set-Location "$repoDir\apps\android"
& .\gradlew.bat assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle compilation failed."
}

# 2. Update version metadata in backend/app/version_info.json
Write-Host "[2/3] Updating backend OTA metadata..." -ForegroundColor Green
$versionJsonPath = "$repoDir\backend\app\version_info.json"
$currentCode = 2
if (Test-Path $versionJsonPath) {
    $existing = Get-Content $versionJsonPath | ConvertFrom-Json
    if ($existing.version_code) {
        $currentCode = [int]$existing.version_code
    }
}
$newCode = $currentCode + 1

$versionData = @{
    version_name = $VersionName
    version_code = $newCode
    release_notes = $ReleaseNotes
    download_url = "/api/v1/app/download"
}

$versionData | ConvertTo-Json -Depth 5 | Set-Content $versionJsonPath -Encoding utf8

Write-Host "[3/3] OTA Release Published!" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host " Version Code: $newCode" -ForegroundColor Yellow
Write-Host " Version Name: $VersionName" -ForegroundColor Yellow
Write-Host " All installed Android devices will now see:" -ForegroundColor Yellow
Write-Host " 'Update Available (v$VersionName)' with 'Update Now' button!" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Cyan
