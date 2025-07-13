# RecordApp

## Project Overview
RecordApp is an Android application for recording and managing expenses. It uses modern Android development practices with Jetpack Compose for UI and follows a clean architecture approach.

## Documentation

All project documentation is organized in the `docs/` directory:
- [Documentation Index](./docs/INDEX.md) - Complete list of all documentation files
- [Project Documentation](./docs/PROJECT_DOCUMENTATION.md) - Overview of the project and recent changes
- [User Guide](./docs/USER_GUIDE.md) - Instructions for end-users

## Recent Updates

### UI Improvements for Dashboard Screen
- **Enhanced User Interface**:
  - Optimized header layout with settings icon integrated into the title banner
  - Reduced unnecessary vertical spacing for better screen real estate usage
  - Improved visual hierarchy and information accessibility
  - Fine-tuned element positioning for better user experience

### Backup System Optimization and Storage Issue Resolution
- **Complete Storage Analysis and Cleanup**:
  - Added functionality to analyze app storage usage by category
  - Implemented cleanup system to remove residual files causing storage bloat
  - Fixed storage size discrepancy between app-reported usage and actual data
  - Added detailed storage metrics logging during backup operations
- **Enhanced Backup System**:
  - Restructured backup format with logical folder organization (database, preferences, images, etc.)
  - Improved ZIP compression with proper file filtering to exclude unnecessary files
  - Implemented proper cleanup of temporary files after backup operations
  - Added detailed progress tracking and size reporting during backup process
- **Optimized Restore Mechanism**:
  - Enhanced restore functionality to properly handle the new folder-based structure
  - Added proper database reconnection after restore to ensure data consistency
  - Improved restore progress reporting with detailed logs
  - Enhanced error handling and validation during restore operations

### Authentication System Fixes and Improvements
- **Supabase Authentication Fixes**:
  - Updated authentication methods to match Supabase SDK 2.0.0 API
  - Replaced deprecated `signUpEmailPassword` with `signUpWith(Email)` 
  - Replaced deprecated `signInEmailPassword` with `signInWith(Email)`
  - Fixed potential memory leak by removing Context from static fields
  - Improved singleton pattern implementation for SupabaseClient
  - Enhanced error handling in authentication flows

### Authentication System Implementation
- **Integrated Supabase Authentication**:
  - Implemented secure login, signup, and logout functionality
  - Added profile management with Supabase profiles table integration
  - Set up proper user role management (user/admin)
  - Configured automatic profile creation via Supabase triggers
  - Added last login time tracking

### Code Cleanup and Optimization
- **Consolidated utility classes**:
  - Merged PDF utility classes (PdfGenerator, PdfExporter) into PdfUtils
  - Merged CSV utility classes (CsvGenerator, CsvExporter) into CsvUtils
  - Combined backup-related classes into a unified BackupModule
- **Optimized dependencies**: 
  - Added direct declarations for transitive dependencies
  - Improved dependency management
- **Fixed compilation issues**:
  - Updated method references after class consolidation
  - Fixed variable declarations and parameter references
  - Added missing implementations for required functionality
- **Code quality improvements**:
  - Eliminated unused imports and constants
  - Improved constructor injection for better testability
  - Fixed singleton implementation to prevent memory leaks

See the [Cleanup Summary](./docs/CLEANUP_SUMMARY.md) for more details.

## Key Technical Specifications

### Development Environment
- **Gradle Version**: 8.11.1
- **Android Gradle Plugin**: 8.10.0
- **Kotlin Version**: 2.0.21
- **Compile SDK**: 35
- **Min SDK**: 24
- **Target SDK**: 35

### Core Dependencies
- **UI Framework**: Jetpack Compose
- **Database**: Room 2.6.1
- **Authentication**: Supabase Auth 2.0.0
- **Image Loading**: Coil 2.5.0
- **Navigation**: Navigation Compose 2.7.7
- **PDF Generation**: iText 7.2.5
- **Text Recognition**: ML Kit
- **Background Processing**: WorkManager 2.9.0

## Project Structure

### App Architecture
The app follows MVVM (Model-View-ViewModel) architecture with Clean Architecture principles:
- **Data Layer**: Room database, DAOs
- **Model Layer**: Entity and domain models
- **Repository Layer**: Data access logic
- **ViewModel Layer**: Business logic
- **UI Layer**: Compose UI components and screens

### Directory Organization
The project follows a structured directory organization for better maintainability:

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

### Main Components

1. **Database**:
   - Room database with ExpenseEntity as the main data model
   - Uses TypeConverters for handling complex types

2. **Authentication System**:
   - Secure login/signup with Supabase Auth
   - User role management (user/admin)
   - Profile management via Supabase profiles table

3. **Features**:
   - Expense recording with gallery integration
   - Dashboard analytics
   - Expense listing with filtering options
   - PDF and CSV export capabilities
   - OCR text recognition from images

4. **Navigation**:
   - Implements Navigation Compose
   - Screen destinations: Home, Expenses, Settings, Profile, Login, Signup

## Key Features

### User Authentication
- Secure signup and login with email/password
- User profiles with roles (user/admin)
- Automatic profile creation on signup
- Last login time tracking

### Expense Record Management
- Create, view, and manage expense records
- Store important details like date, amount, and description
- Attach images to expense records

### Export & Sharing
- Export expense records with images as PDF files
- Multiple PDF layout options:
  - Individual (one image per page)
  - Grid layouts (multiple images per page)
- Export data to CSV format

### Image Management
- Import images from device gallery
- Image ordering system for PDF exports
- OCR to extract text from receipt images

## Build Configuration
- Version catalog (libs.versions.toml) for dependency management
- Properly configured Kotlin options
- Optimized release build with minification and resource shrinking

## Requirements
- Android 7.0 (API Level 24) or higher
- Storage permission for saving PDFs and images

## Development Guidelines

### Running the Project
```bash
# Clone the repository
git clone [repository-url]

# Open in Android Studio
# Build and run on an Android device or emulator
```

### Code Quality Checks
```bash
# Run Android Lint
./gradlew lintDebug

# Check for unused dependencies
./gradlew buildHealth
```

### Fixing Common Issues
- **Duplicate Files**: Use Android Studio's "Find Duplicates" feature (Right-click on project folder → Find → Find Duplicates)
- **Resolve Conflicts**: When merging branches, carefully review conflicts to prevent duplicated code
- **Sync Project**: Use "Sync Project with Gradle Files" after making build configuration changes
- **Clean Project**: Run "Clean Project" to resolve build issues when needed

## Authentication Setup

### Supabase Configuration
- **Project URL**: `https://vuozasjtlcjxbsqkhfch.supabase.co`
- **Public anon key**: Used for anonymous access to Supabase services
- **Redirect URL**: `com.example.recordapp://login-callback/`

### User Authentication Flow
1. **Signup**:
   - User enters email, password, and name
   - Application uses `signUpWith(Email)` to create new account
   - Supabase creates a new auth user
   - Trigger `handle_new_user` creates entry in profiles table
   - User is automatically logged in

2. **Login**:
   - User enters email and password
   - Application uses `signInWith(Email)` to authenticate
   - Supabase authenticates credentials
   - Trigger `handle_user_login` updates last login time
   - User session is created

3. **Logout**:
   - User session is terminated
   - App returns to login screen

### Authentication Implementation Details
- **SupabaseClient**: Singleton implementation with proper resource management
- **AuthRepository**: Uses constructor injection for better testability
- **Error Handling**: Improved error messages for authentication failures
- **Session Management**: Properly maintains user session state 