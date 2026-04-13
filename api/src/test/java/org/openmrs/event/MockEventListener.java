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

import lombok.Getter;
import lombok.Setter;
import org.openmrs.event.Event.Action;

/**
 * A Test Event Listener that just keeps counts of all created, updated and deleted items
 */
@Getter
@Setter
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

	@Override
	public void onEvent(OpenMrsEntityEvent event) {
		String action = event.getAction();
		if (Action.CREATED.toString().equals(action)) {
			createdCount++;
		} else if (Action.UPDATED.toString().equals(action)) {
			updatedCount++;
		} else {
			deletedCount++;
		}

		latch.countDown();
	}
}
