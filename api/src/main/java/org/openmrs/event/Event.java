/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.event;

import javax.jms.Destination;
import javax.jms.JMSException;

import org.openmrs.OpenmrsObject;

/**
 * Allows listeners to subscribe to possible events. When the event occurs, the listener is called.
 */
public class Event {
	
	static EventEngine eventEngine = new EventEngine();
	
	/**
	 * These are the core-defined actions that go in topics
	 */
	public enum Action {
		CREATED, UPDATED, RETIRED, UNRETIRED, VOIDED, UNVOIDED, PURGED
	};
	
	/**
	 * @param action
	 * @param object
	 */
	public static void fireAction(String action, final OpenmrsObject object) {
		eventEngine.fireAction(action, object);
	}
	
	public static void fireEvent(final Destination dest, final OpenmrsObject object) {
		eventEngine.fireEvent(dest, object);
	}
	
	/**
	 * Creates a subscription for the specified class and action, if action is null, the
	 * subscription is created for all the actions
	 * 
	 * @param openmrsObjectClass
	 * @param action
	 * @should subscribe only to the specified action
	 * @should subscribe to every action if action is null
	 * @should not subscribe duplicate event listeners
	 */
	public static void subscribe(Class<? extends OpenmrsObject> openmrsObjectClass, String action, EventListener listener) {
		eventEngine.subscribe(openmrsObjectClass, action, listener);
	}
	
	/**
	 * Removes the subscription associated to the specified class and action, if action is null all
	 * subscriptions associated to the class are dropped
	 * 
	 * @param openmrsObjectClass if null, all objects are unsubscribed
	 * @param action if null, all actions are unsubscribed
	 * @param listener the given listener to unsubscribe
	 * @should unsubscribe only for the specified action
	 * @should unsubscribe for every action if action is null
	 */
	public static void unsubscribe(Class<? extends OpenmrsObject> openmrsObjectClass, Event.Action action, EventListener listener) {
		eventEngine.unsubscribe(openmrsObjectClass, action, listener);
	}
	
	/**
	 * Creates subscriptions for the specified {@link Destination}
	 * 
	 * @param destination e.g. org.openmrs.Patient.CREATED or org.openmrs.Patient.DELETED
	 * @param listenerToRegister
	 */
	public static void subscribe(Destination destination, final EventListener listenerToRegister) {
		eventEngine.subscribe(destination, listenerToRegister);
	}
	
	/**
	 * Removes the subscription associated to the specified {@link Destination}
	 * 
	 * @param dest
	 * @param listener
	 * @throws JMSException
	 * @should unsubscribe from the specified destination
	 * @should maintain subscriptions to the same topic for other listeners
	 */
	public static void unsubscribe(Destination dest, EventListener listener) {
		eventEngine.unsubscribe(dest, listener);
	}
	
	/**
	 * Called by spring application context. It needs to be non static, but it acts like static.
	 * 
	 * @param listenerToRegister and {@link SubscribableEventListener} that specifies which objects
	 *            and actions it wants to listen to
	 */
	public void setSubscription(SubscribableEventListener listenerToRegister) {
		eventEngine.setSubscription(listenerToRegister);
	}
	
	/**
	 * Called by spring application context.  It needs to be non static, but it acts like static.
	 * 
	 * @param listenerToRegister
	 * @should remove given subscriptions
	 */
	public void unsetSubscription(SubscribableEventListener listenerToRegister) {
		eventEngine.unsetSubscription(listenerToRegister);
	}
	
	/**
	 * Returns destination for the given class and action.
	 * 
	 * @param openmrsObjectClass
	 * @param action
	 * @return the destination
	 */
	public static Destination getDestination(final Class<? extends OpenmrsObject> openmrsObjectClass, final String action) {
		return eventEngine.getDestination(openmrsObjectClass, action);
	}
	
	/**
	 * Closes the underlying shared connection which will close the broker too under the hood
	 */
	public static void shutdown() {
		eventEngine.shutdown();
	}
}
