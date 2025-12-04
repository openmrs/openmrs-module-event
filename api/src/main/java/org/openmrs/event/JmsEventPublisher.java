/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.event;

import org.openmrs.event.api.db.hibernate.HibernateEventInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Application listener that publishes all changes after they are committed to an asynchronous JMS topic
 */
@Component
public class JmsEventPublisher implements ApplicationListener<TransactionCommittedEvent> {

	private static final Logger log = LoggerFactory.getLogger(HibernateEventInterceptor.class);

	@Override
	public void onApplicationEvent(TransactionCommittedEvent transactionCommittedEvent) {
		if (transactionCommittedEvent.getEvents() != null) {
			for (EntityEvent entityEvent : transactionCommittedEvent.getEvents()) {
				log.trace("Firing event {}: ", entityEvent);
				Event.fireAction(entityEvent.getAction().name(), entityEvent.getEntity());
			}
		}
	}
}
