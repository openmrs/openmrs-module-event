/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.event.api.db.hibernate;

import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PatientProgram;
import org.openmrs.api.PatientService;
import org.openmrs.event.EntityEvent;
import org.openmrs.event.Event;
import org.openmrs.event.TransactionCommittedEvent;
import org.openmrs.event.TransactionEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockTransactionEventListener extends TransactionEventListener {

	@Autowired
	PatientService patientService;

	@Override
	public void transactionCommitted(TransactionCommittedEvent event) {
		for (EntityEvent entityEvent : event.getEvents()) {
			if (entityEvent.getEntity() instanceof PatientProgram) {
				if (entityEvent.getAction() == Event.Action.CREATED) {
					PatientProgram patientProgram = (PatientProgram) entityEvent.getEntity();
					Patient patient = patientProgram.getPatient();
					PatientIdentifierType pit = patientService.getPatientIdentifierType(2);
					PatientIdentifier identifier = new PatientIdentifier(UUID.randomUUID().toString(), pit, patientProgram.getLocation());
					patient.addIdentifier(identifier);
					patientService.savePatientIdentifier(identifier);
				}
			}
		}
	}
}
