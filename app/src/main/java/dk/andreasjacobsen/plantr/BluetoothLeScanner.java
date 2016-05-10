package dk.andreasjacobsen.plantr;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.util.Log;

/**
 * Created by Andreas on 04-05-2016.
 */
public class BluetoothLeScanner implements IBluetoothScanner {

    private BluetoothAdapter bluetoothAdapter;
    private boolean isScanning;
    private Handler handler = new Handler();;

    private IBluetoothEventListener listener;

    // Stops scanning after 3 seconds
    private static final long SCAN_PERIOD = 3000;

    BluetoothLeScanner(IBluetoothEventListener listener) {
        this.listener = listener;
    }

    public void scan() {
        scanLeDevice(true);
    }

    public void stop(){
        scanLeDevice(false);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isScanning = false;
                    bluetoothAdapter.stopLeScan(leScanCallback);
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

    private BluetoothDevice bleDevice;
    private int lastRssi;

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (rssi < -70)
                return;

            if (rssi > lastRssi){
                bleDevice = device;
                lastRssi = rssi;
                Log.d("BLE", "Found device " + device.getName() + ", " + rssi);
            }
            else if (lastRssi == 0) {
                bleDevice = device;
                lastRssi = rssi;
                Log.d("BLE", "Found device " + device.getName() + ", " + rssi);
            }
        }
    };

}
