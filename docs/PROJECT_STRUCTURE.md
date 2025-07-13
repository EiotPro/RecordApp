# RecordApp Project Structure

## Overview
This document outlines the organization of the RecordApp project and provides guidelines for maintaining a clean and efficient codebase structure.

## Directory Structure

The project follows a structured organization based on the MVVM (Model-View-ViewModel) architecture with Clean Architecture principles:

```
RecordApp/
├── app/                        # Main application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/recordapp/
│   │   │   │   ├── data/       # Data layer components
│   │   │   │   │   ├── db/     # Room database and DAOs
│   │   │   │   │   ├── model/  # Entity models
│   │   │   │   │   └── repo/   # Repositories
│   │   │   │   ├── ui/         # UI layer
│   │   │   │   │   ├── components/  # Reusable UI components
│   │   │   │   │   ├── navigation/  # Navigation-related classes
│   │   │   │   │   ├── screens/     # Screen composables
│   │   │   │   │   └── theme/       # Theme and styling
│   │   │   │   ├── util/       # Utility classes
│   │   │   │   └── viewmodel/  # ViewModel classes
│   │   │   ├── res/            # Resources
│   │   │   └── AndroidManifest.xml
│   │   └── test/               # Unit tests
│   └── build.gradle            # App-level build script
├── docs/                       # Documentation
├── gradle/                     # Gradle configuration
└── README.md                   # Project readme
```

## Package Organization

### Model Layer
- `model/`: Contains data classes and entity definitions for the application
  - All database entities should be suffixed with `Entity`
  - DTOs (Data Transfer Objects) should be suffixed with `Dto`
  - Domain models should use clean names without suffixes

### Data Layer
- `data/db/`: Room database implementation
  - Contains `AppDatabase` class and all DAOs
  - DAOs should be suffixed with `Dao`
- `data/repo/`: Repository implementations
  - Repositories should be suffixed with `Repository`
  - Each repository should have a corresponding interface

### UI Layer
- `ui/components/`: Reusable UI components
  - Should be stateless when possible
  - Keep complex logic in ViewModels, not components
- `ui/screens/`: Full screen composables
  - Each screen should be in its own file
  - Screen files should be suffixed with `Screen`
- `ui/navigation/`: Navigation configuration
- `ui/theme/`: Theme-related classes

### Utility Layer
- `util/`: Helper classes and utility functions
  - Group related functionality into focused utility classes
  - Use object (singleton) pattern for utility classes
  - Each utility class should have a specific purpose

### ViewModel Layer
- `viewmodel/`: ViewModels for the application
  - ViewModels should be suffixed with `ViewModel`
  - Each ViewModel should focus on related functionality

## File Naming Conventions

1. **Kotlin Files**: Use PascalCase for file names (e.g., `ExpenseEntity.kt`, `DashboardScreen.kt`)
2. **XML Files**: Use snake_case for resource files (e.g., `activity_main.xml`, `list_item_expense.xml`)
3. **Class Names**: Use PascalCase for class names that match their file names

## Guidelines for Code Organization

### Preventing Duplicates
1. **Check Existing Code**: Before creating new utility methods or components, check if similar functionality already exists
2. **Use Android Studio's Search**: Use "Find in Path" to search for related functionality
3. **Follow the DRY Principle**: Don't Repeat Yourself - extract common code into reusable functions

### Identifying and Resolving Duplicates
1. **Find Duplicates Tool**: Use Android Studio's "Find Duplicates" feature
   - Right-click on the project → Find → Find Duplicates
   - Review and consolidate duplicate code

2. **Duplicate File Detection**:
   - Look for files with similar names or functionality
   - Check for files that may have been generated multiple times
   - Verify imports to ensure you're using the correct classes

### Improving File Organization
1. **Group Related Files**: Keep related functionality together in appropriate packages
2. **Move Classes to Proper Packages**: If a class is in the wrong package, refactor it to the correct location
3. **Split Large Files**: Break down large files into smaller, focused components

## Best Practices for Maintaining Clean Structure

1. **Regular Code Reviews**: Conduct regular code reviews to ensure proper organization
2. **Refactor When Needed**: Don't hesitate to refactor code when its organization can be improved
3. **Document Structure Changes**: Update this document when making significant structural changes
4. **Use Dependency Injection**: Maintain clean dependencies between components
5. **Follow Single Responsibility Principle**: Each class should have a single responsibility

## Component Structure Guide

### Screens
Each screen should follow this general structure:
```kotlin
@Composable
fun ScreenName(
    // Parameters
) {
    // State declarations
    // Other setup logic
    
    // UI components
}
```

### ViewModels
ViewModels should follow this pattern:
```kotlin
class FeatureViewModel(
    // Dependencies
) : ViewModel() {
    // State
    // Event handlers
    // Business logic
}
```

## File Management and Clean-up

### When to Delete Files
- Only delete files when you are certain they are no longer used
- Check for references before deleting any file
- When replacing functionality, ensure the new implementation is fully tested

### How to Handle Deprecated Files
- Mark deprecated classes/methods with the `@Deprecated` annotation
- Include a reason and suggested alternative
- Consider retaining deprecated code during a transition period before removal

### Tracking Changes
- Document significant structural changes in the `CHANGELOG.md` file
- Update relevant documentation when changing the project structure 