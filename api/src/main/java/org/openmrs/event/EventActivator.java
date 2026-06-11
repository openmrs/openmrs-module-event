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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.OpenmrsObject;
import org.openmrs.event.Event.Action;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.DaemonTokenAware;
import org.openmrs.util.HandlerUtil;

import java.util.Collection;
import java.util.List;

/**
 * This class contains the logic that is run every time this module is either started or stopped.
 */
public class EventActivator extends BaseModuleActivator implements DaemonTokenAware {
	
	protected Log log = LogFactory.getLog(getClass());
	
	@Override
	public void started() {
		log.info("Event Module started");
		List<SubscribableEventListener> listeners = HandlerUtil.getHandlersForType(SubscribableEventListener.class, null);
		try (EventClassScannerThreadHolder holder = new EventClassScannerThreadHolder()) {
			for (SubscribableEventListener listener : listeners) {
				Collection<String> actions = listener.subscribeToActions();
				for (Class<? extends OpenmrsObject> clazz : listener.subscribeToObjects()) {
					Event.subscribe(clazz, actions, listener);
				}
			}
		}
	}
	
	@Override
	public void stopped() {
		log.info("Event Module stopped");
		try {
			List<SubscribableEventListener> listeners = HandlerUtil.getHandlersForType(SubscribableEventListener.class,
			    null);
			try (EventClassScannerThreadHolder holder = new EventClassScannerThreadHolder()) {
				for (SubscribableEventListener listener : listeners) {
					for (Class<? extends OpenmrsObject> clazz : listener.subscribeToObjects()) {
						for (String action : listener.subscribeToActions()) {
							Event.unsubscribe(clazz, Action.valueOf(action), listener);
						}
					}
				}
			}
		}
		finally {
			Event.shutdown();
		}
	}
	
	@Override
	public void setDaemonToken(DaemonToken daemonToken) {
		TransactionEventListener.setDaemonToken(daemonToken);
	}
}
