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
	}

	/**
	 * Fires an event for the given action and object
	 */
	public static void fireAction(String action, final Object object) {
		eventEngine.fireAction(action, object);
	}

	/**
	 * Fires an event to the specified topic
	 *
	 * @see {@link Action}, {@link EventMessage}
	 */
	public static void fireEvent(String topicName, EventMessage eventMessage) {
		eventEngine.fireEvent(topicName, eventMessage);
	}

	/**
	 * Creates a subscription for the specified class and action, if action is null, the
	 * subscription is created for all the actions
	 */
	public static void subscribe(Class<?> clazz, String action, EventListener listener) {
		eventEngine.subscribe(clazz, action, listener);
	}

	/**
	 * Creates a subscription for the specified class and set of actions, if actions are null, the
	 * subscription is created for all the actions
	 */
	public static void subscribe(Class<?> clazz, Collection<String> actions, EventListener listener) {
		eventEngine.subscribe(clazz, actions, listener);
	}

	/**
	 * Creates a subscription to the topic with the specified name
	 */
	public static void subscribe(String topicName, EventListener listener) {
		eventEngine.subscribe(topicName, listener);
	}

	/**
	 * Removes the subscription associated to the specified class and action, if action is null all
	 * subscriptions associated to the class are dropped
	 */
	public static void unsubscribe(Class<?> clazz, Event.Action action, EventListener listener) {
		eventEngine.unsubscribe(clazz, action, listener);
	}

	/**
	 * Removes the subscription associated to the specified class and set of actions, if action is null all
	 * subscriptions associated to the class are dropped
	 */
	public static void unsubscribe(Class<?> clazz, Collection<Event.Action> actions, EventListener listener) {
		eventEngine.unsubscribe(clazz, actions, listener);
	}

	/**
	 * Removes the subscription from the topic with the specified name
	 */
	public static void unsubscribe(String topicName, EventListener listener) {
		eventEngine.unsubscribe(topicName, listener);
	}

	/**
	 * Called by spring application context. It needs to be non static, but it acts like static.
	 *
	 * @param listenerToRegister a {@link SubscribableEventListener} that specifies which objects
	 *            and actions it wants to listen to
	 */
	public void setSubscription(SubscribableEventListener listenerToRegister) {
		eventEngine.setSubscription(listenerToRegister);
	}

	/**
	 * Called by spring application context. It needs to be non static, but it acts like static.
	 */
	public void unsetSubscription(SubscribableEventListener listenerToRegister) {
		eventEngine.unsetSubscription(listenerToRegister);
	}
}
