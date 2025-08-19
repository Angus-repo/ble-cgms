#!/bin/bash
set -e

# CGMS BLE Client Build and Deploy Script
# Version: 2025.08.19
# Function: Build APK and automatically copy to Dropbox directory

echo "🚀 CGMS BLE Client Build and Deploy Script"
echo "========================================="

# Check if running in correct directory
if [ ! -f "settings.gradle" ]; then
    echo "❌ Error: Please run this script from project root directory"
    exit 1
fi

# Setup environment variables
export ANDROID_HOME=~/Library/Android/sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools

# Setup deploy target directory
DEPLOY_DIR="/Users/angusluo/Library/CloudStorage/Dropbox/app/ble-cgms"

echo "🔧 Checking environment..."

# Check Android SDK
if [ ! -d "$ANDROID_HOME" ]; then
    echo "❌ Error: Android SDK not found"
    echo "Please ensure Android SDK is installed at: $ANDROID_HOME"
    exit 1
fi

# Check Gradle
if ! command -v gradle &> /dev/null; then
    echo "❌ Error: Gradle not installed"
    echo "Please run: brew install gradle"
    exit 1
fi

# Check and create deploy directory
if [ ! -d "$DEPLOY_DIR" ]; then
    echo "📁 Creating deploy directory: $DEPLOY_DIR"
    mkdir -p "$DEPLOY_DIR"
fi

echo "✅ Environment check passed"
echo ""

# Display version information
echo "📋 Environment Information:"
echo "- Gradle version: $(gradle --version | head -3 | tail -1)"
echo "- Android SDK: $ANDROID_HOME"
echo "- Java version: $(java --version 2>/dev/null | head -1 || echo "Not detected")"
echo "- Deploy directory: $DEPLOY_DIR"
echo ""

# Choose build type
echo "🏗️  Please choose build type:"
echo "1. Debug version (Quick build and deploy)"
echo "2. Full build (Debug + Release and deploy)"
echo "3. Clean build (Clean then rebuild and deploy)"

read -p "Please enter choice (1-3): " choice

case $choice in
    1)
        echo "🔨 Starting Debug version build..."
        gradle assembleDebug
        ;;
    2)
        echo "🔨 Starting full build..."
        gradle build
        ;;
    3)
        echo "🧹 Clean and rebuild..."
        gradle clean build
        ;;
    *)
        echo "❌ Invalid choice, executing default full build..."
        gradle build
        ;;
esac

echo ""
echo "✅ Build completed!"
echo "=================================="

# Check build results and deploy
SUCCESS=false

if [ -f "./app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "📱 Found Debug APK"
    debug_size=$(ls -lh ./app/build/outputs/apk/debug/app-debug.apk | awk '{print $5}')
    echo "   Size: $debug_size"
    
    # Copy Debug APK
    TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
    DEBUG_TARGET="$DEPLOY_DIR/ble-cgms-debug-${TIMESTAMP}.apk"
    cp ./app/build/outputs/apk/debug/app-debug.apk "$DEBUG_TARGET"
    echo "✅ Debug APK deployed to: $DEBUG_TARGET"
    
    # Create latest version symbolic link
    LATEST_DEBUG="$DEPLOY_DIR/ble-cgms-debug-latest.apk"
    ln -sf "$(basename "$DEBUG_TARGET")" "$LATEST_DEBUG"
    echo "🔗 Latest version link: $LATEST_DEBUG"
    
    SUCCESS=true
fi

if [ -f "./app/build/outputs/apk/release/app-release-unsigned.apk" ]; then
    echo "📦 Found Release APK"
    release_size=$(ls -lh ./app/build/outputs/apk/release/app-release-unsigned.apk | awk '{print $5}')
    echo "   Size: $release_size"
    
    # Copy Release APK
    TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
    RELEASE_TARGET="$DEPLOY_DIR/ble-cgms-release-${TIMESTAMP}.apk"
    cp ./app/build/outputs/apk/release/app-release-unsigned.apk "$RELEASE_TARGET"
    echo "✅ Release APK deployed to: $RELEASE_TARGET"
    
    # Create latest version symbolic link
    LATEST_RELEASE="$DEPLOY_DIR/ble-cgms-release-latest.apk"
    ln -sf "$(basename "$RELEASE_TARGET")" "$LATEST_RELEASE"
    echo "🔗 Latest version link: $LATEST_RELEASE"
    
    SUCCESS=true
fi

if [ "$SUCCESS" = true ]; then
    echo ""
    echo "🔍 Verifying Debug APK signature..."
    if ~/Library/Android/sdk/build-tools/34.0.0/apksigner verify ./app/build/outputs/apk/debug/app-debug.apk 2>/dev/null; then
        echo "✅ Debug APK signature is valid"
    else
        echo "❌ Debug APK signature issue"
    fi
    
    echo ""
    echo "📂 Deploy directory contents:"
    ls -la "$DEPLOY_DIR"
    
    echo ""
    echo "📋 Installation method:"
    echo "  adb install '$LATEST_DEBUG'"
    echo ""
    echo "📊 Deploy statistics:"
    DEPLOY_COUNT=$(find "$DEPLOY_DIR" -name "ble-cgms-*.apk" | wc -l)
    TOTAL_SIZE=$(du -sh "$DEPLOY_DIR" | cut -f1)
    echo "  Number of deployed APKs: $DEPLOY_COUNT"
    echo "  Total space used: $TOTAL_SIZE"
    
    # Clean up old versions (keep latest 5 versions)
    echo ""
    echo "🧹 Cleaning up old versions..."
    find "$DEPLOY_DIR" -name "ble-cgms-debug-*.apk" -not -name "*latest*" | sort -r | tail -n +6 | while read file; do
        echo "🗑️  Deleting old version: $(basename "$file")"
        rm -f "$file"
    done
    
    find "$DEPLOY_DIR" -name "ble-cgms-release-*.apk" -not -name "*latest*" | sort -r | tail -n +6 | while read file; do
        echo "🗑️  Deleting old version: $(basename "$file")"
        rm -f "$file"
    done
    
    echo ""
    echo "🎉 Build and deploy completed!"
else
    echo "❌ No compiled APK files found"
    exit 1
fi
