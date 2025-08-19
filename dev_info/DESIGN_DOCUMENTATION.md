# CGMS BLE 應用程式暗色主題設計文檔

## 📅 專案資訊
- **設計日期**: 2025年8月19日
- **版本**: v2.0 - 暗色主題版本
- **設計師**: GitHub Copilot AI Assistant
- **專案類型**: Android BLE CGMS 客戶端應用程式

## 🎨 設計概述

### 設計目標
- 實現現代化的暗色主題設計
- 提供黑底白字的護眼界面
- 優化用戶體驗與視覺層次
- 實現裝置選擇功能的界面重構

### 主要改進
1. **界面分割**: 上下兩區塊 1:2 比例分割
2. **色彩系統**: 完整的暗色主題色彩系統
3. **交互設計**: 裝置選擇替代自動連接
4. **視覺反饋**: 按鈕狀態與信號強度指示

## 🌙 色彩系統

### 主色調 (Primary Colors)
```xml
<color name="primary_dark">#1A1A1A</color>         <!-- 主要背景色 - 深黑 -->
<color name="secondary_dark">#2D2D2D</color>       <!-- 次要背景色 - 深灰 -->
<color name="surface_dark">#333333</color>         <!-- 表面色 - 中灰 -->
<color name="accent_dark">#4A90E2</color>          <!-- 強調色 - 藍色 -->
```

### 文字色彩 (Text Colors)
```xml
<color name="text_primary_dark">#FFFFFF</color>    <!-- 主要文字 - 白色 -->
<color name="text_secondary_dark">#CCCCCC</color>  <!-- 次要文字 - 淺灰 -->
<color name="text_hint_dark">#888888</color>       <!-- 提示文字 - 暗灰 -->
```

### 功能色彩 (Functional Colors)
```xml
<color name="device_list_bg_dark">#222222</color>  <!-- 設備列表背景 -->
<color name="log_bg_dark">#1E1E1E</color>          <!-- 日誌背景 -->
<color name="divider_dark">#444444</color>         <!-- 分隔線 -->
<color name="border_dark">#555555</color>          <!-- 邊框 -->
```

## 📱 界面結構

### 上方區塊 (1/3 高度)
- **功能**: 裝置列表選擇區域
- **背景色**: `#222222`
- **內容**: 
  - 標題: "🔍 發現的 CGM 裝置："
  - 可滾動的裝置按鈕列表
  - 每個裝置顯示名稱、MAC地址、信號強度

### 下方區塊 (2/3 高度)
- **功能**: 控制和日誌區域
- **背景色**: `#2D2D2D`
- **內容**:
  - 控制按鈕: 掃描/停止掃描、中斷連線
  - 日誌區域: 系統運行日誌顯示

### 分隔設計
- **分隔線**: 3dp厚度，顏色 `#444444`
- **邊距**: 垂直8dp邊距

## 🎯 交互設計

### 按鈕設計
1. **主要按鈕 (掃描/中斷)**
   - 背景: `#4A90E2` (藍色)
   - 按下狀態: 更亮的藍色
   - 禁用狀態: `#666666` (灰色)
   - 圓角: 8dp

2. **裝置按鈕**
   - 背景: `#3A3A3A` (深灰)
   - 按下狀態: `#4A90E2` (藍色高亮)
   - 邊框: 1dp `#555555`
   - 圓角: 12dp
   - 高度: 最少 80dp

### 信號強度指示
- **強信號 (≥ -60dBm)**: 📶
- **中等信號 (-70 ~ -61dBm)**: 📵
- **弱信號 (< -70dBm)**: 📵

## 🔧 技術實現

### 主題系統
```xml
<style name="AppTheme.Dark" parent="Theme.AppCompat.NoActionBar">
    <item name="colorPrimary">@color/primary_dark</item>
    <item name="colorPrimaryDark">@color/primary_dark</item>
    <item name="colorAccent">@color/accent_dark</item>
    <item name="android:windowBackground">@color/primary_dark</item>
    <item name="android:textColorPrimary">@color/text_primary_dark</item>
    <item name="buttonStyle">@style/DarkButton</item>
</style>
```

### 按鈕樣式系統
- **DarkButton**: 主要控制按鈕樣式
- **DeviceButton**: 裝置選擇按鈕樣式
- **使用 Selector 實現狀態效果**

### BLE 功能增強
1. **裝置發現機制**
   - 新增 `DeviceFoundCallback` 介面
   - 防重複顯示機制
   - 動態添加裝置到界面

2. **手動連接功能**
   - `connectToDevice(BluetoothDevice)` 方法
   - 用戶選擇後停止掃描並連接
   - 顯示連接狀態反饋

## 📂 檔案結構

```
app/src/main/res/
├── values/
│   ├── colors.xml          # 色彩定義
│   └── styles.xml          # 主題樣式
├── drawable/
│   ├── dark_button_selector.xml     # 主按鈕選擇器
│   └── device_button_selector.xml   # 裝置按鈕選擇器
└── layout/
    └── activity_main.xml   # 主界面佈局

app/src/main/java/com/example/cgms/
├── MainActivity.java       # 主活動 - 界面邏輯
└── BleManager.java        # BLE 管理器 - 藍牙邏輯
```

## 🚀 部署資訊

### 編譯狀態
- ✅ **編譯成功**: 無編譯錯誤
- ✅ **主題應用**: 已套用 AppTheme.Dark
- ✅ **功能驗證**: 裝置選擇功能正常

### 部署位置
```bash
/Users/angusluo/Library/CloudStorage/Dropbox/app/ble-cgms/
├── ble-cgms-debug-latest.apk      # 最新 Debug 版本
└── ble-cgms-release-latest.apk    # 最新 Release 版本
```

### 版本資訊
- **APK 大小**: 約 5.5MB (Debug) / 4.4MB (Release)
- **最後更新**: 2025-08-19 15:50
- **版本代號**: 20250819_155000

## 📋 使用指南

### 安裝方式
```bash
adb install '/Users/angusluo/Library/CloudStorage/Dropbox/app/ble-cgms/ble-cgms-debug-latest.apk'
```

### 使用流程
1. **開啟應用程式** - 自動套用暗色主題
2. **點擊掃描** - 開始搜尋 CGM 裝置
3. **查看裝置列表** - 上方區域顯示發現的裝置
4. **選擇裝置** - 點擊裝置按鈕連接
5. **查看日誌** - 下方區域顯示系統日誌

## 🎯 設計特色

### 用戶體驗
- 🌙 **護眼設計**: 深色背景減少眼部疲勞
- 🎯 **清晰層次**: 不同深淺區分功能區域
- 👆 **直觀操作**: 點擊即可選擇裝置
- 📱 **現代感**: 符合當前暗色主題趨勢

### 技術亮點
- 🎨 **完整主題系統**: 全面的色彩和樣式定義
- 🔄 **狀態反饋**: 按鈕按下和連接狀態視覺反饋
- 📶 **信號指示**: 直觀的信號強度顯示
- 🛡️ **防重複機制**: 避免重複顯示同一裝置

---

*此文檔記錄了 CGMS BLE 應用程式暗色主題設計的完整實現過程和技術細節。*
