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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/**
 * Tests that Hibernate entity operations result in the correct Spring TransactionEvents
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class EventTest extends BaseEventTest {

	@Autowired
	MockTransactionEventCollector collector;

	@BeforeEach
	public void before() {
		collector.clear();
	}

	@Test
	public void shouldFireCreatedEventOnSave() {
		Concept concept = randomConcept();
		Context.getConceptService().saveConcept(concept);

		Assertions.assertTrue(collector.hasEvent(concept, Event.Action.CREATED));
		Assertions.assertFalse(collector.hasEvent(concept, Event.Action.UPDATED));
	}

	@Test
	public void shouldFireUpdatedEventOnUpdate() {
		ConceptService cs = Context.getConceptService();
		Concept concept = cs.getConcept(3);
		concept.setVersion("new version");
		cs.saveConcept(concept);

		Assertions.assertTrue(collector.hasEvent(concept, Event.Action.UPDATED));
	}

	@Test
	public void shouldFirePurgedEventOnDelete() {
		ConceptService cs = Context.getConceptService();
		Concept concept = randomConcept();
		cs.saveConcept(concept);
		collector.clear();

		cs.purgeConcept(concept);

		Assertions.assertTrue(collector.hasEvent(concept, Event.Action.PURGED));
	}

	@Test
	public void shouldFireCreateUpdateAndPurgeForFullLifecycle() {
		ConceptService cs = Context.getConceptService();

		Concept concept = randomConcept();
		cs.saveConcept(concept);
		Assertions.assertTrue(collector.hasEvent(concept, Event.Action.CREATED));

		collector.clear();
		concept.setVersion("v2");
		cs.saveConcept(concept);
		Assertions.assertTrue(collector.hasEvent(concept, Event.Action.UPDATED));

		collector.clear();
		cs.purgeConcept(concept);
		Assertions.assertTrue(collector.hasEvent(concept, Event.Action.PURGED));
	}

	@Test
	public void shouldFireRetiredEventOnRetire() {
		ConceptService cs = Context.getConceptService();
		Concept concept = cs.getConcept(5497);
		Assertions.assertFalse(concept.isRetired());

		cs.retireConcept(concept, "testing");

		Assertions.assertTrue(collector.hasEvent(concept, Event.Action.RETIRED));
	}

	private Concept randomConcept() {
		ConceptService cs = Context.getConceptService();
		Concept concept = new Concept();
		ConceptName name = new ConceptName(UUID.randomUUID().toString(), Locale.ENGLISH);
		concept.addName(name);
		concept.setDatatype(cs.getConceptDatatypeByName("N/A"));
		concept.setConceptClass(cs.getConceptClassByName("Misc"));
		return concept;
	}
}