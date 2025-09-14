# WristLingo - Multi-IDE Workspace Setup

This document provides instructions for setting up the WristLingo project in various IDEs. The project comes with pre-configured workspace files for seamless development across different environments.

## Supported IDEs

- **Visual Studio Code** - Recommended for cross-platform development
- **IntelliJ IDEA / Android Studio** - Native Android development experience  
- **Eclipse** - Alternative Java/Android IDE support

## Prerequisites

Before opening the project in any IDE, ensure you have:

1. **Java Development Kit (JDK) 17** or later
2. **Android SDK** with API level 36 (or latest)
3. **Android SDK Build Tools 36.0.0**
4. **Git** for version control

## Visual Studio Code Setup

### Opening the Workspace

1. Install VS Code from [code.visualstudio.com](https://code.visualstudio.com/)
2. Open VS Code
3. Click **File → Open Workspace from File**
4. Navigate to the project root and select `WristLingo.code-workspace`

### Required Extensions

The workspace will automatically recommend installing these extensions:

- **Kotlin Language** (`mathiasfrohlich.kotlin`) - Kotlin language support
- **Java Extension Pack** (`vscjava.vscode-java-pack`) - Complete Java development
- **Gradle for Java** (`vscjava.vscode-gradle`) - Gradle build integration
- **YAML Support** (`redhat.vscode-yaml`) - For configuration files

### Build Tasks

Pre-configured tasks available via **Terminal → Run Task**:

- `Build All Debug` - Build all modules in debug mode
- `Build App Debug` - Build only the phone app module
- `Build Wear Debug` - Build only the Wear OS module
- `Clean Build` - Clean all build artifacts
- `Run Tests` - Execute unit tests
- `Print Status` - Show build flavors and configuration
- `Check Wear ABI` - Verify Wear OS ABI configuration
- `Verify 16K` - Check 16KB page alignment

### Keyboard Shortcuts

- `Ctrl+Shift+P` → Type "Tasks: Run Task" to access build commands
- `Ctrl+Shift+B` → Run the default build task
- `F5` → Start debugging (requires Android device/emulator)

## IntelliJ IDEA / Android Studio Setup

### Opening the Project

1. Launch IntelliJ IDEA or Android Studio
2. Click **Open** or **Import Project**
3. Navigate to the project root directory (`WristLingo/`)
4. Select the root folder and click **OK**
5. Choose **Import project from external model → Gradle**
6. Click **Finish**

### Initial Configuration

The project includes pre-configured `.idea/` files that will:

- Set up Gradle integration with proper JVM settings
- Configure the three modules (`:app`, `:wear`, `:core`)  
- Set Java compilation target to JDK 17
- Enable Android project type recognition
- Configure Git version control integration

### Build Configurations

- **app** - Main Android application run configuration
- Custom Gradle tasks available in the Gradle tool window

### Troubleshooting

If the project doesn't import correctly:

1. **File → Invalidate Caches and Restart**
2. Delete `.idea/` folder and re-import
3. Ensure Android SDK path is configured in **Project Structure**

## Eclipse Setup

### Installation Requirements

1. Install Eclipse IDE for Java Developers
2. Install **Buildship Gradle Integration** plugin
3. Install **Kotlin Plugin for Eclipse** (if available)

### Importing the Multi-Module Project

1. Launch Eclipse
2. **File → Import → Existing Projects into Workspace**
3. Browse to the WristLingo directory
4. **Select all projects** (WristLingo root, WristLingo-app, WristLingo-wear, WristLingo-core)
5. Click **Finish**

**Important**: This is a multi-module project. Import all modules:

- `WristLingo` - Root/parent project (no source code)
- `WristLingo-app` - Android phone app module
- `WristLingo-wear` - Wear OS app module  
- `WristLingo-core` - Pure Kotlin shared library

### Gradle Integration

- The project uses Eclipse Buildship for Gradle integration
- Each module has its own `.project`, `.classpath`, and `.settings/` configuration
- Build tasks can be run from the **Gradle Tasks** view
- Source folders are automatically configured for each module

### Troubleshooting Eclipse Import

If you encounter **"Missing required source folder"** errors:

1. **Ensure all modules are imported**: The root project should NOT have source folders - only the individual modules (app, wear, core) contain source code
2. **Refresh projects**: Right-click each project → **Refresh**
3. **Rebuild Gradle projects**: Right-click each project → **Gradle** → **Refresh Gradle Project**
4. **Check .settings folder**: Each module should have its own `.settings/` directory with proper Gradle configuration

Common error messages and fixes:

- `"Missing required source folder: src/main/java"` on root project → This is expected; the root is just a container
- `"Missing Gradle project configuration folder: .settings"` → Fixed by importing all modules correctly
- Build path errors → Clean and refresh all projects

### Limitations

- Eclipse has limited Kotlin support compared to IntelliJ/VS Code
- Android debugging may require additional configuration
- Consider using IntelliJ IDEA or Android Studio for full Android development features

## Build System Details

### Gradle Wrapper

The project uses Gradle wrapper for consistent builds:

```bash
# Linux/macOS
./gradlew assembleOfflineDebug

# Windows
gradlew.bat assembleOfflineDebug
```

### Module Structure

- `:core` - Pure Kotlin contracts and utilities (no Android dependencies)
- `:app` - Android phone app with Compose UI
- `:wear` - Wear OS app with Compose for Wear

### Build Flavors

- `offline` - Whisper + ML Kit only (default)
- `hybrid` - System SpeechRecognizer + Cloud Translation toggle
- `cloudstt` - Enables Cloud STT v2 option (opt-in)

## Debugging

### Android Device/Emulator Setup

1. Enable **Developer Options** on your Android device
2. Enable **USB Debugging**
3. For Wear OS testing, pair a watch or use the Wear OS emulator

### IDE-Specific Debugging

- **VS Code**: Use the Java debugger with Android extension
- **IntelliJ/Android Studio**: Native Android debugging support
- **Eclipse**: Configure Android debugging via DDMS perspective

## Common Issues

### Gradle Sync Issues

1. Check internet connectivity for dependency downloads
2. Clear Gradle cache: `rm -rf ~/.gradle/caches/`
3. Ensure proper JDK version (17+)

### Android SDK Issues

1. Verify Android SDK path in IDE settings
2. Install missing SDK platforms/build tools via SDK Manager
3. Check `local.properties` file for correct SDK path

### Build Failures

1. Clean and rebuild: `./gradlew clean build`
2. Check for missing dependencies in `libs.versions.toml`
3. Verify module dependencies in `settings.gradle.kts`

## Additional Resources

- [Android Developer Guide](https://developer.android.com/guide)
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Gradle Build Tool](https://gradle.org/guides/)
- [Wear OS Development](https://developer.android.com/training/wearables)

---

For project-specific questions, refer to the main [README.md](./README.md) file or the inline documentation within the codebase.
