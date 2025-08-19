package com.angus.cgms;

import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.*;

public class BleManager {
    private static final String TAG = "BleManager";
    
    public interface Logger { void log(String s); }
    public interface ConnectionStateCallback { void onConnectionStateChanged(boolean isConnected); }
    public interface ScanningStateCallback { void onScanningStateChanged(boolean isScanning); }
    public interface DeviceFoundCallback { void onDeviceFound(BluetoothDevice device, int rssi); }

    private final Context ctx;
    private final BluetoothAdapter adapter;
    private final Logger logger;
    private final ConnectionStateCallback connectionCallback;
    private final ScanningStateCallback scanningCallback;
    private final DeviceFoundCallback deviceFoundCallback;

    public static final UUID CGMS_SERVICE =
            UUID.fromString("0000181F-0000-1000-8000-00805F9B34FB");
    public static final UUID CGM_MEASUREMENT =
            UUID.fromString("00002AA7-0000-1000-8000-00805F9B34FB");
    public static final UUID CGM_FEATURE =
            UUID.fromString("00002AA8-0000-1000-8000-00805F9B34FB");
    public static final UUID CGM_STATUS =
            UUID.fromString("00002AA9-0000-1000-8000-00805F9B34FB");
    public static final UUID CGM_SESSION_START_TIME =
            UUID.fromString("00002AAA-0000-1000-8000-00805F9B34FB");
    public static final UUID CGM_SESSION_RUN_TIME =
            UUID.fromString("00002AAB-0000-1000-8000-00805F9B34FB");
    public static final UUID CGM_SPECIFIC_OPS_CP =
            UUID.fromString("00002AAC-0000-1000-8000-00805F9B34FB");
    public static final UUID CCCD =
            UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private boolean isConnected = false;
    private boolean isScanning = false;
    
    private Handler scanHandler;
    private static final long SCAN_TIMEOUT_MS = 60000; // 60秒超時
    private static final long COUNTDOWN_INTERVAL_MS = 5000; // 每5秒顯示一次倒計時
    private int remainingSeconds;
    
    // 儲存已發現的裝置，避免重複顯示
    private Set<String> foundDeviceAddresses = new HashSet<>();

    public BleManager(Context ctx, BluetoothAdapter adapter, Logger logger, ConnectionStateCallback connectionCallback, ScanningStateCallback scanningCallback, DeviceFoundCallback deviceFoundCallback) {
        this.ctx = ctx; 
        this.adapter = adapter; 
        this.logger = logger;
        this.connectionCallback = connectionCallback;
        this.scanningCallback = scanningCallback;
        this.deviceFoundCallback = deviceFoundCallback;
        this.scanHandler = new Handler(Looper.getMainLooper());
    }

    @SuppressLint("MissingPermission")
    public void startScanForCgmsService() {
        if (adapter == null || !adapter.isEnabled()) { logger.log(ctx.getString(R.string.bluetooth_not_enabled)); return; }
        scanner = adapter.getBluetoothLeScanner();
        if (scanner == null) { logger.log(ctx.getString(R.string.scanner_failed)); return; }

        remainingSeconds = (int)(SCAN_TIMEOUT_MS / 1000); // Calculate remaining seconds from timeout
        foundDeviceAddresses.clear(); // Clear previously found device records
        logger.log(ctx.getString(R.string.scan_start, remainingSeconds));
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(CGMS_SERVICE))
                .build();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        
        isScanning = true;
        if (scanningCallback != null) scanningCallback.onScanningStateChanged(true);
        
        scanner.startScan(Collections.singletonList(filter), settings, scanCb);
        
