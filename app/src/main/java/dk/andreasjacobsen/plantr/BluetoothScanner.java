package dk.andreasjacobsen.plantr;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by Andreas on 04-05-2016.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BluetoothScanner implements IBluetoothScanner {
    Context context;

    private boolean isScanning;
    private Handler handler = new Handler();

    private ScanSettings settings;
    private BluetoothLeScanner scanner;
    private List<ScanFilter> filters;

    private IBluetoothEventListener listener;
    boolean isRecurrings;
    BluetoothDevice lastDevice;

    // Stops scanning after 3 seconds
    private static long SCAN_PERIOD = 3000;

    BluetoothScanner(Context context, IBluetoothEventListener listener) {
        this.context = context;
        this.listener = listener;

        SCAN_PERIOD = 3000;

        filters = new ArrayList<>();

        ScanFilter filter = new ScanFilter.Builder().setDeviceName("Plantr").build();
        filters.add(filter);
        settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
        scanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();

        isRecurrings = false;
    }

    BluetoothScanner(Context context, IBluetoothEventListener listener, boolean isRecurrings, BluetoothDevice lastDevice) {
        this(context, listener);
        this.isRecurrings = true;
        this.lastDevice = lastDevice;
        if (lastDevice != null) {
            SCAN_PERIOD = 2000;
            scan();
        }
    }

    public void scan() {
        scanDevice(true);
    }

    public void stop(){
        scanDevice(false);
    }

    private void scanDevice(final boolean enable) {
        if (enable) {
            lastRssi = 0;

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanner.stopScan(callback);
                    isScanning = false;

                    if (device != null) {
                        if (isRecurrings) {
                            if (Objects.equals(device.getAddress(), lastDevice.getAddress())) {
                                listener.onDeviceStill();
                            } else {
                                listener.onDeviceGone();
                            }
                        } else {
                            listener.onDeviceFound(device);
                            new BluetoothLoader(context, listener).connect(device);
                        }
                    } else {
                        if (isRecurrings) {
                            listener.onDeviceGone();
                        } else {
                            listener.onDeviceError(0);
                        }
                    }
                }
            }, SCAN_PERIOD);

            isScanning = true;
            scanner.startScan(filters, settings, callback);
        } else {
            isScanning = false;
            scanner.stopScan(callback);
        }
    }

    private BluetoothDevice device;
    private int lastRssi;

    private ScanCallback callback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            if (result.getRssi() < -70)
                return;

            if (result.getRssi() > lastRssi){
                device = result.getDevice();
                lastRssi = result.getRssi();
                Log.d("BLE", "Found device " + result.getDevice().getName() + ", " + result.getRssi());
            }
            else if (lastRssi == 0) {
                device = result.getDevice();
                lastRssi = result.getRssi();
                Log.d("BLE", "Found device " + result.getDevice().getName() + ", " + result.getRssi());
            }

        }
    };

}
