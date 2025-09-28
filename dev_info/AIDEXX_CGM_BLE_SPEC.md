# AiDEXX CGM 藍牙（BLE）實作規格（掃描／連線／斷線／讀取血糖）
**版本**：v1.0  
**適用平台**：Android 8.0+（API 26+），建議支援至 Android 14/15  
**目的**：提供開發人員可直接落地的 BLE 介面規格，用於與 CGM 發射器（Transmitter）進行掃描、連線、資料收取（血糖）與斷線的完整流程。

> 註：本文以 AiDEXX 反組譯結果為依據，並抽象化為通用規格；涉及專用 UUID/命令之處以占位符表示，供實機比對與實作。【26†source】

---

## 1. 名詞與縮寫
- **CGM**：Continuous Glucose Monitoring，連續血糖監測。
- **GATT**：Generic Attribute Profile。
- **Characteristic**：GATT 特徵值。
- **Descriptor**：GATT 描述元。
- **Notify**：GATT 通知機制。
- **Adapter/Controller**：此處指應用層 BLE 介面實作與其控制器。

---

## 2. 系統需求與權限
- **必要權限**
  - Android 12L/13+：`BLUETOOTH_SCAN`、`BLUETOOTH_CONNECT`（若需廣播則 `BLUETOOTH_ADVERTISE`）。啟動掃描前須檢查 `BLUETOOTH_SCAN` 是否核可，否則不得啟動掃描。【26†source】
- **藍牙需求**
  - 需透過 `BluetoothManager` 取得 `BluetoothAdapter` 與 `BluetoothLeScanner`；若 `BluetoothLeScanner == null`，掃描需立即失敗返回。【26†source】

---

## 3. 服務 UUID 與裝置篩選
- **掃描 Service UUID**：`0000181F-0000-1000-8000-00805F9B34FB`（CGM Service）。所有掃描請以此 Service UUID 作為 Filter，以降低誤觸裝置與耗電。【26†source】
- **Characteristics（占位符）**：
  - `UUID_CGM_MEASUREMENT`：血糖資料通知來源（實機比對後填入）
  - `UUID_..._WRITE`：寫入通道（如有）
  - `UUID_OTA_CONTROL`：OTA 控制（若需，AiDEXX 以 `OtaManager.OTA_CONTROL` 判斷）【26†source】

> 註：標準 CGM Service 定義由 SIG 公布；不同廠商可能擴充或改造。請以實機與服務發現結果為準。

---

## 4. 掃描規格
### 4.1 掃描設定
- Filter：以 `ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("0000181F-..."))` 建構。【26†source】
- Settings：
  - `setScanMode(SCAN_MODE_LOW_LATENCY)`（=2）
  - `setMatchMode(MATCH_MODE_AGGRESSIVE)`（=1）
  - 若裝置支援 offloaded scan batching：`setReportDelay(0)`
  - `setCallbackType(CALLBACK_TYPE_ALL_MATCHES)`（=1）【26†source】

### 4.2 掃描流程
- `startScan()`：
  1. 檢查登入／業務前置條件（如使用者已登入）。【26†source】
  2. 檢查 `BluetoothLeScanner != null` 與權限。
  3. 啟動掃描：`bluetoothLeScanner.startScan(filters, settings, scanCallback)`。
  4. 定時停止：以工作排程（WorkManager）在 30 秒後呼叫 `stopScan()`（Tag 建議 `"1002"`）。【26†source】

- `stopScan()`：
  1. 取消所有掃描相關工作（Tag `"1001"`, `"1002"`）。
  2. 呼叫 `bluetoothLeScanner.stopScan(scanCallback)`。
  3. 若**已配對**但**尚未連線**且**需週期掃描**，以 2 秒延遲重新排程 `startScan()`（Tag 建議 `"1001"`），達到低頻輪詢效果。【26†source】

> 推薦：所有掃描／停掃都透過 WorkManager 加上 Tag 控制，可被集中取消，避免重複排程。AiDEXX 使用 `"1001"`（Start）與 `"1002"`（Stop）。【26†source】

---

## 5. 連線規格
### 5.1 連線節流與狀態
- **節流**：兩次嘗試連線之間應至少相隔 **2000 ms**。若距上次斷線時間未達 2000 ms，延遲至滿足後再 `connectGatt()`。【26†source】
- **狀態旗標**：
  - `isOnConnectState`：進入連線流程時設為 `true`，斷線或失敗時恢復為 `false`。【26†source】
  - `connectStatus`：以 `(state, status)` 配對表示目前 GATT 狀態碼，初始化為 `(-1,-1)`。【26†source】

