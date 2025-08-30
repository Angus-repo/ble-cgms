package com.angus.cgms;

import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
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
    private boolean servicesDiscovered = false;
    private final Set<UUID> cccdEnabledChars = new HashSet<>();
    private final Set<UUID> cccdInProgressChars = new HashSet<>();
    private final Deque<UUID> cccdQueue = new ArrayDeque<>();
    private boolean cccdOpInFlight = false;
    private static final long CCCD_TIMEOUT_MS = 5000;
    private final Runnable cccdTimeoutRunnable = new Runnable() {
        @Override public void run() {
            if (cccdOpInFlight) {
                logBoth("[CCCD] timeout, resetting and processing next");
                cccdOpInFlight = false;
                cccdInProgressChars.clear();
                processNextCccdInQueue();
            }
        }
    };
    private boolean bondingInProgress = false;
    private BluetoothDevice currentDevice;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private static final long RECONNECT_DELAY_MS = 2000;
    private static final long KEEPALIVE_INTERVAL_MS = 3000;
    
    private Handler scanHandler;
    private static final long SCAN_TIMEOUT_MS = 60000; // 60秒超時
    private static final long COUNTDOWN_INTERVAL_MS = 5000; // 每5秒顯示一次倒計時
    private int remainingSeconds;
    
    // 儲存已發現的裝置，避免重複顯示
    private Set<String> foundDeviceAddresses = new HashSet<>();
    private boolean measurementReceived = false;
    private final Runnable keepAliveRunnable = new Runnable() {
        @Override public void run() {
            if (gatt == null || !isConnected || measurementReceived) return;
            try {
                BluetoothGattService svc = gatt.getService(CGMS_SERVICE);
                if (svc != null) {
                    BluetoothGattCharacteristic st = getChar(svc, CGM_STATUS);
                    if (st != null) gatt.readCharacteristic(st);
                }
            } catch (Exception ignored) {}
            scanHandler.postDelayed(this, KEEPALIVE_INTERVAL_MS);
        }
    };

    public BleManager(Context ctx, BluetoothAdapter adapter, Logger logger, ConnectionStateCallback connectionCallback, ScanningStateCallback scanningCallback, DeviceFoundCallback deviceFoundCallback) {
        this.ctx = ctx; 
        this.adapter = adapter; 
        this.logger = logger;
        this.connectionCallback = connectionCallback;
        this.scanningCallback = scanningCallback;
        this.deviceFoundCallback = deviceFoundCallback;
        this.scanHandler = new Handler(Looper.getMainLooper());

        // Listen for bond state changes to defer CCCD enabling until after bonding
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        ctx.registerReceiver(bondReceiver, filter);
    }

    private void logBoth(String message) {
        Log.i(TAG, message);
        if (logger != null) logger.log(message);
    }

    @SuppressLint("MissingPermission")
    public void startScanForCgmsService() {
    if (adapter == null || !adapter.isEnabled()) { logBoth(ctx.getString(R.string.bluetooth_not_enabled)); return; }
        scanner = adapter.getBluetoothLeScanner();
    if (scanner == null) { logBoth(ctx.getString(R.string.scanner_failed)); return; }

        remainingSeconds = (int)(SCAN_TIMEOUT_MS / 1000); // Calculate remaining seconds from timeout
        foundDeviceAddresses.clear(); // Clear previously found device records
    logBoth(ctx.getString(R.string.scan_start, remainingSeconds));
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
            logBoth(ctx.getString(R.string.scan_timeout_message));
        }
    };    @SuppressLint("MissingPermission")
    public void stopScan() {
        if (scanner != null && isScanning) {
            Log.i(TAG, ctx.getString(R.string.stop_scan));
            logBoth(ctx.getString(R.string.stop_scan));
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
        scanHandler.removeCallbacks(keepAliveRunnable);
        scanHandler.removeCallbacks(cccdTimeoutRunnable);
        
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
    servicesDiscovered = false;
    cccdEnabledChars.clear();
    cccdInProgressChars.clear();
    bondingInProgress = false;
        currentDevice = null;
        if (connectionCallback != null) connectionCallback.onConnectionStateChanged(false);

        try { ctx.unregisterReceiver(bondReceiver); } catch (Exception ignore) {}
    }

    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (gatt != null) {
            logBoth(ctx.getString(R.string.active_disconnect));
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
    logBoth(ctx.getString(R.string.user_selected_connect, device.getAddress()));
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
            logBoth(ctx.getString(R.string.device_found, deviceName, dev.getAddress(), rssi));
            
            // Notify UI about new device found, let user choose
            if (deviceFoundCallback != null) {
                deviceFoundCallback.onDeviceFound(dev, rssi);
            }
        }

        @Override public void onScanFailed(int errorCode) {
            logBoth(ctx.getString(R.string.scan_failed, errorCode));
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
        currentDevice = dev;
        reconnectAttempts = 0;
        // 若尚未配對，先進行配對，待配對完成再連線，確保初次連線即為加密連線
        if (dev.getBondState() != BluetoothDevice.BOND_BONDED) {
            bondingInProgress = dev.createBond();
            logBoth(ctx.getString(R.string.request_bonding));
            return;
        }
        gatt = dev.connectGatt(ctx, false, gattCb, BluetoothDevice.TRANSPORT_LE);
    }

    private final BluetoothGattCallback gattCb = new BluetoothGattCallback() {
        @Override public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                if (status == 19) {
                    logBoth(ctx.getString(R.string.connection_state_error, status) + " (peer terminated: security/multi-connection/idle policy)");
                } else {
                    logBoth(ctx.getString(R.string.connection_state_error, status));
                }
                isConnected = false;
                if (connectionCallback != null) connectionCallback.onConnectionStateChanged(false);
                scheduleReconnectIfNeeded();
                return;
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                logBoth(ctx.getString(R.string.connected_discovering));
                isConnected = true;
                if (connectionCallback != null) connectionCallback.onConnectionStateChanged(true);
                servicesDiscovered = false;
                cccdEnabledChars.clear();
                cccdInProgressChars.clear();
                bondingInProgress = false;
                measurementReceived = false;
                try { g.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH); } catch (Exception ignore) {}
                // Prefer larger MTU for CGM notifications; discovery after MTU change
                boolean mtuReq = g.requestMtu(185);
                if (!mtuReq) {
                    g.discoverServices();
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                logBoth(ctx.getString(R.string.disconnected));
                isConnected = false;
                if (connectionCallback != null) connectionCallback.onConnectionStateChanged(false);
                if (gatt != null) {
                    gatt.close();
                    gatt = null;
                }
                scanHandler.removeCallbacks(keepAliveRunnable);
                scheduleReconnectIfNeeded();
            }
        }

        @Override public void onMtuChanged(BluetoothGatt g, int mtu, int status) {
            // Proceed with discovery regardless of MTU success
            g.discoverServices();
        }

        @Override public void onServicesDiscovered(BluetoothGatt g, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) { logBoth(ctx.getString(R.string.service_discovery_failed, status)); return; }
            servicesDiscovered = true;

            BluetoothGattService svc = g.getService(CGMS_SERVICE);
            if (svc == null) { logBoth(ctx.getString(R.string.cgm_service_not_found)); return; }
            logBoth(ctx.getString(R.string.cgm_service_found));

            // If not bonded, request bond first to avoid security-required writes causing disconnects
            BluetoothDevice device = g.getDevice();
            if (device != null) {
                logBoth("[BondState] at discovery: " + device.getBondState());
            }
            if (device != null && device.getBondState() != BluetoothDevice.BOND_BONDED && !bondingInProgress) {
                bondingInProgress = device.createBond();
                logBoth(ctx.getString(R.string.request_bonding));
                return; // Wait for bond completion to continue
            }

            readIfExists(g, svc, CGM_FEATURE);
            readIfExists(g, svc, CGM_STATUS);
            readIfExists(g, svc, CGM_SESSION_START_TIME);
            readIfExists(g, svc, CGM_SESSION_RUN_TIME);

            BluetoothGattCharacteristic meas = getChar(svc, CGM_MEASUREMENT);
            if (meas != null) {
                // 延遲啟用通知，避免剛完成服務/加密時立即寫入 CCCD 造成斷線
                scheduleEnableNotifyWithDelay(g, meas, 500);
            }
            else logBoth(ctx.getString(R.string.cgm_measurement_not_found));

            // 啟用 Specific Ops Control Point 的 Indication（若裝置支援）以便接收會話控制回應
            BluetoothGattCharacteristic socp = getChar(svc, CGM_SPECIFIC_OPS_CP);
            if (socp != null) {
                scheduleEnableNotifyWithDelay(g, socp, 700);
            }

            // 啟動 keepalive，直到收到第一筆量測
            scanHandler.removeCallbacks(keepAliveRunnable);
            scanHandler.postDelayed(keepAliveRunnable, KEEPALIVE_INTERVAL_MS);
        }

        @Override public void onCharacteristicRead(BluetoothGatt g, BluetoothGattCharacteristic c, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) return;
            if (CGM_FEATURE.equals(c.getUuid())) {
                logBoth(ctx.getString(R.string.feature_log, bytesToHex(c.getValue())));
            } else if (CGM_STATUS.equals(c.getUuid())) {
                logBoth(ctx.getString(R.string.status_log, bytesToHex(c.getValue())));
            } else if (CGM_SESSION_START_TIME.equals(c.getUuid())) {
                logBoth(ctx.getString(R.string.session_start_log, bytesToHex(c.getValue())));
            } else if (CGM_SESSION_RUN_TIME.equals(c.getUuid())) {
                logBoth(ctx.getString(R.string.session_run_log, bytesToHex(c.getValue())));
            }
        }

        @Override public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic c) {
            if (CGM_MEASUREMENT.equals(c.getUuid())) {
                byte[] v = c.getValue();
                CgmsParser.CgmMeasurement m = CgmsParser.parseMeasurement(v);
                logBoth(ctx.getString(R.string.measurement_log, m.toString()));
                measurementReceived = true;
                scanHandler.removeCallbacks(keepAliveRunnable);
            } else if (CGM_SPECIFIC_OPS_CP.equals(c.getUuid())) {
                byte[] v = c.getValue();
                logBoth("[SOCP] " + bytesToHex(v));
            }
        }

        @Override public void onCharacteristicWrite(BluetoothGatt g, BluetoothGattCharacteristic c, int status) {
            if (CGM_SPECIFIC_OPS_CP.equals(c.getUuid())) {
                logBoth("[SOCP->] write status=" + status + " value=" + bytesToHex(c.getValue()));
            }
        }

        @Override public void onDescriptorWrite(BluetoothGatt g, BluetoothGattDescriptor descriptor, int status) {
            if (descriptor != null && CCCD.equals(descriptor.getUuid())) {
                UUID cu = descriptor.getCharacteristic() != null ? descriptor.getCharacteristic().getUuid() : null;
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (cu != null) {
                        cccdEnabledChars.add(cu);
                        cccdInProgressChars.remove(cu);
                    }
                    logBoth(ctx.getString(R.string.cccd_write_success) + (cu != null ? (" [" + cu + "]") : ""));
                    if (cu != null && CGM_SPECIFIC_OPS_CP.equals(cu)) {
                        scanHandler.postDelayed(() -> sendSocpGetCommInterval(), 500);
                    }
                } else {
                    if (cu != null) cccdInProgressChars.remove(cu);
                    logBoth(ctx.getString(R.string.cccd_write_failed, status) + (cu != null ? (" [" + cu + "]") : ""));
                    // If failed due to auth, try bonding then re-enable later
                    BluetoothDevice device = g.getDevice();
                    if (device != null && device.getBondState() != BluetoothDevice.BOND_BONDED && !bondingInProgress) {
                        bondingInProgress = device.createBond();
                        logBoth(ctx.getString(R.string.request_bonding));
                    } else if (servicesDiscovered && cu != null && !cccdEnabledChars.contains(cu)) {
                        BluetoothGattService svc = g.getService(CGMS_SERVICE);
                        if (svc != null) {
                            BluetoothGattCharacteristic ch = svc.getCharacteristic(cu);
                            if (ch != null) scheduleEnableNotifyWithDelay(g, ch, 600);
                        }
                    }
                }
                // Reset timeout and process next CCCD in queue
                scanHandler.removeCallbacks(cccdTimeoutRunnable);
                cccdOpInFlight = false;
                processNextCccdInQueue();
            }
        }
    };

    private void processNextCccdInQueue() {
        if (gatt == null) return;
        while (!cccdQueue.isEmpty()) {
            UUID next = cccdQueue.poll();
            if (next == null) continue;
            if (cccdEnabledChars.contains(next) || cccdInProgressChars.contains(next)) continue;
            BluetoothGattService svc = gatt.getService(CGMS_SERVICE);
            if (svc == null) break;
            BluetoothGattCharacteristic ch = svc.getCharacteristic(next);
            if (ch == null) continue;
            enableNotify(gatt, ch);
            break;
        }
    }

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
        if (c == null) return;
        UUID cu = c.getUuid();
        if (cccdEnabledChars.contains(cu) || cccdInProgressChars.contains(cu)) return;
        boolean ok = g.setCharacteristicNotification(c, true);
        BluetoothGattDescriptor d = c.getDescriptor(CCCD);
        if (d != null) {
            final int props = c.getProperties();
            logBoth("[CGM] Properties notify=" + (((props & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) ? "1" : "0") +
                    ", indicate=" + (((props & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) ? "1" : "0"));
            if ((props & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0 &&
                (props & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
                d.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            } else {
                d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            }
            if (cccdOpInFlight) {
                cccdQueue.offer(cu);
                logBoth("[CCCD] queued [" + cu + "]");
            } else {
                cccdInProgressChars.add(cu);
                cccdOpInFlight = true;
                logBoth("[CCCD] write start [" + cu + "]");
                scanHandler.postDelayed(cccdTimeoutRunnable, CCCD_TIMEOUT_MS);
                g.writeDescriptor(d);
            }
            String resultText = ok ? ctx.getString(R.string.ok) : ctx.getString(R.string.fail);
            logBoth(ctx.getString(R.string.subscribe_cgm_measurement, resultText));
        } else {
            // 列出可用的 descriptors 以利除錯
            List<BluetoothGattDescriptor> all = c.getDescriptors();
            StringBuilder ids = new StringBuilder();
            if (all != null) {
                for (BluetoothGattDescriptor x : all) {
                    ids.append(x.getUuid()).append(" ");
                }
            }
            logBoth("[CGM] No CCCD. descriptors=" + ids.toString().trim());
            logBoth(ctx.getString(R.string.cccd_not_found));
        }
    }    private void scheduleEnableNotifyWithDelay(BluetoothGatt g, BluetoothGattCharacteristic c, long delayMs) {
        if (c == null) return;
        UUID cu = c.getUuid();
        if (cccdEnabledChars.contains(cu) || cccdInProgressChars.contains(cu)) return;
        scanHandler.postDelayed(() -> enableNotify(g, c), delayMs);
    }

    private void continueAfterBonding() {
        if (gatt == null) return;
        BluetoothGattService svc = gatt.getService(CGMS_SERVICE);
        if (svc == null) return;
        readIfExists(gatt, svc, CGM_FEATURE);
        readIfExists(gatt, svc, CGM_STATUS);
        readIfExists(gatt, svc, CGM_SESSION_START_TIME);
        readIfExists(gatt, svc, CGM_SESSION_RUN_TIME);
        BluetoothGattCharacteristic meas = getChar(svc, CGM_MEASUREMENT);
        if (meas != null) enableNotify(gatt, meas);
        BluetoothGattCharacteristic socp = getChar(svc, CGM_SPECIFIC_OPS_CP);
        if (socp != null) enableNotify(gatt, socp);
    }

    @SuppressLint("MissingPermission")
    private void sendSocpGetCommInterval() {
        if (gatt == null) return;
        try {
            BluetoothGattService svc = gatt.getService(CGMS_SERVICE);
            if (svc == null) return;
            BluetoothGattCharacteristic socp = svc.getCharacteristic(CGM_SPECIFIC_OPS_CP);
            if (socp == null) return;
            // Prefer write with response
            socp.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            byte[] payload = new byte[]{0x02}; // Get CGM Communication Interval (safe probe)
            socp.setValue(payload);
            logBoth("[SOCP->] " + bytesToHex(payload));
            gatt.writeCharacteristic(socp);
        } catch (Exception e) {
            logBoth("[SOCP->] write failed: " + e.getMessage());
        }
    }

    private final BroadcastReceiver bondReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) return;
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device == null || currentDevice == null) return;
            if (!currentDevice.getAddress().equals(device.getAddress())) return;
            int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
            if (bondState == BluetoothDevice.BOND_BONDED) {
                bondingInProgress = false;
                logBoth(ctx.getString(R.string.bonded_continue));
                // 若尚未連線（預先配對流程），此時開始連線；否則續行 CCCD 啟用
                if (gatt == null) {
                    gatt = currentDevice.connectGatt(ctx, false, gattCb, BluetoothDevice.TRANSPORT_LE);
                } else if (servicesDiscovered) {
                    continueAfterBonding();
                } else {
                    gatt.discoverServices();
                }
            } else if (bondState == BluetoothDevice.BOND_NONE) {
                bondingInProgress = false;
                logBoth(ctx.getString(R.string.bond_failed));
            }
        }
    };

    private void scheduleReconnectIfNeeded() {
        if (currentDevice == null) return;
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) return;
        reconnectAttempts++;
        scanHandler.postDelayed(() -> {
            if (gatt != null) return; // already connected or connecting
            logBoth(ctx.getString(R.string.try_reconnect, reconnectAttempts));
            gatt = currentDevice.connectGatt(ctx, false, gattCb, BluetoothDevice.TRANSPORT_LE);
        }, RECONNECT_DELAY_MS);
    }

    private static String bytesToHex(byte[] b) {
        if (b == null) return "(null)";
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02X ", x));
        return sb.toString().trim();
    }
}
