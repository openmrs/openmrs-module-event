/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.event;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Represents a collection of entity changes during the course of the same transaction, and the status of that tx
 */
@ToString @EqualsAndHashCode(callSuper = false)
public abstract class TransactionEvent extends ApplicationEvent {

	@Getter
	private Set<EntityEvent> events;

	public TransactionEvent(Object source, Set<EntityEvent> incomingEvents) {
		super(source);
		this.events = new LinkedHashSet<>();
		if (incomingEvents != null) {
			// Deduplicate events within the same transaction:
			// - UPDATE is suppressed if a CREATE or PURGE already exists for the same entity
			// - PURGE removes any prior UPDATE for the same entity
			// - PURGE also removes any prior CREATE (entity never "existed" from an observer's perspective)
			for (EntityEvent incomingEvent : incomingEvents) {
				boolean hasEvent = events.contains(incomingEvent);
				if (!hasEvent && incomingEvent.getAction() == Event.Action.UPDATED) {
					hasEvent = events.contains(new EntityEvent(incomingEvent.getEntity(), Event.Action.CREATED));
					hasEvent = hasEvent || events.contains(new EntityEvent(incomingEvent.getEntity(), Event.Action.PURGED));
				}
				if (!hasEvent && incomingEvent.getAction() == Event.Action.PURGED) {
					events.remove(new EntityEvent(incomingEvent.getEntity(), Event.Action.UPDATED));
					// If entity was created in this same tx, remove the CREATE and skip the PURGE —
					// the entity never existed from an observer's perspective
					if (events.remove(new EntityEvent(incomingEvent.getEntity(), Event.Action.CREATED))) {
						continue;
					}
				}
				if (!hasEvent) {
					events.add(incomingEvent);
				}
			}
		}
	}
}
