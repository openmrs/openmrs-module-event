package org.openmrs.event;

import org.openmrs.Concept;
import org.openmrs.Patient;
import org.openmrs.api.OpenmrsService;

public interface MockNestedService extends OpenmrsService {

    void outerTransaction(Concept concept, boolean innerRollback, boolean outerRollback);

    void innerTransaction(boolean rollback);

}
