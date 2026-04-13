/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * A Spring ApplicationEvent representing an action performed on an entity or a custom topic event.
 * Replaces the former JMS MapMessage as the event carrier.
 */
@Getter
public class OpenMrsEntityEvent extends ApplicationEvent {

	private final String action;
	private final String className;
	private final String uuid;
	private final String topic;
	private final EventMessage eventMessage;

	/**
	 * Constructor for entity-based events (fired via fireAction)
	 */
	public OpenMrsEntityEvent(Object source, String topic, String action, String className, String uuid) {
		super(source);
		this.topic = topic;
		this.action = action;
		this.className = className;
		this.uuid = uuid;
		this.eventMessage = null;
	}

	/**
	 * Constructor for custom topic events (fired via fireEvent with EventMessage)
	 */
	public OpenMrsEntityEvent(Object source, String topic, EventMessage eventMessage) {
		super(source);
		this.topic = topic;
		this.action = null;
		this.className = null;
		this.uuid = null;
		this.eventMessage = eventMessage;
	}
}