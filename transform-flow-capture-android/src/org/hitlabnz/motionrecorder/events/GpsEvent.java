package org.hitlabnz.motionrecorder.events;

import android.location.Location;

/**
 * A GPS-event that contains a new location
 * 
 * @author Alexander Pacha
 * 
 */
public class GpsEvent extends MotionEvent {

	public final Location location;

	public GpsEvent(Location location, EventType eventType, long elapsedTime) {
		super(eventType, elapsedTime);
		this.location = location;
	}
}
