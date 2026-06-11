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

import org.junit.jupiter.api.BeforeAll;
import org.openmrs.module.Module;
import org.openmrs.module.ModuleFactory;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;

import java.lang.reflect.Method;

public class BaseEventTest extends BaseModuleContextSensitiveTest {
	
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
}
