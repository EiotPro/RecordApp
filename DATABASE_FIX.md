# Database Schema Fix

## Issue

After updating Android Studio, the app was crashing after login with the following error:

```
Room cannot verify the data integrity. Looks like you've changed schema but forgot to update the version number. 
Expected identity hash: 8f7697475ee02862ed5b37917d21f355, found: 52c7635f654836cdba96ba49c8c90b9b
```

This error occurs when there's a mismatch between the database schema and version number. The schema had changed (with the addition of new TypeConverters for List<String> in the Converters class), but the version number wasn't updated.

## Solution

1. Updated the database version from 4 to 5 in the `@Database` annotation:

```kotlin
@Database(
    entities = [ExpenseEntity::class],
    version = 5,  // Changed from 4 to 5
    exportSchema = false
)
```

2. Added a new migration from version 4 to 5 to handle the schema change:

```kotlin
/**
 * Migration from version 4 to 5 to add support for list converters
 */
private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // This migration doesn't need to modify any tables
        // It's needed because we added new TypeConverters for List<String>
        Log.i("AppDatabase", "Migration 4 to 5 completed for TypeConverter changes")
    }
}
```

3. Updated the `getInstance` method to include the new migration:

```kotlin
.addMigrations(MIGRATION_1_2, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_4)
```

## Why This Works

Room uses a hash of the database schema to verify data integrity. When you add or modify TypeConverters, the schema hash changes, and Room detects a mismatch. By incrementing the version number and providing a migration path, we tell Room that this change is intentional and provide a way to update existing databases.

## Additional Notes

- The migration doesn't need to modify any tables because the change was only to the TypeConverters.
- If users are experiencing this issue, they should update to the latest version of the app.
- In the future, remember to increment the database version whenever you modify:
  - Entity classes
  - TypeConverters
  - DAO methods that affect the database schema 