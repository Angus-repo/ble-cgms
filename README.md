# CGMS BLE Client (Android, Java)

> **⚠️ Experimental Project - No Compatible CGM Device Available for Testing**
> 
> This project is developed for experimental and educational purposes only, designed to research the implementation of the Bluetooth Low Energy CGMS protocol. The developer currently does not have access to real devices that comply with the Bluetooth SIG CGM Service (0x181F) standard for testing, and all functionality implementations are based on protocol specifications and theoretical design.

## 🔬 Project Description

This project is a minimal viable BLE direct-connection **Continuous Glucose Monitoring Service (0x181F)** client:
- Scans for devices containing CGMS and provides device selection functionality
- Connect, discover services, read Feature/Status/Session*
- Subscribe to **CGM Measurement (0x2AA7)** and parse key fields (flags, SFLOAT concentration, time offset, etc.)
- **Active disconnect functionality and dark theme design**

### ⚠️ Important Disclaimer
- This project is for experimental and educational purposes only
- Has not been tested with actual CGM devices
- Not recommended for medical use or clinical environments
- Actual commercial CGM devices may use proprietary protocols or services

## 📱 Features

### 🎯 Core Functions
- **Device Scanning**: Automatically scan and filter BLE devices containing CGM Service (0x181F)
- **Device Selection**: Display discovered device list, users can manually select device to connect
- **Signal Strength Display**: Show RSSI values and signal strength icons for each device
- **Service Discovery**: Automatically read CGM Feature, Status, Session information
- **Real-time Monitoring**: Subscribe and receive real-time glucose measurement data (theoretical implementation)
- **Active Disconnect**: Support user-initiated disconnect and re-scan
- **Scan Timeout**: 60-second scan timeout mechanism with remaining time display every 5 seconds

### 🎨 User Interface Features
- **Dark Theme**: Complete dark theme design with black background and white text for eye protection
- **Dual Layout**: Upper device selection area (1/3) + Lower control/log area (2/3)
- **Smart Buttons**: Scan/Stop scanning button automatically switches based on state
- **Real-time Feedback**: All operations and device discoveries displayed instantly
- **Device Information**: Complete display of device name, MAC address, signal strength

### 🔧 Technical Specifications
- **Minimum Android Version**: 8.0 (API 26)
- **Target Android Version**: 13 (API 33)
- **Compile Android Version**: 14 (API 34)
- **Bluetooth Protocol**: BLE (Bluetooth Low Energy)
- **GATT Service**: CGM Service (0x181F)
- **Data Format**: IEEE 11073 SFLOAT
- **Package Name**: `com.angus.cgms`
- **Theme**: Dark theme design

## 🚀 Build Instructions

### 📋 System Requirements

#### macOS Environment
- **macOS**: 10.14+ (Recommended 11.0+)
- **Xcode Command Line Tools**: Installed
- **Homebrew**: For installing Gradle

#### Required Components
- **Android SDK**: API 26-34
- **Java**: JDK 17+ (will be automatically installed)
- **Gradle**: 9.0+ (will be automatically installed)

### 🔧 Environment Setup

#### 1. Install Android SDK
```bash
# Check if Android SDK is already installed
ls ~/Library/Android/sdk

# If not installed, recommend installing Android Studio or using sdkmanager
```

#### 2. Install Gradle (if not already installed)
```bash
# Install Gradle using Homebrew
brew install gradle

# Verify installation
gradle --version
```

#### 3. Setup Environment Variables
```bash
# Set Android SDK path
export ANDROID_HOME=~/Library/Android/sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools

# Recommend adding these environment variables to ~/.zshrc or ~/.bash_profile
echo 'export ANDROID_HOME=~/Library/Android/sdk' >> ~/.zshrc
echo 'export ANDROID_SDK_ROOT=$ANDROID_HOME' >> ~/.zshrc
echo 'export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools' >> ~/.zshrc
```

### 🏗️ Build Steps

#### Quick Build (Debug Version)
```bash
# 1. Navigate to project directory
cd /path/to/ble-cgms

# 2. Setup environment variables (if not permanently set)
export ANDROID_HOME=~/Library/Android/sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools

# 3. Build Debug APK
gradle assembleDebug

# APK output location: ./app/build/outputs/apk/debug/app-debug.apk
```

#### Full Build (Debug + Release)
```bash
# Clean and build all versions
gradle clean build

# Output files:
# Debug APK:   ./app/build/outputs/apk/debug/app-debug.apk
# Release APK: ./app/build/outputs/apk/release/app-release-unsigned.apk
```

#### One-Click Build Scripts
This project provides two build scripts:

