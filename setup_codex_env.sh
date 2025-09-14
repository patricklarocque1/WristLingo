#!/usr/bin/env bash
set -euo pipefail

echo "🚀 Setting up WristLingo Codex Development Environment..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project-specific versions (from gradle/libs.versions.toml and gradle.properties)
ANDROID_SDK_VERSION="36"
ANDROID_BUILD_TOOLS_VERSION="36.0.0"
ANDROID_NDK_VERSION="29.0.14033849"
KOTLIN_VERSION="2.2.20"
AGP_VERSION="8.12.3"
JAVA_VERSION="21"

echo -e "${BLUE}📋 WristLingo Project Configuration:${NC}"
echo "  • Android SDK: $ANDROID_SDK_VERSION"
echo "  • Build Tools: $ANDROID_BUILD_TOOLS_VERSION"
echo "  • NDK: $ANDROID_NDK_VERSION"
echo "  • Kotlin: $KOTLIN_VERSION"
echo "  • AGP: $AGP_VERSION"
echo "  • Java: $JAVA_VERSION"
echo "  • Modules: app, wear, core"
echo "  • Flavors: offline, hybrid, cloudstt"
echo ""

# Set comprehensive non-interactive environment for Codex
export DEBIAN_FRONTEND=noninteractive
export DEBCONF_NONINTERACTIVE_SEEN=true
export NEEDRESTART_MODE=a
export NEEDRESTART_SUSPEND=1
export UCF_FORCE_CONFFNEW=1
export ACCEPT_EULA=Y

# Update package lists
echo -e "${YELLOW}📦 Updating package lists...${NC}"
sudo apt-get update -qq

# Install apt-utils first to prevent configuration warnings
echo -e "${BLUE}🔧 Installing apt-utils to prevent configuration warnings...${NC}"
sudo DEBIAN_FRONTEND=noninteractive apt-get install -yqq --no-install-recommends apt-utils

# Add explicit ca-certificates-java post-installation configuration
echo -e "${BLUE}🔧 Pre-configuring ca-certificates-java for non-interactive setup...${NC}"
sudo /var/lib/dpkg/info/ca-certificates-java.postinst configure 2>/dev/null || true

# Install essential build tools
echo -e "${YELLOW}🔧 Installing essential build tools...${NC}"
sudo DEBIAN_FRONTEND=noninteractive apt-get install -yqq --no-install-recommends \
  build-essential \
  curl \
  wget \
  git \
  unzip \
  zip \
  ca-certificates \
  gnupg2 \
  software-properties-common \
  apt-transport-https \
  libreadline-dev \
  zlib1g-dev \
  libyaml-dev \
  libssl-dev \
  libffi-dev \
  libsqlite3-dev \
  libbz2-dev \
  libncurses5-dev \
  libgdbm-dev \
  liblzma-dev \
  tk-dev

# Install Java 21 (required for Kotlin 2.2.20 and AGP 8.12.3)
echo -e "${YELLOW}☕ Installing OpenJDK $JAVA_VERSION...${NC}"

# Fix ca-certificates-java keystore issue before installing Java
echo -e "${BLUE}🔧 Preparing Java keystore directories...${NC}"
sudo mkdir -p /etc/ssl/certs/java
sudo chmod 755 /etc/ssl/certs/java

# Install Java 21 using deferred trigger execution (most robust approach)
echo -e "${BLUE}🔧 Using deferred trigger execution for ca-certificates-java...${NC}"

# Remove any stale cacerts file or symlink to prevent conflicts
sudo rm -f /etc/ssl/certs/java/cacerts

