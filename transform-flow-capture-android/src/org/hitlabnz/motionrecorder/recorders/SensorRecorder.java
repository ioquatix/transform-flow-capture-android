package org.hitlabnz.motionrecorder.recorders;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.hitlabnz.motionrecorder.events.FusionEvent;
import org.hitlabnz.motionrecorder.events.MotionEventListener;

import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * This class manages all required resources for accessing, recording and persisting sensor data
 * 
 * @author Carolin, Alex
 * 
 */
public abstract class SensorRecorder {

	/**
	 * The listener that should be notified on changes.
	 */
	protected List<MotionEventListener> listeners;

	/**
	 * The time, when the recording was started
	 */
	protected Date recordingStartDate;

	/**
	 * The application's SensorManager
	 */
	protected SensorManager sensorManager;

	/**
	 * Formatter to write the elapsed time to SD-card.
	 */
	protected SimpleDateFormat formatter;

	/**
	 * The buffered writer that writes the data onto the SD-card
	 */
	protected BufferedWriter writer = null;

	/**
	 * Flag to indicate whether this sensor is currently recording
	 */
	protected boolean recording = false;

	/**
	 * Flag indicating, whether recordings should be written to SD-card or reported to listener.
	 */
	protected boolean recordingToFile = false;

	/**
	 * Initialises the sensor to allow for recording, e.g. initialises SensorManager
	 * 
	 * @param context The application context (i.e. the currently running activity).
	 */
	public void initialize(Context context) {
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		formatter = new SimpleDateFormat("mm:ss.SSS;", Locale.US);
		listeners = new ArrayList<MotionEventListener>();
	}

	public void addMotionEventListener(MotionEventListener listener) {
		listeners.add(listener);
	}

	/**
	 * Starts the recording of this sensor. The recorded data will be written to the SD-card.
	 * 
	 * @param startTime The timestamp, when the recording was started
	 * @param folderName The folder on the SD-Card where the Sensor data should be written to
	 */
	public void startRecording(Date startTime, File folderName) {
		this.recordingStartDate = startTime;
		recording = true;
		recordingToFile = true;
		try {
			// Note that this method gets called from implementation, that construct a real file from the folder-name.
			// So despite the fact that the variable is called folderName, it is the target file.
			writer = new BufferedWriter(new FileWriter(folderName));
		} catch (Exception e) {
			Log.e("SensorRecorder", "Could not open target file for recording: " + folderName.getAbsolutePath(), e);
		}
	}

	/**
	 * Starts the recording of this sensor. The recorded data will be reported to the registered listener
	 * 
	 * @param startTime The timestamp, when the recording was started
	 */
	public void startRecording(Date startTime, MotionEventListener listener) {
		this.recordingStartDate = startTime;
		recording = true;
		listeners.add(listener);
		recordingToFile = false;
	}

	/**
	 * Stops the recording of this sensor, flushes all buffers and write the data to the SD-card.
	 */
	public void stopRecording() {
		recording = false;

		try {
			if (recordingToFile) {
				writer.close();
			}
		} catch (IOException e) {
			Log.e("SensorRecorder", "Could not close output writer", e);
		}
	}

	/**
	 * Closes and releases all resources allocated by this sensor
	 */
	public abstract void close();

	protected void writeToSDCard(SensorEvent event) {
		CharSequence currentTimeStamp = formatter
				.format(new Date((new Date().getTime() - recordingStartDate.getTime())));
		String msg = currentTimeStamp + String.valueOf(event.values[0]) + "," + String.valueOf(event.values[1]) + ","
				+ String.valueOf(event.values[2]) + "\n";
		// Write new location to SD-card
		try {
			writer.write(msg);
		} catch (IOException e) {
			Log.e("SensorRecorder", "Could not write Sensor-Event data from " + event.sensor.getName(), e);
		}
	}

	protected void writeToSDCard(FusionEvent event) {

		// Write new location to SD-card
		String msg = String.format("Time:%.3f;Quaternion:%.3f, %.3f, %.3f, %.3f", event.elapsedTime / 1000f,
				event.fusedQuaternion.x(), event.fusedQuaternion.y(), event.fusedQuaternion.z(),
				event.fusedQuaternion.w())
				+ "\n";
		try {
			writer.write(msg);
		} catch (IOException e) {
			Log.e("SensorRecorder", "Could not write Sensor-Event data from " + event.eventType, e);
		}
	}

	protected long elapsedTimeSinceStart() {
		return (new Date()).getTime() - recordingStartDate.getTime();
	}
}
