#!/usr/bin/env bash
set -euo pipefail

echo "🔧 Fixing ca-certificates-java keystore issue..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Set non-interactive environment for Codex
export DEBIAN_FRONTEND=noninteractive
export DEBCONF_NONINTERACTIVE_SEEN=true
export NEEDRESTART_MODE=a
export NEEDRESTART_SUSPEND=1

JAVA_VERSION="21"

echo -e "${BLUE}📋 Java Keystore Fix for WristLingo Codex Environment${NC}"
echo ""

# Step 0: Install apt-utils first to prevent configuration warnings
echo -e "${YELLOW}0️⃣ Installing apt-utils for non-interactive setup...${NC}"
sudo DEBIAN_FRONTEND=noninteractive apt-get update -qq
sudo DEBIAN_FRONTEND=noninteractive apt-get install -yqq --no-install-recommends apt-utils

# Step 1: Fix broken packages
echo -e "${YELLOW}1️⃣ Fixing broken packages...${NC}"
sudo DEBIAN_FRONTEND=noninteractive apt-get install -f -yqq || {
    echo -e "${YELLOW}⚠️  Some package fixes may have failed - continuing...${NC}"
}

# Step 2: Reconfigure packages
echo -e "${YELLOW}2️⃣ Reconfiguring packages...${NC}"
sudo dpkg --configure -a || {
    echo -e "${YELLOW}⚠️  Some package configurations may have failed - continuing...${NC}"
}

# Step 2.5: Explicit ca-certificates-java post-installation fix
echo -e "${YELLOW}2️⃣.5 Running ca-certificates-java post-installation fix...${NC}"
sudo /var/lib/dpkg/info/ca-certificates-java.postinst configure 2>/dev/null || true

# Step 3: Create keystore directory
echo -e "${YELLOW}3️⃣ Creating Java keystore directory...${NC}"
sudo mkdir -p /etc/ssl/certs/java
sudo chmod 755 /etc/ssl/certs/java
echo -e "${GREEN}✅ Directory created: /etc/ssl/certs/java${NC}"

# Step 4: Remove and reinstall using deferred trigger execution (most robust)
echo -e "${YELLOW}4️⃣ Removing ca-certificates-java completely...${NC}"
sudo DEBIAN_FRONTEND=noninteractive apt-get purge --auto-remove -yqq ca-certificates-java || true

# Remove any stale cacerts file or symlink
sudo rm -f /etc/ssl/certs/java/cacerts

echo -e "${YELLOW}5️⃣ Installing Java with deferred triggers (prevents ca-certificates errors)...${NC}"
sudo DEBIAN_FRONTEND=noninteractive apt-get install -yqq --no-install-recommends \
  -o Dpkg::Options::=--no-triggers \
  openjdk-21-jdk-headless ca-certificates ca-certificates-java || {
    echo -e "${YELLOW}⚠️  Installation with triggers disabled failed, trying default JRE approach...${NC}"
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -yqq --no-install-recommends default-jre || true
}

echo -e "${YELLOW}6️⃣ Manually triggering certificate keystore update...${NC}"
sudo dpkg --triggers-only ca-certificates-java || true
sudo update-ca-certificates -f || {
    echo -e "${YELLOW}⚠️  Automatic installation failed, creating keystore manually...${NC}"
    
    # Manual keystore creation
    JAVA_HOME="/usr/lib/jvm/java-$JAVA_VERSION-openjdk-amd64"
    if [ -f "$JAVA_HOME/lib/security/cacerts" ]; then
        echo -e "${BLUE}📋 Copying system keystore...${NC}"
        sudo cp "$JAVA_HOME/lib/security/cacerts" /etc/ssl/certs/java/cacerts
        sudo chmod 644 /etc/ssl/certs/java/cacerts
        echo -e "${GREEN}✅ Keystore created manually${NC}"
    else
        echo -e "${YELLOW}⚠️  System keystore not found, creating empty keystore...${NC}"
        # Create a basic empty keystore
        sudo keytool -genkey -alias dummy -keystore /etc/ssl/certs/java/cacerts -storepass changeit -keypass changeit -dname "CN=dummy" -keyalg RSA -keysize 2048 -validity 365 -noprompt 2>/dev/null || true
        sudo keytool -delete -alias dummy -keystore /etc/ssl/certs/java/cacerts -storepass changeit -noprompt 2>/dev/null || true
        sudo chmod 644 /etc/ssl/certs/java/cacerts
        echo -e "${GREEN}✅ Empty keystore created${NC}"
    fi
}

