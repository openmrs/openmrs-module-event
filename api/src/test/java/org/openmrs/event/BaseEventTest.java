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
