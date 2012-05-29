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

import java.util.HashMap;
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
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.OpenmrsObject;
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
	
	/**
	 * @see Event#fireAction(String, OpenmrsObject)
	 */
	public void fireAction(String action, final OpenmrsObject object) {
		Destination key = getDestination(object.getClass(), action);
		fireEvent(key, object);
	}
	
	/**
	 * @see Event#fireEvent(Destination, OpenmrsObject)
	 */
	public void fireEvent(final Destination dest, final OpenmrsObject object) {
		
		initializeIfNeeded();
		
		jmsTemplate.send(dest, new MessageCreator() {
			
			@Override
			public Message createMessage(Session session) throws JMSException {
				if (log.isInfoEnabled())
					log.info("Sending object data " + ToStringBuilder.reflectionToString(object));
				
				MapMessage mapMessage = session.createMapMessage();
				//mapMessage.setInt("id", object.getId());
				mapMessage.setString("uuid", object.getUuid());
				mapMessage.setString("classname", object.getClass().getName());
				mapMessage.setString("action", getAction(dest));
				
				// TODO loop over properties here and add the "important" ones?
				
				return mapMessage;
			}
		});
		
		//		// fire the event off to each listener
		//		if (currentListeners != null) {
		//			// TODO make this async in a separate thread? -- EVNT-5
		//			for (EventListener listener : currentListeners) {
		//				try {
		//					if (log.isInfoEnabled())
		//						log.info("firing event for: " + object
		//								+ " because of action: " + action);
		//
		//					listener.handle(new ChangeEvent(action, object));
		//				} catch (Throwable t) {
		//					log.error("Error occurred while firing event: " + action
		//							+ " for object: " + object + " for listener: "
		//							+ listener);
		//				}
		//			}
		//		}
	}
	
	private synchronized void initializeIfNeeded() {
		if (jmsTemplate == null) {
			log.info("creating connection factory");
			ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=true");
			connectionFactory = new SingleConnectionFactory(cf); // or CachingConnectionFactory ?
			jmsTemplate = new JmsTemplate(connectionFactory);
		} else {
			log.trace("messageListener already defined");
		}
	}
	
	/**
	 * @see Event#subscribe(Class, String, EventListener)
	 */
	public void subscribe(Class<? extends OpenmrsObject> openmrsObjectClass, String action, EventListener listener) {
		if (action != null) {
			Destination dest = getDestination(openmrsObjectClass, action);
			subscribe(dest, listener);
		} else {
			for (Event.Action a : Event.Action.values()) {
				subscribe(getDestination(openmrsObjectClass, a.toString()), listener);
			}
		}
	}
	
	/**
	 * @see Event#unsubscribe(Class, org.openmrs.event.Event.Action, EventListener)
	 */
	public void unsubscribe(Class<? extends OpenmrsObject> openmrsObjectClass, Event.Action action, EventListener listener) {
		if (action != null) {
			unsubscribe(getDestination(openmrsObjectClass, action.toString()), listener);
		} else {
			for (Event.Action a : Event.Action.values()) {
				unsubscribe(getDestination(openmrsObjectClass, a.toString()), listener);
			}
		}
	}
	
	/**
	 * @see Event#getDestination(Class, String)
	 */
	public Destination getDestination(final Class<? extends OpenmrsObject> openmrsObjectClass, final String action) {
		return new Topic() {
			
			@Override
			public String getTopicName() throws JMSException {
				return action.toString() + DELIMITER + openmrsObjectClass.getName();
			}
		};
		
	}
	
	/**
	 * @see Event#subscribe(Destination, EventListener)
	 */
	public void subscribe(Destination destination, final EventListener listenerToRegister) {
		
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
			
		}
		catch (JMSException e) {
			// TODO Auto-generated catch block. Do something smarter here.
			e.printStackTrace();
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
		
		initializeIfNeeded();
		
		if (dest != null) {
			Topic topic = (Topic) dest;
			try {
				String key = topic.getTopicName() + DELIMITER + listener.getClass().getName();
				if (subscribers.get(key) != null)
					subscribers.get(key).close();
				
				subscribers.remove(key);
			}
			catch (JMSException e) {
				log.error("Failed to unsubscribe from the specified destination:", e);
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
