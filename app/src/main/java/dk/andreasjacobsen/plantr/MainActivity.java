package dk.andreasjacobsen.plantr;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.github.clans.fab.FloatingActionButton;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements IBluetoothEventListener, GestureDetector.OnGestureListener {

    ImageView bg;
    TextView txt;
    FloatingActionButton fab;
    Toast toast;

    ImageView imgSoil;
    ImageView imgHumid;
    ImageView imgLight;
    ImageView imgRain;
    ImageView imgTree;
    ImageView imgIce;

    TextView time;

    boolean isSearching;

    PlantData plantData;

    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    IBluetoothScanner scanner;

    /**
     * The initial fling velocity is divided by this amount.
     */
    public static final int FLING_VELOCITY_DOWNSCALE = 4;
    private GestureDetectorCompat mDetector;
    private Scroller mScroller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Intent intentLoader = new Intent(MainActivity.this, LoadActivity.class);
            startActivity(intentLoader);
            finish();
            return;
        }

        this.registerReceiver(btStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        // Instantiate the gesture detector with the
        // application context and an implementation of
        // GestureDetector.OnGestureListener
        mDetector = new GestureDetectorCompat(this, this);

        // Create a Scroller to handle the fling gesture.
        if (Build.VERSION.SDK_INT < 11) {
            mScroller = new Scroller(getApplicationContext());
        } else {
            mScroller = new Scroller(getApplicationContext(), null, true);
        }

        fab = (FloatingActionButton)findViewById(R.id.fab);

        imgSoil = (ImageView) findViewById(R.id.soil);
        imgHumid = (ImageView) findViewById(R.id.humidity);
        imgLight = (ImageView) findViewById(R.id.light);
        imgRain = (ImageView) findViewById(R.id.rain);
        imgTree = (ImageView) findViewById(R.id.tree);
        imgIce = (ImageView) findViewById(R.id.ice);

        time = (TextView)findViewById(R.id.time);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scanner = new BluetoothScanner(this, this);
        }
        else {
            scanner = new BluetoothLeScanner(this, this);
        }

        startSearching();
        bluetoothSearch();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scanner.stop();
        this.unregisterReceiver(btStateReceiver);
    }

    private void bluetoothSearch(){
        scanner.scan();
    }

    private void bluetoothStop(){
        scanner.stop();
    }

    private void startSearching() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fab.setIndeterminate(true);
            }
        });
        ShowToast("Searching for nearby plant...");
        isSearching = true;
    }

    private void stopSearching() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fab.hide(true);
            }
        });
        CancelToast();
        isSearching = false;
    }

    public void ShowToast(String text) {
        if (toast != null) toast.cancel();
        toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.show();
    }
    private void CancelToast(){
        if (toast != null) toast.cancel();
    }

    // Used if bluetooth changes state to off
    private final BroadcastReceiver btStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // The user changed the bluetooth state
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
                    Intent intentLoader = new Intent(MainActivity.this, LoadActivity.class);
                    startActivity(intentLoader);
                    finish();
                }
            }
        }
    };

    BluetoothDevice device;
    @Override
    public void onDeviceFound(BluetoothDevice device) {
        this.device = device;
        fab.setImageDrawable(getResources().getDrawable(R.drawable.tree));
        ShowToast("Found plant. Gathering data...");
    }

    @Override
    public void onDeviceError(final int error) {
        if (error != 0) return;
        Intent intentLoader = new Intent(MainActivity.this, LoadActivity.class);
        startActivity(intentLoader);
        finish();
    }

    @Override
    public void onDataReceived(final int i) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(i == 0) {
                    fab.setIndeterminate(false);
                    ShowToast("Calculating data...");
                }
                fab.setProgress((int)(i*0.7), true);
            }
        });
    }

    @Override
    public void onDataCalculated(final PlantData pd) {
        stopSearching();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                plantData = pd;
                time.setVisibility(View.VISIBLE);

                setImage(0);
                SetInt(0);
            }
        });

        handler.postDelayed(runnable, 50);

        handler.postDelayed(image, 500);
        ScanRecurring();
    }

    @Override
    public void onDeviceGone() {
        Intent intentLoader = new Intent(MainActivity.this, LoadActivity.class);
        startActivity(intentLoader);
        finish();
    }

    @Override
    public void onDeviceStill() {

    }

    @Override
    public void onBackPressed() {
        finish();
        System.exit(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scanner = new BluetoothScanner(MainActivity.this, MainActivity.this, true, device);
        }
        else {
            scanner = new BluetoothLeScanner(MainActivity.this, MainActivity.this, true, device);
        }
    }

    Handler handler = new Handler();
    private void ScanRecurring() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    scanner = new BluetoothScanner(MainActivity.this, MainActivity.this, true, device);
                }
                else {
                    scanner = new BluetoothLeScanner(MainActivity.this, MainActivity.this, true, device);
                }
                ScanRecurring();
            }
        }, 300000);
    }

    public void setImage(int i) {
        imgTree.setImageDrawable(getResources().getDrawable(R.drawable.tree_left));
        switch (plantData.getSoilLevel(i)) {
            case EXTREMEDRY:
                animateInOut(imgSoil, (R.drawable.soil_extremedry));
                break;
            case VERYDRY:
                animateInOut(imgSoil, (R.drawable.soil_verydry));
                break;
            case DRY:
                animateInOut(imgSoil, (R.drawable.soil_dry));
                break;
            case LIGHTDRY:
                animateInOut(imgSoil, (R.drawable.soil_lightdry));
                break;
            case NORMALDRY:
                animateInOut(imgSoil, (R.drawable.soil_normaldry));
                break;
            case NORMAL:
                animateInOut(imgSoil, (R.drawable.soil_normal));
                break;
            case NORMALWET:
                animateInOut(imgSoil, (R.drawable.soil_normalwet));
                break;
            case WET:
                animateInOut(imgSoil, (R.drawable.soil_wet));
                break;
            case EXTREMEWET:
                animateInOut(imgSoil, (R.drawable.soil_extremewet));
                break;
            case NA:
                animateInOut(imgSoil, (R.drawable.soil_na));
                break;
        }
        switch (plantData.getHumidityLevel(i)) {
            case VERYDRY:
                animateInOut(imgHumid, (R.drawable.humidity_verydry));
                break;
            case DRY:
                animateInOut(imgHumid, (R.drawable.humidity_dry));
                break;
            case NORMAL:
                animateInOut(imgHumid, 0);//R.drawable.humidity_na);
                break;
            case GREAT:
                animateInOut(imgHumid, (R.drawable.humidity_great));
                break;
            case WET:
                animateInOut(imgHumid, (R.drawable.humidity_wet));
                break;
            case VERYWET:
                animateInOut(imgHumid, (R.drawable.humidity_verywet));
                break;
            case EXTREMEWET:
                animateInOut(imgHumid, (R.drawable.humidity_extremewet));
                break;
            case NA:
                animateInOut(imgHumid, (R.drawable.humidity_na));
                break;
        }
        switch (plantData.getTimeStatus(i)) {
            case SUNRISE:
                switch (plantData.getTemperatureLevel(i)){
                    case BURNING:
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.morning_hot_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.morning_hot_dark));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.morning_hot_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.morning_hot_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.morning_hot_bright));
                                break;
                        }
                        break;
                    case HOT:
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.morning_hot_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.morning_hot_dark));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.morning_hot_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.morning_hot_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.morning_hot_bright));
                                break;
                        }
                        break;
                    case NICE:
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.morning_nice_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.morning_nice_dark));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.morning_nice_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.morning_nice_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.morning_nice_bright));
                                break;
                        }
                        break;
                    case OKAY:
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.morning_nice_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.morning_nice_dark));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.morning_nice_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.morning_nice_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.morning_nice_bright));
                                break;
                        }
                        break;
                    case COLD:
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.morning_cold_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.morning_cold_dark));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.morning_cold_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.morning_cold_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.morning_cold_bright));
                                break;
                        }
                        break;
                    case VERYCOLD:
                        animateInOut(imgIce, (R.drawable.cold));
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.morning_cold_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.morning_cold_dark));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.morning_cold_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.morning_cold_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.morning_cold_bright));
                                break;
                        }
                        break;
                    case FREEZING:
                        animateInOut(imgIce, (R.drawable.freezing));
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.morning_cold_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.morning_cold_dark));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.morning_cold_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.morning_cold_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.morning_cold_bright));
                                break;
                        }
                        break;
                }
                break;
            case SUNSET:
                switch (plantData.getTemperatureLevel(i)){
                    case BURNING:
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.evening_hot_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.evening_hot_dark));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.evening_hot_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.evening_hot_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.evening_hot_bright));
                                break;
                        }
                        break;
                    case HOT:
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.evening_hot_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.evening_hot_dark));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.evening_hot_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.evening_hot_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.evening_hot_bright));
                                break;
                        }
                        break;
                    case NICE:
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.evening_nice_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.evening_nice_dark));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.evening_nice_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.evening_nice_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.evening_nice_bright));
                                break;
                        }
                        break;
                    case OKAY:
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.evening_nice_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.evening_nice_dark));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.evening_nice_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.evening_nice_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.evening_nice_bright));
                                break;
                        }
                        break;
                    case COLD:
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.evening_cold_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.evening_cold_dark));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.evening_cold_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.evening_cold_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.evening_cold_bright));
                                break;
                        }
                        break;
                    case VERYCOLD:
                        animateInOut(imgIce, (R.drawable.cold));
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.evening_cold_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.evening_cold_dark));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.evening_cold_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.evening_cold_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.evening_cold_bright));
                                break;
                        }
                        break;
                    case FREEZING:
                        animateInOut(imgIce, (R.drawable.freezing));
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.evening_cold_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.evening_cold_dark));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.evening_cold_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.evening_cold_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.evening_cold_bright));
                                break;
                        }
                        break;
                }
                break;
            case NIGHT:
                switch (plantData.getTemperatureLevel(i)){
                    case BURNING:
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.night_hot_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.night_hot_dark));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.night_hot_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.night_hot_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.night_hot_bright));
                                break;
                        }
                        break;
                    case HOT:
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.night_hot_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.night_hot_dark));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.night_hot_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.night_hot_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.night_hot_bright));
                                break;
                        }
                        break;
                    case NICE:
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.night_nice_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.night_nice_dark));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.night_nice_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.night_nice_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.night_nice_bright));
                                break;
                        }
                        break;
                    case OKAY:
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.night_nice_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.night_nice_dark));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.night_nice_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.night_nice_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.night_nice_bright));
                                break;
                        }
                        break;
                    case COLD:
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.night_cold_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.night_cold_dark));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.night_cold_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.night_cold_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.night_nice_bright));
                                break;
                        }
                        break;
                    case VERYCOLD:
                        animateInOut(imgIce, (R.drawable.cold));
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.night_cold_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.night_cold_dark));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.night_cold_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.night_cold_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.night_nice_bright));
                                break;
                        }
                        break;
                    case FREEZING:
                        animateInOut(imgIce, (R.drawable.freezing));
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.night_cold_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.night_cold_dark));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.night_cold_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.night_cold_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.night_nice_bright));
                                break;
                        }
                        break;
                }
                break;
            case DAY:
                switch (plantData.getTemperatureLevel(i)){
                    case BURNING:
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.day_burning_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.day_burning_dim));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.day_burning_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.day_burning_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.day_burning_verybright));
                                break;
                        }
                        break;
                    case HOT:
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.day_hot_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.day_hot_dim));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.day_hot_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.day_hot_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.day_hot_verybright));
                                break;
                        }
                        break;
                    case NICE:
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.day_nice_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.day_nice_dim));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.day_nice_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.day_nice_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.day_nice_verybright));
                                break;
                        }
                        break;
                    case OKAY:
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.day_okay_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.day_okay_dim));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.day_okay_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.day_okay_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.day_okay_verybright));
                                break;
                        }
                        break;
                    case COLD:
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.day_cold_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.day_cold_dim));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.day_cold_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.day_cold_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.day_cold_verybright));
                                break;
                        }
                        break;
                    case VERYCOLD:
                        animateInOut(imgIce, (R.drawable.cold));
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.day_cold_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.day_cold_dim));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.day_cold_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.day_cold_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.day_cold_verybright));
                                break;
                        }
                        break;
                    case FREEZING:
                        animateInOut(imgIce, (R.drawable.freezing));
                        switch (plantData.getLightLevel(i)) {
                            case DARK:
                                animateInOut(imgLight, (R.drawable.day_cold_dark));
                                break;
                            case DIM:
                                animateInOut(imgLight, (R.drawable.day_cold_dim));
                                break;
                            case LIGHT:
                                animateInOut(imgLight, (R.drawable.day_cold_light));
                                break;
                            case BRIGHT:
                                animateInOut(imgLight, (R.drawable.day_cold_bright));
                                break;
                            case VERYBRIGHT:
                                animateInOut(imgLight, (R.drawable.day_cold_verybright));
                                break;
                        }
                        break;
                }
                break;
        }
        if (plantData.getLightLevel(i) == PlantData.LightLevel.NA)
            animateInOut(imgLight, (R.drawable.day_na));
    }

    public int i = 0;
    Date dNow = new Date();
    Date dDate = new Date();
    SimpleDateFormat ft = new SimpleDateFormat ("dd/MM HH");

    public void SetInt(int n) {
        if (n > -72 && n <= 0) i = n;

        dDate.setTime(dNow.getTime() + TimeUnit.HOURS.toMillis(i));

        time.setText(String.format("%s:00", ft.format(dDate)));

        //bg.run();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.mDetector.onTouchEvent(event);
        // Be sure to call the superclass implementation
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        if (isAnimationRunning()) {
            mScroller.forceFinished(true);
        }

        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        totalX += distanceX;

        if (totalX > 100) {
            SetInt(i + 1);
            totalX = 0;
        } else if (totalX < -100) {
            SetInt(i - 1);
            totalX = 0;
        }

        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        // Before flinging, aborts the current animation.
        mScroller.forceFinished(true);

        mScroller.fling(0, 0, -(int) velocityX, 0, -1280, 1280, 0, 0);

        return true;
    }

    private boolean isAnimationRunning() {
        return !mScroller.isFinished() || (Build.VERSION.SDK_INT >= 11);
    }

    private void animateInOut2(final ImageView image, final Drawable in) {
        Drawable backgrounds[] = new Drawable[2];
        backgrounds[0] = image.getDrawable();
        backgrounds[1] = in;

        TransitionDrawable crossfader = new TransitionDrawable(backgrounds);
        image.setImageDrawable(crossfader);
        crossfader.startTransition(200);
    }

    private void animateInOut(final ImageView image, final int in) {
        if (in == 0) {
            image.setImageDrawable(null);
        } else {
            image.setImageDrawable(getResources().getDrawable(in));
        }
        //if (!mScroller.isFinished()) return;
        //Glide.with(this).load(in).into(image);
/*
        Animation fadeout = new AlphaAnimation(1, 0);
        fadeout.setInterpolator(new AccelerateInterpolator());
        fadeout.setDuration(200);
        final Animation fadein = new AlphaAnimation(0, 1);
        fadein.setInterpolator(new AccelerateInterpolator());
        fadein.setDuration(200);

        fadeout.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                //image.setVisibility(View.GONE);
                image.setImageDrawable(in);
                image.startAnimation(fadein);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        fadein.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                //image.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        image.startAnimation(fadeout);*/
    }

    private void animateOut(final View view) {
        Animation fadeout = new AlphaAnimation(1, 0);
        fadeout.setInterpolator(new AccelerateInterpolator());
        fadeout.setDuration(500);

        fadeout.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        view.startAnimation(fadeout);
    }
    private void animateIn(final View view) {
        Animation fadein = new AlphaAnimation(0, 1);
        fadein.setInterpolator(new AccelerateInterpolator());
        fadein.setDuration(500);

        fadein.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        view.startAnimation(fadein);
    }

    int totalX;
    int last = 0;
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (!mScroller.isFinished()) {
                mScroller.computeScrollOffset();

                //Log.d("Fling", mScroller.getCurrX() - last + " = " + mScroller.getCurrX() + " - " + last);

                if (mScroller.getCurrX() < 0 && last < mScroller.getCurrX()) last = 0;
                else if (mScroller.getCurrX() > 0 && last > mScroller.getCurrX()) last = 0;

                totalX += mScroller.getCurrX() - last;

                if (totalX > 50) {
                    SetInt(i + 1);
                    totalX = 0;
                } else if (totalX < -50) {
                    SetInt(i - 1);
                    totalX = 0;
                }

                last = mScroller.getCurrX();
            }
            handler.postDelayed(this, 50);
        }
    };

    int lastI;
    private Runnable image = new Runnable() {
        @Override
        public void run() {
            int me = i;
            if (lastI != me) {
                if (compareInt(me)) {
                    setImage(i*-1);
                    lastI = i;
                }
            }

            handler.postDelayed(image, 500);
        }
    };

    public boolean compareInt(int i) {
        return this.i == i;
    }
}
