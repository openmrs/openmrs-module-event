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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.APIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory event engine that replaces the former JMS/ActiveMQ-based implementation.
 * Manages a registry of {@link EventListener}s keyed by topic name, and dispatches
 * {@link OpenMrsEntityEvent}s synchronously to registered listeners.
 */
public class EventEngine {

	protected static final String DELIMITER = ":";

	protected static final Logger log = LoggerFactory.getLogger(EventEngine.class);

	protected final ConcurrentHashMap<String, CopyOnWriteArraySet<EventListener>> subscribers = new ConcurrentHashMap<>();

	/**
	 * @see Event#fireAction(String, Object)
	 */
	public void fireAction(String action, final Object object) {
		String topic = toTopic(object.getClass(), action);
		String uuid = (object instanceof OpenmrsObject) ? ((OpenmrsObject) object).getUuid() : null;

		OpenMrsEntityEvent event = new OpenMrsEntityEvent(this, topic, action, object.getClass().getName(), uuid);
		dispatch(topic, event);
	}

	/**
	 * @see Event#fireEvent(String, EventMessage)
	 */
	public void fireEvent(String topicName, EventMessage eventMessage) {
		if (StringUtils.isBlank(topicName)) {
			throw new APIException("Topic name cannot be null or blank");
		}
		OpenMrsEntityEvent event = new OpenMrsEntityEvent(this, topicName, eventMessage);
		dispatch(topicName, event);
	}

	private void dispatch(String topic, OpenMrsEntityEvent event) {
		Set<EventListener> listeners = subscribers.get(topic);
		if (listeners != null) {
			for (EventListener listener : listeners) {
				try {
					listener.onEvent(event);
				} catch (Exception e) {
                    log.error("Error dispatching event to listener {}", listener.getClass().getName(), e);
				}
			}
		}
	}

	/**
	 * @see Event#subscribe(Class, String, EventListener)
	 */
	public <T> void subscribe(Class<T> clazz, String action, EventListener listener) {
        if (clazz == null || listener == null) {
            return;
        }

        try (SubscriptionContext<T> context = new SubscriptionContext<>(clazz)) {
            if (action != null) {
                subscribeToClass(context, Collections.singletonList(action), listener);
                log.info("{} subscribed to {} events for {}", listener.getClass(), action, clazz.getSimpleName());
            } else {
                subscribeToClass(context, Event.Action.getActionNames(), listener);
                log.info("{} subscribed to all events for {}", listener.getClass(), clazz.getSimpleName());
            }
        }

	}

	/**
	 * @see Event#subscribe(Class, Collection, EventListener)
	 */
	public <T> void subscribe(Class<T> clazz, Collection<String> actions, EventListener listener) {
		if (clazz == null || listener == null) {
			return;
		}

		try (SubscriptionContext<T> context = new SubscriptionContext<>(clazz)) {
			if (actions != null) {
				if (!actions.isEmpty()) {
					subscribeToClass(context, actions, listener);
					log.info("{} subscribed to {} events for {}", listener.getClass(), StringUtils.join(actions, ','), clazz.getSimpleName());
				}
			} else {
				subscribeToClass(context, Event.Action.getActionNames(), listener);
			}
		}
	}

	private <T> void subscribeToClass(SubscriptionContext<T> context, Collection<String> actions, EventListener listener) {
		try {
			for (Class<? extends T> c : context.getEventClasses()) {
				for (String action : actions) {
					String topic = toTopic(c, action);
					subscribeToTopic(topic, listener);
				}
			}
		} catch (IOException | ClassNotFoundException e) {
			throw new APIException("Exception raised while creating subscription for " + context.getClazz(), e);
		}
	}

	/**
	 * @see Event#subscribe(String, EventListener)
	 */
	public void subscribe(String topicName, EventListener listener) {
		if (StringUtils.isBlank(topicName)) {
			throw new APIException("Topic name cannot be null or blank");
		}
		subscribeToTopic(topicName, listener);
	}

	private void subscribeToTopic(String topic, EventListener listener) {
		subscribers.computeIfAbsent(topic, k -> new CopyOnWriteArraySet<>()).add(listener);
	}

	/**
	 * @see Event#unsubscribe(Class, Event.Action, EventListener)
	 */
	public void unsubscribe(Class<?> clazz, Event.Action action, EventListener listener) {
		if (clazz == null || listener == null) {
			return;
		}

		if (action != null) {
			unsubscribeFromClass(clazz, action.toString(), listener);
			log.info("{} unsubscribed from {} events for {}", action, listener.getClass(), clazz.getSimpleName());
		} else {
			unsubscribeFromClass(clazz, Event.Action.getActionNames(), listener);
			log.info("{} unsubscribed from all events for {}", listener.getClass(), clazz.getSimpleName());
		}
	}

