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

import java.util.List;

/**
 * Self-describing variant of {@link EventListener}: the module declares which classes and actions
 * it cares about, and {@link EventActivator} subscribes it on module startup.
 *
 * @deprecated since 5.0.0; subclass {@link TransactionEventListener} instead.
 */
@Deprecated
@SuppressWarnings("DeprecatedIsStillUsed")
public interface SubscribableEventListener extends EventListener {

	List<Class<? extends OpenmrsObject>> subscribeToObjects();

	List<String> subscribeToActions();
}
