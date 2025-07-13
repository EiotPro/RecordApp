# ZIP Export Feature Documentation

## Overview

The ZIP Export feature allows users to export expense data in multiple formats (PDF, CSV, and Grid) combined into a single ZIP archive. This feature is designed to provide a comprehensive data backup solution and facilitate easier sharing of expense records.

## Feature Details

### User Interface

- ZIP Export option is available in the dropdown menu on the Expenses screen
- Available for both "All Expenses" view and individual folder views
- Uses proper permission handling for storage access

### File Structure

The exported ZIP file contains three subdirectories:
1. **PDFs/** - Contains PDF reports of expenses
2. **CSVs/** - Contains CSV data exports
3. **Grid/** - Contains grid layout PDF exports of images

### Naming Convention

- For all expenses: `RecordApp_ALL_[timestamp].zip`
- For specific folder: `RecordApp_Folder_[folderName]_[timestamp].zip`

Where `[timestamp]` is in the format `yyyyMMdd_HHmmss`.

## Implementation

### Core Components

1. **ZipUtils.kt**
   - Main utility class for generating ZIP archives
   - Handles creation of subdirectories and file organization
   - Manages temporary files and cleanup

2. **ExpenseViewModel.kt**
   - Contains `generateZipExport()` method to handle ZIP generation
   - Manages loading states and error handling
   - Filters expenses by folder when needed

3. **ExpensesScreen.kt**
   - Provides UI integration for ZIP export functionality
   - Handles permission requests
   - Shows progress and completion feedback

### Technical Process

1. When a user selects "ZIP Export":
   - Storage permissions are checked and requested if needed
   - A loading indicator is displayed with appropriate message
   
2. During ZIP generation:
   - Temporary directory is created for organizing files
   - Each export type (PDF, CSV, Grid) is generated
   - Files are added to the ZIP archive in their respective subdirectories
   
3. After completion:
   - Temporary files are cleaned up
   - Success message is displayed with the file path
   - User is offered the option to share the ZIP file

## Error Handling

- Proper error handling for permission issues
- Recovery from out-of-memory conditions
- Validation to ensure exported files exist and are valid
- User feedback for all error conditions

## Performance Considerations

- ZIP generation is performed on a background thread
- Large exports show progress updates
- Memory usage is optimized by cleaning up temporary files

## Future Enhancements

Potential future improvements:
- Add option to select which export types to include
- Add password protection for ZIP files
- Implement cloud backup integration
- Add export customization options 