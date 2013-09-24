package org.hitlabnz.motionrecorder.recorders;

import java.io.File;
import java.util.Date;

import org.hitlabnz.glassCubeSample.representation.Quaternion;
import org.hitlabnz.motionrecorder.events.FusionEvent;
import org.hitlabnz.motionrecorder.events.ImuEvent;
import org.hitlabnz.motionrecorder.events.MotionEvent;
import org.hitlabnz.motionrecorder.events.MotionEvent.EventType;
import org.hitlabnz.motionrecorder.events.MotionEventListener;

import android.content.Context;
import android.hardware.SensorEvent;
import android.util.Log;

/**
 * This recorder fuses two sensors and outputs a fused result
 * 
 * @author Alexander Pacha
 * 
 */
public class SensorFusion2Recorder extends SensorRecorder implements MotionEventListener {
    /**
     * The Quaternions that contain the current rotation (Angle and axis in Quaternion format) This quaternion is
     * used for rendering, so it contains the currently displayed value
     */
    private Quaternion mQuaternion = new Quaternion();

    /**
     * The quaternion that contains the absolute orientation as obtained by the rotationVector sensor.
     */
    private Quaternion mQuaternionRotationVector = new Quaternion();
    /**
     * Constant specifying the factor between a Nano-second and a second
     */
    private static final float NS2S = 1.0f / 1000000000.0f;

    /**
     * The quaternion that stores the difference that is obtained by the gyroscope.
     * Basically it contains a rotational difference encoded into a quaternion.
     * 
     * To obtain the absolute orientation one must add this into an initial position by
     * multiplying it with another quaternion
     */
    private final Quaternion deltaQuaternion = new Quaternion();

    /**
     * The time-stamp being used to record the time when the last gyroscope event occurred.
     */
    private float timestamp;

    /**
     * This is a filter-threshold for discarding Gyroscope measurements that are below a certain level and
     * potentially are only noise and not real motion. Values from the gyroscope are usually between 0 (stop) and
     * 10 (rapid rotation), so 0.1 seems to be a reasonable threshold to filter noise (usually smaller than 0.1) and
     * real motion (usually > 0.1). Note that there is a chance of missing real motion, if the use is turning the
     * device really slowly, so this value has to find a balance between accepting noise (threshold = 0) and missing
     * slow user-action (threshold > 0.5). 0.1 seems to work fine for most applications.
     * 
     */
    private static final double EPSILON = 0.05f;

    /**
     * Value giving the total velocity of the gyroscope (will be high, when the device is moving fast and low when
     * the device is standing still). This is usually a value between 0 and 10 for normal motion. Heavy shaking can
     * increase it to about 25. Keep in mind, that these values are time-depended, so changing the sampling rate of
     * the sensor will affect this value!
     */
    private double gyroscopeRotationVelocity = 0;

    /**
     * Counter that sums the number of consecutive frames, where the rotationVector and the gyroscope were
     * significantly different (and the dot-product was smaller than 0.7). This event can either happen when the
     * angles of the rotation vector explode (e.g. during fast tilting) or when the device was shaken heavily and
     * the gyroscope is now completely off.
     */
    private int panicCounter;

    /**
     * Flag that indicates, that the original rotation is initialised
     */
    private boolean init = false;

    /**
     * This weight determines indirectly how much the rotation sensor will be used to correct. This weight will be
     * multiplied by the velocity to obtain the actual weight. (in sensor-fusion-scenario 2 -
     * SensorSelection.GyroscopeAndRotationVector2).
     * Must be a value between 0 and
     */
    private static final float INDIRECT_INTERPOLATION_WEIGHT = 0.01f;

    /**
     * The threshold that indicates that a chaos state has been established rather than just a temporary peak in the
     * rotation vector (caused by exploding angled during fast tilting).
     * 
     * If the chaosCounter is bigger than this threshold, the current position will be reset to whatever the
     * rotation vector indicates.
     */
    private static final int PANIC_THRESHOLD = 60;

    /**
     * The threshold that indicates an outlier of the rotation vector. If the dot-product between the two vectors
     * (gyroscope orientation and rotationVector orientation) falls below this threshold (ideally it should be 1,
     * if they are exactly the same) the system falls back to the gyroscope values only and just ignores the
     * rotation vector.
     * 
     * This value should be quite high (> 0.7) to filter even the slightest discrepancies that causes jumps when
     * tiling the device. Possible values are between 0 and 1, where a value close to 1 means that even a very small
     * difference between the two sensors will be treated as outlier, whereas a value close to zero means that the
     * almost any discrepancy between the two sensors is tolerated.
     */
    private static final float OUTLIER_THRESHOLD = 0.85f;

    /**
     * The threshold that indicates a massive discrepancy between the rotation vector and the gyroscope orientation.
     * If the dot-product between the two vectors
     * (gyroscope orientation and rotationVector orientation) falls below this threshold (ideally it should be 1, if
     * they are exactly the same), the system will start increasing the panic counter (that probably indicates a
     * gyroscope failure).
     * 
     * This value should be lower than OUTLIER_THRESHOLD (0.5 - 0.7) to only start increasing the panic counter,
     * when there is a
     * huge discrepancy between the two fused sensors.
     */
    private static final float OUTLIER_PANIC_THRESHOLD = 0.6f;

    @Override
    public void initialize(Context context) {
        super.initialize(context);
    }

    @Override
    public void startRecording(Date startTime, File folderName) {
        super.startRecording(startTime, new File(folderName + File.separator + "SensorFusion2.txt"));
    }

    @Override
    public void startRecording(Date startTime, MotionEventListener listener) {
        super.startRecording(startTime, listener);
    }

    @Override
    public void stopRecording() {
        super.stopRecording();
    }

