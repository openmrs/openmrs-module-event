package org.openmrs.module.event.advice;

import org.hibernate.proxy.HibernateProxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.internal.verification.Times;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptName;
import org.openmrs.EncounterType;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.User;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.event.Event;
import org.openmrs.event.EventEngine;
import org.openmrs.event.EventEngineUtil;
import org.openmrs.event.MockEventListener;
import org.openmrs.event.MockNestedService;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@SuppressWarnings("deprecation")
public class EventBehaviorTest extends BaseModuleContextSensitiveTest {

	@Autowired
	ConceptService conceptService;
	
	private static EventEngine eventEngine;
	
	@BeforeAll
	public static void beforeClass() {
		eventEngine = spy(EventEngineUtil.getEventEngine());
		EventEngineUtil.setEventEngine(eventEngine);
	}

    @AfterEach
    public void afterTest() {
        reset(eventEngine);  // need to manually reset the event engine to clean up from previous test
    }


	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldFireEventOnCreate() throws Exception {
		Concept concept = randomConcept();
		conceptService.saveConcept(concept);

		verify(eventEngine).fireAction(Event.Action.CREATED.name(), concept);
	}
	
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldFireEventOnUpdate() throws Exception {
		Concept concept = conceptService.getConcept(3);
		final String newVersion = "new random version";
		Assertions.assertFalse(newVersion.equals(concept.getVersion()));
		concept.setVersion(newVersion);
		conceptService.saveConcept(concept);
		
		verify(eventEngine).fireAction(Event.Action.UPDATED.name(), concept);
	}
	
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldFireEventOnUpdatingProxiedObject() throws Exception {
		Concept concept = conceptService.getConcept(4);
		ConceptClass conceptClass = concept.getConceptClass();
		final String newDescription = "new random description";
		Assertions.assertFalse(newDescription.equals(conceptClass.getDescription()));
		conceptClass.setDescription(newDescription);
		Assertions.assertTrue(conceptClass instanceof HibernateProxy);
		
		conceptService.saveConceptClass(conceptClass);
		
		verify(eventEngine).fireAction(Event.Action.UPDATED.name(), conceptClass);
	}
	
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldFireEventOnCreatingObjects() throws Exception {
		GlobalProperty gp1 = new GlobalProperty("1", "1");
		GlobalProperty gp2 = new GlobalProperty("2", "2");
		MockEventListener listener = new MockEventListener(2);
		Event.subscribe(GlobalProperty.class, null, listener);
		
		Context.getAdministrationService().saveGlobalProperties(Arrays.asList(gp1, gp2));
		
		verify(eventEngine).fireAction(Event.Action.CREATED.name(), gp1);
		verify(eventEngine).fireAction(Event.Action.CREATED.name(), gp2);
	}
	
	/**
	 * @verifies fire event on updating User
	 */
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldFireEventOnUpdatingUser() throws Exception {
		User user = Context.getUserService().getUser(1);
		Context.getUserService().saveUser(user);
		
		verify(eventEngine).fireAction(Event.Action.UPDATED.name(), user);
	}
	
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldFireEventOnWhenUpdatingASubclass() throws Exception {
		Patient patient = Context.getPatientService().getPatient(2);
		patient.setGender("F");
		Context.getPersonService().savePerson(patient);
		
		verify(eventEngine).fireAction(Event.Action.UPDATED.name(), patient);
	}
	
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldFireEventOnCreatingAGlobalProperty() throws Exception {
		MockEventListener listener = new MockEventListener(1);
		eventEngine.subscribe(GlobalProperty.class, null, listener);
		Context.getAdministrationService().saveGlobalProperty(new GlobalProperty("property", "value"));
		
		listener.waitForEvents();
		Assertions.assertEquals(1, listener.getCreatedCount());
		Assertions.assertEquals(0, listener.getUpdatedCount());
	}
	
