# CGMS BLE Client (Android, Java)

> **⚠️ 實驗性質專案 - 目前無符合的 CGM 裝置可測試**
> 
> 此專案僅為實驗和學習目的而開發，用於研究 Bluetooth Low Energy CGMS 協議的實現。目前開發者手邊沒有符合 Bluetooth SIG CGM Service (0x181F) 標準的真實設備進行測試，所有功能實現均基於協議規範和理論設計。

## 🔬 專案說明

此專案為最小可用的 BLE 直連 **Continuous Glucose Monitoring Service (0x181F)** 客戶端：
- 掃描含 CGMS 的裝置並提供設備選擇功能
- 連線、發現服務、讀取 Feature/Status/Session*
- 訂閱 **CGM Measurement (0x2AA7)** 並解析關鍵欄位（flags、SFLOAT 濃度、time offset 等）
- **主動中斷連線功能和暗色主題設計**

### ⚠️ 重要聲明
- 本專案僅供實驗和教育目的使用
- 未經過真實 CGM 設備的實際測試
- 不建議用於醫療用途或臨床環境
- 實際的商用 CGM 設備可能使用私有協議或專有服務

## 📱 功能特色

### 🎯 核心功能
- **設備掃描**: 自動掃描並過濾含有 CGM Service (0x181F) 的 BLE 設備
- **設備選擇**: 顯示發現的設備列表，用戶可手動選擇連接設備
- **信號強度顯示**: 顯示各設備的 RSSI 值和信號強度圖示
- **服務探索**: 自動讀取 CGM Feature、Status、Session 資訊
- **實時監測**: 訂閱並接收即時血糖測量數據 (理論實現)
- **主動斷線**: 支援用戶主動中斷連接並重新掃描
- **掃描超時**: 60 秒掃描超時機制，每 5 秒顯示剩餘時間

### 🎨 用戶界面特色
- **暗色主題**: 完整的暗色主題設計，黑底白字護眼界面
- **雙區塊佈局**: 上方設備選擇區 (1/3) + 下方控制日誌區 (2/3)
- **智能按鈕**: 掃描/停止掃描按鈕，根據狀態自動切換
- **即時反饋**: 所有操作和設備發現即時顯示
- **設備資訊**: 完整顯示設備名稱、MAC 地址、信號強度

### 🔧 技術規格
- **最低 Android 版本**: 8.0 (API 26)
- **目標 Android 版本**: 13 (API 33)
- **編譯 Android 版本**: 14 (API 34)
- **藍牙協議**: BLE (Bluetooth Low Energy)
- **GATT 服務**: CGM Service (0x181F)
- **數據格式**: IEEE 11073 SFLOAT
- **Package Name**: `com.angus.cgms`
- **主題**: 暗色主題設計

## 🚀 編譯方法

### 📋 系統需求

#### macOS 環境
- **macOS**: 10.14+ (推薦 11.0+)
- **Xcode Command Line Tools**: 已安裝
- **Homebrew**: 用於安裝 Gradle

#### 必要組件
- **Android SDK**: API 26-34
- **Java**: JDK 17+ (會自動安裝)
- **Gradle**: 9.0+ (會自動安裝)

### 🔧 環境設置

#### 1. 安裝 Android SDK
```bash
# 檢查是否已安裝 Android SDK
ls ~/Library/Android/sdk

# 如果未安裝，建議安裝 Android Studio 或使用 sdkmanager
```

#### 2. 安裝 Gradle (如果尚未安裝)
```bash
# 使用 Homebrew 安裝 Gradle
brew install gradle

# 驗證安裝
gradle --version
```

#### 3. 設置環境變數
```bash
# 設置 Android SDK 路徑
export ANDROID_HOME=~/Library/Android/sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools

# 建議將這些環境變數加入 ~/.zshrc 或 ~/.bash_profile
echo 'export ANDROID_HOME=~/Library/Android/sdk' >> ~/.zshrc
echo 'export ANDROID_SDK_ROOT=$ANDROID_HOME' >> ~/.zshrc
echo 'export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools' >> ~/.zshrc
```

### 🏗️ 編譯步驟

#### 快速編譯 (Debug 版本)
```bash
# 1. 切換到專案目錄
cd /path/to/ble-cgms

# 2. 設置環境變數 (如果未永久設置)
export ANDROID_HOME=~/Library/Android/sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools

# 3. 編譯 Debug APK
gradle assembleDebug

# APK 輸出位置: ./app/build/outputs/apk/debug/app-debug.apk
```

#### 完整編譯 (Debug + Release)
```bash
# 清理並編譯所有版本
gradle clean build

# 輸出文件:
# Debug APK:   ./app/build/outputs/apk/debug/app-debug.apk
# Release APK: ./app/build/outputs/apk/release/app-release-unsigned.apk
```

#### 一鍵編譯腳本
本專案提供了兩個編譯腳本：

**1. 標準編譯腳本 (`build.sh`)**
```bash
# 創建一個編譯腳本
cat > build.sh << 'EOF'
#!/bin/bash
set -e

# 設置環境變數
export ANDROID_HOME=~/Library/Android/sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools

echo "🔧 開始編譯 CGMS BLE Client..."

# 清理並編譯
gradle clean build

echo "✅ 編譯完成!"
echo "📱 Debug APK:   ./app/build/outputs/apk/debug/app-debug.apk"
echo "📦 Release APK: ./app/build/outputs/apk/release/app-release-unsigned.apk"

# 顯示文件大小
ls -lh ./app/build/outputs/apk/debug/app-debug.apk ./app/build/outputs/apk/release/app-release-unsigned.apk
EOF

# 使腳本可執行
chmod +x build.sh

# 執行編譯
./build.sh
```

