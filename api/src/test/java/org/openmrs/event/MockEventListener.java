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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;

import junit.framework.Assert;

import org.openmrs.event.Event.Action;

/**
 * A Test Event Listener that just keeps counts of all created, updated and deleted items
 */
public class MockEventListener implements EventListener {
	
	private int createdCount = 0;
	
	private int updatedCount = 0;
	
	private int deletedCount = 0;
	
	private CountDownLatch latch;
	
	/**
	 * The count should be set to the number of expected events.
	 * 
	 * @param expectedEventsCount
	 */
	public MockEventListener(int expectedEventsCount) {
		latch = new CountDownLatch(expectedEventsCount);
	}
	
	/**
	 * Releases the old counter and sets a new one.
	 * 
	 * @param expectedEventsCount
	 */
	public void setExpectedEventsCount(int expectedEventsCount) {
		while (latch.getCount() > 0) {
			latch.countDown();
		}
		latch = new CountDownLatch(expectedEventsCount);
	}
	
	/**
	 * Waits for events for at most 2 seconds.
	 * 
	 * @throws InterruptedException
	 */
	public void waitForEvents() throws InterruptedException {
		waitForEvents(2, TimeUnit.SECONDS);
	}
	
	/**
	 * Allows to wait for events.
	 * 
	 * @param timeout
	 * @param unit
	 * @throws InterruptedException
	 */
	public void waitForEvents(long timeout, TimeUnit unit) throws InterruptedException {
		latch.await(timeout, unit);
	}
	
	/**
	 * @return the createdCount
	 */
	public int getCreatedCount() {
		return createdCount;
	}
	
	/**
	 * @param createdCount the createdCount to set
	 */
	public void setCreatedCount(int createdCount) {
		this.createdCount = createdCount;
	}
	
	/**
	 * @return the updatedCount
	 */
	public int getUpdatedCount() {
		return updatedCount;
	}
	
	/**
	 * @param updatedCount the updatedCount to set
	 */
	public void setUpdatedCount(int updatedCount) {
		this.updatedCount = updatedCount;
	}
	
	/**
	 * @return the deletedCount
	 */
	public int getDeletedCount() {
		return deletedCount;
	}
	
	/**
	 * @param deletedCount the deletedCount to set
	 */
	public void setDeletedCount(int deletedCount) {
		this.deletedCount = deletedCount;
	}
	
	/**
	 * @see javax.jms.MessageListener#onMessage(Message)
	 */
	@Override
	public void onMessage(Message message) {
		try {
			MapMessage mapMessage = (MapMessage) message;
			if (Action.CREATED.toString().equals(mapMessage.getString("action")))
				createdCount++;
			else if (Action.UPDATED.toString().equals(mapMessage.getString("action")))
				updatedCount++;
			else
				deletedCount++;
			
			latch.countDown();
		}
		catch (JMSException e) {
			Assert.fail(e.getMessage());
		}
	}
	
}
