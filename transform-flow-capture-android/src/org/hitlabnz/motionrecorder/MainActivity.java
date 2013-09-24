package org.hitlabnz.motionrecorder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hitlabnz.glassCubeSample.representation.Quaternion;
import org.hitlabnz.motionrecorder.events.FusionEvent;
import org.hitlabnz.motionrecorder.events.MotionEvent;
import org.hitlabnz.motionrecorder.events.MotionEvent.EventType;
import org.hitlabnz.motionrecorder.events.MotionEventListener;
import org.hitlabnz.motionrecorder.recorders.AccelerometerRecorder;
import org.hitlabnz.motionrecorder.recorders.CompassRecorder;
import org.hitlabnz.motionrecorder.recorders.GPSRecorder;
import org.hitlabnz.motionrecorder.recorders.GravityRecorder;
import org.hitlabnz.motionrecorder.recorders.GyroscopeRecorder;
import org.hitlabnz.motionrecorder.recorders.LinearAccelerometerRecorder;
import org.hitlabnz.motionrecorder.recorders.OrientationRecorder;
import org.hitlabnz.motionrecorder.recorders.PictureRecorder;
import org.hitlabnz.motionrecorder.recorders.RotationVectorRecorder;
import org.hitlabnz.motionrecorder.recorders.SensorFusion1Recorder;
import org.hitlabnz.motionrecorder.recorders.SensorFusion2Recorder;
import org.hitlabnz.motionrecorder.recorders.SensorRecorder;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class MainActivity extends Activity implements MotionEventListener {

	public static final int DRAW_RESULT_BITMAP = 10;
	private List<SensorRecorder> recorders;
	SurfaceHolder holder;
	SensorFusion1Recorder sensorFusion1Recorder;
	SensorFusion2Recorder sensorFusion2Recorder;

	/**
	 * The location client that provides location-API
	 */
	private LocationClient locationClient;

	/**
	 * The last known location from the GPSRecorder
	 */
	Location currentLocation;
	boolean firstRun = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, new OpenCVLoaderCallback(this));

		recorders = new ArrayList<SensorRecorder>();
		recorders.add(new GPSRecorder());
		recorders.add(new AccelerometerRecorder());
		recorders.add(new GyroscopeRecorder());
		recorders.add(new CompassRecorder());
		recorders.add(new LinearAccelerometerRecorder());
		recorders.add(new OrientationRecorder());
		recorders.add(new GravityRecorder());
		recorders.add(new RotationVectorRecorder());
		recorders.add(new PictureRecorder());
		sensorFusion1Recorder = new SensorFusion1Recorder();
		sensorFusion2Recorder = new SensorFusion2Recorder();
		recorders.add(sensorFusion1Recorder);
		recorders.add(sensorFusion2Recorder);

	}

	@Override
	protected void onResume() {
		super.onResume();
		for (SensorRecorder recorder : recorders) {
			recorder.initialize(this);

			// Initialise callbacks for virtual sensors
			if (recorder instanceof RotationVectorRecorder || recorder instanceof GyroscopeRecorder) {
				recorder.addMotionEventListener(sensorFusion1Recorder);
				recorder.addMotionEventListener(sensorFusion2Recorder);
			}
		}

		locationClient = new LocationClient(this, new ConnectionCallbacks() {
			@Override
			public void onDisconnected() {
			}

			@Override
			public void onConnected(Bundle bundle) {
				LocationRequest locationRequest = LocationRequest.create();
				// Use high accuracy
				locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
				// Start receiving location updates
				locationClient.requestLocationUpdates(locationRequest, new LocationListener() {
					@Override
					public void onLocationChanged(Location location) {
						currentLocation = location;
						if (firstRun) {
							firstRun = false;
							findViewById(R.id.buttonStoreGpsTag).setEnabled(true);
						}
					}
				});

			}
		}, new OnConnectionFailedListener() {
			@Override
			public void onConnectionFailed(ConnectionResult arg0) {
			}
		});

		locationClient.connect();

	}

	@Override
	protected void onPause() {
		super.onPause();
		for (SensorRecorder recorder : recorders) {
			recorder.close();
		}

		if (locationClient.isConnected()) {
			/* Remove location updates for a listener. The current Activity is the listener, so the argument is "this". */
			locationClient.disconnect();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present

		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void onStartRecordingButtonClick(View view) {
		startRecordingToFile();
	}

	/**
	 * Calls all Sensor-Recorder to record to separate files
	 */
	private void startRecordingToFile() {
		findViewById(R.id.buttonStopRecording).setEnabled(true);
		findViewById(R.id.buttonStartRecording).setEnabled(false);

		Date startDate = new Date();
		// Create a new folder that represents the date and time when we started to record
		File directory = new File(Environment.getExternalStorageDirectory() + File.separator + "MotionRecordings"
				+ File.separator + DateFormat.format("kkmmss", startDate));
		directory.mkdirs();

		for (SensorRecorder recorder : recorders) {
			recorder.startRecording(startDate, directory);
		}
	}

	/**
	 * Calls all sensor-recorders to only report events to this class but don't write anything to SD-card
	 */
	private void startRecording() {
		findViewById(R.id.buttonStopRecording).setEnabled(true);
		findViewById(R.id.buttonStartRecording).setEnabled(false);

		Date startDate = new Date();
		// Create a new folder that represents the date and time when we started to record
		File directory = new File(Environment.getExternalStorageDirectory() + File.separator + "MotionRecordings"
				+ File.separator + DateFormat.format("kkmmss", startDate));
		directory.mkdirs();

		for (SensorRecorder recorder : recorders) {
			//recorder.startRecording(startDate, directory);
			recorder.startRecording(startDate, this);
		}
	}

	public void onStopRecordingButtonClick(View view) {
		findViewById(R.id.buttonStopRecording).setEnabled(false);
		findViewById(R.id.buttonStartRecording).setEnabled(true);
		for (SensorRecorder recorder : recorders) {
			recorder.stopRecording();
		}
	}

	public void onStoredGpsTagClick(View view) {
		File file = new File(Environment.getExternalStorageDirectory() + File.separator + "MotionRecordings"
				+ File.separator + "GpsTags.txt");
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
			String msg = Double.toString(currentLocation.getLatitude()) + ","
					+ Double.toString(currentLocation.getLongitude()) + ";"
					+ ((EditText) findViewById(R.id.editTextGpsTag)).getText() + "\n";
			writer.write(msg);
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * OpenCV has finished loading, so enable controls
	 */
	private void initCameraView() {
		findViewById(R.id.buttonStartRecording).setEnabled(true);
	}

	/**
	 * This class will receive a callback once the OpenCV library is loaded.
	 */
	private static final class OpenCVLoaderCallback extends BaseLoaderCallback {
		private Context context;

		public OpenCVLoaderCallback(Context context) {
			super((Activity) context);
			this.context = context;
		}

		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:

				((MainActivity) context).initCameraView();
				break;
			default:
				super.onManagerConnected(status);
				break;
			}
		}
	}

	@Override
	public void onMotionEventUpdate(MotionEvent event) {
		if (event.eventType == EventType.FusedRotationVector1) {

			Quaternion q = ((FusionEvent) event).fusedQuaternion;

			Log.i("MotionEvent",
					String.format("Time:%.3f;Quaternion:%.3f, %.3f, %.3f, %.3f", event.elapsedTime / 1000f, q.x(),
							q.y(), q.z(), q.w()));

			//			float[] eulerAngles = q.getOrientationValues();
			//			Log.i("MotionEvent", String.format("New FusionEvent; az: %.2f, pi: %.2f, roll: %.2f", eulerAngles[0],
			//					eulerAngles[1], eulerAngles[2]));
		}
		// TODO Handle different events here in a single place

	}
}
