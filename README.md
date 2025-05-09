# RecordApp

RecordApp is an Android application for capturing, storing, and managing expense records with images. It allows users to:

- Capture images of receipts and expense documentation
- Extract text from images using OCR (Optical Character Recognition)
- Store expense records with details like amount, date and description
- View a list of all expenses
- Export expenses as PDF files with different layout options
- Share expense records with others

## Project Structure

```
app/
├── src/main/
│   ├── java/com/example/recordapp/
│   │   ├── model/                 # Data models
│   │   │   ├── Expense.kt         # Main expense data class
│   │   │   ├── ExpenseEntity.kt   # Database entity
│   │   │   ├── User.kt            # User data class
│   │   │   └── DashboardWidget.kt # Dashboard widget model
│   │   ├── repository/            # Data repositories
│   │   │   ├── ExpenseRepository.kt # Expense data management
│   │   │   └── AuthRepository.kt    # Authentication repository
│   │   ├── viewmodel/             # ViewModels
│   │   │   ├── ExpenseViewModel.kt  # Expense management
│   │   │   └── AuthViewModel.kt     # Authentication
│   │   ├── ui/                    # UI components
│   │   │   ├── screens/           # Full screens
│   │   │   │   ├── HomeScreen.kt
│   │   │   │   ├── ExpensesScreen.kt
│   │   │   │   ├── ExpenseDetailScreen.kt
│   │   │   │   ├── SettingsScreen.kt
│   │   │   │   ├── DashboardScreen.kt
│   │   │   │   └── ProfileScreen.kt
│   │   │   ├── components/        # Reusable UI components
│   │   │   │   ├── ExpenseList.kt
│   │   │   │   ├── PagedExpenseList.kt
│   │   │   │   ├── RecentExpenseCard.kt
│   │   │   │   ├── AnimatedWelcomeText.kt
│   │   │   │   ├── ConfirmationDialog.kt
│   │   │   │   ├── ErrorStateHandler.kt
│   │   │   │   ├── ImageCaptureDialog.kt
│   │   │   │   ├── ImageExportDialog.kt
│   │   │   │   ├── LoadingStateHandler.kt
│   │   │   │   └── DashboardWidget.kt
│   │   │   ├── navigation/        # App navigation
│   │   │   │   ├── AppNavigation.kt
│   │   │   │   └── Screen.kt
│   │   │   └── theme/             # App theming
│   │   │       ├── AppTheme.kt
│   │   │       └── RecordAppTheme.kt
│   │   ├── data/                  # Database related
│   │   │   ├── dao/               # Data Access Objects
│   │   │   │   └── ExpenseDao.kt  # DAO for expense operations
│   │   │   └── AppDatabase.kt     # Room database implementation
│   │   ├── util/                  # Utility classes
│   │   │   ├── OcrUtils.kt        # OCR functionality
│   │   │   ├── FileUtils.kt       # File operations
│   │   │   ├── PdfGenerator.kt    # PDF generation
│   │   │   ├── CsvGenerator.kt    # CSV generation
│   │   │   ├── PermissionUtils.kt # Permission handling
│   │   │   ├── AnimationUtils.kt  # Animation utilities
│   │   │   ├── UiState.kt         # UI state management
│   │   │   ├── ViewModelExt.kt    # ViewModel extensions
│   │   │   └── SettingsManager.kt # Settings management
│   │   ├── MainActivity.kt        # Entry point
│   │   └── RecordApplication.kt   # Application class
```

## Key Features

### Expense Record Management
- Create, view, and manage expense records
- Store important details like date, amount, and description
- Attach images to expense records

### Image Capture & OCR
- Capture images directly within the app
- Extract text from receipt images using OCR technology
- Automatically parse date and amount information when possible

### Export & Sharing
- Export expense records with images as PDF files
- Multiple PDF layout options:
  - Individual (one image per page)
  - 2×2 Grid (4 images per page)
  - 2×3 Grid (6 images per page)
- Share expense reports with other apps

### User Interface
- Modern Material Design UI using Jetpack Compose
- Intuitive navigation between screens
- Visual feedback for user actions
- Support for light and dark themes
- Dashboard widgets for quick access to information

### Authentication
- Simple user authentication system
- Secure credential storage using EncryptedSharedPreferences
- User profile management

## Technical Implementation

### Architecture
- MVVM (Model-View-ViewModel) architecture pattern
- Repository pattern for data access
- Room database for local storage
- Jetpack Compose for UI
- Coroutines for asynchronous operations

### Libraries & Technologies
- Jetpack Compose: UI toolkit
- Room: Database storage
- iText PDF: PDF generation
- Kotlin Coroutines: Asynchronous programming
- ML Kit OCR: Text recognition from images
- Jetpack Navigation: Navigation between screens
- EncryptedSharedPreferences: Secure data storage
- Coil: Image loading and caching

## Requirements

- Android 7.0 (Nougat, API Level 24) or higher
- Camera permission for capturing images
- Storage permission for saving PDFs and images 

## Recent Updates

### Project Cleanup and Optimization (May 2024)
- **Removed Unused Components**: 
  - Deleted empty utility files
  - Removed unused resources from root directories
  - Removed unused test directories and dependencies
- **Modernized Deprecated Code**:
  - Updated to use HorizontalDivider instead of deprecated Divider
  - Implemented proper RTL support with AutoMirrored icons
  - Enhanced status bar handling with modern WindowCompat APIs
  - Updated EncryptedSharedPreferences implementation with MasterKey
  - Improved image loading with ImageDecoder for Android P+
- **Build Configuration**:
  - Cleaned up gradle build files
  - Removed unused dependencies
  - Fixed potential resource conflicts
  - Optimized build process

## Installation

1. Clone the repository
2. Open the project in Android Studio
3. Build and run on an Android device or emulator

## License

This project is licensed under the MIT License - see the LICENSE file for details. 