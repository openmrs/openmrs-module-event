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
