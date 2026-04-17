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

import org.openmrs.api.context.Context;

import java.util.Arrays;
import java.util.Collection;

/**
 * Core event constants for OpenMRS entity lifecycle actions, plus a thin facade over the
 * deprecated subscribe/fire API. New code should subclass {@link TransactionEventListener}
 * and react to {@link TransactionCommittedEvent} directly; the static methods below remain
 * only as a bridge for modules written against the previous ActiveMQ-backed implementation.
 */
public class Event {

	/**
	 * The core-defined actions that represent entity lifecycle events.
	 */
	public enum Action {
		CREATED,
		UPDATED,
		RETIRED,
		UNRETIRED,
		VOIDED,
		UNVOIDED,
		PURGED
	}

	private static LegacyEventBridge bridge() {
		return Context.getRegisteredComponent("legacyEventBridge", LegacyEventBridge.class);
	}

	/**
	 * @deprecated since 5.0.0; subclass {@link TransactionEventListener} instead.
	 */
	@Deprecated
	public static void subscribe(Class<?> clazz, String action, EventListener listener) {
		if (clazz == null || listener == null) {
			return;
		}
		LegacyEventBridge b = bridge();
		if (action != null) {
			b.subscribeClass(clazz, action, listener);
		} else {
			for (Action a : Action.values()) {
				b.subscribeClass(clazz, a.name(), listener);
			}
		}
	}

	/**
	 * @deprecated since 5.0.0; subclass {@link TransactionEventListener} instead.
	 */
	@Deprecated
	public static void subscribe(Class<?> clazz, Collection<String> actions, EventListener listener) {
		if (clazz == null || listener == null) {
			return;
		}
		LegacyEventBridge b = bridge();
		if (actions == null) {
			for (Action a : Action.values()) {
				b.subscribeClass(clazz, a.name(), listener);
			}
		} else {
			for (String action : actions) {
				b.subscribeClass(clazz, action, listener);
			}
		}
	}

	/**
	 * @deprecated since 5.0.0; subclass {@link TransactionEventListener} instead.
	 */
	@Deprecated
	public static void subscribe(String topicName, EventListener listener) {
		if (topicName == null || listener == null) {
			return;
		}
		bridge().subscribeTopic(topicName, listener);
	}

	/**
	 * @deprecated since 5.0.0; subclass {@link TransactionEventListener} instead.
	 */
	@Deprecated
	public static void unsubscribe(Class<?> clazz, Action action, EventListener listener) {
		if (clazz == null || listener == null) {
			return;
		}
		LegacyEventBridge b = bridge();
		if (action != null) {
			b.unsubscribeClass(clazz, action.name(), listener);
		} else {
			for (Action a : Action.values()) {
				b.unsubscribeClass(clazz, a.name(), listener);
			}
		}
	}

	/**
	 * Symmetric counterpart to {@link #subscribe(Class, String, EventListener)} that accepts any
	 * action string, including custom names that are not {@link Action} enum constants.
	 *
	 * @deprecated since 5.0.0; subclass {@link TransactionEventListener} instead.
	 */
	@Deprecated
	public static void unsubscribe(Class<?> clazz, String action, EventListener listener) {
		if (clazz == null || listener == null) {
			return;
		}
		LegacyEventBridge b = bridge();
		if (action != null) {
			b.unsubscribeClass(clazz, action, listener);
		} else {
			for (Action a : Action.values()) {
				b.unsubscribeClass(clazz, a.name(), listener);
			}
		}
	}

	/**
	 * @deprecated since 5.0.0; subclass {@link TransactionEventListener} instead.
	 */
	@Deprecated
	public static void unsubscribe(Class<?> clazz, Collection<Action> actions, EventListener listener) {
		if (clazz == null || listener == null) {
			return;
		}
		LegacyEventBridge b = bridge();
		Collection<Action> toUnsubscribe = actions != null ? actions : Arrays.asList(Action.values());
		for (Action action : toUnsubscribe) {
			b.unsubscribeClass(clazz, action.name(), listener);
		}
	}

	/**
	 * @deprecated since 5.0.0; subclass {@link TransactionEventListener} instead.
	 */
	@Deprecated
	public static void unsubscribe(String topicName, EventListener listener) {
		if (topicName == null || listener == null) {
			return;
		}
		bridge().unsubscribeTopic(topicName, listener);
	}

	/**
	 * @deprecated since 5.0.0; entity events are now published automatically by the Hibernate
	 *             interceptor. Direct invocation remains only for callers that fire ad-hoc
	 *             actions for non-OpenmrsObject sources.
	 */
	@Deprecated
	public static void fireAction(String action, Object object) {
		bridge().fireAction(action, object);
	}

	/**
	 * @deprecated since 5.0.0; subclass {@link TransactionEventListener} instead.
	 */
	@Deprecated
	public static void fireEvent(String topicName, EventMessage eventMessage) {
		bridge().fireTopic(topicName, eventMessage);
	}
}
