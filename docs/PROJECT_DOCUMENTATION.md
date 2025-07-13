# RecordApp Project Documentation

## Project Overview

RecordApp is an Android application that allows users to record, manage, and export expense records. The app follows MVVM architecture with a clean, modular design.

## Recent Cleanup and Optimization

### Code Consolidation

1. **PDF Generation**
   - Merged `PdfGenerator.kt` and `PdfExporter.kt` into unified `PdfUtils.kt`
   - Implemented companion object methods for PDF generation functions
   - Added missing functionality like `generateImageGridPdf`

2. **CSV Export**
   - Merged `CsvGenerator.kt` and `CsvExporter.kt` into unified `CsvUtils.kt`
   - Implemented static helper methods and instance methods for CSV generation

3. **Backup Functionality**
   - Merged `BackupRestoreManager.kt`, `BackupScheduler.kt`, `BackupWorker.kt`, and `BackupFileUtils.kt` into unified `BackupModule.kt`
   - Added proper interface and implementation for backup scheduling
   - Fixed issues with backup restoration

### Dependency Management

1. **Updated Dependencies**
   - Added direct declarations for transitive dependencies
   - Changed dependency types as recommended (e.g., runtimeOnly instead of implementation for SLF4J)
   - Added Paging and WorkManager dependencies needed for app functionality

2. **Build Improvements**
   - Fixed compilation errors related to method references
   - Ensured proper API compatibility between old and new utility classes
   - Successfully built application after cleanup

## Compilation Issues Fixed

1. **PdfUtils Implementation Issues**
   - Updated method calls in ExpenseViewModel to use correct static companion methods
   - Added support for grid-based PDF generation

2. **Variable Reassignment Error**
   - Fixed variable declaration in `generateImageGridPdf` method

3. **CsvUtils Method Calls**
   - Updated method calls to use new CsvUtils companion object methods

## Remaining Technical Debt

1. **Deprecation Warnings**:
   - UI components use deprecated Icons (should update to AutoMirrored versions)
   - MasterKeys API usage is deprecated in the Repository module

2. **Test Coverage**:
   - Consolidated utility classes need unit tests
   - Overall test coverage should be improved

3. **Build Configuration**:
   - Build issues with AGP version compatibility to resolve

## Project Structure

- `data/` - Database and DAO implementations
- `di/` - Dependency injection modules
- `model/` - Data models and entities
- `network/` - Network-related components
- `repository/` - Data access layer
- `ui/screens/` - Full-screen UI components
- `ui/components/` - Reusable UI elements
- `util/` - Utility classes
- `viewmodel/` - ViewModels for business logic

## Next Steps

1. Address deprecation warnings
2. Add unit tests for consolidated utility classes
3. Update AGP to a stable version compatible with the dependency analysis plugin 