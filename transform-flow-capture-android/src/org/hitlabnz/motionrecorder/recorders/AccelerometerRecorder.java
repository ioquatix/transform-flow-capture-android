package org.hitlabnz.motionrecorder.recorders;

import java.io.File;
import java.util.Date;

import org.hitlabnz.motionrecorder.events.ImuEvent;
import org.hitlabnz.motionrecorder.events.MotionEvent.EventType;
import org.hitlabnz.motionrecorder.events.MotionEventListener;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * This recorder tracks the acceleration force in m/s2 that is applied to a device on all three physical axes (x, y, and
 * z), including the force of gravity.
 * 
 * @author Alexander Pacha
 * 
 */
public class AccelerometerRecorder extends SensorRecorder implements SensorEventListener {

	/**
	 * The Accelerometer sensor that returns acceleration forces
	 */
	private Sensor accelerometerSensor;

	@Override
	public void initialize(Context context) {
		super.initialize(context);

		accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

	}

	@Override
	public void startRecording(Date startTime, File folderName) {
		super.startRecording(startTime, new File(folderName + File.separator + "Accelerometer.txt"));
		// Start listening
		sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
	}

	@Override
	public void startRecording(Date startTime, MotionEventListener listener) {
		super.startRecording(startTime, listener);
		// Start listening
		sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
	}

	@Override
	public void stopRecording() {
		sensorManager.unregisterListener(this);
		super.stopRecording();
	}

	@Override
	public void close() {
		// Nothing to do
	}

	@Override
	public final void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do something here if sensor accuracy changes.
		Log.d("Sensor", sensor.getName() + ", Accuracy: " + accuracy);
	}

	@Override
	public final void onSensorChanged(SensorEvent event) {
		if (!recording)
			return;

		if (recordingToFile) {
			writeToSDCard(event);
		} else {
			for (MotionEventListener listener : listeners) {
				listener.onMotionEventUpdate(new ImuEvent(event, EventType.Accelerometer, elapsedTimeSinceStart()));
			}
		}
	}
}
