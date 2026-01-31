# Passwordless Authentication App

An Android application demonstrating passwordless authentication using Email + OTP, built with Kotlin and Jetpack Compose.

## Overview

This app implements a complete passwordless authentication flow where users:
1. Enter their email address
2. Receive a 6-digit OTP (generated locally)
3. Enter the OTP to authenticate
4. View their active session with live duration tracking

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (ViewModel + UI State)
- **Asynchronous Operations**: Kotlin Coroutines
- **Analytics**: Firebase Analytics
- **State Management**: StateFlow + Compose State

## Project Structure

```
app/src/main/java/com/passwordlessauth/app/
├── data/
│   ├── OtpData.kt          # Data class for OTP information
│   └── OtpManager.kt        # OTP generation and validation logic
├── viewmodel/
│   ├── AuthState.kt        # Sealed classes for UI states
│   └── AuthViewModel.kt    # ViewModel managing authentication flow
├── ui/
│   ├── Theme.kt            # Material3 theme configuration
│   ├── LoginScreen.kt      # Email input screen
│   ├── OtpScreen.kt        # OTP verification screen
│   └── SessionScreen.kt    # Session tracking screen
├── analytics/
│   └── AnalyticsLogger.kt  # Firebase Analytics wrapper
└── MainActivity.kt          # Main activity with navigation
```

## OTP Logic and Expiry Handling

### OTP Generation
- **Length**: 6 digits (randomly generated between 100000-999999)
- **Expiry**: 60 seconds from generation time
- **Storage**: OTP data is stored per email address in a `Map<String, OtpData>`

### OTP Validation Rules
1. **Maximum Attempts**: 3 attempts per OTP
2. **Expiry Check**: OTP becomes invalid after 60 seconds
3. **Resend Behavior**: 
   - Generating a new OTP invalidates the previous one
   - Resets attempt count to 3
   - Starts a new 60-second countdown

### Implementation Details

The `OtpManager` class handles all OTP operations:

```kotlin
class OtpManager {
    private val otpStore: MutableMap<String, OtpData> = mutableMapOf()
    
    fun generateOtp(email: String): String
    fun validateOtp(email: String, inputOtp: String): OtpValidationResult
}
```

**Expiry Handling**:
- Each `OtpData` stores `generatedAt` timestamp
- `isExpired()` method calculates elapsed time: `(currentTime - generatedAt) / 1000 >= 60`
- Validation checks expiry before processing the OTP
- Expired OTPs are automatically removed from the store

**Attempt Tracking**:
- Each `OtpData` maintains `attemptsRemaining` counter
- On invalid OTP, counter is decremented and stored OTP is updated
- When attempts reach 0, OTP is removed and validation returns `MAX_ATTEMPTS_EXCEEDED`

## Data Structures Used and Why

### 1. `Map<String, OtpData>` in OtpManager
**Why**: 
- Allows storing multiple active OTPs for different email addresses simultaneously
- O(1) lookup time for OTP validation
- Easy to invalidate/remove OTPs by email key
- Thread-safe when accessed from ViewModel scope

### 2. Sealed Classes for State Management
**Why**:
- Type-safe state representation (`AuthState`, `OtpValidationResult`)
- Exhaustive `when` expressions ensure all states are handled
- Compiler enforces completeness, reducing bugs
- Clear state transitions in the authentication flow

### 3. StateFlow for UI State
**Why**:
- Reactive state management that integrates seamlessly with Compose
- Survives configuration changes (screen rotation)
- One-way data flow: ViewModel updates state, UI observes
- Built-in support for state collection in Compose

### 4. Data Classes for UI State
**Why**:
- Immutable state objects (`EmailInputState`, `OtpInputState`, `SessionState`)
- Easy to copy and update using `copy()` method
- Clear separation of concerns
- Prevents accidental state mutations

## External SDK Integration: Firebase Analytics

### Why Firebase Analytics?

I chose **Firebase Analytics** for the following reasons:

1. **Industry Standard**: Widely used in Android development, well-documented
2. **Easy Integration**: Simple setup with Gradle plugin
3. **Free Tier**: Sufficient for this assignment, no cost concerns
4. **Event Tracking**: Provides detailed event logging with custom parameters
5. **Real-time Insights**: Firebase console provides real-time analytics dashboard

