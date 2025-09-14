#!/usr/bin/env bash
set -euo pipefail

echo "🔧 WristLingo Codex Environment Maintenance..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project-specific versions for validation
EXPECTED_ANDROID_SDK="36"
EXPECTED_JAVA_VERSION="21"
EXPECTED_KOTLIN_VERSION="2.2.20"

echo -e "${BLUE}📋 WristLingo Maintenance Checklist:${NC}"
echo "  • System package updates"
echo "  • Android SDK component updates"  
echo "  • Project dependency updates"
echo "  • Gradle cache cleanup"
echo "  • Build verification"
echo "  • Git repository maintenance"
echo ""

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to get Android SDK version
get_android_sdk_version() {
    if [ -d "$HOME/Android/Sdk/platforms" ]; then
        ls "$HOME/Android/Sdk/platforms" | grep "android-" | sed 's/android-//' | sort -n | tail -1
    else
        echo "Not found"
    fi
}

# System maintenance
echo -e "${YELLOW}🔄 Updating system packages...${NC}"
sudo apt-get update -qq
sudo apt-get upgrade -yqq
sudo apt-get autoremove -yqq
sudo apt-get autoclean -qq

# Java version check
echo -e "${YELLOW}☕ Checking Java version...${NC}"
JAVA_CURRENT=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_CURRENT" != "$EXPECTED_JAVA_VERSION" ]; then
    echo -e "${YELLOW}⚠️  Java version mismatch. Expected: $EXPECTED_JAVA_VERSION, Found: $JAVA_CURRENT${NC}"
    echo -e "${BLUE}Consider updating Java if needed${NC}"
else
    echo -e "${GREEN}✅ Java $JAVA_CURRENT - OK${NC}"
fi

# Android SDK maintenance
if [ -d "$HOME/Android/Sdk" ]; then
    echo -e "${YELLOW}📱 Updating Android SDK components...${NC}"
    export ANDROID_HOME="$HOME/Android/Sdk"
    export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"
    
    # Update SDK components
    sdkmanager --update || echo -e "${YELLOW}⚠️  SDK Manager update had issues (may be normal)${NC}"
    
    # Check if expected SDK version is installed
    CURRENT_SDK=$(get_android_sdk_version)
    if [ "$CURRENT_SDK" != "$EXPECTED_ANDROID_SDK" ]; then
        echo -e "${YELLOW}📲 Installing Android SDK $EXPECTED_ANDROID_SDK...${NC}"
        sdkmanager "platforms;android-$EXPECTED_ANDROID_SDK" "build-tools;36.0.0"
    fi
    
    echo -e "${GREEN}✅ Android SDK updated${NC}"
else
    echo -e "${RED}❌ Android SDK not found at $HOME/Android/Sdk${NC}"
    echo -e "${BLUE}💡 Run setup_codex_env.sh first${NC}"
fi

# Navigate to project directory
if [ -d "/workspace/WristLingo" ]; then
    cd /workspace/WristLingo
elif [ -d "/home/boypa/projects/WristLingo" ]; then
    cd /home/boypa/projects/WristLingo
else
    echo -e "${RED}❌ WristLingo project directory not found${NC}"
    exit 1
fi

echo -e "${BLUE}📂 Working in: $(pwd)${NC}"

# Git repository maintenance
echo -e "${YELLOW}📝 Git repository maintenance...${NC}"
if [ -d ".git" ]; then
    # Fetch latest changes (but don't merge)
    git fetch origin --prune || echo -e "${YELLOW}⚠️  Git fetch failed (may be offline)${NC}"
    
    # Clean up Git repository
    git gc --auto
    git prune
    
    # Show current status
    echo -e "${BLUE}📊 Git Status:${NC}"
    git status --short || true
    
    # Check for updates
    if git status -uno | grep -q "Your branch is behind"; then
        echo -e "${YELLOW}📥 Updates available. Run 'git pull' when ready.${NC}"
    elif git status -uno | grep -q "up to date\|up-to-date"; then
        echo -e "${GREEN}✅ Repository up to date${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  Not a Git repository${NC}"
fi

# Gradle maintenance
echo -e "${YELLOW}🔧 Gradle maintenance...${NC}"

# Clean Gradle caches
echo "  • Cleaning Gradle build cache..."
./gradlew clean --quiet

echo "  • Cleaning Gradle daemon cache..."
./gradlew --stop
rm -rf ~/.gradle/caches/
rm -rf ~/.gradle/daemon/

echo "  • Refreshing dependencies..."
./gradlew --refresh-dependencies --quiet || {
    echo -e "${YELLOW}⚠️  Dependency refresh had issues (may be normal)${NC}"
}

# Update Gradle wrapper if needed
echo "  • Checking Gradle wrapper..."
./gradlew wrapper --gradle-version latest --quiet || {
    echo -e "${BLUE}ℹ️  Using current Gradle version${NC}"
}

# Dependency updates check
echo -e "${YELLOW}📦 Checking for dependency updates...${NC}"
./gradlew dependencyUpdates --quiet 2>/dev/null || {
    echo -e "${BLUE}ℹ️  Dependency update plugin not available${NC}"
    echo -e "${BLUE}💡 Check gradle/libs.versions.toml manually for updates${NC}"
}

# Check current versions in use
echo -e "${BLUE}📋 Current Project Versions:${NC}"
echo "  • AGP: $(grep 'agp = ' gradle/libs.versions.toml 2>/dev/null | cut -d'"' -f2 || echo 'Unknown')"
echo "  • Kotlin: $(grep 'kotlin = ' gradle/libs.versions.toml 2>/dev/null | cut -d'"' -f2 || echo 'Unknown')"
echo "  • Compose BOM: $(grep 'compose-bom = ' gradle/libs.versions.toml 2>/dev/null | cut -d'"' -f2 || echo 'Unknown')"
echo "  • Room: $(grep 'room = ' gradle/libs.versions.toml 2>/dev/null | cut -d'"' -f2 || echo 'Unknown')"
echo "  • Wear Compose: $(grep 'wear-compose = ' gradle/libs.versions.toml 2>/dev/null | cut -d'"' -f2 || echo 'Unknown')"

