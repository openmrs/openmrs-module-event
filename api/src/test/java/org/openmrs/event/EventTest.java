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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNumeric;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.event.Event.Action;
import org.openmrs.test.Verifies;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.Destination;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class EventTest extends BaseModuleContextSensitiveTest {
	
	/**
	 * @see Event#subscribe(Class, String, EventListener)
	 */
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@Verifies(value = "should subscribe only to the specified action", method = "subscribe(Class<OpenmrsObject>,String,EventListener)")
	public void subscribe_shouldSubscribeOnlyToTheSpecifiedAction() throws Exception {
		ConceptService cs = Context.getConceptService();
		MockEventListener listener = new MockEventListener(3); //let's wait for 3 messages
		Event.subscribe(Concept.class, Action.UPDATED.toString(), listener);

		Concept concept = new Concept();
		ConceptName name = new ConceptName(UUID.randomUUID().toString(), Locale.ENGLISH);
		concept.addName(name);
		concept.setDatatype(cs.getConceptDatatypeByName("N/A"));
		concept.setConceptClass(cs.getConceptClassByName("Misc"));
		cs.saveConcept(concept);

		concept.setVersion("new random version");
		cs.saveConcept(concept);

		cs.purgeConcept(concept);

		listener.waitForEvents();

		Assertions.assertEquals(0, listener.getCreatedCount());
		Assertions.assertEquals(1, listener.getUpdatedCount());
		Assertions.assertEquals(0, listener.getDeletedCount());
	}

    /**
     * @see Event#subscribe(Class, Collection, EventListener)
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Verifies(value = "should subscribe only to the specified actions", method = "subscribe(Class<OpenmrsObject>,Collection,EventListener)")
    public void subscribe_shouldSubscribeOnlyToTheSpecifiedActions() throws Exception {
        ConceptService cs = Context.getConceptService();
        MockEventListener listener = new MockEventListener(3); //let's wait for 3 messages
        Event.subscribe(Concept.class, Arrays.asList(Action.CREATED.toString(), Action.UPDATED.toString()), listener);

        Concept concept = new Concept();
        ConceptName name = new ConceptName(UUID.randomUUID().toString(), Locale.ENGLISH);
        concept.addName(name);
        concept.setDatatype(cs.getConceptDatatypeByName("N/A"));
        concept.setConceptClass(cs.getConceptClassByName("Misc"));
        cs.saveConcept(concept);

        concept.setVersion("new random version");
        cs.saveConcept(concept);

        cs.purgeConcept(concept);

        listener.waitForEvents();

        Assertions.assertEquals(1, listener.getCreatedCount());
        Assertions.assertEquals(1, listener.getUpdatedCount());
        Assertions.assertEquals(0, listener.getDeletedCount());
    }
	
	/**
	 * @see Event#subscribe(Class,String,EventListener)
	 */
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@Verifies(value = "should subscribe to every action if action is null", method = "subscribe(Class<OpenmrsObject>,String,EventListener)")
	public void subscribe_shouldSubscribeToEveryActionIfActionIsNullForTheEntireClassHierarchy() throws Exception {
		ConceptService cs = Context.getConceptService();
		MockEventListener listener = new MockEventListener(6);
		Event.subscribe(Concept.class, (String) null, listener);
		
		Concept concept = new Concept();
		concept.setDatatype(cs.getConceptDatatypeByName("N/A"));
		concept.setConceptClass(cs.getConceptClassByName("Misc"));
		ConceptName name = new ConceptName(UUID.randomUUID().toString(), Locale.ENGLISH);
		concept.addName(name);
		cs.saveConcept(concept);
		
		concept.setVersion("new random version");
		cs.saveConcept(concept);
		
		cs.purgeConcept(concept);

		//Should work for subclasses
		ConceptNumeric cn = new ConceptNumeric();
		cn.setDatatype(cs.getConceptDatatypeByName("Numeric"));
		cn.setConceptClass(cs.getConceptClassByName("Question"));
		ConceptName cName = new ConceptName(UUID.randomUUID().toString(), Locale.ENGLISH);
		cn.addName(cName);
		cs.saveConcept(cn);

		cn.setVersion("new random version");
		cs.saveConcept(cn);
		cs.purgeConcept(cn);

		listener.waitForEvents();

		Assertions.assertEquals(2, listener.getCreatedCount());
		Assertions.assertEquals(2, listener.getUpdatedCount());
		Assertions.assertEquals(2, listener.getDeletedCount());
	}
	
	/**
	 * @see {@link Event#unsubscribe(Destination,EventListener)}
	 */
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@Verifies(value = "should unsubscribe from the specified destination", method = "unsubscribe(Destination,EventListener)")
	public void unsubscribe_shouldUnsubscribeFromTheSpecifiedDestination() throws Exception {
		ConceptService cs = Context.getConceptService();
		MockEventListener listener = new MockEventListener(1);
		String action = Action.CREATED.toString();
		Event.subscribe(Concept.class, action, listener);
		
		Concept concept1 = new Concept();
		ConceptName name1 = new ConceptName(UUID.randomUUID().toString(), Locale.ENGLISH);
		concept1.addName(name1);
		concept1.setDatatype(cs.getConceptDatatypeByName("N/A"));
		concept1.setConceptClass(cs.getConceptClassByName("Misc"));
		cs.saveConcept(concept1);
		
		listener.waitForEvents();
		
		Assertions.assertEquals(1, listener.getCreatedCount());
		
		Event.unsubscribe(Event.getDestination(Concept.class, action), listener);
		
		Concept concept2 = new Concept();
		ConceptName name2 = new ConceptName(UUID.randomUUID().toString(), Locale.ENGLISH);
		concept2.addName(name2);
		concept2.setDatatype(cs.getConceptDatatypeByName("N/A"));
		concept2.setConceptClass(cs.getConceptClassByName("Misc"));
		cs.saveConcept(concept2);
		
		Thread.sleep(100);
		
		Assertions.assertEquals(1, listener.getCreatedCount());
	}
	
	/**
	 * @see {@link Event#unsubscribe(Class,Action,EventListener)}
	 */
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@Verifies(value = "should unsubscribe for every action if action is null", method = "unsubscribe(Class<OpenmrsObject>,Action,EventListener)")
	public void unsubscribe_shouldUnsubscribeForEveryActionIfActionIsNullForTheEntireClassHierarchy() throws Exception {
		ConceptService cs = Context.getConceptService();
		MockEventListener listener = new MockEventListener(6);
		Event.subscribe(Concept.class, (String) null, listener);
		
		Event.unsubscribe(Concept.class, (Event.Action) null, listener);
		Concept concept2 = new Concept();
		ConceptName name2 = new ConceptName(UUID.randomUUID().toString(), Locale.ENGLISH);
		concept2.addName(name2);
		concept2.setDatatype(cs.getConceptDatatypeByName("N/A"));
		concept2.setConceptClass(cs.getConceptClassByName("Misc"));
		cs.saveConcept(concept2);
		
		concept2.setVersion("New version");
		cs.saveConcept(concept2);
		cs.purgeConcept(concept2);
		
		//Should work for subclasses
		ConceptNumeric cn2 = new ConceptNumeric();
		cn2.setDatatype(cs.getConceptDatatype(1));
		ConceptName cName2 = new ConceptName(UUID.randomUUID().toString(), Locale.ENGLISH);
		cn2.addName(cName2);
		cn2.setDatatype(cs.getConceptDatatypeByName("N/A"));
		cn2.setConceptClass(cs.getConceptClassByName("Misc"));
		cs.saveConcept(cn2);
		
		cn2.setVersion("new random version");
		cs.saveConcept(cn2);
		cs.purgeConcept(cn2);
		
		listener.waitForEvents();
		
		Assertions.assertEquals(0, listener.getCreatedCount());
		Assertions.assertEquals(0, listener.getUpdatedCount());
		Assertions.assertEquals(0, listener.getDeletedCount());
	}
	
	/**
	 * @see Event#unsubscribe(Class,Action,EventListener)
	 */
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@Verifies(value = "should unsubscribe only for the specified action", method = "unsubscribe(Class<OpenmrsObject>,Action,EventListener)")
	public void unsubscribe_shouldUnsubscribeOnlyForTheSpecifiedAction() throws Exception {
		ConceptService cs = Context.getConceptService();
		MockEventListener listener = new MockEventListener(1);
		Event.subscribe(Concept.class, (String) null, listener);
		
		Concept concept1 = new Concept();
		ConceptName name1 = new ConceptName(UUID.randomUUID().toString(), Locale.ENGLISH);
		concept1.addName(name1);
		concept1.setDatatype(cs.getConceptDatatypeByName("N/A"));
		concept1.setConceptClass(cs.getConceptClassByName("Misc"));
		cs.saveConcept(concept1);
		
		listener.waitForEvents();
		
		Assertions.assertEquals(1, listener.getCreatedCount());
		
		Event.unsubscribe(Concept.class, Action.CREATED, listener);
		Concept concept2 = new Concept();
		ConceptName name2 = new ConceptName(UUID.randomUUID().toString(), Locale.ENGLISH);
		concept2.addName(name2);
		concept2.setDatatype(cs.getConceptDatatypeByName("N/A"));
		concept2.setConceptClass(cs.getConceptClassByName("Misc"));
		cs.saveConcept(concept2);
		cs.purgeConcept(concept1);
		
		Thread.sleep(100);
		
		Assertions.assertEquals(1, listener.getCreatedCount());
		Assertions.assertEquals(1, listener.getDeletedCount());
	}

    /**
     * @see Event#unsubscribe(Class,Collection,EventListener)
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Verifies(value = "should unsubscribe only for the specified actions", method = "unsubscribe(Class<OpenmrsObject>,Collection,EventListener)")
    public void unsubscribe_shouldUnsubscribeOnlyForTheSpecifiedActions() throws Exception {
        ConceptService cs = Context.getConceptService();
        MockEventListener listener = new MockEventListener(1);
        Event.subscribe(Concept.class, (String) null, listener);

        Concept concept1 = new Concept();
        ConceptName name1 = new ConceptName(UUID.randomUUID().toString(), Locale.ENGLISH);
        concept1.addName(name1);
        concept1.setDatatype(cs.getConceptDatatypeByName("N/A"));
        concept1.setConceptClass(cs.getConceptClassByName("Misc"));
        cs.saveConcept(concept1);

        listener.waitForEvents();

        Assertions.assertEquals(1, listener.getCreatedCount());

        Event.unsubscribe(Concept.class, Collections.singletonList(Action.CREATED), listener);
        Concept concept2 = new Concept();
        ConceptName name2 = new ConceptName(UUID.randomUUID().toString(), Locale.ENGLISH);
        concept2.addName(name2);
        concept2.setDatatype(cs.getConceptDatatypeByName("N/A"));
        concept2.setConceptClass(cs.getConceptClassByName("Misc"));
        cs.saveConcept(concept2);
        cs.purgeConcept(concept1);

        Thread.sleep(100);

        Assertions.assertEquals(1, listener.getCreatedCount());
        Assertions.assertEquals(1, listener.getDeletedCount());
    }
	
	/**
	 * @see {@link Event#unsubscribe(Destination,EventListener)}
	 */
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@Verifies(value = "maintain subscriptions to the same topic for other listeners", method = "unsubscribe(Destination,EventListener)")
	public void unsubscribe_shouldMaintainSubscriptionsToTheSameTopicForOtherListeners() throws Exception {
		ConceptService cs = Context.getConceptService();
		MockEventListener listener1 = new MockEventListener(1);
		MockEventListener listener2 = new AnotherTestEventListener(1);
		Event.subscribe(Concept.class, Action.UPDATED.toString(), listener1);
		Event.subscribe(Concept.class, Action.UPDATED.toString(), listener2);
		
		Concept concept = cs.getConcept(3);
		concept.setVersion("new random version");
		cs.saveConcept(concept);
		
		listener1.waitForEvents();
		listener2.waitForEvents();
		
		Assertions.assertEquals(1, listener1.getUpdatedCount());
		Assertions.assertEquals(1, listener2.getUpdatedCount());
		
		listener1.setExpectedEventsCount(0);
		listener2.setExpectedEventsCount(1);
		
		Event.unsubscribe(Concept.class, Action.UPDATED, listener1);
		concept.setVersion("another random version");
		cs.saveConcept(concept);
		
		listener1.waitForEvents();
		listener2.waitForEvents();
		
		Assertions.assertEquals(1, listener1.getUpdatedCount());
		Assertions.assertEquals(2, listener2.getUpdatedCount());
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
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@Verifies(value = "should remove given subscriptions", method = "unsetSubscription(SubscribableEventListener)")
	public void unsetSubscription_shouldRemoveGivenSubscriptions() throws Exception {
		
		Event event = new Event();
		
		TestSubscribableEventListener listener = new TestSubscribableEventListener(3);
		event.setSubscription(listener);
		
		ConceptService cs = Context.getConceptService();
		Concept concept1 = new Concept();
		ConceptName name1 = new ConceptName(UUID.randomUUID().toString(), Locale.ENGLISH);
		concept1.addName(name1);
		concept1.setDatatype(cs.getConceptDatatypeByName("N/A"));
		concept1.setConceptClass(cs.getConceptClassByName("Misc"));
		cs.saveConcept(concept1);
		
		concept1.setVersion("new random version");
		cs.saveConcept(concept1);
		cs.purgeConcept(concept1);
		
		listener.waitForEvents();
		
		Assertions.assertEquals(1, listener.getCreatedCount());
		Assertions.assertEquals(1, listener.getUpdatedCount());
		Assertions.assertEquals(1, listener.getDeletedCount());
		
		event.unsetSubscription(listener);
		Concept concept2 = new Concept();
		ConceptName name2 = new ConceptName(UUID.randomUUID().toString(), Locale.ENGLISH);
		concept2.addName(name2);
		concept2.setDatatype(cs.getConceptDatatypeByName("N/A"));
		concept2.setConceptClass(cs.getConceptClassByName("Misc"));
		cs.saveConcept(concept2);
		cs.saveConcept(concept2);
		cs.purgeConcept(concept2);
		
		Thread.sleep(100);
		
		Assertions.assertEquals(1, listener.getCreatedCount());
		Assertions.assertEquals(1, listener.getUpdatedCount());
		Assertions.assertEquals(1, listener.getDeletedCount());
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
	 * @see {@link Event#subscribe(Class,String,EventListener)}
	 */
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@Verifies(value = "should not subscribe duplicate event listeners ", method = "subscribe(Class<OpenmrsObject>,String,EventListener)")
	public void subscribe_shouldNotSubscribeDuplicateEventListeners() throws Exception {
		ConceptService cs = Context.getConceptService();
		MockEventListener listener = new MockEventListener(1);
		Event.subscribe(Concept.class, Action.CREATED.toString(), listener);
		Event.subscribe(Concept.class, Action.CREATED.toString(), listener);
		
		Concept concept = new Concept();
		ConceptName name = new ConceptName(UUID.randomUUID().toString(), Locale.ENGLISH);
		concept.addName(name);
		concept.setDatatype(cs.getConceptDatatypeByName("N/A"));
		concept.setConceptClass(cs.getConceptClassByName("Misc"));
		cs.saveConcept(concept);
		
		listener.waitForEvents();
		
		Assertions.assertEquals(1, listener.getCreatedCount());
	}
	
	/**
	 * @see {@link Event#fireEvent(String, EventMessage)}
	 */
	@Test
	public void fireEvent_shouldFireAnEventForTheActionAndClassWithTheSpecifiedMessage() throws Exception {
		EventMessageListener listener = new EventMessageListener(1);
		final String dest = "org.openmrs.test";
		Event.subscribe(dest, listener);
		
		final String city = "indianapolis";
		final String state = "indiana";
		EventMessage eventMessage = new EventMessage();
		eventMessage.put("city", city);
		eventMessage.put("state", state);
		
		Event.fireEvent(dest, eventMessage);
		
		listener.waitForEvents();
		
		Assertions.assertEquals(city, listener.getCity());
		Assertions.assertEquals(state, listener.getState());
	}
}
