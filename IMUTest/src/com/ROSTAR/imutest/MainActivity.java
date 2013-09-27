package com.ROSTAR.imutest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.TextView;


public class MainActivity extends Activity implements SensorEventListener {

	private static final String LOG_TAG = "MadgwickAHRS";
	private SensorManager mSensorManager;
	private Sensor mAccelerometer, mGyroscope, mMagnetic;
	TextView title, text_a_x, text_a_y, text_a_z, text_g_x, text_g_y, text_g_z, text_m_x, text_m_y, text_m_z;
	TextView tq0, tq1, tq2, tq3, text_a_t, text_g_t, text_m_t;
	float rate_acc, rate_gyro, rate_mag, time_acc, time_gyro, time_mag, sampleFreq, time_prev, time_curr;

	float[] acc, gyro, mag, quat;
	public static enum SENSOR_TYPE{
		TYPE_ACC, TYPE_GYRO, TYPE_MAG, TYPE_QUAT
	}
	
	BufferedWriter writer_acc, writer_gyro, writer_mag, writer_quat;
	
	Boolean obs_acc, obs_gyro, obs_mag, store_data;
	MadgwickAHRS madgwickFilter = new MadgwickAHRS();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Setting up data display
		
		//get textviews
		title=(TextView)findViewById(R.id.name);

		// Accelerometer Display
		text_a_x=(TextView)findViewById(R.id.a_xval);
		text_a_y=(TextView)findViewById(R.id.a_yval);
		text_a_z=(TextView)findViewById(R.id.a_zval);
		text_a_t=(TextView)findViewById(R.id.a_rate);

		// Gyro Display
		text_g_x=(TextView)findViewById(R.id.g_xval);
		text_g_y=(TextView)findViewById(R.id.g_yval);
		text_g_z=(TextView)findViewById(R.id.g_zval);
		text_g_t=(TextView)findViewById(R.id.g_rate);

		// Mag Display
		text_m_x=(TextView)findViewById(R.id.m_xval);
		text_m_y=(TextView)findViewById(R.id.m_yval);
		text_m_z=(TextView)findViewById(R.id.m_zval);
		text_m_t=(TextView)findViewById(R.id.m_rate);
		
		// Quat Display
		tq0  = (TextView)findViewById(R.id.quart0);
		tq1  = (TextView)findViewById(R.id.quart1);
		tq2  = (TextView)findViewById(R.id.quart2);
		tq3  = (TextView)findViewById(R.id.quart3);

		// Declaring things
		rate_acc = rate_gyro = rate_mag = 0.0f;
		time_acc = time_gyro = time_mag = SystemClock.uptimeMillis();
		acc = gyro = mag = new float[3];
		obs_acc = obs_gyro = obs_mag = false;
		
		// Data writers left open until close to improve speed. NOTE app will crash when buffers full.
		openDataWriters();


		// Check if external storage is available
		store_data = isExternalStorageWritable();
		
