/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.event;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.Concept;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Unit tests for the event-deduplication logic in {@link TransactionEvent}.
 */
public class TransactionEventDedupTest {

	@Test
	public void shouldSuppressUpdateWhenCreateExistsForSameEntity() {
		Concept c = getConcept("uuid-1");
		Set<EntityEvent> incoming = new LinkedHashSet<>();
		incoming.add(new EntityEvent(c, Event.Action.CREATED));
		incoming.add(new EntityEvent(c, Event.Action.UPDATED));

		TransactionEvent tx = new TransactionCommittedEvent(this, incoming);

		Assertions.assertEquals(1, tx.getEvents().size());
		Assertions.assertTrue(tx.getEvents().contains(new EntityEvent(c, Event.Action.CREATED)));
		Assertions.assertFalse(tx.getEvents().contains(new EntityEvent(c, Event.Action.UPDATED)));
	}

	@Test
	public void shouldSuppressUpdateWhenPurgeExistsForSameEntity() {
		Concept c = getConcept("uuid-1");
		Set<EntityEvent> incoming = new LinkedHashSet<>();
		incoming.add(new EntityEvent(c, Event.Action.PURGED));
		incoming.add(new EntityEvent(c, Event.Action.UPDATED));

		TransactionEvent tx = new TransactionCommittedEvent(this, incoming);

		Assertions.assertEquals(1, tx.getEvents().size());
		Assertions.assertTrue(tx.getEvents().contains(new EntityEvent(c, Event.Action.PURGED)));
		Assertions.assertFalse(tx.getEvents().contains(new EntityEvent(c, Event.Action.UPDATED)));
	}

	@Test
	public void shouldRemovePriorUpdateWhenPurgeOccurs() {
		Concept c = getConcept("uuid-1");
		Set<EntityEvent> incoming = new LinkedHashSet<>();
		incoming.add(new EntityEvent(c, Event.Action.UPDATED));
		incoming.add(new EntityEvent(c, Event.Action.PURGED));

		TransactionEvent tx = new TransactionCommittedEvent(this, incoming);

		Assertions.assertEquals(1, tx.getEvents().size());
		Assertions.assertTrue(tx.getEvents().contains(new EntityEvent(c, Event.Action.PURGED)));
		Assertions.assertFalse(tx.getEvents().contains(new EntityEvent(c, Event.Action.UPDATED)));
	}

	@Test
	public void shouldRemoveBothCreateAndPurgeWhenEntityLivesAndDiesInSameTransaction() {
		Concept c = getConcept("uuid-1");
		Set<EntityEvent> incoming = new LinkedHashSet<>();
		incoming.add(new EntityEvent(c, Event.Action.CREATED));
		incoming.add(new EntityEvent(c, Event.Action.PURGED));

		TransactionEvent tx = new TransactionCommittedEvent(this, incoming);

		Assertions.assertTrue(tx.getEvents().isEmpty());
	}

	@Test
	public void shouldRemoveCreateUpdateAndPurgeWhenEntityIsCreatedUpdatedAndPurgedInSameTransaction() {
		Concept c = getConcept("uuid-1");
		Set<EntityEvent> incoming = new LinkedHashSet<>();
		incoming.add(new EntityEvent(c, Event.Action.CREATED));
		incoming.add(new EntityEvent(c, Event.Action.UPDATED));
		incoming.add(new EntityEvent(c, Event.Action.PURGED));

		TransactionEvent tx = new TransactionCommittedEvent(this, incoming);

		Assertions.assertTrue(tx.getEvents().isEmpty());
	}

	@Test
	public void shouldKeepEventsForDifferentEntitiesIndependent() {
		Concept a = getConcept("uuid-a");
		Concept b = getConcept("uuid-b");
		Set<EntityEvent> incoming = new LinkedHashSet<>();
		incoming.add(new EntityEvent(a, Event.Action.CREATED));
		incoming.add(new EntityEvent(a, Event.Action.UPDATED));
		incoming.add(new EntityEvent(b, Event.Action.UPDATED));

		TransactionEvent tx = new TransactionCommittedEvent(this, incoming);

		Assertions.assertEquals(2, tx.getEvents().size());
		Assertions.assertTrue(tx.getEvents().contains(new EntityEvent(a, Event.Action.CREATED)));
		Assertions.assertTrue(tx.getEvents().contains(new EntityEvent(b, Event.Action.UPDATED)));
	}

	@Test
	public void shouldDeduplicateIdenticalEntityEvents() {
		Concept c = getConcept("uuid-1");
		Set<EntityEvent> incoming = new LinkedHashSet<>();
		incoming.add(new EntityEvent(c, Event.Action.UPDATED));
		incoming.add(new EntityEvent(c, Event.Action.UPDATED));

		TransactionEvent tx = new TransactionCommittedEvent(this, incoming);

		Assertions.assertEquals(1, tx.getEvents().size());
	}