### 5.2 連線流程（狀態機）
- **入口**：`executeConnect(mac)`
  1. 由 `BluetoothDeviceStore` 取出 `BluetoothDevice`。
  2. 透過 `Handler` 傳送 `CONNECT_GATT` 訊息至工作執行緒，實際執行 `device.connectGatt(context, /*autoConnect=*/false, bluetoothGattCallback, TRANSPORT_LE)`（細節在 Handler/Callback 內部類）。【26†source】
  3. 連線超時上限：**30 秒**（`BLE_CONNECT_TIME_LIMIT = 30000`），超時訊息代碼 `BLE_CONNECT_TIME_OUT = 1100`，應中斷並回報失敗。【26†source】
  4. 服務發現完成後進入 `DISCOVER_SERVICES = 1004` → 成功則 `CONNECT_SUCCESS = 1010`，失敗則 `CONNECT_FAILURE = 1009`；**成功後立即啟用 Notify（§6）**。【26†source】

- **重試**：`retry()` 遇到中斷或錯誤時遞增 `retryNum`，並重新投遞 `CONNECT_GATT` 訊息；應加入最大重試次數或退避策略避免無限重試。【26†source】

---

## 6. 通知（Notify）與資料通道
### 6.1 啟用通知
- 呼叫 `gatt.setCharacteristicNotification(characteristic, true)` 後，對每個 `descriptor` 寫入 `ENABLE_NOTIFICATION_VALUE`。
- **Android 13+ & 非 ColorOS**：使用新 API `gatt.writeDescriptor(descriptor, ENABLE_NOTIFICATION_VALUE)`；其他版本：`descriptor.setValue(...)` 後 `gatt.writeDescriptor(descriptor)`。【26†source】

### 6.2 寫入資料（如需命令交握）
- 單封包最大 20 bytes，若超過需切片遞送。
- 傳輸節流：兩次寫入間隔至少 **20 ms**；若小於 20 ms，延遲補齊再送，以降低 GATT 忙碌導致的寫入失敗。【26†source】
- 依特徵屬性選擇寫入型態：
  - `WRITE`（有回應，prop & 0x04）：API 33+ 使用 `writeCharacteristic(c, data, WRITE_TYPE_DEFAULT)`；舊版 `setWriteType(WRITE_TYPE_DEFAULT)` 後 `writeCharacteristic(c)`。
  - `WRITE_NO_RESPONSE`（prop & 0x08）：API 33+ 使用 `WRITE_TYPE_NO_RESPONSE`；舊版 `setWriteType(WRITE_TYPE_NO_RESPONSE)` 後 `writeCharacteristic(c)`。【26†source】
- 空閒自動斷線：若未開啟 `keepAlive`，最後一次寫入後 **1.5 秒** 傳送 `BLE_IDLE_DISCONNECT` 以節能（可選）。【26†source】

### 6.3 讀取資料
- 以 `executeReadCharacteristic(int uuidInt)` 通知工作執行緒讀取指定特徵值；或依裝置協議以寫入命令觸發資料回推。【26†source】

---

## 7. 血糖資料（讀取／接收）
> 注意：不同廠家在 CGM Service 內的特徵值布局與封包格式可能擴充。以下為實作規格骨架。

### 7.1 訂閱來源
- 在服務發現完成後，定位血糖資料來源的特徵值（例如 `UUID_CGM_MEASUREMENT`），呼叫 §6.1 啟用 Notify。
- 建議在啟用 Notify 前，完成 MTU 協調（如 185）與連線參數最佳化（若裝置支援）。

### 7.2 資料接收與分發
- `receiveData(c, value)`：Callback 端收到封包後，組成 `RECEIVER_DATA` 訊息送入工作執行緒處理；UUID 會轉為整數做路由分派（`ExtendsKt.toIntBigEndian(uuid)`)。【26†source】
- OTA 相關封包應以 `checkOtaData()` 先行攔截，避免污染血糖資料管線。【26†source】

