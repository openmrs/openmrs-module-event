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
package org.openmrs.event.api.db.hibernate;

import org.junit.jupiter.api.BeforeAll;
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
import org.openmrs.event.EventActivator;
import org.openmrs.event.EventListener;
import org.openmrs.module.Module;
import org.openmrs.module.ModuleFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.lang.reflect.Method;
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

	@BeforeAll
	public static void setupDaemonToken() throws Exception {
		Module module = new Module("event");
		module.setModuleId("event");
		module.setModuleActivator(new EventActivator());

		Method passDaemonTokenMethod = ModuleFactory.class.getDeclaredMethod("passDaemonToken", Module.class);
		passDaemonTokenMethod.setAccessible(true);
		passDaemonTokenMethod.invoke(null, module);

		Method getDaemonTokenMethod = ModuleFactory.class.getDeclaredMethod("getDaemonToken", Module.class);
		getDaemonTokenMethod.setAccessible(true);
	}

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
