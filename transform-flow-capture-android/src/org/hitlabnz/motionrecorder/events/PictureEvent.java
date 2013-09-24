package org.hitlabnz.motionrecorder.events;

import android.graphics.Bitmap;

/**
 * A new picture-event that was received from the camera
 * 
 * @author Alexander Pacha
 * 
 */
public class PictureEvent extends MotionEvent {

	public final Bitmap picture;

	public PictureEvent(Bitmap picture, EventType eventType, long elapsedTime) {
		super(eventType, elapsedTime);

		this.picture = picture;
	}
}
