package org.openmrs.module.event.advice;

import org.aopalliance.intercept.MethodInvocation;
import org.hibernate.proxy.HibernateProxy;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openmrs.*;
import org.openmrs.annotation.Handler;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.event.Event;
import org.openmrs.event.EventEngine;
import org.openmrs.event.EventEngineUtil;
import org.openmrs.event.MockEventListener;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class GeneralEventAdviceTest extends BaseModuleContextSensitiveTest {
	
	private static EventEngine eventEngine;
	
	@BeforeClass
	public static void before() {
		eventEngine = spy(EventEngineUtil.getEventEngine());
		EventEngineUtil.setEventEngine(eventEngine);
	}
	
	/**
	 * @see GeneralEventAdvice#invoke(MethodInvocation)
	 * @verifies fire event on create
	 */
	@Test
	public void invoke_shouldFireEventOnCreate() throws Exception {
		Concept concept = new Concept();
		ConceptName name = new ConceptName("Name", Locale.ENGLISH);
		concept.addName(name);
		Context.getConceptService().saveConcept(concept);
		
		verify(eventEngine).fireAction(Event.Action.CREATED.name(), concept);
	}
	
	/**
	 * @see GeneralEventAdvice#invoke(MethodInvocation)
	 * @verifies fire event on update
	 */
	@Test
	public void invoke_shouldFireEventOnUpdate() throws Exception {
		Concept concept = Context.getConceptService().getConcept(3);
		Context.getConceptService().saveConcept(concept);
		
		verify(eventEngine).fireAction(Event.Action.UPDATED.name(), concept);
	}
	
	/**
	 * @see GeneralEventAdvice#getSupportedClasses(Class)
	 */
	@Test
	public void getSupportedClasses_shouldReturnHandlerAnnotatedObjects() {
		Set<String> classes = new GeneralEventAdvice().getSupportedClasses(TestService.class);
		Assert.assertEquals(2, classes.size());
		Assert.assertTrue(classes.contains(TestObject1.class.getName()));
		Assert.assertTrue(classes.contains(TestObject2.class.getName()));
	}
	
	@Handler(supports = { TestObject1.class, TestObject2.class })
	private class TestService {

	}
	
	private class TestObject1 {

	}
	
	private class TestObject2 {

	}
	
	/**
	 * @see GeneralEventAdvice#invoke(MethodInvocation)
	 * @verifies fire event on updating proxied object
	 */
	@Test
	public void invoke_shouldFireEventOnUpdatingProxiedObject() throws Exception {
		Concept concept = Context.getConceptService().getConcept(4);
		ConceptClass conceptClass = concept.getConceptClass();
		Assert.assertTrue("Hibernate proxy", conceptClass instanceof HibernateProxy);
		
		Context.getConceptService().saveConceptClass(conceptClass);
		
		verify(eventEngine).fireAction(Event.Action.UPDATED.name(), conceptClass);
	}
	
	/**
	 * @see GeneralEventAdvice#invoke(MethodInvocation)
	 * @verifies fire event on creating objects
	 */
	@Test
	public void invoke_shouldFireEventOnCreatingObjects() throws Exception {
		GlobalProperty gp1 = new GlobalProperty("1", "1");
		GlobalProperty gp2 = new GlobalProperty("2", "2");
		
		Context.getAdministrationService().saveGlobalProperties(Arrays.asList(gp1, gp2));
		
		verify(eventEngine).fireAction(Event.Action.CREATED.name(), gp1);
		verify(eventEngine).fireAction(Event.Action.CREATED.name(), gp2);
	}
	
	/**
	 * @see GeneralEventAdvice#invoke(MethodInvocation)
	 * @verifies fire event on updating User
	 */
	@Test
	public void invoke_shouldFireEventOnUpdatingUser() throws Exception {
		User user = Context.getUserService().getUser(1);
		Context.getUserService().saveUser(user, "user");
		
		verify(eventEngine).fireAction(Event.Action.UPDATED.name(), user);
	}

    @Test
    @Verifies(value = "should fire event on updating a sub class", method = "invoke(MethodInvocation")
    public void invoke_shouldFireEventOnWhenUpdatingASubclass() throws Exception {
        Patient patient = Context.getPatientService().getPatient(2);
        patient.setGender("F");
        Context.getPersonService().savePerson(patient);

        verify(eventEngine).fireAction(Event.Action.UPDATED.name(), patient);
    }

	/**
	 * @see {@link GeneralEventAdvice#invoke(MethodInvocation)}
	 */
	@Test
	@Verifies(value = "should fire event on creating a global property", method = "invoke(MethodInvocation)")
	public void invoke_shouldFireEventOnCreatingAGlobalProperty() throws Exception {
		MockEventListener listener = new MockEventListener(1);
		eventEngine.subscribe(GlobalProperty.class, null, listener);
		Context.getAdministrationService().saveGlobalProperty(new GlobalProperty("property", "value"));
		
		listener.waitForEvents();
		Assert.assertEquals(1, listener.getCreatedCount());
		Assert.assertEquals(0, listener.getUpdatedCount());
	}
	
	/**
	 * @see {@link GeneralEventAdvice#invoke(MethodInvocation)}
	 */
	@Test
	@Verifies(value = "should fire event on editing a global property", method = "invoke(MethodInvocation)")
	public void invoke_shouldFireEventOnEditingAGlobalProperty() throws Exception {
		//create a test GP
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = new GlobalProperty("property1", "value1");
		gp = as.saveGlobalProperty(gp);
		
		MockEventListener listener = new MockEventListener(1);
		eventEngine.subscribe(GlobalProperty.class, null, listener);
		as.saveGlobalProperty(gp);
		
		listener.waitForEvents();
		Assert.assertEquals(1, listener.getUpdatedCount());
		Assert.assertEquals(0, listener.getCreatedCount());
	}
}
