# FelixSkin Mod Builder - PowerShell Version
# Run this script in PowerShell with: .\build.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "FelixSkin Mod Builder" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Java is installed
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    if ($javaVersion) {
        Write-Host "Java found: $javaVersion" -ForegroundColor Green
    } else {
        throw "Java not found"
    }
} catch {
    Write-Host "ERROR: Java is not installed or not in PATH" -ForegroundColor Red
    Write-Host "Please install Java 17 or higher and try again" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

# Check if Gradle wrapper exists
if (-not (Test-Path "gradlew.bat")) {
    Write-Host "ERROR: Gradle wrapper not found" -ForegroundColor Red
    Write-Host "Please ensure gradlew.bat exists in the current directory" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "Building FelixSkin mod..." -ForegroundColor Green
Write-Host ""

# Clean previous builds
Write-Host "Cleaning previous builds..." -ForegroundColor Yellow
try {
    & .\gradlew.bat clean
    if ($LASTEXITCODE -ne 0) {
        throw "Clean failed with exit code $LASTEXITCODE"
    }
} catch {
    Write-Host "ERROR: Failed to clean project" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

# Build the mod
Write-Host "Building mod..." -ForegroundColor Yellow
try {
    & .\gradlew.bat build
    if ($LASTEXITCODE -ne 0) {
        throw "Build failed with exit code $LASTEXITCODE"
    }
} catch {
    Write-Host "ERROR: Build failed" -ForegroundColor Red
    Write-Host "Check the error messages above for details" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Build completed successfully!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

# Check if build files exist
$mainJar = "build\libs\felixskin-1.1.0.jar"
$sourceJar = "build\libs\felixskin-1.1.0-sources.jar"

if (Test-Path $mainJar) {
    Write-Host "Output files:" -ForegroundColor Green
    Write-Host "- $mainJar (Main mod)" -ForegroundColor White
    Write-Host "- $sourceJar (Source code)" -ForegroundColor White
    Write-Host ""
    Write-Host "The mod is ready to use!" -ForegroundColor Green
    Write-Host "Copy felixskin-1.1.0.jar to your Minecraft mods folder" -ForegroundColor Yellow
    
    # Show file sizes
    $mainSize = (Get-Item $mainJar).Length
    $mainSizeMB = [math]::Round($mainSize / 1MB, 2)
    Write-Host "Main JAR size: $mainSizeMB MB" -ForegroundColor Cyan
    
} else {
    Write-Host "WARNING: Expected build files not found" -ForegroundColor Yellow
    Write-Host "Check the build directory for actual output files" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Press Enter to exit..."
Read-Host
