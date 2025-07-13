@echo off
REM RecordApp Cleanup Script for Windows
REM -------------------------------------
REM This script performs comprehensive cleanup operations on the RecordApp project.
REM It removes build artifacts, optimizes imports, and helps maintain a clean codebase.

echo ========================================
echo Starting RecordApp Project Cleanup
echo ========================================

REM Gradle clean to remove build artifacts
echo.
echo [Cleaning build artifacts...]
call gradlew.bat clean

REM Removing temporary files
echo.
echo [Removing temporary files...]
del /s /q *.bak
del /s /q *.tmp
del /s /q *.log

REM Run lint checks
echo.
echo [Running Android lint checks...]
call gradlew.bat lintDebug

REM Run dependency analysis
echo.
echo [Analyzing dependencies...]
call gradlew.bat dependencyReportDebug

REM Suggest optimizations
echo.
echo [Cleanup complete! Next steps:]
echo 1. Check lint results at app\build\reports\lint-results-debug.html
echo 2. Check dependency reports at app\build\reports\dependencies\
echo 3. Review the CLEANUP_REPORT.md file for documentation
echo ========================================

pause 