	@Test
	public void shouldCancelRetireAndUnretireInSameTransaction() {
		Concept c = getConcept("uuid-1");
		Set<EntityEvent> incoming = new LinkedHashSet<>();
		incoming.add(new EntityEvent(c, Event.Action.RETIRED));
		incoming.add(new EntityEvent(c, Event.Action.UNRETIRED));

		TransactionEvent tx = new TransactionCommittedEvent(this, incoming);

		Assertions.assertTrue(tx.getEvents().isEmpty());
	}

	@Test
	public void shouldCancelUnretireAndRetireInSameTransaction() {
		Concept c = getConcept("uuid-1");
		Set<EntityEvent> incoming = new LinkedHashSet<>();
		incoming.add(new EntityEvent(c, Event.Action.UNRETIRED));
		incoming.add(new EntityEvent(c, Event.Action.RETIRED));

		TransactionEvent tx = new TransactionCommittedEvent(this, incoming);

		Assertions.assertTrue(tx.getEvents().isEmpty());
	}

	@Test
	public void shouldCancelVoidAndUnvoidInSameTransaction() {
		Concept c = getConcept("uuid-1");
		Set<EntityEvent> incoming = new LinkedHashSet<>();
		incoming.add(new EntityEvent(c, Event.Action.VOIDED));
		incoming.add(new EntityEvent(c, Event.Action.UNVOIDED));

		TransactionEvent tx = new TransactionCommittedEvent(this, incoming);

		Assertions.assertTrue(tx.getEvents().isEmpty());
	}

	@Test
	public void shouldCancelUnvoidAndVoidInSameTransaction() {
		Concept c = getConcept("uuid-1");
		Set<EntityEvent> incoming = new LinkedHashSet<>();
		incoming.add(new EntityEvent(c, Event.Action.UNVOIDED));
		incoming.add(new EntityEvent(c, Event.Action.VOIDED));

		TransactionEvent tx = new TransactionCommittedEvent(this, incoming);

		Assertions.assertTrue(tx.getEvents().isEmpty());
	}

	@Test
	public void shouldKeepUpdateAlongsideRetire() {
		Concept c = getConcept("uuid-1");
		Set<EntityEvent> incoming = new LinkedHashSet<>();
		incoming.add(new EntityEvent(c, Event.Action.UPDATED));
		incoming.add(new EntityEvent(c, Event.Action.RETIRED));

		TransactionEvent tx = new TransactionCommittedEvent(this, incoming);

		Assertions.assertEquals(2, tx.getEvents().size());
		Assertions.assertTrue(tx.getEvents().contains(new EntityEvent(c, Event.Action.UPDATED)));
		Assertions.assertTrue(tx.getEvents().contains(new EntityEvent(c, Event.Action.RETIRED)));
	}

	@Test
	public void shouldKeepRetireWhenEntityAlsoPurgedInSameTransaction() {
		Concept c = getConcept("uuid-1");
		Set<EntityEvent> incoming = new LinkedHashSet<>();
		incoming.add(new EntityEvent(c, Event.Action.RETIRED));
		incoming.add(new EntityEvent(c, Event.Action.PURGED));

		TransactionEvent tx = new TransactionCommittedEvent(this, incoming);

		Assertions.assertTrue(tx.getEvents().contains(new EntityEvent(c, Event.Action.RETIRED)));
		Assertions.assertTrue(tx.getEvents().contains(new EntityEvent(c, Event.Action.PURGED)));
	}

	@Test
	public void shouldLeaveOnlyUpdatedWhenEntityRetiredThenUnretiredInSameTransaction() {
		// Simulates two onFlushDirty calls: retire emits UPDATED+RETIRED, unretire emits UPDATED+UNRETIRED.
		// Duplicate UPDATED collapses via Set equality; RETIRED and UNRETIRED cancel each other out.
		Concept c = getConcept("uuid-1");
		Set<EntityEvent> incoming = new LinkedHashSet<>();
		incoming.add(new EntityEvent(c, Event.Action.UPDATED));
		incoming.add(new EntityEvent(c, Event.Action.RETIRED));
		incoming.add(new EntityEvent(c, Event.Action.UPDATED));
		incoming.add(new EntityEvent(c, Event.Action.UNRETIRED));

		TransactionEvent tx = new TransactionCommittedEvent(this, incoming);

		Assertions.assertEquals(1, tx.getEvents().size());
		Assertions.assertTrue(tx.getEvents().contains(new EntityEvent(c, Event.Action.UPDATED)));
	}

	@Test
	public void shouldKeepVoidWhenEntityAlsoPurgedInSameTransaction() {
		Concept c = getConcept("uuid-1");
		Set<EntityEvent> incoming = new LinkedHashSet<>();
		incoming.add(new EntityEvent(c, Event.Action.VOIDED));
		incoming.add(new EntityEvent(c, Event.Action.PURGED));

		TransactionEvent tx = new TransactionCommittedEvent(this, incoming);

		Assertions.assertTrue(tx.getEvents().contains(new EntityEvent(c, Event.Action.VOIDED)));
		Assertions.assertTrue(tx.getEvents().contains(new EntityEvent(c, Event.Action.PURGED)));
	}

	private Concept getConcept(String uuid) {
		Concept c = new Concept();
		c.setUuid(uuid);
		return c;
	}
}