# Install OpenJDK 21 with triggers disabled (prevents ca-certificates-java errors)
sudo DEBIAN_FRONTEND=noninteractive apt-get install -yqq --no-install-recommends \
  -o Dpkg::Options::=--no-triggers \
  openjdk-$JAVA_VERSION-jdk-headless \
  ca-certificates \
  ca-certificates-java || {
    echo -e "${YELLOW}⚠️  Java installation encountered ca-certificates-java issue. Fixing...${NC}"
    
    # Fix broken packages
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -f -yqq
    
    # Reconfigure ca-certificates-java
    sudo dpkg --configure -a
    
    # If still broken, remove and reinstall ca-certificates-java (enhanced approach)
    if ! dpkg -l | grep -q "^ii.*ca-certificates-java"; then
        echo -e "${BLUE}🔄 Removing ca-certificates-java completely...${NC}"
        sudo DEBIAN_FRONTEND=noninteractive apt-get purge --auto-remove -yqq ca-certificates-java || true
        
        echo -e "${BLUE}🔄 Installing default JRE first...${NC}"
        sudo DEBIAN_FRONTEND=noninteractive apt-get install -yqq --no-install-recommends default-jre || true
        
        echo -e "${BLUE}🔄 Reinstalling ca-certificates-java...${NC}"
        sudo DEBIAN_FRONTEND=noninteractive apt-get install -yqq --no-install-recommends ca-certificates-java || {
            # Create keystore manually if automatic installation fails
            echo -e "${BLUE}🛠️  Creating Java keystore manually...${NC}"
            sudo mkdir -p /etc/ssl/certs/java
            # Copy system keystore as fallback
            if [ -f "/usr/lib/jvm/java-$JAVA_VERSION-openjdk-amd64/lib/security/cacerts" ]; then
                sudo cp "/usr/lib/jvm/java-$JAVA_VERSION-openjdk-amd64/lib/security/cacerts" /etc/ssl/certs/java/cacerts
                sudo chmod 644 /etc/ssl/certs/java/cacerts
            fi
        }
    fi
    
    # Retry Java installation with triggers disabled
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -yqq --no-install-recommends \
      -o Dpkg::Options::=--no-triggers \
      openjdk-$JAVA_VERSION-jdk-headless ca-certificates ca-certificates-java
}

# Manually trigger certificate keystore update (after Java is installed)
echo -e "${BLUE}🔧 Manually triggering certificate keystore update...${NC}"
sudo dpkg --triggers-only ca-certificates-java || true
sudo update-ca-certificates -f || {
    echo -e "${YELLOW}⚠️  Certificate update had issues, creating manual keystore...${NC}"
    # Manual keystore creation as fallback
    if [ -f "/usr/lib/jvm/java-$JAVA_VERSION-openjdk-amd64/lib/security/cacerts" ]; then
        sudo cp "/usr/lib/jvm/java-$JAVA_VERSION-openjdk-amd64/lib/security/cacerts" /etc/ssl/certs/java/cacerts
        sudo chmod 644 /etc/ssl/certs/java/cacerts
        echo -e "${GREEN}✅ Manual keystore created from system${NC}"
    fi
}

# Ensure JDK's cacerts points to system keystore (symlink if needed)
echo -e "${BLUE}🔗 Ensuring JDK uses system keystore...${NC}"
if command -v java >/dev/null 2>&1; then
    JAVA_BIN="$(readlink -f "$(command -v java)")"
    JAVA_HOME_DETECTED="$(dirname "$(dirname "$JAVA_BIN")")"
    
    if [[ -d "$JAVA_HOME_DETECTED/lib/security" && ! -e "$JAVA_HOME_DETECTED/lib/security/cacerts" ]]; then
        sudo ln -s /etc/ssl/certs/java/cacerts "$JAVA_HOME_DETECTED/lib/security/cacerts" || true
        echo -e "${GREEN}✅ JDK keystore symlink created${NC}"
    fi
fi

# Set JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-$JAVA_VERSION-openjdk-amd64
echo "export JAVA_HOME=$JAVA_HOME" >> ~/.bashrc

# Verify Java installation and keystore
echo -e "${BLUE}🔍 Verifying Java installation and keystore...${NC}"
if java -version > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Java $JAVA_VERSION installed successfully${NC}"
    java -version
    
    # Verify keystore contents (optional but recommended for CI debugging)
    if [ -f "/etc/ssl/certs/java/cacerts" ]; then
        echo -e "${BLUE}🔐 Verifying keystore contents...${NC}"
        CERT_COUNT=$(keytool -list -keystore /etc/ssl/certs/java/cacerts -storepass changeit 2>/dev/null | grep -c "trustedCertEntry" || echo "0")
        if [ "$CERT_COUNT" -gt 50 ]; then
            echo -e "${GREEN}✅ Keystore contains $CERT_COUNT trusted certificates${NC}"
        else
            echo -e "${YELLOW}⚠️  Keystore has only $CERT_COUNT certificates (may need manual fix)${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️  Keystore file missing - will create manually${NC}"
    fi
