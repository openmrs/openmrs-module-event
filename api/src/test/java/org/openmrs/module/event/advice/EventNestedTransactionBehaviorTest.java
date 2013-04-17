package org.openmrs.module.event.advice;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.event.Event;
import org.openmrs.event.EventEngine;
import org.openmrs.event.EventEngineUtil;
import org.openmrs.event.MockNestedService;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.test.annotation.NotTransactional;

import java.util.Locale;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class EventNestedTransactionBehaviorTest extends BaseModuleContextSensitiveTest {

    private static EventEngine eventEngine;

    @BeforeClass
    public static void beforeClass() {
        eventEngine = spy(EventEngineUtil.getEventEngine());
        EventEngineUtil.setEventEngine(eventEngine);
    }

    @Test
    @NotTransactional
    public void shouldFireEventsOnNestedTransactions() throws Exception {

        Concept concept = new Concept();
        ConceptName name = new ConceptName("Name", Locale.ENGLISH);
        concept.addName(name);

        Context.getService(MockNestedService.class).outerTransaction(concept, false, false);

        Patient patient = Context.getPatientService().getPatient(2);
        verify(eventEngine).fireAction(Event.Action.UPDATED.name(), patient);
        verify(eventEngine).fireAction(Event.Action.CREATED.name(), concept);

        reset(eventEngine);  // need to manually reset the event engine to prep for next test
    }

    @Test
    @NotTransactional
    public void shouldNotFireInnerEventOnInnerTransactionIfRollback() throws Exception {

        Concept concept = new Concept();
        ConceptName name = new ConceptName("Name", Locale.ENGLISH);
        concept.addName(name);

        try {
            Context.getService(MockNestedService.class).outerTransaction(concept, false, true);
        }
        catch (Exception e) {
        }

        Patient patient = Context.getPatientService().getPatient(2);
        verify(eventEngine, never()).fireAction(Event.Action.UPDATED.name(), patient);
        verify(eventEngine).fireAction(Event.Action.CREATED.name(), concept);

        reset(eventEngine);  // need to manually reset the event engine to prep for next test
    }

    @Test
    @NotTransactional
    public void shouldNotFireOuterEventOnOuterTransactionIfRollback() throws Exception {

        Concept concept = new Concept();
        ConceptName name = new ConceptName("Name", Locale.ENGLISH);
        concept.addName(name);

        try {
            Context.getService(MockNestedService.class).outerTransaction(concept, true, false);
        }
        catch (Exception e) {
        }

        Patient patient = Context.getPatientService().getPatient(2);
        verify(eventEngine).fireAction(Event.Action.UPDATED.name(), patient);
        verify(eventEngine, never()).fireAction(Event.Action.CREATED.name(), concept);

        reset(eventEngine);  // need to manually reset the event engine to prep for next test
    }

    @Test
    @NotTransactional
    public void shouldNotFireEitherEventOnBothTransactionsIfBothRollbacked() throws Exception {

        Concept concept = new Concept();
        ConceptName name = new ConceptName("Name", Locale.ENGLISH);
        concept.addName(name);

        try {
            Context.getService(MockNestedService.class).outerTransaction(concept, true, true);
        }
        catch (Exception e) {
        }

        Patient patient = Context.getPatientService().getPatient(2);
        verify(eventEngine, never()).fireAction(Event.Action.UPDATED.name(), patient);
        verify(eventEngine, never()).fireAction(Event.Action.CREATED.name(), concept);
    }


}
