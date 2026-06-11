/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.event.api.db.hibernate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PatientProgram;
import org.openmrs.Program;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.event.BaseEventTest;
import org.openmrs.event.Event;
import org.openmrs.event.EventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class HibernateEventInterceptorTest extends BaseEventTest {
	
	@Autowired
	PatientService patientService;
	
	@Autowired
	LocationService locationService;
	
	@Autowired
	ProgramWorkflowService programWorkflowService;
	
	@Autowired
	PlatformTransactionManager transactionManager;
	
	DefaultTransactionDefinition definition;
	
	PatientIdentifierType oldId;
	
	Location unknownLocation;
	
	Program program;
	
	@BeforeEach
	public void setupTest() {
		definition = new DefaultTransactionDefinition();
		oldId = patientService.getPatientIdentifierType(2);
		unknownLocation = locationService.getLocation(1);
		program = programWorkflowService.getProgram(1);
	}
	
	/**
	 * @see Event#subscribe(Class, String, EventListener)
	 */
	@Test
	public void shouldSuccessfullySaveAdditionalPatientDataAfterTransactionCompletion() throws Exception {
		Patient patient = patientService.getPatient(2);
		int startingIdentifierNum = patient.getIdentifiers().size();
		
		// This block constructs a patient program, saves and commits it, which is expected to result in a
		// transaction committed event that is received by the MockTransactionEventListener, which creates a new
		// patient identifier for the program patient.  This then clears the hibernate session and confirms that the
		// newly created data actually exists in the DB
		
		PatientProgram pp = new PatientProgram();
		pp.setPatient(patient);
		pp.setProgram(program);
		pp.setLocation(unknownLocation);
		pp.setDateEnrolled(new Date());
		TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
		try {
			programWorkflowService.savePatientProgram(pp);
			transactionManager.commit(status);
		}
		catch (Exception e) {
			System.out.println("Patient program failed to save: " + e.getMessage());
			transactionManager.rollback(status);
		}
		Context.clearSession();
		patient = patientService.getPatient(2);
		assertEquals(startingIdentifierNum + 1, patient.getIdentifiers().size());
		// TestUtil.printOutTableContents(getConnection(), "patient_identifier");
	}
}
