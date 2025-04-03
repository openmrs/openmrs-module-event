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
package org.openmrs.module.event;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.OpenmrsObject;
import org.openmrs.annotation.Handler;
import org.openmrs.api.ConceptService;
import org.openmrs.event.Event.Action;
import org.openmrs.event.MockEventListener;
import org.openmrs.event.SubscribableEventListener;
import org.openmrs.test.BaseModuleContextSensitiveTest;
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

	@Before
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
		Assert.assertEquals(0, listener.getCreatedCount());
		Assert.assertEquals(0, listener.getUpdatedCount());
		Assert.assertEquals(0, listener.getDeletedCount());

		listener.setExpectedEventsCount(2);

		new EventActivator().started();

		concept.setVersion("new version");
		conceptService.saveConcept(concept);

		conceptService.saveConcept(concept);
		Concept concept3 = randomConcept();
		conceptService.saveConcept(concept3);
		conceptService.purgeConcept(concept3);

		listener.waitForEvents();

		Assert.assertEquals(1, listener.getCreatedCount());
		Assert.assertEquals(2, listener.getUpdatedCount());
		Assert.assertEquals(0, listener.getDeletedCount());
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

		Assert.assertEquals(1, listener.getCreatedCount());
		Assert.assertEquals(1, listener.getUpdatedCount());
		Assert.assertEquals(0, listener.getDeletedCount());

		new EventActivator().stopped();

		concept.setVersion("another version");
		conceptService.saveConcept(concept);

		Concept concept4 = randomConcept();
		conceptService.saveConcept(concept4);
		conceptService.purgeConcept(concept4);

		// there should have been no changes
		Assert.assertEquals(1, listener.getCreatedCount());
		Assert.assertEquals(1, listener.getUpdatedCount());
		Assert.assertEquals(0, listener.getDeletedCount());
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
