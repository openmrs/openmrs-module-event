/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNumeric;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.MapMessage;
import javax.jms.Message;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Smoke tests for the deprecated {@link Event#subscribe} / {@link EventListener} bridge.
 * Verifies that legacy listeners still receive events for entities saved through the API,
 * including events for entity subclasses.
 */
@SuppressWarnings("deprecation")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class LegacyEventBridgeTest extends BaseEventTest {

	private final CopyOnWriteArrayList<EventListener> registered = new CopyOnWriteArrayList<>();

	@AfterEach
	public void tearDown() {
		for (EventListener l : registered) {
			Event.unsubscribe(Concept.class, (Event.Action) null, l);
		}
		registered.clear();
	}

	@Test
	public void shouldDeliverCreatedEventToLegacyListener() throws Exception {
		ConceptService cs = Context.getConceptService();
		CountingListener listener = subscribe(Concept.class, Event.Action.CREATED.name());

		cs.saveConcept(randomConcept());

		listener.awaitAtLeast(1);
		Assertions.assertTrue(listener.lastUuid.get() != null);
		Assertions.assertEquals(1, listener.received.get());
	}

	@Test
	public void shouldRespectClassHierarchyOnDispatch() throws Exception {
		ConceptService cs = Context.getConceptService();
		CountingListener listener = subscribe(Concept.class, Event.Action.CREATED.name());

		ConceptNumeric cn = new ConceptNumeric();
		ConceptName name = new ConceptName(UUID.randomUUID().toString(), Locale.ENGLISH);
		cn.addName(name);
		cn.setDatatype(cs.getConceptDatatypeByName("Numeric"));
		cn.setConceptClass(cs.getConceptClassByName("Question"));
		cs.saveConcept(cn);

		listener.awaitAtLeast(1);
		Assertions.assertEquals(1, listener.received.get(),
			"listener registered against Concept.class should also receive ConceptNumeric events");
	}

	@Test
	public void shouldNotDeliverAfterUnsubscribe() throws Exception {
		ConceptService cs = Context.getConceptService();
		CountingListener listener = subscribe(Concept.class, Event.Action.CREATED.name());

		Event.unsubscribe(Concept.class, Event.Action.CREATED, listener);
		registered.remove(listener);

		cs.saveConcept(randomConcept());

		// give the daemon thread a chance to run
		Thread.sleep(200);
		Assertions.assertEquals(0, listener.received.get());
	}

	@Test
	public void shouldDispatchToInterfaceSubscribers() throws Exception {
		ConceptService cs = Context.getConceptService();
		CountingListener listener = new CountingListener();
		Event.subscribe(OpenmrsObject.class, Event.Action.CREATED.name(), listener);
		try {
			cs.saveConcept(randomConcept());

			listener.awaitAtLeast(1);
			Assertions.assertTrue(listener.received.get() >= 1,
				"listener registered against OpenmrsObject interface should receive Concept events");
		} finally {
			Event.unsubscribe(OpenmrsObject.class, Event.Action.CREATED, listener);
		}
	}

	@Test
	public void shouldAcceptCustomActionOnFireAndUnsubscribe() {
		CountingListener listener = new CountingListener();
		String customAction = "CUSTOM_NOTIFIED";
		Event.subscribe(Concept.class, customAction, listener);
		try {
			// fire directly rather than via Hibernate, since custom actions aren't emitted by the interceptor
			Concept concept = randomConcept();
			Event.fireAction(customAction, concept);
			Assertions.assertEquals(1, listener.received.get());
		} finally {
			// String overload must not throw IllegalArgumentException for non-enum action names
			Event.unsubscribe(Concept.class, customAction, listener);
		}
	}

	@Test
	public void shouldReturnDefaultsForMissingMapMessageKeys() {
		CountingListener listener = new CountingListener() {
			@Override
			public void onMessage(Message message) {
				try {
					MapMessage map = (MapMessage) message;
					// JMS contract: missing-key getters return zero/false/null instead of NPE
					Assertions.assertEquals(0, map.getInt("nope"));
					Assertions.assertEquals(0L, map.getLong("nope"));
					Assertions.assertEquals(0.0, map.getDouble("nope"));
					Assertions.assertFalse(map.getBoolean("nope"));
					Assertions.assertNull(map.getString("nope"));
					Assertions.assertNull(map.getObject("nope"));
					received.incrementAndGet();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};
		String topic = "org.openmrs.test.missing-keys";
		Event.subscribe(topic, listener);
		try {
			Event.fireEvent(topic, new EventMessage());
			Assertions.assertEquals(1, listener.received.get());
		} finally {
			Event.unsubscribe(topic, listener);
		}
	}

	@Test
	public void shouldDeliverArbitraryTopicEventsImmediately() {
		CountingListener listener = new CountingListener();
		String topic = "org.openmrs.test.topic";
		Event.subscribe(topic, listener);
		try {
			EventMessage msg = new EventMessage();
			msg.put("uuid", "abc");
			msg.put("classname", "x");
			msg.put("action", "CUSTOM");

			Event.fireEvent(topic, msg);

			Assertions.assertEquals(1, listener.received.get());
			Assertions.assertEquals("abc", listener.lastUuid.get());
		} finally {
			Event.unsubscribe(topic, listener);
		}
	}

	private CountingListener subscribe(Class<?> clazz, String action) {
		CountingListener l = new CountingListener();
		Event.subscribe(clazz, action, l);
		registered.add(l);
		return l;
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

	private static class CountingListener implements EventListener {

		final AtomicInteger received = new AtomicInteger();

		final java.util.concurrent.atomic.AtomicReference<String> lastUuid = new java.util.concurrent.atomic.AtomicReference<>();

		@Override
		public void onMessage(Message message) {
			try {
				MapMessage map = (MapMessage) message;
				lastUuid.set(map.getString("uuid"));
				received.incrementAndGet();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		void awaitAtLeast(int n) throws InterruptedException {
			long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(2);
			while (received.get() < n && System.currentTimeMillis() < deadline) {
				Thread.sleep(20);
			}
		}
	}
}
