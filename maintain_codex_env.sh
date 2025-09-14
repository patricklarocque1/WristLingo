#!/usr/bin/env bash
set -euo pipefail

echo "🔧 WristLingo Codex Environment Maintenance..."

# ── Config ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
EXPECTED_ANDROID_SDK="36"
EXPECTED_BUILD_TOOLS="36.0.0"
EXPECTED_NDK="29.0.14033849"
EXPECTED_JAVA_MAJOR="21"
DEFAULT_CMAKE_VER="3.22.1"        # AGP 8.x friendly
WL_SMOKE="${WL_SMOKE:-0}"         # 0: skip heavy build smokes, 1: run short smokes
WL_CLEAN="${WL_CLEAN:-0}"         # 0: skip gradle clean, 1: run clean (may be slow)

export DEBIAN_FRONTEND=noninteractive
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:/usr/sbin:/usr/bin:/sbin:/bin"

log() { echo -e "${BLUE}$*${NC}"; }
ok()  { echo -e "${GREEN}$*${NC}"; }
warn(){ echo -e "${YELLOW}$*${NC}"; }
err() { echo -e "${RED}$*${NC}"; }
cmd_exists(){ command -v "$1" >/dev/null 2>&1; }
sdk_latest_installed(){ [ -d "$ANDROID_HOME/platforms" ] && ls "$ANDROID_HOME/platforms" | sed -n 's/^android-//p' | sort -n | tail -1 || true; }
tlim(){ # timeout wrapper: tlim <seconds> <cmd...>
  local secs="$1"; shift
  command -v timeout >/dev/null 2>&1 && timeout --preserve-status "${secs}" "$@" || "$@"
}

# ── Find project ────────────────────────────────────────────────────────────
PROJECT_DIR=""
for p in /workspace/WristLingo /home/*/projects/WristLingo; do
  [ -d "$p" ] && PROJECT_DIR="$p" && break
done

# ── APT quick update ────────────────────────────────────────────────────────
log "🔄 Updating system package lists…"
sudo apt-get update -qq || true

# ── Java truststore self-heal ───────────────────────────────────────────────
log "☕ Verifying Java and CA truststore…"
if ! cmd_exists java; then
  err "Java not found. Run setup_codex_env.sh first."
  exit 1
fi
JAVA_BIN="$(readlink -f "$(command -v java)")"
JAVA_HOME="$(dirname "$(dirname "$JAVA_BIN")")"
JAVA_MAJOR="$(java -version 2>&1 | awk -F[\".] '/version/ {print $2}')"
[ "${JAVA_MAJOR:-0}" = "$EXPECTED_JAVA_MAJOR" ] || warn "Java is ${JAVA_MAJOR:-unknown}, expected ${EXPECTED_JAVA_MAJOR}."

sudo install -d -m 755 /etc/ssl/certs/java
[ -e /etc/ssl/certs/java/cacerts ] || { sudo touch /etc/ssl/certs/java/cacerts; sudo chmod 644 /etc/ssl/certs/java/cacerts; }

# Re-run cert hooks safely; some images have ca-certificates-java preinstalled
if dpkg -l 2>/dev/null | awk '{print $2}' | grep -qx ca-certificates-java; then
  sudo dpkg --triggers-only ca-certificates-java || true
fi
sudo update-ca-certificates -f || true

# Ensure JDK uses system keystore
if [ -d "$JAVA_HOME/lib/security" ]; then
  [ -L "$JAVA_HOME/lib/security/cacerts" ] || sudo rm -f "$JAVA_HOME/lib/security/cacerts" || true
  sudo ln -sf /etc/ssl/certs/java/cacerts "$JAVA_HOME/lib/security/cacerts"
fi
ok "Java truststore verified."

# ── Android cmdline-tools & core SDK bits ───────────────────────────────────
log "📱 Verifying Android cmdline-tools and SDK…"
if ! cmd_exists sdkmanager; then
  warn "cmdline-tools missing; reinstalling…"
  TMP_ZIP="/tmp/cmdtools.zip"
  URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
  mkdir -p "$ANDROID_HOME"
  curl -fsSL "$URL" -o "$TMP_ZIP"
  unzip -q "$TMP_ZIP" -d "$ANDROID_HOME"
  rm -f "$TMP_ZIP"
  mv "$ANDROID_HOME/cmdline-tools" "$ANDROID_HOME/cmdline-tools-temp"
  mkdir -p "$ANDROID_HOME/cmdline-tools/latest"
  mv "$ANDROID_HOME/cmdline-tools-temp"/* "$ANDROID_HOME/cmdline-tools/latest/"
  rm -rf "$ANDROID_HOME/cmdline-tools-temp"
fi

yes | sdkmanager --licenses >/dev/null 2>&1 || true
sdkmanager --update >/dev/null 2>&1 || true

NEED_INSTALL=("platform-tools")
[ -d "$ANDROID_HOME/platforms/android-$EXPECTED_ANDROID_SDK" ] || NEED_INSTALL+=("platforms;android-$EXPECTED_ANDROID_SDK")
[ -d "$ANDROID_HOME/build-tools/$EXPECTED_BUILD_TOOLS" ] || NEED_INSTALL+=("build-tools;$EXPECTED_BUILD_TOOLS")
[ -d "$ANDROID_HOME/ndk/$EXPECTED_NDK" ] || NEED_INSTALL+=("ndk;$EXPECTED_NDK")

# Detect cmake version from Gradle; fallback to default
REQUIRED_CMAKE="$DEFAULT_CMAKE_VER"
if [ -n "$PROJECT_DIR" ]; then
  CANDIDATE="$(grep -RhoE "cmake[[:space:]]*\{[^}]*version[[:space:]]*['\"][0-9]+\.[0-9]+\.[0-9]+['\"]" "$PROJECT_DIR" 2>/dev/null \
    | sed -nE "s/.*version[[:space:]]*['\"]([0-9]+\.[0-9]+\.[0-9]+)['\"].*/\1/p" | head -1 || true)"
  [ -n "$CANDIDATE" ] && REQUIRED_CMAKE="$CANDIDATE"