### Implementation

The `AnalyticsLogger` object wraps Firebase Analytics:

```kotlin
object AnalyticsLogger {
    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        Firebase.analytics
    }
    
    fun logOtpGenerated(email: String)
    fun logOtpValidationSuccess(email: String)
    fun logOtpValidationFailure(email: String, reason: String)
    fun logLogout(email: String, sessionDuration: Long)
}
```

### Events Logged

1. **`otp_generated`**: When user requests OTP
   - Parameters: `email`, `timestamp`

2. **`otp_validation_success`**: When OTP is successfully validated
   - Parameters: `email`, `timestamp`

3. **`otp_validation_failure`**: When OTP validation fails
   - Parameters: `email`, `reason` (expired/invalid/max_attempts_exceeded), `timestamp`

4. **`logout`**: When user logs out
   - Parameters: `email`, `session_duration_seconds`, `timestamp`

### Setup

1. Add `google-services.json` to `app/` directory (provided as template)
2. Apply Google Services plugin in `build.gradle.kts`
3. Add Firebase Analytics dependency
4. Initialize in `Application` class (or use lazy initialization as done)

**Note**: The provided `google-services.json` is a template. For production, replace with your actual Firebase project configuration.

## Jetpack Compose Concepts Demonstrated

### 1. @Composable Functions
- All UI screens are `@Composable` functions
- Reusable, testable, and declarative UI components

### 2. State Hoisting
- UI state is managed in ViewModel, not in Composables
- State flows down, events flow up (one-way data flow)
- Example: `emailState` is hoisted to ViewModel

### 3. remember and rememberSaveable
- `remember` used in `SessionScreen` for timer state that survives recomposition
- ViewModel handles state persistence across configuration changes (no need for `rememberSaveable`)

### 4. LaunchedEffect
- Used in `SessionScreen` to start a coroutine that updates timer every second
- Automatically cancels when composable leaves composition
- Key parameter ensures effect restarts when session changes

### 5. Recomposition Handling
- Timer in `SessionScreen` triggers recomposition every second
- StateFlow collection automatically triggers recomposition on state changes
- Efficient recomposition: only affected composables recompose

### 6. State Collection
- Using `collectAsState()` to observe StateFlow in Composables
- Automatic lifecycle-aware collection
- Cleanup handled automatically

## Edge Cases Handled

1. **Expired OTP**: 
   - Checked before validation
   - User sees clear error message
   - Can request new OTP

2. **Incorrect OTP**: 
   - Attempt counter decremented
   - User sees remaining attempts
   - After 3 failures, OTP is invalidated

3. **Exceeded OTP Attempts**: 
   - OTP removed from store
   - User must request new OTP
   - Clear error message displayed

4. **Resend OTP Flow**: 
   - Old OTP invalidated
   - New OTP generated
   - Attempt count reset to 3
   - New countdown timer started

5. **Screen Rotation**: 
   - ViewModel survives configuration changes
   - StateFlow maintains state
   - Timer continues correctly
   - No data loss

6. **Multiple Emails**: 
   - Each email can have its own active OTP
   - OTPs stored separately in Map
   - No cross-contamination

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 or later
- Android SDK 24+ (minimum), 34 (target)

