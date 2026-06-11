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
