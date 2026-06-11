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

import javax.jms.MessageListener;

/**
 * This interface is implemented by modules that want to register/subscribe to events on the
 * {@link Event}. The {@link #handle(ChangeEvent)} method is called when the specific event occurs
 * in OpenMRS. TODO: add reference to what is in the message object passed to onMessage method
 */
public interface EventListener extends MessageListener {
	
}
