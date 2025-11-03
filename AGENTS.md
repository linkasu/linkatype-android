# Repository Guidelines

## Project Structure & Module Organization
Source code for the Android client lives in `app/src/main`, with UI resources under `app/src/main/res`, JVM unit tests in `app/src/test`, and instrumentation tests in `app/src/androidTest`. Shared business logic and data models are implemented in `shared/src/commonMain`, with platform-specific hooks in `shared/src/androidMain` and `shared/src/iosMain`. The iOS wrapper project resides in `iosApp/`, which consumes the `Shared.xcframework` generated from the KMP module. Gradle build logic is centralized at the root `build.gradle` and `settings.gradle`, while CI configuration relies on GitHub Actions.

## Build, Test, and Development Commands
Use `./gradlew assembleDebug` to compile a debug APK at `app/build/outputs/apk/debug/`. Run `./gradlew :shared:assembleSharedDebugXCFramework` to produce an iOS-ready framework at `shared/build/XCFrameworks/debug/Shared.xcframework`. `./gradlew test` executes the combined JVM unit test suite for `app` and `shared`, and `./gradlew lintDebug` applies Android and Kotlin lint checks. When exercising UI flows, launch an emulator and run `./gradlew connectedDebugAndroidTest`.

## Coding Style & Naming Conventions
Write Kotlin with 4-space indentation, trailing commas where helpful, and Kotlin official code style. Keep classes and composables in `PascalCase`, functions and properties in `camelCase`, constants in `UPPER_SNAKE_CASE`, and Android resources in `snake_case`. Shared logic should live in `shared/src/commonMain` whenever possible, with platform-specific implementations isolated to their respective `androidMain` or `iosMain` source sets. Prefer ViewBinding over synthetic accessors, and document non-trivial flows with concise KDoc.

## Testing Guidelines
Add unit tests alongside the code they cover: JVM tests under `app/src/test` for Android-only logic and `shared/src/commonTest` for multiplatform features. Mock asynchronous components with `kotlinx.coroutines-test` and `MockK`, and use Robolectric sparingly for Android UI behavior. Name tests with the `FunctionUnderTest_State_ExpectedOutcome` format. Always run `./gradlew test lintDebug` before opening a PR and ensure new logic has meaningful coverage.

## Commit & Pull Request Guidelines
Follow the Conventional Commits style seen in the history (`feat:`, `fix:`, `chore:`, `refactor:`) and keep each commit focused. Reference Jira/GitHub issues in the commit body when applicable. For PRs, provide a concise summary, testing notes (commands run and results), screenshots or screen recordings for UI changes, and link related issues. Ensure CI is green before requesting review and flag any follow-up work explicitly.

## Security & Configuration Tips
Firebase credentials in `app/google-services.json` are environment-specific; avoid altering them without coordination. Store private API keys in `local.properties` or environment variables, never in versioned files. When working with CI secrets, use GitHub Actions secrets and test locally with fallback mocks to protect sensitive data.
