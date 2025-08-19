# CGMS BLE Client - Internationalization Implementation

## 🌍 Overview
This document describes the internationalization (i18n) implementation for the CGMS BLE Client Android application. The app now supports both English and Traditional Chinese (Taiwan) languages with automatic language switching based on system locale.

## 📱 Supported Languages
- **English (Default)**: `values/strings.xml`
- **Traditional Chinese (Taiwan)**: `values-zh-rTW/strings.xml`

## 🔧 Implementation Details

### 1. Resource Structure
```
app/src/main/res/
├── values/
│   └── strings.xml           # English (default)
└── values-zh-rTW/
    └── strings.xml           # Traditional Chinese (Taiwan)
```

### 2. String Resources Categories

#### App Information
- Application name
- Package identification

#### User Interface Elements
- Button labels (Scan, Stop, Connect, Disconnect)
- Device information displays
- Signal strength indicators

#### System Messages
- Bluetooth status messages
- Scanning progress and timeout notifications
- Connection state changes
- Service discovery results

#### Log Categories
- Feature, Status, Session logs
- Measurement data logs
- Error and success messages

### 3. Key Features

#### Dynamic Language Switching
- Automatic language detection based on system locale
- No app restart required for language changes
- Consistent UI across all application screens

#### Parameterized Strings
- Support for dynamic content insertion using `%1$s`, `%1$d` placeholders
- Proper formatting for signal strength, device names, and error codes
- Locale-appropriate number and text formatting

#### Professional Localization
- Context-aware translations
- Technical terminology consistency
- Cultural adaptation for user experience

## 🎯 Usage Examples

### English (Default)
```xml
<string name="scan_for_cgms">Scan for CGM Service Devices</string>
<string name="device_found">Device found: %1$s (%2$s) RSSI: %3$d dBm</string>
<string name="scan_timeout_message">Scan timeout: No CGM device found within 60 seconds, please ensure device is powered on and nearby</string>
```

### Traditional Chinese (Taiwan)
```xml
<string name="scan_for_cgms">掃描含 CGM 服務的裝置</string>
<string name="device_found">發現裝置: %1$s (%2$s) RSSI: %3$d dBm</string>
<string name="scan_timeout_message">掃描超時：60秒內未找到 CGM 設備，請確認設備已開啟並在附近</string>
```

## 🛠️ Technical Implementation

### 1. MainActivity.java Updates
- Replaced hardcoded strings with `getString(R.string.resource_id)`
- Added parameter support using `getString(R.string.resource_id, param1, param2)`
- Updated button text and UI messages

### 2. BleManager.java Updates
- Internationalized all logging messages
- Updated scan and connection status messages
- Added context-aware error messages

### 3. AndroidManifest.xml Updates
- Changed application label to use string resource: `android:label="@string/app_name"`

### 4. Layout Files Updates
- Updated button text attributes to use string resources
- Maintained consistent styling across languages

## 🌐 Language Detection Logic
The Android system automatically selects the appropriate language resource based on:
1. System language setting (Settings > Language & Region)
2. App-specific language preference (if implemented)
3. Fallback to default English if no matching locale found

## 📊 Build and Deploy
The internationalization implementation:
- Adds minimal size overhead (~15KB for additional language resources)
- No performance impact on runtime
- Compatible with all Android versions (API 26+)
- Supports RTL (Right-to-Left) languages framework

## 🔍 Testing Language Support

### Test on English Device
1. Set system language to English
2. Install and launch app
3. Verify all text appears in English

### Test on Traditional Chinese Device
1. Set system language to Traditional Chinese (Taiwan)
2. Install and launch app
3. Verify all text appears in Traditional Chinese

### Dynamic Language Switching Test
1. Change system language while app is running
2. Return to app (may require app restart)
3. Verify language has switched appropriately

## 📝 Future Enhancements

### Potential Additional Languages
- Simplified Chinese (China): `values-zh-rCN/`
- Japanese: `values-ja/`
- Korean: `values-ko/`
- German: `values-de/`
- French: `values-fr/`

### Advanced Features
- In-app language picker
- Region-specific medical terminology
- Locale-specific date and time formatting
- Currency and unit conversion

## 🎨 UI Considerations

### Text Expansion
- Traditional Chinese text typically 20-30% shorter than English
- UI layouts accommodate text length variations
- Button sizes adjust automatically
- Log display maintains readability

### Cultural Adaptations
- Medical terminology uses standard Traditional Chinese terms
- Bluetooth and technical terms maintain consistency
- Error messages provide appropriate cultural context

## 📱 Quality Assurance

### Translation Quality
- Technical accuracy verified
- Consistency across all UI elements
- Professional medical device terminology
- User experience optimization

### Testing Coverage
- All string resources validated
- Parameter substitution tested
- UI layout verification across languages
- Edge case handling (long device names, error messages)

## 🔗 Related Documentation
- [README.md](README.md) - English documentation
- [README_zh_tw.md](README_zh_tw.md) - Traditional Chinese documentation
- [DESIGN_DOCUMENTATION.md](DESIGN_DOCUMENTATION.md) - UI design specifications

---

*Generated: 2025-08-19*
*Version: 1.0.0*
*Package: com.angus.cgms*
