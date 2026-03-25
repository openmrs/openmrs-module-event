/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.event;

import lombok.Setter;
import org.openmrs.api.context.Daemon;
import org.openmrs.module.DaemonToken;
import org.springframework.context.ApplicationListener;

/**
 * Intended as a superclass for all Application Listeners of Transaction Events
 * Enables a single Listener to support more than one type of transaction event, and
 * ensures that all events that occur after a transaction has completed are processed in a separate Daemon thread
 */
public abstract class TransactionEventListener implements ApplicationListener<TransactionEvent> {

	@Setter
	private static DaemonToken daemonToken = null;

	@Override
	public final void onApplicationEvent(TransactionEvent transactionEvent) {
		if (transactionEvent.getEvents() != null && !transactionEvent.getEvents().isEmpty()) {
			if (transactionEvent instanceof TransactionAfterBeginEvent) {
				afterTransactionBegin((TransactionAfterBeginEvent) transactionEvent);
			}
			else if (transactionEvent instanceof TransactionBeforeCompletionEvent) {
				beforeTransactionCompletion((TransactionBeforeCompletionEvent) transactionEvent);
			}
			else if (transactionEvent instanceof TransactionCommittedEvent) {
				Daemon.runInDaemonThreadAndWait(() ->
					transactionCommitted((TransactionCommittedEvent) transactionEvent), daemonToken);
			}
			else if (transactionEvent instanceof TransactionNotCommittedEvent) {
				Daemon.runInDaemonThreadAndWait(() ->
					transactionNotCommitted((TransactionNotCommittedEvent) transactionEvent), daemonToken);
			}
			transactionEvent(transactionEvent);
		}
	}

	public void afterTransactionBegin(TransactionAfterBeginEvent transactionEvent) {
	}

	public void beforeTransactionCompletion(TransactionBeforeCompletionEvent transactionEvent) {
	}

	public void transactionCommitted(TransactionCommittedEvent transactionEvent) {
	}

	public void transactionNotCommitted(TransactionNotCommittedEvent transactionEvent) {
	}

	public void transactionEvent(TransactionEvent transactionEvent) {
	}
}