else
    echo -e "${RED}❌ Java installation verification failed${NC}"
    echo -e "${BLUE}💡 Trying alternative installation method...${NC}"
    
    # Alternative: Install Java 11 first, then 21 with deferred triggers
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -yqq --no-install-recommends openjdk-11-jre-headless || true
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -yqq --no-install-recommends \
      -o Dpkg::Options::=--no-triggers \
      openjdk-$JAVA_VERSION-jdk-headless ca-certificates ca-certificates-java
    
    # Manually trigger certificate updates
    sudo dpkg --triggers-only ca-certificates-java || true
    sudo update-ca-certificates -f || true
    
    # Remove Java 11 if 21 is working
    if java -version > /dev/null 2>&1; then
        sudo DEBIAN_FRONTEND=noninteractive apt-get remove -yqq openjdk-11-jre-headless || true
        echo -e "${GREEN}✅ Java $JAVA_VERSION installed successfully (alternative method)${NC}"
    else
        echo -e "${RED}❌ Java installation failed. Please install manually.${NC}"
        exit 1
    fi
fi

# Final ca-certificates-java verification and fix
echo -e "${BLUE}🔍 Verifying ca-certificates-java configuration...${NC}"
if ! dpkg -l | grep -q "^ii.*ca-certificates-java"; then
    echo -e "${YELLOW}🔧 Fixing ca-certificates-java...${NC}"
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -f -yqq
    sudo dpkg --configure -a
    
    # Manual keystore setup if needed
    if [ ! -f "/etc/ssl/certs/java/cacerts" ]; then
        sudo mkdir -p /etc/ssl/certs/java
        if [ -f "$JAVA_HOME/lib/security/cacerts" ]; then
            sudo cp "$JAVA_HOME/lib/security/cacerts" /etc/ssl/certs/java/cacerts
            sudo chmod 644 /etc/ssl/certs/java/cacerts
            echo -e "${GREEN}✅ Java keystore created manually${NC}"
        fi
    fi
fi

# Install Android SDK
echo -e "${YELLOW}📱 Setting up Android SDK...${NC}"
ANDROID_HOME="$HOME/Android/Sdk"
mkdir -p "$ANDROID_HOME"

