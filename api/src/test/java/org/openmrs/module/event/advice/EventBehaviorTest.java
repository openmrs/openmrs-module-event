package org.openmrs.module.event.advice;

import org.hibernate.proxy.HibernateProxy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.openmrs.event.BaseEventTest;
import org.openmrs.event.Event;
import org.openmrs.event.MockNestedService;
import org.openmrs.event.MockTransactionEventCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class EventBehaviorTest extends BaseEventTest {

	@Autowired
	ConceptService conceptService;

	@Autowired
	MockTransactionEventCollector collector;

	@BeforeEach
	public void before() {
		collector.clear();
	}

	@Test
	public void shouldFireEventOnCreate() {
		Concept concept = randomConcept();
		conceptService.saveConcept(concept);

		Assertions.assertTrue(collector.hasEvent(concept, Event.Action.CREATED));
	}

	@Test
	public void shouldFireEventOnUpdate() {
		Concept concept = conceptService.getConcept(3);
		final String newVersion = "new random version";
		Assertions.assertFalse(newVersion.equals(concept.getVersion()));
		concept.setVersion(newVersion);
		conceptService.saveConcept(concept);

		Assertions.assertTrue(collector.hasEvent(concept, Event.Action.UPDATED));
	}

	@Test
	public void shouldFireEventOnUpdatingProxiedObject() {
		Concept concept = conceptService.getConcept(4);
		ConceptClass conceptClass = concept.getConceptClass();
		final String newDescription = "new random description";
		Assertions.assertFalse(newDescription.equals(conceptClass.getDescription()));
		conceptClass.setDescription(newDescription);
		Assertions.assertTrue(conceptClass instanceof HibernateProxy);

		conceptService.saveConceptClass(conceptClass);

		Assertions.assertTrue(collector.hasEvent(conceptClass, Event.Action.UPDATED));
	}

	@Test
	public void shouldFireEventOnCreatingObjects() {
		GlobalProperty gp1 = new GlobalProperty("1", "1");
		GlobalProperty gp2 = new GlobalProperty("2", "2");

		Context.getAdministrationService().saveGlobalProperties(Arrays.asList(gp1, gp2));

		Assertions.assertTrue(collector.hasEvent(gp1, Event.Action.CREATED));
		Assertions.assertTrue(collector.hasEvent(gp2, Event.Action.CREATED));
	}

	@Test
	public void shouldFireEventOnUpdatingUser() {
		User user = Context.getUserService().getUser(1);
		Context.getUserService().saveUser(user);

		Assertions.assertTrue(collector.hasEvent(user, Event.Action.UPDATED));
	}

	@Test
	public void shouldFireEventOnWhenUpdatingASubclass() {
		Patient patient = Context.getPatientService().getPatient(2);
		patient.setGender("F");
		Context.getPersonService().savePerson(patient);

		Assertions.assertTrue(collector.hasEvent(patient, Event.Action.UPDATED));
	}

	@Test
	public void shouldFireEventOnCreatingAGlobalProperty() {
		Context.getAdministrationService().saveGlobalProperty(new GlobalProperty("property", "value"));

		long createdCount = collector.getEvents().stream()
			.filter(e -> e.getAction() == Event.Action.CREATED && e.getEntity() instanceof GlobalProperty)
			.count();
		Assertions.assertEquals(1, createdCount);
	}

	@Test
	public void shouldFireEventOnEditingAGlobalProperty() {
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = new GlobalProperty("property1", "value1");
		gp = as.saveGlobalProperty(gp);

		Assertions.assertTrue(collector.hasEvent(gp, Event.Action.CREATED));
	}

	@Test
	public void shouldFireEventWhenAnElementIsAddedToAChildCollection() {
		Concept concept = conceptService.getConcept(5089);
		ConceptDescription cd = new ConceptDescription("new descr", Locale.ENGLISH);
		concept.addDescription(cd);
		conceptService.saveConcept(concept);

		Assertions.assertTrue(collector.hasEvent(concept, Event.Action.UPDATED));
		Assertions.assertTrue(collector.hasEvent(cd, Event.Action.CREATED));
	}

	@Test
	public void shouldFireEventWhenAnElementIsRemovedFromAChildCollection() {
		Concept concept = conceptService.getConcept(5497);
		Assertions.assertTrue(concept.getDescriptions().size() > 0);
		concept.removeDescription(concept.getDescription());
		conceptService.saveConcept(concept);

		Assertions.assertTrue(collector.hasEvent(concept, Event.Action.UPDATED));
	}

	@Test
	public void shouldFireEventOnRetiringAnObject() {
		Concept concept = conceptService.getConcept(5497);
		Assertions.assertFalse(concept.isRetired());
		conceptService.saveConcept(concept);
		Assertions.assertFalse(collector.hasEvent(concept, Event.Action.RETIRED));

		collector.clear();
		conceptService.retireConcept(concept, "testing");
		Assertions.assertTrue(collector.hasEvent(concept, Event.Action.RETIRED));
	}

	@Test
	public void shouldFireEventOnUnRetiringAnObject() {
		EncounterService es = Context.getEncounterService();
		EncounterType eType = es.getEncounterType(6);
		Assertions.assertTrue(eType.isRetired());
		es.saveEncounterType(eType);
		Assertions.assertFalse(collector.hasEvent(eType, Event.Action.UNRETIRED));

		collector.clear();
		eType.setRetired(false);
		es.saveEncounterType(eType);
		Assertions.assertTrue(collector.hasEvent(eType, Event.Action.UNRETIRED));
	}

	@Test
	public void shouldFireEventOnVoidingAnObject() {
		PatientService ps = Context.getPatientService();
		PatientIdentifier pId = ps.getPatientIdentifier(1);
		Assertions.assertFalse(pId.isVoided());
		ps.savePatientIdentifier(pId);
		Assertions.assertFalse(collector.hasEvent(pId, Event.Action.VOIDED));

		collector.clear();
		ps.voidPatientIdentifier(pId, "testing");
		Assertions.assertTrue(collector.hasEvent(pId, Event.Action.VOIDED));
	}

	@Test
	public void shouldFireEventOnUnVoidingAnObject() {
		PatientService ps = Context.getPatientService();
		PatientIdentifier pId = ps.getPatientIdentifier(6);
		Assertions.assertTrue(pId.isVoided());
		ps.savePatientIdentifier(pId);
		Assertions.assertFalse(collector.hasEvent(pId, Event.Action.UNVOIDED));

		collector.clear();
		pId.setVoided(false);
		ps.savePatientIdentifier(pId);
		Assertions.assertTrue(collector.hasEvent(pId, Event.Action.UNVOIDED));
	}

	@Test
	public void shouldFireEventsOnNestedTransactions() {
		Concept concept = randomConcept();
		Context.getService(MockNestedService.class).outerTransaction(concept, false, false);

		Patient patient = Context.getPatientService().getPatient(2);
		Assertions.assertTrue(collector.hasEvent(patient, Event.Action.UPDATED));
		Assertions.assertTrue(collector.hasEvent(concept, Event.Action.CREATED));
	}

	@Test
	public void shouldNotFireInnerEventOnInnerTransactionIfRollback() {
		Concept concept = randomConcept();
		try {
			Context.getService(MockNestedService.class).outerTransaction(concept, false, true);
		} catch (Exception e) {
		}

		Patient patient = Context.getPatientService().getPatient(2);
		Assertions.assertFalse(collector.hasEvent(patient, Event.Action.UPDATED));
		Assertions.assertTrue(collector.hasEvent(concept, Event.Action.CREATED));
	}

	@Test
	public void shouldNotFireOuterEventOnOuterTransactionIfRollback() {
		Concept concept = randomConcept();
		try {
			Context.getService(MockNestedService.class).outerTransaction(concept, true, false);
		} catch (Exception e) {
		}

		Patient patient = Context.getPatientService().getPatient(2);
		Assertions.assertTrue(collector.hasEvent(patient, Event.Action.UPDATED));
		Assertions.assertFalse(collector.hasEvent(concept, Event.Action.CREATED));
	}

	@Test
	public void shouldNotFireEitherEventOnBothTransactionsIfBothRollbacked() {
		Concept concept = randomConcept();
		try {
			Context.getService(MockNestedService.class).outerTransaction(concept, true, true);
		} catch (Exception e) {
		}

		Patient patient = Context.getPatientService().getPatient(2);
		Assertions.assertFalse(collector.hasEvent(patient, Event.Action.UPDATED));
		Assertions.assertFalse(collector.hasEvent(concept, Event.Action.CREATED));
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
