# RecordApp Developer Guide

## Overview

RecordApp is a comprehensive Android expense tracking application built with modern Android development practices. This guide will help you understand the codebase structure and how to work with it effectively.

## Architecture

### MVVM Pattern
The app follows the Model-View-ViewModel (MVVM) architecture pattern:
- **Model**: Data classes and entities (`model/` package)
- **View**: Composable UI components (`ui/` package)
- **ViewModel**: Business logic and state management (`viewmodel/` package)

### Key Technologies
- **UI**: Jetpack Compose
- **Database**: Room with SQLite
- **Dependency Injection**: Hilt
- **Image Loading**: Coil
- **Networking**: Supabase client
- **Background Work**: WorkManager
- **Navigation**: Compose Navigation

## Codebase Structure

### Core Packages

#### `/data`
- **Purpose**: Database setup and data access objects
- **Key Files**:
  - `AppDatabase.kt` - Room database configuration
  - `Converters.kt` - Type converters for Room
  - `dao/ExpenseDao.kt` - Data access operations

#### `/model`
- **Purpose**: Data classes and entities
- **Key Files**:
  - `Expense.kt` - Main expense data model
  - `ExpenseEntity.kt` - Room database entity
  - `User.kt` - User data model

#### `/repository`
- **Purpose**: Data layer abstraction
- **Key Files**:
  - `ExpenseRepository.kt` - Expense data operations
  - `AuthRepository.kt` - Authentication operations

#### `/viewmodel`
- **Purpose**: Business logic and UI state management
- **Key Files**:
  - `ExpenseViewModel.kt` - Main expense operations
  - `AuthViewModel.kt` - Authentication logic

#### `/ui`
- **Purpose**: User interface components
- **Structure**:
  ```
  ui/
  ├── components/
  │   ├── common/          # Reusable components
  │   └── [specific components]
  ├── screens/             # Full-screen composables
  ├── navigation/          # Navigation setup
  └── theme/               # UI theming
  ```

#### `/util`
- **Purpose**: Utility classes and helper functions
- **Key Files**:
  - `FileUtils.kt` - File operations
  - `PdfUtils.kt` - PDF generation
  - `CsvUtils.kt` - CSV export
  - `SettingsManager.kt` - App settings

## Key Features

### 1. Expense Management
- **Create**: Capture expenses with images
- **Edit**: Modify expense details including date/time
- **Organize**: Folder-based organization
- **Search**: Find expenses by various criteria

### 2. Image Handling
- **Capture**: Camera integration with cropping
- **Storage**: Efficient image storage and loading
- **Management**: Reorder and organize images

### 3. Export Capabilities
- **PDF**: Generate formatted PDF reports
- **CSV**: Export data for spreadsheet analysis
- **ZIP**: Combined exports with images

### 4. Backup & Sync
- **Local Backup**: Create local backup files
- **Cloud Sync**: Supabase integration for cloud storage
- **Restore**: Restore from backup files

## Development Guidelines

### 1. Adding New Features

#### UI Components
1. **Check Existing Components**: Look in `ui/components/common/` first
2. **Create Reusable Components**: Place in `common/` if used in multiple places
3. **Follow Naming Conventions**: Use descriptive, clear names
4. **Use Material Design**: Follow Material 3 guidelines

#### Data Operations
1. **Repository Pattern**: Add operations to appropriate repository
2. **ViewModel Integration**: Update ViewModels for UI state management
3. **Database Changes**: Update entities and DAOs as needed

### 2. Code Organization

#### Package Guidelines
- **Single Responsibility**: Each class should have one clear purpose
- **Logical Grouping**: Related functionality should be in the same package
- **Clear Dependencies**: Minimize circular dependencies

#### Naming Conventions
- **Classes**: PascalCase (e.g., `ExpenseViewModel`)
- **Functions**: camelCase (e.g., `updateExpense`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `DEFAULT_FOLDER`)
- **Composables**: PascalCase (e.g., `ExpenseDetailScreen`)

### 3. Best Practices

#### State Management
```kotlin
// Use remember for local UI state
var isEditing by remember { mutableStateOf(false) }

// Use ViewModel for business logic state
val uiState by viewModel.uiState.collectAsState()

// Use keys for remember when needed
var tempData by remember(expense.id) { mutableStateOf(expense.data) }
```

#### Composable Design
```kotlin
@Composable
fun MyComponent(
    data: DataClass,
    onAction: (ActionType) -> Unit,
    modifier: Modifier = Modifier
) {
    // Component implementation
}
```

#### Error Handling
```kotlin
try {
    // Operation
} catch (e: Exception) {
    Log.e(TAG, "Error description", e)
    // User-friendly error handling
}
```

## Common Tasks

### 1. Adding a New Screen
1. Create composable in `ui/screens/`
2. Add navigation route in `ui/navigation/`
3. Create ViewModel if needed
4. Update navigation graph

### 2. Adding Database Fields
1. Update entity in `model/`
2. Update DAO queries in `data/dao/`
3. Create database migration
4. Update repository methods

### 3. Adding Export Features
1. Add utility functions in appropriate `util/` file
2. Update ViewModel with new export logic
3. Add UI controls in relevant screen
4. Test with various data scenarios

## Testing

### Unit Tests
- Test ViewModels and repositories
- Mock dependencies using Hilt testing
- Focus on business logic

### UI Tests
- Test user interactions
- Verify navigation flows
- Test with different screen sizes

## Debugging

### Common Issues
1. **Build Errors**: Check import statements and dependencies
2. **UI Issues**: Verify Composable state management
3. **Data Issues**: Check database migrations and entity definitions

### Debugging Tools
- **Logcat**: Use consistent TAG naming
- **Database Inspector**: View Room database contents
- **Layout Inspector**: Debug Compose UI hierarchy

## Performance Considerations

### Image Loading
- Use Coil for efficient image loading
- Implement proper image caching
- Consider image compression for storage

### Database Operations
- Use Flow for reactive data
- Implement proper indexing
- Consider pagination for large datasets

### Memory Management
- Monitor memory usage during image operations
- Implement proper cleanup in ViewModels
- Use appropriate image sizes

---

**Last Updated**: January 2025  
**Version**: Current  
**Status**: Active Development
