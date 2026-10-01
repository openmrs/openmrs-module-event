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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.OpenmrsObject;
import org.openmrs.annotation.Handler;
import org.openmrs.api.ConceptService;
import org.openmrs.event.Event.Action;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class EventActivatorTest extends BaseModuleContextSensitiveTest {
	
	@Autowired
	TestSubscribableEventListener listener;
	
	@Autowired
	ConceptService conceptService;
	
	@BeforeEach
	public void before() {
		// reset
		listener.setCreatedCount(0);
		listener.setDeletedCount(0);
		listener.setUpdatedCount(0);
		listener.setExpectedEventsCount(0);
	}
	
	/**
	 * @see {@link EventActivator#started()}
	 */
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@Verifies(value = "should create subscriptions for all subscribable event listeners", method = "started()")
	public void started_shouldCreateSubscriptionsForAllSubscribableEventListeners() throws Exception {
		Concept concept = conceptService.getConcept(3);
		
		conceptService.saveConcept(concept);
		Concept concept2 = randomConcept();
		conceptService.saveConcept(concept2);
		conceptService.purgeConcept(concept2);
		
		// sanity check
		listener.waitForEvents();
		Assertions.assertEquals(0, listener.getCreatedCount());
		Assertions.assertEquals(0, listener.getUpdatedCount());
		Assertions.assertEquals(0, listener.getDeletedCount());
		
		listener.setExpectedEventsCount(2);
		
		new EventActivator().started();
		
		concept.setVersion("new version");
		conceptService.saveConcept(concept);
		
		conceptService.saveConcept(concept);
		Concept concept3 = randomConcept();
		conceptService.saveConcept(concept3);
		conceptService.purgeConcept(concept3);
		
		listener.waitForEvents();
		
		Assertions.assertEquals(1, listener.getCreatedCount());
		Assertions.assertEquals(2, listener.getUpdatedCount());
		Assertions.assertEquals(0, listener.getDeletedCount());
	}
	
	/**
	 * @see {@link EventActivator#stopped()}
	 */
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@Verifies(value = "should shutdown the jms connection", method = "stopped()")
	public void stopped_shouldShutdownTheJmsConnection() throws Exception {
		listener.setExpectedEventsCount(2);
		
		new EventActivator().started();
		
		Concept concept = conceptService.getConcept(3);
		concept.setVersion("new version");
		conceptService.saveConcept(concept);
		
		Concept concept3 = randomConcept();
		conceptService.saveConcept(concept3);
		conceptService.purgeConcept(concept3);
		
		listener.waitForEvents();
		
		Assertions.assertEquals(1, listener.getCreatedCount());
		Assertions.assertEquals(1, listener.getUpdatedCount());
		Assertions.assertEquals(0, listener.getDeletedCount());
		
		new EventActivator().stopped();
		
		concept.setVersion("another version");
		conceptService.saveConcept(concept);
		
		Concept concept4 = randomConcept();
		conceptService.saveConcept(concept4);
		conceptService.purgeConcept(concept4);
		
		// there should have been no changes
		Assertions.assertEquals(1, listener.getCreatedCount());
		Assertions.assertEquals(1, listener.getUpdatedCount());
		Assertions.assertEquals(0, listener.getDeletedCount());
	}
	
	@Handler
	public static class TestSubscribableEventListener extends MockEventListener implements SubscribableEventListener {
		
		public TestSubscribableEventListener() {
			super(0);
		}
		
		/**
		 * @see org.openmrs.event.SubscribableEventListener#subscribeToObjects()
		 */
		@Override
		public List<Class<? extends OpenmrsObject>> subscribeToObjects() {
			List<Class<? extends OpenmrsObject>> clazzes = new ArrayList<>();
			clazzes.add(Concept.class);
			return clazzes;
		}
		
		/**
		 * @see org.openmrs.event.SubscribableEventListener#subscribeToActions()
		 */
		@Override
		public List<String> subscribeToActions() {
			List<String> actions = new ArrayList<>();
			actions.add(Action.CREATED.toString());
			actions.add(Action.UPDATED.toString());
			return actions;
		}
	}
	
	Concept randomConcept() {
		Concept concept = new Concept();
		ConceptName name = new ConceptName(UUID.randomUUID().toString(), Locale.ENGLISH);
		concept.addName(name);
		concept.setDatatype(conceptService.getConceptDatatypeByName("N/A"));
		concept.setConceptClass(conceptService.getConceptClassByName("Misc"));
		return concept;
	}
}
