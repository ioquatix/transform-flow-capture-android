package org.hitlabnz.motionrecorder;

import java.io.File;
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
import org.hitlabnz.motionrecorder.recorders.GravityRecorder;
import org.hitlabnz.motionrecorder.recorders.GyroscopeRecorder;
import org.hitlabnz.motionrecorder.recorders.LinearAccelerometerRecorder;
import org.hitlabnz.motionrecorder.recorders.RotationVectorRecorder;
import org.hitlabnz.motionrecorder.recorders.SensorFusion1Recorder;
import org.hitlabnz.motionrecorder.recorders.SensorFusion2Recorder;
import org.hitlabnz.motionrecorder.recorders.SensorRecorder;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class MainActivity extends Activity implements MotionEventListener {

    public static final int DRAW_RESULT_BITMAP = 10;
    private List<SensorRecorder> recorders;
    SensorFusion1Recorder sensorFusion1Recorder;
    SensorFusion2Recorder sensorFusion2Recorder;
    private GestureDetector gestureDetector;
    private boolean isRecording = false;

    private OnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDoubleTap(android.view.MotionEvent motionEvent) {
            // Start/stop recording
            isRecording = !isRecording;

            if (isRecording) {
                startRecordingToFile();
                ((TextView) findViewById(R.id.textView1)).setText("Recording...");
            } else {
                onStopRecordingButtonClick(null);
                ((TextView) findViewById(R.id.textView1)).setText("Stopped.");
            }

            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glass_main);

        recorders = new ArrayList<SensorRecorder>();
        recorders.add(new AccelerometerRecorder());
        recorders.add(new GyroscopeRecorder());
        recorders.add(new CompassRecorder());
        recorders.add(new LinearAccelerometerRecorder());
        recorders.add(new GravityRecorder());
        recorders.add(new RotationVectorRecorder());
        sensorFusion1Recorder = new SensorFusion1Recorder();
        sensorFusion2Recorder = new SensorFusion2Recorder();
        recorders.add(sensorFusion1Recorder);
        recorders.add(sensorFusion2Recorder);

        gestureDetector = new GestureDetector(this, gestureListener);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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
    }

    @Override
    protected void onPause() {
        super.onPause();
        for (SensorRecorder recorder : recorders) {
            recorder.close();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present

        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onGenericMotionEvent(android.view.MotionEvent) */
    @Override
    public boolean onGenericMotionEvent(android.view.MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return false;
    }

    public void onStartRecordingButtonClick(View view) {
        startRecordingToFile();
        //// Alternatively use
        // startRecording();
    }

    /**
     * Calls all Sensor-Recorder to record to separate files
     */
    private void startRecordingToFile() {

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
        for (SensorRecorder recorder : recorders) {
            recorder.stopRecording();
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
