#!/bin/bash
echo "Testing CI-compatible build..."

# Set CI environment variables
export CI=true
export ANDROID_COMPILE_SDK=34
export ANDROID_TARGET_SDK=34
export ANDROID_BUILD_TOOLS_VERSION=34.0.0

# Run the same commands as in your CI
./gradlew clean \
    -Pandroid.compileSdk=34 \
    -Pandroid.targetSdk=34 \
    -Pandroid.buildToolsVersion=34.0.0 \
    :app:externalNativeBuildOfflineDebug \
    :app:assembleOfflineDebug \
    :wear:assembleOfflineDebug \
    :app:testOfflineDebug \
    --stacktrace --no-daemon --warning-mode all

echo "CI build test complete!"