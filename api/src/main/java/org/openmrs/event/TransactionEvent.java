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
		if (events == null) {
			events = new LinkedHashSet<>();
		}
		if (incomingEvents != null) {
			// Do not add duplicate events; CREATE AND PURGE take precedence over UPDATE events
			for (EntityEvent incomingEvent : incomingEvents) {
				boolean hasEvent = events.contains(incomingEvent);
				if (!hasEvent && incomingEvent.getAction() == Event.Action.UPDATED) {
					hasEvent = events.contains(new EntityEvent(incomingEvent.getEntity(), Event.Action.CREATED));
					hasEvent = hasEvent || events.contains(new EntityEvent(incomingEvent.getEntity(), Event.Action.PURGED));
				}
				if (!hasEvent && incomingEvent.getAction() == Event.Action.PURGED) {
					events.remove(new EntityEvent(incomingEvent.getEntity(), Event.Action.UPDATED));
				}
				if (!hasEvent) {
					events.add(incomingEvent);
				}
			}
		}
	}
}