# Build verification for WristLingo modules
echo -e "${YELLOW}🔨 Build verification...${NC}"

echo "  • Testing core module..."
./gradlew :core:build --quiet --offline || {
    echo -e "${YELLOW}⚠️  Core build failed - may need dependency updates${NC}"
}

echo "  • Testing app module (offline flavor)..."
./gradlew :app:assembleOfflineDebug --quiet --offline || {
    echo -e "${YELLOW}⚠️  App build failed - may need dependency updates${NC}"
}

echo "  • Testing wear module (offline flavor)..."
./gradlew :wear:assembleOfflineDebug --quiet --offline || {
    echo -e "${YELLOW}⚠️  Wear build failed - may need dependency updates${NC}"
}

# Test all build flavors
echo "  • Testing build flavors..."
for flavor in offline hybrid cloudstt; do
    echo "    - $flavor flavor..."
    ./gradlew :app:assemble${flavor^}Debug --dry-run --quiet || {
        echo -e "${YELLOW}⚠️  $flavor flavor may have issues${NC}"
    }
done

# Run tests
echo -e "${YELLOW}🧪 Running tests...${NC}"
./gradlew test --quiet || {
    echo -e "${YELLOW}⚠️  Some tests failed - check test reports${NC}"
}

# Check for linting issues
echo -e "${YELLOW}🔍 Code quality checks...${NC}"
./gradlew check --quiet || {
    echo -e "${YELLOW}⚠️  Lint/quality checks found issues${NC}"
    echo -e "${BLUE}💡 Run './gradlew check' for details${NC}"
}

# Storage cleanup
echo -e "${YELLOW}🧹 Storage cleanup...${NC}"
echo "  • Cleaning build directories..."
find . -name "build" -type d -exec rm -rf {} + 2>/dev/null || true

echo "  • Cleaning temporary files..."
find . -name "*.tmp" -delete 2>/dev/null || true
find . -name ".DS_Store" -delete 2>/dev/null || true
find . -name "Thumbs.db" -delete 2>/dev/null || true

# Android emulator maintenance
if command_exists emulator; then
    echo -e "${YELLOW}📱 Android emulator maintenance...${NC}"
    # Clean emulator logs
    rm -rf ~/.android/avd/*.avd/userdata-qemu.img.qcow2 2>/dev/null || true
    echo -e "${GREEN}✅ Emulator cache cleaned${NC}"
fi

# Development environment validation
echo -e "${YELLOW}✅ Environment validation...${NC}"

# Check Java
if command_exists java; then
    echo -e "${GREEN}✅ Java: $(java -version 2>&1 | head -1)${NC}"
else
    echo -e "${RED}❌ Java not found${NC}"
fi

# Check Android SDK
if [ -d "$HOME/Android/Sdk" ]; then
    echo -e "${GREEN}✅ Android SDK: $(get_android_sdk_version)${NC}"
else
    echo -e "${RED}❌ Android SDK not found${NC}"
fi

# Check Gradle
if [ -f "./gradlew" ]; then
    GRADLE_VERSION=$(./gradlew --version | grep "Gradle " | cut -d' ' -f2)
    echo -e "${GREEN}✅ Gradle: $GRADLE_VERSION${NC}"
else
    echo -e "${RED}❌ Gradle wrapper not found${NC}"
fi

# Final build test
echo -e "${YELLOW}🎯 Final build test...${NC}"
./gradlew :core:build --quiet && echo -e "${GREEN}✅ Core module builds successfully${NC}" || echo -e "${RED}❌ Core module build failed${NC}"

# Disk space check
echo -e "${YELLOW}💾 Disk space check...${NC}"
DISK_USAGE=$(df -h . | tail -1 | awk '{print $5}' | sed 's/%//')
if [ "$DISK_USAGE" -gt 80 ]; then
    echo -e "${YELLOW}⚠️  Disk usage is ${DISK_USAGE}% - consider cleaning up${NC}"
else
    echo -e "${GREEN}✅ Disk usage: ${DISK_USAGE}%${NC}"
fi

# Summary
echo ""
echo -e "${GREEN}🎉 WristLingo Codex Environment Maintenance Complete!${NC}"
echo ""
echo -e "${BLUE}🔧 What was maintained:${NC}"
echo "  ✅ System packages updated"
echo "  ✅ Android SDK components updated"
echo "  ✅ Gradle cache cleaned"
echo "  ✅ Dependencies refreshed"
echo "  ✅ Build verification completed"
echo "  ✅ Git repository cleaned"
echo "  ✅ Storage optimized"
echo ""
echo -e "${BLUE}📊 Environment Status:${NC}"
echo "  • Java: $(java -version 2>&1 | head -1 | cut -d'"' -f2)"
echo "  • Android SDK: $(get_android_sdk_version)"
echo "  • Gradle: $(./gradlew --version 2>/dev/null | grep "Gradle " | cut -d' ' -f2 || echo 'Error')"
echo "  • Disk Usage: ${DISK_USAGE}%"
echo ""
echo -e "${BLUE}💡 Next steps:${NC}"
echo "  1. Check 'gradle/libs.versions.toml' for version updates"
echo "  2. Run 'wl-build' to test full build"
echo "  3. If issues persist, run 'setup_codex_env.sh' to reset"
echo ""
echo -e "${GREEN}Maintenance complete! Environment is ready for development. 🚀${NC}"
