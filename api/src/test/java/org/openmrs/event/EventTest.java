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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.jms.Destination;

import junit.framework.Assert;

import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.event.Event.Action;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;

public class EventTest extends BaseModuleContextSensitiveTest {
	
	/**
	 * @see {@link Event#subscribe(Class<OpenmrsObject>,String,EventListener)}
	 */
	@Test
	@Verifies(value = "should subscribe only to the specified action", method = "subscribe(Class<OpenmrsObject>,String,EventListener)")
	public void subscribe_shouldSubscribeOnlyToTheSpecifiedAction() throws Exception {
		ConceptService cs = Context.getConceptService();
		MockEventListener listener = new MockEventListener(3); //let's wait for 3 messages
		Event.subscribe(Concept.class, Action.UPDATED.toString(), listener);
		
		Concept concept = new Concept();
		ConceptName name = new ConceptName("Name", Locale.ENGLISH);
		concept.addName(name);
		cs.saveConcept(concept);
		
		cs.saveConcept(concept);
		
		cs.purgeConcept(concept);
		
		listener.waitForEvents();
		
		Assert.assertEquals(0, listener.getCreatedCount());
		Assert.assertEquals(1, listener.getUpdatedCount());
		Assert.assertEquals(0, listener.getDeletedCount());
	}
	
	/**
	 * @see {@link Event#subscribe(Class<OpenmrsObject>,String,EventListener)}
	 */
	@Test
	@Verifies(value = "should subscribe to every action if action is null", method = "subscribe(Class<OpenmrsObject>,String,EventListener)")
	public void subscribe_shouldSubscribeToEveryActionIfActionIsNull() throws Exception {
		ConceptService cs = Context.getConceptService();
		MockEventListener listener = new MockEventListener(3);
		Event.subscribe(Concept.class, null, listener);
		
		Concept concept = new Concept();
		ConceptName name = new ConceptName("Name", Locale.ENGLISH);
		concept.addName(name);
		cs.saveConcept(concept);
		
		cs.saveConcept(concept);
		
		cs.purgeConcept(concept);
		
		listener.waitForEvents();
		
		Assert.assertEquals(1, listener.getCreatedCount());
		Assert.assertEquals(1, listener.getUpdatedCount());
		Assert.assertEquals(1, listener.getDeletedCount());
	}
	
	/**
	 * @see {@link Event#unsubscribe(Destination,EventListener)}
	 */
	@Test
	@Verifies(value = "should unsubscribe from the specified destination", method = "unsubscribe(Destination,EventListener)")
	public void unsubscribe_shouldUnsubscribeFromTheSpecifiedDestination() throws Exception {
		ConceptService cs = Context.getConceptService();
		MockEventListener listener = new MockEventListener(1);
		String action = Action.CREATED.toString();
		Event.subscribe(Concept.class, action, listener);
		
		Concept concept1 = new Concept();
		ConceptName name1 = new ConceptName("Name1", Locale.ENGLISH);
		concept1.addName(name1);
		cs.saveConcept(concept1);
		
		listener.waitForEvents();
		
		Assert.assertEquals(1, listener.getCreatedCount());
		
		Event.unsubscribe(Event.getDestination(Concept.class, action), listener);
		
		Concept concept2 = new Concept();
		ConceptName name2 = new ConceptName("Name2", Locale.ENGLISH);
		concept2.addName(name2);
		cs.saveConcept(concept2);
		
		Thread.sleep(100);
		
		Assert.assertEquals(1, listener.getCreatedCount());
	}
	
	/**
	 * @see {@link Event#unsubscribe(Class<+QOpenmrsObject;>,Action,EventListener)}
	 */
	@Test
	@Verifies(value = "should unsubscribe for every action if action is null", method = "unsubscribe(Class<+QOpenmrsObject;>,Action,EventListener)")
	public void unsubscribe_shouldUnsubscribeForEveryActionIfActionIsNull() throws Exception {
		ConceptService cs = Context.getConceptService();
		MockEventListener listener = new MockEventListener(1);
		Event.subscribe(Concept.class, null, listener);
		
		Concept concept1 = new Concept();
		ConceptName name1 = new ConceptName("Name1", Locale.ENGLISH);
		concept1.addName(name1);
		cs.saveConcept(concept1);
		
		listener.waitForEvents();
		
		Assert.assertEquals(1, listener.getCreatedCount());
		
		Event.unsubscribe(Concept.class, null, listener);
		Concept concept2 = new Concept();
		ConceptName name2 = new ConceptName("Name2", Locale.ENGLISH);
		concept2.addName(name2);
		cs.saveConcept(concept2);
		cs.purgeConcept(concept1);
		
		Thread.sleep(100);
		
		Assert.assertEquals(1, listener.getCreatedCount());
		Assert.assertEquals(0, listener.getDeletedCount());
	}
	
	/**
	 * @see {@link Event#unsubscribe(Class<OpenmrsObject>,Action,EventListener)}
	 */
	@Test
	@Verifies(value = "should unsubscribe only for the specified action", method = "unsubscribe(Class<OpenmrsObject>,Action,EventListener)")
	public void unsubscribe_shouldUnsubscribeOnlyForTheSpecifiedAction() throws Exception {
		ConceptService cs = Context.getConceptService();
		MockEventListener listener = new MockEventListener(1);
		Event.subscribe(Concept.class, null, listener);
		
		Concept concept1 = new Concept();
		ConceptName name1 = new ConceptName("Name1", Locale.ENGLISH);
		concept1.addName(name1);
		cs.saveConcept(concept1);
		
		listener.waitForEvents();
		
		Assert.assertEquals(1, listener.getCreatedCount());
		
		Event.unsubscribe(Concept.class, Action.CREATED, listener);
		Concept concept2 = new Concept();
		ConceptName name2 = new ConceptName("Name2", Locale.ENGLISH);
		concept2.addName(name2);
		cs.saveConcept(concept2);
		cs.purgeConcept(concept1);
		
		Thread.sleep(100);
		
		Assert.assertEquals(1, listener.getCreatedCount());
		Assert.assertEquals(1, listener.getDeletedCount());
	}
	
