package org.hitlabnz.motionrecorder.events;

/**
 * A general motion-event that was received from any of the sensors, built into a mobile device.
 * 
 * @author Alexander Pacha
 * 
 */
public class MotionEvent {

	public final EventType eventType;
	/**
	 * Kind of a timestamp that gives the elapsed time since the record-button was clicked in milliseconds
	 */
	public long elapsedTime;

	public MotionEvent(EventType eventType, long elapsedTime) {
		this.eventType = eventType;
		this.elapsedTime = elapsedTime;
	}

	/**
	 * Enumeration describing the different types of motion event that can occur
	 */
	public enum EventType {
		Accelerometer, Compass, Gravity, Gyroscope, LinearAccelerometer, Orientation, Picture, RotationVector, FusedRotationVector1, FusedRotationVector2, Gps
	}

}