		// Start Sensors
		SensorActivity();
	}



	public void SensorActivity() {

		// Get an instance of the SensorManager
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		// Get an instance of accelerometer sensor
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);

		// Get an instance of gyroscope sensor
		mGyroscope =  mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);

		// Get an instance of magnetic sensor
		mMagnetic =  mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mSensorManager.registerListener(this, mMagnetic, SensorManager.SENSOR_DELAY_FASTEST);

	}

	@Override
	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
		mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
		mSensorManager.registerListener(this, mMagnetic, SensorManager.SENSOR_DELAY_FASTEST);
		
		openDataWriters();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
		
		// Closing data writers
		try {
			writer_acc.close();
			writer_gyro.close();
			writer_mag.close();
			writer_quat.close();
		} catch (IOException e) {
			Log.e(LOG_TAG, "Unable to close writers:" + e.toString());
		}
		
	}

	@Override
	public final void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		// Do something here if sensor accuracy changes.
	}

	@Override
	public final void onSensorChanged(SensorEvent event)
	{

		//display values using TextView
		title.setText(R.string.app_name);

		switch (event.sensor.getType())
		{

		case Sensor.TYPE_ACCELEROMETER: 
			acc = event.values;
			text_a_x.setText("Accelerometer x axis:" + "\t\t" + acc[0]);
			text_a_y.setText("Accelerometer y axis:" + "\t\t" + acc[1]);
			text_a_z.setText("Accelerometer z axis:" + "\t\t" + acc[2]);
			
			if (1000 / (SystemClock.uptimeMillis()-time_acc) > rate_acc ) {
				rate_acc = 1000 / (SystemClock.uptimeMillis()-time_acc);
				time_acc = SystemClock.uptimeMillis();
				text_a_t.setText("Accelerometer Rate (Hz):" + "\t\t" + rate_acc);
			}
			
			if (store_data)
				writeToFile(SENSOR_TYPE.TYPE_ACC, time_acc + "," + acc[0] + "," + acc[1] + "," +acc[2] + "\n");
			
			obs_acc = true;
			break;

		case Sensor.TYPE_GYROSCOPE:
			gyro = event.values;
			text_g_x.setText("Gyroscope x axis:" + "\t\t" + gyro[0]);
			text_g_y.setText("Gyroscope y axis:" + "\t\t" + gyro[1]);
			text_g_z.setText("Gyroscope z axis:" + "\t\t" + gyro[2]);
			if (1000 / (SystemClock.uptimeMillis()-time_gyro) > rate_gyro ) {
				rate_gyro = 1000 / (SystemClock.uptimeMillis()-time_gyro);
				time_gyro = SystemClock.uptimeMillis();
				text_g_t.setText("Gyroscope Rate (Hz):" + "\t\t" + rate_gyro);
			}
			
			if (store_data)
				writeToFile(SENSOR_TYPE.TYPE_GYRO, time_gyro + "," + gyro[0] + "," + gyro[1] + "," +gyro[2] + "\n");
			obs_gyro = true;
			break;

		case Sensor.TYPE_MAGNETIC_FIELD:
			mag = event.values;
			text_m_x.setText("Magnetic x axis:" + "\t\t" + mag[0]);
			text_m_y.setText("Magnetic y axis:" + "\t\t" + mag[1]);
			text_m_z.setText("Magnetic z axis:" + "\t\t" + mag[2]);
			if (1000 / (SystemClock.uptimeMillis()-time_mag) > rate_mag ) {
				rate_mag = 1000 / (SystemClock.uptimeMillis()-time_mag);
				time_mag = SystemClock.uptimeMillis();
				text_m_t.setText("Magnetic Rate (Hz):" + "\t\t" + rate_mag);
			}
			
			if (store_data)
				writeToFile(SENSOR_TYPE.TYPE_MAG, time_mag + "," + mag[0] + "," + mag[1] + "," +mag[2] + "," + "\n");
			obs_mag = true;
			break;

		default: break;
		}
		
		if (obs_acc && obs_gyro && obs_mag){
			
			// Calculate sampling frequency
			time_curr = SystemClock.uptimeMillis();
			sampleFreq = 1000/(time_curr-time_prev);
			
			// Calculate Quaternion
			quat = madgwickFilter.MadgwickAHRSupdate(gyro[0], gyro[1], gyro[2], acc[0], acc[1], acc[2], mag[0], mag[1], mag[2], sampleFreq);
			
			// Update observation time
			time_prev = time_curr;
			
			// Reset observations
			obs_acc = obs_gyro = obs_mag = false;
			
			// Display values
			tq0.setText("q0 :\t\t" + quat[0]);
			tq1.setText("q1 :\t\t" + quat[1]);
			tq2.setText("q2 :\t\t" + quat[2]);
			tq3.setText("q3 :\t\t" + quat[3]);
			
			if (store_data)
				writeToFile(SENSOR_TYPE.TYPE_QUAT, time_curr + "," + quat[0] + "," + quat[1] + "," +quat[2] + "," + quat[3] + "\n");
		}
	}
	
	// Function to open data loggers.
	private void openDataWriters() {
		try {
			writer_acc = new BufferedWriter(new FileWriter(new File(Environment.getExternalStorageDirectory().getPath() ,"_acc")));
			writer_gyro = new BufferedWriter(new FileWriter(new File(Environment.getExternalStorageDirectory().getPath() , "_gyro")));
			writer_mag = new BufferedWriter(new FileWriter(new File(Environment.getExternalStorageDirectory().getPath() , "_mag")));
			writer_quat = new BufferedWriter(new FileWriter(new File(Environment.getExternalStorageDirectory().getPath() , "_quat")));
		} catch (IOException e) {
			Log.e(LOG_TAG, "Failed to create writer: " + e.toString());
		}		
	}
	
	// Method to handle writing data to files
	private void writeToFile(SENSOR_TYPE type, String data) {
	    try {
	    	switch (type) {
		    	    		
		    	case TYPE_ACC:
		    		writer_acc.append(data);
		    		Log.e(LOG_TAG, "Writing Accelerometer Data");
		    		break;
		    		
		    	case TYPE_GYRO:
		    		writer_gyro.append(data);
		    		Log.e(LOG_TAG, "Writing Gyroscope Data");
		    		break;
		    		
		    	case TYPE_MAG:
		    		writer_mag.append(data);
		    		Log.e(LOG_TAG, "Writing Magnetic Data");
		    		break;
		    		
		    	case TYPE_QUAT:
		    		writer_quat.append(data);
		    		Log.e(LOG_TAG, "Writing Quaternion Data");
		    		break;
		    		
		    	default: break;
	    	}
	    }
	    catch (IOException e) {
	        Log.e(LOG_TAG, "File write failed: " + e.toString());
	    } 
	}
	
	/* Checks if external storage is available for read and write */
	public boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	    	Log.e(LOG_TAG,"SDCard Available");
	        return true;
	    }
	    return false;
	}
}