	/**
	 * @throws Exception
	 */
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldFireEventOnEditingAGlobalProperty() throws Exception {
		//create a test GP
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = new GlobalProperty("property1", "value1");
		gp = as.saveGlobalProperty(gp);
		
		as.saveGlobalProperty(gp);
		
		verify(eventEngine).fireAction(Event.Action.CREATED.name(), gp);
	}
	
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldFireEventWhenAnElementIsAddedToAChildCollection() throws Exception {
		Concept concept = conceptService.getConcept(5089);
		ConceptDescription cd = new ConceptDescription("new descr", Locale.ENGLISH);
		concept.addDescription(cd);
		conceptService.saveConcept(concept);
		
		verify(eventEngine).fireAction(Event.Action.UPDATED.name(), concept);
		verify(eventEngine).fireAction(Event.Action.CREATED.name(), cd);
	}
	
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldFireEventWhenAnElementIsRemovedFromAChildCollection() throws Exception {
		Concept concept = conceptService.getConcept(5497);
		Assertions.assertTrue(concept.getDescriptions().size() > 0);
		concept.removeDescription(concept.getDescription());
		conceptService.saveConcept(concept);
		
		verify(eventEngine).fireAction(Event.Action.UPDATED.name(), concept);
	}
	
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldFireEventOnRetiringAnObject() throws Exception {
		Concept concept = conceptService.getConcept(5497);
		Assertions.assertFalse(concept.isRetired());
		//sanity check that in case we don't retire it, the action isn't fired
		conceptService.saveConcept(concept);
		verify(eventEngine, new Times(0)).fireAction(Event.Action.RETIRED.name(), concept);
		
		conceptService.retireConcept(concept, "testing");
		verify(eventEngine).fireAction(Event.Action.RETIRED.name(), concept);
	}
	
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldFireEventOnUnRetiringAnObject() throws Exception {
		EncounterService es = Context.getEncounterService();
		EncounterType eType = es.getEncounterType(6);
		Assertions.assertTrue(eType.isRetired());
		es.saveEncounterType(eType);
		verify(eventEngine, new Times(0)).fireAction(Event.Action.UNRETIRED.name(), eType);
		
		eType.setRetired(false);
		es.saveEncounterType(eType);
		verify(eventEngine).fireAction(Event.Action.UNRETIRED.name(), eType);
	}
	
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldFireEventOnVoidingAnObject() throws Exception {
		PatientService ps = Context.getPatientService();
		PatientIdentifier pId = ps.getPatientIdentifier(1);
		Assertions.assertFalse(pId.isVoided());
		ps.savePatientIdentifier(pId);
		verify(eventEngine, new Times(0)).fireAction(Event.Action.VOIDED.name(), pId);
		
		ps.voidPatientIdentifier(pId, "testing");
		verify(eventEngine).fireAction(Event.Action.VOIDED.name(), pId);
	}
	
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void shouldFireEventOnUnVoidingAnObject() throws Exception {
		PatientService ps = Context.getPatientService();
		PatientIdentifier pId = ps.getPatientIdentifier(6);
		Assertions.assertTrue(pId.isVoided());
		ps.savePatientIdentifier(pId);
		verify(eventEngine, new Times(0)).fireAction(Event.Action.UNVOIDED.name(), pId);
		
		pId.setVoided(false);
		ps.savePatientIdentifier(pId);
		verify(eventEngine).fireAction(Event.Action.UNVOIDED.name(), pId);
	}

    @Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldFireEventsOnNestedTransactions() throws Exception {

        Concept concept = randomConcept();

        Context.getService(MockNestedService.class).outerTransaction(concept, false, false);

        Patient patient = Context.getPatientService().getPatient(2);
        verify(eventEngine).fireAction(Event.Action.UPDATED.name(), patient);
        verify(eventEngine).fireAction(Event.Action.CREATED.name(), concept);

    }

    @Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldNotFireInnerEventOnInnerTransactionIfRollback() throws Exception {

        Concept concept = randomConcept();

        try {
            Context.getService(MockNestedService.class).outerTransaction(concept, false, true);
        }
        catch (Exception e) {
        }

        Patient patient = Context.getPatientService().getPatient(2);
        verify(eventEngine, never()).fireAction(Event.Action.UPDATED.name(), patient);
        verify(eventEngine).fireAction(Event.Action.CREATED.name(), concept);

    }

    @Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldNotFireOuterEventOnOuterTransactionIfRollback() throws Exception {

		Concept concept = randomConcept();
        try {
            Context.getService(MockNestedService.class).outerTransaction(concept, true, false);
        }
        catch (Exception e) {
        }

        Patient patient = Context.getPatientService().getPatient(2);
        verify(eventEngine).fireAction(Event.Action.UPDATED.name(), patient);
        verify(eventEngine, never()).fireAction(Event.Action.CREATED.name(), concept);
    }

    @Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void shouldNotFireEitherEventOnBothTransactionsIfBothRollbacked() throws Exception {

        Concept concept = randomConcept();

        try {
            Context.getService(MockNestedService.class).outerTransaction(concept, true, true);
        }
        catch (Exception e) {
        }

        Patient patient = Context.getPatientService().getPatient(2);
        verify(eventEngine, never()).fireAction(Event.Action.UPDATED.name(), patient);
        verify(eventEngine, never()).fireAction(Event.Action.CREATED.name(), concept);
    }

	Concept randomConcept() {
		Concept concept = new Concept();
		ConceptName name = new ConceptName(UUID.randomUUID().toString(), Locale.ENGLISH);
		concept.addName(name);
		concept.setDatatype(conceptService.getConceptDatatypeByName("N/A"));
		concept.setConceptClass(conceptService.getConceptClassByName("Misc"));
		return concept;
	}
}
