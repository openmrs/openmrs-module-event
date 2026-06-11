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
import lombok.Getter;
import org.openmrs.OpenmrsObject;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a particular action performed on a particular OpenmrsObject entity.
 * Equality is based on entity UUID + action, not object reference, to ensure correct
 * deduplication even when Hibernate returns different proxy objects for the same entity.
 */
@Getter
@AllArgsConstructor
public class EntityEvent implements Serializable {

	private OpenmrsObject entity;
	private Event.Action action;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof EntityEvent)) return false;
		EntityEvent that = (EntityEvent) o;
		return action == that.action && Objects.equals(entity.getUuid(), that.entity.getUuid());
	}

	@Override
	public int hashCode() {
		return Objects.hash(entity.getUuid(), action);
	}

	@Override
	public String toString() {
		return action + " " + entity.getClass().getSimpleName() + "[" + entity.getUuid() + "]";
	}
}
