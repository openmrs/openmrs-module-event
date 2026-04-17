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
 * Subscribes to events fired through the legacy {@link Event} static API.
 *
 * @deprecated since 5.0.0; subclass {@link TransactionEventListener} instead. The JMS-style
 *             {@code onMessage(Message)} delivery is preserved only for backwards compatibility
 *             with modules written against the previous ActiveMQ-backed implementation, and will
 *             be removed in a future release.
 */
@Deprecated
@SuppressWarnings("DeprecatedIsStillUsed")
public interface EventListener extends MessageListener {

}
