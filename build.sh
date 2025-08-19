#!/bin/bash
set -e

# CGMS BLE Client 編譯腳本
# 版本: 2025.08.19

echo "🚀 CGMS BLE Client 編譯腳本"
echo "=================================="

# 檢查是否在正確的目錄
if [ ! -f "settings.gradle" ]; then
    echo "❌ 錯誤: 請在專案根目錄執行此腳本"
    exit 1
fi

# 設置環境變數
export ANDROID_HOME=~/Library/Android/sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools

echo "🔧 檢查環境..."

# 檢查 Android SDK
if [ ! -d "$ANDROID_HOME" ]; then
    echo "❌ 錯誤: Android SDK 未找到"
    echo "請確保 Android SDK 安裝在: $ANDROID_HOME"
    exit 1
fi

# 檢查 Gradle
if ! command -v gradle &> /dev/null; then
    echo "❌ 錯誤: Gradle 未安裝"
    echo "請執行: brew install gradle"
    exit 1
fi

echo "✅ 環境檢查通過"
echo ""

# 顯示版本資訊
echo "📋 環境資訊:"
echo "- Gradle 版本: $(gradle --version | head -3 | tail -1)"
echo "- Android SDK: $ANDROID_HOME"
echo "- Java 版本: $(java --version 2>/dev/null | head -1 || echo "未檢測到")"
echo ""

# 選擇編譯類型
echo "🏗️  請選擇編譯類型:"
echo "1. Debug 版本 (快速編譯)"
echo "2. 完整編譯 (Debug + Release)"
echo "3. 清理編譯 (清理後重新編譯)"

read -p "請輸入選擇 (1-3): " choice

case $choice in
    1)
        echo "🔨 開始編譯 Debug 版本..."
        gradle assembleDebug
        ;;
    2)
        echo "🔨 開始完整編譯..."
        gradle build
        ;;
    3)
        echo "🧹 清理並重新編譯..."
        gradle clean build
        ;;
    *)
        echo "❌ 無效選擇，默認執行完整編譯..."
        gradle build
        ;;
esac

echo ""
echo "✅ 編譯完成!"
echo "=================================="

# 顯示結果
if [ -f "./app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "📱 Debug APK: ./app/build/outputs/apk/debug/app-debug.apk"
    debug_size=$(ls -lh ./app/build/outputs/apk/debug/app-debug.apk | awk '{print $5}')
    echo "   大小: $debug_size"
fi

if [ -f "./app/build/outputs/apk/release/app-release-unsigned.apk" ]; then
    echo "📦 Release APK: ./app/build/outputs/apk/release/app-release-unsigned.apk"
    release_size=$(ls -lh ./app/build/outputs/apk/release/app-release-unsigned.apk | awk '{print $5}')
    echo "   大小: $release_size"
fi

echo ""
echo "🔍 驗證 APK 簽名..."
if ~/Library/Android/sdk/build-tools/34.0.0/apksigner verify ./app/build/outputs/apk/debug/app-debug.apk 2>/dev/null; then
    echo "✅ Debug APK 簽名有效"
else
    echo "❌ Debug APK 簽名問題"
fi

echo ""
echo "📋 安裝方法:"
echo "  adb install ./app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "🎉 編譯腳本執行完成!"
