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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

/**
 * Used by {@link Event}.
 */
public class EventEngine {

	protected final static String DELIMITER = ":";

	protected static Logger log = LoggerFactory.getLogger(EventEngine.class);

	protected JmsTemplate jmsTemplate = null;

	protected Map<String, TopicSubscriber> subscribers = new HashMap<String, TopicSubscriber>();

	protected SingleConnectionFactory connectionFactory;

    /**
     * This inner class holds the context for managing a subscription. Basically it serves to simplify using the
     * {@link EventClassScanner} to manage subscriptions for a specific class
     *
     * @param <T>
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

	/**
	 * @see Event#fireAction(String, Object)
	 */
	public void fireAction(String action, final Object object) {
		Destination key = getDestination(object.getClass(), action);
		fireEvent(key, object);
	}

	/**
	 * @see Event#fireEvent(Destination, Object)
	 */
	public void fireEvent(final Destination dest, final Object object) {
		EventMessage eventMessage = new EventMessage();
		if (object instanceof OpenmrsObject) {
			eventMessage.put("uuid", ((OpenmrsObject) object).getUuid());
		}
		eventMessage.put("classname", object.getClass().getName());
		eventMessage.put("action", getAction(dest));

		doFireEvent(dest, eventMessage);
	}

	/**
	 * @see Event#fireEvent(String, EventMessage)
	 */
	public void fireEvent(String topicName, EventMessage eventMessage) {
		if (StringUtils.isBlank(topicName)) {
			throw new APIException("Topic name cannot be null or blank");
		}
		doFireEvent(getDestination(topicName), eventMessage);
	}

	private void doFireEvent(final Destination dest, final EventMessage eventMessage) {
		initializeIfNeeded();

		jmsTemplate.send(dest, session -> {
            if (log.isInfoEnabled())
                log.info("Sending data " + eventMessage);

            MapMessage mapMessage = session.createMapMessage();
            if (eventMessage != null) {
                for (Map.Entry<String, Serializable> entry : eventMessage.entrySet()) {
                    mapMessage.setObject(entry.getKey(), entry.getValue());
                }
            }

            return mapMessage;
        });
	}

	private boolean enabled() {
        return !OpenmrsUtil.getApplicationDataDirectoryAsFile().toPath().resolve("activemq-data").resolve("disabled").toFile().exists();
	}


    private synchronized void initializeIfNeeded() {
		if (jmsTemplate == null) {
            log.info("creating connection factory");
			String property = getExternalUrl();
            String brokerURL;
            if (property == null || property.isEmpty()) {
				String dataDirectory = new File(OpenmrsUtil.getApplicationDataDirectory(), "activemq-data").getAbsolutePath();
				try {
                    dataDirectory = URLEncoder.encode(dataDirectory, "UTF-8");
                }
                catch (UnsupportedEncodingException e) {
                    throw new RuntimeException("Failed to encode URI", e);
                }
                brokerURL = "vm://localhost?broker.persistent=true&broker.useJmx=false&broker.dataDirectory="
                    + dataDirectory;
            } else {
                brokerURL = "tcp://" + property;
            }

            ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(brokerURL);
            connectionFactory = new SingleConnectionFactory(cf);
            jmsTemplate = new JmsTemplate(connectionFactory);
        } else {
            log.trace("messageListener already defined");
        }
    }
    