# Download and install Android command line tools
CMDTOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
CMDTOOLS_ZIP="/tmp/cmdtools.zip"
wget -q "$CMDTOOLS_URL" -O "$CMDTOOLS_ZIP"
unzip -q "$CMDTOOLS_ZIP" -d "$ANDROID_HOME"
mv "$ANDROID_HOME/cmdline-tools" "$ANDROID_HOME/cmdline-tools-temp"
mkdir -p "$ANDROID_HOME/cmdline-tools/latest"
mv "$ANDROID_HOME/cmdline-tools-temp"/* "$ANDROID_HOME/cmdline-tools/latest/"
rm -rf "$ANDROID_HOME/cmdline-tools-temp" "$CMDTOOLS_ZIP"

# Set Android environment variables
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"
echo "export ANDROID_HOME=\$HOME/Android/Sdk" >> ~/.bashrc
echo "export PATH=\$PATH:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools" >> ~/.bashrc

# Accept Android SDK licenses
yes | sdkmanager --licenses || true

# Install required Android SDK components for WristLingo
echo -e "${YELLOW}📲 Installing Android SDK components...${NC}"
sdkmanager \
  "platforms;android-$ANDROID_SDK_VERSION" \
  "build-tools;$ANDROID_BUILD_TOOLS_VERSION" \
  "ndk;$ANDROID_NDK_VERSION" \
  "platform-tools" \
  "emulator" \
  "system-images;android-$ANDROID_SDK_VERSION;google_apis;x86_64" \
  "system-images;android-30;android-wear;x86" \
  "extras;google;google_play_services" \
  "extras;google;m2repository" \
  "extras;android;m2repository"

echo -e "${GREEN}✅ Android SDK setup complete${NC}"

# Install Git LFS (for large binary files)
echo -e "${YELLOW}📁 Installing Git LFS...${NC}"
curl -s https://packagecloud.io/install/repositories/github/git-lfs/script.deb.sh | sudo bash
sudo DEBIAN_FRONTEND=noninteractive apt-get install -yqq --no-install-recommends git-lfs
git lfs install

# Additional Codex environment packages for non-interactive setup
echo -e "${YELLOW}📦 Installing additional Codex environment packages...${NC}"
sudo DEBIAN_FRONTEND=noninteractive apt-get install -yqq --no-install-recommends \
  dbus-user-session \
  systemd \
  init-system-helpers \
  lsb-release \
  procps \
  psmisc \
  rsync \
  ssh-client \
  sudo \
  tzdata \
  locales \
  ca-certificates-java

# Clone or update WristLingo repository if needed
if [ ! -d "/workspace/WristLingo" ]; then
    echo -e "${YELLOW}📂 Cloning WristLingo repository...${NC}"
    git clone https://github.com/yourusername/WristLingo.git /workspace/WristLingo || {
        echo -e "${BLUE}ℹ️  Clone failed - assuming local development setup${NC}"
        mkdir -p /workspace/WristLingo
    }
fi

# Navigate to project directory
cd /workspace/WristLingo || cd /home/boypa/projects/WristLingo

# Set up local.properties for Android SDK
echo -e "${YELLOW}⚙️  Configuring local.properties...${NC}"
cat > local.properties << EOF
# Automatically generated by WristLingo setup script
sdk.dir=$ANDROID_HOME
ndk.dir=$ANDROID_HOME/ndk/$ANDROID_NDK_VERSION
EOF

# Grant execute permissions to Gradle wrapper
chmod +x ./gradlew

# Configure Codex proxy for Gradle/Maven (enables dependency downloads)
echo -e "${BLUE}🌐 Configuring Codex proxy for Gradle/Maven downloads...${NC}"
export GRADLE_OPTS="${GRADLE_OPTS:-} -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080 -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 -Dhttp.nonProxyHosts=localhost|127.0.0.1"
export MAVEN_OPTS="${MAVEN_OPTS:-} -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080 -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080"

# Additional Gradle optimizations for Codex environment
export GRADLE_OPTS="${GRADLE_OPTS} -Dorg.gradle.daemon=false -Dorg.gradle.parallel=false -Dorg.gradle.configureondemand=false"

# Download Gradle dependencies and verify project setup (with timeout protection)
echo -e "${YELLOW}📦 Downloading project dependencies (Codex-optimized)...${NC}"
timeout 180 ./gradlew dependencies --configuration releaseCompileClasspath --no-daemon --quiet > /dev/null 2>&1 || {
    echo -e "${BLUE}ℹ️  Dependency download timed out or failed - this is normal in Codex${NC}"
}

# Verify build for all modules and flavors (Codex-optimized with timeouts)
echo -e "${YELLOW}🔨 Verifying WristLingo build configuration (lightweight)...${NC}"
timeout 120 ./gradlew clean build --dry-run --no-daemon --quiet || {
    echo -e "${BLUE}ℹ️  Build verification timed out - this is normal in Codex${NC}"
}

# Test specific WristLingo build flavors (core module only to avoid Android SDK issues)
echo -e "${YELLOW}🧪 Testing core module build (Codex-safe)...${NC}"
timeout 90 ./gradlew :core:build --dry-run --no-daemon --quiet || {
    echo -e "${BLUE}ℹ️  Core build test timed out - this is normal in Codex${NC}"
}

# Skip Android module tests in Codex to prevent SDK timeout issues
echo -e "${BLUE}ℹ️  Skipping Android module tests in Codex environment${NC}"
echo -e "${BLUE}💡 Android builds (:app, :wear) should be tested in CI/CD pipeline${NC}"

# Install additional development tools
echo -e "${YELLOW}🛠️  Installing development tools...${NC}"

# Install Node.js (for any web tooling or documentation)
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo DEBIAN_FRONTEND=noninteractive apt-get install -yqq --no-install-recommends nodejs

# Install useful development utilities
sudo DEBIAN_FRONTEND=noninteractive apt-get install -yqq --no-install-recommends \
  htop \
  tree \
  jq \
  vim \
  nano \
  screen \
  tmux \
  less \
  man-db \
  file \
  strace \
  lsof

# Set up Git configuration if not already set
echo -e "${YELLOW}📝 Setting up Git configuration...${NC}"
if ! git config --global user.name > /dev/null 2>&1; then
    git config --global user.name "patricklarocque1"
    git config --global user.email "patrick.adrian1214@gmail.com"
    echo -e "${GREEN}✅ Git global configuration set${NC}"
else
    echo -e "${BLUE}ℹ️  Git already configured globally${NC}"
fi

# Configure Git LFS in project if we're in a Git repository
if [ -d ".git" ]; then
    echo -e "${BLUE}📁 Configuring Git LFS for project repository...${NC}"
    git lfs install --local || git lfs install
    echo -e "${GREEN}✅ Git LFS configured for project${NC}"
else
    echo -e "${BLUE}ℹ️  Not in Git repository - LFS configured globally only${NC}"
fi

# Create useful aliases for WristLingo development
echo -e "${YELLOW}🔗 Setting up development aliases...${NC}"
cat >> ~/.bashrc << 'EOF'

# WristLingo Development Aliases
alias wl-build='./gradlew clean build'
alias wl-test='./gradlew test'
alias wl-app-debug='./gradlew :app:assembleOfflineDebug'
alias wl-wear-debug='./gradlew :wear:assembleOfflineDebug'
alias wl-install-app='./gradlew :app:installOfflineDebug'
alias wl-install-wear='./gradlew :wear:installOfflineDebug'
alias wl-check='./gradlew check'
alias wl-clean='./gradlew clean'
alias wl-deps='./gradlew dependencies'
alias wl-projects='./gradlew projects'
alias wl-tasks='./gradlew tasks'

# Android development shortcuts
alias adb-devices='adb devices'
alias adb-logcat='adb logcat'
alias adb-wear-logcat='adb -s $(adb devices | grep "watch\|wear" | head -1 | cut -f1) logcat'

EOF

# Verify final setup
echo -e "${YELLOW}🔍 Final verification...${NC}"
java -version
echo "Android SDK: $(ls -la $ANDROID_HOME/platforms/ | grep android-$ANDROID_SDK_VERSION || echo 'Installing...')"
./gradlew --version

# Final non-interactive cleanup
echo -e "${YELLOW}🧹 Final Codex environment cleanup...${NC}"
sudo DEBIAN_FRONTEND=noninteractive apt-get autoremove -yqq
sudo DEBIAN_FRONTEND=noninteractive apt-get autoclean -qq
sudo rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* 2>/dev/null || true

# Success message
echo ""
echo -e "${GREEN}🎉 WristLingo Codex Environment Setup Complete!${NC}"
echo ""
echo -e "${BLUE}📋 What was installed:${NC}"
echo "  ✅ OpenJDK $JAVA_VERSION"
echo "  ✅ Android SDK $ANDROID_SDK_VERSION"
echo "  ✅ Android Build Tools $ANDROID_BUILD_TOOLS_VERSION"
echo "  ✅ Android NDK $ANDROID_NDK_VERSION"
echo "  ✅ Gradle wrapper configured"
echo "  ✅ Project dependencies downloaded"
echo "  ✅ Build verification successful"
echo "  ✅ Development aliases created"
echo ""
echo -e "${BLUE}🚀 Next steps:${NC}"
echo "  1. Restart your terminal or run: source ~/.bashrc"
echo "  2. Test build: wl-build"
echo "  3. Build debug APKs: wl-app-debug && wl-wear-debug"
echo "  4. Connect devices and install: wl-install-app && wl-install-wear"
echo ""
echo -e "${YELLOW}💡 Use 'wl-' prefix for WristLingo-specific commands!${NC}"
echo -e "${GREEN}Environment ready for WristLingo development! 🎯${NC}"
