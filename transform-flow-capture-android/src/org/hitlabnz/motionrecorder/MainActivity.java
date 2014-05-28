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
import org.hitlabnz.motionrecorder.events.GpsEvent;
import org.hitlabnz.motionrecorder.events.ImuEvent;
import org.hitlabnz.motionrecorder.events.MotionEvent;
import org.hitlabnz.motionrecorder.events.MotionEvent.EventType;
import org.hitlabnz.motionrecorder.events.MotionEventListener;
import org.hitlabnz.motionrecorder.events.PictureEvent;
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
	/**
	 * Synchronised running-counter for each incoming event
	 */
	private int eventIndex = 0;
	/**
	 * Synchronised running-counter for each incoming image
	 */
	private int pictureIndex = 0;

	private final String TAG = MainActivity.class.getName();

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
		eventIndex = 0;
		pictureIndex = 0;
		startRecordingToFile();
		//startRecording();
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

		// Example: 1, Location, 349397.8437, -43.521986, 172.582577, 32.1188, 10.0000, 3.0000
		// Example: 2, Heading, 349393.2863, 25.4853, 49.0160
		// Example: 4, Gyroscope, 349398.1878, 0.045122, 0.048391, -0.022473
		// Example: 5, Accelerometer, 349398.1878, -0.003917, -0.004445, -0.001590
		// Example: 6, Gravity, 349398.1878, -0.006581, -0.999796, -0.019071
		// Example: 7, Motion, 349398.1878
		// Example: 8, Gyroscope, 349398.2506, 0.019030, -0.001914, -0.015382
		// Example: 9, Accelerometer, 349398.2506, 0.002335, -0.014158, 0.014360
		// Example: 10, Gravity, 349398.2506, -0.005585, -0.999804, -0.018983
		// Example: 11, Motion, 349398.2506
		// Example: 12, Gyroscope, 349398.3133, -0.034165, -0.015297, -0.007661
		// Example: 13, Accelerometer, 349398.3133, -0.002822, 0.008537, 0.012496
		// Example: 14, Gravity, 349398.3133, -0.004655, -0.999809, -0.018997
		// Example: 15, Motion, 349398.3133
		// Example: 16, Frame, 349398.2933, 1
		// Example: 17, Gyroscope, 349398.3761, -0.008112, 0.005871, 0.001346
		// Example: 18, Accelerometer, 349398.3761, -0.008369, 0.003892, 0.009681
		// Example: 19, Gravity, 349398.3761, -0.004021, -0.999803, -0.019446
		// Example: 20, Motion, 349398.3761
		// Example: 21, Gyroscope, 349398.4388, -0.012919, -0.022335, 0.003316
		// Example: 22, Accelerometer, 349398.4388, 0.013700, -0.014823, -0.023343
		// Example: 23, Gravity, 349398.4388, -0.003736, -0.999764, -0.021395
		// Example: 24, Motion, 349398.4388
		// Example: 25, Frame, 349398.4600, 2
		// Example: 26, Gyroscope, 349398.5016, -0.010605, 0.002553, 0.004502
		// Example: 27, Accelerometer, 349398.5016, -0.009336, -0.003477, 0.015266
		// Example: 28, Gravity, 349398.5016, -0.005099, -0.999773, -0.020668
		// Example: 29, Motion, 349398.5016
		// Example: 30, Gyroscope, 349398.5644, 0.034280, -0.018309, 0.041383
		// Example: 31, Accelerometer, 349398.5644, 0.001692, -0.012252, 0.006426
		// Example: 32, Gravity, 349398.5644, -0.005354, -0.999756, -0.021411
		// Example: 33, Motion, 349398.5644
		// Example: 36, Gravity, 349398.6271, -0.007559, -0.999711, -0.022834
		// Example: 37, Motion, 349398.6271
		// Example: 38, Frame, 349398.5934, 3
		// Example: 39, Gyroscope, 349398.6899, 0.074108, -0.001966, -0.012361
		// Example: 40, Accelerometer, 349398.6899, 0.000289, -0.001352, -0.018369
		// Example: 42, Motion, 349398.6899

		synchronized (this) {
			eventIndex++;

			switch (event.eventType) {
			case Accelerometer:
				// Example: 35, Accelerometer, 349398.6271, -0.011820, 0.005234, -0.016350
				ImuEvent accEvent = (ImuEvent) event;
				Log.i(TAG, String.format("%d, Accelerometer, %.3f, %f, %f, %f", eventIndex, event.elapsedTime / 1000f,
						accEvent.originalEvent.values[0], accEvent.originalEvent.values[1],
						accEvent.originalEvent.values[2]));
				break;
			case Compass:
				// Example: 41, Magnetometer, 349398.6899, -0.007628, -0.999808, -0.018054
				ImuEvent compassEvent = (ImuEvent) event;
				Log.i(TAG, String.format("%d, Magnetometer, %.3f, %f, %f, %f", eventIndex, event.elapsedTime / 1000f,
						compassEvent.originalEvent.values[0], compassEvent.originalEvent.values[1],
						compassEvent.originalEvent.values[2]));
				break;
			case FusedRotationVector1:
				break;
			case FusedRotationVector2:
				break;
			case Gps:
				GpsEvent gpsEvent = (GpsEvent) event;
				// Example: 1, Location, 349332.8458, -43.521736, 172.582731, 29.8943, 10.0000, 3.0000
				Log.i(TAG, String.format("%d, Location, %.3f, %f, %f, %f, %f, %f", eventIndex,
						event.elapsedTime / 1000f, gpsEvent.location.getLongitude(), gpsEvent.location.getLatitude(),
						gpsEvent.location.getAltitude(), gpsEvent.location.getAccuracy(), -1f));
				break;
			case Gravity:
				// Example: 41, Gravity, 349398.6899, -0.007628, -0.999808, -0.018054
				ImuEvent gravityEvent = (ImuEvent) event;
				Log.i(TAG, String.format("%d, Gravity, %.3f, %f, %f, %f", eventIndex, event.elapsedTime / 1000f,
						gravityEvent.originalEvent.values[0], gravityEvent.originalEvent.values[1],
						gravityEvent.originalEvent.values[2]));
				break;
			case Gyroscope:
				// Example: 34, Gyroscope, 349398.6271, -0.010987, -0.070469, 0.000922
				ImuEvent gyroEvent = (ImuEvent) event;
				Log.i(TAG, String.format("%d, Gravity, %.3f, %f, %f, %f", eventIndex, event.elapsedTime / 1000f,
						gyroEvent.originalEvent.values[0], gyroEvent.originalEvent.values[1],
						gyroEvent.originalEvent.values[2]));
				break;
			case LinearAccelerometer:
				ImuEvent linAccEvent = (ImuEvent) event;
				Log.i(TAG, String.format("%d, LinearAccelerometer, %.3f, %f, %f, %f", eventIndex,
						event.elapsedTime / 1000f, linAccEvent.originalEvent.values[0],
						linAccEvent.originalEvent.values[1], linAccEvent.originalEvent.values[2]));
				break;
			case Orientation:
				// Not used, since deprecated
				break;
			case Picture:
				// Example: 3, Frame, 349398.1598, 0
				PictureEvent picEvent = (PictureEvent) event;
				pictureIndex++;
				Log.i(TAG, String.format("%d, Frame, %.3f, %d", eventIndex, event.elapsedTime / 1000f, pictureIndex));
				break;
			case RotationVector:
				break;
			default:
				break;

			}

			// TODO: Motion should bundle multiple events together 

		}

		if (event.eventType == EventType.FusedRotationVector1) {

			Quaternion q = ((FusionEvent) event).fusedQuaternion;

			//            Log.i("MotionEvent",
			//                    String.format("Time:%.3f;Quaternion:%.3f, %.3f, %.3f, %.3f", event.elapsedTime / 1000f, q.x(),
			//                            q.y(), q.z(), q.w()));

			//			float[] eulerAngles = q.getOrientationValues();
			//			Log.i("MotionEvent", String.format("New FusionEvent; az: %.2f, pi: %.2f, roll: %.2f", eulerAngles[0],
			//					eulerAngles[1], eulerAngles[2]));
		}
		// TODO Handle different events here in a single place

	}
}
