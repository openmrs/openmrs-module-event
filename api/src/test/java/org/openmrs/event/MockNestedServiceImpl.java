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

import org.openmrs.Concept;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class MockNestedServiceImpl extends BaseOpenmrsService implements MockNestedService {
	
	@Override
	@Transactional
	public void outerTransaction(Concept concept, boolean outerRollback, boolean innerRollback) {
		Context.getConceptService().saveConcept(concept);
		
		try {
			Context.getService(MockNestedService.class).innerTransaction(innerRollback);
		}
		catch (Exception e) {}
		
		if (outerRollback) {
			throw new APIException();
		}
	}
	
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void innerTransaction(boolean rollback) {
		Patient patient = Context.getPatientService().getPatient(2);
		if (patient.getGender().equals("M")) {
			patient.setGender("F");
		} else {
			patient.setGender("M");
		}
		Context.getPatientService().savePatient(patient);
		
		if (rollback) {
			throw new APIException();
		}
	}
	
}