    @Override
    public void close() {
        // Nothing to do
    }

    @Override
    public void onMotionEventUpdate(MotionEvent motionEvent) {
        if (!recording)
            return;

        // Two types of events can arrive here: Gyroscope-event or RotationVector-event.
        // On rotVector-event, store the value
        if (motionEvent.eventType == EventType.RotationVector) {
            // Calculate angle. Starting with API_18, Android will provide this value as event.values[3], but if not, we have to calculate it manually.
            float w = 0;
            SensorEvent event = ((ImuEvent) motionEvent).originalEvent;
            if (event.values.length == 4) {
                w = event.values[3];
            } else {
                w = 1 - event.values[0] * event.values[0] - event.values[1] * event.values[1] - event.values[2]
                        * event.values[2];
                w = (w > 0) ? (float) Math.sqrt(w) : 0;
            }
            // Store in quaternion
            mQuaternionRotationVector.setXYZW(event.values[0], event.values[1], event.values[2], w);
            if (!init) {
                mQuaternion.setXYZW(event.values[0], event.values[1], event.values[2], w);
                init = true;
            }

        } else if (motionEvent.eventType == EventType.Gyroscope) {

            // On Gyro-event, PERFORM FUSION

            SensorEvent event = ((ImuEvent) motionEvent).originalEvent;
            // Process raw data
            if (timestamp != 0) {
                final float dT = (event.timestamp - timestamp) * NS2S;
                // Axis of the rotation sample, not normalized yet.
                float axisX = event.values[0];
                float axisY = event.values[1];
                float axisZ = event.values[2];

                // Calculate the angular speed of the sample
                gyroscopeRotationVelocity = Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

                // Output the gyroscope velocity that will be used as a weighting factor in Sensor Fusion scenario 2
                Log.d("RotationVelocity", String.format("Gyroscope Velocity %.2f", gyroscopeRotationVelocity));

                // Normalize the rotation vector if it's big enough to get the axis
                if (gyroscopeRotationVelocity > EPSILON) {
                    axisX /= gyroscopeRotationVelocity;
                    axisY /= gyroscopeRotationVelocity;
                    axisZ /= gyroscopeRotationVelocity;
                }

                // Integrate around this axis with the angular speed by the timestep
                // in order to get a delta rotation from this sample over the timestep
                // We will convert this axis-angle representation of the delta rotation
                // into a quaternion before turning it into the rotation matrix.
                double thetaOverTwo = gyroscopeRotationVelocity * dT / 2.0f;
                double sinThetaOverTwo = Math.sin(thetaOverTwo);
                double cosThetaOverTwo = Math.cos(thetaOverTwo);
                deltaQuaternion.setX((float) (sinThetaOverTwo * axisX));
                deltaQuaternion.setY((float) (sinThetaOverTwo * axisY));
                deltaQuaternion.setZ((float) (sinThetaOverTwo * axisZ));
                deltaQuaternion.setW((float) cosThetaOverTwo);
            }
            timestamp = event.timestamp;

            // Fuse data
            if (deltaQuaternion != null && init) {

                // Calculate new position from Gyro
                deltaQuaternion.multiplyByQuat(mQuaternion, mQuaternion);

                // Calculate the dot-product between two vectors. Will be 1, if they are the same and < 1, if they are not.
                float dotProduct = Math.abs(mQuaternion.dotProduct(mQuaternionRotationVector));

                if (dotProduct < OUTLIER_THRESHOLD) {
                    // Increase panic counter
                    if (dotProduct < OUTLIER_PANIC_THRESHOLD)
                        panicCounter++;
                } else {
                    // Reset the counter
                    panicCounter = 0;

                    // Interpolate with a weight between the two absolute quaternions obtained from gyro and rotation vector sensors
                    // The weight should always be quite low, so the rotation vector corrects the gyro only slowly, and the output keeps responsive.
                    Quaternion interpolate = new Quaternion();
                    mQuaternion.slerp(mQuaternionRotationVector, interpolate, (float) gyroscopeRotationVelocity
                            * INDIRECT_INTERPOLATION_WEIGHT);

                    // Use the interpolated value between gyro and rotationVector
                    mQuaternion.copyVec4(interpolate);
                }

                if (panicCounter > PANIC_THRESHOLD) {

                    // Log.d(TAG,"Panic counter is bigger than threshold; this indicates a Gyroscope failure. Panic reset is imminent.");
                    if (gyroscopeRotationVelocity < 3) {
                        // Log.d(TAG, "Performing Panic-reset. Resetting orientation to rotation-vector value.");

                        // Manually set position to whatever rotation vector says.
                        // TODO: It might be useful to not do a hard-reset but a quick back-smooth but for now this works fine.
                        mQuaternion.copyVec4(mQuaternionRotationVector);

                        panicCounter = 0;
                    } else {
                        // Log.d(TAG, String.format("Panic reset delayed due to ongoing motion (user is still shaking the device). Gyroscope Velocity: %.2f",gyroscopeRotationVelocity));
                    }
                }

                //				if (mQuaternion.w() < 0) {
                //					mQuaternion.x(-mQuaternion.x());
                //					mQuaternion.y(-mQuaternion.y());
                //					mQuaternion.z(-mQuaternion.z());
                //					mQuaternion.w(-mQuaternion.w());
                //				}

                // Notify listener
                if (recordingToFile) {
                    writeToSDCard(new FusionEvent(mQuaternion, EventType.FusedRotationVector2, elapsedTimeSinceStart()));
                } else {
                    for (MotionEventListener listener : listeners) {
                        listener.onMotionEventUpdate(new FusionEvent(mQuaternion, EventType.FusedRotationVector2,
                                elapsedTimeSinceStart()));
                    }
                }
            }
        }
    }
}
