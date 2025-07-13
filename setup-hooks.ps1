#!/usr/bin/env pwsh
# RecordApp Git Hooks Setup Script

Write-Host "Setting up Git hooks for RecordApp..." -ForegroundColor Cyan

# Configure Git to use hooks from the .githooks directory
git config core.hooksPath .githooks

# Ensure the pre-commit hook is executable (for non-Windows environments)
if ($PSVersionTable.Platform -ne "Win32NT") {
    Write-Host "Setting executable permissions on pre-commit hooks..." -ForegroundColor Yellow
    chmod +x .githooks/pre-commit
    chmod +x .githooks/pre-commit.ps1
}

# Verify setup
$hooksPath = git config --get core.hooksPath
if ($hooksPath -eq ".githooks") {
    Write-Host "Git hooks have been successfully set up!" -ForegroundColor Green
} else {
    Write-Host "Failed to set up Git hooks. Please check for errors and try again." -ForegroundColor Red
}

Write-Host "Setup complete!" -ForegroundColor Green 