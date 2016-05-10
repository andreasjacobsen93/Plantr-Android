package dk.andreasjacobsen.plantr;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Andreas on 06-05-2016.
 */
public interface IBluetoothEventListener {
    public void onDeviceFound(BluetoothDevice device);
    public void onDeviceError(int error);
    public void onDataReceived(int i);
    public void onDataCalculated(PlantData plantData);
    public void onDeviceGone();
    public void onDeviceStill();
}