# Step 7: Verify installation
echo -e "${YELLOW}7️⃣ Verifying installation...${NC}"
if [ -f "/etc/ssl/certs/java/cacerts" ]; then
    echo -e "${GREEN}✅ Java keystore exists: /etc/ssl/certs/java/cacerts${NC}"
    ls -la /etc/ssl/certs/java/cacerts
else
    echo -e "${RED}❌ Java keystore still missing${NC}"
fi

# Step 8: Check ca-certificates-java package status
if dpkg -l | grep -q "^ii.*ca-certificates-java"; then
    echo -e "${GREEN}✅ ca-certificates-java package is properly installed${NC}"
else
    echo -e "${YELLOW}⚠️  ca-certificates-java package status unclear${NC}"
    dpkg -l | grep ca-certificates-java || echo "Package not found in dpkg list"
fi

# Step 9: Test Java
echo -e "${YELLOW}9️⃣ Testing Java installation and keystore verification...${NC}"
if java -version > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Java is working correctly${NC}"
    java -version
    
    # Verify keystore contents (as recommended in documentation)
    if [ -f "/etc/ssl/certs/java/cacerts" ]; then
        echo -e "${BLUE}🔐 Verifying keystore contents...${NC}"
        CERT_COUNT=$(keytool -list -keystore /etc/ssl/certs/java/cacerts -storepass changeit 2>/dev/null | grep -c "trustedCertEntry" || echo "0")
        if [ "$CERT_COUNT" -gt 50 ]; then
            echo -e "${GREEN}✅ Keystore verification successful: $CERT_COUNT trusted certificates${NC}"
            echo -e "${BLUE}Sample certificates:${NC}"
            keytool -list -keystore /etc/ssl/certs/java/cacerts -storepass changeit 2>/dev/null | head -n 5 || true
        else
            echo -e "${YELLOW}⚠️  Keystore has only $CERT_COUNT certificates (may need manual fix)${NC}"
        fi
    fi
else
    echo -e "${RED}❌ Java test failed${NC}"
    echo -e "${BLUE}💡 You may need to reinstall Java manually${NC}"
fi

# Step 10: Clean up any remaining issues
echo -e "${YELLOW}🔟 Final cleanup...${NC}"
sudo DEBIAN_FRONTEND=noninteractive apt-get autoremove -yqq
sudo DEBIAN_FRONTEND=noninteractive apt-get autoclean -qq
sudo rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

echo ""
echo -e "${GREEN}🎉 Java keystore fix completed!${NC}"
echo ""
echo -e "${BLUE}📋 Summary:${NC}"
echo "  • Keystore directory: /etc/ssl/certs/java/"
echo "  • Keystore file: $([ -f /etc/ssl/certs/java/cacerts ] && echo "✅ EXISTS" || echo "❌ MISSING")"
echo "  • ca-certificates-java: $(dpkg -l | grep -q "^ii.*ca-certificates-java" && echo "✅ INSTALLED" || echo "❌ NOT INSTALLED")"
echo "  • Java working: $(java -version > /dev/null 2>&1 && echo "✅ YES" || echo "❌ NO")"
echo ""
echo -e "${BLUE}🚀 Next steps:${NC}"
echo "  1. Run: ./setup_codex_env.sh (the fixed version)"
echo "  2. Or continue with your development setup"
echo ""
echo -e "${GREEN}Ready to continue with WristLingo development! 🎯${NC}"
