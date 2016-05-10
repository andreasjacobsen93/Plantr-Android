package dk.andreasjacobsen.plantr;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

import co.lujun.lmbluetoothsdk.BluetoothLEController;

/**
 * Created by Andreas on 06-05-2016.
 */
public class BluetoothLoader {
    private BluetoothLEController mBLEController;
    private BluetoothGatt connectGatt;
    private Context context;
    IBluetoothEventListener listener;

    BluetoothLoader(Context context, IBluetoothEventListener listener) {
        this.context = context;
        this.listener = listener;
        mBLEController = BluetoothLEController.getInstance().build(this.context);
    }

    void connect(BluetoothDevice device){
        try {
            if (mBLEController.isAvailable()) {
               if (mBLEController.openBluetooth()) {
                   Log.d("BLE", "Connecting to device...");
                   connectGatt = mBLEController.findDeviceByMac(device.getAddress()).connectGatt(context, false, callback);
               }
            }

        } catch (RuntimeException e) {
            listener.onDeviceError(0);
        }
    }

    private BluetoothGattCallback callback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d("Gatt", "Connection state changed " + gatt.discoverServices() + " " + status);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            Log.d("Gatt", "Services discovered " + status);

            for (BluetoothGattService service : gatt.getServices()) {
                if (service.getUuid().toString().startsWith("0000181a")) {
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        gatt.readCharacteristic(characteristic);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            Log.d("Gatt", "Characteristic " + characteristic.getUuid() + " read " + status);

            gatt.setCharacteristicNotification(characteristic, true);

            // 0x2902 org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml
            UUID uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(uuid);
            if (descriptor == null)
                Log.e("BLE", "Could not get descriptor");
            else
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            if (!gatt.writeDescriptor(descriptor))
                Log.e("BLE", "Could not write client descriptor value");
        }

        int i = 0;
        int[] array = new int[72];

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            if (i == 72) {
                connectGatt.close();
                PlantData plantData = new PlantData(array, context);
                listener.onDataCalculated(plantData);
            }
            else {
                int val = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
                array[i] = val;

                listener.onDataReceived(i);

                Log.d("GATT", "Data: " + val);

                i++;
            }
        }
    };
}
