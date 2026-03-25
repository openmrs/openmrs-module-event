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
			List<SubscribableEventListener> listeners = HandlerUtil.getHandlersForType(SubscribableEventListener.class, null);
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
