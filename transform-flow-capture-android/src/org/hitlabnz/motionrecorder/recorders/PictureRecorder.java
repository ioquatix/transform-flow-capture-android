/**
 * 
 */
package org.hitlabnz.motionrecorder.recorders;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import org.hitlabnz.motionrecorder.OpenCVWorker;
import org.hitlabnz.motionrecorder.events.MotionEvent.EventType;
import org.hitlabnz.motionrecorder.events.MotionEventListener;
import org.hitlabnz.motionrecorder.events.PictureEvent;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

/**
 * Class for recording the video from the camera in separate image-files instead of in one video.
 * 
 * @author Alexander Pacha
 * 
 */
public class PictureRecorder extends SensorRecorder implements OpenCVWorker.ResultCallback {
	/**
	 * Tag for logging
	 */
	private static final String TAG = PictureRecorder.class.getName();

	public static final int DRAW_RESULT_BITMAP = 10;
	private OpenCVWorker mWorker;
	int pictureCounter = 0;

	String folderPath;

	/* (non-Javadoc)
	 * @see org.hitlabnz.motionrecorder.recorders.SensorRecorder#initialize(android.content.Context) */
	@Override
	public void initialize(Context context) {
		Log.e(TAG, "Picture Recorder Initilizes");
		super.initialize(context);
	}

	/* (non-Javadoc)
	 * @see org.hitlabnz.motionrecorder.recorders.SensorRecorder#startRecording(java.util.Date, java.io.File) */
	@Override
	public void startRecording(Date startTime, File folderName) {
		super.startRecording(startTime, new File(folderName + File.separator + "Pictures.txt"));
		// Save the date when recording has started
		pictureCounter = 0;
		folderPath = folderName.getAbsolutePath();
		mWorker = new OpenCVWorker(OpenCVWorker.FIRST_CAMERA);
		mWorker.addResultCallback(this);
		new Thread(mWorker).start();
	}

	@Override
	public void startRecording(Date startTime, MotionEventListener listener) {
		super.startRecording(startTime, listener);
		// Save the date when recording has started
		pictureCounter = 0;
		mWorker = new OpenCVWorker(OpenCVWorker.FIRST_CAMERA);
		mWorker.addResultCallback(this);
		new Thread(mWorker).start();
	}

	/* (non-Javadoc)
	 * @see org.hitlabnz.motionrecorder.recorders.SensorRecorder#stopRecording() */
	@Override
	public void stopRecording() {
		if (mWorker != null) {
			mWorker.stopProcessing();
			mWorker.removeResultCallback(this);
		}
		super.stopRecording();
	}

	/* (non-Javadoc)
	 * @see org.hitlabnz.motionrecorder.recorders.SensorRecorder#close() */
	@Override
	public void close() {
		// Nothing to do here
	}

	@Override
	public void onResultMatrixReady(Bitmap resultBitmap) {
		Log.e(TAG,"Picture Matrix Ready");
		
		if (!recording)
			return;

		if (recordingToFile) {

			CharSequence currentTimeStamp = formatter.format(new Date((new Date().getTime() - recordingStartDate
					.getTime())));
			String msg = currentTimeStamp + String.valueOf(pictureCounter) + ".jpg" + "\n";

			// Write current timestamp to SD-card
			try {
				writer.write(msg);
			} catch (IOException e) {
				Log.e(TAG, "Could not write picture capture timestamp data", e);
			}

			// Store to sd-card
			File file = new File(folderPath, pictureCounter + ".jpg");
			pictureCounter++;
			FileOutputStream fOut;
			try {
				fOut = new FileOutputStream(file);
				resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
				fOut.flush();
				fOut.close();
			} catch (Exception e) {
				Log.e(TAG, "Error when writing image to sd-card", e);
			}
			// Release bitmap
			mWorker.releaseResultBitmap(resultBitmap);
		} else {
			for (MotionEventListener listener : listeners) {
				listener.onMotionEventUpdate(new PictureEvent(resultBitmap, EventType.Picture, elapsedTimeSinceStart()));
			}
			mWorker.releaseResultBitmap(resultBitmap);
		}
	}
}