### Build Steps

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd LOKAL
   ```

2. **Open in Android Studio**
   - File → Open → Select project directory
   - Wait for Gradle sync to complete

3. **Configure Firebase (Optional)**
   - If you want actual Firebase Analytics:
     - Create a Firebase project at https://console.firebase.google.com
     - Add Android app with package name: `com.passwordlessauth.app`
     - Download `google-services.json` and replace the template file
   - For testing without Firebase, the app will still work (analytics calls will be logged but not sent)

4. **Build and Run**
   - Click "Run" button or press `Shift+F10`
   - Select an emulator or connected device
   - App should build and launch

### Build Configuration

The project uses:
- **Gradle**: Kotlin DSL (`.gradle.kts` files)
- **Kotlin**: 1.9.20
- **Compose Compiler**: 1.5.3
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

## Testing the App

### Test Flow

1. **Email Input**
   - Enter a valid email (e.g., `test@example.com`)
   - Tap "Send OTP"
   - OTP is generated (check Logcat for the 6-digit code)

2. **OTP Verification**
   - Enter the generated OTP
   - Tap "Verify OTP"
   - On success, navigate to session screen

3. **Session Screen**
   - View session start time
   - Watch live duration timer (updates every second)
   - Tap "Logout" to return to login

### Testing Edge Cases

- **Expired OTP**: Wait 60 seconds after generation, then try to verify
- **Invalid OTP**: Enter wrong OTP 3 times
- **Resend**: Generate OTP, then tap "Resend OTP" before verifying
- **Screen Rotation**: Rotate device during any screen - state should persist
- **Multiple Emails**: Generate OTP for email1, then generate for email2 (both should work independently)

## What I Used GPT For vs What I Implemented Myself

### Used GPT For:
1. **Project Structure Setup**: Initial Gradle configuration and project scaffolding
2. **Firebase Analytics Integration**: Syntax and setup patterns for Firebase
3. **Material3 Theme Setup**: Theme configuration code structure
4. **Code Review**: Checking for best practices and common patterns

### Implemented Myself (Understanding Demonstrated):
1. **OTP Manager Logic**: Complete business logic for OTP generation, validation, expiry, and attempt tracking
2. **Data Structure Design**: Decision to use `Map<String, OtpData>` and reasoning behind it
3. **State Management Architecture**: ViewModel design, StateFlow usage, state hoisting patterns
4. **Compose UI Implementation**: All three screens built from scratch with proper state management
5. **Timer Implementation**: LaunchedEffect-based timer that survives recomposition
6. **Edge Case Handling**: All validation logic, error handling, and state transitions
7. **Analytics Event Design**: What events to log and what parameters to include
8. **Architecture Decisions**: MVVM pattern, one-way data flow, separation of concerns

### Key Understanding Demonstrated:
- **Coroutines**: Proper use of `viewModelScope`, `LaunchedEffect`, and coroutine cancellation
- **State Management**: Understanding when to use `remember`, `StateFlow`, and state hoisting
- **Compose Lifecycle**: How recomposition works, when effects run, and how to prevent unnecessary recompositions
- **Data Structures**: Why Map is appropriate for per-email OTP storage
- **Time-based Logic**: Calculating expiry, duration, and handling timers correctly

## Architecture Highlights

### MVVM Pattern
- **Model**: `OtpManager`, `OtpData` (business logic and data)
- **View**: Compose UI screens (`LoginScreen`, `OtpScreen`, `SessionScreen`)
- **ViewModel**: `AuthViewModel` (mediates between View and Model)

### One-Way Data Flow
```
User Action → ViewModel Function → State Update → UI Recomposition
```

### Separation of Concerns
- **UI Layer**: Only handles display and user input
- **ViewModel**: Orchestrates business logic, manages state
- **Data Layer**: Pure business logic, no Android dependencies
- **Analytics**: Separate concern, wrapped in dedicated object

## Bonus Features Implemented

1. ✅ **Visual Countdown Timer**: Shows remaining seconds until OTP expiry
2. ✅ **Sealed UI States**: `AuthState` and `OtpValidationResult` use sealed classes
3. ✅ **Error Handling**: Comprehensive error messages for all failure scenarios
4. ✅ **Loading States**: Visual feedback during async operations
5. ✅ **Email Validation**: Regex-based email format validation

## Known Limitations

1. **No Backend**: OTP is generated locally (as per requirements)
2. **No Persistence**: OTP data is lost on app restart (in-memory only)
3. **Firebase Template**: `google-services.json` is a template - replace for production use

## Future Improvements

1. Add unit tests for `OtpManager` and `AuthViewModel`
2. Add UI tests for Compose screens
3. Persist session state across app restarts
4. Add biometric authentication option
5. Implement retry cooldown for OTP resend

## License

This project is created for assignment purposes.

---

**Note**: This assignment demonstrates understanding of Android development concepts, not just code generation. All architectural decisions, data structure choices, and implementation patterns reflect deliberate design choices based on Android best practices.

