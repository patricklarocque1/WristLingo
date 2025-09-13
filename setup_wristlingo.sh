#!/bin/bash
echo "Setting up WristLingo development environment..."

# Check if we're in the project directory
if [ ! -f "gradlew" ]; then
    echo "Error: Not in WristLingo project directory"
    exit 1
fi

# Make gradlew executable
chmod +x gradlew

# Check Java version
echo "Java version:"
java -version

# Check Android SDK
echo "Android SDK location: $ANDROID_HOME"
if [ -d "$ANDROID_HOME" ]; then
    echo "✓ Android SDK found"
else
    echo "✗ Android SDK not found"
fi

# Check if we can access Windows files
if [ -d "/mnt/c" ]; then
    echo "✓ Windows filesystem accessible at /mnt/c"
else
    echo "✗ Windows filesystem not accessible"
fi

# Test build
echo "Testing Gradle build..."
./gradlew --version

echo "Setup complete!"
