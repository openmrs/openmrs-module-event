package org.openmrs.module.event.advice;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Locale;

import org.aopalliance.intercept.MethodInvocation;
import org.hibernate.proxy.HibernateProxy;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
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
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.test.annotation.NotTransactional;

@SuppressWarnings("deprecation")
public class EventBehaviorTest extends BaseModuleContextSensitiveTest {
	
	private static EventEngine eventEngine;
	
	@BeforeClass
	public static void beforeClass() {
		eventEngine = spy(EventEngineUtil.getEventEngine());
		EventEngineUtil.setEventEngine(eventEngine);
	}
	
	@Test
	@NotTransactional
	public void shouldFireEventOnCreate() throws Exception {
		Concept concept = new Concept();
		ConceptName name = new ConceptName("Name", Locale.ENGLISH);
		concept.addName(name);
		
		Context.getConceptService().saveConcept(concept);
		
		verify(eventEngine).fireAction(Event.Action.CREATED.name(), concept);
	}
	
	@Test
	@NotTransactional
	public void shouldFireEventOnUpdate() throws Exception {
		Concept concept = Context.getConceptService().getConcept(3);
		final String newVersion = "new random version";
		Assert.assertFalse(newVersion.equals(concept.getVersion()));
		concept.setVersion(newVersion);
		Context.getConceptService().saveConcept(concept);
		
		verify(eventEngine).fireAction(Event.Action.UPDATED.name(), concept);
	}
	
	@Test
	@NotTransactional
	public void shouldFireEventOnUpdatingProxiedObject() throws Exception {
		Concept concept = Context.getConceptService().getConcept(4);
		ConceptClass conceptClass = concept.getConceptClass();
		final String newDescription = "new random description";
		Assert.assertFalse(newDescription.equals(conceptClass.getDescription()));
		conceptClass.setDescription(newDescription);
		Assert.assertTrue("Hibernate proxy", conceptClass instanceof HibernateProxy);
		
		Context.getConceptService().saveConceptClass(conceptClass);
		
		verify(eventEngine).fireAction(Event.Action.UPDATED.name(), conceptClass);
	}
	
	@Test
	@NotTransactional
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
	 * @see GeneralEventAdvice#invoke(MethodInvocation)
	 * @verifies fire event on updating User
	 */
	@Test
	@NotTransactional
	public void shouldFireEventOnUpdatingUser() throws Exception {
		User user = Context.getUserService().getUser(1);
		Context.getUserService().saveUser(user, "user");
		
		verify(eventEngine).fireAction(Event.Action.UPDATED.name(), user);
	}
	
	@Test
	@NotTransactional
	public void shouldFireEventOnWhenUpdatingASubclass() throws Exception {
		Patient patient = Context.getPatientService().getPatient(2);
		patient.setGender("F");
		Context.getPersonService().savePerson(patient);
		
		verify(eventEngine).fireAction(Event.Action.UPDATED.name(), patient);
	}
	
	@Test
	@NotTransactional
	public void shouldFireEventOnCreatingAGlobalProperty() throws Exception {
		MockEventListener listener = new MockEventListener(1);
		eventEngine.subscribe(GlobalProperty.class, null, listener);
		Context.getAdministrationService().saveGlobalProperty(new GlobalProperty("property", "value"));
		
		listener.waitForEvents();
		Assert.assertEquals(1, listener.getCreatedCount());
		Assert.assertEquals(0, listener.getUpdatedCount());
	}
	
	/**
	 * @throws Exception
	 */
	@Test
	@NotTransactional
	public void shouldFireEventOnEditingAGlobalProperty() throws Exception {
		//create a test GP
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = new GlobalProperty("property1", "value1");
		gp = as.saveGlobalProperty(gp);
		
		as.saveGlobalProperty(gp);
		
		verify(eventEngine).fireAction(Event.Action.CREATED.name(), gp);
	}
	
	@Test
	@NotTransactional
	public void shouldFireEventWhenAnElementIsAddedToAChildCollection() throws Exception {
		ConceptService cs = Context.getConceptService();
		Concept concept = cs.getConcept(5089);
		ConceptDescription cd = new ConceptDescription("new descr", Locale.ENGLISH);
		concept.addDescription(cd);
		cs.saveConcept(concept);
		
		verify(eventEngine).fireAction(Event.Action.UPDATED.name(), concept);
		verify(eventEngine).fireAction(Event.Action.CREATED.name(), cd);
	}
	
	@Test
	@NotTransactional
	public void shouldFireEventWhenAnElementIsRemovedFromAChildCollection() throws Exception {
		ConceptService cs = Context.getConceptService();
		Concept concept = cs.getConcept(5497);
		Assert.assertTrue(concept.getDescriptions().size() > 0);
		concept.removeDescription(concept.getDescription());
		cs.saveConcept(concept);
		
		verify(eventEngine).fireAction(Event.Action.UPDATED.name(), concept);
	}
	
	@Test
	@NotTransactional
	public void shouldFireEventOnRetiringAnObject() throws Exception {
		ConceptService cs = Context.getConceptService();
		Concept concept = cs.getConcept(5497);
		Assert.assertFalse(concept.isRetired());
		//sanity check that in case we don't retire it, the action isn't fired
		cs.saveConcept(concept);
		verify(eventEngine, new Times(0)).fireAction(Event.Action.RETIRED.name(), concept);
		
		cs.retireConcept(concept, "testing");
		verify(eventEngine).fireAction(Event.Action.RETIRED.name(), concept);
	}
	
	@Test
	@NotTransactional
	public void shouldFireEventOnUnRetiringAnObject() throws Exception {
		EncounterService es = Context.getEncounterService();
		EncounterType eType = es.getEncounterType(6);
		Assert.assertTrue(eType.isRetired());
		es.saveEncounterType(eType);
		verify(eventEngine, new Times(0)).fireAction(Event.Action.UNRETIRED.name(), eType);
		
		eType.setRetired(false);
		es.saveEncounterType(eType);
		verify(eventEngine).fireAction(Event.Action.UNRETIRED.name(), eType);
	}
	
	@Test
	@NotTransactional
	public void shouldFireEventOnVoidingAnObject() throws Exception {
		PatientService ps = Context.getPatientService();
		PatientIdentifier pId = ps.getPatientIdentifier(1);
		Assert.assertFalse(pId.isVoided());
		ps.savePatientIdentifier(pId);
		verify(eventEngine, new Times(0)).fireAction(Event.Action.VOIDED.name(), pId);
		
		ps.voidPatientIdentifier(pId, "testing");
		verify(eventEngine).fireAction(Event.Action.VOIDED.name(), pId);
	}
	
	@Test
	@NotTransactional
	public void shouldFireEventOnUnVoidingAnObject() throws Exception {
		PatientService ps = Context.getPatientService();
		PatientIdentifier pId = ps.getPatientIdentifier(6);
		Assert.assertTrue(pId.isVoided());
		ps.savePatientIdentifier(pId);
		verify(eventEngine, new Times(0)).fireAction(Event.Action.UNVOIDED.name(), pId);
		
		pId.setVoided(false);
		ps.savePatientIdentifier(pId);
		verify(eventEngine).fireAction(Event.Action.UNVOIDED.name(), pId);
	}
}
