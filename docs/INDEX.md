# RecordApp Documentation Index

This document provides links to all documentation files for the RecordApp project.

## Project Overview
- [README.md](../README.md) - Main project overview, features and setup
- [Project Documentation](./PROJECT_DOCUMENTATION.md) - Detailed project documentation

## Organization & Structure
- [Project Structure](./PROJECT_STRUCTURE.md) - Codebase structure and organization guidelines
- [Cleanup Summary](./CLEANUP_SUMMARY.md) - Summary of code cleanup efforts
- [Duplicates Guide](./DUPLICATES_GUIDE.md) - Guide to identifying and resolving duplicate files

## User Documentation
- [User Guide](./USER_GUIDE.md) - Instructions for end-users
- [Authentication Guide](./AUTHENTICATION_GUIDE.md) - Details on the authentication system
- [FAQ](./FAQ.md) - Frequently asked questions

## Technical Documentation
- [Architecture Overview](./ARCHITECTURE.md) - Details on application architecture
- [API Documentation](./API_DOCUMENTATION.md) - API documentation
- [Build Configuration](./BUILD_CONFIG.md) - Build and deployment information
- [Backup System Improvements](./BACKUP_SYSTEM_IMPROVEMENTS.md) - Details of backup system optimization

## Development Guides
- [Development Guide](./DEVELOPMENT_GUIDE.md) - Guide for developers working on the project
- [Development Setup](./DEVELOPMENT_SETUP.md) - How to set up the development environment
- [UI Components](./UI_COMPONENTS.md) - Documentation for UI components
- [Testing Guide](./TESTING_GUIDE.md) - Information on testing the application

## Recent Updates
- [Changelog](./CHANGELOG.md) - History of changes to the project

## Project Documentation

- [Authentication Guide](./AUTH_GUIDE.md) - Guide to the Supabase authentication implementation
- [Backup System Improvements](./BACKUP_SYSTEM_IMPROVEMENTS.md) - Details of backup system optimization and storage issue resolution

## User Documentation

- [FAQ](./FAQ.md) - Frequently asked questions

## Developer Documentation

- [Development Setup](./DEVELOPMENT_SETUP.md) - How to set up the development environment
- [API Documentation](./API_DOCUMENTATION.md) - Documentation of the API endpoints

## Feature Documentation

- [Expense Management](./EXPENSE_MANAGEMENT.md) - Documentation for the expense management feature
- [PDF Export](./PDF_EXPORT.md) - Documentation for the PDF export feature
- [CSV Export](./CSV_EXPORT.md) - Documentation for the CSV export feature
- [Image Management](./IMAGE_MANAGEMENT.md) - Documentation for the image management feature
- [OCR Integration](./OCR_INTEGRATION.md) - Documentation for the OCR integration
- [Backup & Restore System](./BACKUP_SYSTEM_IMPROVEMENTS.md) - Documentation for the optimized backup and restore system

## Cleanup & Maintenance Documentation

- [Cleanup Report](./CLEANUP_REPORT.md) - Detailed report of the cleanup process and changes made
- [Cleanup Final](./CLEANUP_FINAL.md) - Final notes on cleanup completion and remaining tasks
- [Compilation Fixes](./compile-fixes.md) - Specific fixes applied to resolve compilation issues
- [Crash Fix Report](./CRASH_FIX_REPORT.md) - Details about fixes applied to resolve application crashes

## Directory Structure

The RecordApp project follows a clean architecture pattern with the following main directories:

```
RecordApp/
├── app/                      # Main application module
│   └── src/
│       └── main/
│           ├── java/        # Kotlin/Java source code
│           │   └── com/example/recordapp/
│           │       ├── data/       # Database, DAOs, and data models
│           │       ├── di/         # Dependency injection modules
│           │       ├── model/      # Domain models and entities
│           │       ├── network/    # Network components
│           │       ├── repository/ # Data repositories
│           │       ├── ui/         # UI components and screens
│           │       ├── util/       # Utility classes
│           │       └── viewmodel/  # ViewModels for UI logic
│           └── res/         # Android resources
├── docs/                    # Project documentation
├── gradle/                  # Gradle configuration
└── archived_cleanup/        # Archived cleanup scripts and logs
``` 