	/**
	 * @see Event#unsubscribe(Class, Collection, EventListener)
	 */
	public void unsubscribe(Class<?> clazz, Collection<Event.Action> actions, EventListener listener) {
		if (clazz == null || listener == null) {
			return;
		}

		if (actions != null) {
			List<String> eventActions = actions.stream().map(Event.Action::toString).collect(Collectors.toList());
			unsubscribeFromClass(clazz, eventActions, listener);
			log.info("{} unsubscribed from {} events for {}", listener.getClass(), StringUtils.join(eventActions, ','), clazz.getSimpleName());
		} else {
			unsubscribeFromClass(clazz, Event.Action.getActionNames(), listener);
			log.info("{} unsubscribed from all events for {}", listener.getClass(), clazz.getSimpleName());
		}
	}

	private <T> void unsubscribeFromClass(Class<T> clazz, String action, EventListener listener) {
		if (clazz == null || listener == null) {
			return;
		}

		try (SubscriptionContext<T> context = new SubscriptionContext<>(clazz)) {
			for (Class<? extends T> c : context.getEventClasses()) {
				String topic = toTopic(c, action);
				unsubscribeFromTopic(topic, listener);
			}
		} catch (IOException | ClassNotFoundException e) {
			throw new APIException(e);
		}
	}

	private <T> void unsubscribeFromClass(Class<T> clazz, Collection<String> actions, EventListener listener) {
		if (clazz == null || listener == null) {
			return;
		}

		try (SubscriptionContext<T> context = new SubscriptionContext<>(clazz)) {
			for (Class<? extends T> c : context.getEventClasses()) {
				for (String action : actions) {
					String topic = toTopic(c, action);
					unsubscribeFromTopic(topic, listener);
				}
			}
		} catch (IOException | ClassNotFoundException e) {
			throw new APIException(e);
		}
	}

	/**
	 * @see Event#unsubscribe(String, EventListener)
	 */
	public void unsubscribe(String topicName, EventListener listener) {
		if (StringUtils.isBlank(topicName)) {
			throw new APIException("Topic name cannot be null or blank");
		}
		unsubscribeFromTopic(topicName, listener);
	}

	private void unsubscribeFromTopic(String topic, EventListener listener) {
		Set<EventListener> listeners = subscribers.get(topic);
		if (listeners != null) {
			listeners.remove(listener);
			if (listeners.isEmpty()) {
				subscribers.remove(topic);
			}
		}
	}

	/**
	 * @see Event#setSubscription(SubscribableEventListener)
	 */
	public void setSubscription(SubscribableEventListener listenerToRegister) {
		for (Class<? extends OpenmrsObject> objectClass : listenerToRegister.subscribeToObjects()) {
			for (String action : listenerToRegister.subscribeToActions()) {
				String topic = toTopic(objectClass, action);
				subscribeToTopic(topic, listenerToRegister);
			}
		}
	}

	/**
	 * @see Event#unsetSubscription(SubscribableEventListener)
	 */
	public void unsetSubscription(SubscribableEventListener listenerToRegister) {
		for (Class<? extends OpenmrsObject> objectClass : listenerToRegister.subscribeToObjects()) {
			for (String action : listenerToRegister.subscribeToActions()) {
				String topic = toTopic(objectClass, action);
				unsubscribeFromTopic(topic, listenerToRegister);
			}
		}
	}

	/**
	 * Returns the topic name for a given class and action
	 */
	public String toTopic(Class<?> clazz, String action) {
		return action + DELIMITER + clazz.getName();
	}

	/**
	 * Inner class for managing subscription context with classpath scanning
	 */
	private static class SubscriptionContext<T> implements AutoCloseable {
		private volatile Collection<Class<? extends T>> eventClasses = null;
		private final EventClassScanner classScanner;
		private final Class<T> clazz;

		public SubscriptionContext(Class<T> clazz) {
			this.classScanner = EventClassScannerThreadHolder.getCurrentEventClassScanner().orElseGet(EventClassScanner::new);
			this.clazz = clazz;
		}

		@Override
		public void close() {
			try {
				if (eventClasses != null) {
					eventClasses.clear();
				}
			} finally {
				classScanner.close();
			}
		}

		public Class<T> getClazz() {
			return clazz;
		}

		public Collection<Class<? extends T>> getEventClasses() throws IOException, ClassNotFoundException {
			if (eventClasses == null) {
				synchronized (this) {
					if (eventClasses == null) {
						eventClasses = classScanner.getClasses(clazz);
					}
				}
			}
			return eventClasses;
		}
	}
}