package dk.andreasjacobsen.plantr;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.Objects;

/**
 * Created by Andreas on 04-05-2016.
 */
public class BluetoothLeScanner implements IBluetoothScanner {

    private BluetoothAdapter bluetoothAdapter;
    private boolean isScanning;
    boolean isRecurring;
    BluetoothDevice lastDevice;
    private Handler handler = new Handler();

    BluetoothLoader loader;

    private IBluetoothEventListener listener;
    Context context;

    // Stops scanning after 3 seconds
    private static long SCAN_PERIOD;

    BluetoothLeScanner(Context context, IBluetoothEventListener listener) {
        this.context = context;
        this.listener = listener;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        SCAN_PERIOD = 3000;
        loader = new BluetoothLoader(context, listener);

        isRecurring = false;
    }

    BluetoothLeScanner(Context context, IBluetoothEventListener listener, boolean isRecurring, BluetoothDevice lastDevice){
        this(context, listener);
        this.isRecurring = true;
        this.lastDevice = lastDevice;
        if (lastDevice != null) {
            SCAN_PERIOD = 2000;
            scan();
        }
    }

    public void scan() {
        scanLeDevice(true);
    }

    public void stop(){
        scanLeDevice(false);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            lastRssi = 0;

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bluetoothAdapter.stopLeScan(leScanCallback);
                    isScanning = false;

                    if (device != null) {
                        if (isRecurring) {
                            if (Objects.equals(device.getAddress(), lastDevice.getAddress())) {
                                listener.onDeviceStill();
                            } else {
                                listener.onDeviceGone();
                            }
                        } else {
                            listener.onDeviceFound(device);
                            loader.connect(device);
                        }
                    } else {
                        if (isRecurring) {
                            listener.onDeviceGone();
                        } else {
                            listener.onDeviceError(0);
                        }
                    }
                }
            }, SCAN_PERIOD);

            isScanning = true;
            bluetoothAdapter.startLeScan(leScanCallback);
        }
        else {
            isScanning = false;
            bluetoothAdapter.stopLeScan(leScanCallback);
        }
    }

    private BluetoothDevice device;
    private int lastRssi;

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice d, int rssi, byte[] scanRecord) {
            if (rssi < -70)
                return;

            if (!d.getName().equals("Plantr")) return;

            if (rssi > lastRssi){
                device = d;
                lastRssi = rssi;
                Log.d("BLE", "Found device " + device.getName() + ", " + rssi);
            }
            else if (lastRssi == 0) {
                device = d;
                lastRssi = rssi;
                Log.d("BLE", "Found device " + device.getName() + ", " + rssi);
            }
        }
    };

}