**1. Standard Build Script (`build.sh`)**
```bash
# Create a build script
cat > build.sh << 'EOF'
#!/bin/bash
set -e

# Setup environment variables
export ANDROID_HOME=~/Library/Android/sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools

echo "🔧 Starting CGMS BLE Client build..."

# Clean and build
gradle clean build

echo "✅ Build completed!"
echo "📱 Debug APK:   ./app/build/outputs/apk/debug/app-debug.apk"
echo "📦 Release APK: ./app/build/outputs/apk/release/app-release-unsigned.apk"

# Show file sizes
ls -lh ./app/build/outputs/apk/debug/app-debug.apk ./app/build/outputs/apk/release/app-release-unsigned.apk
EOF

# Make script executable
chmod +x build.sh

# Execute build
./build.sh
```

**2. Build Deploy Script (`build-deploy.sh`)**
- Build APK and automatically deploy to Dropbox cloud directory
- Automatic version management and old version cleanup
- Provide latest version symbolic links

```bash
# Execute build deploy script
./build-deploy.sh

# Options:
# 1. Debug version (quick build and deploy)
# 2. Full build (Debug + Release and deploy)  
# 3. Clean build (clean then rebuild and deploy)
```

For detailed instructions, see [BUILD_SCRIPTS.md](BUILD_SCRIPTS.md)

### 📦 APK Installation

#### Install Using ADB
```bash
# Ensure Android device is connected and USB debugging is enabled
adb devices

# Install Debug APK
adb install ./app/build/outputs/apk/debug/app-debug.apk

# Reinstall (overwrite existing version)
adb install -r ./app/build/outputs/apk/debug/app-debug.apk
```

#### Manual Installation
1. Transfer APK file to Android device
2. Enable "Install apps from unknown sources" on the device
3. Use file manager to tap the APK file for installation

### 🔍 Troubleshooting

#### Common Issue Solutions

**1. Gradle not found**
```bash
# Check if Gradle is installed
which gradle

# If not installed, install using Homebrew
brew install gradle
```

**2. Android SDK path error**
```bash
# Check if SDK path is correct
ls $ANDROID_HOME/platform-tools
ls $ANDROID_HOME/build-tools

# If path is incorrect, reset
export ANDROID_HOME=~/Library/Android/sdk
```

**3. Permission issues**
```bash
# Ensure files have execute permissions
chmod +x gradlew  # If gradlew file exists
```

**4. Build errors**
```bash
# Clean build cache
gradle clean

# Re-sync project
gradle --refresh-dependencies build
```

### 🧪 Verify Build Results

#### Check APK Signature
```bash
# Use Android SDK tools to verify APK
~/Library/Android/sdk/build-tools/34.0.0/apksigner verify --verbose ./app/build/outputs/apk/debug/app-debug.apk
```

#### Check APK Information
```bash
# View basic APK information
~/Library/Android/sdk/build-tools/34.0.0/aapt dump badging ./app/build/outputs/apk/debug/app-debug.apk
```

## 📊 Build Output

### File Structure
```
app/build/outputs/apk/
├── debug/
│   └── app-debug.apk           # Debug version (~5.4MB)
└── release/
    └── app-release-unsigned.apk # Release version (~4.4MB)
```

### APK Characteristics
- **Debug APK**: Automatically signed, can be installed for testing directly
- **Release APK**: Unsigned, requires official signing for release
- **Compatibility**: Android 8.0+ (API 26+)
- **Architecture**: Supports all Android architectures (ARM64, ARM, x86, x86_64)

### Notes
- ⚠️ **Testing Limitations**: Due to the lack of real devices complying with Bluetooth SIG CGM Service (0x181F) standard, all functionality implementations have not been verified through actual testing
- Many commercial CGMs use proprietary GATT protocols and may not be directly connectable with this project
- This project is for learning and researching BLE CGMS protocol only, not recommended for medical use
- Please refer to Bluetooth SIG **CGM Service 1.0.2** specification and extend flags/annunciation/Control Point as needed

## 🎮 Usage Instructions

### Basic Operation Flow
1. **Launch App**: Check Bluetooth status and grant necessary permissions
2. **Scan Devices**: Click "Scan for CGM Service Devices" button
3. **Device Selection**: Select desired device from discovered device list
4. **Receive Data**: Real-time display of glucose measurement data and device status
5. **Disconnect**: Click "Disconnect" button to actively disconnect
6. **Re-scan**: After disconnection, click scan button again to connect to other devices

### Permission Requirements
- **Android 12+**: Requires BLUETOOTH_SCAN and BLUETOOTH_CONNECT permissions
- **Android 11-**: Requires ACCESS_FINE_LOCATION permission
- App will automatically request appropriate permissions based on Android version

### Data Interpretation
- **Glucose Concentration**: Displayed in mg/dL or mmol/L (depending on device configuration)
- **Time Offset**: Minutes relative to session start time
- **Trend Information**: Glucose change trends (if supported by device)
- **Device Status**: Calibration, warnings, error status information

## 📄 License

This project is licensed under the MIT License - see [LICENSE](LICENSE) file for details

## 🤝 Contributing

Issues and Pull Requests are welcome!

## 📞 Contact

For questions or suggestions, please contact via GitHub Issues.

---

*Read this in other languages: [繁體中文](README_zh_tw.md)*
