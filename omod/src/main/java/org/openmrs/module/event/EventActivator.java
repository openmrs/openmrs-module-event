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
package org.openmrs.module.event;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.OpenmrsObject;
import org.openmrs.event.Event;
import org.openmrs.event.Event.Action;
import org.openmrs.event.EventProperties;
import org.openmrs.event.SubscribableEventListener;
import org.openmrs.module.ModuleActivator;
import org.openmrs.util.HandlerUtil;

/**
 * This class contains the logic that is run every time this module is either started or stopped.
 */
public class EventActivator implements ModuleActivator {

	protected Log log = LogFactory.getLog(getClass());

	public static String activeMQDirectory;

	/**
	 * @see ModuleActivator#willRefreshContext()
	 */
	public void willRefreshContext() {
		log.info("Refreshing Event Module");
	}

	/**
	 * @see ModuleActivator#contextRefreshed()
	 */
	public void contextRefreshed() {
		log.info("Event Module refreshed");

		// TODO: look for @Handler for events and automatically register those advice/listeners? -- EVNT-11
	}

	/**
	 * @see ModuleActivator#willStart()
	 */
	public void willStart() {
		log.info("Starting Event Queue Module");
	}

	/**
	 * @should create subscriptions for all subscribable event listeners
	 * @should create new ActiveMQ directory
	 * @see ModuleActivator#started()
	 */
	public void started() {
		log.info("Event Queue Module started");

		List<SubscribableEventListener> listeners = HandlerUtil.getHandlersForType(SubscribableEventListener.class, null);
		for (SubscribableEventListener listener : listeners) {
			for (Class<? extends OpenmrsObject> clazz : listener.subscribeToObjects()) {
				for (String action : listener.subscribeToActions()) {
					Event.subscribe(clazz, action, listener);
				}
			}
		}
		
		activeMQDirectory = EventProperties.getActiveMQDataDirectory();
	}

	/**
	 * @see ModuleActivator#willStop()
	 */
	public void willStop() {
		log.info("Stopping Event Module");
	}

	/**
	 * @should shutdown the jms connection
	 * @should delete ActiveMQ directory
	 * @should delete old ActiveMQ directory, not new, given by user
	 * @see ModuleActivator#stopped()
	 */
	public void stopped() {
		log.info("Event Module stopped");
		try {
			List<SubscribableEventListener> listeners = HandlerUtil
					.getHandlersForType(SubscribableEventListener.class, null);
			for (SubscribableEventListener listener : listeners) {
				for (Class<? extends OpenmrsObject> clazz : listener.subscribeToObjects()) {
					for (String action : listener.subscribeToActions()) {
						Event.unsubscribe(clazz, Action.valueOf(action), listener);
					}
				}
			}
		}
		finally {
			Event.shutdown();
		}
		
		deleteLastActiveMQDataDirectory();
	}

	private void deleteLastActiveMQDataDirectory() {
		try {
			FileUtils.deleteDirectory(new File(activeMQDirectory));
		}
		catch (IOException e) {
			log.warn(e.getMessage());
		}
	}
}
