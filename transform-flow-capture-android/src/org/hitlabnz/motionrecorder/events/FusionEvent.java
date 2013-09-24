package org.hitlabnz.motionrecorder.events;

import org.hitlabnz.glassCubeSample.representation.Quaternion;

/**
 * A fused sensor-event that contains rotation in form of a quaternion
 * 
 * @author Alexander Pacha
 * 
 */
public class FusionEvent extends MotionEvent {

	public final Quaternion fusedQuaternion;

	public FusionEvent(Quaternion fusedQuaternion, EventType eventType, long elapsedTime) {
		super(eventType, elapsedTime);
		this.fusedQuaternion = fusedQuaternion;
	}
}
