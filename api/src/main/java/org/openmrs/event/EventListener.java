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

import javax.jms.MessageListener;

/**
 * This interface is implemented by modules that want to register/subscribe to
 * events on the {@link Event}. The {@link #handle(ChangeEvent)} method is
 * called when the specific event occurs in OpenMRS.
 * 
 * TODO: add reference to what is in the message object passed to onMessage method
 */
public interface EventListener extends MessageListener {

}
