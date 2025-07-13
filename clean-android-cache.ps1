#!/usr/bin/env pwsh
# RecordApp Android Cache Cleanup Script

param(
    [switch]$Force = $false
)

Write-Host "RecordApp Android Cache Cleanup" -ForegroundColor Cyan

# Ask for confirmation if Force is not specified
if (-not $Force) {
    $confirmation = Read-Host "This will clear Android cache files and build directories. Continue? (y/n)"
    if ($confirmation -ne 'y') {
        Write-Host "Operation cancelled." -ForegroundColor Yellow
        exit 0
    }
}

Write-Host "Cleaning Android cache files..." -ForegroundColor Yellow

# Clean paths to check/remove
$pathsToClean = @(
    ".gradle",
    "build",
    "app/build",
    ".idea/caches",
    ".kotlin"
)

foreach ($path in $pathsToClean) {
    if (Test-Path $path) {
        Write-Host "Removing $path..." -ForegroundColor Yellow
        try {
            Remove-Item -Path $path -Recurse -Force -ErrorAction Stop
            Write-Host "Successfully removed $path" -ForegroundColor Green
        } catch {
            $errorMessage = $_.Exception.Message
            Write-Host "Failed to remove ${path}: $errorMessage" -ForegroundColor Red
        }
    }
}

# Clean Gradle cache
Write-Host "Running Gradle clean task..." -ForegroundColor Yellow
try {
    ./gradlew clean --quiet
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Gradle clean completed successfully" -ForegroundColor Green
    } else {
        Write-Host "Gradle clean task failed with exit code $LASTEXITCODE" -ForegroundColor Red
    }
} catch {
    $errorMessage = $_.Exception.Message
    Write-Host "Failed to run Gradle clean task: $errorMessage" -ForegroundColor Red
}

# Remove Java error logs
Get-ChildItem -Path . -Filter "hs_err_pid*.log" | ForEach-Object {
    Write-Host "Removing error log: $($_.Name)" -ForegroundColor Yellow
    Remove-Item $_.FullName -Force
}

# Remove replay logs
Get-ChildItem -Path . -Filter "replay_pid*.log" | ForEach-Object {
    Write-Host "Removing replay log: $($_.Name)" -ForegroundColor Yellow
    Remove-Item $_.FullName -Force
}

Write-Host "Android cache cleanup completed!" -ForegroundColor Green 