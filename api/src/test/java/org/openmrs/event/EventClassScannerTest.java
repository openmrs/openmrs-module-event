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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptComplex;
import org.openmrs.ConceptNumeric;

public class EventClassScannerTest {
	
	private EventClassScanner classScanner = EventClassScanner.getInstance();
	
	@Test
	public void shouldGetClassesThatMatchTheSpecifiedType() throws Exception {
		List<Class<? extends Concept>> classes = classScanner.getClasses(Concept.class);
		assertEquals(3, classes.size());
		assertTrue(classes.contains(Concept.class));
		assertTrue(classes.contains(ConceptNumeric.class));
		assertTrue(classes.contains(ConceptComplex.class));
	}
	
}
