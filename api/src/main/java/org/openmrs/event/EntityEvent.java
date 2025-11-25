/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.openmrs.OpenmrsObject;

import java.io.Serializable;

/**
 * Represents a particular action performed on a particular OpenmrsObject entity
 */
@Data
@AllArgsConstructor
public class EntityEvent implements Serializable {

	private OpenmrsObject entity;
	private Event.Action action;

	@Override
	public String toString() {
		return action + " " + entity.getClass().getSimpleName() + "[" + entity.getUuid() + "]";
	}
}
