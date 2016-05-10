package dk.andreasjacobsen.plantr;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.github.kayvannj.permission_utils.Func;
import com.github.kayvannj.permission_utils.PermissionUtil;

public class LoadActivity extends AppCompatActivity {

    public static final int REQUEST_ENABLE_BT = 6;

    private PermissionUtil.PermissionRequestObject requestObject;

    BluetoothAdapter bluetoothAdapter;
    boolean hasBluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_load);

        TextView btnContinue = (TextView)findViewById(R.id.continue_btn);

        // User click on allow
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPermissions();
            }
        });

        // This is used to check whether BLE is supported on the device
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            noBT();
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        hasBluetooth = (bluetoothAdapter != null);

        // If the location permission is granted we go straigt to enabling bluetooth
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (hasBluetooth) {
                if (bluetoothAdapter.isEnabled()) {
                    navigateHome();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    void getPermissions(){
        // Request location permission
        requestObject = PermissionUtil.with(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                // When granted enable bluetooth
                .onAllGranted(new Func() {
                    @Override
                    protected void call() {
                        enableBluetooth();
                // If denied go to the gps rationale screen, describing why location is needed
                    }
                }).onAnyDenied(new Func() {
                    @Override
                    protected void call() {
                        noGPS();
                    }
                }).ask(0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        requestObject.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT)
        {
            if (resultCode == RESULT_OK) {
                boolean isEnabling = bluetoothAdapter.enable();
                if (!isEnabling) {
                    noBT();
                }
                else if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON) {
                    navigateHome();
                }
                else if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
                    noBT();
                }
                else if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                    navigateHome();
                }
            }
            else if (resultCode == RESULT_CANCELED) {
                noBT();
            }
        }
    }

    void enableBluetooth(){

        if (hasBluetooth) {
            if (bluetoothAdapter.isEnabled()) {
                navigateHome();
            }
            else {
                // Prompt the user to turn on bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        } else {
            noBT();
        }
    }

    void navigateHome(){
        Intent intent = new Intent(LoadActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    void noGPS(){
        Intent intent = new Intent(LoadActivity.this, NoGpsActivity.class);
        startActivity(intent);
    }

    void noBT(){
        Intent intent = new Intent(LoadActivity.this, NoBtActivity.class);
        startActivity(intent);
    }
}
