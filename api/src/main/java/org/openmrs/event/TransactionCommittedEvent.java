/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.event;

import java.util.Set;

/**
 * Represents an event notification that a transaction has completed successfully and data is saved
 * Listeners for this event will not operate in the same transaction as the underlying data that was changed
 */
public class TransactionCommittedEvent extends TransactionEvent {
    public TransactionCommittedEvent(Object source, Set<EntityEvent> events) {
        super(source, events);
    }
}
