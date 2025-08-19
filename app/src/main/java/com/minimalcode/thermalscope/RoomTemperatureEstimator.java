package com.minimalcode.thermalscope;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Debug;
import android.util.Log;
import android.widget.Toast;

public class RoomTemperatureEstimator {

    private Context context;
    private SensorManager sensorManager;
    private Sensor ambientSensor;
    private Float ambientValue = null;

    public RoomTemperatureEstimator(Context context) {
        this.context = context;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        ambientSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
    }

    // 1. Try to use Ambient Temperature Sensor if available
    public void startAmbientListener() {
        if (ambientSensor != null) {
            Toast.makeText(context, "Ambient temperature sensor found!", Toast.LENGTH_SHORT).show();
            sensorManager.registerListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    ambientValue = event.values[0]; // °C
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {}
            }, ambientSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }else {
            Toast.makeText(context, "No ambient temperature sensor, using battery temp fallback.", Toast.LENGTH_SHORT).show();
        }
    }

    // 2. Battery Temperature (fallback)
    private float getBatteryTemp() {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);
        int temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
        return temp / 10f; // °C
    }

    // 3. CPU Usage (extra heat factor)
    public float getCPUUsage() {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        // Get info about current app’s processes
        int[] pids = new int[]{android.os.Process.myPid()};
        Debug.MemoryInfo[] memInfo = am.getProcessMemoryInfo(pids);

        // Not true CPU % for whole system, but you can use this as a "load factor"
        float myAppLoad = memInfo[0].getTotalPss() / 1024f; // MB
        return myAppLoad;
    }

    // 4. RAM Usage (indirect load)
    private float getRAMUsage() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        long usedMem = mi.totalMem - mi.availMem;
        if (mi.totalMem == 0) return 0f; // Avoid division by zero
        return (usedMem * 100f) / mi.totalMem;
    }

    // 5. Charging Status
    private boolean isCharging() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        if (batteryStatus == null) return false; // Receiver might not be registered or battery status not available
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
    }

    // 🔹 Final Room Temp Estimate
    public float getEstimatedRoomTemp() {
        // Case 1: Use real ambient sensor
        if (ambientValue != null) {
            return ambientValue;
        }

        // Case 2: Use battery temp with corrections
        float batteryTemp = getBatteryTemp();
        float cpuUsage = getCPUUsage();
        float ramUsage = getRAMUsage();
        boolean charging = isCharging();

        // Basic correction model
        float heatOffset = (cpuUsage * 0.02f) + (ramUsage * 0.01f);
        if (charging) heatOffset += 1.5f;

        float estimatedRoom = batteryTemp - heatOffset;

        // Add a log to see the components of the estimation
        Log.d("RoomTempEstimator", "Battery: " + batteryTemp + "°C, CPU: " + cpuUsage + "%, RAM: " + ramUsage + "%, Charging: " + charging + ", Offset: " + heatOffset + "°C, Estimated: " + estimatedRoom + "°C");

        return estimatedRoom;
    }
}
