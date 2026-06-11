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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptComplex;
import org.openmrs.ConceptNumeric;

public class EventClassScannerTest {
	
	private final EventClassScanner classScanner = new EventClassScanner();
	
	@Test
	public void shouldGetClassesThatMatchTheSpecifiedType() throws Exception {
		List<Class<? extends Concept>> classes = classScanner.getClasses(Concept.class);
		assertEquals(3, classes.size());
		assertTrue(classes.contains(Concept.class));
		assertTrue(classes.contains(ConceptNumeric.class));
		assertTrue(classes.contains(ConceptComplex.class));
	}
}
