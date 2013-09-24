package org.hitlabnz.motionrecorder.events;

/**
 * Interface that classes must implement that want to receive event-notifications, when a MotionEvent was obtained.
 * 
 * @author Alexander Pacha
 * 
 */
public interface MotionEventListener {

	/**
	 * When a sensor is ready to deliver data, this method will be called to notify the listener of the update.
	 * 
	 * @param event The event that was reported
	 */
	public void onMotionEventUpdate(MotionEvent event);
}
