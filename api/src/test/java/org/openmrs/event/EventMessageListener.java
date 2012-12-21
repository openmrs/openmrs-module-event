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

import javax.jms.MapMessage;
import javax.jms.Message;

import org.junit.Assert;

public class EventMessageListener extends MockEventListener {
	
	private String city;
	
	private String state;
	
	EventMessageListener(int expectedEventsCount) {
		super(expectedEventsCount);
	}
	
	public void onMessage(Message message) {
		MapMessage mapMessage = (MapMessage) message;
		try {
			city = mapMessage.getString("city");
			state = mapMessage.getString("state");
		}
		catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	/**
	 * @return the city
	 */
	public String getCity() {
		return city;
	}
	
	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}
}
