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

import java.util.List;

import org.openmrs.OpenmrsObject;

/**
 * This interface is used by modules when doing a simple subscription by passing in only this
 * listener class
 */
public interface SubscribableEventListener extends EventListener {
	
	/**
	 * @return a list of classes that this can handle
	 */
	public List<Class<? extends OpenmrsObject>> subscribeToObjects();
	
	/**
	 * @return a list of Actions this listener can deal with
	 */
	public List<String> subscribeToActions();
	
}
