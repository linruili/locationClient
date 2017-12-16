package com.example.administrator.locationclient.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by Administrator on 2017/12/14.
 */

public class SensorUtils
{
    private static SensorUtils mInstance;
    Context mContext;
    private SensorManager mSensorManager;
    private Sensor aSensor;
    private Sensor mSensor;
    float[] accelerometerValues = new float[3];
    float[] magneticFieldValues = new float[3];
    double compass;
    final SensorEventListener myListener = new SensorEventListener()
    {

        public void onSensorChanged(SensorEvent sensorEvent)
        {

            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                magneticFieldValues = sensorEvent.values;
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                accelerometerValues = sensorEvent.values;
        }
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    public static synchronized SensorUtils getInstance(Context context)
    {
        if (mInstance == null)
            mInstance = new SensorUtils(context);
         else
            mInstance.mContext = context;
        return mInstance;
    }

    SensorUtils(Context context)
    {
        mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        aSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void registSensor()
    {
        mSensorManager.registerListener(myListener, aSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(myListener, mSensor,SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void unregistSensor()
    {
        if (mSensorManager != null)
            mSensorManager.unregisterListener(myListener);
    }

    public double getCompassDirection()
    {
        float[] R = new float[9];
        float[] values = new float[3];
        SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);
        SensorManager.getOrientation(R, values);
        compass = (Math.toDegrees(values[0] * -1.0f) + 720) % 360;
        return compass;
    }
}
