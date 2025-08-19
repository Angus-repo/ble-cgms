# CGMS BLE App - Technical Specifications

## 🔧 Development Environment
- **Date**: August 19, 2025
- **Android API**: Min 26, Target 33, Compile 34
- **Build Tools**: Gradle 9.0.0, Android Gradle Plugin 8.5.2
- **Java Version**: 21.0.4 LTS
- **Development Platform**: macOS

## 📱 Application Architecture

### Core Components
1. **MainActivity**: Main UI controller with device selection
2. **BleManager**: Bluetooth Low Energy operations manager
3. **CgmsParser**: CGM measurement data parser

### Key Features
- ✅ **Device Discovery**: Scan for CGMS service (UUID: 0x181F)
- ✅ **Device Selection**: User can choose from discovered devices
- ✅ **Auto Timeout**: 60-second scan timeout with countdown
- ✅ **Dark Theme**: Complete dark mode UI design
- ✅ **Signal Strength**: RSSI display with visual indicators

## 🎨 UI/UX Design Specifications

### Layout Structure
```
┌─────────────────────────────────────┐
│   Device List Area (1/3 height)    │
│   Background: #222222               │
│   ┌─────────────────────────────┐   │
│   │ 🔍 Discovered CGM Devices: │   │
│   │ [Device 1 Button]           │   │
│   │ [Device 2 Button]           │   │
│   │ [Device 3 Button]           │   │
│   └─────────────────────────────┘   │
├─────────────────────────────────────┤
│   Control & Log Area (2/3 height)  │
│   Background: #2D2D2D               │
│   ┌─────────────────────────────┐   │
│   │ [Scan] [Disconnect]         │   │
│   │ 📝 System Logs:             │   │
│   │ ┌─────────────────────────┐ │   │
│   │ │ Log display area        │ │   │
│   │ │ Background: #1E1E1E     │ │   │
│   │ └─────────────────────────┘ │   │
│   └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

### Color Specifications
| Component | Color Code | Usage |
|-----------|------------|--------|
| Primary Background | `#1A1A1A` | Main app background |
| Secondary Background | `#2D2D2D` | Control area background |
| Device List Background | `#222222` | Device selection area |
| Log Background | `#1E1E1E` | System log display |
| Primary Text | `#FFFFFF` | Main text color |
| Secondary Text | `#CCCCCC` | Secondary text |
| Accent Color | `#4A90E2` | Buttons and highlights |
| Divider | `#444444` | Separator line |

## 📟 Bluetooth Specifications

### CGMS Service Details
- **Service UUID**: `0000181F-0000-1000-8000-00805F9B34FB`
- **Standard**: Bluetooth SIG CGM Service 1.0.2

### Characteristics
| Name | UUID | Properties |
|------|------|------------|
| CGM Measurement | `00002AA7-0000-1000-8000-00805F9B34FB` | Notify |
| CGM Feature | `00002AA8-0000-1000-8000-00805F9B34FB` | Read |
| CGM Status | `00002AA9-0000-1000-8000-00805F9B34FB` | Read |
| CGM Session Start Time | `00002AAA-0000-1000-8000-00805F9B34FB` | Read |
| CGM Session Run Time | `00002AAB-0000-1000-8000-00805F9B34FB` | Read |
| CGM Specific Ops CP | `00002AAC-0000-1000-8000-00805F9B34FB` | Write, Indicate |

### Scanning Parameters
- **Scan Mode**: Low Latency
- **Timeout**: 60 seconds
- **Countdown Interval**: 5 seconds
- **Filter**: Service UUID 0x181F

## 🔒 Permissions Required

### Android 12+ (API 31+)
- `BLUETOOTH_SCAN` (neverForLocation)
- `BLUETOOTH_CONNECT`
- `BLUETOOTH_ADVERTISE`

### Android 11 and below
- `ACCESS_FINE_LOCATION`

## 📂 File Structure Details

```
ble-cgms/
├── app/
│   ├── build.gradle                 # App-level build config
│   └── src/main/
│       ├── AndroidManifest.xml      # App manifest with permissions
│       ├── java/com/example/cgms/
│       │   ├── MainActivity.java    # Main activity with device selection
│       │   ├── BleManager.java      # BLE operations manager
│       │   └── CgmsParser.java      # CGM data parser
│       └── res/
│           ├── values/
│           │   ├── colors.xml       # Dark theme color definitions
│           │   └── styles.xml       # App theme and button styles
│           ├── drawable/
│           │   ├── dark_button_selector.xml    # Main button states
│           │   └── device_button_selector.xml  # Device button states
│           └── layout/
│               └── activity_main.xml # Main layout with 1:2 split
├── build.gradle                     # Project-level build config
├── settings.gradle                  # Project settings
├── build.sh                         # Basic build script
├── build-deploy.sh                  # Advanced build & deploy script
├── DESIGN_DOCUMENTATION.md          # This design document
└── README.md                        # Project documentation
```

## 🚀 Build & Deployment

### Build Configuration
```gradle
android {
    compileSdk 34
    defaultConfig {
        minSdk 26
        targetSdk 33
        versionCode 1
        versionName "1.0"
    }
    signingConfigs {
        debug {
            storeFile file("debug.keystore")
            storePassword "android"
            keyAlias "androiddebugkey"
            keyPassword "android"
        }
    }
}
```

### Deployment Locations
- **Debug APK**: `/Users/angusluo/Library/CloudStorage/Dropbox/app/ble-cgms/ble-cgms-debug-latest.apk`
- **Release APK**: `/Users/angusluo/Library/CloudStorage/Dropbox/app/ble-cgms/ble-cgms-release-latest.apk`

### Build Commands
```bash
# Quick build
gradle assembleDebug

# Build with deployment
./build-deploy.sh

# Install on device
adb install ble-cgms-debug-latest.apk
```

## 🔄 State Management

### Scanning States
1. **Idle**: Ready to start scanning
2. **Scanning**: Actively scanning with countdown
3. **Device Found**: Devices discovered, user selection pending
4. **Connecting**: Attempting to connect to selected device
5. **Connected**: Successfully connected to CGM device
6. **Timeout**: Scan timeout reached (60 seconds)

### UI State Transitions
```
[Scan Button] -> "Stop Scanning" (while scanning)
[Device Button] -> Connect and stop scanning
[Disconnect Button] -> Disconnect and return to idle
[Timeout] -> Stop scanning, show timeout message
```

## 🎯 Performance Metrics

### Scan Performance
- **Discovery Time**: Typically 5-30 seconds
- **Timeout**: 60 seconds maximum
- **Update Interval**: 5-second countdown updates
- **Memory Usage**: ~8-12MB during scanning

### UI Performance
- **Layout**: Linear layouts for optimal performance
- **Scrolling**: Smooth scrolling for device list
- **Button Response**: Immediate visual feedback
- **Theme Application**: Consistent dark theme throughout

## 🐛 Known Issues & Limitations

### Current Limitations
1. **Single Connection**: Only one CGM device at a time
2. **Android Version**: Requires API 26+ (Android 8.0)
3. **Permissions**: Location permission required on older Android
4. **Service Filter**: Only discovers devices advertising CGMS service

### Future Enhancements
- [ ] Multiple device connection support
- [ ] Data export functionality
- [ ] Historical data visualization
- [ ] Custom notification settings
- [ ] Widget for quick status view

---

*This technical specification covers the complete implementation of the CGMS BLE application with dark theme design and device selection capabilities.*
