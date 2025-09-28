# BLE CGMS 工作流程圖

本檔案包含完整的 BLE CGMS 應用程式工作流程圖，展示從應用啟動到資料接收的完整過程。

## 完整工作流程

```mermaid
graph TB
    Start([應用啟動]) --> Init[初始化 BLE Manager]
    Init --> CheckBT{藍牙是否啟用?}
    CheckBT -->|否| EnableBT[請求啟用藍牙]
    CheckBT -->|是| StartScan[開始掃描 CGM 設備]
    EnableBT --> StartScan
    
    StartScan --> ScanFilter[設置掃描篩選器<br/>Service UUID: 0x181F]
    ScanFilter --> ScanSettings[配置掃描設定<br/>LOW_LATENCY mode<br/>AGGRESSIVE match]
    ScanSettings --> Scanning{掃描中...}
    
    Scanning -->|30秒超時| ScanTimeout[掃描超時]
    ScanTimeout --> AutoRescan{需要自動重掃?}
    AutoRescan -->|是| Delay[延遲 2 秒]
    Delay --> StartScan
    AutoRescan -->|否| End([結束])
    
    Scanning -->|發現設備| DeviceFound[設備發現回調]
    DeviceFound --> UserSelect{用戶選擇設備?}
    UserSelect -->|否| Scanning
    UserSelect -->|是| StopScan[停止掃描]
    
    StopScan --> CheckBond{檢查配對狀態}
    CheckBond -->|未配對| StartBond[啟動藍牙配對]
    StartBond --> BondProcess[配對流程<br/>金鑰交換<br/>AES-128 加密]
    BondProcess --> BondResult{配對結果}
    BondResult -->|失敗| BondFail[配對失敗]
    BondFail --> Reconnect
    BondResult -->|成功| BondSuccess[配對成功]
    
    CheckBond -->|已配對| BondSuccess
    BondSuccess --> ConnectThrottle[連接節流檢查<br/>2秒間隔限制]
    ConnectThrottle --> Connect[建立 GATT 連接]
    
    Connect --> ConnectTimeout[30秒連接超時監控]
    ConnectTimeout --> ConnectResult{連接結果}
    
    ConnectResult -->|失敗<br/>Status -19| SecurityError[安全機制拒絕<br/>頻繁連接保護]
    SecurityError --> Reconnect{重試次數 < 3?}
    Reconnect -->|是| DelayReconnect[延遲 2 秒重連]
    DelayReconnect --> ConnectThrottle
    Reconnect -->|否| End
    
    ConnectResult -->|成功| Connected[連接成功]
    Connected --> MTU[請求較大 MTU<br/>185 bytes]
    MTU --> Discovery[GATT 服務探索]
    
    Discovery --> ServiceCheck{找到 CGM Service?}
    ServiceCheck -->|否| ServiceNotFound[CGM 服務未找到]
    ServiceNotFound --> End
    
    ServiceCheck -->|是| ReadChars[讀取基本特徵值<br/>Feature, Status<br/>Session Info]
    ReadChars --> EnableCCCD[啟用 CCCD 通知]
    
    EnableCCCD --> CCCDQueue[CCCD 隊列處理]
    CCCDQueue --> MeasurementCCCD[啟用 Measurement 通知]
    MeasurementCCCD --> SOCCPCCCD[啟用 SOCP Indication]
    SOCCPCCCD --> CCCDResult{CCCD 寫入結果}
    
    CCCDResult -->|失敗| CCCDFail[CCCD 寫入失敗<br/>可能需重新配對]
    CCCDFail --> CheckBond
    CCCDResult -->|成功| CCCDSuccess[CCCD 啟用成功]
    
    CCCDSuccess --> SendSOCP[發送 SOCP 命令<br/>Get Communication Interval]
    SendSOCP --> StartKeepAlive[開啟 Keep-Alive<br/>3秒間隔狀態檢查]
    StartKeepAlive --> WaitData[等待 CGM 資料]
    
    WaitData --> DataReceived{收到資料?}
    DataReceived -->|Measurement| ParseData[解析 CGM 測量值<br/>血糖、趨勢、品質]
    ParseData --> DisplayData[顯示資料]
    DisplayData --> StopKeepAlive[停止 Keep-Alive]
    StopKeepAlive --> WaitData
    
    DataReceived -->|SOCP Response| ParseSOCP[解析 SOCP 回應]
    ParseSOCP --> WaitData
    
    DataReceived -->|Keep-Alive| ReadStatus[讀取狀態特徵值]
    ReadStatus --> WaitData
    
    WaitData -->|連接中斷| Disconnected[連接中斷]
    Disconnected --> CleanGATT[清理 GATT 資源]
    CleanGATT --> Reconnect

    subgraph Security ["安全機制"]
        BondProcess
        BondResult
        ConnectThrottle
        SecurityError
    end
    
    subgraph BLEStack ["BLE 協議堆疊"]
        ScanFilter
        Connect
        Discovery
        EnableCCCD
    end
    
    subgraph CGMProtocol ["CGM 協議處理"]
        ParseData
        ParseSOCP
        SendSOCP
        ReadStatus
    end
    
    style Security fill:#ffebee
    style BLEStack fill:#e8f5e8
    style CGMProtocol fill:#fff3e0
    style SecurityError fill:#ffcdd2
    style CCCDFail fill:#ffcdd2
    style BondFail fill:#ffcdd2
```

