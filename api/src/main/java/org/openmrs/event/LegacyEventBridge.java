/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.event;

import org.openmrs.OpenmrsObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.jms.MapMessage;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Backwards-compatibility bridge for the deprecated {@link Event} static API.
 * <p>
 * Maintains in-memory subscriber registries and dispatches to legacy {@link EventListener}s
 * in two ways:
 * <ul>
 *   <li>after each committed transaction, by reacting to {@link TransactionCommittedEvent}, and</li>
 *   <li>synchronously when callers invoke {@link Event#fireAction} or {@link Event#fireEvent}.</li>
 * </ul>
 * A subscription registered against any ancestor type of an event's entity (superclass or
 * interface, transitively) receives the event. This mirrors the behavior of the old
 * {@code EventClassScanner} — which pre-expanded subscriptions to every concrete subtype on the
 * classpath at subscribe time — but does the lookup at dispatch time instead, which also covers
 * subclasses loaded after the subscription was registered. The per-class ancestor set is cached
 * via {@link ClassValue}.
 */
@Component("legacyEventBridge")
@SuppressWarnings("DeprecatedIsStillUsed")
@Deprecated
public class LegacyEventBridge extends TransactionEventListener {

	private static final Logger log = LoggerFactory.getLogger(LegacyEventBridge.class);

	static final String DELIMITER = ":";

	private static final ClassValue<Set<Class<?>>> ANCESTORS = new ClassValue<Set<Class<?>>>() {
		@Override
		protected Set<Class<?>> computeValue(Class<?> type) {
			Set<Class<?>> ancestors = new LinkedHashSet<>();
			collect(type, ancestors);
			ancestors.remove(Object.class);
			return Collections.unmodifiableSet(ancestors);
		}

		private void collect(Class<?> c, Set<Class<?>> into) {
			if (c == null || !into.add(c)) {
				return;
			}
			collect(c.getSuperclass(), into);
			for (Class<?> iface : c.getInterfaces()) {
				collect(iface, into);
			}
		}
	};

	private final ConcurrentMap<String, Set<EventListener>> classListeners = new ConcurrentHashMap<>();

	private final ConcurrentMap<String, Set<EventListener>> topicListeners = new ConcurrentHashMap<>();

	void subscribeClass(Class<?> clazz, String action, EventListener listener) {
		classListeners.computeIfAbsent(key(clazz, action), k -> ConcurrentHashMap.newKeySet()).add(listener);
	}

	void unsubscribeClass(Class<?> clazz, String action, EventListener listener) {
		Set<EventListener> ls = classListeners.get(key(clazz, action));
		if (ls != null) {
			ls.remove(listener);
		}
	}

	void subscribeTopic(String topic, EventListener listener) {
		topicListeners.computeIfAbsent(topic, k -> ConcurrentHashMap.newKeySet()).add(listener);
	}

	void unsubscribeTopic(String topic, EventListener listener) {
		Set<EventListener> ls = topicListeners.get(topic);
		if (ls != null) {
			ls.remove(listener);
		}
	}

	void fireTopic(String topicName, EventMessage payload) {
		dispatch(topicListeners.get(topicName), payload);
	}

	void fireAction(String action, Object object) {
		if (action == null || object == null) {
			return;
		}
		dispatchToAncestors(object, action, buildPayload(object, action));
	}

	@Override
	public void transactionCommitted(TransactionCommittedEvent event) {
		for (EntityEvent entityEvent : event.getEvents()) {
			OpenmrsObject entity = entityEvent.getEntity();
			String action = entityEvent.getAction().name();
			dispatchToAncestors(entity, action, buildPayload(entity, action));
		}
	}

	private void dispatchToAncestors(Object entity, String action, EventMessage payload) {
		MapMessage message = null;
		for (Class<?> c : ANCESTORS.get(entity.getClass())) {
			Set<EventListener> listeners = classListeners.get(key(c, action));
			if (listeners == null || listeners.isEmpty()) {
				continue;
			}
			if (message == null) {
				message = LegacyMapMessage.of(payload);
			}
			deliver(listeners, message);
		}
	}

	private void dispatch(Set<EventListener> listeners, EventMessage payload) {
		if (listeners == null || listeners.isEmpty()) {
			return;
		}
		deliver(listeners, LegacyMapMessage.of(payload));
	}

	private void deliver(Set<EventListener> listeners, MapMessage message) {
		// ConcurrentHashMap.newKeySet iterators are weakly consistent, so concurrent
		// unsubscribe during iteration is safe without a defensive copy
		for (EventListener listener : listeners) {
			try {
				listener.onMessage(message);
			} catch (Exception e) {
				// match old EventEngine semantics: a single listener failure must not
				// prevent other listeners from being invoked
				log.warn("Legacy event listener {} threw while handling event", listener.getClass(), e);
			}
		}
	}

	private static EventMessage buildPayload(Object source, String action) {
		EventMessage msg = new EventMessage();
		if (source instanceof OpenmrsObject) {
			msg.put("uuid", ((OpenmrsObject) source).getUuid());
		}
		msg.put("classname", source.getClass().getName());
		msg.put("action", action);
		return msg;
	}

	static String key(Class<?> clazz, String action) {
		return action + DELIMITER + clazz.getName();
	}
}
