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
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.DaemonTokenAware;
import org.openmrs.util.HandlerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * This class contains the logic that is run every time this module is either started or stopped.
 * Also re-wires the deprecated {@link SubscribableEventListener} registrations on context refresh
 * so that subscriptions survive (and pick up any new beans from) a module restart in the same VM.
 */
public class EventActivator extends BaseModuleActivator implements DaemonTokenAware {

	private static final Logger log = LoggerFactory.getLogger(EventActivator.class);

	private volatile boolean started = false;

	@Override
	public void started() {
		log.info("Event Module started");
		subscribeLegacyListeners();
		started = true;
	}

	@Override
	public void willRefreshContext() {
		if (started) {
			unsubscribeLegacyListeners();
		}
	}

	@Override
	public void contextRefreshed() {
		if (started) {
			subscribeLegacyListeners();
		}
	}

	@Override
	public void stopped() {
		log.info("Event Module stopped");
		unsubscribeLegacyListeners();
		started = false;
	}

	@Override
	public void setDaemonToken(DaemonToken daemonToken) {
		TransactionEventListener.setDaemonToken(daemonToken);
	}

	/**
	 * Wires up legacy {@link SubscribableEventListener} beans against the deprecated {@link Event}
	 * facade. New listeners should subclass {@link TransactionEventListener} directly and are
	 * picked up by Spring automatically — this path exists only for the compat shim.
	 */
	@SuppressWarnings("deprecation")
	private void subscribeLegacyListeners() {
		List<SubscribableEventListener> listeners = HandlerUtil.getHandlersForType(SubscribableEventListener.class, null);
		for (SubscribableEventListener listener : listeners) {
			Collection<String> actions = listener.subscribeToActions();
			for (Class<? extends OpenmrsObject> clazz : listener.subscribeToObjects()) {
				Event.subscribe(clazz, actions, listener);
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void unsubscribeLegacyListeners() {
		List<SubscribableEventListener> listeners = HandlerUtil.getHandlersForType(SubscribableEventListener.class, null);
		for (SubscribableEventListener listener : listeners) {
			for (Class<? extends OpenmrsObject> clazz : listener.subscribeToObjects()) {
				for (String action : listener.subscribeToActions()) {
					// use the String-accepting overload so custom (non-enum) action names
					// registered via SubscribableEventListener.subscribeToActions() don't
					// crash shutdown/refresh with IllegalArgumentException
					Event.unsubscribe(clazz, action, listener);
				}
			}
		}
	}
}

