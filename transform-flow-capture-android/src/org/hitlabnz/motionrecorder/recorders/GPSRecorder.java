package org.hitlabnz.motionrecorder.recorders;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.hitlabnz.motionrecorder.events.GpsEvent;
import org.hitlabnz.motionrecorder.events.MotionEvent.EventType;
import org.hitlabnz.motionrecorder.events.MotionEventListener;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

/**
 * This recorder tracks the user position (longitude and latitude) and writes it to the SD-card.
 * 
 * @author Alexander Pacha
 * 
 */
public class GPSRecorder extends SensorRecorder implements GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {

	/**
	 * Tag for logging
	 */
	private static final String TAG = GPSRecorder.class.getName();

	/**
	 * The location client that provides location-API
	 */
	private LocationClient locationClient;

	/**
	 * Define a request code to send to Google Play services This code is returned in Activity.onActivityResult
	 */
	private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

	/**
	 * Update frequency in milliseconds
	 */
	private static final long UPDATE_INTERVAL = 0;

	/**
	 * A fast frequency ceiling in milliseconds
	 */
	private static final long FASTEST_INTERVAL = 0;

	/**
	 * Application-context
	 */
	private Context context;

	@Override
	public void initialize(Context context) {
		super.initialize(context);
		// Store the context for later use
		this.context = context;
		// Create a new location client, using the enclosing class to handle callbacks.
		locationClient = new LocationClient(context, this, this);
		locationClient.connect();
	}

	@Override
	public void startRecording(Date startTime, File folderName) {
		super.startRecording(startTime, new File(folderName + File.separator + "GPS.txt"));

		// Create the LocationRequest object
		LocationRequest locationRequest = LocationRequest.create();
		// Use high accuracy
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		// Set the update interval to 5 seconds
		locationRequest.setInterval(UPDATE_INTERVAL);
		// Set the fastest update interval to 1 second
		locationRequest.setFastestInterval(FASTEST_INTERVAL);
		// Start receiving location updates
		locationClient.requestLocationUpdates(locationRequest, this);
	}

	@Override
	public void startRecording(Date startTime, MotionEventListener listener) {
		super.startRecording(startTime, listener);
		// Start listening
		// Create the LocationRequest object
		LocationRequest locationRequest = LocationRequest.create();
		// Use high accuracy
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		// Set the update interval to 5 seconds
		locationRequest.setInterval(UPDATE_INTERVAL);
		// Set the fastest update interval to 1 second
		locationRequest.setFastestInterval(FASTEST_INTERVAL);
		// Start receiving location updates
		locationClient.requestLocationUpdates(locationRequest, this);
	}

	@Override
	public void stopRecording() {
		// If the client is connected
		if (locationClient.isConnected()) {
			/* Remove location updates for a listener. The current Activity is the listener, so the argument is "this". */
			locationClient.removeLocationUpdates(this);
		}
		super.stopRecording();
	}

	@Override
	public void close() {
		// Disconnecting the client invalidates it.
		locationClient.disconnect();
	}

	@Override
	public void onConnected(Bundle arg) {
		Log.d(TAG, "GPS-Sensor connected.");
	}

	@Override
	public void onDisconnected() {
		Log.d(TAG, "GPS-Sensor disconnected. Please re-connect.");
	}

	/* Called by Location Services if the attempt to Location Services fails. */
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		/* Google Play services can resolve some errors it detects. If the error has a resolution, try sending an Intent
		 * to start a Google Play services activity that can resolve error. */
		if (connectionResult.hasResolution()) {
			try {
				// Start an Activity that tries to resolve the error
				connectionResult.startResolutionForResult((Activity) context, CONNECTION_FAILURE_RESOLUTION_REQUEST);
				/* Thrown if Google Play services cancelled the original PendingIntent */
			} catch (IntentSender.SendIntentException e) {
				// Log the error
				e.printStackTrace();
			}
		} else {
			/* If no resolution is available, display a dialog to the user with the error. */
			GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), (Activity) context, 0).show();
		}
	}

	@Override
	public void onLocationChanged(Location location) {

		//  location.
		if (!recording)
			return;

		if (recordingToFile) {
			CharSequence currentTimeStamp = formatter.format(new Date((new Date().getTime() - recordingStartDate
					.getTime())));
			// GNSS output file: Lat, Lon, Alt, sigma, UTM time
			String msg = currentTimeStamp + Double.toString(location.getLatitude()) + ","
					+ Double.toString(location.getLongitude()) + "," + Double.toString(location.getAltitude()) + "," + Double.toString(location.getAccuracy()) + "," + Double.toString(location.getTime()) + "\n";

			// Write new location to SD-card
			try {
				writer.write(msg);
			} catch (IOException e) {
				Log.e(TAG, "Could not write GPS data", e);
			}
		} else {
			for (MotionEventListener listener : listeners) {
				listener.onMotionEventUpdate(new GpsEvent(location, EventType.Gps, elapsedTimeSinceStart()));
			}
		}
	}
}
