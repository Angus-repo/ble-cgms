# 編譯腳本說明

本專案提供了兩個編譯腳本，用於不同的編譯需求：

## 📝 腳本概述

### 1. `build.sh` - 標準編譯腳本
- **用途**: 在本地編譯 APK
- **輸出**: APK 文件保留在專案的 build 目錄中
- **適用場景**: 開發測試、本地編譯驗證

### 2. `build-deploy.sh` - 編譯部署腳本
- **用途**: 編譯並自動部署到 Dropbox 目錄
- **輸出**: APK 文件自動複製到 Dropbox 雲端目錄
- **適用場景**: 發布版本、團隊共享、備份存檔

## 🚀 使用方法

### 基本編譯 (build.sh)
```bash
# 執行編譯腳本
./build.sh

# 選項:
# 1. Debug 版本 (快速編譯)
# 2. 完整編譯 (Debug + Release)  
# 3. 清理編譯 (清理後重新編譯)
```

### 編譯並部署 (build-deploy.sh)
```bash
# 執行編譯部署腳本
./build-deploy.sh

# 選項:
# 1. Debug 版本 (快速編譯並部署)
# 2. 完整編譯 (Debug + Release 並部署)
# 3. 清理編譯 (清理後重新編譯並部署)
```

## 📁 部署目錄結構

編譯部署腳本會將 APK 文件組織如下：
```
/Users/angusluo/Library/CloudStorage/Dropbox/app/ble-cgms/
├── ble-cgms-debug-20250819_120220.apk      # 時間戳版本
├── ble-cgms-debug-latest.apk               # 最新版本符號連結
├── ble-cgms-release-20250819_120220.apk    # 時間戳版本
└── ble-cgms-release-latest.apk             # 最新版本符號連結
```

## ✨ 特色功能

### 版本管理
- **時間戳命名**: 每個 APK 都有唯一的時間戳標識
- **最新版本連結**: `*-latest.apk` 始終指向最新版本
- **自動清理**: 保留最近 5 個版本，自動刪除舊版本

### 環境檢測
- **自動環境檢查**: 驗證 Android SDK、Gradle 等必要組件
- **路徑自動設置**: 自動配置 Android SDK 環境變數
- **錯誤提示**: 詳細的錯誤信息和解決建議

### 部署統計
- **文件大小**: 顯示每個 APK 的文件大小
- **部署數量**: 統計已部署的 APK 數量
- **空間占用**: 顯示部署目錄的總空間占用

### APK 驗證
- **簽名檢查**: 自動驗證 Debug APK 的簽名有效性
- **安裝方法**: 提供便捷的 ADB 安裝命令

## 🔧 自定義設置

### 修改部署目錄
如需修改部署目錄，請編輯 `build-deploy.sh` 中的 `DEPLOY_DIR` 變數：
```bash
DEPLOY_DIR="/your/custom/deploy/path"
```

### 修改保留版本數量
如需修改保留的版本數量，請編輯 `build-deploy.sh` 中的清理邏輯：
```bash
# 將 tail -n +6 改為您想要的數量 (保留 N-1 個版本)
tail -n +6  # 保留 5 個版本
```

## 📋 使用建議

### 開發階段
- 使用 `./build.sh` 進行快速本地編譯測試
- 選擇選項 1 (Debug 版本) 進行快速編譯

### 發布階段
- 使用 `./build-deploy.sh` 編譯並部署到雲端
- 選擇選項 3 (清理編譯) 確保乾淨的編譯環境

### 團隊協作
- 團隊成員可以從 Dropbox 目錄獲取最新的 APK 文件
- 使用 `*-latest.apk` 連結始終獲取最新版本

## ⚠️ 注意事項

1. **權限要求**: 確保對 Dropbox 目錄有讀寫權限
2. **網路同步**: Dropbox 需要網路連接進行同步
3. **空間管理**: 定期檢查 Dropbox 空間使用情況
4. **版本控制**: 重要版本建議手動備份或使用 Git tag 標記
