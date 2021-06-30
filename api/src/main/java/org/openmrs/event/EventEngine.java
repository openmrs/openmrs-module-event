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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

/**
 * Used by {@link Event}.
 */
public class EventEngine {

	protected final static String DELIMITER = ":";

	protected static Log log = LogFactory.getLog(Event.class);

	protected JmsTemplate jmsTemplate = null;

	protected Map<String, TopicSubscriber> subscribers = new HashMap<String, TopicSubscriber>();

	protected SingleConnectionFactory connectionFactory;

	protected List<Class<?>> eventClasses = new ArrayList<Class<?>>();

	/**
	 * @see Event#fireAction(String, OpenmrsObject)
	 */
	public void fireAction(String action, final Object object) {
		Destination key = getDestination(object.getClass(), action);
		fireEvent(key, object);
	}

	/**
	 * @see Event#fireEvent(Destination, OpenmrsObject)
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

	/**
	 * @param dest
	 * @param eventMessage
	 */
	private void doFireEvent(final Destination dest, final EventMessage eventMessage) {

		initializeIfNeeded();

		jmsTemplate.send(dest, new MessageCreator() {

			@Override
			public Message createMessage(Session session) throws JMSException {
				if (log.isInfoEnabled())
					log.info("Sending data " + eventMessage);

				MapMessage mapMessage = session.createMapMessage();
				if (eventMessage != null) {
					for (Map.Entry<String, Serializable> entry : eventMessage.entrySet()) {
						mapMessage.setObject(entry.getKey(), entry.getValue());
					}
				}

				return mapMessage;
			}
		});
	}

	private boolean disable() {
		return new File(OpenmrsUtil.getApplicationDataDirectory() + File.separator + "activemq-data", "disabled").exists();
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
            connectionFactory = new SingleConnectionFactory(cf); // or CachingConnectionFactory ?
            jmsTemplate = new JmsTemplate(connectionFactory);
        } else {
            log.trace("messageListener already defined");
        }
    }
    
    private String getExternalUrl() {
    	try {
    		return Context.getRegisteredComponent("adminService", AdministrationService.class)
					.getGlobalProperty("activeMQ.externalUrl");
    	}
    	catch (NullPointerException ex) {
    		log.error("AdministrationService not yet initialized to get the activeMQ.externalUrl setting" , ex);
    	}
    	return null;
    }

	/**
	 * @see Event#subscribe(Class, String, EventListener)
	 */
	public void subscribe(Class<?> clazz, String action, EventListener listener) {
		if (action != null) {
			subscribeToClass(clazz, action, listener);
		} else {
			for (Event.Action a : Event.Action.values()) {
				subscribeToClass(clazz, a.toString(), listener);
			}
		}
		eventClasses.removeAll(eventClasses);
	}

	/**
	 * Adds subscriptions to the topics that match the specified action and class including
	 * subclasses
	 *
	 * @param clazz the class to match
	 * @param action the action to match
	 * @param listener the Listener subscribing to the topic
	 */
	private void subscribeToClass(Class<?> clazz, String action, EventListener listener) {
		try {

			if(eventClasses.isEmpty()){
				eventClasses = EventClassScanner.getInstance().getClasses(clazz);
			}

			for (Class c : eventClasses) {
				Destination dest = getDestination(c, action);
				subscribe(dest, listener);
			}
		}
		catch (IOException e) {
			throw new APIException(e);
		}
		catch (ClassNotFoundException e) {
			new APIException(e);
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
	 * @see Event#unsubscribe(Class, org.openmrs.event.Event.Action, EventListener)
	 */
	public void unsubscribe(Class<?> clazz, Event.Action action, EventListener listener) {
		if (action != null) {
			unsubscribeFromClass(clazz, action.toString(), listener);
		} else {
			for (Event.Action a : Event.Action.values()) {
				unsubscribeFromClass(clazz, a.toString(), listener);
			}
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
	private void unsubscribeFromClass(Class<?> clazz, String action, EventListener listener) {
		try {
			List<Class<?>> classes = EventClassScanner.getInstance().getClasses(clazz);
			for (Class c : classes) {
				Destination dest = getDestination(c, action);
				unsubscribe(dest, listener);
			}
		}
		catch (IOException e) {
			throw new APIException(e);
		}
		catch (ClassNotFoundException e) {
			new APIException(e);
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
		return getDestination(action.toString() + DELIMITER + clazz.getName());
	}
	
	/**
	 * @see Event#getDestination(String)
	 */
	public Destination getDestination(final String topicName) {
		return new Topic() {
			
			@Override
			public String getTopicName() throws JMSException {
				return topicName;
			}
		};
	}
	
	/**
	 * @see Event#subscribe(Destination, EventListener)
	 */
	public void subscribe(Destination destination, final EventListener listenerToRegister) {
		if(!disable()) {
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
				// TODO Auto-generated catch block. Do something smarter here.
				e.printStackTrace();
			}
		}
		//		List<EventListener> currentListeners = listeners.get(key);
		//
		//		if (currentListeners == null) {
		//			currentListeners = new ArrayList<EventListener>();
		//			currentListeners.add(listenerToRegister);
		//			listeners.put(key, currentListeners);
		//			if (log.isInfoEnabled())
		//				log.info("subscribed: " + listenerToRegister + " to key: "
		//						+ key);
		//
		//		} else {
		//			// prevent duplicates because of weird spring loading
		//			String listernToRegisterName = listenerToRegister.getClass()
		//					.getName();
		//			Iterator<EventListener> iterator = currentListeners.iterator();
		//			while (iterator.hasNext()) {
		//				EventListener lstnr = iterator.next();
		//				if (lstnr.getClass().getName().equals(listernToRegisterName))
		//					iterator.remove();
		//			}
		//
		//			if (log.isInfoEnabled())
		//				log.info("subscribing: " + listenerToRegister + " to key: "
		//						+ key);
		//
		//			currentListeners.add(listenerToRegister);
		//		}
		
	}
	
	/**
	 * @see Event#unsubscribe(Destination, EventListener)
	 */
	public void unsubscribe(Destination dest, EventListener listener) {
		if(!disable()) {
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
