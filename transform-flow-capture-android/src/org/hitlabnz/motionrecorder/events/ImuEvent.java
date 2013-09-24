package org.hitlabnz.motionrecorder.events;

import android.hardware.SensorEvent;

/**
 * An event that was received from an IMU-sensor through the Android SensorManager
 * 
 * @author Alexander Pacha
 * 
 */
public class ImuEvent extends MotionEvent {

	public final SensorEvent originalEvent;

	public ImuEvent(SensorEvent event, EventType eventType, long elapsedTime) {
		super(eventType, elapsedTime);
		this.originalEvent = event;
	}
}
