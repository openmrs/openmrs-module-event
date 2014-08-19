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

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsUtil;

public class EventProperties {	
    public static final String ACTIVEMQ_DATA_DIRECTORY = "event.ActiveMQDataDirectory";
    
    public static String getActiveMQDataDirectory() {
		String activeMQDataDirectory = Context.getAdministrationService()
				.getGlobalProperty(EventProperties.ACTIVEMQ_DATA_DIRECTORY, "");
		if (!StringUtils.isBlank(activeMQDataDirectory)) {
			return activeMQDataDirectory;
		} else {
			
			return new File(OpenmrsUtil.getApplicationDataDirectory(), "activemq").getAbsolutePath();
		}
	}
    
    public static void setActiveMQDataDirectory(String dir) {
    	GlobalProperty gp = Context.getAdministrationService()
				.getGlobalPropertyObject(EventProperties.ACTIVEMQ_DATA_DIRECTORY);
    	if (gp == null) {
    		gp = new GlobalProperty(EventProperties.ACTIVEMQ_DATA_DIRECTORY);
    	}
    	gp.setPropertyValue(dir);
    	
    	Context.getAdministrationService().saveGlobalProperty(gp);
    }
}
