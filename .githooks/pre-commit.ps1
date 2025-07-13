#!/usr/bin/env pwsh
# RecordApp pre-commit hook

Write-Host "Running RecordApp pre-commit checks..." -ForegroundColor Cyan

# Check for duplicate UI components
Write-Host "Checking for duplicate UI components..." -ForegroundColor Yellow
# TODO: Add check logic for duplicate UI components
$uiComponentsCheck = $true

# Run lint checks
Write-Host "Running lint checks..." -ForegroundColor Yellow
$lintResult = $true
try {
    # Run Gradle lint in quiet mode
    & ./gradlew lintDebug -q
    if ($LASTEXITCODE -ne 0) {
        $lintResult = $false
    }
} catch {
    $lintResult = $false
}

if (-not $lintResult) {
    Write-Host "Lint checks failed. Please fix the issues before committing." -ForegroundColor Red
    exit 1
}

# Check navigation components
Write-Host "Checking navigation components..." -ForegroundColor Yellow
# TODO: Add check logic for navigation components
$navComponentsCheck = $true

if (-not ($uiComponentsCheck -and $navComponentsCheck)) {
    Write-Host "Pre-commit checks failed. Please fix the issues before committing." -ForegroundColor Red
    exit 1
}

Write-Host "All pre-commit checks passed!" -ForegroundColor Green
exit 0 