        // 設置60秒超時
        scanHandler.postDelayed(scanTimeoutRunnable, SCAN_TIMEOUT_MS);
        // 開始倒計時顯示
        scanHandler.postDelayed(countdownRunnable, COUNTDOWN_INTERVAL_MS);
    }

    // Countdown display
    private Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            remainingSeconds -= (int)(COUNTDOWN_INTERVAL_MS / 1000); // Use interval time to calculate seconds to subtract
            if (remainingSeconds > 0) {
                Log.i(TAG, ctx.getString(R.string.scanning_countdown, remainingSeconds));
                logger.log(ctx.getString(R.string.scanning_countdown, remainingSeconds));
                scanHandler.postDelayed(this, COUNTDOWN_INTERVAL_MS);
            }
        }
    };

    // Scan timeout handling
    private Runnable scanTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, ctx.getString(R.string.scan_timeout));
            scanHandler.removeCallbacks(countdownRunnable); // Stop countdown
            stopScan();
            logger.log(ctx.getString(R.string.scan_timeout_message));
        }
    };    @SuppressLint("MissingPermission")
    public void stopScan() {
        if (scanner != null && isScanning) {
            Log.i(TAG, ctx.getString(R.string.stop_scan));
            logger.log(ctx.getString(R.string.stop_scan));
            scanner.stopScan(scanCb);
            isScanning = false;
            if (scanningCallback != null) scanningCallback.onScanningStateChanged(false);
        }
        // Cancel timeout and countdown handling
        scanHandler.removeCallbacks(scanTimeoutRunnable);
        scanHandler.removeCallbacks(countdownRunnable);
    }

    @SuppressLint("MissingPermission")
    public void close() {
        // Cancel timeout and countdown handling
        scanHandler.removeCallbacks(scanTimeoutRunnable);
        scanHandler.removeCallbacks(countdownRunnable);
        
        if (scanner != null && isScanning) {
            scanner.stopScan(scanCb);
            isScanning = false;
            if (scanningCallback != null) scanningCallback.onScanningStateChanged(false);
        }
        if (gatt != null) {
            gatt.disconnect();
            gatt.close();
            gatt = null;
        }
        isConnected = false;
        if (connectionCallback != null) connectionCallback.onConnectionStateChanged(false);
    }

    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (gatt != null) {
            logger.log(ctx.getString(R.string.active_disconnect));
            gatt.disconnect();
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public boolean isScanning() {
        return isScanning;
    }
    
    // Manually connect to selected device
    @SuppressLint("MissingPermission")
    public void connectToDevice(BluetoothDevice device) {
        logger.log(ctx.getString(R.string.user_selected_connect, device.getAddress()));
        stopScanAndConnect(device);
    }

    private final ScanCallback scanCb = new ScanCallback() {
        @Override public void onScanResult(int callbackType, ScanResult result) {
            if (result == null || result.getDevice() == null) return;
            BluetoothDevice dev = result.getDevice();
            
            // Avoid displaying the same device repeatedly
            if (foundDeviceAddresses.contains(dev.getAddress())) return;
            foundDeviceAddresses.add(dev.getAddress());
            
            String deviceName = dev.getName() != null ? dev.getName() : ctx.getString(R.string.unknown_device);
            int rssi = result.getRssi();
            logger.log(ctx.getString(R.string.device_found, deviceName, dev.getAddress(), rssi));
            
            // Notify UI about new device found, let user choose
            if (deviceFoundCallback != null) {
                deviceFoundCallback.onDeviceFound(dev, rssi);
            }
        }

        @Override public void onScanFailed(int errorCode) {
            logger.log(ctx.getString(R.string.scan_failed, errorCode));
            isScanning = false;
            if (scanningCallback != null) scanningCallback.onScanningStateChanged(false);
            // Cancel timeout and countdown handling
            scanHandler.removeCallbacks(scanTimeoutRunnable);
            scanHandler.removeCallbacks(countdownRunnable);
        }
    };

    @SuppressLint("MissingPermission")
    private void stopScanAndConnect(BluetoothDevice dev) {
        // Cancel timeout and countdown handling since device is found
        scanHandler.removeCallbacks(scanTimeoutRunnable);
        scanHandler.removeCallbacks(countdownRunnable);
        
        if (scanner != null && isScanning) {
            scanner.stopScan(scanCb);
            isScanning = false;
            if (scanningCallback != null) scanningCallback.onScanningStateChanged(false);
        }
        gatt = dev.connectGatt(ctx, false, gattCb, BluetoothDevice.TRANSPORT_LE);
    }

    private final BluetoothGattCallback gattCb = new BluetoothGattCallback() {
        @Override public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                logger.log(ctx.getString(R.string.connection_state_error, status)); 
                isConnected = false;
                if (connectionCallback != null) connectionCallback.onConnectionStateChanged(false);
                return;
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                logger.log(ctx.getString(R.string.connected_discovering));
                isConnected = true;
                if (connectionCallback != null) connectionCallback.onConnectionStateChanged(true);
                g.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                logger.log(ctx.getString(R.string.disconnected));
                isConnected = false;
                if (connectionCallback != null) connectionCallback.onConnectionStateChanged(false);
                if (gatt != null) {
                    gatt.close();
                    gatt = null;
                }
            }
        }

        @Override public void onServicesDiscovered(BluetoothGatt g, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) { logger.log(ctx.getString(R.string.service_discovery_failed, status)); return; }

            BluetoothGattService svc = g.getService(CGMS_SERVICE);
            if (svc == null) { logger.log(ctx.getString(R.string.cgm_service_not_found)); return; }
            logger.log(ctx.getString(R.string.cgm_service_found));

            readIfExists(g, svc, CGM_FEATURE);
            readIfExists(g, svc, CGM_STATUS);
            readIfExists(g, svc, CGM_SESSION_START_TIME);
            readIfExists(g, svc, CGM_SESSION_RUN_TIME);

            BluetoothGattCharacteristic meas = getChar(svc, CGM_MEASUREMENT);
            if (meas != null) enableNotify(g, meas);
            else logger.log(ctx.getString(R.string.cgm_measurement_not_found));
        }

        @Override public void onCharacteristicRead(BluetoothGatt g, BluetoothGattCharacteristic c, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) return;
            if (CGM_FEATURE.equals(c.getUuid())) {
                logger.log(ctx.getString(R.string.feature_log, bytesToHex(c.getValue())));
            } else if (CGM_STATUS.equals(c.getUuid())) {
                logger.log(ctx.getString(R.string.status_log, bytesToHex(c.getValue())));
            } else if (CGM_SESSION_START_TIME.equals(c.getUuid())) {
                logger.log(ctx.getString(R.string.session_start_log, bytesToHex(c.getValue())));
            } else if (CGM_SESSION_RUN_TIME.equals(c.getUuid())) {
                logger.log(ctx.getString(R.string.session_run_log, bytesToHex(c.getValue())));
            }
        }

        @Override public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic c) {
            if (CGM_MEASUREMENT.equals(c.getUuid())) {
                byte[] v = c.getValue();
                CgmsParser.CgmMeasurement m = CgmsParser.parseMeasurement(v);
                logger.log(ctx.getString(R.string.measurement_log, m.toString()));
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void readIfExists(BluetoothGatt g, BluetoothGattService svc, UUID uuid) {
        BluetoothGattCharacteristic c = getChar(svc, uuid);
        if (c != null) g.readCharacteristic(c);
    }

    private BluetoothGattCharacteristic getChar(BluetoothGattService svc, UUID uuid) {
        return svc.getCharacteristic(uuid);
    }

    @SuppressLint("MissingPermission")
    private void enableNotify(BluetoothGatt g, BluetoothGattCharacteristic c) {
        boolean ok = g.setCharacteristicNotification(c, true);
        BluetoothGattDescriptor d = c.getDescriptor(CCCD);
        if (d != null) {
            d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            g.writeDescriptor(d);
            String resultText = ok ? ctx.getString(R.string.ok) : ctx.getString(R.string.fail);
            logger.log(ctx.getString(R.string.subscribe_cgm_measurement, resultText));
        } else {
            logger.log(ctx.getString(R.string.cccd_not_found));
        }
    }

    private static String bytesToHex(byte[] b) {
        if (b == null) return "(null)";
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02X ", x));
        return sb.toString().trim();
    }
}
