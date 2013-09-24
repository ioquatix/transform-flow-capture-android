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
 * This recorder tracks the ambient geomagnetic field for all three physical axes (x, y, z) in μT
 * 
 * @author Alexander Pacha
 * 
 */
public class CompassRecorder extends SensorRecorder implements SensorEventListener {

	/**
	 * The compass sensor that returns magnetic field strengths
	 */
	private Sensor compassSensor;

	@Override
	public void initialize(Context context) {
		super.initialize(context);
		compassSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	}

	@Override
	public void startRecording(Date startTime, File folderName) {
		super.startRecording(startTime, new File(folderName + File.separator + "Compass.txt"));
		// Start listening
		sensorManager.registerListener(this, compassSensor, SensorManager.SENSOR_DELAY_GAME);
	}

	@Override
	public void startRecording(Date startTime, MotionEventListener listener) {
		super.startRecording(startTime, listener);
		// Start listening
		sensorManager.registerListener(this, compassSensor, SensorManager.SENSOR_DELAY_GAME);
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
				listener.onMotionEventUpdate(new ImuEvent(event, EventType.Compass, elapsedTimeSinceStart()));
			}
		}
	}
}
