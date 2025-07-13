# RecordApp Maintenance Scripts

This document provides information about the maintenance scripts available in the RecordApp project to help keep the codebase clean and organized.

## Available Scripts

### setup-hooks.ps1

This script sets up Git hooks for the project.

**Usage:**
```powershell
.\setup-hooks.ps1
```

**What it does:**
- Configures Git to use hooks from the `.githooks` directory
- Ensures the pre-commit hooks are executable (for non-Windows environments)
- Verifies the setup was successful

### cleanup-project.ps1

This script performs various cleanup operations on the project.

**Usage:**
```powershell
.\cleanup-project.ps1
```

**What it does:**
- Checks for duplicate UI components and offers to remove them
- Cleans build files using Gradle
- Cleans Android cache using clean-android-cache.ps1
- Removes Java error logs and replay logs
- Runs lint to identify unused resources

### clean-android-cache.ps1

This script cleans Android-specific cache files.

**Usage:**
```powershell
.\clean-android-cache.ps1 -Force
```

**Parameters:**
- `-Force`: Skip confirmation prompt and proceed with cleanup immediately

**What it does:**
- Removes temporary Android build files
- Clears Gradle cache
- Removes build directories
- Deletes Java error logs and replay logs

## Pre-commit Hooks

The pre-commit hook in `.githooks/pre-commit.ps1` performs the following checks before allowing a commit:

1. Checks for duplicate UI components in the wrong locations
2. Runs lint checks to identify code quality issues
3. Ensures navigation components are properly set up

## When to Use These Scripts

- **setup-hooks.ps1**: When setting up the project for the first time or after a fresh clone
- **cleanup-project.ps1**: Periodically to clean up the project and before major releases
- **clean-android-cache.ps1**: When experiencing build issues or when the cache gets too large

## Best Practices

1. Run `setup-hooks.ps1` after cloning the repository to ensure code quality checks are in place
2. Before submitting a pull request, run `cleanup-project.ps1` to ensure your code is clean
3. If you encounter build issues, try running `clean-android-cache.ps1` to clear cached files 