### 7.3 解析流程（範本）
1. 以特徵 UUID 分派至對應 Parser（不同 UUID 代表不同訊息型別）。
2. 依協議解碼：時間戳、血糖值（mg/dL 或 mmol/L）、品質旗標（如可信度、趨勢方向）、感測器狀態（暖機、失連、校正需求）。
3. 解析後透過資料匯流排發送（例如 `MessageDistributor` / 事件匯流排），統一給上層儲存（DB）與 UI 呈現。【26†source】

> AiDEXX 於 OTA 控制特徵值時，會以 `MessageDistributor` 送出 `BleMessage(AidexXOperation.GET_BOOT_VERSION, ...)`，此機制可類比用於資料分發。【26†source】

---

## 8. 斷線規格
### 8.1 主動斷線
- `executeDisconnect()`：若 `mBluetoothGatt != null`，投遞 `DISCONNECT_GATT`；否則直接 `refreshConnectState(false)` 並呼叫 `onDisconnected()`，確保上層狀態一致。【26†source】

### 8.2 資源釋放與快取清理
- 關閉前**先呼叫** `refreshDeviceCache(gatt)`（反射 `BluetoothGatt.refresh()`）以清除系統快取，避免下次服務／特徵表不一致。
- 其後 `gatt.close()`，統一在 `closeGatt()` 執行，並記錄結果／錯誤以便診斷。【26†source】

### 8.3 被動斷線／藍牙關閉
- 當藍牙被停用或系統層斷線時，投遞 `CONNECT_DISCONNECTED` 至工作執行緒做收斂與重試判斷。【26†source】

---

## 9. 錯誤處理與重試策略
- **超時**：30 秒未完成連線／服務發現 → 視為失敗，觸發 `CONNECT_FAILURE` 並依策略重試或回退。【26†source】
- **節流**：兩次連線間隔 < 2000 ms → 延遲至 2000 ms 再試，避免 GATT 疲勞或控制器異常。【26†source】
- **掃描守護**：失敗後維持低頻掃描輪詢（2 秒延遲排程），當目標裝置再次出現時再嘗試連線。【26†source】

---

## 10. 監控、記錄與除錯
- 建議在應用可寫的外部私有目錄建立日誌檔案（如 `Android/data/<pkg>/files/...`），包含「掃描→連線→服務→通知→資料解析」的關鍵節點。
- AiDEXX 以 Xlog 持久化日誌；若採用類似機制，請評估**隱私與加密**需求，避免個資或醫療資料外洩。

---

## 11. 範例介面（Kotlin 片段）
> 片段僅示範規格參考，實做應配合各專案的架構與抽象層。

```kotlin
interface CgmBleClient {
    fun startScan(periodic: Boolean = true)
    fun stopScan(periodic: Boolean = true)

    /** 檢查裝置是否可連線（已在 DeviceStore 中） */
    fun isReadyToConnect(mac: String): Boolean

    /** 進入連線狀態機，具 2s 節流與 30s 超時 */
    fun connect(mac: String)

    /** 主動斷線並清理資源與系統快取 */
    fun disconnect()

    /** 啟用血糖資料通知（服務發現成功後呼叫） */
    fun enableGlucoseNotify()

    /** 若需命令交握（如要求回傳歷史），提供安全寫入與 20ms 節流 */
    fun write(uuid: UUID, payload: ByteArray)

    /** 讀取特徵值（如版本或當前狀態） */
    fun read(uuid: UUID)
}
```

---

## 12. 實作清單（Checklist）
- [ ] 權限流程（Android 12L/13+）與掃描前置檢查
- [ ] 掃描 Filter（Service 0x181F）與 Settings 正確配置【26†source】
- [ ] WorkManager 週期掃描／停掃 Tag 與時序（建議 `"1001"`/`"1002"`）【26†source】
- [ ] 連線狀態機（`CONNECT_GATT`→`DISCOVER_SERVICES`→`CONNECT_SUCCESS/FAILURE`），30s 超時【26†source】
- [ ] 2s 連線節流、錯誤重試與最大次數／退避策略【26†source】
- [ ] 啟用 Notify 的版本分流（API 33+ 與 ColorOS 例外）【26†source】
- [ ] 寫入分片（>20 bytes）與 20ms 節流；`WRITE`/`WRITE_NO_RESPONSE` 分流【26†source】
- [ ] 資料接收分發管線與解析器（含 OTA 特徵值防汙）【26†source】
- [ ] 主動斷線與 `refreshDeviceCache()` → `gatt.close()` 資源釋放【26†source】
- [ ] 日誌與隱私保護
