#!/bin/bash

# RecordApp Cleanup Script
# ------------------------
# This script performs comprehensive cleanup operations on the RecordApp project.
# It removes build artifacts, optimizes imports, and helps maintain a clean codebase.

echo "========================================"
echo "Starting RecordApp Project Cleanup"
echo "========================================"

# Gradle clean to remove build artifacts
echo -e "\n🧹 Cleaning build artifacts..."
./gradlew clean

# Removing temporary files
echo -e "\n🧹 Removing temporary files..."
find . -type f -name ".DS_Store" -delete
find . -name "*~" -delete
find . -name "*.swp" -delete
find . -name "*.log" -delete

# Remove empty directories
echo -e "\n🧹 Removing empty directories..."
find . -type d -empty -delete

# Run lint checks
echo -e "\n🔍 Running Android lint checks..."
./gradlew lintDebug

# Run dependency analysis
echo -e "\n🔍 Analyzing dependencies..."
./gradlew dependencyReportDebug

# Suggest optimizations
echo -e "\n✅ Cleanup complete! Next steps:"
echo "1. Check lint results at app/build/reports/lint-results-debug.html"
echo "2. Check dependency reports at app/build/reports/dependencies/"
echo "3. Review the CLEANUP_REPORT.md file for documentation"
echo "========================================" 