## 流程說明

### 1. 初始化階段
- **應用啟動**: 初始化 BLE Manager 和相關組件
- **藍牙檢查**: 確認藍牙功能是否啟用
- **權限驗證**: 確保具備必要的藍牙權限

### 2. 設備發現階段
- **掃描設置**: 使用 CGM Service UUID (0x181F) 進行篩選
- **掃描策略**: LOW_LATENCY 模式配合 AGGRESSIVE 匹配
- **超時處理**: 30 秒超時後自動重新掃描
- **用戶選擇**: 讓用戶從發現的設備中選擇目標

### 3. 安全連接階段
- **配對檢查**: 驗證設備是否已完成藍牙配對
- **金鑰交換**: 透過 Android BLE 堆疊處理 SMP 協議
- **加密建立**: 使用 AES-128 建立安全通道
- **節流機制**: 2 秒間隔限制防止頻繁連接

### 4. GATT 通信階段
- **服務探索**: 發現 CGM Service 和相關特徵值
- **MTU 協商**: 請求較大的 MTU (185 bytes) 提升效率
- **CCCD 啟用**: 分階段啟用通知和指示功能
- **隊列處理**: 避免 CCCD 操作衝突

### 5. CGM 協議階段
- **基本資訊**: 讀取 Feature, Status, Session Info
- **通知訂閱**: 啟用 Measurement 和 SOCP 通知
- **Keep-Alive**: 定期狀態檢查直到收到資料
- **資料解析**: 處理血糖測量值、趨勢和品質資訊

### 6. 錯誤處理機制
- **Status -19 錯誤**: CGM 安全機制拒絕，觸發重連邏輯
- **CCCD 失敗**: 可能需要重新配對或延遲重試
- **連接中斷**: 自動清理資源並嘗試重新連接
- **重試限制**: 最多 3 次重連嘗試避免無限循環

## 關鍵技術點

### 連接節流機制
```java
private static final long CONNECT_THROTTLE_MS = 2000;
```
防止頻繁連接觸發 CGM 設備的保護機制。

### CCCD 隊列處理
```java
private final Deque<UUID> cccdQueue = new ArrayDeque<>();
```
確保 CCCD 操作的順序性和可靠性。

### Keep-Alive 機制
```java
private static final long KEEPALIVE_INTERVAL_MS = 3000;
```
在收到第一筆測量資料前保持連接活躍。

### 超時保護
- 掃描超時: 30 秒
- 連接超時: 30 秒  
- CCCD 超時: 5 秒

這個流程圖完整展示了 BLE CGMS 應用程式從啟動到資料接收的所有關鍵步驟和錯誤處理邏輯。