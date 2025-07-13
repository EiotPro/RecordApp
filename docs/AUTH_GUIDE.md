# Authentication Guide for RecordApp

## Overview
RecordApp uses Supabase for authentication and user management. This guide explains how to use the authentication features and troubleshoot common issues.

## Authentication Methods

RecordApp supports the following authentication methods:
- Email/password login
- Email/password signup
- User profile management

## Using the Authentication System

### Login
To login, users should enter their email and password on the login screen. The app will validate the credentials against the Supabase backend.

### Signup
To create a new account, users should provide:
- Email address
- Password (minimum 6 characters)
- Name (optional)

### Logout
Users can logout by tapping the logout button in the profile screen. This will clear the session and return to the login screen.

## Implementation Details

### Key Files
- `SupabaseClient.kt`: Wrapper for the Supabase SDK that handles authentication and other Supabase features
- `AuthRepository.kt`: Repository layer that interfaces with the SupabaseClient
- `AuthViewModel.kt`: ViewModel that exposes authentication operations to the UI
- `LoginScreen.kt`: UI for login
- `SignupScreen.kt`: UI for signup

### Authentication Flow
1. User enters credentials on login/signup screen
2. ViewModel calls repository method
3. Repository uses SupabaseClient to authenticate with Supabase
4. On successful authentication, the user session is stored and the main app UI is displayed

## Supabase Configuration

### Project Setup
The Supabase configuration is set in the `SupabaseClient.kt`:

```kotlin
private const val SUPABASE_URL = "https://vuozasjtlcjxbsqkhfch.supabase.co"
private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ1b3phc2p0bGNqeGJzcWtoZmNoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDgxNzE4MzksImV4cCI6MjA2Mzc0NzgzOX0.d3E7JA6AEgQhDqTmLT_zlGwDV-WyuuXOPahVb_ds94o"
```

### Required Dependencies
The following dependencies are required for Supabase authentication to work:

```kotlin
// Supabase
implementation("io.github.jan-tennert.supabase:supabase-kt:2.0.0")
implementation("io.github.jan-tennert.supabase:gotrue-kt:2.0.0")
implementation("io.github.jan-tennert.supabase:realtime-kt:2.0.0")
implementation("io.github.jan-tennert.supabase:storage-kt:2.0.0")
implementation("io.github.jan-tennert.supabase:postgrest-kt:2.0.0")
implementation("io.github.jan-tennert.supabase:functions-kt:2.0.0")

// Ktor client engines for Supabase
implementation("io.ktor:ktor-client-cio:2.3.7")
implementation("io.ktor:ktor-client-core:2.3.7")
```

## Troubleshooting

### Common Issues

#### App Crashes on Startup
If the app crashes immediately after launch, check the following:

1. **Supabase Initialization**: Ensure SupabaseClient is correctly initialized:

```kotlin
val client = createSupabaseClient(
    supabaseUrl = SUPABASE_URL,
    supabaseKey = SUPABASE_KEY
) {
    install(Auth)
    install(Postgrest)
    install(Realtime)
    install(Storage)
}
```

2. **HTTP Client Engine**: Make sure the correct Ktor HTTP client engine dependency is included:
   
```kotlin
implementation("io.ktor:ktor-client-cio:2.3.7")
```

3. **Important**: With Supabase 2.0.0, do not try to explicitly specify the HTTP engine in code, as it's not supported in this API version. The library will automatically use any available engine in the classpath.

#### Authentication Failures
If authentication fails even with correct credentials:

1. Verify the Supabase URL and key in SupabaseClient.kt
2. Check network connectivity to the Supabase server
3. Verify that the user exists in the Supabase authentication system

#### Session Management Issues
If users are unexpectedly logged out:

1. Check that session storage is working correctly
2. Verify the token refresh mechanism is operational
3. Check for session timeout settings in Supabase

#### Profile Data Not Syncing
If user profile data isn't updating correctly:

1. Verify that the profiles table exists in Supabase
2. Check that database rules allow profile updates
3. Ensure the proper RLS policies are set up in Supabase

## Recent Fixes

### App Crash on Startup (Fixed May 2025)
The app was crashing on startup due to improper HTTP client engine configuration for Supabase. 

**Issue**: While the correct dependency (`io.ktor:ktor-client-cio:2.3.7`) was included, the SupabaseClient was trying to explicitly configure the engine using an API that's not supported in Supabase 2.0.0.