    private String getExternalUrl() {
    	try {
			Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
    		return Context.getRegisteredComponent("adminService", AdministrationService.class)
					.getGlobalProperty("activeMQ.externalUrl");
    	}
    	catch (NullPointerException ex) {
    		log.error("AdministrationService not yet initialized to get the activeMQ.externalUrl setting" , ex);
    	}
		finally {
			Context.removeProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
		}
    	return null;
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
     *
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

    /**
	 * Adds subscriptions to the topics that match the specified action and class including
	 * subclasses
	 *
	 * @param context the current subscription context
	 * @param actions the action(s) to match
	 * @param listener the Listener subscribing to the topic
	 */
	private <T> void subscribeToClass(SubscriptionContext<T> context, Collection<String> actions, EventListener listener) {
		try {
			for (Class<? extends T> c : context.getEventClasses()) {
                for (String action : actions) {
                    Destination dest = getDestination(c, action);
                    subscribe(dest, listener);
                }
			}
		}
		catch (IOException | ClassNotFoundException e) {
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

		subscribe(getDestination(topicName), listener);
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
    public void unsubscribe(Class<?> clazz, Collection<Event.Action> action, EventListener listener) {
        if (clazz == null || listener == null) {
            return;
        }

        if (action != null) {
            List<String> eventActions = action.stream().map(Event.Action::toString).collect(Collectors.toList());
            unsubscribeFromClass(clazz, eventActions, listener);
            log.info("{} unsubscribed from {} events for {}", listener.getClass(), StringUtils.join(eventActions, ','), clazz.getSimpleName());
        } else {
            unsubscribeFromClass(clazz, Event.Action.getActionNames(), listener);
            log.info("{} unsubscribed from all events for {}", listener.getClass(), clazz.getSimpleName());
        }
    }
	
	/**
	 * Removes subscriptions from the topics that match the specified action and class including
	 * subclasses
	 *
	 * @param clazz the class to match
	 * @param action the action to match
	 * @param listener the Listener subscribing to the top
	 */
	private <T> void unsubscribeFromClass(Class<T> clazz, String action, EventListener listener) {
        if (clazz == null || listener == null) {
            return;
        }

		try (SubscriptionContext<T> context = new SubscriptionContext<>(clazz)) {
			for (Class<? extends T> c : context.getEventClasses()) {
				Destination dest = getDestination(c, action);
				unsubscribe(dest, listener);
			}
		}
		catch (IOException | ClassNotFoundException e) {
			throw new APIException(e);
		}
    }

    /**
     * Removes subscriptions from the topics that match the specified action and class including
     * subclasses
     *
     * @param clazz the class to match
     * @param actions the actions to match
     * @param listener the Listener subscribing to the top
     */
    private <T> void unsubscribeFromClass(Class<T> clazz, Collection<String> actions, EventListener listener) {
        if (clazz == null || listener == null) {
            return;
        }

        try (SubscriptionContext<T> context = new SubscriptionContext<>(clazz)) {
            for (Class<? extends T> c : context.getEventClasses()) {
                for (String action : actions) {
                    Destination dest = getDestination(c, action);
                    unsubscribe(dest, listener);
                }
            }
        }
        catch (IOException | ClassNotFoundException e) {
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
		unsubscribe(getDestination(topicName), listener);
	}
	
	/**
	 * @see Event#getDestination(Class, String)
	 */
	public Destination getDestination(final Class<?> clazz, final String action) {
		return getDestination(action + DELIMITER + clazz.getName());
	}
	
	/**
	 * @see Event#getDestinationFor(String)
	 */
	public Destination getDestination(final String topicName) {
		return (Topic) () -> topicName;
	}
	
	/**
	 * @see Event#subscribe(Destination, EventListener)
	 */
	public void subscribe(Destination destination, final EventListener listenerToRegister) {
		if(enabled()) {
			initializeIfNeeded();

			TopicConnection conn;
			Topic topic = (Topic) destination;

			try {
				conn = (TopicConnection) jmsTemplate.getConnectionFactory().createConnection();
				TopicSession session = conn.createTopicSession(false, TopicSession.AUTO_ACKNOWLEDGE);
				TopicSubscriber subscriber = session.createSubscriber(topic);
				subscriber.setMessageListener(new MessageListener() {

					@Override
					public void onMessage(Message message) {
						listenerToRegister.onMessage(message);
					}
				});

				//Check if this is a duplicate and remove it
				String key = topic.getTopicName() + DELIMITER + listenerToRegister.getClass().getName();
				if (subscribers.containsKey(key)) {
					unsubscribe(destination, listenerToRegister);
				}

				subscribers.put(key, subscriber);
				conn.start();

			} catch (JMSException e) {
				log.error("Exception occurred while subscribing", e);
			}
		}
	}
	
	/**
	 * @see Event#unsubscribe(Destination, EventListener)
	 */
	public void unsubscribe(Destination dest, EventListener listener) {
		if(enabled()) {
			initializeIfNeeded();

			if (dest != null) {
				Topic topic = (Topic) dest;
				try {
					String key = topic.getTopicName() + DELIMITER + listener.getClass().getName();
					if (subscribers.get(key) != null)
						subscribers.get(key).close();

					subscribers.remove(key);
				} catch (JMSException e) {
					log.error("Failed to unsubscribe from the specified destination:", e);
				}
			}
		}
	}
	
	/**
	 * @see Event#setSubscription(SubscribableEventListener)
	 */
	public void setSubscription(SubscribableEventListener listenerToRegister) {
		
		// loop over each object and each action to register
		for (Class<? extends OpenmrsObject> objectClass : listenerToRegister.subscribeToObjects()) {
			for (String action : listenerToRegister.subscribeToActions()) {
				Destination key = getDestination(objectClass, action);
				subscribe(key, listenerToRegister);
			}
		}
	}
	
	/**
	 * @see Event#unsetSubscription(SubscribableEventListener)
	 */
	public void unsetSubscription(SubscribableEventListener listenerToRegister) {
		for (Class<? extends OpenmrsObject> objectClass : listenerToRegister.subscribeToObjects()) {
			for (String action : listenerToRegister.subscribeToActions()) {
				Destination key = getDestination(objectClass, action);
				unsubscribe(key, listenerToRegister);
			}
		}
	}
	
	protected String getAction(final Destination dest) {
		if (dest instanceof Topic) {
			// look for delimiter and get string before that
			String topicName;
			try {
				topicName = ((Topic) dest).getTopicName();
			}
			catch (JMSException e) {
				// TODO fail hard here? document this in javadoc too
				return null;
			}
			int index = topicName.indexOf(DELIMITER);
			if (index < 0) {
				// uh, what? TODO: document this
				return null;
			}
			
			return topicName.substring(0, index);
			
		} else {
			// what kind of Destination is this if not a Topic??
			// TODO: document this
			return null;
		}
	}
	
	/**
	 * Closes the underlying shared connection which will close the broker too under the hood
	 */
	public void shutdown() {
		if (log.isDebugEnabled())
			log.debug("Shutting down JMS shared connection...");
		
		if (connectionFactory != null) {
			connectionFactory.destroy();
		}
	}
}
