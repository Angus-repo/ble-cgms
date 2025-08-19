# BLE CGMS 應用程式功能流程圖

```mermaid
graph TD
    A[啟動應用程式] --> B{檢查藍牙狀態}
    B -->|藍牙未啟用| C[顯示藍牙未啟用訊息]
    B -->|藍牙已啟用| D[顯示主界面]
    
    D --> E[掃描按鈕: 可用]
    D --> F[中斷連線按鈕: 隱藏]
    D --> G[日誌顯示區域: 空白]
    
    E --> H[用戶點擊掃描按鈕]
    H --> I{檢查權限}
    
    I -->|Android 12+| J[請求藍牙掃描/連線權限]
    I -->|Android 11-| K[請求位置權限]
    
    J --> L{權限獲取結果}
    K --> L
    
    L -->|權限被拒絕| M[顯示權限錯誤訊息]
    L -->|權限授予| N[開始 BLE 掃描]
    
    N --> N1[更新按鈕狀態: 停止掃描]
    N1 --> N2[中斷連線按鈕: 隱藏]
    N2 --> O[過濾 CGM Service 0x181F]
    
    %% 掃描過程中的停止功能
    N1 --> N3[用戶可點擊停止掃描]
    N3 --> N4[停止 BLE 掃描]
    N4 --> N5[恢復按鈕狀態: 掃描含 CGM 服務的裝置]
    N5 --> D
    
    O --> P{發現 CGM 設備?}
    
    P -->|未發現| Q[顯示掃描失敗訊息]
    P -->|發現設備| R[顯示設備資訊]
    
    R --> S[自動停止掃描]
    S --> T[嘗試連接設備]
    
    T --> U{連接結果}
    
    U -->|連接失敗| V[顯示連接錯誤]
    U -->|連接成功| W[更新UI狀態]
    
    W --> X[掃描按鈕: 顯示已連線/不可用]
    W --> Y[中斷連線按鈕: 顯示並可用]
    W --> Z[開始服務發現]
    
    Z --> AA[發現 CGM 服務 0x181F]
    AA --> BB[讀取 CGM Feature 0x2AA8]
    AA --> CC[讀取 CGM Status 0x2AA9]
    AA --> DD[讀取 Session Start Time 0x2AAA]
    AA --> EE[讀取 Session Run Time 0x2AAB]
    AA --> FF[訂閱 CGM Measurement 0x2AA7]
    
    BB --> GG[顯示設備功能資訊]
    CC --> HH[顯示設備狀態]
    DD --> II[顯示會話開始時間]
    EE --> JJ[顯示會話運行時間]
    FF --> KK[等待測量通知]
    
    KK --> LL[接收血糖數據]
    LL --> MM[解析測量數據]
    MM --> NN[顯示血糖濃度]
    MM --> OO[顯示時間偏移]
    MM --> PP[顯示趨勢資訊 - 如果支援]
    MM --> QQ[顯示品質指標 - 如果支援]
    MM --> RR[顯示感測器狀態]
    
    LL --> KK
    
    Y --> SS[用戶點擊中斷連線]
    SS --> TT[主動斷開連接]
    TT --> UU[清理資源]
    UU --> VV[重置UI狀態]
    
    VV --> WW[掃描按鈕: 恢復掃描功能]
    VV --> XX[中斷連線按鈕: 隱藏]
    VV --> YY[顯示斷線訊息]
    
    %% 錯誤處理路徑
    V --> ZZ[回到待機狀態]
    Q --> ZZ
    M --> ZZ
    C --> AAA[等待用戶啟用藍牙]
    
    %% 應用生命週期
    ZZ --> BBB{應用是否關閉?}
    AAA --> BBB
    YY --> BBB
    
    BBB -->|否| D
    BBB -->|是| CCC[清理所有資源]
    CCC --> DDD[關閉應用程式]
    
    %% 樣式定義
    classDef startEnd fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef process fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef decision fill:#fff8e1,stroke:#f57c00,stroke-width:2px
    classDef data fill:#e8f5e8,stroke:#2e7d32,stroke-width:2px
    classDef ui fill:#fce4ec,stroke:#c2185b,stroke-width:2px
    classDef error fill:#ffebee,stroke:#d32f2f,stroke-width:2px
    
    class A,DDD startEnd
    class N,S,T,Z,AA,BB,CC,DD,EE,FF,TT,UU process
    class B,I,L,P,U,BBB decision
    class LL,MM,NN,OO,PP,QQ,RR data
    class E,F,G,W,X,Y,VV,WW,XX ui
    class C,M,Q,V,ZZ error
```

## 功能說明

### 🎯 主要功能模組

#### **1. 藍牙管理模組**
- 檢查藍牙狀態
- 權限管理 (依 Android 版本)
- 設備掃描與過濾

#### **2. 連接管理模組**
- 自動連接發現的設備
- 連接狀態追蹤
- 主動斷線功能

#### **3. GATT 服務模組**
- 服務發現
- 特性讀取
- 通知訂閱

#### **4. 數據處理模組**
- CGM 數據解析
- 實時數據顯示
- 狀態資訊解釋

#### **5. 用戶界面模組**
- 按鈕狀態管理
- 即時日誌顯示
- 用戶互動回饋

### 🔄 狀態轉換

| 狀態 | 掃描按鈕 | 中斷連線按鈕 | 說明 |
|------|----------|--------------|------|
| 初始狀態 | "掃描含 CGM 服務的裝置" (啟用) | 隱藏 | 應用啟動後的預設狀態 |
| 掃描中 | "停止掃描" (啟用) | 隱藏 | 正在掃描 BLE 設備，可隨時停止 |
| 已連線 | "已連線" (停用) | "中斷連線" (顯示並啟用) | 成功連接到 CGM 設備 |
| 斷線後 | "掃描含 CGM 服務的裝置" (啟用) | 隱藏 | 連線中斷後回到初始狀態 |

### 📊 數據流

**輸入數據流：**
CGM 設備 → BLE 通知 → 數據解析 → UI 顯示

**輸出數據流：**
用戶操作 → 按鈕事件 → BLE 命令 → 設備回應
