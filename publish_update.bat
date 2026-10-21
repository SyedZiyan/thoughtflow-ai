@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo   ThoughtFlow AI - 1-Click Over-The-Air (OTA) Release
echo ========================================================
echo.

set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set REPO_DIR=%~dp0

set /p NEW_VERSION="Enter new version name (e.g. 1.0.2): "
set /p RELEASE_NOTES="Enter release notes for this update: "

echo.
echo [1/3] Compiling new Android APK build...
cd /d "%REPO_DIR%apps\android"
call gradlew.bat assembleDebug
if %errorlevel% neq 0 (
    echo [ERROR] Gradle build failed. Check compilation errors above.
    pause
    exit /b %errorlevel%
)

echo.
echo [2/3] Updating backend OTA version info...
cd /d "%REPO_DIR%backend"
.\.venv\Scripts\python.exe -c "import json, os; p = os.path.join('app', 'version_info.json'); data = json.load(open(p)) if os.path.exists(p) else {'version_code': 1}; data['version_code'] = data.get('version_code', 1) + 1; data['version_name'] = '%NEW_VERSION%'; data['release_notes'] = '%RELEASE_NOTES%'; json.dump(data, open(p, 'w'), indent=2); print(f'OTA Version metadata updated to v%NEW_VERSION% (Code {data[\"version_code\"]})')"

echo.
echo [3/3] Ready!
echo ========================================================
echo   Update published successfully!
echo   All installed mobile apps will now see:
echo   'New Update Available: %NEW_VERSION%' with 'Update Now'
echo ========================================================
echo.
pause
