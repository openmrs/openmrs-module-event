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

import org.openmrs.OpenmrsObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test helper that collects EntityEvents from committed transactions.
 * Uses TransactionEventListener to receive events in a daemon thread after commit.
 */
@Component
public class MockTransactionEventCollector extends TransactionEventListener {

	private final List<EntityEvent> events = Collections.synchronizedList(new ArrayList<>());

	@Override
	public void transactionCommitted(TransactionCommittedEvent transactionEvent) {
		events.addAll(transactionEvent.getEvents());
	}

	public void clear() {
		events.clear();
	}

	public List<EntityEvent> getEvents() {
		return new ArrayList<>(events);
	}

	public boolean hasEvent(OpenmrsObject entity, Event.Action action) {
		String uuid = entity.getUuid();
		return events.stream().anyMatch(e ->
			e.getAction() == action && e.getEntity().getUuid().equals(uuid)
		);
	}
}