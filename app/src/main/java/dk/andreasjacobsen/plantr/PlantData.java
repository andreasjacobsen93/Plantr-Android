package dk.andreasjacobsen.plantr;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Andreas on 04-05-2016.
 */
public class PlantData {

    public enum TemperatureLevel {
        FREEZING, VERYCOLD, COLD, OKAY, NICE, HOT, BURNING, NA
    }

    public enum LightLevel {
        DARK, DIM, LIGHT, BRIGHT, VERYBRIGHT, NA
    }

    public enum SoilLevel {
        EXTREMEDRY, VERYDRY, DRY, LIGHTDRY, NORMALDRY, NORMAL, NORMALWET, WET, EXTREMEWET, NA
    }

    public enum HumidityLevel {
        VERYDRY, DRY, NORMAL, GREAT, WET, VERYWET, EXTREMEWET, NA
    }

    public enum WaterLevel {
        DRY, WET, WATER, NA
    }

    public enum TimeStatus {
        SUNRISE, DAY, SUNSET, NIGHT, NA
    }

    public int score;

    private TemperatureLevel[] temperatureLevels = new TemperatureLevel[72];
    private LightLevel[] lightLevels = new LightLevel[72];
    private SoilLevel[] soilLevels = new SoilLevel[72];
    private HumidityLevel[] humidityLevels = new HumidityLevel[72];
    private WaterLevel[] waterLevels = new WaterLevel[72];
    private Date[] dates = new Date[72];
    private TimeStatus[] timeStatuses = new TimeStatus[72];

    public PlantData(int[] array, Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = ((LocationManager) context.getSystemService(Context.LOCATION_SERVICE)).getLastKnownLocation(LocationManager.GPS_PROVIDER);
        SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(new com.luckycatlabs.sunrisesunset.dto.Location(location.getLatitude(), location.getLongitude()), TimeZone.getDefault());

        Date now = new Date();

        for (int i = 0; i < 72; i++) {
            ((MainActivity)context).onDataReceived(i+72);

            int o = Calculate(array[i], i);

            dates[i] = removeMinutesToDate(i*60, now);

            Calendar cal = Calendar.getInstance();
            cal.setTime(dates[i]);

            Date sunriseStart = removeMinutesToDate(10, calculator.getCivilSunriseCalendarForDate(cal).getTime());
            Date sunsetEnd = addMinutesToDate(10, calculator.getCivilSunsetCalendarForDate(cal).getTime());
            Date sunriseEnd = addMinutesToDate(30, sunriseStart);
            Date sunsetStart = removeMinutesToDate(30, sunsetEnd);

            if (dates[i].after(sunriseStart) && dates[i].before(sunriseEnd)) {
                timeStatuses[i] = TimeStatus.SUNRISE;
            } else if (dates[i].after(sunriseEnd) && dates[i].before(sunsetStart)) {
                timeStatuses[i] = TimeStatus.DAY;
            }else if (dates[i].before(sunsetEnd) && dates[i].after(sunsetStart)) {
                timeStatuses[i] = TimeStatus.SUNSET;
            } else if (dates[i].after(sunsetEnd) || dates[i].before(sunriseStart)) {
                timeStatuses[i] = TimeStatus.NIGHT;
            }

            if (o == 0){
                Log.d("PLANT", "Calculation successful!");
            } else {
                Log.d("PLANT", "Error in calculation! Missing: " + o);
            }
        }
    }

    public TemperatureLevel[] getTemperatureLevels() {
        return temperatureLevels;
    }
    public LightLevel[] getLightLevels() {
        return lightLevels;
    }
    public SoilLevel[] getSoilLevels() {
        return soilLevels;
    }
    public HumidityLevel[] getHumidityLevels() {
        return humidityLevels;
    }
    public WaterLevel[] getWaterLevels() {
        return waterLevels;
    }
    public Date[] getDates() {
        return dates;
    }
    public TimeStatus[] getTimeStatutes() {
        return timeStatuses;
    }

    public TemperatureLevel getTemperatureLevel(int i) {
        return temperatureLevels[i];
    }
    public LightLevel getLightLevel(int i) {
        return lightLevels[i];
    }
    public SoilLevel getSoilLevel(int i) {
        return soilLevels[i];
    }
    public HumidityLevel getHumidityLevel(int i) {
        return humidityLevels[i];
    }
    public WaterLevel getWaterLevel(int i) {
        return waterLevels[i];
    }
    public Date getDate(int i) {
        return dates[i];
    }
    public TimeStatus getTimeStatus(int i) {
        return timeStatuses[i];
    }

    private int Calculate(int n, int i){
        int output = n;
        output = CalculateSoil(output, i);
        output = CalculateWater(output, i);
        output = CalculateTemperature(output, i);
        output = CalculateHumidity(output, i);
        output = CalculateLight(output, i);
        return output;
    }