**2. 編譯部署腳本 (`build-deploy.sh`)**
- 編譯 APK 並自動部署到 Dropbox 雲端目錄
- 自動版本管理和舊版本清理
- 提供最新版本符號連結

```bash
# 執行編譯部署腳本
./build-deploy.sh

# 選項:
# 1. Debug 版本 (快速編譯並部署)
# 2. 完整編譯 (Debug + Release 並部署)  
# 3. 清理編譯 (清理後重新編譯並部署)
```

詳細說明請參考 [BUILD_SCRIPTS.md](BUILD_SCRIPTS.md)

### 📦 APK 安裝

#### 使用 ADB 安裝
```bash
# 確保 Android 設備已連接並啟用 USB 調試
adb devices

# 安裝 Debug APK
adb install ./app/build/outputs/apk/debug/app-debug.apk

# 重新安裝 (覆蓋現有版本)
adb install -r ./app/build/outputs/apk/debug/app-debug.apk
```

#### 手動安裝
1. 將 APK 文件傳輸到 Android 設備
2. 在設備上啟用「允許安裝未知來源的應用」
3. 使用文件管理器點擊 APK 文件安裝

### 🔍 故障排除

#### 常見問題解決

**1. Gradle 無法找到**
```bash
# 檢查 Gradle 是否已安裝
which gradle

# 如果未安裝，使用 Homebrew 安裝
brew install gradle
```

**2. Android SDK 路徑錯誤**
```bash
# 檢查 SDK 路徑是否正確
ls $ANDROID_HOME/platform-tools
ls $ANDROID_HOME/build-tools

# 如果路徑不正確，重新設置
export ANDROID_HOME=~/Library/Android/sdk
```

**3. 權限問題**
```bash
# 確保文件具有執行權限
chmod +x gradlew  # 如果存在 gradlew 文件
```

**4. 編譯錯誤**
```bash
# 清理編譯緩存
gradle clean

# 重新同步專案
gradle --refresh-dependencies build
```

### 🧪 驗證編譯結果

#### 檢查 APK 簽名
```bash
# 使用 Android SDK 工具驗證 APK
~/Library/Android/sdk/build-tools/34.0.0/apksigner verify --verbose ./app/build/outputs/apk/debug/app-debug.apk
```

#### 檢查 APK 資訊
```bash
# 查看 APK 基本資訊
~/Library/Android/sdk/build-tools/34.0.0/aapt dump badging ./app/build/outputs/apk/debug/app-debug.apk
```

## 📊 編譯輸出

### 文件結構
```
app/build/outputs/apk/
├── debug/
│   └── app-debug.apk           # Debug 版本 (約 5.4MB)
└── release/
    └── app-release-unsigned.apk # Release 版本 (約 4.4MB)
```

### APK 特性
- **Debug APK**: 自動簽名，可直接安裝測試
- **Release APK**: 未簽名，需要正式簽名後發佈
- **兼容性**: Android 8.0+ (API 26+)
- **架構**: 支援所有 Android 架構 (ARM64, ARM, x86, x86_64)

### 注意
- ⚠️ **測試限制**: 由於手邊沒有符合 Bluetooth SIG CGM Service (0x181F) 標準的真實設備，所有功能實現均未經過實際測試驗證
- 許多市售 CGM 使用私有 GATT 協議，可能無法用本專案直連
- 本專案僅供學習和研究 BLE CGMS 協議使用，不建議用於醫療用途
- 請參考 Bluetooth SIG **CGM Service 1.0.2** 規範，依需求擴充 flags/annunciation/Control Point

## 🎮 使用方法

### 基本操作流程
1. **啟動應用**: 檢查藍牙狀態並授予必要權限
2. **掃描設備**: 點擊「掃描含 CGM 服務的裝置」按鈕
3. **自動連接**: 應用會自動連接到發現的 CGM 設備
4. **接收數據**: 即時顯示血糖測量數據和設備狀態
5. **中斷連線**: 點擊「中斷連線」按鈕可主動斷開連接
6. **重新掃描**: 斷線後可重新點擊掃描按鈕連接其他設備

### 權限說明
- **Android 12+**: 需要 BLUETOOTH_SCAN 和 BLUETOOTH_CONNECT 權限
- **Android 11-**: 需要 ACCESS_FINE_LOCATION 權限
- 應用會根據 Android 版本自動請求相應權限

### 數據解讀
- **血糖濃度**: 以 mg/dL 或 mmol/L 顯示 (依設備配置)
- **時間偏移**: 相對於會話開始時間的分鐘數
- **趨勢資訊**: 血糖變化趨勢 (如果設備支援)
- **設備狀態**: 校準、警告、錯誤等狀態資訊

## 📄 授權條款

本專案採用 MIT License 授權 - 詳見 [LICENSE](LICENSE) 文件

## 🤝 貢獻

歡迎提交 Issue 和 Pull Request！

## 📞 聯繫

如有問題或建議，請通過 GitHub Issues 聯繫。
