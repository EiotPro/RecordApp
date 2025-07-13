#!/usr/bin/env pwsh
# RecordApp Project Cleanup Script

Write-Host "RecordApp Project Cleanup" -ForegroundColor Cyan

# Function to check for duplicate UI components
function Check-DuplicateUIComponents {
    Write-Host "Checking for duplicate UI components..." -ForegroundColor Yellow
    
    # Define paths to check for UI components
    $uiComponentPaths = @(
        "app/src/main/java/*/ui/components",
        "app/src/main/java/*/presentation/components"
    )
    
    $duplicates = @{}
    
    foreach ($path in $uiComponentPaths) {
        if (Test-Path $path) {
            $components = Get-ChildItem -Path $path -Recurse -Filter "*.kt" | 
                Where-Object { $_.Name -match "^[A-Z].*\.kt$" }
            
            foreach ($component in $components) {
                $baseName = $component.BaseName
                if (-not $duplicates.ContainsKey($baseName)) {
                    $duplicates[$baseName] = @()
                }
                $duplicates[$baseName] += $component.FullName
            }
        }
    }
    
    $hasDuplicates = $false
    foreach ($key in $duplicates.Keys) {
        if ($duplicates[$key].Count -gt 1) {
            $hasDuplicates = $true
            Write-Host "Duplicate UI component found: $key" -ForegroundColor Red
            foreach ($path in $duplicates[$key]) {
                Write-Host "  - $path" -ForegroundColor Red
            }
        }
    }
    
    if ($hasDuplicates) {
        $confirmation = Read-Host "Do you want to remove duplicate components? (y/n)"
        if ($confirmation -eq 'y') {
            # Logic for removing duplicates would go here
            # This would require specific app knowledge to implement properly
            Write-Host "Please manually remove duplicate components at this time." -ForegroundColor Yellow
        }
    } else {
        Write-Host "No duplicate UI components found." -ForegroundColor Green
    }
}

# Run Gradle clean
function Clean-GradleBuild {
    Write-Host "Cleaning build files using Gradle..." -ForegroundColor Yellow
    try {
        ./gradlew clean --quiet
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Gradle clean completed successfully" -ForegroundColor Green
            return $true
        } else {
            Write-Host "Gradle clean failed with exit code $LASTEXITCODE" -ForegroundColor Red
            return $false
        }
    } catch {
        $errorMessage = $_.Exception.Message
        Write-Host "Failed to run Gradle clean: $errorMessage" -ForegroundColor Red
        return $false
    }
}

# Run Android cache cleanup
function Clean-AndroidCache {
    Write-Host "Cleaning Android cache..." -ForegroundColor Yellow
    try {
        & "$PSScriptRoot\clean-android-cache.ps1" -Force
        Write-Host "Android cache cleanup completed" -ForegroundColor Green
        return $true
    } catch {
        $errorMessage = $_.Exception.Message
        Write-Host "Failed to clean Android cache: $errorMessage" -ForegroundColor Red
        return $false
    }
}

# Remove Java error logs
function Remove-ErrorLogs {
    Write-Host "Removing Java error logs..." -ForegroundColor Yellow
    $count = 0
    Get-ChildItem -Path . -Filter "hs_err_pid*.log" | ForEach-Object {
        Remove-Item $_.FullName -Force
        $count++
    }
    
    Get-ChildItem -Path . -Filter "replay_pid*.log" | ForEach-Object {
        Remove-Item $_.FullName -Force
        $count++
    }
    
    Write-Host "Removed $count log files" -ForegroundColor Green
}

# Run lint to identify unused resources
function Find-UnusedResources {
    Write-Host "Running lint to identify unused resources..." -ForegroundColor Yellow
    try {
        ./gradlew lintDebug --quiet
        Write-Host "Lint completed. Check the lint report in app/build/reports/lint-results.html for unused resources." -ForegroundColor Green
    } catch {
        $errorMessage = $_.Exception.Message
        Write-Host "Failed to run lint: $errorMessage" -ForegroundColor Red
    }
}

# Main execution
Check-DuplicateUIComponents
Clean-GradleBuild
Clean-AndroidCache
Remove-ErrorLogs
Find-UnusedResources

Write-Host "RecordApp project cleanup completed!" -ForegroundColor Green 