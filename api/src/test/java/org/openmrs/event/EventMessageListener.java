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

public class EventMessageListener extends MockEventListener {

	private String city;

	private String state;

	EventMessageListener(int expectedEventsCount) {
		super(expectedEventsCount);
	}

	@Override
	public void onEvent(OpenMrsEntityEvent event) {
		EventMessage eventMessage = event.getEventMessage();
		if (eventMessage != null) {
			city = (String) eventMessage.get("city");
			state = (String) eventMessage.get("state");
		}
	}

	public String getCity() {
		return city;
	}

	public String getState() {
		return state;
	}
}
