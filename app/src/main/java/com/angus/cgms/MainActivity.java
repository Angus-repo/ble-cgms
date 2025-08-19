package com.angus.cgms;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TextView log;
    private Button btnScan, btnDisconnect;
    private LinearLayout deviceListLayout;
    private BleManager ble;
    private boolean isScanning = false;
    
    // 儲存發現的裝置
    private List<BluetoothDevice> foundDevices = new ArrayList<>();

    private final ActivityResultLauncher<String[]> permLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                startScan();
            });

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        log = findViewById(R.id.txtLog);
        btnScan = findViewById(R.id.btnScan);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        deviceListLayout = findViewById(R.id.deviceListLayout);

        BluetoothManager bm = getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = bm.getAdapter();
        ble = new BleManager(this, adapter, this::appendLog, this::onConnectionStateChanged, this::onScanningStateChanged, this::onDeviceFound);

        btnScan.setOnClickListener(v -> {
            if (isScanning) {
                stopScan();
            } else {
                ensurePermsAndScan();
            }
        });
        btnDisconnect.setOnClickListener(v -> disconnect());
    }

    private void ensurePermsAndScan() {
        if (Build.VERSION.SDK_INT >= 31) {
            permLauncher.launch(new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            });
        } else {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permLauncher.launch(new String[]{ Manifest.permission.ACCESS_FINE_LOCATION });
                return;
            }
            startScan();
        }
    }

    private void startScan() { 
        foundDevices.clear();
        runOnUiThread(() -> deviceListLayout.removeAllViews());
        ble.startScanForCgmsService(); 
    }

    private void stopScan() {
        ble.stopScan();
    }

    private void disconnect() {
        ble.disconnect();
    }
    
    // 發現新裝置時的回調
    private void onDeviceFound(BluetoothDevice device, int rssi) {
        runOnUiThread(() -> {
            foundDevices.add(device);
            addDeviceToList(device, rssi);
        });
    }
    
    // 在界面上添加裝置按鈕
    private void addDeviceToList(BluetoothDevice device, int rssi) {
        Button deviceButton = new Button(this);
        String deviceName = device.getName() != null ? device.getName() : getString(R.string.unknown_device);
        
        // 設置按鈕文字，包含信號強度圖示
        String rssiIcon = getRssiIcon(rssi);
        deviceButton.setText(rssiIcon + " " + deviceName + "\n" + device.getAddress() + "\n" + getString(R.string.signal_strength, rssi));
        
        // 使用暗色主題樣式
        deviceButton.setBackgroundResource(R.drawable.device_button_selector);
        deviceButton.setTextColor(getResources().getColor(R.color.text_primary_dark));
        deviceButton.setTextSize(12);
        
        deviceButton.setOnClickListener(v -> {
            appendLog(getString(R.string.user_selected_connect, deviceName));
            ble.connectToDevice(device);
        });
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(8, 6, 8, 6);
        deviceButton.setLayoutParams(params);
        
        deviceListLayout.addView(deviceButton);
    }
    
    // 根據信號強度返回對應圖示
    private String getRssiIcon(int rssi) {
        if (rssi >= -50) return "📶"; // 很強
        else if (rssi >= -60) return "📶"; // 強
        else if (rssi >= -70) return "📵"; // 中等
        else if (rssi >= -80) return "📶"; // 弱
        else return "📵"; // 很弱
    }

    private void onScanningStateChanged(boolean scanning) {
        runOnUiThread(() -> {
            isScanning = scanning;
            if (scanning) {
                btnScan.setText(getString(R.string.stop_scanning));
                btnDisconnect.setVisibility(Button.GONE);
                // 開始掃描時清除之前的裝置列表
                deviceListLayout.removeAllViews();
                foundDevices.clear();
            } else if (!ble.isConnected()) {
                btnScan.setText(getString(R.string.scan_for_cgms));
                btnDisconnect.setVisibility(Button.GONE);
            }
        });
    }

    private void onConnectionStateChanged(boolean isConnected) {
        runOnUiThread(() -> {
            if (isConnected) {
                btnScan.setText(getString(R.string.connected));
                btnScan.setEnabled(false);
                btnDisconnect.setVisibility(Button.VISIBLE);
                btnDisconnect.setEnabled(true);
            } else {
                btnScan.setText(getString(R.string.scan_for_cgms));
                btnScan.setEnabled(true);
                btnDisconnect.setVisibility(Button.GONE);
            }
            isScanning = false;
        });
    }

    private void appendLog(@NonNull String s) {
        runOnUiThread(() -> {
            log.append(s);
            log.append("\n");
        });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        ble.close();
    }
}
