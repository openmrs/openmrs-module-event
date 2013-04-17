package org.openmrs.event;

import org.openmrs.Concept;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class MockNestedServiceImpl extends BaseOpenmrsService implements MockNestedService {

    @Override
    @Transactional
    public void outerTransaction(Concept concept, boolean outerRollback, boolean innerRollback) {
        Context.getConceptService().saveConcept(concept);

        try {
            Context.getService(MockNestedService.class).innerTransaction(innerRollback);
        }
        catch (Exception e) {
        }

        if (outerRollback) {
            throw new APIException();
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void innerTransaction(boolean rollback) {
        Patient patient = Context.getPatientService().getPatient(2);
        patient.setGender("F");
        Context.getPatientService().savePatient(patient);

        if (rollback) {
            throw new APIException();
        }
    }

}
