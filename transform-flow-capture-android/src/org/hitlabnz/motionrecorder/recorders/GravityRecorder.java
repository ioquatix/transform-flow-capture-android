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
 * This recorder tracks the force of gravity in m/s2 that is applied to a device on all three physical axes (x, y, z).
 * 
 * @author Alexander Pacha
 * 
 */
public class GravityRecorder extends SensorRecorder implements SensorEventListener {

	/**
	 * The gyro sensor that returns rotation velocities
	 */
	private Sensor gravitySensor;

	@Override
	public void initialize(Context context) {
		super.initialize(context);
		gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
	}

	@Override
	public void startRecording(Date startTime, File folderName) {
		super.startRecording(startTime, new File(folderName + File.separator + "Gravity.txt"));
		// Start listening
		sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	public void startRecording(Date startTime, MotionEventListener listener) {
		super.startRecording(startTime, listener);
		// Start listening
		sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_NORMAL);
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
				listener.onMotionEventUpdate(new ImuEvent(event, EventType.Gravity, elapsedTimeSinceStart()));
			}
		}
	}
}