	/**
	 * @see {@link Event#unsubscribe(Destination,EventListener)}
	 */
	@Test
	@Verifies(value = "maintain subscriptions to the same topic for other listeners", method = "unsubscribe(Destination,EventListener)")
	public void unsubscribe_shouldMaintainSubscriptionsToTheSameTopicForOtherListeners() throws Exception {
		ConceptService cs = Context.getConceptService();
		MockEventListener listener1 = new MockEventListener(1);
		MockEventListener listener2 = new AnotherTestEventListener(1);
		Event.subscribe(Concept.class, Action.UPDATED.toString(), listener1);
		Event.subscribe(Concept.class, Action.UPDATED.toString(), listener2);
		
		Concept concept = cs.getConcept(3);
		cs.saveConcept(concept);
		
		listener1.waitForEvents();
		listener2.waitForEvents();
		
		Assert.assertEquals(1, listener1.getUpdatedCount());
		Assert.assertEquals(1, listener2.getUpdatedCount());
		
		listener1.setExpectedEventsCount(0);
		listener2.setExpectedEventsCount(1);
		
		Event.unsubscribe(Concept.class, Action.UPDATED, listener1);
		cs.saveConcept(concept);
		
		listener1.waitForEvents();
		listener2.waitForEvents();
		
		Assert.assertEquals(1, listener1.getUpdatedCount());
		Assert.assertEquals(2, listener2.getUpdatedCount());
	}
	
	public class AnotherTestEventListener extends MockEventListener {

		/**
         * @param expectedEventsCount
         */
        public AnotherTestEventListener(int expectedEventsCount) {
	        super(expectedEventsCount);
        }
		
	}
	
	/**
	 * @see {@link Event#unsetSubscription(SubscribableEventListener)}
	 */
	@Test
	@Verifies(value = "should remove given subscriptions", method = "unsetSubscription(SubscribableEventListener)")
	public void unsetSubscription_shouldRemoveGivenSubscriptions() throws Exception {
		
		Event event = new Event();
		
		TestSubscribableEventListener listener = new TestSubscribableEventListener(3);
		event.setSubscription(listener);
		
		ConceptService cs = Context.getConceptService();
		Concept concept1 = new Concept();
		ConceptName name1 = new ConceptName("Name1", Locale.ENGLISH);
		concept1.addName(name1);
		cs.saveConcept(concept1);
		cs.saveConcept(concept1);
		cs.purgeConcept(concept1);
		
		listener.waitForEvents();
		
		Assert.assertEquals(1, listener.getCreatedCount());
		Assert.assertEquals(1, listener.getUpdatedCount());
		Assert.assertEquals(1, listener.getDeletedCount());
		
		event.unsetSubscription(listener);
		Concept concept2 = new Concept();
		ConceptName name2 = new ConceptName("Name2", Locale.ENGLISH);
		concept2.addName(name2);
		cs.saveConcept(concept2);
		cs.saveConcept(concept2);
		cs.purgeConcept(concept2);
		
		Thread.sleep(100);
		
		Assert.assertEquals(1, listener.getCreatedCount());
		Assert.assertEquals(1, listener.getUpdatedCount());
		Assert.assertEquals(1, listener.getDeletedCount());
	}
	
	private class TestSubscribableEventListener extends MockEventListener implements SubscribableEventListener {
		
		/**
         * @param expectedEventsCount
         */
        public TestSubscribableEventListener(int expectedEventsCount) {
	        super(expectedEventsCount);
        }

		/**
		 * @return a list of classes that this can handle
		 */
		public List<Class<? extends OpenmrsObject>> subscribeToObjects() {
			List<Class<? extends OpenmrsObject>> objectList = new ArrayList<Class<? extends OpenmrsObject>>();
			objectList.add(Concept.class);
			return objectList;
		}
		
		/**
		 * @return a list of Actions this listener can deal with
		 */
		public List<String> subscribeToActions() {
			List<String> actionList = new ArrayList<String>();
			actionList.add(Action.CREATED.toString());
			actionList.add(Action.UPDATED.toString());
			actionList.add(Action.PURGED.toString());
			return actionList;
		}
	}
	
	/**
	 * @see {@link Event#subscribe(Class<OpenmrsObject>,String,EventListener)}
	 */
	@Test
	@Verifies(value = "should not subscribe duplicate event listeners ", method = "subscribe(Class<OpenmrsObject>,String,EventListener)")
	public void subscribe_shouldNotSubscribeDuplicateEventListeners() throws Exception {
		ConceptService cs = Context.getConceptService();
		MockEventListener listener = new MockEventListener(1);
		Event.subscribe(Concept.class, Action.CREATED.toString(), listener);
		Event.subscribe(Concept.class, Action.CREATED.toString(), listener);
		
		Concept concept = new Concept();
		ConceptName name = new ConceptName("Name", Locale.ENGLISH);
		concept.addName(name);
		cs.saveConcept(concept);
		
		listener.waitForEvents();
		
		Assert.assertEquals(1, listener.getCreatedCount());
	}
}
