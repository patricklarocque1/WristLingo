# WristLingo Codex Environment Scripts

This directory contains two scripts for managing your WristLingo development environment in Codex.

## 🚀 Setup Script: `setup_codex_env.sh`

**Purpose:** Initial setup of the complete development environment for WristLingo.

### What it installs/configures

- ☕ **OpenJDK 21** - Required for Kotlin 2.2.20 and AGP 8.12.3
- 📱 **Android SDK 36** - Latest Android SDK with build tools 36.0.0
- 🔨 **Android NDK 29.0.14033849** - Native development kit
- 🏗️ **Gradle Wrapper** - Project build system
- 📦 **Project Dependencies** - All required libraries and dependencies
- 🛠️ **Development Tools** - Git LFS, Node.js, useful CLI tools
- 🔗 **Aliases** - Convenient commands prefixed with `wl-`

### Usage

```bash
# Make executable (if not already)
chmod +x setup_codex_env.sh

# Run setup
./setup_codex_env.sh
```

### After setup, restart your terminal or run

```bash
source ~/.bashrc
```

## 🔧 Maintenance Script: `maintain_codex_env.sh`

**Purpose:** Regular maintenance and updates for the development environment.

### What it does

- 🔄 **System Updates** - Updates all system packages
- 📱 **SDK Maintenance** - Updates Android SDK components
- 🧹 **Cache Cleanup** - Cleans Gradle and build caches
- 📦 **Dependency Refresh** - Refreshes project dependencies
- 🔍 **Build Verification** - Tests all modules and flavors
- 📝 **Git Maintenance** - Cleans and optimizes git repository
- 💾 **Storage Cleanup** - Removes temporary files and optimizes disk usage

### Usage

```bash
# Run maintenance (recommended weekly)
./maintain_codex_env.sh
```

## 🎯 WristLingo-Specific Aliases

After running the setup script, you'll have these convenient aliases:

### Build Commands

- `wl-build` - Full clean build
- `wl-clean` - Clean build directories
- `wl-test` - Run all tests
- `wl-check` - Run lint and quality checks

### Module-Specific Builds

- `wl-app-debug` - Build app module (offline debug)
- `wl-wear-debug` - Build wear module (offline debug)

### Installation Commands

- `wl-install-app` - Install app to connected device
- `wl-install-wear` - Install wear app to connected watch

### Information Commands

- `wl-deps` - Show project dependencies
- `wl-projects` - List all project modules
- `wl-tasks` - Show available Gradle tasks

### Android Development

- `adb-devices` - List connected devices
- `adb-logcat` - Show device logs
- `adb-wear-logcat` - Show wear device logs

## 📋 Project Structure

WristLingo is a multi-module Android project:

- **`:app`** - Main Android application
- **`:wear`** - Wear OS companion app
- **`:core`** - Shared business logic and contracts

### Build Flavors

- **`offline`** - Pure offline functionality
- **`hybrid`** - Cloud translation, local STT
- **`cloudstt`** - Cloud speech-to-text

## 🔍 Troubleshooting

### Common Issues

1. **Build fails after maintenance:**

   ```bash
   ./gradlew clean
   ./setup_codex_env.sh  # Re-run setup
   ```

2. **Android SDK issues:**

   ```bash
   export ANDROID_HOME=$HOME/Android/Sdk
   export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
   ```

3. **Java version mismatch:**

   ```bash
   sudo apt install openjdk-21-jdk
   export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
   ```

4. **Gradle daemon issues:**

   ```bash
   ./gradlew --stop
   rm -rf ~/.gradle/daemon/
   ```

## 📊 Environment Validation

Both scripts perform comprehensive validation:

- ✅ Java 21 installation
- ✅ Android SDK 36 components
- ✅ Gradle wrapper functionality
- ✅ All project modules build successfully
- ✅ All build flavors work correctly

## 🚀 Quick Start

1. Run setup script: `./setup_codex_env.sh`
2. Restart terminal: `source ~/.bashrc`
3. Test build: `wl-build`
4. Build and install: `wl-install-app && wl-install-wear`

## 🔄 Regular Maintenance

- Run `./maintain_codex_env.sh` weekly
- Keep `gradle/libs.versions.toml` updated
- Monitor disk usage in Codex environment

---

**Note:** These scripts are specifically tailored for the WristLingo project and its dependencies. They handle the unique requirements of Android + Wear OS development with multiple build flavors and modern Android development tools.