**Fix**: Removed the explicit HTTP engine configuration in SupabaseClient.kt:

```kotlin
// INCORRECT - causes crash:
val client = createSupabaseClient(
    supabaseUrl = SUPABASE_URL,
    supabaseKey = SUPABASE_KEY,
    httpEngine = CIO  // This parameter doesn't exist in Supabase 2.0.0
) { ... }

// CORRECT - works with Supabase 2.0.0:
val client = createSupabaseClient(
    supabaseUrl = SUPABASE_URL,
    supabaseKey = SUPABASE_KEY
) {
    install(Auth)
    install(Postgrest)
    install(Realtime)
    install(Storage)
}
```

With Supabase 2.0.0, the library automatically uses the available HTTP engine on the classpath without requiring explicit configuration.

This fix resolved the app crash on startup and allowed the authentication system to initialize properly.

## Security Recommendations

1. **Token Storage**: User tokens are stored securely using EncryptedSharedPreferences
2. **Session Timeouts**: Sessions are automatically refreshed but will eventually time out for security
3. **Password Requirements**: Enforce strong password requirements in the UI validation
4. **Error Messages**: Use generic error messages to avoid exposing sensitive information

## Supabase Setup

### Credentials

- **Project URL:** `https://vuozasjtlcjxbsqkhfch.supabase.co`
- **Public anon key:**
  ```
  eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ1b3phc2p0bGNqeGJzcWtoZmNoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDgxNzE4MzksImV4cCI6MjA2Mzc0NzgzOX0.d3E7JA6AEgQhDqTmLT_zlGwDV-WyuuXOPahVb_ds94o
  ```
- **Redirect URL:** `com.example.recordapp://login-callback/`

### Database Tables

The authentication system relies on the following Supabase tables:

1. **auth.users** - Managed by Supabase Auth (not directly accessible)
2. **public.profiles** - Custom table for user profiles with the following structure:
   - `id` (UUID, primary key, references auth.users.id)
   - `name` (text)
   - `email` (text)
   - `created_at` (timestamp)
   - `last_login_time` (timestamp)
   - `role` (text, default: 'user')
   - `is_admin` (boolean, default: false)

### Supabase Triggers

The app relies on two Supabase database triggers:

1. **handle_new_user** - Creates a profile entry when a new user signs up
2. **handle_user_login** - Updates the last_login_time when a user logs in

## Row Level Security (RLS) Policies

Supabase uses Row Level Security (RLS) to control access to data. The following policies are implemented:

1. **profiles table**:
   - Users can read their own profile
   - Users can update their own profile
   - Admins can read all profiles
   - Admins can update all profiles

2. **expenses table**:
   - Users can read their own expenses
   - Users can create/update/delete their own expenses
   - Admins can read all expenses
   - Admins can update all expenses

## Testing Authentication

To test the authentication system:

1. **Create a test user**:
   - Email: test@example.com
   - Password: testpassword
   - Name: Test User

2. **Test login**:
   - Use the credentials above to log in
   - Verify that the user is redirected to the home screen

3. **Test logout**:
   - Click the logout button in the settings screen
   - Verify that the user is redirected to the login screen

4. **Test admin access** (if you have admin credentials):
   - Log in with admin credentials
   - Verify that you can see all users' data

## Troubleshooting

### Common Issues

1. **Authentication failed**:
   - Check if the email and password are correct
   - Verify that the Supabase URL and anon key are correct
   - Check the internet connection

2. **Profile not created after signup**:
   - Verify that the handle_new_user trigger is working correctly
   - Check the Supabase logs for any errors

3. **Last login time not updated**:
   - Verify that the handle_user_login trigger is working correctly
   - Check the Supabase logs for any errors

4. **Missing HTTP Client Engine**:
   - Ensure the `io.ktor:ktor-client-cio:2.3.7` dependency is included in your project dependencies
   - Verify that the SupabaseClient is correctly configured to use the available HTTP engine

- Enable debug logging in SupabaseClient.kt to see detailed authentication logs
- Check the Android Logcat for any authentication-related errors
- Use the Supabase dashboard to verify user creation and profile updates

## Security Considerations

- The app never stores passwords locally
- All authentication is handled by Supabase's secure authentication system
- User sessions are managed by Supabase and stored securely
- Sensitive operations are protected by Row Level Security policies

## Future Improvements

- Add social login (Google, Facebook, etc.)
- Implement password reset functionality
- Add email verification
- Enhance user profile management
- Implement multi-factor authentication 