    private int CalculateSoil(int n, int i){
        SoilLevel level;
        if (n >= 90000){
            level = SoilLevel.EXTREMEDRY;
            n -= 90000;
        } else if (n >= 80000){
            level = SoilLevel.VERYDRY;
            n -= 80000;
        } else if (n >= 70000){
            level = SoilLevel.DRY;
            n -= 70000;
        } else if (n >= 60000){
            level = SoilLevel.LIGHTDRY;
            n -= 60000;
        } else if (n >= 50000){
            level = SoilLevel.NORMALDRY;
            n -= 50000;
        } else if (n >= 40000){
            level = SoilLevel.NORMAL;
            n -= 40000;
        } else if (n >= 30000){
            level = SoilLevel.NORMALWET;
            n -= 30000;
        } else if (n >= 20000){
            level = SoilLevel.WET;
            n -= 20000;
        } else if (n >= 10000) {
            level = SoilLevel.EXTREMEWET;
            n -= 10000;
        } else {
            level = SoilLevel.NA;
        }
        soilLevels[i] = level;
        return n;
    }
    private int CalculateWater(int n, int i){
        WaterLevel level;
        if (n >= 9000){
            level = WaterLevel.WATER;
            n -= 9000;
        } else if (n >= 2000){
            level = WaterLevel.WET;
            n -= 2000;
        } else if (n >= 1000){
            level = WaterLevel.DRY;
            n -= 1000;
        } else {
            level = WaterLevel.NA;
        }
        waterLevels[i] = level;
        return n;
    }
    private int CalculateTemperature(int n, int i){
        TemperatureLevel level;
        if (n >= 700){
            level = TemperatureLevel.BURNING;
            n -= 700;
        } else if (n >= 600){
            level = TemperatureLevel.HOT;
            n -= 600;
        } else if (n >= 500){
            level = TemperatureLevel.NICE;
            n -= 500;
        } else if (n >= 400){
            level = TemperatureLevel.OKAY;
            n -= 400;
        } else if (n >= 300){
            level = TemperatureLevel.COLD;
            n -= 300;
        } else if (n >= 200){
            level = TemperatureLevel.VERYCOLD;
            n -= 200;
        } else if (n >= 100){
            level = TemperatureLevel.FREEZING;
            n -= 100;
        } else {
            level = TemperatureLevel.NA;
        }
        temperatureLevels[i] = level;
        return n;
    }
    private int CalculateHumidity(int n, int i){
        HumidityLevel level;
        if (n >= 70){
            level = HumidityLevel.EXTREMEWET;
            n -= 70;
        } else if (n >= 60){
            level = HumidityLevel.VERYWET;
            n -= 60;
        } else if (n >= 50){
            level = HumidityLevel.WET;
            n -= 50;
        } else if (n >= 40){
            level = HumidityLevel.GREAT;
            n -= 40;
        } else if (n >= 30){
            level = HumidityLevel.NORMAL;
            n -= 30;
        } else if (n >= 20){
            level = HumidityLevel.DRY;
            n -= 20;
        } else if (n >= 10){
            level = HumidityLevel.VERYDRY;
            n -= 10;
        } else {
            level = HumidityLevel.NA;
        }
        humidityLevels[i] = level;
        return n;
    }
    private int CalculateLight(int n, int i){
        LightLevel level;
        if (n >= 5){
            level = LightLevel.VERYBRIGHT;
            n -= 5;
        } else if (n >= 4){
            level = LightLevel.BRIGHT;
            n -= 4;
        } else if (n >= 3){
            level = LightLevel.LIGHT;
            n -= 3;
        } else if (n >= 2){
            level = LightLevel.DIM;
            n -= 2;
        } else if (n >= 1){
            level = LightLevel.DARK;
            n -= 1;
        } else {
            level = LightLevel.NA;
        }
        lightLevels[i] = level;
        return n;
    }




    private static Date addMinutesToDate(int minutes, Date beforeTime){
        final long ONE_MINUTE_IN_MILLIS = 60000;//millisecs

        long curTimeInMs = beforeTime.getTime();
        Date afterAddingMins = new Date(curTimeInMs + (minutes * ONE_MINUTE_IN_MILLIS));
        return afterAddingMins;
    }

    private static Date removeMinutesToDate(int minutes, Date beforeTime){
        final long ONE_MINUTE_IN_MILLIS = 60000;//millisecs

        long curTimeInMs = beforeTime.getTime();
        Date afterRemovingMins = new Date(curTimeInMs - (minutes * ONE_MINUTE_IN_MILLIS));
        return afterRemovingMins;
    }
}
