@echo off
echo ========================================
echo FelixSkin Mod Builder
echo ========================================
echo.

REM Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 17 or higher and try again
    pause
    exit /b 1
)

REM Check Java version
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
    goto :check_version
)
:check_version
echo Current Java version: %JAVA_VERSION%
echo.

REM Check if Gradle wrapper exists
if not exist "gradlew.bat" (
    echo ERROR: Gradle wrapper not found
    echo Please ensure gradlew.bat exists in the current directory
    pause
    exit /b 1
)

echo Building FelixSkin mod...
echo.

REM Clean previous builds
echo Cleaning previous builds...
call gradlew.bat clean
if errorlevel 1 (
    echo ERROR: Failed to clean project
    pause
    exit /b 1
)

REM Build the mod
echo Building mod...
call gradlew.bat build
if errorlevel 1 (
    echo ERROR: Build failed
    echo Check the error messages above for details
    pause
    exit /b 1
)

echo.
echo ========================================
echo Build completed successfully!
echo ========================================
echo.

REM Check if build files exist
if exist "build\libs\felixskin-1.1.0.jar" (
    echo Output files:
    echo - build\libs\felixskin-1.1.0.jar (Main mod)
    echo - build\libs\felixskin-1.1.0-sources.jar (Source code)
    echo.
    echo The mod is ready to use!
    echo Copy felixskin-1.1.0.jar to your Minecraft mods folder
) else (
    echo WARNING: Expected build files not found
    echo Check the build directory for actual output files
)

echo.
echo Press any key to exit...
pause >nul