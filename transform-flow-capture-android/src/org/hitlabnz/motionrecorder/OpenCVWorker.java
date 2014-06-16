package org.hitlabnz.motionrecorder;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

import android.graphics.Bitmap;
import android.util.Log;

public class OpenCVWorker implements Runnable {
    public static final String TAG = "OpenCVWorker";

    public static final int FIRST_CAMERA = 0;
    public static final int SECOND_CAMERA = 1;

    public static final int RESULT_MATRIX_BUFFER_SIZE = 3;
    /**
     * Boolean to indicate whether this thread should keep on running or terminate
     */
    private boolean doProcess;
    private int mCameraId = SECOND_CAMERA;
    private Size mPreviewSize;
    private VideoCapture mCamera;
    private ConcurrentLinkedQueue<Bitmap> mResultBitmaps = new ConcurrentLinkedQueue<Bitmap>();
    private Set<ResultCallback> mResultCallbacks = Collections.synchronizedSet(new HashSet<ResultCallback>());

    /**
     * Matrices used to hold the actual image data for each processing step
     */
    private Mat mCurrentFrame;

    public OpenCVWorker(int cameraId) {
        mCameraId = cameraId;
        // Default preview size
        mPreviewSize = new Size(480, 320);
    }

    public void releaseResultBitmap(Bitmap bitmap) {
        mResultBitmaps.offer(bitmap);
    }

    public void addResultCallback(ResultCallback resultCallback) {
        mResultCallbacks.add(resultCallback);
    }

    public void removeResultCallback(ResultCallback resultCallback) {
        mResultCallbacks.remove(resultCallback);
    }

    public void stopProcessing() {
        doProcess = false;
    }

    // Setup the camera
    private void setupCamera() {
        if (mCamera != null) {
            VideoCapture camera = mCamera;
            mCamera = null; // Make it null before releasing...
            camera.release();
        }

        mCamera = new VideoCapture(mCameraId);

        // Figure out the most appropriate preview size that this camera
        // supports.
        // We always need to do this as each device support different preview
        // sizes for their cameras
        List<Size> previewSizes = mCamera.getSupportedPreviewSizes();
        
        System.out.println(previewSizes);
        double largestPreviewSize = 1280 * 720; // We should be smaller than
                                                // this...
        double smallestWidth = 480; // Let's not get a smaller width than
                                    // this...
        for (Size previewSize : previewSizes) {
            if (previewSize.area() < largestPreviewSize && previewSize.width >= smallestWidth) {
                mPreviewSize = previewSize;
            }
        }

        System.out.println(mPreviewSize.width);
        System.out.println(mPreviewSize.height);

//        mCamera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, 800);
//        mCamera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, 480);
        mCamera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, mPreviewSize.width);
        mCamera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, mPreviewSize.height);
    }

    /**
     * Initialize the matrices and the bitmaps we will use to draw the result
     */
    private void initMatrices() {
        mCurrentFrame = new Mat();

        // Since drawing to screen occurs on a different thread than the
        // processing,
        // we use a queue to handle the bitmaps we will draw to screen
        mResultBitmaps.clear();
        for (int i = 0; i < RESULT_MATRIX_BUFFER_SIZE; i++) {
            Bitmap resultBitmap = Bitmap.createBitmap((int) mPreviewSize.width, (int) mPreviewSize.height,
                    Bitmap.Config.ARGB_8888);
            mResultBitmaps.offer(resultBitmap);
        }
    }

    /**
     * The thread used to grab and process frames
     */
    @Override
    public void run() {
        doProcess = true;

        Log.e(TAG,"Setup Camera");
        setupCamera();

        Log.e(TAG, "Initiate Matrices");
        initMatrices();

        System.out.println(mCamera != null);
        System.out.println(doProcess);
        
        while (doProcess && mCamera != null) {

            boolean grabbed = mCamera.grab();

            if (grabbed) {
                // Retrieve the next frame from the camera in RGB format
                mCamera.retrieve(mCurrentFrame, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGB);
                notifyResultCallback(mCurrentFrame);
            }
        }

        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }

    }

    private void notifyResultCallback(Mat result) {
        Bitmap resultBitmap = mResultBitmaps.poll();
        if (resultBitmap != null) {
            Utils.matToBitmap(result, resultBitmap, true);
            for (ResultCallback resultCallback : mResultCallbacks) {
                resultCallback.onResultMatrixReady(resultBitmap);
            }
        }
    }

    public Size getPreviewSize() {
        return mPreviewSize;
    }

    public interface ResultCallback {
        void onResultMatrixReady(Bitmap mat);
    }
}