fi
[ -d "$ANDROID_HOME/cmake/$REQUIRED_CMAKE" ] || NEED_INSTALL+=("cmake;$REQUIRED_CMAKE")

if [ "${#NEED_INSTALL[@]}" -gt 0 ]; then
  warn "Installing/repairing Android components: ${NEED_INSTALL[*]}"
  yes | sdkmanager "${NEED_INSTALL[@]}" || {
    warn "Retrying after re-accepting licenses…"
    yes | sdkmanager --licenses || true
    sdkmanager "${NEED_INSTALL[@]}"
  }
else
  ok "Android SDK components already present."
fi

# PATH for cmake/ninja
if [ -d "$ANDROID_HOME/cmake/$REQUIRED_CMAKE/bin" ]; then
  case ":$PATH:" in *":$ANDROID_HOME/cmake/$REQUIRED_CMAKE/bin:"*) ;; \
    *) export PATH="$ANDROID_HOME/cmake/$REQUIRED_CMAKE/bin:$PATH" ;; esac
fi
if ! cmd_exists ninja; then
  warn "Ninja not found on PATH; adding via APT."
  sudo apt-get install -yqq --no-install-recommends ninja-build
fi
ok "CMake ${REQUIRED_CMAKE} & Ninja ready."

# ── Gradle prewarm (fast, cancellation-safe) ────────────────────────────────
if [ -z "$PROJECT_DIR" ]; then
  warn "WristLingo project directory not found. Skipping Gradle maintenance."
else
  log "📂 Using project: $PROJECT_DIR"
  cd "$PROJECT_DIR"
  chmod +x ./gradlew || true

  # Setup proxy for Gradle/Maven in Codex environment (enables dependency downloads)
  log "🌐 Configuring Codex proxy for Gradle/Maven downloads..."
  export GRADLE_OPTS="${GRADLE_OPTS:-} -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080 -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 -Dhttp.nonProxyHosts=localhost|127.0.0.1"
  export MAVEN_OPTS="${MAVEN_OPTS:-} -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080 -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080"
  
  # Additional Gradle optimizations for Codex environment
  export GRADLE_OPTS="${GRADLE_OPTS} -Dorg.gradle.daemon=false -Dorg.gradle.parallel=false -Dorg.gradle.configureondemand=false"
  
  # Light prewarm only (avoid long tasks that cause context cancellation)
  log "🧊 Light Gradle prewarm (Codex-optimized, no heavy tasks)…"
  
  # Basic version check (fast, safe)
  tlim 90 ./gradlew --version --no-daemon --quiet >/dev/null 2>&1 || warn "Gradle version check timed out"
  
  # Core module help (lightweight, avoids Android SDK checks)
  tlim 90 ./gradlew :core:help --no-daemon --quiet >/dev/null 2>&1 || warn "Core module prewarm timed out"
  
  # Core module tasks only (avoid full project task scan that triggers Android modules)
  tlim 90 ./gradlew :core:tasks --no-daemon --quiet >/dev/null 2>&1 || warn "Core tasks listing timed out"
  
  # Skip full project task scan to prevent Android SDK timeout issues
  # tlim 90 ./gradlew tasks --no-daemon --quiet >/dev/null 2>&1 || true  # DISABLED: causes context cancellation

  # Optional clean (OFF by default; can be slow/trigger native)
  if [ "$WL_CLEAN" = "1" ]; then
    log "🧽 Gradle clean (requested)…"
    tlim 120 ./gradlew clean --no-daemon --quiet || warn "clean hit a timeout/cancel; non-fatal."
  else
    warn "Skipping gradle clean (set WL_CLEAN=1 to enable)."
  fi

  # Optional quick smokes (OFF by default)
  if [ "$WL_SMOKE" = "1" ]; then
    log "🔨 Quick smoke (short timeouts)…"
    tlim 180 ./gradlew :core:build --no-daemon --quiet || warn "Core build failed/timeout; check locally."
    tlim 120 ./gradlew :app:assembleOfflineDebug  --no-daemon --quiet || true
    tlim 120 ./gradlew :wear:assembleOfflineDebug --no-daemon --quiet || true
  else
    warn "Skipping build smokes (set WL_SMOKE=1 to enable)."
  fi
fi

# ── Summary ─────────────────────────────────────────────────────────────────
log "✅ Final environment checks…"
JAVA_LINE="$(java -version 2>&1 | head -1)"
SDK_LINE="$(sdk_latest_installed 2>/dev/null || echo 'none')"
GRADLE_LINE="none"
if [ -n "${PROJECT_DIR:-}" ] && [ -x "$PROJECT_DIR/gradlew" ]; then
  GRADLE_LINE="$("$PROJECT_DIR/gradlew" --version 2>/dev/null | awk '/Gradle / {print $2; exit}')"
fi

ok "Java: ${JAVA_LINE}"
ok "Android SDK top-level API: ${SDK_LINE}"
ok "CMake: ${REQUIRED_CMAKE}  (PATH has $ANDROID_HOME/cmake/$REQUIRED_CMAKE/bin)"
ok "Gradle: ${GRADLE_LINE}"

command -v df >/dev/null 2>&1 && echo -e "💾 Disk usage here: $(df -h . | awk 'END{print $5}')" || true
echo -e "\n${GREEN}🎉 Maintenance complete. Environment is ready for WristLingo builds.${NC}"
