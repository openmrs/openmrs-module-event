/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.event;

import javax.jms.Destination;
import javax.jms.JMSException;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Allows listeners to subscribe to possible events. When the event occurs, the listener is called.
 */
public class Event {
	
	static EventEngine eventEngine = new EventEngine();
	
	/**
	 * These are the core-defined actions that go in topics
	 */
	public enum Action {
		
		CREATED,
		UPDATED,
		RETIRED,
		UNRETIRED,
		VOIDED,
		UNVOIDED,
		PURGED;
		
		public static Collection<String> getActionNames() {
			return Arrays.stream(Action.values()).map(Action::name).collect(Collectors.toList());
		}
	};
	
	/**
	 * @param action
	 * @param object
	 */
	public static void fireAction(String action, final Object object) {
		eventEngine.fireAction(action, object);
	}
	
	public static void fireEvent(final Destination dest, final Object object) {
		eventEngine.fireEvent(dest, object);
	}
	
	/**
	 * Fires an event to the specified topic
	 * 
	 * @param topicName
	 * @param eventMessage
	 * @see {@link Action}, {@link EventMessage}
	 */
	public static void fireEvent(String topicName, EventMessage eventMessage) {
		eventEngine.fireEvent(topicName, eventMessage);
	}
	
	/**
	 * Creates a subscription for the specified class and action, if action is null, the subscription is
	 * created for all the actions
	 * 
	 * @param clazz
	 * @param action
	 * @should subscribe only to the specified action
	 * @should subscribe to every action if action is null
	 * @should not subscribe duplicate event listeners
	 */
	public static void subscribe(Class<?> clazz, String action, EventListener listener) {
		eventEngine.subscribe(clazz, action, listener);
	}
	
	/**
	 * Creates a subscription for the specified class and set of actions, if actions are null, the
	 * subscription is created for all the actions
	 *
	 * @param clazz
	 * @param actions
	 * @should subscribe only to the specified action
	 * @should subscribe to every action if action is null
	 * @should not subscribe duplicate event listeners
	 */
	public static void subscribe(Class<?> clazz, Collection<String> actions, EventListener listener) {
		eventEngine.subscribe(clazz, actions, listener);
	}
	
	/**
	 * Creates a subscription to the topic with the specified name
	 * 
	 * @param topicName
	 * @param listener
	 */
	public static void subscribe(String topicName, EventListener listener) {
		eventEngine.subscribe(topicName, listener);
	}
	
	/**
	 * Removes the subscription associated to the specified class and action, if action is null all
	 * subscriptions associated to the class are dropped
	 * 
	 * @param clazz if null, all objects are unsubscribed
	 * @param action if null, all actions are unsubscribed
	 * @param listener the given listener to unsubscribe
	 */
	public static void unsubscribe(Class<?> clazz, Event.Action action, EventListener listener) {
		eventEngine.unsubscribe(clazz, action, listener);
	}
	
	/**
	 * Removes the subscription associated to the specified class and set of actions, if action is null
	 * all subscriptions associated to the class are dropped
	 *
	 * @param clazz if null, all objects are unsubscribed
	 * @param actions if null, all actions are unsubscribed
	 * @param listener the given listener to unsubscribe
	 */
	public static void unsubscribe(Class<?> clazz, Collection<Event.Action> actions, EventListener listener) {
		eventEngine.unsubscribe(clazz, actions, listener);
	}
	
	/**
	 * Removes the subscription from the topic with the specified name
	 * 
	 * @param topicName
	 * @param listener
	 */
	public static void unsubscribe(String topicName, EventListener listener) {
		eventEngine.unsubscribe(topicName, listener);
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
	 * @param listenerToRegister and {@link SubscribableEventListener} that specifies which objects and
	 *            actions it wants to listen to
	 */
	public void setSubscription(SubscribableEventListener listenerToRegister) {
		eventEngine.setSubscription(listenerToRegister);
	}
	
	/**
	 * Called by spring application context. It needs to be non static, but it acts like static.
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
	 * @param clazz
	 * @param action
	 * @return the destination
	 */
	public static Destination getDestination(final Class<?> clazz, final String action) {
		return eventEngine.getDestination(clazz, action);
	}
	
	/**
	 * Returns destination for the given topic
	 * 
	 * @param topicName
	 * @return
	 */
	public static Destination getDestinationFor(String topicName) {
		return eventEngine.getDestination(topicName);
	}
	
	/**
	 * Closes the underlying shared connection which will close the broker too under the hood
	 */
	public static void shutdown() {
		eventEngine.shutdown();
	}
}
